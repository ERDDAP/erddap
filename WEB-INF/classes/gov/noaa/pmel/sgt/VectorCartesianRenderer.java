/*
 * $Id: VectorCartesianRenderer.java,v 1.12 2002/06/26 23:18:23 dwd Exp $
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
import gov.noaa.pmel.sgt.dm.SGTVector;
import gov.noaa.pmel.util.GeoDate;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
// bob added
// import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;

/**
 * Produces a vector plot. If a second data set is specified it must have the same shape as the
 * first.
 *
 * @author Donald Denbo
 * @version $Revision: 1.12 $, $Date: 2002/06/26 23:18:23 $
 * @since 2.1
 */
public class VectorCartesianRenderer extends CartesianRenderer {
  /**
   * @link aggregation
   * @label attr
   */
  private VectorAttribute attr_ = null;

  /**
   * @link aggregation
   * @label vector
   */
  private SGTVector vector_ = null;

  private Collection collection_ = null;
  private float slope_;
  private float xoff_;
  private float yoff_;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      cg_ = null;
      attr_ = null;
      vector_ = null;
      if (collection_ != null) {
        collection_.clear();
        collection_ = null;
      }
      if (JPane.debug) String2.log("sgt.VectorCartesianRenderer.releaseResources() finished");
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

  private void drawVector(Graphics g, SGTVector vector, VectorAttribute attr) {

    Layer ly = cg_.getLayer();
    Graphics2D g2 = (Graphics2D) g;
    // vectors look better with antialiasing, but caller will set it (or not)
    // Object originalAntialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING); //bob
    // added
    // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // //bob added
    slope_ = (float) ly.getXSlope();
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
    double angle = attr_.getOffsetAngle() * 0.017453293;
    double sinAngle = Math.sin(angle); // bob added
    double cosAngle = Math.cos(angle);
    float tScale;
    float[] xp, yp;
    int[] xtail, ytail;
    int xdhead, ydhead;
    int xdtemp, ydtemp;
    float xphead, yphead;
    int count, size, nout, idx;
    double[] xValues = null, yValues = null;
    GeoDate[] xTValues, yTValues;
    double[] uValues, vValues;
    float vx, vy, vclen;
    double vdx, vdy;

    float hx1, hx2;
    float hy1, hy2;
    float headX, headY;
    float orgX, orgY;
    //
    // get axes
    //
    if (vector.isXTime()) {
      xTValues = vector.getU().getTimeArray();
      size = xTValues.length;
      xp = new float[size];
      for (count = 0; count < size; count++) {
        xp[count] = (float) cg_.getXUtoP(xTValues[count]);
      }
    } else {
      xValues = vector.getU().getXArray();
      size = xValues.length;
      xp = new float[size];
      for (count = 0; count < size; count++) {
        xp[count] = (float) cg_.getXUtoP(xValues[count]);
      }
    }
    //
    if (vector.isYTime()) {
      yTValues = vector.getU().getTimeArray();
      size = yTValues.length;
      yp = new float[size];
      for (count = 0; count < size; count++) {
        yp[count] = (float) cg_.getYUtoP(yTValues[count]);
      }
    } else {
      yValues = vector.getU().getYArray();
      size = yValues.length;
      yp = new float[size];
      for (count = 0; count < size; count++) {
        yp[count] = (float) cg_.getYUtoP(yValues[count]);
      }
    }
    //
    // get u & v
    //
    uValues = vector.getU().getZArray();
    vValues = vector.getV().getZArray();
    //
    xtail = new int[uValues.length];
    ytail = new int[uValues.length];
    count = 0;
    //    int nx = xp.length;
    int ny = yp.length;
    int idx_ny;
    for (int i = 0; i < xp.length; i++) {
      idx_ny = i * ny;
      for (int j = 0; j < yp.length; j++) {
        idx = j + idx_ny;
        if (Double.isNaN(uValues[idx]) || Double.isNaN(vValues[idx])) continue;
        orgX = xPtoD(xp[i]);
        orgY = yPtoD(yp[j]);
        xtail[count] = (int) orgX;
        ytail[count] = (int) orgY;
        vdx = uValues[idx] * scale;
        vdy = vValues[idx] * scale;
        if (angle != 0.0) {
          vx = (float) (vdx * cosAngle - vdy * sinAngle);
          vy = (float) (vdy * sinAngle + vdx * sinAngle);
        } else {
          vx = (float) vdx;
          vy = (float) vdy;
        }
        xphead = xp[i] + vx;
        yphead = yp[j] + vy;
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
        vclen = (float) Math.sqrt((double) (vx * vx + vy * vy));
        if (vclen == 0.0) {
          g.drawLine((int) headX, (int) headY, (int) headX, (int) headY);
        } else {
          if (attr_.getVectorStyle() != VectorAttribute.NO_HEAD) {
            gp = new GeneralPath(); // bob added
            if (attr_.getVectorStyle() == VectorAttribute.HEAD) {
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
    }
    g2.setStroke(savedStroke);
    //
    // draw mark   (bob says "marker")
    //
    if (attr_.getOriginStyle() == VectorAttribute.MARK) {
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
  protected void drawMark(Graphics g, int[] xp, int[] yp, int npoints, VectorAttribute attr) {
    Layer ly = cg_.getLayer();

    PlotMark pm = new PlotMark(attr.getMark());
    pm.setMarkHeightP(attr.getMarkHeightP());

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
  public VectorCartesianRenderer(CartesianGraph cg) {
    this(cg, (SGTVector) null, null);
  }

  /**
   * Construct a <code>VectorCartesianRenderer</code>. The default <code>VectorAttribute</code> will
   * be used.
   *
   * @param cg the parent <code>CartesianGraph</code>
   * @param data an <code>SGTVector</code> object
   * @see CartesianGraph
   * @see Graph
   */
  public VectorCartesianRenderer(CartesianGraph cg, SGTVector vector) {
    this(cg, vector, null);
    cg_ = cg;
    vector_ = vector;
  }

  /**
   * Construct a <code>VectorCartesianRenderer</code>.
   *
   * @param cg the parent <code>CartesianGraph</code>
   * @param vector a <code>SGTVector</code>
   * @param attr the <code>VectorAttribute</code>
   * @see CartesianGraph
   * @see Graph
   */
  public VectorCartesianRenderer(CartesianGraph cg, SGTVector vector, VectorAttribute attr) {
    cg_ = cg;
    vector_ = vector;
    attr_ = attr;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
  }

  /**
   * Construct a <code>VectorCartesianRenderer</code>.
   *
   * @param cg the parent <code>CartesianGraph</code>
   * @param col a <code>Collection</code> of <code>SGTVector</code> objects
   * @param attr the <code>VectorAttribute</code>
   * @see CartesianGraph
   * @see Graph
   */
  public VectorCartesianRenderer(CartesianGraph cg, Collection col, VectorAttribute attr) {
    cg_ = cg;
    collection_ = col;
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
    VectorAttribute attr;
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
    if (attr_ == null) {
      attr = new VectorAttribute(1.0, cg_.getPane().getComponent().getForeground());
    } else {
      attr = attr_;
    }
    g.setColor(attr.getVectorColor());
    if (collection_ == null) {
      drawVector(g, vector_, attr);
    } else {
      for (Enumeration li = collection_.elements(); li.hasMoreElements(); ) {
        vector = li.nextElement();
        if (vector instanceof SGTVector) {
          drawVector(g, (SGTVector) vector, attr);
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
   * Set the <code>VectorAttribute</code>. The line appearance is controlled by this object.
   *
   * @param l <code>VectorAttribute</code>
   */
  public void setVectorAttribute(VectorAttribute l) {
    if (attr_ != null) attr_.removePropertyChangeListener(this);
    attr_ = l;
    if (attr_ != null) attr_.addPropertyChangeListener(this);
  }

  /**
   * Get the <code>VectorAttribute</code>.
   *
   * @return <code>VectorAttribute</code>
   */
  public VectorAttribute getVectorAttribute() {
    return attr_;
  }

  /**
   * Test if a <code>Collection</code> of <code>SGTVector</code> was using to construct this
   * renderer.
   *
   * @return true if <code>Collection</code> was used
   */
  public boolean hasCollection() {
    return (collection_ != null);
  }

  /**
   * Get the <code>Collection</code> of <code>SGTVector</code> objects.
   *
   * @return <code>Collection</code>
   */
  public Collection getCollection() {
    return collection_;
  }

  /**
   * Get the <code>SGTVector</code> object.
   *
   * @return <code>SGTVector</code>
   */
  public SGTVector getVector() {
    return vector_;
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
    //        System.out.println("VectorCartesianRenderer: " + evt);
    //        System.out.println("                       " + evt.getPropertyName());
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
