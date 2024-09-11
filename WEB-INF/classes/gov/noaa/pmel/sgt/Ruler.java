/*
 * $Id: Ruler.java,v 1.14 2003/08/22 23:02:32 dwd Exp $
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
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

// jdk1.2
// import java.awt.geom.Rectangle2D;
// import java.awt.geom.Point2D;

/**
 * Description of Class Ruler
 *
 * @author Donald Denbo
 * @version $Revision: 1.14 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 */
public class Ruler implements Cloneable, LayerChild {
  private String ident_;

  /**
   * @directed
   */
  private Layer layer_;

  private int orient_;
  //
  private boolean selected_;
  private boolean selectable_;
  private boolean visible_;

  /**
   * @label title
   * @link aggregationByValue
   */
  private SGLabel title_ = null;

  private String labelFormat_ = "";
  protected Range2D uRange_;
  //  protected Point2D.Double uLocation_;
  protected Rectangle2D.Double pBounds_;
  protected int numSmallTics_ = 0;
  protected double largeTicHeight_ = 0.1;
  protected double smallTicHeight_ = 0.05;
  protected int ticPosition_ = NEGATIVE_SIDE;
  protected int labelPosition_ = NEGATIVE_SIDE;
  protected int labelInterval_ = 2;
  protected Font labelFont_ = new Font("Helvetica", Font.ITALIC, 10);
  protected Color labelColor_ = Color.black;
  protected Color lineColor_ = Color.black;
  //  protected double labelHeight_ = 0.15;
  protected double labelHeight_ = 0.20;
  protected int sigDigits_ = 2;

  /** Orient Key horizontally. */
  public static final int HORIZONTAL = 1;

  /** Orient Key vertically. */
  public static final int VERTICAL = 2;

  /**
   * Place the label and/or tic on the positive side of the axis. The right side of VERTICAL axes
   * and the top of HORIZONTAL axes.
   */
  public static final int POSITIVE_SIDE = 0;

  /**
   * Place the label and/or tic on the negative side of the axis. The left side of VERTICAL axes and
   * the bottom of HORIZONTAL axes.
   */
  public static final int NEGATIVE_SIDE = 1;

  /** Do not draw a label and/or tic. */
  public static final int NO_LABEL = 2;

  /** Draw the tics on both sides of the axes. */
  public static final int BOTH_SIDES = 2;

  //
  static final double TIC_GAP = 0.05;
  static final double TIC_RATIO = 1.3;
  static final double LABEL_RATIO = 1.3;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      layer_ = null;
      title_ = null;
      labelFont_ = null;
      if (JPane.debug) String2.log("sgt.Ruler.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  protected void drawXTic(Graphics g, double xp, double yp, double ticHeight) {
    int x0, y0, y1;
    double yp0, yp1;
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
      yp0 = yp + ticHeight;
    } else {
      yp0 = yp;
    }
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
      yp1 = yp - ticHeight;
    } else {
      yp1 = yp;
    }
    x0 = layer_.getXPtoD(xp);
    y0 = layer_.getYPtoD(yp0);
    y1 = layer_.getYPtoD(yp1);
    g.drawLine(x0, y0, x0, y1);
  }

  //
  protected void drawYTic(Graphics g, double xp, double yp, double ticHeight) {
    int x0, x1, y0;
    double xp0, xp1;
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
      xp0 = xp + ticHeight;
    } else {
      xp0 = xp;
    }
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
      xp1 = xp - ticHeight;
    } else {
      xp1 = xp;
    }
    y0 = layer_.getYPtoD(yp);
    x0 = layer_.getXPtoD(xp0);
    x1 = layer_.getXPtoD(xp1);
    g.drawLine(x0, y0, x1, y0);
  }

  //
  protected void drawSmallXTics(Graphics g, double xu, double xtest, double del, double yp) {
    int x0, y0, y1, i;
    double yp0, yp1, smdel, xt;
    if (numSmallTics_ <= 0) return;
    //        yp = graph_.getYUtoP(yu);
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
      yp0 = yp + smallTicHeight_;
    } else {
      yp0 = yp;
    }
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
      yp1 = yp - smallTicHeight_;
    } else {
      yp1 = yp;
    }
    y0 = layer_.getYPtoD(yp0);
    y1 = layer_.getYPtoD(yp1);
    smdel = del / (numSmallTics_ + 1);
    for (i = 0; i <= numSmallTics_; i++) {
      xt = xu + smdel * i;
      if ((xtest - xt) / del >= 0) {
        x0 = layer_.getXPtoD(xt);
        g.drawLine(x0, y0, x0, y1);
      }
    }
  }

  //
  protected void drawSmallYTics(Graphics g, double xp, double yu, double ytest, double del) {
    int x0, x1, y0, i;
    double xp0, xp1, smdel, yt;
    if (numSmallTics_ <= 0) return;
    //        xp = graph_.getXUtoP(xu);
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
      xp0 = xp + smallTicHeight_;
    } else {
      xp0 = xp;
    }
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
      xp1 = xp - smallTicHeight_;
    } else {
      xp1 = xp;
    }
    x0 = layer_.getXPtoD(xp0);
    x1 = layer_.getXPtoD(xp1);
    smdel = del / (numSmallTics_ + 1);
    for (i = 0; i <= numSmallTics_; i++) {
      yt = yu + smdel * i;
      if ((ytest - yt) / del >= 0) {
        y0 = layer_.getYPtoD(yt);
        g.drawLine(x0, y0, x1, y0);
      }
    }
  }

  /** Default constructor for Ruler. */
  public Ruler() {
    this("");
  }

  /**
   * Constructor for Ruler. Sets the ruler identifier and initializes the defaults. Default values
   * are:
   *
   * <PRE>
   *   numberSmallTics = 0
   *   largeTicHeightP = 0.1
   *   smallTicHeightP = 0.05
   *       ticPosition = NEGATIVE_SIDE
   *     labelPosition = NEGATIVE_SIDE
   *     labelInterval = 2
   *         labelFont = Font("Helvetica", Font.ITALIC, 10);
   *      labelHeightP = 0.15
   * significantDigits = 2;
   *       labelFormat = ""
   *             title = null
   *       orientation = HORIZONTAL
   *                Id = ""
   * </PRE>
   *
   * @param id axis identification
   */
  public Ruler(String ident) {
    ident_ = ident;
    selected_ = false;
    selectable_ = true;
    visible_ = true;
  }

  @Override
  public LayerChild copy() {
    Ruler newRuler;
    try {
      newRuler = (Ruler) clone();
    } catch (CloneNotSupportedException e) {
      newRuler = new Ruler();
    }
    return (LayerChild) newRuler;
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
   * Set the large tic height in physical units.
   *
   * @param lthgt large tic height.
   */
  public void setLargeTicHeightP(double lthgt) {
    if (largeTicHeight_ != lthgt) {
      largeTicHeight_ = lthgt;
      modified("Ruler: setLargeTicHeightP()");
    }
  }

  /**
   * Get the large tic height.
   *
   * @return large tic height in physcial units.
   */
  public double getLargeTicHeightP() {
    return largeTicHeight_;
  }

  /**
   * Set the number of small tics between large tics.
   *
   * @param nstic number of small tics.
   */
  public void setNumberSmallTics(int nstic) {
    if (numSmallTics_ != nstic) {
      numSmallTics_ = nstic;
      modified("Ruler: setNumberSmallTics()");
    }
  }

  /**
   * Get the number of small tics between large tics.
   *
   * @return number of small tics.
   */
  public int getNumberSmallTics() {
    return numSmallTics_;
  }

  /**
   * Set the small tic height in physical units.
   *
   * @param sthgt small tic height.
   */
  public void setSmallTicHeightP(double sthgt) {
    if (smallTicHeight_ != sthgt) {
      smallTicHeight_ = sthgt;
      modified("Ruler: setSmallTicHeightP()");
    }
  }

  /**
   * Get the small tic height.
   *
   * @return small tic height in physical units.
   */
  public double getSmallTicHeightP() {
    return smallTicHeight_;
  }

  /**
   * Set the tic position. Tic position can be POSITIVE_SIDE, NEGATIVE_SIDE, or BOTH_SIDES.
   *
   * @param tpos tic position
   */
  public void setTicPosition(int tpos) {
    if (ticPosition_ != tpos) {
      ticPosition_ = tpos;
      modified("Ruler: setTicPosition()");
    }
  }

  /**
   * Get the tic position.
   *
   * @return tic position
   */
  public int getTicPosition() {
    return ticPosition_;
  }

  /**
   * Set the label position. Label position can be POSITIVE_SIDE, NEGATIVE_SIDE, and NO_LABEL.
   *
   * @param lapb label position.
   */
  public void setLabelPosition(int labp) {
    if (labelPosition_ != labp) {
      labelPosition_ = labp;
      modified("Ruler: setLabelPosition()");
    }
  }

  /**
   * Get the label position.
   *
   * @return label position
   */
  public int getLabelPosition() {
    return labelPosition_;
  }

  /**
   * Set the label font.
   *
   * @param fnt label font
   */
  public void setLabelFont(Font fnt) {
    if (labelFont_ == null || !labelFont_.equals(fnt)) {
      labelFont_ = fnt;
      modified("Ruler: setLabelFont()");
    }
  }

  /**
   * Get the label font.
   *
   * @return label font
   */
  public Font getLabelFont() {
    return labelFont_;
  }

  /**
   * Set the label height in physical units.
   *
   * @param lhgt label height.
   */
  public void setLabelHeightP(double lhgt) {
    if (labelHeight_ != lhgt) {
      labelHeight_ = lhgt;
      modified("Ruler: setLabelHeightP()");
    }
  }

  /**
   * Get the label height.
   *
   * @return label height
   */
  public double getLabelHeightP() {
    return labelHeight_;
  }

  /**
   * Set the axis identifier.
   *
   * @param id identifier
   */
  @Override
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Set the axis identifier.
   *
   * @param id identifier
   */
  @Override
  public String getId() {
    return ident_;
  }

  @Override
  public void setLayer(Layer l) {
    layer_ = l;
  }

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
    //    if(Debug.EVENT) System.out.println("Ruler: modified()");
    if (layer_ != null) layer_.modified(mess);
  }

  /** Change the user unit range of <code>Ruler</code> */
  public void setRangeU(Range2D range) {
    if (uRange_ == null || !uRange_.equals(range)) {
      uRange_ = range;
      modified("Ruler: setRangeU()");
    }
  }

  public Range2D getRangeU() {
    return uRange_;
  }

  /**
   * Set the bounding box for the axis in physical units.
   *
   * @return bounding box
   */
  public void setBoundsP(Rectangle2D.Double bounds) {
    if (pBounds_ == null || !pBounds_.equals(bounds)) {
      if (Debug.EVENT) System.out.println("pBounds_ = " + pBounds_ + ", bounds = " + bounds);
      pBounds_ = bounds;
      modified("Ruler: setBoundsP()");
    }
  }

  /**
   * Get the bounding box for the axis in physical units.
   *
   * @return bounding box
   */
  public Rectangle2D.Double getBoundsP() {
    return pBounds_;
  }

  /**
   * Set ruler orientation. Allowed orientations are HORIZONATAL and VERTICAL.
   *
   * @param or orientation
   */
  public void setOrientation(int orient) {
    if (orient_ != orient) {
      orient_ = orient;
      modified("Ruler: setOrientation()");
    }
  }

  /**
   * Get axis orientation
   *
   * @return axis orientation
   */
  public int getOrientation() {
    return orient_;
  }

  /**
   * Set the axis title.
   *
   * @param title axis title
   */
  public void setTitle(SGLabel title) {
    if (title_ == null || !title_.equals(title)) {
      title_ = title;
      modified("Ruler: setTitle()");
    }
  }

  /**
   * Get the axis title.
   *
   * @return axis title
   */
  public SGLabel getTitle() {
    return title_;
  }

  /**
   * Get the bounding box for the axis in device units.
   *
   * @return bounding box
   */
  @Override
  public Rectangle getBounds() {
    int x, y, height, width;
    x = layer_.getXPtoD(pBounds_.x);
    y = layer_.getYPtoD(pBounds_.y);
    width = layer_.getXPtoD(pBounds_.x + pBounds_.width) - x;
    height = layer_.getYPtoD(pBounds_.y - pBounds_.height) - y;
    if (orient_ == HORIZONTAL) {
      y = y - height;
    }
    return new Rectangle(x, y, width, height);
  }

  public void setBounds(Rectangle r) {
    setBounds(r.x, r.y, r.width, r.height);
  }

  public void setBounds(int x, int y, int width, int height) {}

  @Override
  public void draw(Graphics g) {
    int xloc, yloc, xend, yend;
    int istop, i;
    double xt, yt, dir, x, y, xp, yp;
    double xtitle, ytitle;
    Format format;
    String labelText = null;
    AxisTransform sTrans;
    if (!visible_) return;
    if (title_ != null) title_.setLayer(layer_);
    //
    if (lineColor_ == null) {
      g.setColor(layer_.getPane().getComponent().getForeground());
    } else {
      g.setColor(lineColor_);
    }
    //
    if (labelFormat_.length() <= 0) {
      format = new Format(Format.computeFormat(uRange_.start, uRange_.end, sigDigits_));
    } else {
      format = new Format(labelFormat_);
    }
    if (orient_ == HORIZONTAL) {
      sTrans =
          new LinearTransform(pBounds_.x, pBounds_.x + pBounds_.width, uRange_.start, uRange_.end);
      yloc = layer_.getYPtoD(pBounds_.y);
      yp = pBounds_.y;
      xloc = layer_.getXPtoD(sTrans.getTransP(uRange_.start));
      xend = layer_.getXPtoD(sTrans.getTransP(uRange_.end));
      g.drawLine(xloc, yloc, xend, yloc);
      //
      dir = uRange_.delta > 0 ? 1.0 : -1.0;
      xt =
          (int)
              ((uRange_.start / uRange_.delta + (dir * uRange_.start > 0 ? 1.0 : -1.0) * 0.00001)
                  * uRange_.delta);
      if (dir * xt < dir * uRange_.start) xt += uRange_.delta;
      istop = (int) ((uRange_.end - xt) / uRange_.delta + 0.00001);
      x = xt;
      xp = sTrans.getTransP(x);
      drawSmallXTics(g, x, uRange_.start, -uRange_.delta, yp);
      drawXTic(g, xp, yp, largeTicHeight_);
      for (i = 0; i < istop; i++) {
        drawSmallXTics(g, x, uRange_.end, uRange_.delta, yp);
        x += uRange_.delta;
        xp = sTrans.getTransP(x);
        drawXTic(g, xp, yp, largeTicHeight_);
      }
      drawSmallXTics(g, x, uRange_.end, uRange_.delta, yp);
      //
      if (labelInterval_ <= 0 || labelPosition_ == NO_LABEL) return;
      //
      SGLabel label;
      int vertalign;
      if (labelPosition_ == POSITIVE_SIDE) {
        vertalign = SGLabel.BOTTOM;
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
          yt = yp + TIC_RATIO * largeTicHeight_;
        } else {
          yt = yp + TIC_GAP;
        }
        ytitle = yt + LABEL_RATIO * labelHeight_;
      } else {
        vertalign = SGLabel.TOP;
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
          yt = yp - TIC_RATIO * largeTicHeight_;
        } else {
          yt = yp - TIC_GAP;
        }
        ytitle = yt - LABEL_RATIO * labelHeight_;
      }
      if (dir * uRange_.start <= 0 && dir * uRange_.end >= 0) {
        x =
            ((int) (uRange_.start / (uRange_.delta * labelInterval_) - 0.00001))
                * uRange_.delta
                * labelInterval_;
      } else {
        x = xt;
      }
      istop = (int) ((uRange_.end - x) / (uRange_.delta * labelInterval_) + 0.00001);
      for (i = 0; i <= istop; i++) {
        xt = sTrans.getTransP(x);
        labelText = format.form(x);
        label = new SGLabel("coordinate", labelText, new Point2D.Double(xt, yt));
        label.setFont(labelFont_);
        label.setHeightP(labelHeight_);
        label.setAlign(vertalign, SGLabel.CENTER);
        label.setLayer(layer_);
        label.setColor(labelColor_);
        try {
          label.draw(g);
        } catch (LayerNotFoundException e) {
        }
        x = x + uRange_.delta * labelInterval_;
      }
      if (title_ != null) {
        xtitle = (uRange_.end + uRange_.start) * 0.5;
        yt = ytitle;
        xt = sTrans.getTransP(xtitle);
        title_.setLocationP(new Point2D.Double(xt, yt));
        title_.setAlign(vertalign, SGLabel.CENTER);
        title_.setOrientation(SGLabel.HORIZONTAL);
        try {
          title_.draw(g);
        } catch (LayerNotFoundException e) {
        }
      }
    } else { // orientation is vertical
      sTrans =
          new LinearTransform(pBounds_.y, pBounds_.y + pBounds_.height, uRange_.start, uRange_.end);
      xloc = layer_.getXPtoD(pBounds_.x);
      xp = pBounds_.x;
      yloc = layer_.getYPtoD(sTrans.getTransP(uRange_.start));
      yend = layer_.getYPtoD(sTrans.getTransP(uRange_.end));
      g.drawLine(xloc, yloc, xloc, yend);
      //
      dir = uRange_.delta > 0 ? 1.0 : -1.0;
      yt =
          (int)
              (((uRange_.start / uRange_.delta) + (dir * uRange_.start > 0 ? 1.0 : -1.0) * 0.00001)
                  * uRange_.delta);
      if (dir * yt < dir * uRange_.start) yt += uRange_.delta;
      istop = (int) ((uRange_.end - yt) / uRange_.delta + 0.00001);
      y = yt;
      yp = sTrans.getTransP(y);
      drawSmallYTics(g, xp, y, uRange_.start, -uRange_.delta);
      drawYTic(g, xp, yp, largeTicHeight_);
      for (i = 0; i < istop; i++) {
        drawSmallYTics(g, xp, y, uRange_.end, uRange_.delta);
        y += uRange_.delta;
        yp = sTrans.getTransP(y);
        drawYTic(g, xp, yp, largeTicHeight_);
      }
      drawSmallYTics(g, xp, y, uRange_.end, uRange_.delta);
      //
      if (labelInterval_ <= 0 || labelPosition_ == NO_LABEL) return;
      //
      SGLabel label;
      int vertalign;
      if (labelPosition_ == NEGATIVE_SIDE) {
        vertalign = SGLabel.BOTTOM;
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
          xt = xp - TIC_RATIO * largeTicHeight_;
        } else {
          xt = xp - TIC_GAP;
        }
        xtitle = xt - LABEL_RATIO * labelHeight_;
      } else {
        vertalign = SGLabel.TOP;
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
          xt = xp + TIC_RATIO * largeTicHeight_;
        } else {
          xt = xp + TIC_GAP;
        }
        xtitle = xt + LABEL_RATIO * labelHeight_;
      }
      if (dir * uRange_.start <= 0 && dir * uRange_.end >= 0) {
        y =
            ((int) (uRange_.start / (uRange_.delta * labelInterval_) - 0.00001))
                * uRange_.delta
                * labelInterval_;
      } else {
        y = yt;
      }
      istop = (int) ((uRange_.end - y) / (uRange_.delta * labelInterval_) + 0.00001);
      for (i = 0; i <= istop; i++) {
        yt = sTrans.getTransP(y);
        labelText = format.form(y);
        label = new SGLabel("coordinate", labelText, new Point2D.Double(xt, yt));
        label.setAlign(SGLabel.CENTER, SGLabel.LEFT); // was vertalign, SGLabel.CENTER);
        label.setOrientation(SGLabel.HORIZONTAL); // was VERTICAL);
        label.setFont(labelFont_);
        label.setHeightP(labelHeight_);
        label.setLayer(layer_);
        try {
          label.draw(g);
        } catch (LayerNotFoundException e) {
        }
        y = y + uRange_.delta * labelInterval_;
      }
      if (title_ != null) {
        ytitle = (uRange_.end + uRange_.start) * 0.5;
        yt = sTrans.getTransP(ytitle);
        xt = xtitle;
        title_.setLocationP(new Point2D.Double(xt, yt));
        title_.setAlign(vertalign, SGLabel.CENTER);
        title_.setOrientation(SGLabel.VERTICAL);
        try {
          title_.draw(g);
        } catch (LayerNotFoundException e) {
        }
      }
    }
  }

  @Override
  public boolean isVisible() {
    return visible_;
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible_ != visible) {
      visible_ = visible;
      modified("Ruler: setVisible()");
    }
  }

  /**
   * @since 3.0
   */
  public int getLabelInterval() {
    return labelInterval_;
  }

  /**
   * @since 3.0
   */
  public void setLabelInterval(int labelInterval) {
    labelInterval_ = labelInterval;
  }

  /**
   * @since 3.0
   */
  public int getSignificantDigits() {
    return sigDigits_;
  }

  /**
   * @since 3.0
   */
  public void setSignificantDigits(int sigDigits) {
    sigDigits_ = sigDigits;
  }

  /**
   * @since 3.0
   */
  public String getLabelFormat() {
    return labelFormat_;
  }

  /**
   * @since 3.0
   */
  public void setLabelFormat(String labelFormat) {
    labelFormat_ = labelFormat;
  }

  /**
   * @since 3.0
   */
  public Color getLabelColor() {
    return labelColor_;
  }

  /**
   * @since 3.0
   */
  public void setLabelColor(Color labelColor) {
    labelColor_ = labelColor;
  }

  /**
   * @since 3.0
   */
  public Color getLineColor() {
    return lineColor_;
  }

  /**
   * @since 3.0
   */
  public void setLineColor(Color lineColor) {
    lineColor_ = lineColor;
  }
}
