package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.BitSet;
import java.util.HashSet;

class StringArrayTests {

  /**
   * This tests the methods of this class.
   *
   * @throws Throwable if trouble.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("*** StringArray.basicTest");
    String sar[];
    StringArray anArray = new StringArray();

    StringArray sa1 = null;

    Test.ensureEqual("test".equals(null), false, "");
    Test.ensureTrue(sa1 == null, "");
    Test.ensureEqual(new StringArray().equals(sa1), false, "");

    // ** test default constructor and many of the methods
    Test.ensureEqual(anArray.isIntegerType(), false, "");
    Test.ensureEqual(anArray.missingValue().getRawDouble(), Double.NaN, "");
    anArray.addString("");
    Test.ensureEqual(anArray.get(0), "", "");
    Test.ensureEqual(anArray.getRawInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getRawDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getRawString(0), "", "");
    Test.ensureEqual(anArray.getRawNiceDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    Test.ensureEqual(anArray.getString(0), "", "");
    anArray.clear();

    anArray = StringArray.fromCSV("\"a\\t\", a, \"a\\n\", \"a\\u20AC\", ,  \"a\\uffff\" ");
    Test.ensureEqual(anArray.toString(), "a\\t, a, a\\n, a\\u20ac, , a\\uffff", "");
    Test.ensureEqual(anArray.toNccsvAttString(), "a\\t\\na\\na\\n\\na\u20ac\\n\\na\uffff", "");
    Test.ensureEqual(anArray.toNccsv127AttString(), "a\\t\\na\\na\\n\\na\\u20ac\\n\\na\\uffff", "");
    anArray.clear();

    Test.ensureEqual(anArray.size(), 0, "");
    anArray.add("1234.5");
    Test.ensureEqual(anArray.size(), 1, "");
    Test.ensureEqual(anArray.get(0), "1234.5", "");
    Test.ensureEqual(anArray.getInt(0), 1235, "");
    Test.ensureEqual(anArray.getFloat(0), 1234.5f, "");
    Test.ensureEqual(anArray.getDouble(0), 1234.5, "");
    Test.ensureEqual(anArray.getString(0), "1234.5", "");
    Test.ensureEqual(anArray.elementType(), PAType.STRING, "");
    String tArray[] = anArray.toArray();
    Test.ensureEqual(tArray, new String[] {"1234.5"}, "");

    // intentional errors
    try {
      anArray.get(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.set(1, "100");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getInt(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setInt(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getLong(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setLong(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getFloat(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setFloat(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getDouble(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setDouble(1, 100);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).",
          "");
    }
    try {
      anArray.getString(1);
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.get: index (1) >= size (1).",
          "");
    }
    try {
      anArray.setString(1, "100");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
      Test.ensureEqual(
          e.toString(),
          "java.lang.IllegalArgumentException: ERROR in StringArray.set: index (1) >= size (1).",
          "");
    }

    // set NaN returned as NaN
    anArray.setDouble(0, Double.NaN);
    Test.ensureEqual(anArray.getDouble(0), Double.NaN, "");
    anArray.setDouble(0, -1e300);
    Test.ensureEqual(anArray.getDouble(0), -1e300, "");
    anArray.setDouble(0, 2.2);
    Test.ensureEqual(anArray.getDouble(0), 2.2, "");
    anArray.setFloat(0, Float.NaN);
    Test.ensureEqual(anArray.getFloat(0), Float.NaN, "");
    anArray.setFloat(0, -1e33f);
    Test.ensureEqual(anArray.getFloat(0), -1e33f, "");
    anArray.setFloat(0, 3.3f);
    Test.ensureEqual(anArray.getFloat(0), 3.3f, "");
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
    anArray = new StringArray(2, false);
    Test.ensureEqual(anArray.size(), 0, "");
    for (int i = 0; i < 10; i++) {
      anArray.add(String.valueOf(i));
      Test.ensureEqual(anArray.get(i), "" + i, "");
      Test.ensureEqual(anArray.size(), i + 1, "");
    }
    Test.ensureEqual(anArray.size(), 10, "");
    anArray.clear();
    Test.ensureEqual(anArray.size(), 0, "");

    // active
    anArray = new StringArray(3, true);
    Test.ensureEqual(anArray.size(), 3, "");
    Test.ensureEqual(anArray.get(2), "", "");

    // ** test array constructor
    anArray = new StringArray(new String[] {"0", "2", "4", "6", "8"});
    Test.ensureEqual(anArray.size(), 5, "");
    Test.ensureEqual(anArray.get(0), "0", "");
    Test.ensureEqual(anArray.get(1), "2", "");
    Test.ensureEqual(anArray.get(2), "4", "");
    Test.ensureEqual(anArray.get(3), "6", "");
    Test.ensureEqual(anArray.get(4), "8", "");

    // test compare
    Test.ensureEqual(anArray.compare(1, 3), -4, "");
    Test.ensureEqual(anArray.compare(1, 1), 0, "");
    Test.ensureEqual(anArray.compare(3, 1), 4, "");

    // test compareIgnoreCase
    StringArray cic = StringArray.fromCSV("A, a, ABE, abe");
    Test.ensureEqual(cic.toString(), "A, a, ABE, abe", "");
    Test.ensureEqual(cic.compare(0, 1), -32, "");
    Test.ensureEqual(cic.compare(1, 2), 32, "");
    Test.ensureEqual(cic.compare(2, 3), -32, "");
    Test.ensureEqual(cic.compare(1, 3), -2, "");

    Test.ensureEqual(cic.compareIgnoreCase(0, 1), -32, "");
    Test.ensureEqual(cic.compareIgnoreCase(1, 2), -2, "");
    Test.ensureEqual(cic.compareIgnoreCase(2, 3), -32, "");
    Test.ensureEqual(cic.compareIgnoreCase(1, 3), -2, "");

    // test toString
    Test.ensureEqual(anArray.toString(), "0, 2, 4, 6, 8", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {5, 0, 4}, "");
    Test.ensureEqual(anArray.getNMinMax(), new String[] {"5", "0", "8"}, "");

    // test calculateStats
    anArray.addString("");
    double stats[] = anArray.calculateStats();
    anArray.remove(5);
    Test.ensureEqual(stats[StringArray.STATS_N], 5, "");
    Test.ensureEqual(stats[StringArray.STATS_MIN], 0, "");
    Test.ensureEqual(stats[StringArray.STATS_MAX], 8, "");
    Test.ensureEqual(stats[StringArray.STATS_SUM], 20, "");

    // test indexOf(int) indexOf(String)
    Test.ensureEqual(anArray.indexOf(null), -1, "");
    Test.ensureEqual(anArray.indexOf("0", 0), 0, "");
    Test.ensureEqual(anArray.indexOf("0", 1), -1, "");
    Test.ensureEqual(anArray.indexOf("8", 0), 4, "");
    Test.ensureEqual(anArray.indexOf("9", 0), -1, "");

    Test.ensureEqual(anArray.indexOfIgnoreCase(null), -1, "");
    Test.ensureEqual(anArray.indexOfIgnoreCase("8", 0), 4, "");

    // test lastIndexOf
    Test.ensureEqual(anArray.lastIndexOf(null), -1, "");
    Test.ensureEqual(anArray.lastIndexOf("0", 0), 0, "");
    Test.ensureEqual(anArray.lastIndexOf("0", 0), 0, "");
    Test.ensureEqual(anArray.lastIndexOf("8", 4), 4, "");
    Test.ensureEqual(anArray.lastIndexOf("6", 2), -1, "");
    Test.ensureEqual(anArray.lastIndexOf("6", 3), 3, "");
    Test.ensureEqual(anArray.lastIndexOf("9", 2), -1, "");

    // test remove
    anArray.remove(1);
    Test.ensureEqual(anArray.size(), 4, "");
    Test.ensureEqual(anArray.get(0), "0", "");
    Test.ensureEqual(anArray.get(1), "4", "");
    Test.ensureEqual(anArray.get(3), "8", "");
    Test.ensureEqual(anArray.array[4], null, ""); // can't use get()

    // test atInsert(index, value) and maxStringLength
    anArray.atInsertString(1, "22");
    Test.ensureEqual(anArray.size(), 5, "");
    Test.ensureEqual(anArray.get(0), "0", "");
    Test.ensureEqual(anArray.get(1), "22", "");
    Test.ensureEqual(anArray.get(2), "4", "");
    Test.ensureEqual(anArray.get(4), "8", "");
    Test.ensureEqual(anArray.maxStringLength(), 2, "");
    anArray.remove(1);
    Test.ensureEqual(anArray.maxStringLength(), 1, "");

    // test removeRange
    anArray.removeRange(4, 4); // make sure it is allowed
    anArray.removeRange(1, 3);
    Test.ensureEqual(anArray.size(), 2, "");
    Test.ensureEqual(anArray.get(0), "0", "");
    Test.ensureEqual(anArray.get(1), "8", "");
    Test.ensureEqual(anArray.array[2], null, ""); // can't use get()
    Test.ensureEqual(anArray.array[3], null, "");

    // test (before trimToSize) that toString, toDoubleArray, and toStringArray use
    // 'size'
    Test.ensureEqual(anArray.toString(), "0, 8", "");
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {0, 8}, "");
    Test.ensureEqual(anArray.toStringArray(), new String[] {"0", "8"}, "");

    // test trimToSize
    anArray.trimToSize();
    Test.ensureEqual(anArray.array.length, 2, "");

    // test equals
    StringArray anArray2 = new StringArray();
    anArray2.add("0");
    Test.ensureEqual(
        anArray.testEquals(null),
        "The two objects aren't equal: this object is a StringArray; the other is a null.",
        "");
    Test.ensureEqual(
        anArray.testEquals("A String"),
        "The two objects aren't equal: this object is a StringArray; the other is a java.lang.String.",
        "");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two StringArrays aren't equal: one has 2 value(s); the other has 1 value(s).",
        "");
    Test.ensureTrue(!anArray.equals(anArray2), "");
    anArray2.addString("7");
    Test.ensureEqual(
        anArray.testEquals(anArray2),
        "The two StringArrays aren't equal: this[1]=\"8\"; other[1]=\"7\".",
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
    anArray = new StringArray(new String[] {"1"});
    anArray.append(new ByteArray(new byte[] {5, -5}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, -5}, "");
    anArray.append(new StringArray(new String[] {"a", "9"}));
    Test.ensureEqual(anArray.toDoubleArray(), new double[] {1, 5, -5, Double.NaN, 9}, "");
    anArray2 = (StringArray) anArray.clone();
    Test.ensureEqual(anArray2.toDoubleArray(), new double[] {1, 5, -5, Double.NaN, 9}, "");

    // test move
    anArray = new StringArray(new String[] {"0", "1", "2", "3", "4"});
    anArray.move(1, 3, 0);
    Test.ensureEqual(anArray.toArray(), new String[] {"1", "2", "0", "3", "4"}, "");

    anArray = new StringArray(new String[] {"0", "1", "2", "3", "4"});
    anArray.move(1, 2, 4);
    Test.ensureEqual(anArray.toArray(), new String[] {"0", "2", "3", "1", "4"}, "");

    // test reorder
    anArray.reverse();
    Test.ensureEqual(anArray.toArray(), new String[] {"4", "1", "3", "2", "0"}, "");

    // test getSVString
    anArray =
        new StringArray(
            new String[] {"", "test", ",", " something", "\n\t", "he \"said\"", "a\\b"});
    Test.ensureEqual(anArray.getSVString(0), "", "");
    Test.ensureEqual(anArray.getSVString(1), "test", "");
    Test.ensureEqual(anArray.getSVString(2), "\",\"", "");
    Test.ensureEqual(anArray.getSVString(3), "\" something\"", "");
    Test.ensureEqual(anArray.getSVString(4), "\"\\n\\t\"", "");
    Test.ensureEqual(anArray.getSVString(5), "\"he \\\"said\\\"\"", "");
    Test.ensureEqual(anArray.getSVString(6), "\"a\\\\b\"", "");

    // move does nothing, but is allowed
    anArray = new StringArray(new String[] {"0", "1", "2", "3", "4"});
    anArray.move(1, 1, 0);
    Test.ensureEqual(anArray.toArray(), new String[] {"0", "1", "2", "3", "4"}, "");
    anArray.move(1, 2, 1);
    Test.ensureEqual(anArray.toArray(), new String[] {"0", "1", "2", "3", "4"}, "");
    anArray.move(1, 2, 2);
    Test.ensureEqual(anArray.toArray(), new String[] {"0", "1", "2", "3", "4"}, "");
    anArray.move(5, 5, 0);
    Test.ensureEqual(anArray.toArray(), new String[] {"0", "1", "2", "3", "4"}, "");
    anArray.move(3, 5, 5);
    Test.ensureEqual(anArray.toArray(), new String[] {"0", "1", "2", "3", "4"}, "");

    // makeIndices
    anArray = new StringArray(new String[] {"d", "a", "a", "b"});
    IntArray indices = new IntArray();
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "a, b, d", "");
    Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

    anArray =
        new StringArray(
            new String[] {
              "d", "d", "a", "", "b",
            });
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "a, b, d, ", "");
    Test.ensureEqual(indices.toString(), "2, 2, 0, 3, 1", "");

    anArray = new StringArray(new String[] {"aa", "ab", "ac", "ad"});
    Test.ensureEqual(anArray.makeIndices(indices).toString(), "aa, ab, ac, ad", "");
    Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

    // switchToFakeMissingValue
    anArray = new StringArray(new String[] {"", "1", "2", "", "3", ""});
    Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
    Test.ensureEqual(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
    anArray.switchFromTo("75", "");
    Test.ensureEqual(anArray.toString(), ", 1, 2, , 3, ", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 1, 4}, "");

    // addN
    anArray = new StringArray(new String[] {"a"});
    anArray.addN(2, "bb");
    Test.ensureEqual(anArray.toString(), "a, bb, bb", "");
    Test.ensureEqual(anArray.getNMinMaxIndex(), new int[] {3, 0, 2}, "");

    // add array
    anArray.add(new String[] {"17", "19"});
    Test.ensureEqual(anArray.toString(), "a, bb, bb, 17, 19", "");

    // subset
    PrimitiveArray ss = anArray.subset(1, 3, 4);
    Test.ensureEqual(ss.toString(), "bb, 19", "");
    ss = anArray.subset(0, 1, 0);
    Test.ensureEqual(ss.toString(), "a", "");
    ss = anArray.subset(0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    ss = anArray.subset(1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    ss.trimToSize();
    anArray.subset(ss, 1, 3, 4);
    Test.ensureEqual(ss.toString(), "bb, 19", "");
    anArray.subset(ss, 0, 1, 0);
    Test.ensureEqual(ss.toString(), "a", "");
    anArray.subset(ss, 0, 1, -1);
    Test.ensureEqual(ss.toString(), "", "");
    anArray.subset(ss, 1, 1, 0);
    Test.ensureEqual(ss.toString(), "", "");

    // wordsAndQuotedPhrases(String searchFor)
    Test.ensureEqual(StringArray.wordsAndQuotedPhrases(null).toString(), "", "");
    Test.ensureEqual(StringArray.wordsAndQuotedPhrases("a bb").toString(), "a, bb", "");
    Test.ensureEqual(StringArray.wordsAndQuotedPhrases(" a bb c ").toString(), "a, bb, c", "");
    Test.ensureEqual(
        StringArray.wordsAndQuotedPhrases(",a,bb, c ,d").toString(), "a, bb, c, d", "");
    Test.ensureEqual(
        StringArray.wordsAndQuotedPhrases(" a,\"b b\",c ").toString(), "a, b b, c", "");
    Test.ensureEqual(
        StringArray.wordsAndQuotedPhrases(" a,\"b b").toString(),
        "a, b b",
        ""); // no error for missing "
    Test.ensureEqual(StringArray.wordsAndQuotedPhrases(" , ").toString(), "", "");
    anArray = StringArray.wordsAndQuotedPhrases(" a,\"b\"\"b\",c "); // internal quotes
    Test.ensureEqual(anArray.toString(), "a, \"b\"\"b\", c", "");
    Test.ensureEqual(anArray.get(1), "b\"b", "");
    anArray = StringArray.wordsAndQuotedPhrases(" a \"b\"\"b\" c "); // internal quotes
    Test.ensureEqual(anArray.toString(), "a, \"b\"\"b\", c", "");
    Test.ensureEqual(anArray.get(1), "b\"b", "");
    anArray = StringArray.wordsAndQuotedPhrases("a \"-bob\" c");
    Test.ensureEqual(anArray.get(1), "-bob", "");
    anArray = StringArray.wordsAndQuotedPhrases("a -\"bob\" c"); // internal quotes
    Test.ensureEqual(anArray.get(1), "-\"bob\"", "");

    // fromCSV(String searchFor)
    Test.ensureEqual(StringArray.fromCSV(null).toString(), "", "");
    Test.ensureEqual(StringArray.fromCSV("a, b b").toString(), "a, b b", "");
    Test.ensureEqual(StringArray.fromCSV(" a, b b ,c ").toString(), "a, b b, c", "");
    Test.ensureEqual(StringArray.fromCSV(",a,b b, c ,d,").toString(), ", a, b b, c, d, ", "");
    Test.ensureEqual(StringArray.fromCSV(" a, \"b b\" ,c ").toString(), "a, b b, c", "");
    Test.ensureEqual(
        StringArray.fromCSV(" a,\"b b").toString(), "a, b b", ""); // no error for missing "
    Test.ensureEqual(StringArray.fromCSV(" , ").toString(), ", ", "");
    anArray = StringArray.fromCSV(" a,\"b\"\"b\",c ");
    Test.ensureEqual(anArray.get(1), "b\"b", ""); // internal quotes
    anArray = StringArray.fromCSV(" a,\"b\\\"b\",c ");
    Test.ensureEqual(anArray.get(1), "b\"b", ""); // internal quotes
    anArray = StringArray.fromCSV(" a, \"b\\tb\" ,c ");
    Test.ensureEqual(anArray.get(1), "b\tb", ""); // internal quotes
    anArray = StringArray.fromCSV(" a,\"b\\nb\",c ");
    Test.ensureEqual(anArray.get(1), "b\nb", ""); // internal quotes
    anArray = StringArray.fromCSV(" a,\"b\\'b\",c ");
    Test.ensureEqual(anArray.get(1), "b\'b", ""); // internal quotes
    anArray = StringArray.fromCSV(" a,\"b\\\"b\",c ");
    Test.ensureEqual(anArray.get(1), "b\"b", ""); // internal quotes
    anArray = StringArray.fromCSV(" a \"b\"\"b\" c ");
    Test.ensureEqual(
        anArray.get(0), "b\"b", ""); // internal quotes, only the quoted string is saved

    // evenlySpaced
    anArray = new StringArray(new String[] {"10", "20", "30"});
    Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
    anArray.set(2, "30.1");
    Test.ensureEqual(
        anArray.isEvenlySpaced(),
        "StringArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.05.",
        "");
    Test.ensureEqual(
        anArray.smallestBiggestSpacing(),
        "    smallest spacing=10.0: [0]=10.0, [1]=20.0\n"
            + "    biggest  spacing=10.100000000000001: [1]=20.0, [2]=30.1",
        "");

    // fromCSV
    Test.ensureEqual(StringArray.fromCSV(null).toString(), "", "");
    Test.ensureEqual(StringArray.fromCSV("").toString(), "", "");
    Test.ensureEqual(StringArray.fromCSV(" ").toString(), "", "");
    Test.ensureEqual(StringArray.fromCSV(",").toString(), ", ", "");
    Test.ensureEqual(StringArray.fromCSV(" , ").toString(), ", ", "");
    Test.ensureEqual(StringArray.fromCSV("a,bb").toString(), "a, bb", "");
    Test.ensureEqual(StringArray.fromCSV(" a , bb ").toString(), "a, bb", "");
    Test.ensureEqual(StringArray.fromCSV(" a, bb ,c ").toString(), "a, bb, c", "");
    Test.ensureEqual(StringArray.fromCSV(",a,bb, c ,").toString(), ", a, bb, c, ", "");
    Test.ensureEqual(StringArray.fromCSV(" a,\"b b\",c ").toString(), "a, b b, c", "");
    Test.ensureEqual(
        StringArray.fromCSV(" a,\"b b").toString(), "a, b b", ""); // no error for missing "
    // only the part in quotes is saved
    Test.ensureEqual(
        StringArray.fromCSV(" a,junk\"b \"\"\"\"b\"junk,c ").toString(),
        "a, \"b \"\"\"\"b\", c",
        "");
    Test.ensureEqual(StringArray.fromCSV(" a,junk\"b \"\"\"\"b\"junk,c ").get(1), "b \"\"b", "");
    Test.ensureEqual(StringArray.fromCSV(" a,junk\"b,b\"junk").toString(), "a, \"b,b\"", "");

    // isAscending
    anArray = new StringArray(new String[] {"go", "go", "hi"});
    Test.ensureEqual(anArray.isAscending(), "", "");
    anArray.set(2, null);
    Test.ensureEqual(
        anArray.isAscending(), "StringArray isn't sorted in ascending order: [2]=null.", "");
    anArray.set(1, "ga");
    Test.ensureEqual(
        anArray.isAscending(),
        "StringArray isn't sorted in ascending order: [0]=\"go\" > [1]=\"ga\".",
        "");

    // isDescending
    anArray = new StringArray(new String[] {"hi", "go", "go"});
    Test.ensureEqual(anArray.isDescending(), "", "");
    anArray.set(2, null);
    Test.ensureEqual(
        anArray.isDescending(), "StringArray isn't sorted in descending order: [2]=null.", "");
    anArray.set(1, "pa");
    Test.ensureEqual(
        anArray.isDescending(),
        "StringArray isn't sorted in descending order: [0]=\"hi\" < [1]=\"pa\".",
        "");

    // firstTie
    anArray = new StringArray(new String[] {"hi", "pa", "go"});
    Test.ensureEqual(anArray.firstTie(), -1, "");
    anArray.set(1, "hi");
    Test.ensureEqual(anArray.firstTie(), 0, "");
    anArray.set(1, null);
    Test.ensureEqual(anArray.firstTie(), -1, "");
    anArray.set(2, null);
    Test.ensureEqual(anArray.firstTie(), 1, "");

    // diff
    anArray = new StringArray(new String[] {"0", "11", "22"});
    anArray2 = new StringArray(new String[] {"0", "11", "22"});
    String s = anArray.diffString(anArray2);
    Test.ensureEqual(s, "", "s=" + s);
    anArray2.add("33");
    s = anArray.diffString(anArray2);
    Test.ensureEqual(s, "  old [3]=33,\n  new [3]=null.", "s=" + s);
    anArray2.set(2, "23");
    s = anArray.diffString(anArray2);
    Test.ensureEqual(s, "  old [2]=23,\n  new [2]=22.", "s=" + s);
    IntArray ia = new IntArray(new int[] {0, 11, 22});
    s = anArray.diffString(ia);
    Test.ensureEqual(s, "", "s=" + s);
    ia.set(2, 23);
    s = anArray.diffString(ia);
    Test.ensureEqual(s, "  old [2]=23,\n  new [2]=22.", "s=" + s);

    // utf8
    String os = " s\\\n\tÃ\u20ac ";
    StringArray sa = new StringArray(new String[] {os});
    sa.toUTF8().fromUTF8();
    Test.ensureEqual(sa.get(0), os, "");

    // hashcode
    anArray = new StringArray();
    for (int i = 5; i < 1000; i++) anArray.add("" + i);
    String2.log("hashcode1=" + anArray.hashCode());
    anArray2 = (StringArray) anArray.clone();
    Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
    anArray.atInsertString(0, "2");
    Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

    // justKeep
    BitSet bitset = new BitSet();
    anArray = new StringArray(new String[] {"0", "11", "22", "33", "44"});
    bitset.set(1);
    bitset.set(4);
    anArray.justKeep(bitset);
    Test.ensureEqual(anArray.toString(), "11, 44", "");

    // min max
    anArray = new StringArray();
    anArray.addPAOne(anArray.MINEST_VALUE());
    anArray.addPAOne(anArray.MAXEST_VALUE());
    Test.ensureEqual(
        anArray.getString(0),
        anArray.MINEST_VALUE().getString(),
        ""); // trouble: toString vs getString
    Test.ensureEqual(anArray.getString(0), "\u0000", "");
    Test.ensureEqual(
        anArray.getString(1),
        anArray.MAXEST_VALUE().getString(),
        ""); // trouble: toString vs getString
    Test.ensureEqual(anArray.getString(1), "\uFFFE", "");

    // sort
    anArray = StringArray.fromCSV("AB, AB, Ab, Ab, ABC, ABc, AbC, aB, aB, ab, ab");
    anArray.sort();
    // note that short sort before long when identical
    Test.ensureEqual(anArray.toString(), "AB, AB, ABC, ABc, Ab, Ab, AbC, aB, aB, ab, ab", "");

    // sortIgnoreCase
    anArray = StringArray.fromCSV("AB, AB, Ab, Ab, ABC, ABc, AbC, aB, aB, ab, ab");
    anArray.sortIgnoreCase();
    // note that all short ones sort before all long ones
    Test.ensureEqual(anArray.toString(), "AB, AB, Ab, Ab, aB, aB, ab, ab, ABC, ABc, AbC", "");

    // numbers
    DoubleArray da = new DoubleArray(new double[] {5, Double.NaN});
    da.sort(); // do NaN's sort high?
    Test.ensureEqual(da.toString(), "5.0, NaN", "");
    Test.ensureEqual(da.getString(1), "", "");

    // arrayFromCSV, test with unquoted internal strings
    // outside of a quoted string \\u20ac is json-encoded
    sar = StringArray.arrayFromCSV(" ab , \n\t\\\u00c3\u20ac , \n\u20ac , ", ",", true); // trim?
    Test.ensureEqual(
        String2.annotatedString(String2.toCSVString(sar)),
        "ab,\"\\\\\\u00c3\\u20ac\",\"\\u20ac\",[end]",
        ""); // spaces, \n, \\t are trimmed
    sar = StringArray.arrayFromCSV(" ab , \n\t\\\u00c3\u20ac , \n\u20ac , ", ",", false); // trim?
    Test.ensureEqual(
        String2.toCSVString(sar),
        "\" ab \",\" \\n\\t\\\\\\u00c3\\u20ac \",\" \\n\\u20ac \",\" \"",
        "");

    // arrayFromCSV, test with quoted internal strings
    // inside of a quoted string \\u20ac is Euro
    sar =
        StringArray.arrayFromCSV(
            " ab , \" \n\t\\\u00c3\u20ac \", \" \n\u20ac \" , ", ",", true); // trim?
    Test.ensureEqual(
        String2.toCSVString(sar), "ab,\" \\n\\t\\\\\\u00c3\\u20ac \",\" \\n\\u20ac \",", "");
    sar =
        StringArray.arrayFromCSV(
            " ab , \" \n\t\\\u00c3\u20ac \", \" \n\u20ac \", \" \"", ",", false); // trim?
    Test.ensureEqual(
        String2.toCSVString(sar),
        "\" ab \",\" \\n\\t\\\\\\u00c3\\u20ac \",\" \\n\\u20ac \",\" \"",
        "");

    // inCommon
    anArray = StringArray.fromCSV("a, b, d");
    anArray2 = StringArray.fromCSV("a, c, d");
    anArray.inCommon(anArray2);
    Test.ensureEqual(anArray.toString(), "a, d", "");

    anArray = StringArray.fromCSV("a, d");
    anArray2 = StringArray.fromCSV("b, e");
    anArray.inCommon(anArray2);
    Test.ensureEqual(anArray.toString(), "", "");

    anArray = StringArray.fromCSV("");
    anArray2 = StringArray.fromCSV("a, c, d");
    anArray.inCommon(anArray2);
    Test.ensureEqual(anArray.toString(), "", "");

    anArray = StringArray.fromCSV("");
    anArray2 = StringArray.fromCSV("a, c, d");
    anArray.inCommon(anArray2);
    Test.ensureEqual(anArray.toString(), "", "");

    anArray = StringArray.fromCSV("c");
    anArray2 = StringArray.fromCSV("a, c, d");
    anArray.inCommon(anArray2);
    Test.ensureEqual(anArray.toString(), "c", "");

    anArray = StringArray.fromCSV("a, b, c");
    anArray2 = StringArray.fromCSV("c");
    anArray.inCommon(anArray2);
    Test.ensureEqual(anArray.toString(), "c", "");

    // removeEmpty
    anArray = StringArray.fromCSV("a, , c");
    Test.ensureEqual(anArray.removeIfNothing(), 2, "");
    Test.ensureEqual(anArray.toString(), "a, c", "");

    anArray = StringArray.fromCSV(" , b, ");
    Test.ensureEqual(anArray.removeIfNothing(), 1, "");
    Test.ensureEqual(anArray.toString(), "b", "");

    anArray = StringArray.fromCSV(" , , ");
    Test.ensureEqual(anArray.removeIfNothing(), 0, "");
    Test.ensureEqual(anArray.toString(), "", "");

    // fromCSVNoBlanks
    anArray = StringArray.fromCSVNoBlanks(", b, ,d,,");
    Test.ensureEqual(anArray.toString(), "b, d", "");

    // toHashSet addHashSet
    anArray = StringArray.fromCSV("a, e, i, o, uu");
    HashSet<String> hs = anArray.toHashSet();
    anArray2 = new StringArray().addSet(hs);
    anArray2.sort();
    Test.ensureEqual(anArray.toArray(), anArray2.toArray(), "");

    // fromNccsv
    anArray = StringArray.simpleFromNccsv("");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"\"", "");
    Test.ensureEqual(anArray.size(), 1, "");

    anArray = StringArray.simpleFromNccsv("a");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"a\"", "");
    Test.ensureEqual(anArray.size(), 1, "");

    anArray = StringArray.simpleFromNccsv(" a , b ,");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"a\", \"b\", \"\"", "");
    Test.ensureEqual(anArray.size(), 3, "");

    anArray = StringArray.simpleFromNccsv(" \" a\t\n\b\'z\"\" \" , 1.23f, a\"");
    // \\b is removed
    Test.ensureEqual(
        String2.annotatedString(String2.replaceAll(anArray.get(0), "\n", "")),
        "\" a[9][8]'z\"\" \"[end]",
        "");
    Test.ensureEqual(
        anArray.toJsonCsvString(), "\"\\\" a\\t\\n'z\\\"\\\" \\\"\", \"1.23f\", \"a\\\"\"", "");
    Test.ensureEqual(anArray.size(), 3, "");

    anArray =
        StringArray.simpleFromNccsv(
            // \\b is not allowed
            "'\\f', '\\n', '\\r', '\\t', '\\\\', '\\/', '\\\"', 'a', '~', '\\u00C0', '\\u0000', '\\uffFf'");
    Test.ensureEqual(
        anArray.toJsonCsvString(),
        "\"'\\\\f'\", \"'\\\\n'\", \"'\\\\r'\", \"'\\\\t'\", \"'\\\\\\\\'\", \"'\\\\/'\", \"'\\\\\\\"'\", \"'a'\", \"'~'\", \"'\\\\u00C0'\", \"'\\\\u0000'\", \"'\\\\uffFf'\"",
        anArray.toJsonCsvString());
    Test.ensureEqual(anArray.size(), 12, "");

    // removeEmptyAtEnd();
    anArray = new StringArray(new String[] {"hi", "go", "to"});
    Test.ensureEqual(anArray.size(), 3, "");
    anArray.removeEmptyAtEnd();
    Test.ensureEqual(anArray.size(), 3, "");
    anArray.set(0, "");
    anArray.set(2, "");
    anArray.removeEmptyAtEnd();
    Test.ensureEqual(anArray.size(), 2, "");
    anArray.set(1, "");
    anArray.removeEmptyAtEnd();
    Test.ensureEqual(anArray.size(), 0, "");

    // simpleFromJsonArray
    anArray = StringArray.simpleFromJsonArray("[]");
    Test.ensureEqual(anArray.size(), 0, "");
    anArray = StringArray.simpleFromJsonArray("\n[\n]\n");
    Test.ensureEqual(anArray.size(), 0, "");

    anArray = StringArray.simpleFromJsonArray("[1]");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"1\"", "");
    anArray = StringArray.simpleFromJsonArray("[1,2]");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"1\", \"2\"", "");
    anArray = StringArray.simpleFromJsonArray("[ 1 ]");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"1\"", "");
    anArray = StringArray.simpleFromJsonArray("[ 1 , 2 ]");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"1\", \"2\"", "");

    anArray = StringArray.simpleFromJsonArray("[\"\"]");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"\\\"\\\"\"", "");
    anArray = StringArray.simpleFromJsonArray("[\"\",\"\"]");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"\\\"\\\"\", \"\\\"\\\"\"", "");

    anArray = StringArray.simpleFromJsonArray("[\" a\n\t\f\\\u00C0\u20ac \"]");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"\\\" a\\n\\t\\f\\\\\\u00c0\\u20ac \\\"\"", "");
    anArray = StringArray.simpleFromJsonArray(" [ \" a\n\t\f\\\u00C0\u20ac \" ] ");
    Test.ensureEqual(anArray.toJsonCsvString(), "\"\\\" a\\n\\t\\f\\\\\\u00c0\\u20ac \\\"\"", "");

    // test invalid jsonArray
    s = "shouldn't get here";
    try {
      anArray = StringArray.simpleFromJsonArray(null);
    } catch (Exception e) {
      s = e.toString();
    }
    Test.ensureEqual(
        s, "com.cohort.util.SimpleException: A null value isn't a valid JSON array.", "");

    s = "shouldn't get here";
    try {
      anArray = StringArray.simpleFromJsonArray(" ");
    } catch (Exception e) {
      s = e.toString();
    }
    Test.ensureEqual(s, "com.cohort.util.SimpleException: A JSON array must start with '['.", "");

    s = "shouldn't get here";
    try {
      anArray = StringArray.simpleFromJsonArray(" a[1]");
    } catch (Exception e) {
      s = e.toString();
    }
    Test.ensureEqual(s, "com.cohort.util.SimpleException: A JSON array must start with '['.", "");

    s = "shouldn't get here";
    try {
      anArray = StringArray.simpleFromJsonArray(" [1]a");
    } catch (Exception e) {
      s = e.toString();
    }
    Test.ensureEqual(
        s,
        "com.cohort.util.SimpleException: There must not be content in the JSON array after ']'.",
        "");

    s = "shouldn't get here";
    try {
      anArray = StringArray.simpleFromJsonArray("[a,]");
    } catch (Exception e) {
      s = e.toString();
    }
    Test.ensureEqual(
        s, "com.cohort.util.SimpleException: A value in a JSON array must not be nothing.", "");

    s = "shouldn't get here";
    try {
      anArray = StringArray.simpleFromJsonArray("[\"ab]");
    } catch (Exception e) {
      s = e.toString();
    }
    Test.ensureEqual(
        s,
        "com.cohort.util.SimpleException: A string in the JSON array lacks a closing double quote.",
        "");

    s = "shouldn't get here";
    try {
      anArray = StringArray.simpleFromJsonArray("[\"ab\\");
    } catch (Exception e) {
      s = e.toString();
    }
    Test.ensureEqual(
        s,
        "com.cohort.util.SimpleException: A string in the JSON array lacks a closing double quote.",
        "");

    // makeUnique
    anArray = StringArray.fromCSV("a_3, a, b_2, b, b, a, a_2, a");
    Test.ensureEqual(anArray.makeUnique(), 3, "");
    Test.ensureEqual(anArray.toString(), "a_3, a, b_2, b, b_3, a_4, a_2, a_5", "");

    // tryToFindNumericMissingValue()
    Test.ensureEqual(new StringArray(new String[] {}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(new StringArray(new String[] {""}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(
        new StringArray(new String[] {"a", "", "1", "2"}).tryToFindNumericMissingValue(), null, "");
    Test.ensureEqual(
        new StringArray(new String[] {"a", "", "1", "99"}).tryToFindNumericMissingValue(),
        null,
        ""); // doesn't
    // catch
    // 99.
    // Should
    // it?

  }
}
