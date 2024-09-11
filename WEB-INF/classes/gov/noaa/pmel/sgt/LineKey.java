/*
 * $Id: LineKey.java,v 1.16 2003/08/22 23:02:32 dwd Exp $
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
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Enumeration;
import java.util.Vector;

// jdk1.2
// import java.awt.geom.Point2D;

/**
 * <code>LineKey</code> is used to create a key for the <code>LineCartesianRenderer</code>. Multiple
 * lines can be included in the key.
 *
 * @author Donald Denbo
 * @version $Revision: 1.16 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 */
public class LineKey implements Cloneable, DataKey, Moveable, PropertyChangeListener {
  private String ident_;

  /**
   * @directed
   */
  private Layer layer_;

  /**
   * @link aggregation
   * @supplierCardinality *
   * @label line
   */
  /*#LineCartesianRenderer lnkLineCartesianRenderer;*/
  private Vector line_;

  /**
   * @link aggregation
   * @supplierCardinality *
   * @label label
   */
  /*#SGLabel lnkSGLabel;*/
  private Vector label_;

  private int columns_;
  private int style_;
  private int valign_;
  private int halign_;
  private Point2D.Double porigin_;
  private double lineLengthP_;
  private int maxLabelLength_;
  private int maxLabelHeight_;
  private StrokeDrawer stroke_ = null;
  private boolean selected_;
  private boolean selectable_;
  private boolean visible_;
  private boolean moveable_;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  private static final int VERTICAL_BORDER_ = 3;
  private static final int HORIZONTAL_BORDER_ = 15;
  private static final int COLUMN_SPACE_ = 10;
  private static final int ROW_SPACE_ = 3;
  private static final int LABEL_SPACE_ = 15;

  /** Use plain line border. */
  public static final int PLAIN_LINE = 0;

  /** Use raised border. */
  public static final int RAISED = 1;

  /** Do not draw a border. */
  public static final int NO_BORDER = 2;

  /** Align to top of key. */
  public static final int TOP = 0;

  /** Align to middle of key. */
  public static final int MIDDLE = 1;

  /** Align to bottom of key. */
  public static final int BOTTOM = 2;

  /** Align to left of key. */
  public static final int LEFT = 0;

  /** Align to center of key. */
  public static final int CENTER = 1;

  /** Align to right of key. */
  public static final int RIGHT = 2;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      layer_ = null;
      line_ = null;
      label_ = null;
      stroke_ = null;
      changes_ = null;
      if (JPane.debug) String2.log("sgt.LineKey.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public LineKey() {
    this(new Point2D.Double(0.0, 0.0), BOTTOM, LEFT);
  }

  /** */
  public LineKey(Point2D.Double loc, int valign, int halign) {
    porigin_ = loc;
    valign_ = valign;
    halign_ = halign;
    line_ = new Vector(2, 2);
    label_ = new Vector(2, 2);
    //
    // set defaults
    //
    style_ = PLAIN_LINE;
    columns_ = 1;
    ident_ = "";
    lineLengthP_ = 0.3f;
    selected_ = false;
    selectable_ = true;
    visible_ = true;
    moveable_ = true;
    stroke_ = PaneProxy.strokeDrawer;
  }

  /** Create of copy of LineKey. */
  @Override
  public LayerChild copy() {
    LineKey newKey;
    try {
      newKey = (LineKey) clone();
    } catch (CloneNotSupportedException e) {
      newKey = new LineKey();
    }
    newKey.line_ = new Vector(2, 2);
    newKey.label_ = new Vector(2, 2);
    return newKey;
  }

  @Override
  public void setSelected(boolean sel) {
    selected_ = sel;
  }

  @Override
  public boolean isSelected() {
    return selected_;
  }

  @Override
  public void setSelectable(boolean select) {
    selectable_ = select;
  }

  @Override
  public boolean isSelectable() {
    return selectable_;
  }

  @Override
  public boolean isMoveable() {
    return moveable_;
  }

  @Override
  public void setMoveable(boolean moveable) {
    moveable_ = moveable;
  }

  /**
   * Set parent layer.
   *
   * @param l parent layer
   */
  @Override
  public void setLayer(Layer l) {
    layer_ = l;
  }

  /**
   * Get layer.
   *
   * @return layer
   */
  @Override
  public Layer getLayer() {
    return layer_;
  }

  @Override
  public AbstractPane getPane() {
    return layer_.getPane();
  }

  @Override
  public void modified(String mess) {
    if (layer_ != null) layer_.modified(mess);
  }

  /**
   * Set LineKey identifier.
   *
   * @param id key identifier
   */
  @Override
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Get LineKey identifier
   *
   * @return identifier
   */
  @Override
  public String getId() {
    return ident_;
  }

  /**
   * Set line length.
   *
   * @param len line length
   */
  @Override
  public void setLineLengthP(double len) {
    if (lineLengthP_ != len) {
      lineLengthP_ = len;
      modified("LineKey: setLineLengthP()");
    }
  }

  /**
   * Get line length
   *
   * @return line length
   */
  public double getLineLengthP() {
    return lineLengthP_;
  }

  /**
   * Set the number of columns.
   *
   * @param col number of columns
   */
  @Override
  public void setColumns(int col) {
    if (columns_ != col) {
      columns_ = col;
      modified("LineKey: setColumms()");
    }
  }

  /**
   * Get the number of columns.
   *
   * @return number of columns
   */
  public int getColumns() {
    return columns_;
  }

  /**
   * Set border style.
   *
   * @param style border style
   * @see #PLAIN_LINE
   * @see #RAISED
   * @see #NO_BORDER
   */
  @Override
  public void setBorderStyle(int style) {
    if (style_ != style) {
      style_ = style;
      modified("LineKey: setBorderStyle()");
    }
  }

  /**
   * Get border style.
   *
   * @return border style
   */
  public int getBorderStyle() {
    return style_;
  }

  /**
   * Set alignment.
   *
   * @param vert vertical alignment
   * @param horz horizontal alignment
   */
  @Override
  public void setAlign(int vert, int horz) {
    if (valign_ != vert || halign_ != horz) {
      valign_ = vert;
      halign_ = horz;
      modified("LineKey: setAlign()");
    }
  }

  /**
   * Set vertical alignment
   *
   * @param vert vertical alignment
   */
  @Override
  public void setVAlign(int vert) {
    if (valign_ != vert) {
      valign_ = vert;
      modified("LineKey: setVAlign()");
    }
  }

  /**
   * Set horizontal alignment
   *
   * @param horz horizontal alignment
   */
  @Override
  public void setHAlign(int horz) {
    if (halign_ != horz) {
      halign_ = horz;
      modified("LineKey: setHAlign()");
    }
  }

  /**
   * Get vertical alignment
   *
   * @return vertical alignment
   */
  public int getVAlign() {
    return valign_;
  }

  /**
   * Get horizontal alignment
   *
   * @return horizontal alignment
   */
  public int getHAlign() {
    return halign_;
  }

  /**
   * Set location of key <br>
   * <strong>Property Change:</strong> <code>location</code>.
   *
   * @param loc key location
   */
  @Override
  public void setLocationP(Point2D.Double loc) {
    if (porigin_ == null || !porigin_.equals(loc)) {
      Point2D.Double temp = porigin_;
      porigin_ = loc;
      changes_.firePropertyChange("location", temp, porigin_);
      modified("LineKey: setLocationP()");
    }
  }

  /** Set the bounds of the <code>LineKey</code> in physical units. */
  @Override
  public void setBoundsP(Rectangle2D.Double r) {
    setLocationP(new Point2D.Double(r.x, r.y));
  }

  /** Get key bounds in physical coordinates. Not presently implemented. */
  public Rectangle2D.Double getBoundsP() {
    throw new MethodNotImplementedError();
  }

  /**
   * Get location of key.
   *
   * @return Key location
   */
  public Point2D.Double getLocationP() {
    return porigin_;
  }

  /**
   * Add a LineCartesianRenderer and label to the LineKey.
   *
   * @param line LineCartesianGraph object
   * @param label descriptive label
   */
  public void addLineGraph(LineCartesianRenderer line, SGLabel label) {
    line_.addElement(line);
    label.setLayer(layer_);
    label.setMoveable(false);
    label.setSelectable(false);
    label_.addElement(label);
    ((LineAttribute) line.getAttribute()).addPropertyChangeListener(this);
    modified("LineKey: addLineGraph()");
  }

  /**
   * Add a LineCartesianRenderer and label to the LineKey.
   *
   * @param rend LineCartesianRenderer object
   * @param label descriptive label
   * @since 3.0
   */
  @Override
  public void addGraph(CartesianRenderer rend, SGLabel label) throws IllegalArgumentException {
    if (!(rend instanceof LineCartesianRenderer))
      throw new IllegalArgumentException("Renderer is not a LineCartesianRenderer");
    addLineGraph((LineCartesianRenderer) rend, label);
  }

  /** Remove a line from the LineKey. */
  public void removeLineGraph(SGLabel label) {}

  /** Remove a line from the LineKey. */
  public void removeLineRenderer(LineCartesianRenderer line) {}

  /** Remove a line from the LineKey. */
  public void removeLineGraph(String ident) {}

  /** Remove all lines from the LineKey. */
  public void clearAll() {
    LineAttribute attr;
    for (Enumeration e = line_.elements(); e.hasMoreElements(); ) {
      attr = (LineAttribute) ((LineCartesianRenderer) e.nextElement()).getAttribute();
      attr.removePropertyChangeListener(this);
    }
    line_.removeAllElements();
    label_.removeAllElements();
    modified("LineKey: clearAll()");
  }

  /**
   * Remove line associated with data id from LineKey.
   *
   * @since 2.0
   */
  public void clear(String data_id) {
    LineCartesianRenderer lcr;
    int indx = -1;
    for (Enumeration it = line_.elements(); it.hasMoreElements(); ) {
      indx++;
      lcr = (LineCartesianRenderer) it.nextElement();
      if (lcr.getLine().getId().equals(data_id)) {
        lcr.getAttribute().removePropertyChangeListener(this);
        line_.removeElement(lcr);
        label_.removeElementAt(indx);
        modified("LineKey: clear()");
        break;
      }
    }
  }

  /**
   * Return rowheight of key in pixels.
   *
   * @since 2.0
   */
  public int getRowHeight() {
    Rectangle bounds;
    bounds = getBounds();
    return ROW_SPACE_ + maxLabelHeight_;
  }

  /** Draw the Key. */
  @Override
  public void draw(Graphics g) {
    double maxLabelLength, maxLabelHeight;
    int numLines, numRows, i, lineLength;
    int col, row, ytemp;
    double xloc, labelSpace;
    double[] xp, yp;
    int[] xd, yd;
    int[] xout, yout;
    Rectangle bounds;
    LineCartesianRenderer render = null;
    SGLabel label;
    LineAttribute attr = null;
    //
    numLines = line_.size();
    if ((numLines <= 0) || !visible_) return;

    numRows = numLines / columns_;
    if (numLines % columns_ != 0) numRows++;

    xp = new double[columns_];
    xd = new int[columns_];
    yp = new double[numRows];
    yd = new int[numRows];
    xout = new int[2];
    yout = new int[2];

    g.setColor(layer_.getPane().getComponent().getForeground());
    bounds = getBounds();
    //
    // compute location of rows and columns in device and physical coordinates
    //
    lineLength = layer_.getXPtoD(lineLengthP_) - layer_.getXPtoD(0.0f);
    labelSpace = layer_.getXDtoP(LABEL_SPACE_) - layer_.getXDtoP(0);
    //
    yd[0] = bounds.y + VERTICAL_BORDER_ + maxLabelHeight_;
    yp[0] = layer_.getYDtoP(yd[0]);
    for (i = 1; i < numRows; i++) {
      yd[i] = yd[i - 1] + ROW_SPACE_ + maxLabelHeight_;
      yp[i] = layer_.getYDtoP(yd[i]);
    }
    xd[0] = bounds.x + HORIZONTAL_BORDER_;
    xp[0] = layer_.getXDtoP(xd[0]);
    for (i = 1; i < columns_; i++) {
      xd[i] = xd[i - 1] + COLUMN_SPACE_ + lineLength + LABEL_SPACE_ + maxLabelLength_;
      xp[i] = layer_.getXDtoP(xd[i]);
    }
    //
    row = 0;
    col = 0;
    Object obj;
    Enumeration labelIt = label_.elements();
    for (Enumeration lineIt = line_.elements(); lineIt.hasMoreElements(); ) {
      obj = lineIt.nextElement();
      render = (LineCartesianRenderer) obj;
      attr = (LineAttribute) render.getAttribute();
      label = (SGLabel) labelIt.nextElement();
      //
      // draw line
      //
      g.setColor(attr.getColor());
      xout[0] = xd[col];
      xout[1] = xout[0] + lineLength;
      yout[0] = yd[row] - maxLabelHeight_ / 2;
      yout[1] = yout[0];
      switch (attr.getStyle()) {
        case LineAttribute.MARK:
          render.drawMark(g, xout, yout, 2, attr);
          break;
        case LineAttribute.HIGHLIGHT:
          stroke_.drawHighlight(g, xout, yout, 2, attr);
          break;
        case LineAttribute.HEAVY:
          stroke_.drawHeavy(g, xout, yout, 2, attr);
          break;
        case LineAttribute.DASHED:
          stroke_.drawDashed(g, xout, yout, 2, attr);
          break;
        case LineAttribute.STROKE:
          stroke_.drawStroke(g, xout, yout, 2, attr);
          break;
        case LineAttribute.MARK_LINE:
          render.drawMark(g, xout, yout, 2, attr);
          // fall through
        default:
        case LineAttribute.SOLID:
          g.drawLine(xout[0], yout[0], xout[1], yout[1]);
      }
      //
      xloc = xp[col] + lineLengthP_ + labelSpace;
      label.setLocationP(new Point2D.Double(xloc, yp[row]));
      try {
        label.draw(g);
      } catch (SGException e) {
      }
      //
      col++;
      if (col >= columns_) {
        col = 0;
        row++;
      }
    }
    switch (style_) {
      case PLAIN_LINE:
        g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        break;
      case RAISED:
        break;
      default:
      case NO_BORDER:
    }
  }

  /**
   * Get the bounding rectangle.
   *
   * @return bounding rectangle
   */
  @Override
  public Rectangle getBounds() {
    int lineLength;
    int numLines, rows;
    int x, y, height, width;

    numLines = line_.size();
    if (numLines <= 0) return new Rectangle(0, 0, 0, 0);
    //
    // find longest label
    //
    maxLabelLength_ = 0;
    maxLabelHeight_ = 0;
    SGLabel label;
    for (Enumeration it = label_.elements(); it.hasMoreElements(); ) {
      label = (SGLabel) it.nextElement();
      Rectangle sz = label.getBounds();
      maxLabelLength_ = Math.max(maxLabelLength_, sz.width);
      maxLabelHeight_ = Math.max(maxLabelHeight_, sz.height);
    }
    //
    rows = numLines / columns_;
    if (numLines % columns_ != 0) rows++;
    lineLength = layer_.getXPtoD(lineLengthP_) - layer_.getXPtoD(0.0f);
    width =
        2 * HORIZONTAL_BORDER_
            + columns_ * (lineLength + LABEL_SPACE_ + maxLabelLength_)
            + (columns_ - 1) * COLUMN_SPACE_;
    height = 2 * VERTICAL_BORDER_ + rows * maxLabelHeight_ + (rows - 1) * ROW_SPACE_;
    //
    x = layer_.getXPtoD(porigin_.x);
    y = layer_.getYPtoD(porigin_.y);
    switch (halign_) {
      case RIGHT:
        x = x - width;
        break;
      case CENTER:
        x = x - width / 2;
    }
    switch (valign_) {
      case BOTTOM:
        y = y - height;
        break;
      case MIDDLE:
        y = y - height / 2;
    }
    return new Rectangle(x, y, width, height);
  }

  @Override
  public Point getLocation() {
    Rectangle bnds = getBounds();
    return new Point(bnds.x, bnds.y);
  }

  @Override
  public void setLocation(Point loc) {
    Rectangle bnds = getBounds();
    setBounds(loc.x, loc.y, bnds.width, bnds.height);
  }

  /** Set the bounds of the <code>LineKey</code>. */
  public void setBounds(Rectangle r) {
    setBounds(r.x, r.y, r.width, r.height);
  }

  /**
   * Set the bounds of the <code>LineKey</code>. <br>
   * <strong>Property Change:</strong> <code>location</code>.
   */
  public void setBounds(int x, int y, int width, int height) {
    switch (halign_) {
      case RIGHT:
        x = x + width;
        break;
      case CENTER:
        x = x + width / 2;
    }
    switch (valign_) {
      case BOTTOM:
        y = y + height;
        break;
      case MIDDLE:
        y = y + height / 2;
    }
    double xp = layer_.getXDtoP(x);
    double yp = layer_.getYDtoP(y);
    if (porigin_.x != xp || porigin_.y != yp) {
      Point2D.Double temp = porigin_;
      porigin_.x = xp;
      porigin_.y = yp;
      changes_.firePropertyChange("location", temp, new Point2D.Double(xp, yp));
      modified("LineKey: setBounds()");
    }
  }

  Object getObjectAt(Point pt) {
    Rectangle lbnds;
    Rectangle bounds;
    LineCartesianRenderer line;
    int[] xout, yout;
    int[] xd, yd;
    int numLines, numRows;
    int lineLength;
    double labelSpace;
    int i;

    numLines = line_.size();
    if (numLines <= 0) return null;

    numRows = numLines / columns_;
    if (numLines % columns_ != 0) numRows++;

    xd = new int[columns_];
    yd = new int[numRows];
    xout = new int[2];
    yout = new int[2];
    bounds = getBounds();
    //
    // compute location of rows and columns in device and physical coordinates
    //
    lineLength = layer_.getXPtoD(lineLengthP_) - layer_.getXPtoD(0.0);
    labelSpace = layer_.getXDtoP(LABEL_SPACE_) - layer_.getXDtoP(0);
    //
    yd[0] = bounds.y + VERTICAL_BORDER_ + maxLabelHeight_;
    for (i = 1; i < numRows; i++) {
      yd[i] = yd[i - 1] + ROW_SPACE_ + maxLabelHeight_;
    }
    xd[0] = bounds.x + HORIZONTAL_BORDER_;
    for (i = 1; i < columns_; i++) {
      xd[i] = xd[i - 1] + COLUMN_SPACE_ + lineLength + LABEL_SPACE_ + maxLabelLength_;
    }
    // loop over all the lines
    int row = 0;
    int col = 0;
    for (Enumeration lineIt = line_.elements(); lineIt.hasMoreElements(); ) {
      line = (LineCartesianRenderer) lineIt.nextElement();
      xout[0] = xd[col];
      xout[1] = xout[0] + lineLength + LABEL_SPACE_ + maxLabelLength_;
      //        xout[1] = xout[0] + lineLength + LABEL_SPACE_;
      yout[0] = yd[row] - maxLabelHeight_;
      yout[1] = yd[row];
      lbnds = new Rectangle(xout[0], yout[0], xout[1] - xout[0], yout[1] - yout[0]);
      if (lbnds.contains(pt)) {
        return line;
      }
      //
      col++;
      if (col >= columns_) {
        col = 0;
        row++;
      }
    }
    if (bounds.contains(pt)) {
      return this;
    }
    return null;
  }

  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + ident_;
  }

  @Override
  public boolean isVisible() {
    return visible_;
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible_ != visible) {
      visible_ = visible;
      modified("LineKey: setVisible()");
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    //      if(Debug.EVENT) {
    //        System.out.println("LineKey: " + evt);
    //        System.out.println("         " + evt.getPropertyName());
    //      }
    modified(
        "LineKey: propertyChange("
            + evt.getSource().toString()
            + "["
            + evt.getPropertyName()
            + "]"
            + ")");
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
