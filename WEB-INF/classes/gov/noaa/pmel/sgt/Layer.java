/*
 * $Id: Layer.java,v 1.25 2003/09/03 22:50:51 dwd Exp $
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
import gov.noaa.pmel.sgt.swing.Draggable;
import gov.noaa.pmel.util.Debug;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

// jdk1.2
// import  java.awt.geom.Rectangle2D;

/**
 * A <code>Layer</code> contains a single <code>Graph</code> object and multiple <code>LayerChild
 * </code> objects. There can be many <code>Layer</code> objects associated with each <code>Pane
 * </code> object and the <code>Layer</code> objects can share <code>Transform</code> and <code>Axis
 * </code> objects, but are not required to. The <code>Layer</code> is also where keys related to
 * <code>Color</code>, Vectors, and Lines are attached. The can be at most one key of each type
 * attached to a <code>Layer</code>.
 *
 * <p>The <code>Layer</code> object transforms physical coordinates to device coordinates. All
 * objects that attach to a <code>Layer</code> use physical coordinates. The exception to this is
 * the <code>Graph</code> object (and its children), since these objects transform user coordinates
 * to physical coordinates.
 *
 * <p>The following is a simple example of using the <code>Pane</code>, <code>Layer</code>, and
 * <code>SGLabel</code> objects together. In this example, the <code>Pane</code> and <code>Layer
 * </code> objects are created such that, in the absence of any resizing, 100 pixels is equal to 1.0
 * physical units. Two labels are created, the first contains the current time and is located in the
 * bottom left of the <code>Layer</code>. The second label is a title that is positioned near the
 * top and centered.
 *
 * <pre>
 * Pane pane;
 * Layer layer;
 * SGLabel title;
 * SGLabel label;
 * GeoDate stime;
 * ...
 * //
 * // Instantiate Pane, Layer, and GeoDate objects.
 * //
 * pane = new Pane("test pane", new Dimension(400, 300));
 * pane.setLayout(new StackedLayout());
 * layer = new Layer("Test Layer", new Dimension2D(4.0, 3.0));
 * stime = new GeoDate();
 * //
 * // Instatiate an SGLabel object as label, set its text to the
 * // current time and position it near the lower-left corner
 * // of the layer.
 * //
 * label = new SGLabel("test", stime.toString(), new Point2D.Double(0.05, 0.05));
 * //
 * // Set properties for label.
 * //
 * label.setAlign(SGLabel.BOTTOM, SGLabel.LEFT);
 * label.setColor(Color.magenta);
 * label.setHeightP(0.15);
 * label.setFont(new Font("Dialog", Font.PLAIN, 10));
 * //
 * // Add label to layer.
 * //
 * layer.addChild(label);
 * //
 * // Instatiate an SGLabel object as title, set its text and position
 * // it near the top of the layer and centered. Set the properties
 * // for title.
 * //
 * title = new SGLabel("title", "SciGraph Test!", new Point2D.Double(2.125, 2.9));
 * title.setAlign(SGLabel.TOP, SGLabel.CENTER);
 * title.setHeightP(0.25);
 * title.setFont(new Font("Helvetica", Font.BOLD, 14));
 * //
 * // Add title to layer and add layer to pane.
 * //
 * layer.addChild(title);
 * pane.add(layer);
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.25 $, $Date: 2003/09/03 22:50:51 $
 * @since 1.0
 * @see Pane
 * @see Graph
 * @see ColorKey
 * @see SGLabel
 * @see LineKey
 * @see gov.noaa.pmel.util.GeoDate
 */
public class Layer extends Component implements Cloneable, LayerControl {
  private String ident_;

  /**
   * @shapeType AggregationLink
   * @clientCardinality 1
   * @label graph
   */
  private Graph graph_;

  /**
   * @shapeType AggregationLink
   * @associates <strong>LayerChild</strong>
   * @supplierCardinality 0..*
   * @undirected
   * @label children
   */
  private Vector children_;

  private double pWidth_;
  private double pHeight_;
  private double ax_;
  private double ay_;
  private int xoff_;
  private int yoff_;
  private double xoff2_;
  private double yoff2_;
  protected AbstractPane pane_;

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception {
    try {
      if (graph_ != null) {
        Graph o = graph_; // done this way to avoid infinite loop
        graph_ = null;
        o.releaseResources();
      }
      if (children_ != null) {
        Vector o = children_; // done this way to avoid infinite loop
        children_ = null;
        for (Object o2 : o) ((LayerChild) o2).releaseResources();
        o.clear();
      }
      pane_ = null; // not releaseResources, else infinite loop

      if (JPane.debug) String2.log("sgt.Layer.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  private void computeScale() {
    Dimension d;
    boolean hasG2 = getGraphics() instanceof Graphics2D;
    // compute xoff and yoff as double then truncate to int
    Rectangle pbnds = pane_.getBounds();
    Rectangle bnds = pbnds; // getBounds();
    if (pane_.isPrinter()) {
      ax_ = 72; // java2 is in 1/72 of an inch
      ay_ = ax_;
      xoff2_ = (bnds.width - ax_ * pWidth_) / 2.0 + bnds.x;
      yoff2_ = bnds.height - (bnds.height - ay_ * pHeight_) / 2.0 + bnds.y;
    } else {
      // not printer
      ax_ = (double) bnds.width / pWidth_;
      ay_ = (double) bnds.height / pHeight_;
      if (ax_ > ay_) {
        ax_ = ay_;
      } else if (ay_ > ax_) {
        ay_ = ax_;
      }
      xoff2_ = (bnds.width - ax_ * pWidth_) / 2.0 + bnds.x - pbnds.x;
      yoff2_ = bnds.height - (bnds.height - ay_ * pHeight_) / 2.0 + bnds.y - pbnds.y;
    }

    // bob added:
    // System.out.println("Layer.computerScale xoff2_="+xoff2_+" yoff2_="+yoff2_+" ax_="+ax_+"
    // ay_="+ay_);

    xoff_ = (int) xoff2_;
    yoff_ = (int) yoff2_;
    if (Debug.DEBUG && pane_.isPrinter()) {
      System.out.println("Layer.computeScale[" + getId() + "] printer = " + pane_.isPrinter());
      System.out.println("                  xd(min) = " + getXPtoD(0.0));
      System.out.println("                  xd(max) = " + getXPtoD(pWidth_));
      System.out.println("                  yd(min) = " + getYPtoD(0.0));
      System.out.println("                  yd(max) = " + getYPtoD(pHeight_));
    }
  }

  /**
   * Set the size of the <code>Layer</code> in device units.
   *
   * @param sze dimension of the <code>Layer</code>
   */
  @Override
  public void setSize(Dimension sze) {
    super.setSize(sze);
    computeScale();
    modified("Layer: setSize(Dimension)");
  }

  /**
   * Set the size of the <code>Layer</code> in device units.
   *
   * @param w width of the <code>Layer</code>
   * @param h height of the <code>Layer</code>
   */
  @Override
  public void setSize(int w, int h) {
    super.setSize(w, h);
    computeScale();
    modified("Layer: setSize(int,int)");
  }

  /**
   * Set the location of the <code>Layer</code> in device units.
   *
   * @param pt location of the <code>Layer</code>
   */
  @Override
  public void setLocation(Point pt) {
    super.setLocation(pt);
    computeScale();
    modified("Layer: setLocation(Point)");
  }

  /**
   * Set the location of the <code>Layer</code> in device units.
   *
   * @param x horizontal location of the <code>Layer</code>
   * @param y vertical location of the <code>Layer</code>
   */
  @Override
  public void setLocation(int x, int y) {
    super.setLocation(x, y);
    computeScale();
    modified("Layer: setLocation(int,int)");
  }

  /**
   * Set the bounds of the <code>Layer</code> in device units.
   *
   * @param x horizontal location of the <code>Layer</code>
   * @param y vertical location of the <code>Layer</code>
   * @param w width of the <code>Layer</code>
   * @param h height of the <code>Layer</code>
   */
  @Override
  public void setBounds(int x, int y, int w, int h) {
    super.setBounds(x, y, w, h);
    computeScale();
    //    System.out.println("Layer.setBounds(" + x + ", " + y + ", " +
    //                       w + ", " + h + ")");
    modified("Layer: setBounds(int,int,int,int)");
  }

  /**
   * Set the bounds of the <code>Layer</code> in device units.
   *
   * @param bnds bounds of the <code>Layer</code>
   */
  @Override
  public void setBounds(Rectangle bnds) {
    super.setBounds(bnds);
    computeScale();
    modified("Layer: setBounds(Rectangle)");
  }

  /**
   * Transform physical units to device for x coordinate.
   *
   * @param xp x physical coordinate
   * @return x device coordinate
   * @since 2.0
   */
  public int getXPtoD(double xp) {
    return (int) (ax_ * xp) + xoff_;
  }

  /**
   * Transform physcial units to device for y coordinate.
   *
   * @param yp y physical coordinate
   * @return y device coordinate
   * @since 2.0
   */
  public int getYPtoD(double yp) {
    // System.out.println("layer.getYPtoD yp="+yp);
    return yoff_ - (int) (ay_ * yp);
  }

  /**
   * Transform physical units to device for x coordinate.
   *
   * @param xp x physical coordinate
   * @return x device coordinate
   * @since 3.0
   */
  public double getXPtoD2(double xp) {
    return ax_ * xp + xoff2_;
  }

  /**
   * Transform physcial units to device for y coordinate.
   *
   * @param yp y physical coordinate
   * @return y device coordinate
   * @since 3.0
   */
  public double getYPtoD2(double yp) {
    return yoff2_ - ay_ * yp;
  }

  public double getXSlope() {
    return ax_;
  }

  public double getYSlope() {
    return ay_;
  }

  public double getXOffset() {
    return xoff2_;
  }

  public double getYOffset() {
    return yoff2_;
  }

  /**
   * Transform device units to physical for the x direction.
   *
   * @param xd device x coordinate
   * @return physical x coordinate
   */
  public double getXDtoP(int xd) {
    return (double) (xd - xoff2_) / ax_;
  }

  /**
   * Transform device units to physical for the y direction.
   *
   * @param yd device y coordinate
   * @return physical y coordinate
   */
  public double getYDtoP(int yd) {
    return (double) (yoff2_ - yd) / ay_;
  }

  /**
   * Create a <code>Layer</code> object. The <code>Layer</code> is created with a default width and
   * height equal to 1.0.
   *
   * @param id identifier for Layer
   */
  public Layer(String id) {
    this(id, new Dimension2D(1.0, 1.0));
  }

  /**
   * Create a <code>Layer</code> object. The <code>Layer</code> is created with the specified
   * dimensions and identifier.
   *
   * @param id identifier for Layer
   * @param psize The physical dimensions of the Layer
   */
  public Layer(String id, Dimension2D psize) {
    ident_ = id;
    pWidth_ = psize.width;
    pHeight_ = psize.height;
    children_ = new Vector(5, 5);
  }

  /**
   * Default constructor for <code>Layer</code>. The <code>Layer</code> is created with an empty
   * identifier and a width and height equal to 1.0f.
   */
  public Layer() {
    this("");
  }

  /**
   * Copy the <code>Layer</code> and its attached classes.
   *
   * @return copy
   */
  public Layer copy() {
    Layer newLayer;
    try {
      newLayer = (Layer) clone();
    } catch (CloneNotSupportedException e) {
      newLayer = new Layer(ident_, new Dimension2D(pWidth_, pHeight_));
    }
    //
    // copy children
    //
    newLayer.children_ = new Vector(5, 5);
    //
    if (!children_.isEmpty()) {
      LayerChild newChild;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        newChild = ((LayerChild) it.nextElement()).copy();
        newLayer.addChild(newChild);
      }
    }
    //
    // copy Graph
    //
    if (graph_ != (Graph) null) {
      Graph newGraph = graph_.copy();
      newLayer.setGraph(newGraph);
    }
    return newLayer;
  }

  /**
   * Draw the Layer and its attached classes.
   *
   * @param g graphics context
   * @exception PaneNotFoundException if a pane object is not found
   */
  @Override
  public void draw(Graphics g) throws PaneNotFoundException {
    if (pane_ == null) throw new PaneNotFoundException();
    computeScale();

    if (false) {
      System.out.println("\nLayer.draw(g): " + ident_);
      System.out.println("   layer.getBounds(" + ident_ + ") = " + getBounds());
      System.out.println("   layer.getBoundsP(" + ident_ + ") = " + getBoundsP());
      System.out.println("   pane.getBounds(" + pane_.getId() + ") = " + pane_.getBounds());
    }
    /*    int x0, y0, x1, y1;
    x0 = getXPtoD(0.0f);
    y0 = getYPtoD(0.0f);
    Rectangle2D.Double psize_ = getBoundsP();
    x1 = getXPtoD(psize_.width);
    y1 = getYPtoD(psize_.height);
    g.setColor(Color.blue);
    g.drawRect(x0,y1,x1-x0-1,y0-y1-1); */
    //    System.out.println("Layer.draw(g): " + ident_ + ", [" + ax_ + ", " + ay_ + "], [" +
    //                       xoff2_ + ", " + yoff2_ + "]");

    //
    // draw Graph
    //
    if (graph_ != (Graph) null) graph_.draw(g);
    //
    // draw children
    //
    if (!children_.isEmpty()) {
      LayerChild child;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        child = (LayerChild) it.nextElement();
        if (!(child instanceof Draggable)) {
          try {
            child.draw(g);
          } catch (LayerNotFoundException e) {
          }
        }
      }
    }
  }

  @Override
  public void drawDraggableItems(Graphics g) throws PaneNotFoundException {
    if (pane_ == null) throw new PaneNotFoundException();
    //
    // draw draggable items
    //
    if (!children_.isEmpty()) {
      LayerChild child;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        child = (LayerChild) it.nextElement();
        if (child instanceof Draggable) {
          try {
            child.draw(g);
          } catch (LayerNotFoundException e) {
          }
        }
      }
    }
  }

  /**
   * Associate a graph with the layer. Only one graph or its children may be attached to a layer.
   * Multiple graphs are created by using multiple layers.
   *
   * @param gr graph
   * @return True if attachment was succesful
   * @see Graph
   */
  public boolean setGraph(Graph gr) {
    graph_ = gr;
    if (graph_ != null) graph_.setLayer(this);
    modified("Layer: setGraph()");
    return true;
  }

  /**
   * Get the <code>Graph</code> attached to the layer.
   *
   * @return Reference to the <code>Graph</code>.
   */
  public Graph getGraph() {
    return graph_;
  }

  /**
   * Add a <code>LayerChild</code> to the <code>Layer</code>. Each <code>Layer</code> can contain as
   * many children as needed.
   *
   * @param child A <code>LayerChild</code>
   * @see SGLabel
   * @see LineKey
   * @see ColorKey
   * @see Ruler
   */
  public void addChild(LayerChild child) {
    child.setLayer(this);
    children_.addElement(child);
    modified("Layer: addChild()");
  }

  /**
   * Remove a <code>LayerChild</code> object from the <code>Layer</code>.
   *
   * @param child A <code>ChildLayer</code> object associated with the <code>Layer</code>
   * @exception ChildNotFoundException The child is not associated with the <code>Layer</code>
   * @see SGLabel
   * @see LineKey
   * @see ColorKey
   * @see Ruler
   */
  public void removeChild(LayerChild child) throws ChildNotFoundException {
    if (!children_.isEmpty()) {
      LayerChild chld;
      boolean found = false;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        chld = (LayerChild) it.nextElement();
        if (chld.equals(child)) {
          children_.removeElement(child);
          found = true;
          modified("Layer: removeChild(LayerChild)");
        }
      }
      if (!found) throw new ChildNotFoundException();
    } else {
      throw new ChildNotFoundException();
    }
  }

  /**
   * Remove a <code>LayerChild</code> object from the <code>Layer</code>.
   *
   * @param labid An identifier for a <code>LayerChild</code> associated with the <code>Layer</code>
   * @exception ChildNotFoundException The child is not associated with the <code>Layer</code>
   * @see SGLabel
   * @see LineKey
   * @see ColorKey
   * @see Ruler
   */
  public void removeChild(String labid) throws ChildNotFoundException {
    if (!children_.isEmpty()) {
      boolean found = false;
      LayerChild child;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        child = (LayerChild) it.nextElement();
        if (child.getId().equals(labid)) {
          children_.removeElement(child);
          found = true;
          modified("Layer: removeChild(String)");
        }
      }
      if (!found) throw new ChildNotFoundException();
    } else {
      throw new ChildNotFoundException();
    }
  }

  /**
   * Find <code>LayerChild</code> in <code>Layer</code>.
   *
   * @param id LayerChild identifier
   * @return LayerChild
   * @since 3.0
   */
  public LayerChild findChild(String id) {
    LayerChild child = null;
    for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
      child = (LayerChild) it.nextElement();
      if (child.getId().equals(id)) return child;
    }
    return null;
  }

  /**
   * Tests if a <code>LayerChild</code> is attached to the <code>Layer</code>.
   *
   * @param child LayerChild to test
   * @return true if attached to Layer
   * @since 2.0
   */
  public boolean isChildAttached(LayerChild child) {
    boolean found = false;
    if (!children_.isEmpty()) {
      LayerChild chld;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        chld = (LayerChild) it.nextElement();
        if (chld.equals(child)) {
          children_.removeElement(child);
          found = true;
          break;
        }
      }
    }
    return found;
  }

  /** Remove all <code>LayerChild</code> objects from the <code>Layer</code>. */
  public void removeAllChildren() {
    children_.removeAllElements();
    modified("Layer: removeAllChildren()");
  }

  /**
   * Get a child associated with the <code>Layer</code>.
   *
   * @param labid A <code>LayerChild</code> object identifier
   * @return layerChild with id
   * @exception ChildNotFoundException The child is not associated with the <code>Layer</code>
   * @see SGLabel
   * @see LineKey
   * @see ColorKey
   * @see Ruler
   */
  public LayerChild getChild(String labid) throws ChildNotFoundException {
    if (!children_.isEmpty()) {
      LayerChild child;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        child = (LayerChild) it.nextElement();
        if (child.getId() == labid) return child;
      }
      throw new ChildNotFoundException();
    } else {
      throw new ChildNotFoundException();
    }
  }

  /**
   * Create a <code>Enumeration</code> for the <code>LayerChild</code>'s associated with the <code>
   * Layer</code>.
   *
   * @return <code>Enumeration</code> for the <code>LayerChild</code> objects.
   * @see Enumeration
   * @see SGLabel
   * @see LineKey
   * @see ColorKey
   * @see Ruler
   */
  public Enumeration childElements() {
    return children_.elements();
  }

  /**
   * @since 3.0
   */
  public Iterator childIterator() {
    return children_.iterator();
  }

  /**
   * @since 3.0
   */
  public LayerChild[] getChildren() {
    LayerChild[] childs = new LayerChild[0];
    childs = (LayerChild[]) children_.toArray(childs);
    return childs;
  }

  /**
   * Set the size of the <code>Layer</code> in physical coordinates.
   *
   * @param psize The physical size of the <code>Layer</code>.
   */
  public void setSizeP(Dimension2D psize) {
    pWidth_ = psize.width;
    pHeight_ = psize.height;
    computeScale();
    modified("Layer: setSizeP()");
  }

  /**
   * Get the <code>Layer</code> size in physical coordinates. This returns the physical coordinate
   * size of the <code>Layer</code>.
   *
   * @return A <code>Dimension2D</code> containing the physical size of the <code>Layer</code>.
   * @see Dimension2D
   */
  public Dimension2D getSizeP() {
    return new Dimension2D(pWidth_, pHeight_);
  }

  /**
   * Get the <code>Layer</code> bounds in physical coordinates. The origin of the bounding
   * rectangle, for a <code>Layer</code>, is always (0,0).
   *
   * @return A <code>Rectangle2D.Double</code> containing the physical bounds of the <code>Layer
   *     </code>.
   * @see java.awt.geom.Rectangle2D.Double
   */
  public Rectangle2D.Double getBoundsP() {
    return new Rectangle2D.Double(0.0, 0.0, pWidth_, pHeight_);
  }

  /**
   * Get the <code>Layer</code> identifier.
   *
   * @return The identifier.
   */
  @Override
  public String getId() {
    return ident_;
  }

  /**
   * Set the <code>Layer</code> identifier.
   *
   * @param id identifier
   */
  public void setId(String id) {
    ident_ = id;
  }

  /**
   * Set the <code>Pane</code> the <code>Layer</code> is associated with. This method is called by
   * <code>Pane</code> when the <code>Pane.add</code> method is exectued.
   *
   * @param p The <code>Pane</code>
   */
  @Override
  public void setPane(AbstractPane p) {
    pane_ = p;
    if (p != null) computeScale();
    modified("Layer: setPane()");
  }

  /**
   * Get the <code>Pane</code> the <code>Layer</code> is associated with.
   *
   * @return Refence to the <code>Pane</code>
   */
  public AbstractPane getPane() {
    return pane_;
  }

  /**
   * Used internally by sgt.
   *
   * @param mess message
   * @since 2.0
   */
  public void modified(String mess) {
    if (pane_ != null) {
      //      if(Debug.EVENT) System.out.println("Layer: modified(" + mess + ")");
      pane_.setModified(true, mess);
    }
  }

  /**
   * Find object associated with a MOUSE_DOWN event. The getObjectAt method scans through all the
   * objects associated with the layer to find one whose bounding box contains the mouse location.
   *
   * <p>This method should not be called by a user.
   *
   * @param pt device coordinates
   * @param check if true requires that object isSelectable
   * @return object at location
   */
  public Object getObjectAt(int x, int y, boolean check) {
    return getObjectAt(new Point(x, y), check);
  }

  /**
   * Find object associated with a MOUSE_DOWN event. The getObjectAt method scans through all the
   * objects associated with the layer to find one whose bounding box contains the mouse location.
   *
   * <p>This method should not be called by a user.
   *
   * @param pt device coordinates
   * @return object at location
   */
  public Object getObjectAt(int x, int y) {
    return getObjectAt(new Point(x, y), true);
  }

  /**
   * Find object associated with a MOUSE_DOWN event. The getObjectAt method scans through all the
   * objects associated with the layer to find one whose bounding box contains the mouse location.
   *
   * <p>This method should not be called by a user.
   *
   * @param pt device coordinates
   * @param check if true requires that object isSelectable
   * @return object at location
   */
  public Object getObjectAt(Point pt, boolean check) {
    Rectangle bnds;
    Object obj;
    if (!children_.isEmpty()) {
      LayerChild child;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        child = (LayerChild) it.nextElement();
        bnds = child.getBounds();
        if (bnds.contains(pt) && (!check || child.isSelectable()) && child.isVisible()) {
          if (child instanceof LineKey) {
            return ((LineKey) child).getObjectAt(pt);
          } else if (child instanceof PointCollectionKey) {
            return ((PointCollectionKey) child).getObjectAt(pt);
          } else if (child instanceof VectorKey) {
            return ((VectorKey) child).getObjectAt(pt);
          } else {
            return child;
          }
        }
      }
    }
    if (graph_ != null) {
      obj = graph_.getObjectAt(pt);
      if (obj != null) return obj;
    }
    return (Object) null;
  }

  /**
   * Find objects associated with a MOUSE_DOWN event. The getObjecstAt method scans through all the
   * objects associated with the layer to find those whose bounding box contains the mouse location.
   *
   * <p>This method should not be called by a user.
   *
   * @param x mouse coordinate
   * @param y mouse coordinate
   * @param check if selectable
   * @return object array
   * @since 3.0
   */
  public Object[] getObjectsAt(int x, int y, boolean check) {
    Point pt = new Point(x, y);
    Vector obList = new Vector();
    Object obj = null;
    Rectangle bnds;
    if (!children_.isEmpty()) {
      LayerChild child;
      for (Enumeration it = children_.elements(); it.hasMoreElements(); ) {
        child = (LayerChild) it.nextElement();
        bnds = child.getBounds();
        if (bnds.contains(pt) && (!check || child.isSelectable()) && child.isVisible()) {
          if (child instanceof LineKey) {
            obj = ((LineKey) child).getObjectAt(pt);
            if (obj != null) obList.add(obj);
          } else if (child instanceof PointCollectionKey) {
            obj = ((PointCollectionKey) child).getObjectAt(pt);
            if (obj != null) obList.add(obj);
          } else if (child instanceof VectorKey) {
            obj = ((VectorKey) child).getObjectAt(pt);
            if (obj != null) obList.add(obj);
          } else {
            if (child != null) obList.add(child);
          }
        }
      }
    }
    if (graph_ != null) {
      obj = graph_.getObjectAt(pt);
      if (obj != null) obList.add(obj);
    }

    return obList.toArray();
  }

  /**
   * Get a <code>String</code> representation of the <code>Layer</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + ident_;
  }

  /**
   * Checks to see if a data id matches that data attached to the <code>Graph</code>.
   *
   * @param id data identifier
   * @return true if data is in layer
   * @since 2.0
   */
  public boolean isDataInLayer(String id) {
    if (graph_ instanceof CartesianGraph) {
      CartesianRenderer cr = ((CartesianGraph) graph_).getRenderer();
      if (cr instanceof LineCartesianRenderer) {
        if (((LineCartesianRenderer) cr).hasCollection()) {
          Collection co = ((LineCartesianRenderer) cr).getCollection();
          for (Enumeration it = co.elements(); it.hasMoreElements(); ) {
            if (((SGTData) it.nextElement()).getId().equals(id)) return true;
          }
        } else {
          return ((LineCartesianRenderer) cr).getLine().getId().equals(id);
        }
      } else if (cr instanceof GridCartesianRenderer) {
        return ((GridCartesianRenderer) cr).getGrid().getId().equals(id);
      } else if (cr instanceof PointCartesianRenderer) {
        if (((PointCartesianRenderer) cr).hasCollection()) {
          Collection co = ((PointCartesianRenderer) cr).getCollection();
          for (Enumeration it = co.elements(); it.hasMoreElements(); ) {
            if (((SGTData) it.nextElement()).getId().equals(id)) return true;
          }
        } else {
          return ((PointCartesianRenderer) cr).getPoint().getId().equals(id);
        }
      }
    }
    return false;
  }
}
