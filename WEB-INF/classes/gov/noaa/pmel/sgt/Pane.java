/*
 * $Id: Pane.java,v 1.23 2003/09/19 23:14:24 dwd Exp $
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

import gov.noaa.pmel.util.Debug;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

/**
 * The <code>Pane</code> class is extended from <code>java.awt.Container</code> and is the basis for
 * the <code>gov.noaa.pmel.sgt</code> package.
 *
 * <p>The Java scientific graphics toolkit is designed to allow a graphics client developer a great
 * deal of flexibility and freedom. <code>sgt</code> is a package that greatly aids a developer in
 * creating graphics applets. <code>sgt</code> is not a general purpose graphics package, but
 * provides the tools to enable scientific graphics to be easily incorporated into <code>Applets
 * </code>.
 *
 * <p><code>sgt</code> has three main components, the "pane", on which all graphics are drawn. The
 * <code>Pane</code> is a fairly simple class and all drawing is done in "device" coordinates
 * (pixels). By default, the <code>Pane</code> will draw on the screen, but it is designed to allow
 * drawing in an offscreen buffer that can be printed (for applications).
 *
 * <p>The next component is the <code>Layer</code>. Several <code>Layers</code> can be associated
 * with a single <code>Pane</code>. The <code>Layer</code> class insulates the developer from the
 * details of device coordinates by using "physical" coordinates. Physical coordinates are a
 * right-hand coordinate systems with an origin of (0.0, 0.0) in the lower-left-hand corner and have
 * the same scale in both the vertical and horizontal directions. Thus, a <code>Layer</code> that is
 * 5.0 units wide and 3.0 units high can be made larger and smaller on the screen by resizing the
 * <code>Pane</code>, but will not be distorted. The <code>Layer</code> class is responsible for
 * displaying labels, keys (color, vector, and line), and rulers. A <code>Layer</code> can contain a
 * single <code>Graph</code>.
 *
 * <p>Finally, the <code>Graph</code> component transforms from user coordinates (e.g. cm/sec, time,
 * degC, or meters) to physical coordinates. The <code>Graph</code> classes handle the display of
 * axes and data. Children of the <code>Graph</code> class are capable of creating Cartesian, polar,
 * and map graphics. For Cartesian graphs, several different axes (log, plain and time), transforms
 * (linear, log, and tablelookup), and <code>CartesianGraph</code> (pixel, line, vector, and
 * contour) classes are available. These classes can be combined in almost any combination.
 *
 * <p>While only one dataset may be plotted per <code>Layer</code>, co-plotting is supported by
 * allowing layers to use the same transform objects. The order that the layers are plotted can be
 * changed, allowing the developer (or user) to control what may be obscured.
 *
 * <p>Member functions, in package <code>gov.noaa.pmel.sgt</code>, follow the following naming
 * convention. Member functions that have a <strong>P</strong>, <strong>U</strong>, or
 * <I>nothing</I> at the end of the function name are of type double in <strong>P</strong>hysical
 * units, type double in <strong>U</strong>ser units, and type int in Device units, respectively.
 * Variables that start with p, u, t, or d are coordinates of type physical, user, time, or device,
 * respectively.
 *
 * <p>All graphics are rendered when the <code>draw()</code> method is invoked.
 *
 * <p><strong>Mouse Events</strong>
 *
 * <p>Mouse events are processed by the <code>JPane</code> object to support object selection and
 * zooming. Object selection is accomplished by left clicking the mouse on the desired object.
 * <code>JPane</code> then fires a <code>PropertyChangeEvent</code> of type "objectSelected" that
 * can be listened for by the users application. The user application then invokes the <code>
 * getSelectedObject()</code> method. Zooming is accomplished in several steps.
 *
 * <p>
 *
 * <pre>
 * 1) Begin a zoom operation by pressing the left button.
 * 2) Describe a zoom rectangle by dragging the mouse with the left
 *    button down.
 * 3) Finish the zoom operation by releasing the left mouse button.
 * </pre>
 *
 * <p>When the mouse button has been release <code>JPane</code> fires a <code>PropertyChangeEvent
 * </code> of type "zoomRectangle" that can be listened for by the users application. The user
 * application can then obtain the zoom rectangle by invoking the <code>getZoomBounds()</code>
 * method.
 *
 * <p>An example of object selection and zooming can be found in the {@link JPane} documentation.
 *
 * @author Donald Denbo
 * @version $Revision: 1.23 $, $Date: 2003/09/19 23:14:24 $
 * @since 1.0 @Deprecated As of sgt 3.0, use {@link gov.noaa.pmel.sgt.JPane JPane}.
 * @see Layer
 * @see Graph
 * @see java.awt.Graphics
 */
public class Pane extends Container implements AbstractPane {
  //
  private PaneProxy proxy_;
  private Rectangle bounds;

  //
  /**
   * @shapeType AggregationLink
   * @supplierCardinality 1..*
   */
  /*#  Layer lnkUnnamed*/
  /**
   * Constructs a <code>Pane</code>.
   *
   * @param id the <code>Pane</code> identifier
   * @param size the size of the <code>Pane</code> in pixels
   */
  public Pane(String id, Dimension size) {
    proxy_ = new PaneProxy(this, id, size);
    setSize(size);
    bounds = new Rectangle(0, 0, size.width, size.height);
    //
    // setup for Low-Level Events
    //
    //    this.enableEvents(AWTEvent.MOUSE_EVENT_MASK |
    //                      AWTEvent.MOUSE_MOTION_EVENT_MASK);
  }

  // Bob Simons added:
  @Override
  public Rectangle getBounds() {
    return bounds;
  }

  /**
   * Default constructor. The identifier is set to an empty string and the size is set to a width
   * and height of 50 pixels. A default constructor is required to work as a component with Visual
   * Cafe.
   *
   * <pre>
   * import gov.noaa.pmel.sgt.Pane;
   * ...
   * Pane pane;
   * ...
   * pane = new Pane("main graph", new Dimension(400, 500));
   * pane.setLayout(new StackedLayout());
   * ...
   * </pre>
   *
   * @see StackedLayout
   */
  public Pane() {
    this(new String(""), new Dimension(50, 50));
  }

  /**
   * Return the version of SGT.
   *
   * @since 3.0
   */
  public static String getVersion() {
    return PaneProxy.getVersion();
  }

  @Override
  public void draw() {
    proxy_.draw();
  }

  /** No initialization required by Pane. */
  @Override
  public void init() {}

  @Override
  public void draw(Graphics g) {
    proxy_.draw(g);
  }

  @Override
  public void draw(Graphics g, int width, int height) {
    proxy_.draw(g, width, height);
  }

  @Override
  public boolean isPrinter() {
    return proxy_.isPrinter();
  }

  /** Internal access to jdk1.1 or Java2D line drawing. */
  public static StrokeDrawer getStrokeDrawer() {
    return PaneProxy.strokeDrawer;
  }

  @Override
  public Dimension getPageSize() {
    return proxy_.getPageSize();
  }

  /** Updates the <code>Pane</code> */
  @Override
  public void update(Graphics g) {
    if (Debug.DEBUG) System.out.println("Pane: " + proxy_.getId() + ": update(g)");
    super.update(g);
    paint(g);
  }

  /**
   * Override of the parent <code>paint</code> method. This method should not be called by a user.
   */
  @Override
  public void paint(Graphics g) {
    proxy_.paint(g);
  }

  /**
   * Adds the specified component to the end of the <code>Pane</code>.
   *
   * @param comp the component to be added
   * @return component argument
   */
  @Override
  public Component add(Component comp) {
    if (comp instanceof Layer) {
      ((Layer) comp).setPane(this);
    } else if (comp instanceof LayerContainer) {
      ((LayerContainer) comp).setPane(this);
    }
    return super.add(comp);
  }

  /**
   * Adds the specified component to the <code>Pane</code> at the given position.
   *
   * @param comp the component to be added
   * @param index the position at which to insert the component, or -1 to insert the component at
   *     the end.
   * @return component argument
   */
  @Override
  public Component add(Component comp, int index) {
    if (comp instanceof Layer) {
      ((Layer) comp).setPane(this);
    } else if (comp instanceof LayerContainer) {
      ((LayerContainer) comp).setPane(this);
    }
    return super.add(comp, index);
  }

  /**
   * Adds the specified component to the end of this <code>Pane</code>. Also notifies the layout
   * manager to add the component to this <code>Pane</code>'s layout using the specified constraints
   * object.
   *
   * @param comp the component to be added
   * @param constraints an object expressing layout constraints for this component
   */
  @Override
  public void add(Component comp, Object constraints) {
    super.add(comp, constraints);
    if (comp instanceof Layer) {
      ((Layer) comp).setPane(this);
    } else if (comp instanceof LayerContainer) {
      ((LayerContainer) comp).setPane(this);
    }
  }

  /**
   * Adds the specified component to the end of this <code>Pane</code> at the specified index. Also
   * notifies the layout manager to add the component to this <code>Pane</code>'s layout using the
   * specified constraints object.
   *
   * @param comp the component to be added
   * @param constraints an object expressing layout constraints for this component
   * @param index the position in the <code>Pane</code>'s list at which to insert the component -1
   *     means insert at the end.
   */
  @Override
  public void add(Component comp, Object constraints, int index) {
    super.add(comp, constraints, index);
    if (comp instanceof Layer) {
      ((Layer) comp).setPane(this);
    } else if (comp instanceof LayerContainer) {
      ((LayerContainer) comp).setPane(this);
    }
  }

  /**
   * Adds the specified component to this <code>Pane</code>. It is strongly advised to use
   * add(Component, Object), in place of this method.
   */
  @Override
  public Component add(String name, Component comp) {
    if (comp instanceof Layer) {
      ((Layer) comp).setPane(this);
    } else if (comp instanceof LayerContainer) {
      ((LayerContainer) comp).setPane(this);
    }
    return super.add(name, comp);
  }

  @Override
  public String getId() {
    return proxy_.getId();
  }

  @Override
  public void setId(String id) {
    proxy_.setId(id);
  }

  @Override
  public void setPageAlign(int vert, int horz) {
    proxy_.setPageAlign(vert, horz);
  }

  @Override
  public void setPageVAlign(int vert) {
    proxy_.setPageVAlign(vert);
  }

  @Override
  public void setPageHAlign(int horz) {
    proxy_.setPageHAlign(horz);
  }

  @Override
  public int getPageVAlign() {
    return proxy_.getPageVAlign();
  }

  @Override
  public int getPageHAlign() {
    return proxy_.getPageHAlign();
  }

  @Override
  public void setPageOrigin(Point p) {
    proxy_.setPageOrigin(p);
  }

  @Override
  public Point getPageOrigin() {
    return proxy_.getPageOrigin();
  }

  /** Set the size. */
  @Override
  public void setSize(Dimension d) {
    super.setSize(d);
    proxy_.setSize(d);
  }

  @Override
  public Layer getFirstLayer() {
    return proxy_.getFirstLayer();
  }

  @Override
  public Layer getLayer(String id) throws LayerNotFoundException {
    return proxy_.getLayer(id);
  }

  @Override
  public Layer getLayerFromDataId(String id) throws LayerNotFoundException {
    return proxy_.getLayerFromDataId(id);
  }

  /**
   * Move the <code>Layer</code> up in the stack. The order of the layers determine when they are
   * drawn. Moving the <code>Layer</code> up causes the <code>Layer</code> to be drawn later and
   * over earlier layers.
   *
   * @param lyr <code>Layer</code> object.
   * @exception LayerNotFoundException The specified <code>Layer</code> was not found in the list.
   * @see Layer
   */
  public void moveLayerUp(Layer lyr) throws LayerNotFoundException {
    throw new MethodNotImplementedError();
  }

  /**
   * Move the <code>Layer</code> up in the stack. The order of the layers determine when they are
   * drawn. Moving the <code>Layer</code> up causes the <code>Layer</code> to be drawn later and
   * over earlier layers.
   *
   * @param id identifier.
   * @exception LayerNotFoundException The specified <code>Layer</code> was not found in the list.
   * @see Layer
   */
  public void moveLayerUp(String id) throws LayerNotFoundException {
    throw new MethodNotImplementedError();
  }

  /**
   * Move the <code>Layer</code> down in the stack. The order of the layers determine when they are
   * drawn. Moving the <code>Layer</code> down causes the <code>Layer</code> to be drawn earlier.
   *
   * @param lyr <code>Layer</code> object.
   * @exception LayerNotFoundException The specified <code>Layer</code> was not found in the list.
   * @see Layer
   */
  public void moveLayerDown(Layer lyr) throws LayerNotFoundException {
    throw new MethodNotImplementedError();
  }

  /**
   * Move the <code>Layer</code> down in the stack. The order of the layers determine when they are
   * drawn. Moving the <code>Layer</code> down causes the <code>Layer</code> to be drawn earlier.
   *
   * @param id identifier
   * @exception LayerNotFoundException The specified <code>Layer</code> was not found in the list.
   * @see Layer
   */
  public void moveLayerDown(String id) throws LayerNotFoundException {
    throw new MethodNotImplementedError();
  }

  @Override
  public Object getSelectedObject() {
    return proxy_.getSelectedObject();
  }

  @Override
  public void setSelectedObject(Object obj) {
    proxy_.setSelectedObject(obj);
  }

  /** Overrides the default event methods. */
  @Override
  public void processMouseEvent(MouseEvent event) {
    if (!proxy_.processMouseEvent(event)) super.processMouseEvent(event);
  }

  @Override
  public void processMouseMotionEvent(MouseEvent event) {
    if (!proxy_.processMouseMotionEvent(event)) super.processMouseMotionEvent(event);
  }

  @Override
  public Rectangle getZoomBounds() {
    return proxy_.getZoomBounds();
  }

  /**
   * @since 3.0
   */
  @Override
  public Point getZoomStart() {
    return proxy_.getZoomStart();
  }

  @Override
  public Object getObjectAt(int x, int y) {
    return proxy_.getObjectAt(x, y);
  }

  /**
   * @since 3.0
   */
  @Override
  public Object[] getObjectsAt(int x, int y) {
    return proxy_.getObjectsAt(x, y);
  }

  /**
   * @since 3.0
   */
  @Override
  public Object[] getObjectsAt(Point pt) {
    return proxy_.getObjectsAt(pt.x, pt.y);
  }

  @Override
  public Component getComponent() {
    return (Component) this;
  }

  @Override
  public Dimension getMaximumSize() {
    return proxy_.getMaximumSize();
  }

  @Override
  public Dimension getMinimumSize() {
    return proxy_.getMinimumSize();
  }

  @Override
  public Dimension getPreferredSize() {
    return proxy_.getPreferredSize();
  }

  /**
   * Get a <code>String</code> representatinof the <code>Pane</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    return proxy_.toString();
  }

  @Override
  public void setBatch(boolean batch, String msg) {
    proxy_.setBatch(batch, msg);
  }

  @Override
  public void setBatch(boolean batch) {
    proxy_.setBatch(batch, "");
  }

  @Override
  public boolean isBatch() {
    return proxy_.isBatch();
  }

  @Override
  public void setModified(boolean mod, String mess) {
    proxy_.setModified(mod, mess);
  }

  @Override
  public boolean isModified() {
    return proxy_.isModified();
  }

  /**
   * @since 3.0
   */
  @Override
  public void setMouseEventsEnabled(boolean enable) {
    proxy_.setMouseEventsEnabled(enable);
  }

  /**
   * @since 3.0
   */
  @Override
  public boolean isMouseEventsEnabled() {
    return proxy_.isMouseEventsEnabled();
  }

  @Override
  public void setPageScaleMode(int mode) {
    proxy_.setPageScaleMode(mode);
  }

  @Override
  public int getPageScaleMode() {
    return proxy_.getPageScaleMode();
  }

  /*
   * Pane PropertyChange methods
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    proxy_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    proxy_.removePropertyChangeListener(l);
  }

  @Override
  public void releaseResources() throws Exception { // Kyle and Bob added
    proxy_ = null;
    bounds = null;
  }
}
