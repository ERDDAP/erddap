package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import tags.TagImageComparison;
import tags.TagLocalERDDAP;
import tags.TagMissingDataset;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridLonPM180Tests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests a dataset that is initially all GT180.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  @TagImageComparison
  void testGT180() throws Throwable {
    // String2.log("\n****************** EDDGridLonPM180.testGT180()
    // *****************\n");
    // testVerboseOn();
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;
    int language = 0;

    EDDGrid eddGrid =
        (EDDGrid) EDDGridLonPM180.oneFromDatasetsXml(null, "test_erdMWchlamday_LonPM180");

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_GT180_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 2];\n"
            + "  Float64 altitude[altitude = 1];\n"
            + "  Float64 latitude[latitude = 2321];\n"
            + "  Float64 longitude[longitude = 4001];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 chlorophyll[time = 2][altitude = 1][latitude = 2321][longitude = 4001];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 2];\n"
            + "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 2321];\n"
            + "      Float64 longitude[longitude = 4001];\n"
            + "  } chlorophyll;\n"
            + "} test_erdMWchlamday_LonPM180;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_GT180_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -155.0, -105.0;\n"
            + "    String axis \"X\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 4;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    Float64 geospatial_lon_max -105.0;\n"
            + "    Float64 geospatial_lon_min -155.0;\n"
            + "    Float64 geospatial_lon_resolution 0.0125;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    Float64 Westernmost_Easting -155.0;\n" + "  }\n" + "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // lon values
    userDapQuery = "longitude[(-155):400:(-105)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_GT180_lon", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        "longitude, degrees_east, -155.0, -150.0, -145.0, -140.0, -135.0, -130.0, -125.0, -120.0, -115.0, -110.0, -105.0, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire lon range
    userDapQuery = "chlorophyll[(2002-08-16T12:00:00Z)][][(35):80:(36)][(-155):400:(-105)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_GT180_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // from same request from coastwatch erddap: erdMWchlamday lon=205 to 255
        // but I subtracted 360 from the lon values to get the values below
        "time,altitude,latitude,longitude,chlorophyll\n"
            + "UTC,m,degrees_north,degrees_east,mg m-3\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-155.0,0.06277778\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-150.0,0.091307685\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-145.0,0.06966666\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-140.0,0.0655\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-135.0,0.056166667\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-130.0,0.080142856\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-125.0,0.12250001\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-120.0,NaN\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-115.0,NaN\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-110.0,NaN\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-105.0,NaN\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-155.0,0.0695\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-150.0,0.09836364\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-145.0,0.07525001\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-140.0,0.072000004\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-135.0,0.08716667\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-130.0,0.07828571\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-125.0,0.1932857\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-120.0,NaN\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-115.0,NaN\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-110.0,NaN\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-105.0,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon subset (-119 will be rolled back to -120)
    userDapQuery = "chlorophyll[(2002-08-16T12:00:00Z)][][(35):80:(36)][(-145):400:(-119)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_GT180_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // from same request from coastwatch erddap: erdMWchlamday lon=205 to 255
        // but I subtracted 360 from the lon values to get the values below
        "time,altitude,latitude,longitude,chlorophyll\n"
            + "UTC,m,degrees_north,degrees_east,mg m-3\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-145.0,0.06966666\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-140.0,0.0655\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-135.0,0.056166667\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-130.0,0.080142856\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-125.0,0.12250001\n"
            + "2002-08-16T12:00:00Z,0.0,35.0,-120.0,NaN\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-145.0,0.07525001\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-140.0,0.072000004\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-135.0,0.08716667\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-130.0,0.07828571\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-125.0,0.1932857\n"
            + "2002-08-16T12:00:00Z,0.0,36.0,-120.0,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test image
    userDapQuery = "chlorophyll[(2002-08-16T12:00:00Z)][][][]&.land=under";
    String baseName = eddGrid.className() + "_GT180";
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

    // test of /files/ system for fromErddap in local host dataset
    String2.log("\n* Test getting /files/ from local host erddap...");
    results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:8080/cwexperimental/files/test_erdMWchlamday_LonPM180/");
    expected = "MW2002182&#x5f;2002212&#x5f;chla&#x2e;nc"; // "MW2002182_2002212_chla.nc";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    String2.log("\n*** EDDGridLonPM180.testGT180() finished.");
  }

  /**
   * This tests a dataset that is initially 1to359.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // source 404
  @TagImageComparison
  void test1to359() throws Throwable {
    // String2.log("\n****************** EDDGridLonPM180.test1to359()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.geterdRWdhws1day_LonPM180();

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_1to359_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 1065];\n"
            + "  Float64 altitude[altitude = 1];\n"
            + "  Float64 latitude[latitude = 332];\n"
            + "  Float64 longitude[longitude = 720];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 dhw[time = 1065][altitude = 1][latitude = 332][longitude = 720];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1065];\n"
            + "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 332];\n"
            + "      Float64 longitude[longitude = 720];\n"
            + "  } dhw;\n"
            + "} erdRWdhws1day_LonPM180;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_1to359_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log(results);
    expected =
        "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -179.75, 179.75;\n"
            + "    String axis \"X\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 1;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    Float64 geospatial_lon_max 179.75;\n"
            + "    Float64 geospatial_lon_min -179.75;\n"
            + "    Float64 geospatial_lon_resolution 0.5;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    Float64 Westernmost_Easting -179.75;\n" + "  }\n" + "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // lon values
    // this tests correct jump across lon 0
    userDapQuery = "longitude[(-179.75):100:(179.75)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_1to359_lon", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        "longitude, degrees_east, -179.75, -129.75, -79.75, -29.75, 20.25, 70.25, 120.25, 170.25, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon values near 0
    userDapQuery = "longitude[(-.75):1:(.75)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_1to359_lonNear0",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, -0.75, -0.25, 0.25, 0.75, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // almost entire new lon range
    userDapQuery = "dhw[(2001-10-16)][][(0):22:(1)][(-179.75):100:(179.75)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_1to359_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,dhw\n"
            + "UTC,m,degrees_north,degrees_east,weeks\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-179.75,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-129.75,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-79.75,NaN\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-29.75,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,20.25,NaN\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,70.25,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,120.25,NaN\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,170.25,0.5\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // left and right, smaller range
    userDapQuery = "dhw[(2001-10-16)][][(0):22:(1)][(-108):100:(120)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_1to359_1s", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,dhw\n"
            + "UTC,m,degrees_north,degrees_east,weeks\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-107.75,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-57.75,NaN\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-7.75,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,42.25,NaN\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,92.25,0.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, just <0 (ask for into positive, but it will roll back)
    userDapQuery = "dhw[(2001-10-16)][][(0):22:(1)][(-179.97):100:(10)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_1to359_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // verified in browser from coastwatch nodcPH2sstd1day lon 180.02 to 323.99
        "time,altitude,latitude,longitude,dhw\n"
            + "UTC,m,degrees_north,degrees_east,weeks\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-179.75,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-129.75,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-79.75,NaN\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,-29.75,0.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, just > 0
    userDapQuery = "dhw[(2001-10-16)][][(0):22:(1)][(35.97):100:(179.97)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_1to359_3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // verified in browser from coastwatch erdRWdhws1day lon 35.97 to 179.93
        "time,altitude,latitude,longitude,dhw\n"
            + "UTC,m,degrees_north,degrees_east,weeks\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,35.75,NaN\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,85.75,0.0\n"
            + "2001-10-16T12:00:00Z,0.0,0.25,135.75,0.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test image
    userDapQuery = "dhw[(2001-10-16T12:00:00Z)][][][]&.land=under";
    String baseName = eddGrid.className() + "_1to359";
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

    // THIS TEST NEEDS A NEW DATASET because source is now from tds, so no files
    // available
    // test of /files/ system for fromErddap in local host dataset
    // String2.log("\n* Test getting /files/ from local host erddap...");
    // results = SSR.getUrlResponseStringUnchanged(
    // "http://localhost:8080/cwexperimental/files/erdRWdhws1day_LonPM180/");
    // expected = "PH2001244&#x5f;2001273&#x5f;dhw&#x2e;nc";
    // //"PH2001244_2001273_dhw.nc";
    // Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    String2.log("\n*** EDDGridLonPM180.test1to359 finished.");
  }

  /**
   * This tests a dataset that is initially 0to360.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // Souce 404
  @TagImageComparison
  void test0to360() throws Throwable {
    // String2.log("\n****************** EDDGridLonPM180.test0to360()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettest_erdMHsstnmday_LonPM180();

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_0to360_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 133];\n"
            + "  Float64 altitude[altitude = 1];\n"
            + "  Float64 latitude[latitude = 4320];\n"
            + "  Float64 longitude[longitude = 8639];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 sst[time = 133][altitude = 1][latitude = 4320][longitude = 8639];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 133];\n"
            + "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 4320];\n"
            + "      Float64 longitude[longitude = 8639];\n"
            + "  } sst;\n"
            + "} test_erdMHsstnmday_LonPM180;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_0to360_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log(results);
    expected =
        "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -179.97916425512213, 179.97916425512213;\n"
            + "    String axis \"X\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 3;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    Float64 geospatial_lon_max 179.97916425512213;\n"
            + "    Float64 geospatial_lon_min -179.97916425512213;\n"
            + "    Float64 geospatial_lon_resolution 0.04167148975575877;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    Float64 Westernmost_Easting -179.97916425512213;\n" + "  }\n" + "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // lon values
    // this tests correct jump across lon 0
    userDapQuery = "longitude[(-179.98):1234:(179.98)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_0to360_lon", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        "longitude, degrees_east, -179.97916425512213, -128.55654589651581, -77.13392753790947, -25.711309179303157, 25.71130917930316, 77.13392753790947, 128.5565458965158, 179.97916425512213, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon values near 0
    userDapQuery = "longitude[(-.125):1:(.125)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_0to360_lonNear0",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        // verified in browser with longitude[(359.88):1:(360)] and
        // longitude[(0):1:(.125)]
        "longitude, degrees_east, -0.12501446926728477, -0.08334297951154213, -0.04167148975574264, 0.0, 0.04167148975575877, 0.08334297951151753, 0.1250144692672763, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire new lon range
    userDapQuery = "sst[(2007-08-16T12:00:00Z)][][(0):23:(1)][(-179.98):1234:(179.98)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_0to360_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + "UTC,m,degrees_north,degrees_east,degree_C\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-179.97916425512213,28.06726\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-128.55654589651581,22.61092\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-77.13392753790947,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-25.711309179303157,25.09381\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,25.71130917930316,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,77.13392753790947,29.521\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,128.5565458965158,27.90518\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,179.97916425512213,27.96184\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-179.97916425512213,28.14974\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-128.55654589651581,22.72137\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-77.13392753790947,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-25.711309179303157,25.48109\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,25.71130917930316,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,77.13392753790947,29.48155\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,128.5565458965158,27.58245\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,179.97916425512213,28.20998\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, just <0 (ask for into positive, but it will roll back)
    userDapQuery = "sst[(2007-08-16T12:00:00Z)][][(0):23:(1)][(-179.98):1234:(10)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_0to360_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // verified in browser from coastwatch erdMHsstnmday lon 180.02 to 334.3
        "time,altitude,latitude,longitude,sst\n"
            + "UTC,m,degrees_north,degrees_east,degree_C\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-179.97916425512213,28.06726\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-128.55654589651581,22.61092\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-77.13392753790947,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-25.711309179303157,25.09381\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-179.97916425512213,28.14974\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-128.55654589651581,22.72137\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-77.13392753790947,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-25.711309179303157,25.48109\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, just > 0
    userDapQuery = "sst[(2007-08-16T12:00:00Z)][][(0):23:(1)][(25.7):1234:(179.98)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_0to360_3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // verified in browser from coastwatch erdMHsstnmday lon 25.7 to 179.98
        "time,altitude,latitude,longitude,sst\n"
            + "UTC,m,degrees_north,degrees_east,degree_C\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,25.71130917930316,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,77.13392753790947,29.521\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,128.5565458965158,27.90518\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,179.97916425512213,27.96184\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,25.71130917930316,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,77.13392753790947,29.48155\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,128.5565458965158,27.58245\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,179.97916425512213,28.20998\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // values near 0
    userDapQuery = "sst[(2007-08-16T12:00:00Z)][][(0):23:(1)][(-.125):1:(.125)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_0to360_4", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // This is an important test of no duplicate lon values (from lon 0 and 360)
        // verified in browser from coastwatch erdMHsstnmday lon 359.875 to 360, and 0
        // to 0.125
        "time,altitude,latitude,longitude,sst\n"
            + "UTC,m,degrees_north,degrees_east,degree_C\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-0.12501446926728477,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-0.08334297951154213,24.58318\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,-0.04167148975574264,25.307896\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,0.0,25.46173\n"
            + // 360 has NaN, 0 has 25.46173!
            "2007-08-16T12:00:00Z,0.0,0.020838156980772737,0.04167148975575877,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,0.08334297951151753,25.25877\n"
            + "2007-08-16T12:00:00Z,0.0,0.020838156980772737,0.1250144692672763,25.175934\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-0.12501446926728477,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-0.08334297951154213,25.42515\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,-0.04167148975574264,25.410454\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,0.0,25.75864\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,0.04167148975575877,NaN\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,0.08334297951151753,25.90351\n"
            + "2007-08-16T12:00:00Z,0.0,0.9793933780967734,0.1250144692672763,25.788765\n";
    // In this dataset, lon=360 has all NaNs! Kooky!
    // So it is really good that EDDGridLonPM180 uses values from 0, not values from
    // 360!
    // see
    // sst[(2007-08-16):1:(2007-08-16)][(0.0):1:(0.0)][(-90):1:(90)][(360):1:(360)]
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test image
    userDapQuery = "sst[(2007-08-16T12:00:00Z)][][][]&.land=under";
    String baseName = eddGrid.className() + "_0to360";
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

    // !!!??? what's with that gap near lon=0? It's from the source.
    // The source has odd gaps at lon 358 to 360, see
    // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHsstnmday.graph?sst[(2013-09-16T00:00:00Z)][(0.0)][(-2.479741):(-0.479278)][(357.999768):(360)]&.draw=surface&.vars=longitude|latitude|sst&.colorBar=|||||
    // 2015-11-25 I notified Roy, but these are deprecated datasets so probably
    // won't be fixed

    // String2.log("\n*** EDDGridLonPM180.test0to360 finished.");
  }

  /** This tests badFilesFlag. */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testBadFilesFlag() throws Throwable {
    // String2.log("\n*** EDDGridLonPM180.testBadFilesFlag()\n" +
    // "This test requires that test_erdPHsstamday_LonPM180 be loaded in the local
    // ERDDAP." );
    int language = 0;
    String datasetID = "test_erdPHsstamday_LonPM180";
    String childDatasetID = "test_erdPHsstamday_LonPM180Child";
    String request = "http://localhost:8080/cwexperimental/griddap/" + datasetID + ".csvp?time";
    String results, expected;

    // set badFileFlag (to delete the badFiles.nc file and reload the dataset)
    File2.writeToFile88591(EDStatic.fullBadFilesFlagDirectory + datasetID, "doesn't matter");

    // wait 10 seconds and test that all times are present
    String2.log(
        "I set the badFileFlag. Now I'm waiting 30 seconds while localhost ERDDAP reloads the dataset.");
    Math2.sleep(30000); // sometimes computer is super slow

    // test that all times are present
    results = SSR.getUrlResponseStringUnchanged(request);
    String fullExpected = "time (UTC)\n" + "1981-09-16T00:00:00Z\n" + "1981-10-16T12:00:00Z\n";
    Test.ensureEqual(results, fullExpected, "results=\n" + results);

    // mark a file bad
    EDDGridLonPM180 edd = (EDDGridLonPM180) EDDTestDataset.gettest_erdPHsstamday_LonPM180();
    EDDGridFromFiles childEdd = (EDDGridFromFiles) edd.getChildDataset(language);
    Table fileTable = childEdd.getFileTable(); // throws Throwable
    int dirIndex = fileTable.getColumn(EDDGridFromFiles.FT_DIR_INDEX_COL).getInt(0);
    String fileName = fileTable.getColumn(EDDGridFromFiles.FT_FILE_LIST_COL).getString(0);
    long lastMod = fileTable.getColumn(EDDGridFromFiles.FT_LAST_MOD_COL).getLong(0);
    childEdd.addBadFileToTableOnDisk(
        dirIndex, fileName, lastMod, "for EDDGridLonPM180.testBadFilesFlag()");

    // set regular flag
    File2.writeToFile88591(EDStatic.fullResetFlagDirectory + datasetID, "doesn't matter");

    // wait 10 seconds and test that that time point is gone
    String2.log(
        "I marked a file as bad. Now I'm waiting 30 seconds while localhost ERDDAP reloads the dataset.");
    Math2.sleep(30000); // sometimes computer is super slow
    results = SSR.getUrlResponseStringUnchanged(request);
    expected = "time (UTC)\n" + "1981-10-16T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // set badFileFlag (to delete the badFiles.nc file and reload the dataset)
    File2.writeToFile88591(EDStatic.fullBadFilesFlagDirectory + datasetID, "doesn't matter");

    // wait 10 seconds and test that all times are present
    String2.log(
        "I set the badFileFlag. Now I'm waiting 20 seconds while localhost ERDDAP reloads the dataset.");
    Math2.sleep(20000);
    results = SSR.getUrlResponseStringUnchanged(request);
    Test.ensureEqual(results, fullExpected, "results=\n" + results);
  }

  /** This tests hardFlag. */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testHardFlag() throws Throwable {
    // String2.log("\n*** EDDGridLonPM180.testHardFlag()\n" +
    // "This test requires hawaii_d90f_20ee_c4cb and
    // hawaii_d90f_20ee_c4cb_LonPM180\n" +
    // "be loaded in the local ERDDAP.");

    // set hardFlag
    String startTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
    Math2.sleep(1000);
    File2.writeToFile88591(
        EDStatic.fullHardFlagDirectory + "hawaii_d90f_20ee_c4cb_LonPM180", "test");
    String2.log(
        "I just set a hardFlag for hawaii_d90f_20ee_c4cb_LonPM180.\n"
            + "Now I'm waiting 10 seconds.");
    Math2.sleep(10000);
    // flush the log file
    String tIndex =
        SSR.getUrlResponseStringUnchanged("http://localhost:8080/cwexperimental/status.html");
    Math2.sleep(2000);
    // read the log file
    String tLog = File2.readFromFileUtf8(EDStatic.fullLogsDirectory + "log.txt")[1];
    String expected = // ***
        /*
         * "deleting cached dataset info for datasetID=hawaii_d90f_20ee_c4cb_LonPM180Child\n"
         * +
         * "\\*\\*\\* unloading datasetID=hawaii_d90f_20ee_c4cb_LonPM180\n" +
         * "\\*\\*\\* deleting cached dataset info for datasetID=hawaii_d90f_20ee_c4cb_LonPM180\n"
         * +
         * "\n" +
         * "\\*\\*\\* RunLoadDatasets is starting a new hardFlag LoadDatasets thread at (..........T..............)\n"
         * +
         * "\n" +
         * "\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\n"
         * +
         * "LoadDatasets.run EDStatic.developmentMode=true ..........T..............\n"
         * +
         * "  datasetsRegex=\\(hawaii_d90f_20ee_c4cb_LonPM180\\) inputStream=null majorLoad=false"
         * ;
         */

        "deleting cached dataset info for datasetID=hawaii_d90f_20ee_c4cb_LonPM180Child\n"
            + "File2.deleteIfOld\\(/erddapBPD/dataset/ld/hawaii_d90f_20ee_c4cb_LonPM180Child/\\) nDir=   . nDeleted=   . nRemain=   .\n"
            + "\\*\\*\\* unloading datasetID=hawaii_d90f_20ee_c4cb_LonPM180\n"
            + "nActions=0\n"
            + "\\*\\*\\* deleting cached dataset info for datasetID=hawaii_d90f_20ee_c4cb_LonPM180\n"
            + "File2.deleteIfOld\\(/erddapBPD/dataset/80/hawaii_d90f_20ee_c4cb_LonPM180/  \\) nDir=   . nDeleted=   . nRemain=   .\n"
            + "\n"
            + "\\*\\*\\* RunLoadDatasets is starting a new hardFlag LoadDatasets thread at (..........T..............)\n"
            + "\n"
            + "\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\n"
            + "LoadDatasets.run EDStatic.developmentMode=true ..........T..............\n"
            + "  datasetsRegex=\\(hawaii_d90f_20ee_c4cb_LonPM180\\) inputStream=null majorLoad=false";

    int po = Math.max(0, tLog.lastIndexOf(expected.substring(0, 78)));
    int po2 = tLog.indexOf("majorLoad=false", po) + 15;
    String tResults = tLog.substring(po, po2);
    String2.log("\ntResults=<quote>" + tResults + "</quote>\n");
    Test.testLinesMatch(tResults, expected, "tResults and expected don't match!");

    // so far so good, tResults matches expected
    int po3 = tResults.indexOf("thread at ");
    String reloadTime = tResults.substring(po3 + 10, po3 + 35);
    String2.log(" startTime=" + startTime + "\n" + "reloadTime=" + reloadTime);
    Test.ensureTrue(startTime.compareTo(reloadTime) < 0, "startTime is after reloadTime?!");

    // test that child was successfully constructed after that
    int po4 =
        tLog.indexOf(
            "*** EDDGridFromErddap hawaii_d90f_20ee_c4cb_LonPM180Child constructor finished. TIME=",
            po);
    Test.ensureTrue(po4 > po, "po4=" + po4 + " isn't greater than po=" + po + " !");

    // test that parent was successfully constructed after that
    int po5 =
        tLog.indexOf(
            "*** EDDGridLonPM180 hawaii_d90f_20ee_c4cb_LonPM180 constructor finished. TIME=", po4);
    Test.ensureTrue(po5 > po4, "po5=" + po5 + " isn't greater than po4=" + po4 + " !");
  }

  /**
   * This tests the /files/ "files" system. This requires local_erdMWchlamday_LonPM180 in the
   * localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testFiles() throws Throwable {

    String2.log("\n*** EDDGridLonPM180.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/local_erdMWchlamday_LonPM180/.csv");
    expected =
        "Name,Last modified,Size,Description\n"
            + "MW2002182_2002212_chla.nc.gz,1332025888000,17339003,\n"
            + "MW2002213_2002243_chla.nc.gz,1332026460000,18217295,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/local_erdMWchlamday_LonPM180/");
    Test.ensureTrue(
        results.indexOf("MW2002182&#x5f;2002212&#x5f;chla&#x2e;nc") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">17339003<") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv

    // download a file in root
    results =
        String2.annotatedString(
            SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/local_erdMWchlamday_LonPM180/MW2002182_2002212_chla.nc.gz")
                .substring(0, 50));
    expected =
        "[31][139][8][8] [26]eO[0][3]MW2002182_2002212_chla.nc[0][228][216][127][140]#[231]][199]q[223][229][238]r?[end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // download a file in subdir

    // try to download a non-existent dataset
    try {
      results =
          SSR.getUrlResponseStringNewline("http://localhost:8080/cwexperimental/files/gibberish/");
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
              "http://localhost:8080/cwexperimental/files/local_erdMWchlamday_LonPM180/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/local_erdMWchlamday_LonPM180/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/local_erdMWchlamday_LonPM180/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/local_erdMWchlamday_LonPM180/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existant subdir
  }

  /**
   * This tests that longitude valid_min and valid_max in child are removed by constructor, because
   * they are now out-of-date. E.g., source dataset may have lon valid_min=0 and valid_max=360,
   * which becomes incorrect in the -180 to 180 dataset.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testValidMinMax() throws Throwable {
    // String2.log("\n****************** EDDGridLonPM180.testValidMinMax()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettestPM180LonValidMinMax();
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_validMinMax_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);

    expected =
        "longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range -179.875, 179.875;\n"
            + "    String axis \"X\";\n"
            + "    String grids \"Uniform grid from 0.125 to 359.875 by 0.25\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            +
            // " Float32 valid_max 359.875;\n" + //removed because out-of-date
            // " Float32 valid_min 0.125;\n" +
            "  }";
    po = results.indexOf("longitude {");
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "Float64 geospatial_lon_max 179.875;\n" + "    Float64 geospatial_lon_min -179.875;";
    po = results.indexOf("Float64 geospatial_lon_max");
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);
  }
}
