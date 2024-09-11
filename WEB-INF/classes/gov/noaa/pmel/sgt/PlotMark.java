/*
 * $Id: PlotMark.java,v 1.7 2003/09/17 21:37:18 dwd Exp $
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

import gov.noaa.pmel.util.Dimension2D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.*;

/**
 * Support class used to draw a PlotMark. Plot mark codes are defined in the following table. <br>
 *
 * <p style="text-align:center;"><img src="plotmarkcodes.gif" style="vertical-align:bottom;
 * border:0;">
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/09/17 21:37:18 $
 * @since 2.0
 * @see PointCartesianRenderer
 * @see gov.noaa.pmel.sgt.swing.PlotMarkIcon
 */
public class PlotMark {
  protected int mark_;
  protected int tableSize_ = 51;
  protected int firstPoint_;
  protected int lastPoint_;
  protected double markHeight_;
  protected int fillMark_ = 44;
  protected boolean fill_ = false;
  protected boolean circle_ = false;

  protected static final int[][] markTable = {
    {5, 9}, {11, 15}, {14, 15}, {11, 12}, //  0
    {26, 31}, {32, 37}, {38, 43}, {44, 49}, //  4
    {1, 5}, {64, 67}, {5, 15}, {50, 54}, //  8
    {1, 9}, {55, 63}, {15, 19}, {21, 25}, // 12
    {50, 53}, {51, 54}, {72, 77}, {84, 98}, // 16
    {18, 22}, {11, 19}, {64, 66}, {68, 71}, // 20
    {68, 70}, {78, 83}, {102, 106}, {113, 118}, // 24
    {119, 124}, {125, 130}, {131, 136}, {105, 110}, // 28
    {107, 112}, {137, 139}, {99, 106}, {103, 108}, // 32
    {140, 144}, {140, 147}, {156, 163}, {148, 155}, // 36
    {170, 183}, {184, 189}, {188, 193}, {164, 169}, // 40
    {1, 5}, {64, 67}, {55, 63}, {15, 19}, // 44
    {68, 71}, {164, 169}, {164, 169}
  }; // 48

  protected static final int[] table = {
    9, 41, 45, 13, 9, 45, 0, 13, 41, 0, //   0
    25, 29, 0, 11, 43, 29, 11, 25, 43, 11, //  10
    25, 29, 11, 43, 29, 18, 27, 34, 0, 27, //  20
    24, 20, 27, 36, 0, 27, 30, 20, 27, 18, //  30
    0, 3, 27, 36, 27, 34, 0, 27, 51, 41, //  40
    13, 45, 9, 41, 4, 2, 16, 32, 50, 52, //  50
    38, 22, 4, 9, 29, 41, 9, 13, 25, 45, //  60
    13, 13, 27, 31, 0, 27, 45, 9, 27, 29, //  70
    0, 27, 41, 13, 20, 18, 9, 0, 18, 34, //  80
    0, 20, 36, 0, 45, 36, 34, 41, 19, 35, //  90
    0, 21, 17, 33, 37, 21, 19, 35, 33, 17, // 100
    21, 37, 20, 29, 25, 0, 17, 33, 21, 37, // 110
    35, 19, 17, 33, 21, 37, 19, 35, 33, 17, // 120
    21, 19, 43, 0, 37, 33, 21, 37, 25, 12, // 130
    44, 0, 42, 10, 0, 17, 37, 26, 30, 0, // 140
    12, 44, 0, 8, 40, 13, 45, 0, 43, 11, // 150
    0, 9, 41, 4, 41, 30, 9, 52, 4, 12, // 160
    20, 21, 13, 12, 0, 9, 45, 0, 33, 41, // 170
    42, 34, 33, 14, 44, 10, 0, 9, 41, 0, // 180
    42, 12, 46, 0, 0, 0, 0, 0, 0, 0
  }; // 190

  /**
   * Construct a <code>PlotMark</code> using the code and height from the <code>LineAttribute</code>
   * .
   */
  public PlotMark(LineAttribute attr) {
    setLineAttribute(attr);
  }

  /**
   * Construct a <code>PlotMark</code> using the code and height from the <code>PointAttribute
   * </code>.
   */
  public PlotMark(PointAttribute attr) {
    setPointAttribute(attr);
  }

  /** Construct a <code>PlotMark</code> using the code from the mark code. Default height = 0.08. */
  public PlotMark(int mark) {
    setMark(mark);
    markHeight_ = 0.08;
  }

  /** Set the mark and height from the <code>PointAttribute</code>. */
  public void setPointAttribute(PointAttribute attr) {
    int mark = attr.getMark();
    setMark(mark);
    markHeight_ = attr.getMarkHeightP() / 8.0;
  }

  /** Set the mark and height from the <code>LineAttribute</code>. */
  public void setLineAttribute(LineAttribute attr) {
    int mark = attr.getMark();
    setMark(mark);
    markHeight_ = attr.getMarkHeightP() / 8.0;
  }

  /** Set the mark. */
  public void setMark(int mark) {
    if (mark <= 0) mark = 0;
    fill_ = mark > fillMark_;
    circle_ = mark >= 50;
    if (circle_) fill_ = mark == 51;
    if (mark > tableSize_) mark = tableSize_;
    firstPoint_ = markTable[mark - 1][0] - 1;
    lastPoint_ = markTable[mark - 1][1];
    mark_ = mark;
  }

  /** Get the mark code. */
  public int getMark() {
    return mark_;
  }

  /** Set the mark height. */
  public void setMarkHeightP(double mHeight) {
    markHeight_ = mHeight / 8.0;
  }

  /** Get the mark height */
  public double getMarkHeightP() {
    return markHeight_ * 8.0;
  }

  /** Used internally by sgt. */
  public void paintMark(Graphics g, Layer ly, int xp, int yp) {
    int count, ib;
    int xdOld = 0, ydOld = 0;
    int movex, movey;
    int xt, yt;
    double xscl = ly.getXSlope() * markHeight_;
    double yscl = ly.getYSlope() * markHeight_;

    if (circle_) {
      xt = (int) (xscl * -2) + xp;
      yt = (int) (xscl * -2) + yp;
      int w = (int) (xscl * 4.0) - 1;
      if (fill_) {
        g.fillOval(xt, yt, w, w);
      } else {
        g.drawOval(xt, yt, w, w);
      }
      return;
    }

    int[] xl = new int[lastPoint_ - firstPoint_];
    int[] yl = new int[lastPoint_ - firstPoint_];

    boolean penf = false;
    int i = 0;
    for (count = firstPoint_; count < lastPoint_; count++) {
      ib = table[count];
      if (ib == 0) {
        penf = false;
      } else {
        movex = (ib >> 3) - 3;
        movey = -((ib & 7) - 3);

        xt = (int) (xscl * (double) movex) + xp;
        yt = (int) (yscl * (double) movey) + yp;

        if (penf) {
          if (fill_) {
            xl[i] = xt;
            yl[i] = yt;
            i++;
          } else {
            g.drawLine(xdOld, ydOld, xt, yt);
          }
        }
        penf = true;
        xdOld = xt;
        ydOld = yt;
      }
      if (fill_) g.fillPolygon(xl, yl, i);
    }
  }

  public static void main(String[] args) {
    /** hack code to create a "list" of plot marks. */
    JFrame frame = new JFrame("Plot Marks");
    frame.getContentPane().setLayout(new BorderLayout());
    frame.setSize(500, 700);
    JPane pane = new JPane("Plot Mark Pane", frame.getSize());
    Layer layer = new Layer("Plot Mark Layer", new Dimension2D(5.0, 7.0));
    pane.setBatch(true);
    pane.setLayout(new StackedLayout());
    frame.getContentPane().add(pane, BorderLayout.CENTER);
    pane.add(layer);
    frame.setVisible(true);
    pane.setBatch(false);
    PlotMark pm = new PlotMark(1);
    Graphics g = pane.getGraphics();
    g.setFont(new Font("Helvetica", Font.PLAIN, 18));
    pm.setMarkHeightP(0.32);
    int w = pane.getSize().width;
    int h = pane.getSize().height;
    g.setColor(Color.white);
    g.fillRect(0, 0, w, h);
    g.setColor(Color.black);
    FontMetrics fm = g.getFontMetrics();
    int hgt = fm.getAscent() / 2;
    String label;
    int xt = 100;
    int yt = 400;
    int wid = 0;
    int mark = 1;
    for (int j = 0; j < 13; j++) {
      yt = 45 * j + 100;
      for (int i = 0; i < 4; i++) {
        xt = 120 * i + 75;
        label = mark + ":";
        wid = fm.stringWidth(label) + 20;
        g.setColor(Color.blue.brighter());
        g.drawString(label, xt - wid, yt);
        pm.setMark(mark);
        g.setColor(Color.black);
        pm.paintMark(g, layer, xt, yt - hgt);
        mark++;
        if (mark > 51) break;
      }
    }
  }

  @Override
  public String toString() {
    return "PlotMark: " + mark_;
  }
}
