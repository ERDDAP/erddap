/* 
 * EDDTableFromFiles Copyright 2008, NOAA.
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

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.WatchDirectory;

import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.EDUnits;
import gov.noaa.pfel.erddap.variable.*;

import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.*;

/** 
 * This class represents a virtual table of data from by aggregating a collection of data files.
 * <br>The presumption is that the entire dataset can be read reasonable quickly
 *   (from the local files, unlike remote data) and all variable's min and max info
 *   can be gathered (for each file) 
 *   and cached (facilitating handling constraints in data requests).
 * <br>And file data can be cached and reused because each file has a lastModified 
 *   time and size which can be used to detect if file is unchanged.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2008-04-12
 */
public abstract class EDDTableFromFiles extends EDDTable{ 

    public final static String MF_FIRST = "first", MF_LAST = "last";
    public static int suggestedUpdateEveryNMillis = 10000;
    public static int suggestUpdateEveryNMillis(String tFileDir) {
        return String2.isRemote(tFileDir)? 0 : suggestedUpdateEveryNMillis;
    }
    /** Don't set this to true here.  Some test methods set this to true temporarily. */
    protected static boolean testQuickRestart = false;


    //set by constructor
    protected String fileDir;
    protected String fileNameRegex;
    protected boolean recursive;
    protected String pathRegex;
    protected String metadataFrom;       
    protected String preExtractRegex, postExtractRegex, extractRegex, 
        columnNameForExtract;  // will be "" if not in use
    protected Pattern preExtractPattern, postExtractPattern,
        extractPattern; //will be null if not in use
    protected String sortedColumnSourceName; //may be "", won't be null
    /**
     * filesAreLocal true if files are on a local hard drive or false if files are remote.
     * 1) A failure when reading a local file, causes file to be marked as bad and dataset reloaded;
     *   but a remote failure doesn't.
     * 2) For remote files, the bad file list is rechecked every time dataset is reloaded.
     */
    protected boolean filesAreLocal;
    protected String charset;  //may be null or ""
    protected int columnNamesRow = 1, firstDataRow = 2; 
    //for ColumnarAscii only: the startColumn and stopColumn of each 
    //  dataVariable on each line of the file (0..)
    protected int startColumn[], stopColumn[]; 
    protected boolean removeMVRows = true; //just used by EDDTableFromMultidimNcFiles

    //source info
    protected StringArray sourceDataNames;
    protected StringArray safeSourceDataNames;
    protected String sourceDataTypes[];

    //arrays to hold expected source add_offset, fillValue, missingValue, scale_factor, units
    //for NEC (No Extract Column and no fixed value columns) dv columns
    protected StringArray sourceDataNamesNEC;
    protected String      sourceDataTypesNEC[];
    protected double expectedAddOffsetNEC[]; 
    protected double expectedFillValueNEC[]; 
    protected double expectedMissingValueNEC[];
    protected double expectedScaleFactorNEC[]; 
    protected String expectedUnitsNEC[];
    //arrays to hold addAttributes mv info for NEC dv columns
    //  so source min max can be determined (skipping missing values)
    protected double addAttFillValueNEC[];
    protected double addAttMissingValueNEC[];

    /** Columns in the File Table */
    protected final static int 
        FT_DIR_INDEX_COL=0, //useful that it is #0   (tFileTable uses same positions)
        FT_FILE_LIST_COL=1, //useful that it is #1
        FT_LAST_MOD_COL=2, 
        FT_SIZE_COL=3, 
        FT_SORTED_SPACING_COL=4;
    //then 3 cols for each dataVariable: sourceName + _min_|_max_|_hasNaN starting at dv0
    protected final static int dv0 = 5;
    int     fileTableSortColumns[];   //null if not active
    boolean fileTableSortAscending[]; //size matches fileTableSortcolumns, all true


    /** minMaxTable has a col for each dv; row0=min for all files, row1=max for all files.
        The values are straight from the source; scale_factor and add_offset haven't been applied. 
        Even time is stored as raw source values; see "//EEEK!!!" below.  */
    protected Table minMaxTable; 
    protected int sortedDVI = -1;
    protected String filesChanged = ""; 

    protected int extractedColNameIndex = -1;

    protected long cumNNotRead = 0;  //either don't have matching data or do ('distinct' and 1 value matches)
    protected long cumNReadHaveMatch = 0, cumNReadNoMatch = 0; //read the data file to look for matching data
    protected WatchDirectory watchDirectory;

    //dirTable and fileTable inMemory (default=false)
    protected boolean fileTableInMemory = false;
    protected Table dirTable; //one column with dir names
    protected Table fileTable;

    /**
     * This constructs an EDDTableFromFiles based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="[subclassName]"&gt; 
     *    having just been read.  
     * @return an EDDTableFromFiles.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromFiles fromXml(Erddap erddap, 
        SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromFiles(xmlReader)...");
        boolean tIsLocal = false; //not actually used
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        String tType = xmlReader.attributeValue("type"); 
        Attributes tGlobalAttributes = null;
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        int tUpdateEveryNMillis = 0;
        String tAccessibleTo = null;
        String tGraphsAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        boolean tFileTableInMemory = false;
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        String tFileDir = null;
        String tFileNameRegex = ".*";
        boolean tRecursive = false;
        String tPathRegex = ".*";
        boolean tAccessibleViaFiles = false;
        String tMetadataFrom = MF_LAST;       
        String tPreExtractRegex = "", tPostExtractRegex = "", tExtractRegex = "";
        String tColumnNameForExtract = "";
        String tSortedColumnSourceName = "";
        String tSortFilesBySourceNames = "";
        boolean tRemoveMVRows = true; //used by EDDTableFromMultidimNcFiles
        String tSpecialMode = "";
        String tCharset = null;
        int tColumnNamesRow = 1, tFirstDataRow = 2; //relevant for ASCII files only
        boolean tSourceNeedsExpandedFP_EQ = true;
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
            else if (localTags.equals( "<dataVariable>")) tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<updateEveryNMillis>")) {}
            else if (localTags.equals("</updateEveryNMillis>")) tUpdateEveryNMillis = String2.parseInt(content); 
            else if (localTags.equals( "<fileDir>")) {} 
            else if (localTags.equals("</fileDir>")) tFileDir = content; 
            else if (localTags.equals( "<fileNameRegex>")) {}
            else if (localTags.equals("</fileNameRegex>")) tFileNameRegex = content; 
            else if (localTags.equals( "<recursive>")) {}
            else if (localTags.equals("</recursive>")) tRecursive = String2.parseBoolean(content); 
            else if (localTags.equals( "<pathRegex>")) {}
            else if (localTags.equals("</pathRegex>")) tPathRegex = content; 
            else if (localTags.equals( "<accessibleViaFiles>")) {}
            else if (localTags.equals("</accessibleViaFiles>")) tAccessibleViaFiles = String2.parseBoolean(content); 
            else if (localTags.equals( "<metadataFrom>")) {}
            else if (localTags.equals("</metadataFrom>")) tMetadataFrom = content; 
            else if (localTags.equals( "<nDimensions>")) {}
            else if (localTags.equals("</nDimensions>")) {} //tNDimensions = String2.parseInt(content); 
            else if (localTags.equals( "<preExtractRegex>")) {}
            else if (localTags.equals("</preExtractRegex>")) tPreExtractRegex = content; 
            else if (localTags.equals( "<postExtractRegex>")) {}
            else if (localTags.equals("</postExtractRegex>")) tPostExtractRegex = content; 
            else if (localTags.equals( "<extractRegex>")) {}
            else if (localTags.equals("</extractRegex>")) tExtractRegex = content; 
            else if (localTags.equals( "<columnNameForExtract>")) {}
            else if (localTags.equals("</columnNameForExtract>")) tColumnNameForExtract = content; 
            else if (localTags.equals( "<sortedColumnSourceName>")) {}
            else if (localTags.equals("</sortedColumnSourceName>")) tSortedColumnSourceName = content; 
            else if (localTags.equals( "<sortFilesBySourceNames>")) {}
            else if (localTags.equals("</sortFilesBySourceNames>")) tSortFilesBySourceNames = content; 
            else if (localTags.equals( "<charset>")) {}
            else if (localTags.equals("</charset>")) tCharset = content; 
            else if (localTags.equals( "<columnNamesRow>")) {}
            else if (localTags.equals("</columnNamesRow>")) tColumnNamesRow = String2.parseInt(content); 
            else if (localTags.equals( "<firstDataRow>")) {}
            else if (localTags.equals("</firstDataRow>")) tFirstDataRow = String2.parseInt(content); 
            else if (localTags.equals( "<sourceNeedsExpandedFP_EQ>")) {}
            else if (localTags.equals("</sourceNeedsExpandedFP_EQ>")) tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content); 
            else if (localTags.equals( "<specialMode>")) {}
            else if (localTags.equals("</specialMode>")) tSpecialMode = content; 
            else if (localTags.equals( "<fileTableInMemory>")) {}
            else if (localTags.equals("</fileTableInMemory>")) tFileTableInMemory = String2.parseBoolean(content); 
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<sosOfferingPrefix>")) {}
            else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content; 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 
            else if (localTags.equals( "<isLocal>")) {}
            else if (localTags.equals("</isLocal>")) tIsLocal = String2.parseBoolean(content); 
            else if (localTags.equals( "<removeMVRows>")) {}
            else if (localTags.equals("</removeMVRows>")) tRemoveMVRows = String2.parseBoolean(content); 

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        if (tType == null)
            tType = "";
        if (tType.equals("EDDTableFromAsciiFiles")) {
            return new EDDTableFromAsciiFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);

        } else if (tType.equals("EDDTableFromAwsXmlFiles")) {
            return new EDDTableFromAwsXmlFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames,
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);

        } else if (tType.equals("EDDTableFromColumnarAsciiFiles")) {
            return new EDDTableFromColumnarAsciiFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);

        } else if (tType.equals("EDDTableFromMultidimNcFiles")) { 
            return new EDDTableFromMultidimNcFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);

        } else if (tType.equals("EDDTableFromNcFiles")) { 
            return new EDDTableFromNcFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);

        } else if (tType.equals("EDDTableFromNcCFFiles")) {
            return new EDDTableFromNcCFFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows); 

        /*} else if (tType.equals("EDDTableFromPostNcFiles")) {
            return new EDDTableFromNcFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);
        */
        } else if (tType.equals("EDDTableFromHyraxFiles")) {

            String qrName = quickRestartFullFileName(tDatasetID);
            long tCreationTime = System.currentTimeMillis(); //used below
            if (EDStatic.quickRestart && 
                EDStatic.initialLoadDatasets() && 
                File2.isFile(qrName)) {

                //quickRestart
                //set creationTimeMillis to time of previous creation, so next time
                //to be reloaded will be same as if ERDDAP hadn't been restarted.
                tCreationTime = File2.getLastModified(qrName); //0 if trouble
                if (verbose)
                    String2.log("  quickRestart " + tDatasetID + " previous=" + 
                        Calendar2.millisToIsoZuluString(tCreationTime) + "Z");

            } else {
                //make downloadFileTasks
                EDDTableFromHyraxFiles.makeDownloadFileTasks(tDatasetID,
                    tGlobalAttributes.getString("sourceUrl"), 
                    tFileNameRegex, tRecursive, tPathRegex);

                //save quickRestartFile (file's timestamp is all that matters)
                Attributes qrAtts = new Attributes();
                qrAtts.add("datasetID", tDatasetID);
                File2.makeDirectory(File2.getDirectory(qrName));
                NcHelper.writeAttributesToNc(qrName, qrAtts);
            }

            EDDTableFromFiles tEDDTable = new EDDTableFromHyraxFiles(tDatasetID,
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);

            tEDDTable.creationTimeMillis = tCreationTime;
            return tEDDTable;

        } else if (tType.equals("EDDTableFromThreddsFiles")) {

            String qrName = quickRestartFullFileName(tDatasetID);
            long tCreationTime = System.currentTimeMillis(); //used below
            if (EDStatic.quickRestart && 
                EDStatic.initialLoadDatasets() && 
                File2.isFile(qrName)) {

                //quickRestart
                //set creationTimeMillis to time of previous creation, so next time
                //to be reloaded will be same as if ERDDAP hadn't been restarted.
                tCreationTime = File2.getLastModified(qrName); //0 if trouble
                if (verbose)
                    String2.log("  quickRestart " + tDatasetID + " previous=" + 
                        Calendar2.millisToIsoZuluString(tCreationTime) + "Z");

            } else {
                //make downloadFileTasks
                EDDTableFromThreddsFiles.makeDownloadFileTasks(tDatasetID,
                    tGlobalAttributes.getString("sourceUrl"), 
                    tFileNameRegex, tRecursive, tPathRegex, tSpecialMode);

                //save quickRestartFile (file's timestamp is all that matters)
                Attributes qrAtts = new Attributes();
                qrAtts.add("datasetID", tDatasetID);
                File2.makeDirectory(File2.getDirectory(qrName));
                NcHelper.writeAttributesToNc(qrName, qrAtts);
            }

            EDDTableFromFiles tEDDTable = new EDDTableFromThreddsFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
                tCharset, tColumnNamesRow, tFirstDataRow,
                tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);

            tEDDTable.creationTimeMillis = tCreationTime;
            return tEDDTable;

        } else if (tType.equals("EDDTableFromWFSFiles")) {

            String fileDir = EDStatic.fullCopyDirectory + tDatasetID + "/"; 
            String fileName = "data.tsv";
            long tCreationTime = System.currentTimeMillis(); //used below
            if (EDStatic.quickRestart && EDStatic.initialLoadDatasets() && 
                File2.isFile(fileDir + fileName)) {

                //quickRestart
                //set creationTimeMillis to time of previous creation, so next time
                //to be reloaded will be same as if ERDDAP hadn't been restarted.
                tCreationTime = File2.getLastModified(fileDir + fileName); //0 if trouble
                if (verbose)
                    String2.log("  quickRestart " + tDatasetID + " previous=" + 
                        Calendar2.millisToIsoZuluString(tCreationTime) + "Z");

            } else {
                //download the file  (its timestamp will be *now*)
                File2.makeDirectory(fileDir);
                String error = EDDTableFromWFSFiles.downloadData(
                    tGlobalAttributes.getString("sourceUrl"),
                    tGlobalAttributes.getString("rowElementXPath"),
                    fileDir + fileName);
                if (error.length() > 0) 
                    String2.log(error);
            }

            return new EDDTableFromWFSFiles(tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,  
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis, 
                fileDir,    //force fileDir
                ".*\\.tsv", //force fileNameRegex
                false,      //force !recursive, 
                ".*",       //irrelevant pathRegex
                tMetadataFrom, 
                "UTF-8",    //force charset
                1,          //force columnNamesRow, 
                3,          //force firstDataRow,
                "","","","",//force tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
                tSortedColumnSourceName, tSortFilesBySourceNames, 
                tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
                tAccessibleViaFiles, tRemoveMVRows);

        //} else if (tType.equals("EDDTableFrom???Files")) {
        //    return new EDDTableFromFiles(tDatasetID, 
        //        tAccessibleTo, tGraphsAccessibleTo,
        //        tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
        //        tDefaultDataQuery, tDefaultGraphQuery, 
        //        tGlobalAttributes,
        //        ttDataVariables,
        //        tReloadEveryNMinutes, tUpdateEveryNMillis, 
        //        tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom, 
        //        tCharset, tColumnNamesRow, tFirstDataRow,
        //        tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
        //        tSortedColumnSourceName, tSortFilesBySourceNames,
        //        tSourceNeedsExpandedFP_EQ, tFileTableInMemory, 
        //        tAccessibleViaFiles, tRemoveMVRows);
        } else {
            throw new Exception("type=\"" + tType + 
                "\" needs to be added to EDDTableFromFiles.fromXml at end.");
        }
    }

    /**
     * The constructor.
     *
     * @param tClassName e.g., EDDTableFromNcFiles
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
     *   Special case: if combinedGlobalAttributes name="license", any instance of value="[standard]"
     *     will be converted to the EDStatic.standardLicense.
     * @param tDataVariables is an Object[nDataVariables][3 or 4]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source, 
     *         without the outer or inner sequence name),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>[3]=String source dataType (e.g., "int", "float", "String"). 
     *        Some data sources have ambiguous data types, so it needs to be specified here.
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     *    <p>If there is a time variable,  
     *      either tAddAttributes (read first) or tSourceAttributes must have "units"
     *      which is either <ul>
     *      <li> a UDUunits string (containing " since ")
     *        describing how to interpret source time values 
     *        (which should always be numeric since they are a dimension of a grid)
     *        (e.g., "seconds since 1970-01-01T00:00:00").
     *      <li> a org.joda.time.format.DateTimeFormat string
     *        (which is compatible with java.text.SimpleDateFormat) describing how to interpret 
     *        string times  (e.g., the ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see 
     *        http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html or 
     *        https://docs.oracle.com/javase/8/docs/api/index.html?java/text/SimpleDateFormat.html)).
     *      </ul>
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tFileDir the base directory where the files are located.
     *    For EDDTableFromHyraxFiles, this is the url of the main .html page,
     *    e.g., http://biloxi-bay.ssc.hpc.msstate.edu/dods-bin/nph-dods/WCOS/nmsp/wcos/
     * @param tFileNameRegex the regex which determines which files in 
     *    the directories are to be read (use .* for all)
     *    <br>You can use .* for all, but it is better to be more specific.
     *        For example, .*\.nc will get all files with the extension .nc.
     * @param tRecursive if true, this class will look for files in the
     *    fileDir and all subdirectories
     * @param tMetadataFrom this indicates the file to be used
     *    to extract source metadata (first/last based on sorted file lastModifiedTime).
     *    Valid values are "first", "penultimate", "last".
     * @param tCharset the charset; relevant for ASCII files only
     * @param tColumnNamesRow the number of the row with column names (1..; usually 1, may be 0 (none)); relevant for ASCII files only.
     * @param tDataRow the number of the row with column names (1..; usually 2); relevant for ASCII files only.
     * @param tPreExtractRegex may be "" or null if not needed.
     *    If present, this usually begins with "^" to match the beginning of the file name.
     *    If present, this is used to remove text from the start of the file name.
     *    The removal only occurs if the regex is matched.
     * @param tPostExtractRegex may be "" or null if not needed.
     *    If present, this usually ends with "$" to match the beginning of the file name.
     *    If present, this is used to remove text from the end of the file name.
     *    The removal only occurs if the regex is matched.
     * @param tExtractRegex may be "" or null if not needed. 
     *    Use ".*" to match the entire file name.
     *    If present, this is used after preExtractRegex and postExtractRegex
     *    to extract a string from the file name (e.g., stationID).
     *    If the regex isn't matched, the entire file name is used (minus preExtract and postExtract).
     * @param tColumnNameForExtract the data column name for the extracted Strings.
     *    This column name must be in the tDataVariables list as a source column name 
     *    (with any data type).
     * @param tSortedColumnSourceName the source name of the numeric column that the
     *    data files are usually already sorted by within each file (use null or "" for none), e.g., "time".
     *    It is ok if not all files are sorted by this column.
     *    If present, this can greatly speed up some data requests.
     * @param tSortFilesBySourceNames is a space-separated list of source variable names
     *    specifying how the internal list of files should be sorted (in ascending order).
     *    <br>It is the minimum value of the specified columns in each file that is used for sorting.
     *    <br>When a data request is filled, data is obtained from the files in this order.
     *    <br>Thus it largely determines the overall order of the data in the response.
     *    <br>If you specify more than one column name, 
     *    <br>the second name is used if there is a tie for the first column;
     *    <br>the third is used if there is a tie for the first and second columns; ...
     *    <br>It is optional (the default is fileDir+fileName order).
     * @param tSourceNeedsExpandedFP_EQ
     * @param tRemoveMVRows
     * @throws Throwable if trouble
     */
    public EDDTableFromFiles(String tClassName, String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles, boolean tRemoveMVRows) 
        throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromFiles " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromFiles(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = tClassName;
        datasetID = tDatasetID; 

        //ensure valid for creation of datasetInfo files below
        if (!String2.isFileNameSafe(datasetID)) 
            throw new IllegalArgumentException(errorInMethod + "datasetID=" + datasetID + " isn't fileNameSafe.");
        File2.makeDirectory(datasetDir());  //based on datasetID
        String dirTableFileName  = datasetDir() +  DIR_TABLE_FILENAME;
        String fileTableFileName = datasetDir() + FILE_TABLE_FILENAME;

        setAccessibleTo(tAccessibleTo);
        setGraphsAccessibleTo(tGraphsAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        setUpdateEveryNMillis(tUpdateEveryNMillis);
        fileTableInMemory = tFileTableInMemory;
        fileDir = tFileDir;
        fileNameRegex = tFileNameRegex;
        recursive = tRecursive;
        pathRegex = tPathRegex == null || tPathRegex.length() == 0? ".*": tPathRegex;
        metadataFrom = tMetadataFrom;
        charset = tCharset;
        columnNamesRow = tColumnNamesRow;
        firstDataRow = tFirstDataRow;
        preExtractRegex = tPreExtractRegex;
        postExtractRegex = tPostExtractRegex;
        extractRegex = tExtractRegex;
        columnNameForExtract = tColumnNameForExtract;
        sortedColumnSourceName = tSortedColumnSourceName;
        int ndv = tDataVariables.length;
        if (tSortFilesBySourceNames != null && tSortFilesBySourceNames.indexOf(',') >= 0)
            throw new IllegalArgumentException("datasets.xml error: " +
                "sortFilesBySourceNames should be space separated, not comma separated.");

        if (!String2.isSomething(fileDir))
            throw new IllegalArgumentException(errorInMethod + "fileDir wasn't specified.");
        filesAreLocal = !String2.isRemote(fileDir);
        if (fileNameRegex == null || fileNameRegex.length() == 0) 
            fileNameRegex = ".*";
        if (metadataFrom == null) metadataFrom = "";
        if (metadataFrom.length() == 0) metadataFrom = MF_LAST;
        if (!metadataFrom.equals(MF_FIRST) && 
            !metadataFrom.equals(MF_LAST))
            throw new IllegalArgumentException("datasets.xml error: " +
                "metadataFrom=" + metadataFrom + " must be " + 
                MF_FIRST + " or " + MF_LAST + ".");
        if (columnNamesRow < 0 || columnNamesRow > 10000)
            throw new IllegalArgumentException("datasets.xml error: " +
                "columnNamesRow=" + columnNamesRow + " must be between 0 and 10000.");
        if (firstDataRow <= columnNamesRow || firstDataRow > 10000)
            throw new IllegalArgumentException("datasets.xml error: " +
                "firstDataRow=" + firstDataRow + " must be between " + (columnNamesRow+1) + " and 10000.");
        if (preExtractRegex == null) preExtractRegex = "";
        if (postExtractRegex == null) postExtractRegex = "";
        if (extractRegex == null) extractRegex = "";
        if (columnNameForExtract == null) columnNameForExtract = "";
        if (extractRegex.length() == 0 && columnNameForExtract.length() > 0)
            throw new IllegalArgumentException("datasets.xml error: " +
                "columnNameForExtract=" + columnNameForExtract + 
                " but extractRegex=\"\".  It should be something, e.g., \".*\".");
        if (columnNameForExtract.length() == 0 && extractRegex.length() > 0)
            throw new IllegalArgumentException("datasets.xml error: " +
                "extractRegex=" + extractRegex + 
                " but columnNameForExtract=\"\".  It should be something.");

        preExtractPattern  = preExtractRegex.length()  == 0? null : Pattern.compile(preExtractRegex);
        postExtractPattern = postExtractRegex.length() == 0? null : Pattern.compile(postExtractRegex);
        extractPattern     = extractRegex.length()     == 0? null : Pattern.compile(extractRegex);
        if (sortedColumnSourceName == null) sortedColumnSourceName = "";

        //note sourceDataNames, sourceDataTypes
        sourceDataNames = new StringArray();
        safeSourceDataNames = new StringArray();
        sourceDataTypes = new String[ndv];
        boolean isColumnarAscii = className.equals("EDDTableFromColumnarAsciiFiles");
        if (isColumnarAscii) {
            startColumn = new int[ndv];  //all 0's
            stopColumn = new int[ndv];   //all 0's
        }
        //make the No Extract Column (and no fixed value column) versions
        sourceDataNamesNEC = new StringArray();
        StringArray tSourceDataTypesNEC = new StringArray();
        for (int dv = 0; dv < ndv; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            sourceDataNames.add(tSourceName);
            safeSourceDataNames.add(String2.encodeVariableNameSafe(tSourceName));
            sourceDataTypes[dv] = (String)tDataVariables[dv][3];
            if (sourceDataTypes[dv] == null || sourceDataTypes[dv].length() == 0)
                throw new IllegalArgumentException("Unspecified data type for var#" + dv + ".");

            //note timeIndex
            String tDestName = (String)tDataVariables[dv][1];
            if (EDV.TIME_NAME.equals(tDestName) ||
               ((tDestName == null || tDestName.trim().length() == 0) && 
                EDV.TIME_NAME.equals(tSourceName))) 
                timeIndex = dv;

            Attributes atts = (Attributes)tDataVariables[dv][2];
            if (tSourceName.startsWith("=") ||
                tSourceName.equals(columnNameForExtract)) {
            } else {
                sourceDataNamesNEC.add(tSourceName);
                tSourceDataTypesNEC.add(sourceDataTypes[dv]);
                if (isColumnarAscii) {
                    //required
                    startColumn[dv] = atts.getInt("startColumn");
                    stopColumn[dv]  = atts.getInt("stopColumn");
                    Test.ensureBetween(startColumn[dv], 0, 1000000,  
                        "Invalid startColumn attribute for destinationName=" + tDestName);
                    Test.ensureBetween(stopColumn[dv], startColumn[dv] + 1, 1000000, 
                        "Invalid stopColumn attribute for destinationName=" + tDestName);
                }
            }
            if (isColumnarAscii) {
                atts.remove("startColumn");
                atts.remove("stopColumn");
            }
        }
        if (sourceDataNamesNEC.size() == sourceDataNames.size()) {
            sourceDataNamesNEC = sourceDataNames;
            sourceDataTypesNEC = sourceDataTypes;
        } else {
            sourceDataTypesNEC = tSourceDataTypesNEC.toArray();
        }
        tSourceDataTypesNEC = null;


        //EDDTableFromColumnarAscii needs this
        dataVariableSourceNames = sourceDataNames.toArray();

        if (reallyVerbose) String2.log("sourceDataNames=" + sourceDataNames +
            "\nsourceDataTypes=" + String2.toCSSVString(sourceDataTypes));

        if (sortedColumnSourceName.length() > 0) {
            sortedDVI = sourceDataNames.indexOf(sortedColumnSourceName);
            if (sortedDVI < 0)
                throw new IllegalArgumentException("sortedColumnSourceName=" + sortedColumnSourceName + 
                    " isn't among the source data variable names.");
            String tName = (String)tDataVariables[sortedDVI][1];  //destName
            if (tName == null) tName = (String)tDataVariables[sortedDVI][0];  //sourceName
            if (!tName.equals("time") && 
                "String".equals(sourceDataTypes[sortedDVI]))
                throw new IllegalArgumentException("sortedColumnSourceName must be a time or numeric column.");
        }

        extractedColNameIndex = sourceDataNames.indexOf(columnNameForExtract);
        if (columnNameForExtract.length() > 0 && extractedColNameIndex < 0)
            throw new IllegalArgumentException("columnNameForExtract=" + columnNameForExtract + 
                " isn't among the source data variable names.");
        if (extractedColNameIndex >= 0 && extractPattern == null)
            throw new IllegalArgumentException("columnNameForExtract=" + columnNameForExtract + 
                " but extractRegex wasn't specified.");

        //if (reallyVerbose) String2.log(
        //    "columnNameForExtract=" + columnNameForExtract + " extractedColNameIndex=" + extractedColNameIndex +
        //    "sourceDataNamesNEC=" + sourceDataNamesNEC);

        //This class can handle some constraints; 
        //PARTIAL passes all through to getDataForDapQuery,
        //but also does them again in standardizeResultsTable
        sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //all partially handled
        sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //all partially handled
        sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //partially

        //load cached dirTable->dirList
        dirTable = tryToLoadDirFileTable(dirTableFileName); //may be null
        if (dirTable != null) {
            if (verbose) String2.log(
                dirTable.nRows() + " rows in dirTable");
            if (reallyVerbose) String2.log(
                "first 5 rows=\n" + 
                dirTable.dataToCSVString(5));
        }

        //load cached fileTable
        fileTable = tryToLoadDirFileTable(fileTableFileName); //may be null
        if (fileTable != null) {
            if (verbose) String2.log(
                fileTable.nRows() + " rows in fileTable");
            if (reallyVerbose) String2.log(
                "first 5 rows=\n" + 
                fileTable.dataToCSVString(5));
        }

        //ensure fileTable has correct columns and data types
        if (fileTable != null) {
            boolean ok = true;
            if      (fileTable.findColumnNumber("dirIndex")      != FT_DIR_INDEX_COL)      ok = false; 
            else if (fileTable.findColumnNumber("fileName")      != FT_FILE_LIST_COL)      ok = false; 
            else if (fileTable.findColumnNumber("lastMod")       != FT_LAST_MOD_COL)       ok = false;
            else if (fileTable.findColumnNumber("size")          != FT_SIZE_COL)           ok = false;
            else if (fileTable.findColumnNumber("sortedSpacing") != FT_SORTED_SPACING_COL) ok = false;
            else if (!(fileTable.getColumn(FT_DIR_INDEX_COL)      instanceof ShortArray))  ok = false;
            else if (!(fileTable.getColumn(FT_FILE_LIST_COL)      instanceof StringArray)) ok = false;
            else if (!(fileTable.getColumn(FT_LAST_MOD_COL)       instanceof DoubleArray)) ok = false;
            else if (!(fileTable.getColumn(FT_SIZE_COL)           instanceof DoubleArray)) ok = false;
            else if (!(fileTable.getColumn(FT_SORTED_SPACING_COL) instanceof DoubleArray)) ok = false;
            else for (int dv = 0; dv < ndv; dv++) {
                String sdt = sourceDataTypes[dv];
                if (sdt.equals("boolean"))
                    sdt = "byte";
                if (fileTable.findColumnNumber(safeSourceDataNames.get(dv) + "_min_")    != dv0 + dv*3 + 0 ||
                    fileTable.findColumnNumber(safeSourceDataNames.get(dv) + "_max_")    != dv0 + dv*3 + 1 ||
                    fileTable.findColumnNumber(safeSourceDataNames.get(dv) + "_hasNaN_") != dv0 + dv*3 + 2 ||
                    !fileTable.getColumn(dv0 + dv*3 + 0).elementClassString().equals(sdt) ||
                    !fileTable.getColumn(dv0 + dv*3 + 1).elementClassString().equals(sdt) ||
                    !fileTable.getColumn(dv0 + dv*3 + 2).elementClassString().equals("byte")) {
                    ok = false;
                    break;
                }
            }
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
            fileTable.addColumn("dirIndex",      new ShortArray());  //col 0=FT_DIR_INDEX_COL
            fileTable.addColumn("fileName",      new StringArray()); //col 1=FT_FILE_NAME_COL
            fileTable.addColumn("lastMod",       new DoubleArray()); //col 2=FT_LAST_MOD_COL
            fileTable.addColumn("size",          new DoubleArray()); //col 3=FT_SIZE_COL
            fileTable.addColumn("sortedSpacing", new DoubleArray()); //col 4=FT_SORTED_SPACING_COL
            for (int dv = 0; dv < ndv; dv++) {
                String sdt = sourceDataTypes[dv]; //booleans handled correctly below
                fileTable.addColumn(safeSourceDataNames.get(dv) + "_min_", 
                    PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sdt), 8, false));
                fileTable.addColumn(safeSourceDataNames.get(dv) + "_max_", 
                    PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sdt), 8, false));
                fileTable.addColumn(safeSourceDataNames.get(dv) + "_hasNaN_", 
                    PrimitiveArray.factory(byte.class, 8, false));
            }

            badFileMap = newEmptyBadFileMap();
        }

        //get the PrimitiveArrays from fileTable
        StringArray dirList         = (StringArray)dirTable.getColumn(0);
        ShortArray  ftDirIndex      = (ShortArray)fileTable.getColumn( FT_DIR_INDEX_COL); //0
        StringArray ftFileList      = (StringArray)fileTable.getColumn(FT_FILE_LIST_COL); //1
        DoubleArray ftLastMod       = (DoubleArray)fileTable.getColumn(FT_LAST_MOD_COL); //2
        DoubleArray ftSize          = (DoubleArray)fileTable.getColumn(FT_SIZE_COL); //3
        DoubleArray ftSortedSpacing = (DoubleArray)fileTable.getColumn(FT_SORTED_SPACING_COL); //4
        String msg = "";

        //set up WatchDirectory
        if (updateEveryNMillis > 0) {
            try {
                watchDirectory = WatchDirectory.watchDirectoryAll(fileDir, 
                    recursive, pathRegex);
            } catch (Throwable t) {
                updateEveryNMillis = 0; //disable the inotify system for this instance
                String subject = String2.ERROR + " in " + datasetID + " constructor (inotify)";
                msg = MustBe.throwableToString(t);
                if (msg.indexOf("inotify instances") >= 0)
                    msg += EDStatic.inotifyFix;
                EDStatic.email(EDStatic.adminEmail, subject, msg);
                msg = "";
            }
        }

        //doQuickRestart? 
        boolean doQuickRestart = fileTable.nRows() > 0 && 
            (testQuickRestart || (EDStatic.quickRestart && EDStatic.initialLoadDatasets()));
        if (verbose)
            String2.log("doQuickRestart=" + doQuickRestart);

        if (doQuickRestart) {
            msg = "\nQuickRestart";

        } else {
            //!doQuickRestart

            if (!filesAreLocal) {
                //if files are not local, throw away list of bad files,
                //so each will be retried again.
                //One failure shouldn't be considered permanent.
                //Downside: persistently bad files/urls will be rechecked repeatedly -- probably slow!
                badFileMap = newEmptyBadFileMap();
            }

            //get tFileList of available data files
            long elapsedTime = System.currentTimeMillis();
            //was tFileNames with dir+name
            Table tFileTable = getFileInfo(fileDir, fileNameRegex, recursive, pathRegex);
            StringArray tFileDirPA     = (StringArray)(tFileTable.getColumn(FileVisitorDNLS.DIRECTORY));
            StringArray tFileNamePA    = (StringArray)(tFileTable.getColumn(FileVisitorDNLS.NAME));
            LongArray   tFileLastModPA = (LongArray)  (tFileTable.getColumn(FileVisitorDNLS.LASTMODIFIED));
            LongArray   tFileSizePA    = (LongArray)  (tFileTable.getColumn(FileVisitorDNLS.SIZE));
            tFileTable.removeColumn(FileVisitorDNLS.SIZE);
            int ntft = tFileNamePA.size();
            msg = ntft + " files found in " + fileDir + 
                "\nregex=" + fileNameRegex + " recursive=" + recursive + 
                " pathRegex=" + pathRegex + 
                " time=" + (System.currentTimeMillis() - elapsedTime) + "ms";
            if (ntft == 0)
                //Just exit. Don't delete the dirTable and fileTable files!
                //The problem may be that a drive isn't mounted.
                throw new RuntimeException(msg);
            if (verbose) String2.log(msg);
            msg = "";

            //remove "badFiles" if they no longer exist (in tFileNames)
            {
                //make hashset with all tFileNames
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

            //sort fileTable and tFileTable by dirIndex and fileName
            elapsedTime = System.currentTimeMillis();
            fileTable.leftToRightSort(2);  //lexical sort so can walk through below
            tFileTable.leftToRightSort(2); //lexical sort so can walk through below
            if (reallyVerbose) String2.log("sortTime1=" + (System.currentTimeMillis() - elapsedTime) + "ms");

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

            //make arrays to hold expected source add_offset, fillValue, missingValue, scale_factor, units
            expectedAddOffsetNEC    = new double[sourceDataNamesNEC.size()]; 
            expectedFillValueNEC    = new double[sourceDataNamesNEC.size()]; 
            expectedMissingValueNEC = new double[sourceDataNamesNEC.size()];
            expectedScaleFactorNEC  = new double[sourceDataNamesNEC.size()]; 
            expectedUnitsNEC        = new String[sourceDataNamesNEC.size()];
            //initially filled with NaNs
            Arrays.fill(expectedAddOffsetNEC,    Double.NaN);
            Arrays.fill(expectedFillValueNEC,    Double.NaN);
            Arrays.fill(expectedMissingValueNEC, Double.NaN);
            Arrays.fill(expectedScaleFactorNEC,  Double.NaN);

            //Try to fill expected arrays with info for first file in fileTable.
            //All files should have same info (unless var is missing).
            boolean gotExpected = false;
            for (int f = 0; f < ftDirIndex.size(); f++) {
                //find a file that exists
                String dir = dirList.get(ftDirIndex.get(f));
                String name = ftFileList.get(f);
                if (filesAreLocal) {
                    long lastMod = File2.getLastModified(dir + name);
                    if (lastMod == 0 || ftLastMod.get(f) != lastMod) //0=trouble: unavailable or changed
                        continue;
                    long size = File2.length(dir + name);
                    if (size < 0 || size == Long.MAX_VALUE || 
                        (filesAreLocal && ftSize.get(f) != size)) //-1=touble: unavailable or changed
                        continue;
                }

                try {
                    //get the metadata
                    Table table = getSourceDataFromFile(dir, name,
                        sourceDataNamesNEC, sourceDataTypesNEC, 
                        -1, Double.NaN, Double.NaN, 
                        null, null, null, true, false); //getMetadata=true, getData=false

                    //get the expected attributes;     ok if NaN or null
                    for (int dvNec = 0; dvNec < sourceDataNamesNEC.size(); dvNec++) {
                        String tName = sourceDataNamesNEC.get(dvNec);
                        int tableDv = table.findColumnNumber(tName);
                        Attributes dvAtts = tableDv < 0? new Attributes() : table.columnAttributes(tableDv);
                        expectedAddOffsetNEC[dvNec]    = dvAtts.getDouble("add_offset");  
                        expectedFillValueNEC[dvNec]    = dvAtts.getDouble("_FillValue");
                        expectedMissingValueNEC[dvNec] = dvAtts.getDouble("missing_value");
                        expectedScaleFactorNEC[dvNec]  = dvAtts.getDouble("scale_factor");
                        expectedUnitsNEC[dvNec]        = dvAtts.getString("units");
                    }
                } catch (Throwable t) {
                    String2.log("Unexpected error when getting ExpectedXxx attributes from " + dir + name + ":\n" +
                        MustBe.throwableToString(t));
                    continue;  
                }

                //we got what we needed, no need to look at other files
                if (verbose) String2.log("ExpectedXxx attributes were read from " + dir + name);
                gotExpected = true;
                break;
            }
            if (!gotExpected)
                if (verbose) String2.log(
                    "Didn't get expectedXxx attributes because there were no previously valid files,\n" +
                    "  or none of the previously valid files were unchanged!");

            //make arrays to hold addAttributes fillValue, missingValue 
            // (so fake mv can be converted to NaN, so source min and max can be 
            //  determined exclusive of missingValue)
            //may be NaN
            addAttFillValueNEC    = new double[sourceDataNamesNEC.size()]; //filled with 0's!
            addAttMissingValueNEC = new double[sourceDataNamesNEC.size()];
            Arrays.fill(addAttFillValueNEC, Double.NaN); //2014-07-21 now filled with NaN's  
            Arrays.fill(addAttMissingValueNEC, Double.NaN);
            for (int dvNec = 0; dvNec < sourceDataNamesNEC.size(); dvNec++) {
                int dv = sourceDataNames.indexOf(sourceDataNamesNEC.get(dvNec));
                Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
                //if ("depth".equals(sourceDataNamesNEC.get(dvNec)))
                //    String2.log("depth addAtt=" + tAddAtt);
                if (tAddAtt != null) {
                    addAttFillValueNEC[   dvNec] = tAddAtt.getDouble("_FillValue");    //may be NaN
                    addAttMissingValueNEC[dvNec] = tAddAtt.getDouble("missing_value"); //may be NaN
                }
            }

            //update fileTable  by processing tFileNamePA
            int fileListPo = 0;  //next one to look at
            int tFileListPo = 0; //next one to look at
            int nReadFile = 0, nNoLastMod = 0, nNoSize = 0;
            long readFileCumTime = 0;
            long removeCumTime = 0;
            int nUnchanged = 0, nRemoved = 0, nDifferentModTime = 0, nNew = 0;
            elapsedTime = System.currentTimeMillis();
            while (tFileListPo < tFileNamePA.size()) {
                int tDirI      = tFileDirIndexPA.get(tFileListPo);
                String tFileS  = tFileNamePA.get(tFileListPo);
                int dirI       = fileListPo < ftFileList.size()? ftDirIndex.get(fileListPo) : Integer.MAX_VALUE;
                String fileS   = fileListPo < ftFileList.size()? ftFileList.get(fileListPo) : "\uFFFF";
                double lastMod = fileListPo < ftFileList.size()? ftLastMod.get(fileListPo)  : Double.MAX_VALUE;
                double size    = fileListPo < ftFileList.size()? ftSize.get(fileListPo)     : Double.MAX_VALUE;
                boolean logThis = (reallyVerbose && tFileListPo <= 100) || 
                    ((reallyVerbose || verbose) && 
                        ((tFileListPo <= 1000 && tFileListPo % 100 == 0) ||
                         (tFileListPo % 1000 == 0)));
                if (logThis)
                    String2.log("EDDTableFromFiles file #" + tFileListPo + "=" + dirList.get(tDirI) + tFileS);

                //is tLastMod available for tFile?
                long tLastMod = tFileLastModPA.get(tFileListPo);
                if (tLastMod == 0) { //0=trouble
                    nNoLastMod++;
                    String2.log(tFileListPo + " reject because unable to get lastMod time: " + 
                        dirList.get(tDirI) + tFileS);                
                    tFileListPo++;
                    addBadFile(badFileMap, tDirI, tFileS, tLastMod, "Unable to get lastMod time.");
                    continue;
                }

                //is tSize available for tFile?
                long tSize = tFileSizePA.get(tFileListPo);
                if (tSize < 0 || tSize == Long.MAX_VALUE) { //-1=trouble
                    nNoSize++;
                    String2.log(tFileListPo + " reject because unable to get size: " + 
                        dirList.get(tDirI) + tFileS);                
                    tFileListPo++;
                    addBadFile(badFileMap, tDirI, tFileS, tLastMod, "Unable to get size.");
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
                            //remove it from cached info   (Yes, a file may be marked bad (recently) and so still be in cache)
                            nRemoved++;
                            removeCumTime -= System.currentTimeMillis();
                            fileTable.removeRow(fileListPo);
                            removeCumTime += System.currentTimeMillis();
                        }
                        //go on to next tFile
                        if (logThis)
                            String2.log(tFileListPo + " already in badFile list");
                        continue;
                    } else {
                        //file has been changed since being marked as bad; remove from badFileMap
                        badFileMap.remove(tDirI + "/" + tFileS);
                        //and continue processing this file
                    }
                }

                //is tFile already in cache?
                if (tDirI == dirI && tFileS.equals(fileS) && tLastMod == lastMod && 
                    (tSize == size || !filesAreLocal)) { //remote file's size may be approximate, e.g., 11K
                    if (logThis)
                        String2.log(tFileListPo + " already in fileList");
                    nUnchanged++;
                    tFileListPo++;
                    fileListPo++;
                    continue;
                }

                //file in cache no longer exists: remove from fileTable
                if (dirI < tDirI ||
                    (dirI == tDirI && fileS.compareTo(tFileS) < 0)) {
                    if (logThis)
                        String2.log(tFileListPo + " file no longer exists: remove from fileList: " +
                            dirList.get(dirI) + fileS);
                    nRemoved++;
                    removeCumTime -= System.currentTimeMillis();
                    fileTable.removeRow(fileListPo);  //may be slow
                    removeCumTime += System.currentTimeMillis();
                    //tFileListPo isn't incremented, so it will be considered again in next iteration
                    continue;
                }

                //tFile is new, or tFile is in ftFileList but time is different
                if (dirI == tDirI && fileS.equals(tFileS)) {
                    if (logThis)
                        String2.log(tFileListPo + 
                            " already in fileList (but time changed)");
                    nDifferentModTime++;
                } else {
                    //if new, add row to fileTable
                    if (logThis)
                        String2.log(tFileListPo + " insert in fileList");
                    nNew++;
                    fileTable.insertBlankRow(fileListPo);  //may be slow
                }

                //gather file's info
                try {
                    //read all of the data and metadata in the file
                    nReadFile++;
                    long rfcTime = System.currentTimeMillis();
                    Table tTable = getSourceDataFromFile(dirList.get(tDirI), tFileS, 
                        sourceDataNamesNEC, sourceDataTypesNEC, 
                        -1, Double.NaN, Double.NaN, 
                        null, null, null, true, true); //getMetadata, getData
                    readFileCumTime += System.currentTimeMillis() - rfcTime;

                    //set the values on the fileTable row     throws throwable
                    setFileTableRow(fileTable, fileListPo, tDirI, tFileS, tLastMod, tSize, 
                        tTable, logThis? tFileListPo : -1);
                    tFileListPo++;
                    fileListPo++;

                } catch (Throwable t) {
                    String fullName = dirList.get(tDirI) + tFileS; 
                    msg = tFileListPo + " bad file: removing fileTable row for " + 
                        fullName + "\n" +
                        MustBe.throwableToString(t);
                    String2.log(msg); 
                    msg = "";
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
            if (verbose) String2.log("fileTable updated; time=" + 
                (System.currentTimeMillis() - elapsedTime) + "ms");
            Test.ensureTrue(fileTable.nRows() > 0, 
                "No valid data files were found. See log.txt for details."); 

            //sort fileTable by sortFilesBySourceNames
            if (tSortFilesBySourceNames != null &&
                tSortFilesBySourceNames.length() > 0) {
                String sortBy[] = String2.split(tSortFilesBySourceNames, ' ');
                IntArray sortColumns = new IntArray();
                for (int i = 0; i < sortBy.length; i++) {
                    if (sortBy[i].length() == 0)
                        continue;
                    int dv = sourceDataNames.indexOf(sortBy[i]);
                    if (dv < 0) 
                        throw new RuntimeException("Unknown <sortFilesBySourceNames> name#" + 
                            i + "=\"" + sortBy[i] +
                            "\"\nsourceDataNames=" + sourceDataNames.toString());
                    sortColumns.add(dv0 + dv*3 + 0); //the dataVariable's min value
                }
                if (sortColumns.size() > 0) {
                    //String2.log("first 10 rows of fileTable before sortFilesBySourceNames:\n" +
                    //    fileTable.toString("row", 10));
                    fileTableSortColumns = sortColumns.toArray();
                    fileTableSortAscending = new boolean[sortColumns.size()];
                    Arrays.fill(fileTableSortAscending, true);
                    elapsedTime = System.currentTimeMillis();
                    fileTable.sort(fileTableSortColumns, fileTableSortAscending); 
                    if (debugMode) 
                        String2.log("time to sort fileTable by <sortFilesBySourceNames> = " + 
                            (System.currentTimeMillis() - elapsedTime) + "ms");  
                }
            }
            if (reallyVerbose) String2.log("fileTable.nRows=" + fileTable.nRows() + 
                ".  The first few rows are:\n" + fileTable.toString("row", debugMode? 100 : 10));

            msg = "\n  tFileNamePA.size()=" + tFileNamePA.size() + 
                "\n  dirTable.nRows()=" + dirTable.nRows() +
                "\n  fileTable.nRows()=" + fileTable.nRows() + 
                "\n    fileTableInMemory=" + fileTableInMemory + 
                "\n    nUnchanged=" + nUnchanged + 
                "\n    nRemoved=" + nRemoved + " (nNoLastMod=" + nNoLastMod + 
                     ", nNoSize=" + nNoSize + ")" +
                "\n    nReadFile=" + nReadFile + 
                       " (nDifferentModTime=" + nDifferentModTime + " nNew=" + nNew + ")" +
                       " readFileCumTime=" + Calendar2.elapsedTimeString(readFileCumTime) +
                       " avg=" + (readFileCumTime / Math.max(1,nReadFile)) + "ms";
            if (verbose || fileTable.nRows() == 0) 
                String2.log(msg);
            if (fileTable.nRows() == 0)
                throw new RuntimeException("No valid files!");

            if (nReadFile > 0 || nRemoved > 0) 
                filesChanged = 
                    "The list of aggregated files changed:\n" +
                    "  The number of new or changed data files that were read: " + nReadFile + ".\n" +
                    "  The number of files that were removed from the file list: " + nRemoved + ".\n" +
                    "  The total number of good files is now " + tFileNamePA.size() + ".\n";

            //end !doQuickRestart
        }

        //make combined minMaxTable    one col per dv; row0=min, row1=max, row2=hasNaN
        //it holds raw source values -- scale_factor and add_offset haven't been applied
        Table tMinMaxTable = makeMinMaxTable(dirList, fileTable);

        //if !quickRestart, save dirTable, fileTable, badFileMap
        if (!doQuickRestart) 
            saveDirTableFileTableBadFiles(dirTable, fileTable, badFileMap); //throws Throwable
        //then make related changes as quickly/atomically as possible
        minMaxTable = tMinMaxTable; //swap into place quickly

        //set creationTimeMillis to fileTable lastModified 
        //(either very recent or (if quickRestart) from previous full restart)
        creationTimeMillis = File2.getLastModified(datasetDir() + FILE_TABLE_FILENAME);

        //send email with bad file info
        if (!badFileMap.isEmpty()) {
            StringBuilder emailSB = new StringBuilder();
            emailSB.append(badFileMapToString(badFileMap, dirList));
            emailSB.append(msg + "\n\n");
            EDStatic.email(EDStatic.emailEverythingToCsv, errorInMethod, emailSB.toString());
        }

        //try to open metadataFrom FIRST|LAST file (based on lastModifiedTime) to get source metadata
        int nMinMaxIndex[] = ftLastMod.getNMinMaxIndex();
        int tFileI = metadataFrom.equals(MF_FIRST)? nMinMaxIndex[1] : nMinMaxIndex[2];
        String mdFromDir  = dirList.get(ftDirIndex.get(tFileI));
        String mdFromName = ftFileList.get(tFileI);
        if (verbose) String2.log("getting metadata from " + mdFromDir + mdFromName + 
            "\n  ftLastMod" + 
            " first=" + Calendar2.millisToIsoZuluString(Math.round(ftLastMod.get(nMinMaxIndex[1]))) + 
             " last=" + Calendar2.millisToIsoZuluString(Math.round(ftLastMod.get(nMinMaxIndex[2]))));
        Table tTable = getSourceDataFromFile(mdFromDir, mdFromName,
            sourceDataNamesNEC, sourceDataTypesNEC, -1, Double.NaN, Double.NaN, 
            null, null, null, true, false);
        //remove e.g., global geospatial_lon_min  and column actual_range, data_min, data_max
        tTable.unsetActualRangeAndBoundingBox();
        sourceGlobalAttributes = tTable.globalAttributes();

        //make combinedGlobalAttributes
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");

        //make the dataVariables[]
        dataVariables = new EDV[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            String tSourceName = sourceDataNames.get(dv);
            boolean isFixedValue = tSourceName.startsWith("=");
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            int tableDv = tTable.findColumnNumber(tSourceName); 
            if (reallyVerbose && !isFixedValue && dv != extractedColNameIndex && tableDv < 0)
                String2.log("NOTE: " + tSourceName + " not found in metadataFrom=" + metadataFrom);
            Attributes tSourceAtt = tableDv < 0? new Attributes() : tTable.columnAttributes(tableDv); 
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            //PrimitiveArray taa = tAddAtt.get("_FillValue");
            //String2.log(">>taa " + tSourceName + " _FillValue=" + taa);
            //dMin and dMax are raw source values -- scale_factor and add_offset haven't been applied
            String tSourceType = sourceDataTypes[dv];
            String sMin = tSourceType.equals("String")? "" : 
                          isFixedValue? tSourceName.substring(1) :
                          minMaxTable.getStringData(dv, 0);
            String sMax = tSourceType.equals("String")? "" : 
                          isFixedValue? sMin :
                          minMaxTable.getStringData(dv, 1);
            //String2.log(">>sMin=" + sMin + " sMax=" + sMax + " paMin=" + minMaxTable.getColumn(dv).minValue() +
            //    " paMax=" + minMaxTable.getColumn(dv).maxValue());
            if (sMin.length() > 0 &&
                minMaxTable.getColumn(dv).minValue().equals(sMin) &&
                minMaxTable.getColumn(dv).maxValue().equals(sMax)) {
                //these are placeholder min and max, so don't use for actual_range
                sMin = "";
                sMax = "";
            }
            double dMin = String2.parseDouble(sMin);
            double dMax = String2.parseDouble(sMax);

            //if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType=" + tSourceType);

            if (EDV.LON_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLon(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, dMin, dMax); 
                lonIndex = dv;
            } else if (EDV.LAT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLat(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, dMin, dMax); 
                latIndex = dv;
            } else if (EDV.ALT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVAlt(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType,  dMin, dMax);
                altIndex = dv;
            } else if (EDV.DEPTH_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVDepth(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType,  dMin, dMax);
                depthIndex = dv;

            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                //EEEK!!! this is weak. It will work for ISO strings, or numeric only.
                //If other strings, they won't be sorted right.
                //Need to deal with them above; store minMax as epochSeconds?
                //String2.log("\nTIME sourceAtt:\n" + tSourceAtt);
                //String2.log("\nTIME addAtt:\n" + tAddAtt);
                PrimitiveArray actualRange = PrimitiveArray.factory(
                    PrimitiveArray.elementStringToClass(sourceDataTypes[dv]), 2, false);
                actualRange.addString(minMaxTable.getStringData(dv, 0));
                actualRange.addString(minMaxTable.getStringData(dv, 1));
                if (tAddAtt == null)
                    tAddAtt = new Attributes();
                tAddAtt.set("actual_range", actualRange);
                //String2.log(">> actual_range=" + actualRange);

                if (EDV.TIME_NAME.equals(tDestName)) {
                    //it's the time variable
                    dataVariables[dv] = new EDVTime(tSourceName,
                        tSourceAtt, tAddAtt, 
                        tSourceType); //this constructor gets source / sets destination actual_range
                    timeIndex = dv;

                } else {              
                    //it's a timeStamp variable 
                    dataVariables[dv] = new EDVTimeStamp(tSourceName, tDestName, 
                        tSourceAtt, tAddAtt,
                        tSourceType); //this constructor gets source / sets destination actual_range
                }
            } else {
                dataVariables[dv] = new EDV(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt, tSourceType, dMin, dMax); 
                dataVariables[dv].setActualRangeFromDestinationMinMax();
            }

        //String2.pressEnterToContinue("!!!sourceName=" + dataVariables[dv].sourceName() + 
        //    " type=" + dataVariables[dv].sourceDataType() + " min=" + dataVariables[dv].destinationMin());
        }

        //Try to gather information to serve this dataset via ERDDAP's SOS server.
        //This has an advantage over the generic gathering of SOS data:
        //  if it works, it can determine the min/max lon/lat/time of each station.
        //This can only deal with: each file has info for only one e.g., station,
        //   but there may be several files for same station.
        //If this fails, makeAccessibleViaSOS() may still work.
        if (preliminaryAccessibleViaSOS().length() == 0) { 

            EDV lonVar      = dataVariables[lonIndex];
            EDV latVar      = dataVariables[latIndex];
            EDV timeVar     = dataVariables[timeIndex];
            EDV offeringVar = dataVariables[sosOfferingIndex];

            //Get convenient access to fileTable data min,max,hasNaN
            //  (but converted to destination values).  
            //clone() to ensure original fileTable values are changed.
            PrimitiveArray fFileName    = fileTable.getColumn("fileName");
            PrimitiveArray fLonMin      = lonVar.toDestination(     (PrimitiveArray)fileTable.getColumn(dv0 + lonIndex*3         + 0).clone());
            PrimitiveArray fLonMax      = lonVar.toDestination(     (PrimitiveArray)fileTable.getColumn(dv0 + lonIndex*3         + 1).clone());
            PrimitiveArray fLonNan      =                                           fileTable.getColumn(dv0 + lonIndex*3         + 2);
            PrimitiveArray fLatMin      = latVar.toDestination(     (PrimitiveArray)fileTable.getColumn(dv0 + latIndex*3         + 0).clone());
            PrimitiveArray fLatMax      = latVar.toDestination(     (PrimitiveArray)fileTable.getColumn(dv0 + latIndex*3         + 1).clone());
            PrimitiveArray fLatNan      =                                           fileTable.getColumn(dv0 + latIndex*3         + 2);
            PrimitiveArray fTimeMin     = timeVar.toDestination(    (PrimitiveArray)fileTable.getColumn(dv0 + timeIndex*3        + 0).clone());
            PrimitiveArray fTimeMax     = timeVar.toDestination(    (PrimitiveArray)fileTable.getColumn(dv0 + timeIndex*3        + 1).clone());
            PrimitiveArray fTimeNan     =                                           fileTable.getColumn(dv0 + timeIndex*3        + 2);
            PrimitiveArray fOfferingMin = offeringVar.toDestination((PrimitiveArray)fileTable.getColumn(dv0 + sosOfferingIndex*3 + 0).clone()); 
            PrimitiveArray fOfferingMax = offeringVar.toDestination((PrimitiveArray)fileTable.getColumn(dv0 + sosOfferingIndex*3 + 1).clone()); 
            PrimitiveArray fOfferingNan =                                           fileTable.getColumn(dv0 + sosOfferingIndex*3 + 2); 

            //make the sos PAs to hold destination values
            sosMinLon    = PrimitiveArray.factory(lonVar.destinationDataTypeClass(),  8, false);
            sosMaxLon    = PrimitiveArray.factory(lonVar.destinationDataTypeClass(),  8, false);
            sosMinLat    = PrimitiveArray.factory(latVar.destinationDataTypeClass(),  8, false);
            sosMaxLat    = PrimitiveArray.factory(latVar.destinationDataTypeClass(),  8, false);
            sosMinTime   = PrimitiveArray.factory(timeVar.destinationDataTypeClass(), 8, false);
            sosMaxTime   = PrimitiveArray.factory(timeVar.destinationDataTypeClass(), 8, false);
            sosOfferings = new StringArray();
         
            //Collect info until a file doesn't meet requirements or all files do meet requirements.
            //Do all files contain just one value of sosOfferingIndex (e.g., 1 station)?
            //If so, easy to find min/max lon/lat/time for each station.
            int tnFiles = fLonMin.size();
            HashMap offeringIndexHM = new HashMap(); //key=offering value=new Integer(SosXxx index)
            for (int f = 0; f < tnFiles; f++) {
                String offMin  = fOfferingMin.getString(f);
                String offMax  = fOfferingMax.getString(f);
                boolean offNaN = fOfferingNan.getInt(f) == 1;  //hasNaN? 1=true 0=false

                //if offerings in this file are all "" or null, ignore it
                if (offNaN && 
                    (offMin == null || offMin.length() == 0) && 
                    (offMax == null || offMax.length() == 0)) {

                //if just one offering in file (no mv), add data to sos arrays
                } else if (!offNaN && offMin.equals(offMax)) {
                    //find sos PA index
                    Integer soI = (Integer)offeringIndexHM.get(offMin);
                    if (soI == null) {
                        //it's a new offering.  add it.
                        soI = new Integer(sosOfferings.size());
                        offeringIndexHM.put(offMin, soI);
                        sosMinLon.addFromPA(fLonMin, f);
                        sosMaxLon.addFromPA(fLonMax, f);
                        sosMinLat.addFromPA(fLatMin, f);
                        sosMaxLat.addFromPA(fLatMax, f);
                        sosMinTime.addFromPA(fTimeMin, f);
                        sosMaxTime.addFromPA(fTimeMax, f);
                        sosOfferings.addString(offMin);

                    } else {
                        //a previous file had the same offering, so update its info in sos... PA
                        //store the min min and the max max.
                        int soi = soI.intValue();
                        sosMinLon.setDouble( soi, Math2.finiteMin(sosMinLon.getDouble( soi), fLonMin.getDouble(f)));
                        sosMaxLon.setDouble( soi, Math2.finiteMax(sosMaxLon.getDouble( soi), fLonMax.getDouble(f)));
                        sosMinLat.setDouble( soi, Math2.finiteMin(sosMinLat.getDouble( soi), fLatMin.getDouble(f)));
                        sosMaxLat.setDouble( soi, Math2.finiteMax(sosMaxLat.getDouble( soi), fLatMax.getDouble(f)));
                        sosMinTime.setDouble(soi, Math2.finiteMin(sosMinTime.getDouble(soi), fTimeMin.getDouble(f)));
                        sosMaxTime.setDouble(soi, Math2.finiteMax(sosMaxTime.getDouble(soi), fTimeMax.getDouble(f)));
                        //sosOfferings is already correct                                            
                    } 

                } else {
                    //else trouble: more than one offering per file or contaminated with offering=mv.  
                    //Abandon this approach.
                    //accessibleViaSOS = "";  //???set this?
                    if (verbose) String2.log(
                        "EDDTableFromFiles can't gather sosOffering min/max for datasetID=" + datasetID + 
                        "\nfrom fileTable because fileName=" + fFileName.getString(f) + 
                        " has >1 offering: min=" + offMin + " max=" + offMax + " nan=" + offNaN);
                    sosOfferingType = null;
                    sosOfferingIndex = -1;
                    sosMinLon    = null;
                    sosMaxLon    = null;
                    sosMinLat    = null;
                    sosMaxLat    = null;
                    sosMinTime   = null;
                    sosMaxTime   = null;
                    sosOfferings = null;
                    break;
                }
            }
        } //end gathering sosOfferings info

        //accessibleViaFiles
        if (EDStatic.filesActive && tAccessibleViaFiles) {
            accessibleViaFilesDir = fileDir;
            accessibleViaFilesRegex = fileNameRegex;
            accessibleViaFilesRecursive = recursive;
        }

        //ensure the setup is valid
        ensureValid();

        //EDV edv = findDataVariableByDestinationName("longitude");
        //String2.pressEnterToContinue("!!!end of EDDTableFromFiles constructor: sourceName=" + edv.sourceName() + 
        //    " type=" + edv.sourceDataType() + " min=" + edv.destinationMin());

        //dirTable and fileTable InMemory?
        if (!fileTableInMemory) {
            dirTable = null;
            fileTable = null;
        }

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromFiles " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 
    }

    /**
     * This extracts data from the fileName.
     * 
     * @param fileName
     * @return the extracted String 
     */
    protected String extractFromFileName(String fileName) {

        String eName = fileName;
        Matcher m;
        if (preExtractPattern != null) {
            m = preExtractPattern.matcher(eName);
            if (m.find()) {
                eName = eName.substring(0, m.start()) + eName.substring(m.end());
                if (debugMode) String2.log(">  extractFromFileName found preExtract, result=" + eName);
            }
        }
        if (postExtractPattern != null) {
            m = postExtractPattern.matcher(eName);
            if (m.find()) {
                eName = eName.substring(0, m.start()) + eName.substring(m.end());
                if (debugMode) String2.log(">  extractFromFileName postExtract, result=" + eName);
            }
        }
        m = extractPattern.matcher(eName);
        if (m.find()) {
            eName = eName.substring(m.start(), m.end());
            if (debugMode) String2.log(">  extractFromFileName found extract, result=" + eName);
        }
        if (debugMode) String2.log(">  extractFromFileName converted " + fileName + " -> " + eName);
        return eName;
    }

    /** 
     * This is used by the constructor and lowUpdate to ensure that a new 
     * file's attributes are compatible with the expected attributes.
     *
     * @param dvName dataVariable sourceName
     * @param dvNEC number
     * @param dvSourceAtts for the variable in the new file
     * @throws RuntimeException if not compatible
     */
    protected void testIfNewFileAttsAreCompatible(String dvName, int dvNEC, 
        Attributes dvSourceAtts) {

        double tAddOffset    = dvSourceAtts.getDouble("add_offset");
        double tFillValue    = dvSourceAtts.getDouble("_FillValue");
        double tMissingValue = dvSourceAtts.getDouble("missing_value");
        double tScaleFactor  = dvSourceAtts.getDouble("scale_factor");
        String tUnits        = dvSourceAtts.getString("units");
        if (Double.isNaN(expectedAddOffsetNEC[   dvNEC])) 
                         expectedAddOffsetNEC[   dvNEC] = tAddOffset;
        if (Double.isNaN(expectedFillValueNEC[   dvNEC])) 
                         expectedFillValueNEC[   dvNEC] = tFillValue;
        if (Double.isNaN(expectedMissingValueNEC[dvNEC])) 
                         expectedMissingValueNEC[dvNEC] = tMissingValue;
        if (Double.isNaN(expectedScaleFactorNEC[ dvNEC])) 
                         expectedScaleFactorNEC[ dvNEC] = tScaleFactor;
        if (expectedUnitsNEC[dvNEC] == null) expectedUnitsNEC[dvNEC] = tUnits;
        String oNEe = " sourceAttribute value observed!=expected for sourceName=" + dvName + ".";
        //if null, skip test,   since a given file may not have some variable
        //unfortunate: it is also possible that this file has the variable, but not this attribute
        //   but in that case, reasonable to pretend it should have the expected attribute value.
        Test.ensureEqual(tAddOffset,        expectedAddOffsetNEC[   dvNEC], "add_offset" + oNEe);
        if (!Double.isNaN(tFillValue))
            Test.ensureEqual(tFillValue,    expectedFillValueNEC[   dvNEC], "_FillValue" + oNEe);
        if (!Double.isNaN(tMissingValue))
            Test.ensureEqual(tMissingValue, expectedMissingValueNEC[dvNEC], "missing_value" + oNEe);
        Test.ensureEqual(tScaleFactor,      expectedScaleFactorNEC[ dvNEC], "scale_factor" + oNEe);
        if (!EDUnits.udunitsAreEquivalent(tUnits, expectedUnitsNEC[dvNEC]))
                         Test.ensureEqual(tUnits, expectedUnitsNEC[dvNEC], "units" + oNEe);
    }


    /**
     * This sets the values on a local fileTable row.
     * 
     * @param tTable table (with source atts and data) from a data file
     * @param logAsRowNumber the fileTable row number to be printed in log messages,
     *    or -1 for no log messages
     * @throws throwable if trouble
     */
    protected void setFileTableRow(Table fileTable, int fileListPo,
        int tDirI, String tFileS, double tLastMod, double tSize, Table tTable, int logAsRowNumber) {

        ShortArray  ftDirIndex      =  (ShortArray)fileTable.getColumn(FT_DIR_INDEX_COL);      //0
        StringArray ftFileList      = (StringArray)fileTable.getColumn(FT_FILE_LIST_COL);      //1
        DoubleArray ftLastMod       = (DoubleArray)fileTable.getColumn(FT_LAST_MOD_COL);       //2
        DoubleArray ftSize          = (DoubleArray)fileTable.getColumn(FT_SIZE_COL);           //3
        DoubleArray ftSortedSpacing = (DoubleArray)fileTable.getColumn(FT_SORTED_SPACING_COL); //4

        ftDirIndex.setInt(fileListPo, tDirI);
        ftFileList.set(fileListPo, tFileS);
        ftLastMod.set(fileListPo, tLastMod);
        ftSize.set(fileListPo, tSize);
        ftSortedSpacing.set(fileListPo, -1); //default, usually set below

        //get min,max for dataVariables
        int tTableNCols = tTable.nColumns();
        int ndv = sourceDataTypes.length;
        for (int dv = 0; dv < ndv; dv++) {
            fileTable.setStringData(dv0 + dv*3 + 0, fileListPo, ""); //numeric will be NaN
            fileTable.setStringData(dv0 + dv*3 + 1, fileListPo, "");
            fileTable.setStringData(dv0 + dv*3 + 2, fileListPo, ""); //hasNaN unspecified

            //columnNameForExtract  (isn't in sourceFile)
            if (dv == extractedColNameIndex) {
                String eName = extractFromFileName(tFileS);
                fileTable.setStringData(dv0 + dv*3 + 0, fileListPo, eName);
                fileTable.setStringData(dv0 + dv*3 + 1, fileListPo, eName);
                fileTable.setIntData(   dv0 + dv*3 + 2, fileListPo, eName.length() == 0? 1 : 0);  //hasNaN
                continue;
            }

            //skip this variable if not in this source file 
            //(this skips if fixed value col, too)
            String dvName = sourceDataNames.get(dv);
            int dvNEC = sourceDataNamesNEC.indexOf(dvName);
            int c = tTable.findColumnNumber(dvName);
            if (dvNEC < 0 || c < 0) {
                //String2.log("  " + dvName + " not in source file");
                continue;
            }

            //attributes are as expected???
            Attributes dvSourceAtts = tTable.columnAttributes(c);
            testIfNewFileAttsAreCompatible( //throws exception if trouble
                dvName, dvNEC, dvSourceAtts);

            //convert missing_value and _FillValue to NaN
            //doubles? type not important here, tTable is temporary
            //others attributes (e.g., scale, add_offset, units) not needed for calculation of min max below
            //(if data is packed, missing_value and _FillValue are packed, too)
            if (!Double.isNaN(addAttFillValueNEC[   dvNEC])) dvSourceAtts.set("_FillValue",    addAttFillValueNEC[   dvNEC]);
            if (!Double.isNaN(addAttMissingValueNEC[dvNEC])) dvSourceAtts.set("missing_value", addAttMissingValueNEC[dvNEC]);
            tTable.convertToStandardMissingValues(c);

            //process source min and max for this column's data
            PrimitiveArray pa = tTable.getColumn(c);
            if (pa instanceof StringArray) {
                //get [0]=n,[1]=min,[2]=max (of non-null and non-"") Strings
                String nMinMax[] = ((StringArray)pa).getNMinMax(); 
                int tn = String2.parseInt(nMinMax[0]);
                if (tn > 0) {
                    fileTable.setStringData(dv0 + dv*3 + 0, fileListPo, nMinMax[1]);  
                    fileTable.setStringData(dv0 + dv*3 + 1, fileListPo, nMinMax[2]);
                }
                fileTable.setIntData(dv0 + dv*3 + 2, fileListPo, tn < pa.size()? 1 : 0); //hasNaN
            } else {
                double stats[] = pa.calculateStats();
                int tn = Math2.roundToInt(stats[PrimitiveArray.STATS_N]);
                //if (dvName.equals("bucket_sal")) String2.log("  " + dvName + "  stats=" + String2.toCSSVString(stats));
                fileTable.setIntData(dv0 + dv*3 + 2, fileListPo, tn < pa.size()? 1 : 0); //hasNaN
                if (tn > 0) {
                    fileTable.setDoubleData(dv0 + dv*3 + 0, fileListPo, stats[PrimitiveArray.STATS_MIN]);
                    fileTable.setDoubleData(dv0 + dv*3 + 1, fileListPo, stats[PrimitiveArray.STATS_MAX]);
                    if (dv == sortedDVI) {
                        String ts = pa.isAscending();
                        double tSortedSpacing;
                        if (tn > 1 && ts.length() == 0) {
                            ts = pa.isEvenlySpaced();
                            if (ts.length() == 0) {
                                tSortedSpacing = 
                                    (stats[PrimitiveArray.STATS_MAX] -
                                     stats[PrimitiveArray.STATS_MIN]) / (tn - 1);
                                if (logAsRowNumber >= 0)
                                    String2.log(logAsRowNumber + " " + sortedColumnSourceName + 
                                        " is evenly spaced=" + tSortedSpacing);
                            } else { 
                                if (logAsRowNumber >= 0)
                                    String2.log(logAsRowNumber + " " + sortedColumnSourceName + 
                                        " isAscending but " + ts);
                                tSortedSpacing = 0;
                            }
                        } else {
                            if (logAsRowNumber >= 0) 
                                String2.log(logAsRowNumber + " " + 
                                    sortedColumnSourceName + " " + ts);
                            tSortedSpacing = -1;
                        }
                        ftSortedSpacing.set(fileListPo, tSortedSpacing);
                    }
                }
            }
            //if (logThis)
            //    String2.log(dvName + 
            //        " min="    + fileTable.getStringData(dv0 + dv*3 + 0, fileListPo) + 
            //        " max="    + fileTable.getStringData(dv0 + dv*3 + 1, fileListPo)); 
            //        " hasNaN=" + fileTable.getIntData(   dv0 + dv*3 + 2, fileListPo)); 
        }
    }

    /** 
     * This is used to make a new minMaxTable just before saving a changed fileTable.
     *
     * @param dirList the up-to-date dirList
     * @param fileTable the new fileTable
     * @return the new minMaxTable
     */     
    protected Table makeMinMaxTable(StringArray dirList, Table fileTable) {

        ShortArray  ftDirIndex =  (ShortArray)fileTable.getColumn(FT_DIR_INDEX_COL); //0
        StringArray ftFileList = (StringArray)fileTable.getColumn(FT_FILE_LIST_COL); //1

        Table minMaxTable = new Table();
        int ndv = sourceDataTypes.length;
        for (int dv = 0; dv < ndv; dv++) {
            //String2.log("dv=" + dv + " " + sourceDataTypes[dv]);
            PrimitiveArray minMaxPa = 
                PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sourceDataTypes[dv]), 3, false);
            minMaxPa.addString(""); //min    initially "" or NaN
            minMaxPa.addString(""); //max    initially "" or NaN
            minMaxPa.addString(""); //hasNaN initially NaN
            minMaxTable.addColumn(safeSourceDataNames.get(dv), minMaxPa);

            //calculate min of the min values
            PrimitiveArray pa = fileTable.getColumn(dv0 + dv*3 + 0);
            //String2.log(sourceDataNames.get(dv) + " minCol=" + pa.toString());
            String mm = null;
            if (sourceDataTypes[dv].equals("String")) {
                //for non="" strings
                String nMinMax[] = ((StringArray)pa).getNMinMax(); //[0]=n, [1]=min, [2]=max
                int tn = String2.parseInt(nMinMax[0]);
                if (tn > 0) 
                    minMaxPa.setString(0, mm = nMinMax[1]);
                //else leave min as "" (mv)
            } else {
                double stats[] = pa.calculateStats();
                if (stats[PrimitiveArray.STATS_N] > 0) {
                    double tMin = stats[PrimitiveArray.STATS_MIN];
                    minMaxPa.setDouble(0, tMin);
                    mm = "" + tMin;
                }
            }
            if (reallyVerbose && mm != null) {
                int row = pa.indexOf(mm);
                if (row >= 0) 
                    String2.log(sourceDataNames.get(dv) + " minMin=" + mm + 
                        " file=" + dirList.get(ftDirIndex.get(row)) + ftFileList.get(row));
            }

            //calculate max of the max values
            pa = fileTable.getColumn(dv0 + dv*3 + 1);
            //String2.log(sourceDataNames.get(dv) + " maxCol=" + pa.toString());
            mm = null;
            if (sourceDataTypes[dv].equals("String")) {
                String nMinMax[] = ((StringArray)pa).getNMinMax();
                int tn = String2.parseInt(nMinMax[0]);
                if (tn > 0) 
                    minMaxPa.setString(1, mm = nMinMax[2]);
                //else leave max as "" (mv)
            } else {
                double stats[] = pa.calculateStats();
                if (stats[PrimitiveArray.STATS_N] > 0) {
                    double tMax = stats[PrimitiveArray.STATS_MAX];
                    minMaxPa.setDouble(1, tMax);
                    mm = "" + tMax;
                }
            }
            if (reallyVerbose && mm != null) {
                int row = pa.indexOf(mm);
                if (row >= 0) 
                    String2.log(sourceDataNames.get(dv) + " maxMax=" + mm + 
                        " file=" + dirList.get(ftDirIndex.get(row)) + ftFileList.get(row));
            }

            //calculate hasNaN
            pa = fileTable.getColumn(dv0 + dv*3 + 2);
            minMaxPa.setInt(2, pa.indexOf("1") >= 0? 1 : 0); //does any file hasNaN?
        }
        if (verbose) String2.log("minMaxTable=\n" + minMaxTable.dataToCSVString()); //it's always small
        return minMaxTable;
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
     *     this calls EDD.requestReloadASAP(tDatasetID) and returns without doing anything.
     */
    public boolean lowUpdate(String msg, long startUpdateMillis) throws Throwable {

        //Most of this lowUpdate code is identical in EDDGridFromFiles and EDDTableFromFiles
        if (watchDirectory == null)
            return false;  //no changes

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
            if (fileName.length() > 0 && fileName.matches(fileNameRegex) &&
                (!recursive || dirName.matches(pathRegex)))
                keep.set(evi);
        }
        contexts.justKeep(keep);        
        nEvents = contexts.size();
        if (nEvents == 0) {
            if (verbose) String2.log(msg + 
                "found 0 events related to files matching fileNameRegex+recursive+pathRegex.");
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
        ShortArray  ftDirIndex      = (ShortArray) tFileTable.getColumn(FT_DIR_INDEX_COL);      //0
        StringArray ftFileList      = (StringArray)tFileTable.getColumn(FT_FILE_LIST_COL);      //1
        DoubleArray ftLastMod       = (DoubleArray)tFileTable.getColumn(FT_LAST_MOD_COL);       //2
        DoubleArray ftSize          = (DoubleArray)tFileTable.getColumn(FT_SIZE_COL);           //3
        DoubleArray ftSortedSpacing = (DoubleArray)tFileTable.getColumn(FT_SORTED_SPACING_COL); //4

        //for each changed file
        int nChanges = 0; //BadFiles or FileTable
        for (int evi = 0; evi < nEvents; evi++) {
            String fullName = contexts.get(evi);
            String dirName = File2.getDirectory(fullName);
            String fileName = File2.getNameAndExtension(fullName);  //matched to fileNameRegex above

            //dirIndex   (dirName may not be in dirList!)
            int dirIndex = dirList.indexOf(dirName); //linear search, but should be short list

            //if it is an existing file, see if it is valid
            if (File2.isFile(fullName)) {
                //test that dataVariable units/etc are identical
                Table tTable = null;
                String reasonBad = null;                
                try {
                    //check the NEC columns for compatible metadata
                    //(No Extract Column and no fixed value columns)
                    tTable = getSourceDataFromFile(dirName, fileName, 
                        sourceDataNamesNEC, sourceDataTypesNEC, 
                        -1, Double.NaN, Double.NaN, 
                        null, null, null, true, true); //getMetadata, getData
                    for (int dvNEC = 0; dvNEC < sourceDataNamesNEC.size(); dvNEC++) {

                        //skip this variable if not in this source file
                        String dvName = sourceDataNamesNEC.get(dvNEC);
                        int c = tTable.findColumnNumber(dvName);
                        if (c < 0) {
                            //String2.log("  " + dvName + " not in source file");
                            continue;
                        }

                        //attributes are as expected???
                        testIfNewFileAttsAreCompatible( //throws exception if trouble
                            dvName, dvNEC, tTable.columnAttributes(c));
                    }

                } catch (Exception e) {
                    reasonBad = e.getMessage(); 
                }
                
                if (reasonBad == null) { 
                    //File exists and is good/compatible.
                    nChanges++;

                    //ensure dirIndex is valid
                    int fileListPo = -1;
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

                        //If file name already in tFileTable, find it.
                        //Don't take shortcut, e.g., binary search with tMin.
                        //It is possible file had wrong name/wrong value before.
                        fileListPo = findInFileTable(dirIndex, fileName, 
                            tFileTable, ftDirIndex, ftFileList);
                    }

                    //set info in tFileTable for this valid file
                    //Searching for appropriate row is very hard here 
                    //(and keeping it up-to-date leads to lots of row movement)
                    //so just reuse same row as before or add row at and of fileTable
                    //and (if any changes) sort fileTable below.
                    if (verbose)
                        String2.log(msg + 
                            (fileListPo >= 0? "updated a file in" : "added a file to") + 
                            " fileTable:\n  " + 
                            fullName);
                    if (fileListPo < 0) {
                        //insert row at end of fileTable  (sort below)
                        fileListPo = ftFileList.size(); 
                        tFileTable.insertBlankRow(fileListPo);
                    } //else use same row it was on before (can be inappropriate, but will sort below)
                    setFileTableRow(tFileTable, fileListPo, dirIndex, fileName,
                        File2.getLastModified(fullName), File2.length(fullName), tTable, 
                        debugMode? evi : -1);

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
                                String2.log(msg + 
                                    "found a bad file (but it wasn't in fileTable):\n  " + 
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
                    //Don't take shortcut, e.g., by searching with tMin.
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
            if (fileTableSortColumns != null) {
                //sort the tFileTable
                long sortTime = System.currentTimeMillis();
                tFileTable.sort(fileTableSortColumns, fileTableSortAscending); 
                if (reallyVerbose) 
                    String2.log(msg + "sorted tFileTable, time=" + 
                        (System.currentTimeMillis() - sortTime) + "ms");  
            }
            //make the new minMaxTable
            Table tMinMaxTable = makeMinMaxTable(dirList, tFileTable);

            //then, change secondary parts of instance variables
            //update all variable destinationMinMax
            int ndv = sourceDataTypes.length;
            for (int dv = 0; dv < ndv; dv++) {
                PrimitiveArray minMaxPa = tMinMaxTable.getColumn(dv);
                EDV edv = dataVariables[dv];
                if (edv.isFixedValue()) // min/max won't change
                    continue; 
                if (minMaxPa instanceof StringArray) {
                    if (edv instanceof EDVTimeStamp) {
                        EDVTimeStamp edvts = (EDVTimeStamp)edv;
                        edvts.setDestinationMinMax(
                            edvts.sourceTimeToEpochSeconds(minMaxPa.getString(0)),
                            edvts.sourceTimeToEpochSeconds(minMaxPa.getString(1)));
                        edvts.setActualRangeFromDestinationMinMax();
                    }
                } else { //minMaxPa is numeric 
                    edv.setDestinationMinMaxFromSource(
                        minMaxPa.getDouble(0), minMaxPa.getDouble(1));
                    edv.setActualRangeFromDestinationMinMax();
                }
                if (dv == lonIndex) {
                    combinedGlobalAttributes().set("geospatial_lon_min", edv.destinationMin());
                    combinedGlobalAttributes().set("geospatial_lon_max", edv.destinationMax());
                } else if (dv == latIndex) {
                    combinedGlobalAttributes().set("geospatial_lat_min", edv.destinationMin());
                    combinedGlobalAttributes().set("geospatial_lat_max", edv.destinationMax());
                } else if (dv == altIndex || dv == depthIndex) {
                    //this works with alt and depth because positive=up|down deals with meaning
                    combinedGlobalAttributes().set("geospatial_vertical_min", edv.destinationMin());
                    combinedGlobalAttributes().set("geospatial_vertical_max", edv.destinationMax());
                } else if (dv == timeIndex) {
                    combinedGlobalAttributes().set("time_coverage_start", edv.destinationMinString());
                    combinedGlobalAttributes().set("time_coverage_end",   edv.destinationMaxString());
                }
            }

            //finally: make the important instance changes that use the changes above 
            //as quickly/atomically as possible
            saveDirTableFileTableBadFiles(tDirTable, tFileTable, badFileMap); //throws Throwable
            minMaxTable = tMinMaxTable;
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
     * Try to load the dirTable or fileTable.
     * fileTable PrimitiveArrays: 0=ftDirIndex 1=ftFileList 2=ftLastMod 3=ftSize 4=ftSortedSpacing, 
     * then sourceMin, sourceMax, hasNaN columns for each dv. 
     *
     * @param fileName dirTableFileName or fileTableFileName
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
     * This returns a fileTable (formatted like 
     * FileVisitorDNLS.oneStep(tDirectoriesToo=false, last_mod is LongArray,
     * and size is LongArray of epochMillis)
     * with valid files (or null if unavailable or any trouble).
     * This is a copy of any internal data, so client can modify the contents.
     */
    public Table accessibleViaFilesFileTable() {
        try {
            //get a copy of the source file information
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

            //make the results Table
            Table dnlsTable = FileVisitorDNLS.makeEmptyTable();
            dnlsTable.setColumn(0, tFileTable.getColumn(FT_DIR_INDEX_COL));
            dnlsTable.setColumn(1, tFileTable.getColumn(FT_FILE_LIST_COL));
            dnlsTable.setColumn(2, new LongArray(tFileTable.getColumn(FT_LAST_MOD_COL))); //double -> long
            dnlsTable.setColumn(3, new LongArray(tFileTable.getColumn(FT_SIZE_COL)));     //double -> long
            //convert dir Index to dir names
            tDirTable.addColumn(0, "dirIndex", new IntArray(0, tDirTable.nRows() - 1));
            dnlsTable.join(1, 0, "", tDirTable);
            dnlsTable.removeColumn(0);
            dnlsTable.setColumnName(0, FileVisitorDNLS.DIRECTORY);

            return dnlsTable;
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            return null;
        }
    }

    /**
     * This tests if 'old' is different from this in any way.
     * <br>This test is from the view of a subscriber who wants to know
     *    when a dataset has changed in any way.
     * <br>So some things like onChange and reloadEveryNMinutes are not checked.
     * <br>This only lists the first change found.
     *
     * <p>EDDGrid overwrites this to also check the axis variables.
     *
     * @param old
     * @return "" if same or message if not.
     */
    public String changed(EDD old) {
        return super.changed(old) + filesChanged;
    }

    /**
     * This is the default implementation of getFileInfo, which
     * gets file info from a locally accessible directory.
     * This is called in the middle of the constructor.
     * Some subclasses override this.
     *
     * @param recursive true if the file search should also search subdirectories
     * @return a table with columns with DIRECTORY, NAME, LASTMODIFIED, and SIZE columns;
     * @throws Throwable if trouble
     */
    public Table getFileInfo(String fileDir, String fileNameRegex, 
        boolean recursive, String pathRegex) throws Throwable {
        //String2.log("EDDTableFromFiles getFileInfo");
        return FileVisitorDNLS.oneStep(fileDir, fileNameRegex, recursive, pathRegex,
            false); //dirsToo
    }

    /**
     * This is the low level method to get source data from one file.
     * This is only called by getSourceDataFromFile();
     *
     * <p>This is used by the constructor to get all of the data from each file.
     * So it is good if this also tests the validity of the file and throws 
     * exception if not valid.
     *
     * <p>Constraints are specified by 2 systems:
     * <br>1) ...Sorted - the old simple system 
     * <br>2) sourceConVars, sourceConOps, sourceConValues - a comprehensive system
     * <br>Each subclass can use either, both, or neither.
     *
     * @param fileDir
     * @param fileName
     * @param sourceDataNames the names of the desired source columns.
     *    All constraintVariables (except columnNameForExtract) will be included in this list.
     *    !!!This will not include columnNameForExtract.
     * @param sourceDataTypes the data types of the desired source columns 
     *    (e.g., "String" or "float").  "boolean" indicates data should be
     *    interpreted as boolan, but stored in the response table as bytes.
     * @param sortedSpacing 
     *    -1: this method will assume nothing about sorted-ness of sortColumn.
     *    0: this method will assume sortColumn is sorted ascending
     *    positive: this method will assume sortColumn is sorted ascending, 
     *         evenly spaced by this increment.
     * @param minSorted the minimum desired value for the sortedColumn 
     *   (use NaN if no limit) (ignored if no sortedColumn).
     *   <br>Subclasses can ignore this and get all of the data if they need to.
     *   <br>With respect to scale_factor and add_offset, this is a source value.
     *   <br>For time, this is the source time, not epochSeconds.
     * @param maxSorted the maximum desired value for the sortedColumn 
     *   (use NaN if no limit) (ignored if no sortedColumn).
     *   <br>Subclasses can ignore this and get all of the data if they need to.
     *   <br>If minSorted is non-NaN, maxSorted will be non-NaN.
     *   <br>With respect to scale_factor and add_offset, this is a source value.
     *   <br>For time, this is the source time, not epochSeconds.
     * @param sourceConVars the source constraint variables.  May be null or size=0. 
     * @param sourceConOps the source constraint operators.
     *    regex is always PrimitiveArray.REGEX_OP, not sourceCanConstrainStringRegex.
     * @param sourceConValues the source constraint values.
     *    timeStamp constraints are numeric source values.
     *    If a timeStamp has String source values or timeStamp op is regex, the constraint has been removed.
     * @param getMetadata  if true, this should get global and variable metadata, too.
     * @param mustGetData if true, the caller must get the actual data;
     *   otherwise it can just return all the values of the sorted variable,
     *   and just the ranges of other variables if convenient
     *   (and -infinity and +infinity for the others).
     * @return a table with the results (with the requested sourceDataTypes).
     *   <br>It may have more or fewer columns than sourceDataNames.
     *   <br>These are raw source results: scale_factor and add_offset will not yet have been applied.
     *   <br>If there is no matching data, it is best to return an empty table, not throw Throwable.
     * @throws Throwable if trouble.
     *   If the file doesn't have a sourceDataName, it isn't an error -- it returns a column of mv's.
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     *   This throws an exception if too much data.
     *   This won't throw an exception if no data.
     */
    public abstract Table lowGetSourceDataFromFile(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        StringArray sourceConVars, StringArray sourceConOps, StringArray sourceConValues,
        boolean getMetadata, boolean mustGetData) throws Throwable;


    /** 
     * This parent method for lowGetSourceDataFromFile
     * handles global: sourceDataNames specially (to convert
     * a file's global metadata to be a data column).
     * See lowGetSourceDataFromFile params.
     * 
     * @param sourceDataTypes  e.g., "float", "String". "boolean"
     *   indicates the data should be interpreted as a boolean, but stored as a byte.
     * @throws an exception if too much data.
     *  This won't (shouldn't) throw an exception if no data.
     */
    public Table getSourceDataFromFile(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        StringArray sourceConVars, StringArray sourceConOps, StringArray sourceConValues,
        boolean getMetadata, boolean mustGetData) throws Throwable {

        //grab any "global:..." and "variable:..." sourceDataNames
        int nSourceDataNames = sourceDataNames.size();
        StringArray sourceNames = new StringArray(); //subset of true sourceNames (actual vars)
        StringArray sourceTypes = new StringArray();
        StringArray globalNames = new StringArray();
        StringArray globalTypes = new StringArray();
        StringArray variableNames    = new StringArray();
        StringArray variableAttNames = new StringArray();
        StringArray variableTypes    = new StringArray();
        for (int i = 0; i < nSourceDataNames; i++) {
            String name = sourceDataNames.get(i);
            if (name.startsWith("global:")) {
                globalNames.add(name.substring(7));
                globalTypes.add(sourceDataTypes[i]);
            } else if (name.startsWith("variable:")) {
                String s = name.substring(9);
                int cpo = s.indexOf(':');
                if (cpo <= 0) 
                    throw new SimpleException("datasets.xml error: " +
                        "To convert variable metadata to data, sourceName should be " +
                        "variable:[varName]:{attributeName]. " +
                        "Invalid sourceName=" + name);                
                variableNames.add(s.substring(0, cpo));
                variableAttNames.add(s.substring(cpo + 1));
                variableTypes.add(sourceDataTypes[i]);
            } else {
                sourceNames.add(name);
                sourceTypes.add(sourceDataTypes[i]);
            }
        }
        //ensure variable:[varName]:[attName] varNames are in sourceNames
        for (int i = 0; i < variableNames.size(); i++) {
            if (sourceNames.indexOf(variableNames.get(i)) < 0) {
                int col = String2.indexOf(dataVariableSourceNames(), variableNames.get(i));
                if (col < 0) 
                    throw new SimpleException("datasets.xml error: " +
                        "To convert variable metadata to data, the [varName] in " +
                        "sourceName=variable:[varName]:[attributeName] " +
                        "must also be a variable in the dataset.  Invalid [varName]=" + 
                        variableNames.get(i));  
                EDV edv = dataVariables[col];
                sourceNames.add(variableNames.get(i));
                sourceTypes.add(edv.sourceDataType());                
            }
        }
        sourceDataTypes = sourceTypes.toArray();

        //get the data
        Table table = lowGetSourceDataFromFile( //this is the only place that calls this method
            fileDir, fileName, 
            sourceNames, sourceDataTypes,
            sortedSpacing, minSorted, maxSorted, 
            sourceConVars, sourceConOps, sourceConValues,
            getMetadata || globalNames.size() > 0 || variableNames.size() > 0, 
            mustGetData);
        int nRows = table.nRows();

        //convert global: metadata to be data columns
        Attributes globalAtts = table.globalAttributes();
        int nGlobalNames = globalNames.size();
        for (int gni = 0; gni < nGlobalNames; gni++) {
            PrimitiveArray pa = globalAtts.remove(globalNames.get(gni));
            if (pa == null) 
                pa = new StringArray();

            //make pa the correct size
            if (pa.size() == 0) {
                pa.addString("");  //missing value
            } else if (pa.size() > 1) {
                pa.removeRange(1, pa.size()); //just the first value
            }

            //force column to be specified type
            PrimitiveArray newPa = PrimitiveArray.factory(
                PrimitiveArray.elementStringToClass(globalTypes.get(gni)), 1, false);
            newPa.append(pa);
            pa = newPa;

            //duplicate the value
            if (pa instanceof StringArray) {
                String ts = pa.getString(0);
                pa.addNStrings(nRows - 1, ts == null? "" : ts);
            } else {
                pa.addNDoubles(nRows - 1, pa.getDouble(0));
            }

            //add pa to the table
            table.addColumn("global:" + globalNames.get(gni), pa);
        }

        //convert variable: metadata to be data columns
        int nVariableNames = variableNames.size();
        for (int vni = 0; vni < nVariableNames; vni++) {
            int col = table.findColumnNumber(variableNames.get(vni));
            if (col >= 0) {
                //var is in file. Try to get attribute
                PrimitiveArray pa = table.columnAttributes(col).get(variableAttNames.get(vni));
                if (pa == null) 
                    pa = new StringArray();

                //make pa the correct size 
                if (pa.size() == 0) {
                    pa.addString("");  //missing value
                } else if (pa.size() > 1) {
                    pa.removeRange(1, pa.size()); //just the first value
                }

                //duplicate the value
                if (nRows > 1) {
                    if (pa instanceof StringArray) {
                        String ts = pa.getString(0);
                        pa.addNStrings(nRows - 1, ts == null? "" : ts);
                    } else {
                        pa.addNDoubles(nRows - 1, pa.getDouble(0));
                    }
                }

                //add pa to the table
                table.addColumn("variable:" + variableNames.get(vni) + 
                    ":" + variableAttNames.get(vni), 
                    pa);
            } //If var or att not in results, just don't add to results table.  
        }

        return table;
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
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {
 
        //get the sourceDapQuery (a query that the source can handle)
        StringArray resultsVariablesNEC = new StringArray();
        //constraints are sourceVars Ops Values
        StringArray conVars   = new StringArray();
        StringArray conOps    = new StringArray();
        StringArray conValues = new StringArray(); 
        getSourceQueryFromDapQuery(userDapQuery,
            resultsVariablesNEC,  //sourceNames
            conVars, conOps, conValues); //timeStamp constraints other than regex are epochSeconds
        if (reallyVerbose) String2.log("getDataForDapQuery sourceQuery=" + 
            formatAsDapQuery(resultsVariablesNEC.toArray(), 
                conVars.toArray(), conOps.toArray(), conValues.toArray()));

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
        StringArray dirList         = (StringArray)tDirTable.getColumn(0);
        ShortArray  ftDirIndex      = (ShortArray)tFileTable.getColumn(0);
        StringArray ftFileList      = (StringArray)tFileTable.getColumn(1);        
        DoubleArray ftLastMod       = (DoubleArray)tFileTable.getColumn(2);
        DoubleArray ftSize          = (DoubleArray)tFileTable.getColumn(3);
        DoubleArray ftSortedSpacing = (DoubleArray)tFileTable.getColumn(4);


        //no need to further prune constraints. 
        //minMaxTable and testing each file (below) deal with constraints.
        //sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //all partially handled
        //sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //all partially handled
        //sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //partially

        //remove extractColumn from requested variables
        int tExtractIndex = -1;
        if (columnNameForExtract.length() > 0) {
            //is request for just columnNameForExtract?
            if (resultsVariablesNEC.size() == 1 &&   //this is before NEC is removed from resultsVariablesNEC
                resultsVariablesNEC.get(0).equals(columnNameForExtract)) {

                Table table = new Table();
                PrimitiveArray names = (StringArray)(tFileTable.getColumn(
                    dv0 + extractedColNameIndex*3 + 0).clone());
                PrimitiveArray unique = names.makeIndices(new IntArray()); //it returns unique values, sorted
                table.addColumn(columnNameForExtract, unique);

                //standardizeResultsTable applies all constraints
                preStandardizeResultsTable(loggedInAs, table); 
                standardizeResultsTable(requestUrl, userDapQuery, table);
                tableWriter.writeAllAndFinish(table);

                cumNNotRead += tFileTable.nRows();
                return;
            }

            //remove extractColumn from resultsVariablesNEC (No Extract Column)
            //but add it back in below...
            tExtractIndex = resultsVariablesNEC.indexOf(columnNameForExtract);
            if (tExtractIndex >= 0) 
                resultsVariablesNEC.remove(tExtractIndex);
        }

        //find dvi for each resultsVariable  and make resultsTypes
        int dvi[] = new int[resultsVariablesNEC.size()]; //store var indexes in dataVariables
        String resultsTypes[] = new String[resultsVariablesNEC.size()]; 
        //String2.log("dataVariableSourceNames=" + String2.toCSSVString(dataVariableSourceNames()));
        for (int rv = 0; rv < resultsVariablesNEC.size(); rv++) {
            String sourceName = resultsVariablesNEC.get(rv);
            dvi[rv] = String2.indexOf(dataVariableSourceNames(), sourceName);
            EDV edv = dataVariables[dvi[rv]];
            resultsTypes[rv] = edv.isBoolean()? "boolean" : edv.sourceDataType();
            //String2.log("rv=" + rv + ": " + sourceName + " dv=" + dvi[rv] + " " + resultsTypes[rv]);
        }

        //find cdvi (index in dataVariables) for each conVar
        int nCon = conVars.size();
        int cdvi[] = new int[nCon];
        double conValuesD[] = new double[nCon];
        for (int con = 0; con < nCon; con++) {
            cdvi[con] = String2.indexOf(dataVariableSourceNames(), conVars.get(con));
            conValuesD[con] = String2.parseDouble(conValues.get(con));
            //op may be PrimitiveArray.REGEX_OP  (won't be sourceCanConstrainStringRegex)
        }

        //distinct?    sometimes minMaxTable indicates there is only 1 value in the file
        String[] parts = Table.getDapQueryParts(userDapQuery); //decoded.  
        boolean distinct = String2.indexOf(parts, "distinct()") >= 0;
        if (reallyVerbose && distinct) String2.log("  query includes \"distinct()\"");

        //check if constraints can't be met by this dataset (by checking minMaxTable)
        //(this is just an expedient. checking each file below has same result, but slower.)
        String reasonNotOk = null;
        //and make true sourceCon where timeStamp constraints are sourceValues (and not regex)
        StringArray sourceConVars   = new StringArray();
        StringArray sourceConOps    = new StringArray();
        StringArray sourceConValues = new StringArray();
        for (int con = 0; con < nCon; con++) {
            int dv = cdvi[con];
            EDV edv = dataVariables[dv];
            String tOp = conOps.get(con);
            //tValue initially: usually a source val, but time is epochSeconds
            String tValue = conValues.get(con);  

            //it EDVTimeStamp, convert tValue epochSeconds into source time string
            if ((edv instanceof EDVTimeStamp) && !tOp.equals(PrimitiveArray.REGEX_OP)) {
                double epSec = conValuesD[con];

                //when testing whole dataset, ignore any constraints for today+/-2 days
                double currentEpSec = System.currentTimeMillis() / 1000.0;
                //String2.log(">>currentEpSec=" + currentEpSec + " - constraintEpSec=" + epSec + " = diffDays=" + ((currentEpSec - epSec)/Calendar2.SECONDS_PER_DAY));
                if (Math.abs(epSec - currentEpSec) < 2 * Calendar2.SECONDS_PER_DAY)
                    continue;

                //convert any remaining time constraints to source time (includes scaleAddOffset)
                tValue = ((EDVTimeStamp)edv).epochSecondsToSourceTimeString(epSec);
                if (debugMode) String2.log(">>source var=" + conVars.get(con) + " constraint=" + tValue);
            }

            if (edv.sourceDataTypeClass() == String.class || tOp.equals(PrimitiveArray.REGEX_OP)) {
                String dsMin    = minMaxTable.getStringData(dv, 0);
                String dsMax    = minMaxTable.getStringData(dv, 1);
                int    dsHasNaN = minMaxTable.getIntData(   dv, 2);
                if (!isOK(dsMin, dsMax, dsHasNaN, tOp, tValue)) {
                    reasonNotOk = "No data matches " +
                        edv.destinationName() + tOp + conValues.get(con) + 
                        " because the variable's min=\"" + dsMin + "\", max=\"" + dsMax + 
                        "\", and hasNaN=" + (dsHasNaN != 0) + ".";
                    if (reallyVerbose) String2.log(reasonNotOk);
                    break;
                }
            } else {
                //numeric variables (and not PrimitiveArray.REGEX_OP)
                double dsMin    = minMaxTable.getDoubleData(dv, 0);  //a source value
                double dsMax    = minMaxTable.getDoubleData(dv, 1);
                int    dsHasNaN = minMaxTable.getIntData(   dv, 2);
                double conValD = String2.parseDouble(conValues.get(con)); //if time, conValD is epochSeconds
                double tValueD = String2.parseDouble(tValue);             //if time, tValueD is a numeric source time
                if (!isOK(dsMin, dsMax, dsHasNaN, tOp, tValueD)) {
                    reasonNotOk = 
                        "No data matches " +
                        edv.destinationName() + tOp + 
                        (edv instanceof EDVTimeStamp? 
                            Calendar2.epochSecondsToLimitedIsoStringT(
                                ((EDVTimeStamp)edv).time_precision(), conValD, "NaN") : 
                            conValues.get(con)) + 
                        " because the variable's source min=" + 
                        edv.destinationMinString() +  //works well with numbers and numeric EDVTimeStamp
                        ", max=" + edv.destinationMaxString() + 
                        ", and hasNaN=" + (dsHasNaN != 0) + "." +
                        (debugMode? "\nconValD=" + conValD + " tValueD=" + tValueD + 
                                    " dsMin=" + dsMin + " " + dsMax : "");
                    if (reallyVerbose) String2.log(reasonNotOk);
                    break;
                }
            }

            //make true sourceCon  (even time constraint values are source values)
            boolean keepCon = false;
            if (tOp.equals(PrimitiveArray.REGEX_OP)) {
                keepCon = sourceCanConstrainStringRegex.length() > 0 &&
                          edv.destValuesEqualSourceValues();

            } else if (edv instanceof EDVTimeStamp) {
                keepCon = ((EDVTimeStamp)edv).sourceTimeIsNumeric(); //just keep numeric time constraints
                    
            } else {
                keepCon = true;
            }

            if (keepCon) {
                //keep this con
                sourceConVars.add(conVars.get(con));
                sourceConOps.add(tOp);  //regex is always PrimitiveArray.REGEX_OP 
                sourceConValues.add(tValue);
            }
        }
        if (reasonNotOk != null) {
            cumNNotRead += tFileTable.nRows();
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (" + reasonNotOk + ")");
        }

        //if dataset has sortedColumnName, look for min,max constraints for it.
        //if sortedDVI is time, min/maxSorted are source values.
        double minSorted = -Double.MAX_VALUE, maxSorted = Double.MAX_VALUE; //get everything
        if (sortedDVI >= 0) {
            for (int con = 0; con < nCon; con++) {
                if (conVars.get(con).equals(sortedColumnSourceName)) {
                    int dv = cdvi[con];
                    EDV edv = dataVariables[dv];
                    String op = conOps.get(con);
                    double valD = String2.parseDouble(conValues.get(con));

                    //convert time constraints from epochSeconds to source values
                    if (edv instanceof EDVTimeStamp) {
                        if (op.equals(PrimitiveArray.REGEX_OP) && !edv.destValuesEqualSourceValues())
                            continue;
                        EDVTimeStamp edvts = (EDVTimeStamp)dataVariables[sortedDVI];
                        if (!edvts.sourceTimeIsNumeric())
                            continue;
                        valD = edvts.epochSecondsToSourceTimeDouble(valD);
                    }

                    //0"!=", 1REGEX_OP, 2"<=", 3">=", 4"=", 5"<", 6">"}; 
                    //It is possible that multiple ops conflict (e.g., multiple < and/or =).
                    //Do some things to deal with it.
                    if      (op.charAt(0) == '<') maxSorted = Math.min(maxSorted, valD); //lowest is most important
                    else if (op.charAt(0) == '>') minSorted = Math.max(minSorted, valD); //highest is most important
                    else if (op.charAt(0) == '=') {
                        minSorted = valD; maxSorted = valD;
                        break; //so that it is last found (trumps others)
                    }
                }
            }
        }
        if (minSorted == -Double.MAX_VALUE) {
            minSorted = Double.NaN;
            maxSorted = Double.NaN;
        }
        if (reallyVerbose) String2.log("minSorted=" + minSorted + " maxSorted=" + maxSorted);

        //go though files in tFileTable
        int nFiles = tFileTable.nRows();
        Table distinctTable = null;
        long nNotRead = 0;  //either don't have matching data or do ('distinct' and 1 value matches)
        long nReadHaveMatch = 0, nReadNoMatch = 0; //read the data file to look for matching data
        FILE_LOOP:
        for (int f = 0; f < nFiles; f++) {
            //can file be rejected based on constraints?
            boolean ok = true;
            for (int con = 0; con < nCon; con++) {
                String op = conOps.get(con);
                int dv = cdvi[con];
                EDV edv = dataVariables[dv];
                if ((edv instanceof EDVTimeStamp) && !op.equals(PrimitiveArray.REGEX_OP)) {
                    //conValue is epochSeconds (not source time units), so convert fMin,fMax to epSeconds
                    EDVTimeStamp tdv = (EDVTimeStamp)edv;
                    double fMin = tdv.sourceTimeToEpochSeconds(tFileTable.getStringData(dv0 + dv*3 + 0, f));
                    double fMax = tdv.sourceTimeToEpochSeconds(tFileTable.getStringData(dv0 + dv*3 + 1, f));
                    int    fNaN = tFileTable.getIntData(dv0 + dv*3 + 2, f);

                    //if fMax is in 20 hours ago to 4 hours in future, set fMax to Now+4hours)
                    // 4hrs avoids clock drift problems
                    double secondsNowP4 = System.currentTimeMillis() / 1000 + 4 * Calendar2.SECONDS_PER_HOUR;
                    if (!Double.isNaN(fMax) && 
                        secondsNowP4 - fMax > 0 &&
                        secondsNowP4 - fMax < Calendar2.SECONDS_PER_DAY) {
                        if (reallyVerbose) 
                            String2.log("file " + tdv.destinationName() + 
                              " maxTime is within last 24hrs, so ERDDAP is pretending file maxTime is now+4hours.");
                        fMax = secondsNowP4;
                    }
                    
                    if (!isOK(fMin, fMax, fNaN, op, conValuesD[con])) {
                        if (reallyVerbose) 
                            String2.log("file " + f + " rejected because failed " +
                                conVars.get(con) + op + 
                                Calendar2.safeEpochSecondsToIsoStringTZ(conValuesD[con], "\"\"") + 
                                " when file min=" + fMin + " max=" + fMax + 
                                ", hasNaN=" + (fNaN != 0) + ".");
                        ok = false;
                        break;
                    }                  

                } else if (edv.sourceDataTypeClass() == String.class || op.equals(PrimitiveArray.REGEX_OP)) {
                    //String variables
                    String fMin = tFileTable.getStringData(dv0 + dv*3 + 0, f);
                    String fMax = tFileTable.getStringData(dv0 + dv*3 + 1, f);
                    int    fNaN = tFileTable.getIntData(   dv0 + dv*3 + 2, f);
                    if (!isOK(fMin, fMax, fNaN, op, conValues.get(con))) {
                       if (reallyVerbose) 
                           String2.log("file " + f + " rejected because failed " +
                                conVars.get(con) + op + conValues.get(con) + 
                                " when file min=\"" + fMin + "\", max=\"" + fMax + 
                                "\", hasNaN=" + (fNaN != 0) + ".");
                        ok = false;
                        break;
                    }

                } else {
                    //numeric variables (and not PrimitiveArray.REGEX_OP)
                    double fMin = tFileTable.getDoubleData(dv0 + dv*3 + 0, f); 
                    double fMax = tFileTable.getDoubleData(dv0 + dv*3 + 1, f); 
                    int    fNaN = tFileTable.getIntData(   dv0 + dv*3 + 2, f);
                    if (!isOK(fMin, fMax, fNaN, op, conValuesD[con])) {
                        if (reallyVerbose) 
                            String2.log("file " + f + " rejected because failed " +
                                conVars.get(con) + op + conValues.get(con) + 
                                " when file min=" + fMin + ", max=" + fMax + 
                                ", hasNaN=" + (fNaN != 0) + ".");
                        ok = false;
                        break;
                    }
                }
            }
            if (!ok) {
                nNotRead++;
                continue;
            }

            //if request is for distinct() values and this file just has 1 value for all requested variables,
            //then no need to even look in the file
            if (distinct) {
                //future: this could be sped up by keeping the table for a run of qualified files
                //  then calling standardizeResultsTable (instead of 1 row at a time).
                boolean allDistinct = true;
                for (int rvi = 0; rvi < dvi.length; rvi++) {
                    int dv = dvi[rvi];
                    if (resultsTypes[rvi].equals("String")) {
                        String fMin = tFileTable.getStringData(dv0 + dv*3 + 0, f);
                        String fMax = tFileTable.getStringData(dv0 + dv*3 + 1, f);
                        if (!fMin.equals(fMax)) {
                            allDistinct = false;
                            break;
                        }
                    } else {
                        double fMin = tFileTable.getNiceDoubleData(dv0 + dv*3 + 0, f);
                        double fMax = tFileTable.getNiceDoubleData(dv0 + dv*3 + 1, f);
                        if (fMin != fMax) {
                            allDistinct = false;
                            break; 
                        }
                    }
                }
                
                //if all requested columns are distinct, add a row to distinctTable
                if (allDistinct) {
                    //if (reallyVerbose) String2.log("file " + f + " is allDistinct");
                    boolean newDistinctTable = distinctTable == null;
                    if (newDistinctTable)
                        distinctTable = new Table();
                    for (int rvi = 0; rvi < dvi.length; rvi++) {
                        int dv = dvi[rvi];
                        String tVal = tFileTable.getStringData(dv0 + dv*3 + 0, f);
                        if (newDistinctTable) {
                            EDV edv = dataVariables[dv];
                            distinctTable.addColumn(edv.sourceName(), 
                                PrimitiveArray.factory(edv.sourceDataTypeClass(), 1, tVal));
                        } else {
                            distinctTable.getColumn(rvi).addString(tVal);
                        }
                    }
                    //if (newDistinctTable) String2.log("  initial distinctTable=\n" + distinctTable.dataToCSVString());

                    //add extractColumn
                    if (tExtractIndex >= 0) {
                        String tVal = tFileTable.getStringData(dv0 + extractedColNameIndex*3 + 0, f);
                        if (newDistinctTable) {
                            PrimitiveArray pa = PrimitiveArray.factory( 
                                dataVariables[extractedColNameIndex].sourceDataTypeClass(), //always String(?)
                                1, tVal);
                            distinctTable.addColumn(dataVariables[extractedColNameIndex].destinationName(), pa);
                        } else {
                            distinctTable.getColumn(dvi.length).addString(tVal);
                        } 
                    }

                    nNotRead++;
                    continue; //to next file;
                }
            }

            //end of run for files which added info to distinctTable
            //so empty out distinctTable
            if (distinctTable != null) {
                //standardizeResultsTable applies all constraints
                preStandardizeResultsTable(loggedInAs, distinctTable); 
                if (distinctTable.nRows() > 0) {
                    standardizeResultsTable(requestUrl, userDapQuery, distinctTable);
                    tableWriter.writeSome(distinctTable);
                    if (tableWriter.noMoreDataPlease) {
                        tableWriter.logCaughtNoMoreDataPlease(datasetID);
                        break FILE_LOOP;
                    }
                }
                distinctTable = null;
            }

            //Read all data from file within minSorted to maxSorted.
            //This throws Throwable if trouble. I think that's appropriate.
            Table table;
            String tDir = dirList.get(ftDirIndex.get(f));
            String tName = ftFileList.get(f);
            if (reallyVerbose) String2.log("#" + f + " get data from " + tDir + tName);
            try {
                //file may be unavailable while being updated
                table = getSourceDataFromFile(tDir, tName,
                    resultsVariablesNEC, resultsTypes, 
                    ftSortedSpacing.get(f), minSorted, maxSorted, 
                    sourceConVars, sourceConOps, sourceConValues,
                    false, true); 

            } catch (WaitThenTryAgainException twwae) {
                throw twwae;

            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

                //if too much data, rethrow t
                String tToString = t.toString();
                if (tToString.indexOf(Math2.memoryTooMuchData) >= 0)
                    throw t;

                //sleep and give it one more try
                try {
                    Thread.sleep(1000);
                    table = getSourceDataFromFile(tDir, tName,
                        resultsVariablesNEC, resultsTypes, 
                        ftSortedSpacing.get(f), minSorted, maxSorted, 
                        sourceConVars, sourceConOps, sourceConValues,
                        false, true); 

                } catch (WaitThenTryAgainException twwae) {
                    throw twwae;

                } catch (Throwable t2) {
                    EDStatic.rethrowClientAbortException(t2);  //first thing in catch{}

                    if (filesAreLocal) {
                        //mark the file as bad   and reload the dataset
                        addBadFileToTableOnDisk(ftDirIndex.get(f), tName, ftLastMod.get(f), 
                            MustBe.throwableToShortString(t));
                    }
                    //an exception here will cause data request to fail (as it should)
                    String2.log(MustBe.throwableToString(t));
                    throw new WaitThenTryAgainException(t); //refer to the original exception
                }
            }
            if (reallyVerbose) String2.log("  table.nRows=" + table.nRows());

            if (table.nRows() > 0) {

                //add extractColumn
                if (tExtractIndex >= 0) {
                    PrimitiveArray pa = PrimitiveArray.factory(
                        dataVariables[extractedColNameIndex].sourceDataTypeClass(), //always String(?)
                        table.nRows(), 
                        tFileTable.getStringData(dv0 + extractedColNameIndex*3 + 0, f));
                    table.addColumn(dataVariables[extractedColNameIndex].destinationName(), pa);
                }

                //standardizeResultsTable applies all constraints
                preStandardizeResultsTable(loggedInAs, table); 
                if (table.nRows() > 0) { //preStandardize may have altered it
                    standardizeResultsTable(requestUrl, userDapQuery, table);
                    tableWriter.writeSome(table);
                    nReadHaveMatch++;
                    if (tableWriter.noMoreDataPlease) {
                        tableWriter.logCaughtNoMoreDataPlease(datasetID);
                        break FILE_LOOP;
                    }
                } else {
                    nReadNoMatch++;
                }
            } else {
                nReadNoMatch++;
            }
        }

        //flush distinctTable
        if (distinctTable != null) {
            //standardizeResultsTable applies all constraints
            preStandardizeResultsTable(loggedInAs, distinctTable); 
            if (distinctTable.nRows() > 0) {
                standardizeResultsTable(requestUrl, userDapQuery, distinctTable);
                tableWriter.writeSome(distinctTable);
            }
            distinctTable = null;
        }
        cumNNotRead       += nNotRead;
        cumNReadHaveMatch += nReadHaveMatch;
        cumNReadNoMatch   += nReadNoMatch;
        if (reallyVerbose) { 
            long total = Math.max(1, nNotRead + nReadHaveMatch + nReadNoMatch);
            String2.log("     notRead="       + String2.right("" + (nNotRead       * 100 / total), 3) +
                        "%    readHaveMatch=" + String2.right("" + (nReadHaveMatch * 100 / total), 3) +
                        "%    readNoMatch="   + String2.right("" + (nReadNoMatch   * 100 / total), 3) + 
                        "%    total=" + total);
            long cumTotal     = Math.max(1, cumNNotRead + cumNReadHaveMatch + cumNReadNoMatch);
            String2.log("  cumNotRead="       + String2.right("" + (cumNNotRead       * 100 / cumTotal), 3) +
                        "% cumReadHaveMatch=" + String2.right("" + (cumNReadHaveMatch * 100 / cumTotal), 3) +
                        "% cumReadNoMatch="   + String2.right("" + (cumNReadNoMatch   * 100 / cumTotal), 3) + 
                        "% cumTotal=" + cumTotal + "  " + datasetID);
        }

        //done
        tableWriter.finish();

    }

    /**
     * getDataForDapQuery always calls this right before standardizeResultsTable.
     * EDDTableFromPostNcFiles uses this to remove data not accessible to this user.
     */
    public void preStandardizeResultsTable(String loggedInAs, Table table) {
        //this base version does nothing
    }


    /**
     * For String variables (or numeric variables and PrimitiveArray.REGEX_OP), 
     * given a min, max, hasNaN value for a given file (or the whole dataset),
     * this returns true if the file *may* have data matching opIndex,opValue.
     *
     * <p>See PrimitiveArray.testValueOpValue: Note that "" is not treated specially.  "" isn't like NaN.  
     * <br>testValueOpValue("a" &gt; "")  will return true.
     * <br>testValueOpValue("a" &lt; "")  will return false.
     * <br>Having min here be exclusive of "" allows better testing
     * <br>e.g., it can say that a file with "", "a", "z", !isOK for ="A",
     *   which is an important type of test (given that .subset generates "=" constraints).
     *
     * @param min  exclusive of "".  If no valid values, min and max should be ""
     * @param max 
     * @param hasNaN 0=false 1=true (has "" values)
     * @param conOp
     * @param conValue the constaintValue
     */
    public static boolean isOK(String min, String max, int hasNaN, String conOp, String conValue) {
        //THE SPECIAL TESTS REQUIRE LOTS OF THOUGHT!!!

        //deal with special tests when hasNaN  (where hasNaN=1 makes a difference)
        if (hasNaN == 1) {
            if (conValue.equals("") &&                           // ""="" returns true
                (conOp.equals(PrimitiveArray.REGEX_OP) || conOp.equals("=") || 
                 conOp.equals(">=")     || conOp.equals("<=")))
                return true;
            else if (conOp.equals("<")) 
                return !conValue.equals("");  // ""<"a" returns true
                                              // ""<""  returns false
        }

        //below here, hasNaN is irrelevant (think as if hasNaN=0; tests done with min=someValue)

        int minC = min.compareTo(conValue); //<0 indicates min < conValue;  >0 indicates min > conValue
        int maxC = max.compareTo(conValue);

        //0"!=", 1REGEX_OP, 2"<=", 3">=", 4"=", 5"<", 6">"};         
        if (conOp.equals("!=")) {
            if (min.equals(max) && min.equals(conValue)) return false; 
        } else if (conOp.equals(PrimitiveArray.REGEX_OP)) {
            if (min.equals(max) && !min.matches(conValue)) return false;
        } else if (conOp.equals("<=")) {
            return minC <= 0; 
        } else if (conOp.equals(">=")) {
            return maxC >= 0;
        } else if (conOp.equals("=")) {  
            return minC <= 0 && maxC >= 0;
        } else if (conOp.equals("<")) {
            return minC < 0; 
        } else if (conOp.equals(">")) {
            return maxC > 0;
        }

        return true;
    }

    /**
     * For numeric variables when op isn't PrimitiveArray.REGEX_OP,
     * given a min and a max value for a given file (or the whole dataset),
     * this returns true if the file may have data matching opIndex,opValue.
     *
     * @param min if no valid values, this should be NaN
     * @param max if no valid values, this should be NaN
     * @param hasNaN 0=false 1=true
     * @param conOp    Must *not* be PrimitiveArray.REGEX_OP  
     * @param conValue the constaintValue
     */
    public static boolean isOK(double min, double max, int hasNaN, String conOp, double conValue) {
        //THE SPECIAL TESTS REQUIRE LOTS OF THOUGHT!!!

        //conValue=NaN tests
        if (Double.isNaN(conValue)) {
            if (conOp.equals("=") || conOp.equals("<=") || conOp.equals(">=")) 
                                           //  NaN=NaN returns true     
                return hasNaN == 1;        //  5  =NaN returns false
            else if (conOp.equals("!="))   //  5 !=NaN returns true
                return !Double.isNaN(max); // NaN!=NaN returns false
            else return false;             //NaN tests other than = != return false
        }

        //file has just NaN
        if (Double.isNaN(min) && Double.isNaN(max)) {  //and we know conValue isn't NaN
            if (conOp.equals("!=")) 
                 return true;  //always: NaN != 5
            else return false; //never:  NaN =  5   and other ops, too
        }

        //0"!=", 1REGEX_OP, 2"<=", 3">=", 4"=", 5"<", 6">"};         
        //this does strict comparisons (hard to use AlmostEqual, GAE, LAE)
        //precision=5 significant figures
        //not very precise so works with floats and doubles; for time, this is ~28 hours
        //but that's okay, better to say okay here (and fail later)
        int p = 5;
        if (conOp.equals("!=")) {
            if (min == max && min == conValue) return false;    //be strict to reject
        //PrimitiveArray.REGEX_OP is handled by String isOK
        } else if (conOp.equals("<=")) {
            return Math2.lessThanAE(p, min, conValue); 
        } else if (conOp.equals(">=")) {
            return Math2.greaterThanAE(p, max, conValue);
        } else if (conOp.equals("=")) {  
            return Math2.lessThanAE(p, min, conValue) && Math2.greaterThanAE(p, max, conValue);
        } else if (conOp.equals("<")) {
            return min < conValue; 
        } else if (conOp.equals(">")) {
            return max > conValue;
        }

        return true;
    }

    /** Test isOK() */
    public static void testIsOK() {
        String2.log("\n* EDDTableFromFiles.testIsOK");
        //0"!=", 1REGEX_OP, 2"<=", 3">=", 4"=", 5"<", 6">"};         
        //isOK(String min, String max, int hasNaN, String conOp, String conValue) {
        //isOK(double min, double max, int hasNaN, String conOp, double conValue) {
        String ROP = PrimitiveArray.REGEX_OP;

        Test.ensureEqual(String2.max("a", ""), "a", "");  //"" sorts lower than any string with characters       

        //simple tests  String
        Test.ensureEqual(isOK("a", "z", 0,  "=", "c"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  "=", "5"), false, ""); 
        Test.ensureEqual(isOK("a", "z", 0, "!=", "c"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, "!=", "5"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, "<=", "|"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, "<=", "c"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, "<=", "a"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, "<=", "5"), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  "<", "|"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  "<", "c"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  "<", "a"), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  "<", "5"), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, ">=", "|"), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, ">=", "z"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, ">=", "c"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, ">=", "5"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  ">", "|"), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  ">", "z"), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  ">", "c"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  ">", "5"), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  ROP, "(5)"), true,  ""); 
        Test.ensureEqual(isOK("a", "a", 0,  ROP, "(a)"), true,   ""); //only really tests if min=max
        Test.ensureEqual(isOK("a", "a", 0,  ROP, "(5)"), false,  ""); //only really tests if min=max


        //simple tests  numeric       
        Test.ensureEqual(isOK(2, 4, 0,  "=", 3), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  "=", 0), false, ""); 
        Test.ensureEqual(isOK(2, 4, 0, "!=", 3), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "!=", 0), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "<=", 6), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "<=", 3), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "<=", 2.0000000001), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "<=", 2), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "<=", 1.9999999999), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "<=", 0), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  "<", 6), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  "<", 3), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  "<", 2.0000000001), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  "<", 2), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  "<", 1.9999999999), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  "<", 0), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, ">=", 6), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, ">=", 4.0000000001), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, ">=", 4), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, ">=", 3.9999999999), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, ">=", 3), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, ">=", 0), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  ">", 6), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  ">", 4.0000000001), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  ">", 4), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  ">", 3.9999999999), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  ">", 3), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0,  ">", 0), true,  ""); 
        Test.ensureEqual(isOK("2", "4", 0,  ROP, "(5)"), true,  ""); 
        Test.ensureEqual(isOK("2", "2", 0,  ROP, "(2)"), true,   ""); //only really tests if min=max
        Test.ensureEqual(isOK("2", "2", 0,  ROP, "(5)"), false,  ""); //only really tests if min=max

        // value="" tests  String    hasNaN=0=false
        Test.ensureEqual(isOK("a", "z", 0,  "=", ""), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, "!=", ""), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, "<=", ""), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  "<", ""), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 0, ">=", ""), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  ">", ""), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 0,  ROP, ""), true,  ""); 
        Test.ensureEqual(isOK("a", "a", 0,  ROP, ""), false,  ""); //only really tests if min=max

        //value=NaN tests  numeric    hasNaN=0=false   
        Test.ensureEqual(isOK(2, 4, 0,  "=", Double.NaN), false,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "!=", Double.NaN), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 0, "<=", Double.NaN), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK(2, 4, 0,  "<", Double.NaN), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK(2, 4, 0, ">=", Double.NaN), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK(2, 4, 0,  ">", Double.NaN), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK("2", "4", 0,  ROP, ""), true,  ""); 
        Test.ensureEqual(isOK("2", "2", 0,  ROP, ""), false,   ""); //only really tests if min=max

        // value="" tests  String    hasNaN=1=true
        Test.ensureEqual(isOK("a", "z", 1,  "=", ""), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 1, "!=", ""), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 1, "<=", ""), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 1,  "<", ""), false,  ""); 
        Test.ensureEqual(isOK("a", "z", 1, ">=", ""), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 1,  ">", ""), true,  ""); 
        Test.ensureEqual(isOK("a", "z", 1,  ROP, ""), true,  ""); 
        Test.ensureEqual(isOK("a", "a", 1,  ROP, ""), true,  ""); //only really tests if min=max

        //value=NaN tests  numeric    hasNaN=1=true
        Test.ensureEqual(isOK(2, 4, 1,  "=", Double.NaN), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 1, "!=", Double.NaN), true,  ""); 
        Test.ensureEqual(isOK(2, 4, 1, "<=", Double.NaN), true,  ""); // =
        Test.ensureEqual(isOK(2, 4, 1,  "<", Double.NaN), false, ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK(2, 4, 1, ">=", Double.NaN), true,  ""); // =
        Test.ensureEqual(isOK(2, 4, 1,  ">", Double.NaN), false, ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK("2", "4", 1,  ROP, ""), true,  ""); 
        Test.ensureEqual(isOK("2", "2", 1,  ROP, ""), true,   ""); //only really tests if min=max


        //*** DATA IS ALL ""    hasNaN must be 1
        //DATA IS ALL ""   value="c" tests  String   
        Test.ensureEqual(isOK("", "", 1,  "=", "c"), false, ""); 
        Test.ensureEqual(isOK("", "", 1, "!=", "c"), true,  ""); 
        Test.ensureEqual(isOK("", "", 1, "<=", "c"), true,  ""); 
        Test.ensureEqual(isOK("", "", 1,  "<", "c"), true,  ""); 
        Test.ensureEqual(isOK("", "", 1, ">=", "c"), false,  ""); 
        Test.ensureEqual(isOK("", "", 1,  ">", "c"), false,  ""); 
        Test.ensureEqual(isOK("", "", 1,  ROP, "(c)"), false,  ""); //only really tests if min=max

        //DATA IS ALL ""   value=5 tests  numeric    
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1,  "=", 5), false,  ""); 
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1, "!=", 5), true,  ""); 
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1, "<=", 5), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1,  "<", 5), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1, ">=", 5), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1,  ">", 5), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK("", "", 1,  ROP, ""), true,  ""); 
        Test.ensureEqual(isOK("", "", 1,  ROP, ""), true,   ""); //only really tests if min=max

        //DATA IS ALL ""   value="" tests  String    hasNaN=1=true
        Test.ensureEqual(isOK("", "", 1,  "=", ""), true,  ""); 
        Test.ensureEqual(isOK("", "", 1, "!=", ""), false,  ""); 
        Test.ensureEqual(isOK("", "", 1, "<=", ""), true,  ""); 
        Test.ensureEqual(isOK("", "", 1,  "<", ""), false,  ""); 
        Test.ensureEqual(isOK("", "", 1, ">=", ""), true,  ""); 
        Test.ensureEqual(isOK("", "", 1,  ">", ""), false,  ""); 
        Test.ensureEqual(isOK("", "", 1,  ROP, ""), true,  ""); //only really tests if min=max

        //DATA IS ALL ""   value=NaN tests  numeric    hasNaN=1=true
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1,  "=", Double.NaN), true,  ""); 
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1, "!=", Double.NaN), false,  ""); 
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1, "<=", Double.NaN), true,   ""); // =
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1,  "<", Double.NaN), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1, ">=", Double.NaN), true,   ""); // =
        Test.ensureEqual(isOK(Double.NaN, Double.NaN, 1,  ">", Double.NaN), false,  ""); //NaN tests other than = != return false
        Test.ensureEqual(isOK("", "", 1,  ROP, ""), true,   ""); //only really tests if min=max

    }

    /** Quick test of regex */
    public static void testRegex() {

        String2.log("\n*** EDDTableFromFiles.testRegex()");
        String s = "20070925_41001_5day.csv";
        Test.ensureEqual(String2.extractRegex(s, "^[0-9]{8}_", 0), "20070925_", "");
        Test.ensureEqual(String2.extractRegex(s, "_5day\\.csv$", 0), "_5day.csv", "");
    }


    /** This runs all of the test for this class. */
    public static void test() throws Throwable {
        String2.log("\n*** EDDTableFromFiles.test");
        testIsOK();
        testRegex();
    }


}
