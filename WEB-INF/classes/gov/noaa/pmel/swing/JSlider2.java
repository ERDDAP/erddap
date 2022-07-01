/*
 * $Id: JSlider2.java,v 1.9 2000/11/02 21:22:03 dwd Exp $
 */

package gov.noaa.pmel.swing;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.event.*;
import java.beans.*;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.BorderLayout;
import java.awt.Rectangle;

import gov.noaa.pmel.util.Range;
import gov.noaa.pmel.swing.beans.SliderHandle;

/**
 * JSlider2 provides the graphical input and feedback
 * for JSlider2Double and JSlider2Date.
 *
 * @author Donald Denbo
 * @version $Revision: 1.9 $, $Date: 2000/11/02 21:22:03 $
 * @see JSlider2Double
 * @see JSlider2Date
**/
public class JSlider2 extends JComponent 
  implements java.io.Serializable {
  protected Range rawRange_;
  protected int handleSize_;
  protected Dimension size_;
  protected int yval_;
  boolean showBorder_;
  boolean twoHandles_;
  boolean alwaysPost_;
  SliderHandle minHandle_, maxHandle_;
  int min_, max_;
  double scale_;
  double minValue_, maxValue_;
  double minOld_, maxOld_;
  String minLabel_, maxLabel_;
  private PropertyChangeSupport changes = new PropertyChangeSupport(this);
  boolean indexed_ = false;
  double[] values_;
  int[] pixels_;
  
  private EtchedBorder eBorder_ = null;
  /**
   * Class for the Date and Double JSlider2 classes. This class creates
   * the slider portion of the JSlider2Date and JSlider2Double classes.
   *
   * @see JSlider2Date
   * @see JSlider2Double
   **/
  public JSlider2() {
    this(true);
    setSize(40,40);
  }

  /**
   * Class for the GeoDate and Double JSlider2 classes. This class creates
   * the slider portion of the JSlider2Date and JSlider2Double classes.
   *
   * @param twoHandles if true create two handles
   * @see JSlider2Date
   * @see JSlider2Double
   **/
  public JSlider2(boolean twoHandles) {
    super();
    
    //{{REGISTER_LISTENERS
    SymMouse aSymMouse = new SymMouse();
    this.addMouseListener(aSymMouse);
    SymMouseMotion aSymMouseMotion = new SymMouseMotion();
    this.addMouseMotionListener(aSymMouseMotion);
    MyComponent aMyComponent = new MyComponent();
    this.addComponentListener(aMyComponent);
    //}}
                
    twoHandles_ = twoHandles;
    showBorder_ = true;
    eBorder_ = new EtchedBorder();
    setBorder(eBorder_);
    handleSize_ = 6;
    alwaysPost_ = false;
        
    size_ = new Dimension(175, 50);        
    rawRange_ = new Range(handleSize_ + 1, size_.width - handleSize_ - 1);
        
    reset();
        
    minLabel_ = Double.toString(minValue_);
    maxLabel_ = Double.toString(maxValue_);
        
    minHandle_ = new SliderHandle(handleSize_, Color.green, SliderHandle.LEFT);
    maxHandle_ = new SliderHandle(handleSize_, Color.red, SliderHandle.RIGHT);
    if(!twoHandles_) 
      minHandle_.setStyle(SliderHandle.SINGLE);
  }
  /**
   * Set the minimum handle value. Valid range 0.0 - 1.0.
   *
   * @param min minimum handle value
   **/
  public void setMinValue(double min) {
    minValue_ = Math.max(min, 0.0);
    if(minOld_ != minValue_) {
      Double tempOld = Double.valueOf(minOld_);
      minOld_ = minValue_;
      changes.firePropertyChange("minValue", tempOld, Double.valueOf(minValue_));  
    }
    repaint();
  }
  /**
   * Get the minimum handle value. 
   *
   * @return minimum handle value
   **/
  public double getMinValue() {
    return minValue_;
  }
  /**
   * Set the maximum handle value. Valid range 0.0 - 1.0.
   *
   * @param max maximum handle value
   **/
  public void setMaxValue(double max) {
    maxValue_ = Math.min(max, 1.0);
    if(maxOld_ != maxValue_) {
      Double tempOld = Double.valueOf(maxOld_);
      maxOld_ = maxValue_;
      changes.firePropertyChange("maxValue", tempOld, Double.valueOf(maxValue_));  
    }
    repaint();
  }
  /**
   * Get the maximum handle value.
   *
   * @return maximum handle value
   **/
  public double getMaxValue() {
    return maxValue_;
  } 
  /**
   * Reset the max,min values to the range limits
   **/
  public void reset() {
    maxValue_ = 1.0;
    minValue_ = 0.0;
    maxOld_ = 1.0;
    minOld_ = 0.0;
  }
   
  /**
   * Set the minimum label.
   *
   * @param lab minimum string
   **/
  public void setMinLabel(String lab) {
    minLabel_ = lab;
    repaint();
  }
  /**
   * Get the minimum label.
   *
   * @return the minimum label
   */
  public String getMinLabel() {
    return minLabel_;
  }
  /**
   * Set the maximum label.
   *
   * @param lab maximum string
   **/
  public void setMaxLabel(String lab) {
    maxLabel_ = lab;
    repaint();
  }
  /**
   * Get the maximum label.
   *
   * @return the maximum label
   */
  public String getMaxLabel() {
    return maxLabel_;
  }
  /**
   * Add a property change listener. The properties that fire a property
   * change are "minValue" and "maxValue". The old and new Double objects
   * are set.
   *
   * @param l property change listener
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes.addPropertyChangeListener(l);
  }
  /**
   * Remove a property change listener.
   *
   * @param l property change listener
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes.removePropertyChangeListener(l);
  }
  /**
   * Set the two handle mode.
   *
   * @param th if true set two handles
   */
  public void setTwoHandles(boolean th) {
    twoHandles_ = th;
    if(twoHandles_) {
      minHandle_.setStyle(SliderHandle.LEFT);
    } else {
      minHandle_.setStyle(SliderHandle.SINGLE);
    }
    repaint();
  }
  /**
   * Get the two handle flag.
   *
   * @return true if two handles
   */
  public boolean isTwoHandles() {
    return twoHandles_;
  }
  /**
   * Get the two handle flag.
   *
   * @return true if two handles
   */
  public boolean getTwoHandles() {
    return twoHandles_;
  }
  /**
   * Show a border around the slider.
   *
   * @param sb if true show the border
   */
  public void setShowBorder(boolean sb) {
    showBorder_ = sb;
    if(!showBorder_) setBorder(null);
    repaint();
  }
  /**
   * Get border status for the slider.
   *
   * @return true if border is showing
   */
  public boolean isShowBorder() {
    return showBorder_;
  }
  /**
   * Get border status for the slider.
   *
   * @return true if border is showing
   */
  public boolean getShowBorder() {
    return showBorder_;
  }
  /**
   * Set the handle size for the slider.
   *
   * @param sz handle size in pixels
   */
  public void setHandleSize(int sz) {
    if(handleSize_ != sz) {
      handleSize_ = sz;
      minHandle_ = new SliderHandle(handleSize_, Color.green);
      maxHandle_ = new SliderHandle(handleSize_, Color.red);
      repaint();
    }
  }
  /**
   * Get the current slider handle size.
   *
   * @return handle size in pixels
   */
  public int getHandleSize() {
    return handleSize_;
  }
  /**
   * Set the always post flag for the slider. If true any motion of the
   * slider will fire a property change, if false a property change is
   * only caused when the mouse button is released.
   *
   * @param ap if true always post
   */
  public void setAlwaysPost(boolean ap) {
    alwaysPost_ = ap;
  }
  /**
   * Get the always post flag for the slider.
   *
   * @return true if the slider will always post
   */
  public boolean isAlwaysPost() {
    return alwaysPost_;
  }
  /**
   * Get the always post flag for the slider.
   *
   * @return true if the slider will always post
   */
  public boolean getAlwaysPost() {
    return alwaysPost_;
  }
  /**
   * Get the range of the slider. The range is in pixels and is determined from
   * the size of the slider and the sizes of the handles.
   *
   * @return slider range
   **/
  public Range getRawRange() {
    return rawRange_;
  }

  public void setBounds(Rectangle r) {
    super.setBounds(r);
    invalidate();
    doCompute();
  }

  public void setBounds(int x, int y, int w, int h) {
    super.setBounds(x,y,w,h);
    invalidate();
    doCompute();
  }
  /**
   * Set the size of the slider.
   *
   * @param size slider size
   **/
  public void setSize(Dimension size) {
    super.setSize(size);
    invalidate();
    doCompute();
  }
  
  public void setSize(int width, int height) {
    super.setSize(width, height);
    invalidate();
    doCompute();
  }

  public void setIndexed(boolean ind) {
    indexed_ = ind;
  }
  
  public boolean isIndexed() {
    return indexed_;
  }
  
  public void paintComponent(Graphics g) {
    FontMetrics fmet;
    int xs, ys, swidth;
        
    min_ = rawRange_.start + (int)(minValue_*scale_);
    max_ = rawRange_.start + (int)(maxValue_*scale_);

    g.setColor(getBackground());
    g.fillRect(0, 0, size_.width, size_.height);

    g.setColor(Color.black);
//      if(showBorder_) {
//        g.drawRect(0, 0, size_.width-1, size_.height-1);
//      }
    g.setColor(Color.black);
    g.drawLine(rawRange_.start, yval_ - 3, rawRange_.end, yval_ - 3);
    g.setColor(Color.gray);
    g.drawLine(rawRange_.start, yval_ - 2, rawRange_.end, yval_ - 2);
    //
    // draw labels
    //
    g.setColor(Color.black);
    fmet = g.getFontMetrics();
    ys = yval_ - 8;
    xs = rawRange_.start;
    g.drawString(minLabel_, xs, ys);
    swidth = fmet.stringWidth(maxLabel_);
    xs = rawRange_.end - swidth;
    g.drawString(maxLabel_, xs, ys);
    //
    // draw handles
    //
    minHandle_.draw(g, min_, yval_);
    if(twoHandles_) maxHandle_.draw(g, max_, yval_);
  }
        
  public Dimension getMinimumSize() {
    return new Dimension(96,57);
  }
    
  public Dimension getPreferredSize() {
    return new Dimension(200,57);
  }
    
  public Dimension getMaximumSize() {
    return new Dimension(Short.MAX_VALUE,Short.MIN_VALUE);
  }
  
  public void setIndexValues(double[] array) {
    values_ = array;
    indexed_ = true;
    pixels_ = new int[values_.length];
    doCompute();
  }

  void doCompute() {
    size_ = getSize();
    rawRange_.start = 2*handleSize_ + 1;
    rawRange_.end = size_.width - 2*handleSize_ - 1;    
    yval_ = size_.height - 3*handleSize_ - 10;
    scale_ = (double)(rawRange_.end - rawRange_.start);
    if(indexed_) {
      for(int i=0; i < values_.length; i++) {
        pixels_[i] = rawRange_.start + (int)(values_[i]*scale_);
      }
    }
  }
 
  /**
   *
   **/
        
  void doMove(int xpos, boolean forcePost) {
    double value;
    if(xpos > rawRange_.end) xpos = rawRange_.end;
    if(xpos < rawRange_.start) xpos = rawRange_.start;
            
    if(indexed_ && forcePost) {
      int ind = 0;
      if(xpos <= pixels_[0]) {
        ind = 0;
      } else if(xpos >= pixels_[pixels_.length-1]) {
        ind = pixels_.length-1;
      } else {
        for(int i=0; i < pixels_.length - 1; i++) {
          if(xpos >= pixels_[i] && xpos < pixels_[i+1]) {
            if(xpos - pixels_[i] < pixels_[i+1] - xpos) {
              ind = i;
            } else {
              ind = i+1;
            }
            break;
          }
        }
      }
      value = values_[ind];
      xpos = pixels_[ind];
    } else {
      value = (double)(xpos - rawRange_.start)/scale_;
    }
    
    if(twoHandles_) {
      int toMin = xpos - min_;
      int toMax = xpos - max_;
      if(toMin < 0) {
        minValue_ = value;
      } else if(toMax > 0) {
        maxValue_ = value;
      } else if(toMin < -toMax) {
        minValue_ = value;
      } else {
        maxValue_ = value;
      }
    } else 
      minValue_ = value;
      
    if(forcePost || alwaysPost_) {
      if(minOld_ != minValue_) {
        Double tempOld = Double.valueOf(minOld_);
        minOld_ = minValue_;
        changes.firePropertyChange("minValue", tempOld, Double.valueOf(minValue_));
      }
      if(maxOld_ != maxValue_) {
        Double tempOld = Double.valueOf(maxOld_);
        maxOld_ = maxValue_;
        changes.firePropertyChange("maxValue", tempOld, Double.valueOf(maxValue_));
      }
    }
    paint(getGraphics());
  }

  class SymMouse extends MouseAdapter {
    public void mouseReleased(MouseEvent event) {
      Object object = event.getSource();
      if (object == JSlider2.this)
        JSlider2_MouseRelease(event);
    }

    public void mouseClicked(MouseEvent event) {
      Object object = event.getSource();
      if (object == JSlider2.this)
        JSlider2_MouseClick(event);
    }
  }

  void JSlider2_MouseClick(MouseEvent event) {
    doMove(event.getX(), true);
  }

  class MyComponent extends ComponentAdapter {
    public void componentResized(ComponentEvent event) {
      Object object = event.getSource();
      if(object == JSlider2.this)
        JSlider2_resized(event);
    }
  }
  
  void JSlider2_resized(ComponentEvent event) {
    doCompute();
  }
  
  class SymMouseMotion extends MouseMotionAdapter {
    public void mouseDragged(MouseEvent event) {
      Object object = event.getSource();
      if (object == JSlider2.this)
        JSlider2_MouseDrag(event);
    }
  }

  void JSlider2_MouseDrag(MouseEvent event) {
    doMove(event.getX(), false);
  }
  

  void JSlider2_MouseRelease(MouseEvent event) {
    doMove(event.getX(), true);
  }
  
  public static void main(String[] args) {
    double[] values = {0.0, 0.1, 0.2, 0.3, 0.4,
		       0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    JFrame jf = new JFrame("JSlider2 Test");
    jf.setSize(400,100);
    jf.getContentPane().setLayout(new BorderLayout());
    JSlider2 js2 = new JSlider2();
    //
    // valid range of slider is 0-1
    //
    js2.setDoubleBuffered(true);
    js2.setMinValue(0.2);
    js2.setMaxValue(0.8);
    js2.setIndexValues(values);
    js2.setMinLabel("-20 stuff");
    js2.setMaxLabel("20 stuff");
    jf.getContentPane().add(js2, BorderLayout.CENTER);
    jf.setVisible(true);
  }
}
