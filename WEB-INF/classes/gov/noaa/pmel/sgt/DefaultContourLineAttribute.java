/*
 * $Id: DefaultContourLineAttribute.java,v 1.8 2001/12/13 19:07:04 dwd Exp $
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

/**
 * Sets the default rendering style for contour line data. <code>Color</code>, width, and dash
 * characteristics are <code>DefaultContourLineAttribute</code> properties. For individual contour
 * lines, the characteristics can be overridden by <code>ContourLineAttribute</code> when used with
 * <code>ContourLevels</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2001/12/13 19:07:04 $
 * @since 2.0
 * @see GridCartesianRenderer
 * @see ContourLevels
 * @see ContourLineAttribute
 */
public class DefaultContourLineAttribute extends LineAttribute {
  /**
   * @label attr
   */
  private ContourLineAttribute attr_;

  private boolean labelEnabled_ = true;
  private Color labelColor_;
  private Font labelFont_;
  private double labelHeightP_;
  private int sigDigits_;
  private String labelFormat_;

  /**
   * Default constructor. Defaults are:
   *
   * <pre>
   *   labelColor = <code>Color.black</code>
   * labelHeightP = 0.16
   *    labelFont = null
   *  labelFormat = ""
   *    sigDigits = 2
   * </pre>
   */
  public DefaultContourLineAttribute() {
    super(SOLID, Color.black);
    labelColor_ = Color.black;
    labelHeightP_ = 0.16;
    labelFont_ = null;
    sigDigits_ = 2;
    labelFormat_ = "";
  }

  /** Set the <code>ContourLineAttribute</code> that will potentially override attributes. */
  public DefaultContourLineAttribute setContourLineAttribute(ContourLineAttribute attr) {
    attr_ = attr;
    return this;
  }

  /** Get the associated <code>ContourLineAttribute</code> */
  public ContourLineAttribute getContourLineAttribute() {
    return attr_;
  }

  /** Set label text for associated <code>ContourLineAttribute</code>. */
  public void setLabelText(String label) {
    if (attr_ != null) attr_.setLabelText(label);
  }

  /**
   * Return label text from associated <code>ContourLineAttribute</code>, if none return empty
   * string.
   */
  public String getLabelText() {
    if (attr_ != null) {
      return attr_.getLabelText();
    } else {
      return "";
    }
  }

  /**
   * Enable/disable the contour label. <br>
   * <strong>Property Change:</strong> <code>labelEnabled</code>.
   */
  public void setLabelEnabled(boolean sle) {
    if (labelEnabled_ != sle) {
      Boolean tempOld = Boolean.valueOf(labelEnabled_);
      labelEnabled_ = sle;
      changes_.firePropertyChange("labelEnabled", tempOld, Boolean.valueOf(labelEnabled_));
    }
  }

  /**
   * Test if the contour label is enabled. Use associated <code>ContourLineAttribute</code> if it
   * exists and has labelEnabledOverrideen set to <code>false</code>.
   */
  public boolean isLabelEnabled() {
    if (attr_ != null && attr_.isLabelEnabledOverridden()) {
      return attr_.isLabelEnabled();
    } else {
      return labelEnabled_;
    }
  }

  /**
   * Set the default contour label color <br>
   * <strong>Property Change:</strong> <code>labelColor</code>.
   */
  public void setLabelColor(Color color) {
    if (!labelColor_.equals(color)) {
      Color tempOld = labelColor_;
      labelColor_ = color;
      changes_.firePropertyChange("labelColor", tempOld, labelColor_);
    }
  }

  /**
   * Get the contour label color. Use associated <code>ContourLineAttribute</code> if it exists and
   * has labelColorOverrideen set to <code>false</code>.
   */
  public Color getLabelColor() {
    if (attr_ != null && attr_.isLabelColorOverridden()) {
      return attr_.getLabelColor();
    } else {
      return labelColor_;
    }
  }

  /**
   * Set the default contour label height. <br>
   * <strong>Property Change:</strong> <code>labelHeightP</code>.
   */
  public void setLabelHeightP(double height) {
    if (labelHeightP_ != height) {
      Double tempOld = Double.valueOf(labelHeightP_);
      labelHeightP_ = height;
      changes_.firePropertyChange("labelHeightP", tempOld, Double.valueOf(labelHeightP_));
    }
  }

  /**
   * Get the contour label height. Use associated <code>ContourLineAttribute</code> if it exists and
   * has labelHeightPOverrideen set to <code>false</code>.
   */
  public double getLabelHeightP() {
    if (attr_ != null && attr_.isLabelHeightPOverridden()) {
      return attr_.getLabelHeightP();
    } else {
      return labelHeightP_;
    }
  }

  /**
   * Set the default contour label font. <br>
   * <strong>Property Change:</strong> <code>labelFont</code>.
   */
  public void setLabelFont(Font font) {
    if (labelFont_ == null || !labelFont_.equals(font)) {
      Font tempOld = labelFont_;
      labelFont_ = font;
      changes_.firePropertyChange("labelFont", tempOld, labelFont_);
    }
  }

  /**
   * Get the contour label font. Use associated <code>ContourLineAttribute</code> if it exists and
   * has labelFontOverrideen set to <code>false</code>.
   */
  public Font getLabelFont() {
    if (attr_ != null && attr_.isLabelFontOverridden()) {
      return attr_.getLabelFont();
    } else {
      return labelFont_;
    }
  }

  /**
   * Set the number of significant digits for auto labelling. <br>
   * <strong>Property Change:</strong> <code>significantDigits</code>.
   */
  public void setSignificantDigits(int sig) {
    if (sigDigits_ != sig) {
      Integer tempOld = Integer.valueOf(sigDigits_);
      sigDigits_ = sig;
      changes_.firePropertyChange("significantDigits", tempOld, Integer.valueOf(sigDigits_));
    }
  }

  /** Get the number of significant digits for auto labelling. */
  public int getSignificantDigits() {
    return sigDigits_;
  }

  /**
   * Set the default contour label format. <br>
   * <strong>Property Change:</strong> <code>labelFormat</code>.
   */
  public void setLabelFormat(String format) {
    if (!labelFormat_.equals(format)) {
      String tempOld = labelFormat_;
      labelFormat_ = format;
      changes_.firePropertyChange("labelFormat", tempOld, labelFormat_);
    }
  }

  /**
   * Get the contour label format. Use associated <code>ContourLineAttribute</code> if it exists and
   * has labelFormatOverrideen set to <code>false</code>.
   */
  public String getLabelFormat() {
    if (attr_ != null && attr_.isLabelFormatOverridden()) {
      return attr_.getLabelFormat();
    } else {
      return labelFormat_;
    }
  }

  /**
   * Test if auto label is enabled. Use associated <code>ContourLineAttribute</code> if it exists
   * otherwise always returns <code>true</code>.
   */
  public boolean isAutoLabel() {
    if (attr_ != null) {
      return attr_.isAutoLabel();
    } else {
      return true;
    }
  }

  /**
   * Get dash array. Use associated <code>ContourLineAttribute</code> if it exists and has
   * dashArrayOverrideen set to <code>false</code>.
   */
  @Override
  public float[] getDashArray() {
    if (attr_ != null && attr_.isDashArrayOverridden()) {
      return attr_.getDashArray();
    } else {
      return super.getDashArray();
    }
  }

  /**
   * Get the dash phase. Use associated <code>ContourLineAttribute</code> if it exists and has
   * dashPhaseOverrideen set to <code>false</code>.
   */
  @Override
  public float getDashPhase() {
    if (attr_ != null && attr_.isDashPhaseOverridden()) {
      return attr_.getDashPhase();
    } else {
      return super.getDashPhase();
    }
  }

  /** Override the default setStyle. Legal styles <em>do not</em> include MARK or MARK_LINE. */
  @Override
  public void setStyle(int st) {
    if (st == MARK || st == MARK_LINE) return;
    super.setStyle(st);
  }

  /**
   * Get the contour line style. Use associated <code>ContourLineAttribute</code> if it exists and
   * has styleOverrideen set to <code>false</code>.
   */
  @Override
  public int getStyle() {
    if (attr_ != null && attr_.isStyleOverridden()) {
      return attr_.getStyle();
    } else {
      return super.getStyle();
    }
  }

  /**
   * Get the contour line color. Use associated <code>ContourLineAttribute</code> if it exists and
   * has colorOverrideen set to <code>false</code>.
   */
  @Override
  public Color getColor() {
    if (attr_ != null && attr_.isColorOverridden()) {
      return attr_.getColor();
    } else {
      return super.getColor();
    }
  }

  /**
   * Get the contour line width. Use associated <code>ContourLineAttribute</code> if it exists and
   * has widthOverrideen set to <code>false</code>.
   */
  @Override
  public float getWidth() {
    if (attr_ != null && attr_.isWidthOverridden()) {
      return attr_.getWidth();
    } else {
      return super.getWidth();
    }
  }

  /**
   * Get the contour line cap style. Use associated <code>ContourLineAttribute</code> if it exists
   * and has capStyleOverrideen set to <code>false</code>.
   */
  @Override
  public int getCapStyle() {
    if (attr_ != null && attr_.isCapStyleOverridden()) {
      return attr_.getCapStyle();
    } else {
      return super.getCapStyle();
    }
  }

  /**
   * Get the contour line miter style. Use associated <code>ContourLineAttribute</code> if it exists
   * and has miterStyleOverrideen set to <code>false</code>.
   */
  @Override
  public int getMiterStyle() {
    if (attr_ != null && attr_.isMiterStyleOverridden()) {
      return attr_.getMiterStyle();
    } else {
      return super.getMiterStyle();
    }
  }

  /**
   * Get the contour line miter limit. Use associated <code>ContourLineAttribute</code> if it exists
   * and has miterLimitOverrideen set to <code>false</code>.
   */
  @Override
  public float getMiterLimit() {
    if (attr_ != null && attr_.isMiterLimitOverridden()) {
      return attr_.getMiterLimit();
    } else {
      return super.getMiterLimit();
    }
  }

  @Override
  public String toString() {
    Color col = getColor();
    int style = getStyle();
    String sstyle;
    if (style == SOLID) {
      sstyle = "SOLID";
    } else if (style == DASHED) {
      sstyle = "DASHED";
    } else if (style == HEAVY) {
      sstyle = "HEAVY";
    } else if (style == HIGHLIGHT) {
      sstyle = "HIGHLIGHT";
    } else if (style == MARK) {
      sstyle = "MARK - unsupported";
    } else if (style == MARK_LINE) {
      sstyle = "MARK_LINE - unsupported";
    } else if (style == STROKE) {
      sstyle = "STROKE";
    } else {
      sstyle = "";
    }
    String scol = "[" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + "]";
    return sstyle + ", " + scol + ", labelEnabled=" + labelEnabled_;
  }

  @Override
  public Object copy() {
    DefaultContourLineAttribute newAttr;
    try {
      newAttr = (DefaultContourLineAttribute) clone();
    } catch (CloneNotSupportedException e) {
      newAttr = new DefaultContourLineAttribute();
    }
    return newAttr;
  }
}
