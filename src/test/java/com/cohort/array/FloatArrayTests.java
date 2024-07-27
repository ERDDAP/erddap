package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.BitSet;

class FloatArrayTests {

  /**
   * This tests the methods of this class.
   *
   * @throws Throwable if trouble.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("*** FloatArray.basicTest");

    float f1, f2;
    f1 = Float.NaN;
    f2 = Float.NaN;
    Test.ensureTrue(Float.compare(f1, f2) == 0, "");
    f1 = 4;
    Test.ensureTrue(Float.compare(f1, f2) < 0, "");

    FloatArray anArray =
        FloatArray.fromCSV(-Float.MAX_VALUE + ", " + Float.MAX_VALUE + ", , NaN, 1e400 ");
    Test.ensureEqual(anArray.toString(), "-3.4028235E38, 3.4028235E38, NaN, NaN, NaN", "");
    Test.ensureEqual(anArray.toNccsvAttString(), "-3.4028235E38f,3.4028235E38f,NaNf,NaNf,NaNf", "");
    Test.ensureEqual(
        anArray.toNccsv127AttString(), "-3.4028235E38f,3.4028235E38f,NaNf,NaNf,NaNf", "");

    // ** test default constructor and many of the methods
    anArray = new FloatArray();
    Test.ensureEqual(anArray.isIntegerType(), false, "");
    Test.ensureEqual(anArray.missingValue().getRawDouble(), Double.NaN, "");
    anArray.addString("");
    Test.ensureEqual(anArray.get(0), Float.NaN, "");
    Test.ensureEqual(anArray.getRawInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getRawNiceDouble(0), Float.NaN, "");
    Test.ensureEqual(anArray.getRawString(0), "", "");
    Test.ensureEqual(anArray.getRawestString(0), "NaN", "");
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getString(0), "", "");
    anArray.clear();

    anArray.add(0.1f);
    Test.ensureEqual(anArray.getDouble(0), 0.10000000149011612, "");
    Test.ensureEqual(anArray.getNiceDouble(0), 0.1, "");
    Test.ensureEqual(anArray.getRawNiceDouble(0), 0.1, "");
    anArray.clear();

    Test.ensureEqual(anArray.size(), 0, "");
    anArray.add(1e34f);
    Test.ensureEqual(anArray.size(), 1, "");
    Test.ensureEqual(anArray.get(0), 1e34f, ""); // 'f' bruises it so they match
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getFloat(0), 1e34f, ""); // 'f' defines it as a float
    Test.ensureEqual(anArray.getDouble(0), 1e34f, ""); // 'f' bruises it so they match
    Test.ensureEqual(anArray.getString(0), "1.0E34", "");
    Test.ensureEqual(anArray.elementType(), PAType.FLOAT, "");
    float tArray[] = anArray.toArray();
    Test.ensureEqual(tArray, new float[] {1e34f}, "");

    // intentional errors
    try {
      anArray.get(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.set(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getInt(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setInt(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getLong(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setLong(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getFloat(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setFloat(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getDouble(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setDouble(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getString(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setString(1, "100");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).",
          "");
    }

    // set NaN returned as NaN
    anArray.setDouble(0, Double.NaN);
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    anArray.setDouble(0, -1e300);
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    anArray.setDouble(0, 2.2);
    Test.ensureEqual(anArray.getDouble(0), 2.200000047683716, ""); // not ideal
    anArray.setFloat(0, Float.NaN);
    Test.ensureEqual(anArray.getFloat(0), Float.NaN, "");
    anArray.setFloat(0, -1e33f);
    Test.ensureEqual(anArray.getFloat(0), -1e33f, "");
    anArray.setFloat(0, 3.3f);
    Test.ensureEqual(anArray.getFloat(0), 3.3f, "");
    anArray.setLong(0, Long.MAX_VALUE);
    Test.ensureEqual(anArray.getLong(0), Long.MAX_VALUE, "");
    anArray.setLong(0, 9123456789L);
    Test.ensureEqual(anArray.getLong(0), 9123457024L, ""); // not ideal  (loss of precision)
    anArray.setLong(0, 4);
    Test.ensureEqual(anArray.getLong(0), 4, "");
    anArray.setInt(0, Integer.MAX_VALUE);
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    anArray.setInt(0, 1123456789);
    Test.ensureEqual(anArray.getInt(0), 1123456768, ""); // not ideal (loss of precision)
    anArray.setInt(0, 5);
    Test.ensureEqual(anArray.getInt(0), 5, "");

    // ** test capacity constructor, test expansion, test clear
    anArray = new FloatArray(2, false);
    Test.ensureEqual(anArray.size(), 0, "");
    for (int i = 0; i < 10; i++) {
      anArray.add(i);
      Test.ensureEqual(anArray.get(i), i, "");
      Test.ensureEqual(anArray.size(), i + 1, "");
    }
    Test.ensureEqual(anArray.size(), 10, "");
    anArray.clear();
    Test.ensureEqual(anArray.size(), 0, "");

    // active
    anArray = new FloatArray(3, true);
    Test.ensureEqual(anArray.size(), 3, "");
    Test.ensureEqual(anArray.get(2), 0, "");

    // ** test array constructor
    anArray = new FloatArray(new float[] {0, 2, 4, 6, 8});
    Test.ensureEqual(anArray.size(), 5, "");
    Test.ensureEqual(anArray.get(0), 0, "");
    Test.ensureEqual(anArray.get(1), 2, "");
    Test.ensureEqual(anArray.get(2), 4, "");
    Test.ensureEqual(anArray.get(3), 6, "");
    Test.ensureEqual(anArray.get(4), 8, "");

    // test compare
    Test.ensureEqual(anArray.compare(1, 3), -1, "");
    Test.ensureEqual(anArray.compare(1, 1), 0, "");
    Test.ensureEqual(anArray.compare(3, 1), 1, "");

    // test toString
    Test.ensureEqual(anArray.toString(), "0.0, 2.0, 4.0, 6.0, 8.0", "");

    // test calculateStats
    anArray.addString("");
    double stats[] = anArray.calculateStats();
    anArray.remove(5);
    Test.ensureEqual(stats[FloatArray.STATS_N], 5, "");
    Test.ensureEqual(stats[FloatArray.STATS_MIN], 0, "");
    Test.ensureEqual(stats[FloatArray.STATS_MAX], 8, "");
    Test.ensureEqual(stats[FloatArray.STATS_SUM], 20, "");

    // test indexOf(int) indexOf(String)
    Test.ensureEqual(anArray.indexOf(0, 0), 0, "");
    Test.ensureEqual(anArray.indexOf(0, 1), -1, "");
    Test.ensureEqual(anArray.indexOf(8, 0), 4, "");
    Test.ensureEqual(anArray.indexOf(9, 0), -1, "");

    Test.ensureEqual(anArray.indexOf("0", 0), 0, "");
    Test.ensureEqual(anArray.indexOf("0", 1), -1, "");
    Test.ensureEqual(anArray.indexOf("8", 0), 4, "");
    Test.ensureEqual(anArray.indexOf("9", 0), -1, "");

    // test remove
    anArray.remove(1);
    Test.ensureEqual(anArray.size(), 4, "");
    Test.ensureEqual(anArray.get(0), 0, "");
    Test.ensureEqual(anArray.get(1), 4, "");
    Test.ensureEqual(anArray.get(3), 8, "");

    // test atInsert(index, value)
    anArray.atInsert(1, 22);
    Test.ensureEqual(anArray.size(), 5, "");
    Test.ensureEqual(anArray.get(0), 0, "");
    Test.ensureEqual(anArray.get(1), 22, "");
    Test.ensureEqual(anArray.get(2), 4, "");
    Test.ensureEqual(anArray.get(4), 8, "");
    anArray.remove(1);

    // test removeRange
    anArray.removeRange(4, 4); // make sure it is allowed
    anArray.removeRange(1, 3);
    Test.ensureEqual(anArray.size(), 2, "");
    Test.ensureEqual(anArray.get(0), 0, "");
    Test.ensureEqual(anArray.get(1), 8, "");

    // test (before trimToSize) that toString, toDoubleArray, and toStringArray use 'size'
    Test.ensureEqual(anArray.toString(), "0.0, 8.0", "");
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {0, 8}, "");
    Test.ensureEqual(anArray.toStringArray(), new String[] {"0.0", "8.0"}, "");

    // test trimToSize
    anArray.trimToSize();
    Test.ensureEqual(anArray.array.length, 2, "");

    // test equals
    FloatArray anArray2 = new FloatArray();
    anArray2.add(0);
    Test.ensureEqual(
        anArray.testEquals(null),
        "The two objects aren't equal: this object is a FloatArray; the other is a null.",
        "");
    Test.ensureEqual(
        anArray.testEquals("A String"),
        "The two objects aren't equal: this object is a FloatArray; the other is a java.lang.String.",
        "");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two FloatArrays aren't equal: one has 2 value(s); the other has 1 value(s).",
        "");
    Test.ensureTrue(!anArray.equals(anArray2), "");
    anArray2.addString("7");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two FloatArrays aren't equal: this[1]=8.0; other[1]=7.0.",
        "");
    Test.ensureTrue(!anArray.equals(anArray2), "");
    anArray2.setString(1, "8");
    Test.ensureEqual(anArray.testEquals(anArray2), "", "");
    Test.ensureTrue(anArray.equals(anArray2), "");

    // test toObjectArray
    Test.ensureEqual(anArray.toArray(), anArray.toObjectArray(), "");

    // test toDoubleArray
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {0, 8}, "");

    // test reorder
    int rank[] = {1, 0};
    anArray.reorder(rank);
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {8, 0}, "");

    // ** test append and clone
    anArray = new FloatArray(new float[] {1});
    anArray.append(new ByteArray(new byte[] {5, -5}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, -5}, "");
    anArray.append(new StringArray(new String[] {"a", "9"}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, -5, Double.NaN, 9}, "");
    anArray2 = (FloatArray) anArray.clone();
    Test.ensureEqual(anArray2.toDoubleArray(), new double[] {1, 5, -5, Double.NaN, 9}, "");

    // test sort: ensure mv sorts high
    anArray = new FloatArray(new float[] {-1, 1, Float.NaN});
    anArray.sort();
    Test.ensureEqual(anArray.toString(), "-1.0, 1.0, NaN", "");

    // test move
    anArray = new FloatArray(new float[] {0, 1, 2, 3, 4});
    anArray.move(1, 3, 0);
    Test.ensureEqual(anArray.toArray(), new float[] {1, 2, 0, 3, 4}, "");

    anArray = new FloatArray(new float[] {0, 1, 2, 3, 4});
    anArray.move(1, 2, 4);
    Test.ensureEqual(anArray.toArray(), new float[] {0, 2, 3, 1, 4}, "");

    // move does nothing, but is allowed
    anArray = new FloatArray(new float[] {0, 1, 2, 3, 4});
    anArray.move(1, 1, 0);
    Test.ensureEqual(anArray.toArray(), new float[] {0, 1, 2, 3, 4}, "");
    anArray.move(1, 2, 1);
    Test.ensureEqual(anArray.toArray(), new float[] {0, 1, 2, 3, 4}, "");
    anArray.move(1, 2, 2);
    Test.ensureEqual(anArray.toArray(), new float[] {0, 1, 2, 3, 4}, "");
    anArray.move(5, 5, 0);
    Test.ensureEqual(anArray.toArray(), new float[] {0, 1, 2, 3, 4}, "");
    anArray.move(3, 5, 5);
    Test.ensureEqual(anArray.toArray(), new float[] {0, 1, 2, 3, 4}, "");

    // makeIndices
    anArray = new FloatArray(new float[] {25, 1, 1, 10});
    IntArray indices = new IntArray();
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "1.0, 10.0, 25.0", "");
    Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

    anArray = new FloatArray(new float[] {35, 35, Float.NaN, 1, 2});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "1.0, 2.0, 35.0, NaN", "");
    Test.ensureEqual(indices.toString(), "2, 2, 3, 0, 1", "");

    anArray = new FloatArray(new float[] {10, 20, 30, 40});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "10.0, 20.0, 30.0, 40.0", "");
    Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

    // switchFromTo   switchToFakeMissingValue
    anArray = new FloatArray(new float[] {Float.NaN, 1, 2, Float.NaN, 3, Float.NaN});
    Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
    Test.ensureEqual(anArray.toString(), "75.0, 1.0, 2.0, 75.0, 3.0, 75.0", "");
    anArray.switchFromTo("75", "");
    Test.ensureEqual(anArray.toString(), "NaN, 1.0, 2.0, NaN, 3.0, NaN", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 1, 4}, "");

    // addN
    anArray = new FloatArray(new float[] {25});
    anArray.addN(2, 5.0f);
    Test.ensureEqual(anArray.toString(), "25.0, 5.0, 5.0", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 2, 0}, "");

    // add array
    anArray.add(new float[] {17, 19});
    Test.ensureEqual(anArray.toString(), "25.0, 5.0, 5.0, 17.0, 19.0", "");

    // subset
    PrimitiveArray ss = anArray.subset(1, 3, 4);
    Test.ensureEqual(ss.toString(), "5.0, 19.0", "");
    ss = anArray.subset(0, 1, 0);
    Test.ensureEqual(ss.toString(), "25.0", "");
    ss = anArray.subset(0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    ss = anArray.subset(1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    ss.trimToSize();
    anArray.subset(ss, 1, 3, 4);
    Test.ensureEqual(ss.toString(), "5.0, 19.0", "");
    anArray.subset(ss, 0, 1, 0);
    Test.ensureEqual(ss.toString(), "25.0", "");
    anArray.subset(ss, 0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    anArray.subset(ss, 1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    // evenlySpaced
    String2.log("\nevenlySpaced test #1");
    anArray = new FloatArray(new float[] {10, 20, 30});
    Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
    String2.log("\nevenlySpaced test #2");
    anArray.set(2, 30.1f);
    Test.ensureEqual(
        anArray.isEvenlySpaced(),
        "FloatArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.05.",
        "");
    Test.ensureEqual(
        anArray.smallestBiggestSpacing(),
        "    smallest spacing=10.0: [0]=10.0, [1]=20.0\n"
            + "    biggest  spacing=10.100000381469727: [1]=20.0, [2]=30.100000381469727",
        "");

    // these are unevenly spaced, but the secondary precision test allows it
    // should fail first test, but pass second test
    String2.log("\nevenlySpaced test #3");
    anArray = new FloatArray(new float[] {1.2306f, 1.2307f, 1.230801f});
    Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
    // but this should fail first and second test
    String2.log("\nevenlySpaced test #4");
    anArray.set(2, 1.23081f);
    Test.ensureEqual(
        anArray.isEvenlySpaced(),
        "FloatArray isn't evenly spaced: [0]=1.2306, [1]=1.2307, spacing=1.00016594E-4, average spacing=1.05023384E-4.",
        "");

    // isAscending
    anArray = new FloatArray(new float[] {10, 10, 30});
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.set(2, Float.NaN);
    Test.ensureEqual(
        anArray.isAscending(),
        "FloatArray isn't sorted in ascending order: [2]=(missing value).",
        "");
    anArray.set(1, 9);
    Test.ensureEqual(
        anArray.isAscending(),
        "FloatArray isn't sorted in ascending order: [0]=10.0 > [1]=9.0.",
        "");

    // isDescending
    anArray = new FloatArray(new float[] {30, 10, 10});
    Test.ensureEqual(anArray.isDescending(), "", "");
    anArray.set(2, Float.NaN);
    Test.ensureEqual(
        anArray.isDescending(),
        "FloatArray isn't sorted in descending order: [1]=10.0 < [2]=NaN.",
        "");
    anArray.set(1, 35f);
    Test.ensureEqual(
        anArray.isDescending(),
        "FloatArray isn't sorted in descending order: [0]=30.0 < [1]=35.0.",
        "");

    // firstTie
    anArray = new FloatArray(new float[] {30, 35, 10});
    Test.ensureEqual(anArray.firstTie(), -1, "");
    anArray.set(1, 30f);
    Test.ensureEqual(anArray.firstTie(), 0, "");
    anArray.set(1, Float.NaN);
    Test.ensureEqual(anArray.firstTie(), -1, "");
    anArray.set(2, Float.NaN);
    Test.ensureEqual(anArray.firstTie(), 1, "");

    // hashcode
    anArray = new FloatArray();
    for (int i = 5; i < 1000; i++) anArray.add(i / 100.0f);
    String2.log("hashcode1=" + anArray.hashCode());
    anArray2 = (FloatArray) anArray.clone();
    Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
    anArray.atInsert(0, (float) 2);
    Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

    // justKeep
    BitSet bitset = new BitSet();
    anArray = new FloatArray(new float[] {0, 11, 22, 33, 44});
    bitset.set(1);
    bitset.set(4);
    anArray.justKeep(bitset);
    Test.ensureEqual(anArray.toString(), "11.0, 44.0", "");

    // min max
    anArray = new FloatArray();
    anArray.addPAOne(anArray.MINEST_VALUE());
    anArray.addPAOne(anArray.MAXEST_VALUE());
    Test.ensureEqual(anArray.getString(0), anArray.MINEST_VALUE().toString(), "");
    Test.ensureEqual(anArray.getString(0), "-3.4028235E38", "");
    Test.ensureEqual(anArray.getString(1), anArray.MAXEST_VALUE().toString(), "");
    Test.ensureEqual(anArray.getString(1), "3.4028235E38", "");

    // tryToFindNumericMissingValue()
    Test.ensureEqual(new FloatArray(new float[] {}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new FloatArray(new float[] {1, 2}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(
        new FloatArray(new float[] {-1e37f}).tryToFindNumericMissingValue(), -1e37f, "");
    Test.ensureEqual(new FloatArray(new float[] {1e37f}).tryToFindNumericMissingValue(), 1e37f, "");
    Test.ensureEqual(new FloatArray(new float[] {1, 99}).tryToFindNumericMissingValue(), 99, "");
  }
}
