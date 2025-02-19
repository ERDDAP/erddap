/*
 * Bob Simons made this variant of the sgt VectorAttribute to use
 * a CompoundColorMap instead of a Color.
 *
 * $Id: VectorAttribute.java,v 1.12 2003/09/17 20:32:10 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pfel.coastwatch.sgt;

import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.AttributeChangeEvent;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.LineAttribute;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Sets the rendering style for line data. <code>Color</code>, width, and dash characteristics are
 * <code>VectorAttribute</code> properties. <br>
 * WARNING: The SGT implementation of Vectors requires Java2D. To use Vectors you must be using
 * jdk1.2 or newer.
 *
 * @author Donald Denbo
 * @version $Revision: 1.12 $, $Date: 2003/09/17 20:32:10 $
 * @since 2.1
 * @see LineCartesianRenderer
 * @see ContourLevels
 */
public class VectorAttribute2 implements Attribute, Cloneable, java.io.Serializable {

  protected transient PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  private boolean batch_ = false;
  private boolean local_ = true;
  private boolean modified_ = false;
  private String id_ = null;
  private int vectorStyle_ = HEAD;
  private ColorMap vectorColorMap_ = null; // if vectorColorMap is null, vectorColor is used
  private Color vectorColor_ = null;
  private double vectorScale_ = 0.01; // User units to physical
  private double vectorMaxSize_ = 100.0;
  private double offsetAngle_ = 0.0;

  private double headScale_ = 1.0; // User units to physical
  private double headMaxSize_ = 100.0;
  private double headMinSize_ = 0.05;
  private double headFixedSize_ = 0.2; // Physical units

  private int originStyle_ = NO_MARK;
  private Color markColor_ = Color.black;
  private int mark_ = 1;
  private double markHeightP_ = 0.2;

  private float width_ = 1.0f;
  private int capStyle_ = LineAttribute.CAP_SQUARE;
  private int miterStyle_ = LineAttribute.JOIN_MITER;
  private float miterLimit_ = 10.0f;

  /** Vector head style, None. No arrow head will be drawn. */
  public static final int NO_HEAD = 0;

  /** Vector head style, Un-scaled (default). Head will be drawn a constant size. */
  public static final int HEAD = 1;

  /**
   * Vector head style, Scaled. The size of the head will be proportional to the length of the
   * vector.
   */
  public static final int SCALED_HEAD = 2;

  /**
   * Vector origin style, no mark (default). The origin of the vector will be drawn without a plot
   * mark.
   */
  public static final int NO_MARK = 0;

  /** Vector origin style, Mark. A plot mark will be drawn at the origin of the vector. */
  public static final int MARK = 1;

  /**
   * sourceID identifies the source of the data (e.g., -1 for pointVectorScreen, -2 for
   * pointScreen).
   */
  public final int sourceID;

  /**
   * <code>VectorAttribute</code> constructor. Default vector style is HEAD.
   *
   * @param scale vector scale
   * @param colorMap
   * @param tSourceID identifies the source of the data (e.g., -1 for pointVectorScreen, -2 for
   *     pointScreen)
   */
  public VectorAttribute2(double scale, ColorMap colorMap, Color color, int tSourceID) {
    vectorStyle_ = HEAD;
    vectorScale_ = scale;
    vectorColorMap_ = colorMap;
    vectorColor_ = color;
    sourceID = tSourceID;
  }

  /**
   * <code>VectorAttribute</code> constructor.
   *
   * @param style vector style
   * @param scale vector scale
   * @param colorMap
   * @param head_scale scale of vector head
   * @param tSourceID identifies the source of the data (e.g., -1 for pointVectorScreen, -2 for
   *     pointScreen)
   */
  public VectorAttribute2(
      int style, double scale, ColorMap colorMap, Color color, double head_scale, int tSourceID) {
    vectorStyle_ = style;
    vectorScale_ = scale;
    vectorColorMap_ = colorMap;
    vectorColor_ = color;
    headScale_ = head_scale;
    sourceID = tSourceID;
  }

  /**
   * Copy the <code>VectorAttribute2</code>.
   *
   * @return new <code>VectorAttribute2</code>
   */
  public Object copy() {
    VectorAttribute2 newVector;
    try {
      newVector = (VectorAttribute2) clone();
    } catch (CloneNotSupportedException e) {
      newVector = new VectorAttribute2(vectorScale_, vectorColorMap_, vectorColor_, sourceID);
    }
    return newVector;
  }

  /**
   * Change the head style. Options include <code>NO_HEAD</code>, <code>HEAD</code>, and <code>
   * SCALED_HEAD</code>. <br>
   * <strong>Property Change:</strong> <code>vectorStyle</code>.
   *
   * @see #setVectorColor(java.awt.Color)
   * @see #setVectorMaxSize(double)
   * @see #setVectorScale(double)
   * @see #setOffsetAngle(double)
   */
  public void setVectorStyle(int style) {
    if (vectorStyle_ != style) {
      Integer tempOld = vectorStyle_;
      vectorStyle_ = style;
      firePropertyChange("vectorStyle", tempOld, vectorStyle_);
    }
  }

  /** Get the vector head style. */
  public int getVectorStyle() {
    return vectorStyle_;
  }

  /**
   * Change the vector colorMap. <br>
   * <strong>Property Change:</strong> <code>vectorColorMap</code>.
   */
  public void setVectorColorMap(ColorMap colorMap) {
    if (!vectorColorMap_.equals(colorMap)) {
      ColorMap tempOld = vectorColorMap_;
      vectorColorMap_ = colorMap;
      firePropertyChange("vectorColor", tempOld, vectorColorMap_);
    }
  }

  /** Get the vector color map. */
  public ColorMap getVectorColorMap() {
    return vectorColorMap_;
  }

  /**
   * Change the vector color. <br>
   * <strong>Property Change:</strong> <code>vectorColor</code>.
   */
  public void setVectorColor(Color color) {
    if (!vectorColor_.equals(color)) {
      Color tempOld = vectorColor_;
      vectorColor_ = color;
      firePropertyChange("vectorColor", tempOld, vectorColor_);
    }
  }

  /** Get the vector color. */
  public Color getVectorColor() {
    return vectorColor_;
  }

  /**
   * Change the vector scale. The vector length is determined by the data value times the vector
   * scale. The vector length is bounded by the maximum allowed vector length. <br>
   * <strong>Property Change:</strong> <code>vectorScale</code>.
   *
   * @see #setVectorMaxSize(double)
   */
  public void setVectorScale(double scale) {
    if (vectorScale_ != scale) {
      Double tempOld = vectorScale_;
      vectorScale_ = scale;
      firePropertyChange("vectorScale", tempOld, vectorScale_);
    }
  }

  /** Geth the vector head scale. */
  public double getVectorScale() {
    return vectorScale_;
  }

  /**
   * Set the maximum size for a vector. <br>
   * <strong>Property Change:</strong> <code>vectorMaxSize</code>.
   */
  public void setVectorMaxSize(double size) {
    if (vectorMaxSize_ != size) {
      Double tempOld = vectorMaxSize_;
      vectorMaxSize_ = size;
      firePropertyChange("vectorMaxSize", tempOld, vectorMaxSize_);
    }
  }

  /** Get the maximum vector length allowed. */
  public double getVectorMaxSize() {
    return vectorMaxSize_;
  }

  /**
   * Set the angle (clockwize positive) to rotate the vector. <br>
   * <strong>Property Change:</strong> <code>offsetAngle</code>.
   *
   * @param angle in degrees
   */
  public void setOffsetAngle(double angle) {
    if (offsetAngle_ != angle) {
      Double tempOld = offsetAngle_;
      offsetAngle_ = angle;
      firePropertyChange("offsetAngle", tempOld, offsetAngle_);
    }
  }

  /** Get the vector rotation angle. */
  public double getOffsetAngle() {
    return offsetAngle_;
  }

  /**
   * Change the vector head scale. The vector head size is determined by the length of the vector
   * times the vector head scale. The vector head size is bounded by the minimum and maximum allowed
   * head size. <br>
   * <strong>Property Change:</strong> <code>headScale</code>.
   *
   * @see #setHeadMinSize(double)
   * @see #setHeadMaxSize(double)
   */
  public void setHeadScale(double scale) {
    if (headScale_ != scale) {
      Double tempOld = headScale_;
      headScale_ = scale;
      firePropertyChange("headScale", tempOld, headScale_);
    }
  }

  /** Get the vector head scale. */
  public double getHeadScale() {
    return headScale_;
  }

  /**
   * Set the maximum size for a scaled vector head. <br>
   * <strong>Property Change:</strong> <code>headMaxSize</code>.
   */
  public void setHeadMaxSize(double size) {
    if (headMaxSize_ != size) {
      Double tempOld = headMaxSize_;
      headMaxSize_ = size;
      firePropertyChange("headMaxSize", tempOld, headMaxSize_);
    }
  }

  /** Get the maximum vector head size. */
  public double getHeadMaxSize() {
    return headMaxSize_;
  }

  /**
   * Set the minimum size for a scaled vector head. <br>
   * <strong>Property Change:</strong> <code>headMinSize</code>.
   */
  public void setHeadMinSize(double size) {
    if (headMinSize_ != size) {
      Double tempOld = headMinSize_;
      headMinSize_ = size;
      firePropertyChange("headMinSize", tempOld, headMinSize_);
    }
  }

  /** Get the minimum vector head size. */
  public double getHeadMinSize() {
    return headMinSize_;
  }

  /**
   * Set the fixed size for a unscaled vector head. <br>
   * <strong>Property Change:</strong> <code>headFixedSize</code>.
   */
  public void setHeadFixedSize(double size) {
    if (headFixedSize_ != size) {
      Double tempOld = headFixedSize_;
      headFixedSize_ = size;
      firePropertyChange("headFixedSize", tempOld, headFixedSize_);
    }
  }

  /** Get the fixed vector head size. */
  public double getHeadFixedSize() {
    return headFixedSize_;
  }

  /**
   * Set the vector origin style. Options are <code>NO_MARK</code> and <code>MARK</code>. <br>
   * <strong>Property Change:</strong> <code>originStyle</code>.
   *
   * @see #setMarkColor(java.awt.Color)
   * @see #setMark(int)
   * @see #setMarkHeightP(double)
   */
  public void setOriginStyle(int style) {
    if (originStyle_ != style) {
      Integer tempOld = originStyle_;
      originStyle_ = style;
      firePropertyChange("originStyle", tempOld, originStyle_);
    }
  }

  /** Get vector origin style. */
  public int getOriginStyle() {
    return originStyle_;
  }

  /**
   * Set the color for the origin mark. <br>
   * <strong>Property Change:</strong> <code>markColor</code>.
   */
  public void setMarkColor(Color color) {
    if (!markColor_.equals(color)) {
      Color tempOld = markColor_;
      markColor_ = color;
      firePropertyChange("markColor", tempOld, markColor_);
    }
  }

  /** Get the color for the origin mark. */
  public Color getMarkColor() {
    return markColor_;
  }

  /**
   * Set the mark for the origin. <br>
   * <strong>Property Change:</strong> <code>mark</code>.
   *
   * @param mark the plot mark
   * @see PlotMark
   */
  public void setMark(int mark) {
    if (mark_ != mark) {
      Integer tempOld = mark_;
      if (mark <= 0) mark = 1;
      if (mark > 51) mark = 51;
      mark_ = mark;
      firePropertyChange("mark", tempOld, mark_);
    }
  }

  /**
   * Get plot mark for the origin.
   *
   * @return plot mark
   */
  public int getMark() {
    return mark_;
  }

  /**
   * Set mark height for the origin. <br>
   * <strong>Property Change:</strong> <code>markHeightP</code>.
   *
   * @param markh mark height
   */
  public void setMarkHeightP(double markh) {
    if (markHeightP_ != markh) {
      Double tempOld = markHeightP_;
      markHeightP_ = markh;
      firePropertyChange("markHeightP", tempOld, markHeightP_);
    }
  }

  /**
   * Get mark height for the origin.
   *
   * @return mark height
   */
  public double getMarkHeightP() {
    return markHeightP_;
  }

  /**
   * Set the line width in physical units. <br>
   * <strong>Property Change:</strong> <code>width</code>.
   *
   * @param t line width
   */
  public void setWidth(float t) {
    if (width_ != t) {
      Float tempOld = width_;
      width_ = t;
      firePropertyChange("width", tempOld, width_);
    }
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
   * Set the line Cap Style. Styles include <code>LineAttribute.CAP_BUTT</code>, <code>
   * LineAttribute.CAP_ROUND</code>, and <code>LineAttribute.CAP_SQUARE</code>. <br>
   * <strong>Property Change:</strong> <code>capStyle</code>.
   *
   * @see LineAttribute#CAP_BUTT
   * @see LineAttribute#CAP_ROUND
   * @see LineAttribute#CAP_SQUARE
   */
  public void setCapStyle(int style) {
    if (capStyle_ != style) {
      Integer tempOld = capStyle_;
      capStyle_ = style;
      firePropertyChange("capStyle", tempOld, capStyle_);
    }
  }

  /** Get the line cap style. */
  public int getCapStyle() {
    return capStyle_;
  }

  /**
   * Set the line miter style. Styles include <code>LineAttribute.JOIN_BEVEL</code>, <code>
   * LineAttribute.JOIN_MITER</code>, and <code>LineAttribute.JOIN_ROUND</code>. <br>
   * <strong>Property Change:</strong> <code>miterStyle</code>.
   *
   * @see LineAttribute#JOIN_BEVEL
   * @see LineAttribute#JOIN_MITER
   * @see LineAttribute#JOIN_ROUND
   */
  public void setMiterStyle(int style) {
    if (miterStyle_ != style) {
      Integer tempOld = miterStyle_;
      miterStyle_ = style;
      firePropertyChange("miterStyle", tempOld, miterStyle_);
    }
  }

  /** Get the line miter sytle. */
  public int getMiterStyle() {
    return miterStyle_;
  }

  /**
   * Set the line miter limit. <br>
   * <strong>Property Change:</strong> <code>miterLimit</code>.
   */
  public void setMiterLimit(float limit) {
    if (miterLimit_ != limit) {
      Float tempOld = miterLimit_;
      miterLimit_ = limit;
      firePropertyChange("miterLimit", tempOld, miterLimit_);
    }
  }

  /** Get the line miter limit. */
  public float getMiterLimit() {
    return miterLimit_;
  }

  /**
   * Get a <code>String</code> representation of the <code>VectorAttribute</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1);
  }

  /** Add listener to changes in <code>VectorAttribute</code> properties. */
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
