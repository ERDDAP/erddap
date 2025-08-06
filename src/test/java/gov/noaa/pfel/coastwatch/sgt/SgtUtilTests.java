package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import com.cohort.util.Test;

class SgtUtilTests {
  /** This tests SgtUtil. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    // test splitLine
    String2.log("\n*** SgtUtil.basicTest");
    StringArray sa = new StringArray();

    // wide
    sa.clear();
    SgtUtil.splitLine(38, sa, "This is a test of splitline.");
    Test.ensureEqual(sa.size(), 1, "");
    Test.ensureEqual(sa.get(0), "This is a test of splitline.", "");

    // narrow
    sa.clear();
    SgtUtil.splitLine(12, sa, "This is a test of splitline.");
    Test.ensureEqual(sa.size(), 3, "");
    Test.ensureEqual(sa.get(0), "This is a ", "");
    Test.ensureEqual(sa.get(1), "test of ", "");
    Test.ensureEqual(sa.get(2), "splitline.", "");

    // narrow and can't split, so chop at limit
    sa.clear();
    SgtUtil.splitLine(12, sa, "This1is2a3test4of5splitline.");
    Test.ensureEqual(sa.size(), 3, "");
    Test.ensureEqual(sa.get(0), "This1is2a3t", "");
    Test.ensureEqual(sa.get(1), "est4of5split", "");
    Test.ensureEqual(sa.get(2), "line.", "");

    // caps
    sa.clear();
    SgtUtil.splitLine(12, sa, "THESE ARE a a REALLY WIDE.");
    Test.ensureEqual(sa.size(), 3, "");
    Test.ensureEqual(sa.get(0), "THESE ARE ", "");
    Test.ensureEqual(sa.get(1), "a a REALLY ", "");
    Test.ensureEqual(sa.get(2), "WIDE.", "");
  }
}
