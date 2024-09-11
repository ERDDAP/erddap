/*
 * $Id: StrokeDrawer2.java,v 1.3 2001/12/13 00:16:16 dwd Exp $
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

import java.awt.*;
import java.awt.geom.*;

/**
 * Implements stroke drawing using Java2D functionality.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2001/12/13 00:16:16 $
 * @since 2.1
 */
public class StrokeDrawer2 implements StrokeDrawer, Cloneable {

  @Override
  public void drawHeavy(Graphics g, int[] xout, int[] yout, int size, LineAttribute attr) {
    Graphics2D g2 = (Graphics2D) g;
    Stroke saved = g2.getStroke();
    BasicStroke stroke = new BasicStroke(attr.getWidth());
    g2.setStroke(stroke);
    g2.drawPolyline(xout, yout, size);
    g2.setStroke(saved);
  }

  @Override
  public void drawDashed(Graphics g, int[] xout, int[] yout, int size, LineAttribute attr) {
    Graphics2D g2 = (Graphics2D) g;
    Stroke saved = g2.getStroke();
    float[] dashes = {4.0f, 4.0f};
    BasicStroke stroke =
        new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dashes, 0.0f);
    g2.setStroke(stroke);
    g2.drawPolyline(xout, yout, size);
    g2.setStroke(saved);
  }

  @Override
  public void drawStroke(Graphics g, int[] xout, int[] yout, int size, LineAttribute attr) {

    Graphics2D g2 = (Graphics2D) g;
    Stroke saved = g2.getStroke();
    BasicStroke stroke;
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
    g2.setStroke(stroke);
    g2.drawPolyline(xout, yout, size);
    g2.setStroke(saved);
  }

  @Override
  public void drawHighlight(Graphics g, int[] xout, int[] yout, int size, LineAttribute attr) {
    Graphics2D g2 = (Graphics2D) g;
    Stroke saved = g2.getStroke();
    BasicStroke stroke = new BasicStroke(2.75f);
    Color col = attr.getColor();
    Color rev = new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue());
    g2.setColor(rev);
    g2.setStroke(stroke);
    g2.drawPolyline(xout, yout, size);
    g2.setColor(col);
    g2.setStroke(saved);
    g2.drawPolyline(xout, yout, size);
  }
}
