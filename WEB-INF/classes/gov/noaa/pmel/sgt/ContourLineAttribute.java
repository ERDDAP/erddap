/*
 * $Id: ContourLineAttribute.java,v 1.13 2001/12/13 19:07:04 dwd Exp $
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
 * Sets the rendering style for a contour line. <code>Color</code>, width, and dash characteristics
 * are <code>ContourLineAttribute</code> properties.
 *
 * @author Donald Denbo
 * @version $Revision: 1.13 $, $Date: 2001/12/13 19:07:04 $
 * @since 2.0
 * @see LineCartesianRenderer
 * @see ContourLevels
 */
public class ContourLineAttribute extends LineAttribute {
  //
  private String labelText_;
  private boolean labelEnabled_ = false;
  private Color labelColor_;
  private Font labelFont_;
  private double labelHeightP_;
  private String labelFormat_;
  private boolean autoLabel_ = true;
  //
  private boolean labelColorOverridden_ = false;
  private boolean labelEnabledOverridden_ = false;
  private boolean labelHeightPOverridden_ = false;
  private boolean labelFontOverridden_ = false;
  private boolean labelFormatOverridden_ = false;
  //
  private boolean colorOverridden_ = false;
  private boolean styleOverridden_ = false;
  private boolean widthOverridden_ = false;
  private boolean dashArrayOverridden_ = false;
  private boolean dashPhaseOverridden_ = false;
  private boolean capStyleOverridden_ = false;
  private boolean miterStyleOverridden_ = false;
  private boolean miterLimitOverridden_ = false;

  /** Default constructor. */
  public ContourLineAttribute() {
    super();
    init();
  }

  /**
   * Constructor using default Color. Default are:
   *
   * <pre>
   *   labelColor = <code>Color.black</code>
   * labelHeightP = 0.16
   *    labelFont = null
   *  labelFormat = ""
   * </pre>
   */
  public ContourLineAttribute(int style) {
    super(style);
    init();
  }

  /**
   * <code>ContourLineAttribute</code> constructor.
   *
   * @param style line style
   * @param color line <code>Color</code>
   * @see java.awt.Color
   */
  public ContourLineAttribute(int style, Color color) {
    super(style, color);
    init();
  }

  /**
   * <code>ContourLineAttribute</code> constructor for plot marks.
   *
   * @param style line sytle
   * @param mark plot mark
   * @param color line <code>Color</code>
   */
  public ContourLineAttribute(int style, int mark, Color color) {
    super(style, mark, color);
    init();
  }

  private void init() {
    labelColor_ = Color.black;
    labelHeightP_ = 0.16;
    labelFont_ = null;
    labelFormat_ = "";
  }

  /**
   * Copy the <code>ContourLineAttribute</code>.
   *
   * @return new <code>ContourLineAttribute</code>
   */
  @Override
  public Object copy() {
    ContourLineAttribute newLine;
    try {
      newLine = (ContourLineAttribute) clone();
    } catch (CloneNotSupportedException e) {
      newLine = new ContourLineAttribute();
    }
    return newLine;
  }

  /**
   * Set the text to be used for labelling the contour line. <br>
   * <strong>Property Change:</strong> <code>labelText</code>.
   */
  public void setLabelText(String label) {
    if (labelText_ == null || !labelText_.equals(label)) {
      String tempOld = labelText_;
      labelText_ = label;
      labelEnabled_ = !(labelText_ == null);
      changes_.firePropertyChange("labelText", tempOld, labelText_);
    }
  }

  /** Get the label text. */
  public String getLabelText() {
    return labelText_;
  }

  /**
   * Enable/disable the contour label. <br>
   * <strong>Property Change:</strong> <code>labelEnabled</code>.
   */
  public void setLabelEnabled(boolean sle) {
    if (labelEnabled_ != sle) {
      labelEnabledOverridden_ = true;
      Boolean tempOld = Boolean.valueOf(labelEnabled_);
      labelEnabled_ = sle;
      changes_.firePropertyChange("labelEnabled", tempOld, Boolean.valueOf(labelEnabled_));
    }
  }

  /** Test if the contour label is enabled. */
  public boolean isLabelEnabled() {
    return labelEnabled_;
  }

  /**
   * Change the color of the contour label <br>
   * <strong>Property Change:</strong> <code>labelColor</code>.
   */
  public void setLabelColor(Color color) {
    if (!labelColor_.equals(color)) {
      labelColorOverridden_ = true;
      Color tempOld = labelColor_;
      labelColor_ = color;
      changes_.firePropertyChange("labelColor", tempOld, labelColor_);
    }
  }

  /** Get the color of the contour label */
  public Color getLabelColor() {
    return labelColor_;
  }

  /**
   * Set the label height in physical units <br>
   * <strong>Property Change:</strong> <code>labelHeightP</code>.
   */
  public void setLabelHeightP(double height) {
    if (labelHeightP_ != height) {
      labelHeightPOverridden_ = true;
      Double tempOld = Double.valueOf(labelHeightP_);
      labelHeightP_ = height;
      changes_.firePropertyChange("labelHeightP", tempOld, Double.valueOf(labelHeightP_));
    }
  }

  /** Get the label height in physical units */
  public double getLabelHeightP() {
    return labelHeightP_;
  }

  /**
   * Set the contour label font. <br>
   * <strong>Property Change:</strong> <code>labelFont</code>.
   */
  public void setLabelFont(Font font) {
    if (labelFont_ == null || !labelFont_.equals(font)) {
      labelFontOverridden_ = true;
      Font tempOld = labelFont_;
      labelFont_ = font;
      changes_.firePropertyChange("labelFont", tempOld, labelFont_);
    }
  }

  /** Get the contour label font */
  public Font getLabelFont() {
    return labelFont_;
  }

  /**
   * Set the format for the contour label. The format is used with <code>Format</code>. <br>
   * <strong>Property Change:</strong> <code>labelFormat</code>.
   *
   * @see Format
   */
  public void setLabelFormat(String format) {
    if (!labelFormat_.equals(format)) {
      labelFormatOverridden_ = true;
      String tempOld = labelFormat_;
      labelFormat_ = format;
      changes_.firePropertyChange("labelFormat", tempOld, labelFormat_);
    }
  }

  /** Get the contour label format */
  public String getLabelFormat() {
    return labelFormat_;
  }

  /**
   * Create contour label from value and format. <br>
   * <strong>Property Change:</strong> <code>autoLabel</code>.
   */
  public void setAutoLabel(boolean auto) {
    if (autoLabel_ != auto) {
      Boolean tempOld = Boolean.valueOf(autoLabel_);
      autoLabel_ = auto;
      changes_.firePropertyChange("autoLabel", tempOld, Boolean.valueOf(autoLabel_));
    }
  }

  /** Is auto labelling on? */
  public boolean isAutoLabel() {
    return autoLabel_;
  }

  /** Test if labelEnabled is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isLabelEnabledOverridden() {
    return labelEnabledOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of labelEnabled. <br>
   * <strong>Property Change:</strong> <code>labelEnabledOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setLabelEnabledOverridden(boolean override) {
    if (labelEnabledOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(labelEnabledOverridden_);
      labelEnabledOverridden_ = override;
      changes_.firePropertyChange(
          "labelEnabledOverridden", tempOld, Boolean.valueOf(labelEnabledOverridden_));
    }
  }

  /** Test if labelColor is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isLabelColorOverridden() {
    return labelColorOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of labelColor. <br>
   * <strong>Property Change:</strong> <code>labelColorOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setLabelColorOverridden(boolean override) {
    if (labelColorOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(labelColorOverridden_);
      labelColorOverridden_ = override;
      changes_.firePropertyChange(
          "labelColorOverridden", tempOld, Boolean.valueOf(labelColorOverridden_));
    }
  }

  /** Test if labelHeightP is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isLabelHeightPOverridden() {
    return labelHeightPOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of labelHeightP. <br>
   * <strong>Property Change:</strong> <code>labelHeightPOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setLabelHeightPOverridden(boolean override) {
    if (labelHeightPOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(labelHeightPOverridden_);
      labelHeightPOverridden_ = override;
      changes_.firePropertyChange(
          "labelHeightPOverridden", tempOld, Boolean.valueOf(labelHeightPOverridden_));
    }
  }

  /** Test if labelFont is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isLabelFontOverridden() {
    return labelFontOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of labelFont. <br>
   * <strong>Property Change:</strong> <code>labelFontOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setLabelFontOverridden(boolean override) {
    if (labelFontOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(labelFontOverridden_);
      labelFontOverridden_ = override;
      changes_.firePropertyChange(
          "labelFontOverridden", tempOld, Boolean.valueOf(labelFontOverridden_));
    }
  }

  /** Test if labelFormat is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isLabelFormatOverridden() {
    return labelFormatOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of labelFormat. <br>
   * <strong>Property Change:</strong> <code>labelFormatOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setLabelFormatOverridden(boolean override) {
    if (labelFormatOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(labelFormatOverridden_);
      labelFormatOverridden_ = override;
      changes_.firePropertyChange(
          "labelFormatOverridden", tempOld, Boolean.valueOf(labelFormatOverridden_));
    }
  }

  @Override
  public void setDashArray(float[] dashes) {
    dashArrayOverridden_ = true;
    super.setDashArray(dashes);
  }

  /** Test if dashArray is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isDashArrayOverridden() {
    return dashArrayOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of dashArray. <br>
   * <strong>Property Change:</strong> <code>dashArrayOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setDashArrayOverridden(boolean override) {
    if (dashArrayOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(dashArrayOverridden_);
      dashArrayOverridden_ = override;
      changes_.firePropertyChange(
          "dashArrayOverridden", tempOld, Boolean.valueOf(dashArrayOverridden_));
    }
  }

  @Override
  public void setDashPhase(float phase) {
    dashPhaseOverridden_ = true;
    super.setDashPhase(phase);
  }

  /** Test if dashPhase is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isDashPhaseOverridden() {
    return dashPhaseOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of dashPhase. <br>
   * <strong>Property Change:</strong> <code>dashPhaseOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setDashPhaseOverridden(boolean override) {
    if (dashPhaseOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(dashPhaseOverridden_);
      dashPhaseOverridden_ = override;
      changes_.firePropertyChange(
          "dashPhaseOverridden", tempOld, Boolean.valueOf(dashPhaseOverridden_));
    }
  }

  @Override
  public void setStyle(int st) {
    if (st == MARK || st == MARK_LINE) return;
    styleOverridden_ = true;
    super.setStyle(st);
  }

  /** Test if style is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isStyleOverridden() {
    return styleOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of style. <br>
   * <strong>Property Change:</strong> <code>styleOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setStyleOverridden(boolean override) {
    if (styleOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(styleOverridden_);
      styleOverridden_ = override;
      changes_.firePropertyChange("styleOverridden", tempOld, Boolean.valueOf(styleOverridden_));
    }
  }

  @Override
  public void setColor(Color c) {
    colorOverridden_ = true;
    super.setColor(c);
  }

  /** Test if color is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isColorOverridden() {
    return colorOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of color. <br>
   * <strong>Property Change:</strong> <code>colorOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setColorOverridden(boolean override) {
    if (colorOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(colorOverridden_);
      colorOverridden_ = override;
      changes_.firePropertyChange("colorOverridden", tempOld, Boolean.valueOf(colorOverridden_));
    }
  }

  @Override
  public void setWidth(float t) {
    widthOverridden_ = true;
    super.setWidth(t);
  }

  /** Test if width is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isWidthOverridden() {
    return widthOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of width. <br>
   * <strong>Property Change:</strong> <code>widthOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setWidthOverridden(boolean override) {
    if (widthOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(widthOverridden_);
      widthOverridden_ = override;
      changes_.firePropertyChange("widthOverridden", tempOld, Boolean.valueOf(widthOverridden_));
    }
  }

  @Override
  public void setCapStyle(int style) {
    capStyleOverridden_ = true;
    super.setCapStyle(style);
  }

  /** Test if cap style is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isCapStyleOverridden() {
    return capStyleOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of cap style. <br>
   * <strong>Property Change:</strong> <code>capStyleOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setCapStyleOverridden(boolean override) {
    if (capStyleOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(capStyleOverridden_);
      capStyleOverridden_ = override;
      changes_.firePropertyChange(
          "capStyleOverridden", tempOld, Boolean.valueOf(capStyleOverridden_));
    }
  }

  @Override
  public void setMiterStyle(int style) {
    miterStyleOverridden_ = true;
    super.setMiterStyle(style);
  }

  /** Test if miter style is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isMiterStyleOverridden() {
    return miterStyleOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of miter style. <br>
   * <strong>Property Change:</strong> <code>miterStyleOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setMiterStyleOverridden(boolean override) {
    if (miterStyleOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(miterStyleOverridden_);
      miterStyleOverridden_ = override;
      changes_.firePropertyChange(
          "miterStyleOverridden", tempOld, Boolean.valueOf(miterStyleOverridden_));
    }
  }

  @Override
  public void setMiterLimit(float t) {
    miterLimitOverridden_ = true;
    super.setMiterLimit(t);
  }

  /** Test if miter limit is overridden by <code>DefaultContourLineAttribute</code>. */
  public boolean isMiterLimitOverridden() {
    return miterLimitOverridden_;
  }

  /**
   * Enable/disable having <code>DefaultContourLineAttribute</code> override <code>
   * ContourLineAttribute</code> behavior of miter limit. <br>
   * <strong>Property Change:</strong> <code>miterLimitOverridden</code>.
   *
   * @see DefaultContourLineAttribute
   */
  public void setMiterLimitOverridden(boolean override) {
    if (miterLimitOverridden_ != override) {
      Boolean tempOld = Boolean.valueOf(miterLimitOverridden_);
      miterLimitOverridden_ = override;
      changes_.firePropertyChange(
          "miterLimitOverridden", tempOld, Boolean.valueOf(miterLimitOverridden_));
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
}
