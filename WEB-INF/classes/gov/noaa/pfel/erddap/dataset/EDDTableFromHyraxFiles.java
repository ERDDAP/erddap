/* 
 * EDDTableFromHyraxFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
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

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;

import java.io.File;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.*;
import org.joda.time.format.*;

/** 
 * This class downloads data from a Hyrax data server with lots of files 
 * into .nc files in the [bigParentDirectory]/copy/datasetID, 
 * and then uses superclass EDDTableFromFiles methods to read/serve data 
 * from the .nc files. So don't wrap this class in EDDTableCopy.
 * 
 * <p>The Hyrax files can be n-dimensional (1,2,3,4,...) DArray or DGrid
 * OPeNDAP files, each of which is flattened into a table.
 *
 * <p>This class is very similar to EDDTableFromThreddsFiles.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-06-08
 */
public class EDDTableFromHyraxFiles extends EDDTableFromFiles { 

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;


    /** 
     * The constructor just calls the super constructor. 
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * <p>The sortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     */
    public EDDTableFromHyraxFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
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

        super("EDDTableFromHyraxFiles", true, //isLocal is now set to true (copied files)
            tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes,
            EDStatic.fullCopyDirectory + tDatasetID + "/", //force fileDir to be the copyDir 
            tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory);

    }

    /**
     * Create tasks to download files.
     * If addToHyraxUrlList is completelySuccessful, local files that
     * aren't mentioned on the server will be renamed [fileName].ncRemoved .
     * 
     * @param catalogUrl  should have /catalog/ in the middle and 
     *    / or contents.html at the end
     *    e.g., http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/contents.html
     * This won't throw an exception.
     */
    public static void makeDownloadFileTasks(String tDatasetID, 
        String catalogUrl, String fileNameRegex, boolean recursive) {

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
            //catalogUrl http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/contents.html
            if (catalogUrl == null || catalogUrl.length() == 0)
                throw new RuntimeException("ERROR: <sourceUrl>http://.../contents.html</sourceUrl> " +
                    "must be in the addGlobalAttributes section of the datasets.xml " +
                    "for datasetID=" + tDatasetID);
            if (!catalogUrl.endsWith("/") && !catalogUrl.endsWith("/contents.html"))
                catalogUrl += "/";
            String lookFor = File2.getDirectory(catalogUrl); 
            int lookForLength = lookFor.length();

            //mimic the remote dir structure in baseDir
            String baseDir = EDStatic.fullCopyDirectory + tDatasetID + "/";
            //e.g. localFile EDStatic.fullCopyDirectory + tDatasetID +  / 1987/M07/pentad_19870710_v11l35flk.nc.gz
            File2.makeDirectory(baseDir);

            //gather all sourceFile info
            StringArray sourceFileName  = new StringArray();
            DoubleArray sourceFileLastMod = new DoubleArray();             
            boolean completelySuccessful = EDDGridFromDap.addToHyraxUrlList(
                catalogUrl, fileNameRegex, recursive,
                sourceFileName, sourceFileLastMod);

            //Rename local files that shouldn't exist?
            //If completelySuccessful and found some files, 
            //local files that aren't mentioned on the server will be renamed
            //[fileName].ncRemoved .
            //If not completelySuccessful, perhaps the server is down temporarily,
            //no files will be renamed.
            //!!! This is imperfect. If a remote sub webpage always fails, then
            //  no local files will ever be renamed.
            if (completelySuccessful && sourceFileName.size() > 0) {
                //make a hashset of theoretical local fileNames that will exist 
                //  after copying based on getHyraxFileInfo
                HashSet hashset = new HashSet();
                int nFiles = sourceFileName.size();
                for (int f = 0; f < nFiles; f++) {
                    String sourceName = sourceFileName.get(f);
                    if (sourceName.startsWith(lookFor)) {
                        String willExist = baseDir + sourceName.substring(lookForLength);
                        hashset.add(willExist);
                        //String2.log("  willExist=" + willExist);
                    } 
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
                String sourceName = sourceFileName.get(f);
                if (!sourceName.startsWith(lookFor)) {
                    if (!remoteErrorLogged) {
                        String2.log(
                            "ERROR! lookFor=" + lookFor + " wasn't at start of sourceName=" + sourceName);
                        remoteErrorLogged = true;
                    }
                    continue;
                }

                //see if up-to-date localFile exists  (keep name identical; don't add .nc)
                String localFile = baseDir + sourceName.substring(lookForLength);
                String reason = "";
                try {
                    //don't use File2 so more efficient for current purpose
                    File file = new File(localFile);
                    if (!file.isFile())
                        reason = "new file";
                    else if (file.lastModified() != 
                        Math2.roundToLong(sourceFileLastMod.get(f) * 1000))
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
                taskOA[1] = sourceName;
                taskOA[2] = localFile;
                taskOA[3] = new Long(Math2.roundToLong(sourceFileLastMod.get(f) * 1000));
                int tTaskNumber = EDStatic.addTask(taskOA);
                if (tTaskNumber >= 0) {
                    nTasksCreated++;
                    taskNumber = tTaskNumber;
                    if (reallyVerbose)
                        String2.log("  task#" + taskNumber + " TASK_ALL_DAP_TO_NC reason=" + reason +
                            "\n    from=" + sourceName +
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
        //String2.log("  EDDTableFromHyraxFiles.lowGetSourceDataFromFile table.nRows=" + table.nRows());

        return table;
    }



    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromHyraxFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tLocalDirUrl the locally useful starting (parent) directory with a 
     *    Hyrax sub-catalog for searching for files
     *   e.g., http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *   e.g, "pentad.*\\.nc\\.gz"
     * @param oneFileDapUrl  the locally useful url for one file, without ending .das or .html
     *   e.g., http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/M09/pentad_19870908_v11l35flk.nc.gz
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

        String2.log("EDDTableFromHyraxFiles.generateDatasetsXml" +
            "\n  tLocalDirUrl=" + tLocalDirUrl + 
            "\n  oneFileDapUrl=" + oneFileDapUrl);
        String tPublicDirUrl = convertToPublicSourceUrl(tLocalDirUrl);

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        DConnect dConnect = new DConnect(oneFileDapUrl, acceptDeflate, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);;
        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

        //variables
        Enumeration en = dds.getVariables();
        while (en.hasMoreElements()) {
            BaseType baseType = (BaseType)en.nextElement();
            String varName = baseType.getName();
            Attributes sourceAtts = new Attributes();
            OpendapHelper.getAttributes(das, varName, sourceAtts);
            PrimitiveVector pv = null; //for determining data type
            if (baseType instanceof DGrid) {   //for multidim vars
                DGrid dGrid = (DGrid)baseType;
                BaseType bt0 = dGrid.getVar(0); //holds the data
                pv = bt0 instanceof DArray? ((DArray)bt0).getPrimitiveVector() : bt0.newPrimitiveVector();
            } else if (baseType instanceof DArray) {  //for the dimension vars
                DArray dArray = (DArray)baseType;
                pv = dArray.getPrimitiveVector();
            } else {
                if (verbose) String2.log("  baseType=" + baseType.toString() + " isn't supported yet.\n");
            }
            if (pv != null) {
                dataSourceTable.addColumn(dataSourceTable.nColumns(), varName, 
                    PrimitiveArray.factory(OpendapHelper.getElementClass(pv), 2, false),
                    sourceAtts);
                dataAddTable.addColumn(dataAddTable.nColumns(), varName, 
                    PrimitiveArray.factory(OpendapHelper.getElementClass(pv), 2, false),
                    makeReadyToUseAddVariableAttributesForDatasetsXml(
                        sourceAtts, varName, true, true)); //addColorBarMinMax, tryToFindLLAT

                //if a variable has timeUnits, files are likely sorted by time
                //and no harm if files aren't sorted that way
                if (tSortedColumnSourceName.length() == 0 && 
                    EDVTimeStamp.hasTimeUnits(sourceAtts, null))
                    tSortedColumnSourceName = varName;
            }
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

        //global attributes
        OpendapHelper.getAttributes(das, "GLOBAL", dataSourceTable.globalAttributes());
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", tPublicDirUrl);
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataAddTable)? "Point" : "Other",
                tLocalDirUrl, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //write the information
        StringBuilder sb = new StringBuilder();
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = (tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName)).trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromHyraxFiles\" datasetID=\"" + 
                suggestDatasetID(tPublicDirUrl + tFileNameRegex) + 
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <fileDir></fileDir>\n" +
            "    <recursive>true</recursive>\n" +
            "    <fileNameRegex>" + tFileNameRegex + "</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + tPreExtractRegex + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + tPostExtractRegex + "</postExtractRegex>\n" +
            "    <extractRegex>" + tExtractRegex + "</extractRegex>\n" +
            "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n");
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
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/07/", 
"pentad.*\\.nc\\.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/07/pentad_19870705_v11l35flk.nc.gz", 
2880,
"", "", "", "", //extract
"time", "time", new Attributes());

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromHyraxFiles\" datasetID=\"nasa_jpl_91f4_69e3_32c9\" active=\"true\">\n" +
"    <reloadEveryNMinutes>2880</reloadEveryNMinutes>\n" +
"    <fileDir></fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>pentad.*\\.nc\\.gz</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex></preExtractRegex>\n" +
"    <postExtractRegex></postExtractRegex>\n" +
"    <extractRegex></extractRegex>\n" +
"    <columnNameForExtract></columnNameForExtract>\n" +
"    <sortedColumnSourceName>time</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"base_date\" type=\"shortList\">1987 7 5</att>\n" +
"        <att name=\"Conventions\">COARDS</att>\n" +
"        <att name=\"description\">Time average of level3.0 products for the period: 1987-07-05 to 1987-07-09</att>\n" +
"        <att name=\"history\">Created by NASA Goddard Space Flight Center under the NASA REASoN CAN: A Cross-Calibrated, Multi-Platform Ocean Surface Wind Velocity Product for Meteorological and Oceanographic Applications</att>\n" +
"        <att name=\"title\">Atlas FLK v1.1 derived surface winds (level 3.5)</att>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"creator_email\">podaac@podaac.jpl.nasa.gov</att>\n" +
"        <att name=\"creator_name\">NASA GSFC MEaSUREs, NOAA</att>\n" +
"        <att name=\"creator_url\">http://podaac.jpl.nasa.gov/dataset/CCMP_MEASURES_ATLAS_L4_OW_L3_0_WIND_VECTORS_FLK</att>\n" +
"        <att name=\"infoUrl\">http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/07/.html</att>\n" +
"        <att name=\"institution\">NASA GSFC, NOAA</att>\n" +
"        <att name=\"keywords\">atlas, atmosphere,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Wind Stress,\n" +
"atmospheric, component, derived, downward, eastward, eastward_wind, flk, gsfc, level, meters, nasa, noaa, northward, northward_wind, number, observations, oceanography, physical, physical oceanography, pseudostress, speed, statistics, stress, surface, surface_downward_eastward_stress, surface_downward_northward_stress, time, u-component, u-wind, v-component, v-wind, v1.1, wind, wind_speed, winds</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"sourceUrl\">http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/07/</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">Time average of level3.0 products for the period: 1987-07-05 to 1987-07-09</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.125 359.875</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-78.375 78.375</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">4440.0 4440.0</att>\n" +
"            <att name=\"avg_period\">0000-00-05 00:00:00</att>\n" +
"            <att name=\"delta_t\">0000-00-05 00:00:00</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"units\">hours since 1987-01-01 00:00:0.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">4700.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">4200.0</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>uwnd</sourceName>\n" +
"        <destinationName>uwnd</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-15.897105 22.495602</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"long_name\">u-wind at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.001525972</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">-50.0 50.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">eastward_wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>vwnd</sourceName>\n" +
"        <destinationName>vwnd</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-16.493101 25.951406</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"long_name\">v-wind at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.001525972</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">-50.0 50.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">northward_wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wspd</sourceName>\n" +
"        <destinationName>wspd</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.040334757 29.29576</att>\n" +
"            <att name=\"add_offset\" type=\"float\">37.5</att>\n" +
"            <att name=\"long_name\">wind speed at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.001144479</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">0.0 75.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>upstr</sourceName>\n" +
"        <destinationName>upstr</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-284.62888 657.83044</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"long_name\">u-component of pseudostress at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.03051944</att>\n" +
"            <att name=\"units\">m2/s2</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">-1000.0 1000.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">0.5</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-0.5</att>\n" +
"            <att name=\"ioos_category\">Physical Oceanography</att>\n" +
"            <att name=\"standard_name\">surface_downward_eastward_stress</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>vpstr</sourceName>\n" +
"        <destinationName>vpstr</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-305.23505 694.61383</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"long_name\">v-component of pseudostress at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.03051944</att>\n" +
"            <att name=\"units\">m2/s2</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">-1000.0 1000.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">0.5</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-0.5</att>\n" +
"            <att name=\"ioos_category\">Physical Oceanography</att>\n" +
"            <att name=\"standard_name\">surface_downward_northward_stress</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>nobs</sourceName>\n" +
"        <destinationName>nobs</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 20.0</att>\n" +
"            <att name=\"add_offset\" type=\"float\">32766.0</att>\n" +
"            <att name=\"long_name\">number of observations</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">1.0</att>\n" +
"            <att name=\"units\">count</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">0.0 65532.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            /* *** This doesn't work. Usually no files already downloaded. 
            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "nasa_jpl_ae1a_8793_8b49", "");
            Test.ensureEqual(edd.title(), "Atlas FLK v1.1 derived surface winds (level 3.5)", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "longitude, latitude, time, uwnd, vwnd, wspd, upstr, vpstr, nobs", "");
            */

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml." + 
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml2() throws Throwable {
        testVerboseOn();

        try {
            String results = generateDatasetsXml(
"http://data.nodc.noaa.gov/opendap/wod/XBT/195209-196711/contents.html", 
"wod_002057.*\\.nc",
"http://data.nodc.noaa.gov/opendap/wod/XBT/195209-196711/wod_002057989O.nc", 
2880,
"", "", "", "", //extract
"time", "time", new Attributes());

/* 2012-04-10 That throws exception:     Hyrax isn't compatible with JDAP???
dods.dap.DDSException:
Parse Error on token: String
In the dataset descriptor object:
Expected a variable declaration (e.g., Int32 i;).
 at dods.dap.parser.DDSParser.error(DDSParser.java:710)
 at dods.dap.parser.DDSParser.NonListDecl(DDSParser.java:241)
 at dods.dap.parser.DDSParser.Declaration(DDSParser.java:155)
 at dods.dap.parser.DDSParser.Declarations(DDSParser.java:131)
 at dods.dap.parser.DDSParser.Dataset(DDSParser.java:97)
 at dods.dap.DDS.parse(DDS.java:442)
 at dods.dap.DConnect.getDDS(DConnect.java:388)
 at gov.noaa.pfel.erddap.dataset.EDDTableFromHyraxFiles.generateDatasetsXml(EDDTableFromHyraxFiles.java:570)
 at gov.noaa.pfel.erddap.dataset.EDDTableFromHyraxFiles.testGenerateDatasetsXml(EDDTableFromHyraxFiles.java:930)
 at gov.noaa.pfel.erddap.dataset.EDDTableFromHyraxFiles.test(EDDTableFromHyraxFiles.java:1069)
 at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:1395)
 */
 String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset zzz" +
"\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            /* *** This doesn't work. Usually no files already downloaded. 
            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "nasa_jpl_ae1a_8793_8b49", "");
            Test.ensureEqual(edd.title(), "Atlas FLK v1.1 derived surface winds (level 3.5)", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "longitude, latitude, time, uwnd, vwnd, wspd, upstr, vpstr, nobs", "");
            */

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml." + 
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * This tests the methods in this class.
     * This is a bizarre test since it is really gridded data. But no alternatives currently. 
     * Actually, this is useful, because it tests serving gridded data via tabledap.
     *
     * @throws Throwable if trouble
     */
    public static void testJpl(boolean deleteCachedInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromHyraxFiles.testJpl() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        try {
        String id = "testEDDTableFromHyraxFiles";
        if (deleteCachedInfo) 
            deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        try {
        String2.log("\n****************** EDDTableFromHyraxFiles das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.125, 359.875;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -78.375, 78.375;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 5.576256e+8, 5.597856e+8;\n" +
"    String avg_period \"0000-00-05 00:00:00\";\n" +
"    String axis \"T\";\n" +
"    Float64 colorBarMaximum 6300.0;\n" +
"    Float64 colorBarMinimum 5700.0;\n" +
"    String delta_t \"0000-00-05 00:00:00\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  uwnd {\n" +
"    Float32 actual_range -17.78368, 25.81639;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"u-wind at 10 meters\";\n" +
"    Float32 missing_value -50.001526;\n" +
"    String standard_name \"eastward_wind\";\n" +
"    String units \"m/s\";\n" +
"    Float32 valid_range -50.0, 50.0;\n" +
"  }\n" +
"  vwnd {\n" +
"    Float32 actual_range -17.49374, 19.36153;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"v-wind at 10 meters\";\n" +
"    Float32 missing_value -50.001526;\n" +
"    String standard_name \"northward_wind\";\n" +
"    String units \"m/s\";\n" +
"    Float32 valid_range -50.0, 50.0;\n" +
"  }\n" +
"  wspd {\n" +
"    Float32 actual_range 0.01487931, 27.53731;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"wind speed at 10 meters\";\n" +
"    Float32 missing_value -0.001143393;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m/s\";\n" +
"    Float32 valid_range 0.0, 75.0;\n" +
"  }\n" +
"  upstr {\n" +
"    Float32 actual_range -317.2191, 710.9198;\n" +
"    Float64 colorBarMaximum 0.5;\n" +
"    Float64 colorBarMinimum -0.5;\n" +
"    String ioos_category \"Physical Oceanography\";\n" +
"    String long_name \"u-component of pseudostress at 10 meters\";\n" +
"    Float32 missing_value -1000.0305;\n" +
"    String standard_name \"surface_downward_eastward_stress\";\n" +
"    String units \"m2/s2\";\n" +
"    Float32 valid_range -1000.0, 1000.0;\n" +
"  }\n" +
"  vpstr {\n" +
"    Float32 actual_range -404.8404, 386.9255;\n" +
"    Float64 colorBarMaximum 0.5;\n" +
"    Float64 colorBarMinimum -0.5;\n" +
"    String ioos_category \"Physical Oceanography\";\n" +
"    String long_name \"v-component of pseudostress at 10 meters\";\n" +
"    Float32 missing_value -1000.0305;\n" +
"    String standard_name \"surface_downward_northward_stress\";\n" +
"    String units \"m2/s2\";\n" +
"    Float32 valid_range -1000.0, 1000.0;\n" +
"  }\n" +
"  nobs {\n" +
"    Float32 actual_range 0.0, 20.0;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"number of observations\";\n" +
"    Float32 missing_value -1.0;\n" +
"    String units \"count\";\n" +
"    Float32 valid_range 0.0, 65532.0;\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    Int16 base_date 1987, 9, 28;\n" +
"    String cdm_data_type \"Point\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String description \"Time average of level3.0 products for the period: 1987-09-28 to 1987-10-02\";\n" +
"    Float64 Easternmost_Easting 359.875;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 78.375;\n" +
"    Float64 geospatial_lat_min -78.375;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.875;\n" +
"    Float64 geospatial_lon_min 0.125;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Created by NASA Goddard Space Flight Center under the NASA REASoN CAN: A Cross-Calibrated, Multi-Platform Ocean Surface Wind Velocity Product for Meteorological and Oceanographic Applications\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

        
//        + " http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/M09/\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"tabledap/testEDDTableFromHyraxFiles.das\";\n" +
"    String infoUrl \"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/M09/.html\";\n" +
"    String institution \"NASA JPL\";\n" +
"    String keywords \"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Atmosphere > Atmospheric Winds > Wind Stress,\n" +
"atlas, atmosphere, atmospheric, component, derived, downward, eastward, eastward_wind, flk, jpl, level, meters, nasa, northward, northward_wind, number, observations, oceanography, physical, physical oceanography, pseudostress, speed, statistics, stress, surface, surface_downward_eastward_stress, surface_downward_northward_stress, time, u-component, u-wind, v-component, v-wind, v1.1, wind, wind_speed, winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 78.375;\n" +
"    String sourceUrl \"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/M09/\";\n" +
"    Float64 Southernmost_Northing -78.375;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"Time average of level3.0 products for the period: 1987-09-08 to 1987-09-12\";\n" +
"    String time_coverage_end \"1987-09-28T00:00:00Z\";\n" +
"    String time_coverage_start \"1987-09-03T00:00:00Z\";\n" +
"    String title \"Atlas FLK v1.1 derived surface winds (level 3.5)\";\n" +
"    Float64 Westernmost_Easting 0.125;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
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
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Float32 uwnd;\n" +
"    Float32 vwnd;\n" +
"    Float32 wspd;\n" +
"    Float32 upstr;\n" +
"    Float32 vpstr;\n" +
"    Float32 nobs;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        //*** test make data files
        String2.log("\n****************** EDDTableFromHyraxFiles.testWcosTemp make DATA FILES\n");       

        //.csv    for one lat,lon,time
        try {
        userDapQuery = "longitude,latitude,time,uwnd,vwnd,wspd,upstr,vpstr,nobs&longitude>=220&longitude<=220.5&latitude>=40&latitude<=40.5&time>=1987-09-03&time<=1987-09-28";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_stationList", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,time,uwnd,vwnd,wspd,upstr,vpstr,nobs\n" +
"degrees_east,degrees_north,UTC,m/s,m/s,m/s,m2/s2,m2/s2,count\n" +
"220.125,40.125,1987-09-03T00:00:00Z,-4.080449,-2.3835683,4.9441504,-20.600622,-12.390893,6.0\n" +
"220.375,40.125,1987-09-03T00:00:00Z,-4.194897,-3.0107427,5.30695,-22.767502,-16.93829,8.0\n" +
"220.125,40.375,1987-09-03T00:00:00Z,-3.9186962,-2.346945,4.769045,-19.07465,-11.99414,6.0\n" +
"220.375,40.375,1987-09-03T00:00:00Z,-3.8927546,-2.5865226,4.833136,-19.10517,-13.306476,6.0\n" +
"220.125,40.125,1987-09-08T00:00:00Z,1.6678874,-2.6307757,5.0425754,14.557773,-14.618812,5.0\n" +
"220.375,40.125,1987-09-08T00:00:00Z,1.7289263,-2.7055483,5.1432896,15.351278,-15.320759,5.0\n" +
"220.125,40.375,1987-09-08T00:00:00Z,1.6633095,-2.5712628,5.0608873,14.374657,-14.679851,5.0\n" +
"220.375,40.375,1987-09-08T00:00:00Z,1.7029848,-2.6567173,5.1478677,14.893487,-15.381798,5.0\n" +
"220.125,40.125,1987-09-13T00:00:00Z,3.212171,-0.6378563,7.653132,32.442165,-7.477263,8.0\n" +
"220.375,40.125,1987-09-13T00:00:00Z,3.2533722,-0.8224989,7.526095,32.350605,-9.186352,8.0\n" +
"220.125,40.375,1987-09-13T00:00:00Z,3.5280473,-1.7747054,7.6210866,36.043457,-18.220106,7.0\n" +
"220.375,40.375,1987-09-13T00:00:00Z,3.5936642,-1.9257767,7.5295286,36.196056,-19.471403,7.0\n" +
"220.125,40.125,1987-09-18T00:00:00Z,6.5067444,9.657877,11.65652,76.20704,112.647255,2.0\n" +
"220.375,40.125,1987-09-18T00:00:00Z,6.2396994,9.49765,11.372689,71.32393,108.13038,2.0\n" +
"220.125,40.375,1987-09-18T00:00:00Z,6.590673,9.370994,11.49057,75.932365,107.61154,2.0\n" +
"220.375,40.375,1987-09-18T00:00:00Z,6.3495693,9.279436,11.273119,71.87328,104.5596,2.0\n" +
"220.125,40.125,1987-09-23T00:00:00Z,1.9959713,-7.846548,8.204771,15.625954,-66.83757,3.0\n" +
"220.375,40.125,1987-09-23T00:00:00Z,1.8693157,-7.852652,8.200193,14.405175,-66.80705,3.0\n" +
"220.125,40.375,1987-09-23T00:00:00Z,0.7690899,-5.722395,6.9778895,9.399987,-49.563572,4.0\n" +
"220.375,40.375,1987-09-23T00:00:00Z,0.67753154,-5.797168,7.0122237,8.637002,-50.20448,4.0\n" +
"220.125,40.125,1987-09-28T00:00:00Z,1.1505829,8.963559,11.136927,6.8363547,105.841415,6.0\n" +
"220.375,40.125,1987-09-28T00:00:00Z,1.9196727,8.066288,10.433072,13.214917,90.97845,7.0\n" +
"220.125,40.375,1987-09-28T00:00:00Z,1.2467191,8.910151,11.095725,8.6064825,104.43752,6.0\n" +
"220.375,40.375,1987-09-28T00:00:00Z,1.2406152,8.798755,10.894297,9.003235,100.80571,6.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        //.csv    few variables,  for small lat,lon range,  one time
        userDapQuery = "longitude,latitude,time,upstr,vpstr&longitude>=220&longitude<=221&latitude>=40&latitude<=41&time>=1987-09-28&time<=1987-09-28";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1StationGTLT", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"longitude,latitude,time,upstr,vpstr\n" +
"degrees_east,degrees_north,UTC,m2/s2,m2/s2\n" +
"220.125,40.125,1987-09-28T00:00:00Z,6.8363547,105.841415\n" +
"220.375,40.125,1987-09-28T00:00:00Z,13.214917,90.97845\n" +
"220.625,40.125,1987-09-28T00:00:00Z,10.468168,92.84013\n" +
"220.875,40.125,1987-09-28T00:00:00Z,10.834401,89.54404\n" +
"220.125,40.375,1987-09-28T00:00:00Z,8.6064825,104.43752\n" +
"220.375,40.375,1987-09-28T00:00:00Z,9.003235,100.80571\n" +
"220.625,40.375,1987-09-28T00:00:00Z,12.696087,92.68754\n" +
"220.875,40.375,1987-09-28T00:00:00Z,13.062321,89.87975\n" +
"220.125,40.625,1987-09-28T00:00:00Z,9.9798565,103.399864\n" +
"220.375,40.625,1987-09-28T00:00:00Z,10.468168,100.19532\n" +
"220.625,40.625,1987-09-28T00:00:00Z,14.771409,92.53494\n" +
"220.875,40.625,1987-09-28T00:00:00Z,15.320759,90.15443\n" +
"220.125,40.875,1987-09-28T00:00:00Z,11.139596,102.72843\n" +
"220.375,40.875,1987-09-28T00:00:00Z,11.811024,99.89013\n" +
"220.625,40.875,1987-09-28T00:00:00Z,16.785692,92.4739\n" +
"220.875,40.875,1987-09-28T00:00:00Z,17.4266,90.368065\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        /* */
    }



    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {

        //usually run
/* */
        testGenerateDatasetsXml();
        testJpl(true);   //deleteCachedInfo (the file info, not the data files)
        testJpl(false);  

    }
}

