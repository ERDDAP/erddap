/*
 * $Id: YearDecadeAxis.java,v 1.6 2001/03/22 20:06:59 dwd Exp $
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
 * Draws time axes using the year/decade style.
 *
 * <pre>
 * was
 *            |..........|..........|..........|..........|
 *                 84         85         86         87
 *                               1980
 *  Bob made it
 *            |         |         |         |
 *           1980      1981      1982      1983
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2001/03/22 20:06:59 $
 * @see Axis
 * @see TimeAxis
 */
public class YearDecadeAxis implements TimeAxisStyle {
  static final int DECADE_TEST__ = 1000;
  static final String defaultMinorLabelFormat__ = "yyy"; // was "yy"   Did bob change all of these?
  //  static final String defaultMajorLabelFormat__ = "yyyy";
  static final String defaultMajorLabelFormat__ = ""; // was "decade"
  int defaultMinorLabelInterval_ = 2;
  int defaultMajorLabelInterval_ = 1;
  static final int defaultNumSmallTics__ = 0;
  static final double incrementValue__ = 1.0;
  static final int incrementUnits__ = GeoDate.YEARS;

  /**
   * YearDecadeAxis constructor.
   *
   * @param id axis identifier
   */
  public YearDecadeAxis() {}

  @Override
  public void computeDefaults(GeoDate delta) {
    // bob changed this 2010-10-12
    long days = Math.abs(delta.getTime()) / GeoDate.MSECS_IN_DAY;
    if (days > 250000) {
      defaultMinorLabelInterval_ = 200;
    } else if (days > 125000) {
      defaultMinorLabelInterval_ = 100;
    } else if (days > 60000) {
      defaultMinorLabelInterval_ = 50;
    } else if (days > 25000) {
      defaultMinorLabelInterval_ = 20;
    } else if (days > 12500) {
      defaultMinorLabelInterval_ = 10;
    } else if (days > 6000) {
      defaultMinorLabelInterval_ = 5;
    } else if (days > 2500) {
      defaultMinorLabelInterval_ = 2;
    } else {
      defaultMinorLabelInterval_ = 1;
    }
    defaultMajorLabelInterval_ = 0; // never draw
  }

  @Override
  public double computeLocation(double prev, double now) {
    return prev; // (prev + now)*0.5;
  }

  @Override
  public int getMinorValue(GeoDate time) {
    return time.getGMTYear() - (time.getGMTYear() / 10) * 10; // was + 1;
  }

  @Override
  public int getMajorValue(GeoDate time) {
    return (time.getGMTYear() / 10) * 10;
  }

  @Override
  public boolean isRoomForMajorLabel(GeoDate delta) {
    return delta.getTime() / GeoDate.MSECS_IN_DAY > DECADE_TEST__;
  }

  @Override
  public boolean isStartOfMinor(GeoDate time) {
    return false; // i.e., never draw thick tick    was (time.getGMTYear() % 10) == 0;
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
      if (time_increasing) {
        time = new GeoDate(1, 1, tRange.start.getGMTYear(), 0, 0, 0, 0);
        if (time.getTime() < tRange.start.getTime()) time.increment(1.0, GeoDate.YEARS);
      } else {
        time = new GeoDate(1, 1, tRange.end.getGMTYear(), 0, 0, 0, 0);
        if (!time.equals(tRange.end)) time.increment(1.0, GeoDate.YEARS);
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
    return "YearDecadeAxis inc="
        + incrementValue__
        + " minorLabelInterval="
        + defaultMinorLabelInterval_;
  }
}
