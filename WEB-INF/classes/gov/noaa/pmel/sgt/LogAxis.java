/*
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
// RCS $Id: LogAxis.java,v 1.5 2003/08/22 23:02:32 dwd Exp $

package gov.noaa.pmel.sgt;

import gov.noaa.pmel.util.Debug;
import gov.noaa.pmel.util.Point2D;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Axis class for creation of "log" axes. An {@link gov.noaa.pmel.sgt.demo.JLogLogDemo exmample} is
 * available demonstrating <code>LogAxis</code> use.
 *
 * <p>--------------------------------------------------------------------------<br>
 * NAME : LogAxis.java<br>
 * FUNCTION : Draws axes using "log" style axis.<br>
 * ORIGIN : GFI INFORMATIQUE<br>
 * PROJECT : SONC DPS<br>
 * -------------------------------------------------------------------------<br>
 * HISTORY<br>
 * VERSION : 03/07/2002 : V0.0 : LBE<br>
 * old version had no fonctionality. It was just written for future evolutions. This new version
 * complete the class<br>
 * END-HISTORY<br>
 * ------------------------------------------------------------------------<br>
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/08/22 23:02:32 $
 * @since 3.0
 */
public class LogAxis extends SpaceAxis implements Cloneable {

  gov.noaa.pfel.coastwatch.sgt.GenEFormatter genEFormatter =
      new gov.noaa.pfel.coastwatch.sgt.GenEFormatter();

  public LogAxis(String id) {
    super(id);
    space_ = true;
    numSmallTics_ = 9; // fixed number. only 8 shown
  }

  @Override
  public Axis copy() {
    LogAxis newAxis;
    try {
      newAxis = (LogAxis) clone();
    } catch (CloneNotSupportedException e) {
      newAxis = new LogAxis(getId());
    }
    return (Axis) newAxis;
  }

  @Override
  public void draw(Graphics g) {
    // throw new MethodNotImplementedError();
    int xloc, yloc, xend, yend;
    int istop, i;
    double j;
    double xt, yt, dir, x, y, xp, yp;
    double xtitle, ytitle;
    double delta = uRange_.delta;
    Format format;
    String labelText;
    SGLabel title = getTitle();
    if (!visible_) return;

    // Bob added
    double uRangeStart = uRange_.start;
    double uRangeEnd = uRange_.end;
    if (uRangeStart > uRangeEnd) {
      double t = uRangeStart;
      uRangeStart = uRangeEnd;
      uRangeEnd = t;
    }
    delta = Math.abs(delta);

    if (Double.isNaN(delta)) delta = (uRangeEnd - uRangeStart) / 10.0;
    if (title != null) title.setLayer(graph_.getLayer());
    //
    g.setColor(graph_.getLayer().getPane().getComponent().getForeground());
    //

    if (labelFormat_.length() <= 0) {
      format = new Format(Format.computeFormat(uRangeStart, uRangeEnd, sigDigits_));
    } else {
      format = new Format(labelFormat_);
    }
    if (orientation_ == Axis.HORIZONTAL) {
      if (Debug.DEBUG) System.out.println("LogAxis: start drawing XAxis");
      if (uLocation_ == null) {
        yloc = graph_.getYUtoD(tLocation_.t);
        yp = graph_.getYUtoP(tLocation_.t);
      } else {
        yloc = graph_.getYUtoD(uLocation_.y);
        yp = graph_.getYUtoP(uLocation_.y);
      }
      xloc = graph_.getXUtoD(uRangeStart);
      xend = graph_.getXUtoD(uRangeEnd);
      g.drawLine(xloc, yloc, xend, yloc);

      // X tics drawing
      dir = delta > 0 ? 1.0 : -1.0;
      xt = (int) ((uRangeStart / delta + (dir * uRangeStart > 0 ? 1.0 : -1.0) * 0.00001) * delta);

      if (dir * xt < dir * uRangeStart) xt += delta;
      istop = (int) ((uRangeEnd - xt) / delta + 0.00001);

      if (uRangeStart <= 0) return;

      int imin =
          (int)
              Math.ceil(
                  Math.log10(uRangeStart) - 0.000000000001); // first large tic //Bob added fudge
      int imax = (int) Math.floor(Math.log10(uRangeEnd) + 0.000000000001); // last large tic
      int nblabel = imax - imin + 1;

      /*      System.out.println("uRange.start/end: "+uRangeStart+"/"+uRangeEnd);
            System.out.println("uRangeP: "+graph_.getYUtoP(uRangeStart)+"/"+graph_.getYUtoP(uRangeEnd));
      */
      double min = (double) Math.pow(10, imin);
      double max = (double) Math.pow(10, imax);

      xt = min;
      x = xt;
      xp = graph_.getXUtoP(x);

      drawSmallXTics(g, min / 10, Math.min(min, uRangeEnd), min, yp, uRangeStart);
      for (j = min; j <= max; j = j * 10.0d) {
        if (j > min) drawSmallXTics(g, j / 10, j, j, yp, uRangeStart);
        xp = graph_.getXUtoP(j);
        drawXTic(g, xp, yp, largeTicHeight_);
      }
      drawSmallXTics(g, j / 10, uRangeEnd, j, yp, uRangeStart);

      //
      if (labelInterval_ <= 0 || labelPosition_ == NO_LABEL) return;

      SGLabel label;
      int vertalign;
      int horzalign;

      if (dir * uRangeStart <= 0 && dir * uRangeEnd >= 0) {
        x = ((int) (uRangeStart / (delta * labelInterval_) - 0.00001)) * delta * labelInterval_;
      } else {
        x = xt;
      }
      istop = (int) ((uRangeEnd - x) / (delta * labelInterval_) + 0.00001);
      long jump = 10; // label display on each tic
      // if(istop<nblabel) jump = 100; // one on two

      if (labelPosition_ == POSITIVE_SIDE) {
        vertalign = SGLabel.BOTTOM;
        horzalign = SGLabel.CENTER;
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
          yt = yp + TIC_RATIO * largeTicHeight_;
        } else {
          yt = yp + TIC_GAP;
        }
        ytitle = yt + LABEL_RATIO * labelHeight_;
      } else {
        vertalign = SGLabel.TOP;
        horzalign = SGLabel.CENTER;
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
          yt = yp - TIC_RATIO * largeTicHeight_;
        } else {
          yt = yp - TIC_GAP;
        }
        ytitle = yt - LABEL_RATIO * labelHeight_;
      }

      double tMin = min; // Bob added
      double tMax = max;
      double tJump = jump;
      boolean times = true;
      if (imax - imin > 27) tJump = 10000;
      else if (imax - imin > 18) tJump = 1000;
      else if (imax - imin > 9) tJump = 100; // e.g. 10, 1000, 100000
      // else tJump = 10;                        //e.g. 10,  100,   1000
      else if (imax < imin) {
        times = false;
        tMin = max;
        tMax = min;
        tJump = max;
      } // within a decade, e.g., >10 and <100, increment by 10's
      for (j = tMin; j <= tMax; j = times ? j * tJump : j + tJump) {
        // String2.log(">> uRange=" + uRangeStart + " " + uRangeEnd + " tMin=" + tMin + " tMax=" +
        // tMax + " tJump=" + tJump + " j=" + j);
        if (j < uRangeStart || j > uRangeEnd) continue;
        xt = graph_.getXUtoP(j) - LABEL_RATIO * labelHeight_ * 0.25;
        // xt = graph_.getXUtoP(j);
        // System.out.println("affich["+j+"]: 10e"+Math.round( Math.log10(j) ));
        labelText =
            j >= 0.001 && j <= 1000 ? genEFormatter.format(j) : "10^" + Math.round(Math.log10(j));
        label = new SGLabel("coordinate", labelText, new Point2D.Double(xt, yt));
        label.setAlign(vertalign, horzalign);
        label.setOrientation(SGLabel.HORIZONTAL);
        label.setFont(labelFont_);
        label.setColor(labelColor_);
        label.setHeightP(labelHeight_);
        label.setLayer(graph_.getLayer());
        try {
          label.draw(g);
        } catch (LayerNotFoundException e) {
        }
        // x = x + delta*labelInterval_;
      }
      if (title_ != null) {
        // xtitle = (uRangeEnd + uRangeStart)*0.5;
        xtitle = graph_.getXUtoP(uRangeEnd) + graph_.getXUtoP(uRangeStart);
        xt = xtitle * 0.5;
        yt = ytitle;
        xt = graph_.getXUtoP(xtitle);
        title.setLocationP(new Point2D.Double(xt, yt));
        title.setAlign(vertalign, SGLabel.CENTER);
        title.setOrientation(SGLabel.HORIZONTAL);
        try {
          title.draw(g);
        } catch (LayerNotFoundException e) {
        }
      }
    } else { // orientation is vertical
      if (Debug.DEBUG) System.out.println("LogAxis: start drawing YAxis");
      if (uLocation_ == null) {
        xloc = graph_.getXUtoD(tLocation_.t);
        xp = graph_.getXUtoP(tLocation_.t);
      } else {
        xloc = graph_.getXUtoD(uLocation_.x);
        xp = graph_.getXUtoP(uLocation_.x);
      }
      yloc = graph_.getYUtoD(uRangeStart);
      yend = graph_.getYUtoD(uRangeEnd);
      g.drawLine(xloc, yloc, xloc, yend);

      // draw Y tics
      dir = delta > 0 ? 1.0 : -1.0;
      yt = (int) (((uRangeStart / delta) + (dir * uRangeStart > 0 ? 1.0 : -1.0) * 0.00001) * delta);
      if (dir * yt < dir * uRangeStart) yt += delta;
      istop = (int) ((uRangeEnd - yt) / delta + 0.00001);

      if (uRangeStart <= 0) return;

      int imin =
          (int)
              Math.ceil(
                  Math.log10(uRangeStart) - 0.000000000001); // premier large tic  //Bob added fudge
      int imax = (int) Math.floor(Math.log10(uRangeEnd) + 0.000000000001); // dernier large tic
      int nblabel = imax - imin + 1;

      // System.out.println("uRange.start/end: "+uRangeStart+"/"+uRangeEnd);
      // System.out.println("uRangeP:
      // "+graph_.getYUtoP(uRangeStart)+"/"+graph_.getYUtoP(uRangeEnd));

      double min = (double) Math.pow(10, imin);
      double max = (double) Math.pow(10, imax);
      // System.out.println(">> LogAxis vertical imax=" + imax + " max=" + max);

      yt = min;
      y = yt;
      yp = graph_.getYUtoP(y);

      drawSmallYTics(g, xp, min / 10, Math.min(min, uRangeEnd), min, uRangeStart);
      for (j = min; j <= max; j = j * 10.0d) {
        if (j > min) drawSmallYTics(g, xp, j / 10, j, j, uRangeStart);
        yp = graph_.getYUtoP(j);
        if (j >= uRangeStart && j <= uRangeEnd) drawYTic(g, xp, yp, largeTicHeight_);
      }
      drawSmallYTics(g, xp, j / 10, uRangeEnd, j, uRangeStart);

      //
      if (labelInterval_ <= 0 || labelPosition_ == NO_LABEL) return;
      //
      SGLabel label;
      int vertalign;
      int horzalign;

      if (dir * uRangeStart <= 0 && dir * uRangeEnd >= 0) {
        y = ((int) (uRangeStart / (delta * labelInterval_) - 0.00001)) * delta * labelInterval_;
      } else {
        y = yt;
      }

      istop = (int) ((uRangeEnd - y) / (delta * labelInterval_) + 0.00001);
      long jump = 10; // label display on each tic
      // if(istop<nblabel) jump = 100; // one on two

      Layer l = graph_.getLayer();
      double widthP = 0;
      double maxWidthP = 0;
      /* Bob set labels to VERTICAL so fixed maxWidthP
      if (l!=null) {
        for(j=min; j<=max; j*=jump) {
          labelText =
            j >= 0.001 && j <= 1000? genEFormatter.format(j) :
            "10^"+Math.round(Math.log10(j));
          //get Y Label size in Device unit
          //widthP = l.getXDtoP(l.getFontMetrics(labelFont_).stringWidth(labelText));
          label = new SGLabel("coordinate", labelText, new Point2D.Double(0, yt));
          label.setOrientation(SGLabel.VERTICAL);
          label.setFont(labelFont_);
          label.setHeightP(labelHeight_);
          label.setLayer(l);
          widthP = l.getXDtoP((int)label.getStringWidth(g));
          if (widthP>maxWidthP) maxWidthP = widthP;
        }
      } */
      maxWidthP = 1.2 * labelHeight_;

      if (labelPosition_ == NEGATIVE_SIDE) {
        vertalign = SGLabel.BOTTOM;
        horzalign = SGLabel.RIGHT;
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
          xt = xp - TIC_RATIO * largeTicHeight_;
        } else {
          xt = xp - TIC_GAP;
        }
        // xtitle = xt - LABEL_RATIO*labelHeight_-LABEL_RATIO*maxWidthP*0.5;
        xtitle = xt - maxWidthP;
      } else {
        vertalign = SGLabel.TOP;
        horzalign = SGLabel.LEFT;
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
          xt = xp + TIC_RATIO * largeTicHeight_;
        } else {
          xt = xp + TIC_GAP;
        }
        // xtitle = xt + LABEL_RATIO*labelHeight_ + maxWidthP;
        xtitle = xt + maxWidthP;
      }

      // g.drawLine(l.getXPtoD(xt),l.getYPtoD(0.0),l.getXPtoD(xt),l.getYPtoD(4.0));
      // g.drawLine(l.getXPtoD(xtitle),l.getYPtoD(0.0),l.getXPtoD(xtitle),l.getYPtoD(4.0));

      double tMin = min; // Bob added
      double tMax = max;
      double tJump = jump;
      boolean times = true;
      if (imax - imin > 27) tJump = 10000;
      else if (imax - imin > 18) tJump = 1000;
      else if (imax - imin > 9) tJump = 100; // e.g. 10, 1000, 100000
      // else tJump = 10;                        //e.g. 10,  100,   1000
      else if (imax < imin) {
        times = false;
        tMin = max;
        tMax = min;
        tJump = max;
      } // within a decade, e.g., >10 and <100, increment by 10's
      for (j = tMin; j <= tMax; j = times ? j * tJump : j + tJump) {
        // String2.log(">> uRange=" + uRangeStart + " " + uRangeEnd + " tMin=" + tMin + " tMax=" +
        // tMax + " tJump=" + tJump + " j=" + j);
        if (j < uRangeStart || j > uRangeEnd) continue;
        yt = graph_.getYUtoP(j); // -LABEL_RATIO*labelHeight_*0.25;
        // System.out.println("affich["+j+"]: 10e"+Math.round( Math.log10(j)));
        labelText =
            j >= 0.001 && j <= 1000 ? genEFormatter.format(j) : "10^" + Math.round(Math.log10(j));
        // System.out.println(">> j=" + j + " labelText=" + labelText);
        label = new SGLabel("coordinate", labelText, new Point2D.Double(xt, yt));
        label.setAlign(vertalign, SGLabel.CENTER);
        label.setOrientation(SGLabel.VERTICAL);
        label.setFont(labelFont_);
        label.setColor(labelColor_);
        label.setHeightP(labelHeight_);
        label.setLayer(graph_.getLayer());
        try {
          label.draw(g);
        } catch (LayerNotFoundException e) {
        }
        // y = j*10;//delta*labelInterval_;
      }
      if (title_ != null) {
        ytitle = graph_.getYUtoP(uRangeEnd) + graph_.getYUtoP(uRangeStart);
        yt = ytitle * 0.5;
        xt = xtitle;
        title.setLocationP(new Point2D.Double(xt, yt));
        title.setAlign(vertalign, SGLabel.CENTER);
        title.setOrientation(SGLabel.VERTICAL);
        try {
          title.draw(g);
        } catch (LayerNotFoundException e) {
        }
      }
    }
  }

  /**
   * Get the bounding box for the axis in device units.
   *
   * @return bounding box
   * @see Rectangle
   */
  @Override
  public Rectangle getBounds() {
    double xp, yp, ymin, ymax, xmin, xmax;
    int xd, yd, width, height, x, y;
    if (orientation_ == Axis.HORIZONTAL) {
      xd = graph_.getXUtoD(uRange_.start);
      if (uLocation_ == null) {
        yp = graph_.getYUtoP(tLocation_.t);
      } else {
        yp = graph_.getYUtoP(uLocation_.y);
      }
      width = graph_.getXUtoD(uRange_.end) - xd;
      x = xd;
      ymin = yp;
      ymax = yp;
      if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
        ymax = ymax + largeTicHeight_;
      }
      if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
        ymin = ymin - largeTicHeight_;
      }
      if (labelPosition_ == POSITIVE_SIDE) {
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
          ymax = ymax + (1.0 - TIC_RATIO) * largeTicHeight_ + labelHeight_;
        } else {
          ymax = ymax + TIC_GAP + labelHeight_;
        }
      } else if (labelPosition_ == NEGATIVE_SIDE) {
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
          ymin = ymin - (1.0 - TIC_RATIO) * largeTicHeight_ - labelHeight_;
        } else {
          ymin = ymin - TIC_GAP - labelHeight_;
        }
      }
      y = graph_.getLayer().getYPtoD(ymax);
      height = graph_.getLayer().getYPtoD(ymin) - y;
    } else {
      yd = graph_.getYUtoD(uRange_.start);
      if (uLocation_ == null) {
        xp = graph_.getXUtoP(tLocation_.t);
      } else {
        xp = graph_.getXUtoP(uLocation_.x);
      }
      y = graph_.getYUtoD(uRange_.end);
      height = yd - y;
      xmin = xp;
      xmax = xp;
      if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
        xmax = xmax + largeTicHeight_;
      }
      if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
        xmin = xmin - largeTicHeight_;
      }
      if (labelPosition_ == POSITIVE_SIDE) {
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
          xmax = xmax + (1.0 - TIC_RATIO) * largeTicHeight_ + labelHeight_;
        } else {
          xmax = xmax + TIC_GAP + labelHeight_;
        }
      } else if (labelPosition_ == NEGATIVE_SIDE) {
        if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
          xmin = xmin - (1.0 - TIC_RATIO) * largeTicHeight_ - labelHeight_;
        } else {
          xmin = xmin - TIC_GAP - labelHeight_;
        }
      }
      x = graph_.getLayer().getXPtoD(xmin);
      width = graph_.getLayer().getXPtoD(xmax) - x;
    }
    return new Rectangle(x, y, width, height);
  }

  public void setBounds(int x, int y, int width, int height) {}

  public void setBounds(Rectangle rect) {
    setBounds(rect.x, rect.y, rect.width, rect.height);
  }
}
