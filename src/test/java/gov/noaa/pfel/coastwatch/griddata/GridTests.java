package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import tags.TagMissingFile;

public class GridTests {
  public static final String testDir = GridTests.class.getResource("/data/gridTests/").getPath();
  public static final String testName = "OQNux10S1day_20050712_x-135_X-105_y22_Y50";

  /** This tests the little static methods. */
  @org.junit.jupiter.api.Test
  void testLittleMethods() {
    String2.log("\n*** Grid.testLittleMethods...");

    // generateContourLevels(String contourString, double minData, double maxData) {
    Test.ensureEqual( // single value in string
        Grid.generateContourLevels("1000", -2500, 2500),
        new double[] {-2000, -1000, 0, 1000, 2000},
        "generateContourLevels a");
    Test.ensureEqual( // single value, catch end points
        Grid.generateContourLevels("1000", -2000, 2000),
        new double[] {-2000, -1000, 0, 1000, 2000},
        "generateContourLevels b");
    Test.ensureEqual( // csv
        Grid.generateContourLevels("1000, 2000", -2500, 2500),
        new double[] {1000, 2000},
        "generateContourLevels c");
    Test.ensureEqual( // aware of min, max
        Grid.generateContourLevels("-3000, 1000, 3000", -2500, 2500),
        new double[] {1000},
        "generateContourLevels d");
  }

  /**
   * This tests for a memory leak in readGrd.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void testForMemoryLeak() throws Exception {
    Grid grid = new Grid();
    String dir;

    // one time test for memory leak:
    // I put all the zip files from /u00/data/PH/... in dir and unzipped them
    // String dir = "c:\\programs\\GrdFiles\\";
    // String[] zipFiles = RegexFilenameFilter.list(dir, ".*\\.zip");
    // for (int i = 0; i < zipFiles.length; i++)
    // SSR.unzip(dir + zipFiles[i], dir, true, null);
    // if (true) return;

    // test for memory leak in readGrd
    dir = "c:\\programs\\GrdFiles\\";
    String[] grdFiles = RegexFilenameFilter.list(dir, ".*\\.grd");
    Math2.gcAndWait("Grid (between tests)");
    Math2.gcAndWait("Grid (between tests)"); // before get memoryString() in a test
    long um = Math2.getMemoryInUse();
    String2.log("\n***** Grid.testForMemoryLeak; at start: " + Math2.memoryString());
    for (int i = 0; i < Math.min(50, grdFiles.length); i++) {
      grid.readGrd(dir + grdFiles[i], true);
      // Math2.gcAndWait("Grid (between tests)"); //2013-12-05 Commented out. In a
      // test, let Java handle memory.
    }
    grid = null;
    Math2.gcAndWait("Grid (between tests)");
    Math2.gcAndWait("Grid (between tests)"); // in a test, before getMemoryInUse()
    grid = new Grid();
    long increase = Math2.getMemoryInUse() - um;
    String2.log("Memory used change after MemoryLeak test: " + increase);
    if (increase > 50000)
      throw new Exception("Memory usage increased: " + increase + " memory leak suspected.");
    else Math2.gcAndWait("Grid (between tests)"); // in a test, a pause after message displayed
  }

  /**
   * This test readGrid reading a subset.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testReadGrdSubset() throws Exception {
    String2.log("\n***** Grid.testReadGrdSubset");
    Grid.verbose = true;
    long time = System.currentTimeMillis();
    Grid grid = new Grid();

    grid.readGrd(
        testDir + testName + ".grd", -130, -125, 40, 47, Integer.MAX_VALUE, Integer.MAX_VALUE);
    int nLat = grid.lat.length;
    int nLon = grid.lon.length;
    int nData = grid.data.length;
    Test.ensureEqual(nLat, 29, "subset nLat");
    Test.ensureEqual(nLon, 21, "subset nLon");
    Test.ensureEqual(grid.minData, -6.73213005, "subset minData");
    Test.ensureEqual(grid.maxData, 9.626250267, "subset maxData");
    Test.ensureEqual(grid.nValidPoints, Integer.MIN_VALUE, "subset nValid");
    Test.ensureEqual(grid.latSpacing, .25, "subset latSpacing");
    Test.ensureEqual(grid.lonSpacing, .25, "subset lonSpacing");
    Test.ensureEqual(grid.lon[0], -130, "subset lon[0]");
    Test.ensureEqual(grid.lon[nLon - 1], -125, "subset lon[nLon-1]");
    Test.ensureEqual(grid.lat[0], 40, "subset lat[0]");
    Test.ensureEqual(grid.lat[nLat - 1], 47, "subset lat[nLat-1]");
    Test.ensureEqual(grid.data[0], 3.3181900978, "subset data[0]"); // circular logic
    Test.ensureEqual(grid.data[1], 4.39888000488, "subset data[1]");
    Test.ensureEqual(grid.data[2], 4.78396987915, "subset data[2]");
    Test.ensureEqual(grid.data[nData - 2], 4.03859996795, "subset data[nData - 2]");
    Test.ensureEqual(grid.data[nData - 1], 3.95987010002, "subset data[nData - 1]");
    int lon130 = Math2.binaryFindClosest(grid.lon, -130);
    int lon125 = Math2.binaryFindClosest(grid.lon, -125);
    int lat40 = Math2.binaryFindClosest(grid.lat, 40);
    int lat47 = Math2.binaryFindClosest(grid.lat, 47);
    Test.ensureEqual(grid.lon[lon130], -130, "");
    Test.ensureEqual(grid.lon[lon125], -125, "");
    Test.ensureEqual(grid.lat[lat40], 40, "");
    Test.ensureEqual(grid.lat[lat47], 47, "");
    Test.ensureEqual(grid.getData(lon130, lat40), 3.318190097808838, "");
    Test.ensureEqual(grid.getData(lon125, lat40), 2.3082199096679688, "");
    Test.ensureEqual(grid.getData(lon130, lat47), 6.497610092163086, "");
    Test.ensureEqual(grid.getData(lon125, lat47), 3.9598701000213623, "");

    // circular logic test of calculateStats,
    // but note different values than for testGrd() above
    grid.calculateStats();
    Test.ensureEqual(grid.minData, -2.2247600555, "subset minData 2");
    Test.ensureEqual(grid.maxData, 7.656620025, "subset maxData 2");
    Test.ensureEqual(grid.nValidPoints, 609, "subset nValid 2");

    // test of nLon nLat
    grid.clear();
    grid.readGrd(testDir + testName + ".grd", -135, -120, 33, 47, 4, 3);
    nLat = grid.lat.length;
    nLon = grid.lon.length;
    nData = grid.data.length;
    Test.ensureEqual(nLon, 4, "subset nLon");
    Test.ensureEqual(nLat, 3, "subset nLat");
    Test.ensureEqual(grid.lonSpacing, 5, "subset lonSpacing");
    Test.ensureEqual(grid.latSpacing, 7, "subset latSpacing");
    Test.ensureEqual(grid.lon[0], -135, "subset lon[0]");
    Test.ensureEqual(grid.lon[nLon - 1], -120, "subset lon[nLon-1]");
    Test.ensureEqual(grid.lat[0], 33, "subset lat[0]");
    Test.ensureEqual(grid.lat[nLat - 1], 47, "subset lat[nLat-1]");
    lon130 = Math2.binaryFindClosest(grid.lon, -130);
    lon125 = Math2.binaryFindClosest(grid.lon, -125);
    lat40 = Math2.binaryFindClosest(grid.lat, 40);
    lat47 = Math2.binaryFindClosest(grid.lat, 47);
    Test.ensureEqual(grid.lon[lon130], -130, "");
    Test.ensureEqual(grid.lon[lon125], -125, "");
    Test.ensureEqual(grid.lat[lat40], 40, "");
    Test.ensureEqual(grid.lat[lat47], 47, "");
    Test.ensureEqual(grid.getData(lon130, lat40), 3.318190097808838, "");
    Test.ensureEqual(grid.getData(lon125, lat40), 2.3082199096679688, "");
    Test.ensureEqual(grid.getData(lon130, lat47), 6.497610092163086, "");
    Test.ensureEqual(grid.getData(lon125, lat47), 3.9598701000213623, "");

    // test of nLon nLat --same test, just shifted +360
    grid.clear();
    grid.readGrd(testDir + testName + ".grd", -135 + 360, -120 + 360, 33, 47, 4, 3);
    nLat = grid.lat.length;
    nLon = grid.lon.length;
    nData = grid.data.length;
    Test.ensureEqual(nLon, 4, "subset nLon");
    Test.ensureEqual(nLat, 3, "subset nLat");
    Test.ensureEqual(grid.lonSpacing, 5, "subset lonSpacing");
    Test.ensureEqual(grid.latSpacing, 7, "subset latSpacing");
    Test.ensureEqual(grid.lon[0], -135 + 360, "subset lon[0]");
    Test.ensureEqual(grid.lon[nLon - 1], -120 + 360, "subset lon[nLon-1]");
    Test.ensureEqual(grid.lat[0], 33, "subset lat[0]");
    Test.ensureEqual(grid.lat[nLat - 1], 47, "subset lat[nLat-1]");
    lon130 = Math2.binaryFindClosest(grid.lon, -130 + 360);
    lon125 = Math2.binaryFindClosest(grid.lon, -125 + 360);
    lat40 = Math2.binaryFindClosest(grid.lat, 40);
    lat47 = Math2.binaryFindClosest(grid.lat, 47);
    Test.ensureEqual(grid.lon[lon130], -130 + 360, "");
    Test.ensureEqual(grid.lon[lon125], -125 + 360, "");
    Test.ensureEqual(grid.lat[lat40], 40, "");
    Test.ensureEqual(grid.lat[lat47], 47, "");
    Test.ensureEqual(grid.getData(lon130, lat40), 3.318190097808838, "");
    Test.ensureEqual(grid.getData(lon125, lat40), 2.3082199096679688, "");
    Test.ensureEqual(grid.getData(lon130, lat47), 6.497610092163086, "");
    Test.ensureEqual(grid.getData(lon125, lat47), 3.9598701000213623, "");

    // **********************************************
    // tests of
    String tName = "TestReadGrgTMBchla_x120_X320_y-45_Y65_nx201_ny111.grd";
    // because it covers an odd range:
    // actual file minX=120.0 maxX=320.0 minY=-45.0 maxY=65.0 xInc=1 yInc=1
    // These mimic tests in GridDataSetThredds.test.

    // get 4 points individually
    grid.readGrd(testDir + tName, 315, 315, 30, 30, 1, 1);
    Test.ensureEqual(grid.lon, new double[] {315}, "");
    Test.ensureEqual(grid.lat, new double[] {30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.039, "");

    grid.readGrd(testDir + tName, 135, 135, 30, 30, 1, 1);
    Test.ensureEqual(grid.lon, new double[] {135}, "");
    Test.ensureEqual(grid.lat, new double[] {30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.200333, "");

    grid.readGrd(testDir + tName, 315, 315, -41, -41, 1, 1);
    Test.ensureEqual(grid.lon, new double[] {315}, "");
    Test.ensureEqual(grid.lat, new double[] {-41}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.968, "");

    grid.readGrd(testDir + tName, 135, 135, -41, -41, 1, 1);
    Test.ensureEqual(grid.lon, new double[] {135}, "");
    Test.ensureEqual(grid.lat, new double[] {-41}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.431, "");

    // test 2 wide, 1 high
    grid.readGrd(testDir + tName, 135, 315, 30, 30, 2, 1);
    Test.ensureEqual(grid.lon, new double[] {135, 315}, "");
    Test.ensureEqual(grid.lat, new double[] {30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.200333, "");
    Test.ensureEqual((float) grid.data[1], (float) 0.039, "");

    // test 37 wide, 1 high test most of the data rotated around to right
    // This is a little contrived: getting every 5 degrees of lon allows it to
    // cleanly align at 0 and still get my two test points exactly.
    // But that is part of the nature of having to move the data columns around.
    grid.readGrd(testDir + tName, -45, 135, 30, 30, 37, 1); // 180 range / 5 = 36 + 1
    String2.log("lon=" + String2.toCSSVString(grid.lon));
    Test.ensureEqual(grid.lon, DataHelper.getRegularArray(37, -45, 5), "");
    Test.ensureEqual(grid.lat, new double[] {30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.039, "");
    Test.ensureEqual((float) grid.data[36], (float) 0.200333, "");

    // test 1 wide, 2 high x in 0.. 180
    grid.readGrd(testDir + tName, 135, 135, -41, 30, 1, 2);
    Test.ensureEqual(grid.lon, new double[] {135}, "");
    Test.ensureEqual(grid.lat, new double[] {-41, 30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.431, "");
    Test.ensureEqual((float) grid.data[1], (float) 0.200333, "");

    // test 1 wide, 2 high in x>180
    grid.readGrd(testDir + tName, 315, 315, -41, 30, 1, 2);
    Test.ensureEqual(grid.lon, new double[] {315}, "");
    Test.ensureEqual(grid.lat, new double[] {-41, 30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.968, "");
    Test.ensureEqual((float) grid.data[1], (float) 0.039, "");

    // test 1 wide, 2 high in x<0
    grid.readGrd(testDir + tName, 315 - 360, 315 - 360, -41, 30, 1, 2);
    Test.ensureEqual(grid.lon, new double[] {315 - 360}, "");
    Test.ensureEqual(grid.lat, new double[] {-41, 30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.968, "");
    Test.ensureEqual((float) grid.data[1], (float) 0.039, "");

    // test 2 wide, 2 high in x<0
    grid.readGrd(testDir + tName, 135, 315, -41, 30, 2, 2);
    Test.ensureEqual(grid.lon, new double[] {135, 315}, "");
    Test.ensureEqual(grid.lat, new double[] {-41, 30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.431, "");
    Test.ensureEqual((float) grid.data[1], (float) 0.200333, "");
    Test.ensureEqual((float) grid.data[2], (float) 0.968, "");
    Test.ensureEqual((float) grid.data[3], (float) 0.039, "");

    // test 37 wide, 2 high in x<0
    grid.readGrd(testDir + tName, 315 - 360, 135, -41, 30, 37, 2);
    Test.ensureEqual(grid.lon, DataHelper.getRegularArray(37, -45, 5), "");
    Test.ensureEqual(grid.lat, new double[] {-41, 30}, "");
    Test.ensureEqual((float) grid.data[0], (float) 0.968, "");
    Test.ensureEqual((float) grid.data[1], (float) 0.039, "");
    Test.ensureEqual((float) grid.data[72], (float) 0.431, "");
    Test.ensureEqual((float) grid.data[73], (float) 0.200333, "");

    String2.log(
        "testReadGrdSubset finished successfully TIME="
            + (System.currentTimeMillis() - time)
            + "\n");
  }

  /**
   * This tests readGrid and saveAsGrd.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testGrd() throws Exception {
    String2.log("\n***** Grid.testGrd");
    Grid.verbose = true;

    // This .grd file was originally created by saveAsGrd, so circular logic
    // But manual test showed I could read into GMT.
    String grdDump =
        // "netcdf " + testName +
        ".grd {\n"
            + "  dimensions:\n"
            + "    side = 2;\n"
            + "    xysize = 13673;\n"
            + "  variables:\n"
            + "    double x_range(side=2);\n"
            + "      :units = \"user_x_unit\";\n"
            + "\n"
            + "    double y_range(side=2);\n"
            + "      :units = \"user_y_unit\";\n"
            + "\n"
            + "    double z_range(side=2);\n"
            + "      :units = \"user_z_unit\";\n"
            + "\n"
            + "    double spacing(side=2);\n"
            + "\n"
            + "    int dimension(side=2);\n"
            + "\n"
            + "    float z(xysize=13673);\n"
            + "      :scale_factor = 1.0; // double\n"
            + "      :add_offset = 0.0; // double\n"
            + "      :node_offset = 0; // int\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :title = \"\";\n"
            + "  :source = \"CoastWatch West Coast Node\";\n"
            + "}\n";

    // input file is as expected (independent test)
    Test.ensureEqual(
        NcHelper.ncdump(testDir + testName + ".grd", "-h"),
        "netcdf " + testName + grdDump,
        "ncdump of " + testDir + testName + ".grd");
    Test.ensureEqual(
        File2.length(testDir + testName + ".grd"),
        55320,
        "length of " + testDir + testName + ".grd");

    // test read Grd
    long time = System.currentTimeMillis();
    Grid grid = new Grid();
    int nLat, nLon, nData;
    double NaN = Double.NaN;

    grid.readGrd(testDir + testName + ".grd", true);
    nLat = grid.lat.length;
    nLon = grid.lon.length;
    nData = grid.data.length;
    Test.ensureEqual(nLat, 113, "read nLat");
    Test.ensureEqual(nLon, 121, "read nLon");
    Test.ensureEqual(grid.minData, -6.73213005, "read minData");
    Test.ensureEqual(grid.maxData, 9.626250267, "read maxData");
    Test.ensureEqual(grid.latSpacing, .25, "read latSpacing");
    Test.ensureEqual(grid.lonSpacing, .25, "read lonSpacing");
    Test.ensureEqual(grid.lon[0], -135, "read lon[0]");
    Test.ensureEqual(grid.lon[nLon - 1], -105, "read lon[nLon-1]");
    Test.ensureEqual(grid.lat[0], 22, "read lat[0]");
    Test.ensureEqual(grid.lat[nLat - 1], 50, "read lat[nLat-1]");
    Test.ensureEqual(grid.data[0], -5.724239826, "read data[0]");
    Test.ensureEqual(grid.data[1], -5.685810089, "read data[1]");
    Test.ensureEqual(grid.data[2], -5.750430107, "read data[2]");
    Test.ensureEqual(grid.data[nData - 2], NaN, "read data[nData - 2]");
    Test.ensureEqual(grid.data[nData - 1], NaN, "read data[nData - 1]");

    // circular logic test of calculateStats,
    // but note different values than for testReadGrdSubset() below
    grid.calculateStats();
    Test.ensureEqual(grid.minData, -6.73213005065918, "subset minData 2");
    Test.ensureEqual(grid.maxData, 9.626250267028809, "subset maxData 2");
    Test.ensureEqual(grid.nValidPoints, 6043, "subset nValid 2");

    time = System.currentTimeMillis() - time;
    Math2.gcAndWait("Grid (between tests)"); // before get memoryString() in a test
    String2.log("Grid.testGrd time=" + time + " " + Math2.memoryString());

    // test saveAsGrd;
    time = System.currentTimeMillis();
    grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    grid.saveAsGrd(testDir, "temp");
    Test.ensureEqual(
        NcHelper.ncdump(testDir + "temp.grd", "-h"),
        "netcdf temp" + grdDump,
        "ncdump of " + testDir + "temp.grd");
    Test.ensureEqual(
        File2.length(testDir + "temp.grd"), 55320, "length of " + testDir + "temp.grd");
    File2.delete(testDir + "temp.grd");

    // ******************** test of GMT 4 file
    String dir4 = GridTests.class.getResource("/largeFiles/gmt/").getPath();
    String name4 = "TestGMT4";
    grdDump =
        "netcdf TestGMT4.grd {\n"
            + "  dimensions:\n"
            + "    x = 4001;\n"
            + // (has coord.var)\n" + //changed when switched to netcdf-java 4.0, 2009-02-23
            "    y = 2321;\n"
            + // (has coord.var)\n" +
            "  variables:\n"
            + "    double x(x=4001);\n"
            + "      :long_name = \"x\";\n"
            + "      :actual_range = 205.0, 255.0; // double\n"
            + "\n"
            + "    float y(y=2321);\n"
            + "      :long_name = \"y\";\n"
            + "      :actual_range = 22.0, 51.0; // double\n"
            + "\n"
            + "    float z(y=2321, x=4001);\n"
            + "      :long_name = \"z\";\n"
            + "      :_FillValue = NaNf; // float\n"
            + "      :actual_range = 0.0010000000474974513, 183.41700744628906; // double\n"
            + "      :coordinates = \"x y\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :Conventions = \"COARDS, CF-1.0\";\n"
            + "  :title = \"/u00/modisgf/data/2007/1day/MW2007339_2007339_chla.grd\";\n"
            + "  :history = \"nearneighbor -V -R205/255/22/51 -I0.0125/0.0125 -S2k -G/u00/modisgf/data/2007/1day/MW2007339_2007339_chla.grd -N1\";\n"
            + "  :GMT_version = \"4.2.1\";\n"
            + "  :node_offset = 0; // int\n"
            + "}\n";
    // input file is as expected (independent test)
    String s = NcHelper.ncdump(dir4 + name4 + ".grd", "-h");
    Test.ensureEqual(s, grdDump, "ncdump of " + testDir + testName + ".grd\n" + s);

    // test read Grd
    time = System.currentTimeMillis();
    grid = new Grid();
    grid.readGrd(dir4 + name4 + ".grd", true);
    nLat = grid.lat.length;
    nLon = grid.lon.length;
    nData = grid.data.length;
    String2.log(
        nLat
            + " "
            + nLon
            + " "
            + grid.minData
            + " "
            + grid.maxData
            + " "
            + grid.latSpacing
            + " "
            + grid.lonSpacing
            + "\n"
            + grid.lon[0]
            + " "
            + grid.lon[nLon - 1]
            + " "
            + grid.lat[0]
            + " "
            + grid.lat[nLat - 1]
            + "\n"
            + grid.data[0]
            + " "
            + grid.data[1]
            + " "
            + grid.data[2]
            + " "
            + grid.data[nData - 2]
            + " "
            + grid.data[nData - 1]);
    // int nFound = 0;
    // for (int i = 0; i < nData; i++) {
    // if (!Double.isNaN(grid.data[i])) {
    // String2.log(i + "=" + grid.data[i]);
    // nFound++;
    // if (nFound == 5) break;
    // }
    // }

    Test.ensureEqual(nLat, 2321, "read nLat");
    Test.ensureEqual(nLon, 4001, "read nLon");
    Test.ensureEqual(grid.minData, 0.001f, "read minData");
    Test.ensureEqual(grid.maxData, 183.417f, "read maxData");
    Test.ensureEqual(grid.latSpacing, .0125, "read latSpacing");
    Test.ensureEqual(grid.lonSpacing, .0125, "read lonSpacing");
    Test.ensureEqual(grid.lon[0], -155, "read lon[0]");
    Test.ensureEqual(grid.lon[nLon - 1], -105, "read lon[nLon-1]");
    Test.ensureEqual(grid.lat[0], 22, "read lat[0]");
    Test.ensureEqual(grid.lat[nLat - 1], 51, "read lat[nLat-1]");

    Test.ensureEqual(grid.data[0], NaN, "read data");
    Test.ensureEqual(grid.data[1343], 0.195f, "read data");
    Test.ensureEqual(grid.data[1344], 0.182f, "read data");
    Test.ensureEqual(grid.data[1345], 0.182f, "read data");
    Test.ensureEqual(grid.data[1348], 0.167f, "read data");
  }

  /**
   * This tests some of the lon pm180 code.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testPM180() throws Exception {
    String2.log("\ntestPM180...");
    String tName = "testPM180";
    FloatArray fa = new FloatArray();

    // ******make limited range 0..360 grid (already compliant)
    Grid grid = new Grid();
    int nLon = 3;
    int nLat = 2;
    grid.lonSpacing = 10;
    grid.latSpacing = 1;
    grid.data = new double[nLon * nLat];
    grid.lon = new double[] {10, 20, 30};
    grid.lat = new double[] {1, 2};
    // data is column-by-column from lower left
    grid.data = new double[] {1, 2, 10, 20, 300, 400};

    // make it +-180
    grid.makeLonPM180(true);
    Test.ensureEqual(grid.lon, new double[] {10, 20, 30}, "");
    Test.ensureEqual(grid.lat, new double[] {1, 2}, "");
    Test.ensureEqual(grid.data, new double[] {1, 2, 10, 20, 300, 400}, "");

    // make it 0..360
    grid.makeLonPM180(false);
    Test.ensureEqual(grid.lon, new double[] {10, 20, 30}, "");
    Test.ensureEqual(grid.lat, new double[] {1, 2}, "");
    Test.ensureEqual(grid.data, new double[] {1, 2, 10, 20, 300, 400}, "");

    // ******make limited range 0..360 grid (just lon values need to be changed)
    grid = new Grid();
    nLon = 3;
    nLat = 2;
    grid.lonSpacing = 10;
    grid.latSpacing = 1;
    grid.data = new double[nLon * nLat];
    grid.lon = new double[] {190, 200, 210};
    grid.lat = new double[] {1, 2};
    // data is column-by-column from lower left
    grid.data = new double[] {1, 2, 10, 20, 300, 400};

    // make it +-180
    grid.makeLonPM180(true);
    Test.ensureEqual(grid.lon, new double[] {-170, -160, -150}, "");
    Test.ensureEqual(grid.lat, new double[] {1, 2}, "");
    Test.ensureEqual(grid.data, new double[] {1, 2, 10, 20, 300, 400}, "");

    // make it 0..360
    grid.makeLonPM180(false);
    Test.ensureEqual(grid.lon, new double[] {190, 200, 210}, "");
    Test.ensureEqual(grid.lat, new double[] {1, 2}, "");
    Test.ensureEqual(grid.data, new double[] {1, 2, 10, 20, 300, 400}, "");

    // ******make full range 0..360 grid
    grid = new Grid();
    nLon = 360;
    nLat = 2;
    grid.lonSpacing = 1;
    grid.latSpacing = 1;
    grid.lon = DataHelper.getRegularArray(360, 0, 1);
    grid.lat = new double[] {0, 1};
    // data is column-by-column from lower left
    grid.data = new double[720];
    for (int i = 0; i < 360; i++) {
      grid.data[2 * i] = i;
      grid.data[2 * i + 1] = i;
    }
    // created as expected?
    Test.ensureEqual(grid.lon[0], 0, "");
    Test.ensureEqual(grid.lon[1], 1, "");
    Test.ensureEqual(grid.lon[179], 179, "");
    Test.ensureEqual(grid.lon[180], 180, "");
    Test.ensureEqual(grid.lon[359], 359, "");

    Test.ensureEqual(grid.data[0], 0, "");
    Test.ensureEqual(grid.data[1], 0, "");
    Test.ensureEqual(grid.data[2], 1, "");
    Test.ensureEqual(grid.data[3], 1, "");
    Test.ensureEqual(grid.data[358], 179, "");
    Test.ensureEqual(grid.data[359], 179, "");
    Test.ensureEqual(grid.data[360], 180, "");
    Test.ensureEqual(grid.data[361], 180, "");
    Test.ensureEqual(grid.data[718], 359, "");
    Test.ensureEqual(grid.data[719], 359, "");

    // make it +-180
    grid.makeLonPM180(true);
    Test.ensureEqual(grid.lon[0], -180, "");
    Test.ensureEqual(grid.lon[1], -179, "");
    Test.ensureEqual(grid.lon[179], -1, "");
    Test.ensureEqual(grid.lon[180], 0, "");
    Test.ensureEqual(grid.lon[359], 179, "");

    Test.ensureEqual(grid.data[0], 180, "");
    Test.ensureEqual(grid.data[1], 180, "");
    Test.ensureEqual(grid.data[2], 181, "");
    Test.ensureEqual(grid.data[3], 181, "");
    Test.ensureEqual(grid.data[358], 359, "");
    Test.ensureEqual(grid.data[359], 359, "");
    Test.ensureEqual(grid.data[360], 0, "");
    Test.ensureEqual(grid.data[361], 0, "");
    Test.ensureEqual(grid.data[718], 179, "");
    Test.ensureEqual(grid.data[719], 179, "");

    // make it 0..360
    grid.makeLonPM180(false);
    Test.ensureEqual(grid.lon[0], 0, "");
    Test.ensureEqual(grid.lon[1], 1, "");
    Test.ensureEqual(grid.lon[179], 179, "");
    Test.ensureEqual(grid.lon[180], 180, "");
    Test.ensureEqual(grid.lon[359], 359, "");

    Test.ensureEqual(grid.data[0], 0, "");
    Test.ensureEqual(grid.data[1], 0, "");
    Test.ensureEqual(grid.data[2], 1, "");
    Test.ensureEqual(grid.data[3], 1, "");
    Test.ensureEqual(grid.data[358], 179, "");
    Test.ensureEqual(grid.data[359], 179, "");
    Test.ensureEqual(grid.data[360], 180, "");
    Test.ensureEqual(grid.data[361], 180, "");
    Test.ensureEqual(grid.data[718], 359, "");
    Test.ensureEqual(grid.data[719], 359, "");

    // ******make world-wide tiny 170.5..190.5 grid
    // draw this to envision it
    grid = new Grid();
    nLon = 3;
    nLat = 2;
    grid.lonSpacing = 10;
    grid.latSpacing = 1;
    grid.data = new double[nLon * nLat];
    grid.lon = new double[] {170.5, 180.5, 190.5};
    grid.lat = new double[] {1, 2};
    // data is column-by-column from lower left
    grid.data = new double[] {1, 2, 10, 20, 300, 400};

    // make it +-180
    grid.makeLonPM180(true);
    Test.ensureEqual(grid.lon, DataHelper.getRegularArray(36, -179.5, 10), "");
    Test.ensureEqual(grid.lat, new double[] {1, 2}, "");
    Test.ensureEqual(grid.data[0], 10, "");
    Test.ensureEqual(grid.data[1], 20, "");
    Test.ensureEqual(grid.data[2], 300, "");
    Test.ensureEqual(grid.data[3], 400, "");
    Test.ensureEqual(grid.data[4], Double.NaN, "");
    // big gap
    Test.ensureEqual(grid.data[69], Double.NaN, "");
    Test.ensureEqual(grid.data[70], 1, "");
    Test.ensureEqual(grid.data[71], 2, "");

    // ******make world-wide tiny -9.5..10.5 grid
    // draw this to envision it
    grid = new Grid();
    nLon = 3;
    nLat = 2;
    grid.lonSpacing = 10;
    grid.latSpacing = 1;
    grid.data = new double[nLon * nLat];
    grid.lon = new double[] {-9.5, .5, 10.5};
    grid.lat = new double[] {1, 2};
    // data is column-by-column from lower left
    grid.data = new double[] {1, 2, 10, 20, 300, 400};

    // make it 0..360
    grid.makeLonPM180(false);
    Test.ensureEqual(grid.lon, DataHelper.getRegularArray(36, 0.5, 10), "");
    Test.ensureEqual(grid.lat, new double[] {1, 2}, "");
    Test.ensureEqual(grid.data[0], 10, "");
    Test.ensureEqual(grid.data[1], 20, "");
    Test.ensureEqual(grid.data[2], 300, "");
    Test.ensureEqual(grid.data[3], 400, "");
    Test.ensureEqual(grid.data[4], Double.NaN, "");
    // big gap
    Test.ensureEqual(grid.data[69], Double.NaN, "");
    Test.ensureEqual(grid.data[70], 1, "");
    Test.ensureEqual(grid.data[71], 2, "");

    // ******make a 0..360 grid
    grid = new Grid();
    nLon = 361; // with duplicate column at end. It will be removed if read all.
    nLat = 180;
    grid.lonSpacing = 1;
    grid.latSpacing = 1;
    grid.data = new double[nLon * nLat];
    grid.lon = DataHelper.getRegularArray(nLon, 0, 1);
    grid.lat = DataHelper.getRegularArray(nLat, -90, 1);
    for (int tLon = 0; tLon < nLon; tLon++)
      for (int tLat = 0; tLat < nLat; tLat++)
        grid.setData(tLon, tLat, grid.lon[tLon] + (grid.lat[tLat] / 1000.0));
    grid.lonAttributes().set("units", "degrees_east");
    grid.latAttributes().set("units", "degrees_north");
    grid.globalAttributes().set("time_coverage_start", "2006-01-02T00:00:00");
    grid.globalAttributes().set("time_coverage_end", "2006-01-03T00:00:00");

    // save as grd
    grid.saveAsGrd(testDir, tName);

    // readGrd tricky subset in pm180 units
    Grid grid2 = new Grid();
    grid2.readGrd(testDir + tName + ".grd", -1, 2, 5, 6, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid2.lon, new double[] {-1, 0, 1, 2}, "");
    Test.ensureEqual(grid2.lat, new double[] {5, 6}, "");
    fa.clear();
    fa.append(new DoubleArray(grid2.data));
    Test.ensureEqual(
        fa.toArray(),
        new float[] {359.005f, 359.006f, 0.005f, 0.006f, 1.005f, 1.006f, 2.005f, 2.006f},
        "");
    File2.delete(testDir + tName + ".grd");

    // *save as netcdf
    grid.saveAsNetCDF(testDir, tName, "data");

    // readNetcdf tricky subset in pm180 units
    grid2 = new Grid();
    grid2.readNetCDF(
        testDir + tName + ".nc", "data", -1, 2, 5, 6, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid2.lon, new double[] {-1, 0, 1, 2}, "");
    Test.ensureEqual(grid2.lat, new double[] {5, 6}, "");
    fa.clear();
    fa.append(new DoubleArray(grid2.data));
    Test.ensureEqual(
        fa.toArray(),
        new float[] {359.005f, 359.006f, 0.005f, 0.006f, 1.005f, 1.006f, 2.005f, 2.006f},
        "");

    // read whole grd as pm180, is last lon removed?
    grid2 = new Grid();
    grid2.readNetCDF(
        testDir + tName + ".nc", "data", -180, 180, -90, 90, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid2.lon.length, 360, ""); // not 361 because last one remove
    Test.ensureEqual(grid2.lat.length, 180, ""); // intact
    Test.ensureEqual(grid2.lon[0], -180, "");
    Test.ensureEqual(grid2.lon[grid2.lon.length - 1], 179, "");
    Test.ensureEqual(grid2.getData(0, 90), 180, "");
    Test.ensureEqual(grid2.getData(359, 90), 179, "");

    File2.delete(testDir + tName + ".nc");

    // ****** make a pm180 grid
    grid.lon = DataHelper.getRegularArray(nLon, -180, 1); // with -180 and 180 (excess)
    for (int tLon = 0; tLon < nLon; tLon++)
      for (int tLat = 0; tLat < nLat; tLat++)
        grid.setData(tLon, tLat, grid.lon[tLon] + (grid.lat[tLat] / 1000.0));

    // save as grd
    grid.saveAsGrd(testDir, tName);

    // readGrd tricky subset in 0..360 units
    grid2 = new Grid();
    grid2.readGrd(testDir + tName + ".grd", 179, 182, 5, 6, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid2.lon, new double[] {179, 180, 181, 182}, "");
    Test.ensureEqual(grid2.lat, new double[] {5, 6}, "");
    fa.clear();
    fa.append(new DoubleArray(grid2.data));
    Test.ensureEqual(
        fa.toArray(),
        new float[] {
          // values are encoding: lon + lat/1000
          // also ok -179.995f, -179.994f since orig array had -180 and 180
          179.005f, 179.006f, 180.005f, 180.006f, -178.995f, -178.994f, -177.995f, -177.994f
        },
        "");
    File2.delete(testDir + tName + ".grd");

    // save as netcdf
    grid.saveAsNetCDF(testDir, tName, "data");

    // readNetcdf tricky subset in 0..360 units
    grid2 = new Grid();
    grid2.readNetCDF(
        testDir + tName + ".nc", "data", 179, 182, 5, 6, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid2.lon, new double[] {179, 180, 181, 182}, "");
    Test.ensureEqual(grid2.lat, new double[] {5, 6}, "");
    fa.clear();
    fa.append(new DoubleArray(grid2.data));
    Test.ensureEqual(
        fa.toArray(),
        new float[] {
          // values are encoding: lon + lat/1000
          // also ok -179.995f, -179.994f since orig array had -180 and 180
          179.005f, 179.006f, 180.005f, 180.006f, -178.995f, -178.994f, -177.995f, -177.994f
        },
        "");

    // read whole grd as 0..360, is last lon removed?
    grid2 = new Grid();
    grid2.readNetCDF(
        testDir + tName + ".nc", "data", 0, 360, -90, 90, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid2.lon.length, 360, ""); // not 361 because last one remove
    Test.ensureEqual(grid2.lat.length, 180, ""); // intact
    Test.ensureEqual(grid2.lon[0], 0, "");
    Test.ensureEqual(grid2.lon[grid2.lon.length - 1], 359, "");
    Test.ensureEqual(grid2.getData(0, 90), 0, "");
    Test.ensureEqual(grid2.getData(359, 90), -1, "");

    File2.delete(testDir + tName + ".nc");

    // good test, but disabled while Grid makeLonPM180 requires evenly spaced on
    // values
    // test odd spacing (e.g., .7)
    String2.log("\n***test odd spacing   (.7)");
    grid = new Grid();
    nLon = Math2.roundToInt(360 / .7) + 1; // with duplicate column at end.
    nLat = 2;
    grid.lonSpacing = .7;
    grid.latSpacing = 1;
    grid.data = new double[nLon * nLat];
    grid.lon = DataHelper.getRegularArray(nLon, 0, .7);
    grid.lat = DataHelper.getRegularArray(nLat, 0, 1);
    for (int tLon = 0; tLon < nLon; tLon++)
      for (int tLat = 0; tLat < nLat; tLat++)
        grid.setData(tLon, tLat, grid.lon[tLon] * 10); // so can see which lon they came from
    grid.makeLonPM180(true);
    nLon = grid.lon.length;
    for (int tLon = 0; tLon < nLon; tLon++)
      if (grid.lon[tLon] > -2 && grid.lon[tLon] < 2)
        String2.log(
            "lon["
                + tLon
                + "]="
                + String2.genEFormat10(grid.lon[tLon])
                + " data="
                + (float) grid.getData(tLon, 0));
    Test.ensureEqual(grid.lon[254], -1.6, "");
    Test.ensureEqual(grid.getData(254, 0), 3584, "");
    Test.ensureEqual(grid.lon[255], -0.9, "");
    Test.ensureEqual(grid.getData(255, 0), 3591, "");
    Test.ensureEqual(grid.lon[256], -0.2, "");
    Test.ensureEqual(grid.getData(256, 0), 3598, "");
    // note discontinuity of lon values
    Test.ensureEqual(grid.lon[257], 0.0, "");
    Test.ensureEqual(grid.getData(257, 0), 0, "");
    Test.ensureEqual(grid.lon[258], 0.7, "");
    Test.ensureEqual(grid.getData(258, 0), 7, "");
    Test.ensureEqual(grid.lon[259], 1.4, "");
    Test.ensureEqual(grid.getData(259, 0), 14, "");
  }
}
