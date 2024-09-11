package com.cohort.array;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.BitSet;

class PrimitiveArrayTests {
  /**
   * This tests the methods of this class.
   *
   * @throws Exception if trouble.
   */
  @org.junit.jupiter.api.Test
  void testNccsv() throws Throwable {
    String2.log("*** PrimitiveArray.testNccsv");
    String s;
    StringArray sa;
    PrimitiveArray pa;
    String msg;

    // String2.toNccsvChar
    Test.ensureEqual(String2.toNccsvChar(' '), " ", "");
    Test.ensureEqual(String2.toNccsvChar('\t'), "\\t", "");
    Test.ensureEqual(String2.toNccsvChar('a'), "a", "");
    Test.ensureEqual(String2.toNccsvChar('µ'), "µ", ""); // #181
    Test.ensureEqual(String2.toNccsvChar('\u20AC'), "€", "");
    Test.ensureEqual(String2.toNccsvChar('\u0081'), "\u0081", ""); // now left as is

    // String2.toNccsv127Char
    Test.ensureEqual(String2.toNccsv127Char(' '), " ", "");
    Test.ensureEqual(String2.toNccsv127Char('\t'), "\\t", "");
    Test.ensureEqual(String2.toNccsv127Char('a'), "a", "");
    Test.ensureEqual(String2.toNccsv127Char('µ'), "\\u00b5", ""); // #181
    Test.ensureEqual(String2.toNccsv127Char('€'), "\\u20ac", "");
    Test.ensureEqual(String2.toNccsv127Char('\u0081'), "\\u0081", ""); // not defined in Unicode

    // String2.toNccsvDataString won't be quoted
    Test.ensureEqual(String2.toNccsvDataString(""), "", "");
    Test.ensureEqual(String2.toNccsvDataString("a\n\f\t\r"), "a\\n\\f\\t\\r", ""); // special char
    Test.ensureEqual(String2.toNccsvDataString("a"), "a", "");
    Test.ensureEqual(String2.toNccsvDataString("a ~ µ€"), "a ~ µ€", "");
    Test.ensureEqual(String2.toNccsvDataString("a"), "a", "");
    Test.ensureEqual(String2.toNccsvDataString("5"), "5", ""); // number
    Test.ensureEqual(String2.toNccsvDataString("'c'"), "'c'", ""); // char

    // String2.toNccsvDataString will be quoted
    Test.ensureEqual(String2.toNccsvDataString(" "), "\" \"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsvDataString("aµ€ "), "\"aµ€ \"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsvDataString(" b"), "\" b\"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsvDataString("a,"), "\"a,\"", ""); // ,
    Test.ensureEqual(String2.toNccsvDataString("b\""), "\"b\"\"\"", ""); // "
    Test.ensureEqual(
        String2.toNccsvDataString("\\ \f\n\r\t\"' z\u0000\uffffÿ\u20ac"),
        "\"\\\\ \\f\\n\\r\\t\"\"' z\\u0000\uffff\u00ff€\"",
        "");

    // String2.toNccsv127DataString won't be quoted
    Test.ensureEqual(String2.toNccsv127DataString(""), "", "");
    Test.ensureEqual(String2.toNccsv127DataString("a"), "a", "");
    Test.ensureEqual(String2.toNccsv127DataString("5"), "5", ""); // number
    Test.ensureEqual(String2.toNccsv127DataString("'c'"), "'c'", ""); // char
    Test.ensureEqual(String2.toNccsv127DataString("aµ€"), "a\\u00b5\\u20ac", "");
    Test.ensureEqual(String2.toNccsv127DataString("a\n\f\t\r"), "a\\n\\f\\t\\r", "");

    // String2.toNccsv127DataString will be quoted
    Test.ensureEqual(String2.toNccsv127DataString(" "), "\" \"", ""); // start/end ' '
    Test.ensureEqual(
        String2.toNccsv127DataString("aµ€ "), "\"a\\u00b5\\u20ac \"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsv127DataString(" b"), "\" b\"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsv127DataString("a,"), "\"a,\"", ""); // ,
    Test.ensureEqual(String2.toNccsv127DataString("b\""), "\"b\"\"\"", ""); // "
    Test.ensureEqual(
        String2.toNccsv127DataString("\\ \f\n\r\t\"' z\u0000\uffffÿ\u20ac"),
        "\"\\\\ \\f\\n\\r\\t\"\"' z\\u0000\\uffff\\u00ff\\u20ac\"",
        "");

    // String2.toNccsvAttString won't be quoted
    Test.ensureEqual(String2.toNccsvAttString(""), "", "");
    Test.ensureEqual(String2.toNccsvAttString("a"), "a", "");
    Test.ensureEqual(String2.toNccsvAttString("a ~ µ€"), "a ~ µ€", "");
    s = String2.toNccsvAttString("a\n\f\t\r");
    Test.ensureEqual(s, "a\\n\\f\\t\\r", s);
    Test.ensureEqual(String2.toNccsvAttString("a"), "a", "");
    Test.ensureEqual(String2.toNccsvAttString("aµ€"), "aµ€", "");

    // String2.toNccsvAttString will be quoted
    Test.ensureEqual(String2.toNccsvAttString(" "), "\" \"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsvAttString(" b"), "\" b\"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsvAttString("a,"), "\"a,\"", ""); // ,
    Test.ensureEqual(String2.toNccsvAttString("b\""), "\"b\"\"\"", ""); // "
    Test.ensureEqual(String2.toNccsvAttString("\'c\'"), "\"'c'\"", ""); // char
    Test.ensureEqual(String2.toNccsvAttString("5"), "\"5\"", ""); // number

    // String2.toNccsv127AttString won't be quoted
    Test.ensureEqual(String2.toNccsv127AttString(""), "", "");
    Test.ensureEqual(String2.toNccsv127AttString("a"), "a", "");
    Test.ensureEqual(String2.toNccsv127AttString("a ~ µ€"), "a ~ \\u00b5\\u20ac", "");
    Test.ensureEqual(String2.toNccsv127AttString("a"), "a", "");
    Test.ensureEqual(String2.toNccsv127AttString("a\n\f\t\r"), "a\\n\\f\\t\\r", "");
    Test.ensureEqual(String2.toNccsv127AttString("aµ€"), "a\\u00b5\\u20ac", "");

    // String2.toNccsv127AttString will be quoted
    Test.ensureEqual(String2.toNccsv127AttString(" "), "\" \"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsv127AttString(" b"), "\" b\"", ""); // start/end ' '
    Test.ensureEqual(String2.toNccsv127AttString("a,"), "\"a,\"", ""); // ,
    Test.ensureEqual(String2.toNccsv127AttString("b\""), "\"b\"\"\"", ""); // "
    Test.ensureEqual(String2.toNccsv127AttString("\'c\'"), "\"'c'\"", ""); // char
    Test.ensureEqual(String2.toNccsv127AttString("5"), "\"5\"", ""); // number

    // ByteArray
    s = "1b";
    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv(s));
    Test.ensureEqual(pa.elementTypeString(), "byte", "");
    Test.ensureEqual(pa.toString(), "1", "");
    Test.ensureEqual(pa.toNccsvAttString(), s, "");

    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("-128b,-0b,0b,127b"));
    Test.ensureEqual(pa.elementTypeString(), "byte", "");
    Test.ensureEqual(pa.toString(), "-128, 0, 0, 127", "");
    Test.ensureEqual(pa.toNccsvAttString(), "-128b,0b,0b,127b", "");

    try {
      pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("128b"));
      msg = "shouldn't get here";
    } catch (Throwable t) {
      msg = t.toString();
    }
    Test.ensureEqual(msg, "com.cohort.util.SimpleException: Invalid byte value: 128b", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1b,3")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1b, 3", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1b,1234b")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1b, 1234b", "");

    // ShortArray
    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("1s"));
    Test.ensureEqual(pa.elementTypeString(), "short", "");
    Test.ensureEqual(pa.toString(), "1", "");
    Test.ensureEqual(pa.toNccsvAttString(), "1s", "");

    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("-32768s,-0s,0s,32767s"));
    Test.ensureEqual(pa.elementTypeString(), "short", "");
    Test.ensureEqual(pa.toString(), "-32768, 0, 0, 32767", "");
    Test.ensureEqual(pa.toNccsvAttString(), "-32768s,0s,0s,32767s", "");

    try {
      pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("32768s"));
      msg = "shouldn't get here";
    } catch (Throwable t) {
      msg = t.toString();
    }
    Test.ensureEqual(msg, "com.cohort.util.SimpleException: Invalid short value: 32768s", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1s,3")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1s, 3", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1s,123456s")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1s, 123456s", "");

    // IntArray
    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("1i"));
    Test.ensureEqual(pa.elementTypeString(), "int", "");
    Test.ensureEqual(pa.toString(), "1", "");
    Test.ensureEqual(pa.toNccsvAttString(), "1i", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("-2147483648i,-0i,0i,2147483647i"));
    Test.ensureEqual(pa.elementTypeString(), "int", "");
    Test.ensureEqual(pa.toString(), "-2147483648, 0, 0, 2147483647", "");
    Test.ensureEqual(pa.toNccsvAttString(), "-2147483648i,0i,0i,2147483647i", "");

    try {
      pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("2147483648i"));
      msg = "shouldn't get here";
    } catch (Throwable t) {
      msg = t.toString();
    }
    Test.ensureEqual(msg, "com.cohort.util.SimpleException: Invalid int value: 2147483648i", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1i,123456789091i")); // doesn't match
    // regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1i, 123456789091i", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1i,3.00i")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1i, 3.00i", "");

    // LongArray
    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("1L"));
    Test.ensureEqual(pa.elementTypeString(), "long", "");
    Test.ensureEqual(pa.toString(), "1", "");
    Test.ensureEqual(pa.toNccsvAttString(), "1L", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("-9223372036854775808L,-0L,0L,9223372036854775807L"));
    Test.ensureEqual(pa.elementTypeString(), "long", "");
    Test.ensureEqual(pa.toString(), "-9223372036854775808, 0, 0, 9223372036854775807", "");
    Test.ensureEqual(pa.toNccsvAttString(), "-9223372036854775808L,0L,0L,9223372036854775807L", "");

    try {
      pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("9223372036854775808L"));
      msg = "shouldn't get here";
    } catch (Throwable t) {
      msg = t.toString();
    }
    Test.ensureEqual(
        msg, "com.cohort.util.SimpleException: Invalid long value: 9223372036854775808L", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1L,12345678901234567890L")); // doesn't
    // match
    // regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1L, 12345678901234567890L", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1L,123456")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1L, 123456", "");

    // FloatArray
    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("1f"));
    Test.ensureEqual(pa.elementTypeString(), "float", "");
    Test.ensureEqual(pa.toString(), "1.0", "");
    Test.ensureEqual(pa.toNccsvAttString(), "1.0f", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("-3e-38f,-.12e3f,0f,3e0f,3.e4f,.12e+3f,1.2E38f,NaNf"));
    Test.ensureEqual(pa.elementTypeString(), "float", "");
    Test.ensureEqual(pa.toString(), "-3.0E-38, -120.0, 0.0, 3.0, 30000.0, 120.0, 1.2E38, NaN", "");
    Test.ensureEqual(
        pa.toNccsvAttString(), "-3.0E-38f,-120.0f,0.0f,3.0f,30000.0f,120.0f,1.2E38f,NaNf", "");

    try {
      pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("1.2E39f"));
      msg = "shouldn't get here";
    } catch (Throwable t) {
      msg = t.toString();
    }
    Test.ensureEqual(msg, "com.cohort.util.SimpleException: Invalid float value: 1.2E39f", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1f,3..0e23f")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1f, 3..0e23f", "");

    // DoubleArray
    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("1d"));
    Test.ensureEqual(pa.elementTypeString(), "double", "");
    Test.ensureEqual(pa.toString(), "1.0", "");
    Test.ensureEqual(pa.toNccsvAttString(), "1.0d", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("-3.0e-300d,-.12e3d,1.d,.1d,3.e4d,.12e3d,1.2E+300d,NaNd"));
    Test.ensureEqual(pa.elementTypeString(), "double", "");
    Test.ensureEqual(
        pa.toString(), "-3.0E-300, -120.0, 1.0, 0.1, 30000.0, 120.0, 1.2E300, NaN", "");
    Test.ensureEqual(
        pa.toNccsvAttString(), "-3.0E-300d,-120.0d,1.0d,0.1d,30000.0d,120.0d,1.2E300d,NaNd", "");

    try {
      pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("1.e310d"));
      msg = "shouldn't get here";
    } catch (Throwable t) {
      msg = t.toString();
    }
    Test.ensureEqual(msg, "com.cohort.util.SimpleException: Invalid double value: 1.e310d", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("3.0d,3..0d")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "3.0d, 3..0d", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("1.0d,3")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "1.0d, 3", "");

    // StringArray
    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv(
                // in the nccsv file, it's a string with characters like \
                "\"a~ \\f \\n \\r \\t \\\\ \\/ \\u00C0 \\u0000 \\uffFf\""));
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    // now it's a string with control chars and unicode chars
    Test.ensureEqual(
        String2.annotatedString(pa.getString(0)),
        "a~ [12] [10]\n" + " [13] [9] \\ / [192] [0] [65535][end]",
        "");
    Test.ensureEqual(pa.toNccsvAttString(), "a~ \\f \\n \\r \\t \\\\ / \u00c0 \\u0000 \uffff", "");

    // CharArray
    pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("\"'a'\""));
    Test.ensureEqual(pa.elementTypeString(), "char", "");
    Test.ensureEqual(pa.toString(), "a", "");
    Test.ensureEqual(pa.toNccsvAttString(), "\"'a'\"", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv( // \\b is not supported
                "\"'\\f'\", \"'\\n'\", \"'\\r'\", \"'\\t'\", \"'\\\\'\""));
    Test.ensureEqual(pa.elementTypeString(), "char", "");
    Test.ensureEqual(pa.toString(), "\\f, \\n, \\r, \\t, \\\\", "");
    Test.ensureEqual(
        pa.toNccsvAttString(), "\"'\\f'\",\"'\\n'\",\"'\\r'\",\"'\\t'\",\"'\\\\'\"", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv(
                "\"'\\/'\", \"'/'\", \"'\"\"'\", \"' '\", \"'''\", \"'a'\""));
    Test.ensureEqual(pa.elementTypeString(), "char", "");
    Test.ensureEqual(pa.toString(), "/, /, \"\"\"\", \" \", ', a", "");
    Test.ensureEqual(
        pa.toNccsvAttString(), "\"'/'\",\"'/'\",\"'\"\"'\",\"' '\",\"'''\",\"'a'\"", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("\"'~'\", '\\u00C0', \"'\\u0000'\", \"'\\uffFf'\""));
    Test.ensureEqual(pa.elementTypeString(), "char", "");
    Test.ensureEqual(pa.toString(), "~, \\u00c0, \\u0000, \\uffff", "");
    Test.ensureEqual(pa.toNccsvAttString(), "\"'~'\",\"'\u00c0'\",\"'\\u0000'\",\"'\uffff'\"", "");

    try {
      pa = PrimitiveArray.parseNccsvAttributes(StringArray.simpleFromNccsv("'\\b'"));
      msg = "shouldn't get here";
    } catch (Throwable t) {
      msg = t.toString();
    }
    // Test.ensureEqual(msg, "zztop", "");

    pa =
        PrimitiveArray.parseNccsvAttributes(
            StringArray.simpleFromNccsv("'a', ''")); // doesn't match regex
    Test.ensureEqual(pa.elementTypeString(), "String", "");
    Test.ensureEqual(pa.toString(), "'a', ''", "");
  }

  /**
   * This tests the methods of this class.
   *
   * @throws Exception if trouble.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("*** PrimitiveArray.basicTest");

    // test PrimitiveArray.factory
    PrimitiveArray pa;
    Test.ensureEqual(PrimitiveArray.factory(new byte[] {1}).elementType(), PAType.BYTE, "");
    Test.ensureEqual(PrimitiveArray.factory(new char[] {1}).elementType(), PAType.CHAR, "");
    Test.ensureEqual(PrimitiveArray.factory(new short[] {1}).elementType(), PAType.SHORT, "");
    Test.ensureEqual(PrimitiveArray.factory(new int[] {1}).elementType(), PAType.INT, "");
    Test.ensureEqual(PrimitiveArray.factory(new long[] {1}).elementType(), PAType.LONG, "");
    Test.ensureEqual(PrimitiveArray.factory(new float[] {1}).elementType(), PAType.FLOAT, "");
    Test.ensureEqual(PrimitiveArray.factory(new double[] {1}).elementType(), PAType.DOUBLE, "");
    Test.ensureEqual(PrimitiveArray.factory(new String[] {"1"}).elementType(), PAType.STRING, "");

    Test.ensureEqual(PrimitiveArray.factory(Byte.valueOf((byte) 1)).elementType(), PAType.BYTE, "");
    Test.ensureEqual(
        PrimitiveArray.factory(Character.valueOf((char) 1)).elementType(), PAType.CHAR, "");
    Test.ensureEqual(
        PrimitiveArray.factory(Short.valueOf((short) 1)).elementType(), PAType.SHORT, "");
    Test.ensureEqual(PrimitiveArray.factory(Integer.valueOf(1)).elementType(), PAType.INT, "");
    Test.ensureEqual(PrimitiveArray.factory(Long.valueOf(1)).elementType(), PAType.LONG, "");
    Test.ensureEqual(PrimitiveArray.factory(Float.valueOf(1)).elementType(), PAType.FLOAT, "");
    Test.ensureEqual(PrimitiveArray.factory(Double.valueOf(1)).elementType(), PAType.DOUBLE, "");
    Test.ensureEqual(PrimitiveArray.factory(new String("1")).elementType(), PAType.STRING, "");

    Test.ensureEqual(PrimitiveArray.factory(PAType.BYTE, 1, true).elementType(), PAType.BYTE, "");
    Test.ensureEqual(PrimitiveArray.factory(PAType.CHAR, 1, true).elementType(), PAType.CHAR, "");
    Test.ensureEqual(PrimitiveArray.factory(PAType.SHORT, 1, true).elementType(), PAType.SHORT, "");
    Test.ensureEqual(PrimitiveArray.factory(PAType.INT, 1, true).elementType(), PAType.INT, "");
    Test.ensureEqual(PrimitiveArray.factory(PAType.LONG, 1, true).elementType(), PAType.LONG, "");
    Test.ensureEqual(PrimitiveArray.factory(PAType.FLOAT, 1, true).elementType(), PAType.FLOAT, "");
    pa = PrimitiveArray.factory(PAType.DOUBLE, 1, true);
    Test.ensureEqual(pa.elementType(), PAType.DOUBLE, "");
    Test.ensureEqual(pa.getDouble(0), 0, "");
    pa = PrimitiveArray.factory(PAType.STRING, 1, true);
    Test.ensureEqual(pa.elementType(), PAType.STRING, "");
    Test.ensureEqual(pa.getString(0), "", "");

    Test.ensureEqual(PrimitiveArray.factory(PAType.BYTE, 1, "10").toString(), "10", "");
    Test.ensureEqual(PrimitiveArray.factory(PAType.CHAR, 2, "abc").toString(), "a, a", "");
    Test.ensureEqual(PrimitiveArray.factory(PAType.SHORT, 3, "30").toString(), "30, 30, 30", "");
    Test.ensureEqual(PrimitiveArray.factory(PAType.INT, 4, "40").toString(), "40, 40, 40, 40", "");
    Test.ensureEqual(
        PrimitiveArray.factory(PAType.LONG, 5, "50").toString(), "50, 50, 50, 50, 50", "");
    Test.ensureEqual(
        PrimitiveArray.factory(PAType.FLOAT, 6, "60").toString(),
        "60.0, 60.0, 60.0, 60.0, 60.0, 60.0",
        "");
    Test.ensureEqual(
        PrimitiveArray.factory(PAType.DOUBLE, 7, "70").toString(),
        "70.0, 70.0, 70.0, 70.0, 70.0, 70.0, 70.0",
        "");
    Test.ensureEqual(
        PrimitiveArray.factory(PAType.STRING, 8, "ab").toString(),
        "ab, ab, ab, ab, ab, ab, ab, ab",
        "");

    // test simplify
    pa = new StringArray(new String[] {"-127", "126", ".", "NaN", null});
    pa = pa.simplify("test1");
    Test.ensureTrue(pa instanceof ByteArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getDouble(0), -127, "");
    Test.ensureEqual(pa.getDouble(1), 126, "");
    Test.ensureEqual(pa.getDouble(2), Double.NaN, "");
    Test.ensureEqual(pa.getDouble(3), Double.NaN, "");
    Test.ensureEqual(pa.getDouble(4), Double.NaN, "");

    // pa = new StringArray(new String[]{"0", "65534", "."});
    // pa = pa.simplify();
    // Test.ensureTrue(pa instanceof CharArray, "elementType=" + pa.elementType());
    // Test.ensureEqual(pa.getDouble(0), 0, "");
    // Test.ensureEqual(pa.getDouble(1), 65534, "");
    // Test.ensureEqual(pa.getDouble(2), Character.MAX_VALUE, "");

    pa = new StringArray(new String[] {"-32767", "32766", "."});
    pa = pa.simplify("test2");
    Test.ensureTrue(pa instanceof ShortArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getDouble(0), -32767, "");
    Test.ensureEqual(pa.getDouble(1), 32766, "");
    Test.ensureEqual(pa.getDouble(2), Double.NaN, "");

    pa = new StringArray(new String[] {"-2000000000", "2000000000", "."});
    pa = pa.simplify("test3");
    Test.ensureTrue(pa instanceof IntArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getDouble(0), -2000000000, "");
    Test.ensureEqual(pa.getDouble(1), 2000000000, "");
    Test.ensureEqual(pa.getDouble(2), Double.NaN, "");

    pa = new StringArray(new String[] {"-2000000000000000", "2000000000000000", ""});
    pa = pa.simplify("test4");
    Test.ensureEqual(pa.elementTypeString(), "long", "elementType");
    Test.ensureEqual(pa.getString(0), "-2000000000000000", "");
    Test.ensureEqual(pa.getString(1), "2000000000000000", "");
    Test.ensureEqual(pa.getString(2), "", "");

    pa = new StringArray(new String[] {"-2000000000000000", "2000000000000000", "NaN"});
    pa = pa.simplify("test5");
    Test.ensureEqual(pa.elementTypeString(), "long", "elementType");
    Test.ensureEqual(pa.getString(0), "-2000000000000000", "");
    Test.ensureEqual(pa.getString(1), "2000000000000000", "");
    Test.ensureEqual(pa.getString(2), "", "");

    pa = new StringArray(new String[] {"-1e33", "1e33", "."});
    pa = pa.simplify("test6");
    Test.ensureTrue(pa instanceof FloatArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getDouble(0), -1e33f, ""); // 'f' bruises it
    Test.ensureEqual(pa.getDouble(1), 1e33f, ""); // 'f' bruises it
    Test.ensureEqual(pa.getDouble(2), Double.NaN, "");

    pa = new StringArray(new String[] {"-1e307", "1e307", "."});
    pa = pa.simplify("test7");
    Test.ensureTrue(pa instanceof DoubleArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getDouble(0), -1e307, "");
    Test.ensureEqual(pa.getDouble(1), 1e307, "");
    Test.ensureEqual(pa.getDouble(2), Double.NaN, "");

    pa = new StringArray(new String[] {".", "123", "4b"});
    pa = pa.simplify("test8");
    Test.ensureTrue(pa instanceof StringArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getString(0), ".", "");
    Test.ensureEqual(pa.getString(1), "123", "");
    Test.ensureEqual(pa.getString(2), "4b", "");

    pa = new StringArray(new String[] {".", "33.0"}); // with internal "." -> float
    pa = pa.simplify("test9");
    Test.ensureTrue(pa instanceof FloatArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getString(0), "", "");
    Test.ensureEqual(pa.getString(1), "33.0", "");

    pa = new StringArray(new String[] {".", "33"}); // no internal ".", can be integer type
    pa = pa.simplify("test10");
    Test.ensureTrue(pa instanceof ByteArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getString(0), "", "");
    Test.ensureEqual(pa.getString(1), "33", "");

    pa = new DoubleArray(new double[] {Double.NaN, 123.4, 12});
    pa = pa.simplify("test11");
    Test.ensureTrue(pa instanceof FloatArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getFloat(0), Float.NaN, "");
    Test.ensureEqual(pa.getFloat(1), 123.4f, "");
    Test.ensureEqual(pa.getFloat(2), 12f, "");

    pa = new DoubleArray(new double[] {Double.NaN, 100000, 12});
    pa = pa.simplify("test12");
    Test.ensureTrue(pa instanceof IntArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(pa.getInt(1), 100000, "");
    Test.ensureEqual(pa.getInt(2), 12, "");

    pa = new DoubleArray(new double[] {Double.NaN, 100, 12});
    pa = pa.simplify("test13");
    Test.ensureTrue(pa instanceof ByteArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(pa.getInt(1), 100, "");
    Test.ensureEqual(pa.getInt(2), 12, "");

    pa = new IntArray(new int[] {Integer.MAX_VALUE, 100, 12});
    pa.setMaxIsMV(true);
    pa = pa.simplify("test14");
    Test.ensureTrue(pa instanceof ByteArray, "elementType=" + pa.elementType());
    Test.ensureEqual(pa.getInt(0), Integer.MAX_VALUE, "");
    Test.ensureEqual(pa.getInt(1), 100, "");
    Test.ensureEqual(pa.getInt(2), 12, "");

    // test rank
    ByteArray arByte = new ByteArray(new byte[] {0, 100, 50, 110});
    FloatArray arFloat = new FloatArray(new float[] {1, 3, 3, -5});
    DoubleArray arDouble = new DoubleArray(new double[] {17, 1e300, 3, 0});
    StringArray arString = new StringArray(new String[] {"a", "abe", "A", "ABE"});
    ArrayList table = String2.toArrayList(new Object[] {arByte, arFloat, arDouble, arString});
    Test.ensureEqual(
        PrimitiveArray.rank(table, new int[] {0}, new boolean[] {true}), // ascending
        new int[] {0, 2, 1, 3},
        "");
    Test.ensureEqual(
        PrimitiveArray.rank(table, new int[] {0}, new boolean[] {false}), // descending
        new int[] {3, 1, 2, 0},
        "");
    Test.ensureEqual(
        PrimitiveArray.rank(table, new int[] {1}, new boolean[] {true}), // ties
        new int[] {3, 0, 1, 2},
        "");
    Test.ensureEqual(
        PrimitiveArray.rank(table, new int[] {2}, new boolean[] {true}),
        new int[] {3, 2, 0, 1},
        "");
    Test.ensureEqual(
        PrimitiveArray.rank(table, new int[] {3}, new boolean[] {true}),
        new int[] {2, 3, 0, 1},
        "");
    Test.ensureEqual(
        PrimitiveArray.rank(table, new int[] {1, 0}, new boolean[] {true, true}), // tie,
        // a/ascending
        new int[] {3, 0, 2, 1},
        "");
    Test.ensureEqual(
        PrimitiveArray.rank(table, new int[] {1, 0}, new boolean[] {true, false}), // tie,
        // a/descending
        new int[] {3, 0, 1, 2},
        "");
    Test.ensureEqual(arByte.elementType(), PAType.BYTE, "");
    Test.ensureEqual(arFloat.elementType(), PAType.FLOAT, "");
    Test.ensureEqual(arDouble.elementType(), PAType.DOUBLE, "");
    Test.ensureEqual(arString.elementType(), PAType.STRING, "");

    Test.ensureEqual(arByte.elementTypeString(), "byte", "");
    Test.ensureEqual(arFloat.elementTypeString(), "float", "");
    Test.ensureEqual(arDouble.elementTypeString(), "double", "");
    Test.ensureEqual(arString.elementTypeString(), "String", "");

    // test sort result = {3, 0, 2, 1});
    PrimitiveArray.sort(table, new int[] {1, 0}, new boolean[] {true, true}); // tie, a/ascending
    Test.ensureEqual(arByte.array, new byte[] {110, 0, 50, 100}, ""); // col0
    Test.ensureEqual(arFloat.array, new float[] {-5, 1, 3, 3}, ""); // col1
    Test.ensureEqual(arDouble.array, new double[] {0, 17, 3, 1e300}, ""); // col2
    for (int i = 0; i < 4; i++)
      Test.ensureEqual(
          arString.get(i), (new String[] {"ABE", "a", "A", "abe"})[i], "i=" + i); // col3

    // test sort result = {3, 0, 1, 2});
    PrimitiveArray.sort(table, new int[] {1, 0}, new boolean[] {true, false}); // tie, a/descending
    Test.ensureEqual(arByte.array, new byte[] {110, 0, 100, 50}, "");
    Test.ensureEqual(arFloat.array, new float[] {-5, 1, 3, 3}, "");
    Test.ensureEqual(arDouble.array, new double[] {0, 17, 1e300, 3}, "");
    for (int i = 0; i < 4; i++)
      Test.ensureEqual(arString.get(i), (new String[] {"ABE", "a", "abe", "A"})[i], "i=" + i);

    // test sortIgnoreCase
    PrimitiveArray.sortIgnoreCase(table, new int[] {3}, new boolean[] {true});
    for (int i = 0; i < 4; i++)
      Test.ensureEqual(
          arString.get(i),
          (new String[] {"A", "a", "ABE", "abe"})[i],
          "i=" + i + " arString=" + arString); // col3
    Test.ensureEqual(arByte.array, new byte[] {50, 0, 110, 100}, ""); // col0
    Test.ensureEqual(arFloat.array, new float[] {3, 1, -5, 3}, ""); // col1
    Test.ensureEqual(arDouble.array, new double[] {3, 17, 0, 1e300}, ""); // col2

    // test sortIgnoreCase
    PrimitiveArray.sortIgnoreCase(table, new int[] {3}, new boolean[] {false});
    for (int i = 0; i < 4; i++)
      Test.ensureEqual(
          arString.get(i), (new String[] {"abe", "ABE", "a", "A"})[i], "i=" + i); // col3
    Test.ensureEqual(arByte.array, new byte[] {100, 110, 0, 50}, ""); // col0
    Test.ensureEqual(arFloat.array, new float[] {3, -5, 1, 3}, ""); // col1
    Test.ensureEqual(arDouble.array, new double[] {1e300, 0, 17, 3}, ""); // col2

    // test rank this pa
    arFloat = new FloatArray(new float[] {3, 1, 3, Float.NaN, -5});
    int rank[] = arFloat.rank(true); // ascending?
    Test.ensureEqual(
        rank, new int[] {4, 1, 0, 2, 3}, ""); // NaN PrimitiveArray.ranks like big positive number

    // test removeDuplicates
    IntArray arInt3a = new IntArray(new int[] {1, 5, 5, 7, 7, 7});
    IntArray arInt3b = new IntArray(new int[] {2, 6, 6, 8, 8, 8});
    ArrayList table3 = String2.toArrayList(new Object[] {arInt3a, arInt3b});
    PrimitiveArray.removeDuplicates(table3);
    Test.ensureEqual(arInt3a.toString(), "1, 5, 7", "");
    Test.ensureEqual(arInt3b.toString(), "2, 6, 8", "");

    // test merge (which tests append and sort)
    ByteArray arByte2 = new ByteArray(new byte[] {5, 15, 50, 25});
    FloatArray arFloat2 = new FloatArray(new float[] {4, 14, 3, 24});
    IntArray arInt2 = new IntArray(new int[] {3, 13, 3, 1}); // test: narrower than arDouble
    StringArray arString2 = new StringArray(new String[] {"b", "aa", "A", "c"});
    ArrayList table2 = String2.toArrayList(new Object[] {arByte2, arFloat2, arInt2, arString2});
    PrimitiveArray.merge(table2, table, new int[] {1, 0}, new boolean[] {true, true}, false);
    Test.ensureEqual(
        ((PrimitiveArray) table2.get(0)).toDoubleArray(),
        new double[] {110, 0, 50, 50, 100, 5, 15, 25},
        "");
    Test.ensureEqual(
        ((PrimitiveArray) table2.get(1)).toDoubleArray(),
        new double[] {-5, 1, 3, 3, 3, 4, 14, 24},
        "");
    Test.ensureEqual(
        ((PrimitiveArray) table2.get(2)).toDoubleArray(),
        new double[] {0, 17, 3, 3, 1e300, 3, 13, 1},
        "");
    Test.ensureEqual(
        ((PrimitiveArray) table2.get(3)).toStringArray(),
        new String[] {"ABE", "a", "A", "A", "abe", "b", "aa", "c"},
        "");

    PrimitiveArray.merge(table2, table, new int[] {1, 0}, new boolean[] {true, false}, true);
    Test.ensureEqual(
        ((PrimitiveArray) table2.get(0)).toDoubleArray(),
        new double[] {110, 0, 100, 50, 5, 15, 25},
        "");
    Test.ensureEqual(
        ((PrimitiveArray) table2.get(1)).toDoubleArray(),
        new double[] {-5, 1, 3, 3, 4, 14, 24},
        "");
    Test.ensureEqual(
        ((PrimitiveArray) table2.get(2)).toDoubleArray(),
        new double[] {0, 17, 1e300, 3, 3, 13, 1},
        "");
    Test.ensureEqual(
        ((PrimitiveArray) table2.get(3)).toStringArray(),
        new String[] {"ABE", "a", "abe", "A", "b", "aa", "c"},
        "");

    // ** test speed
    int n = 10000000;
    long time1 = System.currentTimeMillis();
    int iar[] = new int[] {13, 24, 56};
    int sum = 0;
    for (int i = 0; i < n; i++) sum += iar[2];
    time1 = System.currentTimeMillis() - time1;

    long time2 = System.currentTimeMillis();
    IntArray ia = new IntArray(new int[] {13, 24, 56});
    sum = 0;
    for (int i = 0; i < n; i++) sum += ia.get(2);
    time2 = System.currentTimeMillis() - time2;

    String2.log("[] time=" + time1 + " IntArray time=" + time2 + "ms");

    // ** raf tests
    ByteArray bar = new ByteArray(new byte[] {2, 4, 6, 6, 6, 8});
    CharArray car =
        new CharArray(new char[] {'\u0002', '\u0004', '\u0006', '\u0006', '\u0006', '\u0008'});
    DoubleArray dar = new DoubleArray(new double[] {2, 4, 6, 6, 6, 8});
    FloatArray far = new FloatArray(new float[] {2, 4, 6, 6, 6, 8});
    IntArray Iar = new IntArray(new int[] {2, 4, 6, 6, 6, 8});
    LongArray lar = new LongArray(new long[] {2, 4, 6, 6, 6, 8});
    ShortArray sar = new ShortArray(new short[] {2, 4, 6, 6, 6, 8});
    StringArray Sar =
        new StringArray(new String[] {"22", "4444", "666666", "666666", "666666", "88888888"});

    Test.ensureEqual(bar.indexOf("6"), 2, "");
    Test.ensureEqual(car.indexOf("\u0006"), 2, "");
    Test.ensureEqual(dar.indexOf("6"), 2, "");
    Test.ensureEqual(far.indexOf("6"), 2, "");
    Test.ensureEqual(Iar.indexOf("6"), 2, "");
    Test.ensureEqual(lar.indexOf("6"), 2, "");
    Test.ensureEqual(sar.indexOf("6"), 2, "");
    Test.ensureEqual(Sar.indexOf("666666"), 2, "");

    Test.ensureEqual(bar.indexOf("a"), -1, "");
    Test.ensureEqual(car.indexOf("a"), -1, "");
    Test.ensureEqual(dar.indexOf("a"), -1, "");
    Test.ensureEqual(far.indexOf("a"), -1, "");
    Test.ensureEqual(Iar.indexOf("a"), -1, "");
    Test.ensureEqual(lar.indexOf("a"), -1, "");
    Test.ensureEqual(sar.indexOf("a"), -1, "");
    Test.ensureEqual(Sar.indexOf("a"), -1, "");

    Test.ensureEqual(bar.indexOf("6", 3), 3, "");
    Test.ensureEqual(car.indexOf("\u0006", 3), 3, "");
    Test.ensureEqual(dar.indexOf("6", 3), 3, "");
    Test.ensureEqual(far.indexOf("6", 3), 3, "");
    Test.ensureEqual(Iar.indexOf("6", 3), 3, "");
    Test.ensureEqual(lar.indexOf("6", 3), 3, "");
    Test.ensureEqual(sar.indexOf("6", 3), 3, "");
    Test.ensureEqual(Sar.indexOf("666666", 3), 3, "");

    Test.ensureEqual(bar.lastIndexOf("6"), 4, "");
    Test.ensureEqual(car.lastIndexOf("\u0006"), 4, "");
    Test.ensureEqual(dar.lastIndexOf("6"), 4, "");
    Test.ensureEqual(far.lastIndexOf("6"), 4, "");
    Test.ensureEqual(Iar.lastIndexOf("6"), 4, "");
    Test.ensureEqual(lar.lastIndexOf("6"), 4, "");
    Test.ensureEqual(sar.lastIndexOf("6"), 4, "");
    Test.ensureEqual(Sar.lastIndexOf("666666"), 4, "");

    Test.ensureEqual(bar.lastIndexOf("a"), -1, "");
    Test.ensureEqual(car.lastIndexOf("a"), -1, "");
    Test.ensureEqual(dar.lastIndexOf("a"), -1, "");
    Test.ensureEqual(far.lastIndexOf("a"), -1, "");
    Test.ensureEqual(Iar.lastIndexOf("a"), -1, "");
    Test.ensureEqual(lar.lastIndexOf("a"), -1, "");
    Test.ensureEqual(sar.lastIndexOf("a"), -1, "");
    Test.ensureEqual(Sar.lastIndexOf("a"), -1, "");

    Test.ensureEqual(bar.lastIndexOf("6", 3), 3, "");
    Test.ensureEqual(car.lastIndexOf("\u0006", 3), 3, "");
    Test.ensureEqual(dar.lastIndexOf("6", 3), 3, "");
    Test.ensureEqual(far.lastIndexOf("6", 3), 3, "");
    Test.ensureEqual(Iar.lastIndexOf("6", 3), 3, "");
    Test.ensureEqual(lar.lastIndexOf("6", 3), 3, "");
    Test.ensureEqual(sar.lastIndexOf("6", 3), 3, "");
    Test.ensureEqual(Sar.lastIndexOf("666666", 3), 3, "");

    // raf test2
    String raf2Name = File2.getSystemTempDirectory() + "PrimitiveArrayRaf2Test.bin";
    String2.log("raf2Name=" + raf2Name);
    File2.delete(raf2Name);
    Test.ensureEqual(File2.isFile(raf2Name), false, "");

    /*
     * RandomAccessFile raf2 = new RandomAccessFile(raf2Name, "rw");
     * long bStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.BYTE, 1.0);
     * rafWriteDouble(raf2, PAType.BYTE, Double.NaN);
     * long cStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.CHAR, 2.0);
     * rafWriteDouble(raf2, PAType.CHAR, Double.NaN);
     * long dStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.DOUBLE, 3.0);
     * rafWriteDouble(raf2, PAType.DOUBLE, Double.NaN);
     * long fStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.FLOAT, 4.0);
     * rafWriteDouble(raf2, PAType.FLOAT, Double.NaN);
     * long iStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.INT, 5.0);
     * rafWriteDouble(raf2, PAType.INT, Double.NaN);
     * long lStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.LONG, 6.0);
     * rafWriteDouble(raf2, PAType.LONG, Double.NaN);
     * long sStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.SHORT, 7.0);
     * rafWriteDouble(raf2, PAType.SHORT, Double.NaN);
     *
     * long ubStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.UBYTE, 1.0);
     * rafWriteDouble(raf2, PAType.UBYTE, Double.NaN);
     * long uiStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.UINT, 5.0);
     * rafWriteDouble(raf2, PAType.UINT, Double.NaN);
     * long ulStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.ULONG, 6.0);
     * rafWriteDouble(raf2, PAType.ULONG, Double.NaN);
     * long usStart = raf2.getFilePointer();
     * rafWriteDouble(raf2, PAType.USHORT, 7.0);
     * rafWriteDouble(raf2, PAType.USHORT, Double.NaN);
     *
     * //read in reverse order
     * Test.ensureEqual(rafReadDouble(raf2, PAType.USHORT, usStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.USHORT, usStart, 0), 7.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.ULONG, ulStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.ULONG, ulStart, 0), 6.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.UINT, uiStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.UINT, uiStart, 0), 5.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.UBYTE, ubStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.UBYTE, ubStart, 0), 1.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.SHORT, sStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.SHORT, sStart, 0), 7.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.LONG, lStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.LONG, lStart, 0), 6.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.INT, iStart, 1), Double.NaN, "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.INT, iStart, 0), 5.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.FLOAT, fStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.FLOAT, fStart, 0), 4.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.DOUBLE, dStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.DOUBLE, dStart, 0), 3.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.CHAR, cStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.CHAR, cStart, 0), 2.0, "");
     *
     * Test.ensureEqual(rafReadDouble(raf2, PAType.BYTE, bStart, 1), Double.NaN,
     * "");
     * Test.ensureEqual(rafReadDouble(raf2, PAType.BYTE, bStart, 0), 1.0, "");
     *
     * raf2.close();
     */

    // raf test
    String rafName = File2.getSystemTempDirectory() + "PrimitiveArrayRafTest.bin";
    String2.log("rafName=" + rafName);
    File2.delete(rafName);
    Test.ensureEqual(File2.isFile(rafName), false, "");

    DataOutputStream dos =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rafName)));
    long barStart = 0;
    long carStart = barStart + 6 * bar.writeDos(dos);
    long darStart = carStart + 6 * car.writeDos(dos);
    long farStart = darStart + 6 * dar.writeDos(dos);
    long IarStart = farStart + 6 * far.writeDos(dos);
    long larStart = IarStart + 6 * Iar.writeDos(dos);
    long sarStart = larStart + 6 * lar.writeDos(dos);
    long SarStart = sarStart + 6 * sar.writeDos(dos);
    Test.ensureEqual(Sar.writeDos(dos), 10, "");
    int nBytesPerS = 9;
    // String2.log(File2.hexDump(dosName, 500));

    dos.close();

    // test rafReadDouble
    RandomAccessFile raf = new RandomAccessFile(rafName, "rw");
    /*
     * Test.ensureEqual(rafReadDouble(raf, PAType.BYTE, barStart, 0), 2, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.BYTE, barStart, 5), 8, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.CHAR, carStart, 0), 2, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.CHAR, carStart, 5), 8, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.DOUBLE, darStart, 0), 2, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.DOUBLE, darStart, 5), 8, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.FLOAT, farStart, 0), 2, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.FLOAT, farStart, 5), 8, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.INT, IarStart, 0), 2, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.INT, IarStart, 5), 8, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.LONG, larStart, 0), 2, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.LONG, larStart, 5), 8, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.SHORT, sarStart, 0), 2, "");
     * Test.ensureEqual(rafReadDouble(raf, PAType.SHORT, sarStart, 5), 8, "");
     * //Test.ensureEqual(StringArray.rafReadString(raf, SarStart, 0, nBytesPerS),
     * "22", "");
     * //Test.ensureEqual(StringArray.rafReadString(raf, SarStart, 5, nBytesPerS),
     * "88888888", "");
     */

    // test rafBinarySearch
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2)),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(4)),
        1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(6)),
        2,
        ""); // 2,3,4
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(8)),
        5,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(1)),
        -1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(3)),
        -2,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(5)),
        -3,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(7)),
        -6,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(9)),
        -7,
        "");

    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 4, PAOne.fromFloat(2)),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 4, PAOne.fromFloat(4)),
        1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 4, PAOne.fromFloat(6)),
        4,
        ""); // any
    // of
    // 2,3,4
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 4, PAOne.fromFloat(1)),
        -1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 4, PAOne.fromFloat(3)),
        -2,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 4, PAOne.fromFloat(5)),
        -3,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafBinarySearch(raf, PAType.FLOAT, farStart, 0, 4, PAOne.fromFloat(7)),
        -6,
        "");

    // test rafFirstGE
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2)), 0, "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(4)), 1, "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(6)),
        2,
        ""); // first
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(8)), 5, "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(1)), 0, "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(3)), 1, "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(5)), 2, "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(7)), 5, "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(9)), 6, "");

    // test rafFirstGAE lastParam: precision = 5
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2), 5),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2.0000001f), 5),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(1.9999999f), 5),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(4), 5),
        1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(6), 5),
        2,
        ""); // first
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(6.0000001f), 5),
        2,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(5.9999999f), 5),
        2,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(8), 5),
        5,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(1), 5),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(3), 5),
        1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(3.0000001f), 5),
        1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2.9999999f), 5),
        1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(5), 5),
        2,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(7), 5),
        5,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafFirstGAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(9), 5),
        6,
        "");

    // test rafLastLE
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2)), 0, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(4)), 1, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(6)),
        4,
        ""); // last
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(8)), 5, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(1)), -1, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(3)), 0, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(5)), 1, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(7)), 4, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(9)), 5, "");

    // test rafLastLAE lastParam: precision = 5
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2), 5), 0, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2.0000001f), 5),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(1.9999999f), 5),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(4), 5), 1, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(6), 5),
        4,
        ""); // last
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(6.0000001f), 5),
        4,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(5.9999999f), 5),
        4,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(8), 5), 5, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(1), 5),
        -1,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(3), 5), 0, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(3.0000001f), 5),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(
            raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(2.9999999f), 5),
        0,
        "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(5), 5), 1, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(7), 5), 4, "");
    Test.ensureEqual(
        PrimitiveArray.rafLastLAE(raf, PAType.FLOAT, farStart, 0, 5, PAOne.fromFloat(9), 5), 5, "");
    raf.close();

    // test binarySearch
    // FloatArray far = new FloatArray(new float[]{2,4,6,6,6,8});
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(2)), 0, "");
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(4)), 1, "");
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(6)), 2, ""); // 2,3,4
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(8)), 5, "");
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(1)), -1, "");
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(3)), -2, "");
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(5)), -3, "");
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(7)), -6, "");
    Test.ensureEqual(far.binarySearch(0, 5, PAOne.fromFloat(9)), -7, "");

    Test.ensureEqual(far.binarySearch(0, 4, PAOne.fromFloat(2)), 0, "");
    Test.ensureEqual(far.binarySearch(0, 4, PAOne.fromFloat(4)), 1, "");
    Test.ensureEqual(far.binarySearch(0, 4, PAOne.fromFloat(6)), 4, ""); // any of 2,3,4
    Test.ensureEqual(far.binarySearch(0, 4, PAOne.fromFloat(1)), -1, "");
    Test.ensureEqual(far.binarySearch(0, 4, PAOne.fromFloat(3)), -2, "");
    Test.ensureEqual(far.binarySearch(0, 4, PAOne.fromFloat(5)), -3, "");
    Test.ensureEqual(far.binarySearch(0, 4, PAOne.fromFloat(7)), -6, "");

    // test binaryFindFirstGE
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(2)), 0, "");
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(4)), 1, "");
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(6)), 2, ""); // first
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(8)), 5, "");
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(1)), 0, "");
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(3)), 1, "");
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(5)), 2, "");
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(7)), 5, "");
    Test.ensureEqual(far.binaryFindFirstGE(0, 5, PAOne.fromFloat(9)), 6, "");

    // test binaryFindFirstGAE last param: precision=5
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(2), 5), 0, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(2.0000001f), 5), 0, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(1.9999999f), 5), 0, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(4), 5), 1, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(6), 5), 2, ""); // first
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(6.0000001f), 5), 2, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(5.9999999f), 5), 2, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(8), 5), 5, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(1), 5), 0, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(3), 5), 1, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(3.0000001f), 5), 1, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(2.9999999f), 5), 1, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(5), 5), 2, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(7), 5), 5, "");
    Test.ensureEqual(far.binaryFindFirstGAE(0, 5, PAOne.fromFloat(9), 5), 6, "");

    // test binaryFindLastLE
    // FloatArray far = new FloatArray(new float[]{2,4,6,6,6,8});
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(2)), 0, "");
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(4)), 1, "");
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(6)), 4, ""); // last
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(8)), 5, "");
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(1)), -1, "");
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(3)), 0, "");
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(5)), 1, "");
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(7)), 4, "");
    Test.ensureEqual(far.binaryFindLastLE(0, 5, PAOne.fromFloat(9)), 5, "");

    // test binaryFindLastLAE5 lastParam: precision = 5
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(2), 5), 0, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(2.0000001f), 5), 0, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(1.9999999f), 5), 0, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(4), 5), 1, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(6), 5), 4, ""); // last
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(6.0000001f), 5), 4, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(5.9999999f), 5), 4, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(8), 5), 5, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(1), 5), -1, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(3), 5), 0, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(3.0000001f), 5), 0, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(2.9999999f), 5), 0, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(5), 5), 1, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(7), 5), 4, "");
    Test.ensureEqual(far.binaryFindLastLAE(0, 5, PAOne.fromFloat(9), 5), 5, "");

    // test binaryFindClosest
    // FloatArray far = new FloatArray(new float[]{2,4,6,6,6,8});
    Test.ensureEqual(far.binaryFindClosest(2), 0, "");
    Test.ensureEqual(far.binaryFindClosest(2.1f), 0, "");
    Test.ensureEqual(far.binaryFindClosest(1.9f), 0, "");
    Test.ensureEqual(far.binaryFindClosest(4), 1, "");
    Test.ensureEqual(far.binaryFindClosest(6), 2, ""); // by chance
    Test.ensureEqual(far.binaryFindClosest(5.9f), 2, "");
    Test.ensureEqual(far.binaryFindClosest(6.1f), 4, ""); // since between 6 and 8
    Test.ensureEqual(far.binaryFindClosest(8), 5, "");
    Test.ensureEqual(far.binaryFindClosest(1), 0, "");
    Test.ensureEqual(far.binaryFindClosest(2.9f), 0, "");
    Test.ensureEqual(far.binaryFindClosest(3.1f), 1, "");
    Test.ensureEqual(far.binaryFindClosest(5.1f), 2, "");
    Test.ensureEqual(far.binaryFindClosest(7.1f), 5, "");
    Test.ensureEqual(far.binaryFindClosest(9), 5, "");

    // test linearFindClosest
    // FloatArray far = new FloatArray(new float[]{2,4,6,6,6,8});
    Test.ensureEqual(far.linearFindClosest(2), 0, "");
    Test.ensureEqual(far.linearFindClosest(2.1), 0, "");
    Test.ensureEqual(far.linearFindClosest(1.9), 0, "");
    Test.ensureEqual(far.linearFindClosest(4), 1, "");
    Test.ensureEqual(far.linearFindClosest(6), 2, ""); // unspecified
    Test.ensureEqual(far.linearFindClosest(5.9), 2, ""); // unspecified
    Test.ensureEqual(far.linearFindClosest(6.1), 2, ""); // unspecified
    Test.ensureEqual(far.linearFindClosest(8), 5, "");
    Test.ensureEqual(far.linearFindClosest(1), 0, "");
    Test.ensureEqual(far.linearFindClosest(2.9), 0, "");
    Test.ensureEqual(far.linearFindClosest(3.1), 1, "");
    Test.ensureEqual(far.linearFindClosest(5.1), 2, ""); // unspecified
    Test.ensureEqual(far.linearFindClosest(7.1), 5, "");
    Test.ensureEqual(far.linearFindClosest(9), 5, "");

    // strideWillFind
    Test.ensureEqual(PrimitiveArray.strideWillFind(5, 1), 5, "");
    Test.ensureEqual(PrimitiveArray.strideWillFind(5, 2), 3, "");
    Test.ensureEqual(PrimitiveArray.strideWillFind(5, 3), 2, "");
    Test.ensureEqual(PrimitiveArray.strideWillFind(5, 4), 2, "");
    Test.ensureEqual(PrimitiveArray.strideWillFind(5, 5), 1, "");

    // scaleAddOffset
    ia = new IntArray(new int[] {0, 1, 2, 3, Integer.MAX_VALUE});
    ia.scaleAddOffset(1.5, 10);
    Test.ensureEqual(ia.toString(), "10, 12, 13, 15, 2147483647", "");

    // addFromPA(
    DoubleArray other = (DoubleArray) PrimitiveArray.csvFactory(PAType.DOUBLE, "11.1, 22.2, 33.3");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.BYTE, "1.1, 2.2").addFromPA(other, 1, 2).toString(),
        "1, 2, 22, 33",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.CHAR, "1.1, 2.2").addFromPA(other, 1, 2).toString(),
        "1, 2, \\u0016, !",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.DOUBLE, "1.1, 2.2").addFromPA(other, 1, 2).toString(),
        "1.1, 2.2, 22.2, 33.3",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.FLOAT, "1.1, 2.2").addFromPA(other, 1, 2).toString(),
        "1.1, 2.2, 22.2, 33.3",
        "");
    Test.ensureEqual(PrimitiveArray.csvFactory(PAType.INT, "1.1, 2.2").toString(), "1, 2", "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.INT, "1.1, 2.2").addFromPA(other, 1, 2).toString(),
        "1, 2, 22, 33",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.LONG, "1, 2").addFromPA(other, 1, 2).toString(),
        "1, 2, 22, 33",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.SHORT, "1.1, 2.2").addFromPA(other, 1, 2).toString(),
        "1, 2, 22, 33",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.STRING, "1.1, 2.2").addFromPA(other, 1, 2).toString(),
        "1.1, 2.2, 22.2, 33.3",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.STRING, "1.1, 2.2").addFromPA(other, 1, 2).toString(),
        "1.1, 2.2, 22.2, 33.3",
        "");
    Test.ensureEqual(ia.addFromPA(other, 2).toString(), "10, 12, 13, 15, 2147483647, 33", "");

    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.BYTE, "1.1, 2.2")
            .addFromPA(PrimitiveArray.csvFactory(PAType.BYTE, "11.1, 22.2, 33.3"), 1, 2)
            .toString(),
        "1, 2, 22, 33",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.CHAR, "1.1, 2.2")
            .addFromPA(PrimitiveArray.csvFactory(PAType.CHAR, "11.1, 22.2, 33.3"), 1, 2)
            .toString(),
        "1, 2, 2, 3",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.DOUBLE, "1.1, 2.2")
            .addFromPA(PrimitiveArray.csvFactory(PAType.DOUBLE, "11.1, 22.2, 33.3"), 1, 2)
            .toString(),
        "1.1, 2.2, 22.2, 33.3",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.FLOAT, "1.1, 2.2")
            .addFromPA(PrimitiveArray.csvFactory(PAType.FLOAT, "11.1, 22.2, 33.3"), 1, 2)
            .toString(),
        "1.1, 2.2, 22.2, 33.3",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.INT, "1.1, 2.2")
            .addFromPA(PrimitiveArray.csvFactory(PAType.INT, "11.1, 22.2, 33.3"), 1, 2)
            .toString(),
        "1, 2, 22, 33",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.LONG, "1, 2")
            .addFromPA(PrimitiveArray.csvFactory(PAType.LONG, "11, 22, 33"), 1, 2)
            .toString(),
        "1, 2, 22, 33",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.SHORT, "1.1, 2.2")
            .addFromPA(PrimitiveArray.csvFactory(PAType.SHORT, "11.1, 22.2, 33.3"), 1, 2)
            .toString(),
        "1, 2, 22, 33",
        "");
    Test.ensureEqual(
        PrimitiveArray.csvFactory(PAType.STRING, "1.1, 2.2")
            .addFromPA(PrimitiveArray.csvFactory(PAType.STRING, "11.1, 22.2, 33.3"), 1, 2)
            .toString(),
        "1.1, 2.2, 22.2, 33.3",
        "");

    String2.log("PrimitiveArray.basicTest finished successfully.");
  }

  /**
   * @throws RuntimeException if trouble
   */
  @org.junit.jupiter.api.Test
  void testTestValueOpValue() {
    String2.log("\n*** PrimitiveArray.testTestValueOpValue()");

    // numeric Table.testValueOpValue
    // "!=", PrimitiveArray.REGEX_OP, "<=", ">=", "=", "<", ">"};
    long lnan = Long.MAX_VALUE;
    float fnan = Float.NaN;
    double dnan = Double.NaN;
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "=", 1), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "=", 2), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "=", lnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, "=", 1), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, "=", lnan), true, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "=", 1f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "=", 2f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "=", fnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, "=", 1f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, "=", fnan), true, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "=", 1d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "=", 2d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "=", dnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, "=", 1d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, "=", dnan), true, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "!=", 1), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "!=", 2), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "!=", lnan), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, "!=", 1), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, "!=", lnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "!=", 1f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "!=", 2f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "!=", fnan), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, "!=", 1f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, "!=", fnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "!=", 1d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "!=", 2d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "!=", dnan), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, "!=", 1d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, "!=", dnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<=", 1), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<=", 2), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(2, "<=", 1), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<=", lnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, "<=", 1), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, "<=", lnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "<=", 1f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "<=", 2f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(2f, "<=", 1f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "<=", fnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, "<=", 1f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, "<=", fnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "<=", 1d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "<=", 2d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(2d, "<=", 1d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "<=", dnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, "<=", 1d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, "<=", dnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<", 1), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<", 2), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<", lnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, "<", 1), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, "<", lnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "<", 1f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "<", 2f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, "<", fnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, "<", 1f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, "<", fnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "<", 1d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "<", 2d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, "<", dnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, "<", 1d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, "<", dnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">=", 1), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">=", 2), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(2, ">=", 1), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">=", lnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, ">=", 1), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, ">=", lnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, ">=", 1f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, ">=", 2f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(2f, ">=", 1f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, ">=", fnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, ">=", 1f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, ">=", fnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, ">=", 1d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, ">=", 2d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(2d, ">=", 1d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, ">=", dnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, ">=", 1d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, ">=", dnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(2, ">", 1), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">", 2), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">", lnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, ">", 1), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(lnan, ">", lnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(2f, ">", 1f), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, ">", 2f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1f, ">", fnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, ">", 1f), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(fnan, ">", fnan), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue(2d, ">", 1d), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, ">", 2d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(1d, ">", dnan), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, ">", 1d), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(dnan, ">", dnan), false, "");

    // regex tests always via testValueOpValue(string)

    // string testValueOpValue
    // "!=", PrimitiveArray.REGEX_OP, "<=", ">=", "=", "<", ">"};
    String s = "";
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "=", "a"), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "=", "B"), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "=", s), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(s, "=", "a"), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(s, "=", s), true, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "!=", "a"), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "!=", "B"), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "!=", s), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(s, "!=", "a"), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(s, "!=", s), false, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "<=", "a"), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "<=", "B"), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("B", "<=", "a"), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "<=", s), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(s, "<=", "a"), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue(s, "<=", s), true, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "<", "a"), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", "<", "B"), true, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", ">=", "a"), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", ">=", "B"), false, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("B", ">=", "a"), true, "");

    Test.ensureEqual(PrimitiveArray.testValueOpValue("B", ">", "a"), true, "");
    Test.ensureEqual(PrimitiveArray.testValueOpValue("a", ">", "B"), false, "");

    Test.ensureEqual(
        PrimitiveArray.testValueOpValue("12345", PrimitiveArray.REGEX_OP, "[0-9]+"), true, "");
    Test.ensureEqual(
        PrimitiveArray.testValueOpValue("12a45", PrimitiveArray.REGEX_OP, "[0-9]+"), false, "");

    // test speed
    long tTime = System.currentTimeMillis();
    int n = 2000000;
    for (int i = 0; i < n; i++) {
      Test.ensureEqual(
          PrimitiveArray.testValueOpValue("abcdefghijk", "=", "abcdefghijk"), true, "");
      Test.ensureEqual(
          PrimitiveArray.testValueOpValue("abcdefghijk", "!=", "abcdefghijk"), false, "");
      Test.ensureEqual(
          PrimitiveArray.testValueOpValue("abcdefghijk", "<=", "abcdefghijk"), true, "");
      Test.ensureEqual(
          PrimitiveArray.testValueOpValue("abcdefghijk", "<", "abcdefghijk"), false, "");
      Test.ensureEqual(
          PrimitiveArray.testValueOpValue("abcdefghijk", ">=", "abcdefghijk"), true, "");
      Test.ensureEqual(
          PrimitiveArray.testValueOpValue("abcdefghijk", ">", "abcdefghijk"), false, "");
    }
    String2.log(
        "time for "
            + (6 * n)
            + " testValueOpValue(string): "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 456ms, 1.7M4700 624ms, 2012-06-29: 3718 ms)");

    // regex simple
    for (int i = 0; i < n; i++) {
      Test.ensureEqual(
          PrimitiveArray.testValueOpValue("12345", PrimitiveArray.REGEX_OP, "[0-9]+"), true, "");
    }
    String2.log(
        "time for "
            + n
            + " regex testValueOpValue(string, regex): "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 1227ms, 1.7M4700 1436ms, 2012-06-29: 8906 ms)");

    // int
    tTime = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "=", 1), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "!=", 1), false, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<=", 1), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<", 1), false, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">=", 1), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(2, ">", 1), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">", 2), false, "");
      // regex tests always via testValueOpValue(string)
    }
    String2.log(
        "time for "
            + (7 * n)
            + " testValueOpValue(int): "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 47ms, 1.7M4700 156ms, 2012-06-29: 656 ms)");

    // long
    tTime = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      Test.ensureEqual(PrimitiveArray.testValueOpValue(10000000000L, "=", 10000000000L), true, "");
      Test.ensureEqual(
          PrimitiveArray.testValueOpValue(10000000000L, "!=", 10000000000L), false, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(10000000000L, "<=", 10000000000L), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(10000000000L, "<", 10000000000L), false, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(10000000000L, ">=", 10000000000L), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(20000000000L, ">", 10000000000L), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(10000000000L, ">", 20000000000L), false, "");
      // regex tests always via testValueOpValue(string)
    }
    String2.log(
        "time for "
            + (7 * n)
            + " testValueOpValue(long): "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 47ms, 1.7M4700 156ms, 2012-06-29: 656 ms)");

    // float
    tTime = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "=", 1f), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "!=", 1f), false, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<=", 1f), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<", 1f), false, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">=", 1f), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(2, ">", 1f), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">", 2f), false, "");
      // regex tests always via testValueOpValue(string)
    }
    String2.log(
        "time for "
            + (7 * n)
            + " testValueOpValue(float): "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 147ms, 1.7M4700 218ms, 2012-06-29: 656 ms)");

    // double
    tTime = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "=", 1d), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "!=", 1d), false, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<=", 1d), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<", 1d), false, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">=", 1d), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(2, ">", 1d), true, "");
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, ">", 2d), false, "");
      // regex tests always via testValueOpValue(string)
    }
    String2.log(
        "time for "
            + (7 * n)
            + " testValueOpValue(double): "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 145ms, 1.7M4700 156ms, 2012-06-29: 658 ms)");

    tTime = System.currentTimeMillis();
    for (int i = 0; i < 7 * n; i++) {
      Test.ensureEqual(PrimitiveArray.testValueOpValue(1, "<=", 1), true, "");
    }
    String2.log(
        "time for "
            + (7 * n)
            + " testValueOpValue(double <=): "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 39ms, 1.7M4700 124ms, 2012-06-29: 468 ms)");

    // ********** test applyConstraint
    PrimitiveArray pa;
    BitSet keep;

    // regex
    pa = PrimitiveArray.factory(PAType.INT, n, "5");
    pa.addInt(10);
    pa.addString("");
    keep = new BitSet();
    keep.set(0, pa.size());
    tTime = System.currentTimeMillis();
    pa.applyConstraint(false, keep, "=~", "(10|zztop)");
    pa.justKeep(keep);
    Test.ensureEqual(pa.size(), 1, "");
    Test.ensureEqual(pa.getDouble(0), 10, "");
    String2.log(
        "time for applyConstraint(regex) n="
            + n
            + ": "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 188ms, 1.7M4700 278ms, 2012-06-29: 1000 ms)");

    // string
    pa = PrimitiveArray.factory(PAType.STRING, n, "Apple");
    pa.addString("Nate");
    pa.addString("");
    keep = new BitSet();
    keep.set(0, pa.size());
    tTime = System.currentTimeMillis();
    pa.applyConstraint(false, keep, ">=", "hubert"); // >= uses case insensitive test
    pa.justKeep(keep);
    Test.ensureEqual(pa.size(), 1, "");
    Test.ensureEqual(pa.getString(0), "Nate", "");
    String2.log(
        "time for applyConstraint(String) n="
            + n
            + ": "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 176ms, 1.7M4700 186ms, 2012-06-29: 812 ms)");

    // float
    pa = PrimitiveArray.factory(PAType.FLOAT, n, "5");
    pa.addInt(10);
    pa.addString("");
    keep = new BitSet();
    keep.set(0, pa.size());
    tTime = System.currentTimeMillis();
    pa.applyConstraint(false, keep, ">=", "9");
    pa.justKeep(keep);
    Test.ensureEqual(pa.size(), 1, "");
    Test.ensureEqual(pa.getDouble(0), 10, "");
    String2.log(
        "time for applyConstraint(float) n="
            + n
            + ": "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 47ms, 1.7M4700 186ms, 2012-06-29: 280 ms)");

    // double
    pa = PrimitiveArray.factory(PAType.DOUBLE, n, "5");
    pa.addInt(10);
    pa.addString("");
    keep = new BitSet();
    keep.set(0, pa.size());
    tTime = System.currentTimeMillis();
    pa.applyConstraint(false, keep, ">=", "9");
    pa.justKeep(keep);
    Test.ensureEqual(pa.size(), 1, "");
    Test.ensureEqual(pa.getDouble(0), 10, "");
    String2.log(
        "time for applyConstraint(double) n="
            + n
            + ": "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 47ms, 1.7M4700 62ms, 2012-06-29: 250 ms)");

    // long
    pa = PrimitiveArray.factory(PAType.LONG, n, "5");
    pa.addInt(10);
    pa.addString("");
    keep = new BitSet();
    keep.set(0, pa.size());
    tTime = System.currentTimeMillis();
    pa.applyConstraint(false, keep, ">=", "9");
    pa.justKeep(keep);
    Test.ensureEqual(pa.size(), 1, "");
    Test.ensureEqual(pa.getDouble(0), 10, "");
    String2.log(
        "time for applyConstraint(long) n="
            + n
            + ": "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 46ms)");

    // int
    pa = PrimitiveArray.factory(PAType.INT, n, "5");
    pa.addInt(10);
    pa.addString("");
    keep = new BitSet();
    keep.set(0, pa.size());
    tTime = System.currentTimeMillis();
    pa.applyConstraint(false, keep, ">=", "9");
    pa.justKeep(keep);
    Test.ensureEqual(pa.size(), 1, "");
    Test.ensureEqual(pa.getDouble(0), 10, "");
    String2.log(
        "time for applyConstraint(int) n="
            + n
            + ": "
            + (System.currentTimeMillis() - tTime)
            + " (Java 1.8 31ms, 1.7M4700 32ms, 2012-06-29: 282 ms)");
  }
}
