/* 
 * EDDTableFromAsciiFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** 
 * This class represents a table of data from a collection of ASCII CSV or TSV data files.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-02-13
 */
public class EDDTableFromAsciiFiles extends EDDTableFromFiles { 


    /** Used to ensure that all non-axis variables in all files have the same leftmost dimension. */
    protected String dim0Name = null;


    /** 
     * The constructor just calls the super constructor. 
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * <p>The sortedColumnSourceName isn't utilized.
     */
    public EDDTableFromAsciiFiles(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File,
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex,
        String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles,
        boolean tRemoveMVRows) 
        throws Throwable {

        super("EDDTableFromAsciiFiles", tDatasetID, 
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
            tFileTableInMemory, tAccessibleViaFiles, tRemoveMVRows);
    }

    /** The constructor for subclasses. */
    public EDDTableFromAsciiFiles(String tClassName, String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles,
        boolean tRemoveMVRows) 
        throws Throwable {

        super(tClassName, tDatasetID, tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, 
            tMetadataFrom,
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

        if (!mustGetData) 
            //Just return an empty table. There is never any metadata.
            return Table.makeEmptyTable(sourceDataNames.toArray(), sourceDataTypes);

        Table table = new Table();
        table.allowRaggedRightInReadASCII = true;
        table.readASCII(fileDir + fileName, 
            charset, columnNamesRow - 1, firstDataRow - 1,
            null, null, null, //testColumns, testMin, testMax,
            sourceDataNames.toArray(), //loadColumns, 
            false); //don't simplify; just get the strings

        //convert to desired sourceDataTypes
        int nCols = table.nColumns();
        for (int tc = 0; tc < nCols; tc++) {
            int sd = sourceDataNames.indexOf(table.getColumnName(tc));
            if (sd >= 0) {
                PrimitiveArray pa = table.getColumn(tc);
                String tType = sourceDataTypes[sd];
                if (tType.equals("String")) { //do nothing
                } else if (tType.equals("boolean")) {
                    table.setColumn(tc, ByteArray.toBooleanToByte(pa));
                } else { 
                    PrimitiveArray newPa;
                    if (tType.equals("char")) {
                        CharArray ca = new CharArray();
                        int n = pa.size();
                        for (int i = 0; i < n; i++)
                            ca.add(CharArray.firstChar(pa.getString(i)));
                        newPa = ca;
                    } else {
                        newPa = PrimitiveArray.factory(
                            PrimitiveArray.elementStringToClass(sourceDataTypes[sd]), 1, false);
                        newPa.append(pa);
                    }
                    table.setColumn(tc, newPa);
                }
            }
        }

        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromAsciiFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to look at (possibly) private ascii files on the server.
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
        String charset, int columnNamesRow, int firstDataRow, 
        int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromAsciiFiles.generateDatasetsXml" +
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
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        if (charset == null || charset.length() == 0)
            charset = "ISO-8859-1";
        dataSourceTable.readASCII(sampleFileName, charset, columnNamesRow-1, firstDataRow-1,
            null, null, null, null, true);  //simplify

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

        boolean dateTimeAlreadyFound = false;
        for (int col = 0; col < dataSourceTable.nColumns(); col++) {
            String colName = dataSourceTable.getColumnName(col);
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                null, //no source global attributes
                dataSourceTable.columnAttributes(col), colName, 
                true, true); //addColorBarMinMax, tryToFindLLAT

            //dateTime?
            PrimitiveArray pa = (PrimitiveArray)dataSourceTable.getColumn(col).clone();
            boolean isDateTime = false;
            if (pa instanceof StringArray) {
                String dtFormat = Calendar2.suggestDateTimeFormat((StringArray)pa);
                if (dtFormat.length() > 0) { 
                    isDateTime = true;
                    addAtts.set("units", dtFormat);
                }
            }

            //add to dataAddTable
            dataAddTable.addColumn(col, colName, pa, addAtts);

            //files are likely sorted by first date time variable
            //and no harm if files aren't sorted that way
            if (tSortedColumnSourceName.length() == 0 && 
                isDateTime && !dateTimeAlreadyFound) {
                dateTimeAlreadyFound = true;
                tSortedColumnSourceName = colName;
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
            "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"" + 
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
            "    <charset>" + charset + "</charset>\n" +
            "    <columnNamesRow>" + columnNamesRow + "</columnNamesRow>\n" +
            "    <firstDataRow>" + firstDataRow + "</firstDataRow>\n" +
            "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
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
            String suggDatasetID = suggestDatasetID(
                EDStatic.unitTestDataDir + "asciiNdbc/.*\\.csv");
            String results = generateDatasetsXml(
                EDStatic.unitTestDataDir + "asciiNdbc/",  ".*\\.csv",
                EDStatic.unitTestDataDir + "asciiNdbc/31201_2009.csv", 
                "ISO-8859-1", 1, 3, 1440,
                "", "_.*$", ".*", "stationID",  //just for test purposes; station is already a column in the file
                "time", "station time", 
                "http://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!",
                externalAddAttributes) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromAsciiFiles",
                EDStatic.unitTestDataDir + "asciiNdbc/",  ".*\\.csv",
                EDStatic.unitTestDataDir + "asciiNdbc/31201_2009.csv", 
                "ISO-8859-1", "1", "3", "1440",
                "", "_.*$", ".*", "stationID",  //just for test purposes; station is already a column in the file
                "time", "station time", 
                "http://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!"},
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
" * Since the source files don't have any metadata, you must add metadata\n" +
"   below, notably 'units' for each of the dataVariables.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "asciiNdbc/</fileDir>\n" +
"    <fileNameRegex>.*\\.csv</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>3</firstDataRow>\n" +
"    <preExtractRegex></preExtractRegex>\n" +
"    <postExtractRegex>_.*$</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>stationID</columnNameForExtract>\n" +
"    <sortedColumnSourceName>time</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>station time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">webmaster.ndbc@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA NDBC</att>\n" +
"        <att name=\"creator_url\">http://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"infoUrl\">http://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"institution\">NOAA NDBC</att>\n" +
"        <att name=\"keywords\">altitude, atmosphere,\n" +
"Atmosphere &gt; Altitude &gt; Station Height,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"atmospheric, atmp, buoy, center, data, direction, height, identifier, latitude, longitude, national, ndbc, newer, noaa, speed, station, stationID, surface, temperature, time, title, water, wind, wind_from_direction, wind_speed, winds, wspd, wtmp</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">The new summary! NOAA National Data Buoy Center (NDBC) data from a local source.</att>\n" +
"        <att name=\"title\">The Newer Title!</att>\n" +
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
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>altitude</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Altitude</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">yyyy-MM</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>station</sourceName>\n" +
"        <destinationName>station</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wd</sourceName>\n" +
"        <destinationName>wd</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
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
"        <sourceName>wspd</sourceName>\n" +
"        <destinationName>wspd</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
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
"        <sourceName>atmp</sourceName>\n" +
"        <destinationName>atmp</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Atmp</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wtmp</sourceName>\n" +
"        <destinationName>wtmp</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Water Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            //2014-12-24 no longer: this will fail with a specific error which is caught below
            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), suggDatasetID, "");
            Test.ensureEqual(edd.title(), "The Newer Title!", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "stationID, longitude, latitude, altitude, time, station, wd, wspd, atmp, wtmp",
                "");

        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
//2014-12-24 no longer occurs
//            if (msg.indexOf(
//"When a variable's destinationName is \"altitude\", the sourceAttributes or addAttributes " +
//"\"units\" MUST be \"m\" (not \"null\").\n" +
//"If needed, use \"scale_factor\" to convert the source values to meters (positive=up),\n" +
//"use a different destinationName for this variable.") >= 0) {
//                String2.log("EXPECTED ERROR while creating the edd: altitude's units haven't been set.\n");
//            } else 
                String2.pressEnterToContinue(msg + 
                    "\nUnexpected error using generateDatasetsXml."); 
        }

    }

    /**
     * This tests the methods in this class with a 1D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromAsciiFiles.test() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        String id = "testTableAscii";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromAsciiFiles test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -122.88, -48.13;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -27.7, 37.75;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Int16 actual_range 0, 0;\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1045376e+9, 1.167606e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  station {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station\";\n" +
"  }\n" +
"  wd {\n" +
"    Int16 actual_range 0, 359;\n" +
"    Float64 colorBarMaximum 360.0;\n" + 
"    Float64 colorBarMinimum 0.0;\n" + 
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind From Direction\";\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  wspd {\n" +
"    Float32 actual_range 0.0, 18.9;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed\";\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  atmp {\n" +
"    Float32 actual_range 5.4, 18.4;\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temperature\";\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  wtmp {\n" +
"    Float32 actual_range 9.3, 32.2;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Water Temperature\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station, longitude, latitude, altitude\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -48.13;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 37.75;\n" +
"    Float64 geospatial_lat_min -27.7;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -48.13;\n" +
"    Float64 geospatial_lon_min -122.88;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//+ " The source URL.\n" +
//today + " http://localhost:8080/cwexperimental/tabledap/
expected =
"testTableAscii.das\";\n" +
"    String infoUrl \"The Info Url\";\n" +
"    String institution \"NDBC\";\n" +
"    String keywords \"Atmosphere > Atmospheric Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 37.75;\n" +
"    String sourceUrl \"The source URL.\";\n" +
"    Float64 Southernmost_Northing -27.7;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"station, longitude, latitude, altitude\";\n" +
"    String summary \"The summary.\";\n" +
"    String time_coverage_end \"2006-12-31T23:00:00Z\";\n" +
"    String time_coverage_start \"2005-01-01T00:00:00Z\";\n" +
"    String title \"The Title\";\n" +
"    Float64 Westernmost_Easting -122.88;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Int16 altitude;\n" +
"    Float64 time;\n" +
"    String station;\n" +
"    Int16 wd;\n" +
"    Float32 wspd;\n" +
"    Float32 atmp;\n" +
"    Float32 wtmp;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromAsciiFiles.test make DATA FILES\n");       

        //.csv    for one lat,lon,time
        //46012 -122.879997    37.360001
        userDapQuery = "&longitude=-122.88&latitude=37.36&time>=2005-07-01&time<2005-07-01T10";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,altitude,time,station,wd,wspd,atmp,wtmp\n" +
"degrees_east,degrees_north,m,UTC,,m s-1,m s-1,degree_C,degree_C\n" +
"-122.88,37.36,0,2005-07-01T00:00:00Z,46012,294,2.6,12.7,13.4\n" +
"-122.88,37.36,0,2005-07-01T01:00:00Z,46012,297,3.5,12.6,13.0\n" +
"-122.88,37.36,0,2005-07-01T02:00:00Z,46012,315,4.0,12.2,12.9\n" +
"-122.88,37.36,0,2005-07-01T03:00:00Z,46012,325,4.2,11.9,12.8\n" +
"-122.88,37.36,0,2005-07-01T04:00:00Z,46012,330,4.1,11.8,12.8\n" +
"-122.88,37.36,0,2005-07-01T05:00:00Z,46012,321,4.9,11.8,12.8\n" +
"-122.88,37.36,0,2005-07-01T06:00:00Z,46012,320,4.4,12.1,12.8\n" +
"-122.88,37.36,0,2005-07-01T07:00:00Z,46012,325,3.8,12.4,12.8\n" +
"-122.88,37.36,0,2005-07-01T08:00:00Z,46012,298,4.0,12.5,12.8\n" +
"-122.88,37.36,0,2005-07-01T09:00:00Z,46012,325,4.0,12.5,12.8\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv    for one station,time
        userDapQuery = "&station=\"46012\"&time>=2005-07-01&time<2005-07-01T10";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //same expected
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species
        userDapQuery = "&time=2005-07-01";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_3", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,altitude,time,station,wd,wspd,atmp,wtmp\n" +
"degrees_east,degrees_north,m,UTC,,m s-1,m s-1,degree_C,degree_C\n" +
"-48.13,-27.7,0,2005-07-01T00:00:00Z,31201,NaN,NaN,NaN,NaN\n" +
"-122.88,37.36,0,2005-07-01T00:00:00Z,46012,294,2.6,12.7,13.4\n" +
"-122.82,37.75,0,2005-07-01T00:00:00Z,46026,273,2.5,12.6,14.6\n" +
"-121.89,35.74,0,2005-07-01T00:00:00Z,46028,323,4.2,14.7,14.8\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species   String > <
        userDapQuery = "&wtmp>32";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_4", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,altitude,time,station,wd,wspd,atmp,wtmp\n" +
"degrees_east,degrees_north,m,UTC,,m s-1,m s-1,degree_C,degree_C\n" +
"-48.13,-27.7,0,2005-05-07T18:00:00Z,31201,NaN,NaN,NaN,32.2\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv for test station regex
        userDapQuery = "longitude,latitude,altitude,time,station,atmp,wtmp" + 
            "&time=2005-07-01&station=~\"(46012|46026|zztop)\"";  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_5", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,altitude,time,station,atmp,wtmp\n" +
"degrees_east,degrees_north,m,UTC,,degree_C,degree_C\n" +
"-122.88,37.36,0,2005-07-01T00:00:00Z,46012,12.7,13.4\n" +
"-122.82,37.75,0,2005-07-01T00:00:00Z,46026,12.6,14.6\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /** 
     * This tests some aspects of fixedValue variables 
     * (with and without subsetVariables). */
    public static void testFixedValue() throws Throwable {

        String2.log("\n****************** EDDTableFromAsciiFiles.testFixedValue() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        for (int test = 0; test < 2; test++) {
            //!fixedValue variable is the only subsetVariable
            String id = test == 0? "testWTDLwSV" : "testWTDLwoSV"; //with and without subsetVariables
            EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

            //test getting das for entire dataset
            tName = eddTable.makeNewFileForDapQuery(null, null, "", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fv" + test, ".das"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
    "Attributes {\n" +
    " s {\n" +
    "  ship_call_sign {\n" +
    "    String cf_role \"trajectory_id\";\n" +
    "    String ioos_category \"Other\";\n" +
    "  }\n" +
    "  time {\n" +
    "    String _CoordinateAxisType \"Time\";\n" +
    "    Float64 actual_range 1.365167919e+9, 1.36933224e+9;\n" +
    "    String axis \"T\";\n" +
    "    String ioos_category \"Time\";\n" +
    "    String long_name \"Time\";\n" +
    "    String standard_name \"time\";\n" +
    "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
    "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
    "  }\n" +
    "  latitude {\n" +
    "    String _CoordinateAxisType \"Lat\";\n" +
    "    Float32 actual_range 26.6255, 30.368;\n" +
    "    String axis \"Y\";\n" +
    "    String ioos_category \"Location\";\n" +
    "    String long_name \"Latitude\";\n" +
    "    String standard_name \"latitude\";\n" +
    "    String units \"degrees_north\";\n" +
    "  }\n" +
    "  longitude {\n" +
    "    String _CoordinateAxisType \"Lon\";\n" +
    "    Float32 actual_range 263.2194, 274.2898;\n" +
    "    String axis \"X\";\n" +
    "    String ioos_category \"Location\";\n" +
    "    String long_name \"Longitude\";\n" +
    "    String standard_name \"longitude\";\n" +
    "    String units \"degrees_east\";\n" +
    "  }\n" +
    "  seaTemperature {\n" +
    "    Float32 _FillValue -8888.0;\n" +
    "    Float32 actual_range 16.9, 25.9;\n" +
    "    Float64 colorBarMaximum 40.0;\n" +
    "    Float64 colorBarMinimum -10.0;\n" +
    "    String ioos_category \"Temperature\";\n" +
    "    String long_name \"Sea Water Temperature\";\n" +
    "    String standard_name \"sea_water_temperature\";\n" +
    "    String units \"degrees_C\";\n" +
    "  }\n" +
    " }\n" +
    "  NC_GLOBAL {\n" +
    "    String cdm_data_type \"Trajectory\";\n" +
    "    String cdm_trajectory_variables \"ship_call_sign\";\n" +
    "    String Conventions \"COARDS, CF-1.4, ACDD-1.3\";\n" +
    "    String creator_email \"eed.shiptracker@noaa.gov\";\n" +
    "    String creator_name \"NOAA OMAO,Ship Tracker\";\n" +
    "    Float64 Easternmost_Easting 274.2898;\n" +
    "    String featureType \"Trajectory\";\n" +
    "    Float64 geospatial_lat_max 30.368;\n" +
    "    Float64 geospatial_lat_min 26.6255;\n" +
    "    String geospatial_lat_units \"degrees_north\";\n" +
    "    Float64 geospatial_lon_max 274.2898;\n" +
    "    Float64 geospatial_lon_min 263.2194;\n" +
    "    String geospatial_lon_units \"degrees_east\";\n" +
    "    String history \"Data downloaded hourly from http://shiptracker.noaa.gov/shiptracker.html to ERD\n" +
    today;
    //        "2013-05-24T17:24:54Z (local files)\n" +
    //"2013-05-24T17:24:54Z http://localhost:8080/cwexperimental/tabledap/testWTDL.das\";\n" +
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected=
        "String infoUrl \"http://shiptracker.noaa.gov/\";\n" +
    "    String institution \"NOAA OMAO\";\n" +
    "    String license \"The data may be used and redistributed for free but is not intended\n" +
    "for legal use, since it may contain inaccuracies. Neither the data\n" +
    "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
    "of their employees or contractors, makes any warranty, express or\n" +
    "implied, including warranties of merchantability and fitness for a\n" +
    "particular purpose, or assumes any legal liability for the accuracy,\n" +
    "completeness, or usefulness, of this information.\";\n" +
    "    Float64 Northernmost_Northing 30.368;\n" +
    "    String sourceUrl \"(local files)\";\n" +
    "    Float64 Southernmost_Northing 26.6255;\n" +
    "    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
    (test == 0? "    String subsetVariables \"ship_call_sign\";\n" : "") +
    "    String summary \"NOAA Ship Pisces Realtime Data updated every hour\";\n" +
    "    String time_coverage_end \"2013-05-23T18:04:00Z\";\n" +
    "    String time_coverage_start \"2013-04-05T13:18:39Z\";\n" +
    "    String title \"NOAA Ship Pisces Underway Meteorological Data, Realtime\";\n" +
    "    Float64 Westernmost_Easting 263.2194;\n" +
    "  }\n" +
    "}\n";
            int po = results.indexOf("String infoUrl");
            Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

            //test getting just the fixed value variable
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "ship_call_sign&ship_call_sign!=\"zztop\"", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fv" + test, ".csv"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
    "ship_call_sign\n" +
    "\n" +
    "WTDL\n";
            Test.ensureEqual(results, expected, "results=\n" + results);
        }
    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic2() throws Throwable {
        String2.log("\n*** EDDTableFromAsciiFiles.testBasic2() \n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String testDir = EDStatic.fullTestCacheDirectory;

        String id = "testTableAscii2";
        deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //does aBoolean know it's a boolean?
        Test.ensureTrue(eddTable.findVariableByDestinationName("aBoolean").isBoolean(), 
            "Is aBoolean edv.isBoolean() true?");

        //.csv    for all
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_all", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        //String2.log(results);
        expected = 
"fileName,five,aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n" +
",,,,,,,,,,\n" +
"csvAscii,5.0,\"b,d\",65,1,24,24000,24000000,240000000000,2.4,2.412345678987654\n" +
"csvAscii,5.0,short:,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"csvAscii,5.0,fg,70,1,11,12001,1200000,12000000000,1.21,1.0E200\n" +
"csvAscii,5.0,h,72,1,12,12002,120000,1200000000,1.22,2.0E200\n" +
"csvAscii,5.0,i,73,1,13,12003,12000,120000000,1.23,3.0E200\n" +
"csvAscii,5.0,j,74,0,14,12004,1200,12000000,1.24,4.0E200\n" +
"csvAscii,5.0,k,75,0,15,12005,120,1200000,1.25,5.0E200\n" +
"csvAscii,5.0,l,76,0,16,12006,12,120000,1.26,6.0E200\n" +
"csvAscii,5.0,m,77,0,17,12007,121,12000,1.27,7.0E200\n" +
"csvAscii,5.0,n,78,1,18,12008,122,1200,1.28,8.0E200\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test getting das for entire dataset
        String2.log("\nEDDTableFromAsciiFiles test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", testDir, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  fileName {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"File Name\";\n" +
"  }\n" +
"  five {\n" +
"    Float32 actual_range 5.0, 5.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Five\";\n" +
"  }\n" +
"  aString {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A String\";\n" +
"  }\n" +
"  aChar {\n" +
"    Int16 actual_range 65, 78;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Char\";\n" +
"  }\n" +
"  aBoolean {\n" +
"    Byte actual_range 0, 1;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Boolean\";\n" +
"  }\n" +
"  aByte {\n" +
"    Byte actual_range 11, 24;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Byte\";\n" +
"  }\n" +
"  aShort {\n" +
"    Int16 actual_range 12001, 24000;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Short\";\n" +
"  }\n" +
"  anInt {\n" +
"    Int32 actual_range 12, 24000000;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"An Int\";\n" +
"  }\n" +
"  aLong {\n" +
"    Float64 actual_range 1200, 240000000000;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Long\";\n" +
"  }\n" +
"  aFloat {\n" +
"    Float32 actual_range 1.21, 2.4;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Float\";\n" +
"  }\n" +
"  aDouble {\n" +
"    Float64 actual_range 2.412345678987654, 8.0e+200;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Double\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Other\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_name \"NOAA NDBC\";\n" +
"    String creator_url \"http://www.ndbc.noaa.gov/\";\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//"2014-12-04T19:15:21Z (local files)
//2014-12-04T19:15:21Z http://localhost:8080/cwexperimental/tabledap/testTableAscii.das";
expected =
"    String infoUrl \"http://www.ndbc.noaa.gov/\";\n" +
"    String institution \"NOAA NDBC\";\n" +
"    String keywords \"boolean, byte, char, double, float, int, long, ndbc, newer, noaa, short, string, title\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"five, fileName\";\n" +
"    String summary \"The new summary!\";\n" +
"    String title \"The Newer Title!\";\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 20));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", testDir, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String fileName;\n" +
"    Float32 five;\n" +
"    String aString;\n" +
"    Int16 aChar;\n" +
"    Byte aBoolean;\n" +
"    Byte aByte;\n" +
"    Int16 aShort;\n" +
"    Int32 anInt;\n" +
"    Float64 aLong;\n" +
"    Float32 aFloat;\n" +
"    Float64 aDouble;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //only subsetVars
        userDapQuery = "fileName,five";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_sv", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        expected = 
"fileName,five\n" +
",\n" +
"csvAscii,5.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //subset of variables, constrain boolean and five
        userDapQuery = "anInt,fileName,five,aBoolean&aBoolean=1&five=5";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_conbool", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        expected = 
"anInt,fileName,five,aBoolean\n" +
",,,\n" +
"24000000,csvAscii,5.0,1\n" +
"1200000,csvAscii,5.0,1\n" +
"120000,csvAscii,5.0,1\n" +
"12000,csvAscii,5.0,1\n" +
"122,csvAscii,5.0,1\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromAsciiFiles.testBasic2() finished successfully\n");
    }



    /** 
     * This generates a chunk of datasets.xml from info in an inport.xml file.
     * InPort Users guide is at
     * http://ias.pifsc.noaa.gov/inp/docs/inp_userguide_web.pdf
     *
     * @param typeCodeRegex e.g., .*, ENT, DS, 
     * @param datasetTypeRegex e.g., .*, Database, .*[Ff]ile.*.
     *   null appears as a "" (a 0-length String).
     *   Tally results: "": 583, Database: 118, Mixed: 53, Other: 28, Files: 22, 
     *   SAS files: 2, CSV Files: 1, GIS: 1.
     */
    public static String generateDatasetsXmlFromInPort(String xmlFileName, 
        String typeCodeRegex, String datasetTypeRegex) throws Throwable {
        String2.log("\n*** inPortGenerateDatasetsXml(" + xmlFileName + ")");

        String2.log("Here's what is in the InPort .xml file:");
        {
            String readXml[] = String2.readFromFile(xmlFileName, "UTF-8", 1);
            if (readXml[0].length() > 0)
                throw new RuntimeException(readXml[0]);
            String2.log(readXml[1]);
        }

        //check parameters
        int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        String catID = File2.getNameNoExtension(xmlFileName);

        //create tables to hold results
        Table sourceTable = new Table();
        Table addTable    = new Table();
        Attributes sourceAtts = sourceTable.globalAttributes();
        Attributes addAtts    = addTable.globalAttributes();
        Attributes inPortAtts = new Attributes();
        boolean isFgdcPOC = false;
        boolean isSupportRoleOriginator = false;
        String creatorName2 = null, creatorName3 = null;
        String creatorEmail2 = null, creatorEmail3 = null;
        String metaCreatedBy = "???";
        String metaCreated = "???";
        String metaLastMod = "???";
        String dataPublicationDate = "???";
        String acronym = "???";  //institution acronym
        String title = "???";  
        String securityClass = "";
        //accumulate results from some tags
        StringBuilder background = new StringBuilder();
        int nChildEntities = 0;
        StringBuilder childEntities = new StringBuilder();
        StringBuilder dataQuality = new StringBuilder();
        StringBuilder dataset = new StringBuilder();
        int nDownloads = 0;
        StringBuilder downloads = new StringBuilder();
        int nFaqs = 0;
        StringBuilder faqs = new StringBuilder();
        StringBuilder history = new StringBuilder();
        int nIssues = 0;
        StringBuilder issues = new StringBuilder();
        HashSet<String> keywords = new HashSet();
        StringBuilder license = new StringBuilder();
        String lineageStepN = "", lineageName = "", lineageEmail = "", lineageDescription = "";
        int nRelatedItems = 0;
        StringBuilder relatedItems = new StringBuilder();
        int nUrls = 0;
        StringBuilder urls = new StringBuilder();

        HashMap<String,String> typesHM = new HashMap();
        typesHM.put("LIB", "Library");
        typesHM.put("PRJ", "Project");
        typesHM.put("DS",  "Data Set");
        typesHM.put("ENT", "Data Entity");
        typesHM.put("DOC", "Document");
        typesHM.put("PRC", "Procedure");
        
        HashMap<String,String> relationHM = new HashMap();
        relationHM.put("HI", "child");
        relationHM.put("RI", "other");

        //attributes that InPort doesn't help with
        String inportXmlUrl = "https://inport.nmfs.noaa.gov/inport-metadata/" +
            xmlFileName.substring("/u00/data/points/inportXml/".length()); 
        inPortAtts.add("cdm_data_type", "Point");
        inPortAtts.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
        inPortAtts.add("InPort_catalog_URL", inportXmlUrl);
        inPortAtts.add("keywords_vocabulary", "GCMD Science Keywords");
        inPortAtts.add("standard_name_vocabulary", "CF Standard Name Table v29");

        //process the inport.xml file 
        SimpleXMLReader xmlReader = new SimpleXMLReader(
            new FileInputStream(xmlFileName));
        xmlReader.nextTag();
        String tags = xmlReader.allTags();
        String startTag = "<catalog-item>";
        Test.ensureEqual(tags, startTag, 
            "Unexpected first tag"); 
        int startTagLength = startTag.length();

        //process the tags
//references?
        while (true) {
            xmlReader.nextTag();
            tags = xmlReader.allTags();
            int nTags = xmlReader.stackSize();
            String content = xmlReader.content();
            //if (debugMode) String2.log(">>  tags=" + tags + content);
            if (xmlReader.stackSize() == 1) 
                break; //the startTag
            String topTag = xmlReader.tag(nTags - 1);
            boolean hasContent = content.length() > 0 && !content.equals("NA");
            String localTags = tags.substring(startTagLength);
            String attName = String2.replaceAll(localTags, '-', '_');
            attName = String2.replaceAll(attName, "<", "");
            attName = String2.replaceAll(attName, '>', '_');
            attName = attName.substring(0, attName.length() - 1);
            if (debugMode) 
                String2.log(">>  attName=" + attName + (hasContent? "=" + content : ""));
            String attNameNoSlash = String2.replaceAll(attName, "/", "");
 
            //special cases: convert some InPort names to ERDDAP names
            //The order here matches the order in the files.

            //common_information
            if (attName.equals("common_information_/cat_id")) {                
                Test.ensureEqual(content, catID, "cat_id != fileName");
                inPortAtts.add("InPort_catalog_ID", content);
            } else if (attName.equals("common_information_/cat_type_code")) {                
                if (!content.matches(typeCodeRegex))
                    throw new RuntimeException("cat_type_code=" + content + 
                        " doesn't match regex=" + typeCodeRegex);
                String tType = typesHM.get(content);                      
                inPortAtts.add("InPort_catalog_type", tType == null? content : tType);
            } else if (attName.equals("common_information_/is-do-not-publish")) {                
                Test.ensureEqual(content, "N", "Unexpected id-do-not-publish content.");
            } else if (attName.equals("common_information_/title") && hasContent) {
                       inPortAtts.add("title", content); 
                       title = content;
            } else if (attName.equals("common_information_/abstract") && hasContent) {
                       inPortAtts.add("summary", content); 
            } else if (attName.equals("common_information_/purpose") && hasContent) {
                       inPortAtts.add("purpose", content); 
            //common_information_notes has info re InPort metadata creation
            } else if (attName.equals("common_information_/created_by") && hasContent) {
                       metaCreatedBy = content;
            } else if (attName.equals("common_information_/record_created") && hasContent) {
                       metaCreated = content;
            } else if (attName.equals("common_information_/record_last_modified") && hasContent) {
                       metaLastMod = content;
            } else if (attName.equals("common_information_/owner_org_acro") && hasContent) {
                       inPortAtts.add("institution", 
                xmlFileName.indexOf("/NOAA/NMFS/") > 0? "NOAA NMFS " + content: //e.g., SWFSC
                xmlFileName.indexOf("/NOAA/")      > 0? "NOAA " + content: 
                content); 
                acronym = content;
            } else if (attName.equals("common_information_/data_set_publication_date") && hasContent) {
                       inPortAtts.add("date_created", content); 
                       dataPublicationDate = content;
            } else if (attName.equals("common_information_/parent_title") && hasContent) {
                       //parent_title is often better than title
                       title = content + (title.equals("???")? "" : ", " + title);
            } else if (attName.equals("common_information_/notes") && hasContent) {
                       inPortAtts.add("comment", content); 

            //physical_location  org, city, state-province, country  see 1132
            //} else if (attName.startsWith("physical_location")) {

            //data_set   see /u00/data/points/InPortXML/NOAA/NMFS/AFSC/inport/xml/10657.xml
            //This info is relevant to the source, not after it's in ERDDAP.
            //So, don't add to ERDDAP meatadata. 
            } else if (attName.equals("data_set") && xmlReader.attributeValue("type") != null) {
                //Tally: "": 583, Database: 118, Mixed: 53, Other: 28, Files: 22, 
                //  SAS files: 2, CSV Files: 1, GIS: 1
                String dsType = xmlReader.attributeValue("type");
                if (dsType == null)
                    dsType = "";
                if (!dsType.matches(datasetTypeRegex))
                    throw new RuntimeException("data-set type=" + dsType + 
                        " doesn't match regex=" + datasetTypeRegex);
                       background.append("data_set_type=" + dsType + "\n");
            } else if (attName.equals("data_set_/maintenance_frequency") && hasContent) {
                       background.append("data_set_maintenance_frequency=" + content + "\n");
            } else if (attName.equals("data_set_/source_media_type") && hasContent) {
                       background.append("data_set_source_media=" + content + "\n");
            } else if (attName.equals("data_set_/entity_attribute_overview") && hasContent) {
                       background.append("data_set_overview=" + content + "\n");
            } else if (attName.equals("data_set_/distribution_liability") && hasContent) {
                                 license.append("Distribution Liability: " + content + "\n"); 
            } else if (attName.equals("data_set_/data_set_credit") && hasContent) {
                       inPortAtts.add("acknowledgment", content); 

            //keywords see 10657
            } else if ((attName.equals("keywords_theme_keywords_/keyword_list") ||
                        attName.equals("keywords_spatial_keywords_/keyword_list") ||
                        attName.equals("keywords_stratum_keywords_/keyword_list") ||
                        attName.equals("keywords_temporal_keywords_/keyword_list")) && hasContent) {
                chopUpCsvAddAllAndParts(content, keywords);

            //data_attributes  //this ASSUMES that EVERY attribute has a <name>
            } else if (attName.equals("data_attributes_attribute_/name") && hasContent) {
                StringArray sa = new StringArray();
                sourceTable.addColumn(content, sa);
                addTable.addColumn(content, sa);
            } else if (attName.equals("data_attributes_attribute_/data_storage_type") && hasContent) {
                //vs                                             general_data_type?
                int col = addTable.nColumns() - 1;
                Test.ensureTrue(col != -1, "attribute data-storage-type found before name");
                PrimitiveArray pa = null;
                String lcContent = content.toLowerCase();
                if        (String2.indexOf(new String[]{
                    "boolean", "logical"}, lcContent) >= 0) {
                    pa = new ByteArray();
                } else if (String2.indexOf(new String[]{
                    "smallint"}, lcContent) >= 0) {
                    pa = new ShortArray();
                } else if (String2.indexOf(new String[]{
                    "integer", "int", "year", "long integer"}, lcContent) >= 0) {
                    pa = new IntArray();
                //} else if (String2.indexOf(new String[]{
                //    "long integer"}, lcContent) >= 0) {
                //    pa = new LongArray();
                } else if (String2.indexOf(new String[]{
                    "float"}, lcContent) >= 0) {
                    pa = new FloatArray();
                } else if (String2.indexOf(new String[]{
                    "double", "numeric", "number", "numger", "currency"}, lcContent) >= 0) {
                    pa = new DoubleArray();
                } else if (String2.indexOf(new String[]{
                    "date", "date/time", "datetime", "time"}, lcContent) >= 0) {
                    //String time
                    addTable.columnAttributes(col).add("units", EDV.TIME_UNITS); //a guess/ a placeholder
                } //otherwise, leave as String    long->clob!, yes/no?
                if (pa != null) {
                    sourceTable.setColumn(col, pa);
                    addTable.setColumn(   col, pa);
                }
            } else if (attName.equals("data_attributes_attribute_/scale") && hasContent) { 
                //It isn't scale_factor. e.g., 0, 2, 1, 3, 5, 6, 9
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "scale", content);
            } else if (attName.equals("data_attributes_attribute_/max_length") && hasContent) {
                //e.g., 0(?!), 22, 1, 8, 100, 4000 (longest)
                int maxLen = String2.parseInt(content);
                if (maxLen > 0 && maxLen < 10000)
                    addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "max_length", "" + maxLen);
            } else if (attName.equals("data_attributes_attribute_/is_pkey") && hasContent) {
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "is_pkey", content);
            } else if (attName.equals("data_attributes_attribute_/description") && hasContent) {
                //widely used  (Is <description> used another way?)
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "description", content);
            } else if (attName.equals("data_attributes_attribute_/units") && hasContent) {
                //e.g., Decimal degrees, animal, KM, degrees celcius(sic), AlphaNumeric
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "units", content);
            } else if (attName.equals("data_attributes_attribute_/format_mask") && hasContent) {
                //e.g., $999,999.99, MM/DD/YYYY, HH:MM:SS, mm/dd/yyyy, HHMM
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "format_mask", content);
            } else if (attName.equals("data_attributes_attribute_/null_repr") && hasContent) {
                //no examples!
                int col = addTable.nColumns() - 1;
                PrimitiveArray pa = addTable.getColumn(col);
                double d = String2.parseDouble(content);
                if (pa instanceof ByteArray)
                    addTable.columnAttributes(col).add("missing_value", Math2.roundToByte(d));
                else if (pa instanceof ShortArray)
                    addTable.columnAttributes(col).add("missing_value", Math2.roundToShort(d));
                else if (pa instanceof IntArray)
                    addTable.columnAttributes(col).add("missing_value", Math2.roundToInt(d));
                else if (pa instanceof LongArray)
                    addTable.columnAttributes(col).add("missing_value", String2.parseLong(content));
                else if (pa instanceof FloatArray)
                    addTable.columnAttributes(col).add("missing_value", Math2.doubleToFloatNaN(d));
                else if (pa instanceof DoubleArray)
                    addTable.columnAttributes(col).add("missing_value", d);
                //else string?!
            } else if (attName.equals("data_attributes_attribute_/null_meaning") && hasContent) {
                //e.g., 1=Yes, The fish length is unknown or was not recorded.
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "null_meaning", content);
            } else if (attName.equals("data_attributes_attribute_/allowed_values") && hasContent) {
                //e.g., No domain defined., unknown, Free entry text field., text, "1, 2, 3", "False, True"
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "allowed_values", content);
            } else if (attName.equals("data_attributes_attribute_/derivation") && hasContent) {
                //no examples
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "derivation", content);
            } else if (attName.equals("data_attributes_attribute_/validation_rules") && hasContent) {
                //e.g., Gear must be a NMFS Gear Code
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "validation_rules", content);
            } else if (attName.equals("data_attributes_attribute_/version") && hasContent) {
                //no examples
                addTable.columnAttributes(addTable.nColumns() - 1).add(
                        "version", content);

            //support_roles  e.g., see 10657
            //Use role=Originator as backup for creator_name, creator_email
            } else if (attName.startsWith("support_roles")) {
                if (attName.equals("support_roles_support_role")) {
                    isSupportRoleOriginator = false;
                } else if (attName.equals("support_roles_support_role_/role") && hasContent) {
                    isSupportRoleOriginator = "Originator".equals(content);
                } else if (attName.equals("support_roles_support_role_person_contact_info_/name") && hasContent) {
                    int po = content.indexOf(", ");
                    creatorName3 = po > 0?    //any role
                        content.substring(po + 2) + " " + content.substring(0, po) :
                        content; 
                    if (isSupportRoleOriginator)
                        creatorName2 = creatorName3;
                        
                } else if (attName.equals("support_roles_support_role_person_contact_info_/email") && hasContent) {
                    creatorEmail3 = content;  //any role
                    if (isSupportRoleOriginator)
                        creatorEmail2 = creatorEmail3;
                }
                continue;  //multiple support_roles, so don't write just one to datasets.xml

            //geog_area
            } else if (attName.equals("geog_area_/description") && hasContent) {
                       inPortAtts.add("geospatial_description", content); 
            } else if (attName.equals("geog_area_/west_bound") && hasContent) {
                       inPortAtts.add("geospatial_lon_min", String2.parseDouble(content)); 
            } else if (attName.equals("geog_area_/east_bound") && hasContent) {
                       inPortAtts.add("geospatial_lon_max", String2.parseDouble(content)); 
            } else if (attName.equals("geog_area_/south_bound") && hasContent) {
                       inPortAtts.add("geospatial_lat_min", String2.parseDouble(content)); 
            } else if (attName.equals("geog_area_/north_bound") && hasContent) {
                       inPortAtts.add("geospatial_lat_max", String2.parseDouble(content)); 

            //time_frame
            } else if (attName.equals("time_frames_time_frame_/start") && hasContent) {
                       inPortAtts.add("time_coverage_start", content); 
            } else if (attName.equals("time_frames_time_frame_/end") && hasContent) {
                       inPortAtts.add("time_coverage_end", content); 

            //access_info    see AFSC/inport/xml/10657.xml
            } else if (attName.equals("access_info_/security_class") && hasContent) {
                license.append("Security class: " + content + "\n"); 
                inPortAtts.add("security_class", content);
                securityClass = content;
            } else if (attName.equals("access_info_/security_classification_system") && hasContent) {
                                    license.append("Security classification system: " + content + "\n"); 
            } else if (attName.equals("access_info_/data_access_policy") && hasContent) {
                                    license.append("Data access policy: " + content + "\n"); 
            } else if (attName.equals("access_info_/data_access_constraints") && hasContent) {
                                    license.append("Data access constraints: " + content + "\n"); 
            } else if (attName.equals("access_info_/data_access_procedure") && hasContent) {
                                    license.append("Data use procedure: " + content + "\n"); 
            } else if (attName.equals("access_info_/data_use_constraints") && hasContent) {
                                    license.append("Data use constraints: " + content + "\n"); 
            } else if (attName.equals("access_info_/metadata_access_constraints") && hasContent) {
                                    license.append("Metadata access constraints: " + content + "\n"); 
            } else if (attName.equals("access_info_/metadata_use_constraints") && hasContent) {
                                    license.append("Metadata use constraints: " + content + "\n"); 

            //urls   see 10657
            } else if (attName.startsWith("urls")) {
                if        (attName.equals("urls_url")) {
                              urls.append("URL #" + ++nUrls);
                } else if (attName.equals("urls_url_/link") && hasContent) {
                    //!!!???how know which url is best for infoUrl?  heuristic: always use first?
                    if (inPortAtts.get("infoUrl") == null)
                        inPortAtts.add("infoUrl", content); 
                    urls.append(": " + content + "\n");
                } else if (attName.equals("urls_url_/url_type") && hasContent) {
                    String2.addNewlineIfNone(urls).append("Type: " + content + "\n");
                } else if (attName.equals("urls_url_/description") && hasContent) {
                    String2.addNewlineIfNone(urls).append("Description: " + content + "\n");
                } else if (attName.equals("urls_/url")) {
                    String2.addNewlineIfNone(urls).append("\n");
                }
                continue; //multiple url's, so don't write just one to datasets.xml

            //activity_log -- just InPort activity

            //issues see /u00/data/points/InPortXML/NOAA/NMFS/PIFSC/inport/xml/18143.xml
            } else if (attName.startsWith("issues")) {
                if        (attName.equals("issues_issue")) {
                            issues.append("Issue #" + ++nIssues);
                } else if (attName.equals("issues_issue_/date") && hasContent) {
                            issues.append(": date=" + content);
                } else if (attName.equals("issues_issue_/author") && hasContent) {
                            issues.append(", author=" + content + "\n");
                } else if (attName.equals("issues_issue_/summary") && hasContent) {
                    String2.addNewlineIfNone(issues).append("Summary: " + content + "\n");
                } else if (attName.equals("issues_/issue")) {
                    String2.addNewlineIfNone(issues).append("\n");
                }
                continue; //multiple issues's, so don't write just one to datasets.xml

            //technical-environment  #10657
            } else if (attName.equals("technical_environment_/description") && hasContent) {
                       inPortAtts.add("technical_environment", content);

            //data_quality #10657
            } else if (attName.equals("data_quality_/representativeness") && hasContent) {
                                 dataQuality.append("Representativeness: " + content + "\n");
            } else if (attName.equals("data_quality_/accuracy") && hasContent) {
                                 dataQuality.append("Accuracy: " + content + "\n");

            //lineage /u00/data/points/InPortXML/NOAA/NMFS/AFSC/inport/xml/17218.xml
            } else if (attName.startsWith("lineage")) {
                if        (attName.equals("lineage_lineage_process_step")) {
                           lineageStepN = null; 
                           lineageName = null;
                           lineageEmail = null;
                           lineageDescription = null;
                } else if (attName.equals("lineage_lineage_process_step_/seq_no") && hasContent) {
                           lineageStepN = content;
                } else if (attName.equals("lineage_lineage_process_step_/contact_name") && hasContent) {
                           lineageName = content;
                } else if (attName.equals("lineage_lineage_process_step_/email_address") && hasContent) {
                           lineageEmail = content;
                } else if (attName.equals("lineage_lineage_process_step_/description") && hasContent) {
                           lineageDescription = content;
                } else if (attName.equals("lineage_/lineage_process_step") &&
                    (lineageName != null || lineageEmail != null || lineageDescription != null)) {
                           history.append("Lineage Step #" + 
                               (lineageStepN == null? "?" : lineageStepN) + 
                               (lineageName  == null? "" : ", " + lineageName) +
                               (lineageEmail == null? "" : " <" + lineageEmail + ">") +
                               (lineageDescription == null? "" : ": " + lineageDescription) +
                               "\n");
                }
                continue; //multiple lineage's, so don't write just one to datasets.xml

            //acronyms -- no files have it

            //glossary_terms -- no files have it

            //faqs /u00/data/points/InPortXML/NOAA/NMFS/SEFSC/inport/xml/7332.xml
            } else if (attName.startsWith("faqs")) {
                if        (attName.equals("faqs_faq")) {
                              faqs.append("FAQ #" + ++nFaqs);
                } else if (attName.equals("faqs_faq_/faq_date") && hasContent) {
                              faqs.append(": date=" + content);
                } else if (attName.equals("faqs_faq_/faq_author") && hasContent) {
                              faqs.append(", author=" + content + "\n");
                } else if (attName.equals("faqs_faq_/question") && hasContent) {
                    String2.addNewlineIfNone(faqs).append("Question: " + content + "\n");
                } else if (attName.equals("faqs_faq_/answer") && hasContent) {
                    String2.addNewlineIfNone(faqs).append("Answer: " + content + "\n");
                } else if (attName.equals("faqs_/faq")) {
                    String2.addNewlineIfNone(faqs).append("\n");
                }
                continue; //multiple faqs, so don't write just one to datasets.xml

            //downloads  #10657
            } else if (attName.startsWith("downloads")) {
                if        (attName.equals("downloads_download")) {
                         downloads.append("Download #" + ++nDownloads);
                } else if (attName.equals("downloads_download_/file_type") && hasContent) {
                         downloads.append(": file type=" + content + "\n");
                } else if (attName.equals("downloads_download_/description") && hasContent) {
                    String2.addNewlineIfNone(downloads).append("Description: " + content + "\n");
                } else if (attName.equals("downloads_download_/download_url") && hasContent) {
                    String2.addNewlineIfNone(downloads).append("URL: " + content + "\n");
                } else if (attName.equals("downloads_/download")) {
                    String2.addNewlineIfNone(downloads).append("\n");
                }
                continue; //multiple downloads, so don't write just one to datasets.xml

            //related_items  see 10657
            } else if (attName.startsWith("related_items")) {
                if        (attName.equals("related_items_item")) {
                      //catch e.g., <item cat-id="10661" cat-type="ENT" relation-type="HI">
                      String tTypeCode = xmlReader.attributeValue("cat-type");
                      String tType = typesHM.get(tTypeCode);                    
                      String tRelationCode = xmlReader.attributeValue("relation-type");
                      String tRelation = relationHM.get(tRelationCode);
                      relatedItems.append("Related Item #" + ++nRelatedItems + 
                          ": InPort catalog ID=" + xmlReader.attributeValue("cat-id") +
                          ", catalog type=" + (tType == null? tTypeCode : tType) + 
                          ", relation=" + (tRelation == null? tRelationCode : tRelation) + "\n");
                } else if (attName.equals("related_items_item_/title") && hasContent) {
                                          relatedItems.append("Title: " + content + "\n");
                } else if (attName.equals("related_items_item_/abstract") && hasContent) {
                                          relatedItems.append("Abstract: " + content + "\n");
                //} else if (attName.equals("related_items_item_relation-note")) {
                //    No, this is inport log info.
                //         downloads.append("Relation Note: " + content + "\n");
                } else if (attName.equals("related_items_/item")) {
                     relatedItems.append('\n');
                }
                continue; //multiple related_items, so don't write just one to datasets.xml

            //fgdc_contacts   See 10657
            } else if (attName.startsWith("fgdc_contacts")) {
                //multiple fgdc contacts
                //need to catch role and just keep if "Point of Contact"
                if (attName.equals("fgdc_contacts_fgdc_contact")) {
                    isFgdcPOC = "Point of Contact".equals(xmlReader.attributeValue("role"));
                }
                if (isFgdcPOC) {
                    if (attName.equals("fgdc_contacts_fgdc_contact_/contact_person_name") && hasContent) {
                        int po = content.indexOf(", ");
                        inPortAtts.add("creator_name", po > 0? 
                            content.substring(po + 2) + " " + content.substring(0, po) :
                            content); 
                    } else if (attName.equals("fgdc_contacts_fgdc_contact_/email") && hasContent) {
                        inPortAtts.add("creator_email", content);
                    }
                } else {
                   continue; //multiple fgdc_contacts. Just write Point of Contact to datasets.xml
                }

            //fgdc_time_period   see 10657     same as info above

            //publisher  see 10657
            } else if (attName.equals("publisher_person_contact_info_/name") && hasContent &&
                       inPortAtts.get("publisher_name") == null) {
                       int po = content.indexOf(", ");
                       inPortAtts.add("publisher_name", po > 0? 
                           content.substring(po + 2) + " " + content.substring(0, po) :
                           content); 
            } else if (attName.equals("publisher_person_contact_info_/email") && hasContent &&
                       inPortAtts.get("publisher_email") == null) {
                       inPortAtts.add("publisher_email", content);

            //child-entities   see 10657
            } else if (attName.startsWith("child_entities")) {
                if        (attName.equals("child_entities_child_entity")) {
                     childEntities.append("Child Entity #" + ++nChildEntities);
                } else if (attName.equals("child_entities_child_entity_common_information_/cat_id") && hasContent) {                       
                     childEntities.append(": InPort CatalogID=" + content);
                } else if (attName.equals("child_entities_child_entity_common_information_/cat_type_code") && hasContent) {
                     String tType = typesHM.get(content);                     
                     childEntities.append(", catalog type=" + (tType == null? content : tType) + "\n");
                } else if (attName.equals("child_entities_child_entity_common_information_/title") && hasContent) {
                    String2.addNewlineIfNone(childEntities).append("Title: " + content + "\n");
                } else if (attName.equals("child_entities_child_entity_common_information_/abstract") && hasContent) {
                    String2.addNewlineIfNone(childEntities).append("Abstract: " + content + "\n");
                } else if (attName.equals("child_entities_/child_entity")) {
                    String2.addNewlineIfNone(childEntities).append('\n');
                }
                continue; //multiple child_entities, so don't write just one to datasets.xml
            }

            //***** ??? do generic conversion for everything not explicitly 'continue'd above
            if (hasContent) {
                //inPortAtts.add(attNameNoSlash, content);
            }
        }

        //cleanup childEntitles
        if (childEntities.length() > 0)
            inPortAtts.add("child_entities", childEntities.toString());

        //cleanup creator info
        //String2.pressEnterToContinue(
        //    "creator_name=" + inPortAtts.get("creator_name") + ", " + creatorName2 + ", " + creatorName3 + "\n" +
        //    "creator_email=" + inPortAtts.get("creator_email") + ", " + creatorEmail2 + ", " + creatorEmail3 + "\n");
        if (inPortAtts.get("creator_name") == null) {
            if      (creatorName2 != null) 
                inPortAtts.set("creator_name", creatorName2);
            else if (creatorName3 != null) 
                inPortAtts.set("creator_name", creatorName3);
        }

        if (inPortAtts.get("creator_email") == null) {
            if      (creatorEmail2 != null) 
                inPortAtts.set("creator_email", creatorEmail2);
            else if (creatorEmail3 != null) 
                inPortAtts.set("creator_email", creatorEmail3);
        }

        if (inPortAtts.get("creator_url") == null &&
            !acronym.equals("???")) {
            String cu = 
                "AFSC".equals( acronym)? "http://www.afsc.noaa.gov/":
                "GARFO".equals(acronym)? "http://www.greateratlantic.fisheries.noaa.gov/":
                "NWFSC".equals(acronym)? "http://www.nwfsc.noaa.gov/":
                "OSF".equals(  acronym)? "http://www.nmfs.noaa.gov/sfa/":
                "OST".equals(  acronym)? "https://www.st.nmfs.noaa.gov/":
                "PIFSC".equals(acronym)? "http://www.pifsc.noaa.gov/":
                "SEFSC".equals(acronym)? "http://www.sefsc.noaa.gov/":
                "SERO".equals( acronym)? "http://sero.nmfs.noaa.gov/":
                "SWFSC".equals(acronym)? "https://swfsc.noaa.gov/": 
                "";
            if (!cu.equals(""))
                inPortAtts.add("creator_url", cu);
        }

        //cleanup dataQuality
        if (dataQuality.length() > 0)
            inPortAtts.add("data_quality", dataQuality.toString());

        //cleanup downloads
        if (downloads.length() > 0)
            inPortAtts.add("downloads", downloads.toString());

        //cleanup faqs
        if (faqs.length() > 0)
            inPortAtts.add("faqs", faqs.toString());

        //cleanup history (add to lineage info gathered above)
        if (!metaCreated.equals("???"))
            history.append(metaCreated +
                (metaCreatedBy.equals("???")? "" : " " + metaCreatedBy) +
                " originally created InPort metadata cat-id#" + catID + ".\n");
        if (!metaLastMod.equals("???"))
            history.append(metaLastMod +
                (metaCreatedBy.equals("???")? "" : " " + metaCreatedBy) +
                " last modified InPort metadata cat-id#" + catID + ".\n");
        if (history.length() == 0)
            history.append(
                (metaCreatedBy.equals("???")? "" : metaCreatedBy + " created ") +
                "InPort metadata cat-id#" + catID + ".\n");
        if (!dataPublicationDate.equals("???"))
            history.append(dataPublicationDate + " data set originally published.\n"); //???really???
        history.append( 
            Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10) +
            " Bob Simons <bob.simons@noaa.gov> at NOAA NMFS SWFSC ERD converted " +
            "InPort metadata from " + inportXmlUrl + " into ERDDAP dataset metadata.\n");
        inPortAtts.add("history", history.toString());

        //cleanup issues
        if (issues.length() > 0)
            inPortAtts.add("issues", issues.toString());

        //cleanup keywords
        inPortAtts.add("keywords", String2.toCSSVString(keywords));

        //cleanup license
        if (license.indexOf("Security class: Unclassified") >= 0 && //if Unclassified
            license.indexOf("Data access constraints: ") < 0 &&  //and no other info
            license.indexOf("Data access policy: ") < 0 &&
            license.indexOf("Data use constraints: ") < 0) 
            license.append("[standard]");
        else if (license.length() == 0)
            license.append("???");
        inPortAtts.add("license", license.toString());

        //cleanup relatedItems
        if (relatedItems.length() > 0)
            inPortAtts.add("related_items", relatedItems.toString());

        //cleanup urls
        if (urls.length() > 0)
            inPortAtts.add("urls", urls.toString());

        //*** makeReadyToUseGlobalAtts
        addAtts.set(makeReadyToUseAddGlobalAttributesForDatasetsXml(
            sourceAtts, "Point", //better cdm_data_type?
            "(local files)", //???
            inPortAtts, 
            suggestKeywords(sourceTable, addTable)));

        //use original title, with InPort # added
        if (!title.equals("???"))
            addAtts.add("title", title + " (InPort #" + catID + ")"); //catID ensures it is unique

        //fgdc and iso19115
        String fgdcFile     = String2.replaceAll(xmlFileName, "/inport/", "/fgdc/");
        String iso19115File = String2.replaceAll(xmlFileName, "/inport/", "/iso19115/");
        if (!File2.isFile(fgdcFile))
            fgdcFile     = ""; //if so, don't serve an fgdc file
        if (!File2.isFile(iso19115File))
            iso19115File = ""; //if so, don't serve an iso19115 file

        //write datasets.xml
        StringBuilder results = new StringBuilder();
        results.append(
            "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"" + 
                acronym.toLowerCase() + "IP" + catID + "\" active=\"true\">\n" +
            "    <sourceUrl>" + XML.encodeAsXML("tLocalSourceUrl???") + "</sourceUrl>\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +
            "    <accessibleViaFiles>false</accessibleViaFiles>\n" +
            "    <fgdcFile>" + fgdcFile + "</fgdcFile>\n" +
            "    <iso19115File>" + iso19115File + "</iso19115File>\n");
        results.append(writeAttsForDatasetsXml(false, sourceTable.globalAttributes(), "    "));
        results.append(writeAttsForDatasetsXml(true,  addTable.globalAttributes(),    "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        results.append(writeVariablesForDatasetsXml(sourceTable, addTable, 
            "dataVariable", true, true, false));
        results.append(
            "</dataset>\n" +
            "\n");        

        //background
        String2.log("\n-----");
        if (background.length() > 0)
            String2.log("Background for ERDDAP: " + background.toString());
        if (!"Unclassified".equals(securityClass))
            String2.log("WARNING! Security_class = " + securityClass);
        String2.log("generateDatasetsXml finished successfully.\n-----");
        return results.toString();
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedDatasetInfo) throws Throwable {
        testBasic(deleteCachedDatasetInfo);
        testGenerateDatasetsXml();
        testFixedValue();
        testBasic2();

        //not usually run
    }


}

