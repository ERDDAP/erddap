/*
 * $Id: PanelHolderDragBox.java,v 1.4 2003/09/16 22:02:14 dwd Exp $
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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Color;
import java.awt.Insets;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import gov.noaa.pmel.sgt.SGLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/09/16 22:02:14 $
 * @since 3.0
 **/
class PanelHolderDragBox extends DragBox implements ChangeListener {
  private List dragBox_ = new Vector();
  private boolean ignoreStateChange_ = false;

  public PanelHolderDragBox(PanelHolder ph) {
    super(ph);
//    ph.setPanelDragBox(this);
    ignoreStateChange_ = true;
    pHolder_.addChangeListener(this);
    Iterator iter = pHolder_.dataGroupIterator();
    while(iter.hasNext()) {
      DataGroup dg = (DataGroup)iter.next();
      DataGroupDragBox agdb = new DataGroupDragBox(dg, pHolder_);
      AxisHolderDragBox xAxdb = new AxisHolderDragBox(dg.getXAxisHolder(), dg, pHolder_);
      AxisHolderDragBox yAxdb = new AxisHolderDragBox(dg.getYAxisHolder(), dg, pHolder_);
      dragBox_.add(xAxdb);
      dragBox_.add(yAxdb);
      dragBox_.add(agdb);
    }
    iter = pHolder_.labelIterator();
    while(iter.hasNext()) {
      LabelDragBox ldb = new LabelDragBox((Label)iter.next(),  pHolder_);
      dragBox_.add(ldb);
    }
    iter = pHolder_.legendIterator();
    while(iter.hasNext()) {
      LegendDragBox lgdb = new LegendDragBox((Legend)iter.next(), pHolder_);
      dragBox_.add(lgdb);
    }

    for(int i=0; i < handles_.length;  i++) {
      handles_[i] = new Rectangle(0,0,0,0);
    }
    ignoreStateChange_ = false;
    computeHandles();
  }

  public PanelHolder getPanelHolder() {
    return pHolder_;
  }

  public DragBox[] getDragBoxArray() {
    DragBox[] dba = new DragBox[1];
    return (DragBox[])dragBox_.toArray(dba);
  }

  public Iterator getDragBoxIterator() {
    return dragBox_.iterator();
  }

  public void setSelected(boolean sel) {
    super.setSelected(sel);
    if(!selected_) {
      Iterator iter = dragBox_.iterator();
      while(iter.hasNext()) {
        ((DragBox)iter.next()).setSelected(sel);
      }
    }
  }

  public void update(String message) {
//    if(Page.DEBUG) System.out.println("PanelDragBox.update(" + message + ")");
    computeHandles();
    Iterator iter = dragBox_.iterator();
    while(iter.hasNext()) {
      ((DragBox)iter.next()).update(message);
    }
  }

  public void draw(Graphics g) {
    Color saved = g.getColor();
    Rectangle bounds = pHolder_.getBounds();
    if(!pHolder_.isUsePageBackground()) {
      g.setColor(pHolder_.getBackground());
      g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
    g.setColor(Color.darkGray);
    g.drawString(pHolder_.getId(), bounds.x + 10, bounds.y + 12);
    g.drawString("["+bounds.width+"x"+bounds.height+"]",
                 bounds.x + 10, bounds.y + 22);
    g.setColor(color_);
    if(pHolder_.isVisible()) g.drawRect(bounds.x, bounds.y, bounds.width-1, bounds.height-1);
    if(selected_) {
      for(int i=0; i < handles_.length; i++) {
        Rectangle r = handles_[i];
        g.fillRect(r.x, r.y, r.width-1, r.height-1);
      }
    }

    if(pHolder_.isVisible()) {
      Iterator iter = dragBox_.iterator();
      while(iter.hasNext()) {
        ((DragBox)iter.next()).draw(g);
      }
    }

    g.setColor(saved);
  }

  public String getId() {
    return pHolder_.getId();
  }

  public void setId(String id) {
    pHolder_.setId(id);
  }

  public void setBounds(Rectangle bounds) {
    ignoreStateChange_ = true;
    pHolder_.setBounds(bounds);
    update("PanelDragBox.setBounds()");
    ignoreStateChange_ = false;
  }

  public Rectangle getBounds() {
    return pHolder_.getBounds();
  }

  public void setLocation(Point pt) {
    setLocation(pt.x, pt.y);
  }

  public void setLocation(int x, int y) {
    Rectangle bounds = pHolder_.getBounds();
    bounds.x = x;
    bounds.y = y;
    ignoreStateChange_ = true;
    pHolder_.setBounds(bounds);
    update("PanelDragBox.setLocation()");
    ignoreStateChange_ = false;
  }

  public Point getLocation() {
    Rectangle bounds = pHolder_.getBounds();
    return new Point(bounds.x, bounds.y);
  }

  public void addDragBox(DataGroup ag) {
    DataGroupDragBox agdb = new DataGroupDragBox(ag, pHolder_);
    AxisHolderDragBox xAxdb = new AxisHolderDragBox(ag.getXAxisHolder(), ag, pHolder_);
    AxisHolderDragBox yAxdb = new AxisHolderDragBox(ag.getYAxisHolder(), ag, pHolder_);
    agdb.setAxisHolderDB(xAxdb, yAxdb);
    dragBox_.add(xAxdb);
    dragBox_.add(yAxdb);
    dragBox_.add(agdb);
  }

  public void addDragBox(Legend legend) {
    LegendDragBox ldb = new LegendDragBox(legend, pHolder_);
    dragBox_.add(ldb);
  }

  public void addDragBox(Label label) {
    LabelDragBox lbdb = new LabelDragBox(label, pHolder_);
    dragBox_.add(lbdb);
  }

  public void removeDragBox(DataGroupDragBox ag) {
    dragBox_.remove(ag.getXAxisHolderDB());
    dragBox_.remove(ag.getYAxisHolderDB());
    dragBox_.remove(ag);
    update("PanelDragBox.removeDragBox(DataGroupDragBox)");
  }

  public void removeDragBox(LegendDragBox legend) {
    dragBox_.remove(legend);
    update("PanelDragBox.removeDragBox(LegendDragBox)");
  }

  public void removeDragBox(LabelDragBox label) {
    dragBox_.remove(label);
    update("PanelDragBox.removeDragBox(LabelDragBox)");
  }

  public void stateChanged(ChangeEvent e) {
    if(ignoreStateChange_) return;
    update("PanelDragBox.stateChanged");
/*    System.out.println("PanelDragBox.stateChanged(" +
                       e.getSource().getClass().getName() + ")"); */
  }
}