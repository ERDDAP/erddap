/* 
 * EDDTableFromNccsvFiles Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
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
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


/** 
 * This class represents a table of data from a collection of NCCSV files.
 * See https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html .
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2017-04-17
 */
public class EDDTableFromNccsvFiles extends EDDTableFromFiles { 


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
    public EDDTableFromNccsvFiles(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, String tCharset, 
        int tColumnNamesRow, int tFirstDataRow, String tColumnSeparator,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles, boolean tRemoveMVRows, 
        int tStandardizeWhat, int tNThreads, 
        String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex) 
        throws Throwable {

        super("EDDTableFromNccsvFiles", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow, tColumnSeparator,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows, tStandardizeWhat, 
            tNThreads, tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex);

    }


    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
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

        //Future: more efficient if !mustGetData is handled differently

        //read the file
        Table table = new Table();
        table.readNccsv(tFileDir + tFileName);

        table.standardize(standardizeWhat);

        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromNccsvFiles.
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
     * @param useDimensionsCSV If null or "", this finds the group of variables sharing the
     *    highest number of dimensions. Otherwise, it find the variables using
     *    these dimensions (plus related char variables).
     * @param tReloadEveryNMinutes  e.g., 10080 for weekly
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortedColumnSourceName   use "" if not known or not needed. 
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
        String tColumnNameForExtract, //String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        int tStandardizeWhat, String tCacheFromUrl,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** EDDTableFromNccsvFiles.generateDatasetsXml" +
            "\nfileDir=" + tFileDir + " fileNameRegex=" + tFileNameRegex +
            "\nsampleFileName=" + sampleFileName +
            " reloadEveryNMinutes=" + tReloadEveryNMinutes +
            "\nextract pre=" + tPreExtractRegex + " post=" + tPostExtractRegex + " regex=" + tExtractRegex +
            " colName=" + tColumnNameForExtract +
            //"\nsortedColumn=" + tSortedColumnSourceName + 
            " sortFilesBy=" + tSortFilesBySourceNames + 
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
            tReloadEveryNMinutes = 1440; 
        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        dataSourceTable.readNccsv(sampleFileName);

        tStandardizeWhat = tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE?
            DEFAULT_STANDARDIZEWHAT : tStandardizeWhat;
        dataSourceTable.standardize(tStandardizeWhat);

        Table dataAddTable = new Table();
        double maxTimeES = Double.NaN;
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            PrimitiveArray sourcePA = dataSourceTable.getColumn(c);
            PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), sourceAtts, null, colName, 
                destPA.elementClass() != String.class, //tryToAddStandardName
                destPA.elementClass() != String.class, //addColorBarMinMax
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

        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable)? "Point" : "Other",
                tFileDir, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //subsetVariables
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
        if (tSortFilesBySourceNames.length() == 0) {
            //if (tColumnNameForExtract.length() > 0) &&
                //tSortedColumnSourceName.length() > 0 &&
                //!tColumnNameForExtract.equals(tSortedColumnSourceName))
                //tSortFilesBySourceNames = tColumnNameForExtract + ", " + tSortedColumnSourceName;
            //else 
            if (tColumnNameForExtract.length() > 0)
                tSortFilesBySourceNames = tColumnNameForExtract;
            //else 
            //    tSortFilesBySourceNames = tSortedColumnSourceName;
        }
        sb.append(
            "<dataset type=\"EDDTableFromNccsvFiles\" datasetID=\"" + 
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
              "    <columnNameForExtract>" + XML.encodeAsXML(tColumnNameForExtract) + "</columnNameForExtract>\n" : "") +
            //"    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + XML.encodeAsXML(tSortFilesBySourceNames) + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" +
            "    <accessibleViaFiles>true</accessibleViaFiles>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
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

        try {
            String results = generateDatasetsXml(
                EDStatic.unitTestDataDir + "nccsv", 
                "sampleScalar\\.csv",
                "",
                1440,
                "","","","", 
                "ship time", 
                "", "", "", "", 
                -1, null, //defaultStandardizeWhat
                null) + "\n";

            String2.log(results);

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromNccsvFiles",
                EDStatic.unitTestDataDir + "nccsv", 
                "sampleScalar\\.csv",
                "",
                "1440",
                "", "", "", "", 
                "ship time", 
                "", "", "", "", 
                "-1", ""}, //defaultStandardizeWhat
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<dataset type=\"EDDTableFromNccsvFiles\" datasetID=\"nccsv_9632_f0df_07b0\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTest/nccsv/</fileDir>\n" +
"    <fileNameRegex>sampleScalar\\.csv</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <sortFilesBySourceNames>ship time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"cdm_trajectory_variables\">ship</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0</att>\n" +
"        <att name=\"creator_email\">bob.simons@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Bob Simons</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n" +
"        <att name=\"featureType\">trajectory</att>\n" +
"        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html</att>\n" +
"        <att name=\"institution\">NOAA NMFS SWFSC ERD, NOAA PMEL</att>\n" +
"        <att name=\"keywords\">NOAA, sea, ship, sst, surface, temperature, trajectory</att>\n" +
"        <att name=\"license\">&quot;NCCSV Demonstration&quot; by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"subsetVariables\">ship</att>\n" +
"        <att name=\"summary\">This is a paragraph or two describing the dataset.</att>\n" +
"        <att name=\"title\">NCCSV Demonstration</att>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Trajectory</att>\n" +
"        <att name=\"keywords\">center, data, demonstration, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>ship</sourceName>\n" +
"        <destinationName>ship</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"cf_role\">trajectory_id</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Ship</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ssZ</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>status</sourceName>\n" +
"        <destinationName>status</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">From http://some.url.gov/someProjectDocument , Table C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Status</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>testLong</sourceName>\n" +
"        <destinationName>testLong</destinationName>\n" +
"        <dataType>long</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"long\">9223372036854775807</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Test Long</att>\n" +
"            <att name=\"missing_value\" type=\"long\">-9223372036854775808</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sst</sourceName>\n" +
"        <destinationName>sst</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.17 23.58</att>\n" +
"            <att name=\"missing_value\" type=\"float\">99.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_temperature</att>\n" +
"            <att name=\"testBytes\" type=\"byteList\">-128 0 127</att>\n" +
"            <att name=\"testChars\" type=\"charList\">\",\" \"\"\"\" \\u20ac</att>\n" +
"            <att name=\"testDoubles\" type=\"doubleList\">-1.7976931348623157E308 0.0 1.7976931348623157E308</att>\n" +
"            <att name=\"testFloats\" type=\"floatList\">-3.4028235E38 0.0 3.4028235E38</att>\n" +
"            <att name=\"testInts\" type=\"intList\">-2147483648 0 2147483647</att>\n" +
"            <att name=\"testLongs\" type=\"longList\">-9223372036854775808 0 9223372036854775807</att>\n" +
"            <att name=\"testShorts\" type=\"shortList\">-32768 0 32767</att>\n" +
"            <att name=\"testStrings\"> a&#9;~&#xfc;,\n" +
"&#39;z&quot;&#x20ac;</att>\n" +  
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Sea Surface Temperature</att>\n" +
"            <att name=\"testStrings\">a&#9;~&#xfc;,\n" +
"&#39;z&quot;&#x20ac;</att>\n" +  
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            String tDatasetID = "nccsv_9632_f0df_07b0";
            EDD.deleteCachedDatasetInfo(tDatasetID);
            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), tDatasetID, "");
            Test.ensureEqual(edd.title(), "NCCSV Demonstration", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "ship, time, latitude, longitude, status, testLong, sst", 
                "");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml."); 
        }

    }

    /**
     * This does basic tests of this class.
     * Note that ü in utf-8 is \xC3\xBC or [195][188]
     * Note that Euro is \\u20ac (and low byte is #172 is \\u00ac -- I worked to encode as '?')
     *
     * @throws Throwable if trouble
     */
    public static void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNccsvFiles.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String dir = EDStatic.fullTestCacheDirectory;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        Test.ensureEqual(String2.parseFloat("3.40282347e38"), 3.4028234663852886E38, "");
        Test.ensureEqual(String2.parseFloat("3.40282347e38") + "", "3.4028235E38", "");
        Test.ensureEqual(String2.parseFloat("3.4028235e38") + "", "3.4028235E38", "");
        Test.ensureEqual(Float.MAX_VALUE + "", "3.4028235E38", "");
        Test.ensureEqual(String.valueOf(Float.MAX_VALUE), "3.4028235E38", "");

        //long MIN_VALUE=-9,223,372,036,854,775,808 and MAX_VALUE=9,223,372,036,854,775,807
        LongArray la = new LongArray(new long[]{
            -9223372036854775808L,-9007199254740992L,0,9007199254740992L,
             9223372036854775806L,9223372036854775807L});
        String2.log("la stats=" + PrimitiveArray.displayStats(la.calculateStats()));
        DoubleArray da = new DoubleArray(la);
        double laStats[] = la.calculateStats();
        String2.log("da=" + da + "\n" +
            "stats=" + PrimitiveArray.displayStats(laStats) +
            "\nstats as doubles: " + String2.toCSSVString(laStats));
        //String2.pressEnterToContinue();

        String id = "testNccsvScalar"; //straight from generateDatasetsXml
        if (deleteCachedDatasetInfo)
            deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNccsvFiles  test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  ship {\n" +
"    String cf_role \"trajectory_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Ship\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.4902299e+9, 1.4903127e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 27.9998, 28.0003;\n" +
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
"    Float64 actual_range -132.1591, -130.2576;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  status {\n" +
"    String actual_range \"\t\n" +
"?\";\n" +  //important test of \\u20ac
"    String comment \"From http://some.url.gov/someProjectDocument , Table C\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Status\";\n" +
"  }\n" +
"  testLong {\n" +
"    Float64 _FillValue NaN;\n" + //long MAX_VALUE, as a double, is always treated as NaN
          //long MIN_VALUE and MAX_VALUE converted to doubles
"    Float64 actual_range -9.223372036854776e+18, 9.2233720368547748e+18;\n" + 
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Test of Longs\";\n" +
"    String units \"1\";\n" +
"  }\n" +
"  sst {\n" +
"    Float32 actual_range 10.0, 10.9;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Surface Temperature\";\n" +
"    Float32 missing_value 99.0;\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    Byte testBytes -128, 0, 127;\n" +
"    String testChars \",\n" +
"\\\"\n" + //important tests of " 
"?\";\n" + //important tests of \\u20ac
"    Float64 testDoubles -1.7976931348623157e+308, 0.0, 1.7976931348623157e+308;\n" +
"    Float32 testFloats -3.4028235e+38, 0.0, 3.4028235e+38;\n" +
"    Int32 testInts -2147483648, 0, 2147483647;\n" +
"    Float64 testLongs -9.223372036854776e+18, -9.007199254740992e+15, 9.007199254740992e+15, 9.2233720368547748e+18, NaN;\n" + //longs treated as doubles
"    Int16 testShorts -32768, 0, 32767;\n" +
"    String testStrings \" a\t~\u00fc,\n" + //important tests
"'z\\\"?\";\n" + //important test of \\u20ac
"    String units \"degree_C\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Trajectory\";\n" +
"    String cdm_trajectory_variables \"ship\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\";\n" +
"    String creator_email \"bob.simons@noaa.gov\";\n" +
"    String creator_name \"Bob Simons\";\n" +
"    String creator_type \"person\";\n" +
"    String creator_url \"https://www.pfeg.noaa.gov\";\n" +
"    Float64 Easternmost_Easting -130.2576;\n" +
"    String featureType \"Trajectory\";\n" +
"    Float64 geospatial_lat_max 28.0003;\n" +
"    Float64 geospatial_lat_min 27.9998;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -130.2576;\n" +
"    Float64 geospatial_lon_min -132.1591;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected =
"http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.das\";\n" +
"    String infoUrl \"https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD, NOAA PMEL\";\n" +
"    String keywords \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\";\n" +
"    Float64 Northernmost_Northing 28.0003;\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 27.9998;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String subsetVariables \"ship, status, testLong\";\n" +
"    String summary \"This is a paragraph or two describing the dataset.\";\n" +
"    String time_coverage_end \"2017-03-23T23:45:00Z\";\n" +
"    String time_coverage_start \"2017-03-23T00:45:00Z\";\n" +
"    String title \"NCCSV Demonstration\";\n" +
"    Float64 Westernmost_Easting -132.1591;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 40));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String ship;\n" +
"    Float64 time;\n" +
"    Float64 latitude;\n" +
"    Float64 longitude;\n" +
"    String status;\n" +    //char -> String
"    Float64 testLong;\n" + //long -> double
"    Float32 sst;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNccsvFiles.test make DATA FILES\n");       

        //.csv  all data        
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"ship,time,latitude,longitude,status,testLong,sst\n" +
",UTC,degrees_north,degrees_east,,1,degree_C\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-9223372036854775808,10.9\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-9007199254740992,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,9007199254740992,10.7\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",9223372036854775806,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\\u00fc,NaN,10.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv   subset
        userDapQuery = "time,ship,sst&time=2017-03-23T02:45";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1time", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,ship,sst\n" +
"UTC,,degree_C\n" +
"2017-03-23T02:45:00Z,\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",10.7\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv   subset based on string constraint
        userDapQuery = "&ship=\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\""; //json formatted constraint
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1string", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"ship,time,latitude,longitude,status,testLong,sst\n" +
",UTC,degrees_north,degrees_east,,1,degree_C\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-9223372036854775808,10.9\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-9007199254740992,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,9007199254740992,10.7\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",9223372036854775806,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\\u00fc,NaN,10.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv   subset based on char constraint
        userDapQuery = "&status=\"\\u20ac\""; //json formatted constraint
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1char", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"ship,time,latitude,longitude,status,testLong,sst\n" +
",UTC,degrees_north,degrees_east,,1,degree_C\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-9007199254740992,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv   subset based on long constraint
        userDapQuery = "&testLong=-9007199254740992";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1long", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"ship,time,latitude,longitude,status,testLong,sst\n" +
",UTC,degrees_north,degrees_east,,1,degree_C\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-9007199254740992,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv   subset based on harder long constraint
        userDapQuery = "&testLong=-9223372036854775808";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1longb", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"ship,time,latitude,longitude,status,testLong,sst\n" +
",UTC,degrees_north,degrees_east,,1,degree_C\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-9223372036854775808,10.9\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.nccsvMetadata        
        userDapQuery = "time,ship,sst&time=2017-03-23T02:45"; //will be ignored
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all", ".nccsvMetadata"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"\n" +
"*GLOBAL*,cdm_data_type,Trajectory\n" +
"*GLOBAL*,cdm_trajectory_variables,ship\n" +
"*GLOBAL*,creator_email,bob.simons@noaa.gov\n" +
"*GLOBAL*,creator_name,Bob Simons\n" +
"*GLOBAL*,creator_type,person\n" +
"*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n" +
"*GLOBAL*,Easternmost_Easting,-130.2576d\n" +
"*GLOBAL*,featureType,Trajectory\n" +
"*GLOBAL*,geospatial_lat_max,28.0003d\n" +
"*GLOBAL*,geospatial_lat_min,27.9998d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,-130.2576d\n" +
"*GLOBAL*,geospatial_lon_min,-132.1591d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n" +
"*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n" +
"*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n" +
"*GLOBAL*,Northernmost_Northing,28.0003d\n" +
"*GLOBAL*,sourceUrl,(local files)\n" +
"*GLOBAL*,Southernmost_Northing,27.9998d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n" +
"*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n" +
"*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n" +
"*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n" +
"*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n" +
"*GLOBAL*,title,NCCSV Demonstration\n" +
"*GLOBAL*,Westernmost_Easting,-132.1591d\n" +
"ship,*DATA_TYPE*,String\n" +
"ship,cf_role,trajectory_id\n" +
"ship,ioos_category,Identifier\n" +
"ship,long_name,Ship\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,actual_range,2017-03-23T00:45:00Z\\n2017-03-23T23:45:00Z\n" +
"time,axis,T\n" +
"time,ioos_category,Time\n" +
"time,long_name,Time\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"latitude,*DATA_TYPE*,double\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,actual_range,27.9998d,28.0003d\n" +
"latitude,axis,Y\n" +
"latitude,colorBarMaximum,90.0d\n" +
"latitude,colorBarMinimum,-90.0d\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Latitude\n" +
"latitude,standard_name,latitude\n" +
"latitude,units,degrees_north\n" +
"longitude,*DATA_TYPE*,double\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,actual_range,-132.1591d,-130.2576d\n" +
"longitude,axis,X\n" +
"longitude,colorBarMaximum,180.0d\n" +
"longitude,colorBarMinimum,-180.0d\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Longitude\n" +
"longitude,standard_name,longitude\n" +
"longitude,units,degrees_east\n" +
"status,*DATA_TYPE*,char\n" +
"status,actual_range,\"'\\t'\",\"'\\u20ac'\"\n" +
"status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n" +
"status,ioos_category,Unknown\n" +
"status,long_name,Status\n" +
"testLong,*DATA_TYPE*,long\n" +
"testLong,_FillValue,9223372036854775807L\n" +
"testLong,actual_range,-9223372036854775808L,9223372036854774784L\n" + //!!! max is wrong! calculated internally as double. max should be -9223372036854775806L
"testLong,ioos_category,Unknown\n" +
"testLong,long_name,Test of Longs\n" +
"testLong,units,\"1\"\n" +
"sst,*DATA_TYPE*,float\n" +
"sst,actual_range,10.0f,10.9f\n" +
"sst,colorBarMaximum,32.0d\n" +
"sst,colorBarMinimum,0.0d\n" +
"sst,ioos_category,Temperature\n" +
"sst,long_name,Sea Surface Temperature\n" +
"sst,missing_value,99.0f\n" +
"sst,standard_name,sea_surface_temperature\n" +
"sst,testBytes,-128b,0b,127b\n" +
"sst,testChars,\"','\",\"'\"\"'\",\"'\\u20ac'\"\n" +
"sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n" +
"sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n" +
"sst,testInts,-2147483648i,0i,2147483647i\n" +
"sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n" +
"sst,testShorts,-32768s,0s,32767s\n" +
"sst,testStrings,\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\"\n" +
"sst,units,degree_C\n" +
"\n" +
"*END_METADATA*\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.nccsv all
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all", ".nccsv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"\n" +
"*GLOBAL*,cdm_data_type,Trajectory\n" +
"*GLOBAL*,cdm_trajectory_variables,ship\n" +
"*GLOBAL*,creator_email,bob.simons@noaa.gov\n" +
"*GLOBAL*,creator_name,Bob Simons\n" +
"*GLOBAL*,creator_type,person\n" +
"*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n" +
"*GLOBAL*,Easternmost_Easting,-130.2576d\n" +
"*GLOBAL*,featureType,Trajectory\n" +
"*GLOBAL*,geospatial_lat_max,28.0003d\n" +
"*GLOBAL*,geospatial_lat_min,27.9998d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,-130.2576d\n" +
"*GLOBAL*,geospatial_lon_min,-132.1591d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,history," + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected =        
//T17:35:08Z (local files)\\n2017-04-18T17:35:08Z  
"http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.nccsv\n" +
"*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n" +
"*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n" +
"*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n" +
"*GLOBAL*,Northernmost_Northing,28.0003d\n" +
"*GLOBAL*,sourceUrl,(local files)\n" +
"*GLOBAL*,Southernmost_Northing,27.9998d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n" +
"*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n" +
"*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n" +
"*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n" +
"*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n" +
"*GLOBAL*,title,NCCSV Demonstration\n" +
"*GLOBAL*,Westernmost_Easting,-132.1591d\n" +
"ship,*DATA_TYPE*,String\n" +
"ship,cf_role,trajectory_id\n" +
"ship,ioos_category,Identifier\n" +
"ship,long_name,Ship\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,axis,T\n" +
"time,ioos_category,Time\n" +
"time,long_name,Time\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"latitude,*DATA_TYPE*,double\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,axis,Y\n" +
"latitude,colorBarMaximum,90.0d\n" +
"latitude,colorBarMinimum,-90.0d\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Latitude\n" +
"latitude,standard_name,latitude\n" +
"latitude,units,degrees_north\n" +
"longitude,*DATA_TYPE*,double\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,axis,X\n" +
"longitude,colorBarMaximum,180.0d\n" +
"longitude,colorBarMinimum,-180.0d\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Longitude\n" +
"longitude,standard_name,longitude\n" +
"longitude,units,degrees_east\n" +
"status,*DATA_TYPE*,char\n" +
"status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n" +
"status,ioos_category,Unknown\n" +
"status,long_name,Status\n" +
"testLong,*DATA_TYPE*,long\n" +
"testLong,_FillValue,9223372036854775807L\n" +
"testLong,ioos_category,Unknown\n" +
"testLong,long_name,Test of Longs\n" +
"testLong,units,\"1\"\n" +
"sst,*DATA_TYPE*,float\n" +
"sst,colorBarMaximum,32.0d\n" +
"sst,colorBarMinimum,0.0d\n" +
"sst,ioos_category,Temperature\n" +
"sst,long_name,Sea Surface Temperature\n" +
"sst,missing_value,99.0f\n" +
"sst,standard_name,sea_surface_temperature\n" +
"sst,testBytes,-128b,0b,127b\n" +
"sst,testChars,\"','\",\"'\"\"'\",\"'\\u20ac'\"\n" +
"sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n" +
"sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n" +
"sst,testInts,-2147483648i,0i,2147483647i\n" +
"sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n" +
"sst,testShorts,-32768s,0s,32767s\n" +
"sst,testStrings,\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\"\n" +
"sst,units,degree_C\n" +
"\n" +
"*END_METADATA*\n" +
"ship,time,latitude,longitude,status,testLong,sst\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-9223372036854775808L,10.9\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-9007199254740992L,\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,9007199254740992L,10.7\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",9223372036854775806L,99.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\\u00fc,,10.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,,\n" +
"*END_DATA*\n";
        tPo = results.indexOf(expected.substring(0, 40));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        //.nccsv subset
        userDapQuery = "time,ship,sst&time=2017-03-23T02:45";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1time", ".nccsv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"\n" +
"*GLOBAL*,cdm_data_type,Trajectory\n" +
"*GLOBAL*,cdm_trajectory_variables,ship\n" +
"*GLOBAL*,creator_email,bob.simons@noaa.gov\n" +
"*GLOBAL*,creator_name,Bob Simons\n" +
"*GLOBAL*,creator_type,person\n" +
"*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n" +
"*GLOBAL*,Easternmost_Easting,-130.2576d\n" +
"*GLOBAL*,featureType,Trajectory\n" +
"*GLOBAL*,geospatial_lat_max,28.0003d\n" +
"*GLOBAL*,geospatial_lat_min,27.9998d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,-130.2576d\n" +
"*GLOBAL*,geospatial_lon_min,-132.1591d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,history,\"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected =        
//2017-04-18T17:41:53Z (local files)\\n2017-04-18T17:41:53Z 
"http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.nccsv?time,ship,sst&time=2017-03-23T02:45\"\n" +
"*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n" +
"*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n" +
"*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n" +
"*GLOBAL*,Northernmost_Northing,28.0003d\n" +
"*GLOBAL*,sourceUrl,(local files)\n" +
"*GLOBAL*,Southernmost_Northing,27.9998d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n" +
"*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n" +
"*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n" +
"*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n" +
"*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n" +
"*GLOBAL*,title,NCCSV Demonstration\n" +
"*GLOBAL*,Westernmost_Easting,-132.1591d\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,axis,T\n" +
"time,ioos_category,Time\n" +
"time,long_name,Time\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"ship,*DATA_TYPE*,String\n" +
"ship,cf_role,trajectory_id\n" +
"ship,ioos_category,Identifier\n" +
"ship,long_name,Ship\n" +
"sst,*DATA_TYPE*,float\n" +
"sst,colorBarMaximum,32.0d\n" +
"sst,colorBarMinimum,0.0d\n" +
"sst,ioos_category,Temperature\n" +
"sst,long_name,Sea Surface Temperature\n" +
"sst,missing_value,99.0f\n" +
"sst,standard_name,sea_surface_temperature\n" +
"sst,testBytes,-128b,0b,127b\n" +
"sst,testChars,\"','\",\"'\"\"'\",\"'\\u20ac'\"\n" +
"sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n" +
"sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n" +
"sst,testInts,-2147483648i,0i,2147483647i\n" +
"sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n" +
"sst,testShorts,-32768s,0s,32767s\n" +
"sst,testStrings,\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\"\n" +
"sst,units,degree_C\n" +
"\n" +
"*END_METADATA*\n" +
"time,ship,sst\n" +
"2017-03-23T02:45:00Z,\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",10.7\n" +
"*END_DATA*\n";    
        tPo = results.indexOf(expected.substring(0, 40));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
    }

    /**
     * This tests how high ascii and unicode chars in attributes and data
     * appear in various output file types.
     *
     * @throws Throwable if trouble
     */
    public static void testChar() throws Throwable {
        String2.log("\n****************** EDDTableFromNccsvFiles.testChar() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String dir = EDStatic.fullTestCacheDirectory;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        int po, tPo;

        String id = "testNccsvScalar"; //straight from generateDatasetsXml
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //*** getting dap asc
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".asc"); 
        results = String2.annotatedString(
            String2.directReadFrom88591File(dir + tName));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    String ship;[10]\n" +
"    Float64 time;[10]\n" +
"    Float64 latitude;[10]\n" +
"    Float64 longitude;[10]\n" +
"    String status;[10]\n" +
"    Float64 testLong;[10]\n" +
"    Float32 sst;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"---------------------------------------------[10]\n" +
"s.ship, s.time, s.latitude, s.longitude, s.status, s.testLong, s.sst[10]\n" +
"\" a[9]~[252],[10]\n" +
"'z\\\"?\", 1.4902299E9, 28.0002, -130.2576, \"A\", -9223372036854775808, 10.9[10]\n" +
"\" a[9]~[252],[10]\n" +
"'z\\\"?\", 1.4902335E9, 28.0003, -130.3472, \"?\", -9007199254740992, [10]\n" +
"\" a[9]~[252],[10]\n" +
"'z\\\"?\", 1.4902371E9, 28.0001, -130.4305, \"[9]\", 9007199254740992, 10.7[10]\n" +
"\" a[9]~[252],[10]\n" +
"'z\\\"?\", 1.4902731E9, 27.9998, -131.5578, \"\\\"\", 9223372036854775806, 99.0[10]\n" +
"\" a[9]~[252],[10]\n" +
"'z\\\"?\", 1.4903055E9, 28.0003, -132.0014, \"[252]\", , 10.0[10]\n" +
"\" a[9]~[252],[10]\n" +
"'z\\\"?\", 1.4903127E9, 28.0002, -132.1591, \"?\", , [10]\n" +
"[end]";   
//ship is " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting csv
        //  written as 7-bit ASCII NCCSV strings to ISO-8859-1 (irrelevant)
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"ship,time,latitude,longitude,status,testLong,sst\n" +
",UTC,degrees_north,degrees_east,,1,degree_C\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-9223372036854775808,10.9\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-9007199254740992,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,9007199254740992,10.7\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",9223372036854775806,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\\u00fc,NaN,10.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN\n";        
        Test.ensureEqual(results, expected, "results=\n" + results);        
//ship is " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting csvp
        //  written as 7-bit ASCII NCCSV strings to ISO-8859-1 (irrelevant)
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".csvp"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"ship,time (UTC),latitude (degrees_north),longitude (degrees_east),status,testLong (1),sst (degree_C)\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-9223372036854775808,10.9\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-9007199254740992,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,9007199254740992,10.7\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",9223372036854775806,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\\u00fc,NaN,10.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN\n";        Test.ensureEqual(results, expected, "results=\n" + results);        
//ship is " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting csv0
        //  written as 7-bit ASCII NCCSV strings to ISO-8859-1 (irrelevant)
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".csv0"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-9223372036854775808,10.9\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-9007199254740992,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,9007199254740992,10.7\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",9223372036854775806,NaN\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\\u00fc,NaN,10.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN\n";        Test.ensureEqual(results, expected, "results=\n" + results);        
//ship is " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //das and dds tested above

        //*** getting dods
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".dods"); 
        results = String2.annotatedString(
            String2.directReadFrom88591File(dir + tName));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    String ship;[10]\n" +
"    Float64 time;[10]\n" +
"    Float64 latitude;[10]\n" +
"    Float64 longitude;[10]\n" +
"    String status;[10]\n" +
"    Float64 testLong;[10]\n" +
"    Float32 sst;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"[10]\n" +
"Data:[10]\n" +
"Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n" +
"'z\"?[0]A[214]4[198][163][0][0][0]@<[0][13][27]qu[142][192]`H>BZ[238]c[0][0][0][1]A[0][0][0][128][0][0][0][0][0][0][0]A.ffZ[0][0][0][0][0][0][11] a[9]~[252],[10]\n" +
"'z\"?[0]A[214]4[202]'[0][0][0]@<[0][19][169]*0U[192]`K[28]C,[165]z[0][0][0][1]?[0][0][0][255][251][157]*[195]uE@[127][192][0][0]Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n" +
"'z\"?[0]A[214]4[205][171][0][0][0]@<[0][6][141][184][186][199][192]`M[198][167][239][157][178][0][0][0][1][9][0][0][0][0][0][0][0][0][0][0][0]A+33Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n" +
"'z\"?[0]A[214]4[240][211][0][0][0]@;[255][242][228][142][138]r[192]`q[217][127]b[182][174][0][0][0][1]\"[0][0][0][0][4]b[213]<[138][186][192]B[198][0][0]Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n" +
"'z\"?[0]A[214]5[16]w[0][0][0]@<[0][19][169]*0U[192]`[128][11]x[3]F[220][0][0][0][1][252][0][0][0][127][255][255][255][255][255][255][254]A [0][0]Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n" +
"'z\"?[0]A[214]5[23][127][0][0][0]@<[0][13][27]qu[142][192]`[133][23]X[226][25]e[0][0][0][1]?[0][0][0][127][255][255][255][255][255][255][255][127][192][0][0][165][0][0][0][end]";
//ship is " a\t~\u00fc,\n'z""\u20AC".   20AC is being encoded with ISO-8859-1 as '?'
//source status chars are A\u20AC\t"\u00fc\uFFFF


        //*** getting esriCsv        written as 7bit ASCII via ISO-8859-1
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".esriCsv"); 
        results = String2.annotatedString(
            String2.directReadFromUtf8File(dir + tName));
        //String2.log(results);
        expected = 
"ship,date,time,Y,X,status,testLong,sst[10]\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,12:45:00 am,28.0002,-130.2576,A,-9223372036854775808,10.9[10]\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,1:45:00 am,28.0003,-130.3472,\\u20ac,-9007199254740992,-9999.0[10]\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,2:45:00 am,28.0001,-130.4305,\\t,9007199254740992,10.7[10]\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,12:45:00 pm,27.9998,-131.5578,\"\"\"\",9223372036854775806,-9999.0[10]\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,9:45:00 pm,28.0003,-132.0014,\\u00fc,-9999,10.0[10]\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,11:45:00 pm,28.0002,-132.1591,?,-9999,-9999.0[10]\n" +
"[end]";
//ship is " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting geoJson
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".geoJson"); 
        results = String2.annotatedString(
            String2.directReadFromUtf8File(dir + tName));
        //String2.log(results);
        expected = 
//This is JSON encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
"{[10]\n" +
"  \"type\": \"FeatureCollection\",[10]\n" +
"  \"propertyNames\": [\"ship\", \"time\", \"status\", \"testLong\", \"sst\"],[10]\n" +
"  \"propertyUnits\": [null, \"UTC\", null, \"1\", \"degree_C\"],[10]\n" +
"  \"features\": [[10]\n" +
"{\"type\": \"Feature\",[10]\n" +
"  \"geometry\": {[10]\n" +
"    \"type\": \"Point\",[10]\n" +
"    \"coordinates\": [-130.2576, 28.0002] },[10]\n" +
"  \"properties\": {[10]\n" +
"    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",[10]\n" +
"    \"time\": \"2017-03-23T00:45:00Z\",[10]\n" +
"    \"status\": \"A\",[10]\n" +
"    \"testLong\": -9223372036854775808,[10]\n" +
"    \"sst\": 10.9 }[10]\n" +
"},[10]\n" +
"{\"type\": \"Feature\",[10]\n" +
"  \"geometry\": {[10]\n" +
"    \"type\": \"Point\",[10]\n" +
"    \"coordinates\": [-130.3472, 28.0003] },[10]\n" +
"  \"properties\": {[10]\n" +
"    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",[10]\n" +
"    \"time\": \"2017-03-23T01:45:00Z\",[10]\n" +
"    \"status\": \"\\u20ac\",[10]\n" +
"    \"testLong\": -9007199254740992,[10]\n" +
"    \"sst\": null }[10]\n" +
"},[10]\n" +
"{\"type\": \"Feature\",[10]\n" +
"  \"geometry\": {[10]\n" +
"    \"type\": \"Point\",[10]\n" +
"    \"coordinates\": [-130.4305, 28.0001] },[10]\n" +
"  \"properties\": {[10]\n" +
"    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",[10]\n" +
"    \"time\": \"2017-03-23T02:45:00Z\",[10]\n" +
"    \"status\": \"\\t\",[10]\n" +
"    \"testLong\": 9007199254740992,[10]\n" +
"    \"sst\": 10.7 }[10]\n" +
"},[10]\n" +
"{\"type\": \"Feature\",[10]\n" +
"  \"geometry\": {[10]\n" +
"    \"type\": \"Point\",[10]\n" +
"    \"coordinates\": [-131.5578, 27.9998] },[10]\n" +
"  \"properties\": {[10]\n" +
"    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",[10]\n" +
"    \"time\": \"2017-03-23T12:45:00Z\",[10]\n" +
"    \"status\": \"\\\"\",[10]\n" +
"    \"testLong\": 9223372036854775806,[10]\n" +
"    \"sst\": null }[10]\n" +
"},[10]\n" +
"{\"type\": \"Feature\",[10]\n" +
"  \"geometry\": {[10]\n" +
"    \"type\": \"Point\",[10]\n" +
"    \"coordinates\": [-132.0014, 28.0003] },[10]\n" +
"  \"properties\": {[10]\n" +
"    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",[10]\n" +
"    \"time\": \"2017-03-23T21:45:00Z\",[10]\n" +
"    \"status\": \"\\u00fc\",[10]\n" +
"    \"testLong\": null,[10]\n" +
"    \"sst\": 10.0 }[10]\n" +
"},[10]\n" +
"{\"type\": \"Feature\",[10]\n" +
"  \"geometry\": {[10]\n" +
"    \"type\": \"Point\",[10]\n" +
"    \"coordinates\": [-132.1591, 28.0002] },[10]\n" +
"  \"properties\": {[10]\n" +
"    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",[10]\n" +
"    \"time\": \"2017-03-23T23:45:00Z\",[10]\n" +
"    \"status\": \"?\",[10]\n" +
"    \"testLong\": null,[10]\n" +
"    \"sst\": null }[10]\n" +
"}[10]\n" +
"  ],[10]\n" +
"  \"bbox\": [-132.1591, 27.9998, -130.2576, 28.0003][10]\n" +
"}[10]\n" +
"[end]";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting htmlTable
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".htmlTable"); 
        results = String2.directReadFromUtf8File(dir + tName);
        //String2.log(results);
        expected = 
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>ship\n" +
"<th>time\n" +
"<th>latitude\n" +
"<th>longitude\n" +
"<th>status\n" +
"<th>testLong\n" +
"<th>sst\n" +
"</tr>\n" +
"<tr>\n" +
"<th>\n" +
"<th>UTC\n" +
"<th>degrees_north\n" +
"<th>degrees_east\n" +
"<th>\n" +
"<th>1\n" +
"<th>degree_C\n" +
"</tr>\n" +
"<tr>\n" +
"<td>\u00A0a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n" +  // 00A0 is nbsp
"<td>2017-03-23T00:45:00Z\n" +
"<td class=\"R\">28.0002\n" +
"<td class=\"R\">-130.2576\n" +
"<td>A\n" +
"<td class=\"R\">-9223372036854775808\n" +
"<td class=\"R\">10.9\n" +
"</tr>\n" +
"<tr>\n" +
"<td>\u00A0a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n" +
"<td>2017-03-23T01:45:00Z\n" +
"<td class=\"R\">28.0003\n" +
"<td class=\"R\">-130.3472\n" +
"<td>&#x20ac;\n" +
"<td class=\"R\">-9007199254740992\n" +
"<td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>\u00A0a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n" +
"<td>2017-03-23T02:45:00Z\n" +
"<td class=\"R\">28.0001\n" +
"<td class=\"R\">-130.4305\n" +
"<td>\\t\n" +    //write tab as json \t
"<td class=\"R\">9007199254740992\n" +
"<td class=\"R\">10.7\n" +
"</tr>\n" +
"<tr>\n" +
"<td>\u00A0a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n" +
"<td>2017-03-23T12:45:00Z\n" +
"<td class=\"R\">27.9998\n" +
"<td class=\"R\">-131.5578\n" +
"<td>\\&quot;\n" +
"<td class=\"R\">9223372036854775806\n" +
"<td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>\u00A0a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n" +
"<td>2017-03-23T21:45:00Z\n" +
"<td class=\"R\">28.0003\n" +
"<td class=\"R\">-132.0014\n" +
"<td>&uuml;\n" +
"<td>\n" +
"<td class=\"R\">10.0\n" +
"</tr>\n" +
"<tr>\n" +
"<td>\u00A0a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n" +
"<td>2017-03-23T23:45:00Z\n" +
"<td class=\"R\">28.0002\n" +
"<td class=\"R\">-132.1591\n" +
"<td>?\n" +
"<td>\n" +
"<td>\n" +
"</tr>\n" +
"</table>";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        po = results.indexOf(expected.substring(0, 40));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);        

        //*** getting itx
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".itx"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, '\r', '\n');
        //String2.log(results);
        expected = 
"IGOR\n" +
"WAVES/T ship\n" +
"BEGIN\n" +
"\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n" +  // are nccsv strings the best choice?
"\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n" +
"\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n" +
"\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n" +
"\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n" +
"\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n" +
"END\n" +
"\n" +
"WAVES/D time2\n" +
"BEGIN\n" +
"3.5730747E9\n" +
"3.5730783E9\n" +
"3.5730819E9\n" +
"3.5731179E9\n" +
"3.5731503E9\n" +
"3.5731575E9\n" +
"END\n" +
"X SetScale d 3.5730747E9,3.5731575E9, \"dat\", time2\n" +
"\n" +
"WAVES/D latitude\n" +
"BEGIN\n" +
"28.0002\n" +
"28.0003\n" +
"28.0001\n" +
"27.9998\n" +
"28.0003\n" +
"28.0002\n" +
"END\n" +
"X SetScale d 27.9998,28.0003, \"degrees_north\", latitude\n" +
"\n" +
"WAVES/D longitude\n" +
"BEGIN\n" +
"-130.2576\n" +
"-130.3472\n" +
"-130.4305\n" +
"-131.5578\n" +
"-132.0014\n" +
"-132.1591\n" +
"END\n" +
"X SetScale d -132.1591,-130.2576, \"degrees_east\", longitude\n" +
"\n" +
"WAVES/T status\n" +
"BEGIN\n" +
"\"A\"\n" +          //source status chars are A\u20AC\t"\u00fc\uFFFF
"\"\\u20ac\"\n" +
"\"\\t\"\n" +
"\"\\\"\"\n" +
"\"\\u00fc\"\n" +
"\"?\"\n" +
"END\n" +
"\n" +
"WAVES/D testLong\n" +
"BEGIN\n" +
"-9223372036854775808\n" +
"-9007199254740992\n" +
"9007199254740992\n" +
"9223372036854775806\n" +
"NaN\n" +
"NaN\n" +
"END\n" +
    //these are largest consecutive longs that can round trip to doubles
    // 2019-04-05 was               9.2233720368547748E18  why the change?  adoptOpenJdk
"X SetScale d -9.223372036854776E18,9.223372036854776E18, \"1\", testLong\n" +
"\n" +
"WAVES/S sst\n" +
"BEGIN\n" +
"10.9\n" +
"NaN\n" +
"10.7\n" +
"NaN\n" +
"10.0\n" +
"NaN\n" +
"END\n" +
"X SetScale d 10.0,10.9, \"degree_C\", sst\n" +
"\n";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting json
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".json"); 
        results = String2.directReadFromUtf8File(dir + tName);
        //String2.log(results);
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"ship\", \"time\", \"latitude\", \"longitude\", \"status\", \"testLong\", \"sst\"],\n" +
"    \"columnTypes\": [\"String\", \"String\", \"double\", \"double\", \"char\", \"long\", \"float\"],\n" +
"    \"columnUnits\": [null, \"UTC\", \"degrees_north\", \"degrees_east\", null, \"1\", \"degree_C\"],\n" +
"    \"rows\": [\n" +
"      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T00:45:00Z\", 28.0002, -130.2576, \"A\", -9223372036854775808, 10.9],\n" +
"      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T01:45:00Z\", 28.0003, -130.3472, \"\\u20ac\", -9007199254740992, null],\n" +
"      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T02:45:00Z\", 28.0001, -130.4305, \"\\t\", 9007199254740992, 10.7],\n" +
"      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T12:45:00Z\", 27.9998, -131.5578, \"\\\"\", 9223372036854775806, null],\n" +
"      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T21:45:00Z\", 28.0003, -132.0014, \"\\u00fc\", null, 10],\n" +
"      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T23:45:00Z\", 28.0002, -132.1591, \"?\", null, null]\n" +
"    ]\n" +
"  }\n" +
"}\n";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting jsonlCSV1
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".jsonlCSV1"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"[\"ship\", \"time\", \"latitude\", \"longitude\", \"status\", \"testLong\", \"sst\"]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T00:45:00Z\", 28.0002, -130.2576, \"A\", -9223372036854775808, 10.9]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T01:45:00Z\", 28.0003, -130.3472, \"\\u20ac\", -9007199254740992, null]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T02:45:00Z\", 28.0001, -130.4305, \"\\t\", 9007199254740992, 10.7]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T12:45:00Z\", 27.9998, -131.5578, \"\\\"\", 9223372036854775806, null]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T21:45:00Z\", 28.0003, -132.0014, \"\\u00fc\", null, 10]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T23:45:00Z\", 28.0002, -132.1591, \"?\", null, null]\n";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting jsonlCSV
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".jsonlCSV"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T00:45:00Z\", 28.0002, -130.2576, \"A\", -9223372036854775808, 10.9]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T01:45:00Z\", 28.0003, -130.3472, \"\\u20ac\", -9007199254740992, null]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T02:45:00Z\", 28.0001, -130.4305, \"\\t\", 9007199254740992, 10.7]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T12:45:00Z\", 27.9998, -131.5578, \"\\\"\", 9223372036854775806, null]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T21:45:00Z\", 28.0003, -132.0014, \"\\u00fc\", null, 10]\n" +
"[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T23:45:00Z\", 28.0002, -132.1591, \"?\", null, null]\n";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting jsonlKVP
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".jsonlKVP"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T00:45:00Z\", \"latitude\":28.0002, \"longitude\":-130.2576, \"status\":\"A\", \"testLong\":-9223372036854775808, \"sst\":10.9}\n" +
"{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T01:45:00Z\", \"latitude\":28.0003, \"longitude\":-130.3472, \"status\":\"\\u20ac\", \"testLong\":-9007199254740992, \"sst\":null}\n" +
"{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T02:45:00Z\", \"latitude\":28.0001, \"longitude\":-130.4305, \"status\":\"\\t\", \"testLong\":9007199254740992, \"sst\":10.7}\n" +
"{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T12:45:00Z\", \"latitude\":27.9998, \"longitude\":-131.5578, \"status\":\"\\\"\", \"testLong\":9223372036854775806, \"sst\":null}\n" +
"{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T21:45:00Z\", \"latitude\":28.0003, \"longitude\":-132.0014, \"status\":\"\\u00fc\", \"testLong\":null, \"sst\":10}\n" +
"{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T23:45:00Z\", \"latitude\":28.0002, \"longitude\":-132.1591, \"status\":\"?\", \"testLong\":null, \"sst\":null}\n";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

/*        //*** getting mat
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".mat"); 
        results = String2.annotatedString(String2.directReadFrom88591File(dir + tName));
        //String2.log(results);
        expected = 
"zztop";
        Test.ensureEqual(results, expected, "results=\n" + results);        
*/
        //*** getting nc   and ncHeader
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".nc"); 
        results = String2.annotatedString(NcHelper.ncdump(dir + tName, ""));
        //String2.log(results);
        expected = 
"netcdf EDDTableFromNccsvFiles_char.nc {[10]\n" +
"  dimensions:[10]\n" +
"    row = 6;[10]\n" +
"    ship_strlen = 11;[10]\n" +
"  variables:[10]\n" +
"    char ship(row=6, ship_strlen=11);[10]\n" +
"      :_Encoding = \"ISO-8859-1\";[10]\n" +
"      :cf_role = \"trajectory_id\";[10]\n" +
"      :ioos_category = \"Identifier\";[10]\n" +
"      :long_name = \"Ship\";[10]\n" +
"[10]\n" +
"    double time(row=6);[10]\n" +
"      :_CoordinateAxisType = \"Time\";[10]\n" +
"      :actual_range = 1.4902299E9, 1.4903127E9; // double[10]\n" +
"      :axis = \"T\";[10]\n" +
"      :ioos_category = \"Time\";[10]\n" +
"      :long_name = \"Time\";[10]\n" +
"      :standard_name = \"time\";[10]\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";[10]\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"[10]\n" +
"    double latitude(row=6);[10]\n" +
"      :_CoordinateAxisType = \"Lat\";[10]\n" +
"      :actual_range = 27.9998, 28.0003; // double[10]\n" +
"      :axis = \"Y\";[10]\n" +
"      :colorBarMaximum = 90.0; // double[10]\n" +
"      :colorBarMinimum = -90.0; // double[10]\n" +
"      :ioos_category = \"Location\";[10]\n" +
"      :long_name = \"Latitude\";[10]\n" +
"      :standard_name = \"latitude\";[10]\n" +
"      :units = \"degrees_north\";[10]\n" +
"[10]\n" +
"    double longitude(row=6);[10]\n" +
"      :_CoordinateAxisType = \"Lon\";[10]\n" +
"      :actual_range = -132.1591, -130.2576; // double[10]\n" +
"      :axis = \"X\";[10]\n" +
"      :colorBarMaximum = 180.0; // double[10]\n" +
"      :colorBarMinimum = -180.0; // double[10]\n" +
"      :ioos_category = \"Location\";[10]\n" +
"      :long_name = \"Longitude\";[10]\n" +
"      :standard_name = \"longitude\";[10]\n" +
"      :units = \"degrees_east\";[10]\n" +
"[10]\n" +
"    char status(row=6);[10]\n" +
"      :actual_range = \"\\t[8364]\";[10]\n" + 
//"      :charset = \"ISO-8859-1\";[10]\n" +
"      :comment = \"From http://some.url.gov/someProjectDocument , Table C\";[10]\n" +
"      :ioos_category = \"Unknown\";[10]\n" +
"      :long_name = \"Status\";[10]\n" +
"[10]\n" +
"    double testLong(row=6);[10]\n" +
"      :_FillValue = NaN; // double[10]\n" +
                  //these are largest consecutive longs that can round trip to doubles
                  //2019-04-05 max was 9.2233720368547748E18, now NaN   why???
"      :actual_range = -9.223372036854776E18, NaN; // double[10]\n" + //!!! max is wrong! calculated internally as double. max should be -9223372036854775806L
"      :ioos_category = \"Unknown\";[10]\n" +
"      :long_name = \"Test of Longs\";[10]\n" +
"      :units = \"1\";[10]\n" +
"[10]\n" +
"    float sst(row=6);[10]\n" +
"      :actual_range = 10.0f, 10.9f; // float[10]\n" +
"      :colorBarMaximum = 32.0; // double[10]\n" +
"      :colorBarMinimum = 0.0; // double[10]\n" +
"      :ioos_category = \"Temperature\";[10]\n" +
"      :long_name = \"Sea Surface Temperature\";[10]\n" +
"      :missing_value = 99.0f; // float[10]\n" +
"      :standard_name = \"sea_surface_temperature\";[10]\n" +
"      :testBytes = -128B, 0B, 127B; // byte[10]\n" +
"      :testChars = \",\\\"[8364]\";[10]\n" +   //[8364], so unicode!
"      :testDoubles = -1.7976931348623157E308, 0.0, 1.7976931348623157E308; // double[10]\n" +
"      :testFloats = -3.4028235E38f, 0.0f, 3.4028235E38f; // float[10]\n" +
"      :testInts = -2147483648, 0, 2147483647; // int[10]\n" +
          //these are largest consecutive longs that can round trip to doubles
"      :testLongs = -9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.2233720368547748E18, NaN; // double[10]\n" + 
"      :testShorts = -32768S, 0S, 32767S; // short[10]\n" +
"      :testStrings = \" a\\t~[252],[10]\n" +
"'z\\\"[8364]\";[10]\n" +   //[8364], so unicode!
"      :units = \"degree_C\";[10]\n" +
"[10]\n" +
"  // global attributes:[10]\n" +
"  :cdm_data_type = \"Trajectory\";[10]\n" +
"  :cdm_trajectory_variables = \"ship\";[10]\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\";[10]\n" +
"  :creator_email = \"bob.simons@noaa.gov\";[10]\n" +
"  :creator_name = \"Bob Simons\";[10]\n" +
"  :creator_type = \"person\";[10]\n" +
"  :creator_url = \"https://www.pfeg.noaa.gov\";[10]\n" +
"  :Easternmost_Easting = -130.2576; // double[10]\n" +
"  :featureType = \"Trajectory\";[10]\n" +
"  :geospatial_lat_max = 28.0003; // double[10]\n" +
"  :geospatial_lat_min = 27.9998; // double[10]\n" +
"  :geospatial_lat_units = \"degrees_north\";[10]\n" +
"  :geospatial_lon_max = -130.2576; // double[10]\n" +
"  :geospatial_lon_min = -132.1591; // double[10]\n" +
"  :geospatial_lon_units = \"degrees_east\";[10]\n" +
"  :history = \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
        
//        T18:32:36Z (local files)[10]\n" +
//"2017-04-21T18:32:36Z 
expected = 
"http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.nc\";[10]\n" +
"  :id = \"testNccsvScalar\";[10]\n" +
"  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\";[10]\n" +
"  :institution = \"NOAA NMFS SWFSC ERD, NOAA PMEL\";[10]\n" +
"  :keywords = \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\";[10]\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";[10]\n" +
"  :license = \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\";[10]\n" +
"  :Northernmost_Northing = 28.0003; // double[10]\n" +
"  :sourceUrl = \"(local files)\";[10]\n" +
"  :Southernmost_Northing = 27.9998; // double[10]\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v55\";[10]\n" +
"  :subsetVariables = \"ship, status, testLong\";[10]\n" +
"  :summary = \"This is a paragraph or two describing the dataset.\";[10]\n" +
"  :time_coverage_end = \"2017-03-23T23:45:00Z\";[10]\n" +
"  :time_coverage_start = \"2017-03-23T00:45:00Z\";[10]\n" +
"  :title = \"NCCSV Demonstration\";[10]\n" +
"  :Westernmost_Easting = -132.1591; // double[10]\n" +
" data:[10]\n" +
"ship =\" a[9]~[252],[10]\n" +            // Is \n in middle of a value okay?
"'z\"?\", \" a[9]~[252],[10]\n" +
"'z\"?\", \" a[9]~[252],[10]\n" +
"'z\"?\", \" a[9]~[252],[10]\n" +
"'z\"?\", \" a[9]~[252],[10]\n" +
"'z\"?\", \" a[9]~[252],[10]\n" +
"'z\"?\"[10]\n" +
"time =[10]\n" +
"  {1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9}[10]\n" +
"latitude =[10]\n" +
"  {28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002}[10]\n" +
"longitude =[10]\n" +
"  {-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591}[10]\n" +
"status =  \"A?[9]\"[252]?\"[10]\n" +  //source status chars are A\u20AC\t"\u00fc\uFFFF
"testLong =[10]\n" +
    //these are largest doubles that can round trip to a long
"  {-9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.2233720368547748E18, NaN, NaN}[10]\n" + 
"sst =[10]\n" +
"  {10.9, NaN, 10.7, 99.0, 10.0, NaN}[10]\n" +
"}[10]\n" +
"[end]";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        tPo = results.indexOf(expected.substring(0, 40));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        //*** getting ncCF   and ncCFHeader
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".ncCF"); 
        results = String2.annotatedString(NcHelper.ncdump(dir + tName, ""));
        //String2.log(results);
        expected = 
"netcdf EDDTableFromNccsvFiles_char.nc {[10]\n" +
"  dimensions:[10]\n" +
"    trajectory = 1;[10]\n" +
"    obs = 6;[10]\n" +
"    ship_strlen = 11;[10]\n" +
"  variables:[10]\n" +
"    char ship(trajectory=1, ship_strlen=11);[10]\n" +
"      :_Encoding = \"ISO-8859-1\";[10]\n" +
"      :cf_role = \"trajectory_id\";[10]\n" +
"      :ioos_category = \"Identifier\";[10]\n" +
"      :long_name = \"Ship\";[10]\n" +
"[10]\n" +
"    int rowSize(trajectory=1);[10]\n" +
"      :ioos_category = \"Identifier\";[10]\n" +
"      :long_name = \"Number of Observations for this Trajectory\";[10]\n" +
"      :sample_dimension = \"obs\";[10]\n" +
"[10]\n" +
"    double time(obs=6);[10]\n" +
"      :_CoordinateAxisType = \"Time\";[10]\n" +
"      :actual_range = 1.4902299E9, 1.4903127E9; // double[10]\n" +
"      :axis = \"T\";[10]\n" +
"      :ioos_category = \"Time\";[10]\n" +
"      :long_name = \"Time\";[10]\n" +
"      :standard_name = \"time\";[10]\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";[10]\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"[10]\n" +
"    double latitude(obs=6);[10]\n" +
"      :_CoordinateAxisType = \"Lat\";[10]\n" +
"      :actual_range = 27.9998, 28.0003; // double[10]\n" +
"      :axis = \"Y\";[10]\n" +
"      :colorBarMaximum = 90.0; // double[10]\n" +
"      :colorBarMinimum = -90.0; // double[10]\n" +
"      :ioos_category = \"Location\";[10]\n" +
"      :long_name = \"Latitude\";[10]\n" +
"      :standard_name = \"latitude\";[10]\n" +
"      :units = \"degrees_north\";[10]\n" +
"[10]\n" +
"    double longitude(obs=6);[10]\n" +
"      :_CoordinateAxisType = \"Lon\";[10]\n" +
"      :actual_range = -132.1591, -130.2576; // double[10]\n" +
"      :axis = \"X\";[10]\n" +
"      :colorBarMaximum = 180.0; // double[10]\n" +
"      :colorBarMinimum = -180.0; // double[10]\n" +
"      :ioos_category = \"Location\";[10]\n" +
"      :long_name = \"Longitude\";[10]\n" +
"      :standard_name = \"longitude\";[10]\n" +
"      :units = \"degrees_east\";[10]\n" +
"[10]\n" +
"    char status(obs=6);[10]\n" +
"      :actual_range = \"\\t[8364]\";[10]\n" + //[8364], so unicode!
//"      :charset = \"ISO-8859-1\";[10]\n" +
"      :comment = \"From http://some.url.gov/someProjectDocument , Table C\";[10]\n" +
"      :coordinates = \"time latitude longitude\";[10]\n" +
"      :ioos_category = \"Unknown\";[10]\n" +
"      :long_name = \"Status\";[10]\n" +
"[10]\n" +
"    double testLong(obs=6);[10]\n" +
"      :_FillValue = NaN; // double[10]\n" +  
      //these are largest consecutive longs that can round trip to doubles
      //2019-04-05 max was 9.2233720368547748E18, now NaN
"      :actual_range = -9.223372036854776E18, NaN; // double[10]\n" +  //!!! max is wrong! calculated internally as double. max should be -9223372036854775806L
"      :coordinates = \"time latitude longitude\";[10]\n" +  //?
"      :ioos_category = \"Unknown\";[10]\n" +
"      :long_name = \"Test of Longs\";[10]\n" +
"      :units = \"1\";[10]\n" +
"[10]\n" +
"    float sst(obs=6);[10]\n" +
"      :actual_range = 10.0f, 10.9f; // float[10]\n" +
"      :colorBarMaximum = 32.0; // double[10]\n" +
"      :colorBarMinimum = 0.0; // double[10]\n" +
"      :coordinates = \"time latitude longitude\";[10]\n" +
"      :ioos_category = \"Temperature\";[10]\n" +
"      :long_name = \"Sea Surface Temperature\";[10]\n" +
"      :missing_value = 99.0f; // float[10]\n" +
"      :standard_name = \"sea_surface_temperature\";[10]\n" +
"      :testBytes = -128B, 0B, 127B; // byte[10]\n" +
"      :testChars = \",\\\"[8364]\";[10]\n" + //[8364], so unicode!
"      :testDoubles = -1.7976931348623157E308, 0.0, 1.7976931348623157E308; // double[10]\n" +
"      :testFloats = -3.4028235E38f, 0.0f, 3.4028235E38f; // float[10]\n" +
"      :testInts = -2147483648, 0, 2147483647; // int[10]\n" +
          //these are largest consecutive longs that can round trip to doubles
"      :testLongs = -9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.2233720368547748E18, NaN; // double[10]\n" + 
"      :testShorts = -32768S, 0S, 32767S; // short[10]\n" +
"      :testStrings = \" a\\t~[252],[10]\n" +
"'z\\\"[8364]\";[10]\n" +  //[8364], so unicode!
"      :units = \"degree_C\";[10]\n" +
"[10]\n" +
"  // global attributes:[10]\n" +
"  :cdm_data_type = \"Trajectory\";[10]\n" +
"  :cdm_trajectory_variables = \"ship\";[10]\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\";[10]\n" +
"  :creator_email = \"bob.simons@noaa.gov\";[10]\n" +
"  :creator_name = \"Bob Simons\";[10]\n" +
"  :creator_type = \"person\";[10]\n" +
"  :creator_url = \"https://www.pfeg.noaa.gov\";[10]\n" +
"  :Easternmost_Easting = -130.2576; // double[10]\n" +
"  :featureType = \"Trajectory\";[10]\n" +
"  :geospatial_lat_max = 28.0003; // double[10]\n" +
"  :geospatial_lat_min = 27.9998; // double[10]\n" +
"  :geospatial_lat_units = \"degrees_north\";[10]\n" +
"  :geospatial_lon_max = -130.2576; // double[10]\n" +
"  :geospatial_lon_min = -132.1591; // double[10]\n" +
"  :geospatial_lon_units = \"degrees_east\";[10]\n" +
"  :history = \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
expected = 
"http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.ncCF\";[10]\n" +
"  :id = \"testNccsvScalar\";[10]\n" +
"  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\";[10]\n" +
"  :institution = \"NOAA NMFS SWFSC ERD, NOAA PMEL\";[10]\n" +
"  :keywords = \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\";[10]\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";[10]\n" +
"  :license = \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\";[10]\n" +
"  :Northernmost_Northing = 28.0003; // double[10]\n" +
"  :sourceUrl = \"(local files)\";[10]\n" +
"  :Southernmost_Northing = 27.9998; // double[10]\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v55\";[10]\n" +
"  :subsetVariables = \"ship, status, testLong\";[10]\n" +
"  :summary = \"This is a paragraph or two describing the dataset.\";[10]\n" +
"  :time_coverage_end = \"2017-03-23T23:45:00Z\";[10]\n" +
"  :time_coverage_start = \"2017-03-23T00:45:00Z\";[10]\n" +
"  :title = \"NCCSV Demonstration\";[10]\n" +
"  :Westernmost_Easting = -132.1591; // double[10]\n" +
" data:[10]\n" +
"ship =\" a[9]~[252],[10]\n" +     //ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
"'z\"?\"[10]\n" +             
"rowSize =[10]\n" +
"  {6}[10]\n" +
"time =[10]\n" +
"  {1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9}[10]\n" +
"latitude =[10]\n" +
"  {28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002}[10]\n" +
"longitude =[10]\n" +
"  {-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591}[10]\n" +
"status =  \"A?[9]\"[252]?\"[10]\n" + // source status chars are A\u20AC\t"\u00fc\uFFFF
"testLong =[10]\n" +
          //these are largest consecutive longs that can round trip to doubles
"  {-9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.2233720368547748E18, NaN, NaN}[10]\n" +
"sst =[10]\n" +
"  {10.9, NaN, 10.7, 99.0, 10.0, NaN}[10]\n" +
"}[10]\n" +
"[end]";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        tPo = results.indexOf(expected.substring(0, 40));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        //*** getting ncCFMA   and ncCFMAHeader
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".ncCFMA"); 
        results = String2.annotatedString(NcHelper.ncdump(dir + tName, ""));
        //String2.log(results);
        expected = 
"netcdf EDDTableFromNccsvFiles_char.nc {[10]\n" +
"  dimensions:[10]\n" +
"    trajectory = 1;[10]\n" +
"    obs = 6;[10]\n" +
"    ship_strlen = 11;[10]\n" +
"  variables:[10]\n" +
"    char ship(trajectory=1, ship_strlen=11);[10]\n" +
"      :_Encoding = \"ISO-8859-1\";[10]\n" +
"      :cf_role = \"trajectory_id\";[10]\n" +
"      :ioos_category = \"Identifier\";[10]\n" +
"      :long_name = \"Ship\";[10]\n" +
"[10]\n" +
"    double time(trajectory=1, obs=6);[10]\n" +
"      :_CoordinateAxisType = \"Time\";[10]\n" +
"      :_FillValue = NaN; // double[10]\n" +
"      :actual_range = 1.4902299E9, 1.4903127E9; // double[10]\n" +
"      :axis = \"T\";[10]\n" +
"      :ioos_category = \"Time\";[10]\n" +
"      :long_name = \"Time\";[10]\n" +
"      :standard_name = \"time\";[10]\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";[10]\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"[10]\n" +
"    double latitude(trajectory=1, obs=6);[10]\n" +
"      :_CoordinateAxisType = \"Lat\";[10]\n" +
"      :_FillValue = NaN; // double[10]\n" +
"      :actual_range = 27.9998, 28.0003; // double[10]\n" +
"      :axis = \"Y\";[10]\n" +
"      :colorBarMaximum = 90.0; // double[10]\n" +
"      :colorBarMinimum = -90.0; // double[10]\n" +
"      :ioos_category = \"Location\";[10]\n" +
"      :long_name = \"Latitude\";[10]\n" +
"      :standard_name = \"latitude\";[10]\n" +
"      :units = \"degrees_north\";[10]\n" +
"[10]\n" +
"    double longitude(trajectory=1, obs=6);[10]\n" +
"      :_CoordinateAxisType = \"Lon\";[10]\n" +
"      :_FillValue = NaN; // double[10]\n" +
"      :actual_range = -132.1591, -130.2576; // double[10]\n" +
"      :axis = \"X\";[10]\n" +
"      :colorBarMaximum = 180.0; // double[10]\n" +
"      :colorBarMinimum = -180.0; // double[10]\n" +
"      :ioos_category = \"Location\";[10]\n" +
"      :long_name = \"Longitude\";[10]\n" +
"      :standard_name = \"longitude\";[10]\n" +
"      :units = \"degrees_east\";[10]\n" +
"[10]\n" +
"    char status(trajectory=1, obs=6);[10]\n" +
"      :_FillValue = \"[65535]\";[10]\n" +
"      :actual_range = \"\\t[8364]\";[10]\n" +
//"      :charset = \"ISO-8859-1\";[10]\n" +
"      :comment = \"From http://some.url.gov/someProjectDocument , Table C\";[10]\n" +
"      :coordinates = \"time latitude longitude\";[10]\n" +
"      :ioos_category = \"Unknown\";[10]\n" +
"      :long_name = \"Status\";[10]\n" +
"[10]\n" +
"    double testLong(trajectory=1, obs=6);[10]\n" +
"      :_FillValue = NaN; // double[10]\n" +
          //these are largest consecutive longs that can round trip to doubles
"      :actual_range = -9.223372036854776E18, NaN; // double[10]\n" + //!!! max is wrong! calculated internally as double. max should be -9223372036854775806L
"      :coordinates = \"time latitude longitude\";[10]\n" +
"      :ioos_category = \"Unknown\";[10]\n" +
"      :long_name = \"Test of Longs\";[10]\n" +
"      :units = \"1\";[10]\n" +
"[10]\n" +
"    float sst(trajectory=1, obs=6);[10]\n" +
"      :actual_range = 10.0f, 10.9f; // float[10]\n" +
"      :colorBarMaximum = 32.0; // double[10]\n" +
"      :colorBarMinimum = 0.0; // double[10]\n" +
"      :coordinates = \"time latitude longitude\";[10]\n" +
"      :ioos_category = \"Temperature\";[10]\n" +
"      :long_name = \"Sea Surface Temperature\";[10]\n" +
"      :missing_value = 99.0f; // float[10]\n" +
"      :standard_name = \"sea_surface_temperature\";[10]\n" +
"      :testBytes = -128B, 0B, 127B; // byte[10]\n" +
"      :testChars = \",\\\"[8364]\";[10]\n" + //so unicode!
"      :testDoubles = -1.7976931348623157E308, 0.0, 1.7976931348623157E308; // double[10]\n" +
"      :testFloats = -3.4028235E38f, 0.0f, 3.4028235E38f; // float[10]\n" +
"      :testInts = -2147483648, 0, 2147483647; // int[10]\n" +
          //these are largest longs that can round trip to double
"      :testLongs = -9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.2233720368547748E18, NaN; // double[10]\n" +
"      :testShorts = -32768S, 0S, 32767S; // short[10]\n" +
"      :testStrings = \" a\\t~[252],[10]\n" +
"'z\\\"[8364]\";[10]\n" + //[8364], so unicode!
"      :units = \"degree_C\";[10]\n" +
"[10]\n" +
"  // global attributes:[10]\n" +
"  :cdm_data_type = \"Trajectory\";[10]\n" +
"  :cdm_trajectory_variables = \"ship\";[10]\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\";[10]\n" +
"  :creator_email = \"bob.simons@noaa.gov\";[10]\n" +
"  :creator_name = \"Bob Simons\";[10]\n" +
"  :creator_type = \"person\";[10]\n" +
"  :creator_url = \"https://www.pfeg.noaa.gov\";[10]\n" +
"  :Easternmost_Easting = -130.2576; // double[10]\n" +
"  :featureType = \"Trajectory\";[10]\n" +
"  :geospatial_lat_max = 28.0003; // double[10]\n" +
"  :geospatial_lat_min = 27.9998; // double[10]\n" +
"  :geospatial_lat_units = \"degrees_north\";[10]\n" +
"  :geospatial_lon_max = -130.2576; // double[10]\n" +
"  :geospatial_lon_min = -132.1591; // double[10]\n" +
"  :geospatial_lon_units = \"degrees_east\";[10]\n" +
"  :history = \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
expected = 
"http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.ncCFMA\";[10]\n" +
"  :id = \"testNccsvScalar\";[10]\n" +
"  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\";[10]\n" +
"  :institution = \"NOAA NMFS SWFSC ERD, NOAA PMEL\";[10]\n" +
"  :keywords = \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\";[10]\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";[10]\n" +
"  :license = \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\";[10]\n" +
"  :Northernmost_Northing = 28.0003; // double[10]\n" +
"  :sourceUrl = \"(local files)\";[10]\n" +
"  :Southernmost_Northing = 27.9998; // double[10]\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v55\";[10]\n" +
"  :subsetVariables = \"ship, status, testLong\";[10]\n" +
"  :summary = \"This is a paragraph or two describing the dataset.\";[10]\n" +
"  :time_coverage_end = \"2017-03-23T23:45:00Z\";[10]\n" +
"  :time_coverage_start = \"2017-03-23T00:45:00Z\";[10]\n" +
"  :title = \"NCCSV Demonstration\";[10]\n" +
"  :Westernmost_Easting = -132.1591; // double[10]\n" +
" data:[10]\n" +
"ship =\" a[9]~[252],[10]\n" +
"'z\"?\"[10]\n" +            
"time =[10]\n" +
"  {[10]\n" +
"    {1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9}[10]\n" +
"  }[10]\n" +
"latitude =[10]\n" +
"  {[10]\n" +
"    {28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002}[10]\n" +
"  }[10]\n" +
"longitude =[10]\n" +
"  {[10]\n" +
"    {-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591}[10]\n" +
"  }[10]\n" +
"status =\"A?[9]\"[252]?\"[10]\n" +
"testLong =[10]\n" +
"  {[10]\n" +
          //these are largest consecutive longs that can round trip to doubles
"    {-9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.2233720368547748E18, NaN, NaN}[10]\n" +
"  }[10]\n" +
"sst =[10]\n" +
"  {[10]\n" +
"    {10.9, NaN, 10.7, 99.0, 10.0, NaN}[10]\n" +
"  }[10]\n" +
"}[10]\n" +
"[end]";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        tPo = results.indexOf(expected.substring(0, 40));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);


        //*** getting ncoJson
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".ncoJson"); 
        results = String2.annotatedString(
            String2.directReadFromFile(dir + tName, String2.UTF_8));
        //2017-08-03 I tested the resulting file for validity at https://jsonlint.com/
        String2.log(">> NCO JSON " + dir + tName);
        //String2.log(results);
        expected = 
"{[10]\n" +
"  \"attributes\": {[10]\n" +
"    \"cdm_data_type\": {\"type\": \"char\", \"data\": \"Trajectory\"},[10]\n" +
"    \"cdm_trajectory_variables\": {\"type\": \"char\", \"data\": \"ship\"},[10]\n" +
"    \"Conventions\": {\"type\": \"char\", \"data\": \"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"},[10]\n" +
"    \"creator_email\": {\"type\": \"char\", \"data\": \"bob.simons@noaa.gov\"},[10]\n" +
"    \"creator_name\": {\"type\": \"char\", \"data\": \"Bob Simons\"},[10]\n" +
"    \"creator_type\": {\"type\": \"char\", \"data\": \"person\"},[10]\n" +
"    \"creator_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},[10]\n" +
"    \"Easternmost_Easting\": {\"type\": \"double\", \"data\": -130.2576},[10]\n" +
"    \"featureType\": {\"type\": \"char\", \"data\": \"Trajectory\"},[10]\n" +
"    \"geospatial_lat_max\": {\"type\": \"double\", \"data\": 28.0003},[10]\n" +
"    \"geospatial_lat_min\": {\"type\": \"double\", \"data\": 27.9998},[10]\n" +
"    \"geospatial_lat_units\": {\"type\": \"char\", \"data\": \"degrees_north\"},[10]\n" +
"    \"geospatial_lon_max\": {\"type\": \"double\", \"data\": -130.2576},[10]\n" +
"    \"geospatial_lon_min\": {\"type\": \"double\", \"data\": -132.1591},[10]\n" +
"    \"geospatial_lon_units\": {\"type\": \"char\", \"data\": \"degrees_east\"},[10]\n" +
"    \"history\": {\"type\": \"char\", \"data\": \"";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

//        2017-07-28T15:33:25Z (local files)\\n2017-07-28T15:33:25Z 
expected = "http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.ncoJson\"},[10]\n" +
"    \"infoUrl\": {\"type\": \"char\", \"data\": \"https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\"},[10]\n" +
"    \"institution\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD, NOAA PMEL\"},[10]\n" +
"    \"keywords\": {\"type\": \"char\", \"data\": \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"},[10]\n" +
"    \"keywords_vocabulary\": {\"type\": \"char\", \"data\": \"GCMD Science Keywords\"},[10]\n" +
"    \"license\": {\"type\": \"char\", \"data\": \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"},[10]\n" +
"    \"Northernmost_Northing\": {\"type\": \"double\", \"data\": 28.0003},[10]\n" +
"    \"sourceUrl\": {\"type\": \"char\", \"data\": \"(local files)\"},[10]\n" +
"    \"Southernmost_Northing\": {\"type\": \"double\", \"data\": 27.9998},[10]\n" +
"    \"standard_name_vocabulary\": {\"type\": \"char\", \"data\": \"CF Standard Name Table v55\"},[10]\n" +
"    \"subsetVariables\": {\"type\": \"char\", \"data\": \"ship, status, testLong\"},[10]\n" +
"    \"summary\": {\"type\": \"char\", \"data\": \"This is a paragraph or two describing the dataset.\"},[10]\n" +
"    \"time_coverage_end\": {\"type\": \"char\", \"data\": \"2017-03-23T23:45:00Z\"},[10]\n" +
"    \"time_coverage_start\": {\"type\": \"char\", \"data\": \"2017-03-23T00:45:00Z\"},[10]\n" +
"    \"title\": {\"type\": \"char\", \"data\": \"NCCSV Demonstration\"},[10]\n" +
"    \"Westernmost_Easting\": {\"type\": \"double\", \"data\": -132.1591}[10]\n" +
"  },[10]\n" +
"  \"dimensions\": {[10]\n" +
"    \"row\": 6,[10]\n" +
"    \"ship_strlen\": 11[10]\n" +
"  },[10]\n" +
"  \"variables\": {[10]\n" +
"    \"ship\": {[10]\n" +
"      \"shape\": [\"row\", \"ship_strlen\"],[10]\n" +
"      \"type\": \"char\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"cf_role\": {\"type\": \"char\", \"data\": \"trajectory_id\"},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Identifier\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Ship\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"]][10]\n" +
"    },[10]\n" +
"    \"time\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"double\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n" +
"        \"actual_range\": {\"type\": \"double\", \"data\": [1.4902299E9, 1.4903127E9]},[10]\n" +
"        \"axis\": {\"type\": \"char\", \"data\": \"T\"},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n" +
"        \"standard_name\": {\"type\": \"char\", \"data\": \"time\"},[10]\n" +
"        \"time_origin\": {\"type\": \"char\", \"data\": \"01-JAN-1970 00:00:00\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"seconds since 1970-01-01T00:00:00Z\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9][10]\n" +
"    },[10]\n" +
"    \"latitude\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"double\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lat\"},[10]\n" +
"        \"actual_range\": {\"type\": \"double\", \"data\": [27.9998, 28.0003]},[10]\n" +
"        \"axis\": {\"type\": \"char\", \"data\": \"Y\"},[10]\n" +
"        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 90.0},[10]\n" +
"        \"colorBarMinimum\": {\"type\": \"double\", \"data\": -90.0},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Latitude\"},[10]\n" +
"        \"standard_name\": {\"type\": \"char\", \"data\": \"latitude\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"degrees_north\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002][10]\n" +
"    },[10]\n" +
"    \"longitude\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"double\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lon\"},[10]\n" +
"        \"actual_range\": {\"type\": \"double\", \"data\": [-132.1591, -130.2576]},[10]\n" +
"        \"axis\": {\"type\": \"char\", \"data\": \"X\"},[10]\n" +
"        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 180.0},[10]\n" +
"        \"colorBarMinimum\": {\"type\": \"double\", \"data\": -180.0},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Longitude\"},[10]\n" +
"        \"standard_name\": {\"type\": \"char\", \"data\": \"longitude\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"degrees_east\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591][10]\n" +
"    },[10]\n" +
"    \"status\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"char\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"actual_range\": {\"type\": \"char\", \"data\": \"\\t\\n\\u20ac\"},[10]\n" +
"        \"comment\": {\"type\": \"char\", \"data\": \"From http://some.url.gov/someProjectDocument , Table C\"},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Status\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [\"A\\u20ac\\t\\\"\\u00fc?\"][10]\n" +
"    },[10]\n" +
"    \"testLong\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"int64\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"_FillValue\": {\"type\": \"int64\", \"data\": null},[10]\n" +
"        \"actual_range\": {\"type\": \"int64\", \"data\": [-9223372036854775808, null]},[10]\n" +  //!!! max is wrong! calculated internally as double. max should be -9223372036854775806L
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Test of Longs\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [-9223372036854775808, -9007199254740992, 9007199254740992, 9223372036854775806, null, null][10]\n" +
"    },[10]\n" +
"    \"sst\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"float\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"actual_range\": {\"type\": \"float\", \"data\": [10.0, 10.9]},[10]\n" +
"        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 32.0},[10]\n" +
"        \"colorBarMinimum\": {\"type\": \"double\", \"data\": 0.0},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Temperature\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Sea Surface Temperature\"},[10]\n" +
"        \"missing_value\": {\"type\": \"float\", \"data\": 99.0},[10]\n" +
"        \"standard_name\": {\"type\": \"char\", \"data\": \"sea_surface_temperature\"},[10]\n" +
"        \"testBytes\": {\"type\": \"byte\", \"data\": [-128, 0, null]},[10]\n" +
"        \"testChars\": {\"type\": \"char\", \"data\": \",\\n\\\"\\n\\u20ac\"},[10]\n" +
"        \"testDoubles\": {\"type\": \"double\", \"data\": [-1.7976931348623157E308, 0.0, 1.7976931348623157E308]},[10]\n" +
"        \"testFloats\": {\"type\": \"float\", \"data\": [-3.4028235E38, 0.0, 3.4028235E38]},[10]\n" +
"        \"testInts\": {\"type\": \"int\", \"data\": [-2147483648, 0, null]},[10]\n" +
"        \"testLongs\": {\"type\": \"int64\", \"data\": [-9223372036854775808, -9007199254740992, 9007199254740992, 9223372036854775806, null]},[10]\n" +
"        \"testShorts\": {\"type\": \"short\", \"data\": [-32768, 0, null]},[10]\n" +
"        \"testStrings\": {\"type\": \"char\", \"data\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"degree_C\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [10.9, null, 10.7, 99.0, 10.0, null][10]\n" +
"    }[10]\n" +
"  }[10]\n" +
"}[10]\n" +
"[end]";
        po = results.indexOf(expected.substring(0, 20));        
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);        


        //*** getting ncoJson with jsonp
        tName = eddTable.makeNewFileForDapQuery(null, null, "&.jsonp=myFunctionName", dir, 
            eddTable.className() + "_charjp", ".ncoJson"); 
        results = String2.annotatedString(
            String2.directReadFromFile(dir + tName, String2.UTF_8));
        //String2.log(results);
        expected = "myFunctionName(" + 
"{[10]\n" +
"  \"attributes\": {[10]\n" +
"    \"cdm_data_type\": {\"type\": \"char\", \"data\": \"Trajectory\"},[10]\n" +
"    \"cdm_trajectory_variables\": {\"type\": \"char\", \"data\": \"ship\"},[10]\n" +
"    \"Conventions\": {\"type\": \"char\", \"data\": \"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"},[10]\n" +
"    \"creator_email\": {\"type\": \"char\", \"data\": \"bob.simons@noaa.gov\"},[10]\n" +
"    \"creator_name\": {\"type\": \"char\", \"data\": \"Bob Simons\"},[10]\n" +
"    \"creator_type\": {\"type\": \"char\", \"data\": \"person\"},[10]\n" +
"    \"creator_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},[10]\n" +
"    \"Easternmost_Easting\": {\"type\": \"double\", \"data\": -130.2576},[10]\n" +
"    \"featureType\": {\"type\": \"char\", \"data\": \"Trajectory\"},[10]\n" +
"    \"geospatial_lat_max\": {\"type\": \"double\", \"data\": 28.0003},[10]\n" +
"    \"geospatial_lat_min\": {\"type\": \"double\", \"data\": 27.9998},[10]\n" +
"    \"geospatial_lat_units\": {\"type\": \"char\", \"data\": \"degrees_north\"},[10]\n" +
"    \"geospatial_lon_max\": {\"type\": \"double\", \"data\": -130.2576},[10]\n" +
"    \"geospatial_lon_min\": {\"type\": \"double\", \"data\": -132.1591},[10]\n" +
"    \"geospatial_lon_units\": {\"type\": \"char\", \"data\": \"degrees_east\"},[10]\n" +
"    \"history\": {\"type\": \"char\", \"data\": \"";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

//        2017-07-28T15:33:25Z (local files)\\n2017-07-28T15:33:25Z 
expected = "http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.ncoJson?&.jsonp=myFunctionName\"},[10]\n" +
"    \"infoUrl\": {\"type\": \"char\", \"data\": \"https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\"},[10]\n" +
"    \"institution\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD, NOAA PMEL\"},[10]\n" +
"    \"keywords\": {\"type\": \"char\", \"data\": \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"},[10]\n" +
"    \"keywords_vocabulary\": {\"type\": \"char\", \"data\": \"GCMD Science Keywords\"},[10]\n" +
"    \"license\": {\"type\": \"char\", \"data\": \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"},[10]\n" +
"    \"Northernmost_Northing\": {\"type\": \"double\", \"data\": 28.0003},[10]\n" +
"    \"sourceUrl\": {\"type\": \"char\", \"data\": \"(local files)\"},[10]\n" +
"    \"Southernmost_Northing\": {\"type\": \"double\", \"data\": 27.9998},[10]\n" +
"    \"standard_name_vocabulary\": {\"type\": \"char\", \"data\": \"CF Standard Name Table v55\"},[10]\n" +
"    \"subsetVariables\": {\"type\": \"char\", \"data\": \"ship, status, testLong\"},[10]\n" +
"    \"summary\": {\"type\": \"char\", \"data\": \"This is a paragraph or two describing the dataset.\"},[10]\n" +
"    \"time_coverage_end\": {\"type\": \"char\", \"data\": \"2017-03-23T23:45:00Z\"},[10]\n" +
"    \"time_coverage_start\": {\"type\": \"char\", \"data\": \"2017-03-23T00:45:00Z\"},[10]\n" +
"    \"title\": {\"type\": \"char\", \"data\": \"NCCSV Demonstration\"},[10]\n" +
"    \"Westernmost_Easting\": {\"type\": \"double\", \"data\": -132.1591}[10]\n" +
"  },[10]\n" +
"  \"dimensions\": {[10]\n" +
"    \"row\": 6,[10]\n" +
"    \"ship_strlen\": 11[10]\n" +
"  },[10]\n" +
"  \"variables\": {[10]\n" +
"    \"ship\": {[10]\n" +
"      \"shape\": [\"row\", \"ship_strlen\"],[10]\n" +
"      \"type\": \"char\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"cf_role\": {\"type\": \"char\", \"data\": \"trajectory_id\"},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Identifier\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Ship\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"]][10]\n" +
"    },[10]\n" +
"    \"time\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"double\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n" +
"        \"actual_range\": {\"type\": \"double\", \"data\": [1.4902299E9, 1.4903127E9]},[10]\n" +
"        \"axis\": {\"type\": \"char\", \"data\": \"T\"},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n" +
"        \"standard_name\": {\"type\": \"char\", \"data\": \"time\"},[10]\n" +
"        \"time_origin\": {\"type\": \"char\", \"data\": \"01-JAN-1970 00:00:00\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"seconds since 1970-01-01T00:00:00Z\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9][10]\n" +
"    },[10]\n" +
"    \"latitude\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"double\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lat\"},[10]\n" +
"        \"actual_range\": {\"type\": \"double\", \"data\": [27.9998, 28.0003]},[10]\n" +
"        \"axis\": {\"type\": \"char\", \"data\": \"Y\"},[10]\n" +
"        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 90.0},[10]\n" +
"        \"colorBarMinimum\": {\"type\": \"double\", \"data\": -90.0},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Latitude\"},[10]\n" +
"        \"standard_name\": {\"type\": \"char\", \"data\": \"latitude\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"degrees_north\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002][10]\n" +
"    },[10]\n" +
"    \"longitude\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"double\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lon\"},[10]\n" +
"        \"actual_range\": {\"type\": \"double\", \"data\": [-132.1591, -130.2576]},[10]\n" +
"        \"axis\": {\"type\": \"char\", \"data\": \"X\"},[10]\n" +
"        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 180.0},[10]\n" +
"        \"colorBarMinimum\": {\"type\": \"double\", \"data\": -180.0},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Longitude\"},[10]\n" +
"        \"standard_name\": {\"type\": \"char\", \"data\": \"longitude\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"degrees_east\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591][10]\n" +
"    },[10]\n" +
"    \"status\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"char\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"actual_range\": {\"type\": \"char\", \"data\": \"\\t\\n\\u20ac\"},[10]\n" +
"        \"comment\": {\"type\": \"char\", \"data\": \"From http://some.url.gov/someProjectDocument , Table C\"},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Status\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [\"A\\u20ac\\t\\\"\\u00fc?\"][10]\n" +
"    },[10]\n" +
"    \"testLong\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"int64\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"_FillValue\": {\"type\": \"int64\", \"data\": null},[10]\n" +
"        \"actual_range\": {\"type\": \"int64\", \"data\": [-9223372036854775808, null]},[10]\n" + //!!! max is wrong! calculated internally as double. max should be -9223372036854775806L
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Test of Longs\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [-9223372036854775808, -9007199254740992, 9007199254740992, 9223372036854775806, null, null][10]\n" +
"    },[10]\n" +
"    \"sst\": {[10]\n" +
"      \"shape\": [\"row\"],[10]\n" +
"      \"type\": \"float\",[10]\n" +
"      \"attributes\": {[10]\n" +
"        \"actual_range\": {\"type\": \"float\", \"data\": [10.0, 10.9]},[10]\n" +
"        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 32.0},[10]\n" +
"        \"colorBarMinimum\": {\"type\": \"double\", \"data\": 0.0},[10]\n" +
"        \"ioos_category\": {\"type\": \"char\", \"data\": \"Temperature\"},[10]\n" +
"        \"long_name\": {\"type\": \"char\", \"data\": \"Sea Surface Temperature\"},[10]\n" +
"        \"missing_value\": {\"type\": \"float\", \"data\": 99.0},[10]\n" +
"        \"standard_name\": {\"type\": \"char\", \"data\": \"sea_surface_temperature\"},[10]\n" +
"        \"testBytes\": {\"type\": \"byte\", \"data\": [-128, 0, null]},[10]\n" +
"        \"testChars\": {\"type\": \"char\", \"data\": \",\\n\\\"\\n\\u20ac\"},[10]\n" +
"        \"testDoubles\": {\"type\": \"double\", \"data\": [-1.7976931348623157E308, 0.0, 1.7976931348623157E308]},[10]\n" +
"        \"testFloats\": {\"type\": \"float\", \"data\": [-3.4028235E38, 0.0, 3.4028235E38]},[10]\n" +
"        \"testInts\": {\"type\": \"int\", \"data\": [-2147483648, 0, null]},[10]\n" +
"        \"testLongs\": {\"type\": \"int64\", \"data\": [-9223372036854775808, -9007199254740992, 9007199254740992, 9223372036854775806, null]},[10]\n" +
"        \"testShorts\": {\"type\": \"short\", \"data\": [-32768, 0, null]},[10]\n" +
"        \"testStrings\": {\"type\": \"char\", \"data\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"},[10]\n" +
"        \"units\": {\"type\": \"char\", \"data\": \"degree_C\"}[10]\n" +
"      },[10]\n" +
"      \"data\": [10.9, null, 10.7, 99.0, 10.0, null][10]\n" +
"    }[10]\n" +
"  }[10]\n" +
"}[10]\n" +
")[end]";
        po = results.indexOf(expected.substring(0, 20));        
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);        


        //*** getting odvTxt
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".odvTxt"); 
        results = String2.annotatedString(
            String2.directReadFrom88591File(dir + tName));
        //String2.log(results);
        expected = 
//"//<Creator>https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html</Creator>[10]\n" +
//"//<CreateTime>2017-04-21T21:32:32</CreateTime>[10]\n" +
"//<Software>ERDDAP - Version 2.02</Software>[10]\n" +
"//<Source>http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.html</Source>[10]\n" +
"//<Version>ODV Spreadsheet V4.0</Version>[10]\n" +
"//<DataField>GeneralField</DataField>[10]\n" +
"//<DataType>GeneralType</DataType>[10]\n" +
"Type:METAVAR:TEXT:2[9]Cruise:METAVAR:TEXT:2[9]Station:METAVAR:TEXT:2[9]ship:METAVAR:TEXT:12[9]yyyy-mm-ddThh:mm:ss.SSS[9]Latitude [degrees_north]:METAVAR:DOUBLE[9]Longitude [degrees_east]:METAVAR:DOUBLE[9]status:METAVAR:TEXT:2[9]testLong [1]:PRIMARYVAR:DOUBLE[9]sst [degree_C]:FLOAT[10]\n" +
"*[9][9][9] a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T00:45:00Z[9]28.0002[9]-130.2576[9]A[9]-9223372036854775808[9]10.9[10]\n" +
"*[9][9][9] a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T01:45:00Z[9]28.0003[9]-130.3472[9]\\u20ac[9]-9007199254740992[9][10]\n" +
"*[9][9][9] a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T02:45:00Z[9]28.0001[9]-130.4305[9]\\t[9]9007199254740992[9]10.7[10]\n" +
"*[9][9][9] a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T12:45:00Z[9]27.9998[9]-131.5578[9]\\\"[9]9223372036854775806[9][10]\n" +
"*[9][9][9] a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T21:45:00Z[9]28.0003[9]-132.0014[9]\\u00fc[9][9]10.0[10]\n" +
"*[9][9][9] a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T23:45:00Z[9]28.0002[9]-132.1591[9]?[9][9][10]\n" +
"[end]";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        po = results.indexOf(expected.substring(0, 20));        
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);        

        //*** getting tsv
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".tsv"); 
        results = String2.annotatedString(String2.directReadFrom88591File(dir + tName));
        //String2.log(results);
        expected = 
"ship[9]time[9]latitude[9]longitude[9]status[9]testLong[9]sst[10]\n" +
"[9]UTC[9]degrees_north[9]degrees_east[9][9]1[9]degree_C[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T00:45:00Z[9]28.0002[9]-130.2576[9]A[9]-9223372036854775808[9]10.9[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T01:45:00Z[9]28.0003[9]-130.3472[9]\\u20ac[9]-9007199254740992[9]NaN[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T02:45:00Z[9]28.0001[9]-130.4305[9]\\t[9]9007199254740992[9]10.7[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T12:45:00Z[9]27.9998[9]-131.5578[9]\\\"[9]9223372036854775806[9]NaN[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T21:45:00Z[9]28.0003[9]-132.0014[9]\\u00fc[9]NaN[9]10.0[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T23:45:00Z[9]28.0002[9]-132.1591[9]?[9]NaN[9]NaN[10]\n" +
"[end]";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting tsvp
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".tsvp"); 
        results = String2.annotatedString(String2.directReadFrom88591File(dir + tName));
        //String2.log(results);
        expected = 
"ship[9]time (UTC)[9]latitude (degrees_north)[9]longitude (degrees_east)[9]status[9]testLong (1)[9]sst (degree_C)[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T00:45:00Z[9]28.0002[9]-130.2576[9]A[9]-9223372036854775808[9]10.9[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T01:45:00Z[9]28.0003[9]-130.3472[9]\\u20ac[9]-9007199254740992[9]NaN[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T02:45:00Z[9]28.0001[9]-130.4305[9]\\t[9]9007199254740992[9]10.7[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T12:45:00Z[9]27.9998[9]-131.5578[9]\\\"[9]9223372036854775806[9]NaN[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T21:45:00Z[9]28.0003[9]-132.0014[9]\\u00fc[9]NaN[9]10.0[10]\n" +
" a\\t~\\u00fc,\\n'z\\\"\\u20ac[9]2017-03-23T23:45:00Z[9]28.0002[9]-132.1591[9]?[9]NaN[9]NaN[10]\n" +
"[end]";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //*** getting xhtml
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_char", ".xhtml"); 
        results = String2.annotatedString(
            String2.directReadFromUtf8File(dir + tName));
        //String2.log(results);
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>[10]\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"[10]\n" +
"  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">[10]\n" +
"<html xmlns=\"https://www.w3.org/1999/xhtml\">[10]\n" +
"<head>[10]\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />[10]\n" +
"  <title>EDDTableFromNccsvFiles_char</title>[10]\n" +
"  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:8080/cwexperimental/images/erddap2.css\" />[10]\n" +
"</head>[10]\n" +
"<body>[10]\n" +
"[10]\n" +
"&nbsp;[10]\n" +
"<table class=\"erd commonBGColor nowrap\">[10]\n" +
"<tr>[10]\n" +
"<th>ship</th>[10]\n" +
"<th>time</th>[10]\n" +
"<th>latitude</th>[10]\n" +
"<th>longitude</th>[10]\n" +
"<th>status</th>[10]\n" +
"<th>testLong</th>[10]\n" +
"<th>sst</th>[10]\n" +
"</tr>[10]\n" +
"<tr>[10]\n" +
"<th></th>[10]\n" +
"<th>UTC</th>[10]\n" +
"<th>degrees_north</th>[10]\n" +
"<th>degrees_east</th>[10]\n" +
"<th></th>[10]\n" +
"<th>1</th>[10]\n" +
"<th>degree_C</th>[10]\n" +
"</tr>[10]\n" +
"<tr>[10]\n" +
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
"<td>[160]a&#9;~&#xfc;,[10]\n" + // [160] is nbsp
"&#39;z&quot;&#x20ac;</td>[10]\n" +
"<td>2017-03-23T00:45:00Z</td>[10]\n" +
"<td class=\"R\">28.0002</td>[10]\n" +
"<td class=\"R\">-130.2576</td>[10]\n" +
"<td>A</td>[10]\n" +
"<td class=\"R\">-9223372036854775808</td>[10]\n" +
"<td class=\"R\">10.9</td>[10]\n" +
"</tr>[10]\n" +
"<tr>[10]\n" +
"<td>[160]a&#9;~&#xfc;,[10]\n" +
"&#39;z&quot;&#x20ac;</td>[10]\n" +
"<td>2017-03-23T01:45:00Z</td>[10]\n" +
"<td class=\"R\">28.0003</td>[10]\n" +
"<td class=\"R\">-130.3472</td>[10]\n" +
"<td>&#x20ac;</td>[10]\n" +
"<td class=\"R\">-9007199254740992</td>[10]\n" +
"<td></td>[10]\n" +
"</tr>[10]\n" +
"<tr>[10]\n" +
"<td>[160]a&#9;~&#xfc;,[10]\n" +
"&#39;z&quot;&#x20ac;</td>[10]\n" +
"<td>2017-03-23T02:45:00Z</td>[10]\n" +
"<td class=\"R\">28.0001</td>[10]\n" +
"<td class=\"R\">-130.4305</td>[10]\n" +
"<td>&#9;</td>[10]\n" +
"<td class=\"R\">9007199254740992</td>[10]\n" +
"<td class=\"R\">10.7</td>[10]\n" +
"</tr>[10]\n" +
"<tr>[10]\n" +
"<td>[160]a&#9;~&#xfc;,[10]\n" +
"&#39;z&quot;&#x20ac;</td>[10]\n" +
"<td>2017-03-23T12:45:00Z</td>[10]\n" +
"<td class=\"R\">27.9998</td>[10]\n" +
"<td class=\"R\">-131.5578</td>[10]\n" +
"<td>&quot;</td>[10]\n" +
"<td class=\"R\">9223372036854775806</td>[10]\n" +
"<td></td>[10]\n" +
"</tr>[10]\n" +
"<tr>[10]\n" +
"<td>[160]a&#9;~&#xfc;,[10]\n" +
"&#39;z&quot;&#x20ac;</td>[10]\n" +
"<td>2017-03-23T21:45:00Z</td>[10]\n" +
"<td class=\"R\">28.0003</td>[10]\n" +
"<td class=\"R\">-132.0014</td>[10]\n" +
"<td>&#xfc;</td>[10]\n" +
"<td></td>[10]\n" +
"<td class=\"R\">10.0</td>[10]\n" +
"</tr>[10]\n" +
"<tr>[10]\n" +
"<td>[160]a&#9;~&#xfc;,[10]\n" +
"&#39;z&quot;&#x20ac;</td>[10]\n" +
"<td>2017-03-23T23:45:00Z</td>[10]\n" +
"<td class=\"R\">28.0002</td>[10]\n" +
"<td class=\"R\">-132.1591</td>[10]\n" +
"<td>?</td>[10]\n" +
"<td></td>[10]\n" +
"<td></td>[10]\n" +
"</tr>[10]\n" +
"</table>[10]\n" +
"</body>[10]\n" +
"</html>[10]\n" +
"[end]";
//ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
//source status chars are A\u20AC\t"\u00fc\uFFFF
        Test.ensureEqual(results, expected, "results=\n" + results);        
    }


    /**
     * This tests actual_range in .nccsvMetadata and .nccsv responses.
     * This requires pmelTaoDySst and rPmelTaoDySst in localhost erddap.
     */
    public static void testActualRange() throws Throwable {
        String2.log("\n****************** EDDTableFromNccsv.testActualRange\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
        String error = "";
        int epo, tPo;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10); 
        String  baseUrl = "http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst";
        String rbaseUrl = "http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst";


        //*** test getting .nccsvMetadata for entire dataset
        tQuery = ".nccsvMetadata";
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"\n" +
"*GLOBAL*,cdm_data_type,TimeSeries\n" +
"*GLOBAL*,cdm_timeseries_variables,\"array, station, wmo_platform_code, longitude, latitude\"\n" +
"*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov\n" +
"*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL\n" +
"*GLOBAL*,creator_type,group\n" +
"*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission\n" +
"*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL\n" +
"*GLOBAL*,defaultGraphQuery,\"longitude,latitude,T_25&time>=now-7days\"\n" +
"*GLOBAL*,Easternmost_Easting,350.0d\n" +
"*GLOBAL*,featureType,TimeSeries\n" +
"*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov\n" +
"*GLOBAL*,geospatial_lat_max,21.0d\n" +
"*GLOBAL*,geospatial_lat_min,-25.0d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,350.0d\n" +
"*GLOBAL*,geospatial_lon_min,0.0d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,geospatial_vertical_max,15.0d\n" +
"*GLOBAL*,geospatial_vertical_min,1.0d\n" +
"*GLOBAL*,geospatial_vertical_positive,down\n" +
"*GLOBAL*,geospatial_vertical_units,m\n" +   //date in history changes
"*GLOBAL*,history,\"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\\n" +
"2019-08-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.\"\n" +
"*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission\n" +
"*GLOBAL*,institution,\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\"\n" +
"*GLOBAL*,keywords,\"buoys, centered, daily, depth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton\"\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"\n" +
"*GLOBAL*,Northernmost_Northing,21.0d\n" +
"*GLOBAL*,project,\"TAO/TRITON, RAMA, PIRATA\"\n" +
"*GLOBAL*,Request_for_acknowledgement,\"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\"\n" +
"*GLOBAL*,sourceUrl,(local files)\n" +
"*GLOBAL*,Southernmost_Northing,-25.0d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n" +
"*GLOBAL*,subsetVariables,\"array, station, wmo_platform_code, longitude, latitude\"\n" +
"*GLOBAL*,summary,\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\"\n" +
"*GLOBAL*,testOutOfDate,now-3days\n" +
"*GLOBAL*,time_coverage_end,2019-08-01T12:00:00Z\n" + //changes
"*GLOBAL*,time_coverage_start,1977-11-03T12:00:00Z\n" +
"*GLOBAL*,title,\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\n" +
"*GLOBAL*,Westernmost_Easting,0.0d\n" +
"array,*DATA_TYPE*,String\n" +
"array,ioos_category,Identifier\n" +
"array,long_name,Array\n" +
"station,*DATA_TYPE*,String\n" +
"station,cf_role,timeseries_id\n" +
"station,ioos_category,Identifier\n" +
"station,long_name,Station\n" +
"wmo_platform_code,*DATA_TYPE*,int\n" +
"wmo_platform_code,actual_range,13001i,56055i\n" +
"wmo_platform_code,ioos_category,Identifier\n" +
"wmo_platform_code,long_name,WMO Platform Code\n" +
"wmo_platform_code,missing_value,2147483647i\n" +
"longitude,*DATA_TYPE*,float\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,actual_range,0.0f,350.0f\n" +
"longitude,axis,X\n" +
"longitude,epic_code,502i\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Nominal Longitude\n" +
"longitude,missing_value,1.0E35f\n" +
"longitude,standard_name,longitude\n" +
"longitude,type,EVEN\n" +
"longitude,units,degrees_east\n" +
"latitude,*DATA_TYPE*,float\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,actual_range,-25.0f,21.0f\n" +
"latitude,axis,Y\n" +
"latitude,epic_code,500i\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Nominal Latitude\n" +
"latitude,missing_value,1.0E35f\n" +
"latitude,standard_name,latitude\n" +
"latitude,type,EVEN\n" +
"latitude,units,degrees_north\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,actual_range,1977-11-03T12:00:00Z\\n2019-08-01T12:00:00Z\n" +  //stop time changes
"time,axis,T\n" +
"time,ioos_category,Time\n" +
"time,long_name,Centered Time\n" +
"time,point_spacing,even\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,type,EVEN\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"depth,*DATA_TYPE*,float\n" +
"depth,_CoordinateAxisType,Height\n" +
"depth,_CoordinateZisPositive,down\n" +
"depth,actual_range,1.0f,15.0f\n" +
"depth,axis,Z\n" +
"depth,epic_code,3i\n" +
"depth,ioos_category,Location\n" +
"depth,long_name,Depth\n" +
"depth,missing_value,1.0E35f\n" +
"depth,positive,down\n" +
"depth,standard_name,depth\n" +
"depth,type,EVEN\n" +
"depth,units,m\n" +
"T_25,*DATA_TYPE*,float\n" +
"T_25,_FillValue,1.0E35f\n" +
"T_25,actual_range,17.12f,35.4621f\n" +
"T_25,colorBarMaximum,32.0d\n" +
"T_25,colorBarMinimum,0.0d\n" +
"T_25,epic_code,25i\n" +
"T_25,generic_name,temp\n" +
"T_25,ioos_category,Temperature\n" +
"T_25,long_name,Sea Surface Temperature\n" +
"T_25,missing_value,1.0E35f\n" +
"T_25,name,T\n" +
"T_25,standard_name,sea_surface_temperature\n" +
"T_25,units,degree_C\n" +
"QT_5025,*DATA_TYPE*,float\n" +
"QT_5025,_FillValue,1.0E35f\n" +
"QT_5025,actual_range,0.0f,5.0f\n" +
"QT_5025,colorBarContinuous,false\n" +
"QT_5025,colorBarMaximum,6.0d\n" +
"QT_5025,colorBarMinimum,0.0d\n" +
"QT_5025,description,\"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025>=1 and QT_5025<=3.\"\n" +
"QT_5025,epic_code,5025i\n" +
"QT_5025,generic_name,qt\n" +
"QT_5025,ioos_category,Quality\n" +
"QT_5025,long_name,Sea Surface Temperature Quality\n" +
"QT_5025,missing_value,1.0E35f\n" +
"QT_5025,name,QT\n" +
"ST_6025,*DATA_TYPE*,float\n" +
"ST_6025,_FillValue,1.0E35f\n" +
"ST_6025,actual_range,0.0f,5.0f\n" +
"ST_6025,colorBarContinuous,false\n" +
"ST_6025,colorBarMaximum,8.0d\n" +
"ST_6025,colorBarMinimum,0.0d\n" +
"ST_6025,description,\"Source Codes:\\n0 = No Sensor, No Data\\n1 = Real Time (Telemetered Mode)\\n2 = Derived from Real Time\\n3 = Temporally Interpolated from Real Time\\n4 = Source Code Inactive at Present\\n5 = Recovered from Instrument RAM (Delayed Mode)\\n6 = Derived from RAM\\n7 = Temporally Interpolated from RAM\"\n" +
"ST_6025,epic_code,6025i\n" +
"ST_6025,generic_name,st\n" +
"ST_6025,ioos_category,Other\n" +
"ST_6025,long_name,Sea Surface Temperature Source\n" +
"ST_6025,missing_value,1.0E35f\n" +
"ST_6025,name,ST\n" +
"\n" +
"*END_METADATA*\n";

        //note that there is actual_range info
        results = SSR.getUrlResponseStringUnchanged(baseUrl + tQuery);
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        results = SSR.getUrlResponseStringUnchanged(rbaseUrl + tQuery);
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

      
        //*** test getting .nccsv 
        tQuery = ".nccsv?&station=%220n125w%22&time%3E=2010-01-01&time%3C=2010-01-05";
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"\n" +
"*GLOBAL*,cdm_data_type,TimeSeries\n" +
"*GLOBAL*,cdm_timeseries_variables,\"array, station, wmo_platform_code, longitude, latitude\"\n" +
"*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov\n" +
"*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL\n" +
"*GLOBAL*,creator_type,group\n" +
"*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission\n" +
"*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL\n" +
"*GLOBAL*,defaultGraphQuery,\"longitude,latitude,T_25&time>=now-7days\"\n" +
"*GLOBAL*,Easternmost_Easting,350.0d\n" +
"*GLOBAL*,featureType,TimeSeries\n" +
"*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov\n" +
"*GLOBAL*,geospatial_lat_max,21.0d\n" +
"*GLOBAL*,geospatial_lat_min,-25.0d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,350.0d\n" +
"*GLOBAL*,geospatial_lon_min,0.0d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,geospatial_vertical_max,15.0d\n" +
"*GLOBAL*,geospatial_vertical_min,1.0d\n" +
"*GLOBAL*,geospatial_vertical_positive,down\n" +
"*GLOBAL*,geospatial_vertical_units,m\n" +  //date below changes
"*GLOBAL*,history,\"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\\n" + 
  "2019-08-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.\\n";
//  "2017-05-26T18:30:46Z (local files)\\n" + 
//  "2017-05-26T18:30:46Z 
expected2 = 
"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.nccsv?&station=%220n125w%22&time%3E=2010-01-01&time%3C=2010-01-05\"\n" +
"*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission\n" +
"*GLOBAL*,institution,\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\"\n" +
"*GLOBAL*,keywords,\"buoys, centered, daily, depth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton\"\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"\n" +
"*GLOBAL*,Northernmost_Northing,21.0d\n" +
"*GLOBAL*,project,\"TAO/TRITON, RAMA, PIRATA\"\n" +
"*GLOBAL*,Request_for_acknowledgement,\"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\"\n" +
"*GLOBAL*,sourceUrl,(local files)\n" +
"*GLOBAL*,Southernmost_Northing,-25.0d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n" +
"*GLOBAL*,subsetVariables,\"array, station, wmo_platform_code, longitude, latitude\"\n" +
"*GLOBAL*,summary,\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\"\n" +
"*GLOBAL*,testOutOfDate,now-3days\n" +
"*GLOBAL*,time_coverage_end,2019-08-01T12:00:00Z\n" + //changes
"*GLOBAL*,time_coverage_start,1977-11-03T12:00:00Z\n" +
"*GLOBAL*,title,\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\n" +
"*GLOBAL*,Westernmost_Easting,0.0d\n" +
"array,*DATA_TYPE*,String\n" +
"array,ioos_category,Identifier\n" +
"array,long_name,Array\n" +
"station,*DATA_TYPE*,String\n" +
"station,cf_role,timeseries_id\n" +
"station,ioos_category,Identifier\n" +
"station,long_name,Station\n" +
"wmo_platform_code,*DATA_TYPE*,int\n" +
"wmo_platform_code,ioos_category,Identifier\n" +
"wmo_platform_code,long_name,WMO Platform Code\n" +
"wmo_platform_code,missing_value,2147483647i\n" +
"longitude,*DATA_TYPE*,float\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,axis,X\n" +
"longitude,epic_code,502i\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Nominal Longitude\n" +
"longitude,missing_value,1.0E35f\n" +
"longitude,standard_name,longitude\n" +
"longitude,type,EVEN\n" +
"longitude,units,degrees_east\n" +
"latitude,*DATA_TYPE*,float\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,axis,Y\n" +
"latitude,epic_code,500i\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Nominal Latitude\n" +
"latitude,missing_value,1.0E35f\n" +
"latitude,standard_name,latitude\n" +
"latitude,type,EVEN\n" +
"latitude,units,degrees_north\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,axis,T\n" +
"time,ioos_category,Time\n" +
"time,long_name,Centered Time\n" +
"time,point_spacing,even\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,type,EVEN\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"depth,*DATA_TYPE*,float\n" +
"depth,_CoordinateAxisType,Height\n" +
"depth,_CoordinateZisPositive,down\n" +
"depth,axis,Z\n" +
"depth,epic_code,3i\n" +
"depth,ioos_category,Location\n" +
"depth,long_name,Depth\n" +
"depth,missing_value,1.0E35f\n" +
"depth,positive,down\n" +
"depth,standard_name,depth\n" +
"depth,type,EVEN\n" +
"depth,units,m\n" +
"T_25,*DATA_TYPE*,float\n" +
"T_25,_FillValue,1.0E35f\n" +
"T_25,colorBarMaximum,32.0d\n" +
"T_25,colorBarMinimum,0.0d\n" +
"T_25,epic_code,25i\n" +
"T_25,generic_name,temp\n" +
"T_25,ioos_category,Temperature\n" +
"T_25,long_name,Sea Surface Temperature\n" +
"T_25,missing_value,1.0E35f\n" +
"T_25,name,T\n" +
"T_25,standard_name,sea_surface_temperature\n" +
"T_25,units,degree_C\n" +
"QT_5025,*DATA_TYPE*,float\n" +
"QT_5025,_FillValue,1.0E35f\n" +
"QT_5025,colorBarContinuous,false\n" +
"QT_5025,colorBarMaximum,6.0d\n" +
"QT_5025,colorBarMinimum,0.0d\n" +
"QT_5025,description,\"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025>=1 and QT_5025<=3.\"\n" +
"QT_5025,epic_code,5025i\n" +
"QT_5025,generic_name,qt\n" +
"QT_5025,ioos_category,Quality\n" +
"QT_5025,long_name,Sea Surface Temperature Quality\n" +
"QT_5025,missing_value,1.0E35f\n" +
"QT_5025,name,QT\n" +
"ST_6025,*DATA_TYPE*,float\n" +
"ST_6025,_FillValue,1.0E35f\n" +
"ST_6025,colorBarContinuous,false\n" +
"ST_6025,colorBarMaximum,8.0d\n" +
"ST_6025,colorBarMinimum,0.0d\n" +
"ST_6025,description,\"Source Codes:\\n0 = No Sensor, No Data\\n1 = Real Time (Telemetered Mode)\\n2 = Derived from Real Time\\n3 = Temporally Interpolated from Real Time\\n4 = Source Code Inactive at Present\\n5 = Recovered from Instrument RAM (Delayed Mode)\\n6 = Derived from RAM\\n7 = Temporally Interpolated from RAM\"\n" +
"ST_6025,epic_code,6025i\n" +
"ST_6025,generic_name,st\n" +
"ST_6025,ioos_category,Other\n" +
"ST_6025,long_name,Sea Surface Temperature Source\n" +
"ST_6025,missing_value,1.0E35f\n" +
"ST_6025,name,ST\n" +
"\n" +
"*END_METADATA*\n" +
"array,station,wmo_platform_code,longitude,latitude,time,depth,T_25,QT_5025,ST_6025\n" +
"TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-01T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
"TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-02T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
"TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-03T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
"TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-04T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
"*END_DATA*\n";

        //note no actual_range info
        results = SSR.getUrlResponseStringUnchanged(baseUrl + tQuery);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        tPo = results.indexOf(expected2.substring(0, 100));
        Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

        results = SSR.getUrlResponseStringUnchanged(rbaseUrl + tQuery);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        tPo = results.indexOf(expected2.substring(0, 100));
        Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    }


    /** Test reading data from testNccsvScalar by EDDTableFromDapSequence. */
    public static void testDap() throws Exception {
        String2.log("\n*** EDDTableFromDapSequence.testDap\n");
/*        try {
            EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "testTestNccsvScalar"); 

            String tName = edd.makeNewFileForDapQuery(null, null, 
                "longitude,latitude,time&time=%221992-01-01T00:00:00Z%22" +
                "&longitude>=-132.0&longitude<=-112.0&latitude>=30.0&latitude<=50.0" +
                "&distinct()&.draw=markers&.colorBar=|D||||", 
                EDStatic.fullTestCacheDirectory, edd.className() + "_SVGraph", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        } catch (Throwable t) {
            String2.pressEnterToContinue("\n" + MustBe.throwableToString(t) + 
                "\n2014 THIS DATASET HAS BEEN UNAVAILABLE FOR MONTHS."); 
                //"\nUnexpected error for testSubsetVariablesGraph.");
        } */
    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n*** EDDTableFromNccsvFiles.test()");

/* for releases, this line should have open/close comment */
        //long MIN_VALUE=-9223372036854775808  MAX_VALUE=9223372036854775807
        //9007199254740992 (~9e15) see https://www.mathworks.com/help/matlab/ref/flintmax.html
        for (long tl = 9007199254000000L; tl < Long.MAX_VALUE; tl++) {
            double d = tl;
            if (Math.round(d) != tl) {
                String2.log("tl=" + tl + " d=" + d + " is the first large long that can't round trip to/from double");
                break;
            }
        }
        Test.ensureEqual(9007199254740992L, Math.round((double)9007199254740992L), "");

        //-9007199254740992  see https://www.mathworks.com/help/matlab/ref/flintmax.html
        for (long tl = -9007199254000000L; tl > Long.MIN_VALUE; tl--) {
            double d = tl;
            if (Math.round(d) != tl) {
                String2.log("tl=" + tl + " d=" + d + "  is the first small long that can't round trip to/from double");
                break;
            }
        }
        Test.ensureEqual(-9007199254740992L, Math.round((double)-9007199254740992L), "");
        //String2.pressEnterToContinue();

        testGenerateDatasetsXml();
        testBasic(true); //deleteCachedDatasetInfo
        testBasic(false); //deleteCachedDatasetInfo
        testChar();
        testDap();
        testActualRange();
        /* */
    }
}

