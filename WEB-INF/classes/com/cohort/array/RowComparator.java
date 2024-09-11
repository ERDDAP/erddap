/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.*;
import java.util.Comparator;
import java.util.List;

/** This is used by PrimitiveArray.rank to rank a table of data stored as a PrimitiveArray[]. */
public class RowComparator implements Comparator<Integer> {

  protected List<PrimitiveArray> table;
  protected int[] keys;
  protected boolean[] ascending;

  /**
   * A constructor used by PrimitiveArray.rank.
   *
   * @param table a List of PrimitiveArrays
   * @param keys an array of the key column numbers (each is 0..nColumns-1, the first key is the
   *     most important) which are used to determine the sort order
   * @param ascending an array of booleans corresponding to the keys indicating if the arrays are to
   *     be sorted by a given key in ascending or descending order.
   * @throws RuntimeException if trouble
   */
  public RowComparator(List<PrimitiveArray> table, int keys[], boolean[] ascending) {
    String errorInMethod = String2.ERROR + " in RowComparator constructor:\n";
    Test.ensureNotEqual(keys.length, 0, errorInMethod + "keys.length must not be 0.");
    Test.ensureEqual(
        keys.length, ascending.length, errorInMethod + "keys.length must equal ascending.length.");
    for (int k = 0; k < keys.length; k++)
      Test.ensureBetween(
          keys[k],
          0,
          table.size(),
          errorInMethod + "keys[" + k + "] points to a column that isn't in the table.");
    this.table = table;
    this.keys = keys;
    this.ascending = ascending;
  }

  /**
   * This is required for the Comparator interface.
   *
   * @param o1 an Integer with the row number (0 ... size-1)
   * @param o2 an Integer with the row number (0 ... size-1)
   * @return returns a negative integer, zero, or a positive integer if the value at index1 is less
   *     than, equal to, or greater than the value at index2. Think "o1 - o2".
   */
  @Override
  public int compare(Integer o1, Integer o2) {
    for (int k = 0; k < keys.length; k++) {
      int result = table.get(keys[k]).compare(o1, o2);
      if (result != 0) return ascending[k] ? result : -result;
    }
    return 0;
  }

  /**
   * This is required for the Comparator interface.
   *
   * @param obj usually another RowComparator
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RowComparator) {
    } else return false;

    // make sure all the keys and ascending values are the same
    RowComparator other = (RowComparator) obj;
    if (other.table.size() != table.size()
        || other.keys.length != keys.length
        || other.ascending.length != ascending.length) return false;
    for (int t = 0; t < table.size(); t++) {
      if (other.table.get(t) != table.get(t)) // use != to test if they are the same object
      return false;
    }
    for (int k = 0; k < keys.length; k++) {
      if (other.keys[k] != keys[k]
          || // use != to test if they are the same object
          other.ascending[k] != ascending[k]) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = table.hashCode();
    for (int i = 0; i < keys.length; i++) {
      hash = 31 * hash + keys[i];
      hash = 31 * hash + (ascending[i] ? 1 : 0);
    }
    return hash;
  }
}
