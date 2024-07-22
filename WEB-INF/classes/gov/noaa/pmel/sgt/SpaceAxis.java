/*
 * $Id: SpaceAxis.java,v 1.9 2003/08/22 23:02:32 dwd Exp $
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

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTValue;
import gov.noaa.pmel.util.TimePoint;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;

// jdk1.2
// import java.awt.geom.Point2D;

/**
 * Abstract base class for axes whose user coordinates are double values. The following is an
 * example of using a {@link PlainAxis}.
 *
 * <pre>
 * import gov.noaa.pmel.sgt.PlainAxis;
 * import gov.noaa.pmel.sgt.LinearTransform;
 * import gov.noaa.pmel.sgt.Graph;
 * ...
 * Graph graph;
 * PlainAxis xbot, xtop;
 * LinearTransform xt;
 * Point2D.Double lowerleft = new Point2D.Double(10.0, 100.0);
 * ...
 * //
 * // Instatiate xt and set it as the x transform.
 * //
 * xt = new LinearTransform(0.75, 3.5, 10.0, 100.0);
 * graph.setXTransform(xt);
 * ...
 * //
 * // Instatiate xbot and set its range, delta, and
 * // location.  Set xbot the numberSmallTics property
 * // for xbot.
 * //
 * xbot = new PlainAxis("Bottom Axis");
 * xbot.setRangeU(new Range2D(10.0, 100.0));
 * xbot.setDeltaU(20.0);
 * xbot.setNumberSmallTics(4);
 * xbot.setLocationU(lowerleft);
 * //
 * // Create title for xbot.
 * //
 * Font xbfont = new Font("Helvetica", Font.ITALIC, 14);
 * xbot.setLabelFont(xbfont);
 * SGLabel xtitle = new SGLabel("xaxis title",
 *                              "Test X-Axis Title",
 *                              new Point2D.Double(0.0, 0.0));
 * Font xtfont = new Font("Helvetica", Font.PLAIN, 14);
 * xtitle.setFont(xtfont);
 * xtitle.setHeightP(0.2);
 * xbot.setTitle(xtitle);
 * graph.setXAxis(xbot);
 * ...
 * //
 * // Instatiate xtop and set its range, delta, and
 * // location.  Set xtop properties on ticPosition and
 * // labelPosition.
 * //
 * xtop = new PlainAxis("Top Axis");
 * xtop.setRangeU(new Range2D(10.0, 100.0));
 * xtop.setDeltaU(20.0);
 * xtop.setNumberSmallTics(0);
 * xtop.setLocationU(new Point2D.Double(10.0, 300.0));
 * xtop.setLabelFont(xbfont);
 * xtop.setTicPosition(Axis.POSITIVE_SIDE);
 * xtop.setLabelPosition(Axis.NO_LABEL);
 * graph.setXAxis(xtop);
 * ...
 * //
 * // Register the x transform and the top x axis with the bottom x axis.
 * // By registering xt and xtop, any updates to the user or physical range
 * // of xbot will be automatically performed on xt and xtop.
 * //
 * xbot.register(xt);
 * xbot.register(xtop);
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.9 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 * @see Axis
 * @see PlainAxis
 * @see TimeAxis
 */
public abstract class SpaceAxis extends Axis {
  public Range2D uRange_;
  public Point2D.Double uLocation_;
  public TimePoint tLocation_;
  static final double TIC_GAP = 0.05;
  static final double TIC_RATIO = 1.3;
  static final double LABEL_RATIO = 1.3;

  //
  @Override
  protected void updateRegisteredTransforms() {
    if (!registeredTransforms_.isEmpty()) {
      AxisTransform trns;
      for (Enumeration it = registeredTransforms_.elements(); it.hasMoreElements(); ) {
        trns = (AxisTransform) it.nextElement();
        trns.setRangeP(pRange_);
        trns.setRangeU(uRange_);
      }
    }
  }

  //
  @Override
  protected void updateRegisteredAxes() {
    if (!registeredAxes_.isEmpty()) {
      SpaceAxis ax;
      for (Enumeration it = registeredAxes_.elements(); it.hasMoreElements(); ) {
        ax = (SpaceAxis) it.nextElement();
        ax.setRangeU(uRange_);
        ax.setRangeP(pRange_);
      }
    }
  }

  // Bob added atLeast                      start     finish       (big)delta   y
  protected void drawSmallXTics(
      Graphics g, double xu, double xtest, double del, double yp, double atLeast) {
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
    y0 = graph_.getLayer().getYPtoD(yp0);
    y1 = graph_.getLayer().getYPtoD(yp1);
    smdel = del / (numSmallTics_ + 1);
    for (i = 0; i <= numSmallTics_; i++) {
      xt = xu + smdel * i;
      if (xt >= atLeast && (xtest - xt) / del >= 0) {
        x0 = graph_.getXUtoD(xt);
        g.drawLine(x0, y0, x0, y1);
      }
    }
  }

  // bob added atLeast                      x         start     finish       (big)delta  atLeast
  protected void drawSmallYTics(
      Graphics g, double xp, double yu, double ytest, double del, double atLeast) {
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
    x0 = graph_.getLayer().getXPtoD(xp0);
    x1 = graph_.getLayer().getXPtoD(xp1);
    smdel = del / (numSmallTics_ + 1);
    for (i = 0; i <= numSmallTics_; i++) {
      yt = yu + smdel * i;
      if (yt >= atLeast && (ytest - yt) / del >= 0) {
        y0 = graph_.getYUtoD(yt);
        g.drawLine(x0, y0, x1, y0);
      }
    }
  }

  /** Default constructor for SpaceAxis. */
  public SpaceAxis() {
    this("");
  }

  /**
   * Constructor for Axis. Sets the axis identifier and initializes the defaults.
   *
   * @param id axis identification
   */
  public SpaceAxis(String id) {
    super(id);
    space_ = true;
    numSmallTics_ = 0;
  }

  /**
   * Set the number of significant digits in the label. This is used if a format is not specified.
   *
   * @param nsig number of significant digits
   */
  public void setSignificantDigits(int nsig) {
    if (sigDigits_ != nsig) {
      sigDigits_ = nsig;
      modified("SpaceAxis: setSignificantDigits()");
    }
  }

  /**
   * Get the number of significant digits in the label.
   *
   * @return number of significant digits.
   */
  public int getSignificantDigits() {
    return sigDigits_;
  }

  /**
   * Set the label interval.
   *
   * @param lint label interval.
   */
  public void setLabelInterval(int lint) {
    if (labelInterval_ != lint) {
      labelInterval_ = lint;
      modified("SpaceAxis: setLabelInterval()");
    }
  }

  /**
   * Get the label interval.
   *
   * @return label interval
   */
  public int getLabelInterval() {
    return labelInterval_;
  }

  /**
   * Set the label format. Format should be in the sprintf style. The formating uses the Format
   * class in the Core Java book. A null or empty string will cause formating to use the significant
   * digits.
   *
   * <PRE>
   * Gary Cornell and Cay S. Horstmann, Core Java (Book/CD-ROM)
   * Published By SunSoft Press/Prentice-Hall
   * Copyright (C) 1996 Sun Microsystems Inc.
   * All Rights Reserved. ISBN 0-13-596891-7
   * </PRE>
   *
   * @param frmt label format.
   */
  public void setLabelFormat(String frmt) {
    if (labelFormat_ == null || !labelFormat_.equals(frmt)) {
      if (frmt == null) {
        labelFormat_ = "";
      } else {
        labelFormat_ = frmt;
      }
      modified("SpaceAxis: setLabelFormat()");
    }
  }

  /**
   * Get the label format.
   *
   * @return label format
   */
  public String getLabelFormat() {
    return labelFormat_;
  }

  /**
   * Set the user range to draw the axis. Registered Axes and AxisTransforms will be updated.
   *
   * @param ur range in user coordinates
   */
  public void setRangeU(Range2D ur) {
    if (uRange_ == null || !uRange_.equals(ur)) {
      uRange_ = ur;
      updateRegisteredAxes();
      updateRegisteredTransforms();
      modified("SpaceAxis: setRangeU()");
    }
  }

  @Override
  public void setRangeU(SoTRange ur) {
    setRangeU(
        new Range2D(
            ((SoTRange.Double) ur).start,
            ((SoTRange.Double) ur).end,
            ((SoTRange.Double) ur).delta));
  }

  /**
   * Get the user range.
   *
   * @return range in user coordinates
   */
  public Range2D getRangeU() {
    return uRange_;
  }

  @Override
  public SoTRange getSoTRangeU() {
    return new SoTRange.Double(uRange_);
  }

  /**
   * Set the increment between large tics.
   *
   * @param delta increment in user coordinates
   */
  public void setDeltaU(double delta) {
    if (uRange_.delta != delta) {
      uRange_.delta = delta;
      modified("SpaceAxis: setDeltaU()");
    }
  }

  /**
   * Get the increment between large tics.
   *
   * @return user coordinate increment
   */
  public double getDeltaU() {
    return uRange_.delta;
  }

  /**
   * Set the origin in user units of the axis.
   *
   * @param upt origin in user units
   */
  public void setLocationU(TimePoint uptt) {
    if (tLocation_ == null || !tLocation_.equals(uptt)) {
      tLocation_ = uptt;
      uLocation_ = null;
      modified("SpaceAxis: setLocationU(TimePoint)");
    }
  }

  /**
   * Set the origin in user units of the axis.
   *
   * @param upt origin in user units
   */
  public void setLocationU(Point2D.Double upt) {
    if (uLocation_ == null || !uLocation_.equals(upt)) {
      uLocation_ = upt;
      tLocation_ = null;
      modified("SpaceAxis: setLocationU(Point2D)");
    }
  }

  @Override
  public void setLocationU(SoTPoint upt) {
    double x;
    double y;
    if (upt.isXTime() || upt.isYTime()) {
      long t;
      if (upt.isXTime()) {
        t = upt.getX().getLongTime();
        x = ((SoTValue.Double) upt.getY()).getValue();
      } else {
        t = upt.getY().getLongTime();
        x = ((SoTValue.Double) upt.getX()).getValue();
      }
      setLocationU(new TimePoint(x, new GeoDate(t)));
    } else {
      x = ((SoTValue.Double) upt.getX()).getValue();
      y = ((SoTValue.Double) upt.getY()).getValue();
      setLocationU(new Point2D.Double(x, y));
    }
  }

  /**
   * Get the origin in user units of the axis
   *
   * @return origin
   */
  public Point2D.Double getLocationU() {
    return uLocation_;
  }

  /**
   * Get the origin in user units of the axis
   *
   * @return origin
   */
  public TimePoint getTimeLocationU() {
    return tLocation_;
  }

  @Override
  public SoTPoint getSoTLocationU() {
    if (tLocation_ == null) {
      return new SoTPoint(uLocation_.x, uLocation_.y);
    } else {
      if (orientation_ == HORIZONTAL) {
        return new SoTPoint(tLocation_.t, tLocation_.x);
      } else {
        return new SoTPoint(tLocation_.x, tLocation_.t);
      }
    }
  }

  //
  @Override
  public abstract void draw(Graphics g);

  /**
   * Get the bounding box for the axis in device units.
   *
   * @return bounding box
   * @see Rectangle
   */
  @Override
  public abstract Rectangle getBounds();

  @Override
  public void modified(String mess) {
    //    if(Debug.EVENT) System.out.println("SpaceAxis: modified()");
    if (graph_ != null) graph_.modified(mess);
  }
}
