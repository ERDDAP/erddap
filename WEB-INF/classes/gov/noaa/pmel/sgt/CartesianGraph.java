/*
 * $Id: CartesianGraph.java,v 1.20 2003/08/22 23:02:31 dwd Exp $
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
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>CartesianGraph</code> provides the transformation from user to physical coordinates. A
 * Cartesian graph has horizontal and vertical transforms, from user to physical coordinates, that
 * are independent. For example, yp = f(yu) and xp = g(xu), where f() and g() are the vertical and
 * horizontal transformations. Multiple horizontal and vertical, X and Y, axes can be associated
 * with a <code>CartesianGraph</code> and their mapping of user to physical coordinates is based on
 * the <code>AxisTransform</code>s used. The <code>CartesianGraph</code> also provide the support
 * for the rendering of data. The specific renderer is chosen based on the type of <code>SGTData
 * </code> and the data <code>Attribute</code> used.
 *
 * <p>The following demonstrates how a <code>CartesianGraph</code> may be used.
 *
 * <pre>
 *   // Create a CartesianGraph and transforms.
 *
 *    CartesianGraph graph;
 *    LinearTransform xt, yt;
 *    Range2D xPhysRange, xUserRange;
 *    Range2D yPhysRange, yUserRange;
 *    Point2D.Double origin;
 *
 *    graph = new CartesianGraph("Point Graph");
 *    layer.setGraph(graph);
 *    xt = new LinearTransform(xPhysRange, xUserRange);
 *    yt = new LinearTransform(yPhysRange, yUserRange);
 *    graph.setXTransform(xt);
 *    graph.setYTransform(yt);
 *    origin = new Point2D.Double(xUserRange.start,
 *                                yUserRange.start);
 *
 *     // Create the bottom axis, set its range in user units
 *     // and its origin. Add the axis to the graph.
 *
 *    PlainAxis xbot;
 *
 *    xbot = new PlainAxis("Botton Axis");
 *    xbot.setRangeU(xUserRange);
 *    xbot.setLocationU(origin);
 *    graph.addXAxis(xbot);
 *
 *     // Create the left axis, set its range in user units
 *     // and its origin. Add the axis to the graph.
 *
 *    PlainAxis yleft;
 *
 *    yleft = new PlainAxis("Left Axis");
 *    yleft.setRangeU(yUserRange);
 *    yleft.setLocationU(origin);
 *    graph.addYAxis(yleft);
 *
 *     // Create a PointAttribute for the display of the
 *     // Collection of points. The points will be marked
 *     // with a red triangle and labelled at the NE corner
 *     // in blue.
 *
 *    PointAttribute pattr;
 *
 *    pattr = new PointAttribute(10, Color.red);
 *
 *     // Associate the attribute and the point Collection
 *     // with the graph.
 *
 *    graph.setData(col, pattr);
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.20 $, $Date: 2003/08/22 23:02:31 $
 * @since 1.0
 */
public class CartesianGraph extends Graph {
  /**
   * @associates <strong>Axis</strong>
   * @supplierCardinality 0..*
   * @clientRole xAxis_
   */
  public List<Axis> xAxis_;

  /**
   * @associates <strong>Axis</strong>
   * @clientRole yAxis_
   * @supplierCardinality 0..*
   */
  public List<Axis> yAxis_;

  /**
   * @clientRole xTransform_
   * @link aggregation
   * @undirected
   */
  public AxisTransform xTransform_;

  /**
   * @clientRole tTransfrom_
   * @link aggregation
   * @undirected
   */
  public AxisTransform yTransform_;

  public boolean clipping_ = false;
  public Range2D xClipRange_;
  public Range2D yClipRange_;
  public SoTRange.Time tClipRange_;

  /**
   * @shapeType AggregationLink
   * @undirected
   * @clientCardinality 1
   * @supplierCardinality 1
   * @label renderer
   */
  private CartesianRenderer renderer_;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      if (xAxis_ != null) {
        List<Axis> v = xAxis_;
        xAxis_ = null;
        for (Axis o : v) o.releaseResources();
        v.clear();
      }
      if (yAxis_ != null) {
        List<Axis> v = yAxis_;
        yAxis_ = null;
        for (Axis o : v) o.releaseResources();
        v.clear();
      }
      if (xTransform_ != null) {
        AxisTransform o = xTransform_;
        xTransform_ = null;
        o.releaseResources();
      }
      if (yTransform_ != null) {
        AxisTransform o = yTransform_;
        yTransform_ = null;
        o.releaseResources();
      }
      xClipRange_ = null;
      yClipRange_ = null;
      tClipRange_ = null;
      layer_ = null; // from Graph;   not releaseResoureces, else infiniteLoop
      if (renderer_ != null) {
        CartesianRenderer o = renderer_; // done this way to avoid infinite loop
        renderer_ = null;
        o.releaseResources();
      }
      if (JPane.debug) String2.log("sgt.CartesianGraph.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public CartesianGraph() {
    this("");
  }

  /**
   * <code>CartesianGraph</code> constructor. Creates default unity transforms.
   *
   * @param id CartesianGraph identifier
   */
  public CartesianGraph(String id) {
    this(id, new LinearTransform(0.0, 1.0, 0.0, 1.0), new LinearTransform(0.0, 1.0, 0.0, 1.0));
  }

  /**
   * Create a new <code>CartesianGraph</code>. Sets the identifier and sets the x and y transforms.
   *
   * @param id identifier
   * @param xt x transform
   * @param yt y transform
   */
  public CartesianGraph(String id, AxisTransform xt, AxisTransform yt) {
    super(id);
    xAxis_ = new ArrayList<>();
    yAxis_ = new ArrayList<>();
    xTransform_ = xt;
    if (xTransform_ != null) xTransform_.addPropertyChangeListener(this);
    yTransform_ = yt;
    if (yTransform_ != null) yTransform_.addPropertyChangeListener(this);
  }

  /**
   * Associates <code>SGTData</code> and <code>Attribute</code> with the <code>CartesianGraph</code>
   * . A renderer is constucted based on the two arguements.
   *
   * <p>
   *
   * <TABLE style=\"border:1px; padding:2px; background-color:white;">
   *    <TR>
   *            <TH WIDTH="25%" BGCOLOR="#FFFFCC">
   *                    <P>SGTData
   *            </TH>
   *            <TH WIDTH="25%" BGCOLOR="#FFFFCC">
   *                    <P>Attribute
   *            </TH>
   *            <TH WIDTH="50%" BGCOLOR="#FFFFCC">
   *                    <P>CartesianRenderer
   *            </TH>
   *    </TR>
   *    <TR>
   *            <TD WIDTH="25%">SGTPoint</TD>
   *            <TD WIDTH="25%">PontAttribute</TD>
   *            <TD WIDTH="50%">PointCartesianRenderer</TD>
   *    </TR>
   *    <TR>
   *            <TD WIDTH="25%">SGTLine</TD>
   *            <TD WIDTH="25%">LineAttribute</TD>
   *            <TD WIDTH="50%">LineCartesianRenderer</TD>
   *    </TR>
   *    <TR>
   *            <TD WIDTH="25%">SGTGrid</TD>
   *            <TD WIDTH="25%">GridAttribute</TD>
   *            <TD WIDTH="50%">GridCartesianRenderer</TD>
   *    </TR>
   *    <TR>
   *            <TD WIDTH="25%">SGTVector</TD>
   *            <TD WIDTH="25%">VectorAttribute</TD>
   *            <TD WIDTH="50%">VectorCartesianRenderer</TD>
   *    </TR>
   *    <TR>
   *            <TD WIDTH="25%">Collection</TD>
   *            <TD WIDTH="25%">PointAttribute</TD>
   *            <TD WIDTH="50%">PointCartesianRenderer</TD>
   *    </TR>
   *    <TR>
   *            <TD WIDTH="25%">Collection</TD>
   *            <TD WIDTH="25%">LineAttribute</TD>
   *            <TD WIDTH="50%">LineCartesianRenderer</TD>
   *    </TR>
   *    <TR>
   *            <TD WIDTH="25%">Collection</TD>
   *            <TD WIDTH="25%">VectorAttribute</TD>
   *            <TD WIDTH="50%">VectorCartesianRenderer</TD>
   *    </TR>
   *    <TR>
   *            <TD WIDTH="25%">Annotation</TD>
   *            <TD WIDTH="25%">n/a</TD>
   *            <TD WIDTH="50%">AnnotationCartesianRenderer</TD>
   *    </TR>
   * </TABLE>
   *
   * <p>
   *
   * @param data data to be rendered
   * @param attr rendering style information
   * @see CartesianRenderer#getRenderer
   */
  public void setData(SGTData data, Attribute attr) {
    renderer_ = CartesianRenderer.getRenderer(this, data, attr);
    if (data != null) data.addPropertyChangeListener(this);
  }

  /**
   * Get the renderer instance being used by the graph.
   *
   * @return renderer
   */
  public CartesianRenderer getRenderer() {
    return renderer_;
  }

  /**
   * Set the renderer used by the graph.
   *
   * @param rend a renderer object
   */
  public void setRenderer(CartesianRenderer rend) {
    renderer_ = rend;
  }

  /**
   * Draw the graph, axes, and render the data. This method should not be directly called.
   *
   * @see Pane#draw
   */
  @Override
  public void draw(Graphics g) {
    // long time = System.currentTimeMillis();
    if (renderer_ != null) renderer_.draw(g);
    // com.cohort.util.String2.log("CartesianGraph.draw renderer time=" +
    // (System.currentTimeMillis() - time) + "ms");
    if (!xAxis_.isEmpty()) {
      for (Axis axis : xAxis_) {
        // time = System.currentTimeMillis();
        axis.draw(g);
        // com.cohort.util.String2.log("CartesianGraph.draw xaxis time=" +
        // (System.currentTimeMillis() - time) + "ms");
      }
    }
    if (!yAxis_.isEmpty()) {
      for (Axis axis : yAxis_) {
        // time = System.currentTimeMillis();
        axis.draw(g);
        // com.cohort.util.String2.log("CartesianGraph.draw yaxis time=" +
        // (System.currentTimeMillis() - time) + "ms");
      }
    }
    // com.cohort.util.String2.log("CartesianGraph.draw time=" + (System.currentTimeMillis() - time)
    // + "ms");
  }

  /**
   * Set the clipping rectangle in user coordinates.
   *
   * @param xmin minimum horizontal coordinate
   * @param xmax maximum horizontal coordinate
   * @param ymin minimum vertical coordinate
   * @param ymax maximum vertical coordinate
   */
  public void setClip(double xmin, double xmax, double ymin, double ymax) {
    if (xTransform_.isSpace() && yTransform_.isSpace()) {
      clipping_ = true;
      xClipRange_ = new Range2D(xmin, xmax);
      yClipRange_ = new Range2D(ymin, ymax);
    } else {
      clipping_ = false;
    }
  }

  /**
   * Set the clipping rectangle in user coordinates.
   *
   * @param tmin mimimum time
   * @param tmax maximum time
   * @param min miminum user coordinate
   * @param max maximum user coordinate
   */
  public void setClip(GeoDate tmin, GeoDate tmax, double min, double max) {
    if (xTransform_.isTime() || yTransform_.isTime()) {
      clipping_ = true;
      tClipRange_ = new SoTRange.Time(tmin.getTime(), tmax.getTime());
      if (xTransform_.isTime()) {
        yClipRange_ = new Range2D(min, max);
      } else {
        xClipRange_ = new Range2D(min, max);
      }
    } else {
      clipping_ = false;
    }
  }

  /**
   * Set the clipping property.
   *
   * @param clip clipping
   */
  public void setClipping(boolean clip) {
    clipping_ = clip;
  }

  /**
   * Add a X axis (<code>Axis.HORIZONTAL</code>) to the graph. Uses the existing axis identifier.
   *
   * @param axis X axis
   * @see Axis
   * @see PlainAxis
   */
  public void addXAxis(Axis axis) {
    axis.setOrientation(Axis.HORIZONTAL);
    axis.setGraph(this);
    xAxis_.add(axis);
  }

  /**
   * Add a Y axis (<code>Axis.VERTICAL</code>) to the graph. Uses the existing axis identifier.
   *
   * @param axis Y axis
   * @see Axis
   * @see PlainAxis
   */
  public void addYAxis(Axis axis) {
    axis.setOrientation(Axis.VERTICAL);
    axis.setGraph(this);
    yAxis_.add(axis);
  }

  /**
   * Get the current X <code>AxisTransform</code>.
   *
   * @return X Transform
   * @see AxisTransform
   * @see LinearTransform
   */
  public AxisTransform getXTransform() {
    return xTransform_;
  }

  /**
   * Get the current Y <code>AxisTransform</code>.
   *
   * @return Y Transform
   * @see AxisTransform
   * @see LinearTransform
   */
  public AxisTransform getYTransform() {
    return yTransform_;
  }

  //
  @Override
  Object getObjectAt(Point pt) {
    Rectangle bnds;
    SGLabel lab;
    if (!xAxis_.isEmpty()) {
      for (Axis ax : xAxis_) {
        bnds = ax.getBounds();
        if (bnds.contains(pt)) {
          return ax;
        }
        lab = ax.getTitle();
        if ((lab != null) && (lab.getLayer() != null)) {
          bnds = lab.getBounds();
          if (bnds.contains(pt)) {
            return lab;
          }
        }
      }
    }
    if (!yAxis_.isEmpty()) {
      for (Axis ax : yAxis_) {
        bnds = ax.getBounds();
        if (bnds.contains(pt)) {
          return ax;
        }
        lab = ax.getTitle();
        if (lab != null) {
          bnds = lab.getBounds();
          if (bnds.contains(pt)) {
            return lab;
          }
        }
      }
    }
    return null;
  }

  /**
   * Transform user X coordinate to physical coordinate.
   *
   * @since 2.0
   */
  public double getXUtoP(double u) {
    return xTransform_.getTransP(u);
  }

  /**
   * Transform user X coordinate to device coordinate.
   *
   * @since 2.0
   */
  public int getXUtoD(double u) {
    if (Double.isNaN(u)) return Integer.MIN_VALUE;
    return getLayer().getXPtoD(xTransform_.getTransP(u));
  }

  /**
   * Transform <code>GeoDate</code> to physical coordinate.
   *
   * @since 2.0
   */
  public double getXUtoP(GeoDate t) {
    return xTransform_.getTransP(t);
  }

  /**
   * Transform <code>GeoDate</code> to device coordinate.
   *
   * @since 2.0
   */
  public int getXUtoD(GeoDate t) {
    if (t == null) return Integer.MIN_VALUE;
    return getLayer().getXPtoD(xTransform_.getTransP(t));
  }

  /**
   * Transform <code>long</code> to device coordinate.
   *
   * @since 3.0
   */
  public int getXUtoD(long t) {
    if (t == Long.MAX_VALUE) return Integer.MIN_VALUE;
    return getLayer().getXPtoD(xTransform_.getTransP(t));
  }

  /**
   * Transoform user Y coordinate to physical coordinate.
   *
   * @since 2.0
   */
  public double getYUtoP(double u) {
    return yTransform_.getTransP(u);
  }

  /**
   * Transform user Y coordinate to device coordinate
   *
   * @since 2.0
   */
  public int getYUtoD(double u) {
    if (Double.isNaN(u)) return Integer.MIN_VALUE;
    return getLayer().getYPtoD(yTransform_.getTransP(u));
  }

  /**
   * Transform time to physical coordinate.
   *
   * @since 2.0
   */
  public double getYUtoP(GeoDate t) {
    return yTransform_.getTransP(t);
  }

  /**
   * Transform time to device coordinate.
   *
   * @since 2.0
   */
  public int getYUtoD(GeoDate t) {
    if (t == null) return Integer.MIN_VALUE;
    return getLayer().getYPtoD(yTransform_.getTransP(t));
  }

  /**
   * Transform time to device coordinate.
   *
   * @since 3.0
   */
  public int getYUtoD(long t) {
    if (t == Long.MAX_VALUE) return Integer.MIN_VALUE;
    return getLayer().getYPtoD(yTransform_.getTransP(t));
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    //      if(Debug.EVENT) {
    //        System.out.println("CartesianGraph: " + evt);
    //        System.out.println("                " + evt.getPropertyName());
    //      }
    modified(
        "CartesianGraph: propertyChange("
            + evt.getSource().toString()
            + "["
            + evt.getPropertyName()
            + "]"
            + ")");
  }
}
