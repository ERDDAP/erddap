/* 
 * LoadDatasets Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;


import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.sgt.GSHHS;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
//import gov.noaa.pfel.erddap.variable.EDVTimeGridAxis;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
//import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
//import org.apache.lucene.queryParser.ParseException;
//import org.apache.lucene.queryParser.QueryParser;
//import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.SimpleFSDirectory;
//import org.apache.lucene.util.Version;

/**
 * This class is run in a separate thread to load datasets for ERDDAP.
 * !!!A lot of possible thread synchronization issues in this class 
 * don't arise because of the assumption that only one 
 * LoadDatasets thread will be running at once (coordinated by RunLoadDatasets).
 *
 * <p>Features:<ul>
 * <li> Different datasets can have different reloadEveryNMinutes settings.
 * <li> This doesn't load datasets if they exist and are young,
 *     so don't need to be reloaded.
 * <li> Erddap starts up quickly (although without all the datasets).
 *     (The CWBrowsers started up slowly.)
 * <li> Datasets are made available in Erddap one-by-one as they are loaded 
 *     (not in batch mode).
 * <li> Loading datasets takes time, but is done in a separate thread
 *    so it never slows down requests for a dataset.
 * <li> Only one thread is used to load all the datasets, so loading
 *    datasets never becomes a drain of computer resources.
 * <li> The datasets.xml file is read anew each time this is run,
 *    so you can make changes to the file (e.g., add datasets or change metadata)
 *    and the results take effect without restarting Erddap.
 * </ul>
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2008-02-12
 */
public class LoadDatasets extends Thread {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 

    //*** things set by constructor 
    private Erddap erddap;
    private String datasetsRegex;
    private InputStream inputStream;
    private boolean majorLoad;
    private long lastLuceneUpdate = System.currentTimeMillis();

    private static long MAX_MILLIS_BEFORE_LUCENE_UPDATE = 5 * Calendar2.MILLIS_PER_MINUTE;
    private final static boolean ADD = true;
    private final static boolean REMOVE = false;

    /* This is set by run if there is an unexpected error. */   
    public String unexpectedError = ""; 

    /** 
     * This is a collection of all the exceptions from all the 
     * datasets that didn't load successfully and other warnings from LoadDatasets.
     * It will be length=0 if no warnings.
     */
    public StringBuilder warningsFromLoadDatasets = new StringBuilder();

    /** 
     * The constructor.
     *
     * @param erddap  Calling run() places results back in erddap as they become available
     * @param datasetsRegex  usually either EDStatic.datasetsRegex or a custom regex for flagged datasets.
     * @param inputStream with the datasets.xml information.
     *     There is no need to wrap this in a buffered InputStream -- that will be done here.
     *     If null, run() will make a copy of [EDStatic.contentDirectory]/datasets.xml
     *     and make an inputStream from the copy.
     * @param majorLoad if true, this does time-consuming garbage collection,
     *     logs memory usage information,
     *     and checks if Daily report should be sent.
     */
    public LoadDatasets(Erddap erddap, String datasetsRegex, InputStream inputStream,
            boolean majorLoad) {
        this.erddap = erddap;
        this.datasetsRegex = datasetsRegex;
        this.inputStream = inputStream;
        this.majorLoad = majorLoad;
        setName("LoadDatasets");
    }

    /**
     * This reads datasets[2].xml and loads all the datasets, 
     * placing results back in erddap as they become available.
     */
    public void run() {
        SimpleXMLReader xmlReader = null;
        StringArray changedDatasetIDs = new StringArray();
        try {
            String2.log("\n" + String2.makeString('*', 80) +  
                "\nLoadDatasets.run EDStatic.developmentMode=" + EDStatic.developmentMode + 
                " " + Calendar2.getCurrentISODateTimeStringLocalTZ() +
                "\n  datasetsRegex=" + datasetsRegex + 
                " inputStream=" + (inputStream == null? "null" : "something") + 
                " majorLoad=" + majorLoad);
            long memoryInUse = 0;
            if (majorLoad) {
                //gc so getMemoryInUse more accurate
                //don't use Math2.sleep which catches/ignores interrupt
                System.gc();  Thread.sleep(Math2.shortSleep); //before get memoryString
                System.gc();  Thread.sleep(Math2.shortSleep); //before get memoryString
                memoryInUse = Math2.getMemoryInUse();
                String2.log(Math2.memoryString() + " " + Math2.xmxMemoryString());
                //delete decompressed files if not used in last nMinutes (to keep cumulative size down)
                String2.log("After deleting decompressed files not used in the last " + 
                    EDStatic.decompressedCacheMaxMinutesOld + " minutes, nRemain=" +
                    File2.deleteIfOld(EDStatic.fullDecompressedDirectory, 
                         System.currentTimeMillis() - EDStatic.decompressedCacheMaxMinutesOld * Calendar2.MILLIS_PER_MINUTE, 
                         true, false)); //recursive, deleteEmptySubdirectories
            }
            long startTime = System.currentTimeMillis();
            int oldNGrid = erddap.gridDatasetHashMap.size();
            int oldNTable = erddap.tableDatasetHashMap.size();
            HashSet<String> orphanIDSet = null;
            if (majorLoad) {
                orphanIDSet = new HashSet(erddap.gridDatasetHashMap.keySet());
                orphanIDSet.addAll(       erddap.tableDatasetHashMap.keySet());
                orphanIDSet.remove(EDDTableFromAllDatasets.DATASET_ID);
            }
            HashMap tUserHashMap = new HashMap(); //no need for thread-safe, all puts are here (1 thread); future gets are thread safe
            StringBuilder datasetsThatFailedToLoadSB = new StringBuilder();
            HashSet datasetIDSet = new HashSet(); //to detect duplicates, just local use, no need for thread-safe
            StringArray duplicateDatasetIDs = new StringArray(); //list of duplicates
            EDStatic.suggestAddFillValueCSV.setLength(0);


            //ensure EDDTableFromAllDatasets exists
            //If something causes it to not exist, this will recreate it soon.
            try {
                if (!erddap.tableDatasetHashMap.containsKey(EDDTableFromAllDatasets.DATASET_ID))
                    erddap.tableDatasetHashMap.put(EDDTableFromAllDatasets.DATASET_ID,
                        new EDDTableFromAllDatasets(erddap.gridDatasetHashMap, erddap.tableDatasetHashMap));
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }

            //decision: how to process xml dataset description
            //  XPath - a standard, but sometimes slow and takes tons of memory
            //    Good: very concise code for parsing
            //    Severe problem: without making schemas, hard to catch/report mistyped tag names  
            //    Maybe I should be writing schemas...
            //  SimpleXMLReader - not a standard, but fast
            //    Good: easy to catch/report mistyped tag Names
            //    Low memory use.
            //I went with SimpleXMLReader

            if (inputStream == null) {
                //make a copy of datasets.xml so administrator can change file whenever desired
                //  and not affect simpleXMLReader
                String oldFileName = EDStatic.contentDirectory + 
                    "datasets" + (EDStatic.developmentMode? "2" : "") + ".xml";
                String newFileName = EDStatic.bigParentDirectory + "currentDatasets.xml";
                if (!File2.copy(oldFileName, newFileName)) 
                    throw new RuntimeException("Unable to copy " + oldFileName + " to " + newFileName);
                //not File2.getDecompressedBufferedInputStream(). Read file as is.
                inputStream = new FileInputStream(newFileName);  
            }

            //read datasets.xml
            inputStream = new BufferedInputStream(inputStream);
            xmlReader = new SimpleXMLReader(inputStream, "erddapDatasets");
            //there is an enclosing try/catch that handles with closing xmlReader
            String startError = "datasets.xml error on line #";
            int nDatasets = 0;
            int nTry = 0;
            while (true) {
                //check for interruption 
                if (isInterrupted()) { 
                    String2.log("*** The LoadDatasets thread was interrupted at " + 
                        Calendar2.getCurrentISODateTimeStringLocalTZ());
                    updateLucene(erddap, changedDatasetIDs);
                    return;
                }

                xmlReader.nextTag();
                String tags = xmlReader.allTags();
                if (tags.equals("</erddapDatasets>")) {
                    break;
                } else if (tags.equals("<erddapDatasets><dataset>")) {
                    //just load minimal datasets?
                    nDatasets++;
                    String tId = xmlReader.attributeValue("datasetID"); 
                    if (!String2.isSomething(tId))  //"" is trouble. It leads to flagDir being deleted below.
                        throw new RuntimeException(startError + xmlReader.lineNumber() + ": " +
                            "This <dataset> doesn't have a datasetID!");
                    if (majorLoad)
                        orphanIDSet.remove(tId);

                    //Looking for reasons to skip loading this dataset.
                    //Test first: skip dataset because it is a duplicate datasetID?  
                    //  If isDuplicate, act as if this doesn't even occur in datasets.xml.
                    //  This is imperfect. It just tests top-level datasets, 
                    //  not lower level, e.g., within EDDGridCopy.
                    boolean skip = false;
                    boolean isDuplicate = !datasetIDSet.add(tId); 
                    if (isDuplicate) { 
                        skip = true;
                        duplicateDatasetIDs.add(tId);
                        if (reallyVerbose) 
                            String2.log("*** skipping datasetID=" + tId + 
                                " because it's a duplicate.");
                    }
                    
                    //Test second: skip dataset because of datasetsRegex?
                    if (!skip && !tId.matches(datasetsRegex)) {
                        skip = true;
                        if (reallyVerbose) 
                            String2.log("*** skipping datasetID=" + tId + 
                                " because of datasetsRegex.");
                    }

                    //Test third: look at flag/age  or active=false
                    if (!skip) {
                        //always check both flag locations
                        boolean isFlagged     = File2.delete(EDStatic.fullResetFlagDirectory + tId);
                        boolean isHardFlagged = File2.delete(EDStatic.fullHardFlagDirectory  + tId);
                        if (isFlagged) {
                            String2.log("*** reloading datasetID=" + tId + 
                                " because it was in the flag directory.");
                        } else if (isHardFlagged) {
                            String2.log("*** reloading datasetID=" + tId + 
                                " because it was in the hardFlag directory.");
                            EDD oldEdd = erddap.gridDatasetHashMap.get(tId);
                            if (oldEdd == null)
                                oldEdd = erddap.tableDatasetHashMap.get(tId);
                            if (oldEdd != null) {
                                StringArray childDatasetIDs = oldEdd.childDatasetIDs();
                                for (int cd = 0; cd < childDatasetIDs.size(); cd++) {
                                    String cid = childDatasetIDs.get(cd);
                                    EDD.deleteCachedDatasetInfo(cid); //delete the children's info
                                    FileVisitorDNLS.pruneCache(EDD.decompressedDirectory(cid), 2, 0.5); //remove as many files as possible
                                }
                            }
                            tryToUnload(erddap, tId, new StringArray(), true); //needToUpdateLucene
                            EDD.deleteCachedDatasetInfo(tId); //the important difference
                            FileVisitorDNLS.pruneCache(EDD.decompressedDirectory(tId), 2, 0.5); //remove as many files as possible

                        } else {
                            //does the dataset already exist and is young?
                            EDD oldEdd = erddap.gridDatasetHashMap.get(tId);
                            if (oldEdd == null)
                                oldEdd = erddap.tableDatasetHashMap.get(tId);
                            if (oldEdd != null) {
                                long minutesOld = oldEdd.creationTimeMillis() <= 0?  //see edd.setCreationTimeTo0
                                    Long.MAX_VALUE :
                                    (System.currentTimeMillis() - oldEdd.creationTimeMillis()) / 60000; 
                                if (minutesOld < oldEdd.getReloadEveryNMinutes()) {
                                    //it exists and is young
                                    if (reallyVerbose) String2.log("*** skipping datasetID=" + tId + 
                                        ": it already exists and minutesOld=" + minutesOld +
                                        " is less than reloadEvery=" + oldEdd.getReloadEveryNMinutes());
                                    skip = true;
                                }
                            }
                        }

                        //active="false"?  (very powerful)
                        String tActiveString = xmlReader.attributeValue("active"); 
                        boolean tActive = tActiveString != null && tActiveString.equals("false")? false : true; 
                        if (!tActive) {
                            //marked not active now; was it active?
                            boolean needToUpdateLucene = 
                                System.currentTimeMillis() - lastLuceneUpdate > MAX_MILLIS_BEFORE_LUCENE_UPDATE;
                            if (tryToUnload(erddap, tId, changedDatasetIDs, needToUpdateLucene)) {
                                //yes, it was unloaded
                                String2.log("*** unloaded datasetID=" + tId + " because active=\"false\"."); 
                                if (needToUpdateLucene)
                                    lastLuceneUpdate = System.currentTimeMillis(); //because Lucene was updated
                            }

                            skip = true;
                        }
                    }

//To test just EDDTable datasets...
//if (xmlReader.attributeValue("type").startsWith("EDDGrid") &&
//    !tId.startsWith("etopo"))
//    skip = true;

                    if (skip) {
                        //skip over the tags for this dataset
                        while (!tags.equals("<erddapDatasets></dataset>")) {
                            xmlReader.nextTag();
                            tags = xmlReader.allTags();
                        }
                    } else {
                        //try to load this dataset
                        nTry++;
                        String change = "";
                        EDD dataset = null, oldDataset = null;
                        boolean oldCatInfoRemoved = false;
                        long timeToLoadThisDataset = System.currentTimeMillis();
                        try {
                            dataset = EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);

                            //check for interruption right before making changes to Erddap
                            if (isInterrupted()) { //this is a likely place to catch interruption
                                String2.log("*** The LoadDatasets thread was interrupted at " + 
                                    Calendar2.getCurrentISODateTimeStringLocalTZ());
                                updateLucene(erddap, changedDatasetIDs);
                                lastLuceneUpdate = System.currentTimeMillis();
                                return;
                            }

                            //do several things in quick succession...
                            //(??? synchronize on (?) if really need avoid inconsistency)

                            //was there a dataset with the same datasetID?
                            oldDataset = erddap.gridDatasetHashMap.get(tId);
                            if (oldDataset == null)
                                oldDataset = erddap.tableDatasetHashMap.get(tId);

                            //if oldDataset existed, remove its info from categoryInfo
                            //(check now, before put dataset in place, in case EDDGrid <--> EDDTable)
                            if (oldDataset != null) {
                                addRemoveDatasetInfo(REMOVE, erddap.categoryInfo, oldDataset); 
                                oldCatInfoRemoved = true;
                            }

                            //put dataset in place
                            //(hashMap.put atomically replaces old version with new)
                            if ((oldDataset == null || oldDataset instanceof EDDGrid) &&
                                                          dataset instanceof EDDGrid) {
                                erddap.gridDatasetHashMap.put(tId, (EDDGrid)dataset);  //was/is grid

                            } else if ((oldDataset == null || oldDataset instanceof EDDTable) &&
                                                                 dataset instanceof EDDTable) {
                                erddap.tableDatasetHashMap.put(tId, (EDDTable)dataset); //was/is table 

                            } else if (dataset instanceof EDDGrid) {
                                if (oldDataset != null)
                                    erddap.tableDatasetHashMap.remove(tId);   //was table
                                erddap.gridDatasetHashMap.put(tId, (EDDGrid)dataset);  //now grid

                            } else if (dataset instanceof EDDTable) {
                                if (oldDataset != null)
                                    erddap.gridDatasetHashMap.remove(tId);    //was grid
                                erddap.tableDatasetHashMap.put(tId, (EDDTable)dataset); //now table
                            }

                            //add new info to categoryInfo
                            addRemoveDatasetInfo(ADD, erddap.categoryInfo, dataset); 

                            //clear the dataset's cache 
                            //since axis values may have changed and "last" may have changed
                            File2.deleteAllFiles(dataset.cacheDirectory());                           
                       
                            change = dataset.changed(oldDataset);
                            if (change.length() == 0 && dataset instanceof EDDTable)
                                change = "The dataset was reloaded.";

                        } catch (Throwable t) {
                            dataset = null;
                            timeToLoadThisDataset = System.currentTimeMillis() - timeToLoadThisDataset;

                            //check for interruption right before making changes to Erddap
                            if (isInterrupted()) { //this is a likely place to catch interruption
                                String tError2 = "*** The LoadDatasets thread was interrupted at " + 
                                    Calendar2.getCurrentISODateTimeStringLocalTZ();
                                String2.log(tError2);
                                warningsFromLoadDatasets.append(tError2 + "\n\n");
                                updateLucene(erddap, changedDatasetIDs);
                                lastLuceneUpdate = System.currentTimeMillis();
                                return;
                            }


                            //actually remove old dataset (if any existed)
                            EDD tDataset = erddap.gridDatasetHashMap.remove(tId); //always ensure it was removed
                            if (tDataset == null)
                                tDataset = erddap.tableDatasetHashMap.remove(tId);
                            if (oldDataset == null)
                                oldDataset = tDataset;

                            //if oldDataset existed, remove it from categoryInfo
                            if (oldDataset != null && !oldCatInfoRemoved)
                                addRemoveDatasetInfo(REMOVE, erddap.categoryInfo, oldDataset); 

                            String tError = startError + xmlReader.lineNumber() + "\n" + 
                                "While trying to load datasetID=" + tId + " (after " +
                                    timeToLoadThisDataset + " ms)\n" +
                                MustBe.throwableToString(t);
                            String2.log(tError);
                            warningsFromLoadDatasets.append(tError + "\n\n");
                            datasetsThatFailedToLoadSB.append(tId + ", ");

                            //stop???
                            if (!xmlReader.isOpen()) { //error was really serious
                                throw new RuntimeException(startError + xmlReader.lineNumber() + 
                                    ": " + t.toString(), t);
                            }
           
                            //skip over the remaining tags for this dataset
                            try {
                                while (!xmlReader.allTags().equals("<erddapDatasets></dataset>")) 
                                    xmlReader.nextTag();
                            } catch (Throwable t2) {
                                throw new RuntimeException(startError + xmlReader.lineNumber() + 
                                    ": " + t2.toString(), t2);
                            }

                            //change      (if oldDataset=null and new one failed to load, no change)
                            if (oldDataset != null)  
                                change = tError;
                        }
                        if (verbose) String2.log("change=" + change);

                        //whether succeeded (new or swapped in) or failed (removed), it was changed
                        changedDatasetIDs.add(tId);
                        if (System.currentTimeMillis() - lastLuceneUpdate >
                            MAX_MILLIS_BEFORE_LUCENE_UPDATE) {
                            updateLucene(erddap, changedDatasetIDs);
                            lastLuceneUpdate = System.currentTimeMillis();
                        }

                        //trigger subscription and dataset.onChange actions (after new dataset is in place)
                        EDD cooDataset = dataset == null? oldDataset : dataset; //currentOrOld, may be null
                        tryToDoActions(erddap, tId, cooDataset, 
                            startError + xmlReader.lineNumber() + " with Subscriptions",
                            change);
                    }

                } else if (tags.equals("<erddapDatasets><angularDegreeUnits>")) {
                } else if (tags.equals("<erddapDatasets></angularDegreeUnits>")) {
                    String ts = xmlReader.content();
                    if (!String2.isSomething(ts))
                        ts = EDStatic.DEFAULT_ANGULAR_DEGREE_UNITS;
                    EDStatic.angularDegreeUnitsSet =
                        new HashSet<String>(String2.toArrayList(StringArray.fromCSV(ts).toArray())); //so canonical
                    String2.log("angularDegreeUnits=" + String2.toCSVString(EDStatic.angularDegreeUnitsSet));

                } else if (tags.equals("<erddapDatasets><angularDegreeTrueUnits>")) {
                } else if (tags.equals("<erddapDatasets></angularDegreeTrueUnits>")) {
                    String ts = xmlReader.content();
                    if (!String2.isSomething(ts))
                        ts = EDStatic.DEFAULT_ANGULAR_DEGREE_TRUE_UNITS;
                    EDStatic.angularDegreeTrueUnitsSet =
                        new HashSet<String>(String2.toArrayList(StringArray.fromCSV(ts).toArray())); //so canonical
                    String2.log("angularDegreeTrueUnits=" + String2.toCSVString(EDStatic.angularDegreeTrueUnitsSet));

                } else if (tags.equals("<erddapDatasets><cacheMinutes>")) {
                } else if (tags.equals("<erddapDatasets></cacheMinutes>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.cacheMillis = (tnt < 1 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_cacheMinutes : tnt) * Calendar2.MILLIS_PER_MINUTE; 
                    String2.log("cacheMinutes=" + EDStatic.cacheMillis/Calendar2.MILLIS_PER_MINUTE);

                } else if (tags.equals("<erddapDatasets><commonStandardNames>")) {
                } else if (tags.equals("<erddapDatasets></commonStandardNames>")) {
                    String ts = xmlReader.content();
                    EDStatic.commonStandardNames = String2.isSomething(ts)?
                        String2.canonical(StringArray.arrayFromCSV(ts)) :
                        EDStatic.DEFAULT_commonStandardNames;
                    String2.log("commonStandardNames=" + String2.toCSSVString(EDStatic.commonStandardNames));

                } else if (tags.equals("<erddapDatasets><convertToPublicSourceUrl>")) {
                    String tFrom = xmlReader.attributeValue("from");
                    String tTo   = xmlReader.attributeValue("to");
                    int spo = EDStatic.convertToPublicSourceUrlFromSlashPo(tFrom);
                    if (tFrom != null && tFrom.length() > 3 && spo == tFrom.length() - 1 && tTo != null) 
                        EDStatic.convertToPublicSourceUrl.put(tFrom, tTo);                        
                } else if (tags.equals("<erddapDatasets></convertToPublicSourceUrl>")) {

                } else if (tags.equals("<erddapDatasets><decompressedCacheMaxGB>")) {
                } else if (tags.equals("<erddapDatasets></decompressedCacheMaxGB>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.decompressedCacheMaxGB = tnt < 1 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_decompressedCacheMaxGB : tnt; 
                    String2.log("decompressedCacheMaxGB=" + EDStatic.decompressedCacheMaxGB);

                } else if (tags.equals("<erddapDatasets><decompressedCacheMaxMinutesOld>")) {
                } else if (tags.equals("<erddapDatasets></decompressedCacheMaxMinutesOld>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.decompressedCacheMaxMinutesOld = tnt < 1 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_decompressedCacheMaxMinutesOld : tnt; 
                    String2.log("decompressedCacheMaxMinutesOld=" + EDStatic.decompressedCacheMaxMinutesOld);

                } else if (tags.equals("<erddapDatasets><drawLandMask>")) {
                } else if (tags.equals("<erddapDatasets></drawLandMask>")) {
                    String ts = xmlReader.content();
                    int tnt = String2.indexOf(SgtMap.drawLandMask_OPTIONS, ts);
                    EDStatic.drawLandMask = tnt < 1? EDStatic.DEFAULT_drawLandMask : SgtMap.drawLandMask_OPTIONS[tnt]; 
                    String2.log("drawLandMask=" + EDStatic.drawLandMask);

                } else if (tags.equals("<erddapDatasets><graphBackgroundColor>")) {
                } else if (tags.equals("<erddapDatasets></graphBackgroundColor>")) {
                    String ts = xmlReader.content();
                    int tnt = String2.isSomething(ts)? String2.parseInt(ts) : EDStatic.DEFAULT_graphBackgroundColorInt;
                    EDStatic.graphBackgroundColor = new Color(tnt, true); //hasAlpha
                    String2.log("graphBackgroundColor=" + String2.to0xHexString(tnt, 8));

                } else if (tags.equals("<erddapDatasets><loadDatasetsMinMinutes>")) {
                } else if (tags.equals("<erddapDatasets></loadDatasetsMinMinutes>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.loadDatasetsMinMillis = (tnt < 1 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_loadDatasetsMinMinutes : tnt) * Calendar2.MILLIS_PER_MINUTE; 
                    String2.log("loadDatasetsMinMinutes=" + EDStatic.loadDatasetsMinMillis/Calendar2.MILLIS_PER_MINUTE);

                } else if (tags.equals("<erddapDatasets><loadDatasetsMaxMinutes>")) {
                } else if (tags.equals("<erddapDatasets></loadDatasetsMaxMinutes>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.loadDatasetsMaxMillis = (tnt < 1 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_loadDatasetsMaxMinutes : tnt) * Calendar2.MILLIS_PER_MINUTE; 
                    String2.log("loadDatasetsMaxMinutes=" + EDStatic.loadDatasetsMaxMillis/Calendar2.MILLIS_PER_MINUTE);

                } else if (tags.equals("<erddapDatasets><logLevel>")) {
                } else if (tags.equals("<erddapDatasets></logLevel>")) {                    
                    EDStatic.setLogLevel(xmlReader.content()); //""->"info".  It prints diagnostic to log.txt.

                } else if (tags.equals("<erddapDatasets><nGridThreads>")) {
                } else if (tags.equals("<erddapDatasets></nGridThreads>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.nGridThreads = tnt < 1 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_nGridThreads : tnt; 
                    String2.log("nGridThreads=" + EDStatic.nGridThreads);

                } else if (tags.equals("<erddapDatasets><nTableThreads>")) {
                } else if (tags.equals("<erddapDatasets></nTableThreads>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.nTableThreads = tnt < 1 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_nTableThreads : tnt; 
                    String2.log("nTableThreads=" + EDStatic.nTableThreads);

                } else if (tags.equals("<erddapDatasets><partialRequestMaxBytes>")) {
                } else if (tags.equals("<erddapDatasets></partialRequestMaxBytes>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.partialRequestMaxBytes = tnt < 1000000 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_partialRequestMaxBytes : tnt; 
                    String2.log("partialRequestMaxBytes=" + EDStatic.partialRequestMaxBytes);

                } else if (tags.equals("<erddapDatasets><partialRequestMaxCells>")) {
                } else if (tags.equals("<erddapDatasets></partialRequestMaxCells>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.partialRequestMaxCells = tnt < 1000 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_partialRequestMaxCells : tnt; 
                    String2.log("partialRequestMaxCells=" + EDStatic.partialRequestMaxCells);

                } else if (tags.equals("<erddapDatasets><requestBlacklist>")) {
                } else if (tags.equals("<erddapDatasets></requestBlacklist>")) {
                    EDStatic.setRequestBlacklist(xmlReader.content());

                } else if (tags.equals("<erddapDatasets><slowDownTroubleMillis>")) {
                } else if (tags.equals("<erddapDatasets></slowDownTroubleMillis>")) {
                    int tms = String2.parseInt(xmlReader.content());
                    EDStatic.slowDownTroubleMillis = tms < 0 || tms > 1000000? 1000 : tms; 
                    String2.log("slowDownTroubleMillis=" + EDStatic.slowDownTroubleMillis);

                } else if (tags.equals("<erddapDatasets><subscriptionEmailBlacklist>")) {
                } else if (tags.equals("<erddapDatasets></subscriptionEmailBlacklist>")) {
                    if (EDStatic.subscriptionSystemActive)
                        EDStatic.subscriptions.setEmailBlacklist(xmlReader.content());

                } else if (tags.equals("<erddapDatasets><standardLicense>")) {
                } else if (tags.equals("<erddapDatasets></standardLicense>")) {
                    String ts = xmlReader.content();
                    EDStatic.standardLicense = String2.isSomething(ts)? ts : 
                        EDStatic.DEFAULT_standardLicense;
                    String2.log("standardLicense was set.");

                } else if (tags.equals("<erddapDatasets><standardContact>")) {
                } else if (tags.equals("<erddapDatasets></standardContact>")) {
                    String ts = xmlReader.content();
                    ts = String2.isSomething(ts)? ts : EDStatic.DEFAULT_standardContact;
                    ts = String2.replaceAll(ts, "&adminEmail;", SSR.getSafeEmailAddress(EDStatic.adminEmail));
                    EDStatic.standardContact = ts; //swap into place
                    String2.log("standardContact was set.");

                } else if (tags.equals("<erddapDatasets><standardDataLicenses>")) {
                } else if (tags.equals("<erddapDatasets></standardDataLicenses>")) {
                    String ts = xmlReader.content();
                    EDStatic.standardDataLicenses = String2.isSomething(ts)? ts : 
                        EDStatic.DEFAULT_standardDataLicenses;
                    String2.log("standardDataLicenses was set.");

                } else if (tags.equals("<erddapDatasets><standardDisclaimerOfEndorsement>")) {
                } else if (tags.equals("<erddapDatasets></standardDisclaimerOfEndorsement>")) {
                    String ts = xmlReader.content();
                    EDStatic.standardDisclaimerOfEndorsement = String2.isSomething(ts)? ts : 
                        EDStatic.DEFAULT_standardDisclaimerOfEndorsement;
                    String2.log("standardDisclaimerOfEndorsement was set.");

                } else if (tags.equals("<erddapDatasets><standardDisclaimerOfExternalLinks>")) {
                } else if (tags.equals("<erddapDatasets></standardDisclaimerOfExternalLinks>")) {
                    String ts = xmlReader.content();
                    EDStatic.standardDisclaimerOfExternalLinks = String2.isSomething(ts)? ts : 
                        EDStatic.DEFAULT_standardDisclaimerOfExternalLinks;
                    String2.log("standardDisclaimerOfExternalLinks was set.");

                } else if (tags.equals("<erddapDatasets><standardGeneralDisclaimer>")) {
                } else if (tags.equals("<erddapDatasets></standardGeneralDisclaimer>")) {
                    String ts = xmlReader.content();
                    EDStatic.standardGeneralDisclaimer = String2.isSomething(ts)? ts : 
                        EDStatic.DEFAULT_standardGeneralDisclaimer;
                    String2.log("standardGeneralDisclaimer was set.");

                } else if (tags.equals("<erddapDatasets><standardPrivacyPolicy>")) {
                } else if (tags.equals("<erddapDatasets></standardPrivacyPolicy>")) {
                    String ts = xmlReader.content();
                    EDStatic.standardPrivacyPolicy = String2.isSomething(ts)? ts : 
                        EDStatic.DEFAULT_standardPrivacyPolicy;
                    String2.log("standardPrivacyPolicy was set.");

                } else if (tags.equals("<erddapDatasets><startHeadHtml5>")) {
                } else if (tags.equals("<erddapDatasets></startHeadHtml5>")) {
                    String ts = xmlReader.content();
                    ts = String2.isSomething(ts)? ts : EDStatic.DEFAULT_startHeadHtml;
                    if (!ts.startsWith("<!DOCTYPE html>")) {
                        String2.log(String2.ERROR + " in datasets.xml: <startHeadHtml> must start with \"<!DOCTYPE html>\". Using default <startHeadHtml> instead.");
                        ts = EDStatic.DEFAULT_startHeadHtml;
                    }
                    EDStatic.startHeadHtml = ts; //swap into place
                    String2.log("startHeadHtml5 was set.");

                } else if (tags.equals("<erddapDatasets><startBodyHtml5>")) {
                } else if (tags.equals("<erddapDatasets></startBodyHtml5>")) {
                    String ts = xmlReader.content();
                    ts = String2.isSomething(ts)? ts : EDStatic.DEFAULT_startBodyHtml;
                    EDStatic.ampLoginInfoPo = ts.indexOf(EDStatic.ampLoginInfo); //may be -1
                    EDStatic.startBodyHtml = ts; //swap into place
                    String2.log("startBodyHtml5 was set.");

                } else if (tags.equals("<erddapDatasets><theShortDescriptionHtml>")) {
                } else if (tags.equals("<erddapDatasets></theShortDescriptionHtml>")) {
                    String ts = xmlReader.content();
                    ts = String2.isSomething(ts)? ts : EDStatic.DEFAULT_theShortDescriptionHtml;
                    ts = String2.replaceAll(ts, "[standardShortDescriptionHtml]", EDStatic.standardShortDescriptionHtml);
                    ts = String2.replaceAll(ts, "&resultsFormatExamplesHtml;",    EDStatic.resultsFormatExamplesHtml);
                    EDStatic.theShortDescriptionHtml = ts; //swap into place
                    String2.log("theShortDescriptionHtml was set.");

                } else if (tags.equals("<erddapDatasets><endBodyHtml5>")) {
                } else if (tags.equals("<erddapDatasets></endBodyHtml5>")) {
                    String ts = xmlReader.content();
                    EDStatic.endBodyHtml = String2.replaceAll(
                        String2.isSomething(ts)? ts : EDStatic.DEFAULT_endBodyHtml,
                        "&erddapVersion;", EDStatic.erddapVersion);
                    String2.log("endBodyHtml5 was set.");

                } else if (tags.equals("<erddapDatasets><convertInterpolateRequestCSVExample>")) {
                } else if (tags.equals("<erddapDatasets></convertInterpolateRequestCSVExample>")) {
                    EDStatic.convertInterpolateRequestCSVExample = xmlReader.content();
                    String2.log("convertInterpolateRequestCSVExample=" + xmlReader.content());

                } else if (tags.equals("<erddapDatasets><convertInterpolateDatasetIDVariableList>")) {
                } else if (tags.equals("<erddapDatasets></convertInterpolateDatasetIDVariableList>")) {
                    String sar[] = StringArray.arrayFromCSV(xmlReader.content());
                    EDStatic.convertInterpolateDatasetIDVariableList = sar;
                    String2.log("convertInterpolateDatasetIDVariableList=" + String2.toCSVString(sar));

                } else if (tags.equals("<erddapDatasets><unusualActivity>")) {
                } else if (tags.equals("<erddapDatasets></unusualActivity>")) {
                    int tnt = String2.parseInt(xmlReader.content());
                    EDStatic.unusualActivity = tnt < 1 || tnt == Integer.MAX_VALUE? 
                        EDStatic.DEFAULT_unusualActivity : tnt; 
                    String2.log("unusualActivity=" + EDStatic.unusualActivity);

                //<user username="bsimons" password="..." roles="admin, role1" />
                //this mimics tomcat syntax
                } else if (tags.equals("<erddapDatasets><user>")) { 
                    String tUsername = xmlReader.attributeValue("username");
                    String tPassword  = xmlReader.attributeValue("password");  
                    if (tUsername != null) 
                        tUsername = tUsername.trim();
                    if (tPassword != null) 
                        tPassword = tPassword.trim().toLowerCase(); //match Digest Authentication standard case
                    String ttRoles = xmlReader.attributeValue("roles");
                    String tRoles[] = StringArray.arrayFromCSV(
                        (ttRoles == null? "" : ttRoles + ",") + EDStatic.anyoneLoggedIn, 
                        ",", true, false); //splitChars, trim, keepNothing. Result may be String[0].

                    //is username nothing?
                    if (!String2.isSomething(tUsername)) {
                        warningsFromLoadDatasets.append(
                            "datasets.xml error: A <user> tag in datasets.xml had no username=\"someName\" attribute.\n\n");

                    //is username reserved?
                    } else if (EDStatic.loggedInAsHttps.equals(tUsername) ||
                               EDStatic.anyoneLoggedIn.equals(tUsername)  ||
                               EDStatic.loggedInAsSuperuser.equals(tUsername)) { //shouldn't be possible because \t would be trimmed above, but double check
                        warningsFromLoadDatasets.append(
                            "datasets.xml error: <user> username=\"" + 
                            String2.annotatedString(tUsername) + "\" is a reserved username.\n\n");

                    //is username invalid?
                    } else if (!String2.isPrintable(tUsername)) {
                        warningsFromLoadDatasets.append(
                            "datasets.xml error: <user> username=\"" + 
                            String2.annotatedString(tUsername) + "\" has invalid characters.\n\n");

                    //is password invalid?
                    } else if (EDStatic.authentication.equals("custom") &&   //others in future
                        !String2.isHexString(tPassword)) {
                        warningsFromLoadDatasets.append(
                            "datasets.xml error: The password for <user> username=" + tUsername + 
                            " in datasets.xml isn't a hexadecimal string.\n\n");

                    //a role is not allowed?
                    } else if (String2.indexOf(tRoles, EDStatic.loggedInAsSuperuser) >= 0) { //not possible because \t would be trimmed, but be doubly sure
                        warningsFromLoadDatasets.append(
                            "datasets.xml error: For <user> username=" + tUsername + 
                            ", the superuser role isn't allowed for any user.\n\n");

                    //add user info to tUserHashMap
                    } else {                            
                        Arrays.sort(tRoles);
                        if ("email".equals(EDStatic.authentication) ||
                            "google".equals(EDStatic.authentication))
                            tUsername = tUsername.toLowerCase();  //so case insensitive, to avoid trouble
                        if (reallyVerbose) String2.log("user=" + tUsername + " roles=" + String2.toCSSVString(tRoles));
                        Object o = tUserHashMap.put(tUsername, new Object[]{tPassword, tRoles});
                        if (o != null)
                            warningsFromLoadDatasets.append(
                                "datasets.xml error: There are two <user> tags in datasets.xml with username=" + 
                                tUsername + "\nChange one of them.\n\n");
                    }

                } else if (tags.equals("<erddapDatasets></user>")) { //do nothing

                } else {
                    xmlReader.unexpectedTagException();
                }
            }
            xmlReader.close();
            xmlReader = null;

            updateLucene(erddap, changedDatasetIDs);
            lastLuceneUpdate = System.currentTimeMillis();

            //atomic swap into place
            EDStatic.setUserHashMap(tUserHashMap); 
            //datasetsThatFailedToLoad only swapped into place if majorLoad (see below)
            datasetsThatFailedToLoadSB = String2.noLongLinesAtSpace(
                datasetsThatFailedToLoadSB, 100, "    ");
            String dtftl = datasetsThatFailedToLoadSB.toString();
            int ndf = String2.countAll(dtftl, ","); //all have ',' at end (even if just 1)
            String datasetsThatFailedToLoad =
                "n Datasets Failed To Load (in the last " + 
                (majorLoad? "major" : "minor") + " LoadDatasets) = " + ndf + "\n" +
                (datasetsThatFailedToLoadSB.length() == 0? "" :
                    "    " + dtftl + "(end)\n");
            String errorsDuringMajorReload = majorLoad && duplicateDatasetIDs.size() > 0?         
                String2.ERROR + ": Duplicate datasetIDs in datasets.xml:\n    " +
                    String2.noLongLinesAtSpace(duplicateDatasetIDs.toString(), 100, "    ") + "\n" :
                "";
            if (majorLoad && orphanIDSet.size() > 0) 
                errorsDuringMajorReload +=
                    String2.ERROR + ": n Orphan Datasets (datasets in ERDDAP but not in datasets.xml) = " + orphanIDSet.size() + "\n" +
                    "    " + 
                    String2.noLongLinesAtSpace(String2.toCSSVString(orphanIDSet), 100, "    ") + 
                    "(end)\n";

            EDStatic.nGridDatasets = erddap.gridDatasetHashMap.size();
            EDStatic.nTableDatasets = erddap.tableDatasetHashMap.size();

            //*** print lots of useful information
            long loadDatasetsTime = System.currentTimeMillis() - startTime;
            String cDateTimeLocal = Calendar2.getCurrentISODateTimeStringLocalTZ();
            String2.log("\n" + String2.makeString('*', 80) + "\n" + 
                "LoadDatasets.run finished at " + cDateTimeLocal + 
                "  TOTAL TIME=" + loadDatasetsTime + "ms\n" +
                "  nGridDatasets active=" + EDStatic.nGridDatasets + 
                    " change=" + (EDStatic.nGridDatasets - oldNGrid) + "\n" +
                "  nTableDatasets active=" + EDStatic.nTableDatasets + 
                    " change=" + (EDStatic.nTableDatasets - oldNTable) + "\n" +
                "  nDatasets in datasets.xml=" + nDatasets + " (nTry=" + nTry + ")\n" +
                "  nUsers=" + tUserHashMap.size());

            //minorLoad?
            if (!majorLoad) {
                String2.distribute(loadDatasetsTime, EDStatic.minorLoadDatasetsDistribution24);
                String2.distribute(loadDatasetsTime, EDStatic.minorLoadDatasetsDistributionTotal);
                String2.log(datasetsThatFailedToLoad);
            }

            //majorLoad?
            if (majorLoad) {
                String2.distribute(loadDatasetsTime, EDStatic.majorLoadDatasetsDistribution24);
                String2.distribute(loadDatasetsTime, EDStatic.majorLoadDatasetsDistributionTotal);
                //gc so getMemoryInUse more accurate
                //don't use Math2.sleep which catches/ignores interrupt
                System.gc();  Thread.sleep(Math2.shortSleep); //aggressive, before get memoryString()
                System.gc();  Thread.sleep(Math2.shortSleep); //aggressive, before get memoryString()
                String memoryString = Math2.memoryString();
                long using = Math2.getMemoryInUse();
                long maxUsingMemory = Math.max(Math2.maxUsingMemory, using); 

                int nResponseSucceeded      = String2.getDistributionN(
                    EDStatic.responseTimesDistributionLoadDatasets);
                int medianResponseSucceeded = Math.max(0, String2.getDistributionMedian(
                    EDStatic.responseTimesDistributionLoadDatasets, nResponseSucceeded));

                int nResponseFailed      = String2.getDistributionN(
                    EDStatic.failureTimesDistributionLoadDatasets);
                int medianResponseFailed = Math.max(0, String2.getDistributionMedian(
                    EDStatic.failureTimesDistributionLoadDatasets, nResponseFailed));

                //get thread info
                String threadList = MustBe.allStackTraces(true, true);
                String threadSummary = null;
                String threadCounts = String2.right("", 22);
                int po = threadList.indexOf('\n');
                if (po > 0) {
                    //e.g., "Number of threads: Tomcat-waiting=9, inotify=1, other=22"
                    threadSummary = threadList.substring(0, po);

                    Pattern p = Pattern.compile(".*waiting=(\\d+), inotify=(\\d+), other=(\\d+).*");
                    Matcher m = p.matcher(threadSummary);
                    if (m.matches()) {
                        threadCounts = String2.right(m.group(1), 8) +
                                       String2.right(m.group(2), 8) +
                                       String2.right(m.group(3), 6);
                        //System.out.println("**** REGEX MATCHED! " + threadCounts);
                    } else {
                        //System.out.println("**** REGEX NOT MATCHED!");
                    }

                }

                String2.log(
                    "  " + memoryString + " " + Math2.xmxMemoryString() +
                    "\n  change for this run of major Load Datasets (MB) = " + ((Math2.getMemoryInUse() - memoryInUse) / Math2.BytesPerMB) + "\n");

                if (EDStatic.initialLoadDatasets()) {
                    if (EDStatic.suggestAddFillValueCSV.length() > 0) {
                        String tFileName = EDStatic.fullLogsDirectory + 
                                "addFillValueAttributes" + Calendar2.getCompactCurrentISODateTimeStringLocal() + ".csv";
                        String contents = 
                            "datasetID,variableSourceName,attribute\n" + 
                            EDStatic.suggestAddFillValueCSV.toString();
                        String2.writeToFile(tFileName, contents);
                        String afva =
                            "ADD _FillValue ATTRIBUTES?\n" +
                            "The datasets/variables in the table below have integer source data, but no\n" +
                            "_FillValue or missing_value attribute. We recommend adding the suggested\n" +
                            "attributes to the variable's <addAttributes> in datasets.xml to identify\n" +
                            "the default _FillValue used by ERDDAP. You can do this by hand or with the\n" +
                            "addFillValueAttributes option in GenerateDatasetsXml.\n" +
                            "If you don't make these changes, ERDDAP will treat those values (e.g., 127\n" +
                            "for byte variables), if any, as valid data values (for example, on graphs\n" +
                            "and when calculating statistics).\n" +
                            "Or, if you decide a variable should not have a _FillValue attribute, you can add\n" +
                            "  <att name=\"_FillValue\">null</att>\n" +
                            "instead, which will suppress this message for that datasetID+variable\n" +
                            "combination in the future.\n" +
                            "The list below is created each time you start up ERDDAP.\n" +
                            "The list was just written to a CSV file\n" + 
                            tFileName + "\n" +
                            "and is shown here:\n" + 
                            contents + "\n";
                        String2.log("\n" + afva);
                        EDStatic.email(
                            String2.ifSomethingConcat(EDStatic.emailEverythingToCsv, ",", EDStatic.emailDailyReportToCsv), 
                            "ADD _FillValue ATTRIBUTES?", //this exact string is in setupDatasetsXml.html
                            afva);

                    } else {
                        String2.log(
                            "ADD _FillValue ATTRIBUTES?  There are none to report.\n");
                    }
                }

                EDStatic.datasetsThatFailedToLoad = datasetsThatFailedToLoad; //swap into place
                EDStatic.errorsDuringMajorReload  = errorsDuringMajorReload;  //swap into place
                EDStatic.majorLoadDatasetsTimeSeriesSB.insert(0,   //header in EDStatic
//"Major LoadDatasets Time Series: MLD    Datasets Loaded        Requests (medianTime in seconds)         Number of Threads      Memory (MB)\n" +
//"  timestamp                    time   nTry nFail nTotal  nSuccess (median) nFailed (median) memFail  tomWait inotify other  inUse highWater\n");
                    "  " + cDateTimeLocal +  
                    String2.right("" + (loadDatasetsTime/1000 + 1), 7) + "s" + //time
                    String2.right("" + nTry, 7) + 
                    String2.right("" + ndf, 6) + 
                    String2.right("" + (EDStatic.nGridDatasets + EDStatic.nTableDatasets), 7) + //nTotal
                    String2.right("" + nResponseSucceeded, 10) + " (" +
                    String2.right("" + Math.min(999999, medianResponseSucceeded), 6) + ")" +
                    String2.right("" + nResponseFailed, 8) + " (" +
                    String2.right("" + Math.min(999999, medianResponseFailed), 6) + ")" +
                    String2.right("" + Math.min(99999999, EDStatic.dangerousMemoryFailures), 8) + 
                    threadCounts + 
                    String2.right("" + using/Math2.BytesPerMB, 7) + //memory using
                    String2.right("" + maxUsingMemory/Math2.BytesPerMB, 10) + //highWater
                    "\n");
                
                //reset
                EDStatic.dangerousMemoryFailures = 0;

                //email daily report?
                GregorianCalendar reportCalendar = Calendar2.newGCalendarLocal();
                String reportDate = Calendar2.formatAsISODate(reportCalendar);
                int hour = reportCalendar.get(Calendar2.HOUR_OF_DAY);
                //if (true) {  //uncomment to test daily report 

                if (!reportDate.equals(erddap.lastReportDate) && hour >= 7) {
                    //major reload after 7 of new day, so do daily report!  

                    erddap.lastReportDate = reportDate;
                    String stars = String2.makeString('*', 70);
                    String subject = "Daily Report";
                    StringBuilder contentSB = new StringBuilder(subject + "\n\n");
                    EDStatic.addIntroStatistics(contentSB);

                    //append number of active threads
                    if (threadSummary != null)
                        contentSB.append(threadSummary + "\n");

                    contentSB.append(Math2.memoryString() + " " + Math2.xmxMemoryString() + "\n\n");
                    contentSB.append(stars + "\nTallied Usage Information\n\n");
                    contentSB.append(EDStatic.tally.toString(50));
                    EDStatic.addCommonStatistics(contentSB);

                    contentSB.append("\n" + stars + 
                        "\nWarnings from LoadDatasets\n\n");
                    contentSB.append(warningsFromLoadDatasets);

                    contentSB.append("\n" + stars + "\n");
                    contentSB.append(threadList);

                    //clear all the "since last daily report" tallies
                    EDStatic.tally.remove(".subset (since last daily report)");
                    EDStatic.tally.remove(".subset DatasetID (since last daily report)");
                    EDStatic.tally.remove("Advanced Search with Category Constraints (since last daily report)");
                    EDStatic.tally.remove("Advanced Search with Lat Lon Constraints (since last daily report)");
                    EDStatic.tally.remove("Advanced Search with Time Constraints (since last daily report)");
                    EDStatic.tally.remove("Advanced Search, .fileType (since last daily report)");
                    EDStatic.tally.remove("Advanced Search, Search For (since last daily report)");
                    EDStatic.tally.remove("Categorize Attribute (since last daily report)");
                    EDStatic.tally.remove("Categorize Attribute = Value (since last daily report)");
                    EDStatic.tally.remove("Categorize File Type (since last daily report)");
                    EDStatic.tally.remove("Convert (since last daily report)");
                    EDStatic.tally.remove("files browse DatasetID (since last daily report)");
                    EDStatic.tally.remove("files download DatasetID (since last daily report)");
                    EDStatic.tally.remove("griddap DatasetID (since last daily report)");
                    EDStatic.tally.remove("griddap File Type (since last daily report)");
                    EDStatic.tally.remove("Home Page (since last daily report)");
                    EDStatic.tally.remove("Info (since last daily report)");
                    EDStatic.tally.remove("Info File Type (since last daily report)");
                    EDStatic.tally.remove("Large Request, IP address (since last daily report)");
                    EDStatic.tally.remove("Log in attempt blocked temporarily (since last daily report)");
                    EDStatic.tally.remove("Log in failed (since last daily report)");
                    EDStatic.tally.remove("Log in succeeded (since last daily report)");
                    EDStatic.tally.remove("Log out (since last daily report)");
                    EDStatic.tally.remove("Main Resources List (since last daily report)");
                    EDStatic.tally.remove("Metadata requests (since last daily report)");
                    EDStatic.tally.remove("OpenSearch For (since last daily report)");
                    EDStatic.tally.remove("OutOfMemory (Array Size), IP Address (since last daily report)");
                    EDStatic.tally.remove("OutOfMemory (Too Big), IP Address (since last daily report)");
                    EDStatic.tally.remove("OutOfMemory (Way Too Big), IP Address (since last daily report)");
                    EDStatic.tally.remove("POST (since last daily report)");
                    EDStatic.tally.remove("Protocol (since last daily report)");
                    EDStatic.tally.remove("Requester Is Logged In (since last daily report)");
                    EDStatic.tally.remove("Request refused: not authorized (since last daily report)");
                    EDStatic.tally.remove("Requester's IP Address (Allowed) (since last daily report)");
                    EDStatic.tally.remove("Requester's IP Address (Blacklisted) (since last daily report)");
                    EDStatic.tally.remove("Requester's IP Address (Failed) (since last daily report)");
                    EDStatic.tally.remove("RequestReloadASAP (since last daily report)");
                    EDStatic.tally.remove("Response Failed    Time (since last daily report)");
                    EDStatic.tally.remove("Response Succeeded Time (since last daily report)");
                    EDStatic.tally.remove("RSS (since last daily report)");
                    EDStatic.tally.remove("Search File Type (since last daily report)");
                    EDStatic.tally.remove("Search For (since last daily report)");
                    EDStatic.tally.remove("SetDatasetFlag (since last daily report)");
                    EDStatic.tally.remove("SetDatasetFlag Failed, IP Address (since last daily report)");
                    EDStatic.tally.remove("SetDatasetFlag Succeeded, IP Address (since last daily report)");
                    EDStatic.tally.remove("SOS index.html (since last daily report)");
                    EDStatic.tally.remove("Subscriptions (since last daily report)");
                    EDStatic.tally.remove("tabledap DatasetID (since last daily report)");
                    EDStatic.tally.remove("tabledap File Type (since last daily report)");
                    EDStatic.tally.remove("TaskThread Failed    Time (since last daily report)");
                    EDStatic.tally.remove("TaskThread Succeeded Time (since last daily report)");
                    EDStatic.tally.remove("WCS index.html (since last daily report)");
                    EDStatic.tally.remove("WMS doWmsGetMap (since last daily report)");
                    EDStatic.tally.remove("WMS doWmsGetCapabilities (since last daily report)");
                    EDStatic.tally.remove("WMS doWmsDemo (since last daily report)");
                    EDStatic.tally.remove("WMS index.html (since last daily report)");

                    EDStatic.failureTimesDistribution24      = new int[String2.DistributionSize];
                    EDStatic.majorLoadDatasetsDistribution24 = new int[String2.DistributionSize];
                    EDStatic.minorLoadDatasetsDistribution24 = new int[String2.DistributionSize];
                    EDStatic.responseTimesDistribution24     = new int[String2.DistributionSize];

                    String2.log("\n" + stars);
                    String2.log(contentSB.toString());
                    String2.log(
                        "\n" + String2.javaInfo() + //Sort of confidential. This info would simplify attacks on ERDDAP.
                        "\n" + stars + 
                        "\nEnd of Daily Report\n");

                    //after write to log (before email), add URLs to setDatasetFlag (so only in email to admin)
                    contentSB.append("\n" + stars + 
                        "\nsetDatasetFlag URLs can be used to force a dataset to be reloaded (treat as confidential information)\n\n");
                    StringArray datasetIDs = erddap.allDatasetIDs();
                    datasetIDs.sortIgnoreCase();
                    for (int ds = 0; ds < datasetIDs.size(); ds++) {
                        contentSB.append(EDD.flagUrl(datasetIDs.get(ds)));
                        contentSB.append('\n');
                    }

                    //after write to log (before email), add subscription info (so only in email to admin)
                    if (EDStatic.subscriptionSystemActive) {
                        try {
                            contentSB.append("\n\n" + stars + 
                                "\nTreat Subscription Information as Confidential:\n");
                            contentSB.append(EDStatic.subscriptions.listSubscriptions());
                        } catch (Throwable lst) {
                            contentSB.append("LoadDatasets Error: " + MustBe.throwableToString(lst));
                        }
                    } else {
                        contentSB.append("\n\n" + stars + 
                            "\nThe email/URL subscription system is not active.\n");
                    }

                    //write to email
                    contentSB.append(
                        "\n" + String2.javaInfo() + //Sort of confidential. This info would simplify attacks on ERDDAP.
                        "\n" + stars + 
                        "\nEnd of Daily Report\n");
                    String content = contentSB.toString();
                    String2.log(subject + ":");
                    String2.log(content);
                    EDStatic.email(
                        String2.ifSomethingConcat(EDStatic.emailEverythingToCsv, ",", EDStatic.emailDailyReportToCsv), 
                        subject, content);
                } else {
                    //major load, but not daily report
                    StringBuilder sb = new StringBuilder();
                    EDStatic.addIntroStatistics(sb);

                    if (threadSummary != null)
                        sb.append(threadSummary + "\n");

                    sb.append(Math2.memoryString() + " " + Math2.xmxMemoryString() + "\n\n");
                    EDStatic.addCommonStatistics(sb);
                    sb.append(EDStatic.tally.toString("OutOfMemory (Array Size), IP Address (since last Major LoadDatasets)", 50));
                    sb.append(EDStatic.tally.toString("OutOfMemory (Too Big), IP Address (since last Major LoadDatasets)", 50));
                    sb.append(EDStatic.tally.toString("OutOfMemory (Way Too Big), IP Address (since last Major LoadDatasets)", 50));
                    sb.append(EDStatic.tally.toString("Requester's IP Address (Allowed) (since last Major LoadDatasets)", 50));
                    sb.append(EDStatic.tally.toString("Requester's IP Address (Blacklisted) (since last Major LoadDatasets)", 50));
                    sb.append(EDStatic.tally.toString("Requester's IP Address (Failed) (since last Major LoadDatasets)", 50));

                    sb.append(threadList);
                    String2.log(sb.toString());

                    //email if some threshold is surpassed???
                    int nFailed    = String2.getDistributionN(EDStatic.failureTimesDistributionLoadDatasets);
                    int nSucceeded = String2.getDistributionN(EDStatic.responseTimesDistributionLoadDatasets);
                    if (nFailed + nSucceeded > EDStatic.unusualActivity) //high activity level
                        EDStatic.email(EDStatic.emailEverythingToCsv, 
                            "Unusual Activity: lots of requests", sb.toString());
                    else if (nFailed > 10 && nFailed > nSucceeded / 4)    // >25% of requests fail
                        EDStatic.email(EDStatic.emailEverythingToCsv, 
                            "Unusual Activity: >25% of requests failed", sb.toString());
                }

                //after every major loadDatasets
                EDStatic.tally.remove("Large Request, IP address (since last Major LoadDatasets)");
                EDStatic.tally.remove("OutOfMemory (Array Size), IP Address (since last Major LoadDatasets)");
                EDStatic.tally.remove("OutOfMemory (Too Big), IP Address (since last Major LoadDatasets)");
                EDStatic.tally.remove("OutOfMemory (Way Too Big), IP Address (since last Major LoadDatasets)");
                EDStatic.tally.remove("Request refused: not authorized (since last Major LoadDatasets)"); //datasetID (not IP address)
                EDStatic.tally.remove("Requester's IP Address (Allowed) (since last Major LoadDatasets)");
                EDStatic.tally.remove("Requester's IP Address (Blacklisted) (since last Major LoadDatasets)");
                EDStatic.tally.remove("Requester's IP Address (Failed) (since last Major LoadDatasets)");

                EDStatic.failureTimesDistributionLoadDatasets  = new int[String2.DistributionSize];
                EDStatic.responseTimesDistributionLoadDatasets = new int[String2.DistributionSize];
                int tpo = 13200; //132 char/line * 100 lines 
                if (EDStatic.majorLoadDatasetsTimeSeriesSB.length() > tpo) {
                    //hopefully, start looking at exact desired \n location
                    int apo = EDStatic.majorLoadDatasetsTimeSeriesSB.indexOf("\n", tpo - 1);
                    if (apo >= 0)
                        EDStatic.majorLoadDatasetsTimeSeriesSB.setLength(apo + 1);
                }                   

                String2.flushLog(); //useful to have this info ASAP and ensure log is flushed periodically
            }

        } catch (Throwable t) {
            String subject = String2.ERROR + " while processing " + 
                (xmlReader == null? "" : "line #" + xmlReader.lineNumber() + " ") + 
                "datasets.xml";
            if (!isInterrupted()) {
                EDStatic.errorsDuringMajorReload  = 
                    subject + ": see log.txt for details.\n";  //swap into place
                String content = MustBe.throwableToString(t); 
                unexpectedError = subject + ": " + content;
                String2.log(unexpectedError);
                EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
            }
        } finally {
            if (xmlReader != null) 
                try {xmlReader.close();} catch (Exception e) {}
            EDStatic.suggestAddFillValueCSV.setLength(0);
        }
    }

    /**
     * If change is something, this tries to do the actions /notify the subscribers
     * to this dataset.
     * This may or may not succeed but won't throw an exception.
     *
     * @param tDatasetID must be specified or nothing is done
     * @param cooDataset The Current Or Old Dataset may be null
     * @param subject for email messages
     * @param change the change description must be specified or nothing is done
     */
    public static void tryToDoActions(Erddap erddap, String tDatasetID, EDD cooDataset, 
        String subject, String change) {
        if (String2.isSomething(tDatasetID) && String2.isSomething(change)) {
            if (!String2.isSomething(subject))
                subject = "Change to datasetID=" + tDatasetID;
            try {
                StringArray actions = null;

                if (EDStatic.subscriptionSystemActive) { 
                    //get subscription actions
                    try { //beware exceptions from subscriptions
                        actions = EDStatic.subscriptions.listActions(tDatasetID);
                    } catch (Throwable listT) {
                        String content = MustBe.throwableToString(listT); 
                        String2.log(subject + ":\n" + content);
                        EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
                        actions = new StringArray();
                    }
                } else actions = new StringArray();

                //get dataset.onChange actions
                int nSubscriptionActions = actions.size();
                if (cooDataset != null && cooDataset.onChange() != null) 
                    actions.append(cooDataset.onChange());

                //do the actions
                if (verbose) String2.log("nActions=" + actions.size());

                for (int a = 0; a < actions.size(); a++) {
                    String tAction = actions.get(a);
                    if (verbose) 
                        String2.log("doing action[" + a + "]=" + tAction);
                    try {
                        if (tAction.startsWith("http://") ||
                            tAction.startsWith("https://")) {
                            if (tAction.indexOf("/" + EDStatic.warName + "/setDatasetFlag.txt?") > 0 &&
                                EDStatic.urlIsThisComputer(tAction)) { 
                                //a dataset on this ERDDAP! just set the flag
                                //e.g., https://coastwatch.pfeg.noaa.gov/erddap/setDatasetFlag.txt?datasetID=ucsdHfrW500&flagKey=##########
                                String trDatasetID = String2.extractCaptureGroup(tAction, "datasetID=(.+?)&", 1);
                                if (trDatasetID == null)
                                    SSR.touchUrl(tAction, 60000); //fall back; just do it
                                else EDD.requestReloadASAP(trDatasetID);

                            } else {
                                //but don't get the input stream! I don't need to, 
                                //and it is a big security risk.
                                SSR.touchUrl(tAction, 60000);
                            }
                        } else if (tAction.startsWith("mailto:")) {
                            String tEmail = tAction.substring("mailto:".length());
                            EDStatic.email(tEmail,
                                "datasetID=" + tDatasetID + " changed.", 
                                "datasetID=" + tDatasetID + " changed.\n" + 
                                change + "\n\n*****\n" +
                                (a < nSubscriptionActions? 
                                    EDStatic.subscriptions.messageToRequestList(tEmail) :
                                    "This action is specified in datasets.xml.\n")); 
                                    //It would be nice to include unsubscribe 
                                    //info for this action, 
                                    //but it isn't easily available.
                        } else {
                            throw new RuntimeException("The startsWith of action=" + 
                                tAction + " is not allowed!");
                        }
                    } catch (Throwable actionT) {
                        String2.log(subject + "\n" + 
                            "action=" + tAction + "\ncaught:\n" + 
                            MustBe.throwableToString(actionT));
                    }
                }

                //trigger RSS action 
                // (after new dataset is in place and if there is either a current or older dataset)
                if (cooDataset != null && erddap != null) 
                    cooDataset.updateRSS(erddap, change);

            } catch (Throwable subT) {
                String content = MustBe.throwableToString(subT); 
                String2.log(subject + ":\n" + content);
                EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
            }
        }
    }


    /** 
     * If useLuceneSearchEngine, this will update the Lucene indices for these datasets.
     *
     * <p>Since luceneIndexWriter is thread-safe, this is thread-safe to the extent 
     * that data structures won't be corrupted; 
     * however, it is still susceptible to incorrect information if 2+ thredds
     * work with the same datasetID at the same time (if one adding and one removing)
     * because of race conditions. 
     *
     * @param datasetIDs
     */
    public static void updateLucene(Erddap erddap, StringArray datasetIDs) {

        //update dataset's Document in Lucene Index
        int nDatasetIDs = datasetIDs.size();
        if (EDStatic.useLuceneSearchEngine && nDatasetIDs > 0) {

            try {
                //gc to avoid out-of-memory
                Math2.gcAndWait(); //avoid trouble in updateLucene()

                String2.log("start updateLucene()"); 
                if (EDStatic.luceneIndexWriter == null) //if trouble last time
                    EDStatic.createLuceneIndexWriter(false); //throws exception if trouble

                //update the datasetIDs
                long tTime = System.currentTimeMillis();
                for (int idi = 0; idi < nDatasetIDs; idi++) {
                    String tDatasetID = datasetIDs.get(idi); 
                    EDD edd = erddap.gridDatasetHashMap.get(tDatasetID);
                    if (edd == null) 
                        edd = erddap.tableDatasetHashMap.get(tDatasetID);
                    if (edd == null) {
                        //remove it from Lucene     luceneIndexWriter is thread-safe
                        EDStatic.luceneIndexWriter.deleteDocuments( 
                            new Term("datasetID", tDatasetID));
                    } else {
                        //update it in Lucene
                        EDStatic.luceneIndexWriter.updateDocument(
                            new Term("datasetID", tDatasetID),
                            edd.searchDocument());                                    
                    }
                }

                //commit the changes  (recommended over close+reopen)
                EDStatic.luceneIndexWriter.commit();

                String2.log("updateLucene() finished." + 
                    " nDocs=" + EDStatic.luceneIndexWriter.numDocs() + 
                    " nChanged=" + nDatasetIDs + 
                    " time=" + (System.currentTimeMillis() - tTime) + "ms");
            } catch (Throwable t) {

                //any exception is pretty horrible
                //  e.g., out of memory, index corrupt, IO exception
                String subject = String2.ERROR + " in updateLucene()";
                String content = MustBe.throwableToString(t); 
                String2.log(subject + ":\n" + content);
                EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);

                //abandon the changes and the indexWriter
                if (EDStatic.luceneIndexWriter != null) {
                    //close luceneIndexWriter  (see indexWriter javaDocs)
                    try {
                        //abandon pending changes
                        EDStatic.luceneIndexWriter.close(true);
                        Math2.gcAndWait(); //part of dealing with lucene trouble
                    } catch (Throwable t2) {
                        String2.log(MustBe.throwableToString(t2));
                    }

                    //trigger creation of another indexWriter next time updateLucene is called
                    EDStatic.luceneIndexWriter = null;
                }
            }

            //last: update indexReader+indexSearcher 
            //(might as well take the time to do it in this thread,
            //rather than penalize next search request)
            EDStatic.needNewLuceneIndexReader = true; 
            EDStatic.luceneIndexSearcher();
        }
        datasetIDs.clear();
    }


    /** Given a newline separated string in sb, this keeps the newest approximately keepLines. */
    static void removeOldLines(StringBuffer sb, int keepLines, int lineLength) {
        if (sb.length() > (keepLines+1) * lineLength) {
            int po = sb.indexOf("\n", sb.length() - keepLines * lineLength);
            if (po > 0)
                sb.delete(0, po + 1);
        }
    }


    /** 
     * This unloads a dataset with tId if it was active.
     * 
     * <p>Since all the data structures are threadsafe, this is thread-safe to the extent 
     * that the data structures won't be corrupted; 
     * however, it is still susceptible to incorrect information if 2+ thredds
     * work with the same datasetID at the same time (if one adding and one removing)
     * because of race conditions. 
     *
     * @param tId a datasetID
     * @param changedDatasetIDs is a list of <strong>other</strong> changedDatasetIDs that need
     *   to be updated by Lucene if updateLucene is actually called
     * @param needToUpdateLucene if true and if a dataset is actually unloaded, 
     *   this calls updateLucene (i.e., commits the changes).
     *   Even if updateLucene() is called, it doesn't set lastLuceneUpdate = System.currentTimeMillis().
     * @return true if tId existed and was unloaded.
     */
    public static boolean tryToUnload(Erddap erddap, String tId, StringArray changedDatasetIDs, 
        boolean needToUpdateLucene) {

        EDD oldEdd = erddap.gridDatasetHashMap.remove(tId);
        if (oldEdd == null) {
            oldEdd = erddap.tableDatasetHashMap.remove(tId);
            if (oldEdd == null)  
                return false;
        }

        //it was active; finish removing it
        //do in quick succession...   (???synchronized on ?)
        String2.log("*** unloading datasetID=" + tId);
        addRemoveDatasetInfo(REMOVE, erddap.categoryInfo, oldEdd); 
        File2.deleteAllFiles(EDD.cacheDirectory(tId));
        changedDatasetIDs.add(tId);
        if (needToUpdateLucene)
            updateLucene(erddap, changedDatasetIDs);
        //do dataset actions so subscribers know it is gone
        tryToDoActions(erddap, tId, oldEdd, null,  //default subject
            "This dataset is currently unavailable.");

        return true;
    }

    /**
     * This high level method is the entry point to add/remove the 
     * dataset's metadata to/from the proper places in catInfo.
     * 
     * <p>Since catInfo is a ConcurrentHashMap, this is thread-safe to the extent 
     * that data structures won't be corrupted; 
     * however, it is still susceptible to incorrect information if 2+ thredds
     * work with the same datasetID at the same time (if one adding and one removing)
     * because of race conditions. 
     *
     * @param add determines whether datasetID references will be ADDed or REMOVEd
     * @param catInfo the new categoryInfo hashMap of hashMaps of hashSets
     * @param edd the dataset who's info should be added to catInfo
     */
    protected static void addRemoveDatasetInfo(boolean add, 
        ConcurrentHashMap catInfo, EDD edd) {

        //go through the gridDatasets
        String id = edd.datasetID();

        //globalAtts
        categorizeGlobalAtts(add, catInfo, edd, id);

        //go through data variables
        int nd = edd.dataVariables().length;
        for (int dv = 0; dv < nd; dv++) 
            categorizeVariableAtts(add, catInfo, edd.dataVariables()[dv], id);
        
        if (edd instanceof EDDGrid) {
            EDDGrid eddGrid = (EDDGrid)edd;
            //go through axis variables
            int na = eddGrid.axisVariables().length;
            for (int av = 0; av < na; av++) 
                categorizeVariableAtts(add, catInfo, eddGrid.axisVariables()[av], id);
        }
    }

    /** 
     * This low level method adds/removes the global attribute categories of an EDD. 
     *
     * <p>"global:keywords" is treated specially: the value is split by "\n" or ",",
     * then each keyword is added separately.
     *
     * @param add determines whether datasetID references will be ADDed or REMOVEd
     * @param catInfo the new categoryInfo
     * @param edd 
     * @param id the edd.datasetID()
     */
    protected static void categorizeGlobalAtts(boolean add,
        ConcurrentHashMap catInfo, EDD edd, String id) {

        Attributes atts = edd.combinedGlobalAttributes();
        int nCat = EDStatic.categoryAttributes.length;
        for (int cat = 0; cat < nCat; cat++) {
            if (EDStatic.categoryIsGlobal[cat]) {
                String catName = EDStatic.categoryAttributes[cat]; //e.g., global:institution
                String value = atts.getString(catName);
                //String2.log("catName=" + catName + " value=" + String2.toJson(value));

                if (value != null && catName.equals("keywords")) {
                    //split keywords, then add/remove
                    StringArray vals = StringArray.fromCSVNoBlanks(value);
                    for (int i = 0; i < vals.size(); i++) {
                        //remove outdated "earth science > "
                        String s = vals.get(i).toLowerCase();
                        if (s.startsWith("earth science > "))
                            s = s.substring(16);
                        //String2.log("  " + i + "=" + vals[i]);
                        addRemoveIdToCatInfo(add, catInfo, catName, 
                            String2.modifyToBeFileNameSafe(s), id);
                    }
                } else {
                    //add/remove the single value
                    addRemoveIdToCatInfo(add, catInfo, catName, 
                        String2.modifyToBeFileNameSafe(value).toLowerCase(), id);
                }
            }
        }
    }

    /** 
     * This low level method adds/removes the attributes category references of an EDV. 
     *
     * @param add determines whether datasetID references will be ADDed or REMOVEd
     * @param catInfo the new categoryInfo
     * @param edv 
     * @param id the edd.datasetID()
     */
    protected static void categorizeVariableAtts(boolean add,
        ConcurrentHashMap catInfo, EDV edv, String id) {

        Attributes atts = edv.combinedAttributes();
        int nCat = EDStatic.categoryAttributes.length;
        for (int cat = 0; cat < nCat; cat++) {
            if (!EDStatic.categoryIsGlobal[cat]) {
                String catName = EDStatic.categoryAttributes[cat]; //e.g., standard_name
                String catAtt = cat == EDStatic.variableNameCategoryAttributeIndex? //special case
                    edv.destinationName() :
                    atts.getString(catName);
                addRemoveIdToCatInfo(add, catInfo, catName, 
                    String2.modifyToBeFileNameSafe(catAtt).toLowerCase(), //e.g., sea_water_temperature
                    id);
            }
        }
    }

    /**
     * This low level method adds/removes a datasetID to a categorization.
     * 
     * @param add determines whether datasetID references will be ADDed or REMOVEd
     * @param catInfo the new categoryInfo
     * @param catName e.g., institution
     * @param catAtt e.g., NDBC
     * @param id  the edd.datasetID() e.g., ndbcCWind41002
     */
    protected static void addRemoveIdToCatInfo(boolean add, ConcurrentHashMap catInfo, 
        String catName, String catAtt, String id) {

        if (catAtt.length() == 0)
            return;

        ConcurrentHashMap hm = (ConcurrentHashMap)(catInfo.get(catName));  //e.g., for institution
        ConcurrentHashMap hs = (ConcurrentHashMap)hm.get(catAtt);  //e.g., for NDBC,  acts as hashset
        if (hs == null) {
            if (!add) //remove mode and reference isn't there, so we're done
                return;
            hs = new ConcurrentHashMap(16, 0.75f, 4);
            hm.put(catAtt, hs);
        }
        if (add) {
            hs.put(id, Boolean.TRUE);  //Boolean.TRUE is just something to fill the space
        } else {
            if (hs.remove(id) != null && hs.size() == 0) 
                hm.remove(catAtt);
        }
    }


}
