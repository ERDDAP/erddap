/*
 * $Id: SGLabel.java,v 1.20 2003/08/22 23:02:32 dwd Exp $
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
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.*;
import java.beans.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

// jdk1.2
// import  java.awt.geom.Rectangle2D;
// import  java.awt.geom.Point2D;

/**
 * Draws text on a layer object. SGLabel uses the drawString() method of the Graphics class. SGLabel
 * allows the user to align the text both vertically (TOP, MIDDLE, and BOTTOM) and horizontally
 * (LEFT, MIDDLE, and RIGHT). The font, color, and height (in user coordinates) can also be
 * specified. The SGLabel can also be drawn either HORIZONTAL or VERTICAL.
 *
 * @author Donald Denbo
 * @version $Revision: 1.20 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 * @see java.awt.Graphics
 */
public class SGLabel implements Cloneable, LayerChild, Moveable, Serializable {
  private String ident_;
  private LabelDrawer proxy_;
  private boolean selected_;
  private boolean selectable_;
  private boolean moveable_;
  private transient PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  /** Align top of label */
  public static final int TOP = 0;

  /** Align middle of label */
  public static final int MIDDLE = 1;

  /** Align bottom of label */
  public static final int BOTTOM = 2;

  /** Align left of label */
  public static final int LEFT = 0;

  /** Align center of label */
  public static final int CENTER = 1;

  /** Align right of label */
  public static final int RIGHT = 2;

  /** Orient label horizontal */
  public static final int HORIZONTAL = 0;

  /** Orient label vertical */
  public static final int VERTICAL = 1;

  /**
   * Orient label at an angle
   *
   * @since 2.0
   */
  public static final int ANGLE = 2;

  static {
    try {
      BeanInfo info = Introspector.getBeanInfo(SGLabel.class);
      PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
      for (int i = 0; i < descriptors.length; i++) {
        PropertyDescriptor pd = descriptors[i];
        if (pd.getName().equals("layer")) {
          pd.setValue("transient", Boolean.TRUE);
        }
      }
    } catch (IntrospectionException ie) {
      ie.printStackTrace();
    }
  }

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      proxy_ = null;
      changes_ = null;
      if (JPane.debug) String2.log("sgt.SGLabel.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /**
   * Quick SGLabel constructor. Default alignment of BOTTOM and LEFT are used. Default height is
   * 0.12.
   *
   * @param id Label identifier
   * @param lbl String to be plotted.
   */
  public SGLabel(String id, String lbl, Point2D.Double loc) {
    this(id, lbl, 0.12, loc, BOTTOM, LEFT);
  }

  /**
   * Long SGLabel constructor.
   *
   * @param id Label identifier
   * @param lbl String to be plotted
   * @param hgt String height in physical units
   * @param loc Location to plot label in physical units
   * @param valign Vertical alignment
   * @param halign Horizontal alignment
   */
  public SGLabel(String id, String lbl, double hgt, Point2D.Double loc, int valign, int halign) {
    if (PaneProxy.Java2D) {
      proxy_ = new LabelDrawer2(lbl, hgt, loc, valign, halign);
    } else {
      proxy_ = new LabelDrawer1(lbl, hgt, loc, valign, halign);
    }
    ident_ = id;
    proxy_.setOrientation(HORIZONTAL);
    proxy_.setAngle(0.0);
    proxy_.setColor(null);
    proxy_.setFont(new Font("Helvetica", Font.PLAIN, 14));
    selected_ = false;
    selectable_ = true;
    proxy_.setVisible(true);
    moveable_ = true;
  }

  //    private void bindStringDrawer() {
  //      Class cl;
  //      boolean java2d = true;
  //      try {
  //        cl = Class.forName("java.awt.Graphics2D");
  //      } catch (ClassNotFoundException e) {
  //        java2d = false;
  //      }
  //      if(java2d) {
  //        stringDraw_ = new StringDrawer2();
  //      } else {
  //        stringDraw_ = new StringDrawer1();
  //      }
  //    }

  @Override
  public LayerChild copy() {
    SGLabel newLabel;
    try {
      newLabel = (SGLabel) clone();
    } catch (CloneNotSupportedException e) {
      newLabel =
          new SGLabel(
              ident_,
              proxy_.getText(),
              proxy_.getHeightP(),
              proxy_.getLocationP(),
              proxy_.getVAlign(),
              proxy_.getHAlign());
      newLabel.setColor(proxy_.getColor());
      newLabel.setFont(proxy_.getFont());
      if (proxy_.getOrientation() == ANGLE) {
        newLabel.setAngle(proxy_.getAngle());
      } else {
        newLabel.setOrientation(proxy_.getOrientation());
      }
    }
    return newLabel;
  }

  /**
   * @since 3.0
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SGLabel)) return false;
    SGLabel sg = (SGLabel) obj;
    /*    boolean t1 = !ident_.equals(sg.getId());
    boolean t2 = !proxy_.getText().equals(sg.getText());
    boolean t3 = proxy_.getHeightP() != sg.getHeightP();
    boolean t4 = !proxy_.getLocationP().equals(sg.getLocationP());
    boolean t5 = proxy_.getVAlign() != sg.getVAlign();
    boolean t6 = proxy_.getHAlign() != sg.getHAlign();
    boolean t7 = (proxy_.getColor() != null) && !proxy_.getColor().equals(sg.getColor());
    boolean t8 = (proxy_.getFont() != null) && !proxy_.getFont().equals(sg.getFont());
    boolean t9 = proxy_.getOrientation() != sg.getOrientation();
    if(t1 || t2 || t3 || t4 || t5 || t6 || t7 || t8 || t9) return false; */
    if (!ident_.equals(sg.getId())
        || !proxy_.getText().equals(sg.getText())
        || (proxy_.getHeightP() != sg.getHeightP())
        || !proxy_.getLocationP().equals(sg.getLocationP())
        || (proxy_.getVAlign() != sg.getVAlign())
        || (proxy_.getHAlign() != sg.getHAlign())
        || ((proxy_.getColor() != null) && !proxy_.getColor().equals(sg.getColor()))
        || ((proxy_.getFont() != null) && !proxy_.getFont().equals(sg.getFont()))
        || (proxy_.getOrientation() != sg.getOrientation())) return false;
    if (proxy_.getOrientation() == ANGLE) {
      if (proxy_.getAngle() != sg.getAngle()) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    // We might want to include some proxy values, but ideally proxy would
    // be able to provide its own hash.
    return ident_.hashCode();
  }

  @Override
  public void draw(Graphics g) throws LayerNotFoundException {
    proxy_.draw(g);
  }

  @Override
  public void setSelected(boolean sel) {
    selected_ = sel;
  }

  @Override
  public boolean isSelected() {
    return selected_;
  }

  @Override
  public void setSelectable(boolean select) {
    selectable_ = select;
  }

  @Override
  public boolean isSelectable() {
    return selectable_;
  }

  /**
   * Set the color.
   *
   * @param color The color of the label.
   * @see java.awt.Color
   */
  public void setColor(Color color) {
    Color clr = proxy_.getColor();
    if (clr == null || !clr.equals(color)) {
      proxy_.setColor(color);
      modified("SGLabel: setColor()");
    }
  }

  /**
   * Get the color.
   *
   * @return The current color of the label.
   */
  public Color getColor() {
    return proxy_.getColor();
  }

  /**
   * Set the font.
   *
   * @param fnt The Font to use to draw the label.
   * @see java.awt.Font
   */
  public void setFont(Font fnt) {
    Font font = proxy_.getFont();
    if (font == null || !font.equals(fnt)) {
      proxy_.setFont(fnt);
      modified("SGLabel: setFont()");
    }
  }

  /**
   * Get the font.
   *
   * @return The current font for the label.
   */
  public Font getFont() {
    return proxy_.getFont();
  }

  /**
   * Set the height of the label in physical coordinates.
   *
   * @param hgt The label height.
   */
  public void setHeightP(double hgt) {
    double height = proxy_.getHeightP();
    if (height != hgt) {
      proxy_.setHeightP(hgt);
      modified("SGLabel: setHeightP()");
    }
  }

  /**
   * Get the label height in physical coordinates.
   *
   * @return The label height.
   */
  public double getHeightP() {
    return proxy_.getHeightP();
  }

  /**
   * Set the vertical and horizontal alignment. The vertical alignment can be TOP, MIDDLE, or
   * BOTTOM, and the horizontal alignment LEFT, CENTER, or RIGHT.
   *
   * @param vert The vertical alignment.
   * @param horz The horizontal alignment.
   */
  public void setAlign(int vert, int horz) {
    int valign = proxy_.getVAlign();
    int halign = proxy_.getHAlign();
    if (valign != vert || halign != horz) {
      proxy_.setVAlign(vert);
      proxy_.setHAlign(horz);
      modified("SGLabel: setAlign()");
    }
  }

  /**
   * Set the horizontal alignment. The alignment can be LEFT, CENTER, or RIGHT.
   *
   * @param horz The horizontal alignment.
   */
  public void setHAlign(int horz) {
    int halign = proxy_.getHAlign();
    if (halign != horz) {
      proxy_.setHAlign(horz);
      modified("SGLabeo: setHAlign()");
    }
  }

  /**
   * Get the horizontal alignment.
   *
   * @return the horizontal alignment.
   */
  public int getHAlign() {
    return proxy_.getHAlign();
  }

  /**
   * Set the vertical alignment. The alignment can be TOP, MIDDLE, or BOTTOM.
   *
   * @param vert The vertical alignment.
   */
  public void setVAlign(int vert) {
    int valign = proxy_.getVAlign();
    if (valign != vert) {
      proxy_.setVAlign(vert);
      modified("SGLabel: setVAlign()");
    }
  }

  /**
   * Get the vertical alignment.
   *
   * @return the vertical alignment.
   */
  public int getVAlign() {
    return proxy_.getVAlign();
  }

  /**
   * Set the label reference location in physcial coordinates. <br>
   * <strong>Property Change:</strong> <code>location</code>.
   *
   * @param loc physical location of label
   */
  public void setLocationP(Point2D.Double loc) {
    Point2D.Double porigin = proxy_.getLocationP();
    if (porigin == null || !porigin.equals(loc)) {
      Point2D.Double temp = porigin;
      porigin = loc;
      proxy_.setLocationP(loc);
      if (changes_ == null) changes_ = new PropertyChangeSupport(this);
      changes_.firePropertyChange("location", temp, porigin);
      modified("SGLabel: setLocationP()");
    }
  }

  /**
   * Get the label reference location in physcial coordinates.
   *
   * @return the labels position.
   */
  public Point2D.Double getLocationP() {
    return proxy_.getLocationP();
  }

  /**
   * Set the orientation. The orientation can be HORIZONTAL or VERTICAL.
   *
   * @param orient The orientation.
   */
  public void setOrientation(int orient) {
    int or = proxy_.getOrientation();
    if (or != orient) {
      proxy_.setOrientation(orient);
      modified("SGLabel: setOrientation()");
    }
  }

  /**
   * Get the origentation.
   *
   * @return the orientation
   */
  public int getOrientation() {
    return proxy_.getOrientation();
  }

  /**
   * Draw label at arbitrary rotation. Warning: Rotated labels are not drawn very well when using
   * JDK1.1. For best results use JDK1.2 or newer.
   *
   * @since 2.0
   */
  public void setAngle(double angle) {
    proxy_.setAngle(angle);
  }

  /**
   * Get label drawing angle.
   *
   * @since 2.0
   */
  public double getAngle() {
    return proxy_.getAngle();
  }

  /** */
  @Override
  public void setLayer(Layer l) {
    proxy_.setLayer(l);
  }

  /**
   * Get the layer.
   *
   * @return the layer object.
   */
  @Override
  public Layer getLayer() {
    return proxy_.getLayer();
  }

  @Override
  public AbstractPane getPane() {
    return proxy_.getLayer().getPane();
  }

  @Override
  public void modified(String text) {
    Layer layer = proxy_.getLayer();
    if (layer != null) {
      layer.modified(text);
    }
  }

  /**
   * Get the label text.
   *
   * @return the label text
   */
  public String getText() {
    return proxy_.getText();
  }

  /**
   * Set the label text.
   *
   * @param lbl the label text
   */
  public void setText(String lbl) {
    String label = proxy_.getText();
    if (label == null || !label.equals(lbl)) {
      proxy_.setText(lbl);
      modified("SGLabel: setText()");
    }
  }

  /**
   * Get the label identifier.
   *
   * @return the identifier
   */
  @Override
  public String getId() {
    return ident_;
  }

  /**
   * Set the label identifier.
   *
   * @param id the label identifier
   */
  @Override
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Get the label height in device coordinates.
   *
   * @return the label height
   */
  public int getHeight() {
    return 0;
  }

  /**
   * Get the label position in device coordinates.
   *
   * @return the label position
   */
  @Override
  public Point getLocation() {
    return proxy_.getLocation();
  }

  /**
   * Set the label reference location in pixel coordinates. <br>
   * <strong>Property Change:</strong> <code>location</code>.
   *
   * @param loc physical location of label
   */
  @Override
  public void setLocation(Point loc) {
    Point dloc = proxy_.getLocation();
    if (dloc.x != loc.x || dloc.y != loc.y) {
      Point temp = new Point(dloc.x, dloc.y);
      proxy_.setLocation(loc);
      if (changes_ == null) changes_ = new PropertyChangeSupport(this);
      changes_.firePropertyChange("location", temp, loc);
    }
  }

  /**
   * Get the label bounds in physical units.
   *
   * @return the label bounds
   */
  public Rectangle2D.Double getBoundsP() {
    return proxy_.getBoundsP();
  }

  /**
   * Get the label bounds in device units.
   *
   * @return the label bounds
   */
  @Override
  public Rectangle getBounds() {
    return proxy_.getBounds();
  }

  /** Set the label bounds in device units. */
  public void setBounds(Rectangle r) {
    setBounds(r.x, r.y, r.width, r.height);
  }

  /** Set the label bounds in device units. */
  public void setBounds(int x, int y, int width, int height) {
    proxy_.setBounds(x, y, width, height);
  }

  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + ident_;
  }

  @Override
  public boolean isVisible() {
    return proxy_.isVisible();
  }

  @Override
  public void setVisible(boolean visible) {
    boolean vis = proxy_.isVisible();
    if (vis != visible) {
      proxy_.setVisible(visible);
      modified("SGLabel: setVisible()");
    }
  }

  @Override
  public boolean isMoveable() {
    return moveable_;
  }

  @Override
  public void setMoveable(boolean moveable) {
    if (moveable_ != moveable) {
      moveable_ = moveable;
      modified("SGLabel: setMoveable()");
    }
  }

  /**
   * Get the string width in device units.
   *
   * @since 2.0
   */
  public float getStringWidth(Graphics g) {
    return proxy_.getStringWidth(g);
  }

  /**
   * Get the string height in device units.
   *
   * @since 2.0
   */
  public float getStringHeight(Graphics g) {
    return proxy_.getStringHeight(g);
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    if (changes_ == null) changes_ = new PropertyChangeSupport(this);
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
