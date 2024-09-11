/*
 * $Id: DataGroupPropertyPanel.java,v 1.2 2003/08/22 23:02:33 dwd Exp $
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
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;

import gov.noaa.pmel.sgt.PlainAxis;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.TimeAxis;
import gov.noaa.pmel.sgt.LogAxis;
import gov.noaa.pmel.sgt.LogTransform;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.IllegalTimeValue;

/**
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:33 $
 * @since 3.0
 **/
class DataGroupPropertyPanel extends PropertyPanel
    implements ActionListener, ChangeListener, FocusListener {
  private boolean expert_ = false;
  private String[] pNames_ = {"Id", "Margin", "Zoomable", "Z AutoScale", "Z Auto Levels", "Z User Range"};
  private JComponent[] comps_ = new JComponent[pNames_.length];
  private DataGroup dataGroup_ = null;
  private boolean suppressEvent_ = false;
  private int zAutoScale, zUserRange, zAutoLevels;

  public DataGroupPropertyPanel(DataGroup dg, boolean expert) {
    super();
    dataGroup_ = dg;
    dataGroup_.addChangeListener(this);
    expert_ = expert;
    create();
  }

  public void setDataGroup(DataGroup dg, boolean expert) {
    if(dataGroup_ != null) dataGroup_.removeChangeListener(this);
    dataGroup_ = dg;
    dataGroup_.addChangeListener(this);
    expert_ = expert;
    reset();
  }

  void update() {
    int i = -1;
    suppressEvent_ = true;
    int item = -1;
    ((JTextField)comps_[++i]).setText(dataGroup_.getId());
    ((JLabel)comps_[++i]).setText(dataGroup_.getMargin().toString());
    ((JCheckBox)comps_[++i]).setSelected(dataGroup_.isZoomable());
    //
    // z stuff
    //
    ((JCheckBox)comps_[++i]).setSelected(dataGroup_.isZAutoScale());
    ((JTextField)comps_[++i]).setText(format(dataGroup_.getNumberAutoContourLevels()));
    ((JTextField)comps_[++i]).setText(format(dataGroup_.getZRangeU(), false));
    //
    setFieldsEnabled();
    //
    suppressEvent_ = false;
  }

  void create() {
    int i = -1;
    int item = -1;
    comps_[++i] = createTextField(dataGroup_.getId(), pNames_[i], this, !dataGroup_.isInstantiated());
    comps_[++i] = createLabel(dataGroup_.getMargin().toString());
    comps_[++i] = createCheckBox(dataGroup_.isZoomable(), pNames_[i], this);
    //
    // z stuff
    //
    comps_[++i] = createCheckBox(dataGroup_.isZAutoScale(), pNames_[i], this);
    zAutoScale = i;
    comps_[++i] = createTextField(format(dataGroup_.getNumberAutoContourLevels()), pNames_[i], this, true);
    zAutoLevels = i;
    comps_[++i] = createTextField(format(dataGroup_.getZRangeU(), false), pNames_[i], this, true);
    zUserRange = i;
    //
    setFieldsEnabled();
//
    for(i=0; i < comps_.length; i++) {
      addProperty(i+1, pNames_[i], comps_[i], false);
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

  private void setFieldsEnabled() {
    ((JTextField)comps_[zUserRange]).setEnabled(!((JCheckBox)comps_[zAutoScale]).isSelected());
    ((JTextField)comps_[zAutoLevels]).setEnabled(((JCheckBox)comps_[zAutoScale]).isSelected());
  }

  private void processEvent(Object obj, String command) {
    String str = null;
    SoTRange range = null;
    if(command.equals("Id")) {
      String oldId = dataGroup_.getId();
      dataGroup_.getPanelHolder().getDataGroups().remove(oldId);
      dataGroup_.setId(((JTextField)obj).getText());
      dataGroup_.getPanelHolder().getDataGroups().put(dataGroup_.getId(), dataGroup_);
    } else if(command.equals("Zoomable")) {
      dataGroup_.setZoomable(((JCheckBox)obj).isSelected());
    } else if(command.equals("Z AutoScale")) {
      dataGroup_.setZAutoScale(((JCheckBox)obj).isSelected());
    } else if(command.equals("Z Auto Levels")) {
      dataGroup_.setNumberAutoContourLevels(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Z User Range")) {
      range = parseRange(((JTextField)obj).getText(), false);
      if(range != null) dataGroup_.setZRangeU(range);
    }
  }

  public void actionPerformed(ActionEvent e) {
    if(suppressEvent_) return;
    Object obj = e.getSource();
//    String str = null;
    String command = e.getActionCommand();
    processEvent(obj, command);
    setFieldsEnabled();
//    System.out.println(e.paramString() + ",rslt=" + str);
  }


  public void stateChanged(ChangeEvent e) {
    update();
  }

  public void focusGained(FocusEvent e) {
//    Object obj = e.getSource();
//    System.out.println("DataGroupPropertyPanel.focusGained: " + obj.toString());
  }

  public void focusLost(FocusEvent e) {
    Object obj = e.getSource();
    if(obj instanceof JTextField) {
      JTextField tf = (JTextField)obj;
      String name = tf.getName();
      processEvent(obj, name);
    }
  }

/*  SoTRange parseRange(String value, boolean isTime) {
    StringTokenizer tok = new StringTokenizer(value, ",\t\n\r\f");
    if(tok.countTokens() != 3) {
      JOptionPane.showMessageDialog(this, "Three values required", "Illegal Response", JOptionPane.ERROR_MESSAGE);
      return null;
    }
    SoTRange range = null;
    double start = Double.parseDouble(tok.nextToken().trim());
    double end = Double.parseDouble(tok.nextToken().trim());
    double delta  = Double.parseDouble(tok.nextToken().trim());
    range = new SoTRange.Double(start, end, delta);
    return range;
  } */

  public void setExpert(boolean expert) {
    boolean save = expert_;
    expert_ = expert;
    if(expert_ != save) reset();
  }

  public boolean isExpert() {
    return expert_;
  }
}