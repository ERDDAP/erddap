/*
 * $Id: Dimension2D.java,v 1.3 2001/02/09 18:42:30 dwd Exp $
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
 * A class to encapsulate a <code>double</code> width and a height.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2001/02/09 18:42:30 $
 * @since sgt 1.0
 */
public class Dimension2D {
  public double height;
  public double width;

  public Dimension2D() {
    width = 0.0;
    height = 0.0;
  }

  public Dimension2D(double width, double height) {
    this.width = width;
    this.height = height;
  }

  /**
   * Returns the width.
   *
   * @return the width
   */
  public double getWidth() {
    return width;
  }

  /**
   * Returns the height.
   *
   * @return the height
   */
  public double getHeight() {
    return height;
  }

  /**
   * Set the size to the specified width and height. This method is included for completeness, to
   * parallel the getSize method of <code>Component</code>.
   *
   * @param width the new width
   * @param height the new height
   */
  public void setSize(double width, double height) {
    this.width = width;
    this.height = height;
  }

  /**
   * Set the size to match the specified size. This method is included for completeness, to parallel
   * the getSize method of <code>Component</code>.
   *
   * @param d the new size
   */
  public void setSize(Dimension2D d) {
    setSize(d.getWidth(), d.getHeight());
  }

  /** */
  @Override
  public String toString() {
    return getClass().getName() + "[width=" + width + ",height=" + height + "]";
  }

  /** Test for equality. Both width and height must be equal to be true. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Dimension2D)) {
      return false;
    }
    Dimension2D d = (Dimension2D) o;
    return (width == d.width && height == d.height);
  }

  @Override
  public int hashCode() {
    int hash = 31 * 7;
    hash = 31 * hash + (int) (width * 31) % java.lang.Integer.MAX_VALUE;
    hash = 31 * hash + (int) (height * 31) % java.lang.Integer.MAX_VALUE;
    return hash;
  }
}
