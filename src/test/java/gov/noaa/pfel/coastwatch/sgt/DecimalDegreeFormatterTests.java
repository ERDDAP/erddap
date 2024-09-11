package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.Test;

class DecimalDegreeFormatterTests {
  /**
   * This tests the methods in this class.
   *
   * @param args is ignored
   */
  @org.junit.jupiter.api.Test
  void basicTest() {
    DecimalDegreeFormatter ddf = new DecimalDegreeFormatter();
    Test.ensureEqual(ddf.format(4), "4°", "a");
    Test.ensureEqual(ddf.format(4.500000001), "4.5°", "b");
    Test.ensureEqual(ddf.format(4.499999999), "4.5°", "c");
    Test.ensureEqual(ddf.format(0.251), "0.251°", "d");
    Test.ensureEqual(ddf.format(0.00125), "1.25E-3°", "e");

    Test.ensureEqual(ddf.format(-4), "-4°", "a");
    Test.ensureEqual(ddf.format(-4.500000001), "-4.5°", "b");
    Test.ensureEqual(ddf.format(-4.499999999), "-4.5°", "c");
    Test.ensureEqual(ddf.format(-0.251), "-0.251°", "d");
    Test.ensureEqual(ddf.format(-0.00125), "-1.25E-3°", "e");
  }
}
