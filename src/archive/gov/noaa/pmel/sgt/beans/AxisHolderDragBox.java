/*
 * $Id: AxisHolderDragBox.java,v 1.2 2003/08/22 23:02:33 dwd Exp $
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.sgt.Axis;

/**
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:33 $
 * @since 3.0
 **/
class AxisHolderDragBox extends DragBox implements ChangeListener {
  // box dimensions
//  private static int BOX_HEIGHT = 25;
  private static int TIC_LENGTH = 10;
  private static int TIC_SPACE = 5;
  private static int TIC_INCREMENT = 25;
  private static int TITLE_HEIGHT = 10;
  private static int TITLE_SPACE = 5;
  // handle
  public static final int ORIGIN = 0;
  public static final int SIZE = 1;
  public static final int DUMMY_ORIGIN = 2;
  public static final int DUMMY_SIZE = 3;
  //
  private AxisHolder axHolder_;
  private Rectangle boundsD_ = null;
  private Point originD_ = null;
  private String id_ = null;
  private DataGroup dg_ = null;

  public AxisHolderDragBox(AxisHolder axHolder, DataGroup dg, PanelHolder pHolder) {
    super(pHolder);
    axHolder_ = axHolder;
    axHolder_.addChangeListener(this);
    dg_ = dg;
    dg_.addChangeListener(this);
    for(int i=0; i < handles_.length;  i++) {
      handles_[i] = new Rectangle(0,0,0,0);
    }
    id_ = axHolder_.getDataGroup().getId() +
          (axHolder_.getAxisOrientation() == DataGroup.X_DIR? "_X": "_Y");

    Point2D.Double orig = axHolder_.getAxisOriginP();
    if(orig != null) originD_ = toLocation(orig);
    Rectangle2D.Double bnds = (Rectangle2D.Double)axHolder_.getBoundsP();
    if(bnds != null) boundsD_ = toRectangle(bnds);

    update("AxisHolderDragBox.new()");
  }

  public AxisHolder getAxisHolder() {
    return axHolder_;
  }

  public void setBounds(Rectangle bounds) {
//    System.out.println("AxisHolderDragBox.setBounds: bounds updated");
    boundsD_ = bounds;
    axHolder_.setBoundsP(toRectangle(bounds));
    computeHandles();
  }

  public void setOrigin(Point pt) {
    originD_ = pt;
    axHolder_.setAxisOriginP(toLocation(originD_));
    computeHandles();
  }

  public Point getOrigin() {
    return (Point)originD_.clone();
  }

  public Point getLocation() {
    return new Point(boundsD_.x, boundsD_.y);
  }

  public void setId(String id) {
    // operation not allowed
  }

  public void draw(Graphics g) {
    Color savedColor = g.getColor();
    if(selected_) {
      g.setColor(Color.red);
    } else {
      g.setColor(Color.darkGray);
    }
    int xticStart, xticEnd;
    int yticStart, yticEnd;
    int xorg, yorg;
    Rectangle dgBounds = computeDataGroupBounds();  // if not MANUAL
    if(axHolder_.getAxisOrientation() == DataGroup.X_DIR) {
    // XAxis
      if(axHolder_.getAxisPosition() != DataGroup.MANUAL) {
        switch(axHolder_.getAxisPosition()) {
          default:
          case DataGroup.BOTTOM:
            yorg = dgBounds.y + dgBounds.height;
            break;
          case DataGroup.TOP:
            yorg = dgBounds.y;
            break;
        }
      } else { // MANUAL
        yorg = originD_.y;
      }
      switch(axHolder_.getTicPosition()) {
        default:
        case Axis.NEGATIVE_SIDE:
          yticStart = yorg;
          yticEnd = yorg + TIC_LENGTH;
          break;
        case Axis.POSITIVE_SIDE:
          yticStart = yorg - TIC_LENGTH;
          yticEnd = yorg;
          break;
        case Axis.BOTH_SIDES:
          yticStart = yorg - TIC_LENGTH;
          yticEnd = yorg + TIC_LENGTH;
      }
      if(axHolder_.isVisible()) {
        g.drawRect(boundsD_.x, boundsD_.y,
                   boundsD_.width, boundsD_.height);
        g.drawLine(boundsD_.x, yorg, boundsD_.x+boundsD_.width, yorg);
        // Some X tics
        for(int i=boundsD_.x; i < boundsD_.x+boundsD_.width; i += TIC_INCREMENT) {
          g.drawLine(i, yticStart, i, yticEnd);
        }
      }
    } else { // Y_DIR
      // YAxis
      if(axHolder_.getAxisPosition() != DataGroup.MANUAL) {
        switch(axHolder_.getAxisPosition()) {
          default:
          case DataGroup.LEFT:
            xorg = dgBounds.x;
            break;
          case DataGroup.RIGHT:
            xorg = dgBounds.x + dgBounds.width;
            break;
        }
      } else { // MANUAL
        xorg = originD_.x;
      }
      switch(axHolder_.getTicPosition()) {
        default:
        case Axis.NEGATIVE_SIDE:
          xticStart = xorg;
          xticEnd = xorg - TIC_LENGTH;
          break;
        case Axis.POSITIVE_SIDE:
          xticStart = xorg + TIC_LENGTH;
          xticEnd = xorg;
          break;
        case Axis.BOTH_SIDES:
          xticStart = xorg - TIC_LENGTH;
          xticEnd = xorg + TIC_LENGTH;
      }
      if(axHolder_.isVisible()) {
        g.drawRect(boundsD_.x, boundsD_.y,
                   boundsD_.width, boundsD_.height);
        g.drawLine(xorg, boundsD_.y, xorg, boundsD_.y+boundsD_.height);
        // Some Y tics
        for(int i=boundsD_.y; i < boundsD_.y+boundsD_.height;  i += TIC_INCREMENT) {
          g.drawLine(xticStart, i, xticEnd, i);
        }
      }
      // Name
/*      if(dg_.getXPosition() == DataGroup.BOTTOM) {
        yString += BOX_HEIGHT - 2;
      } else {
        yString += BOX_HEIGHT - TIC_LENGTH - 2;
      }
      g.drawString(dg_.getId(), boundsD_.x + 5, yString); */
    }
    if(selected_) {
      for(int i=0; i < handles_.length; i++) {
        Rectangle r = handles_[i];
        g.fillRect(r.x, r.y, r.width-1, r.height-1);
      }
    }

    g.setColor(savedColor);
  }

  private Rectangle computeDataGroupBounds() {
    Rectangle panel = pHolder_.getBounds();
    Margin margin = dg_.getMargin();
    float dpi = pHolder_.getPanelModel().getDpi();
    int left = (int)(margin.left*dpi);
    int right = (int)(margin.right*dpi);
    int top = (int)(margin.top*dpi);
    int bottom = (int)(margin.bottom*dpi);
    return new Rectangle(panel.x + left,
                         panel.y + top,
                         panel.width - (left + right),
                         panel.height - (top + bottom));
  }

  private static int exCount_ = 0;

  public void update(String message) {
    /** @todo can't use dgBounds when MANUAL! */
//    if(Page.DEBUG) System.out.println("AxisHolderDragBox.update(" + message + ")");
//    if(Page.DEBUG && exCount_++ <= 25) (new Exception()).printStackTrace();
//    if(exCount_ >= 25) System.exit(0);
    int ticPos = Axis.NEGATIVE_SIDE;
    int labPos = Axis.NEGATIVE_SIDE;
    int x, y, width, height;
    int xOrig, yOrig;
    Rectangle dgBounds = computeDataGroupBounds();
    if(axHolder_.getAxisOrientation() == DataGroup.X_DIR) {
      width = dgBounds.width;
      x = dgBounds.x;
      ticPos = axHolder_.getTicPosition();
      labPos = axHolder_.getLabelPosition();
      if(axHolder_.getAxisPosition() != DataGroup.MANUAL) {
        switch(axHolder_.getAxisPosition()) {
          default:
          case DataGroup.BOTTOM:
            y = dgBounds.y + dgBounds.height;
            break;
          case DataGroup.TOP:
            y = dgBounds.y;
            break;
        }
      } else { // MANUAL
        y = originD_.y;
        x = boundsD_.x;
        width = boundsD_.width;
      }
      xOrig = x;
      yOrig = y;

      height = TIC_SPACE + TITLE_SPACE;
      switch(labPos) {
        case Axis.NEGATIVE_SIDE:
          height += TITLE_HEIGHT;
          break;
        case Axis.POSITIVE_SIDE:
          height += TITLE_HEIGHT;
          y -= (TITLE_HEIGHT + TITLE_SPACE);
          break;
        case Axis.NO_LABEL:
      }
      switch(ticPos) {
        case Axis.NEGATIVE_SIDE:
          height += TIC_LENGTH;
          break;
        case Axis.POSITIVE_SIDE:
          height += TIC_LENGTH;
          y -= (TIC_LENGTH + TIC_SPACE);
          break;
        case Axis.BOTH_SIDES:
          height += 2*TIC_LENGTH + TIC_SPACE;
          y -= (TIC_LENGTH + TIC_SPACE);
          break;
      }
    } else { // Y_DIR
      height = dgBounds.height;
      y = dgBounds.y;
      ticPos = axHolder_.getTicPosition();
      labPos = axHolder_.getLabelPosition();
      if(axHolder_.getAxisPosition() != DataGroup.MANUAL) {
        switch(axHolder_.getAxisPosition()) {
          default:
          case DataGroup.LEFT:
            x = dgBounds.x;
            break;
          case DataGroup.RIGHT:
            x = dgBounds.x + dgBounds.width;
            break;
        }
      } else { // MANUAL
        x = originD_.x;
        y = boundsD_.y;
        height = boundsD_.height;
      }
      xOrig = x;
      yOrig = y;

      width = TIC_SPACE + TITLE_SPACE;
      switch(labPos) {
        case Axis.NEGATIVE_SIDE:
          width += TITLE_HEIGHT;
          x -= (TITLE_HEIGHT + TITLE_SPACE);
          break;
        case Axis.POSITIVE_SIDE:
          width += TITLE_HEIGHT;
          break;
        case Axis.NO_LABEL:
      }
      switch(ticPos) {
        case Axis.NEGATIVE_SIDE:
          width += TIC_LENGTH;
          x -= (TIC_LENGTH + TIC_SPACE);
          break;
        case Axis.POSITIVE_SIDE:
          width += TIC_LENGTH;
          break;
        case Axis.BOTH_SIDES:
          width += 2*TIC_LENGTH + TIC_SPACE;
          x -= (TIC_LENGTH + TIC_SPACE);
          break;
      }
    }
    setOrigin(new Point(xOrig, yOrig));
    setBounds(new Rectangle(x, y, width, height));
    computeHandles();
  }

  public void setLocation(Point point) {
    boundsD_.x = point.x;
    boundsD_.y = point.y;
    axHolder_.setBoundsP(toRectangle(boundsD_));
    computeHandles();
  }

  public Rectangle getBounds() {
    return (Rectangle)boundsD_.clone();
  }

  public String getId() {
    return id_;
  }

  public void stateChanged(ChangeEvent e) {
    update("AxisHolderDragBox.stateChanged()");
  }

  private Rectangle2D getBoundsP() {
    return null;
  }

  protected void computeHandles() {
    if(boundsD_ == null) return;
    int x, y;
    int ticPos = axHolder_.getTicPosition();
    Rectangle dgBounds = computeDataGroupBounds();
    if(axHolder_.getAxisOrientation() == DataGroup.X_DIR) {
      if(axHolder_.getAxisPosition() != DataGroup.MANUAL) {
        switch(axHolder_.getAxisPosition()) {
          default:
          case DataGroup.BOTTOM:
            y = dgBounds.y + dgBounds.height;
            break;
          case DataGroup.TOP:
            y = dgBounds.y;
            break;
        }
      } else { // MANUAL
        y = originD_.y;
      }
      if(ticPos == Axis.POSITIVE_SIDE ||
         ticPos == Axis.BOTH_SIDES) {
        y -= handleSize_ + 1;
      }
      handles_[ORIGIN].setBounds(boundsD_.x + 1,
                                 y,
                                 handleSize_, handleSize_);
      handles_[SIZE].setBounds(boundsD_.x + boundsD_.width - handleSize_ + 1,
                               boundsD_.y + (boundsD_.height - handleSize_)/2,
                               handleSize_, handleSize_);
    } else { // Y_DIR
      if(axHolder_.getAxisPosition() != DataGroup.MANUAL) {
        switch(axHolder_.getAxisPosition()) {
          default:
          case DataGroup.LEFT:
            x = dgBounds.x;
            break;
          case DataGroup.RIGHT:
            x = dgBounds.x + dgBounds.width;
            break;
        }
      } else { // MANUAL
        x = originD_.x;
      }
      if(ticPos == Axis.NEGATIVE_SIDE ||
         ticPos == Axis.BOTH_SIDES) {
        x -= handleSize_;
      }
      handles_[ORIGIN].setBounds(x,
                                 boundsD_.y + boundsD_.height - handleSize_ + 1,
                                 handleSize_, handleSize_);
      handles_[SIZE].setBounds(boundsD_.x + (boundsD_.width - handleSize_)/2,
                               boundsD_.y + 1,
                               handleSize_, handleSize_);
    }
  }

  public void mouseOperation(int op, int dx, int dy) {
    if(op == -1) return;
//    System.out.println("AxisHolderDragBox.mouseOperation(" + op +
//                      ", " + dx + ", " + dy + ")");
    Point pt;
    Rectangle rect;
    switch (op) {
      case ORIGIN:
        pt = getLocation();
        pt.x += dx;
        pt.y += dy;
        setLocation(pt);
        pt = getOrigin();
        pt.x += dx;
        pt.y += dy;
        setOrigin(pt);
        break;
      case SIZE:
        rect = getBounds();
        if(axHolder_.getAxisOrientation() == DataGroup.X_DIR) {
          rect.width += dx;
        } else {
          rect.height -= dy;
          rect.y += dy;
        }
        setBounds(rect);
        break;
      case DUMMY_ORIGIN:
      case DUMMY_SIZE:
    }
    axHolder_.setAxisPosition(DataGroup.MANUAL);
  }
}