/*
 * 2013-02-28 Bob Simons created MilliSecondAxis based on SecondMinuteAxis.
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
 * Draws time axes using the millis/second style.
 *
 * <pre>
 *                  |..........|..........|..........|
 *                .100       .200       .300       .400
 *                         1987-01-02T12:13:28
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2001/01/05 18:59:27 $
 * @see Axis
 * @see TimeAxis
 */
public class MilliSecondAxis implements TimeAxisStyle {
  static final int MILLIS_TEST__ = 501; // >0.5 sec
  static final String defaultMinorLabelFormat__ = ".SSS";
  // 2011-12-15 Bob Simons changed space to 'T'
  static final String defaultMajorLabelFormat__ = "yyyy-MM-dd'T'HH:mm:ss";
  static final int defaultNumSmallTics__ = 0;
  int defaultMinorLabelInterval_ = 2;
  int defaultMajorLabelInterval_ = 1;
  double incrementValue__ = 1.0;
  static final int incrementUnits__ = GeoDate.MSEC;

  /**
   * MiliSecondAxis constructor.
   *
   * @param id axis identifier
   */
  public MilliSecondAxis() {}

  @Override
  public double computeLocation(double prev, double now) {
    return prev;
  }

  @Override
  public void computeDefaults(GeoDate delta) {
    long msec = Math.abs(delta.getTime()) % GeoDate.MSECS_IN_DAY;
    incrementValue__ = 1.0; // default
    if (msec > 4000) {
      defaultMinorLabelInterval_ = 1000; // labels every 1000 msec
      incrementValue__ = 200; // tics every 200 msec
    } else if (msec > 2000) {
      defaultMinorLabelInterval_ = 500;
      incrementValue__ = 100;
    } else if (msec > 800) {
      defaultMinorLabelInterval_ = 200;
      incrementValue__ = 50;
    } else if (msec > 400) {
      defaultMinorLabelInterval_ = 100;
      incrementValue__ = 20;
    } else if (msec > 200) {
      defaultMinorLabelInterval_ = 50;
      incrementValue__ = 10;
    } else if (msec > 80) {
      defaultMinorLabelInterval_ = 20;
      incrementValue__ = 5;
    } else if (msec > 40) {
      defaultMinorLabelInterval_ = 10;
    } else if (msec > 20) {
      defaultMinorLabelInterval_ = 5;
    } else if (msec > 8) {
      defaultMinorLabelInterval_ = 2;
    } else {
      defaultMinorLabelInterval_ = 1;
    }

    if (msec > 2000) defaultMajorLabelInterval_ = 2; // e.g., 2=everyOther  getMajorValue->seconds
    // else 1

    // System.out.println("  MilliSecondAxis range msec=" + msec + " majorLabelInterval=" +
    // defaultMajorLabelInterval_ +
    //    " minorLabelInter=" + defaultMinorLabelInterval_ + " increment=" + incrementValue__);
  }

  @Override
  public int getMinorValue(GeoDate time) {
    // System.out.println("  gmtMillis time=" + time.getTime() + " sec=" + time.getGMTSeconds() + "
    // millis=" + time.getGMTMillis());
    return time.getGMTMillis();
  }

  @Override
  public int getMajorValue(GeoDate time) {
    return time.getGMTSeconds();
  }

  @Override
  public boolean isRoomForMajorLabel(GeoDate delta) {
    return delta.getGMTMillis() > MILLIS_TEST__;
  }

  @Override
  public boolean isStartOfMinor(GeoDate time) {
    return time.getGMTMillis() == 0; // true -> thick tick
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
    int intIncrement = (int) incrementValue__;
    try {
      if (time_increasing) {
        time =
            new GeoDate(
                tRange.start.getGMTMonth(),
                tRange.start.getGMTDay(),
                tRange.start.getGMTYear(),
                tRange.start.getGMTHours(),
                tRange.start.getGMTMinutes(),
                tRange.start.getGMTSeconds(),
                tRange.start.getGMTMillis() / intIncrement * intIncrement);
        if (!time.equals(tRange.start)) time.increment(intIncrement, GeoDate.MSEC);
      } else {
        time =
            new GeoDate(
                tRange.end.getGMTMonth(),
                tRange.end.getGMTDay(),
                tRange.end.getGMTYear(),
                tRange.end.getGMTHours(),
                tRange.end.getGMTMinutes(),
                tRange.end.getGMTSeconds(),
                tRange.end.getGMTMillis() / intIncrement * intIncrement);
        if (!time.equals(tRange.end)) time.increment(intIncrement, GeoDate.MSEC);
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
    return "MilliSecondAxis inc="
        + incrementValue__
        + " minorLabelInterval="
        + defaultMinorLabelInterval_;
  }
}
