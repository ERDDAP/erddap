/*
 * IntObject Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

/**
 * This class holds in int that can be changed (without repeatedly destroying an old object and
 * creating a new object). Thus, it is mutable, unlike an immutable Java Integer object.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-05-18
 */
public class IntObject {
  public int i;

  /**
   * The constructor.
   *
   * @param initialValue the initial value of i.
   */
  public IntObject(int initialValue) {
    i = initialValue;
  }

  /**
   * Returns the string representation of the int.
   *
   * @return the string representation of the int.
   */
  @Override
  public String toString() {
    return "" + i;
  }
}
