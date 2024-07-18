/*
 * $Id: LegendPropertyPanel.java,v 1.3 2003/09/02 22:40:39 dwd Exp $
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

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;

import gov.noaa.pmel.sgt.Ruler;
import gov.noaa.pmel.sgt.swing.prop.FontDialog;
import gov.noaa.pmel.sgt.swing.prop.ColorDialog;

/**
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/09/02 22:40:39 $
 * @since 3.0
 **/
class LegendPropertyPanel extends PropertyPanel implements ActionListener, ChangeListener, FocusListener {
  private boolean expert_ = false;
  private Legend legend_;
  private String[] pNames_ =
  { "Border",                  "Columns",              "Height",
    "Id",                      "Key Label HeightP",    "Line Length",
    "Location",
    "Type",                    "Scale Color",          "Scale Label Font",
    "Scale Label Format",      "Scale Label HeightP",  "Scale Label Interval",
    "Scale Large Tic HeightP", "Scale Num Small Tics", "Scale Significant Digits",
    "Scale Small Tic HeightP", "Scale Visible",        "Visible",
    "Width"};
  private boolean[] inColor_ =
  { true,                      false,                  true,
    true,                      true,                   false,
    true,
    true,                      true,                   true,
    true,                      true,                   true,
    true,                      true,                   true,
    true,                      true,                   true,
    true};
  private boolean[] inColorOnly_ =
  { false,                     false,                  false,
    false,                     false,                  false,
    false,
    false,                     true,                   true,
    true,                      true,                   true,
    true,                      true,                   true,
    true,                      true,                   false,
    false};
  private boolean[] expertItem_ =
  { false,                     false,                  false,
    false,                     true,                   false,
    false,
    false,                     true,                   true,
    true,                      true,                   false,
    true,                      true,                   false,
    true,                      true,                   false,
    false};
  private JComponent[] comps_ = new JComponent[pNames_.length];
  private String[] keyType = {"Line", "Color", "Vector", "Point"};
  private String[] borderType = {"Plain Line", "Raised", "No Border"};
  private DecimalFormat format_ = new DecimalFormat("#.###"); //2015-09-02 was static!

  public LegendPropertyPanel(Legend legend, boolean expert) {
    super();
    legend_ = legend;
    expert_ = expert;
    legend_.addChangeListener(this);
    reset();
  }

  public void setLegend(Legend legend, boolean expert) {
    if(legend_ != null) legend_.removeChangeListener(this);
    legend_ = legend;
    legend_.addChangeListener(this);
    expert_ = expert;
//    update();
    reset();
  }

  void update() {
    int item = -1;
    int i = -1;
    switch(legend_.getBorderStyle()) {
      default:
      case Legend.PLAIN_LINE:
        item = 0;
        break;
      case Legend.RAISED:
        item = 1;
        break;
      case Legend.NO_BORDER:
        item = 2;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JTextField)comps_[++i]).setText(Integer.toString(legend_.getColumns()));
    ((JTextField)comps_[++i]).setText(format(legend_.getHeightP(), format_));
    ((JTextField)comps_[++i]).setText(legend_.getId());
    ((JTextField)comps_[++i]).setText(format(legend_.getKeyLabelHeightP()));
    ((JTextField)comps_[++i]).setText(format(legend_.getLineLength()));
    ((JTextField)comps_[++i]).setText(format(legend_.getLocationP(), false));
    switch(legend_.getType()) {
      default:
      case Legend.LINE:
        item = 0;
        break;
      case Legend.COLOR:
        item = 1;
        break;
      case Legend.VECTOR:
        item = 2;
        break;
      case Legend.POINT:
        item = 3;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    updateColor((JButton)comps_[++i], legend_.getScaleColor());
    updateFont((JButton)comps_[++i], legend_.getScaleLabelFont());
    ((JTextField)comps_[++i]).setText(legend_.getScaleLabelFormat());
    ((JTextField)comps_[++i]).setText(format(legend_.getScaleLabelHeightP()));
    ((JTextField)comps_[++i]).setText(format(legend_.getScaleLabelInterval()));
    ((JTextField)comps_[++i]).setText(format(legend_.getScaleLargeTicHeightP()));
    ((JTextField)comps_[++i]).setText(format(legend_.getScaleNumberSmallTics()));
    ((JTextField)comps_[++i]).setText(format(legend_.getScaleSignificantDigits()));
    ((JTextField)comps_[++i]).setText(format(legend_.getScaleSmallTicHeightP()));
    ((JCheckBox)comps_[++i]).setSelected(legend_.isScaleVisible());
    ((JCheckBox)comps_[++i]).setSelected(legend_.isVisible());
    ((JTextField)comps_[++i]).setText(format(legend_.getWidthP(), format_));
  }

  void create() {
    int item = -1;
    int i = -1;
    switch(legend_.getBorderStyle()) {
      default:
      case Legend.PLAIN_LINE:
        item = 0;
        break;
      case Legend.RAISED:
        item = 1;
        break;
      case Legend.NO_BORDER:
        item = 2;
        break;
    }
    comps_[++i] = createComboBox(borderType, item, pNames_[i], this, true);
    comps_[++i] = createTextField(Integer.toString(legend_.getColumns()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getHeightP(), format_), pNames_[i], this, true);
    comps_[++i] = createTextField(legend_.getId(), pNames_[i], this, !legend_.isInstantiated());
    comps_[++i] = createTextField(format(legend_.getKeyLabelHeightP()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getLineLength()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getLocationP(), false), pNames_[i], this, true);
    String[] axisPosition;
    switch(legend_.getType()) {
      default:
      case Legend.LINE:
        item = 0;
        break;
      case Legend.COLOR:
        item = 1;
        break;
      case Legend.VECTOR:
        item = 2;
        break;
      case Legend.POINT:
        item = 3;
        break;
    }
    comps_[++i] = createComboBox(keyType, item, pNames_[i], this, !legend_.isInstantiated());
    comps_[++i] = createColor(legend_.getScaleColor(), pNames_[i], this);
    comps_[++i] = createFont(legend_.getScaleLabelFont(), pNames_[i], this);
    comps_[++i] = createTextField(legend_.getScaleLabelFormat(), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getScaleLabelHeightP()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getScaleLabelInterval()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getScaleLargeTicHeightP()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getScaleNumberSmallTics()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getScaleSignificantDigits()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(legend_.getScaleSmallTicHeightP()), pNames_[i], this, true);
    comps_[++i] = createCheckBox(legend_.isScaleVisible(), pNames_[i], this);
    comps_[++i] = createCheckBox(legend_.isVisible(), pNames_[i], this);
    comps_[++i] = createTextField(format(legend_.getWidthP(), format_), pNames_[i], this, true);

    for(i=0; i < comps_.length; i++) {
      if(expert_ || ! expertItem_[i])
      if(legend_.isColor()) {
        if(inColor_[i]) {
           addProperty(i+1, pNames_[i], comps_[i], false);
        }
      } else {
        if(!inColorOnly_[i]) {
           addProperty(i+1, pNames_[i], comps_[i], false);
        }
      }
    }
    addProperty(comps_.length + 1, " ", new JLabel(" "), true);
  }

  void resetFields() {
    for(int i=0; i < comps_.length; i++) {
      if(comps_[i] instanceof JTextField) {
        ((JTextField)comps_[i]).removeActionListener(this);
        ((JTextField)comps_[i]).removeFocusListener(this);
      } else if(comps_[i] instanceof JCheckBox) {
        ((JCheckBox)comps_[i]).removeActionListener(this);
        ((JCheckBox)comps_[i]).removeFocusListener(this);
      } else if(comps_[i] instanceof JComboBox) {
        ((JComboBox)comps_[i]).removeActionListener(this);
        ((JComboBox)comps_[i]).removeFocusListener(this);
      } else if(comps_[i] instanceof JButton) {
        ((JButton)comps_[i]).removeActionListener(this);
        ((JButton)comps_[i]).removeFocusListener(this);
      }
    }
  }
  private void processEvent(Object obj, String command) {
    if(command.equals("Height")) {
      legend_.setHeightP(Float.parseFloat(((JTextField)obj).getText()));
    } else if(command.equals("Id")) {
      String oldId = legend_.getId();
      legend_.getPanelHolder().getLegends().remove(oldId);
      legend_.setId(((JTextField)obj).getText());
      legend_.getPanelHolder().getLegends().put(legend_.getId(), legend_);
    } else if(command.equals("Key Label HeightP")) {
      legend_.setKeyLabelHeightP(Double.parseDouble(((JTextField)obj).getText()));
    } else if(command.equals("Border")) {
      String str = (String)((JComboBox)obj).getSelectedItem();
      int item = -1;
      if(str.equals("Plain Line")) {
        item = Legend.PLAIN_LINE;
      } else if(str.equals("Raised")) {
        item = Legend.RAISED;
      } else if(str.equals("No Border")) {
        item = Legend.NO_BORDER;
      }
      legend_.setBorderStyle(item);
    } else if(command.equals("Columns")) {
      legend_.setColumns(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Line Length")) {
      legend_.setLineLength(Double.parseDouble(((JTextField)obj).getText()));
    } else if(command.equals("Location")) {
      legend_.setLocationP(parsePoint2D(((JTextField)obj).getText()));
    } else if(command.equals("Type")) {
      String str = (String)((JComboBox)obj).getSelectedItem();
       int item = -1;
       if(str.equals("Line")) {
         item = Legend.LINE;
       } else if(str.equals("Color")) {
         item = Legend.COLOR;
       } else if(str.equals("Vector")) {
         item = Legend.VECTOR;
       } else if(str.equals("Point")) {
         item = Legend.POINT;
       }
       legend_.setType(item);
       reset();
    } else if(command.equals("Scale Color")) {
      ColorDialog cd = new ColorDialog(getFrame(), "Select Axis Color", true);
      cd.setColor(legend_.getScaleColor());
      cd.setVisible(true);
      Color newcolor = cd.getColor();
      if(newcolor != null) legend_.setScaleColor(newcolor);
    } else if(command.equals("Scale Label Font")) {
      FontDialog fd = new FontDialog("Label Font");
      int result = fd.showDialog(legend_.getScaleLabelFont());
      if(result == fd.OK_RESPONSE) {
        legend_.setScaleLabelFont(fd.getFont());
      }
    } else if(command.equals("Scale Label Format")) {
      legend_.setScaleLabelFormat(((JTextField)obj).getText());
    } else if(command.equals("Scale Label HeightP")) {
      legend_.setScaleLabelHeightP(Double.parseDouble(((JTextField)obj).getText()));
    } else if(command.equals("Scale Label Interval")) {
      legend_.setScaleLabelInterval(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Scale Large Tic HeightP")) {
      legend_.setScaleLargeTicHeightP(Double.parseDouble(((JTextField)obj).getText()));
    } else if(command.equals("Scale Num Small Tics")) {
      legend_.setScaleNumberSmallTics(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Scale Significant Digits")) {
      legend_.setScaleSignificantDigits(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Scale Small Tic HeightP")) {
      legend_.setScaleSmallTicHeightP(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Scale Visible")) {
      legend_.setScaleVisible(((JCheckBox)obj).isSelected());
    } else if(command.equals("Visible")) {
      legend_.setVisible(((JCheckBox)obj).isSelected());
    } else if(command.equals("Width")) {
      legend_.setWidthP(Float.parseFloat(((JTextField)obj).getText()));
    }
  }

 public void actionPerformed(ActionEvent e) {
    Object obj = e.getSource();
    String command = e.getActionCommand();
    processEvent(obj, command);
  }

  public void stateChanged(ChangeEvent e) {
    update();
  }
  public void focusGained(FocusEvent e) {
  }
  public void focusLost(FocusEvent e) {
    Object obj = e.getSource();
    if(obj instanceof JTextField) {
      JTextField tf = (JTextField)obj;
      String name = tf.getName();
      processEvent(obj, name);
    }
  }
  public void setExpert(boolean expert) {
    boolean save = expert_;
    expert_ = expert;
    if(expert_ != save) reset();
  }

  public boolean isExpert() {
    return expert_;
  }
}