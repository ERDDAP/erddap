/*
 * $Id: Rectangle2D.java,v 1.5 2003/08/22 23:02:40 dwd Exp $
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
 * Rectangle2D will be part of java.java2d
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 1.0
 */
public abstract class Rectangle2D implements Serializable, Cloneable {
  /**
   * Inner class that implements <code>Rectangle2D</code> for type <code>double</code>.
   *
   * @since sgt 1.0
   */
  public static class Double extends Rectangle2D {
    /** height of rectangle */
    public double height;

    /** width of rectangle */
    public double width;

    /** x coordinate of rectangle */
    public double x;

    /** y coordinate of rectangle */
    public double y;

    /** Default constructor */
    public Double() {}

    public Double(double x, double y, double width, double height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }

    public Double(double width, double height) {
      this.width = width;
      this.height = height;
    }

    public Double(Rectangle2D.Double r) {
      x = r.x;
      y = r.y;
      width = r.width;
      height = r.height;
    }

    /** Test for equality. Height, width, x, and y must be equal for equality. */
    @Override
    public boolean equals(Object r) {
      if (r instanceof Rectangle2D.Double) {
        Rectangle2D.Double r2 = (Rectangle2D.Double) r;
        return !(x != r2.x || y != r2.y || width != r2.width || height != r2.height);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return (int) Math.round(7 * 31 * x * y * width * height);
    }

    @Override
    public String toString() {
      String result;
      result = "[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
      return result;
    }

    /**
     * @since 3.0
     */
    public void setWidth(double w) {
      width = w;
    }

    /**
     * @since 3.0
     */
    public double getWidth() {
      return width;
    }

    /**
     * @since 3.0
     */
    public void setHeight(double h) {
      height = h;
    }

    /**
     * @since 3.0
     */
    public double getHeight() {
      return height;
    }

    /**
     * @since 3.0
     */
    public void setX(double x) {
      this.x = x;
    }

    /**
     * @since 3.0
     */
    public double getX() {
      return x;
    }

    /**
     * @since 3.0
     */
    public void setY(double y) {
      this.y = y;
    }

    /**
     * @since 3.0
     */
    public double getY() {
      return y;
    }

    /** Make a copy of the <code>Rectangle2D</code>. */
    @Override
    public Rectangle2D copy() {
      try {
        return (Rectangle2D) clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  /**
   * Inner class that implements <code>Rectangle2D</code> for type <code>float</code>.
   *
   * @since sgt 1.0
   */
  public static class Float extends Rectangle2D {
    /** height of rectangle */
    public float height;

    /** width of rectangle */
    public float width;

    /** x coordinate of rectangle */
    public float x;

    /** y coordinate of rectangle */
    public float y;

    /** Default constructor */
    public Float() {}

    public Float(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }

    public Float(float width, float height) {
      this.width = width;
      this.height = height;
    }

    public Float(Rectangle2D.Float r) {
      x = r.x;
      y = r.y;
      width = r.width;
      height = r.height;
    }

    /** Test for equality. Height, width, x, and y must be equal for equality. */
    @Override
    public boolean equals(Object r) {
      if (r instanceof Rectangle2D.Float) {
        Rectangle2D.Float r2 = (Rectangle2D.Float) r;
        return !(x != r2.x || y != r2.y || width != r2.width || height != r2.height);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return (int) Math.round(7 * 31 * x * y * width * height);
    }

    @Override
    public String toString() {
      String result;
      result = "[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
      return result;
    }

    /**
     * @since 3.0
     */
    public void setWidth(float w) {
      width = w;
    }

    /**
     * @since 3.0
     */
    public float getWidth() {
      return width;
    }

    /**
     * @since 3.0
     */
    public void setHeight(float h) {
      height = h;
    }

    /**
     * @since 3.0
     */
    public float getHeight() {
      return height;
    }

    /**
     * @since 3.0
     */
    public void setX(float x) {
      this.x = x;
    }

    /**
     * @since 3.0
     */
    public float getX() {
      return x;
    }

    /**
     * @since 3.0
     */
    public void setY(float y) {
      this.y = y;
    }

    /**
     * @since 3.0
     */
    public float getY() {
      return y;
    }

    /** Make a copy of the <code>Rectangle2D</code>. */
    @Override
    public Rectangle2D copy() {
      try {
        return (Rectangle2D) clone();
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
  protected Rectangle2D() {}

  public abstract Rectangle2D copy();
}
