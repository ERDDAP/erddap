/* This file is Copyright (c) 2019 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 */
package com.cohort.util;

/**
 * This class makes almost all of the static methods in java.lang.Math accessible to JexlScript
 * scripts as "Math.<i>name</i>()" methods.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2019-11-14
 */
public class ScriptMath {

  /** The double value that is closer than any other to e, the base of the natural logarithms. */
  public static final double E = Math.E;

  /**
   * The double value that is closer than any other to pi, the ratio of the circumference of a
   * circle to its diameter.
   */
  public static final double PI = Math.PI;

  /** Returns the absolute value of a double value. */
  public static double abs(double a) {
    return Math.abs(a);
  }

  /** Returns the absolute value of a float value. */
  public static float abs(float a) {
    return Math.abs(a);
  }

  /** Returns the absolute value of an int value. */
  public static int abs(int a) {
    return Math.abs(a);
  }

  /** Returns the absolute value of a long value. */
  public static long abs(long a) {
    return Math.abs(a);
  }

  /** Returns the arc cosine of a value; the returned angle is in the range 0.0 through pi. */
  public static double acos(double a) {
    return Math.acos(a);
  }

  /** Returns the sum of its arguments, throwing an exception if the result overflows an int. */
  public static int addExact(int x, int y) {
    return Math.addExact(x, y);
  }

  /** Returns the sum of its arguments, throwing an exception if the result overflows a long. */
  public static long addExact(long x, long y) {
    return Math.addExact(x, y);
  }

  /** Returns the arc sine of a value; the returned angle is in the range -pi/2 through pi/2. */
  public static double asin(double a) {
    return Math.asin(a);
  }

  /** Returns the arc tangent of a value; the returned angle is in the range -pi/2 through pi/2. */
  public static double atan(double a) {
    return Math.atan(a);
  }

  /**
   * Returns the angle theta from the conversion of rectangular coordinates (x, y) to polar
   * coordinates (r, theta).
   */
  public static double atan2(double y, double x) {
    return Math.atan2(y, x);
  }

  /** Returns the cube root of a double value. */
  public static double cbrt(double a) {
    return Math.cbrt(a);
  }

  /**
   * Returns the smallest (closest to negative infinity) double value that is greater than or equal
   * to the argument and is equal to a mathematical integer.
   */
  public static double ceil(double a) {
    return Math.ceil(a);
  }

  /**
   * Returns the first floating-point argument with the sign of the second floating-point argument.
   */
  public static double copySign(double magnitude, double sign) {
    return Math.copySign(magnitude, sign);
  }

  /**
   * Returns the first floating-point argument with the sign of the second floating-point argument.
   */
  public static float copySign(float magnitude, float sign) {
    return Math.copySign(magnitude, sign);
  }

  /** Returns the trigonometric cosine of an angle. */
  public static double cos(double a) {
    return Math.cos(a);
  }

  /** Returns the hyperbolic cosine of a double value. */
  public static double cosh(double x) {
    return Math.cosh(x);
  }

  /**
   * Returns the argument decremented by one, throwing an exception if the result overflows an int.
   */
  public static int decrementExact(int a) {
    return Math.decrementExact(a);
  }

  /**
   * Returns the argument decremented by one, throwing an exception if the result overflows a long.
   */
  public static long decrementExact(long a) {
    return Math.decrementExact(a);
  }

  /** Returns Euler's number e raised to the power of a double value. */
  public static double exp(double a) {
    return Math.exp(a);
  }

  /** Returns ex -1. */
  public static double expm1(double x) {
    return Math.expm1(x);
  }

  /**
   * Returns the largest (closest to positive infinity) double value that is less than or equal to
   * the argument and is equal to a mathematical integer.
   */
  public static double floor(double a) {
    return Math.floor(a);
  }

  /**
   * Returns the largest (closest to positive infinity) int value that is less than or equal to the
   * algebraic quotient.
   */
  public static int floorDiv(int x, int y) {
    return Math.floorDiv(x, y);
  }

  /**
   * Returns the largest (closest to positive infinity) long value that is less than or equal to the
   * algebraic quotient.
   */
  public static long floorDiv(long x, long y) {
    return Math.floorDiv(x, y);
  }

  /** Returns the floor modulus of the int arguments. */
  public static int floorMod(int x, int y) {
    return Math.floorMod(x, y);
  }

  /** Returns the floor modulus of the long arguments. */
  public static long floorMod(long x, long y) {
    return Math.floorMod(x, y);
  }

  /** Returns the unbiased exponent used in the representation of a double. */
  public static int getExponent(double d) {
    return Math.getExponent(d);
  }

  /** Returns the unbiased exponent used in the representation of a float. */
  public static int getExponent(float f) {
    return Math.getExponent(f);
  }

  /** Returns sqrt(x2 +y2) without intermediate overflow or underflow. */
  public static double hypot(double x, double y) {
    return Math.hypot(x, y);
  }

  /** Computes the remainder operation on two arguments as prescribed by the IEEE 754 standard. */
  public static double IEEEremainder(double f1, double f2) {
    return Math.IEEEremainder(f1, f2);
  }

  /**
   * Returns the argument incremented by one, throwing an exception if the result overflows an int.
   */
  public static int incrementExact(int a) {
    return Math.incrementExact(a);
  }

  /**
   * Returns the argument incremented by one, throwing an exception if the result overflows a long.
   */
  public static long incrementExact(long a) {
    return Math.incrementExact(a);
  }

  /** Returns the natural logarithm (base e) of a double value. */
  public static double log(double a) {
    return Math.log(a);
  }

  /** Returns the base 10 logarithm of a double value. */
  public static double log10(double a) {
    return Math.log10(a);
  }

  /** Returns the natural logarithm of the sum of the argument and 1. */
  public static double log1p(double x) {
    return Math.log1p(x);
  }

  /** Returns the greater of two double values. */
  public static double max(double a, double b) {
    return Math.max(a, b);
  }

  /** Returns the greater of two float values. */
  public static float max(float a, float b) {
    return Math.max(a, b);
  }

  /** Returns the greater of two int values. */
  public static int max(int a, int b) {
    return Math.max(a, b);
  }

  /** Returns the greater of two long values. */
  public static long max(long a, long b) {
    return Math.max(a, b);
  }

  /** Returns the smaller of two double values. */
  public static double min(double a, double b) {
    return Math.min(a, b);
  }

  /** Returns the smaller of two float values. */
  public static float min(float a, float b) {
    return Math.min(a, b);
  }

  /** Returns the smaller of two int values. */
  public static int min(int a, int b) {
    return Math.min(a, b);
  }

  /** Returns the smaller of two long values. */
  public static long min(long a, long b) {
    return Math.min(a, b);
  }

  /** Returns the product of the arguments, throwing an exception if the result overflows an int. */
  public static int multiplyExact(int x, int y) {
    return Math.multiplyExact(x, y);
  }

  /** Returns the product of the arguments, throwing an exception if the result overflows a long. */
  public static long multiplyExact(long x, long y) {
    return Math.multiplyExact(x, y);
  }

  /** Returns the negation of the argument, throwing an exception if the result overflows an int. */
  public static int negateExact(int a) {
    return Math.negateExact(a);
  }

  /** Returns the negation of the argument, throwing an exception if the result overflows a long. */
  public static long negateExact(long a) {
    return Math.negateExact(a);
  }

  /**
   * Returns the floating-point number adjacent to the first argument in the direction of the second
   * argument.
   */
  public static double nextAfter(double start, double direction) {
    return Math.nextAfter(start, direction);
  }

  /**
   * Returns the floating-point number adjacent to the first argument in the direction of the second
   * argument.
   */
  public static float nextAfter(float start, double direction) {
    return Math.nextAfter(start, direction);
  }

  /** Returns the floating-point value adjacent to d in the direction of negative infinity. */
  public static double nextDown(double d) {
    return Math.nextDown(d);
  }

  /** Returns the floating-point value adjacent to f in the direction of negative infinity. */
  public static float nextDown(float f) {
    return Math.nextDown(f);
  }

  /** Returns the floating-point value adjacent to d in the direction of positive infinity. */
  public static double nextUp(double d) {
    return Math.nextUp(d);
  }

  /** Returns the floating-point value adjacent to f in the direction of positive infinity. */
  public static float nextUp(float f) {
    return Math.nextUp(f);
  }

  /** Returns the value of the first argument raised to the power of the second argument. */
  public static double pow(double a, double b) {
    return Math.pow(a, b);
  }

  /**
   * Returns a double value with a positive sign, greater than or equal to 0.0 and less than 1.0.
   */
  public static double random() {
    return Math.random();
  }

  /**
   * Returns the double value that is closest in value to the argument and is equal to a
   * mathematical integer.
   */
  public static double rint(double a) {
    return Math.rint(a);
  }

  /** Returns the closest long to the argument, with ties rounding to positive infinity. */
  public static long round(double a) {
    return Math.round(a);
  }

  /** Returns the closest int to the argument, with ties rounding to positive infinity. */
  public static int round(float a) {
    return Math.round(a);
  }

  /**
   * Returns d × 2scaleFactor rounded as if performed by a single correctly rounded floating-point
   * multiply to a member of the double value set.
   */
  public static double scalb(double d, int scaleFactor) {
    return Math.scalb(d, scaleFactor);
  }

  /**
   * Returns f × 2scaleFactor rounded as if performed by a single correctly rounded floating-point
   * multiply to a member of the float value set.
   */
  public static float scalb(float f, int scaleFactor) {
    return Math.scalb(f, scaleFactor);
  }

  /**
   * Returns the signum function of the argument; zero if the argument is zero, 1.0 if the argument
   * is greater than zero, -1.0 if the argument is less than zero.
   */
  public static double signum(double d) {
    return Math.signum(d);
  }

  /**
   * Returns the signum function of the argument; zero if the argument is zero, 1.0f if the argument
   * is greater than zero, -1.0f if the argument is less than zero.
   */
  public static float signum(float f) {
    return Math.signum(f);
  }

  /** Returns the trigonometric sine of an angle. */
  public static double sin(double a) {
    return Math.sin(a);
  }

  /** Returns the hyperbolic sine of a double value. */
  public static double sinh(double x) {
    return Math.sinh(x);
  }

  /** Returns the correctly rounded positive square root of a double value. */
  public static double sqrt(double a) {
    return Math.sqrt(a);
  }

  /**
   * Returns the difference of the arguments, throwing an exception if the result overflows an int.
   */
  public static int subtractExact(int x, int y) {
    return Math.subtractExact(x, y);
  }

  /**
   * Returns the difference of the arguments, throwing an exception if the result overflows a long.
   */
  public static long subtractExact(long x, long y) {
    return Math.subtractExact(x, y);
  }

  /** Returns the trigonometric tangent of an angle. */
  public static double tan(double a) {
    return Math.tan(a);
  }

  /** Returns the hyperbolic tangent of a double value. */
  public static double tanh(double x) {
    return Math.tanh(x);
  }

  /**
   * Converts an angle measured in radians to an approximately equivalent angle measured in degrees.
   */
  public static double toDegrees(double angrad) {
    return Math.toDegrees(angrad);
  }

  /**
   * Returns the value of the long argument; throwing an exception if the value overflows an int.
   */
  public static int toIntExact(long value) {
    return Math.toIntExact(value);
  }

  /**
   * Converts an angle measured in degrees to an approximately equivalent angle measured in radians.
   */
  public static double toRadians(double angdeg) {
    return Math.toRadians(angdeg);
  }

  /** Returns the size of an ulp of the argument. */
  public static double ulp(double d) {
    return Math.ulp(d);
  }

  /** Returns the size of an ulp of the argument. */
  public static float ulp(float f) {
    return Math.ulp(f);
  }
}
