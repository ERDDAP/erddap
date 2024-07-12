/*
 * $Id: LabelPropertyPanel.java,v 1.4 2003/09/16 22:02:14 dwd Exp $
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
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.text.DecimalFormat;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import gov.noaa.pmel.sgt.swing.prop.ColorDialog;
import gov.noaa.pmel.sgt.swing.prop.FontDialog;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.Point2D;

/**
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/09/16 22:02:14 $
 * @since 3.0
 **/
class LabelPropertyPanel extends PropertyPanel implements ActionListener, ChangeListener, FocusListener {
  private boolean expert_ = false;
  private Label label_;
  private DecimalFormat format_ = new DecimalFormat("#.###"); //2015-09-02 was static
  private String[] pNames_ = {"Color", "Font", "Height", "Id", "Justification",
                              "Location", "Orientation", "Selectable", "Text", "Visible", "Width"};
  private JComponent[] comps_ = new JComponent[pNames_.length];
  private String[] justType = {"Left", "Center", "Right"};
  private String[] orientType = {"Horizontal", "Vertical"};

  public LabelPropertyPanel(Label label, boolean expert) {
    super();
    label_ = label;
    label_.addChangeListener(this);
    expert_ = expert;
    create();
  }

  public void setLabel(Label label, boolean expert) {
    if(label_ != null) label_.removeChangeListener(this);
    label_ = label;
    label_.addChangeListener(this);
    expert_ = expert;
    reset();
  }

  void update() {
    int item = -1;
    int i = -1;
    updateColor((JButton)comps_[++i], label_.getColor());
    updateFont((JButton)comps_[++i], label_.getFont());
    ((JTextField)comps_[++i]).setText(format(label_.getHeightP(), format_));
    ((JTextField)comps_[++i]).setText(label_.getId());
    switch(label_.getJustification()) {
      default:
      case SGLabel.LEFT:
        item = 0;
        break;
      case SGLabel.CENTER:
        item = 1;
        break;
      case SGLabel.RIGHT:
        item = 2;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JLabel)comps_[++i]).setText(format(label_.getLocationP(), true));
    switch(label_.getOrientation()) {
      default:
      case SGLabel.HORIZONTAL:
        item = 0;
        break;
      case SGLabel.VERTICAL:
        item = 1;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JCheckBox)comps_[++i]).setSelected(label_.isSelectable());
    ((JTextField)comps_[++i]).setText(label_.getText());
    ((JCheckBox)comps_[++i]).setSelected(label_.isVisible());
    ((JTextField)comps_[++i]).setText(format(label_.getWidthP(), format_));
  }

  void create() {
    int i = -1;
    int item = -1;
    comps_[++i] = createColor(label_.getColor(), pNames_[i], this);
    comps_[++i] = createFont(label_.getFont(), pNames_[i], this);
    comps_[++i] = createTextField(format(label_.getHeightP(), format_), pNames_[i], this, true);
    comps_[++i] = createTextField(label_.getId(), pNames_[i], this, !label_.isInstantiated());
    switch(label_.getJustification()) {
      default:
      case SGLabel.LEFT:
        item = 0;
        break;
      case SGLabel.CENTER:
        item = 1;
        break;
      case SGLabel.RIGHT:
        item = 2;
        break;
    }
    comps_[++i] = createComboBox(justType, item, pNames_[i], this, true);
    comps_[++i] = createLabel(format(label_.getLocationP(), true));
    switch(label_.getOrientation()) {
      default:
      case SGLabel.HORIZONTAL:
        item = 0;
        break;
      case SGLabel.VERTICAL:
        item = 1;
        break;
    }
    comps_[++i] = createComboBox(orientType, item, pNames_[i], this, true);
    comps_[++i] = createCheckBox(label_.isSelectable(), pNames_[i], this);
    comps_[++i] = createTextField(label_.getText(), pNames_[i], this, true);
    comps_[++i] = createCheckBox(label_.isVisible(), pNames_[i], this);
    comps_[++i] = createTextField(format(label_.getWidthP(), format_), pNames_[i], this, true);
    for(i=0; i < comps_.length; i++) {
      addProperty(i+1, pNames_[i], comps_[i], false);
    }
    addProperty(comps_.length + 1, " ", new JLabel(" "), true);
  }

  private void processEvent(Object obj, String command) {
    if(command.equals("Id")) {
      String oldId = label_.getId();
      label_.getPanelHolder().getLabels().remove(oldId);
      label_.setId(((JTextField)obj).getText());
      label_.getPanelHolder().getLabels().put(label_.getId(), label_);
    } else if(command.equals("Justification")) {
      String str = (String)((JComboBox)obj).getSelectedItem();
      int item = -1;
      if(str.equals("Left")) {
        item = SGLabel.LEFT;
      } else if(str.equals("Center")) {
        item = SGLabel.CENTER;
      } else if(str.equals("Right")) {
        item = SGLabel.RIGHT;
      }
      label_.setJustification(item);
    } else if(command.equals("Text")) {
      label_.setText(((JTextField)obj).getText());
    } else if(command.equals("Location")) {
      label_.setLocationP(parsePoint2D(((JTextField)obj).getText()));
    } else if(command.equals("Height")) {
      label_.setHeightP(Float.parseFloat(((JTextField)obj).getText()));
    } else if(command.equals("Width")) {
      label_.setWidthP(Float.parseFloat(((JTextField)obj).getText()));
    } else if(command.equals("Visible")) {
      label_.setVisible(((JCheckBox)obj).isSelected());
    } else if(command.equals("Color")) {
      ColorDialog cd = new ColorDialog(getFrame(), "Select Label Color", true);
      cd.setColor(label_.getColor());
      cd.setVisible(true);
      Color newcolor = cd.getColor();
      if(newcolor != null) label_.setColor(newcolor);
    } else if(command.equals("Font")) {
      FontDialog fd = new FontDialog("Label Font");
      int result = fd.showDialog(label_.getFont());
      if(result == fd.OK_RESPONSE) {
        label_.setFont(fd.getFont());
      }
    } else if(command.equals("Orientation")) {
      int old = label_.getOrientation();
      String str = (String)((JComboBox)obj).getSelectedItem();
      int item = -1;
      if(str.equals("Horizontal")) {
        item = SGLabel.HORIZONTAL;
      } else if(str.equals("Vertical")) {
        item = SGLabel.VERTICAL;
      }
      label_.setOrientation(item);
      /**
       * if orientation has changed redefine DragBox.
       */
      if(old != item) {
        Point2D.Double loc = label_.getLocationP();
        double w = label_.getWidthP();
        double h = label_.getHeightP();
        double x;
        double y;
        label_.setWidthP(label_.getHeightP());
        label_.setHeightP(w);
        switch(label_.getJustification()) {
          case SGLabel.CENTER:
            if(item == SGLabel.VERTICAL) {
              x = loc.x + w*0.5 - h;
              y = loc.y - w*0.5;
            } else {
              x = loc.x - h*0.5 + w;
              y = loc.y + h*0.5;
            }
            break;
          default:
          case SGLabel.LEFT:
            if(item == SGLabel.VERTICAL) {
              x = loc.x - h;
              y = loc.y;
            } else {
              x = loc.x + w;
              y = loc.y;
            }
            break;
          case SGLabel.RIGHT:
            if(item == SGLabel.VERTICAL) {
              x = loc.x + w - h;
              y = loc.y - w;
            } else {
              x = loc.x - h + w;
              y = loc.y + h;
            }
        }
        label_.setLocationP(new Point2D.Double(x,y));
      }
     } else if(command.equals("Selectable")) {
      label_.setSelectable(((JCheckBox)obj).isSelected());
    }
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
    expert_ = expert;
  }

  public boolean isExpert() {
    return expert_;
  }
}