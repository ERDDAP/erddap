package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import tags.TagMissingDataset;
import testDataset.Initialization;

class EDDTableFromNccsvFilesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * testGenerateDatasetsXml. This doesn't test suggestTestOutOfDate, except that for old data it
   * doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {

    String dataDir =
        File2.addSlash(
            Path.of(EDDTableFromNccsvFilesTests.class.getResource("/data/nccsv/").toURI())
                .toString());
    String fileNameRegex = "sampleScalar_1.2\\.csv";
    // testVerboseOn();
    String results =
        EDDTableFromNccsvFiles.generateDatasetsXml(
                dataDir,
                fileNameRegex,
                "",
                1440,
                "",
                "",
                "",
                "",
                "ship time",
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
                  "EDDTableFromNccsvFiles",
                  dataDir,
                  fileNameRegex,
                  "",
                  "1440",
                  "",
                  "",
                  "",
                  "",
                  "ship time",
                  "",
                  "",
                  "",
                  "",
                  "-1",
                  ""
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    String tDatasetID = EDDTableFromNccsvFiles.suggestDatasetID(dataDir + fileNameRegex);
    String expected =
        "<dataset type=\"EDDTableFromNccsvFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
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
            + "    <sortFilesBySourceNames>ship time</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <accessibleViaFiles>true</accessibleViaFiles>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"cdm_trajectory_variables\">ship</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3, NCCSV-1.1</att>\n"
            + "        <att name=\"creator_email\">bob.simons@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">Bob Simons</att>\n"
            + "        <att name=\"creator_type\">person</att>\n"
            + "        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n"
            + "        <att name=\"featureType\">trajectory</att>\n"
            + "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html</att>\n"
            + "        <att name=\"institution\">NOAA NMFS SWFSC ERD, NOAA PMEL</att>\n"
            + "        <att name=\"keywords\">NOAA, sea, ship, sst, surface, temperature, trajectory</att>\n"
            + "        <att name=\"license\">&quot;NCCSV Demonstration&quot; by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n"
            + "        <att name=\"subsetVariables\">ship</att>\n"
            + "        <att name=\"summary\">This is a paragraph or two describing the dataset.</att>\n"
            + "        <att name=\"title\">NCCSV Demonstration</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Trajectory</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"keywords\">byte, center, data, demonstration, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testByte, testLong, testUByte, testULong, time, trajectory, ubyte, ulong</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ship</sourceName>\n"
            + "        <destinationName>ship</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"cf_role\">trajectory_id</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Ship</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ssZ</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>status</sourceName>\n"
            + "        <destinationName>status</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">From http://some.url.gov/someProjectDocument , Table C</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Status</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>testByte</sourceName>\n"
            + "        <destinationName>testByte</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"units\">1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">-128</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Test Byte</att>\n"
            + "            <att name=\"missing_value\" type=\"byte\">127</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>testUByte</sourceName>\n"
            + "        <destinationName>testUByte</destinationName>\n"
            + "        <dataType>ubyte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"units\">1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"ubyte\">255</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Test UByte</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>testLong</sourceName>\n"
            + "        <destinationName>testLong</destinationName>\n"
            + "        <dataType>long</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"units\">1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"long\">-9223372036854775808</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Test Long</att>\n"
            + "            <att name=\"missing_value\" type=\"long\">9223372036854775807</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>testULong</sourceName>\n"
            + "        <destinationName>testULong</destinationName>\n"
            + "        <dataType>ulong</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"units\">1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"ulong\">18446744073709551615</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Test ULong</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sst</sourceName>\n"
            + "        <destinationName>sst</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.17 23.58</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">99.0</att>\n"
            + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"testBytes\" type=\"byteList\">-128 0 127</att>\n"
            + "            <att name=\"testChars\" type=\"charList\">\",\" \"\"\"\" \\u20ac</att>\n"
            + "            <att name=\"testDoubles\" type=\"doubleList\">-1.7976931348623157E308 0.0 1.7976931348623157E308</att>\n"
            + "            <att name=\"testFloats\" type=\"floatList\">-3.4028235E38 0.0 3.4028235E38</att>\n"
            + "            <att name=\"testInts\" type=\"intList\">-2147483648 0 2147483647</att>\n"
            + "            <att name=\"testLongs\" type=\"longList\">-9223372036854775808 0 9223372036854775807</att>\n"
            + "            <att name=\"testShorts\" type=\"shortList\">-32768 0 32767</att>\n"
            + "            <att name=\"testStrings\"> a&#9;~\u00fc,\n"
            + "&#39;z&quot;\u20ac</att>\n"
            + "            <att name=\"testUBytes\" type=\"ubyteList\">0 127 255</att>\n"
            + "            <att name=\"testUInts\" type=\"uintList\">0 2147483647 4294967295</att>\n"
            + "            <att name=\"testULongs\" type=\"ulongList\">0 9223372036854775807 18446744073709551615</att>\n"
            + "            <att name=\"testUShorts\" type=\"ushortList\">0 32767 65535</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"long_name\">Sea Surface Temperature</att>\n"
            + "            <att name=\"testStrings\">a&#9;~\u00fc,\n"
            + "&#39;z&quot;\u20ac</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

    // String tDatasetID = "sampleScalar_1_2_980e_c851_4e5e";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromNccsvFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), "NCCSV Demonstration", "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "ship, time, latitude, longitude, status, testByte, testUByte, testLong, testULong, sst",
        "");
  }

  /**
   * This does basic tests of this class. Note that ü in utf-8 is \xC3\xBC or [195][188] Note that
   * Euro is \\u20ac (and low byte is #172 is \\u00ac -- I worked to encode as '?')
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // no definition for testNccsvScalar11
  void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
    // String2.log("\n****************** EDDTableFromNccsvFiles.testBasic()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.

    Test.ensureEqual(String2.parseFloat("3.40282347e38"), 3.4028234663852886E38, "");
    Test.ensureEqual(String2.parseFloat("3.40282347e38") + "", "3.4028235E38", "");
    Test.ensureEqual(String2.parseFloat("3.4028235e38") + "", "3.4028235E38", "");
    Test.ensureEqual(Float.MAX_VALUE + "", "3.4028235E38", "");
    Test.ensureEqual(String.valueOf(Float.MAX_VALUE), "3.4028235E38", "");

    // long MIN_VALUE=-9,223,372,036,854,775,808 and
    // MAX_VALUE=9,223,372,036,854,775,807
    LongArray la =
        new LongArray(
            new long[] {
              -9223372036854775808L,
              -9007199254740992L,
              0,
              9007199254740992L,
              9223372036854775806L,
              9223372036854775807L
            });
    String2.log("la stats=" + PrimitiveArray.displayStats(la.calculateStats()));
    DoubleArray da = new DoubleArray(la);
    double laStats[] = la.calculateStats();
    String2.log(
        "da="
            + da
            + "\n"
            + "stats="
            + PrimitiveArray.displayStats(laStats)
            + "\nstats as doubles: "
            + String2.toCSSVString(laStats));
    // String2.pressEnterToContinue();

    String id = "testNccsvScalar11"; // straight from generateDatasetsXml
    if (deleteCachedDatasetInfo) EDDTableFromNccsvFiles.deleteCachedDatasetInfo(id);

    EDDTable eddTable = (EDDTable) EDDTableFromNccsvFiles.oneFromDatasetsXml(null, id);

    // *** test getting das for entire dataset
    String2.log(
        "\n****************** EDDTableFromNccsvFiles  test das and dds for entire dataset\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  ship {\n"
            + "    String cf_role \"trajectory_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Ship\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.4902299e+9, 1.4903127e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 27.9998, 28.0003;\n"
            + "    String axis \"Y\";\n"
            + "    Float64 colorBarMaximum 90.0;\n"
            + "    Float64 colorBarMinimum -90.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -132.1591, -130.2576;\n"
            + "    String axis \"X\";\n"
            + "    Float64 colorBarMaximum 180.0;\n"
            + "    Float64 colorBarMinimum -180.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  status {\n"
            + "    String actual_range \"\\t\n"
            + "?\";\n"
            + // important test of \\u20ac
            "    String comment \"From http://some.url.gov/someProjectDocument , Table C\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Status\";\n"
            + "  }\n"
            + "  testByte {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range -128, 126;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String units \"1\";\n"
            + "  }\n"
            + "  testUByte {\n"
            + "    Byte _FillValue -1;\n"
            + // important test of _Unsigned, in .das, no ubytes, so 255->-1
            "    String _Unsigned \"true\";\n"
            + // important test of _Unsigned
            "    Byte actual_range 0, -2;\n"
            + // important test of _Unsigned, in .das, no ubytes, so 254->-2
            "    String ioos_category \"Unknown\";\n"
            + "    String units \"1\";\n"
            + "  }\n"
            + "  testLong {\n"
            + "    Float64 _FillValue 9223372036854775807;\n"
            + // long MAX_VALUE is written out as long (not converted to
            // double or NaN)
            // long MIN_VALUE and MAX_VALUE written as longs to preserve full precision
            "    Float64 actual_range -9223372036854775808, 9223372036854775806;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Test of Longs\";\n"
            + "    String units \"1\";\n"
            + "  }\n"
            + "  testULong {\n"
            + "    Float64 _FillValue 18446744073709551615;\n"
            + "    Float64 actual_range 0, 18446744073709551614;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Test ULong\";\n"
            + "    String units \"1\";\n"
            + "  }\n"
            + "  sst {\n"
            + "    Float32 actual_range 10.0, 10.9;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Surface Temperature\";\n"
            + "    Float32 missing_value 99.0;\n"
            + "    String standard_name \"sea_surface_temperature\";\n"
            + "    Byte testBytes -128, 0, 127;\n"
            + "    String testChars \",\n"
            + "\\\"\n"
            + // important test of "
            "?\";\n"
            + // important test of \\u20ac
            "    Float64 testDoubles -1.7976931348623157e+308, 0.0, 1.7976931348623157e+308;\n"
            + "    Float32 testFloats -3.4028235e+38, 0.0, 3.4028235e+38;\n"
            + "    Int32 testInts -2147483648, 0, 2147483647;\n"
            + "    Float64 testLongs -9223372036854775808, -9007199254740992, 9007199254740992, 9223372036854775806, 9223372036854775807;\n"
            + // longs written as pseudo doubles
            "    Int16 testShorts -32768, 0, 32767;\n"
            + "    String testStrings \" a\\t~\u00fc,\n"
            + // important tests...
            "'z\\\"?\";\n"
            + // important test of \\u20ac
            "    Byte testUBytes 0, 127, -1;\n"
            + "    UInt32 testUInts 0, 2147483647, 4294967295;\n"
            + "    Float64 testULongs 0, 9223372036854775807, 18446744073709551615;\n"
            + // no ULong in DAP. Values written as
            // is, will full precision.
            "    UInt16 testUShorts 0, 32767, 65535;\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Trajectory\";\n"
            + "    String cdm_trajectory_variables \"ship\";\n"
            + "    String Conventions \"COARDS, CF-1.10, ACDD-1.3\";\n"
            + "    String creator_email \"bob.simons@noaa.gov\";\n"
            + "    String creator_name \"Bob Simons\";\n"
            + "    String creator_type \"person\";\n"
            + "    String creator_url \"https://www.pfeg.noaa.gov\";\n"
            + "    Float64 Easternmost_Easting -130.2576;\n"
            + "    String featureType \"Trajectory\";\n"
            + "    Float64 geospatial_lat_max 28.0003;\n"
            + "    Float64 geospatial_lat_min 27.9998;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -130.2576;\n"
            + "    Float64 geospatial_lon_min -132.1591;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        "http://127.0.0.1:8080/cwexperimental/tabledap/testNccsvScalar11.das\";\n"
            + "    String infoUrl \"https://erddap.github.io/NCCSV.html\";\n"
            + "    String institution \"NOAA NMFS SWFSC ERD, NOAA PMEL\";\n"
            + "    String keywords \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\";\n"
            + "    Float64 Northernmost_Northing 28.0003;\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing 27.9998;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v55\";\n"
            + "    String subsetVariables \"ship, status, testLong\";\n"
            + "    String summary \"This is a paragraph or two describing the dataset.\";\n"
            + "    String time_coverage_end \"2017-03-23T23:45:00Z\";\n"
            + "    String time_coverage_start \"2017-03-23T00:45:00Z\";\n"
            + "    String title \"NCCSV Demonstration\";\n"
            + "    Float64 Westernmost_Easting -132.1591;\n"
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
            + "    String ship;\n"
            + "    Float64 time;\n"
            + "    Float64 latitude;\n"
            + "    Float64 longitude;\n"
            + "    String status;\n"
            + // char -> String
            "    Byte testByte;\n"
            + // byte and ubyte both -> Byte
            "    Byte testUByte;\n"
            + "    Float64 testLong;\n"
            + // long -> double
            "    Float64 testULong;\n"
            + // long -> double
            "    Float32 sst;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n****************** EDDTableFromNccsvFiles.test make DATA FILES\n");

    // .asc all data
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all", ".asc");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String ship;\n"
            + "    Float64 time;\n"
            + "    Float64 latitude;\n"
            + "    Float64 longitude;\n"
            + "    String status;\n"
            + "    Byte testByte;\n"
            + // byte and ubyte both -> Byte
            "    Byte testUByte;\n"
            + "    Float64 testLong;\n"
            + // long -> double
            "    Float64 testULong;\n"
            + // long -> double
            "    Float32 sst;\n"
            + "  } s;\n"
            + "} s;\n"
            + "---------------------------------------------\n"
            + "s.ship, s.time, s.latitude, s.longitude, s.status, s.testByte, s.testUByte, s.testLong, s.testULong, s.sst\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4902299E9, 28.0002, -130.2576, \"A\", -128, 0, -9223372036854775808, 0, 10.9\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4902335E9, 28.0003, -130.3472, \"\\u20ac\", 0, 127, -9007199254740992, 9223372036854775807, 10.0\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4902371E9, 28.0001, -130.4305, \"\\t\", 126, 254, 9223372036854775806, 18446744073709551614, 99.0\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4902731E9, 27.9998, -131.5578, \"\\\"\", 127, 255, 9223372036854775807, 18446744073709551615, NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4903055E9, 28.0003, -132.0014, \"\\u00fc\", 127, 255, 9223372036854775807, 18446744073709551615, NaN\n"
            + // e.g., 128->127
            "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4903127E9, 28.0002, -132.1591, \"?\", 127, 255, 9223372036854775807, 18446744073709551615, NaN\n"; // e.g.,
    // ""
    // ->127
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv all data
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all", ".csv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + ",UTC,degrees_north,degrees_east,,1,1,1,1,degree_C\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808,0,10.9\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\"\\u20ac\",0,127,-9007199254740992,9223372036854775807,10.0\n"
            + // 2021-06-25 added quotes around \\u00fc
            "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\"\\t\",126,254,9223372036854775806,18446744073709551614,NaN\n"
            + // 2021-06-25 added quotes around \\t
            "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",NaN,NaN,NaN,NaN,NaN\n"
            + // int
            // mv
            // ->
            // NaN???
            "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\"\\u00fc\",NaN,NaN,NaN,NaN,NaN\n"
            + // 2021-06-25
            // added
            // quotes
            // around
            // \\u00fc
            "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN,NaN,NaN,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv subset
    userDapQuery = "time,ship,sst&time=2017-03-23T02:45";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1time", ".csv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "time,ship,sst\n"
            + "UTC,,degree_C\n"
            + "2017-03-23T02:45:00Z,\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv subset based on string constraint
    userDapQuery = "&ship=\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\""; // json formatted constraint
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1string", ".csv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + ",UTC,degrees_north,degrees_east,,1,1,1,1,degree_C\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808,0,10.9\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\"\\u20ac\",0,127,-9007199254740992,9223372036854775807,10.0\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\"\\t\",126,254,9223372036854775806,18446744073709551614,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",NaN,NaN,NaN,NaN,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\"\\u00fc\",NaN,NaN,NaN,NaN,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN,NaN,NaN,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv subset based on char constraint
    userDapQuery = "&status=\"\\u20ac\""; // json formatted constraint
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1char", ".csv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + ",UTC,degrees_north,degrees_east,,1,1,1,1,degree_C\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\"\\u20ac\",0,127,-9007199254740992,9223372036854775807,10.0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv subset based on easy long constraint
    userDapQuery = "&testLong=-9007199254740992";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1long", ".csv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + ",UTC,degrees_north,degrees_east,,1,1,1,1,degree_C\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\"\\u20ac\",0,127,-9007199254740992,9223372036854775807,10.0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv subset based on harder long constraint
    userDapQuery = "&testLong=-9223372036854775808";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1longb", ".csv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + ",UTC,degrees_north,degrees_east,,1,1,1,1,degree_C\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808,0,10.9\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv subset based on harder ulong constraint
    userDapQuery = "&testULong=18446744073709551614";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_u1longb", ".csv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + ",UTC,degrees_north,degrees_east,,1,1,1,1,degree_C\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\"\\t\",126,254,9223372036854775806,18446744073709551614,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .nccsvMetadata
    userDapQuery = "time,ship,sst&time=2017-03-23T02:45"; // will be ignored
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddTable.className() + "_all",
            ".nccsvMetadata");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.10, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_data_type,Trajectory\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,Easternmost_Easting,-130.2576d\n"
            + "*GLOBAL*,featureType,Trajectory\n"
            + "*GLOBAL*,geospatial_lat_max,28.0003d\n"
            + "*GLOBAL*,geospatial_lat_min,27.9998d\n"
            + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
            + "*GLOBAL*,geospatial_lon_max,-130.2576d\n"
            + "*GLOBAL*,geospatial_lon_min,-132.1591d\n"
            + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
            + "*GLOBAL*,infoUrl,https://erddap.github.io/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n"
            + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,Northernmost_Northing,28.0003d\n"
            + "*GLOBAL*,sourceUrl,(local files)\n"
            + "*GLOBAL*,Southernmost_Northing,27.9998d\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n"
            + "*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "*GLOBAL*,Westernmost_Easting,-132.1591d\n"
            + "ship,*DATA_TYPE*,String\n"
            + "ship,cf_role,trajectory_id\n"
            + "ship,ioos_category,Identifier\n"
            + "ship,long_name,Ship\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,_CoordinateAxisType,Time\n"
            + "time,actual_range,2017-03-23T00:45:00Z\\n2017-03-23T23:45:00Z\n"
            + "time,axis,T\n"
            + "time,ioos_category,Time\n"
            + "time,long_name,Time\n"
            + "time,standard_name,time\n"
            + "time,time_origin,01-JAN-1970 00:00:00\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "latitude,*DATA_TYPE*,double\n"
            + "latitude,_CoordinateAxisType,Lat\n"
            + "latitude,actual_range,27.9998d,28.0003d\n"
            + "latitude,axis,Y\n"
            + "latitude,colorBarMaximum,90.0d\n"
            + "latitude,colorBarMinimum,-90.0d\n"
            + "latitude,ioos_category,Location\n"
            + "latitude,long_name,Latitude\n"
            + "latitude,standard_name,latitude\n"
            + "latitude,units,degrees_north\n"
            + "longitude,*DATA_TYPE*,double\n"
            + "longitude,_CoordinateAxisType,Lon\n"
            + "longitude,actual_range,-132.1591d,-130.2576d\n"
            + "longitude,axis,X\n"
            + "longitude,colorBarMaximum,180.0d\n"
            + "longitude,colorBarMinimum,-180.0d\n"
            + "longitude,ioos_category,Location\n"
            + "longitude,long_name,Longitude\n"
            + "longitude,standard_name,longitude\n"
            + "longitude,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,actual_range,\"'\\t'\",\"'\u20ac'\"\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "status,ioos_category,Unknown\n"
            + "status,long_name,Status\n"
            + "testByte,*DATA_TYPE*,byte\n"
            + "testByte,_FillValue,127b\n"
            + "testByte,actual_range,-128b,126b\n"
            + "testByte,ioos_category,Unknown\n"
            + "testByte,units,\"1\"\n"
            + "testUByte,*DATA_TYPE*,ubyte\n"
            + "testUByte,_FillValue,255ub\n"
            + "testUByte,actual_range,0ub,254ub\n"
            + "testUByte,ioos_category,Unknown\n"
            + "testUByte,units,\"1\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,_FillValue,9223372036854775807L\n"
            + "testLong,actual_range,-9223372036854775808L,9223372036854775806L\n"
            + // max should be ...806
            "testLong,ioos_category,Unknown\n"
            + "testLong,long_name,Test of Longs\n"
            + "testLong,units,\"1\"\n"
            + "testULong,*DATA_TYPE*,ulong\n"
            + "testULong,_FillValue,18446744073709551615uL\n"
            + "testULong,actual_range,0uL,18446744073709551614uL\n"
            + // max should be ...614
            "testULong,ioos_category,Unknown\n"
            + "testULong,long_name,Test ULong\n"
            + "testULong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,actual_range,10.0f,10.9f\n"
            + "sst,colorBarMaximum,32.0d\n"
            + "sst,colorBarMinimum,0.0d\n"
            + "sst,ioos_category,Temperature\n"
            + "sst,long_name,Sea Surface Temperature\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .nccsv all
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all", ".nccsv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.10, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_data_type,Trajectory\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,Easternmost_Easting,-130.2576d\n"
            + "*GLOBAL*,featureType,Trajectory\n"
            + "*GLOBAL*,geospatial_lat_max,28.0003d\n"
            + "*GLOBAL*,geospatial_lat_min,27.9998d\n"
            + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
            + "*GLOBAL*,geospatial_lon_max,-130.2576d\n"
            + "*GLOBAL*,geospatial_lon_min,-132.1591d\n"
            + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
            + "*GLOBAL*,history,"
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        // T17:35:08Z (local files)\\n2017-04-18T17:35:08Z
        "http://127.0.0.1:8080/cwexperimental/tabledap/testNccsvScalar11.nccsv\n"
            + "*GLOBAL*,infoUrl,https://erddap.github.io/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n"
            + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,Northernmost_Northing,28.0003d\n"
            + "*GLOBAL*,sourceUrl,(local files)\n"
            + "*GLOBAL*,Southernmost_Northing,27.9998d\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n"
            + "*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "*GLOBAL*,Westernmost_Easting,-132.1591d\n"
            + "ship,*DATA_TYPE*,String\n"
            + "ship,cf_role,trajectory_id\n"
            + "ship,ioos_category,Identifier\n"
            + "ship,long_name,Ship\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,_CoordinateAxisType,Time\n"
            + "time,axis,T\n"
            + "time,ioos_category,Time\n"
            + "time,long_name,Time\n"
            + "time,standard_name,time\n"
            + "time,time_origin,01-JAN-1970 00:00:00\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "latitude,*DATA_TYPE*,double\n"
            + "latitude,_CoordinateAxisType,Lat\n"
            + "latitude,axis,Y\n"
            + "latitude,colorBarMaximum,90.0d\n"
            + "latitude,colorBarMinimum,-90.0d\n"
            + "latitude,ioos_category,Location\n"
            + "latitude,long_name,Latitude\n"
            + "latitude,standard_name,latitude\n"
            + "latitude,units,degrees_north\n"
            + "longitude,*DATA_TYPE*,double\n"
            + "longitude,_CoordinateAxisType,Lon\n"
            + "longitude,axis,X\n"
            + "longitude,colorBarMaximum,180.0d\n"
            + "longitude,colorBarMinimum,-180.0d\n"
            + "longitude,ioos_category,Location\n"
            + "longitude,long_name,Longitude\n"
            + "longitude,standard_name,longitude\n"
            + "longitude,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "status,ioos_category,Unknown\n"
            + "status,long_name,Status\n"
            + "testByte,*DATA_TYPE*,byte\n"
            + "testByte,_FillValue,127b\n"
            + "testByte,ioos_category,Unknown\n"
            + "testByte,units,\"1\"\n"
            + "testUByte,*DATA_TYPE*,ubyte\n"
            + "testUByte,_FillValue,255ub\n"
            + "testUByte,ioos_category,Unknown\n"
            + "testUByte,units,\"1\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,_FillValue,9223372036854775807L\n"
            + "testLong,ioos_category,Unknown\n"
            + "testLong,long_name,Test of Longs\n"
            + "testLong,units,\"1\"\n"
            + "testULong,*DATA_TYPE*,ulong\n"
            + "testULong,_FillValue,18446744073709551615uL\n"
            + "testULong,ioos_category,Unknown\n"
            + "testULong,long_name,Test ULong\n"
            + "testULong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,colorBarMaximum,32.0d\n"
            + "sst,colorBarMinimum,0.0d\n"
            + "sst,ioos_category,Temperature\n"
            + "sst,long_name,Sea Surface Temperature\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n"
            + "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808L,0uL,10.9\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\u20ac,0,127,-9007199254740992L,9223372036854775807uL,10.0\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,126,254,9223372036854775806L,18446744073709551614uL,99.0\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",,,,,\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\u00fc,,,,,\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,,,,,\n"
            + "*END_DATA*\n";
    tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // .nccsv subset
    userDapQuery = "time,ship,sst&time=2017-03-23T02:45";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1time", ".nccsv");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.10, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_data_type,Trajectory\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,Easternmost_Easting,-130.2576d\n"
            + "*GLOBAL*,featureType,Trajectory\n"
            + "*GLOBAL*,geospatial_lat_max,28.0003d\n"
            + "*GLOBAL*,geospatial_lat_min,27.9998d\n"
            + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
            + "*GLOBAL*,geospatial_lon_max,-130.2576d\n"
            + "*GLOBAL*,geospatial_lon_min,-132.1591d\n"
            + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
            + "*GLOBAL*,history,\""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        // 2017-04-18T17:41:53Z (local files)\\n2017-04-18T17:41:53Z
        "http://127.0.0.1:8080/cwexperimental/tabledap/testNccsvScalar11.nccsv?time,ship,sst&time=2017-03-23T02:45\"\n"
            + "*GLOBAL*,infoUrl,https://erddap.github.io/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n"
            + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,Northernmost_Northing,28.0003d\n"
            + "*GLOBAL*,sourceUrl,(local files)\n"
            + "*GLOBAL*,Southernmost_Northing,27.9998d\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n"
            + "*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "*GLOBAL*,Westernmost_Easting,-132.1591d\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,_CoordinateAxisType,Time\n"
            + "time,axis,T\n"
            + "time,ioos_category,Time\n"
            + "time,long_name,Time\n"
            + "time,standard_name,time\n"
            + "time,time_origin,01-JAN-1970 00:00:00\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "ship,*DATA_TYPE*,String\n"
            + "ship,cf_role,trajectory_id\n"
            + "ship,ioos_category,Identifier\n"
            + "ship,long_name,Ship\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,colorBarMaximum,32.0d\n"
            + "sst,colorBarMinimum,0.0d\n"
            + "sst,ioos_category,Temperature\n"
            + "sst,long_name,Sea Surface Temperature\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n"
            + "time,ship,sst\n"
            + "2017-03-23T02:45:00Z,\" a\\t~\u00fc,\\n'z\"\"\u20ac\",99.0\n"
            + "*END_DATA*\n";
    tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);
  }

  /**
   * This tests how high ascii and unicode chars in attributes and data appear in various output
   * file types.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // no definition for testNccsvScalar11
  void testChar() throws Throwable {
    // String2.log("\n****************** EDDTableFromNccsvFiles.testChar()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.
    int po, tPo;

    String id = "testNccsvScalar11"; // straight from generateDatasetsXml
    EDDTable eddTable = (EDDTable) EDDTableFromNccsvFiles.oneFromDatasetsXml(null, id);

    // *** getting dap asc
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".asc");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String ship;\n"
            + "    Float64 time;\n"
            + "    Float64 latitude;\n"
            + "    Float64 longitude;\n"
            + "    String status;\n"
            + "    Byte testByte;\n"
            + "    Byte testUByte;\n"
            + "    Float64 testLong;\n"
            + "    Float64 testULong;\n"
            + "    Float32 sst;\n"
            + "  } s;\n"
            + "} s;\n"
            + "---------------------------------------------\n"
            + "s.ship, s.time, s.latitude, s.longitude, s.status, s.testByte, s.testUByte, s.testLong, s.testULong, s.sst\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4902299E9, 28.0002, -130.2576, \"A\", -128, 0, -9223372036854775808, 0, 10.9\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4902335E9, 28.0003, -130.3472, \"\\u20ac\", 0, 127, -9007199254740992, 9223372036854775807, 10.0\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4902371E9, 28.0001, -130.4305, \"\\t\", 126, 254, 9223372036854775806, 18446744073709551614, 99.0\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4902731E9, 27.9998, -131.5578, \"\\\"\", 127, 255, 9223372036854775807, 18446744073709551615, NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4903055E9, 28.0003, -132.0014, \"\\u00fc\", 127, 255, 9223372036854775807, 18446744073709551615, NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", 1.4903127E9, 28.0002, -132.1591, \"?\", 127, 255, 9223372036854775807, 18446744073709551615, NaN\n";
    // 2020-04-08 I changed to using \-encoded strings (which was implied by stated
    // support for \" )
    // was (was results.annotatedString()) :
    // "\" a[9]~[252],[10]\n" +
    // "'z\\\"?\", 1.4902299E9, 28.0002, -130.2576, \"A\", -9223372036854775808,
    // 10.9[10]\n" +
    // "\" a[9]~[252],[10]\n" +
    // "'z\\\"?\", 1.4902335E9, 28.0003, -130.3472, \"?\", -9007199254740992,
    // [10]\n" +
    // "\" a[9]~[252],[10]\n" +
    // "'z\\\"?\", 1.4902371E9, 28.0001, -130.4305, \"[9]\", 9007199254740992,
    // 10.7[10]\n" +
    // "\" a[9]~[252],[10]\n" +
    // "'z\\\"?\", 1.4902731E9, 27.9998, -131.5578, \"\\\"\", 9223372036854775806,
    // 99.0[10]\n" +
    // "\" a[9]~[252],[10]\n" +
    // "'z\\\"?\", 1.4903055E9, 28.0003, -132.0014, \"[252]\", , 10.0[10]\n" +
    // "\" a[9]~[252],[10]\n" +
    // "'z\\\"?\", 1.4903127E9, 28.0002, -132.1591, \"?\", , [10]\n" +
    // "[end]";
    // ship is " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting csv
    // written as 7-bit ASCII NCCSV strings to ISO-8859-1 (irrelevant)
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + ",UTC,degrees_north,degrees_east,,1,1,1,1,degree_C\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808,0,10.9\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\"\\u20ac\",0,127,-9007199254740992,9223372036854775807,10.0\n"
            + // 2021-05-25 now special chars are in "'s, eg \\u20ac
            "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\"\\t\",126,254,9223372036854775806,18446744073709551614,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",NaN,NaN,NaN,NaN,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\"\\u00fc\",NaN,NaN,NaN,NaN,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN,NaN,NaN,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // ship is " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting csvp
    // written as 7-bit ASCII NCCSV strings to ISO-8859-1 (irrelevant)
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".csvp");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "ship,time (UTC),latitude (degrees_north),longitude (degrees_east),status,testByte (1),testUByte (1),testLong (1),testULong (1),sst (degree_C)\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808,0,10.9\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\"\\u20ac\",0,127,-9007199254740992,9223372036854775807,10.0\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\"\\t\",126,254,9223372036854775806,18446744073709551614,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",NaN,NaN,NaN,NaN,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\"\\u00fc\",NaN,NaN,NaN,NaN,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN,NaN,NaN,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // ship is " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting csv0
    // written as 7-bit ASCII NCCSV strings to ISO-8859-1 (irrelevant)
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".csv0");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808,0,10.9\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\"\\u20ac\",0,127,-9007199254740992,9223372036854775807,10.0\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\"\\t\",126,254,9223372036854775806,18446744073709551614,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",NaN,NaN,NaN,NaN,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\"\\u00fc\",NaN,NaN,NaN,NaN,NaN\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,NaN,NaN,NaN,NaN,NaN\n";
    // ship is " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // das and dds tested above

    // *** getting dods
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".dods");
    results = String2.annotatedString(File2.directReadFrom88591File(dir + tName));
    // String2.log(results);
    expected =
        "Dataset {[10]\n"
            + "  Sequence {[10]\n"
            + "    String ship;[10]\n"
            + "    Float64 time;[10]\n"
            + "    Float64 latitude;[10]\n"
            + "    Float64 longitude;[10]\n"
            + "    String status;[10]\n"
            + "    Byte testByte;[10]\n"
            + "    Byte testUByte;[10]\n"
            + "    Float64 testLong;[10]\n"
            + "    Float64 testULong;[10]\n"
            + "    Float32 sst;[10]\n"
            + "  } s;[10]\n"
            + "} s;[10]\n"
            + "[10]\n"
            + "Data:[10]\n"
            + "Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n"
            + "'z\"?[0]A[214]4[198][163][0][0][0]@<[0][13][27]qu[142][192]`H>BZ[238]c[0][0][0][1]A[0][0][0][128][0][0][0][0][0][0][0][128][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0]A.ffZ[0][0][0][0][0][0][11] a[9]~[252],[10]\n"
            + "'z\"?[0]A[214]4[202]'[0][0][0]@<[0][19][169]*0U[192]`K[28]C,[165]z[0][0][0][1]?[0][0][0][0][0][0][0][127][0][0][0][255][224][0][0][0][0][0][0][127][255][255][255][255][255][255][255]A [0][0]Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n"
            + "'z\"?[0]A[214]4[205][171][0][0][0]@<[0][6][141][184][186][199][192]`M[198][167][239][157][178][0][0][0][1][9][0][0][0]~[0][0][0][254][0][0][0][127][255][255][255][255][255][255][254][255][255][255][255][255][255][255][254]B[198][0][0]Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n"
            + "'z\"?[0]A[214]4[240][211][0][0][0]@;[255][242][228][142][138]r[192]`q[217][127]b[182][174][0][0][0][1]\"[0][0][0][127][0][0][0][255][0][0][0][127][255][255][255][255][255][255][255][255][255][255][255][255][255][255][255][127][192][0][0]Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n"
            + "'z\"?[0]A[214]5[16]w[0][0][0]@<[0][19][169]*0U[192]`[128][11]x[3]F[220][0][0][0][1][252][0][0][0][127][0][0][0][255][0][0][0][127][255][255][255][255][255][255][255][255][255][255][255][255][255][255][255][127][192][0][0]Z[0][0][0][0][0][0][11] a[9]~[252],[10]\n"
            + "'z\"?[0]A[214]5[23][127][0][0][0]@<[0][13][27]qu[142][192]`[133][23]X[226][25]e[0][0][0][1]?[0][0][0][127][0][0][0][255][0][0][0][127][255][255][255][255][255][255][255][255][255][255][255][255][255][255][255][127][192][0][0][165][0][0][0][end]";
    // ship is " a\t~\u00fc,\n'z""\u20AC". 20AC is being encoded with ISO-8859-1 as
    // '?'
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting esriCsv written as 7bit ASCII via ISO-8859-1
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".esriCsv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "ship,date,time,Y,X,status,testByte,testUByte,testLong,testULong,sst\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,12:45:00 am,28.0002,-130.2576,A,-128,0,-9223372036854775808,0,10.9\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,1:45:00 am,28.0003,-130.3472,\\u20ac,0,127,-9007199254740992,9223372036854775807,10.0\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,2:45:00 am,28.0001,-130.4305,\\t,126,254,9223372036854775806,18446744073709551614,-9999.0\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,12:45:00 pm,27.9998,-131.5578,\"\"\"\",-9999,-9999,-9999,-9999,-9999.0\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,9:45:00 pm,28.0003,-132.0014,\\u00fc,-9999,-9999,-9999,-9999,-9999.0\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23,11:45:00 pm,28.0002,-132.1591,?,-9999,-9999,-9999,-9999,-9999.0\n";
    // ship is " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting geoJson
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".geoJson");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        // This is JSON encoding of " a\t~\u00fc,\n'z""\u20AC"
        // source status chars are A\u20AC\t"\u00fc\uFFFF
        results =
            "{\n"
                + "  \"type\": \"FeatureCollection\",\n"
                + "  \"propertyNames\": [\"ship\", \"time\", \"status\", \"testByte\", \"testUByte\", \"testLong\", \"testULong\", \"sst\"],\n"
                + "  \"propertyUnits\": [null, \"UTC\", null, \"1\", \"1\", \"1\", \"1\", \"degree_C\"],\n"
                + "  \"features\": [\n"
                + "{\"type\": \"Feature\",\n"
                + "  \"geometry\": {\n"
                + "    \"type\": \"Point\",\n"
                + "    \"coordinates\": [-130.2576, 28.0002] },\n"
                + "  \"properties\": {\n"
                + "    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",\n"
                + "    \"time\": \"2017-03-23T00:45:00Z\",\n"
                + "    \"status\": \"A\",\n"
                + "    \"testByte\": -128,\n"
                + "    \"testUByte\": 0,\n"
                + "    \"testLong\": -9223372036854775808,\n"
                + "    \"testULong\": 0,\n"
                + "    \"sst\": 10.9 }\n"
                + "},\n"
                + "{\"type\": \"Feature\",\n"
                + "  \"geometry\": {\n"
                + "    \"type\": \"Point\",\n"
                + "    \"coordinates\": [-130.3472, 28.0003] },\n"
                + "  \"properties\": {\n"
                + "    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",\n"
                + "    \"time\": \"2017-03-23T01:45:00Z\",\n"
                + "    \"status\": \"\\u20ac\",\n"
                + "    \"testByte\": 0,\n"
                + "    \"testUByte\": 127,\n"
                + "    \"testLong\": -9007199254740992,\n"
                + "    \"testULong\": 9223372036854775807,\n"
                + "    \"sst\": 10.0 }\n"
                + "},\n"
                + "{\"type\": \"Feature\",\n"
                + "  \"geometry\": {\n"
                + "    \"type\": \"Point\",\n"
                + "    \"coordinates\": [-130.4305, 28.0001] },\n"
                + "  \"properties\": {\n"
                + "    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",\n"
                + "    \"time\": \"2017-03-23T02:45:00Z\",\n"
                + "    \"status\": \"\\t\",\n"
                + "    \"testByte\": 126,\n"
                + "    \"testUByte\": 254,\n"
                + "    \"testLong\": 9223372036854775806,\n"
                + "    \"testULong\": 18446744073709551614,\n"
                + "    \"sst\": null }\n"
                + "},\n"
                + "{\"type\": \"Feature\",\n"
                + "  \"geometry\": {\n"
                + "    \"type\": \"Point\",\n"
                + "    \"coordinates\": [-131.5578, 27.9998] },\n"
                + "  \"properties\": {\n"
                + "    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",\n"
                + "    \"time\": \"2017-03-23T12:45:00Z\",\n"
                + "    \"status\": \"\\\"\",\n"
                + "    \"testByte\": null,\n"
                + "    \"testUByte\": null,\n"
                + "    \"testLong\": null,\n"
                + "    \"testULong\": null,\n"
                + "    \"sst\": null }\n"
                + "},\n"
                + "{\"type\": \"Feature\",\n"
                + "  \"geometry\": {\n"
                + "    \"type\": \"Point\",\n"
                + "    \"coordinates\": [-132.0014, 28.0003] },\n"
                + "  \"properties\": {\n"
                + "    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\",\n"
                + "    \"time\": \"2017-03-23T21:45:00Z\",\n"
                + "    \"status\": \"\\u00fc\",\n"
                + "    \"testByte\": null,\n"
                + "    \"testUByte\": null,\n"
                + "    \"testLong\": null,\n"
                + "    \"testULong\": null,\n"
                + "    \"sst\": null }\n"
                + "},\n"
                + "{\"type\": \"Feature\",\n"
                + "  \"geometry\": {\n"
                + "    \"type\": \"Point\",\n"
                + "    \"coordinates\": [-132.1591, 28.0002] },\n"
                + "  \"properties\": {\n"
                + "    \"ship\": \" a\\t~\\u00fc,\\n'z\\\"\u20ac\",\n"
                + "    \"time\": \"2017-03-23T23:45:00Z\",\n"
                + "    \"status\": \"?\",\n"
                + "    \"testByte\": null,\n"
                + "    \"testUByte\": null,\n"
                + "    \"testLong\": null,\n"
                + "    \"testULong\": null,\n"
                + "    \"sst\": null }\n"
                + "}\n"
                + "  ],\n"
                + "  \"bbox\": [-132.1591, 27.9998, -130.2576, 28.0003]\n"
                + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting htmlTable
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".htmlTable");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "<table class=\"erd commonBGColor nowrap\">\n"
            + "<tr>\n"
            + "<th>ship\n"
            + "<th>time\n"
            + "<th>latitude\n"
            + "<th>longitude\n"
            + "<th>status\n"
            + "<th>testByte\n"
            + "<th>testUByte\n"
            + "<th>testLong\n"
            + "<th>testULong\n"
            + "<th>sst\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>\n"
            + "<th>UTC\n"
            + "<th>degrees_north\n"
            + "<th>degrees_east\n"
            + "<th>\n"
            + "<th>1\n"
            + "<th>1\n"
            + "<th>1\n"
            + "<th>1\n"
            + "<th>degree_C\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td> a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n"
            + "<td>2017-03-23T00:45:00Z\n"
            + "<td class=\"R\">28.0002\n"
            + "<td class=\"R\">-130.2576\n"
            + "<td>A\n"
            + "<td class=\"R\">-128\n"
            + "<td class=\"R\">0\n"
            + "<td class=\"R\">-9223372036854775808\n"
            + "<td class=\"R\">0\n"
            + "<td class=\"R\">10.9\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td> a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n"
            + "<td>2017-03-23T01:45:00Z\n"
            + "<td class=\"R\">28.0003\n"
            + "<td class=\"R\">-130.3472\n"
            + "<td>&#x20ac;\n"
            + "<td class=\"R\">0\n"
            + "<td class=\"R\">127\n"
            + "<td class=\"R\">-9007199254740992\n"
            + "<td class=\"R\">9223372036854775807\n"
            + "<td class=\"R\">10.0\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td> a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n"
            + "<td>2017-03-23T02:45:00Z\n"
            + "<td class=\"R\">28.0001\n"
            + "<td class=\"R\">-130.4305\n"
            + "<td>\\t\n"
            + "<td class=\"R\">126\n"
            + "<td class=\"R\">254\n"
            + "<td class=\"R\">9223372036854775806\n"
            + "<td class=\"R\">18446744073709551614\n"
            + "<td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td> a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n"
            + "<td>2017-03-23T12:45:00Z\n"
            + "<td class=\"R\">27.9998\n"
            + "<td class=\"R\">-131.5578\n"
            + "<td>\\&quot;\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td> a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n"
            + "<td>2017-03-23T21:45:00Z\n"
            + "<td class=\"R\">28.0003\n"
            + "<td class=\"R\">-132.0014\n"
            + "<td>&uuml;\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td> a\\t~&uuml;,\\n&#39;z\\&quot;&#x20ac;\n"
            + "<td>2017-03-23T23:45:00Z\n"
            + "<td class=\"R\">28.0002\n"
            + "<td class=\"R\">-132.1591\n"
            + "<td>?\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "</tr>\n"
            + "</table>";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    po = results.indexOf(expected.substring(0, 40));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // *** getting itx
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".itx");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, '\r', '\n');
    // String2.log(results);
    expected =
        "IGOR\n"
            + "WAVES/T ship\n"
            + "BEGIN\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"\n"
            + "END\n"
            + "\n"
            + "WAVES/D time2\n"
            + "BEGIN\n"
            + "3.5730747E9\n"
            + "3.5730783E9\n"
            + "3.5730819E9\n"
            + "3.5731179E9\n"
            + "3.5731503E9\n"
            + "3.5731575E9\n"
            + "END\n"
            + "X SetScale d 3.5730747E9,3.5731575E9, \"dat\", time2\n"
            + "\n"
            + "WAVES/D latitude\n"
            + "BEGIN\n"
            + "28.0002\n"
            + "28.0003\n"
            + "28.0001\n"
            + "27.9998\n"
            + "28.0003\n"
            + "28.0002\n"
            + "END\n"
            + "X SetScale d 27.9998,28.0003, \"degrees_north\", latitude\n"
            + "\n"
            + "WAVES/D longitude\n"
            + "BEGIN\n"
            + "-130.2576\n"
            + "-130.3472\n"
            + "-130.4305\n"
            + "-131.5578\n"
            + "-132.0014\n"
            + "-132.1591\n"
            + "END\n"
            + "X SetScale d -132.1591,-130.2576, \"degrees_east\", longitude\n"
            + "\n"
            + "WAVES/T status\n"
            + "BEGIN\n"
            + "\"A\"\n"
            + "\"\\u20ac\"\n"
            + "\"\\t\"\n"
            + "\"\\\"\"\n"
            + "\"\\u00fc\"\n"
            + "\"?\"\n"
            + "END\n"
            + "\n"
            + "WAVES/B testByte\n"
            + "BEGIN\n"
            + "-128\n"
            + "0\n"
            + "126\n"
            + "NaN\n"
            + "NaN\n"
            + "NaN\n"
            + "END\n"
            + "X SetScale d -128,126, \"1\", testByte\n"
            + "\n"
            + "WAVES/B/U testUByte\n"
            + "BEGIN\n"
            + "0\n"
            + "127\n"
            + "254\n"
            + "NaN\n"
            + "NaN\n"
            + "NaN\n"
            + "END\n"
            + "X SetScale d 0,254, \"1\", testUByte\n"
            + "\n"
            + "WAVES/T testLong\n"
            + "BEGIN\n"
            + "\"-9223372036854775808\"\n"
            + "\"-9007199254740992\"\n"
            + "\"9223372036854775806\"\n"
            + "\"\"\n"
            + "\"\"\n"
            + "\"\"\n"
            + "END\n"
            + "\n"
            + "WAVES/T testULong\n"
            + "BEGIN\n"
            + "\"0\"\n"
            + "\"9223372036854775807\"\n"
            + "\"18446744073709551614\"\n"
            + "\"\"\n"
            + "\"\"\n"
            + "\"\"\n"
            + "END\n"
            + "\n"
            + "WAVES/S sst\n"
            + "BEGIN\n"
            + "10.9\n"
            + "10.0\n"
            + "NaN\n"
            + "NaN\n"
            + "NaN\n"
            + "NaN\n"
            + "END\n"
            + "X SetScale d 10.0,10.9, \"degree_C\", sst\n"
            + "\n";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting json
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".json");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"ship\", \"time\", \"latitude\", \"longitude\", \"status\", \"testByte\", \"testUByte\", \"testLong\", \"testULong\", \"sst\"],\n"
            + "    \"columnTypes\": [\"String\", \"String\", \"double\", \"double\", \"char\", \"byte\", \"ubyte\", \"long\", \"ulong\", \"float\"],\n"
            + "    \"columnUnits\": [null, \"UTC\", \"degrees_north\", \"degrees_east\", null, \"1\", \"1\", \"1\", \"1\", \"degree_C\"],\n"
            + "    \"rows\": [\n"
            + "      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T00:45:00Z\", 28.0002, -130.2576, \"A\", -128, 0, -9223372036854775808, 0, 10.9],\n"
            + "      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T01:45:00Z\", 28.0003, -130.3472, \"\\u20ac\", 0, 127, -9007199254740992, 9223372036854775807, 10],\n"
            + "      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T02:45:00Z\", 28.0001, -130.4305, \"\\t\", 126, 254, 9223372036854775806, 18446744073709551614, null],\n"
            + "      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T12:45:00Z\", 27.9998, -131.5578, \"\\\"\", null, null, null, null, null],\n"
            + "      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T21:45:00Z\", 28.0003, -132.0014, \"\\u00fc\", null, null, null, null, null],\n"
            + "      [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T23:45:00Z\", 28.0002, -132.1591, \"?\", null, null, null, null, null]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting jsonlCSV1
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".jsonlCSV1");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "[\"ship\", \"time\", \"latitude\", \"longitude\", \"status\", \"testByte\", \"testUByte\", \"testLong\", \"testULong\", \"sst\"]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T00:45:00Z\", 28.0002, -130.2576, \"A\", -128, 0, -9223372036854775808, 0, 10.9]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T01:45:00Z\", 28.0003, -130.3472, \"\\u20ac\", 0, 127, -9007199254740992, 9223372036854775807, 10]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T02:45:00Z\", 28.0001, -130.4305, \"\\t\", 126, 254, 9223372036854775806, 18446744073709551614, null]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T12:45:00Z\", 27.9998, -131.5578, \"\\\"\", null, null, null, null, null]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T21:45:00Z\", 28.0003, -132.0014, \"\\u00fc\", null, null, null, null, null]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T23:45:00Z\", 28.0002, -132.1591, \"?\", null, null, null, null, null]\n";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting jsonlCSV
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".jsonlCSV");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T00:45:00Z\", 28.0002, -130.2576, \"A\", -128, 0, -9223372036854775808, 0, 10.9]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T01:45:00Z\", 28.0003, -130.3472, \"\\u20ac\", 0, 127, -9007199254740992, 9223372036854775807, 10]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T02:45:00Z\", 28.0001, -130.4305, \"\\t\", 126, 254, 9223372036854775806, 18446744073709551614, null]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T12:45:00Z\", 27.9998, -131.5578, \"\\\"\", null, null, null, null, null]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T21:45:00Z\", 28.0003, -132.0014, \"\\u00fc\", null, null, null, null, null]\n"
            + "[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"2017-03-23T23:45:00Z\", 28.0002, -132.1591, \"?\", null, null, null, null, null]\n";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting jsonlKVP
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".jsonlKVP");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T00:45:00Z\", \"latitude\":28.0002, \"longitude\":-130.2576, \"status\":\"A\", \"testByte\":-128, \"testUByte\":0, \"testLong\":-9223372036854775808, \"testULong\":0, \"sst\":10.9}\n"
            + "{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T01:45:00Z\", \"latitude\":28.0003, \"longitude\":-130.3472, \"status\":\"\\u20ac\", \"testByte\":0, \"testUByte\":127, \"testLong\":-9007199254740992, \"testULong\":9223372036854775807, \"sst\":10}\n"
            + "{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T02:45:00Z\", \"latitude\":28.0001, \"longitude\":-130.4305, \"status\":\"\\t\", \"testByte\":126, \"testUByte\":254, \"testLong\":9223372036854775806, \"testULong\":18446744073709551614, \"sst\":null}\n"
            + "{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T12:45:00Z\", \"latitude\":27.9998, \"longitude\":-131.5578, \"status\":\"\\\"\", \"testByte\":null, \"testUByte\":null, \"testLong\":null, \"testULong\":null, \"sst\":null}\n"
            + "{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T21:45:00Z\", \"latitude\":28.0003, \"longitude\":-132.0014, \"status\":\"\\u00fc\", \"testByte\":null, \"testUByte\":null, \"testLong\":null, \"testULong\":null, \"sst\":null}\n"
            + "{\"ship\":\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\", \"time\":\"2017-03-23T23:45:00Z\", \"latitude\":28.0002, \"longitude\":-132.1591, \"status\":\"?\", \"testByte\":null, \"testUByte\":null, \"testLong\":null, \"testULong\":null, \"sst\":null}\n";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    /*
     * //*** getting mat
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, "", dir,
     * eddTable.className() + "_char", ".mat");
     * results = String2.annotatedString(File2.directReadFrom88591File(dir +
     * tName));
     * //String2.log(results);
     * expected =
     * "zztop";
     * Test.ensureEqual(results, expected, "results=\n" + results);
     */

    // *** display source file (useful for diagnosing problems in next section)
    String2.log(File2.directReadFrom88591File("/erddapTest/nccsv/testScalar_1.1.csv"));

    // *** getting nc and ncHeader
    edv = eddTable.findDataVariableByDestinationName("status");
    PrimitiveArray pa = edv.combinedAttributes().get("actual_range");
    String2.log("  status actual_range " + pa.elementType() + " " + pa.toString());

    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".nc");
    results = String2.annotatedString(NcHelper.ncdump(dir + tName, ""));
    // String2.log(results);
    expected =
        "netcdf EDDTableFromNccsvFiles_char.nc {[10]\n"
            + "  dimensions:[10]\n"
            + "    row = 6;[10]\n"
            + "    ship_strlen = 11;[10]\n"
            + "  variables:[10]\n"
            + "    char ship(row=6, ship_strlen=11);[10]\n"
            + "      :_Encoding = \"ISO-8859-1\";[10]\n"
            + "      :cf_role = \"trajectory_id\";[10]\n"
            + "      :ioos_category = \"Identifier\";[10]\n"
            + "      :long_name = \"Ship\";[10]\n"
            + "[10]\n"
            + "    double time(row=6);[10]\n"
            + "      :_CoordinateAxisType = \"Time\";[10]\n"
            + "      :actual_range = 1.4902299E9, 1.4903127E9; // double[10]\n"
            + "      :axis = \"T\";[10]\n"
            + "      :ioos_category = \"Time\";[10]\n"
            + "      :long_name = \"Time\";[10]\n"
            + "      :standard_name = \"time\";[10]\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";[10]\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";[10]\n"
            + "[10]\n"
            + "    double latitude(row=6);[10]\n"
            + "      :_CoordinateAxisType = \"Lat\";[10]\n"
            + "      :actual_range = 27.9998, 28.0003; // double[10]\n"
            + "      :axis = \"Y\";[10]\n"
            + "      :colorBarMaximum = 90.0; // double[10]\n"
            + "      :colorBarMinimum = -90.0; // double[10]\n"
            + "      :ioos_category = \"Location\";[10]\n"
            + "      :long_name = \"Latitude\";[10]\n"
            + "      :standard_name = \"latitude\";[10]\n"
            + "      :units = \"degrees_north\";[10]\n"
            + "[10]\n"
            + "    double longitude(row=6);[10]\n"
            + "      :_CoordinateAxisType = \"Lon\";[10]\n"
            + "      :actual_range = -132.1591, -130.2576; // double[10]\n"
            + "      :axis = \"X\";[10]\n"
            + "      :colorBarMaximum = 180.0; // double[10]\n"
            + "      :colorBarMinimum = -180.0; // double[10]\n"
            + "      :ioos_category = \"Location\";[10]\n"
            + "      :long_name = \"Longitude\";[10]\n"
            + "      :standard_name = \"longitude\";[10]\n"
            + "      :units = \"degrees_east\";[10]\n"
            + "[10]\n"
            + "    char status(row=6);[10]\n"
            + "      :actual_range = \"\\t[8364]\";[10]\n"
            +
            // " :charset = \"ISO-8859-1\";[10]\n" +
            "      :comment = \"From http://some.url.gov/someProjectDocument , Table C\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Status\";[10]\n"
            + "[10]\n"
            + "    byte testByte(row=6);[10]\n"
            + "      :_FillValue = 127B; // byte[10]\n"
            + "      :actual_range = -128B, 126B; // byte[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    byte testUByte(row=6);[10]\n"
            + "      :_Unsigned = \"true\";[10]\n"
            + "      :_FillValue = -1B; // byte[10]\n"
            + // known problem: nc3 doesn't support unsigned attributes, so this
            // ubyte att is stored as byte
            "      :actual_range = 0B, -2B; // byte[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    double testLong(row=6);[10]\n"
            + "      :_FillValue = 9.223372036854776E18; // double[10]\n"
            +
            // trouble: these are largest consecutive longs that can round trip to doubles
            "      :actual_range = -9.223372036854776E18, 9.223372036854776E18; // double[10]\n"
            + // trouble: min/max
            // should be
            // -...854775808L
            // ...854775806L
            "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Test of Longs\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    double testULong(row=6);[10]\n"
            + "      :_Unsigned = \"true\";[10]\n"
            + "      :_FillValue = 1.8446744073709552E19; // double[10]\n"
            + "      :actual_range = 0.0, 1.8446744073709552E19; // double[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Test ULong\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    float sst(row=6);[10]\n"
            + "      :actual_range = 10.0f, 10.9f; // float[10]\n"
            + "      :colorBarMaximum = 32.0; // double[10]\n"
            + "      :colorBarMinimum = 0.0; // double[10]\n"
            + "      :ioos_category = \"Temperature\";[10]\n"
            + "      :long_name = \"Sea Surface Temperature\";[10]\n"
            + "      :missing_value = 99.0f; // float[10]\n"
            + "      :standard_name = \"sea_surface_temperature\";[10]\n"
            + "      :testBytes = -128B, 0B, 127B; // byte[10]\n"
            + "      :testChars = \",\\\"[8364]\";[10]\n"
            + // [8364], so unicode!
            "      :testDoubles = -1.7976931348623157E308, 0.0, 1.7976931348623157E308; // double[10]\n"
            + "      :testFloats = -3.4028235E38f, 0.0f, 3.4028235E38f; // float[10]\n"
            + "      :testInts = -2147483648, 0, 2147483647; // int[10]\n"
            +
            // -max, then 2 of the largest consecutive longs that can round trip to doubles,
            // then max-1, and max
            "      :testLongs = -9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.223372036854776E18, 9.223372036854776E18; // double[10]\n"
            + "      :testShorts = -32768S, 0S, 32767S; // short[10]\n"
            + "      :testStrings = \" a\\t~[252],[10]\n"
            + "'z\\\"[8364]\";[10]\n"
            + // [8364], so unicode!
            "      :testUBytes = 0B, 127B, -1B; // byte[10]\n"
            + "      :testUInts = 0, 2147483647, -1; // int[10]\n"
            + "      :testULongs = 0.0, 9.223372036854776E18, 1.8446744073709552E19; // double[10]\n"
            + "      :testUShorts = 0S, 32767S, -1S; // short[10]\n"
            + "      :units = \"degree_C\";[10]\n"
            + "[10]\n"
            + "  // global attributes:[10]\n"
            + "  :cdm_data_type = \"Trajectory\";[10]\n"
            + "  :cdm_trajectory_variables = \"ship\";[10]\n"
            + "  :Conventions = \"COARDS, CF-1.10, ACDD-1.3\";[10]\n"
            + "  :creator_email = \"bob.simons@noaa.gov\";[10]\n"
            + "  :creator_name = \"Bob Simons\";[10]\n"
            + "  :creator_type = \"person\";[10]\n"
            + "  :creator_url = \"https://www.pfeg.noaa.gov\";[10]\n"
            + "  :Easternmost_Easting = -130.2576; // double[10]\n"
            + "  :featureType = \"Trajectory\";[10]\n"
            + "  :geospatial_lat_max = 28.0003; // double[10]\n"
            + "  :geospatial_lat_min = 27.9998; // double[10]\n"
            + "  :geospatial_lat_units = \"degrees_north\";[10]\n"
            + "  :geospatial_lon_max = -130.2576; // double[10]\n"
            + "  :geospatial_lon_min = -132.1591; // double[10]\n"
            + "  :geospatial_lon_units = \"degrees_east\";[10]\n"
            + "  :history = \""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // T18:32:36Z (local files)[10]\n" +
    // "2017-04-21T18:32:36Z
    expected =
        "http://127.0.0.1:8080/cwexperimental/tabledap/testNccsvScalar11.nc\";[10]\n"
            + "  :id = \"testNccsvScalar11\";[10]\n"
            + "  :infoUrl = \"https://erddap.github.io/NCCSV.html\";[10]\n"
            + "  :institution = \"NOAA NMFS SWFSC ERD, NOAA PMEL\";[10]\n"
            + "  :keywords = \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\";[10]\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";[10]\n"
            + "  :license = \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\";[10]\n"
            + "  :Northernmost_Northing = 28.0003; // double[10]\n"
            + "  :sourceUrl = \"(local files)\";[10]\n"
            + "  :Southernmost_Northing = 27.9998; // double[10]\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v55\";[10]\n"
            + "  :subsetVariables = \"ship, status, testLong\";[10]\n"
            + "  :summary = \"This is a paragraph or two describing the dataset.\";[10]\n"
            + "  :time_coverage_end = \"2017-03-23T23:45:00Z\";[10]\n"
            + "  :time_coverage_start = \"2017-03-23T00:45:00Z\";[10]\n"
            + "  :title = \"NCCSV Demonstration\";[10]\n"
            + "  :Westernmost_Easting = -132.1591; // double[10]\n"
            + "[10]\n"
            + "  data:[10]\n"
            + "    ship =   \" a[9]~[252],[10]\n"
            + // Is \n in middle of a value okay?
            "'z\"?\",   \" a[9]~[252],[10]\n"
            + "'z\"?\",   \" a[9]~[252],[10]\n"
            + "'z\"?\",   \" a[9]~[252],[10]\n"
            + "'z\"?\",   \" a[9]~[252],[10]\n"
            + "'z\"?\",   \" a[9]~[252],[10]\n"
            + "'z\"?\"[10]\n"
            + "    time = [10]\n"
            + "      {1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9}[10]\n"
            + "    latitude = [10]\n"
            + "      {28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002}[10]\n"
            + "    longitude = [10]\n"
            + "      {-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591}[10]\n"
            + "    status =   \"A?[9]\"[252]?\"[10]\n"
            + // source status chars are A\u20AC\t"\u00fc\uFFFF
            "    testByte = [10]\n"
            + "      {-128, 0, 126, 127, 127, 127}[10]\n"
            + // in .nc file, even mv must appear as a value
            "    testUByte = [10]\n"
            + "      {0, 127, -2, -1, -1, -1}[10]\n"
            + // in .nc file, even mv must appear as a value
            "    testLong = [10]\n"
            + // -9.007... is largest consecutive long that is perfectly represented in
            // double.
            "      {-9.223372036854776E18, -9.007199254740992E15, 9.223372036854776E18, NaN, NaN, NaN}[10]\n"
            + "    testULong = [10]\n"
            + "      {0.0, 9.223372036854776E18, 1.8446744073709552E19, NaN, NaN, NaN}[10]\n"
            + "    sst = [10]\n"
            + "      {10.9, 10.0, 99.0, NaN, NaN, NaN}[10]\n"
            + "}[10]\n"
            + "[end]";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** getting ncCF and ncCFHeader
    String2.log(">> getting ncCF " + eddTable.combinedGlobalAttributes().getString("Conventions"));
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".ncCF");
    results = String2.annotatedString(NcHelper.ncdump(dir + tName, ""));
    // String2.log(results);
    expected =
        "netcdf EDDTableFromNccsvFiles_char.nc {[10]\n"
            + "  dimensions:[10]\n"
            + "    trajectory = 1;[10]\n"
            + "    obs = 6;[10]\n"
            + "    ship_strlen = 11;[10]\n"
            + "  variables:[10]\n"
            + "    char ship(trajectory=1, ship_strlen=11);[10]\n"
            + "      :_Encoding = \"ISO-8859-1\";[10]\n"
            + "      :cf_role = \"trajectory_id\";[10]\n"
            + "      :ioos_category = \"Identifier\";[10]\n"
            + "      :long_name = \"Ship\";[10]\n"
            + "[10]\n"
            + "    int rowSize(trajectory=1);[10]\n"
            + "      :ioos_category = \"Identifier\";[10]\n"
            + "      :long_name = \"Number of Observations for this Trajectory\";[10]\n"
            + "      :sample_dimension = \"obs\";[10]\n"
            + "[10]\n"
            + "    double time(obs=6);[10]\n"
            + "      :_CoordinateAxisType = \"Time\";[10]\n"
            + "      :actual_range = 1.4902299E9, 1.4903127E9; // double[10]\n"
            + "      :axis = \"T\";[10]\n"
            + "      :ioos_category = \"Time\";[10]\n"
            + "      :long_name = \"Time\";[10]\n"
            + "      :standard_name = \"time\";[10]\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";[10]\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";[10]\n"
            + "[10]\n"
            + "    double latitude(obs=6);[10]\n"
            + "      :_CoordinateAxisType = \"Lat\";[10]\n"
            + "      :actual_range = 27.9998, 28.0003; // double[10]\n"
            + "      :axis = \"Y\";[10]\n"
            + "      :colorBarMaximum = 90.0; // double[10]\n"
            + "      :colorBarMinimum = -90.0; // double[10]\n"
            + "      :ioos_category = \"Location\";[10]\n"
            + "      :long_name = \"Latitude\";[10]\n"
            + "      :standard_name = \"latitude\";[10]\n"
            + "      :units = \"degrees_north\";[10]\n"
            + "[10]\n"
            + "    double longitude(obs=6);[10]\n"
            + "      :_CoordinateAxisType = \"Lon\";[10]\n"
            + "      :actual_range = -132.1591, -130.2576; // double[10]\n"
            + "      :axis = \"X\";[10]\n"
            + "      :colorBarMaximum = 180.0; // double[10]\n"
            + "      :colorBarMinimum = -180.0; // double[10]\n"
            + "      :ioos_category = \"Location\";[10]\n"
            + "      :long_name = \"Longitude\";[10]\n"
            + "      :standard_name = \"longitude\";[10]\n"
            + "      :units = \"degrees_east\";[10]\n"
            + "[10]\n"
            + "    char status(obs=6);[10]\n"
            + "      :actual_range = \"\\t[8364]\";[10]\n"
            + // [8364], so unicode!
            // " :charset = \"ISO-8859-1\";[10]\n" +
            "      :comment = \"From http://some.url.gov/someProjectDocument , Table C\";[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Status\";[10]\n"
            + "[10]\n"
            + "    byte testByte(obs=6);[10]\n"
            + "      :_FillValue = 127B; // byte[10]\n"
            + "      :actual_range = -128B, 126B; // byte[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    byte testUByte(obs=6);[10]\n"
            + "      :_Unsigned = \"true\";[10]\n"
            + "      :_FillValue = -1B; // byte[10]\n"
            + "      :actual_range = 0B, -2B; // byte[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    double testLong(obs=6);[10]\n"
            + "      :_FillValue = 9.223372036854776E18; // double[10]\n"
            + "      :actual_range = -9.223372036854776E18, 9.223372036854776E18; // double[10]\n"
            + // !!! max should be
            // -9223372036854775806L
            // : loss of precision
            "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Test of Longs\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    double testULong(obs=6);[10]\n"
            + "      :_Unsigned = \"true\";[10]\n"
            + "      :_FillValue = 1.8446744073709552E19; // double[10]\n"
            + "      :actual_range = 0.0, 1.8446744073709552E19; // double[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Test ULong\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    float sst(obs=6);[10]\n"
            + "      :actual_range = 10.0f, 10.9f; // float[10]\n"
            + "      :colorBarMaximum = 32.0; // double[10]\n"
            + "      :colorBarMinimum = 0.0; // double[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Temperature\";[10]\n"
            + "      :long_name = \"Sea Surface Temperature\";[10]\n"
            + "      :missing_value = 99.0f; // float[10]\n"
            + "      :standard_name = \"sea_surface_temperature\";[10]\n"
            + "      :testBytes = -128B, 0B, 127B; // byte[10]\n"
            + "      :testChars = \",\\\"[8364]\";[10]\n"
            + // [8364], so unicode!
            "      :testDoubles = -1.7976931348623157E308, 0.0, 1.7976931348623157E308; // double[10]\n"
            + "      :testFloats = -3.4028235E38f, 0.0f, 3.4028235E38f; // float[10]\n"
            + "      :testInts = -2147483648, 0, 2147483647; // int[10]\n"
            +
            // +/-9.007... are the largest consecutive longs that can round trip to doubles
            "      :testLongs = -9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.223372036854776E18, 9.223372036854776E18; // double[10]\n"
            + "      :testShorts = -32768S, 0S, 32767S; // short[10]\n"
            + "      :testStrings = \" a\\t~[252],[10]\n"
            + "'z\\\"[8364]\";[10]\n"
            + // [8364], so unicode!
            "      :testUBytes = 0B, 127B, -1B; // byte[10]\n"
            + "      :testUInts = 0, 2147483647, -1; // int[10]\n"
            + "      :testULongs = 0.0, 9.223372036854776E18, 1.8446744073709552E19; // double[10]\n"
            + "      :testUShorts = 0S, 32767S, -1S; // short[10]\n"
            + "      :units = \"degree_C\";[10]\n"
            + "[10]\n"
            + "  // global attributes:[10]\n"
            + "  :cdm_data_type = \"Trajectory\";[10]\n"
            + "  :cdm_trajectory_variables = \"ship\";[10]\n"
            + "  :Conventions = \"COARDS, CF-1.10, ACDD-1.3\";[10]\n"
            + "  :creator_email = \"bob.simons@noaa.gov\";[10]\n"
            + "  :creator_name = \"Bob Simons\";[10]\n"
            + "  :creator_type = \"person\";[10]\n"
            + "  :creator_url = \"https://www.pfeg.noaa.gov\";[10]\n"
            + "  :Easternmost_Easting = -130.2576; // double[10]\n"
            + "  :featureType = \"Trajectory\";[10]\n"
            + "  :geospatial_lat_max = 28.0003; // double[10]\n"
            + "  :geospatial_lat_min = 27.9998; // double[10]\n"
            + "  :geospatial_lat_units = \"degrees_north\";[10]\n"
            + "  :geospatial_lon_max = -130.2576; // double[10]\n"
            + "  :geospatial_lon_min = -132.1591; // double[10]\n"
            + "  :geospatial_lon_units = \"degrees_east\";[10]\n"
            + "  :history = \""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        "http://127.0.0.1:8080/cwexperimental/tabledap/testNccsvScalar11.ncCF\";[10]\n"
            + "  :id = \"testNccsvScalar11\";[10]\n"
            + "  :infoUrl = \"https://erddap.github.io/NCCSV.html\";[10]\n"
            + "  :institution = \"NOAA NMFS SWFSC ERD, NOAA PMEL\";[10]\n"
            + "  :keywords = \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\";[10]\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";[10]\n"
            + "  :license = \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\";[10]\n"
            + "  :Northernmost_Northing = 28.0003; // double[10]\n"
            + "  :sourceUrl = \"(local files)\";[10]\n"
            + "  :Southernmost_Northing = 27.9998; // double[10]\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v55\";[10]\n"
            + "  :subsetVariables = \"ship, status, testLong\";[10]\n"
            + "  :summary = \"This is a paragraph or two describing the dataset.\";[10]\n"
            + "  :time_coverage_end = \"2017-03-23T23:45:00Z\";[10]\n"
            + "  :time_coverage_start = \"2017-03-23T00:45:00Z\";[10]\n"
            + "  :title = \"NCCSV Demonstration\";[10]\n"
            + "  :Westernmost_Easting = -132.1591; // double[10]\n"
            + "[10]\n"
            + "  data:[10]\n"
            + "    ship =   \" a[9]~[252],[10]\n"
            + // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
            "'z\"?\"[10]\n"
            + "    rowSize = [10]\n"
            + "      {6}[10]\n"
            + "    time = [10]\n"
            + "      {1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9}[10]\n"
            + "    latitude = [10]\n"
            + "      {28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002}[10]\n"
            + "    longitude = [10]\n"
            + "      {-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591}[10]\n"
            + "    status =   \"A?[9]\"[252]?\"[10]\n"
            + // source status chars are A\u20AC\t"\u00fc\uFFFF
            "    testByte = [10]\n"
            + "      {-128, 0, 126, 127, 127, 127}[10]\n"
            + "    testUByte = [10]\n"
            + "      {0, 127, -2, -1, -1, -1}[10]\n"
            + "    testLong = [10]\n"
            + // -9.007... is largest long precisely held in double
            "      {-9.223372036854776E18, -9.007199254740992E15, 9.223372036854776E18, NaN, NaN, NaN}[10]\n"
            + "    testULong = [10]\n"
            + "      {0.0, 9.223372036854776E18, 1.8446744073709552E19, NaN, NaN, NaN}[10]\n"
            + "    sst = [10]\n"
            + "      {10.9, 10.0, 99.0, NaN, NaN, NaN}[10]\n"
            + "}[10]\n"
            + "[end]";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** getting ncCFMA and ncCFMAHeader
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".ncCFMA");
    results = String2.annotatedString(NcHelper.ncdump(dir + tName, ""));
    // String2.log(results);
    expected =
        "netcdf EDDTableFromNccsvFiles_char.nc {[10]\n"
            + "  dimensions:[10]\n"
            + "    trajectory = 1;[10]\n"
            + "    obs = 6;[10]\n"
            + "    ship_strlen = 11;[10]\n"
            + "  variables:[10]\n"
            + "    char ship(trajectory=1, ship_strlen=11);[10]\n"
            + "      :_Encoding = \"ISO-8859-1\";[10]\n"
            + "      :cf_role = \"trajectory_id\";[10]\n"
            + "      :ioos_category = \"Identifier\";[10]\n"
            + "      :long_name = \"Ship\";[10]\n"
            + "[10]\n"
            + "    double time(trajectory=1, obs=6);[10]\n"
            + "      :_CoordinateAxisType = \"Time\";[10]\n"
            + "      :_FillValue = NaN; // double[10]\n"
            + "      :actual_range = 1.4902299E9, 1.4903127E9; // double[10]\n"
            + "      :axis = \"T\";[10]\n"
            + "      :ioos_category = \"Time\";[10]\n"
            + "      :long_name = \"Time\";[10]\n"
            + "      :standard_name = \"time\";[10]\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";[10]\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";[10]\n"
            + "[10]\n"
            + "    double latitude(trajectory=1, obs=6);[10]\n"
            + "      :_CoordinateAxisType = \"Lat\";[10]\n"
            + "      :_FillValue = NaN; // double[10]\n"
            + "      :actual_range = 27.9998, 28.0003; // double[10]\n"
            + "      :axis = \"Y\";[10]\n"
            + "      :colorBarMaximum = 90.0; // double[10]\n"
            + "      :colorBarMinimum = -90.0; // double[10]\n"
            + "      :ioos_category = \"Location\";[10]\n"
            + "      :long_name = \"Latitude\";[10]\n"
            + "      :standard_name = \"latitude\";[10]\n"
            + "      :units = \"degrees_north\";[10]\n"
            + "[10]\n"
            + "    double longitude(trajectory=1, obs=6);[10]\n"
            + "      :_CoordinateAxisType = \"Lon\";[10]\n"
            + "      :_FillValue = NaN; // double[10]\n"
            + "      :actual_range = -132.1591, -130.2576; // double[10]\n"
            + "      :axis = \"X\";[10]\n"
            + "      :colorBarMaximum = 180.0; // double[10]\n"
            + "      :colorBarMinimum = -180.0; // double[10]\n"
            + "      :ioos_category = \"Location\";[10]\n"
            + "      :long_name = \"Longitude\";[10]\n"
            + "      :standard_name = \"longitude\";[10]\n"
            + "      :units = \"degrees_east\";[10]\n"
            + "[10]\n"
            + "    char status(trajectory=1, obs=6);[10]\n"
            + "      :_FillValue = \"[65535]\";[10]\n"
            + "      :actual_range = \"\\t[8364]\";[10]\n"
            +
            // " :charset = \"ISO-8859-1\";[10]\n" +
            "      :comment = \"From http://some.url.gov/someProjectDocument , Table C\";[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Status\";[10]\n"
            + "[10]\n"
            + "    byte testByte(trajectory=1, obs=6);[10]\n"
            + "      :_FillValue = 127B; // byte[10]\n"
            + "      :actual_range = -128B, 126B; // byte[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    byte testUByte(trajectory=1, obs=6);[10]\n"
            + "      :_Unsigned = \"true\";[10]\n"
            + "      :_FillValue = -1B; // byte[10]\n"
            + "      :actual_range = 0B, -2B; // byte[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    double testLong(trajectory=1, obs=6);[10]\n"
            + "      :_FillValue = 9.223372036854776E18; // double[10]\n"
            + "      :actual_range = -9.223372036854776E18, 9.223372036854776E18; // double[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Test of Longs\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    double testULong(trajectory=1, obs=6);[10]\n"
            + "      :_Unsigned = \"true\";[10]\n"
            + "      :_FillValue = 1.8446744073709552E19; // double[10]\n"
            + "      :actual_range = 0.0, 1.8446744073709552E19; // double[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Unknown\";[10]\n"
            + "      :long_name = \"Test ULong\";[10]\n"
            + "      :units = \"1\";[10]\n"
            + "[10]\n"
            + "    float sst(trajectory=1, obs=6);[10]\n"
            + "      :actual_range = 10.0f, 10.9f; // float[10]\n"
            + "      :colorBarMaximum = 32.0; // double[10]\n"
            + "      :colorBarMinimum = 0.0; // double[10]\n"
            + "      :coordinates = \"time latitude longitude\";[10]\n"
            + "      :ioos_category = \"Temperature\";[10]\n"
            + "      :long_name = \"Sea Surface Temperature\";[10]\n"
            + "      :missing_value = 99.0f; // float[10]\n"
            + "      :standard_name = \"sea_surface_temperature\";[10]\n"
            + "      :testBytes = -128B, 0B, 127B; // byte[10]\n"
            + "      :testChars = \",\\\"[8364]\";[10]\n"
            + // so unicode!
            "      :testDoubles = -1.7976931348623157E308, 0.0, 1.7976931348623157E308; // double[10]\n"
            + "      :testFloats = -3.4028235E38f, 0.0f, 3.4028235E38f; // float[10]\n"
            + "      :testInts = -2147483648, 0, 2147483647; // int[10]\n"
            +
            // these are largest longs that can round trip to double
            "      :testLongs = -9.223372036854776E18, -9.007199254740992E15, 9.007199254740992E15, 9.223372036854776E18, 9.223372036854776E18; // double[10]\n"
            + "      :testShorts = -32768S, 0S, 32767S; // short[10]\n"
            + "      :testStrings = \" a\\t~[252],[10]\n"
            + "'z\\\"[8364]\";[10]\n"
            + // [8364], so unicode!
            "      :testUBytes = 0B, 127B, -1B; // byte[10]\n"
            + "      :testUInts = 0, 2147483647, -1; // int[10]\n"
            + "      :testULongs = 0.0, 9.223372036854776E18, 1.8446744073709552E19; // double[10]\n"
            + "      :testUShorts = 0S, 32767S, -1S; // short[10]\n"
            + "      :units = \"degree_C\";[10]\n"
            + "[10]\n"
            + "  // global attributes:[10]\n"
            + "  :cdm_data_type = \"Trajectory\";[10]\n"
            + "  :cdm_trajectory_variables = \"ship\";[10]\n"
            + "  :Conventions = \"COARDS, CF-1.10, ACDD-1.3\";[10]\n"
            + "  :creator_email = \"bob.simons@noaa.gov\";[10]\n"
            + "  :creator_name = \"Bob Simons\";[10]\n"
            + "  :creator_type = \"person\";[10]\n"
            + "  :creator_url = \"https://www.pfeg.noaa.gov\";[10]\n"
            + "  :Easternmost_Easting = -130.2576; // double[10]\n"
            + "  :featureType = \"Trajectory\";[10]\n"
            + "  :geospatial_lat_max = 28.0003; // double[10]\n"
            + "  :geospatial_lat_min = 27.9998; // double[10]\n"
            + "  :geospatial_lat_units = \"degrees_north\";[10]\n"
            + "  :geospatial_lon_max = -130.2576; // double[10]\n"
            + "  :geospatial_lon_min = -132.1591; // double[10]\n"
            + "  :geospatial_lon_units = \"degrees_east\";[10]\n"
            + "  :history = \""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        "http://127.0.0.1:8080/cwexperimental/tabledap/testNccsvScalar11.ncCFMA\";[10]\n"
            + "  :id = \"testNccsvScalar11\";[10]\n"
            + "  :infoUrl = \"https://erddap.github.io/NCCSV.html\";[10]\n"
            + "  :institution = \"NOAA NMFS SWFSC ERD, NOAA PMEL\";[10]\n"
            + "  :keywords = \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\";[10]\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";[10]\n"
            + "  :license = \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\";[10]\n"
            + "  :Northernmost_Northing = 28.0003; // double[10]\n"
            + "  :sourceUrl = \"(local files)\";[10]\n"
            + "  :Southernmost_Northing = 27.9998; // double[10]\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v55\";[10]\n"
            + "  :subsetVariables = \"ship, status, testLong\";[10]\n"
            + "  :summary = \"This is a paragraph or two describing the dataset.\";[10]\n"
            + "  :time_coverage_end = \"2017-03-23T23:45:00Z\";[10]\n"
            + "  :time_coverage_start = \"2017-03-23T00:45:00Z\";[10]\n"
            + "  :title = \"NCCSV Demonstration\";[10]\n"
            + "  :Westernmost_Easting = -132.1591; // double[10]\n"
            + "[10]\n"
            + "  data:[10]\n"
            + "    ship =   \" a[9]~[252],[10]\n"
            + "'z\"?\"[10]\n"
            + "    time = [10]\n"
            + "      {[10]\n"
            + "        {1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9}[10]\n"
            + "      }[10]\n"
            + "    latitude = [10]\n"
            + "      {[10]\n"
            + "        {28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002}[10]\n"
            + "      }[10]\n"
            + "    longitude = [10]\n"
            + "      {[10]\n"
            + "        {-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591}[10]\n"
            + "      }[10]\n"
            + "    status =   \"A?[9]\"[252]?\"[10]\n"
            + "    testByte = [10]\n"
            + "      {[10]\n"
            + "        {-128, 0, 126, 127, 127, 127}[10]\n"
            + "      }[10]\n"
            + "    testUByte = [10]\n"
            + "      {[10]\n"
            + "        {0, 127, -2, -1, -1, -1}[10]\n"
            + "      }[10]\n"
            + "    testLong = [10]\n"
            + "      {[10]\n"
            + "        {-9.223372036854776E18, -9.007199254740992E15, 9.223372036854776E18, NaN, NaN, NaN}[10]\n"
            + "      }[10]\n"
            + "    testULong = [10]\n"
            + "      {[10]\n"
            + "        {0.0, 9.223372036854776E18, 1.8446744073709552E19, NaN, NaN, NaN}[10]\n"
            + "      }[10]\n"
            + "    sst = [10]\n"
            + "      {[10]\n"
            + "        {10.9, 10.0, 99.0, NaN, NaN, NaN}[10]\n"
            + "      }[10]\n"
            + "}[10]\n"
            + "[end]";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** getting ncoJson
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".ncoJson");
    results = String2.annotatedString(File2.directReadFromUtf8File(dir + tName));
    // 2017-08-03 I tested the resulting file for validity at https://jsonlint.com/
    String2.log(">> NCO JSON " + dir + tName);
    // String2.log(results);
    expected =
        "{[10]\n"
            + "  \"attributes\": {[10]\n"
            + "    \"cdm_data_type\": {\"type\": \"char\", \"data\": \"Trajectory\"},[10]\n"
            + "    \"cdm_trajectory_variables\": {\"type\": \"char\", \"data\": \"ship\"},[10]\n"
            + "    \"Conventions\": {\"type\": \"char\", \"data\": \"COARDS, CF-1.10, ACDD-1.3\"},[10]\n"
            + "    \"creator_email\": {\"type\": \"char\", \"data\": \"bob.simons@noaa.gov\"},[10]\n"
            + "    \"creator_name\": {\"type\": \"char\", \"data\": \"Bob Simons\"},[10]\n"
            + "    \"creator_type\": {\"type\": \"char\", \"data\": \"person\"},[10]\n"
            + "    \"creator_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},[10]\n"
            + "    \"Easternmost_Easting\": {\"type\": \"double\", \"data\": -130.2576},[10]\n"
            + "    \"featureType\": {\"type\": \"char\", \"data\": \"Trajectory\"},[10]\n"
            + "    \"geospatial_lat_max\": {\"type\": \"double\", \"data\": 28.0003},[10]\n"
            + "    \"geospatial_lat_min\": {\"type\": \"double\", \"data\": 27.9998},[10]\n"
            + "    \"geospatial_lat_units\": {\"type\": \"char\", \"data\": \"degrees_north\"},[10]\n"
            + "    \"geospatial_lon_max\": {\"type\": \"double\", \"data\": -130.2576},[10]\n"
            + "    \"geospatial_lon_min\": {\"type\": \"double\", \"data\": -132.1591},[10]\n"
            + "    \"geospatial_lon_units\": {\"type\": \"char\", \"data\": \"degrees_east\"},[10]\n"
            + "    \"history\": {\"type\": \"char\", \"data\": \"";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // 2017-07-28T15:33:25Z (local files)\\n2017-07-28T15:33:25Z
    expected =
        "http://127.0.0.1:8080/cwexperimental/tabledap/testNccsvScalar11.ncoJson\"},[10]\n"
            + "    \"infoUrl\": {\"type\": \"char\", \"data\": \"https://erddap.github.io/NCCSV.html\"},[10]\n"
            + "    \"institution\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD, NOAA PMEL\"},[10]\n"
            + "    \"keywords\": {\"type\": \"char\", \"data\": \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"},[10]\n"
            + "    \"keywords_vocabulary\": {\"type\": \"char\", \"data\": \"GCMD Science Keywords\"},[10]\n"
            + "    \"license\": {\"type\": \"char\", \"data\": \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"},[10]\n"
            + "    \"Northernmost_Northing\": {\"type\": \"double\", \"data\": 28.0003},[10]\n"
            + "    \"sourceUrl\": {\"type\": \"char\", \"data\": \"(local files)\"},[10]\n"
            + "    \"Southernmost_Northing\": {\"type\": \"double\", \"data\": 27.9998},[10]\n"
            + "    \"standard_name_vocabulary\": {\"type\": \"char\", \"data\": \"CF Standard Name Table v55\"},[10]\n"
            + "    \"subsetVariables\": {\"type\": \"char\", \"data\": \"ship, status, testLong\"},[10]\n"
            + "    \"summary\": {\"type\": \"char\", \"data\": \"This is a paragraph or two describing the dataset.\"},[10]\n"
            + "    \"time_coverage_end\": {\"type\": \"char\", \"data\": \"2017-03-23T23:45:00Z\"},[10]\n"
            + "    \"time_coverage_start\": {\"type\": \"char\", \"data\": \"2017-03-23T00:45:00Z\"},[10]\n"
            + "    \"title\": {\"type\": \"char\", \"data\": \"NCCSV Demonstration\"},[10]\n"
            + "    \"Westernmost_Easting\": {\"type\": \"double\", \"data\": -132.1591}[10]\n"
            + "  },[10]\n"
            + "  \"dimensions\": {[10]\n"
            + "    \"row\": 6,[10]\n"
            + "    \"ship_strlen\": 11[10]\n"
            + "  },[10]\n"
            + "  \"variables\": {[10]\n"
            + "    \"ship\": {[10]\n"
            + "      \"shape\": [\"row\", \"ship_strlen\"],[10]\n"
            + "      \"type\": \"char\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"cf_role\": {\"type\": \"char\", \"data\": \"trajectory_id\"},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Identifier\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Ship\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"]][10]\n"
            + "    },[10]\n"
            + "    \"time\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"double\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [1.4902299E9, 1.4903127E9]},[10]\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"T\"},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"time\"},[10]\n"
            + "        \"time_origin\": {\"type\": \"char\", \"data\": \"01-JAN-1970 00:00:00\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"seconds since 1970-01-01T00:00:00Z\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9][10]\n"
            + "    },[10]\n"
            + "    \"latitude\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"double\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lat\"},[10]\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [27.9998, 28.0003]},[10]\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"Y\"},[10]\n"
            + "        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 90.0},[10]\n"
            + "        \"colorBarMinimum\": {\"type\": \"double\", \"data\": -90.0},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Latitude\"},[10]\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"latitude\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_north\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002][10]\n"
            + "    },[10]\n"
            + "    \"longitude\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"double\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lon\"},[10]\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [-132.1591, -130.2576]},[10]\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"X\"},[10]\n"
            + "        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 180.0},[10]\n"
            + "        \"colorBarMinimum\": {\"type\": \"double\", \"data\": -180.0},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Longitude\"},[10]\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"longitude\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_east\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591][10]\n"
            + "    },[10]\n"
            + "    \"status\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"char\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"actual_range\": {\"type\": \"char\", \"data\": \"\\t\\n\\u20ac\"},[10]\n"
            + "        \"comment\": {\"type\": \"char\", \"data\": \"From http://some.url.gov/someProjectDocument , Table C\"},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Status\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [\"A\\u20ac\\t\\\"\\u00fc?\"][10]\n"
            + "    },[10]\n"
            + "    \"testByte\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"byte\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_FillValue\": {\"type\": \"byte\", \"data\": 127},[10]\n"
            + "        \"actual_range\": {\"type\": \"byte\", \"data\": [-128, 126]},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [-128, 0, 126, 127, 127, 127][10]\n"
            + // 127 or null? I say 127 because that is what you will
            // se in .nc file
            "    },[10]\n"
            + "    \"testUByte\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"ubyte\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_FillValue\": {\"type\": \"ubyte\", \"data\": 255},[10]\n"
            + "        \"actual_range\": {\"type\": \"ubyte\", \"data\": [0, 254]},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [0, 127, 254, 255, 255, 255][10]\n"
            + "    },[10]\n"
            + "    \"testLong\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"int64\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_FillValue\": {\"type\": \"int64\", \"data\": 9223372036854775807},[10]\n"
            + "        \"actual_range\": {\"type\": \"int64\", \"data\": [-9223372036854775808, 9223372036854775806]},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Test of Longs\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [-9223372036854775808, -9007199254740992, 9223372036854775806, 9223372036854775807, 9223372036854775807, 9223372036854775807][10]\n"
            + "    },[10]\n"
            + "    \"testULong\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"uint64\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_FillValue\": {\"type\": \"uint64\", \"data\": 18446744073709551615},[10]\n"
            + "        \"actual_range\": {\"type\": \"uint64\", \"data\": [0, 18446744073709551614]},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Test ULong\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [0, 9223372036854775807, 18446744073709551614, 18446744073709551615, 18446744073709551615, 18446744073709551615][10]\n"
            + "    },[10]\n"
            + "    \"sst\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"float\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"actual_range\": {\"type\": \"float\", \"data\": [10.0, 10.9]},[10]\n"
            + "        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 32.0},[10]\n"
            + "        \"colorBarMinimum\": {\"type\": \"double\", \"data\": 0.0},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Temperature\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Sea Surface Temperature\"},[10]\n"
            + "        \"missing_value\": {\"type\": \"float\", \"data\": 99.0},[10]\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"sea_surface_temperature\"},[10]\n"
            + "        \"testBytes\": {\"type\": \"byte\", \"data\": [-128, 0, 127]},[10]\n"
            + "        \"testChars\": {\"type\": \"char\", \"data\": \",\\n\\\"\\n\\u20ac\"},[10]\n"
            + "        \"testDoubles\": {\"type\": \"double\", \"data\": [-1.7976931348623157E308, 0.0, 1.7976931348623157E308]},[10]\n"
            + "        \"testFloats\": {\"type\": \"float\", \"data\": [-3.4028235E38, 0.0, 3.4028235E38]},[10]\n"
            + "        \"testInts\": {\"type\": \"int\", \"data\": [-2147483648, 0, 2147483647]},[10]\n"
            + "        \"testLongs\": {\"type\": \"int64\", \"data\": [-9223372036854775808, -9007199254740992, 9007199254740992, 9223372036854775806, 9223372036854775807]},[10]\n"
            + "        \"testShorts\": {\"type\": \"short\", \"data\": [-32768, 0, 32767]},[10]\n"
            + "        \"testStrings\": {\"type\": \"char\", \"data\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"},[10]\n"
            + "        \"testUBytes\": {\"type\": \"ubyte\", \"data\": [0, 127, 255]},[10]\n"
            + "        \"testUInts\": {\"type\": \"uint\", \"data\": [0, 2147483647, 4294967295]},[10]\n"
            + "        \"testULongs\": {\"type\": \"uint64\", \"data\": [0, 9223372036854775807, 18446744073709551615]},[10]\n"
            + "        \"testUShorts\": {\"type\": \"ushort\", \"data\": [0, 32767, 65535]},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degree_C\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [10.9, 10.0, 99.0, null, null, null][10]\n"
            + "    }[10]\n"
            + "  }[10]\n"
            + "}[10]\n"
            + "[end]";
    po = results.indexOf(expected.substring(0, 20));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

    // *** getting ncoJson with jsonp
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&.jsonp=myFunctionName",
            dir,
            eddTable.className() + "_charjp",
            ".ncoJson");
    results = String2.annotatedString(File2.directReadFromFile(dir + tName, File2.UTF_8));
    // String2.log(results);
    expected =
        "myFunctionName("
            + "{[10]\n"
            + "  \"attributes\": {[10]\n"
            + "    \"cdm_data_type\": {\"type\": \"char\", \"data\": \"Trajectory\"},[10]\n"
            + "    \"cdm_trajectory_variables\": {\"type\": \"char\", \"data\": \"ship\"},[10]\n"
            + "    \"Conventions\": {\"type\": \"char\", \"data\": \"COARDS, CF-1.10, ACDD-1.3\"},[10]\n"
            + "    \"creator_email\": {\"type\": \"char\", \"data\": \"bob.simons@noaa.gov\"},[10]\n"
            + "    \"creator_name\": {\"type\": \"char\", \"data\": \"Bob Simons\"},[10]\n"
            + "    \"creator_type\": {\"type\": \"char\", \"data\": \"person\"},[10]\n"
            + "    \"creator_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},[10]\n"
            + "    \"Easternmost_Easting\": {\"type\": \"double\", \"data\": -130.2576},[10]\n"
            + "    \"featureType\": {\"type\": \"char\", \"data\": \"Trajectory\"},[10]\n"
            + "    \"geospatial_lat_max\": {\"type\": \"double\", \"data\": 28.0003},[10]\n"
            + "    \"geospatial_lat_min\": {\"type\": \"double\", \"data\": 27.9998},[10]\n"
            + "    \"geospatial_lat_units\": {\"type\": \"char\", \"data\": \"degrees_north\"},[10]\n"
            + "    \"geospatial_lon_max\": {\"type\": \"double\", \"data\": -130.2576},[10]\n"
            + "    \"geospatial_lon_min\": {\"type\": \"double\", \"data\": -132.1591},[10]\n"
            + "    \"geospatial_lon_units\": {\"type\": \"char\", \"data\": \"degrees_east\"},[10]\n"
            + "    \"history\": {\"type\": \"char\", \"data\": \"";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // 2017-07-28T15:33:25Z (local files)\\n2017-07-28T15:33:25Z
    expected =
        "http://127.0.0.1:8080/cwexperimental/tabledap/testNccsvScalar11.ncoJson?&.jsonp=myFunctionName\"},[10]\n"
            + "    \"infoUrl\": {\"type\": \"char\", \"data\": \"https://erddap.github.io/NCCSV.html\"},[10]\n"
            + "    \"institution\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD, NOAA PMEL\"},[10]\n"
            + "    \"keywords\": {\"type\": \"char\", \"data\": \"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"},[10]\n"
            + "    \"keywords_vocabulary\": {\"type\": \"char\", \"data\": \"GCMD Science Keywords\"},[10]\n"
            + "    \"license\": {\"type\": \"char\", \"data\": \"\\\"NCCSV Demonstration\\\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"},[10]\n"
            + "    \"Northernmost_Northing\": {\"type\": \"double\", \"data\": 28.0003},[10]\n"
            + "    \"sourceUrl\": {\"type\": \"char\", \"data\": \"(local files)\"},[10]\n"
            + "    \"Southernmost_Northing\": {\"type\": \"double\", \"data\": 27.9998},[10]\n"
            + "    \"standard_name_vocabulary\": {\"type\": \"char\", \"data\": \"CF Standard Name Table v55\"},[10]\n"
            + "    \"subsetVariables\": {\"type\": \"char\", \"data\": \"ship, status, testLong\"},[10]\n"
            + "    \"summary\": {\"type\": \"char\", \"data\": \"This is a paragraph or two describing the dataset.\"},[10]\n"
            + "    \"time_coverage_end\": {\"type\": \"char\", \"data\": \"2017-03-23T23:45:00Z\"},[10]\n"
            + "    \"time_coverage_start\": {\"type\": \"char\", \"data\": \"2017-03-23T00:45:00Z\"},[10]\n"
            + "    \"title\": {\"type\": \"char\", \"data\": \"NCCSV Demonstration\"},[10]\n"
            + "    \"Westernmost_Easting\": {\"type\": \"double\", \"data\": -132.1591}[10]\n"
            + "  },[10]\n"
            + "  \"dimensions\": {[10]\n"
            + "    \"row\": 6,[10]\n"
            + "    \"ship_strlen\": 11[10]\n"
            + "  },[10]\n"
            + "  \"variables\": {[10]\n"
            + "    \"ship\": {[10]\n"
            + "      \"shape\": [\"row\", \"ship_strlen\"],[10]\n"
            + "      \"type\": \"char\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"cf_role\": {\"type\": \"char\", \"data\": \"trajectory_id\"},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Identifier\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Ship\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [[\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"], [\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"]][10]\n"
            + "    },[10]\n"
            + "    \"time\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"double\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [1.4902299E9, 1.4903127E9]},[10]\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"T\"},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Time\"},[10]\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"time\"},[10]\n"
            + "        \"time_origin\": {\"type\": \"char\", \"data\": \"01-JAN-1970 00:00:00\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"seconds since 1970-01-01T00:00:00Z\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [1.4902299E9, 1.4902335E9, 1.4902371E9, 1.4902731E9, 1.4903055E9, 1.4903127E9][10]\n"
            + "    },[10]\n"
            + "    \"latitude\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"double\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lat\"},[10]\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [27.9998, 28.0003]},[10]\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"Y\"},[10]\n"
            + "        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 90.0},[10]\n"
            + "        \"colorBarMinimum\": {\"type\": \"double\", \"data\": -90.0},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Latitude\"},[10]\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"latitude\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_north\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [28.0002, 28.0003, 28.0001, 27.9998, 28.0003, 28.0002][10]\n"
            + "    },[10]\n"
            + "    \"longitude\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"double\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lon\"},[10]\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [-132.1591, -130.2576]},[10]\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"X\"},[10]\n"
            + "        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 180.0},[10]\n"
            + "        \"colorBarMinimum\": {\"type\": \"double\", \"data\": -180.0},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Longitude\"},[10]\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"longitude\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_east\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [-130.2576, -130.3472, -130.4305, -131.5578, -132.0014, -132.1591][10]\n"
            + "    },[10]\n"
            + "    \"status\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"char\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"actual_range\": {\"type\": \"char\", \"data\": \"\\t\\n\\u20ac\"},[10]\n"
            + "        \"comment\": {\"type\": \"char\", \"data\": \"From http://some.url.gov/someProjectDocument , Table C\"},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Status\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [\"A\\u20ac\\t\\\"\\u00fc?\"][10]\n"
            + "    },[10]\n"
            + "    \"testByte\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"byte\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_FillValue\": {\"type\": \"byte\", \"data\": 127},[10]\n"
            + "        \"actual_range\": {\"type\": \"byte\", \"data\": [-128, 126]},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [-128, 0, 126, 127, 127, 127][10]\n"
            + "    },[10]\n"
            + "    \"testUByte\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"ubyte\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_FillValue\": {\"type\": \"ubyte\", \"data\": 255},[10]\n"
            + "        \"actual_range\": {\"type\": \"ubyte\", \"data\": [0, 254]},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [0, 127, 254, 255, 255, 255][10]\n"
            + "    },[10]\n"
            + "    \"testLong\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"int64\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_FillValue\": {\"type\": \"int64\", \"data\": 9223372036854775807},[10]\n"
            + "        \"actual_range\": {\"type\": \"int64\", \"data\": [-9223372036854775808, 9223372036854775806]},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Test of Longs\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [-9223372036854775808, -9007199254740992, 9223372036854775806, 9223372036854775807, 9223372036854775807, 9223372036854775807][10]\n"
            + "    },[10]\n"
            + "    \"testULong\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"uint64\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"_FillValue\": {\"type\": \"uint64\", \"data\": 18446744073709551615},[10]\n"
            + "        \"actual_range\": {\"type\": \"uint64\", \"data\": [0, 18446744073709551614]},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Unknown\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Test ULong\"},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"1\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [0, 9223372036854775807, 18446744073709551614, 18446744073709551615, 18446744073709551615, 18446744073709551615][10]\n"
            + "    },[10]\n"
            + "    \"sst\": {[10]\n"
            + "      \"shape\": [\"row\"],[10]\n"
            + "      \"type\": \"float\",[10]\n"
            + "      \"attributes\": {[10]\n"
            + "        \"actual_range\": {\"type\": \"float\", \"data\": [10.0, 10.9]},[10]\n"
            + "        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 32.0},[10]\n"
            + "        \"colorBarMinimum\": {\"type\": \"double\", \"data\": 0.0},[10]\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Temperature\"},[10]\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Sea Surface Temperature\"},[10]\n"
            + "        \"missing_value\": {\"type\": \"float\", \"data\": 99.0},[10]\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"sea_surface_temperature\"},[10]\n"
            + "        \"testBytes\": {\"type\": \"byte\", \"data\": [-128, 0, 127]},[10]\n"
            + "        \"testChars\": {\"type\": \"char\", \"data\": \",\\n\\\"\\n\\u20ac\"},[10]\n"
            + "        \"testDoubles\": {\"type\": \"double\", \"data\": [-1.7976931348623157E308, 0.0, 1.7976931348623157E308]},[10]\n"
            + "        \"testFloats\": {\"type\": \"float\", \"data\": [-3.4028235E38, 0.0, 3.4028235E38]},[10]\n"
            + "        \"testInts\": {\"type\": \"int\", \"data\": [-2147483648, 0, 2147483647]},[10]\n"
            + "        \"testLongs\": {\"type\": \"int64\", \"data\": [-9223372036854775808, -9007199254740992, 9007199254740992, 9223372036854775806, 9223372036854775807]},[10]\n"
            + "        \"testShorts\": {\"type\": \"short\", \"data\": [-32768, 0, 32767]},[10]\n"
            + "        \"testStrings\": {\"type\": \"char\", \"data\": \" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"},[10]\n"
            + "        \"testUBytes\": {\"type\": \"ubyte\", \"data\": [0, 127, 255]},[10]\n"
            + "        \"testUInts\": {\"type\": \"uint\", \"data\": [0, 2147483647, 4294967295]},[10]\n"
            + "        \"testULongs\": {\"type\": \"uint64\", \"data\": [0, 9223372036854775807, 18446744073709551615]},[10]\n"
            + "        \"testUShorts\": {\"type\": \"ushort\", \"data\": [0, 32767, 65535]},[10]\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degree_C\"}[10]\n"
            + "      },[10]\n"
            + "      \"data\": [10.9, 10.0, 99.0, null, null, null][10]\n"
            + "    }[10]\n"
            + "  }[10]\n"
            + "}[10]\n"
            + ")[end]";
    po = results.indexOf(expected.substring(0, 20));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

    // *** getting odvTxt
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_charODV", ".odvTxt");
    String2.log(">> odv file=" + dir + tName);
    results = String2.annotatedString(File2.directReadFromUtf8File(dir + tName)); // Utf8
    results =
        results.replaceAll(
            "<CreateTime>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}",
            "<CreateTime>9999-99-99T99:99:99");
    // String2.log(results);
    expected =
        "//<Creator>https://erddap.github.io/NCCSV.html</Creator>[10]\n"
            + "//<CreateTime>9999-99-99T99:99:99</CreateTime>[10]\n"
            + "//<Encoding>UTF-8</Encoding>[10]\n"
            + "//<Software>ERDDAP - Version "
            + EDStatic.erddapVersion
            + "</Software>[10]\n"
            + "//<Source>https://127.0.0.1:8443/cwexperimental/tabledap/testNccsvScalar11.html</Source>[10]\n"
            + "//<Version>ODV Spreadsheet V4.6</Version>[10]\n"
            + "//<DataField>GeneralField</DataField>[10]\n"
            + "//<DataType>Trajectories</DataType>[10]\n"
            + "//<MetaVariable>label=\"Cruise\" value_type=\"INDEXED_TEXT\" is_primary_variable=\"F\" comment=\"Ship\" </MetaVariable>[10]\n"
            + "//<MetaVariable>label=\"Station\" value_type=\"INDEXED_TEXT\" is_primary_variable=\"F\" </MetaVariable>[10]\n"
            + "//<MetaVariable>label=\"Type\" value_type=\"TEXT:2\" is_primary_variable=\"F\" </MetaVariable>[10]\n"
            + "//<MetaVariable>label=\"yyyy-mm-ddThh:mm:ss.sss\" value_type=\"DOUBLE\" is_primary_variable=\"F\" comment=\"Time\" </MetaVariable>[10]\n"
            + "//<MetaVariable>label=\"Longitude [degrees_east]\" value_type=\"DOUBLE\" is_primary_variable=\"F\" comment=\"Longitude\" </MetaVariable>[10]\n"
            + "//<MetaVariable>label=\"Latitude [degrees_north]\" value_type=\"DOUBLE\" is_primary_variable=\"F\" comment=\"Latitude\" </MetaVariable>[10]\n"
            + "//<DataVariable>label=\"time_ISO8601\" value_type=\"DOUBLE\" is_primary_variable=\"T\" comment=\"Time\" </DataVariable>[10]\n"
            + "//<DataVariable>label=\"status\" value_type=\"TEXT:2\" is_primary_variable=\"F\" comment=\"From http://some.url.gov/someProjectDocument , Table C\" </DataVariable>[10]\n"
            + "//<DataVariable>label=\"testByte [1]\" value_type=\"SIGNED_BYTE\" is_primary_variable=\"F\" </DataVariable>[10]\n"
            + "//<DataVariable>label=\"testUByte [1]\" value_type=\"BYTE\" is_primary_variable=\"F\" </DataVariable>[10]\n"
            + "//<DataVariable>label=\"testLong [1]\" value_type=\"DOUBLE\" is_primary_variable=\"F\" comment=\"Test of Longs\" </DataVariable>[10]\n"
            + "//<DataVariable>label=\"testULong [1]\" value_type=\"DOUBLE\" is_primary_variable=\"F\" comment=\"Test ULong\" </DataVariable>[10]\n"
            + "//<DataVariable>label=\"sst [degree_C]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Sea Surface Temperature\" </DataVariable>[10]\n"
            + "Cruise[9]Station[9]Type[9]yyyy-mm-ddThh:mm:ss.sss[9]Longitude [degrees_east][9]Latitude [degrees_north][9]time_ISO8601[9]status[9]testByte [1][9]testUByte [1][9]testLong [1][9]testULong [1][9]sst [degree_C][10]\n"
            + " a\\t~[252],\\n'z\\\"[8364][9][9]*[9][9]-130.2576[9]28.0002[9]2017-03-23T00:45:00.000Z[9]A[9]-128[9]0[9]-9223372036854775808[9]0[9]10.9[10]\n"
            + " a\\t~[252],\\n'z\\\"[8364][9][9]*[9][9]-130.3472[9]28.0003[9]2017-03-23T01:45:00.000Z[9][8364][9]0[9]127[9]-9007199254740992[9]9223372036854775807[9]10.0[10]\n"
            + " a\\t~[252],\\n'z\\\"[8364][9][9]*[9][9]-130.4305[9]28.0001[9]2017-03-23T02:45:00.000Z[9]\\t[9]126[9]254[9]9223372036854775806[9]18446744073709551614[9][10]\n"
            + " a\\t~[252],\\n'z\\\"[8364][9][9]*[9][9]-131.5578[9]27.9998[9]2017-03-23T12:45:00.000Z[9]\\\"[9][9][9][9][9][10]\n"
            + " a\\t~[252],\\n'z\\\"[8364][9][9]*[9][9]-132.0014[9]28.0003[9]2017-03-23T21:45:00.000Z[9][252][9][9][9][9][9][10]\n"
            + " a\\t~[252],\\n'z\\\"[8364][9][9]*[9][9]-132.1591[9]28.0002[9]2017-03-23T23:45:00.000Z[9]?[9][9][9][9][9][10]\n"
            + "[end]";

    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log(
        "2020-04-14 .odv has a new version, only partially supported here.\n"
            + "Someday: make more changes to support it.");

    // *** getting tsv
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".tsv");
    results = String2.annotatedString(File2.directReadFrom88591File(dir + tName));
    // String2.log(results);
    expected =
        "ship[9]time[9]latitude[9]longitude[9]status[9]testByte[9]testUByte[9]testLong[9]testULong[9]sst[10]\n"
            + "[9]UTC[9]degrees_north[9]degrees_east[9][9]1[9]1[9]1[9]1[9]degree_C[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T00:45:00Z[9]28.0002[9]-130.2576[9]A[9]-128[9]0[9]-9223372036854775808[9]0[9]10.9[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T01:45:00Z[9]28.0003[9]-130.3472[9]\"\\u20ac\"[9]0[9]127[9]-9007199254740992[9]9223372036854775807[9]10.0[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T02:45:00Z[9]28.0001[9]-130.4305[9]\"\\t\"[9]126[9]254[9]9223372036854775806[9]18446744073709551614[9]NaN[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T12:45:00Z[9]27.9998[9]-131.5578[9]\"\\\"\"[9]NaN[9]NaN[9]NaN[9]NaN[9]NaN[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T21:45:00Z[9]28.0003[9]-132.0014[9]\"\\u00fc\"[9]NaN[9]NaN[9]NaN[9]NaN[9]NaN[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T23:45:00Z[9]28.0002[9]-132.1591[9]?[9]NaN[9]NaN[9]NaN[9]NaN[9]NaN[10]\n"
            + "[end]";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting tsvp
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".tsvp");
    results = String2.annotatedString(File2.directReadFrom88591File(dir + tName));
    // String2.log(results);
    expected =
        "ship[9]time (UTC)[9]latitude (degrees_north)[9]longitude (degrees_east)[9]status[9]testByte (1)[9]testUByte (1)[9]testLong (1)[9]testULong (1)[9]sst (degree_C)[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T00:45:00Z[9]28.0002[9]-130.2576[9]A[9]-128[9]0[9]-9223372036854775808[9]0[9]10.9[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T01:45:00Z[9]28.0003[9]-130.3472[9]\"\\u20ac\"[9]0[9]127[9]-9007199254740992[9]9223372036854775807[9]10.0[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T02:45:00Z[9]28.0001[9]-130.4305[9]\"\\t\"[9]126[9]254[9]9223372036854775806[9]18446744073709551614[9]NaN[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T12:45:00Z[9]27.9998[9]-131.5578[9]\"\\\"\"[9]NaN[9]NaN[9]NaN[9]NaN[9]NaN[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T21:45:00Z[9]28.0003[9]-132.0014[9]\"\\u00fc\"[9]NaN[9]NaN[9]NaN[9]NaN[9]NaN[10]\n"
            + "\" a\\t~\\u00fc,\\n'z\\\"\\u20ac\"[9]2017-03-23T23:45:00Z[9]28.0002[9]-132.1591[9]?[9]NaN[9]NaN[9]NaN[9]NaN[9]NaN[10]\n"
            + "[end]";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** getting xhtml
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_char", ".xhtml");
    results = String2.annotatedString(File2.directReadFromUtf8File(dir + tName));
    // String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>[10]\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"[10]\n"
            + "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">[10]\n"
            + "<html xmlns=\"https://www.w3.org/1999/xhtml\">[10]\n"
            + "<head>[10]\n"
            + "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />[10]\n"
            + "  <title>EDDTableFromNccsvFiles_char</title>[10]\n"
            + "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://127.0.0.1:8080/cwexperimental/images/erddap2.css\" />[10]\n"
            + "</head>[10]\n"
            + "<body>[10]\n"
            + "[10]\n"
            + "&nbsp;[10]\n"
            + "<table class=\"erd commonBGColor nowrap\">[10]\n"
            + "<tr>[10]\n"
            + "<th>ship</th>[10]\n"
            + "<th>time</th>[10]\n"
            + "<th>latitude</th>[10]\n"
            + "<th>longitude</th>[10]\n"
            + "<th>status</th>[10]\n"
            + "<th>testByte</th>[10]\n"
            + "<th>testUByte</th>[10]\n"
            + "<th>testLong</th>[10]\n"
            + "<th>testULong</th>[10]\n"
            + "<th>sst</th>[10]\n"
            + "</tr>[10]\n"
            + "<tr>[10]\n"
            + "<th></th>[10]\n"
            + "<th>UTC</th>[10]\n"
            + "<th>degrees_north</th>[10]\n"
            + "<th>degrees_east</th>[10]\n"
            + "<th></th>[10]\n"
            + "<th>1</th>[10]\n"
            + "<th>1</th>[10]\n"
            + "<th>1</th>[10]\n"
            + "<th>1</th>[10]\n"
            + "<th>degree_C</th>[10]\n"
            + "</tr>[10]\n"
            + "<tr>[10]\n"
            + "<td>[160]a&#9;~[252],[10]\n"
            + "&#39;z&quot;[8364]</td>[10]\n"
            + "<td>2017-03-23T00:45:00Z</td>[10]\n"
            + "<td class=\"R\">28.0002</td>[10]\n"
            + "<td class=\"R\">-130.2576</td>[10]\n"
            + "<td>A</td>[10]\n"
            + "<td class=\"R\">-128</td>[10]\n"
            + "<td class=\"R\">0</td>[10]\n"
            + "<td class=\"R\">-9223372036854775808</td>[10]\n"
            + "<td class=\"R\">0</td>[10]\n"
            + "<td class=\"R\">10.9</td>[10]\n"
            + "</tr>[10]\n"
            + "<tr>[10]\n"
            + "<td>[160]a&#9;~[252],[10]\n"
            + "&#39;z&quot;[8364]</td>[10]\n"
            + "<td>2017-03-23T01:45:00Z</td>[10]\n"
            + "<td class=\"R\">28.0003</td>[10]\n"
            + "<td class=\"R\">-130.3472</td>[10]\n"
            + "<td>[8364]</td>[10]\n"
            + "<td class=\"R\">0</td>[10]\n"
            + "<td class=\"R\">127</td>[10]\n"
            + "<td class=\"R\">-9007199254740992</td>[10]\n"
            + "<td class=\"R\">9223372036854775807</td>[10]\n"
            + "<td class=\"R\">10.0</td>[10]\n"
            + "</tr>[10]\n"
            + "<tr>[10]\n"
            + "<td>[160]a&#9;~[252],[10]\n"
            + "&#39;z&quot;[8364]</td>[10]\n"
            + "<td>2017-03-23T02:45:00Z</td>[10]\n"
            + "<td class=\"R\">28.0001</td>[10]\n"
            + "<td class=\"R\">-130.4305</td>[10]\n"
            + "<td>&#9;</td>[10]\n"
            + "<td class=\"R\">126</td>[10]\n"
            + "<td class=\"R\">254</td>[10]\n"
            + "<td class=\"R\">9223372036854775806</td>[10]\n"
            + "<td class=\"R\">18446744073709551614</td>[10]\n"
            + "<td></td>[10]\n"
            + "</tr>[10]\n"
            + "<tr>[10]\n"
            + "<td>[160]a&#9;~[252],[10]\n"
            + "&#39;z&quot;[8364]</td>[10]\n"
            + "<td>2017-03-23T12:45:00Z</td>[10]\n"
            + "<td class=\"R\">27.9998</td>[10]\n"
            + "<td class=\"R\">-131.5578</td>[10]\n"
            + "<td>&quot;</td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "</tr>[10]\n"
            + "<tr>[10]\n"
            + "<td>[160]a&#9;~[252],[10]\n"
            + "&#39;z&quot;[8364]</td>[10]\n"
            + "<td>2017-03-23T21:45:00Z</td>[10]\n"
            + "<td class=\"R\">28.0003</td>[10]\n"
            + "<td class=\"R\">-132.0014</td>[10]\n"
            + "<td>[252]</td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "</tr>[10]\n"
            + "<tr>[10]\n"
            + "<td>[160]a&#9;~[252],[10]\n"
            + "&#39;z&quot;[8364]</td>[10]\n"
            + "<td>2017-03-23T23:45:00Z</td>[10]\n"
            + "<td class=\"R\">28.0002</td>[10]\n"
            + "<td class=\"R\">-132.1591</td>[10]\n"
            + "<td>?</td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "<td></td>[10]\n"
            + "</tr>[10]\n"
            + "</table>[10]\n"
            + "</body>[10]\n"
            + "</html>[10]\n"
            + "[end]";
    // ship is an encoding of " a\t~\u00fc,\n'z""\u20AC"
    // source status chars are A\u20AC\t"\u00fc\uFFFF
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** Test reading data from testNccsvScalar by EDDTableFromDapSequence. */
  @org.junit.jupiter.api.Test
  void testDap() throws Exception {
    // String2.log("\n*** EDDTableFromDapSequence.testDap\n");
    /*
     * try {
     * EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "testTestNccsvScalar");
     *
     * String tName = edd.makeNewFileForDapQuery(language, null, null,
     * "longitude,latitude,time&time=%221992-01-01T00:00:00Z%22" +
     * "&longitude>=-132.0&longitude<=-112.0&latitude>=30.0&latitude<=50.0" +
     * "&distinct()&.draw=markers&.colorBar=|D||||",
     * EDStatic.fullTestCacheDirectory, edd.className() + "_SVGraph", ".png");
     * Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
     *
     * } catch (Throwable t) {
     * throw new
     * RuntimeException("2014 THIS DATASET HAS BEEN UNAVAILABLE FOR MONTHS.", t);
     * }
     */
  }

  @org.junit.jupiter.api.Test
  void testLong() throws Exception {
    // long MIN_VALUE=-9223372036854775808 MAX_VALUE=9223372036854775807
    // 9007199254740992 (~9e15) see
    // https://www.mathworks.com/help/matlab/ref/flintmax.html
    // for (long tl = 9007199254000000L; tl < Long.MAX_VALUE; tl++) {
    // double d = tl;
    // if (Math.round(d) != tl) {
    // String2.log("tl=" + tl + " d=" + d + " is the first large long that can't
    // round trip to/from double");
    // break;
    // }
    // }
    Test.ensureEqual(9007199254740992L, Math.round((double) 9007199254740992L), "");
    // -9007199254740992 see https://www.mathworks.com/help/matlab/ref/flintmax.html
    // for (long tl = -9007199254000000L; tl > Long.MIN_VALUE; tl--) {
    // double d = tl;
    // if (Math.round(d) != tl) {
    // String2.log("tl=" + tl + " d=" + d + " is the first small long that can't
    // round trip to/from double");
    // break;
    // }
    // }
    Test.ensureEqual(-9007199254740992L, Math.round((double) -9007199254740992L), "");
  }
}
