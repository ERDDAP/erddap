package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import tags.TagImageComparison;
import tags.TagIncompleteTest;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridLon0360Tests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests a dataset that is initially -180.018 to 180.018 (min and max beyond bounds are
   * dropped by this dataset)
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // Flaky (wait then try again error)
  @TagImageComparison
  void testPM181() throws Throwable {
    // String2.log("\n****************** EDDGridLon0360.testPM181()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettest_nesdisVHNchlaWeekly_Lon0360();
    /* */
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_PM181_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    results = results.replaceAll("\\[time = \\d{3}\\]", "[time = ###]");
    expected =
        "Dataset {\n"
            + "  Float64 time[time = ###];\n"
            + // changes
            "  Float64 altitude[altitude = 1];\n"
            + "  Float64 latitude[latitude = 4788];\n"
            + "  Float64 longitude[longitude = 9600];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 chlor_a[time = ###][altitude = 1][latitude = 4788][longitude = 9600];\n"
            + // changes
            "    MAPS:\n"
            + "      Float64 time[time = ###];\n"
            + // changes
            "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 4788];\n"
            + "      Float64 longitude[longitude = 9600];\n"
            + "  } chlor_a;\n"
            + "} test_nesdisVHNchlaWeekly_Lon0360;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_PM181_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log(results);
    expected =
        "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range 0.018749999999981976, 359.98125;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    Float64 geospatial_lon_max 359.98125;\n"
            + "    Float64 geospatial_lon_min 0.018749999999981976;\n"
            + "    Float64 geospatial_lon_resolution 0.037500000000000006;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    Float64 Westernmost_Easting 0.018749999999981976;\n" + "  }\n" + "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // lon values
    // this tests entire range
    userDapQuery = "longitude[0]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM181_lon0", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 0.018749999999981976, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    userDapQuery = "longitude[last]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_PM181_lonLast",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 359.98125, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    userDapQuery = "longitude[(0.01875):1000:(359.98125)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM181_lon", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        "longitude, degrees_east, 0.018749999999981976, 37.51874999999998, 75.01874999999998, 112.51874999999998, 150.01874999999998, 187.51874999999998, 225.01874999999998, 262.51874999999995, 300.01874999999995, 337.51874999999995, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon values near 180
    userDapQuery = "longitude[(179.93):1:(180.07)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_PM181_lonNear0",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        "longitude, degrees_east, 179.94374999999997, 179.98125, 180.01874999999998, 180.05624999999998, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire new lon range. beginTime of this and other star.nesdis datasets creeps
    // forward.
    // I think they just keep one year's worth of data.
    // So now I request a recent time (without too many mv's), and just have to
    // adjust it once per year.
    userDapQuery =
        "chlor_a[(2023-02-28T12:00:00Z)][][(27.0188):1:(26.99)][(0.98):1900:(359.98)]"; // 2022=08-17
    // was
    // 2021-07-14,
    // 2022-08-17
    // was
    // 2022-08-03
    // was
    // 2021-07-12,
    // 2022-07-25
    // was
    // 2021-06-25.
    // 2022-05-10:
    // was
    // 2021-04-27.
    // 2022-05-10:
    // was
    // 2021-04-12,
    // now
    // that
    // is
    // <min
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM181_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = // corresponding PM180 lons: -145.25625, -74.00625, -2.75625
        "time,altitude,latitude,longitude,chlor_a\n"
            + "UTC,m,degrees_north,degrees_east,mg m^-3\n"
            + "2023-02-28T12:00:00Z,0.0,27.01875,0.9937499999999819,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,72.24374999999998,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,143.49374999999998,0.16088724\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,214.74374999999998,0.13979226\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,285.99375,0.08678202\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,357.24375,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,0.9937499999999819,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,72.24374999999998,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,143.49374999999998,0.17107606\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,214.74374999999998,0.16705023\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,285.99375,0.086618856\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,357.24375,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 0-180 subset
    userDapQuery =
        "chlor_a[(2023-02-28T12:00:00Z)][][(27.0188):1:(26.99)][(0.98):1900:(179.9)]"; // beginTime
    // of
    // this
    // and
    // other
    // star.nesdis
    // datasets
    // creeps
    // forward.
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM181_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    // https://coastwatch.pfeg.noaa.gov/erddap/griddap/nesdisVHNchlaWeekly.htmlTable?chlor_a%5B(2021-03-05T12:00:00Z):1:(2021-03-05T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B(27.018749999999996):1:(26.99)%5D%5B(0.98):1900:(179.98)%5D
    expected =
        "time,altitude,latitude,longitude,chlor_a\n"
            + "UTC,m,degrees_north,degrees_east,mg m^-3\n"
            + "2023-02-28T12:00:00Z,0.0,27.01875,0.9937499999999819,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,72.24374999999998,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,143.49374999999998,0.16316497\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,0.9937499999999819,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,72.24374999999998,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,143.49374999999998,0.17747125\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 0-181 subset with not relevant spillover //yes, it is correctly handled by
    // "all from new left"
    userDapQuery =
        "chlor_a[(2023-02-28T12:00:00Z)][][(27.0188):1:(26.99)][(0.98):1900:(181)]"; // beginTime
    // of this
    // and
    // other
    // star.nesdis
    // datasets
    // creeps
    // forward.
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM181_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    // https://coastwatch.pfeg.noaa.gov/erddap/griddap/nesdisVHNchlaWeekly.htmlTable?chlor_a%5B(2021-03-22T12:00:00Z):1:(2021-03-22T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B(27.018749999999996):1:(26.99)%5D%5B(0.98):1900:(179.98)%5D
    expected =
        "time,altitude,latitude,longitude,chlor_a\n"
            + "UTC,m,degrees_north,degrees_east,mg m^-3\n"
            + "2023-02-28T12:00:00Z,0.0,27.01875,0.9937499999999819,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,72.24374999999998,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,143.49374999999998,0.16316497\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,0.9937499999999819,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,72.24374999999998,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,143.49374999999998,0.17747125\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 180-360 subset
    userDapQuery =
        "chlor_a[(2023-02-28T12:00:00Z)][][(27.0188):1:(26.99)][(214.74):1900:(359.98)]"; // beginTime
    // of
    // this
    // and
    // other
    // star.nesdis
    // datasets
    // creeps
    // forward.
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM181_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    // https://coastwatch.pfeg.noaa.gov/erddap/griddap/nesdisVHNchlaWeekly.htmlTable?chlor_a%5B(2021-03-22T12:00:00Z):1:(2021-03-22T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B(27.018749999999996):1:(26.99)%5D%5B(-145.26):1900:(-0.98)%5D
    expected = // corresponding PM180 lons: -145.25625, -74.00625, -2.75625
        "time,altitude,latitude,longitude,chlor_a\n"
            + "UTC,m,degrees_north,degrees_east,mg m^-3\n"
            + "2023-02-28T12:00:00Z,0.0,27.01875,214.74374999999998,0.10357797\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,285.99375,0.08001077\n"
            + //
            "2023-02-28T12:00:00Z,0.0,27.01875,357.24375,NaN\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,214.74374999999998,0.10924427\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,285.99375,0.075266525\n"
            + //
            "2023-02-28T12:00:00Z,0.0,26.98125,357.24375,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);

    // test image entire world
    userDapQuery =
        "chlor_a[(2023-02-28T12:00:00Z)][][][]&.land=under"; // beginTime of this and other
    // star.nesdis
    // datasets creeps forward.
    String baseName = eddGrid.className() + "_PM181_entireWorld";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // test image subset near 180
    userDapQuery =
        "chlor_a[(2023-02-28T12:00:00Z)][][(71):(-61)][(120):(245)]&.land=under"; // beginTime of
    // this
    // and
    // other
    // star.nesdis
    // datasets
    // creeps
    // forward.
    baseName = eddGrid.className() + "_PM181_subsetNear180";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // test image just new left
    userDapQuery =
        "chlor_a[(2023-02-28T12:00:00Z)][][(41):(-61)][(10):(175)]&.land=under"; // beginTime of
    // this and
    // other
    // star.nesdis
    // datasets
    // creeps
    // forward.
    baseName = eddGrid.className() + "_PM181_justNewLeft";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // test image just new right
    userDapQuery =
        "chlor_a[(2023-02-28T12:00:00Z)][][(41):(-61)][(190):(355)]&.land=under"; // beginTime of
    // this
    // and
    // other
    // star.nesdis
    // datasets
    // creeps
    // forward.
    baseName = eddGrid.className() + "_PM181_justNewRight";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // SOURCE is ultimately a remote THREDDS so no source files
    // String2.log("\n* Test getting /files/ from local host erddap...");
    // results = SSR.getUrlResponseStringUnchanged(
    // "http://localhost:8080/cwexperimental/files/erdRWdhws1day_Lon0360/");
    // expected = "PH2001244&#x5f;2001273&#x5f;dhw&#x2e;nc";
    // //"PH2001244_2001273_dhw.nc";
    // Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    /*  */
    // String2.log("\n*** EDDGridLon0360.testPM181 finished.");
  }

  /**
   * This tests that longitude valid_min and valid_max in child are removed by constructor, because
   * they are now out-of-date. E.g., source dataset may have lon valid_min=-180 and valid_max=180,
   * which becomes incorrect in the 0360 dataset.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testValidMinMax() throws Throwable {
    // String2.log("\n****************** EDDGridLon0360.testValidMinMax()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.getecocast_Lon0360();
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_validMinMax_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);

    expected =
        "longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range 228.4086, 244.32899808000002;\n"
            + "    String axis \"X\";\n"
            + "    String comment \"Longitude values are the centers of the grid cells\";\n"
            + "    String coverage_content_type \"coordinate\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            +
            // " Float64 valid_max -115;\n" + //removed because out-of-date
            // " Float64 valid_min -132;\n" +
            "  }";
    po = results.indexOf("longitude {");
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "Float64 geospatial_lon_max 244.32899808000002;\n"
            + "    Float64 geospatial_lon_min 228.4086;";
    po = results.indexOf("Float64 geospatial_lon_max");
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);
  }

  /**
   * This tests a dataset that is initially -180 to 180.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testPM180() throws Throwable {
    // String2.log("\n****************** EDDGridLon0360.testPM180()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettest_etopo180_Lon0360();

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_PM180_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 latitude[latitude = 10801];\n"
            + "  Float64 longitude[longitude = 21600];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int16 altitude[latitude = 10801][longitude = 21600];\n"
            + "    MAPS:\n"
            + "      Float64 latitude[latitude = 10801];\n"
            + "      Float64 longitude[longitude = 21600];\n"
            + "  } altitude;\n"
            + "} test_etopo180_Lon0360;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_PM180_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log(results);
    expected =
        " longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range 0.0, 359.98333333333335;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    Float64 geospatial_lon_max 359.98333333333335;\n"
            + "    Float64 geospatial_lon_min 0.0;\n"
            + "    Float64 geospatial_lon_resolution 0.016666666666666666;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    Float64 Westernmost_Easting 0.0;\n" + "  }\n" + "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // all lon values near 0
    userDapQuery = "longitude[(179.94):(180.06)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_PM180_lonNear0",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        "longitude, degrees_east, 179.93333333333334, 179.95, 179.96666666666664, 179.98333333333335, 180.0, 180.01666666666668, 180.03333333333333, 180.05, 180.06666666666666, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // this tests correct jump across lon 180
    userDapQuery = "longitude[(179.95):3:(180.05)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_PM180_lon_jumpa",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 179.95, 180.0, 180.05, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // this tests correct jump across lon 180
    userDapQuery = "longitude[(179.966):3:(180.03)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_PM180_lon_jumpb",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 179.96666666666664, 180.01666666666668, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // this tests correct jump across lon 180
    userDapQuery = "longitude[(179.983):3:(180.04)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM180__jumpc", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 179.98333333333335, 180.03333333333333, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // this tests correct jump across lon 180
    userDapQuery = "longitude[(179.999):3:(180.05)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM180__jumpd", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 180.0, 180.05, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire new lon range
    userDapQuery = "altitude[(40):60:(41)][(0):6600:(359.99)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM180_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(0):6600:(180.0)%5D
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(-140):6600:(0)%5D
        "latitude,longitude,altitude\n"
            + "degrees_north,degrees_east,m\n"
            + "40.0,0.0,1\n"
            + "40.0,110.0,1374\n"
            + "40.0,220.0,-4514\n"
            + "40.0,330.0,-1486\n"
            + "41.0,0.0,521\n"
            + "41.0,110.0,1310\n"
            + "41.0,220.0,-4518\n"
            + "41.0,330.0,-1998\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, just <180
    userDapQuery = "altitude[(40):60:(41)][(0):6600:(120)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM180_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(0):6600:(180.0)%5D
        "latitude,longitude,altitude\n"
            + "degrees_north,degrees_east,m\n"
            + "40.0,0.0,1\n"
            + "40.0,110.0,1374\n"
            + "41.0,0.0,521\n"
            + "41.0,110.0,1310\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, just > 180
    userDapQuery = "altitude[(40):60:(41)][(220):6600:(359.99)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM180_3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(-140):6600:(0)%5D
        "latitude,longitude,altitude\n"
            + "degrees_north,degrees_east,m\n"
            + "40.0,220.0,-4514\n"
            + "40.0,330.0,-1486\n"
            + "41.0,220.0,-4518\n"
            + "41.0,330.0,-1998\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // bang on 180
    userDapQuery = "altitude[(40):60:(41)][(0):5400:(359.99)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_PM180_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(0):5400:(180.0)%5D
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(-180):5400:(0)%5D
        "latitude,longitude,altitude\n"
            + "degrees_north,degrees_east,m\n"
            + "40.0,0.0,1\n"
            + "40.0,90.0,795\n"
            + "40.0,180.0,-5241\n"
            + "40.0,270.0,183\n"
            + "41.0,0.0,521\n"
            + "41.0,90.0,1177\n"
            + "41.0,180.0,-5644\n"
            + "41.0,270.0,215\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    // test full image
    userDapQuery = "altitude[][]&.land=under";
    String baseName = eddGrid.className() + "_PM180";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // test new left image
    userDapQuery = "altitude[][(0):(170)]&.land=under";
    baseName = eddGrid.className() + "_PM180left";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // test new image
    userDapQuery = "altitude[][(190):(359.99)]&.land=under";
    baseName = eddGrid.className() + "_PM180right";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    String2.log("\n*** EDDGridLon0360.testPM180 finished.");
  }

  /**
   * This tests a dataset that is initially Insert.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testInsert() throws Throwable {
    // String2.log("\n****************** EDDGridLon0360.testInsert()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;
    EDDGrid eddGrid;

    // test notApplicable (dataset maxLon already <180)
    // TODO dataset 404s, find replacement
    // try {
    // eddGrid = (EDDGrid) EDDTestDataset.getnotApplicable_Lon0360();
    // throw new RuntimeException("shouldn't get here");
    // } catch (Throwable t) {
    // String msg = MustBe.throwableToString(t);
    // if (msg.indexOf("Error in EDDGridLon0360(notApplicable_Lon0360)
    // constructor:\n" +
    // "The child longitude axis has no values <0 (min=90.0)!") < 0)
    // throw t;
    // }

    // testInsert
    eddGrid = (EDDGrid) EDDTestDataset.gettestLon0360Insert();

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_Insert_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 1];\n"
            + "  Float32 latitude[latitude = 61];\n"
            + "  Float32 longitude[longitude = 360];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 sst[time = 1][latitude = 61][longitude = 360];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1];\n"
            + "      Float32 latitude[latitude = 61];\n"
            + "      Float32 longitude[longitude = 360];\n"
            + "  } sst;\n"
            + "} testLon0360Insert;\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_Insert_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log(results);
    expected =
        "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range 0.5, 359.5;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    Float64 geospatial_lon_max 359.5;\n"
            + "    Float64 geospatial_lon_min 0.5;\n"
            + "    Float64 geospatial_lon_resolution 1.0;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    Float64 Westernmost_Easting 0.5;\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "results=\n" + results);

    // lon values near 25
    userDapQuery = "longitude[(22.5):1:(27.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_Insert_lonNear0",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 22.5, 23.5, 24.5, 25.5, 26.5, 27.5, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon values near 330
    userDapQuery = "longitude[(327.5):1:(332.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_Insert_lonNear0",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 327.5, 328.5, 329.5, 330.5, 331.5, 332.5, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire new lon range
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][(-33.5):(-34.5)][(0.5):20:(359.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_Insert_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // verified by cw erddap
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdHadISST.htmlTable?sst[(2020-12-16T12)][(-33.5):1:(-34.5)][(0.5):20:(20.5)]
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdHadISST.htmlTable?sst[(2020-12-16T12)][(-33.5):1:(-34.5)][(-19.5):20:(-19.5)]
        "time,latitude,longitude,sst\n"
            + "UTC,degrees_north,degrees_east,C\n"
            + "2020-12-16T12:00:00Z,-33.5,0.5,19.392572\n"
            + "2020-12-16T12:00:00Z,-33.5,20.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,40.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,60.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,80.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,100.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,120.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,140.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,160.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,180.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,200.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,220.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,240.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,260.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,280.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,300.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,320.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,340.5,20.45493\n"
            + "2020-12-16T12:00:00Z,-34.5,0.5,18.77771\n"
            + "2020-12-16T12:00:00Z,-34.5,20.5,19.726223\n"
            + "2020-12-16T12:00:00Z,-34.5,40.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,60.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,80.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,100.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,120.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,140.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,160.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,180.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,200.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,220.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,240.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,260.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,280.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,300.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,320.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,340.5,19.598171\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just left
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][(-33.5):(-34.5)][(0.5):20:(20.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_Insert_1L", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,sst\n"
            + "UTC,degrees_north,degrees_east,C\n"
            + "2020-12-16T12:00:00Z,-33.5,0.5,19.392572\n"
            + "2020-12-16T12:00:00Z,-33.5,20.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,0.5,18.77771\n"
            + "2020-12-16T12:00:00Z,-34.5,20.5,19.726223\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just insert
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][(-33.5):(-34.5)][(160.5):20:(200.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_Insert_1I", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,sst\n"
            + "UTC,degrees_north,degrees_east,C\n"
            + "2020-12-16T12:00:00Z,-33.5,160.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,180.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,200.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,160.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,180.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,200.5,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just left part of insert
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][(-33.5):(-34.5)][(140.5):20:(160.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_Insert_1Ib", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,sst\n"
            + "UTC,degrees_north,degrees_east,C\n"
            + "2020-12-16T12:00:00Z,-33.5,140.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,160.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,140.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,160.5,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just right
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][(-33.5):(-34.5)][(340.5):10:(350.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_Insert_1R", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,sst\n"
            + "UTC,degrees_north,degrees_east,C\n"
            + "2020-12-16T12:00:00Z,-33.5,340.5,20.45493\n"
            + "2020-12-16T12:00:00Z,-33.5,350.5,20.543941\n"
            + "2020-12-16T12:00:00Z,-34.5,340.5,19.598171\n"
            + "2020-12-16T12:00:00Z,-34.5,350.5,19.714016\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, left + insert
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][(-33.5):(-34.5)][(0.5):20:(60.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_Insert_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,sst\n"
            + "UTC,degrees_north,degrees_east,C\n"
            + "2020-12-16T12:00:00Z,-33.5,0.5,19.392572\n"
            + "2020-12-16T12:00:00Z,-33.5,20.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,40.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,60.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,0.5,18.77771\n"
            + "2020-12-16T12:00:00Z,-34.5,20.5,19.726223\n"
            + "2020-12-16T12:00:00Z,-34.5,40.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,60.5,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, insert + right
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][(-33.5):(-34.5)][(320.5):20:(340.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_Insert_3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,sst\n"
            + "UTC,degrees_north,degrees_east,C\n"
            + "2020-12-16T12:00:00Z,-33.5,320.5,NaN\n"
            + "2020-12-16T12:00:00Z,-33.5,340.5,20.45493\n"
            + "2020-12-16T12:00:00Z,-34.5,320.5,NaN\n"
            + "2020-12-16T12:00:00Z,-34.5,340.5,19.598171\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, left + right (jump over insert)
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][(-33.5):(-34.5)][(0.5):340:(340.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_Insert_3b", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,sst\n"
            + "UTC,degrees_north,degrees_east,C\n"
            + "2020-12-16T12:00:00Z,-33.5,0.5,19.392572\n"
            + "2020-12-16T12:00:00Z,-33.5,340.5,20.45493\n"
            + "2020-12-16T12:00:00Z,-34.5,0.5,18.77771\n"
            + "2020-12-16T12:00:00Z,-34.5,340.5,19.598171\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test image
    userDapQuery = "sst[(2020-12-16T12:00:00Z)][][]&.land=under";
    String baseName = eddGrid.className() + "_testMissingValue_Insert";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // String2.log("\n*** EDDGridLon0360.testInsert finished.");
    // debugMode = oDebugMode;
  }
}
