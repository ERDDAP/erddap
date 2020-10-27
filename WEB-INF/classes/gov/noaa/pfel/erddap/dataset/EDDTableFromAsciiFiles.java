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
import com.cohort.array.PAType;
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
import gov.noaa.pfel.coastwatch.util.Tally;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import java.time.ZoneId;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/** 
 * This class represents a table of data from a collection of ASCII CSV or TSV data files.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-02-13
 */
public class EDDTableFromAsciiFiles extends EDDTableFromFiles { 


    /** Used to ensure that all non-axis variables in all files have the same leftmost dimension. */
    protected String dim0Name = null;

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
        String tCharset, String tSkipHeaderToRegex, String tSkipLinesRegex,
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

        super("EDDTableFromAsciiFiles", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tSkipHeaderToRegex, tSkipLinesRegex,
            tColumnNamesRow, tFirstDataRow, tColumnSeparator,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, 
            tFileTableInMemory, tAccessibleViaFiles, tRemoveMVRows, 
            tStandardizeWhat, tNThreads, 
            tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex,
            tAddVariablesWhere);
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
        String tCharset, String tSkipHeaderToRegex, String tSkipLinesRegex,
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

        super(tClassName, tDatasetID, tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, 
            tMetadataFrom,
            tCharset, tSkipHeaderToRegex, tSkipLinesRegex,
            tColumnNamesRow, tFirstDataRow, tColumnSeparator,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, 
            tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows, tStandardizeWhat, 
            tNThreads, tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex,
            tAddVariablesWhere);

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

        if (!mustGetData) 
            //Just return a table with columns but no rows. There is never any metadata.
            return Table.makeEmptyTable(sourceDataNames.toArray(), sourceDataTypes);

        Table table = new Table();
        table.allowRaggedRightInReadASCII = true;
        table.readASCII(tFileDir + tFileName, charset, 
            skipHeaderToRegex, skipLinesRegex, columnNamesRow - 1, firstDataRow - 1, 
            columnSeparator, 
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
                            ca.addString(pa.getString(i));
                        newPa = ca;
                    } else {
                        newPa = PrimitiveArray.factory(
                            PAType.fromCohortString(sourceDataTypes[sd]), 1, false);
                        newPa.append(pa);
                    }
                    table.setColumn(tc, newPa);
                }
            }
        }

        //unpack
        table.standardize(standardizeWhat);

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
        String charset, int columnNamesRow, int firstDataRow, String columnSeparator,
        int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        int tStandardizeWhat, String tCacheFromUrl, 
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromAsciiFiles.generateDatasetsXml" +
            "\nsampleFileName=" + sampleFileName +
            "\ncharset=" + charset + " colNamesRow=" + columnNamesRow + 
            " firstDataRow=" + firstDataRow +
            " columnSeparator=" + String2.annotatedString(columnSeparator) +
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
        tFileNameRegex = String2.isSomething(tFileNameRegex)? 
            tFileNameRegex.trim() : ".*";
        if (String2.isRemote(tCacheFromUrl)) 
            FileVisitorDNLS.sync(tCacheFromUrl, tFileDir, tFileNameRegex,
                true, ".*", false); //not fullSync
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
        tStandardizeWhat = tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE?
            DEFAULT_STANDARDIZEWHAT : tStandardizeWhat;

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        if (charset == null || charset.length() == 0)
            charset = String2.ISO_8859_1;
        dataSourceTable.readASCII(sampleFileName, charset, 
            "", "", //skipHeaderToRegex, skipLinesRegex, 
            columnNamesRow-1, firstDataRow-1, columnSeparator, 
            null, null, null, null, false);  //simplify
        dataSourceTable.convertIsSomething2(); //convert e.g., "N/A" to ""
        dataSourceTable.simplify();            
        dataSourceTable.standardize(tStandardizeWhat);

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
        double maxTimeES = Double.NaN;
        for (int col = 0; col < dataSourceTable.nColumns(); col++) {
            String colName = dataSourceTable.getColumnName(col);
            PrimitiveArray sourcePA = dataSourceTable.getColumn(col);

            //dateTime?
            Attributes addAtts = new Attributes();
            boolean isDateTime = false;
            if (sourcePA instanceof StringArray) {
                String dtFormat = Calendar2.suggestDateTimeFormat((StringArray)sourcePA, false); //evenIfPurelyNumeric
                if (dtFormat.length() > 0) { 
                    isDateTime = true;
                    addAtts.set("units", dtFormat);
                }

                if (!Double.isFinite(maxTimeES) && Calendar2.isTimeUnits(dtFormat)) 
                    maxTimeES = Calendar2.tryToEpochSeconds(sourcePA.getString(sourcePA.size() - 1)); //NaN if trouble
            }

            Attributes sourceAtts = dataSourceTable.columnAttributes(col);
            PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
            addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                null, //no source global attributes
                sourceAtts, addAtts, colName, 
                destPA.elementType() != PAType.STRING, //tryToAddStandardName
                destPA.elementType() != PAType.STRING, //addColorBarMinMax
                true); //tryToFindLLAT

            //add to dataAddTable
            dataAddTable.addColumn(col, colName, destPA, addAtts);

            //add missing_value and/or _FillValue if needed
            addMvFvAttsIfNeeded(colName, destPA, sourceAtts, addAtts);

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

        //tryToFindLLAT
        tryToFindLLAT(dataSourceTable, dataAddTable);

        //use maxTimeES
        String tTestOutOfDate = EDD.getAddOrSourceAtt(
            dataSourceTable.globalAttributes(), 
            dataAddTable.globalAttributes(), "testOutOfDate", null);
        if (Double.isFinite(maxTimeES) && !String2.isSomething(tTestOutOfDate)) {
            tTestOutOfDate = suggestTestOutOfDate(maxTimeES);
            if (String2.isSomething(tTestOutOfDate))
                dataAddTable.globalAttributes().set("testOutOfDate", tTestOutOfDate);
        }

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
            "<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n" +
            "  below, notably 'units' for each of the dataVariables. -->\n" +
            "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + tFileNameRegex) + 
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            (String2.isUrl(tCacheFromUrl)? 
              "    <cacheFromUrl>" + XML.encodeAsXML(tCacheFromUrl) + "</cacheFromUrl>\n" :
              "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + "</updateEveryNMillis>\n") +  
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
            "    <recursive>true</recursive>\n" +
            "    <pathRegex>.*</pathRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <standardizeWhat>" + tStandardizeWhat + "</standardizeWhat>\n" +
            "    <charset>" + charset + "</charset>\n" +
            "    <columnSeparator>" + XML.encodeAsXML(columnSeparator) + "</columnSeparator>\n" + 
            "    <columnNamesRow>" + columnNamesRow + "</columnNamesRow>\n" +
            "    <firstDataRow>" + firstDataRow + "</firstDataRow>\n" +
            (String2.isSomething(tColumnNameForExtract)?  //Discourage Extract. Encourage sourceName=***fileName,...
              "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
              "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
              "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
              "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" : "") +
            "    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + XML.encodeAsXML(tSortFilesBySourceNames) + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n");
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
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();

        String2.log("\n*** EDDTableFromAsciiFiles.testGenerateDatasetsXml()");
        String dir = EDStatic.unitTestDataDir + "ascii/";

        Attributes externalAddAttributes = new Attributes();
        externalAddAttributes.add("title", "New Title!");
        String suggDatasetID = suggestDatasetID(dir + "31201_2009_NoComments\\.csv");
        String results = generateDatasetsXml(
            dir,  "31201_2009_NoComments\\.csv", "",
            String2.ISO_8859_1, 1, 3, "", -1,
            "", "_.*$", ".*", "stationID",  //just for test purposes; station is already a column in the file
            "time", "station time", 
            "https://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!",
            -1, "", externalAddAttributes) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromAsciiFiles",
            dir,  "31201_2009_NoComments\\.csv", "",
            String2.ISO_8859_1, "1", "3", "", "-1",
            "", "_.*$", ".*", "stationID",  //just for test purposes; station is already a column in the file
            "time", "station time", 
            "https://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!", 
            "-1", ""}, //defaultStandardizeWhat
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n" +
"  below, notably 'units' for each of the dataVariables. -->\n" +
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "ascii/</fileDir>\n" +
"    <fileNameRegex>31201_2009_NoComments\\.csv</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnSeparator></columnSeparator>\n" +
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
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">webmaster.ndbc@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA NDBC</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"institution\">NOAA NDBC</att>\n" +
"        <att name=\"keywords\">altitude, atmosphere, atmospheric, atmp, buoy, center, data, direction, earth, Earth Science &gt; Atmosphere &gt; Altitude &gt; Station Height, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, end, height, identifier, latitude, longitude, national, ndbc, newer, noaa, not, parens, science, speed, station, stationID, surface, temperature, test, test_parens_not_at_end, time, title, water, wind, wind_from_direction, wind_speed, winds, wspd, wtmp</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
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
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss&#39;Z&#39;</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>station</sourceName>\n" +
"        <destinationName>station</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wd</sourceName>\n" +
"        <destinationName>wd</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
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
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
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
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
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
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Water Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wtmp (test dup name and remove parens)</sourceName>\n" +
"        <destinationName>wtmp_2</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Water Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>test (parens) not at end</sourceName>\n" +
"        <destinationName>test_parens_not_at_end</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Test (parens) Not At End</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

        Test.ensureEqual(results, expected, "results=\n" + results);
        //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
        //    expected, "");

        //ensure it is ready-to-use by making a dataset from it
        //2014-12-24 no longer: this will fail with a specific error which is caught below
        String tDatasetID = suggDatasetID;
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), "The Newer Title!", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "stationID, longitude, latitude, altitude, time, station, wd, wspd, atmp, wtmp, wtmp_2, test_parens_not_at_end",
            "");

//2014-12-24 no longer occurs
//            if (msg.indexOf(
//"When a variable's destinationName is \"altitude\", the sourceAttributes or addAttributes " +
//"\"units\" MUST be \"m\" (not \"null\").\n" +
//"If needed, use \"scale_factor\" to convert the source values to meters (positive=up),\n" +
//"use a different destinationName for this variable.") >= 0) {
//                String2.log("EXPECTED ERROR while creating the edd: altitude's units haven't been set.\n");
//            } else 

    }

    /**
     * testGenerateDatasetsXml2 - notably to test reloadEveryNMinutes and testOutOfDate.
     */
    public static void testGenerateDatasetsXml2() throws Throwable {
        testVerboseOn();

        String sourceUrl = "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet.csv?station%2Ctime%2Catmp%2Cwtmp&station=%2241004%22&time%3E=now-1year";
        String destDir = File2.getSystemTempDirectory();
        String destName = "latest41004.csv";
        String2.log("\n*** EDDTableFromAsciiFiles.testGenerateDatasetsXml2()\n" +
            "downloading test file from:\n" + sourceUrl + "\nto: " + destName);        
        SSR.downloadFile(sourceUrl, destDir + destName, true); //tryToUseCompression

        Attributes externalAddAttributes = new Attributes();
        externalAddAttributes.add("title", "New Title!");
        String suggDatasetID = suggestDatasetID(destDir + destName);
        String results = generateDatasetsXml(
            destDir, destName, "",
            String2.ISO_8859_1, 1, 3, "", -1,
            "", "", "", "",
            "", "", 
            "https://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!",
            -1, "", externalAddAttributes) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromAsciiFiles",
            destDir, destName, "",
            String2.ISO_8859_1, "1", "3", "", "-1",
            "", "", "", "",
            "", "", 
            "https://www.ndbc.noaa.gov/", "NOAA NDBC", "The new summary!", "The Newer Title!", 
            "-1", ""}, //defaultStandardizeWhat
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n" +
"  below, notably 'units' for each of the dataVariables. -->\n" +
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"Temp_2e43_7944_fda7\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + destDir + "</fileDir>\n" +
"    <fileNameRegex>latest41004.csv</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnSeparator></columnSeparator>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>3</firstDataRow>\n" +
"    <sortedColumnSourceName>time</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">webmaster.ndbc@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA NDBC</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"institution\">NOAA NDBC</att>\n" +
"        <att name=\"keywords\">atmp, buoy, center, data, identifier, national, ndbc, newer, noaa, station, temperature, time, title, water, wtmp</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"subsetVariables\">station</att>\n" +
"        <att name=\"summary\">The new summary! NOAA National Data Buoy Center (NDBC) data from a local source.</att>\n" +
"        <att name=\"testOutOfDate\">now-1day</att>\n" +
"        <att name=\"title\">The Newer Title!</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>station</sourceName>\n" +
"        <destinationName>station</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station</att>\n" +
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
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss&#39;Z&#39;</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>atmp</sourceName>\n" +
"        <destinationName>atmp</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
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
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
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
        String tDatasetID = suggDatasetID;
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), "The Newer Title!", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "station, time, atmp, wtmp",
            "");

    }

    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXmlWithMV() throws Throwable {
        testVerboseOn();

        String2.log("\n*** EDDTableFromAsciiFiles.testGenerateDatasetsXmlWithMV()");

        Attributes externalAddAttributes = new Attributes();
        externalAddAttributes.add("title", "New Title!");
        String suggDatasetID = suggestDatasetID(
            EDStatic.unitTestDataDir + "ascii/mvTest\\.csv");
        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "ascii/",  "mvTest\\.csv", "",
            String2.ISO_8859_1, 1, 2, "", -1,
            "", "", "", "",  //extract
            "", "", 
            "https://www.bco-dmo.org/", "BCO-DMO", "The new summary!", "The Newer Title!",
            -1, "", externalAddAttributes) + "\n";

String expected = 
"<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n" +
"  below, notably 'units' for each of the dataVariables. -->\n" +
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "ascii/</fileDir>\n" +
"    <fileNameRegex>mvTest\\.csv</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnSeparator></columnSeparator>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <sortedColumnSourceName></sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames></sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">info@bco-dmo.org</att>\n" +
"        <att name=\"creator_name\">BCO-DMO</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://www.bco-dmo.org/</att>\n" +
"        <att name=\"infoUrl\">https://www.bco-dmo.org/</att>\n" +
"        <att name=\"institution\">BCO-DMO</att>\n" +
"        <att name=\"keywords\">aByte, aDouble, aFloat, aFloat_with_NaN, aFloat_with_nd, aLong, anInt, aShort, aString, bco, bco-dmo, biological, byte, chemical, data, dmo, double, float, int, long, management, newer, oceanography, office, short, statistics, string, title, with</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"subsetVariables\">aFloat</att>\n" +
"        <att name=\"summary\">The new summary! Biological and Chemical Oceanography Data Management Office (BCO-DMO) data from a local source.</att>\n" +
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
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aFloat_with_NaN</sourceName>\n" +
"        <destinationName>aFloat_with_NaN</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"long_name\">A Float With Na N</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aFloat_with_nd</sourceName>\n" +
"        <destinationName>aFloat_with_nd</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Float With Nd</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aDouble</sourceName>\n" +
"        <destinationName>aDouble</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Double</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aLong</sourceName>\n" +        
"        <destinationName>aLong</destinationName>\n" +
"        <dataType>long</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"long\">9223372036854775807</att>\n" + //important test of auto-add _FillValue, like addFillValueAttributes does posthoc
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Long</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>anInt</sourceName>\n" +
"        <destinationName>anInt</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" + //important test of auto-add _FillValue, like addFillValueAttributes does posthoc
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">An Int</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aShort</sourceName>\n" +
"        <destinationName>aShort</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" + //important test of auto-add _FillValue, like addFillValueAttributes does posthoc
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Short</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aByte</sourceName>\n" +
"        <destinationName>aByte</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" + //important test of auto-add _FillValue, like addFillValueAttributes does posthoc
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">A Byte</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

        Test.ensureEqual(results, expected, "results=\n" + results);
        //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
        //    expected, "");

        //ensure it is ready-to-use by making a dataset from it
        String tDatasetID = suggDatasetID;
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), "The Newer Title!", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "aString, aFloat, aFloat_with_NaN, aFloat_with_nd, aDouble, aLong, anInt, aShort, aByte",
            "");

    }


    /**
     * This tests the methods in this class with a 1D dataset.
     * This tests skipHeaderToRegex and skipLinesRegex.
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
"    Int16 _FillValue 32767;\n" +
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
"    Int16 _FillValue 32767;\n" +
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
"    String keywords \"Earth Science > Atmosphere > Atmospheric Winds > Surface Winds\";\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station, longitude, latitude, altitude\";\n" +
"    String summary \"The summary.\";\n" +
"    String time_coverage_end \"2006-12-31T23:00:00Z\";\n" +
"    String time_coverage_start \"2005-01-01T00:00:00Z\";\n" +
"    String title \"The Title for testTableAscii\";\n" +
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        //same expected
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species
        userDapQuery = "&time=2005-07-01";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_3", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"longitude,latitude,altitude,time,station,atmp,wtmp\n" +
"degrees_east,degrees_north,m,UTC,,degree_C,degree_C\n" +
"-122.88,37.36,0,2005-07-01T00:00:00Z,46012,12.7,13.4\n" +
"-122.82,37.75,0,2005-07-01T00:00:00Z,46026,12.6,14.6\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /** 
     * This tests some aspects of fixedValue variables and script variables
     * (with and without subsetVariables). */
    public static void testFixedValueAndScripts() throws Throwable {

        String2.log("\n****************** EDDTableFromAsciiFiles.testFixedValueAndScripts() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, dapQuery, tQuery;
        String dir = EDStatic.fullTestCacheDirectory;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        boolean oDebugMode = EDD.debugMode;
        EDD.debugMode = true;

        for (int test = 0; test < 2; test++) {
            //!fixedValue variable is the only subsetVariable
            String id = test == 0? "testWTDLwSV" : "testWTDLwoSV"; //with and without subsetVariables
            deleteCachedDatasetInfo(id);
            EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

            //test getting das for entire dataset
            tName = eddTable.makeNewFileForDapQuery(null, null, "", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fv" + test, ".das"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
"Attributes {\n" +
" s {\n" +
"  ship_call_sign {\n" +
"    String actual_range \"WTDL\n" +
"WTDL\";\n" +
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
"  longitude0360 {\n" +
"    Float32 actual_range 263.2194, 274.2898;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude 0-360°\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -96.78061, -85.71021;\n" +
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
"    String units \"degree_C\";\n" +
"  }\n" +
"  seaTemperatureF {\n" +
"    Float32 _FillValue -99.0;\n" +
"    Float32 actual_range 62.42, 78.62;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 20.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Trajectory\";\n" +
"    String cdm_trajectory_variables \"ship_call_sign\";\n" +
"    String Conventions \"COARDS, CF-1.4, ACDD-1.3\";\n" +
"    String creator_email \"eed.shiptracker@noaa.gov\";\n" +
"    String creator_name \"NOAA OMAO,Ship Tracker\";\n" +
"    Float64 Easternmost_Easting -85.71021;\n" +
"    String featureType \"Trajectory\";\n" +
"    Float64 geospatial_lat_max 30.368;\n" +
"    Float64 geospatial_lat_min 26.6255;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -85.71021;\n" +
"    Float64 geospatial_lon_min -96.78061;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Data downloaded hourly from http://shiptracker.noaa.gov/shiptracker.html to ERD\n" +
    today;
    //        "2013-05-24T17:24:54Z (local files)\n" +
    //"2013-05-24T17:24:54Z http://localhost:8080/cwexperimental/tabledap/testWTDL.das\";\n" +
            Test.ensureEqual(results.substring(0, expected.length()), expected, "test=" + test + " results=\n" + results);

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
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
(test == 0? "    String subsetVariables \"ship_call_sign\";\n" : "") +
"    String summary \"NOAA Ship Pisces Realtime Data updated every hour\";\n" +
"    String time_coverage_end \"2013-05-23T18:04:00Z\";\n" +
"    String time_coverage_start \"2013-04-05T13:18:39Z\";\n" +
"    String title \"NOAA Ship Pisces Underway Meteorological Data, Realtime\";\n" +
"    Float64 Westernmost_Easting -96.78061;\n" +
"  }\n" +
"}\n";
            int po = results.indexOf("String infoUrl");
            Test.ensureEqual(results.substring(po), expected, "test=" + test + " results=\n" + results);

            //test getting some data
            tName = eddTable.makeNewFileForDapQuery(null, null, "&time<2013-04-05T17", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fva" + test, ".csv"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
"ship_call_sign,time,latitude,longitude0360,longitude,seaTemperature,seaTemperatureF\n" +
",UTC,degrees_north,degrees_east,degrees_east,degree_C,degree_F\n" +
"WTDL,2013-04-05T13:18:39Z,30.3679,271.4368,-88.5632,17.1,62.78\n" +
"WTDL,2013-04-05T14:18:40Z,30.3679,271.4368,-88.5632,17.0,62.6\n" +
"WTDL,2013-04-05T15:18:40Z,30.368,271.4368,-88.5632,16.9,62.42\n" +
"WTDL,2013-04-05T16:18:40Z,30.2648,271.4898,-88.51019,17.9,64.22\n";
            Test.ensureEqual(results, expected, "test=" + test + " results=\n" + results);

            //test getting just the fixed value variable
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "ship_call_sign&ship_call_sign!=\"zztop\"", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fvb" + test, ".csv"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
"ship_call_sign\n" +
"\n" +
"WTDL\n";
            Test.ensureEqual(results, expected, "test=" + test + " results=\n" + results);

            //test getting longitude0360 (referenced variable) and longitude
            tName = eddTable.makeNewFileForDapQuery(null, null, "longitude0360,longitude&time<2013-04-05T17", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fva" + test, ".csv"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
"longitude0360,longitude\n" +
"degrees_east,degrees_east\n" +
"271.4368,-88.5632\n" +
"271.4368,-88.5632\n" +
"271.4368,-88.5632\n" +
"271.4898,-88.51019\n";
            Test.ensureEqual(results, expected, "test=" + test + " results=\n" + results);

            //test getting time (different variable) and longitude
            tName = eddTable.makeNewFileForDapQuery(null, null, "time,longitude&time<2013-04-05T17", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fva" + test, ".csv"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
"time,longitude\n" +
"UTC,degrees_east\n" +
"2013-04-05T13:18:39Z,-88.5632\n" +
"2013-04-05T14:18:40Z,-88.5632\n" +
"2013-04-05T15:18:40Z,-88.5632\n" +
"2013-04-05T16:18:40Z,-88.51019\n";
            Test.ensureEqual(results, expected, "test=" + test + " results=\n" + results);

            //test just getting longitude
            tName = eddTable.makeNewFileForDapQuery(null, null, "longitude&time<2013-04-05T17", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fva" + test, ".csv"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
"longitude\n" +
"degrees_east\n" +
"-88.5632\n" +
"-88.5632\n" +
"-88.5632\n" +
"-88.51019\n";
            Test.ensureEqual(results, expected, "test=" + test + " results=\n" + results);

            //test just getting seaTemperatureF
            tName = eddTable.makeNewFileForDapQuery(null, null, "seaTemperatureF&time<2013-04-05T17", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_fva" + test, ".csv"); 
            results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
            expected = 
"seaTemperatureF\n" +
"degree_F\n" +
"62.78\n" +
"62.6\n" +
"62.42\n" +
"64.22\n";
            Test.ensureEqual(results, expected, "test=" + test + " results=\n" + results); 

            //test that using longitude0360 as the x axis still draws a map (not a graph)
            dapQuery = 
"longitude0360%2Clatitude%2CseaTemperatureF&time%3E=2013-05-17T00%3A00%3A00Z&time%3C=2013-05-24T00%3A00%3A00Z&.draw=markers&.marker=5%7C5";
            tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
                dir, eddTable.className() + "_XIsLon0360AndcolorBarTemperatureF_test" + test,  ".png"); 
            SSR.displayInBrowser("file://" + dir + tName);
        }

        EDD.debugMode = oDebugMode;
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

        //display the source file (useful for diagnosing problems below)
        String sourceFile = String2.unitTestDataDir + "csvAscii.txt";
        String2.log("sourceFile=" + sourceFile + ":\n" + 
            String2.directReadFrom88591File(sourceFile));

        //.csv    for all
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_all", ".csv"); 
        results = String2.directReadFrom88591File(testDir + tName);
        //String2.log(results);
        expected = 
"fileName,five,aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n" +
",,,,,,,,,,\n" +
"csvAscii,5.0,\" b,d \",A,1,24,24000,24000000,240000000000,2.4,2.412345678987654\n" +
"csvAscii,5.0,needs,1,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"csvAscii,5.0,fg,F,1,11,12001,1200000,12000000000,1.21,1.0E200\n" +
"csvAscii,5.0,h,H,1,12,12002,120000,1200000000,1.22,2.0E200\n" +
"csvAscii,5.0,i,I,1,13,12003,12000,120000000,1.23,3.0E200\n" +
"csvAscii,5.0,j,J,0,14,12004,1200,12000000,1.24,4.0E200\n" +
"csvAscii,5.0,k,K,0,15,12005,120,1200000,1.25,5.0E200\n" +
"csvAscii,5.0,\"BAD LINE: UNCLOSED QUOTE,K,false,15,12005,120,1200000,1.25,   5.5e200\",,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"csvAscii,5.0,l,L,0,16,12006,12,120000,1.26,6.0E200\n" +
"csvAscii,5.0,m,M,0,17,12007,121,12000,1.27,7.0E200\n" +
"csvAscii,5.0,n,N,1,18,12008,122,1200,1.28,8.0E200\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test getting das for entire dataset
        String2.log("\nEDDTableFromAsciiFiles test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", testDir, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(testDir + tName);
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
"    String actual_range \"1\nN\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Char\";\n" +
"  }\n" +
"  aBoolean {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 0, 1;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Boolean\";\n" +
"  }\n" +
"  aByte {\n" +
"    Byte _FillValue 127;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Byte actual_range 11, 24;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Byte\";\n" +
"  }\n" +
"  aShort {\n" +
"    Int16 _FillValue 32767;\n" +
"    Int16 actual_range 12001, 24000;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"A Short\";\n" +
"  }\n" +
"  anInt {\n" +
"    Int32 _FillValue 2147483647;\n" +
"    Int32 actual_range 12, 24000000;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"An Int\";\n" +
"  }\n" +
"  aLong {\n" +
"    Float64 _FillValue 9223372036854775807;\n" +
"    Float64 actual_range 1200, 240000000000;\n" + //long values written as float64 because no longs in nc3
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
"    String creator_url \"https://www.ndbc.noaa.gov/\";\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//"2014-12-04T19:15:21Z (local files)
//2014-12-04T19:15:21Z http://localhost:8080/cwexperimental/tabledap/testTableAscii.das";
expected =
"    String infoUrl \"https://www.ndbc.noaa.gov/\";\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
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
        results = String2.directReadFrom88591File(testDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String fileName;\n" +
"    Float32 five;\n" +
"    String aString;\n" +
"    String aChar;\n" +
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
        results = String2.directReadFrom88591File(testDir + tName);
        expected = 
"fileName,five\n" +
",\n" +
"csvAscii,5.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //subset of variables, constrain boolean and five
        userDapQuery = "anInt,fileName,five,aBoolean&aBoolean=1&five=5";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, testDir, 
            eddTable.className() + "_conbool", ".csv"); 
        results = String2.directReadFrom88591File(testDir + tName);
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
     * from the main entity (or for all of the children) in an inport.xml file.
     * This will not throw an exception.
     * 2017-08-09 I switched from old /inport/ to new /inport-xml/ .
     *
     * @param xmlFileName the URL or full file name of an InPort XML file.
     *   If it's a URL, it will be stored in tInputXmlDir.
     * @param tInputXmlDir The directory that is/will be used to store the input-xml file.
     *   If specified and if it doesn't exist, it will be created.
     * @return content for datasets.xml.
     *   Error messages will be included as comments.
     */
    public static String generateDatasetsXmlFromInPort(String xmlFileName, 
        String tInputXmlDir, String typeRegex, int tStandardizeWhat) {

        String main = null;
        try {
            main = generateDatasetsXmlFromInPort(xmlFileName, 
                tInputXmlDir, typeRegex, 0, "", "", tStandardizeWhat) + "\n";
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            return "<!-- " + String2.replaceAll(msg, "--", "- - ") + " -->\n\n";
        }

        StringBuilder children = new StringBuilder();
        for (int whichChild = 1; whichChild < 10000; whichChild++) {
            try {
                children.append(generateDatasetsXmlFromInPort(xmlFileName, 
                    tInputXmlDir, typeRegex, whichChild, "", "", tStandardizeWhat) + "\n");
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t);
                if (msg.indexOf("ERROR: whichChild=") < 0 ||
                    msg.indexOf(" not found as ") < 0)
                     msg = "";
                else msg = "<!-- " + String2.replaceAll(msg, "--", "- - ") + " -->\n\n";
                return whichChild > 1?
                    children.toString() + msg :
                    main                + msg;
            }
        }
        return children.toString();
    }

    public static String convertInportTimeToIso8601(String time) {
        //Field: start-date-time (just below this:
        //https://inport.nmfs.noaa.gov/inport/help/xml-loader#time-frames )
        //Says for time: "The value must be specified in the following format: YYYYMMDDTHHMMSS.FFFZ"
        //"A time zone component is optional. If no time zone is provided, UTC is assumed."
        time = Calendar2.expandCompactDateTime(time);
        //now e.g., 2017-08-23T00:00:00.000  (23 chars)
        if (time.length() >= 13 && //has hour value
            time.charAt(10) == 'T' && 
            time.charAt(time.length() - 1) != 'Z' && //no Z
            time.substring(11).indexOf('-') < 0 && //no trailing e.g., -05:00 time zone after T
            time.substring(11).indexOf('+') < 0 && //no trailing e.g., +05:00 time zone after T
            time.length() <= 23) //not longer than example above
            time += "Z";
        return time;
    }

    /** 
     * This generates a chunk of datasets.xml for one ERDDAP dataset
     * from the info for the main info (or one of the children) in an inport.xml file.
     * 2017: The inport-xml loader documentation (which is related by not identical to inport-xml) is at
     *   https://inport.nmfs.noaa.gov/inport/help/xml-loader#inport-metadata
     * Because the &lt;downloads&gt;&lt;download&gt;&lt;/download-url&gt;'s 
     *  are unreliable even when present, this just notes the URL in 
     *  &gt;sourceUrl&lt;, but doesn't attempt to download the file.
     *
     * @param xmlFileName the URL or full file name of an InPort XML file.
     *   If it's a URL, it will be stored in tInputXmlDir.
     * @param tInputXmlDir The directory that is/will be used to store the input-xml file.
     *   If specified and if it doesn't exist, it will be created.
     * @param typeRegex e.g., "(Entity|Data Set)". 
     *   Other types are rarer and not useful for ERDDAP:
     *     Document, Procedure, Project.
     * @param whichChild  
     *   If whichChild=0, this will create a dataVariables for columns
     *     described by the high-level data-attributes (if any).
     *   If whichChild is &gt;0, this method will include dataVariable definitions
     *      from the specified entity-attribute, distribution, and/or child-item
     *      (1, 2, 3, ...).
     *   IF &gt;1 IS USED, THIS ASSUMES THAT THEY ARE DEFINED IN PARALLEL!
     *   If the specified whichChild doesn't exist, this will throw a RuntimeException.
     *   In all cases, the "sourceUrl" will specify the URL from where the data 
     *      can be downloaded (if specified in the XML).
     * @param tBaseDataDir the base directory, to which item-id/ will be added.
     *   It that dir doesn't exist, it will be created.
     *   The dataFile, if specified, should be in that directory.
     * @param tDataFileName The name.ext of the data file name for this child, if known. 
     *   It is okay if there is no entity-attribute info for this child.
     *   
     */
    public static String generateDatasetsXmlFromInPort(String xmlFileName, 
        String tInputXmlDir, String typeRegex, int whichChild, 
        String tBaseDataDir, String tDataFileName, int tStandardizeWhat) throws Throwable {

        String2.log("\n*** inPortGenerateDatasetsXml(" + xmlFileName + 
            ", whichChild=" + whichChild + ")");

        //check parameters
        Test.ensureTrue(whichChild >= 0, "whichChild must be >=0."); 
        //whichChild can be found as an entity-attribute, distribution, and/or child-item
        boolean whichChildFound = false;  

        tStandardizeWhat = tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE?
            DEFAULT_STANDARDIZEWHAT : tStandardizeWhat;

        if (String2.isSomething(tBaseDataDir)) {
            tBaseDataDir = File2.addSlash(File2.forwardSlashDir(tBaseDataDir));
            File2.makeDirectory(tBaseDataDir);
        }
        String tDataDir = null; //it should be set below

        //make tInputXmlDir
        if (String2.isSomething(tInputXmlDir)) 
             File2.makeDirectory(tInputXmlDir);
        else tInputXmlDir = "";

        //if xmlFileName is a URL, download it
        if (String2.isRemote(xmlFileName)) {
            if (tInputXmlDir.equals(""))
                throw new RuntimeException(
                    "When the xmlFileName is a URL, you must specify the tInputXmlDir to store it in.");
            String destName = tInputXmlDir + File2.getNameAndExtension(xmlFileName);
            SSR.downloadFile(xmlFileName, destName, true); //tryToUseCompression            
            String2.log("xmlFile saved as " + destName);
            xmlFileName = destName;
        }

        
        { //display what's in the .xml file
            String readXml[] = String2.readFromFile(xmlFileName, String2.UTF_8, 1);
            if (readXml[0].length() > 0)
                throw new RuntimeException(readXml[0]);
            if (whichChild == 0) {
                String2.log("Here's what is in the InPort .xml file:");
                String2.log(readXml[1]);
            }
        }

        int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        String catID = File2.getNameNoExtension(xmlFileName);
        if (!String2.isSomething(tInputXmlDir))
            tInputXmlDir = "???";
        if (!String2.isSomething(tDataFileName))
            tDataFileName = "???";

        //create tables to hold results
        Table sourceTable = new Table();
        Attributes sourceAtts = sourceTable.globalAttributes();
        boolean isCreator2 = false, isCreator3 = false;
        String creatorName2 = null,  creatorName3 = null;
        String creatorEmail2 = null, creatorEmail3 = null;
        String creatorOrg2 = null,   creatorOrg3 = null;
        String creatorUrl2 = null,   creatorUrl3 = null;;
        String metaCreatedBy = "???";
        String metaCreated = "???";
        String metaLastModBy = "???";
        String metaLastMod = "???";
        String acronym = "???";  //institution acronym
        String title = "???";  
        String securityClass = "";
        //accumulate results from some tags
        StringBuilder background = new StringBuilder();
        int nEntities = 0;
        String entityID = null;
        String entityPre = "";
        int nChildItems = 0;
        StringBuilder childItems = new StringBuilder();
        String childItemsPre = "";
        StringBuilder dataQuality = new StringBuilder();
        String dataQualityPre = "InPort_data_quality_";
        StringBuilder dataset = new StringBuilder();
        int nDistributions = 0;
        StringBuilder distribution = new StringBuilder();
        String distPre = "";
        //String distID = null;
        String distUrl = null;
        String distName = null;
        String distType = null;
        String distStatus = null;
        int nFaqs = 0;
        StringBuilder faqs = new StringBuilder();
        String faqsPre = "";
        StringBuilder history = new StringBuilder();
        int nIssues = 0;
        StringBuilder issues = new StringBuilder();
        String issuesPre = "";
        HashSet<String> keywords = new HashSet();
        StringBuilder license = new StringBuilder();
        int lineageSourceN = 0;
        String lineageStepN = "", lineageName = "", lineageEmail = "", lineageDescription = "";
        String sep = ","; //a common separator between items on a line
        String tSourceUrl = "(local files)";
        StringBuilder summary = new StringBuilder();
        int nSupportRoles = 0;
        String supportRolesPre = "";
        int nUrls = 0;
        String pendingUrl = "?";
        StringBuilder urls = new StringBuilder();
        String urlsPre = "";

        //HashMap<String,String> child0RelationHM = new HashMap(); //for whichChild=0
        //child0RelationHM.put("HI", "child");
        //child0RelationHM.put("RI", "other");

        //HashMap<String,String> childNRelationHM = new HashMap(); //for whichChild>0
        //childNRelationHM.put("HI", "sibling");
        //childNRelationHM.put("RI", "other");

        //attributes that InPort doesn't help with
        Attributes gAddAtts = new Attributes();
        String inportXmlUrl = "https://inport.nmfs.noaa.gov/inport-metadata/" +
            xmlFileName.substring("/u00/data/points/inportXml/".length()); 
        gAddAtts.add("cdm_data_type", "Other");
        gAddAtts.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
        gAddAtts.add("infoUrl", inportXmlUrl); 
        gAddAtts.add("InPort_xml_url", inportXmlUrl);
        gAddAtts.add("keywords_vocabulary", "GCMD Science Keywords");
        gAddAtts.add("standard_name_vocabulary", "CF Standard Name Table v55");

        //process the inport.xml file 
        SimpleXMLReader xmlReader = new SimpleXMLReader(
            File2.getDecompressedBufferedInputStream(xmlFileName));
        xmlReader.nextTag();
        String tags = xmlReader.allTags();
        String startTag = "<inport-metadata>";  //2017-08-09 version 0.9
        Test.ensureEqual(tags, startTag, 
            "Unexpected first tag"); 
        int startTagLength = startTag.length();

        //process the tags
        while (true) {
            xmlReader.nextTag();
            tags = xmlReader.allTags();
            int nTags = xmlReader.stackSize();
            String content = xmlReader.content();
            if (xmlReader.stackSize() == 1) 
                break; //the startTag
            String topTag = xmlReader.tag(nTags - 1);
            boolean hasContent = String2.isSomething2(content);
            tags = tags.substring(startTagLength);
            if (debugMode) String2.log(">>  tags=" + tags + content);
            String attTags = 
                tags.startsWith("<entity-attribute-information><entity><data-attributes><data-attribute>")?
                    tags.substring(71) : null;
 
            //special cases: convert some InPort names to ERDDAP names
            //The order here matches the order in the files.

            //item-identification
            if (tags.startsWith("<item-identification>")) {
                if (tags.endsWith(       "</catalog-item-id>")) {                
                    Test.ensureEqual(content, catID, "catalog-item-id != fileName");
                    gAddAtts.add(           "InPort_item_id", content);
                    if (String2.isSomething(tBaseDataDir))
                        tDataDir = tBaseDataDir + content + "/";

                } else if (tags.endsWith("</title>") && hasContent) {
                    title = content;
                //</short-name>
                } else if (tags.endsWith("</catalog-item-type>") && hasContent) {  
                    //tally: Entity: 4811, Data Set: 2065, Document: 168, 
                    //  Procedure: 113, Project: 29
                    if (!content.matches(typeRegex)) {
                        String2.log(String2.ERROR + ": Skipping this item because " +
                            "the catalog-item-type doesn't match the typeRegex.");
                        return "";
                    } 
                    gAddAtts.add(           "InPort_item_type", content);  //e.g., Data Set
                } else if (tags.endsWith("</metadata-workflow-state>") && hasContent) {
                    //this may be overwritten by child below
                    gAddAtts.add(   "InPort_metadata_workflow_state", content); //e.g., Published / External
                } else if (tags.endsWith("</parent-catalog-item-id>") && hasContent) {
                    gAddAtts.add(           "InPort_parent_item_id", content);
                } else if (tags.endsWith("</parent-title>") && hasContent) {
                    //parent-title is useful as precursor to title
                    title = content + (title.endsWith("???")? "" : ", " + title);
                } else if (tags.endsWith("</status>") && hasContent) {
                    //this may be overwritten by child below
                    gAddAtts.add(   "InPort_status", content); //e.g., Complete
                } else if (tags.endsWith("</abstract>") && hasContent) {
                    String2.ifSomethingConcat(summary, "\n\n", content); 
                } else if (tags.endsWith("</purpose>") && hasContent) {
                    String2.ifSomethingConcat(summary, "\n\n", content); 
                //</notes>  inport editing notes
                }

            //keywords see 10657
            } else if (tags.equals("<keywords><keyword></keyword>") && hasContent) {
                chopUpCsvAddAllAndParts(content, keywords);

            //physical-location  
            //   <physical-location>
            //      <organization>National Marine Mammal Laboratory</organization>
            //      <city>Seattle</city>
            //      <state-province>WA</state-province>
            //      <country>United States</country>
            //   </physical-location>
            //???            } else if (tags.equals("<physical-location></organization>") && hasContent) {
            //   not really useful.  use as institution?!

            //data-set-information  
            //  see /u00/data/points/inportXml/NOAA/NMFS/AFSC/inport-xml/xml/17275.xml
            } else if (tags.startsWith("<data-set-information>")) {
                if (tags.endsWith("</data-set-type>") && hasContent) {
                    //Tally: Database: 176, CSV Files: 128, Oracle Database: 98,
                    //  Mixed: 79, MS Excel Spreadsheet: 76, Files: 73,  Other: 47,
                    //  GIS: 22, Access Database, spreadsheets: 21, SAS files: 19,
                    //  Binary: 18, MS Access Database: 18, MS Excel : 15, JPG Files: 12,
                    //  GIS dataset of raster files: 11, Excel and SAS Dataset: 10,
                    //  Files (Word, Excel, PDF, etc.): 7, Website (url): 7,
                    //  Excel, SAS, and Stata data sets: 5, Text files: 5, GIS database: 4,
                    //  SAS data sets (version 7): 4, SQL Server Database: 4 ...
                      background.append("> data-set type=" + content + "\n");
                    gAddAtts.add("InPort_dataset_type", content);
                } else if (tags.endsWith(      "</maintenance-frequency>") && hasContent) {
                     background.append("> data-set maintenance-frequency=" + content + "\n");
                  gAddAtts.add(   "InPort_dataset_maintenance_frequency", content);
                } else if (tags.endsWith("</data-set-publication-status>") && hasContent) {
                        background.append("> data-set publication-status=" + content + "\n");
                    gAddAtts.add(    "InPort_dataset_publication_status", content);
                } else if (tags.endsWith(      "</publish-date>") && hasContent) {
                    content = convertInportTimeToIso8601(content);
                     background.append("> data-set publish-date=" + content + "\n");
                    gAddAtts.add( "InPort_dataset_publish_date", content);
                } else if (tags.endsWith( "</data-presentation-form>") && hasContent) {
                     background.append("> data-set presentation-form=" + content + "\n"); //e.g., Table (digital)           
                    gAddAtts.add( "InPort_dataset_presentation_form", content);
                } else if (tags.endsWith(     "</source-media-type>") && hasContent) {
                    //Tally: online: 146, disc: 101, electronic mail system: 59,
                    //  electronically logged: 48, computer program: 38, paper: 29,
                    //  CD-ROM: 11, physical model: 3, chart: 2, videotape: 2, audiocassette: 1
                    background.append("> data-set source-media-type=" + content + "\n");
                   gAddAtts.add( "InPort_dataset_source_media_type", content);
                } else if (tags.endsWith("</distribution-liability>") && hasContent) {
                            license.append("Distribution Liability: " + content + "\n"); 
                } else if (tags.endsWith("</data-set-credit>") && hasContent) {
                    gAddAtts.add("acknowledgment", content); //e.g., BOEM funded this research.
                } else if (tags.endsWith("</instrument>") && hasContent) {
                              gAddAtts.add("instrument", content); 
                } else if (tags.endsWith("</platform>") && hasContent) {
                              gAddAtts.add("platform", content); 
                } else if (tags.endsWith("</physical-collection-fishing-gear>") && hasContent) {
                                           gAddAtts.add("InPort_fishing_gear", content); 
                }

            //entity-attribute-information  
            //  see /u00/data/points/inportXml/NOAA/NMFS/AFSC/inport-xml/xml/36615.xml
            } else if (tags.startsWith("<entity-attribute-information><entity>")) {

                if (tags.equals("<entity-attribute-information><entity>")) {
                    nEntities++;
                    if (reallyVerbose)
                        String2.log("Since whichChild=" + whichChild + 
                        ", I'm " + 
                        (whichChild == 0 || nEntities == whichChild? "processing" : "skipping") +
                        " <entity-attribute-information> for entity #" + nEntities);
                    if (whichChild == 0) {
                        entityPre = "InPort_entity_" + nEntities + "_";
                    } else if (nEntities == whichChild) {
                        whichChildFound = true;
                    } else {
                        //skip this child
                        xmlReader.skipToStackSize(xmlReader.stackSize());
                    }

                } else if (attTags != null && whichChild > 0 && nEntities == whichChild) {                    
                    //atts has tags after
                    //  <entity-attribute-information><entity><data-attributes><data-attribute>
                    //String2.log(">>attTags=" + attTags);
                    int col = sourceTable.nColumns() - 1;  //0.. for actual columns
                    Attributes varAddAtts = col >= 0? sourceTable.columnAttributes(col) : null;
                    if (attTags.equals("")) {
                        //the start: add the column
                        varAddAtts = new Attributes();
                        col++;  //0..
                        String tName = "column" + col; //a placeholder
                        sourceTable.addColumn(col, tName, new StringArray(), varAddAtts);

                    } else if (attTags.equals("</name>") && hasContent) {
                        sourceTable.setColumnName(col, content);                    

                    } else if (attTags.equals("</data-storage-type>") && hasContent) {
                        //not reliable or useful. Use simplify.

                    } else if (attTags.equals("</null-value-meaning>") && hasContent) {
                        //??? Does this identify the null value (e.g., -999)
                        //    or describe what is meant if there is no value???
                        //from Tally:     0: 53, YES: 37, blank: 36, space or 0: 34,
                        //  blank or 0: 31, null or: 30, Yes: 20, NO: 19, NA: 18,
                        //  blank or -, space/null: 15, ...
                        double imv = String2.parseInt(content);
                        double dmv = String2.parseDouble(content);
                        //if numeric, only 0 or other numeric values matter
                        if (content.endsWith(" or 0"))
                            varAddAtts.set("missing_value", 0);
                        else if (imv < Integer.MAX_VALUE)
                            varAddAtts.set("missing_value", imv);
                        else if (Double.isFinite(dmv))
                            varAddAtts.set("missing_value", dmv);
                        //for strings, it doesn't really matter
                        //else if (content.indexOf("NA")   >= 0) varAddAtts.set("missing_value", "NA");
                        //else if (content.indexOf("NULL") >= 0) varAddAtts.set("missing_value", "NULL");
                        //else if (content.indexOf("null") >= 0) varAddAtts.set("missing_value", "null");

                    //} else if (attTags.equals("</scale>") && hasContent) { 
                    //    //What is this? It isn't scale_factor. 
                    //    //from Tally: 0, 2, 1, 3, 5, 6, 9, 14, 8, 13, 12, 15, 9, ...
                    //    varAddAtts.set("scale", content);

                    } else if (attTags.equals("</max-length>") && hasContent) {
                        //e.g., -1 (?!), 0(?!), 22, 1, 8, 100, 4000 (longest)
                        int maxLen = String2.parseInt(content);
                        if (maxLen > 0 && maxLen < 10000)
                            varAddAtts.add("max_length", "" + maxLen);

                    //} else if (attTags.equals("</is-pkey>") && hasContent) {
                    //I think this loses its meaning in ERDDAP.
                    //    varAddAtts.set("isPrimaryKey", content);

                    } else if (attTags.equals("</units>") && hasContent) {
                        //e.g., Decimal degrees, animal, KM, degrees celcius(sic), AlphaNumeric
                        //<units>micromoles per kilogram</units>
                        //These will be fixed up by makeReadyToUseAddVariableAttributes.
                        varAddAtts.set("units", content);

                    } else if (attTags.equals("</format-mask>") && hasContent) {
                        //Thankfully, format-mask appears after units, so format-mask has precedence.
                        //e.g., $999,999.99, MM/DD/YYYY, HH:MM:SS, mm/dd/yyyy, HHMM

                        //if it's a date time format, convertToJavaDateTimeFormat e.g., yyyy-MM-dd'T'HH:mm:ssZ
                        String newContent = Calendar2.convertToJavaDateTimeFormat(content);
                        if (!newContent.equals(content) ||  //it was changed, so it is a dateTime format
                            Calendar2.isStringTimeUnits(newContent)) {

                            //These will be fixed up by makeReadyToUseAddVariableAttributes.
                            varAddAtts.set("units", content); 

                            if (newContent.indexOf("yyyy") >= 0 && //has years
                                newContent.indexOf("M") >= 0 && //has month
                                newContent.indexOf("d") >= 0 && //has days
                                newContent.indexOf("H") <  0)   //doesn't have hours
                                varAddAtts.set("time_precision", "1970-01-01");
                        } else {
                            varAddAtts.set("format_mask", content);
                        }

                    } else if (attTags.equals("</description>") && hasContent) {
                        //description -> comment
                        //widely used  (Is <description> used another way?)
                        if (content.toLowerCase().equals("month/day/year")) {  //date format
                             varAddAtts.set("units", "M/d/yyyy");
                             varAddAtts.set("time_precision", "1970-01-01");
                        } else {
                            varAddAtts.set("comment", content);
                        }

                    } else if (attTags.equals("</allowed-values>") && hasContent) {
                        //e.g., No domain defined., unknown, Free entry text field., text, "1, 2, 3", "False, True"
                        varAddAtts.set("allowed_values", content);

                    } else if (attTags.equals("</derivation>") && hasContent) {
                        //there are some
                        varAddAtts.set("derivation", content);

                    } else if (attTags.equals("</validation-rules>") && hasContent) {
                        //there are some
                        varAddAtts.set("validation_rules",  content);

                    }

                //*after* attTags processing, get <entity-attribute-information><entity></...> info
                } else if (xmlReader.stackSize() == 4) {
                    if (tags.endsWith(     "</catalog-item-id>") && hasContent) {
                        if (whichChild == 0) {
                            gAddAtts.add(entityPre + "item_id", content); 
                            background.append("> entity #" + nEntities + 
                                " catalog-item-id=" + content + "\n");
                        } else if (nEntities == whichChild) {
                            entityID = content;
                        }

                    } else if (tags.endsWith(      "</title>") && hasContent) {
                        if (whichChild == 0)
                            gAddAtts.add(entityPre + "title", content); 
                        else if (nEntities == whichChild)
                             title += ", " + content;

                    } else if (tags.endsWith(      "</metadata-workflow-state>") && hasContent) {
                        //overwrite parent info
                        if (whichChild == 0)
                            gAddAtts.add(entityPre + "metadata_workflow_state", content); 
                        else if (nEntities == whichChild)
                             gAddAtts.add(    "InPort_metadata_workflow_state", content); //e.g., Published / External

                    } else if (tags.endsWith(      "</status>") && hasContent) {
                        //overwrite parent info
                        if (whichChild == 0)
                            gAddAtts.add(entityPre + "status", content); 
                        else if (nEntities == whichChild)
                             gAddAtts.add(    "InPort_status", content); //e.g., Complete

                    } else if (tags.endsWith(      "</abstract>") && hasContent) {
                        if (whichChild == 0)
                            gAddAtts.add(entityPre + "abstract", content); 
                        else if (nEntities == whichChild)
                            String2.ifSomethingConcat(summary, "\n\n", 
                                "This sub-dataset has: " + content);

                    //<notes> is InPort info, e.g., when/how uploaded         
                    }


                }  
                //skip <entity-information><entity-type>Spreadsheet
                //skip <entity-information><description>...   same/similar to abstract


            //support-roles 
            //Use role=Originator as backup for creator_name, creator_email
            } else if (tags.startsWith("<support-roles>")) {
                if (tags.equals(       "<support-roles>")) { //opening tag
                    isCreator2 = false;
                    isCreator3 = false;
                    nSupportRoles++;
                    supportRolesPre = "InPort_support_role_" + nSupportRoles + "_";
                } else if (tags.equals("<support-roles><support-role></support-role-type>") && hasContent) {
                    isCreator2 = "Originator".equals(content);        //often e.g., organization e.g., AFSC
                    isCreator3 = "Point of Contact".equals(content);  //often a person
                    gAddAtts.add(                                supportRolesPre + "type", content);
                } else if (tags.equals("<support-roles><support-role></person>") && hasContent) {
                    int po = content.indexOf(", ");  //e.g., Clapham, Phillip
                    if (po > 0)   
                        content = content.substring(po + 2) + " " + content.substring(0, po); 
                    if      (isCreator2) creatorName2 = content;
                    else if (isCreator3) creatorName3 = content;                        
                    gAddAtts.add(                   supportRolesPre + "person", content);
                } else if (tags.equals("<support-roles><support-role></person-email>") && hasContent) {
                    if      (isCreator2) creatorEmail2 = content;
                    else if (isCreator3) creatorEmail3 = content;
                    gAddAtts.add(                   supportRolesPre + "person_email", content);
                } else if (tags.equals("<support-roles><support-role></organization>") && hasContent) {
                    if      (isCreator2) creatorOrg2 = content;
                    else if (isCreator3) creatorOrg3 = content;
                    gAddAtts.add(                   supportRolesPre + "organization", content);
                } else if (tags.equals("<support-roles><support-role></organization-url>") && hasContent) {
                    if      (isCreator2) creatorUrl2 = content;
                    else if (isCreator3) creatorUrl3 = content;
                    gAddAtts.add(                   supportRolesPre + "organization_url", content);
                }

            //extent  geo
            } else if (tags.startsWith("<extents><extent><geographic-areas><geographic-area>")) {
                if (tags.endsWith("</west-bound>") && hasContent) 
                    gAddAtts.add("geospatial_lon_min", String2.parseDouble(content)); 
                else if (tags.endsWith("</east-bound>") && hasContent) 
                    gAddAtts.add("geospatial_lon_max", String2.parseDouble(content)); 
                else if (tags.endsWith("</north-bound>") && hasContent) 
                    gAddAtts.add("geospatial_lat_max", String2.parseDouble(content)); 
                else if (tags.endsWith("</south-bound>") && hasContent) 
                    gAddAtts.add("geospatial_lat_min", String2.parseDouble(content)); 

            //extent time-frame
            } else if (tags.startsWith("<extents><extent><time-frames><time-frame>")) {
                if (tags.endsWith("</start-date-time>") && hasContent) {
                    gAddAtts.add("time_coverage_begin", 
                        convertInportTimeToIso8601(content));  
                } else if (tags.endsWith("</end-date-time>") && hasContent) {
                    gAddAtts.add("time_coverage_end", 
                        convertInportTimeToIso8601(content)); 
                }

            //access-information    
            } else if (tags.startsWith("<access-information>")) {
                if        (tags.endsWith("</security-class") && hasContent) {
                            license.append("Security class: " + content + "\n"); 
                       gAddAtts.add("InPort_security_class", content);
                                            securityClass = content;
                } else if (tags.endsWith("</security-classification") && hasContent) {
                            license.append("Security classification: " + content + "\n"); 
                       gAddAtts.add("InPort_security_classification", content);
                } else if (tags.endsWith("</security-handling-description") && hasContent) {
                            license.append("Security handling description: " + content + "\n"); 
                } else if (tags.endsWith("</data-access-policy>") && hasContent) {
                            license.append("Data access policy: " + content + "\n"); 
                } else if (tags.endsWith("</data-access-procedure>") && hasContent) {
                            license.append("Data access procedure: " + content + "\n"); 
                } else if (tags.endsWith("</data-access-constraints>") && hasContent) {
                            license.append("Data access constraints: " + content + "\n"); 
                } else if (tags.endsWith("</data-use-constraints>") && hasContent) {
                            license.append("Data use constraints: " + content + "\n"); 
                } else if (tags.endsWith("</security-classification-system>") && hasContent) {
                            license.append("Security classification system: " + content + "\n"); //all kinds of content and e.g., None
                } else if (tags.endsWith("</metadata-access-constraints>") && hasContent) {
                            license.append("Metadata access constraints: " + content + "\n"); 
                } else if (tags.endsWith("</metadata-use-constraints>") && hasContent) {
                            license.append("Metadata use constraints: " + content + "\n"); 
                } 

            //distribution-information  
            //  see /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            } else if (tags.startsWith("<distribution-information>")) {
                if (tags.endsWith("<distribution>")) {  
                    //start of a distribution
                    nDistributions++;

                    if (reallyVerbose)
                        String2.log("Since whichChild=" + whichChild + 
                        ", I'm " + 
                        (whichChild == 0 || nDistributions == whichChild? "processing" : "skipping") +
                        " <distribution-information> for entity #" + nDistributions);
                    if (whichChild == 0 || nDistributions == whichChild) {
                        if (whichChild > 0) whichChildFound = true;
                        distPre = "InPort_distribution_" + 
                            (whichChild > 0? "" : nDistributions + "_"); 
                        //distID = xmlReader.attributeValue("cc-id"); //skip cc-id: it's an internalDB identifier
                        distUrl = null;
                        distName = null;
                        distType = null;
                        distStatus = null;
                        //gAddAtts.add(distPre + "cc_id", distID);
                    } else {
                        //skip this child
                        xmlReader.skipToStackSize(xmlReader.stackSize());
                    }
                } else if (tags.endsWith("</download-url>") && hasContent) {
                    distUrl = content;
                    if (nDistributions == whichChild)
                        tSourceUrl = content;
                    gAddAtts.add(distPre + "download_url", content);

                } else if (tags.endsWith("</file-name>") && hasContent) {
                    distName = content;
                    gAddAtts.add(distPre + "file_name", content);
                } else if (tags.endsWith("</file-type>") && hasContent) {
                    distType = content;
                    gAddAtts.add(distPre + "file_type", content);
                //seip fgdc-content-type, file-size (in MB?)
                } else if (tags.endsWith("</review-status>") && hasContent) {
                    distStatus = content;
                    gAddAtts.add(distPre + "review_status", content);
                } else if (tags.endsWith("</distribution>") && distUrl != null) {  
                    //end of a distribution
                    String msg = "Distribution" + // " cc-id=" + distID +
                        (distName   == null? "": sep + " file-name="     + distName) +
                        (distType   == null? "": sep + " file-type="     + distType) +
                        (distStatus == null? "": sep + " review-status=" + distStatus) +
                                                 sep + " download-url="  + distUrl + "\n";
                    distribution.append(msg);
                    background.append("> #" + nDistributions + ": " + msg);
                }


            //urls  
            //  see /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            //   <urls>
            //      <url cc-id="223838">
            //         <url>https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer</url>
            //         <url-type>Online Resource</url-type>
            //         <description>REST Service</description>
            //      </url>
            //   </urls>
            } else if (tags.startsWith("<urls>")) {
                if        (tags.equals("<urls><url>")) {
                            urls.append("URL #" + ++nUrls);
                    //skip cc-id: it's an internal DB identifier
                    pendingUrl = "?";
                    urlsPre = "InPort_url_" + nUrls + "_";
                } else if (tags.equals("<urls><url></url>") && hasContent) {
                    pendingUrl = content;
                    gAddAtts.add(         urlsPre + "url", content);
                } else if (tags.equals("<urls><url></url-type>") && hasContent) {
                    urls.append(                 sep + " type=" + content);
                    gAddAtts.add(             urlsPre + "type", content);
                } else if (tags.equals("<urls><url></description>") && hasContent) {
                    urls.append(             sep + " description=" + content);
                    gAddAtts.add(         urlsPre + "description", content);
                } else if (tags.equals("<urls></url>")) {
                    urls.append(sep + " url=" + pendingUrl + "\n");
                }

            //activity-logs  /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            //just inport activity?

            //issues see /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            //   <issues>
            //      <issue cc-id="223840">
            //         <issue-date>2013</issue-date>
            //         <author>Lewis, Steve</author>
            //         <issue>Outlier removal processes</issue>
            //      </issue>
            //   </issues>
            } else if (tags.startsWith("<issues><")) {
                if        (tags.equals("<issues><issue>")) {
                                  issues.append("Issue #" + ++nIssues);
                             issuesPre = "InPort_issue_" + nIssues + "_";
                             //skip cc-id: it's an internal DB identifier
                } else if (tags.equals("<issues><issue></issue-date>") && hasContent) {
                    content = convertInportTimeToIso8601(content);
                    issues.append(                          ": date=" + content);
                    gAddAtts.add(                 issuesPre + "date", content);
                } else if (tags.equals("<issues><issue></author>") && hasContent) {
                    int po = content.indexOf(", ");  //e.g., Clapham, Phillip
                    if (po > 0)   
                        content = content.substring(po + 2) + " " + content.substring(0, po); 
                                        issues.append(", author=" + content);
                    gAddAtts.add(           issuesPre + "author", content);
                } else if (tags.equals("<issues><issue></issue>") && hasContent) {
                    issues.append(               sep + " issue=" + content);
                    gAddAtts.add(           issuesPre + "issue", content);
                } else if (tags.equals("<issues></issue>")) {
                    String2.addNewlineIfNone(issues);
                }

            //technical-environment    /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            //   <technical-environment>
            //      <description>In progress.</description>
            //   </technical-environment>
            } else if (tags.equals("<technical-environment></description>") && hasContent) {
                gAddAtts.add("InPort_technical_environment", content);

            //data-quality   /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            } else if (tags.startsWith("<data-quality>")) {
                if        (tags.endsWith(       "</representativeness>") && hasContent) {
                      dataQuality.append(       "* Representativeness: " + content + "\n");
                    gAddAtts.add(dataQualityPre + "representativeness", content);
                } else if (tags.endsWith(       "</accuracy>") && hasContent) {
                      dataQuality.append(       "* Accuracy: " + content + "\n");
                    gAddAtts.add(dataQualityPre + "accuracy", content);
                } else if (tags.endsWith(       "</analytical-accuracy>") && hasContent) {
                      dataQuality.append(       "* Analytical-accuracy: " + content + "\n");
                    gAddAtts.add(dataQualityPre + "analytical_accuracy", content);
                } else if (tags.endsWith(       "</completeness-measure>") && hasContent) {
                      dataQuality.append(       "* Completeness-measure: " + content + "\n");
                    gAddAtts.add(dataQualityPre + "completeness_measure", content);
                } else if (tags.endsWith(       "</field-precision>") && hasContent) {
                      dataQuality.append(       "* Field-precision: " + content + "\n");
                    gAddAtts.add(dataQualityPre + "field_precision", content);
                } else if (tags.endsWith(       "</sensitivity>") && hasContent) {
                      dataQuality.append(       "* Sensitivity: " + content + "\n");
                    gAddAtts.add(dataQualityPre + "sensitivity", content);
                } else if (tags.endsWith(       "</quality-control-procedures>") && hasContent) {
                      dataQuality.append(       "* Quality-control-procedures: " + content + "\n");
                    gAddAtts.add(dataQualityPre +         "control_procedures", content);
                }

            //data-management /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            } else if (tags.startsWith("<data-management>")) {
                //there are several other attributes, but none of general interest
                //  <resources-identified>Yes</resources-identified>
                //  <resources-budget-percentage>Unknown</resources-budget-percentage>
                //  <data-access-directive-compliant>Yes</data-access-directive-compliant>
                //  <data-access-directive-waiver>No</data-access-directive-waiver>
                //  <hosting-service-needed>No</hosting-service-needed>
                //  <delay-collection-dissemination>1 year</delay-collection-dissemination>
                //  <delay-collection-dissemination-explanation>NA</delay-collection-dissemination-explanation>
                //  <archive-location>Other</archive-location>
                //  <archive-location-explanation-other>yes</archive-location-explanation-other>
                //  <delay-collection-archive>NA</delay-collection-archive>
                //  <data-protection-plan>NA</data-protection-plan>
                if        (tags.equals("<data-management></archive-location>") && hasContent) {
                    gAddAtts.add(                         "archive_location", content); 
                    history.append("archive_location=" + content + "\n");  //e.g. NCEI
                }            

            //lineage-statement   /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            } else if (tags.startsWith("<lineage></lineage-statement>") && hasContent) {
                history.append("Lineage Statement: " + content + "\n");

            //lineage-sources, good example: /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/25229.xml
            } else if (tags.startsWith("<lineage><lineage-sources>")) {
                if (tags.endsWith("<lineage-source>")) {  //start of 
                    history.append("Lineage Source #" + ++lineageSourceN);
                } else if (tags.endsWith("</citation-title>") && hasContent) {
                    history.append(sep + " title=" + content);
                } else if (tags.endsWith("</originator-publisher>") && hasContent) {
                    history.append(sep + " publisher=" + content);
                } else if (tags.endsWith("</publish-date>") && hasContent) {
                    history.append(sep + " date published=" + 
                        convertInportTimeToIso8601(content));
                } else if (tags.endsWith("</citation>") && hasContent) {
                    history.append(sep + " citation=" + content);
                } else if (tags.endsWith("</lineage-source>")) {
                    history.append("\n");
                }

            //lineage-process-steps
            } else if (tags.startsWith("<lineage><lineage-process-steps>")) {
                if        (tags.endsWith("<lineage-process-step>")) {  //start of step
                    lineageStepN = null; 
                    lineageName = null;
                    lineageEmail = null;
                    lineageDescription = null;
                } else if (tags.endsWith("</sequence-number>") && hasContent) {
                    lineageStepN = content;
                } else if (tags.endsWith("</description>") && hasContent) {
                    lineageDescription = content;
                } else if (tags.endsWith("</process-contact>") && hasContent) {
                    lineageName = content;
                } else if (tags.endsWith("</email-address>") && hasContent) {
                    lineageEmail = content;
                } else if (tags.endsWith("</lineage-process-step>") &&  //end of step
                    (lineageName != null || lineageEmail != null || lineageDescription != null)) {
                    history.append("Lineage Step #" + 
                        (lineageStepN == null? "?" : lineageStepN) + 
                        (lineageName  == null? "" : ", " + lineageName) +
                        (lineageEmail == null? "" : " <" + lineageEmail + ">") +
                        (lineageDescription == null? "" : ": " + lineageDescription) +
                        "\n");
                }

            //child-items  
            //If whichChild == 0, add this info to childItems.
            } else if (tags.startsWith("<child-items>") && whichChild == 0) {
                if        (tags.equals("<child-items><child-item>")) {
                    //a new child-item
                    nChildItems++;
                    if (reallyVerbose)
                        String2.log("Since whichChild=" + whichChild + 
                        ", I'm " + 
                        (whichChild == 0 || nChildItems == whichChild? "processing" : "skipping") +
                        " <child-item> for entity #" + nChildItems);
                    if (whichChild == 0 || nChildItems == whichChild) {
                        if (whichChild > 0) whichChildFound = true;
                        childItemsPre = "InPort_child_item_" + 
                            (whichChild > 0? "" : nChildItems + "_");
                    } else {
                        //skip this child
                        xmlReader.skipToStackSize(xmlReader.stackSize());
                    }

                } else if (tags.equals("<child-items><child-item></catalog-item-id>")) {
                    childItems.append("Child Item #" + nChildItems + 
                        ": item-id=" + (hasContent? content : "?"));
                    background.append("> child-item #" + nChildItems + 
                        " catalog-item-id=" + content + "\n");
                    if (hasContent)
                        gAddAtts.add(             childItemsPre + "catalog_id", content);
                } else if (tags.equals("<child-items><child-item></catalog-item-type>") && hasContent) {
                        childItems.append(                         sep + " item-type=" + content + "\n");  //e.g., Entity                    
                        gAddAtts.add(                     childItemsPre + "item_type", content);
                } else if (tags.equals("<child-items><child-item></title>") && hasContent) {
                        childItems.append(                        "Title: " + content + "\n");
                        gAddAtts.add(             childItemsPre + "title", content);
                } else if (tags.equals("<child-items></child-item>")) {
                        childItems.append('\n');
                }

            //faqs    /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
            //   <faqs>
            //      <faq cc-id="223844">
            //         <date>20150922</date>
            //         <author>Lewis, Steve </author>
            //         <question>can this dataset be used for navigation.</question>
            //         <answer>No.</answer>
            //      </faq>
            //   </faqs>
            } else if (tags.startsWith("<faqs>")) {
                if        (tags.equals("<faqs><faq>")) {
                            faqs.append("FAQ #" + ++nFaqs);
                             faqsPre = "InPort_faq_" + nFaqs + "_";
                             //skip cc-id: it's an internal DB identifier
                } else if (tags.equals("<faqs><faq></date>") && hasContent) {
                    content = convertInportTimeToIso8601(content);
                    faqs.append(                  ": date=" + content);
                        gAddAtts.add(     faqsPre + "date", content);
                } else if (tags.equals("<faqs><faq></author>") && hasContent) {
                    int po = content.indexOf(", ");  //e.g., Clapham, Phillip
                    if (po > 0)   
                        content = content.substring(po + 2) + " " + content.substring(0, po); 
                                      faqs.append(", author=" + content + "\n");
                        gAddAtts.add(     faqsPre + "author", content);
                } else if (tags.equals("<faqs><faq></question>") && hasContent) {
                    keywords.add("faq");
                    String2.addNewlineIfNone(faqs).append("Question: " + content + "\n");
                        //insert extra _ so it sorts before "answer"
                        gAddAtts.add(          faqsPre + "_question", content);
                } else if (tags.equals("<faqs><faq></answer>") && hasContent) {
                    String2.addNewlineIfNone(faqs).append("Answer: " + content + "\n");
                        gAddAtts.add(     faqsPre + "answer", content);
                } else if (tags.equals("<faqs></faq>")) {
                    String2.addNewlineIfNone(faqs).append("\n");
                }

            //catalog-details  
            } else if (tags.startsWith("<catalog-details>")) {
          
                if        (tags.endsWith("</metadata-record-created-by>") && hasContent) {
                    metaCreatedBy = content;  //e.g., SysAdmin ...  
                    gAddAtts.add(   "InPort_metadata_record_created_by", content);
                } else if (tags.endsWith("</metadata-record-created>") && hasContent) {
                    content = convertInportTimeToIso8601(content); //e.g., 20160518T185232
                    metaCreated = content;     
                    gAddAtts.add(   "InPort_metadata_record_created", content);
                } else if (tags.endsWith("</metadata-record-last-modified-by>") && hasContent) {
                    metaLastModBy = content;  //e.g., Renold Narita
                    gAddAtts.add(   "InPort_metadata_record_last_modified_by", content);
                } else if (tags.endsWith("</metadata-record-last-modified>") && hasContent) {
                    content = convertInportTimeToIso8601(content); //e.g., 20160518T185232
                    metaLastMod = content;  
                    gAddAtts.add(   "InPort_metadata_record_last_modified", content);
                } else if (tags.endsWith("</owner-organization-acronym>") && hasContent) {
                    //Tally: AFSC: 382, NWFSC: 295, SEFSC: 292, PIFSC: 275, NEFSC: 120,
                    //  SWFSC: 109, OST: 43, PIRO: 31, AKRO: 30, GARFO: 23, SERO: 14,
                    //  WCRO: 11, OHC: 10, GSMFC: 8, OPR: 1, OSF: 1
                    gAddAtts.add("institution", 
                        xmlFileName.indexOf("/NOAA/NMFS/") > 0? "NOAA NMFS " + content: //e.g., SWFSC
                        xmlFileName.indexOf("/NOAA/")      > 0? "NOAA " + content: 
                        content); 
                    acronym = content;
                    gAddAtts.add(   "InPort_owner_organization_acronym", content);
                } else if (tags.endsWith("</publication-status>")) {                
                    Test.ensureEqual(content, "Public", "Unexpected <publication-status> content.");
                    gAddAtts.add(   "InPort_publication_status", content);
                } else if (tags.endsWith("</is-do-not-publish>")) {                
                    //Tally: N: 3953 (100%) (probably because I harvested Public records)
                    Test.ensureEqual(content, "No", "Unexpected <is-do-not-publish> content.");
                }

            } else {
                //log things not handled?
                //if (hasContent) 
                    //String2.log(" not handled: " + tags + " = " content);
            }
        }

        //desired whichChild not found?
        if (whichChild > 0 && !whichChildFound)         
            throw new RuntimeException("ERROR: whichChild=" + whichChild + 
                " not found as <entity-attribute-information>, " +
                "<distribution>, and/or <child-item>.");

        //cleanup creator info
        //String2.pressEnterToContinue(
        //    "creator_name=" + gAddAtts.get("creator_name") + ", " + creatorName2 + ", " + creatorName3 + "\n" +
        //    "creator_email=" + gAddAtts.get("creator_email") + ", " + creatorEmail2 + ", " + creatorEmail3 + "\n");
        if (gAddAtts.get("creator_name") == null) {
            if        (creatorName2 != null) {
                gAddAtts.set("creator_name", creatorName2);
                gAddAtts.set("creator_type", "person");
            } else if (creatorName3 != null) {
                gAddAtts.set("creator_name", creatorName3);
                gAddAtts.set("creator_type", "person");
            } else if (creatorOrg2 != null) {
                gAddAtts.set("creator_name", creatorOrg2);
                gAddAtts.set("creator_type", "institution");
            } else if (creatorOrg3 != null) {
                gAddAtts.set("creator_name", creatorOrg3);
                gAddAtts.set("creator_type", "institution");
            }
        }

        if (gAddAtts.get("creator_email") == null) {
            if      (creatorEmail2 != null) gAddAtts.set("creator_email", creatorEmail2);
            else if (creatorEmail3 != null) gAddAtts.set("creator_email", creatorEmail3);
        }

        if (gAddAtts.get("creator_url") == null) {
            String cu = null;
            if        (creatorUrl2 != null) {
                  cu = creatorUrl2;
            } else if (creatorUrl3 != null) {
                  cu = creatorUrl3;
            } else if (!acronym.equals("???")) {
                cu = 
                "AFSC".equals(  acronym)? "https://www.afsc.noaa.gov/":
                "AKRO".equals(  acronym)? "https://alaskafisheries.noaa.gov/":
                "GARFO".equals( acronym)? "https://www.greateratlantic.fisheries.noaa.gov/":
                "GSMFC".equals( acronym)? "http://www.gsmfc.org/":
                "NEFSC".equals( acronym)? "https://www.nefsc.noaa.gov/":
                "NWFSC".equals( acronym)? "https://www.nwfsc.noaa.gov/":
                "OHC".equals(   acronym)? "http://www.habitat.noaa.gov/":
                "OPR".equals(   acronym)? "https://www.fisheries.noaa.gov/about/office-protected-resources":
                "OSF".equals(   acronym)? "https://www.fisheries.noaa.gov/about/office-sustainable-fisheries":
                "OST".equals(   acronym)? "https://www.st.nmfs.noaa.gov/":
                "PIFSC".equals( acronym)? "https://www.pifsc.noaa.gov/":
                "PIRO".equals(  acronym)? "http://www.fpir.noaa.gov/":
                "SEFSC".equals( acronym)? "https://www.sefsc.noaa.gov/":
                "SERO".equals(  acronym)? "http://sero.nmfs.noaa.gov/":
                "SWFSC".equals( acronym)? "https://swfsc.noaa.gov/": 
                "WCRO".equals(  acronym)? "http://www.westcoast.fisheries.noaa.gov/":
                null;
            }
            if (cu != null)
                gAddAtts.add("creator_url", cu);
        }

        //dataQuality -- now done separately
        //if (dataQuality.length() > 0) 
        //    gAddAtts.add("processing_level", dataQuality.toString().trim());  //an ACDD att

        //distribution -- now done separately
        //if (distribution.length() > 0)
        //    gAddAtts.add("InPort_distribution_information", distribution.toString().trim());

        //faqs -- now done separately
        //if (faqs.length() > 0)
        //    gAddAtts.add("InPort_faqs", faqs.toString().trim());

        //cleanup history (add to lineage info gathered above)
        if (!metaCreatedBy.equals("???"))
            history.append(metaCreated + " " + metaCreatedBy +
                " originally created InPort catalog-item-id #" + catID + ".\n");
        if (!metaLastModBy.equals("???"))
            history.append(metaLastMod + " " + metaLastModBy +
                " last modified InPort catalog-item-id #" + catID + ".\n");
        history.append( 
            Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10) +
            " GenerateDatasetsXml in ERDDAP v" + EDStatic.erddapVersion + 
            " (contact: bob.simons@noaa.gov) converted " +
            "inport-xml metadata from " + inportXmlUrl + " into an ERDDAP dataset description.\n");
        gAddAtts.add("history", history.toString().trim());

        //issues -- now done separately
        //if (issues.length() > 0)
        //    gAddAtts.add("InPort_issues", issues.toString().trim());

        //cleanup keywords
        gAddAtts.add("keywords", String2.toCSSVString(keywords));

        //cleanup license
        if (license.indexOf("Security class: Unclassified") >= 0 && //if Unclassified
            license.indexOf("Data access constraints: ") < 0 &&  //and no other info
            license.indexOf("Data access policy: ") < 0 &&
            license.indexOf("Data use constraints: ") < 0) 
            license.append("[standard]");
        else if (license.length() == 0)
            license.append("???");
        gAddAtts.add("license", license.toString().trim());

        //childItems -- now done separately
        //if (childItems.length() > 0)
        //    gAddAtts.add("InPort_child_items", childItems.toString().trim());

        gAddAtts.add("sourceUrl", tSourceUrl);

        gAddAtts.add("summary", summary.length() == 0? title : summary.toString().trim());

        //urls -- now done separately
        //if (urls.length() > 0)
        //    gAddAtts.add("InPort_urls", urls.toString().trim());

        //*** match to specified file?
        if (String2.isSomething(tDataDir)) {
            String msg;
            try {
                //ensure dir exists
                if (File2.isDirectory(tDataDir)) {
                    msg = "> dataDir     =" + tDataDir + " already exists."; 
                    String2.log(msg);
                    background.append(msg + "\n");
                } else {
                    msg = "> creating dataDir=" + tDataDir;
                    String2.log(msg);
                    background.append(msg + "\n");
                    File2.makeDirectory(tDataDir);  //throws exception if trouble
                }

                //if a dataFileName was specified, read it 
                if (!"???".equals(tDataFileName)) {
                    msg = "> dataFileName=" + tDataDir + tDataFileName; 
                    String2.log(msg);
                    background.append(msg + "\n");
                    Table fileTable = new Table();
                    fileTable.readASCII(tDataDir + tDataFileName, String2.ISO_8859_1,
                        "", "", 0, 1, 
                        null, null, null, null, null, false);  //simplify?  (see below)
                    msg = "> dataFileTable columnNames=" + fileTable.getColumnNamesCSSVString(); 
                    String2.log(msg);
                    background.append(msg + "\n");
                    
                    if (sourceTable.nColumns() == 0) {
                        //inport-xml had no entity-attributes, so just use the file as is
                        for (int fcol = 0; fcol < fileTable.nColumns(); fcol++) {
                            String colName    = fileTable.getColumnName(fcol);
                            Attributes atts   = fileTable.columnAttributes(fcol);
                            PrimitiveArray pa = fileTable.getColumn(fcol);
                            sourceTable.addColumn(fcol, colName, 
                                (PrimitiveArray)(pa.clone()),
                                (Attributes)(atts.clone()));
                        }
                        
                    } else {
                        //inport-xml had entity-attributes, try to match to names in ascii file
                        BitSet matched = new BitSet();
                        for (int icol = 0; icol < sourceTable.nColumns(); icol++) {
                            String colName = sourceTable.getColumnName(icol);
                            for (int fcol = 0; fcol < fileTable.nColumns(); fcol++) {
                                String tryName = fileTable.getColumnName(fcol);
                                if (String2.looselyEquals(colName, tryName)) {
                                    matched.set(icol);
                                    sourceTable.setColumn(icol, fileTable.getColumn(fcol));
                                    if (!colName.equals(tryName)) {
                                        msg = "> I changed InPort entity attribute colName=" + colName +
                                            " into ascii file colName=" + tryName;
                                        String2.log(msg);
                                        background.append(msg + "\n");
                                        sourceTable.setColumnName(icol, tryName);
                                    }
                                    fileTable.removeColumn(fcol);
                                    break;
                                }
                            }
                        }
                        if (sourceTable.nColumns() > 0 &&
                            matched.nextClearBit(0) == sourceTable.nColumns()) {
                            msg = "> Very Good! All InPort columnNames matched columnNames in the fileTable.";
                            String2.log(msg);
                            background.append(msg + "\n");
                        }

                        //for colNames not matched, get admin to try to make a match
                        for (int icol = 0; icol < sourceTable.nColumns(); icol++) {
                            if (!matched.get(icol)) {
                                String colName = sourceTable.getColumnName(icol);
                                String actual = String2.getStringFromSystemIn(
                                    "Column name #" + icol + "=" + colName + " isn't in the ASCII file.\n" +
                                    "Enter one of these names:\n" +
                                    fileTable.getColumnNamesCSSVString() + "\n" +
                                    "or press Enter to append '?' to the column name to signify it is unmatched.");
                                if (actual.length() == 0) {
                                    sourceTable.setColumnName(icol, colName + "?");
                                } else {
                                    int fcol = fileTable.findColumnNumber(actual);
                                    if (fcol >= 0) {
                                        fileTable.removeColumn(fcol);
                                        sourceTable.setColumn(icol, fileTable.getColumn(fcol));
                                    }
                                    sourceTable.setColumnName(icol, actual);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException(String2.ERROR + " while working with " + 
                    tDataDir + tDataFileName,
                    t);
            }

            //ensure all column have same number of values
            //they won't be same size if not all columns matched above
            sourceTable.makeColumnsSameSize();
        }

        //*** end stuff
        boolean dateTimeAlreadyFound = false;
        String tSortedColumnSourceName = "";
        String tSortFilesBySourceNames = "";
        String tColumnNameForExtract   = "";

        //clean up sourceTable
        sourceTable.convertIsSomething2();  //convert e.g., "N/A" to ""
        sourceTable.simplify();
        sourceTable.standardize(tStandardizeWhat);

        //make addTable
        Table addTable    = new Table();
        for (int col = 0; col < sourceTable.nColumns(); col++) {
            String colName = String2.modifyToBeVariableNameSafe(sourceTable.getColumnName(col));
            //if (colName.matches("[A-Z0-9_]+"))  //all uppercase
            //    colName = colName.toLowerCase();
            addTable.addColumn(col, colName, 
                (PrimitiveArray)(sourceTable.getColumn(col).clone()), 
                (Attributes)(sourceTable.columnAttributes(col).clone())); //move from source to add
            sourceTable.columnAttributes(col).clear();
        }

        for (int col = 0; col < addTable.nColumns(); col++) {
            String colName = addTable.getColumnName(col);
            PrimitiveArray destPA = addTable.getColumn(col); 
            //sourceAtts already in use as source global atts
            Attributes addAtts    = addTable.columnAttributes(col);

            //then look for date columns  
            String tUnits = addTable.columnAttributes(col).getString("units");
            if (tUnits == null) tUnits = "";
            if (tUnits.toLowerCase().indexOf("yy") >= 0 &&
                destPA.elementType() != PAType.STRING) {
                //convert e.g., yyyyMMdd columns from int to String
                destPA = new StringArray(destPA);   
                addTable.setColumn(col, destPA);                       
            }
            if (destPA.elementType() == PAType.STRING) {
                tUnits = Calendar2.suggestDateTimeFormat((StringArray)destPA, false); //evenIfPurelyNumeric
                if (tUnits.length() > 0)
                    addAtts.set("units", tUnits);
                //??? and if tUnits = "", set to ""???
            }
            boolean isDateTime = Calendar2.isTimeUnits(tUnits);

            addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                gAddAtts, sourceTable.columnAttributes(col), addAtts, 
                sourceTable.getColumnName(col), 
                destPA.elementType() != PAType.STRING, //tryToAddStandardName
                destPA.elementType() != PAType.STRING, //addColorBarMinMax
                true); //tryToFindLLAT

            //add missing_value and/or _FillValue if needed
            addMvFvAttsIfNeeded(colName, destPA, sourceTable.columnAttributes(col), addAtts);

            //files are likely sorted by first date time variable
            //and no harm if files aren't sorted that way
            if (tSortedColumnSourceName.length() == 0 && 
                isDateTime && !dateTimeAlreadyFound) {
                dateTimeAlreadyFound = true;
                tSortedColumnSourceName = colName;
            }
        }

        //tryToFindLLAT
        tryToFindLLAT(sourceTable, addTable);

        //*** makeReadyToUseGlobalAtts
        addTable.globalAttributes().set(makeReadyToUseAddGlobalAttributesForDatasetsXml(
            sourceAtts, 
            hasLonLatTime(addTable)? "Point" : "Other",
            "(local files)", //???
            gAddAtts, 
            suggestKeywords(sourceTable, addTable)));
        gAddAtts = addTable.globalAttributes();

        //subsetVariables
        if (sourceTable.globalAttributes().getString("subsetVariables") == null &&
               addTable.globalAttributes().getString("subsetVariables") == null) 
            gAddAtts.add("subsetVariables",
                suggestSubsetVariables(sourceTable, addTable, true)); //1file/dataset?

        StringBuilder defaultDataQuery = new StringBuilder();
        StringBuilder defaultGraphQuery = new StringBuilder();
        if (addTable.findColumnNumber(EDV.TIME_NAME) >= 0) {
            defaultDataQuery.append( "&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
            defaultGraphQuery.append("&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
        }
        defaultGraphQuery.append("&amp;.marker=1|5");

        //use original title, with InPort # added
        gAddAtts.add("title", title + " (InPort #" + catID + 
            (whichChild == 0?  "" : 
             entityID != null? "ce" + entityID :
                               "c" + whichChild) +
            ")"); //catID ensures it is unique

        //fgdc and iso19115
        String fgdcFile     = String2.replaceAll(xmlFileName, "/inport-xml/", "/fgdc/");
        String iso19115File = String2.replaceAll(xmlFileName, "/inport-xml/", "/iso19115/");
        if (!File2.isFile(fgdcFile))
            fgdcFile     = ""; //if so, don't serve an fgdc file
        if (!File2.isFile(iso19115File))
            iso19115File = ""; //if so, don't serve an iso19115 file

        //write datasets.xml
        StringBuilder results = new StringBuilder();
        tDataDir      = File2.addSlash(tDataDir);
        tDataDir      = String2.replaceAll(tDataDir,      "\\", "/");
        tDataDir      = String2.replaceAll(tDataDir,      ".",  "\\.");
        tDataFileName = String2.replaceAll(tDataFileName, ".",  "\\.");

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
                acronym.toLowerCase() + "InPort" + catID + 
                (whichChild == 0?  "" : 
                 entityID != null? "ce" + entityID :
                                   "c" + whichChild) +
                "\" active=\"true\">\n" +
            (defaultDataQuery.length() > 0? 
            "    <defaultDataQuery>" + defaultDataQuery + "</defaultDataQuery>\n" : "") +
            (defaultGraphQuery.length() > 0? 
            "    <defaultGraphQuery>" + defaultGraphQuery + "</defaultGraphQuery>\n" : "") +
            "    <fileDir>" + XML.encodeAsXML(tDataDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tDataFileName) + "</fileNameRegex>\n" + 
            "    <charset>ISO-8859-1</charset>\n" +
            "    <columnNamesRow>1</columnNamesRow>\n" +
            "    <firstDataRow>2</firstDataRow>\n" +
            "    <standardizeWhat>" + tStandardizeWhat + "</standardizeWhat>\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +
            "    <updateEveryNMillis>-1</updateEveryNMillis>\n" +  
            "    <accessibleViaFiles>true</accessibleViaFiles>\n" +
            "    <fgdcFile>" + fgdcFile + "</fgdcFile>\n" +
            "    <iso19115File>" + iso19115File + "</iso19115File>\n");
        results.append(writeAttsForDatasetsXml(false, sourceTable.globalAttributes(), "    "));
        results.append(cdmSuggestion());
        results.append(writeAttsForDatasetsXml(true,  addTable.globalAttributes(),    "    "));
        
        results.append(writeVariablesForDatasetsXml(sourceTable, addTable, 
            "dataVariable", 
            true, false)); //includeDataType, questionDestinationName
        results.append(
            "</dataset>\n" +
            "\n");        

        //background
        String2.log("\n-----");
        if (background.length() > 0)
            String2.log("Background for ERDDAP:\n" + background.toString());
        if (whichChild == 0) 
            String2.log( 
                "> nChildItems (with little info)=" + nChildItems + 
                ", nDistributions=" + nDistributions +
                ", nEntities (with attribute info)=" + nEntities +
                ", nUrls=" + nUrls);
        if (!"Unclassified".equals(securityClass))
            String2.log("> WARNING! <security-class>=" + securityClass);
        String2.log("\n* generateDatasetsXmlFromInPort finished successfully.\n-----\n");
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
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");
        ZoneId   zoneId   = ZoneId.of(           "US/Pacific");
        double epSec;

        //test winter/standard time: 2005-04-03T00:00 Pacific
        // see https://www.timeanddate.com/worldclock/converter.html
        epSec = 1112515200; //from 2005-04-03T08:00Z in convert / time
        Test.ensureEqual(
            Calendar2.epochSecondsToIsoStringTZ(epSec), "2005-04-03T08:00:00Z", "");//8hrs 
        Test.ensureEqual(
            Calendar2.isoStringToEpochSeconds("2005-04-03T00:00", timeZone),
            epSec, "");

        //test summer/daylight saving time: 2005-04-03T05:00 Pacific
        epSec = 1112529600; //from 2005-04-03T12:00Z in convert / time
        Test.ensureEqual(
            Calendar2.epochSecondsToIsoStringTZ(epSec), "2005-04-03T12:00:00Z", "");//7hrs
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
"    String infoUrl \"https://www.pfeg.noaa.gov\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"keywords, many\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
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
        results = String2.directReadFrom88591File(testDir + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
"    String infoUrl \"https://www.pfeg.noaa.gov\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"keywords, lots, of\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
     * This tests GenerateDatasetsXml with EDDTableFromInPort whichChild=0
     * and tests if datasets.xml can be generated from csv file even if no child-entity info. 
     * 2017-08-09 I switched from old /inport/ to new /inport-xml/ .
     */
    public static void testGenerateDatasetsXmlFromInPort() throws Throwable {
        String2.log("\n*** EDDTableFromAsciiFiles.testGenerateDatasetsXmlFromInPort()\n");
        testVerboseOn();
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        String xmlDir  = "/u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/";
        String xmlFile = xmlDir + "27377.xml";
        int whichChild = 0;
        String dataDir = "/u00/data/points/inportData/";


        String results = generateDatasetsXmlFromInPort(
            xmlFile, xmlDir, ".*", whichChild, dataDir, "", -1) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromInPort",
            xmlFile, xmlDir, "" + whichChild, dataDir, "", "-1"}, //defaultStandardizeWhat
            false); //doIt loop?

String expected = 
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"akroInPort27377\" active=\"true\">\n" +
"    <defaultGraphQuery>&amp;.marker=1|5</defaultGraphQuery>\n" +
"    <fileDir>/u00/data/points/inportData/27377/</fileDir>\n" +
"    <fileNameRegex>???</fileNameRegex>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <standardizeWhat>0</standardizeWhat>\n"+
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>-1</updateEveryNMillis>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <fgdcFile>/u00/data/points/inportXml/NOAA/NMFS/AKRO/fgdc/xml/27377.xml</fgdcFile>\n" +
"    <iso19115File>/u00/data/points/inportXml/NOAA/NMFS/AKRO/iso19115/xml/27377.xml</iso19115File>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"acknowledgment\">Steve Lewis, Jarvis Shultz</att>\n" +
"        <att name=\"archive_location\">Other</att>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">steve.lewis@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Steve Lewis</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">http://alaskafisheries.noaa.gov/</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">88.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">40.0</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">170.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-133.0</att>\n" +
"        <att name=\"history\">archive_location=Other\n" +
"Lineage Statement: Multibeam (downloaded From NGDC&#xbc; degrees blocks. 2913 downloads) NOAA Fisheries, Alaska 254,125,225 Hydro Survey NOAA Fisheries, Alaska 21,436,742 GOA: UNH Multibeam: 2010 Univ of New Hampshire\\AKRO 17,225,078 Bering SEA UNH Multibeam Univ of New Hampshire\\AKRO 2,120,598 Trackline Geophyics NOAA Fisheries, Alaska 42,851,636 Chart Smooth Sheets Bathy Points SEAK The Nature Conservancy - TNC SEAK 79,481 Multibeam - 2013 NOAA Fisheries, Alaska 25,885,494 Gebco ETOPO NOAA Fisheries, Alaska 56,414,222 Mapped Shoreline (Units) defines MHW ShoreZone Program 151,412  Compiled by NGDC  NOAA Ship Rainier - Multibeam Processing with Caris Compiled by Rainier 1,126,111  Compiled  Lim, E., B.W. Eakins, and R. Wigley, Coastal Relief Model of Southern Alaska: Procedures, Data Sources and Analysis, NOAA Technical Memorandum NESDIS NGDC-43, 22 pp., August 2011. With parts of NGDC:: Southeast Alaska, AK MHHW DEM; Juneau Alaska, AK MHHW DEM, Sitka Alaska, MHHW DEM. TOTAL Processed Features Added to AKRO Terrain Dataset where we did not have multibeam or hydro survey data.  138,195,886559,611,885 \n" +
"Further MB from NCEIis downloaded as 43,000 individual tracklines in XYZ or MB58 format and processed using ArcPY and MB software.\n" +
"There are combined 18.6 billions points of data in the full dataset.  This includes data from Trackline GeoPhysics, Hydro Surveyes, Lidar, and Multibeam trackliens.\n" +
"2015-09-22T22:56:00Z Steve Lewis originally created InPort catalog-item-id #27377.\n" +
"2017-07-06T21:18:53Z Steve Lewis last modified InPort catalog-item-id #27377.\n" +
today + " GenerateDatasetsXml in ERDDAP v2.10 (contact: bob.simons@noaa.gov) converted inport-xml metadata from https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml into an ERDDAP dataset description.</att>\n" +
"        <att name=\"infoUrl\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml</att>\n" +
"        <att name=\"InPort_data_quality_accuracy\">1/4 degree grids multibean at a resolution of 40m\n" +
"\n" +
"Other integrated datasets with resolutiomd far less, such as the 500 meter ETOPO2 dataset.  \n" +
"\n" +
"80,000+ trracklines were extracted and processed at full resolution, often less than 1 meter resolution.</att>\n" +
"        <att name=\"InPort_data_quality_analytical_accuracy\">1/4 degree grids multibean at a resolution of 40m\n" +
"\n" +
"Other integrated datasets with resolutiomd far less, such as the 500 meter ETOPO2 dataset.  \n" +
"\n" +
"42,300 MB trracklines were extracted and processed at full resolution, often less than 1 meter resolution.</att>\n" +
"        <att name=\"InPort_data_quality_completeness_measure\">Multibeam tracks are often prone to conal outliers. However, these outliers and investigated both using various GIS analytical tools</att>\n" +
"        <att name=\"InPort_data_quality_control_procedures\">Used K-natural neighbors, Percentiles, and ArcGIS slope tools to location and remove outliers.</att>\n" +
"        <att name=\"InPort_data_quality_field_precision\">1/10 of a meter.</att>\n" +
"        <att name=\"InPort_data_quality_representativeness\">Data was compiled from downloaded NetCDF GRD files and include trackline geophysics, hydrographic surveys, gridded multi-beam NET CDF,  XYZ files, and MB-58 multi-beam. These data consist of approximately 18.6 billion depth data points \n" +
"\n" +
"Data was extracted, parsed, and groomed by each of the individual 84,009+ tracklines using statistical analysis and visual inspection with some imputation\n" +
"\n" +
"42,300 MB tracklines were extracted and processed at full resolution, often less than 1 meter resolution\n" +
"\n" +
"We are currently downloading a multi-national bathymetric data with another 160,000 surveys (April, 2017)</att>\n" +
"        <att name=\"InPort_data_quality_sensitivity\">Multibeam tracks are often prone with conal outliers.</att>\n" +
"        <att name=\"InPort_dataset_maintenance_frequency\">Quarterly</att>\n" +
"        <att name=\"InPort_dataset_presentation_form\">Map (digital)</att>\n" +
"        <att name=\"InPort_dataset_publication_status\">Published</att>\n" +
"        <att name=\"InPort_dataset_publish_date\">2017</att>\n" +
"        <att name=\"InPort_dataset_source_media_type\">computer program</att>\n" +
"        <att name=\"InPort_dataset_type\">GIS dataset  Point, Terrain, Raster</att>\n" +
"        <att name=\"InPort_distribution_1_download_url\">http://alaskafisheries.noaa.gov/arcgis/rest/services</att>\n" +
"        <att name=\"InPort_distribution_1_file_name\">ShoreZoneFlex</att>\n" +
"        <att name=\"InPort_distribution_1_file_type\">ESRI REST</att>\n" +
"        <att name=\"InPort_distribution_1_review_status\">Chked MD</att>\n" +
"        <att name=\"InPort_distribution_2_download_url\">http://alaskafisheries.noaa.gov/arcgis/rest/services</att>\n" +
"        <att name=\"InPort_distribution_2_file_name\">ALaskaBathy_SE</att>\n" +
"        <att name=\"InPort_distribution_2_file_type\">ESRI REST</att>\n" +
"        <att name=\"InPort_distribution_2_review_status\">Chked MD</att>\n" +
"        <att name=\"InPort_distribution_3_download_url\">https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer</att>\n" +
"        <att name=\"InPort_distribution_3_file_name\">ALaskaBathy</att>\n" +
"        <att name=\"InPort_distribution_3_review_status\">Chked MD</att>\n" +
"        <att name=\"InPort_distribution_4_download_url\">https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer</att>\n" +
"        <att name=\"InPort_distribution_4_file_name\">ALaskaBathy</att>\n" +
"        <att name=\"InPort_distribution_4_file_type\">ESRI REST</att>\n" +
"        <att name=\"InPort_distribution_4_review_status\">Chked MD</att>\n" +
"        <att name=\"InPort_faq_1__question\">can this dataset be used for navigation.</att>\n" +
"        <att name=\"InPort_faq_1_answer\">No.</att>\n" +
"        <att name=\"InPort_faq_1_author\">Steve Lewis</att>\n" +
"        <att name=\"InPort_faq_1_date\">2015-09-22</att>\n" +
"        <att name=\"InPort_fishing_gear\">Soundings, multibeam</att>\n" +
"        <att name=\"InPort_issue_1_author\">Steve Lewis</att>\n" +
"        <att name=\"InPort_issue_1_date\">2013</att>\n" +
"        <att name=\"InPort_issue_1_issue\">Outlier removal processes</att>\n" +
"        <att name=\"InPort_item_id\">27377</att>\n" +
"        <att name=\"InPort_item_type\">Data Set</att>\n" +
"        <att name=\"InPort_metadata_record_created\">2015-09-22T22:56:00Z</att>\n" +
"        <att name=\"InPort_metadata_record_created_by\">Steve Lewis</att>\n" +
"        <att name=\"InPort_metadata_record_last_modified\">2017-07-06T21:18:53Z</att>\n" +
"        <att name=\"InPort_metadata_record_last_modified_by\">Steve Lewis</att>\n" +
"        <att name=\"InPort_metadata_workflow_state\">Published / External</att>\n" +
"        <att name=\"InPort_owner_organization_acronym\">AKRO</att>\n" +
"        <att name=\"InPort_parent_item_id\">26657</att>\n" +
"        <att name=\"InPort_publication_status\">Public</att>\n" +
"        <att name=\"InPort_status\">In Work</att>\n" +
"        <att name=\"InPort_support_role_1_organization\">Alaska Regional Office</att>\n" +
"        <att name=\"InPort_support_role_1_organization_url\">http://alaskafisheries.noaa.gov/</att>\n" +
"        <att name=\"InPort_support_role_1_person\">Steve Lewis</att>\n" +
"        <att name=\"InPort_support_role_1_person_email\">steve.lewis@noaa.gov</att>\n" +
"        <att name=\"InPort_support_role_1_type\">Point of Contact</att>\n" +
"        <att name=\"InPort_technical_environment\">In progress.</att>\n" +
"        <att name=\"InPort_url_1_description\">REST Service</att>\n" +
"        <att name=\"InPort_url_1_type\">Online Resource</att>\n" +
"        <att name=\"InPort_url_1_url\">https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer</att>\n" +
"        <att name=\"InPort_xml_url\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml</att>\n" +
"        <att name=\"institution\">NOAA NMFS AKRO</att>\n" +
"        <att name=\"instrument\">ArcGIS</att>\n" +
"        <att name=\"keywords\">akro, alaska, analytical, analytical purposes only, bathy, bathymetry, centers, century, consists, data, dataset, depth, environmental, faq, fisheries, geographic, imported, inform, information, into, marine, national, ncei, nesdis, nmfs, noaa, numerous, ocean, office, only, point, processed, purposes, regional, se alaska, service, southeast, surveys, taken</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">Distribution Liability: for analytical purposes only.  NOT FOR NAVIGATION\n" +
"Data access policy: Not for Navigation\n" +
"Data access procedure: https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer\n" +
"Data access constraints: via REST Services.  Not for navigation.  Analysis only.\n" +
"Metadata access constraints: https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer</att>\n" +
"        <att name=\"platform\">Windows</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"summary\">This dataset is consists of point data taken from numerous depth surveys from the last century.  These data were processed and imported into a geographic information system (GIS) platform to form a bathymetric map of the ocean floor.  Approximately 18.6 billion depth data points were synthesized from various data sources that have been collected and archived since 1901 and includes lead line surveys, trackline geophysics, hydrographic surveys, gridded multi-beam NET CDF files, XYZ files, and MB-58 multi-beam files.  Bathymetric soundings from these datasets span almost all areas of the Arctic and includes Alaska and the surrounding international waters.  Most of the bathymetry data used for this effort is archived and maintained at the National Center for Environmental Information (National Centers for Environmental Information (NCEI)) https://www.ncei.noaa.gov.\n" +
"\n" +
"The purpose of our effort is to develop a high resolution bathymetry dataset for the entire Alaska Exclusive Economic Zone (AEEZ) and surrounding waters by combining and assimilating multiple sets of existing data from historical and recent ocean depth mapping surveys.</att>\n" +
"        <att name=\"time_coverage_begin\">2013</att>\n" +
"        <att name=\"title\">AKRO Analytical Team Metadata Portfolio, Bathymetry (Alaska and surrounding waters) (InPort #27377)</att>\n" +
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
"</dataset>\n\n\n";

        Test.ensureEqual(results, expected, "results=\n" + results);

        Test.ensureEqual(gdxResults, expected, "gdxResults=\n" + gdxResults);


        xmlDir  = "/u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/";
        xmlFile = xmlDir + "27377.xml";
        whichChild = 1;
        dataDir = "/u00/data/points/inportData/";
        //This file isn't from this dataset.
        //This shows that there needn't be any entity-attribute info in the xmlFile .
        String dataFile = "dummy.csv";  

        results = generateDatasetsXmlFromInPort(
            xmlFile, xmlDir, ".*", whichChild, dataDir, dataFile, -1) + "\n";

        //GenerateDatasetsXml
        gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromInPort",
            xmlFile, xmlDir, "" + whichChild, dataDir, dataFile, "-1"}, //defaultStandardizeWhat
            false); //doIt loop?

expected = 
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"akroInPort27377c1\" active=\"true\">\n" +
"    <defaultGraphQuery>&amp;.marker=1|5</defaultGraphQuery>\n" +
"    <fileDir>/u00/data/points/inportData/27377/</fileDir>\n" +
"    <fileNameRegex>dummy\\.csv</fileNameRegex>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>-1</updateEveryNMillis>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <fgdcFile>/u00/data/points/inportXml/NOAA/NMFS/AKRO/fgdc/xml/27377.xml</fgdcFile>\n" +
"    <iso19115File>/u00/data/points/inportXml/NOAA/NMFS/AKRO/iso19115/xml/27377.xml</iso19115File>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"acknowledgment\">Steve Lewis, Jarvis Shultz</att>\n" +
"        <att name=\"archive_location\">Other</att>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">steve.lewis@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Steve Lewis</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">http://alaskafisheries.noaa.gov/</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">88.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">40.0</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">170.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-133.0</att>\n" +
"        <att name=\"history\">archive_location=Other\n" +
"Lineage Statement: Multibeam (downloaded From NGDC&#xbc; degrees blocks. 2913 downloads) NOAA Fisheries, Alaska 254,125,225 Hydro Survey NOAA Fisheries, Alaska 21,436,742 GOA: UNH Multibeam: 2010 Univ of New Hampshire\\AKRO 17,225,078 Bering SEA UNH Multibeam Univ of New Hampshire\\AKRO 2,120,598 Trackline Geophyics NOAA Fisheries, Alaska 42,851,636 Chart Smooth Sheets Bathy Points SEAK The Nature Conservancy - TNC SEAK 79,481 Multibeam - 2013 NOAA Fisheries, Alaska 25,885,494 Gebco ETOPO NOAA Fisheries, Alaska 56,414,222 Mapped Shoreline (Units) defines MHW ShoreZone Program 151,412  Compiled by NGDC  NOAA Ship Rainier - Multibeam Processing with Caris Compiled by Rainier 1,126,111  Compiled  Lim, E., B.W. Eakins, and R. Wigley, Coastal Relief Model of Southern Alaska: Procedures, Data Sources and Analysis, NOAA Technical Memorandum NESDIS NGDC-43, 22 pp., August 2011. With parts of NGDC:: Southeast Alaska, AK MHHW DEM; Juneau Alaska, AK MHHW DEM, Sitka Alaska, MHHW DEM. TOTAL Processed Features Added to AKRO Terrain Dataset where we did not have multibeam or hydro survey data.  138,195,886559,611,885 \n" +
"Further MB from NCEIis downloaded as 43,000 individual tracklines in XYZ or MB58 format and processed using ArcPY and MB software.\n" +
"There are combined 18.6 billions points of data in the full dataset.  This includes data from Trackline GeoPhysics, Hydro Surveyes, Lidar, and Multibeam trackliens.\n" +
"2015-09-22T22:56:00Z Steve Lewis originally created InPort catalog-item-id #27377.\n" +
"2017-07-06T21:18:53Z Steve Lewis last modified InPort catalog-item-id #27377.\n" +
today + " GenerateDatasetsXml in ERDDAP v2.10 (contact: bob.simons@noaa.gov) converted inport-xml metadata from https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml into an ERDDAP dataset description.</att>\n" +
"        <att name=\"infoUrl\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml</att>\n" +
"        <att name=\"InPort_data_quality_accuracy\">1/4 degree grids multibean at a resolution of 40m\n" +
"\n" +
"Other integrated datasets with resolutiomd far less, such as the 500 meter ETOPO2 dataset.  \n" +
"\n" +
"80,000+ trracklines were extracted and processed at full resolution, often less than 1 meter resolution.</att>\n" +
"        <att name=\"InPort_data_quality_analytical_accuracy\">1/4 degree grids multibean at a resolution of 40m\n" +
"\n" +
"Other integrated datasets with resolutiomd far less, such as the 500 meter ETOPO2 dataset.  \n" +
"\n" +
"42,300 MB trracklines were extracted and processed at full resolution, often less than 1 meter resolution.</att>\n" +
"        <att name=\"InPort_data_quality_completeness_measure\">Multibeam tracks are often prone to conal outliers. However, these outliers and investigated both using various GIS analytical tools</att>\n" +
"        <att name=\"InPort_data_quality_control_procedures\">Used K-natural neighbors, Percentiles, and ArcGIS slope tools to location and remove outliers.</att>\n" +
"        <att name=\"InPort_data_quality_field_precision\">1/10 of a meter.</att>\n" +
"        <att name=\"InPort_data_quality_representativeness\">Data was compiled from downloaded NetCDF GRD files and include trackline geophysics, hydrographic surveys, gridded multi-beam NET CDF,  XYZ files, and MB-58 multi-beam. These data consist of approximately 18.6 billion depth data points \n" +
"\n" +
"Data was extracted, parsed, and groomed by each of the individual 84,009+ tracklines using statistical analysis and visual inspection with some imputation\n" +
"\n" +
"42,300 MB tracklines were extracted and processed at full resolution, often less than 1 meter resolution\n" +
"\n" +
"We are currently downloading a multi-national bathymetric data with another 160,000 surveys (April, 2017)</att>\n" +
"        <att name=\"InPort_data_quality_sensitivity\">Multibeam tracks are often prone with conal outliers.</att>\n" +
"        <att name=\"InPort_dataset_maintenance_frequency\">Quarterly</att>\n" +
"        <att name=\"InPort_dataset_presentation_form\">Map (digital)</att>\n" +
"        <att name=\"InPort_dataset_publication_status\">Published</att>\n" +
"        <att name=\"InPort_dataset_publish_date\">2017</att>\n" +
"        <att name=\"InPort_dataset_source_media_type\">computer program</att>\n" +
"        <att name=\"InPort_dataset_type\">GIS dataset  Point, Terrain, Raster</att>\n" +
"        <att name=\"InPort_distribution_download_url\">http://alaskafisheries.noaa.gov/arcgis/rest/services</att>\n" +
"        <att name=\"InPort_distribution_file_name\">ShoreZoneFlex</att>\n" +
"        <att name=\"InPort_distribution_file_type\">ESRI REST</att>\n" +
"        <att name=\"InPort_distribution_review_status\">Chked MD</att>\n" +
"        <att name=\"InPort_faq_1__question\">can this dataset be used for navigation.</att>\n" +
"        <att name=\"InPort_faq_1_answer\">No.</att>\n" +
"        <att name=\"InPort_faq_1_author\">Steve Lewis</att>\n" +
"        <att name=\"InPort_faq_1_date\">2015-09-22</att>\n" +
"        <att name=\"InPort_fishing_gear\">Soundings, multibeam</att>\n" +
"        <att name=\"InPort_issue_1_author\">Steve Lewis</att>\n" +
"        <att name=\"InPort_issue_1_date\">2013</att>\n" +
"        <att name=\"InPort_issue_1_issue\">Outlier removal processes</att>\n" +
"        <att name=\"InPort_item_id\">27377</att>\n" +
"        <att name=\"InPort_item_type\">Data Set</att>\n" +
"        <att name=\"InPort_metadata_record_created\">2015-09-22T22:56:00Z</att>\n" +
"        <att name=\"InPort_metadata_record_created_by\">Steve Lewis</att>\n" +
"        <att name=\"InPort_metadata_record_last_modified\">2017-07-06T21:18:53Z</att>\n" +
"        <att name=\"InPort_metadata_record_last_modified_by\">Steve Lewis</att>\n" +
"        <att name=\"InPort_metadata_workflow_state\">Published / External</att>\n" +
"        <att name=\"InPort_owner_organization_acronym\">AKRO</att>\n" +
"        <att name=\"InPort_parent_item_id\">26657</att>\n" +
"        <att name=\"InPort_publication_status\">Public</att>\n" +
"        <att name=\"InPort_status\">In Work</att>\n" +
"        <att name=\"InPort_support_role_1_organization\">Alaska Regional Office</att>\n" +
"        <att name=\"InPort_support_role_1_organization_url\">http://alaskafisheries.noaa.gov/</att>\n" +
"        <att name=\"InPort_support_role_1_person\">Steve Lewis</att>\n" +
"        <att name=\"InPort_support_role_1_person_email\">steve.lewis@noaa.gov</att>\n" +
"        <att name=\"InPort_support_role_1_type\">Point of Contact</att>\n" +
"        <att name=\"InPort_technical_environment\">In progress.</att>\n" +
"        <att name=\"InPort_url_1_description\">REST Service</att>\n" +
"        <att name=\"InPort_url_1_type\">Online Resource</att>\n" +
"        <att name=\"InPort_url_1_url\">https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer</att>\n" +
"        <att name=\"InPort_xml_url\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml</att>\n" +
"        <att name=\"institution\">NOAA NMFS AKRO</att>\n" +
"        <att name=\"instrument\">ArcGIS</att>\n" +
"        <att name=\"keywords\">akro, alaska, analytical, analytical purposes only, area, ave, AVE_LAT, AVE_LONG, average, avg_depth, avg_temp, bathy, bathymetry, centers, century, consists, core, cpue, CPUE_km2, CPUE_km3, cpue_tow2, cpue_tow3, data, dataset, depth, description, effort, Effort_Area_km_2, Effort_Volume_km_3, environmental, faq, fisheries, geographic, identifier, imported, inform, information, into, km2, km3, km^2, km^3, long, marine, max, min, national, ncei, nesdis, nmfs, noaa, numerous, ocean, office, only, pcod140, Pcod140_n, point, present, processed, purposes, region, regional, se alaska, service, southeast, station, Station_ID, statistics, surveys, taken, temperature, time, tow2, tow3, trawl, Trawl_Type, type, units, variable, volume, year</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">Distribution Liability: for analytical purposes only.  NOT FOR NAVIGATION\n" +
"Data access policy: Not for Navigation\n" +
"Data access procedure: https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer\n" +
"Data access constraints: via REST Services.  Not for navigation.  Analysis only.\n" +
"Metadata access constraints: https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer</att>\n" +
"        <att name=\"platform\">Windows</att>\n" +
"        <att name=\"sourceUrl\">http://alaskafisheries.noaa.gov/arcgis/rest/services</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"subsetVariables\">Year, region, core, Trawl_Type, Pcod140_n, present, Variable, Description, Units, min, max</att>\n" + //2020-08-07 added min and max
"        <att name=\"summary\">This dataset is consists of point data taken from numerous depth surveys from the last century.  These data were processed and imported into a geographic information system (GIS) platform to form a bathymetric map of the ocean floor.  Approximately 18.6 billion depth data points were synthesized from various data sources that have been collected and archived since 1901 and includes lead line surveys, trackline geophysics, hydrographic surveys, gridded multi-beam NET CDF files, XYZ files, and MB-58 multi-beam files.  Bathymetric soundings from these datasets span almost all areas of the Arctic and includes Alaska and the surrounding international waters.  Most of the bathymetry data used for this effort is archived and maintained at the National Center for Environmental Information (National Centers for Environmental Information (NCEI)) https://www.ncei.noaa.gov.\n" +
"\n" +
"The purpose of our effort is to develop a high resolution bathymetry dataset for the entire Alaska Exclusive Economic Zone (AEEZ) and surrounding waters by combining and assimilating multiple sets of existing data from historical and recent ocean depth mapping surveys.</att>\n" +
"        <att name=\"time_coverage_begin\">2013</att>\n" +
"        <att name=\"title\">AKRO Analytical Team Metadata Portfolio, Bathymetry (Alaska and surrounding waters) (InPort #27377c1)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>Station_ID</sourceName>\n" +
"        <destinationName>Station_ID</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station ID</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Year</sourceName>\n" +
"        <destinationName>Year</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Year</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>region</sourceName>\n" +
"        <destinationName>region</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Region</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>AVE_LAT</sourceName>\n" +
"        <destinationName>AVE_LAT</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">AVE LAT</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>AVE_LONG</sourceName>\n" +
"        <destinationName>AVE_LONG</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">AVE LONG</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>avg depth</sourceName>\n" +
"        <destinationName>avg_depth</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Avg Depth</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>avg temp</sourceName>\n" +
"        <destinationName>avg_temp</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Avg Temp</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>core</sourceName>\n" +
"        <destinationName>core</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Core</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Effort_Area_km^2</sourceName>\n" +
"        <destinationName>Effort_Area_km_2</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Effort Area Km^2</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Effort_Volume_km^3</sourceName>\n" +
"        <destinationName>Effort_Volume_km_3</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Effort Volume Km^3</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Trawl_Type</sourceName>\n" +
"        <destinationName>Trawl_Type</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Trawl Type</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Pcod140_n</sourceName>\n" +
"        <destinationName>Pcod140_n</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"long_name\">Pcod140 N</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>present</sourceName>\n" +
"        <destinationName>present</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Present</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>CPUE km2</sourceName>\n" +
"        <destinationName>CPUE_km2</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">CPUE Km2</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>CPUE km3</sourceName>\n" +
"        <destinationName>CPUE_km3</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">CPUE Km3</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cpue_tow2</sourceName>\n" +
"        <destinationName>cpue_tow2</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cpue Tow2</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cpue_tow3</sourceName>\n" +
"        <destinationName>cpue_tow3</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cpue Tow3</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Variable</sourceName>\n" +
"        <destinationName>Variable</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Variable</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Description</sourceName>\n" +
"        <destinationName>Description</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Description</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Units</sourceName>\n" +
"        <destinationName>Units</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Units</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>min</sourceName>\n" +
"        <destinationName>min</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Min</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>max</sourceName>\n" +
"        <destinationName>max</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Max</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n\n\n";

        Test.ensureEqual(results, expected, "results=\n" + results);

        Test.ensureEqual(gdxResults, expected, "gdxResults=\n" + gdxResults);

    }

    /**
     * This tests GenerateDatasetsXml with EDDTableFromInPort when there are child entities. 
     * This is the dataset Nazila requested (well, she requested 12866 and this=26938, but
     * 12866 has no data).
     */
    public static void testGenerateDatasetsXmlFromInPort2() throws Throwable {
        String2.log("\n*** EDDTableFromAsciiFiles.testGenerateDatasetsXmlFromInPort2()\n");
        testVerboseOn();
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String xmlFile = "/u00/data/points/inportXml/NOAA/NMFS/AFSC/inport-xml/xml/26938.xml";
        String dataDir = "/u00/data/points/inportData/";

        String fileName = "";
        int whichChild = 0;

        String results = generateDatasetsXmlFromInPort(
            xmlFile, "", ".*", whichChild, dataDir, fileName, -1) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromInPort",
            xmlFile, "", "" + whichChild, dataDir, fileName, "-1"}, //defaultStandardizeWhat
            false); //doIt loop?

String expected = 
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"afscInPort26938\" active=\"true\">\n" +
"    <defaultGraphQuery>&amp;.marker=1|5</defaultGraphQuery>\n" +
"    <fileDir>/u00/data/points/inportData/26938/</fileDir>\n" +
"    <fileNameRegex>???</fileNameRegex>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>-1</updateEveryNMillis>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <fgdcFile>/u00/data/points/inportXml/NOAA/NMFS/AFSC/fgdc/xml/26938.xml</fgdcFile>\n" +
"    <iso19115File>/u00/data/points/inportXml/NOAA/NMFS/AFSC/iso19115/xml/26938.xml</iso19115File>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"acknowledgment\">Field collection of data was conducted as part of the Bering-Aleutian Salmon International Survey and was supported in part by the Bering Sea Fishermen&#39;s Association, The Arctic Yukon Kuskokwim Sustainable Salmon Initiative, and the Bering Sea Integrated Ecosystem Research Program. Data analysis was supported in part by a grant from the North Pacific Research Board (#R0816) and published in publication #325.</att>\n" +
"        <att name=\"archive_location\">NCEI-MD</att>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">thomas.hurst@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Thomas Hurst</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">https://www.afsc.noaa.gov</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">70.05075</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">54.4715</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">-158.97892</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-174.08267</att>\n" +
"        <att name=\"history\">archive_location=NCEI-MD\n" +
"Lineage Statement: The late summer distribution of age-0 Pacific cod in the eastern Bering Sea was described for six cohorts (2004-2009), based on trawl catches in the Bering-Aleutian Salmon International Survey (BASIS).\n" +
"Lineage Source #1, title=Distributional patterns of 0-group Pacific cod (Gadus macrocephalus) in the eastern Bering Sea under variable recruitment and thermal conditions, publisher=ICES Journal of Marine Science\n" +
"Lineage Step #1: Trawl survey\n" +
"Lineage Step #2: Cohort strength estimates\n" +
"Lineage Step #3: Thermal regime description\n" +
"Lineage Step #4: Analysis of distribution\n" +
"2015-09-10T12:44:50Z Nancy Roberson originally created InPort catalog-item-id #26938.\n" +
"2017-03-01T12:53:25Z Jeremy Mays last modified InPort catalog-item-id #26938.\n" +
today + " GenerateDatasetsXml in ERDDAP v2.10 (contact: bob.simons@noaa.gov) converted inport-xml metadata from https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport-xml/xml/26938.xml into an ERDDAP dataset description.</att>\n" +
"        <att name=\"infoUrl\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport-xml/xml/26938.xml</att>\n" +
"        <att name=\"InPort_child_item_1_catalog_id\">26939</att>\n" +
"        <att name=\"InPort_child_item_1_item_type\">Entity</att>\n" +
"        <att name=\"InPort_child_item_1_title\">Pacific cod distribution</att>\n" +
"        <att name=\"InPort_data_quality_accuracy\">See Hurst, T.P., Moss, J.H., Miller, J.A., 2012. Distributional patterns of 0-group Pacific cod (Gadus macrocephalus) in the eastern Bering Sea under variable recruitment and thermal conditions. ICES Journal of Marine Science, 69: 163-174</att>\n" +
"        <att name=\"InPort_data_quality_control_procedures\">Data was checked for outliers.</att>\n" +
"        <att name=\"InPort_dataset_maintenance_frequency\">None planned</att>\n" +
"        <att name=\"InPort_dataset_presentation_form\">Table (digital)</att>\n" +
"        <att name=\"InPort_dataset_publication_status\">Published</att>\n" +
"        <att name=\"InPort_dataset_publish_date\">2012</att>\n" +
"        <att name=\"InPort_dataset_type\">MS Excel Spreadsheet</att>\n" +
"        <att name=\"InPort_distribution_1_download_url\">https://noaa-fisheries-afsc.data.socrata.com/Ecosystem-Science/AFSC-RACE-FBEP-Hurst-Distributional-patterns-of-0-/e7r7-2x38</att>\n" +
"        <att name=\"InPort_entity_1_abstract\">Distribution data for age-0 Pacific cod in the eastern Bering Sea, based on catches in the Bering-Aleutian Salmon International Survey (BASIS)</att>\n" +
"        <att name=\"InPort_entity_1_item_id\">26939</att>\n" +
"        <att name=\"InPort_entity_1_metadata_workflow_state\">Published / External</att>\n" +
"        <att name=\"InPort_entity_1_status\">Complete</att>\n" +
"        <att name=\"InPort_entity_1_title\">Pacific cod distribution</att>\n" +
"        <att name=\"InPort_fishing_gear\">Midwater trawl</att>\n" +
"        <att name=\"InPort_item_id\">26938</att>\n" +
"        <att name=\"InPort_item_type\">Data Set</att>\n" +
"        <att name=\"InPort_metadata_record_created\">2015-09-10T12:44:50Z</att>\n" +
"        <att name=\"InPort_metadata_record_created_by\">Nancy Roberson</att>\n" +
"        <att name=\"InPort_metadata_record_last_modified\">2017-03-01T12:53:25Z</att>\n" +
"        <att name=\"InPort_metadata_record_last_modified_by\">Jeremy Mays</att>\n" +
"        <att name=\"InPort_metadata_workflow_state\">Published / External</att>\n" +
"        <att name=\"InPort_owner_organization_acronym\">AFSC</att>\n" +
"        <att name=\"InPort_parent_item_id\">22355</att>\n" +
"        <att name=\"InPort_publication_status\">Public</att>\n" +
"        <att name=\"InPort_status\">Complete</att>\n" +
"        <att name=\"InPort_support_role_1_organization\">Alaska Fisheries Science Center</att>\n" +
"        <att name=\"InPort_support_role_1_organization_url\">https://www.afsc.noaa.gov</att>\n" +
"        <att name=\"InPort_support_role_1_person\">Thomas Hurst</att>\n" +
"        <att name=\"InPort_support_role_1_person_email\">thomas.hurst@noaa.gov</att>\n" +
"        <att name=\"InPort_support_role_1_type\">Point of Contact</att>\n" +
"        <att name=\"InPort_technical_environment\">Datasets were created using Microsoft Excel.</att>\n" +
"        <att name=\"InPort_url_1_type\">Online Resource</att>\n" +
"        <att name=\"InPort_url_1_url\">https://academic.oup.com/icesjms/content/69/2/163.full</att>\n" +
"        <att name=\"InPort_xml_url\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport-xml/xml/26938.xml</att>\n" +
"        <att name=\"institution\">NOAA NMFS AFSC</att>\n" +
"        <att name=\"instrument\">CTD</att>\n" +
"        <att name=\"keywords\">2004-2009, afsc, alaska, analyzed, based, bering, bering sea, center, cod, cohorts, data, dataset, density, density-dependence, dependence, distribution, eastern, fisheries, habitat, juvenile, late, marine, national, nmfs, noaa, ocean, oceans, pacific, pacific cod, science, sea, service, study, summer, temperature</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">Data access constraints: No restriction for accessing this dataset\n" +
"Data use constraints: Must cite originator if used in publications, reports, presentations, etc., and must understand metadata prior to use.</att>\n" +
"        <att name=\"platform\">Fishing vessel</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"summary\">This dataset is from a study that analyzed the late summer distribution of juvenile Pacific cod in the eastern Bering Sea for 6 cohorts (2004-2009), based on catches in the Bering-Aleutian Salmon International Survey (BASIS).\n" +
"\n" +
"The purpose of this study was to examine distributional patterns of juvenile Pacific cod during a period of sginificant variation in cohort strength and thermal regime in the Bering Sea, which allowed the consideration of potential density-dependent effects and climate-induced changes in distribution at the northern limit of the species&#39; range, and evaluation of local scale habitat selection in relation to fish density and water temperature.</att>\n" +
"        <att name=\"time_coverage_begin\">2004</att>\n" +
"        <att name=\"time_coverage_end\">2009</att>\n" +
"        <att name=\"title\">Fisheries Behavioral Ecology Program, AFSC/RACE/FBEP/Hurst: Distributional patterns of 0-group Pacific cod (Gadus macrocephalus) in the eastern Bering Sea under variable recruitment and thermal conditions (InPort #26938)</att>\n" +
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

        Test.ensureEqual(results, expected, "results=\n" + results);

        Test.ensureEqual(gdxResults, expected, "gdxResults=\n" + gdxResults);


        fileName = "AFSC_RACE_FBEP_Hurst__Distributional_patterns_of_0-group_Pacific_cod__Gadus_macrocephalus__in_the_eastern_Bering_Sea_under_variable_recruitment_and_thermal_conditions.csv";
        whichChild = 1;

        results = generateDatasetsXmlFromInPort(
            xmlFile, "", ".*", whichChild, dataDir, fileName, -1) + "\n";

        //GenerateDatasetsXml
        gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromInPort",
            xmlFile, "", "" + whichChild, dataDir, fileName, "-1"}, //defaultStandardizeWhat
            false); //doIt loop?

expected = 
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"afscInPort26938ce26939\" active=\"true\">\n" +
"    <defaultGraphQuery>&amp;.marker=1|5</defaultGraphQuery>\n" +
"    <fileDir>/u00/data/points/inportData/26938/</fileDir>\n" +
"    <fileNameRegex>AFSC_RACE_FBEP_Hurst__Distributional_patterns_of_0-group_Pacific_cod__Gadus_macrocephalus__in_the_eastern_Bering_Sea_under_variable_recruitment_and_thermal_conditions\\.csv</fileNameRegex>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>-1</updateEveryNMillis>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <fgdcFile>/u00/data/points/inportXml/NOAA/NMFS/AFSC/fgdc/xml/26938.xml</fgdcFile>\n" +
"    <iso19115File>/u00/data/points/inportXml/NOAA/NMFS/AFSC/iso19115/xml/26938.xml</iso19115File>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"acknowledgment\">Field collection of data was conducted as part of the Bering-Aleutian Salmon International Survey and was supported in part by the Bering Sea Fishermen&#39;s Association, The Arctic Yukon Kuskokwim Sustainable Salmon Initiative, and the Bering Sea Integrated Ecosystem Research Program. Data analysis was supported in part by a grant from the North Pacific Research Board (#R0816) and published in publication #325.</att>\n" +
"        <att name=\"archive_location\">NCEI-MD</att>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">thomas.hurst@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Thomas Hurst</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">https://www.afsc.noaa.gov</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">70.05075</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">54.4715</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">-158.97892</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-174.08267</att>\n" +
"        <att name=\"history\">archive_location=NCEI-MD\n" +
"Lineage Statement: The late summer distribution of age-0 Pacific cod in the eastern Bering Sea was described for six cohorts (2004-2009), based on trawl catches in the Bering-Aleutian Salmon International Survey (BASIS).\n" +
"Lineage Source #1, title=Distributional patterns of 0-group Pacific cod (Gadus macrocephalus) in the eastern Bering Sea under variable recruitment and thermal conditions, publisher=ICES Journal of Marine Science\n" +
"Lineage Step #1: Trawl survey\n" +
"Lineage Step #2: Cohort strength estimates\n" +
"Lineage Step #3: Thermal regime description\n" +
"Lineage Step #4: Analysis of distribution\n" +
"2015-09-10T12:44:50Z Nancy Roberson originally created InPort catalog-item-id #26938.\n" +
"2017-03-01T12:53:25Z Jeremy Mays last modified InPort catalog-item-id #26938.\n" +
today + " GenerateDatasetsXml in ERDDAP v2.10 (contact: bob.simons@noaa.gov) converted inport-xml metadata from https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport-xml/xml/26938.xml into an ERDDAP dataset description.</att>\n" +
"        <att name=\"infoUrl\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport-xml/xml/26938.xml</att>\n" +
"        <att name=\"InPort_data_quality_accuracy\">See Hurst, T.P., Moss, J.H., Miller, J.A., 2012. Distributional patterns of 0-group Pacific cod (Gadus macrocephalus) in the eastern Bering Sea under variable recruitment and thermal conditions. ICES Journal of Marine Science, 69: 163-174</att>\n" +
"        <att name=\"InPort_data_quality_control_procedures\">Data was checked for outliers.</att>\n" +
"        <att name=\"InPort_dataset_maintenance_frequency\">None planned</att>\n" +
"        <att name=\"InPort_dataset_presentation_form\">Table (digital)</att>\n" +
"        <att name=\"InPort_dataset_publication_status\">Published</att>\n" +
"        <att name=\"InPort_dataset_publish_date\">2012</att>\n" +
"        <att name=\"InPort_dataset_type\">MS Excel Spreadsheet</att>\n" +
"        <att name=\"InPort_distribution_download_url\">https://noaa-fisheries-afsc.data.socrata.com/Ecosystem-Science/AFSC-RACE-FBEP-Hurst-Distributional-patterns-of-0-/e7r7-2x38</att>\n" +
"        <att name=\"InPort_fishing_gear\">Midwater trawl</att>\n" +
"        <att name=\"InPort_item_id\">26938</att>\n" +
"        <att name=\"InPort_item_type\">Data Set</att>\n" +
"        <att name=\"InPort_metadata_record_created\">2015-09-10T12:44:50Z</att>\n" +
"        <att name=\"InPort_metadata_record_created_by\">Nancy Roberson</att>\n" +
"        <att name=\"InPort_metadata_record_last_modified\">2017-03-01T12:53:25Z</att>\n" +
"        <att name=\"InPort_metadata_record_last_modified_by\">Jeremy Mays</att>\n" +
"        <att name=\"InPort_metadata_workflow_state\">Published / External</att>\n" +
"        <att name=\"InPort_owner_organization_acronym\">AFSC</att>\n" +
"        <att name=\"InPort_parent_item_id\">22355</att>\n" +
"        <att name=\"InPort_publication_status\">Public</att>\n" +
"        <att name=\"InPort_status\">Complete</att>\n" +
"        <att name=\"InPort_support_role_1_organization\">Alaska Fisheries Science Center</att>\n" +
"        <att name=\"InPort_support_role_1_organization_url\">https://www.afsc.noaa.gov</att>\n" +
"        <att name=\"InPort_support_role_1_person\">Thomas Hurst</att>\n" +
"        <att name=\"InPort_support_role_1_person_email\">thomas.hurst@noaa.gov</att>\n" +
"        <att name=\"InPort_support_role_1_type\">Point of Contact</att>\n" +
"        <att name=\"InPort_technical_environment\">Datasets were created using Microsoft Excel.</att>\n" +
"        <att name=\"InPort_url_1_type\">Online Resource</att>\n" +
"        <att name=\"InPort_url_1_url\">https://academic.oup.com/icesjms/content/69/2/163.full</att>\n" +
"        <att name=\"InPort_xml_url\">https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport-xml/xml/26938.xml</att>\n" +
"        <att name=\"institution\">NOAA NMFS AFSC</att>\n" +
"        <att name=\"instrument\">CTD</att>\n" +
"        <att name=\"keywords\">2004-2009, afsc, alaska, analyzed, area, ave, AVE_LAT, AVE_LONG, average, avg_temp, based, bering, bering sea, center, cod, cohorts, core, cpue, CPUE_km2, CPUE_km3, cpue_tow2, cpue_tow3, data, dataset, density, density-dependence, dependence, depth, distribution, eastern, effort, Effort_Area_km_2, Effort_Volume_km_3, fisheries, habitat, identifier, juvenile, km2, km3, km^2, km^3, late, long, marine, national, nmfs, noaa, ocean, oceans, pacific, pacific cod, pcod140, Pcod140_n, present, region, science, sea, service, station, Station_ID, statistics, study, summer, temperature, time, tow2, tow3, trawl, Trawl_Type, type, volume, year</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">Data access constraints: No restriction for accessing this dataset\n" +
"Data use constraints: Must cite originator if used in publications, reports, presentations, etc., and must understand metadata prior to use.</att>\n" +
"        <att name=\"platform\">Fishing vessel</att>\n" +
"        <att name=\"sourceUrl\">https://noaa-fisheries-afsc.data.socrata.com/Ecosystem-Science/AFSC-RACE-FBEP-Hurst-Distributional-patterns-of-0-/e7r7-2x38</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"subsetVariables\">Year, region, core, Trawl_Type, Pcod140_n, present</att>\n" +
"        <att name=\"summary\">This dataset is from a study that analyzed the late summer distribution of juvenile Pacific cod in the eastern Bering Sea for 6 cohorts (2004-2009), based on catches in the Bering-Aleutian Salmon International Survey (BASIS).\n" +
"\n" +
"The purpose of this study was to examine distributional patterns of juvenile Pacific cod during a period of sginificant variation in cohort strength and thermal regime in the Bering Sea, which allowed the consideration of potential density-dependent effects and climate-induced changes in distribution at the northern limit of the species&#39; range, and evaluation of local scale habitat selection in relation to fish density and water temperature.\n" +
"\n" +
"This sub-dataset has: Distribution data for age-0 Pacific cod in the eastern Bering Sea, based on catches in the Bering-Aleutian Salmon International Survey (BASIS)</att>\n" +
"        <att name=\"time_coverage_begin\">2004</att>\n" +
"        <att name=\"time_coverage_end\">2009</att>\n" +
"        <att name=\"title\">Fisheries Behavioral Ecology Program, AFSC/RACE/FBEP/Hurst: Distributional patterns of 0-group Pacific cod (Gadus macrocephalus) in the eastern Bering Sea under variable recruitment and thermal conditions, Pacific cod distribution (InPort #26938ce26939)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>Station_ID</sourceName>\n" +
"        <destinationName>Station_ID</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n" +
"            <att name=\"allowed_values\">20041001 - 20095052</att>\n" +
"            <att name=\"comment\">Unique identifier for each trawl station</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station ID</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Year</sourceName>\n" +
"        <destinationName>Year</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"allowed_values\">2004 - 2009</att>\n" +
"            <att name=\"comment\">Survey year</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Year</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>region</sourceName>\n" +
"        <destinationName>region</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">North = North, NM = North-Middle, SM = South-Middle, Slope = Outer shelf, Kusk = Kuskokwim, Bristol = Bristol Bay</att>\n" +
"            <att name=\"comment\">Code for geographic zone</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Region</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>AVE_LAT</sourceName>\n" +
"        <destinationName>AVE_LAT</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"allowed_values\">54.4715 - 70.05075</att>\n" +
"            <att name=\"comment\">Latitude of mid-point of trawl</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">AVE LAT</att>\n" +
"            <att name=\"units\">degrees</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>AVE_LONG</sourceName>\n" +
"        <destinationName>AVE_LONG</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">-174.083 - -158.979</att>\n" +
"            <att name=\"comment\">Longitude of mid-point of trawl</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">AVE LONG</att>\n" +
"            <att name=\"units\">degrees</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>avg depth</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">14 - 1285</att>\n" +
"            <att name=\"comment\">Average of water depths at starting and ending positions</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Avg Depth</att>\n" +
"            <att name=\"source_name\">avg depth</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>avg temp</sourceName>\n" +
"        <destinationName>avg_temp</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"allowed_values\">3 - 16.62</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"comment\">Average of surface temperatures at beginning and ending positions</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Avg Temp</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>core</sourceName>\n" +
"        <destinationName>core</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"allowed_values\">0 - 4</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">4</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Code for frequency of sampling specific grid point</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Core</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Effort_Area_km^2</sourceName>\n" +
"        <destinationName>Effort_Area_km_2</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">0.126399 - 0.349701</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">0.4</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Area fished by trawl: length of trawl x net spread</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Effort Area Km^2</att>\n" +
"            <att name=\"units\">square kilometers</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Effort_Volume_km^3</sourceName>\n" +
"        <destinationName>Effort_Volume_km_3</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">0.0014 - 0.00574</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">0.006</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Area fished by trawl: length of trawl x net spread x vertical opening</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Effort Volume Km^3</att>\n" +
"            <att name=\"units\">cubic kilometers</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Trawl_Type</sourceName>\n" +
"        <destinationName>Trawl_Type</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">Surface = net towed at surface</att>\n" +
"            <att name=\"comment\">Position of net in water column</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Trawl Type</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Pcod140_n</sourceName>\n" +
"        <destinationName>Pcod140_n</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"allowed_values\">0 - 8438</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">9000</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Count of Pacific cod less than 140 mm FL</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"long_name\">Pcod140 N</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>present</sourceName>\n" +
"        <destinationName>present</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"allowed_values\">0 - 1</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">1</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Code for positive capture of Pacific cod less than 140 mm FL: 0 = not present, 1 = present</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Present</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>CPUE km2</sourceName>\n" +
"        <destinationName>CPUE_km2</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">0 - 36817.42</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">40000</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Catch of Pacific cod per square kilometer</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">CPUE Km2</att>\n" +
"            <att name=\"units\">fish per square kilometer</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>CPUE km3</sourceName>\n" +
"        <destinationName>CPUE_km3</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">0 - 2045412</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">2000000</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Catch of Pacific cod per cubic kilometer</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">CPUE Km3</att>\n" +
"            <att name=\"units\">fish per cubic kilometer</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cpue_tow2</sourceName>\n" +
"        <destinationName>cpue_tow2</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">0 - 9057.085</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">10000</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Catch of Pacific cod corrected to standard tow of 0.246 square kilometers</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cpue Tow2</att>\n" +
"            <att name=\"units\">fish per tow</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cpue_tow3</sourceName>\n" +
"        <destinationName>cpue_tow3</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"allowed_values\">0 - 7322.575</att>\n" +
//"            <att name=\"colorBarMaximum\" type=\"double\">10000</att>\n" +
//"            <att name=\"colorBarMinimum\" type=\"double\">0</att>\n" +
"            <att name=\"comment\">Catch of Pacific cod corrected to standard tow of 0.00358 cubic kilometers</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cpue Tow3</att>\n" +
"            <att name=\"units\">fish per tow</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        Test.ensureEqual(gdxResults, expected, "gdxResults=\n" + gdxResults);
    }

    /** 
     * This tests actual_range (should not be set) and 
     * accessible values for non-iso string time values. */
    public static void testTimeRange() throws Throwable {

        String2.log("\n*** EDDTableFromAsciiFiles.testTimeRange()\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        String id = "knb_lter_sbc_14_t1"; //has MM/dd/yyyy time strings
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //test getting das for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_ttr", ".das"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        expected = 
"Attributes {\n" +
" s {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    String axis \"T\";\n" +  //no actual_range
"    String columnNameInSourceFile \"DATE_OF_SURVEY\";\n" +
"    String comment \"In the source file: The Date of the aerial survey\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  region {\n";
        results = results.substring(0, expected.length());
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test getting min and max time values
        tName = eddTable.makeNewFileForDapQuery(null, null, 
            "time&orderByMinMax(\"time\")", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_ttr", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        expected = 
"time\n" +
"UTC\n" +
"1957-08-13T00:00:00Z\n" +
"2007-04-28T00:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
    }

    /** 
     * This tests actual_range (should not be set) and 
     * accessible values for iso string time values. */
    public static void testTimeRange2() throws Throwable {

        String2.log("\n*** EDDTableFromAsciiFiles.testTimeRange2()\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        String id = "knb_lter_sbc_15_t1"; //has yyyy-MM-dd time strings
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //test getting das for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_ttr2", ".das"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        expected = 
"Attributes {\n" +
" s {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 9.682848e+8, 1.4694912e+9;\n" +  //has actual_range
"    String axis \"T\";\n" +
"    String columnNameInSourceFile \"DATE\";\n" +
"    String comment \"In the source file: Date of data collection in format: YYYY-MM-DD\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n";
        results = results.substring(0, expected.length());
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test getting min and max time values
        tName = eddTable.makeNewFileForDapQuery(null, null, 
            "time&orderByMinMax(\"time\")", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_ttr", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        expected = 
"time\n" +
"UTC\n" +
"2000-09-07T00:00:00Z\n" +
"2016-07-26T00:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //do numeric and string min/max time agree?
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(9.682848e+8),  "2000-09-07T00:00:00Z", "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.4694912e+9), "2016-07-26T00:00:00Z", "");

        //data file is /u00/data/points/lterSbc/cover_all_years_20160907.csv
        //The file confirms that is probably the range (there's no data before 2000).
    }


    //Adam says "if there are non 7-bit ASCII chars in our JSON, 
    //  they will be encoded as \\uxxxx"
    //  and tsv are "us-ascii".
    //so safe to use ISO_8859_1 or UTF_8 to decode them.
    public static final String bcodmoCharset = String2.ISO_8859_1;

    /**
     * This is a helper for generateDatasetsXmlFromBCODMO.
     *
     * @param ds 
     * @param dsDir
     * @param name e.g., "parameters"
     * @param useLocalFilesIfPossible
     * @return a JSONArray (or null if 'name'_service) not defined
     */
    public static JSONArray getBCODMOSubArray(JSONObject ds, String dsDir,
        String name, boolean useLocalFilesIfPossible) throws Exception{

        try {
            String serviceName = name + "_service";
            if (!ds.has(serviceName))
                return null;
            String subUrl = ds.getString(serviceName);
            if (!String2.isSomething(subUrl))
                return null;
            String subFileName = dsDir + name + ".json";
            if (!useLocalFilesIfPossible || !File2.isFile(subFileName))
                SSR.downloadFile(subUrl, subFileName, true); //tryToCompress
            String subContent[] = String2.readFromFile(subFileName, bcodmoCharset);
            Test.ensureEqual(subContent[0], "", "");
            JSONTokener subTokener       = new JSONTokener(subContent[1]);
            JSONObject  subOverallObject = new JSONObject( subTokener);
            if (!subOverallObject.has(name))
                return null;
            return subOverallObject.getJSONArray(name);
        } catch (Exception e) {
            String2.log("ERROR while getting " + name + "_service:\n" + 
                MustBe.throwableToString(e));
            return null;
        }
    }

    /**
     * This makes chunks of datasets.xml for datasets from BCO-DMO.
     * It gets info from a BCO-DMO JSON service that Adam Shepherd 
     * (ashepherd at whoi.edu) set up for Bob Simons.
     *
     * @param useLocalFilesIfPossible
     * @param catalogUrl e.g., https://www.bco-dmo.org/erddap/datasets
     * @param baseDir e.g, /u00/data/points/bcodmo/
     * @param datasetNumberRegex   .* for all or e.g., (549122|549123)
     * @throws Throwable if trouble with outer catalog, but individual dataset
     *   exceptions are caught and logged.
     */
    public static String generateDatasetsXmlFromBCODMO(boolean useLocalFilesIfPossible,
        String catalogUrl, String baseDir, String datasetNumberRegex, int tStandardizeWhat)
        throws Throwable {

        baseDir = File2.addSlash(baseDir); //ensure trailing slash
        String2.log("\n*** EDDTableFromAsciiFiles.generateDatasetsXmlFromBCODMO\n" +
            "url=" + catalogUrl + 
            "\ndir=" + baseDir);
        long time = System.currentTimeMillis();
        File2.makeDirectory(baseDir);
        StringBuffer results = new StringBuffer();
        StringArray noTime = new StringArray();
        tStandardizeWhat = tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE?
            DEFAULT_STANDARDIZEWHAT : tStandardizeWhat;

        //for the sub files
        JSONArray subArray;
        JSONObject subObject;

        //get the main catalog and parse it
        String catalogName = baseDir + "catalog.json"; 
        if (!useLocalFilesIfPossible || !File2.isFile(catalogName))
            SSR.downloadFile(catalogUrl, catalogName, true); //tryToCompress
        String catalogContent[] = String2.readFromFile(catalogName, bcodmoCharset);
        Test.ensureEqual(catalogContent[0], "", "");
        JSONTokener tokener = new JSONTokener(catalogContent[1]);
        JSONObject catalogObject = new JSONObject(tokener);
        JSONArray datasetsArray = catalogObject.getJSONArray("datasets");

        int nMatching = 0;
        int nSucceeded = 0; 
        int nFailed = 0; 
        //Tally charTally = new Tally();
        String2.log("datasetsArray has " + datasetsArray.length() + " datasets."); 
        for (int dsi = 0; dsi < datasetsArray.length(); dsi++) {
            try {
                JSONObject ds = datasetsArray.getJSONObject(dsi);

                //"dataset":"http:\/\/lod.bco-dmo.org\/id\/dataset\/549122",
                //Do FIRST to see if it matches regex
                String dsNumber = File2.getNameAndExtension(ds.getString("dataset"));  //549122
                if (!dsNumber.matches(datasetNumberRegex))
                    continue;
                String2.log("\nProcessing #" + dsi + ": bcodmo" + dsNumber); 
                nMatching++;
                Attributes gatts = new Attributes();
                gatts.add("BCO_DMO_dataset_ID", dsNumber);

                //"version_date":"2015-02-17",
                String versionDate = "";
                String compactVersionDate = "";
                if (ds.has(   "version_date")) {
                    versionDate = ds.getString("version_date");
                    gatts.add("date_created", versionDate);
                    gatts.add("version_date", versionDate);
                    compactVersionDate = "v" + String2.replaceAll(versionDate, "-", "");
                }

                //Adam says dsNumber+compactVersionDate is a unique dataset identifier.
                String tDatasetID = "bcodmo" + dsNumber + compactVersionDate; 
                String dsDir = baseDir + dsNumber + compactVersionDate + "/";
                File2.makeDirectory(dsDir);                

                //standard things
                gatts.add("id", tDatasetID);
                gatts.add("naming_authority", "org.bco-dmo");
                gatts.add("institution",      "BCO-DMO");
                gatts.add("keywords", 
                    "Biological, Chemical, Oceanography, Data, Management, Office, " +
                    "BCO-DMO, NSF");
                gatts.add("publisher_name",   "BCO-DMO");
                gatts.add("publisher_email",  "info@bco-dmo.org");
                gatts.add("publisher_type",   "institution");
                gatts.add("publisher_url",    "http://www.bco-dmo.org/");

                //"doi":"10.1575\/1912\/bco-dmo.641155",
                //Adam says "The uniqueness of records will be by this DOI 
                //  (which is a proxy for the 'dataset' and 'version_date' keys combined)."
                if (ds.has("doi")) 
                    gatts.add("doi", ds.getString("doi"));

                //"landing_page":"http:\/\/www.bco-dmo.org\/dataset\/549122",
                if (ds.has(   "landing_page")) {
                    gatts.add("infoUrl",      ds.getString("landing_page"));
                    //gatts.add("landing_page", ds.getString("landing_page"));
                }

                //"title":"Cellular elemental content of individual phytoplankton cells collected during US GEOTRACES North Atlantic Transect cruises in the Subtropical western and eastern North Atlantic Ocean during Oct and Nov, 2010 and Nov. 2011.",          
                String tTitle = "BCO-DMO " + dsNumber + 
                    (versionDate.length() == 0? "" : " " + compactVersionDate) +
                    (ds.has("title"       )? ": " + ds.getString("title") : 
                     ds.has("dataset_name")? ": " + ds.getString("dataset_name") : 
                     ds.has("brief_desc"  )? ": " + ds.getString("brief_desc") : 
                     "");
                gatts.add("title", tTitle);

                //"description" is info from landing_page
                //  ??? I need to extract "Related references" and "related image files"
                //  from html tags in "description"
                //"abstract": different from Description on landing_page
                //  "Phytoplankton contribute significantly to global C
                //  cycling and serve as the base of ocean food webs. 
                //  Phytoplankton require trace metals for growth and also mediate
                //  the vertical distributions of many metals in the ocean. This
                //  dataset provides direct measurements of metal quotas in 
                //  phytoplankton from across the North Atlantic Ocean, known to 
                //  be subjected to aeolian Saharan inputs and anthropogenic inputs from North America and Europe. Bulk particulate material and individual phytoplankton cells were collected from the upper water column (\u003C150 m) as part of the US GEOTRACES North Atlantic Zonal Transect cruises (KN199-4, KN199-5, KN204-1A,B). The cruise tracks spanned several ocean biomes and geochemical regions. Chemical leaches (to extract biogenic and otherwise labile particulate phases) are combined together with synchrotron X-ray fluorescence (SXRF) analyses of individual micro and nanophytoplankton to discern spatial trends across the basin. Individual phytoplankton cells were analyzed for elemental content using SXRF (Synchrotron radiation X-Ray Fluorescence). Carbon was calculated from biovolume using the relationships of Menden-Deuer \u0026 Lessard (2000).",            
                gatts.add("summary", 
                    ds.has("description")? XML.removeHTMLTags(ds.getString("description")):
                    ds.has("abstract")? ds.getString("abstract"): 
                    tTitle);

                //iso_19115_2
                String iso19115File = null;
                if (ds.has("dataset_iso")) {
                    try {
                        iso19115File = dsDir + "iso_19115_2.xml";
                        if (!useLocalFilesIfPossible || !File2.isFile(iso19115File))
                            SSR.downloadFile(ds.getString("dataset_iso"), 
                                iso19115File, true); //tryToUseCompression) 
                    } catch (Exception e) {
                        iso19115File = null;
                        String2.log("ERROR while getting iso_19115_2.xml file:\n" +
                            MustBe.throwableToString(e));
                    }
                }

                //"license":"http:\/\/creativecommons.org\/licenses\/by\/4.0\/",
                gatts.add("license", 
                    (ds.has("license")? ds.getString("license") + "\n" : "") +
                    //modified slightly from Terms of Use at http://www.bco-dmo.org/
                    "This data set is freely available as long as one follows the\n" +                   
                    "terms of use (http://www.bco-dmo.org/terms-use), including\n" +                   
                    "the understanding that any such use will properly acknowledge\n" +                   
                    "the originating Investigator. It is highly recommended that\n" +                   
                    "anyone wishing to use portions of this data should contact\n" +                   
                    "the originating principal investigator (PI).");

                //"filename":"GT10_11_cellular_element_quotas.tsv",
                //"download_url":"http:\/\/darchive.mblwhoilibrary.org\/bitstream\/handle\/
                //  1912\/7908\/1\/GT10_11_cellular_element_quotas.tsv",
                //"file_size_in_bytes":"70567",
                String sourceUrl = ds.getString("download_url");
                gatts.add("sourceUrl", sourceUrl);
                String fileName = File2.getNameAndExtension(sourceUrl);
                String tsvName = dsDir + fileName;
                if (!useLocalFilesIfPossible || !File2.isFile(tsvName))
                    SSR.downloadFile(sourceUrl, tsvName, true); //tryToCompress
                Table sourceTable = new Table();

                //look for colNamesRow after rows starting with "# ".
                //see /u00/data/points/bcodmo/488871_20140127/data_ctdmocness1.tsv
                //Adam says tsv files are US-ASCII chars only
                BufferedReader br = File2.getDecompressedBufferedFileReader(tsvName, bcodmoCharset);

                //skip "# " comment rows to get to columnNames row
                int colNamesRow = -1;
                while (true) {
                    colNamesRow++;
                    br.mark(10000); //max read-ahead bytes
                    String s = br.readLine();
                    if (s == null)
                        throw new Exception("The file contains only comment lines.");
                    if (s.startsWith("# ")) 
                        continue;
                    br.reset(); //go back to mark
                    break;
                }

                //read the data
                sourceTable.readASCII(tsvName, br, 
                    "", "", 0, 1, "\t",
                    null, null, null, null, false); //don't simplify until "nd" removed
                //custom alternative to sourceTable.convertIsSomething2(); //convert e.g., "nd" to ""
                for (int col = 0; col < sourceTable.nColumns(); col++) 
                    sourceTable.getColumn(col).switchFromTo("nd", ""); //the universal BCO-DMO missing value?
                sourceTable.simplify();  
                sourceTable.standardize(tStandardizeWhat); 

                Table addTable = (Table)(sourceTable.clone());
                addTable.globalAttributes().add(gatts);
                gatts = addTable.globalAttributes();

                if (ds.has(      "current_state")) //"Final no updates expected"
                    gatts.add(   "current_state", 
                    ds.getString("current_state"));

                if (ds.has(      "validated")) 
                    gatts.add(   "validated", "" + 
                    String2.parseBoolean(
                    ds.getString("validated"))); //0|1

                if (ds.has(      "restricted")) 
                    gatts.add(   "restricted", "" + 
                    String2.parseBoolean(
                    ds.getString("restricted"))); //0|1
               
                //"dataset_name":"GT10-11 - cellular element quotas",
                if (ds.has(      "dataset_name"))
                    gatts.add(   "dataset_name", 
                    ds.getString("dataset_name"));

                //"acquisition_desc":"\u003Cdiv xmlns=\u0022http:\/\/www.w3.org\/1999\/xhtml\u0022 
                //lang=\u0022en\u0022\u003E\u003Cp\u003ESXRF samples were prepared ...
                //... of Twining et al. (2011).\u003C\/p\u003E\u003C\/div\u003E",
                if (ds.has(      "acquisition_desc"))
                    gatts.add(   "acquisition_description", 
                    XML.removeHTMLTags(
                    ds.getString("acquisition_desc")));

                //"brief_desc":"Element quotas of individual phytoplankton cells",
                if (ds.has(      "brief_desc"))
                    gatts.add(   "brief_description", 
                    ds.getString("brief_desc"));

                //"processing_desc":"\u003Cdiv xmlns=\u0022http:\/\/www.w3.org\/1999\/xhtml\u0022
                //lang=\u0022en\u0022\u003E\u003Cp\u003EData were processed as ...
                //... via the join method.\u003C\/p\u003E\u003C\/div\u003E",
                if (ds.has(      "processing_desc"))
                    gatts.add(   "processing_description", 
                    XML.removeHTMLTags(
                    ds.getString("processing_desc")));

                //"parameters_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/parameters",
                subArray = getBCODMOSubArray(ds, dsDir, "parameters", useLocalFilesIfPossible);
                if (subArray != null) {
                    for (int sai = 0; sai < subArray.length(); sai++) {
                        subObject = subArray.getJSONObject(sai);

                        //"parameter_name":"cruise_id",
                        String colName = subObject.getString("parameter_name");
                        int col = addTable.findColumnNumber(colName);
                        if (col < 0) {
                            String2.log("WARNING: parameter_name=" + colName + 
                                " not found in " + tsvName);
                            continue;
                        }
                        Attributes colAtts = addTable.columnAttributes(col);

                        //"parameter":"http:\/\/lod.bco-dmo.org\/id\/dataset-parameter\/550520",
                        //SKIP since web page has ID# and info
                        //if (subObject.has(      "parameter")) 
                        //    colAtts.add(        "BCO_DMO_dataset_parameter_ID", 
                        //    File2.getNameAndExtension(
                        //    subObject.getString("parameter")));

                        //"units":"unitless",
                        if (subObject.has(  "units")) {
                            //will be cleaned up by makeReadyToUseAddVariableAttributes
                            String s = subObject.getString("units");
                            if (s != null || s.length() > 0)
                                colAtts.add("units", s);
                        }

                        //"data_type":"",  //always "". Adam says this is just a placeholder for now

                        //"desc":"cruise identification", //often long
                        if (subObject.has(      "desc")) 
                            colAtts.add(        "description", 
                            XML.removeHTMLTags(  //some are, some aren't
                            subObject.getString("desc")));

                        //"bcodmo_webpage":"http:\/\/www.bco-dmo.org\/dataset-parameter\/550520",
                        if (subObject.has(      "bcodmo_webpage")) 
                            colAtts.add(        "webpage", //??? "BCO_DMO_webpage",
                            subObject.getString("bcodmo_webpage"));

                        //"master_parameter":"http:\/\/lod.bco-dmo.org\/id\/parameter\/1102",
                        //"master_parameter_name":"cruise_id",
                        //"master_parameter_desc":"cruise designation; name", //often long

                        //"no_data_value":"nd",
                        //"master_parameter_no_data_value":"nd"},
                        //switchFromTo? no.  Leave source file unchanged
                        if (subObject.has("no_data_value")) {
                            String s = subObject.getString("no_data_value");
                            if (!"nd".equals(s) && !"".equals(s))
                                String2.log("WARNING: " + colName + " no_data_value=" + String2.toJson(s));
                        }
                    }
                }


                //"instruments_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/instruments",
                //550520 has only "self" and "previous"
                subArray = getBCODMOSubArray(ds, dsDir, "instruments", useLocalFilesIfPossible);
                if (subArray != null) {
                    for (int sai = 0; sai < subArray.length(); sai++) {
                        subObject = subArray.getJSONObject(sai);
                        String pre = "instrument_" + (sai + 1) + "_";

                        //"instrument":"http:\/\/lod.bco-dmo.org\/id\/dataset-instrument\/643392",
                        //SKIP since web page has ID# and info
                        //if (subObject.has(      "instrument")) 
                        //    gatts.add(pre +     "BCO_DMO_dataset_instrument_ID", 
                        //    File2.getNameAndExtension(
                        //    subObject.getString("instrument")));

                        //"instrument_name":"",
                        if (subObject.has(      "instrument_name")) 
                            gatts.add(pre +     "name", 
                            subObject.getString("instrument_name"));

                        //"desc":"",
                        if (subObject.has(      "desc")) 
                            gatts.add(pre +     "description", 
                            XML.removeHTMLTags(  //some are, some aren't
                            subObject.getString("desc")));

                        //"bcodmo_webpage":"http:\/\/www.bco-dmo.org\/dataset-instrument\/643392",
                        if (subObject.has(      "bcodmo_webpage")) 
                            gatts.add(pre +     "webpage", //??? "BCO_DMO_webpage", 
                            subObject.getString("bcodmo_webpage"));

                        //"instrument_type":"http:\/\/lod.bco-dmo.org\/id\/instrument\/411",

                        //"type_name":"GO-FLO Bottle",
                        if (subObject.has(      "type_name")) 
                            gatts.add(pre +     "type_name", 
                            subObject.getString("type_name"));

                        //"type_desc":"GO-FLO bottle cast used to collect water samples for pigment, nutrient, plankton, etc. The GO-FLO sampling bottle is specially designed to avoid sample contamination at the surface, internal spring contamination, loss of sample on deck (internal seals), and exchange of water from different depths."},
                        if (subObject.has(      "type_desc")) 
                            gatts.add(pre +     "type_description", 
                            XML.removeHTMLTags(  //some are, some aren't
                            subObject.getString("type_desc")));
                    }
                }

                //"people_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/people",
                subArray = getBCODMOSubArray(ds, dsDir, "people", useLocalFilesIfPossible);
                if (subArray != null) {
                    for (int sai = 0; sai < subArray.length(); sai++) {
                        subObject = subArray.getJSONObject(sai);
                        String pre = "person_" + (sai + 1) + "_";

                        //"person":"http:\/\/lod.bco-dmo.org\/id\/person\/51087",
                        //SKIP since web page has ID# and info
                        //if (subObject.has(      "person")) 
                        //    gatts.add(pre +     "BCO_DMO_person_ID", 
                        //    File2.getNameAndExtension(
                        //    subObject.getString("person")));

                        //"person_name":"Dr Benjamin Twining",
                        if (subObject.has(      "person_name")) 
                            gatts.add(pre +     "name", 
                            subObject.getString("person_name"));

                        //"bcodmo_webpage":"http:\/\/www.bco-dmo.org\/person\/51087",
                        if (subObject.has(      "bcodmo_webpage")) 
                            gatts.add(pre +     "webpage", //??? "BCO_DMO_webpage", 
                            subObject.getString("bcodmo_webpage"));

                        //"institution":"http:\/\/lod.bco-dmo.org\/id\/affiliation\/94",
                        //SKIP: the webpage, http://www.bco-dmo.org/affiliation/94
                        //  just has institution_name and list of people affiliated with it.
                        //  Institution name is the important thing.
                        //if (subObject.has(      "institution")) 
                        //    gatts.add(pre +     "BCO_DMO_affiliation_ID", 
                        //    File2.getNameAndExtension(
                        //    subObject.getString("institution")));

                        //"institution_name":"Bigelow Laboratory for Ocean Sciences (Bigelow)",
                        if (subObject.has(      "institution_name")) 
                            gatts.add(pre +     "institution_name", 
                            subObject.getString("institution_name"));

                        //"role_name":"Principal Investigator"},
                        if (subObject.has(      "role_name")) {
                            String role = subObject.getString("role_name");
                            gatts.add(pre +     "role", role);

                            if (gatts.getString("creator_name") == null &&
                                "Principal Investigator".equals(role)) {
                                gatts.add("creator_name", subObject.getString("person_name"));
                                gatts.add("creator_url",  subObject.getString("bcodmo_webpage"));
                                gatts.add("creator_type", "person");
                            }
                        }
                    }
                }

                //"deployments_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/deployments",
                subArray = getBCODMOSubArray(ds, dsDir, "deployments", useLocalFilesIfPossible);
                if (subArray != null) {
                    for (int sai = 0; sai < subArray.length(); sai++) {
                        subObject = subArray.getJSONObject(sai);
                        String pre = "deployment_" + (sai + 1) + "_";

                        //"deployment":"http:\/\/lod.bco-dmo.org\/id\/deployment\/58066",
                        //SKIP since web page has ID# and info
                        //if (subObject.has(      "deployment")) 
                        //    gatts.add(pre +     "BCO_DMO_deployment_ID", 
                        //    File2.getNameAndExtension( 
                        //    subObject.getString("deployment")));

                        //"title":"KN199-04",
                        if (subObject.has(      "title")) 
                            gatts.add(pre +     "title", 
                            subObject.getString("title"));

                        //"bcodmo_webpage":"http:\/\/www.bco-dmo.org\/deployment\/58066",
                        if (subObject.has(      "bcodmo_webpage")) 
                            gatts.add(pre +     "webpage", //??? "BCO_DMO_webpage", 
                            subObject.getString("bcodmo_webpage"));

                        //"description":"\u003Cdiv xmlns=\u0022http:\/\/www.w3.org\/1999\/xhtml\u0022 lang=\u0022en\u0022\u003E\u003Cp\u003EKN199-04 is the US GEOTRACES Zonal North Atlantic Survey Section cruise planned for late Fall 2010 from Lisboa, Portugal to Woods Hole, MA, USA.\u003C\/p\u003E\n\u003Cp\u003E4 November 2010 update: Due to engine failure, the scheduled science activities were canceled on 2 November 2010. On 4 November the R\/V KNORR put in at Porto Grande, Cape Verde and is scheduled to depart November 8, under the direction of Acting Chief Scientist Oliver Wurl of Old Dominion University. The objective of this leg is to carry the vessel in transit to Charleston, SC while conducting science activities modified from the original plan.\u003C\/p\u003E\n\u003Cp\u003EPlanned scientific activities and operations area during this transit will be as follows: the ship\u0027s track will cross from the highly productive region off West Africa into the oligotrophic central subtropical gyre waters, then across the western boundary current (Gulf Stream), and into the productive coastal waters of North America. During this transit, underway surface sampling will be done using the towed fish for trace metals, nanomolar nutrients, and arsenic speciation. In addition, a port-side high volume pumping system will be used to acquire samples for radium isotopes. Finally, routine aerosol and rain sampling will be done for trace elements. This section will provide important information regarding atmospheric deposition, surface transport, and transformations of many trace elements.\u003C\/p\u003E\n\u003Cp\u003EThe vessel is scheduled to arrive at the port of Charleston, SC, on 26 November 2010. The original cruise was intended to be 55 days duration with arrival in Norfolk, VA on 5 December 2010.\u003C\/p\u003E\n\u003Cp\u003Efunding: NSF OCE award 0926423\u003C\/p\u003E\n\u003Cp\u003E\u003Cstrong\u003EScience Objectives\u003C\/strong\u003E are to obtain state of the art trace metal and isotope measurements on a suite of samples taken on a mid-latitude zonal transect of the North Atlantic. In particular sampling will target the oxygen minimum zone extending off the west African coast near Mauritania, the TAG hydrothermal field, and the western boundary current system along Line W. In addition, the major biogeochemical provinces of the subtropical North Atlantic will be characterized. For additional information, please refer to the GEOTRACES program Web site (\u003Ca href=\u0022http:\/\/www.GEOTRACES.org\u0022\u003EGEOTRACES.org\u003C\/a\u003E) for overall program objectives and a summary of properties to be measured.\u003C\/p\u003E\n\u003Cp\u003E\u003Cstrong\u003EScience Activities\u003C\/strong\u003E include seawater sampling via GoFLO and Niskin carousels, in situ pumping (and filtration), CTDO2 and transmissometer sensors, underway pumped sampling of surface waters, and collection of aerosols and rain.\u003C\/p\u003E\n\u003Cp\u003EHydrography, CTD and nutrient measurements will be supported by the Ocean Data Facility (J. Swift) at Scripps Institution of Oceanography and funded through NSF Facilities. They will be providing an additional CTD rosette system along with nephelometer and LADCP. A trace metal clean Go-Flo Rosette and winch will be provided by the group at Old Dominion University (G. Cutter) along with a towed underway pumping system.\u003C\/p\u003E\n\u003Cp\u003EList of cruise participants: [ \u003Ca href=\u0022http:\/\/data.bcodmo.org\/US_GEOTRACES\/AtlanticSection\/GNAT_2010_cruiseParticipants.pdf\u0022\u003EPDF \u003C\/a\u003E]\u003C\/p\u003E\n\u003Cp\u003ECruise track: \u003Ca href=\u0022http:\/\/data.bcodmo.org\/US_GEOTRACES\/AtlanticSection\/KN199-04_crtrk.jpg\u0022 target=\u0022_blank\u0022\u003EJPEG image\u003C\/a\u003E (from Woods Hole Oceanographic Institution, vessel operator)\u003C\/p\u003E\n\u003Cp\u003EAdditional information may still be available from the vessel operator: \u003Ca href=\u0022http:\/\/www.whoi.edu\/cruiseplanning\/synopsis.do?id=581\u0022 target=\u0022_blank\u0022\u003EWHOI cruise planning synopsis\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003ECruise information and original data are available from the \u003Ca href=\u0022http:\/\/www.rvdata.us\/catalog\/KN199-04\u0022 target=\u0022_blank\u0022\u003ENSF R2R data catalog\u003C\/a\u003E.\u003C\/p\u003E\n\u003Cp\u003EADCP data are available from the Currents ADCP group at the University of Hawaii: \u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2010.html#kn199_4\u0022 target=\u0022_blank\u0022\u003EKN199-04 ADCP\u003C\/a\u003E\u003C\/p\u003E\u003C\/div\u003E",
                        if (subObject.has(      "description")) 
                            gatts.add(pre +     "description", 
                            XML.removeHTMLTags(
                            subObject.getString("description")));

                        //"location":"Subtropical northern Atlantic Ocean",
                        if (subObject.has(      "location")) 
                            gatts.add(pre +     "location", 
                            subObject.getString("location"));

                        //"start_date":"2010-10-15",
                        if (subObject.has(      "start_date")) 
                            gatts.add(pre +     "start_date", 
                            subObject.getString("start_date"));

                        //"end_date":"2010-11-04"},
                        if (subObject.has(      "end_date")) 
                            gatts.add(pre +     "end_date", 
                            subObject.getString("end_date"));

                    }
                }

                //"projects_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/projects"},
                subArray = getBCODMOSubArray(ds, dsDir, "projects", useLocalFilesIfPossible);
                if (subArray != null) {
                    for (int sai = 0; sai < subArray.length(); sai++) {
                        subObject = subArray.getJSONObject(sai);
                        String pre = "project_" + (sai + 1) + "_";

                        //"project":"http:\/\/lod.bco-dmo.org\/id\/project\/2066",
                        //SKIP since web page has ID# and info
                        //if (subObject.has(      "project")) 
                        //    gatts.add(pre +     "BCO_DMO_project_ID", 
                        //    File2.getNameAndExtension(
                        //    subObject.getString("project")));

                        //"created_date":"2010-06-09T17:40:05-04:00",
                        //"desc":"\u003Cdiv xmlns=\u0022http:\/\/www.w3.org\/1999\/xhtml\u0022 lang=\u0022en\u0022\u003E\u003Cp\u003E\u003Cem\u003EMuch of this text appeared in an article published in OCB News, October 2008, by the OCB Project Office.\u003C\/em\u003E\u003C\/p\u003E\n\u003Cp\u003EThe first U.S. GEOTRACES Atlantic Section will be specifically centered around a sampling cruise to be carried out in the North Atlantic in 2010. Ed Boyle (MIT) and Bill Jenkins (WHOI) organized a three-day planning workshop that was held September 22-24, 2008 at the Woods Hole Oceanographic Institution. The main goal of the workshop, sponsored by the National Science Foundation and the U.S. GEOTRACES Scientific Steering Committee, was to design the implementation plan for the first U.S. GEOTRACES Atlantic Section. The primary cruise design motivation was to improve knowledge of the sources, sinks and internal cycling of Trace Elements and their Isotopes (TEIs) by studying their distributions along a section in the North Atlantic (Figure 1). The North Atlantic has the full suite of processes that affect TEIs, including strong meridional advection, boundary scavenging and source effects, aeolian deposition, and the salty Mediterranean Outflow. The North Atlantic is particularly important as it lies at the \u0022origin\u0022 of the global Meridional Overturning Circulation.\u003C\/p\u003E\n\u003Cp\u003EIt is well understood that many trace metals play important roles in biogeochemical processes and the carbon cycle, yet very little is known about their large-scale distributions and the regional scale processes that affect them. Recent advances in sampling and analytical techniques, along with advances in our understanding of their roles in enzymatic and catalytic processes in the open ocean provide a natural opportunity to make substantial advances in our understanding of these important elements. Moreover, we are motivated by the prospect of global change and the need to understand the present and future workings of the ocean\u0027s biogeochemistry. The GEOTRACES strategy is to measure a broad suite of TEIs to constrain the critical biogeochemical processes that influence their distributions. In addition to these \u0022exotic\u0022 substances, more traditional properties, including macronutrients (at micromolar and nanomolar levels), CTD, bio-optical parameters, and carbon system characteristics will be measured. The cruise starts at Line W, a repeat hydrographic section southeast of Cape Cod, extends to Bermuda and subsequently through the North Atlantic oligotrophic subtropical gyre, then transects into the African coast in the northern limb of the coastal upwelling region. From there, the cruise goes northward into the Mediterranean outflow. The station locations shown on the map are for the \u0022fulldepth TEI\u0022 stations, and constitute approximately half of the stations to be ultimately occupied.\u003C\/p\u003E\n\u003Cp\u003E\u003Cem\u003EFigure 1. The proposed 2010 Atlantic GEOTRACES cruise track plotted on dissolved oxygen at 400 m depth. Data from the World Ocean Atlas (Levitus et al., 2005) were plotted using Ocean Data View (courtesy Reiner Schlitzer). [click on the image to view a larger version]\u003C\/em\u003E\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/GEOTRACES_Atl_stas.jpg\u0022 target=\u0022_blank\u0022\u003E\u003Cimg alt=\u0022\u0022 src=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/GEOTRACES_Atl_stas.jpg\u0022 style=\u0022width:350px\u0022 \/\u003E\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003EHydrography, CTD and nutrient measurements will be supported by the Ocean Data Facility (J. Swift) at Scripps Institution of Oceanography and funded through NSF Facilities. They will be providing an additional CTD rosette system along with nephelometer and LADCP. A trace metal clean Go-Flo Rosette and winch will be provided by the group at Old Dominion University (G. Cutter) along with a towed underway pumping system.\u003C\/p\u003E\n\u003Cp\u003EThe North Atlantic Transect cruise began in 2010 with KN199 leg 4 (station sampling) and leg 5 (underway sampling only) (Figure 2).\u003C\/p\u003E\n\u003Cp\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/\/US_GEOTRACES\/AtlanticSection\/Cruise_Report_for_Knorr_199_Final_v3.pdf\u0022 target=\u0022_blank\u0022\u003EKN199-04 Cruise Report (PDF)\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003E\u003Cem\u003EFigure 2. The red line shows the cruise track for the first leg of the US Geotraces North Atlantic Transect on the R\/V Knorr in October 2010.\u00a0 The rest of the stations (beginning with 13) will be completed in October-December 2011 on the R\/V Knorr (courtesy of Bill Jenkins, Chief Scientist, GNAT first leg). [click on the image to view a larger version]\u003C\/em\u003E\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/GNAT_stationPlan.jpg\u0022 target=\u0022_blank\u0022\u003E\u003Cimg alt=\u0022Atlantic Transect Station location map\u0022 src=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/GNAT_stationPlan_sm.jpg\u0022 style=\u0022width:350px\u0022 \/\u003E\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003EThe section completion effort resumed again in November 2011 with KN204-01A,B (Figure 3).\u003C\/p\u003E\n\u003Cp\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/\/US_GEOTRACES\/AtlanticSection\/Submitted_Preliminary_Cruise_Report_for_Knorr_204-01.pdf\u0022 target=\u0022_blank\u0022\u003EKN204-01A,B Cruise Report (PDF)\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003E\u003Cem\u003EFigure 3. Station locations occupied on the US Geotraces North Atlantic Transect on the R\/V Knorr in November 2011.\u00a0 [click on the image to view a larger version]\u003C\/em\u003E\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/KN204-01_Stations.png\u0022 target=\u0022_blank\u0022\u003E\u003Cimg alt=\u0022Atlantic Transect\/Part 2 Station location map\u0022 src=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/KN204-01_Stations.png\u0022 style=\u0022width:350px\u0022 \/\u003E\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003EData from the North Atlantic Transect cruises are available under the Datasets heading below, and consensus values for the SAFe and North Atlantic GEOTRACES Reference Seawater Samples are available from the GEOTRACES Program Office: \u003Ca href=\u0022http:\/\/www.geotraces.org\/science\/intercalibration\/322-standards-and-reference-materials?acm=455_215\u0022 target=\u0022_blank\u0022\u003EStandards and Reference Materials\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003E\u003Cstrong\u003EADCP data\u003C\/strong\u003E are available from the Currents ADCP group at the University of Hawaii at the links below:\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2010.html#kn199_4\u0022 target=\u0022_blank\u0022\u003EKN199-04\u003C\/a\u003E\u00a0\u00a0 (leg 1 of 2010 cruise; Lisbon to Cape Verde)\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2010.html#kn199_5\u0022 target=\u0022_blank\u0022\u003EKN199-05\u003C\/a\u003E\u00a0\u00a0 (leg 2 of 2010 cruise; Cape Verde to Charleston, NC)\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2011.html#kn204_01\u0022 target=\u0022_blank\u0022\u003EKN204-01A\u003C\/a\u003E (part 1 of 2011 cruise; Woods Hole, MA to Bermuda)\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2011.html#kn204_02\u0022 target=\u0022_blank\u0022\u003EKN204-01B\u003C\/a\u003E (part 2 of 2011 cruise; Bermuda to Cape Verde)\u003C\/p\u003E\u003C\/div\u003E",
                        if (subObject.has(      "desc")) {
                            String s = XML.removeHTMLTags(subObject.getString("desc"));
                            s = String2.replaceAll(s, "[click on the image to view a larger version]", "");
                            gatts.add(pre +     "description", s);
                        }

                        //"last_modified_date":"2016-02-17T11:37:46-05:00",
                        //"project_title":"U.S. GEOTRACES North Atlantic Transect",
                        if (subObject.has(      "project_title")) 
                            gatts.add(pre +     "title", 
                            subObject.getString("project_title"));

                        //"bcodmo_webpage":"http:\/\/www.bco-dmo.org\/project\/2066",
                        if (subObject.has(      "bcodmo_webpage")) 
                            gatts.add(pre +     "webpage", //??? "BCO_DMO_webpage", 
                            subObject.getString("bcodmo_webpage"));

                        //"project_acronym":"U.S. GEOTRACES NAT"}],
                        if (subObject.has(      "project_acronym")) 
                            gatts.add(pre +     "acronym", 
                            subObject.getString("project_acronym"));
                
                    }
                }

                //"funding_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/funding"},
                subArray = getBCODMOSubArray(ds, dsDir, "funding", useLocalFilesIfPossible);
                if (subArray != null) {
                    for (int sai = 0; sai < subArray.length(); sai++) {
                        subObject = subArray.getJSONObject(sai);
                        String pre = "funding_" + (sai + 1) + "_";

                        //"award":"http:\/\/lod.bco-dmo.org\/id\/award\/55138",
                        //SKIP since web page has ID# and info
                        //if (subObject.has(      "award")) 
                        //    gatts.add(pre +     "BCO_DMO_award_ID", 
                        //    File2.getNameAndExtension(
                        //    subObject.getString("award")));

                        //"award_number":"OCE-0928289",
                        if (subObject.has(      "award_number")) 
                            gatts.add(pre +     "award_number", 
                            subObject.getString("award_number"));

                        //"award_url":"http:\/\/www.nsf.gov\/awardsearch\/showAward?AWD_ID=0928289\u0026HistoricalAwards=false"
                        if (subObject.has(      "award_url")) 
                            gatts.add(pre +     "award_url", 
                            subObject.getString("award_url"));

                        //"funding":"http:\/\/lod.bco-dmo.org\/id\/funding\/355",
                        //SKIP since funding_source and fundref_doi have info
                        //if (subObject.has(      "funding")) 
                        //    gatts.add(pre +     "BCO_DMO_funding_ID", 
                        //    File2.getNameAndExtension(
                        //    subObject.getString("funding")));

                        //"funding_source":"NSF Division of Ocean Sciences (NSF OCE)",
                        if (subObject.has(      "funding_source")) 
                            gatts.add(pre +     "source", 
                            subObject.getString("funding_source"));

                        //"fundref_doi":"http:\/\/dx.doi.org\/10.13039\/100000141"}
                        if (subObject.has(      "fundref_doi")) 
                            gatts.add(pre +     "doi", 
                            subObject.getString("fundref_doi"));

                    }
                }

                //cleanup
                boolean dateTimeAlreadyFound = false;
                String tSortedColumnSourceName = "";
                String tSortFilesBySourceNames = "";
                String tColumnNameForExtract   = "";

                for (int col = 0; col < addTable.nColumns(); col++) {
                    String colName = addTable.getColumnName(col);
                    PrimitiveArray destPA = addTable.getColumn(col); 

                    //look for date columns
                    String tUnits = addTable.columnAttributes(col).getString("units");
                    if (tUnits == null) tUnits = "";
                    if (tUnits.toLowerCase().indexOf("yy") >= 0 &&
                        destPA.elementType() != PAType.STRING) 
                        //convert e.g., yyyyMMdd columns from int to String
                        addTable.setColumn(col, new StringArray(destPA));                       
                    if (destPA.elementType() == PAType.STRING) {
                        tUnits = Calendar2.suggestDateTimeFormat((StringArray)destPA, false); //evenIfPurelyNumeric
                        if (tUnits.length() > 0)
                            addTable.columnAttributes(col).set("units", tUnits);
                        //??? and if tUnits = "", set to ""???
                    }
                    boolean isDateTime = Calendar2.isTimeUnits(tUnits);

                    Attributes sourceAtts = sourceTable.columnAttributes(col); //none
                    Attributes addAtts    = addTable.columnAttributes(col);
                    addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                        gatts, sourceAtts, addAtts, 
                        sourceTable.getColumnName(col), 
                        destPA.elementType() != PAType.STRING, //tryToAddStandardName
                        destPA.elementType() != PAType.STRING, //addColorBarMinMax
                        true); //tryToFindLLAT

                    //add missing_value and/or _FillValue if needed
                    addMvFvAttsIfNeeded(colName, destPA, sourceAtts, addAtts);

                    //files are likely sorted by first date time variable
                    //and no harm if files aren't sorted that way
                    if (tSortedColumnSourceName.length() == 0 && 
                        isDateTime && !dateTimeAlreadyFound) {
                        dateTimeAlreadyFound = true;
                        tSortedColumnSourceName = colName;
                    }
                }

                //tryToFindLLAT
                tryToFindLLAT(sourceTable, addTable);

                //after dataVariables known, add global attributes in the addTable
                addTable.globalAttributes().set(
                    makeReadyToUseAddGlobalAttributesForDatasetsXml(
                        sourceTable.globalAttributes(), 
                        //another cdm_data_type could be better; this is ok
                        hasLonLatTime(addTable)? "Point" : "Other",
                        dsDir, addTable.globalAttributes(), //externalAddGlobalAttributes, 
                        suggestKeywords(sourceTable, addTable)));
                
                //tally for char > #255
                /*
                String s = addTable.globalAttributes().getString("summary");
                if (s != null)
                    for (int i = 0; i < s.length(); i++)
                        if (s.charAt(i) > 255) 
                            charTally.add("charTally", String2.annotatedString("" + s.charAt(i)));
                s = addTable.globalAttributes().getString("acquisition_description");
                if (s != null)
                    for (int i = 0; i < s.length(); i++)
                        if (s.charAt(i) > 255) 
                            charTally.add("charTally", String2.annotatedString("" + s.charAt(i)));
                */

                //subsetVariables
                if (sourceTable.globalAttributes().getString("subsetVariables") == null &&
                       addTable.globalAttributes().getString("subsetVariables") == null) 
                    addTable.globalAttributes().add("subsetVariables",
                        suggestSubsetVariables(sourceTable, addTable, true)); //1file/dataset?

                StringBuilder defaultDataQuery = new StringBuilder();
                StringBuilder defaultGraphQuery = new StringBuilder();
                if (addTable.findColumnNumber(EDV.TIME_NAME) >= 0) {
                    defaultDataQuery.append( "&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
                    defaultGraphQuery.append("&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
                }
                defaultGraphQuery.append("&amp;.marker=1|5");


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
                    //"<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n" +
                    //"  below, notably 'units' for each of the dataVariables. -->\n" +
                    "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"" + tDatasetID + 
                        "\" active=\"true\">\n" +
                    "    <!--  <accessibleTo>bcodmo</accessibleTo>  -->\n" +
                    "    <reloadEveryNMinutes>10000</reloadEveryNMinutes>\n" +  
                    "    <updateEveryNMillis>-1</updateEveryNMillis>\n" +  
                    (defaultDataQuery.length() > 0? 
                    "    <defaultDataQuery>" + defaultDataQuery + "</defaultDataQuery>\n" : "") +
                    (defaultGraphQuery.length() > 0? 
                    "    <defaultGraphQuery>" + defaultGraphQuery + "</defaultGraphQuery>\n" : "") +
                    "    <fileDir>" + XML.encodeAsXML(dsDir) + "</fileDir>\n" +
                    "    <fileNameRegex>" + XML.encodeAsXML(
                        String2.plainTextToRegex(fileName)) + "</fileNameRegex>\n" +
                    "    <recursive>false</recursive>\n" +
                    "    <pathRegex>.*</pathRegex>\n" +
                    "    <metadataFrom>last</metadataFrom>\n" +
                    "    <charset>" + bcodmoCharset + "</charset>\n" +
                    "    <columnNamesRow>" + (colNamesRow + 1) + "</columnNamesRow>\n" +
                    "    <firstDataRow>" + (colNamesRow + 2) + "</firstDataRow>\n" +
                    "    <standardizeWhat>" + tStandardizeWhat + "</standardizeWhat>\n" +
                    //"    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
                    //"    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
                    //"    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
                    //"    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
                    "    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) + "</sortedColumnSourceName>\n" +
                    "    <sortFilesBySourceNames>" + XML.encodeAsXML(tSortFilesBySourceNames) + "</sortFilesBySourceNames>\n" +
                    "    <fileTableInMemory>false</fileTableInMemory>\n" +
                    "    <accessibleViaFiles>true</accessibleViaFiles>\n" +
                    (iso19115File == null? "" : 
                    "    <iso19115File>" + iso19115File + "</iso19115File>\n"));
                sb.append(writeAttsForDatasetsXml(false, sourceTable.globalAttributes(), "    "));
                sb.append(cdmSuggestion());
                sb.append(writeAttsForDatasetsXml(true,     addTable.globalAttributes(), "    "));

                //last 2 params: includeDataType, questionDestinationName
                sb.append(writeVariablesForDatasetsXml(sourceTable, addTable, 
                    "dataVariable", true, false));
                sb.append(
                    "</dataset>\n" +
                    "\n");

                //success
                results.append(sb.toString());                    
                if (addTable.findColumnNumber("time") < 0)
                    noTime.add(dsNumber);
                nSucceeded++;

            } catch (Exception e) {
                nFailed++;
                String2.log(String2.ERROR + " while processing dataset #" + dsi + "\n" +
                    MustBe.throwableToString(e));
            }
        }
        //String2.log(charTally.toString());
        String2.log(">> noTime: " + noTime);
        String2.log("\n*** EDDTableFromAsciiFiles.generateDatasetsXmlFromBCODMO finished in " +
            ((System.currentTimeMillis() - time)/1000) + " seconds\n" +
            "nDatasets: total=" + datasetsArray.length() + 
            " matching=" + nMatching + 
            " (succeeded=" + nSucceeded + " failed=" + nFailed + ")");
        return results.toString();
    }

    /**
     * This tests GenerateDatasetsXml with EDDTableFromInPort when there are  
     * data variables. 
     */
    public static void testGenerateDatasetsXmlFromBCODMO() throws Throwable {
        String2.log("\n*** EDDTableFromAsciiFiles.testGenerateDatasetsXmlFromBCODMO()\n");
        testVerboseOn();
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        boolean useLocal = true;
        String catalogUrl = "https://www.bco-dmo.org/erddap/datasets";
        String dataDir = "/u00/data/points/bcodmo/";
        String numberRegex = "(549122)";

        String results = generateDatasetsXmlFromBCODMO(
            useLocal, catalogUrl, dataDir, numberRegex, -1) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromBCODMO",
            useLocal + "", catalogUrl, dataDir, numberRegex, "-1"}, //defaultStandardizeWhat
            false); //doIt loop?

String expected = 
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"bcodmo549122v20150217\" active=\"true\">\n" +
"    <!--  <accessibleTo>bcodmo</accessibleTo>  -->\n" +
"    <reloadEveryNMinutes>10000</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>-1</updateEveryNMillis>\n" +
"    <defaultDataQuery>&amp;time&gt;=min(time)&amp;time&lt;=max(time)</defaultDataQuery>\n" +
"    <defaultGraphQuery>&amp;time&gt;=min(time)&amp;time&lt;=max(time)&amp;.marker=1|5</defaultGraphQuery>\n" +
"    <fileDir>/u00/data/points/bcodmo/549122v20150217/</fileDir>\n" +
"    <fileNameRegex>GT10_11_cellular_element_quotas\\.tsv</fileNameRegex>\n" +
"    <recursive>false</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <sortedColumnSourceName>BTL_ISO_DateTime_UTC</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>BTL_ISO_DateTime_UTC</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <iso19115File>/u00/data/points/bcodmo/549122v20150217/iso_19115_2.xml</iso19115File>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"acquisition_description\">SXRF samples were prepared from unfiltered water taken from GEOTRACES GO-Flo bottles at the shallowest depth and deep chlorophyll maximum. Cells were preserved with 0.25&#37; trace-metal clean buffered glutaraldehyde and centrifuged onto C/formvar-coated Au TEM grids. Grids were briefly rinsed with a drop of ultrapure water and dried in a Class-100 cabinet. SXRF analysis was performed using the 2-ID-E beamline at the Advanced Photon source (Argonne National Laboratory) following the protocols of Twining et al. (2011).</att>\n" +
"        <att name=\"BCO_DMO_dataset_ID\">549122</att>\n" +
"        <att name=\"brief_description\">Element quotas of individual phytoplankton cells</att>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">info@bco-dmo.org</att>\n" +
"        <att name=\"creator_name\">Dr Benjamin Twining</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">https://www.bco-dmo.org/person/51087</att>\n" +
"        <att name=\"current_state\">Final no updates expected</att>\n" +
"        <att name=\"dataset_name\">GT10-11 - cellular element quotas</att>\n" +
"        <att name=\"date_created\">2015-02-17</att>\n" +
"        <att name=\"deployment_1_description\">KN199-04 is the US GEOTRACES Zonal North Atlantic Survey Section cruise planned for late Fall 2010 from Lisboa, Portugal to Woods Hole, MA, USA.\n" +
"4 November 2010 update: Due to engine failure, the scheduled science activities were canceled on 2 November 2010. On 4 November the R/V KNORR put in at Porto Grande, Cape Verde and is scheduled to depart November 8, under the direction of Acting Chief Scientist Oliver Wurl of Old Dominion University. The objective of this leg is to carry the vessel in transit to Charleston, SC while conducting science activities modified from the original plan.\n" +
"Planned scientific activities and operations area during this transit will be as follows: the ship&#39;s track will cross from the highly productive region off West Africa into the oligotrophic central subtropical gyre waters, then across the western boundary current (Gulf Stream), and into the productive coastal waters of North America. During this transit, underway surface sampling will be done using the towed fish for trace metals, nanomolar nutrients, and arsenic speciation. In addition, a port-side high volume pumping system will be used to acquire samples for radium isotopes. Finally, routine aerosol and rain sampling will be done for trace elements. This section will provide important information regarding atmospheric deposition, surface transport, and transformations of many trace elements.\n" +
"The vessel is scheduled to arrive at the port of Charleston, SC, on 26 November 2010. The original cruise was intended to be 55 days duration with arrival in Norfolk, VA on 5 December 2010.\n" +
"funding: NSF OCE award 0926423\n" +
"Science Objectives are to obtain state of the art trace metal and isotope measurements on a suite of samples taken on a mid-latitude zonal transect of the North Atlantic. In particular sampling will target the oxygen minimum zone extending off the west African coast near Mauritania, the TAG hydrothermal field, and the western boundary current system along Line W. In addition, the major biogeochemical provinces of the subtropical North Atlantic will be characterized. For additional information, please refer to the GEOTRACES program Web site ( [ http://www.GEOTRACES.org ] GEOTRACES.org) for overall program objectives and a summary of properties to be measured.\n" +
"Science Activities include seawater sampling via GoFLO and Niskin carousels, in situ pumping (and filtration), CTDO2 and transmissometer sensors, underway pumped sampling of surface waters, and collection of aerosols and rain.\n" +
"Hydrography, CTD and nutrient measurements will be supported by the Ocean Data Facility (J. Swift) at Scripps Institution of Oceanography and funded through NSF Facilities. They will be providing an additional CTD rosette system along with nephelometer and LADCP. A trace metal clean Go-Flo Rosette and winch will be provided by the group at Old Dominion University (G. Cutter) along with a towed underway pumping system.\n" +
"List of cruise participants: [ [ http://data.bcodmo.org/US_GEOTRACES/AtlanticSection/GNAT_2010_cruiseParticipants.pdf ] PDF ]\n" +
"Cruise track: [ http://data.bcodmo.org/US_GEOTRACES/AtlanticSection/KN199-04_crtrk.jpg ] JPEG image (from Woods Hole Oceanographic Institution, vessel operator)\n" +
"Additional information may still be available from the vessel operator: [ https://www.whoi.edu/cruiseplanning/synopsis.do?id=581 ] WHOI cruise planning synopsis\n" +
"Cruise information and original data are available from the [ https://www.rvdata.us/search/cruise/KN199-04 ] NSF R2R data catalog.\n" +
"ADCP data are available from the Currents ADCP group at the University of Hawaii: [ https://currents.soest.hawaii.edu/uhdas_adcp/year2010.html#kn199_4 ] KN199-04 ADCP</att>\n" +
"        <att name=\"deployment_1_end_date\">2010-11-04</att>\n" +
"        <att name=\"deployment_1_location\">Subtropical northern Atlantic Ocean</att>\n" +
"        <att name=\"deployment_1_start_date\">2010-10-15</att>\n" +
"        <att name=\"deployment_1_title\">KN199-04</att>\n" +
"        <att name=\"deployment_1_webpage\">https://www.bco-dmo.org/deployment/58066</att>\n" +
"        <att name=\"deployment_2_description\">KN199-05 is the completion of the US GEOTRACES Zonal North Atlantic Survey Section cruise originally planned for late Fall 2010 from Lisboa, Portugal to Woods Hole, MA, USA.\n" +
"4 November 2010 update: Due to engine failure, the science activities scehduled for the KN199-04 cruise were canceled on 2 November 2010. On 4 November the R/V KNORR put in at Porto Grande, Cape Verde (ending KN199 leg 4) and is scheduled to depart November 8, under the direction of Acting Chief Scientist Oliver Wurl of Old Dominion University.&#xa0; The objective of KN199 leg 5 (KN199-05) is to carry the vessel in transit to Charleston, SC while conducting abbreviated science activities originally planned for KN199-04. The vessel is scheduled to arrive at the port of Charleston, SC, on 26 November 2010. The original cruise was intended to be 55 days duration with arrival in Norfolk, VA on 5 December 2010.\n" +
"Planned scientific activities and operations area during the KN199 leg 5 (KN199-05)  transit will be as follows: the ship&#39;s track will cross from the highly productive region off West Africa into the oligotrophic central subtropical gyre waters, then across the western boundary current (Gulf Stream), and into the productive coastal waters of North America. During this transit, underway surface sampling will be done using the towed fish for trace metals, nanomolar nutrients, and arsenic speciation. In addition, a port-side high volume pumping system will be used to acquire samples for radium isotopes. Finally, routine aerosol and rain sampling will be done for trace elements. This section will provide important information regarding atmospheric deposition, surface transport, and transformations of many trace elements.\n" +
"Science Objectives are to obtain state of the art  trace metal and isotope measurements on a suite of samples taken on a  mid-latitude zonal transect of the North Atlantic. In particular  sampling will target the oxygen minimum zone extending off the west  African coast near Mauritania, the TAG hydrothermal field, and the  western boundary current system along Line W. In addition, the major  biogeochemical provinces of the subtropical North Atlantic will be  characterized. For additional information, please refer to the GEOTRACES  program Web site ( [ https://www.geotraces.org/ ] GEOTRACES.org) for overall program objectives and a summary of properties to be measured.\n" +
"Science Activities include seawater sampling via  GoFLO and Niskin carousels, in situ pumping (and filtration), CTDO2 and  transmissometer sensors, underway pumped sampling of surface waters, and  collection of aerosols and rain.\n" +
"Hydrography, CTD and nutrient measurements will be supported by the  Ocean Data Facility (J. Swift) at Scripps Institution of Oceanography  and funded through NSF Facilities. They will be providing an additional  CTD rosette system along with nephelometer and LADCP. A trace metal  clean Go-Flo Rosette and winch will be provided by the group at Old  Dominion University (G. Cutter) along with a towed underway pumping  system.\n" +
"List of cruise participants: [ [ http://data.bcodmo.org/US_GEOTRACES/AtlanticSection/GNAT_2010_cruiseParticipants.pdf ] PDF ]\n" +
"[ http://data.bcodmo.org/GEOTRACES/cruises/Atlantic_2010/KN199-04_crtrk.jpg ] JPEG image (from Woods Hole Oceanographic Institution, vessel operator) --&gt;funding: NSF OCE award 0926423\n" +
"[ https://www.whoi.edu/cruiseplanning/synopsis.do?id=581 ] WHOI cruise planning synopsis\n" +
"Cruise information and original data are available from the [ https://www.rvdata.us/search/cruise/KN199-05 ] NSF R2R data catalog.\n" +
"ADCP data are available from the Currents ADCP group at the University of Hawaii: [ https://currents.soest.hawaii.edu/uhdas_adcp/year2010.html#kn199_5 ] KN199-05 ADCP</att>\n" +
"        <att name=\"deployment_2_end_date\">2010-11-26</att>\n" +
"        <att name=\"deployment_2_location\">Subtropical northern Atlantic Ocean</att>\n" +
"        <att name=\"deployment_2_start_date\">2010-11-08</att>\n" +
"        <att name=\"deployment_2_title\">KN199-05</att>\n" +
"        <att name=\"deployment_2_webpage\">https://www.bco-dmo.org/deployment/58142</att>\n" +
"        <att name=\"deployment_3_description\">The US GEOTRACES North Atlantic cruise aboard the R/V Knorr completed the section between Lisbon and Woods Hole that began in October 2010 but was rescheduled for November-December 2011. The R/V Knorr made a brief stop in Bermuda to exchange samples and personnel before continuing across the basin. Scientists disembarked in Praia, Cape Verde, on 11 December. The cruise was identified as KN204-01A (first part before Bermuda) and KN204-01B (after the Bermuda stop). However, the official deployment name for this cruise is KN204-01 and includes both part A and B.\n" +
"Science activities included: ODF 30 liter rosette CTD casts, ODU Trace metal rosette CTD casts, McLane particulate pump casts, underway sampling with towed fish and sampling from the shipboard &quot;uncontaminated&quot; flow-through system.\n" +
"Full depth stations are shown in the accompanying figure (see below). Additional stations to sample for selected trace metals to a depth of 1000 m are not shown. Standard stations are shown in red (as are the ports) and &quot;super&quot; stations, with extra casts to provide large-volume samples for selected parameters, are shown in green.\n" +
"[ http://data.bco-dmo.org/GEOTRACES/cruises/KN204-01_GEOTRACES_Station_Plan.jpg ] \n" +
"Station spacing is concentrated along the western margin to evaluate the transport of trace elements and isotopes by western boundary currents. Stations across the gyre will allow scientists to examine trace element supply by Saharan dust, while also contrasting trace element and isotope distributions in the oligotrophic gyre with conditions near biologically productive ocean margins, both in the west, to be sampled now, and within the eastern boundary upwelling system off Mauritania, sampled last year.\n" +
"The cruise was funded by NSF OCE awards 0926204, 0926433 and 0926659.\n" +
"Additional information may be available from the vessel operator site, URL: [ https://www.whoi.edu/cruiseplanning/synopsis.do?id=1662 ] https://www.whoi.edu/cruiseplanning/synopsis.do?id=1662.\n" +
"Cruise information and original data are available from the [ https://www.rvdata.us/search/cruise/KN204-01 ] NSF R2R data catalog.\n" +
"ADCP data are available from the Currents ADCP group at the University of Hawaii at the links below: [ https://currents.soest.hawaii.edu/uhdas_adcp/year2011.html#kn204_01 ] KN204-01A (part 1 of 2011 cruise; Woods Hole, MA to Bermuda) [ https://currents.soest.hawaii.edu/uhdas_adcp/year2011.html#kn204_02 ] KN204-01B (part 2 of 2011 cruise; Bermuda to Cape Verde)</att>\n" +
"        <att name=\"deployment_3_end_date\">2011-12-11</att>\n" +
"        <att name=\"deployment_3_location\">Subtropical northern Atlantic Ocean</att>\n" +
"        <att name=\"deployment_3_start_date\">2011-11-06</att>\n" +
"        <att name=\"deployment_3_title\">KN204-01</att>\n" +
"        <att name=\"deployment_3_webpage\">https://www.bco-dmo.org/deployment/58786</att>\n" +
"        <att name=\"doi\">10.1575/1912/bco-dmo.641155</att>\n" +
"        <att name=\"id\">bcodmo549122v20150217</att>\n" +
"        <att name=\"infoUrl\">https://www.bco-dmo.org/dataset/549122</att>\n" +
"        <att name=\"institution\">BCO-DMO</att>\n" +
"        <att name=\"instrument_1_type_description\">GO-FLO bottle cast used to collect water samples for pigment, nutrient, plankton, etc. The GO-FLO sampling bottle is specially designed to avoid sample contamination at the surface, internal spring contamination, loss of sample on deck (internal seals), and exchange of water from different depths.</att>\n" +
"        <att name=\"instrument_1_type_name\">GO-FLO Bottle</att>\n" +
"        <att name=\"instrument_1_webpage\">https://www.bco-dmo.org/dataset-instrument/643392</att>\n" +
"        <att name=\"instrument_2_type_description\">The GeoFish towed sampler is a custom designed near surface (&lt;2m) sampling system for the collection of trace metal clean seawater. It consists of a PVC encapsulated lead weighted torpedo and separate PVC depressor vane supporting the intake utilizing all PFA Teflon tubing connected to a deck mounted, air-driven, PFA Teflon dual-diaphragm pump which provides trace-metal clean seawater at up to 3.7L/min. The GeoFish is towed at up to 13kts off to the side of the vessel outside of the ship&#39;s wake to avoid possible contamination from the ship&#39;s hull. It was developed by Geoffrey Smith and Ken Bruland (University of California, Santa Cruz).</att>\n" +
"        <att name=\"instrument_2_type_name\">GeoFish Towed near-Surface Sampler</att>\n" +
"        <att name=\"instrument_2_webpage\">https://www.bco-dmo.org/dataset-instrument/643393</att>\n" +
"        <att name=\"instrument_3_type_description\">Instruments that generate enlarged images of samples using the phenomena of reflection and absorption of visible light. Includes conventional and inverted instruments. Also called a &quot;light microscope&quot;.</att>\n" +
"        <att name=\"instrument_3_type_name\">Microscope-Optical</att>\n" +
"        <att name=\"instrument_3_webpage\">https://www.bco-dmo.org/dataset-instrument/643394</att>\n" +
"        <att name=\"instrument_4_description\">SXRF analysis was performed on the 2-ID-E beamline at the Advanced Photon source (Argonne National Laboratory). The synchetron consists of a storage ring which produces high energy electromagnetic radiation. X-rays diverted to the 2-ID-E beamline are used for x-ray fluorescence mapping of biological samples. X-rays were tuned to an energy of 10 keV to enable the excition of K-alpha fluorescence for the elements reported. The beam is focused using Fresnel zoneplates to achieve high spatial resolution; for our application a focused spot size of 0.5um was used. A single element germanium energy dispersive detector is used to record the X-ray fluorescence spectrum.</att>\n" +
"        <att name=\"instrument_4_type_description\">Instruments that identify and quantify the elemental constituents of a sample from the spectrum of electromagnetic radiation emitted by the atoms in the sample when excited by X-ray radiation.</att>\n" +
"        <att name=\"instrument_4_type_name\">X-ray fluorescence analyser</att>\n" +
"        <att name=\"instrument_4_webpage\">https://www.bco-dmo.org/dataset-instrument/648912</att>\n" +
"        <att name=\"keywords\">atlantic, bco, bco-dmo, biological, bottle, bottle_GEOTRC, btl, cast, cast_GEOTRC, cell, cell_C, cell_Co, cell_Cu, cell_Fe, cell_Mn, cell_Ni, cell_P, cell_S, cell_Si, cell_type, cell_vol, cell_Zn, cells, cellular, chemical, chemistry, chl, chl_image_filename, chlorophyll, collected, concentration, content, cruise, cruise_id, cruises, data, date, depth, depth_GEOTRC_CTD_round, dissolved, dissolved nutrients, dmo, during, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Silicate, eastern, elemental, event, event_GEOTRC, filename, foundation, geotraces, geotrc, grid, grid_num, grid_type, identifier, image, individual, iso, latitude, light, light_image_filename, longitude, management, map, mda, mda_id, mole, mole_concentration_of_silicate_in_sea_water, national, north, nsf, num, nutrients, ocean, oceanography, oceans, office, phytoplankton, project, run, sample, sample_bottle_GEOTRC, sample_GEOTRC, science, sea, seawater, silicate, spectrum, sta, sta_PI, station, station_GEOTRC, subtropical, sxrf, SXRF_map_filename, SXRF_run, SXRF_spectrum_filename, time, transect, type, US, v20150217, vol, water, western</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">https://creativecommons.org/licenses/by/4.0/\n" +
"This data set is freely available as long as one follows the\n" +
"terms of use (https://www.bco-dmo.org/terms-use), including\n" +
"the understanding that any such use will properly acknowledge\n" +
"the originating Investigator. It is highly recommended that\n" +
"anyone wishing to use portions of this data should contact\n" +
"the originating principal investigator (PI).</att>\n" +
"        <att name=\"naming_authority\">org.bco-dmo</att>\n" +
"        <att name=\"person_1_institution_name\">Bigelow Laboratory for Ocean Sciences (Bigelow)</att>\n" +
"        <att name=\"person_1_name\">Dr Benjamin Twining</att>\n" +
"        <att name=\"person_1_role\">Principal Investigator</att>\n" +
"        <att name=\"person_1_webpage\">https://www.bco-dmo.org/person/51087</att>\n" +
"        <att name=\"person_2_institution_name\">Woods Hole Oceanographic Institution (WHOI BCO-DMO)</att>\n" +
"        <att name=\"person_2_name\">Nancy Copley</att>\n" +
"        <att name=\"person_2_role\">BCO-DMO Data Manager</att>\n" +
"        <att name=\"person_2_webpage\">https://www.bco-dmo.org/person/50396</att>\n" +
"        <att name=\"processing_description\">Data were processed as described in Twining et al. (2015)\n" +
"Between 9 and 20 cells were analyzed from the shallowest bottle and deep chlorophyll maximum at the subset of stations. The elemental content of each cell has been corrected for elements contained in the carbon substrate. Trace element concentrations are presented as mmol/mol P. Geometric mean concentrations (+/- standard error of the mean) are presented, along with the number of cells analyzed.\n" +
"BCO-DMO Processing:\n" +
"- added conventional header with dataset name, PI name, version date\n" +
"- renamed parameters to BCO-DMO standard\n" +
"- replaced blank cells with nd\n" +
"- sorted by cruise, station, grid#\n" +
"- changed station 99 and 153 to cruise AT199-05 from AT199-04\n" +
"- revised station 9 depths to match master events log\n" +
"With the agreement of BODC and the US GEOTRACES lead PIs, BCO-DMO added standard US GEOTRACES information, such as the US GEOTRACES event number. To accomplish this, BCO-DMO compiled a &#39;master&#39; dataset composed of the following parameters: station_GEOTRC, cast_GEOTRC (bottle and pump data only), event_GEOTRC, sample_GEOTRC, sample_bottle_GEOTRC (bottle data only), bottle_GEOTRC (bottle data only), depth_GEOTRC_CTD (bottle data only), depth_GEOTRC_CTD_rounded (bottle data only), BTL_ISO_DateTime_UTC (bottle data only), and GeoFish_id (GeoFish data only). This added information will facilitate subsequent analysis and inter comparison of the datasets.\n" +
"Bottle parameters in the master file were taken from the GT-C_Bottle_GT10, GT-C_Bottle_GT11, ODF_Bottle_GT10, and ODF_Bottle_GT11 datasets. Non-bottle parameters, including those from GeoFish tows, Aerosol sampling, and McLane Pumps, were taken from the Event_Log_GT10 and Event_Log_GT11 datasets. McLane pump cast numbers missing in event logs were taken from the Particulate Th-234 dataset submitted by Ken Buesseler.\n" +
"A standardized BCO-DMO method (called &quot;join&quot;) was then used to merge the missing parameters to each US GEOTRACES dataset, most often by matching on sample_GEOTRC or on some unique combination of other parameters.\n" +
"If the master parameters were included in the original data file and the values did not differ from the master file, the original data columns were retained and the names of the parameters were changed from the PI-submitted names to the standardized master names. If there were differences between the PI-supplied parameter values and those in the master file, both columns were retained. If the original data submission included all of the master parameters, no additional columns were added, but parameter names were modified to match the naming conventions of the master file.\n" +
"See the dataset parameters documentation for a description of which parameters were supplied by the PI and which were added via the join method.</att>\n" +
"        <att name=\"project_1_acronym\">U.S. GEOTRACES NAT</att>\n" +
"        <att name=\"project_1_description\">Much of this text appeared in an article published in OCB News, October 2008, by the OCB Project Office.\n" +
"The first U.S. GEOTRACES Atlantic Section will be specifically centered around a sampling cruise to be carried out in the North Atlantic in 2010. Ed Boyle (MIT) and Bill Jenkins (WHOI) organized a three-day planning workshop that was held September 22-24, 2008 at the Woods Hole Oceanographic Institution. The main goal of the workshop, sponsored by the National Science Foundation and the U.S. GEOTRACES Scientific Steering Committee, was to design the implementation plan for the first U.S. GEOTRACES Atlantic Section. The primary cruise design motivation was to improve knowledge of the sources, sinks and internal cycling of Trace Elements and their Isotopes (TEIs) by studying their distributions along a section in the North Atlantic (Figure 1). The North Atlantic has the full suite of processes that affect TEIs, including strong meridional advection, boundary scavenging and source effects, aeolian deposition, and the salty Mediterranean Outflow. The North Atlantic is particularly important as it lies at the &quot;origin&quot; of the global Meridional Overturning Circulation.\n" +
"It is well understood that many trace metals play important roles in biogeochemical processes and the carbon cycle, yet very little is known about their large-scale distributions and the regional scale processes that affect them. Recent advances in sampling and analytical techniques, along with advances in our understanding of their roles in enzymatic and catalytic processes in the open ocean provide a natural opportunity to make substantial advances in our understanding of these important elements. Moreover, we are motivated by the prospect of global change and the need to understand the present and future workings of the ocean&#39;s biogeochemistry. The GEOTRACES strategy is to measure a broad suite of TEIs to constrain the critical biogeochemical processes that influence their distributions. In addition to these &quot;exotic&quot; substances, more traditional properties, including macronutrients (at micromolar and nanomolar levels), CTD, bio-optical parameters, and carbon system characteristics will be measured. The cruise starts at Line W, a repeat hydrographic section southeast of Cape Cod, extends to Bermuda and subsequently through the North Atlantic oligotrophic subtropical gyre, then transects into the African coast in the northern limb of the coastal upwelling region. From there, the cruise goes northward into the Mediterranean outflow. The station locations shown on the map are for the &quot;fulldepth TEI&quot; stations, and constitute approximately half of the stations to be ultimately occupied.\n" +
"Figure 1. The proposed 2010 Atlantic GEOTRACES cruise track plotted on dissolved oxygen at 400 m depth. Data from the World Ocean Atlas (Levitus et al., 2005) were plotted using Ocean Data View (courtesy Reiner Schlitzer).  [ http://bcodata.whoi.edu/US_GEOTRACES/AtlanticSection/GEOTRACES_Atl_stas.jpg ] \n" +
"Hydrography, CTD and nutrient measurements will be supported by the Ocean Data Facility (J. Swift) at Scripps Institution of Oceanography and funded through NSF Facilities. They will be providing an additional CTD rosette system along with nephelometer and LADCP. A trace metal clean Go-Flo Rosette and winch will be provided by the group at Old Dominion University (G. Cutter) along with a towed underway pumping system.\n" +
"The North Atlantic Transect cruise began in 2010 with KN199 leg 4 (station sampling) and leg 5 (underway sampling only) (Figure 2).\n" +
"[ http://bcodata.whoi.edu//US_GEOTRACES/AtlanticSection/Cruise_Report_for_Knorr_199_Final_v3.pdf ] KN199-04 Cruise Report (PDF)\n" +
"Figure 2. The red line shows the cruise track for the first leg of the US Geotraces North Atlantic Transect on the R/V Knorr in October 2010.&#xa0; The rest of the stations (beginning with 13) will be completed in October-December 2011 on the R/V Knorr (courtesy of Bill Jenkins, Chief Scientist, GNAT first leg).  [ http://bcodata.whoi.edu/US_GEOTRACES/AtlanticSection/GNAT_stationPlan.jpg ] \n" +
"The section completion effort resumed again in November 2011 with KN204-01A,B (Figure 3).\n" +
"[ http://bcodata.whoi.edu//US_GEOTRACES/AtlanticSection/Submitted_Preliminary_Cruise_Report_for_Knorr_204-01.pdf ] KN204-01A,B Cruise Report (PDF)\n" +
"Figure 3. Station locations occupied on the US Geotraces North Atlantic Transect on the R/V Knorr in November 2011.&#xa0;  [ http://bcodata.whoi.edu/US_GEOTRACES/AtlanticSection/KN204-01_Stations.png ] \n" +
"Data from the North Atlantic Transect cruises are available under the Datasets heading below, and consensus values for the SAFe and North Atlantic GEOTRACES Reference Seawater Samples are available from the GEOTRACES Program Office: [ https://www.geotraces.org/standards-and-reference-materials/?acm=455_215 ] Standards and Reference Materials\n" +
"ADCP data are available from the Currents ADCP group at the University of Hawaii at the links below: [ https://currents.soest.hawaii.edu/uhdas_adcp/year2010.html#kn199_4 ] KN199-04&#xa0;&#xa0; (leg 1 of 2010 cruise; Lisbon to Cape Verde) [ https://currents.soest.hawaii.edu/uhdas_adcp/year2010.html#kn199_5 ] KN199-05&#xa0;&#xa0; (leg 2 of 2010 cruise; Cape Verde to Charleston, NC) [ https://currents.soest.hawaii.edu/uhdas_adcp/year2011.html#kn204_01 ] KN204-01A (part 1 of 2011 cruise; Woods Hole, MA to Bermuda) [ https://currents.soest.hawaii.edu/uhdas_adcp/year2011.html#kn204_02 ] KN204-01B (part 2 of 2011 cruise; Bermuda to Cape Verde)</att>\n" +
"        <att name=\"project_1_title\">U.S. GEOTRACES North Atlantic Transect</att>\n" +
"        <att name=\"project_1_webpage\">https://www.bco-dmo.org/project/2066</att>\n" +
"        <att name=\"publisher_email\">info@bco-dmo.org</att>\n" +
"        <att name=\"publisher_name\">BCO-DMO</att>\n" +
"        <att name=\"publisher_type\">institution</att>\n" +
"        <att name=\"publisher_url\">https://www.bco-dmo.org/</att>\n" +
"        <att name=\"restricted\">false</att>\n" +
"        <att name=\"sourceUrl\">http://darchive.mblwhoilibrary.org/bitstream/handle/1912/7908/1/GT10_11_cellular_element_quotas.tsv</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"subsetVariables\">cruise_id, project, station_GEOTRC, sta_PI, latitude, longitude, cast_GEOTRC, event_GEOTRC, depth, depth_GEOTRC_CTD_round, sample_GEOTRC, sample_bottle_GEOTRC, bottle_GEOTRC, grid_type, grid_num, SXRF_run, cell_type, time</att>\n" +
"        <att name=\"summary\">Individual phytoplankton cells were collected on the GEOTRACES North Atlantic Transect cruises were analyzed for elemental content using SXRF (Synchrotron radiation X-Ray Fluorescence). Carbon was calculated from biovolume using the relationships of Menden-Deuer &amp; Lessard (2000). Trace metal concentrations are reported.\n" +
"Download zipped images: [ http://data.bco-dmo.org/GEOTRACES/Twining/Chl_image.zip ] Chlorophyll [ http://data.bco-dmo.org/GEOTRACES/Twining/Light_image.zip ] Light [ http://data.bco-dmo.org/GEOTRACES/Twining/SXRF_map.zip ] SXRF maps [ http://data.bco-dmo.org/GEOTRACES/Twining/SXRF_spectra.zip ] SXRF spectra\n" +
"Related references:\n" +
"Menden-Deuer, S. and E. J. Lessard (2000). Carbon to volume relationships for dinoflagellates, diatoms, and other protist plankton. Limnology and Oceanography 45(3): 569-579.\n" +
"* Twining, B. S., S. Rauschenberg, P. L. Morton, and S. Vogt. 2015. Metal contents of phytoplankton and labile particulate material in the North Atlantic Ocean. Progress in Oceanography 137: 261-283.)</att>\n" +
"        <att name=\"title\">BCO-DMO 549122 v20150217: Cellular elemental content of individual phytoplankton cells collected during US GEOTRACES North Atlantic Transect cruises in the Subtropical western and eastern North Atlantic Ocean during Oct and Nov, 2010 and Nov. 2011.</att>\n" +
"        <att name=\"validated\">true</att>\n" +
"        <att name=\"version_date\">2015-02-17</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>cruise_id</sourceName>\n" +
"        <destinationName>cruise_id</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">cruise identification</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Cruise Id</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550520</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>project</sourceName>\n" +
"        <destinationName>project</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">GEOTRACES project: North Atlantic Zonal Transect</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Project</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550531</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>station_GEOTRC</sourceName>\n" +
"        <destinationName>station_GEOTRC</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"description\">GEOTRACES station number; ranges from 1 through 12 for KN199-04 and 1 through 24 for KN204-01. Stations 7 and 9 were skipped on KN204-01. Some GeoFish stations are denoted as X_to_Y indicating the tow occurred between stations X and Y. Values were added from the intermediate US GEOTRACES master file (see Processing Description).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Station GEOTRC</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550521</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sta_PI</sourceName>\n" +
"        <destinationName>sta_PI</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"description\">station number given by PI</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Sta PI</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/564854</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"description\">station latitude; north is positive</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550522</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"description\">station longitude; east is postive</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550523</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cast_GEOTRC</sourceName>\n" +
"        <destinationName>cast_GEOTRC</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"description\">Cast identifier; numbered consecutively within a station. Values were added from the intermediate US GEOTRACES master file (see Processing Description).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cast GEOTRC</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550524</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>event_GEOTRC</sourceName>\n" +
"        <destinationName>event_GEOTRC</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"description\">Unique identifying number for US GEOTRACES sampling events; ranges from 2001 to 2225 for KN199-04 events and from 3001 to 3282 for KN204-01 events. Values were added from the intermediate US GEOTRACES master file (see Processing Description).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Event GEOTRC</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550525</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>depth_GEOTRC_CTD</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n" +
"            <att name=\"colorBarPalette\">TopographyDepth</att>\n" +
"            <att name=\"description\">Observation/sample depth in meters; calculated from CTD pressure. Values were added from the intermediate US GEOTRACES master file (see Processing Description).</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"source_name\">depth_GEOTRC_CTD</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550526</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>depth_GEOTRC_CTD_round</sourceName>\n" +
"        <destinationName>depth_GEOTRC_CTD_round</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n" +
"            <att name=\"colorBarPalette\">TopographyDepth</att>\n" +
"            <att name=\"description\">Rounded observation/sample depth in meters; calculated from CTD pressure. Values were added from the intermediate US GEOTRACES master file (see Processing Description).</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">meters</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550527</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sample_GEOTRC</sourceName>\n" +
"        <destinationName>sample_GEOTRC</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"description\">Unique identifying number for US GEOTRACES samples; ranges from 5033 to 6078 for KN199-04 and from 6112 to 8148 for KN204-01. PI-supplied values were identical to those in the intermediate US GEOTRACES master file. Originally submitted as &#39;GEOTRACES #&#39;; this parameter name has been changed to conform to BCO-DMO&#39;s GEOTRACES naming conventions.</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Sample GEOTRC</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550528</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sample_bottle_GEOTRC</sourceName>\n" +
"        <destinationName>sample_bottle_GEOTRC</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"description\">Unique identification numbers given to samples taken from bottles; ranges from 1 to 24; often used synonymously with bottle number. Values were added from the intermediate US GEOTRACES master file (see Processing Description).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Sample Bottle GEOTRC</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550529</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>bottle_GEOTRC</sourceName>\n" +
"        <destinationName>bottle_GEOTRC</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">Alphanumeric characters identifying bottle type (e.g. NIS representing Niskin and GF representing GOFLO) and position on a CTD rosette. Values were added from the intermediate US GEOTRACES master file (see Processing Description).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Bottle GEOTRC</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550530</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>grid_type</sourceName>\n" +
"        <destinationName>grid_type</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">grid type: plankton samples were mounted onto either gold (Au) or aluminum (Al) electron microscopy grids</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Grid Type</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550532</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>grid_num</sourceName>\n" +
"        <destinationName>grid_num</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">GEOTRACES bottle number followed by an internal designation for the grid</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Grid Num</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550533</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>SXRF_run</sourceName>\n" +
"        <destinationName>SXRF_run</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">samples were analyzed by synchrotron x-ray fluorescence (SXRF) during two analytical runs in July 2011 (2011r2) or August 2012 (2012r2).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">SXRF Run</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550534</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>mda_id</sourceName>\n" +
"        <destinationName>mda_id</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">unique identifier given to each SXRF scan during each run</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Mda Id</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550535</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_type</sourceName>\n" +
"        <destinationName>cell_type</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">each cell was classified as either an autotrophic flagellate (Aflag); autotrophic picoplankter (Apico); or a diatom.</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell Type</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550536</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_vol</sourceName>\n" +
"        <destinationName>cell_vol</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"description\">biovolume of each cell estimated from microscope measurements of cell dimensions</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell Vol</att>\n" +
"            <att name=\"units\">um^3</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550537</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_C</sourceName>\n" +
"        <destinationName>cell_C</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"description\">cellular C content  calculated from biovolume using the relationships of Menden-Deuer &amp; Lessard (2000)</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell C</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550538</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_Si</sourceName>\n" +
"        <destinationName>cell_Si</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">50.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"description\">total elemental Si content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Dissolved Nutrients</att>\n" +
"            <att name=\"long_name\">Mole Concentration Of Silicate In Sea Water</att>\n" +
"            <att name=\"standard_name\">mole_concentration_of_silicate_in_sea_water</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550539</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_P</sourceName>\n" +
"        <destinationName>cell_P</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"description\">total elemental P content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell P</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550540</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_S</sourceName>\n" +
"        <destinationName>cell_S</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">NaN</att>\n" +
"            <att name=\"description\">total elemental S content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell S</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550541</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_Mn</sourceName>\n" +
"        <destinationName>cell_Mn</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"description\">total elemental Mn content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell Mn</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550542</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_Fe</sourceName>\n" +
"        <destinationName>cell_Fe</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"description\">total elemental Fe content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell Fe</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550543</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_Co</sourceName>\n" +
"        <destinationName>cell_Co</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"description\">total elemental Co content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell Co</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550544</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_Ni</sourceName>\n" +
"        <destinationName>cell_Ni</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"description\">total elemental Ni content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell Ni</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550545</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_Cu</sourceName>\n" +
"        <destinationName>cell_Cu</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"description\">total elemental Cu content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell Cu</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550546</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cell_Zn</sourceName>\n" +
"        <destinationName>cell_Zn</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"description\">total elemental Zn content of each cell was measured with SXRF.  Details provided in Twining et al. (2015).</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cell Zn</att>\n" +
"            <att name=\"units\">mol/cell</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550547</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>light_image_filename</sourceName>\n" +
"        <destinationName>light_image_filename</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">light image filename</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Light Image Filename</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550548</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>chl_image_filename</sourceName>\n" +
"        <destinationName>chl_image_filename</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">Chl image filename</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Chl Image Filename</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550549</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>SXRF_map_filename</sourceName>\n" +
"        <destinationName>SXRF_map_filename</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">SXRF map filename</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">SXRF Map Filename</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550550</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>SXRF_spectrum_filename</sourceName>\n" +
"        <destinationName>SXRF_spectrum_filename</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">SXRF spectrum filename</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">SXRF Spectrum Filename</att>\n" +
"            <att name=\"units\">null</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550551</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>BTL_ISO_DateTime_UTC</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">Date and time (UTC) variable recorded at the bottle sampling time in ISO compliant format. Values were added from the intermediate US GEOTRACES master file (see Processing Description). This standard is based on ISO 8601:2004(E) and takes on the following form: 2009-08-30T14:05:00[.xx]Z (UTC time)</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">BTL ISO Date Time UTC</att>\n" +
"            <att name=\"source_name\">BTL_ISO_DateTime_UTC</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00.00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss.SS&#39;Z&#39;</att>\n" +
"            <att name=\"webpage\">https://www.bco-dmo.org/dataset-parameter/550552</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "tResults=\n" + tResults);

        tResults = gdxResults.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "tResults=\n" + tResults);

    }

    /**
     * This tests that a dataset can be quick restarted.
     */
    public static void testQuickRestart() throws Throwable {
        String2.log("\n*** EDDTableFromAsciiFiles.testQuickRestart\n");
        String datasetID = "testTableAscii";
        String fullName = EDStatic.unitTestDataDir + "asciiNdbc/46012_2005.csv";
        long timestamp = File2.getLastModified(fullName); //orig 2009-08-05T08:49 local
        try {
            //restart local erddap
            String2.pressEnterToContinue(
                "Restart the local erddap with quickRestart=true and with datasetID=" +
                datasetID + " .\n" +
                "Wait until all datasets are loaded.");

            //change the file's timestamp
            File2.setLastModified(fullName, timestamp - 60000); //1 minute earlier
            Math2.sleep(1000);

            //request info from that dataset
            //.csv    for one lat,lon,time
            //46012 -122.879997    37.360001
            String userDapQuery = 
                "&longitude=-122.88&latitude=37.36&time%3E=2005-07-01&time%3C2005-07-01T10";
            String results = SSR.getUrlResponseStringUnchanged(
                EDStatic.erddapUrl + "/tabledap/" + datasetID + ".csv?" + userDapQuery);
            //String2.log(results);
            String expected = 
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

            //request status.html
            SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/status.html");
            Math2.sleep(1000);
            SSR.displayInBrowser("file://" + EDStatic.bigParentDirectory + "logs/log.txt");

            String2.pressEnterToContinue(
                "Look at log.txt to see if update was run and successfully "+
                "noticed the changed file.");

        } finally {
            //change timestamp back to original
            File2.setLastModified(fullName, timestamp);
        }
    }

    /**
     * This tests querying a dataset that is using standardizeWhat. 
     */
    public static void testStandardizeWhat() throws Throwable {
        String2.log("\n*** EDDTableFromAsciiFiles.testStandardizeWhat\n");

        String tID = "testStandardizeWhat";
        EDD.deleteCachedDatasetInfo(tID);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, tID); 
        String tName, results, expected;

        Table table = new Table();
        //public void readASCII(String fullFileName, String charset, 
        //    String skipHeaderToRegex, String skipLinesRegex,
        //    int columnNamesLine, int dataStartLine, String tColSeparator,
        //    String testColumns[], double testMin[], double testMax[], 
        //    String loadColumns[], boolean simplify) throws Exception {
        table.readASCII(String2.unitTestDataDir + "ascii/standardizeWhat1.csv",
            "", "", "", 
            0, 1, null, 
            null, null, null,
            null, false);
        results = table.dataToString();
        expected = 
"date,data\n" +
"20100101000000,1\n" +
"20100102000000,2\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        table.standardize(2048);
        results = table.dataToString();
        expected = 
"date,data\n" +
"2010-01-01T00:00:00Z,1\n" +
"2010-01-02T00:00:00Z,2\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //generateDatasetsXml doesn't suggest standardizeWhat
        //Admin must request it.
        results = EDDTableFromAsciiFiles.generateDatasetsXml(
            String2.unitTestDataDir + "ascii/", "standardizeWhat.*\\.csv", "",
            "", 1, 2, ",", 10080, //colNamesRow, firstDataRow, colSeparator, reloadEvery
            "", "", "", "", "",  //regex
            "", // tSortFilesBySourceNames, 
            "", "", "", "", 2048, "", null);  //info, institution, summary, title, standardizeWhat=2048, cacheFromUrl, atts
        expected = 
"<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n" +
"  below, notably 'units' for each of the dataVariables. -->\n" +
"<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"ascii_7d47_7f77_dc41\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTest/ascii/</fileDir>\n" +
"    <fileNameRegex>standardizeWhat.*\\.csv</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <standardizeWhat>2048</standardizeWhat>\n" +
"    <charset>ISO-8859-1</charset>\n" +
"    <columnSeparator>,</columnSeparator>\n" +
"    <columnNamesRow>1</columnNamesRow>\n" +
"    <firstDataRow>2</firstDataRow>\n" +
"    <sortedColumnSourceName>date</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>date</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"keywords\">data, date, local, source, time</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"summary\">Data from a local source.</att>\n" +
"        <att name=\"title\">Data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>date</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ssZ</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Date</att>\n" +
"            <att name=\"source_name\">date</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss&#39;Z&#39;</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>data</sourceName>\n" +
"        <destinationName>data</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Data</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //das 
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_TestStandadizeWhat", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"Attributes {\n" +
" s {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.262304e+9, 1.2625632e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date\";\n" +
"    String source_name \"date\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01T00:00:00Z\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  data {\n" +
"    Int32 _FillValue 2147483647;\n" +
"    Int32 actual_range 1, 4;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Data\";\n" +
"  }\n" +
" }\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        //get data from first file
        tName = eddTable.makeNewFileForDapQuery(null, null, "&time=2010-01-01", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_TestStandadizeWhat", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time,data\n" +
"UTC,\n" +
"2010-01-01T00:00:00Z,1\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //get data from second file
        tName = eddTable.makeNewFileForDapQuery(null, null, "&time=2010-01-03", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_TestStandadizeWhat", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time,data\n" +
"UTC,\n" +
"2010-01-03T00:00:00Z,3\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


    }

    /**
     * This tests the /files/ "files" system.
     * This requires testTableAscii in the localhost ERDDAP.
     */
    public static void testFiles() throws Throwable {

        String2.log("\n*** EDDTableFromAsciiFiles.testFiles()\n");
        String tDir = EDStatic.fullTestCacheDirectory;
        String dapQuery, tName, start, query, results, expected;
        int po;

        try {
            //get /files/datasetID/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableAscii/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"subdir/,NaN,NaN,\n" +
"31201_2009.csv,1576697736354,201320,\n" +
"46026_2005.csv,1576697015884,621644,\n" +
"46028_2005.csv,1576697015900,623250,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //get /files/datasetID/
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableAscii/");
            Test.ensureTrue(results.indexOf("subdir&#x2f;")             > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("subdir/")                  > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("31201&#x5f;2009&#x2e;csv") > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">201320<")                 > 0, "results=\n" + results);

            //get /files/datasetID/subdir/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableAscii/subdir/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"46012_2005.csv,1576697015869,622197,\n" +
"46012_2006.csv,1576697015884,621812,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //download a file in root
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableAscii/31201_2009.csv");
            expected = 
"This is a header line.\n" +
"*** END OF HEADER\n" +
"# a comment line\n" +
"longitude, latitude, altitude, time, station, wd, wspd, atmp, wtmp\n" +
"# a comment line\n" +
"degrees_east, degrees_north, m, UTC, , degrees_true, m s-1, degree_C, degree_C\n" +
"# a comment line\n" +
"-48.13, -27.7, 0.0, 2005-04-19T00:00:00Z, 31201, NaN, NaN, NaN, 24.4\n" +
"# a comment line\n" +
"#-48.13, -27.7, 0.0, 2005-04-19T01:00:00Z, 31201, NaN, NaN, NaN, 24.4\n" +
"-48.13, -27.7, 0.0, 2005-04-19T01:00:00Z, 31201, NaN, NaN, NaN, 24.4\n"; 
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //download a file in subdir
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableAscii/subdir/46012_2005.csv");
            expected = 
"This is a header line.\n" +
"*** END OF HEADER\n" +
"# a comment line\n" +
"longitude, latitude, altitude, time, station, wd, wspd, atmp, wtmp\n" +
"# a comment line\n" +
"degrees_east, degrees_north, m, UTC, , degrees_true, m s-1, degree_C, degree_C\n" +
"# a comment line\n" +
"-122.88, 37.36, 0.0, 2005-01-01T00:00:00Z, 46012, 190, 8.2, 11.8, 12.5\n" +
"# a comment line\n" +
"# a comment line\n" +
"-122.88, 37.36, 0.0, 2005-01-01T01:00:00Z, 46012, 214, 8.4, 10.4, 12.5\n"; 
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //try to download a non-existent dataset
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent directory
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/testTableAscii/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/testTableAscii/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/testTableAscii/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/testTableAscii/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file in existant subdir
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/testTableAscii/subdir/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/testTableAscii/subdir/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

 

        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error. This test requires testTableAscii in the localhost ERDDAP.", t); 
        } 
    }

    /**
     * This tests threading.
     *
     * @throws Throwable if trouble
     */
    public static void testNThreads() throws Throwable {
        String2.log("\n*** EDDTableFromAsciiFiles.testNThreads()\n");

        Table.verbose = false;
        Table.reallyVerbose = false;
        EDD.verbose = false;
        EDD.reallyVerbose = false;
        EDD.debugMode = false;
        EDDTableFromFilesCallable.debugMode = true;
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String dir = EDStatic.fullTestCacheDirectory;
        String error = "";
        int po;

        //one time: make csv version of ndbc2/nrt directory in ndbcMet2Csv
        /*
        String sourceDir = "/u00/data/points/ndbcMet2/nrt/";
        String list[] = (new File(sourceDir)).list();
        for (int i = 0; i < list.length; i++) { //convert each
            Table table = new Table();
            table.readNDNc(sourceDir + list[i], null, 0, null, Double.NaN, Double.NaN);
            table.saveAsCsvASCII("/u00/data/points/ndbcMet2Csv/" + File2.getNameNoExtension(list[i]) + ".csv");
        }
        */
        
        StringBuilder bigResults = new StringBuilder("\nbigResults:\n");

        //this dataset and this request are a good test that the results are always in the same order
        String id = "ndbcMet2Csv";
        userDapQuery = "&time=2020-05-22T20:40:00Z";

        //warmup
        EDDTableFromAsciiFiles eddTable = (EDDTableFromAsciiFiles)oneFromDatasetsXml(null, id); 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            "testNThreadsWarmup", ".csv"); 

        //test
        for (int i = 5; i > -5; i--) {  
            if (i == 0)
                continue;
            eddTable.nThreads = Math.abs(i);
            Math2.gc(5000);

            long startTime = System.currentTimeMillis();
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
                "testNThreads" + i, ".csv"); 

            long eTime = System.currentTimeMillis() - startTime;
            String msg = "nThreads=" + eddTable.nThreads + " time=" + eTime + "ms\n";
            String2.log(msg);
            bigResults.append(msg);
            if (eTime > 18000)
                throw new RuntimeException("Too slow!\n" + bigResults);

            results = String2.directReadFrom88591File(dir + tName);
            //String2.log(results);            
            expected =  //ensure that order is correct
"ID,time,depth,latitude,longitude,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV\n" +
",UTC,m,degrees_north,degrees_east,degrees_true,m s-1,m s-1,m,s,s,degrees_true,hPa,degree_C,degree_C,degree_C,km,hPa,m,m s-1,m s-1\n" +
"41001,2020-05-22T20:40:00Z,0.0,34.675,-72.698,160,7.0,9.0,1.7,NaN,5.9,84,1020.6,21.5,21.7,18.4,NaN,NaN,NaN,-2.4,6.6\n" +
"41004,2020-05-22T20:40:00Z,0.0,32.501,-79.099,NaN,4.0,5.0,NaN,NaN,NaN,,1018.2,24.8,24.4,24.8,NaN,NaN,NaN,NaN,NaN\n" +
"41009,2020-05-22T20:40:00Z,0.0,28.519,-80.166,110,5.0,7.0,NaN,NaN,NaN,,1018.2,27.2,27.4,20.6,NaN,NaN,NaN,-4.7,1.7\n" +
"41010,2020-05-22T20:40:00Z,0.0,28.906,-78.471,130,5.0,6.0,1.8,NaN,7.9,54,NaN,26.4,NaN,22.9,NaN,NaN,NaN,-3.8,3.2\n" +
"41013,2020-05-22T20:40:00Z,0.0,33.436,-77.743,160,5.0,6.0,NaN,NaN,NaN,,1017.8,25.1,24.9,20.6,NaN,NaN,NaN,-1.7,4.7\n" +
"41025,2020-05-22T20:40:00Z,0.0,35.006,-75.402,190,8.0,10.0,NaN,NaN,NaN,,1017.8,23.5,24.4,21.1,NaN,NaN,NaN,1.4,7.9\n" +
"41040,2020-05-22T20:40:00Z,0.0,14.477,-53.008,70,6.0,7.0,NaN,NaN,NaN,,1015.3,26.8,NaN,22.6,NaN,NaN,NaN,-5.6,-2.1\n" +
"41043,2020-05-22T20:40:00Z,0.0,21.061,-64.966,130,1.0,2.0,NaN,NaN,NaN,,1015.7,28.3,28.8,24.8,NaN,NaN,NaN,-0.8,0.6\n" +
"41044,2020-05-22T20:40:00Z,0.0,21.652,-58.695,100,4.0,5.0,1.3,NaN,7.2,3,NaN,25.6,NaN,22.2,NaN,NaN,NaN,-3.9,0.7\n" +
"41046,2020-05-22T20:40:00Z,0.0,23.836,-70.863,80,4.0,5.0,NaN,NaN,NaN,,1017.1,26.7,27.6,24.4,NaN,NaN,NaN,-3.9,-0.7\n" +
"41047,2020-05-22T20:40:00Z,0.0,27.469,-71.491,NaN,5.0,7.0,NaN,NaN,NaN,,1019.6,24.7,NaN,19.1,NaN,NaN,NaN,NaN,NaN\n" +
"41048,2020-05-22T20:40:00Z,0.0,31.978,-69.649,110,7.0,9.0,2.3,NaN,6.6,93,1022.4,NaN,22.4,NaN,NaN,NaN,NaN,-6.6,2.4\n" +
"41049,2020-05-22T20:40:00Z,0.0,27.5,-63.0,90,9.0,12.0,3.0,NaN,7.1,28,1019.0,NaN,NaN,NaN,NaN,NaN,NaN,-9.0,0.0\n" +
"41053,2020-05-22T20:40:00Z,0.0,18.476,-66.099,150,4.0,6.0,NaN,NaN,NaN,,1013.3,30.7,NaN,NaN,NaN,NaN,NaN,-2.0,3.5\n" +
"41056,2020-05-22T20:40:00Z,0.0,18.26,-65.458,140,4.0,5.0,NaN,NaN,NaN,,1013.9,28.4,NaN,NaN,NaN,NaN,NaN,-2.6,3.1\n" +
"41110,2020-05-22T20:40:00Z,0.0,34.141,-77.709,NaN,NaN,NaN,1.1,7.0,5.7,113,NaN,NaN,22.4,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"42001,2020-05-22T20:40:00Z,0.0,25.888,-89.658,130,5.0,7.0,NaN,NaN,NaN,,1014.3,NaN,NaN,NaN,NaN,NaN,NaN,-3.8,3.2\n" +
"42002,2020-05-22T20:40:00Z,0.0,25.79,-93.666,150,8.0,9.0,NaN,NaN,NaN,,1012.0,27.0,26.9,24.1,NaN,NaN,NaN,-4.0,6.9\n" +
"42003,2020-05-22T20:40:00Z,0.0,26.044,-85.612,100,6.0,8.0,NaN,NaN,NaN,,1016.3,NaN,27.3,NaN,NaN,NaN,NaN,-5.9,1.0\n" +
"42012,2020-05-22T20:40:00Z,0.0,30.065,-87.555,180,4.0,5.0,NaN,NaN,NaN,,1016.3,26.6,26.8,26.1,NaN,NaN,NaN,0.0,4.0\n" +
"42019,2020-05-22T20:40:00Z,0.0,27.913,-95.353,130,4.0,6.0,NaN,NaN,NaN,,1011.5,27.0,26.3,25.5,NaN,NaN,NaN,-3.1,2.6\n" +
"42020,2020-05-22T20:40:00Z,0.0,26.966,-96.695,140,5.0,6.0,1.5,NaN,5.5,127,1010.1,27.7,NaN,25.6,NaN,NaN,NaN,-3.2,3.8\n" +
"42035,2020-05-22T20:40:00Z,0.0,29.232,-94.413,160,5.0,7.0,NaN,NaN,NaN,,1012.4,27.2,26.9,25.9,NaN,NaN,NaN,-1.7,4.7\n" +
"42036,2020-05-22T20:40:00Z,0.0,28.5,-84.517,100,1.0,1.0,NaN,NaN,NaN,,1017.6,NaN,NaN,NaN,NaN,NaN,NaN,-1.0,0.2\n" +
"42039,2020-05-22T20:40:00Z,0.0,28.791,-86.008,130,2.0,3.0,NaN,NaN,NaN,,1016.6,NaN,26.8,NaN,NaN,NaN,NaN,-1.5,1.3\n" +
"42040,2020-05-22T20:40:00Z,0.0,29.212,-88.207,180,4.0,4.0,NaN,NaN,NaN,,1016.4,27.3,NaN,25.1,NaN,NaN,NaN,0.0,4.0\n" +
"42055,2020-05-22T20:40:00Z,0.0,22.017,-94.046,130,4.0,5.0,NaN,NaN,NaN,,1009.5,28.6,28.5,27.2,NaN,NaN,NaN,-3.1,2.6\n" +
"42056,2020-05-22T20:40:00Z,0.0,19.874,-85.059,130,5.0,7.0,NaN,NaN,NaN,,1013.4,28.8,28.9,22.9,NaN,NaN,NaN,-3.8,3.2\n" +
"42057,2020-05-22T20:40:00Z,0.0,16.834,-81.501,110,7.0,8.0,NaN,NaN,NaN,,1012.3,28.1,28.5,25.4,NaN,NaN,NaN,-6.6,2.4\n" +
"42058,2020-05-22T20:40:00Z,0.0,15.093,-75.064,80,9.0,12.0,1.9,NaN,5.3,112,1011.1,28.2,27.9,25.3,NaN,NaN,NaN,-8.9,-1.6\n" +
"42059,2020-05-22T20:40:00Z,0.0,15.252,-67.483,110,7.0,9.0,NaN,NaN,NaN,,1012.9,28.3,28.3,26.6,NaN,NaN,NaN,-6.6,2.4\n" +
"42060,2020-05-22T20:40:00Z,0.0,16.5,-63.5,120,6.0,7.0,NaN,NaN,NaN,,1014.5,28.0,28.3,25.3,NaN,NaN,NaN,-5.2,3.0\n" +
"42085,2020-05-22T20:40:00Z,0.0,17.86,-66.524,120,5.0,7.0,NaN,NaN,NaN,,1013.7,28.9,NaN,NaN,NaN,NaN,NaN,-4.3,2.5\n" +
"42395,2020-05-22T20:40:00Z,0.0,26.407,-90.845,160,7.0,9.0,1.2,6.0,NaN,,1013.7,27.7,27.6,NaN,NaN,NaN,NaN,-2.4,6.6\n" +
"44008,2020-05-22T20:40:00Z,0.0,40.502,-69.247,210,5.0,5.0,NaN,NaN,NaN,,1020.6,NaN,NaN,NaN,NaN,NaN,NaN,2.5,4.3\n" +
"44011,2020-05-22T20:40:00Z,0.0,41.118,-66.578,NaN,8.0,9.0,NaN,NaN,NaN,,1019.7,NaN,7.6,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"44014,2020-05-22T20:40:00Z,0.0,36.611,-74.836,160,5.0,7.0,NaN,NaN,NaN,,1018.4,16.5,14.5,15.5,NaN,NaN,NaN,-1.7,4.7\n" +
"44017,2020-05-22T20:40:00Z,0.0,40.692,-72.048,210,3.0,4.0,NaN,NaN,NaN,,1020.0,14.8,11.7,13.1,NaN,NaN,NaN,1.5,2.6\n" +
"44020,2020-05-22T20:40:00Z,0.0,41.443,-70.186,210,7.0,8.0,NaN,NaN,NaN,,1017.7,14.2,12.5,12.4,NaN,NaN,NaN,3.5,6.1\n" +
"44025,2020-05-22T20:40:00Z,0.0,40.25,-73.166,160,3.0,4.0,NaN,NaN,NaN,,1019.2,14.3,11.1,12.9,NaN,NaN,NaN,-1.0,2.8\n" +
"44064,2020-05-22T20:40:00Z,0.0,36.979,-76.043,120,5.0,6.0,NaN,NaN,NaN,,1015.5,NaN,NaN,NaN,NaN,NaN,NaN,-4.3,2.5\n" +
"44065,2020-05-22T20:40:00Z,0.0,40.369,-73.703,100,3.0,3.0,NaN,NaN,NaN,,1019.1,14.4,12.9,13.4,NaN,NaN,NaN,-3.0,0.5\n" +
"44066,2020-05-22T20:40:00Z,0.0,39.583,-72.601,NaN,NaN,NaN,NaN,NaN,NaN,,1020.5,NaN,11.7,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"44072,2020-05-22T20:40:00Z,0.0,37.201,-76.266,210,5.0,6.0,NaN,NaN,NaN,,NaN,18.8,NaN,NaN,NaN,NaN,NaN,2.5,4.3\n" +
"45029,2020-05-22T20:40:00Z,0.0,42.801,-86.264,340,1.0,1.0,0.1,NaN,NaN,174,1017.2,12.1,10.2,10.6,NaN,NaN,NaN,0.3,-0.9\n" +
"45165,2020-05-22T20:40:00Z,0.0,41.806,-83.271,60,3.0,3.0,0.2,NaN,NaN,75,NaN,13.3,13.3,13.3,NaN,NaN,NaN,-2.6,-1.5\n" +
"45168,2020-05-22T20:40:00Z,0.0,42.396,-86.331,340,2.0,2.0,0.1,NaN,NaN,60,1017.3,12.5,14.1,10.5,NaN,NaN,NaN,0.7,-1.9\n" +
"46005,2020-05-22T20:40:00Z,0.0,46.1,-131.001,240,5.0,5.0,1.5,NaN,6.9,289,1023.5,11.3,11.6,7.2,NaN,NaN,NaN,4.3,2.5\n" +
"46011,2020-05-22T20:40:00Z,0.0,34.868,-120.857,320,10.0,13.0,2.7,NaN,6.0,314,1014.6,NaN,12.9,NaN,NaN,NaN,NaN,6.4,-7.7\n" +
"46012,2020-05-22T20:40:00Z,0.0,37.363,-122.881,320,12.0,16.0,NaN,NaN,NaN,,1015.9,12.6,12.7,9.3,NaN,NaN,NaN,7.7,-9.2\n" +
"46013,2020-05-22T20:40:00Z,0.0,38.242,-123.301,320,14.0,18.0,NaN,NaN,NaN,,1014.6,11.7,11.1,8.4,NaN,NaN,NaN,9.0,-10.7\n" +
"46014,2020-05-22T20:40:00Z,0.0,39.196,-123.969,320,14.0,17.0,2.7,NaN,5.4,335,1016.1,12.3,11.5,9.5,NaN,NaN,NaN,9.0,-10.7\n" +
"46015,2020-05-22T20:40:00Z,0.0,42.747,-124.823,350,7.0,8.0,NaN,NaN,NaN,,1022.9,12.0,13.9,7.5,NaN,NaN,NaN,1.2,-6.9\n" +
"46022,2020-05-22T20:40:00Z,0.0,40.763,-124.577,340,12.0,14.0,NaN,NaN,NaN,,1020.0,12.8,13.6,7.8,NaN,NaN,NaN,4.1,-11.3\n" +
"46025,2020-05-22T20:40:00Z,0.0,33.749,-119.053,200,3.0,3.0,NaN,NaN,NaN,,1012.9,16.3,17.9,13.9,NaN,NaN,NaN,1.0,2.8\n" +
"46026,2020-05-22T20:40:00Z,0.0,37.759,-122.833,320,11.0,14.0,NaN,NaN,NaN,,1015.3,11.7,11.3,8.5,NaN,NaN,NaN,7.1,-8.4\n" +
"46027,2020-05-22T20:40:00Z,0.0,41.85,-124.381,330,12.0,16.0,NaN,NaN,NaN,,1020.5,NaN,12.8,NaN,NaN,NaN,NaN,6.0,-10.4\n" +
"46028,2020-05-22T20:40:00Z,0.0,35.741,-121.884,320,12.0,15.0,NaN,NaN,NaN,,1014.2,12.9,11.9,9.7,NaN,NaN,NaN,7.7,-9.2\n" +
"46029,2020-05-22T20:40:00Z,0.0,46.144,-124.51,310,7.0,9.0,NaN,NaN,NaN,,1022.2,11.0,13.8,7.4,NaN,NaN,NaN,5.4,-4.5\n" +
"46042,2020-05-22T20:40:00Z,0.0,36.789,-122.404,310,11.0,15.0,NaN,NaN,NaN,,1015.4,12.7,12.5,9.1,NaN,NaN,NaN,8.4,-7.1\n" +
"46047,2020-05-22T20:40:00Z,0.0,32.433,-119.533,320,10.0,13.0,NaN,NaN,NaN,,1013.9,14.7,NaN,12.6,NaN,NaN,NaN,6.4,-7.7\n" +
"46050,2020-05-22T20:40:00Z,0.0,44.641,-124.5,330,5.0,6.0,NaN,NaN,NaN,,1022.5,12.4,13.5,5.9,NaN,NaN,NaN,2.5,-4.3\n" +
"46053,2020-05-22T20:40:00Z,0.0,34.248,-119.841,260,4.0,5.0,NaN,NaN,NaN,,1013.6,14.0,14.4,12.3,NaN,NaN,NaN,3.9,0.7\n" +
"46054,2020-05-22T20:40:00Z,0.0,34.274,-120.459,310,11.0,13.0,NaN,NaN,NaN,,1013.6,12.4,11.1,9.4,NaN,NaN,NaN,8.4,-7.1\n" +
"46059,2020-05-22T20:40:00Z,0.0,38.047,-129.969,350,6.0,8.0,1.5,NaN,6.1,289,1025.8,13.5,14.7,8.6,NaN,NaN,NaN,1.0,-5.9\n" +
"46069,2020-05-22T20:40:00Z,0.0,33.67,-120.2,NaN,10.0,13.0,NaN,NaN,NaN,,1014.5,NaN,13.4,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"46086,2020-05-22T20:40:00Z,0.0,32.491,-118.034,NaN,4.0,6.0,1.8,NaN,6.8,187,1013.3,16.0,NaN,12.3,NaN,NaN,NaN,NaN,NaN\n" +
"46088,2020-05-22T20:40:00Z,0.0,48.333,-123.167,NaN,8.0,9.0,NaN,NaN,NaN,,1018.3,NaN,9.9,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"46128,2020-05-22T20:40:00Z,0.0,43.295,-124.537,NaN,NaN,NaN,NaN,NaN,NaN,,NaN,11.5,14.1,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"51000,2020-05-22T20:40:00Z,0.0,23.546,-154.056,70,8.0,10.0,NaN,NaN,NaN,,1019.1,24.0,25.2,20.0,NaN,NaN,NaN,-7.5,-2.7\n" +
"51001,2020-05-22T20:40:00Z,0.0,23.445,-162.279,80,6.0,8.0,NaN,NaN,NaN,,1020.6,24.1,24.5,20.0,NaN,NaN,NaN,-5.9,-1.0\n" +
"51002,2020-05-22T20:40:00Z,0.0,17.094,-157.808,90,7.0,9.0,NaN,NaN,NaN,,1015.1,25.0,25.9,21.1,NaN,NaN,NaN,-7.0,0.0\n" +
"51003,2020-05-22T20:40:00Z,0.0,19.087,-160.66,80,8.0,10.0,NaN,NaN,NaN,,1017.1,NaN,26.3,NaN,NaN,NaN,NaN,-7.9,-1.4\n" +
"51004,2020-05-22T20:40:00Z,0.0,17.525,-152.382,80,7.0,9.0,NaN,NaN,NaN,,1015.3,24.5,25.3,21.8,NaN,NaN,NaN,-6.9,-1.2\n" +
"51101,2020-05-22T20:40:00Z,0.0,24.321,-162.058,80,7.0,8.0,NaN,NaN,NaN,,1019.8,24.0,24.5,19.8,NaN,NaN,NaN,-6.9,-1.2\n" +
"APNM4,2020-05-22T20:40:00Z,0.0,45.06,-83.424,160,5.1,5.7,NaN,NaN,NaN,,NaN,13.8,NaN,NaN,NaN,NaN,NaN,-1.7,4.8\n" +
"BIGM4,2020-05-22T20:40:00Z,0.0,46.83,-87.73,130,2.6,6.2,NaN,NaN,NaN,,1014.6,19.5,NaN,NaN,NaN,NaN,NaN,-2.0,1.7\n" +
"BSBM4,2020-05-22T20:40:00Z,0.0,44.055,-86.514,350,2.6,3.6,NaN,NaN,NaN,,1017.9,13.6,NaN,NaN,NaN,NaN,NaN,0.5,-2.6\n" +
"CHII2,2020-05-22T20:40:00Z,0.0,42.0,-87.5,40,3.1,3.1,NaN,NaN,NaN,,NaN,11.9,NaN,11.9,NaN,NaN,NaN,-2.0,-2.4\n" +
"CLSM4,2020-05-22T20:40:00Z,0.0,42.471,-82.877,150,1.5,2.6,NaN,NaN,NaN,,1017.6,17.7,NaN,NaN,NaN,NaN,NaN,-0.7,1.3\n" +
"FPTM4,2020-05-22T20:40:00Z,0.0,45.619,-86.659,130,1.5,1.5,NaN,NaN,NaN,,1016.6,12.8,NaN,NaN,NaN,NaN,NaN,-1.1,1.0\n" +
"GRIM4,2020-05-22T20:40:00Z,0.0,46.721,-87.412,120,3.6,5.7,NaN,NaN,NaN,,1016.1,7.6,NaN,3.3,NaN,NaN,NaN,-3.1,1.8\n" +
"GRMM4,2020-05-22T20:40:00Z,0.0,46.68,-85.97,40,2.6,4.1,NaN,NaN,NaN,,1017.3,10.8,NaN,NaN,NaN,NaN,NaN,-1.7,-2.0\n" +
"GSLM4,2020-05-22T20:40:00Z,0.0,44.018,-83.537,30,5.7,6.2,NaN,NaN,NaN,,NaN,16.5,NaN,NaN,NaN,NaN,NaN,-2.8,-4.9\n" +
"GTLM4,2020-05-22T20:40:00Z,0.0,45.211,-85.55,30,1.0,2.1,NaN,NaN,NaN,,1017.6,15.5,NaN,NaN,NaN,NaN,NaN,-0.5,-0.9\n" +
"HHLO1,2020-05-22T20:40:00Z,0.0,41.401,-82.545,80,1.5,2.6,NaN,NaN,NaN,,1016.3,13.8,NaN,NaN,NaN,NaN,NaN,-1.5,-0.3\n" +
"KNSW3,2020-05-22T20:40:00Z,0.0,42.589,-87.809,NaN,0.0,0.5,NaN,NaN,NaN,,1016.9,11.7,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"LDLC3,2020-05-22T20:40:00Z,0.0,41.305,-72.077,240,5.7,7.2,NaN,NaN,NaN,,1017.9,17.5,NaN,12.8,NaN,NaN,NaN,4.9,2.8\n" +
"LMFS1,2020-05-22T20:40:00Z,0.0,34.107,-81.271,280,1.5,2.6,NaN,NaN,NaN,,NaN,28.0,NaN,NaN,NaN,NaN,NaN,1.5,-0.3\n" +
"LORO1,2020-05-22T20:40:00Z,0.0,41.481,-82.195,50,2.1,2.1,NaN,NaN,NaN,,NaN,13.1,NaN,NaN,NaN,NaN,NaN,-1.6,-1.3\n" +
"MCYI3,2020-05-22T20:40:00Z,0.0,41.729,-86.913,50,2.1,2.1,NaN,NaN,NaN,,NaN,13.2,NaN,13.0,NaN,NaN,NaN,-1.6,-1.3\n" +
"MEEM4,2020-05-22T20:40:00Z,0.0,44.248,-86.346,0,2.6,3.1,NaN,NaN,NaN,,1018.3,15.4,NaN,NaN,NaN,NaN,NaN,0.0,-2.6\n" +
"MKGM4,2020-05-22T20:40:00Z,0.0,43.228,-86.339,340,3.1,3.1,NaN,NaN,NaN,,1016.8,16.5,NaN,13.7,NaN,NaN,NaN,1.1,-2.9\n" +
"MLWW3,2020-05-22T20:40:00Z,0.0,43.046,-87.879,100,2.6,3.1,NaN,NaN,NaN,,NaN,10.7,NaN,NaN,NaN,NaN,NaN,-2.6,0.5\n" +
"NABM4,2020-05-22T20:40:00Z,0.0,46.087,-85.443,160,1.0,1.5,NaN,NaN,NaN,,1016.3,15.7,NaN,NaN,NaN,NaN,NaN,-0.3,0.9\n" +
"NBBA3,2020-05-22T20:40:00Z,0.0,36.087,-114.728,170,9.8,14.9,0.3,2.0,NaN,,1001.6,29.9,NaN,NaN,NaN,NaN,NaN,-1.7,9.7\n" +
"NLMA3,2020-05-22T20:40:00Z,0.0,35.458,-114.666,NaN,NaN,NaN,0.5,2.0,NaN,,NaN,29.8,22.2,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"NPDW3,2020-05-22T20:40:00Z,0.0,45.29,-86.978,40,2.1,2.6,NaN,NaN,NaN,,1016.6,19.0,NaN,NaN,NaN,NaN,NaN,-1.3,-1.6\n" +
"OLCN6,2020-05-22T20:40:00Z,0.0,43.341,-78.719,80,3.1,4.1,NaN,NaN,NaN,,1016.9,10.8,NaN,NaN,NaN,NaN,NaN,-3.1,-0.5\n" +
"PNGW3,2020-05-22T20:40:00Z,0.0,46.792,-91.386,310,0.5,1.5,NaN,NaN,NaN,,1015.6,8.1,NaN,NaN,NaN,NaN,NaN,0.4,-0.3\n" +
"PSCM4,2020-05-22T20:40:00Z,0.0,43.423,-82.536,170,1.0,1.5,NaN,NaN,NaN,,1034.9,16.9,NaN,NaN,NaN,NaN,NaN,-0.2,1.0\n" +
"PTRP4,2020-05-22T20:40:00Z,0.0,18.367,-67.251,20,2.6,4.6,NaN,NaN,NaN,,NaN,29.0,NaN,NaN,NaN,NaN,NaN,-0.9,-2.4\n" +
"PWAW3,2020-05-22T20:40:00Z,0.0,43.388,-87.868,120,1.5,2.1,NaN,NaN,NaN,,1017.9,14.0,NaN,NaN,NaN,NaN,NaN,-1.3,0.7\n" +
"RPRN6,2020-05-22T20:40:00Z,0.0,43.258,-77.592,90,1.5,2.6,NaN,NaN,NaN,,NaN,16.0,NaN,NaN,NaN,NaN,NaN,-1.5,0.0\n" +
"SBBN2,2020-05-22T20:40:00Z,0.0,36.05,-114.748,170,9.3,15.4,0.4,2.0,NaN,,1001.6,30.6,NaN,NaN,NaN,NaN,NaN,-1.6,9.2\n" +
"SBLM4,2020-05-22T20:40:00Z,0.0,43.806,-83.719,30,4.6,5.7,NaN,NaN,NaN,,1015.9,16.2,NaN,NaN,NaN,NaN,NaN,-2.3,-4.0\n" +
"SISW1,2020-05-22T20:40:00Z,0.0,48.318,-122.843,180,6.7,7.2,NaN,NaN,NaN,,1018.5,11.4,NaN,6.9,NaN,NaN,NaN,0.0,6.7\n" +
"SJOM4,2020-05-22T20:40:00Z,0.0,42.099,-86.494,30,2.6,3.6,NaN,NaN,NaN,,1016.9,14.7,NaN,NaN,NaN,NaN,NaN,-1.3,-2.3\n" +
"SLVM5,2020-05-22T20:40:00Z,0.0,47.269,-91.252,50,2.6,5.1,NaN,NaN,NaN,,1015.6,7.4,NaN,NaN,NaN,NaN,NaN,-2.0,-1.7\n" +
"SMKF1,2020-05-22T20:40:00Z,0.0,24.627,-81.11,100,8.8,10.8,NaN,NaN,NaN,,NaN,28.0,NaN,23.2,NaN,NaN,NaN,-8.7,1.5\n" +
"SVNM4,2020-05-22T20:40:00Z,0.0,42.401,-86.289,350,1.0,1.5,NaN,NaN,NaN,,NaN,13.9,NaN,NaN,NaN,NaN,NaN,0.2,-1.0\n" +
"SXHW3,2020-05-22T20:40:00Z,0.0,46.563,-90.44,10,1.5,2.1,NaN,NaN,NaN,,1015.6,10.8,NaN,NaN,NaN,NaN,NaN,-0.3,-1.5\n" +
"TAWM4,2020-05-22T20:40:00Z,0.0,44.256,-83.443,70,2.1,3.6,NaN,NaN,NaN,,1016.9,18.2,NaN,NaN,NaN,NaN,NaN,-2.0,-0.7\n" +
"TBIM4,2020-05-22T20:40:00Z,0.0,45.035,-83.194,150,1.5,2.1,NaN,NaN,NaN,,NaN,11.6,NaN,NaN,NaN,NaN,NaN,-0.7,1.3\n" +
"THLO1,2020-05-22T20:40:00Z,0.0,41.826,-83.194,100,3.1,3.1,NaN,NaN,NaN,,NaN,12.9,NaN,NaN,NaN,NaN,NaN,-3.1,0.5\n" +
"TIBC1,2020-05-22T20:40:00Z,0.0,37.891,-122.447,140,4.6,NaN,NaN,NaN,NaN,,1014.0,17.1,NaN,NaN,NaN,NaN,NaN,-3.0,3.5\n" +
"TWCO1,2020-05-22T20:40:00Z,0.0,41.699,-83.259,70,3.6,4.1,NaN,NaN,NaN,,NaN,NaN,13.0,NaN,NaN,NaN,NaN,-3.4,-1.2\n" +
"VBBA3,2020-05-22T20:40:00Z,0.0,36.132,-114.412,180,8.2,11.8,0.3,2.0,NaN,,1001.9,30.2,NaN,NaN,NaN,NaN,NaN,0.0,8.2\n" +
"WFPM4,2020-05-22T20:40:00Z,0.0,46.762,-84.966,0,4.6,6.7,NaN,NaN,NaN,,1015.9,18.7,NaN,NaN,NaN,NaN,NaN,0.0,-4.6\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

        }
        String2.log(bigResults.toString());
/* times truncted to seconds
     2020-06-16 new 
nThreads=5 time=13  
nThreads=4 time=13  
nThreads=3 time=13  
nThreads=2 time=14  
nThreads=1 time=14  
nThreads=1 time=14  
nThreads=2 time=15  
nThreads=3 time=13  
nThreads=4 time=13  
*/

        Table.verbose = true;
        Table.reallyVerbose = true;
        EDD.verbose = true;
        EDD.reallyVerbose = true;
        EDD.debugMode = false;
        EDDTableFromFilesCallable.debugMode = false;
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
            lastTest = interactive? -1 : 16;
        String msg = "\n^^^ EDDTableFromAsciiFiles.test(" + interactive + ") test=";

        boolean deleteCachedDatasetInfo = false; //usually false, rarely true

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                    //not usually run
                    if (test == 1000) testQuickRestart();

                } else {
                    if (test ==  0) testBasic(deleteCachedDatasetInfo);
                    if (test ==  1) testGenerateDatasetsXml();
                    if (test ==  2) testGenerateDatasetsXml2();
                    if (test ==  3) testGenerateDatasetsXmlFromInPort();
                    if (test ==  4) testGenerateDatasetsXmlFromInPort2();
                    if (test ==  5) testGenerateDatasetsXmlFromBCODMO();
                    if (test ==  6) testGenerateDatasetsXmlWithMV();
                    if (test ==  7) testFixedValueAndScripts();
                    if (test ==  8) testBasic2();
                    if (test ==  9) testTimeZone();
                    if (test == 10) testTimeZone2();
                    if (test == 11) testTimeMV();
                    if (test == 12) testTimeRange();
                    if (test == 13) testTimeRange2();
                    if (test == 14) testStandardizeWhat();
                    if (test == 15) testFiles();
                    if (test == 16) testNThreads();

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

