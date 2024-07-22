/*
 * $Id: ColorMap.java,v 1.14 2002/06/14 17:12:25 dwd Exp $
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

import gov.noaa.pmel.util.Debug;
import gov.noaa.pmel.util.Range2D;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * <code>ColorMap</code> provides a mapping from an index or value to a <code>Color</code>. Several
 * methods of mapping an index or value to a <code>Color</code> are summarized below. <br>
 *
 * <DL>
 *   <DT><CODE>IndexedColorMap</CODE>
 *   <DD><CODE>Color</CODE> is determined from an array, the index computed from a <CODE>Transform
 *       </CODE>.
 *   <DT><CODE>TransformColorMap</CODE>
 *   <DD>Red, green, blue <CODE>Color</CODE> components are computed from <CODE>Transform</CODE>s.
 *   <DT><CODE>CLIndexedColorMap</CODE>
 *   <DD><CODE>Color</CODE> is determined from and array, the index computed from a <CODE>
 *       ContourLevels</CODE> object.
 *   <DT><CODE>CLTransformColorMap</CODE>
 *   <DD>Red, green, blue <CODE>Color</CODE> components are computed from <CODE>Transform</CODE>s,
 *       using the index computed from a <CODE>ContourLevels</CODE> object divided by the maximum
 *       index value.
 * </DL>
 *
 * @author Donald Denbo
 * @version $Revision: 1.14 $, $Date: 2002/06/14 17:12:25 $
 * @since 1.0
 */
public abstract class ColorMap implements Cloneable, PropertyChangeListener, Serializable {
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  protected boolean batch_ = false;
  protected boolean local_ = true;
  protected boolean modified_ = false;

  public abstract ColorMap copy();

  /**
   * Get a <code>Color</code>.
   *
   * @param val Value
   * @return Color
   */
  public abstract Color getColor(double val);

  /**
   * Get the current user range for the <code>Transform</code>s or <code>ContourLevel</code>.
   *
   * @return user range
   */
  public abstract Range2D getRange();

  /** Test for equality of color maps. */
  @Override
  public abstract boolean equals(Object cm);

  @Override
  public abstract int hashCode();

  /** Add listener to changes in <code>ColorMap</code> properties. */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changes_.addPropertyChangeListener(listener);
  }

  /** Remove listener. */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changes_.removePropertyChangeListener(listener);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (Debug.EVENT) {
      System.out.println("ColorMap: " + evt);
      System.out.println("          " + evt.getPropertyName());
    }
    firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
  }

  protected void firePropertyChange(String name, Object oldValue, Object newValue) {
    if (batch_) {
      modified_ = true;
      return;
    }
    AttributeChangeEvent ace = new AttributeChangeEvent(this, name, oldValue, newValue, local_);
    changes_.firePropertyChange(ace);
    modified_ = false;
  }

  /**
   * Batch the changes to the ColorMap.
   *
   * @since 3.0
   */
  public void setBatch(boolean batch) {
    setBatch(batch, true);
  }

  /**
   * Batch the changes to the ColorMap and set local flag. Determines whether <code>
   * AttributeChangeEvent</code> will be set local.
   *
   * @since 3.0
   */
  public void setBatch(boolean batch, boolean local) {
    local_ = local;
    batch_ = batch;
    if (!batch && modified_) firePropertyChange("batch", Boolean.TRUE, Boolean.FALSE);
  }

  /**
   * Is the attribute in batch mode?
   *
   * @since 3.0
   */
  public boolean isBatch() {
    return batch_;
  }
}
