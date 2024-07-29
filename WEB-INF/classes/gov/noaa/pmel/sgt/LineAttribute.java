/*
 * $Id: LineAttribute.java,v 1.20 2003/09/17 20:32:10 dwd Exp $
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

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * Sets the rendering style for line data. <code>Color</code>, width, and dash characteristics are
 * <code>LineAttribute</code> properties.
 *
 * @author Donald Denbo
 * @version $Revision: 1.20 $, $Date: 2003/09/17 20:32:10 $
 * @since 1.0
 * @see LineCartesianRenderer
 * @see ContourLevels
 */
public class LineAttribute implements Attribute, Cloneable {
  protected transient PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  private boolean batch_ = false;
  private boolean local_ = true;
  private boolean modified_ = false;
  private String id_ = null;
  private Color color_ = Color.black;
  private int style_;
  private int mark_ = 1;
  private double markHeightP_ = 0.2;
  private float width_ = 2.0f;
  private float dashes_[] = {12.0f, 12.0f};
  private float dashPhase_ = 0.0f;
  private int cap_style_ = CAP_SQUARE;
  private int miter_style_ = JOIN_MITER;
  private float miter_limit_ = 10.0f;

  private static float HEAVY_WIDTH = 2.0f;

  /** Solid line style. */
  public static final int SOLID = 0;

  /** Dashed line style. */
  public static final int DASHED = 1;

  /**
   * Heavy line style
   *
   * @since 2.0
   */
  public static final int HEAVY = 2;

  /**
   * Highlighted line style. Accomplished by drawing the line over a contrasting polygon of the same
   * shape.
   */
  public static final int HIGHLIGHT = 3;

  /**
   * Mark line style.
   *
   * @see PlotMark
   */
  public static final int MARK = 4;

  /** Mark with connecting lines style. */
  public static final int MARK_LINE = 5;

  /** Stroke. */
  public static final int STROKE = 6;

  /** Cap styles */
  public static final int CAP_BUTT = 0;

  public static final int CAP_ROUND = 1;
  public static final int CAP_SQUARE = 2;

  /** Join styles */
  public static final int JOIN_MITER = 0;

  public static final int JOIN_ROUND = 1;
  public static final int JOIN_BEVEL = 2;

  /** Default constructor. Default style is SOLID and default color is red. */
  public LineAttribute() {
    this(SOLID, Color.red);
  }

  /** Construct <code>LineAttribute</code> with <code>Color.black</code>. */
  public LineAttribute(int style) {
    style_ = style;
    if (style_ == HEAVY) width_ = HEAVY_WIDTH;
  }

  /**
   * <code>LineAttribute</code> constructor.
   *
   * @param style line style
   * @param color line <code>Color</code>
   * @see java.awt.Color
   */
  public LineAttribute(int style, Color color) {
    this(style, 1, color);
  }

  /**
   * <code>LineAttribute</code> constructor for plot marks.
   *
   * @param style line sytle
   * @param mark plot mark
   * @param color line <code>Color</code>
   */
  public LineAttribute(int style, int mark, Color color) {
    style_ = style;
    mark_ = mark;
    color_ = color;
  }

  /**
   * Copy the <code>LineAttribute</code>.
   *
   * @return new <code>LineAttribute</code>
   */
  public Object copy() {
    LineAttribute newLine;
    try {
      newLine = (LineAttribute) clone();
    } catch (CloneNotSupportedException e) {
      newLine = new LineAttribute();
    }
    return newLine;
  }

  /**
   * @since 3.0
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof LineAttribute)) return false;
    LineAttribute attr = (LineAttribute) obj;
    if ((id_ != attr.getId()) || !color_.equals(attr.getColor()) || (style_ != attr.getStyle()))
      return false;
    if (style_ == MARK || style_ == MARK_LINE) {
      if ((mark_ != attr.getMark()) || (markHeightP_ != attr.getMarkHeightP())) return false;
    }
    if (style_ == HEAVY) {
      if (width_ != attr.getWidth()) return false;
    }
    if (style_ == STROKE) {
      if (width_ != attr.getWidth()) return false;
      if (dashes_.length != attr.getDashArray().length) {
        return false;
      } else {
        float[] dar = attr.getDashArray();
        for (int i = 0; i < dashes_.length; i++) {
          if (dashes_[i] != dar[i]) return false;
        }
      }
      if ((dashPhase_ != attr.getDashPhase())
          || (cap_style_ != attr.getCapStyle())
          || (miter_style_ != attr.getMiterStyle())
          || (miter_limit_ != attr.getMiterLimit())) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id_, color_, style_);
  }

  /**
   * Set mark height. <br>
   * <strong>Property Change:</strong> <code>markHeightP</code>.
   *
   * @param markh mark height
   */
  public void setMarkHeightP(double markh) {
    if (markHeightP_ != markh) {
      Double tempOld = Double.valueOf(markHeightP_);
      markHeightP_ = markh;
      firePropertyChange("markHeightP", tempOld, Double.valueOf(markHeightP_));
    }
  }

  /**
   * Get mark height
   *
   * @return mark height
   */
  public double getMarkHeightP() {
    return markHeightP_;
  }

  /**
   * Set the line style. <br>
   * <strong>Property Change:</strong> <code>style</code>.
   *
   * @param st line style
   */
  public void setStyle(int st) {
    if (style_ != st) {
      Integer tempOld = Integer.valueOf(style_);
      style_ = st;
      firePropertyChange("style", tempOld, Integer.valueOf(style_));
    }
  }

  /**
   * Set the line <code>Color</code>. <br>
   * <strong>Property Change:</strong> <code>color</code>.
   *
   * @param c line <code>Color</code>
   */
  public void setColor(Color c) {
    if (!color_.equals(c)) {
      Color tempOld = color_;
      color_ = c;
      firePropertyChange("color", tempOld, color_);
    }
  }

  /**
   * Set the line width in physical units. <br>
   * <strong>Property Change:</strong> <code>width</code>.
   *
   * @param t line width
   */
  public void setWidth(float t) {
    if (width_ != t) {
      Float tempOld = Float.valueOf(width_);
      width_ = t;
      firePropertyChange("width", tempOld, Float.valueOf(width_));
    }
  }

  /**
   * Set the dash characteristics. Lengths are in physical units. <br>
   * <strong>Property Change:</strong> <code>dashArray</code>.
   */
  public void setDashArray(float[] dashes) {
    if (dashes == null) return;
    boolean changed = false;
    if (dashes_.length != dashes.length) {
      changed = true;
    } else {
      for (int i = 0; i < dashes_.length; i++) {
        if (dashes_[i] != dashes[i]) {
          changed = true;
          break;
        }
      }
    }
    if (changed) {
      float[] tempOld = dashes_;
      dashes_ = dashes;
      firePropertyChange("dashArray", tempOld, dashes_);
    }
  }

  /**
   * Get line dash array.
   *
   * @since 2.0
   */
  public float[] getDashArray() {
    return dashes_;
  }

  /**
   * Set line dash phase. <br>
   * <strong>Property Change:</strong> <code>dashPhase</code>.
   *
   * @since 2.0
   */
  public void setDashPhase(float phase) {
    if (dashPhase_ != phase) {
      Float tempOld = Float.valueOf(dashPhase_);
      dashPhase_ = phase;
      firePropertyChange("dashPhase", tempOld, Float.valueOf(dashPhase_));
    }
  }

  /**
   * Get line dash phase.
   *
   * @since 2.0
   */
  public float getDashPhase() {
    return dashPhase_;
  }

  /**
   * Get line style.
   *
   * @return line style
   */
  public int getStyle() {
    return style_;
  }

  /**
   * Get line <code>Color</code>.
   *
   * @return line <code>Color</code>
   */
  public Color getColor() {
    return color_;
  }

  /**
   * Get line width.
   *
   * @return line width in physcial coordinates.
   */
  public float getWidth() {
    return width_;
  }

  /**
   * Set plot mark <br>
   * <strong>Property Change:</strong> <code>mark</code>.
   *
   * @param mark the plot mark
   */
  public void setMark(int mark) {
    if (mark_ != mark) {
      Integer tempOld = Integer.valueOf(mark_);
      if (mark <= 0) mark = 1;
      if (mark > 51) mark = 51;
      mark_ = mark;
      firePropertyChange("mark", tempOld, Integer.valueOf(mark_));
    }
  }

  /**
   * Get plot mark
   *
   * @return plot mark
   */
  public int getMark() {
    return mark_;
  }

  /**
   * Set the current line cap style. Cap styles include <code>CAP_BUTT</code>, <code>CAP_ROUND
   * </code>, and <code>CAP_SQUARE</code>. <br>
   * <strong>Property Change:</strong> <code>capStyle</code>.
   */
  public void setCapStyle(int style) {
    if (cap_style_ != style) {
      Integer tempOld = Integer.valueOf(cap_style_);
      cap_style_ = style;
      firePropertyChange("capStyle", tempOld, Integer.valueOf(cap_style_));
    }
  }

  /** Get the current line cap style. */
  public int getCapStyle() {
    return cap_style_;
  }

  /**
   * Set the current miter style. Styles include <code>JOIN_MITER</code>, <code>JOIN_ROUND</code>,
   * and <code>JOIN_BEVEL</code>. <br>
   * <strong>Property Change:</strong> <code>miterStyle</code>.
   */
  public void setMiterStyle(int style) {
    if (miter_style_ != style) {
      Integer tempOld = Integer.valueOf(miter_style_);
      miter_style_ = style;
      firePropertyChange("miterStyle", tempOld, Integer.valueOf(miter_style_));
    }
  }

  /** Get the current miter sytle. */
  public int getMiterStyle() {
    return miter_style_;
  }

  /**
   * Set the miter limit. <br>
   * <strong>Property Change:</strong> <code>miterLimit</code>.
   */
  public void setMiterLimit(float limit) {
    if (miter_limit_ != limit) {
      Float tempOld = Float.valueOf(miter_limit_);
      miter_limit_ = limit;
      firePropertyChange("miterLimit", tempOld, Float.valueOf(miter_limit_));
    }
  }

  /** Get the current miter limit. */
  public float getMiterLimit() {
    return miter_limit_;
  }

  /**
   * Get a <code>String</code> representation of the <code>LineAttribute</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1);
  }

  /** Add listener to changes in <code>LineAttribute</code> properties. */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (changes_ == null) changes_ = new PropertyChangeSupport(this);
    changes_.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changes_.removePropertyChangeListener(listener);
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
