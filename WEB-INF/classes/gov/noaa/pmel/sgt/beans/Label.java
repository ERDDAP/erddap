/*
 * $Id: Label.java,v 1.4 2003/09/17 23:16:45 dwd Exp $
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

import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.Color;
import java.awt.Font;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.event.*;

/**
 * Encapsulates <code>SGLabel</code> properties.
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/09/17 23:16:45 $
 * @since 3.0
 */
public class Label implements Serializable {
  private transient PanelHolder pHolder_ = null;
  private String id = "";
  private Rectangle2D.Double boundsP = new Rectangle2D.Double(0.0, 0.0, 0.0, 0.0);
  private transient Vector changeListeners;
  private transient ChangeEvent changeEvent_ = new ChangeEvent(this);
  private String text = "";
  private int justification = SGLabel.LEFT;
  private boolean visible = true;
  private boolean instantiated = false;

  private boolean selectable = true;
  private Color color = Color.black;
  private Font font = new Font("Helvetica", Font.PLAIN, 10);
  private int orientation = SGLabel.HORIZONTAL;

  static {
    try {
      BeanInfo info = Introspector.getBeanInfo(Label.class);
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

  /**
   * Default constructor. Width and height are set to 0.0, name and location are <code>null</code>.
   */
  public Label() {
    this(null, null, 0.0f, 0.0f);
  }

  /**
   * Label constructor.
   *
   * @param id label identifier
   * @param loc location in physical coordinates
   * @param wid width in physical coordinates
   * @param hgt height in physical coorindates.
   */
  public Label(String id, Point2D.Double loc, double wid, double hgt) {
    this.id = id;
    if (loc == null) {
      boundsP = null;
    } else {
      boundsP = new Rectangle2D.Double(loc.x, loc.y, wid, hgt);
    }
  }

  /**
   * Get Label identifier.
   *
   * @return identification
   */
  public String getId() {
    return id;
  }

  /**
   * Set label identifier.
   *
   * @param id identifier
   */
  public void setId(String id) {
    String saved = this.id;
    this.id = id;
    if (this.id == null || !this.id.equals(saved)) fireStateChanged();
  }

  /**
   * Set Label bounds.
   *
   * @param bounds bounds in physical coordinates.
   */
  public void setBoundsP(Rectangle2D.Double bounds) {
    Rectangle2D.Double saved = boundsP;
    boundsP = bounds;
    if (saved == null || !saved.equals(boundsP)) fireStateChanged();
  }

  /**
   * Get Label bounds.
   *
   * @return bounds
   */
  public Rectangle2D.Double getBoundsP() {
    return boundsP;
  }

  /**
   * Set Label location. Updates x and y in bounds.
   *
   * @param locationP location in physical coordinates.
   */
  public void setLocationP(Point2D.Double locationP) {
    double x = boundsP.x;
    double y = boundsP.y;

    boundsP.x = locationP.x;
    boundsP.y = locationP.y;
    if (x != boundsP.x || y != boundsP.y) fireStateChanged();
  }

  /**
   * Get Label location
   *
   * @return location in physical coordinates
   */
  public Point2D.Double getLocationP() {
    return new Point2D.Double(boundsP.x, boundsP.y);
  }

  /**
   * Set Label height. Updates height in bounds.
   *
   * @param heightP height in physical coordinates
   */
  public void setHeightP(double heightP) {
    double saved = boundsP.height;
    boundsP.height = (float) heightP;
    if (boundsP.height != saved) fireStateChanged();
  }

  /**
   * Get Label height.
   *
   * @return height
   */
  public double getHeightP() {
    return boundsP.height;
  }

  /**
   * Set label width. Updates width in bounds.
   *
   * @param widthP width in physcial coordinates
   */
  public void setWidthP(double widthP) {
    double saved = boundsP.width;
    boundsP.width = (float) widthP;
    if (boundsP.width != saved) fireStateChanged();
  }

  /**
   * Get label width.
   *
   * @return width
   */
  public double getWidthP() {
    return boundsP.width;
  }

  /**
   * Remove change listener.
   *
   * @param l change listener.
   */
  public synchronized void removeChangeListener(ChangeListener l) {
    if (changeListeners != null && changeListeners.contains(l)) {
      Vector v = (Vector) changeListeners.clone();
      v.removeElement(l);
      changeListeners = v;
    }
  }

  /** Remove all change listeners. */
  public void removeAllChangeListeners() {
    changeListeners = null;
  }

  public synchronized void addChangeListener(ChangeListener l) {
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
   * Set label text.
   *
   * @param text label text
   */
  public void setText(String text) {
    String saved = this.text;
    this.text = text;
    if (this.text == null || !this.text.equals(saved)) fireStateChanged();
  }

  /**
   * Get label text.
   *
   * @return text
   */
  public String getText() {
    return text;
  }

  /**
   * Set label visiblity. Visible if true. Default = true.
   *
   * @param visible label visiblity
   */
  public void setVisible(boolean visible) {
    boolean saved = this.visible;
    this.visible = visible;
    if (saved != this.visible) fireStateChanged();
  }

  /**
   * Is Label visible?
   *
   * @return true, if label is visible
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * Set instantiation for label. Used internally. Set when SGLabel is instantiated from Label.
   *
   * @param instantiated SGLabel instantiated
   */
  public void setInstantiated(boolean instantiated) {
    this.instantiated = instantiated;
  }

  /**
   * Is SGLabel instatiated?
   *
   * @return true, if SGLabel is instantiated
   */
  public boolean isInstantiated() {
    return instantiated;
  }

  /**
   * Set panelholder.
   *
   * @param pHolder panel holder
   */
  public void setPanelHolder(PanelHolder pHolder) {
    pHolder_ = pHolder;
  }

  /**
   * Get panel holder.
   *
   * @return panel holder
   */
  public PanelHolder getPanelHolder() {
    return pHolder_;
  }

  /**
   * Get label justification.
   *
   * @return justification.
   */
  public int getJustification() {
    return justification;
  }

  /**
   * Set label justification. Justification can be SGLabel.LEFT, SGLabel.RIGHT, or SGLabel.CENTER.
   * Default = SGLabel.LEFT.
   *
   * @param justification label justification
   * @see gov.noaa.pmel.sgt.SGLabel
   */
  public void setJustification(int justification) {
    int saved = this.justification;
    this.justification = justification;
    if (saved != this.justification) fireStateChanged();
  }

  /**
   * Get label color.
   *
   * @return color
   */
  public Color getColor() {
    return color;
  }

  /**
   * Get label font.
   *
   * @return font
   */
  public Font getFont() {
    return font;
  }

  /**
   * Set label color. Default = Color.black.
   *
   * @param color label color
   */
  public void setColor(Color color) {
    Color saved = this.color;
    this.color = color;
    if (!saved.equals(this.color)) fireStateChanged();
  }

  /**
   * Set label font. Default = Helvectia, PLAIN
   *
   * @param font label font
   */
  public void setFont(Font font) {
    Font saved = this.font;
    this.font = font;
    if (!saved.equals(this.font)) fireStateChanged();
  }

  /**
   * Get label orientation.
   *
   * @return orientation
   */
  public int getOrientation() {
    return orientation;
  }

  /**
   * Set label orientation. Legal values are SGLabel.HORIZONTAL and SGLabel.VERTICAL. Default =
   * SGLabel.HORIZONTAL.
   *
   * @param orientation label orientation
   * @see gov.noaa.pmel.sgt.SGLabel
   */
  public void setOrientation(int orientation) {
    int saved = this.orientation;
    this.orientation = orientation;
    if (saved != this.orientation) fireStateChanged();
  }

  /**
   * Is label selectable?
   *
   * @return true if label is selectable
   */
  public boolean isSelectable() {
    return selectable;
  }

  /**
   * Set label selectable.
   *
   * @param selectable true if label is selectable
   */
  public void setSelectable(boolean selectable) {
    boolean saved = this.selectable;
    this.selectable = selectable;
    if (saved != this.selectable) fireStateChanged();
  }
}
