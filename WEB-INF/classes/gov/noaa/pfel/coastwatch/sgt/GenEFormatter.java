/*
 * GenEFormatter Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.String2;

/** This formats numbers with a general number formatter. */
public class GenEFormatter implements NumberFormatter {

  /**
   * This formats a numbers with a general number formatter.
   *
   * @param d a double
   * @return the formatted value. NaN returns "NaN".
   */
  @Override
  public String format(double d) {
    return String2.genEFormat6(d);
  }

  /**
   * This formats a numbers with a general number formatter. There is no "NaN" test in this method.
   *
   * @param l a long value
   * @return the formatted value.
   */
  @Override
  public String format(long l) {
    return "" + l;
  }
}
