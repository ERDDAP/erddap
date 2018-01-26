/*
 * $Id: CLTransformColorMap.java,v 1.5 2002/06/12 18:47:26 dwd Exp $
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
 * <code>CLTransformColorMap</code> provides a mapping from a value
 * to a <code>Color</code> via a <code>ContourLevel</code> object.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2002/06/12 18:47:26 $
 * @since 2.0
 */
public class CLTransformColorMap extends ColorMap
  implements Cloneable, PropertyChangeListener, TransformColor, ContourLevelsAccess {
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
   * if not null use LEVEL_INDEXED or LEVEL_TRANSFORM
   * @shapeType AggregationLink
   * @label cl
   */
  private ContourLevels cl_ = null;
  /**
   * Initialize the color map to use red, green, and blue transforms.
   * Each <code>Transform</code> should have identical user
   * ranges.  The physical range will be set to 0.0 to 1.0 for each
   * color component.
   *
   * @see Transform
   */
  public CLTransformColorMap(Transform rTrans,Transform gTrans,Transform bTrans) {
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
   * Get a <code>Color</code>.
   *
   * @param val Value
   * @return Color
   *
   */
  public Color getColor(double val) {
    double ival = val;
    int indx;
    ival = (double)cl_.getIndex(ival)/(double)cl_.getMaximumIndex();
    float red = (float)rTrans_.getTransP(ival);
    float green = (float)gTrans_.getTransP(ival);
    float blue = (float)bTrans_.getTransP(ival);
    return new Color(red, green, blue);
  }
  /**
   * Get the current user range for the <code>Transform</code>s.
   *
   * @return user range
   */
  public Range2D getRange() {
    return cl_.getRange();
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
   * Set the red transform.
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
   * Set the green transform.
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
   * Set the blue transform.
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
  /**
   * Set <code>ContourLevels</code>.
   * <BR><strong>Property Change:</strong> <code>contourLevels</code>.
   *
   * @param cl <code>ContourLevels</code>
   */
  public void setContourLevels(ContourLevels cl) {
    if(!cl_.equals(cl)) {
      ContourLevels tempOld = cl_;
      cl_ = cl;
      firePropertyChange("contourLevels",
                                  tempOld,
                                  cl_);
    }
  }
  /**
   * Get <code>ContourLevels</code> for the color mappings.
   *
   * @return <code>ContourLevels</code>
   */
  public ContourLevels getContourLevels() {
    return cl_;
  }
  /**
   * Test for color map equality
   */
  public boolean equals(ColorMap cm) {
    if(cm == null || !(cm instanceof CLTransformColorMap)) return false;
    if(!cl_.equals(((CLTransformColorMap)cm).cl_)) return false;
    if(!(rTrans_.equals(((CLTransformColorMap)cm).rTrans_) &&
         gTrans_.equals(((CLTransformColorMap)cm).gTrans_) &&
         bTrans_.equals(((CLTransformColorMap)cm).bTrans_))) return false;
    return true;
  }
}
