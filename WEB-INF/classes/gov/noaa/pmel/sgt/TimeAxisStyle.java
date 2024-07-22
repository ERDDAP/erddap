/*
 * $Id: TimeAxisStyle.java,v 1.4 2000/12/19 01:00:38 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt;

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.TimeRange;

/**
 * <code>TimeAxisStyle</code> defines an interface to create a specific time axis style. Currently
 * there are five time axes styles, <code>MINUTE_HOUR</code>, <code>HOUR_DAY</code>, <code>DAY_MONTH
 * </code>, <code>MONTH_YEAR</code>, and <code>YEAR_DECADE</code>. All time axes have two labeling
 * levels, minor and major. For example, <code>DAY_MONTH</code> style has a minor level of days and
 * a major level of months.
 *
 * @see TimeAxis
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2000/12/19 01:00:38 $
 * @since 1.0
 */
public interface TimeAxisStyle {
  /**
   * Get the minor time value for labeling.
   *
   * @param time current date
   * @return minor time value
   */
  public int getMinorValue(GeoDate time);

  /**
   * Get the major time value for labeling.
   *
   * @param time current date
   * @return major time value
   */
  public int getMajorValue(GeoDate time);

  /**
   * Determines if there is enough room in <code>delta</code> time for another major label.
   *
   * @return true if enough room exists
   */
  public boolean isRoomForMajorLabel(GeoDate delta);

  /**
   * Determines if <code>time</code> is the start of a minor interval.
   *
   * @return true if start of minor interval
   */
  public boolean isStartOfMinor(GeoDate time);

  /**
   * Get the default minor label format. The default minor labels are "mm", "HH", "dd", "MMM", and
   * "yy" for <code>MINUTE_HOUR</code>, <code>HOUR_DAY</code>, <code>DAY_MONTH</code>, <code>
   * MONTH_YEAR</code>, and <code>YEAR_DECADE</code>, respectively.
   *
   * @return minor label format
   */
  public String getDefaultMinorLabelFormat();

  /**
   * Get the default major label format. The default major labels are (2011-12-15 Bob Simons changed
   * space to 'T') "yyyy-MM-dd'T'HH", "yyyy-MM-dd", "yyyy-MM", "yyyy", and "yyyy" for <code>
   * MINUTE_HOUR</code>, <code>HOUR_DAY</code>, <code>DAY_MONTH</code>, <code>MONTH_YEAR</code>, and
   * <code>YEAR_DECADE</code>, respectively.
   *
   * @return major label format
   */
  public String getDefaultMajorLabelFormat();

  /**
   * Get the default minor label interval.
   *
   * @return minor label interval
   */
  public int getDefaultMinorLabelInterval();

  /**
   * Get the default major label interval.
   *
   * @return major label interval
   */
  public int getDefaultMajorLabelInterval();

  /**
   * Get the default number of small tics between each minor tic.
   *
   * @return number of small tics
   */
  public int getDefaultNumSmallTics();

  /**
   * Returns a beginning time rounded to the nearest minor increment. For example, for <code>
   * DAY_MONTH</code> if time is increasing then round to the day before <code>tRange.start</code>
   * otherwise the nearest day after <code>tRange.end</code>.
   *
   * @param tRange time range of the axis
   */
  public GeoDate getStartTime(TimeRange trange);

  /**
   * Get the increment value for the minor labeling. The value is 1.0 for all styles.
   *
   * @return increment value
   */
  public double getIncrementValue();

  /**
   * Get the increment units for the minor labeling. The value is <code>GeoDate.MINUTES</code>,
   * <code>GeoDate.HOURS</code>, <code>GeoDate.DAYS</code>, <code>GeoDate.MONTHS</code>, and <code>
   * GoeDate.YEARS</code> for <code>MINUTE_HOUR</code>, <code>HOUR_DAY</code>, <code>DAY_MONTH
   * </code>, <code>MONTH_YEAR</code>, and <code>YEAR_DECADE</code>, respectively.
   *
   * @return increment units
   * @see GeoDate
   */
  public int getIncrementUnits();

  /**
   * Determine the minor label interval from the time extent of the axis. For example, if <code>
   * delta</code> is greater than 30 days, greater than 10 and less that 30 days, or less than 10
   * days, the interval is 5, 2, or 1, respectively, for <code>DAY_MONTH</code> style.
   *
   * @param delta time extent
   */
  public void computeDefaults(GeoDate delta);

  /**
   * Determines the location of the minor time label. Positions the label between the tic marks for
   * <code>DAY_MONTH</code>, <code>MONTH_YEAR</code>, and <code>YEAR_DECADE</code>, or at the tic
   * mark for <code>MINUTES_HOURS</code> and <code>HOURS_DAYS</code>.
   *
   * @param prev previous tic location
   * @param now current tic location
   */
  public double computeLocation(double prev, double now);

  @Override
  public String toString();
}
