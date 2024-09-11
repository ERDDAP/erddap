/*
 * $Id: GridAttribute.java,v 1.19 2003/08/22 23:02:32 dwd Exp $
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

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.util.Debug;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Sets the rendering style for grid data. <code>ColorMap</code>, <code>ContourLevels</code> are
 * <code>GridAttribute</code> properties.
 *
 * @author Donald Denbo
 * @version $Revision: 1.19 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 * @see GridCartesianRenderer
 * @see ContourLevels
 */
public class GridAttribute implements Attribute, Cloneable, PropertyChangeListener {
  private transient PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  // serial version ref 1.18
  private static final long serialVersionUID = 3822340406728567524L;
  private boolean batch_ = false;
  private boolean local_ = true;
  private boolean modified_ = false;
  private String id_ = null;

  /**
   * @shapeType AggregationLink
   * @label cmap
   */
  private ColorMap cmap_;

  /**
   * @shapeType AggregationLink
   * @label clev
   */
  private ContourLevels clev_;

  private int style_;

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception {
    try {
      changes_ = null;
      cmap_ = null;
      if (clev_ != null) {
        ContourLevels o = clev_; // done this way to avoid infinite loop
        clev_ = null;
        o.releaseResources();
      }
      if (JPane.debug) String2.log("sgt.GridAttribute.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Raster style. */
  public static final int RASTER = 0;

  /** Area fill style. */
  public static final int AREA_FILL = 1;

  /** Contour line style. */
  public static final int CONTOUR = 2;

  /**
   * Raster and Contour style.
   *
   * @since 2.0
   */
  public static final int RASTER_CONTOUR = 3;

  /**
   * Area fill and Contour style.
   *
   * @since 2.0
   */
  public static final int AREA_FILL_CONTOUR = 4;

  /**
   * Default constructor. Default style is <code>RASTER</code> and default <code>ColorMap</code> is
   * null.
   */
  public GridAttribute() {
    this(RASTER, null);
  }

  /**
   * <code>GridAttribute</code> constructor for <code>RASTER</code> and <code>AREA_FILL</code>
   * styles.
   *
   * @param style grid style
   * @param cmap <code>ColorMap</code>
   */
  public GridAttribute(int style, ColorMap cmap) {
    style_ = style;
    cmap_ = cmap;
    if (cmap_ != null) cmap_.addPropertyChangeListener(this);
  }

  /**
   * <code>GridAttribute</code> constructor for <code>CONTOUR</code> style.
   *
   * @param clev <code>ContourLevels</code>
   */
  public GridAttribute(ContourLevels clev) {
    style_ = CONTOUR;
    cmap_ = null;
    clev_ = clev;
  }

  /**
   * Set the <code>ContourLevels</code>. <br>
   * <strong>Property Change:</strong> <code>contourLevels</code>.
   *
   * @param clev <code>ContourLevels</code>
   */
  public void setContourLevels(ContourLevels clev) {
    if (clev_ == null || !clev_.equals(clev)) {
      ContourLevels tempOld = clev_;
      clev_ = clev;
      firePropertyChange("contourLevels", tempOld, clev_);
    }
  }

  /**
   * Get the <code>ContourLevels</code>.
   *
   * @return <code>ContourLevels</code>
   */
  public ContourLevels getContourLevels() {
    return clev_;
  }

  /**
   * Copy the <code>GridAttribute</code>.
   *
   * @return new <code>GridAttribute</code>
   */
  public GridAttribute copy() {
    GridAttribute newGrid;
    try {
      newGrid = (GridAttribute) clone();
    } catch (CloneNotSupportedException e) {
      newGrid = new GridAttribute();
    }
    return newGrid;
  }

  /**
   * Set the grid style. <br>
   * <strong>Property Change:</strong> <code>style</code>.
   *
   * @param st grid style
   */
  public void setStyle(int st) {
    if (style_ != st) {
      Integer tempOld = Integer.valueOf(style_);
      style_ = st;
      firePropertyChange("style", tempOld, Integer.valueOf(style_));
    }
  }

  /**
   * Get grid style.
   *
   * @return grid style
   */
  public int getStyle() {
    return style_;
  }

  /**
   * Tests if <code>GridAttribute</code> style is either RASTER or RASTER_CONTOUR.
   *
   * @since 2.0
   */
  public boolean isRaster() {
    return (style_ == RASTER || style_ == RASTER_CONTOUR);
  }

  /**
   * Tests if <code>GridAttribute</code> style is either CONTOUR, RASTER_CONTOUR, or
   * AREA_FILL_CONTOUR.
   *
   * @since 2.0
   */
  public boolean isContour() {
    return (style_ == CONTOUR || style_ == RASTER_CONTOUR || style_ == AREA_FILL_CONTOUR);
  }

  /**
   * Tests if <code>GridAttribute</code> style is eigther AREA_FILL or AREA_FILL_CONTOUR.
   *
   * @since 2.0
   */
  public boolean isAreaFill() {
    return (style_ == AREA_FILL || style_ == AREA_FILL_CONTOUR);
  }

  /**
   * Get the <code>ColorMap</code>.
   *
   * @return the <code>ColorMap</code>
   */
  public ColorMap getColorMap() {
    return cmap_;
  }

  /**
   * Set the <code>ColorMap</code>. <br>
   * <strong>Property Change:</strong> <code>colorMap</code>.
   *
   * @param cmap the <code>ColorMap</code>
   */
  public void setColorMap(ColorMap cmap) {
    if (cmap_ == null && cmap == null) {
      return;
    } else {
      if (cmap_ != null) cmap_.removePropertyChangeListener(this);
      if (cmap_ == null || !cmap_.equals(cmap)) {
        ColorMap tempOld = cmap_;
        cmap_ = cmap;
        firePropertyChange("colorMap", tempOld, cmap_);
        cmap_.addPropertyChangeListener(this);
      }
    }
  }

  /**
   * Get a <code>String</code> representation of the <code>GridAttribute</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1);
  }

  /** Add listener to changes in <code>GridAttribute</code> properties. */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (changes_ == null) changes_ = new PropertyChangeSupport(this);
    changes_.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changes_.removePropertyChangeListener(listener);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (Debug.EVENT) {
      System.out.println("GridAttribute: " + evt);
      System.out.println("                  " + evt.getPropertyName());
    }
    changes_.firePropertyChange(evt);
  }

  /**
   * @since 3.0
   */
  @Override
  public void setId(String id) {
    id_ = id;
  }

  /**
   * @since 3.0
   */
  @Override
  public String getId() {
    return id_;
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
   * @since 3.0
   */
  @Override
  public void setBatch(boolean batch) {
    setBatch(batch, true);
  }

  /**
   * @since 3.0
   */
  @Override
  public void setBatch(boolean batch, boolean local) {
    local_ = local;
    batch_ = batch;
    if (!batch && modified_) firePropertyChange("batch", Boolean.TRUE, Boolean.FALSE);
  }

  /**
   * @since 3.0
   */
  @Override
  public boolean isBatch() {
    return batch_;
  }
}
