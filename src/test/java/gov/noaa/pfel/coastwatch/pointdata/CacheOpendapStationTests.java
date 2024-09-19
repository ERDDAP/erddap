package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SSR;
import java.util.Random;
import tags.TagExternalOther;
import tags.TagIncompleteTest;
import tags.TagSlowTests;

class CacheOpendapStationTests {
  /**
   * This test the mbari opendap server for reliability. Basically, this ensures that the response
   * has the right depth, lat and lon values.
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // wasn't being run with standard tests before, almost entirely testing an
  // external server.
  void testMbariOpendapReliability() throws Exception {
    // verbose = true;
    // DataHelper.verbose = true;
    // Table.verbose = true;
    // StationVariableNc4D.verbose = true;
    // PointDataSet.verbose = true;

    Random random = new Random();
    String url, response, match;
    int rep = 0;

    while (true) {
      // adcp stations chosen because I think they give gibberish answers sometimes
      // (ascii and opendap)
      String2.log(
          "\n*** CacheOpendapStation.test rep="
              + rep
              + " "
              + Calendar2.getCurrentISODateTimeStringLocalTZ());
      if (rep > 0)
        Math2.incgc("CacheOpendapStation (between tests)", 5000); // 5 seconds //in a test
      // first time, ensure there are at least 3000 time points; then pick time
      // randomly
      int randomInt = rep == 0 ? 3000 : random.nextInt(3000);
      rep++;

      // ***************
      // M0: get ascii response
      url =
          "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_adcp1267_20060731.nc.ascii?"
              + "u_component_uncorrected["
              + randomInt
              + ":1:"
              + randomInt
              + "][0:1:1][0:1:0][0:1:0],"
              + "v_component_uncorrected["
              + randomInt
              + ":1:"
              + randomInt
              + "][0:1:1][0:1:0][0:1:0]";
      String2.log("url=" + url);
      // try {
      response = SSR.getUrlResponseStringUnchanged(url);
      String2.log("response=" + response);
      match = "u_component_uncorrected, [1][2][1][1]\n" + "[0][0][0], ";
      Test.ensureTrue(response.startsWith(match), "Start of M0 response doesn't match: " + match);
      match =
          "depth, [2]\n"
              + "6, 10\n"
              + "adcp_latitude, [1]\n"
              + "36.833333\n"
              + "adcp_longitude, [1]\n"
              + "-121.903333\n"
              + "\n"
              + "v_component_uncorrected, [1][2][1][1]\n"
              + "[0][0][0],";
      Test.ensureTrue(response.indexOf(match) > 0, "Middle of M0 doesn't match: " + match);
      match =
          "depth, [2]\n"
              + "6, 10\n"
              + "adcp_latitude, [1]\n"
              + "36.833333\n"
              + "adcp_longitude, [1]\n"
              + "-121.903333\n"
              + "\n";
      Test.ensureTrue(response.endsWith(match), "End of M0 doesn't match: " + match);
      // } catch (Exception e) {
      // String2.log(MustBe.throwableToString(e));
      // }

      // ****************
      // M1: get ascii response
      url =
          "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_adcp1417_20051020.nc.ascii?"
              + "u_component_uncorrected["
              + randomInt
              + ":1:"
              + randomInt
              + "][0:1:1][0:1:0][0:1:0],"
              + "v_component_uncorrected["
              + randomInt
              + ":1:"
              + randomInt
              + "][0:1:1][0:1:0][0:1:0]";
      String2.log("url=" + url);
      // try {
      response = SSR.getUrlResponseStringUnchanged(url);
      String2.log("response=" + response);
      match = "u_component_uncorrected, [1][2][1][1]\n" + "[0][0][0], ";
      Test.ensureTrue(response.startsWith(match), "Start of M1 response doesn't match: " + match);
      match =
          "depth, [2]\n"
              + "15.04, 23.04\n"
              + "adcp_latitude, [1]\n"
              + "36.764\n"
              + "adcp_longitude, [1]\n"
              + "-122.046\n"
              + "\n"
              + "v_component_uncorrected, [1][2][1][1]\n"
              + "[0][0][0],";
      Test.ensureTrue(response.indexOf(match) > 0, "Middle of M1 doesn't match: " + match);
      match =
          "depth, [2]\n"
              + "15.04, 23.04\n"
              + "adcp_latitude, [1]\n"
              + "36.764\n"
              + "adcp_longitude, [1]\n"
              + "-122.046\n"
              + "\n";
      Test.ensureTrue(response.endsWith(match), "End of M1 doesn't match: " + match);
      // } catch (Exception e) {
      // String2.log(MustBe.throwableToString(e));
      // }

      // ****************
      // M2: get ascii response
      url =
          "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_adcp1352_20060330.nc.ascii?"
              + "u_component_uncorrected["
              + randomInt
              + ":1:"
              + randomInt
              + "][0:1:1][0:1:0][0:1:0],"
              + "v_component_uncorrected["
              + randomInt
              + ":1:"
              + randomInt
              + "][0:1:1][0:1:0][0:1:0]";
      String2.log("url=" + url);
      // try {
      response = SSR.getUrlResponseStringUnchanged(url);
      String2.log("response=" + response);
      match = "u_component_uncorrected, [1][2][1][1]\n" + "[0][0][0], ";
      Test.ensureTrue(response.startsWith(match), "Start of M2 response doesn't match: " + match);
      match =
          "depth, [2]\n"
              + "15.04, 23.04\n"
              + "adcp_latitude, [1]\n"
              + "36.69\n"
              + "adcp_longitude, [1]\n"
              + "-122.338\n"
              + "\n"
              + "v_component_uncorrected, [1][2][1][1]\n"
              + "[0][0][0],";
      Test.ensureTrue(response.indexOf(match) > 0, "Middle of M2 doesn't match: " + match);
      match =
          "depth, [2]\n"
              + "15.04, 23.04\n"
              + "adcp_latitude, [1]\n"
              + "36.69\n"
              + "adcp_longitude, [1]\n"
              + "-122.338\n"
              + "\n";
      Test.ensureTrue(response.endsWith(match), "End of M2 doesn't match: " + match);
      // } catch (Exception e) {
      // String2.log(MustBe.throwableToString(e));
      // }

    }
  }

  /** This tests using this class to create a cache file. */
  @org.junit.jupiter.api.Test
  @TagSlowTests // Slow if it needs to build the cache.
  @TagExternalOther
  void basicTest() throws Exception {
    // verbose = true;
    // DataHelper.verbose = true;
    // Table.verbose = true;
    // StationVariableNc4D.verbose = true;
    // PointDataSet.verbose = true;

    String fileName;
    CacheOpendapStation cos;
    String response;
    Table table = new Table();

    // adcp stations chosen because I think they give gibberish answers sometimes
    // (ascii and opendap)
    String2.log("\n*** CacheOpendapStation.basicTest");

    // ***************
    // M0: get ascii response
    response =
        SSR.getUrlResponseStringUnchanged(
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_adcp1267_20060731.nc.ascii?"
                + "u_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0],v_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0]");
    Test.ensureEqual(
        response,
        /*
         * pre 6/22/2007, was
         * "u_component_uncorrected, [1][2][1][1]\n" +
         * "[0][0][0], 20.3\n" +
         * "[0][1][0], 6.9\n" +
         * "time, [1]\n" +
         * "1154396111\n" +
         * "depth, [2]\n" +
         * "6, 10\n" +
         * "latitude, [1]\n" + //was adcp_latitude
         * "36.833333\n" +
         * "longitude, [1]\n" + //was adcp_longitude
         * "-121.903333\n" +
         * "\n" +
         * "v_component_uncorrected, [1][2][1][1]\n" +
         * "[0][0][0], -24.3\n" +
         * "[0][1][0], -8.2\n" +
         * "time, [1]\n" +
         * "1154396111\n" +
         * "depth, [2]\n" +
         * "6, 10\n" +
         * "latitude, [1]\n" + //was adcp_latitude
         * "36.833333\n" +
         * "longitude, [1]\n" + //was adcp_longitude
         * "-121.903333\n" +
         * "\n",
         */
        // post 2010-07-19 is
        "Dataset: m0_adcp1267_20060731.nc\n"
            + "u_component_uncorrected.longitude, -121.903333\n"
            + "u_component_uncorrected.u_component_uncorrected[u_component_uncorrected.time=1154396111][u_component_uncorrected.depth=6][u_component_uncorrected.latitude=36.833333], 20.3\n"
            + "u_component_uncorrected.u_component_uncorrected[u_component_uncorrected.time=1154396111][u_component_uncorrected.depth=10][u_component_uncorrected.latitude=36.833333], 6.9\n"
            + "v_component_uncorrected.longitude, -121.903333\n"
            + "v_component_uncorrected.v_component_uncorrected[v_component_uncorrected.time=1154396111][v_component_uncorrected.depth=6][v_component_uncorrected.latitude=36.833333], -24.3\n"
            + "v_component_uncorrected.v_component_uncorrected[v_component_uncorrected.time=1154396111][v_component_uncorrected.depth=10][v_component_uncorrected.latitude=36.833333], -8.2\n",
        "response=" + response);

    // M0: make cache file
    fileName = File2.getSystemTempDirectory() + "/MBARI_M0_NRT_adcp.nc";
    cos =
        new CacheOpendapStation( // throws exception if trouble
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_adcp1267_20060731.nc",
            fileName,
            new String[] {"u_component_uncorrected", "v_component_uncorrected"});
    Test.ensureTrue(cos.createNewCache(), "m0 adcp");

    // M0: compare first part of cache file to ascii response
    // ***THE TEST WILL CHANGE IF THEY THROW OUT OLD NRT DATA.
    table.clear();
    table.read4DNc(fileName, null, 1, null, -1); // unpackWaht=1
    // String2.log(table.toString(10));
    Test.ensureEqual(table.nColumns(), 6, "");
    Test.ensureEqual(table.getColumnName(0), "longitude", ""); // was adcp_longitude
    Test.ensureEqual(table.getColumnName(1), "latitude", ""); // was adcp_latitude
    Test.ensureEqual(table.getColumnName(2), "depth", "");
    Test.ensureEqual(table.getColumnName(3), "time", "");
    Test.ensureEqual(table.getColumnName(4), "u_component_uncorrected", "");
    Test.ensureEqual(table.getColumnName(5), "v_component_uncorrected", "");
    Test.ensureEqual(table.getFloatData(0, 0), -121.903333f, "");
    Test.ensureEqual(table.getFloatData(1, 0), 36.833333f, "");
    Test.ensureEqual(table.getFloatData(2, 0), 6f, "");
    Test.ensureEqual(table.getDoubleData(3, 0), 1154396111, "");
    Test.ensureEqual(table.getFloatData(4, 0), 20.3f, "");
    Test.ensureEqual(table.getFloatData(5, 0), -24.3f, "");

    Test.ensureEqual(table.getFloatData(0, 1), -121.903333f, "");
    Test.ensureEqual(table.getFloatData(1, 1), 36.833333f, "");
    Test.ensureEqual(table.getFloatData(2, 1), 10f, "");
    Test.ensureEqual(table.getDoubleData(3, 1), 1154396111, "");
    Test.ensureEqual(table.getFloatData(4, 1), 6.9f, "");
    Test.ensureEqual(table.getFloatData(5, 1), -8.2f, "");

    /*
     * M1 and M2 tests work, but slow and no need to do all the time
     * //****************
     * //M1: get ascii response
     * //***THE TEST WILL CHANGE IF THEY THROW OUT OLD NRT DATA.
     * response = SSR.getUrlResponseStringUnchanged(
     * "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_adcp1417_20051020.nc.ascii?"
     * +
     * "u_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0],v_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0]"
     * );
     * Test.ensureEqual(response,
     * "u_component_uncorrected, [1][2][1][1]\n" +
     * "[0][0][0], -20.1\n" +
     * "[0][1][0], -18.6\n" +
     * "time, [1]\n" +
     * "1129838400\n" +
     * "depth, [2]\n" +
     * "15.04, 23.04\n" +
     * "adcp_latitude, [1]\n" +
     * "36.764\n" +
     * "adcp_longitude, [1]\n" +
     * "-122.046\n" +
     * "\n" +
     * "v_component_uncorrected, [1][2][1][1]\n" +
     * "[0][0][0], 0.2\n" +
     * "[0][1][0], -10.9\n" +
     * "time, [1]\n" +
     * "1129838400\n" +
     * "depth, [2]\n" +
     * "15.04, 23.04\n" +
     * "adcp_latitude, [1]\n" +
     * "36.764\n" +
     * "adcp_longitude, [1]\n" +
     * "-122.046\n" +
     * "\n", "");
     *
     * //M1: make cache file
     * fileName = "c:/temp/MBARI_M1_NRT_adcp.nc";
     * cos = new CacheOpendapStation(
     * "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_adcp1417_20051020.nc",
     * fileName,
     * new String[]{"u_component_uncorrected", "v_component_uncorrected"});
     * Test.ensureTrue(cos.createNewCache(), "m1 adcp");
     *
     * //M1: compare first part of cache file to ascii response
     * table.clear();
     * table.read4DNc(fileName, null, 1); //standardizeWhat=1
     * //String2.log(table.toString(10));
     * Test.ensureEqual(table.nColumns(), 6, "");
     * Test.ensureEqual(table.getColumnName(0), "adcp_longitude", "");
     * Test.ensureEqual(table.getColumnName(1), "adcp_latitude", "");
     * Test.ensureEqual(table.getColumnName(2), "depth", "");
     * Test.ensureEqual(table.getColumnName(3), "time", "");
     * Test.ensureEqual(table.getColumnName(4), "u_component_uncorrected", "");
     * Test.ensureEqual(table.getColumnName(5), "v_component_uncorrected", "");
     * Test.ensureEqual(table.getFloatData(0,0), -122.046f, "");
     * Test.ensureEqual(table.getFloatData(1,0), 36.764f, "");
     * Test.ensureEqual(table.getFloatData(2,0), 15.04f, "");
     * Test.ensureEqual(table.getDoubleData(3,0), 1129838400, "");
     * Test.ensureEqual(table.getFloatData(4,0), -20.1f, "");
     * Test.ensureEqual(table.getFloatData(5,0), 0.2f, "");
     *
     * Test.ensureEqual(table.getFloatData(0,1), -122.046f, "");
     * Test.ensureEqual(table.getFloatData(1,1), 36.764f, "");
     * Test.ensureEqual(table.getFloatData(2,1), 23.04f, "");
     * Test.ensureEqual(table.getDoubleData(3,1), 1129838400, "");
     * Test.ensureEqual(table.getFloatData(4,1), -18.6f, "");
     * Test.ensureEqual(table.getFloatData(5,1), -10.9f, "");
     *
     * //****************
     * //M2: get ascii response
     * //***THE TEST WILL CHANGE IF THEY THROW OUT OLD NRT DATA.
     * response = SSR.getUrlResponseStringUnchanged(
     * "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_adcp1352_20060330.nc.ascii?"
     * +
     * "u_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0],v_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0]"
     * );
     * Test.ensureEqual(response,
     * "u_component_uncorrected, [1][2][1][1]\n" +
     * "[0][0][0], -10.8\n" +
     * "[0][1][0], -10\n" +
     * "time, [1]\n" +
     * "1143752400\n" +
     * "depth, [2]\n" +
     * "15.04, 23.04\n" +
     * "adcp_latitude, [1]\n" +
     * "36.69\n" +
     * "adcp_longitude, [1]\n" +
     * "-122.338\n" +
     * "\n" +
     * "v_component_uncorrected, [1][2][1][1]\n" +
     * "[0][0][0], -18.8\n" +
     * "[0][1][0], -12.9\n" +
     * "time, [1]\n" +
     * "1143752400\n" +
     * "depth, [2]\n" +
     * "15.04, 23.04\n" +
     * "adcp_latitude, [1]\n" +
     * "36.69\n" +
     * "adcp_longitude, [1]\n" +
     * "-122.338\n" +
     * "\n", "");
     *
     * //M2: make cache file
     * fileName = "c:/temp/MBARI_M1_NRT_adcp.nc";
     * cos = new CacheOpendapStation(
     * "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_adcp1352_20060330.nc",
     * fileName,
     * new String[]{"u_component_uncorrected", "v_component_uncorrected"});
     * Test.ensureTrue(cos.createNewCache(), "m2 adcp");
     *
     * //M2: compare first part of cache file to ascii response
     * table.clear();
     * table.read4DNc(fileName, null, 1); //standardizeWhat=1
     * //String2.log(table.toString(10));
     * Test.ensureEqual(table.nColumns(), 6, "");
     * Test.ensureEqual(table.getColumnName(0), "adcp_longitude", "");
     * Test.ensureEqual(table.getColumnName(1), "adcp_latitude", "");
     * Test.ensureEqual(table.getColumnName(2), "depth", "");
     * Test.ensureEqual(table.getColumnName(3), "time", "");
     * Test.ensureEqual(table.getColumnName(4), "u_component_uncorrected", "");
     * Test.ensureEqual(table.getColumnName(5), "v_component_uncorrected", "");
     * Test.ensureEqual(table.getFloatData(0,0), -122.338f, "");
     * Test.ensureEqual(table.getFloatData(1,0), 36.69f, "");
     * Test.ensureEqual(table.getFloatData(2,0), 15.04f, "");
     * Test.ensureEqual(table.getDoubleData(3,0), 1143752400, "");
     * Test.ensureEqual(table.getFloatData(4,0), -10.8f, "");
     * Test.ensureEqual(table.getFloatData(5,0), -18.8f, "");
     *
     * Test.ensureEqual(table.getFloatData(0,1), -122.338f, "");
     * Test.ensureEqual(table.getFloatData(1,1), 36.69f, "");
     * Test.ensureEqual(table.getFloatData(2,1), 23.04f, "");
     * Test.ensureEqual(table.getDoubleData(3,1), 1143752400, "");
     * Test.ensureEqual(table.getFloatData(4,1), -10f, "");
     * Test.ensureEqual(table.getFloatData(5,1), -12.9f, "");
     *
     */
  }
}
