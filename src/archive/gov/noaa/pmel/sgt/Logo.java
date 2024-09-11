/*
 * $Id: Logo.java,v 1.15 2001/12/13 19:07:04 dwd Exp $
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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;

// jdk1.2
// import java.awt.geom.Point2D;

/**
 * Logo displays an Image on its parent Layer. Logo implements the LayerChild interface.
 *
 * @author Donald Denbo
 * @version $Revision: 1.15 $, $Date: 2001/12/13 19:07:04 $
 * @since 1.0
 * @see LayerChild
 */
public class Logo implements Cloneable, LayerChild, Moveable {
  private String ident_;

  /**
   * @directed
   */
  private Layer layer_ = null;

  private Point2D.Double porigin_;
  private int valign_;
  private int halign_;
  private Image image_ = null;
  private URL imageURL_;
  private boolean selected_;
  private boolean selectable_;
  private boolean visible_;
  private boolean moveable_;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  /** Align to top of Logo. */
  public static final int TOP = 0;

  /** Align to middle of Logo. */
  public static final int MIDDLE = 1;

  /** Align to bottom of Logo. */
  public static final int BOTTOM = 2;

  /** Align to left of Logo. */
  public static final int LEFT = 0;

  /** Align to center of Logo. */
  public static final int CENTER = 1;

  /** Align to right of Logo. */
  public static final int RIGHT = 2;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      layer_ = null;
      image_ = null;
      changes_ = null;
      if (JPane.debug) String2.log("sgt.VectorKey.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /**
   * Default constructor. The default location for the Logo is at (0.0, 0.0) and aligned BOTTOM and
   * LEFT.
   */
  public Logo() {
    this(new Point2D.Double(0.0, 0.0), BOTTOM, LEFT);
  }

  /**
   * Create a Logo object. The initial object will not have an associated Image until setImage or
   * setImageURL methods have been invoked. Vertical alignments include TOP, MIDDLE, and BOTTOM.
   * Horizontal alignments include LEFT, CENTER, and RIGHT.
   *
   * @param loc Location of Logo
   * @param valign vertical alignment
   * @param halign horizontal alignment
   * @see #setImageURL
   * @see #setImage
   * @see java.awt.Image
   */
  public Logo(Point2D.Double loc, int valign, int halign) {
    porigin_ = loc;
    valign_ = valign;
    halign_ = halign;
    ident_ = "";
    selected_ = false;
    selectable_ = true;
    visible_ = true;
    moveable_ = true;
  }

  /** Create of copy of Logo. */
  @Override
  public LayerChild copy() {
    Logo newLogo;
    try {
      newLogo = (Logo) clone();
    } catch (CloneNotSupportedException e) {
      newLogo = new Logo();
    }
    return newLogo;
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

  @Override
  public boolean isMoveable() {
    return moveable_;
  }

  @Override
  public void setMoveable(boolean moveable) {
    moveable_ = moveable;
  }

  /**
   * Set the URL for the Logo image. The URL or the image must be set.
   *
   * @param url image URL
   * @see #setImage
   */
  public void setImageURL(URL url) {
    if (imageURL_ == null || !imageURL_.equals(url)) {
      imageURL_ = url;
      modified("Logo: setImageURL()");
    }
  }

  /**
   * Get the image URL. The URL will be null if no imageURL was specified.
   *
   * @return the imageURL
   */
  public URL getImageURL() {
    return imageURL_;
  }

  /**
   * Set the Logo image. The image or the imageURL must be set.
   *
   * @param img Logo image
   * @see #setImageURL
   */
  public void setImage(Image img) {
    if (image_ == null || !image_.equals(img)) {
      image_ = img;
      modified("Logo: setImage()");
    }
  }

  /**
   * Set parent layer.
   *
   * @param l parent layer
   */
  @Override
  public void setLayer(Layer l) {
    layer_ = l;
  }

  /**
   * Get layer.
   *
   * @return layer
   */
  @Override
  public Layer getLayer() {
    return layer_;
  }

  @Override
  public AbstractPane getPane() {
    return layer_.getPane();
  }

  @Override
  public void modified(String mess) {
    //    if(Debug.EVENT) System.out.println("Logo: modified()");
    if (layer_ != null) layer_.modified(mess);
  }

  /**
   * Set Logo identifier.
   *
   * @param id logo identifier
   */
  @Override
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Get Logo identifier
   *
   * @return identifier
   */
  @Override
  public String getId() {
    return ident_;
  }

  /**
   * Set alignment.
   *
   * @param vert vertical alignment
   * @param horz horizontal alignment
   */
  public void setAlign(int vert, int horz) {
    if (valign_ != vert || halign_ != horz) {
      valign_ = vert;
      halign_ = horz;
      modified("Logo: setAlign()");
    }
  }

  /**
   * Set vertical alignment
   *
   * @param vert vertical alignment
   */
  public void setVAlign(int vert) {
    if (valign_ != vert) {
      valign_ = vert;
      modified("Logo: setVAlign()");
    }
  }

  /**
   * Set horizontal alignment
   *
   * @param horz horizontal alignment
   */
  public void setHAlign(int horz) {
    if (halign_ != horz) {
      halign_ = horz;
      modified("Logo: setHAlign()");
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
   * Set location of logo <br>
   * <strong>Property Change:</strong> <code>location</code>.
   *
   * @param loc logo location
   */
  public void setLocationP(Point2D.Double loc) {
    if (porigin_ == null || !porigin_.equals(loc)) {
      Point2D.Double temp = porigin_;
      porigin_ = loc;
      changes_.firePropertyChange("location", temp, porigin_);
      modified("Logo: setLocationP()");
    }
  }

  /**
   * Get location of logo.
   *
   * @return Logo location
   */
  public Point2D.Double getLocationP() {
    return porigin_;
  }

  /**
   * Draw the Logo. If the imageURL was specified the image is retrieved using the URL and
   * MediaTracker. The image is displayed after it is loaded.
   */
  @Override
  public void draw(Graphics g) {
    Rectangle bnds;
    if (!visible_) return;
    if (imageURL_ != null && layer_ != null && image_ == null) {
      image_ = layer_.getPane().getComponent().getToolkit().getImage(imageURL_);
      if (image_ != null) {
        MediaTracker mt = new MediaTracker(layer_.getPane().getComponent());
        try {
          mt.addImage(image_, 0);
          mt.waitForAll();
          if (mt.isErrorAny()) System.err.println("Logo: Error loading image");
        } catch (InterruptedException ie) {
        }
        System.out.println("MediaTracker: " + mt.checkAll());
      }
    }
    layer_.getPane().getComponent().invalidate();
    if (image_ != null) {
      bnds = getBounds();
      g.drawImage(image_, bnds.x, bnds.y, layer_.getPane().getComponent());
    }
  }

  /**
   * Get the bounding rectangle.
   *
   * @return bounding rectangle
   */
  @Override
  public Rectangle getBounds() {
    Rectangle bounds;
    int x, y;
    int width, height;
    if (image_ != null) {
      width = image_.getWidth(layer_.getPane().getComponent());
      height = image_.getHeight(layer_.getPane().getComponent());
      x = layer_.getXPtoD(porigin_.x);
      y = layer_.getYPtoD(porigin_.y);
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
      return new Rectangle(x, y, width, height);
    }
    return null;
  }

  @Override
  public Point getLocation() {
    Rectangle bnds = getBounds();
    return new Point(bnds.x, bnds.y);
  }

  @Override
  public void setLocation(Point loc) {
    Rectangle bnds = getBounds();
    setBounds(loc.x, loc.y, bnds.width, bnds.height);
  }

  /** Set the bounds of the <code>Logo</code> */
  public void setBounds(Rectangle r) {
    setBounds(r.x, r.y, r.width, r.height);
  }

  /**
   * Set the bounds of the <code>Logo</code> <br>
   * <strong>Property Change:</strong> <code>location</code>.
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
    double xp = layer_.getXDtoP(x);
    double yp = layer_.getYDtoP(y);
    if (porigin_.x != xp || porigin_.y != yp) {
      Point2D.Double temp = porigin_;
      porigin_.x = xp;
      porigin_.y = yp;
      changes_.firePropertyChange("location", temp, new Point2D.Double(xp, yp));
      modified("Logo: setBounds()");
    }
  }

  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + ident_;
  }

  @Override
  public boolean isVisible() {
    return visible_;
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible_ != visible) {
      visible_ = visible;
      modified("Logo: setVisible()");
    }
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
