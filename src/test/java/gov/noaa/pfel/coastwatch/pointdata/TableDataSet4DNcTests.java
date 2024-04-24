package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.util.Calendar2;
import com.cohort.util.Test;

import tags.TagLargeFiles;

class TableDataSet4DNcTests {

  /**
   * This tests the methods of this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void basicTest() throws Exception {
    // verbose = true;
    TableDataSet4DNc dataset = new TableDataSet4DNc(
        "4NBmeto", "NDBC Meteorological",
        TableDataSet4DNcTests.class.getResource("/veryLarge/points/ndbcMet2/historical/").getPath(),
        // ".+\\.nc");
        "NDBC_41..._met.nc");

    // this mimics NdbcMetStation.test41015
    Table table = dataset.makeSubset("1993-05-23 18:00:00", "1993-05-23 18:00:00",
        new String[] { "NDBC 41015 met" },
        new String[] {
            "WD", "WSPD", "GST", "WVHT", "DPD", // 5...
            "APD", "MWD", "BAR", "ATMP", "WTMP", // 10...
            "DEWP", "VIS", "PTDY", "TIDE", "WSPU", // 15...
            "WSPV" }); // 20...
    // row of data from 41015h1993.txt
    // YY MM DD hh WD WSPD GST WVHT DPD APD MWD BAR ATMP WTMP DEWP VIS
    // 93 05 23 18 303 00.1 00.6 99.00 99.00 99.00 999 1021.1 19.9 18.4 999.0 99.0
    // //first available
    double seconds = Calendar2.isoStringToEpochSeconds("1993-05-23T18");
    int row = table.getColumn(3).indexOf("" + seconds, 0); // should find exact match
    Test.ensureEqual(table.getFloatData(0, row), -75.3f, "");
    Test.ensureEqual(table.getFloatData(1, row), 35.4f, "");
    Test.ensureEqual(table.getDoubleData(2, row), 0, "");
    Test.ensureEqual(table.getStringData(4, row), "NDBC 41015 met", "");
    Test.ensureEqual(table.getDoubleData(5, row), 303, "");
    Test.ensureEqual(table.getFloatData(6, row), .1f, "");
    Test.ensureEqual(table.getFloatData(7, row), .6f, "");
    Test.ensureEqual(table.getDoubleData(8, row), Double.NaN, "");
    Test.ensureEqual(table.getDoubleData(9, row), Double.NaN, "");
    Test.ensureEqual(table.getDoubleData(10, row), Double.NaN, "");
    Test.ensureEqual(table.getIntData(11, row), 32767, "");
    Test.ensureEqual(table.getFloatData(12, row), 1021.1f, "");
    Test.ensureEqual(table.getFloatData(13, row), 19.9f, "");
    Test.ensureEqual(table.getFloatData(14, row), 18.4f, "");
    Test.ensureEqual(table.getFloatData(15, row), Float.NaN, "");
    Test.ensureEqual(table.getFloatData(16, row), Float.NaN, "");
  }

}
