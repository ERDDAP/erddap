/*
 * $Id: LabelDrawer1.java,v 1.4 2003/08/22 23:02:32 dwd Exp $
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

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

/**
 * Implements label drawing for JDK1.1
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.0
 */
public class LabelDrawer1 implements LabelDrawer, Cloneable {
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
  private Rectangle savedBounds_ = null;
  private Point savedLoc_ = null;

  public LabelDrawer1(String lbl, double hgt, Point2D.Double loc, int valign, int halign) {
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
    FontMetrics fmet;
    int xs, ys;
    if ((label_.length() <= 0) || !visible_ || g == null) return;
    if (layer_ == (Layer) null) throw new LayerNotFoundException();
    //
    // set label heigth in physical units
    //
    computeBoundsD(g);
    fmet = g.getFontMetrics();
    if (clr_ == null) {
      g.setColor(layer_.getPane().getComponent().getForeground());
    } else {
      g.setColor(clr_);
    }
    if (orient_ == SGLabel.HORIZONTAL) {
      xs = dbounds_.x;
      ys = dbounds_.y + fmet.getMaxAscent();
      g.drawString(label_, xs, ys);
    } else if (orient_ == SGLabel.VERTICAL) {
      //
      // draw text in offscreen image (horizontal)
      //
      int i, j;
      // make sure bounds are >= 1 pixel!  RWS
      if (dbounds_.height < 1) dbounds_.height = 1;
      if (dbounds_.width < 1) dbounds_.width = 1;
      //
      Image buf = layer_.getPane().getComponent().createImage(dbounds_.height, dbounds_.width);
      Graphics gbuf = buf.getGraphics();

      // bob simons added 2010-12-15   but irrelevant since LabelDrawer2 is used
      // 2019-02-08 This no longer sets RenderingHints.VALUE_ANTIALIAS_ON.
      //  It uses current RenderingHints from the Graphics object.
      // try {
      //    //if possible, set RenderingHints
      //    //System.out.println("1! setting renderingHints");
      //    Graphics2D g2 = (Graphics2D)gbuf;
      //    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      // RenderingHints.VALUE_ANTIALIAS_ON);
      // } catch (Throwable t) {
      //    //System.out.println("1! couldn't set renderingHints:" + t.toString());
      // }

      gbuf.setFont(g.getFont());
      //
      gbuf.setColor(layer_.getPane().getComponent().getBackground());
      gbuf.fillRect(0, 0, dbounds_.height, dbounds_.width);
      if (clr_ == null) {
        gbuf.setColor(layer_.getPane().getComponent().getForeground());
      } else {
        gbuf.setColor(clr_);
      }
      gbuf.drawString(label_, 0, fmet.getMaxAscent());
      //
      // create offscreen image (vertical) and copy pixels
      //
      int[] bufpix = new int[dbounds_.width * dbounds_.height];
      int[] vbufpix = new int[dbounds_.width * dbounds_.height];
      PixelGrabber pg =
          new PixelGrabber(buf, 0, 0, dbounds_.height, dbounds_.width, bufpix, 0, dbounds_.height);
      try {
        pg.grabPixels();
      } catch (InterruptedException e) {
        System.out.println(e);
      }
      for (i = 0; i < dbounds_.height; i++) {
        for (j = 0; j < dbounds_.width; j++) {
          vbufpix[j + (dbounds_.height - i - 1) * dbounds_.width] = bufpix[i + j * dbounds_.height];
        }
      }
      ColorModel cm = ColorModel.getRGBdefault();
      Image vbuf =
          layer_
              .getPane()
              .getComponent()
              .createImage(
                  new MemoryImageSource(
                      dbounds_.width, dbounds_.height, cm, vbufpix, 0, dbounds_.width));
      //
      // copy offscreen area to screen (with rotation)
      //
      g.setPaintMode();
      g.drawImage(vbuf, dbounds_.x, dbounds_.y, layer_.getPane().getComponent());
    } else {
      //
      // Angled text, draw in offscreen image and rotate
      //
      if (dbounds_.height < 1 || dbounds_.width < 1) return;
      int i, j;
      int ii, jj;
      int pixel;
      //
      int[] xpoly = dpolygon_.xpoints;
      int[] ypoly = dpolygon_.ypoints;
      int rectHeight = fmet.getMaxAscent() + fmet.getMaxDescent();
      int rectWidth = fmet.stringWidth(label_);
      int xoff, yoff;
      Image buf = layer_.getPane().getComponent().createImage(rectWidth, rectHeight);
      Graphics gbuf = buf.getGraphics();
      gbuf.setFont(g.getFont());
      //
      gbuf.setColor(layer_.getPane().getComponent().getBackground());
      gbuf.fillRect(0, 0, rectWidth, rectHeight);
      if (clr_ == null) {
        gbuf.setColor(layer_.getPane().getComponent().getForeground());
      } else {
        gbuf.setColor(clr_);
      }
      gbuf.drawString(label_, 0, fmet.getMaxAscent());
      //
      // create offscreen image (vertical) and copy pixels
      //
      int[] bufpix = new int[rectWidth * rectHeight];
      int[] vbufpix = new int[dbounds_.width * dbounds_.height];
      PixelGrabber pg = new PixelGrabber(buf, 0, 0, rectWidth, rectHeight, bufpix, 0, rectWidth);
      try {
        pg.grabPixels();
      } catch (InterruptedException e) {
        System.out.println(e);
      }
      //
      // compute transformation
      //
      xoff = 0;
      yoff = 0;
      for (i = 1; i < 4; i++) {
        xoff = Math.min(xoff, xpoly[i] - xpoly[0]);
        yoff = Math.min(yoff, ypoly[i] - ypoly[0]);
      }
      xoff = -xoff;
      yoff = -yoff;
      //      double[] dx = {0.0, 0.5, -0.5,  0.5, -0.5};
      //      double[] dy = {0.0, 0.5, -0.5, -0.5,  0.5};
      double[] dx = {0.0, 0.5};
      double[] dy = {0.0, 0.5};
      double it, jt;
      for (j = 0; j < rectHeight; j++) {
        for (i = 0; i < rectWidth; i++) {
          for (int k = 0; k < dx.length; k++) {
            it = i + dx[k];
            jt = j + dy[k];
            //	  pixel = bufpix[i + j*rectWidth];
            pixel = bufpix[i + j * rectWidth];
            if (pixel == -1) continue;
            //	  ii = (int)(i*costhta_ + j*sinthta_ + 0.5) + xoff;
            //	  jj = (int)(j*costhta_ - i*sinthta_ + 0.5) + yoff;
            ii = (int) (it * costhta_ + jt * sinthta_ + 0.5) + xoff;
            jj = (int) (jt * costhta_ - it * sinthta_ + 0.5) + yoff;
            if (ii < 0) ii = 0;
            if (ii >= dbounds_.width) ii = dbounds_.width - 1;
            if (jj < 0) jj = 0;
            if (jj >= dbounds_.height) jj = dbounds_.height - 1;
            vbufpix[ii + jj * dbounds_.width] = pixel;
          }
        }
      }
      ColorModel cm = ColorModel.getRGBdefault();
      Image vbuf =
          layer_
              .getPane()
              .getComponent()
              .createImage(
                  new MemoryImageSource(
                      dbounds_.width, dbounds_.height, cm, vbufpix, 0, dbounds_.width));
      //
      // copy offscreen area to screen (with rotation)
      //
      g.setPaintMode();
      g.drawImage(vbuf, dbounds_.x, dbounds_.y, layer_.getPane().getComponent());
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
    //    if(dbounds_.x != loc.x || dbounds_.y != loc.y) {
    //      Point temp = new Point(dbounds_.x, dbounds_.y);
    setBounds(loc.x, loc.y, dbounds_.width, dbounds_.height);
    //        changes_.firePropertyChange("location",
    //  				  temp,
    //  				  loc);
    //    }
  }

  @Override
  public Point getLocation() {
    if (savedLoc_ != null) return savedLoc_;
    return dorigin_;
  }

  @Override
  public void setBounds(int x, int y, int width, int height) {
    int swidth, sascent, xd, yd;
    FontMetrics fmet;
    if (layer_ == null) {
      savedBounds_ = new Rectangle(x, y, width, height);
      return;
    }
    Graphics g = layer_.getPane().getComponent().getGraphics();
    if (g == null) return;
    g.setFont(font_);
    fmet = g.getFontMetrics();
    sascent = fmet.getAscent();
    if (orient_ == SGLabel.HORIZONTAL) {
      swidth = width;
      xd = x;
      //      yd = y + fmet.getMaxAscent();
      yd = y - fmet.getMaxDescent() + height;
      switch (valign_) {
        case SGLabel.TOP:
          yd = yd - sascent;
          break;
        case SGLabel.MIDDLE:
          yd = yd - sascent / 2;
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
      xd = x + fmet.getMaxAscent();
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
      //      modified("LabelDrawer1: setBounds()");
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
    int swidth, sheight, sascent, xd, yd;
    int xorig, yorig;
    int[] xt = new int[4];
    int[] yt = new int[4];
    int[] xn = new int[4];
    int[] yn = new int[4];
    FontMetrics fmet;
    //
    // compute size of font and adjust to be height tall!
    //
    if (g == null) return;
    font_ = computeFontSize(g);
    g.setFont(font_);
    fmet = g.getFontMetrics();
    //
    swidth = fmet.stringWidth(label_);
    sascent = fmet.getAscent();
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
        yd = yd + sascent / 2;
        break;
      case SGLabel.BOTTOM:
    }
    switch (halign_) {
      case SGLabel.RIGHT:
        xd = xd - swidth;
        break;
      case SGLabel.CENTER:
        xd = xd - swidth / 2;
        break;
      case SGLabel.LEFT:
    }
    xt[0] = xd;
    xt[1] = xt[0];
    xt[2] = xt[0] + swidth;
    xt[3] = xt[2];

    sheight = fmet.getMaxAscent() + fmet.getMaxDescent();
    yt[0] = yd + fmet.getMaxDescent() - sheight;
    yt[1] = yt[0] + sheight;
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
    FontMetrics fmet;
    int pt_0, pt_1, hgt;
    int count = 1;
    double hgt_0, hgt_1, del_0, del_1;
    double a, b;
    //
    // first guess
    //
    if (g == null) return font_;
    hgt = layer_.getXPtoD(height_) - layer_.getXPtoD(0.0f);
    pt_0 = hgt - 3;
    tfont = new Font(font_.getName(), font_.getStyle(), pt_0);
    g.setFont(tfont);
    fmet = g.getFontMetrics();
    hgt = fmet.getAscent() + fmet.getDescent();
    hgt_0 = layer_.getXDtoP(hgt) - layer_.getXDtoP(0);
    pt_0 = tfont.getSize();
    pt_1 = (int) ((double) pt_0 * (height_ / hgt_0));
    while ((pt_0 != pt_1) && (count < 5)) {
      tfont = new Font(font_.getName(), font_.getStyle(), pt_1);
      g.setFont(tfont);
      fmet = g.getFontMetrics();
      hgt = fmet.getAscent() + fmet.getDescent();
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

  @Override
  public float getStringWidth(Graphics g) {
    if (g == null) return 0.0f;
    FontMetrics fmet = g.getFontMetrics(font_);
    return (float) fmet.stringWidth(label_);
  }

  @Override
  public float getStringHeight(Graphics g) {
    if (g == null) return 0.0f;
    FontMetrics fmet = g.getFontMetrics(font_);
    return (float) (fmet.getAscent() * 0.75) + 1.0f;
  }
}
