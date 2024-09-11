package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagImageComparison;
import tags.TagLargeFiles;
import tags.TagLocalERDDAP;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridSideBySideTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests the methods in this class (easy test since x and y should have same time points).
   *
   * @param doGraphicsTests this is the only place that tests grid vector graphs.
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagLargeFiles
  @TagImageComparison
  void testQSWind(boolean doGraphicsTests) throws Throwable {
    // String2.log("\n*** EDDGridSideBySide.testQSWind");
    // testVerboseOn();
    int language = 0;
    String name, tName, baseName, userDapQuery, results, expected, error;
    String dapQuery;
    String tDir = EDStatic.fullTestCacheDirectory;

    // *** NDBC is also IMPORTANT UNIQUE TEST of >1 variable in a file
    EDDGrid qsWind8 = (EDDGrid) EDDTestDataset.geterdQSwind8day();

    // ensure that attributes are as expected
    Test.ensureEqual(qsWind8.combinedGlobalAttributes().getString("satellite"), "QuikSCAT", "");
    EDV edv = qsWind8.findDataVariableByDestinationName("x_wind");
    Test.ensureEqual(edv.combinedAttributes().getString("standard_name"), "x_wind", "");
    edv = qsWind8.findDataVariableByDestinationName("y_wind");
    Test.ensureEqual(edv.combinedAttributes().getString("standard_name"), "y_wind", "");
    Test.ensureEqual(
        qsWind8.combinedGlobalAttributes().getString("defaultGraphQuery"), "&.draw=vectors", "");

    // get data
    dapQuery = "x_wind[4:8][0][(-20)][(80)],y_wind[4:8][0][(-20)][(80)]";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery, tDir, qsWind8.className() + "1", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        /*
         * pre 2010-07-19 was
         * "time, altitude, latitude, longitude, x_wind, y_wind\n" +
         * "UTC, m, degrees_north, degrees_east, m s-1, m s-1\n" +
         * "2000-02-06T00:00:00Z, 0.0, -20.0, 80.0, -8.639946, 5.16116\n" +
         * "2000-02-14T00:00:00Z, 0.0, -20.0, 80.0, -10.170271, 4.2629514\n" +
         * "2000-02-22T00:00:00Z, 0.0, -20.0, 80.0, -9.746282, 1.8640769\n" +
         * "2000-03-01T00:00:00Z, 0.0, -20.0, 80.0, -9.27684, 4.580559\n" +
         * "2000-03-09T00:00:00Z, 0.0, -20.0, 80.0, -5.0138364, 8.902227\n";
         */
        /*
         * pre 2010-10-26 was
         * "time, altitude, latitude, longitude, x_wind, y_wind\n" +
         * "UTC, m, degrees_north, degrees_east, m s-1, m s-1\n" +
         * "1999-08-26T00:00:00Z, 0.0, -20.0, 80.0, -6.672732, 5.2165475\n" +
         * "1999-09-03T00:00:00Z, 0.0, -20.0, 80.0, -8.441742, 3.7559745\n" +
         * "1999-09-11T00:00:00Z, 0.0, -20.0, 80.0, -5.842872, 4.5936112\n" +
         * "1999-09-19T00:00:00Z, 0.0, -20.0, 80.0, -8.269525, 4.7751155\n" +
         * "1999-09-27T00:00:00Z, 0.0, -20.0, 80.0, -8.95586, 2.439596\n";
         */
        "time,altitude,latitude,longitude,x_wind,y_wind\n"
            + "UTC,m,degrees_north,degrees_east,m s-1,m s-1\n"
            + "1999-07-29T00:00:00Z,10.0,-20.0,80.0,-8.757242,4.1637316\n"
            + "1999-07-30T00:00:00Z,10.0,-20.0,80.0,-9.012303,3.48984\n"
            + "1999-07-31T00:00:00Z,10.0,-20.0,80.0,-8.631654,3.0311484\n"
            + "1999-08-01T00:00:00Z,10.0,-20.0,80.0,-7.9840736,2.5528698\n"
            + "1999-08-02T00:00:00Z,10.0,-20.0,80.0,-7.423252,2.432058\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    dapQuery = "x_wind[4:8][0][(-20)][(80)]";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery, tDir, qsWind8.className() + "2", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        /*
         * pre 2010-10-26 was
         * "time, altitude, latitude, longitude, x_wind\n" +
         * "UTC, m, degrees_north, degrees_east, m s-1\n" +
         * "1999-08-26T00:00:00Z, 0.0, -20.0, 80.0, -6.672732\n" +
         * "1999-09-03T00:00:00Z, 0.0, -20.0, 80.0, -8.441742\n" +
         * "1999-09-11T00:00:00Z, 0.0, -20.0, 80.0, -5.842872\n" +
         * "1999-09-19T00:00:00Z, 0.0, -20.0, 80.0, -8.269525\n" +
         * "1999-09-27T00:00:00Z, 0.0, -20.0, 80.0, -8.95586\n";
         */
        "time,altitude,latitude,longitude,x_wind\n"
            + "UTC,m,degrees_north,degrees_east,m s-1\n"
            + "1999-07-29T00:00:00Z,10.0,-20.0,80.0,-8.757242\n"
            + "1999-07-30T00:00:00Z,10.0,-20.0,80.0,-9.012303\n"
            + "1999-07-31T00:00:00Z,10.0,-20.0,80.0,-8.631654\n"
            + "1999-08-01T00:00:00Z,10.0,-20.0,80.0,-7.9840736\n"
            + "1999-08-02T00:00:00Z,10.0,-20.0,80.0,-7.423252\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    dapQuery = "y_wind[4:8][0][(-20)][(80)]";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery, tDir, qsWind8.className() + "3", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        /*
         * pre 2010-10-26 was
         * "time, altitude, latitude, longitude, y_wind\n" +
         * "UTC, m, degrees_north, degrees_east, m s-1\n" +
         * "1999-08-26T00:00:00Z, 0.0, -20.0, 80.0, 5.2165475\n" +
         * "1999-09-03T00:00:00Z, 0.0, -20.0, 80.0, 3.7559745\n" +
         * "1999-09-11T00:00:00Z, 0.0, -20.0, 80.0, 4.5936112\n" +
         * "1999-09-19T00:00:00Z, 0.0, -20.0, 80.0, 4.7751155\n" +
         * "1999-09-27T00:00:00Z, 0.0, -20.0, 80.0, 2.439596\n";
         */
        "time,altitude,latitude,longitude,y_wind\n"
            + "UTC,m,degrees_north,degrees_east,m s-1\n"
            + "1999-07-29T00:00:00Z,10.0,-20.0,80.0,4.1637316\n"
            + "1999-07-30T00:00:00Z,10.0,-20.0,80.0,3.48984\n"
            + "1999-07-31T00:00:00Z,10.0,-20.0,80.0,3.0311484\n"
            + "1999-08-01T00:00:00Z,10.0,-20.0,80.0,2.5528698\n"
            + "1999-08-02T00:00:00Z,10.0,-20.0,80.0,2.432058\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    if (doGraphicsTests) {

      tDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
      // graphics requests with no .specs
      String2.log("\n*** EDDGridSideBySide test get vector map\n");
      String vecDapQuery = // minimal settings
          "x_wind[2][][(29):(50)][(225):(247)],y_wind[2][][(29):(50)][(225):(247)]";
      baseName = qsWind8.className() + "_Vec1";
      tName =
          qsWind8.makeNewFileForDapQuery(language, null, null, vecDapQuery, tDir, baseName, ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      vecDapQuery = // max settings
          "x_wind[2][][(29):(50)][(225):(247)],y_wind[2][][(29):(50)][(225):(247)]"
              + "&.color=0xFF9900&.font=1.25&.vec=10";
      baseName = qsWind8.className() + "_Vec2";
      tName =
          qsWind8.makeNewFileForDapQuery(language, null, null, vecDapQuery, tDir, baseName, ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // graphics requests with .specs -- lines
      baseName = qsWind8.className() + "_lines";
      tName =
          qsWind8.makeNewFileForDapQuery(
              language,
              null,
              null,
              "x_wind[0:20][(10.0)][(22.0)][(225.0)]"
                  + "&.draw=lines&.vars=time|x_wind|&.color=0xFF9900",
              tDir,
              baseName,
              ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // linesAndMarkers
      baseName = qsWind8.className() + "_linesAndMarkers";
      tName =
          qsWind8.makeNewFileForDapQuery(
              language,
              null,
              null,
              "x_wind[0:20][(10.0)][(22.0)][(225.0)],"
                  + "y_wind[0:20][(10.0)][(22.0)][(225.0)]"
                  + "&.draw=linesAndMarkers&.vars=time|x_wind|y_wind&.marker=5|5&.color=0xFF9900&.colorBar=|C|Linear|||",
              tDir,
              baseName,
              ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // graphics requests with .specs -- markers
      baseName = qsWind8.className() + "_markers";
      tName =
          qsWind8.makeNewFileForDapQuery(
              language,
              null,
              null,
              "x_wind[0:20][(10.0)][(22.0)][(225.0)]"
                  + "&.draw=markers&.vars=time|x_wind|&.marker=1|5&.color=0xFF9900&.colorBar=|C|Linear|||",
              tDir,
              baseName,
              ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // colored markers
      baseName = qsWind8.className() + "_coloredMarkers";
      tName =
          qsWind8.makeNewFileForDapQuery(
              language,
              null,
              null,
              "x_wind[0:20][(10.0)][(22.0)][(225.0)],"
                  + "y_wind[0:20][(10.0)][(22.0)][(225.0)]"
                  + "&.draw=markers&.vars=time|x_wind|y_wind&.marker=5|5&.colorBar=|C|Linear|||",
              tDir,
              baseName,
              ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // surface
      // needs 4 line legend
      baseName = qsWind8.className() + "_surface";
      tName =
          qsWind8.makeNewFileForDapQuery(
              language,
              null,
              null,
              "x_wind[2][(10.0)][(-75.0):(75.0)][(10.0):(360.0)]"
                  + "&.draw=surface&.vars=longitude|latitude|x_wind&.colorBar=|C|Linear|||",
              tDir,
              baseName,
              ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // sticks
      baseName = qsWind8.className() + "_sticks";
      tName =
          qsWind8.makeNewFileForDapQuery(
              language,
              null,
              null,
              "x_wind[0:10][(10.0)][(75.0)][(360.0)],"
                  + "y_wind[0:10][(10.0)][(75.0)][(360.0)]"
                  + "&.draw=sticks&.vars=time|x_wind|y_wind&.color=0xFF9900",
              tDir,
              baseName,
              ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // vectors
      baseName = qsWind8.className() + "_vectors";
      tName =
          qsWind8.makeNewFileForDapQuery(
              language,
              null,
              null,
              "x_wind[2][(10.0)][(22.0):(50.0)][(225.0):(255.0)],"
                  + "y_wind[2][(10.0)][(22.0):(50.0)][(225.0):(255.0)]"
                  + "&.draw=vectors&.vars=longitude|latitude|x_wind|y_wind&.color=0xFF9900",
              tDir,
              baseName,
              ".png");
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
      /* */
    }
  }

  /**
   * The tests QSstress1day: datasets where children have different axis0 values.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testQSStress() throws Throwable {
    // String2.log("\n*** EDDGridSideBySide.testQSWind");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    String dapQuery;
    String tDir = EDStatic.fullTestCacheDirectory;
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1306736E9), "2005-10-30T12:00:00Z", "");

    EDDGrid qs1 = (EDDGrid) EDDTestDataset.geterdQSstress1day();
    dapQuery = "taux[0:11][0][(-20)][(40)],tauy[0:11][0][(-20)][(40)]";
    tName =
        qs1.makeNewFileForDapQuery(
            language, null, null, dapQuery, tDir, qs1.className() + "sbsxy", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,taux,tauy\n"
            + "UTC,m,degrees_north,degrees_east,Pa,Pa\n"
            + "1999-07-21T12:00:00Z,0.0,-20.0,40.0,-0.104488,0.208256\n"
            + "1999-07-22T12:00:00Z,0.0,-20.0,40.0,-0.00434009,0.0182522\n"
            + "1999-07-23T12:00:00Z,0.0,-20.0,40.0,-0.023387,0.0169275\n"
            + "1999-07-24T12:00:00Z,0.0,-20.0,40.0,-0.0344483,0.0600279\n"
            + "1999-07-25T12:00:00Z,0.0,-20.0,40.0,-0.0758333,0.122013\n"
            + "1999-07-26T12:00:00Z,0.0,-20.0,40.0,-0.00774623,0.0145094\n"
            + "1999-07-27T12:00:00Z,0.0,-20.0,40.0,0.00791195,0.0160653\n"
            + "1999-07-28T12:00:00Z,0.0,-20.0,40.0,0.00524606,0.00868887\n"
            + "1999-07-29T12:00:00Z,0.0,-20.0,40.0,0.00184722,0.0223571\n"
            + "1999-07-30T12:00:00Z,0.0,-20.0,40.0,-0.00843481,0.00153571\n"
            + "1999-07-31T12:00:00Z,0.0,-20.0,40.0,-0.016016,0.00505983\n"
            + "1999-08-01T12:00:00Z,0.0,-20.0,40.0,-0.0246662,0.00822855\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    dapQuery = "taux[0:2:10][0][(-20)][(40)],tauy[0:2:10][0][(-20)][(40)]";
    tName =
        qs1.makeNewFileForDapQuery(
            language, null, null, dapQuery, tDir, qs1.className() + "sbsxy2a", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,taux,tauy\n"
            + "UTC,m,degrees_north,degrees_east,Pa,Pa\n"
            + "1999-07-21T12:00:00Z,0.0,-20.0,40.0,-0.104488,0.208256\n"
            + "1999-07-23T12:00:00Z,0.0,-20.0,40.0,-0.023387,0.0169275\n"
            + "1999-07-25T12:00:00Z,0.0,-20.0,40.0,-0.0758333,0.122013\n"
            + "1999-07-27T12:00:00Z,0.0,-20.0,40.0,0.00791195,0.0160653\n"
            + "1999-07-29T12:00:00Z,0.0,-20.0,40.0,0.00184722,0.0223571\n"
            + "1999-07-31T12:00:00Z,0.0,-20.0,40.0,-0.016016,0.00505983\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    dapQuery = "taux[1:2:11][0][(-20)][(40)],tauy[1:2:11][0][(-20)][(40)]";
    tName =
        qs1.makeNewFileForDapQuery(
            language, null, null, dapQuery, tDir, qs1.className() + "sbsxy2b", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,taux,tauy\n"
            + "UTC,m,degrees_north,degrees_east,Pa,Pa\n"
            + "1999-07-22T12:00:00Z,0.0,-20.0,40.0,-0.00434009,0.0182522\n"
            + "1999-07-24T12:00:00Z,0.0,-20.0,40.0,-0.0344483,0.0600279\n"
            + "1999-07-26T12:00:00Z,0.0,-20.0,40.0,-0.00774623,0.0145094\n"
            + "1999-07-28T12:00:00Z,0.0,-20.0,40.0,0.00524606,0.00868887\n"
            + "1999-07-30T12:00:00Z,0.0,-20.0,40.0,-0.00843481,0.00153571\n"
            + "1999-08-01T12:00:00Z,0.0,-20.0,40.0,-0.0246662,0.00822855\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test missing time point represents >1 missing value
    dapQuery = "taux[0:2:6][0][(-20)][(40):(40.5)],tauy[0:2:6][0][(-20)][(40):(40.5)]";
    tName =
        qs1.makeNewFileForDapQuery(
            language, null, null, dapQuery, tDir, qs1.className() + "sbsxy2c", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,taux,tauy\n"
            + "UTC,m,degrees_north,degrees_east,Pa,Pa\n"
            + "1999-07-21T12:00:00Z,0.0,-20.0,40.0,-0.104488,0.208256\n"
            + "1999-07-21T12:00:00Z,0.0,-20.0,40.125,-0.158432,0.0866751\n"
            + "1999-07-21T12:00:00Z,0.0,-20.0,40.25,-0.128333,0.118635\n"
            + "1999-07-21T12:00:00Z,0.0,-20.0,40.375,-0.124832,0.0660782\n"
            + "1999-07-21T12:00:00Z,0.0,-20.0,40.5,-0.130721,0.132997\n"
            + "1999-07-23T12:00:00Z,0.0,-20.0,40.0,-0.023387,0.0169275\n"
            + "1999-07-23T12:00:00Z,0.0,-20.0,40.125,-0.0133533,0.029415\n"
            + "1999-07-23T12:00:00Z,0.0,-20.0,40.25,-0.0217007,0.0307251\n"
            + "1999-07-23T12:00:00Z,0.0,-20.0,40.375,-0.0148099,0.0284445\n"
            + "1999-07-23T12:00:00Z,0.0,-20.0,40.5,-0.021766,0.0333703\n"
            + "1999-07-25T12:00:00Z,0.0,-20.0,40.0,-0.0758333,0.122013\n"
            + "1999-07-25T12:00:00Z,0.0,-20.0,40.125,-0.0832932,0.120377\n"
            + "1999-07-25T12:00:00Z,0.0,-20.0,40.25,-0.0537047,0.115615\n"
            + "1999-07-25T12:00:00Z,0.0,-20.0,40.375,-0.0693455,0.140338\n"
            + "1999-07-25T12:00:00Z,0.0,-20.0,40.5,-0.066977,0.135431\n"
            + "1999-07-27T12:00:00Z,0.0,-20.0,40.0,0.00791195,0.0160653\n"
            + "1999-07-27T12:00:00Z,0.0,-20.0,40.125,0.0107321,0.0135699\n"
            + "1999-07-27T12:00:00Z,0.0,-20.0,40.25,0.00219832,0.0106224\n"
            + "1999-07-27T12:00:00Z,0.0,-20.0,40.375,-9.002E-4,0.0100063\n"
            + "1999-07-27T12:00:00Z,0.0,-20.0,40.5,-0.00168578,0.0117416\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** Some one time tests of this class. */
  @org.junit.jupiter.api.Test
  void testOneTime() throws Throwable {

    // one time test of separate taux and tauy datasets datasets
    // lowestValue=1.130328E9 290 292
    // lowestValue=1.1304144E9 291 293
    // lowestValue=1.1305008E9 292 294
    // lowestValue=1.1305872E9 293 295
    // lowestValue=1.1306736E9 294 NaN
    // lowestValue=1.13076E9 295 296
    // lowestValue=1.1308464E9 296 297
    // lowestValue=1.1309328E9 297 298

    /*
     * not active; needs work
     * EDDGrid qsx1 = (EDDGrid)oneFromDatasetsXml(null, "erdQStaux1day");
     * dapQuery = "taux[(1.130328E9):(1.1309328E9)][0][(-20)][(40)]";
     * tName = qsx1.makeNewFileForDapQuery(language, null, null, dapQuery,
     * EDStatic.fullTestCacheDirectory,
     * qsz1.className() + "sbsx", ".csv");
     * results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory +
     * tName);
     * //String2.log(results);
     * expected =
     * "time,altitude,latitude,longitude,taux\n" +
     * "UTC,m,degrees_north,degrees_east,Pa\n" +
     * "2005-10-26T12:00:00Z,0.0,-20.0,40.0,-0.0102223\n" +
     * "2005-10-27T12:00:00Z,0.0,-20.0,40.0,-0.0444894\n" +
     * "2005-10-28T12:00:00Z,0.0,-20.0,40.0,-0.025208\n" +
     * "2005-10-29T12:00:00Z,0.0,-20.0,40.0,-0.0156589\n" +
     * "2005-10-30T12:00:00Z,0.0,-20.0,40.0,-0.0345074\n" +
     * "2005-10-31T12:00:00Z,0.0,-20.0,40.0,-0.00270854\n" +
     * "2005-11-01T12:00:00Z,0.0,-20.0,40.0,-0.00875408\n" +
     * "2005-11-02T12:00:00Z,0.0,-20.0,40.0,-0.0597476\n";
     * Test.ensureEqual(results, expected, "results=\n" + results);
     *
     * EDDGrid qsy1 = (EDDGrid)oneFromDatasetsXml(null, "erdQStauy1day");
     * dapQuery = "tauy[(1.130328E9):(1.1309328E9)][0][(-20)][(40)]";
     * tName = qsy1.makeNewFileForDapQuery(language, null, null, dapQuery,
     * EDStatic.fullTestCacheDirectory,
     * qsy1.className() + "sbsy", ".csv");
     * results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory +
     * tName);
     * //String2.log(results);
     * expected =
     * "results=time,altitude,latitude,longitude,tauy\n" +
     * "UTC,m,degrees_north,degrees_east,Pa\n" +
     * "2005-10-26T12:00:00Z,0.0,-20.0,40.0,0.0271539\n" +
     * "2005-10-27T12:00:00Z,0.0,-20.0,40.0,0.180277\n" +
     * "2005-10-28T12:00:00Z,0.0,-20.0,40.0,0.0779955\n" +
     * "2005-10-29T12:00:00Z,0.0,-20.0,40.0,0.0994889\n" +
     * //note no 10-30 data
     * "2005-10-31T12:00:00Z,0.0,-20.0,40.0,0.0184601\n" +
     * "2005-11-01T12:00:00Z,0.0,-20.0,40.0,0.0145052\n" +
     * "2005-11-02T12:00:00Z,0.0,-20.0,40.0,-0.0942029\n";
     * Test.ensureEqual(results, expected, "results=\n" + results);
     */

    /*
     * //test creation of sideBySide datasets
     * EDDGrid ta1 = (EDDGrid)oneFromDatasetsXml(null, "erdTAgeo1day");
     * EDDGrid j110 = (EDDGrid)oneFromDatasetsXml(null, "erdJ1geo10day");
     * EDDGrid tas = (EDDGrid)oneFromDatasetsXml(null, "erdTAssh1day");
     *
     * EDDGrid qs1 = (EDDGrid)oneFromDatasetsXml(null, "erdQSwind1day");
     * EDDGrid qs3 = (EDDGrid)oneFromDatasetsXml(null, "erdQSwind3day");
     * EDDGrid qs4 = (EDDGrid)oneFromDatasetsXml(null, "erdQSwind4day");
     * EDDGrid qs8 = (EDDGrid)oneFromDatasetsXml(null, "erdQSwind8day");
     * EDDGrid qs14 = (EDDGrid)oneFromDatasetsXml(null, "erdQSwind14day");
     * EDDGrid qsm = (EDDGrid)oneFromDatasetsXml(null, "erdQSwindmday");
     *
     * EDDGrid qss1 = (EDDGrid)oneFromDatasetsXml(null, "erdQSstress1day");
     * EDDGrid qss3 = (EDDGrid)oneFromDatasetsXml(null, "erdQSstress3day");
     * EDDGrid qss8 = (EDDGrid)oneFromDatasetsXml(null, "erdQSstress8day");
     * EDDGrid qss14 = (EDDGrid)oneFromDatasetsXml(null, "erdQSstress14day");
     * EDDGrid qssm = (EDDGrid)oneFromDatasetsXml(null, "erdQSstressmday");
     *
     * EDDGrid qse7 = (EDDGrid)oneFromDatasetsXml(null, "erdQSekm7day");
     * EDDGrid qse8 = (EDDGrid)oneFromDatasetsXml(null, "erdQSekm8day");
     * //
     */

    /*
     * //test if datasets can be aggregated side-by-side
     * EDDGrid n1Children[] = new EDDGrid[]{
     * (EDDGrid)oneFromDatasetsXml(null, "erdCAusfchday"),
     * (EDDGrid)oneFromDatasetsXml(null, "erdCAvsfchday"),
     * (EDDGrid)oneFromDatasetsXml(null, "erdQSumod1day"),
     * (EDDGrid)oneFromDatasetsXml(null, "erdQStmod1day"),
     * (EDDGrid)oneFromDatasetsXml(null, "erdQScurl1day"),
     * (EDDGrid)oneFromDatasetsXml(null, "erdQStaux1day"),
     * (EDDGrid)oneFromDatasetsXml(null, "erdQStauy1day")
     * };
     * new EDDGridSideBySide("erdQSwind1day", n1Children);
     */
  }

  /**
   * This tests allowing datasets with duplicate sourceNames (they would be in different datasets).
   */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testDuplicateSourceNames() throws Throwable {
    // String2.log("\n*** EDDGridSideBySide.testDuplicateSourceNames");
    // testVerboseOn();
    int language = 0;
    String dir = EDStatic.fullTestCacheDirectory;
    String name, tName, userDapQuery, results, expected, error;
    String dapQuery;

    // if there is trouble, this will throw an exception
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettestDuplicateSourceNames();

    // get some data
    dapQuery =
        "analysed_sst_a[1][(10):100:(12)][(-20):100:(-18)],analysed_sst_b[1][(10):100:(12)][(-20):100:(-18)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, dapQuery, EDStatic.fullTestCacheDirectory, "sbsDupNames", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = // note that all _a and _b values are the same
        "time,latitude,longitude,analysed_sst_a,analysed_sst_b\n"
            + "UTC,degrees_north,degrees_east,degree_C,degree_C\n"
            + "2002-06-02T09:00:00Z,10.0,-20.0,25.709,25.709\n"
            + "2002-06-02T09:00:00Z,10.0,-19.0,27.203,27.203\n"
            + "2002-06-02T09:00:00Z,10.0,-18.0,28.371,28.371\n"
            + "2002-06-02T09:00:00Z,11.0,-20.0,25.175,25.175\n"
            + "2002-06-02T09:00:00Z,11.0,-19.0,26.355,26.355\n"
            + "2002-06-02T09:00:00Z,11.0,-18.0,28.309,28.309\n"
            + "2002-06-02T09:00:00Z,12.0,-20.0,25.331,25.331\n"
            + "2002-06-02T09:00:00Z,12.0,-19.0,26.343,26.343\n"
            + "2002-06-02T09:00:00Z,12.0,-18.0,27.215,27.215\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This test making transparentPngs. */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  @TagImageComparison
  void testTransparentPng() throws Throwable {
    // String2.log("\n*** EDDGridSideBySide.testTransparentPng");
    // testVerboseOn();
    int language = 0;
    String dir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    String name, tName, userDapQuery, results, expected, error;
    String dapQuery, baseName;

    EDDGrid qsWind8 = (EDDGrid) EDDTestDataset.geterdQSwind8day();
    /* */

    // surface map
    dapQuery = "x_wind[0][][][]" + "&.draw=surface&.vars=longitude|latitude|x_wind";
    baseName = "EDDGridSideBySide_testTransparentPng_surface";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    baseName = "EDDGridSideBySide_testTransparentPng_surface360150";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery + "&.size=360|150", dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // vector map
    dapQuery =
        "x_wind[0][][][],"
            + "y_wind[0][][][]"
            + "&.draw=vectors&.vars=longitude|latitude|x_wind|y_wind&.color=0xff0000";
    baseName = "EDDGridSideBySide_testTransparentPng_vectors";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    baseName = "EDDGridSideBySide_testTransparentPng_vectors360150";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery + "&.size=360|150", dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // lines on a graph
    dapQuery = "x_wind[0:10][0][300][0]" + "&.draw=lines&.vars=time|x_wind&.color=0xff0000";

    // This failed after Chris' .transparentPng changes
    // The problem was in
    // test on an older ERDDAP installation works
    // https://salishsea.eos.ubc.ca/erddap/griddap/ubcSSfCampbellRiverSSH10m.transparentPng?ssh[(2022-07-28T05:55:00Z):(2022-07-28T12:05:00Z)][(-125.2205)][(50.01995)]&.draw=lines&.vars=time%7Cssh&.color=0x000000&.timeRange=7,day(s)&.bgColor=0xffccccff
    baseName = "EDDGridSideBySide_testTransparentPng_lines";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    baseName = "EDDGridSideBySide_testPng_lines500400";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery + "&.size=500|400", dir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    baseName = "EDDGridSideBySide_testTransparentPng_lines500400";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery + "&.size=500|400", dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // markers on a graph
    dapQuery = "x_wind[0:10][0][300][0]" + "&.draw=markers&.vars=time|x_wind&.color=0xff0000";
    baseName = "EDDGridSideBySide_testTransparentPng_markers";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    baseName = "EDDGridSideBySide_testTransparentPng_markers500400";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery + "&.size=500|400", dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // sticks on a graph
    dapQuery =
        "x_wind[0:20][0][300][0],"
            + "y_wind[0:20][0][300][0]"
            + "&.draw=sticks&.vars=time|x_wind|y_wind&.color=0xff0000";
    baseName = "EDDGridSideBySide_testTransparentPng_sticks";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    baseName = "EDDGridSideBySide_testTransparentPng_sticks500500";
    tName =
        qsWind8.makeNewFileForDapQuery(
            language, null, null, dapQuery + "&.size=500|500", dir, baseName, ".transparentPng");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
    /* */
  }

  /** This tests the /files/ "files" system. This requires erdTAgeo1day in the localhost ERDDAP. */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testFiles() throws Throwable {

    String2.log("\n*** EDDGridSideBySide.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/erdTAgeo1day/.csv");
    expected =
        "Name,Last modified,Size,Description\n"
            + "erdTAugeo1day/,NaN,NaN,\n"
            + "erdTAvgeo1day/,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results =
        SSR.getUrlResponseStringNewline("http://localhost:8080/cwexperimental/files/erdTAgeo1day/");
    Test.ensureTrue(results.indexOf("erdTAugeo1day&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("erdTAugeo1day/") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/erdTAgeo1day/erdTAvgeo1day/.csv");
    expected =
        "Name,Last modified,Size,Description\n"
            + "TA1992288_1992288_vgeo.nc,1354883738000,4178528,\n"
            + "TA1992295_1992295_vgeo.nc,1354883742000,4178528,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // download a file in root

    // download a file in subdir
    results =
        String2.annotatedString(
            SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/erdTAgeo1day/erdTAvgeo1day/TA1992288_1992288_vgeo.nc")
                .substring(0, 50));
    expected =
        "CDF[1][0][0][0][0][0][0][0][10]\n"
            + "[0][0][0][4][0][0][0][4]time[0][0][0][1][0][0][0][8]altitude[0][0][0][1][0][0][0][3]la[end]";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

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
              "http://localhost:8080/cwexperimental/files/erdTAgeo1day/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/erdTAgeo1day/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/erdTAgeo1day/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/erdTAgeo1day/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existant subdir
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/erdTAgeo1day/subdir/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/erdTAgeo1day/subdir/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }
}
