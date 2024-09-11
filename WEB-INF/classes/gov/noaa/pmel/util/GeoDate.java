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

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <code>GeoDate</code> extends the capabilities of <code>Date</code>. Additional features of <code>
 * GeoDate</code> include methods for incrementing and decrementing, adding and substracting <code>
 * GeoDate</code> objects. All <code>GeoDate</code> objects share the same <code>GregorianCalendar
 * </code> set to a "GMT" time zone. Thus, all <code>GeoDate</code> times are in "GMT". This
 * simplifies the conversion to and from <code>String</code> representations of time.
 *
 * @see GregorianCalendar
 * @author Donald Denbo
 * @version $Revision: 1.17 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 1.0
 */
public class GeoDate extends java.util.Date implements java.io.Serializable {
  private int max_day_[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
  private boolean splitDone_;
  private int yearday_;
  private int dayofweek_;
  private int year_;
  private int month_;
  private int day_;
  private int hour_;
  private int minute_;
  private int second_;
  private int msec_;
  private int MSec_;
  private int JDay_;
  private boolean EPICTimeDone_;
  private boolean relativeTime_ = false;
  private Calendar cal_ =
      new GregorianCalendar(TimeZone.getTimeZone("GMT")); // 2015-09-02 was static!

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
   * Construct a new <code>GeoDate</code> from a <code>String</code>. Formatting is done using
   * <code>SimpleDateFormat</code>. The specified time is taken to be "GMT".
   *
   * @param time character representation of time
   * @param format codes used to read time
   */
  public GeoDate(String time, String format) throws IllegalTimeValue {
    super();
    ParsePosition pos = new ParsePosition(0);
    DateFormat df = new SimpleDateFormat(format);
    df.setCalendar(cal_);

    Date dte = df.parse(time, pos);
    if (dte == null) {
      throw new IllegalTimeValue("Parse error: " + time + ", " + format);
    }
    setTime(dte.getTime());
  }

  /**
   * Constructs a new <code>GeoDate</code> from an existing <code>GeoDate</code>.
   *
   * @param t <code>GeoDate</code>
   */
  public GeoDate(GeoDate t) {
    super(t.getTime());
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
   * Construct a new <code>GeoDate</code> from a <code>Date</code> object. No time zone conversion
   * is done.
   *
   * @param date Date object
   */
  public GeoDate(Date date) {
    setTime(date.getTime());
  }

  /**
   * Construct a new <code>GeoDate</code> from EPIC double integers. Time zone for conversion is
   * "GMT".
   *
   * @param jday julian day
   * @param msec milliseconds since midnight
   */
  public GeoDate(int jday, int msec) {
    set(jday, msec);
  }

  /**
   * Allocates a GeoDate object and initializes it to represent the specified number of milliseconds
   * since the standard base time know as "the epoch", namely January 1, 1970, 00:00:00 GMT.
   */
  public GeoDate(long date) {
    setTime(date);
  }

  /**
   * Set the relativeTime flag. The relativeTime flag indicates that the <code>GeoDate</code> object
   * does not represent an actual absolute time, but a temporal duration.
   */
  public void setRelativeTime(boolean relative) {
    relativeTime_ = relative;
  }

  /**
   * Tests the relativeTime flag.
   *
   * @return if true, time is a duration
   */
  public boolean isRelativeTime() {
    return relativeTime_;
  }

  /**
   * Change value of <code>GeoDate</code> from EPIC double integers. Time zone for conversion is
   * "GMT".
   *
   * @param jday julian day
   * @param msec milliseconds since midnight
   */
  public void set(int jday, int msec) {
    int ja, jb, jc, jd, je;
    double jalpha, second;
    int day, month, year, hour, minute, sec;

    if (jday > 2299161) {
      jalpha = ((double) (jday - 1867216) - 0.25) / 36524.25;
      ja = jday + 1 + (int) jalpha - (int) (0.25 * jalpha);
    } else {
      ja = jday;
    }
    jb = ja + 1524;
    jc = (int) (6680.0 + ((double) (jb - 2439870) - 122.1) / 365.25);
    jd = (int) (365 * jc + (0.25 * jc));
    je = (int) ((jb - jd) / 30.6001);

    day = (int) (jb - jd) - (int) (30.6001 * je);
    month = (int) (je - 1);
    if (month > 12) month -= 12;
    year = (int) (jc - 4715);
    if (month > 2) --year;
    if (year <= 0) --year;
    ja = msec / 1000;
    hour = (int) (ja / 3600);
    minute = (int) ((ja - hour * 3600) / 60);
    second = (double) (msec - (hour * 3600 + minute * 60) * 1000) / 1000.0;
    sec = (int) second;
    msec = ((int) (second * 1000.0)) % 1000;
    try {
      set(month, day, year, hour, minute, sec, msec);
    } catch (IllegalTimeValue e) {
    }
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
    int jy, jm, ja, jul;
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

    cal_.clear();
    cal_.set(year, mon - 1, day, hour, min, sec);

    //    this.setTime(cal.getTimeInMillis() + msec);
    this.setTime(cal_.getTime().getTime() + msec);
    splitDone_ = false;
    EPICTimeDone_ = false;
  }

  /**
   * Get the number of days in the current month.
   *
   * @return number of days in current month
   */
  public int getDaysInMonth() {
    int leap;
    int year = cal_.get(Calendar.YEAR);
    leap = (year % 4 != 0 ? 0 : (year % 400 == 0 ? 1 : (year % 100 == 0 ? 0 : 1)));
    max_day_[1] = 28 + leap;
    return max_day_[month_ - 1];
  }

  /** Set to current time. */
  public void now() {
    Date nw = new Date();
    this.setTime(nw.getTime());
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
    time2.EPICTimeDone_ = false;
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
    delta.EPICTimeDone_ = false;
    delta.setTime(MSec);
    delta.setRelativeTime(true);
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
    result.EPICTimeDone_ = false;
    result.setRelativeTime(true);
    return result;
  }

  void splitTimeFormat() {
    int ja, jb, jc, jd, je;
    double jalpha;
    GeoDate gt;

    if (splitDone_) return;

    cal_.setTime(this);
    day_ = cal_.get(Calendar.DAY_OF_MONTH);
    month_ = cal_.get(Calendar.MONTH) + 1;
    year_ = cal_.get(Calendar.YEAR);
    hour_ = cal_.get(Calendar.HOUR_OF_DAY);
    minute_ = cal_.get(Calendar.MINUTE);
    second_ = cal_.get(Calendar.SECOND);
    msec_ = cal_.get(Calendar.MILLISECOND);
    dayofweek_ = cal_.get(Calendar.DAY_OF_WEEK) - 1;
    yearday_ = cal_.get(Calendar.DAY_OF_YEAR);

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
    double fract = (val - ival);
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
    EPICTimeDone_ = false;
    return this;
  }

  /**
   * Decrement current <code>GeoDate</code> by <code>SECONDS</code>, <code>MINUTES</code>, <code>
   * HOURS</code>, <code>DAYS</code>, <code>MONTHS</code>, or <code>YEARS</code>
   *
   * @param val amount to decrement
   * @param tu time units (<code>SECONDS</code>, <code>MINUTES</code>, <code>HOURS</code>, <code>
   *     DAYS</code>, <code>MONTHS</code>, or <code>YEARS</code>) operation.
   */
  public GeoDate decrement(float val, int tu) {
    return decrement((double) val, tu);
  }

  /**
   * Decrement current <code>GeoDate</code> by <code>SECONDS</code>, <code>MINUTES</code>, <code>
   * HOURS</code>, <code>DAYS</code>, <code>MONTHS</code>, or <code>YEARS</code>
   *
   * @param val amount to decrement
   * @param tu time units (<code>SECONDS</code>, <code>MINUTES</code>, <code>HOURS</code>, <code>
   *     DAYS</code>, <code>MONTHS</code>, or <code>YEARS</code>) operation.
   */
  public GeoDate decrement(double val, int tu) {
    int leap;
    int ival = (int) val;
    double fract = (val - ival);
    long MSec = getTime();

    switch (tu) {
      case MSEC:
        MSec -= (long) val;
        setTime(MSec);
        break;
      case SECONDS:
        MSec -= (long) (val * 1000);
        setTime(MSec);
        break;
      case MINUTES:
        MSec -= (long) (val * 60000);
        setTime(MSec);
        break;
      case HOURS:
        MSec -= (long) (val * 3600000);
        setTime(MSec);
        break;
      case DAYS:
        MSec -= (long) (val * MSECS_IN_DAY);
        setTime(MSec);
        break;
      case MONTHS:
        splitTimeFormat();
        month_ -= ival;
        if (month_ <= 0) {
          year_ -= (month_ - 1) / 12 + 1;
          month_ += ((month_ - 1) / 12) * 12 + 12;
        }
        if (month_ == 0) month_ = 12;
        leap = (year_ % 4 != 0 ? 0 : (year_ % 400 == 0 ? 1 : (year_ % 100 == 0 ? 0 : 1)));
        max_day_[1] = 28 + leap;
        day_ -= (int) (fract * max_day_[month_ - 1]);
        if (day_ > max_day_[month_ - 1]) {
          day_ -= max_day_[month_ - 1];
          month_--;
          if (month_ <= 0) {
            year_ -= (month_ - 1) / 12 + 1;
            month_ += ((month_ - 1) / 12) * 12;
          }
          if (month_ == 0) month_ = 12;
        }
        try {
          this.set(month_, day_, year_, hour_, minute_, second_, msec_);
        } catch (IllegalTimeValue e) {
          System.err.println(e);
        }
        break;
      case YEARS:
        splitTimeFormat();
        year_ -= ival;
        leap = (year_ % 4 != 0 ? 0 : (year_ % 400 == 0 ? 1 : (year_ % 100 == 0 ? 0 : 1)));
        max_day_[1] = 28 + leap;
        if (day_ > max_day_[month_ - 1]) day_ = max_day_[month_ - 1];
        try {
          this.set(month_, day_, year_, hour_, minute_, second_, msec_);
        } catch (IllegalTimeValue e) {
          System.err.println(e);
        }
        setTime(getTime() - (long) ((fract * 365.25) * MSECS_IN_DAY));
        break;
    }
    splitDone_ = false;
    EPICTimeDone_ = false;
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
   * Set a <code>GeoDate</code> from year and year-day.
   *
   * @param year year
   * @param yearday year-day number (Jan 1 = 1) parameters that does not constitute a legitimate
   *     time value.
   */
  public void setYearYearDay(int year, int yearday) {
    try {
      this.set(1, 1, year, 0, 0, 0, 0);
    } catch (IllegalTimeValue e) {
      System.err.println(e);
    }
    setTime(getTime() + (yearday - 1) * MSECS_IN_DAY);
  }

  /**
   * Get a reference to the <code>GregorianCalendar</code> set to the current <code>GeoDate</code>
   * time.
   *
   * @return <code>Calendar</code>
   */
  public Calendar getCalendar() {
    cal_.setTime(this);
    return cal_;
  }

  /** Get year-day number (Jan 1 = 1) */
  public int getYearday() {
    splitTimeFormat();
    return yearday_;
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

  /** Get month @Deprecated Overrides a deprecated method, replaced by {@link #getGMTMonth}. */
  // public int getMonth() {
  //  splitTimeFormat();
  //  return month_;
  // }
  /** Get month */
  public int getGMTMonth() {
    splitTimeFormat();
    return month_;
  }

  /** Get day @Deprecated Overrides a deprecated method, replaced by {@link #getGMTDay}. */
  // public int getDay() {
  //  splitTimeFormat();
  //  return day_;
  // }
  /** Get day */
  public int getGMTDay() {
    splitTimeFormat();
    return day_;
  }

  /** Get hours @Deprecated Overrides a deprecated method, replaced by {@link #getGMTHours}. */
  // public int getHours() {
  //  splitTimeFormat();
  // return hour_;
  // }
  /** Get hours */
  public int getGMTHours() {
    splitTimeFormat();
    return hour_;
  }

  /** Get minutes @Deprecated Overrides a deprecated method, replaced by {@link #getGMTMinutes}. */
  // public int getMinutes() {
  //  splitTimeFormat();
  //  return minute_;
  // }
  /** Get minutes */
  public int getGMTMinutes() {
    splitTimeFormat();
    return minute_;
  }

  /** Get secondss @Deprecated replaced by {@link #getGMTSeconds}. */
  public double getSecondss() {
    splitTimeFormat();
    return second_;
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

  /** get EPIC Julian Day */
  public int getJDay() {
    splitTimeFormat();
    computeEPICTime();
    return JDay_;
  }

  /** get milliseconds since midnight */
  public int getMSec() {
    splitTimeFormat();
    computeEPICTime();
    return MSec_;
  }

  private void computeEPICTime() {
    if (EPICTimeDone_) return;
    int GREGORIAN__ = (15 + 31 * (10 + 12 * 1582));
    int jy, jm, ja, jul;
    int year = year_;
    int mon = month_;
    int day = day_;
    int hour = hour_;
    int min = minute_;
    int sec = second_;
    int msec = msec_;

    int leap = (year % 4 != 0 ? 0 : (year % 400 == 0 ? 1 : (year % 100 == 0 ? 0 : 1)));

    if (year < 0) ++year;
    if (mon > 2) {
      jy = year;
      jm = mon + 1;
    } else {
      jy = year - 1;
      jm = mon + 13;
    }
    jul = (int) (Math.floor(365.25 * jy) + Math.floor(30.6001 * jm) + day + 1720995);
    if (day + 31L * (mon + 12L * year) >= GREGORIAN__) {
      ja = (int) (0.01 * jy);
      jul += 2 - ja + (int) (0.25 * ja);
    }

    JDay_ = jul;
    MSec_ = (int) ((hour * 3600L + min * 60L) * 1000L + sec * 1000L + msec);

    EPICTimeDone_ = true;
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
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss z");
    df.setCalendar(cal_);
    return df.format(this);
  }

  /**
   * Convert <code>GeoDate</code> to <code>String</code> using provided format. The <code>
   * SimpleDateFormat</code> and the <code>GregorianCalendar</code> is used to format the <code>
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
      DateFormat df = new SimpleDateFormat(format);
      df.setCalendar(cal_);
      return df.format(this);
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
  public int compareTo(GeoDate anotherDate) {
    if (anotherDate != null) {
      return Long.compare(getTime(), anotherDate.getTime());
    } else return -1;
  }
}
