/* 
 * EDDTableFromThreddsFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.ShortArray;
import com.cohort.array.LongArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;


/** 
 * This class downloads data from a THREDDS data server with lots of files 
 * into .nc files in the [bigParentDirectory]/copy/datasetID, 
 * and then uses superclass EDDTableFromFiles methods to read/serve data 
 * from the .nc files. So don't wrap this class in EDDTableCopy.
 * 
 * <p>The TDS files can be n-dimensional (1,2,3,4,...) DArray or DGrid
 * OPeNDAP files, each of which is flattened into a table.
 * For example, http://biloxi-bay.ssc.hpc.msstate.edu/dods-bin/nph-dods/WCOS/nmsp/wcos/ 
 * (the four dimensions there are e.g., time,depth,lat,lon).
 *
 * <p>This class is very similar to EDDTableFromHyraxFiles.
 *
 * @author Bob Simons (bob.simons@noaa.gov) originally 2009-06-08;
 * modified extensively (copy the files first) 2012-02-21.
 */
public class EDDTableFromThreddsFiles extends EDDTableFromFiles { 

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;



    /** 
     * The constructor just calls the super constructor. 
     *
     * <p>The sortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     */
    public EDDTableFromThreddsFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles) throws Throwable {

        super("EDDTableFromThreddsFiles", true, //isLocal is now set to true (copied files)
            tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            EDStatic.fullCopyDirectory + tDatasetID + "/", //force fileDir to be the copyDir 
            tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles);
    }

    /**
     * Create tasks to download files.
     * If getThreddsFileInfo is completelySuccessful, local files that
     * aren't mentioned on the server will be renamed [fileName].ncRemoved .
     * <br>This won't throw an exception.
     * 
     * @param catalogUrl  should have /catalog/ in the middle and 
     *    catalog.html or catalog.xml at the end
     * @param specialMode e.g., "SAMOS" adds special restrictions to accept the file
     */
    public static void makeDownloadFileTasks(String tDatasetID, 
        String catalogUrl, 
        String fileNameRegex, boolean recursive, String specialMode) {

        if (verbose) String2.log("* " + tDatasetID + " makeDownloadFileTasks from " + catalogUrl +
            "\nfileNameRegex=" + fileNameRegex);
        long startTime = System.currentTimeMillis();
        int taskNumber = -1; //unused

        try {
            //if previous tasks are still running, return
            EDStatic.ensureTaskThreadIsRunningIfNeeded();  //ensure info is up-to-date
            Integer lastAssignedTask = (Integer)EDStatic.lastAssignedTask.get(tDatasetID);
            boolean pendingTasks = lastAssignedTask != null &&  
                EDStatic.lastFinishedTask < lastAssignedTask.intValue();
            if (verbose) 
                String2.log("  lastFinishedTask=" + EDStatic.lastFinishedTask + 
                    " < lastAssignedTask(" + tDatasetID + ")=" + lastAssignedTask + 
                    "? pendingTasks=" + pendingTasks);
            if (pendingTasks) 
                return;

            //mimic the remote directory structure (there may be 10^6 files in many dirs)
            //catalogUrl = http://data.nodc.noaa.gov/thredds/catalog /nmsp/wcos/ catalog.xml
            //remoteBase = http://data.nodc.noaa.gov/thredds/dodsC   /nmsp/wcos/ 
            //e.g., a URL  http://data.nodc.noaa.gov/thredds/dodsC   /nmsp/wcos/ WES001/2008/WES001_030MTBD029R00_20080429.nc
            if (catalogUrl == null || catalogUrl.length() == 0)
                throw new RuntimeException("ERROR: <sourceUrl>http://.../catalog.html</sourceUrl> " +
                    "must be in the addGlobalAttributes section of the datasets.xml " +
                    "for datasetID=" + tDatasetID);
            if (catalogUrl.endsWith("/catalog.html"))
                catalogUrl = File2.forceExtension(catalogUrl, ".xml");
            String lookFor = File2.getDirectory(catalogUrl); //e.g., /nmsp/wcos/ , always at least /
            int po = lookFor.indexOf("/catalog/"); 
            if (po >= 0) {
                lookFor = lookFor.substring(po + 8);  //8, not 9, so starts with /
            } else {
                po = lookFor.indexOf("/dodsC/"); 
                if (po >= 0) 
                    lookFor = lookFor.substring(po + 6);  //6, not 7, so starts with /
                else throw new RuntimeException(
                    "ERROR: <sourceUrl>" + catalogUrl + "</sourceUrl> " +
                    "in datasets.xml for datasetID=" + tDatasetID +
                    " must have /catalog/ in the middle.");
            }
            int lookForLength = lookFor.length();

            //mimic the remote dir structure in baseDir
            String baseDir = EDStatic.fullCopyDirectory + tDatasetID + "/";
            //e.g. localFile EDStatic.fullCopyDirectory + tDatasetID +  / WES001/2008/WES001_030MTBD029R00_20080429.nc
            File2.makeDirectory(baseDir);

            //gather all sourceFile info
            StringArray sourceFileDir   = new StringArray();
            StringArray sourceFileName  = new StringArray();
            LongArray sourceFileLastMod = new LongArray();             
            boolean completelySuccessful = getThreddsFileInfo(
                catalogUrl, fileNameRegex, recursive,
                sourceFileDir, sourceFileName, sourceFileLastMod);

            //samos-specific:
            //Given file names like KAQP_20120103v30001.nc
            //and                   KAQP_20120103v30101.nc
            //this just keeps the file with the last version number.
            if ("SAMOS".equals(specialMode)) {
                int n = sourceFileName.size();
                if (n > 1) {
                    //1) sort by sourceFileName
                    ArrayList tfTable = new ArrayList();
                    tfTable.add(sourceFileDir);
                    tfTable.add(sourceFileName);
                    tfTable.add(sourceFileLastMod);
                    PrimitiveArray.sort(tfTable, new int[]{1}, new boolean[]{true});

                    //2) just keep the last version file
                    BitSet keep = new BitSet();
                    keep.set(0, n);
                    int vpo = sourceFileName.get(0).lastIndexOf('v'), ovpo;
                    if (vpo < 0)
                        keep.clear(0);
                    for (int i = 1; i < n; i++) {  //1.. since looking back to previous
                        ovpo = vpo;
                        vpo = sourceFileName.get(i).lastIndexOf('v');
                        if (vpo < 0) {
                            keep.clear(i);                    
                        } else if (ovpo >= 0) {
                            if (sourceFileName.get(i-1).substring(0, ovpo).equals(
                                sourceFileName.get(i  ).substring(0, vpo)))
                                keep.clear(i - 1);
                        }
                    }
                    sourceFileDir.justKeep(keep);
                    sourceFileName.justKeep(keep);
                    sourceFileLastMod.justKeep(keep);
                }
                //String2.log(sourceFileName.toNewlineString());
            }

            //Rename (make inactive) local files that shouldn't exist?
            //If getThreddsFileInfo is completelySuccessful and found some files, 
            //local files that aren't mentioned on the server will be renamed
            //[fileName].ncRemoved .
            //If not completelySuccessful, perhaps the server is down temporarily,
            //no files will be renamed.
            //!!! This is imperfect. If a remote sub webpage always fails, then
            //  no local files will ever be deleted.
            if (completelySuccessful && sourceFileName.size() > 0) {
                //make a hashset of theoretical local fileNames that will exist 
                //  after copying based on getThreddsFileInfo
                HashSet hashset = new HashSet();
                int nFiles = sourceFileName.size();
                for (int f = 0; f < nFiles; f++) {
                    String sourceDir = sourceFileDir.get(f);
                    po = sourceDir.lastIndexOf(lookFor);
                    if (po >= 0) {
                        //String2.log("po=" + po + " lookForLength=" + lookForLength + " sourceDir.length=" + sourceDir.length());
                        String willExist = baseDir + sourceDir.substring(po + lookForLength) + 
                            sourceFileName.get(f);
                        hashset.add(willExist);
                        //String2.log("  willExist=" + willExist);
                    } //else continue;
                }

                //get all the existing local files
                String localFiles[] = recursive?
                    RegexFilenameFilter.recursiveFullNameList(baseDir, fileNameRegex, false) : //directoriesToo
                    RegexFilenameFilter.fullNameList(baseDir, fileNameRegex);

                //rename local files not in the hashset of files that will exist to [fileName].ncRemoved 
                int nLocalFiles = localFiles.length;
                int nRemoved = 0;
                if (reallyVerbose) String2.log("Looking for local files to rename 'Removed' because the " +
                    "datasource no longer has the corresponding file...");
                for (int f = 0; f < nLocalFiles; f++) {
                    //if a localFile isn't in hashset of willExist files, it shouldn't exist
                    if (!hashset.remove(localFiles[f])) {
                        nRemoved++;
                        if (reallyVerbose) String2.log("  renaming to " + localFiles[f] + "Removed");
                        File2.rename(localFiles[f], localFiles[f] + "Removed");
                    }
                    localFiles[f] = null; //allow gc    (as does remove() above)
                }
                if (verbose) String2.log(nRemoved + 
                    " local files were renamed to [fileName].ncRemoved because the datasource no longer has " +
                      "the corresponding file.\n" +
                    (nLocalFiles - nRemoved) + " files remain.");

                /* /if 0 files remain (e.g., from significant change), delete empty subdir
                if (nLocalFiles - nRemoved == 0) {
                    try {
                        RegexFilenameFilter.recursiveDelete(baseDir);
                        if (verbose) String2.log(tDatasetID + " copyDirectory is completely empty.");
                    } catch (Throwable t) {
                        String2.log(MustBe.throwableToString(t));
                    }
                }*/
            }

            //make tasks to download files
            int nTasksCreated = 0;
            boolean remoteErrorLogged = false;  //just display 1st offender
            boolean fileErrorLogged   = false;  //just display 1st offender
            int nFiles = sourceFileName.size();
            for (int f = 0; f < nFiles; f++) {
                String sourceDir  = sourceFileDir.get(f);
                String sourceName = sourceFileName.get(f);
                po = sourceDir.lastIndexOf(lookFor);
                if (po < 0) {
                    if (!remoteErrorLogged) {
                        String2.log(
                            "ERROR! lookFor=" + lookFor + " wasn't in sourceDir=" + sourceDir);
                        remoteErrorLogged = true;
                    }
                    continue;
                }

                //see if up-to-date localFile exists  (keep name identical; don't add .nc)
                String localFile = baseDir + sourceDir.substring(po + lookForLength) + 
                    sourceName;
                String reason = "";
                try {
                    //don't use File2 so more efficient for current purpose
                    File file = new File(localFile);
                    if (!file.isFile())
                        reason = "new file";
                    else if (file.lastModified() != sourceFileLastMod.get(f))
                        reason = "lastModified changed";
                    else 
                        continue; //up-to-date file already exists
                } catch (Exception e) {
                    if (!fileErrorLogged) {
                        String2.log(
                              "ERROR checking localFile=" + localFile +
                            "\n" + MustBe.throwableToString(e));
                        fileErrorLogged = true;
                    }
                }

                //make a task to download sourceFile to localFile
                // taskOA[1]=dapUrl, taskOA[2]=fullFileName, taskOA[3]=lastModified (Long)
                Object taskOA[] = new Object[7];
                taskOA[0] = TaskThread.TASK_ALL_DAP_TO_NC;
                taskOA[1] = sourceDir + sourceName;
                taskOA[2] = localFile;
                taskOA[3] = new Long(sourceFileLastMod.get(f));
                int tTaskNumber = EDStatic.addTask(taskOA);
                if (tTaskNumber >= 0) {
                    nTasksCreated++;
                    taskNumber = tTaskNumber;
                    if (reallyVerbose)
                        String2.log("  task#" + taskNumber + " TASK_DAP_TO_NC reason=" + reason +
                            "\n    from=" + sourceDir + sourceName +
                            "\n    to=" + localFile);
                }
            }

            //create task to flag dataset to be reloaded
            if (taskNumber > -1) {
                Object taskOA[] = new Object[2];
                taskOA[0] = TaskThread.TASK_SET_FLAG;
                taskOA[1] = tDatasetID;
                taskNumber = EDStatic.addTask(taskOA); //TASK_SET_FLAG will always be added
                nTasksCreated++;
                if (reallyVerbose)
                    String2.log("  task#" + taskNumber + " TASK_SET_FLAG " + tDatasetID);
            }

            if (verbose) String2.log("* " + tDatasetID + " makeDownloadFileTasks finished." +
                " nTasksCreated=" + nTasksCreated + 
                " time=" + (System.currentTimeMillis() - startTime));

        } catch (Throwable t) {
            if (verbose)
                String2.log("ERROR in makeDownloadFileTasks for datasetID=" + tDatasetID + "\n" +
                    MustBe.throwableToString(t));
        }
        if (taskNumber > -1) {
            EDStatic.lastAssignedTask.put(tDatasetID, new Integer(taskNumber));
            EDStatic.ensureTaskThreadIsRunningIfNeeded();  //ensure info is up-to-date
        }
    }


    /**
     * This gathers file information from a THREDDS file-directory-like catalog that shows 
     * directories with lists of files, each of which MUST have a lastModified date 
     * (or it is ignored ).
     * This calls itself recursively, adding info to fileDir, fileName and fileLastMod
     * as files are found.
     *
     * @param catalogUrl the url of the current Thredds catalog xml, e.g.,
     *    http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/catalog.xml
     *    which leads to unaggregated datasets (each from a file) like
     *     http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080429.nc(.html)
     *    catalogUrl SHOULD have /catalog/, but sometimes that can be worked around.
     * @param fileNameRegex  to be accepted, a fileName (without dir) must
     *   match this regex, e.g., ".*\\.nc"
     * @param recursive if the method whould descend to subdirectories
     * @param fileDir receives fileDir Url for each accepted file
     * @param fileName receives fileName for each accepted file
     * @param fileLastMod receives lastModified time of each accepted file
     * @returns true if the search was completely successful (no failure
     *   to get any page).
     * @throws RuntimeException if trouble.
     *    Url not responding is not an error.
     */
    public static boolean getThreddsFileInfo(String catalogUrl, String fileNameRegex, 
        boolean recursive,
        StringArray fileDir, StringArray fileName, LongArray fileLastMod) {

        if (reallyVerbose) String2.log("\n<<< getThreddsFileInfo catalogUrl=" + 
            catalogUrl + " regex=" + fileNameRegex);
        boolean completelySuccessful = true;
        long time = System.currentTimeMillis();

        try {
            int catPo = catalogUrl.indexOf( "/catalog/");
            if (catPo < 0) {
                if (verbose) String2.log("  WARNING: '/catalog/' not found in" +
                    //e.g., http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/
                    //        ncep.reanalysis.dailyavgs/surface/catalog.xml
                    "\n    catalogUrl=" + catalogUrl);
                int tPod = catalogUrl.indexOf("/thredds/dodsC/");
                int tPo  = catalogUrl.indexOf("/thredds/");
                if (tPod > 0)
                    catalogUrl = catalogUrl.substring(0, tPo + 9) + 
                        "catalog/" + catalogUrl.substring(tPo + 15);
                else if (tPo > 0)
                    catalogUrl = catalogUrl.substring(0, tPo + 9) + 
                        "catalog/" + catalogUrl.substring(tPo + 9);
                else 
                    catalogUrl = File2.getDirectory(catalogUrl) + 
                        "catalog/" + File2.getNameAndExtension(catalogUrl);
                //e.g., http://www.esrl.noaa.gov/psd/thredds/catalog/Datasets/
                //        ncep.reanalysis.dailyavgs/surface/catalog.xml
                if (verbose) String2.log("    so trying catalogUrl=" + catalogUrl);
                catPo = catalogUrl.indexOf( "/catalog/");
            }       
            String catalogBase = catalogUrl.substring(0, catPo + 9); //ends in "/catalog/";
            if (reallyVerbose) String2.log("  catalogBase=" + catalogBase);

            //e.g., threddsName is usually "thredds"
            String threddsName = File2.getNameAndExtension(catalogUrl.substring(0, catPo)); 
            int ssPo = catalogUrl.indexOf("//");
            if (ssPo < 0) 
                throw new SimpleException("'//' not found in catalogUrl=" + catalogUrl);
            int sPo = catalogUrl.indexOf('/', ssPo + 2);
            if (sPo < 0) 
                throw new SimpleException("'/' not found in catalogUrl=" + catalogUrl);
            //e.g., threddsBase=http://www.esrl.noaa.gov
            String threddsBase = catalogUrl.substring(0, sPo);
            if (reallyVerbose) String2.log("  threddsBase=" + threddsBase);

            String serviceBase = "/" + threddsName + "/dodsC/"; //default
            if (reallyVerbose) String2.log("threddsName=" + threddsName + 
                " threddsBase=" + threddsBase + "\ncatalogBase=" + catalogBase);
            int nLogged = 0;

            String datasetTags[] = {
                "won't match",
                "<catalog><dataset>",
                "<catalog><dataset><dataset>",
                "<catalog><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset><dataset>"};
            String endDatasetTags[] = {
                "won't match",
                "<catalog></dataset>",
                "<catalog><dataset></dataset>",
                "<catalog><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset></dataset>"};
            String endDateTags[] = {
                "won't match",
                "<catalog><dataset></date>",
                "<catalog><dataset><dataset></date>",
                "<catalog><dataset><dataset><dataset></date>",
                "<catalog><dataset><dataset><dataset><dataset></date>",
                "<catalog><dataset><dataset><dataset><dataset><dataset></date>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset></date>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset><dataset></date>"};
            int nNames = datasetTags.length;
            String names[] = new String[nNames];

            //I could get inputStream from catalogUrl, but then (via recursion) perhaps lots of streams open.
            //I think better to get the entire response (succeed or fail *now*).
            byte bytes[] = SSR.getUrlResponseBytes(catalogUrl);
            SimpleXMLReader xmlReader = new SimpleXMLReader(new ByteArrayInputStream(bytes));
            while (true) {
                xmlReader.nextTag();
                String tags = xmlReader.allTags();
                int whichDatasetTag    = String2.indexOf(datasetTags,    tags);
                int whichEndDatasetTag = String2.indexOf(endDatasetTags, tags);
                int whichEndDateTag    = String2.indexOf(endDateTags,    tags);

                //<catalogRef xlink:href="2008/catalog.xml" xlink:title="2008" ID="nmsp/wcos/WES001/2008" name=""/>
                if (recursive && tags.endsWith("<catalogRef>")) {
                    String href = xmlReader.attributeValue("xlink:href");
                    if (href != null) { //look for /...
                        if (!href.startsWith("http")) {  //if not a complete catalogUrl
                            if (href.startsWith("/" + threddsName + "/catalog/"))
                                href = threddsBase + href;
                            else if (href.startsWith("./")) 
                                href = File2.getDirectory(catalogUrl) + href.substring(2);
                            else if (!href.startsWith("/")) 
                                href = File2.getDirectory(catalogUrl) + href;
                            else href = catalogBase + href.substring(1); //href starts with /
                        }
                    if (!getThreddsFileInfo(href, fileNameRegex, recursive,
                        fileDir, fileName, fileLastMod))
                        completelySuccessful = false;
                    }

                //<dataset name="WES001_030MTBD029R00_20080613.nc" ID="nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc" urlPath="nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc">
                //  <date type="modified">2010-01-09 21:37:24Z</date>
                //</dataset> 
                } else if (whichDatasetTag > 0) {
                    String tName = xmlReader.attributeValue("name");
                    if (tName != null) {
                        boolean matches = tName.matches(fileNameRegex);
                        if ((verbose && nLogged < 5) || reallyVerbose) {
                            String2.log("  tName=" + tName + " matches=" + matches);
                            nLogged++;
                        }
                        if (matches)
                            names[whichDatasetTag] = tName;
                    }

                } else if (whichEndDatasetTag > 0) {
                    names[whichEndDatasetTag] = null;
                    
                } else if (whichEndDateTag > 0 && names[whichEndDateTag] != null) {
                    //"<catalog><dataset><dataset></date>"
                    String isoTime = xmlReader.content();
                    double epochSeconds = Calendar2.safeIsoStringToEpochSeconds(isoTime);
                    if (Double.isNaN(epochSeconds)) {
                        if ((verbose && nLogged < 5) || reallyVerbose) 
                            String2.log("    isoTime=" + isoTime + " evaluates to NaN");
                    } else {

                        //add to file list
                        String dodsUrl = String2.replaceAll(File2.getDirectory(catalogUrl), 
                            "/" + threddsName + "/catalog/", "/" + threddsName + "/dodsC/");
                        if (reallyVerbose) String2.log("    found " + 
                            dodsUrl + names[whichEndDateTag] + "   " + isoTime);

                        fileDir.add(dodsUrl);
                        fileName.add(names[whichEndDateTag]); 
                        fileLastMod.add(Math2.roundToLong(epochSeconds * 1000));
                    }

                } else if (tags.equals("</catalog>")) {
                    xmlReader.close();
                    break;
                }
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            completelySuccessful = false;
        }
        if (reallyVerbose) String2.log("\n>>> leaving getThreddsFileInfo" +
            " nFiles=" + fileName.size() + 
            " completelySuccessful=" + completelySuccessful + 
            " time=" + (System.currentTimeMillis() - time));
        return completelySuccessful;
    }


    /**
     * This gets source data from one copied .nc file.
     * See documentation in EDDTableFromFiles.
     *
     * @throws an exception if too much data.
     *  This won't throw an exception if no data.
     */
    public Table lowGetSourceDataFromFile(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        StringArray sourceConVars, StringArray sourceConOps, StringArray sourceConValues,
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        //Future: more efficient if !mustGetData is handled differently

        //read the file
        Table table = new Table();
        table.readNDNc(fileDir + fileName, sourceDataNames.toArray(),
            sortedSpacing >= 0 && !Double.isNaN(minSorted)? sortedColumnSourceName : null,
                minSorted, maxSorted, 
            getMetadata);
        //String2.log("  EDDTableFromThreddsFiles.lowGetSourceDataFromFile table.nRows=" + table.nRows());

        return table;
    }



    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromThreddsFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tLocalDirUrl  the base/starting URL with a Thredds (sub-)catalog, 
     *    usually ending in catalog.xml (but sometimes other file names).
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *    If null or "", it is generated to catch the same extension as the sampleFileName
     *    (".*" if no extension or e.g., ".*\\.nc").
     * @param oneFileDapUrl  url for one file, without ending .das or .html
     * @param tReloadEveryNMinutes
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortedColumnSourceName   use "" if not known or not needed. 
     * @param tSortFilesBySourceNames   This is useful, because it ultimately determines default results order.
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String tLocalDirUrl, 
        String tFileNameRegex, String oneFileDapUrl, int tReloadEveryNMinutes,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, Attributes externalAddGlobalAttributes) 
        throws Throwable {

        String2.log("EDDTableFromThreddsFiles.generateDatasetsXml" +
            "\n  dirUrl=" + tLocalDirUrl + 
            "\n  oneFileDapUrl=" + oneFileDapUrl);
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

        String tPublicDirUrl = convertToPublicSourceUrl(tLocalDirUrl);
        String tPublicDirUrlHtml = tPublicDirUrl;
        String tDatasetID = suggestDatasetID(tPublicDirUrl + tFileNameRegex);
        String dir = EDStatic.fullTestCacheDirectory;
        int po1, po2;

        //download the 1 file     
        //URL may not have .nc at end.  I think that's okay.  Keep exact file name from URL.
        String ncFileName = File2.getNameAndExtension(oneFileDapUrl);
        OpendapHelper.allDapToNc(oneFileDapUrl, dir + ncFileName); 

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        dataSourceTable.readNDNc(dir + ncFileName, null,
            "", Double.NaN, Double.NaN, //constraints
            true); //getMetadata

        Table dataAddTable = new Table();
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            dataAddTable.addColumn(c, colName,
                dataSourceTable.getColumn(c),
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    dataSourceTable.globalAttributes(), sourceAtts, colName, 
                    true, true)); //addColorBarMinMax, tryToFindLLAT

            //if a variable has timeUnits, files are likely sorted by time
            //and no harm if files aren't sorted that way
            if (tSortedColumnSourceName.length() == 0 && 
                EDVTimeStamp.hasTimeUnits(sourceAtts, null))
                tSortedColumnSourceName = colName;
        }

        if (tFileNameRegex == null || tFileNameRegex.length() == 0) {
            String tExt = File2.getExtension(oneFileDapUrl);
            if (tExt == null || tExt.length() == 0)
                tFileNameRegex = ".*";
            else tFileNameRegex = ".*\\" + tExt;
        }

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            //no units or standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);
        }

        //global metadata
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", tPublicDirUrl);
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataSourceTable, dataAddTable)? "Point" : "Other",
                tLocalDirUrl, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //gather the information
        StringBuilder sb = new StringBuilder();
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = (tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName)).trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromThreddsFiles\" datasetID=\"" + 
                tDatasetID + "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>" + suggestedUpdateEveryNMillis + "</updateEveryNMillis>\n" +  
            "    <fileDir></fileDir>  <!-- automatically set to [bigParentDirectory]/copy/" + tDatasetID + "/ -->\n" +
            "    <recursive>true</recursive>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" +
            "    <accessibleViaFiles>false</accessibleViaFiles>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, true, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
        
    }


    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();
        try {
            String results = generateDatasetsXml(
                //I could do wcos/catalog.xml but very slow because lots of files
                "http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml",
                ".*MTBD.*\\.nc",   // ADCP files have different vars and diff metadata, e.g., _FillValue
                "http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc",
                1440, 
                "", "_.*$", ".*", "stationID",
                "Time", "stationID Time",
                null) + "\n"; //externalAddGlobalAttributes

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromThreddsFiles",
                "http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml",
                ".*MTBD.*\\.nc",  
                "http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc",
                "1440", 
                "", "_.*$", ".*", "stationID",
                "Time", "stationID Time"},
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromThreddsFiles\" datasetID=\"noaa_nodc_8fcf_be37_cbe4\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir></fileDir>  <!-- automatically set to [bigParentDirectory]/copy/noaa_nodc_8fcf_be37_cbe4/ -->\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>.*MTBD.*\\.nc</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex></preExtractRegex>\n" +
"    <postExtractRegex>_.*$</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>stationID</columnNameForExtract>\n" +
"    <sortedColumnSourceName>Time</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>stationID Time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Conventions\">CF-1.4</att>\n" +                                                 //dates below change
"        <att name=\"History\">created by the NCDDC PISCO Temperature Profile to NetCDF converter on 2012/31/11 20:31 CST. Original dataset URL:</att>\n" +
"        <att name=\"Mooring_ID\">WES001</att>\n" +
"        <att name=\"Version\">2</att>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">NODC.Webmaster@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA NODC</att>\n" +
"        <att name=\"creator_url\">http://www.nodc.noaa.gov/</att>\n" +
"        <att name=\"History\">null</att>\n" +                            //date below changes
"        <att name=\"history\">created by the NCDDC PISCO Temperature Profile to NetCDF converter on 2012/31/11 20:31 CST. Original dataset URL:</att>\n" +
"        <att name=\"infoUrl\">http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.html</att>\n" +
"        <att name=\"institution\">NOAA NODC</att>\n" +
"        <att name=\"keywords\">altitudes, catalo, center, data, data.nodc.noaa.gov, day, depth, expressed, flag, identifier, latitude, longitude, national, ncei, negative, noaa, nodc, ocean, oceanographic, oceans,\n" +
"Oceans &gt; Ocean Temperature &gt; Water Temperature,\n" +
"quality, sea, sea_water_temperature, sea_water_temperature status_flag, seawater, station, stationID, status, temperature, Temperature_flag, thredds, time, water, year, yearday, yearday_flag</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v27</att>\n" +
"        <att name=\"summary\">NOAA National Oceanographic Data Center (NODC) data from http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.html</att>\n" +
"        <att name=\"title\">NOAA NODC data from http://data.nodc.noaa.gov/thredds/catalo ...</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>stationID</sourceName>\n" +
"        <destinationName>stationID</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station ID</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">-9999</att>\n" +
"            <att name=\"description\">Greenwich Mean Time of each temperature measurement record,in seconds since 1970-01-01 00:00:00.000 0:00</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01 00:00:00.000 0:00</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Depth</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">-9999</att>\n" +
"            <att name=\"description\">Data logger measurement depth (expressed as negative altitudes), referenced to Mean Sea Level (MSL)</att>\n" +
"            <att name=\"long_name\">depth expressed as negative altitudes</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">meter</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarPalette\">OceanDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degree_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degree_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>yearday</sourceName>\n" +
"        <destinationName>yearday</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Day of the year</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature_flag</sourceName>\n" +
"        <destinationName>Temperature_flag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"description\">flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data</att>\n" +
"            <att name=\"long_name\">Temperature flag</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature status_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">128.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature</sourceName>\n" +
"        <destinationName>Temperature</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">9999</att>\n" +
"            <att name=\"description\">Seawater temperature</att>\n" +
"            <att name=\"long_name\">Sea Water Temperature</att>\n" +
"            <att name=\"quantity\">Temperature</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature</att>\n" +
"            <att name=\"units\">Celsius</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>yearday_flag</sourceName>\n" +
"        <destinationName>yearday_flag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"description\">flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data</att>\n" +
"            <att name=\"long_name\">Yearday flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">128.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            /* This won't work because sample file is in testCacheDir (not regular cache dir)
            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "noaa_nodc_8fcf_be37_cbe4", "");
            Test.ensureEqual(edd.title(), "WES001 2008", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "stationID, yearday, latitude, time, Depth, longitude, Temperature_flag, " +
                "Temperature, yearday_flag", "");
            */

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml."); 
        }

    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void testWcosTemp(boolean deleteCachedInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromThreddsFiles.testWcosTemp() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        try {

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.


        String id = "nmspWcosTemp";
        if (deleteCachedInfo) 
            deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        try {
        String2.log("\n****************** EDDTableFromThreddsFiles testWcosTemp das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  station {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -124.932, -119.66934;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 33.89511, 48.325001;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 _FillValue NaN;\n" +
"    Float64 actual_range 1.0971834e+9, 1.29749592e+9;\n" +  //changes sometimes   see time_coverage_end below
"    String axis \"T\";\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +  
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float64 _FillValue -9999.0;\n" +
"    Float64 actual_range 0.0, 99.0;\n" +
"    String axis \"Z\";\n" +
"    String description \"Relative to Mean Sea Level (MSL)\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Temperature {\n" +
"    Float64 _FillValue 9999.0;\n" +
"    Float64 actual_range 6.68, 38.25;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    String quantity \"Temperature\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  Temperature_flag {\n" +
"    Byte actual_range 0, 9;\n" +
"    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Temperature Flag\";\n" +
"    String standard_name \"sea_water_temperature status_flag\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeriesProfile\";\n" +
"    String cdm_profile_variables \"time\";\n" +
"    String cdm_timeseries_variables \"station, longitude, latitude\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -119.66934;\n" +
"    String featureType \"TimeSeriesProfile\";\n" +
"    Float64 geospatial_lat_max 48.325001;\n" +
"    Float64 geospatial_lat_min 33.89511;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -119.66934;\n" +
"    Float64 geospatial_lon_min -124.932;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 99.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Created by the NCDDC PISCO Temperature Profile to NetCDF converter.\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
    
//+ " http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\n" +
//today + " http://127.0.0.1:8080/cwexperimental/tabledap/
expected = 
"nmspWcosTemp.das\";\n" +
"    String infoUrl \"http://www.ncddc.noaa.gov/activities/wcos\";\n" +
"    String institution \"NOAA NMSP\";\n" +
"    String keywords \"Oceans > Ocean Temperature > Water Temperature\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 48.325001;\n" +
"    String sourceUrl \"http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\";\n" +
"    Float64 Southernmost_Northing 33.89511;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v27\";\n" +
"    String subsetVariables \"station, longitude, latitude\";\n" +
"    String summary \"The West Coast Observing System (WCOS) project provides access to temperature and currents data collected at four of the five National Marine Sanctuary sites, including Olympic Coast, Gulf of the Farallones, Monterey Bay, and Channel Islands. A semi-automated end-to-end data management system transports and transforms the data from source to archive, making the data acessible for discovery, access and analysis from multiple Internet points of entry.\n" +
"\n" +
"The stations (and their code names) are Ano Nuevo (ANO001), San Miguel North (BAY), Santa Rosa North (BEA), Big Creek (BIG001), Bodega Head (BOD001), Cape Alava 15M (CA015), Cape Alava 42M (CA042), Cape Alava 65M (CA065), Cape Alava 100M (CA100), Cannery Row (CAN001), Cape Elizabeth 15M (CE015), Cape Elizabeth 42M (CE042), Cape Elizabeth 65M (CE065), Cape Elizabeth 100M (CE100), Cuyler Harbor (CUY), Esalen (ESA001), Point Joe (JOE001), Kalaloch 15M (KL015), Kalaloch 27M (KL027), La Cruz Rock (LAC001), Lopez Rock (LOP001), Makah Bay 15M (MB015), Makah Bay 42M (MB042), Pelican/Prisoners Area (PEL), Pigeon Point (PIG001), Plaskett Rock (PLA001), Southeast Farallon Island (SEF001), San Miguel South (SMS), Santa Rosa South (SRS), Sunset Point (SUN001), Teawhit Head 15M (TH015), Teawhit Head 31M (TH031), Teawhit Head 42M (TH042), Terrace Point 7 (TPT007), Terrace Point 8 (TPT008), Valley Anch (VAL), Weston Beach (WES001).\";\n" +
"    String time_coverage_end \"2011-02-12T07:32:00Z\";\n" + //changes
"    String time_coverage_start \"2004-10-07T21:10:00Z\";\n" +
"    String title \"West Coast Observing System (WCOS) Temperature Data\";\n" +
"    String Version \"2\";\n" +
"    Float64 Westernmost_Easting -124.932;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        //*** test getting dds for entire dataset
        try{
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    Float64 depth;\n" +
"    Float64 Temperature;\n" +
"    Byte Temperature_flag;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        //*** test make data files
        String2.log("\n****************** EDDTableFromThreddsFiles.testWcosTemp make DATA FILES\n");       

        //.csv    for one lat,lon,time
        try {
        userDapQuery = "station&distinct()";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_stationList", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"station\n" +
"\n" +
"ANO001\n" +
"BAYXXX\n" +
"BEAXXX\n" +
"BIG001\n" +
"BOD001\n" +
"CA015X\n" +
"CA042X\n" +
"CA065X\n" +
"CA100X\n" +
"CE015X\n" +
"CE042X\n" +
"CE065X\n" +
"CE100X\n" +
"ESA001\n" +
"JOE001\n" +
"KL015X\n" +
"KL027X\n" +
"LAC001\n" +
"LOP001\n" +
"MB015X\n" +
"MB042X\n" +
"PELXXX\n" +
"PIG001\n" +
"SEF001\n" +
"SMSXXX\n" +
"SRSXXX\n" +
"SUN001\n" +
"TH015X\n" +
"TH031X\n" +
"TH042X\n" +
"TPT007\n" +
"TPT008\n" +
"VALXXX\n" +
"WES001\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        //.csv    for one lat,lon,time, many depths (from different files)      via lon > <
        userDapQuery = "&station=\"ANO001\"&time=1122592440";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1StationGTLT", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);   
//one depth, by hand via DAP form:   (note that depth is already negative!)
//Lat, 37.13015 Time=1122592440 depth=-12 Lon=-122.361253    
// yearday 208.968,  temp_flag 0, temp 10.66, yeardayflag  0
        expected = 
"station,longitude,latitude,time,depth,Temperature,Temperature_flag\n" +
",degrees_east,degrees_north,UTC,m,degree_C,\n" +
"ANO001,-122.361253,37.13015,2005-07-28T23:14:00Z,4.0,12.04,0\n" +
"ANO001,-122.361253,37.13015,2005-07-28T23:14:00Z,12.0,10.66,0\n" +
"ANO001,-122.361253,37.13015,2005-07-28T23:14:00Z,20.0,10.51,0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        /* */
    }

/*
Time              DArray[Time]
Depth             DArray[Depth]
Latitude          DArray[Latitude]
Longitude         DArray[Longitude]
Height            DGrid [Depth,Latitude,Longitude]
Height_flag       DGrid [Depth,Latitude,Longitude]
Pressure          DGrid [Time,Latitude,Longitude]
Pressure_flag     DGrid [Time,Latitude,Longitude]
Temperature       DGrid [Time,Latitude,Longitude]
Temperature_flag  DGrid [Time,Latitude,Longitude]
WaterDepth        DGrid [Time,Latitude,Longitude]
WaterDepth_flag   DGrid [Time,Latitude,Longitude]
YearDay           DGrid [Time,Latitude,Longitude]
YearDay_flag      DGrid [Time,Latitude,Longitude]
DataQuality       DGrid [Time,Depth,Latitude,Longitude]
DataQuality_flag  DGrid [Time,Depth,Latitude,Longitude]
Eastward          DGrid [Time,Depth,Latitude,Longitude]
Eastward_flag     DGrid [Time,Depth,Latitude,Longitude]
ErrorVelocity     DGrid [Time,Depth,Latitude,Longitude]
ErrorVelocity_flag DGrid [Time,Depth,Latitude,Longitude]
Intensity         DGrid [Time,Depth,Latitude,Longitude]
Intensity_flag    DGrid [Time,Depth,Latitude,Longitude]
Northward         DGrid [Time,Depth,Latitude,Longitude]
Northward_flag    DGrid [Time,Depth,Latitude,Longitude]
Upwards_flag      DGrid [Time,Depth,Latitude,Longitude]
Upwards           DGrid [Time,Depth,Latitude,Longitude]
*/




    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void testShipWTEP(boolean deleteCachedInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromThreddsFiles.testShipWTEP() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        try {

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 11); //[10]='T'

        String id = "fsuNoaaShipWTEP";
        if (deleteCachedInfo)
            deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        try {
        String2.log("\n****************** EDDTableFromThreddsFiles testShipWTEP das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_ShipEntire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected =      
"Attributes \\{\n" +
" s \\{\n" +
"  cruise_id \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"  \\}\n" +
"  expocode \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"  \\}\n" +
"  facility \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"  \\}\n" +
"  ID \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"  \\}\n" +
"  IMO \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"  \\}\n" +
"  platform \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"  \\}\n" +
"  platform_version \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"  \\}\n" +
"  site \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"  \\}\n" +
"  time \\{\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1886048e\\+9, .{8,14};\n" + //2nd number changes
"    String axis \"T\";\n" +
"    Int32 data_interval 60;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String observation_type \"calculated\";\n" +
"    Int32 qcindex 1;\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  latitude \\{\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -46.45, 70.05856;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    String axis \"Y\";\n" +
"    Float32 data_precision -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String observation_type \"measured\";\n" +
"    String original_units \"degrees \\(\\+N\\)\";\n" +
"    Int32 qcindex 2;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  \\}\n" +
"  longitude \\{\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.0, 351.15;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    String axis \"X\";\n" +
"    Float32 data_precision -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String observation_type \"measured\";\n" +
"    String original_units \"degrees \\(-W/\\+E\\)\";\n" +
"    Int32 qcindex 3;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  \\}\n" +
"  airPressure \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 958.88, 1047.82;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 1050.0;\n" +
"    Float64 colorBarMinimum 950.0;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Atmospheric Pressure\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String mslp_indicator \"at sensor height\";\n" +
"    String observation_type \"measured\";\n" +
"    String original_units \"hectopascal\";\n" +
"    Int32 qcindex 11;\n" +
"    Float32 sampling_rate 0.1;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"air_pressure\";\n" +
"    String units \"millibar\";\n" +
"  \\}\n" +
"  airTemperature \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range -13.77, 48.07;\n" +  //before 2013-08-28 was 18.97
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temperature\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"measured\";\n" +
"    Int32 qcindex 12;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  \\}\n" +
"  conductivity \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 5.555556E7;\n" + //2013-03-26 new value is nonsense.  was 4.78
"    String average_center \"unknown\";\n" + //2014-01-09 several lines disappeared
"    Int16 average_length 60;\n" +           //2014-08-11 they returned
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 4.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Conductivity\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"measured\";\n" +
"    String original_units \"siemens meter-1\";\n" +
"    Int32 qcindex 16;\n" +
"    Float32 sampling_rate -9999.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"sea_water_electrical_conductivity\";\n" +
"    String units \"siemens meter-1\";\n" +
"  \\}\n" +
"  relativeHumidity \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 24.0, 101.0;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Relative Humidity\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"measured\";\n" +
"    String original_units \"percent\";\n" +
"    Int32 qcindex 13;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"relative_humidity\";\n" +
"    String units \"percent\";\n" +
"  \\}\n" +
"  salinity \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 7777777.0;\n" + //2013-03-26 nonsense!  was 9672.92
"    String average_center \"unknown\";\n" + //2014-01-09 several lines disappeared
"    Int16 average_length -9999;\n" +        //2014-08-11 they returned  
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    Int32 data_interval 60;\n" +
"    Float32 data_precision -9999.0;\n" +  //2014-12-08 several lines disappeared
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Salinity\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"calculated\";\n" +
"    String original_units \"PSU\";\n" +
"    Int32 qcindex 15;\n" +
"    Float32 sampling_rate -9999.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"sea_water_salinity\";\n" +
"    String units \"PSU\";\n" +
"  \\}\n" +
"  seaTemperature \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range -1.1, 7777777.0;\n" +  //nonsense!
"    String average_center \"time at end of period\";\n" + //2014-01-09 several lines disappeared
"    Int16 average_length 60;\n" +                         //2014-08-11 they returned
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"measured\";\n" +
"    String original_units \"celsius\";\n" +
"    Int32 qcindex 14;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    Int16 ts_sensor_category 12;\n" +
"    String units \"degree_C\";\n" +
"  \\}\n" +
"  windDirection \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 360.0;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 data_interval -9999;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Earth Relative Wind Direction\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"calculated\";\n" +
"    String original_units \"degrees \\(clockwise from true north\\)\";\n" +
"    Int32 qcindex 6;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"degrees \\(clockwise from true north\\)\";\n" +
"  \\}\n" +
"  windSpeed \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 2850253.0;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 data_interval -9999;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Earth Relative Wind Speed\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"calculated\";\n" +
"    String original_units \"knot\";\n" +
"    Int32 qcindex 9;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"meter second-1\";\n" +
"  \\}\n" +
"  platformCourse \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 360.0;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Float32 data_precision -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Platform Course\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"calculated\";\n" +
"    String original_units \"degrees \\(clockwise towards true north\\)\";\n" +
"    Int32 qcindex 5;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String units \"degrees_true\";\n" +
"  \\}\n" +
"  platformHeading \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 360.0;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Float32 data_precision -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Platform Heading\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"calculated\";\n" +
"    String original_units \"degrees \\(clockwise towards true north\\)\";\n" +
"    Int32 qcindex 4;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String units \"degrees_true\";\n" +
"  \\}\n" +
"  platformSpeed \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 2850255.0;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Float32 data_precision -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Platform Speed Over Ground\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"calculated\";\n" +
"    String original_units \"knot\";\n" +
"    Int32 qcindex 8;\n" +
"    Float32 sampling_rate 0.5;\n" +
"    Float32 special_value -8888.0;\n" +
"    String units \"meter second-1\";\n" +
"  \\}\n" +
"  platformWindDirection \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 360.0;\n" +
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 data_interval -9999;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Platform Relative Wind Direction\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"measured\";\n" +
"    String original_units \"degrees \\(clockwise from bow\\)\";\n" +
"    Int32 qcindex 7;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"degrees \\(clockwise from bow\\)\";\n" +
"    Float32 zero_line_reference -9999.0;\n" +
"  \\}\n" +
"  platformWindSpeed \\{\n" +
"    Float32 _FillValue -8888.0;\n" +
"    Float32 actual_range 0.0, 180.2509;\n" + //before 2013-08-28 was 36.09545
"    String average_center \"time at end of period\";\n" +
"    Int16 average_length 60;\n" +
"    String average_method \"average\";\n" +
"    Float32 centerline_offset -9999.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 data_interval -9999;\n" +
"    Float32 data_precision -9999.0;\n" +
"    Float32 distance_from_bow -9999.0;\n" +
"    Float32 height -9999.0;\n" +
"    String instrument \"unknown\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Platform Relative Wind Speed\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String observation_type \"measured\";\n" +
"    String original_units \"knot\";\n" +
"    Int32 qcindex 10;\n" +
"    Float32 sampling_rate 1.0;\n" +
"    Float32 special_value -8888.0;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"meter second-1\";\n" +
"  \\}\n" +
"  flag \\{\n" +
"    String A \"Units added\";\n" +
"    String B \"Data out of range\";\n" +
"    String C \"Non-sequential time\";\n" +
"    String D \"Failed T>=Tw>=Td\";\n" +
"    String DODS_dimName \"f_string\";\n" +
"    Int32 DODS_strlen \\d\\d;\n" +   //changes: 13, 16
"    String E \"True wind error\";\n" +
"    String F \"Velocity unrealistic\";\n" +
"    String G \"Value > 4 s. d. from climatology\";\n" +
"    String H \"Discontinuity\";\n" +
"    String I \"Interesting feature\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String J \"Erroneous\";\n" +
"    String K \"Suspect - visual\";\n" +
"    String L \"Ocean platform over land\";\n" +
"    String long_name \"Quality Control Flags\";\n" +
"    String M \"Instrument malfunction\";\n" +
"    String N \"In Port\";\n" +
"    String O \"Multiple original units\";\n" +
"    String P \"Movement uncertain\";\n" +
"    String Q \"Pre-flagged as suspect\";\n" +
"    String R \"Interpolated data\";\n" +
"    String S \"Spike - visual\";\n" +
"    String T \"Time duplicate\";\n" +
"    String U \"Suspect - statistial\";\n" +
"    String V \"Spike - statistical\";\n" +
"    String X \"Step - statistical\";\n" +
"    String Y \"Suspect between X-flags\";\n" +
"    String Z \"Good data\";\n" +
"  \\}\n" +
" \\}\n" +
"  NC_GLOBAL \\{\n" +
"    String cdm_data_type \"Point\";\n" +
"    String contact_email \"samos@coaps.fsu.edu\";\n" +
"    String contact_info \"Center for Ocean-Atmospheric Prediction Studies, The Florida State University, Tallahassee, FL, 32306-2840, USA\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"samos@coaps.fsu.edu\";\n" +
"    String creator_name \"Shipboard Automated Meteorological and Oceanographic System \\(SAMOS\\)\";\n" +
"    String creator_url \"http://samos.coaps.fsu.edu/html/\";\n" +
"    String Data_modification_date \".{19} E.T\";\n" + //changes
"    String data_provider \"unknown at this time\";\n" +
"    Float64 Easternmost_Easting 351.15;\n" +
"    Int16 elev 0;\n" +
"    String featureType \"Point\";\n" +
"    String fsu_version \"300\";\n" +
"    Float64 geospatial_lat_max 70.05856;\n" +
"    Float64 geospatial_lat_min -46.45;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 351.15;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today;
        String seek = "String history \"" + today;
        int tPo = results.indexOf(seek);
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + seek.length()), expected,
            "\nresults=\n" + results);

//+ " http://coaps.fsu.edu/thredds/catalog/samos/data/research/WTEP/catalog.xml\n" +
//today + " http://127.0.0.1:8080/cwexperimental/tabledap/
expected = 
"fsuNoaaShipWTEP.das\";\n" +
"    String infoUrl \"http://samos.coaps.fsu.edu/html/\";\n" +
"    String institution \"FSU\";\n" +
"    String keywords \"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Surface Air Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Humidity,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Salinity/Density > Conductivity,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"air, air_pressure, air_temperature, atmosphere, atmospheric, calender, conductivity, control, course, data, date, day, density, direction, dyson, earth, electrical, file, flags, from, fsu, ground, heading, history, humidity, information, level, measurements, meteorological, meteorology, oceans, oscar, over, platform, pressure, quality, relative, relative_humidity, salinity, sea, sea_water_electrical_conductivity, sea_water_salinity, seawater, speed, static, surface, temperature, time, vapor, water, wind, wind_from_direction, wind_speed, winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_modification_date \".{19} E.T\";\n" + //changes
"    Float64 Northernmost_Northing 70.05856;\n" +
"    String receipt_order \"01\";\n" +
"    String sourceUrl \"http://coaps.fsu.edu/thredds/catalog/samos/data/research/WTEP/catalog.xml\";\n" +
"    Float64 Southernmost_Northing -46.45;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v27\";\n" +
"    String subsetVariables \"cruise_id, expocode, facility, ID, IMO, platform, platform_version, site\";\n" +
"    String summary \"NOAA Ship Oscar Dyson Underway Meteorological Data " +
    "\\(delayed ~10 days for quality control\\) are from the Shipboard " +
    "Automated Meteorological and Oceanographic System \\(SAMOS\\) program.\n" +
"\n" +
"IMPORTANT: ALWAYS USE THE QUALITY FLAG DATA! Each data variable's metadata " +
    "includes a qcindex attribute which indicates a character number in the " +
    "flag data.  ALWAYS check the flag data for each row of data to see which " +
    "data is good \\(flag='Z'\\) and which data isn't.  For example, to extract " +
    "just data where time \\(qcindex=1\\), latitude \\(qcindex=2\\), longitude " +
    "\\(qcindex=3\\), and airTemperature \\(qcindex=12\\) are 'good' data, " +
    "include this constraint in your ERDDAP query:\n" +
"  flag=~\\\\\"ZZZ........Z.*\\\\\"\n" +
"in your query.\n" +
"'=~' indicates this is a regular expression constraint.\n" +
"The 'Z's are literal characters.  In this dataset, 'Z' indicates 'good' data.\n" +
"The '\\.'s say to match any character.\n" +
"The '\\*' says to match the previous character 0 or more times.\n" +
"\\(Don't include backslashes in your query.\\)\n" +
"See the tutorial for regular expressions at\n" +
"http://www.vogella.de/articles/JavaRegularExpressions/article.html\";\n" +
"    String time_coverage_end \"20.{8}T.{8}Z\";\n" +  //changes
"    String time_coverage_start \"2007-09-01T00:00:00Z\";\n" +
"    String title \"NOAA Ship Oscar Dyson Underway Meteorological Data, Quality Controlled\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  \\}\n" +
"\\}\n";
            tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureLinesMatch(results.substring(tPo), expected, "results=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        //*** test getting dds for entire dataset
        try{
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_ShipEntire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String cruise_id;\n" +
"    String expocode;\n" +
"    String facility;\n" +
"    String ID;\n" +
"    String IMO;\n" +
"    String platform;\n" +
"    String platform_version;\n" +
"    String site;\n" +
"    Float64 time;\n" +
"    Float32 latitude;\n" +
"    Float32 longitude;\n" +
"    Float32 airPressure;\n" +
"    Float32 airTemperature;\n" +
"    Float32 conductivity;\n" +
"    Float32 relativeHumidity;\n" +
"    Float32 salinity;\n" +
"    Float32 seaTemperature;\n" +
"    Float32 windDirection;\n" +
"    Float32 windSpeed;\n" +
"    Float32 platformCourse;\n" +
"    Float32 platformHeading;\n" +
"    Float32 platformSpeed;\n" +
"    Float32 platformWindDirection;\n" +
"    Float32 platformWindSpeed;\n" +
"    String flag;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        //*** test make data files
        String2.log("\n****************** EDDTableFromThreddsFiles.testShipWTEP make DATA FILES\n");       

        //.csv    for one lat,lon,time
        try {
        userDapQuery = "cruise_id&distinct()";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_ShipCruiseList", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"cruise_id\n" +
"\n" +
"Cruise_id undefined for now\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        //.csv    
        userDapQuery = "time,latitude,longitude,airPressure,airTemperature,flag&time%3E=2012-01-29T19:30:00Z&time%3C=2012-01-29T19:34:00Z";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Ship1StationGTLT", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);   
        expected = 
"time,latitude,longitude,airPressure,airTemperature,flag\n" +
"UTC,degrees_north,degrees_east,millibar,degree_C,\n" +
"2012-01-29T19:30:00Z,48.47,235.11,1009.1,9.01,ZZZZZZZZZZZZBZZZ\n" +
"2012-01-29T19:31:00Z,48.47,235.11,1009.05,8.94,ZZZZZZZZZZZZBZZZ\n" +
"2012-01-29T19:32:00Z,48.47,235.12,1009.03,8.9,ZZZZZZZZZZZZBZZZ\n" +
"2012-01-29T19:33:00Z,48.47,235.12,1009.03,8.93,ZZZZZZZZZZZZBZZZ\n" +
"2012-01-29T19:34:00Z,48.47,235.13,1009.02,8.96,ZZZZZZZZZZZZBZZZ\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

    }

    /**
     * This tests getThreddsFileInfo.
     * @throws Throwable if trouble
     */
    public static void testGetThreddsFileInfo() throws Throwable {

        String2.log("\n*** EDDTableFromThredds.testGetThreddsFileInfo");
        String results, expected;
        
        StringArray fileDir     = new StringArray();
        StringArray fileName    = new StringArray();
        LongArray   fileLastMod = new LongArray();

        //*** test http://coaps.fsu.edu/thredds/catalog/samos/data/quick/WTEP/2011/catalog.html
        if (true) {
        getThreddsFileInfo(
            "http://coaps.fsu.edu/thredds/catalog/samos/data/quick/WTEP/catalog.xml", 
            "WTEP_2011082.*\\.nc", true, //recursive
            fileDir, fileName, fileLastMod);

        results = fileDir.toNewlineString();
        expected = 
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n" +
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n" +
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n" +
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n" +
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n" +
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n" +
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n" +
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n" +
"http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        results = fileName.toNewlineString();
        expected = 
"WTEP_20110821v10001.nc\n" +
"WTEP_20110822v10001.nc\n" +
"WTEP_20110823v10001.nc\n" +
"WTEP_20110824v10001.nc\n" +
"WTEP_20110825v10001.nc\n" +
"WTEP_20110826v10001.nc\n" +
"WTEP_20110827v10001.nc\n" +
"WTEP_20110828v10001.nc\n" +
"WTEP_20110829v10001.nc\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fileLastMod.size(); i++)
            sb.append(Calendar2.millisToIsoZuluString(fileLastMod.get(i)) + "\n");
        results = sb.toString();
        expected = 
"2011-08-22T00:02:00\n" +
"2011-08-23T00:03:13\n" +
"2011-08-24T00:04:06\n" +
"2011-08-25T00:02:02\n" +
"2011-08-26T00:03:13\n" +
"2011-08-27T00:04:46\n" +
"2011-08-28T00:05:25\n" +
"2011-08-29T00:03:35\n" +
"2011-08-30T00:05:39\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        }


        //*** test http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/SMS/catalog.xml
        if (true) {
        fileDir.clear();
        fileName.clear();
        fileLastMod.clear();        
        getThreddsFileInfo(
            "http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/SMS/catalog.xml", 
            "SMS.*_2004.*\\.nc", true, //recursive
            fileDir, fileName, fileLastMod);

        results = fileDir.toNewlineString();
        expected = 
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n" +
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n" +
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n" +
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n" +
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n" +
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n" +
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        results = fileName.toNewlineString();
        expected = 
"SMSXXX_015ADCP015R00_20041130.nc\n" +
"SMSXXX_015MTBD003R00_20041011.nc\n" +
"SMSXXX_015MTBD003R00_20041122.nc\n" +
"SMSXXX_015MTBD009R00_20041011.nc\n" +
"SMSXXX_015MTBD009R00_20041122.nc\n" +
"SMSXXX_015MTBD014R00_20041011.nc\n" +
"SMSXXX_015MTBD014R00_20041122.nc\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fileLastMod.size(); i++)
            sb.append(Calendar2.millisToIsoZuluString(fileLastMod.get(i)) + "\n");
        results = sb.toString();
        expected = 
"2012-05-18T05:42:56\n" +
"2012-05-11T01:03:50\n" +
"2012-05-12T14:37:14\n" +
"2012-05-14T12:55:06\n" +
"2012-05-10T07:25:54\n" +
"2012-05-11T17:41:56\n" +
"2012-05-14T20:46:04\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        }

        String2.log("\n*** EDDTableFromThredds.testGetThreddsFileInfo finished.");
    }




    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedInfo) throws Throwable {

        //usually run
/* */
        testGetThreddsFileInfo();
        testGenerateDatasetsXml();
        testWcosTemp(deleteCachedInfo);  
        testShipWTEP(deleteCachedInfo);
 
        //not usually run

    }
}

