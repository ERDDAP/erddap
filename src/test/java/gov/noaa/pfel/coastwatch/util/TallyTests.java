package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.Test;

class TallyTests {
  /**
   * This tests Tally.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() {
    Tally tally = new Tally();
    tally.add("cat a", "att 2");
    tally.add("cat a", "att 2");
    tally.add("cat b", "att 2");
    tally.add("cat a", "att 2");
    tally.add("cat a", "att 1");
    tally.add("cat b", "att 1");
    tally.add("cat c", "att 3");
    String s = tally.toString();
    Test.ensureEqual(
        s,
        "cat a\n"
            + "    att 2: 3  (75%)\n"
            + // sorted by count
            "    att 1: 1  (25%)\n"
            + "\n"
            + "cat b\n"
            + "    att 1: 1  (50%)\n"
            + // tied count; sort by attName
            "    att 2: 1  (50%)\n"
            + "\n"
            + "cat c\n"
            + "    att 3: 1  (100%)\n"
            + "\n",
        "");
  }
}
