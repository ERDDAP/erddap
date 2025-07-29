package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.String2;
import com.cohort.util.Test;

class DataHelperTests {

  /** This tests the methods in this class. */
  @org.junit.jupiter.api.Test
  void basicTest() {
    String2.log("\n*** DataHelper.basicTest...");

    // ensure that FAKE_MISSING_VALUE is exactly equal when converted to float or
    // double
    Test.ensureTrue(-9999999f == -9999999.0, "");
    // 8 9's fails
    Test.ensureTrue(
        (float) DataHelper.FAKE_MISSING_VALUE == (double) DataHelper.FAKE_MISSING_VALUE, "");

    // copy(double dar[], int start, int end, int stride) {
    double dar[] = {0, 0.1, 0.2, 0.3, 0.4, 0.5};
    Test.ensureEqual(DataHelper.copy(dar, 0, 5, 1), dar, "copy a");
    Test.ensureEqual(DataHelper.copy(dar, 0, 5, 2), new double[] {0, 0.2, 0.4}, "copy b");
    Test.ensureEqual(DataHelper.copy(dar, 1, 4, 2), new double[] {0.1, 0.3}, "copy c");

    // binaryFindStartIndex(double dar[], double end) {
    Test.ensureEqual(
        DataHelper.binaryFindStartIndex(dar, -.06), 0, "binaryFindStartIndex a1"); // important
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, -.05), 0, "binaryFindStartIndex a2");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, -.00000001), 0, "binaryFindStartIndex a");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, 0), 0, "binaryFindStartIndex b");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, 0.00000001), 0, "binaryFindStartIndex c");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, 0.01), 0, "binaryFindStartIndex d");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, 0.09), 1, "binaryFindStartIndex e");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, 0.49999999), 5, "binaryFindStartIndex f");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, 0.5), 5, "binaryFindStartIndex g");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, 0.50000001), 5, "binaryFindStartIndex h");
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, 0.55), 5, "binaryFindStartIndex j");
    Test.ensureEqual(
        DataHelper.binaryFindStartIndex(dar, 0.56), -1, "binaryFindStartIndex j2"); // important
    Test.ensureEqual(DataHelper.binaryFindStartIndex(dar, Double.NaN), 0, "binaryFindStartIndex l");

    // binaryFindEndIndex(double dar[], double end) {
    Test.ensureEqual(
        DataHelper.binaryFindEndIndex(dar, -.06), -1, "binaryFindEndIndex a1"); // important
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, -.05), 0, "binaryFindEndIndex a2");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, -.00000001), 0, "binaryFindEndIndex a");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, 0), 0, "binaryFindEndIndex b");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, 0.00000001), 0, "binaryFindEndIndex c");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, 0.01), 0, "binaryFindEndIndex d");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, 0.09), 1, "binaryFindEndIndex e");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, 0.49999999), 5, "binaryFindEndIndex f");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, 0.5), 5, "binaryFindEndIndex g");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, 0.50000001), 5, "binaryFindEndIndex h");
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, 0.55), 5, "binaryFindEndIndex j");
    Test.ensureEqual(
        DataHelper.binaryFindEndIndex(dar, 0.56), 5, "binaryFindEndIndex j2"); // important
    Test.ensureEqual(DataHelper.binaryFindEndIndex(dar, Double.NaN), 5, "binaryFindEndIndex l");

    Test.ensureEqual(DataHelper.findStride(7, 1000), 1, "findStride a");
    Test.ensureEqual(DataHelper.findStride(7, 7), 1, "findStride b");
    Test.ensureEqual(DataHelper.findStride(7, 6), 1, "findStride c");
    Test.ensureEqual(DataHelper.findStride(7, 5), 1, "findStride d");
    Test.ensureEqual(DataHelper.findStride(7, 4), 2, "findStride e");
    Test.ensureEqual(DataHelper.findStride(7, 3), 3, "findStride f");
    Test.ensureEqual(DataHelper.findStride(7, 2), 6, "findStride g");
    Test.ensureEqual(DataHelper.findStride(7, 1), 7, "findStride h");

    Test.ensureEqual(DataHelper.strideWillFind(5, 1), 5, "strideWillFind a");
    Test.ensureEqual(DataHelper.strideWillFind(5, 2), 3, "strideWillFind b");
    Test.ensureEqual(DataHelper.strideWillFind(5, 3), 2, "strideWillFind c");
    Test.ensureEqual(DataHelper.strideWillFind(5, 4), 2, "strideWillFind d");
    Test.ensureEqual(DataHelper.strideWillFind(5, 5), 1, "strideWillFind e");

    // findStride(double lonSpacing, double desiredMinLon, double desiredMaxLon, int
    // nLonPointsNeeded)
    Test.ensureEqual(DataHelper.findStride(1, Double.NaN, 6, 1000), 1, "findStride n1");
    Test.ensureEqual(DataHelper.findStride(1, 0, Double.NaN, 1000), 1, "findStride n2");
    Test.ensureEqual(DataHelper.findStride(1, 0, 6, 1000), 1, "findStride a");
    Test.ensureEqual(DataHelper.findStride(1, 0, 6, 7), 1, "findStride b");
    Test.ensureEqual(DataHelper.findStride(1, 0, 6, 6), 1, "findStride c");
    Test.ensureEqual(DataHelper.findStride(1, 0, 6, 5), 1, "findStride d");
    Test.ensureEqual(DataHelper.findStride(1, 0, 6, 4), 2, "findStride e");
    Test.ensureEqual(DataHelper.findStride(0.99, 0, 6, 4), 2, "findStride e2");
    Test.ensureEqual(DataHelper.findStride(1.01, 0, 6, 4), 2, "findStride e3");
    Test.ensureEqual(DataHelper.findStride(1, 0, 6, 3), 3, "findStride f");
    Test.ensureEqual(DataHelper.findStride(1, 0, 6, 2), 6, "findStride g");
    Test.ensureEqual(DataHelper.findStride(1, 0, 6, 1), 7, "findStride h");
    // incorrectly setup test? Test.ensureEqual(findStride(.1, -179.4, 179.9, 515),
    // .7, "findStride i");

    // getRegularArray(int n, double min, double spacing) {
    Test.ensureEqual(
        DataHelper.getRegularArray(5, 2, 0.1),
        new double[] {2, 2.1, 2.2, 2.3, 2.4},
        "getRegularArray");

    // adjustNPointsNeeded n, oldRange, newRange
    Test.ensureEqual(DataHelper.adjustNPointsNeeded(100, 30, 10), 34, "");
    Test.ensureEqual(DataHelper.adjustNPointsNeeded(100, 30, 10.00001), 34, "");
    Test.ensureEqual(DataHelper.adjustNPointsNeeded(100, 30, 9.99999), 34, "");
    Test.ensureEqual(DataHelper.adjustNPointsNeeded(100, 10, 30), 300, "");

    // done
    String2.log("\n***** DataHelper.test finished successfully");
  }
}
