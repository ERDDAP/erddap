/*
 * DegreeMinuteFormatter Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

/** This formats numbers as degrees and minutes. */
public class DegreeMinuteFormatter implements NumberFormatter {

  /**
   * This formats a decimal degree value formatted as "degree°minute'". If minutes=0, that part is
   * not displayed.
   *
   * @param d a decimal degree value
   * @return the formatted value. NaN returns "NaN".
   */
  @Override
  public String format(double d) {
    if (Double.isNaN(d)) return "NaN";

    // round to the nearest minute
    long min = Math.round(d * 60);

    // convert to degrees and minutes
    long degrees = Math.abs(min) / 60;
    long minutes = Math.abs(min) % 60;

    // return the formatted string
    return (min < 0 ? "-" : "")
        + // optional "-" sign
        degrees
        + "°"
        + (minutes > 0 ? minutes + "'" : ""); // optional minutes
  }

  /**
   * This formats a decimal degree value formatted as "degree°minute'". There is no "NaN" test in
   * this method.
   *
   * @param d a decimal degree value
   * @return the formatted value.
   */
  @Override
  public String format(long l) {
    return format((double) l);
  }
}
