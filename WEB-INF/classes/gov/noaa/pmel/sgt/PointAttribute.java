/*
 * $Id: PointAttribute.java,v 1.11 2003/08/22 23:02:32 dwd Exp $
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
 * Set the rendereing style for point data. <code>Color</code>, width, and mark type are <code>
 * PointAttribute</code> properties.
 *
 * @author Donald Denbo
 * @version $Revision: 1.11 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 * @see CartesianGraph
 * @see PointCartesianRenderer
 * @see PlotMark
 */
public class PointAttribute implements Attribute, Cloneable {
  private transient PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  // serial version ref 1.10.2.2
  private static final long serialVersionUID = -1147362012307247472L;
  private boolean batch_ = false;
  private boolean local_ = true;
  private boolean modified_ = false;
  private String id_ = "";
  private Color color_;
  private int mark_;
  private double markHeightP_;
  private double pwidth_;
  //
  private boolean drawLabel_ = false;
  private int labelPosition_;
  private Font labelFont_ = null;
  private Color labelColor_ = null;
  private double labelHeightP_;

  /** Position label over the point */
  public static final int CENTERED = 0;

  /** Position label north of the point */
  public static final int N = 1;

  /** Position label northeast of the point */
  public static final int NE = 2;

  /** Position label east of the point */
  public static final int E = 3;

  /** Position label southeast of the point */
  public static final int SE = 4;

  /** Position label south of the point */
  public static final int S = 5;

  /** Position label southwest of the point */
  public static final int SW = 6;

  /** Position label west of the point */
  public static final int W = 7;

  /** Position label northwest of the point */
  public static final int NW = 8;

  /** Default constructor. Default mark is 2 and default color is red. */
  public PointAttribute() {
    this(2, Color.red);
  }

  /**
   * Constructor for plot marks.
   *
   * @param mark plot mark
   * @param color Point color
   * @see PlotMark
   */
  public PointAttribute(int mark, Color color) {
    mark_ = mark;
    color_ = color;
    markHeightP_ = 0.1;
    labelHeightP_ = 0.1;
    labelColor_ = Color.black;
    drawLabel_ = false;
  }

  /**
   * Copy the <code>PointAttribute</code>.
   *
   * @return new <code>PointAttribute</code>
   */
  public PointAttribute copy() {
    PointAttribute newPoint;
    try {
      newPoint = (PointAttribute) clone();
    } catch (CloneNotSupportedException e) {
      newPoint = new PointAttribute();
    }
    return newPoint;
  }

  /**
   * @since 3.0
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof PointAttribute)) return false;
    PointAttribute attr = (PointAttribute) obj;
    if (!id_.equals(attr.getId())
        || !color_.equals(attr.getColor())
        || (mark_ != attr.getMark())
        || (markHeightP_ != attr.getMarkHeightP())
        || (pwidth_ != attr.getWidthP())
        || (drawLabel_ != attr.isDrawLabel())) return false;
    if (drawLabel_) {
      if (labelFont_ == null) {
        if (attr.getLabelFont() != null) return false;
      } else {
        if (attr.getLabelFont() == null) return false;
        if (!labelFont_.equals(attr.getLabelFont())) return false;
      }
      if (labelColor_ == null) {
        if (attr.getLabelColor() != null) return false;
      } else {
        if (attr.getLabelColor() == null) return false;
        if (!labelColor_.equals(attr.getLabelColor())) return false;
      }
      if ((labelPosition_ != attr.getLabelPosition()) || (labelHeightP_ != attr.getLabelHeightP()))
        return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id_, color_, mark_, markHeightP_, pwidth_, drawLabel_);
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
   * Set the point <code>Color</code>. <br>
   * <strong>Property Change:</strong> <code>color</code>.
   *
   * @param c point <code>Color</code>
   */
  public void setColor(Color c) {
    if (!color_.equals(c)) {
      Color tempOld = color_;
      color_ = c;
      firePropertyChange("color", tempOld, color_);
    }
  }

  /**
   * Set the Point width in physical units. <br>
   * <strong>Property Change:</strong> <code>widthP</code>.
   *
   * @param t Point width
   */
  public void setWidthP(double t) {
    if (pwidth_ != t) {
      Double tempOld = Double.valueOf(pwidth_);
      pwidth_ = t;
      firePropertyChange("widthP", tempOld, Double.valueOf(pwidth_));
    }
  }

  /**
   * Get Point <code>Color</code>.
   *
   * @return Point <code>Color</code>
   */
  public Color getColor() {
    return color_;
  }

  /**
   * Get Point width.
   *
   * @return Point width in physcial coordinates.
   */
  public double getWidthP() {
    return pwidth_;
  }

  /**
   * Set plot mark <br>
   * <strong>Property Change:</strong> <code>mark</code>.
   *
   * @param mark the plot mark
   * @see PlotMark
   */
  public void setMark(int mark) {
    if (mark_ != mark) {
      Integer tempOld = Integer.valueOf(mark_);
      mark_ = mark;
      firePropertyChange("mark", tempOld, Integer.valueOf(mark_));
    }
  }

  /**
   * Get plot mark
   *
   * @return plot mark
   * @see PlotMark
   */
  public int getMark() {
    return mark_;
  }

  /**
   * Set label position. <br>
   * <strong>Property Change:</strong> <code>labelPosition</code>.
   */
  public void setLabelPosition(int pos) {
    if (labelPosition_ != pos) {
      Integer tempOld = Integer.valueOf(labelPosition_);
      labelPosition_ = pos;
      firePropertyChange("labelPosition", tempOld, Integer.valueOf(labelPosition_));
    }
  }

  /** Get label position. */
  public int getLabelPosition() {
    return labelPosition_;
  }

  /**
   * Set label <code>Color</code>. <br>
   * <strong>Property Change:</strong> <code>labelColor</code>.
   */
  public void setLabelColor(Color col) {
    if (labelColor_ == null || !labelColor_.equals(col)) {
      Color tempOld = labelColor_;
      labelColor_ = col;
      firePropertyChange("labelColor", tempOld, labelColor_);
    }
  }

  /** Get label <code>Color</code>. */
  public Color getLabelColor() {
    return labelColor_;
  }

  /**
   * Set label <code>Font</code>. <br>
   * <strong>Property Change:</strong> <code>labelFont</code>.
   */
  public void setLabelFont(Font font) {
    if (labelFont_ == null || !labelFont_.equals(font)) {
      Font tempOld = labelFont_;
      labelFont_ = font;
      firePropertyChange("labelFont", tempOld, labelFont_);
    }
  }

  /** Get label <code>Font</code>. */
  public Font getLabelFont() {
    return labelFont_;
  }

  /**
   * Set label height. <br>
   * <strong>Property Change:</strong> <code>labelHeightP</code>.
   */
  public void setLabelHeightP(double h) {
    if (labelHeightP_ != h) {
      Double tempOld = Double.valueOf(labelHeightP_);
      labelHeightP_ = h;
      firePropertyChange("labelHeightP", tempOld, Double.valueOf(labelHeightP_));
    }
  }

  /** Get label height. */
  public double getLabelHeightP() {
    return labelHeightP_;
  }

  /**
   * Set label drawing. <br>
   * <strong>Property Change:</strong> <code>drawLabel</code>.
   */
  public void setDrawLabel(boolean dl) {
    if (drawLabel_ != dl) {
      Boolean tempOld = Boolean.valueOf(drawLabel_);
      drawLabel_ = dl;
      firePropertyChange("drawLabel", tempOld, Boolean.valueOf(drawLabel_));
    }
  }

  /** Is label drawing on? */
  public boolean isDrawLabel() {
    return drawLabel_;
  }

  /**
   * Get a <code>String</code> representation of the <code>PointAttribute</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1);
  }

  /** Add listener to changes in <code>PointAttribute</code> properties. */
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
    batch_ = batch;
    local_ = local;
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
