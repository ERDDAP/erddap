/*
 * $Id: LabelDragBox.java,v 1.2 2003/08/22 23:02:34 dwd Exp $
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

import java.awt.*;
import java.util.Iterator;

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:34 $
 * @since 3.0
 **/
class LabelDragBox extends DragBox implements ChangeListener {
  Label label_ = null;
  Rectangle boundsD_ = null;

  // deal with angles later

  public LabelDragBox(Label label, PanelHolder pHolder) {
    super(pHolder);
    label_ = label;
    label_.addChangeListener(this);
    for(int i=0; i < handles_.length;  i++) {
      handles_[i] = new Rectangle(0,0,0,0);
    }
    boundsD_ = toRectangle(label_.getBoundsP());
    computeHandles();
  }

  public Label getLabel() {
    return label_;
  }

  public void setBounds(Rectangle bounds) {
    boundsD_ = bounds;
    label_.setBoundsP(toRectangle(boundsD_));
    computeHandles();
  }

  public void draw(Graphics g) {
    Rectangle bounds = getBounds();
    Color saved = g.getColor();
    g.setColor(Color.darkGray);
    g.drawString(getId(), bounds.x + 5, bounds.y + 12);
    g.setColor(color_);
    if(label_.isVisible()) g.drawRect(bounds.x, bounds.y, bounds.width-1, bounds.height-1);
    if(selected_) {
      for(int i=0; i < handles_.length; i++) {
        Rectangle r = handles_[i];
        g.fillRect(r.x, r.y, r.width-1, r.height-1);
      }
    }
    g.setColor(saved);
  }

  public void setLocation(Point point) {
    boundsD_.x = point.x;
    boundsD_.y = point.y;
    label_.setBoundsP(toRectangle(boundsD_));
    computeHandles();
  }

  public Point getLocation() {
    return new Point(boundsD_.x, boundsD_.y);
  }

  public Rectangle getBounds() {
    return boundsD_;
  }

  public String getId() {
    return label_.getId();
  }

  public void update(String message) {
    boundsD_ = toRectangle(label_.getBoundsP());
    computeHandles();
   }

  public void setId(String id) {
    label_.setId(id);
  }

  public void stateChanged(ChangeEvent e) {
    update("LabelDragBox.stateChanged()");
  }
}