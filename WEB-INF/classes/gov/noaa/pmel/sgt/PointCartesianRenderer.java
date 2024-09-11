/*
 * $Id: PointCartesianRenderer.java,v 1.16 2003/08/22 23:02:32 dwd Exp $
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
import gov.noaa.pmel.sgt.dm.Collection;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTPoint;
import gov.noaa.pmel.util.Point2D;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;

/**
 * Produces a point plot with optional coloring from a second data set. If a second data set is
 * specified it must have the same shape as the first.
 *
 * @author Donald Denbo
 * @version $Revision: 1.16 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.0
 */
public class PointCartesianRenderer extends CartesianRenderer {
  /**
   * @shapeType AggregationLink
   * @label attr
   * @supplierCardinality 1
   * @undirected
   */
  private PointAttribute attr_ = null;

  /**
   * @shapeType AggregationLink
   * @supplierCardinality 0..1
   * @label point
   * @undirected
   */
  private SGTPoint point_ = null;

  /**
   * @shapeType AggregationLink
   * @supplierCardinality 0..1
   */
  private Collection collection_ = null;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      attr_ = null;
      if (point_ != null) {
        SGTPoint o = point_; // done this way to avoid infinite loop
        point_ = null;
        o.releaseResources();
      }
      if (collection_ != null) {
        collection_.clear();
        collection_ = null;
      }
      if (JPane.debug) String2.log("sgt.PointCartesianRenderer.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Get the Attribute associated with the data. */
  @Override
  public Attribute getAttribute() {
    return attr_;
  }

  private void drawPoint(Graphics g, SGTPoint point, PlotMark pm) {
    int xp, yp;

    if (pm.getMark() == 0) return;

    if (point.isXTime()) {
      xp = cg_.getXUtoD(point.getLongTime());
    } else {
      xp = cg_.getXUtoD(point.getX());
    }
    if (point.isYTime()) {
      yp = cg_.getYUtoD(point.getLongTime());
    } else {
      yp = cg_.getYUtoD(point.getY());
    }
    //
    // check for missing values a Double.NaN is converted to a Integer.MIN_VALUE
    //
    if (xp == Integer.MIN_VALUE || yp == Integer.MIN_VALUE) {
      return;
    }
    //
    // draw regular point
    //
    pm.paintMark(g, cg_.getLayer(), xp, yp);
  }

  private void drawLabel(Graphics g, SGTPoint point, PointAttribute attr) {
    int valign, halign;
    Point2D.Double loc;
    double xp, yp;
    double xl, yl, loff;
    Layer ly = cg_.getLayer();

    if (point.isXTime()) {
      xp = cg_.getXUtoP(point.getLongTime());
    } else {
      xp = cg_.getXUtoP(point.getX());
    }
    if (point.isYTime()) {
      yp = cg_.getYUtoP(point.getLongTime());
    } else {
      yp = cg_.getYUtoP(point.getY());
    }

    loff = attr.getMarkHeightP() / 2.0;
    xl = 0.0;
    yl = 0.0;
    switch (attr.getLabelPosition()) {
      case PointAttribute.CENTERED:
        valign = SGLabel.MIDDLE;
        halign = SGLabel.CENTER;
        break;
      case PointAttribute.N:
        valign = SGLabel.BOTTOM;
        halign = SGLabel.CENTER;
        yl = loff;
        break;
      default:
      case PointAttribute.NE:
        valign = SGLabel.BOTTOM;
        halign = SGLabel.LEFT;
        yl = loff;
        xl = loff;
        break;
      case PointAttribute.E:
        valign = SGLabel.MIDDLE;
        halign = SGLabel.LEFT;
        xl = loff;
        break;
      case PointAttribute.SE:
        valign = SGLabel.TOP;
        halign = SGLabel.LEFT;
        yl = -loff;
        xl = loff;
        break;
      case PointAttribute.S:
        valign = SGLabel.TOP;
        halign = SGLabel.CENTER;
        yl = -loff;
        break;
      case PointAttribute.SW:
        valign = SGLabel.TOP;
        halign = SGLabel.RIGHT;
        yl = -loff;
        xl = -loff;
        break;
      case PointAttribute.W:
        valign = SGLabel.MIDDLE;
        halign = SGLabel.RIGHT;
        xl = -loff;
        break;
      case PointAttribute.NW:
        valign = SGLabel.BOTTOM;
        halign = SGLabel.RIGHT;
        yl = loff;
        xl = -loff;
        break;
    }
    SGLabel pl =
        new SGLabel(
            "point",
            point.getTitle(),
            attr.getLabelHeightP(),
            new Point2D.Double(xp + xl, yp + yl),
            valign,
            halign);
    pl.setColor(attr.getLabelColor());
    pl.setFont(attr.getLabelFont());
    pl.setLayer(ly);
    try {
      pl.draw(g);
    } catch (LayerNotFoundException e) {
    }
  }

  /**
   * Default constructor.
   *
   * @see CartesianGraph
   * @see Graph
   */
  public PointCartesianRenderer(CartesianGraph cg) {
    this(cg, (SGTPoint) null, null);
  }

  /**
   * Construct a PointCartesianRenderer. The default PointAttribute will be used.
   *
   * @param cg the parent CartesianGraph
   * @param data an SGTPoint object
   * @see CartesianGraph
   * @see Graph
   */
  public PointCartesianRenderer(CartesianGraph cg, SGTPoint point) {
    this(cg, point, null);
    cg_ = cg;
    point_ = point;
  }

  /**
   * Construct a PointCartesianRenderer.
   *
   * @param cg the parent CartesianGraph
   * @param data an SGTPoint object
   * @param Point the PointAttribute
   * @see CartesianGraph
   * @see Graph
   */
  public PointCartesianRenderer(CartesianGraph cg, SGTPoint point, PointAttribute attr) {
    cg_ = cg;
    point_ = point;
    attr_ = attr;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
  }

  /**
   * Construct a PointCartesianRenderer.
   *
   * @param cg the parent CartesianGraph
   * @param col a Collection of SGTPoint objects
   * @param point the PointAttribute
   * @see CartesianGraph
   * @see Graph
   */
  public PointCartesianRenderer(CartesianGraph cg, Collection col, PointAttribute attr) {
    cg_ = cg;
    collection_ = col;
    attr_ = attr;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
  }

  @Override
  public void draw(Graphics g) {
    PointAttribute attr;
    Object point;
    PlotMark pm;

    if (cg_.clipping_) {
      int xmin, xmax, ymin, ymax;
      int x, y, width, height;
      if (cg_.xTransform_.isSpace()) {
        xmin = cg_.getXUtoD(cg_.xClipRange_.start);
        xmax = cg_.getXUtoD(cg_.xClipRange_.end);
      } else {
        xmin = cg_.getXUtoD(cg_.tClipRange_.start);
        xmax = cg_.getXUtoD(cg_.tClipRange_.end);
      }
      if (cg_.yTransform_.isSpace()) {
        ymin = cg_.getYUtoD(cg_.yClipRange_.start);
        ymax = cg_.getYUtoD(cg_.yClipRange_.end);
      } else {
        ymin = cg_.getYUtoD(cg_.tClipRange_.start);
        ymax = cg_.getYUtoD(cg_.tClipRange_.end);
      }
      if (xmin < xmax) {
        x = xmin;
        width = xmax - xmin;
      } else {
        x = xmax;
        width = xmin - xmax;
      }
      if (ymin < ymax) {
        y = ymin;
        height = ymax - ymin;
      } else {
        y = ymax;
        height = ymin - ymax;
      }
      g.setClip(x, y, width, height);
    }
    if (attr_ == null) {
      attr = new PointAttribute(2, cg_.layer_.getPane().getComponent().getForeground());
    } else {
      attr = attr_;
    }
    pm = new PlotMark(attr);
    if (collection_ == null) {
      g.setColor(attr.getColor());
      drawPoint(g, point_, pm);
      if (attr.isDrawLabel()) drawLabel(g, point_, attr);
    } else {
      for (Enumeration li = collection_.elements(); li.hasMoreElements(); ) {
        point = li.nextElement();
        if (point instanceof SGTPoint) {
          g.setColor(attr.getColor());
          drawPoint(g, (SGTPoint) point, pm);
          if (attr.isDrawLabel()) drawLabel(g, (SGTPoint) point, attr);
        }
      }
    }

    //
    // reset clip
    //
    Rectangle rect = cg_.getLayer().getPane().getBounds();
    g.setClip(rect);
  }

  /**
   * Set the Point attribute object. The Point appearance is controlled by this object.
   *
   * @param l Point attribute
   */
  public void setPointAttribute(PointAttribute l) {
    if (attr_ != null) attr_.removePropertyChangeListener(this);
    attr_ = l;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
  }

  /**
   * Get the Point attribute object.
   *
   * @return Point attribute
   */
  public PointAttribute getPointAttribute() {
    return attr_;
  }

  /** Is point data as a collection? */
  public boolean hasCollection() {
    return (collection_ != null);
  }

  /** Get the <code>Collection</code> of points. */
  public Collection getCollection() {
    return collection_;
  }

  public SGTPoint getPoint() {
    return point_;
  }

  /**
   * Get the associated <code>CartesianGraph</code> object.
   *
   * @return <code>CartesianGraph</code>
   */
  @Override
  public CartesianGraph getCartesianGraph() {
    return cg_;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    //      if(Debug.EVENT) {
    //        System.out.println("PointCartesianRenderer: " + evt);
    //        System.out.println("                        " + evt.getPropertyName());
    //      }
    modified(
        "PointCartesianRenderer: propertyChange("
            + evt.getSource().toString()
            + "["
            + evt.getPropertyName()
            + "]"
            + ")");
  }

  /**
   * @since 3.0
   */
  @Override
  public SGTData getDataAt(Point pt) {
    return null;
  }
}
