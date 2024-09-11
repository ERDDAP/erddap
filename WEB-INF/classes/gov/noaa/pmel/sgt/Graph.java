/*
 * $Id: Graph.java,v 1.12 2003/08/22 23:02:32 dwd Exp $
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

import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyChangeListener;

/**
 * Abstract base class for all graphics drawn on a <code>Layer</code>. The <code>Graph</code> class
 * defines the interfaces for the user to physical coordinate, user to device, and physical to user
 * coordinate systems.
 *
 * <p>The following demonstrates how a {@link CartesianGraph} may be used.
 *
 * <p>
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
 * @version $Revision: 1.12 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 * @see Layer
 * @see LinearTransform
 * @see PlainAxis
 * @see SGLabel
 * @see CartesianGraph
 */
public abstract class Graph implements PropertyChangeListener {
  private String ident_;

  /**
   * @directed
   * @label layer
   */
  protected Layer layer_;

  /** Bob Simons added this to avoid memory leak problems. */
  public abstract void releaseResources() throws Exception;

  /** Default constructor. */
  public Graph() {
    this("");
  }

  /**
   * Constructor for <code>Graph</code> class.
   *
   * @param id identifier
   */
  public Graph(String id) {
    ident_ = id;
  }

  /** Copy the <code>Graph</code> object and all attached classes. */
  public abstract Graph copy();

  /**
   * Get the <code>Graph</code> identifier
   *
   * @return ident
   */
  public String getId() {
    return ident_;
  }

  //
  abstract void draw(Graphics g);

  //
  public void setLayer(Layer l) {
    layer_ = l;
  }

  /**
   * Get the associated <code>Layer</code>.
   *
   * @return <code>Layer</code> object
   */
  public Layer getLayer() {
    return layer_;
  }

  /**
   * Return parent pane.
   *
   * @since 2.0
   */
  public AbstractPane getPane() {
    return layer_.getPane();
  }

  /**
   * Used internally by sgt.
   *
   * @since 2.0
   */
  public void modified(String mess) {
    if (layer_ != null) layer_.modified(mess);
  }

  /**
   * Compute a "nice" range from a range and number of intervals.
   *
   * @param range min and max values
   * @param num number of intervals
   * @return "nice" range
   */
  public static Range2D computeRange(Range2D range, int num) {
    return computeRange(range.start, range.end, num);
  }

  /**
   * Compute a "nice" range from a range and number of intervals.
   *
   * @since 2.0
   * @param range min and max values
   * @param num number of intervals
   * @return "nice" range
   */
  public static SoTRange computeRange(SoTRange range, int num) {
    if (!range.isTime()) {
      SoTRange.Double drange = (SoTRange.Double) range;
      return new SoTRange.Double(computeRange(drange.start, drange.end, num));
    }
    return null;
  }

  /**
   * Compute a "nice" range from the minimum, maximum, and number of intervals.
   *
   * @param min minimum value
   * @param max maximum value
   * @param num number of intervals
   * @return "nice" range
   */
  public static Range2D computeRange(double min, double max, int num) {
    int interval = Math.abs(num);
    double temp, pow, delta;
    int nt;
    boolean reversed = false;
    //
    // check inputs to make sure that they are valid
    //
    if (min == max) {
      if (min == 0.0) {
        min = -1.0;
        max = 1.0;
      } else {
        min = 0.9 * max;
        max = 1.1 * max;
      }
    }
    if (min > max) {
      temp = min;
      min = max;
      max = temp;
      reversed = true;
    }
    if (interval == 0) interval = 1;
    //
    // find the approximate size of the interval
    //
    temp = (max - min) / (double) interval;
    if (temp == 0.0) temp = max;
    if (temp == 0.0) {
      min = -1.0;
      max = 1.0;
      temp = 2.0 / (double) interval;
    }
    //
    // scale the interval size by powers of ten to a value between
    // one and ten
    //
    nt = (int) log10(temp);
    if (temp < 1.0) nt--;
    pow = Math.pow(10.0, (double) nt);
    temp = temp / pow;
    //
    // find the closest permissible value for the interval size
    //
    if (temp < 1.414213562) {
      delta = pow;
    } else if (temp < 3.162277660) {
      delta = 2.0 * pow;
    } else if (temp < 7.071067812) {
      delta = 5.0 * pow;
    } else {
      delta = 10.0 * pow;
    }
    //
    // calculate the minimum value of the range
    //
    temp = min / delta;
    nt = (int) temp;
    if (temp < 0.0) nt--;
    min = delta * nt;
    //
    // calculate the maximum value of the range
    //
    temp = max / delta;
    nt = (int) temp;
    if (temp > 0.0) nt++;
    max = delta * nt;
    //
    if (reversed) {
      temp = min;
      min = max;
      max = temp;
      delta = -delta;
    }
    return new Range2D(min, max, delta);
  }

  static final double log10(double x) {
    return 0.4342944819 * Math.log(x);
  }

  //
  abstract Object getObjectAt(Point pt);

  /**
   * Get a <code>String</code> representation of the <code>Graph</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + ident_;
  }

  /**
   * Find data at a <code>Point</code>
   *
   * @since 3.0
   */
  public abstract SGTData getDataAt(Point pt);
}
