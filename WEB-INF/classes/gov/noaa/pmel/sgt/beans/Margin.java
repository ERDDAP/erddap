/*
 * $Id: Margin.java,v 1.2 2003/08/22 23:02:35 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.beans;

import java.io.*;

/**
 * Margin class.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:35 $
 * @since 3.0
 */
public class Margin implements Cloneable, Serializable {
  /** Left margin. */
  public float left = Float.NaN;

  /** Right margin. */
  public float right = Float.NaN;

  /** Top margin. */
  public float top = Float.NaN;

  /** Bottom margin. */
  public float bottom = Float.NaN;

  /** Default constructor. Sets all margin components to 0.0. */
  public Margin() {
    this(0.0f, 0.0f, 0.0f, 0.0f);
  }

  /**
   * Margin constructor.
   *
   * @param top top margin
   * @param left left margin
   * @param bottom bottom margin
   * @param right right margin
   */
  public Margin(float top, float left, float bottom, float right) {
    this.top = top;
    this.left = left;
    this.bottom = bottom;
    this.right = right;
  }

  /**
   * Copy Margin object.
   *
   * @return Margin
   */
  public Margin copy() {
    try {
      return (Margin) clone();
    } catch (CloneNotSupportedException ex) {
    }
    return null;
  }

  /**
   * Set top margin.
   *
   * @param top top margin
   */
  public void setTop(float top) {
    this.top = top;
  }

  /**
   * Get top margin.
   *
   * @return top margin
   */
  public float getTop() {
    return top;
  }

  /**
   * Set bottom margin.
   *
   * @param bottom bottom margin
   */
  public void setBottom(float bottom) {
    this.bottom = bottom;
  }

  /**
   * Get bottom margin
   *
   * @return bottom margin
   */
  public float getBottom() {
    return bottom;
  }

  /**
   * Set left margin.
   *
   * @param left left margin
   */
  public void setLeft(float left) {
    this.left = left;
  }

  /**
   * Get left margin.
   *
   * @return left margin
   */
  public float getLeft() {
    return left;
  }

  /**
   * Set right margin.
   *
   * @param right right margin
   */
  public void setRight(float right) {
    this.right = right;
  }

  /**
   * Get right margin.
   *
   * @return right margin
   */
  public float getRight() {
    return right;
  }

  /**
   * Get string representation of <code>Margin</code>.
   *
   * @return string
   */
  @Override
  public String toString() {
    return Float.toString(top)
        + ", "
        + Float.toString(left)
        + ", "
        + Float.toString(bottom)
        + ", "
        + Float.toString(right);
  }
}
