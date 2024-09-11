/* This file is Copyright (c) 2019 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 */
package com.cohort.util;

import java.math.BigInteger;

/**
 * This class makes almost all of the static methods in com.cohort.Math2 accessible to JexlScript
 * scripts as "Math2.<i>name</i>()" methods. This doesn't include the memory related and sleep
 * related methods.
 *
 * <p>The underlying Math2 class is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com). See
 * the MIT/X-like license in com/cohort/util/LICENSE.txt.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2019-11-14
 */
public class ScriptMath2 {

  /**
   * Eps values define small values that are suitable for quick tests if the difference between two
   * values is close to the precision of of the data type. epsilon for floats=fEps=1e-5f
   */
  public static final float fEps = Math2.fEps;

  /** epsilon suitable for doubles = 1e-13. */
  public static final double dEps = Math2.dEps;

  /** 180.0 / Math.PI */
  public static final double OneRadian = Math2.OneRadian;

  /** 2 * Math.PI */
  public static final double TwoPi = Math2.TwoPi;

  /** Math.log(10.0); //2.302585092994046; */
  public static final double ln10 = Math2.ln10;

  /** Math.log(2.0) */
  public static final double ln2 = Math2.ln2;

  /** -273.15 */
  public static final double kelvinToC = Math2.kelvinToC;

  /** 255 */
  public static final short UBYTE_MAX_VALUE = Math2.UBYTE_MAX_VALUE;

  /** 65535 */
  public static final int USHORT_MAX_VALUE = Math2.USHORT_MAX_VALUE;

  /** 4294967295L */
  public static final long UINT_MAX_VALUE = Math2.UINT_MAX_VALUE;

  /** 18446744073709551615.0 */
  public static final BigInteger ULONG_MAX_VALUE = Math2.ULONG_MAX_VALUE;

  /** -2000 */
  public static final int Binary0 = Math2.Binary0;

  /** 980; //2^980 = ~1e295 */
  public static final int BinaryLimit = Math2.BinaryLimit;

  /** 1024 */
  public static final int BytesPerKB = Math2.BytesPerKB;

  /** BytesPerKB * BytesPerKB */
  public static final int BytesPerMB = Math2.BytesPerMB;

  /** BytesPerMB * (long)BytesPerKB */
  public static final long BytesPerGB = Math2.BytesPerGB;

  /** BytesPerGB * BytesPerKB */
  public static final long BytesPerTB = Math2.BytesPerTB;

  /** BytesPerTB * BytesPerKB; */
  public static final long BytesPerPB = Math2.BytesPerPB;

  /** ((long) Integer.MAX_VALUE * 2) + 1; //mask for low 32 bits */
  public static final long loAnd = Math2.loAnd;

  /**
   * For security, this is a different object than Math2's random. It is private so only accessible
   * via methods in this class.
   */
  private static java.util.Random random = new java.util.Random();

  /**
   * 1200.0 / 3927.0. udunits says: US_survey_footS (1200/3937) meter# exact US_survey_feetS
   * US_survey_foot# alias US_survey_mileP 5280 US_survey_feet# exact US_statute_mileP
   * US_survey_mile# alias so convert nMiles * 5280 ft/mile * 1200/3927 m/ft * .001 km/m -&gt; km
   */
  public static final double meterPerFoot = Math2.meterPerFoot;

  /** 5280 * meterPerFoot */
  public static final double mPerMile = Math2.mPerMile;

  /** mPerMile * 0.001 */
  public static final double kmPerMile = Math2.kmPerMile;

  /** <tt>two</tt> defines powers of two, e.g., Two[0]=1, Two[1]=2, Two[2]=4, ... Two[31]. */
  public static final int[] Two = Math2.Two;

  /** This defines powers of ten. e.g., Ten[0]=1, Ten[1]=10, Ten[2]=100... Ten[18] */
  public static final double[] Ten = Math2.Ten;

  /**
   * This defines inverse powers of ten. e.g., InverseTen[0]=1, InverseTen[1]=.01,
   * InverseTen[2]=.001... InverseTen[18]
   */
  public static final double[] InverseTen = Math2.InverseTen;

  /**
   * -99, -99.9, -99.99, -999, -999.9, -9999, -99999, -999999, -9999999, 99, 99.9, 99.99, 999,
   * 999.9, 9999, 99999, 999999, 9999999
   */
  public static final double[] COMMON_MV9 = Math2.COMMON_MV9;

  /**
   * This returns the truncated part of a double.
   *
   * @param d a double value
   * @return d truncated. The return value will be the same sign as d. !isFinite(d), this returns
   *     NaN. d=trunc(d)+frac(d);
   */
  public static double trunc(double d) {
    return Math2.trunc(d);
  }

  /**
   * This returns the truncated part of a double, stored as an int.
   *
   * @param d any double
   * @return the truncated version of d (or NaN if trouble)
   * @see Math2#trunc
   */
  public static int truncToInt(double d) {
    return Math2.truncToInt(d);
  }

  /**
   * This returns the fraction part of a double. The return value will be the same sign as d.
   * d=trunc(d)+frac(d);
   *
   * @param d a double
   * @return the fractional part of d
   */
  public static double frac(double d) {
    return Math2.frac(d);
  }

  /**
   * This returns 1 for positive i's, -1 for negative i's, and 1 if i is 0 (i.e., 0 is treated as a
   * positive number).
   *
   * @param i an int
   * @return 1 for positive i's, -1 for negative i's, and 1 if i is 0 (i.e., 0 is treated as a
   *     positive number).
   */
  public static int sign1(int i) {
    return Math2.sign1(i);
  }

  /**
   * This returns the integer exponent of a double (-0.0175 returns -2 since -0.0175=-1.75*10^-2).
   * It handles 0, +, and - numbers. 0 and NaN returns Integer.MAX_VALUE. WARNING: round off
   * problems cause 100 -&gt; 10 *10^1, not 1*10^2!
   *
   * <p>See the similar String2.toRational()
   *
   * @param d a double value
   * @return the integer exponent of the number. If !isFinite(d), this returns Integer.MAX_VALUE d=0
   *     returns 0.
   */
  public static int intExponent(double d) {
    return Math2.intExponent(d);
  }

  /**
   * This returns the double exponent of a double (e.g., -0.0175 returns 0.01 since
   * -0.0175=-1.75*0.01). It handles 0, +, and - numbers. WARNING: round off problems cause 100
   * -&gt; 10 *10^1, not 1*10^2!
   *
   * @param d a double value
   * @return the exponent of the number. d=0 returns 1. If !isFinite(d), this returns NaN.
   */
  public static double exponent(double d) {
    return Math2.exponent(d);
  }

  /**
   * This returns the mantissa of a double (-0.0175 returns -1.75 since -0.0175=-1.75*10^-2). It
   * handles 0, +, and - numbers. WARNING: round off problems can cause (for example) 100 to be
   * treated 10 *10^1, not 1*10^2!
   *
   * <p>See the similar String2.toRational()
   *
   * @param d any double
   * @return d / exponent(d) (or 0 if d=0, or NaN if !finite(d))
   */
  public static double mantissa(double d) {
    return Math2.mantissa(d);
  }

  /**
   * Checks if the value is NaN or infinite: returns Double.NaN if so; otherwise returns the
   * original value.
   *
   * @param d and double value
   * @return d (or NaN if !isFinite(d))
   */
  public static double NaNCheck(double d) {
    return Math2.NaNCheck(d);
  }

  /**
   * Indicates if i is odd.
   *
   * @param i any int
   * @return true if i is an odd number
   */
  public static boolean odd(int i) {
    return Math2.odd(i);
  }

  /**
   * This returns an integer power of ten. It uses Ten[] when possible for increased speed. It is
   * protected against errors. It returns Double.POSITIVE_INFINITY if trouble.
   *
   * @param toThe the number that ten is to be raised to the power of
   * @return 10^toThe
   */
  public static double ten(int toThe) {
    return Math2.ten(toThe);
  }

  /**
   * This returns the binary exponent of a double: usually +-1023.
   *
   * @param d any double
   * @return the binary exponent of d. If d = 0, this returns Binary0 (which is a flag, not a real
   *     value). If !isFinite(d), this return BinaryLimit (which is a flag, not a real value).
   */
  public static int binaryExponent(double d) {
    return Math2.binaryExponent(d);
  }

  /**
   * This quickly tests if <tt>d</tt> is almost 0 (Math.abs(d)&lt;dEps).
   *
   * <UL>
   *   <LI>If you are working with small numbers, this test may be inappropriate.
   *   <LI>This is very fast, since it only involves one comparison.
   *   <LI>Use almost0(d) instead of almostEqual5 or almostEqual9.
   * </UL>
   *
   * @param d any double
   * @return true if Math.abs(d) &lt; dEps. NaN and Infinity correctly return false.
   */
  public static final boolean almost0(double d) {
    return Math2.almost0(d);
  }

  /**
   * This tests if d1 is less than or almostEqual d2.
   *
   * @param nSignificantDigits 0 to 18 are allowed; 5, 9, and 14 are common
   * @param d1 the first number
   * @param d2 the second number
   * @return true if d1 is less than or almostEqual d2. If isNaN(d1) || isNaN(d2), this returns
   *     false.
   */
  public static boolean lessThanAE(int nSignificantDigits, double d1, double d2) {
    return Math2.lessThanAE(nSignificantDigits, d1, d2);
  }

  /**
   * This tests if d1 is greater than or almostEqual9 d2.
   *
   * @param nSignificantDigits 0 to 18 are allowed; 5, 9, and 14 are common
   * @param d1 the first number
   * @param d2 the second number
   * @return true if d1 is greater than or almostEqual9 d2. If isNaN(d1) || isNaN(d2), this returns
   *     false.
   */
  public static boolean greaterThanAE(int nSignificantDigits, double d1, double d2) {
    return Math2.greaterThanAE(nSignificantDigits, d1, d2);
  }

  /**
   * This tests if the numbers are equal to at least n significant digits.
   *
   * <UL>
   *   <LI>Numbers must match to 1 part in 10^n to ensure that rounding to n-1 digits is identical.
   *   <LI>If d1 and d2 are almost0, this returns true.
   *   <LI>This is slow compared to almost0.
   * </UL>
   *
   * @param nSignificantDigits 0 to 18 are allowed; 5, 9, and 14 are common
   * @param d1 any double
   * @param d2 any double
   * @return true if the numbers are equal to at least n significant digits (or are both almost0).
   *     If either number is NaN or Infinity, this returns false.
   */
  public static boolean almostEqual(int nSignificantDigits, double d1, double d2) {
    return Math2.almostEqual(nSignificantDigits, d1, d2);
  }

  /**
   * This tests if two floats are equal to at least n significant digits.
   *
   * @param nSignificantDigits 0 to 18 are allowed; 5, 9, and 14 are common
   * @param f1 any float
   * @param f2 any float
   * @return true if the numbers are equal to at least n significant digits (or are both almost0).
   *     If either number is NaN or Infinity, this returns false.
   */
  public static boolean almostEqual(int nSignificantDigits, float f1, float f2) {
    return Math2.almostEqual(nSignificantDigits, f1, f2);
  }

  /**
   * A div that rounds. Positive numbers only. e.g., 1/4 goes to 0; 3/4 goes to 1; den = 0 throws an
   * exception.
   *
   * @param num the numerator (a positive number)
   * @param den the denominator (a positive number)
   * @return num/den, but rounded to the next larger (abs) int
   */
  public static int roundDiv(int num, int den) {
    return Math2.roundDiv(num, den);
  }

  /**
   * A div that rounds up if den&gt;0. e.g., 1/4 goes to 1; 4/4 goes to 1; den = 0 throws an
   * exception.
   *
   * @param num the numerator (a positive number)
   * @param den the denominator (a positive number)
   * @return num/den, but rounded to the next larger (abs) int
   */
  public static int hiDiv(int num, int den) {
    return Math2.hiDiv(num, den);
  }

  /**
   * A div that rounds up if den&gt;0. e.g., 1/4 goes to 1; 4/4 goes to 1; den = 0 throws an
   * exception.
   *
   * @param num the numerator (a positive number)
   * @param den the denominator (a positive number)
   * @return num/den, but rounded to the next larger (abs) int
   */
  public static long hiDiv(long num, long den) {
    return Math2.hiDiv(num, den);
  }

  /**
   * A div where the implied mod is always &gt;=0. This is a consistent div for positive and
   * negative numerators. For example, with regular division 1/2=0 and -1/2=0. But
   * floorDiv(-1,2)=-1. den = 0 throws an exception.
   *
   * @param num the numerator (a positive or negative number)
   * @param den the denominator (a positive number)
   * @return num/den, but is consistent for positive and negative numbers
   */
  public static int floorDiv(int num, int den) {
    return Math2.floorDiv(num, den);
  }

  /**
   * A div where the implied mod is always &gt;=0. This is a consistent div for positive and
   * negative numerators. For example, with regular division 1/2=0 and -1/2=0. But
   * floorDiv(-1,2)=-1. den = 0 throws an exception.
   *
   * @param num the numerator (a positive or negative number)
   * @param den the denominator (a positive number)
   * @return num/den, but is consistent for positive and negative numbers
   */
  public static long floorDiv(long num, long den) {
    return Math2.floorDiv(num, den);
  }

  /**
   * This forces a int into a range defined by min..max.
   *
   * @param min the minimum allowed value
   * @param max the maximum allowed value
   * @param current the current value
   * @return min if current is less than min; or max if current is greater than max; else it returns
   *     the current value.
   */
  public static final int minMax(int min, int max, int current) {
    return Math2.minMax(min, max, current);
  }

  /**
   * This forces a double into a range defined by min..max.
   *
   * @param min the minimum allowed value
   * @param max the maximum allowed value
   * @param current the current value
   * @return min if current is less than min; or max if current is greater than max; else it returns
   *     the current value.
   */
  public static final double minMax(double min, double max, double current) {
    return Math2.minMax(min, max, current);
  }

  /**
   * This forces an int into a range defined by min..max.
   *
   * @param min the minimum allowed value
   * @param max the maximum allowed value
   * @param def the default value
   * @param current the current value
   * @return def if current is less than min; or def if current is greater than max; else it returns
   *     the current value.
   */
  public static final int minMaxDef(int min, int max, int def, int current) {
    return Math2.minMaxDef(min, max, def, current);
  }

  /**
   * This forces a double into a range defined by min..max.
   *
   * @param min the minimum allowed value
   * @param max the maximum allowed value
   * @param def the default value
   * @param current the current value
   * @return def if current is less than min; or def if current is greater than max; else it returns
   *     the current value.
   */
  public static final double minMaxDef(double min, double max, double def, double current) {
    return Math2.minMaxDef(min, max, def, current);
  }

  /**
   * Safely rounds a double to a byte.
   *
   * @param d any double
   * @return Byte.MAX_VALUE if d is too small, too big, or NaN; otherwise d, rounded to the nearest
   *     byte. Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final byte roundToByte(double d) {
    return Math2.roundToByte(d);
  }

  /**
   * Safely rounds a double to a ubyte.
   *
   * @param d any double
   * @return 255 if d is too small, too big, or NaN; otherwise d, rounded to the nearest byte.
   *     Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final short roundToUByte(double d) {
    return Math2.roundToUByte(d);
  }

  /**
   * Safely rounds a double to a char (treated as unsigned short).
   *
   * @param d any double
   * @return Character.MAX_VALUE if d is too small, too big, or NaN; otherwise d, rounded to the
   *     nearest char. Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final char roundToChar(double d) {
    return Math2.roundToChar(d);
  }

  /**
   * Safely rounds a double to a short.
   *
   * @param d any double
   * @return Short.MAX_VALUE if d is too small, too big, or NaN; otherwise d, rounded to the nearest
   *     short. Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final short roundToShort(double d) {
    return Math2.roundToShort(d);
  }

  /**
   * Safely rounds a double to a ushort.
   *
   * @param d any double
   * @return 0xffff if d is too small, too big, or NaN; otherwise d, rounded to the nearest short.
   *     Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final int roundToUShort(double d) {
    return Math2.roundToUShort(d);
  }

  /**
   * Safely rounds a double to an int. (Math.round but rounds to a long and not safely.)
   *
   * @param d any double
   * @return Integer.MAX_VALUE if d is too small, too big, or NaN; otherwise d, rounded to the
   *     nearest int. Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final int roundToInt(double d) {
    return Math2.roundToInt(d);
  }

  /**
   * Safely rounds a double to a uint.
   *
   * @param d any double
   * @return 4294967295L if d is too small, too big, or NaN; otherwise d, rounded to the nearest
   *     short. Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final long roundToUInt(double d) {
    return Math2.roundToUInt(d);
  }

  /**
   * Safely rounds a double to a long.
   *
   * @param d any double
   * @return Long.MAX_VALUE if d is too small, too big, or NaN; otherwise d, rounded to the nearest
   *     int. Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final long roundToLong(double d) {
    return Math2.roundToLong(d);
  }

  /**
   * Safely rounds a double to a ulong.
   *
   * @param d any double
   * @return ULONG_MAX_VALUE if d is too small, too big, or NaN; otherwise d, rounded to the nearest
   *     short. Undesirable: d.5 rounds up for positive numbers, down for negative.
   */
  public static final BigInteger roundToULong(double d) {
    return Math2.roundToULong(d);
  }

  /**
   * Safely rounds a double to the nearest integer (stored as a double).
   *
   * @param d any double
   * @return Double.NaN if d is !finite; otherwise d, rounded to the nearest int. Undesirable: d.5
   *     rounds up for positive numbers, down for negative. But this rounds d.5 in a way that is
   *     often more useful than Math.rint (which rounds to nearest even number).
   */
  public static final double roundToDouble(double d) {
    return Math2.roundToDouble(d);
  }

  /**
   * Rounds the value to the specified number of decimal places.
   *
   * @param d any double
   * @param nPlaces the desired number of digits to the right of the decimal point
   */
  public static final double roundTo(double d, int nPlaces) {
    return Math2.roundTo(d, nPlaces);
  }

  /**
   * Safely narrows an int to a byte.
   *
   * @param i any int
   * @return Byte.MAX_VALUE if i is too small or too big; otherwise i.
   */
  public static final byte narrowToByte(int i) {
    return Math2.narrowToByte(i);
  }

  /**
   * Safely narrows a long to a byte.
   *
   * @param i any long
   * @return Byte.MAX_VALUE if i is too small or too big; otherwise i.
   */
  public static final byte narrowToByte(long i) {
    return Math2.narrowToByte(i);
  }

  /**
   * Safely narrows an int to a char.
   *
   * @param i any int
   * @return Character.MAX_VALUE if i is too small or too big; otherwise i.
   */
  public static final char narrowToChar(int i) {
    return Math2.narrowToChar(i);
  }

  /**
   * Safely narrows a long to a char.
   *
   * @param i any long
   * @return Character.MAX_VALUE if i is too small or too big; otherwise i.
   */
  public static final char narrowToChar(long i) {
    return Math2.narrowToChar(i);
  }

  /**
   * Safely narrows an int to a short.
   *
   * @param i any int
   * @return Short.MAX_VALUE if i is too small or too big; otherwise i.
   */
  public static final short narrowToShort(int i) {
    return Math2.narrowToShort(i);
  }

  /**
   * Safely narrows a long to a short.
   *
   * @param i any long
   * @return Short.MAX_VALUE if i is too small or too big; otherwise i.
   */
  public static final short narrowToShort(long i) {
    return Math2.narrowToShort(i);
  }

  /**
   * Safely narrows a long to an int.
   *
   * @param i any long
   * @return Integer.MAX_VALUE if i is too small or too big; otherwise i.
   */
  public static final int narrowToInt(long i) {
    return Math2.narrowToInt(i);
  }

  /**
   * Safely converts a byte (-128..127) to char (0..255). Note that reverse is easy: (byte)ch works
   * (for 0..255) because narrowing just saves the low order bits, so &gt;127 becomes negative
   * bytes.
   *
   * @param b a byte (-128 .. 127) (or char, short, or int where you just want the lower 8 bits
   *     stored as 0..255)
   * @return a char (0..255)
   */
  public static final char byteToChar(int b) {
    return Math2.byteToChar(b);
  }

  /**
   * Safely converts a signed byte (-128..127) to an unsigned byte (0..255). Note that reverse is
   * easy: (byte)ch works (for 0..255) because narrowing just saves the low order bits, so &gt;127
   * becomes negative bytes.
   *
   * @param b a byte (-128 .. 127) (or char, short, or int where you just want the lower 8 bits
   *     stored as 0..255)
   * @return an int (0..255)
   */
  public static final int unsignedByte(int b) {
    return Math2.unsignedByte(b);
  }

  /**
   * This converts an angle (in degrees) into the range &gt;=0 to &lt;360.
   *
   * <UL>
   *   <LI>If isMV(angle), it returns 0.
   * </UL>
   */
  public static final double angle0360(double degrees) {
    return Math2.angle0360(degrees);
  }

  /**
   * This converts an angle (in degrees) into the range &gt;= 0 to &lt;=360 (note 360 is valid).
   *
   * @param degrees an angle (in degrees)
   * @return the angle (in degrees) in the range &gt;=0 to &lt;=360. A non-finite degrees returns
   *     the original value.
   */
  public static final double looserAngle0360(double degrees) {
    return Math2.looserAngle0360(degrees);
  }

  /**
   * This converts a compass heading (usually 0..360, where North is 0, East is 90...) to Math-style
   * degrees (East is 0, North is 90, ...).
   *
   * @param compass
   * @return degrees always &gt;=0 and &lt;360 (compass=NaN -&gt; 0).
   */
  public static final double compassToMathDegrees(double compass) {
    return Math2.compassToMathDegrees(compass);
  }

  /**
   * This converts a Math-style degrees (East is 0, North is 90, ...) to a compass heading (where
   * North is 0, East is 90, ...).
   *
   * @param math
   * @return degrees always &gt;=0 and &lt;360 (compass=NaN -&gt; 0).
   */
  public static final double mathToCompassDegrees(double math) {
    return Math2.mathToCompassDegrees(math);
  }

  /**
   * This converts an angle (in degrees) into the range &gt;=-180 to &lt;180 (180 becomes -180).
   *
   * @param degrees an angle (in degrees)
   * @return the angle (in degrees) in the range &gt;=-180 to &lt;180. If isMV(angle), it returns 0.
   */
  public static final double anglePM180(double degrees) {
    return Math2.anglePM180(degrees);
  }

  /**
   * This converts an angle (in degrees) into the range &gt;=-180 to &lt;=180 (note 180 is valid).
   *
   * @param degrees an angle (in degrees)
   * @return the angle (in degrees) in the range &gt;=-180 to &lt;=180. A non-finite degrees returns
   *     the original value.
   */
  public static final double looserAnglePM180(double degrees) {
    return Math2.looserAnglePM180(degrees);
  }

  /**
   * This converts an angle (in radians) into the range &gt;=0 to &lt;2PI.
   *
   * <UL>
   *   <LI>If !Double.isFinite(angle), it returns 0.
   * </UL>
   */
  public static final double angle02Pi(double radians) {
    return Math2.angle02Pi(radians);
  }

  /**
   * Finds the greatest common divisor.
   *
   * <UL>
   *   <LI>Euclidean greatest common divisor algorithm.
   *   <LI>If either is 0, it returns 1.
   *   <LI>For example, gcd(18,30) returns 6.
   * </UL>
   */
  public static int gcd(int n, int d) {
    return Math2.gcd(n, d);
  }

  /**
   * Looks for a fraction very close to some decimal value.
   *
   * <UL>
   *   <LI>Tries denominators 1..1000. So answer is at least accurate to within 1/1000th. For
   *       example, .33333 -&gt; 1/3.
   *   <LI>For example: -1.75 -&gt; whole=-1, numerator=-3 denominator=4
   *   <LI>Results stored in int3[0=whole, 1=num, 2=den].
   *   <LI>Slow if no good match found, but this does a good approximate job that gcd() might miss.
   *       <pre>
   *     int ar[]=new int[3];
   *     double d=-1.75;
   *     int whole=guessFrac(d,ar);
   *     //results: ar[0]=-1, ar[1]=-3, ar[2]=4
   * </pre>
   * </UL>
   */
  public static void guessFrac(double r, int[] int3) {
    Math2.guessFrac(r, int3);
  }

  /** This creates a String based on the results of guessFrac() */
  public static String guessFracString(double d) {
    return Math2.guessFracString(d);
  }

  /**
   * This converts a long to a double (Long.MAX_VALUE becomes NaN).
   *
   * @param tl
   * @return a double. If tl is Long.MAX_VALUE, this returns Double.NaN.
   */
  public static final double longToDoubleNaN(long tl) {
    return Math2.longToDoubleNaN(tl);
  }

  /**
   * This converts an unsigned long to a double. !!! possible loss of precision!
   *
   * @param tl
   * @return a double.
   */
  public static final double ulongToDouble(long tl) {
    return Math2.ulongToDouble(tl); // !!! possible loss of precision
  }

  /**
   * Safely converts a float to a double.
   *
   * @param f a double or float
   * @return an unbruised double (not xxxx999999 or xxxx000001). If f is NaN, this returns
   *     Double.NaN. If f is +-INFINITY, this returns Double.+-INFINITY.
   */
  public static final double floatToDouble(double f) {
    return Math2.floatToDouble(f);
  }

  /**
   * Crudely (not nicely) converts a float to a double (including the non-standard conversion of
   * INFINITY values to NaN). See floatToDouble.
   *
   * @param f a float or double
   * @return a double. If f is NaN or +-INFINITY, this returns Double.NaN.
   */
  public static final double floatToDoubleNaN(double f) {
    return Math2.floatToDoubleNaN(f);
  }

  /**
   * Safely converts a double to a float (including the non-standard conversion of large values to
   * Float.NaN, not Float.POSITIVE_INFINITY).
   *
   * @param d a double
   * @return a float. If f is not finite or greater than Float.MAX_VALUE, this returns Float.NaN.
   */
  public static final float doubleToFloatNaN(double d) {
    return Math2.doubleToFloatNaN(d);
  }

  /**
   * Safely tries to un-bruise a double (8.999999999 -&gt; 9.0, or 1.000000001 -&gt; 1.0).
   *
   * <UL>
   *   <LI>nDigits specifies how many digits. Use 7 for converting float to double. Use 11 for
   *       converting 6byte real to double.
   *   <LI>nGoodDigits afterward is ~ 22-nDigits.
   * </UL>
   *
   * @param d a double (often bruised)
   * @param nDigits the desired number of significant digits
   */
  public static final double niceDouble(double d, int nDigits) {
    return Math2.niceDouble(d, nDigits);
  }

  /**
   * This sets the seed for the next call to random(). You can use this for testing purposes if you
   * need to get repeatable results from random().
   *
   * @param seed the new seed -- be sure that it is randomly chosen from the full range of longs
   */
  public static void setSeed(long seed) {

    // this is probably thread-safe
    // http://www.velocityreviews.com/forums/t367261-java-util-random-nextint-thread-safety.html
    // but synchronize it to be extra careful
    synchronized (random) {
      random.setSeed(seed);
    }
  }

  /**
   * This returns a random integer between 0 and max-1. For testing purposes, you can call
   * Math2.setSeed(long seed) to get repeatable results.
   *
   * @param max a int greater than 0
   * @return a "random" number in the range 0 .. max-1
   */
  public static int random(int max) {

    // this is probably thread-safe
    // http://www.velocityreviews.com/forums/t367261-java-util-random-nextint-thread-safety.html
    // but synchronize it to be extra careful
    synchronized (random) {
      return random.nextInt(max);
    }
  }

  // current time: use:  long time=System.currentTimeMillis();

  /**
   * This increases the value (nicely).
   *
   * @param d a double
   * @return a number slightly larger than d. If !Double.isFinite(d), it returns d. If almost 0, it
   *     returns 0.01.
   */
  public static double bigger(double d) {
    return Math2.bigger(d);
  }

  /**
   * This decreases the value (nicely).
   *
   * @param d a double
   * @return a number slightly smaller than d. If !Double.isFinite(d), it returns d. If almost 0, it
   *     returns -0.01.
   */
  public static double smaller(double d) {
    return Math2.smaller(d);
  }

  /**
   * This increases the first digit of d (for example, .8, .9, 1, 2, 3, ..., 9, 10, 20, 30, ...). It
   * rounds to nearest single digit mantissa, then changes it. If !Double.isFinite(d), it returns
   * def. If d = 0, it returns 1.
   *
   * @param max the maximum value which may be returned
   * @param def the default value, to be used if !isFinite
   * @param d the initial value
   * @return d, rounded to a single digit mantissa and with the initial digit increased
   */
  public static double oneDigitBigger(double max, double def, double d) {
    return Math2.oneDigitBigger(max, def, d);
  }

  /**
   * This decreases the first digit of d (for example, 30, 20, 10, 9, ..., 3, 2, 1, .9, .8, ...). It
   * rounds to nearest single digit mantissa, then changes it. If !Double.isFinite(d), it returns
   * def. If d = 0, it returns -1.
   *
   * @param min the minimum value which may be returned
   * @param def the default value, to be used if !isFinite
   * @param d the initial value
   * @return d, rounded to a single digit mantissa and with the initial digit decreased
   */
  public static double oneDigitSmaller(double min, double def, double d) {
    return Math2.oneDigitSmaller(min, def, d);
  }

  /**
   * This returns a small increment roughly 1/100th the range (e.g., .1, 1, 10, ....).
   *
   * @param range
   * @return an appropriate increment for the range
   */
  public static double getSmallIncrement(double range) {
    return Math2.getSmallIncrement(range);
  }

  /**
   * This increases d to the next multiple of mult.
   *
   * @param def the default value
   * @param mult the multiple
   * @param max the maximum value
   * @param d the initial value
   * @return the next multiple of mult bigger than d. It rounds to nearest mult, then changes it. If
   *     !Double.isFinite(d), it returns def.
   */
  public static double biggerDouble(double def, double mult, double max, double d) {
    return Math2.biggerDouble(def, mult, max, d);
  }

  /**
   * This decreases d to the previous multiple of mult.
   *
   * @param def the default value
   * @param mult the multiple
   * @param min the minimum value
   * @param d the initial value
   * @return the next multiple of mult smaller than d. It rounds to nearest mult, then changes it.
   *     If !Double.isFinite(d), it returns def.
   */
  public static double smallerDouble(double def, double mult, double min, double d) {
    return Math2.smallerDouble(def, mult, min, d);
  }

  /**
   * This increases the double degrees value (nicely), and returns it as a string.
   *
   * @param d the initial value
   * @return the next multiple 15. It rounds to nearest mult, then changes it. If
   *     !Double.isFinite(d), it returns d.
   */
  public static double biggerAngle(double d) {
    return Math2.biggerAngle(d);
  }

  /**
   * This decreases the double degree value (nicely).
   *
   * @param d the initial value
   * @return the previous multiple 15. It rounds to nearest mult, then changes it. If
   *     !Double.isFinite(d), it returns d.
   */
  public static double smallerAngle(double d) {
    return Math2.smallerAngle(d);
  }

  /**
   * This increases the value (nicely) so the mantissa is 1 or 5.
   *
   * @param d the initial value
   * @return the next 1 or 5 * 10^x. It rounds to nearest 1 or 5, then changes it. If
   *     !Double.isFinite(d), it returns d. If almost0(d), it returns 0.01.
   */
  public static double bigger15(double d) {
    return Math2.bigger15(d);
  }

  /**
   * This gets the double value from the string, decreases it (nicely), so the mantissa is 1 or 5.
   *
   * @param d the initial value
   * @return the next 1 or 5 * 10^x. It rounds to nearest 1 or 5, then changes it. If
   *     !Double.isFinite(d), it returns d. If almost0(d), it returns 0.01.
   */
  public static double smaller15(double d) {
    return Math2.smaller15(d);
  }

  /**
   * This returns a nice bounding range (e.g., for an axis) which includes low and high.
   *
   * @param low the low end of the range
   * @param high the high end of the range
   * @return returnLowHigh an array with 2 elements. The resulting bounds are stored as [0]=low and
   *     [1]=high. If low and high are not finite, this returns 0,1. In all other cases, this
   *     returns an appropriate wider range.
   */
  public static double[] suggestLowHigh(double low, double high) {
    return Math2.suggestLowHigh(low, high);
  }

  /**
   * This suggests the division distance along an axis so that there will be about 5-7 primary
   * divisions and 10-25 secondary.
   *
   * @param range the range of the axis, e.g., an axis spanning 30 - 50 would have a range of 20
   * @return a double[2] with [0]=suggested major division distance and [1]=suggested minor division
   *     distance. If range isn't finite, this returns 2 Double.NaN's. If range == 0, this returns 1
   *     and .5. If range &lt; 0, this uses Math.abs(range).
   */
  public static double[] suggestDivisions(double range) {
    return Math2.suggestDivisions(range);
  }

  /**
   * This suggests the division distance along an axis so that there will be between maxDivisions/2
   * and maxDivisions.
   *
   * @param range the range of the axis, e.g., an axis spanning 30 - 50 would have a range of 20
   * @param maxDivisions the maximum number of divisions (segments). If you have maxNValues, use
   *     maxNValues-1 to call this method.
   * @return a double with the suggested division distance. If range isn't finite, this returns NaN.
   *     If range == 0, this returns 1. If range &lt; 0, this the result will be negative. If
   *     maxDivisions == 0, this returns range. If maxDivisions &lt; 0, this uses
   *     Math.abs(maxDivisions).
   */
  public static double suggestMaxDivisions(double range, int maxDivisions) {
    return Math2.suggestMaxDivisions(range, maxDivisions);
  }

  /**
   * Find the last element which is &lt;= x in an ascending sorted array.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param dar an ascending sorted double[] which may have duplicate values
   * @param x
   * @return the index of the last element which is &lt;= x in an ascending sorted array. If x &lt;
   *     the smallest element, this returns -1 (no element is appropriate). If x &gt; the largest
   *     element, this returns dar.length-1. If x is NaN, this is currently undefined.
   */
  public static int binaryFindLastLE(double[] dar, double x) {
    return Math2.binaryFindLastLE(dar, x);
  }

  /**
   * Find the last element which is &lt;x or almostEqual(5, x) in an ascending sorted array.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param dar an ascending sorted double[] which may have duplicate values
   * @param x
   * @param precision e.g., 5 for floats and 9 for doubles
   * @return the index of the last element which is &lt;= x in an ascending sorted array. If x &lt;
   *     the smallest element, this returns -1 (no element is appropriate). If x &gt; the largest
   *     element, this returns dar.length-1. If x is NaN, this is currently undefined.
   */
  public static int binaryFindLastLAE(double[] dar, double x, int precision) {
    return Math2.binaryFindLastLAE(dar, x, precision);
  }

  /**
   * Find the first element which is &gt;= x in an ascending sorted array.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param dar an ascending sorted double[] which currently may not have duplicate values
   * @param x
   * @return the index of the first element which is &gt;= x in an ascending sorted array. If x &lt;
   *     the smallest element, this returns 0. If x &gt; the largest element, this returns
   *     dar.length (no element is appropriate). If x is NaN, this is currently undefined.
   */
  public static int binaryFindFirstGE(double[] dar, double x) {
    return Math2.binaryFindFirstGE(dar, x);
  }

  /**
   * Find the first element which is &gt;x or almostEqual(precision, x) in an ascending sorted
   * array.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param dar an ascending sorted double[] which currently may not have duplicate values
   * @param x
   * @param precision e.g., 5 for floats and 9 for doubles
   * @return the index of the first element which is &gt;= x in an ascending sorted array. If x &lt;
   *     the smallest element, this returns 0. If x &gt; the largest element, this returns
   *     dar.length (no element is appropriate). If x is NaN, this is currently undefined.
   */
  public static int binaryFindFirstGAE(double[] dar, double x, int precision) {
    return Math2.binaryFindFirstGAE(dar, x, precision);
  }

  /**
   * Find the closest element to x in an ascending sorted array.
   *
   * @param dar an ascending sorted double[]. It the array has duplicates and x equals one of them,
   *     it isn't specified which duplicate's index will be returned.
   * @param x
   * @return the index of the index of the element closest to x. If x is NaN, this returns -1.
   */
  public static int binaryFindClosest(double[] dar, double x) {
    return Math2.binaryFindClosest(dar, x);
  }

  /**
   * This reduces a hash code (currently to a 10 digit unsigned number -- no loss of information).
   * For much stronger than hashCode, use String2.md5Hex or even String2.md5Hex12.
   *
   * @param hashCode
   * @return the reduced version
   */
  public static String reduceHashCode(int hashCode) {
    return Math2.reduceHashCode(hashCode);
  }

  /** This returns the lesser value. If one is NaN and the other isn't, this returns the other. */
  public static double finiteMin(double a, double b) {
    return Math2.finiteMin(a, b);
  }

  /** This returns the greater value. If one is NaN and the other isn't, this returns the other. */
  public static double finiteMax(double a, double b) {
    return Math2.finiteMax(a, b);
  }

  /**
   * This returns true if a == b. This treats as true: NaN=NaN (which Java says is false),
   * NEGATIVE_INFINITY=NEGATIVE_INFINITY, and POSITIVE_INFINITY=POSITIVE_INFINITY.
   *
   * @param a
   * @param b
   * @return true if a == b.
   */
  public static boolean equalsIncludingNanOrInfinite(double a, double b) {
    return Math2.equalsIncludingNanOrInfinite(a, b);
  }

  /**
   * This returns true if a == b. This treats as true: NaN=NaN (which Java says is false),
   * NEGATIVE_INFINITY=NEGATIVE_INFINITY, and POSITIVE_INFINITY=POSITIVE_INFINITY.
   *
   * @param a
   * @param b
   * @return true if a == b.
   */
  public static boolean equalsIncludingNanOrInfinite(float a, float b) {
    return Math2.equalsIncludingNanOrInfinite(a, b);
  }
}
