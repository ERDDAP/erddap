/*
 * $Id: AnnotationCartesianRenderer.java,v 1.12 2003/08/22 23:02:31 dwd Exp $
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
import gov.noaa.pmel.sgt.dm.Annotation;
import gov.noaa.pmel.sgt.dm.Annote;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.dm.SGTPoint;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.SoTPoint;
// import gov.noaa.pmel.util.GeoDate;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
// import java.awt.geom.*;
import java.beans.PropertyChangeEvent;
import java.util.Iterator;

/**
 * Renders <code>Annote</code> and <code>Annotation</code> objects.
 *
 * @author Donald Denbo
 * @version $Revision: 1.12 $
 * @since 3.0
 */
public class AnnotationCartesianRenderer extends CartesianRenderer {
  /**
   * @link aggregation
   * @undirected
   * @label annotation
   * @supplierCardinality 1
   */
  private Annotation data_ = null;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      cg_ = null;
      data_ = null;
      if (JPane.debug) String2.log("sgt.AnnotationCartesianRenderer.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  public AnnotationCartesianRenderer(CartesianGraph cg, Annotation annotation, Attribute attr) {
    cg_ = cg;
    data_ = annotation;
  }

  @Override
  public Attribute getAttribute() {
    return null;
  }

  @Override
  public CartesianGraph getCartesianGraph() {
    return cg_;
  }

  @Override
  public SGTData getDataAt(Point pt) {
    SGTData data = null;
    Annote note = null;
    Iterator iter;
    //    System.out.println("point: " + pt);
    Rectangle bnds;

    if (data_.hasLine()) {
      iter = data_.getLineIterator();
      while (iter.hasNext()) {
        note = (Annote) iter.next();
        bnds = note.getBounds(cg_);
        //        System.out.println("Line.Bounds: " + bnds);
        if (note.getBounds(cg_).contains(pt)) {
          return note;
        }
      }
    }
    if (data_.hasPoint()) {
      iter = data_.getPointIterator();
      while (iter.hasNext()) {
        note = (Annote) iter.next();
        bnds = note.getBounds(cg_);
        //        System.out.println("Point.Bounds: " + bnds);
        if (bnds.contains(pt)) {
          return note;
        }
      }
    }
    if (data_.hasText()) {
      iter = data_.getTextIterator();
      while (iter.hasNext()) {
        note = (Annote) iter.next();
        bnds = note.getBounds(cg_);
        //        System.out.println("Text.Bounds: " + bnds);
        if (bnds.contains(pt)) {
          return note;
        }
      }
    }
    if (data_.hasOval()) {
      iter = data_.getOvalIterator();
      while (iter.hasNext()) {
        note = (Annote) iter.next();
        bnds = note.getBounds(cg_);
        //        System.out.println("Oval.Bounds: " + bnds);
        if (bnds.contains(pt)) {
          return note;
        }
      }
    }
    if (data_.hasRect()) {
      iter = data_.getRectIterator();
      while (iter.hasNext()) {
        note = (Annote) iter.next();
        bnds = note.getBounds(cg_);
        //        System.out.println("Rect.Bounds: " + bnds);
        if (bnds.contains(pt)) {
          return note;
        }
      }
    }
    return data;
  }

  /** Render Annotation using java.awt.Graphic2D primatives. */
  @Override
  public void draw(Graphics g) {
    if (cg_.clipping_) {
      //      System.out.println("clipping: on");
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
    Graphics2D g2 = (Graphics2D) g;
    Iterator iter;
    if (data_.hasLine()) {
      SGTLine line;
      LineAttribute attr;
      Annote.Line pLine;
      iter = data_.getLineIterator();
      while (iter.hasNext()) {
        pLine = (Annote.Line) iter.next();
        line = pLine.getLine();
        attr = pLine.getAttribute();
        //
        renderLine(g2, line, attr);
      }
    }
    if (data_.hasPoint()) {
      SGTPoint point;
      PointAttribute attr;
      Annote.Point pPoint;
      iter = data_.getPointIterator();
      while (iter.hasNext()) {
        pPoint = (Annote.Point) iter.next();
        point = pPoint.getPoint();
        attr = pPoint.getAttribute();
        //
        renderPoint(g2, point, attr);
      }
    }
    if (data_.hasText()) {
      SGLabel text;
      SoTPoint location;
      Annote.Text pText;
      iter = data_.getTextIterator();
      while (iter.hasNext()) {
        pText = (Annote.Text) iter.next();
        text = pText.getText();
        location = pText.getLocation();
        //
        renderText(g2, location, text);
      }
    }
    if (data_.hasOval()) {
      SoTPoint pt1;
      SoTPoint pt2;
      LineAttribute attr;
      Color color;
      Annote.Oval pOval;
      iter = data_.getOvalIterator();
      while (iter.hasNext()) {
        pOval = (Annote.Oval) iter.next();
        pt1 = pOval.getUpperLeft();
        pt2 = pOval.getLowerRight();
        attr = pOval.getLineAttribute();
        color = pOval.getFillColor();
        //
        renderOval(g2, pt1, pt2, attr, color);
      }
    }
    if (data_.hasRect()) {
      SoTPoint pt1;
      SoTPoint pt2;
      LineAttribute attr;
      Color color;
      Annote.Rect pRect;
      iter = data_.getRectIterator();
      while (iter.hasNext()) {
        pRect = (Annote.Rect) iter.next();
        pt1 = pRect.getUpperLeft();
        pt2 = pRect.getLowerRight();
        attr = pRect.getLineAttribute();
        color = pRect.getFillColor();
        //
        renderRect(g2, pt1, pt2, attr, color);
      }
    }
    /**
     * @todo: implement this gov.noaa.pmel.sgt.CartesianRenderer abstract method
     */
    //
    // reset clip
    //
    Rectangle rect = cg_.getLayer().getPane().getBounds();
    g.setClip(rect);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    modified(
        "AnnotationCartesianRenderer: propertyChange("
            + evt.getSource().toString()
            + "["
            + evt.getPropertyName()
            + "]"
            + ")");
  }

  /** Render oval using Graphics2D */
  private void renderOval(
      Graphics2D g2, SoTPoint pt1, SoTPoint pt2, LineAttribute attr, Color color) {
    Stroke stroke = getStrokeFromLineAttribute(attr);
    //
    float xPt1 = (float) cg_.getXUtoD2(pt1.getX());
    float yPt1 = (float) cg_.getYUtoD2(pt1.getY());
    float xPt2 = (float) cg_.getXUtoD2(pt2.getX());
    float yPt2 = (float) cg_.getYUtoD2(pt2.getY());
    float width = xPt2 - xPt1;
    float height = yPt2 - yPt1;
    //
    Shape oval = new Ellipse2D.Float(xPt1, yPt1, width, height);
    Paint savedPaint = g2.getPaint();
    Stroke savedStroke = g2.getStroke();
    Color savedColor = g2.getColor();
    //
    if (color != null) {
      g2.setPaint(color);
      g2.fill(oval);
    }
    if (stroke != null) {
      g2.setStroke(stroke);
      g2.setColor(attr.getColor());
      g2.draw(oval);
    }
    g2.setPaint(savedPaint);
    g2.setStroke(savedStroke);
    g2.setColor(savedColor);
  }

  /** Render rect using Graphics2D */
  private void renderRect(
      Graphics2D g2, SoTPoint pt1, SoTPoint pt2, LineAttribute attr, Color color) {
    Stroke stroke = getStrokeFromLineAttribute(attr);
    //
    float xPt1 = (float) cg_.getXUtoD2(pt1.getX());
    float yPt1 = (float) cg_.getYUtoD2(pt1.getY());
    float xPt2 = (float) cg_.getXUtoD2(pt2.getX());
    float yPt2 = (float) cg_.getYUtoD2(pt2.getY());
    float width = xPt2 - xPt1;
    float height = yPt2 - yPt1;
    //
    Shape rect = new Rectangle2D.Float(xPt1, yPt1, width, height);
    Paint savedPaint = g2.getPaint();
    Stroke savedStroke = g2.getStroke();
    Color savedColor = g2.getColor();
    //
    if (color != null) {
      g2.setPaint(color);
      g2.fill(rect);
    }
    if (stroke != null) {
      g2.setStroke(stroke);
      g2.setColor(attr.getColor());
      g2.draw(rect);
    }
    g2.setPaint(savedPaint);
    g2.setStroke(savedStroke);
    g2.setColor(savedColor);
  }

  /** Render line using Graphics2D */
  private void renderLine(Graphics2D g2, SGTLine line, LineAttribute attr) {
    LineCartesianRenderer lcr = new LineCartesianRenderer(cg_, line, attr);
    lcr.draw(g2);
    /*
        Stroke savedStroke = g2.getStroke();
        Color savedColor = g2.getColor();
        //
        Stroke stroke = getStrokeFromLineAttribute(attr);
        // build line
        float[] xd;
        float[] yd;
        if(line.isXTime()) {
          long[] xu = line.getGeoDateArray().getTime();
          xd = new float[xu.length];
          for(int i=0; i < xu.length; i++) {
            xd[i] = (float)cg_.getXUtoD2(xu[i]);
          }
        } else {
          double[] xu = line.getXArray();
          xd = new float[xu.length];
          for(int i=0; i < xu.length; i++) {
            xd[i] = (float)cg_.getXUtoD2(xu[i]);
          }
        }
        if(line.isYTime()) {
          long[] yu = line.getGeoDateArray().getTime();
          yd = new float[yu.length];
          for(int i=0; i < yu.length; i++) {
            yd[i] = (float)cg_.getYUtoD2(yu[i]);
          }
        } else {
          double[] yu = line.getYArray();
          yd = new float[yu.length];
          for(int i=0; i < yu.length; i++) {
            yd[i] = (float)cg_.getYUtoD2(yu[i]);
          }
        }
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xd.length);
    //
        int start = 0;
        for(int i=0; i < xd.length; i++) {
          if(!(Float.isNaN(xd[i]) || Float.isNaN(yd[i]))) {
            path.moveTo(xd[i], yd[i]);
            start = i+1;
            break;
          }
        }
        boolean move = false;
        for(int i=start; i < xd.length; i++) {
          if(Float.isNaN(xd[i]) || Float.isNaN(yd[i])) {
            move = true;
            continue;
          }
          if(move) {
            path.moveTo(xd[i], yd[i]);
            move = false;
          } else {
            path.lineTo(xd[i], yd[i]);
          }
        }
        if(stroke != null) g2.setStroke(stroke);
        g2.setColor(attr.getColor());
        g2.draw(path);
        g2.setColor(savedColor);
        g2.setStroke(savedStroke); */
  }

  /** Render point using Graphics2D */
  private void renderPoint(Graphics2D g, SGTPoint point, PointAttribute attr) {
    PointCartesianRenderer pcr = new PointCartesianRenderer(cg_, point, attr);
    pcr.draw(g);
  }

  /** Render label using Graphics2D */
  private void renderText(Graphics2D g, SoTPoint loc, SGLabel text) {
    double xp;
    double yp;
    if (loc.getX().isTime()) {
      xp = cg_.getXUtoP(loc.getX().getLongTime());
    } else {
      xp = cg_.getXUtoP(((Number) loc.getX().getObjectValue()).doubleValue());
    }
    if (loc.getY().isTime()) {
      yp = cg_.getYUtoP(loc.getY().getLongTime());
    } else {
      yp = cg_.getYUtoP(((Number) loc.getY().getObjectValue()).doubleValue());
    }
    text.setLocationP(new Point2D.Double(xp, yp));
    text.setLayer(cg_.getLayer());
    try {
      text.draw(g);
    } catch (LayerNotFoundException ex) {
      ex.printStackTrace();
    }
  }

  private Stroke getStrokeFromLineAttribute(LineAttribute attr) {
    BasicStroke stroke = null;
    if (attr == null) return stroke;
    switch (attr.getStyle()) {
        //      case LineAttribute.MARK:
        //        drawMark(g, xout, yout, lsize, attr);
        //        break;
        //      case LineAttribute.HIGHLIGHT:
        //        stroke_.drawHighlight(g, xout, yout, lsize, attr);
        //        break;
      case LineAttribute.HEAVY:
        stroke = new BasicStroke(attr.getWidth());
        break;
      case LineAttribute.DASHED:
        float[] dashes = {4.0f, 4.0f};
        stroke =
            new BasicStroke(
                1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dashes, 0.0f);
        break;
      case LineAttribute.STROKE:
        float[] arr = attr.getDashArray();
        if (arr == null || (arr.length <= 1)) {
          stroke =
              new BasicStroke(
                  attr.getWidth(), attr.getCapStyle(), attr.getMiterStyle(), attr.getMiterLimit());
        } else {
          stroke =
              new BasicStroke(
                  attr.getWidth(),
                  attr.getCapStyle(),
                  attr.getMiterStyle(),
                  attr.getMiterLimit(),
                  attr.getDashArray(),
                  attr.getDashPhase());
        }
        break;
        //      case LineAttribute.MARK_LINE:
        //        drawMark(g, xout, yout, lsize, attr);
      default:
        //      case LineAttribute.SOLID:
        //        g.drawPolyline(xout, yout, lsize);
    }
    return stroke;
  }
}
