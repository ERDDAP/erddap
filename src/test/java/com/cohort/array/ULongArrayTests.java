package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.math.BigInteger;
import java.util.BitSet;

class ULongArrayTests {
  /**
   * This tests the methods of this class.
   *
   * @throws Throwable if trouble.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("*** ULongArray.basicTest");
    ULongArray anArray;

    Test.ensureEqual(ULongArray.pack(new BigInteger("" + 0L)), 0, "");
    Test.ensureEqual(ULongArray.pack(new BigInteger("" + Long.MAX_VALUE)), Long.MAX_VALUE, "");
    Test.ensureEqual(
        ULongArray.pack(new BigInteger("" + Long.MAX_VALUE).add(BigInteger.ONE)),
        Long.MIN_VALUE,
        "");
    Test.ensureEqual(
        ULongArray.pack(new BigInteger("" + Long.MAX_VALUE).multiply(new BigInteger("2"))), -2, "");
    Test.ensureEqual(
        ULongArray.pack(
            new BigInteger("" + Long.MAX_VALUE).multiply(new BigInteger("2")).add(BigInteger.ONE)),
        -1,
        "");

    anArray = ULongArray.fromCSV("0");
    Test.ensureEqual(anArray.get(0), new BigInteger("0"), "");
    anArray = ULongArray.fromCSV("" + Long.MAX_VALUE);
    Test.ensureEqual(anArray.get(0), new BigInteger("" + Long.MAX_VALUE), "");
    anArray.add(new BigInteger("1"));
    Test.ensureEqual(anArray.get(0), new BigInteger("9223372036854775807"), "");
    anArray = ULongArray.fromCSV("" + anArray.MAXEST_VALUE());
    Test.ensureEqual(anArray.get(0), anArray.MAXEST_VALUE(), "");
    anArray = ULongArray.fromCSV("" + ULongArray.MAX_VALUE);
    Test.ensureEqual(anArray.get(0), ULongArray.MAX_VALUE, "");
    anArray = ULongArray.fromCSV("-1");
    Test.ensureEqual(anArray.get(0), null, "");
    anArray = ULongArray.fromCSV("1e30");
    Test.ensureEqual(anArray.get(0), null, "");
    anArray = ULongArray.fromCSV("NaN");
    Test.ensureEqual(anArray.get(0), null, "");

    anArray =
        new ULongArray(
            new long[] {-9223372036854775808L, -1, 0, 1, 9223372036854775807L}); // packed values
    Test.ensureEqual(
        anArray.toString(),
        "9223372036854775808, 18446744073709551615, 0, 1, 9223372036854775807",
        "");
    anArray =
        ULongArray.fromCSV(
            " 0,  1,9223372036854775807, 18446744073709551615, -1, 18446744073709551616");
    Test.ensureEqual(
        anArray.toString(),
        "0, 1, 9223372036854775807, 18446744073709551615, 18446744073709551615, 18446744073709551615",
        "");
    Test.ensureEqual(
        anArray.toNccsvAttString(),
        "0uL,1uL,9223372036854775807uL,18446744073709551615uL,18446744073709551615uL,18446744073709551615uL",
        "");
    Test.ensureEqual(
        anArray.toNccsv127AttString(),
        "0uL,1uL,9223372036854775807uL,18446744073709551615uL,18446744073709551615uL,18446744073709551615uL",
        "");

    // ** test default constructor and many of the methods
    anArray = new ULongArray();
    Test.ensureEqual(anArray.isIntegerType(), true, "");
    Test.ensureEqual(
        anArray.missingValue().getRawDouble(), ULongArray.MAX_VALUE.doubleValue(), ""); // loss of
    // precision
    anArray.addString("");
    Test.ensureEqual(anArray.get(0), null, "");
    Test.ensureEqual(anArray.getRawInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(
        anArray.getRawDouble(0), ULongArray.MAX_VALUE.doubleValue(), ""); // loss of precision
    Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getRawString(0), "" + ULongArray.MAX_VALUE, "");
    Test.ensureEqual(
        anArray.getRawNiceDouble(0), ULongArray.MAX_VALUE.doubleValue(), ""); // loss of precision
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getString(0), "", "");

    anArray.clear();

    // make from packed values
    anArray = new ULongArray(new long[] {0, 1, Long.MAX_VALUE, Long.MIN_VALUE, -2, -1}); // -1 -> mv
    Test.ensureEqual(
        anArray.toString(),
        "0, 1, 9223372036854775807, 9223372036854775808, "
            + ULongArray.MAX_VALUE.subtract(BigInteger.ONE)
            + ", 18446744073709551615",
        "");
    anArray.clear();

    Test.ensureEqual(anArray.size(), 0, "");
    anArray.addPacked(2000000000000000L);
    Test.ensureEqual(anArray.size(), 1, "");
    Test.ensureEqual(anArray.get(0), 2000000000000000L, "");
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getFloat(0), 2000000000000000.0f, "");
    Test.ensureEqual(anArray.getDouble(0), 2000000000000000L, "");
    Test.ensureEqual(anArray.getString(0), "2000000000000000", "");
    Test.ensureEqual(anArray.elementType(), PAType.ULONG, "");
    long tArray[] = anArray.toArray();
    Test.ensureEqual(tArray, new long[] {2000000000000000L}, "");

    // intentional errors
    try {
      anArray.get(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.set(1, BigInteger.ONE);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getInt(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setInt(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getULong(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setULong(1, new BigInteger("100"));
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getFloat(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setFloat(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getDouble(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setDouble(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getString(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setString(1, "100");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in ULongArray.set: index (1) >= size (1).",
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
    anArray.setULong(0, ULongArray.MAX_VALUE);
    Test.ensureEqual(anArray.getULong(0), null, "");
    anArray.setULong(0, new BigInteger("9123456789"));
    Test.ensureEqual(anArray.getULong(0), new BigInteger("9123456789"), "");
    anArray.setULong(0, new BigInteger("4"));
    Test.ensureEqual(anArray.getULong(0), new BigInteger("4"), "");
    anArray.setInt(0, Integer.MAX_VALUE);
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    anArray.setInt(0, 1123456789);
    Test.ensureEqual(anArray.getInt(0), 1123456789, "");
    anArray.setInt(0, 5);
    Test.ensureEqual(anArray.getInt(0), 5, "");

    // ** test capacity constructor, test expansion, test clear
    anArray = new ULongArray(2, false);
    Test.ensureEqual(anArray.size(), 0, "");
    for (int i = 0; i < 10; i++) {
      anArray.add(new BigInteger("" + i));
      Test.ensureEqual(anArray.get(i), i, "");
      Test.ensureEqual(anArray.size(), i + 1, "");
    }
    Test.ensureEqual(anArray.size(), 10, "");
    anArray.clear();
    Test.ensureEqual(anArray.size(), 0, "");

    // active
    anArray = new ULongArray(3, true);
    Test.ensureEqual(anArray.size(), 3, "");
    Test.ensureEqual(anArray.get(2), 0, "");

    // ** test array constructor
    anArray = new ULongArray(new long[] {0, 2, 4, 6, 8});
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
    Test.ensureEqual(stats[ULongArray.STATS_N], 5, "");
    Test.ensureEqual(stats[ULongArray.STATS_MIN], 0, "");
    Test.ensureEqual(stats[ULongArray.STATS_MAX], 8, "");
    Test.ensureEqual(stats[ULongArray.STATS_SUM], 20, "");

    // test indexOf(int) indexOf(String)
    Test.ensureEqual(anArray.indexOf(new BigInteger("0"), 0), 0, "");
    Test.ensureEqual(anArray.indexOf(new BigInteger("0"), 1), -1, "");
    Test.ensureEqual(anArray.indexOf(new BigInteger("8"), 0), 4, "");
    Test.ensureEqual(anArray.indexOf(new BigInteger("9"), 0), -1, "");
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
    anArray.atInsert(1, new BigInteger("22"));
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
    ULongArray anArray2 = new ULongArray();
    anArray2.add(BigInteger.ZERO);
    Test.ensureEqual(
        anArray.testEquals(null),
        "The two objects aren't equal: this object is a ULongArray; the other is a null.",
        "");
    Test.ensureEqual(
        anArray.testEquals("A String"),
        "The two objects aren't equal: this object is a ULongArray; the other is a java.lang.String.",
        "");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two ULongArrays aren't equal: one has 2 value(s); the other has 1 value(s).",
        "");
    Test.ensureTrue(!anArray.equals(anArray2), "");
    anArray2.addString("7");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two ULongArrays aren't equal: this[1]=8; other[1]=7.",
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
    anArray = new ULongArray(new long[] {1});
    anArray.append(new ByteArray(new byte[] {5, -5}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, Double.NaN}, "");
    anArray.append(new StringArray(new String[] {"a", "9"}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, Double.NaN, Double.NaN, 9}, "");
    anArray2 = (ULongArray) anArray.clone();
    Test.ensureEqual(anArray2.toDoubleArray(), new double[] {1, 5, Double.NaN, Double.NaN, 9}, "");

    // test move
    anArray = new ULongArray(new long[] {0, 1, 2, 3, 4});
    anArray.move(1, 3, 0);
    Test.ensureEqual(anArray.toArray(), new long[] {1, 2, 0, 3, 4}, "");

    anArray = new ULongArray(new long[] {0, 1, 2, 3, 4});
    anArray.move(1, 2, 4);
    Test.ensureEqual(anArray.toArray(), new long[] {0, 2, 3, 1, 4}, "");

    // move does nothing, but is allowed
    anArray = new ULongArray(new long[] {0, 1, 2, 3, 4});
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
    anArray = new ULongArray(new long[] {25, 1, 1, 10});
    IntArray indices = new IntArray();
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 10, 25", "");
    Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

    anArray = new ULongArray(new long[] {35, 35, ULongArray.PACKED_MAX_VALUE, 1, 2});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 2, 35, 18446744073709551615", "");
    Test.ensureEqual(indices.toString(), "2, 2, 3, 0, 1", "");

    anArray = new ULongArray(new long[] {10, 20, 30, 40});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "10, 20, 30, 40", "");
    Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

    // switchToFakeMissingValue
    anArray =
        new ULongArray(
            new long[] {
              ULongArray.PACKED_MAX_VALUE,
              1,
              2,
              ULongArray.PACKED_MAX_VALUE,
              3,
              ULongArray.PACKED_MAX_VALUE
            });
    Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
    Test.ensureEqual(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
    anArray.switchFromTo("75", "");
    Test.ensureEqual(
        anArray.toString(),
        "18446744073709551615, 1, 2, 18446744073709551615, 3, 18446744073709551615",
        "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 1, 4}, "");

    // addN
    anArray = new ULongArray(new long[] {25});
    anArray.addN(2, new BigInteger("5"));
    Test.ensureEqual(anArray.toString(), "25, 5, 5", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 2, 0}, "");

    // add array
    anArray.add(new BigInteger[] {new BigInteger("17"), new BigInteger("19")});
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
    anArray = new ULongArray(new long[] {10, 20, 30});
    Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
    anArray.set(2, new BigInteger("31"));
    Test.ensureEqual(
        anArray.isEvenlySpaced(),
        "ULongArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.5.",
        "");
    Test.ensureEqual(
        anArray.smallestBiggestSpacing(),
        "    smallest spacing=10.0: [0]=10.0, [1]=20.0\n"
            + "    biggest  spacing=11.0: [1]=20.0, [2]=31.0",
        "");

    // isAscending
    anArray = new ULongArray(new long[] {10, 10, 30});
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.set(2, ULongArray.MAX_VALUE);
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.setMaxIsMV(true);
    Test.ensureEqual(
        anArray.isAscending(),
        "ULongArray isn't sorted in ascending order: [2]=(missing value).",
        "");
    anArray.set(1, new BigInteger("9"));
    Test.ensureEqual(
        anArray.isAscending(), "ULongArray isn't sorted in ascending order: [0]=10 > [1]=9.", "");

    // isDescending
    anArray = new ULongArray(new long[] {30, 10, 10});
    Test.ensureEqual(anArray.isDescending(), "", "");
    anArray.set(2, ULongArray.MAX_VALUE);
    anArray.setMaxIsMV(true);
    Test.ensureEqual(
        anArray.isDescending(),
        "ULongArray isn't sorted in descending order: [1]=10 < [2]=18446744073709551615.",
        "");
    anArray.set(1, new BigInteger("35"));
    Test.ensureEqual(
        anArray.isDescending(),
        "ULongArray isn't sorted in descending order: [0]=30 < [1]=35.",
        "");

    // firstTie
    anArray = new ULongArray(new long[] {30, 35, 10});
    Test.ensureEqual(anArray.firstTie(), -1, "");
    anArray.set(1, new BigInteger("30"));
    Test.ensureEqual(anArray.firstTie(), 0, "");

    // hashcode
    anArray = new ULongArray();
    for (int i = 5; i < 1000; i++) anArray.add(new BigInteger("" + (i * 1000000000L)));
    String2.log("hashcode1=" + anArray.hashCode());
    anArray2 = (ULongArray) anArray.clone();
    Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
    anArray.atInsert(0, new BigInteger("4123123123"));
    Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

    // justKeep
    BitSet bitset = new BitSet();
    anArray = new ULongArray(new long[] {0, 11, 22, 33, 44});
    bitset.set(1);
    bitset.set(4);
    anArray.justKeep(bitset);
    Test.ensureEqual(anArray.toString(), "11, 44", "");

    // min max
    anArray = new ULongArray();
    anArray.addPAOne(anArray.MINEST_VALUE());
    anArray.addPAOne(anArray.MAXEST_VALUE());
    Test.ensureEqual(anArray.getString(0), anArray.MINEST_VALUE().toString(), "");
    Test.ensureEqual(anArray.getString(0), "0", "");
    Test.ensureEqual(anArray.getString(1), anArray.MAXEST_VALUE().toString(), "");
    Test.ensureEqual(anArray.getString(1), "" + ULongArray.MAX_VALUE.subtract(BigInteger.ONE), "");

    // tryToFindNumericMissingValue()
    Test.ensureEqual(new ULongArray(new long[] {}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new ULongArray(new long[] {1, 2}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new ULongArray(new long[] {0}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(
        new ULongArray(new long[] {Long.MAX_VALUE}).tryToFindNumericMissingValue(), null, "");
    anArray = new ULongArray(new BigInteger[] {ULongArray.MAX_VALUE});
    Test.ensureEqual(anArray.getMaxIsMV(), false, "");
    Test.ensureEqual(anArray.tryToFindNumericMissingValue(), "18446744073709551615", "");
    anArray.setMaxIsMV(true);
    Test.ensureEqual(anArray.tryToFindNumericMissingValue(), "18446744073709551615", "");
    Test.ensureEqual(new ULongArray(new long[] {1, 99}).tryToFindNumericMissingValue(), 99, "");

    // sort
    anArray =
        new ULongArray(
            new BigInteger[] {
              ULongArray.MAX_VALUE,
              ULongArray.MAX_VALUE.divide(new BigInteger("2")).add(BigInteger.ONE),
              new BigInteger("0"),
              new BigInteger("5"),
              ULongArray.MAX_VALUE.divide(new BigInteger("2"))
            });
    anArray.sort();
    Test.ensureEqual(
        anArray.toString(),
        "0, 5, 9223372036854775807, 9223372036854775808, 18446744073709551615",
        "");

    /* */
  }
}
