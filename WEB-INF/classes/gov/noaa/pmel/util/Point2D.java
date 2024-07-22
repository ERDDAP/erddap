/*
 * $Id: Point2D.java,v 1.4 2003/08/22 23:02:40 dwd Exp $
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
 * Point2D will be part of java.java2d.
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 1.0
 */
public abstract class Point2D implements Serializable, Cloneable {
  /**
   * Inner class for <code>Point2D</code> for type <code>double</code>.
   *
   * @since sgt 1.0
   */
  public static class Double extends Point2D {
    /** x coordinate */
    public double x;

    /** y coordinate */
    public double y;

    /** Default constructor */
    public Double() {}

    public Double(double x, double y) {
      this.x = x;
      this.y = y;
    }

    /**
     * Test for equality. Both x and y coordinates must be equal for equality.
     *
     * @since sgt 3.0
     */
    @Override
    public boolean equals(Object pt) {
      if (pt instanceof Point2D.Double) {
        Point2D.Double pt2 = (Point2D.Double) pt;
        return (x == pt2.x) && (y == pt2.y);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return (int) Math.round(7 * 31 * x * y);
    }

    @Override
    public String toString() {
      return new String("(" + x + ", " + y + ")");
    }

    /**
     * Make a copy of the <code>Rectangle2D</code>.
     *
     * @since sgt 3.0
     */
    public Point2D copy() {
      try {
        return (Point2D) clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  /**
   * Inner class for <code>Point2D</code> for type <code>float</code>.
   *
   * @since sgt 2.0
   */
  public static class Float extends Point2D {
    /** x coordinate */
    public float x;

    /** y coordinate */
    public float y;

    /** Default constructor */
    public Float() {}

    public Float(float x, float y) {
      this.x = x;
      this.y = y;
    }

    /**
     * Test for equality. Both x and y coordinates must be equal for equality.
     *
     * @since sgt 3.0
     */
    @Override
    public boolean equals(Object pt) {
      if (pt instanceof Point2D.Float) {
        Point2D.Float pt2 = (Point2D.Float) pt;
        return (x == pt2.x) && (y == pt2.y);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return (int) Math.round(7 * 31 * x * y);
    }

    @Override
    public String toString() {
      return new String("(" + x + ", " + y + ")");
    }

    /**
     * Make a copy of the <code>Rectangle2D</code>.
     *
     * @since sgt 3.0
     */
    public Point2D copy() {
      try {
        return (Point2D) clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  /**
   * This is an abstract class that cannot be instantiated directly. Type-specific implementation
   * subclasses are available for instantiation and provide a number of formats for storing the
   * information necessary to satisfy the various accessor methods below.
   */
  protected Point2D() {}
}
