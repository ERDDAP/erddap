/*
 * $Id: SGTFull3DVector.java,v 1.2 2003/02/06 23:19:33 oz Exp $
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
 * Defines a data object to be a Vector. Interpretation
 * of U and V is determined by the <code>CoordinateSystem</code>.  For
 * <code>Cartesian</code>, U and V are the Cartesian vector
 * components. For <code>Polar</code> ,
 * U and V are R (radius) and Theta (angle) vector components,
 * respectively.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/02/06 23:19:33 $
 * @since 1.0
 * @see SGTData
 * @see CoordinateSystem
 */
public class SGTFull3DVector implements SGTData, Cloneable, Serializable {
  String title_;
  SGLabel keyTitle_ = null;
  String id_ = null;
    /**@shapeType AggregationLink
  * @clientRole u comp*/
    ThreeDGrid uComp_;
    /**@shapeType AggregationLink
  * @clientRole v comp*/
    ThreeDGrid vComp_;
    /**@shapeType AggregationLink
  * @clientRole w comp*/
    ThreeDGrid wComp_;

    /** 
     * Bob Simons added this to avoid memory leak problems.
     */
    public void releaseResources() throws Exception {
        try {  
            keyTitle_ = null;
            uComp_ = null;
            vComp_ = null;
            wComp_ = null;
            if (JPane.debug) String2.log("sgt.dm.SGTFull3DVector.releaseResources() finished");
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            if (JPane.debug) 
                String2.pressEnterToContinue(); 
        }
    }
    
  /**
   * Default constructor.
   */
  public SGTFull3DVector() {
  }
  /**
   * Construct a SGT3DVector from three components. The three components
   * must match in both SGTData and CoordinateSystem Interfaces.
   * All components must be the same shape.
   *
   * @param uComp U component of the vector
   * @param vComp V component of the vector
   * @param wComp W component of the vector
   */
  public SGTFull3DVector(ThreeDGrid uComp, ThreeDGrid vComp, ThreeDGrid wComp) {
    uComp_ = uComp;
    vComp_ = vComp;
    wComp_ = wComp;
  }
  /**
   * Create a copy. Creates a shallow copy.
   *
   * @see SGTData
   */
  public SGTData copy() {
    SGTFull3DVector newSGTVector;
    try {
      newSGTVector = (SGTFull3DVector)clone();
    } catch (CloneNotSupportedException e) {
      newSGTVector = new SGTFull3DVector(this.uComp_, this.vComp_, this.wComp_);
    }
    return newSGTVector;
  }
  /**
   * Get the U component.
   *
   * @return U component
   */
  public ThreeDGrid getU() {
    return uComp_;
  }
  /**
   * Get the V component.
   *
   * @return V component
   */
  public ThreeDGrid getV() {
    return vComp_;
  }
  /**
   * Get the W component.
   *
   * @return W component
   */
  public ThreeDGrid getW() {
    return wComp_;
  }
  /**
   * Set the U component.
   *
   * @param uComp U component
   */
  public void setU(ThreeDGrid uComp) {
    uComp_ = uComp;
  }
  /**
   * Set the V component.
   *
   * @param vComp V component
   */
  public void setV(ThreeDGrid vComp) {
    vComp_ = vComp;
  }
  /**
   * Set the W component.
   *
   * @param vComp W component
   */
  public void setW(ThreeDGrid wComp) {
    wComp_ = wComp;
  }
  /**
   * Set the vector components.
   *
   * @param uComp U component
   * @param vComp V component
   */
  public void setComponents(ThreeDGrid uComp, ThreeDGrid vComp, ThreeDGrid wComp) {
    uComp_ = uComp;
    vComp_ = vComp;
    wComp_ = wComp;
  }
  /**
   * Set the vector's title.
   *
   * @param title
   */
  public void setTitle(String title) {
    title_ = title;
  }
  public SGLabel getKeyTitle() {
    return keyTitle_;
  }
  /** Set the title formatted for the <code>VectorKey</code>. */
  public void setKeyTitle(SGLabel title) {
    keyTitle_ = title;
  }
  /**
   * Get the unique identifier.  The presence of the identifier
   * is optional, but if it is present it should be unique.  This
   * field is used to search for the layer that contains the data.
   *
   * @return unique identifier
   * @see gov.noaa.pmel.sgt.Pane
   * @see gov.noaa.pmel.sgt.Layer
   */
  public String getId() {
    return id_;
  }
  /**
   * Set the unique identifier.
   */
  public void setId(String ident) {
    id_ = ident;
  }
  /**
   * Get the vector's title.
   *
   * @return the title
   */
  public String getTitle() {
    return title_;
  }
  public boolean isXTime() {
    return uComp_.isXTime();
  }
  public boolean isYTime() {
    return uComp_.isYTime();
  }
  public boolean isZTime() {
    return uComp_.isZTime();
  }
  public SGTMetaData getXMetaData() {
    return uComp_.getXMetaData();
  }
  public SGTMetaData getYMetaData() {
    return uComp_.getYMetaData();
  }
  public SGTMetaData getZMetaData() {
    return uComp_.getZMetaData();
  }
  public SoTRange getXRange() {
    return uComp_.getXRange();
  }
  public SoTRange getYRange() {
    return uComp_.getYRange();
  }
  public SoTRange getZRange() {
    return uComp_.getZRange();
  }
  public void addPropertyChangeListener(PropertyChangeListener l) {
    uComp_.addPropertyChangeListener(l);
    vComp_.addPropertyChangeListener(l);
    wComp_.addPropertyChangeListener(l);
  }
  public void removePropertyChangeListener(PropertyChangeListener l) {
    uComp_.removePropertyChangeListener(l);
    vComp_.removePropertyChangeListener(l);
    wComp_.removePropertyChangeListener(l);
  }
}
