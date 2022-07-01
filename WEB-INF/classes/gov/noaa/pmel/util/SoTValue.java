/*
 * $Id: SoTValue.java,v 1.7 2003/08/22 23:02:40 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package  gov.noaa.pmel.util;

import java.io.Serializable;

/**
 * <code>SoTValue</code> is an abstract class used to wrap either a
 * <code>double</code> or <code>GeoDate</code>.  SoT stands for
 * space or time, but being basically lazy I've abbreviated it.
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 2.0
 */
public abstract class SoTValue implements Serializable {
  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>GeoDate</code>.
   * @since sgt 2.0
   * deprecated As of sgt 3.0, replaced by {@link gov.noaa.pmel.util.SoTValue.Time SoTValue.Time}
   */
  public static class GeoDate extends SoTValue {
    gov.noaa.pmel.util.GeoDate date_;
    /**
     * Default constructor.
     */
    public GeoDate() {}
    /**
     * Construct a <code>SoTValue</code> from a <code>GeoDate</code>.
     */
    public GeoDate(gov.noaa.pmel.util.GeoDate date) {
      date_ = date;
    }
    /**
     * Construct a <code>SoTValue</code> and initialize it to
     * represent the specified number of milliseconds since the
     * standard base time known as "the epoch", namely January 1,
     * 1970, 00:00:00.
     */
    public GeoDate(long time) {
      this(new gov.noaa.pmel.util.GeoDate(time));
    }
    /**
     * Get the value
     */
    public gov.noaa.pmel.util.GeoDate getValue() {
      return date_;
    }
    /**
     * Set the value from a <code>GeoDate</code>
     */
    public void setValue(gov.noaa.pmel.util.GeoDate date) {
      date_ = date;
    }
    /**
     * Set the value from the number of milliseconds since the epoch.
     */
    public void setValue(long time) {
      date_ = new gov.noaa.pmel.util.GeoDate(time);
    }
    public Object getObjectValue() {
      return date_;
    }
    /**
     * Test if <code>SoTValue</code> is time.
     */
    public boolean isTime() {
      return true;
    }
    /**
     * Test for equality
     */
    public boolean equals(SoTValue val) {
      if(val.isTime()) {
        return date_.equals(((SoTValue.GeoDate)val).getValue());
      } else {
        return false;
      }
    }
    /**
     * Add to value.
     *
     * @since 3.0
     */
    public void add(SoTValue val) {
      if(!val.isTime()) return;
      date_.add(((SoTValue.GeoDate)val).getValue());
    }
    /**
     * Get time as <code>long</code> since 1970-01-01.
     *
     * @since 3.0
     */
    public long getLongTime() {
      if(date_ == null) return Long.MAX_VALUE;
      return date_.getTime();
    }
    /**
     * Get time as <code>GeoDate</code>.
     *
     * @since 3.0
     */
    public gov.noaa.pmel.util.GeoDate getGeoDate() {
      return new gov.noaa.pmel.util.GeoDate(date_);
    }

    public String toString() {
      return date_.toString();
    }
  }
  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>long</code>.  Used for time storage.
   * @since sgt 3.0
   */
  public static class Time extends SoTValue {
    long value_;
    /**
     * Default constructor.
     */
    public Time() {}
    /**
     * Construct and initialize value.
     */
    public Time(long value) {
      value_ = value;
    }
    public Time(gov.noaa.pmel.util.GeoDate value) {
      value_ = value.getTime();
    }
    /**
     * Get the value
     */
    public long getValue() {
      return value_;
    }
    /**
     * Set the value
     */
    public void setValue(long value) {
      value_ = value;
    }
    public Object getObjectValue() {
      return java.lang.Long.valueOf(value_);
    }
    /**
     * Test if <code>SoTValue</code> is time
     */
    public boolean isTime() {
      return true;
    }
    /**
     * Test for equality
     */
    public boolean equals(SoTValue val) {
      if(val.isTime()) {
        return value_ == ((SoTValue.Time)val).getValue();
      } else {
        return false;
      }
    }
    /**
     * Add to value.
     *
     * @since 3.0
     */
    public void add(SoTValue val) {
      if(!val.isTime()) return;
      long dval = val.getLongTime();
      value_ += dval;
    }
    /**
     * Get time as <code>long</code> since 1970-01-01.
     *
     * @since 3.0
     */
    public long getLongTime() {
      return value_;
    }
    /**
     * Get time as <code>GeoDate</code>.
     *
     * @since 3.0
     */
    public gov.noaa.pmel.util.GeoDate getGeoDate() {
      return new gov.noaa.pmel.util.GeoDate(value_);
    }

    public String toString() {
//      return java.lang.Long.toString(value_);
      return getGeoDate().toString();
    }
  }


  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>int</code>.
   * @since sgt 2.0
   */
  public static class Integer extends SoTValue {
    int value_;
    /**
     * Default constructor.
     */
    public Integer() {}
    /**
     * Construct and initialize value.
     */
    public Integer(int value) {
      value_ = value;
    }
    /**
     * Get the value
     */
    public int getValue() {
      return value_;
    }
    /**
     * Set the value
     */
    public void setValue(int value) {
      value_ = value;
    }
    public Object getObjectValue() {
      return java.lang.Integer.valueOf(value_);
    }
    /**
     * Test if <code>SoTValue</code> is time
     */
    public boolean isTime() {
      return false;
    }
    /**
     * Test for equality
     */
    public boolean equals(SoTValue val) {
      if(val.isTime()) {
        return false;
      } else {
        return value_ == ((SoTValue.Integer)val).getValue();
      }
    }
    /**
     * Add to value.
     *
     * @since 3.0
     */
    public void add(SoTValue val) {
      if(val.isTime()) return;
      int dval = ((Number)val.getObjectValue()).intValue();
      value_ += dval;
    }
    /**
     * Get time as <code>long</code> since 1970-01-01.
     *
     * @since 3.0
     */
    public long getLongTime() {
      throw new Error("Method not appropriate for SoTValue.Int");
    }
    /**
     * Get time as <code>GeoDate</code>.
     *
     * @since 3.0
     */
    public gov.noaa.pmel.util.GeoDate getGeoDate() {
      throw new Error("Method not appropriate for SoTValue.Int");
    }

    public String toString() {
      return java.lang.Integer.toString(value_);
    }
  }

  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>short</code>.
   * @since sgt 2.0
   */
  public static class Short extends SoTValue {
    short value_;
    /**
     * Default constructor.
     */
    public Short() {}
    /**
     * Construct and initialize value.
     */
    public Short(short value) {
      value_ = value;
    }
    /**
     * Get the value
     */
    public short getValue() {
      return value_;
    }
    /**
     * Set the value
     */
    public void setValue(short value) {
      value_ = value;
    }
    public Object getObjectValue() {
      return java.lang.Short.valueOf(value_);
    }
    /**
     * Test if <code>SoTValue</code> is time
     */
    public boolean isTime() {
      return false;
    }
    /**
     * Test for equality
     */
    public boolean equals(SoTValue val) {
      if(val.isTime()) {
        return false;
      } else {
        return value_ == ((SoTValue.Short)val).getValue();
      }
    }
    /**
     * Add to value.
     *
     * @since 3.0
     */
    public void add(SoTValue val) {
      if(val.isTime()) return;
      short dval = ((Number)val.getObjectValue()).shortValue();
      value_ += dval;
    }
    /**
     * Get time as <code>long</code> since 1970-01-01.
     *
     * @since 3.0
     */
    public long getLongTime() {
      throw new Error("Method not appropriate for SoTValue.Short");
    }
    /**
     * Get time as <code>GeoDate</code>.
     *
     * @since 3.0
     */
    public gov.noaa.pmel.util.GeoDate getGeoDate() {
      throw new Error("Method not appropriate for SoTValue.Short");
    }

    public String toString() {
      return java.lang.Short.toString(value_);
    }
  }

  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>float</code>.
   * @since sgt 2.0
   */
  public static class Float extends SoTValue {
    float value_;
    /**
     * Default constructor.
     */
    public Float() {}
    /**
     * Construct and initialize value.
     */
    public Float(float value) {
      value_ = value;
    }
    /**
     * Get the value
     */
    public float getValue() {
      return value_;
    }
    /**
     * Set the value
     */
    public void setValue(float value) {
      value_ = value;
    }
    public Object getObjectValue() {
      return java.lang.Float.valueOf(value_);
    }
    /**
     * Test if <code>SoTValue</code> is time
     */
    public boolean isTime() {
      return false;
    }
    /**
     * Test for equality
     */
    public boolean equals(SoTValue val) {
      if(val.isTime()) {
        return false;
      } else {
        return value_ == ((SoTValue.Float)val).getValue();
      }
    }
    /**
     * Add to value.
     *
     * @since 3.0
     */
    public void add(SoTValue val) {
      if(val.isTime()) return;
      float dval = ((Number)val.getObjectValue()).floatValue();
      value_ += dval;
    }
    /**
     * Get time as <code>long</code> since 1970-01-01.
     *
     * @since 3.0
     */
    public long getLongTime() {
      throw new Error("Method not appropriate for SoTValue.Float");
    }
    /**
     * Get time as <code>GeoDate</code>.
     *
     * @since 3.0
     */
    public gov.noaa.pmel.util.GeoDate getGeoDate() {
      throw new Error("Method not appropriate for SoTValue.Float");
    }

    public String toString() {
      return java.lang.Float.toString(value_);
    }
  }

  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>double</code>.
   * @since sgt 2.0
   */
  public static class Double extends SoTValue {
    double value_;
    /**
     * Default constructor.
     */
    public Double() {}
    /**
     * Construct and initialize value.
     */
    public Double(double value) {
      value_ = value;
    }
    /**
     * Get the value
     */
    public double getValue() {
      return value_;
    }
    /**
     * Set the value
     */
    public void setValue(double value) {
      value_ = value;
    }
    public Object getObjectValue() {
      return java.lang.Double.valueOf(value_);
    }
    /**
     * Test if <code>SoTValue</code> is time
     */
    public boolean isTime() {
      return false;
    }
    /**
     * Test for equality
     */
    public boolean equals(SoTValue val) {
      if(val.isTime()) {
        return false;
      } else {
        return value_ == ((SoTValue.Double)val).getValue();
      }
    }
    /**
     * Add to value.
     *
     * @since 3.0
     */
    public void add(SoTValue val) {
      if(val.isTime()) return;
      double dval = ((Number)val.getObjectValue()).doubleValue();
      value_ += dval;
    }
    /**
     * Get time as <code>long</code> since 1970-01-01.
     *
     * @since 3.0
     */
    public long getLongTime() {
      throw new Error("Method not appropriate for SoTValue.Double");
    }
    /**
     * Get time as <code>GeoDate</code>.
     *
     * @since 3.0
     */
    public gov.noaa.pmel.util.GeoDate getGeoDate() {
      throw new Error("Method not appropriate for SoTValue.Double");
    }
    public String toString() {
      return java.lang.Double.toString(value_);
    }
  }

  /**
   * This is an abstract class that cannot be instantiated directly.
   * Type-specific implementation subclasses are available for
   * instantiation and provide a number of formats for storing
   * the information necessary to satisfy the various accessor
   * methods below.
   *
   */
  protected SoTValue() {}

  public abstract boolean isTime();
  public abstract String toString();
  public abstract boolean equals(SoTValue val);
  public abstract Object getObjectValue();
  public abstract long getLongTime();
  public abstract gov.noaa.pmel.util.GeoDate getGeoDate();
  public abstract void add(SoTValue val);
}

