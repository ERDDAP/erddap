package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.hdf.HdfConstants;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import tags.TagMissingFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

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
    else Math2.gc("Grid (between tests)", 5000); // in a test, a pause after message displayed
  }

  /** This tests the 'subtract' method. */
  @org.junit.jupiter.api.Test
  void testSubtract() {
    String2.log("Grid.testSubtract");
    double mv = Double.NaN;

    Grid grid1 = new Grid();
    grid1.lat = new double[] {0, 1, 2, 3, 4};
    grid1.lon = new double[] {10, 12, 14, 16};
    grid1.latSpacing = 1;
    grid1.lonSpacing = 2;
    // A 1D array, column by column, from the lower left (the way SGT wants it).
    grid1.data =
        new double[] {
          0, 10, 20, 30, 40, // a column's data
          100, mv, 120, 130, 140,
          200, 210, 220, 230, 240,
          300, 310, 320, 330, 340
        };

    // make grid 2 cover a smaller area, and closest values are .1*grid1 values
    Grid grid2 = new Grid();
    grid2.lat = new double[] {1.1, 2.1, 3.1}; // match 1, 2, 3
    grid2.lon = new double[] {12.1, 14.1}; // mateh 12, 14
    grid2.latSpacing = 1;
    grid2.lonSpacing = 2;
    // A 1D array, column by column, from the lower left (the way SGT wants it).
    grid2.data =
        new double[] {
          11, 12, 13, // a column's data
          21, mv, 23
        };

    // subtract grid1 from grid2
    grid1.subtract(grid2);
    Test.ensureEqual(
        grid1.data,
        new double[] {
          mv, mv, mv, mv, mv, // a column's data
          mv, mv, 120 - 12, 130 - 13, mv, mv, 210 - 21, mv, 230 - 23, mv, mv, mv, mv, mv, mv
        },
        "result not as expected: ");
  }

  /** This tests the 'interpolate' method. */
  @org.junit.jupiter.api.Test
  void testInterpolate() {
    String2.log("Grid.testInterpolate");
    double mv = Double.NaN;

    double lat[] = {0, 1, 2, 3, 4};
    double lon[] = {0};
    double latSpacing = 1;
    double lonSpacing = 2;
    double data1[] = {0, 10, mv, 30, 40};
    double data2[] = {0, mv, 20, 40, 80};

    Grid grid1 = new Grid();
    grid1.lat = lat;
    grid1.lon = lon;
    grid1.latSpacing = latSpacing;
    grid1.lonSpacing = lonSpacing;
    grid1.data = data1;

    Grid grid2 = new Grid();
    grid2.lat = lat;
    grid2.lon = lon;
    grid2.latSpacing = latSpacing;
    grid2.lonSpacing = lonSpacing;
    grid2.data = data2;

    grid1.data = data1;
    grid1.interpolate(grid2, 0);
    Test.ensureEqual(grid1.data, new double[] {0, 10, 20, 30, 40}, "");

    grid1.data = data1;
    grid1.interpolate(grid2, .75);
    Test.ensureEqual(grid1.data, new double[] {0, 10, 20, 37.5, 70}, "");

    grid1.data = data1;
    grid1.interpolate(grid2, 1);
    Test.ensureEqual(grid1.data, new double[] {0, 10, 20, 40, 80}, "");
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
   * This tests GridSaveAs.main and the local method saveAs.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAs() throws Exception {

    String2.log("\n*** Grid.testSaveAs");
    String errorIn = String2.ERROR + " in Grid.testSaveAs: ";
    File2.verbose = true;
    String cwName = testName;

    // read a .grd file (for reference)
    Grid grid1 = new Grid();
    grid1.readGrd(testDir + cwName + ".grd", false); // false=not pm180

    // ************* create the write-only file types
    // make a Dave-style .grd file from the cwName'd file
    FileNameUtility fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods");
    String daveName = fnu.convertCWBrowserNameToDaveName(cwName);

    // make sure the files don't exist
    File2.delete(testDir + daveName + ".asc");
    File2.delete(testDir + daveName + ".grd");
    File2.delete(testDir + daveName + ".hdf");
    File2.delete(testDir + daveName + ".mat");
    File2.delete(testDir + daveName + ".nc");
    File2.delete(testDir + daveName + ".xyz");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".asc"), ".asc");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".grd"), ".grd");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".hdf"), ".hdf");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".mat"), ".mat");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".nc"), ".nc");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".xyz"), ".xyz");

    // starting point: make a daveName.grd file by copying from cwName.grd file
    Test.ensureTrue(
        File2.copy(testDir + cwName + ".grd", testDir + daveName + ".grd"),
        errorIn + "copy " + testDir + cwName + ".grd.");

    // test GridSaveAs.main: make the .asc file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".asc"});

    // test GridSaveAs.main: make the .mat file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".mat"});

    // test GridSaveAs.main: make the .xyz file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".xyz"});

    // ensure asc was saved as pm180
    if (true) { // so sar is garbage collected
      String sar[] = File2.readFromFile88591(testDir + daveName + ".asc");
      Test.ensureEqual(sar[0], "", "");
      // String2.log(sar[1].substring(1, 200));
      String t =
          "ncols 121\n"
              + "nrows 113\n"
              + "xllcenter -135.0\n"
              + "yllcenter 22.0\n"
              + "cellsize 0.25\n"
              + "nodata_value -9999999\n"
              + "4.2154 4.20402 4.39077";
      Test.ensureEqual(sar[1].substring(0, t.length()), t, "");
    }

    // ************ Test .grd to .nc to .grd
    // make a Dave-style .grd file from the cwName'd file
    Test.ensureTrue(
        File2.copy(testDir + cwName + ".grd", testDir + daveName + ".grd"),
        errorIn + "copy " + testDir + cwName + ".grd.");

    // test GridSaveAs.main: make the .nc file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".nc"});

    // delete the Dave-style grd file
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".grd"),
        errorIn + " while deleting " + testDir + daveName + ".grd");

    // read .nc file
    Grid grid2 = new Grid();
    grid2.readNetCDF(
        testDir + daveName + ".nc", FileNameUtility.get6CharName(cwName), false); // false=not pm180

    // are they the same?
    Test.ensureTrue(grid1.equals(grid2), errorIn);

    // test GridSaveAs.main: make a .grd file from the .nc file
    GridSaveAs.main(new String[] {testDir + daveName + ".nc", testDir + daveName + ".grd"});

    // read .grd file
    Grid grid3 = new Grid();
    grid3.readGrd(testDir + daveName + ".grd", false); // false=not pm180

    // are they the same?
    Test.ensureTrue(grid1.equals(grid3), errorIn);

    // delete the .grd file
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".grd"),
        errorIn + " while deleting " + testDir + daveName + ".grd");

    // ******************* Test .grd to .hdf to .grd
    // make a Dave-style .grd file from the cwName'd file
    Test.ensureTrue(
        File2.copy(testDir + cwName + ".grd", testDir + daveName + ".grd"),
        errorIn + "copy " + testDir + cwName + ".grd.");

    // test GridSaveAs.main: make the .hdf file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".hdf"});

    // delete the Dave-style grd file
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".grd"),
        errorIn + " while deleting " + testDir + daveName + ".grd");

    // read .hdf file
    grid2 = new Grid();
    grid2.readHDF(testDir + daveName + ".hdf", HdfConstants.DFNT_FLOAT64); // I write FLOAT64 files
    grid2.makeLonPM180(false);

    // are they the same?
    Test.ensureTrue(grid1.equals(grid2), errorIn);

    // test GridSaveAs.main: make a .grd file from the .hdf file
    GridSaveAs.main(new String[] {testDir + daveName + ".hdf", testDir + daveName + ".grd"});

    // delete the .hdf file
    // Test.ensureTrue(File2.delete(testDir + daveName + ".hdf"),
    // errorIn + " while deleting " + testDir + daveName + ".hdf");

    // read .grd file
    grid3 = new Grid();
    grid3.readGrd(testDir + daveName + ".grd", false); // false=not pm180

    // are they the same?
    Test.ensureTrue(grid1.equals(grid3), errorIn);

    // that leaves a created .grd file extant

    // ***test whole directory .grd to .hdf.zip
    // make sure the files don't exist
    File2.delete(testDir + daveName + ".hdf");
    File2.delete(testDir + daveName + ".hdf.zip");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".hdf"), ".hdf");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".hdf.zip"), ".hdf.zip");

    // test GridSaveAs.main: make the .hdf file from the .grd file
    GridSaveAs.main(
        new String[] {
          testDir + ".grd", // no "daveName+" because doing whole directory
          testDir + ".hdf.zip"
        }); // no "daveName+" because doing whole directory

    // ensure that the temp .hdf (without the .zip) was deleted
    Test.ensureEqual(
        File2.isFile(testDir + daveName + ".hdf"),
        false,
        ".hdf (no .zip) not deleted: " + testDir + daveName + ".hdf");

    // unzip the .hdf file
    SSR.unzipRename(testDir, daveName + ".hdf.zip", testDir, daveName + ".hdf", 10);

    // read the hdf file
    Grid grid4 = new Grid();
    grid4.readHDF(testDir + daveName + ".hdf", HdfConstants.DFNT_FLOAT64);
    grid4.makeLonPM180(false);

    // are they the same?
    Test.ensureTrue(grid4.equals(grid3), errorIn);

    // ***test whole directory .grd to .hdf
    // make sure the files don't exist
    File2.delete(testDir + daveName + ".hdf");
    File2.delete(testDir + daveName + ".hdf.zip");

    // test GridSaveAs.main: make the .hdf file from the .grd file
    GridSaveAs.main(new String[] {testDir + ".grd", testDir + ".hdf"});

    // read the hdf file
    Grid grid5 = new Grid();
    grid5.readHDF(testDir + daveName + ".hdf", HdfConstants.DFNT_FLOAT64);
    grid5.makeLonPM180(false);

    // are they the same?
    Test.ensureTrue(grid5.equals(grid3), errorIn);

    // ******* done
    // ??? better: automate by looking at the first 300 bytes of hexDump?
    // but I've re-read the files and checked them above

    // delete the files (or leave them for inspection by hand)
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".asc"),
        errorIn + " while deleting " + testDir + daveName + ".asc");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".grd"),
        errorIn + " while deleting " + testDir + daveName + ".grd");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".hdf"),
        errorIn + " while deleting " + testDir + daveName + ".hdf");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".mat"),
        errorIn + " while deleting " + testDir + daveName + ".mat");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".nc"),
        errorIn + " while deleting " + testDir + daveName + ".nc");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".xyz"),
        errorIn + " while deleting " + testDir + daveName + ".xyz");

    // FUTURE: check metadata
  }

  /**
   * This tests saveAsEsriASCII.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsEsriASCII() throws Exception {

    String2.log("\n***** Grid.testSaveAsEsriASCII");
    Grid grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    grid.makeLonPM180(false); // so saveAsEsriASCII will have to change it
    grid.saveAsEsriASCII(testDir, "temp");
    String result[] = File2.readFromFile88591(testDir + "temp.asc");
    Test.ensureEqual(result[0], "", "");
    String reference =
        "ncols 121\n"
            + "nrows 113\n"
            + "xllcenter -135.0\n"
            + "yllcenter 22.0\n"
            + "cellsize 0.25\n"
            + "nodata_value -9999999\n"
            + "4.2154 4.20402 4.39077 4.3403 4.31166 4.06579 4.0913";
    Test.ensureEqual(result[1].substring(0, reference.length()), reference, "");

    File2.delete(testDir + "temp.asc");
  }

  /**
   * This tests saveAsASCII.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsASCII() throws Exception {

    // 7/14/05 this file was tested by rich.cosgrove@noaa.gov -- fine, see email
    String2.log("\n***** Grid.testSaveAsASCII");
    Grid grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    grid.saveAsASCII(testDir, "temp");
    String result[] = File2.readFromFile88591(testDir + "temp.asc");
    Test.ensureEqual(result[0], "", "");
    String reference =
        "ncols 121\n"
            + "nrows 113\n"
            + "xllcenter -135.0\n"
            + "yllcenter 22.0\n"
            + "cellsize 0.25\n"
            + "nodata_value -9999999\n"
            + "4.2154 4.20402 4.39077 4.3403 4.31166 4.06579 4.0913";
    Test.ensureEqual(result[1].substring(0, reference.length()), reference, "");
    File2.delete(testDir + "temp.asc");
  }

  /**
   * Test saveAsXYZ
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsXYZ() throws Exception {
    String2.log("\n***** Grid.testSaveAsXYZ");
    Grid grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    for (int i = 0; i < 3; i++) {
      grid.saveAsXYZ(testDir, "temp");
    }
    String sa[] = File2.readFromFile88591(testDir + "temp.xyz");
    Test.ensureEqual(sa[0], "", "");
    sa[1] = String2.annotatedString(sa[1].substring(0, 300));
    String2.log(sa[1]);
    Test.ensureEqual(
        sa[1], // note it is now bottom up
        "-135[9]22[9]-5.72424[10]\n"
            + "-134.75[9]22[9]-6.14737[10]\n"
            + "-134.5[9]22[9]-6.21717[10]\n"
            + "-134.25[9]22[9]-5.81695[10]\n"
            + "-134[9]22[9]-5.90187[10]\n"
            + "-133.75[9]22[9]-5.80732[10]\n"
            + "-133.5[9]22[9]-5.92826[10]\n"
            + "-133.25[9]22[9]-5.83655[10]\n"
            + "-133[9]22[9]-4.88194[10]\n"
            + "-132.75[9]22[9]-2.69262[10]\n"
            + "-132.5[9]22[9]-2.63237[10]\n"
            + "-132.25[9]22[9]-2.61885[10]\n"
            + "-132[9]22[9]-2.5787[10]\n"
            + "-131.75[9]22[9]-2.56497[10]\n"
            + "-131.5[9]22[9]NaN[10]\n"
            + "-131.25[9]22[9]NaN[10]\n"
            + "-131[9]22[end]",
        "");
    File2.delete(testDir + "temp.xyz");
  }

  /**
   * This tests saveAsMatlab().
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsMatlab() throws Exception {

    // 7/14/05 this file was tested by luke.spence@noaa.gov in Matlab
    String2.log("\n***** Grid.testSaveAsMatlab");
    Grid grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    grid.saveAsMatlab(testDir, "temp", "ssta");
    String mhd = File2.hexDump(testDir + "temp.mat", 300);
    Test.ensureEqual(
        mhd.substring(0, 71 * 4) + mhd.substring(71 * 7), // remove the creation dateTime
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n"
            + "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            + "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            + "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            + "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            + "00 00 00 0e 00 00 03 f8   00 00 00 06 00 00 00 08                    |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 79   00 03 00 01 6c 6f 6e 00          y    lon  |\n"
            + "00 00 00 09 00 00 03 c8   c0 60 e0 00 00 00 00 00            `       |\n"
            + "c0 60 d8 00 00 00 00 00   c0 60 d0 00 00 00 00 00    `       `       |\n"
            + "c0 60 c8 00 00 00 00 00   c0 60 c0 00 00 00 00 00    `       `       |\n"
            + "c0 60 b8 00 00 00 00 00   c0 60 b0 00 00 00 00 00    `       `       |\n"
            + "c0 60 a8 00 00 00 00 00   c0 60 a0 00 00 00 00 00    `       `       |\n"
            + "c0 60 98 00 00 00 00 00   c0 60 90 00 00 00 00 00    `       `       |\n"
            + "c0 60 88 00 00 00 00 00   c0 60 80 00 00 00 00 00    `       `       |\n"
            + "c0 60 78 00 00 00 00 00   c0 60 70 00  `x      `p                    |\n",
        "File2.hexDump(testDir + \"temp.mat\", 300)");
    File2.delete(testDir + "temp.mat");
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

  /** This reads a grid and save it as geotiff. */
  @org.junit.jupiter.api.Test
  void testSaveAsGeotiff() throws Exception {
    String2.log("\n***** Grid.testSaveAsGeotiff");
    FileNameUtility fileNameUtility = new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods");
    Grid grid = new Grid();
    grid.readGrd(
        testDir + testName + ".grd", -180, 180, 22, 50, Integer.MAX_VALUE, Integer.MAX_VALUE);
    grid.setAttributes(testName, fileNameUtility, false); // data saved not as doubles
    grid.saveAsGeotiff(testDir, testName, "ATssta");
    // do some test of it?
    File2.delete(testDir + testName + ".tif");
  }

  /**
   * This is a unit test for saveAsHDF and readHDF; it makes
   * .../coastwatch/griddata/<testName>Test.hdf.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testHDF() throws Exception {
    String2.log("\n*** Grid.testHDF");
    // load the data
    Grid grid1 = new Grid();
    grid1.readGrd(
        testDir + testName + ".grd", true); // +-180; put longitude into Atlanticentric reference

    // set attributes
    // if (testResult) {
    // set just a few attributes, because that's all the source has
    grid1.globalAttributes().set("title", "Wind, QuikSCAT Seawinds, Composite, Zonal");
    grid1.latAttributes().set("units", FileNameUtility.getLatUnits());
    grid1.lonAttributes().set("units", FileNameUtility.getLonUnits());
    grid1.dataAttributes().set("units", "m s-1");
    // } else {
    // //set all atts
    // grid1.setAttributes(testName, fileNameUtility, false); //data saved not as
    // doubles
    // }

    // save the data
    grid1.saveAsHDF(testDir + testName + "Test", "QNux10");

    // if (testResult) {
    // now try to read it
    Grid grid2 = new Grid();
    grid2.readHDF(testDir + testName + "Test.hdf", HdfConstants.DFNT_FLOAT64);
    // makeLonPM180(true);

    // look for differences
    Test.ensureTrue(grid1.equals(grid2), "testHDF");

    // attributes
    Test.ensureEqual(
        grid1.globalAttributes().getString("title"),
        "Wind, QuikSCAT Seawinds, Composite, Zonal",
        "");
    Test.ensureEqual(grid1.latAttributes().getString("units"), FileNameUtility.getLatUnits(), "");
    Test.ensureEqual(grid1.lonAttributes().getString("units"), FileNameUtility.getLonUnits(), "");
    Test.ensureEqual(grid1.dataAttributes().getString("units"), "m s-1", "");

    // delete the test file
    Test.ensureTrue(
        File2.delete(testDir + testName + "Test.hdf"),
        String2.ERROR + " in Grid.testHDF: unable to delete " + testDir + testName + "Test.hdf");
    // }
  }

  /**
   * This is a test of readNetCDF and saveAsNetCDF.
   *
   * @param fileNameUtility is used to generate all of the metadata based on the file name
   * @throws Exception of trouble
   */
  @org.junit.jupiter.api.Test
  void testNetCDF() throws Exception {
    String2.log("\n*** Grid.testNetCDF");
    FileNameUtility fileNameUtility = new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods");
    // ***** test composite *******************************************************

    Grid grid1 = new Grid();
    grid1.latSpacing = 0.5;
    grid1.lonSpacing = 0.25;
    grid1.minData = 1;
    grid1.maxData = 12;
    grid1.nValidPoints = Integer.MIN_VALUE;
    grid1.lat = new double[] {22, 22.5, 23};
    grid1.lon = new double[] {-135, -134.75, -134.5, -134.25};
    grid1.data = new double[] {1, 2, 3, 4, Double.NaN, 6, 7, 8, 9, 10, 11, 12};

    // add the metadata attributes
    String fileName =
        "LATsstaS1day_20030304120000_x-135_X-134.25_y22_Y23."; // a CWBrowser compatible name
    grid1.setAttributes(fileName, fileNameUtility, false);

    // save the file
    grid1.saveAsNetCDF(testDir, fileName + "Test", "ATssta");

    // read the file
    Grid grid2 = new Grid();
    grid2.readNetCDF(testDir + fileName + "Test.nc", "ATssta", true);
    // String2.log("\ngrid.readNetCDF read: " + grid2.toString(false));

    // see if it has the expected values
    int nLat = grid2.lat.length;
    int nLon = grid2.lon.length;
    Test.ensureTrue(grid1.equals(grid2), "TestNetCDF"); // tests lat, lon, data
    Test.ensureEqual(
        grid2.globalAttributes().get("Conventions"),
        new StringArray(new String[] {"COARDS, CF-1.6, ACDD-1.3, CWHDF"}),
        "Conventions");
    Test.ensureEqual(
        grid2.globalAttributes().get("title"),
        new StringArray(
            new String[] {"SST, NOAA POES AVHRR, LAC, 0.0125 degrees, West US, Day and Night"}),
        "title");
    Test.ensureEqual(
        grid2.globalAttributes().get("summary"),
        new StringArray(
            new String[] {
              "NOAA CoastWatch provides sea surface temperature (SST) products derived from NOAA's Polar Operational Environmental Satellites (POES).  This data is provided at high resolution (0.0125 degrees) for the North Pacific Ocean.  Measurements are gathered by the Advanced Very High Resolution Radiometer (AVHRR) instrument, a multiband radiance sensor carried aboard the NOAA POES satellites."
            }),
        "summary");
    Test.ensureEqual(
        grid2.globalAttributes().get("keywords"),
        new StringArray(
            new String[] {"EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature"}),
        "keywords");
    Test.ensureEqual(
        grid2.globalAttributes().get("id"), new StringArray(new String[] {"LATsstaS1day"}), "id");
    Test.ensureEqual(
        grid2.globalAttributes().get("naming_authority"),
        new StringArray(new String[] {"gov.noaa.pfeg.coastwatch"}),
        "naming_authority");
    Test.ensureEqual(
        grid2.globalAttributes().get("keywords_vocabulary"),
        new StringArray(new String[] {"GCMD Science Keywords"}),
        "keywords_vocabulary");
    Test.ensureEqual(
        grid2.globalAttributes().get("cdm_data_type"),
        new StringArray(new String[] {"Grid"}),
        "cdm_data_typ");
    String history = grid2.globalAttributes().getString("history");
    String2.log("history=" + history);
    Test.ensureTrue(history.startsWith("NOAA NWS Monterey and NOAA CoastWatch\n20"), "getHistory");
    Test.ensureTrue(
        history.endsWith("NOAA CoastWatch (West Coast Node) and NOAA SWFSC ERD"), "getHistory");
    Test.ensureEqual(
        grid2.globalAttributes().get("date_created"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_created");
    Test.ensureEqual(
        grid2.globalAttributes().get("creator_name"),
        new StringArray(new String[] {"NOAA CoastWatch, West Coast Node"}),
        "creator_name");
    Test.ensureEqual(
        grid2.globalAttributes().get("creator_url"),
        new StringArray(new String[] {"https://coastwatch.pfeg.noaa.gov"}),
        "creator_url");
    Test.ensureEqual(
        grid2.globalAttributes().get("creator_email"),
        new StringArray(new String[] {"erd.data@noaa.gov"}),
        "creator_email");
    Test.ensureEqual(
        grid2.globalAttributes().get("institution"),
        new StringArray(new String[] {"NOAA CoastWatch, West Coast Node"}),
        "institution");
    Test.ensureEqual(
        grid2.globalAttributes().get("project"),
        new StringArray(new String[] {"CoastWatch (https://coastwatch.noaa.gov/)"}),
        "project");
    Test.ensureEqual(
        grid2.globalAttributes().get("processing_level"),
        new StringArray(new String[] {"3 (projected)"}),
        "processing_level");
    Test.ensureEqual(
        grid2.globalAttributes().get("acknowledgement"),
        new StringArray(new String[] {"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD"}),
        "acknowledgement");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lat_min"),
        new DoubleArray(new double[] {22}),
        "geospatial_lat_min");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lat_max"),
        new DoubleArray(new double[] {23}),
        "geospatial_lat_max");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lon_min"),
        new DoubleArray(new double[] {-135}),
        "geospatial_lon_min");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lon_max"),
        new DoubleArray(new double[] {-134.25}),
        "geospatial_lon_max");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lat_units"),
        new StringArray(new String[] {"degrees_north"}),
        "geospatial_lat_units");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lat_resolution"),
        new DoubleArray(new double[] {0.5}),
        "geospatial_lat_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lon_units"),
        new StringArray(new String[] {"degrees_east"}),
        "geospatial_lon_units");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lon_resolution"),
        new DoubleArray(new double[] {0.25}),
        "geospatial_lon_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_start"),
        new StringArray(new String[] {"2003-03-04T00:00:00Z"}),
        "time_coverage_start");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_end"),
        new StringArray(new String[] {"2003-03-05T00:00:00Z"}),
        "time_coverage_end");
    // Test.ensureEqual(grid2.globalAttributes().get("time_coverage_resolution", new
    // StringArray(new String[]{""}), "time_coverage_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("standard_name_vocabulary"),
        new StringArray(new String[] {"CF Standard Name Table v70"}),
        "standard_name_vocabulary");
    Test.ensureEqual(
        grid2.globalAttributes().get("license"),
        new StringArray(
            new String[] {
              "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information."
            }),
        "license");
    Test.ensureEqual(
        grid2.globalAttributes().get("contributor_name"),
        new StringArray(new String[] {"NOAA NWS Monterey and NOAA CoastWatch"}),
        "contributor_name");
    Test.ensureEqual(
        grid2.globalAttributes().get("contributor_role"),
        new StringArray(new String[] {"Source of level 2 data."}),
        "contributor_role");
    Test.ensureEqual(
        grid2.globalAttributes().get("date_issued"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_issued");
    Test.ensureEqual(
        grid2.globalAttributes().getString("references"),
        "NOAA POES satellites information: https://coastwatch.noaa.gov/poes_sst_overview.html . Processing information: https://www.ospo.noaa.gov/Operations/POES/index.html . Processing reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b.",
        "references");
    Test.ensureEqual(
        grid2.globalAttributes().getString("source"),
        "satellite observation: POES, AVHRR HRPT",
        "source");
    // Google Earth
    Test.ensureEqual(
        grid2.globalAttributes().get("Southernmost_Northing"),
        new DoubleArray(new double[] {22}),
        "southernmost");
    Test.ensureEqual(
        grid2.globalAttributes().get("Northernmost_Northing"),
        new DoubleArray(new double[] {23}),
        "northernmost");
    Test.ensureEqual(
        grid2.globalAttributes().get("Westernmost_Easting"),
        new DoubleArray(new double[] {-135}),
        "westernmost");
    Test.ensureEqual(
        grid2.globalAttributes().get("Easternmost_Easting"),
        new DoubleArray(new double[] {-134.25}),
        "easternmost");

    // cwhdf attributes
    Test.ensureEqual(
        grid2.globalAttributes().get("cwhdf_version"),
        new StringArray(new String[] {"3.4"}),
        "cwhdf_version"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("satellite"),
        new StringArray(new String[] {"POES"}),
        "satellite"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("sensor"),
        new StringArray(new String[] {"AVHRR HRPT"}),
        "sensor"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("composite"),
        new StringArray(new String[] {"true"}),
        "composite"); // string

    Test.ensureEqual(
        grid2.globalAttributes().get("pass_date"),
        new IntArray(new int[] {12115}),
        "pass_date"); // int32[nDays]
    Test.ensureEqual(
        grid2.globalAttributes().get("start_time"),
        new DoubleArray(new double[] {0}),
        "start_time"); // float64[nDays]
    Test.ensureEqual(
        grid2.globalAttributes().get("origin"),
        new StringArray(new String[] {"NOAA NWS Monterey and NOAA CoastWatch"}),
        "origin"); // string
    // Test.ensureEqual(grid2.globalAttributes().get("history"), new StringArray(new
    // String[]{"unknown"}), "history"); //string

    Test.ensureEqual(
        grid2.globalAttributes().get("projection_type"),
        new StringArray(new String[] {"mapped"}),
        "projection_type"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("projection"),
        new StringArray(new String[] {"geographic"}),
        "projection"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("gctp_sys"), new IntArray(new int[] {0}), "gctp_sys"); // int32
    Test.ensureEqual(
        grid2.globalAttributes().get("gctp_zone"),
        new IntArray(new int[] {0}),
        "gctp_zone"); // int32
    Test.ensureEqual(
        grid2.globalAttributes().get("gctp_parm"),
        new DoubleArray(new double[15]),
        "gctp_parm"); // float64[15
    // 0's]
    Test.ensureEqual(
        grid2.globalAttributes().get("gctp_datum"),
        new IntArray(new int[] {12}),
        "gctp_datum"); // int32
    // 12=WGS84

    Test.ensureEqual(
        grid2.globalAttributes().get("et_affine"),
        new DoubleArray(
            new double[] {0, grid1.latSpacing, grid1.lonSpacing, 0, grid1.lon[0], grid1.lat[0]}),
        "et_affine"); // right side up

    Test.ensureEqual(
        grid2.globalAttributes().get("rows"),
        new IntArray(new int[] {grid1.lat.length}),
        "rows"); // int32
    // number
    // of
    // rows
    Test.ensureEqual(
        grid2.globalAttributes().get("cols"),
        new IntArray(new int[] {grid1.lon.length}),
        "cols"); // int32
    // number
    // of
    // columns
    Test.ensureEqual(
        grid2.globalAttributes().get("polygon_latitude"),
        new DoubleArray(
            new double[] {
              grid1.lat[0], grid1.lat[nLat - 1], grid1.lat[nLat - 1], grid1.lat[0], grid1.lat[0]
            }),
        "polygon_latitude");
    Test.ensureEqual(
        grid2.globalAttributes().get("polygon_longitude"),
        new DoubleArray(
            new double[] {
              grid1.lon[0], grid1.lon[0], grid1.lon[nLon - 1], grid1.lon[nLon - 1], grid1.lon[0]
            }),
        "polygon_longitude");

    // lat attributes
    Test.ensureEqual(
        grid2.latAttributes().get("long_name"),
        new StringArray(new String[] {"Latitude"}),
        "lat long_name");
    Test.ensureEqual(
        grid2.latAttributes().get("standard_name"),
        new StringArray(new String[] {"latitude"}),
        "lat standard_name");
    Test.ensureEqual(
        grid2.latAttributes().get("units"),
        new StringArray(new String[] {"degrees_north"}),
        "lat units");
    Test.ensureEqual(
        grid2.latAttributes().get("point_spacing"),
        new StringArray(new String[] {"even"}),
        "lat point_spacing");
    Test.ensureEqual(
        grid2.latAttributes().get("actual_range"),
        new DoubleArray(new double[] {22, 23}),
        "lat actual_range");

    // CWHDF metadata/attributes for Latitude
    Test.ensureEqual(
        grid2.latAttributes().get("coordsys"),
        new StringArray(new String[] {"geographic"}),
        "coordsys"); // string
    Test.ensureEqual(
        grid2.latAttributes().get("fraction_digits"),
        new IntArray(new int[] {4}),
        "fraction_digits"); // int32

    // lon attributes
    Test.ensureEqual(
        grid2.lonAttributes().get("long_name"),
        new StringArray(new String[] {"Longitude"}),
        "lon long_name");
    Test.ensureEqual(
        grid2.lonAttributes().get("standard_name"),
        new StringArray(new String[] {"longitude"}),
        "lon standard_name");
    Test.ensureEqual(
        grid2.lonAttributes().get("units"),
        new StringArray(new String[] {"degrees_east"}),
        "lon units");
    Test.ensureEqual(
        grid2.lonAttributes().get("point_spacing"),
        new StringArray(new String[] {"even"}),
        "lon point_spacing");
    Test.ensureEqual(
        grid2.lonAttributes().get("actual_range"),
        new DoubleArray(new double[] {-135, -134.25}),
        "lon actual_range");

    // CWHDF metadata/attributes for Longitude
    Test.ensureEqual(
        grid2.lonAttributes().get("coordsys"),
        new StringArray(new String[] {"geographic"}),
        "coordsys"); // string
    Test.ensureEqual(
        grid2.lonAttributes().get("fraction_digits"),
        new IntArray(new int[] {4}),
        "fraction_digits"); // int32

    // data attributes
    Test.ensureEqual(
        grid2.dataAttributes().get("long_name"),
        new StringArray(
            new String[] {"SST, NOAA POES AVHRR, LAC, 0.0125 degrees, West US, Day and Night"}),
        "data long_name");
    Test.ensureEqual(
        grid2.dataAttributes().get("standard_name"),
        new StringArray(new String[] {"sea_surface_temperature"}),
        "data standard_name");
    Test.ensureEqual(
        grid2.dataAttributes().get("units"),
        new StringArray(new String[] {"degree_C"}),
        "data units");
    Test.ensureEqual(
        grid2.dataAttributes().get("_FillValue"),
        new FloatArray(new float[] {-9999999}),
        "data _FillValue");
    Test.ensureEqual(
        grid2.dataAttributes().get("missing_value"),
        new FloatArray(new float[] {-9999999}),
        "data missing_value");
    Test.ensureEqual(
        grid2.dataAttributes().get("numberOfObservations"),
        new IntArray(new int[] {11}),
        "data numberOfObservations");
    Test.ensureEqual(
        grid2.dataAttributes().get("percentCoverage"),
        new DoubleArray(new double[] {0.9166666666666666}),
        "data percentCoverage");

    // CWHDF metadata/attributes for the data: varName
    Test.ensureEqual(
        grid2.dataAttributes().get("coordsys"),
        new StringArray(new String[] {"geographic"}),
        "coordsys"); // string
    Test.ensureEqual(
        grid2.dataAttributes().get("fraction_digits"),
        new IntArray(new int[] {1}),
        "fraction_digits"); // int32

    // test that it has correct centeredTime value
    NetcdfFile netcdfFile = NcHelper.openFile(testDir + fileName + "Test.nc");
    try {
      Variable timeVariable = NcHelper.findVariable(netcdfFile, "time");
      PrimitiveArray pa = NcHelper.getPrimitiveArray(timeVariable);
      Test.ensureEqual(pa.size(), 1, "");
      Test.ensureEqual(
          pa.getDouble(0), Calendar2.isoStringToEpochSeconds("2003-03-04T12:00:00"), "");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }

    // delete the file
    File2.delete(testDir + fileName + "Test.nc");

    // ****** test hday *****************************************************
    // add the metadata attributes
    fileName = "LATsstaSpass_20030304170000_x-135_X-134.25_y22_Y23."; // a CWBrowser compatible name
    grid1.setAttributes(fileName, fileNameUtility, false);

    // save the file
    grid1.saveAsNetCDF(testDir, fileName + "Test", "ATssta");

    // read the file
    grid2.clear(); // extra insurance
    grid2.readNetCDF(testDir + fileName + "Test.nc", "ATssta", true);
    // String2.log("\ngrid.readNetCDF read: " + grid2.toString(false));

    // see if it has the expected values
    nLat = grid2.lat.length;
    nLon = grid2.lon.length;
    Test.ensureTrue(grid1.equals(grid2), "TestNetCDF"); // tests lat, lon, data
    Test.ensureEqual(
        grid2.globalAttributes().get("date_created"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_created");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_start"),
        new StringArray(new String[] {"2003-03-04T17:00:00Z"}),
        "time_coverage_start");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_end"),
        new StringArray(new String[] {"2003-03-04T17:00:00Z"}),
        "time_coverage_end");
    // Test.ensureEqual(grid2.globalAttributes().get("time_coverage_resolution", new
    // StringArray(new String[]{""}), "time_coverage_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("date_issued"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_issued");

    Test.ensureEqual(
        grid2.globalAttributes().get("pass_date"),
        new IntArray(new int[] {12115}),
        "pass_date"); // int32[nDays]
    Test.ensureEqual(
        grid2.globalAttributes().get("start_time"),
        new DoubleArray(new double[] {17 * Calendar2.SECONDS_PER_HOUR}),
        "start_time"); // float64[nDays]

    // test that it has correct centeredTime value
    netcdfFile = NcHelper.openFile(testDir + fileName + "Test.nc");
    try {
      Variable timeVariable = NcHelper.findVariable(netcdfFile, "time");
      PrimitiveArray pa = NcHelper.getPrimitiveArray(timeVariable);
      Test.ensureEqual(pa.size(), 1, "");
      Test.ensureEqual(
          pa.getDouble(0), Calendar2.isoStringToEpochSeconds("2003-03-04T17:00:00"), "");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }

    // delete the file
    File2.delete(testDir + fileName + "Test.nc");

    // ****** test climatology *****************************************************
    // add the metadata attributes
    fileName = "LATsstaS1day_00010304120000_x-135_X-134.25_y22_Y23."; // a CWBrowser compatible name
    grid1.setAttributes(fileName, fileNameUtility, false);

    // save the file
    grid1.saveAsNetCDF(testDir, fileName + "Test", "ATssta");

    // read the file
    grid2.clear(); // extra insurance
    grid2.readNetCDF(testDir + fileName + "Test.nc", "ATssta", true);
    // String2.log("\ngrid.readNetCDF read: " + grid2.toString(false));

    // see if it has the expected values
    nLat = grid2.lat.length;
    nLon = grid2.lon.length;
    Test.ensureTrue(grid1.equals(grid2), "TestNetCDF"); // tests lat, lon, data
    Test.ensureEqual(
        grid2.globalAttributes().get("date_created"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_created");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_start"),
        new StringArray(new String[] {"0001-03-04T00:00:00Z"}),
        "time_coverage_start");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_end"),
        new StringArray(new String[] {"0001-03-05T00:00:00Z"}),
        "time_coverage_end");
    // Test.ensureEqual(grid2.globalAttributes().get("time_coverage_resolution", new
    // StringArray(new String[]{""}), "time_coverage_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("date_issued"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_issued");

    Test.ensureEqual(
        grid2.globalAttributes().get("pass_date"),
        new IntArray(new int[] {-719101}),
        "pass_date"); // int32[nDays]
    Test.ensureEqual(
        grid2.globalAttributes().get("start_time"),
        new DoubleArray(new double[] {0}),
        "start_time"); // float64[nDays]

    // test that it has correct centeredTime value
    netcdfFile = NcHelper.openFile(testDir + fileName + "Test.nc");
    try {
      Variable timeVariable = NcHelper.findVariable(netcdfFile, "time");
      PrimitiveArray pa = NcHelper.getPrimitiveArray(timeVariable);
      Test.ensureEqual(pa.size(), 1, "");
      Test.ensureEqual(
          pa.getDouble(0), Calendar2.isoStringToEpochSeconds("0001-03-04T12:00:00"), "");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }

    // delete the file
    File2.delete(testDir + fileName + "Test.nc");
  }
}
