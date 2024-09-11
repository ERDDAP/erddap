/*
 * DoubleObject Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package com.cohort.util;

/**
 * This class holds a double that can be changed.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-05-18
 */
public class DoubleObject {
  public double d;

  /**
   * The constructor.
   *
   * @param initialValue the initial value of d.
   */
  public DoubleObject(double initialValue) {
    d = initialValue;
  }

  /**
   * Returns the string representation of the double.
   *
   * @return the string representation of the double.
   */
  @Override
  public String toString() {
    return "" + d;
  }
}
