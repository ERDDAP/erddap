/*
 * $Id: DesignPanel.java,v 1.3 2003/08/27 23:29:40 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.beans;


import java.awt.Color;
import java.awt.Point;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;

import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/08/27 23:29:40 $
 * @since 3.0
 **/
class DesignPanel extends JComponent implements MouseListener,
    MouseMotionListener, ChangeListener, DesignListener, PropertyChangeListener {
  public static final int ALIGN_TOP = 0;
  public static final int ALIGN_BOTTOM = 1;
  public static final int ALIGN_LEFT = 2;
  public static final int ALIGN_RIGHT = 3;
  public static final int JUSTIFY_VERTICAL = 4;
  public static final int JUSTIFY_HORIZONTAL = 5;

  private Map pDragBoxes_ = new HashMap();
  private Vector selectedBoxes_ = new Vector();
  private boolean inMove_ = false;
  private DragBox dragBox_ = null;
  private int dragState_ = -1;
  private Point move_ref_ = null;
  private PanelModel model_ = null;
  private DataGroup selectedDG_ = null;
  private Dimension size = new Dimension(100, 100);

//  private boolean inSelect_ = false;
  private int selectCount_ = 0;
  private Point firstSelectedPoint_ = new Point(-1, -1);
  private DragBox[] selectList_ = null;
  private static int SELECT_DISTANCE = 2;

  public DesignPanel() {
    super();
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  public DesignPanel(PanelModel model) {
    this();
    setPanelModel(model);
  }

  void jbInit() {
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public Dimension getMinimumSize() {
    return size;
  }

  public Dimension getPreferredSize() {
    return size;
  }

  public Dimension getMaximumSize() {
    return size;
  }

  public Dimension getWorkSize() {
    return size;
  }

  public void setPanelModel(PanelModel model) {
    pDragBoxes_.clear();
    selectedBoxes_.clear();

    if(model != null) model.removeChangeListener(this);
    model_ = model;
    if(model_ == null) return;

    model_.addChangeListener(this);
    Page pg = model_.getPage();
    if(pg == null) {
      size = model_.getPageSize();
      System.out.println("No page found!: size = " + size);
      setSize(size);
    } else {
      size = pg.getSize();
      if(Page.DEBUG) System.out.println("DeisignPanel: size = " + size);
      setSize(size);
    }

    Iterator iter = model_.panelIterator();
    while(iter.hasNext()) {
      PanelHolder ph = (PanelHolder)iter.next();
      ph.addChangeListener(this);
      PanelHolderDragBox pdb = new PanelHolderDragBox(ph);
      pDragBoxes_.put(ph, pdb);
    }
    repaint();
  }

  public PanelModel getPanelModel() {
    return model_;
  }

  public void addPanel(PanelHolderDragBox panel) {
    panel.getPanelHolder().addChangeListener(this);
    pDragBoxes_.put(panel.getPanelHolder(), panel);
  }

  public void addDataGroup(DataGroup dg) {
    PanelHolder ph = getSelectedPanel();
    if(ph == null) return;
    PanelHolderDragBox pdb = (PanelHolderDragBox)pDragBoxes_.get(ph);
    if(pdb == null) return;
    if(ph.getDataGroupSize() >= 1) {
      dg.getXAxisHolder().setAxisPosition(DataGroup.TOP);
      dg.getYAxisHolder().setAxisPosition(DataGroup.RIGHT);
    }
    pdb.addDragBox(dg);
    ph.addDataGroup(dg);
    // little fix
  }

  public void addLabel(Label label) {
    PanelHolder ph = getSelectedPanel();
    if(ph == null) return;
    PanelHolderDragBox pdb = (PanelHolderDragBox)pDragBoxes_.get(ph);
    if(pdb == null) return;
    pdb.addDragBox(label);
    ph.addLabel(label);
   }

  public void addLegend(Legend legend) {
    PanelHolder ph = getSelectedPanel();
    if(ph == null) return;
    PanelHolderDragBox pdb = (PanelHolderDragBox)pDragBoxes_.get(ph);
    pdb.addDragBox(legend);
    ph.addLegend(legend);
  }

  public void alignBoxes(int mode) {
    int count = getSelectedCount();
    if(count < 0) return;
    int y, x;
    PanelHolderDragBox pdb;
    PanelHolderDragBox box = (PanelHolderDragBox)selectedBoxes_.firstElement();
    Rectangle bounds = box.getBounds();
    switch (mode) {
      case ALIGN_TOP:
        if(count <= 1) return;
        y = bounds.y;
        for(int i=1; i < count; i++) {
          pdb = (PanelHolderDragBox)selectedBoxes_.elementAt(i);
          pdb.setLocation(pdb.getLocation().x, y);
        }
        break;
      case ALIGN_BOTTOM:
        if(count <= 1) return;
        int y2 = bounds.y + bounds.height;
        for(int i=1; i < count; i++) {
          pdb = (PanelHolderDragBox)selectedBoxes_.elementAt(i);
          y = y2 - pdb.getBounds().height;
          pdb.setLocation(pdb.getLocation().x, y);
        }
        break;
      case ALIGN_LEFT:
        if(count <= 1) return;
        x = bounds.x;
        for(int i=1; i < count; i++) {
          pdb = (PanelHolderDragBox)selectedBoxes_.elementAt(i);
          pdb.setLocation(x, pdb.getLocation().y);
        }
        break;
      case ALIGN_RIGHT:
        if(count <= 1) return;
        int x2 = bounds.x + bounds.width;
        for(int i=1; i < count; i++) {
          pdb = (PanelHolderDragBox)selectedBoxes_.elementAt(i);
          x = x2 - pdb.getBounds().width;
          pdb.setLocation(x, pdb.getLocation().y);
        }
        break;
      case JUSTIFY_VERTICAL:
        if(count >= 2) {
          for(int i=1; i < count; i++) {
            pdb = (PanelHolderDragBox)selectedBoxes_.elementAt(i);
            Rectangle bnds = pdb.getBounds();
            bnds.y = bounds.y;
            bnds.height = bounds.height;
            pdb.setBounds(bnds);
          }
        } else {
          Dimension sze = getWorkSize();
          bounds.y = 0;
          bounds.height = sze.height;
          box.setBounds(bounds);
        }
        break;
      case JUSTIFY_HORIZONTAL:
        if(count >= 2) {
          for(int i=1; i < count; i++) {
            pdb = (PanelHolderDragBox)selectedBoxes_.elementAt(i);
            Rectangle bnds = pdb.getBounds();
            bnds.x = bounds.x;
            bnds.width = bounds.width;
            pdb.setBounds(bnds);
          }
        } else {
          Dimension sze = getWorkSize();
          bounds.x = 0;
          bounds.width = sze.width;
          box.setBounds(bounds);
        }
    }
    repaint();
  }

  public void removeSelected() {
    PanelHolderDragBox pdb;
    PanelHolder ph;
    if(selectedBoxes_.size() == 1) {
      pdb = (PanelHolderDragBox)selectedBoxes_.firstElement();
      pdb.getPanelHolder().removeAllChangeListeners();
      // remove contents
      DragBox[] dbArray = pdb.getDragBoxArray();
      for(int i=0; i < dbArray.length; i++) {
        ph = pdb.getPanelHolder();
        if(dbArray[i] instanceof DataGroupDragBox) {
          DataGroup ag = ((DataGroupDragBox)dbArray[i]).getDataGroup();
          ph.removeDataGroup(ag);
          pdb.removeDragBox((DataGroupDragBox)dbArray[i]);
        } else if(dbArray[i] instanceof LegendDragBox) {
          Legend legend = ((LegendDragBox)dbArray[i]).getLegend();
          ph.removeLegend(legend);
          pdb.removeDragBox((LegendDragBox)dbArray[i]);
        } else if(dbArray[i] instanceof LabelDragBox) {
          Label label = ((LabelDragBox)dbArray[i]).getLabel();
          ph.removeLabel(label);
          pdb.removeDragBox((LabelDragBox)dbArray[i]);
        }
      }
      //    pdb.getPanelHolder().removeChangeListener(this);
      ph = pdb.getPanelHolder();
      pDragBoxes_.remove(ph);
      model_.removePanel(ph);
      ph.removeAllChangeListeners();
    } else {
      Iterator iter = pDragBoxes_.values().iterator();
      while(iter.hasNext()) {
        pdb = (PanelHolderDragBox)iter.next();
        Iterator dbIter = pdb.getDragBoxIterator();
        while(dbIter.hasNext()) {
          DragBox db = (DragBox)dbIter.next();
          if(db.isSelected()) {
            ph = pdb.getPanelHolder();
            if(db instanceof DataGroupDragBox) {
              DataGroup ag = ((DataGroupDragBox)db).getDataGroup();
              ph.removeDataGroup(ag);
              pdb.removeDragBox((DataGroupDragBox)db);
            } else if(db instanceof LegendDragBox) {
              Legend legend = ((LegendDragBox)db).getLegend();
              ph.removeLegend(legend);
              pdb.removeDragBox((LegendDragBox)db);
            } else if(db instanceof LabelDragBox) {
              Label label = ((LabelDragBox)db).getLabel();
              ph.removeLabel(label);
              pdb.removeDragBox((LabelDragBox)db);
            }
            break;
          }
        }
      }
    }
    repaint();
  }

  boolean isAxisHolderDragBoxSelected() {
    PanelHolderDragBox pdb;
    Iterator iter = pDragBoxes_.values().iterator();
    while(iter.hasNext()) {
      pdb = (PanelHolderDragBox)iter.next();
      Iterator dbIter = pdb.getDragBoxIterator();
      while(dbIter.hasNext()) {
        DragBox db = (DragBox)dbIter.next();
        if(db.isSelected() && db instanceof AxisHolderDragBox) {
          return true;
        }
      }
    }
    return false;
  }

  boolean isChildDragBoxSelected() {
    PanelHolderDragBox pdb;
    Iterator iter = pDragBoxes_.values().iterator();
    while(iter.hasNext()) {
      pdb = (PanelHolderDragBox)iter.next();
      Iterator dbIter = pdb.getDragBoxIterator();
      while(dbIter.hasNext()) {
        DragBox db = (DragBox)dbIter.next();
        if(db.isSelected()) {
          return true;
        }
      }
    }
    return false;
  }

  int getSelectedCount() {
    return selectedBoxes_.size();
  }

  PanelHolder getSelectedPanel() {
    if(selectedBoxes_.size() != 1) return null;
    return ((PanelHolderDragBox)selectedBoxes_.firstElement()).getPanelHolder();
  }

  public void paintComponent(Graphics g) {
    g.setColor(model_.getPageBackgroundColor());
    g.fillRect(0, 0, size.width, size.height);
    Iterator iter = pDragBoxes_.values().iterator();
    while(iter.hasNext()) {
      ((PanelHolderDragBox)iter.next()).draw(g);
    }
  }

  void clearAllSelections() {
    Iterator pIter = pDragBoxes_.values().iterator();
    while(pIter.hasNext()) {
      PanelHolderDragBox pdb = (PanelHolderDragBox)pIter.next();
      pdb.setSelected(false);
      Iterator dbIter = pdb.getDragBoxIterator();
      while(dbIter.hasNext()) {
        ((DragBox)dbIter.next()).setSelected(false);
      }
    }
  }

  private DragBox[] dragBoxesThatContain(Point pt) {
    PanelHolderDragBox pDragBox;
    Vector boxes = new Vector(4);
    Iterator pIter = pDragBoxes_.values().iterator();
    while(pIter.hasNext()) {
      pDragBox = (PanelHolderDragBox)pIter.next();
      if(pDragBox.contains(pt)) {
        Iterator dbIter = pDragBox.getDragBoxIterator();
        while(dbIter.hasNext()) {
          DragBox db = (DragBox)dbIter.next();
          if(db.contains(pt))  {
            boxes.add(db);  // add dragBox
          }
        } // dbIter
        boxes.add(pDragBox);  // add panelDragBox
      } // pDragBox contains pt
    } // pIter
    return (DragBox[])boxes.toArray(new DragBox[boxes.size()]);
  }

  private void resetSelect() {
    selectList_ = null;
    selectCount_ = 0;
    firstSelectedPoint_.setLocation(-1,-1);
  }

  private boolean newSelectPoint(Point pt) {
    int dx = Math.abs(pt.x - firstSelectedPoint_.x);
    int dy = Math.abs(pt.y - firstSelectedPoint_.y);

    return dx > SELECT_DISTANCE || dy > SELECT_DISTANCE;
  }

  // mouse listener
  public void mouseClicked(MouseEvent e) {
    // object selection/deselection
    DragBox dragBox = null;
    boolean dble = e.getClickCount() >= 2;
    int mods = e.getModifiers();
    Point pt = e.getPoint();
    boolean shiftctrl = (mods & InputEvent.CTRL_MASK) != 0 ||
                        (mods & InputEvent.SHIFT_MASK) != 0;
    // get selected DragBox
    if(selectList_ == null || newSelectPoint(pt)) {
      selectList_ = dragBoxesThatContain(pt);
      if(selectList_.length <= 0) {
        resetSelect();
        // nothing selected
        clearAllSelections();
        int old = getSelectedCount();
        selectedBoxes_.removeAllElements();
        repaint();
        firePropertyChange("allUnselected", null, null);

        inMove_ = false;
        dragState_ = -1;
        return;
      }
      selectCount_ = 0;
      firstSelectedPoint_.setLocation(pt);
      dragBox = selectList_[selectCount_];
    } else {
      selectCount_++;
      if(selectCount_ >= selectList_.length) selectCount_ = 0;
      dragBox = selectList_[selectCount_];
    }
    // process selected DragBox
    if(dragBox instanceof PanelHolderDragBox) {
      if(!shiftctrl && !dragBox.isSelected()) {
        clearAllSelections();
        selectedBoxes_.removeAllElements();
      }
      dragBox.setSelected(!dragBox.isSelected());
      if(dragBox.isSelected()) {
        selectedBoxes_.add(dragBox);
      } else {
        selectedBoxes_.remove(dragBox);
      }
      repaint();
      firePropertyChange("panelSelected", null, dragBox);
      return;
    } else {
      clearAllSelections();
      selectedBoxes_.removeAllElements();
      dragBox.setSelected(true);
      repaint();
      if(dragBox instanceof DataGroupDragBox) {
        firePropertyChange("dataGroupSelected", null, dragBox);
      } else if(dragBox instanceof AxisHolderDragBox) {
        firePropertyChange("axisSelected", null, dragBox);
      } else if(dragBox instanceof LabelDragBox) {
        firePropertyChange("labelSelected", null, dragBox);
      } else if(dragBox instanceof LegendDragBox) {
        firePropertyChange("legendSelected", null, dragBox);
      }
      return;
    }
  }

  public void mousePressed(MouseEvent e) {
    // initiate object move/resize
    PanelHolderDragBox pDragBox = null;
    DragBox db = null;
    dragState_ = -1;
    Iterator iter = pDragBoxes_.values().iterator();
    while(iter.hasNext()) {
      pDragBox = (PanelHolderDragBox)iter.next();
      if(pDragBox.isSelected() && pDragBox.handlesContain(e.getPoint())) {
        dragBox_ = pDragBox;
        dragState_ = dragBox_.getSelectedHandle();
        inMove_ = true;
        move_ref_ = new Point(e.getX(), e.getY());
        break;
      }
      // look for DragBoxes
      Iterator dbIter = pDragBox.getDragBoxIterator();
      while(dbIter.hasNext()) {
        db = (DragBox)dbIter.next();
        if(db.isSelected() && db.handlesContain(e.getPoint())) {
          dragBox_ = db;
          dragState_ = dragBox_.getSelectedHandle();
          inMove_ = true;
          move_ref_ = new Point(e.getX(), e.getY());
          break;
        }
      } // dbIter
    } // iter
  }

  public void mouseReleased(MouseEvent e) {
    inMove_ = false;
    dragState_ = -1;
  }
  public void mouseEntered(MouseEvent e) {
  }
  public void mouseExited(MouseEvent e) {
  }
  // mouse motion listener
  public void mouseDragged(MouseEvent e) {
    resetSelect();
    Point pt = null;
    Rectangle rect = null;
    int x = e.getX();
    int y = e.getY();
    if(inMove_) {
      int dx = x - move_ref_.x;
      int dy = y - move_ref_.y;
      dragBox_.mouseOperation(dragState_, dx, dy);
      repaint();
      move_ref_ = new Point(x, y);
    }  // inMove_
  }
  public void mouseMoved(MouseEvent e) {
  }

  public void stateChanged(ChangeEvent e) {
    size = model_.getPageSize();
    repaint();
  }

  public String toString() {
    return getClass().getName() + '@' + Integer.toHexString(hashCode());
  }
  public void propertyChange(PropertyChangeEvent evt) {
    if(evt.getPropertyName().equals("pageSize")) {
      size = model_.getPageSize();
      setSize(size);
      repaint();
    }
  }
}