/*
 * $Id: HourDayAxis.java,v 1.8 2001/01/05 18:59:27 dwd Exp $
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
import gov.noaa.pmel.util.IllegalTimeValue;
import gov.noaa.pmel.util.TimeRange;

/**
 * Draws time axes using the hour/day style.
 *
 * <pre>
 *                  |..........|..........|..........|..........|
 *                       3           4         5           6
 *                                    jun 7
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2001/01/05 18:59:27 $
 * @see Axis
 * @see TimeAxis
 */
public class HourDayAxis implements TimeAxisStyle {
  static final int HOUR_TEST__ = 4;
  static final String defaultMinorLabelFormat__ = "HH";
  static final String defaultMajorLabelFormat__ = "yyyy-MM-dd";
  static final int defaultNumSmallTics__ = 0;
  int defaultMinorLabelInterval_ = 2;
  int defaultMajorLabelInterval_ = 1;
  static final double incrementValue__ = 1.0;
  static final int incrementUnits__ = GeoDate.HOURS;

  /**
   * HourDayAxis constructor.
   *
   * @param id axis identifier
   */
  public HourDayAxis() {}

  @Override
  public double computeLocation(double prev, double now) {
    return prev;
  }

  @Override
  public void computeDefaults(GeoDate delta) {
    long days = Math.abs(delta.getTime()) / GeoDate.MSECS_IN_DAY;
    long msec = Math.abs(delta.getTime()) % GeoDate.MSECS_IN_DAY;
    if (days >= 6) {
      defaultMinorLabelInterval_ = 12;
    } else if (days >= 2) {
      defaultMinorLabelInterval_ = 6;
    } else if ((days > 0) || (msec > 43200000)) { // 12 hrs
      defaultMinorLabelInterval_ = 2;
    } else {
      defaultMinorLabelInterval_ = 1;
    }
  }

  @Override
  public int getMinorValue(GeoDate time) {
    return time.getGMTHours();
  }

  @Override
  public int getMajorValue(GeoDate time) {
    return time.getGMTDay();
  }

  @Override
  public boolean isRoomForMajorLabel(GeoDate delta) {
    return 24.0 * (((double) delta.getTime()) / ((double) GeoDate.MSECS_IN_DAY)) > HOUR_TEST__;
  }

  @Override
  public boolean isStartOfMinor(GeoDate time) {
    return time.getGMTHours() == 0;
  }

  @Override
  public String getDefaultMinorLabelFormat() {
    return defaultMinorLabelFormat__;
  }

  @Override
  public String getDefaultMajorLabelFormat() {
    return defaultMajorLabelFormat__;
  }

  @Override
  public int getDefaultNumSmallTics() {
    return defaultNumSmallTics__;
  }

  @Override
  public int getDefaultMinorLabelInterval() {
    return defaultMinorLabelInterval_;
  }

  @Override
  public int getDefaultMajorLabelInterval() {
    return defaultMajorLabelInterval_;
  }

  @Override
  public GeoDate getStartTime(TimeRange tRange) {
    boolean time_increasing;
    GeoDate time = null;
    time_increasing = tRange.end.after(tRange.start);
    try {
      if (time_increasing) {
        time =
            new GeoDate(
                tRange.start.getGMTMonth(),
                tRange.start.getGMTDay(),
                tRange.start.getGMTYear(),
                tRange.start.getGMTHours(),
                0,
                0,
                0);
        if (!time.equals(tRange.start)) time.increment(1.0f, GeoDate.HOURS);
      } else {
        time =
            new GeoDate(
                tRange.end.getGMTMonth(),
                tRange.end.getGMTDay(),
                tRange.end.getGMTYear(),
                tRange.end.getGMTHours(),
                0,
                0,
                0);
        if (!time.equals(tRange.end)) time.increment(1.0f, GeoDate.HOURS);
      }
    } catch (IllegalTimeValue e) {
    }
    return time;
  }

  @Override
  public double getIncrementValue() {
    return incrementValue__;
  }

  @Override
  public int getIncrementUnits() {
    return incrementUnits__;
  }

  @Override
  public String toString() {
    return "HourDayAxis inc="
        + incrementValue__
        + " minorLabelInterval="
        + defaultMinorLabelInterval_;
  }
}
