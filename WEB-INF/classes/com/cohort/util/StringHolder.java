/* This file is part of the EMA project and is
 * Copyright (c) 2019 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import java.util.Arrays;

/**
 * StringHolder is a thin shell which holds an immutable String in a char[]. It implements with
 * equals() and hashCode() methods so it can be used in a Set or a Map.
 */
public class StringHolder implements Comparable<StringHolder> {

  private char[] car; // may be null
  private int hashCode = -1; // this uses extra memory but saves considerable time

  /**
   * A constructor.
   *
   * @param string can be null
   */
  public StringHolder(final String string) {
    car = string == null ? null : string.toCharArray();
  }

  /**
   * A constructor.
   *
   * @param tcar can be null
   */
  public StringHolder(final char tcar[]) {
    car = tcar;
  }

  /**
   * This returns the string.
   *
   * @return the string.
   */
  public String string() {
    return car == null ? null : car.length == 0 ? String2.EMPTY_STRING : new String(car);
  }

  /**
   * This returns the charArray, but don't change the contents!
   *
   * @return the charArray
   */
  public char[] charArray() {
    return car;
  }

  /**
   * This returns the number of characters.
   *
   * @return the number of characters
   */
  public int length() {
    return car == null ? 0 : car.length;
  }

  /**
   * This returns the hashcode for this StringHolder (dependent only on values).
   *
   * @return the hashcode for this StringHolder (dependent only on values).
   */
  @Override
  public int hashCode() {
    // return Arrays.hashCode(car); //it allows null and returns 0
    // return car == null? 0 : HashDigest.murmur32(car, car.length);

    if (hashCode == -1) hashCode = Arrays.hashCode(car); // it allows null and returns 0
    return hashCode;
  }

  /**
   * Test if o is a StringHolder with the same size and values.
   *
   * @param o
   * @return true if equal. o=null returns false
   */
  @Override
  public boolean equals(final Object o) {
    if (o == null || !(o instanceof StringHolder)) return false;
    return Arrays.equals(car, ((StringHolder) o).car); // either or both cars may be null
  }

  /**
   * Compare o to this.
   *
   * @param o
   * @return true if equal. o=null returns false
   */
  @Override
  public int compareTo(final StringHolder o) {
    if (o == null) return 1; // see StringComparatorIgnoreCase
    // see String compareTo documentation
    final char other[] = ((StringHolder) o).charArray();
    final int thisSize = car == null ? 0 : car.length;
    final int otherSize = other == null ? 0 : other.length;
    final int min = Math.min(thisSize, otherSize);
    for (int po = 0; po < min; po++) {
      final int result = car[po] - other[po];
      if (result != 0) return result;
    }
    return thisSize - otherSize;
  }

  /**
   * Compare o to this in a case insensitive way.
   *
   * @param o
   * @return this-o. o=null returns 1
   */
  public int compareToIgnoreCase(final StringHolder o) {
    if (o == null) return 1; // see StringComparatorIgnoreCase
    // see String compareTo documentation
    final char other[] = o.charArray();
    final int thisSize = car == null ? 0 : car.length;
    final int otherSize = other.length;
    final int min = Math.min(thisSize, otherSize);
    for (int po = 0; po < min; po++) {
      final int result = Character.toLowerCase(car[po]) - Character.toLowerCase(other[po]);
      if (result != 0) return result;
    }
    if (thisSize != otherSize) return thisSize - otherSize;
    return this.compareTo(o); // be nice and sort case-sensitive
  }

  /**
   * This returns the string.
   *
   * @return the string, which may be null
   */
  @Override
  public String toString() {
    return string();
  }
}
