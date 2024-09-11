/*
 * $Id: MinuteHourAxis.java,v 1.7 2001/01/05 18:59:27 dwd Exp $
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
 * Draws time axes using the minute/hour style.
 *
 * <pre>
 *                  |..........|..........|..........|..........|
 *                       10         20         30         40
 *                                 13:00 7-Jun-87
 * or
 *
 *                  |..........|..........|..........|..........|
 *                     13:10       13:20     13:30      13:40
 *                                     7-Jun-87
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2001/01/05 18:59:27 $
 * @see Axis
 * @see TimeAxis
 */
public class MinuteHourAxis implements TimeAxisStyle {
  static final int MINUTE_TEST__ = 31; // >0.5 hr
  static final String defaultMinorLabelFormat__ = "mm";
  // 2011-12-15 Bob Simons changed space to 'T'
  static final String defaultMajorLabelFormat__ = "yyyy-MM-dd'T'HH";
  static final int defaultNumSmallTics__ = 0;
  int defaultMinorLabelInterval_ = 2;
  int defaultMajorLabelInterval_ = 1;
  static final double incrementValue__ = 1.0;
  static final int incrementUnits__ = GeoDate.MINUTES;

  /**
   * MinuteHourAxis constructor.
   *
   * @param id axis identifier
   */
  public MinuteHourAxis() {}

  @Override
  public double computeLocation(double prev, double now) {
    return prev;
  }

  @Override
  public void computeDefaults(GeoDate delta) {
    long msec = Math.abs(delta.getTime()) % GeoDate.MSECS_IN_DAY;
    if (msec > 6000000) {
      defaultMinorLabelInterval_ = 15;
      defaultMajorLabelInterval_ = 2;
    } else if (msec > 1200000) {
      defaultMinorLabelInterval_ = 5;
      defaultMajorLabelInterval_ = 1;
    } else {
      defaultMinorLabelInterval_ = 1;
      defaultMajorLabelInterval_ = 1;
    }
  }

  @Override
  public int getMinorValue(GeoDate time) {
    return time.getGMTMinutes();
  }

  @Override
  public int getMajorValue(GeoDate time) {
    return time.getGMTHours();
  }

  @Override
  public boolean isRoomForMajorLabel(GeoDate delta) {
    return 1440.0 * (((double) delta.getTime()) / ((double) GeoDate.MSECS_IN_DAY)) > MINUTE_TEST__;
  }

  @Override
  public boolean isStartOfMinor(GeoDate time) {
    return time.getGMTMinutes() == 0;
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
                tRange.start.getGMTMinutes(),
                0,
                0);
        if (!time.equals(tRange.start)) time.increment(1.0, GeoDate.MINUTES);
      } else {
        time =
            new GeoDate(
                tRange.end.getGMTMonth(),
                tRange.end.getGMTDay(),
                tRange.end.getGMTYear(),
                tRange.end.getGMTHours(),
                tRange.end.getGMTMinutes(),
                0,
                0);
        if (!time.equals(tRange.end)) time.increment(1.0, GeoDate.MINUTES);
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
    return "MinuteHourAxis inc="
        + incrementValue__
        + " minorLabelInterval="
        + defaultMinorLabelInterval_;
  }
}
