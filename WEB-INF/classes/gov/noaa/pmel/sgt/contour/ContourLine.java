/*
 * $Id: ContourLine.java,v 1.16 2002/06/24 23:19:21 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.contour;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.ContourLineAttribute;
import gov.noaa.pmel.sgt.DefaultContourLineAttribute;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.LayerNotFoundException;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.StrokeDrawer;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Point2D;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <code>ContourLine</code> contains a single closed or open contour and a list of its labels. The
 * level, closedness, and path are properties. The path is stored as a <code>Vector</code> of <code>
 * Point2D</code> user coordinate values. <code>ContourLine</code> objects are created by <code>
 * Contour</code> and drawn by <code>GridCartesianRenderer</code>. Time coordinates are stored in
 * the <code>Point2D</code> objects releative to a reference time.
 *
 * @author D. W. Denbo
 * @version $Revision: 1.16 $, $Date: 2002/06/24 23:19:21 $
 * @since 2.0
 * @see Contour
 * @see ContourLineAttribute
 * @see DefaultContourLineAttribute
 * @see gov.noaa.pmel.sgt.GridCartesianRenderer
 */
public class ContourLine extends Vector {
  private StrokeDrawer stroke_ = null;

  private boolean closed_ = false;
  private double level_;
  private GeoDate timeRef_ = null;
  private boolean xTime_ = false;
  private boolean yTime_ = false;
  private int kmax_;

  /**
   * @label attr
   */
  private ContourLineAttribute attr_ = null;

  private DefaultContourLineAttribute defaultAttr_ = null;
  private Vector conLabels_;

  /**
   * @supplierCardinality *
   * @link aggregationByValue
   */
  private ContourLabel lnkContourLabel;

  private CartesianGraph cg_;

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception {
    try {
      stroke_ = null;
      attr_ = null;
      defaultAttr_ = null;
      if (conLabels_ != null) {
        Vector o = conLabels_;
        conLabels_ = null;
        o.clear();
      }
      lnkContourLabel = null;
      cg_ = null; // not releaseResources() else infinite loop
      if (JPane.debug) String2.log("sgt.contour.ContourLine.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public ContourLine() {
    super();
    conLabels_ = new Vector();
    stroke_ = JPane.getStrokeDrawer();
  }

  /** Constructor setting initial size and extend values of coordinate <code>Vector</code>. */
  public ContourLine(int size, int extend) {
    super(size, extend);
    conLabels_ = new Vector();
    stroke_ = JPane.getStrokeDrawer();
  }

  /** Constructor setting initial size of coordinate <code>Vector</code>. */
  public ContourLine(int size) {
    super(size);
    conLabels_ = new Vector();
    stroke_ = JPane.getStrokeDrawer();
  }

  /** Set the parent <code>CartesianGraph</code>. Used internally by sgt. */
  public void setCartesianGraph(CartesianGraph cg) {
    cg_ = cg;
  }

  /** Set the attributes for the contour line. */
  public void setAttributes(DefaultContourLineAttribute def, ContourLineAttribute attr) {
    defaultAttr_ = def;
    attr_ = attr;
  }

  /** Set the <code>ContourLineAttribute</code> */
  public void setContourLineAttribute(ContourLineAttribute attr) {
    attr_ = attr;
  }

  /** Get the <code>ContourLineAttribute</code> */
  public ContourLineAttribute getContourLineAttribute() {
    return attr_;
  }

  /** Set the <code>DefaultContourLineAttribute</code> */
  public void setDefaultContourLineAttribute(DefaultContourLineAttribute def) {
    defaultAttr_ = def;
  }

  /** Get the <code>DefaultContourLineAttribute</code> */
  public DefaultContourLineAttribute getDefaultContourLineAttribute() {
    return defaultAttr_;
  }

  void setClosed(boolean closed) {
    closed_ = closed;
  }

  /** Is the contour line closed? */
  public boolean isClosed() {
    return closed_;
  }

  /** The level value of the contour line. */
  public double getLevel() {
    return level_;
  }

  void setLevel(double level) {
    level_ = level;
  }

  void addPoint(double x, double y) {
    addElement(new Point2D.Double(x, y));
  }

  void addPoint(Point2D.Double point) {
    addElement(point);
  }

  void setKmax(int kmax) {
    kmax_ = kmax;
  }

  /** Get the number of points in the contour line. */
  public int getKmax() {
    return kmax_;
  }

  void setTime(GeoDate tref, boolean xtime, boolean ytime) {
    timeRef_ = tref;
    xTime_ = xtime;
    yTime_ = ytime;
  }

  void setReferenceTime(GeoDate tref) {
    timeRef_ = tref;
  }

  /** Get reference time for the time point of the <code>Point2D</code> object. */
  public GeoDate getReferenceTime() {
    return timeRef_;
  }

  void setXTime(boolean time) {
    xTime_ = time;
  }

  /** Is the x coordinate time? */
  public boolean isXTime() {
    return xTime_;
  }

  void setYTime(boolean time) {
    yTime_ = time;
  }

  /** Is the y coordinate time? */
  public boolean isYTime() {
    return yTime_;
  }

  void reverseElements(int k) {
    /* k is reversal length */
    Point2D.Double point;
    int kkr;
    int kh = 1 + k / 2;
    for (int kk = 1; kk < kh; kk++) {
      kkr = k + 1 - kk;
      point = (Point2D.Double) elementAt(kk);
      setElementAt(elementAt(kkr), kk);
      setElementAt(point, kkr);
    }
  }

  /** Add a label to the contour line. */
  public void addLabel(int point, SGLabel lab, double hgt, double wid) {
    ContourLabel clab = new ContourLabel(point, lab, hgt, wid);
    conLabels_.addElement(clab);
  }

  /** Remove all labels. */
  public void removeAllLabels() {
    conLabels_.removeAllElements();
  }

  /** Used internally by sgt. */
  public void draw(Graphics g) {
    int k, kk, loc, kp1;
    double[] x = new double[kmax_ + 1];
    double[] y = new double[kmax_ + 1];
    ContourLabel clab;
    double width, hhgt;
    double dxx, dyy, space;
    double xa, xb, ya, yb, aa, bb, cc, zxy;
    double xendl, yendl, angle;
    double xlab, ylab;
    SGLabel label;
    GeoDate time;
    defaultAttr_.setContourLineAttribute(attr_);
    Color lineColor = defaultAttr_.getColor();
    //
    // convert ContourLine to physical units
    //
    x = getXArrayP();
    y = getYArrayP();
    //
    // loop through labels
    //
    k = 1;
    Enumeration cenum = conLabels_.elements();
    while (cenum.hasMoreElements()) {
      clab = (ContourLabel) cenum.nextElement();
      loc = clab.getIndex();
      width = clab.getWidth();
      hhgt = clab.getHeight() * 0.5;
      label = clab.getLabel();
      //
      g.setColor(lineColor);
      drawLine(g, x, y, k, loc);
      //
      k = loc;
      kp1 = k + 1;
      for (kk = kp1; kk <= kmax_; kk++) {
        dxx = x[kk] - x[k];
        dyy = y[kk] - y[k];
        space = Math.sqrt(dxx * dxx + dyy * dyy);
        if (space >= width) break;
      }
      xa = x[kk - 1] - x[k];
      xb = x[kk] - x[kk - 1];
      ya = y[kk - 1] - y[k];
      yb = y[kk] - y[kk - 1];
      aa = xb * xb + yb * yb;
      bb = xa * xb + ya * yb;
      cc = xa * xa + ya * ya - width * width;
      zxy = (-bb + Math.sqrt(bb * bb - aa * cc)) / aa;
      dxx = xa + xb * zxy;
      dyy = ya + yb * zxy;
      xendl = x[k] + dxx;
      yendl = y[k] + dyy;
      //
      // compute label angle
      //
      angle = 90.0;
      if (dyy < 0.0) angle = -90.0;
      if (dxx != 0.0) {
        angle = Math.atan(dyy / dxx) * 180.0 / Math.PI;
      }
      //
      // compute label position
      //
      if (dxx >= 0) {
        xlab = x[k] + hhgt * (0.5 * dxx + dyy) / width;
        ylab = y[k] + hhgt * (0.5 * dyy - dxx) / width;
      } else {
        xlab = xendl - hhgt * (0.5 * dxx + dyy) / width;
        ylab = yendl - hhgt * (0.5 * dyy - dxx) / width;
      }
      label.setAngle(angle);
      label.setLocationP(new Point2D.Double(xlab, ylab));
      try {
        label.draw(g);
      } catch (LayerNotFoundException e) {
        System.out.println(e);
      }
      g.setColor(lineColor);
      drawLineSegment(g, xendl, yendl, x[kk], y[kk]);
      k = kk;
    }
    if (k < kmax_) {
      g.setColor(lineColor);
      drawLine(g, x, y, k, kmax_);
    }
  }

  private void drawLine(Graphics g, double[] x, double[] y, int kstart, int kend) {
    int size = kend - kstart + 1;
    int[] xp = new int[size];
    int[] yp = new int[size];
    int i, k;
    for (i = 0, k = kstart; k <= kend; k++, i++) {
      xp[i] = cg_.getLayer().getXPtoD(x[k]);
      yp[i] = cg_.getLayer().getYPtoD(y[k]);
    }
    switch (defaultAttr_.getStyle()) {
      case LineAttribute.HIGHLIGHT:
        stroke_.drawHighlight(g, xp, yp, size, (LineAttribute) defaultAttr_);
        break;
      case LineAttribute.HEAVY:
        stroke_.drawHeavy(g, xp, yp, size, (LineAttribute) defaultAttr_);
        break;
      case LineAttribute.DASHED:
        stroke_.drawDashed(g, xp, yp, size, (LineAttribute) defaultAttr_);
        break;
      case LineAttribute.STROKE:
        stroke_.drawStroke(g, xp, yp, size, (LineAttribute) defaultAttr_);
        break;
      default:
      case LineAttribute.MARK:
      case LineAttribute.MARK_LINE:
      case LineAttribute.SOLID:
        g.drawPolyline(xp, yp, size);
    }
  }

  private void drawLineSegment(Graphics g, double x0, double y0, double x1, double y1) {
    int[] xp, yp;
    int size = 2;
    xp = new int[2];
    yp = new int[2];
    xp[0] = cg_.getLayer().getXPtoD(x0);
    yp[0] = cg_.getLayer().getYPtoD(y0);
    xp[1] = cg_.getLayer().getXPtoD(x1);
    yp[1] = cg_.getLayer().getYPtoD(y1);
    switch (defaultAttr_.getStyle()) {
      case LineAttribute.HIGHLIGHT:
        stroke_.drawHighlight(g, xp, yp, size, (LineAttribute) defaultAttr_);
        break;
      case LineAttribute.HEAVY:
        stroke_.drawHeavy(g, xp, yp, size, (LineAttribute) defaultAttr_);
        break;
      case LineAttribute.DASHED:
        stroke_.drawDashed(g, xp, yp, size, (LineAttribute) defaultAttr_);
        break;
      case LineAttribute.STROKE:
        stroke_.drawStroke(g, xp, yp, size, (LineAttribute) defaultAttr_);
        break;
      default:
      case LineAttribute.MARK:
      case LineAttribute.MARK_LINE:
      case LineAttribute.SOLID:
        g.drawPolyline(xp, yp, size);
    }
  }

  /** Get x physical coordinates of the contour line. */
  public double[] getXArrayP() {
    double[] xp = null;
    Point2D.Double pt;
    GeoDate time;
    if (cg_ != null) {
      xp = new double[kmax_ + 1];
      for (int k = 0; k <= kmax_; k++) {
        pt = (Point2D.Double) elementAt(k);
        if (isXTime()) {
          time = new GeoDate(timeRef_).increment(pt.x, GeoDate.DAYS);
          xp[k] = cg_.getXUtoP(time);
        } else {
          xp[k] = cg_.getXUtoP(pt.x);
        }
      }
    }
    return xp;
  }

  /** Get y physical coordinates of the contour line. */
  public double[] getYArrayP() {
    double[] yp = null;
    Point2D.Double pt;
    GeoDate time;
    if (cg_ != null) {
      yp = new double[kmax_ + 1];
      for (int k = 0; k <= kmax_; k++) {
        pt = (Point2D.Double) elementAt(k);
        if (isYTime()) {
          time = new GeoDate(timeRef_).increment(pt.x, GeoDate.DAYS);
          yp[k] = cg_.getYUtoP(time);
        } else {
          yp[k] = cg_.getYUtoP(pt.y);
        }
      }
    }
    return yp;
  }
}
