package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.BitSet;

class UByteArrayTests {
  /**
   * This tests the methods of this class.
   *
   * @throws Throwable if trouble.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("*** UByteArray.basicTest");

    byte b;
    short s;
    b = 0;
    Test.ensureEqual(0, b & 255, "");
    b = 127;
    Test.ensureEqual(127, b & 255, "");
    b = -128;
    Test.ensureEqual(128, b & 255, "");
    b = -1;
    Test.ensureEqual(255, b & 255, "");
    Test.ensureEqual("" + PAType.STRING, "STRING", "");

    Test.ensureEqual(UByteArray.pack((short) 0), 0, "");
    Test.ensureEqual(UByteArray.pack((short) 127), 127, "");
    Test.ensureEqual(UByteArray.pack((short) 128), -128, "");
    Test.ensureEqual(UByteArray.pack((short) 254), -2, "");
    Test.ensureEqual(UByteArray.pack((short) 255), -1, "");

    Test.ensureEqual(UByteArray.unpack((byte) 0), 0, "");
    Test.ensureEqual(UByteArray.unpack((byte) 127), 127, "");
    Test.ensureEqual(UByteArray.unpack((byte) -128), 128, "");
    Test.ensureEqual(UByteArray.unpack((byte) -2), 254, "");
    Test.ensureEqual(UByteArray.unpack((byte) -1), 255, "");

    UByteArray anArray = new UByteArray(new byte[] {-128, -1, 0, 1, 127}); // packed values
    Test.ensureEqual(anArray.toString(), "128, 255, 0, 1, 127", "");
    anArray = UByteArray.fromCSV("0, 1, 127, 128, 255,  -1, 999");
    Test.ensureEqual(anArray.toString(), "0, 1, 127, 128, 255, 255, 255", "");
    Test.ensureEqual(anArray.toNccsvAttString(), "0ub,1ub,127ub,128ub,255ub,255ub,255ub", "");
    Test.ensureEqual(anArray.toNccsv127AttString(), "0ub,1ub,127ub,128ub,255ub,255ub,255ub", "");

    // ** test default constructor and many of the methods
    anArray.clear();
    Test.ensureEqual(anArray.isIntegerType(), true, "");
    Test.ensureEqual(anArray.missingValue().getRawDouble(), UByteArray.MAX_VALUE, "");
    anArray.addString("");
    Test.ensureEqual(anArray.get(0), UByteArray.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawInt(0), UByteArray.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawDouble(0), UByteArray.MAX_VALUE, "");
    Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getRawString(0), "" + UByteArray.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawNiceDouble(0), UByteArray.MAX_VALUE, "");
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getString(0), "", "");

    anArray.set(0, (short) 0);
    Test.ensureEqual(anArray.getUnsignedDouble(0), 0, "");
    anArray.set(0, (short) 127);
    Test.ensureEqual(anArray.getUnsignedDouble(0), 127, "");
    anArray.set(0, (short) 128);
    Test.ensureEqual(anArray.getUnsignedDouble(0), 128, "");
    anArray.set(0, (short) 254);
    Test.ensureEqual(anArray.getUnsignedDouble(0), 254, "");
    anArray.set(0, (short) 255);
    Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN, "");
    anArray.clear();

    Test.ensureEqual(anArray.size(), 0, "");
    anArray.add((byte) 120);
    Test.ensureEqual(anArray.size(), 1, "");
    Test.ensureEqual(anArray.get(0), 120, "");
    Test.ensureEqual(anArray.getInt(0), 120, "");
    Test.ensureEqual(anArray.getFloat(0), 120, "");
    Test.ensureEqual(anArray.getDouble(0), 120, "");
    Test.ensureEqual(anArray.getString(0), "120", "");
    Test.ensureEqual(anArray.elementType(), PAType.UBYTE, "");
    byte tArray[] = anArray.toArray();
    Test.ensureEqual(tArray, new byte[] {(byte) 120}, "");

    // intentional errors
    try {
      anArray.get(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.set(1, (byte) 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getInt(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setInt(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getLong(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setLong(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getFloat(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setFloat(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getDouble(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setDouble(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getString(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setString(1, "100");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in UByteArray.set: index (1) >= size (1).",
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
    Test.ensureEqual(anArray.getLong(0), Long.MAX_VALUE, "");
    anArray.setLong(0, 4);
    Test.ensureEqual(anArray.getLong(0), 4, "");
    anArray.setInt(0, Integer.MAX_VALUE);
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    anArray.setInt(0, 1123456789);
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    anArray.setInt(0, 5);
    Test.ensureEqual(anArray.getInt(0), 5, "");

    // ** test capacity constructor, test expansion, test clear
    anArray = new UByteArray(2, false);
    Test.ensureEqual(anArray.size(), 0, "");
    for (int i = 0; i < 10; i++) {
      anArray.add((byte) i);
      Test.ensureEqual(anArray.get(i), i, "");
      Test.ensureEqual(anArray.size(), i + 1, "");
    }
    Test.ensureEqual(anArray.size(), 10, "");
    anArray.clear();
    Test.ensureEqual(anArray.size(), 0, "");

    // active
    anArray = new UByteArray(3, true);
    Test.ensureEqual(anArray.size(), 3, "");
    Test.ensureEqual(anArray.get(2), 0, "");

    // ** test array constructor
    anArray = new UByteArray(new byte[] {0, 2, 4, 6, 8});
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
    Test.ensureEqual(stats[UByteArray.STATS_N], 5, "");
    Test.ensureEqual(stats[UByteArray.STATS_MIN], 0, "");
    Test.ensureEqual(stats[UByteArray.STATS_MAX], 8, "");
    Test.ensureEqual(stats[UByteArray.STATS_SUM], 20, "");

    // test indexOf(int) indexOf(String)
    Test.ensureEqual(anArray.indexOf((byte) 0, 0), 0, "");
    Test.ensureEqual(anArray.indexOf((byte) 0, 1), -1, "");
    Test.ensureEqual(anArray.indexOf((byte) 8, 0), 4, "");
    Test.ensureEqual(anArray.indexOf((byte) 9, 0), -1, "");

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
    anArray.atInsert(1, (byte) 22);
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
    UByteArray anArray2 = new UByteArray();
    anArray2.add((byte) 0);
    Test.ensureEqual(
        anArray.testEquals(null),
        "The two objects aren't equal: this object is a UByteArray; the other is a null.",
        "");
    Test.ensureEqual(
        anArray.testEquals("A String"),
        "The two objects aren't equal: this object is a UByteArray; the other is a java.lang.String.",
        "");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two UByteArrays aren't equal: one has 2 value(s); the other has 1 value(s).",
        "");
    Test.ensureTrue(!anArray.equals(anArray2), "");
    anArray2.addString("7");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two UByteArrays aren't equal: this[1]=8; other[1]=7.",
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
    anArray = new UByteArray(new byte[] {1});
    anArray.append(new ByteArray(new byte[] {5, -5}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, Double.NaN}, "");
    anArray.append(new StringArray(new String[] {"a", "9"}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, Double.NaN, Double.NaN, 9}, "");
    anArray2 = (UByteArray) anArray.clone();
    Test.ensureEqual(anArray2.toDoubleArray(), new double[] {1, 5, Double.NaN, Double.NaN, 9}, "");

    // test move
    anArray = new UByteArray(new byte[] {0, 1, 2, 3, 4});
    anArray.move(1, 3, 0);
    Test.ensureEqual(anArray.toArray(), new byte[] {1, 2, 0, 3, 4}, "");

    anArray = new UByteArray(new byte[] {0, 1, 2, 3, 4});
    anArray.move(1, 2, 4);
    Test.ensureEqual(anArray.toArray(), new byte[] {0, 2, 3, 1, 4}, "");

    // move does nothing, but is allowed
    anArray = new UByteArray(new byte[] {0, 1, 2, 3, 4});
    anArray.move(1, 1, 0);
    Test.ensureEqual(anArray.toArray(), new byte[] {0, 1, 2, 3, 4}, "");
    anArray.move(1, 2, 1);
    Test.ensureEqual(anArray.toArray(), new byte[] {0, 1, 2, 3, 4}, "");
    anArray.move(1, 2, 2);
    Test.ensureEqual(anArray.toArray(), new byte[] {0, 1, 2, 3, 4}, "");
    anArray.move(5, 5, 0);
    Test.ensureEqual(anArray.toArray(), new byte[] {0, 1, 2, 3, 4}, "");
    anArray.move(3, 5, 5);
    Test.ensureEqual(anArray.toArray(), new byte[] {0, 1, 2, 3, 4}, "");

    // makeIndices
    anArray = new UByteArray(new byte[] {25, 1, 1, 10});
    IntArray indices = new IntArray();
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 10, 25", "");
    Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

    anArray = new UByteArray(new short[] {35, 35, UByteArray.MAX_VALUE, 1, 2});
    indices = new IntArray();
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 2, 35, 255", "");
    Test.ensureEqual(indices.toString(), "2, 2, 3, 0, 1", "");

    anArray = new UByteArray(new byte[] {10, 20, 30, 40});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "10, 20, 30, 40", "");
    Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

    // switchToFakeMissingValue
    anArray =
        new UByteArray(
            new short[] {
              UByteArray.MAX_VALUE, 1, 2, UByteArray.MAX_VALUE, 3, UByteArray.MAX_VALUE
            });
    Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
    Test.ensureEqual(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
    anArray.switchFromTo("75", "");
    Test.ensureEqual(anArray.toString(), "255, 1, 2, 255, 3, 255", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 1, 4}, "");

    // addN
    anArray = new UByteArray(new byte[] {25});
    anArray.addN(2, (byte) 5);
    Test.ensureEqual(anArray.toString(), "25, 5, 5", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 2, 0}, "");

    // add array
    anArray.add(new byte[] {17, 19});
    Test.ensureEqual(anArray.toString(), "25, 5, 5, 17, 19", "");
    Test.ensureEqual(anArray.getClass().getName(), "com.cohort.array.UByteArray", "");

    // subset
    PrimitiveArray ss = anArray.subset(1, 3, 4);
    Test.ensureEqual(ss.toString(), "5, 19", "");
    ss = anArray.subset(0, 1, 0);
    Test.ensureEqual(ss.getClass().getName(), "com.cohort.array.UByteArray", "");
    Test.ensureEqual(ss.toString(), "25", "");
    ss = anArray.subset(0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    ss = anArray.subset(1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    ss.trimToSize();
    Test.ensureEqual(ss.getClass().getName(), "com.cohort.array.UByteArray", "");
    anArray.subset(ss, 1, 3, 4);
    Test.ensureEqual(ss.toString(), "5, 19", "");
    anArray.subset(ss, 0, 1, 0);
    Test.ensureEqual(ss.toString(), "25", "");
    anArray.subset(ss, 0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    anArray.subset(ss, 1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    // evenlySpaced
    anArray = new UByteArray(new byte[] {10, 20, 30});
    Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
    anArray.set(2, (byte) 31);
    Test.ensureEqual(
        anArray.isEvenlySpaced(),
        "UByteArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.5.",
        "");
    Test.ensureEqual(
        anArray.smallestBiggestSpacing(),
        "    smallest spacing=10.0: [0]=10.0, [1]=20.0\n"
            + "    biggest  spacing=11.0: [1]=20.0, [2]=31.0",
        "");

    // isAscending
    anArray = new UByteArray(new byte[] {10, 10, 30});
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.set(2, UByteArray.MAX_VALUE);
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.setMaxIsMV(true);
    Test.ensureEqual(
        anArray.isAscending(),
        "UByteArray isn't sorted in ascending order: [2]=(missing value).",
        "");
    anArray.set(1, (byte) 9);
    Test.ensureEqual(
        anArray.isAscending(), "UByteArray isn't sorted in ascending order: [0]=10 > [1]=9.", "");

    // isDescending
    anArray = new UByteArray(new byte[] {30, 10, 10});
    Test.ensureEqual(anArray.isDescending(), "", "");
    anArray.set(2, UByteArray.MAX_VALUE);
    anArray.setMaxIsMV(true);
    Test.ensureEqual(
        anArray.isDescending(),
        "UByteArray isn't sorted in descending order: [1]=10 < [2]=255.",
        "");
    anArray.set(1, (byte) 35);
    Test.ensureEqual(
        anArray.isDescending(),
        "UByteArray isn't sorted in descending order: [0]=30 < [1]=35.",
        "");

    // firstTie
    anArray = new UByteArray(new byte[] {30, 35, 10});
    Test.ensureEqual(anArray.firstTie(), -1, "");
    anArray.set(1, (byte) 30);
    Test.ensureEqual(anArray.firstTie(), 0, "");

    // hashcode
    anArray = new UByteArray();
    for (int i = 5; i < 1000; i++) anArray.add((byte) i);
    String2.log("hashcode1=" + anArray.hashCode());
    anArray2 = (UByteArray) anArray.clone();
    Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
    anArray.atInsert(0, (byte) 2);
    Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

    // justKeep
    BitSet bitset = new BitSet();
    anArray = new UByteArray(new byte[] {0, 11, 22, 33, 44});
    bitset.set(1);
    bitset.set(4);
    anArray.justKeep(bitset);
    Test.ensureEqual(anArray.toString(), "11, 44", "");

    // min max
    anArray = new UByteArray();
    anArray.addPAOne(anArray.MINEST_VALUE());
    anArray.addPAOne(anArray.MAXEST_VALUE());
    Test.ensureEqual(anArray.getString(0), anArray.MINEST_VALUE().toString(), "");
    Test.ensureEqual(anArray.getString(0), "0", "");
    Test.ensureEqual(anArray.getString(1), anArray.MAXEST_VALUE().toString(), "");
    Test.ensureEqual(anArray.getString(1), "254", "");

    // tryToFindNumericMissingValue()
    Test.ensureEqual(new UByteArray(new byte[] {}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new UByteArray(new byte[] {1, 2}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new UByteArray(new byte[] {-128}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new UByteArray(new byte[] {127}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new UByteArray(new short[] {255}).tryToFindNumericMissingValue(), 255, "");
    Test.ensureEqual(new UByteArray(new byte[] {1, 99}).tryToFindNumericMissingValue(), 99, "");

    // sort
    anArray = new UByteArray(new short[] {255, 128, 0, 5, 127});
    anArray.sort();
    Test.ensureEqual(anArray.toString(), "0, 5, 127, 128, 255", "");

    /* */
  }
}
