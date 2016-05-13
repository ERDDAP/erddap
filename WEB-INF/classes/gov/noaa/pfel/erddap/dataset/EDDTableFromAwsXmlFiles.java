/* 
 * EDDTableFromAwsXmlFiles Copyright 2012, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
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
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.FileInputStream;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

/** 
 * This class represents a table of data from a collection of AWS 
 * (Automatic Weather Station) XML data files (namespace http://www.aws.com/aws).
 * http://developer.weatherbug.com/docs/read/WeatherBug_Rest_XML_API
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2012-11-05
 */
public class EDDTableFromAwsXmlFiles extends EDDTableFromFiles { 

    /** 
     * The constructor just calls the super constructor. 
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * <p>The sortedColumnSourceName isn't utilized.
     */
    public EDDTableFromAwsXmlFiles(String tDatasetID, 
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
        boolean tSourceNeedsExpandedFP_EQ, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles,
        boolean tRemoveMVRows) 
        throws Throwable {

        super("EDDTableFromAwsXmlFiles", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, 
            tFileTableInMemory, tAccessibleViaFiles,
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

        Table table = new Table();
        table.readAwsXmlFile(fileDir + fileName);

        //convert to desired sourceDataTypes
        int nCols = table.nColumns();
        for (int tc = 0; tc < nCols; tc++) {
            int sd = sourceDataNames.indexOf(table.getColumnName(tc));
            if (sd >= 0) {
                PrimitiveArray pa = table.getColumn(tc);
                if (!sourceDataTypes[sd].equals(pa.elementClassString())) {
                    PrimitiveArray newPa = PrimitiveArray.factory(
                        PrimitiveArray.elementStringToClass(sourceDataTypes[sd]), 1, false);
                    newPa.append(pa);
                    table.setColumn(tc, newPa);
                }
            }
        }

        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromAwsXmlFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private ascii files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     * @param sampleFileName one of the files in the collection
     * @param columnNamesRow first row of file is called 1.
     * @param firstDataRow   first row if file is called 1.
     * @param tReloadEveryNMinutes
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortedColumnSourceName   use "" if not known or not needed. 
     * @param tSortFilesBySourceNames   This is useful, because it ultimately determines default results order.
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available (but try hard!)
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available (but try hard!)
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available (but try hard!)
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available (but try hard!)
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String tFileDir, String tFileNameRegex, 
        String sampleFileName, 
        int columnNamesRow, int firstDataRow, int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromAwsXmlFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);
        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis
        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        //*** basically, make a table to hold the sourceAttributes 
        Table dataSourceTable = new Table();
        dataSourceTable.readAwsXmlFile(sampleFileName);

        //simplify dataSourceTable to make a guess for data types
        //but prevent zip from becoming a String
        int zipCol = dataSourceTable.findColumnNumber("city-state-zip");
        if (zipCol >= 0)
            dataSourceTable.setStringData(zipCol, 0, 
                "A" + dataSourceTable.getStringData(zipCol, 0));
        dataSourceTable.simplify();
        if (zipCol >= 0)
            dataSourceTable.setStringData(zipCol, 0, 
                dataSourceTable.getStringData(zipCol, 0).substring(1));

        //and make a parallel table to hold addAttributes
        Table dataAddTable = new Table();

        //globalAttributes 
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");

        for (int col = 0; col < dataSourceTable.nColumns(); col++) {
            String colName = dataSourceTable.getColumnName(col);
            Attributes sourceAtts = dataSourceTable.columnAttributes(col);
            dataAddTable.addColumn(col, colName,
                (PrimitiveArray)dataSourceTable.getColumn(col).clone(),
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    null, //no source global attributes
                    sourceAtts, colName, true, true)); //addColorBarMinMax, tryToFindLLAT
            Attributes addAtts = dataAddTable.columnAttributes(col);

            //if a variable has timeUnits, files are likely sorted by time
            //and no harm if files aren't sorted that way
            boolean hasTimeUnits = EDVTimeStamp.hasTimeUnits(sourceAtts, null);
            if (tSortedColumnSourceName.length() == 0 && hasTimeUnits)
                tSortedColumnSourceName = dataSourceTable.getColumnName(col);

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

        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataSourceTable, dataAddTable)? "Point" : "Other",
                tFileDir, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //write the information
        StringBuilder sb = new StringBuilder();
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = (tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName)).trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            " * Since the source files don't have any metadata, you must add metadata\n" +
            "   below, notably 'units' for each of the dataVariables.\n" +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromAwsXmlFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + tFileNameRegex) + 
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + 
            "</updateEveryNMillis>\n" +  
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
            "    <recursive>true</recursive>\n" +
            "    <pathRegex>.*</pathRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <columnNamesRow>" + columnNamesRow + "</columnNamesRow>\n" +
            "    <firstDataRow>" + firstDataRow + "</firstDataRow>\n" +
            "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            "    <columnNameForExtract>" + XML.encodeAsXML(tColumnNameForExtract) + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) + "</sortedColumnSourceName>\n" +
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
            Attributes externalAddAttributes = new Attributes();
            externalAddAttributes.add("title", "New Title!");
            String results = generateDatasetsXml(
                "c:/data/aws/xml/",  ".*\\.xml",
                "c:/data/aws/xml/SNFLS-2012-11-03T20_30_01Z.xml", 
                1, 2, 1440,
                "", "-.*$", ".*", "fileName",  //just for test purposes; station is already a column in the file
                "ob-date", "station-id ob-date", 
                "http://www.exploratorium.edu", "exploratorium", "The new summary!", "The Newer Title!",
                externalAddAttributes) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromAwsXmlFiles",
                "c:/data/aws/xml/",  ".*\\.xml",
                "c:/data/aws/xml/SNFLS-2012-11-03T20_30_01Z.xml", 
                "1", "2", "1440",
                "", "-.*$", ".*", "fileName",  //just for test purposes; station is already a column in the file
                "ob-date", "station-id ob-date", 
                "http://www.exploratorium.edu", "exploratorium", "The new summary!", "The Newer Title!"},
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
" * Since the source files don't have any metadata, you must add metadata\n" +
"   below, notably 'units' for each of the dataVariables.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromAwsXmlFiles\" datasetID=\"xml_fa11_d004_6990\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>c:/data/aws/xml/</fileDir>\n" +
"    <fileNameRegex>.*\\.xml</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <preExtractRegex></preExtractRegex>\n" +
"    <postExtractRegex>-.*$</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>fileName</columnNameForExtract>\n" +
"    <sortedColumnSourceName>ob-date</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>station-id ob-date</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">exploratorium</att>\n" +
"        <att name=\"creator_url\">http://www.exploratorium.edu</att>\n" +
"        <att name=\"infoUrl\">http://www.exploratorium.edu</att>\n" +
"        <att name=\"institution\">exploratorium</att>\n" +
"        <att name=\"keywords\">air, altitude, atmosphere,\n" +
"Atmosphere &gt; Altitude &gt; Station Height,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Dew Point Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Water Vapor &gt; Dew Point Temperature,\n" +
"Atmosphere &gt; Atmospheric Water Vapor &gt; Humidity,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"atmospheric, aux, aux-temp, aux-temp-rate, bulb, city, city-state, city-state-zip, currents, data, date, dew, dew point, dew-point, dew_point_temperature, direction, elevation, exploratorium, feels, feels-like, file, fileName, gust, gust-direction, gust-speed, gust-time, height, high, humidity, humidity-high, humidity-low, humidity-rate, identifier, img, indoor, indoor-temp, indoor-temp-rate, light, light-rate, like, low, max, meteorology, month, moon, moon-phase, moon-phase-moon-phase-img, name, newer, ob-date, phase, point, precipitation, pressure, pressure-high, pressure-low, pressure-rate, rain, rain-month, rain-rate, rain-rate-max, rain-today, rain-year, rainfall, rate, relative, relative_humidity, site, site-url, speed, state, station, station-id, sunrise, sunset, surface, temp-high, temp-low, temp-rate, temperature, time, title, today, vapor, water, wet, wet-bulb, wet_bulb_temperature, wind, wind-direction, wind-direction-avg, wind-speed, wind-speed-avg, wind_from_direction, wind_speed, wind_speed_of_gust, winds, year, zip</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">The new summary! exploratorium data from a local source.</att>\n" +
"        <att name=\"title\">The Newer Title!</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>fileName</sourceName>\n" +
"        <destinationName>fileName</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">File Name</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ob-date</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Ob-date</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>station-id</sourceName>\n" +
"        <destinationName>station_id</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station-id</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>station</sourceName>\n" +
"        <destinationName>station</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>city-state-zip</sourceName>\n" +
"        <destinationName>city_state_zip</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">City-state-zip</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>city-state</sourceName>\n" +
"        <destinationName>city_state</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">City-state</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>site-url</sourceName>\n" +
"        <destinationName>site_url</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Site-url</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aux-temp</sourceName>\n" +
"        <destinationName>aux_temp</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">104.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">14.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Aux-temp</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aux-temp-rate</sourceName>\n" +
"        <destinationName>aux_temp_rate</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Aux-temp-rate</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>dew-point</sourceName>\n" +
"        <destinationName>dew_point</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">104.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">14.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Dew Point Temperature</att>\n" +
"            <att name=\"standard_name\">dew_point_temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>elevation</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">ft</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Altitude</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.3048</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>feels-like</sourceName>\n" +
"        <destinationName>feels_like</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">104.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">14.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Feels-like</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>gust-time</sourceName>\n" +
"        <destinationName>gust_time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Gust-time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>gust-direction</sourceName>\n" +
"        <destinationName>gust_direction</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"            <att name=\"long_name\">Gust-direction</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>gust-speed</sourceName>\n" +
"        <destinationName>gust_speed</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">mph</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Wind Speed Of Gust</att>\n" +
"            <att name=\"standard_name\">wind_speed_of_gust</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>humidity</sourceName>\n" +
"        <destinationName>humidity</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">&#37;</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Relative Humidity</att>\n" +
"            <att name=\"standard_name\">relative_humidity</att>\n" +
"            <att name=\"units\">percent</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>humidity-high</sourceName>\n" +
"        <destinationName>humidity_high</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">&#37;</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Relative Humidity</att>\n" +
"            <att name=\"standard_name\">relative_humidity</att>\n" +
"            <att name=\"units\">percent</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>humidity-low</sourceName>\n" +
"        <destinationName>humidity_low</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">&#37;</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Relative Humidity</att>\n" +
"            <att name=\"standard_name\">relative_humidity</att>\n" +
"            <att name=\"units\">percent</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>humidity-rate</sourceName>\n" +
"        <destinationName>humidity_rate</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Humidity-rate</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>indoor-temp</sourceName>\n" +
"        <destinationName>indoor_temp</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">104.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">14.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Indoor-temp</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>indoor-temp-rate</sourceName>\n" +
"        <destinationName>indoor_temp_rate</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Indoor-temp-rate</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>light</sourceName>\n" +
"        <destinationName>light</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Light</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>light-rate</sourceName>\n" +
"        <destinationName>light_rate</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Light-rate</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>moon-phase-moon-phase-img</sourceName>\n" +
"        <destinationName>moon_phase_moon_phase_img</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Moon-phase-moon-phase-img</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>moon-phase</sourceName>\n" +
"        <destinationName>moon_phase</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Moon-phase</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>pressure</sourceName>\n" +
"        <destinationName>pressure</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inch_Hg</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Pressure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>pressure-high</sourceName>\n" +
"        <destinationName>pressure_high</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inch_Hg</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Pressure-high</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>pressure-low</sourceName>\n" +
"        <destinationName>pressure_low</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inch_Hg</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Pressure-low</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>pressure-rate</sourceName>\n" +
"        <destinationName>pressure_rate</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inch_Hg/h</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Pressure-rate</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>rain-month</sourceName>\n" +
"        <destinationName>rain_month</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inches</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Rain-month</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>rain-rate</sourceName>\n" +
"        <destinationName>rain_rate</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inches/h</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Rain-rate</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>rain-rate-max</sourceName>\n" +
"        <destinationName>rain_rate_max</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inches/h</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Rain-rate-max</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>rain-today</sourceName>\n" +
"        <destinationName>rain_today</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inches</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Rain-today</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>rain-year</sourceName>\n" +
"        <destinationName>rain_year</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">inches</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"long_name\">Rain-year</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>temp</sourceName>\n" +
"        <destinationName>temp</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">104.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">14.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>temp-high</sourceName>\n" +
"        <destinationName>temp_high</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">104.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">14.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Temp-high</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>temp-low</sourceName>\n" +
"        <destinationName>temp_low</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">104.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">14.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Temp-low</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>temp-rate</sourceName>\n" +
"        <destinationName>temp_rate</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Temp-rate</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sunrise</sourceName>\n" +
"        <destinationName>sunrise</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Sunrise</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sunset</sourceName>\n" +
"        <destinationName>sunset</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Sunset</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wet-bulb</sourceName>\n" +
"        <destinationName>wet_bulb</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degree_F</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">104.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">14.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Wet Bulb Temperature</att>\n" +
"            <att name=\"standard_name\">wet_bulb_temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wind-speed</sourceName>\n" +
"        <destinationName>wind_speed</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">mph</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Wind Speed</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wind-speed-avg</sourceName>\n" +
"        <destinationName>wind_speed_avg</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">mph</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Wind Speed</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wind-direction</sourceName>\n" +
"        <destinationName>wind_direction</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Wind From Direction</att>\n" +
"            <att name=\"standard_name\">wind_from_direction</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wind-direction-avg</sourceName>\n" +
"        <destinationName>wind_direction_avg</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Wind From Direction</att>\n" +
"            <att name=\"standard_name\">wind_from_direction</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //ensure it is ready-to-use by making a dataset from it
            //!!! actually this will fail with a specific error which is caught below
            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), "xml_fa11_d004_6990", "");
            Test.ensureEqual(edd.title(), "The Newer Title!", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
"fileName, time, station_id, station, city_state_zip, city_state, site_url, aux_temp, aux_temp_rate, dew_point, altitude, feels_like, gust_time, gust_direction, gust_speed, humidity, humidity_high, humidity_low, humidity_rate, indoor_temp, indoor_temp_rate, light, light_rate, moon_phase_moon_phase_img, moon_phase, pressure, pressure_high, pressure_low, pressure_rate, rain_month, rain_rate, rain_rate_max, rain_today, rain_year, temp, temp_high, temp_low, temp_rate, sunrise, sunset, wet_bulb, wind_speed, wind_speed_avg, wind_direction, wind_direction_avg", "");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error using generateDatasetsXml."); 
        }

    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromAwsXmlFiles.test() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10); 

        String id = "testAwsXml";
        if (deleteCachedDatasetInfo)
            deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromAwsXmlFiles test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  fileName {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"File Name\";\n" +
"  }\n" +
"  station_id {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Station-id\";\n" +
"  }\n" +
"  station {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Station\";\n" +
"  }\n" +
"  city_state_zip {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"City-state-zip\";\n" +
"  }\n" +
"  city_state {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"City-state\";\n" +
"  }\n" +
"  site_url {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Site-url\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.3519746e+9, 1.3519746e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  aux_temp {\n" +
"    Float32 actual_range 32.0, 32.0;\n" +
"    Float64 colorBarMaximum 104.0;\n" +
"    Float64 colorBarMinimum 14.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Aux-temp\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  aux_temp_rate {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Aux-temp-rate\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  dew_point {\n" +
"    Float32 actual_range 54.0, 54.0;\n" +
"    Float64 colorBarMaximum 104.0;\n" +
"    Float64 colorBarMinimum 14.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Dew Point Temperature\";\n" +
"    String standard_name \"dew_point_temperature\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  feels_like {\n" +
"    Float32 actual_range 67.0, 67.0;\n" +
"    Float64 colorBarMaximum 104.0;\n" +
"    Float64 colorBarMinimum 14.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Feels-like\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  gust_time {\n" +
"    Float64 actual_range 1.3519746e+9, 1.3519746e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Gust-time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  gust_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Gust-direction\";\n" +
"  }\n" +
"  gust_speed {\n" +
"    Float32 actual_range 8.0, 8.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed Of Gust\";\n" +
"    String standard_name \"wind_speed_of_gust\";\n" +
"    String units \"mph\";\n" +
"  }\n" +
"  humidity {\n" +
"    Float32 actual_range 63.0, 63.0;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Relative Humidity\";\n" +
"    String standard_name \"relative_humidity\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  humidity_high {\n" +
"    Float32 actual_range 100.0, 100.0;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Relative Humidity\";\n" +
"    String standard_name \"relative_humidity\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  humidity_low {\n" +
"    Float32 actual_range 63.0, 63.0;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Relative Humidity\";\n" +
"    String standard_name \"relative_humidity\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  humidity_rate {\n" +
"    Float32 actual_range -6.0, -6.0;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Humidity-rate\";\n" +
"  }\n" +
"  indoor_temp {\n" +
"    Float32 actual_range 90.0, 90.0;\n" +
"    Float64 colorBarMaximum 104.0;\n" +
"    Float64 colorBarMinimum 14.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Indoor-temp\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  indoor_temp_rate {\n" +
"    Float32 actual_range 4.6, 4.6;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Indoor-temp-rate\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  light {\n" +
"    Float32 actual_range 67.9, 67.9;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Light\";\n" +
"  }\n" +
"  light_rate {\n" +
"    Float32 actual_range -0.3, -0.3;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Light-rate\";\n" +
"  }\n" +
"  moon_phase_moon_phase_img {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Moon-phase-moon-phase-img\";\n" +
"  }\n" +
"  moon_phase {\n" +
"    Byte actual_range 82, 82;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Moon-phase\";\n" +
"  }\n" +
"  pressure {\n" +
"    Float32 actual_range 30.1, 30.1;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Pressure\";\n" +
"    String units \"inch_Hg\";\n" +
"  }\n" +
"  pressure_high {\n" +
"    Float32 actual_range 30.14, 30.14;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Pressure-high\";\n" +
"    String units \"inch_Hg\";\n" +
"  }\n" +
"  pressure_low {\n" +
"    Float32 actual_range 30.06, 30.06;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Pressure-low\";\n" +
"    String units \"inch_Hg\";\n" +
"  }\n" +
"  pressure_rate {\n" +
"    Float32 actual_range -0.01, -0.01;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Pressure-rate\";\n" +
"    String units \"inch_Hg/h\";\n" +
"  }\n" +
"  rain_month {\n" +
"    Float32 actual_range 0.21, 0.21;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Rain-month\";\n" +
"    String units \"inches\";\n" +
"  }\n" +
"  rain_rate {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Rain-rate\";\n" +
"    String units \"inches/h\";\n" +
"  }\n" +
"  rain_rate_max {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Rain-rate-max\";\n" +
"    String units \"inches/h\";\n" +
"  }\n" +
"  rain_today {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Rain-today\";\n" +
"    String units \"inches\";\n" +
"  }\n" +
"  rain_year {\n" +
"    Float32 actual_range 1.76, 1.76;\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Rain-year\";\n" +
"    String units \"inches\";\n" +
"  }\n" +
"  temp {\n" +
"    Float32 actual_range 66.9, 66.9;\n" +
"    Float64 colorBarMaximum 104.0;\n" +
"    Float64 colorBarMinimum 14.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Temp\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  temp_high {\n" +
"    Float32 actual_range 67.0, 67.0;\n" +
"    Float64 colorBarMaximum 104.0;\n" +
"    Float64 colorBarMinimum 14.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Temp-high\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  temp_low {\n" +
"    Float32 actual_range 52.0, 52.0;\n" +
"    Float64 colorBarMaximum 104.0;\n" +
"    Float64 colorBarMinimum 14.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Temp-low\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  temp_rate {\n" +
"    Float32 actual_range 3.8, 3.8;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Temp-rate\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  sunrise {\n" +
"    Float64 actual_range 1.351953497e+9, 1.351953497e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Sunrise\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sunset {\n" +
"    Float64 actual_range 1.351991286e+9, 1.351991286e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Sunset\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  wet_bulb {\n" +
"    Float32 actual_range 59.162, 59.162;\n" +
"    Float64 colorBarMaximum 104.0;\n" +
"    Float64 colorBarMinimum 14.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Wet Bulb Temperature\";\n" +
"    String standard_name \"wet_bulb_temperature\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  wind_speed {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed\";\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"mph\";\n" +
"  }\n" +
"  wind_speed_avg {\n" +
"    Float32 actual_range 2.0, 2.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed\";\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"mph\";\n" +
"  }\n" +
"  wind_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind From Direction\";\n" +
"    String standard_name \"wind_from_direction\";\n" +
"  }\n" +
"  wind_direction_avg {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind From Direction\";\n" +
"    String standard_name \"wind_from_direction\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Other\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_name \"exploratorium\";\n" +
"    String creator_url \"http://www.exploratorium.edu\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today; //T16:36:59Z (local files)\n" +
//"2012-11-21T16:36:59Z http://localhost:8080/cwexperimental/tabledap/testAwsXml.das\";\n" +
String expected2 = 
"    String infoUrl \"http://www.exploratorium.edu\";\n" +
"    String institution \"exploratorium\";\n" +
"    String keywords \"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"atmosphere, atmospheric, aux, aux-temp, aux-temp-rate, bulb, city, city-state, city-state-zip, dew point, dew_point_temperature, direction, elevation, exploratorium, feels, feels-like, file, from, gust, high, humidity, humidity-high, humidity-low, humidity-rate, identifier, img, indoor, indoor-temp, indoor-temp-rate, light, light-rate, like, low, max, meteorology, month, moon, moon-phase, moon-phase-moon-phase-img, name, newer, phase, pressure, pressure-high, pressure-low, pressure-rate, rain, rain-month, rain-rate, rain-rate-max, rain-today, rain-year, rate, site, site-url, speed, state, station, station-id, surface, temp-high, temp-low, temp-rate, temperature, time, title, today, url, vapor, water, wet, wet-bulb, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, year, zip\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"fileName, station_id, station, city_state_zip, city_state, site_url, altitude\";\n" +
"    String summary \"The new summary!\";\n" +
"    String time_coverage_end \"2012-11-03T20:30:00Z\";\n" +
"    String time_coverage_start \"2012-11-03T20:30:00Z\";\n" +
"    String title \"The Newer Title!\";\n" +
"  }\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
        int tPo = results.indexOf(expected2.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected2.length())),
            expected2, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String fileName;\n" +
"    String station_id;\n" +
"    String station;\n" +
"    String city_state_zip;\n" +
"    String city_state;\n" +
"    String site_url;\n" +
"    Float32 altitude;\n" +
"    Float64 time;\n" +
"    Float32 aux_temp;\n" +
"    Float32 aux_temp_rate;\n" +
"    Float32 dew_point;\n" +
"    Float32 feels_like;\n" +
"    Float64 gust_time;\n" +
"    String gust_direction;\n" +
"    Float32 gust_speed;\n" +
"    Float32 humidity;\n" +
"    Float32 humidity_high;\n" +
"    Float32 humidity_low;\n" +
"    Float32 humidity_rate;\n" +
"    Float32 indoor_temp;\n" +
"    Float32 indoor_temp_rate;\n" +
"    Float32 light;\n" +
"    Float32 light_rate;\n" +
"    String moon_phase_moon_phase_img;\n" +
"    Byte moon_phase;\n" +
"    Float32 pressure;\n" +
"    Float32 pressure_high;\n" +
"    Float32 pressure_low;\n" +
"    Float32 pressure_rate;\n" +
"    Float32 rain_month;\n" +
"    Float32 rain_rate;\n" +
"    Float32 rain_rate_max;\n" +
"    Float32 rain_today;\n" +
"    Float32 rain_year;\n" +
"    Float32 temp;\n" +
"    Float32 temp_high;\n" +
"    Float32 temp_low;\n" +
"    Float32 temp_rate;\n" +
"    Float64 sunrise;\n" +
"    Float64 sunset;\n" +
"    Float32 wet_bulb;\n" +
"    Float32 wind_speed;\n" +
"    Float32 wind_speed_avg;\n" +
"    String wind_direction;\n" +
"    String wind_direction_avg;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromAwsXmlFiles.test make DATA FILES\n");       

        //.csv    for one lat,lon,time
        //46012 -122.879997    37.360001
        userDapQuery = "&fileName=~\"SNFLS|zztop\"";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"fileName,station_id,station,city_state_zip,city_state,site_url,altitude,time,aux_temp,aux_temp_rate,dew_point,feels_like,gust_time,gust_direction,gust_speed,humidity,humidity_high,humidity_low,humidity_rate,indoor_temp,indoor_temp_rate,light,light_rate,moon_phase_moon_phase_img,moon_phase,pressure,pressure_high,pressure_low,pressure_rate,rain_month,rain_rate,rain_rate_max,rain_today,rain_year,temp,temp_high,temp_low,temp_rate,sunrise,sunset,wet_bulb,wind_speed,wind_speed_avg,wind_direction,wind_direction_avg\n" +
",,,,,,m,UTC,degree_F,degree_F,degree_F,degree_F,UTC,,mph,percent,percent,percent,,degree_F,degree_F,,,,,inch_Hg,inch_Hg,inch_Hg,inch_Hg/h,inches,inches/h,inches/h,inches,inches,degree_F,degree_F,degree_F,degree_F,UTC,UTC,degree_F,mph,mph,,\n" +
"SNFLS,SNFLS,Exploratorium,94123,\"San Francisco, CA\",,0.0,2012-11-03T20:30:00Z,32.0,0.0,54.0,67.0,2012-11-03T20:30:00Z,E,8.0,63.0,100.0,63.0,-6.0,90.0,4.6,67.9,-0.3,mphase16.gif,82,30.1,30.14,30.06,-0.01,0.21,0.0,0.0,0.0,1.76,66.9,67.0,52.0,3.8,2012-11-03T14:38:17Z,2012-11-04T01:08:06Z,59.162,0.0,2.0,ENE,E\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);



    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        testGenerateDatasetsXml();
        testBasic(true);
        testBasic(false);

        //not usually run
    }


}

