/*
 * $Id: VectorKey.java,v 1.14 2003/08/22 23:02:32 dwd Exp $
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
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <code>VectorKey</code> is used to create a key for the <code>VectorCartesianRenderer</code>.
 * Multiple lines can be included in the key.
 *
 * @author Donald Denbo
 * @version $Revision: 1.14 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.1
 */
public class VectorKey implements Cloneable, DataKey, Moveable, PropertyChangeListener {
  private String ident_;

  /**
   * @directed
   */
  private Layer layer_;

  /**
   * @link aggregation
   * @supplierCardinality *
   * @label vectors
   */
  /*#VectorCartesianRenderer lnkVectorCartesianRenderer; */
  private Vector vectors_;

  /**
   * @link aggregation
   * @associates <{SGLabel}>
   * @label label
   * @supplierCardinality *
   * @undirected
   */
  /* private Vector lnkSGLabel; */
  private Vector label_;

  private Vector scaleLabel_;
  private int columns_;
  private int style_;
  private int valign_;
  private int halign_;
  private Point2D.Double porigin_;
  private double vectorLengthP_;
  private int maxLabelLength_;
  private int maxLabelHeight_;
  private int maxScaleLength_;
  private String floatScaleFormat_ = "###.##";
  private String expScaleFormat_ = "0.####E0";
  private DecimalFormat floatFormat_;
  private DecimalFormat expFormat_;
  private float xoff_;
  private float yoff_;
  private float slope_;
  private boolean selected_;
  private boolean selectable_;
  private boolean visible_;
  private boolean moveable_;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  private static final int VERTICAL_BORDER_ = 3;
  private static final int HORIZONTAL_BORDER_ = 15;
  private static final int COLUMN_SPACE_ = 10;
  private static final int ROW_SPACE_ = 3;
  private static final int LABEL_SPACE_ = 10;
  private static final int SCALE_SPACE_ = 7;

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
      if (vectors_ != null) {
        vectors_.clear();
        vectors_ = null;
      }
      if (label_ != null) {
        label_.clear();
        label_ = null;
      }
      if (scaleLabel_ != null) {
        scaleLabel_.clear();
        scaleLabel_ = null;
      }
      changes_ = null;
      if (JPane.debug) String2.log("sgt.VectorKey.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public VectorKey() {
    this(new Point2D.Double(0.0, 0.0), BOTTOM, LEFT);
  }

  /** Create <code>VectorKey</code>. */
  public VectorKey(Point2D.Double loc, int valign, int halign) {
    porigin_ = loc;
    valign_ = valign;
    halign_ = halign;
    vectors_ = new Vector(2, 2);
    label_ = new Vector(2, 2);
    scaleLabel_ = new Vector(2, 2);
    //
    // set defaults
    //
    style_ = PLAIN_LINE;
    columns_ = 1;
    ident_ = "";
    vectorLengthP_ = 0.3f;
    selected_ = false;
    selectable_ = true;
    visible_ = true;
    moveable_ = true;

    floatFormat_ = new DecimalFormat(floatScaleFormat_);
    expFormat_ = new DecimalFormat(expScaleFormat_);
  }

  /** Create of copy of VectorKey. */
  @Override
  public LayerChild copy() {
    VectorKey newKey;
    try {
      newKey = (VectorKey) clone();
    } catch (CloneNotSupportedException e) {
      newKey = new VectorKey();
    }
    newKey.vectors_ = new Vector(2, 2);
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
   * Set VectorKey identifier.
   *
   * @param id key identifier
   */
  @Override
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Get VectorKey identifier
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
  public void setVectorLengthP(double len) {
    if (vectorLengthP_ != len) {
      vectorLengthP_ = len;
      modified("VectorKey: setVectorLengthP()");
    }
  }

  /**
   * @since 3.0
   */
  @Override
  public void setLineLengthP(double len) {
    setVectorLengthP(len);
  }

  /**
   * Get line length
   *
   * @return line length
   */
  public double getVectorLengthP() {
    return vectorLengthP_;
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
      modified("VectorKey: setColumms()");
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
      modified("VectorKey: setBorderStyle()");
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
      modified("VectorKey: setAlign()");
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
      modified("VectorKey: setVAlign()");
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
      modified("VectorKey: setHAlign()");
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
      modified("VectorKey: setLocationP()");
    }
  }

  /** Set the bounds, in physical units, of the <code>VectorKey</code> */
  @Override
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
   * Add a VectorCartesianRenderer and label to the VectorKey.
   *
   * @param line VectorCartesianRenderer object
   * @param label descriptive label
   */
  public void addVectorGraph(VectorCartesianRenderer vector, SGLabel label) {
    vectors_.addElement(vector);
    label.setLayer(layer_);
    label.setMoveable(false);
    label.setSelectable(false);
    label_.addElement(label);
    SGLabel scale =
        new SGLabel(
            "scale label",
            " ",
            label.getHeightP(),
            label.getLocationP(),
            label.getVAlign(),
            label.getHAlign());
    scale.setFont(label.getFont());
    scale.setLayer(label.getLayer());
    scale.setMoveable(false);
    scale.setSelectable(false);
    scaleLabel_.addElement(scale);
    ((VectorAttribute) vector.getAttribute()).addPropertyChangeListener(this);
    modified("VectorKey: addVectorGraph()");
  }

  /**
   * Add a VectorCartesianRenderer and label to the VectorKey.
   *
   * @param rend CartesianRenderer object
   * @param label descriptive label
   * @since 3.0
   */
  @Override
  public void addGraph(CartesianRenderer rend, SGLabel label) throws IllegalArgumentException {
    if (!(rend instanceof VectorCartesianRenderer))
      throw new IllegalArgumentException("Renderer is not a VectorCartesianRenderer");
    addVectorGraph((VectorCartesianRenderer) rend, label);
  }

  /** Remove a line from the VectorKey. */
  public void removeVectorGraph(SGLabel label) {}

  /** Remove a line from the VectorKey. */
  public void removeVectorRenderer(VectorCartesianRenderer line) {}

  /** Remove a line from the VectorKey. */
  public void removeVectorGraph(String ident) {}

  /** Remove all lines from the VectorKey. */
  public void clearAll() {
    VectorAttribute attr;
    for (Enumeration e = vectors_.elements(); e.hasMoreElements(); ) {
      attr = (VectorAttribute) ((VectorCartesianRenderer) e.nextElement()).getAttribute();
      attr.removePropertyChangeListener(this);
    }
    vectors_.removeAllElements();
    label_.removeAllElements();
    modified("VectorKey: clearAll()");
  }

  /** Remove data from key by id. */
  public void clear(String data_id) {
    VectorCartesianRenderer vcr;
    int indx = -1;
    for (Enumeration it = vectors_.elements(); it.hasMoreElements(); ) {
      indx++;
      vcr = (VectorCartesianRenderer) it.nextElement();
      //        if(pcr.getLine().getId().equals(data_id)) {
      //  	pcr.getAttribute().removePropertyChangeListener(this);
      //  	points_.removeElement(lcr);
      //  	label_.removeElementAt(indx);
      //  	modified("VectorKey: clear()");
      //  	break;
      //        }
    }
  }

  /** Return height of key row in pixels. */
  public int getRowHeight() {
    Rectangle bounds;
    bounds = getBounds();
    return ROW_SPACE_ + maxLabelHeight_;
  }

  /** Draw the Key. */
  @Override
  public void draw(Graphics g) {
    double maxLabelLength, maxLabelHeight;
    int numLines, numRows, i;
    float vectorLength;
    float vectorLengthU;
    int col, row, ytemp;
    double xloc, labelSpace, scaleSpace, scaleLength;
    double[] xp, yp;
    int[] xd, yd;
    int[] xout, yout;
    float hx1, hx2;
    float hy1, hy2;
    float headX, headY;
    float orgX, orgY;
    float xphead, yphead;
    float tScale, fixedScale;
    float scale;
    float headScale;
    float minSize;
    float maxSize;
    Rectangle bounds;
    VectorCartesianRenderer render = null;
    SGLabel label;
    SGLabel scaleLabel;
    String scaleStr;
    VectorAttribute attr = null;
    Graphics2D g2 = (Graphics2D) g;
    GeneralPath gp;
    Stroke savedStroke = g2.getStroke();
    BasicStroke stroke;
    slope_ = (float) layer_.getXSlope();
    xoff_ = (float) layer_.getXOffset();
    yoff_ = (float) layer_.getYOffset();
    //
    numLines = vectors_.size();
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
    vectorLength = xPtoD((float) vectorLengthP_) - xPtoD(0.0f);
    labelSpace = layer_.getXDtoP(LABEL_SPACE_) - layer_.getXDtoP(0);
    scaleLength = layer_.getXDtoP(maxScaleLength_) - layer_.getXDtoP(0);
    scaleSpace = layer_.getXDtoP(SCALE_SPACE_) - layer_.getXDtoP(0);
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
      xd[i] =
          xd[i - 1]
              + COLUMN_SPACE_
              + (int) vectorLength
              + SCALE_SPACE_
              + maxScaleLength_
              + LABEL_SPACE_
              + maxLabelLength_;
      xp[i] = layer_.getXDtoP(xd[i]);
    }
    //
    row = 0;
    col = 0;
    Object obj;
    Enumeration labelIt = label_.elements();
    Enumeration scaleIt = scaleLabel_.elements();
    for (Enumeration lineIt = vectors_.elements(); lineIt.hasMoreElements(); ) {
      obj = lineIt.nextElement();
      render = (VectorCartesianRenderer) obj;
      attr = (VectorAttribute) render.getAttribute();
      label = (SGLabel) labelIt.nextElement();
      scaleLabel = (SGLabel) scaleIt.nextElement();
      //
      // draw line
      //
      g2.setColor(attr.getVectorColor());
      stroke =
          new BasicStroke(
              attr.getWidth(), attr.getCapStyle(), attr.getMiterStyle(), attr.getMiterLimit());
      scale = (float) attr.getVectorScale();
      headScale = (float) attr.getHeadScale() * 0.94386f;
      fixedScale = (float) (headScale * attr.getHeadFixedSize());
      minSize = (float) attr.getHeadMinSize();
      maxSize = (float) attr.getHeadMaxSize();
      vectorLengthU = (float) (vectorLengthP_ / scale);
      g2.setStroke(stroke);
      gp = new GeneralPath();
      orgX = (float) xd[col];
      orgY = (float) (yd[row] - maxLabelHeight_ / 2);
      headX = orgX + vectorLength;
      headY = orgY;
      xphead = xDtoP(headX);
      yphead = yDtoP(headY);
      gp.moveTo(orgX, orgY);
      gp.lineTo(headX, headY);
      //
      // add head
      //
      if (vectorLength == 0.0) {
        g.drawLine((int) headX, (int) headY, (int) headX, (int) headY);
      } else {
        if (attr.getVectorStyle() != VectorAttribute.NO_HEAD) {
          if (attr.getVectorStyle() == VectorAttribute.HEAD) {
            // unscaled head
            tScale = fixedScale;
            hx1 = xPtoD(xphead - tScale);
            hy1 = yPtoD(yphead + 0.35f * tScale);
            hx2 = xPtoD(xphead - tScale);
            hy2 = yPtoD(yphead - 0.35f * tScale);
            gp.moveTo(hx1, hy1);
            gp.lineTo(headX, headY);
            gp.lineTo(hx2, hy2);
          } else {
            // scaled head
            if (vectorLengthP_ >= maxSize) {
              tScale = maxSize * headScale;
            } else if (vectorLengthP_ <= minSize) {
              tScale = minSize * headScale;
            } else {
              tScale = (float) (vectorLengthP_ * headScale);
            }
            hx1 = xPtoD(xphead - tScale);
            hy1 = yPtoD(yphead + 0.35f * tScale);
            hx2 = xPtoD(xphead - tScale);
            hy2 = yPtoD(yphead - 0.35f * tScale);
            gp.moveTo(hx1, hy1);
            gp.lineTo(headX, headY);
            gp.lineTo(hx2, hy2);
          }
        }
      }

      g2.draw(gp);
      g2.setStroke(savedStroke);
      //
      // draw mark
      //
      xout[0] = (int) orgX;
      yout[0] = (int) orgY;
      if (attr.getOriginStyle() == VectorAttribute.MARK) {
        g.setColor(attr.getMarkColor());
        render.drawMark(g, xout, yout, 1, attr);
      }
      //
      // scale label
      //
      xloc = xp[col] + vectorLengthP_ + scaleSpace;
      if (vectorLengthU > 1000.0 || vectorLengthU < 0.01) {
        scaleStr = expFormat_.format(vectorLengthU);
      } else {
        scaleStr = floatFormat_.format(vectorLengthU);
      }
      scaleLabel.setText(scaleStr);
      scaleLabel.setLocationP(new Point2D.Double(xloc, yp[row]));

      //
      // user label
      //
      xloc = xloc + scaleLength + labelSpace;
      label.setLocationP(new Point2D.Double(xloc, yp[row]));
      try {
        scaleLabel.draw(g);
        label.draw(g);
      } catch (SGException e) {
        System.out.println(e);
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

  private float xPtoD(float xp) {
    return slope_ * xp + xoff_;
  }

  private float yPtoD(float yp) {
    return yoff_ - slope_ * yp;
  }

  private float xDtoP(float xd) {
    return (xd - xoff_) / slope_;
  }

  private float yDtoP(float yd) {
    return (yoff_ - yd) / slope_;
  }

  /**
   * Get the bounding rectangle.
   *
   * @return bounding rectangle
   */
  @Override
  public Rectangle getBounds() {
    int vectorLength;
    int numLines, rows;
    int x, y, height, width;

    numLines = vectors_.size();
    if (numLines <= 0) return new Rectangle(0, 0, 0, 0);
    //
    // find longest label and scale
    //
    maxLabelLength_ = 0;
    maxLabelHeight_ = 0;
    maxScaleLength_ = 0;
    Object obj;
    VectorCartesianRenderer render;
    VectorAttribute attr;
    double vectorLengthU;
    Enumeration lineIt = vectors_.elements();
    Enumeration scaleIt = scaleLabel_.elements();
    SGLabel label;
    String scaleStr;
    SGLabel scaleLabel;
    for (Enumeration it = label_.elements(); it.hasMoreElements(); ) {
      label = (SGLabel) it.nextElement();
      Rectangle sz = label.getBounds();
      maxLabelLength_ = Math.max(maxLabelLength_, sz.width);
      maxLabelHeight_ = Math.max(maxLabelHeight_, sz.height);
      //
      obj = lineIt.nextElement();
      render = (VectorCartesianRenderer) obj;
      attr = (VectorAttribute) render.getAttribute();
      vectorLengthU = vectorLengthP_ / attr.getVectorScale();
      if (vectorLengthU > 1000.0 || vectorLengthU < 0.01) {
        scaleStr = expFormat_.format(vectorLengthU);
      } else {
        scaleStr = floatFormat_.format(vectorLengthU);
      }
      scaleLabel = (SGLabel) scaleIt.nextElement();
      scaleLabel.setText(scaleStr);
      sz = scaleLabel.getBounds();
      maxScaleLength_ = Math.max(maxScaleLength_, sz.width);
    }
    //
    rows = numLines / columns_;
    if (numLines % columns_ != 0) rows++;
    vectorLength = layer_.getXPtoD(vectorLengthP_) - layer_.getXPtoD(0.0f);
    width =
        2 * HORIZONTAL_BORDER_
            + columns_
                * (vectorLength + SCALE_SPACE_ + maxScaleLength_ + LABEL_SPACE_ + maxLabelLength_)
            + (columns_ - 1) * COLUMN_SPACE_;
    height = 2 * VERTICAL_BORDER_ + rows * maxLabelHeight_ + (rows - 1) * ROW_SPACE_;
    // temp fudge
    height = height + 5;
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

  /** Set the bounds, in pixels, of the <code>VectorKey</code> */
  public void setBounds(Rectangle r) {
    setBounds(r.x, r.y, r.width, r.height);
  }

  /**
   * Set the bounds, in pixels, of the <code>VectorKey</code> <br>
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
      modified("VectorKey: setBounds()");
    }
  }

  Object getObjectAt(Point pt) {
    Rectangle lbnds;
    Rectangle bounds;
    VectorCartesianRenderer vector;
    int[] xout, yout;
    int[] xd, yd;
    int numLines, numRows;
    int vectorLength;
    double labelSpace, scaleSpace;
    int i;

    numLines = vectors_.size();
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
    vectorLength = layer_.getXPtoD(vectorLengthP_) - layer_.getXPtoD(0.0);
    labelSpace = layer_.getXDtoP(LABEL_SPACE_) - layer_.getXDtoP(0);
    scaleSpace = layer_.getXDtoP(SCALE_SPACE_) - layer_.getXDtoP(0);
    //
    yd[0] = bounds.y + VERTICAL_BORDER_ + maxLabelHeight_;
    for (i = 1; i < numRows; i++) {
      yd[i] = yd[i - 1] + ROW_SPACE_ + maxLabelHeight_;
    }
    xd[0] = bounds.x + HORIZONTAL_BORDER_;
    for (i = 1; i < columns_; i++) {
      xd[i] =
          xd[i - 1]
              + COLUMN_SPACE_
              + vectorLength
              + SCALE_SPACE_
              + maxScaleLength_
              + LABEL_SPACE_
              + maxLabelLength_;
    }
    // loop over all the lines
    int row = 0;
    int col = 0;
    for (Enumeration lineIt = vectors_.elements(); lineIt.hasMoreElements(); ) {
      vector = (VectorCartesianRenderer) lineIt.nextElement();
      xout[0] = xd[col];
      //        xout[1] = xout[0] + vectorLength + 2*LABEL_SPACE_ +
      //                  maxScaleLenght_ + maxLabelLength_;
      xout[1] = xout[0] + vectorLength + SCALE_SPACE_;
      yout[0] = yd[row] - maxLabelHeight_;
      yout[1] = yd[row];
      lbnds = new Rectangle(xout[0], yout[0], xout[1] - xout[0], yout[1] - yout[0]);

      if (lbnds.contains(pt)) {
        return vector;
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
      modified("VectorKey: setVisible()");
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    //      if(Debug.EVENT) {
    //        System.out.println("VectorKey: " + evt);
    //        System.out.println("         " + evt.getPropertyName());
    //      }
    modified(
        "VectorKey: propertyChange("
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
