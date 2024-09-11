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
    String s = ("" + Float.MIN_VALUE * -4f / 3f);
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
    String name = EDStatic.fullTestCacheDirectory + "testPersistentTable.txt";
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
    pt.writeBoolean(0, 0, true);
    pt.writeBoolean(0, 1, false);
    pt.writeByte(1, 0, Byte.MIN_VALUE);
    pt.writeByte(1, 1, Byte.MAX_VALUE);
    pt.writeBinaryByte(2, 0, Byte.MIN_VALUE);
    pt.writeBinaryByte(2, 1, Byte.MAX_VALUE);
    pt.writeBinaryChar(3, 0, ' '); // hard because read will trim it to ""
    pt.writeBinaryChar(3, 1, '\u0F22');
    pt.writeShort(4, 0, Short.MIN_VALUE);
    pt.writeShort(4, 1, Short.MAX_VALUE);
    pt.writeBinaryShort(5, 0, Short.MIN_VALUE);
    pt.writeBinaryShort(5, 1, Short.MAX_VALUE);
    pt.writeInt(6, 0, Integer.MIN_VALUE);
    pt.writeInt(6, 1, Integer.MAX_VALUE);
    pt.writeBinaryInt(7, 0, Integer.MIN_VALUE);
    pt.writeBinaryInt(7, 1, Integer.MAX_VALUE);
    pt.writeLong(8, 0, Long.MIN_VALUE);
    pt.writeLong(8, 1, Long.MAX_VALUE);
    pt.writeBinaryLong(9, 0, Long.MIN_VALUE);
    pt.writeBinaryLong(9, 1, Long.MAX_VALUE);
    pt.writeFloat(10, 0, -Float.MAX_VALUE);
    pt.writeFloat(10, 1, Float.NaN);
    pt.writeBinaryFloat(11, 0, -Float.MAX_VALUE);
    pt.writeBinaryFloat(11, 1, Float.NaN);
    pt.writeDouble(12, 0, -Double.MAX_VALUE);
    pt.writeDouble(12, 1, Double.NaN);
    pt.writeBinaryDouble(13, 0, -Double.MAX_VALUE);
    pt.writeBinaryDouble(13, 1, Double.NaN);
    pt.writeString(14, 0, "");
    pt.writeString(14, 1, testS);

    Test.ensureEqual(pt.readBoolean(0, 0), true, "");
    Test.ensureEqual(pt.readBoolean(0, 1), false, "");
    Test.ensureEqual(pt.readByte(1, 0), Byte.MIN_VALUE, "");
    Test.ensureEqual(pt.readByte(1, 1), Byte.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryByte(2, 0), Byte.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryByte(2, 1), Byte.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryChar(3, 0), ' ', "");
    Test.ensureEqual(pt.readBinaryChar(3, 1), '\u0F22', "");
    Test.ensureEqual(pt.readShort(4, 0), Short.MIN_VALUE, "");
    Test.ensureEqual(pt.readShort(4, 1), Short.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryShort(5, 0), Short.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryShort(5, 1), Short.MAX_VALUE, "");
    Test.ensureEqual(pt.readInt(6, 0), Integer.MIN_VALUE, "");
    Test.ensureEqual(pt.readInt(6, 1), Integer.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryInt(7, 0), Integer.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryInt(7, 1), Integer.MAX_VALUE, "");
    Test.ensureEqual(pt.readLong(8, 0), Long.MIN_VALUE, "");
    Test.ensureEqual(pt.readLong(8, 1), Long.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryLong(9, 0), Long.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryLong(9, 1), Long.MAX_VALUE, "");
    Test.ensureEqual(pt.readFloat(10, 0), -Float.MAX_VALUE, "");
    Test.ensureEqual(pt.readFloat(10, 1), Float.NaN, "");
    Test.ensureEqual(pt.readBinaryFloat(11, 0), -Float.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryFloat(11, 1), Float.NaN, "");
    Test.ensureEqual(pt.readDouble(12, 0), -Double.MAX_VALUE, "");
    Test.ensureEqual(pt.readDouble(12, 1), Double.NaN, "");
    Test.ensureEqual(pt.readBinaryDouble(13, 0), -Double.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryDouble(13, 1), Double.NaN, "");
    Test.ensureEqual(pt.readString(14, 0), "", "");
    // only 18 char returned because one takes 3 bytes in UTF-8
    Test.ensureEqual(pt.readString(14, 1), testS.substring(0, 18), "");
    pt.close();

    // reopen the file data still there?
    pt = new PersistentTable(name, "rw", widths);
    Test.ensureEqual(pt.nRows(), 2, "");
    Test.ensureEqual(pt.readBoolean(0, 0), true, "");
    Test.ensureEqual(pt.readBoolean(0, 1), false, "");
    Test.ensureEqual(pt.readByte(1, 0), Byte.MIN_VALUE, "");
    Test.ensureEqual(pt.readByte(1, 1), Byte.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryByte(2, 0), Byte.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryByte(2, 1), Byte.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryChar(3, 0), ' ', "");
    Test.ensureEqual(pt.readBinaryChar(3, 1), '\u0F22', "");
    Test.ensureEqual(pt.readShort(4, 0), Short.MIN_VALUE, "");
    Test.ensureEqual(pt.readShort(4, 1), Short.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryShort(5, 0), Short.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryShort(5, 1), Short.MAX_VALUE, "");
    Test.ensureEqual(pt.readInt(6, 0), Integer.MIN_VALUE, "");
    Test.ensureEqual(pt.readInt(6, 1), Integer.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryInt(7, 0), Integer.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryInt(7, 1), Integer.MAX_VALUE, "");
    Test.ensureEqual(pt.readLong(8, 0), Long.MIN_VALUE, "");
    Test.ensureEqual(pt.readLong(8, 1), Long.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryLong(9, 0), Long.MIN_VALUE, "");
    Test.ensureEqual(pt.readBinaryLong(9, 1), Long.MAX_VALUE, "");
    Test.ensureEqual(pt.readFloat(10, 0), -Float.MAX_VALUE, "");
    Test.ensureEqual(pt.readFloat(10, 1), Float.NaN, "");
    Test.ensureEqual(pt.readBinaryFloat(11, 0), -Float.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryFloat(11, 1), Float.NaN, "");
    Test.ensureEqual(pt.readDouble(12, 0), -Double.MAX_VALUE, "");
    Test.ensureEqual(pt.readDouble(12, 1), Double.NaN, "");
    Test.ensureEqual(pt.readBinaryDouble(13, 0), -Double.MAX_VALUE, "");
    Test.ensureEqual(pt.readBinaryDouble(13, 1), Double.NaN, "");
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

      // double speed test
      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++) pt.writeDouble(2, i, i);
      if (mode == 1) pt.flush();
      String2.log(
          modes[mode]
              + " time to write "
              + n
              + " doubles="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {17, 145, 135, 135}[mode]
              + "ms)"); // java 17 17,145,135,135 java 1.6 16,130,96,109

      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++) {
        int tRow = Math2.random(n);
        Test.ensureEqual(pt.readDouble(2, tRow), tRow, "");
      }
      String2.log(
          modes[mode]
              + " time to read "
              + n
              + " doubles="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {17, 21, 6, 9}[mode]
              + "ms)"); // java 17 17,21,6,9 java 1.6 15,32,16,0

      // binary double speed test
      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++) pt.writeBinaryDouble(1, i, i);
      if (mode == 1) pt.flush();
      String2.log(
          modes[mode]
              + " time to write "
              + n
              + " binary doubles="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {45, 158, 969, 1106}[mode]
              + "ms)"); // java 17 45,158,969,1106 java 8 31,125,770,772 java 1.6
      // 16,31,1968,1687

      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++) {
        int tRow = Math2.random(n);
        Test.ensureEqual(pt.readBinaryDouble(1, tRow), tRow, "");
      }
      String2.log(
          modes[mode]
              + " time to read "
              + n
              + " binary doubles="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {41, 72, 27, 29}[mode]
              + "ms)"); // java 17 41,72,27,29 java 1.6 16,31,16,16

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

      // binary int speed test
      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++) pt.writeBinaryInt(3, i, i);
      if (mode == 1) pt.flush();
      String2.log(
          modes[mode]
              + " time to write "
              + n
              + " binary int="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {22, 119, 503, 569}[mode]
              + "ms)"); // java 17 22,119,503,137 java 8 15,125,360,402 java 1.6
      // 15,1531,922

      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++) {
        int tRow = Math2.random(n);
        Test.ensureEqual(pt.readBinaryInt(3, tRow), tRow, "");
      }
      String2.log(
          modes[mode]
              + " time to read "
              + n
              + " binary int="
              + (System.currentTimeMillis() - time)
              + "   ("
              + new int[] {31, 38, 15, 13}[mode]
              + "ms)"); // java 17 31,38,15,13 java 1.6 16,16,16,3

      int expected[] = {
        203, 744, 1819, 2063
      }; // java 17 203,744,1819,2063 java 1.6 140,693,1491,1615
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
