/*
 * $Id: TransformColorMap.java,v 1.6 2002/06/12 18:47:26 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt;

import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Debug;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeEvent;

/**
 * <code>TransformColorMap</code> provides a mapping from a value
 * to a <code>Color</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2002/06/12 18:47:26 $
 * @since 2.0
 */
public class TransformColorMap extends ColorMap
  implements Cloneable, PropertyChangeListener,
             TransformColor, TransformAccess {
  /**
   * Red Transform

   * @supplierCardinality 0..1
   * @clientRole rTrans_
   * @link aggregationByValue
   */
  private Transform rTrans_ = null;
  /**
   * Green Transform

   * @supplierCardinality 0..1
   * @clientRole gTrans_
   * @link aggregationByValue
   */
  private Transform gTrans_ = null;
  /**
   * Blue Transform

   * @supplierCardinality 0..1
   * @clientRole bTrans_
   * @link aggregationByValue
   */
  private Transform bTrans_ = null;
  /**
   * Initialize the color map to use red, green, and blue transforms. Sets up
   * <code>ColorMap</code> for <code>TRANSFORM</code> access.
   * Each <code>Transform</code> should have identical user
   * ranges.  The physical range will be set to 0.0 to 1.0 for each
   * color component.
   *
   * @see Transform
   */
  public TransformColorMap(Transform rTrans,Transform gTrans,Transform bTrans) {
    rTrans_ = rTrans;
    rTrans_.setRangeP(0.0, 1.0);
    gTrans_ = gTrans;
    gTrans_.setRangeP(0.0, 1.0);
    bTrans_ = bTrans;
    bTrans_.setRangeP(0.0, 1.0);
  }
  /**
   * Create a copy of the <code>ColorMap</code> object.
   */
  public ColorMap copy() {
    ColorMap newMap;
    try {
      newMap = (ColorMap)clone();
    } catch (CloneNotSupportedException e) {
      newMap = null;
    }
    return newMap;
  }
  /**
   * Get a <code>Color</code>. Returns a <code>Color</code> by
   * one of four methods. <code>INDEXED</code>, <code>TRANSFORM</code>,
   * <code>LEVEL_INDEXED</code>, and <code>LEVEL_TRANSFORM</code>.
   *
   * @param val Value
   * @return Color
   *
   */
  public Color getColor(double val) {
    double ival = val;
    float red = (float)rTrans_.getTransP(ival);
    float green = (float)gTrans_.getTransP(ival);
    float blue = (float)bTrans_.getTransP(ival);
    System.out.println("TransformColorMap getColor val=" + val + " r=" + red + " g=" + green + " b=" + blue );

    return new Color(red, green, blue);
  }
  /**
   * Set the user range for all the <code>Transform</codes>s.
   *
   */
  public void setRange(Range2D range) {
    rTrans_.setRangeU(range);
    gTrans_.setRangeU(range);
    bTrans_.setRangeU(range);
  }
  /**
   * Get the current user range for the <code>Transform</code>s.
   *
   * @return user range
   */
  public Range2D getRange() {
    return rTrans_.getRangeU();
  }
  /**
   * Set the color <code>Transform</code>s.
   * <BR><strong>Property Change:</strong> <code>redColorTransform</code>,
   * <code>greenColorTransform</code>, and
   * <code>blueColorTransform</code>.
   *
   * @param rTrans red <code>Transform</code>
   * @param gTrans green <code>Transform</code>
   * @param bTrans blue <code>Transform</code>
   */
  public void setColorTransforms(Transform rTrans,
                                 Transform gTrans,
                                 Transform bTrans) {
    if(!rTrans_.equals(rTrans) ||
       !gTrans_.equals(gTrans) ||
       !bTrans_.equals(bTrans)) {
      if(rTrans_ != null) rTrans_.removePropertyChangeListener(this);
      if(gTrans_ != null) gTrans_.removePropertyChangeListener(this);
      if(bTrans_ != null) bTrans_.removePropertyChangeListener(this);

      Transform tempOld = rTrans_;
      rTrans_ = rTrans;
      rTrans_.setRangeP(0.0, 1.0);
      firePropertyChange("redColorTransform",
                                  tempOld,
                                  rTrans_);
      tempOld = gTrans_;
      gTrans_ = gTrans;
      gTrans_.setRangeP(0.0, 1.0);
      firePropertyChange("greenColorTransform",
                                  tempOld,
                                  gTrans_);
      tempOld = bTrans_;
      bTrans_ = bTrans;
      bTrans_.setRangeP(0.0, 1.0);
      firePropertyChange("blueColorTransform",
                                  tempOld,
                                  bTrans_);

      rTrans_.addPropertyChangeListener(this);
      gTrans_.addPropertyChangeListener(this);
      bTrans_.addPropertyChangeListener(this);
    }
  }
  /**
   * Set the red color <code>Transform</code>
   */
  public void setRedTransform(Transform red) {
    rTrans_ = red;
  }
  /**
   * Get the red color <code>Transform</code>.
   *
   * @return red <code>Transform</code>
   */
  public Transform getRedTransform() {
    return rTrans_;
  }
  /**
   * Set the green color <code>Transform</code>
   */
  public void setGreenTransform(Transform green) {
    gTrans_ = green;
  }
  /**
   * Get the green color <code>Transform</code>.
   *
   * @return green <code>Transform</code>
   */
  public Transform getGreenTransform() {
    return gTrans_;
  }
  /**
   * Set the blue color <code>Transform</code>
   */
  public void setBlueTransform(Transform blue) {
    bTrans_ = blue;
  }
  /**
   * Get the blue color <code>Transform</code>.
   *
   * @return blue <code>Transform</code>
   */
  public Transform getBlueTransform() {
    return bTrans_;
  }

  public boolean equals(ColorMap cm) {
    if(cm == null || !(cm instanceof TransformColorMap)) return false;
    if(!(rTrans_.equals(((TransformColorMap)cm).rTrans_) &&
         gTrans_.equals(((TransformColorMap)cm).gTrans_) &&
         bTrans_.equals(((TransformColorMap)cm).bTrans_))) return false;
    return true;
  }
}
