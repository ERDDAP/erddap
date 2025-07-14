package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagSlowTests;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromAudioFilesTests {

  static boolean initialCroissantSetting = false;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
    initialCroissantSetting = EDStatic.config.generateCroissantSchema;
  }

  @AfterEach
  void cleanup() {
    EDStatic.config.generateCroissantSchema = initialCroissantSetting;
  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    int language = EDMessages.DEFAULT_LANGUAGE;
    // testVerboseOn();
    boolean oEDDDebugMode = EDD.debugMode;
    // EDD.debugMode = true;

    String dataDir =
        File2.addSlash(
            Path.of(EDDTableFromAudioFilesTests.class.getResource("/largeFiles/audio/wav/").toURI())
                .toString());
    String fileNameRegex = ".*\\.wav";
    String results =
        EDDTableFromAudioFiles.generateDatasetsXml(
                dataDir, // test no trailing /
                fileNameRegex,
                "",
                1440,
                "aco_acoustic\\.",
                "\\.wav",
                ".*",
                "time",
                "yyyyMMdd'_'HHmmss",
                "",
                "",
                "",
                "",
                "",
                -1,
                null, // defaultStandardizeWhat
                null)
            + "\n";

    String2.log(results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromAudioFiles",
                  dataDir,
                  fileNameRegex,
                  "",
                  "1440",
                  "aco_acoustic\\.",
                  "\\.wav",
                  ".*",
                  "time",
                  "yyyyMMdd'_'HHmmss",
                  "",
                  "",
                  "",
                  "",
                  "",
                  "-1",
                  ""
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");
    String suggDatasetID = EDDTableFromAudioFiles.suggestDatasetID(dataDir + fileNameRegex);
    String expected =
        "<dataset type=\"EDDTableFromAudioFiles\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <defaultGraphQuery>elapsedTime,channel_1&amp;time=min(time)&amp;elapsedTime&gt;=0&amp;elapsedTime&lt;=1&amp;.draw=lines</defaultGraphQuery>\n"
            + "    <defaultDataQuery>&amp;time=min(time)</defaultDataQuery>\n"
            + "    <fileDir>"
            + dataDir
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + fileNameRegex
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <preExtractRegex>aco_acoustic\\.</preExtractRegex>\n"
            + "    <postExtractRegex>\\.wav</postExtractRegex>\n"
            + "    <extractRegex>.*</extractRegex>\n"
            + "    <columnNameForExtract>time</columnNameForExtract>\n"
            + "    <sortedColumnSourceName>elapsedTime</sortedColumnSourceName>\n"
            + "    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"audioBigEndian\">false</att>\n"
            + "        <att name=\"audioChannels\" type=\"int\">1</att>\n"
            + "        <att name=\"audioEncoding\">PCM_SIGNED</att>\n"
            + "        <att name=\"audioFrameRate\" type=\"float\">96000.0</att>\n"
            + "        <att name=\"audioFrameSize\" type=\"int\">2</att>\n"
            + "        <att name=\"audioSampleRate\" type=\"float\">96000.0</att>\n"
            + "        <att name=\"audioSampleSizeInBits\" type=\"int\">16</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"infoUrl\">???</att>\n"
            + "        <att name=\"institution\">???</att>\n"
            + "        <att name=\"keywords\">channel, channel_1, data, elapsed, elapsedTime, local, source, time</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">time</att>\n"
            + "        <att name=\"summary\">Audio data from a local source.</att>\n"
            + "        <att name=\"title\">Audio data from a local source.</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"units\">yyyyMMdd&#39;_&#39;HHmmss</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>elapsedTime</sourceName>\n"
            + "        <destinationName>elapsedTime</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Elapsed Time</att>\n"
            + "            <att name=\"units\">seconds</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>channel_1</sourceName>\n"
            + "        <destinationName>channel_1</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Channel 1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">33000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-33000.0</att>\n"
            + "            <att name=\"ioos_category\">Other</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

    EDD.deleteCachedDatasetInfo(suggDatasetID);
    EDD edd = EDDTableFromAudioFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), suggDatasetID, "");
    Test.ensureEqual(edd.title(language), "Audio data from a local source.", "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "time, elapsedTime, channel_1",
        "");

    EDD.debugMode = oEDDDebugMode;
  }

  /**
   * This does basic tests of this class.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagSlowTests
  void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
    // String2.log("\n****************** EDDTableFromAudioFiles.testBasic()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String tName, results, tResults, expected, userDapQuery;
    String dir = EDStatic.config.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu().substring(0, 12); // 12 is enough to check date

    String id = "testTableWav"; // straight from generateDatasetsXml
    if (deleteCachedDatasetInfo) EDDTableFromAudioFiles.deleteCachedDatasetInfo(id);

    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestTableWav();

    // *** test getting das for entire dataset
    String2.log(
        "\n****************** EDDTableFromAudioFiles  test das and dds for entire dataset\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.4163561e+9, 1.4163564e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  elapsedTime {\n"
            + "    Float64 actual_range 0.0, 299.99998958333333;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Elapsed Time\";\n"
            + "    String units \"seconds\";\n"
            + "  }\n"
            + "  channel_1 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    Int16 actual_range -32768, 24572;\n"
            + "    Float64 colorBarMaximum 33000.0;\n"
            + "    Float64 colorBarMinimum -33000.0;\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Channel 1\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String audioBigEndian \"false\";\n"
            + "    Int32 audioChannels 1;\n"
            + "    String audioEncoding \"PCM_SIGNED\";\n"
            + "    Float32 audioFrameRate 96000.0;\n"
            + "    Int32 audioFrameSize 2;\n"
            + "    Float32 audioSampleRate 96000.0;\n"
            + "    Int32 audioSampleSizeInBits 16;\n"
            + "    String cdm_data_type \"Other\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String defaultDataQuery \"&time=min(time)\";\n"
            + "    String defaultGraphQuery \"elapsedTime,channel_1&time=min(time)&elapsedTime>=0&elapsedTime<=1&.draw=lines\";\n"
            + "    String history \""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        "/erddap/tabledap/testTableWav.das\";\n"
            + "    String infoUrl \"???\";\n"
            + "    String institution \"???\";\n"
            + "    String keywords \"channel, channel_1, data, elapsed, elapsedTime, local, source, time\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"time\";\n"
            + "    String summary \"Audio data from a local source.\";\n"
            + "    String time_coverage_end \"2014-11-19T00:20:00Z\";\n"
            + "    String time_coverage_start \"2014-11-19T00:15:00Z\";\n"
            + "    String title \"Audio data from a local source.\";\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float64 time;\n"
            + "    Float64 elapsedTime;\n"
            + "    Int16 channel_1;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n****************** EDDTableFromAudioFiles.test make DATA FILES\n");

    // .csv subset
    userDapQuery = "&time=2014-11-19T00:15:00Z&elapsedTime<=0.0001";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1time", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,elapsedTime,channel_1\n"
            + "UTC,seconds,\n"
            + "2014-11-19T00:15:00Z,0.0,-7217\n"
            + "2014-11-19T00:15:00Z,1.0416666666666666E-5,-7255\n"
            + "2014-11-19T00:15:00Z,2.0833333333333333E-5,-7462\n"
            + "2014-11-19T00:15:00Z,3.125E-5,-7404\n"
            + "2014-11-19T00:15:00Z,4.1666666666666665E-5,-7456\n"
            + "2014-11-19T00:15:00Z,5.208333333333334E-5,-7529\n"
            + "2014-11-19T00:15:00Z,6.25E-5,-7157\n"
            + "2014-11-19T00:15:00Z,7.291666666666667E-5,-7351\n"
            + "2014-11-19T00:15:00Z,8.333333333333333E-5,-7388\n"
            + "2014-11-19T00:15:00Z,9.375E-5,-7458\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv subset with constraints in reverse order
    userDapQuery = "&elapsedTime<=0.0001&time=2014-11-19T00:15:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_2time", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    // same expected
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .wav subset
    userDapQuery = "channel_1&time=2014-11-19T00:15:00Z&elapsedTime<=15";
    tName = eddTable.className() + "test";
    File2.delete(dir + tName + ".wav");
    tName = eddTable.makeNewFileForDapQuery(language, null, null, userDapQuery, dir, tName, ".wav");
    Table table = new Table();
    table.readAudioFile(dir + tName, true, true); // readData, addElapsedTimeColumn
    results = table.toString(10);
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 1440001 ;\n"
            + "variables:\n"
            + "\tdouble elapsedTime(row) ;\n"
            + "\t\telapsedTime:long_name = \"Elapsed Time\" ;\n"
            + "\t\telapsedTime:units = \"seconds\" ;\n"
            + "\tshort channel_1(row) ;\n"
            + "\t\tchannel_1:long_name = \"Channel 1\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:audioBigEndian = \"false\" ;\n"
            + "\t\t:audioChannels = 1 ;\n"
            + "\t\t:audioEncoding = \"PCM_SIGNED\" ;\n"
            + "\t\t:audioFrameRate = 96000.0f ;\n"
            + "\t\t:audioFrameSize = 2 ;\n"
            + "\t\t:audioSampleRate = 96000.0f ;\n"
            + "\t\t:audioSampleSizeInBits = 16 ;\n"
            + "}\n"
            + "elapsedTime,channel_1\n"
            + "0.0,-7217\n"
            + "1.0416666666666666E-5,-7255\n"
            + "2.0833333333333333E-5,-7462\n"
            + "3.125E-5,-7404\n"
            + "4.1666666666666665E-5,-7456\n"
            + "5.208333333333334E-5,-7529\n"
            + "6.25E-5,-7157\n"
            + "7.291666666666667E-5,-7351\n"
            + "8.333333333333333E-5,-7388\n"
            + "9.375E-5,-7458\n"
            + "...\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    // TestUtil.displayInBrowser("file://" + dir + tName);
    // String2.pressEnterToContinue("Close audio player when done.");
    File2.delete(dir + tName);
  }

  /**
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testSchema(boolean generateCroissantSchema) throws Throwable {
    EDStatic.config.generateCroissantSchema = generateCroissantSchema;
    int language = 0;
    String tName, results, tResults, expected;

    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestTableWav();
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.config.fullTestCacheDirectory,
            eddTable.className(),
            ".croissant");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    expected =
        EDStatic.config.generateCroissantSchema
            ? "{\n"
                + "  \"@context\":  {\n"
                + "    \"@language\": \"en\",\n"
                + "    \"@vocab\": \"https://schema.org/\",\n"
                + "    \"sc\": \"https://schema.org/\",\n"
                + "    \"cr\": \"http://mlcommons.org/croissant/\",\n"
                + "    \"rai\": \"http://mlcommons.org/croissant/RAI/\",\n"
                + "    \"dct\": \"http://purl.org/dc/terms/\",\n"
                + "    \"citeAs\": \"cr:citeAs\",\n"
                + "    \"column\": \"cr:column\",\n"
                + "    \"conformsTo\": \"dct:conformsTo\",\n"
                + "    \"data\": {\n"
                + "      \"@id\": \"cr:data\",\n"
                + "      \"@type\": \"@json\"\n"
                + "    },\n"
                + "    \"dataType\": {\n"
                + "      \"@id\": \"cr:dataType\",\n"
                + "      \"@type\": \"@vocab\"\n"
                + "    },\n"
                + "    \"examples\": {\n"
                + "      \"@id\": \"cr:examples\",\n"
                + "      \"@type\": \"@json\"\n"
                + "    },\n"
                + "    \"extract\": \"cr:extract\",\n"
                + "    \"field\": \"cr:field\",\n"
                + "    \"fileProperty\": \"cr:fileProperty\",\n"
                + "    \"fileObject\": \"cr:fileObject\",\n"
                + "    \"fileSet\": \"cr:fileSet\",\n"
                + "    \"format\": \"cr:format\",\n"
                + "    \"includes\": \"cr:includes\",\n"
                + "    \"isLiveDataset\": \"cr:isLiveDataset\",\n"
                + "    \"jsonPath\": \"cr:jsonPath\",\n"
                + "    \"key\": \"cr:key\",\n"
                + "    \"md5\": \"cr:md5\",\n"
                + "    \"parentField\": \"cr:parentField\",\n"
                + "    \"path\": \"cr:path\",\n"
                + "    \"recordSet\": \"cr:recordSet\",\n"
                + "    \"references\": \"cr:references\",\n"
                + "    \"regex\": \"cr:regex\",\n"
                + "    \"repeated\": \"cr:repeated\",\n"
                + "    \"replace\": \"cr:replace\",\n"
                + "    \"separator\": \"cr:separator\",\n"
                + "    \"source\": \"cr:source\",\n"
                + "    \"subField\": \"cr:subField\",\n"
                + "    \"transform\": \"cr:transform\"  },\n"
                + "  \"@type\": \"sc:Dataset\",\n"
                + "  \"conformsTo\": \"http://mlcommons.org/croissant/1.0\",\n"
                + "  \"name\": \"Audio data from a local source.\",\n"
                + "  \"headline\": \"testTableWav\",\n"
                + "  \"isLiveDataset\": true,\n"
                + "  \"distribution\": [\n"
                + "  {\n"
                + "    \"@type\": \"cr:FileObject\",\n"
                + "    \"@id\": \"aco_acoustic.20141119_001500.wav\",\n"
                + "    \"contentSize\": \"57600044 B\",\n"
                + "    \"contentUrl\": \"http://localhost:8080/erddap/files/testTableWav/aco_acoustic.20141119_001500.wav\",\n"
                + "    \"encodingFormat\": \"audio/wav\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"@type\": \"cr:FileObject\",\n"
                + "    \"@id\": \"aco_acoustic.20141119_002000.wav\",\n"
                + "    \"contentSize\": \"57600044 B\",\n"
                + "    \"contentUrl\": \"http://localhost:8080/erddap/files/testTableWav/aco_acoustic.20141119_002000.wav\",\n"
                + "    \"encodingFormat\": \"audio/wav\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"@type\": \"cr:FileSet\",\n"
                + "    \"@id\": \"testTableWavFiles\",\n"
                + "    \"description\": \"Files that contain the data.\",\n"
                + "    \"encodingFormat\": \"application/json\",\n"
                + "    \"includes\": \"http://localhost:8080/erddap/files/testTableWav/*.*\"\n"
                + "  }\n"
                + "  ],\n"
                + "  \"recordSet\": [\n"
                + "    {\n"
                + "      \"@type\": \"cr:RecordSet\",\n"
                + "      \"@id\": \"dataRecordSet\",\n"
                + "      \"field\": [\n"
                + "        {\n"
                + "          \"@type\": \"cr:Field\",\n"
                + "          \"@id\": \"dataRecordSet/time\",\n"
                + "          \"description\": \"Time\",\n"
                + "          \"dataType\": \"cr:Float64\",\n"
                + "          \"source\": {\n"
                + "            \"fileSet\": {\n"
                + "              \"@id\": \"testTableWavFiles\"\n"
                + "            },\n"
                + "            \"extract\": {\n"
                + "              \"column\": \"time\"\n"
                + "            }\n"
                + "          }\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"cr:Field\",\n"
                + "          \"@id\": \"dataRecordSet/elapsedTime\",\n"
                + "          \"description\": \"Elapsed Time\",\n"
                + "          \"dataType\": \"cr:Float64\",\n"
                + "          \"source\": {\n"
                + "            \"fileSet\": {\n"
                + "              \"@id\": \"testTableWavFiles\"\n"
                + "            },\n"
                + "            \"extract\": {\n"
                + "              \"column\": \"elapsedTime\"\n"
                + "            }\n"
                + "          }\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"cr:Field\",\n"
                + "          \"@id\": \"dataRecordSet/channel_1\",\n"
                + "          \"description\": \"Channel 1\",\n"
                + "          \"dataType\": \"cr:Int16\",\n"
                + "          \"source\": {\n"
                + "            \"fileSet\": {\n"
                + "              \"@id\": \"testTableWavFiles\"\n"
                + "            },\n"
                + "            \"extract\": {\n"
                + "              \"column\": \"channel_1\"\n"
                + "            }\n"
                + "          }\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ],\n"
                + "  \"description\": \"Audio data from a local source.\\n"
                + "audioBigEndian=false\\n"
                + "audioChannels=1\\n"
                + "audioEncoding=PCM_SIGNED\\n"
                + "audioFrameRate=96000.0\\n"
                + "audioFrameSize=2\\n"
                + "audioSampleRate=96000.0\\n"
                + "audioSampleSizeInBits=16\\n"
                + "cdm_data_type=Other\\n"
                + "Conventions=COARDS, CF-1.6, ACDD-1.3\\n"
                + "defaultDataQuery=&time=min(time)\\n"
                + "defaultGraphQuery=elapsedTime,channel_1&time=min(time)&elapsedTime>=0&elapsedTime<=1&.draw=lines\\n"
                + "infoUrl=???\\n"
                + "institution=???\\n"
                + "sourceUrl=(local files)\\n"
                + "standard_name_vocabulary=CF Standard Name Table v70\\n"
                + "subsetVariables=time\\n"
                + "time_coverage_end=2014-11-19T00:20:00Z\\n"
                + "time_coverage_start=2014-11-19T00:15:00Z\",\n"
                + "  \"url\": \"http://localhost:8080/erddap/tabledap/testTableWav.html\",\n"
                + "  \"includedInDataCatalog\": {\n"
                + "    \"@type\": \"DataCatalog\",\n"
                + "    \"name\": \"ERDDAP Data Server at ERDDAP Jetty Install\",\n"
                + "    \"sameAs\": \"http://localhost:8080/erddap\"\n"
                + "  },\n"
                + "  \"keywords\": [\n"
                + "    \"channel\",\n"
                + "    \"channel_1\",\n"
                + "    \"data\",\n"
                + "    \"elapsed\",\n"
                + "    \"elapsedTime\",\n"
                + "    \"local\",\n"
                + "    \"source\",\n"
                + "    \"time\"\n"
                + "  ],\n"
                + "  \"license\": \"The data may be used and redistributed for free but is not intended\\n"
                + "for legal use, since it may contain inaccuracies. Neither the data\\n"
                + "Contributor, ERD, NOAA, nor the United States Government, nor any\\n"
                + "of their employees or contractors, makes any warranty, express or\\n"
                + "implied, including warranties of merchantability and fitness for a\\n"
                + "particular purpose, or assumes any legal liability for the accuracy,\\n"
                + "completeness, or usefulness, of this information.\",\n"
                + "  \"variableMeasured\": [\n"
                + "    {\n"
                + "      \"@type\": \"PropertyValue\",\n"
                + "      \"name\": \"time\",\n"
                + "      \"alternateName\": \"Time\",\n"
                + "      \"description\": \"Time\",\n"
                + "      \"valueReference\": [\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"axisOrDataVariable\",\n"
                + "          \"value\": \"data\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"_CoordinateAxisType\",\n"
                + "          \"value\": \"Time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"axis\",\n"
                + "          \"value\": \"T\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"ioos_category\",\n"
                + "          \"value\": \"Time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"long_name\",\n"
                + "          \"value\": \"Time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"standard_name\",\n"
                + "          \"value\": \"time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"time_origin\",\n"
                + "          \"value\": \"01-JAN-1970 00:00:00\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"maxValue\": \"2014-11-19T00:20:00Z\",\n"
                + "      \"minValue\": \"2014-11-19T00:15:00Z\",\n"
                + "      \"propertyID\": \"time\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"@type\": \"PropertyValue\",\n"
                + "      \"name\": \"elapsedTime\",\n"
                + "      \"alternateName\": \"Elapsed Time\",\n"
                + "      \"description\": \"Elapsed Time\",\n"
                + "      \"valueReference\": [\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"axisOrDataVariable\",\n"
                + "          \"value\": \"data\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"ioos_category\",\n"
                + "          \"value\": \"Time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"long_name\",\n"
                + "          \"value\": \"Elapsed Time\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"maxValue\": 299.99998958333333,\n"
                + "      \"minValue\": 0,\n"
                + "      \"unitText\": \"seconds\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"@type\": \"PropertyValue\",\n"
                + "      \"name\": \"channel_1\",\n"
                + "      \"alternateName\": \"Channel 1\",\n"
                + "      \"description\": \"Channel 1\",\n"
                + "      \"valueReference\": [\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"axisOrDataVariable\",\n"
                + "          \"value\": \"data\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"_FillValue\",\n"
                + "          \"value\": 32767\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"colorBarMaximum\",\n"
                + "          \"value\": 33000\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"colorBarMinimum\",\n"
                + "          \"value\": -33000\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"ioos_category\",\n"
                + "          \"value\": \"Other\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"long_name\",\n"
                + "          \"value\": \"Channel 1\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"maxValue\": 24572,\n"
                + "      \"minValue\": -32768\n"
                + "    }\n"
                + "  ],\n"
                + "  \"identifier\": \"testTableWav\",\n"
                + "  \"temporalCoverage\": \"2014-11-19T00:15:00Z/2014-11-19T00:20:00Z\"\n"
                + "}\n"
            : "{\n"
                + "  \"@context\": \"http://schema.org\",\n"
                + "  \"@type\": \"Dataset\",\n"
                + "  \"name\": \"Audio data from a local source.\",\n"
                + "  \"headline\": \"testTableWav\",\n"
                + "  \"description\": \"Audio data from a local source.\\n"
                + "audioBigEndian=false\\n"
                + "audioChannels=1\\n"
                + "audioEncoding=PCM_SIGNED\\n"
                + "audioFrameRate=96000.0\\n"
                + "audioFrameSize=2\\n"
                + "audioSampleRate=96000.0\\n"
                + "audioSampleSizeInBits=16\\n"
                + "cdm_data_type=Other\\n"
                + "Conventions=COARDS, CF-1.6, ACDD-1.3\\n"
                + "defaultDataQuery=&time=min(time)\\n"
                + "defaultGraphQuery=elapsedTime,channel_1&time=min(time)&elapsedTime>=0&elapsedTime<=1&.draw=lines\\n"
                + "infoUrl=???\\n"
                + "institution=???\\n"
                + "sourceUrl=(local files)\\n"
                + "standard_name_vocabulary=CF Standard Name Table v70\\n"
                + "subsetVariables=time\\n"
                + "time_coverage_end=2014-11-19T00:20:00Z\\n"
                + "time_coverage_start=2014-11-19T00:15:00Z\",\n"
                + "  \"url\": \"http://localhost:8080/erddap/tabledap/testTableWav.html\",\n"
                + "  \"includedInDataCatalog\": {\n"
                + "    \"@type\": \"DataCatalog\",\n"
                + "    \"name\": \"ERDDAP Data Server at ERDDAP Jetty Install\",\n"
                + "    \"sameAs\": \"http://localhost:8080/erddap\"\n"
                + "  },\n"
                + "  \"keywords\": [\n"
                + "    \"channel\",\n"
                + "    \"channel_1\",\n"
                + "    \"data\",\n"
                + "    \"elapsed\",\n"
                + "    \"elapsedTime\",\n"
                + "    \"local\",\n"
                + "    \"source\",\n"
                + "    \"time\"\n"
                + "  ],\n"
                + "  \"license\": \"The data may be used and redistributed for free but is not intended\\n"
                + "for legal use, since it may contain inaccuracies. Neither the data\\n"
                + "Contributor, ERD, NOAA, nor the United States Government, nor any\\n"
                + "of their employees or contractors, makes any warranty, express or\\n"
                + "implied, including warranties of merchantability and fitness for a\\n"
                + "particular purpose, or assumes any legal liability for the accuracy,\\n"
                + "completeness, or usefulness, of this information.\",\n"
                + "  \"variableMeasured\": [\n"
                + "    {\n"
                + "      \"@type\": \"PropertyValue\",\n"
                + "      \"name\": \"time\",\n"
                + "      \"alternateName\": \"Time\",\n"
                + "      \"description\": \"Time\",\n"
                + "      \"valueReference\": [\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"axisOrDataVariable\",\n"
                + "          \"value\": \"data\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"_CoordinateAxisType\",\n"
                + "          \"value\": \"Time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"axis\",\n"
                + "          \"value\": \"T\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"ioos_category\",\n"
                + "          \"value\": \"Time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"long_name\",\n"
                + "          \"value\": \"Time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"standard_name\",\n"
                + "          \"value\": \"time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"time_origin\",\n"
                + "          \"value\": \"01-JAN-1970 00:00:00\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"maxValue\": \"2014-11-19T00:20:00Z\",\n"
                + "      \"minValue\": \"2014-11-19T00:15:00Z\",\n"
                + "      \"propertyID\": \"time\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"@type\": \"PropertyValue\",\n"
                + "      \"name\": \"elapsedTime\",\n"
                + "      \"alternateName\": \"Elapsed Time\",\n"
                + "      \"description\": \"Elapsed Time\",\n"
                + "      \"valueReference\": [\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"axisOrDataVariable\",\n"
                + "          \"value\": \"data\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"ioos_category\",\n"
                + "          \"value\": \"Time\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"long_name\",\n"
                + "          \"value\": \"Elapsed Time\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"maxValue\": 299.99998958333333,\n"
                + "      \"minValue\": 0,\n"
                + "      \"unitText\": \"seconds\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"@type\": \"PropertyValue\",\n"
                + "      \"name\": \"channel_1\",\n"
                + "      \"alternateName\": \"Channel 1\",\n"
                + "      \"description\": \"Channel 1\",\n"
                + "      \"valueReference\": [\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"axisOrDataVariable\",\n"
                + "          \"value\": \"data\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"_FillValue\",\n"
                + "          \"value\": 32767\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"colorBarMaximum\",\n"
                + "          \"value\": 33000\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"colorBarMinimum\",\n"
                + "          \"value\": -33000\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"ioos_category\",\n"
                + "          \"value\": \"Other\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"@type\": \"PropertyValue\",\n"
                + "          \"name\": \"long_name\",\n"
                + "          \"value\": \"Channel 1\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"maxValue\": 24572,\n"
                + "      \"minValue\": -32768\n"
                + "    }\n"
                + "  ],\n"
                + "  \"identifier\": \"testTableWav\",\n"
                + "  \"temporalCoverage\": \"2014-11-19T00:15:00Z/2014-11-19T00:20:00Z\"\n"
                + "}\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "results=\n" + results);
  }
}
