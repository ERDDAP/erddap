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
 * <code>SoTRange</code> is an abstract class used to wrap either a
 * <code>double</code> or <code>GeoDate</code>.  SoT stands for
 * space or time, but being basically lazy I've abbreviated it.
 * Contains minimum, maximum, and delta <code>SoTRange</code>.
 * The <code>SoTRange</code> object represents the range of all
 * missing data as <code>Double.NaN</code>
 * as the start and end values for data of type <code>Double</code>
 * and return <code>GeoDate(Long.MIN_VALUE)</code> for data of type
 * <code>GeoDate</code>.
 *
 * @author Donald Denbo
 * @verstion $Revision: 1.14 $ $Date: 2003/08/25 21:14:26 $
 * @since sgt 2.0
 * @see gov.noaa.pmel.sgt.dm.SGTData
 */
public abstract class SoTRange implements java.io.Serializable, Cloneable {
  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>GeoDate</code>.
   * @since sgt 2.0
   * deprecated As of sgt 3.0, replaced by {@link gov.noaa.pmel.util.SoTRange.Time SoTRange.Time}
   */
  public static class GeoDate extends SoTRange {
    /** The range's first time  */
    public gov.noaa.pmel.util.GeoDate start = null;
    /** The range's last time  */
    public gov.noaa.pmel.util.GeoDate end = null;
    /** The range's time increment  */
    public gov.noaa.pmel.util.GeoDate delta = null;
    /**
     * the Default constructor
     */
    public GeoDate() {
    }
    /**
     * Constructor
     *
     * @param tstart first time
     * @param tend last time
     */
    public GeoDate(gov.noaa.pmel.util.GeoDate tstart,
       gov.noaa.pmel.util.GeoDate tend) {
      this(tstart, tend, null);
    }
    /**
     * Constructor
     *
     * @param tstart first time
     * @param tend last time
     * @param delta time increment
     */
    public GeoDate(gov.noaa.pmel.util.GeoDate tstart,
       gov.noaa.pmel.util.GeoDate tend,
       gov.noaa.pmel.util.GeoDate tdelta) {
      this.start = tstart;
      this.end = tend;
      this.delta = tdelta;
    }
    /**
     * @Deprecated use SoTRange
     */
    public GeoDate(TimeRange trange) {
      start = new gov.noaa.pmel.util.GeoDate(trange.start.getTime());
      end = new gov.noaa.pmel.util.GeoDate(trange.end.getTime());
      if(trange.delta != null) {
        delta = new gov.noaa.pmel.util.GeoDate(trange.delta.getTime());
      } else {
        delta = null;
      }
    }
    /**
     * Get start value
     */
    public SoTValue getStart() {
      return new SoTValue.GeoDate(start);
    }
    public void setStart(SoTValue value) {
      start = ((SoTValue.GeoDate)value).getValue();
    }
    /**
     * Get end value
     */
    public SoTValue getEnd() {
      return new SoTValue.GeoDate(end);
    }
    public void setEnd(SoTValue value) {
      end = ((SoTValue.GeoDate)value).getValue();
    }
    /**
     * Get delta value
     */
    public SoTValue getDelta() {
      return new SoTValue.GeoDate(delta);
    }
    public void setDelta(SoTValue value) {
      delta = ((SoTValue.GeoDate)value).getValue();
    }
    public Object getStartObject() {
      return start;
    }
    public Object getEndObject() {
      return end;
    }
    public Object getDeltaObject() {
      return delta;
    }
    /**
     * Adds the <code>TimeRange</code> object to this
     * <code>TimeRange</code>. The resulting <code>TimeRange</code> is
     * the smallest <code>TimeRange</code> that contains both the
     * origial <code>TimeRange</code> and the specified
     * <code>TimeRange</code>.
     */
    public void add(SoTRange tr) {
      if(!tr.isTime()) return;
      SoTRange.GeoDate tRange = (SoTRange.GeoDate)tr;

      if(tRange.start.before(start)) start = tRange.start;
      if(tRange.end.after(end)) end = tRange.end;
    }
    /**
     * Test for equality.  For equality start, end, and delta must all
     * be equal.
     */
    public boolean equals(SoTRange tr) {
      if(!tr.isTime()) return false;

      SoTRange.GeoDate tRange = (SoTRange.GeoDate)tr;
      if(start != null && tRange.start != null) {
        if(!start.equals(tRange.start)) return false;
      } else {
        return false;
      }
      if(end != null && tRange.end != null) {
        if(!end.equals(tRange.end)) return false;
      } else {
        return false;
      }
      if(delta != null && tRange.delta != null) {
        if(!delta.equals(tRange.delta)) return false;
      } else if(delta == null && tRange.delta == null){
        return true;
      } else {
        return false;
      }
      return true;
    }
    /**
     * Test if <code>SoTRange</code> is temporal.
     */
    public boolean isTime() {
      return true;
    }
    /**
     * Exchange start and end values
     */
    public void flipStartAndEnd() {
      gov.noaa.pmel.util.GeoDate save = end;
      end = start;
      start = save;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer(50);
      buf.append("[").append(start).append(";").append(end);
      if(delta == null) {
        buf.append("]");
      } else {
        buf.append(";").append(delta).append("]");
      }
      return buf.toString();
    }
    /**
     * Make a copy of the <code>SoTRange</code>.
     */
    public SoTRange copy() {
      gov.noaa.pmel.util.GeoDate cStart = null;
      gov.noaa.pmel.util.GeoDate cEnd = null;
      gov.noaa.pmel.util.GeoDate cDelta = null;
      if(start != null)
        cStart = new gov.noaa.pmel.util.GeoDate(start.getTime());
      if(end != null)
        cEnd = new gov.noaa.pmel.util.GeoDate(end.getTime());
      if(delta != null)
        cDelta = new gov.noaa.pmel.util.GeoDate(delta.getTime());

      SoTRange.GeoDate cRange = new SoTRange.GeoDate(cStart, cEnd, cDelta);
      return cRange;
    }
    /**
     * Test if start or end is missing
     */
    public boolean isStartOrEndMissing() {
      if((start == null) || (end == null)) return true;
      if(start.isMissing() || end.isMissing()) return true;
      return false;
    }

  }
  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>long</code>.  Alternative method for storing
   * time range.
   * @since sgt 3.0
   */
  public static class Time extends SoTRange {
    /** The range's first value  */
    public long start;
    /** The range's last value  */
    public long end;
    /** The value of the increment  */
    public long delta;
    /**
     * Default constructor.
     */
    public Time() {
      this(java.lang.Long.MAX_VALUE,
           java.lang.Long.MAX_VALUE,
           java.lang.Long.MAX_VALUE);
    }
    /**
     * Construct <code>SoTRange</code> with start and end. Default for
     * delta is MAX_VALUE
     *
     * @param ustart first value
     * @param uend last value
     */
    public Time(long ustart, long uend) {
      this(ustart, uend, java.lang.Long.MAX_VALUE);
    }
    public Time(gov.noaa.pmel.util.GeoDate ustart,
                gov.noaa.pmel.util.GeoDate uend) {
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
    public Time(gov.noaa.pmel.util.GeoDate ustart,
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
      if(trange.delta != null) {
        delta = trange.delta.getTime();
      } else {
        delta = java.lang.Long.MAX_VALUE;
      }
    }
    /**
     * @Deprecated use SoTRange.Time
     */
    public Time(SoTRange.GeoDate trange) {
      this(trange.start, trange.end, trange.delta);
    }
    public Time(SoTRange.Time trange) {
      this(trange.start, trange.end, trange.delta);
    }
    /**
     * Get start value
     */
    public SoTValue getStart() {
      return new SoTValue.Time(start);
    }
    public void setStart(SoTValue value) {
      start = ((SoTValue.Time)value).getValue();
    }
    /**
     * Get end value
     */
    public SoTValue getEnd() {
      return new SoTValue.Time(end);
    }
    public void setEnd(SoTValue value) {
      end = ((SoTValue.Time)value).getValue();
    }
    /**
     * Get delta value
     */
    public SoTValue getDelta() {
      return new SoTValue.Time(delta);
    }
    public void setDelta(SoTValue value) {
      delta = ((SoTValue.Time)value).getValue();
    }
    public Object getStartObject() {
      return java.lang.Long.valueOf(start);
    }
    public Object getEndObject() {
      return java.lang.Long.valueOf(end);
    }
    public Object getDeltaObject() {
      return java.lang.Long.valueOf(delta);
    }
    /**
     * Adds the <code>SoTRange</code> object to this
     * <code>SoTRange</code>. The resulting <code>SoTRange</code> is
     * the smallest <code>SoTRange</code> that contains both the
     * origial <code>SoTRange</code> and the specified
     * <code>SoTRange</code>.
     */
    public void add(SoTRange range) {
      if(!range.isTime()) return;
      if((start <= end) &&
        ((SoTRange.Time)range).start <= ((SoTRange.Time)range).end) {
        start = Math.min(start, ((SoTRange.Time)range).start);
        end = Math.max(end, ((SoTRange.Time)range).end);
      } else {
        start = Math.max(start, ((SoTRange.Time)range).start);
        end = Math.min(end, ((SoTRange.Time)range).end);
      }
    }
    /**
     * Test for equality.  For equality start, end, and delta must all
     * be equal.
     */
    public boolean equals(SoTRange r) {
      if(r.isTime()) return false;

      long rstart = ((SoTRange.Time)r).start;
      long rend = ((SoTRange.Time)r).end;
      long rdelta = ((SoTRange.Time)r).delta;

      if(!(start == java.lang.Long.MAX_VALUE) &&
         !(rstart == java.lang.Long.MAX_VALUE)) {
        if((start == java.lang.Long.MAX_VALUE) ||
           (rstart == java.lang.Long.MAX_VALUE)) return false;
        if(start != rstart) return false;
      }
      if(!(end == java.lang.Long.MAX_VALUE) &&
         !(rend == java.lang.Long.MAX_VALUE)) {
        if((end == java.lang.Long.MAX_VALUE) ||
           (rend == java.lang.Long.MAX_VALUE)) return false;
        if(end != rend) return false;
      }
      if(!(delta == java.lang.Long.MAX_VALUE) &&
         !(rdelta == java.lang.Long.MAX_VALUE)) {
        if((delta == java.lang.Long.MAX_VALUE) ||
           (rdelta == java.lang.Long.MAX_VALUE)) return false;
        if(delta != rdelta) return false;
      }
      return true;
    }
    /**
     * Test if <code>SoTRange</code> is temportal
     */
    public boolean isTime() {
      return true;
    }
    /**
     * Exchange start and end values
     */
    public void flipStartAndEnd() {
      long save = end;
      end = start;
      start = save;
    }
    public String toString() {
      StringBuffer buf = new StringBuffer(50);
      buf.append("[").append(start).append(";").append(end);
      if(delta == java.lang.Long.MAX_VALUE) {
        buf.append("]");
      } else {
        buf.append(";").append(delta).append("]");
      }
      return buf.toString();
    }
    /**
     * Make a copy of the <code>SoTRange</code>.
     */
    public SoTRange copy() {
      try {
        return (SoTRange)clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
    /**
     * Test if start or end values are missing
     */
    public boolean isStartOrEndMissing() {
      return (start == java.lang.Long.MAX_VALUE) ||
        (end == java.lang.Long.MAX_VALUE);
    }
  }
  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>integer</code>.
   * @since sgt 2.0
   */
  public static class Integer extends SoTRange {
    /** The range's first value  */
    public int start;
    /** The range's last value  */
    public int end;
    /** The value of the increment  */
    public int delta;
    /**
     * Default constructor.
     */
    public Integer() {
      this(java.lang.Integer.MAX_VALUE,
           java.lang.Integer.MAX_VALUE,
           java.lang.Integer.MAX_VALUE);
    }
    /**
     * Construct <code>SoTRange</code> with start and end. Default for
     * delta is MAX_VALUE
     *
     * @param ustart first value
     * @param uend last value
     */
    public Integer(int ustart,int uend) {
      this(ustart, uend, java.lang.Integer.MAX_VALUE);
    }
    /**
     * <code>SoTRange</code> constructor.
     *
     * @param ustart first value
     * @param uend last value
     * @param udel increment value
     */
    public Integer(int ustart, int uend, int udel) {
      start = ustart;
      end = uend;
      delta = udel;
    }
    /**
     * Construct a <code>SoTRange</code> from a <code>Range</code>.
     */
    public Integer(Range range) {
      start = range.start;
      end = range.end;
      delta = java.lang.Integer.MAX_VALUE;
    }
    /**
     * Get start value
     */
    public SoTValue getStart() {
      return new SoTValue.Integer(start);
    }
    public void setStart(SoTValue value) {
      start = ((SoTValue.Integer)value).getValue();
    }
    /**
     * Get end value
     */
    public SoTValue getEnd() {
      return new SoTValue.Integer(end);
    }
    public void setEnd(SoTValue value) {
      end = ((SoTValue.Integer)value).getValue();
    }
    /**
     * Get delta value
     */
    public SoTValue getDelta() {
      return new SoTValue.Integer(delta);
    }
    public void setDelta(SoTValue value) {
      delta = ((SoTValue.Integer)value).getValue();
    }
    public Object getStartObject() {
      return java.lang.Integer.valueOf(start);
    }
    public Object getEndObject() {
      return java.lang.Integer.valueOf(end);
    }
    public Object getDeltaObject() {
      return java.lang.Integer.valueOf(delta);
    }
    /**
     * Adds the <code>SoTRange</code> object to this
     * <code>SoTRange</code>. The resulting <code>SoTRange</code> is
     * the smallest <code>SoTRange</code> that contains both the
     * origial <code>SoTRange</code> and the specified
     * <code>SoTRange</code>.
     */
    public void add(SoTRange range) {
      if(range.isTime()) return;
      if((start <= end) &&
        ((SoTRange.Integer)range).start <= ((SoTRange.Integer)range).end) {
        start = Math.min(start, ((SoTRange.Integer)range).start);
        end = Math.max(end, ((SoTRange.Integer)range).end);
      } else {
        start = Math.max(start, ((SoTRange.Integer)range).start);
        end = Math.min(end, ((SoTRange.Integer)range).end);
      }
    }
    /**
     * Test for equality.  For equality start, end, and delta must all
     * be equal.
     */
    public boolean equals(SoTRange r) {
      if(r.isTime()) return false;

      int rstart = ((SoTRange.Integer)r).start;
      int rend = ((SoTRange.Integer)r).end;
      int rdelta = ((SoTRange.Integer)r).delta;

      if(!(start == java.lang.Integer.MAX_VALUE) &&
         !(rstart == java.lang.Integer.MAX_VALUE)) {
        if((start == java.lang.Integer.MAX_VALUE) ||
           (rstart == java.lang.Integer.MAX_VALUE)) return false;
        if(start != rstart) return false;
      }
      if(!(end == java.lang.Integer.MAX_VALUE) &&
         !(rend == java.lang.Integer.MAX_VALUE)) {
        if((end == java.lang.Integer.MAX_VALUE) ||
           (rend == java.lang.Integer.MAX_VALUE)) return false;
        if(end != rend) return false;
      }
      if(!(delta == java.lang.Integer.MAX_VALUE) &&
         !(rdelta == java.lang.Integer.MAX_VALUE)) {
        if((delta == java.lang.Integer.MAX_VALUE) ||
           (rdelta == java.lang.Integer.MAX_VALUE)) return false;
        if(delta != rdelta) return false;
      }
      return true;
    }
    /**
     * Test if <code>SoTRange</code> is temportal
     */
    public boolean isTime() {
      return false;
    }
    /**
     * Exchange start and end values
     */
    public void flipStartAndEnd() {
      int save = end;
      end = start;
      start = save;
    }
    public String toString() {
      StringBuffer buf = new StringBuffer(50);
      buf.append("[").append(start).append(";").append(end);
      if(delta == java.lang.Integer.MAX_VALUE) {
        buf.append("]");
      } else {
        buf.append(";").append(delta).append("]");
      }
      return buf.toString();
    }
    /**
     * Make a copy of the <code>SoTRange</code>.
     */
    public SoTRange copy() {
      try {
        return (SoTRange)clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
    /**
     * Test if start or end values are missing
     */
    public boolean isStartOrEndMissing() {
      return (start == java.lang.Integer.MAX_VALUE) ||
        (end == java.lang.Integer.MAX_VALUE);
    }
  }
  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>short</code>.
   * @since sgt 2.0
   */
  public static class Short extends SoTRange {
    /** The range's first value  */
    public short start;
    /** The range's last value  */
    public short end;
    /** The value of the increment  */
    public short delta;
    /**
     * Default constructor.
     */
    public Short() {
      this(java.lang.Short.MAX_VALUE,
           java.lang.Short.MAX_VALUE,
           java.lang.Short.MAX_VALUE);
    }
    /**
     * Construct <code>SoTRange</code> with start and end. Default for
     * delta is MAX_VALUE
     *
     * @param ustart first value
     * @param uend last value
     */
    public Short(short ustart,short uend) {
      this(ustart, uend, java.lang.Short.MAX_VALUE);
    }
    /**
     * <code>SoTRange</code> constructor.
     *
     * @param ustart first value
     * @param uend last value
     * @param udel increment value
     */
    public Short(short ustart, short uend, short udel) {
      start = ustart;
      end = uend;
      delta = udel;
    }
//      /**
//       * Construct a <code>SoTRange</code> from a <code>Range2D</code>.
//       */
//      public Double(Range2D range) {
//        start = range.start;
//        end = range.end;
//        delta = range.delta;
//      }
    /**
     * Get start value
     */
    public SoTValue getStart() {
      return new SoTValue.Short(start);
    }
    public void setStart(SoTValue value) {
      start = ((SoTValue.Short)value).getValue();
    }
    /**
     * Get end value
     */
    public SoTValue getEnd() {
      return new SoTValue.Short(end);
    }
    public void setEnd(SoTValue value) {
      end = ((SoTValue.Short)value).getValue();
    }
    /**
     * Get delta value
     */
    public SoTValue getDelta() {
      return new SoTValue.Short(delta);
    }
    public void setDelta(SoTValue value) {
      delta = ((SoTValue.Short)value).getValue();
    }
    public Object getStartObject() {
      return java.lang.Short.valueOf(start);
    }
    public Object getEndObject() {
      return java.lang.Short.valueOf(end);
    }
    public Object getDeltaObject() {
      return java.lang.Short.valueOf(delta);
    }
    /**
     * Adds the <code>SoTRange</code> object to this
     * <code>SoTRange</code>. The resulting <code>SoTRange</code> is
     * the smallest <code>SoTRange</code> that contains both the
     * origial <code>SoTRange</code> and the specified
     * <code>SoTRange</code>.
     */
    public void add(SoTRange range) {
      if(range.isTime()) return;
      if((start <= end) &&
        ((SoTRange.Short)range).start <= ((SoTRange.Short)range).end) {
        start = (short)Math.min(start, ((SoTRange.Short)range).start);
        end = (short)Math.max(end, ((SoTRange.Short)range).end);
      } else {
        start = (short)Math.max(start, ((SoTRange.Short)range).start);
        end = (short)Math.min(end, ((SoTRange.Short)range).end);
      }
    }
    /**
     * Test for equality.  For equality start, end, and delta must all
     * be equal.
     */
    public boolean equals(SoTRange r) {
      if(r.isTime()) return false;

      short rstart = ((SoTRange.Short)r).start;
      short rend = ((SoTRange.Short)r).end;
      short rdelta = ((SoTRange.Short)r).delta;

      if(!(start == java.lang.Short.MAX_VALUE) &&
         !(rstart == java.lang.Short.MAX_VALUE)) {
        if((start == java.lang.Short.MAX_VALUE) ||
           (rstart == java.lang.Short.MAX_VALUE)) return false;
        if(start != rstart) return false;
      }
      if(!(end == java.lang.Short.MAX_VALUE) &&
         !(rend == java.lang.Short.MAX_VALUE)) {
        if((end == java.lang.Short.MAX_VALUE) ||
           (rend == java.lang.Short.MAX_VALUE)) return false;
        if(end != rend) return false;
      }
      if(!(delta == java.lang.Short.MAX_VALUE) &&
         !(rdelta == java.lang.Short.MAX_VALUE)) {
        if((delta == java.lang.Short.MAX_VALUE) ||
           (rdelta == java.lang.Short.MAX_VALUE)) return false;
        if(delta != rdelta) return false;
      }
      return true;
    }
    /**
     * Test if <code>SoTRange</code> is temportal
     */
    public boolean isTime() {
      return false;
    }
    /**
     * Exchange start and end values
     */
    public void flipStartAndEnd() {
      short save = end;
      end = start;
      start = save;
    }
    public String toString() {
      StringBuffer buf = new StringBuffer(50);
      buf.append("[").append(start).append(";").append(end);
      if(delta == java.lang.Short.MAX_VALUE) {
        buf.append("]");
      } else {
        buf.append(";").append(delta).append("]");
      }
      return buf.toString();
    }
    /**
     * Make a copy of the <code>SoTRange</code>.
     */
    public SoTRange copy() {
      try {
        return (SoTRange)clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
    /**
     * Test if start or end values are missing
     */
    public boolean isStartOrEndMissing() {
      return (start == java.lang.Short.MAX_VALUE) ||
        (end == java.lang.Short.MAX_VALUE);
    }
  }
  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>flaot</code>.
   * @since sgt 2.0
   */
  public static class Float extends SoTRange {
    /** The range's first value  */
    public float start;
    /** The range's last value  */
    public float end;
    /** The value of the increment  */
    public float delta;
    /**
     * Default constructor.
     */
    public Float() {
      this(java.lang.Float.NaN,
           java.lang.Float.NaN,
           java.lang.Float.NaN);
    }
    /**
     * Construct <code>SoTRange</code> with start and end. Default for
     * delta is NaN
     *
     * @param ustart first value
     * @param uend last value
     */
    public Float(float ustart,float uend) {
      this(ustart, uend, java.lang.Float.NaN);
    }
    /**
     * <code>SoTRange</code> constructor.
     *
     * @param ustart first value
     * @param uend last value
     * @param udel increment value
     */
    public Float(float ustart,float uend,float udel) {
      start = ustart;
      end = uend;
      delta = udel;
    }
//      /**
//       * Construct a <code>SoTRange</code> from a <code>Range2D</code>.
//       */
//      public Float(Range2D range) {
//        start = range.start;
//        end = range.end;
//        delta = range.delta;
//      }
    /**
     * Get start value
     */
    public SoTValue getStart() {
      return new SoTValue.Float(start);
    }
    public void setStart(SoTValue value) {
      start = ((SoTValue.Float)value).getValue();
    }
    /**
     * Get end value
     */
    public SoTValue getEnd() {
      return new SoTValue.Float(end);
    }
    public void setEnd(SoTValue value) {
      end = ((SoTValue.Float)value).getValue();
    }
    /**
     * Get delta value
     */
    public SoTValue getDelta() {
      return new SoTValue.Float(delta);
    }
    public void setDelta(SoTValue value) {
      delta = ((SoTValue.Float)value).getValue();
    }
    public Object getStartObject() {
      return java.lang.Float.valueOf(start);
    }
    public Object getEndObject() {
      return java.lang.Float.valueOf(end);
    }
    public Object getDeltaObject() {
      return java.lang.Float.valueOf(delta);
    }
    /**
     * Adds the <code>SoTRange</code> object to this
     * <code>SoTRange</code>. The resulting <code>SoTRange</code> is
     * the smallest <code>SoTRange</code> that contains both the
     * origial <code>SoTRange</code> and the specified
     * <code>SoTRange</code>.
     */
    public void add(SoTRange range) {
      if(range.isTime()) return;
      if((start <= end) &&
        ((SoTRange.Float)range).start <= ((SoTRange.Float)range).end) {
        start = Math.min(start, ((SoTRange.Float)range).start);
        end = Math.max(end, ((SoTRange.Float)range).end);
      } else {
        start = Math.max(start, ((SoTRange.Float)range).start);
        end = Math.min(end, ((SoTRange.Float)range).end);
      }
    }
    /**
     * Test for equality.  For equality start, end, and delta must all
     * be equal.
     */
    public boolean equals(SoTRange r) {
      if(r.isTime()) return false;

      float rstart = ((SoTRange.Float)r).start;
      float rend = ((SoTRange.Float)r).end;
      float rdelta = ((SoTRange.Float)r).delta;

      if(!java.lang.Float.isNaN(start) &&
         !java.lang.Float.isNaN(rstart)) {
        if(java.lang.Float.isNaN(start) ||
           java.lang.Float.isNaN(rstart)) return false;
        if(start != rstart) return false;
      }
      if(!java.lang.Float.isNaN(end) &&
         !java.lang.Float.isNaN(rend)) {
        if(java.lang.Float.isNaN(end) ||
           java.lang.Float.isNaN(rend)) return false;
        if(end != rend) return false;
      }
      if(!java.lang.Float.isNaN(delta) &&
         !java.lang.Float.isNaN(rdelta)) {
        if(java.lang.Float.isNaN(delta) ||
           java.lang.Float.isNaN(rdelta)) return false;
        if(delta != rdelta) return false;
      }
      return true;
    }
    /**
     * Test if <code>SoTRange</code> is temportal
     */
    public boolean isTime() {
      return false;
    }
    /**
     * Exchange start and end values
     */
    public void flipStartAndEnd() {
      float save = end;
      end = start;
      start = save;
    }
    public String toString() {
      StringBuffer buf = new StringBuffer(50);
      buf.append("[").append(start).append(";").append(end);
      if(java.lang.Float.isNaN(delta)) {
        buf.append("]");
      } else {
        buf.append(";").append(delta).append("]");
      }
      return buf.toString();
    }
    /**
     * Make a copy of the <code>SoTRange</code>.
     */
    public SoTRange copy() {
      try {
        return (SoTRange)clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
    /**
     * Test if start or end values are missing
     */
    public boolean isStartOrEndMissing() {
      return java.lang.Float.isNaN(start) ||
        java.lang.Float.isNaN(end);
    }
  }
  /**
   * Inner class for <code>SoTRange</code> for type
   * <code>double</code>.
   * @since sgt 2.0
   */
  public static class Double extends SoTRange {
    /** The range's first value  */
    public double start;
    /** The range's last value  */
    public double end;
    /** The value of the increment  */
    public double delta;
    /**
     * Default constructor.
     */
    public Double() {
      this(java.lang.Double.NaN,
           java.lang.Double.NaN,
           java.lang.Double.NaN);
    }
    /**
     * Construct <code>SoTRange</code> with start and end. Default for
     * delta is NaN
     *
     * @param ustart first value
     * @param uend last value
     */
    public Double(double ustart,double uend) {
      this(ustart, uend, java.lang.Double.NaN);
    }
    /**
     * <code>SoTRange</code> constructor.
     *
     * @param ustart first value
     * @param uend last value
     * @param udel increment value
     */
    public Double(double ustart,double uend,double udel) {
      start = ustart;
      end = uend;
      delta = udel;
    }
    /**
     * Construct a <code>SoTRange</code> from a <code>Range2D</code>.
     */
    public Double(Range2D range) {
      start = range.start;
      end = range.end;
      delta = range.delta;
    }
    /**
     * Get start value
     */
    public SoTValue getStart() {
      return new SoTValue.Double(start);
    }
    public void setStart(SoTValue value) {
      start = ((SoTValue.Double)value).getValue();
    }
    /**
     * Get end value
     */
    public SoTValue getEnd() {
      return new SoTValue.Double(end);
    }
    public void setEnd(SoTValue value) {
      end = ((SoTValue.Double)value).getValue();
    }
    /**
     * Get delta value
     */
    public SoTValue getDelta() {
      return new SoTValue.Double(delta);
    }
    public void setDelta(SoTValue value) {
      delta = ((SoTValue.Double)value).getValue();
    }
    public Object getStartObject() {
      return java.lang.Double.valueOf(start);
    }
    public Object getEndObject() {
      return java.lang.Double.valueOf(end);
    }
    public Object getDeltaObject() {
      return java.lang.Double.valueOf(delta);
    }
    /**
     * Adds the <code>SoTRange</code> object to this
     * <code>SoTRange</code>. The resulting <code>SoTRange</code> is
     * the smallest <code>SoTRange</code> that contains both the
     * origial <code>SoTRange</code> and the specified
     * <code>SoTRange</code>.
     */
    public void add(SoTRange range) {
      if(range.isTime()) return;
      if((start <= end) &&
        ((SoTRange.Double)range).start <= ((SoTRange.Double)range).end) {
        start = Math.min(start, ((SoTRange.Double)range).start);
        end = Math.max(end, ((SoTRange.Double)range).end);
      } else {
        start = Math.max(start, ((SoTRange.Double)range).start);
        end = Math.min(end, ((SoTRange.Double)range).end);
      }
    }
    /**
     * Test for equality.  For equality start, end, and delta must all
     * be equal.
     */
    public boolean equals(SoTRange r) {
      if(r.isTime()) return false;

      double rstart = ((SoTRange.Double)r).start;
      double rend = ((SoTRange.Double)r).end;
      double rdelta = ((SoTRange.Double)r).delta;

      if(!java.lang.Double.isNaN(start) &&
         !java.lang.Double.isNaN(rstart)) {
        if(java.lang.Double.isNaN(start) ||
           java.lang.Double.isNaN(rstart)) return false;
        if(start != rstart) return false;
      }
      if(!java.lang.Double.isNaN(end) &&
         !java.lang.Double.isNaN(rend)) {
        if(java.lang.Double.isNaN(end) ||
           java.lang.Double.isNaN(rend)) return false;
        if(end != rend) return false;
      }
      if(!java.lang.Double.isNaN(delta) &&
         !java.lang.Double.isNaN(rdelta)) {
        if(java.lang.Double.isNaN(delta) ||
           java.lang.Double.isNaN(rdelta)) return false;
        if(delta != rdelta) return false;
      }
      return true;
    }
    /**
     * Test if <code>SoTRange</code> is temportal
     */
    public boolean isTime() {
      return false;
    }
    /**
     * Exchange start and end values
     */
    public void flipStartAndEnd() {
      double save = end;
      end = start;
      start = save;
    }
    public String toString() {
      StringBuffer buf = new StringBuffer(50);
      buf.append("[").append(start).append(";").append(end);
      if(java.lang.Double.isNaN(delta)) {
        buf.append("]");
      } else {
        buf.append(";").append(delta).append("]");
      }
      return buf.toString();
    }
    /**
     * Make a copy of the <code>SoTRange</code>.
     */
    public SoTRange copy() {
      try {
        return (SoTRange)clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
    /**
     * Test if start or end values are missing
     */
    public boolean isStartOrEndMissing() {
      return java.lang.Double.isNaN(start) ||
        java.lang.Double.isNaN(end);
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
  protected SoTRange() {}

  public abstract boolean isTime();
  public abstract String toString();
  public abstract boolean equals(SoTRange range);
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
