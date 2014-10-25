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
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
//import gov.noaa.pfel.erddap.variable.EDVTimeGridAxis;

import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

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
                " " + Calendar2.getCurrentISODateTimeStringLocal() +
                "\n  datasetsRegex=" + datasetsRegex + 
                " inputStream=" + (inputStream == null? "null" : "something") + 
                " majorLoad=" + majorLoad);
            long memoryInUse = 0;
            if (majorLoad) {
                //gc so getMemoryInUse more accurate
                //don't use Math2.sleep which catches/ignores interrupt
                System.gc();  Thread.sleep(Math2.gcSleep); //before get memoryString
                System.gc();  Thread.sleep(Math2.gcSleep); //before get memoryString
                memoryInUse = Math2.getMemoryInUse();
                String2.log(Math2.memoryString() + " " + Math2.xmxMemoryString());
            }
            long startTime = System.currentTimeMillis();
            int oldNGrid = erddap.gridDatasetHashMap.size();
            int oldNTable = erddap.tableDatasetHashMap.size();
            HashMap tUserHashMap = new HashMap(); //no need for thread-safe, all puts are here (1 thread); future gets are thread safe
            StringBuilder datasetsThatFailedToLoadSB = new StringBuilder();
            HashSet datasetIDSet = new HashSet(); //to detect duplicates, just local use, no need for thread-safe
            StringArray duplicateDatasetIDs = new StringArray(); //list of duplicates

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
                inputStream = new FileInputStream(newFileName);
            }

            //read datasets.xml
            xmlReader = new SimpleXMLReader(inputStream, "erddapDatasets");
            String startError = "datasets.xml error on line #";
            int nDatasets = 0;
            int nTry = 0;
            while (true) {
                //check for interruption 
                if (isInterrupted()) { 
                    String2.log("*** The LoadDatasets thread was interrupted at " + 
                        Calendar2.getCurrentISODateTimeStringLocal());
                    xmlReader.close();
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
                        boolean isFlagged = File2.delete(EDStatic.fullResetFlagDirectory + tId);
                        if (isFlagged) {
                            String2.log("*** reloading datasetID=" + tId + 
                                " because it was in flag directory.");
                        } else {
                            //does the dataset already exist and is young?
                            EDD oldEdd = (EDD)erddap.gridDatasetHashMap.get(tId);
                            if (oldEdd == null)
                                oldEdd = (EDD)erddap.tableDatasetHashMap.get(tId);
                            if (oldEdd != null) {
                                long minutesOld = oldEdd.creationTimeMillis() <= 0?  //see edd.setCreationTimeTo0
                                    Long.MAX_VALUE :
                                    (System.currentTimeMillis() - oldEdd.creationTimeMillis()) / 60000; 
                                if (minutesOld < oldEdd.getReloadEveryNMinutes()) {
                                    //it exists and is young
                                    String2.log("*** skipping datasetID=" + tId + 
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
                            dataset = EDD.fromXml(xmlReader.attributeValue("type"), xmlReader);

                            //check for interruption right before making changes to Erddap
                            if (isInterrupted()) { //this is a likely place to catch interruption
                                String2.log("*** The LoadDatasets thread was interrupted at " + 
                                    Calendar2.getCurrentISODateTimeStringLocal());
                                xmlReader.close();
                                updateLucene(erddap, changedDatasetIDs);
                                lastLuceneUpdate = System.currentTimeMillis();
                                return;
                            }

                            //do several things in quick succession...
                            //(??? synchronize on (?) if really need avoid inconsistency)

                            //was there a dataset with the same datasetID?
                            oldDataset = (EDD)(erddap.gridDatasetHashMap.get(tId));
                            if (oldDataset == null)
                                oldDataset = (EDD)(erddap.tableDatasetHashMap.get(tId));

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
                                erddap.gridDatasetHashMap.put(tId, dataset);  //was/is grid

                            } else if ((oldDataset == null || oldDataset instanceof EDDTable) &&
                                                                 dataset instanceof EDDTable) {
                                erddap.tableDatasetHashMap.put(tId, dataset); //was/is table 

                            } else if (dataset instanceof EDDGrid) {
                                if (oldDataset != null)
                                    erddap.tableDatasetHashMap.remove(tId);   //was table
                                erddap.gridDatasetHashMap.put(tId, dataset);  //now grid

                            } else if (dataset instanceof EDDTable) {
                                if (oldDataset != null)
                                    erddap.gridDatasetHashMap.remove(tId);    //was grid
                                erddap.tableDatasetHashMap.put(tId, dataset); //now table
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
                                    Calendar2.getCurrentISODateTimeStringLocal();
                                String2.log(tError2);
                                warningsFromLoadDatasets.append(tError2 + "\n\n");
                                xmlReader.close();
                                updateLucene(erddap, changedDatasetIDs);
                                lastLuceneUpdate = System.currentTimeMillis();
                                return;
                            }


                            //actually remove old dataset (if any existed)
                            EDD tDataset = (EDD)erddap.gridDatasetHashMap.remove(tId); //always ensure it was removed
                            if (tDataset == null)
                                tDataset = (EDD)erddap.tableDatasetHashMap.remove(tId);
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
                                xmlReader.close();
                                throw new RuntimeException(startError + xmlReader.lineNumber() + 
                                    ": " + t.toString(), t);
                            }
           
                            //skip over the remaining tags for this dataset
                            try {
                                while (!xmlReader.allTags().equals("<erddapDatasets></dataset>")) 
                                    xmlReader.nextTag();
                            } catch (Throwable t2) {
                                xmlReader.close();
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
                        if (change.length() > 0) {
                            try {
                                StringArray actions = null;

                                if (EDStatic.subscriptionSystemActive) { 
                                    //get subscription actions
                                    try { //beware exceptions from subscriptions
                                        actions = EDStatic.subscriptions.listActions(tId);
                                    } catch (Throwable listT) {
                                        String subject = startError + xmlReader.lineNumber() + " with Subscriptions";
                                        String content = MustBe.throwableToString(listT); 
                                        String2.log(subject + ":\n" + content);
                                        EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
                                        actions = new StringArray();
                                    }
                                } else actions = new StringArray();

                                //get dataset.onChange actions
                                int nSubscriptionActions = actions.size();
                                if (cooDataset != null) {
                                    if (cooDataset.onChange() != null) actions.append(cooDataset.onChange());
                                }

                                //do the actions
                                if (verbose) String2.log("nActions=" + actions.size());

                                for (int a = 0; a < actions.size(); a++) {
                                    String tAction = actions.get(a);
                                    if (reallyVerbose) String2.log("doing action=" + tAction);
                                    try {
                                        if (tAction.startsWith("http://")) {
                                            //but don't get the input stream! I don't need to, 
                                            //and it is a big security risk.
                                            SSR.touchUrl(tAction, 60000);
                                        } else if (tAction.startsWith("mailto:")) {
                                            String tEmail = tAction.substring("mailto:".length());
                                            EDStatic.email(tEmail,
                                                "datasetID=" + tId + " changed.", 
                                                "datasetID=" + tId + " changed.\n" + 
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
                                        String2.log(startError + xmlReader.lineNumber() + "\n" + 
                                            "action=" + tAction + "\n" + 
                                            MustBe.throwableToString(actionT));
                                    }
                                }
                            } catch (Throwable subT) {
                                String subject = startError + xmlReader.lineNumber() + " with Subscriptions";
                                String content = MustBe.throwableToString(subT); 
                                String2.log(subject + ":\n" + content);
                                EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
                            }
                        }

                        //trigger RSS action 
                        // (after new dataset is in place and if there is either a current or older dataset)
                        if (cooDataset != null && change.length() > 0) {
                            try {
                                //generate the rss xml
                                //See general info: http://en.wikipedia.org/wiki/RSS_(file_format)
                                //  background: http://www.mnot.net/rss/tutorial/
                                //  rss 2.0 spec: http://cyber.law.harvard.edu/rss/rss.html
                                //I chose rss 2.0 for no special reason (most modern version of that fork; I like "simple").
                                //The feed programs didn't really care if just pubDate changed.
                                //  They care about item titles changing.
                                //  So this treats every change as a new item with a different title, 
                                //    replacing the previous item.
                                StringBuilder rss = new StringBuilder();
                                GregorianCalendar gc = Calendar2.newGCalendarZulu();
                                String pubDate = 
                                    "    <pubDate>" + Calendar2.formatAsRFC822GMT(gc) + "</pubDate>\n";
                                String link = 
                                    "    <link>" + EDStatic.publicErddapUrl(cooDataset.getAccessibleTo() == null) +
                                        "/" + cooDataset.dapProtocol() + "/" + tId;
                                rss.append(
                                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                    "<rss version=\"2.0\" xmlns=\"http://backend.userland.com/rss2\">\n" +
                                    "  <channel>\n" +
                                    "    <title>ERDDAP: " + XML.encodeAsXML(cooDataset.title()) + "</title>\n" +
                                    "    <description>This RSS feed changes when the dataset changes.</description>\n" +      
                                    link + ".html</link>\n" +
                                    pubDate +
                                    "    <item>\n" +
                                    "      <title>This dataset changed " + Calendar2.formatAsISODateTimeT(gc) + "Z</title>\n" +
                                    "  " + link + ".html</link>\n" +
                                    "      <description>" + XML.encodeAsXML(change) + "</description>\n" +      
                                    "    </item>\n" +
                                    "  </channel>\n" +
                                    "</rss>\n");

                                //store the xml
                                erddap.rssHashMap.put(tId, String2.getUTF8Bytes(rss.toString()));

                            } catch (Throwable rssT) {
                                String subject = startError + xmlReader.lineNumber() + " with RSS";
                                String content = MustBe.throwableToString(rssT); 
                                String2.log(subject + ":\n" + content);
                                EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
                            }
                        }
                    }

                } else if (tags.equals("<erddapDatasets><subscriptionEmailBlacklist>")) {
                } else if (tags.equals("<erddapDatasets></subscriptionEmailBlacklist>")) {
                    if (EDStatic.subscriptionSystemActive)
                        EDStatic.subscriptions.setEmailBlacklist(xmlReader.content());

                } else if (tags.equals("<erddapDatasets><requestBlacklist>")) {
                } else if (tags.equals("<erddapDatasets></requestBlacklist>")) {
                    EDStatic.setRequestBlacklist(xmlReader.content());

                } else if (tags.equals("<erddapDatasets><convertToPublicSourceUrl>")) {
                    String tFrom = xmlReader.attributeValue("from");
                    String tTo   = xmlReader.attributeValue("to");
                    int spo = EDStatic.convertToPublicSourceUrlFromSlashPo(tFrom);
                    if (tFrom != null && tFrom.length() > 3 && spo == tFrom.length() - 1 && tTo != null) 
                        EDStatic.convertToPublicSourceUrl.put(tFrom, tTo);                        
                } else if (tags.equals("<erddapDatasets></convertToPublicSourceUrl>")) {

                //<user username="bsimons" password="..." roles="admin, role1" />
                //this mimics tomcat syntax
                } else if (tags.equals("<erddapDatasets><user>")) { 
                    String tUser = xmlReader.attributeValue("username");
                    String tPassword  = xmlReader.attributeValue("password");  
                    if (tPassword != null && !EDStatic.passwordEncoding.equals("plaintext")) 
                        tPassword = tPassword.toLowerCase(); //match Digest Authentication standard case
                    String ttRoles = xmlReader.attributeValue("roles");
                    String tRoles[] = ttRoles == null || ttRoles.trim().length() == 0?
                        new String[0] : String2.split(ttRoles, ','); 

                    if (tUser != null && tUser.length() > 0) {
                        if (EDStatic.authentication.equals("custom") &&   //others in future
                            (tPassword == null || tPassword.length() < 7)) {
                            warningsFromLoadDatasets.append(
                                "datasets.xml error: The password for <user> username=" + tUser + 
                                " in datasets.xml had fewer than 7 characters.\n\n");
                        } else {
                            Arrays.sort(tRoles);
                            if (reallyVerbose) String2.log("user=" + tUser + " roles=" + String2.toCSSVString(tRoles));
                            Object o = tUserHashMap.put(tUser, new Object[]{tPassword, tRoles});
                            if (o != null)
                                warningsFromLoadDatasets.append(
                                    "datasets.xml error: There are two <user> tags in datasets.xml with username=" + 
                                    tUser + "\nChange one of them.\n\n");
                        }
                    } else {
                        warningsFromLoadDatasets.append(
                            "datasets.xml error: A <user> tag in datasets.xml had no username=\"\" attribute.\n\n");
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
            String duplicateDatasetIDsMsg = majorLoad && duplicateDatasetIDs.size() > 0?         
                String2.ERROR + ": Duplicate datasetIDs in datasets.xml:\n    " +
                    String2.noLongLinesAtSpace(duplicateDatasetIDs.toString(), 100, "    ") + "\n" :
                "";

            EDStatic.nGridDatasets = erddap.gridDatasetHashMap.size();
            EDStatic.nTableDatasets = erddap.tableDatasetHashMap.size();

            //*** print lots of useful information
            long loadDatasetsTime = System.currentTimeMillis() - startTime;
            String cDateTimeLocal = Calendar2.getCurrentISODateTimeStringLocal();
            String2.log("\n" + String2.makeString('*', 80) + "\n" + 
                "LoadDatasets.run finished at " + cDateTimeLocal + "  TOTAL TIME=" + loadDatasetsTime + "\n" +
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
                if (duplicateDatasetIDsMsg.length() > 0)
                    String2.log(duplicateDatasetIDsMsg);
            }

            //majorLoad?
            if (majorLoad) {
                String2.distribute(loadDatasetsTime, EDStatic.majorLoadDatasetsDistribution24);
                String2.distribute(loadDatasetsTime, EDStatic.majorLoadDatasetsDistributionTotal);
                //gc so getMemoryInUse more accurate
                //don't use Math2.sleep which catches/ignores interrupt
                System.gc();  Thread.sleep(Math2.gcSleep); //aggressive, before get memoryString()
                System.gc();  Thread.sleep(Math2.gcSleep); //aggressive, before get memoryString()
                String memoryString = Math2.memoryString();
                String2.log(
                    "  " + memoryString + " " + Math2.xmxMemoryString() +
                    "\n  change for this run of major Load Datasets (MB) = " + ((Math2.getMemoryInUse() - memoryInUse) / Math2.BytesPerMB) + "\n");

                EDStatic.datasetsThatFailedToLoad = datasetsThatFailedToLoad; //swap into place
                EDStatic.duplicateDatasetIDsMsg   = duplicateDatasetIDsMsg;   //swap into place
                EDStatic.memoryUseLoadDatasetsSB.append("  " + cDateTimeLocal + "  " + memoryString + "\n");
                EDStatic.failureTimesLoadDatasetsSB.append("  " + cDateTimeLocal + "  " + 
                    String2.getBriefDistributionStatistics(EDStatic.failureTimesDistributionLoadDatasets) + "\n");
                EDStatic.responseTimesLoadDatasetsSB.append("  " + cDateTimeLocal + "  " + 
                    String2.getBriefDistributionStatistics(EDStatic.responseTimesDistributionLoadDatasets) + "\n");

                //email daily report?
                GregorianCalendar reportCalendar = Calendar2.newGCalendarLocal();
                String reportDate = Calendar2.formatAsISODate(reportCalendar);
                int hour = reportCalendar.get(Calendar2.HOUR_OF_DAY);
                //if (true) {  //uncomment to test daily report 

                if (!reportDate.equals(erddap.lastReportDate) && hour >= 7) {

                    erddap.lastReportDate = reportDate;
                    String stars = String2.makeString('*', 70);
                    String subject = "ERDDAP Daily Report " + cDateTimeLocal;
                    StringBuilder contentSB = new StringBuilder(subject + "\n\n");
                    EDStatic.addIntroStatistics(contentSB);

                    //append number of active threads
                    String traces = MustBe.allStackTraces(true, true);
                    int po = traces.indexOf('\n');
                    if (po > 0)
                        contentSB.append(traces.substring(0, po + 1));

                    contentSB.append(Math2.memoryString() + " " + Math2.xmxMemoryString() + "\n\n");
                    contentSB.append(stars + "\nTallied Usage Information\n\n");
                    contentSB.append(EDStatic.tally.toString(50));
                    EDStatic.addCommonStatistics(contentSB);

                    contentSB.append("\n" + stars + 
                        "\nWarnings from LoadDatasets\n\n");
                    contentSB.append(warningsFromLoadDatasets);

                    contentSB.append("\n" + stars + "\n");
                    contentSB.append(traces);

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
                    EDStatic.tally.remove("griddap DatasetID (since last daily report)");
                    EDStatic.tally.remove("griddap File Type (since last daily report)");
                    EDStatic.tally.remove("Home Page (since last daily report)");
                    EDStatic.tally.remove("Info (since last daily report)");
                    EDStatic.tally.remove("Info File Type (since last daily report)");
                    EDStatic.tally.remove("Log in attempt blocked temporarily (since last daily report)");
                    EDStatic.tally.remove("Log in failed (since last daily report)");
                    EDStatic.tally.remove("Log in succeeded (since last daily report)");
                    EDStatic.tally.remove("Log in Redirect (since last daily report)");
                    EDStatic.tally.remove("Log out (since last daily report)");
                    EDStatic.tally.remove("Main Resources List (since last daily report)");
                    EDStatic.tally.remove("MemoryInUse > MaxSafeMemory (since last daily report)");
                    EDStatic.tally.remove("Metadata requests (since last daily report)");
                    EDStatic.tally.remove("OpenSearch For (since last daily report)");
                    EDStatic.tally.remove("POST (since last daily report)");
                    EDStatic.tally.remove("Protocol (since last daily report)");
                    EDStatic.tally.remove("Requester Is Logged In (since last daily report)");
                    EDStatic.tally.remove("Request refused: array size >= Integer.MAX_VALUE (since last daily report)");
                    EDStatic.tally.remove("Request refused: not enough memory currently (since last daily report)");
                    EDStatic.tally.remove("Request refused: not enough memory ever (since last daily report)");
                    EDStatic.tally.remove("Requester's IP Address (Allowed) (since last daily report)");
                    EDStatic.tally.remove("Requester's IP Address (Blocked) (since last daily report)");
                    EDStatic.tally.remove("RequestReloadASAP (since last daily report)");
                    EDStatic.tally.remove("Response Failed    Time (since last daily report)");
                    EDStatic.tally.remove("Response Succeeded Time (since last daily report)");
                    EDStatic.tally.remove("RSS (since last daily report)");
                    EDStatic.tally.remove("Search File Type (since last daily report)");
                    EDStatic.tally.remove("Search For (since last daily report)");
                    EDStatic.tally.remove("SetDatasetFlag (since last daily report)");
                    EDStatic.tally.remove("SOS index.html (since last daily report)");
                    EDStatic.tally.remove("Subscriptions (since last daily report)");
                    EDStatic.tally.remove("tabledap DatasetID (since last daily report)");
                    EDStatic.tally.remove("tabledap File Type (since last daily report)");
                    EDStatic.tally.remove("TaskThread Failed    Time (since last daily report)");
                    EDStatic.tally.remove("TaskThread Succeeded Time (since last daily report)");
                    EDStatic.tally.remove("WCS index.html (since last daily report)");
                    EDStatic.tally.remove("WMS doWmsGetMap (since last daily report)");
                    EDStatic.tally.remove("WMS doWmsGetCapabilities (since last daily report)");
                    EDStatic.tally.remove("WMS doWmsOpenLayers (since last daily report)");
                    EDStatic.tally.remove("WMS index.html (since last daily report)");

                    EDStatic.failureTimesDistribution24      = new int[String2.DistributionSize];
                    EDStatic.majorLoadDatasetsDistribution24 = new int[String2.DistributionSize];
                    EDStatic.minorLoadDatasetsDistribution24 = new int[String2.DistributionSize];
                    EDStatic.responseTimesDistribution24     = new int[String2.DistributionSize];

                    erddap.todaysNRequests = 0;
                    String2.log("\n" + stars);
                    String2.log(contentSB.toString());
                    String2.log(
                        "\n" + String2.javaInfo() + //Sort of confidential. This info would simplify attacks on ERDDAP.
                        "\n" + stars + 
                        "\nEnd of ERDDAP Daily Report\n");

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
                        "\nEnd of ERDDAP Daily Report\n");
                    String content = contentSB.toString();
                    String2.log(subject + ":");
                    String2.log(content);
                    EDStatic.email(
                        EDStatic.emailEverythingToCsv + //won't be null
                        ((EDStatic.emailEverythingToCsv.length()  > 0 &&
                          EDStatic.emailDailyReportToCsv.length() > 0)? "," : "") +
                        EDStatic.emailDailyReportToCsv, //won't be null
                        subject, content);
                } else {
                    //major load, but not daily report
                    StringBuilder sb = new StringBuilder();
                    EDStatic.addIntroStatistics(sb);

                    //append number of active threads
                    String traces = MustBe.allStackTraces(true, true);
                    int po = traces.indexOf('\n');
                    if (po > 0)
                        sb.append(traces.substring(0, po + 1));

                    sb.append(Math2.memoryString() + " " + Math2.xmxMemoryString() + "\n\n");
                    EDStatic.addCommonStatistics(sb);
                    sb.append(EDStatic.tally.toString("Requester's IP Address (Allowed) (since last Major LoadDatasets)", 50));
                    sb.append(EDStatic.tally.toString("Requester's IP Address (Blocked) (since last Major LoadDatasets)", 50));
                    sb.append(traces);
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
                EDStatic.tally.remove("Requester's IP Address (Allowed) (since last Major LoadDatasets)");
                EDStatic.tally.remove("Requester's IP Address (Blocked) (since last Major LoadDatasets)");
                EDStatic.failureTimesDistributionLoadDatasets  = new int[String2.DistributionSize];
                EDStatic.responseTimesDistributionLoadDatasets = new int[String2.DistributionSize];
                removeOldLines(EDStatic.memoryUseLoadDatasetsSB,     101, 82);
                removeOldLines(EDStatic.failureTimesLoadDatasetsSB,  101, 59);
                removeOldLines(EDStatic.responseTimesLoadDatasetsSB, 101, 59);
            }

        } catch (Throwable t) {
            if (xmlReader != null) 
                xmlReader.close();
            if (!isInterrupted()) {
                String subject = "Error while processing datasets.xml at " + 
                    Calendar2.getCurrentISODateTimeStringLocal();
                String content = MustBe.throwableToString(t); 
                unexpectedError = subject + ": " + content;
                String2.log(unexpectedError);
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
                    EDD edd = (EDD)erddap.gridDatasetHashMap.get(tDatasetID);
                    if (edd == null) 
                        edd = (EDD)erddap.tableDatasetHashMap.get(tDatasetID);
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
                    " time=" + (System.currentTimeMillis() - tTime));
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
     * @param changedDatasetIDs is a list of <b>other</b> changedDatasetIDs that need
     *   to be updated by Lucene if updateLucene is actually called
     * @param needToUpdateLucene if true and if a dataset is actually unloaded, 
     *   this calls updateLucene (i.e., commits the changes).
     *   Even if updateLucene() is called, it doesn't set lastLuceneUpdate = System.currentTimeMillis().
     * @return true if tId existed and was unloaded.
     */
    public static boolean tryToUnload(Erddap erddap, String tId, StringArray changedDatasetIDs, 
        boolean needToUpdateLucene) {

        EDD oldEdd = (EDD)erddap.gridDatasetHashMap.remove(tId);
        if (oldEdd == null) {
            oldEdd = (EDD)erddap.tableDatasetHashMap.remove(tId);
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
