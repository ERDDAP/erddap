/*
 * $Id: AbstractPane.java,v 1.24 2003/09/18 21:21:33 dwd Exp $
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

import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyChangeListener;

/**
 * Defines the basic sgt Pane functionality. <code>Pane</code> and <code>JPane</code> implement the
 * <code>AbstractPane</code> interface.
 *
 * @author Donald Denbo
 * @version $Revision: 1.24 $, $Date: 2003/09/18 21:21:33 $
 * @since 2.0
 * @see Pane
 * @see JPane
 */
public interface AbstractPane {

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception;

  /** Align to top of printer page. */
  public static final int TOP = 0;

  /** Align to middle of printer page. */
  public static final int MIDDLE = 1;

  /** Align to bottom of printer page. */
  public static final int BOTTOM = 2;

  /** Align to left of printer page. */
  public static final int LEFT = 0;

  /** Align to center of printer page. */
  public static final int CENTER = 1;

  /** Align to right of printer page. */
  public static final int RIGHT = 2;

  /** Align to location specified on printer page. */
  public static final int SPECIFIED_LOCATION = -1;

  /**
   * Fit onto printer page. Magnify or shrink to fit onto printer page.
   *
   * @since 3.0
   */
  public static final int TO_FIT = 0;

  /**
   * Default scale for printing. A value of 1.0 physical units = 72 pts.
   *
   * @since 3.0
   */
  public static final int DEFAULT_SCALE = 1;

  /**
   * Shrink to fit onto printer page. Will not magnify if graphic will already fit.
   *
   * @since 3.0
   */
  public static final int SHRINK_TO_FIT = 2;

  /**
   * The <code>AbstractPane</code> and all of the attached Classes will be drawn. Drawing will occur
   * in an offscreen image and then copied to the screen. A new offscreen image is created on the
   * first call to draw() or if the size of the pane has been changed. The offscreen image will be
   * used as a "double" buffer when the screen requires redrawing.
   *
   * <p>Each <code>Layer</code> that has been added will be drawn in the order added, except if that
   * order has been change using the <code>moveLayerUp()</code> or <code>moveLayerDown()</code>
   * methods.
   *
   * @see java.awt.Graphics
   * @see Layer
   */
  public void draw();

  /**
   * The <code>AbstractPane</code> and all of the attached Classes will be drawn. Drawing will occur
   * using the supplied <code>Graphics</code> object.
   *
   * @param g User supplied <code>Graphics</code> object
   * @see java.awt.Graphics
   */
  public void draw(Graphics g);

  /**
   * The <code>AbstractPane</code> and all of the attached Classes will be drawn. Drawing will occur
   * using the supplied <code>Graphics</code> object. And clipping will be done to the width and
   * height.
   *
   * @param g User supplied <code>Graphics</code> object
   * @param width clipping width
   * @param height clipping height
   * @see java.awt.Graphics
   */
  public void draw(Graphics g, int width, int height);

  /**
   * This method is called when the <code>AbstractPane</code> first becomes visible. The types of
   * operations that should be implemented here include those that require a valid <code>Graphics
   * </code> object.
   */
  public void init();

  /**
   * Test if the current <code>Graphics</code> object is a printer.
   *
   * @return true if a printer
   */
  public boolean isPrinter();

  /**
   * Return an array of objects whose bounds include x,y.
   *
   * @since 3.0
   */
  public Object[] getObjectsAt(int x, int y);

  /**
   * Return an array of objects whose bounds are at point pt.
   *
   * @since 3.0
   */
  public Object[] getObjectsAt(Point pt);

  /**
   * Get the printer page size.
   *
   * @return page size
   */
  public java.awt.Dimension getPageSize();

  /**
   * Get the <code>Pane</code> identifier.
   *
   * @return <code>String</code> containing the <code>Pane</code> identifier.
   */
  public String getId();

  /** Set the <code>Pane</code> identifier */
  public void setId(String id);

  /**
   * Set printing scale mode. Allowable choices are <code>TO_FIT</code>, <code>SHRINK_TO_FIT</code>
   * and <code>DEFAULT_SCALE</code>. Default = DEFAULT_SCALE.
   *
   * @param mode print page scaling
   * @since 3.0
   * @see AbstractPane#DEFAULT_SCALE
   * @see AbstractPane#TO_FIT
   * @see AbstractPane#SHRINK_TO_FIT
   */
  public void setPageScaleMode(int mode);

  /**
   * Set alignment for printing.
   *
   * @param vert vertical alignment
   * @param horz horizontal alignment
   * @see AbstractPane#TOP
   * @see AbstractPane#MIDDLE
   * @see AbstractPane#BOTTOM
   * @see AbstractPane#LEFT
   * @see AbstractPane#CENTER
   * @see AbstractPane#RIGHT
   * @see AbstractPane#SPECIFIED_LOCATION
   */
  public void setPageAlign(int vert, int horz);

  /**
   * Set vertical alignment for printing. Allowed choices include <code>TOP</code>, <code>MIDDLE
   * </code>, and <code>BOTTOM</code> for vert and <code>LEFT</code>, <code>CENTER</code>, and
   * <code>RIGHT</code> for horz. Either can be <code>SPECIFIED_LOCATION</code>.
   *
   * @param vert vertical alignment
   * @see AbstractPane#TOP
   * @see AbstractPane#MIDDLE
   * @see AbstractPane#BOTTOM
   * @see AbstractPane#SPECIFIED_LOCATION
   */
  public void setPageVAlign(int vert);

  /**
   * Set horizontal alignment for printing. Allowed choices include <code>TOP</code>, <code>MIDDLE
   * </code>, and <code>BOTTOM</code>.
   *
   * @param horz horizontal alignment
   * @see AbstractPane#LEFT
   * @see AbstractPane#CENTER
   * @see AbstractPane#RIGHT
   * @see AbstractPane#SPECIFIED_LOCATION
   */
  public void setPageHAlign(int horz);

  /**
   * Get printing scale mode.
   *
   * @return AUTO_SCALE, TO_FIT, or SHRINK_TO_FIT
   * @since 3.0
   * @see AbstractPane#DEFAULT_SCALE
   * @see AbstractPane#TO_FIT
   * @see AbstractPane#SHRINK_TO_FIT
   */
  public int getPageScaleMode();

  /**
   * Get vertical alignment for printing. Allowed choices include <code>LEFT</code>, <code>CENTER
   * </code>, and <code>RIGHT</code>.
   *
   * @return vertical alignment
   * @see AbstractPane#TOP
   * @see AbstractPane#MIDDLE
   * @see AbstractPane#BOTTOM
   * @see AbstractPane#SPECIFIED_LOCATION
   */
  public int getPageVAlign();

  /**
   * Get horizontal alignment for printing.
   *
   * @return horizontal alignment
   * @see AbstractPane#LEFT
   * @see AbstractPane#CENTER
   * @see AbstractPane#RIGHT
   * @see AbstractPane#SPECIFIED_LOCATION
   */
  public int getPageHAlign();

  /**
   * Set the printer page origin. Valid for HAlign = <code>SPECIFIED_LOCATION</code> or VAlign =
   * <code>SPECIFIED_LOCATION</code>.
   */
  public void setPageOrigin(java.awt.Point p);

  /**
   * Get the printer page origin. Valid for HAlign = <code>SPECIFIED_LOCATION</code> or VAlign =
   * <code>SPECIFIED_LOCATION</code>.
   */
  public java.awt.Point getPageOrigin();

  /**
   * Get the first <code>Layer</code> associated with the <code>Pane</code>
   *
   * @return the first <code>Layer</code> object
   */
  public Layer getFirstLayer();

  /**
   * Get the <code>Layer</code> associated with the <code>Pane</code> indicated by the id.
   *
   * @param id identifier.
   * @exception LayerNotFoundException The <code>Layer</code> indicated by the id was not found.
   */
  public Layer getLayer(String id) throws LayerNotFoundException;

  /**
   * Get the <code>Layer</code> associated with the <code>Pane</code> indicated by the data id.
   *
   * @param id data identifier
   * @exception LayerNotFoundException The <code>Layer</code> indicated by the id was not found.
   * @see gov.noaa.pmel.sgt.dm.SGTData
   */
  public Layer getLayerFromDataId(String id) throws LayerNotFoundException;

  /*
   * methods to get mouse input results
   */
  /**
   * Return the last object selected. Returns only objects that are part of <code>Layer</code>s
   * currently connected to the pane. <code>AbstractPane</code> tests each layer after a MOUSE_DOWN
   * event for an object whose bounding box contains the mouse. The pane object then passes the
   * event on to the next level.
   */
  public Object getSelectedObject();

  /**
   * Primarily used internally by sgt. This can also be used to mark an object as selected for use
   * in an event handler.
   */
  public void setSelectedObject(Object obj);

  /**
   * Return the device coordinates of the zoom action. The coordinates are in device units and may
   * require transformation to the physical units or user units.
   *
   * @return zoom rectangle
   */
  public java.awt.Rectangle getZoomBounds();

  /**
   * Return the device coordinates of the start of the zoom action. The <code>Point</code> is in
   * device coordinates and may require transformation to physical units or user units. Zoom start
   * may be useful to indicate which graph to zoom.
   *
   * @return zoom start
   * @since 3.0
   */
  public Point getZoomStart();

  /** Get the current selected object at a point. Used internally by sgt. */
  public Object getObjectAt(int x, int y);

  /**
   * Get the bounding rectangle in pixels (device units).
   *
   * @return Rectangle object containing the bounding box for the pane.
   */
  public java.awt.Rectangle getBounds();

  /** Get the <code>Component</code> associated with the pane. */
  public java.awt.Component getComponent();

  /*
   * methods to handle ChangeEvent and PropertyChangeEvent's
   */
  /**
   * Turn on/off batching of updates to the pane. While batching is <code>true</code> property
   * change events will <strong>not</strong> cause pane to redraw. When batching is turned back on
   * if the pane has been modified it will then redraw.
   */
  public void setBatch(boolean batch, String msg);

  /**
   * Turn on/off batching of updates to the pane. While batching is <code>true</code> property
   * change events will <strong>not</strong> cause pane to redraw. When batching is turned back on
   * if the pane has been modified it will then redraw.
   */
  public void setBatch(boolean batch);

  /** Is batching turned on? */
  public boolean isBatch();

  /**
   * Notify the pane that something has changed and a redraw is required. Used internally by sgt.
   */
  public void setModified(boolean mod, String mess);

  /** Has the plot been modified? */
  public boolean isModified();

  /**
   * Enable/disable the handling of <code>MouseEvent</code>s by SGT. Disabling mouse events will
   * turn off object selection, moveable, selectable, draggable, and zooming.
   *
   * @since 3.0
   */
  public void setMouseEventsEnabled(boolean enable);

  /**
   * Are <code>MouseEvent</code>s enabled for processing by SGT?
   *
   * @since 3.0
   */
  public boolean isMouseEventsEnabled();

  /*
   * Pane PropertyChange methods
   */
  /**
   * Add a PropertyChangeListener to the list. Properties for <code>Pane</code> and <code>JPane
   * </code> include "objectSelected" and "zoomRectangle".
   */
  public void addPropertyChangeListener(PropertyChangeListener l);

  /** Remove the PropertyChangeListener from the list. */
  public void removePropertyChangeListener(PropertyChangeListener l);
}
