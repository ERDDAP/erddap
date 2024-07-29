package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.BitSet;

class CharArrayTests {
  /**
   * This tests the methods of this class.
   *
   * @throws Throwable if trouble.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("*** CharArray.basicTest");

    CharArray anArray = CharArray.fromCSV("\"\\t\", a, \"\\n\", \"\\u20AC\", ,  \"\\uffff\" ");
    Test.ensureEqual(anArray.toString(), "\\t, a, \\n, \\u20ac, \\uffff, \\uffff", "");
    Test.ensureEqual(
        anArray.toNccsvAttString(),
        "\"'\\t'\",\"'a'\",\"'\\n'\",\"'\u20ac'\",\"'\uffff'\",\"'\uffff'\"",
        "");
    Test.ensureEqual(
        anArray.toNccsv127AttString(),
        "\"'\\t'\",\"'a'\",\"'\\n'\",\"'\\u20ac'\",\"'\\uffff'\",\"'\\uffff'\"",
        "");

    // ** test default constructor and many of the methods
    anArray = new CharArray();
    Test.ensureEqual(anArray.isIntegerType(), false, "");
    Test.ensureEqual(anArray.missingValue().getRawDouble(), 65535, "");
    anArray.addString("");
    Test.ensureEqual(anArray.get(0), (char) 65535, "");
    Test.ensureEqual(anArray.getRawInt(0), 65535, "");
    Test.ensureEqual(anArray.getRawDouble(0), 65535, "");
    Test.ensureEqual(anArray.getUnsignedDouble(0), 65535, "");
    Test.ensureEqual(anArray.getRawString(0), "\uFFFF", "");
    Test.ensureEqual(anArray.getRawNiceDouble(0), 65535, "");
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getString(0), "", "");
    anArray.clear();

    Test.ensureEqual(anArray.size(), 0, "");
    anArray.add('z');
    Test.ensureEqual(anArray.size(), 1, "");
    Test.ensureEqual(anArray.get(0), 'z', "");
    Test.ensureEqual(anArray.getInt(0), 122, "");
    Test.ensureEqual(anArray.getFloat(0), 122, "");
    Test.ensureEqual(anArray.getDouble(0), 122, "");
    Test.ensureEqual(anArray.getString(0), "z", "");
    Test.ensureEqual(anArray.elementType(), PAType.CHAR, "");
    char tArray[] = anArray.toArray();
    Test.ensureEqual(tArray, new char[] {'z'}, "");

    // intentional errors
    try {
      anArray.get(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.set(1, (char) 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getInt(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setInt(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getLong(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setLong(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getFloat(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setFloat(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getDouble(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setDouble(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getString(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setString(1, "100");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in CharArray.set: index (1) >= size (1).",
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
    anArray = new CharArray(2, false);
    Test.ensureEqual(anArray.size(), 0, "");
    for (int i = 0; i < 10; i++) {
      anArray.add((char) i);
      Test.ensureEqual(anArray.get(i), i, "");
      Test.ensureEqual(anArray.size(), i + 1, "");
    }
    Test.ensureEqual(anArray.size(), 10, "");
    anArray.clear();
    Test.ensureEqual(anArray.size(), 0, "");

    // active
    anArray = new CharArray(3, true);
    Test.ensureEqual(anArray.size(), 3, "");
    Test.ensureEqual(anArray.get(2), 0, "");

    // ** test array constructor
    anArray = new CharArray(new char[] {'a', 'e', 'i', 'o', 'u'});
    Test.ensureEqual(anArray.size(), 5, "");
    Test.ensureEqual(anArray.get(0), 'a', "");
    Test.ensureEqual(anArray.get(1), 'e', "");
    Test.ensureEqual(anArray.get(2), 'i', "");
    Test.ensureEqual(anArray.get(3), 'o', "");
    Test.ensureEqual(anArray.get(4), 'u', "");

    // test compare
    Test.ensureEqual(anArray.compare(1, 3), -10, "");
    Test.ensureEqual(anArray.compare(1, 1), 0, "");
    Test.ensureEqual(anArray.compare(3, 1), 10, "");

    // test toString
    Test.ensureEqual(anArray.toString(), "a, e, i, o, u", "");

    // test calculateStats
    anArray.addString("");
    double stats[] = anArray.calculateStats();
    anArray.remove(5);
    Test.ensureEqual(stats[CharArray.STATS_N], 5, "");
    Test.ensureEqual(stats[CharArray.STATS_MIN], 97, "");
    Test.ensureEqual(stats[CharArray.STATS_MAX], 117, "");
    Test.ensureEqual(stats[CharArray.STATS_SUM], 531, "");

    // test indexOf(int) indexOf(String)
    Test.ensureEqual(anArray.indexOf('a', 0), 0, "");
    Test.ensureEqual(anArray.indexOf('a', 1), -1, "");
    Test.ensureEqual(anArray.indexOf('u', 0), 4, "");
    Test.ensureEqual(anArray.indexOf('t', 0), -1, "");

    Test.ensureEqual(anArray.indexOf("a", 0), 0, "");
    Test.ensureEqual(anArray.indexOf("a", 1), -1, "");
    Test.ensureEqual(anArray.indexOf("u", 0), 4, "");
    Test.ensureEqual(anArray.indexOf("t", 0), -1, "");

    // test remove
    anArray.remove(1);
    Test.ensureEqual(anArray.size(), 4, "");
    Test.ensureEqual(anArray.get(0), 'a', "");
    Test.ensureEqual(anArray.get(1), 'i', "");
    Test.ensureEqual(anArray.get(3), 'u', "");

    // test atInsert(index, value)
    anArray.atInsert(1, (char) 22);
    Test.ensureEqual(anArray.size(), 5, "");
    Test.ensureEqual(anArray.get(0), 'a', "");
    Test.ensureEqual(anArray.get(1), 22, "");
    Test.ensureEqual(anArray.get(2), 'i', "");
    Test.ensureEqual(anArray.get(4), 'u', "");
    anArray.remove(1);

    // test removeRange
    anArray.removeRange(4, 4); // make sure it is allowed
    anArray.removeRange(1, 3);
    Test.ensureEqual(anArray.size(), 2, "");
    Test.ensureEqual(anArray.get(0), 'a', "");
    Test.ensureEqual(anArray.get(1), 'u', "");

    // test (before trimToSize) that toString, toDoubleArray, and toStringArray use
    // 'size'
    Test.ensureEqual(anArray.toString(), "a, u", "");
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {97, 117}, "");
    Test.ensureEqual(anArray.toStringArray(), new String[] {"a", "u"}, "");

    // test trimToSize
    anArray.trimToSize();
    Test.ensureEqual(anArray.array.length, 2, "");

    // test equals
    CharArray anArray2 = new CharArray();
    anArray2.add('a');
    Test.ensureEqual(
        anArray.testEquals(null),
        "The two objects aren't equal: this object is a CharArray; the other is a null.",
        "");
    Test.ensureEqual(
        anArray.testEquals("A String"),
        "The two objects aren't equal: this object is a CharArray; the other is a java.lang.String.",
        "");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two CharArrays aren't equal: one has 2 value(s); the other has 1 value(s).",
        "");
    Test.ensureTrue(!anArray.equals(anArray2), "");
    anArray2.addString("7");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two CharArrays aren't equal: this[1]=u; other[1]=7.",
        "");
    Test.ensureTrue(!anArray.equals(anArray2), "");
    anArray2.setString(1, "u");
    Test.ensureEqual(anArray.testEquals(anArray2), "", "");
    Test.ensureTrue(anArray.equals(anArray2), "");

    // test toObjectArray
    Test.ensureEqual(anArray.toArray(), anArray.toObjectArray(), "");

    // test toDoubleArray
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {97, 117}, "");

    // test reorder
    int rank[] = {1, 0};
    anArray.reorder(rank);
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {117, 97}, "");

    // ** test append and clone
    anArray = new CharArray(new char[] {(char) 1});
    anArray.append(new ByteArray(new byte[] {5, 2}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, 2}, "");
    anArray.append(new StringArray(new String[] {"", "9"}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, 2, Double.NaN, 57}, "");
    anArray2 = (CharArray) anArray.clone();
    Test.ensureEqual(anArray.getMaxIsMV(), true, "");
    Test.ensureEqual(anArray2.getMaxIsMV(), true, "");
    Test.ensureEqual(anArray2.toDoubleArray(), new double[] {1, 5, 2, Double.NaN, 57}, "");

    // test move
    anArray = new CharArray(new char[] {0, 1, 2, 3, 4});
    anArray.move(1, 3, 0);
    Test.ensureEqual(anArray.toArray(), new char[] {1, 2, 0, 3, 4}, "");

    anArray = new CharArray(new char[] {0, 1, 2, 3, 4});
    anArray.move(1, 2, 4);
    Test.ensureEqual(anArray.toArray(), new char[] {0, 2, 3, 1, 4}, "");

    // move does nothing, but is allowed
    anArray = new CharArray(new char[] {0, 1, 2, 3, 4});
    anArray.move(1, 1, 0);
    Test.ensureEqual(anArray.toArray(), new char[] {0, 1, 2, 3, 4}, "");
    anArray.move(1, 2, 1);
    Test.ensureEqual(anArray.toArray(), new char[] {0, 1, 2, 3, 4}, "");
    anArray.move(1, 2, 2);
    Test.ensureEqual(anArray.toArray(), new char[] {0, 1, 2, 3, 4}, "");
    anArray.move(5, 5, 0);
    Test.ensureEqual(anArray.toArray(), new char[] {0, 1, 2, 3, 4}, "");
    anArray.move(3, 5, 5);
    Test.ensureEqual(anArray.toArray(), new char[] {0, 1, 2, 3, 4}, "");

    // makeIndices
    anArray = new CharArray(new char[] {25, 1, 1, 10});
    IntArray indices = new IntArray();
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "\\u0001, \\n, \\u0019", "");
    Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

    anArray = new CharArray(new char[] {35, 35, Character.MAX_VALUE, 1, 2});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "\\u0001, \\u0002, #, \\uffff", "");
    Test.ensureEqual(indices.toString(), "2, 2, 3, 0, 1", "");

    anArray = new CharArray(new char[] {10, 20, 30, 40});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "\\n, \\u0014, \\u001e, (", "");
    Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

    // switchToFakeMissingValue
    anArray =
        new CharArray(
            new char[] {Character.MAX_VALUE, 1, 2, Character.MAX_VALUE, 3, Character.MAX_VALUE});
    Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
    Test.ensureEqual(anArray.toString(), "7, \\u0001, \\u0002, 7, \\u0003, 7", "");
    anArray.switchFromTo("75", "");
    Test.ensureEqual(
        anArray.toString(), "\\uffff, \\u0001, \\u0002, \\uffff, \\u0003, \\uffff", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 1, 4}, "");

    // addN
    anArray = new CharArray(new char[] {25});
    anArray.addN(2, (char) 5);
    Test.ensureEqual(anArray.toString(), "\\u0019, \\u0005, \\u0005", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 2, 0}, "");

    // add array
    anArray.add(new char[] {17, 19});
    Test.ensureEqual(anArray.toString(), "\\u0019, \\u0005, \\u0005, \\u0011, \\u0013", "");

    // subset
    PrimitiveArray ss = anArray.subset(1, 3, 4);
    Test.ensureEqual(ss.toString(), "\\u0005, \\u0013", "");
    ss = anArray.subset(0, 1, 0);
    Test.ensureEqual(ss.toString(), "\\u0019", "");
    ss = anArray.subset(0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    ss = anArray.subset(1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    ss.trimToSize();
    anArray.subset(ss, 1, 3, 4);
    Test.ensureEqual(ss.toString(), "\\u0005, \\u0013", "");
    anArray.subset(ss, 0, 1, 0);
    Test.ensureEqual(ss.toString(), "\\u0019", "");
    anArray.subset(ss, 0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    anArray.subset(ss, 1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    // evenlySpaced
    anArray = new CharArray(new char[] {10, 20, 30});
    Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
    anArray.set(2, (char) 31);
    Test.ensureEqual(
        anArray.isEvenlySpaced(),
        "CharArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.5.",
        "");
    Test.ensureEqual(
        anArray.smallestBiggestSpacing(),
        "    smallest spacing=10.0: [0]=10.0, [1]=20.0\n"
            + "    biggest  spacing=11.0: [1]=20.0, [2]=31.0",
        "");

    // isAscending
    anArray = new CharArray(new char[] {10, 10, 30});
    Test.ensureEqual(anArray.getMaxIsMV(), true, "");
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.set(2, Character.MAX_VALUE);
    Test.ensureEqual(anArray.getMaxIsMV(), true, "");
    Test.ensureEqual(
        anArray.isAscending(),
        "CharArray isn't sorted in ascending order: [2]=(missing value).",
        "");
    anArray.set(1, (char) 9);
    Test.ensureEqual(
        anArray.isAscending(), "CharArray isn't sorted in ascending order: [0]=#10 > [1]=#9.", "");

    // isDescending
    anArray = new CharArray(new char[] {30, 10, 10});
    Test.ensureEqual(anArray.isDescending(), "", "");
    anArray.set(2, Character.MAX_VALUE);
    anArray.setMaxIsMV(true);
    Test.ensureEqual(
        anArray.isDescending(),
        "CharArray isn't sorted in descending order: [1]=#10 < [2]=#65535.",
        "");
    anArray.set(1, (char) 35);
    Test.ensureEqual(
        anArray.isDescending(),
        "CharArray isn't sorted in descending order: [0]=#30 < [1]=#35.",
        "");

    // firstTie
    anArray = new CharArray(new char[] {30, 35, 10});
    Test.ensureEqual(anArray.firstTie(), -1, "");
    anArray.set(1, (char) 30);
    Test.ensureEqual(anArray.firstTie(), 0, "");

    // hashcode
    anArray = new CharArray();
    for (int i = 5; i < 1000; i++) anArray.add((char) i);
    String2.log("hashcode1=" + anArray.hashCode());
    anArray2 = (CharArray) anArray.clone();
    Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
    anArray.atInsert(0, (char) 2);
    Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

    // justKeep
    BitSet bitset = new BitSet();
    anArray = new CharArray(new char[] {(char) 0, (char) 11, (char) 22, (char) 33, (char) 44});
    bitset.set(1);
    bitset.set(4);
    anArray.justKeep(bitset);
    Test.ensureEqual(anArray.toString(), "\\u000b, \",\"", "");

    // min max
    anArray = new CharArray();
    anArray.addPAOne(anArray.MINEST_VALUE());
    anArray.addPAOne(anArray.MAXEST_VALUE());
    anArray.addString("");
    Test.ensureEqual(anArray.getString(0), "\u0000", "");
    Test.ensureEqual(anArray.getString(1), "\uFFFE", "");
    Test.ensureEqual(anArray.getString(2), "", "");

    // tryToFindNumericMissingValue()
    Test.ensureEqual(new CharArray(new char[] {}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new CharArray(new char[] {'1', '2'}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(
        new CharArray(new char[] {Character.MIN_VALUE}).tryToFindNumericMissingValue(),
        PAOne.fromChar(Character.MIN_VALUE),
        "");
    anArray = new CharArray(new char[] {Character.MAX_VALUE});
    Test.ensureEqual(anArray.getMaxIsMV(), true, "");
    PAOne paOne1 = anArray.tryToFindNumericMissingValue();
    Test.ensureEqual(paOne1.pa().getMaxIsMV(), true, "");
    PAOne paOne2 = PAOne.fromChar(Character.MAX_VALUE);
    Test.ensureEqual(paOne2.pa().getMaxIsMV(), true, "");
    Test.ensureEqual(paOne1, paOne2, "");
    Test.ensureEqual(
        new CharArray(new char[] {'1', '\uffff'}).tryToFindNumericMissingValue(), paOne2, "");
  }
}
