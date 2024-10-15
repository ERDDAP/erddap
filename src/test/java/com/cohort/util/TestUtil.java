/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import com.cohort.array.StringArray;
import java.io.File;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import tags.TagFlaky;
import tags.TagIncompleteTest;
import tags.TagSlowTests;

/** This is a Java program to test all of the methods in com.cohort.util. */
@Isolated
class TestUtil {

  @TempDir private static Path TEMP_DIR;

  /** Test the methods in Test. */
  @org.junit.jupiter.api.Test
  void testTest() {
    String2.log("\n*** TestUtil.testTest");
    Test.ensureTrue(true, "a");
    Test.ensureEqual(true, true, "a");
    Test.ensureEqual(false, false, "b");
    Test.ensureEqual(345, 345, "c");
    Test.ensureNotEqual(123, -123, "d");
    Test.ensureEqual(34.545f, 34.545f, "e");
    Test.ensureEqual(34.512345, 34.512345, "f");
    Test.ensureNotEqual(12.3, -12.3, "g");
    Test.ensureEqual("abc", "ab" + "c", "h");
    float far1[] = {1.1f, 2.200001f};
    float far2[] = {1.1f, 2.2f};
    Test.ensureEqual(far1, far2, "iff");
    double dar1[] = {1.1, 2.2};
    double dar2[] = {1.1, 2.200000000001};
    Test.ensureEqual(dar1, dar2, "idd");

    String result;
    result = Test.testLinesMatch("ab\nbbbb\n\nccc", "ab\n.{4}\n\nc.c", "Shouldn't happen.");
    Test.ensureEqual(result, "", "\nresult=\n" + result);

    result = Test.testLinesMatch("ab\nbbbb\n\nccc", "ab\n.{3}\n\nc.c", "A specific message.");
    Test.ensureEqual(
        result,
        "\n"
            + "ERROR in Test.ensureLinesMatch():\n"
            + "A specific message.\n"
            + "The first line that differs is:\n"
            + "  text [1]=bbbb[end]\n"
            + "  regex[1]=.{3}[end]\n",
        "\nresult=\n" + result);

    Double Dar1[] = {Double.valueOf(1.1), Double.valueOf(2.2)};
    Double Dar2[] = {Double.valueOf(1.1), Double.valueOf(2.2)};
    Test.ensureEqual(Dar1, Dar2, "j");

    /*
     * //test: if exception in catch clause, is finally still done?
     * String s = null;
     * try {
     * System.out.println("Test.testTest in try block");
     * s = s.substring(0);
     * } catch (Exception e) {
     * System.out.println("Test.testTest in catch block");
     * s = s.substring(0); //2) but this exception then stops the program
     * } finally {
     * System.out.println("Test.testTest finally!"); //1) this is done
     * }
     * Math2.sleep(5000);
     */

  }

  /** Test the methods in Math2. */
  @org.junit.jupiter.api.Test
  void testMath2() {
    String2.log("\n*** TestUtil.testMath2");

    // can 215.1125 be exactly represented in a float?
    float f = 215.1125f;
    String2.log("test 215.1125 = " + f);
    String2.log("test log10 20215.1125 = " + f);
    Test.ensureEqual("215.1125", "" + f, "215.1125");

    double d0 = 5.0;
    double d1 = Double.POSITIVE_INFINITY;
    double d2 = Double.NEGATIVE_INFINITY;
    double d3 = Double.NaN;
    Test.ensureTrue(Math2.equalsIncludingNanOrInfinite(d0, d0), "");
    Test.ensureTrue(Math2.equalsIncludingNanOrInfinite(d1, d1), "");
    Test.ensureTrue(Math2.equalsIncludingNanOrInfinite(d2, d2), "");
    Test.ensureTrue(Math2.equalsIncludingNanOrInfinite(d3, d3), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d0, d1), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d0, d2), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d0, d3), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d1, d0), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d1, d2), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d1, d3), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d2, d0), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d2, d1), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d2, d3), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d3, d0), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d3, d1), "");
    Test.ensureTrue(!Math2.equalsIncludingNanOrInfinite(d3, d2), "");

    // getMemoryInUse
    StringBuilder sb = new StringBuilder();
    long time = System.currentTimeMillis();
    long sum = 0;
    for (int i = 0; i < 100000; i++) {
      sum += Math2.getMemoryInUse();
      sb.append("" + i);
      if (i % 100 == 0) sb.setLength(0);
    }
    String2.log(
        "getMemoryInUse is fast! It takes about "
            + (System.currentTimeMillis() - time) / 100000.0
            + "ms per call. "
            + sum
            + sb);

    // longToDoubleNaN
    String2.log("test longToDoubleNaN");
    Test.ensureEqual(
        Math.round(Math2.longToDoubleNaN(-9223372036854775808L)), -9223372036854775808L, "k-");
    Test.ensureEqual(
        String2.parseDouble("" + Math2.longToDoubleNaN(-9223372036854775808L)),
        -9.223372036854776E18,
        "k-2");
    Test.ensureEqual(
        Math.round(-9.223372036854776E18), -9223372036854775808L, "k-3"); // Long.MIN_VALUE
    Test.ensureEqual(
        Math2.roundToLong(-9.223372036854776E18), -9223372036854775808L, "k-3"); // Long.MIN_VALUE
    Test.ensureEqual(
        Math2.longToDoubleNaN(9223372036854775806L), // Long.MAX_VALUE - 1
        9223372036854774784.0,
        "k+"); // Not good, but best available
    Test.ensureEqual(Math.rint(9223372036854774784.0), 9223372036854774784L, "k+");

    Test.ensureEqual(Math2.longToDoubleNaN(9223372036854775807L), Double.NaN, "kMV");
    Test.ensureEqual(Math2.longToDoubleNaN(Long.MAX_VALUE), Double.NaN, "kMV2");

    // log10
    String2.log("test log10");
    Test.ensureEqual(Math.log10(100), 2, "a");
    Test.ensureEqual(Math.log10(0.01), -2, "b");
    Test.ensureEqual(Math.log10(0), Double.NEGATIVE_INFINITY, "c");
    Test.ensureEqual(Math.log10(-1), Double.NaN, "d");
    Test.ensureEqual(Math.log10(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY, "e");
    Test.ensureEqual(Math.log10(Double.NaN), Double.NaN, "f");

    // trunc
    String2.log("test trunc");
    Test.ensureEqual(Math2.trunc(0.9), 0, "a");
    Test.ensureEqual(Math2.trunc(-0.9), 0, "b");
    Test.ensureEqual(Math2.trunc(3.2), 3, "c");
    Test.ensureEqual(Math2.trunc(-3.2), -3, "d");
    Test.ensureEqual(Math2.trunc(3), 3, "e");
    Test.ensureEqual(Math2.trunc(-3), -3, "f");
    Test.ensureEqual(Math2.trunc(Double.NaN), Double.NaN, "g");

    // truncToInt
    String2.log("test truncToInt");
    Test.ensureEqual(Math2.truncToInt(0.9), 0, "a");
    Test.ensureEqual(Math2.truncToInt(-0.9), 0, "b");
    Test.ensureEqual(Math2.truncToInt(3.2), 3, "c");
    Test.ensureEqual(Math2.truncToInt(-3.2), -3, "d");
    Test.ensureEqual(Math2.truncToInt(3), 3, "e");
    Test.ensureEqual(Math2.truncToInt(-3), -3, "f");
    Test.ensureEqual(Math2.truncToInt(Double.NaN), Integer.MAX_VALUE, "g");
    Test.ensureEqual(Math2.truncToInt(Double.POSITIVE_INFINITY), Integer.MAX_VALUE, "h");
    Test.ensureEqual(Math2.truncToInt(-1e12), Integer.MAX_VALUE, "i");

    // frac
    String2.log("test frac");
    Test.ensureEqual(Math2.frac(0.9), 0.9, "a");
    Test.ensureEqual(Math2.frac(-0.9), -0.9, "b");
    Test.ensureEqual(Math2.frac(3.2), 0.2, "c");
    Test.ensureEqual(Math2.frac(-3.2), -0.2, "d");
    Test.ensureEqual(Math2.frac(3), 0, "e");
    Test.ensureEqual(Math2.frac(-3), 0, "f");
    Test.ensureEqual(Math2.frac(Double.NaN), Double.NaN, "g");
    Test.ensureEqual(Math2.frac(Double.POSITIVE_INFINITY), Double.NaN, "h");

    // sign1
    String2.log("test sign1");
    Test.ensureEqual(Math2.sign1(-1), -1, "a");
    Test.ensureEqual(Math2.sign1(0), 1, "b");
    Test.ensureEqual(Math2.sign1(1), 1, "c");
    Test.ensureEqual(Math2.sign1(3123), 1, "d");

    // intExponent
    String2.log("test intExponent");
    Test.ensureEqual(Math2.intExponent(0.02), -2, "a");
    Test.ensureEqual(Math2.intExponent(-0.02), -2, "b");
    Test.ensureEqual(Math2.intExponent(0.01), -2, "c");
    Test.ensureEqual(Math2.intExponent(-0.01), -2, "d");
    Test.ensureEqual(Math2.intExponent(312), 2, "e");
    Test.ensureEqual(Math2.intExponent(-312), 2, "f");
    Test.ensureEqual(Math2.intExponent(Double.NaN), Integer.MAX_VALUE, "g");
    Test.ensureEqual(Math2.intExponent(Double.POSITIVE_INFINITY), Integer.MAX_VALUE, "h");
    Test.ensureEqual(Math2.intExponent(0), 0, "i");

    // exponent
    String2.log("test exponent");
    Test.ensureEqual(Math2.exponent(0.02), 0.01, "a");
    Test.ensureEqual(Math2.exponent(-0.02), 0.01, "b");
    Test.ensureEqual(Math2.exponent(0.01), 0.01, "c");
    Test.ensureEqual(Math2.exponent(-0.01), 0.01, "d");
    Test.ensureEqual(Math2.exponent(312), 100, "e");
    Test.ensureEqual(Math2.exponent(-312), 100, "f");
    Test.ensureEqual(Math2.exponent(Double.NaN), Double.NaN, "g");
    Test.ensureEqual(Math2.exponent(Double.POSITIVE_INFINITY), Double.NaN, "h");
    Test.ensureEqual(Math2.exponent(0), 1, "i");

    // mantissa
    String2.log("test mantissa");
    Test.ensureEqual(Math2.mantissa(0.0234), 2.34, "a");
    Test.ensureEqual(Math2.mantissa(-0.0234), -2.34, "b");
    Test.ensureEqual(Math2.mantissa(0.0123), 1.23, "c");
    Test.ensureEqual(Math2.mantissa(-0.0123), -1.23, "d");
    Test.ensureEqual(Math2.mantissa(312.9), 3.129, "e");
    Test.ensureEqual(Math2.mantissa(-312.9), -3.129, "f");
    Test.ensureEqual(Math2.mantissa(Double.NaN), Double.NaN, "g");
    Test.ensureEqual(Math2.mantissa(0), 0, "h");

    // isFinite
    String2.log("test isFinite");
    Test.ensureEqual(Double.isFinite(0.0234), true, "a");
    Test.ensureEqual(Double.isFinite(-0.0234), true, "b");
    Test.ensureEqual(Double.isFinite(0), true, "c");
    Test.ensureEqual(Double.isFinite(1e301), true, "d");
    Test.ensureEqual(Double.isFinite(Double.NaN), false, "e");
    Test.ensureEqual(Double.isFinite(Double.POSITIVE_INFINITY), false, "f");
    Test.ensureEqual(Double.isFinite(Double.NEGATIVE_INFINITY), false, "g");

    // NaNCheck
    String2.log("test NaNCheck");
    Test.ensureEqual(Math2.NaNCheck(0.0234), 0.0234, "a");
    Test.ensureEqual(Math2.NaNCheck(-0.0234), -0.0234, "b");
    Test.ensureEqual(Math2.NaNCheck(0), 0, "c");
    Test.ensureEqual(Math2.NaNCheck(1e301), 1e301, "d");
    Test.ensureEqual(Math2.NaNCheck(Double.NaN), Double.NaN, "e");
    Test.ensureEqual(Math2.NaNCheck(Double.POSITIVE_INFINITY), Double.NaN, "f");
    Test.ensureEqual(Math2.NaNCheck(Double.NEGATIVE_INFINITY), Double.NaN, "g");

    // sleep
    String2.log("test sleep(3000)");
    Math2.sleep(3000);
    String2.log("test sleep(-5000)");
    Math2.sleep(-5000);

    // memory
    double da[] = new double[1000000]; // use some memory
    String2.log("getAllocatedMemory = " + Math2.getAllocatedMemory());
    String2.log("maxSafeMemory = " + Math2.maxSafeMemory);
    String2.log("getUsingMemory = " + Math2.getMemoryInUse());
    String2.log("memoryString = " + Math2.memoryString());

    // incgc
    String2.log("test incgc(3000)");
    da = null; // free the memory
    Math2.incgc("EDDTableFromNcFiles (between tests)", 3000);
    String2.log("after incgc: " + Math2.memoryString());

    // gc
    String2.log("test gcAndWait()");
    da = new double[1000000];
    String2.log("after allocate = " + Math2.memoryString());
    da = null;
    Math2.gcAndWait("TestUtil (between tests)"); // in test
    String2.log("after gc = " + Math2.memoryString());

    // odd
    String2.log("test odd");
    Test.ensureEqual(Math2.odd(-2), false, "a");
    Test.ensureEqual(Math2.odd(-1), true, "b");
    Test.ensureEqual(Math2.odd(0), false, "c");
    Test.ensureEqual(Math2.odd(1), true, "d");
    Test.ensureEqual(Math2.odd(2), false, "e");

    // ten
    String2.log("test ten");
    Test.ensureEqual(Math2.ten(-2), 0.01, "a");
    Test.ensureEqual(Math2.ten(0), 1, "b");
    Test.ensureEqual(Math2.ten(1), 10, "c");
    Test.ensureEqual(Math2.ten(2), 100, "d");
    Test.ensureEqual(Math2.ten(10), 1e10, "e");
    Test.ensureEqual(Math2.ten(20), 1e20, "f");

    // binaryExponent
    String2.log("test binaryExponent");
    Test.ensureEqual(Math2.binaryExponent(1), 0, "a");
    Test.ensureEqual(Math2.binaryExponent(-1), 0, "b");
    Test.ensureEqual(Math2.binaryExponent(0.5), -1, "c");
    Test.ensureEqual(Math2.binaryExponent(-0.5), -1, "d");
    Test.ensureEqual(Math2.binaryExponent(2), 1, "e");
    Test.ensureEqual(Math2.binaryExponent(-2), 1, "f");
    Test.ensureEqual(Math2.binaryExponent(3), 1, "g");
    Test.ensureEqual(Math2.binaryExponent(1024), 10, "h");
    Test.ensureEqual(Math2.binaryExponent(0), Math2.Binary0, "i");
    Test.ensureEqual(Math2.binaryExponent(Double.NaN), Math2.BinaryLimit, "j");

    // almost0
    String2.log("test almost0");
    Test.ensureEqual(Math2.almost0(0), true, "a");
    Test.ensureEqual(Math2.almost0(1e-14), true, "b");
    Test.ensureEqual(Math2.almost0(-1e-14), true, "c");
    Test.ensureEqual(Math2.almost0(1e-13), false, "d");
    Test.ensureEqual(Math2.almost0(-1e-13), false, "e");
    Test.ensureEqual(Math2.almost0(Double.NaN), false, "f");
    Test.ensureEqual(Math2.almost0(Double.POSITIVE_INFINITY), false, "g");

    // almostEqual5 digits
    String2.log("test almostEqual5");
    Test.ensureEqual(Math2.almostEqual(5, -0.00000000000000123, 0), true, "a"); // almost 0
    Test.ensureEqual(
        Math2.almostEqual(5, 0.00000000000000123, 0.00000000000000567), true, "b"); // almost 0
    Test.ensureEqual(
        Math2.almostEqual(5, 0.00000000000000123, 0.0000123), false, "c"); // not almost 0
    Test.ensureEqual(Math2.almostEqual(5, 0.0001234567, 0.0001234567), true, "d");
    Test.ensureEqual(Math2.almostEqual(5, 0.000123456, 0.000123449), false, "e");
    Test.ensureEqual(Math2.almostEqual(5, 1234567, 1234566), true, "f");
    Test.ensureEqual(Math2.almostEqual(5, 123456, 123449), false, "g");
    Test.ensureEqual(Math2.almostEqual(5, 1234567e20, 1234566e20), true, "h");
    Test.ensureEqual(Math2.almostEqual(5, 123456e20, 123449e20), false, "i");
    Test.ensureEqual(Math2.almostEqual(5, Double.NaN, Double.NaN), false, "j");
    Test.ensureEqual(Math2.almostEqual(5, Double.POSITIVE_INFINITY, 1e300), false, "k");
    Test.ensureEqual(Math2.almostEqual(5, Double.POSITIVE_INFINITY, Double.NaN), false, "l");

    // almostEqual9
    String2.log("test almostEqual9");
    Test.ensureEqual(Math2.almostEqual(9, -0.0000000000000123, 0), true, "a"); // almost 0
    Test.ensureEqual(
        Math2.almostEqual(9, 0.0000000000000123, 0.0000000000000567), true, "b"); // almost 0
    Test.ensureEqual(
        Math2.almostEqual(9, 0.0000000000000123, 0.000000123), false, "c"); // not almost 0
    Test.ensureEqual(Math2.almostEqual(9, 1, 1.0000000001), true, "d");
    Test.ensureEqual(Math2.almostEqual(9, 4.999999999, 5.000000001), true, "e");
    Test.ensureEqual(Math2.almostEqual(9, 0.0000000000000123, 0.00000123), false, "f"); // almost 0
    Test.ensureEqual(Math2.almostEqual(9, -4.999999999, -5.000000001), true, "g");
    Test.ensureEqual(Math2.almostEqual(9, -0.00000000000001, 0.00000000000001), true, "h");
    Test.ensureEqual(Math2.almostEqual(9, 4.53, 4.530000001), true, "i");
    Test.ensureEqual(Math2.almostEqual(9, 1, 1.000000001), false, "j");
    Test.ensureEqual(Math2.almostEqual(9, 4.99999999, 5.00000001), false, "k");
    Test.ensureEqual(Math2.almostEqual(9, -4.99999999, -5.00000001), false, "l");
    Test.ensureEqual(Math2.almostEqual(9, -0.00000001, 0.00000001), false, "m");
    Test.ensureEqual(Math2.almostEqual(9, 4.53, 4.53000001), false, "n");
    Test.ensureEqual(Math2.almostEqual(9, Double.NaN, Double.NaN), false, "o");
    Test.ensureEqual(Math2.almostEqual(9, Double.POSITIVE_INFINITY, 1e300), false, "p");
    Test.ensureEqual(Math2.almostEqual(9, Double.POSITIVE_INFINITY, Double.NaN), false, "q");

    // almostEqual14 14 digits
    String2.log("test almostEqual14");
    Test.ensureEqual(Math2.almostEqual(14, -0.00000000000000123, 0), true, "a"); // almost 0
    Test.ensureEqual(
        Math2.almostEqual(14, 0.00000000000000123, 0.00000000000000567), true, "b"); // almost 0
    Test.ensureEqual(
        Math2.almostEqual(14, 0.00000000000000123, 0.000000123), false, "c"); // not almost 0
    Test.ensureEqual(Math2.almostEqual(14, 0.12345678901234567, 0.12345678901234566), true, "d");
    Test.ensureEqual(Math2.almostEqual(14, 0.12345678901234, 0.12345678901235), false, "e");
    Test.ensureEqual(Math2.almostEqual(14, 12345678901234567.0, 12345678901234566.0), true, "f");
    Test.ensureEqual(Math2.almostEqual(14, 12345678901234.0, 12345678901235.0), false, "g");
    Test.ensureEqual(Math2.almostEqual(14, Double.NaN, Double.NaN), false, "h");
    Test.ensureEqual(Math2.almostEqual(14, Double.POSITIVE_INFINITY, 1e300), false, "i");
    Test.ensureEqual(Math2.almostEqual(14, Double.POSITIVE_INFINITY, Double.NaN), false, "j");

    // lessThanAE5
    String2.log("test lessThanAE5");
    Test.ensureEqual(Math2.lessThanAE(5, 1, 2), true, "");
    Test.ensureEqual(Math2.lessThanAE(5, 1, 1), true, "");
    Test.ensureEqual(Math2.lessThanAE(5, 2, 1), false, "");
    Test.ensureEqual(Math2.lessThanAE(5, 1, 1.000001), true, "");
    Test.ensureEqual(Math2.lessThanAE(5, 1.000001, 1), true, "");

    // greaterThanAE5
    String2.log("test greaterThanAE5");
    Test.ensureEqual(Math2.greaterThanAE(5, 2, 1), true, "");
    Test.ensureEqual(Math2.greaterThanAE(5, 1, 1), true, "");
    Test.ensureEqual(Math2.greaterThanAE(5, 1, 2), false, "");
    Test.ensureEqual(Math2.greaterThanAE(5, 1, 1.000001), true, "");
    Test.ensureEqual(Math2.greaterThanAE(5, 1.000001, 1), true, "");

    // roundDiv
    String2.log("test roundDiv");
    Test.ensureEqual(Math2.roundDiv(1, 4), 0, "a");
    Test.ensureEqual(Math2.roundDiv(3, 4), 1, "b");
    Test.ensureEqual(Math2.roundDiv(5, 4), 1, "c");
    Test.ensureEqual(Math2.roundDiv(7, 4), 2, "d");
    Test.ensureEqual(Math2.roundDiv(8, 4), 2, "e");

    // hiDiv
    String2.log("test hiDiv");
    Test.ensureEqual(Math2.hiDiv(0, 4), 0, "a");
    Test.ensureEqual(Math2.hiDiv(1, 4), 1, "b");
    Test.ensureEqual(Math2.hiDiv(4, 4), 1, "c");
    Test.ensureEqual(Math2.hiDiv(5, 4), 2, "d");
    Test.ensureEqual(Math2.hiDiv(8, 4), 2, "e");

    // floorDiv
    String2.log("test floorDiv");
    Test.ensureEqual(Math2.floorDiv(0, 2), 0, "a");
    Test.ensureEqual(Math2.floorDiv(1, 2), 0, "b");
    Test.ensureEqual(Math2.floorDiv(-1, 2), -1, "c");
    Test.ensureEqual(Math2.floorDiv(2, 2), 1, "d");
    Test.ensureEqual(Math2.floorDiv(-2, 2), -1, "e");
    Test.ensureEqual(Math2.floorDiv(3, 2), 1, "f");
    Test.ensureEqual(Math2.floorDiv(-3, 2), -2, "g");

    // minMax
    String2.log("test minMax");
    Test.ensureEqual(Math2.minMax(0, 10, -1), 0, "a");
    Test.ensureEqual(Math2.minMax(0, 10, 4), 4, "b");
    Test.ensureEqual(Math2.minMax(0, 10, 11), 10, "c");

    // minMax
    String2.log("test minMax double");
    Test.ensureEqual(Math2.minMax(0, 10, -1.1), 0, "a");
    Test.ensureEqual(Math2.minMax(0, 10, 4.1), 4.1, "b");
    Test.ensureEqual(Math2.minMax(0, 10, 11.1), 10, "c");

    // minMaxDef
    String2.log("test minMaxDef");
    Test.ensureEqual(Math2.minMaxDef(0, 10, 5, -1), 5, "a");
    Test.ensureEqual(Math2.minMaxDef(0, 10, 5, 4), 4, "b");
    Test.ensureEqual(Math2.minMaxDef(0, 10, 5, 11), 5, "c");

    // minMaxDef
    String2.log("test minMaxDef double");
    Test.ensureEqual(Math2.minMaxDef(0, 10, 5, -1.1), 5, "a");
    Test.ensureEqual(Math2.minMaxDef(0, 10, 5, 4.1), 4.1, "b");
    Test.ensureEqual(Math2.minMaxDef(0, 10, 5, 11.1), 5, "c");

    // roundToByte
    String2.log("test roundToByte");
    Test.ensureEqual(Math2.roundToByte(-1.49), -1, "a");
    Test.ensureEqual(Math2.roundToByte(-0.6), -1, "b");
    Test.ensureEqual(Math2.roundToByte(-0.5), 0, "c");
    Test.ensureEqual(Math2.roundToByte(-0.1), 0, "d");
    Test.ensureEqual(Math2.roundToByte(0), 0, "e");
    Test.ensureEqual(Math2.roundToByte(0.1), 0, "f");
    Test.ensureEqual(Math2.roundToByte(0.5), 1, "g");
    Test.ensureEqual(Math2.roundToByte(0.6), 1, "h");
    Test.ensureEqual(Math2.roundToByte(1.49), 1, "i");
    Test.ensureEqual(Math2.roundToByte(1.5), 2, "j");
    Test.ensureEqual(Math2.roundToByte(Byte.MIN_VALUE - 0.499), Byte.MIN_VALUE, "k");
    Test.ensureEqual(Math2.roundToByte(Byte.MAX_VALUE + 0.499), Byte.MAX_VALUE, "l");
    Test.ensureEqual(Math2.roundToByte(Double.NaN), Byte.MAX_VALUE, "n");
    Test.ensureEqual(Math2.roundToByte(Double.POSITIVE_INFINITY), Byte.MAX_VALUE, "o");
    Test.ensureEqual(Math2.roundToByte(Double.NEGATIVE_INFINITY), Byte.MAX_VALUE, "p");

    // roundToChar
    String2.log("test roundToChar");
    Test.ensureEqual(Math2.roundToChar(-1.49), Character.MAX_VALUE, "a");
    Test.ensureEqual(Math2.roundToChar(-0.6), Character.MAX_VALUE, "b");
    Test.ensureEqual(Math2.roundToChar(-0.5), Character.MAX_VALUE, "c");
    Test.ensureEqual(Math2.roundToChar(-0.1), 0, "d");
    Test.ensureEqual(Math2.roundToChar(0), 0, "e");
    Test.ensureEqual(Math2.roundToChar(0.1), 0, "f");
    Test.ensureEqual(Math2.roundToChar(0.5), 1, "g");
    Test.ensureEqual(Math2.roundToChar(0.6), 1, "h");
    Test.ensureEqual(Math2.roundToChar(1.49), 1, "i");
    Test.ensureEqual(Math2.roundToChar(1.5), 2, "j");
    Test.ensureEqual(Math2.roundToChar(Character.MIN_VALUE - 0.499), Character.MIN_VALUE, "k");
    Test.ensureEqual(Math2.roundToChar(Character.MAX_VALUE + 0.499), Character.MAX_VALUE, "l");
    Test.ensureEqual(Math2.roundToChar(Double.NaN), Character.MAX_VALUE, "n");
    Test.ensureEqual(Math2.roundToChar(Double.POSITIVE_INFINITY), Character.MAX_VALUE, "o");
    Test.ensureEqual(Math2.roundToChar(Double.NEGATIVE_INFINITY), Character.MAX_VALUE, "p");

    // roundToShort
    String2.log("test roundToShort");
    Test.ensureEqual(Math2.roundToShort(-1.49), -1, "a");
    Test.ensureEqual(Math2.roundToShort(-0.6), -1, "b");
    Test.ensureEqual(Math2.roundToShort(-0.5), 0, "c");
    Test.ensureEqual(Math2.roundToShort(-0.1), 0, "d");
    Test.ensureEqual(Math2.roundToShort(0), 0, "e");
    Test.ensureEqual(Math2.roundToShort(0.1), 0, "f");
    Test.ensureEqual(Math2.roundToShort(0.5), 1, "g");
    Test.ensureEqual(Math2.roundToShort(0.6), 1, "h");
    Test.ensureEqual(Math2.roundToShort(1.49), 1, "i");
    Test.ensureEqual(Math2.roundToShort(1.5), 2, "j");
    Test.ensureEqual(Math2.roundToShort(Short.MIN_VALUE - 0.499), Short.MIN_VALUE, "k");
    Test.ensureEqual(Math2.roundToShort(Short.MAX_VALUE + 0.499), Short.MAX_VALUE, "l");
    Test.ensureEqual(Math2.roundToShort(Double.NaN), Short.MAX_VALUE, "n");
    Test.ensureEqual(Math2.roundToShort(Double.POSITIVE_INFINITY), Short.MAX_VALUE, "o");
    Test.ensureEqual(Math2.roundToShort(Double.NEGATIVE_INFINITY), Short.MAX_VALUE, "p");

    // roundToInt
    String2.log("test roundToInt");
    Test.ensureEqual(Math2.roundToInt(-1.49), -1, "a");
    Test.ensureEqual(Math2.roundToInt(-0.6), -1, "b");
    Test.ensureEqual(Math2.roundToInt(-0.5), 0, "c");
    Test.ensureEqual(Math2.roundToInt(-0.1), 0, "d");
    Test.ensureEqual(Math2.roundToInt(0), 0, "e");
    Test.ensureEqual(Math2.roundToInt(0.1), 0, "f");
    Test.ensureEqual(Math2.roundToInt(0.5), 1, "g");
    Test.ensureEqual(Math2.roundToInt(0.6), 1, "h");
    Test.ensureEqual(Math2.roundToInt(1.49), 1, "i");
    Test.ensureEqual(Math2.roundToInt(1.5), 2, "j");
    Test.ensureEqual(Math2.roundToInt(Integer.MIN_VALUE - 0.499), Integer.MIN_VALUE, "k");
    Test.ensureEqual(Math2.roundToInt(Integer.MAX_VALUE + 0.499), Integer.MAX_VALUE, "l");
    Test.ensureEqual(Math2.roundToInt(Double.NaN), Integer.MAX_VALUE, "n");
    Test.ensureEqual(Math2.roundToInt(Double.POSITIVE_INFINITY), Integer.MAX_VALUE, "o");
    Test.ensureEqual(Math2.roundToInt(Double.NEGATIVE_INFINITY), Integer.MAX_VALUE, "p");

    // roundToLong
    String2.log("test roundToLong");
    Test.ensureEqual(Math2.roundToLong(-1.49), -1, "a");
    Test.ensureEqual(Math2.roundToLong(-0.6), -1, "b");
    Test.ensureEqual(Math2.roundToLong(-0.5), 0, "c");
    Test.ensureEqual(Math2.roundToLong(-0.1), 0, "d");
    Test.ensureEqual(Math2.roundToLong(0), 0, "e");
    Test.ensureEqual(Math2.roundToLong(0.1), 0, "f");
    Test.ensureEqual(Math2.roundToLong(0.5), 1, "g");
    Test.ensureEqual(Math2.roundToLong(0.6), 1, "h");
    Test.ensureEqual(Math2.roundToLong(1.49), 1, "i");
    // MIN_VALUE -9223372036854775808.0
    Test.ensureEqual(
        Math2.roundToLong(-9223372036854777000.0), Long.MAX_VALUE, "k"); // unusual: limited double
    // precision throws this to mv
    Test.ensureEqual(
        Math2.roundToLong(Long.MIN_VALUE),
        Long.MIN_VALUE,
        "k"); // unusual: limited double precision
    // throws this to mv
    Test.ensureEqual(
        Math2.roundToLong(9.223372036854774784E18), // largest that can do round trip
        9223372036854774784L,
        "l");
    Test.ensureEqual(Math2.roundToLong(Long.MAX_VALUE), Long.MAX_VALUE, "l");
    Test.ensureEqual(Math2.roundToLong(Double.NaN), Long.MAX_VALUE, "m");
    Test.ensureEqual(Math2.roundToLong(Double.POSITIVE_INFINITY), Long.MAX_VALUE, "o");
    Test.ensureEqual(Math2.roundToLong(Double.NEGATIVE_INFINITY), Long.MAX_VALUE, "p");

    // roundToDouble
    String2.log("test roundToDouble");
    Test.ensureEqual(Math2.roundToDouble(-1.49), -1, "a");
    Test.ensureEqual(Math2.roundToDouble(-0.6), -1, "b");
    Test.ensureEqual(Math2.roundToDouble(-0.5), 0, "c");
    Test.ensureEqual(Math2.roundToDouble(-0.1), 0, "d");
    Test.ensureEqual(Math2.roundToDouble(0), 0, "e");
    Test.ensureEqual(Math2.roundToDouble(0.1), 0, "f");
    Test.ensureEqual(Math2.roundToDouble(0.5), 1, "g");
    Test.ensureEqual(Math2.roundToDouble(0.6), 1, "h");
    Test.ensureEqual(Math2.roundToDouble(1.49), 1, "i");
    Test.ensureEqual(Math2.roundToDouble(1e100), 1e100, "j");
    Test.ensureEqual(Math2.roundToDouble(Double.POSITIVE_INFINITY), Double.NaN, "k");
    Test.ensureEqual(Math2.roundToDouble(Double.NaN), Double.NaN, "l");

    // roundTo
    String2.log("test roundTo");
    Test.ensureEqual(Math2.roundTo(21.23456, 3), 21.235, "a");
    Test.ensureEqual(Math2.roundTo(21.23456, 0), 21, "b");
    Test.ensureEqual(Math2.roundTo(21.23456, -1), 20, "c");

    // narrowToByte
    String2.log("test narrowToByte");
    Test.ensureEqual(Math2.narrowToByte(Byte.MAX_VALUE + 1), Byte.MAX_VALUE, "a");
    Test.ensureEqual(Math2.narrowToByte(Byte.MIN_VALUE - 1), Byte.MAX_VALUE, "b");
    Test.ensureEqual(Math2.narrowToByte(5), 5, "c");

    // narrowToUByte
    String2.log("test narrowToUByte");
    Test.ensureEqual(Math2.narrowToUByte(0), 0, "");
    Test.ensureEqual(Math2.narrowToUByte(127), 127, "");
    Test.ensureEqual(Math2.narrowToUByte(254), 254, "");
    Test.ensureEqual(Math2.narrowToUByte(255), 255, "");
    Test.ensureEqual(Math2.narrowToUByte(-1), 255, "");
    Test.ensureEqual(Math2.narrowToUByte(256), 255, "");

    // narrowToChar
    String2.log("test narrowToChar");
    Test.ensureEqual(Math2.narrowToChar(Character.MAX_VALUE + 1), Character.MAX_VALUE, "a");
    Test.ensureEqual(Math2.narrowToChar(Character.MIN_VALUE - 1), Character.MAX_VALUE, "b");
    Test.ensureEqual(Math2.narrowToChar(5), 5, "c");

    // narrowToShort
    String2.log("test narrowToShort");
    Test.ensureEqual(Math2.narrowToShort(Short.MAX_VALUE + 1), Short.MAX_VALUE, "a");
    Test.ensureEqual(Math2.narrowToShort(Short.MIN_VALUE - 1), Short.MAX_VALUE, "b");
    Test.ensureEqual(Math2.narrowToShort(5), 5, "c");

    // narrowToUShort
    String2.log("test narrowToUShort");
    Test.ensureEqual(Math2.narrowToUShort(0), 0, "");
    Test.ensureEqual(Math2.narrowToUShort(32767), 32767, "");
    Test.ensureEqual(Math2.narrowToUShort(65534), 65534, "");
    Test.ensureEqual(Math2.narrowToUShort(65535), 65535, "");
    Test.ensureEqual(Math2.narrowToUShort(-1), 65535, "");
    Test.ensureEqual(Math2.narrowToUShort(65536), 65535, "");

    // narrowToUInt
    String2.log("test narrowToUInt");
    Test.ensureEqual(Math2.narrowToUInt(0), 0, "");
    Test.ensureEqual(Math2.narrowToUInt(2147483647), 2147483647, "");
    Test.ensureEqual(Math2.narrowToUInt(4294967294L), 4294967294L, "");
    Test.ensureEqual(Math2.narrowToUInt(4294967295L), 4294967295L, "");
    Test.ensureEqual(Math2.narrowToUInt(-1), 4294967295L, "");
    Test.ensureEqual(Math2.narrowToUInt(4294967296L), 4294967295L, "");

    // floatToDouble
    String2.log("test floatToDouble");
    Test.ensureEqual(Math2.floatToDouble(3.5000000432), 3.5, "k");
    Test.ensureEqual(Math2.floatToDouble(3.5), 3.5f, "k");
    Test.ensureEqual(Math2.floatToDouble(1e32f), 1e32, "k");
    Test.ensureEqual(Math2.floatToDouble(-1e32f), -1e32, "l");
    Test.ensureEqual(Math2.floatToDouble(Float.NaN), Double.NaN, "m");
    Test.ensureEqual(Math2.floatToDouble(Float.POSITIVE_INFINITY), Double.POSITIVE_INFINITY, "o");
    Test.ensureEqual(Math2.floatToDouble(Float.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY, "p");

    // floatToDoubleNaN
    String2.log("test floatToDoubleNaN");
    Test.ensureEqual(Math2.floatToDoubleNaN(3.5f), 3.5, "k");
    Test.ensureEqual(Math2.floatToDoubleNaN(1e32f), 1e32f, "k"); // f rebruises it
    Test.ensureEqual(Math2.floatToDoubleNaN(-1e32f), -1e32f, "l"); // f rebruises it
    Test.ensureEqual(Math2.floatToDoubleNaN(Float.NaN), Double.NaN, "m");
    Test.ensureEqual(Math2.floatToDoubleNaN(Float.POSITIVE_INFINITY), Double.NaN, "o");
    Test.ensureEqual(Math2.floatToDoubleNaN(Float.NEGATIVE_INFINITY), Double.NaN, "p");

    // doubleToFloatNaN
    String2.log("test doubleToFloatNaN");
    Test.ensureEqual(Math2.doubleToFloatNaN(3.5), 3.5f, "k");
    Test.ensureEqual(Math2.doubleToFloatNaN(1e100), Float.NaN, "k");
    Test.ensureEqual(Math2.doubleToFloatNaN(-1e100), Float.NaN, "l");
    Test.ensureEqual(Math2.doubleToFloatNaN(Double.NaN), Float.NaN, "m");
    Test.ensureEqual(Math2.doubleToFloatNaN(Double.POSITIVE_INFINITY), Float.NaN, "o");
    Test.ensureEqual(Math2.doubleToFloatNaN(Double.NEGATIVE_INFINITY), Float.NaN, "p");

    // (float)
    String2.log("test (float)d");
    Test.ensureEqual((float) 1e100, Float.POSITIVE_INFINITY, "k");
    Test.ensureEqual((float) -1e100, Float.NEGATIVE_INFINITY, "l");
    Test.ensureEqual((float) Double.NaN, Float.NaN, "m");
    Test.ensureEqual((float) Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, "o");
    Test.ensureEqual((float) Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, "p");

    // byteToChar
    String2.log("test byteToChar");
    Test.ensureEqual(Math2.byteToChar(5), 5, "a");
    Test.ensureEqual(Math2.byteToChar(0), 0, "b");
    Test.ensureEqual(Math2.byteToChar(-1), 255, "c");

    // byteToChar
    String2.log("test unsignedByte");
    Test.ensureEqual(Math2.unsignedByte(5), 5, "a");
    Test.ensureEqual(Math2.unsignedByte(0), 0, "b");
    Test.ensureEqual(Math2.unsignedByte(-1), 255, "c");

    // angle0360
    String2.log("test angle0360");
    Test.ensureEqual(Math2.angle0360(17), 17, "a");
    Test.ensureEqual(Math2.angle0360(-90), 270, "b");
    Test.ensureEqual(Math2.angle0360(-360 - 90), 270, "c");
    Test.ensureEqual(Math2.angle0360(365), 5, "d");
    Test.ensureEqual(Math2.angle0360(3605), 5, "e");

    // anglePM180
    String2.log("test anglePM180");
    Test.ensureEqual(Math2.anglePM180(17), 17, "a");
    Test.ensureEqual(Math2.anglePM180(270), -90, "b");
    Test.ensureEqual(Math2.anglePM180(270 + 360), -90, "c");
    Test.ensureEqual(Math2.anglePM180(-90), -90, "d");
    Test.ensureEqual(Math2.anglePM180(-90 - 360), -90, "e");

    // angle02Pi
    String2.log("test angle02Pi");
    Test.ensureEqual(Math2.angle02Pi(3), 3, "a");
    Test.ensureEqual(Math2.angle02Pi(3 + 2 * Math.PI), 3, "b");
    Test.ensureEqual(Math2.angle02Pi(3 - 4 * Math.PI), 3, "c");

    // compassToMathDegrees
    String2.log("test compassToMathDegrees");
    Test.ensureEqual(Math2.compassToMathDegrees(0), 90, "a");
    Test.ensureEqual(Math2.compassToMathDegrees(90), 0, "b");
    Test.ensureEqual(Math2.compassToMathDegrees(180), 270, "c");
    Test.ensureEqual(Math2.compassToMathDegrees(270), 180, "d");

    // mathToCompassDegrees
    String2.log("test mathToCompassDegrees");
    Test.ensureEqual(Math2.mathToCompassDegrees(0), 90, "a");
    Test.ensureEqual(Math2.mathToCompassDegrees(90), 0, "b");
    Test.ensureEqual(Math2.mathToCompassDegrees(180), 270, "c");
    Test.ensureEqual(Math2.mathToCompassDegrees(270), 180, "d");

    // gcd
    String2.log("test gcd");
    Test.ensureEqual(Math2.gcd(18, 30), 6, "a");
    Test.ensureEqual(Math2.gcd(5, 2), 1, "b");
    Test.ensureEqual(Math2.gcd(2, 5), 1, "c");
    Test.ensureEqual(Math2.gcd(5, 0), 1, "d");
    Test.ensureEqual(Math2.gcd(0, 5), 1, "e");

    // guessFrac
    String2.log("test guessFrac");
    int int3[] = new int[3];
    Math2.guessFrac(18.6, int3);
    Test.ensureEqual(int3[0], 18, "a");
    Test.ensureEqual(int3[1], 3, "b");
    Test.ensureEqual(int3[2], 5, "c");
    Math2.guessFrac(1.3333, int3);
    Test.ensureEqual(int3[0], 1, "d");
    Test.ensureEqual(int3[1], 1, "e");
    Test.ensureEqual(int3[2], 3, "f");
    Math2.guessFrac(-7.127, int3);
    Test.ensureEqual(int3[0], -7, "g");
    Test.ensureEqual(int3[1], -127, "h");
    Test.ensureEqual(int3[2], 1000, "i");
    Math2.guessFrac(2, int3);
    Test.ensureEqual(int3[0], 2, "j");
    Test.ensureEqual(int3[1], 0, "k");
    Test.ensureEqual(int3[2], 1, "l");

    // guessFracString
    String2.log("test guessFracString");
    Test.ensureEqual(Math2.guessFracString(18.6), "18 3/5", "a");
    Test.ensureEqual(Math2.guessFracString(1.3333), "1 1/3", "b");
    Test.ensureEqual(Math2.guessFracString(-7.127), "-7 127/1000", "c");
    Test.ensureEqual(Math2.guessFracString(-.127), "-127/1000", "d");
    Test.ensureEqual(Math2.guessFracString(2), "2", "e");

    // floatToDouble
    String2.log("test floatToDouble");
    Test.ensureEqual(Math2.floatToDouble(4f / 3), 1.333333, "a");
    Test.ensureEqual(Math2.floatToDouble(-4f / 3), -1.333333, "b");
    Test.ensureEqual(Math2.floatToDouble(Float.NaN), Double.NaN, "c");
    Test.ensureEqual(Math2.floatToDouble(Float.POSITIVE_INFINITY), Double.POSITIVE_INFINITY, "d");
    Test.ensureEqual(Math2.floatToDouble(Float.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY, "e");

    // niceDouble
    String2.log("test niceDouble");
    Test.ensureEqual(Math2.niceDouble(8.999999999999999, 7), 9, "a");
    Test.ensureEqual(Math2.niceDouble(-4f / 3, 7), -1.333333, "b");

    // random
    String2.log("test random");
    Math2.setSeed(35487099483750183L);
    Test.ensureEqual(Math2.random(5), 4, "a");
    Test.ensureEqual(Math2.random(5), 2, "b");
    Test.ensureEqual(Math2.random(5), 2, "c");

    // bigger
    String2.log("test bigger");
    Test.ensureEqual(Math2.bigger(1), 1.1, "a");
    Test.ensureEqual(Math2.bigger(-1.1), -1, "b");
    Test.ensureEqual(Math2.bigger(0), 0.01, "c");
    Test.ensureEqual(Math2.bigger(Double.NaN), Double.NaN, "d");

    // smaller
    String2.log("test smaller");
    Test.ensureEqual(Math2.smaller(1.1), 1, "a");
    Test.ensureEqual(Math2.smaller(-1), -1.1, "b");
    Test.ensureEqual(Math2.smaller(0), -0.01, "c");
    Test.ensureEqual(Math2.smaller(Double.NaN), Double.NaN, "d");

    // oneDigitBigger
    String2.log("test oneDigitBigger");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, 0.0234), 0.03, "a");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, -0.0234), -0.01, "b");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, 0.016), 0.03, "c");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, -0.016), -0.01, "d");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, 312.9), 400, "e");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, -312.9), -200, "f");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, 912.9), 1000, "g");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, -912.9), -800, "h");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, Double.NaN), 2, "i");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, 0), 1, "j");
    Test.ensureEqual(Math2.oneDigitBigger(10000, 2, 9999), 10000, "k");

    // oneDigitSmaller
    String2.log("test oneDigitSmaller");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, 0.0234), 0.01, "a");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, -0.0234), -0.03, "b");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, 0.016), 0.01, "c");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, -0.016), -0.03, "d");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, 312.9), 200, "e");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, -312.9), -400, "f");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, 1012.9), 900, "g");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, -912.9), -1000, "h");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, Double.NaN), 2, "i");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, 0), -1, "j");
    Test.ensureEqual(Math2.oneDigitSmaller(-10000, 2, -9999), -10000, "k");

    // getSmallIncrement
    String2.log("test getSmallIncrement");
    Test.ensureEqual(Math2.getSmallIncrement(37.2), 1, "a");
    Test.ensureEqual(Math2.getSmallIncrement(0.5), 0.01, "b");

    // biggerDouble
    String2.log("test biggerDouble");
    Test.ensureEqual(Math2.biggerDouble(9, 2, 20, 4.2), 6, "a");
    Test.ensureEqual(Math2.biggerDouble(9, 2, 20, 19.8), 20, "b");
    Test.ensureEqual(Math2.biggerDouble(9, 2, 20, 29.8), 20, "c");
    Test.ensureEqual(Math2.biggerDouble(9, 2, 20, Double.NaN), 9, "d");

    // smallerDouble
    String2.log("test smallerDouble");
    Test.ensureEqual(Math2.smallerDouble(9, 2, 0, 4.2), 2, "a");
    Test.ensureEqual(Math2.smallerDouble(9, 2, 0, 0.2), 0, "b");
    Test.ensureEqual(Math2.smallerDouble(9, 2, 0, -2.2), 0, "c");
    Test.ensureEqual(Math2.smallerDouble(9, 2, 0, Double.NaN), 9, "d");

    // biggerAngle
    String2.log("test biggerAngle");
    Test.ensureEqual(Math2.biggerAngle(16), 30, "a");
    Test.ensureEqual(Math2.biggerAngle(365), 15, "b");
    Test.ensureEqual(Math2.biggerAngle(3605), 15, "c");
    Test.ensureEqual(Math2.biggerAngle(Double.NaN), Double.NaN, "d");

    // smallerAngle
    String2.log("test smallerAngle");
    Test.ensureEqual(Math2.smallerAngle(16), 0, "a");
    Test.ensureEqual(Math2.smallerAngle(365), 345, "b");
    Test.ensureEqual(Math2.smallerAngle(3605), 345, "c");
    Test.ensureEqual(Math2.smallerAngle(Double.NaN), Double.NaN, "d");

    // bigger15
    String2.log("test bigger15");
    Test.ensureEqual(Math2.bigger15(0.7), 1, "a");
    Test.ensureEqual(Math2.bigger15(1.2), 5, "b");
    Test.ensureEqual(Math2.bigger15(4.5), 10, "c");
    Test.ensureEqual(Math2.bigger15(Double.NaN), Double.NaN, "d");

    // smaller15
    String2.log("test smaller15");
    Test.ensureEqual(Math2.smaller15(30), 10, "a");
    Test.ensureEqual(Math2.smaller15(9), 5, "b");
    Test.ensureEqual(Math2.smaller15(4), 1, "c");
    Test.ensureEqual(Math2.smaller15(Double.NaN), Double.NaN, "d");

    // suggestLowHigh
    String2.log("test suggestLowHigh");
    double lowHigh[];
    lowHigh = Math2.suggestLowHigh(432, Double.NaN); // low is NaN
    Test.ensureEqual(lowHigh[0], 400, "");
    Test.ensureEqual(lowHigh[1], 900, "");
    lowHigh = Math2.suggestLowHigh(-432, Double.NaN); // low is NaN
    Test.ensureEqual(lowHigh[0], -450, "");
    Test.ensureEqual(lowHigh[1], -200, "");
    lowHigh = Math2.suggestLowHigh(Double.NaN, 1547); // high is NaN
    Test.ensureEqual(lowHigh[0], 600, "");
    Test.ensureEqual(lowHigh[1], 1600, "");
    lowHigh = Math2.suggestLowHigh(Double.NaN, -1547); // high is NaN
    Test.ensureEqual(lowHigh[0], -3500, "");
    Test.ensureEqual(lowHigh[1], -1500, "");
    lowHigh = Math2.suggestLowHigh(Double.NaN, Double.NaN); // one is NaN
    Test.ensureEqual(lowHigh[0], 0, "");
    Test.ensureEqual(lowHigh[1], 1, "");
    lowHigh = Math2.suggestLowHigh(0, 0); // both 0
    Test.ensureEqual(lowHigh[0], -1, "");
    Test.ensureEqual(lowHigh[1], 1, "");
    lowHigh = Math2.suggestLowHigh(2, 2); // positive and ==
    Test.ensureEqual(lowHigh[0], 1.9, "");
    Test.ensureEqual(lowHigh[1], 2.1, "");
    lowHigh = Math2.suggestLowHigh(-2, -2); // negative and ==
    Test.ensureEqual(lowHigh[0], -2.1, "");
    Test.ensureEqual(lowHigh[1], -1.9, "");
    lowHigh = Math2.suggestLowHigh(0, 10); // 0 low is special case, high has buffer zone
    Test.ensureEqual(lowHigh[0], 0, "a");
    Test.ensureEqual(lowHigh[1], 15, "b");
    lowHigh = Math2.suggestLowHigh(0, 1214); // 0 low is special case, high has buffer zone
    Test.ensureEqual(lowHigh[0], 0, "a");
    Test.ensureEqual(lowHigh[1], 1500, "b");
    lowHigh = Math2.suggestLowHigh(5, 10); // high has buffer zone
    Test.ensureEqual(lowHigh[0], 4, "a2");
    Test.ensureEqual(lowHigh[1], 11, "b2");
    lowHigh = Math2.suggestLowHigh(0.1, 9.9);
    Test.ensureEqual(lowHigh[0], 0, "c");
    Test.ensureEqual(lowHigh[1], 10, "d");
    lowHigh = Math2.suggestLowHigh(0, 14234);
    Test.ensureEqual(lowHigh[0], 0, "c2");
    Test.ensureEqual(lowHigh[1], 15000, "d2");
    lowHigh = Math2.suggestLowHigh(-9.9, -0.1);
    Test.ensureEqual(lowHigh[0], -10, "e");
    Test.ensureEqual(lowHigh[1], 0, "f");
    lowHigh = Math2.suggestLowHigh(1.2, 8.5);
    Test.ensureEqual(lowHigh[0], 0, "g");
    Test.ensureEqual(lowHigh[1], 10, "h");
    lowHigh = Math2.suggestLowHigh(2.2, 6.9);
    Test.ensureEqual(lowHigh[0], 2, "i");
    Test.ensureEqual(lowHigh[1], 7, "j");
    lowHigh = Math2.suggestLowHigh(1012, 3789);
    Test.ensureEqual(lowHigh[0], 0, "k");
    Test.ensureEqual(lowHigh[1], 4000, "l");

    // suggestDivisions;
    String2.log("test suggestDivision");
    double divisions[];
    divisions = Math2.suggestDivisions(Double.NaN);
    Test.ensureEqual(divisions[0], Double.NaN, "");
    Test.ensureEqual(divisions[1], Double.NaN, "");
    divisions = Math2.suggestDivisions(0);
    Test.ensureEqual(divisions[0], 1, "");
    Test.ensureEqual(divisions[1], 0.5, "");
    divisions = Math2.suggestDivisions(10);
    Test.ensureEqual(divisions[0], 2, "a");
    Test.ensureEqual(divisions[1], 1, "b");
    divisions = Math2.suggestDivisions(9.8);
    Test.ensureEqual(divisions[0], 2, "c");
    Test.ensureEqual(divisions[1], 1, "d");
    divisions = Math2.suggestDivisions(-6.3);
    Test.ensureEqual(divisions[0], 1, "g");
    Test.ensureEqual(divisions[1], 0.2, "h");
    divisions = Math2.suggestDivisions(.047);
    Test.ensureEqual(divisions[0], .01, "i");
    Test.ensureEqual(divisions[1], .002, "j");
    divisions = Math2.suggestDivisions(2775);
    Test.ensureEqual(divisions[0], 500, "k");
    Test.ensureEqual(divisions[1], 100, "l");

    // suggestMaxDivisions
    Test.ensureEqual(Math2.suggestMaxDivisions(999.9, 500), 2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(1000.0, 500), 2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(1000.1, 500), 5, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(150, 500), .5, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(200, 500), .5, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(250, 500), .5, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(251, 500), 1, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(300, 500), 1, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(400, 500), 1, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(500, 500), 1, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(501, 500), 2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(600, 500), 2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(700, 500), 2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(800, 500), 2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(900, 500), 2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(0, 500), 1, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(900, 0), 900, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(-900, 500), -2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(900, -500), 2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(-900, -500), -2, "");
    Test.ensureEqual(Math2.suggestMaxDivisions(Double.NaN, 500), Double.NaN, "");

    // binaryFindLastLE
    String2.log("test binaryFindLastLE");
    double sorted[] = {1, 7, 14, 21};
    double sortedDuplicates[] = {1, 2, 2, 2, 2, 2, 3};
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, -1), -1, "a");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 0.99999999999), -1, "b");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 1), 0, "c");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 1.00000000001), 0, "d");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 6), 0, "e");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 6.99999999999), 0, "f");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 7), 1, "g");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 7.00000000001), 1, "h");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 8), 1, "i");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 13.99999999999), 1, "j");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 14), 2, "k");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 14.00000000001), 2, "l");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 15), 2, "m");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 20.99999999999), 2, "n");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 21), 3, "o");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 21.00000000001), 3, "p");
    Test.ensureEqual(Math2.binaryFindLastLE(sorted, 22), 3, "q");
    // Test.ensureEqual(Math2.binaryFindLastLE(sorted, Double.NaN), 0, "r");

    // double sortedDuplicates[] = {1, 2,2,2,2,2, 3};
    Test.ensureEqual(Math2.binaryFindLastLE(sortedDuplicates, 0.5), -1, "");
    Test.ensureEqual(Math2.binaryFindLastLE(sortedDuplicates, 1.5), 0, "");
    Test.ensureEqual(Math2.binaryFindLastLE(sortedDuplicates, 2.5), 5, "");
    Test.ensureEqual(Math2.binaryFindLastLE(sortedDuplicates, 3.5), 6, "");

    // binaryFindLastLAE lastParam: precision = 9
    String2.log("test binaryFindLastLAE");
    // double sorted[] = {1, 7, 14, 21};
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, -1, 9), -1, "a");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 0.99999999999, 9), 0, "b");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 1, 9), 0, "c");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 1.00000000001, 9), 0, "d");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 6, 9), 0, "e");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 6.99999999999, 9), 1, "f");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 7, 9), 1, "g");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 7.00000000001, 9), 1, "h");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 8, 9), 1, "i");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 13.99999999999, 9), 2, "j");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 14, 9), 2, "k");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 14.00000000001, 9), 2, "l");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 15, 9), 2, "m");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 20.99999999999, 9), 3, "n");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 21, 9), 3, "o");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 21.00000000001, 9), 3, "p");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 22, 9), 3, "q");
    // Test.ensureEqual(Math2.binaryFindLastLAE(sorted, Double.NaN, 9), 0, "r");

    // double sortedDuplicates[] = {1, 2,2,2,2,2, 3}; lastParam: precision = 9
    Test.ensureEqual(Math2.binaryFindLastLAE(sortedDuplicates, 0.5, 9), -1, "");
    Test.ensureEqual(Math2.binaryFindLastLAE(sortedDuplicates, 1.5, 9), 0, "");
    Test.ensureEqual(Math2.binaryFindLastLAE(sortedDuplicates, 2.5, 9), 5, "");
    Test.ensureEqual(Math2.binaryFindLastLAE(sortedDuplicates, 3.5, 9), 6, "");

    // binaryFindLastLAE5 lastParam: precision = 5
    String2.log("test binaryFindLastLAE");
    // double sorted[] = {1, 7, 14, 21};
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, -1, 5), -1, "a");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 0.9999999, 5), 0, "b");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 1, 5), 0, "c");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 1.0000001, 5), 0, "d");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 6, 5), 0, "e");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 6.9999999, 5), 1, "f");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 7, 5), 1, "g");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 7.0000001, 5), 1, "h");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 8, 5), 1, "i");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 13.9999999, 5), 2, "j");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 14, 5), 2, "k");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 14.0000001, 5), 2, "l");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 15, 5), 2, "m");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 20.9999999, 5), 3, "n");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 21, 5), 3, "o");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 21.0000001, 5), 3, "p");
    Test.ensureEqual(Math2.binaryFindLastLAE(sorted, 22, 5), 3, "q");
    // Test.ensureEqual(Math2.binaryFindLastLAE(sorted, Double.NaN, 5),0, "r");

    // double sortedDuplicates[] = {1, 2,2,2,2,2, 3}; lastParam: precision = 5
    Test.ensureEqual(Math2.binaryFindLastLAE(sortedDuplicates, 0.5, 5), -1, "");
    Test.ensureEqual(Math2.binaryFindLastLAE(sortedDuplicates, 1.5, 5), 0, "");
    Test.ensureEqual(Math2.binaryFindLastLAE(sortedDuplicates, 2.5, 5), 5, "");
    Test.ensureEqual(Math2.binaryFindLastLAE(sortedDuplicates, 3.5, 5), 6, "");

    // binaryFindFirstGE
    String2.log("test binaryFindFirstGE");
    // double sorted[] = {1, 7, 14, 21};
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, -1), 0, "a");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 0.99999999999), 0, "b");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 1), 0, "c");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 1.00000000001), 1, "d");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 6), 1, "e");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 6.99999999999), 1, "f");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 7), 1, "g");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 7.00000000001), 2, "h");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 8), 2, "i");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 13.99999999999), 2, "j");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 14), 2, "k");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 14.00000000001), 3, "l");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 15), 3, "m");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 20.99999999999), 3, "n");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 21), 3, "o");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 21.00000000001), 4, "p");
    Test.ensureEqual(Math2.binaryFindFirstGE(sorted, 22), 4, "q");
    // Test.ensureEqual(Math2.binaryFindFirstGE(sorted, Double.NaN), 3, "r");

    // double sortedDuplicates[] = {1, 2,2,2,2,2, 3};
    Test.ensureEqual(Math2.binaryFindFirstGE(sortedDuplicates, 0.5), 0, "");
    Test.ensureEqual(Math2.binaryFindFirstGE(sortedDuplicates, 1.5), 1, "");
    Test.ensureEqual(Math2.binaryFindFirstGE(sortedDuplicates, 2.5), 6, "");
    Test.ensureEqual(Math2.binaryFindFirstGE(sortedDuplicates, 3.5), 7, "");

    // binaryFindFirstGAE lastParam: precision = 9
    String2.log("test binaryFindFirstGAE");
    // double sorted[] = {1, 7, 14, 21};
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, -1, 9), 0, "a");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 0.99999999999, 9), 0, "b");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 1, 9), 0, "c");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 1.00000000001, 9), 0, "d");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 6, 9), 1, "e");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 6.99999999999, 9), 1, "f");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 7, 9), 1, "g");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 7.00000000001, 9), 1, "h");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 8, 9), 2, "i");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 13.99999999999, 9), 2, "j");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 14, 9), 2, "k");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 14.00000000001, 9), 2, "l");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 15, 9), 3, "m");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 20.99999999999, 9), 3, "n");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 21, 9), 3, "o");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 21.00000000001, 9), 3, "p");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 22, 9), 4, "q");
    // Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, Double.NaN, 9), 3, "r");

    // double sortedDuplicates[] = {1, 2,2,2,2,2, 3}; lastParam: precision = 9
    Test.ensureEqual(Math2.binaryFindFirstGAE(sortedDuplicates, 0.5, 9), 0, "");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sortedDuplicates, 1.5, 9), 1, "");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sortedDuplicates, 2.5, 9), 6, "");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sortedDuplicates, 3.5, 9), 7, "");

    // binaryFindFirstGAE lastParam: precision = 5
    String2.log("test binaryFindFirstGAE");
    // double sorted[] = {1, 7, 14, 21};
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, -1, 5), 0, "a");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 0.9999999, 5), 0, "b");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 1, 5), 0, "c");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 1.0000001, 5), 0, "d");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 6, 5), 1, "e");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 6.9999999, 5), 1, "f");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 7, 5), 1, "g");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 7.0000001, 5), 1, "h");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 8, 5), 2, "i");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 13.9999999, 5), 2, "j");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 14, 5), 2, "k");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 14.0000001, 5), 2, "l");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 15, 5), 3, "m");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 20.9999999, 5), 3, "n");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 21, 5), 3, "o");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 21.0000001, 5), 3, "p");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, 22, 5), 4, "q");
    // Test.ensureEqual(Math2.binaryFindFirstGAE(sorted, Double.NaN, 5),3, "r");

    // double sortedDuplicates[] = {1, 2,2,2,2,2, 3}; lastParam: precision = 5
    Test.ensureEqual(Math2.binaryFindFirstGAE(sortedDuplicates, 0.5, 5), 0, "");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sortedDuplicates, 1.5, 5), 1, "");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sortedDuplicates, 2.5, 5), 6, "");
    Test.ensureEqual(Math2.binaryFindFirstGAE(sortedDuplicates, 3.5, 5), 7, "");

    // binaryFindClosest
    String2.log("test binaryFindClosest");
    // double sorted[] = {1, 7, 14, 21};
    Test.ensureEqual(Math2.binaryFindClosest(sorted, -1), 0, "a");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 0.9), 0, "b");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 1), 0, "c");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 1.1), 0, "d");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 6), 1, "e");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 6.9), 1, "f");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 7), 1, "g");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 7.1), 1, "h");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 8), 1, "i");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 13.9), 2, "j");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 14), 2, "k");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 14.1), 2, "l");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 15), 2, "m");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 20.9), 3, "n");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 21), 3, "o");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 21.1), 3, "p");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, 22), 3, "q");
    Test.ensureEqual(Math2.binaryFindClosest(sorted, Double.NaN), -1, "r");

    // double sortedDuplicates[] = {1, 2,2,2,2,2, 3};
    Test.ensureEqual(Math2.binaryFindClosest(sortedDuplicates, 0.9), 0, "");
    Test.ensureEqual(Math2.binaryFindClosest(sortedDuplicates, 1.1), 0, "");
    int i = Math2.binaryFindClosest(sortedDuplicates, 1.9);
    Test.ensureTrue(i >= 1 && i <= 5, "");
    i = Math2.binaryFindClosest(sortedDuplicates, 2.1);
    Test.ensureTrue(i >= 1 && i <= 5, "");
    Test.ensureEqual(Math2.binaryFindClosest(sortedDuplicates, 2.9), 6, "");
    Test.ensureEqual(Math2.binaryFindClosest(sortedDuplicates, 3.1), 6, "");

    String2.log("test reduceHashCode");
    Random random = new Random();
    for (i = 0; i < 100; i++) {
      int j = random.nextInt();
      String s = Math2.reduceHashCode(j);
      String error = "random=" + j + " reduced=" + s;
      int n = s.length();
      Test.ensureTrue(n <= 10, error);
      for (int k = 0; k < n; k++) Test.ensureTrue(String2.isDigit(s.charAt(k)), error);
    }
  }

  @org.junit.jupiter.api.Test
  void timeCurrentTimeMillis() {
    long time = System.currentTimeMillis();
    long sum = 0;
    for (int i = 0; i < 1000000; i++) sum += System.currentTimeMillis();
    String2.log(
        "TestUtil.timeCurrentTimeMillis  per call = "
            + ((System.currentTimeMillis() - time) / 1000000.0)
            + " ms (typical=7.8e-5ms)");
  }

  @org.junit.jupiter.api.Test
  void timeString2Log() {
    Math2.sleep(1000); // take a cleansing breath
    long time1 = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) String2.log("1234567" + i);
    String results =
        "TestUtil.timeString2Log  per shortString call = "
            + ((System.currentTimeMillis() - time1) / 1000.0)
            + " ms (typical=0.515ms)\n";

    long time2 = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) String2.log("12345678901234567" + i);
    results +=
        "TestUtil.timeString2Log  per mediumString call = "
            + ((System.currentTimeMillis() - time2) / 1000.0)
            + " ms (typical=3.219ms)\n";

    long time3 = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) String2.log("1234567" + i + "\n1234567" + i);
    results +=
        "TestUtil.timeString2Log  per 2 line short String call = "
            + ((System.currentTimeMillis() - time3) / 1000.0)
            + " ms (typical=5.172ms)\n";

    String2.log(
        results
            + "So it seems close to the number of characters printed with newline taking longer.\n"
            + "And there is a penalty for longer strings!\n"
            + "NOTE: this is for log to file and screen. I think screen is very slow and blocks.\n"
            + "Because file write times are very fast (see testWriteToFileSpeed()).");
  }

  /** Test String2LogOutputStream. */
  @org.junit.jupiter.api.Test
  void testString2LogOutputStream() throws Exception {
    String2.log("\n*** TestUtil.testString2LogOutputStream");

    String2LogOutputStream out = new String2LogOutputStream();
    try {
      out.write('a');
      out.write(new byte[] {(byte) 'b', (byte) 'c', (byte) 13, (byte) 10, (byte) 'a'});
      out.write(new byte[] {(byte) 'b', (byte) 'c', (byte) 13, (byte) 10, (byte) 'a'});
    } finally {
      out.close();
    }
    String2.log("It should have just logged \"bc\\r\\na\" twice.");
  }

  /** This runs the interactive tests of the methods in String2. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void interactiveTestString2() throws Exception {
    String2.log("\n*** TestUtil.interactiveTestString2");
    // getPasswordFromConsole
    String s =
        String2.getPasswordFromSystemIn( // String2.beep(1) +
            "test getPasswordFromSystemIn: Enter a string (it won't be echoed): ");
    String2.log("You entered: " + s);

    // getStringFromConsole
    s =
        String2.getStringFromSystemIn( // String2.beep(1) +
            "test getStringFromSystemIn: Enter a string (it will be echoed): ");
    String2.log("You entered: " + s);
  }

  /** Test the methods in String2. */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testString2() throws Throwable {
    String2.log("\n*** TestUtil.testString2()");
    String sar[];
    StringBuilder sb = new StringBuilder();
    double dar[];
    int n, iar[];
    String s, results, expected;
    long time;

    // md5Hex
    Test.ensureEqual(
        String2.md5Hex("This is a test01234.ÀÑ"), "b62023b8dffda52f4b1ea48f2cee739e", "");
    Test.ensureEqual(String2.md5Hex(""), "d41d8cd98f00b204e9800998ecf8427e", "");
    Test.ensureEqual(String2.md5Hex(null), null, "");

    // speed of parseInt success
    time = System.currentTimeMillis();
    long t1 = 0;
    for (int i = 0; i < 1000000; i++) t1 += String2.parseInt("1023456789");
    String2.log("time1 for 1000000 parseInt good=" + (System.currentTimeMillis() - time)); // 119

    // speed of parseInt fail
    time = System.currentTimeMillis();
    long t2 = 0;
    for (int i = 0; i < 1000000; i++) t2 += String2.parseInt("102345678901");
    String2.log("time2 for 1000000 parseInt  bad=" + (System.currentTimeMillis() - time)); // 1822
    // String2.pressEnterToContinue();

    // digestFile
    s = TestUtil.class.getResource("/data/simpleTest.nc").getPath();
    // md5 was verified by command line
    Test.ensureEqual(
        String2.fileDigest("MD5", s),
        "FDBCF902940C66250177A62DA165E467".toLowerCase(),
        ""); // 32 digits
    // I haven't verified the sha options, but they generate reasonable-looking
    // responses
    Test.ensureEqual(
        String2.fileDigest("SHA-1", s),
        "0a0a06a0687cd7eae77a686c3888bfaa8eb71905",
        ""); // 40 digits
    Test.ensureEqual(
        String2.fileDigest("SHA-256", s),
        "30ed6a6ce8417d47b077b7506c01e5dfc6d507fe50243dbb7cc7de10cc19da77",
        ""); // 64 digits

    // isNumber
    s = "0";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "91234567898765439";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "-12";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "0xA";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "0x1234567890ABCDEFabcdef";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "NAN";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "nan";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "-1.0e+3";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "-1.0e-33";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "-1.0E1";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "-1.e3";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "1e3";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "0.5";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "-0.5";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = ".5";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "-.5";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "9e0";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "0e0";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s);
    s = "9e05";
    Test.ensureEqual(String2.isNumber(s), true, "s=" + s); // valid?

    s = null;
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "..9";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-.";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-12 ";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-1z";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-12.";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "0x";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "0x12x";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "012";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "0k";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "0.";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "0e";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "NaN4";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-1.0f+5";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "99+8";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "1999-12";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s); // date-like
    s = ".9.";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "9e1.9";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "9e1e2";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "9e12a";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-1.0e";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-1.0e+";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-1.0e-";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-1.0ez";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-1.0e1+5";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "-1.0ee+5";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);
    s = "e2";
    Test.ensureEqual(String2.isNumber(s), false, "s=" + s);

    // isUrl
    Test.ensureEqual(String2.isUrl(null), false, "");
    Test.ensureEqual(String2.isUrl("http://"), false, "");
    Test.ensureEqual(String2.isUrl("http://a"), true, "");
    Test.ensureEqual(String2.isUrl("https://a.com"), true, "");
    Test.ensureEqual(String2.isUrl("rttp://a.com"), false, "");

    // isSomething
    Test.ensureEqual(String2.isSomething(null), false, "");
    Test.ensureEqual(String2.isSomething(""), false, "");
    Test.ensureEqual(String2.isSomething(" "), false, "");
    Test.ensureEqual(String2.isSomething("\n"), false, "");
    Test.ensureEqual(String2.isSomething(" \n"), false, "");
    Test.ensureEqual(String2.isSomething("ab"), true, "");

    Test.ensureEqual(String2.isSomething("."), true, "");
    Test.ensureEqual(String2.isSomething("?"), true, "");

    // isSomething2
    Test.ensureEqual(String2.isSomething2(null), false, "");
    Test.ensureEqual(String2.isSomething2(""), false, "");
    Test.ensureEqual(String2.isSomething2(" "), false, "");
    Test.ensureEqual(String2.isSomething2("\n"), false, "");
    Test.ensureEqual(String2.isSomething2(" \n"), false, "");
    Test.ensureEqual(String2.isSomething2("ab"), true, "");

    // things different in isSomething2 (case insensitive)
    Test.ensureEqual(String2.isSomething2("."), false, "");
    Test.ensureEqual(String2.isSomething2("-"), false, "");
    Test.ensureEqual(String2.isSomething2("?"), false, "");
    Test.ensureEqual(String2.isSomething2("n/a"), false, "");
    Test.ensureEqual(String2.isSomething2("N/A"), false, "");
    Test.ensureEqual(String2.isSomething2("na"), false, "");
    Test.ensureEqual(String2.isSomething2(" None "), false, "");
    Test.ensureEqual(String2.isSomething2("none."), false, "");
    Test.ensureEqual(String2.isSomething2("not Applicable"), false, "");
    Test.ensureEqual(String2.isSomething2("null"), false, "");
    Test.ensureEqual(String2.isSomething2("unKnown"), false, "");
    Test.ensureEqual(String2.isSomething2("unSpecified"), false, "");
    Test.ensureEqual(String2.isSomething2("..."), false, "");
    Test.ensureEqual(String2.isSomething2("???"), false, "");

    // removeLeading
    Test.ensureEqual(String2.removeLeading(null, ' '), null, "");
    Test.ensureEqual(String2.removeLeading("", ' '), "", "");
    Test.ensureEqual(String2.removeLeading("0", '0'), "", "");
    Test.ensureEqual(String2.removeLeading("00", '0'), "", "");
    Test.ensureEqual(String2.removeLeading("00a0", '0'), "a0", "");
    Test.ensureEqual(String2.removeLeading("b", 'a'), "b", "");

    // getAwsS3BucketName(String url) {
    Test.ensureEqual(
        String2.parseAwsS3Url("http://buc-k.et.s3.re-gion.amazonaws.com"), // http (which isn't
        // recommended) and no
        // trailing slash
        new String[] {"buc-k.et", "re-gion", ""},
        "");
    String s3 = "https://buc-k.et.s3.re-gion.amazonaws.com/ob_ject/n-a.me";
    Test.ensureEqual(
        String2.parseAwsS3Url(s3), new String[] {"buc-k.et", "re-gion", "ob_ject/n-a.me"}, "");

    s3 = "https://buc-k.et.s3.amazonaws.com/ob_ject/n-a.me"; // no region
    Test.ensureEqual(String2.parseAwsS3Url(s3), null, "");

    // noLongLines
    s =
        "asdf asdf asfd asdf (b)asdflakjf(a) abc flkjf aflkjj(b) sl;kj abcdefghijklmnopqrstuvwxyzabcdef(b) a asdlkj(b) f aflkja(b) fasl faslfkj(b) flkajf sflkj(b) adfsl;kj";
    s = String2.noLongLines(s, 25, "  ");
    Test.ensureEqual(
        s,
        "asdf asdf asfd asdf (b)\n"
            + "  asdflakjf(a) abc flkjf \n"
            + "  aflkjj(b) sl;kj \n"
            + "  abcdefghijklmnopqrstuvw\n"
            + "  xyzabcdef(b) a \n"
            + "  asdlkj(b) f aflkja(b) \n"
            + "  fasl faslfkj(b) flkajf \n"
            + "  sflkj(b) adfsl;kj",
        s);

    // extractAllCaptureGroupAsHashSet
    results =
        String2.toCSSVString(
            String2.extractAllCaptureGroupsAsHashSet(" a1 ;  a456 a23 bca23 ", "a(\\d+)", 1));
    expected = "1, 23, 456";
    Test.ensureEqual(results, expected, "");

    // extractAllCaptureGroupAsStringArray
    results =
        String2.toCSSVString(
            String2.extractAllCaptureGroupsAsStringArray(" a1 ;  a456 a23 bca23 ", "a(\\d+)", 1));
    expected = "1, 456, 23, 23";
    Test.ensureEqual(results, expected, "");

    // findWholeWord
    s = "aa)z (b) /cc/ d_z=ee";
    Test.ensureEqual(String2.findWholeWord(s, "aa"), 0, "");
    Test.ensureEqual(String2.findWholeWord(s, "b"), 6, "");
    Test.ensureEqual(String2.findWholeWord(s, "cc"), 10, "");
    Test.ensureEqual(String2.findWholeWord(s, "d"), -1, "");
    Test.ensureEqual(String2.findWholeWord(s, "ee"), 18, "");
    Test.ensureEqual(String2.findWholeWord(s, "AA"), -1, "");
    Test.ensureEqual(String2.findWholeWord(s, ""), -1, "");
    Test.ensureEqual(String2.findWholeWord(s, null), -1, "");
    Test.ensureEqual(String2.findWholeWord("", "a"), -1, "");
    Test.ensureEqual(String2.findWholeWord(null, "a"), -1, "");
    Test.ensureEqual(String2.findWholeWord("a", "a"), 0, "");
    Test.ensureEqual(String2.findWholeWord("a ", "a"), 0, "");
    Test.ensureEqual(String2.findWholeWord(" a", "a"), 1, "");

    // removeValues(Map map, Set set)
    Map map = new HashMap();
    map.put("0", "00");
    map.put("1", "11");
    map.put("2", "22");
    Set set = new HashSet();
    set.add("11");
    set.add("zz");
    String2.removeValues(map, set);
    Test.ensureEqual(
        String2.toString(map), "0 = 00\n2 = 22\n", "results=\n" + String2.toString(map));
    Test.ensureEqual(String2.toCSSVString(set), "zz", "");

    // noLongLinesAtSpace
    s = "a abcdefghijklmnopqrstuvwxyzabcdef(b) a asdlasdabc fakjasdfg(b)";
    s = String2.noLongLinesAtSpace(s, 15, "  ");
    Test.ensureEqual(
        s, "a abcdefghijklmnopqrstuvwxyzabcdef(b)\n" + "  a asdlasdabc\n" + "  fakjasdfg(b)", s);

    s = "a asdf sdf jk;lk qwer abcdefghijklmnopqrstuvwxyzabcdef(b)";
    s = String2.noLongLinesAtSpace(s, 15, "  ");
    Test.ensureEqual(
        s, "a asdf sdf\n" + "  jk;lk qwer\n" + "  abcdefghijklmnopqrstuvwxyzabcdef(b)", s);

    s = "a asdf sdf jk;lk qwer abcdefghijklmnopqrstuvwxyzabcdef(b) ";
    s = String2.noLongLinesAtSpace(s, 15, "  ");
    Test.ensureEqual(
        s, "a asdf sdf\n" + "  jk;lk qwer\n" + "  abcdefghijklmnopqrstuvwxyzabcdef(b)\n" + "  ", s);

    s = "a asdf sdf jk;lk qwer abcdefghijklmnopqrstuvwxyzabcdef(b) a";
    s = String2.noLongLinesAtSpace(s, 15, "  ");
    Test.ensureEqual(
        s,
        "a asdf sdf\n" + "  jk;lk qwer\n" + "  abcdefghijklmnopqrstuvwxyzabcdef(b)\n" + "  a",
        s);

    // noLongerThan
    s = "123456789";
    Test.ensureEqual(String2.noLongerThan(s, 10), "123456789", "");
    Test.ensureEqual(String2.noLongerThan(s, 9), "123456789", "");
    Test.ensureEqual(String2.noLongerThan(s, 8), "12345678", "");
    Test.ensureEqual(String2.noLongerThan(s, 1), "1", "");
    Test.ensureEqual(String2.noLongerThan(s, 0), "", "");
    Test.ensureEqual(String2.noLongerThan(s, -1), "", "");

    // noLongerThanDots
    Test.ensureEqual(String2.noLongerThanDots(s, 10), "123456789", "");
    Test.ensureEqual(String2.noLongerThanDots(s, 9), "123456789", "");
    Test.ensureEqual(String2.noLongerThanDots(s, 8), "12345...", "");
    Test.ensureEqual(String2.noLongerThanDots(s, 4), "1...", "");
    Test.ensureEqual(String2.noLongerThanDots(s, 3), "...", "");
    Test.ensureEqual(String2.noLongerThanDots(s, 1), "...", "");
    Test.ensureEqual(String2.noLongerThanDots(s, 0), "...", "");
    Test.ensureEqual(String2.noLongerThanDots(s, -1), "...", "");

    // test for infinite loop
    String2.log("test for infinite loop in String2.noLongLinesAtSpace");
    sb = new StringBuilder();
    for (int test = 0; test < 20000; test++) { // once I tested 1000000
      if (test % 10000 == 0) String2.log("" + test);
      sb.setLength(0);
      for (int word = 0; word < 10; word++)
        // if randomLength=0, there will be 2 spaces in a row
        sb.append(" " + String2.makeString((char) (word + 65), Math2.random(12)));
      sb = String2.noLongLinesAtSpace(sb, 15, "  ");
      if (test < 10) String2.log(sb.toString());
      // if (test == 10) System.exit(0);
    }

    Math2.incgc("EDDTableFromNcFiles (between tests)", 3000);

    // binaryFindLastLE
    String tsar[] = {"abc", "bcd", "bcj"};
    String dupSar[] = {"ac", "bc", "bc", "bc", "bc", "bc", "cc"};
    Test.ensureEqual(String2.binaryFindLastLE(tsar, "aa"), -1, "");
    Test.ensureEqual(String2.binaryFindLastLE(tsar, "abc"), 0, "");
    Test.ensureEqual(String2.binaryFindLastLE(tsar, "abd"), 0, "");
    Test.ensureEqual(String2.binaryFindLastLE(tsar, "bcd"), 1, "");
    Test.ensureEqual(String2.binaryFindLastLE(tsar, "bce"), 1, "");
    Test.ensureEqual(String2.binaryFindLastLE(tsar, "bcj"), 2, "");
    Test.ensureEqual(String2.binaryFindLastLE(tsar, "c"), 2, "");

    // String dupSar[] = {"ac", "bc", "bc", "bc", "bc", "bc", "cc"};
    Test.ensureEqual(String2.binaryFindLastLE(dupSar, "aa"), -1, "");
    Test.ensureEqual(String2.binaryFindLastLE(dupSar, "ba"), 0, "");
    Test.ensureEqual(String2.binaryFindLastLE(dupSar, "bc"), 5, "");
    Test.ensureEqual(String2.binaryFindLastLE(dupSar, "ca"), 5, "");
    Test.ensureEqual(String2.binaryFindLastLE(dupSar, "da"), 6, "");

    Test.ensureEqual(String2.binaryFindFirstGE(tsar, "aa"), 0, "");
    Test.ensureEqual(String2.binaryFindFirstGE(tsar, "abc"), 0, "");
    Test.ensureEqual(String2.binaryFindFirstGE(tsar, "abd"), 1, "");
    Test.ensureEqual(String2.binaryFindFirstGE(tsar, "bcd"), 1, "");
    Test.ensureEqual(String2.binaryFindFirstGE(tsar, "bce"), 2, "");
    Test.ensureEqual(String2.binaryFindFirstGE(tsar, "bcj"), 2, "");
    Test.ensureEqual(String2.binaryFindFirstGE(tsar, "c"), 3, "");

    // String dupSar[] = {"ac", "bc", "bc", "bc", "bc", "bc", "cc"};
    Test.ensureEqual(String2.binaryFindFirstGE(dupSar, "aa"), 0, "");
    Test.ensureEqual(String2.binaryFindFirstGE(dupSar, "ba"), 1, "");
    Test.ensureEqual(String2.binaryFindFirstGE(dupSar, "bc"), 1, "");
    Test.ensureEqual(String2.binaryFindFirstGE(dupSar, "ca"), 6, "");
    Test.ensureEqual(String2.binaryFindFirstGE(dupSar, "da"), 7, "");

    Test.ensureEqual(String2.binaryFindClosest(tsar, "aa"), 0, "");
    Test.ensureEqual(String2.binaryFindClosest(tsar, "abc"), 0, "");
    Test.ensureEqual(String2.binaryFindClosest(tsar, "abd"), 0, "");
    Test.ensureEqual(String2.binaryFindClosest(tsar, "bcd"), 1, "");
    Test.ensureEqual(String2.binaryFindClosest(tsar, "bce"), 1, "");
    Test.ensureEqual(String2.binaryFindClosest(tsar, "bcj"), 2, "");
    Test.ensureEqual(String2.binaryFindClosest(tsar, "c"), 2, "");

    // String dupSar[] = {"ac", "bc", "bc", "bc", "bc", "bc", "cc"};
    Test.ensureEqual(String2.binaryFindClosest(dupSar, "aa"), 0, "");
    Test.ensureEqual(String2.binaryFindClosest(dupSar, "ba"), 1, "");
    int i = String2.binaryFindClosest(dupSar, "bb");
    Test.ensureTrue(i >= 1 && i <= 5, "");
    i = String2.binaryFindClosest(dupSar, "bd");
    Test.ensureTrue(i >= 1 && i <= 5, "");
    Test.ensureEqual(String2.binaryFindClosest(dupSar, "ca"), 6, "");
    Test.ensureEqual(String2.binaryFindClosest(dupSar, "da"), 6, "");

    // looselyContains
    Test.ensureTrue(String2.looselyContains("(a b,C'D&5+e", "A&b[c]d 5%E"), "");
    Test.ensureTrue(String2.looselyContains("(a b,C'D&e", "dE!"), "");
    Test.ensureTrue(!String2.looselyContains("(a b,C'D&e[]", "E!4-"), "");
    Test.ensureTrue(!String2.looselyContains("(a b,C'D&e", "[]"), "");
    Test.ensureTrue(!String2.looselyContains("()", "e4"), "");
    Test.ensureTrue(!String2.looselyContains("(a b,C'D&e", null), "");
    Test.ensureTrue(!String2.looselyContains(null, "e4"), "");

    // min
    Test.ensureEqual(String2.min("aab", "aac"), "aab", "");
    Test.ensureEqual(String2.min("aab", "aaba"), "aab", "");
    Test.ensureEqual(String2.min(null, "aaba"), null, "");
    Test.ensureEqual(String2.min("aab", null), null, "");

    // max
    Test.ensureEqual(String2.max("aab", "aac"), "aac", "");
    Test.ensureEqual(String2.max("aab", "aaba"), "aaba", "");
    Test.ensureEqual(String2.max(null, "aaba"), "aaba", "");
    Test.ensureEqual(String2.max("aab", null), "aab", "");

    // makeString
    String2.log("test makeString");
    Test.ensureEqual(String2.makeString(' ', 5), "     ", "a");
    Test.ensureEqual(String2.makeString('+', 1), "+", "b");
    Test.ensureEqual(String2.makeString('0', 0), "", "c");
    Test.ensureEqual(String2.makeString('-', -5), "", "d");

    // right
    String2.log("test right");
    Test.ensureEqual(String2.right("aaa", 4), " aaa", "a");
    Test.ensureEqual(String2.right("aaa", 5), "  aaa", "b");
    Test.ensureEqual(String2.right("bb", 0), "bb", "c");
    Test.ensureEqual(String2.right("", -5), "", "d");
    Test.ensureEqual(String2.right("aaa", 3), "aaa", "e");

    // left
    String2.log("test left");
    Test.ensureEqual(String2.left("aaa", 4), "aaa ", "a");
    Test.ensureEqual(String2.left("aaa", 5), "aaa  ", "b");
    Test.ensureEqual(String2.left("bb", 0), "bb", "c");
    Test.ensureEqual(String2.left("", -5), "", "d");
    Test.ensureEqual(String2.left("aaa", 3), "aaa", "e");

    // center
    String2.log("test center");
    Test.ensureEqual(String2.center("aaa", 4), "aaa ", "a");
    Test.ensureEqual(String2.center("aaa", 5), " aaa ", "b");
    Test.ensureEqual(String2.center("bb", 0), "bb", "c");
    Test.ensureEqual(String2.center("", -5), "", "d");
    Test.ensureEqual(String2.center("aaa", 3), "aaa", "e");

    // toJson
    String2.log("test toJson");
    String a = "\\ \f\n\r\t\" z\u0000\uffff\u00ff";
    String b = "\"\\\\ \\f\\n\\r\\t\\\" z\\u0000\\uffff\\u00ff\"";
    Test.ensureEqual(String2.toJson(a), b, "");
    Test.ensureEqual(String2.fromJson(b), a, "");
    Test.ensureEqual(String2.fromJson("\\?\\'"), "?'", "");
    Test.ensureEqual(String2.fromJson("\\a\\b\\v"), "", "");
    Test.ensureEqual(String2.fromJson("\\101"), "A", ""); // octal
    Test.ensureEqual(String2.fromJson("\\x4f"), "O", ""); // 'OH'
    Test.ensureEqual(String2.fromJson("\\x4F"), "O", "");
    Test.ensureEqual(String2.fromJson("\\u004f"), "O", "");
    Test.ensureEqual(String2.fromJson("\\u004F"), "O", "");
    String2.log(
        "Intentional errors:"); // source just passes through, assume source didn't want json-like
    Test.ensureEqual(String2.fromJson("\\z"), "\\z", ""); // not a standard \\char
    Test.ensureEqual(String2.fromJson("\\108"), "\\108", ""); // not quite octal
    Test.ensureEqual(String2.fromJson("\\x6m"), "\\x6m", ""); // not quite hex
    Test.ensureEqual(String2.fromJson("\\u006m"), "\\u006m", ""); // not quite unicode
    Test.ensureEqual(String2.fromJson("a\\"), "a\\", ""); // missing subsequent char
    Test.ensureEqual(String2.fromJson("\\1"), "\\1", ""); // not octal
    Test.ensureEqual(String2.fromJson("\\x"), "\\x", ""); // not hex
    Test.ensureEqual(String2.fromJson("\\u"), "\\u", ""); // not unicode

    Test.ensureEqual(String2.fromJson("null"), "null", "");
    Test.ensureEqual(String2.fromJson(null), null, "");
    Test.ensureEqual(String2.fromJsonNotNull("null"), "null", "");
    Test.ensureEqual(String2.fromJsonNotNull(null), "", "");

    // toIso88591String
    String2.log("test toIso88591String");
    s = String2.annotatedString(String2.toIso88591String("\u0000\n\r\t\f aA\u0091\u00fc\u20ac"));
    Test.ensureEqual(s, "[0][10]\n[13][9][12] aA?[252]?[end]", "results=" + s);

    // fromNccsvChar
    String2.log("test fromNccsvChar");
    Test.ensureEqual("" + String2.fromNccsvChar("a"), "a", "");
    Test.ensureEqual("" + String2.fromNccsvChar(" "), " ", "");
    Test.ensureEqual("" + String2.fromNccsvChar("' '"), " ", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\" \""), " ", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\"' '\""), " ", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\"'\"\"'\""), "\"", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\\t"), "\t", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\\n"), "\n", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\u20AC"), "\u20ac", "");
    Test.ensureEqual("" + String2.fromNccsvChar("'\u20AC'"), "\u20ac", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\"\u20AC\""), "\u20ac", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\"'\u20AC'\""), "\u20ac", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\"\""), "?", "");
    Test.ensureEqual("" + String2.fromNccsvChar("\'\'"), "?", "");
    Test.ensureEqual("" + String2.fromNccsvChar(""), "?", "");
    Test.ensureEqual("" + String2.fromNccsvChar(null), "?", "");

    // toNccsvString etc.: see PrimitiveArray.testToNccsv();

    // PERSON_REGEX
    String2.log("test PERSON_REGEX");
    Test.ensureTrue("Dr. Kenneth S. Jones, something".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("Dr. Kenneth S. Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("Dr. K S. Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("Ken S. Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("Ken S Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("Ken Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("K Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("Ke Jo".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("Mary McKibbon".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue("Mary MacKibbon".matches(String2.ACDD_PERSON_REGEX2), "");

    Test.ensureTrue(!"Dr. Kenneth S. Jones,".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue(!"Kenneth R S Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue(!"D. K S. Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue(!"K S. CJones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue(!"K S.. Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue(!"KS Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue(!"Ke. Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue(!"Jones".matches(String2.ACDD_PERSON_REGEX2), "");
    Test.ensureTrue(!"Ken MdKelty".matches(String2.ACDD_PERSON_REGEX2), "");

    // annotatedString
    String2.log("test annotatedString");
    Test.ensureEqual(String2.annotatedString("\ta\nb"), "[9]a[10]\nb[end]", "a");

    // extractRegex
    String2.log("test extractRegex");
    String regex = "<strong>[-]?[0-9]+\\.[0-9]+ [SN]\\s+[-]?[0-9]+\\.[0-9]+ [WE] \\(";
    Test.ensureEqual(
        String2.extractRegex("JUNK<strong>9.9 S 105.2 W (JUNK", regex, 0),
        "<strong>9.9 S 105.2 W (",
        "a");
    Test.ensureEqual(
        String2.extractRegex(
            "JUNK<strong>9.9 Sz 105.2 W (JUNK", // extra z in middle
            regex,
            0),
        null,
        "b");

    // extractAllRegexes
    String2.log("test extractAllRegexes");
    regex = "url=\".+?\"";
    sar = String2.extractAllRegexes("junk url=\"first\"  curl=\"second\" junk", regex);
    Test.ensureEqual(
        String2.toCSSVString(sar),
        "\"url=\\\"first\\\"\", \"url=\\\"second\\\"\"",
        "sar=" + String2.toCSSVString(sar));

    // extractCaptureGroup
    String2.log("test extractRegex");
    Test.ensureEqual(
        String2.extractCaptureGroup("bc&a&", "b(\\w+)&.*", 1), "c", "a"); // match all of s
    Test.ensureEqual(
        String2.extractCaptureGroup("abc&a&", "b(\\w+)&", 1), "c", "a"); // match part of s
    Test.ensureEqual(String2.extractCaptureGroup("bedad", "b(.+?)d.*", 1), "e", "b"); // reluctant

    // utf8 conversions
    String os = " s\\\n\tÃ\u20ac ";
    byte bar[] = String2.stringToUtf8Bytes(os);
    Test.ensureEqual(
        String2.toCSSVString(bar), "32, 115, 92, 10, 9, -61, -125, -30, -126, -84, 32", "");
    s = String2.utf8BytesToString(bar);
    Test.ensureEqual(s, os, "s=" + String2.annotatedString(s));

    time = System.currentTimeMillis();
    for (i = 0; i < 1000000; i++) {
      bar = String2.stringToUtf8Bytes(os);
    }
    time = System.currentTimeMillis() - time;
    // TODO handle time based performance tests better
    // Test.ensureTrue(time <= 270,
    //         "Too slow!  Time for 1000000 StringToUtf8Bytes=" + time + "ms (usual = 203)");

    time = System.currentTimeMillis();
    for (i = 0; i < 1000000; i++) {
      s = String2.utf8BytesToString(bar);
    }
    time = System.currentTimeMillis() - time;
    // TODO handle time based performance tests better
    // Test.ensureTrue(time <= 270,
    //         "Too slow!  Time for 1000000 utf8BytesToString=" + time + "ms (usual = 203)");

    s = String2.stringToUtf8String(os);
    Test.ensureEqual(
        String2.annotatedString(s),
        " s\\[10]\n" + "[9][195][131][226][130][172] [end]",
        "s=" + String2.annotatedString(s));
    s = String2.utf8StringToString(s);
    Test.ensureEqual(s, os, "s=" + String2.annotatedString(s));

    // compareTo times
    time = System.currentTimeMillis();
    a = "aaaaabc";
    b = "aaaaaa";
    int sum = 0;
    for (i = 0; i < 1000000; i++) {
      sum += b.compareTo(a);
    }
    Test.ensureEqual(sum, -1000000, "");
    time = System.currentTimeMillis() - time;
    // TODO handle time based performance tests better
    // Test.ensureTrue(time <= 70,
    //        "Too slow!  Time for 1000000 String.compareTo=" + time + "ms (usual = 46-63)");

    // compareTo times
    time = System.currentTimeMillis();
    StringHolder sha = new StringHolder("aaaaabc");
    StringHolder shb = new StringHolder("aaaaaa");
    sum = 0;
    for (i = 0; i < 1000000; i++) {
      sum += shb.compareTo(sha);
    }
    Test.ensureEqual(sum, -1000000, "");
    time = System.currentTimeMillis() - time;
    // TODO handle time based performance tests better
    // Test.ensureTrue(time <= 80,
    //        "time for 1000000 StringHolder.compareTo=" + time + "ms (usual = 46-63)");

    // compareTo times
    time = System.currentTimeMillis();
    sum = 0;
    for (i = 0; i < 1000000; i++) {
      sum += shb.toString().compareTo(sha.toString());
    }
    Test.ensureEqual(sum, -1000000, "");
    time = System.currentTimeMillis() - time;
    // TODO handle time based performance tests better
    // Test.ensureTrue(time <= 200,
    //        "time for 1000000 StringHolder.toString().compareTo=" + time + "ms (usual = 154)");

    // indexOfIgnoreCase(s)
    String2.log("test indexOfIgnoreCase(s)");
    s = "ABCDEFGHIJK";
    Test.ensureEqual(String2.indexOfIgnoreCase(s, "AB", 0), 0, "");
    Test.ensureEqual(String2.indexOfIgnoreCase(s, "AB", 1), -1, "");
    Test.ensureEqual(String2.indexOfIgnoreCase(s, "Z", 0), -1, "");
    Test.ensureEqual(String2.indexOfIgnoreCase(s, "ab", 0), 0, "");
    Test.ensureEqual(String2.indexOfIgnoreCase(s, "JKL", 0), -1, "");
    Test.ensureEqual(String2.indexOfIgnoreCase(s, "jk", 0), 9, "");

    // indexOf(StringBuilder)
    String2.log("test indexOf(StringBuilder)");
    StringBuilder abcd = new StringBuilder("abcd");
    Test.ensureEqual(String2.indexOf(abcd, "a", 0), 0, "a");
    Test.ensureEqual(String2.indexOf(abcd, "a", 1), -1, "b");
    Test.ensureEqual(String2.indexOf(abcd, "a", -1), 0, "c");
    Test.ensureEqual(String2.indexOf(abcd, "cd", 0), 2, "d");
    Test.ensureEqual(String2.indexOf(abcd, "ce", 0), -1, "e");
    Test.ensureEqual(String2.indexOf(abcd, "d", 0), 3, "f");
    Test.ensureEqual(String2.indexOf(abcd, "de", 0), -1, "g");

    // indexOf(int[])
    String2.log("test indexOf(int[])");
    iar = new int[] {6, 5, 4};
    Test.ensureEqual(String2.indexOf(iar, 6, 0), 0, "a");
    Test.ensureEqual(String2.indexOf(iar, 5, 0), 1, "b");
    Test.ensureEqual(String2.indexOf(iar, 4, 0), 2, "c");
    Test.ensureEqual(String2.indexOf(iar, 6, 1), -1, "d");
    Test.ensureEqual(String2.indexOf(iar, 5, 1), 1, "e");
    Test.ensureEqual(String2.indexOf(iar, 6, -1), 0, "f");
    Test.ensureEqual(String2.indexOf(iar, 4, 3), -1, "g");

    // indexOf(char[])
    String2.log("test indexOf(char[])");
    char[] car = new char[] {'c', 'b', 'a'};
    Test.ensureEqual(String2.indexOf(car, 'c', 0), 0, "a");
    Test.ensureEqual(String2.indexOf(car, 'b', 0), 1, "b");
    Test.ensureEqual(String2.indexOf(car, 'a', 0), 2, "c");
    Test.ensureEqual(String2.indexOf(car, 'c', 1), -1, "d");
    Test.ensureEqual(String2.indexOf(car, 'b', 1), 1, "e");
    Test.ensureEqual(String2.indexOf(car, 'c', -1), 0, "f");
    Test.ensureEqual(String2.indexOf(car, 'a', 3), -1, "g");

    // indexOf(double[])
    String2.log("test indexOf(double[])");
    dar = new double[] {6, 5.1, 4};
    Test.ensureEqual(String2.indexOf(dar, 6, 0), 0, "a");
    Test.ensureEqual(String2.indexOf(dar, 5.1, 0), 1, "b");
    Test.ensureEqual(String2.indexOf(dar, 4, 0), 2, "c");
    Test.ensureEqual(String2.indexOf(dar, 6, 1), -1, "d");
    Test.ensureEqual(String2.indexOf(dar, 5.1, 1), 1, "e");
    Test.ensureEqual(String2.indexOf(dar, 6, -1), 0, "f");
    Test.ensureEqual(String2.indexOf(dar, 4, 3), -1, "g");

    // indexOfChar(s, char[])
    String2.log("test indexOfChar(s, char[])");
    car = new char[] {'c', 'b', 'a'};
    Test.ensureEqual(String2.indexOfChar("czz", car, 0), 0, "a");
    Test.ensureEqual(String2.indexOfChar("zcz", car, 0), 1, "b");
    Test.ensureEqual(String2.indexOfChar("zzc", car, 0), 2, "c");
    Test.ensureEqual(String2.indexOfChar("azz", car, 0), 0, "a");
    Test.ensureEqual(String2.indexOfChar("zaz", car, 0), 1, "b");
    Test.ensureEqual(String2.indexOfChar("zza", car, 0), 2, "c");

    Test.ensureEqual(String2.indexOfChar("czz", car, 1), -1, "d");
    Test.ensureEqual(String2.indexOfChar("aaz", car, 1), 1, "e");
    Test.ensureEqual(String2.indexOfChar("abc", car, -1), 0, "f");
    Test.ensureEqual(String2.indexOfChar("abc", car, 3), -1, "g");

    // whichWord
    tsar = new String[] {"abc", "bcd", "bcj"};
    Test.ensureEqual(String2.whichWord("qbcjd", tsar), 2, "");
    tsar = new String[] {"abc", "bcd", "bcr"};
    Test.ensureEqual(String2.whichWord("qbcjd", tsar), -1, "");

    // writeToFile
    String2.log("test writeToFile");
    String fileName = "TestString2.tmp";
    String contents =
        "This is a file\n"
            + "with a few lines.\n"
            + // used below
            Calendar2.newGCalendarLocal()
            + "\n"; // unique content
    Test.ensureEqual(File2.writeToFileUtf8(fileName, contents), "", "a");

    // readFromFile
    String2.log("test readFromFile");
    String result[] = File2.readFromFileUtf8(fileName);
    Test.ensureEqual(result[0], "", "a");
    Test.ensureEqual(result[1], contents, "b");

    // appendFile
    String2.log("test appendFile");
    String contents2 = "This was appended.\n";
    Test.ensureEqual(File2.appendFileUtf8(fileName, contents2), "", "a");
    result = File2.readFromFileUtf8(fileName);
    Test.ensureEqual(result[0], "", "b");
    Test.ensureEqual(result[1], contents + contents2, "c");
    File2.delete(fileName);

    // speed of writeToFile
    // make a big StringBuilder
    // see temp changes in String2.lowWriteToFile for bufferSize and write in 39
    // byte chunks
    // I tried defaultSize, 4096, 16384, 65536: all gave 234ms +/-1
    // String2.log("test writeToFile speed");
    // StringBuilder sb2 = new StringBuilder(10000000);
    // for (int i = 0; i < 10000000; i++)
    // sb.append("a");
    // String s9 = sb2.toString();
    // File2.writeToFile88591(fileName, s9); //warm up
    // File2.writeToFile88591(fileName, s9);
    // long tTime = System.currentTimeMillis();
    // File2.writeToFile88591(fileName, s9);
    // String2.log("time=" + (System.currentTimeMillis() - tTime));
    // sb2 = null;
    // s9 = null;
    // Math2.sleep(5000);

    // test isLetter
    String2.log("test isLetter");
    Test.ensureEqual(String2.isLetter('1'), false, "a");
    Test.ensureEqual(String2.isLetter('b'), true, "b");
    Test.ensureEqual(String2.isLetter('-'), false, "c");
    Test.ensureEqual(String2.isLetter('$'), false, "d");
    Test.ensureEqual(String2.isLetter('_'), false, "e");
    Test.ensureEqual(String2.isLetter(' '), false, "f");
    Test.ensureEqual(String2.isLetter('\t'), false, "g");

    // test isIDFirstLetter
    String2.log("test isIDFirstLetter");
    Test.ensureEqual(String2.isIDFirstLetter('1'), false, "a");
    Test.ensureEqual(String2.isIDFirstLetter('b'), true, "b");
    Test.ensureEqual(String2.isIDFirstLetter('-'), false, "c");
    Test.ensureEqual(String2.isIDFirstLetter('$'), true, "d");
    Test.ensureEqual(String2.isIDFirstLetter('_'), true, "e");
    Test.ensureEqual(String2.isIDFirstLetter(' '), false, "f");
    Test.ensureEqual(String2.isIDFirstLetter('\t'), false, "g");

    // test isHexDigit
    String2.log("test isHexDigit");
    Test.ensureEqual(String2.isHexDigit('1'), true, "a");
    Test.ensureEqual(String2.isHexDigit('b'), true, "b");
    Test.ensureEqual(String2.isHexDigit('-'), false, "c");
    Test.ensureEqual(String2.isHexDigit('$'), false, "d");
    Test.ensureEqual(String2.isHexDigit('_'), false, "e");
    Test.ensureEqual(String2.isHexDigit(' '), false, "f");
    Test.ensureEqual(String2.isHexDigit('\t'), false, "g");

    // test isDigit
    String2.log("test isDigit");
    Test.ensureEqual(String2.isDigit('1'), true, "a");
    Test.ensureEqual(String2.isDigit('b'), false, "b");
    Test.ensureEqual(String2.isDigit('-'), false, "c");
    Test.ensureEqual(String2.isDigit('$'), false, "d");
    Test.ensureEqual(String2.isDigit('_'), false, "e");
    Test.ensureEqual(String2.isDigit(' '), false, "f");
    Test.ensureEqual(String2.isDigit('\t'), false, "g");

    // test isDigitLetter
    String2.log("test isDigitLetter");
    Test.ensureEqual(String2.isDigitLetter('1'), true, "a");
    Test.ensureEqual(String2.isDigitLetter('b'), true, "b");
    Test.ensureEqual(String2.isDigitLetter('-'), false, "c");
    Test.ensureEqual(String2.isDigitLetter('$'), false, "d");
    Test.ensureEqual(String2.isDigitLetter('_'), false, "e");
    Test.ensureEqual(String2.isDigitLetter(' '), false, "f");
    Test.ensureEqual(String2.isDigitLetter('\t'), false, "g");

    // test isWhite
    String2.log("test isWhite");
    Test.ensureEqual(String2.isWhite('1'), false, "a");
    Test.ensureEqual(String2.isWhite('b'), false, "b");
    Test.ensureEqual(String2.isWhite('-'), false, "c");
    Test.ensureEqual(String2.isWhite('$'), false, "d");
    Test.ensureEqual(String2.isWhite('_'), false, "e");
    Test.ensureEqual(String2.isWhite(' '), true, "f");
    Test.ensureEqual(String2.isWhite('\t'), true, "g");

    // test isPrintable
    String2.log("test isPrintable");
    Test.ensureEqual(String2.isPrintable('1'), true, "a");
    Test.ensureEqual(String2.isPrintable('b'), true, "b");
    Test.ensureEqual(String2.isPrintable('-'), true, "c");
    Test.ensureEqual(String2.isPrintable('$'), true, "d");
    Test.ensureEqual(String2.isPrintable('_'), true, "e");
    Test.ensureEqual(String2.isPrintable(' '), true, "f");
    Test.ensureEqual(String2.isPrintable('\t'), false, "g"); // it is treated separately

    // justPrintable
    String2.log("test justPrintable");
    Test.ensureEqual(String2.justPrintable("\ta\bb\n"), "ab", "a");

    // modifyToBeASCII
    String2.log("test modifyToBeASCII");
    Test.ensureEqual(String2.modifyToBeASCII("\ta\bA\n1-_.ÀÉÐÝ¡ÿ"), "\ta\bA\n1-_.AEDY!y", "a");

    // modifyToBeFileNameSafe
    String2.log("test modifyToBeFileNameSafe");
    String hard = "\ta\bA\n1- _._ !@#$%^&*()+={}[];:'\"<>,/?ÀÉÐÝ¡ÿ";
    expected = "_a_A_1-_._AEDY_y";
    Test.ensureTrue(!String2.isFileNameSafe(hard), "a");
    Test.ensureTrue(String2.isFileNameSafe(expected), "b");
    Test.ensureEqual(String2.modifyToBeFileNameSafe(hard), expected, "c");
    Test.ensureEqual(String2.modifyToBeFileNameSafe(null), "_null", "");
    Test.ensureEqual(String2.modifyToBeFileNameSafe(""), "_", "");
    Test.ensureEqual(String2.modifyToBeFileNameSafe("_"), "_", "");

    // isVariableNameSafe (doesn't allow >255/unicode)
    String2.log("test isVariableNameSafe");
    Test.ensureEqual(String2.isVariableNameSafe(null), false, "");
    Test.ensureEqual(String2.isVariableNameSafe(""), false, "");
    Test.ensureEqual(String2.isVariableNameSafe("_"), true, "");
    Test.ensureEqual(String2.isVariableNameSafe("a"), true, "");
    Test.ensureEqual(String2.isVariableNameSafe("À"), true, "");
    Test.ensureEqual(String2.isVariableNameSafe("."), false, "");
    Test.ensureEqual(String2.isVariableNameSafe("\u0100"), false, ""); // A macron
    Test.ensureEqual(String2.isVariableNameSafe("__"), true, "");
    Test.ensureEqual(String2.isVariableNameSafe("aa"), true, "");
    Test.ensureEqual(String2.isVariableNameSafe("ÀÀ"), true, "");
    Test.ensureEqual(String2.isVariableNameSafe("_."), false, "");
    Test.ensureEqual(String2.isVariableNameSafe("a."), false, "");
    Test.ensureEqual(String2.isVariableNameSafe("À."), false, "");

    // isJsonpNameSafe (doesn't allow >255/unicode)
    String2.log("test isJsonpNameSafe");
    Test.ensureEqual(String2.isJsonpNameSafe(null), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe(""), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("_"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe("a"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe("À"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe(" "), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("."), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("9"), false, "");
    Test.ensureEqual(
        String2.isJsonpNameSafe("\u0100"), false, ""); // A macron outside fo 8859 charset
    Test.ensureEqual(String2.isJsonpNameSafe("__"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe("aa"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe("a9"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe("ÀÀ"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe("À "), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("_."), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("a."), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("À."), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("a. "), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("a.."), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("a.9"), false, "");
    Test.ensureEqual(String2.isJsonpNameSafe("_._"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe("À.À"), true, "");
    Test.ensureEqual(String2.isJsonpNameSafe("À.À._a9"), true, "");

    // modifyToBeVariableNameSafe
    String2.log("test modifyToBeVariableNameSafe");
    hard = "\ta\bA\n1- _._ !@#$%^&*()+={}[];:'\"<>,/?ÀÉÐÝ¡ÿ";
    expected = "_a_A_1_ÀÉÐÝ_ÿ";
    Test.ensureEqual(String2.modifyToBeVariableNameSafe(hard), expected, "c");
    Test.ensureEqual(String2.modifyToBeVariableNameSafe(null), "_null", "");
    Test.ensureEqual(String2.modifyToBeVariableNameSafe(""), "_", "");
    Test.ensureEqual(String2.modifyToBeVariableNameSafe("_"), "_", "");
    Test.ensureEqual(String2.modifyToBeVariableNameSafe(","), "_", "");
    Test.ensureEqual(String2.modifyToBeVariableNameSafe("a"), "a", "");
    Test.ensureEqual(String2.modifyToBeVariableNameSafe("2"), "_2", "");
    Test.ensureEqual(String2.modifyToBeVariableNameSafe("a_"), "a_", "");
    Test.ensureEqual(String2.modifyToBeVariableNameSafe("_a"), "_a", "");

    // differentLine
    String2.log("test differentLine");
    s = String2.differentLine("a\nbb\nccc", "a\nbb\nccc");
    Test.ensureEqual(s, "", "s=" + s);
    s = String2.differentLine("\na\nbb\nccc\n", "\na\nbb\nccc\n");
    Test.ensureEqual(s, "", "s=" + s);
    s = String2.differentLine("", "");
    Test.ensureEqual(s, "", "s=" + s);
    s = String2.differentLine("a", "a");
    Test.ensureEqual(s, "", "s=" + s);

    s = String2.differentLine("\nbb\nccc", "a\nbb\nccc");
    Test.ensureEqual(s, "  old line #1=\"\",\n  new line #1=\"a\".", "s=" + s);
    s = String2.differentLine("a\nbb\nccc", "a\nbb\ncc");
    Test.ensureEqual(s, "  old line #3=\"ccc\",\n  new line #3=\"cc\".", "s=" + s);
    s = String2.differentLine("\na\nbb\nccc\n", "a\nbb\nccc\n");
    Test.ensureEqual(s, "  old line #1=\"\",\n  new line #1=\"a\".", "s=" + s);
    s = String2.differentLine("\na\nbb\nccc\n", "\na\nbb\nccc");
    Test.ensureEqual(s, "  old line #4=\"ccc\",\n  new line #4=\"ccc\".", "s=" + s);
    s = String2.differentLine("", "a");
    Test.ensureEqual(s, "  old line #1=\"\",\n  new line #1=\"a\".", "s=" + s);
    s = String2.differentLine("a", "");
    Test.ensureEqual(s, "  old line #1=\"a\",\n  new line #1=\"\".", "s=" + s);

    // isEmailAddress
    String2.log("test isEmailAddress");
    Test.ensureTrue(String2.isEmailAddress("john.smith@company.com"), "");
    Test.ensureTrue(String2.isEmailAddress("john.smith._%+-56@com.pany.com"), "");
    Test.ensureTrue(String2.isEmailAddress("John.Smith.._%+-56@Com.-pany.COM"), "");
    Test.ensureTrue(String2.isEmailAddress("a@b.co"), "");

    Test.ensureTrue(!String2.isEmailAddress("joh(n.smith@company.com"), "");
    Test.ensureTrue(!String2.isEmailAddress("john.smith@company.c"), "");
    Test.ensureTrue(!String2.isEmailAddress("john.smith@company.comedu"), "");
    Test.ensureTrue(!String2.isEmailAddress("john.smith@company.co2"), "");
    Test.ensureTrue(!String2.isEmailAddress("john.smith@comp+any.com"), "");
    Test.ensureTrue(!String2.isEmailAddress("john@smith@company.com"), "");
    Test.ensureTrue(!String2.isEmailAddress("@company.com"), "");
    Test.ensureTrue(!String2.isEmailAddress("john.smith@.com"), "");
    Test.ensureTrue(!String2.isEmailAddress("john smith@.com"), "");
    Test.ensureTrue(!String2.isEmailAddress("john.smith@"), "");

    // replaceAll
    String2.log("test replaceAll");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "b", "qt");
    Test.ensureEqual(sb.toString(), "Aqtcqtcd", "");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "b", "bb");
    Test.ensureEqual(sb.toString(), "Abbcbbcd", "");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "bc", "");
    Test.ensureEqual(sb.toString(), "Ad", "");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "", "a");
    Test.ensureEqual(sb.toString(), "Abcbcd", "");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "q", "r");
    Test.ensureEqual(sb.toString(), "Abcbcd", "");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "d", "q");
    Test.ensureEqual(sb.toString(), "Abcbcq", "");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "de", "q");
    Test.ensureEqual(sb.toString(), "Abcbcd", "");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "Ab", "q");
    Test.ensureEqual(sb.toString(), "qcbcd", "");
    sb = new StringBuilder("Abcbcd");
    String2.replaceAll(sb, "", "q");
    Test.ensureEqual(sb.toString(), "Abcbcd", "");

    // replaceAll
    String2.log("test replaceAll");
    Test.ensureEqual(String2.replaceAll("Abcbcd", "b", "qt"), "Aqtcqtcd", "");
    Test.ensureEqual(String2.replaceAll("ABcbcd", "B", "bb"), "Abbcbcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", "bc", ""), "Ad", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", "", "A"), "Abcbcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", "q", "r"), "Abcbcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", "d", "q"), "Abcbcq", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", "de", "q"), "Abcbcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", "Ab", "q"), "qcbcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", "", "q"), "Abcbcd", "");

    Test.ensureEqual(String2.replaceAll("abcbcd", 'A', 'q'), "abcbcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", 'a', 'q'), "Abcbcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", 'A', 'q'), "qbcbcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", 'b', 'r'), "Arcrcd", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", 'd', 's'), "Abcbcs", "");
    Test.ensureEqual(String2.replaceAll("Abcbcd", ' ', 't'), "Abcbcd", "");

    // replaceAllIgnoreCase
    String2.log("test replaceAllIgnoreCase");
    Test.ensureEqual(String2.replaceAllIgnoreCase("Abcbcd", "b", "qt"), "Aqtcqtcd", "");
    Test.ensureEqual(String2.replaceAllIgnoreCase("Abcbcd", "B", "qt"), "Aqtcqtcd", "");
    Test.ensureEqual(String2.replaceAllIgnoreCase("ABcbcd", "b", "qt"), "Aqtcqtcd", "");
    Test.ensureEqual(String2.replaceAllIgnoreCase("ABcbcd", "B", "qt"), "Aqtcqtcd", "");

    // repeatedlyReplaceAll
    Test.ensureEqual(String2.repeatedlyReplaceAll("AbBbBaBBaB", "bb", "b", true), "AbabaB", "");
    Test.ensureEqual(String2.repeatedlyReplaceAll("AbBbBaBBaB", "c", "c", true), "AbBbBaBBaB", "");
    sb = new StringBuilder("AbBbBaBBaB");
    Test.ensureEqual(String2.repeatedlyReplaceAll(sb, "bb", "b", true).toString(), "AbabaB", "");
    sb = new StringBuilder("AbBbBaBBaB");
    Test.ensureEqual(String2.repeatedlyReplaceAll(sb, "c", "c", true).toString(), "AbBbBaBBaB", "");

    // regexReplaceAll
    sb =
        new StringBuilder(
            "test1 &term1; <kbd>&amp;justLettersNumbers3;</kbd> and <kbd>&amp;good;</kbd> but not &amp;bad; or <kbd>&amp;other_char;</kbd> or <kbd>&amp;noCloseSemi</kbd>!");
    Test.ensureEqual(
        String2.regexReplaceAll(sb, "<kbd>(&amp;)[a-zA-Z0-9]+;</kbd>", 1, "&").toString(),
        "test1 &term1; <kbd>&justLettersNumbers3;</kbd> and <kbd>&good;</kbd> but not &amp;bad; or <kbd>&amp;other_char;</kbd> or <kbd>&amp;noCloseSemi</kbd>!",
        "");

    // canonical
    s = "twopart";
    String s2 = "two";
    s2 += "part";
    Test.ensureEqual(s == s2, false, "first test");
    s = String2.canonical(s);
    s2 = String2.canonical(s2);
    Test.ensureEqual(s == s2, true, "second test");

    // canonicalLock
    s = "twopart";
    s2 = "two";
    Lock lock1 = String2.canonicalLock(s);
    Lock lock2 = String2.canonicalLock(s2 + "part");
    Test.ensureEqual(lock1 == lock2, true, "first test");
    lock1 = String2.canonicalLock(String2.canonical(s));
    lock2 = String2.canonicalLock(String2.canonical(s2 + "part"));
    Test.ensureEqual(lock1 == lock2, true, "second test");

    // combine spaces
    String2.log("test combineSpaces");
    Test.ensureEqual(String2.combineSpaces("abcdef"), "abcdef", "a");
    Test.ensureEqual(
        String2.combineSpaces(" ab  (   c  {  d  e  f  )  g  }  "), "ab (c {d e f) g}", "b");

    // whitespacesToSpace
    String2.log("test whitespacesToSpace");
    Test.ensureEqual(String2.whitespacesToSpace("abcdef"), "abcdef", "a");
    Test.ensureEqual(
        String2.whitespacesToSpace("\tab\t\t(\t\t\tc\t\t{\t\td\t\te\t\tf\t\t)\t\tg\t\t}\t\t"),
        "ab (c {d e f) g}",
        "b");
    // zeroPad
    String2.log("test zeroPad");
    Test.ensureEqual(String2.zeroPad("a", 2), "0a", "a");
    Test.ensureEqual(String2.zeroPad("", 3), "000", "b");
    Test.ensureEqual(String2.zeroPad("abc", 3), "abc", "c");
    Test.ensureEqual(String2.zeroPad("54", 2), "54", "d");
    Test.ensureEqual(String2.zeroPad("54", 3), "054", "e");
    Test.ensureEqual(String2.zeroPad("54", 0), "54", "f");
    Test.ensureEqual(String2.zeroPad("54.6", 3), "054.6", "g");

    // multiLineStringToArrayList
    String2.log("test multiLineStringToArrayList");
    Test.ensureEqual(
        String2.multiLineStringToArrayList("a\t b\nc\n\nq\n").toArray(),
        new String[] {"a\t b", "c", "", "q", ""},
        "a");

    // substitute
    String2.log("test substitute");
    Test.ensureEqual(
        String2.substitute("a{0}bc{1}d{2}e{3}", "Bob", "Nate", "Nancy"),
        "aBobbcNatedNancye{3}",
        "a");
    Test.ensureEqual(
        String2.substitute("a{0}bc{1}d{2}e{3}", null, "Nate", null), "a{0}bcNated{2}e{3}", "b");

    // toCSVString
    String2.log("test toCSVString");
    Test.ensureEqual(String2.toCSSVString(new String[] {}), "", "a");
    Test.ensureEqual(String2.toCSSVString(new String[] {"a", null, "ccc"}), "a, [null], ccc", "b");
    ArrayList al = new ArrayList(); // this is also used in the next few tests
    al.add("1");
    al.add(null);
    al.add("333");
    Test.ensureEqual(String2.toCSSVString(al.toArray()), "1, [null], 333", "c");

    // toSSVString
    String2.log("test toSSVString");
    Test.ensureEqual(String2.toSSVString(new String[] {}), "", "a");
    Test.ensureEqual(String2.toSSVString(new String[] {"a", null, "ccc"}), "a [null] ccc", "b");
    Test.ensureEqual(String2.toSSVString(al.toArray()), "1 [null] 333", "c");

    // toNewlineString
    String2.log("test toNewlineString");
    Test.ensureEqual(String2.toNewlineString((Object[]) null), null, "a");
    Test.ensureEqual(String2.toNewlineString(new String[] {}), "", "b");
    Test.ensureEqual(
        String2.toNewlineString(new String[] {"a", null, "ccc"}), "a\n[null]\nccc\n", "c");
    Test.ensureEqual(String2.toNewlineString(al.toArray()), "1\n[null]\n333\n", "d");

    // toCSVString(byte[])
    String2.log("test toCSVString(byte[])");
    Test.ensureEqual(String2.toCSSVString(new byte[] {}), "", "a");
    Test.ensureEqual(String2.toCSSVString(new byte[] {1, 55, -4}), "1, 55, -4", "b");

    // toHexCSVString(short[])
    String2.log("test toHexCSVString(byte[])");
    Test.ensureEqual(String2.toHexCSSVString(new byte[] {}), "", "a");
    Test.ensureEqual(String2.toHexCSSVString(new byte[] {15, 60, -4}), "0xf, 0x3c, 0xfc", "b");

    // toCSVString(short[])
    String2.log("test toCSVString(short[])");
    Test.ensureEqual(String2.toCSSVString(new short[] {}), "", "a");
    Test.ensureEqual(String2.toCSSVString(new short[] {1, 55, -4}), "1, 55, -4", "b");

    // toHexCSVString(short[])
    String2.log("test toHexCSVString(short[])");
    Test.ensureEqual(String2.toHexCSSVString(new short[] {}), "", "a");
    Test.ensureEqual(String2.toHexCSSVString(new short[] {15, 60, -4}), "0xf, 0x3c, 0xfffc", "b");

    // toCSVString(int[])
    String2.log("test toCSVString(int[])");
    Test.ensureEqual(String2.toCSSVString(new int[] {}), "", "a");
    Test.ensureEqual(String2.toCSSVString(new int[] {1, 55, -4}), "1, 55, -4", "b");

    // toHexCSVString(int[])
    String2.log("test toHexCSVString(int[])");
    Test.ensureEqual(String2.toHexCSSVString(new int[] {}), "", "a");
    Test.ensureEqual(String2.toHexCSSVString(new int[] {15, 60, -4}), "0xf, 0x3c, 0xfffffffc", "b");

    // toCSVString(float[])
    String2.log("test toCSVString(float[])");
    Test.ensureEqual(String2.toCSSVString(new float[] {}), "", "a");
    Test.ensureEqual(String2.toCSSVString(new float[] {1f, 55.5f, -4.4f}), "1.0, 55.5, -4.4", "b");

    // toCSVString(double[])
    String2.log("test toCSVString(double[])");
    Test.ensureEqual(String2.toCSSVString(new double[] {}), "", "a");
    Test.ensureEqual(String2.toCSSVString(new double[] {1, 55.5, -4.4}), "1.0, 55.5, -4.4", "b");

    // toNewlineString(int[])
    String2.log("test toNewlineString(int[])");
    Test.ensureEqual(String2.toNewlineString(new int[] {}), "", "a");
    Test.ensureEqual(String2.toNewlineString(new int[] {1, 55, -4}), "1\n55\n-4\n", "b");

    // toNewlineString(double[])
    String2.log("test toNewlineString(double[])");
    Test.ensureEqual(String2.toNewlineString(new double[] {}), "", "a");
    Test.ensureEqual(
        String2.toNewlineString(new double[] {1, 55.5, -4.4}), "1.0\n55.5\n-4.4\n", "b");

    // toStringArray (via arrayList.toArray())
    String2.log("test toStringArray");
    sar = String2.toStringArray(al.toArray());
    Test.ensureEqual(sar.length, 3, "a");
    Test.ensureEqual(sar[0], "1", "b");
    Test.ensureEqual(sar[1], null, "c");
    Test.ensureEqual(sar[2], "333", "d");

    // toString(map)
    String2.log("test toString(map)");
    map = new HashMap();
    map.put("key a", "value a");
    map.put("Bob", "Simons");
    // order of elements is not specified and may change
    Test.ensureEqual(String2.toString(map), "key a = value a\nBob = Simons\n", "a");

    // toByteArray(s)
    String2.log("test toByteArray(s)");
    s = "ABCDEFGHIJKLMNOPÀÁÂ";
    Test.ensureEqual(
        String2.toCSSVString(String2.toByteArray(s)),
        "65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, -64, -63, -62",
        "a");

    // toByteArray(sb)
    String2.log("test toByteArray(s)");
    sb = new StringBuilder("ABCDEFGHIJKLMNOPÀÁÂ");
    Test.ensureEqual(
        String2.toCSSVString(String2.toByteArray(sb)),
        "65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, -64, -63, -62",
        "a");

    // hexDump
    String2.log("test hexDump");
    Test.ensureEqual(
        String2.hexDump(String2.toByteArray(s)),
        "41 42 43 44 45 46 47 48   49 4a 4b 4c 4d 4e 4f 50   ABCDEFGHIJKLMNOP |\n"
            + "c0 c1 c2                                                             |\n",
        "a");

    // indexOf(array, string)
    String2.log("test indexOf(array, string)");
    sar = new String[] {"Bob", "Nate", "Nancy"};
    Test.ensureEqual(String2.indexOf(sar, ""), -1, "a");
    Test.ensureEqual(String2.indexOf(sar, "Bob"), 0, "b");
    Test.ensureEqual(String2.indexOf(sar, "Nate"), 1, "c");
    Test.ensureEqual(String2.indexOf(sar, "Nancy"), 2, "d");
    Test.ensureEqual(String2.indexOf(sar, "Dave"), -1, "e");
    Test.ensureEqual(String2.indexOf(sar, "Bo"), -1, "f");
    Test.ensureEqual(String2.indexOf(sar, "Bobb"), -1, "g");

    // indexOf(array, string, startAt)
    String2.log("test indexOf(array, string, startAt)");
    sar = new String[] {"Bob", "Nate", "Nancy"};
    Test.ensureEqual(String2.indexOf(sar, "Bob", -1), 0, "a");
    Test.ensureEqual(String2.indexOf(sar, "Bob", 0), 0, "b");
    Test.ensureEqual(String2.indexOf(sar, "Bob", 1), -1, "c");
    Test.ensureEqual(String2.indexOf(sar, "Bob", 3), -1, "d");
    Test.ensureEqual(String2.indexOf(sar, "Nancy", 2), 2, "e");
    Test.ensureEqual(String2.indexOf(sar, "Dave", 0), -1, "f");

    // lineContaining(array, string)
    String2.log("test lineContaining(array, string)");
    sar = new String[] {"Bob", "Nate", "Nancy"};
    Test.ensureEqual(String2.lineContaining(sar, ""), 0, "a");
    Test.ensureEqual(String2.lineContaining(sar, "Bob"), 0, "b");
    Test.ensureEqual(String2.lineContaining(sar, "Nate"), 1, "c");
    Test.ensureEqual(String2.lineContaining(sar, "Nancy"), 2, "d");
    Test.ensureEqual(String2.lineContaining(sar, "Dave"), -1, "e");
    Test.ensureEqual(String2.lineContaining(sar, "Bo"), 0, "f");
    Test.ensureEqual(String2.lineContaining(sar, "Bobb"), -1, "g");
    Test.ensureEqual(String2.lineContaining(sar, "ob"), 0, "h");
    Test.ensureEqual(String2.lineContaining(sar, "a"), 1, "i");

    // lineContaining(array, string, startAt)
    String2.log("test lineContaining(array, string, startAt)");
    sar = new String[] {"Bob", "Nate", "Nancy"};
    Test.ensureEqual(String2.lineContaining(sar, "a", -1), 1, "a");
    Test.ensureEqual(String2.lineContaining(sar, "a", 1), 1, "b");
    Test.ensureEqual(String2.lineContaining(sar, "a", 2), 2, "c");
    Test.ensureEqual(String2.lineContaining(sar, "a", 3), -1, "d");

    // splitToArrayList
    String2.log("test splitToArrayList");
    Test.ensureEqual(String2.splitToArrayList(null, '\t'), null, "a");
    Test.ensureEqual(String2.splitToArrayList("", '\t').toArray(), new String[] {""}, "a");
    Test.ensureEqual(
        String2.toCSSVString(String2.splitToArrayList(" b \t b", '\t').toArray()), "b, b", "b");
    Test.ensureEqual(
        String2.toCSSVString(String2.splitToArrayList("ab\tcd\t", '\t').toArray()),
        "ab, cd, ",
        "c");

    // split to String[]
    String2.log("test split to String[]");
    Test.ensureEqual(String2.split(null, '\t'), null, "a");
    Test.ensureEqual(String2.split("", '\t'), new String[] {""}, "a");
    Test.ensureEqual(String2.toCSSVString(String2.split(" b \t b", '\t')), "b, b", "b");
    Test.ensureEqual(String2.toCSSVString(String2.split("ab\tcd\t", '\t')), "ab, cd, ", "c");

    // toIntArray(I[])
    String2.log("test toIntArray(I[])");
    Integer Iar[] = {Integer.valueOf(1), Integer.valueOf(333)};
    Test.ensureEqual(String2.toCSSVString(String2.toIntArray(Iar)), "1, 333", "a");
    Object oar[] = {Integer.valueOf(-1), " -333 ", "b"};
    Test.ensureEqual(String2.toCSSVString(String2.toIntArray(oar)), "-1, -333, 2147483647", "b");

    // toFloatArray(F[])
    String2.log("test toFloatArray(F[])");
    Float Far[] = {Float.valueOf(1.1f), Float.valueOf(333.3f)};
    Test.ensureEqual(String2.toCSSVString(String2.toFloatArray(Far)), "1.1, 333.3", "a");
    oar = new Object[] {Float.valueOf(-1.1f), " -333.3 ", "b"};
    Test.ensureEqual(String2.toCSSVString(String2.toFloatArray(oar)), "-1.1, -333.3, NaN", "b");

    // toDoubleArray(D[])
    String2.log("test toDoubleArray(D[])");
    Double Dar[] = {Double.valueOf(1.1), Double.valueOf(333.3)};
    Test.ensureEqual(String2.toCSSVString(String2.toDoubleArray(Dar)), "1.1, 333.3", "a");
    oar = new Object[] {Double.valueOf(-1.1), " -333.3 ", "b"};
    Test.ensureEqual(String2.toCSSVString(String2.toDoubleArray(oar)), "-1.1, -333.3, NaN", "b");

    // toIntArray(arrayList)
    String2.log("test toIntArray(arrayList)");
    al = new ArrayList();
    al.add(Integer.valueOf(1));
    al.add(Integer.valueOf(333));
    Test.ensureEqual(String2.toCSSVString(String2.toIntArray(al)), "1, 333", "a");

    // toFloatArray(arrayList)
    String2.log("test toFloatArray(arrayList)");
    al = new ArrayList();
    al.add(Float.valueOf(1.1f));
    al.add(Float.valueOf(333.3f));
    Test.ensureEqual(String2.toCSSVString(String2.toFloatArray(al)), "1.1, 333.3", "a");

    // toDoubleArray(arrayList)
    String2.log("test toDoubleArray(arrayList)");
    al = new ArrayList();
    al.add(Double.valueOf(1.1));
    al.add(Double.valueOf(333.3));
    Test.ensureEqual(String2.toCSSVString(String2.toDoubleArray(al)), "1.1, 333.3", "a");

    // justFiniteValues(int[])
    String2.log("test justFiniteValues(int[])");
    iar = new int[] {2147483647, 2, 2147483647, 444, 2147483647};
    Test.ensureEqual(String2.toCSSVString(String2.justFiniteValues(iar)), "2, 444", "a");

    // justFiniteValues(double[])
    String2.log("test justFiniteValues(double[])");
    dar = new double[] {Double.NaN, 2.2, Double.POSITIVE_INFINITY, 444.4, Double.NEGATIVE_INFINITY};
    Test.ensureEqual(String2.toCSSVString(String2.justFiniteValues(dar)), "2.2, 444.4", "a");

    // removeNull
    String2.log("test removeNull");
    sar = new String[] {"a", null, "", "d"};
    String sar2[] = new String[] {"a", "", "d"};
    Test.ensureEqual(String2.removeNull(sar), sar2, "a");

    // removeNullOrEmpty
    String2.log("test removeNullOrEmpty");
    sar = new String[] {"a", null, "", "d"};
    sar2 = new String[] {"a", "d"};
    Test.ensureEqual(String2.removeNullOrEmpty(sar), sar2, "a");

    // csvToIntArray
    String2.log("test csvToIntArray");
    Test.ensureEqual(
        String2.toCSSVString(String2.csvToIntArray("5, a, -9")), "5, 2147483647, -9", "a");
    Test.ensureEqual(String2.csvToIntArray("5"), new int[] {5}, "b");
    Test.ensureEqual(String2.csvToIntArray("a"), new int[] {Integer.MAX_VALUE}, "c");
    Test.ensureEqual(String2.csvToIntArray(""), new int[] {Integer.MAX_VALUE}, "d");

    // csvToDoubleArray
    String2.log("test csvToDoubleArray");
    Test.ensureEqual(
        String2.toCSSVString(String2.csvToDoubleArray("5.5, a, -9.9")), "5.5, NaN, -9.9", "a");
    Test.ensureEqual(String2.csvToDoubleArray("5.5"), new double[] {5.5}, "b");
    Test.ensureEqual(String2.csvToDoubleArray("a"), new double[] {Double.NaN}, "c");
    Test.ensureEqual(String2.csvToDoubleArray(""), new double[] {Double.NaN}, "a");

    // parseBoolean
    String2.log("test parseBoolean");
    Test.ensureEqual(String2.parseBoolean("FALSE"), false, "a");
    Test.ensureEqual(String2.parseBoolean("falSE"), false, "b");
    Test.ensureEqual(String2.parseBoolean(" f  "), false, "c");
    Test.ensureEqual(String2.parseBoolean("F"), false, "d");
    Test.ensureEqual(String2.parseBoolean("0"), false, "e");
    Test.ensureEqual(String2.parseBoolean("TRUE"), true, "f");
    Test.ensureEqual(String2.parseBoolean("truE"), true, "g");
    Test.ensureEqual(String2.parseBoolean("t"), true, "h");
    Test.ensureEqual(String2.parseBoolean("T"), true, "i");
    Test.ensureEqual(String2.parseBoolean("1.1"), true, "j");
    Test.ensureEqual(String2.parseBoolean("x"), true, "k");

    // parseInt
    String2.log("test parseInt");
    Test.ensureEqual(String2.parseInt("12"), 12, "a");
    Test.ensureEqual(String2.parseInt(" -12"), -12, "b");
    Test.ensureEqual(String2.parseInt(" 1e2 "), 100, "c");
    Test.ensureEqual(String2.parseInt("9000000000"), Integer.MAX_VALUE, "d");
    Test.ensureEqual(String2.parseInt("0.2"), 0, "e");
    Test.ensureEqual(String2.parseInt("2a"), Integer.MAX_VALUE, "f");
    Test.ensureEqual(String2.parseInt("0x0"), 0, "g");
    Test.ensureEqual(String2.parseInt("0xFF"), 255, "h");
    Test.ensureEqual(String2.parseInt("0x7ffffffe"), 2147483646, "");
    Test.ensureEqual(String2.parseInt("0x7fffffff"), 2147483647, "");
    Test.ensureEqual(String2.parseInt("0x80000000"), -2147483648, "");
    Test.ensureEqual(String2.parseInt("0x80000001"), -2147483647, "");
    Test.ensureEqual(String2.parseInt("0xfffffffc"), -4, "");
    // number starting with 0 is treated as decimal (not octal as Java would)
    Test.ensureEqual(String2.parseInt("0012"), 12, "h");

    Test.ensureEqual(String2.to0xHexString(10, 0), "0xa", "");
    Test.ensureEqual(String2.to0xHexString(10, 8), "0x0000000a", "");
    Test.ensureEqual(String2.to0xHexString(-4, 2), "0xfffffffc", "");

    // 2011-02-09 avoid Java bug parsing certain floating point numbers
    // http://www.exploringbinary.com/java-hangs-when-converting-2-2250738585072012e-308/
    String2.log("test parseInt troublesome strings");
    Test.ensureEqual(String2.parseInt("2.2250738585072012e-308"), 0, "trouble");
    Test.ensureEqual(String2.parseInt("0.00022250738585072012e-304"), 0, "trouble");
    Test.ensureEqual(String2.parseInt("00000000002.2250738585072012e-308"), 0, "trouble");
    Test.ensureEqual(String2.parseInt("2.225073858507201200000e-308"), 0, "trouble");
    Test.ensureEqual(String2.parseInt("2.2250738585072012e-00308"), 0, "trouble");
    Test.ensureEqual(String2.parseInt("2.2250738585072012997800001e-308"), 0, "trouble");
    String2.log("done testing parseInt troublesome strings");

    // parseDouble
    String2.log("test parseDouble");
    Test.ensureEqual(String2.parseDouble("12.3"), 12.3, "a");
    Test.ensureEqual(String2.parseDouble(" -12.3"), -12.3, "b-");
    Test.ensureEqual(
        String2.parseDouble(" +12.3"), 12.3, "b+"); // NdbcMeteorologicalStation.txt files use this
    // format
    Test.ensureEqual(String2.parseDouble(" 1.7e2 "), 170, "c");
    Test.ensureEqual(String2.parseDouble("0xFF"), 255, "d");
    Test.ensureEqual(String2.parseDouble("0x7ffffffe"), 2147483646, "");
    Test.ensureEqual(String2.parseDouble("0x7fffffff"), 2147483647, "");
    Test.ensureEqual(String2.parseDouble("0x80000000"), 2147483648L, "");
    Test.ensureEqual(String2.parseDouble("0x80000001"), 2147483649L, "");
    Test.ensureEqual(String2.parseDouble("0xfffffffc"), 4294967292L, "");
    Test.ensureEqual(
        String2.parseDouble("1e400"),
        Double.NaN,
        "d"); // 2020-04-03 was Double.POSITIVE_INFINITY, but
    // now I just want NaN
    Test.ensureEqual(String2.parseDouble("0a"), Double.NaN, "e");
    Test.ensureEqual(String2.parseDouble(null), Double.NaN, "e");
    // number starting with 0 is treated as decimal (not octal as Java would)
    Test.ensureEqual(String2.parseDouble("0012"), 12, "h");

    // 2011-02-09 avoid Java bug parsing certain floating point numbers
    // http://www.exploringbinary.com/java-hangs-when-converting-2-2250738585072012e-308/
    String2.log("test parseDouble troublesome strings");
    Test.ensureEqual(String2.parseDouble("2.2250738585072012e-308"), 0, "trouble");
    Test.ensureEqual(String2.parseDouble("0.00022250738585072012e-304"), 0, "trouble");
    Test.ensureEqual(String2.parseDouble("00000000002.2250738585072012e-308"), 0, "trouble");
    Test.ensureEqual(String2.parseDouble("2.225073858507201200000e-308"), 0, "trouble");
    Test.ensureEqual(String2.parseDouble("2.2250738585072012e-00308"), 0, "trouble");
    Test.ensureEqual(String2.parseDouble("2.2250738585072012997800001e-308"), 0, "trouble");
    String2.log("done testing parseDouble troublesome strings");

    // roundingParseInt
    String2.log("test roundingParseInt");
    Test.ensureEqual(String2.roundingParseInt("12.3"), 12, "a");
    Test.ensureEqual(String2.roundingParseInt(" -12.3"), -12, "b");
    Test.ensureEqual(String2.roundingParseInt(" +1.7123e2 "), 171, "c");
    Test.ensureEqual(String2.roundingParseInt("0xff "), 255, "c");
    Test.ensureEqual(String2.roundingParseInt("9000000000"), Integer.MAX_VALUE, "d");
    Test.ensureEqual(String2.roundingParseInt("0a"), Integer.MAX_VALUE, "e");
    Test.ensureEqual(String2.roundingParseInt(null), Integer.MAX_VALUE, "e");

    // parseLong
    String2.log("test parseLong");
    Test.ensureEqual(String2.parseLong("12"), 12, "a");
    Test.ensureEqual(String2.parseLong(" -12"), -12, "b");
    Test.ensureEqual(String2.parseLong(" 1e2 "), 100, "c");
    Test.ensureEqual(String2.parseLong("9000000000000000000000000"), Long.MAX_VALUE, "d");
    Test.ensureEqual(String2.parseLong("0.2"), 0, "e");
    Test.ensureEqual(String2.parseLong("2a"), Long.MAX_VALUE, "f");
    Test.ensureEqual(String2.parseLong("0xFFFFFFFFFF"), 0xFFFFFFFFFFL, "g");

    // test speed of native parseLong vs String2.parseLong (now using
    // parseBigDecimal)
    n = 10000000;
    long speedResults[] = new long[2];
    for (int test = 0; test < 2; test++) {
      Math2.gc("TestUtil (between tests)", 2000);
      Math2.gc("TestUtil (between tests)", 2000);
      long testSum = 0;
      time = System.currentTimeMillis();
      for (i = 0; i < n; i++) {
        String ts = "" + i;
        testSum += test == 0 ? Long.parseLong(ts) : String2.parseLong(ts);
      }
      speedResults[test] = System.currentTimeMillis() - time;
      String2.log("sum=" + testSum); // so compiler can't delete all of this
    }
    // TODO handle time based performance tests better
    // Test.ensureTrue(speedResults[1] < 2700,
    //         "String2.parseLong is too slow! " + speedResults[1] + " (Java 17 typical: 1900ms in
    // TestAll)");
    String2.log(
        "test speed of Long.parseLong vs String2.parseLong (now using parseBigDecimal): "
            + String2.toCSSVString(speedResults));
    // TODO handle time based performance tests better
    // Test.ensureTrue(speedResults[1] < speedResults[0] * 2,
    //        "String2.parseLong is too slow! " + speedResults[1] + " vs " + speedResults[0]
    //                + " (Java 17 typical: 1900ms vs 1300ms");
    Math2.gc("TestUtil (between tests)", 2000);
    Math2.gc("TestUtil (between tests)", 2000);

    // parseFloat
    String2.log("test parseFloat");
    Test.ensureEqual(String2.parseFloat("12.5"), 12.5, "a"); // .5 avoids bruised number
    Test.ensureEqual(String2.parseFloat(" -12.5"), -12.5, "b");
    Test.ensureEqual(String2.parseFloat(" 1.7e2 "), 170, "c");
    Test.ensureEqual(
        String2.parseFloat("1e40"),
        Float.NaN,
        "d"); // 2020-04-03 was Float.POSITIVE_INFINITY, but now
    // I just want NaN
    Test.ensureEqual(String2.parseFloat("0a"), Float.NaN, "e");

    // tokenize
    String2.log("test tokenize");
    Test.ensureEqual(
        String2.tokenize(" a  bb   ccc \"d d\" eeee"),
        new String[] {"a", "bb", "ccc", "d d", "eeee"},
        "a");

    // distributeTime and timeDistributionStatistics
    String2.log("test distributeTime");
    int timeDist[] = new int[String2.TimeDistributionSize];
    String2.distributeTime(7200000, timeDist);
    n = String2.getTimeDistributionN(timeDist);
    Test.ensureEqual(n, 1, "");
    Test.ensureEqual(String2.getTimeDistributionMedian(timeDist, n), 3600000 * 2, "");

    timeDist = new int[String2.TimeDistributionSize];
    String2.distributeTime(87, timeDist);
    String2.distributeTime(85, timeDist);
    String2.distributeTime(0, timeDist);
    String2.distributeTime(1234, timeDist);
    String2.distributeTime(12345678, timeDist);
    n = String2.getTimeDistributionN(timeDist);
    Test.ensureEqual(n, 5, "");
    Test.ensureEqual(String2.getTimeDistributionMedian(timeDist, n), 88, "");
    Test.ensureEqual(
        String2.getTimeDistributionStatistics(timeDist),
        "    n =        5,  median ~=       88 ms\n"
            + "    0 ms:               1\n"
            + "    1 ms:               0\n"
            + "    2 ms:               0\n"
            + "    <= 5 ms:            0\n"
            + "    <= 10 ms:           0\n"
            + "    <= 20 ms:           0\n"
            + "    <= 50 ms:           0\n"
            + "    <= 100 ms:          2\n"
            + "    <= 200 ms:          0\n"
            + "    <= 500 ms:          0\n"
            + "    <= 1 s:             0\n"
            + "    <= 2 s:             1\n"
            + "    <= 5 s:             0\n"
            + "    <= 10 s:            0\n"
            + "    <= 20 s:            0\n"
            + "    <= 1 min:           0\n"
            + "    <= 2 min:           0\n"
            + "    <= 5 min:           0\n"
            + "    <= 10 min:          0\n"
            + "    <= 20 min:          0\n"
            + "    <= 1 hr:            0\n"
            + "    >  1 hr:            1\n",
        "a");
    timeDist = new int[String2.TimeDistributionSize];
    String2.distributeTime(52, timeDist);
    String2.distributeTime(2 * Calendar2.MILLIS_PER_HOUR, timeDist);
    String2.distributeTime(10 * Calendar2.MILLIS_PER_HOUR, timeDist);
    n = String2.getTimeDistributionN(timeDist);
    Test.ensureEqual(n, 3, "");
    Test.ensureEqual(String2.getTimeDistributionMedian(timeDist, n), 5400000, "");

    // distributeCount and CountDistributionStatistics
    String2.log("test distributeCount");
    int countDist[] = new int[String2.CountDistributionSize];
    String2.distributeCount(200, countDist);
    n = String2.getCountDistributionN(countDist);
    Test.ensureEqual(n, 1, "");
    Test.ensureEqual(String2.getCountDistributionMedian(countDist, n), 200, "");

    String2.distributeCount(-1, countDist);
    String2.distributeCount(4, countDist);
    Test.ensureEqual(
        String2.getCountDistributionStatistics(countDist),
        "    n =        3,  median ~=        4\n"
            + "    0:               1\n"
            + "    1:               0\n"
            + "    2:               0\n"
            + "    <= 5:            1\n"
            + "    <= 10:           0\n"
            + "    <= 20:           0\n"
            + "    <= 50:           0\n"
            + "    <= 100:          0\n"
            + "    >  100:          1\n",
        "a");
    n = String2.getCountDistributionN(countDist);
    Test.ensureEqual(n, 3, "");
    Test.ensureEqual(String2.getCountDistributionMedian(countDist, n), 4, "");

    // simpleSearchAndReplace
    String2.log("test simpleSearchAndReplace");
    int random = Math2.random(Integer.MAX_VALUE);
    String utilDir = TEMP_DIR.toAbsolutePath().toString() + "/";
    Test.ensureEqual(
        File2.writeToFile88591(utilDir + random + ".asc", "1\nNaNny\nhi, NaN!\n4\n"), "", "a");
    File2.simpleSearchAndReplace(
        utilDir + random + ".asc", utilDir + random + "b.asc", File2.ISO_8859_1, "NaN", "99999");
    sar = File2.readFromFile88591(utilDir + random + "b.asc");
    Test.ensureEqual(sar[0], "", "b");
    Test.ensureEqual(sar[1], "1\n99999ny\nhi, 99999!\n4\n", "c");
    File2.delete(utilDir + random + "b.asc");

    // regexSearchAndReplace
    String2.log("test regexSearchAndReplace");
    File2.regexSearchAndReplace(
        utilDir + random + ".asc",
        utilDir + random + "b.asc",
        File2.ISO_8859_1,
        "\\bNaN\\b",
        "99999"); // \b = word boundary
    sar = File2.readFromFile88591(utilDir + random + "b.asc");
    Test.ensureEqual(sar[0], "", "b");
    Test.ensureEqual(sar[1], "1\nNaNny\nhi, 99999!\n4\n", "c");
    File2.delete(utilDir + random + ".asc");
    File2.delete(utilDir + random + "b.asc");

    // getKeysAndValuesString
    String2.log("test getKeysAndValuesString");
    HashMap hm = new HashMap();
    hm.put("key3", "value3");
    hm.put("key2", "value2");
    hm.put("key1", "value1");
    hm.put("key4", "value4");
    Test.ensureEqual(
        String2.getKeysAndValuesString(hm),
        "key1: value1\n" + "key2: value2\n" + "key3: value3\n" + "key4: value4\n",
        "a");

    // genEFormat6
    String2.log("test genEFormat6");
    Test.ensureEqual(String2.genEFormat6(Double.NaN), "NaN", ""); // not finite
    Test.ensureEqual(String2.genEFormat6(Double.POSITIVE_INFINITY), "Infinity", "");
    Test.ensureEqual(String2.genEFormat6(Double.NEGATIVE_INFINITY), "-Infinity", "");
    Test.ensureEqual(String2.genEFormat6(3), "3", ""); // integers
    Test.ensureEqual(String2.genEFormat6(-3), "-3", "");
    Test.ensureEqual(String2.genEFormat6(0), "0", ""); // zero
    Test.ensureEqual(String2.genEFormat6(1e-15), "0", ""); // almostZero
    Test.ensureEqual(String2.genEFormat6(-1e-15), "0", ""); // almostZero
    Test.ensureEqual(String2.genEFormat6(1.123456), "1.123456", ""); // 6 digits to right
    Test.ensureEqual(String2.genEFormat6(-1.123456), "-1.123456", "");
    Test.ensureEqual(String2.genEFormat6(1.12345678), "1.123457", ""); // 7 -> 6 digits to right
    Test.ensureEqual(String2.genEFormat6(-1.12345678), "-1.123457", "");
    Test.ensureEqual(String2.genEFormat6(999999.9999996), "1E6", ""); // 6 digits to left boundary
    Test.ensureEqual(String2.genEFormat6(-999999.9999996), "-1E6", "");
    Test.ensureEqual(
        String2.genEFormat6(999999.9999994), "999999.999999", ""); // 6 digits to left boundary
    Test.ensureEqual(String2.genEFormat6(-999999.9999994), "-999999.999999", "");
    Test.ensureEqual(String2.genEFormat6(123456.98765432), "123456.987654", ""); // 6 digits to left
    Test.ensureEqual(String2.genEFormat6(-123456.98765432), "-123456.987654", "");
    Test.ensureEqual(String2.genEFormat6(1234567.98765432), "1.234568E6", ""); // 7 digits to left
    Test.ensureEqual(String2.genEFormat6(-1234567.98765432), "-1.234568E6", "");
    Test.ensureEqual(String2.genEFormat6(0.09876), "9.876E-2", ""); // <.1
    Test.ensureEqual(String2.genEFormat6(-0.09876), "-9.876E-2", "");
    Test.ensureEqual(String2.genEFormat6(0.0999995), "0.1", ""); // .1 boundary
    Test.ensureEqual(String2.genEFormat6(-0.0999995), "-0.1", ""); //
    Test.ensureEqual(String2.genEFormat6(0.0999994), "9.99994E-2", ""); // .1 boundary
    Test.ensureEqual(String2.genEFormat6(-0.0999994), "-9.99994E-2", ""); //
    Test.ensureEqual(
        String2.genEFormat6(0.0123456789), "1.234568E-2", ""); // <.1, to 6 decimal digits
    Test.ensureEqual(String2.genEFormat6(-0.0123456789), "-1.234568E-2", "");
    Test.ensureEqual(String2.genEFormat6(0.0021), "0.0021", ""); // simple numbers
    Test.ensureEqual(String2.genEFormat6(-0.0021), "-0.0021", "");
    Test.ensureEqual(String2.genEFormat6(0.00213), "2.13E-3", ""); // but not almost-simple numbers
    Test.ensureEqual(String2.genEFormat6(-0.00213), "-2.13E-3", "");
    Test.ensureEqual(String2.genEFormat6(0.00024), "2.4E-4", ""); // but or smaller simple numbers
    Test.ensureEqual(String2.genEFormat6(-0.00024), "-2.4E-4", "");

    // genEFormat
    String2.log("test genEFormat10");
    Test.ensureEqual(String2.genEFormat10(Double.NaN), "NaN", ""); // not finite
    Test.ensureEqual(String2.genEFormat10(Double.POSITIVE_INFINITY), "Infinity", "");
    Test.ensureEqual(String2.genEFormat10(Double.NEGATIVE_INFINITY), "-Infinity", "");
    Test.ensureEqual(String2.genEFormat10(3), "3", ""); // integers
    Test.ensureEqual(String2.genEFormat10(-3), "-3", "");
    Test.ensureEqual(String2.genEFormat10(0), "0", ""); // zero
    Test.ensureEqual(String2.genEFormat10(1e-15), "0", ""); // almostZero
    Test.ensureEqual(String2.genEFormat10(-1e-15), "0", ""); // almostZero
    Test.ensureEqual(String2.genEFormat10(1.4444123456), "1.4444123456", ""); // 10 digits to right
    Test.ensureEqual(String2.genEFormat10(-1.4444123456), "-1.4444123456", "");
    Test.ensureEqual(
        String2.genEFormat10(1.444412345678), "1.4444123457", ""); // 11 -> 10 digits to right
    Test.ensureEqual(String2.genEFormat10(-1.444412345678), "-1.4444123457", "");
    Test.ensureEqual(
        String2.genEFormat10(999999.99999999995), "1000000", ""); // UNEXPECTED! because of limited
    // precision. 6 digits to left
    // boundary
    Test.ensureEqual(String2.genEFormat10(-999999.99999999995), "-1000000", "");
    Test.ensureEqual(
        String2.genEFormat10(999999.99999999994), "999999.9999999999", ""); // 6 digits to left
    // boundary
    Test.ensureEqual(String2.genEFormat10(-999999.99999999994), "-999999.9999999999", "");
    Test.ensureEqual(
        String2.genEFormat10(123456.444498765432), "123456.4444987654", ""); // 6 digits to left
    Test.ensureEqual(String2.genEFormat10(-123456.444498765432), "-123456.4444987654", "");
    Test.ensureEqual(
        String2.genEFormat10(1234567.444498765432), "1.2345674445E6", ""); // 7 digits to left
    Test.ensureEqual(String2.genEFormat10(-1234567.444498765432), "-1.2345674445E6", "");
    Test.ensureEqual(String2.genEFormat10(0.094444876), "9.4444876E-2", ""); // <.1
    Test.ensureEqual(String2.genEFormat10(-0.094444876), "-9.4444876E-2", ""); //
    Test.ensureEqual(String2.genEFormat10(0.09999999996), "0.1", ""); // .1 boundary
    Test.ensureEqual(String2.genEFormat10(-0.09999999996), "-0.1", ""); //
    Test.ensureEqual(String2.genEFormat10(0.01234543212345), "1.2345432123E-2", ""); // .1 boundary
    Test.ensureEqual(String2.genEFormat10(-0.01234543212345), "-1.2345432123E-2", ""); //
    Test.ensureEqual(String2.genEFormat10(0.002112), "0.002112", ""); // simple numbers
    Test.ensureEqual(String2.genEFormat10(-0.002112), "-0.002112", "");
    Test.ensureEqual(
        String2.genEFormat10(0.002131234), "2.131234E-3", ""); // but not almost-simple numbers
    Test.ensureEqual(String2.genEFormat10(-0.002131234), "-2.131234E-3", "");
    Test.ensureEqual(
        String2.genEFormat10(0.000000213), "2.13E-7", ""); // and not smaller simple numbers
    Test.ensureEqual(String2.genEFormat10(-0.000000213), "-2.13E-7", "");

    // genX10Format6
    String2.log("test genX10Format6");
    Test.ensureEqual(String2.genX10Format6(1.123456), "1.123456", ""); // no E
    Test.ensureEqual(String2.genX10Format6(-1.123456), "-1.123456", "");
    Test.ensureEqual(String2.genX10Format6(0.09876), "9.876x10^-2", ""); // E -> x10^
    Test.ensureEqual(String2.genX10Format6(-0.09876), "-9.876x10^-2", "");

    // genX10Format10
    String2.log("test genX10Format10");
    Test.ensureEqual(String2.genX10Format10(1.4444123456), "1.4444123456", ""); // no E
    Test.ensureEqual(String2.genX10Format10(-1.4444123456), "-1.4444123456", "");
    Test.ensureEqual(String2.genX10Format10(0.098765432), "9.8765432x10^-2", ""); // E -> x10^
    Test.ensureEqual(String2.genX10Format10(-0.098765432), "-9.8765432x10^-2", "");

    // genHTMLFormat6
    String2.log("test genHTMLFormat6");
    Test.ensureEqual(String2.genHTMLFormat6(1.123456), "1.123456", ""); // no E
    Test.ensureEqual(String2.genHTMLFormat6(-1.123456), "-1.123456", "");
    Test.ensureEqual(
        String2.genHTMLFormat6(0.09876), "9.876x10<sup>-2</sup>", ""); // E -> x10<sup> </sup>
    Test.ensureEqual(String2.genHTMLFormat6(-0.09876), "-9.876x10<sup>-2</sup>", "");

    // genHTMLFormat
    String2.log("test genHTMLFormat10");
    Test.ensureEqual(String2.genHTMLFormat10(1.4444123456), "1.4444123456", ""); // no E
    Test.ensureEqual(String2.genHTMLFormat10(-1.4444123456), "-1.4444123456", "");
    Test.ensureEqual(
        String2.genHTMLFormat10(0.098765432),
        "9.8765432x10<sup>-2</sup>",
        ""); // E -> x10<sup> </sup>
    Test.ensureEqual(String2.genHTMLFormat10(-0.098765432), "-9.8765432x10<sup>-2</sup>", "");

    // trim
    String2.log("test trim");
    sb.setLength(0);
    Test.ensureEqual(String2.trim(sb).toString(), "", "a");
    sb = new StringBuilder("b");
    Test.ensureEqual(String2.trim(sb).toString(), "b", "b");
    sb = new StringBuilder(" \t c \t");
    Test.ensureEqual(String2.trim(sb).toString(), "c", "c");
    sb = new StringBuilder(" \t\r\n\t ");
    Test.ensureEqual(String2.trim(sb).toString(), "", "");

    // trimStart
    Test.ensureEqual(String2.trimStart("\n A\n "), "A\n ", "");
    Test.ensureEqual(String2.trimStart("\n \n "), "", "");
    Test.ensureEqual(String2.trimStart("AB"), "AB", "");
    Test.ensureEqual(String2.trimStart(""), "", "");
    Test.ensureEqual(String2.trimStart(null), null, "");

    // trimEnd
    Test.ensureEqual(String2.trimEnd("\n A\n "), "\n A", "");
    Test.ensureEqual(String2.trimEnd("\n \n "), "", "");
    Test.ensureEqual(String2.trimEnd("AB"), "AB", "");
    Test.ensureEqual(String2.trimEnd(""), "", "");
    Test.ensureEqual(String2.trimEnd(null), null, "");

    // alternate
    String2.log("test alternate");
    Test.ensureEqual(String2.alternateToString(null), "    [null]\n", "test a");
    Test.ensureEqual(
        String2.alternateGetValue(null, "a"), null, "test b"); // 'get' when arraylist is null
    ArrayList alternate = new ArrayList();
    String2.alternateSetValue(alternate, "a", "able"); // "add 'a'");
    Test.ensureEqual(String2.alternateSetValue(alternate, "b", "bob"), null, "set 'b' bob");
    Test.ensureEqual(
        String2.alternateSetValue(alternate, "b", "baker"), "bob", "replace 'bob' with 'baker'");
    Test.ensureEqual(String2.alternateToString(alternate), "    a=able\n    b=baker\n", "test e");
    Test.ensureEqual(String2.alternateGetValue(alternate, "a"), "able", "test f");
    Test.ensureEqual(String2.alternateGetValue(alternate, "b"), "baker", "test g");
    Test.ensureEqual(
        String2.alternateGetValue(alternate, "c"), null, "look for something not there");
    Test.ensureEqual(String2.alternateSetValue(alternate, "a", null), "able", "remove 'a'");
    Test.ensureEqual(alternate.size(), 2, "size is smaller now");
    Test.ensureEqual(String2.alternateGetValue(alternate, "a"), null, "'a' is gone");
    Test.ensureEqual(String2.alternateGetValue(alternate, "b"), "baker", "'b' still there");

    // TODO: add test for getClassPath
    // getClassPath (with / separator and / at the end)
    // String2.log("test getClassPath current=" + File2.getClassPath());
    // there is no way to test this and have it work with different installations
    // test for my computer (comment out on other computers):
    // Test.ensureEqual(File2.getClassPath(),
    // "C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/classes/", "a");
    // this is a wimpy test, but will work with all installations under Tomcat
    // Test.ensureEqual(File2.getClassPath().endsWith("/WEB-INF/classes/"), true,
    // "a");

    // toRational
    String2.log("test String2.toRational");
    int iar1[], iar2[];
    Test.ensureEqual(
        iar1 = String2.toRational(0),
        iar2 = new int[] {0, 0},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(1),
        iar2 = new int[] {1, 0},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(10000e-4),
        iar2 = new int[] {1, 0},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(32.48),
        iar2 = new int[] {3248, -2},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-32.48),
        iar2 = new int[] {-3248, -2},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(1054.468),
        iar2 = new int[] {1054468, -3},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-1054.468),
        iar2 = new int[] {-1054468, -3},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(1234567898765.0),
        iar2 = new int[] {12345679, 5},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-1234567898765.0),
        iar2 = new int[] {-12345679, 5},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(3.248e-9),
        iar2 = new int[] {3248, -12},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-3.248e-9),
        iar2 = new int[] {-3248, -12},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(3.248e12),
        iar2 = new int[] {3248, 9},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-3.248e12),
        iar2 = new int[] {-3248, 9},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(6.6260755e-24),
        iar2 = new int[] {66260755, -31},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-6.6260755e-24),
        iar2 = new int[] {-66260755, -31},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(0e-9),
        iar2 = new int[] {0, 0},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-0e-9),
        iar2 = new int[] {0, 0},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(1234e3),
        iar2 = new int[] {1234000, 0},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-1234e3),
        iar2 = new int[] {-1234000, 0},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(1234e4),
        iar2 = new int[] {1234, 4},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);
    Test.ensureEqual(
        iar1 = String2.toRational(-1234e4),
        iar2 = new int[] {-1234, 4},
        "" + iar1[0] + " " + iar1[1] + ", " + iar2[0] + " " + iar2[1]);

    // encodeFileNameSafe
    String2.log("test String2.encodeFileNameSafe");
    Test.ensureEqual(String2.encodeFileNameSafe("1!\ncx \u0fff"), "1x21x0acx78x20xx0fff", "");
    Test.ensureEqual(String2.encodeFileNameSafe(null), "x-1", "");
    Test.ensureEqual(String2.encodeFileNameSafe(""), "x-0", "");
    Test.ensureEqual(
        String2.encodeFileNameSafe("This is really too long for a file name and so is shortened."),
        "Thisx20isx20reallyx20toox20xhec4d_3575_a772",
        "");
    Test.ensureEqual(String2.encodeFileNameSafe("1"), "1", "");
    Test.ensureEqual(String2.encodeFileNameSafe("."), ".", "");
    Test.ensureEqual(String2.encodeFileNameSafe("aBc"), "aBc", "");
    Test.ensureEqual(
        String2.encodeFileNameSafe("-._ abxA&°\u1234"), "-._x20abx78Ax26xb0xx1234", "");
    Test.ensureEqual(
        String2.encodeFileNameSafe("ThisIsReallyLongThisIsReallyLongThisIsReallyLong"),
        "ThisIsReallyLongThisIsReaxhdce7_15a4_56ff",
        "");

    // encodeMatlabNameSafe
    String2.log("test String2.encodeMatlabNameSafe");
    Test.ensureEqual(String2.encodeMatlabNameSafe(null), "x_1", "");
    Test.ensureEqual(String2.encodeMatlabNameSafe(""), "x_0", "");
    Test.ensureEqual(String2.encodeMatlabNameSafe("1z"), "x31z", "");
    Test.ensureEqual(String2.encodeMatlabNameSafe(".z"), "x2ez", "");
    Test.ensureEqual(String2.encodeMatlabNameSafe("aBc"), "aBc", "");
    Test.ensureEqual(String2.encodeMatlabNameSafe("maxLength"), "max78Length", "");
    Test.ensureEqual(String2.encodeMatlabNameSafe("max_length"), "max78_length", "");
    Test.ensureEqual(String2.encodeMatlabNameSafe("max!length"), "max78x21length", "");
    Test.ensureEqual(
        String2.encodeMatlabNameSafe("1-._ xA&°\u1234"), "x31x2dx2e_x20x78Ax26xb0xx1234", "");
    Test.ensureEqual(
        String2.encodeMatlabNameSafe("ThisIsReallyLongThisIsReallyLongThisIsReallyLong"),
        "ThisIsReallyLongThisIsReaxhdce7_15a4_56ff",
        "");

    // toTitleCase
    String2.log("test String2.toTitleCase");
    Test.ensureEqual(String2.toTitleCase("abc def (ghi) ;jk"), "Abc Def (Ghi) ;Jk", "");
    Test.ensureEqual(String2.toTitleCase("ABC DEF (GHI) ;JK"), "Abc Def (Ghi) ;Jk", "");
    Test.ensureEqual(String2.toTitleCase(" abc def (ghi) ;jk"), " Abc Def (Ghi) ;Jk", "");
    Test.ensureEqual(String2.toTitleCase(" ABC DEF (GHI) ;JK"), " Abc Def (Ghi) ;Jk", "");
    Test.ensureEqual(String2.toTitleCase(null), null, "");
    Test.ensureEqual(String2.toTitleCase(""), "", "");
    Test.ensureEqual(String2.toTitleCase("a"), "A", "");
    Test.ensureEqual(String2.toTitleCase("a b"), "A B", "");

    // camelCaseToTitleCase
    String2.log("test String2.camelCaseToTitleCase");
    Test.ensureEqual(
        String2.camelCaseToTitleCase("camelCase9.9String"), "Camel Case 9.9 String", "");

    // toVariableName
    String2.log("test String2.toVariableName");
    Test.ensureEqual(String2.toVariableName("abc dÉf (ghi) ;jk"), "abcDefGhiJk", "");
    Test.ensureEqual(String2.toVariableName("ABÇ DEF (GHI) ;JK"), "abcDefGhiJk", "");
    Test.ensureEqual(String2.toVariableName(" abc def (ghi) _;jk"), "abcDefGhiJk", "");
    Test.ensureEqual(String2.toVariableName(" 2BC DEF (GHI) _;JK"), "a2BcDefGhiJk", "");
    Test.ensureEqual(String2.toVariableName(null), "null", "");
    Test.ensureEqual(String2.toVariableName(""), "a", "");
    Test.ensureEqual(String2.toVariableName(" ?"), "a", "");
    Test.ensureEqual(String2.toVariableName("a b"), "aB", "");
    Test.ensureEqual(String2.toVariableName("a"), "a", "");
    Test.ensureEqual(String2.toVariableName("a"), "a", "");
    Test.ensureEqual(String2.toVariableName("_"), "a", "");

    // whichPrefix
    sar = new String[] {"ab", "c", "d"};
    Test.ensureEqual(String2.whichPrefix(sar, "ab", 0), 0, "");
    Test.ensureEqual(String2.whichPrefix(sar, "cat", 0), 1, "");
    Test.ensureEqual(String2.whichPrefix(sar, "dog", 0), 2, "");
    Test.ensureEqual(String2.whichPrefix(sar, "zab", 0), -1, "");
    Test.ensureEqual(String2.whichPrefix(sar, "", 0), -1, "");
    Test.ensureEqual(String2.whichPrefix(sar, null, 0), -1, "");

    // whichSuffix
    Test.ensureEqual(String2.whichSuffix(sar, "ab", 0), 0, "");
    Test.ensureEqual(String2.whichSuffix(sar, "atc", 0), 1, "");
    Test.ensureEqual(String2.whichSuffix(sar, "ogd", 0), 2, "");
    Test.ensureEqual(String2.whichSuffix(sar, "abz", 0), -1, "");
    Test.ensureEqual(String2.whichSuffix(sar, "", 0), -1, "");
    Test.ensureEqual(String2.whichSuffix(sar, null, 0), -1, "");

    // endsWith(StringBuilder, suffix)
    sb = null;
    Test.ensureTrue(!String2.endsWith(sb, ""), "");
    sb = new StringBuilder();
    Test.ensureTrue(!String2.endsWith(sb, null), "");
    Test.ensureTrue(String2.endsWith(sb, ""), "");
    sb.append("ab");
    Test.ensureTrue(String2.endsWith(sb, ""), "");
    Test.ensureTrue(String2.endsWith(sb, "b"), "");
    Test.ensureTrue(String2.endsWith(sb, "ab"), "");
    Test.ensureTrue(!String2.endsWith(sb, "bb"), "");
    Test.ensureTrue(!String2.endsWith(sb, "cab"), "");

    // addNewlineIfNone
    sb = null;
    Test.ensureEqual(String2.addNewlineIfNone(sb), null, "");
    sb = new StringBuilder();
    Test.ensureEqual(String2.addNewlineIfNone(sb).toString(), "", "");
    sb.append('a');
    Test.ensureEqual(String2.addNewlineIfNone(sb).toString(), "a\n", "");
    Test.ensureEqual(String2.addNewlineIfNone(sb).toString(), "a\n", "");
  }

  @org.junit.jupiter.api.Test
  @DisabledIf(
      value = "java.awt.GraphicsEnvironment#isHeadless",
      disabledReason = "headless environment")
  void testString2Clipboard() throws Throwable {
    // clipboard
    String2.log("Clipboard was: " + String2.getClipboardString());
    String2.setClipboardString("Test String2.setClipboardString.");
    Test.ensureEqual(String2.getClipboardString(), "Test String2.setClipboardString.", "");
  }

  private static double nextEpochSecond() {
    return Math.ceil(System.currentTimeMillis() / 1000.0);
  }

  /**
   * These tests that the Java DateTimeFormatter and Calendar2.parseDateTime parse dateTimeString
   * the same way.
   *
   * @param dts dateTimeString
   * @param dtf dateTimeFormat
   * @param tzs timeZoneString
   * @param eis expected ISO 8601 string
   * @throws RuntimeException if trouble
   */
  private static void testDateTimeFormatters(String dts, String dtf, String tzs, String eis) {
    DateTimeFormatter dtfr = Calendar2.makeDateTimeFormatter(dtf, tzs);
    String s =
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.parseToEpochSecondsViaBuggyDateTimeFormatter(dts, dtfr));
    Test.ensureEqual(
        s, eis, "viaBuggyOfficialJavaDateTimeFormatter: " + dts + "  " + dtf + "  " + tzs);
    s = Calendar2.epochSecondsToIsoStringT3Z(Calendar2.parseToEpochSeconds(dts, dtf, tzs));
    Test.ensureEqual(s, eis, "via my parseDateTime: " + dts + "  " + dtf + "  " + tzs);
  }

  /** Do the interactive tests of the methods in Calendar2. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void interactiveTestCalendar2() throws Exception {
    String2.log("\n*** TestUtil.interactiveTestCalendar2");

    String2.log("current time local: " + Calendar2.getCurrentISODateTimeStringLocalTZ());
    String2.pressEnterToContinue();

    try {
      Test.ensureEqual(
          Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy-MMM-dd")),
          "1970-Jan-01",
          ""); // MMM -> Jan ! Really should be done by LLL?
    } catch (Exception e) {
      String2.pressEnterToContinue(
          MustBe.throwableToString(e)
              + "\nOracle Java versus AdoptOpenJDK may be slightly different for these.");
    }

    String2.pressEnterToContinue(
        "getCurrentISODateTimeString = "
            + Calendar2.formatAsISODateTimeT(Calendar2.newGCalendarLocal())
            + "    *** Check that HOUR accounts for daylight saving time!\n\n"
            + "current UTC time = "
            + Calendar2.formatAsISODateTimeT(Calendar2.newGCalendarZulu())
            + "\n"
            + "    *** Check that HOUR is local+7 in summer (DST), local+8 in winter (in California)\n"
            + "                        or local+4 in summer (DST), local+5 in winter (on East Coast).\n"
            + "    See current UTC time at http://www.xav.com/time.cgi");
  }

  /** Test the methods in Calendar2. */
  @org.junit.jupiter.api.Test
  void testCalendar2() throws Throwable {
    String2.log("\n*** TestUtil.testCalendar2");
    String s, expected;
    double d;
    GregorianCalendar gc;
    DateTimeFormatter dtfr;
    Instant instant;

    /*
     * String tests[] = new String[]{"2020-05-21T00:00:00Z",
     * "1970-01-01T00:00:00Z",
     * "1600-01-01T00:00:00Z",
     * "1500-01-01T00:00:00Z", //this and below fail because Instant uses proleptic
     * Gregorian Calendar (as if Gregorian was used before 1582)
     * "0001-05-21T00:00:00Z",
     * "-2020-05-21T00:00:00Z"};
     * for (int i = 0; i < tests.length; i++) {
     * gc = Calendar2.parseISODateTimeZulu(tests[i]);
     * instant = Instant.parse(tests[i]); //tried instant.toEpochMilli(),
     * getEpochSecond()
     * Test.ensureEqual(gc.getTimeInMillis(), instant.toEpochMilli(),
     * "equals test fails for " + tests[i]);
     * Test.ensureTrue(gc.getTimeInMillis() % Calendar2.SECONDS_PER_DAY == 0,
     * "mod test fails for " + tests[i]);
     * }
     */

    /*
     * //can double epochSeconds handle nanoseconds? no
     * instant = Instant.parse("2020-05-21T01:02:03.123456789Z");
     * Test.ensureEqual(instant.getEpochSecond(), 1590022923, "");
     * Test.ensureEqual(instant.getNano(), 123456789, "");
     * d = 1590022923.123456789;
     * Test.ensureEqual("" + d, "1590022923.123456789", ""); //no, it is
     * 1.5900229231234567E9 which isn't good enough
     */

    // Can I support super-precise times if I limit micro and nanoseconds to 000?
    String ISO8601T9_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS000000";
    s = "2020-05-21T01:02:03.456000000";
    d = Calendar2.parseToEpochSeconds(s, ISO8601T9_FORMAT);
    Test.ensureEqual(
        Calendar2.format(d, ISO8601T9_FORMAT, null), "2020-05-21T01:02:03.456000000", "");

    testDateTimeFormatters("20151012", "yyyyMMdd", "Zulu", "2015-10-12T00:00:00.000Z");
    testDateTimeFormatters("2015", "yyyy", "Zulu", "2015-01-01T00:00:00.000Z");

    Test.ensureEqual(String2.parseInt("+8"), 8, "");
    Test.ensureEqual(String2.parseInt("+08"), 8, "");

    // timePrecisionToTimeFormat
    Test.ensureEqual(
        Calendar2.timePrecisionToTimeFormat("1970-01-01T00:00:00.000000000Z"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000Z",
        "");
    Test.ensureEqual(
        Calendar2.timePrecisionToTimeFormat("1970-01-01T00:00:00.000000Z"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000Z",
        "");
    Test.ensureEqual(
        Calendar2.timePrecisionToTimeFormat("1970-01-01T00:00:00.000Z"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "");
    Test.ensureEqual(
        Calendar2.timePrecisionToTimeFormat("1970-01-01T00:00:00.0Z"),
        "yyyy-MM-dd'T'HH:mm:ss.SZ",
        "");
    Test.ensureEqual(
        Calendar2.timePrecisionToTimeFormat("1970-01-01T00:00:00Z"), "yyyy-MM-dd'T'HH:mm:ssZ", "");
    Test.ensureEqual(
        Calendar2.timePrecisionToTimeFormat("1970-01-01T00:00Z"), "yyyy-MM-dd'T'HH:mmZ", "");
    Test.ensureEqual(Calendar2.timePrecisionToTimeFormat("1970-01-01T00Z"), "yyyy-MM-dd'T'HHZ", "");
    Test.ensureEqual(Calendar2.timePrecisionToTimeFormat("1970-01-01"), "yyyy-MM-dd", "");
    Test.ensureEqual(Calendar2.timePrecisionToTimeFormat("1970-01"), "yyyy-MM", "");
    Test.ensureEqual(Calendar2.timePrecisionToTimeFormat("1970"), "yyyy-MM-dd'T'HH:mm:ssZ", "");
    Test.ensureEqual(Calendar2.timePrecisionToTimeFormat("zz"), "yyyy-MM-dd'T'HH:mm:ssZ", "");
    Test.ensureEqual(Calendar2.timePrecisionToTimeFormat(null), "yyyy-MM-dd'T'HH:mm:ssZ", "");

    d = Calendar2.isoStringToEpochSeconds("2020-05-22T01:02:03.123000000");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.000000000Z", d, "zz"),
        "2020-05-22T01:02:03.123000000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.000000000", d, "zz"),
        "2020-05-22T01:02:03.123000000",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.00000000Z", d, "zz"),
        "2020-05-22T01:02:03.12300000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.000000Z", d, "zz"),
        "2020-05-22T01:02:03.123000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.00000Z", d, "zz"),
        "2020-05-22T01:02:03.12300Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.00000", d, "zz"),
        "2020-05-22T01:02:03.12300",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.000Z", d, "zz"),
        "2020-05-22T01:02:03.123Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.0Z", d, "zz"),
        "2020-05-22T01:02:03.1Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.0", d, "zz"),
        "2020-05-22T01:02:03.1",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00Z", d, "zz"),
        "2020-05-22T01:02:03Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00Z", d, "zz"),
        "2020-05-22T01:02Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00Z", d, "zz"), "2020-05-22T01Z", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00", d, "zz"), "2020-05-22T01", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01", d, "zz"), "2020-05-22", "");
    Test.ensureEqual(Calendar2.epochSecondsToLimitedIsoStringT("1970-01", d, "zz"), "2020-05", "");
    Test.ensureEqual(Calendar2.epochSecondsToLimitedIsoStringT("1970", d, "zz"), "2020", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00Z", Double.NaN, "zz"), "zz", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT(null, d, "zz"), "2020-05-22T01:02:03Z", "");

    // convertToJavaDateTimeFormat(String s) -> yyyy-MM-dd'T'HH:mm:ssZ
    // y-m-d --> push to Calendar2.parseISODateTime
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("Y-M-D"), "yyyy-MM-dd", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("Y-M-D H:M"), "yyyy-MM-dd' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("Y-M-DTH:M:SZ"), "yyyy-MM-dd'T'HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("y-m-d"), "yyyy-MM-dd", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("y-m-d h:m"), "yyyy-MM-dd' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("y-m-dTh:m:sZ"), "yyyy-MM-dd'T'HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("YY-MM-DD"), "yyyy-MM-dd", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("YY-MM-DD HH:MM"), "yyyy-MM-dd' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("YY-MM-DDTHH:MM:SSZ"), "yyyy-MM-dd'T'HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("yy-mm-dd"), "yyyy-MM-dd", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("yy-mm-dd hh:mm"), "yyyy-MM-dd' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("yy-mm-ddThh:mm:ssZ"), "yyyy-MM-dd'T'HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("YYYY-MM-DD"), "yyyy-MM-dd", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("YYYY-MM-DD HH:MM"), "yyyy-MM-dd' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("YYYY-MM-DDTHH:MM:SSZ"),
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("yyyy-mm-dd"), "yyyy-MM-dd", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("yyyy-mm-dd hh:mm"), "yyyy-MM-dd' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("yyyy-mm-ddThh:mm:ssZ"),
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "");

    // compact
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("YMD"), "yyyyMd", ""); // yyyyMMdd?
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("YMDHM"), "yyyyMdHm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("YMDHMSZ"), "yyyyMdHmsZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("ymd"), "yyyyMd", ""); // yyyyMMdd?
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("ymdhm"), "yyyyMdHm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("ymdhmsZ"), "yyyyMdHmsZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("YYYYMMDD"), "yyyyMMdd", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("YYYYMMDDHHMM"), "yyyyMMddHHmm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("YYYYMMDDHHMMSSZ"), "yyyyMMddHHmmssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("yyyymmdd"), "yyyyMMdd", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("yyyymmddhhmm"), "yyyyMMddHHmm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("yyyymmddhhmmssZ"), "yyyyMMddHHmmssZ", "");

    // m/d/y
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("M/D/Y"), "M/d/yyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("M/D/Y H:M"), "M/d/yyyy' 'H:m", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("M/D/YTH:M:SZ"), "M/d/yyyy'T'H:m:sZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("m/d/y"), "M/d/yyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("m/d/y h:m"), "M/d/yyyy' 'H:m", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("m/d/yTh:m:sZ"), "M/d/yyyy'T'H:m:sZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("MM/DD/YY"), "MM/dd/yy", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("MM/DD/YY HH:MM"), "MM/dd/yy' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("MM/DD/YYTHH:MM:SSZ"), "MM/dd/yy'T'HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("mm/dd/yy"), "MM/dd/yy", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("mm/dd/yy hh:mm"), "MM/dd/yy' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("mm/dd/yyThh:mm:ssZ"), "MM/dd/yy'T'HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("MM/DD/YYYY"), "MM/dd/yyyy", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("MM/DD/YYYY HH:MM"), "MM/dd/yyyy' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("MM/DD/YYYYTHH:MM:SSZ"),
        "MM/dd/yyyy'T'HH:mm:ssZ",
        "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("mm/dd/yyyy"), "MM/dd/yyyy", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("mm/dd/yyyy hh:mm"), "MM/dd/yyyy' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("mm/dd/yyyyThh:mm:ssZ"),
        "MM/dd/yyyy'T'HH:mm:ssZ",
        "");

    // compact
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("MDY"), "Mdyyyy", ""); // MMddyyyy?
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("MDYHM"), "MdyyyyHm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("MDYHMSZ"), "MdyyyyHmsZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("mdy"), "Mdyyyy", ""); // MMddyyyy?
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("mdyhm"), "MdyyyyHm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("mdyhmsZ"), "MdyyyyHmsZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("MMDDYYYY"), "MMddyyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("MMDDYYYYHHMM"), "MMddyyyyHHmm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("MMDDYYYYHHMMSSZ"), "MMddyyyyHHmmssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("mmddyyyy"), "MMddyyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("mmddyyyyhhmm"), "MMddyyyyHHmm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("mmddyyyyhhmmssZ"), "MMddyyyyHHmmssZ", "");

    // d/m/y
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("D/M/Y"), "d/M/yyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("D/M/Y H:M"), "d/M/yyyy' 'H:m", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("D/M/YTH:M:SZ"), "d/M/yyyy'T'H:m:sZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("d/m/y"), "d/M/yyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("d/m/y h:m"), "d/M/yyyy' 'H:m", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("d/m/yTh:m:sZ"), "d/M/yyyy'T'H:m:sZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("DD/MM/YY"), "dd/MM/yy", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("DD/MM/YY HH:MM"), "dd/MM/yy' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("DD/MM/YYTHH:MM:SSZ"), "dd/MM/yy'T'HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("dd/mm/yy"), "dd/MM/yy", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("dd/mm/yy hh:mm"), "dd/MM/yy' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("dd/mm/yyThh:mm:ssZ"), "dd/MM/yy'T'HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("DD/MM/YYYY"), "dd/MM/yyyy", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("DD/MM/YYYY HH:MM"), "dd/MM/yyyy' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("DD/MM/YYYYTHH:MM:SSZ"),
        "dd/MM/yyyy'T'HH:mm:ssZ",
        "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("dd/mm/yyyy"), "dd/MM/yyyy", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("dd/mm/yyyy hh:mm"), "dd/MM/yyyy' 'HH:mm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("dd/mm/yyyyThh:mm:ssZ"),
        "dd/MM/yyyy'T'HH:mm:ssZ",
        "");

    // compact
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("DMY"), "dMyyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("DMYHM"), "dMyyyyHm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("DMYHMSZ"), "dMyyyyHmsZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("dmy"), "dMyyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("dmyhm"), "dMyyyyHm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("dmyhmsZ"), "dMyyyyHmsZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("DDMMYYYY"), "ddMMyyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("DDMMYYYYHHMM"), "ddMMyyyyHHmm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("DDMMYYYYHHMMSSZ"), "ddMMyyyyHHmmssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("ddmmyyyy"), "ddMMyyyy", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("ddmmyyyyhhmm"), "ddMMyyyyHHmm", "");
    Test.ensureEqual(
        Calendar2.convertToJavaDateTimeFormat("ddmmyyyyhhmmssZ"), "ddMMyyyyHHmmssZ", "");

    // just time
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("H:M"), "H:m", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("H:M:SZ"), "H:m:sZ", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("h:m"), "H:m", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("h:m:sZ"), "H:m:sZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("HH:MM"), "HH:mm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("HH:MM:SSZ"), "HH:mm:ssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("hh:mm"), "HH:mm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("hh:mm:ssZ"), "HH:mm:ssZ", "");

    // compact
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("HM"), "Hm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("HMSZ"), "HmsZ", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("hm"), "Hm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("hmsZ"), "HmsZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("HHMM"), "HHmm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("HHMMSSZ"), "HHmmssZ", "");

    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("hhmm"), "HHmm", "");
    Test.ensureEqual(Calendar2.convertToJavaDateTimeFormat("hhmmssZ"), "HHmmssZ", "");

    // isNumericTimeUnits
    Test.ensureTrue(Calendar2.isNumericTimeUnits("HoURs  SInCE  1980-01-01T00:00:00Z"), "");
    Test.ensureTrue(Calendar2.isNumericTimeUnits("daYs SINCE 1-1-1"), "");
    Test.ensureTrue(Calendar2.isNumericTimeUnits("days SINCE -4713-01-01"), "");

    // convert local time (Standard or DST) to UTC
    // IDs are TZ strings from
    // https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
    String2.log(String2.toCSSVString(ZoneId.getAvailableZoneIds()));
    // Most common in the US (with comments in parentheses):
    // US/Hawaii, Pacific/Honolulu (no DST),
    // US/Aleutian, America/Adak,
    // US/Alaska, America/Anchorage,
    // US/Pacific, America/Los_Angeles,
    // US/Mountain, America/Denver (Mountain with DST),
    // US/Arizona, America/Phoenix (Mountain without DST),
    // US/Central, America/Chicago, (but there are exceptions like US/Michigan,
    // US/Indiana-Starke)
    // US/Eastern, America/New_York,
    // Zulu

    // parse iso format
    String2.log("\nparse iso format");
    // zulu time zone
    // In US, DST change has Sunday March 13, 2016
    TimeZone zuluTZ = TimeZone.getTimeZone("Zulu"); // java
    TimeZone pacificTZ = TimeZone.getTimeZone("America/Los_Angeles"); // java
    s = "2016-03-12T00";
    s =
        s
            + " Zulu => "
            + Calendar2.formatAsISODateTimeT(
                Calendar2.parseISODateTime(new GregorianCalendar(zuluTZ), s))
            + "Z";
    String2.log(s);
    Test.ensureEqual(s, "2016-03-12T00 Zulu => 2016-03-12T00:00:00Z", "");

    s = "2016-03-14T00";
    s =
        s
            + " Zulu => "
            + Calendar2.formatAsISODateTimeT(
                Calendar2.parseISODateTime(new GregorianCalendar(zuluTZ), s))
            + "Z";
    String2.log(s);
    Test.ensureEqual(s, "2016-03-14T00 Zulu => 2016-03-14T00:00:00Z", "");

    // pacific time zone
    s = "2016-03-12T00";
    gc = Calendar2.parseISODateTime(new GregorianCalendar(pacificTZ), s);
    gc.setTimeZone(zuluTZ);
    s = s + " Pacific => " + Calendar2.formatAsISODateTimeT(gc) + "Z";
    String2.log(s);
    Test.ensureEqual(s, "2016-03-12T00 Pacific => 2016-03-12T08:00:00Z", "");

    s = "2016-03-14T00";
    gc = Calendar2.parseISODateTime(new GregorianCalendar(pacificTZ), s);
    gc.setTimeZone(zuluTZ);
    s = s + " Pacific => " + Calendar2.formatAsISODateTimeT(gc) + "Z";
    String2.log(s);
    Test.ensureEqual(s, "2016-03-14T00 Pacific => 2016-03-14T07:00:00Z", "");

    // parse
    String2.log("\nparse time format (was java.time, was joda)");
    // zulu time zone
    // In US, DST change has Sunday March 13, 2016
    String zulu = Calendar2.zulu;
    String pacific = "America/Los_Angeles";

    // zulu
    testDateTimeFormatters("3/12/2016", "M/d/yyyy", zulu, "2016-03-12T00:00:00.000Z");
    testDateTimeFormatters("3/14/2016", "M/d/yyyy", zulu, "2016-03-14T00:00:00.000Z");

    // pacific time zone
    testDateTimeFormatters("3/12/2016", "M/d/yyyy", pacific, "2016-03-12T08:00:00.000Z");
    testDateTimeFormatters("3/14/2016", "M/d/yyyy", pacific, "2016-03-14T07:00:00.000Z");

    Test.ensureTrue(Calendar2.isIsoDate("1234-01-01T00"), "");
    Test.ensureTrue(Calendar2.isIsoDate("0000-0"), "");
    Test.ensureTrue(Calendar2.isIsoDate("-9999-9"), "");
    Test.ensureTrue(Calendar2.isIsoDate("-0001-01"), "");

    Test.ensureTrue(!Calendar2.isIsoDate("2016-112"), ""); // uuuu-DDD
    Test.ensureTrue(!Calendar2.isIsoDate("a1234-01-01T00"), "");
    Test.ensureTrue(!Calendar2.isIsoDate("1234=01-01T00"), "");
    Test.ensureTrue(!Calendar2.isIsoDate("1234e2"), "");
    Test.ensureTrue(!Calendar2.isIsoDate("123-01-01T00"), "");
    Test.ensureTrue(!Calendar2.isIsoDate("12a4-01-01T00"), "");
    Test.ensureTrue(!Calendar2.isIsoDate("+1234-01-01T00"), "");
    Test.ensureTrue(!Calendar2.isIsoDate("1234-"), "");
    Test.ensureTrue(!Calendar2.isIsoDate("-0001"), "");

    // test year 0000 manipulations
    int ymdhmsmom[];
    gc = new GregorianCalendar(Calendar2.zuluTimeZone);
    gc.set(1970, 0, 1, 0, 0, 0);
    gc.set(Calendar2.YEAR, 0);
    gc.set(Calendar2.DAY_OF_YEAR, 5);
    gc.get(Calendar2.MONTH); // force recalculations
    // epSec = Calendar2.gcToEpochSeconds(gc);
    s = Calendar2.epochSecondsToIsoStringTZ(Calendar2.gcToEpochSeconds(gc));
    Test.ensureEqual(s, "0000-01-05T00:00:00Z", "");

    // test how Java DateTimeFormatter works by seeing what it writes out
    // M is for numeric Month, L is for text Month
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
        "1970-01-01T00:00:00.000Z",
        "");
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")),
        "1970-01-01T00:00:00.000+0000",
        ""); // !
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy-MM-dd")), "1970-01-01", "");
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy-MMMM-dd")),
        "1970-January-01",
        ""); // MMMM -> January ! Really should be done by LLLL?
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy-LL-dd")),
        "1970-01-01",
        ""); // LL -> 01 ! Really should be done by MM
    // 2020-05-12 known difference between Oracle and AdoptOpenJDK. Doesn't really
    // affect me.
    // Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern(
    // "yyyy-LLL-dd")), "1970-Jan-01", ""); //oracle java LLL->Jan adoptOpenJDK8->1
    // !
    // Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern(
    // "yyyy-LLLL-dd")), "1970-January-01",
    // "This is a KNOWN DIFFERENCE between Oracle and AdoptOpenJDK. It doesn't
    // affect ERDDAP.");

    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy QQQQ")), "1970 1st quarter", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy QQQ")), "1970 Q1", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy QQ")), "1970 01", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy Q")), "1970 1", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy qq")), "1970 01", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy q")), "1970 1", "");
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy EEEEE")),
        "1970 T",
        ""); // ! the "narrow" form
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy EEEE")), "1970 Thursday", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy EEE")), "1970 Thu", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy EE")), "1970 Thu", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy E")), "1970 Thu", "");
    // do lots of 0's parse without error to 0?
    Test.ensureEqual(String2.parseInt("000000000"), 0, ""); // e.g. nano of day
    // Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern(
    // "yyyy aa")), "1970 AM", ""); //error: too many pattern letters
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy a")), "1970 AM", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy hh")), "1970 12", "");
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy h")), "1970 12", ""); // clock hour
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy KK")),
        "1970 00",
        ""); // hour of am pm
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy K")), "1970 0", ""); // hour of am pm
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy xxx")), "1970 +00:00", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy xx")), "1970 +0000", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy x")), "1970 +00", "");
    Test.ensureEqual(
        Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy XXX")),
        "1970 Z",
        ""); // like x, but Z if 00
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy XX")), "1970 Z", "");
    Test.ensureEqual(Calendar2.format(0, DateTimeFormatter.ofPattern("yyyy X")), "1970 Z", "");

    Test.ensureEqual(
        Calendar2.format(62.003, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", null),
        "1970-01-01T00:01:02.003Z",
        "");
    Test.ensureEqual(Calendar2.format(62.003, null, null), "1970-01-01T00:01:02Z", "");
    Test.ensureEqual(
        Calendar2.format(Long.MAX_VALUE / 1000.0, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", null), "", "");
    Test.ensureEqual(Calendar2.format(Double.NaN, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", null), "", "");

    // tryToIsoStringZ(someDateTimeString)
    Test.ensureEqual(
        Calendar2.tryToIsoString(" 2 Jan 1985  "), "1985-01-02", ""); // removes time if unused
    Test.ensureEqual(Calendar2.tryToIsoString("1985-01-02T01:02:34Z"), "1985-01-02T01:02:34Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-01-02Z"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-1-2Z"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-1-2T1:2:3Z"), "1985-01-02T01:02:03Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-1-2T1:2:3"), "1985-01-02T01:02:03Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-1-2 1:2"), "1985-01-02T01:02:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("15Z9dec2013"), "2013-12-09T15:00:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 UTC"), "9985-01-02T23:59:59.999Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1-1-1 00:00:0.0Z"), "0001-01-01T00:00:00.000Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1-1-1 00:00:0Z"), "0001-01-01T00:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("20171221T023531Z"), "2017-12-21T02:35:31Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.987000000Z"),
        "9985-01-02T23:59:59.987000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.100Z"), "9985-01-02T23:59:59.100Z", "");
    // test to consider supporting SSSSSSSSS
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02T23:59:59.987654321Z"),
    // "9985-01-02T23:59:59.987Z", ""); //it trunc's, not rounds

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("2017-12-21T023531"), "yyyy-MM-dd'T'HHmmss", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2017-12-21T023531"), "2017-12-21T02:35:31Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2017-12-21T023531Z"), "2017-12-21T02:35:31Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2017-12-21T0235"), "2017-12-21T02:35:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2017-12-21T0235Z"), "2017-12-21T02:35:00Z", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sun, 20 Nov 1994 08:49:37 -0800"),
        "EEE, d MMM yyyy HH:mm:ss xx",
        "");
    dtfr = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss xx");
    dtfr.parse("Sun, 20 Nov 1994 08:49:37 -0800");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun, 20 Nov 1994 08:49:37 -0800"), "1994-11-20T16:49:37Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("sun, 06 nov 1994 08:49:37 -0800"), "1994-11-06T16:49:37Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("SUN, 6 NOV 1994 08:49:37 -0800"), "1994-11-06T16:49:37Z", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("Fri Jun 5 2:22:20 2020"), "2020-06-05T02:22:20Z", ""); // variable
    // nDigits
    Test.ensureEqual(
        Calendar2.tryToIsoString("Fri Jun  5  2:22:20 2020"),
        "2020-06-05T02:22:20Z",
        ""); // 2 digits
    // (first
    // may be '
    // ')
    Test.ensureEqual(
        Calendar2.tryToIsoString("Fri Jun 15 10:22:20 2020"), "2020-06-15T10:22:20Z", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Wed Dec 20 17:36:16 2017"),
        "EEE MMM dd HH:mm:ss yyyy",
        "");
    dtfr = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy");
    dtfr.parse("Wed Dec 20 17:36:16 2017");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Wed Dec 20 17:36:16 2017"), "2017-12-20T17:36:16Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("WED DEC 06 17:36:16 2017"), "2017-12-06T17:36:16Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("wed dec 6 17:36:16 2017"), "2017-12-06T17:36:16Z", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("1994-11-20T08:49:37-0800"), "yyyy-MM-dd'T'HH:mm:ssxx", "");
    dtfr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxx");
    dtfr.parse("1994-11-20T08:49:37-0800");
    Test.ensureEqual(
        Calendar2.tryToIsoString("1994-11-20T08:49:37-0800"), "1994-11-20T16:49:37Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("1994-11-20T08:49:37-0830"), "1994-11-20T17:19:37Z", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("1858-11-17 00:00:00 +0:00"),
        "yyyy-MM-dd HH:mm:ss xxx",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("1858-11-17 00:00:00 +0:00"), "1858-11-17T00:00:00Z", "");

    // suggestDateTimeFormat and tryToIsoString
    Test.ensureEqual(Calendar2.suggestDateTimeFormat((String) null), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat(""), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("zztop"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985"), "", "");

    String2.log("\nlots of expected error messages...");
    Test.ensureEqual(Calendar2.tryToIsoString((String) null), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString(""), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("zztop"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985"), "", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2016-035"), "yyyy-DDD", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2999-366"), "yyyy-DDD", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000-001"), "yyyy-DDD", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9999-399"), "yyyy-DDD", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-4713-001"), "yyyy-DDD", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985-400"), "", "");

    // Tests that give Java DateTimeFormatter trouble are marked with /**/
    Test.ensureEqual(Calendar2.tryToIsoString("2016-035"), "2016-02-04", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2999-366"), "3000-01-01", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("0000-001"), "0000-01-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9999-399"), "10000-02-03", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("-4713-001"), "-4713-01-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-400"), "", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2016035"), "yyyyDDD", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000001"), "yyyyDDD", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2999366"), "yyyyDDD", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2999399"), "yyyyDDD", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("3000034"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985400"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-4713400"), "", "");

    Test.ensureEqual(Calendar2.tryToIsoString("2016035"), "2016-02-04", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("0000001"), "0000-01-01", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("2999366"), "3000-01-01", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("2999399"), "3000-02-03", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("3000034"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.tryToIsoString("1985400"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713400"), "", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.001000000 UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000 GMT"),
        "yyyy-MM-dd HH:mm:ss.SSS000000 'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.000000000UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000000'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000GMT"),
        "yyyy-MM-dd HH:mm:ss.SSS000000'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000-08:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000-08:30"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000-0800"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000-0830"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000-08"),
        "yyyy-MM-dd HH:mm:ss.SSS000000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000+08:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000+8:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000+0800"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000+800"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000+08"),
        "yyyy-MM-dd HH:mm:ss.SSS000000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000 08:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000 8:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000 0800"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000 800"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000 08"),
        "yyyy-MM-dd HH:mm:ss.SSS000000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000Z"),
        "yyyy-MM-dd HH:mm:ss.SSS000000'Z'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000"),
        "yyyy-MM-dd HH:mm:ss.SSS000000",
        "");
    // Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02
    // 23:59:59.99900000"), "yyyy-MM-dd HH:mm:ss.SSS000000", "");
    // Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02
    // 23:59:59.9990000"), "yyyy-MM-dd HH:mm:ss.SSS000000", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.001000 UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000 GMT"),
        "yyyy-MM-dd HH:mm:ss.SSS000 'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.000000UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000GMT"),
        "yyyy-MM-dd HH:mm:ss.SSS000'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000-08:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000-08:30"),
        "yyyy-MM-dd HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000-0800"),
        "yyyy-MM-dd HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000-0830"),
        "yyyy-MM-dd HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000-08"),
        "yyyy-MM-dd HH:mm:ss.SSS000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000+08:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000+8:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000+0800"),
        "yyyy-MM-dd HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000+800"),
        "yyyy-MM-dd HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000+08"),
        "yyyy-MM-dd HH:mm:ss.SSS000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000 08:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000 8:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000 0800"),
        "yyyy-MM-dd HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000 800"),
        "yyyy-MM-dd HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000 08"),
        "yyyy-MM-dd HH:mm:ss.SSS000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000Z"),
        "yyyy-MM-dd HH:mm:ss.SSS000'Z'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000"),
        "yyyy-MM-dd HH:mm:ss.SSS000",
        "");
    // Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02
    // 23:59:59.99900"), "yyyy-MM-dd HH:mm:ss.SSS000", "");
    // Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.9990"),
    // "yyyy-MM-dd HH:mm:ss.SSS000", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.001 UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999 GMT"),
        "yyyy-MM-dd HH:mm:ss.SSS 'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.000UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999GMT"),
        "yyyy-MM-dd HH:mm:ss.SSS'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999-08:00"),
        "yyyy-MM-dd HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999-08:30"),
        "yyyy-MM-dd HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999-0800"),
        "yyyy-MM-dd HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999-0830"),
        "yyyy-MM-dd HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999-08"),
        "yyyy-MM-dd HH:mm:ss.SSSx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999+08:00"),
        "yyyy-MM-dd HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999+8:00"),
        "yyyy-MM-dd HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999+0800"),
        "yyyy-MM-dd HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999+800"),
        "yyyy-MM-dd HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999+08"),
        "yyyy-MM-dd HH:mm:ss.SSSx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999 08:00"),
        "yyyy-MM-dd HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999 8:00"),
        "yyyy-MM-dd HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999 0800"),
        "yyyy-MM-dd HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999 800"),
        "yyyy-MM-dd HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999 08"),
        "yyyy-MM-dd HH:mm:ss.SSSx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999Z"),
        "yyyy-MM-dd HH:mm:ss.SSS'Z'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999"), "yyyy-MM-dd HH:mm:ss.SSS", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59-08:00"), "yyyy-MM-dd HH:mm:ssxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59"), "yyyy-MM-dd HH:mm:ss", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02-23:59:59"), "yyyy-MM-dd-HH:mm:ss", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02:23:59:59"), "yyyy-MM-dd:HH:mm:ss", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23:59"), "yyyy-MM-dd HH:mm", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23 UTC"), "yyyy-MM-dd HH 'UTC'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23 GMT"), "yyyy-MM-dd HH 'GMT'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23UTC"), "yyyy-MM-dd HH'UTC'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23GMT"), "yyyy-MM-dd HH'GMT'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23-08:00"), "yyyy-MM-dd HHxxx", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23-0800"), "yyyy-MM-dd HHxx", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23Z"), "yyyy-MM-dd HH'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23"), "yyyy-MM-dd HH", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 UTC"), "yyyy-MM-dd 'UTC'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 GMT"), "yyyy-MM-dd 'GMT'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02UTC"), "yyyy-MM-dd'UTC'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02GMT"), "yyyy-MM-dd'GMT'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02Z"), "yyyy-MM-dd'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02"), "yyyy-MM-dd", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000-01"), "yyyy-MM", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9999-19"), "yyyy-MM", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02-08:00"), "yyyy-MM-dd-HH:mm", ""); // ambiguous
    // (time or
    // timezone).
    // treat as time
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02-08"),
        "yyyy-MM-dd-HH",
        ""); // ambiguous (time or
    // timezone). treat as
    // time
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02-0800"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985-20"), "", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-1009-01-02 23:59:59.999Z"),
        "yyyy-MM-dd HH:mm:ss.SSS'Z'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("0000-01-02 23:59:59.999Z"),
        "yyyy-MM-dd HH:mm:ss.SSS'Z'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("2010-01-19 00:00:00-1:00"), "yyyy-MM-dd HH:mm:ssxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("2010-01-19 00:00:00+1:00"), "yyyy-MM-dd HH:mm:ssxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("2010-01-19 00:00:00 1:00"), "yyyy-MM-dd HH:mm:ssxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("2010-01-19 00:00:00-100"), "yyyy-MM-dd HH:mm:ssxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("2010-01-19 00:00:00+100"), "yyyy-MM-dd HH:mm:ssxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("2010-01-19 00:00:00 100"), "yyyy-MM-dd HH:mm:ssxx", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.001000000 UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.001000000 UTC"),
        "9985-01-02T23:59:59.001000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 GMT"),
        "9985-01-02T23:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.000000000UTC"),
        "9985-01-02T23:59:59.000000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000GMT"),
        "9985-01-02T23:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000-08:00"),
        "9985-01-03T07:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000-8:00"),
        "9985-01-03T07:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000-08:30"),
        "9985-01-03T08:29:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000-0800"),
        "9985-01-03T07:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000-0830"),
        "9985-01-03T08:29:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000-08"),
        "9985-01-03T07:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000+08:00"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000+8:00"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000+0800"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000+800"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000-0030"),
        "9985-01-03T00:29:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000+0030"),
        "9985-01-02T23:29:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000+08"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000 08:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 08:00"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 8:00"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 0800"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 800"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 0030"),
        "9985-01-02T23:29:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 030"),
        "9985-01-02T23:29:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 08"),
        "9985-01-02T15:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000Z"),
        "9985-01-02T23:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000"),
        "9985-01-02T23:59:59.999000000Z",
        "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.001000 UTC"),
        "9985-01-02T23:59:59.001000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 GMT"),
        "9985-01-02T23:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.000000UTC"),
        "9985-01-02T23:59:59.000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000GMT"),
        "9985-01-02T23:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000-08:00"),
        "9985-01-03T07:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000-8:00"),
        "9985-01-03T07:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000-08:30"),
        "9985-01-03T08:29:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000-0800"),
        "9985-01-03T07:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000-0830"),
        "9985-01-03T08:29:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000-08"),
        "9985-01-03T07:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000+08:00"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000+8:00"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000+0800"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000+800"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000-0030"),
        "9985-01-03T00:29:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000+0030"),
        "9985-01-02T23:29:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000+08"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000 08:00"),
        "yyyy-MM-dd HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 08:00"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 8:00"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 0800"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 800"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 0030"),
        "9985-01-02T23:29:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 030"),
        "9985-01-02T23:29:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 08"),
        "9985-01-02T15:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000Z"), "9985-01-02T23:59:59.999000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000"), "9985-01-02T23:59:59.999000Z", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.001 UTC"), "9985-01-02T23:59:59.001Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 GMT"), "9985-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.000UTC"), "9985-01-02T23:59:59.000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999GMT"), "9985-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999-08:00"), "9985-01-03T07:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999-8:00"), "9985-01-03T07:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999-08:30"), "9985-01-03T08:29:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999-0800"), "9985-01-03T07:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999-0830"), "9985-01-03T08:29:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999-08"), "9985-01-03T07:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999+08:00"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999+8:00"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999+0800"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999+800"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999-0030"), "9985-01-03T00:29:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999+0030"), "9985-01-02T23:29:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999+08"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999 08:00"),
        "yyyy-MM-dd HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 08:00"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 8:00"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 0800"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 800"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 0030"), "9985-01-02T23:29:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 030"), "9985-01-02T23:29:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 08"), "9985-01-02T15:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999Z"), "9985-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999"), "9985-01-02T23:59:59.999Z", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59-08:00"), "9985-01-03T07:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23:59:59"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02-23:59:59"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02:23:59:59"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23:59"), "9985-01-02T23:59:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23UTC"), "9985-01-02T23:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23GMT"), "9985-01-02T23:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23-08:00"), "9985-01-03T07:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23-0800"), "9985-01-03T07:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23-08"), "9985-01-03T07:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23Z"), "9985-01-02T23:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23"), "9985-01-02T23:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02UTC"), "9985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02Z"), "9985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02"), "9985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("0000-01"), "0000-01-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9999-19"), "10000-07-01", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02-08:00"),
        "9985-01-02T08:00:00Z",
        ""); // ambiguous (time
    // or timezone).
    // treat as time
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02-08"),
        "9985-01-02T08:00:00Z",
        ""); // ambiguous (time or
    // timezone). treat as
    // time
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02-0800"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-20"), "", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-1009-01-02 23:59:59.999Z"), "-1009-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("0000-01-02 23:59:59.000Z"), "0000-01-02T23:59:59.000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00-1:30"), "2010-01-19T01:30:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00+1:30"), "2010-01-18T22:30:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00 1:30"), "2010-01-18T22:30:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00-130"), "2010-01-19T01:30:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00+130"), "2010-01-18T22:30:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00 130"), "2010-01-18T22:30:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00-01"), "2010-01-19T01:00:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00+01"), "2010-01-18T23:00:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("2010-01-19 00:00:00 01"), "2010-01-18T23:00:00Z", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.999 UTC"), "yyyy-M-d H:m:s.S 'UTC'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.9 GMT"), "yyyy-M-d H:m:s.S 'GMT'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.99UTC"), "yyyy-M-d H:m:s.S'UTC'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.1GMT"), "yyyy-M-d H:m:s.S'GMT'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.01-08:00"), "yyyy-M-d H:m:s.Sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.001+08:00"), "yyyy-M-d H:m:s.Sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.0-0800"), "yyyy-M-d H:m:s.Sxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.00+0800"), "yyyy-M-d H:m:s.Sxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.000-08"), "yyyy-M-d H:m:s.Sx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.999+08"), "yyyy-M-d H:m:s.Sx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.999Z"), "yyyy-M-d H:m:s.S'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9.999"), "yyyy-M-d H:m:s.S", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9-08:00"), "yyyy-M-d H:m:sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9-0800"), "yyyy-M-d H:m:sxx", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9Z"), "yyyy-M-d H:m:s'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2 3:9:9"), "yyyy-M-d H:m:s", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2-3:9:9"), "yyyy-M-d-H:m:s", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2:3:9:9"), "yyyy-M-d:H:m:s", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2 3:9"), "yyyy-M-d H:m", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2 3:9Z"), "yyyy-M-d H:m'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2 3Z"), "yyyy-M-d H'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2 3"), "yyyy-M-d H", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2Z"), "yyyy-M-d'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2"), "yyyy-M-d", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000-1"), "yyyy-M", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985-2"), "yyyy-M", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-1009-1-2 3:9:9"), "yyyy-M-d H:m:s", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000-1-2 3:9:9"), "yyyy-M-d H:m:s", "");

    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9.999000000 UTC"),
    // "9985-01-02T03:09:09.999000000Z", "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9.000000000Z"),
    // "9985-01-02T03:09:09.000000000Z", "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9.9999000000"),
    // "9985-01-02T03:09:10.000000000Z", ""); //test of rounding
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9.999000 UTC"),
    // "9985-01-02T03:09:09.999000Z", "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9.000000Z"),
    // "9985-01-02T03:09:09.000000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.999 UTC"), "9985-01-02T03:09:09.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.99UTC"), "9985-01-02T03:09:09.990Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.9-08:00"), "9985-01-02T11:09:09.900Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.1+08:00"), "9985-01-01T19:09:09.100Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.1 08:00"), "9985-01-01T19:09:09.100Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.01-08:30"), "9985-01-02T11:39:09.010Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.01 08:30"), "9985-01-01T18:39:09.010Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.001+08:30"), "9985-01-01T18:39:09.001Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.001 08:30"), "9985-01-01T18:39:09.001Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.0-0800"), "9985-01-02T11:09:09.000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.00+0800"), "9985-01-01T19:09:09.000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.0 800"), "9985-01-01T19:09:09.000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.00 800"), "9985-01-01T19:09:09.000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2 3:9:9.000Z"), "9985-01-02T03:09:09.000Z", "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9.9999"),
    // "9985-01-02T03:09:10.000Z", ""); //test of rounding
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9-08:00"), "9985-01-02T11:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9-0800"), "9985-01-02T11:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9 08:00"), "9985-01-01T19:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9 0800"), "9985-01-01T19:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9 8:00"), "9985-01-01T19:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9 800"), "9985-01-01T19:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9Z"), "9985-01-02T03:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9:9"), "9985-01-02T03:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2-3:9:9"), "9985-01-02T03:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2:3:9:9"), "9985-01-02T03:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9"), "9985-01-02T03:09:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3:9Z"), "9985-01-02T03:09:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2 3"), "9985-01-02T03:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2Z"), "9985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2"), "9985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("0000-1"), "0000-01-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-2"), "1985-02-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-1009-1-2 3:9:9"), "-1009-01-02T03:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("0000-1-2 3:9:9"), "0000-01-02T03:09:09Z", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000 UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000-08:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000-8:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000-0830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000-830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000-08"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000 08:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000 8:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000 0830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000 830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000 08"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000Z"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000'Z'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000000"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000",
        "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000 UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000-08:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000-8:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000-0830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000-830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000-08"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000 08:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000 8:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000 0830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000 830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000 08"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000Z"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000'Z'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999000"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000",
        "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999 UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999-08:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999-8:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999-0830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999-830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999-08"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999 08:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999 8:00"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999 0830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999 830"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999 08"),
        "yyyy-MM-dd'T'HH:mm:ss.SSSx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999Z"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59.999"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59-00:30"),
        "yyyy-MM-dd'T'HH:mm:ssxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59+0030"), "yyyy-MM-dd'T'HH:mm:ssxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59 UTC"),
        "yyyy-MM-dd'T'HH:mm:ss 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59:59UTC"),
        "yyyy-MM-dd'T'HH:mm:ss'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23:59Z"), "yyyy-MM-dd'T'HH:mm'Z'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02T23-0800"), "yyyy-MM-dd'T'HHxx", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02Z"), "yyyy-MM-dd'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02"), "yyyy-MM-dd", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000-01"), "yyyy-MM", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9999-19"), "yyyy-MM", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985-20"), "", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-1009-01-02T23:59Z"), "yyyy-MM-dd'T'HH:mm'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-1010-01-02"), "yyyy-MM-dd", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("0000-01-02T23:59Z"), "yyyy-MM-dd'T'HH:mm'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000-01-02"), "yyyy-MM-dd", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999000000 UTC"),
        "9985-01-02T23:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.001000000-08:00"),
        "9985-01-03T07:59:59.001000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.001000000-0830"),
        "9985-01-03T08:29:59.001000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.000000000-08"),
        "9985-01-03T07:59:59.000000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999000000Z"),
        "9985-01-02T23:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999000000"),
        "9985-01-02T23:59:59.999000000Z",
        "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999000 UTC"),
        "9985-01-02T23:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.001000-08:00"),
        "9985-01-03T07:59:59.001000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.001000-0830"),
        "9985-01-03T08:29:59.001000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.000000-08"),
        "9985-01-03T07:59:59.000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999000Z"), "9985-01-02T23:59:59.999000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999000"), "9985-01-02T23:59:59.999000Z", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999 UTC"), "9985-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.010-08:00"), "9985-01-03T07:59:59.010Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.001-0830"), "9985-01-03T08:29:59.001Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.000-08"), "9985-01-03T07:59:59.000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999Z"), "9985-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59.999"), "9985-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59-08:30"), "9985-01-03T08:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59+08:30"), "9985-01-02T15:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59 08:30"), "9985-01-02T15:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59-8:30"), "9985-01-03T08:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59+8:30"), "9985-01-02T15:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59 8:30"), "9985-01-02T15:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59-0830"), "9985-01-03T08:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59+0830"), "9985-01-02T15:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59 0830"), "9985-01-02T15:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59-830"), "9985-01-03T08:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59+830"), "9985-01-02T15:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59 830"), "9985-01-02T15:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59-08"), "9985-01-03T07:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59+08"), "9985-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59 08"), "9985-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59 UTC"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02T23:59:59 GMT"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02T23:59Z"), "9985-01-02T23:59:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02T23-0800"), "9985-01-03T07:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02Z"), "9985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02"), "9985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("0000-01"), "0000-01-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9999-19"), "10000-07-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-20"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-1009-01-02T23:59Z"), "-1009-01-02T23:59:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-1010-01-02"), "-1010-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("0000-01-02T23:59Z"), "0000-01-02T23:59:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("0000-01-02"), "0000-01-02", "");

    // Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9.999500
    // UTC"),"yyyy-M-d'T'H:m:s.S 'UTC'", ""); //test 6 digits
    // Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9.999500UTC"),
    // "yyyy-M-d'T'H:m:s.S'UTC'", ""); //test 6 digits
    // Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9.9995-08:00"),"yyyy-M-d'T'H:m:s.Sxxx",
    // "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9.999-0800"), "yyyy-M-d'T'H:m:s.Sxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9.999Z"), "yyyy-M-d'T'H:m:s.S'Z'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9.999"), "yyyy-M-d'T'H:m:s.S", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9+00:30"), "yyyy-M-d'T'H:m:sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9-00:30"), "yyyy-M-d'T'H:m:sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9 00:30"), "yyyy-M-d'T'H:m:sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9+0:30"), "yyyy-M-d'T'H:m:sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9-0:30"), "yyyy-M-d'T'H:m:sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9 0:30"), "yyyy-M-d'T'H:m:sxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9+0030"), "yyyy-M-d'T'H:m:sxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9-0030"), "yyyy-M-d'T'H:m:sxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9 0030"), "yyyy-M-d'T'H:m:sxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9+030"), "yyyy-M-d'T'H:m:sxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9-030"), "yyyy-M-d'T'H:m:sxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9 030"), "yyyy-M-d'T'H:m:sxx", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9Z"), "yyyy-M-d'T'H:m:s'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2T3:9:9"), "yyyy-M-d'T'H:m:s", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2T3:9"), "yyyy-M-d'T'H:m", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2T3:9Z"), "yyyy-M-d'T'H:m'Z'", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2T3"), "yyyy-M-d'T'H", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-1-2"), "yyyy-M-d", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000-1"), "yyyy-M", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985-2"), "yyyy-M", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-1009-1-2T3:9:9"), "yyyy-M-d'T'H:m:s", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000-1-2T3:9:9"), "yyyy-M-d'T'H:m:s", "");

    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.999000000500
    // UTC"),"9985-01-02T03:09:10.000000000Z", ""); //test rounding.
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.9990000005-08:00"),"9985-01-02T11:09:10.000000000Z",
    // "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.999000000-0800"),
    // "9985-01-02T11:09:09.999000000Z", "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.000000000Z"),
    // "9985-01-02T03:09:09.000000000Z", "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.00100000"),
    // "9985-01-02T03:09:09.001000000Z", "");

    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.9990005-08:00"),"9985-01-02T11:09:10.000000Z",
    // "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.999000-0800"),
    // "9985-01-02T11:09:09.999000Z", "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.000000Z"),
    // "9985-01-02T03:09:09.000000Z", "");
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.00100"),
    // "9985-01-02T03:09:09.001000Z", "");

    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.999500
    // UTC"),"9985-01-02T03:09:10.000Z", ""); //test rounding.
    // Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.9995-08:00"),"9985-01-02T11:09:10.000Z",
    // "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2T3:9:9.999-0800"), "9985-01-02T11:09:09.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-1-2T3:9:9.000Z"), "9985-01-02T03:09:09.000Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9.01"), "9985-01-02T03:09:09.010Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9+00:30"), "9985-01-02T02:39:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9+0030"), "9985-01-02T02:39:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9 0030"), "9985-01-02T02:39:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9-0030"), "9985-01-02T03:39:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9+030"), "9985-01-02T02:39:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9 030"), "9985-01-02T02:39:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9-030"), "9985-01-02T03:39:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9Z"), "9985-01-02T03:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9:9"), "9985-01-02T03:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9"), "9985-01-02T03:09:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3:9Z"), "9985-01-02T03:09:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2T3"), "9985-01-02T03:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-1-2"), "9985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("0000-1"), "0000-01-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985-2"), "1985-02-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-1009-1-2T3:9:9"), "-1009-01-02T03:09:09Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("0000-1-2T3:9:9"), "0000-01-02T03:09:09Z", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59.999000000 UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59.999000000UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000000'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59.999000 UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59.999000UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS000'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59.999 UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59.999UTC"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59 UTC"),
        "yyyy-MM-dd'T'HH:mm:ss 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59UTC"),
        "yyyy-MM-dd'T'HH:mm:ss'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59-08:00"),
        "yyyy-MM-dd'T'HH:mm:ssxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59-8:00"),
        "yyyy-MM-dd'T'HH:mm:ssxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59-0800"),
        "yyyy-MM-dd'T'HH:mm:ssxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59-800"), "yyyy-MM-dd'T'HH:mm:ssxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59-08"), "yyyy-MM-dd'T'HH:mm:ssx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59 08:00"),
        "yyyy-MM-dd'T'HH:mm:ssxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59 8:00"),
        "yyyy-MM-dd'T'HH:mm:ssxxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59 0800"),
        "yyyy-MM-dd'T'HH:mm:ssxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59 800"), "yyyy-MM-dd'T'HH:mm:ssxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59 08"), "yyyy-MM-dd'T'HH:mm:ssx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59Z"), "yyyy-MM-dd'T'HH:mm:ss'Z'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59:59"), "yyyy-MM-dd'T'HH:mm:ss", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59+00:30"), "yyyy-MM-dd'T'HH:mmxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59-0030"), "yyyy-MM-dd'T'HH:mmxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59+00"), "yyyy-MM-dd'T'HH:mmx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59Z"), "yyyy-MM-dd'T'HH:mm'Z'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-4713-01-02T23:59"), "yyyy-MM-dd'T'HH:mm", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-4713-01-02T23"), "yyyy-MM-dd'T'HH", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-4713-01-02"), "yyyy-MM-dd", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-4713-01"), "yyyy-MM", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59.999 UTC"), "-4713-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(
            Calendar2.parseDateTimeZulu(
                "-4713-01-02T23:59:59.999000 UTC", "yyyy-MM-dd'T'HH:mm:ss.SSS000 'UTC'")),
        "-4713-01-02T23:59:59.999Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59.999000 UTC"),
        "-4713-01-02T23:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59.999000000 UTC"),
        "-4713-01-02T23:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59 UTC"), "-4713-01-02T23:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59-08:00"), "-4713-01-03T07:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59-0800"), "-4713-01-03T07:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59-08"), "-4713-01-03T07:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59-8:00"), "-4713-01-03T07:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59-800"), "-4713-01-03T07:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59+08:00"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59+0800"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59+08"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59+8:00"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59+800"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59 08:00"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59 0800"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59 08"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59 8:00"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59 800"), "-4713-01-02T15:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59:59Z"), "-4713-01-02T23:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713-01-02T23:59:59"), "-4713-01-02T23:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59+00:30"), "-4713-01-02T23:29:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("-4713-01-02T23:59-0030"), "-4713-01-03T00:29:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713-01-02T23:59+00"), "-4713-01-02T23:59:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713-01-02T23:59Z"), "-4713-01-02T23:59:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713-01-02T23:59"), "-4713-01-02T23:59:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713-01-02T23"), "-4713-01-02T23:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713-01-02"), "-4713-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713-01"), "-4713-01-01", "");

    Test.ensureEqual(Calendar2.tryToIsoString("07-2013"), "2013-07-01", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000 UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000000UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000000'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000 UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999000UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS000'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999 UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.999UTC"),
        "yyyy-MM-dd HH:mm:ss.SSS'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.000"), "yyyy-MM-dd HH:mm:ss.SSS", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59.9"), "yyyy-MM-dd HH:mm:ss.S", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59 UTC"),
        "yyyy-MM-dd HH:mm:ss 'UTC'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59 GMT"),
        "yyyy-MM-dd HH:mm:ss 'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59UTC"), "yyyy-MM-dd HH:mm:ss'UTC'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59GMT"), "yyyy-MM-dd HH:mm:ss'GMT'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59+00:30"), "yyyy-MM-dd HH:mm:ssxxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59-0030"), "yyyy-MM-dd HH:mm:ssxx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59-08"), "yyyy-MM-dd HH:mm:ssx", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59Z"), "yyyy-MM-dd HH:mm:ss'Z'", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("9985-01-02 23:59:59"), "yyyy-MM-dd HH:mm:ss", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23:59"), "yyyy-MM-dd HH:mm", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02 23"), "yyyy-MM-dd HH", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("9985-01-02"), "yyyy-MM-dd", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000000 UTC"),
        "9985-01-02T23:59:59.999000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.000000000"),
        "9985-01-02T23:59:59.000000000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999000 UTC"),
        "9985-01-02T23:59:59.999000Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.000000"), "9985-01-02T23:59:59.000000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.999 UTC"), "9985-01-02T23:59:59.999Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.000"), "9985-01-02T23:59:59.000Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59.9"), "9985-01-02T23:59:59.900Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59UTC"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59GMT"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59+00:30"), "9985-01-02T23:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59-0030"), "9985-01-03T00:29:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("9985-01-02 23:59:59-08"), "9985-01-03T07:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23:59:59Z"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23:59:59"), "9985-01-02T23:59:59Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23:59"), "9985-01-02T23:59:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02 23"), "9985-01-02T23:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("9985-01-02"), "9985-01-02", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("20180208123456"), "yyyyMMddHHmmss", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("00000101000000"), "yyyyMMddHHmmss", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("29991939295959"), "yyyyMMddHHmmss", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("30000102235959"),
        "",
        ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("19852002235959"), "", ""); // invalid month
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("19850942235959"), "", ""); // invalid date
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("19850909335959"), "", ""); // invalid hour
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("19850909296959"), "", ""); // invalid minute
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("19850909295969"), "", ""); // invalid second
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("-47130909295969"), "", ""); // looks too much like a number

    Test.ensureEqual(Calendar2.tryToIsoString("20180208123456"), "2018-02-08T12:34:56Z", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("00000101000000"), "0000-01-01T00:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("29991939295959"), "3000-08-09T05:59:59Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("30000102235959"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.tryToIsoString("19852002235959"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("19850942235959"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("19850909335959"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("19850909296959"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("19850909295969"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-47130909295969"), "", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("201802081234"), "yyyyMMddHHmm", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("000001010000"), "yyyyMMddHHmm", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("299919392959"), "yyyyMMddHHmm", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("300001022359"),
        "",
        ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("198520022359"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("198509422359"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("198509093359"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("198509092969"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-471309092969"), "", "");

    Test.ensureEqual(Calendar2.tryToIsoString("201802081234"), "2018-02-08T12:34:00Z", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("000001010000"), "0000-01-01T00:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("299919392959"), "3000-08-09T05:59:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("300001022359"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.tryToIsoString("198520022359"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("198509422359"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("198509093359"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("198509092969"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-471309092969"), "", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2018020812"), "yyyyMMddHH", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("0000010100"), "yyyyMMddHH", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2999193929"), "yyyyMMddHH", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("3000010223"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985200223"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985094223"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1985090933"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-4713090933"), "", "");

    Test.ensureEqual(Calendar2.tryToIsoString("2018020812"), "2018-02-08T12:00:00Z", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("0000010100"), "0000-01-01T00:00:00Z", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2999193929"), "3000-08-09T05:00:00Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("3000010223"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.tryToIsoString("1985200223"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985094223"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1985090933"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-4713090933"), "", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("20180208"), "yyyyMMdd", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("00000101"), "yyyyMMdd", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("29991939"), "yyyyMMdd", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("30000102"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("19852002"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("19850942"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-47130942"), "", "");

    Test.ensureEqual(Calendar2.tryToIsoString("20180208"), "2018-02-08", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("00000101"), "0000-01-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("29991939"), "3000-08-08", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("30000102"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(Calendar2.tryToIsoString("19852002"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("19850942"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-47130942"), "", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("201802"), "yyyyMM", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("100001"), "yyyyMM", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("299919"), "yyyyMM", "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("300001"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("000001"),
        "",
        ""); // 0 millenia invalid to avoid confusion w
    // 6 digit numbers
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("198520"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("-471320"), "", "");

    Test.ensureEqual(Calendar2.tryToIsoString("201802"), "2018-02-01", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("100001"), "1000-01-01", "");
    /**/ Test.ensureEqual(
        Calendar2.tryToIsoString("299919"), "3000-07-01", ""); // invalid date. Why not okay since
    // lenient?
    Test.ensureEqual(
        Calendar2.tryToIsoString("300001"), "", ""); // 3000 invalid for compact formats
    Test.ensureEqual(
        Calendar2.tryToIsoString("000001"),
        "",
        ""); // 0 millenia invalid to avoid confusion w 6 digit
    // numbers
    Test.ensureEqual(Calendar2.tryToIsoString("198520"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("-471320"), "", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1/2/85"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("01/2/85"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1/02/85"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("11/22/85"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("Jan 2, 85"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("Jan 02, 85"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("Jan 22, 85"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2 Jan 85"), "", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2-JAN-85"), "", "");

    Test.ensureEqual(Calendar2.tryToIsoString("1/2/85"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("01/2/85"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1/02/85"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("11/22/85"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("Jan 2, 85"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("Jan 02, 85"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("Jan 22, 85"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2 Jan 85"), "", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2-JAN-85"), "", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1/2/1985"), "M/d/yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("01/2/1985"), "M/d/yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("1/02/1985"), "M/d/yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("11/22/1985"), "M/d/yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("11/31/1985"), "M/d/yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2 Jan 1985"), "d MMM yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("02 Jan 1985"), "dd MMM yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("22 Jan 1985"), "dd MMM yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2-JAN-1985"), "d-MMM-yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("02-JAN-1985"), "dd-MMM-yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2 January 1985"), "d MMMM yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("02 January 1985"), "dd MMMM yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("22 January 1985"), "dd MMMM yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("2-JANuary-1985"), "d-MMMM-yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("02-JANuary-1985"), "dd-MMMM-yyyy", "");

    Test.ensureEqual(Calendar2.tryToIsoString("1/2/1985"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("01/2/1985"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("1/02/1985"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("11/22/1985"), "1985-11-22", "");
    Test.ensureEqual(Calendar2.tryToIsoString("11/31/1985"), "1985-12-01", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2 Jan 1985"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("02 Jan 1985"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("22 Jan 1985"), "1985-01-22", "");
    Test.ensureEqual(Calendar2.tryToIsoString("2-JAN-1985"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("02-JAN-1985"), "1985-01-02", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("Jan 2, 1985"), "MMM d, yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("Jan 22, 1985"), "MMM d, yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("Jan 02, 1985"), "MMM d, yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("Jan 2, 0000"), "MMM d, yyyy", "");

    Test.ensureEqual(Calendar2.suggestDateTimeFormat("July 2, 1985"), "MMMM d, yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("july 2, 1985"), "MMMM d, yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("JULY 2, 1985"), "MMMM d, yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("July 22, 1985"), "MMMM d, yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("July 02, 1985"), "MMMM d, yyyy", "");
    Test.ensureEqual(Calendar2.suggestDateTimeFormat("July 2, 0000"), "MMMM d, yyyy", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sun, 06 Nov 1994 08:49:37 GMT"),
        "EEE, d MMM yyyy HH:mm:ss 'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sun, 06 Nov 1994 08:49:37 -08"),
        "EEE, d MMM yyyy HH:mm:ss x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sun, 06 Nov 1994 08:49:37 -0800"),
        "EEE, d MMM yyyy HH:mm:ss xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sun, 06 Nov 1994 08:49:37 -08:00"),
        "EEE, d MMM yyyy HH:mm:ss xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sun Nov 20 17:36:16 1994"),
        "EEE MMM dd HH:mm:ss yyyy",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sun Nov 6 7:36:16 1994"), "EEE MMM d H:mm:ss yyyy", "");

    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sunday, 06 November 1994 08:49:37 GMT"),
        "EEEE, d MMMM yyyy HH:mm:ss 'GMT'",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sunday, 06 November 1994 08:49:37 -08"),
        "EEEE, d MMMM yyyy HH:mm:ss x",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sun, 06 November 1994 08:49:37 -0800"), // 3 letter day
        "EEEE, d MMMM yyyy HH:mm:ss xx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sunday, 06 Nov 1994 08:49:37 -08:00"), // 3 letter month
        "EEEE, d MMMM yyyy HH:mm:ss xxx",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sunday November 20 17:36:16 1994"),
        "EEEE MMMM d HH:mm:ss yyyy",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat("Sunday November 6 17:36:16 1994"),
        "EEEE MMMM d HH:mm:ss yyyy",
        "");

    Test.ensureEqual(Calendar2.tryToIsoString("Jan 2, 1985"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("JAN 22, 1985"), "1985-01-22", "");
    Test.ensureEqual(Calendar2.tryToIsoString("jan 02, 1985"), "1985-01-02", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("Jan 2, 0000"), "0000-01-02", "");

    Test.ensureEqual(Calendar2.tryToIsoString("January 2, 1985"), "1985-01-02", "");
    Test.ensureEqual(Calendar2.tryToIsoString("january 22, 1985"), "1985-01-22", "");
    Test.ensureEqual(Calendar2.tryToIsoString("JANUARY 02, 1985"), "1985-01-02", "");
    /**/ Test.ensureEqual(Calendar2.tryToIsoString("January 2, 0000"), "0000-01-02", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun, 06 Nov 1994 08:49:37 GMT"), "1994-11-06T08:49:37Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun, 06 Nov 1994 08:49:37 -0800"), "1994-11-06T16:49:37Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun, 06 Nov 1994 08:49:37 -08:00"), "1994-11-06T16:49:37Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun, 06 Nov 1994 08:49:37 +0030"), "1994-11-06T08:19:37Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun, 06 Nov 1994 08:49:37 -00:30"), "1994-11-06T09:19:37Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun Nov 20 17:36:16 1994"), "1994-11-20T17:36:16Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun Nov 6 17:36:16 1994"), "1994-11-06T17:36:16Z", "");

    Test.ensureEqual(
        Calendar2.tryToIsoString("Sunday, 06 November 1994 08:49:37 GMT"),
        "1994-11-06T08:49:37Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sun, 06 November 1994 08:49:37 -0800"),
        "1994-11-06T16:49:37Z",
        ""); // 3
    // letter
    // day
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sunday, 06 Nov 1994 08:49:37 -08:00"),
        "1994-11-06T16:49:37Z",
        ""); // 3
    // letter
    // month
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sunday, 06 November 1994 08:49:37 +0030"),
        "1994-11-06T08:19:37Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("Sunday, 06 November 1994 08:49:37 -00:30"),
        "1994-11-06T09:19:37Z",
        "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("sunday NOVember 20 17:36:16 1994"), "1994-11-20T17:36:16Z", "");
    Test.ensureEqual(
        Calendar2.tryToIsoString("SUNDAY november 6 17:36:16 1994"), "1994-11-06T17:36:16Z", "");

    // arrays
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 1985-01-02, 9990-10-11"), false), // evenIfPurelyNumeric),
        "yyyy-MM-dd",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 19851011, 21001231"), true), // evenIfPurelyNumeric),
        "yyyyMMdd",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 19851011, 21001231"), false), // evenIfPurelyNumeric),
        "",
        "");
    // test finding one, then move on to another and go back and check the previous
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 1985-10-11, 9990-1-2"), false), // evenIfPurelyNumeric),
        "yyyy-M-d",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 1985-01-02, 9000-10-a1"), false), // evenIfPurelyNumeric),
        "",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 4 Feb 9999, 2 Jan 1985"), false), // evenIfPurelyNumeric),
        "d MMM yyyy",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 12 Feb 9999, 2 Jan 1985"), false), // evenIfPurelyNumeric),
        "d MMM yyyy",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 4 FEB 9999, 2 january 1985"), false), // evenIfPurelyNumeric),
        "d MMMM yyyy",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 12 FEBRUARY 9999, 2 jan 1985"), false), // evenIfPurelyNumeric),
        "d MMMM yyyy",
        "");
    Test.ensureEqual(
        Calendar2.suggestDateTimeFormat(
            StringArray.fromCSV(", 1985-01-02, Jan 2, 1985"), false), // evenIfPurelyNumeric),
        "",
        "");

    // toEpochSeconds(source, format)
    Test.ensureEqual(Calendar2.parseToEpochSeconds("2015-10-12", "yyyy-MM-dd"), 1.444608E9, "");
    Test.ensureEqual(Calendar2.parseToEpochSeconds("6015-10-12", "yyyy-MM-dd"), 1.27672416E11, "");
    Test.ensureEqual(Calendar2.parseToEpochSeconds("2015-1a-12", "yyyy-MM-dd"), Double.NaN, "");
    d = -6.21357696E10;
    Test.ensureEqual(Calendar2.parseToEpochSeconds("0001-01-01", "yyyy-MM-dd"), d, "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds("0000-01-01", "yyyy-MM-dd"), d -= 366 * 86400, "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds("-0001-01-01", "yyyy-MM-dd"), d -= 365 * 86400, "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds("-0002-01-01", "yyyy-MM-dd"), d -= 365 * 86400, "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds(
            "Sun, 06 Nov 1994 08:49:37 GMT", "EEE, dd MMM yyyy HH:mm:ss 'GMT'"),
        7.84111777E8,
        "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds(
            "Sun, 06 Nov 1994 08:49:3a GMT", "EEE, dd MMM yyyy HH:mm:ss 'GMT'"),
        Double.NaN,
        "");
    Test.ensureEqual(Calendar2.parseToEpochSeconds("4 Feb 9999", "d MMM yyyy"), 2.533737024E11, "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds("4 Feb 9999 '", "d MMM yyyy ''"), 2.533737024E11, "");
    Test.ensureEqual(Calendar2.parseToEpochSeconds("1985-01-04", "d MMM yy"), Double.NaN, "");

    // convert StringArray
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds(
                // since regex starts with yyyy-M it is parsed by parseISODateTime
                StringArray.fromCSV(", 1985-01-02, 9990-10-11"), "yyyy-M")
            .toString(),
        "NaN, 4.73472E8, 2.531112192E11",
        "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds(StringArray.fromCSV(", 1985-01-02, 2 Jan 1985"), "yyyy-M")
            .toString(),
        "NaN, 4.73472E8, NaN",
        "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds(StringArray.fromCSV(", 4 Feb 9999, 2 Jan 1985"), "d MMM yyyy")
            .toString(),
        "NaN, 2.533737024E11, 4.73472E8",
        "");
    Test.ensureEqual(
        Calendar2.parseToEpochSeconds(StringArray.fromCSV(", 4 Feb 9999, 1985-01-03"), "d MMM yyyy")
            .toString(),
        "NaN, 2.533737024E11, NaN",
        "");

    // cleanUpNumericTimeUnits(tUnits)
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("sec since 2 Jan 1985"),
        "seconds since 1985-01-02T00:00:00Z",
        ""); // adds time if unused
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("m    Since  1985-01-02T01:02:34Z"),
        "minutes since 1985-01-02T01:02:34Z",
        "");
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("hrs SINce 1985-01-02Z"),
        "hours since 1985-01-02T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("year sinCE 1985-1-2Z"),
        "years since 1985-01-02T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("day since 1985-1-2T1:2:3.0Z"),
        "days since 1985-01-02T01:02:03.000Z",
        ""); // adds millis if used
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("day since 1985-1-2T1:2:3Z"),
        "days since 1985-01-02T01:02:03Z",
        "");
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("mon since 1985-1-2T1:2:3"),
        "months since 1985-01-02T01:02:03Z",
        "");
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("ms  since 1985-1-2 1:2"),
        "milliseconds since 1985-01-02T01:02:00Z",
        "");
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits("millis since 15Z9dec2013"),
        "milliseconds since 2013-12-09T15:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.cleanUpNumericTimeUnits(" julian days since January 2, 1985 "),
        "days since 1985-01-02T00:00:00Z",
        "");

    /*
     * DISABLED, not used anywhere
     * //tryToIsoString(StringArray someDateTimeStrings)
     * Test.ensureEqual(Calendar2.tryToIsoString(
     * new StringArray(new String[]{"2 Jan 1985", "31 Dec 1986"}), false),
     * new StringArray(new String[]{"1985-01-02", "1986-12-31"}), //removes time if
     * unused
     * "");
     * Test.ensureEqual(Calendar2.tryToIsoString(
     * new StringArray(new String[]{"1985-01-02T01:02:34Z",
     * "1986-12-31T05:06:07Z"}), false),
     * new StringArray(new String[]{"1985-01-02T01:02:34Z",
     * "1986-12-31T05:06:07Z"}),
     * "");
     * Test.ensureEqual(Calendar2.tryToIsoString(
     * new StringArray(new String[]{"19850102", "19861231"}), true),
     * //evenIfPurelyNumeric
     * new StringArray(new String[]{"1985-01-02", "1986-12-31"}),
     * "");
     * Test.ensureEqual(Calendar2.tryToIsoString(
     * new StringArray(new String[]{"19850102", "19861231"}), false),
     * //evenIfPurelyNumeric
     * new StringArray(new String[]{"19850102", "19861231"}), //unchanged
     * "");
     * Test.ensureEqual(Calendar2.tryToIsoString(
     * new StringArray(new String[]{"1985zztop", "1986-12-31T05:06:07Z"}), false),
     * new StringArray(new String[]{"", ""}),
     * "");
     */

    // pseudo-super-precise times
    // 2020-05-18 These tests show that ERDDAP rounds to the millisecond.
    // Fix underlying methods and then fix these tests (for Yibo Jiang PODAAC).
    String format = "yyyy-MM-dd' 'HH:mm:ss.SSS000000'Z'";
    String val = "2020-05-18 01:02:03.123000000Z";
    gc = Calendar2.parseDateTimeZulu(val, format);
    double epochSeconds = Calendar2.gcToEpochSeconds(gc);
    String2.log("super-precise time gc=" + gc.toString() + " epochSeconds=" + epochSeconds);
    Test.ensureEqual(epochSeconds, 1.589763723123E9, "");
    Test.ensureEqual(
        Calendar2.format(epochSeconds, format, ""), "2020-05-18 01:02:03.123000000Z", "");

    // more tests of parseDateTime
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "2 Jan 1985", // optional space is present
                    "d MMM yyyy",
                    null)
                / 1000.0),
        "1985-01-02T00:00:00.000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis("2 Jan 1985 10 UTC", "d MMM yyyy HH 'UTC'", null)
                / 1000.0),
        "1985-01-02T10:00:00.000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "2 Jan 1985 10:11:12 UTC", "d MMM yyyy HH:mm:ss 'UTC'", null)
                / 1000.0),
        "1985-01-02T10:11:12.000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "2 Jan 1985 10:11:12.123 GMT", "d MMM yyyy HH:mm:ss.SSS 'GMT'", null)
                / 1000.0),
        "1985-01-02T10:11:12.123Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "2 Jan 1985 20:11:12.123UTC", "d MMM yyyy HH:mm:ss.SSS'UTC'", null)
                / 1000.0),
        "1985-01-02T20:11:12.123Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "2 jAn 1985 1:2:3.4 UTC", // jAn
                    "d MMM y H:m:s.S[ ]'UTC'",
                    null)
                / 1000.0), // flexi digits
        "1985-01-02T01:02:03.400Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 Jan 1985 10:20:30.5 UTC", // flexi digits
                    "d MMM yyyy H:m:s.S 'UTC'",
                    null)
                / 1000.0),
        "1985-01-12T10:20:30.500Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 Jan 1985 10:20:30.55 UTC", // flexi digits
                    "d MMM yyyy H:m:s.S 'UTC'",
                    null)
                / 1000.0),
        "1985-01-12T10:20:30.550Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 Jan 1985 10:20:30.555UTC", // flexi digits
                    "d MMM yyyy H:m:s.S'UTC'",
                    null)
                / 1000.0),
        "1985-01-12T10:20:30.555Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 Jan 1985 10:20:30.5555 UTC", // flexi digits
                    "d MMM yyyy H:m:s.S 'UTC'",
                    null)
                / 1000.0),
        "1985-01-12T10:20:30.555Z",
        ""); // 2020-05-21 was round to millis, now trunc.
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis( // {}# are reserved
                    "12 Jan 1985 !@(10~20$30%5555&*_=|\";:<>,) UTC", // other punctuation are
                    // literal
                    "dd MMM yyyy !@(HH~mm$ss%SSSS&*_=|\";:<>,)[ <>,]'UTC'", null)
                / 1000.0),
        "1985-01-12T10:20:30.555Z",
        ""); // 2020-05-21 was round to millis, now trunc.
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 12:20:30.555 am", // am clock hour 12, 1- 11
                    "d MMM yyyy h:m:s.S a",
                    null)
                / 1000.0), // h a
        "1985-01-12T00:20:30Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 12:20:30.555 pm", // pm clock hour 12, 1- 11
                    "d MMM yyyy h:m:s.S a",
                    null)
                / 1000.0), // h a
        "1985-01-12T12:20:30Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 1:2:3 AM", // am
                    "d MMM yyyy h:m:s a",
                    null)
                / 1000.0), // h a
        "1985-01-12T01:02:03Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 10:20:30PM", // pm
                    "d MMM yyyy h:m:sa",
                    null)
                / 1000.0), // h a
        "1985-01-12T22:20:30Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 00:20:30.555 am", // am hour of am/pm
                    "d MMM yyyy K:m:s.S a",
                    null)
                / 1000.0), // h a
        "1985-01-12T00:20:30Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 00:20:30.555 pm", // pm hour of am/pm
                    "d MMM yyyy KK:m:s.S a",
                    null)
                / 1000.0), // h a
        "1985-01-12T12:20:30Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 12:20:30.555 am", // am hour of am/pm 0-11, out-of-range->rolls to
                    // 12
                    "d MMM yyyy K:m:s.S a",
                    null)
                / 1000.0), // h a
        "1985-01-12T12:20:30Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 12:20:30.555 pm", // am hour of am/pm 0-11 out-of-range->rolls to
                    // 12
                    "d MMM yyyy K:m:s.S a",
                    null)
                / 1000.0), // h a
        "1985-01-13T00:20:30Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 00000000", // millis of day
                    "d MMM yyyy A",
                    null)
                / 1000.0),
        "1985-01-12T00:00:00.000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 86399999", // millis of day
                    "d MMM yyyy A",
                    null)
                / 1000.0),
        "1985-01-12T23:59:59.999Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 86399999", // millis of day
                    "d MMM yyyy AAAAAAAA",
                    null)
                / 1000.0),
        "1985-01-12T23:59:59.999Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 00000000000000", // nano of day
                    "d MMM yyyy N",
                    null)
                / 1000.0),
        "1985-01-12T00:00:00.000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 86399999999999", // nano of day
                    "d MMM yyyy N",
                    null)
                / 1000.0),
        "1985-01-12T23:59:59.999Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 86399999999999", // nano of day
                    "d MMM yyyy NNNNNNNNNNNNNN",
                    null)
                / 1000.0),
        "1985-01-12T23:59:59.999Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 01:02:03 00000000000000", // nano of second
                    "d MMM yyyy H:m:s N",
                    null)
                / 1000.0),
        "1985-01-12T01:02:03.000Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 01:02:03 86399999999999", // nano of second
                    "d MMM yyyy H:m:s N",
                    null)
                / 1000.0),
        "1985-01-13T01:02:02.999Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringT3Z(
            Calendar2.formattedStringToMillis(
                    "12 jan 1985 01:02:03 86399999999999", // nano of second
                    "d MMM yyyy H:m:s NNNNNNNNNNNNNN",
                    null)
                / 1000.0),
        "1985-01-13T01:02:02.999Z",
        "");

    // test catching errors in parseDateTime
    String tryIt[] = { // alternate: dateTime, format, error string
      "2 Jan 1985 10:11:12.123  UTC", // 2 spaces
      "d MMM yyyy HH:mm:ss.SSS[ ]'UTC'",
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12.123  UTC\" as \"d MMM yyyy HH:mm:ss.SSS[ ]'UTC'\": "
          + "The time string doesn't match the format at time string character #25.",
      "2 Jan 985 10:11:12.123 UTC", // 3 digit year
      "d MMM yyyy HH:mm:ss.SSS[ ]'UTC'",
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 985 10:11:12.123 UTC\" as \"d MMM yyyy HH:mm:ss.SSS[ ]'UTC'\": "
          + "The time string doesn't match the format at time string character #10.",
      "2 Jan 1985 10:11:12.123 UT", // C missing
      "d MMM yyyy HH:mm:ss.SSS[ ]'UTC'",
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12.123 UT\" as \"d MMM yyyy HH:mm:ss.SSS[ ]'UTC'\": "
          + "The time string lacks information specified at end of format.",
      "2 Jan 1985 1:11:12.123 UTC", // not right fixed #digits
      "d MMM yyyy HH:mm:ss.SSS[ ]'UTC'",
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 1:11:12.123 UTC\" as \"d MMM yyyy HH:mm:ss.SSS[ ]'UTC'\": "
          + "The time string doesn't match the format at time string character #13.",
      "2 Jan 1985 10:11:12.123 UTC",
      "d MMM yyyy HH:mm:ss.SSS[ ]'UT'", // C missing
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12.123 UTC\" as \"d MMM yyyy HH:mm:ss.SSS[ ]'UT'\": "
          + "The time string doesn't match the format at time string character #27.",
      "2 Jan 1985 10:11:12.123 UTC",
      "d DDD yyyy HH:mm:ss.SSS[ ]'UTC'", // DDD text
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12.123 UTC\" as \"d DDD yyyy HH:mm:ss.SSS[ ]'UTC'\": "
          + "The time string doesn't match the format at time string character #3.",
      "2 Jan 1985 10:11:12.123 UTC",
      "d MMM yyyy HH:mm:ss.SSS[[ ]]'UTC'", // [[
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12.123 UTC\" as \"d MMM yyyy HH:mm:ss.SSS[[ ]]'UTC'\": "
          + "Unexpected or unsupported format character count: '[[' at #24.",
      "2 Jan 1985 10:11:12.123 UTC",
      "d MMM yyyy BB:mm:ss.SSS[ ]'UTC'", // unsupported B
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12.123 UTC\" as \"d MMM yyyy BB:mm:ss.SSS[ ]'UTC'\": "
          + "Unexpected or unsupported format character 'B' at #12.",
      "1985 3rd quarter",
      "yyyy QQQ", // unsupported Q
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"1985 3rd quarter\" as \"yyyy QQQ\": "
          + "Unexpected or unsupported format character 'Q' at #6.",
      "2 Jan 1985 10:11:12.123 UTC",
      "d MMM yyyy HH{mm:ss.SSS[ ]'UTC'", // {} are reserved
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12.123 UTC\" as \"d MMM yyyy HH{mm:ss.SSS[ ]'UTC'\": "
          + "Unexpected or unsupported format character '{' at #14.",
      "2 Jan 1985 10:11:12.123 UTC",
      "d MMM yyyy HH#mm:ss.SSS[ ]'UTC'", // # is reserved
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12.123 UTC\" as \"d MMM yyyy HH#mm:ss.SSS[ ]'UTC'\": "
          + "Unexpected or unsupported format character '#' at #14.",
      "2 Jan 1985 10:11:12+123 UTC", // +
      "d MMM yyyy HH:mm:ss.SSS[ ]'UTC'",
      "java.lang.RuntimeException: ERROR: parseDateTime was unable to parse "
          + "\"2 Jan 1985 10:11:12+123 UTC\" as \"d MMM yyyy HH:mm:ss.SSS[ ]'UTC'\": "
          + "The time string doesn't match the format at time string character #20."
    };

    for (int i = 0; i < tryIt.length; i += 3) {
      String msg = "shouldn't get here 1";
      try {
        Calendar2.formattedStringToMillis(tryIt[i], tryIt[i + 1], null); // timezone=null
        msg = "shouldn't get here 2";
      } catch (Exception e) {
        msg = e.toString();
      }
      Test.ensureEqual(msg, tryIt[i + 2], "tryIt i=" + i + "  " + tryIt[i] + "  " + tryIt[i + 1]);
    }

    // isTimeUnits
    s = "mm-dd-yyyy";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = "mm-dd-yyyy";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = "d since 1-";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = "d  since 1-";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = "d since  1-";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = " hours since 1970-01-01T00:00:00Z ";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = " hours since 0000-01-01T00:00:00Z ";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = " hours since -0001-01-01T00:00:00Z ";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = "millis since 1970-01-01";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);
    s = "d SiNCE 2001";
    Test.ensureTrue(Calendar2.isTimeUnits(s), s);

    s = null;
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = "";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = " ";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = "m-d-y";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = "m-d-Y";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = " since 2001";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = "d1 since 2001";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = "d since analysis";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = "d since2001";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = "mm-dd-yy";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);
    s = "mm-dd-YY";
    Test.ensureTrue(!Calendar2.isTimeUnits(s), s);

    // test that all of those formats work
    testDateTimeFormatters("2002-027", "yyyy-DDD", "UTC", "2002-01-27T00:00:00.000Z");
    testDateTimeFormatters("19850102235959", "yyyyMMddHHmmss", "UTC", "1985-01-02T23:59:59.000Z");
    testDateTimeFormatters("198501022359", "yyyyMMddHHmm", "UTC", "1985-01-02T23:59:00.000Z");
    testDateTimeFormatters("1985010223", "yyyyMMddHH", "Zulu", "1985-01-02T23:00:00.000Z");
    testDateTimeFormatters("19850102", "yyyyMMdd", null, "1985-01-02T00:00:00.000Z");
    testDateTimeFormatters("198501", "yyyyMM", "UTC", "1985-01-01T00:00:00.000Z");

    // !!! 2017-03-20 with switch to java.time yy expands to 2085 instead of 1985
    /*
     * testDateTimeFormatters("1/2/85", "M/d/yy", "UTC",
     * "2085-01-02T00:00:00.000Z");
     * testDateTimeFormatters("11/22/85", "M/d/yy", "UTC",
     * "2085-11-22T00:00:00.000Z");
     * testDateTimeFormatters("1/2/1985", "M/d/yy", "UTC",
     * "1985-01-02T00:00:00.000Z");
     * testDateTimeFormatters("11/22/1985", "M/d/yy", "UTC",
     * "1985-11-22T00:00:00.000Z");
     * testDateTimeFormatters("Jan 2, 85" "MMM d, yy", "UTC",
     * "2085-01-02T00:00:00.000Z");
     * testDateTimeFormatters("Jan 22, 85", "MMM d, yy", "UTC",
     * "2085-01-22T00:00:00.000Z");
     * testDateTimeFormatters("Jan 22, 1985", "MMM d, yy", "UTC",
     * "1985-01-22T00:00:00.000Z");
     * testDateTimeFormatters("2 Jan 85", "d MMM yy", "UTC",
     * "2085-01-02T00:00:00.000Z");
     * testDateTimeFormatters("22 Jan 1985", "d MMM yy", "UTC",
     * "1985-01-22T00:00:00.000Z");
     * testDateTimeFormatters("2-JAN-85", "d-MMM-yy", "UTC",
     * "2085-01-02T00:00:00.000Z");
     * testDateTimeFormatters("02-JAN-1985", "d-MMM-yy", "UTC",
     * "1985-01-02T00:00:00.000Z");
     */
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03 GMT",
        "EEE, dd MMM yyyy HH:mm:ss 'GMT'",
        "UTC",
        "1985-01-02T01:02:03.000Z");
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03Z",
        "EEE, dd MMM yyyy HH:mm:ssXXX",
        "UTC",
        "1985-01-02T01:02:03.000Z");
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03-08",
        "EEE, dd MMM yyyy HH:mm:ssX",
        "UTC",
        "1985-01-02T09:02:03.000Z");
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03-0800",
        "EEE, dd MMM yyyy HH:mm:ssXX",
        "UTC",
        "1985-01-02T09:02:03.000Z");
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03-08:00",
        "EEE, dd MMM yyyy HH:mm:ssXXX",
        "UTC",
        "1985-01-02T09:02:03.000Z");
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03 Z",
        "EEE, dd MMM yyyy HH:mm:ss X",
        "UTC",
        "1985-01-02T01:02:03.000Z");
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03 -08",
        "EEE, dd MMM yyyy HH:mm:ss X",
        "UTC",
        "1985-01-02T09:02:03.000Z");
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03 -0800",
        "EEE, dd MMM yyyy HH:mm:ss XX",
        "UTC",
        "1985-01-02T09:02:03.000Z");
    testDateTimeFormatters(
        "WED, 02 JAN 1985 01:02:03 -08:00",
        "EEE, dd MMM yyyy HH:mm:ss XXX",
        "UTC",
        "1985-01-02T09:02:03.000Z");

    // factorToGetSeconds
    Test.ensureEqual(Calendar2.factorToGetSeconds("ms"), 0.001, "a");
    Test.ensureEqual(Calendar2.factorToGetSeconds("Milliseconds "), 0.001, "b");
    Test.ensureEqual(Calendar2.factorToGetSeconds("s"), 1, "c");
    Test.ensureEqual(Calendar2.factorToGetSeconds(" SECONDS"), 1, "d");
    Test.ensureEqual(Calendar2.factorToGetSeconds("m"), 60, "e");
    Test.ensureEqual(Calendar2.factorToGetSeconds("min"), 60, "f");
    Test.ensureEqual(Calendar2.factorToGetSeconds("h"), 3600, "g");
    Test.ensureEqual(Calendar2.factorToGetSeconds("hr"), 3600, "h");
    Test.ensureEqual(Calendar2.factorToGetSeconds("hour"), 3600, "i");
    Test.ensureEqual(Calendar2.factorToGetSeconds("d"), 86400, "j");
    Test.ensureEqual(Calendar2.factorToGetSeconds("day"), 86400, "k");
    Test.ensureEqual(Calendar2.factorToGetSeconds("mon"), 30 * 86400, "m");
    Test.ensureEqual(Calendar2.factorToGetSeconds("months"), 30 * 86400, "n");
    Test.ensureEqual(Calendar2.factorToGetSeconds("year"), 360 * 86400, "o");
    Test.ensureEqual(Calendar2.factorToGetSeconds("years"), 360 * 86400, "p");
    try {
      Calendar2.factorToGetSeconds("zzz");
      throw new Throwable("Shouldn't get here.1");
    } catch (Exception e) {
    }
    String willFail[] = {
      "no", "now-", "now+", "now ", "nowa", "now2", "now-2.3", "now-2.3seconds", "now-2secondsa"
    };
    for (int i = 0; i < willFail.length; i++) {
      try {
        d = Calendar2.nowStringToEpochSeconds(willFail[i]);
        throw new RuntimeException(willFail[i] + " should have failed i=" + i + "=" + willFail[i]);
      } catch (Exception e) {
        String es = MustBe.throwableToString(e);
        expected =
            "SimpleException: Query error: Invalid \"now\" constraint: \""
                + willFail[i]
                + "\". "
                + "Timestamp constraints with \"now\" must be in the form "
                + "\"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units).\n"
                + " at ";
        Test.ensureEqual(
            es.substring(0, Math.min(es.length(), expected.length())),
            expected,
            "willFail=\"" + willFail[i] + "\" other failure: " + es);
      }
    }

    // parseMinMaxString(mmString, mmValue, allowTimeUnits) test allowTimeUnits=true
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)", 100, true), 100, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max()-0", 100, true), 100, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)+0", 100, true), 100, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)+35", 100, true), 100 + 35, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)-3.5", 100, true), 100 - 3.5, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z) 36", 100, true), 100 + 36, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)-0ms", 100, true), 100, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)+0ms", 100, true), 100, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)-2ms", 100, true), 100 - 0.002, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)+2millis", 100, true), 100 + 0.002, "");
    Test.ensureEqual(
        Calendar2.parseMinMaxString("max(z)+2millisecond", 100, true), 100 + 0.002, "");
    Test.ensureEqual(
        Calendar2.parseMinMaxString("min(z)-2milliseconds", 100, true), 100 - 0.002, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)+s", 100, true), 100 + 1, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)-2second", 100, true), 100 - 2, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z) 34seconds", 100, true), 100 + 34, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)-2min", 100, true), 100 - 120, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)+3minutes", 100, true), 100 + 180, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)-2hour", 100, true), 100 - 7200, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)+3hours", 100, true), 100 + 10800, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)+day", 100, true), 100 + 86400, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)-3days", 100, true), 100 - 259200, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)-3week", 100, true), 100 - 1814400.0, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)+4weeks", 100, true), 100 + 2419200, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)-3month", 0, true), -7948800.0, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)+4months", 0, true), 1.0368E7, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)-2year", 0, true), -6.31584E7, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)-2years", 0, true), -6.31584E7, "");

    try {
      d = Calendar2.parseMinMaxString("min", 100, true);
      throw new RuntimeException("should have failed.");
    } catch (Exception e) {
      String es = MustBe.throwableToString(e);
      Test.ensureTrue(
          es.startsWith("SimpleException: Query error: \"min(\" or \"max(\" expected."),
          " other failure: " + es);
    }

    willFail =
        new String[] {
          "min(z",
          "max(z",
          "min(z)-",
          "max(z)+",
          "min(z) ",
          "max(z)a",
          "min(z)2",
          "max(z) 2q",
          "min(z)-2.3seconds",
          "min(z)-2secondsa"
        };
    for (int i = 0; i < willFail.length; i++) {
      try {
        d = Calendar2.parseMinMaxString(willFail[i], 100, true);
        throw new RuntimeException(willFail[i] + " should have failed.");
      } catch (Exception e) {
        String es = MustBe.throwableToString(e);
        expected =
            "SimpleException: Query error: Invalid \""
                + willFail[i].substring(0, 3)
                + "()\" constraint: \""
                + willFail[i]
                + "\". "
                + "Timestamp constraints with \""
                + willFail[i].substring(0, 3)
                + "()\" must be in the form "
                + "\""
                + willFail[i].substring(0, 3)
                + "(varName)[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units)."
                + "\n at ";
        Test.ensureEqual(
            es.substring(0, Math.min(es.length(), expected.length())),
            expected,
            "willFail=\"" + willFail[i] + "\" other failure: " + es);
      }
    }

    // parseMinMaxString(mmString, mmValue, allowTimeUnits) test
    // allowTimeUnits=false
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)", 100, false), 100, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max()-0", 100, false), 100, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)+0", 100, false), 100, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("max(z)+35", 100, false), 100 + 35, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z)-3.5", 100, false), 100 - 3.5, "");
    Test.ensureEqual(Calendar2.parseMinMaxString("min(z) 36", 100, false), 100 + 36, "");

    String2.log("\nExpected errors:");
    try {
      d = Calendar2.parseMinMaxString("min", 100, false);
      throw new RuntimeException("should have failed.");
    } catch (Exception e) {
      String es = MustBe.throwableToString(e);
      Test.ensureTrue(
          es.startsWith("SimpleException: Query error: \"min(\" or \"max(\" expected."),
          " other failure: " + es);
    }

    willFail =
        new String[] {
          "min(z",
          "max(z",
          "min(z)-",
          "max(z)+",
          "min(z) ",
          "max(z)a",
          "min(z)2",
          "max(z) 2d",
          "min(z)-2seconds"
        };
    for (int i = 0; i < willFail.length; i++) {
      try {
        d = Calendar2.parseMinMaxString(willFail[i], 100, false);
        throw new RuntimeException(willFail[i] + " should have failed.");
      } catch (Exception e) {
        String es = MustBe.throwableToString(e);
        expected =
            "SimpleException: Query error: Invalid \""
                + willFail[i].substring(0, 3)
                + "()\" constraint: \""
                + willFail[i]
                + "\". "
                + "Non-timestamp constraints with \""
                + willFail[i].substring(0, 3)
                + "()\" must be in the form "
                + "\""
                + willFail[i].substring(0, 3)
                + "(varName)[+|-positiveNumber]\"."
                + "\n at ";
        Test.ensureEqual(
            es.substring(0, Math.min(es.length(), expected.length())),
            expected,
            "willFail=\"" + willFail[i] + "\" other failure: " + es);
      }
    }
    String2.log("> End of expected errors");

    // parseNumberTimeUnits(String ntu) {
    Test.ensureEqual(Calendar2.parseNumberTimeUnits("1.4e5sec"), new double[] {1.4e5, 1}, "");
    Test.ensureEqual(Calendar2.parseNumberTimeUnits("2min"), new double[] {2, 60}, "");
    Test.ensureEqual(Calendar2.parseNumberTimeUnits("hours"), new double[] {1, 3600}, "");
    Test.ensureEqual(Calendar2.parseNumberTimeUnits("2.3e3"), new double[] {2.3e3, 1}, "");

    String2.log("\nExpected errors:");
    willFail = new String[] {null, "", "  ,  ", "1e500", "2monw", "zztop"};
    for (int i = 0; i < willFail.length; i++) {
      try {
        double dar[] = Calendar2.parseNumberTimeUnits(willFail[i]);
        throw new RuntimeException(willFail[i] + " should have failed.");
      } catch (Exception e) {
        String es = MustBe.throwableToString(e);
        if (es.indexOf("ERROR in parseNumberTimeUnits: ") < 0
            && es.indexOf("ERROR in Calendar2.factorToGetSeconds: ") < 0) {
          throw new RuntimeException("Unexpected error for " + willFail[i], e);
        }
      }
    }
    String2.log("> End of expected errors");

    // getMonthName3
    Test.ensureEqual(Calendar2.getMonthName3(1), "Jan", "a");
    Test.ensureEqual(Calendar2.getMonthName3(12), "Dec", "b");
    try {
      Calendar2.getMonthName3(0);
      throw new Throwable("Shouldn't get here.2");
    } catch (Exception e) {
    }

    // getMonthName
    Test.ensureEqual(Calendar2.getMonthName(1), "January", "a");
    Test.ensureEqual(Calendar2.getMonthName(12), "December", "b");
    try {
      Calendar2.getMonthName(0);
      throw new Throwable("Shouldn't get here.3");
    } catch (Exception e) {
    }

    // equals
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(2005, 9, 1).equals(Calendar2.newGCalendarZulu(2005, 8, 32)),
        true,
        "a1"); // 8/32 -> 9/1
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(2005, 9, 1).equals(Calendar2.newGCalendarZulu(2005, 9, 2)),
        false,
        "a2");

    Test.ensureEqual(Calendar2.zuluTimeZone.useDaylightTime(), false, "a3");

    // constructors
    Test.ensureEqual(Calendar2.newGCalendarZulu(1970, 1, 1).getTimeInMillis(), 0, "a1970-01-01");
    // 2 tests from http://www.xav.com/time.cgi
    // 2005-08-31T16:00:00 was 1125504000 seconds + 1min+2sec+3milli=62003 millis
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(2005, 8, 31, 16, 1, 2, 3).getTimeInMillis(),
        1125504062003L,
        "a2005-08-31");
    // 2005-11-02 18:04:09 was 1130954649 seconds
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(2005, 11, 2, 18, 4, 9, 0).getTimeInMillis(),
        1130954649000L,
        "a2005-11-02");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1, 1, 1, 0, 0, 0, 0).getTimeInMillis(),
        -62135769600000L,
        "a0001-01-01");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(0, 1, 1, 0, 0, 0, 0).getTimeInMillis(),
        -62167392000000L,
        "a0000-01-01");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(-1, 1, 1, 0, 0, 0, 0).getTimeInMillis(),
        -62198928000000L,
        "a-0001-01-01");

    Test.ensureEqual(Calendar2.newGCalendarZulu(0).getTimeInMillis(), 0, "a8");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1125504062003L).getTimeInMillis(), 1125504062003L, "a9");
    try {
      Calendar2.newGCalendarZulu(Long.MAX_VALUE);
      throw new Throwable("Shouldn't get here.4");
    } catch (Exception e) {
    }

    // newGCalendar, parse/format ISODate, yyyyDDD, IsoDateHM, CompactDateTime
    String2.log("test parse/format USDate, ISODate, yyyyDDD, IsoDateHM, CompactDateTime");
    s = "2004-03-02T14:35:08"; // 'T'
    GregorianCalendar localGC = Calendar2.newGCalendarLocal();
    GregorianCalendar zuluGC = Calendar2.newGCalendarZulu();
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTime(localGC, s)), s, "nL");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTime(zuluGC, s)), s, "nZ");
    String s2 = "1970-01-01 00:00:00 UTC";
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTime(zuluGC, s2)),
        "1970-01-01T00:00:00",
        "nZ2");
    s2 = "0001-01-01T02:00:00";
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu(s2)), s2, "n0001");
    s2 = "0000-02-01T00:03:00";
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu(s2)), s2, "n0000");
    s2 = "-0001-02-03T00:00:04";
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu(s2)), s2, "n-0001");
    s2 = "1970-01-01 00:00:00.000 1:00"; // a test of timeZone offset
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu(s2)),
        "1969-12-31T23:00:00",
        "");

    // 2012-05-22 new harder tests of modified Calendar2 methods
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2Q3:4:5.678")),
        "1970-01-02T03:04:05.678Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2!3:4:5.67")),
        "1970-01-02T03:04:05.670Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5.6")),
        "1970-01-02T03:04:05.600Z",
        "");
    // tests of timeZone offset...
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5.678 1")),
        "1970-01-02T02:04:05.678Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5.678+1")),
        "1970-01-02T02:04:05.678Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5.678-1")),
        "1970-01-02T04:04:05.678Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5.678 1:12")),
        "1970-01-02T01:52:05.678Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5.678+1:12")),
        "1970-01-02T01:52:05.678Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5.678-1:12")),
        "1970-01-02T04:16:05.678Z",
        "");
    // jump to timezone
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5 1:00")),
        "1970-01-02T02:04:05.000Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4 1:00")),
        "1970-01-02T02:04:00.000Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3 1:00")),
        "1970-01-02T02:00:00.000Z",
        "");
    // just (part of) date
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1971-2-3")),
        "1971-02-03T00:00:00.000Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1971-2")),
        "1971-02-01T00:00:00.000Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1971")),
        "1971-01-01T00:00:00.000Z",
        "");

    gc = Calendar2.parseISODateTimeZulu("2011-12-31T23:59:59.997Z");
    Test.ensureEqual(gc.getTimeInMillis(), 1.325376E12, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeT3Z(gc), "2011-12-31T23:59:59.997Z", "");

    // 2012-12-26 support comma in SS,SSS
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT3Z(Calendar2.parseISODateTimeZulu("1970-1-2 3:4:5,6")),
        "1970-01-02T03:04:05.600Z",
        "");

    // support any T character and Z or z
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeTZ(Calendar2.parseISODateTimeZulu("1970-1-2t3:4:5z")),
        "1970-01-02T03:04:05Z",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeTZ(Calendar2.parseISODateTimeZulu("1970-01-02a03:04:05z")),
        "1970-01-02T03:04:05Z",
        "");

    try {
      Calendar2.formatAsISODateTimeT(null);
      throw new Throwable("Shouldn't get here.5");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTime(zuluGC, null);
      throw new Throwable("Shouldn't get here.6");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTime(zuluGC, "");
      throw new Throwable("Shouldn't get here.7");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTime(zuluGC, "a");
      throw new Throwable("Shouldn't get here.8");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTime(null, s);
      throw new Throwable("Shouldn't get here.9");
    } catch (Exception e) {
    }
    s = "2004-03-02 14:35:08"; // space
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(Calendar2.parseISODateTime(localGC, s)), s, "n2L");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(Calendar2.parseISODateTime(zuluGC, s)), s, "n2Z");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.parseISODateTime(zuluGC, "2004-03-02-14:35:08")),
        s,
        "n2Z2"); // other connecting char allowed
    s2 = "0001-01-01 02:00:00";
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(Calendar2.parseISODateTimeZulu(s2)), s2, "n2 0001");
    s2 = "0000-02-01 00:03:00";
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(Calendar2.parseISODateTimeZulu(s2)), s2, "n2 0000");
    s2 = "-0001-03-04 00:00:04";
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(Calendar2.parseISODateTimeZulu(s2)), s2, "n2 -0001");
    try {
      Calendar2.formatAsISODateTimeSpace(null);
      throw new Throwable("Shouldn't get here.10");
    } catch (Exception e) {
    }
    s = "2003-05-16";
    Test.ensureEqual(Calendar2.formatAsISODate(Calendar2.parseISODateTime(localGC, s)), s, "oL");
    Test.ensureEqual(Calendar2.formatAsISODate(Calendar2.parseISODateTime(zuluGC, s)), s, "oZ");
    try {
      Calendar2.formatAsISODate(null);
      throw new Throwable("Shouldn't get here.11");
    } catch (Exception e) {
    }

    Test.ensureEqual(
        Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarZulu(2003, 1, 2, 17, 4, 5, 6)),
        "02-Jan-2003 17:04:05",
        "DDMonYYYY");
    try {
      Calendar2.formatAsDDMonYYYY(null);
      throw new Throwable("Shouldn't get here.12");
    } catch (Exception e) {
    }

    // time zones
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06")),
        "2003-02-03T04:05:06",
        "z1");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06.1")),
        "2003-02-03T04:05:06",
        "z1");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06.9")),
        "2003-02-03T04:05:06",
        "z1");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06Z")),
        "2003-02-03T04:05:06",
        "z2");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06-00:00")),
        "2003-02-03T04:05:06",
        "z3");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06-01:00")),
        "2003-02-03T05:05:06",
        "z4");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06-01:30")),
        "2003-02-03T05:35:06",
        "z5");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06+01:00")),
        "2003-02-03T03:05:06",
        "z6");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(
            Calendar2.parseISODateTimeZulu("2003-02-03T04:05:06.1+01:00")),
        "2003-02-03T03:05:06",
        "z6");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(
            Calendar2.parseISODateTimeZulu("0001-02-03T04:05:06.1+01:00")),
        "0001-02-03T03:05:06",
        "z6");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(
            Calendar2.parseISODateTimeZulu("0000-02-03T04:05:06.1+01:00")),
        "0000-02-03T03:05:06",
        "z6");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(
            Calendar2.parseISODateTimeZulu("-0001-02-03T04:05:06.1+01:00")),
        "-0001-02-03T03:05:06",
        "z6");

    // no 0 padding
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("2003-2-3")),
        "2003-02-03T00:00:00",
        "oZ1");
    // year <50 before 11/13/2006, behaviour was to add 2000 to the year
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("3-1-2")),
        "0003-01-02T00:00:00",
        "oZ2");
    // year <50 before 11/13/2006, behaviour was to add 2000 to the year
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("3")),
        "0003-01-01T00:00:00",
        "oZ3");
    // year >50 before 11/13/2006, behaviour was to add 1900 to the year
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeT(Calendar2.parseISODateTimeZulu("98-11-12T10")),
        "0098-11-12T10:00:00",
        "oZ4");
    // bad
    try {
      Calendar2.parseISODateTimeZulu("1998/03");
      throw new Throwable("Shouldn't get here.13");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTimeZulu("1990a");
      throw new Throwable("Shouldn't get here.14");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTimeZulu("1998::01");
      throw new Throwable("Shouldn't get here.15");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTimeZulu("1990:a");
      throw new Throwable("Shouldn't get here.16");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTimeZulu("a");
      throw new Throwable("Shouldn't get here.17");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTimeZulu("");
      throw new Throwable("Shouldn't get here.18");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseISODateTimeZulu(null);
      throw new Throwable("Shouldn't get here.19");
    } catch (Exception e) {
    }

    s = "2003002";
    Test.ensureEqual(Calendar2.formatAsYYYYDDD(Calendar2.parseYYYYDDD(localGC, s)), s, "pL");
    Test.ensureEqual(Calendar2.formatAsYYYYDDD(Calendar2.parseYYYYDDD(zuluGC, s)), s, "pZ");
    Test.ensureEqual(Calendar2.formatAsYYYYDDD(Calendar2.parseYYYYDDDZulu(s)), s, "pZ");
    Test.ensureEqual(
        Calendar2.formatAsYYYYDDD(Calendar2.parseYYYYDDDZulu("0001002")), "0001002", "pL0001");
    Test.ensureEqual(
        Calendar2.formatAsYYYYDDD(Calendar2.parseYYYYDDDZulu("0000003")), "0000003", "pL0000");
    Test.ensureEqual(
        Calendar2.formatAsYYYYDDD(Calendar2.parseYYYYDDDZulu("-0001004")), "-0001004", "pL-0001");
    try {
      Calendar2.formatAsYYYYDDD(null);
      throw new Throwable("Shouldn't get here.20");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseYYYYDDD(zuluGC, null);
      throw new Throwable("Shouldn't get here.21");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseYYYYDDD(zuluGC, "a");
      throw new Throwable("Shouldn't get here.22");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseYYYYDDD(zuluGC, "200600");
      throw new Throwable("Shouldn't get here.23");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseYYYYDDD(zuluGC, "200600a");
      throw new Throwable("Shouldn't get here.24");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseYYYYDDD(zuluGC, "20060011");
      throw new Throwable("Shouldn't get here.25");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseYYYYDDDZulu(null);
      throw new Throwable("Shouldn't get here.26");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseYYYYDDDZulu("a");
      throw new Throwable("Shouldn't get here.27");
    } catch (Exception e) {
    }

    s = "2006-02-03";
    Test.ensureEqual(Calendar2.formatAsYYYYMM(Calendar2.parseISODateTimeZulu(s)), "200602", "pL");
    try {
      Calendar2.formatAsYYYYMM(null);
      throw new Throwable("Shouldn't get here.28");
    } catch (Exception e) {
    }

    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarLocal(2003, 365)), "2003-12-31", "cL");
    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarZulu(2003, 365)), "2003-12-31", "cZ");
    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarLocal(2004, 365)), "2004-12-30", "dL");
    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarZulu(2004, 365)), "2004-12-30", "dZ");
    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarZulu(1, 34)), "0001-02-03", "dZ1");
    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarZulu(0, 34)), "0000-02-03", "dZ0");
    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarZulu(-1, 34)), "-0001-02-03", "dZ-1");
    try {
      Calendar2.newGCalendarLocal(Integer.MAX_VALUE, 365);
      throw new Throwable("Shouldn't get here.30");
    } catch (Exception e) {
    }
    try {
      Calendar2.newGCalendarZulu(Integer.MAX_VALUE, 365);
      throw new Throwable("Shouldn't get here.31");
    } catch (Exception e) {
    }
    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarLocal(2003, 1, 2)), "2003-01-02", "fL");
    Test.ensureEqual(
        Calendar2.formatAsISODate(Calendar2.newGCalendarZulu(2003, 1, 2)), "2003-01-02", "fZ");
    try {
      Calendar2.newGCalendarLocal(Integer.MAX_VALUE, 1, 2);
      throw new Throwable("Shouldn't get here.32");
    } catch (Exception e) {
    }
    try {
      Calendar2.newGCalendarZulu(Integer.MAX_VALUE, 1, 2);
      throw new Throwable("Shouldn't get here.33");
    } catch (Exception e) {
    }

    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.newGCalendarLocal(2003, 1, 2, 3, 4, 5, 6)),
        "20030102030405",
        "hL");
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.newGCalendarZulu(2003, 1, 2, 3, 4, 5, 6)),
        "20030102030405",
        "hZ");
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.newGCalendarZulu(1, 1, 2, 3, 4, 5, 6)),
        "00010102030405",
        "hZ");
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.newGCalendarZulu(0, 1, 2, 3, 4, 5, 6)),
        "00000102030405",
        "hZ");
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.newGCalendarZulu(-1, 1, 2, 3, 4, 5, 6)),
        "-00010102030405",
        "hZ");
    try {
      Calendar2.formatAsCompactDateTime(null);
      throw new Throwable("Shouldn't get here.34");
    } catch (Exception e) {
    }
    try {
      Calendar2.newGCalendarLocal(Integer.MAX_VALUE, 1, 2, 3, 4, 5, 6);
      throw new Throwable("Shouldn't get here.35");
    } catch (Exception e) {
    }
    try {
      Calendar2.newGCalendarZulu(Integer.MAX_VALUE, 1, 2, 3, 4, 5, 6);
      throw new Throwable("Shouldn't get here.36");
    } catch (Exception e) {
    }
    Test.ensureEqual(
        Calendar2.formatAsYYYYDDD(Calendar2.newGCalendarLocal(2003, 1, 2)), "2003002", "mL");
    Test.ensureEqual(
        Calendar2.formatAsYYYYDDD(Calendar2.newGCalendarZulu(2003, 1, 2)), "2003002", "mZ");
    Test.ensureEqual(
        Calendar2.formatAsYYYYDDD(Calendar2.newGCalendarZulu(1, 1, 2)), "0001002", "mZ");
    Test.ensureEqual(
        Calendar2.formatAsYYYYDDD(Calendar2.newGCalendarZulu(0, 1, 2)), "0000002", "mZ");
    Test.ensureEqual(
        Calendar2.formatAsYYYYDDD(Calendar2.newGCalendarZulu(-1, 1, 2)), "-0001002", "mZ");

    Test.ensureEqual(
        Calendar2.formatAsUSSlash24(Calendar2.parseUSSlash24(zuluGC, "1/2/2006 03:04:05")),
        "1/2/2006 03:04:05",
        "qe1");
    Test.ensureEqual(
        Calendar2.formatAsUSSlash24(Calendar2.parseUSSlash24(zuluGC, "1/2/06 3:4")),
        "1/2/2006 03:04:00",
        "qe2");
    Test.ensureEqual(
        Calendar2.formatAsUSSlash24(Calendar2.parseUSSlash24(zuluGC, "12/31/99 3")),
        "12/31/1999 03:00:00",
        "qe3");
    Test.ensureEqual(
        Calendar2.formatAsUSSlash24(Calendar2.parseUSSlash24(zuluGC, "1/2/2006")),
        "1/2/2006 00:00:00",
        "qe4");
    Test.ensureEqual(
        Calendar2.formatAsUSSlash24(Calendar2.parseUSSlash24(zuluGC, "1/2/06")),
        "1/2/2006 00:00:00",
        "qe5");
    Test.ensureEqual(
        Calendar2.formatAsUSSlash24(Calendar2.parseUSSlash24(zuluGC, "12/31/99")),
        "12/31/1999 00:00:00",
        "qe6");
    try {
      Calendar2.formatAsUSSlash24(null);
      throw new Throwable("Shouldn't get here.37");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseUSSlash24(null, "12/31/99");
      throw new Throwable("Shouldn't get here.38");
    } catch (Exception e) {
    }
    Test.ensureEqual(
        Calendar2.formatAsUSSlash24(Calendar2.parseUSSlash24Zulu("12/31/99")),
        "12/31/1999 00:00:00",
        "qe7");
    try {
      Calendar2.parseUSSlash24Zulu(null);
      throw new Throwable("Shouldn't get here.39");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseUSSlash24Zulu("12");
      throw new Throwable("Shouldn't get here.40");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseUSSlash24Zulu("12/31");
      throw new Throwable("Shouldn't get here.41");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseUSSlash24Zulu("12/31/a");
      throw new Throwable("Shouldn't get here.42");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseUSSlash24Zulu("12-31-99");
      throw new Throwable("Shouldn't get here.43");
    } catch (Exception e) {
    }

    Test.ensureEqual(
        Calendar2.formatAsUSSlashAmPm(Calendar2.parseISODateTimeZulu("2006-01-02 00:03:04")),
        "1/2/2006 12:03:04 am",
        "");
    Test.ensureEqual(
        Calendar2.formatAsUSSlashAmPm(Calendar2.parseISODateTimeZulu("2006-01-02 01:03:00")),
        "1/2/2006 1:03:00 am",
        "");
    Test.ensureEqual(
        Calendar2.formatAsUSSlashAmPm(Calendar2.parseISODateTimeZulu("2006-01-02 12:00:04")),
        "1/2/2006 12:00:04 pm",
        "");
    Test.ensureEqual(
        Calendar2.formatAsUSSlashAmPm(Calendar2.parseISODateTimeZulu("2006-01-02 14:00:00")),
        "1/2/2006 2:00:00 pm",
        "");
    Test.ensureEqual(
        Calendar2.formatAsUSSlashAmPm(Calendar2.parseISODateTimeZulu("0001-01-02 14:00:00")),
        "1/2/0001 2:00:00 pm",
        "");
    Test.ensureEqual(
        Calendar2.formatAsUSSlashAmPm(Calendar2.parseISODateTimeZulu("0000-01-02 14:00:00")),
        "1/2/0000 2:00:00 pm",
        "");
    Test.ensureEqual(
        Calendar2.formatAsUSSlashAmPm(Calendar2.parseISODateTimeZulu("-0001-01-02 14:00:00")),
        "1/2/-0001 2:00:00 pm",
        "");
    try {
      Calendar2.formatAsUSSlashAmPm(null);
      throw new Throwable("Shouldn't get here.44");
    } catch (Exception e) {
    }

    s = "19991231235904";
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.parseCompactDateTime(localGC, s)), s, "rL");
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.parseCompactDateTime(zuluGC, s)), s, "rZ");
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.parseCompactDateTime(zuluGC, "00011231235904")),
        "00011231235904",
        "rZ");
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.parseCompactDateTime(zuluGC, "00001231235904")),
        "00001231235904",
        "rZ");
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(
            Calendar2.parseCompactDateTime(zuluGC, "-00011231235904")),
        "-00011231235904",
        "rZ");
    try {
      Calendar2.formatAsCompactDateTime(null);
      throw new Throwable("Shouldn't get here.45");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseCompactDateTime(null, s);
      throw new Throwable("Shouldn't get here.46");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseCompactDateTime(zuluGC, null);
      throw new Throwable("Shouldn't get here.47");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseCompactDateTime(zuluGC, "");
      throw new Throwable("Shouldn't get here.48");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseCompactDateTime(zuluGC, "1999123");
      throw new Throwable("Shouldn't get here.49");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseCompactDateTime(zuluGC, "1999123a");
      throw new Throwable("Shouldn't get here.50");
    } catch (Exception e) {
    }
    Test.ensureEqual(
        Calendar2.formatAsCompactDateTime(Calendar2.parseCompactDateTimeZulu(s)), s, "rL1");
    try {
      Calendar2.parseCompactDateTimeZulu(null);
      throw new Throwable("Shouldn't get here.51");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseCompactDateTimeZulu("");
      throw new Throwable("Shouldn't get here.52");
    } catch (Exception e) {
    }

    Test.ensureEqual(
        Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarLocal(2003, 1, 2, 3, 4, 5, 6)),
        "02-Jan-2003 03:04:05",
        "hL");
    Test.ensureEqual(
        Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarZulu(2003, 1, 2, 3, 4, 5, 6)),
        "02-Jan-2003 03:04:05",
        "hZ");
    Test.ensureEqual(
        Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarZulu(1, 1, 2, 3, 4, 5, 6)),
        "02-Jan-0001 03:04:05",
        "hZ");
    Test.ensureEqual(
        Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarZulu(0, 1, 2, 3, 4, 5, 6)),
        "02-Jan-0000 03:04:05",
        "hZ");
    Test.ensureEqual(
        Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarZulu(-1, 1, 2, 3, 4, 5, 6)),
        "02-Jan--0001 03:04:05",
        "hZ");
    try {
      Calendar2.parseDDMonYYYYZulu(null);
      throw new Throwable("Shouldn't get here.53");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseDDMonYYYYZulu("02-Jan-200");
      throw new Throwable("Shouldn't get here.54");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseDDMonYYYYZulu("02-Jan-200a 04:05:06");
      throw new Throwable("Shouldn't get here.55");
    } catch (Exception e) {
    }
    try {
      Calendar2.parseDDMonYYYYZulu("02-Jab-2003 04:05:06");
      throw new Throwable("Shouldn't get here.56");
    } catch (Exception e) {
    }
    Test.ensureEqual(
        Calendar2.formatAsDDMonYYYY(Calendar2.parseDDMonYYYYZulu("02-Jan-2003")),
        "02-Jan-2003 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsDDMonYYYY(Calendar2.parseDDMonYYYYZulu("32-DEC-2003 04:05:06")),
        "01-Jan-2004 04:05:06",
        "");

    // Test.ensureEqual(Calendar2.utcToLocal(Calendar2.localToUtc(Calendar2.newGCalendar())),
    // Calendar2.newGCalendar(), "s");
    Test.ensureEqual(Calendar2.yyyydddToIsoDate("2003365"), "2003-12-31", "tL");
    try {
      Calendar2.yyyydddToIsoDate("200336a");
      throw new Throwable("Shouldn't get here.57");
    } catch (Exception e) {
    }
    try {
      Calendar2.yyyydddToIsoDate(null);
      throw new Throwable("Shouldn't get here.58");
    } catch (Exception e) {
    }

    // easy: test that 1970-01-01 is 0
    long mpd = Calendar2.MILLIS_PER_DAY;
    long spd = Calendar2.SECONDS_PER_DAY;
    // Test.ensureEqual(Calendar2.utcToMillis(Calendar2.newGCalendar(1970, 1, 1)),
    // 0, "v1970");

    // test years forward from 1970 (thus okay forward to 2099) (2000 was a leap
    // year)
    Test.ensureEqual(Calendar2.newGCalendarZulu(1971, 1, 1).getTimeInMillis(), 365 * mpd, "v1971");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1972, 1, 1).getTimeInMillis(), (2 * 365) * mpd, "v1972");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1973, 1, 1).getTimeInMillis(),
        (2 * 365 + 366) * mpd,
        "v1973"); // intervening
    // leap
    // day
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1974, 1, 1).getTimeInMillis(), (3 * 365 + 366) * mpd, "v1974");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1975, 1, 1).getTimeInMillis(), (4 * 365 + 366) * mpd, "v1975");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1976, 1, 1).getTimeInMillis(), (5 * 365 + 366) * mpd, "v1976");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1977, 1, 1).getTimeInMillis(),
        (5 * 365 + 2 * 366) * mpd,
        "v1977");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(2000, 1, 1).getTimeInMillis(),
        (23 * 365 + 7 * 366) * mpd,
        "v2000");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(2001, 1, 1).getTimeInMillis(),
        (23 * 365 + 8 * 366) * mpd,
        "v2001");

    // test years backward from 1970 (thus okay back to 1901)
    Test.ensureEqual(Calendar2.newGCalendarZulu(1969, 1, 1).getTimeInMillis(), -365 * mpd, "v1969");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1968, 1, 1).getTimeInMillis(),
        -(365 + 366) * mpd,
        "v1968"); // intervening
    // leap
    // day
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1967, 1, 1).getTimeInMillis(), -(2 * 365 + 366) * mpd, "v1967");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1966, 1, 1).getTimeInMillis(), -(3 * 365 + 366) * mpd, "v1966");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1965, 1, 1).getTimeInMillis(), -(4 * 365 + 366) * mpd, "v1965");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(1964, 1, 1).getTimeInMillis(),
        -(4 * 365 + 2 * 366) * mpd,
        "v1964");

    long m0001 = -62135769600000L;
    Test.ensureEqual(Calendar2.newGCalendarZulu(1, 1, 1).getTimeInMillis(), m0001, "v1");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(0, 1, 1).getTimeInMillis(), m0001 + -366 * mpd, "v0");
    Test.ensureEqual(
        Calendar2.newGCalendarZulu(-1, 1, 1).getTimeInMillis(), m0001 + (-365 - 366) * mpd, "v-1");

    Test.ensureEqual(Calendar2.isoStringToMillis("2005-08-31T16:01:02"), 1125504062000L, "w1");
    try {
      Calendar2.isoStringToMillis(null);
      throw new Throwable("Shouldn't get here.59");
    } catch (Exception e) {
    }
    try {
      Calendar2.isoStringToMillis("");
      throw new Throwable("Shouldn't get here.60");
    } catch (Exception e) {
    }
    Test.ensureEqual(Calendar2.millisToIsoStringTZ(1125504062000L), "2005-08-31T16:01:02Z", "w2");
    try {
      Calendar2.millisToIsoStringTZ(Long.MAX_VALUE);
      throw new Throwable("Shouldn't get here.61");
    } catch (Exception e) {
    }

    // epochSecondsToIsoString
    Test.ensureEqual(Calendar2.isoStringToEpochSeconds("2005-08-31T16:01:02"), 1125504062.0, "x1");
    Test.ensureEqual(Calendar2.isoStringToEpochSeconds("0001-01-01"), m0001 / 1000, "x1");
    Test.ensureEqual(
        Calendar2.isoStringToEpochSeconds("0000-01-01"), m0001 / 1000 - 366 * spd, "x1");
    Test.ensureEqual(
        Calendar2.isoStringToEpochSeconds("-0001-01-01"), m0001 / 1000 + (-365 - 366) * spd, "x1");
    try {
      Calendar2.isoStringToEpochSeconds("");
      throw new Throwable("Shouldn't get here.62");
    } catch (Exception e) {
    }
    try {
      Calendar2.isoStringToEpochSeconds(null);
      throw new Throwable("Shouldn't get here.63");
    } catch (Exception e) {
    }
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(1125504062.0), "2005-08-31T16:01:02Z", "x2");
    try {
      Calendar2.epochSecondsToIsoStringTZ(Double.NaN);
      throw new Throwable("Shouldn't get here.64");
    } catch (Exception e) {
    }
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(1125504062.0), "2005-08-31T16:01:02Z", "x3");

    // epoch hours
    Test.ensureEqual(Calendar2.isoStringToEpochHours("2005-08-31T16:01:02"), 312640, "xa1");
    Test.ensureEqual(Calendar2.isoStringToEpochHours("1970-01-01"), 0, "xa1");
    Test.ensureEqual(Calendar2.isoStringToEpochHours("1969-12-31T23"), -1, "xa1");
    int h0001 = -17259936;
    Test.ensureEqual(Calendar2.isoStringToEpochHours("0001-01-01"), h0001, "xa1");
    Test.ensureEqual(Calendar2.isoStringToEpochHours("0000-01-01"), h0001 + -366 * 24, "xa1");
    Test.ensureEqual(
        Calendar2.isoStringToEpochHours("-0001-01-01"), h0001 + (-365 - 366) * 24, "xa1");
    try {
      Calendar2.isoStringToEpochHours("");
      throw new Throwable("Shouldn't get here.66");
    } catch (Exception e) {
    }
    try {
      Calendar2.isoStringToEpochHours(null);
      throw new Throwable("Shouldn't get here.67");
    } catch (Exception e) {
    }

    // gcToEpochSeconds(GregorianCalendar gc) (test with values above)
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Test.ensureEqual(Calendar2.gcToEpochSeconds(gc), 1125504062.0, "");
    gc = Calendar2.parseISODateTimeZulu("0001-01-01");
    Test.ensureEqual(Calendar2.gcToEpochSeconds(gc), m0001 / 1000, "");
    gc = Calendar2.parseISODateTimeZulu("0000-01-01");
    Test.ensureEqual(Calendar2.gcToEpochSeconds(gc), m0001 / 1000 + -366 * 24 * 3600, "");
    gc = Calendar2.parseISODateTimeZulu("-0001-01-01");
    Test.ensureEqual(Calendar2.gcToEpochSeconds(gc), m0001 / 1000 + (-365 - 366) * 24 * 3600, "");
    try {
      Calendar2.gcToEpochSeconds(null);
      throw new Throwable("Shouldn't get here.69");
    } catch (Exception e) {
    }

    // epochSecondsToGc(double seconds) (test with values above)
    gc = Calendar2.epochSecondsToGc(1125504062.0);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-08-31T16:01:02", "");
    gc = Calendar2.epochSecondsToGc(m0001 / 1000);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "0001-01-01T00:00:00", "");
    gc = Calendar2.epochSecondsToGc(m0001 / 1000 + -366 * 24 * 3600);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "0000-01-01T00:00:00", "");
    gc = Calendar2.epochSecondsToGc(m0001 / 1000 + (-365 - 366) * 24 * 3600);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "-0001-01-01T00:00:00", "");
    try {
      Calendar2.epochSecondsToGc(Double.NaN);
      throw new Throwable("Shouldn't get here.70");
    } catch (Exception e) {
    }

    // test getTimeBaseAndFactor(String tsUnits) {
    String2.log("test getTimeBaseAndFactor(String tsUnits)");
    double[] da = Calendar2.getTimeBaseAndFactor("seconds since 1970-01-01");
    Test.ensureEqual(da[0], 0, "");
    Test.ensureEqual(da[1], 1, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 12)),
        "1970-01-01T00:00:12Z",
        "");
    Test.ensureEqual(Calendar2.epochSecondsToUnitsSince(da[0], da[1], da[0] + 12), 12, "");

    da = Calendar2.getTimeBaseAndFactor("minutes since 1970-01-02");
    Test.ensureEqual(da[0], 86400, "");
    Test.ensureEqual(da[1], 60, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 12)),
        "1970-01-02T00:12:00Z",
        "");
    Test.ensureEqual(Calendar2.epochSecondsToUnitsSince(da[0], da[1], da[0] + 12 * 60), 12, "");

    da = Calendar2.getTimeBaseAndFactor("hours since 1970-01-03Z");
    Test.ensureEqual(da[0], 2 * 86400, "");
    Test.ensureEqual(da[1], 3600, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 12)),
        "1970-01-03T12:00:00Z",
        "");
    Test.ensureEqual(Calendar2.epochSecondsToUnitsSince(da[0], da[1], da[0] + 12 * 3600), 12, "");

    da = Calendar2.getTimeBaseAndFactor("days since 1970-01-04");
    Test.ensureEqual(da[0], 3 * 86400, "");
    Test.ensureEqual(da[1], 86400, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 12)),
        "1970-01-16T00:00:00Z",
        "");
    Test.ensureEqual(Calendar2.epochSecondsToUnitsSince(da[0], da[1], da[0] + 12 * 86400), 12, "");

    da = Calendar2.getTimeBaseAndFactor("months since 1975-06-01");
    Test.ensureEqual(da[0], 1.708128E8, "");
    Test.ensureEqual(da[1], 30 * 86400, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 12)),
        "1976-06-01T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToUnitsSince(
            da[0], da[1], Calendar2.isoStringToEpochSeconds("1976-06-01")),
        12,
        "");

    da = Calendar2.getTimeBaseAndFactor("years since 1975-06-01");
    Test.ensureEqual(da[0], 1.708128E8, "");
    Test.ensureEqual(da[1], 360 * 86400, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 12)),
        "1987-06-01T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToUnitsSince(
            da[0], da[1], Calendar2.isoStringToEpochSeconds("1987-06-01")),
        12,
        "");

    da = Calendar2.getTimeBaseAndFactor("days since 1-1-1"); // test really forgiving
    Test.ensureEqual(da[0], -6.21357696E10, "");
    Test.ensureEqual(da[1], 86400, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 2)),
        "0001-01-03T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToUnitsSince(
            da[0], da[1], Calendar2.isoStringToEpochSeconds("0001-01-03")),
        2,
        "");

    da =
        Calendar2.getTimeBaseAndFactor(
            " julian days  since  July 1, 2000  "); // test odd units and not iso-like
    // base dateTime, spaces before and
    // after
    Test.ensureEqual(da[0], 9.624096E8, "");
    Test.ensureEqual(da[1], 86400, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 2)),
        "2000-07-03T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToUnitsSince(
            da[0], da[1], Calendar2.isoStringToEpochSeconds("2000-07-03")),
        2,
        "");

    // SeaDataNet, astronomical year, Chronological Julian Date
    da =
        Calendar2.getTimeBaseAndFactor(
            "days since -4712-01-01"); // -4713 BC is astronomicalYear=-4712
    Test.ensureEqual(da[0], -2.108668032E11, "");
    Test.ensureEqual(da[1], 86400, "");
    // http://www.julian-date.com/
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.unitsSinceToEpochSeconds(
                da[0], da[1], 2457711.5)), // Chronological JD (CJD) starts at
        // midnight
        "2016-11-18T12:00:00Z",
        "");
    // https://www.hermetic.ch/cal_stud/jdn.htm section 5 says
    // A chronological Julian day number is a count of nychthemerons, assumed
    // to begin at midnight GMT, from the nychthemeron which began at
    // midnight GMT on -4712-01-01 JC.
    // Chronological Julian day number 2,452,952 is the period from
    // midnight GMT on 2003-11-08 CE (Common Era) to the next midnight GMT.
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.unitsSinceToEpochSeconds(
                da[0], da[1], 2452952)), // Chronological JD (CJD) starts at midnight
        "2003-11-08T00:00:00Z",
        "");

    da = Calendar2.getTimeBaseAndFactor("years since 0001-01-01"); // some datasets use this!
    s = "1985-01-01T00:00:00Z";
    d = Calendar2.isoStringToEpochSeconds(s);
    double us = Calendar2.epochSecondsToUnitsSince(da[0], da[1], d);
    String2.log(s + " = " + us + " years since 0001-01-01  base=" + da[0] + " factor=" + da[1]);
    Test.ensureEqual(us, 1984, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], us)),
        s,
        "");

    da = Calendar2.getTimeBaseAndFactor("years since 0000-01-01"); // some datasets use this!
    s = "1985-01-01T00:00:00Z";
    d = Calendar2.isoStringToEpochSeconds(s);
    us = Calendar2.epochSecondsToUnitsSince(da[0], da[1], d);
    String2.log(s + " = " + us + " years since 0000-01-01  base=" + da[0] + " factor=" + da[1]);
    Test.ensureEqual(us, 1985, "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], us)),
        s,
        "");

    // test fractional years since e.g., 15.5 "years since 1970-01-01"
    da = Calendar2.getTimeBaseAndFactor("years since 1970-01-01");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 15.25)),
        "1985-04-01T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.unitsSinceToEpochSeconds(da[0], da[1], -15.25)),
        "1954-10-01T00:00:00Z",
        "");

    // test fractional months since e.g., 2.5 "months since 1970-01-01"
    // I chose to make it consistent (as if 30 days/month) regardless of days in
    // that particular month
    da = Calendar2.getTimeBaseAndFactor("months since 1970-01-01");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 0.25)),
        "1970-01-09T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 1.25)),
        "1970-02-09T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.unitsSinceToEpochSeconds(da[0], da[1], 2.25)),
        "1970-03-09T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.unitsSinceToEpochSeconds(da[0], da[1], -0.25)),
        "1969-12-24T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.unitsSinceToEpochSeconds(da[0], da[1], -1.25)),
        "1969-11-24T00:00:00Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(
            Calendar2.unitsSinceToEpochSeconds(da[0], da[1], -2.25)),
        "1969-10-24T00:00:00Z",
        "");

    try {
      Calendar2.getTimeBaseAndFactor(null);
      throw new Throwable("Shouldn't get here.71");
    } catch (Exception e) {
    }
    try {
      Calendar2.getTimeBaseAndFactor("days 1970-01-04"); // no since
      throw new Throwable("Shouldn't get here.72");
    } catch (Exception e) {
    }
    try {
      Calendar2.getTimeBaseAndFactor("nanos since 1970-01-04"); // nanos not supported
      throw new Throwable("Shouldn't get here.73");
    } catch (Exception e) {
    }
    try {
      Calendar2.getTimeBaseAndFactor("seconds since a"); // 'a' isn't a valid iso datetime
      throw new Throwable("Shouldn't get here.74");
    } catch (Exception e) {
    }

    // test removeSpacesDashesColons
    Test.ensureEqual(
        Calendar2.removeSpacesDashesColons("2000-05-01 23:00:00"), "20000501230000", "");
    Test.ensureEqual(
        Calendar2.removeSpacesDashesColons("2000-05-01T23:00:00"), "20000501230000", "");
    Test.ensureEqual(
        Calendar2.removeSpacesDashesColons("-0001-05-01T23:00:00"), "-00010501230000", "");
    Test.ensureEqual(Calendar2.removeSpacesDashesColons(""), "", "");
    try {
      Calendar2.removeSpacesDashesColons(null);
      throw new Throwable("Shouldn't get here.75");
    } catch (Exception e) {
    }

    // test isoDateTimeAdd
    String2.log("test isoDateTimeAdd");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("2001-02-03", 1, Calendar2.MONTH)),
        "2001-03-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("2001-02-03", -1, Calendar2.MONTH)),
        "2001-01-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("2001-02-03", 2, Calendar2.YEAR)),
        "2003-02-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("2001-02-03", -2, Calendar2.YEAR)),
        "1999-02-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("0001-02-03", -1, Calendar2.YEAR)),
        "0000-02-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("0001-02-03", -2, Calendar2.YEAR)),
        "-0001-02-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("0000-02-03", -1, Calendar2.YEAR)),
        "-0001-02-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("0000-02-03", -2, Calendar2.YEAR)),
        "-0002-02-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("-0001-02-03", 1, Calendar2.YEAR)),
        "0000-02-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("-0001-02-03", 2, Calendar2.YEAR)),
        "0001-02-03 00:00:00",
        "");
    Test.ensureEqual(
        Calendar2.formatAsISODateTimeSpace(
            Calendar2.isoDateTimeAdd("-0001-02-03", -3, Calendar2.MONTH)),
        "-0002-11-03 00:00:00",
        "");
    try {
      Calendar2.isoDateTimeAdd(null, -2, Calendar2.YEAR);
      throw new Throwable("Shouldn't get here.76");
    } catch (Exception e) {
    }
    try {
      Calendar2.isoDateTimeAdd("2001-02-03", Integer.MAX_VALUE, Calendar2.YEAR);
      throw new Throwable("Shouldn't get here.77");
    } catch (Exception e) {
    }
    try {
      Calendar2.isoDateTimeAdd("2001-02-03", -2, Integer.MAX_VALUE);
      throw new Throwable("Shouldn't get here.78");
    } catch (Exception e) {
    }

    // binaryFindClosest
    String tsar1[] = {"0000-03-02", "2000-05-02", "2000-05-04", "2000-05-06"}; // 0000 ok; <0 isn't
    String tsar2[] = {
      "2000-05-02 12:00:00", "2000-05-03 12:00:00", "2000-05-04 12:00:00"
    }; // important tests
    String dupSar1[] = {
      "2000-05-02",
      "2000-05-04",
      "2000-05-04",
      "2000-05-04",
      "2000-05-04",
      "2000-05-04",
      "2000-05-06"
    };
    String dupSar2[] = {
      "2000-05-02 00:00:00",
      "2000-05-04 00:00:00",
      "2000-05-04 00:00:00",
      "2000-05-04 00:00:00",
      "2000-05-04 00:00:00",
      "2000-05-04 00:00:00",
      "2000-05-06 00:00:00"
    };
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar1, "0000-01-01"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar1, "0500-01-01"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar1, "2000-05-01 23:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar1, "2000-05-03 23:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar1, "2000-05-04 00:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar1, "2000-05-04 01:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar1, "2000-05-05 23:00:00"), 3, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar1, "2000-05-07 23:00:00"), 3, "");

    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, "2000-05-01"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, "2000-05-03 23:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, "2000-05-03"), 1, ""); // important
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, "2000-05-03 01:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, "2000-05-04 23:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, "2000-05-05"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, ""), 2, ""); // binaryFinds last in list
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, "a"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(tsar2, null), 2, "");

    Test.ensureEqual(Calendar2.binaryFindClosest(dupSar1, "2000-05-01 00:00:00"), 0, "");
    int i = Calendar2.binaryFindClosest(dupSar1, "2000-05-04");
    Test.ensureTrue(i >= 1 && i <= 5, "");
    i = Calendar2.binaryFindClosest(dupSar1, "2000-05-04 00:00:00");
    Test.ensureTrue(i >= 1 && i <= 5, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(dupSar1, "2000-05-05 01:00:00"), 6, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(dupSar1, "2000-05-07 00:00:00"), 6, "");

    Test.ensureEqual(Calendar2.binaryFindClosest(dupSar2, "2000-05-01"), 0, "");
    i = Calendar2.binaryFindClosest(dupSar2, "2000-05-04");
    Test.ensureTrue(i >= 1 && i <= 5, "");
    i = Calendar2.binaryFindClosest(dupSar2, "2000-05-04 00:00:00");
    Test.ensureTrue(i >= 1 && i <= 5, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(dupSar2, "2000-05-06"), 6, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(dupSar2, "2000-05-07"), 6, "");

    // binaryFindLastLE
    // String tsar1[] = {"0000-03-02", "2000-05-02", "2000-05-04", "2000-05-06"};
    // String tsar2[] = {"2000-05-02 12:00:00", "2000-05-03 12:00:00", "2000-05-04
    // 12:00:00"}; //important tests
    // String dupSar1[] = {"2000-05-02", "2000-05-04", "2000-05-04", "2000-05-04",
    // "2000-05-04", "2000-05-04", "2000-05-06"};
    // String dupSar2[] = {"2000-05-02 00:00:00", "2000-05-04 00:00:00", "2000-05-04
    // 00:00:00",
    // "2000-05-04 00:00:00", "2000-05-04 00:00:00", "2000-05-04 00:00:00",
    // "2000-05-06 00:00:00"};
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, "0000-01-01"), -1, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, "2000-05-01 23:00:00"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, "2000-05-03 23:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, "2000-05-04 00:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, "2000-05-04 01:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, "2000-05-05 23:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, "2000-05-07 23:00:00"), 3, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, ""), -1, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, "a"), -1, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar1, null), -1, "");

    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar2, "2000-05-01"), -1, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar2, "2000-05-02 23:00:00"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar2, "2000-05-03"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar2, "2000-05-03 01:00:00"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar2, "2000-05-03 12:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(tsar2, "2000-05-05"), 2, "");

    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar1, "2000-05-01"), -1, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar1, "2000-05-03"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar1, "2000-05-04"), 5, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar1, "2000-05-04 00:00:00"), 5, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar1, "2000-05-05"), 5, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar1, "2000-05-07"), 6, "");

    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar2, "2000-05-01"), -1, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar2, "2000-05-03"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar2, "2000-05-04"), 5, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar2, "2000-05-04 00:00:00"), 5, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar2, "2000-05-05"), 5, "");
    Test.ensureEqual(Calendar2.binaryFindLastLE(dupSar2, "2000-05-07"), 6, "");

    // binaryFindLastGE
    // String tsar1[] = {"0000-03-02", "2000-05-02", "2000-05-04", "2000-05-06"};
    // String tsar2[] = {"2000-05-02 12:00:00", "2000-05-03 12:00:00", "2000-05-04
    // 12:00:00"}; //important tests
    // String dupSar1[] = {"2000-05-02", "2000-05-04", "2000-05-04", "2000-05-04",
    // "2000-05-04", "2000-05-04", "2000-05-06"};
    // String dupSar2[] = {"2000-05-02 00:00:00", "2000-05-04 00:00:00", "2000-05-04
    // 00:00:00",
    // "2000-05-04 00:00:00", "2000-05-04 00:00:00", "2000-05-04 00:00:00",
    // "2000-05-06 00:00:00"};
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, "0000-01-01"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, "2000-05-01 23:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, "2000-05-03 23:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, "2000-05-04 00:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, "2000-05-04 01:00:00"), 3, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, "2000-05-05 23:00:00"), 3, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, "2000-05-07 23:00:00"), 4, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, ""), tsar1.length, ""); //
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, "a"), tsar1.length, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar1, null), tsar1.length, "");

    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar2, "2000-05-01"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar2, "2000-05-02 23:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar2, "2000-05-03"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar2, "2000-05-03 01:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar2, "2000-05-03 12:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar2, "2000-05-03 13:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(tsar2, "2000-05-05"), 3, "");

    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar1, "2000-05-01"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar1, "2000-05-03"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar1, "2000-05-04 00:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar1, "2000-05-04"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar1, "2000-05-05"), 6, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar1, "2000-05-07"), 7, "");

    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar2, "2000-05-01"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar2, "2000-05-03"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar2, "2000-05-04 00:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar2, "2000-05-04"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar2, "2000-05-05"), 6, "");
    Test.ensureEqual(Calendar2.binaryFindFirstGE(dupSar2, "2000-05-07"), 7, "");

    // test binaryFindClosest
    String activeTimeOptions[] = {
      "2005-12-31 12:00:00", "2006-01-01 13:00:00", "2006-01-01 14:00:00", "2006-02-05 02:00:00"
    };
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2001-12-31 12:00:00"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2005-12-31 11:00:00"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2005-12-31 12:00:00"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2005-12-31 13:00:00"), 0, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-01-01 12:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-01-01 13:00:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-01-01 13:29:00"), 1, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-01-01 13:31:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-01-01 14:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-01-01 15:00:00"), 2, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-01-31 14:00:00"), 3, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-02-05 02:00:00"), 3, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, "2006-03-01 14:00:00"), 3, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, ""), 3, "");
    Test.ensureEqual(Calendar2.binaryFindClosest(activeTimeOptions, null), 3, "");

    // elapsedTimeString
    Test.ensureEqual(Calendar2.elapsedTimeString(0), "0 ms", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(765), "765 ms", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(4567), "4.567 s", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(4000), "4.000 s", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(224567), "3m 44s", ""); // was "00:03:44.567", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(224000), "3m 44s", ""); // was "00:03:44", "");
    Test.ensureEqual(
        Calendar2.elapsedTimeString(2 * Calendar2.MILLIS_PER_HOUR + 3004), "2h 0m 3s", ""); // was
    // "02:00:03.004",
    // "");
    Test.ensureEqual(Calendar2.elapsedTimeString(63004), "1m 3s", "");
    Test.ensureEqual(
        Calendar2.elapsedTimeString(3 * Calendar2.MILLIS_PER_DAY + 4005),
        "3 days 0h 0m 4s",
        ""); // was
    // "3
    // days
    // 00:00:04.005",
    // "");
    Test.ensureEqual(Calendar2.elapsedTimeString(1 * Calendar2.MILLIS_PER_DAY), "1 day", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(2 * Calendar2.MILLIS_PER_DAY), "2 days", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(Long.MAX_VALUE), "infinity", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(Double.NaN), "infinity", "");

    Test.ensureEqual(Calendar2.elapsedTimeString(0), "0 ms", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(-765), "-765 ms", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(-4567), "-4.567 s", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(-4000), "-4.000 s", "");
    Test.ensureEqual(
        Calendar2.elapsedTimeString(-224567), "-3m 44s", ""); // was "-00:03:44.567", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(-224000), "-3m 44s", ""); // was "-00:03:44", "");
    Test.ensureEqual(
        Calendar2.elapsedTimeString(-2 * Calendar2.MILLIS_PER_HOUR - 3004), "-2h 0m 3s", ""); // was
    // "-02:00:03.004",
    // "");
    Test.ensureEqual(Calendar2.elapsedTimeString(-63004), "-1m 3s", "");
    Test.ensureEqual(
        Calendar2.elapsedTimeString(-3 * Calendar2.MILLIS_PER_DAY - 4005),
        "-3 days 0h 0m 4s",
        ""); // was
    // "-3
    // days
    // 00:00:04.005",
    // "");
    Test.ensureEqual(Calendar2.elapsedTimeString(-1 * Calendar2.MILLIS_PER_DAY), "-1 day", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(-2 * Calendar2.MILLIS_PER_DAY), "-2 days", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(Long.MIN_VALUE), "infinity", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(Long.MAX_VALUE - 1), "infinity", "");
    Test.ensureEqual(Calendar2.elapsedTimeString(-Double.NaN), "infinity", "");

    // clearSmallerFields
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.MILLISECOND);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-08-31T16:01:02", "");
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.SECOND);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-08-31T16:01:02", "");
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.MINUTE);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-08-31T16:01:00", "");
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.HOUR);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-08-31T16:00:00", "");
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.HOUR_OF_DAY);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-08-31T16:00:00", "");
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.DATE);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-08-31T00:00:00", "");
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.MONTH);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-08-01T00:00:00", "");
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.DAY_OF_YEAR);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-01-01T00:00:00", "");
    gc = Calendar2.parseISODateTimeZulu("2005-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.YEAR);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "2005-01-01T00:00:00", "");
    gc = Calendar2.parseISODateTimeZulu("0000-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.MINUTE);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "0000-08-31T16:01:00", "");
    gc = Calendar2.parseISODateTimeZulu("-0000-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.MINUTE);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "0000-08-31T16:01:00", "");
    gc = Calendar2.parseISODateTimeZulu("-0001-08-31T16:01:02");
    Calendar2.clearSmallerFields(gc, Calendar2.MINUTE);
    Test.ensureEqual(Calendar2.formatAsISODateTimeT(gc), "-0001-08-31T16:01:00", "");
    try {
      Calendar2.clearSmallerFields(gc, Calendar2.AM_PM);
      throw new Throwable("Shouldn't get here.79");
    } catch (Exception e) {
    }

    // is System.currentTimeMillis()/1000 = current epochSeconds? (system might use
    // local time)
    double systemSec = System.currentTimeMillis() / 1000;
    double epochSec = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu());
    Test.ensureTrue(
        Math.abs(systemSec - epochSec) < 1, "systemSec=" + systemSec + " epochSec=" + epochSec);

    // epochSecondsToLimitedIsoStringT
    s = "2005-08-31T16:01:02.123";
    d = Calendar2.isoStringToEpochSeconds(s);
    String2.log(s + " = " + d);
    Test.ensureEqual(Calendar2.epochSecondsToLimitedIsoStringT("1970", Double.NaN, "."), ".", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT(null, d, "."), "2005-08-31T16:01:02Z", "");
    Test.ensureEqual(Calendar2.epochSecondsToLimitedIsoStringT("1970", d, "."), "2005", "");
    Test.ensureEqual(Calendar2.epochSecondsToLimitedIsoStringT("1970-01", d, "."), "2005-08", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01", d, "."), "2005-08-31", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00", d, "."), "2005-08-31T16", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00Z", d, "."), "2005-08-31T16Z", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00", d, "."),
        "2005-08-31T16:01",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00Z", d, "."),
        "2005-08-31T16:01Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00", d, "."),
        "2005-08-31T16:01:02",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00Z", d, "."),
        "2005-08-31T16:01:02Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.0", d, "."),
        "2005-08-31T16:01:02.1",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.0Z", d, "."),
        "2005-08-31T16:01:02.1Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.00", d, "."),
        "2005-08-31T16:01:02.12",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.00Z", d, "."),
        "2005-08-31T16:01:02.12Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.000", d, "."),
        "2005-08-31T16:01:02.123",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.000Z", d, "."),
        "2005-08-31T16:01:02.123Z",
        "");

    // test negative year
    s = "-0005-08-31T16:01:02.123";
    d = Calendar2.isoStringToEpochSeconds(s);
    String2.log(s + " = " + d);
    Test.ensureEqual(Calendar2.epochSecondsToLimitedIsoStringT("1970", Double.NaN, "."), ".", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT(null, d, "."), "-0005-08-31T16:01:02Z", "");
    Test.ensureEqual(Calendar2.epochSecondsToLimitedIsoStringT("1970", d, "."), "-0005", "");
    Test.ensureEqual(Calendar2.epochSecondsToLimitedIsoStringT("1970-01", d, "."), "-0005-08", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01", d, "."), "-0005-08-31", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00", d, "."), "-0005-08-31T16", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00Z", d, "."), "-0005-08-31T16Z", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00", d, "."),
        "-0005-08-31T16:01",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00Z", d, "."),
        "-0005-08-31T16:01Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00", d, "."),
        "-0005-08-31T16:01:02",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00Z", d, "."),
        "-0005-08-31T16:01:02Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.0", d, "."),
        "-0005-08-31T16:01:02.1",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.0Z", d, "."),
        "-0005-08-31T16:01:02.1Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.00", d, "."),
        "-0005-08-31T16:01:02.12",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.00Z", d, "."),
        "-0005-08-31T16:01:02.12Z",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.000", d, "."),
        "-0005-08-31T16:01:02.123",
        "");
    Test.ensureEqual(
        Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01T00:00:00.000Z", d, "."),
        "-0005-08-31T16:01:02.123Z",
        "");

    Math2.gcAndWait("TestUtil (between tests)"); // in test
  }

  @org.junit.jupiter.api.Test
  @TagFlaky
  void testCalendar2Now_flaky() {
    // nowStringToEpochSeconds(String nowString)
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now"), nextEpochSecond(), "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now-0"), nextEpochSecond(), "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now+0"), nextEpochSecond(), "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now+35"), nextEpochSecond() + 35, "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now 36"), nextEpochSecond() + 36, "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now-0ms"), nextEpochSecond(), "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now+0millis"), nextEpochSecond(), "");
    Test.ensureEqual(
        Calendar2.nowStringToEpochSeconds("now-2millis"), nextEpochSecond() - 0.002, "");
    Test.ensureEqual(
        Calendar2.nowStringToEpochSeconds("now+2millis"), nextEpochSecond() + 0.002, "");
    Test.ensureEqual(
        Calendar2.nowStringToEpochSeconds("now+2millisecond"), nextEpochSecond() + 0.002, "");
    Test.ensureEqual(
        Calendar2.nowStringToEpochSeconds("now-2milliseconds"), nextEpochSecond() - 0.002, "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now+second"), nextEpochSecond() + 1, "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now-2second"), nextEpochSecond() - 2, "");
    Test.ensureEqual(
        Calendar2.nowStringToEpochSeconds("now 34seconds"), nextEpochSecond() + 34, "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now-2minute"), nextEpochSecond() - 120, "");
    Test.ensureEqual(
        Calendar2.nowStringToEpochSeconds("now+3minutes"), nextEpochSecond() + 180, "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now-2hour"), nextEpochSecond() - 7200, "");
    Test.ensureEqual(
        Calendar2.nowStringToEpochSeconds("now+3hours"), nextEpochSecond() + 10800, "");
    Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now+day"), nextEpochSecond() + 86400, "");
    Test.ensureEqual(
        Calendar2.nowStringToEpochSeconds("now-3days"), nextEpochSecond() - 259200, "");
    // Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now-3month"),
    // nextEpochSecond()-, "");
    // Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now+4months"),
    // nextEpochSecond()-1, "");
    // Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now-2year"),
    // nextEpochSecond()-1, "");
    // Test.ensureEqual(Calendar2.nowStringToEpochSeconds("now-2years"),
    // nextEpochSecond()-1, "");
  }

  /** Test the methods in MustBe. */
  @org.junit.jupiter.api.Test
  void testMustBe() {
    String2.log("\n*** TestUtil.testMustBe");

    // getStackTrace
    String2.log("test getStackTrace");
    String2.log("intentional trace=" + MustBe.getStackTrace());

    // printStackTrace
    String2.log("test printStackTrace");
    System.err.print("intentional trace=");
    MustBe.printStackTrace();

    try {
      throw new Exception("I threw this exception!");
    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
    }
  }

  /** Test the methods in ResourceBundle2. */
  @org.junit.jupiter.api.Test
  void testResourceBundle2() {
    String2.log("\n*** TestUtil.testResourceBundle2");

    ResourceBundle2 rb2 = new ResourceBundle2("com.cohort.util.TestResourceBundle2");
    Test.ensureEqual(rb2.getBoolean("boolean", false), true, "boolean");
    Test.ensureEqual(
        rb2.getBoolean("booleaX", false), false, "boolean"); // X tests use of default value
    Test.ensureEqual(rb2.getInt("int", 999), 12345678, "int");
    Test.ensureEqual(rb2.getInt("inX", 999), 999, "int");
    Test.ensureEqual(rb2.getLong("long", 123), 123456789876L, "long");
    Test.ensureEqual(rb2.getLong("lonX", 123), 123, "long");
    Test.ensureEqual(rb2.getDouble("double", 1e300), 1.234e19, "double");
    Test.ensureEqual(rb2.getDouble("doublX", 1e300), 1e300, "double");
    Test.ensureEqual(rb2.getString("String", "abc"), "Nate", "String"); // tests split line
    Test.ensureEqual(rb2.getString("StrinX", "abc"), "abc", "String");
    String defaultArray[] = new String[] {"a", "b"};
    Test.ensureEqual(
        rb2.getStringArray("StringArray", defaultArray),
        new String[] {"line 1", "line 2", "line 3"},
        "StringArray");
    Test.ensureEqual(rb2.getStringArray("StringArraX", defaultArray), defaultArray, "StringArray");
    Test.ensureEqual(
        String2.toCSSVString(rb2.getKeys()), "boolean, double, int, long, String, StringArray", "");

    // test 2 resource bundles
    rb2 =
        new ResourceBundle2(
            "com.cohort.util.TestResourceBundle2", "com.cohort.util.DefaultResourceBundle2");
    // in primary and secondary
    Test.ensureEqual(rb2.getInt("int", 999), 12345678, "");
    // only in primary
    Test.ensureEqual(rb2.getLong("long", 123), 123456789876L, "");
    // only in secondary
    Test.ensureEqual(rb2.getInt("int3", 999), 33333, "");
    // not in either
    Test.ensureEqual(rb2.getInt("inX", 999), 999, "");
    Test.ensureEqual(
        String2.toCSSVString(rb2.getKeys()),
        "boolean, double, int, int, int3, long, String, StringArray",
        "");
  }

  /** Test the methods in File2. */
  @org.junit.jupiter.api.Test
  void testFile2() throws Exception {
    String2.log("\n*** TestUtil.testFile2");

    Test.ensureEqual(File2.getExtension("a/b.c"), ".c", "");
    Test.ensureEqual(File2.getExtension("a.z/b"), "", "");
    Test.ensureEqual(File2.getExtension("a/b.c/"), "", "");

    Test.ensureEqual(File2.removeExtension("a/b.c"), "a/b", "");
    Test.ensureEqual(File2.removeExtension("a.z/b"), "a.z/b", "");
    Test.ensureEqual(File2.removeExtension("a/b.c/"), "a/b.c/", "");

    // The following tests must all be done and must be done in this sequence.
    // make a temp file to work with
    String utilDir = TEMP_DIR.toAbsolutePath().toString() + "/";
    Test.ensureEqual(
        File2.writeToFile88591(utilDir + "temp.txt", "This\nis a\n\ntest.\n"), "", "writeToFile");

    Test.ensureEqual(
        File2.writeToFile88591(utilDir + "temp2.txt", "This\nis another\n\ntest.\n"),
        "",
        "writeToFile2");

    Test.ensureEqual(File2.writeToFile88591(utilDir + "temp3.txt", "This\nis"), "", "writeToFile3");

    // test whereDifferent
    String2.log("test whereDifferent");
    Test.ensureEqual(File2.whereDifferent(utilDir + "temp.txt", utilDir + "temp.txt"), -1, "a");
    Test.ensureEqual(
        File2.whereDifferent(utilDir + "temp.txt", utilDir + "temp2.txt"), 9, "b"); // different
    // character
    Test.ensureEqual(
        File2.whereDifferent(utilDir + "temp.txt", utilDir + "temp3.txt"), 7, "b"); // different
    // length;
    // otherwise the
    // same

    // test hexDump
    String2.log("test hexDump");
    Test.ensureEqual(
        File2.hexDump(utilDir + "temp.txt", 256),
        "54 68 69 73 0a 69 73 20   61 0a 0a 74 65 73 74 2e   This is a  test. |\n"
            + "0a                                                                   |\n",
        "test hexDump failed.");
    Test.ensureEqual(File2.length(utilDir + "temp.txt"), 17, "length");

    // and make sure temp.gibberish doesn't exist
    File2.delete(utilDir + "temp.gibberish");

    // test boolean isFile(String dirName) {
    String2.log("test isFile");
    Test.ensureEqual(File2.isFile(utilDir + "temp.txt"), true, "a");
    Test.ensureEqual(File2.isFile(utilDir + "temp.gibberish"), false, "b");

    // test boolean isDirectory(String dir) {
    String2.log("test isDirectory");
    Test.ensureEqual(File2.isDirectory(utilDir), true, "a");
    Test.ensureEqual(File2.isDirectory(utilDir + "gibberish"), false, "b");

    // test String getDirectory(String dirName) {
    String2.log("test getDirectory");
    Test.ensureEqual(File2.getDirectory(utilDir + "temp.gibberish"), utilDir, "a");

    // test String nameAndExtension(String dirName) {
    String2.log("test nameAndExtension");
    Test.ensureEqual(File2.getNameAndExtension(utilDir + "temp.gibberish"), "temp.gibberish", "a");

    // test boolean rename(String dir, String oldName, String newName) {
    String2.log("test rename(dir, old, new)");
    File2.rename(utilDir, "temp.txt", "temp.gibberish");
    Test.ensureEqual(File2.isFile(utilDir + "temp.txt"), false, "a");
    Test.ensureEqual(File2.isFile(utilDir + "temp.gibberish"), true, "b");

    // test boolean rename(String fullOldName, String fullNewName)
    // sleep first, so the following tests go faster
    String2.log("test rename(old, new)");
    File2.rename(utilDir + "temp.gibberish", utilDir + "temp.txt");
    Test.ensureEqual(File2.isFile(utilDir + "temp.txt"), true, "a");
    Test.ensureEqual(File2.isFile(utilDir + "temp.gibberish"), false, "b");

    // test boolean touch(String dirName) and getLastModified
    String2.log("test touch and getLastModified");
    Math2.gc("TestUtil (between tests)", 1000);
    Math2.gc("TestUtil (between tests)", 1000);
    File2.writeToFile88591(utilDir + "temp.txt", "This\nis a\n\ntest.\n");
    Math2.sleep(20); // make the file a little older
    long fileTime = File2.getLastModified(utilDir + "temp.txt");
    long time1 = System.currentTimeMillis();
    Test.ensureTrue(time1 >= fileTime + 10, "a1 " + time1 + " " + fileTime);
    Test.ensureTrue(
        time1 <= fileTime + 50,
        "a2 " + time1 + " " + fileTime + "\nThis fails when the computer is busy.");
    Test.ensureTrue(File2.touch(utilDir + "temp.txt"), "a"); // touch the file
    long time2 = System.currentTimeMillis();
    fileTime = File2.getLastModified(utilDir + "temp.txt");
    Test.ensureTrue(fileTime >= time1, "b");
    Test.ensureTrue(fileTime <= time2, "c");
    Test.ensureEqual(File2.touch(utilDir + "temp.gibberish"), false, "d");

    // test boolean delete(String dirName) {
    String2.log("test delete");
    Test.ensureEqual(File2.isFile(utilDir + "temp2.txt"), true, "a");
    Test.ensureEqual(File2.delete(utilDir + "temp2.txt"), true, "b");
    Test.ensureEqual(File2.isFile(utilDir + "temp2.txt"), false, "c");
    Test.ensureEqual(File2.delete(utilDir + "temp.gibberish"), false, "d"); // doesn't exist

    // test copy
    String2.log("test copy");
    Test.ensureEqual(File2.copy(utilDir + "temp.txt", utilDir + "temp2.txt"), true, "a");
    Test.ensureEqual(
        File2.whereDifferent(utilDir + "temp.txt", utilDir + "temp2.txt"), -1, "b"); // different
    // character
    Test.ensureEqual(File2.delete(utilDir + "temp2.txt"), true, "c");

    // delete the temp.txt file
    Test.ensureEqual(File2.delete(utilDir + "temp.txt"), true, "d");
    Test.ensureEqual(File2.delete(utilDir + "temp3.txt"), true, "e");

    // test getSystemTempDirectory
    // this only works on Bob's computer
    String2.log("File2.getSystemTempDirectory()=" + File2.getSystemTempDirectory());
    String tempDir = File2.getSystemTempDirectory();
    // if (!tempDir.equals("C:/Users/Bob.Simons/AppData/Local/Temp/") &&
    // !tempDir.equals("C:/Users/Robert/AppData/Local/Temp/")) {
    // String2.log(
    // "getSystemTempDirectory =" + tempDir);
    // //+ "\n" + String2.Press_CtrlC_or_Enter);
    // Math2.gc(5000); //pause in test to display info
    // }

    // test int deleteIfOld(String dir, long time) {
    // make dir in tempDir
    String tTempDir = File2.addSlash(tempDir) + "comCohortUtilTest/";
    File2.makeDirectory(tTempDir); // throws Exception
    // make a file
    File2.writeToFile88591(tTempDir + "test1.txt", "test1.txt");
    Math2.sleep(100);
    long midTime = System.currentTimeMillis();
    Math2.sleep(100);
    // make a file
    File2.writeToFile88591(tTempDir + "test2.txt", "test2.txt");
    Math2.sleep(100);
    // delete based on midTime
    Test.ensureEqual(
        File2.deleteIfOld(tTempDir, midTime, true, true),
        1,
        "Unexpected nFiles remaining after initial File2.deleteIfOld(" + tTempDir + ")");
    // delete based on currentTime
    Test.ensureEqual(
        File2.deleteIfOld(tTempDir, System.currentTimeMillis(), true, true),
        0,
        "Unexpected nFiles remaining after initial File2.deleteIfOld(" + tTempDir + ")");

    // delete tTempDir
    Test.ensureTrue(File2.simpleDelete(tTempDir), "");

    // ensurePrintable
    Test.ensurePrintable("test123\n\t ~¡ÿ", "ensurePrintable");
    try {
      Test.ensurePrintable("test123\n\t ~¡ÿ’", "ensurePrintable");
      Test.error(String2.ERROR + ": previous line should have failed.");
    } catch (Exception e) {
    }

    // addSlash
    String2.log("test addSlash");
    Test.ensureEqual(File2.addSlash("a\\"), "a\\", "a"); // already has slash
    Test.ensureEqual(File2.addSlash("b/"), "b/", "b");
    Test.ensureEqual(File2.addSlash("c\\c"), "c\\c\\", "c"); // needs a slash
    Test.ensureEqual(File2.addSlash("/d"), "/d/", "d");

    // forceExtension
    String2.log("test forceExtension");
    Test.ensureEqual(File2.forceExtension("a/b/c.das", ".txt"), "a/b/c.txt", "a");
    Test.ensureEqual(File2.forceExtension("a/b/c", ".txt"), "a/b/c.txt", "b");
    Test.ensureEqual(File2.forceExtension("a/b/c.d.das", ".txt"), "a/b/c.d.txt", "c");
    Test.ensureEqual(File2.forceExtension("a/b/c.das", ""), "a/b/c", "d");

    // getProtocolDomain
    String2.log("test getProtocolDomain");
    Test.ensureEqual(File2.getProtocolDomain(null), null, "");
    Test.ensureEqual(File2.getProtocolDomain(""), "", "");
    Test.ensureEqual(File2.getProtocolDomain("http://aa/bb"), "http://aa", "");
    Test.ensureEqual(File2.getProtocolDomain("http://a/b"), "http://a", "");
    Test.ensureEqual(File2.getProtocolDomain("http://a/"), "http://a", "");
    Test.ensureEqual(File2.getProtocolDomain("http://a"), "http://a", "");
    Test.ensureEqual(File2.getProtocolDomain("http://a"), "http://a", "");
    Test.ensureEqual(File2.getProtocolDomain("http://"), "http://", "");
    Test.ensureEqual(File2.getProtocolDomain("http:/"), "http:", "");
    Test.ensureEqual(File2.getProtocolDomain("http:"), "http:", "");
    Test.ensureEqual(File2.getProtocolDomain("aa/b/"), "aa", "");
    Test.ensureEqual(File2.getProtocolDomain("a/b/"), "a", "");
    Test.ensureEqual(File2.getProtocolDomain("a/"), "a", "");
    Test.ensureEqual(File2.getProtocolDomain("a"), "a", "");
    Test.ensureEqual(File2.getProtocolDomain("a/b"), "a", "");
    Test.ensureEqual(File2.getProtocolDomain("a/"), "a", "");
    Test.ensureEqual(File2.getProtocolDomain("//aa/bb"), "//aa", "");
    Test.ensureEqual(File2.getProtocolDomain("//a/b"), "//a", "");
    Test.ensureEqual(File2.getProtocolDomain("//a"), "//a", "");
    Test.ensureEqual(File2.getProtocolDomain("//"), "//", "");
    Test.ensureEqual(File2.getProtocolDomain("/aa/bb"), "", "");
    Test.ensureEqual(File2.getProtocolDomain("/a/b"), "", "");
    Test.ensureEqual(File2.getProtocolDomain("/a"), "", "");
    Test.ensureEqual(File2.getProtocolDomain("/"), "", "");

    // test File2.getFileInputStream(
    String path = "test-data/data";
    File file = new File(path);
    String absolutePath = file.getAbsolutePath();
    String utff = absolutePath + "/compressed/AUTF8File";
    String sd = absolutePath + "/compressed/subdir";
    String sar[];
    String shouldBe =
        "This is a UTF-8 file with special characters like [255], [8364], [968], [8644], and [24179][25104]![10]\n[end]";

    sar = File2.readFromFile(utff + ".txt", File2.UTF_8, 1);
    String2.log("?" + String2.annotatedString(sar[1]) + "?");
    Test.ensureEqual(sar[0], "", "error msg:" + sar[0]);
    Test.ensureEqual(String2.annotatedString(sar[1]), shouldBe, "sar[1]=" + sar[1]);

    sar = File2.readFromFile(utff + ".txt.bz2", File2.UTF_8, 1);
    Test.ensureEqual(sar[0], "", "error msg:" + sar[0]);
    Test.ensureEqual(String2.annotatedString(sar[1]), shouldBe, "sar[1]=" + sar[1]);

    sar = File2.readFromFile(utff + ".txt.gz", File2.UTF_8, 1);
    Test.ensureEqual(sar[0], "", "error msg:" + sar[0]);
    Test.ensureEqual(String2.annotatedString(sar[1]), shouldBe, "sar[1]=" + sar[1]);

    // more complicated. not supported.
    // sar = File2.readFromFile(sd + ".7z", File2.UTF_8, 1);
    // Test.ensureEqual(sar[0], "", "error msg:" + sar[0]);
    // Test.ensureEqual(String2.annotatedString(sar[1]), shouldBe, "sar[1]=" +
    // sar[1]);

    sar = File2.readFromFile(sd + ".tar.gz", File2.UTF_8, 1);
    Test.ensureEqual(sar[0], "", "error msg:" + sar[0]);
    Test.ensureEqual(String2.annotatedString(sar[1]), shouldBe, "sar[1]=" + sar[1]);

    sar = File2.readFromFile(sd + ".tgz", File2.UTF_8, 1);
    Test.ensureEqual(sar[0], "", "error msg:" + sar[0]);
    Test.ensureEqual(String2.annotatedString(sar[1]), shouldBe, "sar[1]=" + sar[1]);

    sar = File2.readFromFile(sd + ".zip", File2.UTF_8, 1);
    Test.ensureEqual(sar[0], "", "error msg:" + sar[0]);
    Test.ensureEqual(String2.annotatedString(sar[1]), shouldBe, "sar[1]=" + sar[1]);

    // sar = File2.readFromFile(sd + ".zztop", File2.UTF_8, 1); //good: java's msg
    // displays the file name
    // Test.ensureEqual(sar[0], "", "error msg:" + sar[0]);
    // Test.ensureEqual(String2.annotatedString(sar[1]), shouldBe, "sar[1]=" +
    // sar[1]);

    // test of .Z is EDDGridFromMergeIR.testMergeIR() which accesses .Z files

    File2.delete(utilDir + "temp.txt");
    File2.delete(utilDir + "temp2.txt");
    File2.delete(utilDir + "temp3.txt");
  }

  /** Test storing longs in double. What range is completely accurate? */
  @org.junit.jupiter.api.Test
  void storeLongsInDoubles() {
    // note that long range is about -9e18
    // by my crude calculations,
    // I only need 14 digits if I store millis since 1970-01-01
    // or 11 if I store seconds since 1970-01-01
    // to support a very wide range of dates (100's of years before and after).
    // IEEE 754 doubles have 11 bits for exponent, 1 sign bit, leaving 52 for data.
    // 2^52 is about 4e15.
    // Yes I could use Double.longBitsToDouble, but that makes the files
    // very Java-centric.

    // brute force can only check so many numbers
    long max = 1000000000000L; // 10^12
    String2.log(
        "storeLongsInDoubles max=" + max + " Math.pow(2.0, 52)=" + Math.pow(2.0, 52)); // 4e15
    for (long i = -max; i < max; i += 1000000)
      Test.ensureEqual(Math.round((double) i), i, "storeLongsInDoubles");
    String2.log("storeLongsInDoubles finished successfully");

    // random adds insurance that it will work
    Random random = new Random();
    for (int i = 0; i < 1000000; i++) {
      long j = random.nextLong();
      if (Math.abs(j) <= max) {
        Test.ensureEqual(Math.round((double) j), j, "storeLongsInDoubles random");
        Test.ensureEqual(Math.round((double) -j), -j, "storeLongsInDoubles random");
      }
    }
    String2.log("storeLongsInDoubles random finished successfully");
  }

  private static int byteIndexOf(String s, String find) throws Exception {
    byte findB[] = String2.stringToUtf8Bytes(find);
    byte sB[] = String2.stringToUtf8Bytes(s);
    int jump[] = String2.makeJumpTable(findB);
    return String2.indexOf(sB, findB, jump);
  }

  /** Test String2.indexOf(byte[]) This throws an exception if an error occurs */
  @org.junit.jupiter.api.Test
  void testByteIndexOf() throws Exception {
    String2.log("TestUtil.testByteIndexOf");

    // test makeJumpTable
    String s, find = "nine"; // has duplicate char; should catch the 2nd one
    byte findBytes[] = String2.stringToUtf8Bytes(find);
    int jump[] = String2.makeJumpTable(findBytes);
    for (int i = 0; i < 256; i++) {
      if (i == 'n') Test.ensureEqual(jump[i], 1, "");
      else if (i == 'i') Test.ensureEqual(jump[i], 2, "");
      else if (i == 'e') Test.ensureEqual(jump[i], 0, "");
      else Test.ensureEqual(jump[i], 4, "");
    }

    // test indexOf
    Test.ensureEqual(byteIndexOf("nine is fine", "nine"), 0, "");
    Test.ensureEqual(byteIndexOf("is nine fine", "nine"), 3, "");
    Test.ensureEqual(byteIndexOf("fine is nine", "nine"), 8, "");
    Test.ensureEqual(byteIndexOf("dine is fine", "nine"), -1, "");
    Test.ensureEqual(byteIndexOf("fine is nine", "f"), 0, "");
    Test.ensureEqual(byteIndexOf("fine is nine", "n"), 2, "");
    Test.ensureEqual(byteIndexOf("fine is niny", "y"), 11, "");
    Test.ensureEqual(byteIndexOf("fine is niny", "x"), -1, "");
    Test.ensureEqual(byteIndexOf("", "nine"), -1, "");
    Test.ensureEqual("fine".indexOf(""), 0, ""); // test and match behavior of String.indexOf
    Test.ensureEqual("".indexOf(""), 0, "");
    Test.ensureEqual("".indexOf("a"), -1, "");
    Test.ensureEqual(byteIndexOf("fine", ""), 0, "");
    Test.ensureEqual(byteIndexOf("", ""), 0, "");
    Test.ensureEqual(byteIndexOf("", "a"), -1, "");

    // randomized test
    for (int i = 0; i < 100; i++) {
      s = "" + Math2.random(Integer.MAX_VALUE);
      find = "" + Math2.random(100);
      String msg = "i=" + i + " s=" + s + " find=" + find;
      if (s.indexOf(find) >= 0) String2.log(msg);
      Test.ensureEqual(s.indexOf(find), byteIndexOf(s, find), msg);
    }

    // speed test
    find = "9charniño"; // search has utf8-able char
    findBytes = String2.stringToUtf8Bytes(find);
    s =
        "<att>The TAO/TRITON array consists of approximately 70 moorings in the \n"
            + "Tropical Pacific Ocean, telemetering oceanographic and \n"
            + "meteorological data to shore in real-time via the Argos satellite \n"
            + "system.  The array is a major component of the El Niño/Southern \n"
            + "Oscillation (ENSO) Observing System, the Global Climate Observing \n"
            + "System (GCOS) and the Global Ocean Observing System (GOOS). \n"
            + "Support is provided primarily by the United States (National \n"
            + "Oceanic and Atmospheric Administration) and Japan (Japan Agency \n"
            + "for Marine-earth Science and TEChnology) with additional \n"
            + "contributions from France (Institut de recherche pour le \n"
            + "developpement). </att>\n";
    s = s.toLowerCase();
    while (s.length() < 10000) s += s;
    s += find;
    byte sBytes[] = String2.stringToUtf8Bytes(s);
    jump = String2.makeJumpTable(findBytes);
    Test.ensureEqual(s.indexOf(find), s.length() - 9, "");
    Test.ensureEqual(
        String2.indexOf(sBytes, findBytes, jump),
        sBytes.length - 9 - 1,
        ""); // -1 because of utf encoding n~
    int reps = 100000;

    long time = System.currentTimeMillis();
    int result1 = 0;
    for (int i = 0; i < reps; i++) result1 += s.indexOf(find);
    String2.log(
        "String.indexOf reps="
            + reps
            + " time="
            + (System.currentTimeMillis() - time)
            + "ms  (504ms on Lenovo, was ~115ms on Java 1.7M4700)");

    time = System.currentTimeMillis();
    int result2 = 0;
    for (int i = 0; i < reps; i++) result2 += String2.indexOf(sBytes, findBytes, jump);
    String2.log(
        "String2 byteIndexOf reps="
            + reps
            + " time="
            + (System.currentTimeMillis() - time)
            + "ms  (656ms on Lenovo, ~470ms on Java 1.7M4700)");
    // so if 1000 datasets (or 500 and 2word search), time~=9ms
    // and I think most dataset searchStrings are shorter
  }

  /** This tests String2.utf8 methods. */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testString2utf8() throws Exception {
    String2.log("\n*** TestUtil.testString2utf8()");
    int n = 1000000;
    long memoryInUse, time;

    for (int loop = 0; loop < 3; loop++) {
      // test Strings
      Math2.gcAndWait("TestUtil (between tests)");
      Math2.gcAndWait("TestUtil (between tests)"); // aggressive //in a test
      memoryInUse = Math2.getMemoryInUse();
      time = System.currentTimeMillis();
      String sa[] = new String[n];
      for (int i = 0; i < n; i++) sa[i] = "testABCD" + i;
      time = System.currentTimeMillis() - time;
      Math2.gcAndWait("TestUtil (between tests)");
      Math2.gcAndWait("TestUtil (between tests)"); // aggressive //in a test
      String2.log(
          "String memoryUse/item="
              + ((Math2.getMemoryInUse() - memoryInUse) / (n + 0.0))
              + // 68.1 bytes
              " time="
              + time
              + "ms"); // ~562 (after first time)

      // test utf8s
      memoryInUse = Math2.getMemoryInUse();
      time = System.currentTimeMillis();
      byte ba[][] = new byte[n][];
      for (int i = 0; i < n; i++)
        ba[i] =
            String2.stringToUtf8Bytes("testABCD" + i); // usually 14 characters +4length +4pointer
      time = System.currentTimeMillis() - time;
      Math2.gcAndWait("TestUtil (between tests)");
      Math2.gcAndWait("TestUtil (between tests)"); // aggressive //in a test
      String2.log(
          "utf8 memoryUse/item="
              + ((Math2.getMemoryInUse() - memoryInUse) / (n + 0.0))
              + // 36.0 bytes; why so many?
              " time="
              + time
              + "ms"); // ~1094

      // test double
      memoryInUse = Math2.getMemoryInUse();
      time = System.currentTimeMillis();
      double da[] = new double[n];
      for (int i = 0; i < n; i++) da[i] = i;
      time = System.currentTimeMillis() - time;
      Math2.gcAndWait("TestUtil (between tests)");
      Math2.gcAndWait("TestUtil (between tests)"); // aggressive //in a test
      String2.log(
          "double memoryUse/item="
              + ((Math2.getMemoryInUse() - memoryInUse) / (n + 0.0))
              + // 8 bytes
              " time="
              + time
              + "ms"); // ~6
    }

    // conclusion: memory savings 50%, but takes 2X longer

    // public static String utf8BytesToString(byte[] bar) {
  }

  /** This tests String2.canonical(). */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testString2canonical() throws Exception {
    String2.log("\n*** TestUtil.testString2canonical()");
    // find a way to make != strings (for tests below)
    String a = "" + 1;
    int i = 1;
    String b = "" + i;
    Test.ensureTrue(a != b, "");

    // ensure that new string makes a new String
    a = "This is a test";
    b = a.substring(10, 14);
    String c = new String(b);
    Test.ensureTrue(b != c, "");

    String filler100 =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    int n = 1000;
    String sa[] = new String[95 * 95];
    long oMemoryInUse = -1;
    Math2.gcAndWait("TestUtil (between tests)");
    Math2.gcAndWait("TestUtil (between tests)"); // aggressive preparation //in a test
    String2.log("initialMemoryUse=" + Math2.memoryString() + "\n" + String2.canonicalStatistics());
    int canSize = -1;
    int canSHSize = -1;

    // for each outer loop, create a different group of 95*95 canonical strings
    for (int outer = 0; outer < 10; outer++) {

      // create 1000000 strings, but only 1000 different strings per outer loop
      // With each outer loop, sa values are overwritten.
      // So previous values should be garbage collected.
      // If not, memory use and count of nStrings in canonical system
      // (tested below) will increase with each outer loop.
      long time = System.currentTimeMillis();
      for (int inner = 0; inner < n; inner++) {
        for (int inner2 = 0; inner2 < n; inner2++) {
          int j = (inner % 95) * 95 + (inner2 % 95);
          sa[j] =
              String2.canonical(
                  // makes 95*95 different strings, dispersed to different canonical maps
                  ((char) (32 + (inner % 95)))
                      + ""
                      + // "" keeps char+char->int
                      ((char) (32 + (inner2 % 95)))
                      + ""
                      +
                      // make this string unique to this outer loop iteration
                      ((char) (65 + outer))
                      + ""
                      +
                      // make it a long string
                      filler100);
          // String2.log(">[" + j + "]=" + sa[j]);
        }
      }

      // ensure that memory use and nStrings in maps don't grow unexpectedly
      time = System.currentTimeMillis() - time;
      Math2.gcAndWait("TestUtil (between tests)");
      Math2.gcAndWait("TestUtil (between tests)"); // aggressive //in a test
      long memoryInUse = Math2.getMemoryInUse();
      int shouldBe = outer == 0 ? 415 : 260;
      String2.log(
          String2.canonicalStatistics()
              + "\ntime="
              + time
              + "ms (should be Java 1.8=~"
              + shouldBe
              + "ms [1st pass is slower]) "
              + Math2.memoryString());
      // TODO get a better system for time based performance tests
      //     Test.ensureTrue(time < shouldBe * 2, "Unexpected time (" + time + "ms > " + shouldBe +
      // "ms");
      if (oMemoryInUse == -1) {
        // initial sizes
        oMemoryInUse = memoryInUse;
        canSize = String2.canonicalSize(); // added strings should be gc'd after each iteration
        canSHSize =
            String2
                .canonicalStringHolderSize(); // added strings should be gc'd after each iteration
      } else {
        // String2.log(" bytes/string=" + ((memoryInUse - oMemoryInUse) / (n + 0.0)));
        // too inaccurate to be useful
        Test.ensureTrue(memoryInUse - oMemoryInUse < 5000000, "Memory use is growing!");
        // TODO Memory use checks can fail in GitHub runners
        // Test.ensureTrue(
        //     memoryInUse < 40L * Math2.BytesPerMB, // 2021-11-16 increased because of translated
        //     // messages.xml
        //     "Unexpected memoryInUse=" + (memoryInUse / Math2.BytesPerMB));
      }
      Test.ensureEqual(String2.canonicalSize(), canSize, "Unexpected String2.canonicalSize!");
      Test.ensureEqual(
          String2.canonicalStringHolderSize(),
          canSHSize,
          "Unexpected String2.canonicalStringHolderSize!");
    }
    // for (int j = 0; j < sa.length; j++) String2.log(">> " + sa[j]);
    // TODO Memory use checks can fail in GitHub runners
    // Test.ensureTrue(
    //     Math2.getMemoryInUse() / Math2.BytesPerMB <= 75,
    //     "Unexpected memoryInUse="
    //         + (Math2.getMemoryInUse() / Math2.BytesPerMB)
    //         + "MB (usually 69MB)"); // 2021-11-16
    // increased
    // because
    // of
    // translated
    // messages.xml
  }

  /** This tests String2.canonicalStringHolder(). */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testString2canonicalStringHolder() throws Exception {
    String2.log("\n*** TestUtil.testString2canonicalStringHolder()");
    // find a way to make != strings (for tests below)
    byte[] a = String2.stringToUtf8Bytes("" + 1);
    int i = 1;
    byte[] b = String2.stringToUtf8Bytes("" + i);
    Test.ensureTrue(a != b, "");

    String filler100 =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    int n = 1000;
    StringHolder sa[] = new StringHolder[95 * 95];
    long oMemoryInUse = -1;
    Math2.gcAndWait("TestUtil (between tests)");
    Math2.gcAndWait("TestUtil (between tests)"); // aggressive preparation //in a test
    String2.log("initialMemoryUse=" + Math2.memoryString() + "\n" + String2.canonicalStatistics());
    int canSize = -1;
    int canSHSize = -1;

    // for each outer loop, create a different group of 95*95 canonical strings
    for (int outer = 0; outer < 10; outer++) {

      // create 1000000 strings, but only 1000 different strings per outer loop
      // With each outer loop, sa values are overwritten.
      // So previous values should be garbage collected.
      // If not, memory use and count of nStrings in canonical system
      // (tested below) will increase with each outer loop.
      long time = System.currentTimeMillis();
      for (int inner = 0; inner < n; inner++) {
        for (int inner2 = 0; inner2 < n; inner2++) {
          int j = (inner % 95) * 95 + (inner2 % 95);
          sa[j] =
              String2.canonicalStringHolder(
                  new StringHolder(
                      // makes 95*95 different strings, dispersed to different canonical maps
                      ((char) (32 + (inner % 95)))
                          + ""
                          + // "" keeps char+char->int
                          ((char) (32 + (inner2 % 95)))
                          + ""
                          +
                          // make this string unique to this outer loop iteration
                          ((char) (65 + outer))
                          + ""
                          +
                          // make it a long string
                          filler100));
          // String2.log(">[" + j + "]=" + sa[j]);
        }
      }

      // ensure that memory use and nStrings in maps don't grow unexpectedly
      time = System.currentTimeMillis() - time;
      Math2.gcAndWait("TestUtil (between tests)");
      Math2.gcAndWait("TestUtil (between tests)"); // aggressive //in a test
      long memoryInUse = Math2.getMemoryInUse();
      // TODO get a better system for time based performance tests
      //     int shouldBe = outer == 0 ? 415 : 260; // ms
      //     String2.log(String2.canonicalStatistics() +
      //             "\ntime=" + time + "ms (should be Java 1.8=~" + shouldBe +
      //             "ms [1st pass is slower]) " +
      //             Math2.memoryString());
      //     Test.ensureTrue(time < shouldBe * 3, "Unexpected time");
      if (oMemoryInUse == -1) {
        oMemoryInUse = memoryInUse;
        canSize = String2.canonicalSize(); // added strings should be gc'd after each iteration
        canSHSize =
            String2
                .canonicalStringHolderSize(); // added strings should be gc'd after each iteration
      } else {
        // String2.log(" bytes/string=" + ((memoryInUse - oMemoryInUse) / (n + 0.0)));
        // too inaccurate to be useful
        Test.ensureTrue(memoryInUse - oMemoryInUse < 5000000, "Memory use is growing!");
        // Disable this total memory usage check because with new test approach there is no
        // guarantee
        // about what else might be running. If we need this check, make an isolated test to do
        // this.
        // Test.ensureTrue(memoryInUse < 50L * Math2.BytesPerMB,
        //         "Unexpected memoryInUse=" + (memoryInUse / Math2.BytesPerMB));
      }
      Test.ensureEqual(String2.canonicalSize(), canSize, "Unexpected String2.canonicalSize!");
      Test.ensureEqual(
          String2.canonicalStringHolderSize(),
          canSHSize,
          "Unexpected String2.canonicalStringHolderSize!");
    }
    // for (int j = 0; j < sa.length; j++) String2.log(">> " + sa[j]);

    Test.ensureTrue(
        Math2.getMemoryInUse() / Math2.BytesPerMB <= 90,
        "Unexpected memoryInUse="
            + (Math2.getMemoryInUse() / Math2.BytesPerMB)
            + "MB (usually 73MB)");
  }

  /**
   * This tests String2.canonical(), specifically: does new String(s) (where s is a substring of a
   * larger string) grab just the chars in question (good) or a reference to a substring (bad).
   */
  @RepeatedTest(value = 5, failureThreshold = 4)
  @TagIncompleteTest // repeated test is marking as a fail if any fail, not a pass, need a better
  // approach
  void testString2canonical2() throws Exception {
    String2.log("\n*** TestUtil.testString2canonical2()");
    String sar[] = new String[127];

    // what is initial memory level?
    Math2.gcAndWait("TestUtil (between tests)");
    Math2.gcAndWait("TestUtil (between tests)"); // aggressive //in a test
    long oMem = Math2.getMemoryInUse();

    // just store first 5 chars large strings
    for (int i = 32; i < 127; i++) {
      String s = String2.makeString((char) i, 900000);
      sar[i] = String2.canonical(s.substring(0, 5));
    }

    // what is final memory level?
    Math2.gcAndWait("TestUtil (between tests)");
    Math2.gcAndWait("TestUtil (between tests)"); // aggressive //in a test
    long cMem = Math2.getMemoryInUse();
    String2.log("oMem=" + oMem + "\n" + "cMem=" + cMem + "\n");
    Test.ensureTrue(
        cMem - oMem < 100000,
        "canonical(substring(s,,)) is storing references to the parent string!!!");
  }

  /** Test the speed of writing to hard drive. Does it block? No */
  @org.junit.jupiter.api.Test
  void testFileWriteSpeed() throws Exception {
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/TestUtilTestFileSpeed.txt";
    Writer writer = File2.getBufferedFileWriterUtf8(fileName); // 387 ~490 ms if not buffered
    try {
      long time = System.currentTimeMillis();
      for (int i = 0; i < 1000000; i++) {
        writer.write("This is a test of a pretty long string that changes on each line " + i);
        writer.write("\r\n");
        // writer.flush(); //if flush, ~2853ms
      }
      String2.log(
          "TestUtil.testFileWriteSpeed write 1000000 lines. time="
              + (System.currentTimeMillis() - time)
              + "ms (expected=387ms which is really fast)");
    } finally {
      writer.close();
    }
  }

  /** Test the speed of writing to hard drive. Does it block? */
  @org.junit.jupiter.api.Test
  void testWriteToFileSpeed() throws Exception {
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/TestUtilTestFileSpeed.txt";
    StringBuilder sb = new StringBuilder(1000000 * 100);
    for (int i = 0; i < 1000000; i++)
      sb.append("This is a test of a pretty long string that changes on each line " + i + "\n");
    String s = sb.toString();
    long time = System.currentTimeMillis();
    File2.writeToFile88591(fileName, s);
    String2.log(
        "TestUtil.testWriteToFile write 1000000 lines. time="
            + (System.currentTimeMillis() - time)
            + "ms (expected=3406ms which is really fast)");
  }

  /** Test the speed of reading to hard drive. */
  @org.junit.jupiter.api.Test
  void testReadFromFileSpeed() throws Exception {
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/TestUtilTestFileSpeed.txt";
    long time = System.currentTimeMillis();
    String s[] = File2.readFromFile88591(fileName);
    if (s[0].length() > 0) String2.log(s[0]);
    String2.log(
        "TestUtil.testReadFromFile nChar="
            + s[1].length()
            + " time="
            + (System.currentTimeMillis() - time)
            + "ms (expected=5109ms which is really fast)");
  }
}
