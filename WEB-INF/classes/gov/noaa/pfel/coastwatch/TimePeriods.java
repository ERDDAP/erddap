/*
 * TimePeriods Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.util.Calendar2;
import com.cohort.util.Test;
import java.time.ZonedDateTime;

/**
 * This class holds the master TimePeriods.OPTIONS list and related information and methods.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-02-28
 */
public class TimePeriods {

  /**
   * This converts an End Calendar into a centered time (centered to the nearest second).
   *
   * <p>WARNING: Old-style (pre Dec 2006) 25 and 33 hour files were stored as e.g., 00:00 (Dave says
   * that is correct to nearest second), so use before calling this:
   *
   * <pre>
   * //fix old-style 25 and 33 hour end times  so 59:59
   * if (timePeriodNHours &gt; 1 &amp;&amp; timePeriodNHours % 24 != 0)
   * cal.add(Calendar2.SECOND, -1);
   * </pre>
   *
   * @param timePeriodNHours one of the TimePeriod.NHOURS options
   * @param cal initially, the end time ZonedDateTime usually from getEndCalendar. This will be
   *     modified so that it holds the centered time when finished.
   * @param errorInMethod the start of the error message
   * @throws Exception if trouble
   */
  public static ZonedDateTime endCalendarToCenteredTime(
      int timePeriodNHours, ZonedDateTime cal, String errorInMethod) throws Exception {
    if (timePeriodNHours <= 1) {
      // Dave says: do nothing
    } else if (timePeriodNHours % 24 != 0) {
      // covers 25, 33 hours
      if (cal.getMinute() != 0 || cal.getSecond() != 0) {
        Test.error(
            errorInMethod
                + "timePeriodNHours="
                + timePeriodNHours
                + " Time="
                + Calendar2.formatAsISODateTimeT(cal)
                + " doesn't end in 00:00.");
      }
      cal = cal.plusMinutes(-timePeriodNHours * 60 / 2);
    } else if (timePeriodNHours < 30 * 24) {
      // nDays
      if (cal.getHour() != 0 || cal.getMinute() != 0 || cal.getSecond() != 0) {
        Test.error(
            errorInMethod
                + "timePeriodNHours="
                + timePeriodNHours
                + " Time="
                + Calendar2.formatAsISODateTimeT(cal)
                + " doesn't end in 00:00:00.");
      }
      cal = cal.plusHours(-timePeriodNHours / 2);
    } else if (timePeriodNHours == 30 * 24) {
      // 1 month
      if (cal.getHour() != 0 || cal.getMinute() != 0 || cal.getSecond() != 0) {
        Test.error(
            errorInMethod
                + "timePeriodNHours="
                + timePeriodNHours
                + "Time="
                + Calendar2.formatAsISODateTimeT(cal)
                + " doesn't end in 00:00:00.");
      }
      cal = cal.plusSeconds(-1); // -1 changes to 23:59:59 on last day of previous month
      cal = Calendar2.centerOfMonth(cal);
    } else {
      // there are longer time periods, but no data sets used them
      Test.error(errorInMethod + "Unexpected timePeriodNHours=" + timePeriodNHours);
    }
    return cal;
  }
}
