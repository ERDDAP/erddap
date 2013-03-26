/*
 * $Id: MonthYearAxis.java,v 1.5 2000/12/19 01:00:38 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package  gov.noaa.pmel.sgt;

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.util.IllegalTimeValue;

/**
 * Draws time axes using the month/year style.
 *
 * <pre>
 *            |..........|..........|..........|..........|
 *                 mar         apr        may      jun
 *                               1980
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2000/12/19 01:00:38 $
 * @see Axis
 * @see TimeAxis
 */
public class MonthYearAxis implements TimeAxisStyle {
  static final int YEAR_TEST__ = 31;
  static final String defaultMinorLabelFormat__ = "MMM";
  static final String defaultMajorLabelFormat__ = "yyyy";
  static final int defaultNumSmallTics__ = 0;
  int defaultMinorLabelInterval_ = 2;
  int defaultMajorLabelInterval_ = 1;
  static final double incrementValue__ = 1.0;
  static final int incrementUnits__ = GeoDate.MONTHS;
  /**
   * MonthYearAxis constructor.
   *
   * @param id axis identifier
   **/
  public MonthYearAxis() {
  }
  public double computeLocation(double prev,double now) {
    return prev; //(prev + now)*0.5;
  }
  public void computeDefaults(GeoDate delta) {
    if(delta.getTime()/GeoDate.MSECS_IN_DAY > 240) {
      defaultMinorLabelInterval_ = 2;
    } else {
      defaultMinorLabelInterval_ = 1;
    }
  }
  public int getMinorValue(GeoDate time) {
    return time.getGMTMonth() + 1;  //was  not +1
  }
  public int getMajorValue(GeoDate time) {
    return time.getGMTYear();
  }
  public boolean isRoomForMajorLabel(GeoDate delta) {
    return delta.getTime()/GeoDate.MSECS_IN_DAY > YEAR_TEST__;
  }
  public boolean isStartOfMinor(GeoDate time) {
    return time.getGMTMonth() == 1;
  }
  public String getDefaultMinorLabelFormat() {
    return defaultMinorLabelFormat__;
  }
  public String getDefaultMajorLabelFormat() {
    return defaultMajorLabelFormat__;
  }
  public int getDefaultNumSmallTics() {
    return defaultNumSmallTics__;
  }
  public int getDefaultMinorLabelInterval() {
    return defaultMinorLabelInterval_;
  }
  public int getDefaultMajorLabelInterval() {
    return defaultMajorLabelInterval_;
  }
  public GeoDate getStartTime(TimeRange tRange) {
    boolean time_increasing;
    GeoDate time = null;
    time_increasing = tRange.end.after(tRange.start);
    try {
      if(time_increasing) {
        time = new GeoDate(tRange.start.getGMTMonth(), 1,
                           tRange.start.getGMTYear(), 0, 0, 0, 0);
        if(!time.equals(tRange.start)) time.increment(1.0, GeoDate.MONTHS);
      } else {
        time = new GeoDate(tRange.end.getGMTMonth(), 1,
                           tRange.end.getGMTYear(), 0, 0, 0, 0);
        if(!time.equals(tRange.end)) time.increment(1.0, GeoDate.MONTHS);
      }
    } catch (IllegalTimeValue e) {}
    return time;
  }
  public double getIncrementValue() {
    return incrementValue__;
  }
  public int getIncrementUnits() {
    return incrementUnits__;
  }
  public String toString() {
    return "MonthYearAxis inc=" + incrementValue__ + " minorLabelInterval=" + defaultMinorLabelInterval_;
  }
}
