/*
 * $Id: DragBox.java,v 1.2 2003/08/22 23:02:34 dwd Exp $
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
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Graphics;

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;

/**
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:34 $
 * @since 3.0
 **/
abstract class DragBox implements DesignListener {
  public static final int UPPER_LEFT = 0;
  public static final int UPPER_RIGHT = 1;
  public static final int LOWER_LEFT = 2;
  public static final int LOWER_RIGHT = 3;
  public static final int CENTER = 4;

  protected static int handleSize_ = 8;
  protected Rectangle[] handles_ = new Rectangle[5];
  protected int selectedHandle_ = -1;
  protected boolean selected_ = false;
  protected Color selectedColor_ = Color.red;
  protected Color unSelectedColor_ = Color.darkGray;
  protected Color color_ = unSelectedColor_;
  protected PanelHolder pHolder_;

  public DragBox(PanelHolder ph) {
    pHolder_ = ph;
  }

  public void setSelected(boolean sel) {
    selected_ = sel;
    if(selected_) {
      color_ = selectedColor_;
    } else {
      color_ = unSelectedColor_;
    }
  }

  public boolean isSelected() {
    return selected_;
  }

  public boolean handlesContain(Point pt) {
    for(int i=0; i < handles_.length; i++) {
      if(handles_[i].contains(pt)) {
        selectedHandle_ = i;
        return true;
      }
    }
    selectedHandle_ = -1;
    return false;
  }

  protected void computeHandles() {
    Rectangle bounds = getBounds();
    handles_[UPPER_LEFT].setBounds(bounds.x,
                                   bounds.y,
                                   handleSize_, handleSize_);
    handles_[UPPER_RIGHT].setBounds(bounds.x+bounds.width-handleSize_,
                                    bounds.y,
                                    handleSize_, handleSize_);
    handles_[LOWER_LEFT].setBounds(bounds.x,
                                   bounds.y+bounds.height-handleSize_,
                                   handleSize_, handleSize_);
    handles_[LOWER_RIGHT].setBounds(bounds.x+bounds.width-handleSize_,
                                    bounds.y+bounds.height-handleSize_,
                                    handleSize_, handleSize_);
    handles_[CENTER].setBounds(bounds.x+(bounds.width-handleSize_)/2,
                               bounds.y+(bounds.height-handleSize_)/2,
                               handleSize_, handleSize_);
  }

  public void mouseOperation(int op, int dx, int dy) {
    if(op == -1) return;
    Point pt;
    Rectangle rect;
    if(op == DragBox.CENTER) {
      pt = getLocation();
      pt.x += dx;
      pt.y += dy;
      setLocation(pt);
    } else {
      rect = getBounds();
      int x2 = rect.x + rect.width;
      int y2 = rect.y + rect.height;
      switch (op) {
        case DragBox.UPPER_LEFT:
          rect.x += dx;
          rect.y += dy;
          break;
        case DragBox.UPPER_RIGHT:
          x2 += dx;
          rect.y += dy;
          break;
        case DragBox.LOWER_LEFT:
          rect.x += dx;
          y2 += dy;
          break;
        case DragBox.LOWER_RIGHT:
          x2 += dx;
          y2 += dy;
      }
      rect.width = x2 - rect.x;
      rect.height = y2 - rect.y;
      setBounds(rect);
    }
  }

  public boolean contains(Point pt) {
    return getBounds().contains(pt);
  }

  public int getSelectedHandle() {
    return selectedHandle_;
  }

  Point2D.Double toLocation(Point pd) {
    return new Point2D.Double(toXLocation(pd.x),
                              toYLocation(pd.y));
  }

  Point toLocation(Point2D.Double pp) {
    return new Point(toXLocation(pp.x),
                     toYLocation(pp.y));
  }

  Rectangle2D.Double toRectangle(Rectangle rd) {
    return new Rectangle2D.Double(toXLocation(rd.x),
                                  toYLocation(rd.y + rd.height),
                                  transform(rd.width),
                                  transform(rd.height));
  }

  Rectangle toRectangle(Rectangle2D.Double rp) {
    int h = transform(rp.height);
    return new Rectangle(toXLocation(rp.x),
                         toYLocation(rp.y) - h,
                         transform(rp.width),
                         h);
  }

  double toXLocation(int xd) {
    return (xd - pHolder_.getBounds().x)/pHolder_.getPanelModel().getDpi();
  }

  double toYLocation(int yd) {
    return (pHolder_.getBounds().height - yd +
            pHolder_.getBounds().y)/pHolder_.getPanelModel().getDpi();
  }

  int toXLocation(double xp) {
    return (int)(xp*pHolder_.getPanelModel().getDpi()+0.5f) +
        pHolder_.getBounds().x;
  }

  int toYLocation(double yp) {
    return pHolder_.getBounds().height - (int)(yp*pHolder_.getPanelModel().getDpi()+0.5f) +
        pHolder_.getBounds().y;
  }

  double transform(int dev) {
    return dev/pHolder_.getPanelModel().getDpi();
  }

  int transform(double phy) {
    return (int)(phy*pHolder_.getPanelModel().getDpi()+0.5f);
  }

  abstract public void draw(Graphics g);
  abstract public void update(String message);
  abstract public void setId(String id);
  abstract public String getId();
  abstract public void setBounds(Rectangle bounds);
  abstract public Rectangle getBounds();
  abstract public void setLocation(Point pt);
  abstract public Point getLocation();
}