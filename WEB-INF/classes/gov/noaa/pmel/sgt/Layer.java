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
import gov.noaa.pmel.sgt.swing.Draggable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

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
  private List<LayerChild> children_;

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
        List<LayerChild> o = children_; // done this way to avoid infinite loop
        children_ = null;
        for (LayerChild o2 : o) o2.releaseResources();
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
    // compute xoff and yoff as double then truncate to int
    Rectangle pbnds = pane_.getBounds();
    // getBounds();
    if (pane_.isPrinter()) {
      ax_ = 72; // java2 is in 1/72 of an inch
      ay_ = ax_;
      xoff2_ = (pbnds.width - ax_ * pWidth_) / 2.0 + pbnds.x;
      yoff2_ = pbnds.height - (pbnds.height - ay_ * pHeight_) / 2.0 + pbnds.y;
    } else {
      // not printer
      ax_ = (double) pbnds.width / pWidth_;
      ay_ = (double) pbnds.height / pHeight_;
      if (ax_ > ay_) {
        ax_ = ay_;
      } else if (ay_ > ax_) {
        ay_ = ax_;
      }
      xoff2_ = (pbnds.width - ax_ * pWidth_) / 2.0 + pbnds.x - pbnds.x;
      yoff2_ = pbnds.height - (pbnds.height - ay_ * pHeight_) / 2.0 + pbnds.y - pbnds.y;
    }

    // bob added:
    // System.out.println("Layer.computerScale xoff2_="+xoff2_+" yoff2_="+yoff2_+" ax_="+ax_+"
    // ay_="+ay_);

    xoff_ = (int) xoff2_;
    yoff_ = (int) yoff2_;
    // if (Debug.DEBUG && pane_.isPrinter()) {
    //   System.out.println("Layer.computeScale[" + getId() + "] printer = " + pane_.isPrinter());
    //   System.out.println("                  xd(min) = " + getXPtoD(0.0));
    //   System.out.println("                  xd(max) = " + getXPtoD(pWidth_));
    //   System.out.println("                  yd(min) = " + getYPtoD(0.0));
    //   System.out.println("                  yd(max) = " + getYPtoD(pHeight_));
    // }
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
    return (xd - xoff2_) / ax_;
  }

  /**
   * Transform device units to physical for the y direction.
   *
   * @param yd device y coordinate
   * @return physical y coordinate
   */
  public double getYDtoP(int yd) {
    return (yoff2_ - yd) / ay_;
  }

  /**
   * Create a <code>Layer</code> object. The <code>Layer</code> is created with the specified
   * dimensions and identifier.
   *
   * @param id identifier for Layer
   * @param psize The physical dimensions of the Layer
   */
  public Layer(String id, double pWidth, double pHeight) {
    ident_ = id;
    pWidth_ = pWidth;
    pHeight_ = pHeight;
    children_ = new ArrayList<>();
  }

  /**
   * Draw the Layer and its attached classes.
   *
   * @param g graphics context
   * @exception PaneNotFoundException if a pane object is not found
   */
  @Override
  public void draw(Graphics g) throws PaneNotFoundException {
    if (pane_ == null) {
      throw new PaneNotFoundException();
    }
    computeScale();
    //
    // draw Graph
    //
    if (graph_ != null) {
      graph_.draw(g);
    }
    //
    // draw children
    //
    if (!children_.isEmpty()) {
      for (LayerChild child : children_) {
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
      for (LayerChild child : children_) {
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
    children_.add(child);
    modified("Layer: addChild()");
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
      for (LayerChild child : children_) {
        bnds = child.getBounds();
        if (bnds.contains(pt) && (!check || child.isSelectable()) && child.isVisible()) {
          return child;
        }
      }
    }
    if (graph_ != null) {
      obj = graph_.getObjectAt(pt);
      return obj;
    }
    return null;
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
        return ((LineCartesianRenderer) cr).getLine().getId().equals(id);
      } else if (cr instanceof GridCartesianRenderer) {
        return ((GridCartesianRenderer) cr).getGrid().getId().equals(id);
      }
    }
    return false;
  }
}
