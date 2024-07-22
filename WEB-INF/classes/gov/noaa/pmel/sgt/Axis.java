/*
 * $Id: Axis.java,v 1.20 2003/08/22 23:02:31 dwd Exp $
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
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.SoTRange;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

/**
 * Abstract base class for Cartesian axes. Cartesian axes are designed to be used with the <code>
 * CartesianGraph</code> class. Axes and <code>AxisTransform</code>s can be registed with an <code>
 * Axis</code>. This allows changes in both the physical range and user range to be immediatedly
 * updated for the registered <code>AxisTransform</code>s and axes.
 *
 * <p>Cartesian axes can have their user coordinates be double values or time (as <code>GeoDate
 * </code> objects). These have been separated into two child objects.
 *
 * @author Donald Denbo
 * @version $Revision: 1.20 $, $Date: 2003/08/22 23:02:31 $
 * @since 1.0
 * @see SpaceAxis
 * @see TimeAxis
 */
public abstract class Axis implements Selectable {
  private String ident_;

  /**
   * @directed
   * @label graph
   */
  public CartesianGraph graph_;

  public Vector registeredAxes_;
  public Vector registeredTransforms_;
  public Color lineColor_;
  public int numSmallTics_;
  public double largeTicHeight_;
  public double smallTicHeight_;
  public double thickTicWidth_;
  public int ticPosition_;
  public int labelPosition_;
  public int labelInterval_;
  public Font labelFont_;
  public Color labelColor_;
  public double labelHeight_;
  public int sigDigits_;
  public String labelFormat_;

  /**
   * @link aggregation
   * @label title
   */
  public SGLabel title_;

  public Range2D pRange_;
  public boolean space_;
  public int orientation_;
  public boolean selected_;
  public boolean selectable_;
  public boolean visible_;

  /**
   * Place the label and/or tic on the positive side of the axis. The right side of <code>VERTICAL
   * </code> axes and the top of <code>HORIZONTAL</code> axes.
   */
  public static final int POSITIVE_SIDE = 0;

  /**
   * Place the label and/or tic on the negative side of the axis. The left side of <code>VERTICAL
   * </code> axes and the bottom of <code>HORIZONTAL</code> axes.
   */
  public static final int NEGATIVE_SIDE = 1;

  /** Do not draw a label and/or tic. */
  public static final int NO_LABEL = 2;

  /** Draw the tics on both sides of the axes. */
  public static final int BOTH_SIDES = 2;

  /** Draw a horizontal axis. */
  public static final int HORIZONTAL = 0;

  /** Draw a vertical axis. */
  public static final int VERTICAL = 1;

  public static final int AUTO = 3;

  protected abstract void updateRegisteredTransforms();

  protected abstract void updateRegisteredAxes();

  //

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception {
    try {
      // objects from Axis
      graph_ = null;
      if (registeredAxes_ != null) {
        Vector o = registeredAxes_;
        registeredAxes_ = null;
        for (Object o2 : o) ((Axis) o2).releaseResources();
        o.clear();
      }
      if (registeredTransforms_ != null) {
        Vector o = registeredTransforms_;
        registeredTransforms_ = null;
        for (Object o2 : o) ((Transform) o2).releaseResources();
        o.clear();
      }
      labelFont_ = null;
      title_ = null;
      pRange_ = null;
      if (JPane.debug) String2.log("sgt.PlainAxis.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  protected void drawXTic(Graphics g, double xp, double yp, double ticHeight) {
    int x0, y0, y1;
    double yp0, yp1;
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
      yp0 = yp + ticHeight;
    } else {
      yp0 = yp;
    }
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
      yp1 = yp - ticHeight;
    } else {
      yp1 = yp;
    }
    x0 = graph_.getLayer().getXPtoD(xp);
    y0 = graph_.getLayer().getYPtoD(yp0);
    y1 = graph_.getLayer().getYPtoD(yp1);
    // System.out.println("Axis.drawXTic x0="+x0+" y0="+y0+" y1="+y1);
    g.drawLine(x0, y0, x0, y1);
  }

  //
  protected void drawThickXTic(Graphics g, double xp, double yp, double ticHeight) {
    int x0, x1, y0, y1, xc;
    int ticW, ticH;
    double yp0, yp1;
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
      yp0 = yp + ticHeight;
    } else {
      yp0 = yp;
    }
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
      yp1 = yp - ticHeight;
    } else {
      yp1 = yp;
    }
    xc = graph_.getLayer().getXPtoD(xp);
    x0 = graph_.getLayer().getXPtoD(xp - thickTicWidth_ / 2.0);
    x1 = graph_.getLayer().getXPtoD(xp + thickTicWidth_ / 2.0);
    y0 = graph_.getLayer().getYPtoD(yp0);
    y1 = graph_.getLayer().getYPtoD(yp1);
    if ((x1 - x0) < 3) {
      x0 = xc - 1;
      x1 = xc + 1;
    }
    ticW = x1 - x0;
    ticH = y1 - y0;
    g.fillRect(x0, y0, ticW, ticH);
    /*    g.drawLine(x0-1, y0, x0-1, y1);
    g.drawLine(x0, y0, x0, y1);
    g.drawLine(x0+1, y0, x0+1, y1); */
  }

  //
  protected void drawYTic(Graphics g, double xp, double yp, double ticHeight) {
    int x0, x1, y0;
    double xp0, xp1;
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
      xp0 = xp + ticHeight;
    } else {
      xp0 = xp;
    }
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
      xp1 = xp - ticHeight;
    } else {
      xp1 = xp;
    }
    y0 = graph_.getLayer().getYPtoD(yp);
    x0 = graph_.getLayer().getXPtoD(xp0);
    x1 = graph_.getLayer().getXPtoD(xp1);
    // System.out.println("Axis.drawYTic x0="+x0+" y0="+y0+" x1="+x1);
    g.drawLine(x0, y0, x1, y0);
  }

  //
  protected void drawThickYTic(Graphics g, double xp, double yp, double ticHeight) {
    int x0, x1, y0;
    double xp0, xp1;
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == POSITIVE_SIDE) {
      xp0 = xp + ticHeight;
    } else {
      xp0 = xp;
    }
    if (ticPosition_ == BOTH_SIDES || ticPosition_ == NEGATIVE_SIDE) {
      xp1 = xp - ticHeight;
    } else {
      xp1 = xp;
    }
    y0 = graph_.getLayer().getYPtoD(yp);
    x0 = graph_.getLayer().getXPtoD(xp0);
    x1 = graph_.getLayer().getXPtoD(xp1);
    g.drawLine(x0, y0 - 1, x1, y0 - 1);
    g.drawLine(x0, y0, x1, y0);
    g.drawLine(x0, y0 + 1, x1, y0 + 1);
  }

  /** Default constructor for Axis. */
  public Axis() {
    this("");
  }

  /**
   * Constructor for Axis. Sets the axis identifier and initializes the defaults. Default values
   * are:
   *
   * <PRE>
   *    numberSmallTics = 0
   *    largeTicHeightP = 0.1
   *    smallTicHeightP = 0.05
   *     thickTicWidth_ = 0.025
   *        ticPosition = NEGATIVE_SIDE
   *      labelPosition = NEGATIVE_SIDE
   *      labelInterval = 2
   *          labelFont = Font("Helvetica", Font.ITALIC, 10);
   *         labelColor = Color.black;
   *       labelHeightP = 0.15
   *  significantDigits = 2;
   *        labelFormat = ""
   *              title = null
   *        orientation = HORIZONTAL
   *         selectable = true
   *            visible = true
   *  </PRE>
   *
   * @param id axis identifier
   */
  public Axis(String id) {
    ident_ = id;
    registeredAxes_ = new Vector();
    registeredTransforms_ = new Vector();
    //
    // set defaults
    //
    lineColor_ = Color.black;
    numSmallTics_ = 0;
    largeTicHeight_ = 0.1;
    smallTicHeight_ = 0.05;
    thickTicWidth_ = 0.025;
    ticPosition_ = NEGATIVE_SIDE;
    labelPosition_ = NEGATIVE_SIDE;
    labelInterval_ = 2;
    labelHeight_ = 0.15;
    sigDigits_ = 2;
    labelFormat_ = "";
    title_ = null;
    orientation_ = HORIZONTAL;
    labelFont_ = new Font("Helvetica", Font.PLAIN, 10);
    labelColor_ = Color.black;
    selected_ = false;
    selectable_ = true;
    visible_ = true;
  }

  /**
   * Create a copy of the axis.
   *
   * @return the copy
   */
  public abstract Axis copy();

  //
  public abstract void draw(Graphics g);

  void setGraph(CartesianGraph g) {
    graph_ = g;
  }

  /**
   * Get the <code>Graph</code> associated with the axis.
   *
   * @return Graph object.
   */
  public CartesianGraph getGraph() {
    return graph_;
  }

  /**
   * Get the parent pane.
   *
   * @return AbstractPane
   * @since 2.0
   */
  public AbstractPane getPane() {
    return graph_.getPane();
  }

  /**
   * Used internally by sgt.
   *
   * @since 2.0
   */
  public void modified(String mess) {
    //    if(Debug.EVENT) System.out.println("Axis: modified()");
    if (graph_ != null) graph_.modified(mess);
  }

  /**
   * Register an axis. Registered axes will be notified of changes to the user coordinate range.
   *
   * @param axis An Axis object.
   */
  public void register(Axis axis) {
    registeredAxes_.addElement(axis);
  }

  /**
   * Register an <code>AxisTransform</code>. Registered <code>AxisTransform</code>s will be notified
   * of changes to the user coordinate range and physical coordinate range.
   *
   * @param trns A AxisTransform object.
   */
  public void register(AxisTransform trns) {
    registeredTransforms_.addElement(trns);
  }

  /**
   * Unregister an axis. Axis will no longer be notified of changes in the user range.
   *
   * @param axis An Axis object.
   */
  public void clear(Axis axis) {
    if (!registeredAxes_.isEmpty()) {
      registeredAxes_.removeElement(axis);
    }
  }

  /**
   * Unregister an <code>AxisTransform</code>. The <code>AxisTransform</code> will no longer be
   * notified of changes to the user or physical coordinate range.
   *
   * @param trns A AxisTransform ojbect.
   */
  public void clear(AxisTransform trns) {
    if (!registeredTransforms_.isEmpty()) {
      registeredTransforms_.removeElement(trns);
    }
  }

  /** Unregister all axes. No axes will be notified of changes in the user range. */
  public void clearAllRegisteredAxes() {
    registeredAxes_.removeAllElements();
  }

  /**
   * Unregister all <code>AxisTransform</code>s. No <code>AxisTransform</code>s will be notified of
   * changes in the user of physical range.
   */
  public void clearAllRegisteredTransforms() {
    registeredTransforms_.removeAllElements();
  }

  /**
   * Get the number of currently registered transforms.
   *
   * @return number of registered transforms
   */
  public int getNumberRegisteredTransforms() {
    return registeredTransforms_.size();
  }

  /**
   * Get the number of currently registered axes.
   *
   * @return number of registered axes
   */
  public int getNumberRegisteredAxes() {
    return registeredAxes_.size();
  }

  /**
   * Set the large tic height in physical units.
   *
   * @param lthgt large tic height.
   */
  public void setLargeTicHeightP(double lthgt) {
    if (largeTicHeight_ != lthgt) {
      largeTicHeight_ = lthgt;
      modified("Axis: setLargeTicHeightP()");
    }
  }

  /**
   * Get the large tic height.
   *
   * @return large tic height in physcial units.
   */
  public double getLargeTicHeightP() {
    return largeTicHeight_;
  }

  /**
   * Set the number of small tics between large tics.
   *
   * @param nstic number of small tics.
   */
  public void setNumberSmallTics(int nstic) {
    if (numSmallTics_ != nstic) {
      numSmallTics_ = nstic;
      modified("Axis: setNumerSmallTics()");
    }
  }

  /**
   * Get the number of small tics between large tics.
   *
   * @return number of small tics.
   */
  public int getNumberSmallTics() {
    return numSmallTics_;
  }

  /**
   * Set the small tic height in physical units.
   *
   * @param sthgt small tic height.
   */
  public void setSmallTicHeightP(double sthgt) {
    if (smallTicHeight_ != sthgt) {
      smallTicHeight_ = sthgt;
      modified("Axis: setSmallTicHeightP()");
    }
  }

  /**
   * Get the small tic height.
   *
   * @return small tic height in physical units.
   */
  public double getSmallTicHeightP() {
    return smallTicHeight_;
  }

  /**
   * Set the thick tic width in physical units. Minimum thickness is 3 pixels.
   *
   * @param wid thick tic width.
   */
  public void setThickTicWidthP(double wid) {
    if (thickTicWidth_ != wid) {
      thickTicWidth_ = wid;
      modified("Axis: setThickTicWidthP()");
    }
  }

  /**
   * Get the thick tic width.
   *
   * @return thick tic width in physical units.
   */
  public double getThickTicWidthP() {
    return thickTicWidth_;
  }

  /**
   * Set the tic position. Tic position can be <code>POSITIVE_SIDE</code>, <code>NEGATIVE_SIDE
   * </code>, or <code>BOTH_SIDES</code>.
   *
   * @param tpos tic position
   */
  public void setTicPosition(int tpos) {
    if (ticPosition_ != tpos) {
      ticPosition_ = tpos;
      modified("Axis: setTicPosition()");
    }
  }

  /**
   * Get the tic position.
   *
   * @return tic position
   */
  public int getTicPosition() {
    return ticPosition_;
  }

  /**
   * Set the label position. Label position can be <code>POSITIVE_SIDE</code>, <code>NEGATIVE_SIDE
   * </code>, and <code>NO_LABEL</code>.
   *
   * @param labp label position.
   */
  public void setLabelPosition(int labp) {
    if (labelPosition_ != labp) {
      labelPosition_ = labp;
      modified("Axis: setLabelPosition()");
    }
  }

  /**
   * Get the label position.
   *
   * @return label position
   */
  public int getLabelPosition() {
    return labelPosition_;
  }

  /**
   * Set the label font.
   *
   * @param fnt label font
   */
  public void setLabelFont(Font fnt) {
    if (labelFont_ == null || !labelFont_.equals(fnt)) {
      labelFont_ = fnt;
      modified("Axis: setLabelFont()");
    }
  }

  /**
   * Get the label font.
   *
   * @return label font
   */
  public Font getLabelFont() {
    return labelFont_;
  }

  /**
   * Set the line and tick color.
   *
   * @param color line and tick color
   * @since 3.0
   */
  public void setLineColor(Color color) {
    if (lineColor_ == null || !lineColor_.equals(color)) {
      lineColor_ = color;
      modified("Axis: setLineColor()");
    }
  }

  /**
   * Get the line color.
   *
   * @since 3.0
   * @return color
   */
  public Color getLineColor() {
    return lineColor_;
  }

  /**
   * Set the label color.
   *
   * @param color label color
   * @since 2.0
   */
  public void setLabelColor(Color color) {
    if (labelColor_ == null || !labelColor_.equals(color)) {
      labelColor_ = color;
      modified("Axis: setLabelColor()");
    }
  }

  /**
   * Get the label color.
   *
   * @since 2.0
   * @return color
   */
  public Color getLabelColor() {
    return labelColor_;
  }

  /**
   * Set the label height in physical units.
   *
   * @param lhgt label height.
   */
  public void setLabelHeightP(double lhgt) {
    if (labelHeight_ != lhgt) {
      labelHeight_ = lhgt;
      modified("Axis: setLabelHeightP()");
    }
  }

  /**
   * Get the label height.
   *
   * @return label height
   */
  public double getLabelHeightP() {
    return labelHeight_;
  }

  /**
   * Set the axis title.
   *
   * @param title axis title
   */
  public void setTitle(SGLabel title) {
    if (title_ == null || !title_.equals(title)) {
      title_ = title;
      title_.setMoveable(false);
      modified("Axis: setTitle()");
    }
  }

  /**
   * Get the axis title.
   *
   * @return axis title
   */
  public SGLabel getTitle() {
    return title_;
  }

  /**
   * Set the physical range. This method updates any registered <code>Transform</code>s. If no
   * <code>Transform</CODE>s are registered, the <code>setRangeP</code> method has no effect.
   *
   * @param pr physcial range
   */
  public void setRangeP(Range2D pr) {
    if (pRange_ == null || !pRange_.equals(pr)) {
      pRange_ = pr;
      updateRegisteredTransforms();
      modified("Axis: setRangeP()");
    }
  }

  /**
   * Get the physical range. Obtains the physical range from the associated <code>CartesianGraph
   * </code> object and attached <code>Transform</code>.
   *
   * @return physical range
   */
  public Range2D getRangeP() {
    if (orientation_ == HORIZONTAL) {
      return graph_.xTransform_.getRangeP();
    } else {
      return graph_.yTransform_.getRangeP();
    }
  }

  /**
   * Set the axis identifier.
   *
   * @param id identifier
   */
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Get the axis identifier.
   *
   * @return identifier
   */
  public String getId() {
    return ident_;
  }

  /**
   * Set axis orientation. Allowed orientations are <code>HORIZONATAL</code> and <code>VERTICAL
   * </code>.
   *
   * @param or orientation
   */
  public void setOrientation(int or) {
    if (orientation_ != or) {
      orientation_ = or;
      modified("Axis: setOrientation()");
    }
  }

  /**
   * Get axis orientation
   *
   * @return axis orientation
   */
  public int getOrientation() {
    return orientation_;
  }

  /**
   * Tests if axis is space.
   *
   * @return true if space
   */
  public boolean isSpace() {
    return space_;
  }

  /**
   * Tests if axis is time.
   *
   * @return true if time
   */
  public boolean isTime() {
    return !space_;
  }

  /**
   * Get the bounding box for the axis in device units.
   *
   * @return bounding box
   */
  @Override
  public abstract Rectangle getBounds();

  /**
   * Get a <code>String</code> representation of the <code>Axis</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + ident_;
  }

  @Override
  public void setSelected(boolean sel) {
    selected_ = sel;
  }

  /**
   * Determines if the axis has been selected.
   *
   * @return true, if selected
   * @since 2.0
   */
  @Override
  public boolean isSelected() {
    return selected_;
  }

  /**
   * Set the selectable state.
   *
   * @since 2.0
   */
  @Override
  public void setSelectable(boolean select) {
    selectable_ = select;
  }

  /**
   * Determines if the axis is selectable.
   *
   * @since 2.0
   */
  @Override
  public boolean isSelectable() {
    return selectable_;
  }

  /**
   * Determines if the axis is visible.
   *
   * @since 2.0
   */
  public boolean isVisible() {
    return visible_;
  }

  /**
   * Set the visibility state.
   *
   * @since 2.0
   */
  public void setVisible(boolean visible) {
    if (visible_ != visible) {
      visible_ = visible;
      modified("Axis: setVisible()");
    }
  }

  /**
   * Set the axis location.
   *
   * @since 2.0
   */
  public abstract void setLocationU(SoTPoint pt);

  /**
   * Get current axis location.
   *
   * @since 2.0
   */
  public abstract SoTPoint getSoTLocationU();

  /**
   * Set user range.
   *
   * @since 2.0
   */
  public abstract void setRangeU(SoTRange range);

  /**
   * Get user range.
   *
   * @since 2.0
   */
  public abstract SoTRange getSoTRangeU();
}
