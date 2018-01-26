/*
 * $Id: IndexedColorMap.java,v 1.9 2003/08/22 23:02:32 dwd Exp $
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
 * <code>IndexedColorMap</code> provides a mapping from a value to a
 * <code>Color</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.9 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.0
 */
public class IndexedColorMap extends ColorMap
  implements Cloneable, PropertyChangeListener, IndexedColor, TransformAccess {
  protected Color[] colors_;
  /**
   * index Transform.  Default transform is Linear.
   * @supplierCardinality 0..1
   * @clientRole iTrans_
   * @link aggregationByValue
   */
  private Transform iTrans_ = new LinearTransform(0.0, 1.0, 0.0, 1.0);
  /**
   * Initialize the color map with int arrays of red, green, and blue.
   * The arrays must be the same length. Sets up <code>ColorMap</code>
   * for <code>INDEXED</code> access.
   *
   * @param red Array of the red component 0 - 255.
   * @param green Array of the green component 0 - 255.
   * @param blue Array of the blue component 0 - 255.
   *
   * @see java.awt.Color
   */
  public IndexedColorMap(int[] red,int[] green,int[] blue) {
    int indx;
    colors_ = new Color[red.length];
    for (indx=0; indx < red.length; indx++) {
      colors_[indx] = new Color(red[indx], green[indx], blue[indx]);
    }
    iTrans_.setRangeP(0.0, (double)colors_.length);
  }
  /**
   * Initialize the color map with float arrays of red, green, and blue.
   * The arrays must be the same length. Sets up <code>ColorMap</code>
   * for <code>INDEXED</code> access.
   *
   * @param red Array of the red component 0.0 - 1.0.
   * @param green Array of the green component 0.0 - 1.0.
   * @param blue Array of the blue component 0.0 - 1.0.
   *
   * @see java.awt.Color
   */
  public IndexedColorMap(float[] red,float[] green,float[] blue) {
    int indx;
    colors_ = new Color[red.length];
    for (indx=0; indx < red.length; indx++) {
      colors_[indx] = new Color(red[indx], green[indx], blue[indx]);
    }
    iTrans_.setRangeP(0.0, (double)colors_.length);
  }
  /**
   * Initialize the color map with an array of <code>Color</code>
   * objects. Sets up <code>ColorMap</code> for
   * <code>INDEXED</code> access.
   *
   * @param colors Array of the Color objects.
   *
   * @see java.awt.Color
   */
  public IndexedColorMap(Color[] colors) {
    colors_ = colors;
    iTrans_.setRangeP(0.0, (double)colors_.length);
  }
  /**
   * Create a copy of the <code>ColorMap</code>
   */
  public ColorMap copy() {
    ColorMap newMap;
    try {
      newMap = (ColorMap)clone();
    } catch (CloneNotSupportedException e) {
      newMap = new IndexedColorMap(colors_);
    }
    return newMap;
  }
  /**
   * Get a <code>Color</code>.
   *
   * @return color
   * @since 3.0
   */
  public Color getColorByIndex(int indx) {
    return colors_[indx];
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
    indx = (int)Math.round(iTrans_.getTransP(ival));
    if(indx < 0) indx=0;
    if(indx > colors_.length-1) indx = colors_.length-1;
    return colors_[indx];
  }
  /**
   * Set the user range for the <code>Transform</codes>.
   *
   */
  public void setRange(Range2D range) {
    iTrans_.setRangeU(range);
  }
  /**
   * Get the current user range for the <code>Transform</code>.
   *
   * @return user range
   */
  public Range2D getRange() {
    return iTrans_.getRangeU();
  }
  /**
   * Change the <code>Color</code>.
   *
   * @param colr new <code>Color</code>
   * @param indx index of color
   */
  public void setColor(int index, Color colr) {
    setColor(index, colr.getRed(), colr.getGreen(), colr.getBlue());
  }
  /**
   * Change the <code>Color</code>.
   * <BR><strong>Property Change:</strong> <code>color</code>.
   *
   * @param red red component
   * @param green green component
   * @param blue blue component
   * @param indx index of color
   */
  public void setColor(int indx, int red, int green, int blue) {
    if(indx < 0 || indx > colors_.length) return;
    Color newColor = new Color(red, green, blue);
    if(!colors_[indx].equals(newColor)) {
      Color tempOld = colors_[indx];
      colors_[indx] = newColor;
      firePropertyChange("color",
                                  tempOld,
                                  newColor);
    }
  }
  /**
   * Get the maximum color index.
   *
   * @return maximum legal color index
   */
  public int getMaximumIndex() {
    return colors_.length - 1;
  }
  /**
   * Set the transform for the color mapping.
   * <BR><strong>Property Change:</strong> <code>transform</code>.
   *
   * @param trans index color <code>Transform</code>
   */
  public void setTransform(Transform trans) {
    if(!trans.equals(iTrans_)) {
      Transform tempOld = iTrans_;
      if(iTrans_ != null) iTrans_.removePropertyChangeListener(this);
      iTrans_ = trans;
      iTrans_.setRangeP(0.0, (double)colors_.length);
      firePropertyChange("transform",
                                  tempOld,
                                  iTrans_);
      iTrans_.addPropertyChangeListener(this);
    }
  }
  /**
   * Get the transform for the color mapping.
   *
   * @return index color <code>Transform</code>
   */
  public Transform getTransform() {
    return iTrans_;
  }
  public boolean equals(ColorMap cm) {
    if(cm == null || !(cm instanceof IndexedColorMap)) return false;
    if(!iTrans_.equals(((IndexedColorMap)cm).iTrans_)) return false;
    if(colors_.length != ((IndexedColorMap)cm).colors_.length) return false;
    for(int i=0; i < colors_.length; i++) {
      if(!colors_[i].equals(((IndexedColorMap)cm).colors_[i])) return false;
    }
    return true;
  }
}
