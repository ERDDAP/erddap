/*
 * $Id: Legend.java,v 1.3 2003/09/18 21:01:14 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.beans;

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.Color;
import java.awt.Font;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.event.*;

/**
 * Encapsulates reference to <code>SGTData</code> and key.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/09/18 21:01:14 $
 * @since 3.0
 */
public class Legend implements Serializable {
  private transient ChangeEvent changeEvent_ = new ChangeEvent(this);
  private transient PanelHolder pHolder_ = null;
  // properties for all keys.
  private String id = "";
  private Rectangle2D.Double boundsP = new Rectangle2D.Double(0.0, 0.0, 0.0, 0.0);
  private boolean visible = true;
  private boolean instantiated = false;
  // VectorKey, LineKey, PointCollectionKey specific properties
  private double lineLength = 0.3;
  private int columns = 1;
  // ColorKey specific properties
  private Color scaleColor = Color.black; // advanced
  private double scaleLabelHeightP = 0.2;
  private int scaleNumberSmallTics = 0; // advanced
  private int scaleLabelInterval = 2;
  private Font scaleLabelFont = new Font("Helvetica", Font.PLAIN, 10); // advanced
  private double scaleLargeTicHeightP = 0.1; // advanced
  private double scaleSmallTicHeightP = 0.05; // advanced
  private boolean scaleVisible = true; // advanced
  private int scaleSignificantDigits = 2;
  private String scaleLabelFormat = ""; // advanced
  private double keyLabelHeightP = 0.16; // advanced
  //
  private int borderStyle = NO_BORDER;

  /** Plain line border style. */
  public static final int PLAIN_LINE = 0;

  /** Raised line border style. */
  public static final int RAISED = 1;

  /** No border line border style. */
  public static final int NO_BORDER = 2;

  private int type = LINE;

  /** LineKey legend type */
  public static final int LINE = 0;

  /** ColorKey legend type */
  public static final int COLOR = 1;

  /** VectorKey legend type */
  public static final int VECTOR = 2;

  /** PointCollectionKey legend type */
  public static final int POINT = 3;

  private transient Vector changeListeners;

  static {
    try {
      BeanInfo info = Introspector.getBeanInfo(Legend.class);
      PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
      for (int i = 0; i < descriptors.length; i++) {
        PropertyDescriptor pd = descriptors[i];
        if (pd.getName().equals("instantiated")) {
          pd.setValue("transient", Boolean.TRUE);
        } else if (pd.getName().equals("panelHolder")) {
          pd.setValue("transient", Boolean.TRUE);
        }
      }
    } catch (IntrospectionException ie) {
      ie.printStackTrace();
    }
  }

  /** Default constructor. Legend id and bounds set to <code>null</code>. */
  public Legend() {
    this(null, null);
  }

  /**
   * Legend constructor.
   *
   * @param id legend identifier
   * @param boundsP bounds in physical units
   */
  public Legend(String id, Rectangle2D.Double boundsP) {
    this.id = id;
    this.boundsP = boundsP;
  }

  /**
   * Set legend identifier.
   *
   * @param id identifier
   */
  public void setId(String id) {
    String saved = this.id;
    this.id = id;
    if (saved == null || !saved.equals(this.id)) fireStateChanged();
  }

  /**
   * Get legend identifier
   *
   * @return ident
   */
  public String getId() {
    return id;
  }

  /**
   * Set legend bounds.
   *
   * @param boundsP bounds in physical coordinates
   */
  public void setBoundsP(Rectangle2D.Double boundsP) {
    Rectangle2D.Double saved = this.boundsP;
    this.boundsP = boundsP;
    if (saved == null || !saved.equals(this.boundsP)) fireStateChanged();
  }

  /**
   * Get Legend bounds.
   *
   * @return bounds
   */
  public Rectangle2D.Double getBoundsP() {
    return boundsP;
  }

  /**
   * Set the location of the TOP-LEFT corner
   *
   * @param locationP upper left corner in physical coordinates
   */
  public void setLocationP(Point2D.Double locationP) {
    double x = boundsP.x;
    double y = boundsP.y - boundsP.height;

    boundsP.x = locationP.x;
    boundsP.y = locationP.y;
    if (x != boundsP.x || y != boundsP.y) fireStateChanged();
  }

  /**
   * Get the location of the upper left corner.
   *
   * @return upper left corner
   */
  public Point2D.Double getLocationP() {
    return new Point2D.Double(boundsP.x, boundsP.y + boundsP.height);
  }

  /**
   * Set legend height.
   *
   * @param heightP height on physical coordinates
   */
  public void setHeightP(double heightP) {
    double saved = boundsP.height;
    boundsP.height = (float) heightP;
    if (boundsP.height != saved) fireStateChanged();
  }

  /**
   * Get legend height.
   *
   * @return height
   */
  public double getHeightP() {
    return boundsP.height;
  }

  /**
   * Set legend width
   *
   * @param widthP width in physcial coordinates
   */
  public void setWidthP(double widthP) {
    double saved = boundsP.width;
    boundsP.width = (float) widthP;
    if (boundsP.width != saved) fireStateChanged();
  }

  /**
   * Get legend width
   *
   * @return width
   */
  public double getWidthP() {
    return boundsP.width;
  }

  /**
   * Remove change listener.
   *
   * @param l change listener
   */
  public synchronized void removeChangeListener(ChangeListener l) {
    if (changeListeners != null && changeListeners.contains(l)) {
      Vector v = (Vector) changeListeners.clone();
      v.removeElement(l);
      changeListeners = v;
    }
  }

  /**
   * Add change listener.
   *
   * @param l change listener
   */
  public synchronized void addChangeListener(ChangeListener l) {
    if (Page.DEBUG) System.out.println("Legend.addChangeListener(" + l + ")");
    Vector v = changeListeners == null ? new Vector(2) : (Vector) changeListeners.clone();
    if (!v.contains(l)) {
      v.addElement(l);
      changeListeners = v;
    }
  }

  /** Remove change listeners that implement <code>DesignListener</code>. */
  public synchronized void removeDesignChangeListeners() {
    if (changeListeners != null) {
      Vector v = (Vector) changeListeners.clone();
      Iterator iter = v.iterator();
      while (iter.hasNext()) {
        Object obj = iter.next();
        if (obj instanceof DesignListener) changeListeners.removeElement(obj);
      }
    }
  }

  /** Remove all change listeners. */
  public synchronized void removeAllChangeListeners() {
    changeListeners = null;
  }

  protected void fireStateChanged() {
    if (changeListeners != null) {
      Vector listeners = changeListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((ChangeListener) listeners.elementAt(i)).stateChanged(changeEvent_);
      }
    }
  }

  /**
   * Set visibility for legend.
   *
   * @param visible visibility state
   */
  public void setVisible(boolean visible) {
    boolean saved = this.visible;
    this.visible = visible;
    if (saved != this.visible) fireStateChanged();
  }

  /**
   * Test if legend visible.
   *
   * @return true, if legend is visible
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * Set legend state to instatiated. This is called internally when the property Key has been
   * instatiated.
   *
   * @param instantiated instatiation state
   */
  public void setInstantiated(boolean instantiated) {
    this.instantiated = instantiated;
  }

  /**
   * Test if the key instantiated.
   *
   * @return true, if key has been instantiated
   */
  public boolean isInstantiated() {
    return instantiated;
  }

  /**
   * Test if legend of type COLOR.
   *
   * @return true, if legend is COLOR
   */
  public boolean isColor() {
    return type == Legend.COLOR;
  }

  /**
   * Get legend type.
   *
   * @return legend type.
   */
  public int getType() {
    return type;
  }

  /**
   * Set legend type. Types include <code>COLOR</code>, <code>LINE</code>, <code>POINT</code>, and
   * <code>VECTOR</code>.
   *
   * @param type legend type
   */
  public void setType(int type) {
    this.type = type;
  }

  /**
   * Get legend border style.
   *
   * @return border style
   */
  public int getBorderStyle() {
    return borderStyle;
  }

  /**
   * Set legend border style. Border styles include: <code>PLAIN_LINE</code>, <code>RAISED</code>,
   * and <code>NO_BORDER</code>. Default = NO_BORDER.
   *
   * @param borderStyle border style
   */
  public void setBorderStyle(int borderStyle) {
    int saved = this.borderStyle;
    this.borderStyle = borderStyle;
    if (saved != this.borderStyle) fireStateChanged();
  }

  /**
   * Get number of columns. Not used with <code>COLOR</code> legends.
   *
   * @return number of columns
   */
  public int getColumns() {
    return columns;
  }

  /**
   * Set number of columns. Not used with <code>COLOR</code> legends. Default = 1.
   *
   * @param columns number of columns
   */
  public void setColumns(int columns) {
    int saved = this.columns;
    this.columns = columns;
    if (saved != this.columns) fireStateChanged();
  }

  /**
   * Get the legend line, or vector length in physical coordinages. Not used with <code>COLOR</code>
   * legends.
   *
   * @return line or vector length
   */
  public double getLineLength() {
    return lineLength;
  }

  /**
   * Set the legend line or vector lenght in physical units. Not used with <code>COLOR</code>
   * legends. Defautl = 0.3
   *
   * @param lineLength line or vector length
   */
  public void setLineLength(double lineLength) {
    double saved = this.lineLength;
    this.lineLength = lineLength;
    if (saved != this.lineLength) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale color. Only used with <code>COLOR</code> legends.
   *
   * @return scale color
   */
  public Color getScaleColor() {
    return scaleColor;
  }

  /**
   * Set <code>COLOR</code> legend scale color. Only used with <code>COLOR</code> legends. Deault =
   * black.
   *
   * @param scaleColor scale color
   */
  public void setScaleColor(Color scaleColor) {
    Color saved =
        new Color(
            this.scaleColor.getRed(),
            this.scaleColor.getGreen(),
            this.scaleColor.getBlue(),
            this.scaleColor.getAlpha());
    this.scaleColor = scaleColor;
    if (!saved.equals(this.scaleColor)) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale font. Only used with <code>COLOR</code> legends.
   *
   * @return scale font
   */
  public Font getScaleLabelFont() {
    return scaleLabelFont;
  }

  /**
   * Set <code>COLOR</code> legend scale font. Only used with <code>COLOR</code> legends. Deault =
   * ("Helvetica", PLAIN, 10).
   *
   * @param scaleLabelFont scale font
   */
  public void setScaleLabelFont(Font scaleLabelFont) {
    Font saved =
        new Font(
            this.scaleLabelFont.getName(),
            this.scaleLabelFont.getStyle(),
            this.scaleLabelFont.getSize());
    this.scaleLabelFont = scaleLabelFont;
    if (!saved.equals(this.scaleLabelFont)) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale label height in physical coordinates. Only used with <code>
   * COLOR</code> legends.
   *
   * @return scale label height
   */
  public double getScaleLabelHeightP() {
    return scaleLabelHeightP;
  }

  /**
   * Set <code>COLOR</code> legend scale label heigth. Only used with <code>COLOR</code> legends.
   * Deault = 0.2.
   *
   * @param scaleLabelHeightP scale label height
   */
  public void setScaleLabelHeightP(double scaleLabelHeightP) {
    double saved = this.scaleLargeTicHeightP;
    this.scaleLabelHeightP = scaleLabelHeightP;
    if (saved != this.scaleLabelHeightP) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale label interval. Only used with <code>COLOR</code> legends.
   *
   * @return scale label interval
   */
  public int getScaleLabelInterval() {
    return scaleLabelInterval;
  }

  /**
   * Set <code>COLOR</code> legend scale label interval. Only used with <code>COLOR</code> legends.
   * Deault = 2.
   *
   * @param scaleLabelInterval scale label interval
   */
  public void setScaleLabelInterval(int scaleLabelInterval) {
    int saved = this.scaleLabelInterval;
    this.scaleLabelInterval = scaleLabelInterval;
    if (saved != this.scaleLabelInterval) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale number of small tics. Only used with <code>COLOR</code>
   * legends.
   *
   * @return scale number of small tics
   */
  public int getScaleNumberSmallTics() {
    return scaleNumberSmallTics;
  }

  /**
   * Set <code>COLOR</code> legend scale number of small tics. Only used with <code>COLOR</code>
   * legends. Deault = 0.
   *
   * @param scaleNumberSmallTics scale number of small tics
   */
  public void setScaleNumberSmallTics(int scaleNumberSmallTics) {
    int saved = this.scaleNumberSmallTics;
    this.scaleNumberSmallTics = scaleNumberSmallTics;
    if (saved != this.scaleNumberSmallTics) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale label format. Only used with <code>COLOR</code> legends.
   *
   * @return scale label format
   */
  public String getScaleLabelFormat() {
    return scaleLabelFormat;
  }

  /**
   * Set <code>COLOR</code> legend scale label format. Only used with <code>COLOR</code> legends.
   * Deault = "".
   *
   * @param scaleLabelFormat scale label format
   */
  public void setScaleLabelFormat(String scaleLabelFormat) {
    String saved = this.scaleLabelFormat;
    this.scaleLabelFormat = scaleLabelFormat;
    if (!saved.equals(this.scaleLabelFormat)) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale large tick height. Only used with <code>COLOR</code>
   * legends.
   *
   * @return scale large tick height
   */
  public double getScaleLargeTicHeightP() {
    return scaleLargeTicHeightP;
  }

  /**
   * Set <code>COLOR</code> legend scale large tick height in physical coordinates. Only used with
   * <code>COLOR</code> legends. Deault = 0.1.
   *
   * @param scaleLargeTicHeightP scale large tick height
   */
  public void setScaleLargeTicHeightP(double scaleLargeTicHeightP) {
    double saved = this.scaleLargeTicHeightP;
    this.scaleLargeTicHeightP = scaleLargeTicHeightP;
    if (saved != this.scaleLargeTicHeightP) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale significant digits. Only used with <code>COLOR</code>
   * legends.
   *
   * @return scale significant digits
   */
  public int getScaleSignificantDigits() {
    return scaleSignificantDigits;
  }

  /**
   * Set <code>COLOR</code> legend scale significant digits. Only used with <code>COLOR</code>
   * legends. Deault = 2.
   *
   * @param scaleSignificantDigits scale significant digits
   */
  public void setScaleSignificantDigits(int scaleSignificantDigits) {
    int saved = this.scaleSignificantDigits;
    this.scaleSignificantDigits = scaleSignificantDigits;
    if (saved != this.scaleSignificantDigits) fireStateChanged();
  }

  /**
   * Get <code>COLOR</code> legend scale small tick height. Only used with <code>COLOR</code>
   * legends.
   *
   * @return scale small tick height
   */
  public double getScaleSmallTicHeightP() {
    return scaleSmallTicHeightP;
  }

  /**
   * Set <code>COLOR</code> legend scale small tick height in physical coordinates. Only used with
   * <code>COLOR</code> legends. Deault = 0.05.
   *
   * @param scaleSmallTicHeightP scale small tick height
   */
  public void setScaleSmallTicHeightP(double scaleSmallTicHeightP) {
    double saved = this.scaleSmallTicHeightP;
    this.scaleSmallTicHeightP = scaleSmallTicHeightP;
    if (saved != this.scaleSmallTicHeightP) fireStateChanged();
  }

  /**
   * Test if <code>COLOR</code> legend scale visible. Only used with <code>COLOR</code> legends.
   *
   * @return true, if scale visible
   */
  public boolean isScaleVisible() {
    return scaleVisible;
  }

  /**
   * Set <code>COLOR</code> legend scale visible. Only used with <code>COLOR</code> legends. Deault
   * = true.
   *
   * @param scaleVisible scale visible
   */
  public void setScaleVisible(boolean scaleVisible) {
    boolean saved = this.scaleVisible;
    this.scaleVisible = scaleVisible;
    if (saved != this.scaleVisible) fireStateChanged();
  }

  /**
   * Get <code>PanelHolder</code> parent.
   *
   * @return panelholder
   */
  public PanelHolder getPanelHolder() {
    return pHolder_;
  }

  /**
   * Set <code>PanelHolder</code> parent.
   *
   * @param pHolder panelholder
   */
  public void setPanelHolder(PanelHolder pHolder) {
    pHolder_ = pHolder;
  }

  /**
   * Get key label height.
   *
   * @return key label height
   */
  public double getKeyLabelHeightP() {
    return keyLabelHeightP;
  }

  /**
   * Set key label height in physical coordinates. Default = 0.16.
   *
   * @param keyLabelHeightP key label height
   */
  public void setKeyLabelHeightP(double keyLabelHeightP) {
    double saved = this.keyLabelHeightP;
    this.keyLabelHeightP = keyLabelHeightP;
    if (saved != this.keyLabelHeightP) fireStateChanged();
  }
}
