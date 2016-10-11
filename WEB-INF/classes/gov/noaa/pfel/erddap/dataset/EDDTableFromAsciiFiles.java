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
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import org.joda.time.DateTimeZone;

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
            "\nsampleFileName=" + sampleFileName +
            "\ncharset=" + charset + " colNamesRow=" + columnNamesRow + 
            " firstDataRow=" + firstDataRow +
            " reloadEveryNMinutes=" + tReloadEveryNMinutes +
            "\nextract pre=" + tPreExtractRegex + " post=" + tPostExtractRegex + 
            " regex=" + tExtractRegex + " colName=" + tColumnNameForExtract +
            "\nsortedColumn=" + tSortedColumnSourceName + 
            " sortFilesBy=" + tSortFilesBySourceNames + 
            "\ninfoUrl=" + tInfoUrl + 
            "\ninstitution=" + tInstitution +
            "\nsummary=" + tSummary +
            "\ntitle=" + tTitle +
            "\nexternalAddGlobalAttributes=" + externalAddGlobalAttributes);
        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        tColumnNameForExtract = String2.isSomething(tColumnNameForExtract)?
            tColumnNameForExtract.trim() : "";
        tSortedColumnSourceName = String2.isSomething(tSortedColumnSourceName)?
            tSortedColumnSourceName.trim() : "";
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
        DoubleArray mv9 = new DoubleArray(new double[]{
            -99, -999, -9999, -99999, -999999, -9999999,
             99,  999,  9999,  99999,  999999,  9999999});
        for (int col = 0; col < dataSourceTable.nColumns(); col++) {
            String colName = dataSourceTable.getColumnName(col);
            PrimitiveArray pa = (PrimitiveArray)dataSourceTable.getColumn(col).clone(); //clone because going into addTable

            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                null, //no source global attributes
                dataSourceTable.columnAttributes(col), colName, 
                true, true); //addColorBarMinMax, tryToFindLLAT

            //dateTime?
            boolean isDateTime = false;
            if (pa instanceof StringArray) {
                String dtFormat = Calendar2.suggestDateTimeFormat((StringArray)pa);
                if (dtFormat.length() > 0) { 
                    isDateTime = true;
                    addAtts.set("units", dtFormat);
                }
            }

            //look for missing_value = -99, -999, -9999, -99999, -999999, -9999999 
            //  even if StringArray
            double stats[] = pa.calculateStats();
            int whichMv9 = mv9.indexOf(stats[PrimitiveArray.STATS_MIN]);
            if (whichMv9 < 0)
                whichMv9 = mv9.indexOf(stats[PrimitiveArray.STATS_MAX]);
            if (whichMv9 >= 0) {
                addAtts.add("missing_value", 
                    PrimitiveArray.factory(pa.elementClass(), 1, 
                        "" + mv9.getInt(whichMv9)));
                String2.log("\nADDED missing_value=" + mv9.getInt(whichMv9) +
                    " to col=" + colName);
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

        //subsetVariables
        if (dataSourceTable.globalAttributes().getString("subsetVariables") == null &&
               dataAddTable.globalAttributes().getString("subsetVariables") == null) 
            dataAddTable.globalAttributes().add("subsetVariables",
                suggestSubsetVariables(dataSourceTable, dataAddTable, 1)); //nFiles

        //write the information
        StringBuilder sb = new StringBuilder();
        if (tSortFilesBySourceNames.length() == 0) {
            if (tColumnNameForExtract.length() > 0 &&
                tSortedColumnSourceName.length() > 0 &&
                !tColumnNameForExtract.equals(tSortedColumnSourceName))
                tSortFilesBySourceNames = tColumnNameForExtract + ", " + tSortedColumnSourceName;
            else if (tColumnNameForExtract.length() > 0)
                tSortFilesBySourceNames = tColumnNameForExtract;
            else 
                tSortFilesBySourceNames = tSortedColumnSourceName;
        }
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
"csvAscii,5.0,needs,49,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
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
"    Int16 actual_range 49, 78;\n" +
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
     * This generates a chunk of datasets.xml for one or more ERDDAP datasets
     * from the main (or for each of the children) in an inport.xml file.
     * This will not throw an exception.
     *
     * @return content for datasets.xml.
     *   Error messages will be included as comments.
     */
    public static String generateDatasetsXmlFromInPort(String xmlFileName, 
        String typeCodeRegex, String datasetTypeRegex, 
        String tFileDir) {

        String main = null;
        try {
            main = generateDatasetsXmlFromInPort(xmlFileName, 
                typeCodeRegex, datasetTypeRegex, tFileDir, 0, "") + "\n";
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            return "<!-- " + msg + " -->\n\n";
        }

        StringBuilder children = new StringBuilder();
        for (int whichChild = 1; whichChild < 10000; whichChild++) {
            try {
                children.append(generateDatasetsXmlFromInPort(xmlFileName, 
                    typeCodeRegex, datasetTypeRegex, tFileDir, whichChild, "") + "\n");
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t);
                if (msg.indexOf("ERROR: This is no childEntity=") >= 0)
                     msg = "";
                else msg = "<!-- " + msg + " -->\n\n";
                return whichChild > 1?
                    children.toString() + msg :
                    main                + msg;
            }
        }
        return children.toString();
    }

    /** 
     * This generates a chunk of datasets.xml for one ERDDAP dataset
     * from the info for the main info (or one of the children) in an inport.xml file.
     * InPort Users guide is at
     *   http://ias.pifsc.noaa.gov/inp/docs/inp_userguide_web.pdf.
     * Because the &lt;downloads&gt;&lt;download&gt;&lt;/download-url&gt;'s 
     *  are unreliable even when present, this just notes the URL in 
     *  &gt;sourceUrl&lt;, but doesn't attempt to download the file.
     *
     * @param xmlFileName the URL or full file name of an InPort XML file.
     *   If it's a URL, it will be stored in tFileDir.
     * @param typeCodeRegex e.g., .*, ENT, DS.
     *   If the &lt;common-information&gt;&lt;/cat-type-code&gt; value in the 
     *   xml file doesn't match, this will throw an exception.
     * @param datasetTypeRegex e.g., .*, Database, .*[Ff]ile.*.
     *   null appears as a "" (a 0-length String).
     *   If the &lt;data-set&gt; type attribute's value in the 
     *   xml file doesn't match, this will throw an exception.
     *   Observed by the Tally system: "": 583, Database: 118, Mixed: 53, 
     *     Other: 28, Files: 22, SAS files: 2, CSV Files: 1, GIS: 1.
     * @param tFileDir The directory that is/will be used to store the data file.
     *   If specified and if it doesn't exist, it will be created.
     * @param whichChild  
     *   If whichChild=0, this will create a dataVariables for columns
     *     described by the high-level data-attributes (if any).
     *   If whichChild is &gt;0, this method will include dataVariable definitions
     *      from the specified child (1, 2, 3, ...).
     *   If the specified whichChild doesn't exist, this will throw a RuntimeException.
     *   In all cases, the "sourceUrl" will specify the URL from where the data 
     *      can be downloaded (if specified in the XML).
     * @param tFileName The name.ext of the data file name, if known. 
     *   
     */
    public static String generateDatasetsXmlFromInPort(String xmlFileName, 
        String typeCodeRegex, String datasetTypeRegex, 
        String tFileDir, int whichChild, String tFileName) throws Throwable {

        String2.log("\n*** inPortGenerateDatasetsXml(" + xmlFileName + 
            ", whichChild=" + whichChild + ")");

        //check parameters
        Test.ensureTrue(whichChild >= 0, "whichChild must be >=0."); 

        //make tFileDir
        if (String2.isSomething(tFileDir)) 
             File2.makeDirectory(tFileDir);
        else tFileDir = "";

        //if xmlFileName is a URL, download it
        if (String2.isRemote(xmlFileName)) {
            if (tFileDir.equals(""))
                throw new RuntimeException(
                    "Since xmlFileName is a URL, you must specify tFileDir.");
            String destName = tFileDir + File2.getNameAndExtension(xmlFileName);
            SSR.downloadFile(xmlFileName, destName, true); //tryToUseCompression            
            String2.log("xmlFile saved as " + destName);
            xmlFileName = destName;
        }

        
        { //display what's in the .xml file
            String readXml[] = String2.readFromFile(xmlFileName, "UTF-8", 1);
            if (readXml[0].length() > 0)
                throw new RuntimeException(readXml[0]);
            if (whichChild == 0) {
                String2.log("Here's what is in the InPort .xml file:");
                String2.log(readXml[1]);
            }
        }

        int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        String catID = File2.getNameNoExtension(xmlFileName);
        if (!String2.isSomething(tFileDir))
            tFileDir = "???";
        if (!String2.isSomething(tFileName))
            tFileName = "???";

        //create tables to hold results
        Table sourceTable = new Table();
        Table addTable    = new Table();
        Attributes sourceAtts = sourceTable.globalAttributes();
        Attributes addAtts    = addTable.globalAttributes();
        boolean isFgdcPOC = false;
        boolean isSupportRoleOriginator = false;
        String creatorName2 = null, creatorName3 = null;
        String creatorEmail2 = null, creatorEmail3 = null;
        String metaCreatedBy = "???";
        String metaCreated = "???";
        String metaLastMod = "???";
        String dataPublicationDate = "???";
        String acronym = "???";  //institution acronym
        String tSourceUrl = "???";
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
        StringBuilder summary = new StringBuilder();
        int nUrls = 0;
        StringBuilder urls = new StringBuilder();

        HashMap<String,String> typesHM = new HashMap();
        typesHM.put("LIB", "Library");
        typesHM.put("PRJ", "Project");
        typesHM.put("DS",  "Data Set");
        typesHM.put("ENT", "Data Entity");
        typesHM.put("DOC", "Document");
        typesHM.put("PRC", "Procedure");
        
        HashMap<String,String> child0RelationHM = new HashMap(); //for whichChild=0
        child0RelationHM.put("HI", "child");
        child0RelationHM.put("RI", "other");

        HashMap<String,String> childNRelationHM = new HashMap(); //for whichChild>0
        childNRelationHM.put("HI", "sibling");
        childNRelationHM.put("RI", "other");

        //attributes that InPort doesn't help with
        String inportXmlUrl = "https://inport.nmfs.noaa.gov/inport-metadata/" +
            xmlFileName.substring("/u00/data/points/inportXml/".length()); 
        addAtts.add("cdm_data_type", "Other");
        addAtts.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
        addAtts.add("infoUrl", inportXmlUrl); 
        addAtts.add("InPort_XML_URL", inportXmlUrl);
        addAtts.add("keywords_vocabulary", "GCMD Science Keywords");
        addAtts.add("standard_name_vocabulary", "CF Standard Name Table v29");

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
            if (xmlReader.stackSize() == 1) 
                break; //the startTag
            String topTag = xmlReader.tag(nTags - 1);
            boolean hasContent = content.length() > 0 && 
                !content.equals("NA") && 
                !content.toLowerCase().equals("unknown") ;
            tags = tags.substring(startTagLength);
            if (debugMode) 
                String2.log(">>  tags=" + tags + (hasContent? "=" + content : ""));
            String attTags = 
                tags.startsWith("<data-attributes><attribute>")? 
                    tags.substring(28) :
                tags.startsWith("<child-entities><child-entity><data-attributes><attribute>")?
                    tags.substring(58) :
                null;
 
            //special cases: convert some InPort names to ERDDAP names
            //The order here matches the order in the files.

            //common_information
            if (tags.equals(       "<common-information></cat-id>")) {                
                Test.ensureEqual(content, catID, "cat-id != fileName");
                addAtts.add("InPort_catalog_ID", content);
            } else if (tags.equals("<common-information></cat-type-code>")) {                
                //Tally: ENT: 2378 (60%), DS: 1575  (40%)
                if (!content.matches(typeCodeRegex))
                    throw new RuntimeException("cat-type-code=" + content + 
                        " doesn't match typeCodeRegex=" + typeCodeRegex);
                String tType = typesHM.get(content);                      
                addAtts.add("InPort_catalog_type", tType == null? content : tType);
            } else if (tags.equals("<common-information></metadata-workflow-state>") && hasContent) {
                //Tally: Published / External: 3953  (100%)
                addAtts.add("metadata_workflow_state", content); 
            } else if (tags.equals("<common-information></is-do-not-publish>")) {                
                //Tally: N: 3953 (100%) (probably because I harvested Public records)
                Test.ensureEqual(content, "N", "Unexpected id-do-not-publish content.");
            } else if (tags.equals("<common-information></title>") && hasContent) {
                title = content;
            } else if (tags.equals("<common-information></abstract>") && hasContent) {
                String2.ifSomethingConcat(summary, "\n\n", content); 
            } else if (tags.equals("<common-information></purpose>") && hasContent) {
                String2.ifSomethingConcat(summary, "\n\n", content); 
            } else if (tags.equals("<common-information></notes>") && hasContent) {
                String2.ifSomethingConcat(summary, "\n\n", content); 
            } else if (tags.equals("<common-information></supplemental-info>") && hasContent) {
                String2.ifSomethingConcat(summary, "\n\n", content); 
            } else if (tags.equals("<common-information></data-status>") && hasContent) {
                //Tally: Complete: 2341  (62%), In Work: 1363  (36%), Planned: 56  (1%)
                addAtts.add("data_status", content); 
            //<common-information><notes has info re InPort metadata creation
            } else if (tags.equals("<common-information></created-by>") && hasContent) {
                metaCreatedBy = content;
            } else if (tags.equals("<common-information></record-created>") && hasContent) {
                metaCreated = content;
            } else if (tags.equals("<common-information></record-last-modified>") && hasContent) {
                metaLastMod = content;
            } else if (tags.equals("<common-information></owner-org-acro>") && hasContent) {
                //Tally: AFSC: 1240 (31%), NEFSC: 832 (21%), SEFSC: 618 (16%), NWFSC: 316 (8%), PIFSC: 276  (7%)
                //  OPR, SWFSC, GARFO, PIRO, OST, AKRO, SERO, OHC, WCRO, GSMFC, OSF
                addAtts.add("institution", 
                    xmlFileName.indexOf("/NOAA/NMFS/") > 0? "NOAA NMFS " + content: //e.g., SWFSC
                    xmlFileName.indexOf("/NOAA/")      > 0? "NOAA " + content: 
                    content); 
                acronym = content;

            } else if (tags.equals("<common-information></data-set-publication-date>") && hasContent) {
                addAtts.add("date-created", content); 
                dataPublicationDate = content;
            } else if (tags.equals("<common-information></parent-title>") && hasContent) {
                //parent-title is often better than title
                title = content + (title.equals("???")? "" : ", " + title);
            } else if (tags.equals("<common-information></notes>") && hasContent) {
                addAtts.add("comment", content); 
            } else if (tags.equals("<common-information></publication-status>") && hasContent) {
                //Tally:  Public: 3953 (100%)  (probably because I harvest Public records)
                addAtts.add("publication_status", content); 

            //physical-location  org, city, state-province, country  see 1132
            //} else if (tags.startsWith("physical-location")) {

            //data-set   see /u00/data/points/inportXml/NOAA/NMFS/AFSC/inport/xml/10657.xml
            //This info is relevant to the source, not after it's in ERDDAP.
            //So, don't add to ERDDAP meatadata. 
            } else if (tags.equals("<data-set>") && xmlReader.attributeValue("type") != null) {
                //Tally:  : 749 (48%), Database: 177 (11%), Oracle Database: 102 (6%). 
                //  CSV Files: 77 (5%), Mixed: 75 (5%), MS Excel Spreadsheet: 75 (5%),
                //  Files: 70 (4%), Other: 46 (3%), MS Access Database: 23  (1%),
                //  SAS files: 20 (1%), Files (Word, Excel, PDF, etc.): 19  (1%), 
                //  GIS: 19 (1%), Access Database, spreadsheets: 18 (1%), Binary: 18 (1%), 
                //  MS Excel : 11 (1%), Excel and SAS Dataset: 10 (1%), Website (url): 8 (1%), ...
                String dsType = xmlReader.attributeValue("type");
                if (dsType == null)
                    dsType = "";
                if (!dsType.matches(datasetTypeRegex))
                    throw new RuntimeException("<data-set> type=" + dsType + 
                        " doesn't match datasetTypeRegex=" + datasetTypeRegex);
                  background.append("data-set type=" + dsType + "\n");
            } else if (tags.equals("<data-set></maintenance-frequency>") && hasContent) {
                 background.append(" data-set maintenance-frequency=" + content + "\n");
            } else if (tags.equals("<data-set></source-media-type>") && hasContent) {
                //Tally:  online: 96 (26%), disc: 95 (25%), electronic email system: 56 (15%),
                //  electronically logged: 46 (12%), computer program: 37 (10%), paper: 28 (7%),
                //  CD-ROM: 10 (3%), physical model: 3 (1%), chart: 2 (1%), videotape: 2 (1%),
                //  audiocassette: 1 (0%)
                 background.append(" data-set source-media=" + content + "\n");
            } else if (tags.equals("<data-set></entity-attribute-overview>") && hasContent) {
                //Tally: Modeled in Oracle Designer: 4 (2%), Not available.: 4 (2%), ...,
                //       Modelled in Oracle Designer: 2  (1%)
                // 1 MS-Excel spreadsheet with 4 worksheets: 1  (0%)
                // 1 MS-Excel spreadsheet with 8 worksheets: 1  (0%)
                // 1 MS-Excel spreadsheet, with 2 worksheets: 1  (0%)
                 background.append(" data-set overview=" + content + "\n");
            } else if (tags.equals("<data-set></distribution-liability>") && hasContent) {
                                license.append("Distribution Liability: " + content + "\n"); 
            } else if (tags.equals("<data-set></data-set-credit>") && hasContent) {
                addAtts.add("acknowledgment", content); 

            //keywords see 10657
            } else if ((tags.equals("<keywords><theme-keywords></keyword-list>") ||
                        tags.equals("<keywords><spatial-keywords></keyword-list>") ||
                        tags.equals("<keywords><stratum-keywords></keyword-list>") ||
                        tags.equals("<keywords><temporal-keywords></keyword-list>")) && hasContent) {
                chopUpCsvAddAllAndParts(content, keywords);

            //<data-attributes><attribute>   see attTags below

            //support-roles  e.g., see 10657
            //Use role=Originator as backup for creator_name, creator_email
            } else if (tags.startsWith("<support-roles>")) {
                if (tags.equals(       "<support-roles><support-role>")) {
                    isSupportRoleOriginator = false;
                } else if (tags.equals("<support-roles><support-role></role>") && hasContent) {
                    isSupportRoleOriginator = "Originator".equals(content);
                } else if (tags.equals("<support-roles><support-role><person-contact-info></name>") && hasContent) {
                    int po = content.indexOf(", ");
                    creatorName3 = po > 0?    //any role
                        content.substring(po + 2) + " " + content.substring(0, po) :
                        content; 
                    if (isSupportRoleOriginator)
                        creatorName2 = creatorName3;
                        
                } else if (tags.equals("<support-roles><support-role><person-contact-info></email>") && hasContent) {
                    creatorEmail3 = content;  //any role
                    if (isSupportRoleOriginator)
                        creatorEmail2 = creatorEmail3;
                }

            //geog-area
            } else if (tags.equals("<geog-area></description>") ||  //older xml
                       tags.equals("<extents><extent><geog-areas><geog-area></description>")) { //newer xml
                 if (hasContent) 
                        addAtts.add("geospatial_description", content); 
            } else if (tags.equals("<geog-area></west-bound>") ||
                       tags.equals("<extents><extent><geog-areas><geog-area></west-bound>")) {
                 if (hasContent) 
                        addAtts.add("geospatial_lon_min", String2.parseDouble(content)); 
            } else if (tags.equals("<geog-area></east-bound>") ||
                       tags.equals("<extents><extent><geog-areas><geog-area></east-bound>")) {
                 if (hasContent) 
                        addAtts.add("geospatial_lon_max", String2.parseDouble(content)); 
            } else if (tags.equals("<geog-area></south-bound>") ||
                       tags.equals("<extents><extent><geog-areas><geog-area></south-bound>")) {
                 if (hasContent) 
                        addAtts.add("geospatial_lat_min", String2.parseDouble(content)); 
            } else if (tags.equals("<geog-area></north-bound>") ||
                       tags.equals("<extents><extent><geog-areas><geog-area></north-bound>")) {
                 if (hasContent) 
                        addAtts.add("geospatial_lat_max", String2.parseDouble(content)); 

            //time-frame
            } else if (tags.equals("<time-frames><time-frame></start>") ||
                       tags.equals("<extents><extent><time-frames><time-frame></start>")) {
                 if (hasContent) 
                        addAtts.add("time_coverage_start", content); 
            } else if (tags.equals("<time-frames><time-frame></end>") ||
                       tags.equals("<extents><extent><time-frames><time-frame></end>")) {
                 if (hasContent) 
                        addAtts.add("time_coverage_end", content); 

            //access-info    see AFSC/inport/xml/10657.xml
            } else if (tags.equals("<access-info></security-class") && hasContent) {
                license.append("Security class: " + content + "\n"); 
                addAtts.add("security_class", content);
                securityClass = content;
            } else if (tags.equals("<access-info></security-classification-system>") && hasContent) {
                                   license.append("Security classification system: " + content + "\n"); 
            } else if (tags.equals("<access-info></data-access-policy>") && hasContent) {
                                   license.append("Data access policy: " + content + "\n"); 
            } else if (tags.equals("<access-info></data-access-constraints>") && hasContent) {
                                   license.append("Data access constraints: " + content + "\n"); 
            } else if (tags.equals("<access-info></data-access-procedure>") && hasContent) {
                                   license.append("Data access procedure: " + content + "\n"); 
            } else if (tags.equals("<access-info></data-use-constraints>") && hasContent) {
                                   license.append("Data use constraints: " + content + "\n"); 
            } else if (tags.equals("<access-info></metadata-access-constraints>") && hasContent) {
                                   license.append("Metadata access constraints: " + content + "\n"); 
            } else if (tags.equals("<access-info></metadata-use-constraints>") && hasContent) {
                                   license.append("Metadata use constraints: " + content + "\n"); 

            //urls   see 10657
            } else if (tags.startsWith("<urls>")) {
                if        (tags.equals("<urls><url>")) {
                            urls.append("URL #" + ++nUrls);
                } else if (tags.equals("<urls><url></link>") && hasContent) {
                    urls.append(": " + content + "\n");
                } else if (tags.equals("<urls><url></url-type>") && hasContent) {
                    String2.addNewlineIfNone(urls).append("Type: " + content + "\n");
                } else if (tags.equals("<urls><url></description>") && hasContent) {
                    String2.addNewlineIfNone(urls).append("Description: " + content + "\n");
                } else if (tags.equals("<urls></url>")) {
                    String2.addNewlineIfNone(urls).append("\n");
                }

            //activity-log -- just InPort activity

            //issues see /u00/data/points/inportXml/NOAA/NMFS/PIFSC/inport/xml/18143.xml
            } else if (tags.startsWith("<issues><")) {
                if        (tags.equals("<issues><issue>")) {
                          issues.append("Issue #" + ++nIssues);
                } else if (tags.equals("<issues><issue></date>") && hasContent) {
                                        issues.append(": date=" + content);
                } else if (tags.equals("<issues><issue></author>") && hasContent) {
                                        issues.append(", author=" + content + "\n");
                } else if (tags.equals("<issues><issue></summary>") && hasContent) {
                    String2.addNewlineIfNone(issues).append("Summary: " + content + "\n");
                } else if (tags.equals("<issues></issue>")) {
                    String2.addNewlineIfNone(issues).append("\n");
                }

            //technical-environment  #10657
            } else if (tags.equals("<technical-environment></description>") && hasContent) {
                        addAtts.add("technical_environment", content);

            //data-quality #10657
            } else if (tags.equals("<data-quality></representativeness>") && hasContent) {
                                dataQuality.append("Representativeness: " + content + "\n");
            } else if (tags.equals("<data-quality></accuracy>") && hasContent) {
                                dataQuality.append("Accuracy: " + content + "\n");

            //lineage /u00/data/points/inportXml/NOAA/NMFS/AFSC/inport/xml/17218.xml
            } else if (tags.startsWith("<lineage><")) {
                if        (tags.equals("<lineage><lineage-process-step>")) {
                    lineageStepN = null; 
                    lineageName = null;
                    lineageEmail = null;
                    lineageDescription = null;
                } else if (tags.equals("<lineage><lineage-process-step></seq-no>") && hasContent) {
                    lineageStepN = content;
                } else if (tags.equals("<lineage><lineage-process-step></contact-name>") && hasContent) {
                    lineageName = content;
                } else if (tags.equals("<lineage><lineage-process-step></email-address>") && hasContent) {
                    lineageEmail = content;
                } else if (tags.equals("<lineage><lineage-process-step></description>") && hasContent) {
                    lineageDescription = content;
                } else if (tags.equals("<lineage></lineage-process-step>") &&
                    (lineageName != null || lineageEmail != null || lineageDescription != null)) {
                    history.append("Lineage Step #" + 
                        (lineageStepN == null? "?" : lineageStepN) + 
                        (lineageName  == null? "" : ", " + lineageName) +
                        (lineageEmail == null? "" : " <" + lineageEmail + ">") +
                        (lineageDescription == null? "" : ": " + lineageDescription) +
                        "\n");
                }

            //acronyms -- no files have it

            //glossary-terms -- no files have it

            //faqs /u00/data/points/inportXml/NOAA/NMFS/SEFSC/inport/xml/7332.xml
            } else if (tags.startsWith("<faqs>")) {
                if        (tags.equals("<faqs><faq>")) {
                            faqs.append("FAQ #" + ++nFaqs);
                } else if (tags.equals("<faqs><faq></faq-date>") && hasContent) {
                                          faqs.append(": date=" + content);
                } else if (tags.equals("<faqs><faq></faq-author>") && hasContent) {
                                          faqs.append(", author=" + content + "\n");
                } else if (tags.equals("<faqs><faq></question>") && hasContent) {
                    String2.addNewlineIfNone(faqs).append("Question: " + content + "\n");
                } else if (tags.equals("<faqs><faq></answer>") && hasContent) {
                    String2.addNewlineIfNone(faqs).append("Answer: " + content + "\n");
                } else if (tags.equals("<faqs></faq>")) {
                    String2.addNewlineIfNone(faqs).append("\n");
                }

            //downloads  #10657
            } else if (tags.startsWith("<downloads>")) {
                if        (tags.equals("<downloads><download>")) {
                       downloads.append("Download #" + ++nDownloads);
                } else if (tags.equals("<downloads><download></file-type>") && hasContent) {
                                           downloads.append(": file type=" + content + "\n");
                } else if (tags.equals("<downloads><download></description>") && hasContent) {
                   String2.addNewlineIfNone(downloads).append("Description: " + content + "\n");

                // *** DOWNLOAD URL!
                } else if (tags.equals("<downloads><download></download-url>") && hasContent) {
                    String2.addNewlineIfNone(downloads).append("URL: " + content + "\n");
                    if (tSourceUrl.equals("???"))
                        tSourceUrl = content;
                } else if (tags.equals("<downloads></download>")) {
                    String2.addNewlineIfNone(downloads).append("\n");
                }

            //related-items  see 10657
            } else if (tags.startsWith("<related-items>")) {
                if        (tags.equals("<related-items><item>")) {
                      //catch e.g., <item cat-id="10661" cat-type="ENT" relation-type="HI">
                      String tTypeCode = xmlReader.attributeValue("cat-type");
                      String tType = typesHM.get(tTypeCode);                    
                      String tRelationCode = xmlReader.attributeValue("relation-type");
                      String tRelation = whichChild == 0?
                          child0RelationHM.get(tRelationCode) :
                          childNRelationHM.get(tRelationCode);
                      if (tRelation == null)
                          tRelation = tRelationCode; //may be null
                      relatedItems.append("Related Item #" + ++nRelatedItems + 
                          ": InPort catalog ID=" + xmlReader.attributeValue("cat-id") +
                          ", catalog type=" + (tType == null? tTypeCode : tType) + 
                          ", relation=" + tRelation + "\n");
                } else if (tags.equals("<related-items><item></title>") && hasContent) {
                                          relatedItems.append("Title: " + content + "\n");
                } else if (tags.equals("<related-items><item></abstract>") && hasContent) {
                                          relatedItems.append("Abstract: " + content + "\n");
                //} else if (tags.equals("<related-items><item><relation-note>")) {
                //    No, this is inport log info.
                //         downloads.append("Relation Note: " + content + "\n");
                } else if (tags.equals("<related-items></item>")) {
                     relatedItems.append('\n');
                }

            //fgdc-contacts   See 10657
            } else if (tags.startsWith("<fgdc-contacts>")) {
                //multiple fgdc contacts
                //need to catch role and just keep if "Point of Contact"
                if (tags.equals("<fgdc-contacts><fgdc-contact>")) {
                    isFgdcPOC = "Point of Contact".equals(xmlReader.attributeValue("role"));
                }
                if (isFgdcPOC) {
                    if (tags.equals("<fgdc-contacts><fgdc-contact></contact-person-name>") && hasContent) {
                        int po = content.indexOf(", ");
                        addAtts.add("creator_name", po > 0? 
                            content.substring(po + 2) + " " + content.substring(0, po) :
                            content); 
                    } else if (tags.equals("<fgdc-contacts><fgdc-contact></email>") && hasContent) {
                        addAtts.add("creator_email", content);
                    }
                }

            //fgdc-time-period   see 10657     same as info above

            //publisher  see 10657
            } else if (tags.equals("<publisher><person-contact-info></name>") && hasContent &&
                addAtts.get("publisher_name") == null) {
                int po = content.indexOf(", ");
                addAtts.add("publisher_name", po > 0? 
                    content.substring(po + 2) + " " + content.substring(0, po) :
                    content); 
            } else if (tags.equals("<publisher><person-contact-info></email>") && hasContent &&
                addAtts.get("publisher_email") == null) {
                addAtts.add("publisher_email", content);

            //*** attTags - This section processes <data-attributes><attribute>
            //  information for high level <data-attributes> if whichChild = 0 
            //  or a child if whichChild = this child
            //Other code uses whichChild to determine if this info should be 
            //  processed or skipped.
            } else if (attTags != null) {
                int col = addTable.nColumns() - 1;
                Attributes atts = col >= 0? addTable.columnAttributes(col) : null;
                if (attTags.equals("")) {
                    String tName = "column" + (col + 1);
                    sourceTable.addColumn(tName, new StringArray());
                    addTable.addColumn(   tName, new StringArray());
                    if (reallyVerbose)
                        String2.log("Creating column#" + (col+1) + " for tags=" + tags);

                } else if (attTags.equals("</name>") && hasContent) {
                    sourceTable.setColumnName(col, content);                    
                    content = String2.modifyToBeVariableNameSafe(content);
                    if (content.matches("[A-Z0-9_]+"))  //all uppercase
                        content = content.toLowerCase();
                    addTable.setColumnName(col, content);                        

                } else if (attTags.equals("</data-storage-type>") && hasContent) {
                    //vs general-data-type?
                    //I did tally to see all the options -- clearly not a defined vocabulary
                    PrimitiveArray pa = null;
                    String lcContent = content.toLowerCase();
                    if        (String2.indexOf(new String[]{
                        "bit", "boolean", "byte", "logical", 
                        "tinyint", "yes/no"}, lcContent) >= 0) {
                        pa = new ByteArray();
                    } else if (String2.indexOf(new String[]{
                        "smallint"}, lcContent) >= 0) {
                        pa = new ShortArray();
                    } else if (String2.indexOf(new String[]{
                        "autonumber", "autonumber4", "best32", "guid", 
                        "integer", "int", "oid", 
                        "raster", "year"}, lcContent) >= 0) {
                        pa = new IntArray();
                    //long -> double below
                    } else if (String2.indexOf(new String[]{
                        "float"}, lcContent) >= 0) {
                        pa = new FloatArray();
                    } else if (String2.indexOf(new String[]{
                        "currency", "binary_double", "decimal", "double", 
                        "long integer", "long raw",
                        "num", "numeric", "number", "number(2)", "numger", 
                        ""}, lcContent) >= 0) {
                        pa = new DoubleArray();
                    } //otherwise, leave as String    
                    if (pa != null) {
                        sourceTable.setColumn(col, pa);
                        addTable.setColumn(   col, pa);
                    }

                } else if (attTags.equals("</null-repr>") && hasContent) {
                    //from Tally: YES, NO, Yes, No, N, NA, "Y, N, NULL"   How bizarre!
                    PrimitiveArray pa = addTable.getColumn(col);
                    double d = String2.parseDouble(content);
                    if (pa instanceof ByteArray)
                        atts.set("missing_value", Math2.roundToByte(d));
                    else if (pa instanceof ShortArray)
                        atts.set("missing_value", Math2.roundToShort(d));
                    else if (pa instanceof IntArray)
                        atts.set("missing_value", Math2.roundToInt(d));
                    else if (pa instanceof LongArray)
                        atts.set("missing_value", String2.parseLong(content));
                    else if (pa instanceof FloatArray)
                        atts.set("missing_value", Math2.doubleToFloatNaN(d));
                    else if (pa instanceof DoubleArray)
                        atts.set("missing_value", d);
                    else if (pa instanceof StringArray) {
                        if      (content.indexOf("NA")   >= 0) atts.set("missing_value", "NA");
                        else if (content.indexOf("NULL") >= 0) atts.set("missing_value", "NULL");
                        else if (content.indexOf("null") >= 0) atts.set("missing_value", "null");
                    }

                } else if (attTags.equals("</scale>") && hasContent) { 
                    //What is this? It isn't scale_factor. 
                    //from Tally: 0, 2, 1, 3, 5, 6, 9, 14, 8, 13, 12, 15, 9, ...
                    atts.set("scale", content);

                } else if (attTags.equals("</max-length>") && hasContent) {
                    //e.g., -1 (?!), 0(?!), 22, 1, 8, 100, 4000 (longest)
                    int maxLen = String2.parseInt(content);
                    if (maxLen > 0 && maxLen < 10000)
                        atts.add("max_length", "" + maxLen);

                } else if (attTags.equals("</is-pkey>") && hasContent) {
                    atts.set("isPrimaryKey", content);

                } else if (attTags.equals("</units>") && hasContent) {
                    //e.g., Decimal degrees, animal, KM, degrees celcius(sic), AlphaNumeric
                    //a few common fixups:
                    String contentLC = content.toLowerCase();
                    content = //if same, it is to change to correct case
                        contentLC.equals("<![cdata[mm<sup>2</sup>]]>")? "mm^2" :
                        contentLC.equals("%")? "percent" :
                        contentLC.equals("alphanumeric")? "" :
                        contentLC.equals("animal")?  "count" :
                        contentLC.equals("c")? "degrees_C" :
                        contentLC.equals("centimeters")? "centimeters" : 
                        contentLC.equals("crabs")?   "count" :
                        contentLC.equals("crabs per square meter")?   "count/m^2" :
                        contentLC.equals("celsius")? "degrees_C" :
                        contentLC.equals("date")? "" :
                        contentLC.equals("decimal degrees")? "degrees" :
                        contentLC.equals("decmal degrees")?  "degrees" :
                        contentLC.equals("degree celsius days")? "degrees_C days" :
                        contentLC.equals("degrees celsius")? "degrees_C" :
                        contentLC.equals("degree relative to true n")? "degrees_true" :
                        contentLC.equals("g")? "grams" :
                        contentLC.equals("holding cell number")? "1" :
                        contentLC.equals("integer")? "count" :
                        contentLC.equals("microgram/kilogram")? "microgram/kilogram" :
                        contentLC.equals("milimeters")? "millimeters" :
                        contentLC.equals("time")? "" :
                        contentLC.equals("volts")? "volts" :
                        contentLC.equals("1")? "" :
                        contentLC.equals("4")? "" :
                        content;
                    if (String2.isSomething(content))
                        atts.set("units", content);

                } else if (attTags.equals("</format-mask>") && hasContent) {
                    //Thankfully, format-mask appears after units, so format-mask has precedence.
                    //e.g., $999,999.99, MM/DD/YYYY, HH:MM:SS, mm/dd/yyyy, HHMM

                    //if it's a date time format, convertToJavaDateTimeFormat e.g., yyyy-MM-dd'T'HH:mm:ssZ
                    String newContent = Calendar2.convertToJavaDateTimeFormat(content);
                    if (!newContent.equals(content) ||
                        newContent.indexOf("yy") >= 0) {

                        atts.set("units", newContent);

                        if (newContent.indexOf("y") >= 0 && //has years
                            newContent.indexOf("M") >= 0 && //has month
                            newContent.indexOf("d") >= 0 && //has days
                            newContent.indexOf("H") <  0)   //doesn't have hours
                            atts.set("time_precision", "1970-01-01");
                    } else {
                        atts.set("format_mask", content);
                    }

                } else if (attTags.equals("</description>") && hasContent) {
                    //description -> comment
                    //widely used  (Is <description> used another way?)
                    if (content.toLowerCase().equals("month/day/year")) {  //date format
                         atts.set("units", "M/d/yy");
                         atts.set("time_precision", "1970-01-01");
                    } else {
                        atts.set("comment", content);
                    }

                } else if (attTags.equals("</null-meaning>") && hasContent) {
                    //e.g., 1=Yes, The fish length is unknown or was not recorded.
                    atts.set("null_meaning", content);

                } else if (attTags.equals("</allowed-values>") && hasContent) {
                    //e.g., No domain defined., unknown, Free entry text field., text, "1, 2, 3", "False, True"
                    atts.set("allowed_values", content);

                } else if (attTags.equals("</derivation>") && hasContent) {
                    //no examples
                    atts.set("derivation", content);

                } else if (attTags.equals("</validation-rules>") && hasContent) {
                    atts.set("validation_rules",  content);

                } else if (attTags.equals("</version>") && hasContent) {
                    //no examples
                    atts.set("version", content);

                }


            //data-attributes   see AFSC 17336
            //This MUST BE AFTER processing of attTags, otherwise they won't be seen
            } else if (tags.equals("<data-attributes>")) {
                if (whichChild != 0) {
                    //skip this info
                    if (reallyVerbose)
                        String2.log("Since whichChild=" + whichChild + ", I'm skipping the outer <data-attributes>.");
                    xmlReader.skipToStackSize(xmlReader.stackSize());
                } else {
                    if (reallyVerbose)
                        String2.log("Since whichChild=" + whichChild + ", I'm processing the outer <data-attributes>.");
                }

            //child-entities   see AFSC 17336
            //This MUST BE AFTER processing of attTags, otherwise they won't be seen
            } else if (tags.startsWith("<child-entities>")) {
                if        (tags.equals("<child-entities><child-entity>")) {
                    //the start of a child-entity
                    ++nChildEntities;
                    if (whichChild != nChildEntities) {
                        xmlReader.skipToStackSize(xmlReader.stackSize());
                        if (reallyVerbose)
                            String2.log("Since whichChild=" + whichChild + ", I'm skipping child#" + nChildEntities + ".");
                    } else {
                        childEntities.append("Child Entity #" + nChildEntities);
                        if (reallyVerbose)
                            String2.log("Since whichChild=" + whichChild + ", I'm processing child#" + nChildEntities + ".");
                    }

                //these are only found if we're processing this child
                //most of these overwrite the info from main <common-information>
                } else if (tags.equals("<child-entities><child-entity><common-information></cat-id>") && hasContent) {                       
                    addAtts.set("InPort_catalog_ID", 
                        catID = addAtts.getString("InPort_catalog_ID") + "_" + content);
                } else if (tags.equals("<child-entities><child-entity><common-information></cat-type-code>") && hasContent) {                       
                    String tType = typesHM.get(content);                      
                    addAtts.add("InPort_catalog_type", tType == null? content : tType);
                } else if (tags.equals("<child-entities><child-entity><common-information></metadata-workflow-state>") && hasContent) {
                    addAtts.add("metadata_workflow_state", content); 
                } else if (tags.equals("<child-entities><child-entity><common-information></name>") && hasContent &&
                    "???".equals(tFileName)) {
                    tFileName = content;
                } else if (tags.equals("<child-entities><child-entity><common-information></title>") && hasContent) {
                    title = String2.ifSomethingConcat(title, " - ", content);
                } else if (tags.equals("<child-entities><child-entity><common-information></abstract>") && hasContent) {
                    addAtts.set("summary", String2.ifSomethingConcat(
                        addAtts.getString("summary"), "\n\n", content));
                } else if (tags.equals("<child-entities><child-entity><common-information></data-status>") && hasContent) {
                    addAtts.add("data_status", content); 
                } else if (tags.equals("<child-entities><child-entity><common-information></created-by>") && hasContent) {
                    metaCreatedBy = content;
                } else if (tags.equals("<child-entities><child-entity><common-information></record-created>") && hasContent) {
                    metaCreated = content;
                } else if (tags.equals("<child-entities><child-entity><common-information></record-last-modified>") && hasContent) {
                    metaLastMod = content;
                } else if (tags.equals("<child-entities><child-entity><common-information></publication-status>") && hasContent) {
                    addAtts.add("publication_status", content); 
                }

            } else {
                //log things not handled?
                //if (hasContent) 
                    //String2.log(" not handled: " + tags + " = " content);
            }
        }

        //desired whichChild not found?
        if (whichChild > nChildEntities)         
            throw new RuntimeException("ERROR: This is no childEntity=" + whichChild);

        //cleanup childEntities
        //DON'T include: either we're processing 1 child entity, or info is already in 
        //  "related_items"
        //if (childEntities.length() > 0)
        //    addAtts.add("child_entities", childEntities.toString());

        //cleanup creator info
        //String2.pressEnterToContinue(
        //    "creator_name=" + addAtts.get("creator_name") + ", " + creatorName2 + ", " + creatorName3 + "\n" +
        //    "creator_email=" + addAtts.get("creator_email") + ", " + creatorEmail2 + ", " + creatorEmail3 + "\n");
        if (addAtts.get("creator_name") == null) {
            if      (creatorName2 != null) 
                addAtts.set("creator_name", creatorName2);
            else if (creatorName3 != null) 
                addAtts.set("creator_name", creatorName3);
        }

        if (addAtts.get("creator_email") == null) {
            if      (creatorEmail2 != null) 
                addAtts.set("creator_email", creatorEmail2);
            else if (creatorEmail3 != null) 
                addAtts.set("creator_email", creatorEmail3);
        }

        if (addAtts.get("creator_url") == null &&
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
                addAtts.add("creator_url", cu);
        }

        //cleanup dataQuality
        if (dataQuality.length() > 0)
            addAtts.add("data_quality", dataQuality.toString().trim());

        //cleanup downloads
        if (downloads.length() > 0)
            addAtts.add("downloads", downloads.toString().trim());

        //cleanup faqs
        if (faqs.length() > 0)
            addAtts.add("faqs", faqs.toString().trim());

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
            " ERDDAP's GenerateDatasetsXml (contact: bob.simons@noaa.gov) converted " +
            "InPort metadata from " + inportXmlUrl + " into an ERDDAP dataset description.\n");
        addAtts.add("history", history.toString().trim());

        //cleanup issues
        if (issues.length() > 0)
            addAtts.add("issues", issues.toString().trim());

        //cleanup keywords
        addAtts.add("keywords", String2.toCSSVString(keywords));

        //cleanup license
        if (license.indexOf("Security class: Unclassified") >= 0 && //if Unclassified
            license.indexOf("Data access constraints: ") < 0 &&  //and no other info
            license.indexOf("Data access policy: ") < 0 &&
            license.indexOf("Data use constraints: ") < 0) 
            license.append("[standard]");
        else if (license.length() == 0)
            license.append("???");
        addAtts.add("license", license.toString().trim());

        if (relatedItems.length() > 0)
            addAtts.add("related_items", relatedItems.toString().trim());

        addAtts.add("summary", summary.length() == 0? title : summary.toString().trim());

        if (urls.length() > 0)
            addAtts.add("urls", urls.toString().trim());

        //*** makeReadyToUseGlobalAtts
        addAtts.set(makeReadyToUseAddGlobalAttributesForDatasetsXml(
            sourceAtts, "Other", //cdm_data_type. Can't know if Point/etc correct without info about variables.
            "(local files)", //???
            addAtts, 
            suggestKeywords(sourceTable, addTable)));

        //subsetVariables
        if (sourceTable.globalAttributes().getString("subsetVariables") == null &&
               addTable.globalAttributes().getString("subsetVariables") == null) 
            addAtts.add("subsetVariables",
                suggestSubsetVariables(sourceTable, addTable, 1)); //nFiles

        //use original title, with InPort # added
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
        tFileDir  = File2.addSlash(tFileDir);
        tFileDir  = String2.replaceAll(tFileDir, "\\", "/");
        tFileDir  = String2.replaceAll(tFileDir,  ".", "\\.");
        tFileName = String2.replaceAll(tFileName, ".", "\\.");

        if (addTable.nColumns() == 0) {
            Attributes tAddAtts = new Attributes();
            tAddAtts.set("ioos_category", "Unknown");
            tAddAtts.set("missing_value", "???");
            tAddAtts.set("units", "???");
            sourceTable.addColumn(0, "noVariablesDefinedInInPort", new DoubleArray(), new Attributes());
               addTable.addColumn(0, "sampleDataVariable", new DoubleArray(), tAddAtts);
        }

        results.append(
            "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"" + 
                acronym.toLowerCase() + "InPort" + catID + "\" active=\"true\">\n" +
            "    <sourceUrl>" + tSourceUrl + "</sourceUrl>\n" +
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tFileName) + "</fileNameRegex>\n" + 
            "    <charset>ISO-8859-1</charset>\n" +
            "    <columnNamesRow>1</columnNamesRow>\n" +
            "    <firstDataRow>2</firstDataRow>\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +
            "    <accessibleViaFiles>true</accessibleViaFiles>\n" +
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
            String2.log("Background for ERDDAP: " + background.toString().trim());
        if (!"Unclassified".equals(securityClass))
            String2.log("WARNING! Security_class = " + securityClass);
        String2.log("generateDatasetsXml finished successfully.\n-----");
        return results.toString();
    }


    /**
     * This tests the time_zone attribute.
     *
     * @throws Throwable if trouble
     */
    public static void testTimeZone() throws Throwable {
        String2.log("\n*** EDDTableFromAsciiFiles.testTimeZone() \n");
        testVerboseOn();
        int po;
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String testDir = EDStatic.fullTestCacheDirectory;

        //test Calendar2.unitsSinceToEpochSeconds() with timeZone
        TimeZone     timeZone     = TimeZone.getTimeZone("US/Pacific");
        DateTimeZone dateTimeZone = DateTimeZone.forID(  "US/Pacific");
        double epSec;

        //test winter/standard time: 2005-04-03T00:00 Pacific
        // see https://www.timeanddate.com/worldclock/converter.html
        epSec = 1112515200; //from 2005-04-03T08:00Z in convert / time
        Test.ensureEqual(
            Calendar2.epochSecondsToIsoStringT(epSec), "2005-04-03T08:00:00", "");//8hrs 
        Test.ensureEqual(
            Calendar2.isoStringToEpochSeconds("2005-04-03T00:00", timeZone),
            epSec, "");

        //test summer/daylight savings time: 2005-04-03T05:00 Pacific
        epSec = 1112529600; //from 2005-04-03T12:00Z in convert / time
        Test.ensureEqual(
            Calendar2.epochSecondsToIsoStringT(epSec), "2005-04-03T12:00:00", "");//7hrs
        Test.ensureEqual(
            Calendar2.isoStringToEpochSeconds("2005-04-03T05:00", timeZone),
            epSec, "");

        //the source file
        results = String2.readFromFile(EDStatic.unitTestDataDir + "time/time_zone.txt")[1];
        expected = 
"timestamp_local,timestamp_utc,m\n" +
"2005-04-03T00:00,2005-04-03T08:00,1\n" + //spring time change
"2005-04-03T01:00,2005-04-03T09:00,2\n" +
"2005-04-03T02:00,2005-04-03T10:00,3\n" + //local jumps 2am to 4am
"2005-04-03T04:00,2005-04-03T11:00,4\n" +
"2005-04-03T05:00,2005-04-03T12:00,5\n" +
"9999-02-01T00:00,9999-02-01T00:00,-999\n" +
"unexpectedMV,unexpectedMV,unexpectedMV\n" +
"NaN,NaN,NaN\n" +
",,\n" +
"2005-10-30T00:00,2005-10-30T07:00,10\n" + //fall time change
"2005-10-30T01:00,2005-10-30T08:00,11\n" + //duplicate 1am
"2005-10-30T01:00,2005-10-30T09:00,12\n" +
"2005-10-30T02:00,2005-10-30T10:00,13\n" +
"2005-10-30T03:00,2005-10-30T11:00,14\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test dataset where local time -> Zulu
        String id = "testTimeZone";
        deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //.das
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_tz_all", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1125152e+9, NaN;\n" + //NaN because of string values->NaN
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +  //note missing_value removed
"    String standard_name \"time\";\n" +  
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  timestamp_utc {\n" +
"    Float64 actual_range 1.1125152e+9, NaN;\n" + //NaN because of string values->NaN
"    String ioos_category \"Time\";\n" +
"    String long_name \"Timestamp Utc\";\n" +  //note missing_value removed
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  m {\n" +
"    Int32 actual_range 1, 14;\n" + 
"    String ioos_category \"Time\";\n" +
"    Int32 missing_value -999;\n" + 
"    String units \"m\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Other\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        expected = 
//2016-09-19T20:17:35Z
"http://localhost:8080/cwexperimental/tabledap/testTimeZone.das\";\n" +
"    String infoUrl \"http://www.pfeg.noaa.gov\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"many, keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"Test time_zone\";\n" +
"    String time_coverage_start \"2005-04-03T08:00:00Z\";\n" + //UTC
"    String title \"Test time_zone\";\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 40));
        Test.ensureEqual(results.substring(Math.max(0, po)), expected, "\nresults=\n" + results);


        //.csv    for all
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_tz_all", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,timestamp_utc,m\n" +
"UTC,UTC,m\n" +
"2005-04-03T08:00:00Z,2005-04-03T08:00:00Z,1\n" + //spring time change
"2005-04-03T09:00:00Z,2005-04-03T09:00:00Z,2\n" +
"2005-04-03T10:00:00Z,2005-04-03T10:00:00Z,3\n" + //local jumps 2am to 4am
"2005-04-03T11:00:00Z,2005-04-03T11:00:00Z,4\n" +
"2005-04-03T12:00:00Z,2005-04-03T12:00:00Z,5\n" +
",,NaN\n" + //note all 3 mv's -> ""
",,NaN\n" +
",,NaN\n" +
",,NaN\n" +
"2005-10-30T07:00:00Z,2005-10-30T07:00:00Z,10\n" + //fall time change
"2005-10-30T09:00:00Z,2005-10-30T08:00:00Z,11\n" +
"2005-10-30T09:00:00Z,2005-10-30T09:00:00Z,12\n" + //duplicate 1am -> 9am
"2005-10-30T10:00:00Z,2005-10-30T10:00:00Z,13\n" +
"2005-10-30T11:00:00Z,2005-10-30T11:00:00Z,14\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }


    /** 
     * This does more tests of string time. */
    public static void testTimeZone2() throws Throwable {

        String2.log("\n****************** EDDTableFromAsciiFiles.testTimeZone2() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        String id = "testTimeZone2"; 
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //.csv   
        userDapQuery = "&time>=2004-12-03T15:55";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time,a,b\n" +
"UTC,liter per second,celsius\n" +
"2004-12-03T15:55:00Z,29.32,12.2\n" +
"2004-12-03T16:55:00Z,14.26,12.5\n" +
"2004-12-03T17:55:00Z,14.26,12.2\n" +
"2004-12-03T18:55:00Z,29.32,10.6\n" +
"2004-12-03T19:55:00Z,9.5,10.2\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "\nresults=\n" + results);
    }

    /** 
     * This tests string var with missing_value and string time with missing_value. */
    public static void testTimeMV() throws Throwable {

        String2.log("\n****************** EDDTableFromAsciiFiles.testTimeMV() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //the source file
        results = String2.readFromFile(EDStatic.unitTestDataDir + "time/testTimeMV.csv")[1];
        expected = 
"a,localStringTime,m\n" +
"a,2004-09-13T07:15:00,1\n" +
"NO SAMPLE,9997-04-06T00:00:00,-999\n" +
"c,2008-07-13T11:50:00,3\n" +
"d,9997-04-06T00:00:00,4\n" +
"NO SAMPLE,2008-07-29T09:50:00,-999\n" +
"f,2008-11-01T10:00:00,6\n" +
"NULL,9997-04-06T00:00:00,-999999\n" +
"h,2009-01-12T12:00:00,8\n" +
"i,99999,\n" +
"j,,\n" +
"k,2010-12-07T12:00:00,11\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String id = "testTimeMV"; 
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //.das   
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_3", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  a {\n" +
"    String ioos_category \"Unknown\";\n" + //note: no missing_value
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.0950849e+9, NaN;\n" + //NaN because max String isn't a valid time
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +   //note: no missing_value
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  m {\n" + 
"    Int32 _FillValue -999999;\n" + 
"    Int32 actual_range 1, 11;\n" + 
"    String ioos_category \"Unknown\";\n" +
"    Int32 missing_value -999;\n" + 
"    String units \"m\";\n" +
"  }\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        expected = 
//2016-09-19T22:37:33Z
"http://localhost:8080/cwexperimental/tabledap/testTimeMV.das\";\n" +
"    String infoUrl \"http://www.pfel.noaa.gov\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"lots, of, keywords\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"testTimeMV\";\n" +
"    String time_coverage_start \"2004-09-13T14:15:00Z\";\n" +  //UTC
"    String title \"testTimeMV\";\n" +
"  }\n" +
"}\n";
        int po = results.indexOf(expected.substring(0, 40));
        Test.ensureEqual(results.substring(Math.max(0, po)), expected, "\nresults=\n" + results);


        // a>b won't return mv=NO SAMPLE   or NULL
        //all string and date missing values are treated like / become ""
        userDapQuery = "&a>\"b\"";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
"c,2008-07-13T18:50:00Z,3\n" +
"d,,4\n" +
"f,2008-11-01T17:00:00Z,6\n" +
"h,2009-01-12T20:00:00Z,8\n" +
"i,,NaN\n" +
"j,,NaN\n" +
"k,2010-12-07T20:00:00Z,11\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        // a<k will return mv=NO SAMPLE 
        //all string and date missing values are treated like / become ""
        userDapQuery = "&a<\"k\"";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
"a,2004-09-13T14:15:00Z,1\n" +
",,NaN\n" +
"c,2008-07-13T18:50:00Z,3\n" +
"d,,4\n" +
",2008-07-29T16:50:00Z,NaN\n" +
"f,2008-11-01T17:00:00Z,6\n" +
",,NaN\n" +
"h,2009-01-12T20:00:00Z,8\n" +
"i,,NaN\n" +
"j,,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        // a<j & a!=""  won't return mv converted to ""
        userDapQuery = "&a<\"j\"&a!=\"\"";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv2b", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
"a,2004-09-13T14:15:00Z,1\n" +
"c,2008-07-13T18:50:00Z,3\n" +
"d,,4\n" +
"f,2008-11-01T17:00:00Z,6\n" +
"h,2009-01-12T20:00:00Z,8\n" +
"i,,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        // a="" will return mv rows converted to ""
        userDapQuery = "&a=\"\"";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv3", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
",,NaN\n" +
",2008-07-29T16:50:00Z,NaN\n" +
",,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    
        // a!="" will return non mv rows
        userDapQuery = "&a!=\"\"";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv3b", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
"a,2004-09-13T14:15:00Z,1\n" +
"c,2008-07-13T18:50:00Z,3\n" +
"d,,4\n" +
"f,2008-11-01T17:00:00Z,6\n" +
"h,2009-01-12T20:00:00Z,8\n" +
"i,,NaN\n" +
"j,,NaN\n" +
"k,2010-12-07T20:00:00Z,11\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    
        //time>... works in UTC time and won't return mv   
        userDapQuery = "&time>=2010-12-07T20";  //request in UTC
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv4", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
"k,2010-12-07T20:00:00Z,11\n"; //local +8 hrs
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //time=NaN  returns mv   
        userDapQuery = "&time=NaN";  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv4aa", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
",,NaN\n" +
"d,,4\n" +
",,NaN\n" +
"i,,NaN\n" +
"j,,NaN\n"; 
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //m>=11  won't return mv   
        userDapQuery = "&m>=11";  //request in UTC
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv4b", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
"k,2010-12-07T20:00:00Z,11\n"; //local +8 hrs
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //m<=1  won't return mv   
        userDapQuery = "&m<=1";  //request in UTC
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv4c", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
"a,2004-09-13T14:15:00Z,1\n"; //local +8 hrs
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //m=NaN returns correct info
        userDapQuery = "&m=NaN";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_mv5", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"a,time,m\n" +
",UTC,m\n" +
",,NaN\n" +
",2008-07-29T16:50:00Z,NaN\n" +
",,NaN\n" +
"i,,NaN\n" +
"j,,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    
    }

    /**
     * This tests GenerateDatasetsXml with EDDTableFromInPort when there are  
     * data variables. 
     */
    public static void testGenerateDatasetsXmlFromInPort() throws Throwable {
        String2.log("\n*** testGenerateDatasetsXmlFromInPort()\n");
        testVerboseOn();
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        try {
            String xmlFile = "/u00/data/points/inportXml/NOAA/NMFS/AFSC/inport/xml/17336.xml";
            String dataDir = "/u00/data/points/inportData/afsc/";

            String results = generateDatasetsXmlFromInPort(
                xmlFile, ".*", ".*", dataDir) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromInPort",
                xmlFile, dataDir},
                false); //doIt loop?

String expected = 
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"afscInPort17336_25511\" active=\"true\">\n" +
"    <sourceUrl>http://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0131425</sourceUrl>\n" +
"    <fileDir>/u00/data/points/inportData/afsc/</fileDir>\n" +
"    <fileNameRegex>Comments</fileNameRegex>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <fgdcFile>/u00/data/points/inportXml/NOAA/NMFS/AFSC/fgdc/xml/17336.xml</fgdcFile>\n" +
"    <iso19115File>/u00/data/points/inportXml/NOAA/NMFS/AFSC/iso19115/xml/17336.xml</iso19115File>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"acknowledgment\">BOEM funded this research.</att>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">phillip.clapham@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Phillip Clapham</att>\n" +
"        <att name=\"creator_url\">http://www.afsc.noaa.gov/</att>\n" +
"        <att name=\"data_status\">Complete</att>\n" +
"        <att name=\"date-created\">2015</att>\n" +
"        <att name=\"downloads\">Download #1\n" +
"URL: http://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0131425</att>\n" +
"        <att name=\"geospatial_description\">Chukchi and Beaufort Seas off Barrow, AK.</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">72.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">70.0</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">-151.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-158.0</att>\n" +
"        <att name=\"history\">2015-06-17T17:51:02 Janice Waite originally created InPort metadata cat-id#17336_25511.\n" +
"2016-05-18T18:52:32 Janice Waite last modified InPort metadata cat-id#17336_25511.\n" +
"2015 data set originally published.\n" +
today + " ERDDAP&#39;s GenerateDatasetsXml (contact: bob.simons@noaa.gov) converted InPort metadata from https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport/xml/17336.xml into an ERDDAP dataset description.</att>\n" +
"        <att name=\"infoUrl\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport/xml/17336.xml</att>\n" +
"        <att name=\"InPort_catalog_ID\">17336_25511</att>\n" +
"        <att name=\"InPort_catalog_type\">Data Entity</att>\n" +
"        <att name=\"InPort_XML_URL\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport/xml/17336.xml</att>\n" +
"        <att name=\"institution\">NOAA NMFS AFSC</att>\n" +
"        <att name=\"keywords\">aerial, afsc, beaufort, beaufort sea, biota, boem, bowfest, bowhead, bureau, chukchi, chukchi sea, comments, data, date, ecology, energy, feeding, files, fisheries, hole, identification, initia, institution, local, management, marine, national, nmfs, noaa, ocean, oceanographic, oceans, oregon, osu, photo, photo-identification, sea, service, state, study, summer, time, university, was, whale, whoi, woods</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">Distribution Liability: The user is responsible for the results of any application of this data for other than its intended purpose. NOAA denies liability if the data are misused.\n" +
"Data access constraints: There are no legal restrictions on access to the data.  They reside in public domain and can be freely distributed.\n" +
"Data access procedure: Data is available at http://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0131425\n" +
"Data use constraints: User must read and fully comprehend the metadata prior to use.  Applications or inferences derived from the data should be carefully considered for accuracy.  Acknowledgement\n" +
"of NOAA/NMFS/AFSC, as the source from which these data were obtained in any publications and/or other representations of these, data is suggested.</att>\n" +
"        <att name=\"metadata_workflow_state\">Published / External</att>\n" +
"        <att name=\"publication_status\">Public</att>\n" +
"        <att name=\"publisher_email\">ren.narita@noaa.gov</att>\n" +
"        <att name=\"publisher_name\">Renold E Narita</att>\n" +
"        <att name=\"related_items\">Related Item #1: InPort catalog ID=25511, catalog type=Data Entity, relation=sibling\n" +
"Title: Comments\n" +
"\n" +
"Related Item #2: InPort catalog ID=25512, catalog type=Data Entity, relation=sibling\n" +
"Title: GPS\n" +
"\n" +
"Related Item #3: InPort catalog ID=25513, catalog type=Data Entity, relation=sibling\n" +
"Title: GrpPassMark\n" +
"\n" +
"Related Item #4: InPort catalog ID=25514, catalog type=Data Entity, relation=sibling\n" +
"Title: Positions\n" +
"\n" +
"Related Item #5: InPort catalog ID=25515, catalog type=Data Entity, relation=sibling\n" +
"Title: Sightings\n" +
"\n" +
"Related Item #6: InPort catalog ID=25516, catalog type=Data Entity, relation=sibling\n" +
"Title: Weather</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">The Bowhead Whale Feeding Ecology Study (BOWFEST) was initiated in May 2007 through an Interagency Agreement between the Bureau of Ocean Energy Management (BOEM) (formerly Minerals Management Service (MMS)) and the National Marine Mammal Laboratory (NMML). This was a multi-disciplinary study involving oceanography, acoustics, tagging, stomach sampling and aerial surveys and included scientists from a wide range of institutions (Woods Hole Oceanographic Institution (WHOI), University of Rhode Island (URI), University of Alaska Fairbanks (UAF), University of Washington (UW), Oregon State University (OSU), North Slope Borough (NSB), and NMML).  The data described and presented here are only from the aerial survey component of this larger study.  The focus of the aerial survey was to document patterns and variability in the timing and locations of bowhead whales. Using a NOAA Twin Otter, scientists from NMML conducted aerial surveys from mid-August to mid-September during this five year study between years 2007-2011.  Surveys were conducted in the BOWFEST study area (continental shelf waters between 157 degree W and 152 degree W and from the coastline to 72 degree N, with most of the effort concentrated between 157 degree W and 154 degree W and between the coastline and 71 degree 44&#39;N).\n" +
"\n" +
"Document patterns and variability in the timing and locations of bowhead whales relative to oceanography and prey densities to learn about bowhead whale feeding ecology during late summer in the vicinity of Barrow, AK.\n" +
"\n" +
"Loaded by FGDC Metadata Uploader, batch 6250, 06-17-2015 17:51\n" +
"\n" +
"The following FGDC sections are not currently supported in InPort, but were preserved and will be included in the FGDC export:\n" +
"-  Taxonomy (FGDC:taxonomy)</att>\n" +
"        <att name=\"time_coverage_end\">2011</att>\n" +
"        <att name=\"time_coverage_start\">2007</att>\n" +
"        <att name=\"title\">Cetacean Assessment and Ecology Program, AFSC/NMML: Bowhead Whale Feeding Ecology Study (BOWFEST): Aerial Survey in Chukchi and Beaufort Seas, 2007-2011 - Comments (InPort #17336_25511)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>COMMENTS</sourceName>\n" +
"        <destinationName>comments</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"comment\">Any pertinent data that is not recorded elsewhere</att>\n" +
"            <att name=\"isPrimaryKey\">N</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TIME</sourceName>\n" +
"        <destinationName>TIME</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">931 - 2145</att>\n" +
"            <att name=\"comment\">Alaksa Standard Time (-8 GMT)</att>\n" +
"            <att name=\"isPrimaryKey\">N</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DATE</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">200708230000 - 201109160000</att>\n" +
"            <att name=\"isPrimaryKey\">N</att>\n" +
"            <att name=\"time_precision\">1970-01-01</att>\n" +
"            <att name=\"units\">M/d/yy</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

            String tResults = results.substring(0, expected.length());
            Test.ensureEqual(tResults, expected, "tResults=\n" + tResults);

            tResults = gdxResults.substring(0, expected.length());
            Test.ensureEqual(tResults, expected, "tResults=\n" + tResults);

        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
                String2.pressEnterToContinue(msg + 
                    "\nUnexpected error using generateDatasetsXmlFromInPort."); 
        }

    }

    /**
     * This tests GenerateDatasetsXml with EDDTableFromInPort when there are no 
     * data variables. 
     */
    public static void testGenerateDatasetsXmlFromInPort2() throws Throwable {
        String2.log("\n*** testGenerateDatasetsXmlFromInPort2()\n");
        testVerboseOn();
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        try {
            String xmlFile = "/u00/data/points/inportXml/NOAA/NMFS/OST/inport/xml/25048.xml";
            String dataDir = "/u00/data/points/inportData/ost/";

            String results = generateDatasetsXmlFromInPort(
                xmlFile, ".*", ".*", dataDir) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromInPort",
                xmlFile, dataDir},
                false); //doIt loop?

String expected = 
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"ostInPort25048\" active=\"true\">\n" +
"    <sourceUrl>ftp://ftp.nodc.noaa.gov/nodc/archive/</sourceUrl>\n" +
"    <fileDir>/u00/data/points/inportData/ost/</fileDir>\n" +
"    <fileNameRegex>???</fileNameRegex>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <fgdcFile>/u00/data/points/inportXml/NOAA/NMFS/OST/fgdc/xml/25048.xml</fgdcFile>\n" +
"    <iso19115File>/u00/data/points/inportXml/NOAA/NMFS/OST/iso19115/xml/25048.xml</iso19115File>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">rob.andrews@noaa.gov</att>\n" +
"        <att name=\"creator_name\">William R Andrews</att>\n" +
"        <att name=\"creator_url\">https://www.st.nmfs.noaa.gov/</att>\n" +
"        <att name=\"data_status\">In Work</att>\n" +
"        <att name=\"downloads\">Download #1\n" +
"URL: ftp://ftp.nodc.noaa.gov/nodc/archive/</att>\n" +
"        <att name=\"geospatial_description\">atlantic and gulf coast states</att>\n" +
"        <att name=\"history\">2015-05-19T10:25:24 Lauren Dolinger Few originally created InPort metadata cat-id#25048.\n" +
"2016-02-10T12:35:20 Lauren Dolinger Few last modified InPort metadata cat-id#25048.\n" +
today + " ERDDAP&#39;s GenerateDatasetsXml (contact: bob.simons@noaa.gov) converted InPort metadata from https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/OST/inport/xml/25048.xml into an ERDDAP dataset description.</att>\n" +
"        <att name=\"infoUrl\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/OST/inport/xml/25048.xml</att>\n" +
"        <att name=\"InPort_catalog_ID\">25048</att>\n" +
"        <att name=\"InPort_catalog_type\">Data Set</att>\n" +
"        <att name=\"InPort_XML_URL\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/OST/inport/xml/25048.xml</att>\n" +
"        <att name=\"institution\">NOAA NMFS OST</att>\n" +
"        <att name=\"keywords\">cleaned, data, effort, files, fisheries, fishing, local, marine, national, nmfs, noaa, ost, qcd, question, service, survey</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">Data access constraints: none.\n" +
"Data access procedure: download from website</att>\n" +
"        <att name=\"metadata_workflow_state\">Published / External</att>\n" +
"        <att name=\"publication_status\">Public</att>\n" +
"        <att name=\"publisher_email\">nathan.wilson@noaa.gov</att>\n" +
"        <att name=\"publisher_name\">Nathan Wilson</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">Cleaned and QCd data for the Fishing Effort Survey. Questions on fishing and other out are asked on weather and outdoor activity, including fishing trips. Used for estimation of recreational fishing effort for shore and private/rental boats.</att>\n" +
"        <att name=\"time_coverage_start\">2015</att>\n" +
"        <att name=\"title\">Fishing Effort Survey (FES), Survey Data (InPort #25048)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>noVariablesDefinedInInPort</sourceName>\n" +
"        <destinationName>sampleDataVariable</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"missing_value\">???</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

            String tResults = results.substring(0, expected.length());
            Test.ensureEqual(tResults, expected, "tResults=\n" + tResults);

            tResults = gdxResults.substring(0, expected.length());
            Test.ensureEqual(tResults, expected, "tResults=\n" + tResults);

        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
                String2.pressEnterToContinue(msg + 
                    "\nUnexpected error using generateDatasetsXmlFromInPort2."); 
        }

    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedDatasetInfo) throws Throwable {
        /* */
        testBasic(deleteCachedDatasetInfo);
        testGenerateDatasetsXml();
        testFixedValue();
        testBasic2();
        testTimeZone();
        testTimeZone2();
        testTimeMV();
        testGenerateDatasetsXmlFromInPort();
        testGenerateDatasetsXmlFromInPort2();
        /* */

        //not usually run
    }


}

