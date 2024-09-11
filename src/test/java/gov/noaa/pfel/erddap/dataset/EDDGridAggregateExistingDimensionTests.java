package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import org.junit.jupiter.api.BeforeAll;
import tags.TagLocalERDDAP;
import tags.TagMissingDataset;
import tags.TagThredds;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridAggregateExistingDimensionTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests the basic methods in this class.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testBasic() throws Throwable {

    String2.log(
        "\n****************** EDDGridAggregateExistingDimension.testBasic() *****************\n");
    // testVerboseOn();
    String name, tName, userDapQuery, results, expected, error;
    int language = 0;

    // *** NDBC is also IMPORTANT UNIQUE TEST of >1 variable in a file
    EDDGrid gridDataset = (EDDGrid) EDDTestDataset.getndbcCWind41002();

    // min max
    EDV edv = gridDataset.findAxisVariableByDestinationName(0, "longitude");
    Test.ensureEqual(edv.destinationMinDouble(), -75.42, "");
    Test.ensureEqual(edv.destinationMaxDouble(), -75.42, "");

    String ndbcDapQuery = "wind_speed[1:5][0][0]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className(),
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,wind_speed\n"
            + "UTC,degrees_north,degrees_east,m s-1\n"
            + "1989-06-13T16:10:00Z,32.27,-75.42,15.7\n"
            + "1989-06-13T16:20:00Z,32.27,-75.42,15.0\n"
            + "1989-06-13T16:30:00Z,32.27,-75.42,14.4\n"
            + "1989-06-13T16:40:00Z,32.27,-75.42,14.0\n"
            + "1989-06-13T16:50:00Z,32.27,-75.42,13.6\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    ndbcDapQuery = "wind_speed[1:5][0][0]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className(),
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    int dataPo = results.indexOf("data:");
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    expected =
        "netcdf EDDGridAggregateExistingDimension.nc {\n"
            + "  dimensions:\n"
            + "    time = 5;\n"
            + // (has coord.var)\n" + //changed when switched to netcdf-java 4.0, 2009-02-23
            "    latitude = 1;\n"
            + // (has coord.var)\n" +
            "    longitude = 1;\n"
            + // (has coord.var)\n" +
            "  variables:\n"
            + "    double time(time=5);\n"
            + "      :_CoordinateAxisType = \"Time\";\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // BUG???!!!
    // " :actual_range = 6.137568E8, 7.258458E8; // double\n" + //this changes
    // depending on how many files aggregated

    expected =
        ":axis = \"T\";\n"
            + "      :ioos_category = \"Time\";\n"
            + "      :long_name = \"Epoch Time\";\n"
            + "      :short_name = \"time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "\n"
            + "    float latitude(latitude=1);\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "      :actual_range = 32.27f, 32.27f; // float\n"
            + "      :axis = \"Y\";\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :short_name = \"latitude\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "\n"
            + "    float longitude(longitude=1);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :actual_range = -75.42f, -75.42f; // float\n"
            + "      :axis = \"X\";\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :short_name = \"longitude\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    float wind_speed(time=5, latitude=1, longitude=1);\n"
            + "      :_FillValue = 99.0f; // float\n"
            + "      :ioos_category = \"Wind\";\n"
            + "      :long_name = \"Wind Speed\";\n"
            + "      :missing_value = 99.0f; // float\n"
            + "      :short_name = \"wspd\";\n"
            + "      :standard_name = \"wind_speed\";\n"
            + "      :units = \"m s-1\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :cdm_data_type = \"Grid\";\n"
            + "  :comment = \"S HATTERAS - 250 NM East of Charleston, SC\";\n"
            + "  :contributor_name = \"NOAA NDBC\";\n"
            + "  :contributor_role = \"Source of data.\";\n"
            + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "  :Easternmost_Easting = -75.42f; // float\n"
            + "  :geospatial_lat_max = 32.27f; // float\n"
            + "  :geospatial_lat_min = 32.27f; // float\n"
            + "  :geospatial_lat_units = \"degrees_north\";\n"
            + "  :geospatial_lon_max = -75.42f; // float\n"
            + "  :geospatial_lon_min = -75.42f; // float\n"
            + "  :geospatial_lon_units = \"degrees_east\";\n"
            + "  :history = \"";
    // today + "
    // https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always non-https url
    int po = results.indexOf(":axis =");
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "/griddap/ndbcCWind41002.nc?wind_speed[1:5][0][0]\";\n"
            + "  :infoUrl = \"https://www.ndbc.noaa.gov/cwind.shtml\";\n"
            + "  :institution = \"NOAA NDBC\";\n"
            + "  :keywords = \"Earth Science > Atmosphere > Atmospheric Winds > Surface Winds\";\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + "  :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "  :location = \"32.27 N 75.42 W\";\n"
            + "  :Northernmost_Northing = 32.27f; // float\n"
            + "  :quality = \"Automated QC checks with manual editing and comprehensive monthly QC\";\n"
            + "  :sourceUrl = \"https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc\";\n"
            + "  :Southernmost_Northing = 32.27f; // float\n"
            + "  :station = \"41002\";\n"
            + "  :summary = \"These continuous wind measurements from the NOAA National Data Buoy Center (NDBC) stations are 10-minute average values of wind speed (in m/s) and direction (in degrees clockwise from North).\";\n"
            + "  :time_coverage_end = \"1989-06-13T16:50:00Z\";\n"
            + "  :time_coverage_start = \"1989-06-13T16:10:00Z\";\n"
            + "  :title = \"Wind Data from NDBC 41002\";\n"
            + "  :url = \"https://dods.ndbc.noaa.gov\";\n"
            + "  :Westernmost_Easting = -75.42f; // float\n"
            + "\n"
            + "  data:\n"
            + "    time = \n"
            + "      {6.137574E8, 6.13758E8, 6.137586E8, 6.137592E8, 6.137598E8}\n"
            + "    latitude = \n"
            + "      {32.27}\n"
            + "    longitude = \n"
            + "      {-75.42}\n"
            + "    wind_speed = \n"
            + "      {\n"
            + "        {\n"
            + "          {15.7}\n"
            + "        },\n"
            + "        {\n"
            + "          {15.0}\n"
            + "        },\n"
            + "        {\n"
            + "          {14.4}\n"
            + "        },\n"
            + "        {\n"
            + "          {14.0}\n"
            + "        },\n"
            + "        {\n"
            + "          {13.6}\n"
            + "        }\n"
            + "      }\n"
            + "}\n";
    po = results.indexOf(expected.substring(0, 50));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

    ndbcDapQuery = "wind_direction[1:5][0][0]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "2",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,wind_direction\n"
            + "UTC,degrees_north,degrees_east,degrees_true\n"
            + "1989-06-13T16:10:00Z,32.27,-75.42,234\n"
            + "1989-06-13T16:20:00Z,32.27,-75.42,233\n"
            + "1989-06-13T16:30:00Z,32.27,-75.42,233\n"
            + "1989-06-13T16:40:00Z,32.27,-75.42,235\n"
            + "1989-06-13T16:50:00Z,32.27,-75.42,235\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    ndbcDapQuery = "wind_speed[1:5][0][0],wind_direction[1:5][0][0]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "3",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,wind_speed,wind_direction\n"
            + "UTC,degrees_north,degrees_east,m s-1,degrees_true\n"
            + "1989-06-13T16:10:00Z,32.27,-75.42,15.7,234\n"
            + "1989-06-13T16:20:00Z,32.27,-75.42,15.0,233\n"
            + "1989-06-13T16:30:00Z,32.27,-75.42,14.4,233\n"
            + "1989-06-13T16:40:00Z,32.27,-75.42,14.0,235\n"
            + "1989-06-13T16:50:00Z,32.27,-75.42,13.6,235\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    ndbcDapQuery = "wind_direction[1:5][0][0],wind_speed[1:5][0][0]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "4",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,wind_direction,wind_speed\n"
            + "UTC,degrees_north,degrees_east,degrees_true,m s-1\n"
            + "1989-06-13T16:10:00Z,32.27,-75.42,234,15.7\n"
            + "1989-06-13T16:20:00Z,32.27,-75.42,233,15.0\n"
            + "1989-06-13T16:30:00Z,32.27,-75.42,233,14.4\n"
            + "1989-06-13T16:40:00Z,32.27,-75.42,235,14.0\n"
            + "1989-06-13T16:50:00Z,32.27,-75.42,235,13.6\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "4",
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    dataPo = results.indexOf("data:");
    results = results.substring(dataPo);
    expected =
        "data:\n"
            + "    time = \n"
            + "      {6.137574E8, 6.13758E8, 6.137586E8, 6.137592E8, 6.137598E8}\n"
            + "    latitude = \n"
            + "      {32.27}\n"
            + "    longitude = \n"
            + "      {-75.42}\n"
            + "    wind_direction = \n"
            + "      {\n"
            + "        {\n"
            + "          {234}\n"
            + "        },\n"
            + "        {\n"
            + "          {233}\n"
            + "        },\n"
            + "        {\n"
            + "          {233}\n"
            + "        },\n"
            + "        {\n"
            + "          {235}\n"
            + "        },\n"
            + "        {\n"
            + "          {235}\n"
            + "        }\n"
            + "      }\n"
            + "    wind_speed = \n"
            + "      {\n"
            + "        {\n"
            + "          {15.7}\n"
            + "        },\n"
            + "        {\n"
            + "          {15.0}\n"
            + "        },\n"
            + "        {\n"
            + "          {14.4}\n"
            + "        },\n"
            + "        {\n"
            + "          {14.0}\n"
            + "        },\n"
            + "        {\n"
            + "          {13.6}\n"
            + "        }\n"
            + "      }\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test seam of two datasets
    ndbcDapQuery = "wind_direction[22232:22239][0][0],wind_speed[22232:22239][0][0]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "5",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,wind_direction,wind_speed\n"
            + "UTC,degrees_north,degrees_east,degrees_true,m s-1\n"
            + "1989-12-05T05:20:00Z,32.27,-75.42,232,23.7\n"
            + "1989-12-05T05:30:00Z,32.27,-75.42,230,24.1\n"
            + "1989-12-05T05:40:00Z,32.27,-75.42,225,23.5\n"
            + "1989-12-05T05:50:00Z,32.27,-75.42,233,23.3\n"
            + "1992-04-28T23:00:00Z,32.27,-75.42,335,29.4\n"
            + "1992-04-28T23:10:00Z,32.27,-75.42,335,30.5\n"
            + "1992-04-28T23:20:00Z,32.27,-75.42,330,32.3\n"
            + "1992-04-28T23:30:00Z,32.27,-75.42,331,33.2\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test seam of two datasets with stride
    ndbcDapQuery = "wind_direction[22232:2:22239][0][0],wind_speed[22232:2:22239][0][0]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "6",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,wind_direction,wind_speed\n"
            + "UTC,degrees_north,degrees_east,degrees_true,m s-1\n"
            + "1989-12-05T05:20:00Z,32.27,-75.42,232,23.7\n"
            + "1989-12-05T05:40:00Z,32.27,-75.42,225,23.5\n"
            + "1992-04-28T23:00:00Z,32.27,-75.42,335,29.4\n"
            + "1992-04-28T23:20:00Z,32.27,-75.42,330,32.3\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // */

    // test last data value (it causes a few problems -- including different axis
    // values)
    ndbcDapQuery =
        "wind_direction[(2007-12-31T22:40:00):1:(2007-12-31T22:55:00)][0][0],"
            + "wind_speed[(2007-12-31T22:40:00):1:(2007-12-31T22:55:00)][0][0]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            ndbcDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "7",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,wind_direction,wind_speed\n"
            + "UTC,degrees_north,degrees_east,degrees_true,m s-1\n"
            + "2007-12-31T22:40:00Z,32.27,-75.42,36,4.0\n"
            + "2007-12-31T22:50:00Z,32.27,-75.42,37,4.3\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("\n*** EDDGridAggregateExistingDimension.test finished.");
  }

  /**
   * This tests rtofs - where the aggregated dimension, time, has sourceValues != destValues since
   * sourceTimeFormat isn't "seconds since 1970-01-01". !!!THIS USED TO WORK, BUT NEEDS TO BE
   * UPDATED; datasets are removed after ~1 month
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testRtofs() throws Throwable {
    String2.log(
        "\n****************** EDDGridAggregateExistingDimension.testRtofs() *****************\n");
    // testVerboseOn();
    String tName, results, expected;
    int language = 0;
    EDDGrid eddGrid =
        (EDDGrid) EDDGridAggregateExistingDimension.oneFromDatasetsXml(null, "RTOFSWOC1");
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time",
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_rtofs",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = "time\n" + "UTC\n" + "2009-07-01T00:00:00Z\n" + "2009-07-02T00:00:00Z\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /** This tests the /files/ "files" system. This requires nceiOisst2Agg in the localhost ERDDAP. */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testFiles() throws Throwable {

    String2.log("\n*** EDDGridSideBySide.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    try {
      // get /files/datasetID/.csv
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/.csv");
      expected = "Name,Last modified,Size,Description\n" + "nceiOisst2Agg_nc3/,NaN,NaN,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // get /files/datasetID/
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/");
      Test.ensureTrue(results.indexOf("nceiOisst2Agg&#x5f;nc3&#x2f;") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("nceiOisst2Agg_nc3/") > 0, "results=\n" + results);

      // get /files/datasetID/subdir/.csv
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/nceiOisst2Agg_nc3/.csv");
      expected =
          "Name,Last modified,Size,Description\n"
              + "avhrr-only-v2.20170330.nc,1493422520000,8305496,\n"
              + "avhrr-only-v2.20170331.nc,1493422536000,8305496,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // download a file in root

      // download a file in subdir
      results =
          String2.annotatedString(
              SSR.getUrlResponseStringNewline(
                      "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/nceiOisst2Agg_nc3/avhrr-only-v2.20170331.nc")
                  .substring(0, 50));
      expected =
          "CDF[1][0][0][0][0][0][0][0][10]\n"
              + "[0][0][0][4][0][0][0][4]time[0][0][0][1][0][0][0][4]zlev[0][0][0][1][0][0][0][3]lat[0][0][0][end]";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // try to download a non-existent dataset
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent directory
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiOisst2Agg/gibberish/\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiOisst2Agg/gibberish.csv\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file in existant subdir
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/subdir/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiOisst2Agg/subdir/gibberish.csv\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

    } catch (Throwable t) {
      throw new Exception(
          "Unexpected error. This test requires nceiOisst2Agg in the localhost ERDDAP.", t);
    }
  }

  /** */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGenerateDatasetsXml() throws Throwable {
    String2.log("\n*** EDDGridAggregateExistingDimension.testGenerateDatasetsXml()\n");
    String results, expected;

    String url =
        "https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml";
    // ******* test thredds -- lame test: the datasets can't actually be aggregated
    results =
        EDDGridAggregateExistingDimension.generateDatasetsXml(
            "thredds", url, ".*", 1440); // recursive
    String suggDatasetID = EDDGridAggregateExistingDimension.suggestDatasetID(url);
    expected =
        "<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + "\n"
            + "<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_077f_0be4_21d3\" active=\"true\">\n"
            + "    <sourceUrl>https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/1day</sourceUrl>\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"cols\" type=\"int\">8640</att>\n"
            + "        <att name=\"composite\">true</att>\n"
            + "        <att name=\"contributor_name\">NASA GSFC (OBPG)</att>\n"
            + "        <att name=\"contributor_role\">Source of level 2 data.</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0, CWHDF</att>\n"
            + "        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n"
            + "        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n"
            + "        <att name=\"cwhdf_version\">3.4</att>\n"
            + "        <att name=\"date_created\">2013-04-18Z</att>\n"
            + // changes
            "        <att name=\"date_issued\">2013-04-18Z</att>\n"
            + // changes
            "        <att name=\"Easternmost_Easting\" type=\"double\">360.0</att>\n"
            + "        <att name=\"et_affine\" type=\"doubleList\">0.0 0.041676313961565174 0.04167148975575877 0.0 0.0 -90.0</att>\n"
            + "        <att name=\"gctp_datum\" type=\"int\">12</att>\n"
            + "        <att name=\"gctp_parm\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n"
            + "        <att name=\"gctp_sys\" type=\"int\">0</att>\n"
            + "        <att name=\"gctp_zone\" type=\"int\">0</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"double\">90.0</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"double\">-90.0</att>\n"
            + "        <att name=\"geospatial_lat_resolution\" type=\"double\">0.041676313961565174</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"double\">360.0</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_lon_resolution\" type=\"double\">0.04167148975575877</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_vertical_positive\">up</att>\n"
            + "        <att name=\"geospatial_vertical_units\">m</att>\n"
            + "        <att name=\"history\">NASA GSFC (OBPG)\n"
            + "2013-04-18T13:37:12Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD</att>\n"
            + // changes
            "        <att name=\"id\">LMHchlaS1day_20130415120000</att>\n"
            + // changes
            "        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n"
            + "        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n"
            + "        <att name=\"Northernmost_Northing\" type=\"double\">90.0</att>\n"
            + "        <att name=\"origin\">NASA GSFC (OBPG)</att>\n"
            + "        <att name=\"pass_date\" type=\"int\">15810</att>\n"
            + // changes
            "        <att name=\"polygon_latitude\" type=\"doubleList\">-90.0 90.0 90.0 -90.0 -90.0</att>\n"
            + "        <att name=\"polygon_longitude\" type=\"doubleList\">0.0 0.0 360.0 360.0 0.0</att>\n"
            + "        <att name=\"processing_level\">3</att>\n"
            + "        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n"
            + "        <att name=\"projection\">geographic</att>\n"
            + "        <att name=\"projection_type\">mapped</att>\n"
            + "        <att name=\"references\">Aqua/MODIS information: http://oceancolor.gsfc.nasa.gov/ . MODIS information: http://coastwatch.noaa.gov/modis_ocolor_overview.html .</att>\n"
            + "        <att name=\"rows\" type=\"int\">4320</att>\n"
            + "        <att name=\"satellite\">Aqua</att>\n"
            + "        <att name=\"sensor\">MODIS</att>\n"
            + "        <att name=\"source\">satellite observation: Aqua, MODIS</att>\n"
            + "        <att name=\"Southernmost_Northing\" type=\"double\">-90.0</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n"
            + "        <att name=\"start_time\" type=\"double\">0.0</att>\n"
            + "        <att name=\"summary\">NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.</att>\n"
            + "        <att name=\"time_coverage_end\">2013-04-16T00:00:00Z</att>\n"
            + // changes
            "        <att name=\"time_coverage_start\">2013-04-15T00:00:00Z</att>\n"
            + // changes
            "        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</att>\n"
            + "        <att name=\"Westernmost_Easting\" type=\"double\">0.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cols\">null</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://coastwatch.pfeg.noaa.gov</att>\n"
            + "        <att name=\"cwhdf_version\">null</att>\n"
            + "        <att name=\"date_created\">2013-04-18</att>\n"
            + // changes
            "        <att name=\"date_issued\">2013-04-18</att>\n"
            + // changes
            "        <att name=\"et_affine\">null</att>\n"
            + "        <att name=\"gctp_datum\">null</att>\n"
            + "        <att name=\"gctp_parm\">null</att>\n"
            + "        <att name=\"gctp_sys\">null</att>\n"
            + "        <att name=\"gctp_zone\">null</att>\n"
            + "        <att name=\"infoUrl\">https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/1day.html</att>\n"
            + "        <att name=\"institution\">NOAA CoastWatch WCN</att>\n"
            + "        <att name=\"keywords\">1day, altitude, aqua, chemistry, chla, chlorophyll, chlorophyll-a, coast, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, daily, data, day, degrees, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll, global, imaging, latitude, longitude, MHchla, moderate, modis, national, noaa, node, npp, ocean, ocean color, oceans, orbiting, partnership, polar, polar-orbiting, quality, resolution, science, science quality, sea, seawater, spectroradiometer, time, water, wcn, west</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n"
            + "        <att name=\"pass_date\">null</att>\n"
            + "        <att name=\"polygon_latitude\">null</att>\n"
            + "        <att name=\"polygon_longitude\">null</att>\n"
            + "        <att name=\"project\">CoastWatch (https://coastwatch.noaa.gov/)</att>\n"
            + "        <att name=\"references\">Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .</att>\n"
            + "        <att name=\"rows\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"start_time\">null</att>\n"
            + "        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality (1day), 2003-2013</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">1.3660272E9 1.3660272E9</att>\n"
            + // both
            // change
            "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"long_name\">Centered Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>altitude</sourceName>\n"
            + "        <destinationName>altitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Height</att>\n"
            + "            <att name=\"_CoordinateZisPositive\">up</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"long_name\">Altitude</att>\n"
            + "            <att name=\"positive\">up</att>\n"
            + "            <att name=\"standard_name\">altitude</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">-90.0 90.0</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">4</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 360.0</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">4</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>MHchla</sourceName>\n"
            + "        <destinationName>MHchla</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.001"
            +
            // trailing 0, an odd difference between Java 1.6 and Java 1.7
            (System.getProperty("java.version").startsWith("1.6") ? "0" : "")
            + " 63.914</att>\n"
            + // changes was 63.845, was 63.624, was 63.848
            "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">2</att>\n"
            + "            <att name=\"long_name\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"numberOfObservations\" type=\"int\">1952349</att>\n"
            + // changes
            "            <att name=\"percentCoverage\" type=\"double\">0.05230701838991769</att>\n"
            + // changes
            "            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n"
            + "            <att name=\"units\">mg m-3</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.03</att>\n"
            + "            <att name=\"colorBarScale\">Log</att>\n"
            + "            <att name=\"ioos_category\">Ocean Color</att>\n"
            + "            <att name=\"numberOfObservations\">null</att>\n"
            + "            <att name=\"percentCoverage\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n"
            + "<sourceUrls serverType=\"thredds\" regex=\".*\" recursive=\"true\" pathRegex=\".*\">https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml</sourceUrls>\n"
            + "\n"
            + "</dataset>\n"
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ****** test HYRAX
    results =
        EDDGridAggregateExistingDimension.generateDatasetsXml(
            "hyrax",
            "https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html",
            "month_[0-9]{8}_v11l35flk\\.nc\\.gz", // note: v one one L
            1440);
    url = "https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html";
    suggDatasetID = EDDGridAggregateExistingDimension.suggestDatasetID(url);
    expected =
        "<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + "\n"
            + "<dataset type=\"EDDGridFromDap\" datasetID=\"nasa_jpl_ef57_7a3b_e14d\" active=\"true\">\n"
            + "    <sourceUrl>https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz</sourceUrl>\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"base_date\" type=\"shortList\">1988 1 1</att>\n"
            + "        <att name=\"Conventions\">COARDS</att>\n"
            + "        <att name=\"description\">Time average of level3.0 products for the period: 1988-01-01 to 1988-01-31</att>\n"
            + "        <att name=\"history\">Created by NASA Goddard Space Flight Center under the NASA REASoN CAN: A Cross-Calibrated, Multi-Platform Ocean Surface Wind Velocity Product for Meteorological and Oceanographic Applications</att>\n"
            + "        <att name=\"title\">Atlas FLK v1.1 derived surface winds (level 3.5)</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">support-podaac@earthdata.nasa.gov</att>\n"
            + "        <att name=\"creator_name\">NASA GSFC MEaSUREs, NOAA</att>\n"
            + "        <att name=\"creator_type\">group</att>\n"
            + "        <att name=\"creator_url\">https://podaac.jpl.nasa.gov/dataset/CCMP_MEASURES_ATLAS_L4_OW_L3_0_WIND_VECTORS_FLK</att>\n"
            + "        <att name=\"infoUrl\">https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html</att>\n"
            + "        <att name=\"institution\">NASA GSFC, NOAA</att>\n"
            + "        <att name=\"keywords\">atlas, atmosphere, atmospheric, center, component, data, derived, downward, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Wind Stress, eastward, eastward_wind, flight, flk, goddard, gsfc, latitude, level, longitude, meters, month, nasa, noaa, nobs, northward, northward_wind, number, observations, oceanography, physical, physical oceanography, pseudostress, science, space, speed, statistics, stress, surface, surface_downward_eastward_stress, surface_downward_northward_stress, time, u-component, u-wind, upstr, uwnd, v-component, v-wind, v1.1, v11l35flk, vpstr, vwnd, wind, wind_speed, winds, wspd</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">Time average of level3.0 products for the period: 1988-01-01 to 1988-01-31</att>\n"
            + "        <att name=\"title\">Atlas FLK v1.1 derived surface winds (level 3.5) (month 19880101 v11l35flk), 0.25°</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">8760.0 8760.0</att>\n"
            + "            <att name=\"avg_period\">0000-01-00 00:00:00</att>\n"
            + "            <att name=\"delta_t\">0000-01-00 00:00:00</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"units\">hours since 1987-01-01 00:00:0.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">hours since 1987-01-01T00:00:00.000Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-78.375 78.375</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.125 359.875</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>uwnd</sourceName>\n"
            + "        <destinationName>uwnd</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-20.97617 21.307398</att>\n"
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
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-18.43927 19.008484</att>\n"
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
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.10927376 22.478634</att>\n"
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
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-446.93918 478.96118</att>\n"
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
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-356.3739 407.52307</att>\n"
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
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.0 124.0</att>\n"
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
            + "\n"
            + "<sourceUrls serverType=\"hyrax\" regex=\"month_[0-9]{8}_v11l35flk\\.nc\\.gz\" recursive=\"true\" pathRegex=\".*\">https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html</sourceUrls>\n"
            + "\n"
            + "</dataset>\n"
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log(
        "\n*** EDDGridAggregateExistingDimension.testGenerateDatasetsXml() finished successfully.\n");
  }

  /** This tests getDodsIndexUrls. */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testGetDodsIndexUrls() throws Exception {
    String2.log("\nEDDGridAggregateExistingDimension.testGetDodsIndexUrls");

    try {
      StringArray sourceUrls = new StringArray();
      String dir =
          "https://opendap.jpl.nasa.gov/opendap/GeodeticsGravity/tellus/L3/ocean_mass/RL05/netcdf/contents.html";
      // "http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/"; //now
      // requires authorization
      // "http://dods.marine.usf.edu/dods-bin/nph-dods/modis/"; //but it has no files
      EDDGridAggregateExistingDimension.getDodsIndexUrls(
          dir, "[0-9]{8}\\.bodas_ts\\.nc", true, "", sourceUrls);

      Test.ensureEqual(sourceUrls.get(0), dir + "19921014.bodas_ts.nc", "");
      Test.ensureEqual(sourceUrls.size(), 741, "");
      Test.ensureEqual(sourceUrls.get(740), dir + "20061227.bodas_ts.nc", "");
      String2.log("EDDGridAggregateExistingDimension.testGetDodsIndexUrls finished successfully.");
    } catch (Throwable t) {
      Test.knownProblem("CSIRO bodas now requires authorization.", "No known examples now.", t);
    }
  }
}
