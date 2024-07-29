/*
 * $Id: SGT3DVector.java,v 1.3 2003/08/22 23:02:38 dwd Exp $
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
import java.beans.PropertyChangeListener;

/**
 * Defines a data object to be a Vector. Interpretation of U and V is determined by the <code>
 * CoordinateSystem</code>. For <code>Cartesian</code>, U and V are the Cartesian vector components.
 * For <code>Polar</code> , U and V are R (radius) and Theta (angle) vector components,
 * respectively.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/08/22 23:02:38 $
 * @since 1.0
 * @see SGTData
 * @see CoordinateSystem
 */
public class SGT3DVector extends SGTVector {
  /**
   * @shapeType AggregationLink
   * @clientRole w comp
   */
  SGTGrid wComp_;

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
      if (vComp_ == null) {
        SGTGrid o = vComp_; // done this way to avoid infinite loop
        vComp_ = null;
        o.releaseResources();
      }
      if (wComp_ == null) {
        SGTGrid o = wComp_; // done this way to avoid infinite loop
        wComp_ = null;
        o.releaseResources();
      }
      if (JPane.debug) String2.log("sgt.dm.SGT3DVector.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public SGT3DVector() {}

  /**
   * Construct a SGTVector from two components. The two components must match in both SGTData and
   * CoordinateSystem Interfaces. Both components must be the same shape.
   *
   * @param uComp U component of the vector
   * @param vComp V component of the vector
   * @param vComp W component of the vector
   */
  public SGT3DVector(SGTGrid uComp, SGTGrid vComp, SGTGrid wComp) {
    uComp_ = uComp;
    vComp_ = vComp;
    wComp_ = wComp;
  }

  /**
   * Create a copy. Creates a shallow copy.
   *
   * @see SGTData
   */
  @Override
  public SGTData copy() {
    SGT3DVector newSGTVector;
    try {
      newSGTVector = (SGT3DVector) clone();
    } catch (CloneNotSupportedException e) {
      newSGTVector = new SGT3DVector(this.uComp_, this.vComp_, this.wComp_);
    }
    return (SGTData) newSGTVector;
  }

  /**
   * Get the W component.
   *
   * @return W component
   */
  public SGTGrid getW() {
    return wComp_;
  }

  /**
   * Set the W component.
   *
   * @param uComp W component
   */
  @Override
  public void setU(SGTGrid wComp) {
    wComp_ = wComp;
  }

  /**
   * Set the vector components.
   *
   * @param uComp U component
   * @param vComp V component
   */
  public void setComponents(SGTGrid uComp, SGTGrid vComp, SGTGrid wComp) {
    uComp_ = uComp;
    vComp_ = vComp;
    wComp_ = wComp;
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
  @Override
  public void setId(String ident) {
    id_ = ident;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    uComp_.addPropertyChangeListener(l);
    vComp_.addPropertyChangeListener(l);
    wComp_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    uComp_.removePropertyChangeListener(l);
    vComp_.removePropertyChangeListener(l);
    wComp_.removePropertyChangeListener(l);
  }
}
