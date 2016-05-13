/* 
 * EDDTableFromMultidimNcFiles Copyright 2016, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
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
 * This class represents a table of data from a collection of multidimensional .nc data files.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2016-05-05
 */
public class EDDTableFromMultidimNcFiles extends EDDTableFromFiles { 


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
    public EDDTableFromMultidimNcFiles(String tDatasetID, 
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
        String tSortedColumnSourceName, 
        String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles, boolean tRemoveMVRows) 
        throws Throwable {

        super("EDDTableFromMultidimNcFiles", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows);

    }

    /**
     * This gets source data from one file.
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

        //read the file
        Table table = new Table();
        table.readMultidimNc(fileDir + fileName, sourceDataNames, null,
            getMetadata, true, removeMVRows, //trimStrings, 
            sourceConVars, sourceConOps, sourceConValues);
        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromMultidimNcFiles.
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
     * @param tRemoveMVRows        
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
        String useDimensionsCSV, int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, 
        boolean tRemoveMVRows,  //siblings have String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromMultidimNcFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);

        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        StringArray useDimensions = StringArray.fromCSV(useDimensionsCSV);
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis
        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        //show structure of sample file
        String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
        String2.log(NcHelper.dumpString(sampleFileName, false));

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();

        //read the sample file
        dataSourceTable.readMultidimNc(sampleFileName, null, useDimensions,  
            true, true, tRemoveMVRows, //getMetadata, trimStrings, removeMVRows
            null, null, null); //conVars, conOps, conVals
        StringArray varNames = new StringArray(dataSourceTable.getColumnNames());
        Test.ensureTrue(varNames.size() > 0, 
            "The file has no variables with dimensions: " + useDimensionsCSV);
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            dataAddTable.addColumn(c, colName,
                dataSourceTable.getColumn(c),
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    dataSourceTable.globalAttributes(), sourceAtts, colName, 
                    true, true)); //addColorBarMinMax, tryToFindLLAT
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
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataSourceTable, dataAddTable)? "Point" : "Other",
                tFileDir, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            //no units or standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);
        }

        //write the information
        StringBuilder sb = new StringBuilder();
        String suggestedRegex = (tFileNameRegex == null || tFileNameRegex.length() == 0)? 
            ".*\\" + File2.getExtension(sampleFileName) :
            tFileNameRegex;
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = tColumnNameForExtract.trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromMultidimNcFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + suggestedRegex) +  //dirs can't be made public
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + 
            "</updateEveryNMillis>\n" +  
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(suggestedRegex) + "</fileNameRegex>\n" +
            "    <recursive>true</recursive>\n" +
            "    <pathRegex>.*</pathRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            "    <columnNameForExtract>" + XML.encodeAsXML(tColumnNameForExtract) + "</columnNameForExtract>\n" +
            //"    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) + "</sortedColumnSourceName>\n" +
            "    <removeMVRows>" + ("" + tRemoveMVRows).toLowerCase() + "</removeMVRows>\n" +
            "    <sortFilesBySourceNames>" + XML.encodeAsXML(tSortFilesBySourceNames) + "</sortFilesBySourceNames>\n" +
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
                EDStatic.unitTestDataDir + "nc", ".*_prof\\.nc", "",
                "N_PROF, N_LEVELS",
                1440,
                "^", "_prof.nc$", ".*", "fileNumber", //just for test purposes
                true, //removeMVRows
                "FLOAT_SERIAL_NO JULD", //sort files by 
                "", "", "", "", null) + "\n";

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromMultidimNcFiles\" datasetID=\"nc_65cd_4c8a_93f3\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTest/nc/</fileDir>\n" +
"    <fileNameRegex>.*_prof\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex>^</preExtractRegex>\n" +
"    <postExtractRegex>_prof.nc$</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>fileNumber</columnNameForExtract>\n" +
"    <removeMVRows>true</removeMVRows>\n" +
"    <sortFilesBySourceNames>FLOAT_SERIAL_NO JULD</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Conventions\">Argo-3.1 CF-1.6</att>\n" +
"        <att name=\"featureType\">trajectoryProfile</att>\n" +
"        <att name=\"history\">2016-04-15T20:47:22Z creation</att>\n" +
"        <att name=\"institution\">Coriolis GDAC</att>\n" +
"        <att name=\"references\">http://www.argodatamgt.org/Documentation</att>\n" +
"        <att name=\"source\">Argo float</att>\n" +
"        <att name=\"title\">Argo float vertical profile</att>\n" +
"        <att name=\"user_manual_version\">3.1</att>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">Argo-3.1 CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">Coriolis GDAC</att>\n" +
"        <att name=\"creator_url\">http://www.argodatamgt.org/Documentation</att>\n" +
"        <att name=\"infoUrl\">http://www.argodatamgt.org/Documentation</att>\n" +
"        <att name=\"keywords\">adjusted, argo, array, assembly, best, centre, centres, charge, coded, CONFIG_MISSION_NUMBER, contains, coriolis, creation, currents, cycle, CYCLE_NUMBER, data, DATA_CENTRE, DATA_MODE, DATA_STATE_INDICATOR, DATA_TYPE, date, DATE_CREATION, DATE_UPDATE, day, days, DC_REFERENCE, degree, delayed, denoting, density, determined, direction, equals, error, estimate, file, firmware, FIRMWARE_VERSION, flag, float, FLOAT_SERIAL_NO, format, FORMAT_VERSION, gdac, geostrophic, global, handbook, HANDBOOK_VERSION, have, identifier, in-situ, instrument, investigator, its, its-90, JULD, JULD_LOCATION, JULD_QC, julian, latitude, level, longitude, missions, mode, name, number, ocean, oceanography, oceans,\n" +
"Oceans &gt; Ocean Pressure &gt; Water Pressure,\n" +
"Oceans &gt; Ocean Temperature &gt; Water Temperature,\n" +
"Oceans &gt; Salinity/Density &gt; Salinity,\n" +
"passed, performed, PI_NAME, PLATFORM_NUMBER, PLATFORM_TYPE, position, POSITION_QC, positioning, POSITIONING_SYSTEM, practical, pres, PRES_ADJUSTED, PRES_ADJUSTED_ERROR, PRES_ADJUSTED_QC, PRES_QC, pressure, principal, process, processing, profile, PROFILE_PRES_QC, PROFILE_PSAL_QC, PROFILE_TEMP_QC, profiles, project, PROJECT_NAME, psal, PSAL_ADJUSTED, PSAL_ADJUSTED_ERROR, PSAL_ADJUSTED_QC, PSAL_QC, quality, rdac, real, real time, real-time, realtime, reference, REFERENCE_DATE_TIME, regional, relative, salinity, sampling, scale, scheme, sea, sea level, sea-level, sea_water_practical_salinity, sea_water_pressure, sea_water_temperature, seawater, serial, situ, station, statistics, system, TEMP, TEMP_ADJUSTED, TEMP_ADJUSTED_ERROR, TEMP_ADJUSTED_QC, TEMP_QC, temperature, through, time, type, unique, update, values, version, vertical, VERTICAL_SAMPLING_SCHEME, water, WMO_INST_TYPE</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">Argo float vertical profile. Coriolis Regional Data Assembly Centres (GDAC) data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>fileNumber</sourceName>\n" +
"        <destinationName>fileNumber</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">File Number</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DATA_TYPE</sourceName>\n" +
"        <destinationName>DATA_TYPE</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 1</att>\n" +
"            <att name=\"long_name\">Data type</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>FORMAT_VERSION</sourceName>\n" +
"        <destinationName>FORMAT_VERSION</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">File format version</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>HANDBOOK_VERSION</sourceName>\n" +
"        <destinationName>HANDBOOK_VERSION</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Data handbook version</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>REFERENCE_DATE_TIME</sourceName>\n" +
"        <destinationName>REFERENCE_DATE_TIME</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">YYYYMMDDHHMISS</att>\n" +
"            <att name=\"long_name\">Date of reference for Julian days</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DATE_CREATION</sourceName>\n" +
"        <destinationName>DATE_CREATION</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">YYYYMMDDHHMISS</att>\n" +
"            <att name=\"long_name\">Date of file creation</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DATE_UPDATE</sourceName>\n" +
"        <destinationName>DATE_UPDATE</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">YYYYMMDDHHMISS</att>\n" +
"            <att name=\"long_name\">Date of update of this file</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PLATFORM_NUMBER</sourceName>\n" +
"        <destinationName>PLATFORM_NUMBER</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">WMO float identifier : A9IIIII</att>\n" +
"            <att name=\"long_name\">Float unique identifier</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PROJECT_NAME</sourceName>\n" +
"        <destinationName>PROJECT_NAME</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Name of the project</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PI_NAME</sourceName>\n" +
"        <destinationName>PI_NAME</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Name of the principal investigator</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>CYCLE_NUMBER</sourceName>\n" +
"        <destinationName>CYCLE_NUMBER</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">99999</att>\n" +
"            <att name=\"conventions\">0...N, 0 : launch cycle (if exists), 1 : first complete cycle</att>\n" +
"            <att name=\"long_name\">Float cycle number</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DIRECTION</sourceName>\n" +
"        <destinationName>DIRECTION</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">A: ascending profiles, D: descending profiles</att>\n" +
"            <att name=\"long_name\">Direction of the station profiles</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DATA_CENTRE</sourceName>\n" +
"        <destinationName>DATA_CENTRE</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 4</att>\n" +
"            <att name=\"long_name\">Data centre in charge of float data processing</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DC_REFERENCE</sourceName>\n" +
"        <destinationName>DC_REFERENCE</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Data centre convention</att>\n" +
"            <att name=\"long_name\">Station unique identifier in data centre</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DATA_STATE_INDICATOR</sourceName>\n" +
"        <destinationName>DATA_STATE_INDICATOR</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 6</att>\n" +
"            <att name=\"long_name\">Degree of processing the data have passed through</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DATA_MODE</sourceName>\n" +
"        <destinationName>DATA_MODE</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">R : real time; D : delayed mode; A : real time with adjustment</att>\n" +
"            <att name=\"long_name\">Delayed mode or real time data</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PLATFORM_TYPE</sourceName>\n" +
"        <destinationName>PLATFORM_TYPE</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 23</att>\n" +
"            <att name=\"long_name\">Type of float</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>FLOAT_SERIAL_NO</sourceName>\n" +
"        <destinationName>FLOAT_SERIAL_NO</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Serial number of the float</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>FIRMWARE_VERSION</sourceName>\n" +
"        <destinationName>FIRMWARE_VERSION</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Instrument firmware version</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WMO_INST_TYPE</sourceName>\n" +
"        <destinationName>WMO_INST_TYPE</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 8</att>\n" +
"            <att name=\"long_name\">Coded instrument type</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>JULD</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">999999.0</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"conventions\">Relative julian days with decimal part (as parts of day)</att>\n" +
"            <att name=\"long_name\">Julian day (UTC) of the station relative to REFERENCE_DATE_TIME</att>\n" +
"            <att name=\"resolution\" type=\"double\">0.0</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">days since 1950-01-01 00:00:00 UTC</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>JULD_QC</sourceName>\n" +
"        <destinationName>JULD_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2</att>\n" +
"            <att name=\"long_name\">Quality on date and time</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>JULD_LOCATION</sourceName>\n" +
"        <destinationName>JULD_LOCATION</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">999999.0</att>\n" +
"            <att name=\"conventions\">Relative julian days with decimal part (as parts of day)</att>\n" +
"            <att name=\"long_name\">Julian day (UTC) of the location relative to REFERENCE_DATE_TIME</att>\n" +
"            <att name=\"resolution\" type=\"double\">0.0</att>\n" +
"            <att name=\"units\">days since 1950-01-01 00:00:00 UTC</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>LATITUDE</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">99999.0</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"long_name\">Latitude of the station, best estimate</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degree_north</att>\n" +
"            <att name=\"valid_max\" type=\"double\">90.0</att>\n" +
"            <att name=\"valid_min\" type=\"double\">-90.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>LONGITUDE</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">99999.0</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"long_name\">Longitude of the station, best estimate</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degree_east</att>\n" +
"            <att name=\"valid_max\" type=\"double\">180.0</att>\n" +
"            <att name=\"valid_min\" type=\"double\">-180.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>POSITION_QC</sourceName>\n" +
"        <destinationName>POSITION_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2</att>\n" +
"            <att name=\"long_name\">Quality on position (latitude and longitude)</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>POSITIONING_SYSTEM</sourceName>\n" +
"        <destinationName>POSITIONING_SYSTEM</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Positioning system</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PROFILE_PRES_QC</sourceName>\n" +
"        <destinationName>PROFILE_PRES_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2a</att>\n" +
"            <att name=\"long_name\">Global quality flag of PRES profile</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PROFILE_TEMP_QC</sourceName>\n" +
"        <destinationName>PROFILE_TEMP_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2a</att>\n" +
"            <att name=\"long_name\">Global quality flag of TEMP profile</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PROFILE_PSAL_QC</sourceName>\n" +
"        <destinationName>PROFILE_PSAL_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2a</att>\n" +
"            <att name=\"long_name\">Global quality flag of PSAL profile</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>VERTICAL_SAMPLING_SCHEME</sourceName>\n" +
"        <destinationName>VERTICAL_SAMPLING_SCHEME</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 16</att>\n" +
"            <att name=\"long_name\">Vertical sampling scheme</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>CONFIG_MISSION_NUMBER</sourceName>\n" +
"        <destinationName>CONFIG_MISSION_NUMBER</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">99999</att>\n" +
"            <att name=\"conventions\">1...N, 1 : first complete mission</att>\n" +
"            <att name=\"long_name\">Unique number denoting the missions performed by the float</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PRES</sourceName>\n" +
"        <destinationName>PRES</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"C_format\">&#37;7.1f</att>\n" +
"            <att name=\"FORTRAN_format\">F7.1</att>\n" +
"            <att name=\"long_name\">Sea water pressure, equals 0 at sea-level</att>\n" +
"            <att name=\"resolution\" type=\"float\">1.0</att>\n" +
"            <att name=\"standard_name\">sea_water_pressure</att>\n" +
"            <att name=\"units\">decibar</att>\n" +
"            <att name=\"valid_max\" type=\"float\">12000.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">0.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Sea Level</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PRES_QC</sourceName>\n" +
"        <destinationName>PRES_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2</att>\n" +
"            <att name=\"long_name\">quality flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PRES_ADJUSTED</sourceName>\n" +
"        <destinationName>PRES_ADJUSTED</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"C_format\">&#37;7.1f</att>\n" +
"            <att name=\"FORTRAN_format\">F7.1</att>\n" +
"            <att name=\"long_name\">Sea water pressure, equals 0 at sea-level</att>\n" +
"            <att name=\"resolution\" type=\"float\">1.0</att>\n" +
"            <att name=\"standard_name\">sea_water_pressure</att>\n" +
"            <att name=\"units\">decibar</att>\n" +
"            <att name=\"valid_max\" type=\"float\">12000.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">0.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Sea Level</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PRES_ADJUSTED_QC</sourceName>\n" +
"        <destinationName>PRES_ADJUSTED_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2</att>\n" +
"            <att name=\"long_name\">quality flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PRES_ADJUSTED_ERROR</sourceName>\n" +
"        <destinationName>PRES_ADJUSTED_ERROR</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"C_format\">&#37;7.1f</att>\n" +
"            <att name=\"FORTRAN_format\">F7.1</att>\n" +
"            <att name=\"long_name\">Contains the error on the adjusted values as determined by the delayed mode QC process</att>\n" +
"            <att name=\"resolution\" type=\"float\">1.0</att>\n" +
"            <att name=\"units\">decibar</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">50.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TEMP</sourceName>\n" +
"        <destinationName>TEMP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"C_format\">&#37;9.3f</att>\n" +
"            <att name=\"FORTRAN_format\">F9.3</att>\n" +
"            <att name=\"long_name\">Sea temperature in-situ ITS-90 scale</att>\n" +
"            <att name=\"resolution\" type=\"float\">0.001</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature</att>\n" +
"            <att name=\"units\">degree_Celsius</att>\n" +
"            <att name=\"valid_max\" type=\"float\">40.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-2.5</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TEMP_QC</sourceName>\n" +
"        <destinationName>TEMP_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2</att>\n" +
"            <att name=\"long_name\">quality flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TEMP_ADJUSTED</sourceName>\n" +
"        <destinationName>TEMP_ADJUSTED</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"C_format\">&#37;9.3f</att>\n" +
"            <att name=\"FORTRAN_format\">F9.3</att>\n" +
"            <att name=\"long_name\">Sea temperature in-situ ITS-90 scale</att>\n" +
"            <att name=\"resolution\" type=\"float\">0.001</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature</att>\n" +
"            <att name=\"units\">degree_Celsius</att>\n" +
"            <att name=\"valid_max\" type=\"float\">40.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-2.5</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TEMP_ADJUSTED_QC</sourceName>\n" +
"        <destinationName>TEMP_ADJUSTED_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2</att>\n" +
"            <att name=\"long_name\">quality flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TEMP_ADJUSTED_ERROR</sourceName>\n" +
"        <destinationName>TEMP_ADJUSTED_ERROR</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"C_format\">&#37;9.3f</att>\n" +
"            <att name=\"FORTRAN_format\">F9.3</att>\n" +
"            <att name=\"long_name\">Contains the error on the adjusted values as determined by the delayed mode QC process</att>\n" +
"            <att name=\"resolution\" type=\"float\">0.001</att>\n" +
"            <att name=\"units\">degree_Celsius</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PSAL</sourceName>\n" +
"        <destinationName>PSAL</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"C_format\">&#37;9.3f</att>\n" +
"            <att name=\"FORTRAN_format\">F9.3</att>\n" +
"            <att name=\"long_name\">Practical salinity</att>\n" +
"            <att name=\"resolution\" type=\"float\">0.001</att>\n" +
"            <att name=\"standard_name\">sea_water_salinity</att>\n" +
"            <att name=\"units\">psu</att>\n" +
"            <att name=\"valid_max\" type=\"float\">41.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">2.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" +
"            <att name=\"units\">PSU</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PSAL_QC</sourceName>\n" +
"        <destinationName>PSAL_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2</att>\n" +
"            <att name=\"long_name\">quality flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PSAL_ADJUSTED</sourceName>\n" +
"        <destinationName>PSAL_ADJUSTED</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"C_format\">&#37;9.3f</att>\n" +
"            <att name=\"FORTRAN_format\">F9.3</att>\n" +
"            <att name=\"long_name\">Practical salinity</att>\n" +
"            <att name=\"resolution\" type=\"float\">0.001</att>\n" +
"            <att name=\"standard_name\">sea_water_salinity</att>\n" +
"            <att name=\"units\">psu</att>\n" +
"            <att name=\"valid_max\" type=\"float\">41.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">2.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" +
"            <att name=\"units\">PSU</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PSAL_ADJUSTED_QC</sourceName>\n" +
"        <destinationName>PSAL_ADJUSTED_QC</destinationName>\n" +
"        <dataType>char</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"conventions\">Argo reference table 2</att>\n" +
"            <att name=\"long_name\">quality flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PSAL_ADJUSTED_ERROR</sourceName>\n" +
"        <destinationName>PSAL_ADJUSTED_ERROR</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n" +
"            <att name=\"C_format\">&#37;9.3f</att>\n" +
"            <att name=\"FORTRAN_format\">F9.3</att>\n" +
"            <att name=\"long_name\">Contains the error on the adjusted values as determined by the delayed mode QC process</att>\n" +
"            <att name=\"resolution\" type=\"float\">0.001</att>\n" +
"            <att name=\"units\">psu</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"resolution\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //GenerateDatasetsXml
            results = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromMultidimNcFiles", 
                EDStatic.unitTestDataDir + "nc", ".*_prof\\.nc", "",
                "N_PROF, N_LEVELS",
                "1440",
                "^", "_prof.nc$", ".*", "fileNumber", //just for test purposes
                "true", //removeMVRows
                "FLOAT_SERIAL_NO JULD", //sort files by 
                "", "", "", ""},
                false); //doIt loop?
            Test.ensureEqual(results, expected, "Unexpected results from GenerateDatasetsXml.doIt.");

            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            //with one small change to addAttributes:
            results = String2.replaceAll(results, 
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n",
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n" +
                "        <att name=\"cdm_data_type\">Other</att>\n");
            String2.log(results);

            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), "nc_65cd_4c8a_93f3", "");
            Test.ensureEqual(edd.title(), "Argo float vertical profile", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
"fileNumber, DATA_TYPE, FORMAT_VERSION, HANDBOOK_VERSION, REFERENCE_DATE_TIME, DATE_CREATION, DATE_UPDATE, PLATFORM_NUMBER, PROJECT_NAME, PI_NAME, CYCLE_NUMBER, DIRECTION, DATA_CENTRE, DC_REFERENCE, DATA_STATE_INDICATOR, DATA_MODE, PLATFORM_TYPE, FLOAT_SERIAL_NO, FIRMWARE_VERSION, WMO_INST_TYPE, time, JULD_QC, JULD_LOCATION, latitude, longitude, POSITION_QC, POSITIONING_SYSTEM, PROFILE_PRES_QC, PROFILE_TEMP_QC, PROFILE_PSAL_QC, VERTICAL_SAMPLING_SCHEME, CONFIG_MISSION_NUMBER, PRES, PRES_QC, PRES_ADJUSTED, PRES_ADJUSTED_QC, PRES_ADJUSTED_ERROR, TEMP, TEMP_QC, TEMP_ADJUSTED, TEMP_ADJUSTED_QC, TEMP_ADJUSTED_ERROR, PSAL, PSAL_QC, PSAL_ADJUSTED, PSAL_ADJUSTED_QC, PSAL_ADJUSTED_ERROR", 
                "");

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
    public static void testBasic() throws Throwable {
        String2.log("\n****************** EDDTableFromMultidimNcFiles.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String dir = EDStatic.fullTestCacheDirectory;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        String id = "argoFloats";
        deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromMultidimNcFiles test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  fileNumber {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"File Number\";\n" +
"  }\n" +
"  data_type {\n" +
"    String conventions \"Argo reference table 1\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Data type\";\n" +
"  }\n" +
"  format_version {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"File format version\";\n" +
"  }\n" +
"  handbook_version {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Data handbook version\";\n" +
"  }\n" +
"  reference_date_time {\n" +
"    Float64 actual_range -6.31152e+8, -6.31152e+8;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date of reference for Julian days\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  date_creation {\n" +
"    Float64 actual_range 1.083872349e+9, 1.240402753e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date of file creation\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  date_update {\n" +
"    Float64 actual_range 1.446058107e+9, 1.460753242e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date of update of this file\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  platform_number {\n" +
"    String cf_role \"trajectory_id\";\n" +
"    String conventions \"WMO float identifier : A9IIIII\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Float unique identifier\";\n" +
"  }\n" +
"  project_name {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Name of the project\";\n" +
"  }\n" +
"  pi_name {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Name of the principal investigator\";\n" +
"  }\n" +
"  cycle_number {\n" +
"    Int32 _FillValue 99999;\n" +
"    Int32 actual_range 0, 256;\n" +
"    String cf_role \"profile_id\";\n" +
"    Float64 colorBarMaximum 200.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"0...N, 0 : launch cycle (if exists), 1 : first complete cycle\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Float cycle number\";\n" +
"  }\n" +
"  direction {\n" +
"    Int16 actual_range 65, 65;\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"A: ascending profiles, D: descending profiles\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Direction of the station profiles\";\n" +
"  }\n" +
"  data_center {\n" +
"    String conventions \"Argo reference table 4\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Data centre in charge of float data processing\";\n" +
"  }\n" +
"  dc_reference {\n" +
"    String conventions \"Data centre convention\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station unique identifier in data centre\";\n" +
"  }\n" +
"  data_state_indicator {\n" +
"    String conventions \"Argo reference table 6\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Degree of processing the data have passed through\";\n" +
"  }\n" +
"  data_mode {\n" +
"    Int16 actual_range 65, 68;\n" +
"    String conventions \"R : real time; D : delayed mode; A : real time with adjustment\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Delayed mode or real time data\";\n" +
"  }\n" +
"  platform_type {\n" +
"    String conventions \"Argo reference table 23\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Type of float\";\n" +
"  }\n" +
"  float_serial_no {\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Serial number of the float\";\n" +
"  }\n" +
"  firmware_version {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Instrument firmware version\";\n" +
"  }\n" +
"  wmo_inst_type {\n" +
"    String conventions \"Argo reference table 8\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Coded instrument type\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 _FillValue NaN;\n" +
"    Float64 actual_range 1.03197888e+9, 1.460630588e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Julian day (UTC) of the station relative to REFERENCE_DATE_TIME\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  time_qc {\n" +
"    Int16 actual_range 49, 49;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Quality on date and time\";\n" +
"  }\n" +
"  time_location {\n" +
"    Float64 _FillValue NaN;\n" +
"    Float64 actual_range 1.03197888e+9, 1.460630588e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Julian day (UTC) of the location relative to REFERENCE_DATE_TIME\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 _FillValue 99999.0;\n" +
"    Float64 actual_range 19.875999450683594, 38.83700180053711;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude of the station, best estimate\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"    Float64 valid_max 90.0;\n" +
"    Float64 valid_min -90.0;\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 _FillValue 99999.0;\n" +
"    Float64 actual_range -30.612000000000002, 162.89100646972656;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude of the station, best estimate\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float64 valid_max 180.0;\n" +
"    Float64 valid_min -180.0;\n" +
"  }\n" +
"  position_qc {\n" +
"    Int16 actual_range 49, 49;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Quality on position (latitude and longitude)\";\n" +
"  }\n" +
"  positioning_system {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Positioning system\";\n" +
"  }\n" +
"  profile_pres_qc {\n" +
"    Int16 actual_range 65, 70;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2a\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Global quality flag of PRES profile\";\n" +
"  }\n" +
"  profile_temp_qc {\n" +
"    Int16 actual_range 65, 70;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2a\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Global quality flag of TEMP profile\";\n" +
"  }\n" +
"  profile_psal_qc {\n" +
"    Int16 actual_range 65, 70;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2a\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Global quality flag of PSAL profile\";\n" +
"  }\n" +
"  vertical_sampling_scheme {\n" +
"    String conventions \"Argo reference table 16\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Vertical sampling scheme\";\n" +
"  }\n" +
"  config_mission_number {\n" +
"    Int32 _FillValue 99999;\n" +
"    Int32 actual_range 1, 2;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"1...N, 1 : first complete mission\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Unique number denoting the missions performed by the float\";\n" +
"  }\n" +
"  pres {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 4.5, 2014.0;\n" +
"    String axis \"Z\";\n" +
"    String C_format \"%7.1f\";\n" +
"    Float64 colorBarMaximum 5000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String FORTRAN_format \"F7.1\";\n" +
"    String ioos_category \"Sea Level\";\n" +
"    String long_name \"Sea water pressure, equals 0 at sea-level\";\n" +
"    String standard_name \"sea_water_pressure\";\n" +
"    String units \"decibar\";\n" +
"    Float32 valid_max 12000.0;\n" +
"    Float32 valid_min 0.0;\n" +
"  }\n" +
"  pres_qc {\n" +
"    Int16 actual_range 49, 52;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"quality flag\";\n" +
"  }\n" +
"  pres_adjusted {\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 4.0, 2014.0;\n" +
"    String axis \"Z\";\n" +
"    String C_format \"%7.1f\";\n" +
"    Float64 colorBarMaximum 5000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String FORTRAN_format \"F7.1\";\n" +
"    String ioos_category \"Sea Level\";\n" +
"    String long_name \"Sea water pressure, equals 0 at sea-level\";\n" +
"    String standard_name \"sea_water_pressure\";\n" +
"    String units \"decibar\";\n" +
"    Float32 valid_max 12000.0;\n" +
"    Float32 valid_min 0.0;\n" +
"  }\n" +
"  pres_adjusted_qc {\n" +
"    Int16 actual_range 32, 52;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"quality flag\";\n" +
"  }\n" +
"  pres_aqdjusted_error {\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 2.4, 5.0;\n" +
"    String C_format \"%7.1f\";\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String FORTRAN_format \"F7.1\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n" +
"    String units \"decibar\";\n" +
"  }\n" +
"  temp {\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 1.805, 33.258;\n" +
"    String C_format \"%9.3f\";\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String FORTRAN_format \"F9.3\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea temperature in-situ ITS-90 scale\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_Celsius\";\n" +
"    Float32 valid_max 40.0;\n" +
"    Float32 valid_min -2.5;\n" +
"  }\n" +
"  temp_qc {\n" +
"    Int16 actual_range 49, 52;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"quality flag\";\n" +
"  }\n" +
"  temp_adjusted {\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 1.805, 33.258;\n" +
"    String C_format \"%9.3f\";\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String FORTRAN_format \"F9.3\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea temperature in-situ ITS-90 scale\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_Celsius\";\n" +
"    Float32 valid_max 40.0;\n" +
"    Float32 valid_min -2.5;\n" +
"  }\n" +
"  temp_adjusted_qc {\n" +
"    Int16 actual_range 32, 52;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"quality flag\";\n" +
"  }\n" +
"  temp_adjusted_error {\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 0.002, 0.01;\n" +
"    String C_format \"%9.3f\";\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String FORTRAN_format \"F9.3\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n" +
"    String units \"degree_Celsius\";\n" +
"  }\n" +
"  psal {\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 33.346, 37.349;\n" +
"    String C_format \"%9.3f\";\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String FORTRAN_format \"F9.3\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Practical salinity\";\n" +
"    String standard_name \"sea_water_practical_salinity\";\n" +
"    String units \"PSU\";\n" +
"    Float32 valid_max 41.0;\n" +
"    Float32 valid_min 2.0;\n" +
"  }\n" +
"  psal_qc {\n" +
"    Int16 actual_range 49, 52;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"quality flag\";\n" +
"  }\n" +
"  psal_adjusted {\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 33.47303, 37.70372;\n" +
"    String C_format \"%9.3f\";\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String FORTRAN_format \"F9.3\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Practical salinity\";\n" +
"    String standard_name \"sea_water_practical_salinity\";\n" +
"    String units \"PSU\";\n" +
"    Float32 valid_max 41.0;\n" +
"    Float32 valid_min 2.0;\n" +
"  }\n" +
"  psal_adjusted_qc {\n" +
"    Int16 actual_range 32, 52;\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String conventions \"Argo reference table 2\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"quality flag\";\n" +
"  }\n" +
"  psal_adjusted_error {\n" +
"    Float32 _FillValue 99999.0;\n" +
"    Float32 actual_range 0.01, 0.01;\n" +
"    String C_format \"%9.3f\";\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String FORTRAN_format \"F9.3\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n" +
"    String units \"psu\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_altitude_proxy \"pres\";\n" +
"    String cdm_data_type \"TrajectoryProfile\";\n" +
"    String cdm_profile_variables \"cycle_number, data_type, format_version, handbook_version, reference_date_time, date_creation, date_update, direction, data_center, dc_reference, data_state_indicator, data_mode, firmware_version, wmo_inst_type, time, time_qc, time_location, latitude, longitude, position_qc, positioning_system, profile_pres_qc, profile_temp_qc, profile_psal_qc, vertical_sampling_scheme\";\n" +
"    String cdm_trajectory_variables \"platform_number, project_name, pi_name, platform_type, float_serial_no\";\n" +
"    String Conventions \"Argo-3.1, CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"support@argo.net\";\n" +
"    String creator_name \"Argo\";\n" +
"    String creator_url \"http://www.argo.net/\";\n" +
"    Float64 Easternmost_Easting 162.89100646972656;\n" +
"    String featureType \"TrajectoryProfile\";\n" +
"    Float64 geospatial_lat_max 38.83700180053711;\n" +
"    Float64 geospatial_lat_min 19.875999450683594;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 162.89100646972656;\n" +
"    Float64 geospatial_lon_min -30.612000000000002;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \""; 
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

//2016-05-09T15:34:11Z (local files)
//2016-05-09T15:34:11Z http://localhost:8080/cwexperimental/tabledap/testMultidimNc.das\";
expected=
   "String infoUrl \"http://www.argo.net/\";\n" +
"    String institution \"Argo\";\n" +
"    String keywords \"adjusted, argo, array, assembly, best, centre, centres, charge, coded, CONFIG_MISSION_NUMBER, contains, coriolis, creation, currents, cycle, CYCLE_NUMBER, data, DATA_CENTRE, DATA_MODE, DATA_STATE_INDICATOR, DATA_TYPE, date, DATE_CREATION, DATE_UPDATE, day, days, DC_REFERENCE, degree, delayed, denoting, density, determined, direction, equals, error, estimate, file, firmware, FIRMWARE_VERSION, flag, float, FLOAT_SERIAL_NO, format, FORMAT_VERSION, gdac, geostrophic, global, handbook, HANDBOOK_VERSION, have, identifier, in-situ, instrument, investigator, its, its-90, JULD, JULD_LOCATION, JULD_QC, julian, latitude, level, longitude, missions, mode, name, number, ocean, oceanography, oceans,\n" +
"Oceans > Ocean Pressure > Water Pressure,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"passed, performed, PI_NAME, PLATFORM_NUMBER, PLATFORM_TYPE, position, POSITION_QC, positioning, POSITIONING_SYSTEM, practical, pres, PRES_ADJUSTED, PRES_ADJUSTED_ERROR, PRES_ADJUSTED_QC, PRES_QC, pressure, principal, process, processing, profile, PROFILE_PRES_QC, PROFILE_PSAL_QC, PROFILE_TEMP_QC, profiles, project, PROJECT_NAME, psal, PSAL_ADJUSTED, PSAL_ADJUSTED_ERROR, PSAL_ADJUSTED_QC, PSAL_QC, quality, rdac, real, real time, real-time, realtime, reference, REFERENCE_DATE_TIME, regional, relative, salinity, sampling, scale, scheme, sea, sea level, sea-level, sea_water_practical_salinity, sea_water_pressure, sea_water_temperature, seawater, serial, situ, station, statistics, system, TEMP, TEMP_ADJUSTED, TEMP_ADJUSTED_ERROR, TEMP_ADJUSTED_QC, TEMP_QC, temperature, through, time, type, unique, update, values, version, vertical, VERTICAL_SAMPLING_SCHEME, water, WMO_INST_TYPE\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 38.83700180053711;\n" +
"    String references \"http://www.argodatamgt.org/Documentation\";\n" +
"    String source \"Argo float\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 19.875999450683594;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"platform_number, project_name, pi_name, platform_type, float_serial_no, cycle_number, data_type, format_version, handbook_version, reference_date_time, date_creation, date_update, direction, data_center, dc_reference, data_state_indicator, data_mode, firmware_version, wmo_inst_type, time, time_qc, time_location, latitude, longitude, position_qc, positioning_system, profile_pres_qc, profile_temp_qc, profile_psal_qc, vertical_sampling_scheme\";\n" +
"    String summary \"Argo float vertical profiles from Coriolis Global Data Assembly Centres\n" +
"(GDAC). Argo is an international collaboration that collects high-quality\n" +
"temperature and salinity profiles from the upper 2000m of the ice-free\n" +
"global ocean and currents from intermediate depths. The data come from\n" +
"battery-powered autonomous floats that spend most of their life drifting\n" +
"at depth where they are stabilised by being neutrally buoyant at the\n" +
"\\\"parking depth\\\" pressure by having a density equal to the ambient pressure\n" +
"and a compressibility that is less than that of sea water. At present there\n" +
"are several models of profiling float used in Argo. All work in a similar\n" +
"fashion but differ somewhat in their design characteristics. At typically\n" +
"10-day intervals, the floats pump fluid into an external bladder and rise\n" +
"to the surface over about 6 hours while measuring temperature and salinity.\n" +
"Satellites or GPS determine the position of the floats when they surface,\n" +
"and the floats transmit their data to the satellites. The bladder then\n" +
"deflates and the float returns to its original density and sinks to drift\n" +
"until the cycle is repeated. Floats are designed to make about 150 such\n" +
"cycles.\n" +
"Data Management URL: http://www.argodatamgt.org/Documentation\";\n" +
"    String time_coverage_end \"2016-04-14T10:43:08Z\";\n" +
"    String time_coverage_start \"2002-09-14T04:48:00Z\";\n" +
"    String title \"Argo Float Vertical Profiles\";\n" +
"    String user_manual_version \"3.1\";\n" +
"    Float64 Westernmost_Easting -30.612000000000002;\n" +
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
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String fileNumber;\n" +
"    String data_type;\n" +
"    String format_version;\n" +
"    String handbook_version;\n" +
"    Float64 reference_date_time;\n" +
"    Float64 date_creation;\n" +
"    Float64 date_update;\n" +
"    String platform_number;\n" +
"    String project_name;\n" +
"    String pi_name;\n" +
"    Int32 cycle_number;\n" +
"    Int16 direction;\n" +
"    String data_center;\n" +
"    String dc_reference;\n" +
"    String data_state_indicator;\n" +
"    Int16 data_mode;\n" +
"    String platform_type;\n" +
"    String float_serial_no;\n" +
"    String firmware_version;\n" +
"    String wmo_inst_type;\n" +
"    Float64 time;\n" +
"    Int16 time_qc;\n" +
"    Float64 time_location;\n" +
"    Float64 latitude;\n" +
"    Float64 longitude;\n" +
"    Int16 position_qc;\n" +
"    String positioning_system;\n" +
"    Int16 profile_pres_qc;\n" +
"    Int16 profile_temp_qc;\n" +
"    Int16 profile_psal_qc;\n" +
"    String vertical_sampling_scheme;\n" +
"    Int32 config_mission_number;\n" +
"    Float32 pres;\n" +
"    Int16 pres_qc;\n" +
"    Float32 pres_adjusted;\n" +
"    Int16 pres_adjusted_qc;\n" +
"    Float32 pres_aqdjusted_error;\n" +
"    Float32 temp;\n" +
"    Int16 temp_qc;\n" +
"    Float32 temp_adjusted;\n" +
"    Int16 temp_adjusted_qc;\n" +
"    Float32 temp_adjusted_error;\n" +
"    Float32 psal;\n" +
"    Int16 psal_qc;\n" +
"    Float32 psal_adjusted;\n" +
"    Int16 psal_adjusted_qc;\n" +
"    Float32 psal_adjusted_error;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromMultidimNcFiles.test make DATA FILES\n");       

        //.csv    for one lat,lon 26.587,154.853
        userDapQuery = "" +
            "&longitude=154.853&latitude=26.587";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1profile", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc,positioning_system,profile_pres_qc,profile_temp_qc,profile_psal_qc,vertical_sampling_scheme,config_mission_number,pres,pres_qc,pres_adjusted,pres_adjusted_qc,pres_aqdjusted_error,temp,temp_qc,temp_adjusted,temp_adjusted_qc,temp_adjusted_error,psal,psal_qc,psal_adjusted,psal_adjusted_qc,psal_adjusted_error\n" +
",,,,UTC,UTC,UTC,,,,,,,,,,,,,,UTC,,UTC,degrees_north,degrees_east,,,,,,,,decibar,,decibar,,decibar,degree_Celsius,,degree_Celsius,,degree_Celsius,PSU,,PSU,,psu\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,4.6,49,4.0,49,NaN,23.123,49,23.123,49,NaN,35.288,49,35.288,49,NaN\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,9.7,49,9.1,49,NaN,23.131,49,23.131,49,NaN,35.289,49,35.289,49,NaN\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,20.5,49,19.9,49,NaN,23.009,49,23.009,49,NaN,35.276,49,35.276,49,NaN\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,1850.0,49,1849.4,49,NaN,2.106,49,2.106,49,NaN,34.604,49,34.604,49,NaN\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,1899.9,49,1899.3,49,NaN,2.055,49,2.055,49,NaN,34.612,49,34.612,49,NaN\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,1950.0,49,1949.4,49,NaN,2.014,49,2.014,49,NaN,34.617,49,34.617,49,NaN\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv    for one lat,lon      via lon > <
        userDapQuery = "" +
            "&longitude>154.852&longitude<=154.854";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1StationGTLT", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc,positioning_system,profile_pres_qc,profile_temp_qc,profile_psal_qc,vertical_sampling_scheme,config_mission_number,pres,pres_qc,pres_adjusted,pres_adjusted_qc,pres_aqdjusted_error,temp,temp_qc,temp_adjusted,temp_adjusted_qc,temp_adjusted_error,psal,psal_qc,psal_adjusted,psal_adjusted_qc,psal_adjusted_error\n" +
",,,,UTC,UTC,UTC,,,,,,,,,,,,,,UTC,,UTC,degrees_north,degrees_east,,,,,,,,decibar,,decibar,,decibar,degree_Celsius,,degree_Celsius,,degree_Celsius,PSU,,PSU,,psu\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,4.6,49,4.0,49,NaN,23.123,49,23.123,49,NaN,35.288,49,35.288,49,NaN\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,9.7,49,9.1,49,NaN,23.131,49,23.131,49,NaN,35.289,49,35.289,49,NaN\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,20.5,49,19.9,49,NaN,23.009,49,23.009,49,NaN,35.276,49,35.276,49,NaN\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,1850.0,49,1849.4,49,NaN,2.106,49,2.106,49,NaN,34.604,49,34.604,49,NaN\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,1899.9,49,1899.3,49,NaN,2.055,49,2.055,49,NaN,34.612,49,34.612,49,NaN\n" +
"2901175,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA ARGO PROJECT,JIANPING XU,256,65,HZ,0066_80617_256,2B,65,APEX,4136,013108,846,2016-04-14T10:43:08Z,49,2016-04-14T10:43:08Z,26.587,154.853,49,ARGOS,65,65,65,Primary sampling: discrete,1,1950.0,49,1949.4,49,NaN,2.014,49,2.014,49,NaN,34.617,49,34.617,49,NaN\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting scalar var        
        userDapQuery = "data_type&data_type=~\".*go.*\"";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_scalar", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"data_type\n" +
"\n" +
"Argo profile\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv for test requesting distinct        
        userDapQuery = "pres&pres>10&pres<10.5&distinct()";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_scalar", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"pres\n" +
"decibar\n" +
"10.1\n" +
"10.2\n" +
"10.3\n" +
"10.4\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
/* */
        testGenerateDatasetsXml();
        testBasic();

        /* */
    }
}

