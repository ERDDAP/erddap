package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.awt.geom.GeneralPath;

class GSHHSTests {

  /** This runs a unit test. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** GSSHS.basicTest");

    // verbose = true;
    int xi[], yi[];
    double xr[], yr[];
    int n;

    // 1 good point
    xi = new int[] {15};
    yi = new int[] {150};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 1, "");
    Test.ensureEqual(String2.toCSSVString(xi), "15", "");
    Test.ensureEqual(String2.toCSSVString(yi), "150", "");

    xr = new double[] {15};
    yr = new double[] {150};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 1, "");
    Test.ensureEqual(String2.toCSSVString(xr), "15.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "150.0", "");

    // 1 bad point
    xi = new int[] {5};
    yi = new int[] {6};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    xr = new double[] {5};
    yr = new double[] {6};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    // 1 good and 1 bad point
    xi = new int[] {15, 5};
    yi = new int[] {150, 6};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 2, "");
    Test.ensureEqual(String2.toCSSVString(xi), "15, 5", "");
    Test.ensureEqual(String2.toCSSVString(yi), "150, 6", "");

    xr = new double[] {15, 5};
    yr = new double[] {150, 6};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 2, "");
    Test.ensureEqual(String2.toCSSVString(xr), "15.0, 5.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "150.0, 6.0", "");

    // 1 bad and 1 good point
    xi = new int[] {5, 15};
    yi = new int[] {6, 150};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 2, "");
    Test.ensureEqual(String2.toCSSVString(xi), "5, 15", "");
    Test.ensureEqual(String2.toCSSVString(yi), "6, 150", "");

    xr = new double[] {5, 15};
    yr = new double[] {6, 150};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 2, "");
    Test.ensureEqual(String2.toCSSVString(xr), "5.0, 15.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "6.0, 150.0", "");

    // 2 good and 2 bad points
    xi = new int[] {15, 16, 5, 6};
    yi = new int[] {150, 160, 6, 7};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 4, "");
    Test.ensureEqual(String2.toCSSVString(xi), "15, 16, 5, 6", "");
    Test.ensureEqual(String2.toCSSVString(yi), "150, 160, 6, 7", "");

    xr = new double[] {15, 16, 5, 6};
    yr = new double[] {150, 160, 6, 7};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 4, "");
    Test.ensureEqual(String2.toCSSVString(xr), "15.0, 16.0, 5.0, 6.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "150.0, 160.0, 6.0, 7.0", "");

    // 2 bad and 2 good point
    xi = new int[] {5, 6, 15, 16};
    yi = new int[] {6, 7, 150, 160};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 4, "");
    Test.ensureEqual(String2.toCSSVString(xi), "5, 6, 15, 16", "");
    Test.ensureEqual(String2.toCSSVString(yi), "6, 7, 150, 160", "");

    xr = new double[] {5, 6, 15, 16};
    yr = new double[] {6, 7, 150, 160};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 4, "");
    Test.ensureEqual(String2.toCSSVString(xr), "5.0, 6.0, 15.0, 16.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "6.0, 7.0, 150.0, 160.0", "");

    // 2 good and 2 bad (diff sector) points
    xi = new int[] {15, 16, 5, 6};
    yi = new int[] {150, 160, 6, 250};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 4, "");
    Test.ensureEqual(String2.toCSSVString(xi), "15, 16, 5, 6", "");
    Test.ensureEqual(String2.toCSSVString(yi), "150, 160, 6, 250", "");

    xr = new double[] {15, 16, 5, 6};
    yr = new double[] {150, 160, 6, 250};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 4, "");
    Test.ensureEqual(String2.toCSSVString(xr), "15.0, 16.0, 5.0, 6.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "150.0, 160.0, 6.0, 250.0", "");

    // 2 bad (diff sector) and 2 good point
    xi = new int[] {5, 6, 15, 16};
    yi = new int[] {6, 250, 150, 160};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 4, "");
    Test.ensureEqual(String2.toCSSVString(xi), "5, 6, 15, 16", "");
    Test.ensureEqual(String2.toCSSVString(yi), "6, 250, 150, 160", "");

    xr = new double[] {5, 6, 15, 16};
    yr = new double[] {6, 250, 150, 160};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 4, "");
    Test.ensureEqual(String2.toCSSVString(xr), "5.0, 6.0, 15.0, 16.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "6.0, 250.0, 150.0, 160.0", "");

    // 3 good and 3 bad points
    xi = new int[] {15, 16, 17, 5, 6, 7};
    yi = new int[] {150, 160, 170, 6, 7, 8};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 5, "");
    Test.ensureEqual(String2.toCSSVString(xi), "15, 16, 17, 5, 7, 7", "");
    Test.ensureEqual(String2.toCSSVString(yi), "150, 160, 170, 6, 8, 8", "");

    xr = new double[] {15, 16, 17, 5, 6, 7};
    yr = new double[] {150, 160, 170, 6, 7, 8};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 5, "");
    Test.ensureEqual(String2.toCSSVString(xr), "15.0, 16.0, 17.0, 5.0, 7.0, 7.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "150.0, 160.0, 170.0, 6.0, 8.0, 8.0", "");

    // 3 bad and 3 good point
    xi = new int[] {5, 6, 7, 15, 16, 17};
    yi = new int[] {6, 7, 8, 150, 160, 170};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 5, "");
    Test.ensureEqual(String2.toCSSVString(xi), "5, 7, 15, 16, 17, 17", "");
    Test.ensureEqual(String2.toCSSVString(yi), "6, 8, 150, 160, 170, 170", "");

    xr = new double[] {5, 6, 7, 15, 16, 17};
    yr = new double[] {6, 7, 8, 150, 160, 170};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 5, "");
    Test.ensureEqual(String2.toCSSVString(xr), "5.0, 7.0, 15.0, 16.0, 17.0, 17.0", "");
    Test.ensureEqual(String2.toCSSVString(yr), "6.0, 8.0, 150.0, 160.0, 170.0, 170.0", "");

    // 3 sectors, but all north
    xi = new int[] {5, 15, 25};
    yi = new int[] {250, 260, 270};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    xr = new double[] {5, 15, 25};
    yr = new double[] {250, 260, 270};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    // 3 sectors, but all south
    xi = new int[] {5, 15, 25};
    yi = new int[] {60, 70, 80};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    xr = new double[] {5, 15, 25};
    yr = new double[] {60, 70, 80};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    // 3 sectors, but all east
    xi = new int[] {25, 26, 27};
    yi = new int[] {50, 150, 250};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    xr = new double[] {25, 26, 27};
    yr = new double[] {50, 150, 250};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    // 3 sectors, but all west
    xi = new int[] {5, 6, 7};
    yi = new int[] {50, 150, 250};
    n = GSHHS.reduce(xi.length, xi, yi, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    xr = new double[] {5, 6, 7};
    yr = new double[] {50, 150, 250};
    n = GSHHS.reduce(xr.length, xr, yr, 10, 20, 100, 200); // wesn
    Test.ensureEqual(n, 0, "");

    // force creation of new file
    GeneralPath gp1 = GSHHS.getGeneralPath('h', 1, -135, -105, 22, 50, true);

    // read cached version
    long time = System.currentTimeMillis();
    GeneralPath gp2 = GSHHS.getGeneralPath('h', 1, -135, -105, 22, 50, true);
    time = System.currentTimeMillis() - time;

    // is it the same (is GeneralPath.equals a deep test? probably not)

    // test speed
    Test.ensureTrue(time < 20, "time=" + time);
  }
}
