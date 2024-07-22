/*
 * $Id: StrokeDrawer1.java,v 1.2 2001/12/11 21:31:42 dwd Exp $
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

/**
 * Implements stroke drawing for JDK1.1
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2001/12/11 21:31:42 $
 * @since 2.1
 */
public class StrokeDrawer1 implements StrokeDrawer, Cloneable {

  @Override
  public void drawHeavy(Graphics g, int[] xout, int[] yout, int size, LineAttribute attr) {
    g.drawPolyline(xout, yout, size);
  }

  @Override
  public void drawDashed(Graphics g, int[] xout, int[] yout, int size, LineAttribute attr) {
    g.drawPolyline(xout, yout, size);
  }

  @Override
  public void drawStroke(Graphics g, int[] xout, int[] yout, int size, LineAttribute attr) {
    g.drawPolyline(xout, yout, size);
  }

  @Override
  public void drawHighlight(Graphics g, int[] x, int[] y, int size, LineAttribute attr) {
    int[] xr = new int[size];
    int[] yr = new int[size];
    int i;
    Color col = attr.getColor();
    Color rev = new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue());
    //
    // simple hightlight draw with pixel displacements in reverse
    // color
    //
    g.setPaintMode();
    for (i = 0; i < size; i++) {
      xr[i] = x[i] + 1;
      yr[i] = y[i] + 1;
    }
    g.setColor(col);
    g.drawPolyline(xr, yr, size);
    for (i = 0; i < size; i++) {
      xr[i] = x[i] - 1;
      yr[i] = y[i] - 1;
    }
    g.drawPolyline(xr, yr, size);
    g.setColor(rev);
    g.drawPolyline(x, y, size);
  }
}
