/*
 * $Id: JSlider2Double.java,v 1.8 2000/10/06 23:16:20 dwd Exp $
 */

package gov.noaa.pmel.swing;

import javax.swing.*;
import java.awt.event.*;
import java.beans.*;
//import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.text.DecimalFormat;

import gov.noaa.pmel.util.Range2D;

/**
 * Class provides graphical and textual input of a range.
 * Minimum value are required to be less than or equal 
 * to the maximum value.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2000/10/06 23:16:20 $
 * @see JSlider2
**/
public 
//class JSlider2Double extends Container implements java.io.Serializable
class JSlider2Double extends JComponent implements java.io.Serializable
{
  boolean twoHandles_;
  Range2D range_;
  double minValue_, oldMinValue_;
  double maxValue_, oldMaxValue_;
  double scale_;
  String format_ = "";
  DecimalFormat form_;
  boolean indexed_ = false;
  double[] values_;
  double[] scaled_;
/**
 * @link aggregationByValue
 * @clientCardinality 1
 * @supplierCardinality 1 
 */
  JSlider2 slider_;
  JLabel minLabel_;
  JTextField minField_;
  JLabel maxLabel_;
  JTextField maxField_;
  JPanel panel_;
  GridBagLayout layout_;

  private PropertyChangeSupport changes = new PropertyChangeSupport(this);
  
  /**
   * Default constructor. The default creates two handles.
   **/
  public JSlider2Double() {
    this(true);
  }
  /**
   * Constructs a one or two handled slider.
   *
   * @param twoHandles if true create two handles
   */
  public JSlider2Double(boolean twoHandles) {
    super();
    twoHandles_ = twoHandles;
    
    setLayout(new BorderLayout(0,0));
    panel_ = new JPanel();
    layout_ = new GridBagLayout();
    panel_.setLayout(layout_);
    panel_.setBounds(12,12,384,156);
    slider_ = new JSlider2();
    slider_.setBounds(5,20,374,49);
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(10,5,5,5);
    layout_.setConstraints(slider_, gbc);
    panel_.add(slider_);
    minLabel_ = new JLabel("Minimum:",JLabel.RIGHT);
    minLabel_.setBounds(6,79,68,23);
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets = new Insets(5,5,5,5);
    layout_.setConstraints(minLabel_, gbc);
    panel_.add(minLabel_);
    minField_ = new JTextField();
    minField_.setBounds(80,79,299,23);
    gbc = new GridBagConstraints();
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5,0,5,5);
    layout_.setConstraints(minField_, gbc);
    panel_.add(minField_);
    // second handle information
    maxLabel_ = new JLabel("Maximum:",JLabel.RIGHT);
    maxLabel_.setBounds(5,112,70,23);
    maxField_ = new JTextField();
    maxField_.setBounds(80,112,299,23);
    if(twoHandles_) {
      gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.NONE;
      gbc.insets = new Insets(5,5,10,5);
      layout_.setConstraints(maxLabel_, gbc);
      panel_.add(maxLabel_);
		
      gbc = new GridBagConstraints();
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.insets = new Insets(5,0,10,5);
      layout_.setConstraints(maxField_, gbc);
      panel_.add(maxField_);
    } else {
      minLabel_.setText("Value:");
    }
    add("Center", panel_);
		
    SymPropertyChange lSymPropertyChange = new SymPropertyChange();
    slider_.addPropertyChangeListener(lSymPropertyChange);
    SymAction lSymAction = new SymAction();
    minField_.addActionListener(lSymAction);
    maxField_.addActionListener(lSymAction);
	
    form_ = new DecimalFormat(format_);

    form_.setGroupingUsed(false);
    range_ = new Range2D(0.0f, 1.0f);
    setRange(range_);

  }
  /**
   * Set the range for the slider. The minimum value must be
   * less than the maximum value.
   *
   * @param min minimum value
   * @param max maximum value
   **/
  public void setRange(double min, double max) {
    setRange(new Range2D(min, max));
  }
  /**
   * Set the range for the slider.
   *
   * @param range slider total range
   **/
  public void setRange(Range2D range) {
    double min, max;
    range_ = range;
    scale_ = (double)(range_.end - range_.start);
    slider_.setMinLabel(form_.format(range_.start));
    slider_.setMaxLabel(form_.format(range_.end));
    
    minValue_ = range_.start;
    oldMinValue_ = minValue_;
    min = (minValue_ - range_.start)/scale_;
    slider_.setMinValue(min); 
    minField_.setText(form_.format(minValue_));
    
    maxValue_ = range_.end;
    oldMaxValue_ = maxValue_;
    max = (maxValue_ - range_.start)/scale_;
    slider_.setMaxValue(max);
    maxField_.setText(form_.format(maxValue_));
    
    if(indexed_) {
        for(int i=0; i < values_.length; i++) {
            scaled_[i] = (values_[i] - range_.start)/scale_;
        }
        slider_.setIndexValues(scaled_);
    }
  }
  /**
   * Get the slider range.
   *
   * @return slider range
   **/
  public Range2D getRange() {
    return range_;
  }
  /**
   * Set the minimum for the range.
   *
   * @param min minimum range value
   */
  public void setMinRange(double min) {
    range_.start = min;
    setRange(range_);
  }
  /**
   * Get the minimum for the range.
   *
   * @return minimum range value
   */
  public double getMinRange() {
    return (double)range_.start;
  }
  /**
   * Set the maximum for the range.
   *
   * @param max maximum range value
   */
  public void setMaxRange(double max) {
    range_.end = max;
    setRange(range_);
  }
  /**
   * Get the maximum for the range.
   *
   * @return maximum range value
   */
  public double getMaxRange() {
    return (double)range_.end;
  }
  /**
   * Reset the slider handles
   **/
  public void reset()
  {
    slider_.reset();
  }
  
  public void setIndexValues(double[] array) {
     values_ = array;
     indexed_ = true;
     scaled_ = new double[values_.length];
     setRange(range_);
  }
  /**
   * Set the two handle mode.
   *
   * @param th if true set two handles
   */
  public void setTwoHandles(boolean th) {
    GridBagConstraints gbc;
    twoHandles_ = th;
    slider_.setTwoHandles(th);
    if(twoHandles_) {
      if(!panel_.isAncestorOf(maxLabel_)) {
	gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.NONE;
	gbc.insets = new Insets(5,5,10,5);
	layout_.setConstraints(maxLabel_, gbc);
	panel_.add(maxLabel_);
      }
      if(!panel_.isAncestorOf(maxField_)) {
	gbc = new GridBagConstraints();
	gbc.weightx = 1.0;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.insets = new Insets(5,0,10,5);
	layout_.setConstraints(maxField_, gbc);
	panel_.add(maxField_);
      }
      minLabel_.setText("Minimum:");
      panel_.invalidate();
    } else {
      minLabel_.setText("Value:");
      if(panel_.isAncestorOf(maxLabel_)) panel_.remove(maxLabel_);
      if(panel_.isAncestorOf(maxField_)) panel_.remove(maxField_);
      panel_.invalidate();
    }

    slider_.setDoubleBuffered(true);
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
   * Get the two handle flag.
   *
   * @return true if two handles
   */
  public boolean isTwoHandles() {
    return twoHandles_;
  }
  /**
   * Set the format for the slider range label and display.
   *
   * @param frmt format in Format syntax
   */
  public void setFormat(String frmt) {
    format_ = frmt;
    form_ = new DecimalFormat(format_);
    form_.setGroupingUsed(false);
    setRange(range_);
  }
  /**
   * Get the format for the slider range label.
   *
   * @return the format in Format syntax
   */
  public String getFormat() {
    return format_;
  }
  public double getStartValue() {
    if(range_.start > range_.end) {
    minValue_ =
      Math.min((Double.valueOf(minField_.getText())).doubleValue(),
	       range_.start);
    } else {
    minValue_ =
      Math.max((Double.valueOf(minField_.getText())).doubleValue(),
	       range_.start);
    }
    return minValue_;
  }
  public double getEndValue() {
    if(range_.start > range_.end) {
    maxValue_ =
      Math.max((Double.valueOf(maxField_.getText())).doubleValue(), 
	       range_.end);
    } else {
    maxValue_ =
      Math.min((Double.valueOf(maxField_.getText())).doubleValue(), 
	       range_.end);
    }
    return maxValue_;
  }
  public void setStartValue(double min) {
    if(range_.start > range_.end) {
      minValue_ = Math.min(min, range_.start);
    } else {
      minValue_ = Math.max(min, range_.start);
    }
    min = (minValue_ - range_.start)/scale_;
    slider_.setMinValue(min); 
  }
  public void setEndValue(double max) {
    if(range_.start > range_.end) {
      maxValue_ = Math.max(max, range_.end);
    } else {
      maxValue_ = Math.min(max, range_.end);
    }
    max = (maxValue_ - range_.start)/scale_;
    slider_.setMaxValue(max);
  }
  /**
   * Get the minimum handle value.
   *
   * @return minimum handle value.
   **/
  public double getMinValue() {
    minValue_ = Math.max((Double.valueOf(minField_.getText())).doubleValue(), range_.start);
    minValue_ = Math.min(minValue_, maxValue_);
    return minValue_;
  }
  /**
   * Set the minimum handle value.
   *
   * @param min minimum handle value.
   **/
  public void setMinValue(double min) {
    minValue_ = min;
    minValue_ = Math.min(minValue_, maxValue_);
    min = (minValue_ - range_.start)/scale_;
    slider_.setMinValue(min); 
  }
  /**
   * Get the maximum handle value.
   *
   * @return maximum handle value
   **/
  public double getMaxValue() {
    maxValue_ = Math.min((Double.valueOf(maxField_.getText())).doubleValue(), range_.end);
    maxValue_ = Math.max(maxValue_, minValue_);
    return maxValue_;
  }
  /**
   * Set the maximum handle value.
   *
   * @param max maximum handle value
   **/
  public void setMaxValue(double max) {
    maxValue_ = max;
    maxValue_ = Math.max(maxValue_, minValue_);
    max = (maxValue_ - range_.start)/scale_;
    slider_.setMaxValue(max);
  }
  /**
   * Show a border around the slider.
   *
   * @param sb if true show the border
   */
  public void setShowBorder(boolean sb) {
    slider_.setShowBorder(sb);
  }
  /**
   * Get border status for the slider.
   *
   * @return true if border is showing
   */
  public boolean getShowBorder() {
    return slider_.getShowBorder();
  }
  /**
   * Set the handle size for the slider.
   *
   * @param sz handle size in pixels
   */
  public void setHandleSize(int sz) {
    slider_.setHandleSize(sz);
  }
  /**
   * Get the current slider handle size.
   *
   * @return handle size in pixels
   */
  public int getHandleSize() {
    return slider_.getHandleSize();
  }
  /**
   * Set the always post flag for the slider. If true any motion of the
   * slider will fire a property change, if false a property change is
   * only caused when the mouse button is released.
   *
   * @param ap if true always post
   */
  public void setAlwaysPost(boolean ap) {
    slider_.setAlwaysPost(ap);
  }
  /**
   * Get the always post flag for the slider.
   *
   * @return true if the slider will always post
   */
  public boolean getAlwaysPost() {
    return slider_.getAlwaysPost();
  }
  /**
   * Add a property change listener. The properties that fire a property
   * change are "minValue" and "maxValue". The old and new Double objects
   * values are set.
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
  class SymPropertyChange implements java.beans.PropertyChangeListener
  {
    public void propertyChange(java.beans.PropertyChangeEvent event)
    {
      Object object = event.getSource();
      if (object == slider_)
	slider_propertyChange(event);
    }
  }

  public Dimension getMinimumSize() {
    int hgt;
    if(twoHandles_)
      hgt = 135;
    else
      hgt = 105;
    return new Dimension(180,hgt);
  }
    
  public Dimension getPreferredSize() {
    int hgt;
    if(twoHandles_)
      hgt = 135;
    else
      hgt = 105;
    return new Dimension(384,hgt);
  }

  public boolean isIndexed() {
     return indexed_;
  }
  
  public void setIndexed(boolean ind) {
      indexed_ = ind;
  }
  
  public void setSize(Dimension dim) {
    super.setSize(dim);
    validate();
  }
  public void setSize(int w, int h) {
    super.setSize(w, h);
    validate();
  }
    
  public Dimension getMaximumSize() {
    return new Dimension(Short.MAX_VALUE,Short.MAX_VALUE);
  }

  void slider_propertyChange(java.beans.PropertyChangeEvent event)
  {
    if(event.getPropertyName().equals("minValue")) {
      minValue_ = range_.start + slider_.getMinValue()*scale_;
      minField_.setText(form_.format(minValue_));
      testMin();
    } else if(event.getPropertyName().equals("maxValue")) {
      maxValue_ = range_.start + slider_.getMaxValue()*scale_;
      maxField_.setText(form_.format(maxValue_));
      testMax();
    }
  }

  class SymAction implements java.awt.event.ActionListener
  {
    public void actionPerformed(java.awt.event.ActionEvent event)
    {
      Object object = event.getSource();
      if (object == minField_)
	minField_EnterHit(event);
      else if (object == maxField_)
	maxField_EnterHit(event);
    }
  }

  void minField_EnterHit(java.awt.event.ActionEvent event) {
    double min;
    minValue_ = Math.max((Double.valueOf(minField_.getText())).doubleValue(), range_.start);
    minValue_ = Math.min(minValue_, maxValue_);
    min = (minValue_ - range_.start)/scale_;
    slider_.setMinValue(min);
  }
  void maxField_EnterHit(java.awt.event.ActionEvent event) {
    double max;
    maxValue_ = Math.min((Double.valueOf(maxField_.getText())).doubleValue(), range_.end);
    maxValue_ = Math.max(maxValue_, minValue_);
    max = (maxValue_ - range_.start)/scale_;
    slider_.setMaxValue(max);
  }
  
  void testMax() {
    if(oldMaxValue_ != maxValue_) {
      Double tempOldValue = Double.valueOf(oldMaxValue_);
      oldMaxValue_ = maxValue_;
      changes.firePropertyChange("maxValue", tempOldValue, Double.valueOf(maxValue_)); 
    }
  }
  void testMin() {
    if(oldMinValue_ != minValue_) {
      Double tempOldValue = Double.valueOf(oldMinValue_);
      oldMinValue_ = minValue_;
      changes.firePropertyChange("minValue", tempOldValue, Double.valueOf(minValue_)); 
    }
  }

  public static void main(String[] args) {
      double[] values = {-20.0, -10.0, -5.0, -2.5, 0., 2.5, 5.0, 10., 15., 20.0};
    JFrame jf = new JFrame("JSlider2Double Test");
    jf.setSize(400,200);
    jf.getContentPane().setLayout(new BorderLayout());
    JSlider2Double js2db = new JSlider2Double();
    //
    js2db.setRange(new Range2D(-20.0, 20.0));
    js2db.setStartValue(-10.0);
    js2db.setEndValue(15.0);
    js2db.setIndexValues(values);
    js2db.setAlwaysPost(true);
    jf.getContentPane().add(js2db, BorderLayout.CENTER);
    jf.setVisible(true);
  }

}
