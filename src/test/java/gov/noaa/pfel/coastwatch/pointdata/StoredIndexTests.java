package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.DoubleArray;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

class StoredIndexTests {

  /**
   * This tests the methods in this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    StoredIndex.verbose = true;

    String dir = File2.getSystemTempDirectory();
    String name = "StoredIndexTest";
    int n = 1000000;
    DoubleArray pa = new DoubleArray(n, false);
    for (int i = 0; i < n; i++)
      pa.add(i * 0.1);
    StoredIndex index = new StoredIndex(dir + name, pa);
    try {
      // get all
      Test.ensureEqual(String2.toCSSVString(index.subset(0, n / .1)), "0, 999999", "");

      // get some
      Test.ensureEqual(String2.toCSSVString(index.subset(1, 2)), "10, 20", "");

      // between 2 indices
      Test.ensureEqual(String2.toCSSVString(index.subset(1.55, 1.56)), "16, 15", "");

      // get none
      Test.ensureEqual(String2.toCSSVString(index.subset(-.1, -.1)), "-1, -1", "");
      Test.ensureEqual(String2.toCSSVString(index.subset(100000, 100000)), "-1, -1", "");

    } finally {
      index.close();
    }
    String2.log("\n***** StoredIndex.basicTest finished successfully");

  }

}
