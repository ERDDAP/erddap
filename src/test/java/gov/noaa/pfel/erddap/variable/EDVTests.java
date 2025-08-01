package gov.noaa.pfel.erddap.variable;

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
    Test.ensureEqual(EDV.suggestLongName("real-time temp", "rt", null), "Real-time Temp", "");
    Test.ensureEqual(EDV.suggestLongName("real_time_temp", "rt", null), "Real Time Temp", "");
    Test.ensureEqual(EDV.suggestLongName("real.time.temp", "rt", null), "Real.time.temp", "");
    Test.ensureEqual(EDV.suggestLongName("RealTimeTemp", "rt", null), "Real Time Temp", "");
    Test.ensureEqual(EDV.suggestLongName(null, "rhum", null), "Relative Humidity", "");
  }
}
