/*
 * DecimalDegreeFormatter Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.String2;

/** This formats numbers as decimal degrees. */
public class DecimalDegreeFormatter implements NumberFormatter {

  /**
   * This formats a decimal degree value formatted as "degree.dddddd°".
   *
   * @param d a decimal degree value
   * @return the formatted value. NaN returns "NaN".
   */
  @Override
  public String format(double d) {
    if (Double.isNaN(d)) return "NaN";

    return String2.genEFormat6(d) + "°";
  }

  /**
   * This formats a degree value formatted as "degree°". There is no "NaN" test in this method.
   *
   * @param d a decimal degree value
   * @return the formatted value.
   */
  @Override
  public String format(long l) {
    return l + "°";
  }
}
