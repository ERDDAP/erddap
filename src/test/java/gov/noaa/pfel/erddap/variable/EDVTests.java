package gov.noaa.pfel.erddap.variable;

import com.cohort.util.String2;
import com.cohort.util.Test;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class EDVTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests the methods of this class.
   *
   * @throws Throwable if trouble.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("\n*** EDV.test()");
    Test.ensureEqual(EDV.toDecimalDegrees("1.1W"), -1.1, "");
    Test.ensureEqual(EDV.toDecimalDegrees("2.2E"), 2.2, "");
    Test.ensureEqual(EDV.toDecimalDegrees("3.3S"), -3.3, "");
    Test.ensureEqual(EDV.toDecimalDegrees("4.4N"), 4.4, "");
    Test.ensureEqual(EDV.toDecimalDegrees("1°2.3'W"), -(1 + 2.3 / 60.0), "");
    Test.ensureEqual(EDV.toDecimalDegrees("4°5.6'"), 4 + 5.6 / 60.0, "");
    Test.ensureEqual(EDV.toDecimalDegrees("1°2'3.4\"S"), -(1 + 2 / 60.0 + 3.4 / 3600.0), "");
    Test.ensureEqual(EDV.toDecimalDegrees("4°5'6.7\""), 4 + 5 / 60.0 + 6.7 / 3600.0, "");

    Test.ensureEqual(EDV.suggestLongName("real-time temp", "rt", null), "Real-time Temp", "");
    Test.ensureEqual(EDV.suggestLongName("real_time_temp", "rt", null), "Real Time Temp", "");
    Test.ensureEqual(EDV.suggestLongName("real.time.temp", "rt", null), "Real.time.temp", "");
    Test.ensureEqual(EDV.suggestLongName("RealTimeTemp", "rt", null), "Real Time Temp", "");
    Test.ensureEqual(EDV.suggestLongName(null, "rhum", null), "Relative Humidity", "");
  }
}
