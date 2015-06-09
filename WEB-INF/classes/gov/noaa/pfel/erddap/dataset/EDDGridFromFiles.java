/* 
 * EDDGridFromFiles Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.WatchDirectory;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.EDUnits;
import gov.noaa.pfel.erddap.variable.*;

import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.*;

/** 
 * This class represents a virtual table of data from by aggregating the existing outer dimension 
 * of a collection of data files.
 * <br>In a given file, if the outer dimension has more than one value, 
 *    the values must be sorted ascending, with no ties.
 * <br>The outer dimension values in different files can't overlap.
 * <br>The presumption is that the entire dataset can be read reasonable quickly
 *   (from the local files, unlike remote data) and all variable's min and max info
 *   can be gathered (for each file) 
 *   and cached (facilitating handling constraints in data requests).
 * <br>And file data can be cached and reused because each file has a lastModified
 *   time which can be used to detect if file is unchanged.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2008-11-26
 */
public abstract class EDDGridFromFiles extends EDDGrid{ 

    public final static String MF_FIRST = "first", MF_LAST = "last";
    public static int suggestedUpdateEveryNMillis = 10000;

    /** Columns in the File Table */
    protected final static int 
        FT_DIR_INDEX_COL=0, //useful that it is #0   (tFileTable uses same positions)
        FT_FILE_LIST_COL=1, //useful that it is #1
        FT_LAST_MOD_COL=2, 
        FT_N_VALUES_COL=3, FT_MIN_COL=4, FT_MAX_COL=5, FT_CSV_VALUES_COL=6,
        FT_START_INDEX_COL = 7;

    //set by constructor
    protected String fileDir;
    protected boolean recursive;
    protected String fileNameRegex;
    protected String metadataFrom;       
    protected boolean ensureAxisValuesAreExactlyEqual;
    protected StringArray sourceDataNames;
    protected StringArray sourceAxisNames;
    protected String sourceDataTypes[];

    protected boolean haveValidSourceInfo = false; //if true, following 3 are correctly set 
    protected Attributes sourceAxisAttributes[];
    protected PrimitiveArray sourceAxisValues[];
    protected Attributes sourceDataAttributes[];

    protected WatchDirectory watchDirectory;

    //dirTable and fileTable inMemory (default=false)
    protected boolean fileTableInMemory = false;
    protected Table dirTable; 
    protected Table fileTable;


    /**
     * This constructs an EDDGridFromFiles based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="[subclassName]"&gt; 
     *    having just been read.  
     * @return an EDDGridFromFiles.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridFromFiles fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDGridFromFiles(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        String tType = xmlReader.attributeValue("type"); 
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        boolean tFileTableInMemory = false;
        String tFgdcFile = null;
        String tIso19115File = null;
        Attributes tGlobalAttributes = null;
        ArrayList tAxisVariables = new ArrayList();
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        int tUpdateEveryNMillis = 0;
        String tFileDir = null;
        boolean tRecursive = false;
        String tFileNameRegex = ".*";
        boolean tAccessibleViaFiles = false;
        String tMetadataFrom = MF_LAST;       
        boolean tEnsureAxisValuesAreExactlyEqual = true;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;

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
            if      (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) 
                throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
            else if (localTags.equals( "<axisVariable>")) tAxisVariables.add(getSDAVVariableFromXml(xmlReader));           
            else if (localTags.equals( "<dataVariable>")) tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<updateEveryNMillis>")) {}
            else if (localTags.equals("</updateEveryNMillis>")) tUpdateEveryNMillis = String2.parseInt(content); 
            else if (localTags.equals( "<fileDir>")) {} 
            else if (localTags.equals("</fileDir>")) tFileDir = content; 
            else if (localTags.equals( "<recursive>")) {}
            else if (localTags.equals("</recursive>")) tRecursive = String2.parseBoolean(content); 
            else if (localTags.equals( "<fileNameRegex>")) {}
            else if (localTags.equals("</fileNameRegex>")) tFileNameRegex = content; 
            else if (localTags.equals( "<accessibleViaFiles>")) {}
            else if (localTags.equals("</accessibleViaFiles>")) tAccessibleViaFiles = String2.parseBoolean(content); 
            else if (localTags.equals( "<metadataFrom>")) {}
            else if (localTags.equals("</metadataFrom>")) tMetadataFrom = content; 
            else if (localTags.equals( "<fileTableInMemory>")) {}
            else if (localTags.equals("</fileTableInMemory>")) tFileTableInMemory = String2.parseBoolean(content); 
            //ensureAxisValuesAreExactlyEqual is currently not allowed; 
            //if false, it is hard to know which are desired values   (same as metadataFrom?)
            //else if (localTags.equals( "<ensureAxisValuesAreExactlyEqual>")) {}
            //else if (localTags.equals("</ensureAxisValuesAreExactlyEqual>")) 
            //    tEnsureAxisValuesAreExactlyEqual = String2.parseBoolean(content); 
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 
            else xmlReader.unexpectedTagException();
        }
        int nav = tAxisVariables.size();
        Object ttAxisVariables[][] = new Object[nav][];
        for (int i = 0; i < tAxisVariables.size(); i++)
            ttAxisVariables[i] = (Object[])tAxisVariables.get(i);

        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        if (tType == null)
            tType = "";
        if (tType.equals("EDDGridFromNcFiles")) 
            return new EDDGridFromNcFiles(tDatasetID, tAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File,
                tDefaultDataQuery, tDefaultGraphQuery, tGlobalAttributes,
                ttAxisVariables,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis,
                tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
                tEnsureAxisValuesAreExactlyEqual, tFileTableInMemory, 
                tAccessibleViaFiles);
        else if (tType.equals("EDDGridFromMergeIRFiles")) 
            return new EDDGridFromMergeIRFiles(tDatasetID, tAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File,
                tDefaultDataQuery, tDefaultGraphQuery, tGlobalAttributes,
                ttAxisVariables,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis,
                tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
                tEnsureAxisValuesAreExactlyEqual, tFileTableInMemory, 
                tAccessibleViaFiles);
        else throw new Exception("type=\"" + tType + 
            "\" needs to be added to EDDGridFromFiles.fromXml at end.");

    }

    /**
     * The constructor.
     *
     * @param tClassName  e.g., EDDGridFromNcFiles
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
     * @param tAddGlobalAttributes are global attributes which will
     *   be added to (and take precedence over) the data source's global attributes.
     *   This may be null if you have nothing to add.
     *   The combined global attributes must include:
     *   <ul>
     *   <li> "title" - the short (&lt; 80 characters) description of the dataset 
     *   <li> "summary" - the longer description of the dataset.
     *      It may have newline characters (usually at &lt;= 72 chars per line). 
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   <li> "infoUrl" - the url with information about this data set 
     *   <li> "cdm_data_type" - one of the EDD.CDM_xxx options
     *   </ul>
     *   Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     *   Special case: if combinedGlobalAttributes name="license", any instance of "[standard]"
     *     will be converted to the EDStatic.standardLicense
     * @param tAxisVariables is an Object[nAxisVariables][3]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" -
     *        a category from EDV.ioosCategories, 
     *        although they are added automatically for lon, lat, alt, and time). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>If there are longitude, latitude, altitude, or time variables,
     *        they must have that name as the destinationName (or sourceName) 
     *        to be identified as such.
     *    <br>Or, use tAxisVariables=null if the axis variables need no addAttributes
     *        and the longitude,latitude,altitude,time variables (if present) 
     *        all have their correct names in the source.
     *    <br>The order of variables you define must match the
     *       order in the source.
     *    <br>A time variable must have "units" specified in addAttributes (read first)
     *       or sourceAttributes.  "units" must be
     *       a udunits string (containing " since ")
     *        describing how to interpret numbers 
     *        (e.g., "seconds since 1970-01-01T00:00:00Z").
     * @param tDataVariables is an Object[nDataVariables][4]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source, 
     *         without the outer or inner sequence name),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>[3]=String the source dataType (e.g., "int", "float", "String"). 
     *        Some data sources have ambiguous data types, so it needs to be specified here.
     *        <br>This class is unusual: it is okay if different source files have different dataTypes.
     *        <br>All will be converted to the dataType specified here.           
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tFileDir the base directory where the files are located
     * @param tRecursive if true, this class will look for files in the
     *    fileDir and all subdirectories
     * @param tFileNameRegex the regex which determines which files in 
     *    the directories are to be read.
     *    <br>You can use .* for all, but it is better to be more specific.
     *        For example, .*\.nc will get all files with the extension .nc.
     *    <br>All files must have all of the axisVariables and all of the dataVariables.
     * @param tMetadataFrom this indicates the file to be used
     *    to extract source metadata (first/last based on file list sorted by minimum axis #0 value).
     *    Valid values are "first", "penultimate", "last".
     *    If invalid, "last" is used.
     * @param tEnsureAxisValuesAreExactlyEqual if true (default, currently required),
     *    a file's axis values must exactly equal the others or the file is rejected;
     *    if false, almostEqual is used.
     * @throws Throwable if trouble
     */
    public EDDGridFromFiles(String tClassName, String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tAxisVariables,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, boolean tRecursive, String tFileNameRegex, 
        String tMetadataFrom, boolean tEnsureAxisValuesAreExactlyEqual, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles) 
        throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridFromFiles " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in ERDDAP EDDGridFromFiles(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = tClassName;
        datasetID = tDatasetID; 
        //ensure valid for creation of datasetInfo files below
        if (!String2.isFileNameSafe(datasetID)) 
            throw new IllegalArgumentException(errorInMethod + "datasetID=" + datasetID + " isn't fileNameSafe.");
        File2.makeDirectory(datasetDir()); //based on datasetID
        String dirTableFileName  = datasetDir() +  DIR_TABLE_FILENAME;
        String fileTableFileName = datasetDir() + FILE_TABLE_FILENAME;

        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        setUpdateEveryNMillis(tUpdateEveryNMillis);
        fileTableInMemory = tFileTableInMemory;
        fileDir = tFileDir;
        recursive = tRecursive;
        fileNameRegex = tFileNameRegex;
        metadataFrom = tMetadataFrom;
        ensureAxisValuesAreExactlyEqual = true; //tEnsureAxisValuesAreExactlyEqual;
        int nav = tAxisVariables.length;
        int ndv = tDataVariables.length;

        if (fileDir == null || fileDir.length() == 0)
            throw new IllegalArgumentException(errorInMethod + "fileDir wasn't specified.");
        if (fileNameRegex == null || fileNameRegex.length() == 0) 
            fileNameRegex = ".*";
        if (metadataFrom == null) metadataFrom = "";
        if (metadataFrom.length() == 0) metadataFrom = MF_LAST;
        if (!metadataFrom.equals(MF_FIRST) && 
            !metadataFrom.equals(MF_LAST))
            throw new IllegalArgumentException("metadataFrom=" + metadataFrom + " must be " + 
                MF_FIRST + " or " + MF_LAST + ".");

        //note sourceAxisNames
        if (tAxisVariables.length == 0) 
            throw new IllegalArgumentException("No axisVariables were specified.");
        sourceAxisNames = new StringArray();
        for (int av = 0; av < nav; av++) 
            sourceAxisNames.add((String)tAxisVariables[av][0]);
        if (reallyVerbose) String2.log("sourceAxisNames=" + sourceAxisNames);

        //note sourceDataNames, sourceDataTypes
        sourceDataNames = new StringArray();
        sourceDataTypes = new String[ndv];
        sourceDataAttributes = new Attributes[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            sourceDataNames.add((String)tDataVariables[dv][0]);
            sourceDataTypes[dv] = (String)tDataVariables[dv][3];
            if (sourceDataTypes[dv] == null || sourceDataTypes[dv].length() == 0)
                throw new IllegalArgumentException("Unspecified data type for var#" + dv + ".");
            sourceDataAttributes[dv] = new Attributes();
        }
        if (reallyVerbose) String2.log("sourceDataNames=" + sourceDataNames +
            "\nsourceDataTypes=" + String2.toCSSVString(sourceDataTypes));

        //load cached dirTable->dirList
        dirTable = tryToLoadDirFileTable(dirTableFileName); //may be null
        if (dirTable != null) {
            if (verbose) String2.log(
                dirTable.nRows() + " rows in old dirTable");
            if (reallyVerbose) String2.log(
                "first 5 rows=\n" + 
                dirTable.dataToCSVString(5));
        }


        //load cached fileTable
        fileTable = tryToLoadDirFileTable(fileTableFileName); //may be null
        if (fileTable != null) {
            if (verbose) String2.log(
                fileTable.nRows() + " rows in old fileTable");
            if (reallyVerbose) String2.log(
                "first 5 rows=\n" + 
                fileTable.dataToCSVString(5));
        }

        //ensure fileTable has correct columns and data types
        if (fileTable != null) {
            boolean ok = true;
            if      (fileTable.findColumnNumber("dirIndex")      != FT_DIR_INDEX_COL)   ok = false;
            else if (fileTable.findColumnNumber("fileList")      != FT_FILE_LIST_COL)   ok = false;
            else if (fileTable.findColumnNumber("lastMod")       != FT_LAST_MOD_COL)    ok = false;
            else if (fileTable.findColumnNumber("nValues")       != FT_N_VALUES_COL)    ok = false;
            else if (fileTable.findColumnNumber("min")           != FT_MIN_COL)         ok = false;
            else if (fileTable.findColumnNumber("max")           != FT_MAX_COL)         ok = false;
            else if (fileTable.findColumnNumber("csvValues")     != FT_CSV_VALUES_COL)  ok = false;
            else if (fileTable.findColumnNumber("startIndex")    != FT_START_INDEX_COL) ok = false;
            else if (!(fileTable.getColumn(FT_DIR_INDEX_COL)   instanceof ShortArray))  ok = false;
            else if (!(fileTable.getColumn(FT_FILE_LIST_COL)   instanceof StringArray)) ok = false;
            else if (!(fileTable.getColumn(FT_LAST_MOD_COL)    instanceof DoubleArray)) ok = false;
            else if (!(fileTable.getColumn(FT_N_VALUES_COL)    instanceof IntArray))    ok = false;
            else if (!(fileTable.getColumn(FT_MIN_COL)         instanceof DoubleArray)) ok = false;
            else if (!(fileTable.getColumn(FT_MAX_COL)         instanceof DoubleArray)) ok = false;
            else if (!(fileTable.getColumn(FT_CSV_VALUES_COL)  instanceof StringArray)) ok = false;
            else if (!(fileTable.getColumn(FT_START_INDEX_COL) instanceof IntArray))    ok = false;
            if (!ok) {
                String2.log("Old fileTable discarded because of incorrect column arrangement (first 2 rows):\n" +
                    fileTable.toString("row", 2));
                fileTable = null;
            }
        }

        //load badFileMap
        ConcurrentHashMap badFileMap = readBadFileMap();  

        //if trouble reading any, recreate all
        if (dirTable == null || fileTable == null || badFileMap == null) {
            if (verbose) String2.log("creating new dirTable and fileTable " +
                "(dirTable=null?" + (dirTable==null) + 
                " fileTable=null?" + (fileTable==null) + 
                " badFileMap=null?" + (badFileMap==null) + ")");
            dirTable = new Table();
            dirTable.addColumn("dirName", new StringArray());

            fileTable = new Table();
            Test.ensureEqual(fileTable.addColumn(FT_DIR_INDEX_COL,   "dirIndex",      new ShortArray()),  FT_DIR_INDEX_COL,   "FT_DIR_INDEX_COL is wrong.");
            Test.ensureEqual(fileTable.addColumn(FT_FILE_LIST_COL,   "fileList",      new StringArray()), FT_FILE_LIST_COL,   "FT_FILE_LIST_COL is wrong.");
            Test.ensureEqual(fileTable.addColumn(FT_LAST_MOD_COL,    "lastMod",       new DoubleArray()), FT_LAST_MOD_COL,    "FT_LAST_MOD_COL is wrong.");
            Test.ensureEqual(fileTable.addColumn(FT_N_VALUES_COL,    "nValues",       new IntArray()),    FT_N_VALUES_COL,    "FT_N_VALUES_COL is wrong.");
            Test.ensureEqual(fileTable.addColumn(FT_MIN_COL,         "min",           new DoubleArray()), FT_MIN_COL,         "FT_MIN_COL is wrong.");
            Test.ensureEqual(fileTable.addColumn(FT_MAX_COL,         "max",           new DoubleArray()), FT_MAX_COL,         "FT_MAX_COL is wrong.");
            Test.ensureEqual(fileTable.addColumn(FT_CSV_VALUES_COL,  "csvValues",     new StringArray()), FT_CSV_VALUES_COL,  "FT_CSV_VALUES_COL is wrong.");
            Test.ensureEqual(fileTable.addColumn(FT_START_INDEX_COL, "startIndex",    new IntArray()),    FT_START_INDEX_COL, "FT_START_INDEX_COL is wrong.");

            badFileMap = newEmptyBadFileMap();
        }

        //get the dirTable and fileTable PrimitiveArrays
        StringArray dirList      = (StringArray)dirTable.getColumn(0);
        ShortArray  ftDirIndex   = (ShortArray) fileTable.getColumn(FT_DIR_INDEX_COL);
        StringArray ftFileList   = (StringArray)fileTable.getColumn(FT_FILE_LIST_COL);        
        DoubleArray ftLastMod    = (DoubleArray)fileTable.getColumn(FT_LAST_MOD_COL);
        IntArray    ftNValues    = (IntArray)   fileTable.getColumn(FT_N_VALUES_COL);
        DoubleArray ftMin        = (DoubleArray)fileTable.getColumn(FT_MIN_COL);
        DoubleArray ftMax        = (DoubleArray)fileTable.getColumn(FT_MAX_COL);
        StringArray ftCsvValues  = (StringArray)fileTable.getColumn(FT_CSV_VALUES_COL);
        IntArray    ftStartIndex = (IntArray)   fileTable.getColumn(FT_START_INDEX_COL);

        //get sourceAxisValues and sourceAxisAttributes from an existing file (if any)
        //first one (if any) should succeed
        for (int i = 0; i < ftFileList.size(); i++) {
            String tDir  = dirList.get(ftDirIndex.get(i));
            String tName = ftFileList.get(i);

            Attributes tSourceGlobalAttributes = new Attributes();
            Attributes tSourceAxisAttributes[] = new Attributes[nav];
            Attributes tSourceDataAttributes[] = new Attributes[ndv];
            for (int avi = 0; avi < nav; avi++) 
                tSourceAxisAttributes[avi] = new Attributes();
            for (int dvi = 0; dvi < ndv; dvi++) 
                tSourceDataAttributes[dvi] = new Attributes();
            try {
                getSourceMetadata(tDir, tName,
                    sourceAxisNames, sourceDataNames, sourceDataTypes,
                    tSourceGlobalAttributes, tSourceAxisAttributes, tSourceDataAttributes);
                PrimitiveArray tSourceAxisValues[] = 
                    getSourceAxisValues(tDir, tName, sourceAxisNames);
                //sets haveValidSourceInfo=true if okay; throws Exception if not
                validateCompareSet(tDir, tName,
                    tSourceGlobalAttributes, 
                    tSourceAxisAttributes, tSourceAxisValues,
                    tSourceDataAttributes); 
                break; //successful, no need to continue
            } catch (Throwable t) {
                String reason = MustBe.throwableToShortString(t); 
                addBadFile(badFileMap, ftDirIndex.get(i), tName, ftLastMod.get(i), reason);
                String2.log("Error getting metadata for " + tDir + tName + "\n" + reason);
            }
        }
        //initially there are no files, so haveValidSourceInfo will still be false

        //get tAvailableFiles with available data files
        //and make tDirIndex and tFileList
        long elapsedTime = System.currentTimeMillis();
        //was tAvailableFiles with dir+name
        Table tFileTable = getFileInfo(fileDir, fileNameRegex, recursive);
        if (updateEveryNMillis > 0)
            watchDirectory = WatchDirectory.watchDirectoryAll(fileDir, recursive);
        StringArray tFileDirPA     = (StringArray)(tFileTable.getColumn(FileVisitorDNLS.DIRECTORY));
        StringArray tFileNamePA    = (StringArray)(tFileTable.getColumn(FileVisitorDNLS.NAME));
        LongArray   tFileLastModPA = (LongArray)  (tFileTable.getColumn(FileVisitorDNLS.LASTMODIFIED));
        tFileTable.removeColumn(FileVisitorDNLS.SIZE);
        int ntft = tFileNamePA.size();
        String msg = ntft + " files found in " + fileDir + 
            "\nregex=" + fileNameRegex + " recursive=" + recursive + 
            " time=" + (System.currentTimeMillis() - elapsedTime) + "ms";
        if (ntft == 0)
            //Just exit. Don't delete the dirTable and fileTable files!
            //The problem may be that a drive isn't mounted.
            throw new RuntimeException(msg); 
        if (verbose) String2.log(msg);

        //remove "badFiles" if they no longer exist (in tAvailableFiles)
        {
            //make hashset with all tAvailableFiles
            HashSet tFileSet = new HashSet(Math2.roundToInt(1.4 * ntft));
            for (int i = 0; i < ntft; i++)
                tFileSet.add(tFileDirPA.get(i) + tFileNamePA.get(i));

            Object badFileNames[] = badFileMap.keySet().toArray();
            int nMissing = 0;
            int nbfn = badFileNames.length;
            for (int i = 0; i < nbfn; i++) {
                 Object name = badFileNames[i];
                 if (!tFileSet.contains(name)) {
                     if (reallyVerbose) 
                        String2.log("previously bad file now missing: " + name);
                     nMissing++;
                     badFileMap.remove(name);
                 }
            }
            if (verbose) String2.log(
                "old nBadFiles size=" + nbfn + "   nMissing=" + nMissing);  
        } 

        //switch to dir indexes
        ShortArray tFileDirIndexPA = new ShortArray(ntft, false);  
        tFileTable.removeColumn(0);  //tFileDirPA col
        tFileTable.addColumn(0, "dirIndex", tFileDirIndexPA); //col 0, matches fileTable
        tFileTable.setColumnName(1, "fileList"); //col 1, matches fileTable
        String lastDir = "\u0000";
        int lastPo = -1;
        for (int i = 0; i < ntft; i++) {
            String tDir = tFileDirPA.get(i);
            int po = lastPo;
            if (!tDir.equals(lastDir)) {    //rare
                po = dirList.indexOf(tDir); //linear search, but should be short list
                if (po < 0) {
                    po = dirList.size();
                    dirList.add(tDir);
                }
                lastDir = tDir;
                lastPo = po;
            }
            tFileDirIndexPA.addInt(po);
        }
        tFileDirPA = null; //allow gc
        
        //sort fileTable and tFileTable based on dirIndex and file names
        elapsedTime = System.currentTimeMillis();
        fileTable.leftToRightSort(2); 
        tFileTable.leftToRightSort(2);
        if (verbose) String2.log("sortTime=" + (System.currentTimeMillis() - elapsedTime) + "ms");

        //remove any files in fileTable not in tFileTable  (i.e., the file was deleted)
        //I can step through fileTable and tFileTable since both sorted same way
        {
            int nft = ftFileList.size();
            BitSet keepFTRow = new BitSet(nft);  //all false
            int nFilesMissing = 0;
            int tPo = 0;
            for (int ftPo = 0; ftPo < nft; ftPo++) {
                int dirI       = ftDirIndex.get(ftPo);
                String fileS   = ftFileList.get(ftPo);

                //skip through tDir until it is >= ftDir
                while (tPo < ntft && tFileDirIndexPA.get(tPo) < dirI)
                    tPo++;

                //if dirs match, skip through tFile until it is >= ftFile
                boolean keep;
                if (tPo < ntft && tFileDirIndexPA.get(tPo) == dirI) {               
                    while (tPo < ntft && tFileDirIndexPA.get(tPo) == dirI && 
                        tFileNamePA.get(tPo).compareTo(fileS) < 0)
                        tPo++;
                    keep = tPo < ntft && tFileDirIndexPA.get(tPo) == dirI &&
                        tFileNamePA.get(tPo).equals(fileS);
                } else {
                    keep = false;
                }

                //deal with keep
                if (keep)
                    keepFTRow.set(ftPo, true);
                else {
                    nFilesMissing++;
                    if (reallyVerbose) 
                        String2.log("previously valid file now missing: " + 
                            dirList.get(dirI) + fileS);
                }
            }
            if (verbose)
                String2.log("old fileTable size=" + nft + "   nFilesMissing=" + nFilesMissing);  
            fileTable.justKeep(keepFTRow);
        }

        //update fileTable  by processing tFileTable
        int fileListPo = 0;  //next one to look at
        int tFileListPo = 0; //next one to look at
        long lastModCumTime = 0;
        int nReadFile = 0, nNoLastMod = 0;
        long readFileCumTime = 0;
        long removeCumTime = 0;
        int nUnchanged = 0, nRemoved = 0, nDifferentModTime = 0, nNew = 0;
        elapsedTime = System.currentTimeMillis();
        while (tFileListPo < tFileNamePA.size()) {
            int tDirI     = tFileDirIndexPA.get(tFileListPo);
            String tFileS = tFileNamePA.get(tFileListPo);
            int dirI       = fileListPo < ftFileList.size()? ftDirIndex.get(fileListPo) : Integer.MAX_VALUE;
            String fileS   = fileListPo < ftFileList.size()? ftFileList.get(fileListPo) : "\uFFFF";
            double lastMod = fileListPo < ftFileList.size()? ftLastMod.get(fileListPo)  : Double.MAX_VALUE;
            if (reallyVerbose) String2.log("#" + tFileListPo + 
                " file=" + dirList.get(tDirI) + tFileS);

            //is tLastMod available for tFile?
            long lmcTime = System.currentTimeMillis();
            long tLastMod = tFileLastModPA.get(tFileListPo);
            lastModCumTime += System.currentTimeMillis() - lmcTime;
            if (tLastMod == 0) { //0=trouble
                nNoLastMod++;
                String2.log("#" + tFileListPo + " reject because unable to get lastMod time: " + 
                    dirList.get(tDirI) + tFileS);                
                tFileListPo++;
                addBadFile(badFileMap, tDirI, tFileS, tLastMod, "Unable to get lastMod time.");
                continue;
            }

            //is tFile in badFileMap?
            Object bfi = badFileMap.get(tDirI + "/" + tFileS);
            if (bfi != null) {
                //tFile is in badFileMap
                Object bfia[] = (Object[])bfi;
                double bfLastMod = ((Double)bfia[0]).doubleValue();
                if (bfLastMod == tLastMod) {
                    //file hasn't been changed; it is still bad
                    tFileListPo++;
                    if (tDirI == dirI && tFileS.equals(fileS)) {
                        //remove it from cache   (Yes, a file may be marked bad (recently) and so still be in cache)
                        nRemoved++;
                        removeCumTime -= System.currentTimeMillis();
                        fileTable.removeRow(fileListPo);
                        removeCumTime += System.currentTimeMillis();
                    }
                    //go on to next tFile
                    continue;
                } else {
                    //file has been changed since being marked as bad; remove from badFileMap
                    badFileMap.remove(tDirI + "/" + tFileS);
                    //and continue processing this file
                }
            }

            //is tFile already in cache?
            if (tDirI == dirI && tFileS.equals(fileS) && tLastMod == lastMod) {
                if (reallyVerbose) String2.log("#" + tFileListPo + " already in cache");
                nUnchanged++;
                tFileListPo++;
                fileListPo++;
                continue;
            }

            //file in cache no longer exists: remove from fileTable
            if (dirI < tDirI ||
                (dirI == tDirI && fileS.compareTo(tFileS) < 0)) {
                if (verbose) String2.log("#" + tFileListPo + " file no longer exists: remove from cache: " +
                    dirList.get(dirI) + fileS);
                nRemoved++;
                removeCumTime -= System.currentTimeMillis();
                fileTable.removeRow(fileListPo);
                removeCumTime += System.currentTimeMillis();
                //tFileListPo isn't incremented, so it will be considered again in next iteration
                continue;
            }

            //tFile is new, or tFile is in ftFileList but time is different
            if (dirI == tDirI && fileS.equals(tFileS)) {
                if (verbose) String2.log("#" + tFileListPo + 
                    " already in cache (but time changed): " + dirList.get(tDirI) + tFileS);
                nDifferentModTime++;
            } else {
                //if new, add row to fileTable
                if (verbose) String2.log("#" + tFileListPo + " inserted in cache");
                nNew++;
                fileTable.insertBlankRow(fileListPo);
            }

            //gather file's info
            try {
                ftDirIndex.setInt(fileListPo, tDirI);
                ftFileList.set(fileListPo, tFileS);
                ftLastMod.set(fileListPo, tLastMod);

                //read axis values
                nReadFile++;
                long rfcTime = System.currentTimeMillis();
                PrimitiveArray[] tSourceAxisValues = getSourceAxisValues(
                    dirList.get(tDirI), tFileS, sourceAxisNames);
                readFileCumTime += System.currentTimeMillis() - rfcTime;

                //test that all axisVariable and dataVariable units are identical
                //this also tests if all dataVariables are present
                Attributes tSourceGlobalAttributes = new Attributes();
                Attributes tSourceAxisAttributes[] = new Attributes[nav];
                Attributes tSourceDataAttributes[] = new Attributes[ndv];
                for (int avi = 0; avi < nav; avi++) tSourceAxisAttributes[avi] = new Attributes();
                for (int dvi = 0; dvi < ndv; dvi++) tSourceDataAttributes[dvi] = new Attributes();
                getSourceMetadata(dirList.get(tDirI), tFileS,
                    sourceAxisNames, sourceDataNames, sourceDataTypes,
                    tSourceGlobalAttributes, tSourceAxisAttributes, tSourceDataAttributes);
                validateCompareSet( //throws Exception if not
                    dirList.get(tDirI), tFileS,
                    tSourceGlobalAttributes,
                    tSourceAxisAttributes, tSourceAxisValues,
                    tSourceDataAttributes);

                //store n, min, max, values
                int tnValues = tSourceAxisValues[0].size();
                ftNValues.set(fileListPo, tnValues);
                ftMin.set(fileListPo, tSourceAxisValues[0].getNiceDouble(0));
                ftMax.set(fileListPo, tSourceAxisValues[0].getNiceDouble(tnValues - 1));
                ftCsvValues.set(fileListPo, tSourceAxisValues[0].toString());

                tFileListPo++;
                fileListPo++;

            } catch (Throwable t) {
                String fullName = dirList.get(tDirI) + tFileS;
                msg = "#" + tFileListPo + " bad file: removing fileTable row for " + 
                    fullName + "\n" +
                    MustBe.throwableToString(t);
                String2.log(msg);
                nRemoved++;
                removeCumTime -= System.currentTimeMillis();
                fileTable.removeRow(fileListPo);
                removeCumTime += System.currentTimeMillis();
                tFileListPo++;
                if (System.currentTimeMillis() - tLastMod > 30 * Calendar2.MILLIS_PER_MINUTE) 
                    //>30 minutes old, so not still being ftp'd, so add to badFileMap
                    addBadFile(badFileMap, tDirI, tFileS, tLastMod, MustBe.throwableToShortString(t));
            }
        }
        if (verbose) String2.log("fileTable updated; time=" + (System.currentTimeMillis() - elapsedTime) + "ms");

        //sort fileTable by FT_MIN_COL
        elapsedTime = System.currentTimeMillis();
        fileTable.sort(new int[]{FT_MIN_COL}, new boolean[]{true});
        if (verbose) String2.log("2nd sortTime=" + (System.currentTimeMillis() - elapsedTime) + "ms");

        //finish up, validate, and save dirTable, fileTable, badFileMap
        PrimitiveArray sourceAxisValues0 = PrimitiveArray.factory(
            sourceAxisValues[0].elementClass(), 
            ftDirIndex.size(), //size is a guess: the minimum possible, but usually correct
            false); //not active
        updateValidateFileTable(dirList, ftDirIndex, ftFileList,
            ftMin, ftMax, ftStartIndex, ftNValues, ftCsvValues, 
            sourceAxisValues0);
        saveDirTableFileTableBadFiles(dirTable, fileTable, badFileMap); //throws Throwable

        msg = "\n  tFileNamePA.size=" + tFileNamePA.size() + 
                 " lastModCumTime=" + Calendar2.elapsedTimeString(lastModCumTime) + 
                 " avg=" + (lastModCumTime / Math.max(1, tFileNamePA.size())) + "ms" +
            "\n  dirTable.nRows=" + dirTable.nRows() +
            "\n  fileTable.nRows=" + fileTable.nRows() + 
            "\n    fileTableInMemory=" + fileTableInMemory + 
            "\n    nUnchanged=" + nUnchanged + 
            "\n    nRemoved=" + nRemoved + " (nNoLastMod=" + nNoLastMod + 
                 ") removedCumTime=" + Calendar2.elapsedTimeString(lastModCumTime) +
            "\n    nReadFile=" + nReadFile + 
                   " (nDifferentModTime=" + nDifferentModTime + " nNew=" + nNew + ")" +
                   " readFileCumTime=" + Calendar2.elapsedTimeString(readFileCumTime) +
                   " avg=" + (readFileCumTime / Math.max(1,nReadFile)) + "ms";
        if (verbose) String2.log(msg);
        if (!badFileMap.isEmpty()) {
            StringBuilder emailSB = new StringBuilder();
            emailSB.append(badFileMapToString(badFileMap, dirList));
            emailSB.append(msg + "\n\n");
            EDStatic.email(EDStatic.emailEverythingToCsv, errorInMethod, emailSB.toString());
        }

        //get source metadataFrom FIRST|LAST file (lastModifiedTime)
        sourceGlobalAttributes = new Attributes();
        sourceAxisAttributes   = new Attributes[nav];
        sourceDataAttributes   = new Attributes[ndv];
        for (int avi = 0; avi < nav; avi++) sourceAxisAttributes[avi] = new Attributes();
        for (int dvi = 0; dvi < ndv; dvi++) sourceDataAttributes[dvi] = new Attributes();
        int nMinMaxIndex[] = ftLastMod.getNMinMaxIndex();
        int tFileI = metadataFrom.equals(MF_FIRST)? nMinMaxIndex[1] : nMinMaxIndex[2];
        if (verbose) String2.log("getting metadataFrom " + dirList.get(ftDirIndex.get(tFileI)) + ftFileList.get(tFileI) +
            "\n  ftLastMod" + 
            " first=" + Calendar2.millisToIsoZuluString(Math.round(ftLastMod.get(nMinMaxIndex[1]))) + 
             " last=" + Calendar2.millisToIsoZuluString(Math.round(ftLastMod.get(nMinMaxIndex[2]))));
        getSourceMetadata(
            dirList.get(ftDirIndex.get(tFileI)),
            ftFileList.get(tFileI),
            sourceAxisNames, sourceDataNames, sourceDataTypes,
            sourceGlobalAttributes, sourceAxisAttributes, sourceDataAttributes);

        //make combinedGlobalAttributes
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");
        if (combinedGlobalAttributes.getString("cdm_data_type") == null)
            combinedGlobalAttributes.add("cdm_data_type", "Grid");
        if (combinedGlobalAttributes.get("sourceUrl") == null) {
            localSourceUrl = "(local files)"; //keep location private
            addGlobalAttributes.set(     "sourceUrl", localSourceUrl);
            combinedGlobalAttributes.set("sourceUrl", localSourceUrl);
        }

        //set combined sourceAxisValues[0]
        sourceAxisValues[0] = sourceAxisValues0;

        //make the axisVariables[]
        axisVariables = new EDVGridAxis[nav];
        for (int av = 0; av < nav; av++) {
            String tSourceName = sourceAxisNames.get(av);
            String tDestName = (String)tAxisVariables[av][1];
            Attributes tAddAtt = (Attributes)tAxisVariables[av][2];
            Attributes tSourceAtt = sourceAxisAttributes[av];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            axisVariables[av] = makeAxisVariable(av, tSourceName, tDestName, 
                tSourceAtt, tAddAtt, sourceAxisValues[av]);
        }

        //if aggregating time index, fix time_coverage_start/end global metadata
        if (timeIndex == 0) {
            EDVTimeGridAxis tga = (EDVTimeGridAxis)axisVariables[0];
            combinedGlobalAttributes.add("time_coverage_start", 
                tga.destinationToString(tga.destinationMin()));
            combinedGlobalAttributes.add("time_coverage_end", 
                tga.destinationToString(tga.destinationMax()));
        }

        //make the dataVariables[]
        dataVariables = new EDV[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            String tSourceName = sourceDataNames.get(dv);
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.length() == 0)
                tDestName = tSourceName;
            Attributes tSourceAtt = sourceDataAttributes[dv];
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            //PrimitiveArray taa = tAddAtt.get("_FillValue");
            //String2.log(">>taa " + tSourceName + " _FillValue=" + taa);
            String tSourceType = sourceDataTypes[dv];
            //if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType=" + tSourceType);

            if (tDestName.equals(EDV.TIME_NAME))
                throw new RuntimeException(errorInMethod +
                    "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
            else if (EDVTime.hasTimeUnits(tSourceAtt, tAddAtt)) 
                dataVariables[dv] = new EDVTimeStamp(tSourceName, tDestName,
                    tSourceAtt, tAddAtt, tSourceType);  
            else dataVariables[dv] = new EDV(tSourceName, tDestName, 
                tSourceAtt, tAddAtt, tSourceType, Double.NaN, Double.NaN); 
            dataVariables[dv].setActualRangeFromDestinationMinMax();
        }

        //accessibleViaFiles
        if (EDStatic.filesActive && tAccessibleViaFiles) {
            accessibleViaFilesDir = fileDir;
            accessibleViaFilesRegex = fileNameRegex;
            accessibleViaFilesRecursive = recursive;
        }

        //ensure the setup is valid
        ensureValid();

        //dirTable and fileTable InMemory?
        if (!fileTableInMemory) {
            dirTable = null;
            fileTable = null;
        }

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDGridFromFiles " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }

    /**
     * This ensures that ftMin and ftMax don't overlap,
     * recalculates ftStartIndex, 
     * and creates the cumulative sourceAxisValues[0].
     *
     * @param sourceAxisValues0 must be of the correct type
     * @throws RuntimeException if trouble
     */
    protected void updateValidateFileTable(StringArray dirList, 
        ShortArray ftDirIndex, StringArray ftFileList,
        DoubleArray ftMin, DoubleArray ftMax, 
        IntArray ftStartIndex, IntArray ftNValues, StringArray ftCsvValues, 
        PrimitiveArray sourceAxisValues0) {

        int nFiles = ftDirIndex.size();
        if (nFiles == 0)
            throw new RuntimeException("No valid data files were found.");
        for (int f = 1; f < nFiles; f++) { //1 since looking backward
            //min max overlap?
            if (ftMax.get(f - 1) > ftMin.get(f))
                throw new RuntimeException("Outer axis overlap between files.\n" +
                    "max=" + ftMax.get(f-1) + " for " + dirList.get(ftDirIndex.get(f-1)) + ftFileList.get(f-1) + "\n" +
                    "is greater than\n" +
                    "min=" + ftMin.get(f)   + " for " + dirList.get(ftDirIndex.get(f  )) + ftFileList.get(f  ) + "\n");
        }

        int tStart = 0;
        sourceAxisValues0.clear();
        for (int f = 0; f < nFiles; f++) { 
            //startIndex
            ftStartIndex.set(f, tStart);
            tStart += ftNValues.get(f);

            //sourceAxisValues
            StringArray sa = StringArray.fromCSV(ftCsvValues.get(f));
            if (sa.size() != ftNValues.get(f))
                throw new RuntimeException("Data source error: Observed nCsvValues=" + sa.size() + 
                    " != expected=" + ftNValues.get(f) + 
                    "\nfor file #" + f + "=" + dirList.get(ftDirIndex.get(f)) + ftFileList.get(f) +
                    "\ncsv=" + ftCsvValues.get(f));
            sourceAxisValues0.append(sa);
        }
    }


    /** 
     * This is used by the constructor and lowUpdate to:
     * ensure the incoming values are valid, and if so:
     * compare the incoming values with expected to ensure compatible,
     * or set expected if not already set.
     *
     * @param dirName
     * @param fileName
     * @param tSourceGlobalAttributes from the new file
     * @param tSourceAxisAttributes from the new file
     * @param tSourceAxisValues from the new file
     * @param tSourceDataAttributes from the new file
     * @throws RuntimeException if not compatible
     */
    protected void validateCompareSet(String dirName, String fileName,
        Attributes tSourceGlobalAttributes,
        Attributes tSourceAxisAttributes[], PrimitiveArray tSourceAxisValues[], 
        Attributes tSourceDataAttributes[]) {

        String emsg1 = "For " + dirName + fileName + ", the observed and expected values of ";

        //test axis values
        //test if ascending or descending
        String ascError = tSourceAxisValues[0].isAscending(); 
        if (ascError.length() > 0) {
            String desError = tSourceAxisValues[0].isDescending(); 
            if (desError.length() > 0)
                throw new RuntimeException("AxisVariable=" + sourceAxisNames.get(0) + 
                    "\nisn't ascending sorted (" + ascError + ")\n" +
                    "or descending sorted ("     + desError + ").");
        }

        //test for ties
        int firstTie = tSourceAxisValues[0].firstTie();
        if (firstTie >= 0)
            throw new RuntimeException("AxisVariable=" + sourceAxisNames.get(0) + 
                " has tied values: #" + firstTie + " and #" + (firstTie + 1) + 
                " both equal " + tSourceAxisValues[0].getNiceDouble(firstTie) + ".");

        if (haveValidSourceInfo) {
            //compare incoming to expected

            //compare globalAttributes
            //currently no tests

            //compare sourceAxisValues[1..]
            int nav = sourceAxisAttributes.length;
            for (int av = 1; av < nav; av++) {
                //be less strict?
                PrimitiveArray exp = sourceAxisValues[av];
                PrimitiveArray obs = tSourceAxisValues[av];
                boolean equal; 
                if (ensureAxisValuesAreExactlyEqual) {
                    String eqError = exp.testEquals(obs);
                    if (eqError.length() > 0)
                        throw new RuntimeException("axis=" + av + " values are not exactly equal.\n" +
                            eqError);
                } else {
                    String eqError = exp.almostEqual(obs);
                    if (eqError.length() > 0)
                        throw new RuntimeException("axis=" + av + 
                            " values are not even approximately equal.\n" + eqError);
                }
            }

            //compare axisAttributes
            for (int avi = 0; avi < nav; avi++) {
                Attributes tsaAtt = tSourceAxisAttributes[avi];
                Attributes  saAtt =  sourceAxisAttributes[avi];
                String emsg2 = " for sourceName=" + sourceAxisNames.get(avi) + " are different.";
                Test.ensureEqual(tsaAtt.getDouble("add_offset"),
                                  saAtt.getDouble("add_offset"),
                    emsg1 + "add_offset"    + emsg2);
                Test.ensureEqual(tsaAtt.getDouble("_FillValue"),
                                  saAtt.getDouble("_FillValue"),
                    emsg1 + "_FillValue"    + emsg2);
                Test.ensureEqual(tsaAtt.getDouble("missing_value"),
                                  saAtt.getDouble("missing_value"),
                    emsg1 + "missing_value" + emsg2);
                Test.ensureEqual(tsaAtt.getDouble("scale_factor"),
                                  saAtt.getDouble("scale_factor"),
                    emsg1 + "scale_factor"  + emsg2);
                String observedUnits = tsaAtt.getString("units");
                String expectedUnits =  saAtt.getString("units");
                if (!EDUnits.udunitsAreEquivalent(observedUnits, expectedUnits))
                    Test.ensureEqual(observedUnits, expectedUnits,
                        emsg1 + "units" + emsg2);
            }

            //compare sourceDataAttributes
            int ndv = sourceDataAttributes.length;
            for (int dvi = 0; dvi < ndv; dvi++) {
                Attributes tsdAtt = tSourceDataAttributes[dvi];
                Attributes  sdAtt =  sourceDataAttributes[dvi];
                String emsg2 = " for sourceName=" + sourceDataNames.get(dvi) + " are different.";
                Test.ensureEqual(tsdAtt.getDouble("add_offset"),
                                  sdAtt.getDouble("add_offset"),
                    emsg1 + "add_offset"    + emsg2);
                Test.ensureEqual(tsdAtt.getDouble("_FillValue"),
                                  sdAtt.getDouble("_FillValue"),
                    emsg1 + "_FillValue"    + emsg2);
                Test.ensureEqual(tsdAtt.getDouble("missing_value"),
                                  sdAtt.getDouble("missing_value"),
                    emsg1 + "missing_value" + emsg2);
                Test.ensureEqual(tsdAtt.getDouble("scale_factor"),
                                  sdAtt.getDouble("scale_factor"),
                    emsg1 + "scale_factor"  + emsg2);
                String observedUnits = tsdAtt.getString("units");
                String expectedUnits =  sdAtt.getString("units");
                if (!EDUnits.udunitsAreEquivalent(observedUnits, expectedUnits))
                    Test.ensureEqual(observedUnits, expectedUnits,
                        emsg1 + "units" + emsg2);
            }
        }

        //it passed!
        //set instance info if not already set
        if (!haveValidSourceInfo) {
            sourceGlobalAttributes = tSourceGlobalAttributes;
            sourceAxisAttributes   = tSourceAxisAttributes;
            sourceAxisValues       = tSourceAxisValues;
            sourceDataAttributes   = tSourceDataAttributes;
            //String2.log("sourceAxisValues=" + sourceAxisValues);
            haveValidSourceInfo = true;
        }

    }

    /**
     * This does the actual incremental update of this dataset 
     * (i.e., for real time datasets).
     * 
     * <p>Concurrency issue: The changes here are first prepared and 
     * then applied as quickly as possible (but not atomically!).
     * There is a chance that another thread will get inconsistent information
     * (from some things updated and some things not yet updated).
     * But I don't want to synchronize all activities of this class.
     *
     * @param msg the start of a log message, e.g., "update(thisDatasetID): ".
     * @param startUpdateMillis the currentTimeMillis at the start of this update.
     * @return true if a change was made
     * @throws Throwable if serious trouble. 
     *   For simple failures, this writes info to log.txt but doesn't throw an exception.
     *   If the dataset has changed in a serious / incompatible way and needs a full
     *     reload, this throws WaitThenTryAgainException 
     *     (usually, catcher calls LoadDatasets.tryToUnload(...) and EDD.requestReloadASAP(tDatasetID))..
     *   If the changes needed are probably fine but are too extensive to deal with here, 
     *     this calls requestReloadASAP() and returns without doing anything.
     */
    public boolean lowUpdate(String msg, long startUpdateMillis) throws Throwable {

        //Most of this lowUpdate code is identical in EDDGridFromFiles and EDDTableFromFiles

        //get the file events
        ArrayList<WatchEvent.Kind> eventKinds = new ArrayList();
        StringArray contexts  = new StringArray();
        int nEvents = watchDirectory.getEvents(eventKinds, contexts);
        if (nEvents == 0) {
            if (verbose) String2.log(msg + "found 0 events.");
            return false; //no changes
        }

        //if any OVERFLOW, reload this dataset
        for (int evi = 0; evi < nEvents; evi++) {
            if (eventKinds.get(evi) == WatchDirectory.OVERFLOW) {
                if (verbose) String2.log(msg +  
                    "caught OVERFLOW event in " + contexts.get(evi) + 
                    ", so I called requestReloadASAP() instead of making changes here."); 
                requestReloadASAP();
                return false; 
            }
        }

        //Don't try to sort out multiple events or event order, just note which files changed.
        long startLowUpdate = System.currentTimeMillis();
        eventKinds = null;
        contexts.sort();
        contexts.removeDuplicates();
        nEvents = contexts.size();

        //remove events for files that don't match fileNameRegex
        BitSet keep = new BitSet(nEvents); //initially all false
        for (int evi = 0; evi < nEvents; evi++) {
            String fullName = contexts.get(evi);
            String dirName = File2.getDirectory(fullName);
            String fileName = File2.getNameAndExtension(fullName);

            //if not a directory and fileName matches fileNameRegex, keep it
            if (fileName.length() > 0 && fileName.matches(fileNameRegex))
                keep.set(evi);
        }
        contexts.justKeep(keep);        
        nEvents = contexts.size();
        if (nEvents == 0) {
            if (verbose) String2.log(msg + "found 0 events related to files matching fileNameRegex.");
            return false; //no changes
        }

        //If too many events, call for reload.
        //This method isn't as nearly as efficient as full reload.
        if (nEvents > 10) {
            if (verbose) String2.log(msg + nEvents + 
                ">10 file events, so I called requestReloadASAP() instead of making changes here."); 
            requestReloadASAP();
            return false;
        }

        //get BadFile and FileTable info and make local copies
        ConcurrentHashMap badFileMap = readBadFileMap(); //already a copy of what's in file
        Table tDirTable; 
        Table tFileTable;
        if (fileTableInMemory) {
            tDirTable  = (Table)dirTable.clone();
            tFileTable = (Table)fileTable.clone(); 
        } else {
            tDirTable  = tryToLoadDirFileTable(datasetDir() +  DIR_TABLE_FILENAME); //shouldn't be null
            tFileTable = tryToLoadDirFileTable(datasetDir() + FILE_TABLE_FILENAME); //shouldn't be null
            Test.ensureNotNull(tDirTable, "dirTable");
            Test.ensureNotNull(tFileTable, "fileTable");
        }
        if (debugMode) String2.log(msg + "\n" +
            tDirTable.nRows() + " rows in old dirTable.  first 5 rows=\n" + 
                tDirTable.dataToCSVString(5) + 
            tFileTable.nRows() + " rows in old fileTable.  first 5 rows=\n" + 
                tFileTable.dataToCSVString(5));

        StringArray dirList = (StringArray)tDirTable.getColumn(0);
        ShortArray  ftDirIndex   = (ShortArray) tFileTable.getColumn(FT_DIR_INDEX_COL);
        StringArray ftFileList   = (StringArray)tFileTable.getColumn(FT_FILE_LIST_COL);        
        DoubleArray ftLastMod    = (DoubleArray)tFileTable.getColumn(FT_LAST_MOD_COL);
        IntArray    ftNValues    = (IntArray)   tFileTable.getColumn(FT_N_VALUES_COL); 
        DoubleArray ftMin        = (DoubleArray)tFileTable.getColumn(FT_MIN_COL); //sorted by
        DoubleArray ftMax        = (DoubleArray)tFileTable.getColumn(FT_MAX_COL);
        StringArray ftCsvValues  = (StringArray)tFileTable.getColumn(FT_CSV_VALUES_COL);
        IntArray    ftStartIndex = (IntArray)   tFileTable.getColumn(FT_START_INDEX_COL);

        //for each changed file
        int nChanges = 0; //BadFiles or FileTable
        int nav = sourceAxisAttributes.length;
        int ndv = sourceDataAttributes.length;
        for (int evi = 0; evi < nEvents; evi++) {
            String fullName = contexts.get(evi);
            String dirName = File2.getDirectory(fullName);
            String fileName = File2.getNameAndExtension(fullName);  //matched to fileNameRegex above

            //dirIndex   (dirName may not be in dirList!)
            int dirIndex = dirList.indexOf(dirName); //linear search, but should be short list

            //if it is an existing file, see if it is valid
            if (File2.isFile(fullName)) {
                //test that all axisVariable and dataVariable units are identical
                //this also tests if all dataVariables are present
                PrimitiveArray tSourceAxisValues[] = null;
                Attributes tSourceGlobalAttributes = new Attributes();
                Attributes tSourceAxisAttributes[] = new Attributes[nav];
                Attributes tSourceDataAttributes[] = new Attributes[ndv];
                for (int avi = 0; avi < nav; avi++) 
                    tSourceAxisAttributes[avi] = new Attributes();
                for (int dvi = 0; dvi < ndv; dvi++) 
                    tSourceDataAttributes[dvi] = new Attributes();
                String reasonBad = null;
                try {
                    getSourceMetadata(dirName, fileName,
                        sourceAxisNames, sourceDataNames, sourceDataTypes,
                        tSourceGlobalAttributes, tSourceAxisAttributes, tSourceDataAttributes);
                    tSourceAxisValues = getSourceAxisValues(dirName, fileName, sourceAxisNames);
                    validateCompareSet( //throws Exception (with fileName) if not compatible
                        dirName, fileName,
                        tSourceGlobalAttributes,
                        tSourceAxisAttributes, tSourceAxisValues,
                        tSourceDataAttributes);
                } catch (Exception e) {
                    reasonBad = e.getMessage(); 
                }
                
                if (reasonBad == null) { 
                    //File exists and is good/compatible.
                    nChanges++;

                    //ensure dirIndex is valid
                    boolean wasInFileTable = false;
                    if (dirIndex < 0) {
                        //dir isn't in dirList, so file can't be in BadFileMap or tFileTable.
                        //But I do need to add dir to dirList.
                        dirIndex = dirList.size();
                        dirList.add(dirName);
                        if (verbose)
                            String2.log(msg + 
                                "added a new dir to dirList (" + dirName + ") and ..."); 
                                //another msg is always for this file printed below
                    } else {
                        //Remove from BadFileMap if it is present
                        if (badFileMap.remove(dirIndex + "/" + fileName) != null) {
                            //It was in badFileMap
                            if (verbose)
                                String2.log(msg + 
                                    "removed from badFileMap a file that now exists and is valid, and ..."); 
                                    //another msg is always for this file printed below
                        }

                        //If file name already in tFileTable, remove it.
                        //Don't take shortcut, e.g., by searching with tMin.
                        //It is possible file had wrong name/wrong value before.
                        wasInFileTable = removeFromFileTable(dirIndex, fileName, 
                            tFileTable, ftDirIndex, ftFileList);

                    }

                    //Insert row in tFileTable for this valid file.
                    //Use exact binary search.  AlmostEquals isn't a problem. 
                    //  If file was in tFileTable, it is gone now (above).
                    double tMin = tSourceAxisValues[0].getDouble(0);
                    int fileListPo = ftMin.binaryFindFirstGE(0, ftMin.size() - 1, tMin); 
                    if (verbose)
                        String2.log(msg + 
                            (wasInFileTable? "updated a file in" : "added a file to") + 
                            " fileTable:\n  " + 
                            fullName);
                    tFileTable.insertBlankRow(fileListPo);
                    int tnValues = tSourceAxisValues[0].size();
                    ftDirIndex.setInt(fileListPo, dirIndex);
                    ftFileList.set(fileListPo, fileName);
                    ftLastMod.set(fileListPo, File2.getLastModified(fullName));
                    ftNValues.set(fileListPo, tnValues);
                    ftMin.set(fileListPo, tSourceAxisValues[0].getNiceDouble(0));
                    ftMax.set(fileListPo, tSourceAxisValues[0].getNiceDouble(tnValues - 1));
                    ftCsvValues.set(fileListPo, tSourceAxisValues[0].toString());
                    //ftStartIndex is updated when file is saved                    

                } else {
                    //File exists and is bad.

                    //Remove from tFileTable if it is there.
                    if (dirIndex >= 0) { //it might be in tFileTable
                        if (removeFromFileTable(dirIndex, fileName, 
                                tFileTable, ftDirIndex, ftFileList)) {
                            nChanges++;
                            if (verbose)
                                String2.log(msg + 
                                    "removed from fileTable a file that is now bad/incompatible:\n  " + 
                                    fullName + "\n  " + reasonBad);
                        } else {
                            if (verbose)
                                String2.log(msg + "found a bad file (but it wasn't in fileTable):\n  " + 
                                    fullName + "\n  " + reasonBad);
                        }
                    }

                    //add to badFileMap 
                    //No don't. Perhaps file is half written.
                    //Let main reload be the system to addBadFile
                }
            } else if (dirIndex >= 0) { 
                //File now doesn't exist, but it might be in badFile or tFileTable.

                //Remove from badFileMap if it's there.
                if (badFileMap.remove(dirIndex + "/" + fileName) != null) {
                    //Yes, it was in badFileMap
                    nChanges++;
                    if (verbose)
                        String2.log(msg + "removed from badFileMap a now non-existent file:\n  " + 
                            fullName);
                } else {
                    //If it wasn't in badFileMap, it might be in tFileTable.
                    //Remove it from tFileTable if it's there.
                    //Don't take shortcut, e.g., binary search with tMin.
                    //It is possible file had wrong name/wrong value before.
                    if (removeFromFileTable(dirIndex, fileName, 
                            tFileTable, ftDirIndex, ftFileList)) {
                        nChanges++;
                        if (verbose)
                            String2.log(msg + 
                                "removed from fileTable a file that now doesn't exist:\n  " + 
                                fullName);
                    } else {
                        if (verbose)
                            String2.log(msg + 
                                "a file that now doesn't exist wasn't in badFileMap or fileTable(!):\n  " + 
                                fullName);
                    }
                }

            } //else file doesn't exist and dir is not in dirList
              //so file can't be in badFileMap or tFileTable
              //so nothing needs to be done.
        }

        //if changes observed, make the changes to the dataset (as fast/atomically as possible)
        if (nChanges > 0) {
            //first, change local info only
            //finish up, validate, and save dirTable, fileTable, badFileMap
            PrimitiveArray sourceAxisValues0 = PrimitiveArray.factory(
                axisVariables[0].sourceValues().elementClass(), 
                ftDirIndex.size(), //size is a guess: the minimum possible, but usually correct
                false); //not active
            updateValidateFileTable(dirList, ftDirIndex, ftFileList,
                ftMin, ftMax, ftStartIndex, ftNValues, ftCsvValues, 
                sourceAxisValues0); //sourceAxisValues0 is filled 

            //then, change secondary parts of instance variables
            //update axisVariables[0]  (atomic: make new axisVar, then swap into place)     
            EDVGridAxis av0 = axisVariables[0];
            axisVariables[0] = makeAxisVariable(0, av0.sourceName(), av0.destinationName(),
                av0.sourceAttributes(), av0.addAttributes(), 
                sourceAxisValues0);
            av0 = axisVariables[0]; //the new one
            if (av0.destinationName().equals(EDV.TIME_NAME)) {
                combinedGlobalAttributes().set("time_coverage_start", av0.destinationMinString());
                combinedGlobalAttributes().set("time_coverage_end",   av0.destinationMaxString());
            }

            //finally: make the important instance changes that use the changes above 
            //(eg fileTable leads to seeing changed axisVariables[0])
            saveDirTableFileTableBadFiles(tDirTable, tFileTable, badFileMap); //throws Throwable
            if (fileTableInMemory) {
                //quickly swap into place
                dirTable  = tDirTable;
                fileTable = tFileTable; 
            }

            //after changes all in place
//Currently, update() doesn't trigger these changes.
//The problem is that some datasets might update every second, others every day.
//Even if they are done, perhaps do them in ERDDAP ((low)update return changes?)
//?update rss?
//?subscription and onchange actions?

        }      

        if (verbose)
            String2.log(msg + "succeeded. " + Calendar2.getCurrentISODateTimeStringLocal() +
                " nFileEvents=" + nEvents + 
                " nChangesMade=" + nChanges + 
                " time=" + (System.currentTimeMillis() - startLowUpdate) + "ms");
        return nChanges > 0;
    }

    /**
     * This is the default implementation of getFileInfo, which
     * gets file info from a locally accessible directory.
     * This is called in the middle of the constructor.
     * Some subclasses override this.
     *
     * @param recursive true if the file search should also search subdirectories
     * @return a table with columns with DIR, NAME, LASTMOD, and SIZE columns;
     * @throws Throwable if trouble
     */
    public Table getFileInfo(String fileDir, String fileNameRegex, boolean recursive) 
        throws Throwable {
        //String2.log("EDDTableFromFiles getFileInfo");
        return FileVisitorDNLS.oneStep(fileDir, fileNameRegex, recursive, false); //dirsToo
    }

    /** 
     * Try to load the dirTable or fileTable.
     *
     * @param fileName datasetDir() + DIR_TABLE_FILENAME or FILE_TABLE_FILENAME
     * @return the dirTable fileTable (null if trouble).  (No exception if trouble.)
     */
    protected Table tryToLoadDirFileTable(String fileName) {
        Table table = null;
        try {
            if (File2.isFile(fileName)) {
                table = new Table();
                table.readFlatNc(fileName, null, 0); //it logs fileName and nRows=
            } else {
                if (verbose) String2.log("table file doesn't exist: " + fileName);  
            }
        } catch (Throwable t) {
            table = null; 
            String2.log(String2.ERROR + " reading table " + fileName + "\n" + 
                MustBe.throwableToString(t));  
        }
        return table;
    }


    /**
     * This gets sourceGlobalAttributes and sourceDataAttributes from the specified 
     * source file (or does nothing if that isn't possible).
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames
     * @param sourceDataNames the names of the desired source data columns.
     * @param sourceDataTypes the data types of the desired source columns 
     *    (e.g., "String" or "float") 
     * @param sourceGlobalAttributes should be an empty Attributes. It will be populated by this method
     * @param sourceAxisAttributes should be an array of empty Attributes. It will be populated by this method
     * @param sourceDataAttributes should be an array of empty Attributes. It will be populated by this method
     * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or sourceDataName not found).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public abstract void getSourceMetadata(String fileDir, String fileName, 
        StringArray sourceAxisNames,
        StringArray sourceDataNames, String sourceDataTypes[],
        Attributes sourceGlobalAttributes, 
        Attributes sourceAxisAttributes[],
        Attributes sourceDataAttributes[]) throws Throwable;

    /**
     * This gets source axis values from one file.
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames the names of the desired source axis variables.
     * @return a PrimitiveArray[] with the results (with the requested sourceDataTypes).
     *   It needn't set sourceGlobalAttributes or sourceDataAttributes
     *   (but see getSourceMetadata).
     * @throws Throwable if trouble (e.g., invalid file).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public abstract PrimitiveArray[] getSourceAxisValues(String fileDir, String fileName, 
        StringArray sourceAxisNames) throws Throwable;

    /**
     * This gets source data from one file.
     *
     * @param fileDir
     * @param fileName
     * @param tDataVariables the desired data variables
     * @param tConstraints  where the first axis variable's constraints
     *   have been customized for this file.
     * @return a PrimitiveArray[] with an element for each tDataVariable with the dataValues.
     *   <br>The dataValues are straight from the source, not modified.
     *   <br>The primitiveArray dataTypes are usually the sourceDataTypeClass,
     *     but can be any type. EDDGridFromFiles will convert to the sourceDataTypeClass.
     *   <br>Note the lack of axisVariable values!
     * @throws Throwable if trouble (e.g., invalid file).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public abstract PrimitiveArray[] getSourceDataFromFile(String fileDir, String fileName, 
        EDV tDataVariables[], IntArray tConstraints) throws Throwable;

    /** 
     * This gets data (not yet standardized) from the data source for this EDDGrid.     
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
     *   are the dataValues (using the sourceDataTypeClass).
     *   Both the axisValues and dataValues are straight from the source,
     *   not modified.
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public PrimitiveArray[] getSourceData(EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable {

        //get a local reference to dirTable and fileTable
        Table tDirTable = dirTable;
        if (tDirTable == null)
            tDirTable = tryToLoadDirFileTable(datasetDir() + DIR_TABLE_FILENAME);
        Table tFileTable = fileTable;
        if (verbose && tFileTable != null)
            String2.log("  fileTableInMemory=true");
        if (tFileTable == null && tDirTable != null) 
            tFileTable = tryToLoadDirFileTable(datasetDir() + FILE_TABLE_FILENAME);
        if (tDirTable == null || tFileTable == null) 
            throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain +
                "\n(Details: unable to read fileTable.)"); 

        //get the tDirTable and tFileTable PrimitiveArrays
        StringArray dirList      = (StringArray)tDirTable.getColumn(0);
        ShortArray  ftDirIndex   = (ShortArray) tFileTable.getColumn(FT_DIR_INDEX_COL);
        StringArray ftFileList   = (StringArray)tFileTable.getColumn(FT_FILE_LIST_COL);        
        DoubleArray ftLastMod    = (DoubleArray)tFileTable.getColumn(FT_LAST_MOD_COL);
        IntArray    ftNValues    = (IntArray)   tFileTable.getColumn(FT_N_VALUES_COL);
        DoubleArray ftMin        = (DoubleArray)tFileTable.getColumn(FT_MIN_COL);
        DoubleArray ftMax        = (DoubleArray)tFileTable.getColumn(FT_MAX_COL);
        StringArray ftCsvValues  = (StringArray)tFileTable.getColumn(FT_CSV_VALUES_COL);
        IntArray    ftStartIndex = (IntArray)   tFileTable.getColumn(FT_START_INDEX_COL);

        //make results[]
        int nav = axisVariables.length;
        int ndv = tDataVariables.length;
        PrimitiveArray results[] = new PrimitiveArray[nav + ndv];
        for (int avi = 0; avi < nav; avi++)
            results[avi] = axisVariables[avi].sourceValues().subset(
                tConstraints.get(avi*3 + 0),
                tConstraints.get(avi*3 + 1),
                tConstraints.get(avi*3 + 2));
        for (int dvi = 0; dvi < ndv; dvi++) {
            //String2.log("!dvi#" + dvi + " " + tDataVariables[dvi].destinationName() + " " + tDataVariables[dvi].sourceDataTypeClass().toString());
            results[nav + dvi] = PrimitiveArray.factory(
                tDataVariables[dvi].sourceDataTypeClass(), 64, false);
        }
        IntArray ttConstraints = (IntArray)tConstraints.clone();
        int nFiles = ftStartIndex.size();
        int axis0Start  = tConstraints.get(0);
        int axis0Stride = tConstraints.get(1);
        int axis0Stop   = tConstraints.get(2);
        int ftRow = 0;
        while (axis0Start <= axis0Stop) {

            //find next relevant file
            ftRow = ftStartIndex.binaryFindLastLE(ftRow, nFiles - 1, axis0Start);
            int tNValues = ftNValues.get(ftRow);
            int tStart = axis0Start - ftStartIndex.get(ftRow);
            int tStop = tStart;
            //get as many axis0 values as possible from this file
            //                    (in this file, if this file had all the remaining values)
            int lookMax = Math.min(tNValues - 1, axis0Stop - ftStartIndex.get(ftRow));
            while (tStop + axis0Stride <= lookMax) 
                tStop += axis0Stride;          
            //String2.log("!tStart=" + tStart + " stride=" + axis0Stride + " tStop=" + tStop + " tNValues=" + tNValues);

            //set ttConstraints
            ttConstraints.set(0, tStart);
            ttConstraints.set(2, tStop);
            String tFileDir  = dirList.get(ftDirIndex.get(ftRow));
            String tFileName = ftFileList.get(ftRow);
            if (reallyVerbose)
                String2.log("ftRow=" + ftRow + " axis0Start=" + axis0Start +
                    " local=" + tStart + ":" + axis0Stride + ":" + tStop +
                    " " + tFileDir + tFileName);

            //get the data
            PrimitiveArray[] tResults;
            try {
                tResults = getSourceDataFromFile(tFileDir, tFileName, 
                    tDataVariables, ttConstraints);
                //String2.log("!tResults[0]=" + tResults[0].toString());
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

                //if too much data, rethrow t
                String tToString = t.toString();
                if (tToString.indexOf(Math2.memoryTooMuchData) >= 0)
                    throw t;

                //sleep and give it one more try
                try {
                    Thread.sleep(1000); //not Math2.sleep(1000);
                    tResults = getSourceDataFromFile(tFileDir, tFileName, 
                        tDataVariables, ttConstraints);
                } catch (Throwable t2) {
                    EDStatic.rethrowClientAbortException(t2);  //first thing in catch{}

                    //mark the file as bad   and reload the dataset
                    addBadFileToTableOnDisk(ftDirIndex.get(ftRow), tFileName, 
                        ftLastMod.get(ftRow), MustBe.throwableToShortString(t)); 
                    //an exception here will cause data request to fail (as it should)
                    throw new WaitThenTryAgainException(t);  //original exception
                }
            }

            //merge dataVariables   (converting to sourceDataTypeClass if needed)
            for (int dv = 0; dv < ndv; dv++) 
                results[nav + dv].append(tResults[dv]);
            //String2.log("!merged tResults[1stDV]=" + results[nav].toString());

            //set up for next while-iteration
            axis0Start += (tStop - tStart) + axis0Stride; 
            ftRow++; //first possible file is next file
        }
        return results;
    }


}
