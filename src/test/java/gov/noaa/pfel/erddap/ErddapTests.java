package gov.noaa.pfel.erddap;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;

import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.util.EDStatic;
import tags.TagIncompleteTest;
import tags.TagLocalERDDAP;
import testDataset.EDDTestDataset;

class ErddapTests {
  @BeforeAll
  static void init() {
    File2.setWebInfParentDirectory();
    System.setProperty("erddapContentDirectory", System.getProperty("user.dir") + "\\content\\erddap");
    System.setProperty("doSetupValidation", String.valueOf(false));
    EDD.debugMode = true;
  }

  /**
   * Test Convert Nearest Data.
   *
   */
  @org.junit.jupiter.api.Test
  void testConvertInterpolate() throws Throwable {
    String2.log("\n*** Erddap.testConvertInterpolate");
    Table table;
    String results, expected;
    int language = 0;
    long time;
    ConcurrentHashMap<String, EDDGrid> tGridDatasetHashMap = new ConcurrentHashMap();
    tGridDatasetHashMap.put("testGriddedNcFiles",
        (EDDGrid) EDDTestDataset.gettestGriddedNcFiles());
    // boolean oDebugMode = debugMode;
    // debugMode = true;

    // test error: no TLL table
    try {
      table = Erddap.interpolate(language, tGridDatasetHashMap,
          "",
          "testGriddedNcFiles/x_wind/Nearest/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Query error: TimeLatLonTable (nothing) is invalid.", "");

    // test error: no TLL rows
    try {
      table = Erddap.interpolate(language, tGridDatasetHashMap,
          "ID,latitude,longitude,time\n",
          "testGriddedNcFiles/x_wind/Nearest/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Query error: TimeLatLonTable is invalid.", "");

    // test error: no specification
    try {
      table = Erddap.interpolate(language, tGridDatasetHashMap,
          "ID,latitude,longitude,time\n" +
              "TC1,33.125,176.875,2008-01-10T12Z\n" +
              "TC1,33.125,176.900,2008-01-10T12Z\n" +
              "TC1,33.125,177.100,2008-01-10T12Z\n" +
              "TC1,33.125,177.125,2008-01-10T12Z\n",
          "");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Query error: requestCSV (nothing) is invalid.", "");

    // test error: incomplete specification
    try {
      table = Erddap.interpolate(language, tGridDatasetHashMap,
          "ID,latitude,longitude,time\n" +
              "TC1,33.125,176.875,2008-01-10T12Z\n" +
              "TC1,33.125,176.900,2008-01-10T12Z\n" +
              "TC1,33.125,177.100,2008-01-10T12Z\n" +
              "TC1,33.125,177.125,2008-01-10T12Z\n",
          "testGriddedNcFiles");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results,
        "Query error: datasetID/variable/algorithm/nearby value=\"testGriddedNcFiles\" is invalid.", "");

    // test error: unknown dataset
    try {
      table = Erddap.interpolate(language, tGridDatasetHashMap,
          "ID,latitude,longitude,time\n" +
              "TC1,33.125,176.875,2008-01-10T12Z\n" +
              "TC1,33.125,176.900,2008-01-10T12Z\n" +
              "TC1,33.125,177.100,2008-01-10T12Z\n" +
              "TC1,33.125,177.125,2008-01-10T12Z\n",
          "junk/x_wind/Nearest/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Error: datasetID=junk wasn't found.", "");

    // test error: unknown var
    try {
      table = Erddap.interpolate(language, tGridDatasetHashMap,
          "ID,latitude,longitude,time\n" +
              "TC1,33.125,176.875,2008-01-10T12Z\n" +
              "TC1,33.125,176.900,2008-01-10T12Z\n" +
              "TC1,33.125,177.100,2008-01-10T12Z\n" +
              "TC1,33.125,177.125,2008-01-10T12Z\n",
          "testGriddedNcFiles/junk/Nearest/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Error: destinationVariableName=junk wasn't found in datasetID=testGriddedNcFiles.", "");

    // test error: unknown algorithm
    try {
      table = Erddap.interpolate(language, tGridDatasetHashMap,
          "ID,latitude,longitude,time\n" +
              "TC1,33.125,176.875,2008-01-10T12Z\n" +
              "TC1,33.125,176.900,2008-01-10T12Z\n" +
              "TC1,33.125,177.100,2008-01-10T12Z\n" +
              "TC1,33.125,177.125,2008-01-10T12Z\n",
          "testGriddedNcFiles/x_wind/junk/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Query error: algorithm in testGriddedNcFiles/x_wind/junk/1 is invalid. " +
        "(must be one of Nearest, Bilinear, Mean, SD, Median, Scaled, InverseDistance, InverseDistance2, InverseDistance4, InverseDistance6)",
        "");

    // test error: unsupported nearby
    try {
      table = Erddap.interpolate(language, tGridDatasetHashMap,
          "ID,latitude,longitude,time\n" +
              "TC1,33.125,176.875,2008-01-10T12Z\n" +
              "TC1,33.125,176.900,2008-01-10T12Z\n" +
              "TC1,33.125,177.100,2008-01-10T12Z\n" +
              "TC1,33.125,177.125,2008-01-10T12Z\n",
          "testGriddedNcFiles/x_wind/Nearest/3");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Query error: 'nearby' value in testGriddedNcFiles/x_wind/Nearest/3 is invalid.", "");

    /*
     * http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles.htmlTable?
     * x_wind[(2008-01-09T12:00:00Z):1:(2008-01-10T12:00:00Z)]
     * [(0.0):1:(0.0)][(32.875):1:(33.625)][(176.625):1:(177.375)]
     * time altitude latitude longitude x_wind
     * UTC m d_north d_east m s-1
     * 2008-01-09T12:00:00Z 0.0 32.625 176.625 12.452795
     * 2008-01-09T12:00:00Z 0.0 32.625 176.875 12.282755
     * 2008-01-09T12:00:00Z 0.0 32.625 177.125 11.99785
     * 2008-01-09T12:00:00Z 0.0 32.625 177.375 12.354391
     * 2008-01-09T12:00:00Z 0.0 32.875 176.625 12.689985
     * 2008-01-09T12:00:00Z 0.0 32.875 176.875 12.002501
     * 2008-01-09T12:00:00Z 0.0 32.875 177.125 11.9996195
     * 2008-01-09T12:00:00Z 0.0 32.875 177.375 12.855539
     * 2008-01-09T12:00:00Z 0.0 33.125 176.625 13.6933
     * 2008-01-09T12:00:00Z 0.0 33.125 176.875 11.938499
     * 2008-01-09T12:00:00Z 0.0 33.125 177.125 11.453595
     * 2008-01-09T12:00:00Z 0.0 33.125 177.375 11.90157
     * 2008-01-09T12:00:00Z 0.0 33.375 176.625 13.9815
     * 2008-01-09T12:00:00Z 0.0 33.375 176.875 12.8286495
     * 2008-01-09T12:00:00Z 0.0 33.375 177.125 13.00185
     * 2008-01-09T12:00:00Z 0.0 33.375 177.375 11.661514
     * 2008-01-10T12:00:00Z 0.0 32.625 176.625 17.5809
     * 2008-01-10T12:00:00Z 0.0 32.625 176.875 20.372
     * 2008-01-10T12:00:00Z 0.0 32.625 177.125 13.870621
     * 2008-01-10T12:00:00Z 0.0 32.625 177.375 12.935455
     * 2008-01-10T12:00:00Z 0.0 32.875 176.625 15.3107
     * 2008-01-10T12:00:00Z 0.0 32.875 176.875 16.7248
     * 2008-01-10T12:00:00Z 0.0 32.875 177.125
     * 2008-01-10T12:00:00Z 0.0 32.875 177.375 6.22317
     * 2008-01-10T12:00:00Z 0.0 33.125 176.625 14.7641
     * 2008-01-10T12:00:00Z 0.0 33.125 176.875
     * 2008-01-10T12:00:00Z 0.0 33.125 177.125 5.72859
     * 2008-01-10T12:00:00Z 0.0 33.125 177.375 5.2508
     * 2008-01-10T12:00:00Z 0.0 33.375 176.625 9.068385
     * 2008-01-10T12:00:00Z 0.0 33.375 176.875 9.7424
     * 2008-01-10T12:00:00Z 0.0 33.375 177.125 4.60233
     * 2008-01-10T12:00:00Z 0.0 33.375 177.375 4.44326
     */
    // Nearest/1
    time = System.currentTimeMillis();
    table = Erddap.interpolate(language, tGridDatasetHashMap,
        "ID,latitude,longitude,time\n" +
            "TC1,33.125,176.875,2008-01-10T10Z\n" + // right on NaN
            "TC1,33.100,176.9,2008-01-10T11Z\n" +
            "TC1,33.100,177.1,2008-01-10T12Z\n" +
            "TC1,33.125,177.125,2008-01-10T13Z\n",
        "testGriddedNcFiles/x_wind/Nearest/1,testGriddedNcFiles/x_wind/Nearest/4,testGriddedNcFiles/x_wind/Bilinear/4,"
            +
            "testGriddedNcFiles/x_wind/Mean/4,testGriddedNcFiles/x_wind/SD," +
            "testGriddedNcFiles/x_wind/Median/4,testGriddedNcFiles/x_wind/Scaled/4," +
            "testGriddedNcFiles/x_wind/InverseDistance/4,testGriddedNcFiles/x_wind/InverseDistance2/4," +
            "testGriddedNcFiles/x_wind/InverseDistance4/4,testGriddedNcFiles/x_wind/InverseDistance6/4");
    time = System.currentTimeMillis() - time;
    String2.log("elapsedTime=" + time);
    results = table.dataToString();
    expected =
        // for me, it is essential to see this layout when validating results below by
        // hand
        //
        // time [9] 2008-01-10T12Z
        // 176.625 176.875 177.125 177.375
        // ---------------------------------------
        // [493] 33.375 9.068385 9.7424 4.60233 4.44326
        // [492] 33.125 14.7641 x 5.72859 5.2508
        // [491] 32.875 15.3107 16.7248 6.22317
        // [490] 32.625 17.5809 20.372 13.870621 12.935455

        // time [8] 2008-01-09T12Z (approx data)
        // 176.625 176.875 177.125 177.375
        // ---------------------------------------
        // [493] 33.375 ... 12.82865 13.00185 11.6615
        // [492] 33.125 ... 11.9385 11.453595 11.90157
        // [491] 32.875 ... 12.0025 11.9916 12.8555
        // [490] 32.625 ...

        // nearest 4, first point: if equally close points, it picks nearby lon value
        // (since closer on globe)
        // ! all these tested with calculator
        "ID,latitude,longitude,time,testGriddedNcFiles_x_wind_Nearest_1,testGriddedNcFiles_x_wind_Nearest_4,testGriddedNcFiles_x_wind_Bilinear_4,"
            +
            "testGriddedNcFiles_x_wind_Mean_4,testGriddedNcFiles_x_wind_SD,testGriddedNcFiles_x_wind_Median_4,testGriddedNcFiles_x_wind_Scaled_4,"
            +
            "testGriddedNcFiles_x_wind_InverseDistance_4,testGriddedNcFiles_x_wind_InverseDistance2_4,testGriddedNcFiles_x_wind_InverseDistance4_4,testGriddedNcFiles_x_wind_InverseDistance6_4\n"
            +
            "TC1,33.125,176.875,2008-01-10T10Z,,5.72859," + // right on NaN // nearest1, nearest4,
            "5.72859," + // bilinear4
            "6.691106666666667,7.299908651433333,5.72859,7.735494999999999," + // mean4, sd4, median4, scaled4
            "6.9171001610992295,7.108862,7.387365555555555,7.551191176470589\n" + // invDist, invDist2, invDist4,
                                                                                  // invDist6,
            "TC1,33.1,176.9,2008-01-10T11Z,,5.72859," + // close to NaN
            "6.82821100000025," +
            "11.226695,60.458317182049974,11.226695,11.226695," + // scaled4 is test of both valid nearby dataset points
                                                                  // are equidistant
            "11.226695,11.226695,11.226694999999998,11.226695\n" + // all invDist are same because both valid nearby
                                                                   // dataset points are equidistant, so get equal wt
            "TC1,33.1,177.1,2008-01-10T12Z,5.72859,5.72859," + // close to 5.72859
            "6.82821100000025," +
            "11.226695,60.458317182049974,11.226695,5.72859," +
            "6.828211000000249,5.862690121951286,5.730265740627859,5.728610691270167\n" + // increasing wt to closest
                                                                                          // dataset point: 5.72859
            "TC1,33.125,177.125,2008-01-10T13Z,5.72859,5.72859," + // right on 5.72859
            "5.72859," +
            "5.006245,0.35389629483333246,4.926565,5.72859," +
            "5.72859,5.72859,5.72859,5.72859\n"; // all same because atm point is right on dataset point
    // ok: nearest1, nearest4, mean,
    Test.ensureEqual(results, expected, "results=\n" + results);

    /*
     * http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles.htmlTable?
     * x_wind[(2008-01-09T12:00:00Z)][(0.0)][(33.125):1:(33.375)][(176.875):1:(177.
     * 125)]
     * time alt lat longitude x_wind
     * UTC m deg_n deg_east m s-1
     * 2008-01-09T12:00:00Z 0.0 33.125 176.875 11.938499
     * 2008-01-09T12:00:00Z 0.0 33.125 177.125 11.453595
     * 2008-01-09T12:00:00Z 0.0 33.375 176.875 12.8286495
     * 2008-01-09T12:00:00Z 0.0 33.375 177.125 13.00185
     */

    // Nearest/8, Mean/8 (3D)
    time = System.currentTimeMillis();
    table = Erddap.interpolate(language, tGridDatasetHashMap,
        "ID,latitude,longitude,time\n" +
            "TC1,33.125,176.875,2008-01-10T10Z\n" +
            "TC1,33.1,176.900,2008-01-10T10Z\n" +
            "TC1,33.1,177.100,2008-01-10T10Z\n" +
            "TC1,33.125,177.125,2008-01-10T10Z\n" +
            "TC1,33.125,176.875,2008-01-10T06Z\n" +
            "TC1,33.125,176.875,2008-01-09T18Z\n" +
            "TC1,33.125,176.875,2008-01-09T12Z\n",
        "testGriddedNcFiles/x_wind/Nearest/8,testGriddedNcFiles/x_wind/Mean/8");
    time = System.currentTimeMillis() - time;
    String2.log("elapsedTime=" + time);
    results = table.dataToString();
    expected = // verified by hand
        "ID,latitude,longitude,time,testGriddedNcFiles_x_wind_Nearest_8,testGriddedNcFiles_x_wind_Mean_8\n" +
            "TC1,33.125,176.875,2008-01-10T10Z,11.938499,9.899416214285715\n" +
            "TC1,33.1,176.9,2008-01-10T10Z,5.72859,11.641267416666665\n" +
            "TC1,33.1,177.1,2008-01-10T10Z,5.72859,11.641267416666665\n" +
            "TC1,33.125,177.125,2008-01-10T10Z,5.72859,8.505438625\n" + // nearest: mv->next lon
            "TC1,33.125,176.875,2008-01-10T06Z,11.938499,9.899416214285715\n" + // nearest: mv->prev time
            "TC1,33.125,176.875,2008-01-09T18Z,11.938499,9.899416214285715\n" +
            "TC1,33.125,176.875,2008-01-09T12Z,11.938499,9.899416214285715\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Nearest/16, Mean/16 (2D)
    time = System.currentTimeMillis();
    table = Erddap.interpolate(language, tGridDatasetHashMap,
        "ID,latitude,longitude,time\n" +
            "TC1,33.1,176.900,2008-01-10T10Z\n",
        "testGriddedNcFiles/x_wind/Nearest/16," +
            "testGriddedNcFiles/x_wind/Mean/4,testGriddedNcFiles/x_wind/Mean/16,testGriddedNcFiles/x_wind/Mean/36," +
            "testGriddedNcFiles/x_wind/Mean/8,testGriddedNcFiles/x_wind/Mean/64,testGriddedNcFiles/x_wind/Mean/216");
    time = System.currentTimeMillis() - time;
    String2.log("elapsedTime=" + time);
    results = table.dataToString();
    expected = // verified by hand
        "ID,latitude,longitude,time,testGriddedNcFiles_x_wind_Nearest_16," +
            "testGriddedNcFiles_x_wind_Mean_4,testGriddedNcFiles_x_wind_Mean_16,testGriddedNcFiles_x_wind_Mean_36," +
            "testGriddedNcFiles_x_wind_Mean_8,testGriddedNcFiles_x_wind_Mean_64,testGriddedNcFiles_x_wind_Mean_216\n" +
            "TC1,33.1,176.9,2008-01-10T10Z,5.72859," +
            "11.226695,11.18696507142857,9.728878844117647," + // /4 /16 by hand, /36 looks good
            "11.641267416666665,11.813308039215682,14.658405367015714\n"; // look good
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Scaled/x
    time = System.currentTimeMillis();
    table = Erddap.interpolate(language, tGridDatasetHashMap,
        "ID,latitude,longitude,time\n" +
            "TC1,33.1,176.900,2008-01-10T10Z\n",
        "testGriddedNcFiles/x_wind/Scaled/4,testGriddedNcFiles/x_wind/Scaled/16,testGriddedNcFiles/x_wind/Scaled/36," +
            "testGriddedNcFiles/x_wind/Scaled/8,testGriddedNcFiles/x_wind/Scaled/64,testGriddedNcFiles/x_wind/Scaled/216");
    time = System.currentTimeMillis() - time;
    String2.log("elapsedTime=" + time);
    results = table.dataToString();
    expected = // verified by hand
        "ID,latitude,longitude,time," +
            "testGriddedNcFiles_x_wind_Scaled_4,testGriddedNcFiles_x_wind_Scaled_16,testGriddedNcFiles_x_wind_Scaled_36,"
            +
            "testGriddedNcFiles_x_wind_Scaled_8,testGriddedNcFiles_x_wind_Scaled_64,testGriddedNcFiles_x_wind_Scaled_216\n"
            +
            "TC1,33.1,176.9,2008-01-10T10Z," +
            "11.226695,11.26523262746242,10.791734445086393," + // looks good (based on the debug msg showing pts
                                                                // involved)
            "11.474244755147355,11.677036187464559,12.158548484000905\n"; // looks good (test of duplicating values
                                                                          // beyond dataset's time values)
    Test.ensureEqual(results, expected, "results=\n" + results);

    // debugMode = oDebugMode;

  }

  /**
   * This is used by Bob to do simple tests of the basic Erddap services
   * from the ERDDAP at EDStatic.erddapUrl. It assumes Bob's test datasets are
   * available.
   *
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testBasic() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String results, expected;
    String2.log("\n*** Erddap.testBasic");
    int po;
    int language = 0;
    EDStatic.sosActive = false; // currently, never true because sos is unfinished //some other tests may have
                                // left this as true

    // home page
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl); // redirects to index.html
    expected = "The small effort to set up ERDDAP brings many benefits.";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/"); // redirects to index.html
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // test version info (opendap spec section 7.2.5)
    // "version" instead of datasetID
    expected = "Core Version: DAP/2.0\n" +
        "Server Version: dods/3.7\n" +
        "ERDDAP_version: " + EDStatic.erddapVersion + "\n";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/version");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/version");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // "version.txt"
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/version.txt");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/version.txt");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ".ver"
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/etopo180.ver");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.ver");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // help
    expected = "griddap to Request Data and Graphs from Gridded Datasets";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/documentation.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMHchla8day.help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    expected = "tabledap to Request Data and Graphs from Tabular Datasets";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/documentation.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // error 404
    results = "";
    try {
      SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/gibberish");
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureTrue(results.indexOf("java.io.FileNotFoundException") >= 0, "results=\n" + results);

    // info list all datasets
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.csv?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") < 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.tsv");
    Test.ensureTrue(results.indexOf("\t") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

    // search
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Do a Full Text Search for Datasets") >= 0, "results=\n" + results);
    // index.otherFileType must have ?searchFor=...

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery + "&searchFor=all");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\n") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">GLOBEC NEP Rosette Bottle Data (2002)") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.htmlTable?" +
        EDStatic.defaultPIppQuery + "&searchFor=all");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\n") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(">TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\n") > 0,
        "results=\n" + results);

    // .json
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.json?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Accessible\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n"
        +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n"
        +
        "    \"rows\": [\n" +
        "      [\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/pmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n"
        +
        "      [\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/rPmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/rPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rPmelTaoDySst\"],\n"
        +
        "      [\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/rlPmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n"
        +
        "    ]\n" +
        "  }\n" +
        "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .json with jsonp
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.json?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel&.jsonp=fnName");
    expected = "fnName({\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Accessible\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n"
        +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n"
        +
        "    \"rows\": [\n" +
        "      [\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/pmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n"
        +
        "      [\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/rPmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/rPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rPmelTaoDySst\"],\n"
        +
        "      [\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/rlPmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n"
        +
        "    ]\n" +
        "  }\n" +
        "}\n" +
        ")";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // and read the header to see the mime type
    results = String2.toNewlineString(
        SSR.dosShell("curl -i \"" + EDStatic.erddapUrl + "/search/index.json?" +
            EDStatic.defaultPIppQuery + "&searchFor=tao+pmel&.jsonp=fnName\"",
            120).toArray());
    po = results.indexOf("HTTP");
    results = results.substring(po);
    po = results.indexOf("chunked");
    results = results.substring(0, po + 7);
    expected = "HTTP/1.1 200 \n" +
        "Content-Encoding: identity\n" +
        "Content-Type: application/javascript;charset=UTF-8\n" +
        "Transfer-Encoding: chunked";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlCSV1
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlCSV1?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "[\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Accessible\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"]\n"
        +
        "[\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/pmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/pmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n"
        +
        "[\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/rPmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/rPmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rPmelTaoDySst\"]\n"
        +
        "[\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/rlPmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/rlPmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlCSV
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlCSV?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "[\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/pmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/pmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n"
        +
        "[\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/rPmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/rPmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rPmelTaoDySst\"]\n"
        +
        "[\"\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://127.0.0.1:8080/cwexperimental/files/rlPmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://127.0.0.1:8080/cwexperimental/info/rlPmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://127.0.0.1:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlKVP
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlKVP?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "{\"griddap\":\"\", \"Subset\":\"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"tabledap\":\"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst\", \"Make A Graph\":\"http://127.0.0.1:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://127.0.0.1:8080/cwexperimental/files/pmelTaoDySst/\", \"Accessible\":\"public\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"FGDC\":\"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"Info\":\"http://127.0.0.1:8080/cwexperimental/info/pmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://127.0.0.1:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"Email\":\"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySst\"}\n"
        +
        "{\"griddap\":\"\", \"Subset\":\"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"tabledap\":\"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"Make A Graph\":\"http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://127.0.0.1:8080/cwexperimental/files/rPmelTaoDySst/\", \"Accessible\":\"public\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"FGDC\":\"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"Info\":\"http://127.0.0.1:8080/cwexperimental/info/rPmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://127.0.0.1:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"Email\":\"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"rPmelTaoDySst\"}\n"
        +
        "{\"griddap\":\"\", \"Subset\":\"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"tabledap\":\"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"Make A Graph\":\"http://127.0.0.1:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://127.0.0.1:8080/cwexperimental/files/rlPmelTaoDySst/\", \"Accessible\":\"public\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"FGDC\":\"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"Info\":\"http://127.0.0.1:8080/cwexperimental/info/rlPmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://127.0.0.1:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"Email\":\"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"rlPmelTaoDySst\"}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.tsv?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    Test.ensureTrue(
        results.indexOf("\t\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\t") > 0,
        "results=\n" + results);

    // categorize
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">standard_name\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.json");
    Test.ensureEqual(results,
        "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [\"Categorize\", \"URL\"],\n" +
            "    \"columnTypes\": [\"String\", \"String\"],\n" +
            "    \"rows\": [\n" +
            "      [\"cdm_data_type\", \"http://127.0.0.1:8080/cwexperimental/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"institution\", \"http://127.0.0.1:8080/cwexperimental/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"ioos_category\", \"http://127.0.0.1:8080/cwexperimental/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"keywords\", \"http://127.0.0.1:8080/cwexperimental/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"long_name\", \"http://127.0.0.1:8080/cwexperimental/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"standard_name\", \"http://127.0.0.1:8080/cwexperimental/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"variableName\", \"http://127.0.0.1:8080/cwexperimental/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n"
            +
            "    ]\n" +
            "  }\n" +
            "}\n",
        "results=\n" + results);

    // json with jsonp
    String jsonp = "myFunctionName";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.json?.jsonp=" + SSR.percentEncode(jsonp));
    Test.ensureEqual(results,
        jsonp + "(" +
            "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [\"Categorize\", \"URL\"],\n" +
            "    \"columnTypes\": [\"String\", \"String\"],\n" +
            "    \"rows\": [\n" +
            "      [\"cdm_data_type\", \"http://127.0.0.1:8080/cwexperimental/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"institution\", \"http://127.0.0.1:8080/cwexperimental/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"ioos_category\", \"http://127.0.0.1:8080/cwexperimental/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"keywords\", \"http://127.0.0.1:8080/cwexperimental/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"long_name\", \"http://127.0.0.1:8080/cwexperimental/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"standard_name\", \"http://127.0.0.1:8080/cwexperimental/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"variableName\", \"http://127.0.0.1:8080/cwexperimental/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n"
            +
            "    ]\n" +
            "  }\n" +
            "}\n" +
            ")",
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">sea_water_temperature\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/index.json");
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"sea_water_temperature\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/institution/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">ioos_category\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">noaa_coastwatch_west_coast_node\n") >= 0,
        "results=\n" + results);

    results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/institution/index.tsv"));
    Test.ensureTrue(results.indexOf("Category[9]URL[10]") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "noaa_coastwatch_west_coast_node[9]http://127.0.0.1:8080/cwexperimental/categorize/institution/noaa_coastwatch_west_coast_node/index.tsv?page=1&itemsPerPage=1000[10]") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/sea_water_temperature/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">erdGlobecBottle\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/sea_water_temperature/index.json");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", " +
        (EDStatic.sosActive ? "\"sos\", " : "") +
        (EDStatic.wcsActive ? "\"wcs\", " : "") +
        (EDStatic.wmsActive ? "\"wms\", " : "") +
        (EDStatic.filesActive ? "\"files\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"Accessible\", " : "") +
        "\"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", " +
        (EDStatic.subscriptionSystemActive ? "\"Email\", " : "") +
        "\"Institution\", \"Dataset ID\"],\n" +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", " +
        (EDStatic.sosActive ? "\"String\", " : "") +
        (EDStatic.wcsActive ? "\"String\", " : "") +
        (EDStatic.wmsActive ? "\"String\", " : "") +
        (EDStatic.filesActive ? "\"String\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"String\", " : "") +
        "\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", " +
        (EDStatic.subscriptionSystemActive ? "\"String\", " : "") +
        "\"String\", \"String\"],\n" +
        "    \"rows\": [\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected = "http://127.0.0.1:8080/cwexperimental/tabledap/erdGlobecBottle.subset\", " +
        "\"http://127.0.0.1:8080/cwexperimental/tabledap/erdGlobecBottle\", " +
        "\"http://127.0.0.1:8080/cwexperimental/tabledap/erdGlobecBottle.graph\", " +
        (EDStatic.sosActive ? "\"\", " : "") + // currently, it isn't made available via sos
        (EDStatic.wcsActive ? "\"\", " : "") +
        (EDStatic.wmsActive ? "\"\", " : "") +
        (EDStatic.filesActive ? "\"http://127.0.0.1:8080/cwexperimental/files/erdGlobecBottle/\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"public\", " : "") +
        "\"GLOBEC NEP Rosette Bottle Data (2002)\", \"GLOBEC (GLOBal " +
        "Ocean ECosystems Dynamics) NEP (Northeast Pacific)\\nRosette Bottle Data from " +
        "New Horizon Cruise (NH0207: 1-19 August 2002).\\nNotes:\\nPhysical data " +
        "processed by Jane Fleischbein (OSU).\\nChlorophyll readings done by " +
        "Leah Feinberg (OSU).\\nNutrient analysis done by Burke Hales (OSU).\\n" +
        "Sal00 - salinity calculated from primary sensors (C0,T0).\\n" +
        "Sal11 - salinity calculated from secondary sensors (C1,T1).\\n" +
        "secondary sensor pair was used in final processing of CTD data for\\n" +
        "most stations because the primary had more noise and spikes. The\\n" +
        "primary pair were used for cast #9, 24, 48, 111 and 150 due to\\n" +
        "multiple spikes or offsets in the secondary pair.\\n" +
        "Nutrient samples were collected from most bottles; all nutrient data\\n" +
        "developed from samples frozen during the cruise and analyzed ashore;\\n" +
        "data developed by Burke Hales (OSU).\\n" +
        "Operation Detection Limits for Nutrient Concentrations\\n" +
        "Nutrient  Range         Mean    Variable         Units\\n" +
        "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\\n" +
        "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\\n" +
        "Si        0.13-0.24     0.16    Silicate         micromoles per liter\\n" +
        "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\\n" +
        "Dates and Times are UTC.\\n\\n" +
        "For more information, see https://www.bco-dmo.org/dataset/2452\\n\\n" +
        // was "http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\\n\\n"
        // +
        "Inquiries about how to access this data should be directed to\\n" +
        "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\\n\\n" +
        "cdm_data_type = TrajectoryProfile\\n" +
        "VARIABLES:\\ncruise_id\\n... (24 more variables)\\n\", " +
        "\"http://127.0.0.1:8080/cwexperimental/metadata/fgdc/xml/erdGlobecBottle_fgdc.xml\", " +
        "\"http://127.0.0.1:8080/cwexperimental/metadata/iso19115/xml/erdGlobecBottle_iso19115.xml\", " +
        "\"http://127.0.0.1:8080/cwexperimental/info/erdGlobecBottle/index.json\", " +
        "\"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\", " + // was "\"http://www.globec.org/\", " +
        "\"http://127.0.0.1:8080/cwexperimental/rss/erdGlobecBottle.rss\", " +
        (EDStatic.subscriptionSystemActive
            ? "\"http://127.0.0.1:8080/cwexperimental/subscriptions/add.html?datasetID=erdGlobecBottle&showErrors=false&email=\", "
            : "")
        +
        "\"GLOBEC\", \"erdGlobecBottle\"],";
    po = results.indexOf("http://127.0.0.1:8080/cwexperimental/tabledap/erdGlobecBottle");
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // griddap
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("List of griddap Datasets") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)\n") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(">erdMHchla8day\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/index.json?" +
        EDStatic.defaultPIppQuery + "");
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "\"SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)\"") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMHchla8day.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(Centered Time, UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("chlorophyll") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMHchla8day.graph");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("chlorophyll") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0,
        "results=\n" + results);

    // tabledap
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("List of tabledap Datasets") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(">erdGlobecBottle\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/index.json?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"GLOBEC NEP Rosette Bottle Data (2002)\"") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"erdGlobecBottle\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.graph");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Filled Square") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0,
        "results=\n" + results);

    // files
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/");
    Test.ensureTrue(
        results.indexOf(
            "ERDDAP's \"files\" system lets you browse a virtual file system and download source data files.") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("cwwcNDBCMet") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("directories") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/cwwcNDBCMet/nrt/");
    Test.ensureTrue(results.indexOf("NDBC Standard Meteorological Buoy Data") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make a graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NDBC&#x5f;41008&#x5f;met&#x2e;nc") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

    String localName = EDStatic.fullTestCacheDirectory + "NDBC_41008_met.nc";
    File2.delete(localName);
    SSR.downloadFile( // throws Exception if trouble
        EDStatic.erddapUrl + "/files/cwwcNDBCMet/nrt/NDBC_41008_met.nc",
        localName, true); // tryToUseCompression
    Test.ensureTrue(File2.isFile(localName),
        "/files download failed. Not found: localName=" + localName);
    File2.delete(localName);

    // sos
    if (EDStatic.sosActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List of SOS Datasets") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">NDBC Standard Meteorological Buoy Data") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">cwwcNDBCMet") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"NDBC Standard Meteorological Buoy Data\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"cwwcNDBCMet\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(
          "available via ERDDAP's Sensor Observation Service (SOS) web service.") >= 0,
          "results=\n" + results);

      String sosUrl = EDStatic.erddapUrl + "/sos/cwwcNDBCMet/" + EDDTable.sosServer;
      results = SSR.getUrlResponseStringUnchanged(sosUrl + "?service=SOS&request=GetCapabilities");
      Test.ensureTrue(results.indexOf("<ows:ServiceIdentification>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("<ows:Get xlink:href=\"" + sosUrl + "?\"/>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("</Capabilities>") >= 0, "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://127.0.0.1:8080/cwexperimental/sos/index.html?page=1&itemsPerPage=1000\n"
          +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: The \\\"SOS\\\" system has been disabled on this ERDDAP.\";\n" +
          "})\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected,
          "results=\n" + results);
    }

    // wcs
    if (EDStatic.wcsActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Datasets Which Can Be Accessed via WCS") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title</th>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS</th>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(">Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)</td>") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">erdMHchla8day<") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("\"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(
          "ERDDAP makes some datasets available via ERDDAP's Web Coverage Service (WCS) web service.") >= 0,
          "results=\n" + results);

      String wcsUrl = EDStatic.erddapUrl + "/wcs/erdMHchla8day/" + EDDGrid.wcsServer;
      results = SSR.getUrlResponseStringUnchanged(wcsUrl + "?service=WCS&request=GetCapabilities");
      Test.ensureTrue(results.indexOf("<CoverageOfferingBrief>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("<lonLatEnvelope srsName") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("</WCS_Capabilities>") >= 0, "results=\n" + results);
    } else {
      // wcs is inactive
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf(
          "java.io.FileNotFoundException: http://127.0.0.1:8080/cwexperimental/wcs/index.html?page=1&itemsPerPage=1000") >= 0,
          "results=\n" + results);
    }

    // wms
    if (EDStatic.wmsActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List of WMS Datasets") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results
              .indexOf(">Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\n") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">erdMHchla8day\n") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
              "\"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("display of registered and superimposed map-like views") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/erdMHchla8day/index.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("on-the-fly by ERDDAP's") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("altitude") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);
    }

    // results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
    // "/categorize/standard_name/index.html");
    // Test.ensureTrue(results.indexOf(">sea_water_temperature<") >= 0,
    // "results=\n" + results);

    // validate the various GetCapabilities documents
    /*
     * NOT ACTIVE
     * String s = https://xmlvalidation.com/ ".../xml/validate/?lang=en" +
     * "&url=" + EDStatic.erddapUrl + "/wms/" + EDD.WMS_SERVER + "?service=WMS&" +
     * "request=GetCapabilities&version=";
     * Test.displayInBrowser(s + "1.1.0");
     * Test.displayInBrowser(s + "1.1.1");
     * Test.displayInBrowser(s + "1.3.0");
     */

    // more information
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/information.html");
    Test.ensureTrue(results.indexOf(
        "ERDDAP a solution to everyone's data distribution / data access problems?") >= 0,
        "results=\n" + results);

    // subscriptions
    if (EDStatic.subscriptionSystemActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/index.html");
      Test.ensureTrue(results.indexOf("Add a new subscription") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Validate a subscription") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List your subscriptions") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Remove a subscription") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/add.html");
      Test.ensureTrue(results.indexOf(
          "To add a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/validate.html");
      Test.ensureTrue(results.indexOf(
          "To validate a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/list.html");
      Test.ensureTrue(results.indexOf(
          "To request an email with a list of your subscriptions, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/remove.html");
      Test.ensureTrue(results.indexOf(
          "To remove a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
            "/subscriptions/index.html");
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);
    }

    // slideSorter
    if (EDStatic.slideSorterActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/slidesorter.html");
      Test.ensureTrue(results.indexOf(
          "Your slides will be lost when you close this browser window, unless you:") >= 0,
          "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
            "/slidesorter.html");
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);
    }

    // embed a graph (always at coastwatch)
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/images/embed.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "Embed a Graph in a Web Page") >= 0,
        "results=\n" + results);

    // Computer Programs
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/rest.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "ERDDAP's RESTful Web Services") >= 0,
        "results=\n" + results);

    // list of services
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.csv");
    expected = "Resource,URL\n" +
        "info,http://127.0.0.1:8080/cwexperimental/info/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        "search,http://127.0.0.1:8080/cwexperimental/search/index.csv?" + EDStatic.defaultPIppQuery + "&searchFor=\n" +
        "categorize,http://127.0.0.1:8080/cwexperimental/categorize/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        "griddap,http://127.0.0.1:8080/cwexperimental/griddap/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        "tabledap,http://127.0.0.1:8080/cwexperimental/tabledap/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        (EDStatic.sosActive
            ? "sos,http://127.0.0.1:8080/cwexperimental/sos/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "")
        +
        (EDStatic.wcsActive
            ? "wcs,http://127.0.0.1:8080/cwexperimental/wcs/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "")
        +
        (EDStatic.wmsActive
            ? "wms,http://127.0.0.1:8080/cwexperimental/wms/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "");
    // subscriptions?
    // converters?
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.htmlTable?" +
        EDStatic.defaultPIppQuery);
    expected = EDStatic.startHeadHtml(0, EDStatic.erddapUrl((String) null, language), "Resources") + "\n" +
        "</head>\n" +
        EDStatic.startBodyHtml(0, null, "index.html", EDStatic.defaultPIppQuery) + // 2022-11-22 .htmlTable converted to
                                                                                   // .html to avoid user requesting all
                                                                                   // data in a dataset if they change
                                                                                   // language
        "&nbsp;<br>\n" +
        "&nbsp;\n" +
        "<table class=\"erd commonBGColor nowrap\">\n" +
        "<tr>\n" +
        "<th>Resource\n" +
        "<th>URL\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>info\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;info&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://127.0.0.1:8080/cwexperimental/info/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>search\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;search&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000&#x26;searchFor&#x3d;\">http://127.0.0.1:8080/cwexperimental/search/index.htmlTable?page=1&amp;itemsPerPage=1000&amp;searchFor=</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>categorize\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;categorize&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://127.0.0.1:8080/cwexperimental/categorize/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>griddap\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;griddap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://127.0.0.1:8080/cwexperimental/griddap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>tabledap\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://127.0.0.1:8080/cwexperimental/tabledap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        (EDStatic.sosActive ? "<tr>\n" +
            "<td>sos\n" +
            "<td><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;sos&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://127.0.0.1:8080/cwexperimental/sos/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
            +
            "</tr>\n" : "")
        +
        "<tr>\n" +
        "<td>wms\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;wms&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://127.0.0.1:8080/cwexperimental/wms/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "</table>\n" +
        EDStatic.endBodyHtml(0, EDStatic.erddapUrl((String) null, language), (String) null) + "\n" +
        "</html>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.json");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"Resource\", \"URL\"],\n" +
        "    \"columnTypes\": [\"String\", \"String\"],\n" +
        "    \"rows\": [\n" +
        "      [\"info\", \"http://127.0.0.1:8080/cwexperimental/info/index.json?page=1&itemsPerPage=1000\"],\n" +
        "      [\"search\", \"http://127.0.0.1:8080/cwexperimental/search/index.json?page=1&itemsPerPage=1000&searchFor=\"],\n"
        +
        "      [\"categorize\", \"http://127.0.0.1:8080/cwexperimental/categorize/index.json?page=1&itemsPerPage=1000\"],\n"
        +
        "      [\"griddap\", \"http://127.0.0.1:8080/cwexperimental/griddap/index.json?page=1&itemsPerPage=1000\"],\n" +
        "      [\"tabledap\", \"http://127.0.0.1:8080/cwexperimental/tabledap/index.json?page=1&itemsPerPage=1000\"]"
        + (EDStatic.sosActive || EDStatic.wcsActive || EDStatic.wmsActive ? "," : "") + "\n" +
        (EDStatic.sosActive
            ? "      [\"sos\", \"http://127.0.0.1:8080/cwexperimental/sos/index.json?page=1&itemsPerPage=1000\"]"
                + (EDStatic.wcsActive || EDStatic.wmsActive ? "," : "") + "\n"
            : "")
        +
        (EDStatic.wcsActive
            ? "      [\"wcs\", \"http://127.0.0.1:8080/cwexperimental/wcs/index.json?page=1&itemsPerPage=1000\"]"
                + (EDStatic.wmsActive ? "," : "") + "\n"
            : "")
        +
        (EDStatic.wmsActive
            ? "      [\"wms\", \"http://127.0.0.1:8080/cwexperimental/wms/index.json?page=1&itemsPerPage=1000\"]\n"
            : "")
        +
        // subscriptions?
        "    ]\n" +
        "  }\n" +
        "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.tsv"));
    expected = "Resource[9]URL[10]\n" +
        "info[9]http://127.0.0.1:8080/cwexperimental/info/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        "search[9]http://127.0.0.1:8080/cwexperimental/search/index.tsv?page=1&itemsPerPage=1000&searchFor=[10]\n" +
        "categorize[9]http://127.0.0.1:8080/cwexperimental/categorize/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        "griddap[9]http://127.0.0.1:8080/cwexperimental/griddap/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        "tabledap[9]http://127.0.0.1:8080/cwexperimental/tabledap/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        (EDStatic.sosActive ? "sos[9]http://127.0.0.1:8080/cwexperimental/sos/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        (EDStatic.wcsActive ? "wcs[9]http://127.0.0.1:8080/cwexperimental/wcs/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        (EDStatic.wmsActive ? "wms[9]http://127.0.0.1:8080/cwexperimental/wms/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        "[end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.xhtml");
    expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
        "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
        "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" +
        "<head>\n" +
        "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
        "  <title>Resources</title>\n" +
        "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://127.0.0.1:8080/cwexperimental/images/erddap2.css\" />\n"
        +
        "</head>\n" +
        "<body>\n" +
        "\n" +
        "&nbsp;\n" +
        "<table class=\"erd commonBGColor nowrap\">\n" +
        "<tr>\n" +
        "<th>Resource</th>\n" +
        "<th>URL</th>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>info</td>\n" +
        "<td>http://127.0.0.1:8080/cwexperimental/info/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>search</td>\n" +
        "<td>http://127.0.0.1:8080/cwexperimental/search/index.xhtml?page=1&amp;itemsPerPage=1000&amp;searchFor=</td>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>categorize</td>\n" +
        "<td>http://127.0.0.1:8080/cwexperimental/categorize/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>griddap</td>\n" +
        "<td>http://127.0.0.1:8080/cwexperimental/griddap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>tabledap</td>\n" +
        "<td>http://127.0.0.1:8080/cwexperimental/tabledap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        (EDStatic.sosActive ? "<tr>\n" +
            "<td>sos</td>\n" +
            "<td>http://127.0.0.1:8080/cwexperimental/sos/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        (EDStatic.wcsActive ? "<tr>\n" +
            "<td>wcs</td>\n" +
            "<td>http://127.0.0.1:8080/cwexperimental/wcs/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        (EDStatic.wmsActive ? "<tr>\n" +
            "<td>wms</td>\n" +
            "<td>http://127.0.0.1:8080/cwexperimental/wms/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        "</table>\n" +
        "</body>\n" +
        "</html>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This test the json-ld responses from the ERDDAP at EDStatic.erddapUrl.
   * It assumes jplMURSST41 datasets is available.
   *
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testJsonld() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String results, expected;
    String2.log("\n*** Erddap.testJsonld");
    int po;

    // info list all datasets
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.html?" +
        EDStatic.defaultPIppQuery);

    // json-ld all datasets
    expected = "<script type=\"application/ld+json\">\n" +
        "{\n" +
        "  \"@context\": \"http://schema.org\",\n" +
        "  \"@type\": \"DataCatalog\",\n" +
        "  \"name\": \"ERDDAP Data Server at NOAA NMFS SWFSC Environmental Research Division\",\n" +
        "  \"url\": \"https://127.0.0.1:8443/cwexperimental\",\n" +
        "  \"publisher\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"NOAA NMFS SWFSC Environmental Research Division\",\n" +
        "    \"address\": {\n" +
        "      \"@type\": \"PostalAddress\",\n" +
        "      \"addressCountry\": \"USA\",\n" +
        "      \"addressLocality\": \"99 Pacific St, Suite 255A, Monterey\",\n" +
        "      \"addressRegion\": \"CA\",\n" +
        "      \"postalCode\": \"93940\"\n" +
        "    },\n" +
        "    \"telephone\": \"+1 831-420-3666\",\n" +
        "    \"email\": \"BobSimons2.00@gmail.com\",\n" +
        "    \"sameAs\": \"https://www.fisheries.noaa.gov/about/environmental-research-division-southwest-fisheries-science-center\"\n"
        +
        "  },\n" +
        "  \"fileFormat\": [\n" +
        "    \"application/geo+json\",\n" +
        "    \"application/json\",\n" +
        "    \"text/csv\"\n" +
        "  ],\n" +
        "  \"isAccessibleForFree\": \"True\",\n" +
        "  \"dataset\": [\n" +
        "    {\n" +
        "      \"@type\": \"Dataset\",\n" +
        "      \"name\": \"";
    po = Math.max(0, results.indexOf(expected.substring(0, 80)));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    {\n" +
        "      \"@type\": \"Dataset\",\n" +
        "      \"name\": \"JPL MUR SST Images\",\n" +
        "      \"sameAs\": \"https://127.0.0.1:8443/cwexperimental/info/testFileNames/index.html\"\n" +
        "    }";
    po = results.indexOf(expected.substring(0, 80));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // json-ld 1 dataset
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/jplMURSST41/index.html");
    expected = "<script type=\"application/ld+json\">\n" +
        "{\n" +
        "  \"@context\": \"http://schema.org\",\n" +
        "  \"@type\": \"Dataset\",\n" +
        "  \"name\": \"Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global, 0.01°, 2002-present, Daily\",\n"
        +
        "  \"headline\": \"jplMURSST41\",\n" +
        "  \"description\": \"This is a merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysi" +
        "s product from Jet Propulsion Laboratory (JPL). This daily, global, Multi-scale, Ultra-high Resolution (MUR) Se"
        +
        "a Surface Temperature (SST) 1-km data set, Version 4.1, is produced at JPL under the NASA MEaSUREs program. For"
        +
        " details, see https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1 . This dataset is part of the Group for "
        +
        "High-Resolution Sea Surface Temperature (GHRSST) project. The data for the most recent 7 days is usually revise"
        +
        "d everyday.  The data for other days is sometimes revised.\\nacknowledgement=Please acknowledge the use of these "
        +
        "data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.\\ncd"
        +
        "m_data_type=Grid\\ncomment=MUR = \\\"Multi-scale Ultra-high Resolution\\\"\\nConventions=CF-1.6, COARDS, ACDD-1.3\\nEa"
        +
        "sternmost_Easting=180.0\\nfile_quality_level=1\\ngds_version_id=2.0\\ngeospatial_lat_max=89.99\\ngeospatial_lat_min"
        +
        "=-89.99\\ngeospatial_lat_resolution=0.01\\ngeospatial_lat_units=degrees_north\\ngeospatial_lon_max=180.0\\ngeospati"
        +
        "al_lon_min=-179.99\\ngeospatial_lon_resolution=0.01\\ngeospatial_lon_units=degrees_east\\nhistory=created at nomin"
        +
        "al 4-day latency; replaced nrt (1-day latency) version.\\nData is downloaded daily from " +
        "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/ " +
        "to NOAA NMFS SWFSC ERD by erd.data@noaa.gov .\\nThe data fo" +
        "r the most recent 7 days is usually revised everyday. The data for other days is sometimes revised.\\nid=MUR-JPL"
        +
        "-L4-GLOB-v04.1\\ninfoUrl=https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\\ninstitution=NASA JPL\\nkeyword"
        +
        "s_vocabulary=GCMD Science Keywords\\nnaming_authority=org.ghrsst\\nnetcdf_version_id=4.1\\nNorthernmost_Northing=8"
        +
        "9.99\\nplatform=Terra, Aqua, GCOM-W, NOAA-19, MetOp-A, Buoys/Ships\\nprocessing_level=L4\\nproject=NASA Making Ear"
        +
        "th Science Data Records for Use in Research Environments (MEaSUREs) Program\\nreferences=https://podaac.jpl.nasa.gov/"
        +
        "dataset/MUR-JPL-L4-GLOB-v4.1\\nsensor=MODIS, AMSR2, AVHRR, in-situ\\nsource=MODIS_T-JPL, MODIS_" +
        "A-JPL, AMSR2-REMSS, AVHRR19_G-NAVO, AVHRRMTA_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\\nsourceUrl=(local files"
        +
        ")\\nSouthernmost_Northing=-89.99\\nspatial_resolution=0.01 degrees\\nstandard_name_vocabulary=CF Standard Name Tab"
        +
        // end date changes
        "le v70\\ntestOutOfDate=now-3days\\ntime_coverage_end=2018-08-07T09:00:00Z\\ntime_coverage_start=2002-06-01T09:00:0"
        +
        "0Z\\nWesternmost_Easting=-179.99\",\n" +
        "  \"url\": \"https://127.0.0.1:8443/cwexperimental/griddap/jplMURSST41.html\",\n" +
        "  \"includedInDataCatalog\": {\n" +
        "    \"@type\": \"DataCatalog\",\n" +
        "    \"name\": \"ERDDAP Data Server at NOAA NMFS SWFSC Environmental Research Division\",\n" +
        "    \"sameAs\": \"https://127.0.0.1:8443/cwexperimental\"\n" +
        "  },\n" +
        "  \"keywords\": [\n" +
        "    \"analysed\",\n" +
        "    \"analysed_sst\",\n" +
        "    \"analysis\",\n" +
        "    \"analysis_error\",\n" +
        "    \"area\",\n" +
        "    \"binary\",\n" +
        "    \"composite\",\n" +
        "    \"daily\",\n" +
        "    \"data\",\n" +
        "    \"day\",\n" +
        "    \"deviation\",\n" +
        "    \"distribution\",\n" +
        "    \"Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature\",\n" +
        "    \"error\",\n" +
        "    \"estimated\",\n" +
        "    \"field\",\n" +
        "    \"final\",\n" +
        "    \"foundation\",\n" +
        "    \"fraction\",\n" +
        "    \"ghrsst\",\n" +
        "    \"high\",\n" +
        "    \"ice\",\n" +
        "    \"ice distribution\",\n" +
        "    \"identifier\",\n" +
        "    \"jet\",\n" +
        "    \"jpl\",\n" +
        "    \"laboratory\",\n" +
        "    \"land\",\n" +
        "    \"land_binary_mask\",\n" +
        "    \"mask\",\n" +
        "    \"multi\",\n" +
        "    \"multi-scale\",\n" +
        "    \"mur\",\n" +
        "    \"nasa\",\n" +
        "    \"ocean\",\n" +
        "    \"oceans\",\n" +
        "    \"product\",\n" +
        "    \"propulsion\",\n" +
        "    \"resolution\",\n" +
        "    \"scale\",\n" +
        "    \"sea\",\n" +
        "    \"sea ice area fraction\",\n" +
        "    \"sea/land\",\n" +
        "    \"sea_ice_fraction\",\n" +
        "    \"sea_surface_foundation_temperature\",\n" +
        "    \"sst\",\n" +
        "    \"standard\",\n" +
        "    \"statistics\",\n" +
        "    \"surface\",\n" +
        "    \"temperature\",\n" +
        "    \"time\",\n" +
        "    \"ultra\",\n" +
        "    \"ultra-high\"\n" +
        "  ],\n" +
        "  \"license\": \"These data are available free of charge under the JPL PO.DAAC data policy.\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\",\n"
        +
        "  \"variableMeasured\": [\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"time\",\n" +
        "      \"alternateName\": \"reference time of sst field\",\n" +
        "      \"description\": \"reference time of sst field\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"T\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"Nominal time of analyzed fields\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"reference time of sst field\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"time_origin\",\n" +
        "          \"value\": \"01-JAN-1970 00:00:00\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": \"2018-08-07T09:00:00Z\",\n" + // changes
        "      \"minValue\": \"2002-06-01T09:00:00Z\",\n" +
        "      \"propertyID\": \"time\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"latitude\",\n" +
        "      \"alternateName\": \"Latitude\",\n" +
        "      \"description\": \"Latitude\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Lat\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"Y\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Location\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Latitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"latitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 90\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -90\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": 89.99,\n" +
        "      \"minValue\": -89.99,\n" +
        "      \"propertyID\": \"latitude\",\n" +
        "      \"unitText\": \"degrees_north\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"longitude\",\n" +
        "      \"alternateName\": \"Longitude\",\n" +
        "      \"description\": \"Longitude\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Lon\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"X\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Location\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Longitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"longitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 180\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -180\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": 180,\n" +
        "      \"minValue\": -179.99,\n" +
        "      \"propertyID\": \"longitude\",\n" +
        "      \"unitText\": \"degrees_east\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"analysed_sst\",\n" +
        "      \"alternateName\": \"Analysed Sea Surface Temperature\",\n" +
        "      \"description\": \"Analysed Sea Surface Temperature\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -7.768000000000001\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 32\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"Interim near-real-time (nrt) version using Multi-Resolution Variational Analysis (MRVA) method for interpolation; to be replaced by Final version\"\n"
        +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Analysed Sea Surface Temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRR19_G-NAVO, AVHRRMTA_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\"\n"
        +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"sea_surface_foundation_temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 57.767\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -7.767000000000003\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"sea_surface_foundation_temperature\",\n" +
        "      \"unitText\": \"degree_C\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"analysis_error\",\n" +
        "      \"alternateName\": \"Estimated Error Standard Deviation of analysed_sst\",\n" +
        "      \"description\": \"Estimated Error Standard Deviation of analysed_sst\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -327.68\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 5\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Statistics\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Estimated Error Standard Deviation of analysed_sst\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 327.67\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 0\n" +
        "        }\n" +
        "      ],\n" +
        "      \"unitText\": \"degree_C\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"mask\",\n" +
        "      \"alternateName\": \"Sea/Land Field Composite Mask\",\n" +
        "      \"description\": \"Sea/Land Field Composite Mask\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -128\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 20\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"mask can be used to further filter the data.\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"flag_masks\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"flag_meanings\",\n" +
        "          \"value\": \"1=open-sea, 2=land, 5=open-lake, 9=open-sea with ice in the grid, 13=open-lake with ice in the grid\"\n"
        +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"flag_values\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Identifier\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Sea/Land Field Composite Mask\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"GMT \\\"grdlandmask\\\", ice flag from sea_ice_fraction data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"land_binary_mask\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 31\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 1\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"land_binary_mask\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"sea_ice_fraction\",\n" +
        "      \"alternateName\": \"Sea Ice Area Fraction\",\n" +
        "      \"description\": \"Sea Ice Area Fraction\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -1.28\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"ice data interpolated by a nearest neighbor approach.\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Ice Distribution\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Sea Ice Area Fraction\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"EUMETSAT OSI-SAF, copyright EUMETSAT\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"sea_ice_area_fraction\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 0\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"sea_ice_area_fraction\",\n" +
        "      \"unitText\": \"1\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"creator\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"JPL MUR SST project\",\n" +
        "    \"email\": \"ghrsst@podaac.jpl.nasa.gov\",\n" +
        "    \"sameAs\": \"https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\"\n" +
        "  },\n" +
        "  \"publisher\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"GHRSST Project Office\",\n" +
        "    \"email\": \"ghrsst-po@nceo.ac.uk\",\n" +
        "    \"sameAs\": \"https://www.ghrsst.org\"\n" +
        "  },\n" +
        "  \"dateCreated\": \"2018-08-08T02:38:31Z\",\n" + // changes
        "  \"identifier\": \"org.ghrsst/MUR-JPL-L4-GLOB-v04.1\",\n" +
        "  \"version\": \"04.1nrt\",\n" +
        "  \"temporalCoverage\": \"2002-06-01T09:00:00Z/2018-08-07T09:00:00Z\",\n" + // end date changes
        "  \"spatialCoverage\": {\n" +
        "    \"@type\": \"Place\",\n" +
        "    \"geo\": {\n" +
        "      \"@type\": \"GeoShape\",\n" +
        "      \"box\": \"-89.99 -179.99 89.99 180\"\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "</script>\n";
    po = Math.max(0, results.indexOf(expected.substring(0, 80)));
    Test.ensureEqual(results.substring(po, Math.min(results.length(), po + expected.length())),
        expected, "results=\n" + results);

  }

  /**
   * This repeatedly gets the info/index.html web page and ensures it is without
   * error.
   * It is best to run this when many datasets are loaded.
   * For a harder test: run this in 4 threads simultaneously.
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // wasn't run as a test, this is for load testing
  void testHammerGetDatasets() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String results, expected;
    String2.log("\n*** Erddap.testHammerGetDatasets");
    int count = -5; // let it warm up
    long sumTime = 0;

    while (true) {
      if (count == 0)
        sumTime = 0;
      sumTime -= System.currentTimeMillis();
      // if uncompressed, it is 1Thread=280 4Threads=900ms
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/info/index.html?" + EDStatic.defaultPIppQuery);
      // if compressed, it is 1Thread=1575 4=Threads=5000ms
      // results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
      // "/info/index.html?" + EDStatic.defaultPIppQuery);
      sumTime += System.currentTimeMillis();
      count++;
      if (count > 0)
        String2.log("count=" + count + " AvgTime=" + (sumTime / count));
      expected = "List of All Datasets";
      Test.ensureTrue(results.indexOf(expected) >= 0,
          "results=\n" + results.substring(0, Math.min(results.length(), 5000)));
      expected = "dataset(s)";
      Test.ensureTrue(results.indexOf(expected) >= 0,
          "results=\n" + results.substring(0, Math.min(results.length(), 5000)));
    }
  }

  /**
   * This is used by Bob to do simple tests of Search.
   * This requires a running local ERDDAP with erdMHchla8day and rMHchla8day
   * (among others which will be not matched).
   * This can be used with searchEngine=original or lucene.
   *
   * @throws exception if trouble.
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testSearch() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String htmlUrl = EDStatic.erddapUrl + "/search/index.html?page=1&itemsPerPage=1000";
    String csvUrl = EDStatic.erddapUrl + "/search/index.csv?page=1&itemsPerPage=1000";
    String expected = "erdMHchla8day";
    String expected2, query, results;
    int count;
    String2.log("\n*** Erddap.testSearch\n" +
        "This assumes localhost ERDDAP is running with erdMHchla8day and rMHchla8day (among others which will be not matched).");
    int po;

    // test valid search string, values are case-insensitive
    query = "";
    String goodQueries[] = {
        // "&searchFor=erdMHchla8day",
        "&searchFor=" + SSR.minimalPercentEncode("MH/chla/"), // both datasets have this in sourceURl
        "&searchFor=MHchla8day", // lucene fail?
        "&searchFor=erdMHchla8", // lucene fail?
        "&searchFor=" + SSR.minimalPercentEncode(
            "\"sourceUrl=https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\n\"")
    };
    for (int i = 0; i < goodQueries.length; i++) {
      query += goodQueries[i];
      results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      count = String2.countAll(results, "public");
      Test.ensureEqual(count, 3, "results=\n" + results + "i=" + i); // one in help, plus one per dataset

      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      count = String2.countAll(results, "public");
      Test.ensureEqual(count, 2, "results=\n" + results + "i=" + i); // one per dataset
    }

    // query with no matches: valid for .html but error for .csv: protocol
    query = "&searchFor=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    expected = "Your query produced no matching results.";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: Your query produced no matching results. Check the spelling of the word(s) you searched for.\";\n"
        +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

  }

  /**
   * This is used by Bob to do simple tests of Advanced Search.
   * 
   * @throws exception if trouble.
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testAdvancedSearch() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String htmlUrl = EDStatic.erddapUrl + "/search/advanced.html?page=1&itemsPerPage=1000";
    String csvUrl = EDStatic.erddapUrl + "/search/advanced.csv?page=1&itemsPerPage=1000";
    String expected = "cwwcNDBCMet";
    String expected2, query, results;
    String2.log("\n*** Erddap.testAdvancedSearch\n" +
        "This assumes localhost ERDDAP is running with at least cwwcNDBCMet.");
    int po;

    // test valid search string, values are case-insensitive
    query = "";
    String goodQueries[] = {
        "&searchFor=CWWCndbc",
        "&protocol=TAbleDAp",
        "&standard_name=sea_surface_WAVE_significant_height",
        "&minLat=0&maxLat=45",
        "&minLon=-135&maxLon=-120",
        "&minTime=now-20years&maxTime=now-19years" };
    for (int i = 0; i < goodQueries.length; i++) {
      query += goodQueries[i];
      results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
    }

    // valid for .html but error for .csv: protocol
    query = "&searchFor=CWWCndbc&protocol=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Your query produced no matching results. (protocol=gibberish)\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: standard_name
    query = "&searchFor=CWWCndbc&standard_name=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Your query produced no matching results. (standard_name=gibberish)\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: &minLat > &maxLat
    query = "&searchFor=CWWCndbc&minLat=45&maxLat=0";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=400;\n" +
        "    message=\"Bad Request: Query error: minLat=45.0 > maxLat=0.0\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: &minTime > &maxTime
    query = "&searchFor=CWWCndbc&minTime=now-10years&maxTime=now-11years";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=400;\n" +
        "    message=\"Bad Request: Query error: minTime=now-10years > maxTime=now-11years\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

  }

  /**
   * This is used by Bob to do simple tests of Categorize.
   * 
   * @throws exception if trouble.
   */
  @org.junit.jupiter.api.Test
  void testCategorize() throws Throwable {
    /*
     * THIS IS NOT YET IMPLEMENTED
     * Erddap.verbose = true;
     * Erddap.reallyVerbose = true;
     * EDD.testVerboseOn();
     * String baseUrl = EDStatic.erddapUrl + "/categorize/";
     * String expected = "cwwcNDBCMet";
     * String expected2, query, results;
     * String2.log("\n*** Erddap.testCategorize\n" +
     * "This assumes localhost ERDDAP is running with at least cwwcNDBCMet.");
     * int po;
     * 
     * //test valid search string, values are case-insensitive
     * query = "";
     * String goodQueries[] = {
     * "&searchFor=CWWCndbc",
     * "&protocol=TAbleDAp",
     * "&standard_name=sea_surface_WAVE_significant_height",
     * "&minLat=0&maxLat=45",
     * "&minLon=-135&maxLon=-120",
     * "&minTime=now-20years&maxTime=now-19years"};
     * for (int i = 0; i < goodQueries.length; i++) {
     * query += goodQueries[i];
     * results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
     * Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" +
     * results);
     * results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
     * Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" +
     * results);
     * }
     * 
     * //valid for .html but error for .csv: protocol
     * query = "&searchFor=CWWCndbc&protocol=gibberish";
     * results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
     * Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
     * try {
     * results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
     * } catch (Throwable t) {
     * results = t.toString();
     * }
     * expected2 =
     * "(Error {\n" +
     * "    code=404;\n" +
     * "    message=\"Not Found: Your query produced no matching results. (protocol=gibberish)\";\n"
     * +
     * "})";
     * Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" +
     * String2.annotatedString(results));
     */ }

  /**
   * This asks you to try accessing media files in public and private AWS S3
   * buckets.
   * It was a project to get this to work.
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testAwsS3MediaFiles() {
    // String2.pressEnterToContinue("\n*** Erddap.testAwsS3MediaFiles()\n" +
    // "Try to view (and restart in the middle) the small.mp4 and other video,
    // audio, and image files in\n" +
    // "testMediaFiles, testPrivateAwsS3MediaFiles and testPublicAwsS3MediaFiles.\n"
    // +
    // "It was hard to get the public and private AWS S3 datasets working
    // correctly.\n");
  }

  /**
   * This checks that the links on the specified web page return error 200.
   *
   * @param tUrl the web page to be checked
   * @throws RuntimeException for
   */
  void testForBrokenLinks(String tUrl) throws Exception {

    String2.log("\nSSR.testForBrokenLinks(" + tUrl + ")");
    String regex = "\"(https?://.+?)\"";
    Pattern pattern = Pattern.compile(regex);
    String lines[] = SSR.getUrlResponseLines(tUrl);
    StringBuilder log = new StringBuilder();
    int errorCount = 0;
    HashSet<String> tried = new HashSet();
    String skip[] = new String[] {
        "http://",
        "http://127.0.0.1:8080/manager/html/", // will always fail this test
        "http://127.0.0.1:8080/erddap/status.html", // will always fail this test
        "https://127.0.0.1:8443/cwexperimental/login.html", // the links to log in (upper right of most web pages) will
                                                            // fail on my test computer
        "https://192.168.31.18/",
        "https://basin.ceoe.udel.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path building
                                                         // failed:
                                                         // sun.security.provider.certpath.SunCertPathBuilderException:
                                                         // unable to find valid certification path to requested target
        "http://coastwatch.pfeg.noaa.gov:8080/", // java.net.SocketTimeoutException: Connect timed out
        "https://coastwatch.pfeg.noaa.gov/erddap/files/cwwcNDBCMet/nrt/NDBC_{41008,41009,41010}_met.nc", // intended for
                                                                                                         // curl
                                                                                                         // (globbing)
        "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/pmelTaoDySst.csv?longitude,latitude,time,station,wmo_platform_code,T_25&time>=2015-05-23T12:00:00Z&time<=2015-05-31T12:00:00Z", // always
                                                                                                                                                                                          // fails
                                                                                                                                                                                          // because
                                                                                                                                                                                          // of
                                                                                                                                                                                          // invalid
                                                                                                                                                                                          // character
        "https://dev.mysql.com", // fails here, but works in browser
        "https://myhsts.org", // javax.net.ssl.SSLHandshakeException: No subject alternative DNS name matching
                              // myhsts.org found.
        "https://gcoos5.geos.tamu.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path building
                                                          // failed:
                                                          // sun.security.provider.certpath.SunCertPathBuilderException:
                                                          // unable to find valid certification path to requested target
        "https://gcoos4.tamu.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path building failed:
                                                     // sun.security.provider.certpath.SunCertPathBuilderException:
                                                     // unable to find valid certification path to requested target
        "https://github.com/ERDDAP/", // fails here, but works in browser
        "http://localhost:8080/manager/html/", // will always fail this test
        "https://mvnrepository.com/artifact/com.datastax.cassandra/cassandra-driver-core", // it's clever: no follow
        "https://mvnrepository.com/artifact/com.codahale.metrics/metrics-core/3.0.2", // it's clever: no follow
        "https://mvnrepository.com/artifact/org.postgresql/postgresql", // it's clever: no follow
        "https://mvnrepository.com/", // it's clever: no follow
        "https://noaa-goes17.s3.us-east-1.amazonaws.com", // will always fail
        "https://noaa-goes17.s3.us-east-1.amazonaws.com/", // will always fail
        "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2019/235/22/OR_ABI-L1b-RadC-M6C01_G17_s20192352201196_e20192352203569_c20192352204013.nc", // always
                                                                                                                                                                // fails
        "https://whatismyipaddress.com/ip-lookup", // it's clever: no follow
        "https://www.adobe.com" // fails here, but works in browser
    };
    // https://unitsofmeasure.org/ucum.html fails in tests because of certificate,
    // but succeeds in my browser. Others are like this, too.
    for (int linei = 0; linei < lines.length; linei++) {
      String urls[] = String2.extractAllCaptureGroupsAsHashSet(lines[linei], pattern, 1).toArray(new String[0]);
      for (int urli = 0; urli < urls.length; urli++) {
        // just try a given url once
        if (tried.contains(urls[urli]))
          continue;
        tried.add(urls[urli]);

        String ttUrl = XML.decodeEntities(urls[urli]);
        if (String2.indexOf(skip, ttUrl) >= 0)
          continue;
        String msg = null;
        try {
          Object[] o3 = SSR.getUrlConnBufferedInputStream(ttUrl,
              20000, false, true); // timeOutMillis, requestCompression, touchMode
          if (o3[0] == null) {
            ((InputStream) o3[1]).close();
            throw new IOException("The URL for SSR.testForBrokenLinks can't be an AWS S3 URL.");
          }
          HttpURLConnection conn = (HttpURLConnection) (o3[0]);
          int code = conn.getResponseCode();
          if (code != 200)
            msg = " code=" + code + " " + ttUrl;
        } catch (Exception e) {
          msg = " code=ERR " + ttUrl + " error=\n" +
              e.toString() + "\n";
        }
        if (msg != null) {
          String fullMsg = "#" + ++errorCount + " line=" + String2.left("" + (linei + 1), 4) + msg;
          String2.log(fullMsg);
          log.append(fullMsg + "\n");
        }
      }
    }
    if (log.length() > 0)
      throw new RuntimeException(
          "\nSSR.testForBrokenLinks(" + tUrl + ") found:\n" +
              log.toString());
  }

  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testForBrokenLinks() throws Exception {
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/convert/oceanicAtmosphericAcronyms.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/convert/fipscounty.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/convert/keywords.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/convert/time.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/convert/units.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/convert/urls.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/convert/oceanicAtmosphericVariableNames.html");

    this.testForBrokenLinks("http://localhost:8080/cwexperimental/download/AccessToPrivateDatasets.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/download/changes.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/download/EDDTableFromEML.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/download/grids.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/download/NCCSV.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/download/NCCSV_1.00.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/download/setup.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/download/setupDatasetsXml.html");

    this.testForBrokenLinks("http://localhost:8080/cwexperimental/information.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/rest.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/griddap/documentation.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/tabledap/documentation.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/files/documentation.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/wms/documentation.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/images/erddapTalk/TablesAndGrids.html");
    this.testForBrokenLinks("http://localhost:8080/cwexperimental/images/erddapTalk/erdData.html");
  }
}
