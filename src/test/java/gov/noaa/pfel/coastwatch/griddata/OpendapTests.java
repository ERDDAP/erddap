package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.DConnect;
import gov.noaa.pfel.coastwatch.util.SSR;
import tags.TagIncompleteTest;
import tags.TagThredds;

class OpendapTests {

  @org.junit.jupiter.api.Test
  @TagThredds
  void basicTest() throws Exception {

    Grid.verbose = true;
    Opendap.verbose = true;
    Opendap opendap;

    /*
     * //test Oceanwatch Opendap no longer active
     * opendap = new Opendap(
     * //
     * "http://las.pfeg.noaa.gov/cgi-bin/nph-dods/data/oceanwatch/nrt/gac/AG1day.nc",
     * "http://192.168.31.13/cgi-bin/nph-dods/data/oceanwatch/nrt/qscat/QNuy108day.nc",
     * true, null);
     * //opendap.getGridInfo("ssta", "-1.0e34");
     * opendap.getGridInfo("uy10", "-1.0e34");
     * opendap.numberOfObservations =
     * DataHelper.getDoubleArray(opendap.dConnect,
     * "?numberOfObservations.numberOfObservations");
     * opendap.getTimeOptions(true, Calendar2.SECONDS_PER_DAY,
     * Calendar2.isoStringToEpochSeconds("1985-01-01"));
     * File2.delete(SSR.getTempDirectory() + "OpendapTest.grd");
     * opendap.makeGrid(SSR.getTempDirectory(), "OpendapTest.grd",
     * opendap.timeOptions[0],
     * -135 + 360, -105 + 360, 22, 50, //US+Mexico
     * Integer.MAX_VALUE, Integer.MAX_VALUE, null);
     */

    // test THREDDS //was :8081
    opendap =
        new Opendap(
            "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/GA/ssta/3day", true, null);
    DConnect dConnect = new DConnect(opendap.url, opendap.acceptDeflate, 1, 1);
    opendap.getGridInfo(
        dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT),
        dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT),
        "GAssta",
        "-1.0e34");
    Test.ensureEqual(
        opendap.getLat(0), -44.975, ""); // I'm not sure about exact range, should be global data
    Test.ensureEqual(opendap.getLat(opendap.gridNLatValues - 1), 59.975, "");
    Test.ensureEqual(opendap.gridLatIncrement, .05, "");
    Test.ensureEqual(opendap.getLon(0), 180.025, "");
    Test.ensureEqual(opendap.getLon(opendap.gridNLonValues - 1), 329.975, "");
    Test.ensureEqual(opendap.gridLonIncrement, .05, "");

    // test THREDDS
    opendap =
        new Opendap( // was :8081
            "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/AG/ssta/3day", true, null);
    dConnect = new DConnect(opendap.url, opendap.acceptDeflate, 1, 1);
    opendap.getGridInfo(dConnect.getDAS(60000), dConnect.getDDS(60000), "AGssta", "-1.0e34");
    Test.ensureEqual(opendap.getLat(0), -75, "");
    Test.ensureEqual(opendap.getLat(opendap.gridNLatValues - 1), 75, "");
    Test.ensureEqual(opendap.gridLatIncrement, .1, "");
    Test.ensureEqual(opendap.getLon(0), 0, "");
    Test.ensureEqual(opendap.getLon(opendap.gridNLonValues - 1), 360, "");
    Test.ensureEqual(opendap.gridLonIncrement, .1, "");
    opendap.getTimeOptions(
        false, opendap.gridTimeFactorToGetSeconds, opendap.gridTimeBaseSeconds, 3 * 24);

    // ensure not oddly spaced after makeLonPM180
    // data is 0..360, so ask for ~-180 to ~180
    String dir = SSR.getTempDirectory();
    Grid grid = opendap.makeGrid(opendap.timeOptions[0], -170, 170, 22, 50, 53, 37);
    String2.log("lon values: " + String2.toCSSVString(grid.lon));
    DataHelper.ensureEvenlySpaced(grid.lon, "The lon values aren't evenly spaced:\n");
    DataHelper.ensureEvenlySpaced(grid.lat, "The lat values aren't evenly spaced:\n");
    int nLon = grid.lon.length;
    int nLat = grid.lat.length;
    String lonString = "lon values: " + String2.toCSSVString(grid.lon);
    String latString = "lat values: " + String2.toCSSVString(grid.lat);
    // ???the results seem to flip flop, perhaps based on some intermediate file
    // that isn't being deleted every time
    // but both answers are ok (179.2 is preferred?)
    // if (nLon == 55) {
    Test.ensureEqual(grid.lon[0], -172.8, lonString);
    Test.ensureEqual(grid.lon[nLon - 1], 172.8, lonString);
    // } else if (nLon == 57) {
    // Test.ensureEqual(grid.lon[0], -179.2, lonString);
    // Test.ensureEqual(grid.lon[nLon-1], 179.2, lonString);
    // } else
    // Test.error(grid.lonInfoString());
    Test.ensureEqual(nLat, 41, latString);
    Test.ensureEqual(grid.lat[0], 22, latString);
    Test.ensureEqual(grid.lat[nLat - 1], 50, latString);
  }

  /**
   * This connects to the opendapUrl and gets the dataDds from the query.
   *
   * @param opendapUrl e.g.,
   *     "https://oceanwatch.pfeg.noaa.gov:8081/thredds/dodsC/satellite/AG/ssta/3day"
   * @param query e.g., "?CMusfc.CMusfc[0:1:0][0:1:0][0:1:20][0:1:20]", already percentEncoded as
   *     needed
   * @param doAsciiTestToo
   * @throws Exception if trouble
   */
  public static void simpleSpeedTest(String opendapUrl, String query, boolean doAsciiTestToo)
      throws Exception {
    // do opendap query
    System.out.println(
        "Opendap.simpleSpeedTest\n" + "  opendapUrl=" + opendapUrl + "\n" + "  query=" + query);
    boolean acceptDeflate = true;
    dods.dap.DConnect dConnect = new dods.dap.DConnect(opendapUrl, acceptDeflate, 1, 1);
    long time = System.currentTimeMillis();
    dods.dap.DataDDS dataDds = dConnect.getData(query, null);
    System.out.println(
        "  Opendap.simpleSpeedTest binary query TIME="
            + (System.currentTimeMillis() - time)
            + "ms");

    // do ascii query
    if (doAsciiTestToo) {
      time = System.currentTimeMillis();
      String result = SSR.getUrlResponseStringUnchanged(opendapUrl + ".ascii" + query);
      // String2.log(result);
      System.out.println(
          "  Opendap.simpleSpeedTest ascii  query TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
    }
  }

  /**
   * This performs a series of simple speed tests. I had noticed with THREDDS 3.10 that the speed
   * reflected the number of files in the directory. This provides a way to time that.
   *
   * @param satelliteUrl
   * @param doAsciiTestToo
   * @throws Exception if trouble
   */
  private static void doSimpleSpeedTests(
      String satelliteUrl, boolean doDotTestToo, boolean doAsciiTestToo) throws Exception {

    // Times vary ~20% on various runs.
    // times below are for oceanwatch
    // With THREDDS 3.12 and later, times are much faster for 2nd pass
    // And if dataset has already been touched, times ~0
    if (doDotTestToo)
      simpleSpeedTest( // 625 ms for THREDDS 3.10; 31 ms for 3.12 2nd pass
          satelliteUrl + "AG/ssta/3day",
          "?AGssta.AGssta[0][0:1:0][0:1:20][0:1:20]",
          doAsciiTestToo);
    simpleSpeedTest(
        satelliteUrl + "AG/ssta/3day", "?AGssta[1][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
    if (doDotTestToo)
      simpleSpeedTest( // 2656 ms for THREDDS 3.10; 203 ms for 3.12 2nd pass
          satelliteUrl + "CM/usfc/hday",
          "?CMusfc.CMusfc[0][0:1:0][0:1:20][0:1:20]",
          doAsciiTestToo);
    simpleSpeedTest(
        satelliteUrl + "CM/usfc/hday", "?CMusfc[1][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
    if (doDotTestToo)
      simpleSpeedTest( // 3516 ms for THREDDS 3.10; 34 ms for 3.12 2nd pass
          satelliteUrl + "GA/ssta/hday",
          "?GAssta.GAssta[0][0:1:0][0:1:20][0:1:20]",
          doAsciiTestToo);
    simpleSpeedTest( // 3516 ms for THREDDS 3.10; 34 ms for 3.12 2nd pass
        satelliteUrl + "GA/ssta/hday", "?GAssta[1][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
    if (doDotTestToo)
      simpleSpeedTest( // 531 ms for THREDDS 3.10; 47 ms for 3.12 2nd pass
          satelliteUrl + "MB/chla/1day",
          "?MBchla.MBchla[0][0:1:0][0:1:20][0:1:20]",
          doAsciiTestToo);
    simpleSpeedTest(
        satelliteUrl + "MB/chla/1day", "?MBchla[1][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
    if (doDotTestToo)
      simpleSpeedTest( // 1344 ms for THREDDS 3.10; 0 ms for 3.12 2nd pass
          satelliteUrl + "QS/curl/8day",
          "?QScurl.QScurl[0][0:1:0][0:1:20][0:1:20]",
          doAsciiTestToo);
    simpleSpeedTest(
        satelliteUrl + "QS/curl/8day", "?QScurl[1][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
  }

  /**
   * This performs a series of simple speed tests on OceanWatch. I had noticed with THREDDS 3.10
   * that the speed reflected the number of files in the directory. This provides a way to time
   * that.
   *
   * @param doAsciiTestToo
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // Connection cannot be opened
  void doOceanWatchSpeedTests() throws Exception {
    boolean doDotTestToo = false;
    boolean doAsciiTestToo = false;
    // System.out.println("\nOpendap.doOceanWatchSpeedTests");
    // try {
    doSimpleSpeedTests(
        "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/", doDotTestToo, doAsciiTestToo);
    // } catch (Exception e) {
    // String2.log(MustBe.throwableToString(e));
    // String2.pressEnterToContinue(
    // "\nRecover from oceanwatch failure?");
    // }
  }

  /**
   * This performs a series of simple speed tests on Thredds1 thredds.
   *
   * @param doDotTestToo
   * @param doAsciiTestToo
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest //  Connection cannot be opened
  void doThredds1_8081SpeedTests() throws Exception {
    boolean doDotTestToo = false;
    boolean doAsciiTestToo = true;
    // System.out.println("\nOpendap.doThredds1_8081SpeedTests");
    // try {
    doSimpleSpeedTests(
        "https://thredds1.pfeg.noaa.gov:8081/thredds/dodsC/satellite/",
        doDotTestToo,
        doAsciiTestToo);
    // } catch (Exception e) {
    // String2.log(MustBe.throwableToString(e));
    // String2.pressEnterToContinue(
    // "\nRecover from thredds1 8081 thredds failure?");
    // }
  }

  /**
   * This performs a series of simple speed tests on Erddap. I had noticed with THREDDS 3.10 that
   * the speed reflected the number of files in the directory. This provides a way to time that.
   *
   * @param doDotTestToo
   * @param doAsciiTestToo
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // Connection cannot be opened
  void doErddapSpeedTests() throws Exception {
    boolean doDotTestToo = false;
    boolean doAsciiTestToo = false;
    // System.out.println("\nOpendap.doErddapSpeedTests");
    // try {
    String baseUrl = "https://coastwatch.pfeg.noaa.gov/erddap/griddap/";
    simpleSpeedTest( // 625 ms for THREDDS 3.10; 31 ms for 3.12 2nd pass
        baseUrl + "erdAGssta3day", "?sst[0:1:0][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
    simpleSpeedTest( // 2656 ms for THREDDS 3.10; 203 ms for 3.12 2nd pass
        baseUrl + "erdCMsfc", "?eastCurrent[0:1:0][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
    // now password protected
    // simpleSpeedTest( //3516 ms for THREDDS 3.10; 34 ms for 3.12 2nd pass
    // baseUrl+"erdGAsstahday",
    // "?GAssta.GAssta[0:1:0][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
    simpleSpeedTest( // 531 ms for THREDDS 3.10; 47 ms for 3.12 2nd pass
        baseUrl + "erdMBchla1day", "?chlorophyll[0:1:0][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);
    simpleSpeedTest( // 1344 ms for THREDDS 3.10; 0 ms for 3.12 2nd pass
        baseUrl + "erdQSstress8day", "?curl[0:1:0][0:1:0][0:1:20][0:1:20]", doAsciiTestToo);

    // } catch (Exception e) {
    // String2.log(MustBe.throwableToString(e));
    // String2.pressEnterToContinue("\nRecover from erddap failure?");
    // }
  }

  /**
   * This performs a THREDDS test: a series of requests for one x,y,t point from a grid dataset.
   * When this fails, error from thredds is: Exception in thread "main"
   * dods.dap.parser.TokenMgrError: Lexical error at line 1, column 1. Encountered: "" (0), after :
   * "" at dods.dap.parser.ErrorParserTokenManager.getNextToken(ErrorParserTokenManager.java:823) at
   * dods.dap.parser.ErrorParser.jj_consume_token(ErrorParser.java:155) at
   * dods.dap.parser.ErrorParser.ErrorObject(ErrorParser.java:10) at
   * dods.dap.DODSException.parse(DODSException.java:193) at
   * dods.dap.DConnect.handleContentDesc(DConnect.java:633) at
   * dods.dap.DConnect.openConnection(DConnect.java:202) at
   * dods.dap.DConnect.getDataFromUrl(DConnect.java:380) at
   * dods.dap.DConnect.getData(DConnect.java:339) at dods.dap.DConnect.getData(DConnect.java:522) at
   * gov.noaa.pfel.coastwatch.griddata.Opendap.simpleSpeedTest(Opendap.java:1180) at
   * gov.noaa.pfel.coastwatch.griddata.Opendap.threddsTunnelTest(Opendap.java:1264) at
   * gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:156)
   *
   * @param nTimes the number of time points to test
   * @param baseUrl
   * @param varName
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // Connection cannot be opened
  void threddsTunnelTest() throws Exception {
    int nTimes = 10; // 200 for a good test
    String baseUrl = "https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdCMsfc";
    String varName = "eastCurrent";
    // currently, GAssta hday on otter has time dimension size is 1392
    // currently, GAssta hday on oceanwatch has time dimension size is 2877
    int nTimePoints = 1000;
    System.out.println("\nOpendap.threddsTunnelTest(" + nTimes + ")");
    long elapsedTime = System.currentTimeMillis();
    java.util.Random random = new java.util.Random();

    // run the test
    for (int time = 0; time < nTimes; time++) {
      String2.log("iteration #" + time);
      int tIndex = random.nextInt(nTimePoints);
      int xIndex = random.nextInt(44); // CMusfc now 44x26
      int yIndex = random.nextInt(26);
      simpleSpeedTest(
          baseUrl,
          "?" + varName + "[" + tIndex + ":1:" + tIndex + "]" + "[0:1:0]" + "[" + yIndex + ":1:"
              + yIndex + "]" + "[" + xIndex + ":1:" + xIndex + "]",
          false);
    }
    System.out.println(
        "\nOpendap.threddsTunnelTest done.  TIME="
            + (System.currentTimeMillis() - elapsedTime)
            + "ms\n");
  }

  /**
   * This performs a series of simple speed tests on Otter. I had noticed with THREDDS 3.10 that the
   * speed reflected the number of files in the directory. This provides a way to time that.
   *
   * @param doDotTestToo
   * @param doAsciiTestToo
   * @param port e.g., 8081 or 8087
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // Connection cannot be opened
  void doOtterSpeedTests() throws Exception {
    boolean doDotTestToo = false;
    boolean doAsciiTestToo = false;
    int port = 8081;
    // System.out.println("\nOpendap.doOtterSpeedTests");
    doSimpleSpeedTests(
        "http://161.55.17.243:" + port + "/thredds/dodsC/satellite/", doDotTestToo, doAsciiTestToo);
  }
}
