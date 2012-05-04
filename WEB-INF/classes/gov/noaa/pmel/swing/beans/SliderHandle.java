/*
 * $Id: SliderHandle.java,v 1.2 2000/02/09 00:21:57 dwd Exp $
 */
 
package gov.noaa.pmel.swing.beans;
 
import java.awt.Color;
import java.awt.Graphics;
 
/**
 * Description of Class SliderHandle
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2000/02/09 00:21:57 $
**/
public class SliderHandle implements java.io.Serializable {
  static public int LEFT = 0;
  static public int RIGHT = 1;
  static public int SINGLE = 2;
  Color color_;
  int size_;
  int posx_;
  int posy_;
  int style_ = SINGLE;
  int[] xpts = new int[9];
  int[] ypts = new int[9];
  /**
   * Default constructor
   **/
  public SliderHandle() {
    this(6, Color.red, SINGLE);
  }
  public SliderHandle(int size, Color color) {
    this(size, color, SINGLE);
  }
  /**
   * SliderHandle constructor.
   *
   * @param xpos horizontal position of handle
   * @param ypos vertical position of handle
   * @param size size of handle in pixels
   * @param color handle color
   **/
  public SliderHandle(int size, Color color, int style) {
    size_ = size;
    color_ = color;
    style_ = style;
  }
  /**
   * Get the size of the handle in pixels;
   *
   * @return handle size
   **/
  public int getSize() {
    return size_;
  }
  /**
   * Set the size of the handle (in pixels).
   *
   * @param sz handle size in pixels
   **/
  public void setSize(int sz) {
    size_ = sz;
  }
  public void setStyle(int st) {
    style_ = st;
  }
  public int getStyle() {
    return style_;
  }
  /**
   * Get the color of the handle
   *
   * @return the handle color
   **/
  public Color getColor() {
    return color_;
  }
  /**
   * Set the handle color
   *
   * @param clr handle color
   **/
  public void setColor(Color clr) {
    color_ = clr;
  }
  /**
   * Get the current handle position
   *
   * @return current handle position
   **/
  public int getPosition() {
    return posx_;
  }
  public void draw(Graphics g, int posx, int posy) {
    g.setColor(color_);
        
    posx_ = posx;
    posy_ = posy;

    int pt = 0;
    if(style_ == SINGLE || style_ == LEFT) {
      xpts[0] = posx_;
      ypts[0] = posy_;
      xpts[1] = posx_ - size_;
      ypts[1] = posy_ + size_;
      xpts[2] = xpts[1];
      ypts[2] = ypts[1] + size_;
      xpts[3] = xpts[0];
      ypts[3] = ypts[2];
      xpts[4] = xpts[0];
      ypts[4] = ypts[0];
      pt = 5;
    }
    if(style_ == SINGLE || style_ == RIGHT) {
      if(pt == 0) {
        xpts[pt] = posx_;
        ypts[pt] = posy_;
        pt++;
      } else {
        pt = 4;
      }
      xpts[pt] = posx_;
      ypts[pt] = posy_ + 2*size_;
      xpts[pt+1] = posx_ + size_;
      ypts[pt+1] = ypts[pt];
      xpts[pt+2] = xpts[pt+1];
      ypts[pt+2] = posy_ + size_;
      xpts[pt+3] = posx_;
      ypts[pt+3] = posy_;
      pt = pt + 4;
    }
    
 /*   xpts[0] = posx_;
    ypts[0] = posy_;
    xpts[1] = posx_ - size_;
    ypts[1] = posy_ + 2*size_;
    xpts[2] = posx_ + size_;
    ypts[2] = ypts[1];
    xpts[3] = xpts[0];
    ypts[3] = ypts[0]; */
        
    g.fillPolygon(xpts, ypts, pt);
  }
}
