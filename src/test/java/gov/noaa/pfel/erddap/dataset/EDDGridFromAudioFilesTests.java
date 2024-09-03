package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagIncompleteTest;
import tags.TagLocalERDDAP;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridFromAudioFilesTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests generateDatasetsXml. The test files are from Jim Potemra
   * https://pae-paha.pacioos.hawaii.edu/erddap/files/aco_acoustic/ Note that the first 3 files (for
   * :00, :05, :10 minutes) have float data, seem to be have almost all ~0, whereas the :15 and :20
   * files have audible short data. I'm using the first 3 files here.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {

    // String2.log("\n*** EDDGridFromAudioFiles.testGenerateDatasetsXml");

    String dir =
        Path.of(EDDGridFromAudioFilesTests.class.getResource("/largeFiles/audio/floatWav/").toURI())
            .toString();
    String regex = "aco_acoustic\\.[0-9]{8}_[0-9]{6}\\.wav";
    String extractFileNameRegex = "aco_acoustic\\.([0-9]{8}_[0-9]{6})\\.wav";
    String extractDataType = "timeFormat=yyyyMMdd'_'HHmmss";
    String extractColumnName = "time";

    String results =
        EDDGridFromAudioFiles.generateDatasetsXml(
                dir,
                regex,
                "",
                extractFileNameRegex,
                extractDataType,
                extractColumnName,
                -1,
                null,
                null)
            + "\n";

    String suggDatasetID =
        EDDGridFromAudioFiles.suggestDatasetID(File2.addSlash(dir) + regex.trim());
    String expected =
        "<dataset type=\"EDDGridFromAudioFiles\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + "    <defaultGraphQuery>channel_1[0][(0):(1)]&amp;.draw=lines&amp;.vars=elapsedTime|time</defaultGraphQuery>\n"
            + "    <defaultDataQuery>&amp;time=min(time)</defaultDataQuery>\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + File2.addSlash(dir)
            + "</fileDir>\n"
            + "    <fileNameRegex>aco_acoustic\\.[0-9]{8}_[0-9]{6}\\.wav</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <matchAxisNDigits>20</matchAxisNDigits>\n"
            + "    <dimensionValuesInMemory>false</dimensionValuesInMemory>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"audioBigEndian\">false</att>\n"
            + "        <att name=\"audioChannels\" type=\"int\">1</att>\n"
            + "        <att name=\"audioEncoding\">PCM_FLOAT</att>\n"
            + "        <att name=\"audioFrameRate\" type=\"float\">24000.0</att>\n"
            + "        <att name=\"audioFrameSize\" type=\"int\">4</att>\n"
            + "        <att name=\"audioSampleRate\" type=\"float\">24000.0</att>\n"
            + "        <att name=\"audioSampleSizeInBits\" type=\"int\">32</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"infoUrl\">???</att>\n"
            + "        <att name=\"institution\">???</att>\n"
            + "        <att name=\"keywords\">channel, channel_1, data, elapsedtime, local, source, time</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">Audio data from a local source.</att>\n"
            + "        <att name=\"title\">Audio data from a local source.</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>***fileName,\"timeFormat=yyyyMMdd'_'HHmmss\",\"aco_acoustic\\\\.([0-9]{8}_[0-9]{6})\\\\.wav\",1</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>elapsedTime</sourceName>\n"
            + "        <destinationName>elapsedTime</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Elapsed Time</att>\n"
            + "            <att name=\"units\">seconds</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>channel_1</sourceName>\n"
            + "        <destinationName>channel_1</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Channel 1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Other</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(
        results,
        expected,
        "results.length="
            + results.length()
            + " expected.length="
            + expected.length()
            + "\nresults=\n"
            + results);

    // GenerateDatasetsXml
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDGridFromAudioFiles",
                  dir,
                  regex,
                  "",
                  extractFileNameRegex,
                  extractDataType,
                  extractColumnName,
                  "-1",
                  ""
                }, // default reloadEvery, cacheFromUrl
                false); // doIt loop?

    Test.ensureEqual(
        results,
        expected,
        "Unexpected results from GenerateDatasetsXml.doIt. "
            + "results.length="
            + results.length()
            + " expected.length="
            + expected.length());

    // ensure it is ready-to-use by making a dataset from it
    EDDGridFromAudioFiles.deleteCachedDatasetInfo("_0_9_8_0_9_6_7538_7112_995c");
    EDDGrid eddg = (EDDGrid) EDDGridFromAudioFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(eddg.className(), "EDDGridFromAudioFiles", "className");
    Test.ensureEqual(eddg.title(), "Audio data from a local source.", "title");
    Test.ensureEqual(
        String2.toCSSVString(eddg.axisVariableDestinationNames()),
        "time, elapsedTime",
        "axisVariableDestinationNames");
    Test.ensureEqual(
        String2.toCSSVString(eddg.dataVariableDestinationNames()),
        "channel_1",
        "dataVariableDestinationNames");

    String2.log("\nEDDGridFromAudioFiles.testGenerateDatasetsXml passed the test.");
  }

  /**
   * This tests generateDatasetsXml. The test files are from Jim Potemra
   * https://pae-paha.pacioos.hawaii.edu/erddap/files/aco_acoustic/ Note that the first 3 files (for
   * :00, :05, :10 minutes) have float data, seem to be have almost all ~0, whereas the :15 and :20
   * files have audible short data. I'm using the latter 2 files here.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml2() throws Throwable {

    // String2.log("\n*** EDDGridFromAudioFiles.testGenerateDatasetsXml2");

    // test files are from Jim Potemra
    // https://pae-paha.pacioos.hawaii.edu/erddap/files/aco_acoustic/
    // Note that the first 3 files (for :00, :05, :10 minutes) have float data,
    // seem to be have almost all ~0,
    // whereas the :15 and :20 files have audible short data.
    // I'm using the latter 2 files here.

    String dir =
        Path.of(EDDGridFromAudioFilesTests.class.getResource("/largeFiles/audio/wav/").toURI())
            .toString();
    String regex = "aco_acoustic\\.[0-9]{8}_[0-9]{6}\\.wav";
    String extractFileNameRegex = "aco_acoustic\\.([0-9]{8}_[0-9]{6})\\.wav";
    String extractDataType = "timeFormat=yyyyMMdd'_'HHmmss";
    String extractColumnName = "time";

    String results =
        EDDGridFromAudioFiles.generateDatasetsXml(
                dir,
                regex,
                "",
                extractFileNameRegex,
                extractDataType,
                extractColumnName,
                -1,
                null,
                null)
            + "\n";

    String suggDatasetID =
        EDDGridFromAudioFiles.suggestDatasetID(File2.addSlash(dir) + regex.trim());
    String expected =
        "<dataset type=\"EDDGridFromAudioFiles\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + "    <defaultGraphQuery>channel_1[0][(0):(1)]&amp;.draw=lines&amp;.vars=elapsedTime|time</defaultGraphQuery>\n"
            + "    <defaultDataQuery>&amp;time=min(time)</defaultDataQuery>\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + File2.addSlash(dir)
            + "</fileDir>\n"
            + "    <fileNameRegex>aco_acoustic\\.[0-9]{8}_[0-9]{6}\\.wav</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <matchAxisNDigits>20</matchAxisNDigits>\n"
            + "    <dimensionValuesInMemory>false</dimensionValuesInMemory>\n"
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
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"infoUrl\">???</att>\n"
            + "        <att name=\"institution\">???</att>\n"
            + "        <att name=\"keywords\">channel, channel_1, data, elapsedtime, local, source, time</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">Audio data from a local source.</att>\n"
            + "        <att name=\"title\">Audio data from a local source.</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>***fileName,\"timeFormat=yyyyMMdd'_'HHmmss\",\"aco_acoustic\\\\.([0-9]{8}_[0-9]{6})\\\\.wav\",1</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>elapsedTime</sourceName>\n"
            + "        <destinationName>elapsedTime</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Elapsed Time</att>\n"
            + "            <att name=\"units\">seconds</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>channel_1</sourceName>\n"
            + "        <destinationName>channel_1</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Channel 1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">33000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-33000.0</att>\n"
            + "            <att name=\"ioos_category\">Other</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(
        results,
        expected,
        "results.length="
            + results.length()
            + " expected.length="
            + expected.length()
            + "\nresults=\n"
            + results);

    // GenerateDatasetsXml
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDGridFromAudioFiles",
                  dir,
                  regex,
                  "",
                  extractFileNameRegex,
                  extractDataType,
                  extractColumnName,
                  "-1",
                  ""
                }, // default reloadEvery, cacheFromUrl
                false); // doIt loop?

    Test.ensureEqual(
        results,
        expected,
        "Unexpected results from GenerateDatasetsXml.doIt. "
            + "results.length="
            + results.length()
            + " expected.length="
            + expected.length());

    // ensure it is ready-to-use by making a dataset from it
    EDDGridFromAudioFiles.deleteCachedDatasetInfo("_0_9_8_0_9_6_1225_8cb7_768d");
    EDDGrid eddg = (EDDGrid) EDDGridFromAudioFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(eddg.className(), "EDDGridFromAudioFiles", "className");
    Test.ensureEqual(eddg.title(), "Audio data from a local source.", "title");
    Test.ensureEqual(
        String2.toCSSVString(eddg.axisVariableDestinationNames()),
        "time, elapsedTime",
        "axisVariableDestinationNames");
    Test.ensureEqual(
        String2.toCSSVString(eddg.dataVariableDestinationNames()),
        "channel_1",
        "dataVariableDestinationNames");

    String2.log("\nEDDGridFromAudioFiles.testGenerateDatasetsXml2 passed the test.");
  }

  /** This tests this class. */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {

    // String2.log("\n*** EDDGridFromAudioFiles.testBasic(" +
    // deleteCachedDatasetInfo + ")\n");
    // testVerboseOn();
    // EDDGrid.debugMode = true;
    // EDV.debugMode = true; //to see dimensionValuesInMemory diagnostic messages
    // delete cached info
    if (deleteCachedDatasetInfo) EDDGridFromAudioFiles.deleteCachedDatasetInfo("testGridWav");
    EDDGrid edd = (EDDGrid) EDDTestDataset.gettestGridWav();
    String dir = EDStatic.fullTestCacheDirectory;
    String tName, results, expected, dapQuery;
    int po;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    int language = 0;

    // .dds
    tName =
        edd.makeNewFileForDapQuery(language, null, null, "", dir, edd.className() + "_", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 2];\n"
            + "  Float64 elapsedTime[elapsedTime = 28800000];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int16 channel_1[time = 2][elapsedTime = 28800000];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 2];\n"
            + "      Float64 elapsedTime[elapsedTime = 28800000];\n"
            + "  } channel_1;\n"
            + "} testGridWav;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** .das
    tName =
        edd.makeNewFileForDapQuery(language, null, null, "", dir, edd.className() + "_", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Attributes {\n"
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
            + "    Float64 colorBarMaximum 33000.0;\n"
            + "    Float64 colorBarMinimum -33000.0;\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Channel 1\";\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String audioBigEndian \"false\";\n"
            + "    Int32 audioChannels 1;\n"
            + "    String audioEncoding \"PCM_SIGNED\";\n"
            + "    Float32 audioFrameRate 96000.0;\n"
            + "    Int32 audioFrameSize 2;\n"
            + "    Float32 audioSampleRate 96000.0;\n"
            + "    Int32 audioSampleSizeInBits 16;\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String defaultDataQuery \"&time=min(time)\";\n"
            + "    String defaultGraphQuery \"channel_1[0][(0):(1)]&.draw=lines&.vars=elapsedTime|time\";\n"
            + "    String history \""
            + today;
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected =
        "/erddap/griddap/testGridWav.das\";\n"
            + "    String infoUrl \"???\";\n"
            + "    String institution \"???\";\n"
            + "    String keywords \"channel_1, data, elapsedTime, local, source, time\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"Audio data from a local source.\";\n"
            + "    String time_coverage_end \"2014-11-19T00:20:00Z\";\n"
            + "    String time_coverage_start \"2014-11-19T00:15:00Z\";\n"
            + "    String title \"Audio data from a local source.\";\n"
            + "  }\n"
            + "}\n";

    po = results.indexOf(expected.substring(0, 40));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

    // find some data
    /*
     * String dir2 = EDStatic.unitTestDataDir + "audio/wav/";
     * String name = "aco_acoustic.20141119_001500.wav";
     * Table table = new Table();
     * table.readAudioFile(dir2 + name, true, false); //getData,
     * addElapsedTimeColumn
     * int nRows = table.nRows();
     * PrimitiveArray pa = table.getColumn(0);
     * int count = 0;
     * for (int row = 0; row < nRows; row++) {
     * if (Math.abs(pa.getInt(row)) > 10000) {
     * String2.log("[" + row + "]=" + pa.getInt(row));
     * if (count++ > 100)
     * break;
     * }
     * }
     * table = null;
     * /*
     */

    // *** data
    dapQuery = "channel_1[1][344855:344864]";
    expected =
        "time,elapsedTime,channel_1\n"
            + "UTC,seconds,\n"
            + "2014-11-19T00:20:00Z,3.5922395833333334,-10026\n"
            + "2014-11-19T00:20:00Z,3.59225,-9807\n"
            + "2014-11-19T00:20:00Z,3.5922604166666665,-10025\n"
            + "2014-11-19T00:20:00Z,3.5922708333333335,-10092\n"
            + "2014-11-19T00:20:00Z,3.59228125,-10052\n"
            + "2014-11-19T00:20:00Z,3.5922916666666667,-9832\n"
            + "2014-11-19T00:20:00Z,3.5923020833333332,-9652\n"
            + "2014-11-19T00:20:00Z,3.5923125,-9758\n"
            + "2014-11-19T00:20:00Z,3.592322916666667,-9800\n"
            + "2014-11-19T00:20:00Z,3.5923333333333334,-9568\n";
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, edd.className() + "_", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** data
    dapQuery = "channel_1[0][0:(15)]"; // 15 seconds
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, edd.className() + "_", ".wav");
    Table table = new Table();
    table.readAudioFile(dir + tName, true, true);
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
    // Test.displayInBrowser("file://" + dir + tName);
    // String2.pressEnterToContinue("Close audio player when done.");
    File2.delete(dir + tName);

    String2.log(
        "\n*** EDDGridFromAudioFiles.testBasic("
            + deleteCachedDatasetInfo
            + ") finished successfully");
    EDDGrid.debugMode = false;
    EDV.debugMode = false;
  }

  /** This was used one time to fix some test files. */
  @TagIncompleteTest // wasn't used before migration, more of a script than a test
  void oneTimeFixFiles() throws Exception {
    Table table = new Table();
    String dir = "audio/wav/"; // probably needs to be a resource if we're doing anything with this.
    String name1 = "aco_acoustic.20141119_00";
    String name2 = "00.wav";

    for (int min = 0; min <= 10; min += 5) {
      String name = name1 + String2.zeroPad("" + min, 2) + name2;
      table.readAudioFile(dir + name, true, false); // getData, addElapsedTimeColumn
      String2.log(name + " nRows=" + table.nRows() + " time=" + (table.nRows() / 24000.0));
    }
  }

  /** This tests byte range requests to /files/ */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testByteRangeRequest() throws Throwable {

    // String2.log("\n*** EDDGridFromAudioFiles.testByteRangeRequest\n");
    // testVerboseOn();

    String results, results2, expected;
    ArrayList al;
    List list;
    int po;
    int timeOutSeconds = 120;
    String reqBase = "curl http://localhost:8080/cwexperimental/";
    String req =
        reqBase + "files/testGridWav/aco_acoustic.20141119_001500.wav -i "; // -i includes header in
    // response

    // * request no byte range
    al = SSR.dosShell(req, timeOutSeconds);
    list = al.subList(0, 8);
    results = String2.annotatedString(String2.toNewlineString(list.toArray()));
    expected =
        // "[10]\n" +
        // "c:\\programs\\_tomcat\\webapps\\cwexperimental\\WEB-INF>call
        // C:\\programs\\curl7600\\I386\\curl.exe
        // http://localhost:8080/cwexperimental/files/testGridWav/aco_acoustic.20141119_001500.wav
        // -i [10]\n" +
        "HTTP/1.1 200 [10]\n"
            + // 200=SC_OK
            "Content-Encoding: identity[10]\n"
            + "Accept-ranges: bytes[10]\n"
            + "Content-Type: audio/wav[10]\n"
            + "Content-Length: 57600044[10]\n"
            + "Date: "; // Wed, 27 Sep 2017 18:08:20 GMT[10]\n" +
    results2 = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(results2, expected, "results2=\n" + results2);

    // * request short byte range
    al = SSR.dosShell(req + "-H \"Range: bytes=0-30\"", timeOutSeconds);
    list = al.subList(0, 8);
    results = String2.annotatedString(String2.toNewlineString(list.toArray()));
    expected =
        "HTTP/1.1 206 [10]\n"
            + // 206=SC_PARTIAL_CONTENT
            "Content-Encoding: identity[10]\n"
            + "Content-Range: bytes 0-30/57600044[10]\n"
            + "Content-Type: audio/wav[10]\n"
            + "Content-Length: 31[10]\n"
            + "Date: "; // Wed, 27 Sep 2017 18:08:20 GMT[10]\n" +
    results2 = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(results2, expected, "results2=\n" + results2);

    // * request bytes=0- which is what <audio> seems to do
    al = SSR.dosShell(req + "-H \"Range: bytes=0-\"", timeOutSeconds);
    list = al.subList(0, 8);
    results = String2.annotatedString(String2.toNewlineString(list.toArray()));
    expected =
        "HTTP/1.1 206 [10]\n"
            + // 206=SC_PARTIAL_CONTENT
            "Content-Encoding: identity[10]\n"
            + "Content-Range: bytes 0-57600043/57600044[10]\n"
            + "Content-Type: audio/wav[10]\n"
            + "Content-Length: 57600044[10]\n"
            + "Date: "; // Wed, 27 Sep 2017 18:08:20 GMT[10]\n" +
    results2 = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(results2, expected, "results2=\n" + results2);

    // * request bytes=[start]- which is what <audio> seems to do
    al = SSR.dosShell(req + "-H \"Range: bytes=50000000-\"", timeOutSeconds);
    list = al.subList(0, 8);
    results = String2.annotatedString(String2.toNewlineString(list.toArray()));
    expected =
        "HTTP/1.1 206 [10]\n"
            + // 206=SC_PARTIAL_CONTENT
            "Content-Encoding: identity[10]\n"
            + "Content-Range: bytes 50000000-57600043/57600044[10]\n"
            + "Content-Type: audio/wav[10]\n"
            + "Content-Length: 7600044[10]\n"
            + "Date: "; // Wed, 27 Sep 2017 18:08:20 GMT[10]\n" +
    results2 = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(results2, expected, "results2=\n" + results2);

    // * request images/wz_tooltip.js
    al = SSR.dosShell(reqBase + "images/wz_tooltip.js -i", timeOutSeconds);
    list = al.subList(0, 5);
    results = String2.annotatedString(String2.toNewlineString(list.toArray()));
    expected =
        "HTTP/1.1 200 [10]\n"
            + "Cache-Control: PUBLIC, max-age=604800, must-revalidate[10]\n"
            + "Expires: ";
    results2 = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(results2, expected, "results2=\n" + results2);

    list = al.subList(4, 10);
    results = String2.annotatedString(String2.toNewlineString(list.toArray()));
    expected =
        "Content-Encoding: identity[10]\n"
            + "Accept-ranges: bytes[10]\n"
            + "Content-Type: application/x-javascript;charset=UTF-8[10]\n"
            + "Content-Length: 36673[10]\n"
            + "Date: ";
    results2 = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(results2, expected, "results2=\n" + results2);
  }
}
