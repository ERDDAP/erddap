package gov.noaa.pfel.coastwatch.pointdata;

import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import tags.TagIncompleteTest;
import tags.TagLargeFiles;
import testDataset.EDDTestDataset;

class PointDataSetFromStationVariablesTests {
  /**
   * This tests using this class to make caches and pointDataSets of mbari nrt
   * station data.
   * This also tests PointVectors.
   */
  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void testMakeCachesAndPointDataSets(boolean clearCache) throws Exception {
    // verbose = true;
    // DataHelper.verbose = true;
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // StationVariableNc4D.verbose = true;
    // PointDataSet.verbose = true;

    PointDataSet pds = null;
    Table table = null;
    // String2.log("\n***
    // PointDataSetFromStationVariables.testMakeCachesAndPointDataSets(" +
    // clearCache + ")");
    String dir = Path.of(PointDataSetFromStationVariablesTests.class.getResource("/data/").toURI()).toString();
    // try {
    // make mbari nrt PointDataSet
    ArrayList cacheOpendapStations = new ArrayList();
    ArrayList pointDataSets = new ArrayList();
    PointDataSetFromStationVariables.makeMbariNrtCachesAndDataSets(
        dir, -180, 180, -90, 90, true, // ensureUpToDate
        true, // throwExceptionIfAnyTrouble
        cacheOpendapStations, pointDataSets);

    // *** M0 current, zonal: do comparisons to info returned by ascii requests in
    // test above
    PointDataSet pdsu = null;
    String tName = "Current, Near Real Time, Zonal";
    for (int i = 0; i < pointDataSets.size(); i++) {
      PointDataSet tpds = (PointDataSet) pointDataSets.get(i);
      if (tpds.boldTitle.startsWith(tName)) {
        pdsu = tpds;
        break;
      }
    }
    if (pdsu == null) {
      String2.log(tName + " not found in ");
      for (int i = 0; i < pointDataSets.size(); i++) {
        PointDataSet tpds = (PointDataSet) pointDataSets.get(i);
        String2.log("  " + tpds.boldTitle);
      }
      Test.error("");
    }
    // Row LON LAT DEPTH TIME ID u_component_un
    // 0 -121.9031 36.83338 18 1194293891 MBARI M0 NRT a -0.031
    // 1 -121.9031 36.83338 18 1194294491 MBARI M0 NRT a -0.035
    table = pdsu.makeSubset(-121.91, -121.90, 36.83, 36.84, 15, 20,
        Calendar2.epochSecondsToIsoStringT(1194293891),
        Calendar2.epochSecondsToIsoStringT(1194294491));
    String2.log(table.toString());
    Test.ensureEqual(table.nRows(), 2, "");
    Test.ensureEqual(table.nColumns(), 6, "");
    Test.ensureEqual(table.getColumnName(0), "LON", "");
    Test.ensureEqual(table.getColumnName(1), "LAT", "");
    Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
    Test.ensureEqual(table.getColumnName(3), "TIME", "");
    Test.ensureEqual(table.getColumnName(4), "ID", "");
    Test.ensureEqual(table.getColumnName(5), "u_component_uncorrected", "");
    Test.ensureEqual(table.getFloatData(0, 0), -121.9031f, "");
    Test.ensureEqual(table.getFloatData(1, 0), 36.83338f, "");
    Test.ensureEqual(table.getFloatData(2, 0), 18f, "");
    Test.ensureEqual(table.getDoubleData(3, 0), 1194293891, "");
    Test.ensureEqual(table.getStringData(4, 0), "MBARI M0 NRT adcp", "");
    Test.ensureEqual(table.getFloatData(5, 0), -.031f, ""); // dataset returns m/s (not original cm/s)

    Test.ensureEqual(table.getFloatData(0, 1), -121.9031f, "");
    Test.ensureEqual(table.getFloatData(1, 1), 36.83338f, "");
    Test.ensureEqual(table.getFloatData(2, 1), 18f, "");
    Test.ensureEqual(table.getDoubleData(3, 1), 1194294491, "");
    Test.ensureEqual(table.getStringData(4, 1), "MBARI M0 NRT adcp", "");
    Test.ensureEqual(table.getFloatData(5, 1), -.035f, ""); // dataset returns m/s (not original cm/s)

    // M0 current, meridional
    PointDataSet pdsv = null;
    tName = "Current, Near Real Time, Meridional";
    for (int i = 0; i < pointDataSets.size(); i++) {
      PointDataSet tpds = (PointDataSet) pointDataSets.get(i);
      if (tpds.boldTitle.indexOf(tName) >= 0) {
        pdsv = tpds;
        break;
      }
    }
    if (pdsv == null) {
      String2.log(tName + " not found in ");
      for (int i = 0; i < pointDataSets.size(); i++) {
        PointDataSet tpds = (PointDataSet) pointDataSets.get(i);
        String2.log("  " + tpds.boldTitle);
      }
      Test.error("");
    }
    table = pdsv.makeSubset(-121.91, -121.90, 36.83, 36.84, 15, 20,
        Calendar2.epochSecondsToIsoStringT(1194293891),
        Calendar2.epochSecondsToIsoStringT(1194294491));
    String2.log(table.toString());
    Test.ensureEqual(table.nRows(), 2, "");
    Test.ensureEqual(table.nColumns(), 6, "");
    Test.ensureEqual(table.getColumnName(0), "LON", "");
    Test.ensureEqual(table.getColumnName(1), "LAT", "");
    Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
    Test.ensureEqual(table.getColumnName(3), "TIME", "");
    Test.ensureEqual(table.getColumnName(4), "ID", "");
    Test.ensureEqual(table.getColumnName(5), "v_component_uncorrected", "");
    Test.ensureEqual(table.getFloatData(0, 0), -121.9031f, "");
    Test.ensureEqual(table.getFloatData(1, 0), 36.83338f, "");
    Test.ensureEqual(table.getFloatData(2, 0), 18f, "");
    Test.ensureEqual(table.getDoubleData(3, 0), 1194293891, "");
    Test.ensureEqual(table.getStringData(4, 0), "MBARI M0 NRT adcp", "");
    Test.ensureEqual(table.getFloatData(5, 0), .145f, ""); // dataset returns m/s (not original cm/s)

    Test.ensureEqual(table.getFloatData(0, 1), -121.9031f, "");
    Test.ensureEqual(table.getFloatData(1, 1), 36.83338f, "");
    Test.ensureEqual(table.getFloatData(2, 1), 18f, "");
    Test.ensureEqual(table.getDoubleData(3, 1), 1194294491, "");
    Test.ensureEqual(table.getStringData(4, 1), "MBARI M0 NRT adcp", "");
    Test.ensureEqual(table.getFloatData(5, 1), .139f, ""); // dataset returns m/s (not original cm/s)

    // ** do equivalent test of PointVectors.makeAveragedTimeSeries
    table = PointVectors.makeAveragedTimeSeries(pdsu, pdsv,
        -121.91, -121.90, 36.83, 36.84, 15, 20,
        Calendar2.epochSecondsToIsoStringT(1194293891),
        Calendar2.epochSecondsToIsoStringT(1194294491), "pass");
    String2.log(table.toString());
    Test.ensureEqual(table.nRows(), 2, "");
    Test.ensureEqual(table.nColumns(), 7, "");
    Test.ensureEqual(table.getColumnName(0), "LON", "");
    Test.ensureEqual(table.getColumnName(1), "LAT", "");
    Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
    Test.ensureEqual(table.getColumnName(3), "TIME", "");
    Test.ensureEqual(table.getColumnName(4), "ID", "");
    Test.ensureEqual(table.getColumnName(5), "u_component_uncorrected", "");
    Test.ensureEqual(table.getColumnName(6), "v_component_uncorrected", "");
    Test.ensureEqual(table.getFloatData(0, 0), -121.9031f, "");
    Test.ensureEqual(table.getFloatData(1, 0), 36.83338f, "");
    Test.ensureEqual(table.getFloatData(2, 0), 18f, "");
    Test.ensureEqual(table.getDoubleData(3, 0), 1194293891, "");
    Test.ensureEqual(table.getStringData(4, 0), "MBARI M0 NRT adcp", "");
    Test.ensureEqual(table.getFloatData(5, 0), -.031f, ""); // dataset returns m/s (not original cm/s)
    Test.ensureEqual(table.getFloatData(6, 0), .145f, ""); // dataset returns m/s (not original cm/s)

    Test.ensureEqual(table.getFloatData(0, 1), -121.9031f, "");
    Test.ensureEqual(table.getFloatData(1, 1), 36.83338f, "");
    Test.ensureEqual(table.getFloatData(2, 1), 18f, "");
    Test.ensureEqual(table.getDoubleData(3, 1), 1194294491, "");
    Test.ensureEqual(table.getStringData(4, 1), "MBARI M0 NRT adcp", "");
    Test.ensureEqual(table.getFloatData(5, 1), -.035f, ""); // dataset returns m/s (not original cm/s)
    Test.ensureEqual(table.getFloatData(6, 1), .139f, ""); // dataset returns m/s (not original cm/s)
    // } catch (Exception e) {
    // String2.pressEnterToContinue(MustBe.throwableToString(e));
    // }

  }

  /**
   * This runs a test of this class and StationVariableNc4D.addToSubset.
   *
   * @throws Exception if trouble or no data
   */
  @org.junit.jupiter.api.Test
  void testMbariSqStations() throws Exception {
    // String2.log("\n*** PointDataSetFromStationVariables.testMbariSqStations");
    // CacheOpendapStation.verbose = true;
    // CacheOpendapStation.reallyVerbose = true;
    // DataHelper.verbose = true;
    // PointDataSet.verbose = true;
    // PointDataSet.reallyVerbose = true;
    // GroupVariable.verbose = false;
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    String dir = Path.of(PointDataSetFromStationVariablesTests.class.getResource("/data/").toURI()).toString();
    // to view, use, e.g.,:
    // \programs\nc361\ncdump -v DEPTH_HR f:\data\MBARI\MBARI_M0_SQ_adcp.nc
    // http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.ascii?TIME_HR

    ArrayList list = new ArrayList();
    PointDataSetFromStationVariables pointDataSet = null;
    Table table;
    double seconds;

    // try {
    // make the pointDataSets
    PointDataSetFromStationVariables.makeMbariSqCachesAndDataSets(dir,
        -180, 180, -90, 90, true, // ensureUpToDate
        true, // throwExceptionIfAnyTrouble
        null, list);
    Test.ensureEqual(list.size(), 11, "");

    // find PMBucur
    for (int i = 0; i < list.size(); i++) {
      pointDataSet = (PointDataSetFromStationVariables) list.get(i);
      if (pointDataSet.internalName.equals("PMBcrus"))
        break;
    }
    Test.ensureNotNull(pointDataSet, "pointDataSet is null");

    // PMBucur combines various depthLevels to make uniqueDepthLevels
    // m0 DEPTH_HR = 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55
    // m1 DEPTH_HR = 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80,
    // 85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155,
    // 160, 165, 170, 175, 180, 185, 190, 195, 200, 205, 210, 215, 220, 225,
    // 230, 235, 240, 245, 250, 255, 260, 265, 270, 275, 280, 285, 290, 295,
    // 300, 305, 310, 315, 320, 325, 330, 335, 340, 345, 350, 355, 360, 365,
    // 370, 375, 380, 385, 390, 395, 400, 405, 410, 415, 420, 425, 430, 435,
    // 440, 445, 450, 455, 460, 465, 470, 475, 480, 485, 490, 495, 500 ;
    Test.ensureEqual(String2.toCSSVString(pointDataSet.uniqueDepthLevels),
        "5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, " +
            "85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, " +
            "165, 170, 175, 180, 185, 190, 195, 200, 205, 210, 215, 220, 225, 230, 235, 240, " +
            "245, 250, 255, 260, 265, 270, 275, 280, 285, 290, 295, 300, 305, 310, 315, 320, " +
            "325, 330, 335, 340, 345, 350, 355, 360, 365, 370, 375, 380, 385, 390, 395, 400, " +
            "405, 410, 415, 420, 425, 430, 435, 440, 445, 450, 455, 460, 465, 470, 475, 480, " +
            "485, 490, 495, 500",
        "");

    Test.ensureEqual(pointDataSet.getStationMinTime("MBARI M0 SQ adcp"),
        1086285600, ""); // from opendap
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(pointDataSet.getStationMinTime("MBARI M0 SQ adcp")),
        "2004-06-03T18:00:00Z", ""); // converted to iso format
    String oneDayLater = "2004-06-04T18:00:00";
    String oneDayLater1 = "2004-06-04T19:00:00";

    // test m0 ucur 1 day later
    String2.log("\ntest m0");
    // http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.ascii?U_COMPONENT_UNCORR_HR[24:1:25][1:1:2][0:1:0][0:1:0]
    // reply:
    // U_COMPONENT_UNCORR_HR, [2][2][1][1]
    // [0][0][0], 2.91875
    // [0][1][0], 3.06146
    // [1][0][0], 4.20833
    // [1][1][0], 3.36562
    // TIME_HR, [2]
    // 1086372000, 1086375600
    // DEPTH_HR, [2]
    // 10, 15
    // LATITUDE_HR, [1]
    // 36.8339996337891
    // LONGITUDE_HR, [1]
    // 238.100006103516
    table = pointDataSet.makeSubset(238.1, 238.1, 36.834, 36.834,
        9, 16, // the second and 3rd depth values, 10 and 15
        oneDayLater, oneDayLater1);
    Test.ensureEqual(table.nRows(), 4, "");
    Test.ensureEqual(table.nColumns(), 6, "");

    // test m0 ucur 1 day later, 2nd z
    seconds = Calendar2.isoStringToEpochSeconds(oneDayLater);
    String2.log(oneDayLater + " (oneDayLater) converts to " + seconds);
    Test.ensureEqual(table.getFloatData(0, 0), 238.1f, "");
    Test.ensureEqual(table.getFloatData(1, 0), 36.834f, "");
    Test.ensureEqual(table.getDoubleData(2, 0), 10, "");
    Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
    Test.ensureEqual(table.getStringData(4, 0), "MBARI M0 SQ adcp", "");
    Test.ensureEqual(table.getFloatData(5, 0), .0291875f, ""); // ucur

    // test m0 ucur 1 day + 1 hour later, 2nd z
    seconds = Calendar2.isoStringToEpochSeconds(oneDayLater1);
    String2.log(oneDayLater1 + " (oneDayLater1) converts to " + seconds);
    Test.ensureEqual(table.getFloatData(0, 1), 238.1f, "");
    Test.ensureEqual(table.getFloatData(1, 1), 36.834f, "");
    Test.ensureEqual(table.getDoubleData(2, 1), 10, "");
    Test.ensureEqual(table.getDoubleData(3, 1), seconds, "");
    Test.ensureEqual(table.getStringData(4, 1), "MBARI M0 SQ adcp", "");
    Test.ensureEqual(table.getFloatData(5, 1), .0420833f, ""); // ucur

    // test m0 ucur 1 day later, 3rd z
    seconds = Calendar2.isoStringToEpochSeconds(oneDayLater);
    Test.ensureEqual(table.getFloatData(0, 2), 238.1f, "");
    Test.ensureEqual(table.getFloatData(1, 2), 36.834f, "");
    Test.ensureEqual(table.getDoubleData(2, 2), 15, "");
    Test.ensureEqual(table.getDoubleData(3, 2), seconds, "");
    Test.ensureEqual(table.getStringData(4, 2), "MBARI M0 SQ adcp", "");
    Test.ensureEqual(table.getFloatData(5, 2), .0306146f, ""); // ucur

    // test m0 ucur 1 day + 1 hour later, 3rd z
    seconds = Calendar2.isoStringToEpochSeconds(oneDayLater1);
    Test.ensureEqual(table.getFloatData(0, 3), 238.1f, "");
    Test.ensureEqual(table.getFloatData(1, 3), 36.834f, "");
    Test.ensureEqual(table.getDoubleData(2, 3), 15, "");
    Test.ensureEqual(table.getDoubleData(3, 3), seconds, "");
    Test.ensureEqual(table.getStringData(4, 3), "MBARI M0 SQ adcp", "");
    Test.ensureEqual(table.getFloatData(5, 3), .03365625f, ""); // ucur

    // *** and the related tests of makeAveragedTimeSeries (with 2 z values!)
    table = pointDataSet.makeAveragedTimeSeries(238.1, 238.1, 36.834, 36.834,
        9, 16, // the second and 3rd depth values, 10 and 15
        "2004-06-04", "2004-06-04", "1 day");
    Test.ensureEqual(table.nRows(), 2, "");
    Test.ensureEqual(table.nColumns(), 6, "");

    // test m0 ucur 1 day later, 2nd z
    // 0 time is 18:00:00 so start of next day is element 6
    // http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.ascii?U_COMPONENT_UNCORR_HR[6:1:29][1:1:1][0:1:0][0:1:0]
    String values = "-6.70208, -3.53021, 0.135417, 3.94375, 2.28646, 2.90729, 2.51354, -0.00624998, -3.62604, -8.55, " +
        "-11.5198, -10.8156, -12.4094, -9.25833, -7.99271, -5.2, -3.44896, 0.759375, 2.91875, 4.20833" +
        ", 5.21667, 6.64792, 2.33646, 0.494792";
    DoubleArray da = new DoubleArray(String2.csvToDoubleArray(values));
    da.scaleAddOffset(.01, 0); // convert cm/s to m/s
    double stats[] = da.calculateStats();
    seconds = Calendar2.isoStringToEpochSeconds("2004-06-04 12:00:00");
    Test.ensureEqual(table.getFloatData(0, 0), 238.1f, "");
    Test.ensureEqual(table.getFloatData(1, 0), 36.834f, "");
    Test.ensureEqual(table.getDoubleData(2, 0), 10, "");
    Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
    Test.ensureEqual(table.getStringData(4, 0), "MBARI M0 SQ adcp", "");
    Test.ensureEqual(table.getFloatData(5, 0),
        (float) (stats[PrimitiveArray.STATS_SUM] / stats[PrimitiveArray.STATS_N]), ""); // ucur

    // test m0 ucur 1 day + 1 hour later, 3rd z
    // http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.ascii?U_COMPONENT_UNCORR_HR[6:1:29][2:1:2][0:1:0][0:1:0]
    values = "-5.79792, -3.39375, 2.05208, 4.26875, 2.09687, 1.73854, 0.369792, -0.767708" +
        ", -1.71354, -4.38646, -7.70208, -9.68125, -14.2104, -11.6458, -9.92083, -6.72604" +
        ", -3.97604, 0.278125, 3.06146, 3.36562, 2.60313, 2.70417, 0.308333, 0.01875";
    da = new DoubleArray(String2.csvToDoubleArray(values));
    da.scaleAddOffset(.01, 0); // convert cm/s to m/s
    stats = da.calculateStats();
    Test.ensureEqual(table.getFloatData(0, 1), 238.1f, "");
    Test.ensureEqual(table.getFloatData(1, 1), 36.834f, "");
    Test.ensureEqual(table.getDoubleData(2, 1), 15, ""); // yes, it averages depth levels separately
    Test.ensureEqual(table.getDoubleData(3, 1), seconds, "");
    Test.ensureEqual(table.getStringData(4, 1), "MBARI M0 SQ adcp", "");
    Test.ensureEqual(table.getFloatData(5, 1),
        (float) (stats[PrimitiveArray.STATS_SUM] / stats[PrimitiveArray.STATS_N]), ""); // ucur
    // } catch (Exception e) {
    // String2.pressEnterToContinue(MustBe.throwableToString(e));
    // }

  }

  /**
   * This runs a test of this class and StationVariableNc4D.makeSubset with NDBC
   * stations.
   *
   * @throws Exception if trouble or no data
   */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testNc4DMakeSubset() throws Exception {
    // String2.log("\n*** PointDataSetFromStationVariables.testNc4DMakeSubset");
    // DataHelper.verbose = true;
    // PointDataSet.verbose = true;
    // GroupVariable.verbose = false;
    // NdbcMetStation.verbose = true;

    // String2.log(NcHelper.ncdump("c:/data/ndbcMet4D/TAML1.nc", "-h"));

    ArrayList list = new ArrayList();
    PointDataSet pointDataSet = null;
    Table table;
    double seconds;

    // option 1) more direct test of makePointDataSets
    // list.clear();
    // makePointDataSets(list,
    // "PN2", //String internalDataSetBaseName,
    // "NDBC Meteorological", //String userDataSetBaseName,
    // "c:/data/ndbcMet/",
    // //"(31201.nc|46088.nc|TAML1.nc)", //just 3 files
    // ".+\\.nc", //all files
    // new String[]{ //variableInfo[]
    // "WTMP` wtmp` Water Temperature` Rainbow` Linear` 1` 8` 32` degree_C` NaN"},
    // //"WSPU` wspu` Zonal Wind Speed` BlueWhiteRed` Linear` 1` -20` 20` m s^-1`
    // NaN",
    // //"WSPV` wspv` Merid. Wind Speed` BlueWhiteRed` Linear` 1` -20` 20` m s^-1`
    // NaN"},
    // NdbcMetStation.courtesy, -180, 180, -90, 90);
    // Test.ensureEqual(list.size(), 1, "");
    // pointDataSet = (PointDataSet)list.get(0);

    // option 2) test of NdbcMetStation.addPointDataSets
    list.clear();
    String dir = Path.of(PointDataSetFromStationVariablesTests.class.getResource("/veryLarge/points/ndbcMet/").toURI()).toString();
    NdbcMetStation.addPointDataSets(list, dir, -180, 180, -90, 90);
    for (int i = 0; i < list.size(); i++) {
      pointDataSet = (PointDataSet) list.get(i);
      if (pointDataSet.internalName.equals("PNBwtmp"))
        break;
    }
    Test.ensureNotNull(pointDataSet, "pointDataSet is null");

    // test 31201
    String2.log("\n*** whole world: test 31201");
    // table is x,y,z,t,id,data
    // YYYY MM DD hh mm WD WSPD GST WVHT DPD APD MWD BARO ATMP WTMP DEWP VIS TIDE
    // 2005 04 25 18 00 999 99.0 99.0 3.90 8.00 99.00 999 9999.0 999.0 23.9 999.0
    // 99.0 99.00
    // use of floats ensures that makeSubset() can handle imprecise requests
    table = pointDataSet.makeSubset(311.87f, 311.87f, -27.7f, -27.7f, 0, 0,
        "2005-04-25T18:00:00", "2005-04-25T18:00:00");
    seconds = Calendar2.isoStringToEpochSeconds("2005-04-25T18:00:00");
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 6, "");
    // No rows in table
    // Test.ensureEqual(table.getDoubleData(0, 0), 311.87, "");
    // Test.ensureEqual(table.getDoubleData(1, 0), -27.7, "");
    // Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
    // Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
    // Test.ensureEqual(table.getStringData(4, 0), "NDBC 31201 met", "");
    // Test.ensureEqual(table.getFloatData(5, 0), 23.9f, ""); // wtmp

    // test 46088 test lon 0..360
    String2.log("\n*** whole world: test 46088");
    // YYYY MM DD hh mm WD WSPD GST WVHT DPD APD MWD BAR ATMP WTMP DEWP VIS TIDE
    // 2005 12 31 23 30 11 1.9 2.2 99.00 99.00 99.00 999 987.7 8.5 8.6 7.4 99.0
    // 99.00
    table = pointDataSet.makeSubset(-123.17f, -123.17f, 48.33f, 48.33f, 0, 0,
        "2006-01-01T00:00:00", "2006-01-01T00:00:00");
    seconds = Calendar2.isoStringToEpochSeconds("2006-01-01T00:00:00");
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 6, "");
    // No rows in table
    // Test.ensureEqual(table.getDoubleData(0, 0), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 0), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 0), 0f, "");
    // Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
    // Test.ensureEqual(table.getStringData(4, 0), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 0), 8.6f, ""); // wtmp

    // test TAML1
    String2.log("\n*** whole world: test TAML1");
    // YYYY MM DD hh WD WSPD GST WVHT DPD APD MWD BAR ATMP WTMP DEWP VIS TIDE
    // 2004 12 31 23 90 5.1 6.2 99.00 99.00 99.00 999 1022.9 16.3 13.7 15.2 99.0
    // -0.04 //last row of first year
    table = pointDataSet.makeSubset(-90.67f, -90.67f,
        29.19f, 29.19f, 0, 0, "2004-01-01T01:00:00", "2004-01-01T01:00:00");
    seconds = Calendar2.isoStringToEpochSeconds("2004-01-01T01:00:00");
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 6, "");
    // No rows in table
    // Test.ensureEqual(table.getDoubleData(0, 0), -90.67, "");
    // Test.ensureEqual(table.getDoubleData(1, 0), 29.19, "");
    // Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
    // Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
    // Test.ensureEqual(table.getStringData(4, 0), "NDBC TAML1 met", "");
    // Test.ensureEqual(table.getFloatData(5, 0), 13.7f, ""); // wtmp

    // all stations, 1 time ? ms
    String2.log("\n*** whole world: all stations, 1 time, lonPM180");
    table = pointDataSet.makeSubset(
        -180, 180, -90, 90, 0, 0, "2004-01-07", "2004-01-07");
    // String2.log(tTable.toString());
    int tn = table.nRows();
    // this changes, but it is good to keep the test in case the number changes
    // badly (e.g., smaller)
    Test.ensureEqual(table.nRows(), 1169, "all sta, 1 time, nRows");
    Test.ensureEqual(table.nColumns(), 6, "all sta, 1 time, nColumns");
    Test.ensureEqual(Calendar2.isoStringToEpochSeconds("2004-01-07"), 1073433600, "time check");
    for (int i = 0; i < tn; i++) {
      Test.ensureEqual(table.getDoubleData(3, i), 1073433600, "all sta, 1 time, time, row " + i);
    }
    int row = table.getColumn(0).indexOf("-85.5", 0);
    Test.ensureEqual(table.getDoubleData(0, row), -85.5, "all sta, 1 time, lon row 0");
    Test.ensureEqual(table.getDoubleData(1, row), 27.5, "all sta, 1 time, lat row 0");
    Test.ensureEqual(table.getFloatData(5, row), Float.NaN, "all sta, 1 time, wtmp row 0");
    row = table.getColumn(0).indexOf("-88.496", 0);
    Test.ensureEqual(table.getDoubleData(0, row), -88.496, "all sta, 1 time, lon row 1");
    Test.ensureEqual(table.getDoubleData(1, row), 28.191, "all sta, 1 time, lat row 1");
    Test.ensureEqual(table.getFloatData(5, row), Float.NaN, "all sta, 1 time, wtmp row 1");

    // westus, 2 time points ? ms
    String2.log("\n*** westus, 2 time points");
    table = pointDataSet.makeSubset(
        -135, -105, 22, 50, 0, 0, "2004-01-07T00", "2004-01-07T01");
    // String2.log(table.toString(1000));
    // nRows changes a little sometimes...
    Test.ensureEqual(table.nRows(), 243, "westus, 2 time points, nRows");
    Test.ensureEqual(table.nColumns(), 6, "westus, 2 time points, nColumns");
    row = table.getColumn(0).indexOf("-123.708", 0);
    // row = table.getColumn(3).indexOf("1073433600", row);
    Test.ensureEqual(table.getDoubleData(0, row), -123.708, "westus, 2 time points, lon row 50");
    Test.ensureEqual(table.getDoubleData(1, row), 38.913, "westus, 2 time points, lat row 50");
    Test.ensureEqual(table.getDoubleData(3, row), 1073437200, "westus, 2 time points, time row 50");
    Test.ensureEqual(table.getFloatData(5, row), Float.NaN, "westus, 2 time points, wtmp row 50");
    // row = table.getColumn(0).indexOf("-118", 0);
    row = table.getColumn(3).indexOf("1073433600", row);
    Test.ensureEqual(table.getDoubleData(0, row), -124.375, "westus, 2 time points, lon row 51");
    Test.ensureEqual(table.getDoubleData(1, row), 43.342, "westus, 2 time points, lat row 51");
    Test.ensureEqual(table.getDoubleData(3, row), 1073433600, "westus, 2 time points, time row 51");
    Test.ensureEqual(table.getFloatData(5, row), Float.NaN, "westus, 2 time points, wtmp row 51");

    // all stations 1 month, lonPM180
    String2.log("\n*** whole world: all stations, 1 month, lonPM180");
    table = pointDataSet.makeSubset(
        -180, 180, -90, 90, 0, 0, "2004-01-01", "2004-02-01");
    // String2.log(table.toString(1000));
    table.convertToFakeMissingValues(); // so I see what file will look like
    String2.log(table.toString(1));
    tn = table.nRows();
    for (int i = 0; i < tn; i++) {
      double tLon = table.getDoubleData(0, i);
      Test.ensureTrue(tLon >= -180 && tLon < 180, "all sta, 1 month, lonPM180, avg, row" + i + "=" + tLon);
    }
    // Test.ensureEqual(table.nRows(), 130227, "all sta, 1 month, lonPM180, avg,
    // nRows"); //circular logic, changes a little sometimes
    Test.ensureEqual(table.nColumns(), 6, "all sta, 1 month, lonPM180, avg, nColumns");
  }

  /**
   * This runs a test of this class and PointDataSet.makeAveragedTimeSeries with
   * NDBC stations.
   *
   * @throws Exception if trouble or no data
   */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testNc4DMakeAveragedTimeSeries() throws Exception {
    // String2.log("\n***
    // PointDataSetFromStationVariables.testNc4DmakeAveragedTimeSeries");
    // DataHelper.verbose = true;
    // PointDataSet.verbose = true;
    // GroupVariable.verbose = false;
    // NdbcMetStation.verbose = true;

    // String2.log(NcHelper.ncdump("c:/data/ndbcMet4D/TAML1.nc", "-h"));

    ArrayList list = new ArrayList();
    PointDataSet pointDataSet = null;
    Table table;
    double seconds;

    // *** test of NdbcMetStation.addPointDataSets WITH RANGE LIMITATION
    list.clear();
    String dir = Path.of(PointDataSetFromStationVariablesTests.class.getResource("/veryLarge/points/ndbcMet/").toURI()).toString();
    NdbcMetStation.addPointDataSets(list, dir,
        -135 + 360, -105 + 360, 22, 50); // set range limitation to westus in 0..360
    pointDataSet = null;
    for (int i = 0; i < list.size(); i++) {
      pointDataSet = (PointDataSet) list.get(i);
      if (pointDataSet.internalName.equals("PNBwtmp"))
        break;
    }
    Test.ensureNotNull(pointDataSet, "pointDataSet is null");

    // westus, 2 time points ? ms
    String2.log("\n*** westus: 2 time points");
    table = pointDataSet.makeSubset(
        -135, -105, 22, 50, 0, 0, "2004-01-07T00", "2004-01-07T01"); // then ask for data in -180..180
    // String2.log(table.toString(1000));
    // nRows changes sometimes...
    Test.ensureEqual(table.nRows(), 243, "westus, 2 time points, nRows");
    Test.ensureEqual(table.nColumns(), 6, "westus, 2 time points, nColumns");
    // int row = table.getColumn(0).indexOf("-118", 0);
    int row = table.getColumn(3).indexOf("1073433600");
    Test.ensureEqual(table.getDoubleData(0, row), -130.46, "westus, 2 time points, lon row");
    Test.ensureEqual(table.getDoubleData(1, row), 42.57, "westus, 2 time points, lat row");
    Test.ensureEqual(table.getDoubleData(3, row), 1073433600, "westus, 2 time points, time row");
    Test.ensureEqual(table.getFloatData(5, row), Float.NaN, "westus, 2 time points, wtmp row");
    Test.ensureEqual(table.getDoubleData(0, row + 1), -130.46, "westus, 2 time points, lon row+1");
    Test.ensureEqual(table.getDoubleData(1, row + 1), 42.57, "westus, 2 time points, lat row+1");
    Test.ensureEqual(table.getDoubleData(3, row + 1), 1073437200, "westus, 2 time points, time row+1");
    Test.ensureEqual(table.getFloatData(5, row + 1), Float.NaN, "westus, 2 time points, wtmp row+1");

    // *** test of makeAveragedTimeSeries -123.17 48.33 is station 46088
    // 1 observation
    table = pointDataSet.makeAveragedTimeSeries(-123.17, -123.17, 48.33, 48.33, 0, 0,
        "2005-10-01", "2005-10-02", "1 observation");
    Test.ensureEqual(table.nRows(), 0, "");
    // No rows
    // Test.ensureEqual(table.getDoubleData(0, 0), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 0), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 0)), "2005-10-01T00:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 0), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 0), 11.4f, "");

    // Test.ensureEqual(table.getDoubleData(0, 1), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 1), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 1), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 1)), "2005-10-01T01:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 1), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 1), 11.4f, "");

    // Test.ensureEqual(table.getDoubleData(0, 24), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 24), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 24), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 24)), "2005-10-02T00:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 24), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 24), 11.9f, "");

    // 1 day
    table = pointDataSet.makeAveragedTimeSeries(-123.17, -123.17, 48.33, 48.33, 0, 0,
        "2005-10-01", "2005-11-01", "1 day");
    Test.ensureEqual(table.nRows(), 0, "");
    // Table has no rows
    // Test.ensureEqual(table.getDoubleData(0, 0), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 0), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 0)), "2005-10-01T12:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 0), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 0), 11.2375f, "");
    // double average = pointDataSet.calculateAverage( // independent test
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2005-10-01 00:00:00", "2005-10-01 23:00:00");
    // Test.ensureEqual((float) average, 11.2375f, "");

    // Test.ensureEqual(table.getDoubleData(0, 1), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 1), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 1), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 1)), "2005-10-02T12:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 1), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 1), 11.154166f, "");
    // average = pointDataSet.calculateAverage(
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2005-10-02 00:00:00", "2005-10-02 23:00:00");
    // Test.ensureEqual((float) average, 11.154166f, "");

    // Test.ensureEqual(table.getDoubleData(0, 31), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 31), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 31), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 31)), "2005-11-01T12:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 31), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 31), 10.025001f, "");
    // average = pointDataSet.calculateAverage(
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2005-11-01 00:00:00", "2005-11-01 23:00:00");
    // Test.ensureEqual((float) average, 10.025001f, "");

    // 8 day
    table = pointDataSet.makeAveragedTimeSeries(-123.17, -123.17, 48.33, 48.33, 0, 0,
        "2005-10-01", "2005-11-01", "8 day");
    Test.ensureEqual(table.nRows(), 0, "");
    // Test.ensureEqual(table.getDoubleData(0, 0), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 0), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 0)), "2005-10-01T00:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 0), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 0), 11.261979f, "");
    // double average = pointDataSet.calculateAverage(
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2005-09-27 00:00:00", "2005-10-04 23:00:00");
    // Test.ensureEqual((float) average, 11.261979f, "");

    // Test.ensureEqual(table.getDoubleData(0, 1), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 1), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 1), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 1)), "2005-10-02T00:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 1), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 1), 11.144271f, "");
    // average = pointDataSet.calculateAverage(
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2005-09-28 00:00:00", "2005-10-05 23:00:00");
    // Test.ensureEqual((float) average, 11.144271f, "");

    // Test.ensureEqual(table.getDoubleData(0, 31), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 31), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 31), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 31)), "2005-11-01T00:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 31), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 31), 10.145312f, "");
    // average = pointDataSet.calculateAverage(
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2005-10-28 00:00:00", "2005-11-04 23:00:00");
    // Test.ensureEqual((float) average, 10.145312f, "");

    // 1 month
    table = pointDataSet.makeAveragedTimeSeries(-123.17, -123.17, 48.33, 48.33, 0, 0,
        "2004-10-01", "2005-10-01", "1 month");
    Test.ensureEqual(table.nRows(), 0, "");
    // Table has no rows
    // Test.ensureEqual(table.getDoubleData(0, 0), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 0), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 0)), "2004-10-16T12:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 0), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 0), 10.540457f, "");
    // double average = pointDataSet.calculateAverage(
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2004-10-01 00:00:00", "2004-10-31 23:00:00");
    // Test.ensureEqual((float) average, 10.540457f, "");

    // Test.ensureEqual(table.getDoubleData(0, 1), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 1), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 1), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 1)), "2004-11-16T00:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 1), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 1), 9.550487f, "");
    // average = pointDataSet.calculateAverage(
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2004-11-01 00:00:00", "2004-11-30 23:00:00");
    // Test.ensureEqual((float) average, 9.550487f, "");

    // Test.ensureEqual(table.getDoubleData(0, 12), -123.17, "");
    // Test.ensureEqual(table.getDoubleData(1, 12), 48.33, "");
    // Test.ensureEqual(table.getDoubleData(2, 12), 0, "");
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(3, 12)), "2005-10-16T12:00:00Z", "");
    // Test.ensureEqual(table.getStringData(4, 12), "NDBC 46088 met", "");
    // Test.ensureEqual(table.getFloatData(5, 12), 10.479704f, "");
    // average = pointDataSet.calculateAverage(
    //     -123.17, -123.17, 48.33, 48.33, 0, 0, "2005-10-01 00:00:00", "2005-10-31 23:00:00");
    // Test.ensureEqual((float) average, 10.479704f, "");

    // String2.log("\n  end PointDataSetFromStationVariables.testNc4DmakeAveragedTimeSeries");
  }

  /**
   * This runs a test of this class and StationVariableOpendap4D.
   *
   * @throws Exception if trouble or no data
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    // PointDataSet.reallyVerbose = true;
    // inDevelopment = true;
    /*
     * //based on ndbc data
     * testNc4DMakeSubset();
     * testNc4DMakeAveragedTimeSeries();
     * //String2.pressEnterToContinue("Check things over.");
     */
    /**
     * MBARI datasets are inactive as of 2008-07-07
     * //mbari data
     * //testMakeCachesAndPointDataSets(false); //clearCache
     * //testMbariSqStations();
     * 
     * //test mbari nrt data
     * try {
     * ArrayList cacheOpendapStations = new ArrayList();
     * ArrayList pointDataSets = new ArrayList();
     * makeMbariNrtCachesAndDataSets(
     * "c:/data/", -180, 180, -90, 90, true, //ensureUpToDate
     * true, //throwExceptionIfAnyTrouble (this commonly true just here for testing)
     * cacheOpendapStations, pointDataSets);
     * Test.ensureEqual(cacheOpendapStations.size(), 9, "");
     * Test.ensureEqual(pointDataSets.size(), 10, "");
     * } catch (Exception e) {
     * String2.log(MustBe.throwableToString(e));
     * String2.pressEnterToContinue("\nRecover from mbari nrt failure?");
     * }
     */

  }

  /**
     * This remakes the caches of the mbari nrt and sq station data in f:/data/MBARI.
     */
    @TagIncompleteTest // Not really a test
    void remakeMbariCachesAndDataSets() throws Exception {

      File2.deleteAllFiles("c:/data/MBARI");

      ArrayList cacheOpendapStations = new ArrayList();
      ArrayList pointDataSets = new ArrayList();
      PointDataSetFromStationVariables.makeMbariNrtCachesAndDataSets(
          "c:/data/", -180, 180, -90, 90, true, //ensureUpToDate
          true, //throwExceptionIfAnyTrouble    (this commonly true just here for testing)
          cacheOpendapStations, pointDataSets);

          PointDataSetFromStationVariables.makeMbariSqCachesAndDataSets(
          "c:/data/", -180, 180, -90, 90, true, //ensureUpToDate
          true, //throwExceptionIfAnyTrouble    (this commonly true just here for testing) 
          cacheOpendapStations, pointDataSets);
  }
}
