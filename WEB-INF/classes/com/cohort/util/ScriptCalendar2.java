/* This file is Copyright (c) 2019 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 */
package com.cohort.util;

import java.util.TimeZone;

/**
 * This class makes some of the static methods in com.cohort.Calendar2 accessible to JexlScript
 * scripts as "Calendar2.<i>name</i>()" methods.
 *
 * <p>The underlying Calendar2 class is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in com/cohort/util/LICENSE.txt.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2019-11-20
 */
public class ScriptCalendar2 {

  /** These are the fields in a Calendar or GregorianCalendar object. */
  public static final int ERA = Calendar2.ERA;

  public static final int BC = Calendar2.BC;
  public static final int YEAR = Calendar2.YEAR; // BEWARE: use getYear() not gc.get(YEAR)

  /** Jan=0, ... */
  public static final int MONTH = Calendar2.MONTH;

  /** 1.. of month */
  public static final int DATE = Calendar2.DATE;

  public static final int DAY_OF_YEAR = Calendar2.DAY_OF_YEAR; // 1..

  /** 0..11, so rarely used */
  public static final int HOUR = Calendar2.HOUR;

  /** 0..23 */
  public static final int HOUR_OF_DAY = Calendar2.HOUR_OF_DAY;

  public static final int MINUTE = Calendar2.MINUTE;
  public static final int SECOND = Calendar2.SECOND;
  public static final int MILLISECOND = Calendar2.MILLISECOND;
  public static final int AM_PM = Calendar2.AM_PM;

  /** in millis */
  public static final int ZONE_OFFSET = Calendar2.ZONE_OFFSET;

  /** in millis */
  public static final int DST_OFFSET = Calendar2.DST_OFFSET;

  /**
   * This converts a string "[units] since [isoDate]" (e.g., "minutes since 1985-01-01") into a
   * baseSeconds (seconds since 1970-01-01) and a factor ("minutes" returns 60). <br>
   * So simplistically, epochSeconds = storedTime * factor + baseSeconds. <br>
   * Or simplistically, storedTime = (epochSeconds - baseSeconds) / factor.
   *
   * <p>WARNING: don't use the equations above. Use unitsSinceToEpochSeconds or
   * epochSecondsToUnitsSince which correctly handle special cases.
   *
   * @param tsUnits e.g., "minutes since 1985-01-01". This may include hours, minutes, seconds,
   *     decimal, and Z or timezone offset (default=Zulu). This is lenient.
   * @param timeZone Is a TimeZone from TimeZone.gettimeZone(id). For valid ID's, see the "TZ
   *     database names" column in the table at
   *     https://en.wikipedia.org/wiki/List_of_tz_database_time_zones If this is null, Zulu will be
   *     used.
   * @return double[]{baseSeconds, factorToGetSeconds}
   * @throws RuntimeException if trouble (tsUnits is null or invalid)
   */
  public static double[] getTimeBaseAndFactor(String tsUnits, TimeZone timeZone) {
    return Calendar2.getTimeBaseAndFactor(tsUnits, timeZone);
  }

  /**
   * This converts a unitsSince value into epochSeconds. This properly handles 'special'
   * factorToGetSeconds values (for month and year).
   *
   * @param baseSeconds from getTimeBaseAndFactor[0]
   * @param factorToGetSeconds from getTimeBaseAndFactor[1]
   * @param unitsSince a numeric time value in the source units
   * @return seconds since 1970-01-01 (or NaN if unitsSince is NaN)
   */
  public static double unitsSinceToEpochSeconds(
      double baseSeconds, double factorToGetSeconds, double unitsSince) {
    return Calendar2.unitsSinceToEpochSeconds(baseSeconds, factorToGetSeconds, unitsSince);
  }

  /**
   * This converts an epochSeconds value into a unitsSince value. This properly handles 'special'
   * factorToGetSeconds values (for month and year).
   *
   * @param baseSeconds from getTimeBaseAndFactor[0]
   * @param factorToGetSeconds from getTimeBaseAndFactor[1]
   * @param epochSeconds seconds since 1970-01-01 (or NaN if epochSeconds is NaN)
   * @return a numeric time value in source units "<i>units</i> since <i>baseTime</i>"
   */
  public static double epochSecondsToUnitsSince(
      double baseSeconds, double factorToGetSeconds, double epochSeconds) {
    return Calendar2.epochSecondsToUnitsSince(baseSeconds, factorToGetSeconds, epochSeconds);
  }

  /**
   * This converts a sourceTime string into a double with epochSeconds.
   *
   * @param sourceTime a formatted time string
   * @param dateTimeFormat See https://erddap.github.io/setupDatasetsXml.html#stringTimeUnits
   * @param timeZoneString For a list of valid timezone ID's, see the "TZ database names" column in
   *     the table at https://en.wikipedia.org/wiki/List_of_tz_database_time_zones . If this is null
   *     or "", Zulu will be used.
   * @return the epochSeconds value or NaN if trouble
   */
  public static double parseToEpochSeconds(
      String sourceTime, String dateTimeFormat, String timeZoneString) {
    return Calendar2.parseToEpochSeconds(sourceTime, dateTimeFormat, timeZoneString);
  }

  /**
   * A variant of parseToEpochSeconds that uses the Zulu time zone.
   *
   * @param sourceTime a formatted time string
   * @param dateTimeFormat See https://erddap.github.io/setupDatasetsXml.html#stringTimeUnits
   * @return the epochSeconds value or NaN if trouble
   */
  public static double parseToEpochSeconds(String sourceTime, String dateTimeFormat) {
    return Calendar2.parseToEpochSeconds(sourceTime, dateTimeFormat);
  }

  /**
   * This tries to figure out the format of someDateTimeString then parse the value and convert it
   * to epochSeconds.
   *
   * @param someDateTimeString a formatted time string
   * @return epochSeconds (or Double.NaN if trouble);
   */
  public static double tryToEpochSeconds(String someDateTimeString) {
    return Calendar2.tryToEpochSeconds(someDateTimeString);
  }

  /**
   * This tries to figure out the format of someDateTimeString then parse the value and convert to
   * an ISO 8601 string with 'Z' at end. This is the most flexible approach to parsing/cleaning a
   * weird date time string.
   *
   * @param someDateTimeString
   * @return an iso8601String as a date, a dateTime with T and Z, or "" if trouble;
   */
  public static String tryToIsoString(String someDateTimeString) {
    return Calendar2.tryToIsoString(someDateTimeString);
  }

  /**
   * This formats the epochSeconds time value using the pattern.
   *
   * @param epochSeconds
   * @param pattern see
   *     https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   *     If pattern is null or "", this uses the ISO TZ format with seconds precision.
   * @param zone if null or "", Zulu is used
   * @return the formatted time string (or "" if trouble)
   */
  public static String format(double epochSeconds, String pattern, String zone) {
    return Calendar2.format(epochSeconds, pattern, zone);
  }

  /**
   * This is like safeEpochSecondsToIsoStringT3Z, but returns a limited precision string. This won't
   * throw an exception.
   *
   * @param time_precision can be "1970", "1970-01", "1970-01-01", "1970-01-01T00Z",
   *     "1970-01-01T00:00Z", "1970-01-01T00:00:00Z" (used if time_precision not matched),
   *     "1970-01-01T00:00:00.0Z", "1970-01-01T00:00:00.00Z", "1970-01-01T00:00:00.000Z". Or any of
   *     those without "Z". If time_precision ends in Z, the result will too. If time_precision
   *     doesn't end in Z, the result won't end in Z. Note that ERDDAP requires/forces/ensures any
   *     format with hours(min(sec)) to have Z.
   * @param seconds the epochSeconds value
   * @param NaNString the value to return if seconds is not finite or is too big.
   * @return the formatted time string (or NaNString if trouble)
   */
  public static String epochSecondsToLimitedIsoStringT(
      String time_precision, double seconds, String NaNString) {
    return Calendar2.epochSecondsToLimitedIsoStringT(time_precision, seconds, NaNString);
  }

  /**
   * This clears the fields smaller than 'field' (e.g., HOUR_OF_DAY clears MINUTE, SECOND, and
   * MILLISECOND, but doesn't change HOUR_OF_DAY, MONTH, or YEAR).
   *
   * @param epochSeconds
   * @param field e.g., HOUR_OF_DAY
   * @return the new epochSeconds value (or NaN if trouble).
   */
  public static double clearSmallerFields(double epochSeconds, int field) {
    return Calendar2.clearSmallerFields(epochSeconds, field);
  }
}
