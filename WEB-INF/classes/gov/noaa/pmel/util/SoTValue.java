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

package gov.noaa.pmel.util;

import java.io.Serializable;

/**
 * <code>SoTValue</code> is an abstract class used to wrap either a <code>double</code> or <code>
 * GeoDate</code>. SoT stands for space or time, but being basically lazy I've abbreviated it.
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 2.0
 */
public abstract class SoTValue implements Serializable {
  /**
   * Inner class for <code>SoTRange</code> for type <code>long</code>. Used for time storage.
   *
   * @since sgt 3.0
   */
  public static class Time extends SoTValue {
    long value_;

    /** Default constructor. */
    public Time() {}

    /** Construct and initialize value. */
    public Time(long value) {
      value_ = value;
    }

    public Time(gov.noaa.pmel.util.GeoDate value) {
      value_ = value.getTime();
    }

    /** Get the value */
    public long getValue() {
      return value_;
    }

    /** Set the value */
    public void setValue(long value) {
      value_ = value;
    }

    @Override
    public Object getObjectValue() {
      return java.lang.Long.valueOf(value_);
    }

    /** Test if <code>SoTValue</code> is time */
    @Override
    public boolean isTime() {
      return true;
    }

    /** Test for equality */
    @Override
    public boolean equals(Object val) {
      if (!(val instanceof Time)) {
        return false;
      }
      return value_ == ((SoTValue.Time) val).getValue();
    }

    @Override
    public int hashCode() {
      return (int) (31 * value_ % java.lang.Integer.MAX_VALUE);
    }

    /**
     * Add to value.
     *
     * @since 3.0
     */
    @Override
    public void add(SoTValue val) {
      if (!val.isTime()) return;
      long dval = val.getLongTime();
      value_ += dval;
    }

    /**
     * Get time as <code>long</code> since 1970-01-01.
     *
     * @since 3.0
     */
    @Override
    public long getLongTime() {
      return value_;
    }

    /**
     * Get time as <code>GeoDate</code>.
     *
     * @since 3.0
     */
    @Override
    public gov.noaa.pmel.util.GeoDate getGeoDate() {
      return new gov.noaa.pmel.util.GeoDate(value_);
    }

    @Override
    public String toString() {
      //      return java.lang.Long.toString(value_);
      return getGeoDate().toString();
    }
  }

  /**
   * Inner class for <code>SoTRange</code> for type <code>double</code>.
   *
   * @since sgt 2.0
   */
  public static class Double extends SoTValue {
    double value_;

    /** Default constructor. */
    public Double() {}

    /** Construct and initialize value. */
    public Double(double value) {
      value_ = value;
    }

    /** Get the value */
    public double getValue() {
      return value_;
    }

    /** Set the value */
    public void setValue(double value) {
      value_ = value;
    }

    @Override
    public Object getObjectValue() {
      return java.lang.Double.valueOf(value_);
    }

    /** Test if <code>SoTValue</code> is time */
    @Override
    public boolean isTime() {
      return false;
    }

    /** Test for equality */
    @Override
    public boolean equals(Object val) {
      if (!(val instanceof Double)) {
        return false;
      }
      return value_ == ((SoTValue.Double) val).getValue();
    }

    @Override
    public int hashCode() {
      return (int) (31 * value_ % java.lang.Integer.MAX_VALUE);
    }

    /**
     * Add to value.
     *
     * @since 3.0
     */
    @Override
    public void add(SoTValue val) {
      if (val.isTime()) return;
      double dval = ((Number) val.getObjectValue()).doubleValue();
      value_ += dval;
    }

    /**
     * Get time as <code>long</code> since 1970-01-01.
     *
     * @since 3.0
     */
    @Override
    public long getLongTime() {
      throw new Error("Method not appropriate for SoTValue.Double");
    }

    /**
     * Get time as <code>GeoDate</code>.
     *
     * @since 3.0
     */
    @Override
    public gov.noaa.pmel.util.GeoDate getGeoDate() {
      throw new Error("Method not appropriate for SoTValue.Double");
    }

    @Override
    public String toString() {
      return java.lang.Double.toString(value_);
    }
  }

  /**
   * This is an abstract class that cannot be instantiated directly. Type-specific implementation
   * subclasses are available for instantiation and provide a number of formats for storing the
   * information necessary to satisfy the various accessor methods below.
   */
  protected SoTValue() {}

  public abstract boolean isTime();

  @Override
  public abstract String toString();

  @Override
  public abstract boolean equals(Object val);

  @Override
  public abstract int hashCode();

  public abstract Object getObjectValue();

  public abstract long getLongTime();

  public abstract gov.noaa.pmel.util.GeoDate getGeoDate();

  public abstract void add(SoTValue val);
}
