/*
 * $Id: AxisTransform.java,v 1.11 2003/08/22 23:02:31 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTValue;
import gov.noaa.pmel.util.TimeRange;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Abstract base class for cartesian axis transforms. Adds additional functionality to <code>
 * Transform</code> necessary for use with axes.
 *
 * @author Donald Denbo
 * @version $Revision: 1.11 $, $Date: 2003/08/22 23:02:31 $
 * @since 1.0
 */
public abstract class AxisTransform implements Transform {
  protected PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  // serialVersion ref 1.9.2.2
  private static final long serialVersionUID = -1577305732337537031L;
  protected double p1_;
  protected double p2_;
  protected double u1_;
  protected double u2_;
  protected long t1_;
  protected long t2_;
  protected boolean space_;
  protected String ident_;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      changes_ = null;
      if (JPane.debug) String2.log("sgt.AxisTransform.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /**
   * Default constructor. Creates a transform with arguments <code>AxisTransform(0.0, 1.0, 0.0, 1.0)
   * </code>.
   */
  public AxisTransform() {
    this(0.0, 1.0, 0.0, 1.0);
  }

  /**
   * <code>AxisTransform</code> space constructor. This constructor is used to define transforms
   * that use double values.
   *
   * @param p1 minimum value, physical coordinates
   * @param p2 maximum value, physical coordinates
   * @param u1 minimum value, user coordinates
   * @param u2 maximum value, user coordinates
   * @see LinearTransform
   */
  public AxisTransform(double p1, double p2, double u1, double u2) {
    this.p1_ = p1;
    this.p2_ = p2;
    this.u1_ = u1;
    this.u2_ = u2;
    space_ = true;
    computeTransform();
    ident_ = "space transform";
  }

  /**
   * <code>AxisTransform</code> space constructor. This constructor is used to define transforms
   * that use <Range2D> values.
   *
   * @param pr physical coordinate range
   * @param ur user coordinate range
   * @see Range2D
   * @see LinearTransform
   */
  public AxisTransform(Range2D pr, Range2D ur) {
    this(pr.start, pr.end, ur.start, ur.end);
  }

  /**
   * <code>AxisTransform</code> time constructor. This constructor is used to define transforms that
   * use <code>GeoDate</code> values.
   *
   * @param p1 minimum value, physical coordinates
   * @param p2 maximum value, physical coordinates
   * @param t1 minimum time
   * @param t2 maximum time
   * @see GeoDate
   * @see LinearTransform
   */
  public AxisTransform(double p1, double p2, GeoDate t1, GeoDate t2) {
    p1_ = p1;
    p2_ = p2;
    t1_ = t1.getTime();
    t2_ = t2.getTime();
    space_ = false;
    computeTransform();
    ident_ = "time transform";
  }

  /**
   * <code>AxisTransform</code> time constructor. This constructor is used to define transforms that
   * use <code>long</code> values to represent number of milliseconds since 1970-01-01.
   *
   * @since 3.0
   * @param p1 minimum value, physical coordinates
   * @param p2 maximum value, physical coordinates
   * @param t1 minimum time
   * @param t2 maximum time
   * @see LinearTransform
   */
  public AxisTransform(double p1, double p2, long t1, long t2) {
    p1_ = p1;
    p2_ = p2;
    t1_ = t1;
    t2_ = t2;
    space_ = false;
    computeTransform();
    ident_ = "time transform";
  }

  /**
   * <code>AxisTransform</code> time constructor. This constructor is used to define transforms that
   * use <TimeRange> values.
   *
   * @param pr physical coordinates range
   * @param tr time range
   * @see Range2D
   * @see TimeRange
   * @see GeoDate
   * @see LinearTransform
   */
  public AxisTransform(Range2D pr, TimeRange tr) {
    this(pr.start, pr.end, tr.start, tr.end);
  }

  /**
   * <code>AxisTransform</code> SoT constructor. This constructor uses the <code>SoTRange</code>
   * class enabling the construction of a Time or Space transform.
   *
   * @since 2.0
   */
  public AxisTransform(Range2D pr, SoTRange str) {
    if (str.isTime()) {
      t1_ = str.getStart().getLongTime();
      t2_ = str.getEnd().getLongTime();
      space_ = false;
    } else {
      u1_ = ((SoTRange.Double) str).start;
      u2_ = ((SoTRange.Double) str).end;
      space_ = true;
    }
    setRangeP(pr);
    computeTransform();
  }

  /**
   * Set physical coordinate range. <br>
   * <strong>Property Change:</strong> <code>rangeP</code>.
   *
   * @param p1 minimum value, physical coordinates
   * @param p2 maximum value, physical coordinates
   * @see LinearTransform
   */
  @Override
  public void setRangeP(double p1, double p2) {
    if (p1_ != p1 || p2_ != p2) {
      Range2D tempOld = new Range2D(p1_, p2_);
      this.p1_ = p1;
      this.p2_ = p2;
      computeTransform();
      changes_.firePropertyChange("rangeP", tempOld, new Range2D(p1_, p2_));
    }
  }

  /**
   * Set transform identifier.
   *
   * @param id transform identifier
   */
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Get transform identifier.
   *
   * @return identifier
   */
  public String getId() {
    return ident_;
  }

  /**
   * Set physical coordinate range.
   *
   * @param prange physcial coordinate range
   * @see Range2D
   * @see LinearTransform
   */
  @Override
  public void setRangeP(Range2D prange) {
    setRangeP(prange.start, prange.end);
  }

  /**
   * Get the physical coordinate range.
   *
   * @return physcial coordinate range
   * @see Range2D
   */
  @Override
  public Range2D getRangeP() {
    return new Range2D(p1_, p2_);
  }

  /**
   * Set the user coordinate range for space values. <br>
   * <strong>Property Change:</strong> <code>rangeU</code>.
   *
   * @param u1 minimum value, user coordinates
   * @param u2 maximum value, user coordinates
   * @see LinearTransform
   */
  @Override
  public void setRangeU(double u1, double u2) {
    if (u1_ != u1 || u2_ != u2) {
      Range2D tempOld = new Range2D(u1_, u2_);
      this.u1_ = u1;
      this.u2_ = u2;
      space_ = true;
      computeTransform();
      changes_.firePropertyChange("rangeU", tempOld, new Range2D(u1_, u2_));
    }
  }

  /**
   * Set the user coordinate range for <code>Range2D</code> values.
   *
   * @param urange user coordinate range
   * @see Range2D
   * @see LinearTransform
   */
  @Override
  public void setRangeU(Range2D urange) {
    setRangeU(urange.start, urange.end);
  }

  /**
   * Get the user coordinate range for double values.
   *
   * @return user range
   * @see Range2D
   */
  @Override
  public Range2D getRangeU() {
    return new Range2D(u1_, u2_);
  }

  /**
   * Set the user coordinate range for <code>GeoDate</code> values. <br>
   * <strong>Property Change:</strong> <code>rangeU</code>.
   *
   * @param t1 minimum time
   * @param t2 maximum time
   * @see GeoDate
   * @see LinearTransform
   */
  public void setRangeU(GeoDate gt1, GeoDate gt2) {
    setRangeU(gt1.getTime(), gt2.getTime());
  }

  /**
   * @since 3.0
   */
  public void setRangeU(long t1, long t2) {
    if (!(t1_ == t1) || !(t2_ == t2)) {
      SoTRange tempOld = new SoTRange.Time(t1_, t2_);
      t1_ = t1;
      t2_ = t2;
      space_ = false;
      computeTransform();
      changes_.firePropertyChange("rangeU", tempOld, new SoTRange.Time(t1_, t2_));
    }
  }

  /**
   * Set the user coordinate range for <code>TimeRange</code> value.
   *
   * @param trange time range
   * @see TimeRange
   * @see LinearTransform
   */
  public void setRangeU(TimeRange trange) {
    setRangeU(trange.start, trange.end);
  }

  /**
   * Get the user coordinate range for <code>GeoDate</code> values.
   *
   * @return time range
   * @see TimeRange
   */
  public TimeRange getTimeRangeU() {
    return new TimeRange(new GeoDate(t1_), new GeoDate(t2_));
  }

  /**
   * Set the user range with a <code>SoTRange</code> object.
   *
   * @since 2.0
   */
  public void setRangeU(SoTRange str) {
    if (str.isTime()) {
      setRangeU(str.getStart().getLongTime(), str.getEnd().getLongTime());
    } else {
      setRangeU(((SoTRange.Double) str).start, ((SoTRange.Double) str).end);
    }
  }

  /**
   * Get the user range as a <code>SoTRange</code> object.
   *
   * @since 2.0
   */
  public SoTRange getSoTRangeU() {
    if (space_) {
      return new SoTRange.Double(u1_, u2_);
    } else {
      return new SoTRange.Time(t1_, t2_);
    }
  }

  /**
   * Test if transform has user double values.
   *
   * @return true if user coordinates are double values
   */
  public boolean isSpace() {
    return space_;
  }

  /**
   * Test if transform has user <code>GeoDate</code> values.
   *
   * @return true if user coordinates are <code>GeoDate</code> values.
   * @see GeoDate
   */
  public boolean isTime() {
    return !space_;
  }

  abstract double getTransP(GeoDate t);

  /**
   * @since 3.0
   */
  abstract double getTransP(long t);

  abstract GeoDate getTimeTransU(double p);

  /**
   * @since 3.0
   */
  abstract long getLongTimeTransU(double p);

  abstract void computeTransform();

  abstract AxisTransform copy();

  /**
   * Get physical value as a function of <code>SoTValue</code>.
   *
   * @since 2.0
   */
  public abstract double getTransP(SoTValue value);

  /**
   * Get user transform value as a <code>SoTValue</code>
   *
   * @since 2.0
   */
  public abstract SoTValue getSoTTransU(double value);

  /**
   * Add listener to changes in <code>LinearTransform</code> properties.
   *
   * @since 2.0
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changes_.addPropertyChangeListener(listener);
  }

  /**
   * Remove listener from list.
   *
   * @since 2.0
   */
  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changes_.removePropertyChangeListener(listener);
  }
}
