/* 
 * EDDTableFromAudioFiles Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
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

//import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


/** 
 * This class represents a table of sampled sound data from a collection of 
 * audio files (e.g., WAV or AU).
 * See Table.readAudio file for info on which types of audio files can be read.
 * https://docs.oracle.com/javase/tutorial/sound/converters.html
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2017-08-29
 */
public class EDDTableFromAudioFiles extends EDDTableFromFiles { 


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
    public EDDTableFromAudioFiles(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo,
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
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles, boolean tRemoveMVRows, 
        int tStandardizeWhat, int tNThreads, 
        String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex,
        String tAddVariablesWhere) 
        throws Throwable {

        super("EDDTableFromAudioFiles", tDatasetID, 
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
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows, tStandardizeWhat, 
            tNThreads, tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex,
            tAddVariablesWhere);

        //String2.log(">> EDDTableFromAudioFiles end of constructor:\n" + toString());
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
        String decompFullName = FileVisitorDNLS.decompressIfNeeded(
            tFileDir + tFileName, fileDir, decompressedDirectory(), 
            EDStatic.decompressedCacheMaxGB, true); //reuseExisting
        table.readAudioFile(decompFullName, mustGetData, true); //addElapsedTime

        //unpack
        table.standardize(standardizeWhat);
        //String2.log(">> EDDTableFromAudioFiles.lowGetSourceDataFromFile header after unpack (nCol=" + table.nColumns() + "):\n" + table.getNCHeader("row"));

        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromAudioFiles.
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
     * @param tExtractUnits          e.g., yyyyMMdd'_'HHmmss, or "" if not needed.
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
        String tColumnNameForExtract, String tExtractUnits, //String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        int tStandardizeWhat, String tCacheFromUrl,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** EDDTableFromAudioFiles.generateDatasetsXml" +
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
        String tSortedColumnSourceName = Table.ELAPSED_TIME;
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; 
        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        dataSourceTable.readAudioFile(sampleFileName, true, true); //getMetadata, addElapsedTime

        tStandardizeWhat = tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE?
            DEFAULT_STANDARDIZEWHAT : tStandardizeWhat;
        dataSourceTable.standardize(tStandardizeWhat);

        Table dataAddTable = new Table();
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            PrimitiveArray sourcePA = dataSourceTable.getColumn(c);
            PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts); 
            dataAddTable.addColumn(c, colName, destPA,
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    dataSourceTable.globalAttributes(), sourceAtts, null, colName, 
                    destPA.elementType() != PAType.STRING, //tryToAddStandardName
                    destPA.elementType() != PAType.STRING, //addColorBarMinMax
                    true)); //tryToFindLLAT
            if (c > 0) {
                if (EDStatic.variablesMustHaveIoosCategory)
                    dataAddTable.columnAttributes(c).set("ioos_category", "Other");
                if (sourcePA.isIntegerType()) {
                    dataAddTable.columnAttributes(c)
                        .add("colorBarMinimum", Math2.niceDouble(-sourcePA.missingValueAsDouble(), 2))
                        .add("colorBarMaximum", Math2.niceDouble( sourcePA.missingValueAsDouble(), 2));
                }
            }

            //if a variable has timeUnits, files are likely sorted by time
            //and no harm if files aren't sorted that way
            //if (tSortedColumnSourceName.length() == 0 && 
            //    EDVTimeStamp.hasTimeUnits(sourceAtts, null))
            //    tSortedColumnSourceName = colName;
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
        if ("Data from a local source.".equals(dataAddTable.globalAttributes().getString("summary")))
            dataAddTable.globalAttributes().set("summary", 
                "Audio data from a local source.");
        if ("Data from a local source.".equals(dataAddTable.globalAttributes().getString("title")))
            dataAddTable.globalAttributes().set("title", 
                "Audio data from a local source.");

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            if (String2.isSomething(tExtractUnits))
                atts.add("units", tExtractUnits);
            //no standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);

            //subsetVariables
            dataAddTable.globalAttributes().add("subsetVariables", tColumnNameForExtract);
        }

        //default queries
        String defGraph = "";
        String defData = "";
        String et = Table.ELAPSED_TIME;
        if ("time".equals(tColumnNameForExtract)) {
            defGraph =  et + ",channel_1&time=min(time)&" + et + ">=0&" + et + "<=1&.draw=lines";
            defData  =  "&time=min(time)";
        } else {
            defGraph =  et + ",channel_1&" + et + ">=0&" + et + "<=1&.draw=lines";
            defData  =  "";
        }

        //write the information
        StringBuilder sb = new StringBuilder();
        String suggestedRegex = (tFileNameRegex == null || tFileNameRegex.length() == 0)? 
            ".*\\" + File2.getExtension(sampleFileName) :
            tFileNameRegex;
        if (tSortFilesBySourceNames.length() == 0) {
            if (tColumnNameForExtract.length() > 0)
                tSortFilesBySourceNames = tColumnNameForExtract;
        }
        sb.append(
            "<dataset type=\"EDDTableFromAudioFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + suggestedRegex) +  //dirs can't be made public
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            (String2.isUrl(tCacheFromUrl)? 
              "    <cacheFromUrl>" + XML.encodeAsXML(tCacheFromUrl) + "</cacheFromUrl>\n" :
              "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + "</updateEveryNMillis>\n") +  
            "    <defaultGraphQuery>" + XML.encodeAsXML(defGraph) + "</defaultGraphQuery>\n" +
            "    <defaultDataQuery>" + XML.encodeAsXML(defData) + "</defaultDataQuery>\n" +
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
            "    <sortedColumnSourceName>" + XML.encodeAsXML(Table.ELAPSED_TIME) + "</sortedColumnSourceName>\n" +
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
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();
        boolean oEDDDebugMode = EDD.debugMode;
        //EDD.debugMode = true;

        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "audio/wav", //test no trailing /
            ".*\\.wav",
            "",
            1440,
            "aco_acoustic\\.", "\\.wav", ".*", "time", "yyyyMMdd'_'HHmmss",
            "", "", "", "", "", 
            -1, null, //defaultStandardizeWhat
            null) + "\n";

        String2.log(results);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromAudioFiles",
            EDStatic.unitTestDataDir + "audio/wav", 
            ".*\\.wav",
            "",
            "1440",
            "aco_acoustic\\.", "\\.wav", ".*", "time", "yyyyMMdd'_'HHmmss",
            "", "", "", "", "", 
            "-1", ""}, //defaultStandardizeWhat
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<dataset type=\"EDDTableFromAudioFiles\" datasetID=\"wav_56d5_230d_1887\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <defaultGraphQuery>elapsedTime,channel_1&amp;time=min(time)&amp;elapsedTime&gt;=0&amp;elapsedTime&lt;=1&amp;.draw=lines</defaultGraphQuery>\n" +
"    <defaultDataQuery>&amp;time=min(time)</defaultDataQuery>\n" +
"    <fileDir>/erddapTest/audio/wav/</fileDir>\n" +
"    <fileNameRegex>.*\\.wav</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <preExtractRegex>aco_acoustic\\.</preExtractRegex>\n" +
"    <postExtractRegex>\\.wav</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>time</columnNameForExtract>\n" +
"    <sortedColumnSourceName>elapsedTime</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"audioBigEndian\">false</att>\n" +
"        <att name=\"audioChannels\" type=\"int\">1</att>\n" +
"        <att name=\"audioEncoding\">PCM_SIGNED</att>\n" +
"        <att name=\"audioFrameRate\" type=\"float\">96000.0</att>\n" +
"        <att name=\"audioFrameSize\" type=\"int\">2</att>\n" +
"        <att name=\"audioSampleRate\" type=\"float\">96000.0</att>\n" +
"        <att name=\"audioSampleSizeInBits\" type=\"int\">16</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"keywords\">channel, channel_1, data, elapsed, elapsedTime, local, source, time</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"subsetVariables\">time</att>\n" +
"        <att name=\"summary\">Audio data from a local source.</att>\n" +
"        <att name=\"title\">Audio data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"units\">yyyyMMdd&#39;_&#39;HHmmss</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>elapsedTime</sourceName>\n" +
"        <destinationName>elapsedTime</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Elapsed Time</att>\n" +
"            <att name=\"units\">seconds</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>channel_1</sourceName>\n" +
"        <destinationName>channel_1</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Channel 1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">33000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-33000.0</att>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
        //    expected, "");

        String tDatasetID = "wav_56d5_230d_1887";
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), "wav_56d5_230d_1887", "");
        Test.ensureEqual(edd.title(), "Audio data from a local source.", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "time, elapsedTime, channel_1", 
            "");

        EDD.debugMode = oEDDDebugMode;

    }

    /**
     * This does basic tests of this class.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromAudioFiles.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String dir = EDStatic.fullTestCacheDirectory;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 12); //12 is enough to check date

        String id = "testTableWav"; //straight from generateDatasetsXml
        if (deleteCachedDatasetInfo)
            deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromAudioFiles  test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.4163561e+9, 1.4163564e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  elapsedTime {\n" +
"    Float64 actual_range 0.0, 299.99998958333333;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Elapsed Time\";\n" +
"    String units \"seconds\";\n" +
"  }\n" +
"  channel_1 {\n" +
"    Int16 actual_range -32768, 24572;\n" +
"    Float64 colorBarMaximum 33000.0;\n" +
"    Float64 colorBarMinimum -33000.0;\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Channel 1\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String audioBigEndian \"false\";\n" +
"    Int32 audioChannels 1;\n" +
"    String audioEncoding \"PCM_SIGNED\";\n" +
"    Float32 audioFrameRate 96000.0;\n" +
"    Int32 audioFrameSize 2;\n" +
"    Float32 audioSampleRate 96000.0;\n" +
"    Int32 audioSampleSizeInBits 16;\n" +
"    String cdm_data_type \"Other\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String defaultDataQuery \"&time=min(time)\";\n" +
"    String defaultGraphQuery \"elapsedTime,channel_1&time=min(time)&elapsedTime>=0&elapsedTime<=1&.draw=lines\";\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected =
"http://localhost:8080/cwexperimental/tabledap/testTableWav.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"channel, channel_1, data, elapsed, elapsedTime, local, source, time\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"time\";\n" +
"    String summary \"Audio data from a local source.\";\n" +
"    String time_coverage_end \"2014-11-19T00:20:00Z\";\n" +
"    String time_coverage_start \"2014-11-19T00:15:00Z\";\n" +
"    String title \"Audio data from a local source.\";\n" +
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
"    Float64 time;\n" +
"    Float64 elapsedTime;\n" +
"    Int16 channel_1;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromAudioFiles.test make DATA FILES\n");       

        //.csv   subset
        userDapQuery = "&time=2014-11-19T00:15:00Z&elapsedTime<=0.0001";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1time", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,elapsedTime,channel_1\n" +
"UTC,seconds,\n" +
"2014-11-19T00:15:00Z,0.0,-7217\n" +
"2014-11-19T00:15:00Z,1.0416666666666666E-5,-7255\n" +
"2014-11-19T00:15:00Z,2.0833333333333333E-5,-7462\n" +
"2014-11-19T00:15:00Z,3.125E-5,-7404\n" +
"2014-11-19T00:15:00Z,4.1666666666666665E-5,-7456\n" +
"2014-11-19T00:15:00Z,5.208333333333334E-5,-7529\n" +
"2014-11-19T00:15:00Z,6.25E-5,-7157\n" +
"2014-11-19T00:15:00Z,7.291666666666667E-5,-7351\n" +
"2014-11-19T00:15:00Z,8.333333333333333E-5,-7388\n" +
"2014-11-19T00:15:00Z,9.375E-5,-7458\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv   subset   with constraints in reverse order 
        userDapQuery = "&elapsedTime<=0.0001&time=2014-11-19T00:15:00Z";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_2time", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        //same expected
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.wav   subset
        userDapQuery = "channel_1&time=2014-11-19T00:15:00Z&elapsedTime<=15";
        tName = eddTable.className() + "test";
        File2.delete(dir + tName + ".wav");
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            tName, ".wav"); 
        Table table = new Table();
        table.readAudioFile(dir + tName, true, true); //readData, addElapsedTimeColumn
        results = table.toString(10);
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 1440001 ;\n" +
"variables:\n" +
"\tdouble elapsedTime(row) ;\n" +
"\t\telapsedTime:long_name = \"Elapsed Time\" ;\n" +
"\t\telapsedTime:units = \"seconds\" ;\n" +
"\tshort channel_1(row) ;\n" +
"\t\tchannel_1:long_name = \"Channel 1\" ;\n" +
"\n" +
"// global attributes:\n" +
"\t\t:audioBigEndian = \"false\" ;\n" +
"\t\t:audioChannels = 1 ;\n" +
"\t\t:audioEncoding = \"PCM_SIGNED\" ;\n" +
"\t\t:audioFrameRate = 96000.0f ;\n" +
"\t\t:audioFrameSize = 2 ;\n" +
"\t\t:audioSampleRate = 96000.0f ;\n" +
"\t\t:audioSampleSizeInBits = 16 ;\n" +
"}\n" +
"elapsedTime,channel_1\n" +
"0.0,-7217\n" +
"1.0416666666666666E-5,-7255\n" +
"2.0833333333333333E-5,-7462\n" +
"3.125E-5,-7404\n" +
"4.1666666666666665E-5,-7456\n" +
"5.208333333333334E-5,-7529\n" +
"6.25E-5,-7157\n" +
"7.291666666666667E-5,-7351\n" +
"8.333333333333333E-5,-7388\n" +
"9.375E-5,-7458\n" +
"...\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        SSR.displayInBrowser("file://" + dir + tName);        
        String2.pressEnterToContinue("Close audio player when done.");
        File2.delete(dir + tName);
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
            lastTest = interactive? 1 : 0;
        String msg = "\n^^^ EDDTableFromAudioFiles.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    if (test ==  0) testBasic(true); //deleteCachedDatasetInfo
                    if (test ==  1) testBasic(false); //deleteCachedDatasetInfo

                } else {
                    if (test ==  0) testGenerateDatasetsXml();
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

