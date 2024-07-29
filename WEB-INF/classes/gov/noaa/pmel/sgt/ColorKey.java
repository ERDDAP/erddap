/*
 * $Id: ColorKey.java,v 1.20 2003/08/22 23:02:31 dwd Exp $
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
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

// jdk1.2
// import java.awt.geom.Rectangle2D;
// import java.awt.geom.Point2D;

/**
 * The <code>ColorKey</code> class provides a graphical depiction of the relationship between a
 * <code>ColorMap</code> and user values. A single <code>ColorKey</code> can be attached to a <code>
 * Layer</code>. A <code>ColorMap</code> is associated with the Key and therefor with a specific
 * transformation and optionally a <code>SGTData</code> object.
 *
 * @author Donald Denbo
 * @version $Revision: 1.20 $, $Date: 2003/08/22 23:02:31 $
 * @since 1.0
 * @see Ruler
 * @see ColorMap
 * @see Layer
 */
public class ColorKey implements Cloneable, DataKey, PropertyChangeListener {
  // Scale members
  /**
   * @label title
   * @link aggregationByValue
   * @undirected
   */
  private SGLabel title_ = null;

  //
  private String ident_;

  /**
   * @directed
   */
  private Layer layer_;

  /**
   * @label scale
   * @link aggregationByValue
   */
  private Ruler scale_ = new Ruler(); // mod Tom Ewing

  private int valign_;
  private int halign_;
  private int orient_;
  private int style_;
  private boolean selected_;
  private boolean selectable_;
  private boolean visible_;

  /** maximum bounds of key in p-space */
  //  private Rectangle2D.Double pbounds_;
  private Point2D.Double porigin_;

  private Dimension2D psize_;

  /** top, left, bottom, right in p-space */
  private double[] insets_ = {0.05, 0.15, 0.05, 0.15};

  private double barWidth_ = 0.2;

  /**
   * @label cm
   */
  private ColorMap cm_ = null;

  /** Use plain line border. */
  public static final int PLAIN_LINE = 0;

  /** Use raised border. */
  public static final int RAISED = 1;

  /** Do not draw a border. */
  public static final int NO_BORDER = 2;

  /** Align to top of key. */
  public static final int TOP = 0;

  /** Align to middle of key. */
  public static final int MIDDLE = 1;

  /** Align to bottom of key. */
  public static final int BOTTOM = 2;

  /** Align to left of key. */
  public static final int LEFT = 0;

  /** Align to center of key. */
  public static final int CENTER = 1;

  /** Align to right of key. */
  public static final int RIGHT = 2;

  /** Orient key horizontally. */
  public static final int HORIZONTAL = 1;

  /** Orient key vertically. */
  public static final int VERTICAL = 2;

  //  private boolean moveable_ = false;
  //  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      title_ = null;
      layer_ = null;
      scale_ = null;
      cm_ = null;
      if (JPane.debug) String2.log("sgt.ColorKey.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /**
   * Default <code>ColorKey</code> constructor. The location and size must be specified before the
   * <code>ColorKey</code> is attached to a layer!
   */
  public ColorKey() {
    this(new Rectangle2D.Double(), BOTTOM, LEFT);
  }

  /**
   * <code>ColorKey</code> constructor that include location, size, and alignment information.
   * Default orientation is <code>HORIZONTAL</code>.
   *
   * @param pr a Rectangle2D object that includes location and size
   * @param valign vertical alignment
   * @param halign horizontal alignment
   */
  public ColorKey(Rectangle2D.Double pr, int valign, int halign) {
    this(new Point2D.Double(pr.x, pr.y), new Dimension2D(pr.width, pr.height), valign, halign);
  }

  /**
   * @since 3.0
   */
  public ColorKey(Point2D.Double pt, Dimension2D size, int valign, int halign) {
    porigin_ = pt;
    psize_ = size;
    //    pbounds_ = new Rectangle2D.Double(pt.x, pt.y, size.width, size.height);
    valign_ = valign;
    halign_ = halign;
    //
    // set defaults
    //
    orient_ = HORIZONTAL;
    selected_ = false;
    selectable_ = true;
    visible_ = true;
    style_ = PLAIN_LINE;
    //    moveable_ = false;

  }

  /**
   * Create a copy of <code>ColorKey</code>.
   *
   * @return a copy of the key
   */
  @Override
  public LayerChild copy() {
    ColorKey newKey;
    try {
      newKey = (ColorKey) clone();
    } catch (CloneNotSupportedException e) {
      newKey = new ColorKey();
    }
    return (LayerChild) newKey;
  }

  /**
   * Sets the <code>selected</code> property.
   *
   * @since 2.0
   * @param sel true if selected, false if not.
   */
  @Override
  public void setSelected(boolean sel) {
    selected_ = sel;
  }

  /**
   * Returns true if the <code>selected</code> property is set.
   *
   * @return true is selected, false if not.
   */
  @Override
  public boolean isSelected() {
    return selected_;
  }

  /**
   * Sets the selectable property.
   *
   * @since 2.0
   */
  @Override
  public void setSelectable(boolean select) {
    selectable_ = select;
  }

  /**
   * Tests the selectable property.
   *
   * @since 2.0
   */
  @Override
  public boolean isSelectable() {
    return selectable_;
  }

  /**
   * Set ColorKey identifier.
   *
   * @param id key identifier
   */
  @Override
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Get <code>ColorKey</code> identifier
   *
   * @return identifier
   */
  @Override
  public String getId() {
    return ident_;
  }

  /**
   * Set parent <code>Layer</code>. Method should not be called directly, called when the <code>
   * Layer.addChild</code> method is called.
   *
   * @param l parent layer
   */
  @Override
  public void setLayer(Layer l) {
    layer_ = l;
  }

  /**
   * Returns the layer the ColorKey is attached.
   *
   * @return The parent layer.
   * @see Layer
   */
  @Override
  public Layer getLayer() {
    return layer_;
  }

  /**
   * Get the parent pane.
   *
   * @since 2.0
   */
  @Override
  public AbstractPane getPane() {
    return layer_.getPane();
  }

  /**
   * For internal sgt use.
   *
   * @since 2.0
   */
  @Override
  public void modified(String mess) {
    //    if(Debug.EVENT) System.out.println("ColorKey: modified()");
    if (layer_ != null) layer_.modified(mess);
  }

  /**
   * Set color map.
   *
   * @param cm color map
   */
  public void setColorMap(ColorMap cm) {
    if (cm_ == null || !cm_.equals(cm)) {
      if (cm_ != null) cm_.removePropertyChangeListener(this);
      cm_ = cm;
      cm_.addPropertyChangeListener(this);
      modified("ColorKey: setColorMap()");
    }
  }

  /**
   * Add a GridCartesianRenderer and label to the ColorKey.
   *
   * @param rend GridCartesianRenderer object
   * @param label descriptive label
   * @since 3.0
   */
  @Override
  public void addGraph(CartesianRenderer rend, SGLabel label) throws IllegalArgumentException {
    if (!(rend instanceof GridCartesianRenderer))
      throw new IllegalArgumentException("Renderer is not a GridCartesianRenderer");
    GridAttribute ga = (GridAttribute) ((GridCartesianRenderer) rend).getAttribute();
    ColorMap cm = ga.getColorMap();
    setColorMap(cm);
    setTitle(label);
    //   addVectorGraph((VectorCartesianRenderer)rend, label);
  }

  /**
   * Get color map.
   *
   * @return color map
   */
  public ColorMap getColorMap() {
    return cm_;
  }

  /**
   * Set border style.
   *
   * @param style border style
   * @see #PLAIN_LINE
   * @see #RAISED
   * @see #NO_BORDER
   */
  @Override
  public void setBorderStyle(int style) {
    if (style_ != style) {
      style_ = style;
      modified("LineKey: setBorderStyle()");
    }
  }

  /**
   * Get border style.
   *
   * @return border style
   */
  public int getBorderStyle() {
    return style_;
  }

  /**
   * Set color key alignment.
   *
   * @param vert vertical alignment
   * @param horz horizontal alignment
   */
  @Override
  public void setAlign(int vert, int horz) {
    if (valign_ != vert || halign_ != horz) {
      valign_ = vert;
      halign_ = horz;
      modified("ColorKey: setAlign()");
    }
  }

  /**
   * Set orientation.
   *
   * @param orient key orientation
   */
  public void setOrientation(int orient) {
    if (orient_ != orient) {
      orient_ = orient;
      modified("ColorKey: setOrientation()");
    }
  }

  /**
   * Set vertical alignment
   *
   * @param vert vertical alignment
   */
  @Override
  public void setVAlign(int vert) {
    if (valign_ != vert) {
      valign_ = vert;
      modified("ColorKey: setVAlign()");
    }
  }

  /**
   * Set horizontal alignment
   *
   * @param horz horizontal alignment
   */
  @Override
  public void setHAlign(int horz) {
    if (halign_ != horz) {
      halign_ = horz;
      modified("ColorKey: setHAlign()");
    }
  }

  /**
   * Get vertical alignment
   *
   * @return vertical alignment
   */
  public int getVAlign() {
    return valign_;
  }

  /**
   * Get horizontal alignment
   *
   * @return horizontal alignment
   */
  public int getHAlign() {
    return halign_;
  }

  /**
   * Set location of key in physical coordinates.
   *
   * @param loc key location
   */
  @Override
  public void setLocationP(Point2D.Double loc) {
    if (porigin_.x != loc.x || porigin_.y != loc.y) {
      //      Point2D.Double temp = new Point2D.Double(pbounds_.x, pbounds_.y);
      porigin_.x = loc.x;
      porigin_.y = loc.y;
      modified("ColorKey: setLocationP()");
      //      changes_.firePropertyChange("location", temp, loc);
    }
  }

  /**
   * Set the size of the key in physical coordinates.
   *
   * @param d size of key
   */
  public void setSizeP(Dimension2D d) {
    if (psize_.width != d.width || psize_.height != d.height) {
      psize_.width = d.width;
      psize_.height = d.height;
      modified("ColorKey: setSizeP()");
    }
  }

  /**
   * Set the bounds of the key in physical coordinates.
   *
   * @param r bounding rectangle
   */
  @Override
  public void setBoundsP(Rectangle2D.Double r) {
    if (Debug.EVENT) System.out.println("ColorKey: porigin = " + porigin_ + ", r = " + r);
    setLocationP(new Point2D.Double(r.x, r.y));
    setSizeP(new Dimension2D(r.width, r.height));
    //    if(pbounds_ == null || !pbounds_.equals(r)) {
    //      pbounds_ = r;
    //      modified("ColorKey: setBoundsP()");
    //    }
  }

  /**
   * Get the bounding rectangle for the key in physical coordinates.
   *
   * @return bounding rectangle
   */
  public Rectangle2D.Double getBoundsP() {
    return new Rectangle2D.Double(
        porigin_.x, porigin_.y,
        psize_.width, psize_.height);
  }

  /**
   * Gets the bounding rectangle in device coordinates.
   *
   * @return bounding rectangle
   */
  @Override
  public Rectangle getBounds() {
    int x, y, height, width;
    x = layer_.getXPtoD(porigin_.x);
    y = layer_.getYPtoD(porigin_.y);
    width = layer_.getXPtoD(porigin_.x + psize_.width) - x;
    height = layer_.getYPtoD(porigin_.y - psize_.height) - y;
    switch (halign_) {
      case RIGHT:
        x = x - width;
        break;
      case CENTER:
        x = x - width / 2;
    }
    switch (valign_) {
      case BOTTOM:
        y = y - height;
        break;
      case MIDDLE:
        y = y - height / 2;
    }
    if (false) {
      StringBuffer sbuf = new StringBuffer("\nColorKey.getBounds(): ");
      switch (halign_) {
        case RIGHT:
          sbuf.append("RIGHT");
          break;
        case LEFT:
          sbuf.append("LEFT");
          break;
        case CENTER:
          sbuf.append("CENTER");
          break;
      }
      sbuf.append(", ");
      switch (valign_) {
        case TOP:
          sbuf.append("TOP");
          break;
        case BOTTOM:
          sbuf.append("BOTTOM");
          break;
        case MIDDLE:
          sbuf.append("MIDDLE");
          break;
      }
      System.out.println(sbuf);
      System.out.println("    orig = " + porigin_ + ", " + "size = " + psize_);
      System.out.println("    x,y,width,height = " + x + ", " + y + ", " + width + ", " + height);
      System.out.println("   layer.getBounds() = " + layer_.getBounds());
      System.out.println("   layer.getBoundsP() = " + layer_.getBoundsP());
    }
    return new Rectangle(x, y, width, height);
  }

  /**
   * Change the selected objects bounding rectangle in device coordinates. The object will move to
   * the new bounding rectangle.
   *
   * @param r new bounding rectangle
   */
  public void setBounds(Rectangle r) {
    setBounds(r.x, r.y, r.width, r.height);
  }

  /**
   * Change the selected objects bounding rectangle in device coordinates. The object will move to
   * the new bounding rectangle.
   *
   * @param x horizontal location, positive right
   * @param y vertical location, positive down
   * @param width horizontal size
   * @param height vertical size
   */
  public void setBounds(int x, int y, int width, int height) {
    switch (halign_) {
      case RIGHT:
        x = x + width;
        break;
      case CENTER:
        x = x + width / 2;
    }
    switch (valign_) {
      case BOTTOM:
        y = y + height;
        break;
      case MIDDLE:
        y = y + height / 2;
    }
    //    Point orig = new Point(layer_.getXPtoD(pbounds_.x),
    //                           layer_.getYPtoD(pbounds_.y));
    double xp = layer_.getXDtoP(x);
    double yp = layer_.getYDtoP(y);
    if (porigin_.x != xp || porigin_.y != yp) {
      porigin_.x = xp;
      porigin_.y = yp;
      modified("ColorKey: setBounds()");
      //      changes_.firePropertyChange("location",
      //                                  orig, new Point(x,y));
    }
  }

  /**
   * Set the title of the key.
   *
   * @param title key title
   */
  public void setTitle(SGLabel title) {
    if (title_ == null || !title_.equals(title)) {
      title_ = title;
      modified("ColorKey: setTitle()");
    }
  }

  /**
   * Get the key's title.
   *
   * @return the title
   */
  public SGLabel getTitle() {
    return title_;
  }

  /**
   * Get the <code>Ruler</code> associated with the key.
   *
   * @return the ruler
   */
  public Ruler getRuler() {
    return scale_;
  }

  /**
   * Draw the ColorKey. This method should not be directly called.
   *
   * @see Pane#draw
   */
  @Override
  public void draw(Graphics g) {
    Rectangle bounds;
    double xp, yp; // lower-left coordinates in p-space
    double ptop, pbottom, pleft, pright; // insets in p-space
    double delta;
    if (!visible_) return;
    ptop = insets_[0];
    pbottom = insets_[2];
    pleft = insets_[1];
    pright = insets_[3];
    xp = porigin_.x;
    yp = porigin_.y;
    switch (halign_) {
      case RIGHT:
        xp = xp - psize_.width;
        break;
      case CENTER:
        xp = xp - psize_.width / 2;
    }
    switch (valign_) {
      case TOP:
        yp = yp - psize_.height;
        break;
      case MIDDLE:
        yp = yp - psize_.height / 2;
    }
    bounds = getBounds();
    g.setColor(Color.black);
    switch (style_) {
      case PLAIN_LINE:
        g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        break;
      case RAISED:
        break;
      default:
      case NO_BORDER:
    }

    if (cm_ == null) return;
    Range2D uRange = cm_.getRange();
    //
    if (Double.isNaN(uRange.delta)) {
      Range2D rnge = Graph.computeRange(uRange.start, uRange.end, 10);
      delta = rnge.delta;
    } else {
      delta = uRange.delta;
    }
    if (orient_ == HORIZONTAL) {
      drawBar(
          g,
          xp + pleft,
          yp + psize_.height - ptop,
          psize_.width - pleft - pright,
          psize_.height - ptop - pbottom);
      // scale_ = new Ruler(); // mod Tom Ewing
      scale_.setOrientation(Ruler.HORIZONTAL);
      scale_.setLayer(layer_);
      scale_.setTitle(title_);
      scale_.setTicPosition(Ruler.NEGATIVE_SIDE);
      scale_.setLabelPosition(Ruler.NEGATIVE_SIDE);
      scale_.setBoundsP(
          new Rectangle2D.Double(
              xp + pleft,
              yp + psize_.height - ptop - barWidth_,
              psize_.width - pleft - pright,
              psize_.height - ptop - pbottom));
      scale_.setRangeU(new Range2D(uRange.start, uRange.end, delta));
      scale_.draw(g);
    } else {
      drawBar(
          g,
          xp + pleft,
          yp + pbottom,
          psize_.width - pleft - pright,
          psize_.height - ptop - pbottom);
      // scale_ = new Ruler(); // mod dwd
      scale_.setOrientation(Ruler.VERTICAL);
      scale_.setLayer(layer_);
      scale_.setTitle(title_);
      scale_.setTicPosition(Ruler.POSITIVE_SIDE);
      scale_.setLabelPosition(Ruler.POSITIVE_SIDE);
      scale_.setBoundsP(
          new Rectangle2D.Double(
              xp + pleft + barWidth_,
              yp + pbottom,
              psize_.width - pleft - pright,
              psize_.height - ptop - pbottom));
      scale_.setRangeU(new Range2D(uRange.start, uRange.end, delta));
      scale_.draw(g);
    }
  }

  //
  void drawBar(Graphics g, double plocx, double plocy, double pwidth, double pheight) {
    int yloc, xloc, xend, yend;
    int dBarHeight, dBarWidth;
    Range2D uRange = cm_.getRange();
    AxisTransform sTrans;
    if (orient_ == HORIZONTAL) {
      dBarHeight = layer_.getYPtoD(plocy - barWidth_) - layer_.getYPtoD(plocy);
      sTrans = new LinearTransform(plocx, plocx + pwidth, uRange.start, uRange.end);
      yloc = layer_.getYPtoD(plocy);
      xloc = layer_.getXPtoD(sTrans.getTransP(uRange.start));
      xend = layer_.getXPtoD(sTrans.getTransP(uRange.end));
      for (int i = xloc; i <= xend; i++) {
        g.setColor(cm_.getColor(sTrans.getTransU(layer_.getXDtoP(i))));
        g.fillRect(i, yloc, 1, dBarHeight);
      }
    } else {
      dBarWidth = layer_.getXPtoD(plocx + barWidth_) - layer_.getXPtoD(plocx);
      sTrans = new LinearTransform(plocy, plocy + pheight, uRange.start, uRange.end);
      xloc = layer_.getXPtoD(plocx);
      yloc = layer_.getYPtoD(sTrans.getTransP(uRange.start));
      yend = layer_.getYPtoD(sTrans.getTransP(uRange.end));
      for (int i = yend; i <= yloc; i++) {
        System.out.println(
            "ColorKey.drawBar i="
                + i
                + " P="
                + layer_.getXDtoP(i)
                + " trans="
                + sTrans.getTransU(layer_.getXDtoP(i))
                + " color="
                + Integer.toHexString(cm_.getColor(sTrans.getTransU(layer_.getXDtoP(i))).getRGB()));
        g.setColor(cm_.getColor(sTrans.getTransU(layer_.getXDtoP(i))));
        g.fillRect(xloc, i, dBarWidth, 1);
      }
    }
  }

  /**
   * Get a string representation of the key.
   *
   * @return string representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + ident_;
  }

  /**
   * Check if ColorKey is visible.
   *
   * @since 2.0
   */
  @Override
  public boolean isVisible() {
    return visible_;
  }

  /**
   * Set visibility state for ColorKey.
   *
   * @since 2.0
   */
  @Override
  public void setVisible(boolean visible) {
    if (visible_ != visible) {
      visible_ = visible;
      modified("ColorKey: setVisible()");
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    modified(
        "ColorKey: propertyChange("
            + evt.getSource().toString()
            + "["
            + evt.getPropertyName()
            + "]"
            + ")");
  }

  /**
   * Set columns. Unimplmented.
   *
   * @param col
   * @since 3.0
   */
  @Override
  public void setColumns(int col) {}

  /**
   * Set line lenght. Unimplemented.
   *
   * @since 3.0
   */
  @Override
  public void setLineLengthP(double len) {}
}
