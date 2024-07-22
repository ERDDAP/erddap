/*
 * $Id: TimePoint.java,v 1.4 2003/08/22 23:02:40 dwd Exp $
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
 * TimePoint allows specification of a time-space point.
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 1.0 @Deprecated As of sgt 3.0, replaced by {@link gov.noaa.pmel.util.SoTPoint
 *     SoTPoint}.
 */
public class TimePoint {
  /** Space coordinate */
  public double x;

  /** Time coordinate */
  public GeoDate t;

  /** Default constructor. */
  public TimePoint() {}

  /**
   * Construct a TimePoint.
   *
   * @param x space coordinate
   * @param t time coordinate
   */
  public TimePoint(double x, GeoDate t) {
    this.x = x;
    this.t = t;
  }

  /** Test for equality. Both x and t must be equal for equality. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TimePoint)) {
      return false;
    }
    TimePoint tp = (TimePoint) o;
    if (t != null && tp.t != null) {
      return (x == tp.x && t.equals(tp.t));
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = t.hashCode();
    hash = 31 * hash + (int) ((31 * x) % java.lang.Integer.MAX_VALUE);
    return hash;
  }

  /**
   * Convert TimePoint to a default string
   *
   * @return string representation of the TimePoint.
   */
  @Override
  public String toString() {
    return new String("(" + x + ", " + t.toString() + ")");
  }
}
