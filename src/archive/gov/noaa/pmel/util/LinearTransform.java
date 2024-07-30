/*
 * $Id: LinearTransform.java,v 1.3 2002/12/03 16:35:50 oz Exp $
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

import gov.noaa.pmel.sgt.Transform;
import java.beans.PropertyChangeListener;

/**
 * <code>LinearTransform</code> defines a liniear transformations between user and physical
 * coordinates defined between two end points.
 *
 * @see AxisTransform
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2002/12/03 16:35:50 $
 */
public class LinearTransform implements Transform {
  Range2D mPhysRange = null;
  Range2D mUserRange = null;
  double mPUSlope, mPUYintercept;
  double mUPSlope, mUPYintercept;

  public LinearTransform(double p1, double p2, double u1, double u2) {
    setRangeP(p1, p2);
    setRangeU(u1, u2);
    computeTransforms();
  }

  public LinearTransform(Range2D prange, Range2D urange) {
    setRangeP(prange);
    setRangeU(urange);
    computeTransforms();
  }

  public LinearTransform() {}

  /**
   * Set physical coordinate range.
   *
   * @param p1 minimum value, physical coordinates
   * @param p2 maximum value, physical coordinates
   * @see LinearTransform
   */
  @Override
  public void setRangeP(double p1, double p2) {
    mPhysRange = new Range2D(p1, p2);
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
    mPhysRange = null;
    mPhysRange = new Range2D();
    mPhysRange.add(prange);
  }

  /**
   * Get the physical coordinate range.
   *
   * @return physcial coordinate range
   * @see Range2D
   */
  @Override
  public Range2D getRangeP() {
    return mPhysRange;
  }

  /**
   * Set the user coordinate range for double values.
   *
   * @param u1 minimum value, user coordinates
   * @param u2 maximum value, user coordinates
   * @see LinearTransform
   */
  @Override
  public void setRangeU(double u1, double u2) {
    mUserRange = new Range2D(u1, u2);
  }

  /**
   * Set the user coordinate range for double values.
   *
   * @param urange user coordinate range
   * @see Range2D
   * @see LinearTransform
   */
  @Override
  public void setRangeU(Range2D urange) {
    mUserRange = null;
    mUserRange = new Range2D();
    mUserRange.add(urange);
    computeTransforms();
  }

  /**
   * Get the user coordinate range for double values.
   *
   * @return user range
   * @see Range2D
   */
  @Override
  public Range2D getRangeU() {
    return mUserRange;
  }

  /**
   * Transform from user to physical coordinates.
   *
   * @param u user value
   * @return physical value
   */
  @Override
  public double getTransP(double u) {
    return (mUPSlope * u) + mUPYintercept;
  }

  /**
   * Transform from physical to user coordinates.
   *
   * @param p physical value
   * @return user value
   */
  @Override
  public double getTransU(double p) {
    return (mPUSlope * p) + mPUYintercept;
  }

  /** Add listener for changes to transform properties. */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {}

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {}

  public void computeTransforms() {
    computePULine();
    computeUPLine();
  }

  private void computePULine() {
    double denom = mPhysRange.start - mPhysRange.end;
    double num = mUserRange.start - mUserRange.end;
    mPUSlope = num / denom;
    mPUYintercept = mUserRange.start - (mPUSlope * mPhysRange.start);
  }

  private void computeUPLine() {
    double num = mPhysRange.start - mPhysRange.end;
    double denom = mUserRange.start - mUserRange.end;
    mUPSlope = num / denom;
    mUPYintercept = mPhysRange.start - (mPUSlope * mUserRange.start);
  }

  @Override
  public void releaseResources() throws Exception {
    mPhysRange = null;
    mUserRange = null;
  }
}
