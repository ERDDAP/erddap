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
import gov.noaa.pmel.util.Debug;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * PaneProxy implements the functionality common to <code>JPane</code> and <code>Pane</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.35 $, $Date: 2003/09/19 23:14:24 $
 */
public class PaneProxy { // Bob Simons made public
  public static final String SGTVersion = "3.0";
  public static boolean Java2D = false;
  public static StrokeDrawer strokeDrawer = null;

  private Container pane_;

  private String ident_;
  private boolean opaque_ = true;
  private boolean batch_ = true;
  private boolean modified_ = false;
  private boolean ignoreModified_ = false;

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception {
    try {
      pane_ = null; // not releaseResources -> infinite loop
      if (JPane.debug) String2.log("sgt.PaneProxy.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  public PaneProxy(Container pane, String id, Dimension size) {
    ident_ = id;
    pane_ = pane;
    //
    testJava2D();
  }

  private void testJava2D() {
    @SuppressWarnings("unused")
    Class unusedCl;
    boolean java2d = true;
    try {
      unusedCl = Class.forName("java.awt.Graphics2D");
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

  void draw(Graphics g) {
    Rectangle clip = pane_.getBounds();
    draw(g, clip.width, clip.height);
  }

  void draw(Graphics g, int width, int height) {
    ignoreModified_ = true;
    if (Debug.DEBUG) System.out.println("PaneProxy: [" + ident_ + "] draw(g), batch=" + batch_);
    g.setClip(0, 0, width, height);
    drawLayers(g);
    drawDraggableItems(g);
    modified_ = false;
    ignoreModified_ = false;
  }

  void setOpaque(boolean opaque) {
    opaque_ = opaque;
  }

  void drawLayers(Graphics g) {
    if (opaque_) {
      Rectangle r = pane_.getBounds();
      g.setColor(pane_.getBackground());
      g.fillRect(0, 0, r.width, r.height);
    }
    g.setColor(pane_.getForeground());
    // Pane drawing for layers
    //        g.drawRect(2,2,r.width-5,r.height-5);
    //
    // draw Layers
    //
    Component[] comps = pane_.getComponents();
    for (Component comp : comps) {
      try {
        if (comp instanceof Layer layer) {
          layer.draw(g);
        } else if (comp instanceof LayerControl layerControl) {
          layerControl.draw(g);
        }
        // printing support removed; keep drawing focused on layers and controls
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
    for (Component comp : comps) {
      try {
        if (comp instanceof LayerControl layerControl) {
          layerControl.drawDraggableItems(g);
        }
      } catch (PaneNotFoundException e) {
      }
    }
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

  void setModified(boolean mod, String mess) {
    if (ignoreModified_) return;
    modified_ = mod;
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
}
