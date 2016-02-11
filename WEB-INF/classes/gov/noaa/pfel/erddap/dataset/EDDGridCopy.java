/* 
 * EDDGridCopy Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class makes and maintains a local copy of the data from a remote source.
 * This class serves data from the local copy.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2009-05-25
 */
public class EDDGridCopy extends EDDGrid { 

    protected EDDGrid sourceEdd;
    protected EDDGridFromNcFiles localEdd;

    /** 
     * This is used to test equality of axis values. 
     * 0=no testing (not recommended). 
     * &gt;18 does exact test. default=20.
     * 1-18 tests that many digets for doubles and hidiv(n,2) for floats.
     */
    protected int matchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

    /** 
     * Some tests set EDDGridCopy.defaultCheckSourceData = false; 
     * Don't set it here.
     */
    public static boolean defaultCheckSourceData = true; 

    private static int maxChunks = Integer.MAX_VALUE;    //some test methods reduce this

    /**
     * This constructs an EDDGridCopy based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridCopy"&gt; 
     *    having just been read.  
     * @return an EDDGridCopy.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridCopy fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDGridCopy(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        EDDGrid tSourceEdd = null;
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        boolean tAccessibleViaWMS = true;
        int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        boolean checkSourceData = defaultCheckSourceData;
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
            else if (localTags.equals( "<accessibleViaWMS>")) {}
            else if (localTags.equals("</accessibleViaWMS>")) tAccessibleViaWMS = String2.parseBoolean(content);
            else if (localTags.equals( "<matchAxisNDigits>")) {}
            else if (localTags.equals("</matchAxisNDigits>")) 
                tMatchAxisNDigits = String2.parseInt(content, DEFAULT_MATCH_AXIS_N_DIGITS); 
            else if (localTags.equals( "<ensureAxisValuesAreEqual>")) {} //deprecated
            else if (localTags.equals("</ensureAxisValuesAreEqual>")) 
                tMatchAxisNDigits = String2.parseBoolean(content)? 20 : 0;
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<checkSourceData>")) {}
            else if (localTags.equals("</checkSourceData>")) checkSourceData = String2.parseBoolean(content); 
            else if (localTags.equals( "<fileTableInMemory>")) {}
            else if (localTags.equals("</fileTableInMemory>")) tFileTableInMemory = String2.parseBoolean(content); 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 
            else if (localTags.equals( "<accessibleViaFiles>")) {}
            else if (localTags.equals("</accessibleViaFiles>")) tAccessibleViaFiles = String2.parseBoolean(content); 
            else if (localTags.equals("<dataset>")) {

                if ("false".equals(xmlReader.attributeValue("active"))) {
                    //skip it - read to </dataset>
                    if (verbose) String2.log("  skipping " + xmlReader.attributeValue("datasetID") + 
                        " because active=\"false\".");
                    while (xmlReader.stackSize() != startOfTagsN + 1 ||
                           !xmlReader.allTags().substring(startOfTagsLength).equals("</dataset>")) {
                        xmlReader.nextTag();
                        //String2.log("  skippping tags: " + xmlReader.allTags());
                    }

                } else {
                    try {
                        if (checkSourceData) {
                            //after first time, it's ok if source dataset isn't available
                            tSourceEdd = (EDDGrid)EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);
                        } else {
                            String2.log("WARNING!!! checkSourceData is false, so EDDGridCopy datasetID=" + 
                                tDatasetID + " is not checking the source dataset!");
                            int stackSize = xmlReader.stackSize();
                            do {  //will throw Exception if trouble (e.g., unexpected end-of-file
                                xmlReader.nextTag();
                            } while (xmlReader.stackSize() != stackSize); 
                            tSourceEdd = null;
                        }

                        //was  (so xmlReader in right place)
                        //if (!checkSourceData) {
                        //    tSourceEdd = null;
                        //    throw new RuntimeException("TESTING checkSourceData=false.");
                        //}
                    } catch (Throwable t) {
                        String2.log(MustBe.throwableToString(t));
                    }
                }
            } 
            else xmlReader.unexpectedTagException();
        }

        return new EDDGridCopy(tDatasetID, 
            tAccessibleTo, tAccessibleViaWMS, tMatchAxisNDigits, 
            tOnChange, tFgdcFile, tIso19115File,
            tDefaultDataQuery, tDefaultGraphQuery, 
            tReloadEveryNMinutes, 
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
     * @param tSourceEdd the remote dataset to be copied.
     *   After the very first time (to generate tasks to copy data), 
     *   there will be local files so it's ok if tSourceEdd is null (unavailable).
     * @throws Throwable if trouble
     */
    public EDDGridCopy(String tDatasetID, 
        String tAccessibleTo, boolean tAccessibleViaWMS, int tMatchAxisNDigits, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        int tReloadEveryNMinutes, EDDGrid tSourceEdd, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridCopy " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridCopy(" + 
            tDatasetID + ") constructor:\n";
            
        //save the parameters
        className = "EDDGridCopy"; 
        datasetID = tDatasetID;
        sourceEdd = tSourceEdd;
        setAccessibleTo(tAccessibleTo);
        if (!tAccessibleViaWMS) 
            accessibleViaWMS = String2.canonical(
                MessageFormat.format(EDStatic.noXxx, "WMS"));
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        matchAxisNDigits = tMatchAxisNDigits;

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

                    //make a task for each axis0 value (if the file doesn't already exist)
                    PrimitiveArray tDestValues = sourceEdd.axisVariables[0].destinationValues();
                    int nAV = sourceEdd.axisVariables.length;
                    int nDV = sourceEdd.dataVariables.length;
                    StringBuilder av1on = new StringBuilder();
                    for (int av = 1; av < nAV; av++)
                        av1on.append("[]");
                    int nValues = tDestValues.size();
                    nValues = Math.min(maxChunks, nValues); 
                    for (int vi = 0; vi < nValues; vi++) {
                        //[fullCopyDirectory]/datasetID/value.nc
                        //does the file already exist?
                        String tDestValue = tDestValues.getString(vi);
                        String fileName = String2.encodeFileNameSafe(tDestValues.getString(vi));
                        if (File2.isFile(copyDatasetDir + fileName + ".nc")) {
                            if (reallyVerbose) String2.log("  file already exists: " + 
                                copyDatasetDir + fileName + ".nc");
                            continue;
                        }
                        StringBuilder tQuery = new StringBuilder();
                        sourceEdd.dataVariableDestinationNames();//ensure [] has been created
                        for (int dv = 0; dv < nDV; dv++) {
                            if (dv > 0)
                                tQuery.append(',');
                            tQuery.append(sourceEdd.dataVariableDestinationNames[dv] + 
                                "[(" + tDestValue + ")]" + av1on);
                        }

                        //make the task
                        Object taskOA[] = new Object[6];
                        taskOA[0] = TaskThread.TASK_MAKE_A_DATAFILE;
                        taskOA[1] = sourceEdd;
                        taskOA[2] = tQuery.toString(); //String, not StringBuilder
                        taskOA[3] = copyDatasetDir;
                        taskOA[4] = fileName;
                        taskOA[5] = ".nc";
                        int tTaskNumber = EDStatic.addTask(taskOA);
                        if (tTaskNumber >= 0) {
                            taskNumber = tTaskNumber;
                            if (reallyVerbose)
                                String2.log("  task#" + taskNumber + " TASK_MAKE_A_DATAFILE " + tQuery.toString() + "\n    " +
                                    copyDatasetDir + fileName + ".nc");
                        }
                    }

                    //create task to flag dataset to be reloaded
                    if (taskNumber > -1) {
                        Object taskOA[] = new Object[2];
                        taskOA[0] = TaskThread.TASK_SET_FLAG;
                        taskOA[1] = datasetID;
                        taskNumber = EDStatic.addTask(taskOA);  //TASK_SET_FLAG will always be added
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
        Object[][] tAxisVariables = null;
        Object[][] tDataVariables = null;
        if (sourceEdd == null) {
            //get info from existing copied datafiles, which is a standard EDDGrid)
            //get a list of copied files
            String tFileNames[] = RegexFilenameFilter.fullNameList(  //not recursiveFullNameList, since just 1 dir
                copyDatasetDir, ".*\\.nc");
            if (tFileNames.length == 0)
                throw new RuntimeException("Warning: There are no copied files in " + 
                    copyDatasetDir +
                    ",\nso localEdd can't be made yet for datasetID=" + datasetID + ".\n" +
                    "But it will probably succeed in next loadDatasets (15 minutes?),\n" +
                    "after some files are copied.");

            //get the axisVariable and dataVariable info from the file
            String getFromName = File2.getYoungest(tFileNames);
            String2.log("!!! sourceEDD is unavailable, so getting info from youngest file\n" + getFromName);
            StringArray ncDataVarNames = new StringArray();
            StringArray ncDataVarTypes = new StringArray();
            NetcdfFile ncFile = NcHelper.openFile(getFromName); //may throw exception
            try {
                //list all variables with dimensions
                List allVariables = ncFile.getVariables(); 
                for (int v = 0; v < allVariables.size(); v++) {
                    Variable var = (Variable)allVariables.get(v);
                    String varName = var.getShortName();
                    List dimensions = var.getDimensions();
                    if (dimensions != null && dimensions.size() > 1) {
                        if (tAxisVariables == null) {
                            //gather tAxisVariables
                            tAxisVariables = new Object[dimensions.size()][];
                            for (int avi = 0; avi < dimensions.size(); avi++) {
                                String axisName = ((Dimension)dimensions.get(avi)).getName();
                                tAxisVariables[avi] = new Object[]{axisName, axisName, new Attributes()};
                            }
                        }
                        ncDataVarNames.add(varName);
                        Class tClass = NcHelper.getElementClass(var.getDataType());
                        if      (tClass == char.class)    tClass = String.class;
                        else if (tClass == boolean.class) tClass = byte.class; 
                        ncDataVarTypes.add(PrimitiveArray.elementClassToString(tClass));
                    }
                }
                ncFile.close();
                ncFile = null;

                //gather tDataVariables
                if (ncDataVarNames.size() == 0 || tAxisVariables == null)
                    throw new RuntimeException("Error: No multidimensional variables were found in " +
                        getFromName);
                tDataVariables = new Object[ncDataVarNames.size()][];
                for (int dv = 0; dv < ncDataVarNames.size(); dv++) {
                    tDataVariables[dv] = new Object[]{ncDataVarNames.get(dv), ncDataVarNames.get(dv), 
                        new Attributes(), ncDataVarTypes.get(dv)};
                }
            //don't catch exception, but do close the ncFile.
            } finally {
                try {
                    if (ncFile != null) ncFile.close();
                } catch (Throwable t2) {
                }
            }
        } else {
            //get info from sourceEdd, which is a standard EDDGrid
            int nAxisVariables = sourceEdd.axisVariableDestinationNames().length;
            tAxisVariables = new Object[nAxisVariables][];
            for (int av = 0; av < nAxisVariables; av++) {
                String tName = sourceEdd.axisVariableDestinationNames[av];
                tAxisVariables[av] = new Object[]{tName, tName, new Attributes()};
            }
            int nDataVariables = sourceEdd.dataVariables.length;
            tDataVariables = new Object[nDataVariables][];
            for (int dv = 0; dv < nDataVariables; dv++) {
                EDV edv = sourceEdd.dataVariables[dv];
                tDataVariables[dv] = new Object[]{edv.destinationName(), edv.destinationName(), 
                    new Attributes(), edv.sourceDataType()};
            }
        }

        //make localEDD
        //It will fail if 0 local files -- that's okay, taskThread will continue to work 
        //  and constructor will try again in 15 min.
        boolean recursive = false; //false=notRecursive   since always just 1 directory
        String fileNameRegex = ".*\\.nc";
        localEdd = new EDDGridFromNcFiles(datasetID, 
            tAccessibleTo, tAccessibleViaWMS,
            tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery,
            new Attributes(), //addGlobalAttributes
            tAxisVariables,
            tDataVariables,
            tReloadEveryNMinutes, 0, //updateEveryNMillis
            copyDatasetDir, fileNameRegex, recursive, ".*", //true pathRegex is for remote site
            EDDGridFromFiles.MF_LAST,
            matchAxisNDigits,  //sourceEdd should have made them consistent
            tFileTableInMemory, false); //accessibleViaFiles false here

        //copy things from localEdd 
        //remove last 2 lines from history (will be redundant)
        String tHistory = localEdd.combinedGlobalAttributes.getString("history");
        if (tHistory != null) {
            StringArray tHistoryLines = (StringArray)PrimitiveArray.factory(String2.split(tHistory, '\n'));
            if (tHistoryLines.size() > 2) {  
                tHistoryLines.removeRange(tHistoryLines.size()-2, tHistoryLines.size());
                String ts = tHistoryLines.toNewlineString();
                localEdd.combinedGlobalAttributes.add("history", ts.substring(0, ts.length() - 1)); //remove last \n
            }
        }
        sourceGlobalAttributes   = localEdd.combinedGlobalAttributes;
        addGlobalAttributes      = new Attributes();
        combinedGlobalAttributes = localEdd.combinedGlobalAttributes; //new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important

        //data variables
        axisVariables = localEdd.axisVariables;
        dataVariables = localEdd.dataVariables;
        lonIndex      = localEdd.lonIndex;
        latIndex      = localEdd.latIndex;
        altIndex      = localEdd.altIndex;
        depthIndex    = localEdd.depthIndex;
        timeIndex     = localEdd.timeIndex;

        //accessibleViaFiles
        if (EDStatic.filesActive && tAccessibleViaFiles) {
            accessibleViaFilesDir = copyDatasetDir;
            accessibleViaFilesRegex = fileNameRegex;
            accessibleViaFilesRecursive = recursive;
        }

        //ensure the setup is valid
        ensureValid(); //this ensures many things are set, e.g., sourceUrl

        //If the child is a FromErddap, try to subscribe to the remote dataset.
        if (sourceEdd instanceof FromErddap) 
            tryToSubscribeToChildFromErddap(sourceEdd);

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDGridCopy " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }

    /** 
     * This gets data (not yet standardized) from the data 
     * source for this EDDGrid.     
     * Because this is called by GridDataAccessor, the request won't be the 
     * full user's request, but will be a partial request (for less than
     * EDStatic.partialRequestMaxBytes).
     * 
     * @param tDataVariables EDV[] with just the requested data variables
     * @param tConstraints  int[nAxisVariables*3] 
     *   where av*3+0=startIndex, av*3+1=stride, av*3+2=stopIndex.
     *   AxisVariables are counted left to right, e.g., sst[0=time][1=lat][2=lon].
     * @return a PrimitiveArray[] where the first axisVariables.length elements
     *   are the axisValues and the next tDataVariables.length elements
     *   are the dataValues.
     *   Both the axisValues and dataValues are straight from the source,
     *   not modified.
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public PrimitiveArray[] getSourceData(EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable {

        return localEdd.getSourceData(tDataVariables, tConstraints);
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException("Error: " + 
            "EDDGridCopy doesn't support method=\"sibling\".");
    }

    /** 
     * This returns a fileTable (formatted like 
     * FileVisitorDNLS.oneStep(tDirectoriesToo=false, last_mod is LongArray,
     * and size is LongArray of epochMillis)
     * with valid files (or null if unavailable or any trouble).
     * This is a copy of any internal data, so client can modify the contents.
     */
    public Table accessibleViaFilesFileTable() {
        return localEdd.accessibleViaFilesFileTable();
    }


    /**
     * The basic tests of this class (erdGlobecBottle).
     * 
     */
    public static void testBasic(boolean checkSourceData) throws Throwable {
        String2.log("\n****************** EDDGridCopy.testBasic(checkSourceData=" + checkSourceData + 
            ") *****************\n");
        testVerboseOn();
        defaultCheckSourceData = checkSourceData;
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        EDDGrid eddGrid = null;

        try {
         
            try {
                eddGrid = (EDDGridCopy)oneFromDatasetsXml(null, "testGridCopy");
            } catch (Throwable t2) {
                //it will fail if no files have been copied
                String2.log(MustBe.throwableToString(t2));
            }
            if (checkSourceData) {
                while (EDStatic.nUnfinishedTasks() > 0) {
                    String2.log("nUnfinishedTasks=" + EDStatic.nUnfinishedTasks());
                    Math2.sleep(10000);
                }
                //recreate edd to see new copied data files
                eddGrid = (EDDGridCopy)oneFromDatasetsXml(null, "testGridCopy");
            }

            //*** test getting das for entire dataset
            String2.log("\n*** .nc test das dds for entire dataset\n");
            tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddGrid.className() + "_Entire", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Attributes {\n" +
    "  time {\n" +
    "    String _CoordinateAxisType \"Time\";\n" +
    "    Float64 actual_range 1.1991888e+9, 1.1999664e+9;\n" +
    "    String axis \"T\";\n" +
    "    Int32 fraction_digits 0;\n" +
    "    String ioos_category \"Time\";\n" +
    "    String long_name \"Centered Time\";\n" +
    "    String standard_name \"time\";\n" +
    "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
    "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
    "  }\n" +
    "  altitude {\n" +
    "    String _CoordinateAxisType \"Height\";\n" +
    "    String _CoordinateZisPositive \"up\";\n" +
    "    Float64 actual_range 0.0, 0.0;\n" +
    "    String axis \"Z\";\n" +
    "    Int32 fraction_digits 0;\n" +
    "    String ioos_category \"Location\";\n" +
    "    String long_name \"Altitude\";\n" +
    "    String positive \"up\";\n" +
    "    String standard_name \"altitude\";\n" +
    "    String units \"m\";\n" +
    "  }\n" +
    "  latitude {\n" +
    "    String _CoordinateAxisType \"Lat\";\n" +
    "    Float64 actual_range -89.875, 89.875;\n" +
    "    String axis \"Y\";\n" +
    "    String coordsys \"geographic\";\n" +
    "    Int32 fraction_digits 2;\n" +
    "    String ioos_category \"Location\";\n" +
    "    String long_name \"Latitude\";\n" +
    "    String point_spacing \"even\";\n" +
    "    String standard_name \"latitude\";\n" +
    "    String units \"degrees_north\";\n" +
    "  }\n" +
    "  longitude {\n" +
    "    String _CoordinateAxisType \"Lon\";\n" +
    "    Float64 actual_range 0.125, 359.875;\n" +
    "    String axis \"X\";\n" +
    "    String coordsys \"geographic\";\n" +
    "    Int32 fraction_digits 2;\n" +
    "    String ioos_category \"Location\";\n" +
    "    String long_name \"Longitude\";\n" +
    "    String point_spacing \"even\";\n" +
    "    String standard_name \"longitude\";\n" +
    "    String units \"degrees_east\";\n" +
    "  }\n" +
    "  x_wind {\n" +
    "    Float32 _FillValue -9999999.0;\n" +
    "    Float64 colorBarMaximum 15.0;\n" +
    "    Float64 colorBarMinimum -15.0;\n" +
    "    String coordsys \"geographic\";\n" +
    "    Int32 fraction_digits 1;\n" +
    "    String ioos_category \"Wind\";\n" +
    "    String long_name \"Zonal Wind\";\n" +
    "    Float32 missing_value -9999999.0;\n" +
    "    String standard_name \"x_wind\";\n" +
    "    String units \"m s-1\";\n" +
    "  }\n" +
    "  y_wind {\n" +
    "    Float32 _FillValue -9999999.0;\n" +
    "    Float64 colorBarMaximum 15.0;\n" +
    "    Float64 colorBarMinimum -15.0;\n" +
    "    String coordsys \"geographic\";\n" +
    "    Int32 fraction_digits 1;\n" +
    "    String ioos_category \"Wind\";\n" +
    "    String long_name \"Meridional Wind\";\n" +
    "    Float32 missing_value -9999999.0;\n" +
    "    String standard_name \"y_wind\";\n" +
    "    String units \"m s-1\";\n" +
    "  }\n" +
    "  mod {\n" +
    "    Float32 _FillValue -9999999.0;\n" +
    "    Float64 colorBarMaximum 18.0;\n" +
    "    Float64 colorBarMinimum 0.0;\n" +
    "    String colorBarPalette \"WhiteRedBlack\";\n" +
    "    String coordsys \"geographic\";\n" +
    "    Int32 fraction_digits 1;\n" +
    "    String ioos_category \"Wind\";\n" +
    "    String long_name \"Modulus of Wind\";\n" +
    "    Float32 missing_value -9999999.0;\n" +
    "    String units \"m s-1\";\n" +
    "  }\n" +
    "  NC_GLOBAL {\n" +
    "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
    "    String cdm_data_type \"Grid\";\n" +
    "    String composite \"true\";\n" +
    "    String contributor_name \"Remote Sensing Systems, Inc\";\n" +
    "    String contributor_role \"Source of level 2 data.\";\n" +
    "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" + 
    "    String creator_email \"dave.foley@noaa.gov\";\n" +
    "    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
    "    String creator_url \"http://coastwatch.pfel.noaa.gov\";\n" +
    "    String date_created \"2008-08-29Z\";\n" +
    "    String date_issued \"2008-08-29Z\";\n" +
    "    Float64 Easternmost_Easting 359.875;\n" +
    "    Float64 geospatial_lat_max 89.875;\n" +
    "    Float64 geospatial_lat_min -89.875;\n" +
    "    Float64 geospatial_lat_resolution 0.25;\n" +
    "    String geospatial_lat_units \"degrees_north\";\n" +
    "    Float64 geospatial_lon_max 359.875;\n" +
    "    Float64 geospatial_lon_min 0.125;\n" +
    "    Float64 geospatial_lon_resolution 0.25;\n" +
    "    String geospatial_lon_units \"degrees_east\";\n" +
    "    Float64 geospatial_vertical_max 0.0;\n" +
    "    Float64 geospatial_vertical_min 0.0;\n" +
    "    String geospatial_vertical_positive \"up\";\n" +
    "    String geospatial_vertical_units \"m\";\n" +
    "    String history \"Remote Sensing Systems, Inc\n" +
    "2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
    
//+ " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
    " http://127.0.0.1:8080/cwexperimental/griddap/testGridCopy.das\";\n" + //different
    "    String infoUrl \"http://coastwatch.pfel.noaa.gov/infog/QS_ux10_las.html\";\n" +
    "    String institution \"NOAA CoastWatch, West Coast Node\";\n" +
    "    String keywords \"EARTH SCIENCE > Oceans > Ocean Winds > Surface Winds\";\n" +
    "    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
    "    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
    "    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
    "    Float64 Northernmost_Northing 89.875;\n" +
    "    String origin \"Remote Sensing Systems, Inc\";\n" +
    "    String processing_level \"3\";\n" +
    "    String project \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
    "    String projection \"geographic\";\n" +
    "    String projection_type \"mapped\";\n" +
    "    String references \"RSS Inc. Winds: http://www.remss.com/ .\";\n" +
    "    String satellite \"QuikSCAT\";\n" +
    "    String sensor \"SeaWinds\";\n" +
    "    String source \"satellite observation: QuikSCAT, SeaWinds\";\n" +
        //it's still a numeric ip because the source file was created long ago
    "    String sourceUrl \"http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\";\n" +
    "    Float64 Southernmost_Northing -89.875;\n" +
    "    String standard_name_vocabulary \"CF Standard Name Table v27\";\n" +
    "    String summary \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\";\n" +
    "    String time_coverage_end \"2008-01-10T12:00:00Z\";\n" +
    "    String time_coverage_start \"2008-01-01T12:00:00Z\";\n" +
    "    String title \"Wind, QuikSCAT, Global, Science Quality (1 Day Composite)\";\n" +
    "    Float64 Westernmost_Easting 0.125;\n" +
    "  }\n" +
    "}\n";
            int tpo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tpo > 0, "tpo=-1 results=\n" + results);
            Test.ensureEqual(results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
                expected, "results=\n" + results);
            
            //*** test getting dds for entire dataset
            tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddGrid.className() + "_Entire", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Dataset {\n" +
    "  Float64 time[time = 10];\n" +
    "  Float64 altitude[altitude = 1];\n" +
    "  Float64 latitude[latitude = 720];\n" +
    "  Float64 longitude[longitude = 1440];\n" +
    "  GRID {\n" +
    "    ARRAY:\n" +
    "      Float32 x_wind[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
    "    MAPS:\n" +
    "      Float64 time[time = 10];\n" +
    "      Float64 altitude[altitude = 1];\n" +
    "      Float64 latitude[latitude = 720];\n" +
    "      Float64 longitude[longitude = 1440];\n" +
    "  } x_wind;\n" +
    "  GRID {\n" +
    "    ARRAY:\n" +
    "      Float32 y_wind[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
    "    MAPS:\n" +
    "      Float64 time[time = 10];\n" +
    "      Float64 altitude[altitude = 1];\n" +
    "      Float64 latitude[latitude = 720];\n" +
    "      Float64 longitude[longitude = 1440];\n" +
    "  } y_wind;\n" +
    "  GRID {\n" +
    "    ARRAY:\n" +
    "      Float32 mod[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
    "    MAPS:\n" +
    "      Float64 time[time = 10];\n" +
    "      Float64 altitude[altitude = 1];\n" +
    "      Float64 latitude[latitude = 720];\n" +
    "      Float64 longitude[longitude = 1440];\n" +
    "  } mod;\n" +
    "} testGridCopy;\n";  //different
            Test.ensureEqual(results, expected, "\nresults=\n" + results);



            //.csv  with data from one file
            String2.log("\n*** .nc test read from one file\n");       
            userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
            tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddGrid.className() + "_Data1", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    //verified with 
    //http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
    "time,altitude,latitude,longitude,y_wind\n" +
    "UTC,m,degrees_north,degrees_east,m s-1\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,230.875,2.82175\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,231.625,4.539375\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,232.375,4.975015\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,233.125,5.643055\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,233.875,2.72394\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,234.625,1.39762\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,235.375,2.10711\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,236.125,3.019165\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,236.875,3.551915\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,237.625,NaN\n";          //test of NaN
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


            //.csv  with data from several files
            String2.log("\n*** .nc test read from several files\n");       
            userDapQuery = "y_wind[(1.1991888e9):3:(1.1999664e9)][0][(36.5)][(230)]";
            tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddGrid.className() + "_Data1", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    //verified with 
    //http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1991888e9):3:(1.1999664e9)][0][(36.5)][(230)]
    "time,altitude,latitude,longitude,y_wind\n" +
    "UTC,m,degrees_north,degrees_east,m s-1\n" +
    "2008-01-01T12:00:00Z,0.0,36.625,230.125,7.6282454\n" +
    "2008-01-04T12:00:00Z,0.0,36.625,230.125,-12.3\n" +
    "2008-01-07T12:00:00Z,0.0,36.625,230.125,-5.974585\n" +
    "2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            //  */

        } catch (Throwable t) {
            String2.pressEnterToContinue(
                "\nThis EDDGridCopy test only works on Bob's computer." +
                "\n" + MustBe.throwableToString(t)); 
        }

        defaultCheckSourceData = true;
    } //end of testBasic


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDGridCopy.test() *****************\n");
        testVerboseOn();

        //always done
        testBasic(true); //checkSourceData
        testBasic(false); 
        
        //not usually done

    }

}
