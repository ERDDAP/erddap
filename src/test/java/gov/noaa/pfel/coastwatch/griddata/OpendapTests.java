package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.String2;
import com.cohort.util.Test;

import dods.dap.DConnect;
import gov.noaa.pfel.coastwatch.util.SSR;
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
    opendap = new Opendap("https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/GA/ssta/3day", true, null);
    DConnect dConnect = new DConnect(opendap.url, opendap.acceptDeflate, 1, 1);
    opendap.getGridInfo(dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT),
        dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT), "GAssta", "-1.0e34");
    Test.ensureEqual(opendap.getLat(0), -44.975, ""); // I'm not sure about exact range, should be global data
    Test.ensureEqual(opendap.getLat(opendap.gridNLatValues - 1), 59.975, "");
    Test.ensureEqual(opendap.gridLatIncrement, .05, "");
    Test.ensureEqual(opendap.getLon(0), 180.025, "");
    Test.ensureEqual(opendap.getLon(opendap.gridNLonValues - 1), 329.975, "");
    Test.ensureEqual(opendap.gridLonIncrement, .05, "");

    // test THREDDS
    opendap = new Opendap( // was :8081
        "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/AG/ssta/3day",
        true, null);
    dConnect = new DConnect(opendap.url, opendap.acceptDeflate, 1, 1);
    opendap.getGridInfo(dConnect.getDAS(60000), dConnect.getDDS(60000), "AGssta", "-1.0e34");
    Test.ensureEqual(opendap.getLat(0), -75, "");
    Test.ensureEqual(opendap.getLat(opendap.gridNLatValues - 1), 75, "");
    Test.ensureEqual(opendap.gridLatIncrement, .1, "");
    Test.ensureEqual(opendap.getLon(0), 0, "");
    Test.ensureEqual(opendap.getLon(opendap.gridNLonValues - 1), 360, "");
    Test.ensureEqual(opendap.gridLonIncrement, .1, "");
    opendap.getTimeOptions(false,
        opendap.gridTimeFactorToGetSeconds,
        opendap.gridTimeBaseSeconds,
        3 * 24);

    // ensure not oddly spaced after makeLonPM180
    // data is 0..360, so ask for ~-180 to ~180
    String dir = SSR.getTempDirectory();
    Grid grid = opendap.makeGrid(opendap.timeOptions[0],
        -170, 170, 22, 50, 53, 37);
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
}
