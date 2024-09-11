/*
 * VectorPointsRenderer
 * was derived from VectorCartesianRenderer by Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com 2006-05-11),
 * so that vectors could be draw from points (x,y,u,v values, not 2 grids)
 * and with a ColorMap or a Color, and with anti-aliasing.
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.IntArray;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.*;
import gov.noaa.pmel.sgt.dm.SGTData;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
// bob added
// import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;

/**
 * Produces a vector plot.
 *
 * @author Donald Denbo
 * @version $Revision: 1.12 $, $Date: 2002/06/26 23:18:23 $
 * @since 2.1
 */
public class VectorPointsRenderer extends CartesianRenderer {
  /**
   * @link aggregation
   * @label attr
   */
  private VectorAttribute2 attr_ = null;

  /**
   * @link aggregation
   * @label vector
   */
  private double[] xValues, yValues, uValues, vValues;

  private float slope_;
  private float xoff_;
  private float yoff_;
  public IntArray resultBaseX, resultBaseY, resultRowNumber;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      attr_ = null;
      xValues = null;
      yValues = null;
      uValues = null;
      vValues = null;
      resultBaseX = null;
      resultBaseY = null;
      resultRowNumber = null;
      if (JPane.debug) String2.log("sgt.SGTPointsVector.releaseResources() finished");
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

  /**
   * This actually draws the vectors.
   *
   * @param transparent if true, antialiasing is turned off; else it is turned on.
   */
  private void drawVector(Graphics g, VectorAttribute2 attr) {

    Layer ly = cg_.getLayer();
    Graphics2D g2 = (Graphics2D) g;
    // vectors look better with antialiasing, but caller will set it (or not)
    // Object originalAntialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING); //bob
    // added
    // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // //bob added
    slope_ = (float) ly.getXSlope(); // method was changed from protected to public
    xoff_ = (float) ly.getXOffset();
    yoff_ = (float) ly.getYOffset();
    GeneralPath gp = null;
    Stroke savedStroke = g2.getStroke();
    BasicStroke stroke =
        new BasicStroke(
            attr_.getWidth(), attr_.getCapStyle(), attr_.getMiterStyle(), attr_.getMiterLimit());
    g2.setStroke(stroke);
    //
    // a 1 unit lenght vector with a headScale of 1.0 will
    // create a head barb that is 1.05948 long.  A factor of
    // 0.94386 is required to remove this effect
    //
    float scale = (float) attr_.getVectorScale();
    float headScale = (float) attr_.getHeadScale() * 0.94386f;
    float fixedScale = (float) (headScale * attr_.getHeadFixedSize());
    float minSize = (float) attr_.getHeadMinSize();
    float maxSize = (float) attr_.getHeadMaxSize();
    ColorMap vectorColorMap = attr_.getVectorColorMap();
    Color vectorColor = attr_.getVectorColor();
    double angle = attr_.getOffsetAngle() * 0.017453293;
    double sinAngle = Math.sin(angle); // bob added
    double cosAngle = Math.cos(angle);
    float tScale;
    int nValues = xValues.length;
    int[] xtail = new int[nValues];
    int[] ytail = new int[nValues];
    int xdhead, ydhead;
    int xdtemp, ydtemp;
    float xphead, yphead;
    int count, size, nout;
    float vx, vy, vclen;
    double vdx, vdy;
    resultBaseX = new IntArray();
    resultBaseY = new IntArray();
    resultRowNumber = new IntArray();

    float hx1, hx2;
    float hy1, hy2;
    float headX, headY;
    float orgX, orgY;
    count = 0;
    for (int i = 0; i < nValues; i++) {
      if (Double.isNaN(xValues[i])
          || Double.isNaN(yValues[i])
          || Double.isNaN(uValues[i])
          || Double.isNaN(vValues[i])) continue;
      float xp = (float) cg_.getXUtoP(xValues[i]);
      float yp = (float) cg_.getYUtoP(yValues[i]);
      orgX = xPtoD(xp);
      orgY = yPtoD(yp);
      xtail[count] = (int) orgX; // safe
      ytail[count] = (int) orgY;
      resultBaseX.add(xtail[count]);
      resultBaseY.add(ytail[count]);
      resultRowNumber.add(i);
      vdx = uValues[i];
      vdy = vValues[i];
      double vclenUnscaled = Math.sqrt((vdx * vdx + vdy * vdy)); // bob added this section
      Color color = vectorColorMap == null ? vectorColor : vectorColorMap.getColor(vclenUnscaled);
      g2.setColor(color);
      vdx = vdx * scale;
      vdy = vdy * scale;
      // String2.log("vectorPointsRenderer x=" + xValues[i] + "=" + orgX +
      //    " y=" + yValues[i] + "=" + orgY +
      //    " u=" + uValues[i] + "=" + vdx +
      //    " v=" + vValues[i] + "=" + vdy);
      if (angle != 0.0) {
        vx = (float) (vdx * cosAngle - vdy * sinAngle);
        vy = (float) (vdy * cosAngle + vdx * sinAngle);
      } else {
        vx = (float) vdx;
        vy = (float) vdy;
      }
      xphead = xp + vx;
      yphead = yp + vy;
      //
      // draw line
      //
      gp = new GeneralPath();
      gp.moveTo(orgX, orgY);
      headX = xPtoD(xphead);
      headY = yPtoD(yphead);
      gp.lineTo(headX, headY);
      g2.draw(gp); // bob added
      //
      // draw head
      //
      //        if(true) continue;
      vclen = (float) (vclenUnscaled * scale);
      if (vclen == 0.0) {
        g.drawLine((int) headX, (int) headY, (int) headX, (int) headY);
      } else {
        if (attr_.getVectorStyle() != VectorAttribute2.NO_HEAD) {
          gp = new GeneralPath(); // bob added
          if (attr_.getVectorStyle() == VectorAttribute2.HEAD) {
            // unscaled head
            tScale = fixedScale / vclen;
            hx1 = xPtoD(xphead + (-vx - 0.35f * vy) * tScale);
            hy1 = yPtoD(yphead + (-vy + 0.35f * vx) * tScale);
            hx2 = xPtoD(xphead + (-vx + 0.35f * vy) * tScale);
            hy2 = yPtoD(yphead + (-vy - 0.35f * vx) * tScale);
            gp.moveTo(hx1, hy1);
            gp.lineTo(headX, headY);
            gp.lineTo(hx2, hy2);
          } else {
            // scaled head
            if (vclen >= maxSize) {
              tScale = maxSize * headScale / vclen;
            } else if (vclen <= minSize) {
              tScale = minSize * headScale / vclen;
            } else {
              tScale = headScale;
            }
            hx1 = xPtoD(xphead + (-vx - 0.35f * vy) * tScale);
            hy1 = yPtoD(yphead + (-vy + 0.35f * vx) * tScale);
            hx2 = xPtoD(xphead + (-vx + 0.35f * vy) * tScale);
            hy2 = yPtoD(yphead + (-vy - 0.35f * vx) * tScale);
            gp.moveTo(hx1, hy1);
            gp.lineTo(headX, headY);
            gp.lineTo(hx2, hy2);
          }
          gp.closePath(); // bob added
          g2.draw(gp); // bob added
          g2.fill(gp); // bob added
        }
      }
      count++;
      // g2.draw(gp); //bob commented out
    }
    g2.setStroke(savedStroke);
    //
    // draw mark
    //
    if (attr_.getOriginStyle() == VectorAttribute2.MARK) {
      g.setColor(attr_.getMarkColor());
      drawMark(
          g, xtail, ytail, count, // Bob changed from xtail.length to count,
          attr_);
    }

    // if (originalAntialiasing != null)
    //    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialiasing); //bob added
  }

  private float xPtoD(float xp) {
    return slope_ * xp + xoff_;
  }

  private float yPtoD(float yp) {
    return yoff_ - slope_ * yp;
  }

  /**
   * Draw a mark at the requested location. This routine is used by VectorCartesianGraph and
   * VectorKey.
   *
   * @param g Graphics object
   * @param xp horizontal coordinate
   * @param yp vertical coordinate
   * @param attr vector attribute
   * @see VectorKey
   */
  protected void drawMark(Graphics g, int[] xp, int[] yp, int npoints, VectorAttribute2 attr) {
    Layer ly = cg_.getLayer();

    PlotMark pm = new PlotMark(attr.getMark());
    pm.setMarkHeightP(attr.getMarkHeightP());

    for (int i = 0; i < npoints; i++) {
      pm.paintMark(g, ly, xp[i], yp[i]);
    }
  }

  /**
   * Construct a <code>VectorCartesianRenderer</code>.
   *
   * @param cg the parent <code>CartesianGraph</code>
   * @param xValues is the x location of the vector
   * @param yValues is the y location of the vector
   * @param uValues is the u component of the vector
   * @param vValues is the v component of the vector
   * @param attr the <code>VectorAttribute2</code>
   * @see CartesianGraph
   * @see Graph
   */
  public VectorPointsRenderer(
      CartesianGraph cg, SGTPointsVector sgtPointsVector, VectorAttribute2 attr) {
    cg_ = cg;
    xValues = sgtPointsVector.xValues;
    yValues = sgtPointsVector.yValues;
    uValues = sgtPointsVector.uValues;
    vValues = sgtPointsVector.vValues;
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
    VectorAttribute2 attr;
    Object vector;

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
    attr = attr_;
    drawVector(g, attr);

    //
    // reset clip
    //
    Rectangle rect = cg_.getLayer().getPane().getBounds();
    g.setClip(rect);
  }

  /**
   * Set the <code>VectorAttribute2</code>. The line appearance is controlled by this object.
   *
   * @param l <code>VectorAttribute2</code>
   */
  public void setVectorAttribute(VectorAttribute2 l) {
    if (attr_ != null) attr_.removePropertyChangeListener(this);
    attr_ = l;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
  }

  /**
   * Get the <code>VectorAttribute2</code>.
   *
   * @return <code>VectorAttribute2</code>
   */
  public VectorAttribute2 getVectorAttribute() {
    return attr_;
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
    //        String2.log("VectorCartesianRenderer: " + evt);
    //        String2.log("                       " + evt.getPropertyName());
    //      }
    modified(
        "VectorCartesianRenderer: propertyChange("
            + evt.getSource().toString()
            + "["
            + evt.getPropertyName()
            + "]"
            + ")");
  }

  @Override
  public SGTData getDataAt(Point pt) {
    return null;
  }
}
