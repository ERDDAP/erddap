/* 
 * EDDTableFromInvalidCRAFiles Copyright 2018, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.PAType;
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

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


/** 
 * This class represents a table of data from a collection of 
 * invalid NCEI Contiguous Ragged Array Files.
 * These files have multiple row_size variables, each with a sample_dimension attribute. 
 *   The files are non-CF-standard files because the multiple sample (obs) dimensions 
 *   are to be decoded and related to each other with this additional rule and promise 
 *   that is not part of the CF DSG specification:
 *   "you can associate a given e.g., temperature value (temp_obs dimension) 
 *   with a given depth value (z_obs dimension, the dimension with the most values), because: 
 *   the temperature row_size (for a given cast) will be either 0 or equal to the
 *   corresponding depth row_size (for that cast) (that's the rule).
 *   So, if the temperature row_size isn't 0, then the n temperature values for that cast relate 
 *   directly to the n depth values for that cast (that's the promise)."
 *
 * <p>Also, the Principal_Investigator row_size variable doesn't have
 * a sample_dimension attribute and doesn't follow the above rule.
 *
 * <p>Examples from https://data.nodc.noaa.gov/thredds/catalog/ncei/wod/
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2018-05-08
 */
public class EDDTableFromInvalidCRAFiles extends EDDTableFromFiles { 

    /**
     * This returns the default value for standardizeWhat for this subclass.
     * See Attributes.unpackVariable for options.
     * The default was chosen to mimic the subclass' behavior from
     * before support for standardizeWhat options was added.
     */
    public int defaultStandardizeWhat() {return DEFAULT_STANDARDIZEWHAT; } 
    public static int DEFAULT_STANDARDIZEWHAT = 0;


    /** 
     * The constructor just calls the super constructor. 
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
     * @param tFileDir the base URL or file directory. 
     *    See https://www.unidata.ucar.edu/software/netcdf-java/v4.2/javadoc/index.html
     *    FeatureDatasetFactoryManager open().
     *    This may be a
     *    <ul>
     *    <li> local file (.nc or compatible)
     *    <li> thredds catalog#dataset (with a thredds: prefix)
     *    <li> cdmremote dataset (with an cdmremote: prefix)
     *    <li> collection dataset (with a collection: prefix)
     *    </ul>
     * @param tSortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     *
     */
    public EDDTableFromInvalidCRAFiles(
        String tDatasetID, String tAccessibleTo, String tGraphsAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, String tCharset, 
        String tSkipHeaderToRegex, String tSkipLinesRegex,
        int tColumnNamesRow, int tFirstDataRow, String tColumnSeparator,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles,
        boolean tRemoveMVRows, int tStandardizeWhat, int tNThreads, 
        String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex,
        String tAddVariablesWhere) 
        throws Throwable {

        super("EDDTableFromInvalidCRAFiles",  
            tDatasetID, tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, 
            tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tSkipHeaderToRegex, tSkipLinesRegex,
            tColumnNamesRow, tFirstDataRow, tColumnSeparator, //irrelevant
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, //irrelevant
            tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows, tStandardizeWhat, 
            tNThreads, tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex,
            tAddVariablesWhere);
    }

    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
     * 
     * <p>For this class, sortedColumn is always time. See constructor.
     *
     * @throws an exception if too much data.
     *  This won't throw an exception if no data.
     */
    public Table lowGetSourceDataFromFile(String tFileDir, String tFileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        StringArray sourceConVars, StringArray sourceConOps, StringArray sourceConValues,
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        //FUTURE: when !mustGetData, much better if get metadata quickly, 
        //e.g., via: table.readNcMetadata, but can't because readInvalidCRA
        //adds some global attributes (e.g., cdm_profile_variables)
        //when it reads the file (see testGenerateDatasetsXml).

        //get the data from the source file
        Table table = new Table();
        String decompFullName = FileVisitorDNLS.decompressIfNeeded(
            tFileDir + tFileName, fileDir, decompressedDirectory(), 
            EDStatic.decompressedCacheMaxGB, true); //reuseExisting
        table.readInvalidCRA(decompFullName, sourceDataNames, 
            standardizeWhat,
            sourceConVars, sourceConOps, sourceConValues);

        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromInvalidCRAFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private .nc files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *    If null or "", it is generated to catch the same extension as the sampleFileName
     *    (usually ".*\\.nc").
     * @param sampleFileName the full file name of one of the files in the collection
     * @param tReloadEveryNMinutes  e.g., 10080 for weekly
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortFilesBySourceNames   This is useful, because it ultimately determines default results order.
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, 
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        int tStandardizeWhat, String tCacheFromUrl, 
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** EDDTableFromInvalidCRAFiles.generateDatasetsXml" +
            "\nfileDir=" + tFileDir + " fileNameRegex=" + tFileNameRegex + 
            "\nsampleFileName=" + sampleFileName +
            "\nextract pre=" + tPreExtractRegex + " post=" + tPostExtractRegex + 
            " regex=" + tExtractRegex + " colNameForExtract=" + tColumnNameForExtract +
            "\nsortFilesBy=" + tSortFilesBySourceNames + 
            "\ninfoUrl=" + tInfoUrl + 
            "\ninstitution=" + tInstitution +
            "\nsummary=" + tSummary +
            "\ntitle=" + tTitle +
            "\nexternalAddGlobalAttributes=" + externalAddGlobalAttributes);
        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        tFileNameRegex = String2.isSomething(tFileNameRegex)? 
            tFileNameRegex.trim() : ".*";
        if (String2.isRemote(tCacheFromUrl)) 
            FileVisitorDNLS.sync(tCacheFromUrl, tFileDir, tFileNameRegex,
                true, ".*", false); //not fullSync
        tColumnNameForExtract = String2.isSomething(tColumnNameForExtract)?
            tColumnNameForExtract.trim() : "";
        //tSortedColumnSourceName = String2.isSomething(tSortedColumnSourceName)?
        //    tSortedColumnSourceName.trim() : "";
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis
        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
        String2.log(NcHelper.ncdump(sampleFileName, "-h"));

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        tStandardizeWhat = tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE?
            DEFAULT_STANDARDIZEWHAT : tStandardizeWhat;
        dataSourceTable.readInvalidCRA(sampleFileName, null, tStandardizeWhat,
            null, null, null);
        double maxTimeES = Double.NaN;
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            PrimitiveArray sourcePA = dataSourceTable.getColumn(c);
            PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), sourceAtts, null, colName, 
                destPA.elementType() != PAType.STRING, //tryToAddStandardName
                destPA.elementType() != PAType.STRING, //addColorBarMinMax
                true); //tryToFindLLAT
            dataAddTable.addColumn(c, colName, destPA, addAtts);

            //maxTimeES
            String tUnits = sourceAtts.getString("units");
            if (!Double.isFinite(maxTimeES) && Calendar2.isTimeUnits(tUnits)) {
                try {
                    if (Calendar2.isNumericTimeUnits(tUnits)) {
                        double tbf[] = Calendar2.getTimeBaseAndFactor(tUnits); //throws exception
                        maxTimeES = Calendar2.unitsSinceToEpochSeconds(
                            tbf[0], tbf[1], destPA.getDouble(destPA.size() - 1));
                    } else { //string time units
                        maxTimeES = Calendar2.tryToEpochSeconds(destPA.getString(destPA.size() - 1)); //NaN if trouble
                    }
                } catch (Throwable t) {
                    String2.log("caught while trying to get maxTimeES: " + 
                        MustBe.throwableToString(t));
                }
            }

            //add missing_value and/or _FillValue if needed
            addMvFvAttsIfNeeded(colName, sourcePA, sourceAtts, addAtts); //sourcePA since strongly typed

        }
        //String2.log("SOURCE COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());
        //String2.log("DEST   COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());

        //globalAttributes
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", 
            "(" + (String2.isRemote(tFileDir)? "remote" : "local") + " files)");

        //tryToFindLLAT
        tryToFindLLAT(dataSourceTable, dataAddTable);

        //find altitude_proxy
        String ft = dataSourceTable.globalAttributes().getString("featureType");
        if (ft != null && ft.toLowerCase().equals("profile") &&
            dataAddTable.findColumnNumber("depth") < 0 &&
            dataAddTable.findColumnNumber("altitude") < 0) {
            int col = dataAddTable.findColumnNumber("z");
            if (col < 0)
                col = dataAddTable.findColumnNumber("Pressure");
            if (col < 0)
                col = dataAddTable.findColumnNumber("pressure");
            if (col >= 0) 
                dataAddTable.globalAttributes().add("cdm_altitude_proxy", dataAddTable.getColumnName(col));
        }

        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                dataSourceTable.globalAttributes().getString("cdm_data_type"),
                tFileDir, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //subsetVariables  (or get from outer variables in some file types?)
        if (dataSourceTable.globalAttributes().getString("subsetVariables") == null &&
               dataAddTable.globalAttributes().getString("subsetVariables") == null) 
            dataAddTable.globalAttributes().add("subsetVariables",
                suggestSubsetVariables(dataSourceTable, dataAddTable, false));

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            //no units or standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);
        }

        //useMaxTimeES
        String tTestOutOfDate = EDD.getAddOrSourceAtt(
            dataSourceTable.globalAttributes(), 
            dataAddTable.globalAttributes(), "testOutOfDate", null);
        if (Double.isFinite(maxTimeES) && !String2.isSomething(tTestOutOfDate)) {
            tTestOutOfDate = suggestTestOutOfDate(maxTimeES);
            if (String2.isSomething(tTestOutOfDate))
                dataAddTable.globalAttributes().set("testOutOfDate", tTestOutOfDate);
        }

        //write the information
        StringBuilder sb = new StringBuilder();
        String suggestedRegex = (tFileNameRegex == null || tFileNameRegex.length() == 0)? 
            ".*\\" + File2.getExtension(sampleFileName) :
            tFileNameRegex;
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = tColumnNameForExtract;
        sb.append(
            "<dataset type=\"EDDTableFromInvalidCRAFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + suggestedRegex) +  //dirs can't be made public
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            (String2.isUrl(tCacheFromUrl)? 
              "    <cacheFromUrl>" + XML.encodeAsXML(tCacheFromUrl) + "</cacheFromUrl>\n" :
              "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + "</updateEveryNMillis>\n") +  
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(suggestedRegex) + "</fileNameRegex>\n" +
            "    <recursive>true</recursive>\n" +
            "    <pathRegex>.*</pathRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <standardizeWhat>" + tStandardizeWhat + "</standardizeWhat>\n" +
            (String2.isSomething(tColumnNameForExtract)? //Discourage Extract. Encourage sourceName=***fileName,...
              "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
              "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
              "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
              "    <columnNameForExtract>" + XML.encodeAsXML(tColumnNameForExtract) + "</columnNameForExtract>\n" : "")+
            "    <sortFilesBySourceNames>" + XML.encodeAsXML(tSortFilesBySourceNames) + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 2 params: includeDataType, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
        
    }


    /**
     * testGenerateDatasetsXml.
     * This doesn't test suggestTestOutOfDate, except that for old data
     * it doesn't suggest anything.
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();
        //debugMode = true;

        //public static String generateDatasetsXml(
        //    String tFileDir, String tFileNameRegex, String sampleFileName, 
        //    int tReloadEveryNMinutes,
        //    String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        //    String tColumnNameForExtract, 
        //    String tSortFilesBySourceNames, 
        //    String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        //    Attributes externalAddGlobalAttributes) throws Throwable {

        String results = generateDatasetsXml(
            EDStatic.unitTestBigDataDir + "nccf/wod/", "wod_drb_.*\\.nc",
            "",
            1440,
            "", "", "", "", //just for test purposes; station is already a column in the file
            "time", 
            "", "", "", "", 
            -1, null, //defaultStandardizeWhat
            null) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromInvalidCRAFiles",
            EDStatic.unitTestBigDataDir + "nccf/wod/", "wod_drb_.*\\.nc",
            "",
            "1440",
            "", "", "", "", //just for test purposes; station is already a column in the file
            "time", 
            "", "", "", "", "", 
            "-1", ""}, //defaultStandardizeWhat
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<dataset type=\"EDDTableFromInvalidCRAFiles\" datasetID=\"wod_d3d4_46bc_cdfa\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTestBig/nccf/wod/</fileDir>\n" +
"    <fileNameRegex>wod_drb_.*\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"cdm_data_type\">Profile</att>\n" +
"        <att name=\"cdm_profile_variables\">country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Platform, Institute, Orig_Stat_Num, dataset, real_time, Ocean_Vehicle, Temperature_WODprofileflag, Temperature_Instrument, Salinity_WODprofileflag, Salinity_Instrument, Oxygen_WODprofileflag, Oxygen_Original_units, Primary_Investigator, Primary_Investigator_VAR</att>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"creator_email\">OCLhelp@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Ocean Climate Lab/NCEI</att>\n" +
"        <att name=\"creator_url\">http://www.nodc.noaa.gov</att>\n" +
"        <att name=\"crs_epsg_code\">EPSG:4326</att>\n" +
"        <att name=\"crs_grid_mapping_name\">latitude_longitude</att>\n" +
"        <att name=\"crs_inverse_flattening\" type=\"float\">298.25723</att>\n" +
"        <att name=\"crs_longitude_of_prime_meridian\" type=\"float\">0.0</att>\n" +
"        <att name=\"crs_semi_major_axis\" type=\"float\">6378137.0</att>\n" +
"        <att name=\"date_created\">2018-03-25</att>\n" +
"        <att name=\"date_modified\">2018-03-25</att>\n" +
"        <att name=\"featureType\">Profile</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"float\">87.5248</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"float\">68.572</att>\n" +
"        <att name=\"geospatial_lat_resolution\">point</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"float\">-11.6521</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"float\">-153.0565</att>\n" +
"        <att name=\"geospatial_lon_resolution\">point</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"float\">780.2255</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"float\">5.0467224</att>\n" +
"        <att name=\"geospatial_vertical_positive\">down</att>\n" +
"        <att name=\"geospatial_vertical_units\">meters</att>\n" +
"        <att name=\"grid_mapping_epsg_code\">EPSG:4326</att>\n" +
"        <att name=\"grid_mapping_inverse_flattening\" type=\"float\">298.25723</att>\n" +
"        <att name=\"grid_mapping_longitude_of_prime_meridian\" type=\"float\">0.0</att>\n" +
"        <att name=\"grid_mapping_name\">latitude_longitude</att>\n" +
"        <att name=\"grid_mapping_semi_major_axis\" type=\"float\">6378137.0</att>\n" +
"        <att name=\"id\">/nodc/data/oc5.clim.0/wod_update_nc/2006/wod_drb_2006.nc</att>\n" +
"        <att name=\"institution\">National Centers for Environmental Information (NCEI), NOAA</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.nodc</att>\n" +
"        <att name=\"project\">World Ocean Database</att>\n" +
"        <att name=\"publisher_email\">NODC.Services@noaa.gov</att>\n" +
"        <att name=\"publisher_name\">US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION</att>\n" +
"        <att name=\"publisher_url\">http://www.nodc.noaa.gov</att>\n" +
"        <att name=\"references\">World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf</att>\n" +
"        <att name=\"source\">World Ocean Database</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v41</att>\n" +
"        <att name=\"summary\">Data for multiple casts from the World Ocean Database</att>\n" +
"        <att name=\"time_coverage_end\">2006-12-31</att>\n" +
"        <att name=\"time_coverage_start\">2006-01-01</att>\n" +
"        <att name=\"title\">World Ocean Database - Multi-cast file</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://www.nodc.noaa.gov</att>\n" +
"        <att name=\"history\">World Ocean Database</att>\n" +
"        <att name=\"infoUrl\">https://www.nodc.noaa.gov</att>\n" +
"        <att name=\"institution\">NCEI, NOAA</att>\n" +
"        <att name=\"keywords\">Access_no, accession, below, cast, center, centers, chemistry, country, cruise, data, database, dataset, date, density, depth, depth status_flag, dissolved, dissolved o2, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Oxygen, Earth Science &gt; Oceans &gt; Ocean Pressure &gt; Water Pressure, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Water Temperature, Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity, environmental, figures, file, flag, fraction, GMT_time, identifier, information, institute, instrument, investigator, latitude, level, longitude, multi, multi-cast, name, national, ncei, nesdis, noaa, nodc, number, O2, observation, ocean, Ocean_Vehicle, oceanographic, oceans, Orig_Stat_Num, original, originators, originators_cruise_identifier, oxygen, Oxygen_Original_units, Oxygen_sigfigs, Oxygen_WODflag, Oxygen_WODprofileflag, platform, practical, pressure, Pressure_sigfigs, primary, Primary_Investigator, Primary_Investigator_VAR, profile, quality, real, real_time, responsible, salinity, Salinity_Instrument, Salinity_sigfigs, Salinity_WODflag, Salinity_WODprofileflag, science, sea, sea_water_practical_salinity, sea_water_pressure, sea_water_salinity status_flag, sea_water_temperature, sea_water_temperature status_flag, seawater, significant, station, statistics, status, surface, temperature, Temperature_Instrument, Temperature_sigfigs, Temperature_WODflag, Temperature_WODprofileflag, time, unique, units, vehicle, volume, volume_fraction_of_oxygen_in_sea_water, volume_fraction_of_oxygen_in_sea_water status_flag, water, wod, WOD_cruise_identifier, wod_unique_cast, world, z_sigfigs, z_WODflag</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"publisher_type\">institution</att>\n" +
"        <att name=\"publisher_url\">https://www.nodc.noaa.gov</att>\n" +
"        <att name=\"references\">World Ocean Database 2013. URL:https://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"subsetVariables\">dataset, Temperature_WODprofileflag, Salinity_WODprofileflag, Oxygen_WODprofileflag, z_WODflag, Temperature_WODflag</att>\n" +
"        <att name=\"summary\">World Ocean Database - Multi-cast file. Data for multiple casts from the World Ocean Database</att>\n" +
"        <att name=\"title\">World Ocean Database, Multi-cast file</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>country</sourceName>\n" +
"        <destinationName>country</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Country</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WOD_cruise_identifier</sourceName>\n" +
"        <destinationName>WOD_cruise_identifier</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">two byte country code + WOD cruise number (unique to country code)</att>\n" +
"            <att name=\"long_name\">WOD_cruise_identifier</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>originators_cruise_identifier</sourceName>\n" +
"        <destinationName>originators_cruise_identifier</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Originators Cruise Identifier</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wod_unique_cast</sourceName>\n" +
"        <destinationName>wod_unique_cast</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"cf_role\">profile_id</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Wod Unique Cast</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">days since 1770-01-01 00:00:00 UTC</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"units\">days since 1770-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>date</sourceName>\n" +
"        <destinationName>date</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">YYYYMMDD</att>\n" +
"            <att name=\"long_name\">date</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>GMT_time</sourceName>\n" +
"        <destinationName>GMT_time</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">GMT_time</att>\n" +
"            <att name=\"units\">hours</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Access_no</sourceName>\n" +
"        <destinationName>Access_no</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">-99999</att>\n" +
"            <att name=\"comment\">used to find original data at NODC</att>\n" +
"            <att name=\"long_name\">NODC_accession_number</att>\n" +
"            <att name=\"units_wod\">NODC_code</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Platform</sourceName>\n" +
"        <destinationName>Platform</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">name of platform from which measurements were taken</att>\n" +
"            <att name=\"long_name\">Platform_name</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Institute</sourceName>\n" +
"        <destinationName>Institute</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">name of institute which collected data</att>\n" +
"            <att name=\"long_name\">Responsible_institute</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Orig_Stat_Num</sourceName>\n" +
"        <destinationName>Orig_Stat_Num</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">-99999</att>\n" +
"            <att name=\"comment\">number assigned to a given station by data originator</att>\n" +
"            <att name=\"long_name\">Originators_Station_Number</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>dataset</sourceName>\n" +
"        <destinationName>dataset</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">WOD_dataset</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>real_time</sourceName>\n" +
"        <destinationName>real_time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">timeliness and quality status</att>\n" +
"            <att name=\"long_name\">real_time_data</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Real Time Data</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Ocean_Vehicle</sourceName>\n" +
"        <destinationName>Ocean_Vehicle</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">Ocean_vehicle</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Ocean Vehicle</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature_WODprofileflag</sourceName>\n" +
"        <destinationName>Temperature_WODprofileflag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"flag_meanings\">accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n" +
"            <att name=\"long_name\">WOD_profile_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature_Instrument</sourceName>\n" +
"        <destinationName>Temperature_Instrument</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">Device used for measurement</att>\n" +
"            <att name=\"long_name\">Instrument</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Salinity_WODprofileflag</sourceName>\n" +
"        <destinationName>Salinity_WODprofileflag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"flag_meanings\">accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n" +
"            <att name=\"long_name\">WOD_profile_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Salinity_Instrument</sourceName>\n" +
"        <destinationName>Salinity_Instrument</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">Device used for measurement</att>\n" +
"            <att name=\"long_name\">Instrument</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" + //so standard_name or units because String data
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Oxygen_WODprofileflag</sourceName>\n" +
"        <destinationName>Oxygen_WODprofileflag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"flag_meanings\">accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n" +
"            <att name=\"long_name\">WOD_profile_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Oxygen_Original_units</sourceName>\n" +
"        <destinationName>Oxygen_Original_units</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">Units originally used: coverted to standard units</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Dissolved O2</att>\n" +
"            <att name=\"long_name\">Oxygen Original Units</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Primary_Investigator</sourceName>\n" +
"        <destinationName>Primary_Investigator</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Primary Investigator</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Primary_Investigator_VAR</sourceName>\n" +
"        <destinationName>Primary_Investigator_VAR</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Primary Investigator VAR</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>z</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ancillary_variables\">z_sigfigs z_WODflag</att>\n" +
"            <att name=\"long_name\">depth_below_sea_surface</att>\n" +
"            <att name=\"positive\">down</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n" +
"            <att name=\"colorBarPalette\">TopographyDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Depth Below Sea Surface</att>\n" +
"            <att name=\"source_name\">z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>z_WODflag</sourceName>\n" +
"        <destinationName>z_WODflag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"flag_meanings\">accepted duplicate_or_inversion density_inversion</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1 2</att>\n" +
"            <att name=\"long_name\">WOD_depth_level_flag</att>\n" +
"            <att name=\"standard_name\">depth status_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">2.5</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>z_sigfigs</sourceName>\n" +
"        <destinationName>z_sigfigs</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">depth significant figures   </att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"long_name\">depth significant figures</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature</sourceName>\n" +
"        <destinationName>Temperature</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-1.0E10</att>\n" +
"            <att name=\"ancillary_variables\">Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag</att>\n" +
"            <att name=\"coordinates\">time lat lon z</att>\n" +
"            <att name=\"grid_mapping\">crs</att>\n" +
"            <att name=\"long_name\">sea_water_temperature</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Sea Water Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature_sigfigs</sourceName>\n" +
"        <destinationName>Temperature_sigfigs</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">sea_water_temperature significant_figures</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature_WODflag</sourceName>\n" +
"        <destinationName>Temperature_WODflag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"flag_meanings\">accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n" +
"            <att name=\"long_name\">WOD_observation_flag</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature status_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Salinity</sourceName>\n" +
"        <destinationName>Salinity</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-1.0E10</att>\n" +
"            <att name=\"ancillary_variables\">Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag</att>\n" +
"            <att name=\"coordinates\">time lat lon z</att>\n" +
"            <att name=\"grid_mapping\">crs</att>\n" +
"            <att name=\"long_name\">sea_water_salinity</att>\n" +
"            <att name=\"standard_name\">sea_water_salinity</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"long_name\">Sea Water Salinity</att>\n" +
"            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" +
"            <att name=\"units\">PSU</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Salinity_sigfigs</sourceName>\n" +
"        <destinationName>Salinity_sigfigs</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">sea_water_salinity significant_figures</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" +
"            <att name=\"units\">PSU</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Salinity_WODflag</sourceName>\n" +
"        <destinationName>Salinity_WODflag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"flag_meanings\">accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n" +
"            <att name=\"long_name\">WOD_observation_flag</att>\n" +
"            <att name=\"standard_name\">sea_water_salinity status_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Pressure</sourceName>\n" +
"        <destinationName>Pressure</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-1.0E10</att>\n" +
"            <att name=\"ancillary_variables\">Pressure_sigfigs Pressure_WODflag Pressure_WODprofileflag</att>\n" +
"            <att name=\"coordinates\">time lat lon z</att>\n" +
"            <att name=\"grid_mapping\">crs</att>\n" +
"            <att name=\"long_name\">sea_water_pressure</att>\n" +
"            <att name=\"standard_name\">sea_water_pressure</att>\n" +
"            <att name=\"units\">dbar</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Sea Water Pressure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Pressure_sigfigs</sourceName>\n" +
"        <destinationName>Pressure_sigfigs</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">sea_water_pressure significant_figures</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Oxygen</sourceName>\n" +
"        <destinationName>Oxygen</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-1.0E10</att>\n" +
"            <att name=\"ancillary_variables\">Oxygen_sigfigs Oxygen_WODflag Oxygen_WODprofileflag</att>\n" +
"            <att name=\"coordinates\">time lat lon z</att>\n" +
"            <att name=\"grid_mapping\">crs</att>\n" +
"            <att name=\"long_name\">volume_fraction_of_oxygen_in_sea_water</att>\n" +
"            <att name=\"standard_name\">volume_fraction_of_oxygen_in_sea_water</att>\n" +
"            <att name=\"units\">ml/l</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Dissolved O2</att>\n" +
"            <att name=\"long_name\">Volume Fraction Of Oxygen In Sea Water</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Oxygen_sigfigs</sourceName>\n" +
"        <destinationName>Oxygen_sigfigs</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">volume_fraction_of_oxygen_in_sea_water significant_figures</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"missing_value\" type=\"byte\">127</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Oxygen_WODflag</sourceName>\n" +
"        <destinationName>Oxygen_WODflag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"flag_meanings\">accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n" +
"            <att name=\"long_name\">WOD_observation_flag</att>\n" +
"            <att name=\"standard_name\">volume_fraction_of_oxygen_in_sea_water status_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"missing_value\" type=\"byte\">127</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
        //    expected, "");

        String tDatasetID = "wod_d3d4_46bc_cdfa";
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), "World Ocean Database, Multi-cast file", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Platform, Institute, Orig_Stat_Num, dataset, real_time, Ocean_Vehicle, Temperature_WODprofileflag, Temperature_Instrument, Salinity_WODprofileflag, Salinity_Instrument, Oxygen_WODprofileflag, Oxygen_Original_units, Primary_Investigator, Primary_Investigator_VAR, depth, z_WODflag, z_sigfigs, Temperature, Temperature_sigfigs, Temperature_WODflag, Salinity, Salinity_sigfigs, Salinity_WODflag, Pressure, Pressure_sigfigs, Oxygen, Oxygen_sigfigs, Oxygen_WODflag", 
            "");


    }



    /**
     *
     * @throws Throwable if trouble
     */
    public static void testBasic() throws Throwable {
        String2.log("\n****************** EDDTableFromInvalidCRAFiles.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        Table table;
        String testCacheDir = EDStatic.fullTestCacheDirectory;

        String id = "testInvalidCRAFiles";
        deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //.dds    
        tName = eddTable.makeNewFileForDapQuery(null, null, "", 
            testCacheDir, eddTable.className() + "_wod", ".dds"); 
        results = String2.directReadFrom88591File(testCacheDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String country;\n" +
"    String WOD_cruise_identifier;\n" +
"    String originators_cruise_identifier;\n" +
"    Int32 wod_unique_cast;\n" +
"    Float32 latitude;\n" +
"    Float32 longitude;\n" +
"    Float64 time;\n" +
"    Int32 date;\n" +
"    Float32 GMT_time;\n" +
"    Int32 Access_no;\n" +
"    String Platform;\n" +
"    String Institute;\n" +
"    Int32 Orig_Stat_Num;\n" +
"    String dataset;\n" +
"    String real_time;\n" +
"    String Ocean_Vehicle;\n" +
"    Byte Temperature_WODprofileflag;\n" +
"    String Temperature_Instrument;\n" +
"    Byte Salinity_WODprofileflag;\n" +
"    String Salinity_Instrument;\n" +
"    Byte Oxygen_WODprofileflag;\n" +
"    String Oxygen_Original_units;\n" +
"    String Primary_Investigator;\n" +
"    String Primary_Investigator_VAR;\n" +
"    Float32 depth;\n" +
"    Byte z_WODflag;\n" +
"    Byte z_sigfigs;\n" +
"    Float32 Temperature;\n" +
"    Byte Temperature_sigfigs;\n" +
"    Byte Temperature_WODflag;\n" +
"    Float32 Salinity;\n" +
"    Byte Salinity_sigfigs;\n" +
"    Byte Salinity_WODflag;\n" +
"    Float32 Pressure;\n" +
"    Byte Pressure_sigfigs;\n" +
"    Float32 Oxygen;\n" +
"    Byte Oxygen_sigfigs;\n" +
"    Byte Oxygen_WODflag;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        //.das    
        tName = eddTable.makeNewFileForDapQuery(null, null, "", 
            testCacheDir, eddTable.className() + "_wod", ".das"); 
        results = String2.directReadFrom88591File(testCacheDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  country {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Country\";\n" +
"  }\n" +
"  WOD_cruise_identifier {\n" +
"    String comment \"two byte country code + WOD cruise number (unique to country code)\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"WOD_cruise_identifier\";\n" +
"  }\n" +
"  originators_cruise_identifier {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Originators Cruise Identifier\";\n" +
"  }\n" +
"  wod_unique_cast {\n" +
"    Int32 _FillValue 2147483647;\n" +
"    Int32 actual_range 10899854, 17921929;\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Wod Unique Cast\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 68.572, 89.625;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -165.4138, 149.8134;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1045376e+9, 1.167588002637e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  date {\n" +
"    Int32 _FillValue 2147483647;\n" +
"    Int32 actual_range 20050101, 20061231;\n" +
"    String comment \"YYYYMMDD\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"date\";\n" +
"  }\n" +
"  GMT_time {\n" +
"    Float32 actual_range 0.0, 23.0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"GMT_time\";\n" +
"    String units \"hours\";\n" +
"  }\n" +
"  Access_no {\n" +
"    Int32 _FillValue -99999;\n" +
"    Int32 actual_range -504834352, 2026234576;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"used to find original data at NODC\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"NODC_accession_number\";\n" +
"    String units_wod \"NODC_code\";\n" +
"  }\n" +
"  Platform {\n" +
"    String comment \"name of platform from which measurements were taken\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Platform_name\";\n" +
"  }\n" +
"  Institute {\n" +
"    String comment \"name of institute which collected data\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Responsible_institute\";\n" +
"  }\n" +
"  Orig_Stat_Num {\n" +
"    Int32 _FillValue -99999;\n" +
"    Int32 actual_range -504849023, 2026222775;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"number assigned to a given station by data originator\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Originators_Station_Number\";\n" +
"  }\n" +
"  dataset {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"WOD_dataset\";\n" +
"  }\n" +
"  real_time {\n" +
"    String comment \"timeliness and quality status\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Real Time Data\";\n" +
"  }\n" +
"  Ocean_Vehicle {\n" +
"    String comment \"Ocean_vehicle\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Ocean Vehicle\";\n" +
"  }\n" +
"  Temperature_WODprofileflag {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 0, 0;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String flag_meanings \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"WOD_profile_flag\";\n" +
"  }\n" +
"  Temperature_Instrument {\n" +
"    String comment \"Device used for measurement\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Instrument\";\n" +
"  }\n" +
"  Salinity_WODprofileflag {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 0, 0;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String flag_meanings \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"WOD_profile_flag\";\n" +
"  }\n" +
"  Salinity_Instrument {\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String comment \"Device used for measurement\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Instrument\";\n" +
"    String standard_name \"sea_water_practical_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
"  Oxygen_WODprofileflag {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 0, 0;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String flag_meanings \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"WOD_profile_flag\";\n" +
"  }\n" +
"  Oxygen_Original_units {\n" +
"    String comment \"Units originally used: coverted to standard units\";\n" +
"    String ioos_category \"Dissolved O2\";\n" +
"    String long_name \"Oxygen Original Units\";\n" +
"  }\n" +
"  Primary_Investigator {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Primary Investigator\";\n" +
"  }\n" +
"  Primary_Investigator_VAR {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Primary Investigator VAR\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float32 actual_range 5.046722, 780.2255;\n" +
"    String ancillary_variables \"z_sigfigs z_WODflag\";\n" +
"    String axis \"Z\";\n" +
"    String cdm_ioos_category \"Location\";\n" +
"    Float64 colorBarMaximum 8000.0;\n" +
"    Float64 colorBarMinimum -8000.0;\n" +
"    String colorBarPalette \"TopographyDepth\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth Below Sea Surface\";\n" +
"    String positive \"down\";\n" +
"    String source_name \"z\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  z_WODflag {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 0, 0;\n" +
"    Float64 colorBarMaximum 2.5;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String flag_meanings \"accepted duplicate_or_inversion density_inversion\";\n" +
"    Byte flag_values 0, 1, 2;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"WOD_depth_level_flag\";\n" +
"    String standard_name \"depth status_flag\";\n" +
"  }\n" +
"  z_sigfigs {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 2, 7;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"depth significant figures   \";\n" +
"  }\n" +
"  Temperature {\n" +
"    Float32 _FillValue -1.0e+10;\n" +
"    Float32 actual_range -1.914, 6.166;\n" +
"    String ancillary_variables \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag\";\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  Temperature_sigfigs {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 4, 5;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"sea_water_temperature significant_figures\";\n" +
"  }\n" +
"  Temperature_WODflag {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 0, 0;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String flag_meanings \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"WOD_observation_flag\";\n" +
"    String standard_name \"sea_water_temperature status_flag\";\n" +
"  }\n" +
"  Salinity {\n" +
"    Float32 _FillValue -1.0e+10;\n" +
"    Float32 actual_range 24.6164, 37.7583;\n" +
"    String ancillary_variables \"Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag\";\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Sea Water Salinity\";\n" +
"    String standard_name \"sea_water_practical_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
"  Salinity_sigfigs {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range -127, 6;\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"sea_water_salinity significant_figures\";\n" +
"    String standard_name \"sea_water_practical_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
"  Salinity_WODflag {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range -127, 0;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String flag_meanings \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"WOD_observation_flag\";\n" +
"    String standard_name \"sea_water_salinity status_flag\";\n" +
"  }\n" +
"  Pressure {\n" +
"    Float32 _FillValue -1.0e+10;\n" +
"    Float32 actual_range 5.1, 790.0;\n" +
"    String ancillary_variables \"Pressure_sigfigs Pressure_WODflag Pressure_WODprofileflag\";\n" +
"    Float64 colorBarMaximum 5000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Sea Water Pressure\";\n" +
"    String standard_name \"sea_water_pressure\";\n" +
"    String units \"dbar\";\n" +
"  }\n" +
"  Pressure_sigfigs {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 2, 4;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"sea_water_pressure significant_figures\";\n" +
"  }\n" +
"  Oxygen {\n" +
"    Float32 _FillValue -1.0e+10;\n" +
"    Float32 actual_range 5.71734, 9.488957;\n" +
"    String ancillary_variables \"Oxygen_sigfigs Oxygen_WODflag Oxygen_WODprofileflag\";\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Dissolved O2\";\n" +
"    String long_name \"Volume Fraction Of Oxygen In Sea Water\";\n" +
"    String standard_name \"volume_fraction_of_oxygen_in_sea_water\";\n" +
"    String units \"ml/l\";\n" +
"  }\n" +
"  Oxygen_sigfigs {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range -127, 7;\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved O2\";\n" +
"    String long_name \"volume_fraction_of_oxygen_in_sea_water significant_figures\";\n" +
"  }\n" +
"  Oxygen_WODflag {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range -127, 0;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String flag_meanings \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"WOD_observation_flag\";\n" +
"    String standard_name \"volume_fraction_of_oxygen_in_sea_water status_flag\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Profile\";\n" +
"    String cdm_profile_variables \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Platform, Institute, Orig_Stat_Num, dataset, real_time, Ocean_Vehicle, Temperature_WODprofileflag, Temperature_Instrument, Salinity_WODprofileflag, Salinity_Instrument, Oxygen_WODprofileflag, Oxygen_Original_units, Primary_Investigator, Primary_Investigator_VAR\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"OCLhelp@noaa.gov\";\n" +
"    String creator_name \"Ocean Climate Lab/NCEI\";\n" +
"    String creator_type \"institution\";\n" +
"    String creator_url \"https://www.nodc.noaa.gov\";\n" +
"    String date_created \"2018-03-25\";\n" +
"    String date_modified \"2018-03-25\";\n" +
"    Float64 Easternmost_Easting 149.8134;\n" +
"    String featureType \"Profile\";\n" +
"    Float64 geospatial_lat_max 89.625;\n" +
"    Float64 geospatial_lat_min 68.572;\n" +
"    String geospatial_lat_resolution \"point\";\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 149.8134;\n" +
"    Float64 geospatial_lon_min -165.4138;\n" +
"    String geospatial_lon_resolution \"point\";\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 780.2255;\n" +
"    Float64 geospatial_vertical_min 5.046722;\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String grid_mapping_epsg_code \"EPSG:4326\";\n" +
"    Float32 grid_mapping_inverse_flattening 298.25723;\n" +
"    Float32 grid_mapping_longitude_of_prime_meridian 0.0;\n" +
"    String grid_mapping_name \"latitude_longitude\";\n" +
"    Float32 grid_mapping_semi_major_axis 6378137.0;\n" +
"    String history \"World Ocean Database";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);

//        "2018-05-08T21:27:53Z (local files)
//2018-05-08T21:27:53Z http://localhost:8080/cwexperimental/tabledap/testInvalidCRAFiles.das";
expected =
"String id \"/nodc/data/oc5.clim.0/wod_update_nc/2006/wod_drb_2006.nc\";\n" +
"    String infoUrl \"https://www.nodc.noaa.gov\";\n" +
"    String institution \"NCEI, NOAA\";\n" +
"    String keywords \"Access_no, accession, below, cast, center, centers, chemistry, country, cruise, data, database, dataset, date, density, depth, depth status_flag, dissolved, dissolved o2, earth, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Pressure > Water Pressure, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, environmental, figures, file, flag, fraction, GMT_time, identifier, information, institute, instrument, investigator, latitude, level, longitude, multi, multi-cast, name, national, ncei, nesdis, noaa, nodc, number, O2, observation, ocean, Ocean_Vehicle, oceanographic, oceans, Orig_Stat_Num, original, originators, originators_cruise_identifier, oxygen, Oxygen_Original_units, Oxygen_sigfigs, Oxygen_WODflag, Oxygen_WODprofileflag, platform, practical, pressure, Pressure_sigfigs, primary, Primary_Investigator, Primary_Investigator_VAR, profile, quality, real, real_time, responsible, salinity, Salinity_Instrument, Salinity_sigfigs, Salinity_WODflag, Salinity_WODprofileflag, science, sea, sea_water_practical_salinity, sea_water_pressure, sea_water_salinity status_flag, sea_water_temperature, sea_water_temperature status_flag, seawater, significant, station, statistics, status, surface, temperature, Temperature_Instrument, Temperature_sigfigs, Temperature_WODflag, Temperature_WODprofileflag, time, unique, units, vehicle, volume, volume_fraction_of_oxygen_in_sea_water, volume_fraction_of_oxygen_in_sea_water status_flag, water, wod, WOD_cruise_identifier, wod_unique_cast, world, z_sigfigs, z_WODflag\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.nodc\";\n" +
"    Float64 Northernmost_Northing 89.625;\n" +
"    String project \"World Ocean Database\";\n" +
"    String publisher_email \"NODC.Services@noaa.gov\";\n" +
"    String publisher_name \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\";\n" +
"    String publisher_type \"institution\";\n" +
"    String publisher_url \"https://www.nodc.noaa.gov\";\n" +
"    String references \"World Ocean Database 2013. URL:https://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\";\n" +
"    String source \"World Ocean Database\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 68.572;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v41\";\n" +
"    String subsetVariables \"dataset, Temperature_WODprofileflag, Salinity_WODprofileflag, Oxygen_WODprofileflag, z_WODflag, Temperature_WODflag\";\n" +
"    String summary \"World Ocean Database - Multi-cast file. Data for multiple casts from the World Ocean Database\";\n" +
"    String time_coverage_end \"2006-12-31T18:00:02Z\";\n" +
"    String time_coverage_start \"2005-01-01T00:00:00Z\";\n" +
"    String title \"World Ocean Database, Multi-cast file\";\n" +
"    Float64 Westernmost_Easting -165.4138;\n" +
"  }\n" +
"}\n";
        int po = Math.max(0, results.indexOf(expected.substring(0, 20)));
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

        //.csv     all vars
        userDapQuery = "&time>2005-06-11T06&time<2005-06-11T07";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
            testCacheDir, eddTable.className() + "_wod_all", ".csv"); 
        results = String2.directReadFrom88591File(testCacheDir + tName);
        //String2.log(results);
        expected = 
"country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,latitude,longitude,time,date,GMT_time,Access_no,Platform,Institute,Orig_Stat_Num,dataset,real_time,Ocean_Vehicle,Temperature_WODprofileflag,Temperature_Instrument,Salinity_WODprofileflag,Salinity_Instrument,Oxygen_WODprofileflag,Oxygen_Original_units,Primary_Investigator,Primary_Investigator_VAR,depth,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Pressure,Pressure_sigfigs,Oxygen,Oxygen_sigfigs,Oxygen_WODflag\n" +
",,,,degrees_north,degrees_east,UTC,,hours,,,,,,,,,,,PSU,,,,,m,,,degree_C,,,PSU,PSU,,dbar,,ml/l,,\n" +
"JAPAN,JP033440,,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,20050611,7.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504847942,drifting buoy,,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,NaN,,,,24.215,0,6,-1.728,5,0,31.7409,6,0,NaN,NaN,NaN,NaN,NaN\n" +
"JAPAN,JP033440,,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,20050611,7.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504847942,drifting buoy,,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,NaN,,,,48.427,0,6,-1.732,5,0,31.758,6,0,NaN,NaN,NaN,NaN,NaN\n" +
"JAPAN,JP033440,,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,20050611,7.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504847942,drifting buoy,,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,NaN,,,,77.478,0,6,-1.658,5,0,33.5968,6,0,NaN,NaN,NaN,NaN,NaN\n" +
"JAPAN,JP033440,,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,20050611,7.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504847942,drifting buoy,,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,NaN,,,,116.205,0,7,-1.198,5,0,34.2219,6,0,NaN,NaN,NaN,NaN,NaN\n" +
"JAPAN,JP033440,,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,20050611,7.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504847942,drifting buoy,,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,NaN,,,,195.751,0,7,0.224,4,0,34.6426,6,0,NaN,NaN,NaN,NaN,NaN\n" +
"JAPAN,JP033440,,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,20050611,7.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504847942,drifting buoy,,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,NaN,,,,285.203,0,7,0.99,4,0,34.8372,6,0,NaN,NaN,NaN,NaN,NaN\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);

        //.csv    outer vars only,   constrain time, but don't include time in results
        userDapQuery = "WOD_cruise_identifier,wod_unique_cast,latitude,longitude&time>2005-06-11T06&time<2005-06-11T07";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
            testCacheDir, eddTable.className() + "_wod_outer", ".csv"); 
        results = String2.directReadFrom88591File(testCacheDir + tName);
        //String2.log(results);
        expected = 
"WOD_cruise_identifier,wod_unique_cast,latitude,longitude\n" +
",,degrees_north,degrees_east\n" +
"JP033440,10901522,89.0668,6.2756\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);

        //.csv     outer and inner vars
        userDapQuery = "WOD_cruise_identifier,wod_unique_cast,latitude,longitude,time,depth,Temperature,Salinity&time>2005-06-11T06&time<2005-06-11T07";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
            testCacheDir, eddTable.className() + "_wod_outerInner", ".csv"); 
        results = String2.directReadFrom88591File(testCacheDir + tName);
        //String2.log(results);
        expected = 
"WOD_cruise_identifier,wod_unique_cast,latitude,longitude,time,depth,Temperature,Salinity\n" +
",,degrees_north,degrees_east,UTC,m,degree_C,PSU\n" +
"JP033440,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,24.215,-1.728,31.7409\n" +
"JP033440,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,48.427,-1.732,31.758\n" +
"JP033440,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,77.478,-1.658,33.5968\n" +
"JP033440,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,116.205,-1.198,34.2219\n" +
"JP033440,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,195.751,0.224,34.6426\n" +
"JP033440,10901522,89.0668,6.2756,2005-06-11T06:59:59Z,285.203,0.99,34.8372\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);

        //.csv   inner vars vars only   based on outer constraint
        userDapQuery = "depth,Temperature,Salinity&wod_unique_cast=10901522";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery,
            testCacheDir, eddTable.className() + "_wod_inner", ".csv"); 
        results = String2.directReadFrom88591File(testCacheDir + tName);
        //String2.log(results);
        expected = 
"depth,Temperature,Salinity\n" +
"m,degree_C,PSU\n" +
"24.215,-1.728,31.7409\n" +
"48.427,-1.732,31.758\n" +
"77.478,-1.658,33.5968\n" +
"116.205,-1.198,34.2219\n" +
"195.751,0.224,34.6426\n" +
"285.203,0.99,34.8372\n"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromInvalidCRAFiles.test7SampleDimensions() finished.");
    }
    
    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 1;
        String msg = "\n^^^ EDDTableFromInvalidCRAFiles.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) testGenerateDatasetsXml();
                    if (test ==  1) testBasic();
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }

}

