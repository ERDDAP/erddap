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
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTValue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;
import java.util.Vector;

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
  public Vector xAxis_;

  /**
   * @associates <strong>Axis</strong>
   * @clientRole yAxis_
   * @supplierCardinality 0..*
   */
  public Vector yAxis_;

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
        Vector v = xAxis_;
        xAxis_ = null;
        for (Object o : v) ((Axis) o).releaseResources();
        v.clear();
      }
      if (yAxis_ != null) {
        Vector v = yAxis_;
        yAxis_ = null;
        for (Object o : v) ((Axis) o).releaseResources();
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
    xAxis_ = new Vector(2, 2);
    yAxis_ = new Vector(2, 2);
    xTransform_ = xt;
    if (xTransform_ != null) xTransform_.addPropertyChangeListener(this);
    yTransform_ = yt;
    if (yTransform_ != null) yTransform_.addPropertyChangeListener(this);
  }

  /** Create a copy of the <code>CartesianGraph</code> */
  @Override
  public Graph copy() {
    throw new MethodNotImplementedError();
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
      for (Enumeration it = xAxis_.elements(); it.hasMoreElements(); ) {
        // time = System.currentTimeMillis();
        ((Axis) it.nextElement()).draw(g);
        // com.cohort.util.String2.log("CartesianGraph.draw xaxis time=" +
        // (System.currentTimeMillis() - time) + "ms");
      }
    }
    if (!yAxis_.isEmpty()) {
      for (Enumeration it = yAxis_.elements(); it.hasMoreElements(); ) {
        // time = System.currentTimeMillis();
        ((Axis) it.nextElement()).draw(g);
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
   * Set the clipping rectangle in user coordinates.
   *
   * @since 3.0
   * @param tmin mimimum time
   * @param tmax maximum time
   * @param min miminum user coordinate
   * @param max maximum user coordinate
   */
  public void setClip(long tmin, long tmax, double min, double max) {
    if (xTransform_.isTime() || yTransform_.isTime()) {
      clipping_ = true;
      tClipRange_ = new SoTRange.Time(tmin, tmax);
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
   * Set the clipping rectangle in user coordinates.
   *
   * @since 2.0
   */
  public void setClip(SoTRange xr, SoTRange yr) {
    if (xr.isTime() || yr.isTime()) {
      SoTRange.Double dub;
      long tstart;
      long tend;
      if (xr.isTime()) {
        tstart = xr.getStart().getLongTime();
        tend = xr.getEnd().getLongTime();
        dub = (SoTRange.Double) yr;
      } else {
        tstart = yr.getStart().getLongTime();
        tend = yr.getEnd().getLongTime();
        dub = (SoTRange.Double) xr;
      }
      setClip(tstart, tend, dub.start, dub.end);
    } else {
      SoTRange.Double xrd = (SoTRange.Double) xr;
      SoTRange.Double yrd = (SoTRange.Double) yr;
      setClip(xrd.start, xrd.end, yrd.start, yrd.end);
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
   * Test the clipping property.
   *
   * @return true if clipping is active
   */
  public boolean isClipping() {
    return clipping_;
  }

  /**
   * Add a X axis (<code>Axis.HORIZONTAL</code>) to the graph.
   *
   * @param id axis identifier
   * @param axis X axis
   * @see Axis
   * @see PlainAxis
   */
  public void addXAxis(String id, Axis axis) {
    if (id.length() != 0) axis.setId(id);
    addXAxis(axis);
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
    xAxis_.addElement(axis);
  }

  /**
   * Get a reference to an X axis.
   *
   * @param id axis identifier
   * @return axis found
   * @exception AxisNotFoundException An axis was not found with the correct identifier.
   * @see Axis
   * @see PlainAxis
   */
  public Axis getXAxis(String id) throws AxisNotFoundException {
    if (!xAxis_.isEmpty()) {
      Axis ax;
      for (Enumeration it = xAxis_.elements(); it.hasMoreElements(); ) {
        ax = (Axis) it.nextElement();
        if (ax.getId() == id) return ax;
      }
      throw new AxisNotFoundException();
    } else {
      throw new AxisNotFoundException();
    }
  }

  /**
   * Remove an X axis from the graph.
   *
   * @param id axis identifier
   * @exception AxisNotFoundException An axis was not found with the correct identifier.
   * @see Axis
   * @see PlainAxis
   */
  public void removeXAxis(String id) throws AxisNotFoundException {
    if (!xAxis_.isEmpty()) {
      Axis ax;
      for (Enumeration it = xAxis_.elements(); it.hasMoreElements(); ) {
        ax = (Axis) it.nextElement();
        if (ax.getId() == id) xAxis_.removeElement(ax);
      }
      throw new AxisNotFoundException();
    } else {
      throw new AxisNotFoundException();
    }
  }

  /**
   * Remove an X axis from the graph.
   *
   * @exception AxisNotFoundException An axis was not found with the correct identifier.
   * @see Axis
   * @see PlainAxis
   */
  public void removeXAxis(Axis axis) throws AxisNotFoundException {
    if (!xAxis_.isEmpty()) {
      Axis ax;
      for (Enumeration it = xAxis_.elements(); it.hasMoreElements(); ) {
        ax = (Axis) it.nextElement();
        if (ax.equals(axis)) xAxis_.removeElement(ax);
      }
      throw new AxisNotFoundException();
    } else {
      throw new AxisNotFoundException();
    }
  }

  /** Remove all X axes from the graph. */
  public void removeAllXAxes() {
    xAxis_.removeAllElements();
  }

  /**
   * Get the number of X axes associated with the graph.
   *
   * @return number of axes
   * @see Axis
   * @see PlainAxis
   */
  public int getNumberXAxis() {
    return xAxis_.size();
  }

  /**
   * Get an <code>Enumeration</code> object for the X axes.
   *
   * @return enumeration
   */
  public Enumeration xAxisElements() {
    return xAxis_.elements();
  }

  /**
   * Add a Y axis (<code>Axis.VERTICAL</code>) to the graph.
   *
   * @param id axis identifier
   * @param axis Y axis
   * @see Axis
   * @see PlainAxis
   */
  public void addYAxis(String id, Axis axis) {
    if (id.length() != 0) axis.setId(id);
    addYAxis(axis);
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
    yAxis_.addElement(axis);
  }

  /**
   * Get a reference to an Y axis.
   *
   * @param id axis identifier
   * @return axis found
   * @exception AxisNotFoundException An axis was not found with the correct identifier.
   * @see Axis
   * @see PlainAxis
   */
  public Axis getYAxis(String id) throws AxisNotFoundException {
    if (!yAxis_.isEmpty()) {
      Axis ax;
      for (Enumeration it = yAxis_.elements(); it.hasMoreElements(); ) {
        ax = (Axis) it.nextElement();
        if (ax.getId() == id) return ax;
      }
      throw new AxisNotFoundException();
    } else {
      throw new AxisNotFoundException();
    }
  }

  /**
   * Remove an Y axis from the graph.
   *
   * @param id axis identifier
   * @exception AxisNotFoundException An axis was not found with the correct identifier.
   * @see Axis
   * @see PlainAxis
   */
  public void removeYAxis(String id) throws AxisNotFoundException {
    if (!yAxis_.isEmpty()) {
      Axis ax;
      for (Enumeration it = yAxis_.elements(); it.hasMoreElements(); ) {
        ax = (Axis) it.nextElement();
        if (ax.getId() == id) yAxis_.removeElement(ax);
      }
      throw new AxisNotFoundException();
    } else {
      throw new AxisNotFoundException();
    }
  }

  /**
   * Remove an Y axis from the graph.
   *
   * @exception AxisNotFoundException An axis was not found with the correct identifier.
   * @see Axis
   * @see PlainAxis
   */
  public void removeYAxis(Axis axis) throws AxisNotFoundException {
    if (!yAxis_.isEmpty()) {
      Axis ax;
      for (Enumeration it = yAxis_.elements(); it.hasMoreElements(); ) {
        ax = (Axis) it.nextElement();
        if (ax.equals(axis)) yAxis_.removeElement(ax);
      }
      throw new AxisNotFoundException();
    } else {
      throw new AxisNotFoundException();
    }
  }

  /** Remove all Y axes from the graph. */
  public void removeAllYAxes() {
    yAxis_.removeAllElements();
  }

  /**
   * Get the number of Y axes associated with the graph.
   *
   * @return number of axes
   * @see Axis
   * @see PlainAxis
   */
  public int getNumberYAxis() {
    return yAxis_.size();
  }

  /**
   * Get an <code>Enumeration</code> object for the Y axes.
   *
   * @return enumeration
   */
  public Enumeration yAxisElements() {
    return yAxis_.elements();
  }

  /**
   * Set the X <code>AxisTransform</code>. This transform is used to convert to and from user to
   * physical coordinates.
   *
   * @param xfrm X transform
   * @see AxisTransform
   * @see LinearTransform
   */
  public void setXTransform(AxisTransform xfrm) {
    if (xTransform_ != null) xTransform_.removePropertyChangeListener(this);
    xTransform_ = xfrm;
    if (xfrm != null) xTransform_.addPropertyChangeListener(this);
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
   * Set the Y <code>AxisTransform</code>. This transform is used to convert to and from user to
   * physical coordinates.
   *
   * @param xfrm Y transform
   * @see AxisTransform
   * @see LinearTransform
   */
  public void setYTransform(AxisTransform xfrm) {
    if (yTransform_ != null) yTransform_.removePropertyChangeListener(this);
    yTransform_ = xfrm;
    if (xfrm != null) yTransform_.addPropertyChangeListener(this);
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
    Axis ax;
    SGLabel lab;
    if (!xAxis_.isEmpty()) {
      for (Enumeration it = xAxis_.elements(); it.hasMoreElements(); ) {
        ax = (Axis) it.nextElement();
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
      for (Enumeration it = yAxis_.elements(); it.hasMoreElements(); ) {
        ax = (Axis) it.nextElement();
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
    return (Object) null;
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
   * Transform user X coordinate to device coordinate.
   *
   * @since 3.0
   */
  public double getXUtoD2(double u) {
    if (Double.isNaN(u)) return u;
    return getLayer().getXPtoD2(xTransform_.getTransP(u));
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
   * Transform <code>long</code> to physical coordinate.
   *
   * @since 3.0
   */
  public double getXUtoP(long t) {
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
   * Transform <code>GeoDate</code> to device coordinate.
   *
   * @since 3.0
   */
  public double getXUtoD2(GeoDate t) {
    if (t == null) return Double.NaN;
    return getLayer().getXPtoD2(xTransform_.getTransP(t));
  }

  /**
   * Transform <code>long</code> to device coordinate.
   *
   * @since 3.0
   */
  public double getXUtoD2(long t) {
    if (t == Long.MAX_VALUE) return Double.NaN;
    return getLayer().getXPtoD2(xTransform_.getTransP(t));
  }

  /**
   * Transform X <code>SoTValue</code> to device coordinate.
   *
   * @since 3.0
   */
  public int getXUtoD(SoTValue val) {
    if (val.isTime()) {
      return getXUtoD(val.getLongTime());
    } else {
      return getXUtoD(((SoTValue.Double) val).getValue());
    }
  }

  /**
   * Transform Y <code>SoTValue</code> to device coordinate.
   *
   * @since 3.0
   */
  public int getYUtoD(SoTValue val) {
    if (val.isTime()) {
      return getYUtoD(val.getLongTime());
    } else {
      return getYUtoD(((SoTValue.Double) val).getValue());
    }
  }

  /**
   * Transform X <code>SoTValue</code> to device coordinate.
   *
   * @since 3.0
   */
  public double getXUtoD2(SoTValue val) {
    if (val.isTime()) {
      return getXUtoD2(val.getLongTime());
    } else {
      return getXUtoD2(((SoTValue.Double) val).getValue());
    }
  }

  /**
   * Transform Y <code>SoTValue</code> to device coordinate.
   *
   * @since 3.0
   */
  public double getYUtoD2(SoTValue val) {
    if (val.isTime()) {
      return getYUtoD2(val.getLongTime());
    } else {
      return getYUtoD2(((SoTValue.Double) val).getValue());
    }
  }

  /**
   * Transform X <code>SoTValue</code> to physical coordinate.
   *
   * @since 3.0
   */
  public double getXUtoP(SoTValue val) {
    if (val.isTime()) {
      return getXUtoP(val.getLongTime());
    } else {
      return getXUtoP(((SoTValue.Double) val).getValue());
    }
  }

  /**
   * Transform Y <code>SoTValue</code> to physical coordinate.
   *
   * @since 3.0
   */
  public double getYUtoP(SoTValue val) {
    if (val.isTime()) {
      return getYUtoP(val.getLongTime());
    } else {
      return getYUtoP(((SoTValue.Double) val).getValue());
    }
  }

  /**
   * Transform physical X coordinate to user coordinate using <code>SoTValue</code>
   *
   * @param p physical coordinate
   * @return user coorindinate
   * @since 3.0
   */
  public SoTValue getXPtoSoT(double p) {
    if (xTransform_.isTime()) {
      return new SoTValue.Time(xTransform_.getLongTimeTransU(p));
    } else {
      return new SoTValue.Double(xTransform_.getTransU(p));
    }
  }

  /**
   * Transform physical X coordinate to user coordinate.
   *
   * @param p physical coorindate
   * @return user coordinate
   */
  public double getXPtoU(double p) {
    return xTransform_.getTransU(p);
  }

  /**
   * Transform physical X coordinate to time.
   *
   * @param p physical coordinate
   * @return time
   */
  public GeoDate getXPtoTime(double p) {
    return xTransform_.getTimeTransU(p);
  }

  /**
   * Transform physical X coordinate to time.
   *
   * @param p physical coordinate
   * @return time
   * @since 3.0
   */
  public long getXPtoLongTime(double p) {
    return xTransform_.getLongTimeTransU(p);
  }

  /**
   * Transform physical coordinate to a <code>SoTPoint</code>
   *
   * @since 3.0
   * @param p physical coordinate
   * @return <code>SoTPoint</code>
   */
  public SoTPoint getPtoU(Point2D.Double loc) {
    SoTValue xv;
    SoTValue yv;
    // x - transform
    if (xTransform_.isTime()) {
      xv = new SoTValue.Time(getXPtoLongTime(loc.x));
    } else {
      xv = new SoTValue.Double(getXPtoU(loc.x));
    }
    if (yTransform_.isTime()) {
      yv = new SoTValue.Time(getYPtoLongTime(loc.y));
    } else {
      yv = new SoTValue.Double(getYPtoU(loc.y));
    }
    return new SoTPoint(xv, yv);
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
   * Transform user Y coordinate to device coordinate
   *
   * @since 3.0
   */
  public double getYUtoD2(double u) {
    if (Double.isNaN(u)) return u;
    return getLayer().getYPtoD2(yTransform_.getTransP(u));
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
   * Transform time to physical coordinate.
   *
   * @since 3.0
   */
  public double getYUtoP(long t) {
    if (t == Long.MAX_VALUE) return Double.NaN;
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

  /**
   * Transform time to device coordinate.
   *
   * @since 3.0
   */
  public double getYUtoD2(GeoDate t) {
    if (t == null) return Double.NaN;
    return getLayer().getYPtoD2(yTransform_.getTransP(t));
  }

  /**
   * Transform time to device coordinate.
   *
   * @since 3.0
   */
  public double getYUtoD2(long t) {
    if (t == Long.MAX_VALUE) return Double.NaN;
    return getLayer().getYPtoD2(yTransform_.getTransP(t));
  }

  /**
   * Transform physical Y coordinate to user coordinate using <code>SoTValue</code>
   *
   * @param p physical coordinate
   * @return user coorindinate
   * @since 3.0
   */
  public SoTValue getYPtoSoT(double p) {
    if (yTransform_.isTime()) {
      return new SoTValue.Time(yTransform_.getLongTimeTransU(p));
    } else {
      return new SoTValue.Double(yTransform_.getTransU(p));
    }
  }

  /**
   * Transform physical Y coordinate to user coordinate.
   *
   * @param p physical coorindate
   * @return user coordinate
   */
  public double getYPtoU(double p) {
    return yTransform_.getTransU(p);
  }

  /**
   * Transform physical Y coordinate to time.
   *
   * @param p physical coordinate
   * @return time
   */
  public GeoDate getYPtoTime(double p) {
    return yTransform_.getTimeTransU(p);
  }

  /**
   * Transform physical Y coordinate to time.
   *
   * @param p physical coordinate
   * @return time
   * @since 3.0
   */
  public long getYPtoLongTime(double p) {
    return yTransform_.getLongTimeTransU(p);
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

  /**
   * Find data at point
   *
   * @since 3.0
   */
  @Override
  public SGTData getDataAt(Point pt) {
    return renderer_.getDataAt(pt);
  }
}
