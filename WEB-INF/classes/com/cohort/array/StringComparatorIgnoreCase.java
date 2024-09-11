/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import java.util.Comparator;

/**
 * This is used by StringArray to do a case-insensitive sort (better than
 * String.CASE_INSENSITIVE_ORDER). 2013-12-30 Bob modified this to work with any type of object (via
 * o.toString()), not just Strings.
 */
public class StringComparatorIgnoreCase implements Comparator {

  /**
   * This is required for the Comparator interface.
   *
   * @param o1
   * @param o2
   * @return returns a negative integer, zero, or a positive integer if the value at index1 is less
   *     than, equal to, or greater than the value at index2. Think "o1 - o2".
   */
  @Override
  public int compare(Object o1, Object o2) {
    if (o1 == null) return o2 == null ? 0 : -1;
    if (o2 == null) return 1;
    String s1 = o1.toString(); // as opposed to (String)o1, this works with many object types
    String s2 = o2.toString();
    int c = s1.compareToIgnoreCase(s2);
    if (c != 0) return c;
    return s1.compareTo(s2); // be nice and sort case-sensitive
  }

  /**
   * This is required for the Comparator interface.
   *
   * @param obj usually another RowComparator
   */
  @Override
  public boolean equals(Object obj) {
    return obj == this;
  }

  @Override
  public int hashCode() {
    // There is nothing meaningul to hash.
    return 1;
  }
}
