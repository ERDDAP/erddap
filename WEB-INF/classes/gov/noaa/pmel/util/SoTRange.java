/*
 * $Id: SoTRange.java,v 1.14 2003/08/25 21:14:26 dwd Exp $
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

/**
 * <code>SoTRange</code> is an abstract class used to wrap either a <code>double</code> or <code>
 * GeoDate</code>. SoT stands for space or time, but being basically lazy I've abbreviated it.
 * Contains minimum, maximum, and delta <code>SoTRange</code>. The <code>SoTRange</code> object
 * represents the range of all missing data as <code>Double.NaN</code> as the start and end values
 * for data of type <code>Double</code> and return <code>GeoDate(Long.MIN_VALUE)</code> for data of
 * type <code>GeoDate</code>.
 *
 * @author Donald Denbo
 * @verstion $Revision: 1.14 $ $Date: 2003/08/25 21:14:26 $
 * @since sgt 2.0
 * @see gov.noaa.pmel.sgt.dm.SGTData
 */
public abstract class SoTRange implements java.io.Serializable, Cloneable {
  /**
   * Inner class for <code>SoTRange</code> for type <code>long</code>. Alternative method for
   * storing time range.
   *
   * @since sgt 3.0
   */
  public static class Time extends SoTRange {
    /** The range's first value */
    public long start;

    /** The range's last value */
    public long end;

    /** The value of the increment */
    public long delta;

    /** Default constructor. */
    public Time() {
      this(java.lang.Long.MAX_VALUE, java.lang.Long.MAX_VALUE, java.lang.Long.MAX_VALUE);
    }

    /**
     * Construct <code>SoTRange</code> with start and end. Default for delta is MAX_VALUE
     *
     * @param ustart first value
     * @param uend last value
     */
    public Time(long ustart, long uend) {
      this(ustart, uend, java.lang.Long.MAX_VALUE);
    }

    public Time(gov.noaa.pmel.util.GeoDate ustart, gov.noaa.pmel.util.GeoDate uend) {
      this(ustart.getTime(), uend.getTime(), java.lang.Long.MAX_VALUE);
    }

    /**
     * <code>SoTRange</code> constructor.
     *
     * @param ustart first value
     * @param uend last value
     * @param udel increment value
     */
    public Time(long ustart, long uend, long udel) {
      start = ustart;
      end = uend;
      delta = udel;
    }

    public Time(
        gov.noaa.pmel.util.GeoDate ustart,
        gov.noaa.pmel.util.GeoDate uend,
        gov.noaa.pmel.util.GeoDate udel) {
      this(ustart.getTime(), uend.getTime(), udel.getTime());
    }

    /**
     * @Deprecated use SoTRange
     */
    public Time(TimeRange trange) {
      start = trange.start.getTime();
      end = trange.end.getTime();
      if (trange.delta != null) {
        delta = trange.delta.getTime();
      } else {
        delta = java.lang.Long.MAX_VALUE;
      }
    }

    public Time(SoTRange.Time trange) {
      this(trange.start, trange.end, trange.delta);
    }

    /** Get start value */
    @Override
    public SoTValue getStart() {
      return new SoTValue.Time(start);
    }

    @Override
    public void setStart(SoTValue value) {
      start = ((SoTValue.Time) value).getValue();
    }

    /** Get end value */
    @Override
    public SoTValue getEnd() {
      return new SoTValue.Time(end);
    }

    @Override
    public void setEnd(SoTValue value) {
      end = ((SoTValue.Time) value).getValue();
    }

    /** Get delta value */
    @Override
    public SoTValue getDelta() {
      return new SoTValue.Time(delta);
    }

    @Override
    public void setDelta(SoTValue value) {
      delta = ((SoTValue.Time) value).getValue();
    }

    @Override
    public Object getStartObject() {
      return java.lang.Long.valueOf(start);
    }

    @Override
    public Object getEndObject() {
      return java.lang.Long.valueOf(end);
    }

    @Override
    public Object getDeltaObject() {
      return java.lang.Long.valueOf(delta);
    }

    /**
     * Adds the <code>SoTRange</code> object to this <code>SoTRange</code>. The resulting <code>
     * SoTRange</code> is the smallest <code>SoTRange</code> that contains both the origial <code>
     * SoTRange</code> and the specified <code>SoTRange</code>.
     */
    @Override
    public void add(SoTRange range) {
      if (!range.isTime()) return;
      if ((start <= end) && ((SoTRange.Time) range).start <= ((SoTRange.Time) range).end) {
        start = Math.min(start, ((SoTRange.Time) range).start);
        end = Math.max(end, ((SoTRange.Time) range).end);
      } else {
        start = Math.max(start, ((SoTRange.Time) range).start);
        end = Math.min(end, ((SoTRange.Time) range).end);
      }
    }

    /** Test for equality. For equality start, end, and delta must all be equal. */
    @Override
    public boolean equals(Object r) {
      if (!(r instanceof Time)) {
        return false;
      }
      long rstart = ((SoTRange.Time) r).start;
      long rend = ((SoTRange.Time) r).end;
      long rdelta = ((SoTRange.Time) r).delta;

      if (!(start == java.lang.Long.MAX_VALUE) && !(rstart == java.lang.Long.MAX_VALUE)) {
        if ((start == java.lang.Long.MAX_VALUE) || (rstart == java.lang.Long.MAX_VALUE))
          return false;
        if (start != rstart) return false;
      }
      if (!(end == java.lang.Long.MAX_VALUE) && !(rend == java.lang.Long.MAX_VALUE)) {
        if ((end == java.lang.Long.MAX_VALUE) || (rend == java.lang.Long.MAX_VALUE)) return false;
        if (end != rend) return false;
      }
      if (!(delta == java.lang.Long.MAX_VALUE) && !(rdelta == java.lang.Long.MAX_VALUE)) {
        if ((delta == java.lang.Long.MAX_VALUE) || (rdelta == java.lang.Long.MAX_VALUE))
          return false;
        if (delta != rdelta) return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int max = java.lang.Integer.MAX_VALUE;
      int hash = 31 * 7 + (int) (start % max);
      hash = 31 * hash + (int) (end % max);
      hash = 31 * hash + (int) (delta % max);
      return hash;
    }

    /** Test if <code>SoTRange</code> is temportal */
    @Override
    public boolean isTime() {
      return true;
    }

    /** Exchange start and end values */
    @Override
    public void flipStartAndEnd() {
      long save = end;
      end = start;
      start = save;
    }

    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer(50);
      buf.append("[").append(start).append(";").append(end);
      if (delta == java.lang.Long.MAX_VALUE) {
        buf.append("]");
      } else {
        buf.append(";").append(delta).append("]");
      }
      return buf.toString();
    }

    /** Make a copy of the <code>SoTRange</code>. */
    @Override
    public SoTRange copy() {
      try {
        return (SoTRange) clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    /** Test if start or end values are missing */
    @Override
    public boolean isStartOrEndMissing() {
      return (start == java.lang.Long.MAX_VALUE) || (end == java.lang.Long.MAX_VALUE);
    }
  }

  /**
   * Inner class for <code>SoTRange</code> for type <code>double</code>.
   *
   * @since sgt 2.0
   */
  public static class Double extends SoTRange {
    /** The range's first value */
    public double start;

    /** The range's last value */
    public double end;

    /** The value of the increment */
    public double delta;

    /** Default constructor. */
    public Double() {
      this(java.lang.Double.NaN, java.lang.Double.NaN, java.lang.Double.NaN);
    }

    /**
     * Construct <code>SoTRange</code> with start and end. Default for delta is NaN
     *
     * @param ustart first value
     * @param uend last value
     */
    public Double(double ustart, double uend) {
      this(ustart, uend, java.lang.Double.NaN);
    }

    /**
     * <code>SoTRange</code> constructor.
     *
     * @param ustart first value
     * @param uend last value
     * @param udel increment value
     */
    public Double(double ustart, double uend, double udel) {
      start = ustart;
      end = uend;
      delta = udel;
    }

    /** Construct a <code>SoTRange</code> from a <code>Range2D</code>. */
    public Double(Range2D range) {
      start = range.start;
      end = range.end;
      delta = range.delta;
    }

    /** Get start value */
    @Override
    public SoTValue getStart() {
      return new SoTValue.Double(start);
    }

    @Override
    public void setStart(SoTValue value) {
      start = ((SoTValue.Double) value).getValue();
    }

    /** Get end value */
    @Override
    public SoTValue getEnd() {
      return new SoTValue.Double(end);
    }

    @Override
    public void setEnd(SoTValue value) {
      end = ((SoTValue.Double) value).getValue();
    }

    /** Get delta value */
    @Override
    public SoTValue getDelta() {
      return new SoTValue.Double(delta);
    }

    @Override
    public void setDelta(SoTValue value) {
      delta = ((SoTValue.Double) value).getValue();
    }

    @Override
    public Object getStartObject() {
      return java.lang.Double.valueOf(start);
    }

    @Override
    public Object getEndObject() {
      return java.lang.Double.valueOf(end);
    }

    @Override
    public Object getDeltaObject() {
      return java.lang.Double.valueOf(delta);
    }

    /**
     * Adds the <code>SoTRange</code> object to this <code>SoTRange</code>. The resulting <code>
     * SoTRange</code> is the smallest <code>SoTRange</code> that contains both the origial <code>
     * SoTRange</code> and the specified <code>SoTRange</code>.
     */
    @Override
    public void add(SoTRange range) {
      if (range.isTime()) return;
      if ((start <= end) && ((SoTRange.Double) range).start <= ((SoTRange.Double) range).end) {
        start = Math.min(start, ((SoTRange.Double) range).start);
        end = Math.max(end, ((SoTRange.Double) range).end);
      } else {
        start = Math.max(start, ((SoTRange.Double) range).start);
        end = Math.min(end, ((SoTRange.Double) range).end);
      }
    }

    /** Test for equality. For equality start, end, and delta must all be equal. */
    @Override
    public boolean equals(Object r) {
      if (!(r instanceof Double)) {
        return false;
      }
      double rstart = ((SoTRange.Double) r).start;
      double rend = ((SoTRange.Double) r).end;
      double rdelta = ((SoTRange.Double) r).delta;

      if (!java.lang.Double.isNaN(start) && !java.lang.Double.isNaN(rstart)) {
        if (java.lang.Double.isNaN(start) || java.lang.Double.isNaN(rstart)) return false;
        if (start != rstart) return false;
      }
      if (!java.lang.Double.isNaN(end) && !java.lang.Double.isNaN(rend)) {
        if (java.lang.Double.isNaN(end) || java.lang.Double.isNaN(rend)) return false;
        if (end != rend) return false;
      }
      if (!java.lang.Double.isNaN(delta) && !java.lang.Double.isNaN(rdelta)) {
        if (java.lang.Double.isNaN(delta) || java.lang.Double.isNaN(rdelta)) return false;
        if (delta != rdelta) return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int max = java.lang.Integer.MAX_VALUE;
      int hash = 31 * 7 + (int) (start % max);
      hash = 31 * hash + (int) (end % max);
      hash = 31 * hash + (int) (delta % max);
      return hash;
    }

    /** Test if <code>SoTRange</code> is temportal */
    @Override
    public boolean isTime() {
      return false;
    }

    /** Exchange start and end values */
    @Override
    public void flipStartAndEnd() {
      double save = end;
      end = start;
      start = save;
    }

    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer(50);
      buf.append("[").append(start).append(";").append(end);
      if (java.lang.Double.isNaN(delta)) {
        buf.append("]");
      } else {
        buf.append(";").append(delta).append("]");
      }
      return buf.toString();
    }

    /** Make a copy of the <code>SoTRange</code>. */
    @Override
    public SoTRange copy() {
      try {
        return (SoTRange) clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    /** Test if start or end values are missing */
    @Override
    public boolean isStartOrEndMissing() {
      return java.lang.Double.isNaN(start) || java.lang.Double.isNaN(end);
    }
  }

  /**
   * This is an abstract class that cannot be instantiated directly. Type-specific implementation
   * subclasses are available for instantiation and provide a number of formats for storing the
   * information necessary to satisfy the various accessor methods below.
   */
  protected SoTRange() {}

  public abstract boolean isTime();

  @Override
  public abstract String toString();

  @Override
  public abstract boolean equals(Object range);

  @Override
  public abstract int hashCode();

  public abstract void add(SoTRange range);

  public abstract SoTValue getStart();

  public abstract SoTValue getEnd();

  public abstract SoTValue getDelta();

  public abstract Object getStartObject();

  public abstract Object getEndObject();

  public abstract Object getDeltaObject();

  public abstract void setStart(SoTValue value);

  public abstract void setEnd(SoTValue value);

  public abstract void setDelta(SoTValue value);

  public abstract void flipStartAndEnd();

  public abstract boolean isStartOrEndMissing();

  public abstract SoTRange copy();
}
