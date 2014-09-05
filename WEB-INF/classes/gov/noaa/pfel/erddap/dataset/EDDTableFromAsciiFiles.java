/* 
 * EDDTableFromAsciiFiles Copyright 2009, NOAA.
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
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.util.HashMap;
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
    public EDDTableFromAsciiFiles(String tDatasetID, String tAccessibleTo,
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

        super("EDDTableFromAsciiFiles", true, tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory);
    }

    /** The constructor for subclasses. */
    public EDDTableFromAsciiFiles(String tClassName, String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, String tSosOfferingPrefix,
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

        super(tClassName, true, tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory);

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
        table.readASCII(fileDir + fileName, 
            "ISO-8859-1", columnNamesRow - 1, firstDataRow - 1,
            null, null, null, null, true); //testColumns, testMin, testMax, loadColumns, simplify);

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
        String charset, int columnNamesRow, int firstDataRow, int tReloadEveryNMinutes,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromAsciiFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash

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
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");

        boolean dateTimeAlreadyFound = false;
        for (int col = 0; col < dataSourceTable.nColumns(); col++) {
            String colName = dataSourceTable.getColumnName(col);
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
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
                probablyHasLonLatTime(dataSourceTable)? "Point" : "Other",
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
            "    <fileDir>" + tFileDir + "</fileDir>\n" +
            "    <recursive>true</recursive>\n" +
            "    <fileNameRegex>" + tFileNameRegex + "</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <charset>" + charset + "</charset>\n" +
            "    <columnNamesRow>" + columnNamesRow + "</columnNamesRow>\n" +
            "    <firstDataRow>" + firstDataRow + "</firstDataRow>\n" +
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
            Attributes externalAddAttributes = new Attributes();
            externalAddAttributes.add("title", "New Title!");
            String results = generateDatasetsXml(
                "c:/u00/cwatch/testData/asciiNdbc/",  ".*\\.csv",
                "c:/u00/cwatch/testData/asciiNdbc/31201_2009.csv", 
                "ISO-8859-1", 1, 3, 1440,
                "", "_.*$", ".*", "stationID",  //just for test purposes; station is already a column in the file
                "time", "station time", 
                "http://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!",
                externalAddAttributes) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromAsciiFiles",
                "c:/u00/cwatch/testData/asciiNdbc/",  ".*\\.csv",
                "c:/u00/cwatch/testData/asciiNdbc/31201_2009.csv", 
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
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"asciiNdbc_8443_4c8b_d0e1\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <fileDir>c:/u00/cwatch/testData/asciiNdbc/</fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>.*\\.csv</fileNameRegex>\n" +
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
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"creator_name\">NOAA NDBC</att>\n" +
"        <att name=\"creator_url\">http://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"infoUrl\">http://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"institution\">NOAA NDBC</att>\n" +
"        <att name=\"keywords\">altitude, atmosphere,\n" +
"Atmosphere &gt; Altitude &gt; Station Height,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"atmospheric, atmp, direction, height, identifier, ndbc, newer, noaa, speed, station, surface, temperature, time, title, wind, wind_from_direction, wind_speed, winds, wtmp</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">The new summary!</att>\n" +
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
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ssZ</att>\n" +
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
"            <att name=\"long_name\">WTMP</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            //!!! actually this will fail with a specific error which is caught below
            EDD edd = oneFromXmlFragment(results);
            //these won't actually be tested...
            Test.ensureEqual(edd.datasetID(), "asciiNdbc_8443_4c8b_d0e1", "");
            Test.ensureEqual(edd.title(), "The Newer Title!", "");
            //If it does get here, this will fail on purpose...
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "zztop???", "");

        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            if (msg.indexOf(
"When a variable's destinationName is \"altitude\", the sourceAttributes or addAttributes " +
"\"units\" MUST be \"m\" (not \"null\").\n" +
"If needed, use \"scale_factor\" to convert the source values to meters (positive=up),\n" +
"use a different destinationName for this variable.") >= 0) {
                String2.log("EXPECTED ERROR while creating the edd: altitude's units haven't been set.\n");
            } else 
                String2.getStringFromSystemIn(msg + 
                    "\nUnexpected error using generateDatasetsXml." + 
                    "\nPress ^C to stop or Enter to continue..."); 
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
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

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
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
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
//today + " http://127.0.0.1:8080/cwexperimental/tabledap/
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
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 37.75;\n" +
"    String sourceUrl \"The source URL.\";\n" +
"    Float64 Southernmost_Northing -27.7;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
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
            EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

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
    "    String Conventions \"COARDS, CF-1.4, Unidata Dataset Discovery v1.0\";\n" +
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
    //"2013-05-24T17:24:54Z http://127.0.0.1:8080/cwexperimental/tabledap/testWTDL.das\";\n" +
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
    "    String Metadata_Conventions \"COARDS, CF-1.4, Unidata Dataset Discovery v1.0\";\n" +
    "    Float64 Northernmost_Northing 30.368;\n" +
    "    String sourceUrl \"(local files)\";\n" +
    "    Float64 Southernmost_Northing 26.6255;\n" +
    "    String standard_name_vocabulary \"CF-12\";\n" +
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
    public static void test(boolean deleteCachedDatasetInfo) throws Throwable {
        testBasic(deleteCachedDatasetInfo);
        testGenerateDatasetsXml();
        testFixedValue();

        //not usually run
    }


}

