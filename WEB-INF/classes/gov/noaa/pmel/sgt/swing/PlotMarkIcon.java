/*
 * $Id: PlotMarkIcon.java,v 1.5 2003/09/17 20:32:10 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.swing;

import gov.noaa.pmel.sgt.PlotMark;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;

/**
 * <code>PlotMarkIcon</code> extends <code>PlotMark</code> to create a
 * icon than displays the <code>sgt</code> plot marks.  The
 * <code>PlotMarkIcon</code> can be used with buttons, e.g. selecting
 * a plot mark for a line, or labels.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/09/17 20:32:10 $
 * @since 2.0
 * @see PlotMark
 * @see Icon
 */
public class PlotMarkIcon extends PlotMark implements Icon {
  private int size_;
  private double scale_;
  /**
   * Construct a <code>PlotMarkIcon</code>.
   *
   * @param mark the plot mark code
   * @param size plot mark size in device units
   */
  public PlotMarkIcon(int mark, int size) {
    super(mark);
    setSize(size);
  }
  /**
   * Construct a <code>PlotMarkIcon</code>.
   *
   * @param mark the plot mark code
   */
  public PlotMarkIcon(int mark) {
    this(mark, 16);
  }
  /**
   * Set the size of the plot mark in device units.
   */
  public void setSize(int size) {
    size_ = size;
    scale_ = (double)size_/8.0;
  }
  /**
   * Get the size of the plot mark
   */
  public int getSize() {
    return size_;
  }
  /**
   * Paint the icon at the specified location
   */
  public void paintIcon(Component c, Graphics g, int x, int y) {
    int ib;
    boolean penf;
    int movex, movey;
    int xt, yt;
    int xtOld, ytOld;

    g.setColor(c.getForeground());

    if(circle_) {
      xt = (int)(scale_*2.0) + x;
      yt = (int)(scale_*2.0) + y;
      int w = (int)(scale_*4.0);
      if(fill_) {
        g.fillOval(xt, yt, w, w);
      } else {
        g.drawOval(xt, yt, w, w);
      }
      return;
    }

    int[] xl = new int[lastPoint_-firstPoint_];
    int[] yl = new int[lastPoint_-firstPoint_];

    double scale;

    xtOld = x;
    ytOld = y;

    penf = false;
    int i=0;
    for(int count=firstPoint_; count < lastPoint_; count++) {
      ib = table[count];
      if(ib == 0) {
        penf=false;
      } else {
        movex = (ib>>3);
        movey = 7 - (ib&7);
        xt = (int)(scale_*(double)movex) + x;
        yt = (int)(scale_*(double)movey) + y;
        if(penf) {
          if(fill_) {
            xl[i] = xt;
            yl[i] = yt;
            i++;
          } else {
            g.drawLine(xtOld, ytOld, xt, yt);
          }
        }
        penf = true;
        xtOld = xt;
        ytOld = yt;
      }
    }
    if(fill_) g.fillPolygon(xl, yl, i);
  }
  /**
   * Get the icon with
   */
  public int getIconWidth() {
    return size_;
  }
  /**
   * Set the icon height
   */
  public int getIconHeight() {
    return size_;
  }

  public String toString() {
    return "PlotMarkIcon: " + mark_;
  }
}



