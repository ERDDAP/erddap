/*
 * $Id: DayMonthAxis.java,v 1.5 2000/12/19 01:00:38 dwd Exp $
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
import gov.noaa.pmel.util.IllegalTimeValue;
import gov.noaa.pmel.util.TimeRange;

/**
 * Draws time axes using the day/month style.
 *
 * <pre>
 *        |...........|...........|...........|...........|
 *             3           4           5           6
 *                             jun 93
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2000/12/19 01:00:38 $
 * @see Axis
 * @see TimeAxis
 */
public class DayMonthAxis implements TimeAxisStyle {
  static final int MONTH_TEST__ = 3;
  static final String defaultMinorLabelFormat__ = "dd";
  static final String defaultMajorLabelFormat__ = "yyyy-MM";
  static final int defaultNumSmallTics__ = 0;
  int defaultMinorLabelInterval_ = 2;
  int defaultMajorLabelInterval_ = 1;
  static final double incrementValue__ = 1.0;
  static final int incrementUnits__ = GeoDate.DAYS;
  /**
   * DayMonthAxis constructor.
   */
  public DayMonthAxis() {
  }
  public double computeLocation(double prev,double now) {
    return prev; //(prev + now)*0.5;
  }
  public void computeDefaults(GeoDate delta) {
    long days = delta.getTime()/GeoDate.MSECS_IN_DAY;
    if(days > 30) {
      defaultMinorLabelInterval_ = 5;
    } else if(days > 10) {
      defaultMinorLabelInterval_ = 2;
    } else {
      defaultMinorLabelInterval_ = 1;
    }
  }
  public int getMinorValue(GeoDate time) {
    return time.getGMTDay();
  }
  public int getMajorValue(GeoDate time) {
    return time.getGMTMonth();
  }
  public boolean isRoomForMajorLabel(GeoDate delta) {
    return delta.getTime()/GeoDate.MSECS_IN_DAY > MONTH_TEST__;
  }
  public boolean isStartOfMinor(GeoDate time) {
    return time.getGMTDay() == 1;
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
        time = new GeoDate(tRange.start.getGMTMonth(),
         tRange.start.getGMTDay(),
                           tRange.start.getGMTYear(), 0, 0, 0, 0);
        if(!time.equals(tRange.start)) time.increment(1.0, GeoDate.DAYS);
      } else {
        time = new GeoDate(tRange.end.getGMTMonth(),
         tRange.end.getGMTDay(),
                           tRange.end.getGMTYear(), 0, 0, 0, 0);
        if(!time.equals(tRange.end)) time.increment(1.0, GeoDate.DAYS);
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
    return "DayMonthAxis inc=" + incrementValue__ + " minorLabelInterval=" + defaultMinorLabelInterval_;
  }
}
