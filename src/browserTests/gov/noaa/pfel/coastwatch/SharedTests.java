package gov.noaa.pfel.coastwatch;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;

import tags.TagRequiresContent;

class SharedTests {
  /**
   *
   * This is a simple test of Shared.
   * To use this on Bob's Windows computer, make changes to CWBrowser.properties:
   * <ul>
   * <li>change bigParentDirectory
   * <li>change pointsDir
   * <li>change localDataSetBaseDir
   * </ul>
   */
  @org.junit.jupiter.api.Test
  @TagRequiresContent // actually Browser properties, failing on bigParentDirectory
  void basicTest() throws Exception {
    long time = System.currentTimeMillis();
    OneOf oneOf = new OneOf("gov.noaa.pfel.coastwatch.CWBrowser");
    // oneOf suppresses output to screen; re start it
    String2.setupLog(true, false, "",
        true, String2.logFileDefaultMaxSize); // append
    Shared shared = new Shared(oneOf);
    shared.run();
    String[] options = shared.activeGridDataSetOptions();
    String[] name7s = shared.activeGridDataSet7Names();
    for (int i = 0; i < options.length; i++)
      String2.log(name7s[i] + " = " + String2.noLongerThanDots(options[i], 68));
    Math2.gcAndWait("Shared (between tests)");
    Math2.gcAndWait("Shared (between tests)"); // part of a test
    String2.log(Math2.memoryString() + // ~45 MB for CWBrowser
        "\nShared.test done. TIME=" +
        Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
    String2.returnLoggingToSystemOut();
    // System.exit(0); //exit when shared is alive when using
    // -agentlib:hprof=heap=sites,file=/JavaHeap.txt

  }
}
