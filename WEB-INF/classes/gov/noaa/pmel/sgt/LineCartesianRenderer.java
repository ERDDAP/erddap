/*
 * $Id: LineCartesianRenderer.java,v 1.18 2003/08/22 23:02:32 dwd Exp $
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
import gov.noaa.pmel.sgt.dm.SGTLine;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;

/**
 * Produces a line plot with optional coloring from a second data set. If a second data set is
 * specified it must have the same shape as the first.
 *
 * @author Donald Denbo
 * @version $Revision: 1.18 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 */
public class LineCartesianRenderer extends CartesianRenderer {
  /**
   * @shapeType AggregationLink
   * @label attr
   * @undirected
   * @supplierCardinality 1
   */
  private LineAttribute attr_ = null;

  /**
   * @shapeType AggregationLink
   * @supplierCardinality 0..1
   * @label line
   * @undirected
   */
  private SGTLine line_ = null;

  /**
   * @shapeType AggregationLink
   * @supplierCardinality 0..1
   */
  private Collection collection_ = null;

  private StrokeDrawer stroke_ = null;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      cg_ = null;
      attr_ = null;
      line_ = null;
      stroke_ = null;
      if (collection_ != null) {
        collection_.clear();
        collection_ = null;
      }
      if (JPane.debug) String2.log("sgt.LineCartesianRenderer.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Get the <code>Attribute</code> associated with the data. */
  @Override
  public Attribute getAttribute() {
    return attr_;
  }

  private void drawLine(Graphics g, SGTLine line, LineAttribute attr) {
    int[] xp, yp;
    int[] xout, yout;
    int count, size, nout;
    double[] xValues, yValues;
    long[] xTValues, yTValues;

    if (line.isXTime()) {
      xTValues = line.getGeoDateArray().getTime();
      size = xTValues.length;
      xp = new int[size];
      for (count = 0; count < size; count++) {
        xp[count] = cg_.getXUtoD(xTValues[count]);
      }
    } else {
      xValues = line.getXArray();
      size = xValues.length;
      xp = new int[size];
      for (count = 0; count < size; count++) {
        xp[count] = cg_.getXUtoD(xValues[count]);
      }
    }
    //
    if (line.isYTime()) {
      yTValues = line.getGeoDateArray().getTime();
      size = yTValues.length;
      yp = new int[size];
      for (count = 0; count < size; count++) {
        yp[count] = cg_.getYUtoD(yTValues[count]);
      }
    } else {
      yValues = line.getYArray();
      size = yValues.length;
      yp = new int[size];
      for (count = 0; count < size; count++) {
        yp[count] = cg_.getYUtoD(yValues[count]);
      }
    }
    //
    // check for missing values a Double.NaN is converted to a Integer.MIN_VALUE
    //
    int first = 0;
    int lsize = 0;
    xout = new int[size];
    yout = new int[size];
    while (first < size) {
      nout = -1;
      line:
      for (count = first; count < size; count++) {
        if (xp[count] != Integer.MIN_VALUE && yp[count] != Integer.MIN_VALUE) {
          nout++;
          xout[nout] = xp[count];
          yout[nout] = yp[count];
        } else if (nout >= 0) {
          break line;
        }
      }
      first = count + 1;
      lsize = nout + 1;
      if (lsize <= 0) return;
      //
      // draw regular line
      //
      switch (attr.getStyle()) {
        case LineAttribute.MARK:
          drawMark(g, xout, yout, lsize, attr);
          break;
        case LineAttribute.HIGHLIGHT:
          stroke_.drawHighlight(g, xout, yout, lsize, attr);
          break;
        case LineAttribute.HEAVY:
          stroke_.drawHeavy(g, xout, yout, lsize, attr);
          break;
        case LineAttribute.DASHED:
          stroke_.drawDashed(g, xout, yout, lsize, attr);
          break;
        case LineAttribute.STROKE:
          stroke_.drawStroke(g, xout, yout, lsize, attr);
          break;
        case LineAttribute.MARK_LINE:
          drawMark(g, xout, yout, lsize, attr);
          // fall through
        default:
        case LineAttribute.SOLID:
          g.drawPolyline(xout, yout, lsize);
      }
    }
  }

  /**
   * Draw a mark at the requested location. This routine is used by LineCartesianGraph and LineKey.
   *
   * @param g Graphics object
   * @param xp horizontal coordinate
   * @param yp vertical coordinate
   * @param attr line attribute
   * @see LineKey
   */
  protected void drawMark(Graphics g, int[] xp, int[] yp, int npoints, LineAttribute attr) {
    Layer ly = cg_.getLayer();
    PlotMark pm = new PlotMark(attr);

    for (int i = 0; i < npoints; i++) {
      pm.paintMark(g, ly, xp[i], yp[i]);
    }
  }

  /**
   * Default constructor.
   *
   * @see CartesianGraph
   * @see Graph
   */
  public LineCartesianRenderer(CartesianGraph cg) {
    this(cg, (SGTLine) null, null);
  }

  /**
   * Construct a <code>LineCartesianRenderer</code>. The default <code>LineAttribute</code> will be
   * used.
   *
   * @param cg the parent <code>CartesianGraph</code>
   * @param data an <code>SGTLine</code> object
   * @see CartesianGraph
   * @see Graph
   */
  public LineCartesianRenderer(CartesianGraph cg, SGTLine line) {
    this(cg, line, null);
    cg_ = cg;
    line_ = line;
  }

  /**
   * Construct a <code>LineCartesianRenderer</code>.
   *
   * @param cg the parent <code>CartesianGraph</code>
   * @param data an <code>SGTLine</code>
   * @param line the <code>LineAttribute</code>
   * @see CartesianGraph
   * @see Graph
   */
  public LineCartesianRenderer(CartesianGraph cg, SGTLine line, LineAttribute attr) {
    cg_ = cg;
    line_ = line;
    attr_ = attr;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
    stroke_ = PaneProxy.strokeDrawer;
  }

  /**
   * Construct a <code>LineCartesianRenderer</code>.
   *
   * @param cg the parent <code>CartesianGraph</code>
   * @param col a <code>Collection</code> of <code>SGTLine</code> objects
   * @param line the <code>LineAttribute</code>
   * @see CartesianGraph
   * @see Graph
   */
  public LineCartesianRenderer(CartesianGraph cg, Collection col, LineAttribute attr) {
    cg_ = cg;
    collection_ = col;
    attr_ = attr;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
    stroke_ = PaneProxy.strokeDrawer;
  }

  /**
   * Render the <code>SGTData</code>. This method should not be directly called.
   *
   * @param g graphics context
   * @see Pane#draw
   */
  @Override
  public void draw(Graphics g) {
    LineAttribute attr;
    Object line;

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
      attr = new LineAttribute(LineAttribute.SOLID, cg_.getPane().getComponent().getForeground());
    } else {
      attr = attr_;
    }
    g.setColor(attr.getColor());
    if (collection_ == null) {
      drawLine(g, line_, attr);
    } else {
      for (Enumeration li = collection_.elements(); li.hasMoreElements(); ) {
        line = li.nextElement();
        if (line instanceof SGTLine) {
          drawLine(g, (SGTLine) line, attr);
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
   * Set the <code>LineAttribute</code>. The line appearance is controlled by this object.
   *
   * @param l <code>LineAttribute</code>
   */
  public void setLineAttribute(LineAttribute l) {
    if (attr_ != null) attr_.removePropertyChangeListener(this);
    attr_ = l;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
  }

  /**
   * Get the <code>LineAttribute</code>.
   *
   * @return <code>LineAttribute</code>
   */
  public LineAttribute getLineAttribute() {
    return attr_;
  }

  /**
   * Test if a <code>Collection</code> of <code>SGTLine</code> was using to construct this renderer.
   *
   * @return true if <code>Collection</code> was used
   */
  public boolean hasCollection() {
    return (collection_ != null);
  }

  /**
   * Get the <code>Collection</code> of <code>SGTLine</code> objects.
   *
   * @return <code>Collection</code>
   */
  public Collection getCollection() {
    return collection_;
  }

  /**
   * Get the <code>SGTLine</code> object.
   *
   * @return <code>SGTLine</code>
   */
  public SGTLine getLine() {
    return line_;
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
    //        System.out.println("LineCartesianRenderer: " + evt);
    //        System.out.println("                       " + evt.getPropertyName());
    //      }
    modified(
        "LineCartesianRenderer: propertyChange("
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
