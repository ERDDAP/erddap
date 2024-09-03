/*
 * 2013-02-27 DecadeAxis.java created by Bob Simons, based on YearDecadeAxis
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
 * Draws time axes using the decade style.
 *
 * <pre>
 *            |         |         |         |
 *           1980      1990      2000      2010
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2001/03/22 20:06:59 $
 * @see Axis
 * @see TimeAxis
 */
public class DecadeAxis implements TimeAxisStyle {
  static final int DECADE_TEST__ = 1000;
  static final String defaultMinorLabelFormat__ = "yyy"; // was "yy"
  //  static final String defaultMajorLabelFormat__ = "yyyy";
  static final String defaultMajorLabelFormat__ = ""; // was "decade"
  int defaultMinorLabelInterval_ = 2;
  int defaultMajorLabelInterval_ = 1;
  static final int defaultNumSmallTics__ = 0;
  static final double incrementValue__ = 10.0;
  static final int incrementUnits__ = GeoDate.YEARS;

  /**
   * DecadeAxis constructor.
   *
   * @param id axis identifier
   */
  public DecadeAxis() {}

  @Override
  public void computeDefaults(GeoDate delta) {
    // bob changed this 2013-02-27
    long years = Math.abs(delta.getTime()) / (365 * GeoDate.MSECS_IN_DAY);
    if (years > 800) {
      defaultMinorLabelInterval_ = 200;
    } else if (years > 400) {
      defaultMinorLabelInterval_ = 100;
    } else if (years > 200) {
      defaultMinorLabelInterval_ = 50;
    } else if (years > 80) {
      defaultMinorLabelInterval_ = 20;
    } else {
      defaultMinorLabelInterval_ = 10;
    }
    defaultMajorLabelInterval_ = 0; // never draw
  }

  @Override
  public double computeLocation(double prev, double now) {
    return prev; // (prev + now)*0.5;
  }

  @Override
  public int getMinorValue(GeoDate time) {
    return time.getGMTYear();
  }

  @Override
  public int getMajorValue(GeoDate time) {
    return time.getGMTYear();
  }

  @Override
  public boolean isRoomForMajorLabel(GeoDate delta) {
    return false; // delta.getTime()/GeoDate.MSECS_IN_DAY > DECADE_TEST__;
  }

  @Override
  public boolean isStartOfMinor(GeoDate time) {
    return false; // i.e., never draw thick tick    was (time.getGMTYear() %
    // defaultMinorLabelInterval_) == 0;
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
  public int getDefaultMajorLabelInterval() {
    return defaultMajorLabelInterval_;
  }

  @Override
  public int getDefaultMinorLabelInterval() {
    return defaultMinorLabelInterval_;
  }

  @Override
  public GeoDate getStartTime(TimeRange tRange) {
    boolean time_increasing;
    GeoDate time = null;
    time_increasing = tRange.end.after(tRange.start);
    try {
      if (time_increasing) { // max(10,) because GMT year doesn't like year 0
        time = new GeoDate(1, 1, Math.max(10, tRange.start.getGMTYear() / 10 * 10), 0, 0, 0, 0);
        if (time.getTime() < tRange.start.getTime())
          time.increment(incrementValue__, GeoDate.YEARS);
      } else {
        time = new GeoDate(1, 1, Math.max(10, tRange.end.getGMTYear() / 10 * 10), 0, 0, 0, 0);
        if (!time.equals(tRange.end)) time.increment(incrementValue__, GeoDate.YEARS);
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
    return "DecadeAxis inc="
        + incrementValue__
        + " minorLabelInterval="
        + defaultMinorLabelInterval_;
  }
}
