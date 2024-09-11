package gov.noaa.pfel.erddap;

import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeAll;
import tags.TagIncompleteTest;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class ErddapTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** Test Convert Nearest Data. */
  @org.junit.jupiter.api.Test
  void testConvertInterpolate() throws Throwable {
    String2.log("\n*** Erddap.testConvertInterpolate");
    Table table;
    String results, expected;
    int language = 0;
    long time;
    ConcurrentHashMap<String, EDDGrid> tGridDatasetHashMap = new ConcurrentHashMap();
    tGridDatasetHashMap.put("testGriddedNcFiles", (EDDGrid) EDDTestDataset.gettestGriddedNcFiles());
    // boolean oDebugMode = debugMode;
    // debugMode = true;

    // test error: no TLL table
    try {
      table =
          Erddap.interpolate(
              language, tGridDatasetHashMap, "", "testGriddedNcFiles/x_wind/Nearest/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Query error: TimeLatLonTable (nothing) is invalid.", "");

    // test error: no TLL rows
    try {
      table =
          Erddap.interpolate(
              language,
              tGridDatasetHashMap,
              "ID,latitude,longitude,time\n",
              "testGriddedNcFiles/x_wind/Nearest/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Query error: TimeLatLonTable is invalid.", "");

    // test error: no specification
    try {
      table =
          Erddap.interpolate(
              language,
              tGridDatasetHashMap,
              "ID,latitude,longitude,time\n"
                  + "TC1,33.125,176.875,2008-01-10T12Z\n"
                  + "TC1,33.125,176.900,2008-01-10T12Z\n"
                  + "TC1,33.125,177.100,2008-01-10T12Z\n"
                  + "TC1,33.125,177.125,2008-01-10T12Z\n",
              "");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Query error: requestCSV (nothing) is invalid.", "");

    // test error: incomplete specification
    try {
      table =
          Erddap.interpolate(
              language,
              tGridDatasetHashMap,
              "ID,latitude,longitude,time\n"
                  + "TC1,33.125,176.875,2008-01-10T12Z\n"
                  + "TC1,33.125,176.900,2008-01-10T12Z\n"
                  + "TC1,33.125,177.100,2008-01-10T12Z\n"
                  + "TC1,33.125,177.125,2008-01-10T12Z\n",
              "testGriddedNcFiles");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(
        results,
        "Query error: datasetID/variable/algorithm/nearby value=\"testGriddedNcFiles\" is invalid.",
        "");

    // test error: unknown dataset
    try {
      table =
          Erddap.interpolate(
              language,
              tGridDatasetHashMap,
              "ID,latitude,longitude,time\n"
                  + "TC1,33.125,176.875,2008-01-10T12Z\n"
                  + "TC1,33.125,176.900,2008-01-10T12Z\n"
                  + "TC1,33.125,177.100,2008-01-10T12Z\n"
                  + "TC1,33.125,177.125,2008-01-10T12Z\n",
              "junk/x_wind/Nearest/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(results, "Error: datasetID=junk wasn't found.", "");

    // test error: unknown var
    try {
      table =
          Erddap.interpolate(
              language,
              tGridDatasetHashMap,
              "ID,latitude,longitude,time\n"
                  + "TC1,33.125,176.875,2008-01-10T12Z\n"
                  + "TC1,33.125,176.900,2008-01-10T12Z\n"
                  + "TC1,33.125,177.100,2008-01-10T12Z\n"
                  + "TC1,33.125,177.125,2008-01-10T12Z\n",
              "testGriddedNcFiles/junk/Nearest/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(
        results,
        "Error: destinationVariableName=junk wasn't found in datasetID=testGriddedNcFiles.",
        "");

    // test error: unknown algorithm
    try {
      table =
          Erddap.interpolate(
              language,
              tGridDatasetHashMap,
              "ID,latitude,longitude,time\n"
                  + "TC1,33.125,176.875,2008-01-10T12Z\n"
                  + "TC1,33.125,176.900,2008-01-10T12Z\n"
                  + "TC1,33.125,177.100,2008-01-10T12Z\n"
                  + "TC1,33.125,177.125,2008-01-10T12Z\n",
              "testGriddedNcFiles/x_wind/junk/1");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(
        results,
        "Query error: algorithm in testGriddedNcFiles/x_wind/junk/1 is invalid. "
            + "(must be one of Nearest, Bilinear, Mean, SD, Median, Scaled, InverseDistance, InverseDistance2, InverseDistance4, InverseDistance6)",
        "");

    // test error: unsupported nearby
    try {
      table =
          Erddap.interpolate(
              language,
              tGridDatasetHashMap,
              "ID,latitude,longitude,time\n"
                  + "TC1,33.125,176.875,2008-01-10T12Z\n"
                  + "TC1,33.125,176.900,2008-01-10T12Z\n"
                  + "TC1,33.125,177.100,2008-01-10T12Z\n"
                  + "TC1,33.125,177.125,2008-01-10T12Z\n",
              "testGriddedNcFiles/x_wind/Nearest/3");
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.getMessage();
    }
    Test.ensureEqual(
        results,
        "Query error: 'nearby' value in testGriddedNcFiles/x_wind/Nearest/3 is invalid.",
        "");

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
    table =
        Erddap.interpolate(
            language,
            tGridDatasetHashMap,
            "ID,latitude,longitude,time\n"
                + "TC1,33.125,176.875,2008-01-10T10Z\n"
                + // right on NaN
                "TC1,33.100,176.9,2008-01-10T11Z\n"
                + "TC1,33.100,177.1,2008-01-10T12Z\n"
                + "TC1,33.125,177.125,2008-01-10T13Z\n",
            "testGriddedNcFiles/x_wind/Nearest/1,testGriddedNcFiles/x_wind/Nearest/4,testGriddedNcFiles/x_wind/Bilinear/4,"
                + "testGriddedNcFiles/x_wind/Mean/4,testGriddedNcFiles/x_wind/SD,"
                + "testGriddedNcFiles/x_wind/Median/4,testGriddedNcFiles/x_wind/Scaled/4,"
                + "testGriddedNcFiles/x_wind/InverseDistance/4,testGriddedNcFiles/x_wind/InverseDistance2/4,"
                + "testGriddedNcFiles/x_wind/InverseDistance4/4,testGriddedNcFiles/x_wind/InverseDistance6/4");
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
            + "testGriddedNcFiles_x_wind_Mean_4,testGriddedNcFiles_x_wind_SD,testGriddedNcFiles_x_wind_Median_4,testGriddedNcFiles_x_wind_Scaled_4,"
            + "testGriddedNcFiles_x_wind_InverseDistance_4,testGriddedNcFiles_x_wind_InverseDistance2_4,testGriddedNcFiles_x_wind_InverseDistance4_4,testGriddedNcFiles_x_wind_InverseDistance6_4\n"
            + "TC1,33.125,176.875,2008-01-10T10Z,,5.72859,"
            + // right on NaN // nearest1, nearest4,
            "5.72859,"
            + // bilinear4
            "6.691106666666667,7.299908651433333,5.72859,7.735494999999999,"
            + // mean4, sd4, median4, scaled4
            "6.9171001610992295,7.108862,7.387365555555555,7.551191176470589\n"
            + // invDist, invDist2, invDist4,
            // invDist6,
            "TC1,33.1,176.9,2008-01-10T11Z,,5.72859,"
            + // close to NaN
            "6.82821100000025,"
            + "11.226695,60.458317182049974,11.226695,11.226695,"
            + // scaled4 is test of both valid nearby dataset points
            // are equidistant
            "11.226695,11.226695,11.226694999999998,11.226695\n"
            + // all invDist are same because both valid nearby
            // dataset points are equidistant, so get equal wt
            "TC1,33.1,177.1,2008-01-10T12Z,5.72859,5.72859,"
            + // close to 5.72859
            "6.82821100000025,"
            + "11.226695,60.458317182049974,11.226695,5.72859,"
            + "6.828211000000249,5.862690121951286,5.730265740627859,5.728610691270167\n"
            + // increasing wt to closest
            // dataset point: 5.72859
            "TC1,33.125,177.125,2008-01-10T13Z,5.72859,5.72859,"
            + // right on 5.72859
            "5.72859,"
            + "5.006245,0.35389629483333246,4.926565,5.72859,"
            + "5.72859,5.72859,5.72859,5.72859\n"; // all same because atm point is right on dataset
    // point
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
    table =
        Erddap.interpolate(
            language,
            tGridDatasetHashMap,
            "ID,latitude,longitude,time\n"
                + "TC1,33.125,176.875,2008-01-10T10Z\n"
                + "TC1,33.1,176.900,2008-01-10T10Z\n"
                + "TC1,33.1,177.100,2008-01-10T10Z\n"
                + "TC1,33.125,177.125,2008-01-10T10Z\n"
                + "TC1,33.125,176.875,2008-01-10T06Z\n"
                + "TC1,33.125,176.875,2008-01-09T18Z\n"
                + "TC1,33.125,176.875,2008-01-09T12Z\n",
            "testGriddedNcFiles/x_wind/Nearest/8,testGriddedNcFiles/x_wind/Mean/8");
    time = System.currentTimeMillis() - time;
    String2.log("elapsedTime=" + time);
    results = table.dataToString();
    expected = // verified by hand
        "ID,latitude,longitude,time,testGriddedNcFiles_x_wind_Nearest_8,testGriddedNcFiles_x_wind_Mean_8\n"
            + "TC1,33.125,176.875,2008-01-10T10Z,11.938499,9.899416214285715\n"
            + "TC1,33.1,176.9,2008-01-10T10Z,5.72859,11.641267416666665\n"
            + "TC1,33.1,177.1,2008-01-10T10Z,5.72859,11.641267416666665\n"
            + "TC1,33.125,177.125,2008-01-10T10Z,5.72859,8.505438625\n"
            + // nearest: mv->next lon
            "TC1,33.125,176.875,2008-01-10T06Z,11.938499,9.899416214285715\n"
            + // nearest: mv->prev time
            "TC1,33.125,176.875,2008-01-09T18Z,11.938499,9.899416214285715\n"
            + "TC1,33.125,176.875,2008-01-09T12Z,11.938499,9.899416214285715\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Nearest/16, Mean/16 (2D)
    time = System.currentTimeMillis();
    table =
        Erddap.interpolate(
            language,
            tGridDatasetHashMap,
            "ID,latitude,longitude,time\n" + "TC1,33.1,176.900,2008-01-10T10Z\n",
            "testGriddedNcFiles/x_wind/Nearest/16,"
                + "testGriddedNcFiles/x_wind/Mean/4,testGriddedNcFiles/x_wind/Mean/16,testGriddedNcFiles/x_wind/Mean/36,"
                + "testGriddedNcFiles/x_wind/Mean/8,testGriddedNcFiles/x_wind/Mean/64,testGriddedNcFiles/x_wind/Mean/216");
    time = System.currentTimeMillis() - time;
    String2.log("elapsedTime=" + time);
    results = table.dataToString();
    expected = // verified by hand
        "ID,latitude,longitude,time,testGriddedNcFiles_x_wind_Nearest_16,"
            + "testGriddedNcFiles_x_wind_Mean_4,testGriddedNcFiles_x_wind_Mean_16,testGriddedNcFiles_x_wind_Mean_36,"
            + "testGriddedNcFiles_x_wind_Mean_8,testGriddedNcFiles_x_wind_Mean_64,testGriddedNcFiles_x_wind_Mean_216\n"
            + "TC1,33.1,176.9,2008-01-10T10Z,5.72859,"
            + "11.226695,11.18696507142857,9.728878844117647,"
            + // /4 /16 by hand, /36 looks good
            "11.641267416666665,11.813308039215682,14.658405367015714\n"; // look good
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Scaled/x
    time = System.currentTimeMillis();
    table =
        Erddap.interpolate(
            language,
            tGridDatasetHashMap,
            "ID,latitude,longitude,time\n" + "TC1,33.1,176.900,2008-01-10T10Z\n",
            "testGriddedNcFiles/x_wind/Scaled/4,testGriddedNcFiles/x_wind/Scaled/16,testGriddedNcFiles/x_wind/Scaled/36,"
                + "testGriddedNcFiles/x_wind/Scaled/8,testGriddedNcFiles/x_wind/Scaled/64,testGriddedNcFiles/x_wind/Scaled/216");
    time = System.currentTimeMillis() - time;
    String2.log("elapsedTime=" + time);
    results = table.dataToString();
    expected = // verified by hand
        "ID,latitude,longitude,time,"
            + "testGriddedNcFiles_x_wind_Scaled_4,testGriddedNcFiles_x_wind_Scaled_16,testGriddedNcFiles_x_wind_Scaled_36,"
            + "testGriddedNcFiles_x_wind_Scaled_8,testGriddedNcFiles_x_wind_Scaled_64,testGriddedNcFiles_x_wind_Scaled_216\n"
            + "TC1,33.1,176.9,2008-01-10T10Z,"
            + "11.226695,11.26523262746242,10.791734445086393,"
            + // looks good (based on the debug msg showing pts
            // involved)
            "11.474244755147355,11.67703618746455"; // looks good (test of duplicating values
    // beyond dataset's time values)
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // debugMode = oDebugMode;

  }

  /**
   * This repeatedly gets the info/index.html web page and ensures it is without error. It is best
   * to run this when many datasets are loaded. For a harder test: run this in 4 threads
   * simultaneously.
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
      if (count == 0) sumTime = 0;
      sumTime -= System.currentTimeMillis();
      // if uncompressed, it is 1Thread=280 4Threads=900ms
      results =
          SSR.getUrlResponseStringUnchanged(
              EDStatic.erddapUrl + "/info/index.html?" + EDStatic.defaultPIppQuery);
      // if compressed, it is 1Thread=1575 4=Threads=5000ms
      // results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
      // "/info/index.html?" + EDStatic.defaultPIppQuery);
      sumTime += System.currentTimeMillis();
      count++;
      if (count > 0) String2.log("count=" + count + " AvgTime=" + (sumTime / count));
      expected = "List of All Datasets";
      Test.ensureTrue(
          results.indexOf(expected) >= 0,
          "results=\n" + results.substring(0, Math.min(results.length(), 5000)));
      expected = "dataset(s)";
      Test.ensureTrue(
          results.indexOf(expected) >= 0,
          "results=\n" + results.substring(0, Math.min(results.length(), 5000)));
    }
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
   * This asks you to try accessing media files in public and private AWS S3 buckets. It was a
   * project to get this to work.
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
}
