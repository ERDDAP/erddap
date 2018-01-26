/*
 * $Id: PointCollectionKey.java,v 1.9 2003/08/22 23:02:32 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package  gov.noaa.pmel.sgt;

import com.cohort.util.MustBe;
import com.cohort.util.String2;

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.Debug;

import java.util.Vector;
import java.util.Enumeration;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Point;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;

// jdk1.2
//import java.awt.geom.Point2D;

/**
 * <code>PointCollectionKey</code> is used to create a key for the
 * <code>PointCartesianRenderer</code>. Multiple
 * lines can be included in the key.
 *
 * @author Donald Denbo
 * @version $Revision: 1.9 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.0
**/
public class PointCollectionKey implements Cloneable,
        DataKey, Moveable, PropertyChangeListener {
  private String ident_;
/** @directed */
  private Layer layer_;
  private Vector points_;
  private Vector label_;
  private int columns_;
  private int style_;
  private int valign_;
  private int halign_;
  private Point2D.Double porigin_;
  private double lineLengthP_;
  private int maxLabelLength_;
  private int maxLabelHeight_;
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
  /**
   * Use plain line border.
   */
  public static final int PLAIN_LINE = 0;
  /**
   * Use raised border.
   */
  public static final int RAISED = 1;
  /**
   * Do not draw a border.
   */
  public static final int NO_BORDER = 2;
  /**
   * Align to top of key.
   */
  public static final int TOP = 0;
  /**
   * Align to middle of key.
   */
  public static final int MIDDLE = 1;
  /**
   * Align to bottom of key.
   */
  public static final int BOTTOM = 2;
  /**
   * Align to left of key.
   */
  public static final int LEFT = 0;
  /**
   * Align to center of key.
   */
  public static final int CENTER = 1;
  /**
   * Align to right of key.
   */
  public static final int RIGHT = 2;

  /**
   * @link aggregation
   * @supplierCardinality *
   * @label points
   */
  /*#PointCartesianRenderer lnkPointCartesianRenderer;*/

  /** @link aggregation
   * @label label
   * @supplierCardinality **/
  /*#SGLabel lnkSGLabel;*/

    /** 
     * Bob Simons added this to avoid memory leak problems.
     */
    public void releaseResources() throws Exception {
        try {  
            layer_ = null;
            if (points_ != null) {points_.clear(); points_ = null; }
            if (label_ != null) {label_.clear(); label_ = null; }
            changes_ = null;
            if (JPane.debug) String2.log("sgt.VectorKey.releaseResources() finished");
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            if (JPane.debug) 
                String2.pressEnterToContinue(); 
        }
    }

  /**
   * Default constructor.
   */
  public PointCollectionKey() {
    this(new Point2D.Double(0.0, 0.0), BOTTOM, LEFT);
  }
  /**
   * Create <code>PointCollectionKey</code>.
   */
  public PointCollectionKey(Point2D.Double loc,int valign,int halign) {
    porigin_ = loc;
    valign_ = valign;
    halign_ = halign;
    points_ = new Vector(2,2);
    label_ = new Vector(2,2);
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
  }
  /**
   * Create of copy of PointCollectionKey.
   */
  public LayerChild copy() {
    PointCollectionKey newKey;
    try {
      newKey = (PointCollectionKey)clone();
    } catch (CloneNotSupportedException e) {
      newKey = new PointCollectionKey();
    }
    newKey.points_ = new Vector(2,2);
    newKey.label_ = new Vector(2,2);
    return newKey;
  }
  public void setSelected(boolean sel) {
    selected_ = sel;
  }
  public boolean isSelected() {
    return selected_;
  }
  public void setSelectable(boolean select) {
    selectable_ = select;
  }
  public boolean isSelectable() {
    return selectable_;
  }
  public boolean isMoveable() {
    return moveable_;
  }
  public void setMoveable(boolean moveable) {
    moveable_ = moveable;
  }
  /**
   * Set parent layer.
   *
   * @param l parent layer
   */
  public void setLayer(Layer l) {
    layer_ = l;
  }
  /**
   * Get layer.
   *
   * @return layer
   */
  public Layer getLayer() {
    return layer_;
  }

  public AbstractPane getPane() {
    return layer_.getPane();
  }

  public void modified(String mess) {
    if(layer_ != null)
      layer_.modified(mess);
  }
  /**
   * Set PointCollectionKey identifier.
   *
   * @param id key identifier
   */
  public void setId(String id) {
    ident_ = id;
  }
  /**
   * Get PointCollectionKey identifier
   *
   * @return identifier
   */
  public String getId() {
    return ident_;
  }
  /**
   * Set line length.
   *
   * @param len line length
   */
  public void setLineLengthP(double len) {
    if(lineLengthP_ != len) {
      lineLengthP_ = len;
      modified("PointCollectionKey: setLineLengthP()");
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
  public void setColumns(int col) {
    if(columns_ != col) {
      columns_ = col;
      modified("PointCollectionKey: setColumms()");
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
  public void setBorderStyle(int style) {
    if(style_ != style) {
      style_ = style;
      modified("PointCollectionKey: setBorderStyle()");
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
  public void setAlign(int vert,int horz) {
    if(valign_ != vert || halign_ != horz) {
      valign_ = vert;
      halign_ = horz;
      modified("PointCollectionKey: setAlign()");
    }
  }
  /**
   * Set vertical alignment
   *
   * @param vert vertical alignment
   */
  public void setVAlign(int vert) {
    if(valign_ != vert) {
      valign_ = vert;
      modified("PointCollectionKey: setVAlign()");
    }
  }
  /**
   * Set horizontal alignment
   *
   * @param horz horizontal alignment
   */
  public void setHAlign(int horz) {
    if(halign_ != horz) {
      halign_ = horz;
      modified("PointCollectionKey: setHAlign()");
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
   * Set location of key
   * <BR><strong>Property Change:</strong> <code>location</code>.
   *
   * @param loc key location
   */
  public void setLocationP(Point2D.Double loc) {
    if(porigin_ == null || !porigin_.equals(loc)) {
      Point2D.Double temp = porigin_;
      porigin_ = loc;
      changes_.firePropertyChange("location",
          temp,
          porigin_);
      modified("PointCollectionKey: setLocationP()");
    }
  }
  /**
   * Set the bounds, in physical units, of the <code>PointCollectionKey</code>
   */
  public void setBoundsP(Rectangle2D.Double r) {
    setLocationP(new Point2D.Double(r.x, r.y));
  }
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
   * Add a PointCartesianRenderer and label to the PointCollectionKey.
   *
   * @param line PointCartesianRenderer object
   * @param label descriptive label
   */
  public void addPointGraph(PointCartesianRenderer points, SGLabel label) {
    points_.addElement(points);
    label.setLayer(layer_);
    label.setMoveable(false);
    label.setSelectable(false);
    label_.addElement(label);
    ((PointAttribute)points.getAttribute()).addPropertyChangeListener(this);
    modified("PointCollectionKey: addPointGraph()");
  }
  /**
   * Add a PointCartesianRenderer and label to the PointCollectionKey.
   *
   * @param rend CartesianRenderer object
   * @param label descriptive label
   * @since 3.0
   */
  public void addGraph(CartesianRenderer rend, SGLabel label)
      throws IllegalArgumentException {
    if(!(rend instanceof PointCartesianRenderer))
      throw new IllegalArgumentException("Renderer is not a PointCartesianRenderer");
    addPointGraph((PointCartesianRenderer)rend, label);
  }
  /**
   * Remove a line from the PointCollectionKey.
   *
   */
  public void removePointGraph(SGLabel label) {
  }
  /**
   * Remove a line from the PointCollectionKey.
   *
   */
  public void removePointRenderer(PointCartesianRenderer line) {
  }
  /**
   * Remove a line from the PointCollectionKey.
   *
   */
  public void removePointGraph(String ident) {
  }
  /**
   * Remove all lines from the PointCollectionKey.
   */
  public void clearAll() {
    PointAttribute attr;
    for(Enumeration e = points_.elements(); e.hasMoreElements(); ) {
      attr = (PointAttribute)((PointCartesianRenderer)e.nextElement()).getAttribute();
      attr.removePropertyChangeListener(this);
    }
    points_.removeAllElements();
    label_.removeAllElements();
    modified("PointCollectionKey: clearAll()");
  }
  /**
   * Remove data from key by id.
   */
  public void clear(String data_id) {
    PointCartesianRenderer pcr;
    int indx = -1;
    for(Enumeration it = points_.elements(); it.hasMoreElements();) {
      indx++;
      pcr = (PointCartesianRenderer)it.nextElement();
//        if(pcr.getLine().getId().equals(data_id)) {
//  	pcr.getAttribute().removePropertyChangeListener(this);
//  	points_.removeElement(lcr);
//  	label_.removeElementAt(indx);
//  	modified("PointCollectionKey: clear()");
//  	break;
//        }
    }
  }
  /**
   * Return height of key row in pixels.
   */
  public int getRowHeight() {
    Rectangle bounds;
    bounds = getBounds();
    return ROW_SPACE_ + maxLabelHeight_;
  }
  /**
   * Draw the Key.
   */
  public void draw(Graphics g) {
    double maxLabelLength, maxLabelHeight;
    int numLines, numRows, i, lineLength;
    int col, row, ytemp;
    double xloc, labelSpace;
    double[] xp, yp;
    int[] xd, yd;
    int[] xout, yout;
    Rectangle bounds;
    PointCartesianRenderer render = null;
    SGLabel label;
    PointAttribute attr = null;
    //
    numLines = points_.size();
    if((numLines <= 0) || !visible_) return;

    numRows = numLines/columns_;
    if(numLines%columns_ != 0) numRows++;

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
    for(i=1; i < numRows; i++) {
      yd[i] = yd[i-1] + ROW_SPACE_ + maxLabelHeight_;
      yp[i] = layer_.getYDtoP(yd[i]);
    }
    xd[0] = bounds.x + HORIZONTAL_BORDER_;
    xp[0] = layer_.getXDtoP(xd[0]);
    for(i=1; i < columns_; i++) {
      xd[i] = xd[i-1] + COLUMN_SPACE_ + lineLength + LABEL_SPACE_ + maxLabelLength_;
      xp[i] = layer_.getXDtoP(xd[i]);
    }
    //
    row = 0;
    col = 0;
    Object obj;
    Enumeration labelIt = label_.elements();
    for(Enumeration lineIt = points_.elements(); lineIt.hasMoreElements();) {
      obj = lineIt.nextElement();
      render = (PointCartesianRenderer)obj;
      attr = (PointAttribute)render.getAttribute();
      label = (SGLabel)labelIt.nextElement();
      //
      // draw line
      //
      g.setColor(attr.getColor());
      xout[0] = xd[col];
      xout[1] = xout[0] + lineLength;
      yout[0] = yd[row] - maxLabelHeight_/2;
      yout[1] = yout[0];

      // hack because mark is a little too high
      int ymark = yout[0]
;
      PlotMark pm = new PlotMark(attr);
      pm.setMarkHeightP(label.getHeightP());
      pm.paintMark(g, layer_, xout[0], ymark);
      pm.paintMark(g, layer_, xout[1], ymark);
      //
      xloc = xp[col] + lineLengthP_ + labelSpace;
      label.setLocationP(new Point2D.Double(xloc, yp[row]));
      try {
        label.draw(g);
      } catch (SGException e) {}
      //
      col++;
      if(col >= columns_) {
        col = 0;
        row++;
      }
    }
    switch(style_) {
    case PLAIN_LINE:
      g.drawRect(bounds.x, bounds.y, bounds.width-1, bounds.height-1);
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
  public Rectangle getBounds() {
    int lineLength;
    int numLines, rows;
    int x, y, height, width;

    numLines = points_.size();
    if(numLines <= 0) return new Rectangle(0, 0, 0, 0);
    //
    // find longest label
    //
    maxLabelLength_ = 0;
    maxLabelHeight_ = 0;
    SGLabel label;
    for(Enumeration it = label_.elements(); it.hasMoreElements();) {
      label = (SGLabel)it.nextElement();
      Rectangle sz = label.getBounds();
      maxLabelLength_ = Math.max(maxLabelLength_, sz.width);
      maxLabelHeight_ = Math.max(maxLabelHeight_, sz.height);
    }
    //
    rows = numLines/columns_;
    if(numLines%columns_ != 0) rows++;
    lineLength = layer_.getXPtoD(lineLengthP_) - layer_.getXPtoD(0.0f);
    width = 2*HORIZONTAL_BORDER_ +
      columns_*(lineLength + LABEL_SPACE_ + maxLabelLength_) +
      (columns_ - 1)*COLUMN_SPACE_;
    height = 2*VERTICAL_BORDER_ + rows*maxLabelHeight_ +
      (rows-1)*ROW_SPACE_;
    // temp fudge
    height = height + 5;
    //
    x = layer_.getXPtoD(porigin_.x);
    y = layer_.getYPtoD(porigin_.y);
    switch(halign_) {
    case RIGHT:
      x = x - width;
      break;
    case CENTER:
      x = x - width/2;
    }
    switch(valign_) {
    case BOTTOM:
      y = y - height;
      break;
    case MIDDLE:
      y = y - height/2;
    }
    return new Rectangle(x, y, width, height);
  }
  public Point getLocation() {
    Rectangle bnds = getBounds();
    return new Point(bnds.x, bnds.y);
  }
  public void setLocation(Point loc) {
    Rectangle bnds = getBounds();
    setBounds(loc.x, loc.y, bnds.width, bnds.height);
  }
  /**
   * Set the bounds, in pixels, of the <code>PointCollectionKey</code>
   */
  public void setBounds(Rectangle r) {
    setBounds(r.x, r.y, r.width, r.height);
  }
  /**
   * Set the bounds, in pixels, of the <code>PointCollectionKey</code>
   * <BR><strong>Property Change:</strong> <code>location</code>.
   */
  public void setBounds(int x, int y, int width, int height) {
    switch(halign_) {
    case RIGHT:
      x = x + width;
      break;
    case CENTER:
      x = x + width/2;
    }
    switch(valign_) {
    case BOTTOM:
      y = y + height;
      break;
    case MIDDLE:
      y = y + height/2;
    }
    double xp = layer_.getXDtoP(x);
    double yp = layer_.getYDtoP(y);
    if(porigin_.x != xp || porigin_.y != yp) {
      Point2D.Double temp = porigin_;
      porigin_.x = xp;
      porigin_.y = yp;
      changes_.firePropertyChange("location",
          temp,
          new Point2D.Double(xp, yp));
      modified("PointCollectionKey: setBounds()");
    }
  }
  Object getObjectAt(Point pt) {
    Rectangle lbnds;
    Rectangle bounds;
    PointCartesianRenderer point;
    int[] xout, yout;
    int[] xd, yd;
    int numLines, numRows;
    int lineLength;
    double labelSpace;
    int i;

    numLines = points_.size();
    if(numLines <= 0) return null;

    numRows = numLines/columns_;
    if(numLines%columns_ != 0) numRows++;

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
    for(i=1; i < numRows; i++) {
      yd[i] = yd[i-1] + ROW_SPACE_ + maxLabelHeight_;
    }
    xd[0] = bounds.x + HORIZONTAL_BORDER_;
    for(i=1; i < columns_; i++) {
      xd[i] = xd[i-1] + COLUMN_SPACE_ + lineLength + LABEL_SPACE_ + maxLabelLength_;
    }
    // loop over all the lines
    int row = 0;
    int col = 0;
    for(Enumeration lineIt = points_.elements(); lineIt.hasMoreElements();) {
      point = (PointCartesianRenderer)lineIt.nextElement();
      xout[0] = xd[col];
//        xout[1] = xout[0] + lineLength + LABEL_SPACE_ + maxLabelLength_;
      xout[1] = xout[0] + lineLength + LABEL_SPACE_;
      yout[0] = yd[row] - maxLabelHeight_;
      yout[1] = yd[row];
      lbnds = new Rectangle(xout[0], yout[0],
                            xout[1] - xout[0],
                            yout[1] - yout[0]);
      if(lbnds.contains(pt)) {
        return point;
      }
      //
      col++;
      if(col >= columns_) {
        col = 0;
        row++;
      }
    }
    if(bounds.contains(pt)) {
      return this;
    }
    return null;
  }
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".")+1) + ": " + ident_;
  }
  public boolean isVisible() {
    return visible_;
  }
  public void setVisible(boolean visible) {
    if(visible_ != visible) {
      visible_ = visible;
      modified("PointCollectionKey: setVisible()");
    }
  }
  public void propertyChange(PropertyChangeEvent evt) {
//      if(Debug.EVENT) {
//        System.out.println("PointCollectionKey: " + evt);
//        System.out.println("         " + evt.getPropertyName());
//      }
    modified("PointCollectionKey: propertyChange(" +
       evt.getSource().toString() + "[" +
       evt.getPropertyName() + "]" + ")");
  }
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
