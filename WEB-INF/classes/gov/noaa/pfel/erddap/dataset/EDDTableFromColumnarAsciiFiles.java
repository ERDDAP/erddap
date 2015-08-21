/* 
 * EDDTableFromColumnarAsciiFiles Copyright 2014, NOAA.
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
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.util.HashMap;
import java.util.List;

/** 
 * This class represents a table of data from a collection of 
 * Columnar / Fixed Length / Fixed Format ASCII data files.
 * I.e., each data variable is stored in a specific, fixed substring of each row.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2014-11-07
 */
public class EDDTableFromColumnarAsciiFiles extends EDDTableFromFiles { 


    /** Used to ensure that all non-axis variables in all files have the same leftmost dimension. */
    //protected String dim0Name = null;


    /** 
     * The constructor. 
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * <p>The sortedColumnSourceName isn't utilized.
     */
    public EDDTableFromColumnarAsciiFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File,
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles) 
        throws Throwable {

        super("EDDTableFromColumnarAsciiFiles", true, tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles);
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

        //gather the info needed to read the file 
        String tLoadCol[] = sourceDataNames.toArray();
        int nCol = tLoadCol.length;
        int tStartColumn[] = new int[nCol];
        int tStopColumn[]  = new int[nCol];
        Class tColClass[]  = new Class[nCol];
        String sourceNames[] = dataVariableSourceNames();
        //String2.log(">> sourceDataTypes=" + String2.toCSSVString(sourceDataTypes));
        for (int col = 0; col < nCol; col++) {
            int dv = String2.indexOf(sourceNames, tLoadCol[col]);
            if (dv < 0) 
                throw new SimpleException("sourceName=" + tLoadCol[col] + 
                    " not found in " + String2.toCSSVString(sourceNames));
            tStartColumn[col] = startColumn[dv];
            tStopColumn[col] = stopColumn[dv];
            tColClass[col] = sourceDataTypes[col].equals("boolean")? boolean.class :
                PrimitiveArray.elementStringToClass(sourceDataTypes[col]);
        }

        Table table = new Table();
        table.readColumnarASCIIFile(fileDir + fileName, charset, 
            firstDataRow - 1, tLoadCol, tStartColumn, tStopColumn, tColClass);
        //String2.log(">> lowGetSourceData:\n" + table.dataToCSVString(5));
        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromColumnarAsciiFiles.
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
     * No: SortedColumnSourceName   use "" if not known or not needed. 
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
        String tColumnNameForExtract, //String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromColumnarAsciiFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        firstDataRow = Math.max(1, firstDataRow); //1..
        if (charset == null || charset.length() == 0)
            charset = "ISO-8859-1";
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

        //read the lines of the sample file
        String lines[] = String2.readLinesFromFile(sampleFileName, charset, 2);

        //hueristic: col with low usage then col with high usage (or vice versa)
        //  indicates new column
        int nLines = lines.length;
        int longest = 0;
        if (columnNamesRow >= 1)
            longest = lines[columnNamesRow-1].length();
        for (int i = firstDataRow - 1; i < nLines; i++)
            longest = Math.max(longest, lines[i].length());
        longest++; //ensure at least one empty col at end
        int nCharsInCol[] = new int[longest]; 
        if (columnNamesRow >= 1) {
            String s = lines[columnNamesRow - 1];
            int len = s.length();
            for (int po = 0; po < len; po++)
                if (s.charAt(po) != ' ')
                    nCharsInCol[po]++;
        }
        for (int i = firstDataRow - 1; i < nLines; i++) {
            String s = lines[i];
            int len = s.length();
            for (int po = 0; po < len; po++)
                if (s.charAt(po) != ' ')
                    nCharsInCol[po]++;
        }
        //for (int po = 0; po < longest; po++) 
        //    String2.log(po + " n=" + nCharsInCol[po]);
        int firstEmptyPo = longest - 1; //first empty col at end of line
        while (firstEmptyPo > 0 && nCharsInCol[firstEmptyPo - 1] == 0)
            firstEmptyPo--;
        IntArray start = new IntArray(); //0..
        IntArray stop = new IntArray();  //0..   exclusive
        int lowThresh = Math.max(2, (nLines-firstDataRow) / 10);
        int highThresh = (nLines-firstDataRow) / 2;
        int po = 0;
        start.add(po);
        while (po < firstEmptyPo) {

            //seek col > highThresh
            while (po < firstEmptyPo && nCharsInCol[po] < highThresh)
                po++;
            if (po == firstEmptyPo) {
                stop.add(longest); 
                break;
            }

            //seek col <= lowThresh
            while (po < firstEmptyPo && nCharsInCol[po] > lowThresh)
                po++;
            //seek lowest point of columns < lowThresh
            int lowestPo = po;
            int lowestN = nCharsInCol[po];
            while (po <= firstEmptyPo && nCharsInCol[po] <= lowThresh) {
                if (nCharsInCol[po] <= lowestN) {
                    lowestPo = po; 
                    lowestN = nCharsInCol[po];
                }
                po++;
            }
            stop.add(lowestPo + 1);
            if (lowestPo == firstEmptyPo)
                break;

            start.add(lowestPo + 1); //it has >0 chars
        }
        int nCols = start.size();

        //read column names
        String colNames[] = new String[nCols];
        String namesLine = columnNamesRow >= 1 && columnNamesRow < nLines?
            lines[columnNamesRow - 1] : "";
        int namesLineLength = namesLine.length();
        for (int col = 0; col < nCols; col++) {            
            colNames[col] = start.get(col) < namesLineLength?
                namesLine.substring(start.get(col), Math.min(stop.get(col), namesLineLength)).trim() : 
                "";
            if (colNames[col].length() == 0)
                colNames[col] = "column" + (col + 1);
        }

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        if (charset == null || charset.length() == 0)
            charset = "ISO-8859-1";
        dataSourceTable.readColumnarASCII(sampleFileName, lines, firstDataRow - 1,
            colNames, start.toArray(), stop.toArray(), null); //null = simplify

        //globalAttributes 
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", 
            "(" + (File2.isRemote(tFileDir)? "remote" : "local") + " files)");
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");

        boolean dateTimeAlreadyFound = false;
        for (int col = 0; col < dataSourceTable.nColumns(); col++) {
            String colName = dataSourceTable.getColumnName(col);
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                null, //no source global attributes
                dataSourceTable.columnAttributes(col), colName, 
                true, true); //addColorBarMinMax, tryToFindLLAT
            addAtts.add("startColumn", start.get(col));
            addAtts.add("stopColumn", stop.get(col));

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
            //if (tSortedColumnSourceName.length() == 0 && 
            //    isDateTime && !dateTimeAlreadyFound) {
            //    dateTimeAlreadyFound = true;
            //    tSortedColumnSourceName = colName;
            //}
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
        //if (tSortFilesBySourceNames.length() == 0)
        //    tSortFilesBySourceNames = (tColumnNameForExtract + 
        //        (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName)).trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            " * Since the source files don't have any metadata, you must add metadata\n" +
            "   below, notably 'units' for each of the dataVariables.\n" +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromColumnarAsciiFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + tFileNameRegex) + 
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + 
            "</updateEveryNMillis>\n" +  
            "    <fileDir>" + tFileDir + "</fileDir>\n" +
            "    <recursive>true</recursive>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <charset>" + charset + "</charset>\n" +
            "    <columnNamesRow>" + columnNamesRow + "</columnNamesRow>\n" +
            "    <firstDataRow>" + firstDataRow + "</firstDataRow>\n" +
            "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
            //"    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n" +
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

        Attributes externalAddAttributes = new Attributes();
        externalAddAttributes.add("title", "New Title!");
        //public static String generateDatasetsXml(String tFileDir, String tFileNameRegex, 
        //    String sampleFileName, 
        //    String charset, int columnNamesRow, int firstDataRow, int tReloadEveryNMinutes,
        //    String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        //    String tColumnNameForExtract,    //no tSortedColumnSourceName,
        //    String tSortFilesBySourceNames, 
        //    String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        //    Attributes externalAddGlobalAttributes)
        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir,  "columnar.*\\.txt",
            EDStatic.unitTestDataDir + "columnarAscii.txt", 
            null, 3, 4, 1440,
            "", "", "", "",  
            "", 
            "http://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!",
            externalAddAttributes) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromColumnarAsciiFiles",
            EDStatic.unitTestDataDir,  "columnar.*\\.txt",
            EDStatic.unitTestDataDir + "columnarAscii.txt", 
            "", "3", "4", "1440",
            "", "", "", "",  
            "", 
            "http://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!"},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
" * Since the source files don't have any metadata, you must add metadata\n" +
"   below, notably 'units' for each of the dataVariables.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromColumnarAsciiFiles\" datasetID=\"erddapTest_4df3_40f4_29c6\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "</fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>columnar.*\\.txt</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>3</columnNamesRow>\n" +
"    <firstDataRow>4</firstDataRow>\n" +
"    <preExtractRegex></preExtractRegex>\n" +
"    <postExtractRegex></postExtractRegex>\n" +
"    <extractRegex></extractRegex>\n" +
"    <columnNameForExtract></columnNameForExtract>\n" +
"    <sortFilesBySourceNames></sortFilesBySourceNames>\n" +
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
"        <att name=\"creator_email\">webmaster.ndbc@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA NDBC</att>\n" +
"        <att name=\"creator_url\">http://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"infoUrl\">http://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"institution\">NOAA NDBC</att>\n" +
"        <att name=\"keywords\">aBoolean, aByte, aChar, aDouble, aFloat, aLong, anInt, aShort, aString, boolean, buoy, byte, center, char, data, double, float, int, long, national, ndbc, newer, noaa, short, string, title</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">The new summary! NOAA National Data Buoy Center (NDBC) data from a local source.</att>\n" +
"        <att name=\"title\">The Newer Title!</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>aString</sourceName>\n" +
"        <destinationName>aString</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A String</att>\n" +
"            <att name=\"startColumn\" type=\"int\">0</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">9</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aChar</sourceName>\n" +
"        <destinationName>aChar</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Char</att>\n" +
"            <att name=\"startColumn\" type=\"int\">9</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">15</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aBoolean</sourceName>\n" +
"        <destinationName>aBoolean</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Boolean</att>\n" +
"            <att name=\"startColumn\" type=\"int\">15</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">24</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aByte</sourceName>\n" +
"        <destinationName>aByte</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Byte</att>\n" +
"            <att name=\"startColumn\" type=\"int\">24</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">30</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aShort</sourceName>\n" +
"        <destinationName>aShort</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Short</att>\n" +
"            <att name=\"startColumn\" type=\"int\">30</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">37</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>anInt</sourceName>\n" +
"        <destinationName>anInt</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">An Int</att>\n" +
"            <att name=\"startColumn\" type=\"int\">37</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">45</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aLong</sourceName>\n" +
"        <destinationName>aLong</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Long</att>\n" +
"            <att name=\"startColumn\" type=\"int\">45</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">57</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aFloat</sourceName>\n" +
"        <destinationName>aFloat</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Float</att>\n" +
"            <att name=\"startColumn\" type=\"int\">57</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">66</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aDouble</sourceName>\n" +
"        <destinationName>aDouble</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Double</att>\n" +
"            <att name=\"startColumn\" type=\"int\">66</att>\n" +
"            <att name=\"stopColumn\" type=\"int\">84</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        EDD edd = oneFromXmlFragment(results);
        Test.ensureEqual(edd.datasetID(), "erddapTest_4df3_40f4_29c6", "");
        Test.ensureEqual(edd.title(), "The Newer Title!", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "aString, aChar, aBoolean, aByte, aShort, anInt, aLong, aFloat, aDouble", 
            "destinationNames");

        String userDapQuery = "";
        String tName = edd.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, edd.className() + "_1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n" +
",,,,,,,,\n" +
"abcdef,Ab,t,24,24000,24000000,240000000000,2.4,2.412345678987654\n" +
"short:,,,NaN,NaN,NaN,,NaN,NaN\n" +
"fg,F,true,11,12001,1200000,12000000000,1.21,1.0E200\n" +
"h,H,1,12,12002,120000,1200000000,1.22,2.0E200\n" +
"i,I,TRUE,13,12003,12000,120000000,1.23,3.0E200\n" +
"j,J,f,14,12004,1200,12000000,1.24,4.0E200\n" +
"k,K,false,15,12005,120,1200000,1.25,5.0E200\n" +
"l,L,0,16,12006,12,120000,1.26,6.0E200\n" +
"m,M,FALSE,17,12007,121,12000,1.27,7.0E200\n" +
"n,N,8,18,12008,122,1200,1.28,8.0E200\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests the methods in this class with a 1D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic() throws Throwable {
        String2.log("\n*** EDDTableFromColumnarAsciiFiles.testBasic()\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String testDir = EDStatic.fullTestCacheDirectory;

        String id = "testTableColumnarAscii";
        deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        String2.log("\nEDDTableFromColumnarAsciiFiles test das and dds for entire dataset\n");
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
//2014-12-04T19:15:21Z http://127.0.0.1:8080/cwexperimental/tabledap/testTableColumnarAscii.das";
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
"columnarAscii,5.0,abcdef,65,1,24,24000,24000000,240000000000,2.4,2.412345678987654\n" +
"columnarAscii,5.0,short:,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"columnarAscii,5.0,fg,70,1,11,12001,1200000,12000000000,1.21,1.0E200\n" +
"columnarAscii,5.0,h,72,1,12,12002,120000,1200000000,1.22,2.0E200\n" +
"columnarAscii,5.0,i,73,1,13,12003,12000,120000000,1.23,3.0E200\n" +
"columnarAscii,5.0,j,74,0,14,12004,1200,12000000,1.24,4.0E200\n" +
"columnarAscii,5.0,k,75,0,15,12005,120,1200000,1.25,5.0E200\n" +
"columnarAscii,5.0,l,76,0,16,12006,12,120000,1.26,6.0E200\n" +
"columnarAscii,5.0,m,77,0,17,12007,121,12000,1.27,7.0E200\n" +
"columnarAscii,5.0,n,78,1,18,12008,122,1200,1.28,8.0E200\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //only subsetVars
        userDapQuery = "fileName,five";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_sv", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        expected = 
"fileName,five\n" +
",\n" +
"columnarAscii,5.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //subset of variables, constrain boolean and five
        userDapQuery = "anInt,fileName,five,aBoolean&aBoolean=1&five=5";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_conbool", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        expected = 
"anInt,fileName,five,aBoolean\n" +
",,,\n" +
"24000000,columnarAscii,5.0,1\n" +
"1200000,columnarAscii,5.0,1\n" +
"120000,columnarAscii,5.0,1\n" +
"12000,columnarAscii,5.0,1\n" +
"122,columnarAscii,5.0,1\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromColumnarAsciiFiles.testBasic() finished successfully\n");
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        testGenerateDatasetsXml();
        testBasic();

        //not usually run
    }

}

