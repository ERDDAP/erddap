package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.PersistentTable;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class PersistentTableTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests this class.
   *
   * @throws Throwable if trouble.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("\nPersistentTable.basicTest()");
    // verbose = true;
    // reallyVerbose = true;
    int n;
    long time;

    // find longest FLOAT_LENGTH
    String s = "" + Float.MIN_VALUE * -4f / 3f;
    int longest = s.length();
    String longestS = s;
    for (int i = 0; i < 1000; i++) {
      s = "" + ((float) Math.random() / -1e10f);
      if (s.length() > longest) {
        longest = s.length();
        longestS = s;
      }
    }
    String2.log("float longestS=" + longestS + " length=" + longest);
    Test.ensureTrue(longest <= 15, "");

    // find longest DOUBLE_LENGTH
    s = ("" + Double.MIN_VALUE * -4.0 / 3.0);
    longest = s.length();
    longestS = s;
    for (int i = 0; i < 1000; i++) {
      s = "" + (Math.random() / -1e150);
      if (s.length() > longest) {
        longest = s.length();
        longestS = s;
      }
    }
    String2.log("double longestS=" + longestS + " length=" + longest);
    Test.ensureTrue(longest <= 24, "");

    // make a new table
    String name = EDStatic.config.fullTestCacheDirectory + "testPersistentTable.txt";
    File2.delete(name);
    int widths[] = {
      PersistentTable.BOOLEAN_LENGTH,
      PersistentTable.BYTE_LENGTH,
      PersistentTable.BINARY_BYTE_LENGTH,
      PersistentTable.BINARY_CHAR_LENGTH,
      PersistentTable.SHORT_LENGTH,
      PersistentTable.BINARY_SHORT_LENGTH,
      PersistentTable.INT_LENGTH,
      PersistentTable.BINARY_INT_LENGTH,
      PersistentTable.LONG_LENGTH,
      PersistentTable.BINARY_LONG_LENGTH,
      PersistentTable.FLOAT_LENGTH,
      PersistentTable.BINARY_FLOAT_LENGTH,
      PersistentTable.DOUBLE_LENGTH,
      PersistentTable.BINARY_DOUBLE_LENGTH,
      20
    };

    PersistentTable pt = new PersistentTable(name, "rw", widths);
    Test.ensureEqual(pt.nRows(), 0, "");
    Test.ensureEqual(pt.addRows(2), 2, "");
    String testS = "Now is the time f\u0F22r all good countrymen to come ...";
    pt.writeBinaryByte(2, 0, Byte.MIN_VALUE);
    pt.writeBinaryByte(2, 1, Byte.MAX_VALUE);
    pt.writeInt(6, 0, Integer.MIN_VALUE);
    pt.writeInt(6, 1, Integer.MAX_VALUE);
    pt.writeString(14, 0, "");
    pt.writeString(14, 1, testS);

    Test.ensureEqual(pt.readBinaryByte(2, 0), Byte.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryByte(2, 1), Byte.MAX_VALUE, "");
    Test.ensureEqual(pt.readInt(6, 0), Integer.MIN_VALUE, "");
    Test.ensureEqual(pt.readInt(6, 1), Integer.MAX_VALUE, "");
    Test.ensureEqual(pt.readString(14, 0), "", "");
    // only 18 char returned because one takes 3 bytes in UTF-8
    Test.ensureEqual(pt.readString(14, 1), testS.substring(0, 18), "");
    pt.close();

    // reopen the file data still there?
    pt = new PersistentTable(name, "rw", widths);
    Test.ensureEqual(pt.nRows(), 2, "");
    Test.ensureEqual(pt.readBinaryByte(2, 0), Byte.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryByte(2, 1), Byte.MAX_VALUE, "");
    Test.ensureEqual(pt.readInt(6, 0), Integer.MIN_VALUE, "");
    Test.ensureEqual(pt.readInt(6, 1), Integer.MAX_VALUE, "");
    Test.ensureEqual(pt.readString(14, 0), "", "");
    // only 18 char returned because one takes 3 bytes in UTF-8
    Test.ensureEqual(pt.readString(14, 1), testS.substring(0, 18), "");
    pt.close();

    // rws is rw + synchronized update of metadata
    // rwd is rw + synchronized write to underlying storage device
    String modes[] = {"rw", "rw", "rws", "rwd"};
    n = 1000;
    for (int mode = 0; mode < modes.length; mode++) {
      File2.delete(name);
      pt =
          new PersistentTable(
              name,
              modes[mode],
              new int[] {
                80,
                PersistentTable.BINARY_DOUBLE_LENGTH,
                PersistentTable.DOUBLE_LENGTH,
                PersistentTable.BINARY_INT_LENGTH,
                PersistentTable.INT_LENGTH
              });
      pt.addRows(n);
      if (mode == 1) String2.log("*** Note: 2nd rw test uses flush()");
      // 2018-09-13 times adjusted for Lenovo and Java 1.8

      // string speed test
      time = System.currentTimeMillis();
      long modeTime = System.currentTimeMillis();
      for (int i = 0; i < n; i++) pt.writeString(0, i, testS + i);
      if (mode == 1) pt.flush();
      time = System.currentTimeMillis();
      String2.log(
          "\n"
              + modes[mode]
              + " time to write "
              + n
              + " Strings="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {0, 0, 0, 0}[mode]
              + "ms)"); // java 17 java 1.6 0,0,0,0

      for (int i = 0; i < n; i++) {
        int tRow = Math2.random(n);
        Test.ensureEqual(pt.readString(0, tRow), testS + tRow, "");
      }
      String2.log(
          modes[mode]
              + " time to read "
              + n
              + " Strings="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {22, 30, 10, 6}[mode]
              + "ms)"); // java 17 22,30,10,6 java 1.6 15,16,47,15

      // int speed test
      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++) pt.writeInt(4, i, i);
      if (mode == 1) pt.flush();
      String2.log(
          modes[mode]
              + " time to write "
              + n
              + " ints="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {15, 136, 148, 137}[mode]
              + "ms)"); // java 17 15,136,148,137 java 8 16,110,109,106 java 1.6
      // 16,0,219,219

      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++) {
        int tRow = Math2.random(n);
        Test.ensureEqual(pt.readInt(4, tRow), tRow, "");
      }
      String2.log(
          modes[mode]
              + " time to read "
              + n
              + " ints="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {9, 25, 6, 5}[mode]
              + "ms)"); // java 17 9,25,6,5 java 1.6 0,16,0,0

      modeTime = System.currentTimeMillis() - modeTime;
      String2.log("mode=" + mode + " time=" + modeTime + "ms");
      // TODO set up a better way to do performance testing
      // Test.ensureTrue(modeTime < 2 * expected[mode],
      //     modes[mode] + " TOTAL time to read " + n + " items=" +
      //         modeTime + "ms  (" + expected[mode] + "ms)\n" +
      //         "That is too slow! But it is usually fast enough when I run the test by itself.");

      pt.close();
    }
    // String2.pressEnterToContinue("\n(considerable variation) Okay?");
  }
}
