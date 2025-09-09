/*
 * TimePeriods Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.google.common.collect.ImmutableList;
import java.time.ZonedDateTime;

/**
 * This class holds the master TimePeriods.OPTIONS list and related information and methods.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-02-28
 */
public class TimePeriods {

  public static final String _25HOUR_OPTION = "25 hour";
  public static final String _33HOUR_OPTION = "33 hour";
  public static final String MONTHLY_OPTION = "monthly";

  /**
   * All possible time period OPTIONS used within CWBrowser, in order of duration. Note that other
   * options may be added in the future.
   */
  public static final ImmutableList<String> OPTIONS =
      ImmutableList.of(
          // If these change, make similar changes in closestTimePeriod below.
          "pass",
          "pass",
          "1 observation",
          "hourly",
          "1 day",
          _25HOUR_OPTION,
          _33HOUR_OPTION,
          "3 day",
          "4 day",
          "5 day",
          "7 day",
          "8 day",
          "10 day",
          "14 day",
          "1 month",
          MONTHLY_OPTION,
          "3 month",
          "1 year",
          "5 year",
          "10 year",
          "20 year",
          "all");

  /** The OPTIONs index for "1 day". */
  public static final int _1DAY_INDEX = 4;

  /** The OPTIONs index for "25 hour". */
  public static final int _25HOUR_INDEX = 5;

  /** The OPTIONs index for "33 hour". */
  public static final int _33HOUR_INDEX = 6;

  /**
   * All possible time period OPTIONS used within CWBrowser-style file names, in order of duration.
   * Note that other options may be added in the future.
   */
  public static final ImmutableList<String> IN_FILE_NAMES =
      ImmutableList.of(
          // If these change, make similar changes in closestTimePeriod below.
          "pass",
          "hday",
          "1obs",
          "hourly",
          "1day",
          "25hour",
          "33hour",
          "3day",
          "4day",
          "5day",
          "7day",
          "8day",
          "10day",
          "14day",
          "mday",
          "mday",
          "3month",
          "1year",
          "5year",
          "10year",
          "20year",
          "all");

  /** The value of N_HOURS for timePeriod = "all". */
  public static final int N_HOURS_ALL = Integer.MAX_VALUE;

  public static final int N_HOURS_MONTHLY = 30 * 24;

  /**
   * The nominal number of hours related to an OPTIONS. Since the number of hours in a time period
   * varies (e.g., Nov vs Dec), these are fixed numbers which represent idealized values e.g.,
   * 'month' options are represented as multiples of 30*24 hours, and 'year' options are represented
   * as multiples of 365*24 hours. "all" time is represented is Integer.MAX_VALUE.
   */
  public static final ImmutableList<Integer> N_HOURS =
      ImmutableList.of(
          0,
          0,
          0,
          0,
          24,
          25,
          33,
          3 * 24,
          4 * 24,
          5 * 24,
          7 * 24,
          8 * 24,
          10 * 24,
          14 * 24,
          N_HOURS_MONTHLY,
          30 * 24,
          90 * 24,
          365 * 24,
          5 * 365 * 24,
          10 * 365 * 24,
          20 * 365 * 24,
          N_HOURS_ALL);

  /**
   * Find the index of a time period option in OPTIONS.
   *
   * @param option usually one of the OPTIONS.
   * @return the index in OPTIONS for value, or -1 if not found.
   */
  public static int exactTimePeriod(String option) {
    return OPTIONS.indexOf(option);
  }

  /**
   * Given an option from OPTIONS, this returns the corresponding N_HOURS.
   *
   * @param option an option from OPTIONS
   * @return the corresponding N_HOURS
   * @throws exception if option not found in OPTIONS
   */
  public static int getNHours(String option) {
    int index = exactTimePeriod(option);
    Test.ensureNotEqual(
        index,
        -1,
        String2.ERROR + " in TimePeriods.getNHours: '" + option + "' not in TimePeriods.OPTIONS.");
    return N_HOURS.get(index);
  }

  /**
   * Find the closest time period in a list.
   *
   * @param value The desired value, which should be in OPTIONS, but doesn't have to be in 'list'.
   * @param list is a String[] with the valid options, all of which must be in options.
   * @return index in list for the closest time period. If value is null, "" or not in the list,
   *     this returns the index of the option in list closest to "1 day".
   * @throws Exception if trouble
   */
  public static int closestTimePeriod(String value, ImmutableList<String> list) {
    // make sure it is one of standard timePeriods
    int valueIndex = exactTimePeriod(value);
    if (valueIndex < 0) valueIndex = _1DAY_INDEX;
    value = OPTIONS.get(valueIndex);
    int valueHours = N_HOURS.get(valueIndex);

    // find exact
    int index = list.indexOf(value);
    if (index >= 0) return index;

    // find closest
    int diff = Integer.MAX_VALUE;
    index = -1;
    for (int i = 0; i < list.size(); i++) {
      int tHours = getNHours(list.get(i));
      if (tHours == N_HOURS_ALL) tHours = 30 * 365 * 24;
      int tDiff = Math.abs(tHours - valueHours);
      // String2.log(i + " TimePeriods.closestTimePeriod value=" + value +
      //    " tHours=" + tHours + " tDiff=" + tDiff + " index=" + index);
      if (tDiff < diff) {
        index = i;
        diff = tDiff;
      }
    }
    return index;
  }

  /**
   * Find the closest time period in a list.
   *
   * @param nHours
   * @param list is a String[] with the valid options, all of which must be in OPTIONS.
   * @return index in list for the closest time period. If value is NaN, this returns the index of
   *     the option in list closest to "1 day".
   */
  public static int closestTimePeriod(int nHours, ImmutableList<String> list) {
    if (nHours == Integer.MAX_VALUE) return closestTimePeriod(OPTIONS.get(_1DAY_INDEX), list);

    // find closest
    int diff = Integer.MAX_VALUE;
    int closestI = -1;
    for (int i = 0; i < list.size(); i++) {
      int tNHours = getNHours(list.get(i));
      if (tNHours == N_HOURS_ALL) tNHours = 30 * 365 * 24;
      int tDiff = Math.abs(nHours - tNHours);
      // String2.log(i + " TimePeriods.closestTimePeriod tNHours=" + tNHours +
      //    " nHours=" + nHours + " tDiff=" + tDiff + " index=" + index);
      if (tDiff < diff) {
        closestI = i;
        diff = tDiff;
      }
    }

    return closestI;
  }

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
