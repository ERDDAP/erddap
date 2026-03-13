/*
 * $Id: GeoDate.java,v 1.17 2003/08/22 23:02:40 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <code>GeoDate</code> extends the capabilities of <code>Date</code>. Additional features of <code>
 * GeoDate</code> include methods for incrementing and decrementing, adding and substracting <code>
 * GeoDate</code> objects. All <code>GeoDate</code> times are in "UTC". This simplifies the
 * conversion to and from <code>String</code> representations of time.
 *
 * @author Donald Denbo
 * @version $Revision: 1.17 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 1.0
 */
public class GeoDate implements java.io.Serializable, Comparable<GeoDate> {
  private final int[] max_day_ = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
  private boolean splitDone_;
  private int year_;
  private int month_;
  private int day_;
  private int hour_;
  private int minute_;
  private int second_;
  private int msec_;

  private Instant instant = Instant.now();

  /** Increment or decrement in days. */
  public static final int DAYS = 1;

  /** Increment or decrement in months. */
  public static final int MONTHS = 2;

  /** Increment or decrement in years. */
  public static final int YEARS = 4;

  /** Increment or decrement in hours. */
  public static final int HOURS = 5;

  /** Increment or decrement in minutes */
  public static final int MINUTES = 6;

  /** Increment or decrement in seonds */
  public static final int SECONDS = 7;

  /** Increment or decrement in milliseconds */
  public static final int MSEC = 8;

  /** Number of milliseconds in a day. */
  public static final long MSECS_IN_DAY = 86400000;

  /** Construct a new <code>GeoDate</code>. */
  public GeoDate() {
    super();
  }

  /**
   * Constructs a new <code>GeoDate</code> from an existing <code>GeoDate</code>.
   *
   * @param t <code>GeoDate</code>
   */
  public GeoDate(GeoDate t) {
    this(t.getTime());
  }

  /**
   * Construct a new <code>GeoDate</code> from values. The specified time is taken to be "GMT".
   *
   * @param mon month
   * @param day day of the month
   * @param year year (no offset!)
   * @param hour hour
   * @param min minutes
   * @param sec seconds
   * @param msec milliseconds
   * @exception IllegalTimeValue The constructor was called with a set of parameters that does not
   *     constitute a legitimate time value.
   */
  public GeoDate(int mon, int day, int year, int hour, int min, int sec, int msec)
      throws IllegalTimeValue {
    this.set(mon, day, year, hour, min, sec, msec);
  }

  /**
   * Allocates a GeoDate object and initializes it to represent the specified number of milliseconds
   * since the standard base time know as "the epoch", namely January 1, 1970, 00:00:00 GMT.
   */
  public GeoDate(long date) {
    setTime(date);
  }

  /**
   * Gets the milliseconds since epoch.
   *
   * @return milliseconds since epoch
   */
  public long getTime() {
    return instant.toEpochMilli();
  }

  /**
   * Sets the time to be milliseconds since epoch of time.
   *
   * @param time
   */
  public void setTime(long time) {
    instant = Instant.ofEpochMilli(time);
  }

  public boolean before(GeoDate other) {
    return getTime() < other.getTime();
  }

  public boolean after(GeoDate other) {
    return getTime() > other.getTime();
  }

  /**
   * Change value of <code>GeoDate</code> from values. Time zone for conversion is "GMT".
   *
   * @param mon month (1=January, 12=December)
   * @param day day of the month
   * @param year year (no offset!)
   * @param hour hour
   * @param min minutes
   * @param sec seconds
   * @param msec milliseconds
   * @exception IllegalTimeValue The parameters passed to this method represent a time value that is
   *     invalid
   */
  public void set(int mon, int day, int year, int hour, int min, int sec, int msec)
      throws IllegalTimeValue {
    int leap = (year % 4 != 0 ? 0 : (year % 400 == 0 ? 1 : (year % 100 == 0 ? 0 : 1)));
    max_day_[1] = 28 + leap;

    if (mon > 12 || mon < 1) {
      this.setTime(0);
      throw new IllegalTimeValue("value of month out of range");
    }
    if (day > max_day_[mon - 1] || day < 1) {
      this.setTime(0);
      throw new IllegalTimeValue("value of day out of range");
    }
    if (hour >= 24 || hour < 0) {
      this.setTime(0);
      throw new IllegalTimeValue("value of hour out of range");
    }
    if (min >= 60 || min < 0) {
      this.setTime(0);
      throw new IllegalTimeValue("value of minute out of range");
    }
    if (sec >= 60 || sec < 0) {
      this.setTime(0);
      throw new IllegalTimeValue("value of second out of range");
    }
    if (msec >= 1000 || msec < 0) {
      this.setTime(0);
      throw new IllegalTimeValue("value of msec out of range");
    }

    instant =
        LocalDateTime.of(year, mon, day, hour, min, sec, msec * 1000000 /* milli to nano */)
            .toInstant(ZoneOffset.UTC);

    splitDone_ = false;
  }

  /**
   * Add time to current <code>GeoDate</code>. This operation only makes since if <code>time</code>
   * is a relative time value, i.e. the result of a <code>GeoDate</code> subrtraction.
   *
   * @param time <code>GeoDate</code>
   * @return new <code>GeoDate</code>
   */
  public GeoDate add(GeoDate time) {
    GeoDate time2 = new GeoDate();
    long MSec = getTime() + time.getTime();

    time2.splitDone_ = false;
    time2.setTime(MSec);
    return time2;
  }

  /**
   * Subtract time2 from current <code>GeoDate</code>.
   *
   * @param time2 subtracthend
   * @return new <code>GeoDate</code>
   */
  public GeoDate subtract(GeoDate time2) {
    GeoDate delta = new GeoDate();
    long MSec = getTime() - time2.getTime();

    delta.splitDone_ = false;
    delta.setTime(MSec);
    return delta;
  }

  /**
   * Divide by value. Current time should the result of adding two times or subtracting two times to
   * be a meaningful calculation.
   *
   * @param val divisor
   * @return new <code>GeoDate</code>
   */
  public GeoDate divide(double val) {
    GeoDate result = new GeoDate();

    if (val == 0.0) return null;

    result.setTime((long) (((double) getTime()) / val));

    result.splitDone_ = false;
    return result;
  }

  void splitTimeFormat() {
    if (splitDone_) return;

    ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
    day_ = dateTime.getDayOfMonth();
    month_ = dateTime.getMonthValue();
    year_ = dateTime.getYear();
    hour_ = dateTime.getHour();
    minute_ = dateTime.getMinute();
    second_ = dateTime.getSecond();
    msec_ = dateTime.getNano() / 1000000; // Nano to millis

    splitDone_ = true;
  }

  /**
   * Increment current <code>GeoDate</code> by <code>SECONDS</code>, <code>MINUTES</code>, <code>
   * HOURS</code>, <code>DAYS</code>, <code>MONTHS</code>, or <code>YEARS</code>
   *
   * @param val amount to increment
   * @param tu time units (<code>SECONDS</code>, <code>MINUTES</code>, <code>HOURS</code>, <code>
   *     DAYS</code>, <code>MONTHS</code>, or <code>YEARS</code>) operation.
   */
  public GeoDate increment(float val, int tu) {
    return increment((double) val, tu);
  }

  /**
   * Increment current <code>GeoDate</code> by <code>SECONDS</code>, <code>MINUTES</code>, <code>
   * HOURS</code>, <code>DAYS</code>, <code>MONTHS</code>, or <code>YEARS</code>
   *
   * @param val amount to increment
   * @param tu time units (<code>SECONDS</code>, <code>MINUTES</code>, <code>HOURS</code>, <code>
   *     DAYS</code>, <code>MONTHS</code>, or <code>YEARS</code>) operation.
   */
  public GeoDate increment(double val, int tu) {
    int leap;
    int ival = (int) val;
    double fract = val - ival;
    long MSec = getTime();
    switch (tu) {
      case MSEC:
        MSec += (long) val;
        setTime(MSec);
        break;
      case SECONDS:
        MSec += (long) (val * 1000);
        setTime(MSec);
        break;
      case MINUTES:
        MSec += (long) (val * 60000);
        setTime(MSec);
        break;
      case HOURS:
        MSec += (long) (val * 3600000);
        setTime(MSec);
        break;
      case DAYS: // days and fraction days
        MSec += (long) (val * MSECS_IN_DAY);
        setTime(MSec);
        break;
      case MONTHS: // nearest day
        splitTimeFormat();
        month_ += ival;
        year_ += ((month_ - 1) / 12);
        month_ -= ((month_ - 1) / 12) * 12;
        if (month_ == 0) month_ = 1;
        leap = (year_ % 4 != 0 ? 0 : (year_ % 400 == 0 ? 1 : (year_ % 100 == 0 ? 0 : 1)));
        max_day_[1] = 28 + leap;
        day_ += (int) (fract * max_day_[month_ - 1]);
        if (day_ > max_day_[month_ - 1]) {
          day_ -= max_day_[month_ - 1];
          month_++;
          year_ += ((month_ - 1) / 12);
          month_ -= ((month_ - 1) / 12) * 12;
          if (month_ == 0) month_ = 1;
        }
        try {
          this.set(month_, day_, year_, hour_, minute_, second_, msec_);
        } catch (IllegalTimeValue e) {
          System.err.println(e);
        }
        break;
      case YEARS: // nearest day
        splitTimeFormat();
        year_ += ival;
        leap = (year_ % 4 != 0 ? 0 : (year_ % 400 == 0 ? 1 : (year_ % 100 == 0 ? 0 : 1)));
        max_day_[1] = 28 + leap;
        if (day_ > max_day_[month_ - 1]) day_ = max_day_[month_ - 1];
        try {
          this.set(month_, day_, year_, hour_, minute_, second_, msec_);
        } catch (IllegalTimeValue e) {
          System.err.println(e);
        }
        setTime(getTime() + (long) ((fract * 365.25) * MSECS_IN_DAY));
        break;
    }
    splitDone_ = false;
    return this;
  }

  /**
   * Time offset from reference <code>GeoDate</code>
   *
   * @param ref reference <code>GeoDate</code>
   * @return offset in days
   */
  public double offset(GeoDate ref) {
    double val;
    val = ((double) (this.getTime() - ref.getTime())) / 86400000.0;
    return val;
  }

  /**
   * Get year
   *
   * <p>deprecated Overrides a deprecated method, replaced by {@link #getGMTYear}.
   */
  // public int getYear() {
  //  splitTimeFormat();
  //  return year_;
  // }
  /** Get year. */
  public int getGMTYear() {
    splitTimeFormat();
    return year_;
  }

  /** Get month */
  public int getGMTMonth() {
    splitTimeFormat();
    return month_;
  }

  /** Get day */
  public int getGMTDay() {
    splitTimeFormat();
    return day_;
  }

  /** Get hours */
  public int getGMTHours() {
    splitTimeFormat();
    return hour_;
  }

  /** Get minutes */
  public int getGMTMinutes() {
    splitTimeFormat();
    return minute_;
  }

  /** Get int seconds. //doc said +fraction, that's wasn't true */
  public int getGMTSeconds() { // was double; bob made int
    splitTimeFormat();
    return second_;
  }

  /** Get millis (0 - 999). Bob added this. */
  public int getGMTMillis() {
    splitTimeFormat();
    return msec_;
  }

  /**
   * Convert <code>GeoDate</code> to <code>String</code> using standard format
   * "yyyy-MM-dd'T'HH:mm:ss z" and "GMT" time zone.
   *
   * @return date
   */
  @Override
  public String toString() {
    // 2011-12-15 Bob Simons changed space to 'T'
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss z");
    ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
    return zdt.format(dtf);
  }

  /**
   * Convert <code>GeoDate</code> to <code>String</code> using provided format. The <code>
   * SimpleDateFormat</code> and the <code>ZonedDateTime</code> is used to format the <code>
   * GeoDate</code>. A format of "decade" will create a string of the form 1990 or 1980.
   *
   * @see SimpleDateFormat
   * @param format String containing codes used to write time.
   */
  public String toString(String format) {
    if (format.equals("decade")) {
      splitTimeFormat();
      return Integer.toString((year_ / 10) * 10);
    } else {
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
      ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
      return zdt.format(dtf);
    }
  }

  /**
   * Tests to see if value represents missing data. A value from the getTime() method of <code>
   * Long.MIN_VALUE</code> represents a missing observation.
   *
   * @see gov.noaa.pmel.sgt.dm.SGTData#getXRange()
   */
  public boolean isMissing() {
    return getTime() == Long.MIN_VALUE;
  }

  /**
   * Tests to see if the current GeoDate is less than, equal, or greater than. another Date object.
   *
   * @param o a Date or GeoDate object
   * @return a negative int if anotherDate is greater than this date; 0 if they are equal; or a
   *     positive int it is less. This returns -1 if anotherDate is not a Date object.
   */
  @Override
  public int compareTo(GeoDate anotherDate) {
    if (anotherDate != null) {
      return Long.compare(getTime(), anotherDate.getTime());
    } else return -1;
  }
}
