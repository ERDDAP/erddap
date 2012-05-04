/*
 * $Id: DataGroupDragBox.java,v 1.2 2003/08/22 23:02:33 dwd Exp $
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
//import gov.noaa.pmel.util.Rectangle2D;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.geom.Rectangle2D;

/**
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:33 $
 * @since 3.0
 **/
class DataGroupDragBox extends DragBox implements ChangeListener {
  private Rectangle boundsD_ = new Rectangle();
  private DataGroup dg_ = null;
  private AxisHolderDragBox xAxDB_ = null;
  private AxisHolderDragBox yAxDB_ = null;

  public DataGroupDragBox(DataGroup dg, PanelHolder pHolder) {
    super(pHolder);
    dg_ = dg;
    dg_.addChangeListener(this);
    for(int i=0; i < handles_.length;  i++) {
      handles_[i] = new Rectangle(0,0,0,0);
    }
    update("DataGroupDragBox.new()");
  }

  public DataGroup getDataGroup() {
    return dg_;
  }

  public void setBounds(Rectangle bounds) {
    boundsD_ = bounds;
    dg_.setMargin(computeMargin());
    computeHandles();
  }

  private Margin computeMargin() {
    Rectangle panel = pHolder_.getBounds();
    float dpi = pHolder_.getPanelModel().getDpi();
    float top = ((float)(boundsD_.y - panel.y))/dpi;
    float bottom = ((float)((panel.y + panel.height) - (boundsD_.y + boundsD_.height)))/dpi;
    float left = ((float)(boundsD_.x - panel.x))/dpi;
    float right = ((float)((panel.x + panel.width) - (boundsD_.x + boundsD_.width)))/dpi;
    return new Margin(top, left, bottom, right);
  }

  public void draw(Graphics g) {
    Rectangle bounds = getBounds();
    Color savedColor = g.getColor();
    if(selected_) {
      g.setColor(Color.red);
    } else {
      g.setColor(Color.green);
    }
    // draw AxisHolder

    // draw rectangle dashed
    Rectangle2D rect = new Rectangle2D.Float(bounds.x, bounds.y,
        bounds.width, bounds.height);
    Graphics2D g2 = (Graphics2D)g;
    Stroke saved = g2.getStroke();
    float[] dashes = {4.0f, 4.0f};
    BasicStroke stroke = new BasicStroke(1.0f,
                                         BasicStroke.CAP_SQUARE,
                                         BasicStroke.JOIN_MITER,
                                         10.0f,
                                         dashes,
                                         0.0f);
    g2.setStroke(stroke);
    g2.draw(rect);
    g2.setStroke(saved);
    // Name
/*    if(dg_.getXPosition() == DataGroup.BOTTOM) {
      yString += BOX_HEIGHT - 2;
    } else {
      yString += BOX_HEIGHT - TIC_LENGTH - 2;
    }
    g.drawString(dg_.getId(), xBoundsD_.x + 5, yString); */

    if(selected_) {
      for(int i=0; i < handles_.length; i++) {
        Rectangle r = handles_[i];
        g.fillRect(r.x, r.y, r.width-1, r.height-1);
      }
    }

    g.setColor(savedColor);
  }


  public void update(String message) {
//    if(Page.DEBUG) System.out.println("DataGroupDragBox.update(" + message + ")");
    Rectangle panel = pHolder_.getBounds();
    Margin margin = dg_.getMargin();
    float dpi = pHolder_.getPanelModel().getDpi();
    int left = (int)(margin.left*dpi);
    int right = (int)(margin.right*dpi);
    int top = (int)(margin.top*dpi);
    int bottom = (int)(margin.bottom*dpi);
    boundsD_.x = panel.x + left;
    boundsD_.y = panel.y + top;
    boundsD_.width = panel.width - (left + right);
    boundsD_.height = panel.height - (top + bottom);
    computeHandles();
  }

  public void setLocation(Point pt) {
    boundsD_.x = pt.x;
    boundsD_.y = pt.y;
    dg_.setMargin(computeMargin());
    computeHandles();
  }

  public Point getLocation() {
    return new Point(boundsD_.x, boundsD_.y);
  }

  public Rectangle getBounds() {
    return boundsD_;
  }

  public String getId() {
    return dg_.getId();
  }

  public void setId(String id) {
    dg_.setId(id);
  }

  public void setAxisHolderDB(AxisHolderDragBox x, AxisHolderDragBox y) {
    xAxDB_ = x;
    yAxDB_ = y;
  }

  public AxisHolderDragBox getXAxisHolderDB() {
    return xAxDB_;
  }

  public AxisHolderDragBox getYAxisHolderDB() {
    return yAxDB_;
  }

  public void stateChanged(ChangeEvent e) {
    update("DataGroupDragBox.stateChanged()");
  }
}