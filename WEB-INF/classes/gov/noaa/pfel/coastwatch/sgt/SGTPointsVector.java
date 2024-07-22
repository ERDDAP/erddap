/*
 * SGTPointsVector
 * modified from gov.noaa.pmel.sgt.SGTVector.java by Bob Simons (2006-05-11 was bob.simons@noaa.gov, now BobSimons2.00@gmail.com)
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTMetaData;
import gov.noaa.pmel.util.SoTRange;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * Defines a data object to be a Vector. Interpretation of U and V is determined by the <code>
 * CoordinateSystem</code>. For <code>Cartesian</code>, U and V are the Cartesian vector components.
 * For <code>Polar</code> , U and V are R (radius) and Theta (angle) vector components,
 * respectively.
 *
 * @author Donald Denbo
 * @version $Revision: 1.11 $, $Date: 2002/05/16 22:41:29 $
 * @since 1.0
 * @see SGTData
 * @see CoordinateSystem
 */
public class SGTPointsVector implements SGTData, Cloneable, Serializable {
  String title_;
  SGLabel keyTitle_ = null;
  String id_ = null;
  double[] xValues, yValues, uValues, vValues;
  SoTRange xRange, yRange;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      xValues = null;
      yValues = null;
      uValues = null;
      vValues = null;
      if (JPane.debug) String2.log("sgt.SGTPointsVector.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public SGTPointsVector() {}

  /**
   * Construct a SGTPointVector from 4 double[] of the same length.
   *
   * @param xValues the X location of the vectors
   * @param yValues the Y location of the vectors
   * @param uValues the U component of the vectors
   * @param vValues the V component of the vectors
   */
  public SGTPointsVector(double[] xValues, double[] yValues, double[] uValues, double[] vValues) {
    this.xValues = xValues;
    this.yValues = yValues;
    this.uValues = uValues;
    this.vValues = vValues;
    DoubleArray xArray = new DoubleArray(xValues);
    DoubleArray yArray = new DoubleArray(yValues);
    double xStats[] = xArray.calculateStats();
    double yStats[] = yArray.calculateStats();
    xRange =
        new SoTRange.Double(xStats[PrimitiveArray.STATS_MIN], xStats[PrimitiveArray.STATS_MAX]);
    yRange =
        new SoTRange.Double(yStats[PrimitiveArray.STATS_MIN], yStats[PrimitiveArray.STATS_MAX]);
  }

  /**
   * Create a copy. Creates a shallow copy.
   *
   * @see SGTData
   */
  @Override
  public SGTData copy() {
    return new SGTPointsVector(xValues, yValues, uValues, vValues);
  }

  /**
   * Set the vector's title.
   *
   * @param title
   */
  public void setTitle(String title) {
    title_ = title;
  }

  @Override
  public SGLabel getKeyTitle() {
    return keyTitle_;
  }

  /** Set the title formatted for the <code>VectorKey</code>. */
  public void setKeyTitle(SGLabel title) {
    keyTitle_ = title;
  }

  /**
   * Get the unique identifier. The presence of the identifier is optional, but if it is present it
   * should be unique. This field is used to search for the layer that contains the data.
   *
   * @return unique identifier
   * @see gov.noaa.pmel.sgt.Pane
   * @see gov.noaa.pmel.sgt.Layer
   */
  @Override
  public String getId() {
    return id_;
  }

  /** Set the unique identifier. */
  public void setId(String ident) {
    id_ = ident;
  }

  /**
   * Get the vector's title.
   *
   * @return the title
   */
  @Override
  public String getTitle() {
    return title_;
  }

  @Override
  public boolean isXTime() {
    return false;
  }

  @Override
  public boolean isYTime() {
    return false;
  }

  @Override
  public SGTMetaData getXMetaData() {
    return null;
  }

  @Override
  public SGTMetaData getYMetaData() {
    return null;
  }

  @Override
  public SoTRange getXRange() {
    return xRange;
  }

  @Override
  public SoTRange getYRange() {
    return yRange;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {}

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {}
}
