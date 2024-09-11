/*
 * $Id: LinearTransform.java,v 1.10 2003/08/22 23:02:32 dwd Exp $
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

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTValue;
import gov.noaa.pmel.util.TimeRange;

/**
 * Performs a linear transformation on cartesian axes. If the transformtion is for space the
 * equation is phys = a*user + b and if time is phys = at*time + bt.
 *
 * @author Donald Denbo
 * @version $Revision: 1.10 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 */
public class LinearTransform extends AxisTransform implements Cloneable {
  double at_;
  double bt_;
  double a_;
  double b_;

  /**
   * Default constructor. Creates a transform with arguments <code>Transform(0.0, 1.0, 0.0, 1.0)
   * </code>.
   */
  public LinearTransform() {
    super();
  }

  /**
   * <code>LinearTransform</code> constructor. This constructor is used to define transforms that
   * use double user values.
   *
   * @param p1 minimum value, physical coordinates
   * @param p2 maximum value, physical coordinates
   * @param u1 minimum value, user coordinates
   * @param u2 maximum value, user coordinates
   */
  public LinearTransform(double p1, double p2, double u1, double u2) {
    super(p1, p2, u1, u2);
  }

  /**
   * <code>LinearTransform</code> constructor. This constructor is used to define transforms that
   * use double user values.
   *
   * @param pr physical coordinate range
   * @param ur user coordinate range
   * @see Range2D
   */
  public LinearTransform(Range2D pr, Range2D ur) {
    super(pr, ur);
  }

  /**
   * <code>LinearTransform</code> constructor. This constructor is used to define transforms that
   * use <code>GeoDate</code> user values.
   *
   * @param p1 minimum value, physical coordinates
   * @param p2 maximum value, physical coordinates
   * @param t1 minimum time
   * @param t2 maximum time
   * @see GeoDate
   */
  public LinearTransform(double p1, double p2, GeoDate t1, GeoDate t2) {
    super(p1, p2, t1, t2);
  }

  /**
   * <code>LinearTransform</code> constructor. This constructor is used to define transforms that
   * use <code>GeoDate</code> user values.
   *
   * @param pr physical coordinates range
   * @param tr time range
   * @see Range2D
   * @see TimeRange
   * @see GeoDate
   */
  public LinearTransform(Range2D pr, TimeRange tr) {
    super(pr, tr);
  }

  /**
   * <code>LinearTransform</code> constructor. This constructor is used to define transforms that
   * use <code>SoTRange</code> user values.
   *
   * @since 2.0
   * @param pr physical coordinates range
   * @param str space or time range
   * @see SoTRange
   * @see Range2D
   */
  public LinearTransform(Range2D pr, SoTRange str) {
    super(pr, str);
  }

  /**
   * Transform from user to physical coordinates.
   *
   * @param u user value
   * @return physical value
   */
  @Override
  public double getTransP(double u) {
    return a_ * u + b_;
  }

  /**
   * Create a copy of the <code>LinearTransform</code>.
   *
   * @return the copy
   */
  @Override
  public AxisTransform copy() {
    LinearTransform newTransform;
    try {
      newTransform = (LinearTransform) clone();
    } catch (CloneNotSupportedException e) {
      newTransform = new LinearTransform();
    }
    return (AxisTransform) newTransform;
  }

  //
  /**
   * Transform from time to physical coordinates.
   *
   * @param t time
   * @return user value
   */
  @Override
  public double getTransP(GeoDate t) {
    return (double) (at_ * t.getTime() + bt_);
  }

  @Override
  public double getTransP(SoTValue v) {
    if (v.isTime()) {
      long t = v.getLongTime();
      return (double) at_ * t + bt_;
    } else {
      double u = ((SoTValue.Double) v).getValue();
      return a_ * u + b_;
    }
  }

  /**
   * Transform from <code>long</code> representation of time to physical coordinates.
   *
   * @since 3.0
   */
  @Override
  public double getTransP(long t) {
    return (double) at_ * t + bt_;
  }

  /**
   * Transform from physical to user coordinates.
   *
   * @param p physical value
   * @return user value
   */
  @Override
  public double getTransU(double p) {
    return (p - b_) / a_;
  }

  /**
   * Transform from physical coordinates to time.
   *
   * @param p physical value
   * @return time value
   */
  @Override
  public GeoDate getTimeTransU(double p) {
    return new GeoDate((long) ((p - bt_) / at_));
  }

  /**
   * Transform from physical coordinates to <code>long</code> representation of time.
   *
   * @since 3.0
   * @param p physical value
   * @return milliseconds since 1970-01-01
   */
  @Override
  public long getLongTimeTransU(double p) {
    return (long) ((p - bt_) / at_);
  }

  @Override
  public SoTValue getSoTTransU(double p) {
    if (!space_) {
      return new SoTValue.Time((long) ((p - bt_) / at_));
    } else {
      return new SoTValue.Double((p - b_) / a_);
    }
  }

  //
  @Override
  void computeTransform() {
    if (space_) {
      double denom;
      denom = u1_ - u2_;
      if (denom == 0) {
        a_ = 1.0f;
        b_ = 0.0f;
      } else {
        a_ = (p1_ - p2_) / denom;
        b_ = p1_ - a_ * u1_;
      }
    } else {
      double denom;
      denom = t1_ - t2_;
      if (denom == 0) {
        at_ = 1.0;
        bt_ = 0.0;
      } else {
        at_ = (p1_ - p2_) / denom;
        bt_ = p1_ - at_ * t1_;
      }
    }
  }

  @Override
  public String toString() {
    return "LinearTransform: " + a_ + ", " + b_ + "; " + at_ + ", " + bt_;
  }
}
