package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

class TwoGridsTests {
  /**
   * This makes a small test Grid.
   *
   * @param grid1 if true, this makes grid1, otherwise it makes grid2.
   */
  private static Grid makeTestGrid(boolean grid1) {
    Grid grid = new Grid();
    grid.lon = new double[] { 0, 1, 2 };
    grid.lat = new double[] { 10, 20 };
    grid.lonSpacing = 1;
    grid.latSpacing = 10;
    grid.data = new double[grid.lon.length * grid.lat.length];
    for (int x = 0; x < grid.lon.length; x++)
      for (int y = 0; y < grid.lat.length; y++)
        grid.setData(x, y, (grid1 ? 1 : 2) * (grid.lon[x] + grid.lat[y]));
    grid.setData(1, 0, Double.NaN);

    grid.globalAttributes().set("creator", "Bob Simons");
    grid.globalAttributes().set("time_coverage_start", "2006-07-03T00:00:00");
    grid.globalAttributes().set("time_coverage_end", "2006-07-04T00:00:00");
    grid.lonAttributes().set("axis", "X");
    grid.lonAttributes().set("units", "degrees_east");
    grid.latAttributes().set("axis", "Y");
    grid.latAttributes().set("units", "degrees_north");
    grid.dataAttributes().set("units", "m s-1");

    return grid;
  }

  /**
   * This tests saveAsMatlab().
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsMatlab() throws Exception {

    // Luke verified the .mat file 2006-10-24
    String2.log("\n***** TwoGrids.testSaveAsMatlab");
    TwoGrids.saveAsMatlab(makeTestGrid(true), makeTestGrid(false), GridTests.testDir, "temp", "QNux10", "QNuy10");
    String mhd = File2.hexDump(GridTests.testDir + "temp.mat", 10000000);
    // String2.log(mhd);
    Test.ensureEqual(
        mhd.substring(0, 71 * 4) + mhd.substring(71 * 7), // remove the creation dateTime
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
            "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" +
            "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" +
            "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" +
            "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" +
            "00 00 00 0e 00 00 00 48   00 00 00 06 00 00 00 08          H         |\n" +
            "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
            "00 00 00 01 00 00 00 03   00 03 00 01 6c 6f 6e 00               lon  |\n" +
            "00 00 00 09 00 00 00 18   00 00 00 00 00 00 00 00                    |\n" +
            "3f f0 00 00 00 00 00 00   40 00 00 00 00 00 00 00   ?       @        |\n" +
            "00 00 00 0e 00 00 00 40   00 00 00 06 00 00 00 08          @         |\n" +
            "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
            "00 00 00 01 00 00 00 02   00 03 00 01 6c 61 74 00               lat  |\n" +
            "00 00 00 09 00 00 00 10   40 24 00 00 00 00 00 00           @$       |\n" +
            "40 34 00 00 00 00 00 00   00 00 00 0e 00 00 00 68   @4             h |\n" +
            "00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n" +
            "00 00 00 05 00 00 00 08   00 00 00 02 00 00 00 03                    |\n" +
            "00 00 00 01 00 00 00 06   51 4e 75 78 31 30 00 00           QNux10   |\n" +
            "00 00 00 09 00 00 00 30   40 24 00 00 00 00 00 00          0@$       |\n" +
            "40 34 00 00 00 00 00 00   7f f8 00 00 00 00 00 00   @4               |\n" +
            "40 35 00 00 00 00 00 00   40 28 00 00 00 00 00 00   @5      @(       |\n" +
            "40 36 00 00 00 00 00 00   00 00 00 0e 00 00 00 68   @6             h |\n" +
            "00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n" +
            "00 00 00 05 00 00 00 08   00 00 00 02 00 00 00 03                    |\n" +
            "00 00 00 01 00 00 00 06   51 4e 75 79 31 30 00 00           QNuy10   |\n" +
            "00 00 00 09 00 00 00 30   40 34 00 00 00 00 00 00          0@4       |\n" +
            "40 44 00 00 00 00 00 00   7f f8 00 00 00 00 00 00   @D               |\n" +
            "40 45 00 00 00 00 00 00   40 38 00 00 00 00 00 00   @E      @8       |\n" +
            "40 46 00 00 00 00 00 00   @F                                         |\n",
        "");
    File2.delete(GridTests.testDir + "temp.mat");

  }

  /**
   * This is a test of readNetCDF and saveAsNetCDF.
   *
   * @throws Exception of trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsNetCDF() throws Exception {
    String2.log("\n*** TwoGrids.testSaveAsNetCDF");

    // add the metadata attributes
    String fileName = "temp";

    // save the file
    Grid grid1 = makeTestGrid(true);
    Grid grid2 = makeTestGrid(false);
    TwoGrids.saveAsNetCDF(grid1, grid2, GridTests.testDir, "temp", "QNux10", "QNuy10");

    // read the grid1 part of the file
    Grid grid3 = new Grid();
    grid3.readNetCDF(GridTests.testDir + "temp.nc", "QNux10", true);
    Test.ensureTrue(grid1.equals(grid3), "");
    Test.ensureTrue(grid1.globalAttributes().equals(grid3.globalAttributes()), "");
    Test.ensureTrue(grid1.latAttributes().equals(grid3.latAttributes()), "");
    Test.ensureTrue(grid1.lonAttributes().equals(grid3.lonAttributes()), "");
    Test.ensureTrue(grid1.dataAttributes().equals(grid3.dataAttributes()), "");

    // read the grid2 part of the file
    grid3.readNetCDF(GridTests.testDir + "temp.nc", "QNuy10", true);
    Test.ensureTrue(grid2.equals(grid3), "");
    Test.ensureTrue(grid2.globalAttributes().equals(grid3.globalAttributes()), "");
    Test.ensureTrue(grid2.latAttributes().equals(grid3.latAttributes()), "");
    Test.ensureTrue(grid2.lonAttributes().equals(grid3.lonAttributes()), "");
    Test.ensureTrue(grid2.dataAttributes().equals(grid3.dataAttributes()), "");

    File2.delete(GridTests.testDir + "temp.nc");

  }

  /**
   * This tests saveAsXyz().
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsXyz() throws Exception {

    String2.log("\n***** TwoGrids.testSaveAsXyz");
    TwoGrids.saveAsXyz(makeTestGrid(true), makeTestGrid(false), GridTests.testDir, "temp", "NaN");
    String xyz[] = File2.readFromFile88591(GridTests.testDir + "temp.xyz");
    Test.ensureEqual(xyz[0], "", "");
    // String2.log(xyz[1]);
    Test.ensureEqual(xyz[1],
        "0\t10\t10.0\t20.0\n" +
            "1\t10\tNaN\tNaN\n" +
            "2\t10\t12.0\t24.0\n" +
            "0\t20\t20.0\t40.0\n" +
            "1\t20\t21.0\t42.0\n" +
            "2\t20\t22.0\t44.0\n",
        "");
    File2.delete(GridTests.testDir + "temp.xyz");

  }

}
