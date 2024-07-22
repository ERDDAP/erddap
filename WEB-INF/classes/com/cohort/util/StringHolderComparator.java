/* This file is part of the EMA project and is
 * Copyright (c) 2019 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import java.util.Comparator;

/**
 * This is used by StringArray to do a case-insensitive sort (better than
 * String.CASE_INSENSITIVE_ORDER).
 */
public class StringHolderComparator implements Comparator<StringHolder> {

  /**
   * This is required for the Comparator interface.
   *
   * @param o1
   * @param o2
   * @return returns a negative integer, zero, or a positive integer if the value at index1 is less
   *     than, equal to, or greater than the value at index2. Think "o1 - o2".
   */
  @Override
  public int compare(StringHolder o1, StringHolder o2) {
    if (o1 == null) return o2 == null ? 0 : -1;
    return o1.compareTo(o2);
  }

  /**
   * This is required for the Comparator interface.
   *
   * @param obj usually another RowComparator
   */
  @Override
  public boolean equals(Object obj) {
    return obj != null && obj instanceof StringHolderComparator;
  }

  @Override
  public int hashCode() {
    // There is nothing meaningul to hash.
    return 1;
  }
}
