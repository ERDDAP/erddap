/*
 * $Id: LabelDrawer2.java,v 1.6 2003/08/22 23:02:32 dwd Exp $
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

import gov.noaa.pmel.swing.MRJUtil;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.beans.*;
import java.text.AttributedString;

/**
 * Implements label drawing using Java2D functionality.
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.0
 */
public class LabelDrawer2 implements LabelDrawer, Cloneable {
  private String label_;
  private Color clr_;
  private Font font_;
  private transient Layer layer_;
  private int orient_;
  private int halign_;
  private int valign_;
  private Point dorigin_;
  private Rectangle dbounds_;
  private Point2D.Double porigin_;
  private Rectangle2D.Double pbounds_;
  private Polygon dpolygon_;
  private double angle_;
  private double sinthta_;
  private double costhta_;
  private double height_;
  private boolean visible_;
  private static boolean fixMetrics_ = MRJUtil.fixFontMetrics();

  private int xoff_ = 0;
  private int yoff_ = 0;

  private boolean alwaysAntiAlias_ = true;

  private Rectangle savedBounds_ = null;
  private Point savedLoc_ = null;

  public LabelDrawer2(String lbl, double hgt, Point2D.Double loc, int valign, int halign) {
    label_ = lbl;
    height_ = hgt;
    porigin_ = loc;
    valign_ = valign;
    halign_ = halign;
    //
    dbounds_ = new Rectangle();
    dorigin_ = new Point(0, 0);
    pbounds_ = new Rectangle2D.Double();
  }

  public LabelDrawer copy() {
    LabelDrawer1 newLabel = null;
    //      try {
    //        newLabel = (LabelDrawer1)clone();
    //      } catch (CloneNotSupportedException e) {
    //        newLabel = new LabelDrawer1(ident_, label_, height_,
    //  			     porigin_, valign_, halign_);
    //        newLabel.setColor(clr_);
    //        newLabel.setFont(font_);
    //        if(orient_ == ANGLE) {
    //  	newLabel.setAngle(angle_);
    //        } else {
    //  	newLabel.setOrientation(orient_);
    //        }
    //      }
    return newLabel;
  }

  @Override
  public void draw(Graphics g) throws LayerNotFoundException {
    int xs, ys;
    if ((label_.length() <= 0) || !visible_ || g == null) return;
    if (layer_ == (Layer) null) throw new LayerNotFoundException();
    //
    // set label heigth in physical units
    //
    computeBoundsD(g);
    if (clr_ == null) {
      g.setColor(layer_.getPane().getComponent().getForeground());
    } else {
      g.setColor(clr_);
    }
    if (orient_ == SGLabel.HORIZONTAL) {
      xs = dbounds_.x + xoff_;
      ys = dbounds_.y + yoff_;
      drawString(g, xs, ys);
    } else if (orient_ == SGLabel.VERTICAL) {
      //
      // draw text in offscreen image (horizontal)
      //
      xs = dbounds_.x + xoff_;
      ys = dbounds_.y + yoff_;
      drawString(g, xs, ys);
    } else {
      //
      // Angled text, draw in offscreen image and rotate
      //
      xs = layer_.getXPtoD(porigin_.x);
      ys = layer_.getYPtoD(porigin_.y);
      drawString(g, xs, ys);
    }
  }

  @Override
  public void setText(String lbl) {
    label_ = lbl;
  }

  @Override
  public String getText() {
    return label_;
  }

  @Override
  public void setColor(Color clr) {
    clr_ = clr;
  }

  @Override
  public Color getColor() {
    return clr_;
  }

  @Override
  public void setFont(Font font) {
    font_ = font;
  }

  @Override
  public Font getFont() {
    return font_;
  }

  @Override
  public void setLayer(Layer layer) {
    layer_ = layer;
    if (savedBounds_ != null) {
      setBounds(
          savedBounds_.x, savedBounds_.y,
          savedBounds_.width, savedBounds_.height);
      savedBounds_ = null;
    }
    if (savedLoc_ != null) {
      setLocation(savedLoc_);
      savedLoc_ = null;
    }
  }

  @Override
  public Layer getLayer() {
    return layer_;
  }

  @Override
  public void setOrientation(int orient) {
    if (orient_ != orient) {
      if (orient == SGLabel.HORIZONTAL) {
        costhta_ = 1.0;
        sinthta_ = 0.0;
      } else if (orient == SGLabel.VERTICAL) {
        costhta_ = 0.0;
        sinthta_ = 1.0;
      }
      orient_ = orient;
    }
  }

  @Override
  public int getOrientation() {
    return orient_;
  }

  @Override
  public void setHAlign(int halign) {
    halign_ = halign;
  }

  @Override
  public int getHAlign() {
    return halign_;
  }

  @Override
  public void setVAlign(int valign) {
    valign_ = valign;
  }

  @Override
  public int getVAlign() {
    return valign_;
  }

  @Override
  public void setLocation(Point loc) {
    if (layer_ == null) {
      savedLoc_ = new Point(loc);
      return;
    }
    computeBoundsD(layer_.getPane().getComponent().getGraphics());
    if (dbounds_.x != loc.x || dbounds_.y != loc.y) {
      setBounds(loc.x, loc.y, dbounds_.width, dbounds_.height);
    }
  }

  @Override
  public Point getLocation() {
    if (savedLoc_ != null) return savedLoc_;
    return dorigin_;
  }

  @Override
  public void setBounds(int x, int y, int width, int height) {
    int swidth, sascent, xd, yd;
    if (layer_ == null) {
      savedBounds_ = new Rectangle(x, y, width, height);
      return;
    }
    Graphics g = layer_.getPane().getComponent().getGraphics();
    if (g == null) return;
    font_ = computeFontSize(g);
    FontRenderContext frc = getFontRenderContext((Graphics2D) g);
    TextLayout tlayout;
    if (label_.length() == 0) {
      tlayout = new TextLayout(" ", font_, frc);
    } else {
      tlayout = new TextLayout(label_, font_, frc);
    }
    java.awt.geom.Rectangle2D tbounds = tlayout.getBounds();
    int theight = (int) tbounds.getHeight();
    int twidth = (int) tbounds.getWidth();
    int tx = (int) tbounds.getX();
    int ty = (int) tbounds.getY();
    if (fixMetrics_)
      ty -=
          (int) (0.7 * tlayout.getAscent()); // hack for MacOS X java.runtime.version = 1.4.1_01-39
    sascent = (int) tlayout.getAscent();

    if (orient_ == SGLabel.HORIZONTAL) {
      swidth = width;
      xd = x;
      yd = y - ty;
      switch (valign_) {
        case SGLabel.TOP:
          yd = yd - sascent;
          break;
        case SGLabel.MIDDLE:
          yd = yd + (ty + theight / 2);
          break;
        case SGLabel.BOTTOM:
      }
      switch (halign_) {
        case SGLabel.RIGHT:
          xd = xd + swidth;
          break;
        case SGLabel.CENTER:
          xd = xd + swidth / 2;
          break;
        case SGLabel.LEFT:
      }
    } else {
      swidth = height;
      yd = y + height;
      xd = x - ty;
      switch (valign_) {
        case SGLabel.TOP:
          xd = xd - sascent;
          break;
        case SGLabel.MIDDLE:
          xd = xd - sascent / 2;
          break;
        case SGLabel.BOTTOM:
      }
      switch (halign_) {
        case SGLabel.RIGHT:
          yd = yd - swidth;
          break;
        case SGLabel.CENTER:
          yd = yd - swidth / 2;
          break;
        case SGLabel.LEFT:
      }
    }
    if (dorigin_.x != xd || dorigin_.y != yd) {
      dorigin_.x = xd;
      dorigin_.y = yd;
      porigin_.x = layer_.getXDtoP(xd);
      porigin_.y = layer_.getYDtoP(yd);
    }
  }

  @Override
  public Rectangle getBounds() {
    if (savedBounds_ != null) return savedBounds_;
    if (layer_ != null) computeBoundsD(layer_.getPane().getComponent().getGraphics());
    return dbounds_;
  }

  @Override
  public void setLocationP(Point2D.Double loc) {
    porigin_ = loc;
  }

  @Override
  public Point2D.Double getLocationP() {
    return porigin_;
  }

  @Override
  public Rectangle2D.Double getBoundsP() {
    computeBoundsD(layer_.getPane().getComponent().getGraphics());
    return pbounds_;
  }

  @Override
  public void setAngle(double angle) {
    angle_ = angle;
    double thta = angle_ * Math.PI / 180.0;
    if (Math.abs(thta) < 0.001) {
      orient_ = SGLabel.HORIZONTAL;
      costhta_ = 1.0;
      sinthta_ = 0.0;
    } else if (Math.abs(thta - 90.0) < 0.001) {
      orient_ = SGLabel.VERTICAL;
      costhta_ = 0.0;
      sinthta_ = 1.0;
    } else {
      orient_ = SGLabel.ANGLE;
      costhta_ = Math.cos(thta);
      sinthta_ = Math.sin(thta);
    }
  }

  @Override
  public double getAngle() {
    return angle_;
  }

  @Override
  public void setHeightP(double hgt) {
    height_ = hgt;
  }

  @Override
  public double getHeightP() {
    return height_;
  }

  @Override
  public void setVisible(boolean vis) {
    visible_ = vis;
  }

  @Override
  public boolean isVisible() {
    return visible_;
  }

  private void computeBoundsD(Graphics g) {
    int sascent, xd, yd;
    int xorig, yorig;
    int[] xt = new int[4];
    int[] yt = new int[4];
    int[] xn = new int[4];
    int[] yn = new int[4];
    //
    // compute size of font and adjust to be height tall!
    //
    if (g == null) return;
    font_ = computeFontSize(g);
    FontRenderContext frc = getFontRenderContext((Graphics2D) g);
    TextLayout tlayout;
    if (label_.length() == 0) {
      tlayout = new TextLayout(" ", font_, frc);
    } else {
      tlayout = new TextLayout(label_, font_, frc);
    }
    java.awt.geom.Rectangle2D tbounds = tlayout.getBounds();

    //    System.out.println("LabelDrawer2('" + label_ + "'): fixMetrics = " + fixMetrics_);
    //    System.out.println("LabelDrawer2('" + label_ + "'): TextLayout.bounds = " + tbounds);
    //    System.out.println("LabelDrawer2('" + label_ + "'): ascent, descent = " +
    // tlayout.getAscent() + ", " + tlayout.getDescent());
    //    System.out.println("LabelDrawer2('" + label_ + "'): leading, baseline = " +
    // tlayout.getLeading() + ", " + tlayout.getBaseline());
    //    System.out.println("LabelDrawer2('" + label_ + "'): TextLayout = " + tlayout);

    int theight = (int) tbounds.getHeight();
    int twidth = (int) tbounds.getWidth();
    int tx = (int) tbounds.getX();
    int ty = (int) tbounds.getY();
    if (fixMetrics_)
      ty -=
          (int) (0.7 * tlayout.getAscent()); // hack for MacOS X java.runtime.version = 1.4.1_01-39
    sascent = (int) tlayout.getAscent();
    //
    xd = layer_.getXPtoD(porigin_.x);
    yd = layer_.getYPtoD(porigin_.y);
    //
    // set device origin
    //
    dorigin_.x = xd;
    dorigin_.y = yd;
    xorig = xd;
    yorig = yd;
    //
    switch (valign_) {
      case SGLabel.TOP:
        yd = yd + sascent;
        break;
      case SGLabel.MIDDLE:
        yd = yd - (ty + theight / 2);
        break;
      case SGLabel.BOTTOM:
    }
    switch (halign_) {
      case SGLabel.RIGHT:
        xd = xd - twidth;
        break;
      case SGLabel.CENTER:
        xd = xd - twidth / 2;
        break;
      case SGLabel.LEFT:
    }
    if (orient_ == SGLabel.HORIZONTAL) {
      xoff_ = 0;
      yoff_ = -ty;
    } else if (orient_ == SGLabel.VERTICAL) {
      xoff_ = -ty;
      yoff_ = twidth;
    }
    xt[0] = xd + tx;
    xt[1] = xt[0];
    xt[2] = xt[0] + twidth;
    xt[3] = xt[2];

    yt[0] = yd + ty;
    yt[1] = yt[0] + theight;
    yt[2] = yt[1];
    yt[3] = yt[0];
    //
    // rotate
    //
    for (int i = 0; i < 4; i++) {
      xn[i] = (int) ((xt[i] - xorig) * costhta_ + (yt[i] - yorig) * sinthta_) + xorig;
      yn[i] = (int) ((yt[i] - yorig) * costhta_ - (xt[i] - xorig) * sinthta_) + yorig;
    }

    dpolygon_ = new Polygon(xn, yn, 4);
    dbounds_ = dpolygon_.getBounds();
    //
    // compute pbounds
    //
    pbounds_.x = layer_.getXDtoP(dbounds_.x);
    pbounds_.y = layer_.getYDtoP(dbounds_.y);
    pbounds_.width = layer_.getXDtoP(dbounds_.x + dbounds_.width) - pbounds_.x;
    pbounds_.height = pbounds_.y - layer_.getYDtoP(dbounds_.y + dbounds_.height);
  }

  //
  // a bad method to compute the font size!
  //
  Font computeFontSize(Graphics g) {
    Font tfont;
    int pt_0, pt_1, hgt;
    int count = 1;
    double hgt_0, hgt_1, del_0, del_1;
    double a, b;
    FontRenderContext frc = getFontRenderContext((Graphics2D) g);
    TextLayout tlayout;
    //
    // first guess
    //
    if (g == null) return font_; // return original font!
    hgt = layer_.getXPtoD(height_) - layer_.getXPtoD(0.0f);
    pt_0 = hgt - 3;
    tfont = new Font(font_.getName(), font_.getStyle(), pt_0);
    if (label_.length() == 0) {
      tlayout = new TextLayout(" ", tfont, frc);
    } else {
      tlayout = new TextLayout(label_, tfont, frc);
    }
    hgt = (int) (tlayout.getAscent() + tlayout.getDescent());
    hgt_0 = layer_.getXDtoP(hgt) - layer_.getXDtoP(0);
    pt_0 = tfont.getSize();
    pt_1 = (int) ((double) pt_0 * (height_ / hgt_0));
    while ((pt_0 != pt_1) && (count < 5)) {
      tfont = new Font(font_.getName(), font_.getStyle(), pt_1);
      if (label_.length() == 0) {
        tlayout = new TextLayout(" ", tfont, frc);
      } else {
        tlayout = new TextLayout(label_, tfont, frc);
      }
      hgt = (int) (tlayout.getAscent() + tlayout.getDescent());
      hgt_1 = layer_.getXDtoP(hgt) - layer_.getXDtoP(0);
      del_0 = Math.abs(height_ - hgt_0);
      del_1 = Math.abs(height_ - hgt_1);
      if ((Math.abs(pt_0 - pt_1) <= 1) && (del_0 > del_1)) return tfont;
      pt_0 = pt_1;
      hgt_0 = hgt_1;
      pt_1 = (int) ((double) pt_0 * (height_ / hgt_0));
      count++;
    }
    return tfont;
  }

  private void drawString(Graphics g, int x, int y) {
    float angle;
    if (g == null) return;
    if (orient_ == SGLabel.HORIZONTAL) {
      angle = 0.0f;
    } else if (orient_ == SGLabel.VERTICAL) {
      angle = -90.0f;
    } else {
      angle = -(float) angle_;
    }
    // Object originalAntiAliasingHint = null;
    Graphics2D g2 = (Graphics2D) g;
    // if(angle != 0.0f || alwaysAntiAlias_) {
    // originalAntiAliasingHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    // 2019-02-08 This no longer sets RenderingHints.VALUE_ANTIALIAS_ON.
    //  It uses current RenderingHints from the Graphics object.
    // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    //                    RenderingHints.VALUE_ANTIALIAS_ON);
    // }
    AffineTransform oldTransform = g2.getTransform();
    //    System.out.println("\n angle = " + angle + ", text = " + label_);
    //    System.out.println("oldTransform = " + oldTransform);
    AttributedString as = new AttributedString(label_);
    // System.out.println("2!! setting renderingHints x=" + x + " y=" + y);
    as.addAttribute(TextAttribute.FONT, font_);
    g2.translate(x, y);
    //   System.out.println("translated = " + g2.getTransform());
    if (angle != 0.0f) g2.rotate(Math.PI * angle / 180.0);
    //   System.out.println("newTransform = " + g2.getTransform());
    g2.drawString(as.getIterator(), 0, 0);
    g2.setTransform(oldTransform);
    // if(originalAntiAliasingHint != null) {
    //  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntiAliasingHint);
    // }
    //
    //      g2.setStroke(normal);
    //      Color oldColor = g.getColor();
    //      g.setColor(Color.red);
    //      int x1= x - 5;
    //      int x2= x + 5;
    //      int y1= y - 5;
    //      int y2= y + 5;
    //      g.drawLine(x1, y, x2, y);
    //      g.drawLine(x, y1, x, y2);
    //      g.setColor(oldColor);

  }

  @Override
  public float getStringWidth(Graphics g) {
    if (g == null) return 0.0f;
    font_ = computeFontSize(g);
    FontRenderContext frc = getFontRenderContext((Graphics2D) g);
    TextLayout tlayout;
    if (label_.length() == 0) {
      tlayout = new TextLayout(" ", font_, frc);
    } else {
      tlayout = new TextLayout(label_, font_, frc);
    }
    java.awt.geom.Rectangle2D tbounds = tlayout.getBounds();
    return (float) tbounds.getWidth();
  }

  @Override
  public float getStringHeight(Graphics g) {
    if (g == null) return 0.0f;
    font_ = computeFontSize(g);
    FontRenderContext frc = getFontRenderContext((Graphics2D) g);
    TextLayout tlayout;
    if (label_.length() == 0) {
      tlayout = new TextLayout(" ", font_, frc);
    } else {
      tlayout = new TextLayout(label_, font_, frc);
    }
    java.awt.geom.Rectangle2D tbounds = tlayout.getBounds();
    return (float) tbounds.getHeight();
  }

  FontRenderContext getFontRenderContext(Graphics2D g2) {
    if (g2 == null) return null;
    // Object originalAntiAliasingHint = null;
    // if(angle_ != 0.0 || alwaysAntiAlias_) {
    // 2019-02-08 This no longer sets RenderingHints.VALUE_ANTIALIAS_ON.
    //  It uses current RenderingHints from the Graphics object.
    // originalAntiAliasingHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    //  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    //                      RenderingHints.VALUE_ANTIALIAS_ON);
    // }
    FontRenderContext frc = g2.getFontRenderContext();
    // if(originalAntiAliasingHint != null) {
    //  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntiAliasingHint);
    // }
    return frc;
  }
}
