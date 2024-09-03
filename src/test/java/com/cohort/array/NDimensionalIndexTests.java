package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;

class NDimensionalIndexTests {
  /** This tests the subsetIndex system. */
  @org.junit.jupiter.api.Test
  void testSubsetIndex() {
    String2.log("\n*** NDimensionalIndex.testSubsetIndex");

    // test get all
    NDimensionalIndex index = new NDimensionalIndex(new int[] {4, 5});
    int current[] = index.getCurrent();
    IntArray tConstraints = IntArray.fromCSV("0, 1, 3, 0, 1, 4"); // get all
    int subsetIndex[] = index.makeSubsetIndex("myVarName", tConstraints);
    Test.ensureEqual(index.willGetAllValues(tConstraints), true, "");
    for (int i = 0; i < 20; i++) {
      Test.ensureEqual(index.increment(), true, "i=" + i);
      Test.ensureEqual(current, subsetIndex, "i=" + i);

      Test.ensureEqual(index.incrementSubsetIndex(subsetIndex, tConstraints), i < 19, "i=" + i);
    }
    Test.ensureEqual(index.increment(), false, "at the end");

    // test get subset
    index.reset();
    tConstraints =
        IntArray.fromCSV("1, 2, 3, 0, 3, 4"); // not that 2nd stop is beyond last matching value
    subsetIndex = index.makeSubsetIndex("myVarName", tConstraints);
    Test.ensureEqual(index.willGetAllValues(tConstraints), false, "");
    StringBuilder results = new StringBuilder();
    for (int i = 0; i < 20; i++) {
      Test.ensureEqual(index.increment(), true, "i=" + i);

      if (Test.testEqual(String2.toCSSVString(current), String2.toCSSVString(subsetIndex), "msg")
          .equals("")) {
        results.append("equal at i=" + i + " current=" + String2.toCSSVString(current) + "\n");
        if (!index.incrementSubsetIndex(subsetIndex, tConstraints)) {
          results.append("done at i=" + i + "\n");
          break;
        }
      }
    }
    String expected =
        "equal at i=5 current=1, 0\n"
            + "equal at i=8 current=1, 3\n"
            + "equal at i=15 current=3, 0\n"
            + "equal at i=18 current=3, 3\n"
            + "done at i=18\n";
    Test.ensureEqual(results.toString(), expected, "results=\n" + results.toString());

    // test get subset
    index.reset();
    tConstraints = IntArray.fromCSV("0, 3, 3, 1, 2, 4");
    subsetIndex = index.makeSubsetIndex("myVarName", tConstraints);
    Test.ensureEqual(index.willGetAllValues(tConstraints), false, "");
    results = new StringBuilder();
    for (int i = 0; i < 20; i++) {
      Test.ensureEqual(index.increment(), true, "i=" + i);

      if (Test.testEqual(String2.toCSSVString(current), String2.toCSSVString(subsetIndex), "msg")
          .equals("")) {
        results.append("equal at i=" + i + " current=" + String2.toCSSVString(current) + "\n");
        if (!index.incrementSubsetIndex(subsetIndex, tConstraints)) {
          results.append("done at i=" + i + "\n");
          break;
        }
      }
    }
    expected =
        "equal at i=1 current=0, 1\n"
            + "equal at i=3 current=0, 3\n"
            + "equal at i=16 current=3, 1\n"
            + "equal at i=18 current=3, 3\n"
            + "done at i=18\n";
    Test.ensureEqual(results.toString(), expected, "results=\n" + results.toString());
  }

  /**
   * This tests this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("*** NDimensionalIndex.basicTest");

    // test increment
    NDimensionalIndex a = new NDimensionalIndex(new int[] {2, 2, 3});
    Test.ensureEqual(a.size, 12, "");

    Test.ensureTrue(a.increment(), "0");
    Test.ensureEqual(a.getIndex(), 0, "0");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 0, 0}, "0");

    Test.ensureTrue(a.increment(), "1");
    Test.ensureEqual(a.getIndex(), 1, "1");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 0, 1}, "1");

    Test.ensureTrue(a.increment(), "2");
    Test.ensureEqual(a.getIndex(), 2, "2");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 0, 2}, "2");

    Test.ensureTrue(a.increment(), "3");
    Test.ensureEqual(a.getIndex(), 3, "3");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 1, 0}, "3");

    Test.ensureTrue(a.increment(), "4");
    Test.ensureEqual(a.getIndex(), 4, "4");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 1, 1}, "4");

    Test.ensureTrue(a.increment(), "5");
    Test.ensureEqual(a.getIndex(), 5, "5");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 1, 2}, "5");

    Test.ensureTrue(a.increment(), "6");
    Test.ensureEqual(a.getIndex(), 6, "6");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 0, 0}, "6");

    Test.ensureTrue(a.increment(), "7");
    Test.ensureEqual(a.getIndex(), 7, "7");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 0, 1}, "7");

    Test.ensureTrue(a.increment(), "8");
    Test.ensureEqual(a.getIndex(), 8, "8");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 0, 2}, "8");

    Test.ensureTrue(a.increment(), "9");
    Test.ensureEqual(a.getIndex(), 9, "9");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 1, 0}, "9");

    Test.ensureTrue(a.increment(), "10");
    Test.ensureEqual(a.getIndex(), 10, "10");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 1, 1}, "10");

    Test.ensureTrue(a.increment(), "11");
    Test.ensureEqual(a.getIndex(), 11, "11");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 1, 2}, "11");

    Test.ensureTrue(!a.increment(), "12");
    Test.ensureEqual(a.getIndex(), 12, "12");

    a.reset();
    Test.ensureTrue(a.increment(), "0");
    Test.ensureEqual(a.getIndex(), 0, "0");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 0, 0}, "0");

    // test incrementCM //shape = 2,2,3 factors 6,3,1
    a.reset();
    Test.ensureTrue(a.incrementCM(), "0");
    Test.ensureEqual(a.getIndex(), 0, "0");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 0, 0}, "0");

    Test.ensureTrue(a.incrementCM(), "1");
    Test.ensureEqual(a.getIndex(), 6, "1");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 0, 0}, "1");

    Test.ensureTrue(a.incrementCM(), "2");
    Test.ensureEqual(a.getIndex(), 3, "2");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 1, 0}, "2");

    Test.ensureTrue(a.incrementCM(), "3");
    Test.ensureEqual(a.getIndex(), 9, "3");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 1, 0}, "3");

    Test.ensureTrue(a.incrementCM(), "4");
    Test.ensureEqual(a.getIndex(), 1, "4");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 0, 1}, "4");

    Test.ensureTrue(a.incrementCM(), "5");
    Test.ensureEqual(a.getIndex(), 7, "5");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 0, 1}, "5");

    Test.ensureTrue(a.incrementCM(), "6");
    Test.ensureEqual(a.getIndex(), 4, "6");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 1, 1}, "6");

    Test.ensureTrue(a.incrementCM(), "7");
    Test.ensureEqual(a.getIndex(), 10, "7");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 1, 1}, "7");

    Test.ensureTrue(a.incrementCM(), "8");
    Test.ensureEqual(a.getIndex(), 2, "8");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 0, 2}, "8");

    Test.ensureTrue(a.incrementCM(), "9");
    Test.ensureEqual(a.getIndex(), 8, "9");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 0, 2}, "9");

    Test.ensureTrue(a.incrementCM(), "10");
    Test.ensureEqual(a.getIndex(), 5, "10");
    Test.ensureEqual(a.getCurrent(), new int[] {0, 1, 2}, "10");

    Test.ensureTrue(a.incrementCM(), "11");
    Test.ensureEqual(a.getIndex(), 11, "11");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 1, 2}, "11");

    Test.ensureTrue(!a.incrementCM(), "12");
    Test.ensureEqual(a.getIndex(), 12, "12");

    // test some invalid requests
    try {
      NDimensionalIndex b = new NDimensionalIndex(new int[0]);
      throw new Exception("");
    } catch (Exception e) {
      if (e.toString().indexOf("nDimensions=0") < 0) throw e;
    }
    try {
      NDimensionalIndex b = new NDimensionalIndex(new int[] {2, 0});
      throw new Exception("");
    } catch (Exception e) {
      if (e.toString().indexOf("value less than 1") < 0) throw e;
    }

    try {
      a.setIndex(-1);
      throw new Exception("");
    } catch (Exception e) {
      if (e.toString().indexOf("less than 0") < 0) throw e;
    }
    try {
      a.setCurrent(new int[] {1, 1});
      throw new Exception("");
    } catch (Exception e) {
      if (e.toString().indexOf("isn't 3") < 0) throw e;
    }

    try {
      a.setCurrent(new int[] {1, -1, 1});
      throw new Exception("");
    } catch (Exception e) {
      if (e.toString().indexOf("is invalid") < 0) throw e;
    }

    // test get/set index/current
    Test.ensureEqual(a.setCurrent(new int[] {1, 1, 2}), 11, "");
    Test.ensureEqual(a.getIndex(), 11, "");
    Test.ensureEqual(a.setIndex(9), new int[] {1, 1, 0}, "");
    Test.ensureEqual(a.getCurrent(), new int[] {1, 1, 0}, "");
  }
}
