/*
 * $Id: GridCartesianRenderer.java,v 1.30 2003/08/22 23:02:32 dwd Exp $
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
import gov.noaa.pmel.sgt.contour.Contour;
import gov.noaa.pmel.sgt.contour.ContourLine;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.util.Debug;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Range2D;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;

/**
 * Produces a cartesian plot from a <code>SGTGrid</code> object.
 *
 * @author Donald Denbo
 * @version $Revision: 1.30 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 */
public class GridCartesianRenderer extends CartesianRenderer {
  /**
   * @shapeType AggregationLink
   * @label grid
   * @undirected
   * @supplierCardinality 1
   */
  private SGTGrid grid_;

  /**
   * @shapeType AggregationLink
   * @label attr
   * @undirected
   * @supplierCardinality 1
   */
  private GridAttribute attr_ = null;

  /**
   * @link aggregationByValue
   * @supplierCardinality 1
   * @label con
   */
  private Contour con_ = null;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      if (grid_ != null) {
        SGTGrid o = grid_; // done this way to avoid infinite loop
        grid_ = null;
        o.releaseResources();
      }
      if (attr_ != null) {
        GridAttribute o = attr_; // done this way to avoid infinite loop
        attr_ = null;
        o.releaseResources();
      }
      if (con_ != null) {
        Contour o = con_; // done this way to avoid infinite loop
        con_ = null;
        o.releaseResources();
      }
      if (JPane.debug) String2.log("sgt.GridCartesianRenderer.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  //
  private void drawRaster(Graphics g) {
    int nT, nX, nY, nZ;
    int[] xp, yp;
    int i, j;
    Color color;
    int xSize, ySize, count;
    double[] xValues, yValues, gValues;
    double val;
    GeoDate[] tValues;
    //
    if (JPane.debug) String2.log(">>drawRaster");
    if (grid_.isXTime()) {
      if (JPane.debug) String2.log(">>isXTime xTimeArLen=" + grid_.getTimeArray().length);
      if (grid_.getTimeArray().length == 0) return; // 2014-04-06 bob added this and similar below
      //      if(grid_.getTimeArray().length <= 2) return;
      if (grid_.hasXEdges()) {
        if (JPane.debug) String2.log(">>hasXEdges");
        tValues = grid_.getTimeEdges();
        if (JPane.debug) String2.log(">>timeEdges=" + String2.toCSSVString(tValues));
        xSize = tValues.length;
        xp = new int[xSize];
        for (count = 0; count < xSize; count++) {
          xp[count] = cg_.getXUtoD(tValues[count]);
        }

      } else {
        if (JPane.debug) String2.log(">>!hasXEdges");
        tValues = grid_.getTimeArray();
        if (JPane.debug) String2.log(">>timeArray=" + String2.toCSSVString(tValues));
        xSize = tValues.length;
        xp = new int[xSize + 1];
        if (xSize == 1) { // 2014-04-06 bob added this and similar below
          xp[0] = cg_.getLayer().getXPtoD(0);
          xp[1] = cg_.getLayer().getXPtoD(1);
          // String2.log(">>xp[0]=" + xp[0] + " xp[1]=" + xp[1]);
        } else {
          xp[0] = cg_.getXUtoD(tValues[0].subtract(tValues[1].subtract(tValues[0]).divide(2.0)));
          for (count = 1; count < xSize; count++) {
            xp[count] = cg_.getXUtoD(tValues[count - 1].add(tValues[count]).divide(2.0));
          }
          xp[xSize] =
              cg_.getXUtoD(
                  tValues[xSize - 1].add(
                      tValues[xSize - 1].subtract(tValues[xSize - 2]).divide(2.0)));
        }
      }
    } else {
      if (JPane.debug) String2.log(">>!isXTime XArLen=" + grid_.getXArray().length);
      if (grid_.getXArray().length == 0) return;
      //      if(grid_.getXArray().length <= 2) return;
      if (grid_.hasXEdges()) {
        xValues = grid_.getXEdges();
        xSize = xValues.length;
        xp = new int[xSize];
        for (count = 0; count < xSize; count++) {
          xp[count] = cg_.getXUtoD(xValues[count]);
        }
      } else {
        xValues = grid_.getXArray();
        xSize = xValues.length;
        xp = new int[xSize + 1];
        if (xSize == 1) {
          xp[0] = cg_.getLayer().getXPtoD(0);
          xp[1] = cg_.getLayer().getXPtoD(1);
          // String2.log(">>xp[0]=" + xp[0] + " xp[1]=" + xp[1]);
        } else {
          xp[0] = cg_.getXUtoD(xValues[0] - (xValues[1] - xValues[0]) * 0.5);
          for (count = 1; count < xSize; count++) {
            xp[count] = cg_.getXUtoD((xValues[count - 1] + xValues[count]) * 0.5);
          }
          xp[xSize] =
              cg_.getXUtoD(xValues[xSize - 1] + (xValues[xSize - 1] - xValues[xSize - 2]) * 0.5);
        }
      }
    }
    if (grid_.isYTime()) {
      if (JPane.debug) String2.log(">>isYTime yTimeArLen=" + grid_.getTimeArray().length);
      if (grid_.getTimeArray().length == 0) return;
      //      if(grid_.getTimeArray().length <= 2) return;
      if (grid_.hasYEdges()) {
        if (JPane.debug) String2.log(">>hasYEdges");
        tValues = grid_.getTimeEdges();
        ySize = tValues.length;
        yp = new int[ySize];
        for (count = 0; count < ySize; count++) {
          yp[count] = cg_.getYUtoD(tValues[count]);
        }
      } else {
        if (JPane.debug) String2.log(">>!hasYEdges");
        tValues = grid_.getTimeArray();
        ySize = tValues.length;
        yp = new int[ySize + 1];
        if (ySize == 1) {
          yp[0] = cg_.getLayer().getYPtoD(0);
          yp[1] = cg_.getLayer().getYPtoD(1);
          // String2.log(">>yp[0]=" + yp[0] + " yp[1]=" + yp[1]);
        } else {
          yp[0] = cg_.getYUtoD(tValues[0].subtract(tValues[1].subtract(tValues[0]).divide(2.0)));
          for (count = 1; count < ySize; count++) {
            yp[count] = cg_.getYUtoD(tValues[count - 1].add(tValues[count]).divide(2.0));
          }
          yp[ySize] =
              cg_.getYUtoD(
                  tValues[ySize - 1].add(
                      tValues[ySize - 1].subtract(tValues[ySize - 2]).divide(2.0)));
        }
      }
    } else {
      if (JPane.debug) String2.log(">>!isYTime YArLen=" + grid_.getYArray().length);
      if (grid_.getYArray().length == 0) return;
      //      if(grid_.getYArray().length <= 2) return;
      if (grid_.hasYEdges()) {
        yValues = grid_.getYEdges();
        ySize = yValues.length;
        yp = new int[ySize];
        for (count = 0; count < ySize; count++) {
          yp[count] = cg_.getYUtoD(yValues[count]);
        }
      } else {
        yValues = grid_.getYArray();
        ySize = yValues.length;
        yp = new int[ySize + 1];
        if (ySize == 1) {
          yp[0] = cg_.getLayer().getYPtoD(0);
          yp[1] = cg_.getLayer().getYPtoD(1);
          // String2.log(">>yp[0]=" + yp[0] + " yp[1]=" + yp[1]);
        } else {
          yp[0] = cg_.getYUtoD(yValues[0] - (yValues[1] - yValues[0]) * 0.5);
          for (count = 1; count < ySize; count++) {
            yp[count] = cg_.getYUtoD((yValues[count - 1] + yValues[count]) * 0.5);
          }
          yp[ySize] =
              cg_.getYUtoD(yValues[ySize - 1] + (yValues[ySize - 1] - yValues[ySize - 2]) * 0.5);
        }
      }
    }
    //
    // draw raster
    //
    gValues = grid_.getZArray();
    count = 0;
    if (JPane.debug)
      String2.log(
          ">>xSize="
              + xSize
              + " ySize="
              + ySize
              + "\n"
              + ">>xp[]="
              + String2.toCSSVString(xp)
              + "\n"
              + ">>yp[]="
              + String2.toCSSVString(yp));
    for (i = 0; i < xSize; i++) {
      for (j = 0; j < ySize; j++) {
        val = gValues[count++];
        if (!Double.isNaN(val)) {
          // if (count<20) String2.log(">>val=" + val + " color=0x" +
          // Integer.toHexString(color.getRGB()));
          g.setColor(attr_.getColorMap().getColor(val));
          drawRect(g, xp[i], yp[j], xp[i + 1], yp[j + 1]);
        }
      }
    }
  }

  /**
   * Get the <code>Attribute</code> associated with the <code>SGTGrid</code> data.
   *
   * @return <code>Attribute</code>
   */
  @Override
  public Attribute getAttribute() {
    return attr_;
  }

  /**
   * Set the <code>GridAttribute</code> for the renderer.
   *
   * @since 2.0
   */
  public void setAttribute(GridAttribute attr) {
    if (attr_ != null) attr_.removePropertyChangeListener(this);
    attr_ = attr;
    attr_.addPropertyChangeListener(this);
  }

  private void drawRect(Graphics g, int x1, int y1, int x2, int y2) {
    int x, y, width, height;
    if (x1 < x2) {
      x = x1;
      width = x2 - x1;
    } else {
      x = x2;
      width = x1 - x2;
    }
    if (y1 < y2) {
      y = y1;
      height = y2 - y1;
    } else {
      y = y2;
      height = y1 - y2;
    }
    g.fillRect(x, y, width, height);
  }

  /**
   * Default constructor. The <code>GridCartesianRenderer</code> should be created using the <code>
   * CartesianRenderer.getRenderer</code> method.
   *
   * @see CartesianRenderer#getRenderer
   * @see Graph
   */
  public GridCartesianRenderer(CartesianGraph cg) {
    this(cg, null, null);
  }

  /**
   * Construct a <code>GridCartesianRenderer</code>. The <code>GridCartesianRenderer</code> should
   * be created using the <code>CartesianRenderer.getRenderer</code> method.
   *
   * @see CartesianRenderer#getRenderer
   * @see Graph
   */
  public GridCartesianRenderer(CartesianGraph cg, SGTGrid data) {
    this(cg, data, null);
  }

  /**
   * Construct a <code>GridCartesianRenderer</code>. The <code>GridCartesianRenderer</code> should
   * be created using the <code>CartesianRenderer.getRenderer</code> method.
   *
   * @see CartesianRenderer#getRenderer
   * @see Graph
   */
  public GridCartesianRenderer(CartesianGraph cg, SGTGrid grid, GridAttribute attr) {
    cg_ = cg;
    grid_ = grid;
    attr_ = attr;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
  }

  /**
   * Render the <code>SGTData</code>. This method should not be directly called.
   *
   * @param g graphics context
   * @see Pane#draw
   */
  @Override
  public void draw(Graphics g) {
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
    if (attr_.isRaster()) {
      drawRaster(g);
    }
    if (attr_.isAreaFill()) {
      //
      // This is a temporary method based on the
      // PPLUS area fill algorthim
      //
      // To be replaced by a area fill method that
      // uses the ContourLines
      //
      double[] x = xArrayP();
      double[] y = yArrayP();
      double[] z = grid_.getZArray();
      int i, j;
      int nx = x.length;
      int ny = y.length;
      double[] xt = new double[5];
      double[] yt = new double[5];
      double[] zt = new double[5];
      for (i = 0; i < nx - 1; i++) {
        for (j = 0; j < ny - 1; j++) {
          xt[0] = x[i];
          yt[0] = y[j];
          zt[0] = z[j + i * ny];
          //
          xt[1] = x[i + 1];
          yt[1] = y[j];
          zt[1] = z[j + (i + 1) * ny];
          //
          xt[2] = x[i + 1];
          yt[2] = y[j + 1];
          zt[2] = z[j + 1 + (i + 1) * ny];
          //
          xt[3] = xt[0];
          yt[3] = yt[2];
          zt[3] = z[j + 1 + i * ny];
          //
          // repeat first point
          //
          xt[4] = xt[0];
          yt[4] = yt[0];
          zt[4] = zt[0];
          //
          fillSquare(g, xt, yt, zt);
        }
      }
    }
    if (attr_.isContour()) {
      double val;
      String label;
      Range2D range = computeRange(10);
      Format format;
      //	con_ = new Contour(cg_, grid_, range);
      con_ = new Contour(cg_, grid_, attr_.getContourLevels());
      ContourLevels clevels = con_.getContourLevels();
      DefaultContourLineAttribute attr;
      //
      // set labels
      //
      for (int i = 0; i < clevels.size(); i++) {
        try {
          val = clevels.getLevel(i);
          attr = clevels.getDefaultContourLineAttribute(i);
          if (attr.isAutoLabel()) {
            if (attr.getLabelFormat().length() <= 0) {
              format =
                  new Format(
                      Format.computeFormat(range.start, range.end, attr.getSignificantDigits()));
            } else {
              format = new Format(attr.getLabelFormat());
            }
            label = format.form(val);
            attr.setLabelText(label);
          }
        } catch (ContourLevelNotFoundException e) {
          System.out.println(e);
        }
      }
      con_.generateContourLines();
      con_.generateContourLabels(g);
      Enumeration elem = con_.elements();
      ContourLine cl;
      while (elem.hasMoreElements()) {
        cl = (ContourLine) elem.nextElement();
        if (Debug.CONTOUR) {
          System.out.println(
              " level = "
                  + cl.getLevel()
                  + ", length = "
                  + cl.getKmax()
                  + ", closed = "
                  + cl.isClosed());
        }
        cl.draw(g);
      }
    }
    //
    // reset clip
    //
    Rectangle rect = cg_.getLayer().getPane().getBounds();
    g.setClip(rect);
  }

  private void fillSquare(Graphics g, double[] x, double[] y, double[] z) {
    ContourLevels clevels = attr_.getContourLevels();
    IndexedColor cmap = (IndexedColor) attr_.getColorMap();
    int i, j, cindex, npoly, maxindex;
    double zlev, zlevp1, f;
    Color col;
    double[] xpoly = new double[20];
    double[] ypoly = new double[20];
    double zmin = Math.min(z[0], z[1]);
    double zmax = Math.max(z[0], z[1]);
    for (i = 2; i <= 3; i++) {
      zmin = Math.min(zmin, z[i]);
      zmax = Math.max(zmax, z[i]);
    }
    if (Double.isNaN(zmax)) return;
    maxindex = clevels.getMaximumIndex();
    for (cindex = -1; cindex <= maxindex; cindex++) {
      try {
        if (cindex == -1) {
          zlev = -Double.MAX_VALUE;
        } else {
          zlev = clevels.getLevel(cindex);
        }
        if (cindex == maxindex) {
          zlevp1 = Double.MAX_VALUE;
        } else {
          zlevp1 = clevels.getLevel(cindex + 1);
        }
      } catch (ContourLevelNotFoundException e) {
        System.out.println(e);
        break;
      }
      col = cmap.getColorByIndex(cindex + 1);
      if (zmin > zlevp1 || zmax < zlev) continue;
      if (zmin >= zlev && zmax <= zlevp1) {
        fillPolygon(g, col, x, y, 4);
        return;
      }
      npoly = -1;
      for (j = 0; j < 4; j++) {
        /* sides */
        if (z[j] < zlev) {
          //
          // z[j] is below
          //
          if (z[j + 1] > zlevp1) {
            //
            // z[j+1] is above
            //
            npoly = npoly + 1;
            f = (z[j] - zlev) / (z[j] - z[j + 1]);
            xpoly[npoly] = x[j] - f * (x[j] - x[j + 1]);
            ypoly[npoly] = y[j] - f * (y[j] - y[j + 1]);
            //
            npoly = npoly + 1;
            f = (z[j] - zlevp1) / (z[j] - z[j + 1]);
            xpoly[npoly] = x[j] - f * (x[j] - x[j + 1]);
            ypoly[npoly] = y[j] - f * (y[j] - y[j + 1]);
          } else if (z[j + 1] >= zlev && z[j + 1] <= zlevp1) {
            //
            // z[j+1] is inside
            //
            npoly = npoly + 1;
            f = (z[j] - zlev) / (z[j] - z[j + 1]);
            xpoly[npoly] = x[j] - f * (x[j] - x[j + 1]);
            ypoly[npoly] = y[j] - f * (y[j] - y[j + 1]);
            //
            npoly = npoly + 1;
            xpoly[npoly] = x[j + 1];
            ypoly[npoly] = y[j + 1];
          }
        } else if (z[j] > zlevp1) {
          //
          // z[j] is above
          //
          if (z[j + 1] < zlev) {
            //
            // z[j+1] is below
            //
            npoly = npoly + 1;
            f = (z[j] - zlevp1) / (z[j] - z[j + 1]);
            xpoly[npoly] = x[j] - f * (x[j] - x[j + 1]);
            ypoly[npoly] = y[j] - f * (y[j] - y[j + 1]);
            //
            npoly = npoly + 1;
            f = (z[j] - zlev) / (z[j] - z[j + 1]);
            xpoly[npoly] = x[j] - f * (x[j] - x[j + 1]);
            ypoly[npoly] = y[j] - f * (y[j] - y[j + 1]);
          } else if (z[j + 1] >= zlev && z[j + 1] <= zlevp1) {
            //
            // z[j+1] is inside
            //
            npoly = npoly + 1;
            f = (z[j] - zlevp1) / (z[j] - z[j + 1]);
            xpoly[npoly] = x[j] - f * (x[j] - x[j + 1]);
            ypoly[npoly] = y[j] - f * (y[j] - y[j + 1]);
            //
            npoly = npoly + 1;
            xpoly[npoly] = x[j + 1];
            ypoly[npoly] = y[j + 1];
          }
        } else {
          //
          // x[j] is inside
          //
          if (z[j + 1] > zlevp1) {
            //
            // z[j+1] is above
            //
            npoly = npoly + 1;
            f = (z[j] - zlevp1) / (z[j] - z[j + 1]);
            xpoly[npoly] = x[j] - f * (x[j] - x[j + 1]);
            ypoly[npoly] = y[j] - f * (y[j] - y[j + 1]);
          } else if (z[j + 1] < zlev) {
            //
            // z[j+1] is below
            //
            npoly = npoly + 1;
            f = (z[j] - zlev) / (z[j] - z[j + 1]);
            xpoly[npoly] = x[j] - f * (x[j] - x[j + 1]);
            ypoly[npoly] = y[j] - f * (y[j] - y[j + 1]);
          } else {
            //
            // z[j+1] is inside
            //
            npoly = npoly + 1;
            xpoly[npoly] = x[j + 1];
            ypoly[npoly] = y[j + 1];
          }
        }
      }
      fillPolygon(g, col, xpoly, ypoly, npoly + 1);
    }
  }

  private void fillPolygon(Graphics g, Color c, double[] x, double[] y, int npoints) {
    Layer layer = cg_.getLayer();
    int[] xt = new int[20];
    int[] yt = new int[20];
    g.setColor(c);
    for (int i = 0; i < npoints; i++) {
      xt[i] = layer.getXPtoD(x[i]);
      yt[i] = layer.getYPtoD(y[i]);
    }
    g.fillPolygon(xt, yt, npoints);
  }

  private double[] xArrayP() {
    int i;
    double[] p;
    if (grid_.isXTime()) {
      GeoDate[] t = grid_.getTimeArray();
      p = new double[t.length];
      for (i = 0; i < t.length; i++) {
        p[i] = cg_.getXUtoP(t[i]);
      }
    } else {
      double[] x = grid_.getXArray();
      p = new double[x.length];
      for (i = 0; i < x.length; i++) {
        p[i] = cg_.getXUtoP(x[i]);
      }
    }
    return p;
  }

  private double[] yArrayP() {
    int i;
    double[] p;
    if (grid_.isYTime()) {
      GeoDate[] t = grid_.getTimeArray();
      p = new double[t.length];
      for (i = 0; i < t.length; i++) {
        p[i] = cg_.getYUtoP(t[i]);
      }
    } else {
      double[] y = grid_.getYArray();
      p = new double[y.length];
      for (i = 0; i < y.length; i++) {
        p[i] = cg_.getYUtoP(y[i]);
      }
    }
    return p;
  }

  private Range2D computeRange(int levels) {
    Range2D range;
    double zmin = Double.POSITIVE_INFINITY;
    double zmax = Double.NEGATIVE_INFINITY;
    double[] array = grid_.getZArray();
    for (int i = 0; i < array.length; i++) {
      if (!Double.isNaN(array[i])) {
        zmin = Math.min(zmin, array[i]);
        zmax = Math.max(zmax, array[i]);
      }
    }
    range = Graph.computeRange(zmin, zmax, levels);
    return range;
  }

  /**
   * Get the <code>SGTGrid</code>.
   *
   * @return <code>SGTGrid</code>
   */
  public SGTGrid getGrid() {
    return grid_;
  }

  /**
   * Get the associated <code>CartesianGraph</code> object.
   *
   * @since 2.0
   * @return <code>CartesianGraph</code>
   */
  @Override
  public CartesianGraph getCartesianGraph() {
    return cg_;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    //      if(Debug.EVENT) {
    //        System.out.println("GridCartesianRenderer: " + evt);
    //        System.out.println("                       " + evt.getPropertyName());
    //      }
    modified(
        "GridCartesianRenderer: propertyChange("
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
