package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.BitSet;

class LongArrayTests {

  /**
   * This tests the methods of this class.
   *
   * @throws Throwable if trouble.
   */
  @org.junit.jupiter.api.Test
  public static void basicTest() throws Throwable {
    String2.log("*** LongArray.basicTest");

    // smallest long and biggest long which can round-trip to double:
    // 9007199254740992 (~9e15) see
    // https://www.mathworks.com/help/matlab/ref/flintmax.html
    // -9007199254740992 see https://www.mathworks.com/help/matlab/ref/flintmax.html

    LongArray anArray =
        LongArray.fromCSV(
            " -9223372036854775808, -1,0,  9223372036854775806,  ,                   9223372036854775807, 9999999999999999999 ");
    Test.ensureEqual(
        anArray.toString(),
        "-9223372036854775808, -1, 0, 9223372036854775806, 9223372036854775807, 9223372036854775807, 9223372036854775807",
        "");
    Test.ensureEqual(
        anArray.toNccsvAttString(),
        "-9223372036854775808L,-1L,0L,9223372036854775806L,9223372036854775807L,9223372036854775807L,9223372036854775807L",
        "");
    Test.ensureEqual(
        anArray.toNccsv127AttString(),
        "-9223372036854775808L,-1L,0L,9223372036854775806L,9223372036854775807L,9223372036854775807L,9223372036854775807L",
        "");

    // ** test default constructor and many of the methods
    anArray = new LongArray();
    Test.ensureEqual(anArray.isIntegerType(), true, "");
    Test.ensureEqual(
        anArray.missingValue().getRawDouble(),
        Long.MAX_VALUE,
        ""); // both round to same double value
    anArray.addString("");
    Test.ensureEqual(anArray.get(0), Long.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawDouble(0), (double) Long.MAX_VALUE, "");
    Test.ensureEqual(anArray.getUnsignedDouble(0), (double) Long.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawString(0), "" + Long.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawString(0), "9223372036854775807", ""); // MAX_VALUE
    Test.ensureEqual(anArray.getRawNiceDouble(0), (double) Long.MAX_VALUE, "");
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getString(0), "", "");

    anArray.set(0, Long.MIN_VALUE);
    Test.ensureEqual(anArray.getUnsignedDouble(0), 9223372036854775808.0, "");
    anArray.set(0, Long.MIN_VALUE + 1);
    Test.ensureEqual(anArray.getUnsignedDouble(0), 9223372036854775809.0, "");
    anArray.set(0, -1);
    Test.ensureEqual(anArray.getUnsignedDouble(0), 18446744073709551615.0, "");
    anArray.clear();

    Test.ensureEqual(anArray.size(), 0, "");
    anArray.add(2000000000000000L);
    Test.ensureEqual(anArray.size(), 1, "");
    Test.ensureEqual(anArray.get(0), 2000000000000000L, "");
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getFloat(0), 2000000000000000.0f, "");
    Test.ensureEqual(anArray.getDouble(0), 2000000000000000L, "");
    Test.ensureEqual(anArray.getString(0), "2000000000000000", "");
    Test.ensureEqual(anArray.elementType(), PAType.LONG, "");
    long tArray[] = anArray.toArray();
    Test.ensureEqual(tArray, new long[] {2000000000000000L}, "");

    // intentional errors
    try {
      anArray.get(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.set(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getInt(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setInt(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getLong(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setLong(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getFloat(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setFloat(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getDouble(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setDouble(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getString(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setString(1, "100");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in LongArray.set: index (1) >= size (1).",
          "");
    }

    // set NaN returned as NaN
    anArray.setDouble(0, Double.NaN);
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    anArray.setDouble(0, -1e300);
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    anArray.setDouble(0, 2.2);
    Test.ensureEqual(anArray.getDouble(0), 2, "");
    anArray.setFloat(0, Float.NaN);
    Test.ensureEqual(anArray.getFloat(0), Float.NaN, "");
    anArray.setFloat(0, -1e33f);
    Test.ensureEqual(anArray.getFloat(0), Float.NaN, "");
    anArray.setFloat(0, 3.3f);
    Test.ensureEqual(anArray.getFloat(0), 3, "");
    anArray.setLong(0, Long.MAX_VALUE);
    Test.ensureEqual(anArray.getLong(0), Long.MAX_VALUE, "");
    anArray.setLong(0, 9123456789L);
    Test.ensureEqual(anArray.getLong(0), 9123456789L, "");
    anArray.setLong(0, 4);
    Test.ensureEqual(anArray.getLong(0), 4, "");
    anArray.setInt(0, Integer.MAX_VALUE);
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    anArray.setInt(0, 1123456789);
    Test.ensureEqual(anArray.getInt(0), 1123456789, "");
    anArray.setInt(0, 5);
    Test.ensureEqual(anArray.getInt(0), 5, "");

    // ** test capacity constructor, test expansion, test clear
    anArray = new LongArray(2, false);
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
    anArray = new LongArray(3, true);
    Test.ensureEqual(anArray.size(), 3, "");
    Test.ensureEqual(anArray.get(2), 0, "");

    // ** test array constructor
    anArray = new LongArray(new long[] {0, 2, 4, 6, 8});
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
    Test.ensureEqual(anArray.toString(), "0, 2, 4, 6, 8", "");

    // test calculateStats
    anArray.addString("");
    double stats[] = anArray.calculateStats();
    anArray.remove(5);
    Test.ensureEqual(stats[LongArray.STATS_N], 5, "");
    Test.ensureEqual(stats[LongArray.STATS_MIN], 0, "");
    Test.ensureEqual(stats[LongArray.STATS_MAX], 8, "");
    Test.ensureEqual(stats[LongArray.STATS_SUM], 20, "");

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

    // test (before trimToSize) that toString, toDoubleArray, and toStringArray use
    // 'size'
    Test.ensureEqual(anArray.toString(), "0, 8", "");
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {0, 8}, "");
    Test.ensureEqual(anArray.toStringArray(), new String[] {"0", "8"}, "");

    // test trimToSize
    anArray.trimToSize();
    Test.ensureEqual(anArray.array.length, 2, "");

    // test equals
    LongArray anArray2 = new LongArray();
    anArray2.add(0);
    Test.ensureEqual(
        anArray.testEquals(null),
        "The two objects aren't equal: this object is a LongArray; the other is a null.",
        "");
    Test.ensureEqual(
        anArray.testEquals("A String"),
        "The two objects aren't equal: this object is a LongArray; the other is a java.lang.String.",
        "");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two LongArrays aren't equal: one has 2 value(s); the other has 1 value(s).",
        "");
    Test.ensureTrue(!anArray.equals(anArray2), "");
    anArray2.addString("7");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two LongArrays aren't equal: this[1]=8; other[1]=7.",
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
    anArray = new LongArray(new long[] {1});
    anArray.append(new ByteArray(new byte[] {5, -5}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, -5}, "");
    anArray.append(new StringArray(new String[] {"a", "9"}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, -5, Double.NaN, 9}, "");
    anArray2 = (LongArray) anArray.clone();
    Test.ensureEqual(anArray2.toDoubleArray(), new double[] {1, 5, -5, Double.NaN, 9}, "");

    // test move
    anArray = new LongArray(new long[] {0, 1, 2, 3, 4});
    anArray.move(1, 3, 0);
    Test.ensureEqual(anArray.toArray(), new long[] {1, 2, 0, 3, 4}, "");

    anArray = new LongArray(new long[] {0, 1, 2, 3, 4});
    anArray.move(1, 2, 4);
    Test.ensureEqual(anArray.toArray(), new long[] {0, 2, 3, 1, 4}, "");

    // move does nothing, but is allowed
    anArray = new LongArray(new long[] {0, 1, 2, 3, 4});
    anArray.move(1, 1, 0);
    Test.ensureEqual(anArray.toArray(), new long[] {0, 1, 2, 3, 4}, "");
    anArray.move(1, 2, 1);
    Test.ensureEqual(anArray.toArray(), new long[] {0, 1, 2, 3, 4}, "");
    anArray.move(1, 2, 2);
    Test.ensureEqual(anArray.toArray(), new long[] {0, 1, 2, 3, 4}, "");
    anArray.move(5, 5, 0);
    Test.ensureEqual(anArray.toArray(), new long[] {0, 1, 2, 3, 4}, "");
    anArray.move(3, 5, 5);
    Test.ensureEqual(anArray.toArray(), new long[] {0, 1, 2, 3, 4}, "");

    // makeIndices
    anArray = new LongArray(new long[] {25, 1, 1, 10});
    IntArray indices = new IntArray();
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 10, 25", "");
    Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

    anArray = new LongArray(new long[] {35, 35, Long.MAX_VALUE, 1, 2});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 2, 35, 9223372036854775807", "");
    Test.ensureEqual(indices.toString(), "2, 2, 3, 0, 1", "");

    anArray = new LongArray(new long[] {10, 20, 30, 40});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "10, 20, 30, 40", "");
    Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

    // switchToFakeMissingValue
    anArray = new LongArray(new long[] {Long.MAX_VALUE, 1, 2, Long.MAX_VALUE, 3, Long.MAX_VALUE});
    Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
    Test.ensureEqual(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
    anArray.switchFromTo("75", "");
    Test.ensureEqual(
        anArray.toString(),
        "9223372036854775807, 1, 2, 9223372036854775807, 3, 9223372036854775807",
        "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 1, 4}, "");

    // addN
    anArray = new LongArray(new long[] {25});
    anArray.addN(2, 5);
    Test.ensureEqual(anArray.toString(), "25, 5, 5", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 2, 0}, "");

    // add array
    anArray.add(new long[] {17, 19});
    Test.ensureEqual(anArray.toString(), "25, 5, 5, 17, 19", "");

    // subset
    PrimitiveArray ss = anArray.subset(1, 3, 4);
    Test.ensureEqual(ss.toString(), "5, 19", "");
    ss = anArray.subset(0, 1, 0);
    Test.ensureEqual(ss.toString(), "25", "");
    ss = anArray.subset(0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    ss = anArray.subset(1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    ss.trimToSize();
    anArray.subset(ss, 1, 3, 4);
    Test.ensureEqual(ss.toString(), "5, 19", "");
    anArray.subset(ss, 0, 1, 0);
    Test.ensureEqual(ss.toString(), "25", "");
    anArray.subset(ss, 0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    anArray.subset(ss, 1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    // evenlySpaced
    anArray = new LongArray(new long[] {10, 20, 30});
    Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
    anArray.set(2, 31);
    Test.ensureEqual(
        anArray.isEvenlySpaced(),
        "LongArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.5.",
        "");
    Test.ensureEqual(
        anArray.smallestBiggestSpacing(),
        "    smallest spacing=10.0: [0]=10.0, [1]=20.0\n"
            + "    biggest  spacing=11.0: [1]=20.0, [2]=31.0",
        "");

    // isAscending
    anArray = new LongArray(new long[] {10, 10, 30});
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.set(2, Long.MAX_VALUE);
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.setMaxIsMV(true);
    Test.ensureEqual(
        anArray.isAscending(),
        "LongArray isn't sorted in ascending order: [2]=(missing value).",
        "");
    anArray.set(1, 9);
    Test.ensureEqual(
        anArray.isAscending(), "LongArray isn't sorted in ascending order: [0]=10 > [1]=9.", "");

    // isDescending
    anArray = new LongArray(new long[] {30, 10, 10});
    Test.ensureEqual(anArray.isDescending(), "", "");
    anArray.set(2, Long.MAX_VALUE);
    anArray.setMaxIsMV(true);
    Test.ensureEqual(
        anArray.isDescending(),
        "LongArray isn't sorted in descending order: [1]=10 < [2]=9223372036854775807.",
        "");
    anArray.set(1, 35);
    Test.ensureEqual(
        anArray.isDescending(), "LongArray isn't sorted in descending order: [0]=30 < [1]=35.", "");

    // firstTie
    anArray = new LongArray(new long[] {30, 35, 10});
    Test.ensureEqual(anArray.firstTie(), -1, "");
    anArray.set(1, 30);
    Test.ensureEqual(anArray.firstTie(), 0, "");

    // hashcode
    anArray = new LongArray();
    for (int i = 5; i < 1000; i++) anArray.add(i * 1000000000L);
    String2.log("hashcode1=" + anArray.hashCode());
    anArray2 = (LongArray) anArray.clone();
    Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
    anArray.atInsert(0, 4123123123L);
    Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

    // justKeep
    BitSet bitset = new BitSet();
    anArray = new LongArray(new long[] {0, 11, 22, 33, 44});
    bitset.set(1);
    bitset.set(4);
    anArray.justKeep(bitset);
    Test.ensureEqual(anArray.toString(), "11, 44", "");

    // min max
    anArray = new LongArray();
    anArray.addPAOne(anArray.MINEST_VALUE());
    anArray.addPAOne(anArray.MAXEST_VALUE());
    Test.ensureEqual(anArray.getString(0), anArray.MINEST_VALUE().toString(), "");
    Test.ensureEqual(anArray.getString(0), "" + Long.MIN_VALUE, "");
    Test.ensureEqual(anArray.getString(1), anArray.MAXEST_VALUE().toString(), "");
    Test.ensureEqual(anArray.getString(1), "" + (Long.MAX_VALUE - 1), "");

    // tryToFindNumericMissingValue()
    Test.ensureEqual(new LongArray(new long[] {}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new LongArray(new long[] {1, 2}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(
        new LongArray(new long[] {Long.MIN_VALUE}).tryToFindNumericMissingValue(),
        Long.MIN_VALUE,
        "");
    Test.ensureEqual(
        new LongArray(new long[] {Long.MAX_VALUE}).tryToFindNumericMissingValue(),
        Long.MAX_VALUE,
        "");
    Test.ensureEqual(new LongArray(new long[] {1, 99}).tryToFindNumericMissingValue(), 99, "");
  }
}
