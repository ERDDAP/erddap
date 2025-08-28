/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * This class has static methods for dealing with dates and times.
 *
 * <p>A summary of ISO 8601 Date Time formats is at http://www.cl.cam.ac.uk/~mgk25/iso-time.html
 * https://en.wikipedia.org/wiki/ISO_8601 and http://dotat.at/tmp/ISO_8601-2004_E.pdf (was
 * https://www.iso.org/iso/date_and_time_format) and years B.C at
 * http://www.tondering.dk/claus/cal/node4.html#SECTION00450000000000000000
 *
 * <p>Calendar2 does not use ERA designations. It uses negative year values for B.C years
 * (calendar2Year = 1 - BCYear). Note that BCYears are 1..., so 1 BC is calendar2Year 0 (or 0000),
 * and 2 BC is calendar2Year -1 (or -0001). Thus, use getYear(gc) instead gc.get(YEAR).
 */
public class Calendar2 {

  // useful static variables
  public static final int ERA = Calendar.ERA;
  public static final int BC = GregorianCalendar.BC;
  public static final int YEAR = Calendar.YEAR; // BEWARE: use getYear() not gc.get(YEAR)
  public static final int MONTH = Calendar.MONTH; // java counts 0..
  public static final int DATE = Calendar.DATE; // 1..  of month
  public static final int DAY_OF_YEAR = Calendar.DAY_OF_YEAR; // 1..
  public static final int HOUR = Calendar.HOUR; // 0..11     //rarely used
  public static final int HOUR_OF_DAY = Calendar.HOUR_OF_DAY; // 0..23
  public static final int MINUTE = Calendar.MINUTE;
  public static final int SECOND = Calendar.SECOND;
  public static final int MILLISECOND = Calendar.MILLISECOND;
  public static final int AM_PM = Calendar.AM_PM;
  public static final int ZONE_OFFSET = Calendar.ZONE_OFFSET; // millis
  public static final int DST_OFFSET = Calendar.DST_OFFSET; // millis

  public static final int MINUTES_PER_DAY = 1440;
  public static final int MINUTES_PER_7DAYS = 7 * MINUTES_PER_DAY; // 10080
  public static final int MINUTES_PER_30DAYS = 30 * MINUTES_PER_DAY; // 43200
  public static final int SECONDS_PER_MINUTE = 60;
  public static final int SECONDS_PER_HOUR = 60 * 60; // 3600
  public static final int SECONDS_PER_DAY =
      24 * 60 * 60; // 86400   31Days=2678400  365days=31536000
  public static final long MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * 1000L;
  public static final long MILLIS_PER_HOUR = SECONDS_PER_HOUR * 1000L;
  public static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;

  public static final String SECONDS_SINCE_1970 = "seconds since 1970-01-01T00:00:00Z";
  public static final String MILLISECONDS_SINCE_1970 = "milliseconds since 1970-01-01T00:00:00Z";

  public static final String zulu = "Zulu";
  public static final TimeZone zuluTimeZone = TimeZone.getTimeZone(zulu);
  public static final ZoneId zuluZoneId = ZoneId.of(zulu);

  private static final ImmutableList<String> MONTH_3 =
      ImmutableList.of(
          "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
  private static final ImmutableList<String> MONTH_FULL =
      ImmutableList.of(
          "January",
          "February",
          "March",
          "April",
          "May",
          "June",
          "July",
          "August",
          "September",
          "October",
          "November",
          "December");
  private static final ImmutableList<String> DAY_OF_WEEK_3 =
      ImmutableList.of( // corresponding to DAY_OF_WEEK values
          "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
  private static final ImmutableList<String> DAY_OF_WEEK_FULL =
      ImmutableList.of(
          "",
          "Sunday",
          "Monday",
          "Tuesday",
          "Wednesday",
          "Thursday",
          "Friday",
          "Saturday",
          "Sunday");

  /** special Formats for ISO date time without a suffix (assumed to be UTC) */
  // use of yyyy isn't best. But converted to uuuu below.
  public static final String ISO8601DATE_FORMAT = "yyyy-MM-dd";

  /** special case format supports suffix 'Z' or +/-HH:MM. For format() use 'Z' to get 'Z'. */
  public static final String ISO8601TZ_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  public static final String ISO8601T3Z_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  public static final String ISO8601T9Z_FORMAT =
      "yyyy-MM-dd'T'HH:mm:ss.SSS000000Z"; // WARNING: only useful if S digits 4-9 are '0'. gc
  // doesn't support nanoseconds.
  public static final DateTimeFormatter ISO_OFFSET_LOCAL_FORMATTER =
      // since this is formatter (not parser), bypass stuff in makeDateTimeFormatter
      DateTimeFormatter.ofPattern(
          "uuuu-MM-dd'T'HH:mm:ssxxxxx"); // offset always ISO formatted e.g., -07:00

  public static final String RFC822_GMT_REGEX =
      "[a-zA-Z]{3}, [0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] GMT";
  public static final String RFC822_GMT_FORMAT = "EEE, d MMM yyyy HH:mm:ss 'GMT'";

  // month can be e.g., 9 | 19 | [01]\\D      //avoid uuuu-DDD
  public static final Pattern ISO_DATE_PATTERN = Pattern.compile("-?\\d{4}-[01]?\\d(|\\-\\d.*)");
  public static final Pattern NUMERIC_TIME_PATTERN =
      Pattern.compile(" *[a-z]+ +since +.*[0-9].*"); // lenient. Test already lowercase string.

  /** This is used to catch e.g. time/1day for cell_methods for some orderBy TableWriters. */
  public static final Pattern TIME_N_UNITS_PATTERN = Pattern.compile("time */ *(\\d+) *([a-z]+)");

  private static DateTimeFormatter FORMAT_YEAR = DateTimeFormatter.ofPattern("uuuu");
  private static DateTimeFormatter FORMAT_MONTH = DateTimeFormatter.ofPattern("uuuu-MM");
  private static DateTimeFormatter FORMAT_DAY = DateTimeFormatter.ofPattern("uuuu-MM-dd");
  private static DateTimeFormatter FORMAT_HOUR = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH");
  private static DateTimeFormatter FORMAT_MINUTE =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm");
  private static DateTimeFormatter FORMAT_SECOND =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
  private static DateTimeFormatter FORMAT_MILLISECOND =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.S");
  private static DateTimeFormatter FORMAT_MILLISECOND2 =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SS");
  private static DateTimeFormatter FORMAT_MILLISECOND3 =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS");
  private static DateTimeFormatter FORMAT_MILLISECOND4 =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSS");
  private static DateTimeFormatter FORMAT_MILLISECOND5 =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSS");
  private static DateTimeFormatter FORMAT_MILLISECOND6 =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS");
  private static DateTimeFormatter FORMAT_MILLISECOND7 =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSS");
  private static DateTimeFormatter FORMAT_MILLISECOND8 =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSS");
  private static DateTimeFormatter FORMAT_MILLISECOND9 =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS");
  private static DateTimeFormatter FORMAT_YEARZ = DateTimeFormatter.ofPattern("uuuu'Z'");
  private static DateTimeFormatter FORMAT_MONTHZ = DateTimeFormatter.ofPattern("uuuu-MM'Z'");
  private static DateTimeFormatter FORMAT_DAYZ = DateTimeFormatter.ofPattern("uuuu-MM-dd'Z'");
  private static DateTimeFormatter FORMAT_HOURZ = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH'Z'");
  private static DateTimeFormatter FORMAT_MINUTEZ =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm'Z'");
  private static DateTimeFormatter FORMAT_SECONDZ =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'");
  private static DateTimeFormatter FORMAT_MILLISECONDZ =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.S'Z'");
  private static DateTimeFormatter FORMAT_MILLISECOND2Z =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SS'Z'");
  public static DateTimeFormatter FORMAT_MILLISECOND3Z =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'");
  private static DateTimeFormatter FORMAT_MILLISECOND4Z =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSS'Z'");
  private static DateTimeFormatter FORMAT_MILLISECOND5Z =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSS'Z'");
  private static DateTimeFormatter FORMAT_MILLISECOND6Z =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
  private static DateTimeFormatter FORMAT_MILLISECOND7Z =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSS'Z'");
  private static DateTimeFormatter FORMAT_MILLISECOND8Z =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSS'Z'");
  private static DateTimeFormatter FORMAT_MILLISECOND9Z =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'");
  public static DateTimeFormatter FORMAT_ISODate_TIME_SPACE =
      DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
  private static DateTimeFormatter FORMAT_COMPACT_DATE_TIME =
      DateTimeFormatter.ofPattern("uuuuMMddHHmmss");

  /**
   * This has alternating regex/timeFormat for formats where the first char is a digit. This is used
   * by suggestDateTimeFormat. This makes a huge number of formats because they may be used for
   * number to String as well as the usual String to number. See JavaDocs for
   */
  public static final ImmutableList<String> digitRegexTimeFormat =
      ImmutableList.of(
          // There are many cases where 2 formats will match (e.g., with d versus dd).
          // So suggestDateTimeFormat(StringArray) works correctly,
          // always put the more specific version (e.g., dd) before the more general (d).

          // * Compact (number-only) formats only support years 0000 - 4999.
          //  That makes it likely that numbers won't be interpreted as compact date times.

          // Note + inside [] is a literal, not "1 or more instances of preceding item"

          // These use yyyy (year in era), not uuuu (astronomical year) as they should.
          // But in practice, all the parsing and formatting converts yyyy to uuuu.

          // yyyy-DDD   check for day-of-year (3 digit date) before ISO 8601 format.
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9} UTC", // in
              // practice, I
              // have only
              // seen " UTC"
              "yyyy-DDD'T'HH:mm:ss,SSS000000 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9} GMT", // GMT is
              // rarely used
              "yyyy-DDD'T'HH:mm:ss,SSS000000 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9}UTC",
              "yyyy-DDD'T'HH:mm:ss,SSS000000'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9}GMT",
              "yyyy-DDD'T'HH:mm:ss,SSS000000'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9}[ +\\-][0-9]{1,2}:[0-9]{2}", // timezone {1} is non-standard
              "yyyy-DDD'T'HH:mm:ss,SSS000000xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9}[ +\\-][0-9]{3,4}", // timezone 3 (hmm) is non-standard
              "yyyy-DDD'T'HH:mm:ss,SSS000000xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9}[ +\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss,SSS000000x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9}Z",
              "yyyy-DDD'T'HH:mm:ss,SSS000000'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{7,9}",
              "yyyy-DDD'T'HH:mm:ss,SSS000000",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9} UTC",
              "yyyy-DDD'T'HH:mm:ss.SSS000000 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9} GMT",
              "yyyy-DDD'T'HH:mm:ss.SSS000000 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS000000 xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9} [+\\-][0-9]{3,4}",
              "yyyy-DDD'T'HH:mm:ss.SSS000000 xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9} [+\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS000000 x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9}UTC",
              "yyyy-DDD'T'HH:mm:ss.SSS000000'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9}GMT",
              "yyyy-DDD'T'HH:mm:ss.SSS000000'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS000000xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9}[ +\\-][0-9]{3,4}",
              "yyyy-DDD'T'HH:mm:ss.SSS000000xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9}[ +\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS000000x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9}Z",
              "yyyy-DDD'T'HH:mm:ss.SSS000000'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{7,9}",
              "yyyy-DDD'T'HH:mm:ss.SSS000000",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6} UTC", // in
              // practice, I
              // have only
              // seen " UTC"
              "yyyy-DDD'T'HH:mm:ss,SSS000 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6} GMT", // GMT is
              // rarely used
              "yyyy-DDD'T'HH:mm:ss,SSS000 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6}UTC",
              "yyyy-DDD'T'HH:mm:ss,SSS000'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6}GMT",
              "yyyy-DDD'T'HH:mm:ss,SSS000'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6}[ +\\-][0-9]{1,2}:[0-9]{2}", // timezone {1} is non-standard
              "yyyy-DDD'T'HH:mm:ss,SSS000xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6}[ +\\-][0-9]{3,4}", // timezone 3 (hmm) is non-standard
              "yyyy-DDD'T'HH:mm:ss,SSS000xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6}[ +\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss,SSS000x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6}Z",
              "yyyy-DDD'T'HH:mm:ss,SSS000'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{4,6}",
              "yyyy-DDD'T'HH:mm:ss,SSS000",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6} UTC",
              "yyyy-DDD'T'HH:mm:ss.SSS000 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6} GMT",
              "yyyy-DDD'T'HH:mm:ss.SSS000 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS000 xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6} [+\\-][0-9]{3,4}",
              "yyyy-DDD'T'HH:mm:ss.SSS000 xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6} [+\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS000 x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6}UTC",
              "yyyy-DDD'T'HH:mm:ss.SSS000'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6}GMT",
              "yyyy-DDD'T'HH:mm:ss.SSS000'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS000xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6}[ +\\-][0-9]{3,4}",
              "yyyy-DDD'T'HH:mm:ss.SSS000xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6}[ +\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS000x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6}Z",
              "yyyy-DDD'T'HH:mm:ss.SSS000'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{4,6}",
              "yyyy-DDD'T'HH:mm:ss.SSS000",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3} UTC", // in
              // practice, I
              // have only
              // seen " UTC"
              "yyyy-DDD'T'HH:mm:ss,SSS 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3} GMT", // GMT is
              // rarely used
              "yyyy-DDD'T'HH:mm:ss,SSS 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3}UTC",
              "yyyy-DDD'T'HH:mm:ss,SSS'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3}GMT",
              "yyyy-DDD'T'HH:mm:ss,SSS'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}", // timezone {1} is non-standard
              "yyyy-DDD'T'HH:mm:ss,SSSxxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3}[ +\\-][0-9]{3,4}", // timezone 3 (hmm) is non-standard
              "yyyy-DDD'T'HH:mm:ss,SSSxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss,SSSx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3}Z",
              "yyyy-DDD'T'HH:mm:ss,SSS'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{1,3}",
              "yyyy-DDD'T'HH:mm:ss,SSS",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3} UTC",
              "yyyy-DDD'T'HH:mm:ss.SSS 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3} GMT",
              "yyyy-DDD'T'HH:mm:ss.SSS 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3} [+\\-][0-9]{3,4}",
              "yyyy-DDD'T'HH:mm:ss.SSS xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3} [+\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSS x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}UTC",
              "yyyy-DDD'T'HH:mm:ss.SSS'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}GMT",
              "yyyy-DDD'T'HH:mm:ss.SSS'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSSxxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-DDD'T'HH:mm:ss.SSSxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss.SSSx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}Z",
              "yyyy-DDD'T'HH:mm:ss.SSS'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{1,3}",
              "yyyy-DDD'T'HH:mm:ss.SSS",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] UTC",
              "yyyy-DDD'T'HH:mm:ss 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] GMT",
              "yyyy-DDD'T'HH:mm:ss 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{3,4}",
              "yyyy-DDD'T'HH:mm:ss xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ss x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]UTC",
              "yyyy-DDD'T'HH:mm:ss'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]GMT",
              "yyyy-DDD'T'HH:mm:ss'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm:ssxxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-DDD'T'HH:mm:ssxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-DDD'T'HH:mm:ssx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z", "yyyy-DDD'T'HH:mm:ss'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]", "yyyy-DDD'T'HH:mm:ss",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9] UTC", "yyyy-DDD'T'HH:mm 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9] GMT", "yyyy-DDD'T'HH:mm 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9] [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mm xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9] [+\\-][0-9]{3,4}", "yyyy-DDD'T'HH:mm xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9] [+\\-][0-9]{2}", "yyyy-DDD'T'HH:mm x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]UTC", "yyyy-DDD'T'HH:mm'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]GMT", "yyyy-DDD'T'HH:mm'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-DDD'T'HH:mmxxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}", "yyyy-DDD'T'HH:mmxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{2}", "yyyy-DDD'T'HH:mmx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]Z", "yyyy-DDD'T'HH:mm'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]:[0-5][0-9]", "yyyy-DDD'T'HH:mm",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9] UTC", "yyyy-DDD'T'HH 'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9] GMT", "yyyy-DDD'T'HH 'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9] [+\\-][0-9]{1,2}:[0-9]{2}", "yyyy-DDD'T'HH xxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9] [+\\-][0-9]{3,4}", "yyyy-DDD'T'HH xx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9] [+\\-][0-9]{2}", "yyyy-DDD'T'HH x",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]UTC", "yyyy-DDD'T'HH'UTC'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]GMT", "yyyy-DDD'T'HH'GMT'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9][ +\\-][0-9]{1,2}:[0-9]{2}", "yyyy-DDD'T'HHxxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9][ +\\-][0-9]{3,4}", "yyyy-DDD'T'HHxx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9][ +\\-][0-9]{2}", "yyyy-DDD'T'HHx",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]Z", "yyyy-DDD'T'HH'Z'",
          "-?[0-9]{4}-[0-3][0-9]{2}T[0-2][0-9]", "yyyy-DDD'T'HH",
          "-?[0-9]{4}-[0-3][0-9]{2}", "yyyy-DDD",

          // yyyyDDD   check for day-of-year (3 digit date) before ISO 8601 format
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9] UTC", "yyyyDDD'T'HHmmss 'UTC'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9] GMT", "yyyyDDD'T'HHmmss 'GMT'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9]UTC", "yyyyDDD'T'HHmmss'UTC'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9]GMT", "yyyyDDD'T'HHmmss'GMT'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyyDDD'T'HHmmssxxx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyyDDD'T'HHmmssxx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{2}",
              "yyyyDDD'T'HHmmssx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9]Z", "yyyyDDD'T'HHmmss'Z'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][0-5][0-9]", "yyyyDDD'T'HHmmss",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9] UTC", "yyyyDDD'T'HHmm 'UTC'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9] GMT", "yyyyDDD'T'HHmm 'GMT'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9]UTC", "yyyyDDD'T'HHmm'UTC'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9]GMT", "yyyyDDD'T'HHmm'GMT'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyyDDD'T'HHmmxxx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][ +\\-][0-9]{3,4}", "yyyyDDD'T'HHmmxx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9][ +\\-][0-9]{2}", "yyyyDDD'T'HHmmx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9]Z", "yyyyDDD'T'HHmm'Z'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][0-5][0-9]", "yyyyDDD'T'HHmm",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9] UTC", "yyyyDDD'T'HH 'UTC'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9] GMT", "yyyyDDD'T'HH 'GMT'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9]UTC", "yyyyDDD'T'HH'UTC'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9]GMT", "yyyyDDD'T'HH'GMT'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][ +\\-][0-9]{1,2}:[0-9]{2}", "yyyyDDD'T'HHxxx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][ +\\-][0-9]{3,4}", "yyyyDDD'T'HHxx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9][ +\\-][0-9]{2}", "yyyyDDD'T'HHx",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9]Z", "yyyyDDD'T'HH'Z'",
          "-?[0-9]{4}[0-3][0-9]{2}T[0-2][0-9]", "yyyyDDD'T'HH",

          // Below are variants of ISO 8601 that will go to my parseISODateTime() for parsing
          //  because they start with yyyy-M
          //  so format identifies that and otherwise is for formatting/output to mimic source.

          // order of ISO-like groups is important and optimized to conversion of
          //  single string (not StringArray):
          //    zero pad before flexi [I don't deal with this for round trip] so no groupings for it
          //    space separator before T

          // 0-padded digits with space separator
          // comma  9 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6} UTC",
              "yyyy-MM-dd HH:mm:ss,SSS000000 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6} GMT",
              "yyyy-MM-dd HH:mm:ss,SSS000000 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}UTC",
              "yyyy-MM-dd HH:mm:ss,SSS000000'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}GMT",
              "yyyy-MM-dd HH:mm:ss,SSS000000'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SSS000000xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss,SSS000000xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}[ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SSS000000x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}Z",
              "yyyy-MM-dd HH:mm:ss,SSS000000'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}",
              "yyyy-MM-dd HH:mm:ss,SSS000000",

          // period  6 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} UTC",
              "yyyy-MM-dd HH:mm:ss.SSS000000 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} GMT",
              "yyyy-MM-dd HH:mm:ss.SSS000000 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS000000 xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} [+\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.SSS000000 xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} [+\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS000000 x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}UTC",
              "yyyy-MM-dd HH:mm:ss.SSS000000'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}GMT",
              "yyyy-MM-dd HH:mm:ss.SSS000000'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS000000xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.SSS000000xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}[ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS000000x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}Z",
              "yyyy-MM-dd HH:mm:ss.SSS000000'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}",
              "yyyy-MM-dd HH:mm:ss.SSS000000",

          // 0-padded digits with space separator
          // comma  6 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3} UTC",
              "yyyy-MM-dd HH:mm:ss,SSS000 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3} GMT",
              "yyyy-MM-dd HH:mm:ss,SSS000 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}UTC",
              "yyyy-MM-dd HH:mm:ss,SSS000'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}GMT",
              "yyyy-MM-dd HH:mm:ss,SSS000'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SSS000xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss,SSS000xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SSS000x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}Z",
              "yyyy-MM-dd HH:mm:ss,SSS000'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}",
              "yyyy-MM-dd HH:mm:ss,SSS000",

          // period  6 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} UTC",
              "yyyy-MM-dd HH:mm:ss.SSS000 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} GMT",
              "yyyy-MM-dd HH:mm:ss.SSS000 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS000 xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} [+\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.SSS000 xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} [+\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS000 x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}UTC",
              "yyyy-MM-dd HH:mm:ss.SSS000'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}GMT",
              "yyyy-MM-dd HH:mm:ss.SSS000'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS000xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.SSS000xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS000x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}Z",
              "yyyy-MM-dd HH:mm:ss.SSS000'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}",
              "yyyy-MM-dd HH:mm:ss.SSS000",

          // 0-padded digits with space separator
          // comma  3 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3} UTC",
              "yyyy-MM-dd HH:mm:ss,SSS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3} GMT",
              "yyyy-MM-dd HH:mm:ss,SSS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}UTC",
              "yyyy-MM-dd HH:mm:ss,SSS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}GMT",
              "yyyy-MM-dd HH:mm:ss,SSS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SSSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss,SSSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SSSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}Z",
              "yyyy-MM-dd HH:mm:ss,SSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}",
              "yyyy-MM-dd HH:mm:ss,SSS",

          // period  3 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} UTC",
              "yyyy-MM-dd HH:mm:ss.SSS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} GMT",
              "yyyy-MM-dd HH:mm:ss.SSS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} [+\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.SSS xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} [+\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSS x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}UTC",
              "yyyy-MM-dd HH:mm:ss.SSS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}GMT",
              "yyyy-MM-dd HH:mm:ss.SSS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.SSSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}Z",
              "yyyy-MM-dd HH:mm:ss.SSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}",
              "yyyy-MM-dd HH:mm:ss.SSS",

          // comma  2
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2} UTC",
              "yyyy-MM-dd HH:mm:ss,SS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2} GMT",
              "yyyy-MM-dd HH:mm:ss,SS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}UTC",
              "yyyy-MM-dd HH:mm:ss,SS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}GMT",
              "yyyy-MM-dd HH:mm:ss,SS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss,SSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}Z",
              "yyyy-MM-dd HH:mm:ss,SS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,SS",

          // period  2
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} UTC",
              "yyyy-MM-dd HH:mm:ss.SS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} GMT",
              "yyyy-MM-dd HH:mm:ss.SS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SS xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} [+\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.SS xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} [+\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SS x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}UTC",
              "yyyy-MM-dd HH:mm:ss.SS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}GMT",
              "yyyy-MM-dd HH:mm:ss.SS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.SSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}Z",
              "yyyy-MM-dd HH:mm:ss.SS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.SS",

          // comma  1
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9] UTC",
              "yyyy-MM-dd HH:mm:ss,S 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9] GMT",
              "yyyy-MM-dd HH:mm:ss,S 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]UTC",
              "yyyy-MM-dd HH:mm:ss,S'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]GMT",
              "yyyy-MM-dd HH:mm:ss,S'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,Sxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss,Sxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss,Sx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]Z",
              "yyyy-MM-dd HH:mm:ss,S'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]",
              "yyyy-MM-dd HH:mm:ss,S",

          // period  1
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] UTC",
              "yyyy-MM-dd HH:mm:ss.S 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] GMT",
              "yyyy-MM-dd HH:mm:ss.S 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.S xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] [+\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.S xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] [+\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.S x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]UTC",
              "yyyy-MM-dd HH:mm:ss.S'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]GMT",
              "yyyy-MM-dd HH:mm:ss.S'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.Sxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss.Sxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss.Sx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]Z",
              "yyyy-MM-dd HH:mm:ss.S'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]",
              "yyyy-MM-dd HH:mm:ss.S",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9] UTC",
              "yyyy-MM-dd HH:mm:ss 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9] GMT",
              "yyyy-MM-dd HH:mm:ss 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ss xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ss xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ss x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]UTC",
              "yyyy-MM-dd HH:mm:ss'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]GMT",
              "yyyy-MM-dd HH:mm:ss'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm:ssxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm:ssxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm:ssx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z",
              "yyyy-MM-dd HH:mm:ss'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]", "yyyy-MM-dd HH:mm:ss",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9] UTC", "yyyy-MM-dd HH:mm 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9] GMT", "yyyy-MM-dd HH:mm 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9] [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mm xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9] [+\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mm xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9] [+\\-][0-9]{2}",
              "yyyy-MM-dd HH:mm x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]UTC", "yyyy-MM-dd HH:mm'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]GMT", "yyyy-MM-dd HH:mm'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HH:mmxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd HH:mmxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd HH:mmx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]Z", "yyyy-MM-dd HH:mm'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]", "yyyy-MM-dd HH:mm",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9] UTC", "yyyy-MM-dd HH 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9] GMT", "yyyy-MM-dd HH 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]UTC", "yyyy-MM-dd HH'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]GMT", "yyyy-MM-dd HH'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd HHxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9][ +\\-][0-9]{3,4}", "yyyy-MM-dd HHxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9][ +\\-][0-9]{2}", "yyyy-MM-dd HHx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]Z", "yyyy-MM-dd HH'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] [0-2][0-9]", "yyyy-MM-dd HH",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] UTC", "yyyy-MM-dd 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9] GMT", "yyyy-MM-dd 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]UTC", "yyyy-MM-dd'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]GMT", "yyyy-MM-dd'GMT'",
          // "-?[0-9]{4}-[01][0-9]-[0-3][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",  //ambiguous (time or
          // timezone). treat as time (above)
          //    "yyyy-MM-ddxxx",
          // "-?[0-9]{4}-[01][0-9]-[0-3][0-9][ +\\-][0-9]{3,4}",
          //    "yyyy-MM-ddxx",
          // "-?[0-9]{4}-[01][0-9]-[0-3][0-9][ +\\-][0-9]{2}",
          //    "yyyy-MM-ddx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]Z", "yyyy-MM-dd'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]", "yyyy-MM-dd",
          "-?[0-9]{4}-[01][0-9]", "yyyy-MM",

          // 0-padded digits with non-Digit separator (converted to T)
          //   Separator is T
          //   Note that - makes it ambiguous for a few cases:
          //      start of time (I'll assume that) or timezone.
          // comma, 9 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6} UTC",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6} GMT",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}UTC",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}GMT",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}[ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}Z",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{6}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000000",

          // period 9 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} UTC",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} GMT",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000 xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} [+\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000 xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6} [+\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000 x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}UTC",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}GMT",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}[ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}Z",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000'Z'",
          // test to switch to allowing SSSSSSSSS instead of SSS000000
          // "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{9}Z",
          //    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000000",

          // comma, 6 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3} UTC",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3} GMT",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}UTC",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}GMT",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}Z",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}0{3}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS000",

          // period 6 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} UTC",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} GMT",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000 xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} [+\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000 xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3} [+\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000 x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}UTC",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}GMT",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}Z",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS000",

          // comma, 3 S
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3} UTC",
              "yyyy-MM-dd'T'HH:mm:ss,SSS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3} GMT",
              "yyyy-MM-dd'T'HH:mm:ss,SSS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}UTC",
              "yyyy-MM-dd'T'HH:mm:ss,SSS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}GMT",
              "yyyy-MM-dd'T'HH:mm:ss,SSS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SSSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss,SSSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SSSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}Z",
              "yyyy-MM-dd'T'HH:mm:ss,SSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}",
              "yyyy-MM-dd'T'HH:mm:ss,SSS",

          // period 3
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} UTC",
              "yyyy-MM-dd'T'HH:mm:ss.SSS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} GMT",
              "yyyy-MM-dd'T'HH:mm:ss.SSS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} [+\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} [+\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}UTC",
              "yyyy-MM-dd'T'HH:mm:ss.SSS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}GMT",
              "yyyy-MM-dd'T'HH:mm:ss.SSS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.SSSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}Z",
              "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}",
              "yyyy-MM-dd'T'HH:mm:ss.SSS",

          // comma 2
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2} UTC",
              "yyyy-MM-dd'T'HH:mm:ss,SS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2} GMT",
              "yyyy-MM-dd'T'HH:mm:ss,SS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}UTC",
              "yyyy-MM-dd'T'HH:mm:ss,SS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}GMT",
              "yyyy-MM-dd'T'HH:mm:ss,SS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss,SSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}Z",
              "yyyy-MM-dd'T'HH:mm:ss,SS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,SS",

          // period 2
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} UTC",
              "yyyy-MM-dd'T'HH:mm:ss.SS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} GMT",
              "yyyy-MM-dd'T'HH:mm:ss.SS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SS xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} [+\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.SS xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} [+\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SS x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}UTC",
              "yyyy-MM-dd'T'HH:mm:ss.SS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}GMT",
              "yyyy-MM-dd'T'HH:mm:ss.SS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.SSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}Z",
              "yyyy-MM-dd'T'HH:mm:ss.SS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.SS",

          // comma 1
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9] UTC",
              "yyyy-MM-dd'T'HH:mm:ss,S 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9] GMT",
              "yyyy-MM-dd'T'HH:mm:ss,S 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]UTC",
              "yyyy-MM-dd'T'HH:mm:ss,S'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]GMT",
              "yyyy-MM-dd'T'HH:mm:ss,S'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,Sxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss,Sxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss,Sx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]Z",
              "yyyy-MM-dd'T'HH:mm:ss,S'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]",
              "yyyy-MM-dd'T'HH:mm:ss,S",

          // period 1
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] UTC",
              "yyyy-MM-dd'T'HH:mm:ss.S 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] GMT",
              "yyyy-MM-dd'T'HH:mm:ss.S 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.S xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] [+\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.S xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] [+\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.S x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]UTC",
              "yyyy-MM-dd'T'HH:mm:ss.S'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]GMT",
              "yyyy-MM-dd'T'HH:mm:ss.S'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.Sxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss.Sxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss.Sx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]Z",
              "yyyy-MM-dd'T'HH:mm:ss.S'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]",
              "yyyy-MM-dd'T'HH:mm:ss.S",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] UTC",
              "yyyy-MM-dd'T'HH:mm:ss 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] GMT",
              "yyyy-MM-dd'T'HH:mm:ss 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ss xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9] [+\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ss x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]UTC",
              "yyyy-MM-dd'T'HH:mm:ss'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]GMT",
              "yyyy-MM-dd'T'HH:mm:ss'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ssxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm:ssxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm:ssx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z",
              "yyyy-MM-dd'T'HH:mm:ss'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]",
              "yyyy-MM-dd'T'HH:mm:ss",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9]Z",
              "yyyy-MM-dd'T'HHmmss'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9]", "yyyy-MM-dd'T'HHmmss",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9][0-5][0-9]Z", "yyyy-MM-dd'T'HHmm'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9][0-5][0-9]", "yyyy-MM-dd'T'HHmm",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9] UTC", "yyyy-MM-dd'T'HH:mm 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9] GMT", "yyyy-MM-dd'T'HH:mm 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9] [+\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mm xxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9] [+\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mm xx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9] [+\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mm x",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]UTC", "yyyy-MM-dd'T'HH:mm'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]GMT", "yyyy-MM-dd'T'HH:mm'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HH:mmxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd'T'HH:mmxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd'T'HH:mmx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]Z", "yyyy-MM-dd'T'HH:mm'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]", "yyyy-MM-dd'T'HH:mm",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9] UTC", "yyyy-MM-dd'T'HH 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9] GMT", "yyyy-MM-dd'T'HH 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]UTC", "yyyy-MM-dd'T'HH'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]GMT", "yyyy-MM-dd'T'HH'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd'T'HHxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9][ +\\-][0-9]{3,4}", "yyyy-MM-dd'T'HHxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9][ +\\-][0-9]{2}", "yyyy-MM-dd'T'HHx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]Z", "yyyy-MM-dd'T'HH'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]", "yyyy-MM-dd'T'HH",

          // 0-padded digits with non-Digit separator (converted to T)
          //   Separator is -
          //   Note that - makes it ambiguous for a few cases:
          //      start of time (I'll assume that) or timezone.
          // comma 3
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3} UTC",
              "yyyy-MM-dd-HH:mm:ss,SSS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3} GMT",
              "yyyy-MM-dd-HH:mm:ss,SSS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}UTC",
              "yyyy-MM-dd-HH:mm:ss,SSS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}GMT",
              "yyyy-MM-dd-HH:mm:ss,SSS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss,SSSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd-HH:mm:ss,SSSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss,SSSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}Z",
              "yyyy-MM-dd-HH:mm:ss,SSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}",
              "yyyy-MM-dd-HH:mm:ss,SSS",

          // period 3
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} UTC",
              "yyyy-MM-dd-HH:mm:ss.SSS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} GMT",
              "yyyy-MM-dd-HH:mm:ss.SSS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}UTC",
              "yyyy-MM-dd-HH:mm:ss.SSS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}GMT",
              "yyyy-MM-dd-HH:mm:ss.SSS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss.SSSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd-HH:mm:ss.SSSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss.SSSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}Z",
              "yyyy-MM-dd-HH:mm:ss.SSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}",
              "yyyy-MM-dd-HH:mm:ss.SSS",

          // comma 2
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2} UTC",
              "yyyy-MM-dd-HH:mm:ss,SS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2} GMT",
              "yyyy-MM-dd-HH:mm:ss,SS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}UTC",
              "yyyy-MM-dd-HH:mm:ss,SS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}GMT",
              "yyyy-MM-dd-HH:mm:ss,SS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss,SSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd-HH:mm:ss,SSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss,SSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}Z",
              "yyyy-MM-dd-HH:mm:ss,SS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss,SS",

          // period 2
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} UTC",
              "yyyy-MM-dd-HH:mm:ss.SS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} GMT",
              "yyyy-MM-dd-HH:mm:ss.SS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}UTC",
              "yyyy-MM-dd-HH:mm:ss.SS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}GMT",
              "yyyy-MM-dd-HH:mm:ss.SS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss.SSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd-HH:mm:ss.SSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss.SSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}Z",
              "yyyy-MM-dd-HH:mm:ss.SS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss.SS",

          // comma 1
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9] UTC",
              "yyyy-MM-dd-HH:mm:ss,S 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9] GMT",
              "yyyy-MM-dd-HH:mm:ss,S 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]UTC",
              "yyyy-MM-dd-HH:mm:ss,S'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]GMT",
              "yyyy-MM-dd-HH:mm:ss,S'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss,Sxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd-HH:mm:ss,Sxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss,Sx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]Z",
              "yyyy-MM-dd-HH:mm:ss,S'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]",
              "yyyy-MM-dd-HH:mm:ss,S",

          // period 1
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] UTC",
              "yyyy-MM-dd-HH:mm:ss.S 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] GMT",
              "yyyy-MM-dd-HH:mm:ss.S 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]UTC",
              "yyyy-MM-dd-HH:mm:ss.S'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]GMT",
              "yyyy-MM-dd-HH:mm:ss.S'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss.Sxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd-HH:mm:ss.Sxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd-HH:mm:ss.Sx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]Z",
              "yyyy-MM-dd-HH:mm:ss.S'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]",
              "yyyy-MM-dd-HH:mm:ss.S",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9] UTC",
              "yyyy-MM-dd-HH:mm:ss 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9] GMT",
              "yyyy-MM-dd-HH:mm:ss 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]UTC",
              "yyyy-MM-dd-HH:mm:ss'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]GMT",
              "yyyy-MM-dd-HH:mm:ss'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HH:mm:ssxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd-HH:mm:ssxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd-HH:mm:ssx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z",
              "yyyy-MM-dd-HH:mm:ss'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]:[0-5][0-9]", "yyyy-MM-dd-HH:mm:ss",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9] UTC", "yyyy-MM-dd-HH:mm 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9] GMT", "yyyy-MM-dd-HH:mm 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]UTC", "yyyy-MM-dd-HH:mm'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]GMT", "yyyy-MM-dd-HH:mm'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HH:mmxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd-HH:mmxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd-HH:mmx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]Z", "yyyy-MM-dd-HH:mm'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]:[0-5][0-9]", "yyyy-MM-dd-HH:mm",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9] UTC", "yyyy-MM-dd-HH 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9] GMT", "yyyy-MM-dd-HH 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]UTC", "yyyy-MM-dd-HH'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]GMT", "yyyy-MM-dd-HH'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd-HHxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9][ +\\-][0-9]{3,4}", "yyyy-MM-dd-HHxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9][ +\\-][0-9]{2}", "yyyy-MM-dd-HHx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]Z", "yyyy-MM-dd-HH'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]-[0-2][0-9]", "yyyy-MM-dd-HH",

          // 0-padded digits with non-Digit separator (converted to T)
          //   Separator is :
          //   Note that - makes it ambiguous for a few cases:
          //      start of time (I'll assume that) or timezone.
          // comma 3
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3} UTC",
              "yyyy-MM-dd:HH:mm:ss,SSS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3} GMT",
              "yyyy-MM-dd:HH:mm:ss,SSS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}UTC",
              "yyyy-MM-dd:HH:mm:ss,SSS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}GMT",
              "yyyy-MM-dd:HH:mm:ss,SSS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss,SSSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd:HH:mm:ss,SSSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss,SSSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}Z",
              "yyyy-MM-dd:HH:mm:ss,SSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{3}",
              "yyyy-MM-dd:HH:mm:ss,SSS",

          // period 3
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} UTC",
              "yyyy-MM-dd:HH:mm:ss.SSS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3} GMT",
              "yyyy-MM-dd:HH:mm:ss.SSS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}UTC",
              "yyyy-MM-dd:HH:mm:ss.SSS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}GMT",
              "yyyy-MM-dd:HH:mm:ss.SSS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss.SSSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd:HH:mm:ss.SSSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}[ +\\-][0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss.SSSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}Z",
              "yyyy-MM-dd:HH:mm:ss.SSS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}",
              "yyyy-MM-dd:HH:mm:ss.SSS",

          // comma 2
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2} UTC",
              "yyyy-MM-dd:HH:mm:ss,SS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2} GMT",
              "yyyy-MM-dd:HH:mm:ss,SS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}UTC",
              "yyyy-MM-dd:HH:mm:ss,SS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}GMT",
              "yyyy-MM-dd:HH:mm:ss,SS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss,SSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd:HH:mm:ss,SSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}[ +\\-][0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss,SSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}Z",
              "yyyy-MM-dd:HH:mm:ss,SS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss,SS",

          // period 2
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} UTC",
              "yyyy-MM-dd:HH:mm:ss.SS 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2} GMT",
              "yyyy-MM-dd:HH:mm:ss.SS 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}UTC",
              "yyyy-MM-dd:HH:mm:ss.SS'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}GMT",
              "yyyy-MM-dd:HH:mm:ss.SS'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss.SSxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{3,4}",
              "yyyy-MM-dd:HH:mm:ss.SSxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}[ +\\-][0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss.SSx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}Z",
              "yyyy-MM-dd:HH:mm:ss.SS'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss.SS",

          // comma 1
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9] UTC",
              "yyyy-MM-dd:HH:mm:ss,S 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9] GMT",
              "yyyy-MM-dd:HH:mm:ss,S 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]UTC",
              "yyyy-MM-dd:HH:mm:ss,S'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]GMT",
              "yyyy-MM-dd:HH:mm:ss,S'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss,Sxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd:HH:mm:ss,Sxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss,Sx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]Z",
              "yyyy-MM-dd:HH:mm:ss,S'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9],[0-9]",
              "yyyy-MM-dd:HH:mm:ss,S",

          // period 1
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] UTC",
              "yyyy-MM-dd:HH:mm:ss.S 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9] GMT",
              "yyyy-MM-dd:HH:mm:ss.S 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]UTC",
              "yyyy-MM-dd:HH:mm:ss.S'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]GMT",
              "yyyy-MM-dd:HH:mm:ss.S'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss.Sxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd:HH:mm:ss.Sxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd:HH:mm:ss.Sx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]Z",
              "yyyy-MM-dd:HH:mm:ss.S'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]",
              "yyyy-MM-dd:HH:mm:ss.S",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9] UTC",
              "yyyy-MM-dd:HH:mm:ss 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9] GMT",
              "yyyy-MM-dd:HH:mm:ss 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]UTC",
              "yyyy-MM-dd:HH:mm:ss'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]GMT",
              "yyyy-MM-dd:HH:mm:ss'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HH:mm:ssxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd:HH:mm:ssxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd:HH:mm:ssx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z",
              "yyyy-MM-dd:HH:mm:ss'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]:[0-5][0-9]", "yyyy-MM-dd:HH:mm:ss",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9] UTC", "yyyy-MM-dd:HH:mm 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9] GMT", "yyyy-MM-dd:HH:mm 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]UTC", "yyyy-MM-dd:HH:mm'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]GMT", "yyyy-MM-dd:HH:mm'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HH:mmxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyy-MM-dd:HH:mmxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9][ +\\-][0-9]{2}",
              "yyyy-MM-dd:HH:mmx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]Z", "yyyy-MM-dd:HH:mm'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]:[0-5][0-9]", "yyyy-MM-dd:HH:mm",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9] UTC", "yyyy-MM-dd:HH 'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9] GMT", "yyyy-MM-dd:HH 'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]UTC", "yyyy-MM-dd:HH'UTC'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]GMT", "yyyy-MM-dd:HH'GMT'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-MM-dd:HHxxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9][ +\\-][0-9]{3,4}", "yyyy-MM-dd:HHxx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9][ +\\-][0-9]{2}", "yyyy-MM-dd:HHx",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]Z", "yyyy-MM-dd:HH'Z'",
          "-?[0-9]{4}-[01][0-9]-[0-3][0-9]:[0-2][0-9]", "yyyy-MM-dd:HH",

          // flexi digits with space separator                                  //3 = up to 3
          // decimal digits
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3} UTC",
              "yyyy-M-d H:m:s,S 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3} GMT",
              "yyyy-M-d H:m:s,S 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}UTC",
              "yyyy-M-d H:m:s,S'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}GMT",
              "yyyy-M-d H:m:s,S'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d H:m:s,Sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-M-d H:m:s,Sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-M-d H:m:s,Sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}Z",
              "yyyy-M-d H:m:s,S'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}",
              "yyyy-M-d H:m:s,S",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3} UTC",
              "yyyy-M-d H:m:s.S 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3} GMT",
              "yyyy-M-d H:m:s.S 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}UTC",
              "yyyy-M-d H:m:s.S'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}GMT",
              "yyyy-M-d H:m:s.S'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d H:m:s.Sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-M-d H:m:s.Sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-M-d H:m:s.Sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}Z",
              "yyyy-M-d H:m:s.S'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}",
              "yyyy-M-d H:m:s.S",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "yyyy-M-d H:m:s 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] GMT",
              "yyyy-M-d H:m:s 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "yyyy-M-d H:m:s'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]GMT",
              "yyyy-M-d H:m:s'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d H:m:sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "yyyy-M-d H:m:sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "yyyy-M-d H:m:sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "yyyy-M-d H:m:s'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]", "yyyy-M-d H:m:s",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9] UTC", "yyyy-M-d H:m 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9] GMT", "yyyy-M-d H:m 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]UTC", "yyyy-M-d H:m'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]GMT", "yyyy-M-d H:m'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d H:mxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "yyyy-M-d H:mxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "yyyy-M-d H:mx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]Z", "yyyy-M-d H:m'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]:[0-5]?[0-9]", "yyyy-M-d H:m",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9] UTC", "yyyy-M-d H 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9] GMT", "yyyy-M-d H 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]UTC", "yyyy-M-d H'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]GMT", "yyyy-M-d H'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d Hxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9][ +\\-][0-9]{3,4}", "yyyy-M-d Hxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9][ +\\-][0-9]{2}", "yyyy-M-d Hx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]Z", "yyyy-M-d H'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] [0-2]?[0-9]", "yyyy-M-d H",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] UTC", "yyyy-M-d 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9] GMT", "yyyy-M-d 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]UTC", "yyyy-M-d'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]GMT", "yyyy-M-d'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]Z", "yyyy-M-d'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]", "yyyy-M-d",
          "-?[0-9]{4}-[01]?[0-9]", "yyyy-M",

          // flexi digits with T separator                 //3=up to 3 decimal digits
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3} UTC",
              "yyyy-M-d'T'H:m:s,S 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3} GMT",
              "yyyy-M-d'T'H:m:s,S 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}UTC",
              "yyyy-M-d'T'H:m:s,S'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}GMT",
              "yyyy-M-d'T'H:m:s,S'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d'T'H:m:s,Sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-M-d'T'H:m:s,Sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-M-d'T'H:m:s,Sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}Z",
              "yyyy-M-d'T'H:m:s,S'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]",
              "yyyy-M-d'T'H:m:s,S",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3} UTC",
              "yyyy-M-d'T'H:m:s.S 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3} GMT",
              "yyyy-M-d'T'H:m:s.S 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}UTC",
              "yyyy-M-d'T'H:m:s.S'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}GMT",
              "yyyy-M-d'T'H:m:s.S'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d'T'H:m:s.Sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-M-d'T'H:m:s.Sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-M-d'T'H:m:s.Sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}Z",
              "yyyy-M-d'T'H:m:s.S'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}",
              "yyyy-M-d'T'H:m:s.S",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "yyyy-M-d'T'H:m:s 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] GMT",
              "yyyy-M-d'T'H:m:s 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "yyyy-M-d'T'H:m:s'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]GMT",
              "yyyy-M-d'T'H:m:s'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d'T'H:m:sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "yyyy-M-d'T'H:m:sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "yyyy-M-d'T'H:m:sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "yyyy-M-d'T'H:m:s'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "yyyy-M-d'T'H:m:s",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9] UTC", "yyyy-M-d'T'H:m 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9] GMT", "yyyy-M-d'T'H:m 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]UTC", "yyyy-M-d'T'H:m'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]GMT", "yyyy-M-d'T'H:m'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d'T'H:mxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "yyyy-M-d'T'H:mxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "yyyy-M-d'T'H:mx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]Z", "yyyy-M-d'T'H:m'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]:[0-5]?[0-9]", "yyyy-M-d'T'H:m",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9] UTC", "yyyy-M-d'T'H 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9] GMT", "yyyy-M-d'T'H 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]UTC", "yyyy-M-d'T'H'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]GMT", "yyyy-M-d'T'H'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d'T'Hxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9][ +\\-][0-9]{3,4}", "yyyy-M-d'T'Hxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9][ +\\-][0-9]{2}", "yyyy-M-d'T'Hx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]Z", "yyyy-M-d'T'H'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]T[0-2]?[0-9]", "yyyy-M-d'T'H",

          // flexi digits with - separator                   //3=up to 3 decimal digits
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3} UTC",
              "yyyy-M-d-H:m:s,S 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3} GMT",
              "yyyy-M-d-H:m:s,S 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}UTC",
              "yyyy-M-d-H:m:s,S'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}GMT",
              "yyyy-M-d-H:m:s,S'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d-H:m:s,Sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-M-d-H:m:s,Sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-M-d-H:m:s,Sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}Z",
              "yyyy-M-d-H:m:s,S'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]",
              "yyyy-M-d-H:m:s,S",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3} UTC",
              "yyyy-M-d-H:m:s.S 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3} GMT",
              "yyyy-M-d-H:m:s.S 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}UTC",
              "yyyy-M-d-H:m:s.S'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}GMT",
              "yyyy-M-d-H:m:s.S'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d-H:m:s.Sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-M-d-H:m:s.Sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-M-d-H:m:s.Sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}Z",
              "yyyy-M-d-H:m:s.S'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}",
              "yyyy-M-d-H:m:s.S",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "yyyy-M-d-H:m:s 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] GMT",
              "yyyy-M-d-H:m:s 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "yyyy-M-d-H:m:s'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]GMT",
              "yyyy-M-d-H:m:s'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d-H:m:sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "yyyy-M-d-H:m:sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "yyyy-M-d-H:m:sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "yyyy-M-d-H:m:s'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]", "yyyy-M-d-H:m:s",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9] UTC", "yyyy-M-d-H:m 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9] GMT", "yyyy-M-d-H:m 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]UTC", "yyyy-M-d-H:m'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]GMT", "yyyy-M-d-H:m'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d-H:mxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "yyyy-M-d-H:mxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "yyyy-M-d-H:mx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]Z", "yyyy-M-d-H:m'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]:[0-5]?[0-9]", "yyyy-M-d-H:m",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9] UTC", "yyyy-M-d-H 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9] GMT", "yyyy-M-d-H 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]UTC", "yyyy-M-d-H'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]GMT", "yyyy-M-d-H'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d-Hxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9][ +\\-][0-9]{3,4}", "yyyy-M-d-Hxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9][ +\\-][0-9]{2}", "yyyy-M-d-Hx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]Z", "yyyy-M-d-H'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]-[0-2]?[0-9]", "yyyy-M-d-H",

          // flexi digits with : separator                  //3=up to 3 decimal digits
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3} UTC",
              "yyyy-M-d:H:m:s,S 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3} GMT",
              "yyyy-M-d:H:m:s,S 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}UTC",
              "yyyy-M-d:H:m:s,S'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}GMT",
              "yyyy-M-d:H:m:s,S'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d:H:m:s,Sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-M-d:H:m:s,Sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-M-d:H:m:s,Sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]{1,3}Z",
              "yyyy-M-d:H:m:s,S'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9],[0-9]",
              "yyyy-M-d:H:m:s,S",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3} UTC",
              "yyyy-M-d:H:m:s.S 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3} GMT",
              "yyyy-M-d:H:m:s.S 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}UTC",
              "yyyy-M-d:H:m:s.S'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}GMT",
              "yyyy-M-d:H:m:s.S'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d:H:m:s.Sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{3,4}",
              "yyyy-M-d:H:m:s.Sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}[ +\\-][0-9]{2}",
              "yyyy-M-d:H:m:s.Sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}Z",
              "yyyy-M-d:H:m:s.S'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]\\.[0-9]{1,3}",
              "yyyy-M-d:H:m:s.S",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "yyyy-M-d:H:m:s 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] GMT",
              "yyyy-M-d:H:m:s 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "yyyy-M-d:H:m:s'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]GMT",
              "yyyy-M-d:H:m:s'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d:H:m:sxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "yyyy-M-d:H:m:sxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "yyyy-M-d:H:m:sx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "yyyy-M-d:H:m:s'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]", "yyyy-M-d:H:m:s",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9] UTC", "yyyy-M-d:H:m 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9] GMT", "yyyy-M-d:H:m 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]UTC", "yyyy-M-d:H:m'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]GMT", "yyyy-M-d:H:m'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d:H:mxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "yyyy-M-d:H:mxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "yyyy-M-d:H:mx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]Z", "yyyy-M-d:H:m'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]:[0-5]?[0-9]", "yyyy-M-d:H:m",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9] UTC", "yyyy-M-d:H 'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9] GMT", "yyyy-M-d:H 'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]UTC", "yyyy-M-d:H'UTC'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]GMT", "yyyy-M-d:H'GMT'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyy-M-d:Hxxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9][ +\\-][0-9]{3,4}", "yyyy-M-d:Hxx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9][ +\\-][0-9]{2}", "yyyy-M-d:Hx",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]Z", "yyyy-M-d:H'Z'",
          "-?[0-9]{4}-[01]?[0-9]-[0-3]?[0-9]:[0-2]?[0-9]", "yyyy-M-d:H",

          // compact ISO
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9] UTC",
              "yyyyMMddHHmmss 'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9] GMT",
              "yyyyMMddHHmmss 'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9]UTC",
              "yyyyMMddHHmmss'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9]GMT",
              "yyyyMMddHHmmss'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyyMMddHHmmssxxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyyMMddHHmmssxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{2}",
              "yyyyMMddHHmmssx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9]Z", "yyyyMMddHHmmss'Z'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9] UTC", "yyyyMMddHHmm 'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9] GMT", "yyyyMMddHHmm 'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9]UTC", "yyyyMMddHHmm'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9]GMT", "yyyyMMddHHmm'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyyMMddHHmmxxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][ +\\-][0-9]{3,4}", "yyyyMMddHHmmxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][ +\\-][0-9]{2}", "yyyyMMddHHmmx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9]Z", "yyyyMMddHHmm'Z'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9] UTC", "yyyyMMddHH 'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9] GMT", "yyyyMMddHH 'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9]UTC", "yyyyMMddHH'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9]GMT", "yyyyMMddHH'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][ +\\-][0-9]{1,2}:[0-9]{2}", "yyyyMMddHHxxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][ +\\-][0-9]{3,4}", "yyyyMMddHHxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][ +\\-][0-9]{2}", "yyyyMMddHHx",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9]Z", "yyyyMMddHH'Z'",

          // compact ISO with T
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9] UTC",
              "yyyyMMdd'T'HHmmss 'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9] GMT",
              "yyyyMMdd'T'HHmmss 'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9]UTC",
              "yyyyMMdd'T'HHmmss'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9]GMT",
              "yyyyMMdd'T'HHmmss'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyyMMdd'T'HHmmssxxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyyMMdd'T'HHmmssxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9][ +\\-][0-9]{2}",
              "yyyyMMdd'T'HHmmssx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9]Z",
              "yyyyMMdd'T'HHmmss'Z'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][0-5][0-9]", "yyyyMMdd'T'HHmmss",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9] UTC", "yyyyMMdd'T'HHmm 'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9] GMT", "yyyyMMdd'T'HHmm 'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9]UTC", "yyyyMMdd'T'HHmm'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9]GMT", "yyyyMMdd'T'HHmm'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyyMMdd'T'HHmmxxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][ +\\-][0-9]{3,4}",
              "yyyyMMdd'T'HHmmxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9][ +\\-][0-9]{2}",
              "yyyyMMdd'T'HHmmx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9]Z", "yyyyMMdd'T'HHmm'Z'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][0-5][0-9]", "yyyyMMdd'T'HHmm",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9] UTC", "yyyyMMdd'T'HH 'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9] GMT", "yyyyMMdd'T'HH 'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9]UTC", "yyyyMMdd'T'HH'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9]GMT", "yyyyMMdd'T'HH'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "yyyyMMdd'T'HHxxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][ +\\-][0-9]{3,4}", "yyyyMMdd'T'HHxx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9][ +\\-][0-9]{2}", "yyyyMMdd'T'HHx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9]Z", "yyyyMMdd'T'HH'Z'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]T[0-2][0-9]", "yyyyMMdd'T'HH",
          "[012][0-9]{3}[01][0-9][0-3][0-9] UTC", "yyyyMMdd 'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9] GMT", "yyyyMMdd 'GMT'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]UTC", "yyyyMMdd'UTC'",
          "[012][0-9]{3}[01][0-9][0-3][0-9]GMT", "yyyyMMdd'GMT'",
          // "[012][0-9]{3}[01][0-9][0-3][0-9][ +\\-][0-9]{1,2}:[0-9]{2}", "yyyyMMddxxx", //if just
          // date, time offset will screw it up
          // "[012][0-9]{3}[01][0-9][0-3][0-9][ +\\-][0-9]{3,4}",          "yyyyMMddxx",
          // "[012][0-9]{3}[01][0-9][0-3][0-9][ +\\-][0-9]{2}",          "yyyyMMddx",
          "[012][0-9]{3}[01][0-9][0-3][0-9]Z", "yyyyMMdd'Z'",

          // 2017-03-23 2 digit year (yy) is no longer supported

          // misc formats
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "M/d/yyyy H:m:s 'UTC'", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "M-d-yyyy H:m:s 'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d MMM yyyy H:m:s 'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d-MMM-yyyy H:m:s 'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d MMMM yyyy H:m:s 'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d-MMMM-yyyy H:m:s 'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "M/d/yyyy H:m:s'UTC'", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "M-d-yyyy H:m:s'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d MMM yyyy H:m:s'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d-MMM-yyyy H:m:s'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d MMMM yyyy H:m:s'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d-MMMM-yyyy H:m:s'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "M/d/yyyy H:m:s 'UTC'", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "M-d-yyyy H:m:s 'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d MMM yyyy H:m:s 'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d-MMM-yyyy H:m:s 'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d MMMM yyyy H:m:s 'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d-MMMM-yyyy H:m:s 'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "M/d/yyyy H:m:s'UTC'", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "M-d-yyyy H:m:s'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d MMM yyyy H:m:s'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d-MMM-yyyy H:m:s'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d MMMM yyyy H:m:s'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d-MMMM-yyyy H:m:s'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "M/d/yyyy H:m:sxxx", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "M-d-yyyy H:m:sxxx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d MMM yyyy H:m:sxxx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d-MMM-yyyy H:m:sxxx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d MMMM yyyy H:m:sxxx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d-MMMM-yyyy H:m:sxxx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "M/d/yyyy H:m:sxx", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "M-d-yyyy H:m:sxx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d MMM yyyy H:m:sxx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d-MMM-yyyy H:m:sxx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d MMMM yyyy H:m:sxx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d-MMMM-yyyy H:m:sxx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "M/d/yyyy H:m:sx", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "M-d-yyyy H:m:sx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d MMM yyyy H:m:sx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d-MMM-yyyy H:m:sx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d MMMM yyyy H:m:sx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d-MMMM-yyyy H:m:sx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "M/d/yyyy H:m:s'Z'", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "M-d-yyyy H:m:s'Z'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "d MMM yyyy H:m:s'Z'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "d-MMM-yyyy H:m:s'Z'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "d MMMM yyyy H:m:s'Z'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "d-MMMM-yyyy H:m:s'Z'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "M/d/yyyy H:m:s", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "M-d-yyyy H:m:s", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "d MMM yyyy H:m:s", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "d-MMM-yyyy H:m:s", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "d MMMM yyyy H:m:s", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "d-MMMM-yyyy H:m:s", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "M/d/yyyy H:m 'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d MMM yyyy H:m 'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d-MMM-yyyy H:m 'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d MMMM yyyy H:m 'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d-MMMM-yyyy H:m 'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "M/d/yyyy H:m'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d MMM yyyy H:m'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d-MMM-yyyy H:m'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d MMMM yyyy H:m'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d-MMMM-yyyy H:m'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "M/d/yyyy H:m 'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d MMM yyyy H:m 'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d-MMM-yyyy H:m 'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d MMMM yyyy H:m 'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d-MMMM-yyyy H:m 'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "M/d/yyyy H:m'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d MMM yyyy H:m'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d-MMM-yyyy H:m'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d MMMM yyyy H:m'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d-MMMM-yyyy H:m'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "M/d/yyyy H:mxxx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d MMM yyyy H:mxxx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d-MMM-yyyy H:mxxx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d MMMM yyyy H:mxxx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d-MMMM-yyyy H:mxxx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "M/d/yyyy H:mxx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d MMM yyyy H:mxx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d-MMM-yyyy H:mxx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d MMMM yyyy H:mxx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d-MMMM-yyyy H:mxx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "M/d/yyyy H:mx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d MMM yyyy H:mx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d-MMM-yyyy H:mx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d MMMM yyyy H:mx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d-MMMM-yyyy H:mx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]Z",
              "M/d/yyyy H:m'Z'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]Z",
              "d MMM yyyy H:m'Z'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]Z",
              "d-MMM-yyyy H:m'Z'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]Z",
              "d MMMM yyyy H:m'Z'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]Z",
              "d-MMMM-yyyy H:m'Z'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]",
              "M/d/yyyy H:m", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]", "d MMM yyyy H:m", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]",
              "d-MMM-yyyy H:m", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]:[0-5]?[0-9]",
              "d MMMM yyyy H:m", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]",
              "d-MMMM-yyyy H:m", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9] UTC",
              "M/d/yyyy H 'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9] UTC", "d MMM yyyy H 'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9] UTC", "d-MMM-yyyy H 'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9] UTC",
              "d MMMM yyyy H 'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9] UTC",
              "d-MMMM-yyyy H 'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]UTC",
              "M/d/yyyy H'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]UTC", "d MMM yyyy H'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]UTC", "d-MMM-yyyy H'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]UTC", "d MMMM yyyy H'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]UTC",
              "d-MMMM-yyyy H'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9] UTC",
              "M/d/yyyy H 'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9] UTC", "d MMM yyyy H 'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9] UTC", "d-MMM-yyyy H 'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9] UTC",
              "d MMMM yyyy H 'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9] UTC",
              "d-MMMM-yyyy H 'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]UTC",
              "M/d/yyyy H'UTC'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]UTC", "d MMM yyyy H'UTC'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]UTC", "d-MMM-yyyy H'UTC'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]UTC", "d MMMM yyyy H'UTC'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]UTC",
              "d-MMMM-yyyy H'UTC'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "M/d/yyyy Hxxx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d MMM yyyy Hxxx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d-MMM-yyyy Hxxx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d MMMM yyyy Hxxx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d-MMMM-yyyy Hxxx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9][ +\\-][0-9]{3,4}",
              "M/d/yyyy Hxx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9][ +\\-][0-9]{3,4}",
              "d MMM yyyy Hxx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9][ +\\-][0-9]{3,4}",
              "d-MMM-yyyy Hxx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9][ +\\-][0-9]{3,4}",
              "d MMMM yyyy Hxx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9][ +\\-][0-9]{3,4}",
              "d-MMMM-yyyy Hxx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9][ +\\-][0-9]{2}",
              "M/d/yyyy Hx", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9][ +\\-][0-9]{2}",
              "d MMM yyyy Hx", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9][ +\\-][0-9]{2}",
              "d-MMM-yyyy Hx", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9][ +\\-][0-9]{2}",
              "d MMMM yyyy Hx", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9][ +\\-][0-9]{2}",
              "d-MMMM-yyyy Hx", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]Z", "M/d/yyyy H'Z'", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]Z", "d MMM yyyy H'Z'", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]Z", "d-MMM-yyyy H'Z'", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]Z", "d MMMM yyyy H'Z'", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]Z", "d-MMMM-yyyy H'Z'", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4} [012]?[0-9]", "M/d/yyyy H", // assume US ordering
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012]?[0-9]", "d MMM yyyy H", // 2 Jan 85
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4} [012]?[0-9]", "d-MMM-yyyy H", // 02-JAN-1985
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012]?[0-9]", "d MMMM yyyy H", // 2 January 85
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4} [012]?[0-9]", "d-MMMM-yyyy H", // 02-JANUARY-1985
          "[01]?[0-9]/[0123]?[0-9]/[0-9]{4}", "M/d/yyyy", // assume US ordering
          "[01]?[0-9]-[0123]?[0-9]-[0-9]{4}", "M-d-yyyy", // assume US ordering
          "[0123][0-9] [a-zA-Z]{3} [0-9]{4}", "dd MMM yyyy", // 02 Jan 85  //catch zero-padded first
          "[0123]?[0-9] [a-zA-Z]{3} [0-9]{4}", "d MMM yyyy", // 2 Jan 85
          "[0123][0-9]-[a-zA-Z]{3}-[0-9]{4}",
              "dd-MMM-yyyy", // 02-JAN-1985 //catch zero-padded first
          "[0123]?[0-9]-[a-zA-Z]{3}-[0-9]{4}", "d-MMM-yyyy", // 2-JAN-1985
          "[0123][0-9][a-zA-Z]{3}[0-9]{4}", "ddMMMyyyy", // 02JAN1985 //catch zero-padded first
          "[0123]?[0-9][a-zA-Z]{3}[0-9]{4}", "dMMMyyyy", // 2JAN1985
          "[0-9]{4}[a-zA-Z]{3}[0123][0-9]",
              "yyyyMMMdd", // 1985JAN02  DOD uses this  //catch zero-padded first
          "[0-9]{4}[a-zA-Z]{3}[0123]?[0-9]", "yyyyMMMd", // 1985JAN02
          "[0123][0-9] [a-zA-Z]{3,} [0-9]{4}",
              "dd MMMM yyyy", // 02 January 85 //catch zero-padded first
          "[0123]?[0-9] [a-zA-Z]{3,} [0-9]{4}", "d MMMM yyyy", // 2 January 85
          "[0123][0-9]-[a-zA-Z]{3,}-[0-9]{4}",
              "dd-MMMM-yyyy", // 02-JANUARY-1985  //catch zero-padded first
          "[0123]?[0-9]-[a-zA-Z]{3,}-[0-9]{4}", "d-MMMM-yyyy", // 2-JANUARY-1985
          "[0-9]{4}[a-zA-Z]{3,}[0123]?[0-9]", "yyyyMMMMd", // 1985JANUARY02
          "[0123][0-9]Z[0123]?[0-9][a-zA-Z]{3}[0-9]{4}", "H'Z'dMMMyyyy", // 00Z29dec2013
          "[0123][0-9]Z[0123]?[0-9][a-zA-Z]{3,}[0-9]{4}", "H'Z'dMMMMyyyy", // 00Z29december2013
          "[0-9]{4}/[01]?[0-9]/[0123]?[0-9]", "yyyy/M/d", // bad iso variant
          "[01][0-9]-[0-9]{4}", "MM-yyyy",

          // check for Euro ordering of slashes if fell through
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d/M/yyyy H:m:s 'UTC'", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d-M-yyyy H:m:s 'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d/M/yyyy H:m:s'UTC'", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d-M-yyyy H:m:s'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d/M/yyyy H:m:s 'UTC'", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9] UTC",
              "d-M-yyyy H:m:s 'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d/M/yyyy H:m:s'UTC'", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]UTC",
              "d-M-yyyy H:m:s'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d/M/yyyy H:m:sxxx", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d-M-yyyy H:m:sxxx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d/M/yyyy H:m:sxx", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d-M-yyyy H:m:sxx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d/M/yyyy H:m:sx", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d-M-yyyy H:m:sx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "d/M/yyyy H:m:s'Z'", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]Z",
              "d-M-yyyy H:m:s'Z'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "d/M/yyyy H:m:s", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4} [012]?[0-9]:[0-5]?[0-9]:[0-5]?[0-9]",
              "d-M-yyyy H:m:s", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d/M/yyyy H:m 'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d/M/yyyy H:m'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9] UTC",
              "d/M/yyyy H:m 'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]UTC",
              "d/M/yyyy H:m'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d/M/yyyy H:mxxx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{3,4}",
              "d/M/yyyy H:mxx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9][ +\\-][0-9]{2}",
              "d/M/yyyy H:mx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]Z",
              "d/M/yyyy H:m'Z'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]:[0-5]?[0-9]",
              "d/M/yyyy H:m", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9] UTC",
              "d/M/yyyy H 'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]UTC", "d/M/yyyy H'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9] UTC",
              "d/M/yyyy H 'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]UTC", "d/M/yyyy H'UTC'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9][ +\\-][0-9]{1,2}:[0-9]{2}",
              "d/M/yyyy Hxxx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9][ +\\-][0-9]{3,4}",
              "d/M/yyyy Hxx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9][ +\\-][0-9]{2}",
              "d/M/yyyy Hx", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]Z", "d/M/yyyy H'Z'", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4} [012]?[0-9]", "d/M/yyyy H", // try Euro ordering
          "[0123]?[0-9]/[01]?[0-9]/[0-9]{4}", "d/M/yyyy", // try Euro ordering
          "[0123]?[0-9]-[01]?[0-9]-[0-9]{4}", "d-M-yyyy" // try Euro ordering
          );

  /**
   * This has alternating regex/timeFormat for formats where all chars are digits. This is used by
   * suggestDateTimeFormat.
   */
  public static final ImmutableList<String> allDigitsRegexTimeFormat =
      ImmutableList.of(
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9]", "yyyyMMddHHmmss",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9][0-5][0-9]", "yyyyMMddHHmm",
          "[012][0-9]{3}[01][0-9][0-3][0-9][0-2][0-9]", "yyyyMMddHH",
          "[012][0-9]{3}[01][0-9][0-3][0-9]", "yyyyMMdd",
          "[12][0-9]{3}[01][0-9]", "yyyyMM", // no 0xxx years, avoid misinterpret HHMMSS
          "[012][0-9]{3}[0-3][0-9]{2}",
              "yyyyDDD" // compact, negative is uncommon and too much like a number
          );

  /**
   * This has alternating regex/timeFormat for formats where the first char is a letter. This is
   * used by suggestDateTimeFormat. For time zone offset parsing:
   * https://stackoverflow.com/questions/34637626/java-datetimeformatter-for-time-zone-with-an-optional-colon-separator/34637904
   */
  public static final ImmutableList<String> letterRegexTimeFormat =
      ImmutableList.of(
          // test formats that start with a letter
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}Z",
          "MMM d, yyyy HH:mm:ss.SSS000000'Z'", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}",
          "MMM d, yyyy HH:mm:ss.SSS000000", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}Z",
          "MMM d, yyyy HH:mm:ss.SSS000'Z'", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}",
          "MMM d, yyyy HH:mm:ss.SSS000", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}Z",
          "MMM d, yyyy HH:mm:ss.SSS'Z'", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}",
          "MMM d, yyyy HH:mm:ss.SSS", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}Z",
          "MMM d, yyyy HH:mm:ss.SS'Z'", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}",
          "MMM d, yyyy HH:mm:ss.SS", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]Z",
          "MMM d, yyyy HH:mm:ss.S'Z'", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]",
          "MMM d, yyyy HH:mm:ss.S", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]Z",
          "MMM d, yyyy HH:mm:ss'Z'", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]",
          "MMM d, yyyy HH:mm:ss", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]Z",
          "MMM d, yyyy HH:mm'Z'", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]",
          "MMM d, yyyy HH:mm", // Jan 2, 1985
          "[a-zA-Z]{3} [0123]?[0-9], [0-9]{4}",
          "MMM d, yyyy", // Jan 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}Z",
          "MMMM d, yyyy HH:mm:ss.SSS000000'Z'", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{6}",
          "MMMM d, yyyy HH:mm:ss.SSS000000", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}Z",
          "MMMM d, yyyy HH:mm:ss.SSS000'Z'", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}0{3}",
          "MMMM d, yyyy HH:mm:ss.SSS000", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}Z",
          "MMMM d, yyyy HH:mm:ss.SSS'Z'", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}",
          "MMMM d, yyyy HH:mm:ss.SSS", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}Z",
          "MMMM d, yyyy HH:mm:ss.SS'Z'", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{2}",
          "MMMM d, yyyy HH:mm:ss.SS", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]Z",
          "MMMM d, yyyy HH:mm:ss.S'Z'", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]",
          "MMMM d, yyyy HH:mm:ss.S", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]Z",
          "MMMM d, yyyy HH:mm:ss'Z'", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]",
          "MMMM d, yyyy HH:mm:ss", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]Z",
          "MMMM d, yyyy HH:mm'Z'", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4} [012][0-9]:[0-5][0-9]",
          "MMMM d, yyyy HH:mm", // January 2, 1985
          "[a-zA-Z]{3,} [0123]?[0-9], [0-9]{4}",
          "MMMM d, yyyy", // January 2, 1985

          //                 "Sun, 06 Nov 1994 08:49:37 GMT"
          // GMT is literal. java.time.format.DateTimeFormatter (was Joda) doesn't parse z
          RFC822_GMT_REGEX,
          RFC822_GMT_FORMAT, // RFC 822 format date time
          //                 "Sun, 06 Nov 1994 08:49:37 -0800" or -08:00
          "[a-zA-Z]{3}, [0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] [ +\\-][0-9]{2}",
          "EEE, d MMM yyyy HH:mm:ss x",
          "[a-zA-Z]{3}, [0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] [ +\\-][0-9]{3,4}",
          "EEE, d MMM yyyy HH:mm:ss xx", // RFC 822 format date time
          "[a-zA-Z]{3}, [0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] [ +\\-][0-9]{1,2}:[0-9]{2}",
          "EEE, d MMM yyyy HH:mm:ss xxx",
          "[a-zA-Z]{3}, [0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] Z",
          "EEE, d MMM yyyy HH:mm:ss 'Z'",
          "[a-zA-Z]{3}, [0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]Z",
          "EEE, d MMM yyyy HH:mm:ss'Z'",
          "[a-zA-Z]{3}, [0123]?[0-9] [a-zA-Z]{3} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]",
          "EEE, d MMM yyyy HH:mm:ss",
          // Tue Dec 6 17:36:16 2017  some variety in how single digit date and hour are written
          "[a-zA-Z]{3} [a-zA-Z]{3} [ 0123][0-9] [ 012][0-9]:[0-5][0-9]:[0-5][0-9] [0-9]{4}", // 2
          // digits
          "EEE MMM dd HH:mm:ss yyyy",
          "[a-zA-Z]{3} [a-zA-Z]{3} [0123]?[0-9] [012]?[0-9]:[0-5][0-9]:[0-5][0-9] [0-9]{4}", // variable nDigits
          "EEE MMM d H:mm:ss yyyy",

          //                 "Sun, 06 November 1994 08:49:37 GMT"
          // GMT is literal. java.time.format.DateTimeFormatter (was Joda) doesn't parse z
          "[a-zA-Z]{3,}, [0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] GMT",
          "EEEE, d MMMM yyyy HH:mm:ss 'GMT'", // RFC 822 format date time
          //                 "Sun, 06 Nove,ber 1994 08:49:37 -0800" or -08:00
          "[a-zA-Z]{3,}, [0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] [ +\\-][0-9]{2}",
          "EEEE, d MMMM yyyy HH:mm:ss x",
          "[a-zA-Z]{3,}, [0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] [ +\\-][0-9]{3,4}",
          "EEEE, d MMMM yyyy HH:mm:ss xx", // RFC 822 format date time
          "[a-zA-Z]{3,}, [0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] [ +\\-][0-9]{1,2}:[0-9]{2}",
          "EEEE, d MMMM yyyy HH:mm:ss xxx",
          "[a-zA-Z]{3,}, [0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9] Z",
          "EEEE, d MMMM yyyy HH:mm:ss 'Z'",
          "[a-zA-Z]{3,}, [0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]Z",
          "EEEE, d MMMM yyyy HH:mm:ss'Z'",
          "[a-zA-Z]{3,}, [0123]?[0-9] [a-zA-Z]{3,} [0-9]{4} [012][0-9]:[0-5][0-9]:[0-5][0-9]",
          "EEEE, d MMMM yyyy HH:mm:ss",
          // Tue December 6 17:36:16 2017
          "[a-zA-Z]{3,} [a-zA-Z]{3,} [0123]?[0-9] [012][0-9]:[0-5][0-9]:[0-5][0-9] [0-9]{4}",
          "EEEE MMMM d HH:mm:ss yyyy");

  /** This makes a hashMap of the dateTimeFormat pointing to a compiled regex. */
  public static final HashMap<String, Pattern> dateTimeFormatPatternHM = new HashMap<>();

  // can't test hasComma because of e.g., {1,6}
  public static char[] digitRegexTimeFormatLastChar =
      new char[digitRegexTimeFormat.size()]; // to catch/match C|T|Z
  public static final BitSet digitRegexTimeFormatHasColon = new BitSet(digitRegexTimeFormat.size());
  public static final BitSet digitRegexTimeFormatHasPeriod =
      new BitSet(digitRegexTimeFormat.size()); // all periods in regexes are literals
  public static final BitSet digitRegexTimeFormatHasSlash = new BitSet(digitRegexTimeFormat.size());
  // not hasT because of 'T'hursday
  public static char[] letterRegexTimeFormatLastChar = new char[letterRegexTimeFormat.size()];

  static {
    for (int i = 0; i < digitRegexTimeFormat.size(); i += 2) {
      String drtf = digitRegexTimeFormat.get(i);
      digitRegexTimeFormatLastChar[i] = drtf.charAt(drtf.length() - 1);
      digitRegexTimeFormatHasColon.set(i, drtf.indexOf(':') >= 0);
      digitRegexTimeFormatHasPeriod.set(i, drtf.indexOf('.') >= 0);
      digitRegexTimeFormatHasSlash.set(i, drtf.indexOf('/') >= 0);
      dateTimeFormatPatternHM.put(digitRegexTimeFormat.get(i + 1), Pattern.compile(drtf));
    }

    for (int i = 0; i < allDigitsRegexTimeFormat.size(); i += 2) {
      String drtf = allDigitsRegexTimeFormat.get(i);
      dateTimeFormatPatternHM.put(allDigitsRegexTimeFormat.get(i + 1), Pattern.compile(drtf));
    }

    for (int i = 0; i < letterRegexTimeFormat.size(); i += 2) {
      letterRegexTimeFormatLastChar[i] =
          letterRegexTimeFormat.get(i).charAt(letterRegexTimeFormat.get(i).length() - 1);
      dateTimeFormatPatternHM.put(
          letterRegexTimeFormat.get(i + 1), Pattern.compile(letterRegexTimeFormat.get(i)));
    }
  }

  /** The IDEAL values are used for makeIdealGC. */
  public static String[] IDEAL_N_OPTIONS = new String[100];

  static {
    for (int i = 0; i < 100; i++) IDEAL_N_OPTIONS[i] = "" + (i + 1);
  }

  public static final ImmutableList<String> IDEAL_UNITS_OPTIONS =
      ImmutableList.of("second(s)", "minute(s)", "hour(s)", "day(s)", "month(s)", "year(s)");
  public static final ImmutableList<Integer> IDEAL_UNITS_SECONDS =
      ImmutableList.of( // where imprecise, these are on the low end
          1, 60, SECONDS_PER_HOUR, SECONDS_PER_DAY, 30 * SECONDS_PER_DAY, 365 * SECONDS_PER_DAY);
  public static final ImmutableList<Integer> IDEAL_UNITS_FIELD =
      ImmutableList.of(SECOND, MINUTE, HOUR_OF_DAY, DATE, MONTH, YEAR); // month is 0..

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
   * if you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean reallyVerbose = false;

  /**
   * Set this to true (by calling debugMode=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static final boolean debugMode = false;

  /**
   * This tests if the units are numeric time units. This is a good, lenient, quick hueristic. For a
   * definitive test, use getTimeBaseAndFactor(String tsUnits).
   */
  public static boolean isNumericTimeUnits(String tUnits) {
    if (tUnits == null) return false;
    return NUMERIC_TIME_PATTERN.matcher(tUnits.toLowerCase()).matches();
  }

  /**
   * This tests if the units are String time units ("uuuu", "yyyy": a formatting string which has
   * the year designator).
   *
   * <p>The test for numeric time units is a good, quick hueristic. For a definitive test, use
   * getTimeBaseAndFactor(String tsUnits).
   */
  public static boolean isStringTimeUnits(String tUnits) {
    if (tUnits == null) return false;
    tUnits = tUnits.toLowerCase();
    return tUnits.indexOf("uuuu") >= 0 || tUnits.indexOf("yyyy") >= 0;
  }

  /**
   * This tests if the units are numeric time units (basically, has "since") or or String time units
   * ("uuuu", "yyyy": a formatting string which has the year designator).
   *
   * <p>The test for numeric time units is a good, quick hueristic. For a definitive test, use
   * getTimeBaseAndFactor(String tsUnits).
   */
  public static boolean isTimeUnits(String tUnits) {
    if (tUnits == null) return false;
    tUnits = tUnits.toLowerCase();
    return tUnits.indexOf("uuuu") >= 0 || tUnits.indexOf("yyyy") >= 0 || isNumericTimeUnits(tUnits);
  }

  /** This variant assumes Zulu time zone. */
  public static double[] getTimeBaseAndFactor(String tsUnits, boolean legacyAdjust) {
    return getTimeBaseAndFactor(tsUnits, null, legacyAdjust);
  }

  public static double[] getTimeBaseAndFactor(String tsUnits) {
    return getTimeBaseAndFactor(tsUnits, null, false);
  }

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
  public static double[] getTimeBaseAndFactor(String tsUnits, ZoneId zoneId, boolean legacyAdjust) {
    String errorInMethod = String2.ERROR + " in Calendar2.getTimeBaseAndFactor(" + tsUnits + "):\n";

    Test.ensureNotNull(tsUnits, errorInMethod + "units string is null.");
    int sincePo = tsUnits.toLowerCase().indexOf(" since ");
    if (sincePo <= 0)
      throw new SimpleException(errorInMethod + "units string doesn't contain \" since \".");
    String tUnits = tsUnits.substring(0, sincePo).trim().toLowerCase();
    tUnits = String2.replaceAll(tUnits, "julian day", "day");
    double factorToGetSeconds = factorToGetSeconds(tUnits); // throws exception if trouble
    String dateTime = tsUnits.substring(sincePo + 7); // any string format
    String isoDateTime = tryToIsoString(dateTime);
    if (isoDateTime.length() == 0) {
      String2.log("base dateTime=" + dateTime);
      throw new SimpleException(errorInMethod + "unable to parse base dateTime.");
    }

    ZonedDateTime baseZdt = parseISODateTime(isoDateTime, zoneId == null ? ZoneOffset.UTC : zoneId);
    double baseSeconds = baseZdt.toInstant().toEpochMilli() / 1000.0;

    if (legacyAdjust) {
      GregorianCalendar gc =
          new GregorianCalendar(
              baseZdt.getYear(),
              baseZdt.getMonthValue() - 1,
              baseZdt.getDayOfMonth(),
              baseZdt.getHour(),
              baseZdt.getMinute(),
              baseZdt.getSecond());
      if (zoneId != null) {
        gc.setTimeZone(TimeZone.getTimeZone(zoneId));
      } else {
        gc.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
      }
      baseSeconds = gc.getTimeInMillis() / 1000.0;
    }
    return new double[] {baseSeconds, factorToGetSeconds};
  }

  /**
   * This converts a unitsSince value into epochSeconds. This properly handles 'special'
   * factorToGetSeconds values (for month and year).
   *
   * @param baseSeconds from getTimeBaseAndFactor[0]
   * @param factorToGetSeconds from getTimeBaseAndFactor[1]
   * @param sourceUnitsSince a numeric PrimitiveArray with time values in the source units
   * @return a DoubleArray with seconds since 1970-01-01 (or NaN if unitsSince is NaN). If
   *     sourceUnitsSince was a DoubleArray, it will be the same DoubleArray.
   */
  public static DoubleArray unitsSinceToEpochSeconds(
      double baseSeconds, double factorToGetSeconds, PrimitiveArray sourceUnitsSince) {
    int n = sourceUnitsSince.size();
    DoubleArray epSec =
        sourceUnitsSince instanceof DoubleArray da ? da : new DoubleArray(n, true); // active
    for (int i = 0; i < n; i++)
      epSec.set(
          i,
          unitsSinceToEpochSeconds(baseSeconds, factorToGetSeconds, sourceUnitsSince.getDouble(i)));
    return epSec;
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
    double epSec;
    if (factorToGetSeconds >= 30 * SECONDS_PER_DAY) { // i.e. >= a month
      // floor yields consistent results below for decimal months
      int intUnitsSince = Math2.roundToInt(Math.floor(unitsSince));
      if (intUnitsSince == Integer.MAX_VALUE) return Double.NaN;
      TemporalUnit units;
      if (factorToGetSeconds == 30 * SECONDS_PER_DAY) units = ChronoUnit.MONTHS;
      else if (factorToGetSeconds == 360 * SECONDS_PER_DAY) units = ChronoUnit.YEARS;
      else
        throw new RuntimeException(
            String2.ERROR
                + " in Calendar2.unitsSinceToEpochSeconds: factorToGetSeconds=\""
                + factorToGetSeconds
                + "\" not expected.");
      ZonedDateTime zd = epochSecondsToZdt(baseSeconds);
      zd = zd.plus(intUnitsSince, units);
      if (unitsSince != intUnitsSince) {
        double frac = unitsSince - intUnitsSince; // will be positive because floor was used
        if (units == ChronoUnit.MONTHS) {
          // Round fractional part to nearest day.  Better if based on nDays in current month?
          // (Note this differs from UDUNITS month = 3.15569259747e7 / 12 seconds.)
          zd = zd.plusDays(Math2.roundToInt(frac * 30));
        } else if (units == ChronoUnit.YEARS) {
          // Round fractional part to nearest month.
          // (Note this differs from UDUNITS year = 3.15569259747e7 seconds.)
          zd = zd.plusMonths(Math2.roundToInt(frac * 12));
        }
      }
      epSec = zdtToEpochSeconds(zd);
    } else if (factorToGetSeconds >= 60) { // i.e. >= a minute
      // It's a bad idea to store to-the-second times as e.g., "days since"
      // because the floating point numbers are bruised.
      // So I round "minutes since" or longer to nearest milli
      // ???Should it round to nearest 0.1s or nearest second???
      epSec = Math2.roundTo(baseSeconds + unitsSince * factorToGetSeconds, 3); // 3 decimal places
    } else {
      epSec = baseSeconds + unitsSince * factorToGetSeconds;
    }

    return epSec;
  }

  /**
   * This converts an epochSeconds value into a unitsSince value. This properly handles 'special'
   * factorToGetSeconds values (for month and year).
   *
   * @param baseSeconds
   * @param factorToGetSeconds
   * @param epochSeconds
   * @return seconds since 1970-01-01 (or NaN if epochSeconds is NaN)
   */
  public static double epochSecondsToUnitsSince(
      double baseSeconds, double factorToGetSeconds, double epochSeconds) {
    if (factorToGetSeconds >= 30 * SECONDS_PER_DAY) {
      if (!Double.isFinite(epochSeconds)) return Double.NaN;
      ZonedDateTime es = epochSecondsToZdt(epochSeconds);
      ZonedDateTime bs = epochSecondsToZdt(baseSeconds);
      if (factorToGetSeconds == 30 * SECONDS_PER_DAY) {
        // months (and days)
        // expand this to support fractional months???
        int esm = es.getYear() * 12 + es.getMonthValue() - 1;
        int bsm = bs.getYear() * 12 + bs.getMonthValue() - 1;
        return esm - bsm;
      } else if (factorToGetSeconds == 360 * SECONDS_PER_DAY) {
        // years (and months)
        // expand this to support fractional years???
        return es.getYear() - bs.getYear();
      } else
        throw new RuntimeException(
            String2.ERROR
                + " in Calendar2.epochSecondsToUnitsSince: factorToGetSeconds=\""
                + factorToGetSeconds
                + "\" not expected.");
    }
    return (epochSeconds - baseSeconds) / factorToGetSeconds;
  }

  /**
   * This returns the factor to multiply by 'units' data to get seconds data (e.g., "minutes"
   * returns 60). This is used for part of dealing with udunits-style "minutes since
   * 1970-01-01"-style strings.
   *
   * @param units e.g., msec, sec, minutes, hours, days, weeks, months, years ... (abbreviated or
   *     not) (singular or plural). This is lenient.
   * @return the factor to multiply by 'units' data to get seconds data. Since there is no exact
   *     value for months or years, this returns special values of 30*SECONDS_PER_DAY and
   *     360*SECONDS_PER_DAY, respectively.
   * @throws RuntimeException if trouble (e.g., units is null or not an expected value)
   */
  public static double factorToGetSeconds(String units) {
    units = units.trim().toLowerCase();
    switch (units) {
      case "ms",
          "msec",
          "msecs",
          "millis",
          "millisec",
          "millisecs",
          "millisecond",
          "milliseconds" -> {
        return 0.001;
      }
      case "s", "sec", "secs", "second", "seconds" -> {
        return 1;
      }
      case "m", "min", "mins", "minute", "minutes" -> {
        return SECONDS_PER_MINUTE;
      }
      case "h", "hr", "hrs", "hour", "hours" -> {
        return SECONDS_PER_HOUR;
      }
      case "d", "day", "days" -> {
        return SECONDS_PER_DAY;
      }
      case "week", "weeks" -> {
        return 7 * SECONDS_PER_DAY;
      }
      case "mon", "mons", "month", "months" -> {
        return 30 * SECONDS_PER_DAY;
      }
      case "yr", "yrs", "year", "years" -> {
        return 360 * SECONDS_PER_DAY;
      }
    }
    Test.error(
        String2.ERROR + " in Calendar2.factorToGetSeconds: units=\"" + units + "\" is invalid.");
    return Double.NaN; // won't happen, but method needs return statement
  }

  /**
   * This converts units (e.g. "seconds") into the corresponding Calendar constant (e.g.,
   * Calendar.SECOND).
   *
   * @param units e.g., "s", "seconds", "hours" or "Days"
   * @return the corresponding Calendar constant, e.g., HOUR_OF_DAY (0..23) (not HOUR, which is
   *     0..11).
   * @throws Exception if trouble (e.g., units is null or not an expected value)
   */
  public static int unitsToConstant(String units) throws Exception {

    double d = factorToGetSeconds(units); // e.g., "SECONDS"
    if (d == 0.001) return MILLISECOND;
    if (d == 1) return SECOND;
    if (d == SECONDS_PER_MINUTE) return MINUTE;
    if (d == SECONDS_PER_HOUR) return HOUR_OF_DAY;
    if (d == SECONDS_PER_DAY) return DATE;
    if (d == SECONDS_PER_DAY * 7) return Calendar.WEEK_OF_YEAR;
    if (d == SECONDS_PER_DAY * 30) return MONTH;
    if (d == SECONDS_PER_DAY * 360) return YEAR;
    // shouldn't get here
    Test.error(
        String2.ERROR + " in Calendar2.factorToGetSeconds: units=\"" + units + "\" is invalid.");
    return -1; // won't happen, but method needs return statement
  }

  /**
   * This converts a string with number[timeUnits] into the number (with timeUnits applied), e.g.,
   * 10.4 or 10 minutes (becomes 600). If timeUnits are specified, this returns the number of
   * seconds.
   *
   * @param ntu optional number + optional timeUnits. But one of them must be specified.
   * @return [0]=the number (1 if not specified), [1]=factorToGetSeconds (1 if not specified)
   * @throws RuntimeException if trouble, e.g, ntu is null or "", or number is not a number, or
   *     optional timeUnits not valid.
   */
  public static double[] parseNumberTimeUnits(String ntu) {
    String errIn = "ERROR in parseNumberTimeUnits: ";
    if (ntu == null) throw new SimpleException(errIn + "nothing specified.");
    ntu = ntu.trim();
    if (ntu.length() == 0) throw new SimpleException(errIn + "nothing specified.");

    // find last non-letter by walking backward, e.g., '9' in 1.4e9minutes
    int po = ntu.length() - 1;
    while (po >= 0) {
      if (!Character.isLetter(ntu.charAt(po))) break;
      po--;
    }

    // extract the number
    double results[] = new double[2];
    String num = ntu.substring(0, po + 1);
    results[0] =
        po == -1
            ? 1
            : // 1 if not specified
            String2.parseDouble(num);
    if (!Double.isFinite(results[0]))
      throw new SimpleException(errIn + "invalid number=" + ntu.substring(0, po + 1));

    // extract the timeUnits
    String units = ntu.substring(po + 1).trim();
    results[1] = units.length() == 0 ? 1 : factorToGetSeconds(units); // throws exception

    return results;
  }

  /**
   * This converts an ISO dateTime String to seconds since 1970-01-01T00:00:00Z, rounded to the
   * nearest milli. [Before 2012-05-22, millis were removed. Now they are kept.] In many ways trunc
   * would be better, but doubles are often bruised. round works symmetrically with + and - numbers.
   * If any of the end of the dateTime is missing, a trailing portion of "1970-01-01T00:00:00" is
   * added. The 'T' connector can be any non-digit. This may optionally include hours, minutes,
   * seconds, decimal, and Z or timezone offset (default=Zulu).
   *
   * @param isoString (to millis precision)
   * @return seconds
   * @throws RuntimeException if trouble (e.g., input is null or invalid format)
   */
  public static double isoStringToEpochSeconds(String isoString) {
    return isoStringToMillis(isoString) / 1000.0;
  }

  /** This is like isoStringToEpochSeconds, but returns NaN if trouble. */
  public static double safeIsoStringToEpochSeconds(String isoString) {
    if (isoString == null || isoString.length() < 4) return Double.NaN;
    try {
      return isoStringToMillis(isoString) / 1000.0;
    } catch (Exception e) {
      return Double.NaN;
    }
  }

  /**
   * This converts an EDDTable "now-nUnits" string to epochSeconds. - can also be + or space. n is a
   * positive integer units can be singular or plural or abbreviated.
   *
   * @param nowString e.g., now-4days, case insensitive
   * @return epochSeconds (rounded up to the next second) (or Double.NaN if trouble)
   * @throws SimpleException if trouble
   */
  public static double nowStringToEpochSeconds(String nowString) {
    return zdtToEpochSeconds(nowStringToZdt(nowString));
  }

  private static ZonedDateTime zdtApplyFactor(ZonedDateTime zd, String units, int n, String error) {
    double factor = 1; // default is seconds
    if (units.length() > 0) {
      try {
        factor = factorToGetSeconds(units);
      } catch (Exception e2) {
        throw new SimpleException(error);
      }
    }
    if (factor == 0.001) zd = zd.plusNanos(n * 1000000L);
    else if (factor == 1) zd = zd.plusSeconds(n);
    else if (factor == SECONDS_PER_MINUTE) zd = zd.plusMinutes(n);
    else if (factor == SECONDS_PER_HOUR) zd = zd.plusHours(n);
    else if (factor == SECONDS_PER_DAY) zd = zd.plusDays(n);
    else if (factor == SECONDS_PER_DAY * 7) zd = zd.plusWeeks(n);
    else if (factor == SECONDS_PER_DAY * 30) zd = zd.plusMonths(n);
    else if (factor == SECONDS_PER_DAY * 360) zd = zd.plusYears(n);
    else throw new SimpleException(error);

    return zd;
  }

  /**
   * This converts an EDDTable "now-nUnits" string to epochSeconds. - can also be + or space. n is a
   * positive integer units can be singular or plural or abbreviated.
   *
   * @param nowString e.g., now-4days, case insensitive
   * @return epochSeconds (rounded up to the next second) (or Double.NaN if trouble)
   * @throws SimpleException if trouble
   */
  public static ZonedDateTime nowStringToZdt(String nowString) {

    // now is next second (ms=0)
    ZonedDateTime zd = ZonedDateTime.now(ZoneOffset.UTC);
    zd = zd.plusSeconds(1);
    zd = zd.withNano(0);
    String tError =
        "Query error: Invalid \"now\" constraint: \""
            + nowString
            + "\". "
            + "Timestamp constraints with \"now\" must be in the form "
            + "\"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units).";
    if (nowString == null) throw new SimpleException(tError);
    nowString = nowString.toLowerCase();
    if (!nowString.startsWith("now") || nowString.length() == 4) throw new SimpleException(tError);
    if (nowString.length() == 3) return zd;

    // e.g., now-5hours
    char ch = nowString.charAt(3);
    int start = -1; // trouble
    // non-%encoded '+' will be decoded as ' ', so treat ' ' as equal to '+'
    if (ch == '+' || ch == ' ') start = 4;
    else if (ch == '-') start = 3;
    else throw new SimpleException(tError);

    // find the end of the number
    int end = 4;
    while (nowString.length() > end && String2.isDigit(nowString.charAt(end))) end++;
    // parse the number
    int n =
        String2.parseInt(nowString.substring(start, end) + (end == 4 ? "1" : "")); // if no digits
    if (n == Integer.MAX_VALUE) throw new SimpleException(tError);
    start = end;

    // find the units, adjust gc
    // test sUnits.equals to ensure no junk at end of constraint
    String sUnits = nowString.substring(start).trim();
    zd = zdtApplyFactor(zd, sUnits, n, tError);

    return zd;
  }

  /**
   * This is like nowStringToEpochSeconds, but returns troubleValue if trouble.
   *
   * @param nowString
   * @param troubleValue
   * @return epochSeconds (or troubleValue if trouble)
   */
  public static double safeNowStringToEpochSeconds(String nowString, double troubleValue) {
    try {
      return nowStringToEpochSeconds(nowString);
    } catch (Throwable t) {
      String2.log(t.toString());
      return troubleValue;
    }
  }

  /**
   * This converts an EDDTable "min(varName)-nUnits" or "max(varName)-nUnits" string the resulting
   * value. - can also be + or space. n is a positive floating point number If allowTimeUnits, units
   * is optional (default=seconds) and can be singular or plural and n must be a positive integer.
   *
   * @param mmString presumably, the min(varName) or max(varName) part has already been parsed and
   *     dealt with (see the mmValue param)
   * @param mmValue the variable's min or max value (known because mmString was already partly
   *     parsed).
   * @param allowTimeUnits specify true if var is a timestamp variable
   * @return epochSeconds (rounded up to the next second) (or Double.NaN if trouble)
   * @throws SimpleException if trouble
   */
  public static double parseMinMaxString(String mmString, double mmValue, boolean allowTimeUnits) {

    if (!mmString.startsWith("min(") && !mmString.startsWith("max("))
      throw new SimpleException("Query error: \"min(\" or \"max(\" expected.");
    String mm = mmString.substring(0, 4);

    String tError =
        "Query error: Invalid \""
            + mm
            + ")\" constraint: \""
            + mmString
            + "\". "
            + (allowTimeUnits ? "T" : "Non-t")
            + "imestamp constraints with \""
            + mm
            + ")\" must be in the form "
            + "\""
            + mmString.substring(0, 3)
            + "(varName)[+|-"
            + (allowTimeUnits
                ? "positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units)."
                : "positiveNumber]\".");
    int start = mmString.indexOf(')');
    if (start < 0) throw new SimpleException(tError);
    if (start == mmString.length() - 1) return mmValue;

    start++;
    if (start == mmString.length() - 1)
      throw new SimpleException(tError); // can't be just min(varName)+
    char ch = mmString.charAt(start);
    // non-%encoded '+' will be decoded as ' ', so treat ' ' as equal to '+'
    if (ch == '+' || ch == ' ') start++;
    else if (ch == '-') {
    } else throw new SimpleException(tError);

    // parse the number
    double d = 1;
    int end = start;
    while (end < mmString.length() && "0123456789eE+-.".indexOf(mmString.charAt(end)) >= 0) end++;
    if (end > start) d = String2.parseDouble(mmString.substring(start, end));
    if (Double.isNaN(d)) throw new SimpleException(tError);
    start = end;
    if (start >= mmString.length()) return mmValue += d;

    // test sUnits.equals to ensure no junk at end of constraint
    String sUnits = mmString.substring(start); // it will be something
    if (allowTimeUnits) {
      int n = Math2.roundToInt(d);
      if (n != d) throw new SimpleException(tError);
      ZonedDateTime zd = epochSecondsToZdt(mmValue);
      zd = zdtApplyFactor(zd, sUnits, n, tError);

      mmValue = zdtToEpochSeconds(zd);

    } else { // !allowTimeUnits
      throw new SimpleException(tError);
    }
    return mmValue;
  }

  /**
   * This returns true if the string appears to be an ISO date/time (matching yyyy-M... or
   * uuuu-M..., but a little more sophisticated than that).
   *
   * @param s
   * @return true if the string appears to be an ISO date/time (matching yyyy-M..., or uuuu-M...,
   *     but a little more sophisticated than that).
   */
  public static boolean isIsoDate(String s) {
    if (s == null) return false;
    return ISO_DATE_PATTERN.matcher(s.trim()).matches();
  }

  /**
   * This converts a ZonedDateTime to seconds since 1970-01-01T00:00:00Z. Note that
   * System.currentTimeMillis/1000 = epochSeconds(zulu).
   *
   * @param dt
   * @return seconds, including fractional seconds (Double.NaN if trouble)
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static double zdtToEpochSeconds(ZonedDateTime dt) {
    return dt.toInstant().toEpochMilli() / 1000.0;
  }

  /**
   * This converts seconds since 1970-01-01T00:00:00Z to a ZonedDateTime (UTC timezone).
   *
   * @param seconds (including fractional seconds)
   * @return an iso UTC time-zone ZonedDateTime (rounded to nearest ms)
   * @throws RuntimeException if trouble (e.g., seconds is NaN)
   */
  public static ZonedDateTime epochSecondsToZdt(double seconds) {
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE) {
      Test.error(String2.ERROR + " in epochSecondsToZdt: millis is NaN!");
    }
    return newZdtUtc(millis);
  }

  public static ZonedDateTime newZdtUtc(long millis) {
    if (millis == Long.MAX_VALUE) {
      Test.error(String2.ERROR + " in newZdtUtc: millis valie is Long.MAX_VALUE!");
    }
    Instant instant = Instant.ofEpochMilli(millis);
    return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  /**
   * This converts seconds since 1970-01-01T00:00:00Z to an ISO Zulu dateTime String with 'T'. The
   * doubles are rounded to the nearest millisecond. In many ways trunc would be better, but doubles
   * are often bruised. round works symmetrically with + and - numbers.
   *
   * @param seconds with optional fractional part
   * @return isoZuluString with 'T' and with the trailing Z)
   * @throws RuntimeException if trouble (e.g., seconds is NaN)
   */
  public static String epochSecondsToIsoStringTZ(double seconds) {
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE)
      Test.error(String2.ERROR + " in epochSecondsToIsoStringT: millis is NaN!");
    return millisToIsoStringTZ(millis);
  }

  /**
   * This converts seconds since 1970-01-01T00:00:00Z to an ISO Zulu dateTime String with 'T'.
   *
   * @param seconds with optional fractional part
   * @return isoZuluString with 'T' and with the trailing Z)
   * @throws RuntimeException if trouble (e.g., seconds is NaN)
   */
  public static String epochSecondsToIsoStringTZ(long seconds) {
    return millisToIsoStringTZ(seconds * 1000);
  }

  /** Like epochSecondsToIsoStringTZ, but without the trailing Z. */
  public static String epochSecondsToIsoStringT(double seconds) {
    String s = epochSecondsToIsoStringTZ(seconds);
    return s.substring(0, s.length() - 1);
  }

  /** This is like epochSecondsToIsoStringTZ, but includes millis. */
  public static String epochSecondsToIsoStringT3Z(double seconds) {
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE)
      Test.error(String2.ERROR + " in epochSecondsToIsoStringT3: millis is NaN!");
    return millisToIsoStringT3Z(millis);
  }

  /**
   * This is like epochSecondsToIsoStringT, but add "Z" at end of time, and returns NaNString if
   * seconds is NaN.
   */
  public static String safeEpochSecondsToIsoStringTZ(double seconds, String NaNString) {
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE) return NaNString;
    try {
      return millisToIsoStringTZ(millis);
    } catch (Exception e) {
      return NaNString;
    }
  }

  /**
   * This is like epochSecondsToIsoStringTZ, but showing milliseconds, and returns NaNString if
   * seconds is NaN.
   */
  public static String safeEpochSecondsToIsoStringT3Z(double seconds, String NaNString) {
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE) return NaNString;
    try {
      return millisToIsoStringT3Z(millis);
    } catch (Exception e) {
      return NaNString;
    }
  }

  /**
   * This is like epochSecondsToIsoStringTZ, but showing microseconds, and returns NaNString if
   * seconds is NaN.
   */
  public static String safeEpochSecondsToIsoStringT6Z(double seconds, String NaNString) {
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE) return NaNString;
    try {
      return millisToIsoStringT6Z(millis);
    } catch (Exception e) {
      return NaNString;
    }
  }

  /**
   * This is like epochSecondsToIsoStringTZ, but showing nanoseconds, and returns NaNString if
   * seconds is NaN.
   */
  public static String safeEpochSecondsToIsoStringT9Z(double seconds, String NaNString) {
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE) return NaNString;
    try {
      return millisToIsoStringT9Z(millis);
    } catch (Exception e) {
      return NaNString;
    }
  }

  /** This formats as date only, and returns NaNString if seconds is NaN. */
  public static String safeEpochSecondsToIsoDateString(double seconds, String NaNString) {
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE) return NaNString;
    try {
      return formatAsISODate(newZdtUtc(millis));
    } catch (Exception e) {
      return NaNString;
    }
  }

  /**
   * This is like safeEpochSecondsToIsoStringT3Z, but returns a limited precision string. This won't
   * throw an exception.
   *
   * @param time_precision can be "1970", "1970-01", "1970-01-01", "1970-01-01T00Z",
   *     "1970-01-01T00:00Z", "1970-01-01T00:00:00Z" (used if time_precision not matched),
   *     "1970-01-01T00:00:00.0Z", ..., "1970-01-01T00:00:00.000000000Z". Or any of those without
   *     "Z". If time_precision ends in Z, the result will too. If time_precision doesn't end in Z,
   *     the result won't end in Z. Note that ERDDAP requires/forces/ensures any format with
   *     hours(min(sec)) to have Z.
   * @param seconds the epochSeconds value
   * @param NaNString the value to return if seconds is not finite or is too big.
   * @return the formatted time string (or NaNString if trouble)
   */
  public static String epochSecondsToLimitedIsoStringT(
      String time_precision, double seconds, String NaNString) {

    // TODO optimization- pass the DateTimeFormatter into this function. It is common to
    // reuse a format within a loop.
    DateTimeFormatter format = timePrecisionToDateTimeFormatter(time_precision);
    if (format == null) {
      format = FORMAT_SECONDZ;
    }
    long millis = Math2.roundToLong(seconds * 1000);
    if (millis == Long.MAX_VALUE) return NaNString;
    try {
      Instant instant = Instant.ofEpochMilli(millis);
      return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).format(format);
    } catch (Exception e) {
      return NaNString;
    }
  }

  /**
   * This returns a ISO-style formatted date string e.g., "2004-01-02" using its current get()
   * values (not influenced by the format's timeZone).
   *
   * @param dt a ZonedDateTime object
   * @return the date in dt, formatted as (for example) "2004-01-02"
   * @throws RuntimeException if trouble (e.g., dt is null)
   */
  public static String formatAsISODate(ZonedDateTime dt) {
    return FORMAT_DAY.format(dt);
  }

  /**
   * This converts a ZonedDateTime object into an ISO-format dateTime string (with 'T' separator:
   * [-]uuuu-MM-ddTHH:mm:ss) using its current get() values (not influenced by the format's
   * timeZone). [was calendarToString]
   *
   * @param dt
   * @return the corresponding dateTime String (without timezone info).
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsISODateTimeT(ZonedDateTime dt) {
    return FORMAT_SECOND.format(dt);
  }

  /** This is like formatAsISODateTimeT(), but with trailing Z. */
  public static String formatAsISODateTimeTZ(ZonedDateTime dt) {
    return FORMAT_SECONDZ.format(dt);
  }

  /**
   * This is like formatAsISODateTimeT, but WITH time zone indicator.
   *
   * @param gc
   * @return the corresponding dateTime String (WITHh timezone info).
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsISODateTimeTTZ(ZonedDateTime dt) {
    return ISO_OFFSET_LOCAL_FORMATTER.format(dt);
  }

  /**
   * Like formatAsISODateTimeTZ, but seconds will have 3 decimal digits.
   *
   * @param dt
   * @return the corresponding dateTime String (with the trailing Z).
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsISODateTimeT3Z(ZonedDateTime dt) {
    return FORMAT_MILLISECOND3Z.format(dt);
  }

  /**
   * Like formatAsISODateTimeTZ, but seconds will have 6 decimal digits.
   *
   * @param dt
   * @return the corresponding dateTime String (with the trailing Z).
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsISODateTimeT6Z(ZonedDateTime dt) {
    return FORMAT_MILLISECOND6Z.format(dt);
  }

  /**
   * Like formatAsISODateTimeTZ, but seconds will have 9 decimal digits.
   *
   * @param dt
   * @return the corresponding dateTime String (with the trailing Z).
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsISODateTimeT9Z(ZonedDateTime dt) {
    return FORMAT_MILLISECOND9Z.format(dt);
  }

  /**
   * This converts a time_precision string into a time format string.
   *
   * @param pre a time_precision string, e.g., "1970-01-01T00Z"
   * @return the corresponding time format string, e.g., "yyyy-MM-dd'T'HHZ", or ISO8601T_FORMAT if
   *     trouble
   */
  public static String timePrecisionToTimeFormat(String pre) {
    if (pre == null) return ISO8601TZ_FORMAT;
    pre = pre.trim();
    if (!pre.startsWith("1970-01")) return ISO8601TZ_FORMAT;
    if (pre.endsWith("Z")) pre = pre.substring(0, pre.length() - 1);
    // this ASSUMES(!) that the characters are as expected and just looks at length of pre
    // ISO8601T9_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS000000";
    String iso =
        String2.replaceAll(
            ISO8601T9Z_FORMAT.substring(0, 31), "'T'", "T"); // remove 'Z' and change T
    iso = iso.substring(0, Math.min(pre.length(), iso.length()));
    iso = String2.replaceAll(iso, "T", "'T'");
    return iso + (iso.length() >= 15 ? "Z" : ""); // if has time (hour or more precise)
  }

  public static DateTimeFormatter timePrecisionToDateTimeFormatter(String time_precision) {
    if (time_precision == null || time_precision.length() == 0) {
      return FORMAT_SECONDZ;
    }
    if (time_precision.equals("1970")) {
      return FORMAT_YEAR;
    }
    if (time_precision.equals("1970-01")) {
      return FORMAT_MONTH;
    }
    if (time_precision.equals("1970-01-01")) {
      return FORMAT_DAY;
    }
    if (time_precision.equals("1970-01-01T00")) {
      return FORMAT_HOUR;
    }
    if (time_precision.equals("1970-01-01T00:00")) {
      return FORMAT_MINUTE;
    }
    if (time_precision.equals("1970-01-01T00:00:00")) {
      return FORMAT_SECOND;
    }
    if (time_precision.equals("1970-01-01T00:00:00.0")) {
      return FORMAT_MILLISECOND;
    }
    if (time_precision.equals("1970-01-01T00:00:00.00")) {
      return FORMAT_MILLISECOND2;
    }
    if (time_precision.equals("1970-01-01T00:00:00.000")) {
      return FORMAT_MILLISECOND3;
    }
    if (time_precision.equals("1970-01-01T00:00:00.0000")) {
      return FORMAT_MILLISECOND4;
    }
    if (time_precision.equals("1970-01-01T00:00:00.00000")) {
      return FORMAT_MILLISECOND5;
    }
    if (time_precision.equals("1970-01-01T00:00:00.000000")) {
      return FORMAT_MILLISECOND6;
    }
    if (time_precision.equals("1970-01-01T00:00:00.0000000")) {
      return FORMAT_MILLISECOND7;
    }
    if (time_precision.equals("1970-01-01T00:00:00.00000000")) {
      return FORMAT_MILLISECOND8;
    }
    if (time_precision.equals("1970-01-01T00:00:00.000000000")) {
      return FORMAT_MILLISECOND9;
    }
    if (time_precision.equals("1970Z")) {
      return FORMAT_YEARZ;
    }
    if (time_precision.equals("1970-01Z")) {
      return FORMAT_MONTHZ;
    }
    if (time_precision.equals("1970-01-01Z")) {
      return FORMAT_DAYZ;
    }
    if (time_precision.equals("1970-01-01T00Z")) {
      return FORMAT_HOURZ;
    }
    if (time_precision.equals("1970-01-01T00:00Z")) {
      return FORMAT_MINUTEZ;
    }
    if (time_precision.equals("1970-01-01T00:00:00Z")) {
      return FORMAT_SECONDZ;
    }
    if (time_precision.equals("1970-01-01T00:00:00.0Z")) {
      return FORMAT_MILLISECONDZ;
    }
    if (time_precision.equals("1970-01-01T00:00:00.00Z")) {
      return FORMAT_MILLISECOND2Z;
    }
    if (time_precision.equals("1970-01-01T00:00:00.000Z")) {
      return FORMAT_MILLISECOND3Z;
    }
    if (time_precision.equals("1970-01-01T00:00:00.0000Z")) {
      return FORMAT_MILLISECOND4Z;
    }
    if (time_precision.equals("1970-01-01T00:00:00.00000Z")) {
      return FORMAT_MILLISECOND5Z;
    }
    if (time_precision.equals("1970-01-01T00:00:00.000000Z")) {
      return FORMAT_MILLISECOND6Z;
    }
    if (time_precision.equals("1970-01-01T00:00:00.0000000Z")) {
      return FORMAT_MILLISECOND7Z;
    }
    if (time_precision.equals("1970-01-01T00:00:00.00000000Z")) {
      return FORMAT_MILLISECOND8Z;
    }
    if (time_precision.equals("1970-01-01T00:00:00.000000000Z")) {
      return FORMAT_MILLISECOND9Z;
    }
    // default
    return FORMAT_SECONDZ;
  }

  /**
   * This is like formatAsISODateTime, but returns a limited precision string.
   *
   * @param time_precision can be "1970", "1970-01", "1970-01-01", "1970-01-01T00Z",
   *     "1970-01-01T00:00Z", "1970-01-01T00:00:00Z" (used if time_precision is null or not
   *     matched), "1970-01-01T00:00:00.0Z", "1970-01-01T00:00:00.00Z", "1970-01-01T00:00:00.000Z".
   *     Versions without 'Z' are allowed here, but ERDDAP requires hours or finer to have 'Z'.
   */
  public static String limitedFormatAsISODateTimeT(String time_precision, ZonedDateTime dt) {
    // TODO optimization- pass the DateTimeFormatter into this function. It is common to
    // reuse a format within a loop.
    DateTimeFormatter format = timePrecisionToDateTimeFormatter(time_precision);
    if (format == null) {
      format = FORMAT_SECONDZ;
    }
    return dt.format(format);
  }

  /**
   * This converts a ZonedDateTime object into an ISO-format dateTime string (with space separator:
   * [-]uuuu-MM-dd HH:mm:ss) using its current get() values (not influenced by the format's
   * timeZone). [was calendarToString]
   *
   * @param gc
   * @return the corresponding dateTime String (without the trailing Z).
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsISODateTimeSpace(ZonedDateTime dt) {
    return FORMAT_ISODate_TIME_SPACE.format(dt);
  }

  private static DateTimeFormatter FORMAT_ESRI =
      DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss 'UTC'");

  /**
   * This converts a ZonedDateTime object into an ESRI dateTime string (YYYY/MM/DD HH:MM:SS UTC)
   * using its current get() values (not influenced by the format's timeZone).
   *
   * @param dt
   * @return the corresponding ESRI dateTime String.
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsEsri(ZonedDateTime dt) {
    return FORMAT_ESRI.format(dt);
  }

  /**
   * This returns a compact formatted [-]uuuuMMddHHmmss string e.g., "20040102030405" using its
   * current get() values (not influenced by the format's timeZone).
   *
   * @param dt a ZonedDateTime object
   * @return the date in gc, formatted as (for example) "20040102030405".
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsCompactDateTime(ZonedDateTime zd) {
    return FORMAT_COMPACT_DATE_TIME.format(zd);
  }

  /** This adds -'s, T, and :'s as needed to a compact datetime (with or without T). */
  public static String expandCompactDateTime(String cdt) {
    if (cdt == null) return "";
    int len = cdt.length();
    StringBuilder sb = new StringBuilder();
    if (len < 4
        || !String2.isDigit(cdt.charAt(0))
        || !String2.isDigit(cdt.charAt(1))
        || !String2.isDigit(cdt.charAt(2))
        || !String2.isDigit(cdt.charAt(3))) return cdt; // unchanged
    sb.append(cdt, 0, 4);
    if (len < 6 || !String2.isDigit(cdt.charAt(4)) || !String2.isDigit(cdt.charAt(5)))
      return sb.toString();
    sb.append("-" + cdt.substring(4, 6));
    if (len < 8 || !String2.isDigit(cdt.charAt(6)) || !String2.isDigit(cdt.charAt(7)))
      return sb.toString();
    sb.append("-" + cdt.substring(6, 8));

    int tLen = len >= 9 && cdt.charAt(8) == 'T' ? 1 : 0;

    if (len < 10 + tLen
        || !String2.isDigit(cdt.charAt(8 + tLen))
        || !String2.isDigit(cdt.charAt(9 + tLen))) return sb.toString();
    sb.append("T" + cdt.substring(8 + tLen, 10 + tLen));
    if (len < 12 + tLen
        || !String2.isDigit(cdt.charAt(10 + tLen))
        || !String2.isDigit(cdt.charAt(11 + tLen))) return sb.toString();
    sb.append(":" + cdt.substring(10 + tLen, 12 + tLen));
    if (len < 14 + tLen
        || !String2.isDigit(cdt.charAt(12 + tLen))
        || !String2.isDigit(cdt.charAt(13 + tLen))) return sb.toString();
    sb.append(":" + cdt.substring(12 + tLen)); // to the end, e.g., time zone? Z?

    return sb.toString();
  }

  private static DateTimeFormatter FORMAT_DDMonYYYY =
      DateTimeFormatter.ofPattern("dd-MMM-uuuu HH:mm:ss");

  /**
   * This returns a DD-Mon-[-]uuuu string e.g., "31-Jul-2004 00:00:00" using its current get()
   * values (not influenced by the format's timeZone). Ferret often uses this format.
   *
   * @param dt a ZonedDateTime object
   * @return the date in dt, formatted as (for example) "31-Jul-2004 00:00:00".
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsDDMonYYYY(ZonedDateTime dt) {
    return FORMAT_DDMonYYYY.format(dt);
  }

  private static DateTimeFormatter FORMAT_US_SLASH_AMPM =
      DateTimeFormatter.ofPattern("M/d/uuuu h:mm:ss a");

  /**
   * This returns a US-style slash format date time string ("1/20/2006 9:00:00 pm").
   *
   * @param dt a ZonedDateTime object. The dateTime will be interpreted as being in dt's time zone.
   * @return gc in the US slash format ("1/20/2006 9:00:00 pm").
   * @throws RuntimeException if trouble (e.g., gc is null)
   */
  public static String formatAsUSSlashAmPm(ZonedDateTime dt) {
    return FORMAT_US_SLASH_AMPM.format(dt);
  }

  private static DateTimeFormatter RFC822GMT =
      DateTimeFormatter.ofPattern("EEE, dd MMM uuuu HH:mm:ss 'GMT'");

  /**
   * This returns an RFC 822 format date time string ("Sun, 06 Nov 1994 08:49:37 GMT").
   *
   * @param dt a ZonedDateTime object. The dateTime will be interpreted as being in the gc's time
   *     zone (which should always be GMT because "GMT" is put at the end).
   * @return gc in the RFC 822 format ("Sun, 06 Nov 1994 08:49:37 GMT").
   * @throws RuntimeException if trouble (e.g., dt is null)
   */
  public static String formatAsRFC822GMT(ZonedDateTime dt) {
    return RFC822GMT.format(dt);
  }

  /**
   * This parses n int values from s and stores results in resultsN (or leaves items in resultsN
   * untouched if no value available).
   *
   * @param s the date time string
   * @param separatorN is the separators (use "\u0000" to match any non-digit). (± matches + or -
   *     and that becomes part of the number) (. matches . or , (the European decimal point))
   * @param resultsN should initially have the defaults and will receive the results. If trouble,
   *     resultsN[0] will be Integer.MAX_VALUE, so caller can throw exception with good error
   *     message. Timezone hours and minutes will both be + or -, as indicated by sign in s.
   */
  private static void parseN(String s, char separatorN[], int resultsN[]) {
    // String2.log(">> parseN " + s);
    // ensure s starts with a digit
    if (s == null) s = "";
    s = s.trim();
    int sLength = s.length();
    if (sLength < 1 || !(s.charAt(0) == '-' || String2.isDigit(s.charAt(0)))) {
      resultsN[0] = Integer.MAX_VALUE;
      return;
    }
    int po1, po2 = -1;
    int pmFactor = 1;
    // String2.log("parseN " + s);

    // search for digits, non-digit.   "1970-01-01T00:00:00.000-01:00"
    boolean mMode =
        s.charAt(0) == '-'; // initial '-' is required and included when evaluating number
    int nParts = separatorN.length;
    for (int part = 0; part < nParts; part++) {
      if (po2 + 1 < sLength) {
        // accumulate digits
        po1 = po2 + 1;
        po2 = po1;
        if (mMode) {
          if (po2 < sLength && s.charAt(po2) == '-') po2++;
          else {
            resultsN[0] = Integer.MAX_VALUE;
            return;
          }
        }
        while (po2 < sLength && String2.isDigit(s.charAt(po2))) po2++; // digit

        // if no number, return; we're done
        // String2.log(">> nChar=" + (po2-po1) + " sub=\"" + s.substring(po1, po2) + "\"");
        if (po2 == po1) return;
        if (part > 0 && separatorN[part - 1] == '.') {
          // this works for any number of digits 1-9 (and more), but standardizes (truncates) result
          // to millis
          resultsN[part] =
              Math2.truncToInt(1000 * String2.parseDouble("0." + s.substring(po1, po2)));
          // String2.log("  millis=" + resultsN[part]);
        } else if (part > 0
            && separatorN[part - 1] == '±'
            && separatorN[part] == ':'
            && po2 - po1 >= (s.charAt(po1) == '-' ? 1 : 0) + 3) { // 4 or 5 characters
          // deal with timezones like -0830 and -830
          int ti = String2.parseInt(s.substring(po1, po2));
          resultsN[part] = ti / 100; // as expected: -830/100 = -8
          resultsN[part + 1] = ti % 100; // as expected: -830%100 = -30
          part += 1; //
          // String2.log("  millis=" + resultsN[part]);
        } else if (part > 1 && separatorN[part - 2] == '±' && separatorN[part - 1] == ':') {
          // minutes part of time zone
          resultsN[part] = String2.parseInt(s.substring(po1, po2));
          if (resultsN[part] != Integer.MAX_VALUE)
            resultsN[part] *= pmFactor; // give it the correct sign
        } else {
          resultsN[part] = String2.parseInt(s.substring(po1, po2));
          if (part > 0 && separatorN[part - 1] == '±') pmFactor = s.charAt(po1) == '-' ? -1 : 1;
        }

        // if invalid number, return trouble
        if (resultsN[part] == Integer.MAX_VALUE) {
          resultsN[0] = Integer.MAX_VALUE;
          return;
        }

        // if no more source characters, we're done
        if (po2 >= sLength) {
          // String2.log("  " + String2.toCSSVString(resultsN));
          return;
        }

        // if invalid separator, stop trying to read more; return trouble
        mMode = false;
        char ch = s.charAt(po2);
        if (ch == ',') ch = '.';
        if (separatorN[part] == '\u0000') {

        } else if (separatorN[part] == '±') {
          if (ch == '+') { // do nothing
          } else if (ch == '-') {
            po2--; // number starts with -
            mMode = true;
          } else {
            resultsN[0] = Integer.MAX_VALUE;
            return;
          }

        } else if (ch != separatorN[part]) { // if not exact match ...

          // if current part is ':' or '.' and not matched, try to skip forward to '±'
          if ((separatorN[part] == ':' || separatorN[part] == '.') && part < nParts - 1) {
            int pmPart = String2.indexOf(separatorN, '±', part + 1);
            if (pmPart >= 0) {
              // String2.log("  jump to +/-");
              part = pmPart;
              if (ch == '+') { // do nothing
              } else if (ch == '-') {
                po2--; // number starts with -
                mMode = true;
              } else {
                resultsN[0] = Integer.MAX_VALUE;
                return;
              }
              continue;
            } // if < 0, fall through to failure
          }
          resultsN[0] = Integer.MAX_VALUE;
          // String2.log("  " + String2.toCSSVString(resultsN));
          return;
        }
      }
    }
    // String2.log("  " + String2.toCSSVString(resultsN));
  }

  /**
   * This converts an ISO (default *UTC* time zone) date time string (variations of
   * [-]uuuu-MM-ddTHH:mm:ss.SSS±XX:XX) into a ZonedDateTime object with the UTC time zone. See
   * parseISODateTime documentation.
   *
   * @param s the dateTimeString in the ISO format (variations of [-]uuuu-MM-ddTHH:mm:ss) This may
   *     include hours, minutes, seconds, decimal, and Z or timezone offset (default=Zulu).
   * @return a ZonedDateTime object
   * @throws RuntimeException if trouble (e.g., s is null or not at least #)
   */
  public static ZonedDateTime parseISODateTimeUtc(String s) {
    return parseISODateTime(s, ZoneOffset.UTC);
  }

  /**
   * This converts an ISO date time string (variations of [-]uuuu-MM-ddTHH:mm:ss.SSS±XX:XX) into a
   * ZonedDateTime object. <br>
   * It is lenient; so Jan 32 is converted to Feb 1; <br>
   * The 'T' may be any non-digit. <br>
   * The time zone can be omitted. <br>
   * The parts at the end of the time can be omitted. <br>
   * If there is no time, the end parts of the date can be omitted. Year is required. <br>
   * This tries hard to be tolerant of non-valid formats (e.g., no lead 0 "1971-1-2", or just
   * year+month "1971-01") <br>
   * As of 11/9/2006, NO LONGER TRUE: If year is 0..49, it is assumed to be 2000..2049. <br>
   * As of 11/9/2006, NO LONGER TRUE: If year is 50..99, it is assumed to be 1950..1999. <br>
   * If the string is too short, the end of "1970-01-01T00:00:00.000Z" will be added (effectively).
   * <br>
   * If the string is too long, the excess will be ignored. <br>
   * If a required separator is incorrect, it is an error. <br>
   * If the date is improperly formatted, it returns null. <br>
   * Timezone "Z" or "" is treated as "-00:00" (UTC/Zulu time) <br>
   * Timezones: e.g., 2007-01-02T03:04:05-01:00 is same as 2007-01-02T04:04:05
   *
   * @param zt a TimeZone object. Timezone info is relative to the tz's time zone.
   * @param s the dateTimeString in the ISO format (variations of uuuu-MM-ddTHH:mm:ss.SSS±XX:XX
   *     where uuuu is an astronomical year) For years B.C., use calendar2Year = 1 - BCYear. Note
   *     that BCYears are 1..., so 1 BC is calendar2Year 0 (or 0000), and 2 BC is calendar2Year -1
   *     (or -0001). This supports ss.SSS and ss,SSS (which ISO 8601 prefers!).
   * @return the same ZonedDateTime object, but with the date info
   * @throws RuntimeException if trouble (e.g., gc is null or s is null or not at least #)
   */
  public static ZonedDateTime parseISODateTime(String s, ZoneId tz) {
    if (s == null) s = "";
    s = s.trim();
    if ("nd".equals(s)) return null;
    boolean negative = s.startsWith("-");
    if (negative) s = s.substring(1);
    if (s.length() < 1 || !String2.isDigit(s.charAt(0)))
      Test.error(
          String2.ERROR
              + " in parseISODateTime: for first character of dateTime='"
              + s
              + "' isn't a digit!");

    // default ymdhmsmom     year is the only required value
    int ymdhmsmom[] = {Integer.MAX_VALUE, 1, 1, 0, 0, 0, 0, 0, 0};

    // remove trailing Z or "UTC"
    s = s.trim();
    if (Character.toLowerCase(s.charAt(s.length() - 1)) == 'z')
      s = s.substring(0, s.length() - 1).trim();
    if (s.length() >= 3) {
      String last3 = s.substring(s.length() - 3).toLowerCase();
      if (last3.equals("utc") || last3.equals("gmt")) s = s.substring(0, s.length() - 3).trim();
    }

    // if e.g., 1970-01-01 00:00:00 0:00, change ' ' to '+' (first ' '->'+' is irrelevant)
    s = String2.replaceAll(s, ' ', '+');

    // separators (\u0000=any non-digit)
    char separator[] = {'-', '-', '\u0000', ':', ':', '.', '±', ':', '\u0000'};
    parseN(s, separator, ymdhmsmom);
    if (ymdhmsmom[0] == Integer.MAX_VALUE)
      Test.error(
          String2.ERROR + " in parseISODateTime: dateTime='" + s + "' has an invalid format!");

    // do time zone adjustment
    // String2.log(">> #7=" + ymdhmsmom[7] + " #8=" + ymdhmsmom[8]);
    if (ymdhmsmom[7] != 0) ymdhmsmom[3] -= ymdhmsmom[7];
    if (ymdhmsmom[8] != 0)
      ymdhmsmom[4] -= ymdhmsmom[8]; // parseN returns appropriately signed value

    ZonedDateTime zd =
        ZonedDateTime.of((negative ? -1 : 1) * ymdhmsmom[0], 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    // Use Plus to handle things like too many days in the month.
    zd =
        zd.plusMonths(ymdhmsmom[1] - 1)
            .plusDays(ymdhmsmom[2] - 1)
            .plusHours(ymdhmsmom[3])
            .plusMinutes(ymdhmsmom[4])
            .plusSeconds(ymdhmsmom[5])
            .plusNanos(ymdhmsmom[6] * 1000000L);

    zd = zd.withZoneSameLocal(tz);
    return zd;
  }

  /**
   * This converts a non-ISO (default *ZULU* time zone) date time string into a ZonedDateTime object
   * with the UTC time zone. See parseDateTime documentation.
   *
   * @param s the dateTimeString in the specified format This may include hours, minutes, seconds,
   *     decimal, and Z or timezone offset (default=Zulu).
   * @param format a Java DateTimeFormatter format string.
   * @return a ZonedDateTime object
   * @throws RuntimeException if trouble (e.g., s is null)
   */
  public static ZonedDateTime parseDateTimeZulu(String s, String format) {
    return parseDateTime(s, format, ZoneOffset.UTC);
  }

  /**
   * This converts a non-ISO (default *ZULU* time zone) date time string into a ZonedDateTime object
   * with the UTC time zone. See parseDateTime documentation.
   *
   * @param s the dateTimeString in the specified format This may include hours, minutes, seconds,
   *     decimal, and Z or timezone offset (default=Zulu).
   * @param format a Java DateTimeFormatter format string.
   * @return a ZonedDateTime object
   * @throws RuntimeException if trouble (e.g., s is null)
   */
  public static ZonedDateTime parseDateTimeUtc(String s, String format) {
    return ZonedDateTime.parse(s, DateTimeFormatter.ofPattern(format));
  }

  //    public static boolean parseWithCalendar2IsoParser(String format) {
  //        if (format == null)
  //            return false;
  //        return
  //            format.startsWith("uuuu-M") ||
  //            format.startsWith("yyyy-M") ||
  //            format.startsWith("YYYY-M");  //Y is discouraged/incorrect
  //    }

  /**
   * This parses as ISO or non-ISO format.
   *
   * @throws RuntimeException if trouble
   */
  public static long formattedStringToMillis(String s, String format, ZoneId zoneId) {
    if (zoneId == null) {
      zoneId = ZoneOffset.UTC;
    }
    return parseDateTime(s, format, zoneId).toInstant().toEpochMilli();
  }

  private static String parseError(String s, String format) {
    return String2.ERROR
        + ": parseDateTime was unable to parse \""
        + s
        + "\" as \""
        + format
        + "\": ";
  }

  private static String parseErrorUnexpectedEndOfContent(String s, String format) {
    return parseError(s, format) + "The time string lacks information specified at end of format.";
  }

  /**
   * Generate an error message for parseDateTime.
   *
   * @param sPo is 0.. but is printed as 1..
   */
  private static String parseErrorUnexpectedContent(String s, String format, int sPo) {
    return parseError(s, format)
        + "The time string doesn't match the format at time string character #"
        + (sPo + 1)
        + ".";
  }

  /**
   * Generate an error message for parseDateTime.
   *
   * @param formatPo is 0.. but is printed as 1..
   */
  private static String parseErrorUnexpectedFormat(String s, String format, char ch, int formatPo) {
    return parseError(s, format)
        + "Unexpected or unsupported format character '"
        + ch
        + "' at #"
        + (formatPo + 1)
        + ".";
  }

  /**
   * Generate an error message for parseDateTime.
   *
   * @param formatPo is 0.. but is printed as 1..
   */
  private static String parseErrorUnexpectedCount(
      String s, String format, int nCh, char ch, int formatPo) {
    return parseError(s, format)
        + "Unexpected or unsupported format character count: '"
        + String2.makeString(ch, nCh)
        + "' at #"
        + (formatPo + 1)
        + ".";
  }

  /**
   * @param s the source dateTimeString.
   * @param format A DateTimeFormatter-style specification, e.g., yyyy-DDD. Note that yyyy is
   *     processed as uuuu, so year 0 and before are supported.
   * @param timeZone the time zone to use for the ZonedDateTime.
   * @return A ZonedDateTime object with the date info
   * @throws RuntimeException if trouble (e.g., any input param is null, or s doesn't exactly match
   *     the format.
   */
  public static ZonedDateTime parseDateTime(String s, String format, ZoneId timeZone) {
    // Ideally this would just be the below.
    // TemporalAccessor parsed = DateTimeFormatter.ofPattern(format).withZone(timeZone).parse(s);
    // if (!parsed.isSupported(ChronoField.SECOND_OF_MINUTE)) {
    //     LocalDate date = LocalDate.from(parsed);
    //     return date.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(timeZone);
    // }
    // return ZonedDateTime.from(parsed);
    // However there's a lot of history of lenient date time parsing in the project (including
    // by project code, not just data). So we do the legacy manual parsing.
    int sLength = s.length();
    int formatLength = format.length();
    int sPo = 0; // next to be read
    int formatPo = 0; // next to be read
    boolean literalMode = false; // e.g., 'UTC'
    boolean optionalMode = false; // e.g., [ ]
    ZonedDateTime dt = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    // go through formatPo to find next item to match
    while (formatPo >= 0 && formatPo < formatLength) {
      // what is the next format character?
      int oFormatPo = formatPo;
      char ch = format.charAt(formatPo++);
      while (formatPo < formatLength && format.charAt(formatPo) == ch) formatPo++;
      int nCh = formatPo - oFormatPo;
      // String2.log(">> ch=" + ch + " n=" + nCh + " " + gcToEpochSeconds(gc));

      // deal with literal mode first
      if (ch == '\'') {
        while (nCh >= 2) {
          if (optionalMode) {
            // character may match '
            if (sPo < sLength && s.charAt(sPo) == '\'') sPo++;
          } else {
            // character must match '
            if (sPo >= sLength)
              throw new RuntimeException(parseErrorUnexpectedEndOfContent(s, format));
            else if (s.charAt(sPo) == '\'') sPo++;
            else throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
          }
          nCh -= 2;
        }
        if (nCh == 1) literalMode = !literalMode;

      } else if (literalMode) {
        for (int i = 0; i < nCh; i++) {
          if (optionalMode) {
            // character may match
            if (sPo < sLength && s.charAt(sPo) == ch) sPo++;
          } else {
            // character must match
            if (sPo >= sLength)
              throw new RuntimeException(parseErrorUnexpectedEndOfContent(s, format));
            else if (s.charAt(sPo) == ch) sPo++;
            else throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
          }
        }

        // turn optionalMode on/off
      } else if (ch == '[') {
        if (nCh > 1)
          throw new RuntimeException(parseErrorUnexpectedCount(s, format, nCh, ch, formatPo - nCh));
        else if (optionalMode)
          throw new RuntimeException(parseError(s, format) + "'[' inside [] isn't allowed.");
        optionalMode = true;
      } else if (ch == ']') {
        if (!optionalMode || nCh > 1)
          throw new RuntimeException(parseError(s, format) + "']' found without matching '['.");
        optionalMode = false;

      } else if (nCh >= 3 && (ch == 'M' || ch == 'E')) {
        // get month or day-of-week text
        // ???deal properly with nCh (see spec)
        // ??? L?
        int ospo = sPo;
        while (sPo < sLength && String2.isAsciiLetter(s.charAt(sPo))) sPo++;
        String s2 = String2.toTitleCase(s.substring(ospo, sPo));
        if (nCh > 4) {
          throw new RuntimeException(parseErrorUnexpectedCount(s, format, nCh, ch, formatPo - nCh));

        } else if (ch == 'M') {
          // MMM and MMMM support 3-letter or full length
          int i =
              // both are titleCase
              (s2.length() == 3 ? MONTH_3 : MONTH_FULL).indexOf(s2);
          // String2.log(">>    i=" + i);
          if (i >= 0) {
            dt = dt.withMonth(i + 1); // month is 0..
          } else throw new RuntimeException(parseErrorUnexpectedContent(s, format, ospo));

        } else if (ch == 'E') {
          // EEE and EEEE support 3-letter or full length
          int i =
              // both are titleCase
              (s2.length() == 3 ? DAY_OF_WEEK_3 : DAY_OF_WEEK_FULL).indexOf(s2);
          // don't use it, just ensure it matches a valid value
          if (i < 1) // [0]=""
          throw new RuntimeException(parseErrorUnexpectedContent(s, format, ospo));
        }

      } else if (ch == 'a') {
        // am/pm
        if (nCh > 1) {
          throw new RuntimeException(parseErrorUnexpectedCount(s, format, nCh, ch, formatPo - nCh));

        } else if (sPo < sLength - 1) {
          String valS = s.substring(sPo, sPo + 2).toLowerCase();
          if (valS.equalsIgnoreCase("am")) {
            // Do Nothing, the hour is set properly.
            sPo += 2;

          } else if (valS.equalsIgnoreCase("pm")) {
            // Add 12 hours to shift the time to afternoon.
            dt = dt.plusHours(12);
            sPo += 2;

            // optional mode not allowed
          } else {
            throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
          }

        } else {
          throw new RuntimeException(parseErrorUnexpectedEndOfContent(s, format));
        }

      } else if (ch == 'x' || ch == 'X' || ch == 'Z') {
        // time zone
        if (nCh > 3) {
          throw new RuntimeException(parseErrorUnexpectedCount(s, format, nCh, ch, formatPo - nCh));

          // X (and non-standard Z) allows 'Z'.   If content is 'Z', we're done
        } else if ((ch == 'X' || ch == 'Z') && sPo < sLength && s.charAt(sPo) == 'Z') {
          sPo++;

        } else { // for nCh = 1 (+08), 2 (+0800), 3 (+08:00)  but also allow +8  or no +-
          // non-standard treat Z as xxx
          if (ch == 'Z') {
            ch = 'x';
            nCh = 3;
          }
          // read +/-HH (or H, improper)
          if (sPo + 2 >= sLength) // ensure 2+ valid char
          throw new RuntimeException(parseErrorUnexpectedEndOfContent(s, format));
          // eat sp/+/-
          int factor = 1;
          char ch2 = s.charAt(sPo);
          if (ch2 == ' ' || ch2 == '+') {
            sPo++;
          } else if (ch2 == '-') {
            sPo++;
            factor = -1;
          }
          int nDigits = 0;
          while (sPo + nDigits < sLength && String2.isDigit(s.charAt(sPo + nDigits))) nDigits++;
          if (nDigits == 0) throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
          int HH = 0;
          int mm = 0;
          if (nCh == 1) { // x   8, 08
            if (nDigits > 2) nDigits = 2;
            HH = factor * String2.parseInt(s.substring(sPo, sPo + nDigits));
            sPo += nDigits;
          } else if (nCh == 2) { // xx   800, 0800
            if (nDigits < 3)
              throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
            if (nDigits == 3) { // 800
              HH = factor * String2.parseInt(s.substring(sPo, sPo + 1));
              mm = factor * String2.parseInt(s.substring(sPo + 1, sPo + 3));
            } else if (nDigits >= 4) { // 0800
              nDigits = 4;
              HH = factor * String2.parseInt(s.substring(sPo, sPo + 2));
              mm = factor * String2.parseInt(s.substring(sPo + 2, sPo + 4));
            }
            sPo += nDigits;
          } else if (nCh == 3) { // xxx  8:00, 08:00
            if (nDigits > 2) nDigits = 2;
            HH = factor * String2.parseInt(s.substring(sPo, sPo + nDigits));
            sPo += nDigits;

            // next must be colon
            if (sPo >= sLength || s.charAt(sPo) != ':')
              throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
            sPo++;

            // next must be 2-digit minutes
            if (sPo + 1 >= sLength
                || !String2.isDigit(s.charAt(sPo))
                || !String2.isDigit(s.charAt(sPo + 1)))
              throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
            mm = factor * String2.parseInt(s.substring(sPo, sPo + 2));
            sPo += 2;
          }

          dt = dt.withZoneSameLocal(ZoneOffset.ofHoursMinutes(HH, mm));
          timeZone = null;
        }

        // parse as long int
      } else if (ch == 'n' || ch == 'N') {

        // get digits
        int ospo = sPo;
        if (nCh == 1) {
          // get as many digits as possible
          while (sPo < sLength && String2.isDigit(s.charAt(sPo))) sPo++;
          if (sPo - ospo == 0)
            throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));

        } else {
          // get specified number of digits
          for (int i = 0; i < nCh; i++) {
            if (sPo >= sLength)
              throw new RuntimeException(parseErrorUnexpectedEndOfContent(s, format));
            else if (!String2.isDigit(s.charAt(sPo)))
              throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
            sPo++;
          }
        }
        String vals = s.substring(ospo, sPo);
        long valL = String2.parseLong(vals);
        if (valL == Long.MAX_VALUE)
          throw new RuntimeException(parseErrorUnexpectedContent(s, format, ospo));
        dt = dt.plusNanos(valL);

        // all other letters  a-zA-Z, parse at int
      } else if (String2.isAsciiLetter(ch)) {

        // specifically catch unsupported format letters that read strings
        if ("GLQcVzOp".indexOf(ch) >= 0) {
          // currently unsupported format characters
          throw new RuntimeException(parseErrorUnexpectedFormat(s, format, ch, oFormatPo));
        }

        // if uYy, get optional starting '-'
        int factor = 1;
        if ("uYy".indexOf(ch) >= 0 && sPo < sLength && s.charAt(sPo) == '-') {
          factor = -1;
          sPo++;
        }

        // get digits
        int ospo = sPo;
        if (nCh == 1) {
          // get as many digits as possible
          while (sPo < sLength && String2.isDigit(s.charAt(sPo))) sPo++;
          if (sPo - ospo == 0)
            throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));

        } else {
          // get specified number of digits
          for (int i = 0; i < nCh; i++) {
            if (sPo >= sLength)
              throw new RuntimeException(parseErrorUnexpectedEndOfContent(s, format));

            char tch = s.charAt(sPo);
            if (tch == ' '
                && i == 0
                && nCh == 2
                && "dHh".indexOf(ch) >= 0) { // first 'digit' of dd, HH, hh may be a space
              ospo++;
            } else if (!String2.isDigit(tch)) {
              throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
            }
            sPo++;
          }
        }
        String vals = s.substring(ospo, sPo);
        int val = String2.parseInt(vals);
        if (val == Integer.MAX_VALUE)
          throw new RuntimeException(parseErrorUnexpectedContent(s, format, ospo));
        if ("uYy".indexOf(ch) >= 0) {
          dt = dt.plusYears(factor * val - 1970);
        } else if (ch == 'M') {
          dt = dt.plusMonths(val - 1);
        } else if (ch == 'm') dt = dt.plusMinutes(val);
        else if (ch == 'D') {
          dt = dt.plusDays(val - 1);
        } else if (ch == 'd') {
          dt = dt.plusDays(val - 1);
        } else if (ch == 'H') {
          dt = dt.plusHours(val);
        } else if (ch == 'h') {
          dt = dt.plusHours(val == 12 ? 0 : val); // clock am/pm hour, 12 -> 0
        } else if (ch == 'K') {
          dt = dt.plusHours(val); // am/pm hour
        } else if (ch == 's') dt = dt.plusSeconds(val);
        else if (ch == 'S') {
          // fraction of a second
          int valsl = vals.length();
          // int plus = 0;
          if (valsl == 0) vals = "0";
          else if (valsl == 1) vals += "00";
          else if (valsl == 2) vals += "0";
          else if (valsl > 3) {
            // truncate to millis
            vals = vals.substring(0, 3);

            // round to millis (debatable)
            // char ch2 = vals.charAt(3);
            // if (ch2 >= '5' && ch2 <= '9')
            //    plus = 1;
            // vals = vals.substring(0, 3);
          }
          val = String2.parseInt(vals); // + plus;
          // String2.log(">> S format=" + format + " vals=" + vals + " val=" + val);
          if (val == Integer.MAX_VALUE)
            throw new RuntimeException(parseErrorUnexpectedContent(s, format, ospo));
          dt = dt.plusNanos(val * 1000000L);

        } else if (ch == 'A') {
          dt = dt.plusNanos(val * 1000000L);
        } else throw new RuntimeException(parseErrorUnexpectedFormat(s, format, ch, oFormatPo));

      } else if ("{}#".indexOf(ch) >= 0) {
        // currently unsupported format characters
        throw new RuntimeException(parseErrorUnexpectedFormat(s, format, ch, oFormatPo));

      } else {
        // match other chars as literals, e.g., ' '
        for (int i = 0; i < nCh; i++) {
          if (optionalMode) {
            // character may match
            if (sPo < sLength && s.charAt(sPo) == ch) sPo++;
          } else {
            // character must match
            if (sPo >= sLength)
              throw new RuntimeException(parseErrorUnexpectedEndOfContent(s, format));
            else if (s.charAt(sPo) != ch)
              throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));
            sPo++;
          }
        }
      }
    }
    if (sPo != sLength) throw new RuntimeException(parseErrorUnexpectedContent(s, format, sPo));

    if (timeZone != null) {
      dt = dt.withZoneSameLocal(timeZone);
    }
    return dt;
  }

  /**
   * This converts compact string (must be [-]uuuuMMdd, [-]uuuuMMddHH, [-]uuuuMMddHHmm, or
   * [-]uuuuMMddHHmmss) into a ZonedDateTime object. It is lenient; so Jan 32 is converted to Feb 1.
   * If the date is improperly formatted, it returns null.
   *
   * @param s dateTimeString in compact format (must be [-]uuuuMMdd, [-]uuuuMMddHH, [-]uuuuMMddHHmm,
   *     or [-]uuuuMMddHHmmss)
   * @return the same ZonedDateTime object, but with the date info
   * @throws RuntimeException if trouble (e.g., gc is null or s is null or not at least YYYYMMDD)
   */
  public static ZonedDateTime parseCompactDateTime(String s) {

    // ensure it has at least 8 characters, and all characters are digits
    if (s == null) s = "";
    boolean negative = s.startsWith("-");
    if (negative) s = s.substring(1);
    int sLength = s.length();
    if (sLength < 8)
      Test.error(String2.ERROR + " in parseCompactDateTime: s=" + s + " has an invalid format!");
    for (int i = 0; i < sLength; i++)
      if (!String2.isDigit(s.charAt(i)))
        Test.error(String2.ERROR + " in parseCompactDateTime: s=" + s + " has an invalid format!");

    s += String2.makeString('0', 14 - sLength);
    try {
      ZonedDateTime dateTime = ZonedDateTime.parse(s, FORMAT_COMPACT_DATE_TIME);
      return dateTime;
    } catch (Exception e) {
      ZonedDateTime dt =
          ZonedDateTime.of(
              (negative ? -1 : 1) * String2.parseInt(s.substring(0, 4)),
              1,
              1,
              0,
              0,
              0,
              0,
              ZoneOffset.UTC);
      dt = dt.plusMonths(String2.parseInt(s.substring(4, 6)) - 1);
      dt = dt.plusDays(String2.parseInt(s.substring(6, 8)) - 1);
      dt = dt.plusHours(String2.parseInt(s.substring(8, 10)));
      dt = dt.plusMinutes(String2.parseInt(s.substring(10, 12)));
      dt = dt.plusSeconds(String2.parseInt(s.substring(12, 14)));
      return dt;
    }
  }

  /**
   * This converts a dd-MMM-[-]uuuu string e.g., "31-Jul-2004 00:00:00" into a ZonedDateTime object.
   * It is lenient; so day 0 is converted to Dec 31 of previous year. If the date is shortenend,
   * this does the best it can, or returns null. Ferret often uses this format.
   *
   * @param gc a ZonedDateTime object. The dateTime will be interpreted as being in gc's time zone.
   * @param s dateTimeString in dd-MMM-uuuu format. The time part can be shorter or missing.
   * @return the same ZonedDateTime object, but with the date info
   * @throws RuntimeException if trouble (e.g., gc is null or s is null or not dd-MMM-uuuu)
   */
  public static ZonedDateTime parseDDMonYYYY(String s) {

    if (s == null) s = "";
    int sLength = s.length();
    boolean negative = sLength >= 8 && s.charAt(7) == '-';
    if (negative) s = s.substring(0, 7) + s.substring(8);
    if (sLength < 11
        || !String2.isDigit(s.charAt(0))
        || !String2.isDigit(s.charAt(1))
        || s.charAt(2) != '-'
        || s.charAt(6) != '-'
        || !String2.isDigit(s.charAt(7))
        || !String2.isDigit(s.charAt(8))
        || !String2.isDigit(s.charAt(9))
        || !String2.isDigit(s.charAt(10)))
      Test.error(String2.ERROR + " in parseDDMonYYYY: s=" + s + " has an invalid format!");

    int hour = 0, min = 0, sec = 0;
    if (sLength >= 13) {
      if (s.charAt(11) != ' ' || !String2.isDigit(s.charAt(12)) || !String2.isDigit(s.charAt(13)))
        Test.error(String2.ERROR + " in parseDDMonYYYY: s=" + s + " has an invalid format!");
      hour = String2.parseInt(s.substring(12, 14));
    }
    if (sLength >= 16) {
      if (s.charAt(14) != ':' || !String2.isDigit(s.charAt(15)) || !String2.isDigit(s.charAt(16)))
        Test.error(String2.ERROR + " in parseDDMonYYYY: s=" + s + " has an invalid format!");
      min = String2.parseInt(s.substring(15, 17));
    }
    if (sLength >= 19) {
      if (s.charAt(17) != ':' || !String2.isDigit(s.charAt(18)) || !String2.isDigit(s.charAt(19)))
        Test.error(String2.ERROR + " in parseDDMonYYYY: s=" + s + " has an invalid format!");
      sec = String2.parseInt(s.substring(18, 20));
    }

    String month = s.substring(3, 6).toLowerCase();
    int mon = 0;
    while (mon < 12) {
      if (MONTH_3.get(mon).toLowerCase().equals(month)) break;
      mon++;
    }
    if (mon == 12)
      Test.error(String2.ERROR + " in parseDDMonYYYY: s=" + s + " has an invalid format!");

    ZonedDateTime dt =
        ZonedDateTime.of(
            (negative ? -1 : 1) * String2.parseInt(s.substring(7, 11)),
            1,
            1,
            0,
            0,
            0,
            0,
            ZoneOffset.UTC);
    dt =
        dt.plusMonths(mon) // dt mon starts at 1, but mon is 0 based
            // dt days starts at 1 and the input is 1 based
            .plusDays(String2.parseInt(s.substring(0, 2)) - 1)
            .plusHours(hour)
            .plusMinutes(min)
            .plusSeconds(sec);
    return dt;
  }

  private static DateTimeFormatter YYYYDDDFormat = DateTimeFormatter.ofPattern("uuuuDDD");

  /**
   * This converts a [-]YYYYDDD string into a ZonedDateTime object. It is lenient; so day 0 is
   * converted to Dec 31 of previous year. If the date is improperly formatted, this does the best
   * it can, or returns null.
   *
   * @param s dateTimeString in YYYYDDD format
   * @return a ZonedDateTime with the date info object, but with the date info
   * @throws RuntimeException if trouble (e.g., gc is null or s is null or not YYYYDDDD)
   */
  public static ZonedDateTime parseYYYYDDD(String s) {
    // ensure it is a string with 7 digits
    if (s == null) s = "";

    if (s.endsWith("000")) {
      boolean negative = s.startsWith("-");
      if (negative) s = s.substring(1);
      int sLength = s.length();
      if (sLength != 7)
        Test.error(String2.ERROR + " in parseYYYYDDD: s=" + s + " has an invalid format!");
      for (int i = 0; i < sLength; i++)
        if (!String2.isDigit(s.charAt(i)))
          Test.error(String2.ERROR + " in parseYYYYDDD: s=" + s + " has an invalid format!");
      ZonedDateTime zd =
          ZonedDateTime.of(
              (negative ? -1 : 1) * String2.parseInt(s.substring(0, 4)),
              1,
              1,
              0,
              0,
              0,
              0,
              ZoneOffset.UTC);
      // day 000 is invalid in DateTimeFormatter, but we want to support it as the last day of the
      // previous year,
      // so do -1 day.
      return zd.minusDays(1);
    }

    LocalDate ld = LocalDate.parse(s, YYYYDDDFormat);
    ZonedDateTime zd = ld.atStartOfDay(ZoneOffset.UTC);

    return zd;
  }

  /**
   * This is like parseYYYYDDD, but assumes the time zone is Zulu.
   *
   * @throws RuntimeException if trouble (e.g., s is null or not YYYYDDD)
   */
  public static ZonedDateTime parseYYYYDDDZulu(String s) {
    return parseYYYYDDD(s);
  }

  /**
   * Convert a String with [-]uuuuDDD to a String with YYYY-mm-dd. This works the same for Local or
   * Zulu or other time zones.
   *
   * @param s a String with a date in the form yyyyddd
   * @return the date formatted as YYYY-mm-dd
   * @throws RuntimeException if trouble (e.g., s is null or not YYYYDDD)
   */
  public static String yyyydddToIsoDate(String s) {
    return formatAsISODate(parseYYYYDDD(s));
  }

  /**
   * This returns the current local dateTime in ISO T format.
   *
   * @return the current local dateTime in ISO T format (WITH timezone id)
   */
  public static String getCurrentISODateTimeStringLocalTZ() {
    return formatAsISODateTimeTTZ(ZonedDateTime.now(ZoneId.systemDefault()));
  }

  /**
   * This returns the current local dateTime in ISO T format.
   *
   * @return the current local dateTime in ISO T format (with no timezone id)
   */
  public static String getCurrentISODateTimeStringLocal() {
    return formatAsISODateTimeT(ZonedDateTime.now(ZoneId.systemDefault()));
  }

  /**
   * This returns the current local dateTime in compact ISO format (yyyyMMddHHmmss).
   *
   * @return the current local dateTime in compact ISO format (yyyyMMddHHmmss).
   */
  public static String getCompactCurrentISODateTimeStringLocal() {
    return formatAsCompactDateTime(ZonedDateTime.now(ZoneId.systemDefault()));
  }

  /**
   * This returns the current Zulu dateTime in ISO T format.
   *
   * @return the current Zulu dateTime in ISO T format (without the trailing Z)
   */
  public static String getCurrentISODateTimeStringZulu() {
    return formatAsISODateTimeT(ZonedDateTime.now(ZoneOffset.UTC));
  }

  /**
   * This returns the current Zulu date in RFC 822 format.
   *
   * @return the current Zulu date in RFC 822 format
   */
  public static String getCurrentRFC822Zulu() {
    return formatAsRFC822GMT(ZonedDateTime.now(ZoneOffset.UTC));
  }

  /**
   * This returns the current Zulu date in ISO format.
   *
   * @return the current Zulu date in ISO format
   */
  public static String getCurrentISODateStringZulu() {
    return formatAsISODate(ZonedDateTime.now(ZoneOffset.UTC));
  }

  /**
   * This converts an ISO DateTime string to millis since 1970-01-01T00:00:00Z.
   *
   * @param s the ISO DateTime string. This may optionally include hours, minutes, seconds, millis
   *     and Z or timezone offset (default=Zulu).
   * @return the millis since 1970-01-01T00:00:00Z
   * @throws RuntimeException if trouble (e.g., s is null or not at least #)
   */
  public static long isoStringToMillis(String s) {
    return isoStringToMillis(s, ZoneOffset.UTC);
  }

  /**
   * This converts an ISO DateTime string from the specified timeZone to millis since
   * 1970-01-01T00:00:00Z.
   *
   * @param s the ISO DateTime string. This may include hours, minutes, seconds, millis and perhaps
   *     Z or timezone offset (default=Zulu).
   * @param timeZone the externally supplied time zone (often zuluTimeZone)
   * @return the millis since 1970-01-01T00:00:00Z
   * @throws RuntimeException if trouble (e.g., s is null or not at least #)
   */
  public static long isoStringToMillis(String s, ZoneId timeZone) {
    // ZonedDateTime dt = parseISODateTime(s, timeZone == null ? ZoneOffset.UTC :
    // timeZone.toZoneId());
    // return dt.toInstant().toEpochMilli();
    ZonedDateTime dt = parseISODateTime(s, (timeZone == null ? ZoneOffset.UTC : timeZone));
    return dt.toInstant().toEpochMilli();
  }

  /**
   * This converts millis since 1970-01-01T00:00:00Z to an ISO Zulu DateTime string.
   *
   * @param millis the millis since 1970-01-01T00:00:00Z
   * @return the ISO Zulu Date string
   * @throws RuntimeException if trouble (e.g., millis is Long.MAX_VALUE)
   */
  public static String millisToIsoDateString(long millis) {
    if (millis == Long.MAX_VALUE)
      throw new RuntimeException(String2.ERROR + ": millis value is MAX_VALUE.");
    return formatAsISODate(newZdtUtc(millis));
  }

  /**
   * This converts millis since 1970-01-01T00:00:00Z to an ISO Zulu DateTime string with a trailing
   * Z.
   *
   * @param millis the millis since 1970-01-01T00:00:00Z
   * @return the ISO Zulu DateTime string 'T' with the trailing Z
   * @throws RuntimeException if trouble (e.g., millis is Long.MAX_VALUE)
   */
  public static String millisToIsoStringTZ(long millis) {
    return formatAsISODateTimeTZ(newZdtUtc(millis));
  }

  /**
   * This converts millis since 1970-01-01T00:00:00Z to an ISO Zulu DateTime string.
   *
   * @param millis the millis since 1970-01-01T00:00:00Z
   * @return the ISO Zulu DateTime string 'T', with 3 decimal places, with the trailing Z
   * @throws RuntimeException if trouble (e.g., millis is Long.MAX_VALUE)
   */
  public static String millisToIsoStringT3Z(long millis) {
    return formatAsISODateTimeT3Z(newZdtUtc(millis));
  }

  /**
   * This converts millis since 1970-01-01T00:00:00Z to an ISO Zulu DateTime string.
   *
   * @param millis the millis since 1970-01-01T00:00:00Z
   * @return the ISO Zulu DateTime string 'T', with 6 decimal places, with the trailing Z
   * @throws RuntimeException if trouble (e.g., millis is Long.MAX_VALUE)
   */
  public static String millisToIsoStringT6Z(long millis) {
    return formatAsISODateTimeT6Z(newZdtUtc(millis));
  }

  /**
   * This converts millis since 1970-01-01T00:00:00Z to an ISO Zulu DateTime string.
   *
   * @param millis the millis since 1970-01-01T00:00:00Z
   * @return the ISO Zulu DateTime string 'T', with 9 decimal places, with the trailing Z
   * @throws RuntimeException if trouble (e.g., millis is Long.MAX_VALUE)
   */
  public static String millisToIsoStringT9Z(long millis) {
    return formatAsISODateTimeT9Z(newZdtUtc(millis));
  }

  /**
   * Remove any spaces, dashes (except optional initial dash), colons, and T's from s.
   *
   * @param s a string
   * @return s with any spaces, dashes, colons removed (if s == null, this throws RuntimeException)
   * @throws RuntimeException if trouble (e.g., s is null)
   */
  public static String removeSpacesDashesColons(String s) {
    boolean negative = s.startsWith("-");
    if (negative) s = s.substring(1);
    s = String2.replaceAll(s, " ", "");
    s = String2.replaceAll(s, "-", "");
    s = String2.replaceAll(s, "T", "");
    return (negative ? "-" : "") + String2.replaceAll(s, ":", "");
  }

  /**
   * This adds the specified n field's to the isoDate, and returns the resulting ZonedDateTime
   * object.
   *
   * <p>This correctly handles B.C. dates.
   *
   * @param isoDate an iso formatted date time string. This may include hours, minutes, seconds,
   *     decimal, and Z or timezone offset (default=Zulu).
   * @param n the number of 'units' to be added
   * @param field one of the Calendar or Calendar2 constants for a field (e.g., Calendar2.YEAR).
   * @return the ZonedDateTime for isoDate with the specified n field's added
   * @throws Exception if trouble e.g., n is Integer.MAX_VALUE
   */
  public static ZonedDateTime isoDateTimeAdd(String isoDate, int n, int field) throws Exception {

    if (n == Integer.MAX_VALUE)
      Test.error(String2.ERROR + " in Calendar2.isoDateTimeAdd: invalid addN=" + n);
    ZonedDateTime dt = parseISODateTimeUtc(isoDate);
    dt = dt.plus(n, getChronoFieldFromCalendarField(field).getBaseUnit());
    return dt;
  }

  /**
   * This converts a millis elapsed time value (139872234 ms or 783 ms) to a nice string (e.g., "7h
   * 4m 5s", "5.783 s", or "783 ms"). <br>
   * was (e.g., "7:04:05.233" or "783 ms").
   *
   * @param millis may be negative
   * @return a simplified approximate string representation of elapsed time (or "infinite[!]" if
   *     trouble, e.g., millis is Double.NaN).
   */
  public static String elapsedTimeString(double millis) {
    if (!Double.isFinite(millis)) return "infinity";

    long time = Math2.roundToLong(millis);
    return elapsedTimeString(time);
  }

  public static String elapsedTimeString(long time) {
    if (time < Long.MIN_VALUE + 10000 || time > Long.MAX_VALUE - 10000) return "infinity";
    String negative = "";
    if (time < 0) {
      negative = "-";
      time = Math.abs(time);
    }
    long ms = time % 1000;
    long sec = time / 1000;
    long min = sec / 60;
    sec = sec % 60;
    long hr = min / 60;
    min = min % 60;
    long day = hr / 24;
    hr = hr % 24;

    if (day + hr + min + sec == 0) return negative + time + " ms";
    if (day + hr + min == 0) return negative + sec + "." + String2.zeroPad("" + ms, 3) + " s";
    String ds = day + (day == 1 ? " day" : " days");
    if (hr + min + sec == 0) return negative + ds;

    // was
    // return (day > 0? negative + ds + " " : negative) +
    //    String2.zeroPad("" + hr,  2) + ":" +
    //    String2.zeroPad("" + min, 2) + ":" +
    //    String2.zeroPad("" + sec, 2) +
    //    (ms > 0? "." + String2.zeroPad("" + ms,  3) : "");

    // e.g., 4h 17m 3s apple uses this style; easier to read
    return (day > 0 ? negative + ds + " " : negative)
        + ((day > 0 || hr > 0) ? hr + "h " : "")
        + min
        + "m "
        + // hr or min will be >0, so always include it
        sec
        +
        // since >59 seconds, don't include millis
        // (ms > 0? "." + String2.zeroPad("" + ms,  3) : "") +
        "s";
  }

  /**
   * This converts the date, hour, minute, second so the return is at the exact center of its
   * current month.
   *
   * @param gc
   * @return the new dt
   * @throws Exception if trouble (e.g., gc is null)
   */
  public static ZonedDateTime centerOfMonth(ZonedDateTime dt) throws Exception {
    int nDaysInMonth = dt.with(lastDayOfMonth()).getDayOfMonth();
    return dt.withDayOfMonth(1 + nDaysInMonth / 2)
        .withHour(Math2.odd(nDaysInMonth) ? 12 : 0)
        .withMinute(0)
        .withSecond(0)
        .withNano(0);
  }

  /**
   * This clears the fields smaller than 'field' (e.g., HOUR_OF_DAY clears MINUTE, SECOND, and
   * MILLISECOND, but doesn't change HOUR_OF_DAY, MONTH, or YEAR).
   *
   * @param gc
   * @param field e.g., HOUR_OF_DAY
   * @return the same gc, but modified, for convenience
   * @throws Exception if trouble (e.g., gc is null or field is not supported)
   */
  public static ZonedDateTime clearSmallerFields(ZonedDateTime zd, int field) throws Exception {

    if (field == MILLISECOND
        || field == SECOND
        || field == MINUTE
        || field == HOUR
        || field == HOUR_OF_DAY
        || field == DATE
        || field == DAY_OF_YEAR
        || field == MONTH
        || field == YEAR) {
    } else {
      Test.error(String2.ERROR + " in Calendar2.clearSmallerFields: unsupported field=" + field);
    }
    if (field == MILLISECOND) return zd;
    zd = zd.withNano(0);
    if (field == SECOND) return zd;
    zd = zd.withSecond(0);
    if (field == MINUTE) return zd;
    zd = zd.withMinute(0);
    if (field == HOUR || field == HOUR_OF_DAY) return zd;
    zd = zd.withHour(0);
    if (field == DATE) return zd;
    zd = zd.withDayOfMonth(1);
    if (field == MONTH) return zd;
    zd = zd.withMonth(1);
    return zd;
  }

  /**
   * This clears the fields smaller than 'field' (e.g., HOUR_OF_DAY clears MINUTE, SECOND, and
   * MILLISECOND, but doesn't change HOUR_OF_DAY, MONTH, or YEAR).
   *
   * @param epochSeconds (use NaN for missing value)
   * @param field e.g., HOUR_OF_DAY
   * @return the new epochSeconds value (or NaN if trouble).
   */
  public static double clearSmallerFields(double epochSeconds, int field) {
    if (!Double.isFinite(epochSeconds)) return Double.NaN;
    try {
      return zdtToEpochSeconds(clearSmallerFields(epochSecondsToZdt(epochSeconds), field));
    } catch (Exception e) {
      return Double.NaN;
    }
  }

  /**
   * This returns the start of a day, n days back from max (or from now if max=NaN).
   *
   * @param nDays
   * @param max seconds since epoch
   * @return seconds since epoch for the start of a day, n days back from max (or from now if
   *     max=NaN).
   * @throws Exception if trouble
   */
  public static double backNDays(int nDays, double max) throws Exception {
    ZonedDateTime dt =
        Double.isFinite(max) ? epochSecondsToZdt(max) : ZonedDateTime.now(ZoneOffset.UTC);
    // round to previous midnight, then go back nDays
    dt = clearSmallerFields(dt, DATE);
    return zdtToEpochSeconds(dt) - SECONDS_PER_DAY * nDays;
  }

  /**
   * This returns a double[] of maxNValues (or fewer) evenly spaced, between start and stop. The
   * first and last values will be start and stop. The intermediate values will be evenly spaced in
   * a human sense (eg monthly) but the start and stop won't necessarily use the same stride.
   *
   * @param start epoch seconds
   * @param stop epoch seconds
   * @param maxNValues maximum desired nValues
   * @return a double[] of nValues (or fewer) epoch seconds values, evenly spaced, between start and
   *     stop. <br>
   *     If start or stop is not finite, this returns null. <br>
   *     If start=stop, this returns just one value. <br>
   *     If start &gt; stop, they are swapped so the results are always ascending. <br>
   *     If trouble, this returns null.
   */
  public static double[] getNEvenlySpaced(double start, double stop, int maxNValues) {

    try {
      if (!Double.isFinite(start) || !Double.isFinite(stop)) return null;
      if (start == stop) return new double[] {start};
      if (start > stop) {
        double d = start;
        start = stop;
        stop = d;
      }

      double spm = SECONDS_PER_MINUTE; // double avoids int MAX_VALUE problem
      double sph = SECONDS_PER_HOUR;
      double spd = SECONDS_PER_DAY;
      double range = stop - start;
      double mnv2 = Math2.divideNoRemainder(maxNValues, 2); // double avoids int MAX_VALUE problem
      TemporalUnit field;
      int biggerField;
      int[] nice;
      double divisor;
      if (range <= mnv2 * spm) {
        field = ChronoUnit.SECONDS;
        biggerField = MINUTE;
        divisor = 1;
        nice = new int[] {1, 2, 5, 10, 15, 20, 30, 60};
      } else if (range <= mnv2 * sph) {
        field = ChronoUnit.MINUTES;
        biggerField = HOUR_OF_DAY;
        divisor = spm;
        nice = new int[] {1, 2, 5, 10, 15, 20, 30, 60};
      } else if (range <= mnv2 * spd) {
        field = ChronoUnit.HOURS;
        biggerField = DATE;
        divisor = sph;
        nice = new int[] {1, 2, 3, 4, 6, 12, 24};
      } else if (range <= mnv2 * 30 * spd) {
        field = ChronoUnit.DAYS;
        biggerField = MONTH;
        divisor = spd;
        nice = new int[] {1, 2, 5, 7};
      } else if (range <= mnv2 * 365 * spd) {
        field = ChronoUnit.MONTHS;
        biggerField = YEAR;
        divisor = 30 * spd;
        nice = new int[] {1, 2, 3, 6, 12};
      } else {
        field = ChronoUnit.YEARS;
        biggerField = -9999;
        divisor = 365 * spd;
        nice = new int[] {1, 2, 5, 10};
      }

      // find stride (some number of fields, e.g., 10 seconds)
      // range testing above ensures range/divisor=n, e.g. seconds will be < 60,
      //  or n minutes will be < 60, nHours < 24, ...
      // and ensure stride is at least 1.
      double dnValues = (range / divisor) / maxNValues;
      int stride = nextNice(dnValues, nice); // minimum stride will be 1
      if (field == ChronoUnit.DAYS) stride = Math.min(14, stride);
      DoubleArray da = new DoubleArray();
      da.add(start);
      ZonedDateTime nextZd = epochSecondsToZdt(start);
      if (field != ChronoUnit.YEARS) nextZd = clearSmallerFields(nextZd, biggerField);
      double next = zdtToEpochSeconds(nextZd);
      while (next < stop) {
        if (next > start) da.add(next); // it may not be for the first few
        if (field == ChronoUnit.DAYS) {
          // repeatedly using DATE=1 is nice, so ...
          // will subsequent value be in next month?
          // non-permanent test of this: ndbcSosSalinity has stride = 2 days; results have
          // 2008-09-27 then 2008-10-01
          int oMonth = nextZd.getMonthValue();
          nextZd = nextZd.plusDays(2L * stride); // 2* sets subsequent value
          if (nextZd.getMonthValue() == oMonth) {
            nextZd = nextZd.minusDays(stride); // go back to regular value
          } else {
            nextZd =
                nextZd.withDayOfMonth(
                    1); // go for DATE=1 in next month  e.g., 1,15,1,15 or 1,8,14,21,1,8,14,21,
          }
        } else {
          nextZd = nextZd.plus(stride, field);
        }
        next = zdtToEpochSeconds(nextZd);
      }
      da.add(stop);
      return da.toArray();

    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
      return null;
    }
  }

  /**
   * This returns the value in nice which is &gt;= d, or a multiple of the last value which is
   * higher than d. This is used to suggest the division distance along an axis.
   *
   * @param d a value e.g., 2.3 seconds
   * @param nice an ascending list. e.g., for seconds: 1,2,5,10,15,20,30,60
   * @return the value in nice which is &gt;= d, or a multiple of the last value which is higher
   *     than d
   */
  public static int nextNice(double d, int nice[]) {
    int n = nice.length;
    for (int j : nice) {
      if (d <= j) return j;
    }
    return Math2.roundToInt(Math.ceil(d / nice[n - 1]));
  }

  /**
   * This rounds to the nearest idealN, idealUnits (e.g., 2 months) (starting at Jan 1, 0000).
   *
   * @param epochSeconds
   * @param idealN e.g., 1 to 100
   * @param idealUnits an index of one of the IDEAL_UNITS
   * @return epochSeconds, converted to Zulu GC and rounded to the nearest idealN, idealUnits (e.g.,
   *     2 months)
   */
  public static ZonedDateTime roundToIdealGC(double epochSeconds, int idealN, int idealUnits) {
    long millis = Math2.roundToLong(epochSeconds * 1000);
    if (millis == Long.MAX_VALUE) Test.error(String2.ERROR + " in roundToIdealGC: millis is NaN!");

    ZonedDateTime dt = newZdtUtc(millis);
    if (idealUnits == 5) { // year
      double td = dt.getYear() + (dt.getMonthValue() - 1) / 12.0; // month is 0..
      int ti = Math2.roundToInt(td / idealN) * idealN; // round to nearest n units
      dt = ZonedDateTime.of(ti, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    } else if (idealUnits == 4) { // months
      double td = dt.getYear() * 12 + dt.getMonthValue() - 1; // month is 0..
      int ti = Math2.roundToInt(td / idealN) * idealN; // round to nearest n units
      dt = ZonedDateTime.of(ti / 12, (ti % 12) + 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    } else { // seconds ... days: all have consistent length
      double chunk = idealN * IDEAL_UNITS_SECONDS.get(idealUnits); // e.g., decimal number of days
      double td = Math.rint(epochSeconds / chunk) * chunk; // round to nearest n units
      dt = newZdtUtc(Math2.roundToLong(td * 1000));
    }
    return dt;
  }

  /**
   * Given a date time string, this suggests a java.time.format.DateTimeFormatter (was Joda)
   * date/time format suitable for parsing and output formatting.
   *
   * @param sample
   * @return a digitRegexTimeFormat an appropriate java.time.format.DateTimeFormatter (was Joda)
   *     date/time format or "" if not matched. If the response starts with "yyyy-M", parse with
   *     Calendar2.parseISODateTimeZulu(); else parse with java.time.format.DateTimeFormatter (was
   *     Joda).
   */
  public static String suggestDateTimeFormat(String sample) {
    if (sample == null) return "";
    sample = sample.trim();
    if (sample.length() == 0) return "";

    char ch = Character.toLowerCase(sample.charAt(0));
    if (ch == '-' && sample.length() > 1) ch = Character.toLowerCase(sample.charAt(1));
    int slm1 = sample.length() - 1;
    char lastCh = sample.charAt(slm1);
    // if sample ends with UT'C', GM'T', or 'Z', regex must too.
    boolean matchLastCh = "CTZ".indexOf(lastCh) >= 0;
    // sample ends with digit, so regex must end with a digit specification
    boolean lastChIsDigit = lastCh >= '0' && lastCh <= '9';
    if (ch >= '0' && ch <= '9') {
      // all digits?
      boolean allDigits = lastChIsDigit;
      if (allDigits) {
        for (int i = 1; i < slm1; i++) {
          if (!String2.isDigit(sample.charAt(i))) {
            allDigits = false;
            break;
          }
        }
      }
      if (allDigits) {
        for (int i = 0; i < allDigitsRegexTimeFormat.size(); i += 2) {
          // does it match this regex?
          if (sample.matches(allDigitsRegexTimeFormat.get(i)))
            return allDigitsRegexTimeFormat.get(i + 1);
        }

      } else {
        boolean hasColon = sample.indexOf(':') >= 0;
        boolean hasPeriod = sample.indexOf('.') >= 0;
        boolean hasSlash = sample.indexOf('/') >= 0;
        for (int i = 0; i < digitRegexTimeFormat.size(); i += 2) {
          // quick reject lots of regexes
          if (matchLastCh) { // to catch/match C|T|Z
            if (digitRegexTimeFormatLastChar[i] != lastCh)
              // regex must end with C|T|Z, but it doesn't
              continue;
          } else if (lastChIsDigit) {
            if ("CTZ".indexOf(digitRegexTimeFormatLastChar[i]) >= 0)
              // regex must end with digit specification, but it doesn't
              continue;
          }
          // regex must have same punctuation as sample, else quick reject
          if (digitRegexTimeFormatHasColon.get(i) != hasColon
              || digitRegexTimeFormatHasPeriod.get(i) != hasPeriod
              || digitRegexTimeFormatHasSlash.get(i) != hasSlash) continue;

          // does it match this regex?
          if (sample.matches(digitRegexTimeFormat.get(i))) return digitRegexTimeFormat.get(i + 1);
        }
      }

    } else if (ch >= 'a' && ch <= 'z') {
      for (int i = 0; i < letterRegexTimeFormat.size(); i += 2) {
        // quick reject lots of regexes
        if (matchLastCh) {
          if (letterRegexTimeFormatLastChar[i] != lastCh)
            // regex must end with C|T|Z, but it doesn't
            continue;
        } else if (lastChIsDigit) {
          if ("CTZ".indexOf(letterRegexTimeFormatLastChar[i]) >= 0)
            // regex must end with digit specification, but it doesn't
            continue;
        }
        // does it match this regex?
        if (sample.matches(letterRegexTimeFormat.get(i))) return letterRegexTimeFormat.get(i + 1);
      }
    }

    // fail
    return "";
  }

  /**
   * This looks for a date time format which is suitable for all elements of sa (other than nulls
   * and ""'s).
   *
   * @param sa a PrimitiveArray (usually StringArray or integer type), perhaps with consistently
   *     formatted date time String values.
   * @param evenIfPurelyNumeric lets you specify whether to try to match purely numeric formats
   * @return a date time format which is suitable for all elements of sa (other than nulls and
   *     ""'s), or "" if no suggestion. The format is suitable for parsing and output formatting If
   *     the response starts with "yyyy-M", parse with Calendar2.parseISODateTimeZulu(); else parse
   *     with java.time.format.DateTimeFormatter (was Joda).
   */
  public static String suggestDateTimeFormat(PrimitiveArray sa, boolean evenIfPurelyNumeric) {
    if (sa == null || sa.size() == 0) return "";
    boolean isIntegerArray = sa.isIntegerType();
    int n = sa.size();
    String noMatch = ">> suggestDateTimeFormat(StringArray): no match because ";

    String startWithDigit = null;
    String startWithLetter = null;
    BitSet isSomething = new BitSet(n); // initially all false
    for (int sai = 0; sai < n; sai++) {
      String s = sa.getString(sai);
      if (String2.isSomething(s)) isSomething.set(sai);
      else continue;
      char ch = Character.toLowerCase(s.charAt(0));
      if (ch == '-' || (ch >= '0' && ch <= '9')) {
        startWithDigit = s;
      } else if (ch >= 'a' && ch <= 'z') {
        startWithLetter = s;
      } else {
        if (debugMode)
          String2.log(
              noMatch + "#" + sai + "=\"" + s + "\" doesn't start with a digit or a letter.");
        return "";
      }

      if (startWithDigit != null && startWithLetter != null) {
        if (debugMode)
          String2.log(
              noMatch
                  + "some start with digits ("
                  + startWithDigit
                  + ") and some with letters ("
                  + startWithLetter
                  + ").");
        return "";
      }
    }

    int first = isSomething.nextSetBit(0);
    if (first == -1) {
      if (debugMode) String2.log(noMatch + "no value isSomething.");
      return "";
    }

    // restrict search to allDigits formats?
    boolean allDigits =
        startWithDigit != null && (isIntegerArray || String2.allDigits(sa.getString(first).trim()));
    if (allDigits && !evenIfPurelyNumeric) {
      if (debugMode)
        String2.log(noMatch + "some strings are purely numeric but evenIfPurelyNumeric=false.");
      return "";
    }

    // go through options
    // This is O(m*n) but in practice fast (O(m)) because most formats will be rejected by first
    // string.
    ImmutableList<String> regexTimeFormat =
        allDigits
            ? allDigitsRegexTimeFormat
            : startWithDigit != null ? digitRegexTimeFormat : letterRegexTimeFormat;
    for (int i = 0; i < regexTimeFormat.size(); i += 2) {
      String format = regexTimeFormat.get(i + 1);
      Pattern regexPattern = dateTimeFormatPatternHM.get(format);
      int sai = first;
      while (sai >= 0) {
        String sample = sa.getString(sai).trim();
        if (regexPattern.matcher(sample).matches()) {
          sai = isSomething.nextSetBit(sai + 1);
        } else {
          break;
        }
      }
      if (sai < 0) { // success!
        // String2.log(">> Calendar2.suggestDateTimeFormat found " + format + "\n" +
        //    MustBe.stackTrace());
        return format;
      }
    }
    if (debugMode) String2.log(noMatch + "no regex matched all Strings.");
    return "";
  }

  /**
   * This tries to figure out the format of someDateTimeString then parse the value and convert it
   * to epochSeconds.
   *
   * @param someDateTimeString
   * @return epochSeconds (or Double.NaN if trouble);
   */
  public static double tryToEpochSeconds(String someDateTimeString) {

    // going through tryToIsoString is more round about,
    //  but catches some additional weird options
    String isoString = tryToIsoString(someDateTimeString);
    if (isoString.length() == 0) return Double.NaN;
    return isoStringToEpochSeconds(isoString);
  }

  /**
   * This tries to find a format string for a dateTimeString.
   *
   * @return "" if unable.
   */
  public static String tryToFindFormat(String someDateTimeString) {
    if (someDateTimeString == null) return "";
    someDateTimeString = someDateTimeString.trim();
    if (someDateTimeString.length() == 0) return "";
    int spo = someDateTimeString.indexOf(';'); // NGDC often has "2009-05-27; publication"
    if (spo >= 0) {
      someDateTimeString = someDateTimeString.substring(0, spo).trim();
      if (someDateTimeString.length() == 0) return "";
    }
    if (someDateTimeString.endsWith(" (original)")) // e.g.,
      // https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/noaa.ersst.v5/sst.mon.ltm.nc
      someDateTimeString = someDateTimeString.substring(0, someDateTimeString.length() - 11);

    // catch some non-standard formats that aren't caught below
    char ch = someDateTimeString.charAt(0);
    if (ch == '0' || ch == '1') { // starts with 0 or 1
      String ts = someDateTimeString;
      if (ts.endsWith("UTC")) ts = ts.substring(0, ts.length() - 3).trim();
      if (ts.endsWith("Z")) ts = ts.substring(0, ts.length() - 1).trim();
      int zeroTimePo = ts.indexOf(" 00");
      if (zeroTimePo < 0) zeroTimePo = ts.indexOf("T00");
      if (zeroTimePo > 0) {
        String remains = ts.substring(zeroTimePo + 3);
        if (remains.matches("[:.0]*")) { // ends with e.g., 00:00:0.0
          ts = ts.substring(0, zeroTimePo);
        }
      }

      // I removed the direct conversions of e.g., 1-1-1 that were here
      // but still in code below
    }

    // formats I'm aware of
    String format = suggestDateTimeFormat(someDateTimeString);
    if (format.length() == 0)
      String2.log(
          "! Calendar2.tryToFindFormat was unable to find a format for " + someDateTimeString);
    return format;
  }

  /**
   * This tries to figure out the format of someDateTimeString then parse the value and convert to
   * an ISO 8601 string with 'Z' at end. This is the most flexible approach to parsing a weird date
   * time string.
   *
   * @param someDateTimeString
   * @return an iso8601String as a date, a dateTime with T and Z, or "" if trouble;
   */
  public static String tryToIsoString(String someDateTimeString) {
    if (someDateTimeString == null) return "";
    someDateTimeString = someDateTimeString.trim();
    if (someDateTimeString.length() == 0) return "";
    int spo = someDateTimeString.indexOf(';'); // NGDC often has "2009-05-27; publication"
    if (spo >= 0) {
      someDateTimeString = someDateTimeString.substring(0, spo).trim();
      if (someDateTimeString.length() == 0) return "";
    }
    if (someDateTimeString.endsWith(" (original)")) // e.g.,
      // https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/noaa.ersst.v5/sst.mon.ltm.nc
      someDateTimeString = someDateTimeString.substring(0, someDateTimeString.length() - 11);

    // catch some non-standard formats that aren't caught below
    char ch = someDateTimeString.charAt(0);
    if (ch == '0' || ch == '1') { // starts with 0 or 1
      String ts = someDateTimeString;
      String append = "";
      if (ts.endsWith("UTC")) ts = ts.substring(0, ts.length() - 3).trim();
      if (ts.endsWith("Z")) ts = ts.substring(0, ts.length() - 1).trim();
      int zeroTimePo = ts.indexOf(" 00");
      if (zeroTimePo < 0) zeroTimePo = ts.indexOf("T00");
      if (zeroTimePo > 0) {
        String remains = ts.substring(zeroTimePo + 3);
        if (remains.matches("[:.0]*")) { // ends with e.g., 00:00:0.0
          ts = ts.substring(0, zeroTimePo);
          append = remains.indexOf('.') >= 0 ? "T00:00:00.000Z" : "T00:00:00Z";
        }
      }
      // it must start with one of these
      // String2.log(">> ts=" + ts);
      if (ts.equals("1-1-1980")) // 1-1-1980 00:00 UTC
      return "1980-01-01" + append;
      if (String2.indexOf(new String[] {"01/01/01", "1/1/1", "1-1-1", "01-01-01"}, ts) >= 0)
        return "0001-01-01" + append;
      if (String2.indexOf(new String[] {"01/01/00", "1/1/0", "0-1-1", "00-01-01"}, ts) >= 0)
        return "0000-01-01" + append;
    }

    String format = suggestDateTimeFormat(someDateTimeString);
    if (format.length() == 0) {
      String2.log(
          "! Calendar2.tryToIsoString was unable to find a format for " + someDateTimeString);
      return "";
    }

    double sec = parseToEpochSeconds(someDateTimeString, format); // NaN if trouble
    // if source format has time, keep time in iso format (even if 00:00:00)
    // String2.log(">> format=" + format);
    return format.indexOf("SSS000000") >= 0
        ? safeEpochSecondsToIsoStringT9Z(sec, "")
        : format.indexOf("SSS000") >= 0
            ? safeEpochSecondsToIsoStringT6Z(sec, "")
            : format.indexOf('S') >= 0
                ? safeEpochSecondsToIsoStringT3Z(sec, "")
                : // 1-3 S
                format.indexOf('H') >= 0
                    ? safeEpochSecondsToIsoStringTZ(sec, "")
                    : safeEpochSecondsToIsoDateString(sec, ""); // else just date
  }

  /**
   * This cleans up a numeric time units string or throws a SimpleException.
   *
   * @param tUnits something like "seconds since 1970-01-01T00:00:00Z", but the units can be
   *     different and the base time can be in another format (if ERDDAP can parse it).
   * @throws RuntimeException if trouble (e.g., tUnits is invalid)
   */
  public static String cleanUpNumericTimeUnits(String tUnits) {
    String tUnitsLC = tUnits.toLowerCase();
    int sincePo = tUnitsLC.indexOf(" since ");
    if (sincePo <= 0)
      throw new SimpleException(
          String2.ERROR + ": units=\"" + tUnits + "\" doesn't include \" since \".");

    // new units
    String tu = tUnitsLC.substring(0, sincePo).trim();
    tu = String2.replaceAll(tu, "julian day", "day");
    String nu = null;
    double factor = factorToGetSeconds(tu); // throws exception if trouble
    if (factor == 0.001) nu = "milliseconds";
    else if (factor == 1) nu = "seconds";
    else if (factor == SECONDS_PER_MINUTE) nu = "minutes";
    else if (factor == SECONDS_PER_HOUR) nu = "hours";
    else if (factor == SECONDS_PER_DAY) nu = "days";
    else if (factor == 7 * SECONDS_PER_DAY) nu = "weeks";
    else if (factor == 30 * SECONDS_PER_DAY) nu = "months";
    else if (factor == 360 * SECONDS_PER_DAY) nu = "years";
    else throw new SimpleException(String2.ERROR + ": invalid units.");

    // new base time
    String newBase = tryToIsoString(tUnits.substring(sincePo + 7));
    if (newBase.length() == 0)
      throw new SimpleException(
          String2.ERROR + ": The format of the base time in time units isn't supported.");
    else if (newBase.length() == 10) newBase += "T00:00:00Z";
    return nu + " since " + newBase;
  }

  /**
   * This formats the epochSeconds time value using the pattern. WARNING: This may give incorrect
   * results with years before 0001.
   *
   * @param epochSeconds
   * @param pattern see
   *     https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   * @param zone if "" or null, Zulu is used
   * @return the formatted time string (or "" if trouble)
   */
  public static String format(double epochSeconds, String pattern, String zone) {
    if (!Double.isFinite(epochSeconds) || Math.abs(epochSeconds * 1000) >= Long.MAX_VALUE)
      return "";
    if (!String2.isSomething(pattern)) pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // with 'Z'
    try {
      DateTimeFormatter dtf = makeDateTimeFormatter(pattern, zone);
      return format(epochSeconds, dtf);
    } catch (Exception e) {
      if (reallyVerbose) String2.log("Caught: " + MustBe.throwableToString(e));
      return "";
    }
  }

  /**
   * This formats the epochSeconds time value using the DateTimeFormatter. WARNING: This may give
   * incorrect results with years before 0001.
   */
  public static String format(double epochSeconds, DateTimeFormatter dtf) {
    String s = dtf.format(epochSecondsToZdt(epochSeconds));
    s = String2.replaceAll(s, "[XXX][XX]", "Z");
    return s;
  }

  /**
   * This makes a case insensitive DateTimeFormatter with -01-01T00:00:00.000 defaults.
   *
   * @param pattern see
   *     https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   * @param zone if not specified, Zulu is used
   */
  public static DateTimeFormatter makeDateTimeFormatter(String pattern, String zone) {
    // always deal with proleptic YEAR (-1=2 BCE, 0=1 BCE, 1=1 CE), not YEAR_OF_ERA
    // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/temporal/ChronoField.html#YEAR_OF_ERA
    // ??? Are there cases where y is used not as year?  eg as literal?
    String yy = "yy";
    int po = pattern.indexOf(yy);
    if (po < 0) {
      yy = "YY";
      po = pattern.indexOf(yy);
      if (po < 0) {
        yy = "uu";
        po = pattern.indexOf(yy);
        // there is no U
      }
    }
    if (po >= 0 && pattern.indexOf(yy + yy) < 0)
      throw new SimpleException(
          "DateTime formats with "
              + yy
              + " are not allowed. "
              + "Change the source values to use 4-digit years, and use "
              + yy
              + yy
              + " in the dateTime format.");

    if (yy.charAt(0) != 'u') pattern = String2.replaceAll(pattern, yy.charAt(0), 'u');

    // https://stackoverflow.com/questions/34637626/java-datetimeformatter-for-time-zone-with-an-optional-colon-separator
    // 2018-02-07 Don't second guess. Require the correct pattern.
    // pattern = String2.replaceAll(pattern, "Z", "[XXX][X]"); //most flexible time offset support

    // https://stackoverflow.com/questions/38307816/datetimeformatterbuilder-with-specified-parsedefaulting-conflicts-for-year-field
    DateTimeFormatter dtf =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive() // needed for supporting e.g., WED in addition to official Wed
            // .parseLenient()   //My tests pass without this (or adding this didn't help). It's
            // effect is unclear: for parsing the format or parsing a date string?
            .appendPattern(pattern)
            // .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)  //this approach didn't work
            .toFormatter()
            .withResolverStyle(ResolverStyle.LENIENT); // needed for e.g. day 366 in a leap year
    // so dtf has either an offset (via X) or a timezone
    if (pattern.indexOf('X') < 0 && pattern.indexOf('x') < 0) {
      dtf = dtf.withZone(String2.isSomething(zone) ? Calendar2.getZoneId(zone) : zuluZoneId);
    }
    return dtf;
  }

  /**
   * This converts a sourceTime string into a double with epochSeconds.
   *
   * @param sourceTime a formatted time string
   * @param dateTimeFormat one of the ISO8601 formats above, or a java.time.format.DateTimeFormatter
   *     (was Joda) format. If it starts with "uuuu-M", "yyyy-M", or "YYYY-M" (Y is
   *     discouraged/incorrect), sourceTime will be parsed with Calendar2.parseISODateTimeZulu();
   *     else parse with Calendar2 methods (was java.time.format.DateTimeFormatter, was Joda).
   * @param timeZone if null, default is Zulu
   * @return the epochSeconds value or NaN if trouble
   */
  public static double parseToEpochSeconds(
      String sourceTime, String dateTimeFormat, String timeZone) {
    ZoneId zoneId = getZoneId(timeZone);
    return parseToEpochSeconds(sourceTime, dateTimeFormat, zoneId);
  }

  public static ZoneId getZoneId(String timeZone) {
    if (timeZone == null || timeZone.isEmpty()) {
      return ZoneOffset.UTC;
    }
    try {
      return ZoneId.of(timeZone);
    } catch (NullPointerException e) {
      return TimeZone.getTimeZone(timeZone).toZoneId();
    }
  }

  /**
   * This converts a sourceTime string into a double with epochSeconds.
   *
   * @param sourceTime a formatted time string
   * @param dateTimeFormat one of the ISO8601 formats above, or a java.time.format.DateTimeFormatter
   *     (was Joda) format. If it starts with "uuuu-M", "yyyy-M", or "YYYY-M" (Y is
   *     discouraged/incorrect), sourceTime will be parsed with Calendar2.parseISODateTimeZulu();
   *     else parse with Calendar2 methods (was java.time.format.DateTimeFormatter, was Joda).
   * @param timeZone if null, default is Zulu
   * @return the epochSeconds value or NaN if trouble
   */
  public static double parseToEpochSeconds(
      String sourceTime, String dateTimeFormat, ZoneId zoneId) {
    // String2.log(">> toEpochSeconds " + sourceTime + "  " + dateTimeFormat);
    try {

      // parse with parseDateTime
      return formattedStringToMillis(sourceTime, dateTimeFormat, zoneId) / 1000.0;

    } catch (Throwable t) {
      if (verbose && sourceTime != null && sourceTime.length() > 0) {
        String msg = t.toString();
        if (msg.indexOf(sourceTime) < 0) // ensure sourceTime is in msg
        msg =
              String2.ERROR
                  + ": Invalid sourceTime=\""
                  + sourceTime
                  + "\" format=\""
                  + dateTimeFormat
                  + "\": "
                  + msg;
        String2.log(msg);
      }
      return Double.NaN;
    }
  }

  /** A variant of parseToEpochSeconds that uses the Zulu time zone. */
  public static double parseToEpochSeconds(String sourceTime, String dateTimeFormat) {
    return parseToEpochSeconds(sourceTime, dateTimeFormat, ZoneOffset.UTC);
  }

  /**
   * This converts sa into a DoubleArray with epochSeconds.
   *
   * @param sa is a StringArray or an integer-type array
   * @param dateTimeFormat one of the ISO8601 formats above, or a java.time.format.DateTimeFormatter
   *     (was Joda) format. If it starts with "uuuu-M", "yyyy-M", or "YYYY-M" (Y is
   *     discouraged/incorrect), sa strings will be parsed with Calendar2.parseISODateTimeZulu();
   *     else parse with java.time.format.DateTimeFormatter (was Joda).
   * @return a DoubleArray with the epochSeconds values (any/all will be NaN if touble)
   */
  public static DoubleArray parseToEpochSeconds(PrimitiveArray sa, String dateTimeFormat) {
    int n = sa.size();
    DoubleArray da = new DoubleArray(n, false);
    if (dateTimeFormat == null || dateTimeFormat.length() == 0) {
      da.addN(n, Double.NaN);
      return da;
    }
    try {

      if (dateTimeFormat.startsWith("uuuu-M")
          || dateTimeFormat.startsWith("yyyy-M")
          || dateTimeFormat.startsWith("YYYY-M")) { // Y is discouraged/incorrect
        // use Calendar2
        for (int i = 0; i < n; i++) da.add(safeIsoStringToEpochSeconds(sa.getString(i)));
      } else {
        // was: use java.time.format.DateTimeFormatter (was Joda)
        boolean printError = verbose;
        // DateTimeFormatter formatter = makeDateTimeFormatter(dateTimeFormat, zulu);
        da.addN(n, Double.NaN);
        for (int i = 0; i < n; i++) {
          String s = sa.getString(i);
          if (s != null && s.length() > 0) {
            try {
              da.set(i, parseToEpochSeconds(s, dateTimeFormat)); // was formatter)); //thread safe
            } catch (Throwable t2) {
              if (printError) {
                String2.log(
                    "  EDVTimeStamp.sourceTimeToEpochSeconds: error while parsing sourceTime="
                        + s
                        + " with format="
                        + dateTimeFormat
                        + "\n"
                        + t2);
                printError = false;
              }
            }
          }
        }
      }

    } catch (Throwable t) {
      if (verbose)
        String2.log("  Calendar2.toEpochSeconds: format=" + dateTimeFormat + ", error=" + t);
    }
    return da;
  }

  /**
   * If s is a crude String date time format, this converts it to the proper Java Date Time Format,
   * e.g., yyyy-MM-dd'T'HH:mm:ssZ
   * https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   * was http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html This does
   * the best it can but it is an impossible task without seeing all the actual String date time
   * data values. This assumes hour, if present, is 24 hour. This doesn't handle am pm.
   *
   * @param s a String that might have a poorly defined String date time format.
   * @return s converted into a (hopefully) correctly defined Java String date time format, or the
   *     original s if it was already valid or wasn't a String date time format.
   */
  public static String convertToJavaDateTimeFormat(String s) {
    if (s == null) return s;
    s = s.trim();
    if (s.length() == 0) return s;
    String sLC = s.toLowerCase();

    if (sLC.indexOf("yy") >= 0
        || // has years
        sLC.indexOf("mm") >= 0
        || // has month or minutes
        sLC.indexOf("dd") >= 0
        || // has month
        sLC.indexOf("hh") >= 0) { // has hours
      // fall through   these are very rare in normal text

      // impossible to know if e.g. y (or m or d) should be y, yy or yyyy
    } else if (sLC.indexOf("y-m") >= 0
        || // ISO
        sLC.indexOf("m/d/y") >= 0
        || // US common
        sLC.indexOf("m.d.y") >= 0
        || // US uncommon
        sLC.indexOf("d/m/y") >= 0
        || // Europe common
        sLC.indexOf("ymd") >= 0
        || // compact ISO
        sLC.indexOf("mdy") >= 0
        || // compact US
        sLC.indexOf("dmy") >= 0
        || // compact Europe
        sLC.indexOf("h:m") >= 0
        || // time
        sLC.indexOf("hms") >= 0) { // compact time
      // fall through   these are very rare in normal text

    } else if (sLC.equals("hm")) {
      return "Hm";
    } else {
      return s; // no evidence this is a date or time format
    }

    // if it is already a suggested date time format, don't touch it
    for (int i = 1; i < digitRegexTimeFormat.size(); i += 2) // every other one has a format
    if (s.equals(digitRegexTimeFormat.get(i))) return s;
    for (int i = 1; i < allDigitsRegexTimeFormat.size(); i += 2) // every other one has a format
    if (s.equals(allDigitsRegexTimeFormat.get(i))) return s;
    for (int i = 1; i < letterRegexTimeFormat.size(); i += 2) // every other one has a format
    if (s.equals(letterRegexTimeFormat.get(i))) return s;

    // fix common problems
    s = String2.replaceAllIgnoreCase(s, "mon", "MMM");

    // push iso toward yyyy-MM-dd'T'HH:mm:ssZ and thus Calendar2.parseISODateTime
    if (String2.indexOfIgnoreCase(s, "yy") < 0)
      s = String2.replaceAllIgnoreCase(s, "y-m-d", "yyyy-MM-dd");
    s = String2.replaceAllIgnoreCase(s, "yyyy-mm-dd", "yyyy-MM-dd");
    if (s.toLowerCase().startsWith("yy-mm-dd")) s = "yyyy-MM-dd" + s.substring(8);
    if (s.indexOf("yyyy-MM-dd") >= 0) {
      // If start is ISO-like, the rest almost always is, too.
      // flexi-digit times are rare. Incorrect case is common.
      s = String2.replaceAllIgnoreCase(s, "h:m:s", "HH:mm:ss");
      if (s.toLowerCase().endsWith("h:m")) s = s.substring(0, s.length() - 3) + "HH:mm";
    }
    sLC = s.toLowerCase();

    // deal with different case   -> yyyy-MM-dd'T'HH:mm:ssZ
    // Remember: single letter accepts 1/2/4 digit values
    // This assumes hour, if present, is 24 hour.   This doesn't handle am pm.
    int ypo = sLC.indexOf('y');
    int hpo = sLC.indexOf('h');
    if (ypo < hpo) {
      // date (if present) is before time (which is present)
      // e.g., y-m-d h:m:s, or h:m:s
      s = String2.replaceAll(s.substring(0, hpo), "Y", "y") + s.substring(hpo); // year
      s =
          String2.replaceAll(s.substring(0, hpo), "m", "M")
              + // ->months
              String2.replaceAll(s.substring(hpo), "M", "m"); // ->minutes
      s = String2.replaceAll(s.substring(0, hpo), "D", "d") + s.substring(hpo); // date
      s = s.substring(0, hpo) + String2.replaceAll(s.substring(hpo), "h", "H"); // hour
      s = s.substring(0, hpo) + String2.replaceAll(s.substring(hpo), "S", "s"); // sec

    } else if (hpo >= 0 && hpo < ypo) {
      // time and date exists, and time is before date
      // e.g., h:m:s y-m-d, or h:m:s m/d/y
      int po = sLC.indexOf(' ');
      if (po < 0) {
        po = sLC.lastIndexOf('s');
        if (po > 0) po++;
      }
      if (po < 0) po = ypo; // not ideal; too many ambiguous options
      s = s.substring(0, po) + String2.replaceAll(s.substring(0, po), "Y", "y"); // year
      s =
          String2.replaceAll(s.substring(0, po), "M", "m")
              + // ->minutes
              String2.replaceAll(s.substring(po), "m", "M"); // ->months
      s = s.substring(0, po) + String2.replaceAll(s.substring(0, po), "D", "d"); // date
      s = String2.replaceAll(s.substring(0, po), "h", "H") + s.substring(po); // hour
      s = String2.replaceAll(s.substring(0, po), "S", "s") + s.substring(po); // sec

    } else {
      // hpo = -1,   no time,  just date
      // e.g., y-m-d
      s = String2.replaceAll(s, "Y", "y"); // year
      s = String2.replaceAll(s, "m", "M"); // month
      s = String2.replaceAll(s, "D", "d"); // date
    }

    // change a single y to yyyy.
    // It won't cause problems parsing dates and will be much easier to identify as data format.
    ypo = s.indexOf('y');
    int yypo = s.indexOf("yy");
    if (ypo >= 0 && yypo < 0) s = s.substring(0, ypo) + "yyy" + s.substring(ypo);

    // if just MM, assume it's minutes, not months    //but this is just a guess
    if (s.equals("MM")) s = "mm";

    // special cases
    s = String2.replaceAllIgnoreCase(s, "h:m:s", "H:m:s"); // unknowable if it should be HH:mm:ss
    s = String2.replaceAllIgnoreCase(s, "HH:MM:SS", "HH:mm:ss");
    s = String2.replaceAll(s, "' '", " "); // in case already quoted
    s = String2.replaceAll(s, " ", "' '");
    s = String2.replaceAll(s, "'T'", "T"); // in case already quoted
    s = String2.replaceAll(s, "T", "'T'");

    s = String2.replaceAll(s, ".sss", ".SSS");
    s = String2.replaceAll(s, ".ss", ".SS");
    s = String2.replaceAll(s, ".s", ".S");

    // String2.log(">Calendar2.convertToJavaDateTimeFormat " + os + " -> " + s);
    return s;
  }

  public static ChronoField getChronoFieldFromCalendarField(int calendarField) {
    return switch (calendarField) {
      case Calendar.ERA -> ChronoField.ERA;
      case Calendar.YEAR -> ChronoField.YEAR;
      case Calendar.MONTH -> ChronoField.MONTH_OF_YEAR;
      case Calendar.WEEK_OF_YEAR -> ChronoField.ALIGNED_WEEK_OF_YEAR;
      case Calendar.WEEK_OF_MONTH -> ChronoField.ALIGNED_WEEK_OF_MONTH;
      case Calendar.DATE -> ChronoField.DAY_OF_MONTH;
      case Calendar.DAY_OF_YEAR -> ChronoField.DAY_OF_YEAR;
      case Calendar.DAY_OF_WEEK -> ChronoField.DAY_OF_WEEK;
      case Calendar.DAY_OF_WEEK_IN_MONTH -> ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
      case Calendar.AM_PM -> ChronoField.AMPM_OF_DAY;
      case Calendar.HOUR -> ChronoField.HOUR_OF_AMPM;
      case Calendar.HOUR_OF_DAY -> ChronoField.HOUR_OF_DAY;
      case Calendar.MINUTE -> ChronoField.MINUTE_OF_HOUR;
      case Calendar.SECOND -> ChronoField.SECOND_OF_MINUTE;
      case Calendar.MILLISECOND -> ChronoField.MILLI_OF_SECOND;
        // Calendar.ZONE_OFFSET and Calendar.DST_OFFSET not currently supported.

      default ->
          throw new IllegalArgumentException("No support for calendar field: " + calendarField);
    };
  }

  public static int getGcFieldFromZdt(ZonedDateTime zdt, int field) {
    ChronoField chronoField = getChronoFieldFromCalendarField(field);
    if (chronoField == ChronoField.MONTH_OF_YEAR) {
      return zdt.get(chronoField) - 1;
    }
    return zdt.get(chronoField);
  }

  public static ZonedDateTime setGcFieldOnZdt(ZonedDateTime zdt, int field, long value) {
    ChronoField chronoField = getChronoFieldFromCalendarField(field);
    if (chronoField == ChronoField.MONTH_OF_YEAR) {
      return zdt.with(chronoField, value + 1);
    }
    return zdt.with(chronoField, value);
  }

  public static ZonedDateTime addGcFieldToZdt(ZonedDateTime zdt, int field, long value) {
    ChronoField chronoField = getChronoFieldFromCalendarField(field);
    return zdt.plus(value, chronoField.getBaseUnit());
  }
}
