/*
 * $Id: Range2D.java,v 1.3 2001/02/09 18:42:30 dwd Exp $
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
 * Contains minimum, maximum, and delta double values.
 *
 * @author Donald Denbo
 * @verstion $Revision: 1.3 $ $Date: 2001/02/09 18:42:30 $
 * @since sgt 1.0
 */
public class Range2D implements java.io.Serializable, Cloneable {
  /** The range's first value */
  public double start;

  /** The range's last value */
  public double end;

  /** The value of the increment */
  public double delta;

  /** Default constructor. */
  public Range2D() {
    this(Double.NaN, Double.NaN, Double.NaN);
  }

  /**
   * Construct Range2D with start and end. Default for delta is NaN
   *
   * @param ustart first value
   * @param uend last value
   */
  public Range2D(double ustart, double uend) {
    this(ustart, uend, Double.NaN);
  }

  /**
   * Range2D constructor.
   *
   * @param ustart first value
   * @param uend last value
   * @param udel increment value
   */
  public Range2D(double ustart, double uend, double udel) {
    start = ustart;
    end = uend;
    delta = udel;
  }

  /**
   * Adds the <code>Range2D</code> object to this <code>Range2D</code>. The resulting <code>Range2D
   * </code> is the smallest <code>Range2D</code> that contains both the origial <code>Range2D
   * </code> and the specified <code>Range2D</code>.
   */
  public void add(Range2D range) {
    start = Math.min(start, range.start);
    end = Math.max(end, range.end);
  }

  /** Test for equality. Both start, end, and delta must be equal for equality. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Range2D)) {
      return false;
    }
    Range2D r = (Range2D) o;
    if (!Double.isNaN(start) && !Double.isNaN(r.start)) {
      if (Double.isNaN(start) || Double.isNaN(r.start)) return false;
      if (start != r.start) return false;
    }
    if (!Double.isNaN(end) && !Double.isNaN(r.end)) {
      if (Double.isNaN(end) || Double.isNaN(r.end)) return false;
      if (end != r.end) return false;
    }
    if (!Double.isNaN(delta) && !Double.isNaN(r.delta)) {
      if (Double.isNaN(delta) || Double.isNaN(r.delta)) return false;
      if (delta != r.delta) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7 * 31;
    hash = 31 * hash + (int) ((31 * start) % java.lang.Integer.MAX_VALUE);
    hash = 31 * hash + (int) ((31 * end) % java.lang.Integer.MAX_VALUE);
    hash = 31 * hash + (int) ((31 * delta) % java.lang.Integer.MAX_VALUE);
    return hash;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer(50);
    buf.append("[").append(start).append(";").append(end);
    if (Double.isNaN(delta)) {
      buf.append("]");
    } else {
      buf.append(";").append(delta).append("]");
    }
    return buf.toString();
  }

  /** Create a copy of <code>Range2D</code> object. */
  public Range2D copy() {
    try {
      return (Range2D) clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
}
