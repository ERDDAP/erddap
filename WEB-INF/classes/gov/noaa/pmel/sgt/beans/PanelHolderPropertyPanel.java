/*
 * $Id: PanelHolderPropertyPanel.java,v 1.4 2003/08/27 23:29:40 dwd Exp $
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
import javax.swing.border.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import gov.noaa.pmel.sgt.swing.prop.ColorDialog;
/**
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/08/27 23:29:40 $
 * @since 3.0
 **/
class PanelHolderPropertyPanel extends PropertyPanel implements ActionListener, ChangeListener, FocusListener {
  private boolean expert_ = false;
  private PanelHolder pHolder_ = null;
  private String[] pNames_ = {"Background", "Border", "Bounds", "Data Groups",
    "Id", "Labels", "Legends", "Page Background", "Visible"};
  private JComponent[] comps_ = new JComponent[pNames_.length];

  public PanelHolderPropertyPanel(PanelHolder ph, boolean expert) {
    super();
    pHolder_ = ph;
    pHolder_.addChangeListener(this);
    expert_ = expert;
    create();
  }

  public void setPanelHolder(PanelHolder ph, boolean expert) {
    if(pHolder_ != null) pHolder_.removeChangeListener(this);
    pHolder_ = ph;
    pHolder_.addChangeListener(this);
    expert_ = expert;
    reset();
  }

  void create() {
    int i = -1;
    comps_[++i] = createColor(pHolder_.getBackground(), pNames_[i], this);
    comps_[++i] = createBorder(pHolder_.getBorder(), pNames_[i], this);
    Rectangle b = pHolder_.getBounds();
    comps_[++i] = createLabel(b.x + ", " + b.y + ", " + b.width + ", " + b.height);
    comps_[++i] = createLabel(pHolder_.getDataGroupSize());
    comps_[++i] = createTextField(pHolder_.getId(), pNames_[i], this, !pHolder_.isInstantiated());
    comps_[++i] = createLabel(pHolder_.getLabelSize());
    comps_[++i] = createLabel(pHolder_.getLegendSize());
    comps_[++i] = createCheckBox(pHolder_.isUsePageBackground(), pNames_[i], this);
    comps_[++i] = createCheckBox(pHolder_.isVisible(), pNames_[i], this);
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

  void update() {
    int i = -1;
    updateColor((JButton)comps_[++i], pHolder_.getBackground());
    updateBorder((JButton)comps_[++i], pHolder_.getBorder());
    Rectangle b = pHolder_.getBounds();
    ((JLabel)comps_[++i]).setText(b.x + ", " + b.y + ", " + b.width + ", " + b.height);
    ((JLabel)comps_[++i]).setText(Integer.toString(pHolder_.getDataGroupSize()));
    ((JTextField)comps_[++i]).setText(pHolder_.getId());
    ((JLabel)comps_[++i]).setText(Integer.toString(pHolder_.getLabelSize()));
    ((JLabel)comps_[++i]).setText(Integer.toString(pHolder_.getLegendSize()));
    ((JCheckBox)comps_[++i]).setSelected(pHolder_.isUsePageBackground());
    ((JCheckBox)comps_[++i]).setSelected(pHolder_.isVisible());
  }

  private void processEvent(Object obj, String command) {
    if(command.equals("Border")) {
      BorderDialog bd = new BorderDialog(null, "Select Border", true);
      bd.setBorder(pHolder_.getBorder());
      bd.setVisible(true);
      pHolder_.setBorder(bd.getBorder());
    } else if(command.equals("Id")) {
      String oldId = pHolder_.getId();
      pHolder_.getPanelModel().getPanelList().remove(oldId);
      pHolder_.setId(((JTextField)obj).getText());
      pHolder_.getPanelModel().getPanelList().put(pHolder_.getId(), pHolder_);
    } else if(command.equals("Visible")) {
      pHolder_.setVisible(((JCheckBox)obj).isSelected());
    } else if(command.equals("Background")) {
      ColorDialog cd = new ColorDialog(getFrame(), "Select Axis Color", true);
      cd.setColor(pHolder_.getBackground());
      cd.setVisible(true);
      Color newcolor = cd.getColor();
      if(newcolor != null) {
        pHolder_.setBackground(newcolor);
        updateColor((JButton)obj, newcolor);
      }
    } else if(command.equals("Page Background")) {
      pHolder_.setUsePageBackground(((JCheckBox)obj).isSelected());
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

  private JButton createBorder (Border value,
                                String action, ActionListener listen) {
    JButton jb = new JButton(getBorderDescription(value));
    jb.setActionCommand(action);
    if(listen != null) jb.addActionListener(listen);
    return jb;
  }

  private void updateBorder(JButton comp, Border value) {
    comp.setText(getBorderDescription(value));
  }

  private String getBorderDescription(Border border) {
    String description = "None";
    if(border != null) {
      if(border instanceof BevelBorder) {
        description = "Beveled";
      } else if(border instanceof EtchedBorder) {
        description = "Etched";
      } else if(border instanceof LineBorder) {
        description = "Line";
      } else if(border instanceof TitledBorder) {
        description = "Titled";
      }
    }
    return description;
  }

  public void setExpert(boolean expert) {
    expert_ = expert;
  }

  public boolean isExpert() {
    return expert_;
  }
}
