/* 
 * EDDTableCopy Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;

import java.util.ArrayList;
import java.util.Enumeration;


/** 
 * This class makes and maintains a local copy of the data from a remote source.
 * This class serves data from the local copy.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2009-05-19
 */
public class EDDTableCopy extends EDDTable{ 

    protected EDDTable sourceEdd;
    protected EDDTableFromFiles localEdd;

    /** Some tests set EDDTableCopy.defaultCheckSourceData = false; 
     *  Don't set it here.
     */
    public static boolean defaultCheckSourceData = true; 

    protected static int maxChunks = Integer.MAX_VALUE;  //some test methods reduce this

    /**
     * This constructs an EDDTableCopy based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableCopy"&gt; 
     *    having just been read.  
     * @return an EDDTableCopy.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableCopy fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableCopy(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        EDDTable tSourceEdd = null;
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        String tExtractDestinationNames = "";
        String tOrderExtractBy = "";
        boolean checkSourceData = defaultCheckSourceData;
        boolean tSourceNeedsExpandedFP_EQ = true;
        boolean tFileTableInMemory = false;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        boolean tAccessibleViaFiles = false;

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            //if (reallyVerbose) String2.log("  tags=" + tags + content);
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if      (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<sosOfferingPrefix>")) {}
            else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content; 
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<extractDestinationNames>")) {}
            else if (localTags.equals("</extractDestinationNames>")) tExtractDestinationNames = content; 
            else if (localTags.equals( "<orderExtractBy>")) {}
            else if (localTags.equals("</orderExtractBy>")) tOrderExtractBy = content; 
            else if (localTags.equals( "<checkSourceData>")) {}
            else if (localTags.equals("</checkSourceData>")) checkSourceData = String2.parseBoolean(content); 
            else if (localTags.equals( "<sourceNeedsExpandedFP_EQ>")) {}
            else if (localTags.equals("</sourceNeedsExpandedFP_EQ>")) tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content); 
            else if (localTags.equals( "<fileTableInMemory>")) {}
            else if (localTags.equals("</fileTableInMemory>")) tFileTableInMemory = String2.parseBoolean(content); 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 
            else if (localTags.equals( "<accessibleViaFiles>")) {}
            else if (localTags.equals("</accessibleViaFiles>")) tAccessibleViaFiles = String2.parseBoolean(content); 
            else if (localTags.equals("<dataset>")) {
                try {

                    if (checkSourceData) {
                        //after first time, it's ok if source dataset isn't available
                        tSourceEdd = (EDDTable)EDD.fromXml(xmlReader.attributeValue("type"), xmlReader);
                    } else {
                        String2.log("WARNING!!! checkSourceData is false, so EDDTableCopy datasetID=" + 
                            tDatasetID + " is not checking the source dataset!");
                        int stackSize = xmlReader.stackSize();
                        do {  //will throw Exception if trouble (e.g., unexpected end-of-file
                            xmlReader.nextTag();
                        } while (xmlReader.stackSize() != stackSize); 
                        tSourceEdd = null;
                    }
                } catch (Throwable t) {
                    String2.log(MustBe.throwableToString(t));
                }
            } 
            else xmlReader.unexpectedTagException();
        }

        return new EDDTableCopy(tDatasetID, 
            tAccessibleTo, tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tReloadEveryNMinutes, 
            tExtractDestinationNames, tOrderExtractBy, tSourceNeedsExpandedFP_EQ,
            tSourceEdd, tFileTableInMemory, tAccessibleViaFiles);
    }

    /**
     * The constructor.
     *
     * @param tDatasetID is a very short string identifier 
     *   (required: just safe characters: A-Z, a-z, 0-9, _, -, or .)
     *   for this dataset. See EDD.datasetID().
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with "http://" or "mailto:")
     *    to be done whenever the dataset changes significantly
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tExtractDestinationNames a space separated list of destination names (at least one)
     *     that describe how chunks of source data will be extracted/copied.
     *     For example, "pi commonName surgeryId" indicates that for each distinct
     *     combination of pi+surgeryId values, data will be extracted and put in a file:
     *     [bigParentDirectory]/copiedData/datasetID/piValue/commonNameValue/surgeryIdValue.nc
     *    <p>This is also used as EDDTableFromNcFiles tSortFilesByDestinationNames:
     *    a space-separated list of destination variable names (because it's in localEdd) 
     *    specifying how the internal list of files should be sorted (in ascending order).
     *    <br>When a data request is filled, data is obtained from the files in this order.
     *    <br>Thus it largely determines the overall order of the data in the response.
     * @param tOrderExtractBy are the space-separated destination (because it's in localEdd) names
     *    specifying how to sort each extract subset file.
     *    This is optional (use null or ""), but almost always used.
     *    Ideally, the first name is a numeric variable 
     *    (this can greatly speed up some data requests).
     *    <p>Note that the combination of extractDestinationNames+orderExtractBy should
     *    fully define the desired sort order for the dataset.
     * @param tSourceNeedsExpandedFP_EQ
     * @param tSourceEdd the remote dataset to be copied.
     *   After the first time (to generate tasks to copy data), 
     *   there will be local files so it's okay if tSourceEdd is null (unavailable).
     * @throws Throwable if trouble
     */
    public EDDTableCopy(String tDatasetID, 
        String tAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        int tReloadEveryNMinutes,
        String tExtractDestinationNames, String tOrderExtractBy,
        Boolean tSourceNeedsExpandedFP_EQ,
        EDDTable tSourceEdd, boolean tFileTableInMemory,
        boolean tAccessibleViaFiles) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableCopy " + tDatasetID + " reallyVerbose=" + reallyVerbose); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableCopy(" + 
            tDatasetID + ") constructor:\n";
            
        //save the parameters
        className = "EDDTableCopy"; 
        datasetID = tDatasetID;
        sourceEdd = tSourceEdd;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        setReloadEveryNMinutes(tReloadEveryNMinutes);

        //check some things
        if (tSourceEdd instanceof EDDTableFromThreddsFiles) 
            throw new IllegalArgumentException("datasets.xml error: " +
                "EDDTableFromThreddsFiles makes its own local copy of " +
                "the data, so it MUST NEVER be enclosed by EDDTableCopy (" + datasetID + ").");
        if (tSourceEdd instanceof EDDTableFromHyraxFiles) 
            throw new IllegalArgumentException("datasets.xml error: " +
                "EDDTableFromHyraxFiles makes its own local copy of " +
                "the data, so it MUST NEVER be enclosed by EDDTableCopy (" + datasetID + ").");
        if (tExtractDestinationNames.indexOf(',') >= 0)
            throw new IllegalArgumentException("datasets.xml error: " +
                "extractDestinationNames should be space separated, not comma separated.");
        if (tOrderExtractBy != null && tOrderExtractBy.indexOf(',') >= 0)
            throw new IllegalArgumentException("datasets.xml error: " +
                "orderExtractBy should be space separated, not comma separated.");
        StringArray orderExtractBy = null;
        if (tOrderExtractBy != null &&
            tOrderExtractBy.length() > 0)
            orderExtractBy = StringArray.wordsAndQuotedPhrases(tOrderExtractBy);
        if (reallyVerbose) String2.log("orderExtractBy=" + orderExtractBy);

        //ensure copyDatasetDir exists
        String copyDatasetDir = EDStatic.fullCopyDirectory + datasetID + "/";
        File2.makeDirectory(copyDatasetDir);

        //assign copy tasks to taskThread
        if (sourceEdd != null) {
            int taskNumber = -1; //i.e. unused
            try {
                //check if taskThread has finished previously assigned tasks for this dataset
                EDStatic.ensureTaskThreadIsRunningIfNeeded();  //ensure info is up-to-date
                Integer lastAssignedTask = (Integer)EDStatic.lastAssignedTask.get(datasetID);
                boolean pendingTasks = lastAssignedTask != null &&  
                    EDStatic.lastFinishedTask < lastAssignedTask.intValue();
                if (verbose) 
                    String2.log("  lastFinishedTask=" + EDStatic.lastFinishedTask + 
                        " < lastAssignedTask(" + tDatasetID + ")=" + lastAssignedTask + 
                        "? pendingTasks=" + pendingTasks);
                if (!pendingTasks) {

                    //get the distinct() combination of values for tExtractDestinationNames
                    StringArray extractNames = StringArray.wordsAndQuotedPhrases(tExtractDestinationNames);
                    if (extractNames.size() == 0)
                        throw new RuntimeException("datasets.xml error: " +
                            "There are no extractDestinationNames.");
                    if (reallyVerbose) String2.log("extractNames=" + extractNames);
                    String query = String2.replaceAll(extractNames.toString(), " ", "") + 
                        "&distinct()";
                    String cacheDir = cacheDirectory();
                    File2.makeDirectory(cacheDir);  //ensure it exists
                    TableWriterAll twa = new TableWriterAll(cacheDir, "extract");
                    TableWriter tw = encloseTableWriter(cacheDir, "extractDistinct", twa, query); 
                    sourceEdd.getDataForDapQuery(EDStatic.loggedInAsSuperuser, 
                        "", query, tw); //"" is requestUrl, not relevant here
                    Table table = twa.cumulativeTable();
                    tw = null;
                    twa.releaseResources();
                    int nRows = table.nRows();  //nRows = 0 will throw an exception above
                    if (verbose) String2.log("source nChunks=" + nRows);
                    nRows = Math.min(maxChunks, nRows); 
                    int nCols = table.nColumns();
                    boolean isString[] = new boolean[nCols];
                    for (int col = 0; col < nCols; col++) 
                        isString[col] = table.getColumn(col).elementClass() == String.class;

                    //make a task for each row (if the file doesn't already exist)
                    for (int row = 0; row < nRows; row++) {
                        //gather the query, fileDir, fileName, ... for the task
                        //[fullCopyDirectory]/datasetID/piValue/commonNameValue/surgeryIdValue.nc
                        StringBuilder fileDir = new StringBuilder(copyDatasetDir);
                        for (int col = 0; col < nCols - 1; col++) {  //-1 since last part is for file name, not dir
                            String s = table.getStringData(col, row);
                            fileDir.append(String2.encodeFileNameSafe(s) + "/");
                        }

                        StringBuilder tQuery = new StringBuilder(); 
                        for (int col = 0; col < nCols; col++) {
                            String s = table.getStringData(col, row);
                            tQuery.append("&" + table.getColumnName(col) + "=" + 
                                (isString[col]? String2.toJson(s) : 
                                 "".equals(s)? "NaN" :
                                 s));
                        }

                        //does the file already exist
                        String fileName = String2.encodeFileNameSafe(table.getStringData(nCols-1, row));
                        if (File2.isFile(fileDir.toString() + fileName + ".nc")) {
                            if (reallyVerbose) String2.log("  file already exists: " + fileDir + fileName + ".nc");
                            continue;
                        }
                        File2.makeDirectory(fileDir.toString());
                        if (orderExtractBy != null)
                            tQuery.append("&orderBy(\"" + 
                                String2.replaceAll(orderExtractBy.toString(), " ", "") + "\")");

                        //make the task
                        Object taskOA[] = new Object[6];
                        taskOA[0] = TaskThread.TASK_MAKE_A_DATAFILE;
                        taskOA[1] = sourceEdd;
                        taskOA[2] = tQuery.toString();  //string, not StringBuilder
                        taskOA[3] = fileDir.toString(); //string, not StringBuilder
                        taskOA[4] = fileName;
                        taskOA[5] = ".nc";
                        int tTaskNumber = EDStatic.addTask(taskOA);
                        if (tTaskNumber >= 0) {
                            taskNumber = tTaskNumber;
                            if (reallyVerbose)
                                String2.log("  task#" + taskNumber + " TASK_MAKE_A_DATAFILE " + tQuery.toString() + "\n    " +
                                    fileDir.toString() + fileName + ".nc");
                        }
                    }

                    //create task to flag dataset to be reloaded
                    if (taskNumber > -1) {
                        Object taskOA[] = new Object[2];
                        taskOA[0] = TaskThread.TASK_SET_FLAG;
                        taskOA[1] = datasetID;
                        taskNumber = EDStatic.addTask(taskOA); //TASK_SET_FLAG will always be added
                        if (reallyVerbose)
                            String2.log("  task#" + taskNumber + " TASK_SET_FLAG " + datasetID);
                    }
                }
            } catch (Throwable t) {
                String2.log("Error while assigning " + datasetID + " copy tasks to taskThread:\n" +
                    MustBe.throwableToString(t));
            }
            if (taskNumber >= 0) {
                EDStatic.lastAssignedTask.put(datasetID, new Integer(taskNumber));
                EDStatic.ensureTaskThreadIsRunningIfNeeded();  //clients (like this class) are responsible for checking on it
            }
        }

        //gather info about dataVariables to create localEdd
        int nDataVariables;
        Object[][] tDataVariables;
        if (sourceEdd == null) {
            //get info from existing copied datafiles, which is a standard EDDTable)
            //get a list of copied files
            String tFileNames[] = RegexFilenameFilter.recursiveFullNameList(
                copyDatasetDir, ".*\\.nc", false);
            if (tFileNames.length == 0)
                throw new RuntimeException("Warning: There are no copied files in " + 
                    copyDatasetDir +
                    ",\nso localEdd can't be made yet for datasetID=" + datasetID + ".\n" +
                    "But it will probably succeed in next loadDatasets (15 minutes?),\n" +
                    "after some files are copied.");

            //load the table
            String getFromName = File2.getYoungest(tFileNames);
            Table table = new Table();
            String2.log("!!! sourceEDD is unavailable, so getting dataVariable info from youngest file\n" + 
                getFromName);
            table.readFlatNc(getFromName, null, 0);  //null=allVars, 0=data is already unpacked
            nDataVariables = table.nColumns();
            tDataVariables = new Object[nDataVariables][];
            for (int dv = 0; dv < nDataVariables; dv++) {
                tDataVariables[dv] = new Object[]{table.getColumnName(dv), table.getColumnName(dv), 
                    new Attributes(), table.getColumn(dv).elementClassString()};
            }
        } else {
            //get info from sourceEdd, which is a standard EDDTable
            nDataVariables = sourceEdd.dataVariables.length;
            tDataVariables = new Object[nDataVariables][];
            for (int dv = 0; dv < nDataVariables; dv++) {
                EDV edv = sourceEdd.dataVariables[dv];
                tDataVariables[dv] = new Object[]{edv.destinationName(), edv.destinationName(), 
                    new Attributes(), edv.destinationDataType()};  //2012-07-26 e.g., var with scale_factor will be destType in the copied files
            }
        }
        //if the first orderExtractBy column is numeric, it can be used as 
        //  EDDTableFromFiles sortedColumn (the basis of faster searches within a file);
        //  otherwise it can't be used (not a big deal)
        String sortedColumn = orderExtractBy == null? "" : orderExtractBy.get(0); //the first column
        if (sortedColumn.length() > 0) {
            for (int dv = 0; dv < nDataVariables; dv++) {
                if (sortedColumn.equals((String)tDataVariables[dv][0]) && //columnName
                    "String".equals((String)tDataVariables[dv][3])) {     //columnType
                    if (verbose) String2.log("orderExtractBy #0=" + sortedColumn + 
                        " can't be used as EDDTableFromFiles sortedColumn because it isn't numeric.");
                    sortedColumn = "";
                    break;
                }
            }
        }

        //make localEdd
        //It will fail if 0 local files -- that's okay, TaskThread will continue to work 
        //  and constructor will try again in 15 min.
        boolean recursive = true;
        String fileNameRegex = ".*\\.nc";
        localEdd = makeLocalEdd(datasetID, 
            tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, 
            new Attributes(), //addGlobalAttributes
            tDataVariables,
            tReloadEveryNMinutes,
            copyDatasetDir, recursive, fileNameRegex, 
            EDDTableFromFiles.MF_LAST,
            "", 1, 2, //columnNamesRow and firstDataRow are irrelevant for .nc files, but must be valid values
            null, null, null, null,  //extract from fileNames
            sortedColumn, 
            tExtractDestinationNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory); 

        //copy things from localEdd 
        sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
        sourceCanConstrainNumericData = localEdd.sourceCanConstrainNumericData;
        sourceCanConstrainStringData  = localEdd.sourceCanConstrainStringData;
        sourceCanConstrainStringRegex = localEdd.sourceCanConstrainStringRegex;
      
        sourceGlobalAttributes   = localEdd.combinedGlobalAttributes;
        addGlobalAttributes      = new Attributes();
        combinedGlobalAttributes = localEdd.combinedGlobalAttributes; //new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important

        //copy data variables
        dataVariables   = localEdd.dataVariables;
        lonIndex        = localEdd.lonIndex;
        latIndex        = localEdd.latIndex;
        altIndex        = localEdd.altIndex;
        depthIndex      = localEdd.depthIndex;
        timeIndex       = localEdd.timeIndex;

        //copy sos info
        sosOfferingType  = localEdd.sosOfferingType;
        sosOfferingIndex = localEdd.sosOfferingIndex;
        sosOfferings     = localEdd.sosOfferings;
        sosMinTime       = localEdd.sosMinTime; 
        sosMaxTime       = localEdd.sosMaxTime; 
        sosMinLat        = localEdd.sosMinLat; 
        sosMaxLat        = localEdd.sosMaxLat; 
        sosMinLon        = localEdd.sosMinLon; 
        sosMaxLon        = localEdd.sosMaxLon;

        //accessibleViaFiles
        if (EDStatic.filesActive && tAccessibleViaFiles) {
            accessibleViaFilesDir = copyDatasetDir;
            accessibleViaFilesRegex = fileNameRegex;
            accessibleViaFilesRecursive = recursive;
        }

        //ensure the setup is valid
        ensureValid(); //this ensures many things are set, e.g., sourceUrl

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableCopy " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }

    /**
     * This is used by the constructor to make localEDD.
     * This version makes an EDDTableFromNcFiles, but subclasses
     * use it to make subsclasses of EDDTableFromFiles, e.g., EDDTableFromPostNcFiles.
     *
     * <p>It will fail if 0 local files -- that's okay, TaskThread will continue to work 
     *  and constructor will try again in 15 min.
     */
    EDDTableFromFiles makeLocalEdd(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames, 
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory) 
        throws Throwable {

        return new EDDTableFromNcFiles(tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, 
            "", "", "", //tSosOfferingPrefix, tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, 0, //updateEveryNMillis
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, 
            tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
            false); //accessibleViaFiles is always false. parent may or may not be.  
    }


    /** This gets changed info from localEdd.changed. */
    public String changed(EDD old) {
        return localEdd.changed(old);
    }

    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param tableWriter
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, String userDapQuery, 
        TableWriter tableWriter) throws Throwable {

        localEdd.getDataForDapQuery(loggedInAs, requestUrl, userDapQuery, tableWriter);
    }

    /**
     * The basic tests of this class (erdGlobecBottle).
     * 
     */
    public static void testBasic() throws Throwable {
        testVerboseOn();

        String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
        String error = "";
        int epo, tPo;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&time>=2002-08-03";
        userDapQuery = "longitude,NO3,time,ship&latitude>0&time>=2002-08-03";

        try {
            EDDTable edd = (EDDTableCopy)oneFromDatasetXml("testTableCopy");

            //*** test getting das for entire dataset
            String2.log("\n****************** EDDTableCopy.test das dds for entire dataset\n");
            tName = edd.makeNewFileForDapQuery(null, null, "", 
                EDStatic.fullTestCacheDirectory, edd.className() + "_Entire", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = //see OpendapHelper.EOL for comments
"Attributes {\n" +
" s {\n" +
"  cruise_id {\n" +
"    String cf_role \"trajectory_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cruise ID\";\n" +
"  }\n" +
"  ship {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Ship\";\n" +
"  }\n" +
"  cast {\n" +
"    Int16 _FillValue 32767;\n" +
"    Int16 actual_range 1, 127;\n" +
"    Float64 colorBarMaximum 140.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cast Number\";\n" +
"    Int16 missing_value 32767;\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range -126.2, -124.1;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range 41.9, 44.65;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 _FillValue NaN;\n" +
"    Float64 actual_range 1.02272886e+9, 1.02978828e+9;\n" +
"    String axis \"T\";\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"time\";\n" +  
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  bottle_posn {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    Byte _FillValue 127;\n" +
"    Byte actual_range 0, 12;\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 12.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Bottle Number\";\n" +
"    Byte missing_value -128;\n" +
"  }\n" +
"  chl_a_total {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -2.602, 40.17;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll-a\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"ug L-1\";\n" +
"  }\n" +
"  chl_a_10um {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.21, 11.495;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll-a after passing 10um screen\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"ug L-1\";\n" +
"  }\n" +
"  phaeo_total {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -3.111, 33.821;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Total Phaeopigments\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"ug L-1\";\n" +
"  }\n" +
"  phaeo_10um {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.071, 5.003;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Phaeopigments 10um\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"ug L-1\";\n" +
"  }\n" +
"  sal00 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 28.3683, 34.406;\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Salinity from T0 and C0 Sensors\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"sea_water_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
"  sal11 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 34.4076;\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Salinity from T1 and C1 Sensors\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"sea_water_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
"  temperature0 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 3.6186, 16.871;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature from T0 Sensor\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  temperature1 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 3.6179, 16.863;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature from T1 Sensor\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  fluor_v {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.046, 5.0;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Fluorescence Voltage\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"volts\";\n" +
"  }\n" +
"  xmiss_v {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.493, 4.638;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Optical Properties\";\n" +
"    String long_name \"Transmissivity Voltage\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"volts\";\n" +
"  }\n" +
"  PO4 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.07, 3.237;\n" +
"    Float64 colorBarMaximum 4.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Phosphate\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_phosphate_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  N_N {\n" +
"    Float32 _FillValue -99.0;\n" +
"    Float32 actual_range -0.1, 43.47;\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Nitrate plus Nitrite\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  NO3 {\n" +
"    Float32 _FillValue -99.0;\n" +
"    Float32 actual_range 0.0, 99.79;\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Nitrate\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_nitrate_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  Si {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -0.08, 117.12;\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Silicate\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_silicate_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  NO2 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -0.03, 0.757;\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Nitrite\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_nitrite_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  NH4 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -0.14, 4.93;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Ammonium\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_ammonium_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  oxygen {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.07495, 9.93136;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved O2\";\n" +
"    String long_name \"Oxygen\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"volume_fraction_of_oxygen_in_sea_water\";\n" +
"    String units \"mL L-1\";\n" +
"  }\n" +
"  par {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.1515, 3.261;\n" +
"    Float64 colorBarMaximum 3.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Photosynthetically Active Radiation\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"volts\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_altitude_proxy \"bottle_posn\";\n" +
"    String cdm_data_type \"TrajectoryProfile\";\n" +
"    String cdm_profile_variables \"cast, longitude, latitude, time\";\n" +
"    String cdm_trajectory_variables \"cruise_id, ship\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -124.1;\n" +
"    String featureType \"TrajectoryProfile\";\n" +
"    Float64 geospatial_lat_max 44.65;\n" +
"    Float64 geospatial_lat_min 41.9;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -124.1;\n" +
"    Float64 geospatial_lon_min -126.2;\n" +
"    String geospatial_lon_units \"degrees_east\";\n";
//"    String history \"" + today + " 2012-07-29T19:11:09Z (local files; contact erd.data@noaa.gov)\n";  //date is from last created file, so varies sometimes
//today + " http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle.das"; //\n" +
//today + " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " http://127.0.0.1:8080/cwexperimental/tabledap/rGlobecBottle.das\";\n" +
    expected2 = 
"    String infoUrl \"http://www.globec.org/\";\n" +
"    String institution \"GLOBEC\";\n" +
"    String keywords \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 44.65;\n" +
"    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n" +
"    Float64 Southernmost_Northing 41.9;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v27\";\n" +
"    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
"    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
"    String time_coverage_end \"2002-08-19T20:18:00Z\";\n" +
"    String time_coverage_start \"2002-05-30T03:21:00Z\";\n" +
"    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
"    Float64 Westernmost_Easting -126.2;\n" +
"  }\n" +
"}\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            tPo = results.indexOf("    String infoUrl ");
            Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

            
            //*** test getting dds for entire dataset
            tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                edd.className() + "_Entire", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String cruise_id;\n" +
"    String ship;\n" +
"    Int16 cast;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Byte bottle_posn;\n" +
"    Float32 chl_a_total;\n" +
"    Float32 chl_a_10um;\n" +
"    Float32 phaeo_total;\n" +
"    Float32 phaeo_10um;\n" +
"    Float32 sal00;\n" +
"    Float32 sal11;\n" +
"    Float32 temperature0;\n" +
"    Float32 temperature1;\n" +
"    Float32 fluor_v;\n" +
"    Float32 xmiss_v;\n" +
"    Float32 PO4;\n" +
"    Float32 N_N;\n" +
"    Float32 NO3;\n" +
"    Float32 Si;\n" +
"    Float32 NO2;\n" +
"    Float32 NH4;\n" +
"    Float32 oxygen;\n" +
"    Float32 par;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


            //*** test DAP data access form
            tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                edd.className() + "_Entire", ".html"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = "<option>.png - View a standard, medium-sized .png image file with a graph or map.";
            expected2 = "    String _CoordinateAxisType &quot;Lon&quot;;";
            Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);


            //*** test make data files
            String2.log("\n****************** EDDTableCopy.test make DATA FILES\n");       

            //.asc
            tName = edd.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_Data", ".asc"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Dataset {\n" +
    "  Sequence {\n" +
    "    Float32 longitude;\n" +
    "    Float32 NO3;\n" +
    "    Float64 time;\n" +
    "    String ship;\n" +
    "  } s;\n" +
    "} s;\n" +
    "---------------------------------------------\n" +
    "s.longitude, s.NO3, s.time, s.ship\n" +
    "-124.4, 35.7, 1.02833814E9, \"New_Horizon\"\n";
            expected2 = "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"\n"; //row with missing value  has source missing value
            expected3 = "-124.1, 24.45, 1.02978828E9, \"New_Horizon\"\n"; //last row
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf(expected3) > 0, "\nresults=\n" + results); //last row in erdGlobedBottle, not last here


            //.csv
            tName = edd.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_Data", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
            expected2 = "-124.8,NaN,2002-08-03T07:17:00Z,New_Horizon\n"; //row with missing value  has source missing value
            expected3 = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf(expected3) > 0, "\nresults=\n" + results); //last row in erdGlobedBottle, not last here


            //.dds 
            tName = edd.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_Data", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Dataset {\n" +
    "  Sequence {\n" +
    "    Float32 longitude;\n" +
    "    Float32 NO3;\n" +
    "    Float64 time;\n" +
    "    String ship;\n" +
    "  } s;\n" +
    "} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


            //.dods
            //tName = edd.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            //    edd.className() + "_Data", ".dods"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            try {
                String2.log("\ndo .dods test");
                String tUrl = EDStatic.erddapUrl + //in tests, always use non-https url
                    "/tabledap/" + edd.datasetID();
                //for diagnosing during development:
                //String2.log(String2.annotatedString(SSR.getUrlResponseString(
                //    "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt.dods?stn_id&unique()")));
                //String2.log("\nDAS RESPONSE=" + SSR.getUrlResponseString(tUrl + ".das?" + userDapQuery));
                //String2.log("\nDODS RESPONSE=" + String2.annotatedString(SSR.getUrlResponseString(tUrl + ".dods?" + userDapQuery)));

                //test if table.readOpendapSequence works with Erddap opendap server
                //!!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in tests, always use non-https url                
                //!!!THIS IS NOT JUST A LOCAL TEST!!!
                Table tTable = new Table();
                tTable.readOpendapSequence(tUrl + "?" + userDapQuery, false);
                Test.ensureEqual(tTable.globalAttributes().getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");
                Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
                Test.ensureEqual(tTable.getColumnNames(), new String[]{"longitude", "NO3", "time", "ship"}, "");
                Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
                Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
                Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
                Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
                String2.log("  .dods test succeeded");
            } catch (Throwable t) {
                String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                    "\nError accessing " + EDStatic.erddapUrl + //in tests, always use non-https url
                    " and reading erddap as a data source."); 
            }

            //test .png
            tName = edd.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_GraphM", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\n*** This EDDTableCopy test requires testTableCopy"); 
        }

    } //end of testBasic


    /**
     * The tests testRepPostDet.
     * 
     */
    public static void testRepPostDet(boolean tCheckSourceData) throws Throwable {
        String2.log("\n****************** EDDTableCopy.testRepPostDet(tCheckSourceData=" + 
            tCheckSourceData + ") *****************\n");
        testVerboseOn();
        defaultCheckSourceData = tCheckSourceData;
        String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
        String error = "";
        int epo, tPo;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        long eTime;
        EDDTable edd = null;

        try {
//maxChunks = 400;

            try {
                edd = (EDDTableCopy)oneFromDatasetXml("repPostDet");
            } catch (Throwable t2) {
                //it will fail if no files have been copied
                String2.log(MustBe.throwableToString(t2));
            }
            if (tCheckSourceData && EDStatic.nUnfinishedTasks() > 0) {
                while (EDStatic.nUnfinishedTasks() > 0) {
                    String2.log("nUnfinishedTasks=" + EDStatic.nUnfinishedTasks());
                    Math2.sleep(10000);
                }
                //recreate edd to see new copied data files
                edd = (EDDTableCopy)oneFromDatasetXml("repPostDet");
            }
reallyVerbose=false;

            //.dds
            eTime = System.currentTimeMillis();
            tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                edd.className() + "_postDet", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    String common_name;\n" +
"    String pi;\n" +
"    String project;\n" +
"    Int32 surgery_id;\n" +
"    String tag_id_code;\n" +
"    String tag_sn;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
 
            //.das 
            eTime = System.currentTimeMillis();
            tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                edd.className() + "_postDet", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            expected = 
"    String infoUrl \"http://www.postprogram.org/\";\n" +
"    String institution \"POST\";\n";
            Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
            expected = 
"  surgery_id {\n";
            Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

            //1var
            eTime = System.currentTimeMillis();
            tName = edd.makeNewFileForDapQuery(null, null, 
                "pi&distinct()", 
                EDStatic.fullTestCacheDirectory, edd.className() + "_postDet1Var", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            String2.log(results);
            expected = 
"pi\n" +
"\n" +
"BARRY BEREJIKIAN\n" +
"CEDAR CHITTENDEN\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** 1var elapsed time=" + (System.currentTimeMillis() - eTime) + 
                " ms (vs 148,000 or 286,000 ms for POST).");

            //2var
            eTime = System.currentTimeMillis();
            tName = edd.makeNewFileForDapQuery(null, null, 
                "pi,common_name&distinct()", 
                EDStatic.fullTestCacheDirectory, edd.className() + "_postDet2var", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = //this will change
"pi,common_name\n" +
",\n" +
"BARRY BEREJIKIAN,STEELHEAD\n" +
"CEDAR CHITTENDEN,COHO\n" +
"CHRIS WOOD,\"SOCKEYE,KOKANEE\"\n" +
"CHUCK BOGGS,COHO\n" +
"DAVID WELCH,CHINOOK\n" +
"DAVID WELCH,COHO\n" +
"DAVID WELCH,DOLLY VARDEN\n" +
"DAVID WELCH,\"SOCKEYE,KOKANEE\"\n" +
"DAVID WELCH,STEELHEAD\n" +
"FRED GOETZ,CHINOOK\n" +
"FRED GOETZ,CUTTHROAT\n" +
"JACK TIPPING,STEELHEAD\n" +
"JEFF MARLIAVE,BLACK ROCKFISH\n" +
"JOHN PAYNE,SQUID\n" +
"LYSE GODBOUT,\"SOCKEYE,KOKANEE\"\n" +
"MIKE MELNYCHUK,COHO\n" +
"MIKE MELNYCHUK,\"SOCKEYE,KOKANEE\"\n" +
"MIKE MELNYCHUK,STEELHEAD\n" +
"ROBERT BISON,STEELHEAD\n" +
"SCOTT STELTZNER,CHINOOK\n" +
"SCOTT STELTZNER,COHO\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            String2.log(results);
            String2.log("*** 2var elapsed time=" + (System.currentTimeMillis() - eTime) + 
                " ms (vs 192,000 ms for POST).");

            //3var
            eTime = System.currentTimeMillis();
            tName = edd.makeNewFileForDapQuery(null, null, 
                "pi,common_name,surgery_id&distinct()", 
                EDStatic.fullTestCacheDirectory, edd.className() + "_postDet3var", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"pi,common_name,surgery_id\n" +
",,\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            expected = 
"BARRY BEREJIKIAN,STEELHEAD,2846\n";
            Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
            String lines[] = String2.split(results, '\n');
            Test.ensureEqual(lines.length, 4317 + 3, "\nresults=\n" + results);
            lines = null;
            String2.log("*** 3var elapsed time=" + (System.currentTimeMillis() - eTime) + 
                " ms (vs 152,000 ms for POST).");

            //1tag
            eTime = System.currentTimeMillis();
            tName = edd.makeNewFileForDapQuery(null, null, 
                "&pi=\"BARRY BEREJIKIAN\"&common_name=\"STEELHEAD\"&surgery_id=2846", 
                EDStatic.fullTestCacheDirectory, edd.className() + "_postDet1tag", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n" +
"degrees_east,degrees_north,UTC,,,,,,\n" +
"-127.34393,50.67973,2004-05-30T06:08:40Z,STEELHEAD,BARRY BEREJIKIAN,NOAA|NOAA FISHERIES,2846,3985,1031916\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** 1tag elapsed time=" + (System.currentTimeMillis() - eTime) + 
                " ms (vs 5,700 ms for POST).");

            //constraint
            eTime = System.currentTimeMillis();
            tQuery = "&pi=\"DAVID WELCH\"&common_name=\"CHINOOK\"&latitude>50" +
                "&surgery_id>=1201&surgery_id<1202&time>=2007-05-01T08&time<2007-05-01T09";
            tName = edd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_peb_constrained", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =  
"longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n" +
"degrees_east,degrees_north,UTC,,,,,,\n" +
"-127.48843,50.78142,2007-05-01T08:43:33Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:48:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:51:14Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:53:18Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:56:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:59:27Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n";
            Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
                expected, "\nresults=\n" + results);
            String2.log("*** constraint elapsed time=" + (System.currentTimeMillis() - eTime) +
                " ms (usually 31)."); 


            //done
            String2.pressEnterToContinue("EDDTableCopy.testRepPostDet done.");

        } catch (Throwable t) {
            String2.pressEnterToContinue(
                "\nThis EDDTableCopy test only works on Bob's computer." +
                "\n" + MustBe.throwableToString(t) + 
                "\nTHIS IS BROKEN as of 2009-08-24  Fix it when Jose recreates the table."); 
        }
        defaultCheckSourceData = true;

    } //end of testRepPostDet
        

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableCopy.test() *****************\n");
        testVerboseOn();

        //always done
        testBasic();    //tests testTableCopy dataset on local erddap

        //no longer active?
        //testRepPostDet(true);  //checkSourceData 
        //testRepPostDet(false); //checkSourceData (faster)
        
        //not usually done

    }

}
