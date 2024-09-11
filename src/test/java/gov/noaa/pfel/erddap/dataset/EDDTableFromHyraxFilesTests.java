package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagIncompleteTest;
import tags.TagMissingDataset;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromHyraxFilesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * testGenerateDatasetsXml. This doesn't test suggestTestOutOfDate, except that for old data it
   * doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // Source 404
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();

    String results =
        EDDTableFromHyraxFiles.generateDatasetsXml(
            "https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/07/",
            "pentad.*\\.nc\\.gz",
            "https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/07/pentad_19870705_v11l35flk.nc.gz",
            2880,
            "",
            "",
            "",
            "", // extract
            "time",
            "time",
            -1, // defaultStandardizeWhat
            new Attributes());

    String expected =
        "<dataset type=\"EDDTableFromHyraxFiles\" datasetID=\"nasa_jpl_6965_9def_b894\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>2880</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>0</updateEveryNMillis>\n"
            + "    <fileDir></fileDir>\n"
            + "    <fileNameRegex>pentad.*\\.nc\\.gz</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <sortedColumnSourceName>time</sortedColumnSourceName>\n"
            + "    <sortFilesBySourceNames>time</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"base_date\" type=\"shortList\">1987 7 5</att>\n"
            + "        <att name=\"Conventions\">COARDS</att>\n"
            + "        <att name=\"description\">Time average of level3.0 products for the period: 1987-07-05 to 1987-07-09</att>\n"
            + "        <att name=\"history\">Created by NASA Goddard Space Flight Center under the NASA REASoN CAN: A Cross-Calibrated, Multi-Platform Ocean Surface Wind Velocity Product for Meteorological and Oceanographic Applications</att>\n"
            + "        <att name=\"title\">Atlas FLK v1.1 derived surface winds (level 3.5)</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Point</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">support-podaac@earthdata.nasa.gov</att>\n"
            + "        <att name=\"creator_name\">NASA GSFC MEaSUREs, NOAA</att>\n"
            + "        <att name=\"creator_type\">group</att>\n"
            + "        <att name=\"creator_url\">https://podaac.jpl.nasa.gov/dataset/CCMP_MEASURES_ATLAS_L4_OW_L3_0_WIND_VECTORS_FLK</att>\n"
            + "        <att name=\"infoUrl\">https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/07/.html</att>\n"
            + "        <att name=\"institution\">NASA GSFC, NOAA</att>\n"
            + "        <att name=\"keywords\">atlas, atmosphere, atmospheric, center, component, data, derived, downward, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Wind Stress, eastward, eastward_wind, flight, flk, goddard, gsfc, latitude, level, longitude, meters, nasa, noaa, nobs, northward, northward_wind, number, observations, oceanography, physical, physical oceanography, pseudostress, science, space, speed, statistics, stress, surface, surface_downward_eastward_stress, surface_downward_northward_stress, time, u-component, u-wind, upstr, uwnd, v-component, v-wind, v1.1, vpstr, vwnd, wind, wind_speed, winds, wspd</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/07/</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">Time average of level3.0 products for the period: 1987-07-05 to 1987-07-09</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.125 359.875</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-78.375 78.375</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">4440.0 4440.0</att>\n"
            + "            <att name=\"avg_period\">0000-00-05 00:00:00</att>\n"
            + "            <att name=\"delta_t\">0000-00-05 00:00:00</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"units\">hours since 1987-01-01 00:00:0.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">4700.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">4200.0</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">hours since 1987-01-01T00:00:00.000Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>uwnd</sourceName>\n"
            + "        <destinationName>uwnd</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-15.897105 22.495602</att>\n"
            + "            <att name=\"add_offset\" type=\"float\">0.0</att>\n"
            + "            <att name=\"long_name\">u-wind at 10 meters</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">-32767</att>\n"
            + "            <att name=\"scale_factor\" type=\"float\">0.001525972</att>\n"
            + "            <att name=\"units\">m/s</att>\n"
            + "            <att name=\"valid_range\" type=\"floatList\">-50.0 50.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n"
            + "            <att name=\"ioos_category\">Wind</att>\n"
            + "            <att name=\"standard_name\">eastward_wind</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>vwnd</sourceName>\n"
            + "        <destinationName>vwnd</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-16.493101 25.951406</att>\n"
            + "            <att name=\"add_offset\" type=\"float\">0.0</att>\n"
            + "            <att name=\"long_name\">v-wind at 10 meters</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">-32767</att>\n"
            + "            <att name=\"scale_factor\" type=\"float\">0.001525972</att>\n"
            + "            <att name=\"units\">m/s</att>\n"
            + "            <att name=\"valid_range\" type=\"floatList\">-50.0 50.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n"
            + "            <att name=\"ioos_category\">Wind</att>\n"
            + "            <att name=\"standard_name\">northward_wind</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>wspd</sourceName>\n"
            + "        <destinationName>wspd</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.040334757 29.29576</att>\n"
            + "            <att name=\"add_offset\" type=\"float\">37.5</att>\n"
            + "            <att name=\"long_name\">wind speed at 10 meters</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">-32767</att>\n"
            + "            <att name=\"scale_factor\" type=\"float\">0.001144479</att>\n"
            + "            <att name=\"units\">m/s</att>\n"
            + "            <att name=\"valid_range\" type=\"floatList\">0.0 75.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Wind</att>\n"
            + "            <att name=\"standard_name\">wind_speed</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>upstr</sourceName>\n"
            + "        <destinationName>upstr</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-284.62888 657.83044</att>\n"
            + "            <att name=\"add_offset\" type=\"float\">0.0</att>\n"
            + "            <att name=\"long_name\">u-component of pseudostress at 10 meters</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">-32767</att>\n"
            + "            <att name=\"scale_factor\" type=\"float\">0.03051944</att>\n"
            + "            <att name=\"units\">m2/s2</att>\n"
            + "            <att name=\"valid_range\" type=\"floatList\">-1000.0 1000.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">0.5</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-0.5</att>\n"
            + "            <att name=\"ioos_category\">Physical Oceanography</att>\n"
            + "            <att name=\"standard_name\">surface_downward_eastward_stress</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>vpstr</sourceName>\n"
            + "        <destinationName>vpstr</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-305.23505 694.61383</att>\n"
            + "            <att name=\"add_offset\" type=\"float\">0.0</att>\n"
            + "            <att name=\"long_name\">v-component of pseudostress at 10 meters</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">-32767</att>\n"
            + "            <att name=\"scale_factor\" type=\"float\">0.03051944</att>\n"
            + "            <att name=\"units\">m2/s2</att>\n"
            + "            <att name=\"valid_range\" type=\"floatList\">-1000.0 1000.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">0.5</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-0.5</att>\n"
            + "            <att name=\"ioos_category\">Physical Oceanography</att>\n"
            + "            <att name=\"standard_name\">surface_downward_northward_stress</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>nobs</sourceName>\n"
            + "        <destinationName>nobs</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.0 20.0</att>\n"
            + "            <att name=\"add_offset\" type=\"float\">32766.0</att>\n"
            + "            <att name=\"long_name\">number of observations</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">-32767</att>\n"
            + "            <att name=\"scale_factor\" type=\"float\">1.0</att>\n"
            + "            <att name=\"units\">count</att>\n"
            + "            <att name=\"valid_range\" type=\"floatList\">0.0 65532.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";

    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

    /*
     * *** This doesn't work. Usually no files already downloaded.
     * //ensure it is ready-to-use by making a dataset from it
     * String tDatasetID = "nasa_jpl_ae1a_8793_8b49";
     * EDD.deleteCachedDatasetInfo(tDatasetID);
     * EDD edd = oneFromXmlFragment(null, results);
     * Test.ensureEqual(edd.datasetID(), tDatasetID, "");
     * Test.ensureEqual(edd.title(),
     * "Atlas FLK v1.1 derived surface winds (level 3.5)", "");
     * Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()),
     * "longitude, latitude, time, uwnd, vwnd, wspd, upstr, vpstr, nobs", "");
     */

  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // Source 404
  void testGenerateDatasetsXml2() throws Throwable {
    // testVerboseOn();
    String results =
        EDDTableFromHyraxFiles.generateDatasetsXml(
            "https://data.nodc.noaa.gov/opendap/wod/XBT/195209-196711/contents.html",
            "wod_002057.*\\.nc",
            "https://data.nodc.noaa.gov/opendap/wod/XBT/195209-196711/wod_002057989O.nc",
            2880,
            "",
            "",
            "",
            "", // extract
            "time",
            "time",
            -1, // defaultStandardizeWhat
            new Attributes());

    /*
     * 2012-04-10 That throws exception: Hyrax isn't compatible with JDAP???
     * dods.dap.DDSException:
     * Parse Error on token: String
     * In the dataset descriptor object:
     * Expected a variable declaration (e.g., Int32 i;).
     * at dods.dap.parser.DDSParser.error(DDSParser.java:710)
     * at dods.dap.parser.DDSParser.NonListDecl(DDSParser.java:241)
     * at dods.dap.parser.DDSParser.Declaration(DDSParser.java:155)
     * at dods.dap.parser.DDSParser.Declarations(DDSParser.java:131)
     * at dods.dap.parser.DDSParser.Dataset(DDSParser.java:97)
     * at dods.dap.DDS.parse(DDS.java:442)
     * at dods.dap.DConnect.getDDS(DConnect.java:388)
     * at gov.noaa.pfel.erddap.dataset.EDDTableFromHyraxFiles.generateDatasetsXml(
     * EDDTableFromHyraxFiles.java:570)
     * at
     * gov.noaa.pfel.erddap.dataset.EDDTableFromHyraxFiles.testGenerateDatasetsXml(
     * EDDTableFromHyraxFiles.java:930)
     * at gov.noaa.pfel.erddap.dataset.EDDTableFromHyraxFiles.test(
     * EDDTableFromHyraxFiles.java:1069)
     * at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:1395)
     */
    String expected = "<dataset zzz" + "\n";

    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

    /*
     * *** This doesn't work. Usually no files already downloaded.
     * //ensure it is ready-to-use by making a dataset from it
     * String tDatasetID = "nasa_jpl_ae1a_8793_8b49";
     * EDD.deleteCachedDatasetInfo(tDatasetID);
     * EDD edd = oneFromXmlFragment(null, results);
     * Test.ensureEqual(edd.datasetID(), tDatasetID, "");
     * Test.ensureEqual(edd.title(),
     * "Atlas FLK v1.1 derived surface winds (level 3.5)", "");
     * Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()),
     * "longitude, latitude, time, uwnd, vwnd, wspd, upstr, vpstr, nobs", "");
     */

  }

  /**
   * This tests the methods in this class. This is a bizarre test since it is really gridded data.
   * But no alternatives currently. Actually, this is useful, because it tests serving gridded data
   * via tabledap.
   *
   * <p>Also, this is a test of ERDDAP adjusting valid_range by scale_factor add_offset
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagIncompleteTest // This fails because source was .gz so created local files were called .gz
  // even
  // though they aren't .gz compressed.
  void testJpl(boolean deleteCachedInfoAndOneFile) throws Throwable {
    // String2.log("\n******
    // EDDTableFromHyraxFiles.testJpl(deleteCachedInfoAndOneFile=" +
    // deleteCachedInfoAndOneFile + ")\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    int po;
    EDV edv;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.
    String id = "testEDDTableFromHyraxFiles";
    String deletedFile = "pentad_19870928_v11l35flk.nc.gz";

    EDDTable eddTable = null;
    try {

      // delete the last file in the collection
      if (deleteCachedInfoAndOneFile) {
        EDDTableFromHyraxFiles.deleteCachedDatasetInfo(id);
        File2.delete(EDStatic.fullCopyDirectory + id + "/" + deletedFile);
        Math2.sleep(1000);
      }

      eddTable = (EDDTable) EDDTestDataset.gettestEDDTableFromHyraxFiles();

      if (deleteCachedInfoAndOneFile) {
        // String2.pressEnterToContinue(
        // "\n****** BOB! ******\n" +
        // "This test just deleted a file:\n" +
        // EDStatic.fullCopyDirectory + id + "/" + deletedFile + "\n" +
        // "The background task to re-download it should have already started.\n" +
        // "The remote dataset is really slow.\n" +
        // "Wait for it to finish background tasks.\n\n");
        Math2.sleep(5000);
        eddTable =
            (EDDTable) EDDTestDataset.gettestEDDTableFromHyraxFiles(); // redownload the dataset
      }
    } catch (Throwable t) {
      throw new RuntimeException(
          "2019-05 This fails because source was .gz so created local files were called .gz\n"
              + "even though they aren't .gz compressed.\n"
              + "Solve this, or better: stop using EDDTableFromHyraxfiles",
          t);
    }

    // *** test getting das for entire dataset
    try {
      String2.log("\n****************** EDDTableFromHyraxFiles das and dds for entire dataset\n");
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_Entire",
              ".das");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "Attributes {\n"
              + " s {\n"
              + "  longitude {\n"
              + "    String _CoordinateAxisType \"Lon\";\n"
              + "    Float32 actual_range 0.125, 359.875;\n"
              + "    String axis \"X\";\n"
              + "    Float64 colorBarMaximum 180.0;\n"
              + "    Float64 colorBarMinimum -180.0;\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Longitude\";\n"
              + "    String standard_name \"longitude\";\n"
              + "    String units \"degrees_east\";\n"
              + "  }\n"
              + "  latitude {\n"
              + "    String _CoordinateAxisType \"Lat\";\n"
              + "    Float32 actual_range -78.375, 78.375;\n"
              + "    String axis \"Y\";\n"
              + "    Float64 colorBarMaximum 90.0;\n"
              + "    Float64 colorBarMinimum -90.0;\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Latitude\";\n"
              + "    String standard_name \"latitude\";\n"
              + "    String units \"degrees_north\";\n"
              + "  }\n"
              + "  time {\n"
              + "    String _CoordinateAxisType \"Time\";\n"
              + "    Float64 actual_range 5.576256e+8, 5.597856e+8;\n"
              + "    String avg_period \"0000-00-05 00:00:00\";\n"
              + "    String axis \"T\";\n"
              + "    Float64 colorBarMaximum 6300.0;\n"
              + "    Float64 colorBarMinimum 5700.0;\n"
              + "    String delta_t \"0000-00-05 00:00:00\";\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Time\";\n"
              + "    String standard_name \"time\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  uwnd {\n"
              + "    Float32 actual_range -17.78368, 25.81639;\n"
              + "    Float64 colorBarMaximum 15.0;\n"
              + "    Float64 colorBarMinimum -15.0;\n"
              + "    String ioos_category \"Wind\";\n"
              + "    String long_name \"u-wind at 10 meters\";\n"
              + "    Float32 missing_value -50.001526;\n"
              + "    String standard_name \"eastward_wind\";\n"
              + "    String units \"m/s\";\n"
              + "    Float32 valid_range -50.0, 50.0;\n"
              + // this is a test of ERDDAP adjusting valid_range by scale_factor
              // add_offset
              "  }\n"
              + "  vwnd {\n"
              + "    Float32 actual_range -17.49374, 19.36153;\n"
              + "    Float64 colorBarMaximum 15.0;\n"
              + "    Float64 colorBarMinimum -15.0;\n"
              + "    String ioos_category \"Wind\";\n"
              + "    String long_name \"v-wind at 10 meters\";\n"
              + "    Float32 missing_value -50.001526;\n"
              + "    String standard_name \"northward_wind\";\n"
              + "    String units \"m/s\";\n"
              + "    Float32 valid_range -50.0, 50.0;\n"
              + "  }\n"
              + "  wspd {\n"
              + "    Float32 actual_range 0.01487931, 27.53731;\n"
              + "    Float64 colorBarMaximum 15.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    String ioos_category \"Wind\";\n"
              + "    String long_name \"wind speed at 10 meters\";\n"
              + "    Float32 missing_value -0.001143393;\n"
              + "    String standard_name \"wind_speed\";\n"
              + "    String units \"m/s\";\n"
              + "    Float32 valid_range 0.0, 75.0;\n"
              + "  }\n"
              + "  upstr {\n"
              + "    Float32 actual_range -317.2191, 710.9198;\n"
              + "    Float64 colorBarMaximum 0.5;\n"
              + "    Float64 colorBarMinimum -0.5;\n"
              + "    String ioos_category \"Physical Oceanography\";\n"
              + "    String long_name \"u-component of pseudostress at 10 meters\";\n"
              + "    Float32 missing_value -1000.0305;\n"
              + "    String standard_name \"surface_downward_eastward_stress\";\n"
              + "    String units \"m2/s2\";\n"
              + "    Float32 valid_range -1000.0, 1000.0;\n"
              + "  }\n"
              + "  vpstr {\n"
              + "    Float32 actual_range -404.8404, 386.9255;\n"
              + "    Float64 colorBarMaximum 0.5;\n"
              + "    Float64 colorBarMinimum -0.5;\n"
              + "    String ioos_category \"Physical Oceanography\";\n"
              + "    String long_name \"v-component of pseudostress at 10 meters\";\n"
              + "    Float32 missing_value -1000.0305;\n"
              + "    String standard_name \"surface_downward_northward_stress\";\n"
              + "    String units \"m2/s2\";\n"
              + "    Float32 valid_range -1000.0, 1000.0;\n"
              + "  }\n"
              + "  nobs {\n"
              + "    Float32 actual_range 0.0, 20.0;\n"
              + "    Float64 colorBarMaximum 100.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    String ioos_category \"Statistics\";\n"
              + "    String long_name \"number of observations\";\n"
              + "    Float32 missing_value -1.0;\n"
              + "    String units \"count\";\n"
              + "    Float32 valid_range 0.0, 65532.0;\n"
              + "  }\n"
              + " }\n"
              + "  NC_GLOBAL {\n"
              + "    String cdm_data_type \"Point\";\n"
              + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
              + "    Float64 Easternmost_Easting 359.875;\n"
              + "    String featureType \"Point\";\n"
              + "    Float64 geospatial_lat_max 78.375;\n"
              + "    Float64 geospatial_lat_min -78.375;\n"
              + "    String geospatial_lat_units \"degrees_north\";\n"
              + "    Float64 geospatial_lon_max 359.875;\n"
              + "    Float64 geospatial_lon_min 0.125;\n"
              + "    String geospatial_lon_units \"degrees_east\";\n"
              + "    String history \"Created by NASA Goddard Space Flight Center under the NASA REASoN CAN: A Cross-Calibrated, Multi-Platform Ocean Surface Wind Velocity Product for Meteorological and Oceanographic Applications\n"
              + today;
      tResults = results.substring(0, Math.min(results.length(), expected.length()));
      Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

      // + "
      // https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/M09/\n"
      // +
      // today + " http://localhost:8080/cwexperimental/
      expected =
          "tabledap/testEDDTableFromHyraxFiles.das\";\n"
              + "    String infoUrl \"https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/09/.html\";\n"
              + "    String institution \"NASA JPL\";\n"
              + "    String keywords \"atlas, atmosphere, atmospheric, component, derived, downward, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Atmosphere > Atmospheric Winds > Wind Stress, eastward, eastward_wind, flk, jpl, level, meters, nasa, northward, northward_wind, number, observations, oceanography, physical, physical oceanography, pseudostress, speed, statistics, stress, surface, surface_downward_eastward_stress, surface_downward_northward_stress, time, u-component, u-wind, v-component, v-wind, v1.1, wind, wind_speed, winds\";\n"
              + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
              + "    String license \"The data may be used and redistributed for free but is not intended\n"
              + "for legal use, since it may contain inaccuracies. Neither the data\n"
              + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
              + "of their employees or contractors, makes any warranty, express or\n"
              + "implied, including warranties of merchantability and fitness for a\n"
              + "particular purpose, or assumes any legal liability for the accuracy,\n"
              + "completeness, or usefulness, of this information.\";\n"
              + "    Float64 Northernmost_Northing 78.375;\n"
              + "    String sourceUrl \"https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/09/\";\n"
              + "    Float64 Southernmost_Northing -78.375;\n"
              + "    String standard_name_vocabulary \"CF Standard Name Table v55\";\n"
              + "    String summary \"Time average of level3.0 products.\";\n"
              + "    String time_coverage_end \"1987-09-28T00:00:00Z\";\n"
              + "    String time_coverage_start \"1987-09-03T00:00:00Z\";\n"
              + "    String title \"Atlas FLK v1.1 derived surface winds (level 3.5)\";\n"
              + "    Float64 Westernmost_Easting 0.125;\n"
              + "  }\n"
              + "}\n";
      int tPo = results.indexOf(expected.substring(0, 17));
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
      Test.ensureEqual(
          results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
          expected,
          "results=\n" + results);

    } catch (Throwable t) {
      throw new RuntimeException(
          "2019-05 This fails because source was .gz so created local files were called .gz\n"
              + "even though they aren't .gz compressed.\n"
              + "Solve this, or better: stop using EDDTableFromHyraxfiles",
          t);
    }

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_Entire",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 longitude;\n"
            + "    Float32 latitude;\n"
            + "    Float64 time;\n"
            + "    Float32 uwnd;\n"
            + "    Float32 vwnd;\n"
            + "    Float32 wspd;\n"
            + "    Float32 upstr;\n"
            + "    Float32 vpstr;\n"
            + "    Float32 nobs;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n****************** EDDTableFromHyraxFiles.testWcosTemp make DATA FILES\n");

    // .csv for one lat,lon,time
    userDapQuery =
        "longitude,latitude,time,uwnd,vwnd,wspd,upstr,vpstr,nobs&longitude>=220&longitude<=220.5&latitude>=40&latitude<=40.5&time>=1987-09-03&time<=1987-09-28";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_stationList",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "longitude,latitude,time,uwnd,vwnd,wspd,upstr,vpstr,nobs\n"
            + "degrees_east,degrees_north,UTC,m/s,m/s,m/s,m2/s2,m2/s2,count\n"
            + "220.125,40.125,1987-09-03T00:00:00Z,-4.080449,-2.3835683,4.9441504,-20.600622,-12.390893,6.0\n"
            + "220.375,40.125,1987-09-03T00:00:00Z,-4.194897,-3.0107427,5.30695,-22.767502,-16.93829,8.0\n"
            + "220.125,40.375,1987-09-03T00:00:00Z,-3.9186962,-2.346945,4.769045,-19.07465,-11.99414,6.0\n"
            + "220.375,40.375,1987-09-03T00:00:00Z,-3.8927546,-2.5865226,4.833136,-19.10517,-13.306476,6.0\n"
            + "220.125,40.125,1987-09-08T00:00:00Z,1.6678874,-2.6307757,5.0425754,14.557773,-14.618812,5.0\n"
            + "220.375,40.125,1987-09-08T00:00:00Z,1.7289263,-2.7055483,5.1432896,15.351278,-15.320759,5.0\n"
            + "220.125,40.375,1987-09-08T00:00:00Z,1.6633095,-2.5712628,5.0608873,14.374657,-14.679851,5.0\n"
            + "220.375,40.375,1987-09-08T00:00:00Z,1.7029848,-2.6567173,5.1478677,14.893487,-15.381798,5.0\n"
            + "220.125,40.125,1987-09-13T00:00:00Z,3.212171,-0.6378563,7.653132,32.442165,-7.477263,8.0\n"
            + "220.375,40.125,1987-09-13T00:00:00Z,3.2533722,-0.8224989,7.526095,32.350605,-9.186352,8.0\n"
            + "220.125,40.375,1987-09-13T00:00:00Z,3.5280473,-1.7747054,7.6210866,36.043457,-18.220106,7.0\n"
            + "220.375,40.375,1987-09-13T00:00:00Z,3.5936642,-1.9257767,7.5295286,36.196056,-19.471403,7.0\n"
            + "220.125,40.125,1987-09-18T00:00:00Z,6.5067444,9.657877,11.65652,76.20704,112.647255,2.0\n"
            + "220.375,40.125,1987-09-18T00:00:00Z,6.2396994,9.49765,11.372689,71.32393,108.13038,2.0\n"
            + "220.125,40.375,1987-09-18T00:00:00Z,6.590673,9.370994,11.49057,75.932365,107.61154,2.0\n"
            + "220.375,40.375,1987-09-18T00:00:00Z,6.3495693,9.279436,11.273119,71.87328,104.5596,2.0\n"
            + "220.125,40.125,1987-09-23T00:00:00Z,1.9959713,-7.846548,8.204771,15.625954,-66.83757,3.0\n"
            + "220.375,40.125,1987-09-23T00:00:00Z,1.8693157,-7.852652,8.200193,14.405175,-66.80705,3.0\n"
            + "220.125,40.375,1987-09-23T00:00:00Z,0.7690899,-5.722395,6.9778895,9.399987,-49.563572,4.0\n"
            + "220.375,40.375,1987-09-23T00:00:00Z,0.67753154,-5.797168,7.0122237,8.637002,-50.20448,4.0\n"
            + "220.125,40.125,1987-09-28T00:00:00Z,1.1505829,8.963559,11.136927,6.8363547,105.841415,6.0\n"
            + "220.375,40.125,1987-09-28T00:00:00Z,1.9196727,8.066288,10.433072,13.214917,90.97845,7.0\n"
            + "220.125,40.375,1987-09-28T00:00:00Z,1.2467191,8.910151,11.095725,8.6064825,104.43752,6.0\n"
            + "220.375,40.375,1987-09-28T00:00:00Z,1.2406152,8.798755,10.894297,9.003235,100.80571,6.0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv few variables, for small lat,lon range, one time
    userDapQuery =
        "longitude,latitude,time,upstr,vpstr&longitude>=220&longitude<=221&latitude>=40&latitude<=41&time>=1987-09-28&time<=1987-09-28";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_1StationGTLT",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,time,upstr,vpstr\n"
            + "degrees_east,degrees_north,UTC,m2/s2,m2/s2\n"
            + "220.125,40.125,1987-09-28T00:00:00Z,6.8363547,105.841415\n"
            + "220.375,40.125,1987-09-28T00:00:00Z,13.214917,90.97845\n"
            + "220.625,40.125,1987-09-28T00:00:00Z,10.468168,92.84013\n"
            + "220.875,40.125,1987-09-28T00:00:00Z,10.834401,89.54404\n"
            + "220.125,40.375,1987-09-28T00:00:00Z,8.6064825,104.43752\n"
            + "220.375,40.375,1987-09-28T00:00:00Z,9.003235,100.80571\n"
            + "220.625,40.375,1987-09-28T00:00:00Z,12.696087,92.68754\n"
            + "220.875,40.375,1987-09-28T00:00:00Z,13.062321,89.87975\n"
            + "220.125,40.625,1987-09-28T00:00:00Z,9.9798565,103.399864\n"
            + "220.375,40.625,1987-09-28T00:00:00Z,10.468168,100.19532\n"
            + "220.625,40.625,1987-09-28T00:00:00Z,14.771409,92.53494\n"
            + "220.875,40.625,1987-09-28T00:00:00Z,15.320759,90.15443\n"
            + "220.125,40.875,1987-09-28T00:00:00Z,11.139596,102.72843\n"
            + "220.375,40.875,1987-09-28T00:00:00Z,11.811024,99.89013\n"
            + "220.625,40.875,1987-09-28T00:00:00Z,16.785692,92.4739\n"
            + "220.875,40.875,1987-09-28T00:00:00Z,17.4266,90.368065\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    /* */
  }
}
