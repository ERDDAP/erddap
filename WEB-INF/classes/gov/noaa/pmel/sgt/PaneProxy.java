/*
 * $Id: PaneProxy.java,v 1.35 2003/09/19 23:14:24 dwd Exp $
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
import gov.noaa.pmel.sgt.beans.Panel;
import gov.noaa.pmel.sgt.swing.Draggable;
import gov.noaa.pmel.util.Debug;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.PrintGraphics;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

/**
 * PaneProxy implements the functionality common to <code>JPane</code> and <code>Pane</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.35 $, $Date: 2003/09/19 23:14:24 $
 */
public class PaneProxy { // Bob Simons made public
  public static String SGTVersion = "3.0";
  public static boolean Java2D = false;
  public static StrokeDrawer strokeDrawer = null;

  private Container pane_;

  private String ident_;
  private Dimension panesize_;
  private Image offscreen_;
  private Dimension pagesize_;
  private Point pageOrigin_ = new Point(0, 0);
  private boolean printer_ = false;
  private boolean opaque_ = true;
  private int halign_ = AbstractPane.CENTER;
  private int valign_ = AbstractPane.MIDDLE;
  private int printMode_ = AbstractPane.SHRINK_TO_FIT;
  private Object selectedobject_;
  private Object old_selectedobject_ = null;
  private Rectangle selectedRect_;
  private Rectangle zoom_rect_ = new Rectangle(0, 0, 0, 0);
  private Rectangle old_zoom_rect_;
  private Point zoom_start_ = new Point(0, 0);
  private boolean in_zoom_ = false;
  private boolean in_select_ = false;
  private boolean in_move_ = false;
  private boolean moved_ = false;
  private boolean draggable_ = false;
  private boolean moveable_ = false;
  private Point move_ref_;
  private boolean batch_ = true;
  private boolean modified_ = false;
  private boolean ignoreModified_ = false;
  private boolean firstDraw_ = true;
  private boolean mouseEventsEnabled_ = true;

  private PropertyChangeSupport changes_ = null;

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception {
    try {
      pane_ = null; // not releaseResources -> infinite loop
      offscreen_ = null;
      if (JPane.debug) String2.log("sgt.PaneProxy.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  public PaneProxy(Container pane, String id, Dimension size) {
    ident_ = id;
    panesize_ = size;
    pane_ = pane;
    changes_ = new PropertyChangeSupport(pane_);
    //
    testJava2D();
  }

  /**
   * @since 3.0
   */
  public static String getVersion() {
    return SGTVersion;
  }

  private void testJava2D() {
    Class cl;
    boolean java2d = true;
    try {
      cl = Class.forName("java.awt.Graphics2D");
    } catch (ClassNotFoundException e) {
      java2d = false;
    }
    Java2D = java2d;
    if (Java2D) {
      strokeDrawer = new StrokeDrawer2();
    } else {
      strokeDrawer = new StrokeDrawer1();
    }
  }

  public Dimension getSize() {
    return panesize_;
  }

  void draw() {
    ignoreModified_ = true;
    if (Debug.DEBUG)
      System.out.println("PaneProxy: [" + ident_ + "] " + " draw(), batch=" + batch_);
    printer_ = false;
    Graphics g = pane_.getGraphics();
    //
    // test existance of graphics context.
    //
    if (g == null) {
      ignoreModified_ = false;
      return;
    }
    if (firstDraw_) {
      ((AbstractPane) pane_).init();
      firstDraw_ = false;
    }
    Graphics goff;
    Dimension isze = pane_.getSize();
    if (offscreen_ == (Image) null) {
      offscreen_ = pane_.createImage(isze.width, isze.height);
    } else {
      if (isze.width != panesize_.width || isze.height != panesize_.height) {
        offscreen_ = pane_.createImage(isze.width, isze.height);
      }
    }
    panesize_ = isze;
    goff = offscreen_.getGraphics();
    //        super.paint(g);
    Rectangle clip = pane_.getBounds();
    goff.setClip(0, 0, clip.width, clip.height);

    drawLayers(goff);
    g.drawImage(offscreen_, 0, 0, pane_);
    drawDraggableItems(g);
    modified_ = false;
    ignoreModified_ = false;
    pane_.repaint();
  }

  void draw(Graphics g) {
    Rectangle clip = pane_.getBounds();
    draw(g, clip.width, clip.height);
  }

  void draw(Graphics g, int width, int height) {
    ignoreModified_ = true;
    if (Debug.DEBUG) System.out.println("PaneProxy: [" + ident_ + "] draw(g), batch=" + batch_);
    if (g instanceof PrintGraphics) {
      printer_ = true;
      pagesize_ = ((PrintGraphics) g).getPrintJob().getPageDimension();
    } else {
      printer_ = false;
      pagesize_ = null;
    }
    g.setClip(0, 0, width, height);
    drawLayers(g);
    drawDraggableItems(g);
    modified_ = false;
    ignoreModified_ = false;
  }

  void drawPage(Graphics g, double width, double height) {
    ignoreModified_ = true;
    if (Debug.DEBUG) System.out.println("PaneProxy: [" + ident_ + "] drawPage(g), batch=" + batch_);
    printer_ = true;

    Color saved = g.getColor();
    g.setColor(pane_.getBackground());
    Rectangle r = pane_.getBounds();
    g.fillRect(r.x, r.y, r.width, r.height);
    g.setColor(saved);

    drawLayers(g);
    drawDraggableItems(g);
    modified_ = false;
    ignoreModified_ = false;
    printer_ = false;
  }

  void setOpaque(boolean opaque) {
    opaque_ = opaque;
  }

  boolean isOpaque() {
    return opaque_;
  }

  void drawLayers(Graphics g) {
    if (!printer_) {
      if (opaque_) {
        Rectangle r = pane_.getBounds();
        g.setColor(pane_.getBackground());
        g.fillRect(0, 0, r.width, r.height);
      }
      g.setColor(pane_.getForeground());
    }
    //    System.out.println("PaneProxy.drawLayers("+getId()+"): print = " + printer_ + ", pane = "
    // + pane_.getBounds());
    //        g.drawRect(2,2,r.width-5,r.height-5);
    //
    // draw Layers
    //
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      try {
        if (comps[i] instanceof Layer) {
          ((Layer) comps[i]).draw(g);
        } else if (comps[i] instanceof LayerControl) {
          ((LayerControl) comps[i]).draw(g);
        }
        /*       if(printer_ && comps[i] instanceof Panel) {
          ((Panel)comps[i]).paintBorder(g);
        } */
      } catch (PaneNotFoundException e) {
      }
    }
  }

  void drawDraggableItems(Graphics g) {
    if (Debug.DEBUG)
      System.out.println("PaneProxy: [" + ident_ + "] drawDraggableItems, batch=" + batch_);
    //
    // draw draggable items in each layer
    //
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      try {
        if (comps[i] instanceof LayerControl) {
          ((LayerControl) comps[i]).drawDraggableItems(g);
        }
      } catch (PaneNotFoundException e) {
      }
    }
  }

  void paint(Graphics g) {
    if (Debug.DEBUG || Debug.DRAW_TRACE)
      System.out.println(
          "PaneProxy: ["
              + ident_
              + "] paint(g): "
              + g.getClipBounds()
              + ", batch="
              + batch_
              + ", modified="
              + modified_);
    Dimension isze = pane_.getSize();
    if (isze.width != panesize_.width || isze.height != panesize_.height) offscreen_ = null;
    if (offscreen_ != null && !modified_) {
      g.drawImage(offscreen_, 0, 0, pane_);
      drawDraggableItems(g);
    } else {
      if (Debug.DRAW_TRACE)
        System.out.println("PaneProxy: [" + ident_ + "] paint: calling draw(), batch=" + batch_);
      draw();
    }
    modified_ = false;
  }

  String getId() {
    return ident_;
  }

  void setId(String id) {
    ident_ = id;
  }

  void setPageAlign(int vert, int horz) {
    valign_ = vert;
    halign_ = horz;
  }

  void setPageVAlign(int vert) {
    valign_ = vert;
  }

  void setPageHAlign(int horz) {
    halign_ = horz;
  }

  int getPageVAlign() {
    return valign_;
  }

  int getPageHAlign() {
    return halign_;
  }

  void setPageOrigin(Point p) {
    pageOrigin_ = p;
  }

  Point getPageOrigin() {
    return pageOrigin_;
  }

  boolean isPrinter() {
    return printer_;
  }

  Dimension getPageSize() {
    return pagesize_;
  }

  void setSize(Dimension d) {
    panesize_ = d;
    offscreen_ = null;
  }

  Layer getFirstLayer() {
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Layer) {
        return (Layer) comps[i];
      }
    }
    return null;
  }

  Layer getLayer(String id) throws LayerNotFoundException {
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Layer) {
        if (((Layer) comps[i]).getId() == id) return (Layer) comps[i];
      } else if (comps[i] instanceof Panel) {
        if (((Panel) comps[i]).hasLayer(id)) return (Layer) ((Panel) comps[i]).getLayer(id);
      }
    }
    throw new LayerNotFoundException();
  }

  Layer getLayerFromDataId(String id) throws LayerNotFoundException {
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Layer) {
        if (((Layer) comps[i]).isDataInLayer(id)) return (Layer) comps[i];
      } else if (comps[i] instanceof Panel) {
        if (((Panel) comps[i]).isDataInPanel(id))
          return (Layer) ((Panel) comps[i]).getLayerFromDataId(id);
      }
    }
    throw new LayerNotFoundException();
  }

  Object getSelectedObject() {
    return selectedobject_;
  }

  void setSelectedObject(Object obj) {
    old_selectedobject_ = selectedobject_;
    selectedobject_ = obj;
  }

  Rectangle getZoomBounds() {
    return zoom_rect_;
  }

  /**
   * @since 3.0
   */
  Point getZoomStart() {
    return zoom_start_;
  }

  Object getObjectAt(int x, int y) {
    Object obj = null;
    Component[] comps = pane_.getComponents();
    if (comps.length != 0) {
      Layer ly;
      for (int i = 0; i < comps.length; i++) {
        if (comps[i] instanceof Layer) {
          obj = ((Layer) comps[i]).getObjectAt(x, y, false);
          if (obj != null) return obj;
        } else if (comps[i] instanceof Panel) {
          obj = ((Panel) comps[i]).getObjectAt(x, y, false);
          if (obj != null) return obj;
        }
      }
    }
    return obj;
  }

  /**
   * @since 3.0
   */
  Object[] getObjectsAt(int x, int y) {
    Vector obList = new Vector();
    Object obj = null;
    Component[] comps = pane_.getComponents();
    if (comps.length != 0) {
      Layer ly;
      for (int i = 0; i < comps.length; i++) {
        if (comps[i] instanceof Layer) {
          obj = ((Layer) comps[i]).getObjectAt(x, y, false);
          if (obj != null) obList.addElement(obj);
        } else if (comps[i] instanceof Panel) {
          obj = ((Panel) comps[i]).getObjectAt(x, y, false);
          if (obj != null) obList.addElement(obj);
        }
      }
    }
    return obList.toArray();
  }

  boolean processMouseEvent(MouseEvent event) {
    boolean event_handled = false;
    if (!mouseEventsEnabled_) return event_handled;
    if (event.getID() == MouseEvent.MOUSE_CLICKED) {
      event_handled = Pane_MouseClicked(event);
    } else if (event.getID() == MouseEvent.MOUSE_PRESSED) {
      event_handled = Pane_MouseDown(event);
    } else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
      event_handled = Pane_MouseUp(event);
    }
    return event_handled;
  }

  boolean processMouseMotionEvent(MouseEvent event) {
    boolean event_handled = false;
    if (!mouseEventsEnabled_) return event_handled;
    if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
      event_handled = Pane_MouseDrag(event);
    } else if (event.getID() == MouseEvent.MOUSE_MOVED) {
      event_handled = Pane_MouseMoved(event);
    }
    return event_handled;
  }

  private boolean Pane_MouseClicked(MouseEvent event) {
    Object obj;
    Rectangle rect;
    Selectable savedobj = null;
    int mod = event.getModifiers();
    //
    // button 1 must be clicked.  Modifiers are now allowed!
    //
    //      if(!(((mod & MouseEvent.BUTTON1_MASK) != 0) &&
    //           ((mod - MouseEvent.BUTTON1_MASK) == 0))) return false;
    if (!((mod & MouseEvent.BUTTON1_MASK) != 0)) return false;
    in_zoom_ = false;
    in_select_ = false;
    //    if(in_move_ && !moved_ && selectedobject_ instanceof Selectable) {
    if (!moved_ && selectedobject_ instanceof Selectable) {
      if (Debug.DEBUG) System.out.println("MouseClicked (in_move_ && !moved_ && Selectable)");
      //
      // second click of selected object   de-select
      //
      if (((Selectable) selectedobject_).isSelected()) {
        if (Debug.DEBUG) System.out.println("MouseClicked (isSelected)");
        savedobj = (Selectable) selectedobject_;
        //
        // remove box
        //
        Graphics g = pane_.getGraphics();
        if (moveable_) {
          g.setColor(Color.red);
        } else {
          g.setColor(Color.blue);
        }
        g.setXORMode(pane_.getBackground());
        g.drawRect(
            selectedRect_.x, selectedRect_.y,
            selectedRect_.width, selectedRect_.height);
        g.setPaintMode();
        savedobj.setSelected(false);
        pane_.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return false;
      }
    }
    //    old_selectedobject_ = selectedobject_;
    selectedobject_ = null;
    Component[] comps = pane_.getComponents();
    if (comps.length != 0) {
      Layer ly;
      for (int i = 0; i < comps.length; i++) {
        if (comps[i] instanceof Layer) {
          obj = ((Layer) comps[i]).getObjectAt(event.getX(), event.getY());
          if (obj != null) {
            selectedobject_ = obj;
            break;
          }
        } else if (comps[i] instanceof Panel) {
          obj = ((Panel) comps[i]).getObjectAt(event.getX(), event.getY(), false);
          if (obj != null) {
            selectedobject_ = obj;
            break;
          }
        }
      }
      if (selectedobject_ instanceof Selectable) {
        if (Debug.DEBUG) System.out.println("MouseClicked (new Selectable)");
        if (!selectedobject_.equals(savedobj) && ((Selectable) selectedobject_).isSelectable()) {
          if (Debug.DEBUG) System.out.println("MouseClicked (new isSelectable)");
          ((Selectable) selectedobject_).setSelected(true);
          //
          // mouseclick on selectable object select object
          //
          in_select_ = true;
          if (!in_move_) {
            if (Debug.DEBUG) System.out.println("MouseClicked (new !in_move_)");
            //
            // if in_move_ = true, already have drawn box
            //
            //
            // draw initial box
            //
            selectedRect_ = new Rectangle(((Selectable) selectedobject_).getBounds());
            Graphics g = pane_.getGraphics();
            if (moveable_) {
              g.setColor(Color.red);
            } else {
              g.setColor(Color.blue);
            }
            g.setXORMode(pane_.getBackground());
            g.drawRect(
                selectedRect_.x, selectedRect_.y,
                selectedRect_.width, selectedRect_.height);
            g.setPaintMode();
          }
          in_move_ = false;
          changes_.firePropertyChange("objectSelected", old_selectedobject_, selectedobject_);
        }
      } else if (selectedobject_ != null) {
        //
        // selectedobject is in a Key
        //
        changes_.firePropertyChange("objectSelected", old_selectedobject_, selectedobject_);
      }
    }
    return false; // pass event to next level
  }

  private boolean Pane_MouseMoved(MouseEvent event) {
    if (in_select_) {
      if (selectedRect_.contains(event.getX(), event.getY())) {
        pane_.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      } else {
        pane_.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
      return true;
    } else {
      pane_.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    return false;
  }

  private boolean Pane_MouseDown(MouseEvent event) {
    Object obj;
    Selectable savedobj = null;
    //
    // continue only if button1 is pressed
    //
    if ((event.getModifiers() & InputEvent.BUTTON1_MASK) == 0) return false;
    //
    // reset logicals
    //

    old_zoom_rect_ = new Rectangle(zoom_rect_);

    zoom_rect_.x = 0;
    zoom_rect_.y = 0;
    zoom_rect_.width = 0;
    zoom_rect_.height = 0;

    if (event.isShiftDown()) {
      //
      // shift mousedown start zoom
      //
      in_zoom_ = true;
      zoom_start_.x = event.getX();
      zoom_start_.y = event.getY();
      zoom_rect_.x = event.getX();
      zoom_rect_.y = event.getY();
      zoom_rect_.width = 1;
      zoom_rect_.height = 1;
      Graphics g = pane_.getGraphics();
      g.setColor(pane_.getForeground());
      g.setXORMode(pane_.getBackground());
      g.drawRect(
          zoom_rect_.x, zoom_rect_.y,
          zoom_rect_.width, zoom_rect_.height);
      g.setPaintMode();
      return true; // handled event here!
    } else if (event.isControlDown()) {
      //
      // control mousedown ends zoom
      //
      return false; // pass event to next level
    } else if (in_select_ && moveable_ && selectedRect_.contains(event.getX(), event.getY())) {
      if (Debug.DEBUG) System.out.println("MouseDown (in_select_ && in Rect)");
      //
      // object selected start move operation
      //
      in_move_ = true;
      moved_ = false;
      move_ref_ = new Point(event.getX(), event.getY());
      return true;
    } else if (in_select_ && !moveable_ && selectedRect_.contains(event.getX(), event.getY())) {
      in_move_ = false;
      moved_ = false;
      return true;
    } else {
      //
      // object not selected begin move operation
      //
      if (selectedobject_ instanceof Selectable) savedobj = (Selectable) selectedobject_;
      else savedobj = null;
      selectedobject_ = null;
      Component[] comps = pane_.getComponents();
      if (comps.length != 0) {
        Layer ly;
        for (int i = 0; i < comps.length; i++) {
          if (comps[i] instanceof Layer) {
            obj = ((Layer) comps[i]).getObjectAt(event.getX(), event.getY());
            if (obj != null) {
              selectedobject_ = obj;
              break;
            }
          } else if (comps[i] instanceof Panel) {
            obj = ((Panel) comps[i]).getObjectAt(event.getX(), event.getY(), false);
            if (obj != null) {
              selectedobject_ = obj;
              break;
            }
          }
        }
        if (selectedobject_ instanceof Selectable) {
          if (Debug.DEBUG) System.out.println("MouseDown (Selectable)");
          //
          // found selectable object begin single operation move
          //
          if (((Selectable) selectedobject_).isSelectable()) {
            if (Debug.DEBUG) System.out.println("MouseDown (isSelectable)");
            draggable_ = selectedobject_ instanceof Draggable;
            if (selectedobject_ instanceof Moveable) {
              moveable_ = ((Moveable) selectedobject_).isMoveable();
            } else {
              moveable_ = false;
            }
            if (moveable_ || draggable_) {
              in_move_ = true;
            } else {
              in_move_ = false;
            }
            moved_ = false;
            if (Debug.DEBUG) System.out.println("MouseDown (isDraggable) " + draggable_);
            ((Selectable) selectedobject_).setSelected(false);
            selectedRect_ = new Rectangle(((Selectable) selectedobject_).getBounds());
            if (!draggable_ && moveable_) {
              //
              // draw initial box
              //
              Graphics g = pane_.getGraphics();
              g.setColor(Color.red);
              g.setXORMode(pane_.getBackground());
              g.drawRect(
                  selectedRect_.x, selectedRect_.y,
                  selectedRect_.width, selectedRect_.height);
              g.setPaintMode();
            }
            if (!draggable_) pane_.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            move_ref_ = new Point(event.getX(), event.getY());
            return true;
          }
        }
      }
      //
      // mousedown start zoom if object not found
      //
      in_zoom_ = true;
      zoom_start_.x = event.getX();
      zoom_start_.y = event.getY();
      zoom_rect_.x = event.getX();
      zoom_rect_.y = event.getY();
      zoom_rect_.width = 1;
      zoom_rect_.height = 1;
      Graphics g = pane_.getGraphics();
      g.setColor(pane_.getForeground());
      g.setXORMode(pane_.getBackground());
      g.drawRect(
          zoom_rect_.x, zoom_rect_.y,
          zoom_rect_.width, zoom_rect_.height);
      g.setPaintMode();
      return true; // handled event here!
    }
    //    return false;
  }

  private boolean Pane_MouseDrag(MouseEvent event) {
    boolean handled = false;
    //
    // continue only if button1 is pressed
    //
    //    if((event.getModifiers() & InputEvent.BUTTON1_MASK) == 0) return handled;

    if (in_zoom_) {
      handled = true;
      Graphics g = pane_.getGraphics();
      g.setColor(pane_.getForeground());
      g.setXORMode(pane_.getBackground());
      g.drawRect(
          zoom_rect_.x, zoom_rect_.y,
          zoom_rect_.width, zoom_rect_.height);
      zoom_rect_.width = event.getX() - zoom_start_.x;
      if (zoom_rect_.width < 0) {
        zoom_rect_.x = event.getX();
        zoom_rect_.width = Math.abs(zoom_rect_.width);
      } else {
        zoom_rect_.x = zoom_start_.x;
      }
      zoom_rect_.height = event.getY() - zoom_start_.y;
      if (zoom_rect_.height < 0) {
        zoom_rect_.y = event.getY();
        zoom_rect_.height = Math.abs(zoom_rect_.height);
      } else {
        zoom_rect_.y = zoom_start_.y;
      }
      g.drawRect(
          zoom_rect_.x, zoom_rect_.y,
          zoom_rect_.width, zoom_rect_.height);
      g.setPaintMode();
    } else if (in_move_) {
      handled = true;
      moved_ = true;
      Graphics g = pane_.getGraphics();
      if (!draggable_ && moveable_) {
        g.setColor(Color.red);
        g.setXORMode(pane_.getBackground());
        g.drawRect(
            selectedRect_.x, selectedRect_.y,
            selectedRect_.width, selectedRect_.height);
      } else if (draggable_) {
        ((LayerChild) selectedobject_).setVisible(false);
        Rectangle rect = ((LayerChild) selectedobject_).getBounds();
        pane_.repaint(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);
      }
      selectedRect_.x += event.getX() - move_ref_.x;
      selectedRect_.y += event.getY() - move_ref_.y;
      if (!draggable_ && moveable_) {
        g.drawRect(
            selectedRect_.x, selectedRect_.y,
            selectedRect_.width, selectedRect_.height);
        g.setPaintMode();
      } else if (draggable_) {
        try {
          ((LayerChild) selectedobject_).setVisible(true);
          ((Draggable) selectedobject_)
              .setLocation(new Point(selectedRect_.x, selectedRect_.y), false);
          //      ((Draggable)selectedobject_).setLocationNoVeto(selectedRect_.x,
          //                                                      selectedRect_.y);
          ((LayerChild) selectedobject_).draw(g);
        } catch (LayerNotFoundException e) {
        }
      }
      move_ref_ = new Point(event.getX(), event.getY());
    }
    return handled;
  }

  private boolean Pane_MouseUp(MouseEvent event) {
    //
    // continue only if button1 is pressed
    //
    if ((event.getModifiers() & InputEvent.BUTTON1_MASK) == 0) return false;

    if (in_zoom_) {
      //
      // finish zoom
      //
      in_zoom_ = false;
      Graphics g = pane_.getGraphics();
      g.setColor(pane_.getForeground());
      g.setXORMode(pane_.getBackground());
      g.drawRect(
          zoom_rect_.x, zoom_rect_.y,
          zoom_rect_.width, zoom_rect_.height);
      changes_.firePropertyChange("zoomRectangle", old_zoom_rect_, zoom_rect_);
      //      if(!event.isShiftDown()) return true;  // abort zoom!
      return false; // pass event to next level
    } else if (in_move_ && moved_) {
      if (Debug.DEBUG) System.out.println("MouseUp (in_move_ && moved_)");
      //
      // finish move
      //
      Graphics g = pane_.getGraphics();
      if (!draggable_ && moveable_) {
        g.setColor(Color.red);
        g.setXORMode(pane_.getBackground());
        g.drawRect(
            selectedRect_.x, selectedRect_.y,
            selectedRect_.width, selectedRect_.height);
        g.setPaintMode();
      }
      // redraw, but should this only be if actually moved?
      if (Debug.DRAW_TRACE)
        System.out.println("PaneProxy: Pane_MouseUp: calling draw(), batch=" + batch_);
      Point loc = new Point(selectedRect_.x, selectedRect_.y);
      if (draggable_) {
        ((Draggable) selectedobject_).setLocation(loc);
        paint(g);
      } else if (moveable_) {
        ((Moveable) selectedobject_).setLocation(loc);
        // draw();
        modified_ = true;
        pane_.repaint();
      }
      in_move_ = false;
      in_select_ = false;
      moved_ = false;
      draggable_ = false;
      moveable_ = false;
      ((Selectable) selectedobject_).setSelected(false);
      pane_.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      return true;
    }
    return false;
  }

  Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Returns the minimum size of this <code>Pane</code>.
   *
   * @return minimum size
   */
  Dimension getMinimumSize() {
    return new Dimension(panesize_.width, panesize_.height);
  }

  /**
   * Returns the preferred size of this <code>Pane</code>.
   *
   * @return preferred size
   */
  Dimension getPreferredSize() {
    return new Dimension(panesize_.width, panesize_.height);
  }

  /**
   * Get a <code>String</code> representatinof the <code>Pane</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = pane_.getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + ident_;
  }

  void setBatch(boolean batch, String msg) {
    batch_ = batch;
    if (Debug.EVENT)
      System.out.println("PaneProxy: [" + ident_ + "] setBatch(" + batch_ + "): " + msg);
    if (!batch_ && modified_) {
      /*
       * batching turned off and environment has been modified
       * re-draw pane()
       */
      if (Debug.EVENT)
        System.out.println("PaneProxy: [" + ident_ + "] modified=true, draw(): batch off");
      // draw();
      if (Debug.DRAW_TRACE)
        System.out.println(
            "PaneProxy: ["
                + ident_
                + "] setBatch: calling repaint(), batch="
                + batch_
                + ", modified="
                + modified_);
      pane_.repaint();
      //      modified_ = false;
    }
  }

  boolean isBatch() {
    return batch_;
  }

  void setIgnoreModified(boolean ig) {
    ignoreModified_ = ig;
  }

  void clearModified() {
    modified_ = false;
  }

  void setModified(boolean mod, String mess) {
    if (ignoreModified_) return;
    modified_ = mod;
    if (Debug.EVENT && batch_) {
      System.out.println(
          "PaneProxy: [" + ident_ + "] setModified(" + modified_ + "), Batch on: " + mess);
    }
    if (modified_ && !batch_) {
      if (Debug.EVENT)
        System.out.println(
            "PaneProxy: [" + ident_ + "] setModified(" + modified_ + "), Batch off: " + mess);
      // draw();
      if (Debug.DRAW_TRACE)
        System.out.println(
            "PaneProxy.setModified: calling repaint(), batch="
                + batch_
                + ", modified="
                + modified_);
      pane_.repaint();
    }
  }

  boolean isModified() {
    return modified_;
  }

  /**
   * @since 3.0
   */
  void setMouseEventsEnabled(boolean enable) {
    mouseEventsEnabled_ = enable;
  }

  /**
   * @since 3.0
   */
  boolean isMouseEventsEnabled() {
    return mouseEventsEnabled_;
  }

  /**
   * @since 3.0
   */
  void setPageScaleMode(int mode) {
    printMode_ = mode;
  }

  /**
   * @since 3.0
   */
  int getPageScaleMode() {
    return printMode_;
  }

  /*
   * Pane PropertyChange methods
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }

  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
