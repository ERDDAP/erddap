/*
 * $Id: SGTVector.java,v 1.11 2002/05/16 22:41:29 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.dm;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.SGLabel;
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
public class SGTVector implements SGTData, Cloneable, Serializable {
  String title_;
  SGLabel keyTitle_ = null;
  String id_ = null;

  /**
   * @shapeType AggregationLink
   * @clientRole u comp
   */
  SGTGrid uComp_;

  /**
   * @shapeType AggregationLink
   * @clientRole v comp
   */
  SGTGrid vComp_;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      keyTitle_ = null;
      if (uComp_ == null) {
        SGTGrid o = uComp_; // done this way to avoid infinite loop
        uComp_ = null;
        o.releaseResources();
      }
      if (vComp_ == null) { // done this way to avoid infinite loop
        SGTGrid o = vComp_;
        vComp_ = null;
        o.releaseResources();
      }
      if (JPane.debug) String2.log("sgt.dm.SGTVector.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public SGTVector() {}

  /**
   * Construct a SGTVector from two components. The two components must match in both SGTData and
   * CoordinateSystem Interfaces. Both components must be the same shape.
   *
   * @param uComp U component of the vector
   * @param vComp V component of the vector
   */
  public SGTVector(SGTGrid uComp, SGTGrid vComp) {
    uComp_ = uComp;
    vComp_ = vComp;
  }

  /**
   * Create a copy. Creates a shallow copy.
   *
   * @see SGTData
   */
  @Override
  public SGTData copy() {
    SGTVector newSGTVector;
    try {
      newSGTVector = (SGTVector) clone();
    } catch (CloneNotSupportedException e) {
      newSGTVector = new SGTVector(this.uComp_, this.vComp_);
    }
    return (SGTData) newSGTVector;
  }

  /**
   * Get the U component.
   *
   * @return U component
   */
  public SGTGrid getU() {
    return uComp_;
  }

  /**
   * Get the V component.
   *
   * @return V component
   */
  public SGTGrid getV() {
    return vComp_;
  }

  /**
   * Set the U component.
   *
   * @param uComp U component
   */
  public void setU(SGTGrid uComp) {
    uComp_ = uComp;
  }

  /**
   * Set the V component.
   *
   * @param vComp V component
   */
  public void setV(SGTGrid vComp) {
    vComp_ = vComp;
  }

  /**
   * Set the vector components.
   *
   * @param uComp U component
   * @param vComp V component
   */
  public void setComponents(SGTGrid uComp, SGTGrid vComp) {
    uComp_ = uComp;
    vComp_ = vComp;
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
    return uComp_.isXTime();
  }

  @Override
  public boolean isYTime() {
    return uComp_.isYTime();
  }

  @Override
  public SGTMetaData getXMetaData() {
    return uComp_.getXMetaData();
  }

  @Override
  public SGTMetaData getYMetaData() {
    return uComp_.getYMetaData();
  }

  @Override
  public SoTRange getXRange() {
    return uComp_.getXRange();
  }

  @Override
  public SoTRange getYRange() {
    return uComp_.getYRange();
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    uComp_.addPropertyChangeListener(l);
    vComp_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    uComp_.removePropertyChangeListener(l);
    vComp_.removePropertyChangeListener(l);
  }
}
