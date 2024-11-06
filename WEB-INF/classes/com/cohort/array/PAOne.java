/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.Math2;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;

/**
 * This class holds a PrimitiveArray which always has 1 value. This it is like Java's Number, but
 * for PrimitiveArrays. The constructors set a specific PAType, which the PAOne uses for its entire
 * life.
 *
 * <p>Yes, you could use BigDecimal instead of this. The advantage is: using a PAOne to transfer the
 * values from one pa to another can be very efficient (one fast operation vs two slow operations).
 */
public class PAOne implements Comparable<PAOne> {

  private PrimitiveArray pa;
  private int elementSize;

  /** This constructs a paOne (with a value of 0) of the specified type. */
  public PAOne(PAType paType) {
    pa = PrimitiveArray.factory(paType, 1, true);
    elementSize = pa.elementSize();
  }

  /** This constructs a paOne of the specified type with a value derived from parsing 'value'. */
  public PAOne(PAType paType, String value) {
    pa = PrimitiveArray.factory(paType, 1, false);
    pa.addString(value);
    elementSize = pa.elementSize();
  }

  /** This constructs a paOne (with a value of 0) of the same type as the PrimitiveArray. */
  public PAOne(PrimitiveArray otherPA) {
    pa = PrimitiveArray.factory(otherPA.elementType(), 1, true);
    pa.setMaxIsMV(otherPA.getMaxIsMV());
    elementSize = pa.elementSize();
  }

  /** This constructs a paOne with the specified value. */
  public PAOne(PrimitiveArray otherPA, int index) {
    pa = PrimitiveArray.factory(otherPA.elementType(), 1, true);
    pa.setFromPA(0, otherPA, index);
    pa.setMaxIsMV(otherPA.getMaxIsMV());
    elementSize = pa.elementSize();
  }

  /** This makes a new PAOne which is a copy of the other PAOne. */
  public PAOne(PAOne tPAOne) {
    pa = PrimitiveArray.factory(tPAOne.pa.elementType(), 1, true);
    pa.setFromPA(0, tPAOne.pa, 0);
    pa.setMaxIsMV(tPAOne.pa.getMaxIsMV());
    elementSize = pa.elementSize();
  }

  /** This constructs a String paOne with the specified value. */
  public static final PAOne fromString(String s) {
    return new PAOne(PAType.STRING).setString(s);
  }

  /** This constructs a char paOne with the specified value. */
  public static final PAOne fromChar(char c) {
    PAOne paOne = new PAOne(PAType.CHAR);
    ((CharArray) paOne.pa()).set(0, c);
    return paOne;
  }

  /** This constructs a double paOne with the specified value. */
  public static final PAOne fromDouble(double d) {
    return new PAOne(PAType.DOUBLE).setDouble(d);
  }

  /** This constructs a float paOne with the specified value. */
  public static final PAOne fromFloat(float f) {
    return new PAOne(PAType.FLOAT).setFloat(f);
  }

  /** This constructs a long paOne with the specified value. */
  public static final PAOne fromLong(long i) {
    return new PAOne(PAType.LONG).setLong(i);
  }

  /** This constructs a ulong paOne with the specified value. */
  public static final PAOne fromULong(BigInteger bi) {
    return new PAOne(PAType.ULONG).setULong(bi);
  }

  /** This constructs an int paOne with the specified value. */
  public static final PAOne fromInt(int i) {
    return new PAOne(PAType.INT).setInt(i);
  }

  /**
   * This makes a new object which is a copy of this object.
   *
   * @return a new object, with the same elements. It will have a new backing array with a capacity
   *     equal to its size.
   */
  @Override
  public Object clone() {
    return new PAOne(pa, 0);
  }

  /**
   * This returns the pa held by this PAOne. Its size must remain 1.
   *
   * @return the pa held by this PAOne.
   */
  public final PrimitiveArray pa() {
    return pa;
  }

  /** This indicates if the PAOne is an integer type. */
  public final boolean isIntegerType() {
    return pa.isIntegerType();
  }

  /** This returns the PAType of this PAOne. */
  public final PAType paType() {
    return pa.elementType();
  }

  /**
   * This sets this PAOne's value.
   *
   * @param s a String
   * @return this PAOne, for convenience
   */
  public final PAOne setString(String s) {
    pa.setString(0, s);
    return this;
  }

  /**
   * Return a value from the array as a String (NaN and the cohort missing values appear as "", not
   * a value).
   *
   * @return For numeric types, this returns (String.valueOf(ar[index])) or "" (for missing value).
   *     If this PA is unsigned, this method returns the unsigned value.
   */
  public final String getString() {
    return pa.getString(0);
  }

  /**
   * This sets this PAOne's value.
   *
   * @param d a double
   * @return this PAOne, for convenience
   */
  public final PAOne setDouble(double d) {
    pa.setDouble(0, d);
    return this;
  }

  /**
   * This gets this PAOne's value as a double.
   *
   * @return this PAOne's value as a double.
   */
  public final double getDouble() {
    return pa.getDouble(0);
  }

  /**
   * This gets this PAOne's value as a double. This "raw" variant leaves missingValue from smaller
   * data types (e.g., ByteArray missingValue=127) AS IS.
   *
   * @return this PAOne's value as a double.
   */
  public final double getRawDouble() {
    return pa.getRawDouble(0);
  }

  /**
   * This gets this PAOne's value as a double. This "unsigned" variant treats signed integer types
   * as if they were unsigned types.
   *
   * @return this PAOne's value as a double.
   */
  public final double getUnsignedDouble() {
    return pa.getUnsignedDouble(0);
  }

  /**
   * This gets this PAOne's value as a double. This "nice" variant tries to promote floats nicely
   * (e.g., so they don't end in 99999 or 00001.
   *
   * @return this PAOne's value as a double.
   */
  public final double getNiceDouble() {
    return pa.getNiceDouble(0);
  }

  /**
   * This sets this PAOne's value.
   *
   * @param f a float
   * @return this PAOne, for convenience
   */
  public final PAOne setFloat(float f) {
    pa.setFloat(0, f);
    return this;
  }

  /**
   * This gets this PAOne's value as a float.
   *
   * @return this PAOne's value as a float.
   */
  public final float getFloat() {
    return pa.getFloat(0);
  }

  /**
   * This sets this PAOne's value.
   *
   * @param i a ULong
   * @return this PAOne, for convenience
   */
  public final PAOne setULong(BigInteger i) {
    pa.setULong(0, i);
    return this;
  }

  /**
   * This gets this PAOne's value as a ULong.
   *
   * @return this PAOne's value as a ULong (which may be null)
   */
  public final BigInteger getULong() {
    return pa.getULong(0);
  }

  /**
   * This sets this PAOne's value.
   *
   * @param i a long
   * @return this PAOne, for convenience
   */
  public final PAOne setLong(long i) {
    pa.setLong(0, i);
    return this;
  }

  /**
   * This gets this PAOne's value as a long.
   *
   * @return this PAOne's value as a long.
   */
  public final long getLong() {
    return pa.getLong(0);
  }

  /**
   * This sets this PAOne's value.
   *
   * @param i an int
   * @return this PAOne, for convenience
   */
  public final PAOne setInt(int i) {
    pa.setInt(0, i);
    return this;
  }

  /**
   * This gets this PAOne's value as an int.
   *
   * @return this PAOne's value as an int.
   */
  public final int getInt() {
    return pa.getInt(0);
  }

  /**
   * This sets this PAOne's value from the value in otherPAOne.
   *
   * @param otherPA the source PAOne which must be of the same (or smaller) PAType.
   * @return this PAOne, for convenience
   */
  public final PAOne readFrom(PAOne otherPA) {
    pa.setFromPA(0, otherPA.pa, 0);
    return this;
  }

  /**
   * This sets this PAOne's value from an element in otherPA.
   *
   * @param otherPA the source PAOne which must be of the same (or smaller) PAType.
   * @param index the source index in PAOne.
   * @return this PAOne, for convenience
   */
  public final PAOne readFrom(PrimitiveArray otherPA, int index) {
    pa.setFromPA(0, otherPA, index);
    return this;
  }

  /**
   * This sets an element of otherPA from the value in this PAOne.
   *
   * @param otherPA the destination PAOne which must be of the same (or smaller) PAType.
   * @param index the destination index in PAOne.
   */
  public final void writeTo(PrimitiveArray otherPA, int index) {
    otherPA.setFromPA(index, pa, 0);
  }

  /**
   * This adds this PAOne's value to the otherPA.
   *
   * @param otherPA the destination PAOne which must be of the same (or smaller) PAType.
   */
  public final void addTo(PrimitiveArray otherPA) {
    otherPA.addFromPA(pa, 0);
  }

  /**
   * This indicates if the PAOne's value is a missing value. For integerTypes, isMissingValue can
   * only be true if maxIsMv is 'true'.
   */
  public final boolean isMissingValue() {
    return pa.isMissingValue(0);
  }

  /**
   * This compares this object's value to the specified object's value.
   *
   * @param otherPA the other PA which must be of the same (or smaller) PAType.
   * @param index the index in otherPA
   * @return a negative integer (if this is less than Other), zero (if this is same as Other), or a
   *     positive integer (if this is greater than Other). Think "this - other".
   */
  public final int compareTo(PrimitiveArray otherPA, int index) {
    return pa.compare(0, otherPA, index);
  }

  /**
   * This compares this object's value to the other object's value.
   *
   * @param otherPAOne the other PA which must be of the same (or smaller) PAType.
   * @return a negative integer (if this is less than Other), zero (if this is same as Other), or a
   *     positive integer (if this is greater than Other). Think "this - other".
   */
  @Override
  public final int compareTo(PAOne otherPAOne) {
    return pa.compare(0, otherPAOne.pa, 0);
  }

  /**
   * This returns the lesser of this or otherPA. If this or other isNaN, this returns the non-NaN
   * value.
   */
  public final PAOne min(PAOne otherPAOne) {
    if (isMissingValue()) return otherPAOne;
    if (otherPAOne.isMissingValue()) return this;
    return this.compareTo(otherPAOne) <= 0 ? this : otherPAOne;
  }

  /**
   * This returns the max of this or otherPA. If this or other isNaN, this returns the non-NaN
   * value.
   */
  public final PAOne max(PAOne otherPAOne) {
    if (isMissingValue()) return otherPAOne;
    if (otherPAOne.isMissingValue()) return this;
    return this.compareTo(otherPAOne) > 0 ? this : otherPAOne;
  }

  /**
   * This test if this value equals the otherPA object's value.
   *
   * @param otherPA the other PA which must be of the same (or smaller) PAType.
   * @param index the index in otherPA
   * @return true if they are equal.
   */
  public final boolean equals(PrimitiveArray otherPA, int index) {
    return pa.compare(0, otherPA, index) == 0;
  }

  /**
   * This returns true if the values are equal.
   *
   * @param otherPAOne the other PA which must be of the same (or smaller) PAType.
   * @return true if the values are equal. This returns false if otherPAOne is null.
   */
  @Override
  public final boolean equals(Object otherPAOne) {
    if (otherPAOne == null || !(otherPAOne instanceof PAOne)) {
      return false;
    }
    return compareTo((PAOne) otherPAOne) == 0;
  }

  @Override
  public int hashCode() {
    return pa.hashCode();
  }

  /**
   * This returns true of the values are almost equal.
   *
   * @param precision
   * @param otherPAOne the other PA which must be of the same (or smaller) PAType.
   * @return true if the values are almost equal. Only FLOAT and DOUBLE test 'almost'. Other classes
   *     do pure ==. This returns false if otherPAOne is null.
   */
  public boolean almostEqual(int precision, PAOne otherPAOne) {
    if (otherPAOne == null) return false;
    PAType type1 = pa.elementType();
    PAType type2 = otherPAOne.paType();
    if (type1 == PAType.DOUBLE || type2 == PAType.DOUBLE)
      return Math2.almostEqual(precision, pa.getDouble(0), otherPAOne.pa.getDouble(0));
    else if (type1 == PAType.FLOAT || type2 == PAType.FLOAT)
      return Math2.almostEqual(precision, pa.getFloat(0), otherPAOne.pa.getFloat(0));
    else return compareTo(otherPAOne) == 0;
  }

  /**
   * For numeric types, this multiplies the value by value.
   *
   * @param value some value
   * @return this PAOne for convenience.
   */
  public PAOne multiply(PAOne value) {
    PAType paType = paType();
    if (paType == PAType.ULONG) {
      BigInteger bi1 = getULong();
      BigInteger bi2 = value.getULong();
      setULong(bi1 == null || bi2 == null ? null : bi1.multiply(bi2));

    } else if (isIntegerType()) {
      setLong(getLong() * value.getLong());

    } else if (paType == PAType.FLOAT) {
      setFloat(getFloat() * value.getFloat());

    } else if (paType == PAType.DOUBLE) {
      setDouble(getDouble() * value.getDouble());
    }

    // skip String and char

    return this;
  }

  /**
   * For numeric types, this adds the value by value.
   *
   * @param value some value
   * @return this PAOne for convenience.
   */
  public PAOne add(PAOne value) {
    PAType paType = paType();
    if (paType == PAType.ULONG) {
      BigInteger bi1 = getULong();
      BigInteger bi2 = value.getULong();
      setULong(bi1 == null || bi2 == null ? null : bi1.add(bi2));

    } else if (isIntegerType()) {
      setLong(getLong() + value.getLong());

    } else if (paType == PAType.FLOAT) {
      setFloat(getFloat() + value.getFloat());

    } else if (paType == PAType.DOUBLE) {
      setDouble(getDouble() + value.getDouble());
    }

    // skip String and char

    return this;
  }

  /**
   * This returns a string representation of the value. Integer types show MAX_VALUE number (not
   * "").
   */
  @Override
  public final String toString() {
    return pa.toString();
  }

  /**
   * This reads one number from the current position in the randomAccessFile. This doesn't support
   * StringArray (for which you need nBytesPer) and in which you generally wouldn't be storing
   * numbers.
   *
   * @param raf the RandomAccessFile, which MUST have data of the same PAType as this PAOne.
   * @throws Exception if trouble
   */
  public final void readFromRAF(RandomAccessFile raf) throws Exception {
    pa.clear();
    pa.readFromRAF(raf);
  }

  /**
   * This reads one number from a randomAccessFile. This doesn't support StringArray (for which you
   * need nBytesPer) and in which you generally wouldn't be storing numbers.
   *
   * @param raf the RandomAccessFile, which MUST have data of the same PAType as this PAOne.
   * @param start the raf offset of the start of the array (nBytes)
   * @param index the index of the desired value (0..)
   * @return this (for convenience)
   * @throws Exception if trouble
   */
  public final PAOne readFromRAF(RandomAccessFile raf, long start, long index) throws Exception {
    raf.seek(start + elementSize * index);
    pa.clear();
    pa.readFromRAF(raf);
    return this;
  }

  /**
   * This writes the one number to a randomAccessFile at the current position. This doesn't support
   * StringArray (for which you need nBytesPer) and in which you generally wouldn't be storing
   * numbers.
   *
   * @param raf the RandomAccessFile
   * @throws Exception if trouble
   */
  public final void writeToRAF(RandomAccessFile raf) throws Exception {
    pa.writeToRAF(raf, 0);
  }

  /**
   * This writes the one number to a randomAccessFile. This doesn't support StringArray (for which
   * you need nBytesPer) and in which you generally wouldn't be storing numbers.
   *
   * @param raf the RandomAccessFile
   * @param start the raf offset of the start of the array (nBytes)
   * @param index the index of the value (0..)
   * @throws Exception if trouble
   */
  public final void writeToRAF(RandomAccessFile raf, long start, long index) throws Exception {
    raf.seek(start + elementSize * index);
    pa.writeToRAF(raf, 0);
  }

  /**
   * This writes the one number to a DataOutputStream. This doesn't yet support StringArray (for
   * which you need nBytesPer) and in which you generally wouldn't be storing numbers.
   *
   * @param dos the DataOutputStream
   * @throws Exception if trouble
   */
  public final void writeToDOS(DataOutputStream dos) throws Exception {
    pa.writeDos(dos, 0);
  }

  /**
   * This converts a PAOne[] into a double[]. MAX_VALUE values are converted to Double.NaN values.
   *
   * @param paOneAr a BigDecimal array
   */
  public static double[] toDoubleArray(PAOne paOneAr[]) {
    if (paOneAr == null) return null;
    int n = paOneAr.length;
    double dar[] = new double[n];
    for (int i = 0; i < n; i++) dar[i] = paOneAr[i].getDouble();
    return dar;
  }
}
