package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.Test;

class DegreeMinuteFormatterTests {
  /**
   * This tests the methods in this class.
   *
   * @param args is ignored
   */
  @org.junit.jupiter.api.Test
  void basicTest() {
    DegreeMinuteFormatter dmf = new DegreeMinuteFormatter();
    Test.ensureEqual(dmf.format(4), "4°", "a");
    Test.ensureEqual(dmf.format(4.501), "4°30'", "b");
    Test.ensureEqual(dmf.format(4.499), "4°30'", "c");
    Test.ensureEqual(dmf.format(0.251), "0°15'", "d");
    Test.ensureEqual(dmf.format(0.001), "0°", "e");

    Test.ensureEqual(dmf.format(-4), "-4°", "j");
    Test.ensureEqual(dmf.format(-4.501), "-4°30'", "k");
    Test.ensureEqual(dmf.format(-4.499), "-4°30'", "l");
    Test.ensureEqual(dmf.format(-0.251), "-0°15'", "m");
    Test.ensureEqual(dmf.format(-0.001), "0°", "n");
  }
}
