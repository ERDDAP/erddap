/*
 * $Id: PanelModelPropertyPanel.java,v 1.8 2003/09/18 17:31:44 dwd Exp $
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import gov.noaa.pmel.sgt.swing.prop.ColorDialog;
import gov.noaa.pmel.sgt.AbstractPane;

/**
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2003/09/18 17:31:44 $
 * @since 3.0
 **/
class PanelModelPropertyPanel extends PropertyPanel implements PropertyChangeListener, ActionListener {
  private boolean expert_ = false;
  private PanelModel pModel_ = null;
  private String[] pNames_ = {"DPI", "Page Color", "Page Size", "Panels",
                              "Print Borders",
    "Print HAlign", "Print Origin", "Print Scale Mode", "Print VAlign",
    "Print White"};
  private JComponent[] comps_ = new JComponent[pNames_.length];
  private String[] vAlignItems = {"Top", "Middle", "Bottom", "Specified Location"};
  private String[] hAlignItems = {"Left", "Center", "Right", "Specified Location"};
  private String[] sModeItems = {"Default", "To Fit", "Shrink To Fit"};

  public PanelModelPropertyPanel(PanelModel pm) {
    super();
    pModel_ = pm;
    pModel_.addPropertyChangeListener(this);

    create();
  }

  public void propertyChange(PropertyChangeEvent evt) {
    update();
  }

  void create() {
    int i = -1;
    int item = -1;
    comps_[++i] = createLabel(Float.toString(pModel_.getDpi()));
    comps_[++i] = createColor(pModel_.getPageBackgroundColor(), pNames_[i], this);
    comps_[++i] = createTextField(format(pModel_.getPageSize(), false),pNames_[i], this, true);
    comps_[++i] = createLabel(pModel_.getPanelCount());
    comps_[++i] = createCheckBox(pModel_.isPrintBorders(), pNames_[i], this);
    switch(pModel_.getPrintHAlign()) {
      case AbstractPane.LEFT:
        item = 0;
        break;
      default:
      case AbstractPane.CENTER:
        item = 1;
        break;
      case AbstractPane.RIGHT:
        item = 2;
        break;
      case AbstractPane.SPECIFIED_LOCATION:
        item = 3;
        break;
    }
    comps_[++i] = createComboBox(hAlignItems, item, pNames_[i], this, true);
    comps_[++i] = createTextField(format(pModel_.getPrintOrigin(), false), pNames_[i], this, true);
    switch(pModel_.getPrintScaleMode()) {
      default:
      case AbstractPane.DEFAULT_SCALE:
        item = 0;
        break;
      case AbstractPane.TO_FIT:
        item = 1;
        break;
      case AbstractPane.SHRINK_TO_FIT:
        item = 2;
        break;
    }
    comps_[++i] = createComboBox(sModeItems, item, pNames_[i], this, true);
    switch(pModel_.getPrintVAlign()) {
      default:
      case AbstractPane.TOP:
        item = 0;
        break;
      case AbstractPane.MIDDLE:
        item = 1;
        break;
      case AbstractPane.BOTTOM:
        item = 2;
        break;
      case AbstractPane.SPECIFIED_LOCATION:
        item = 3;
        break;
    }
    comps_[++i] = createComboBox(vAlignItems, item, pNames_[i], this, true);
    comps_[++i] = createCheckBox(pModel_.isPrintWhitePage(), pNames_[i], this);
    for(i=0; i < comps_.length; i++) {
      addProperty(i+1, pNames_[i], comps_[i], false);
    }
    addProperty(comps_.length + 1, " ", new JLabel(" "), true);
  }

  void update() {
    int i = -1;
    int item = -1;
    ((JLabel)comps_[++i]).setText(Float.toString(pModel_.getDpi()));
    updateColor((JButton)comps_[++i], pModel_.getPageBackgroundColor());
    ((JTextField)comps_[++i]).setText(format(pModel_.getPageSize(), false));
    ((JLabel)comps_[++i]).setText(Integer.toString(pModel_.getPanelCount()));
    ((JCheckBox)comps_[++i]).setSelected(pModel_.isPrintBorders());
    switch(pModel_.getPrintHAlign()) {
      case AbstractPane.LEFT:
        item = 0;
        break;
      default:
      case AbstractPane.CENTER:
        item = 1;
        break;
      case AbstractPane.RIGHT:
        item = 2;
        break;
      case AbstractPane.SPECIFIED_LOCATION:
        item = 3;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JTextField)comps_[++i]).setText(format(pModel_.getPrintOrigin(), false));
    switch(pModel_.getPrintScaleMode()) {
      default:
      case AbstractPane.DEFAULT_SCALE:
        item = 0;
        break;
      case AbstractPane.TO_FIT:
        item = 1;
        break;
      case AbstractPane.SHRINK_TO_FIT:
        item = 2;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    switch(pModel_.getPrintVAlign()) {
      default:
      case AbstractPane.TOP:
        item = 0;
        break;
      case AbstractPane.MIDDLE:
        item = 1;
        break;
      case AbstractPane.BOTTOM:
        item = 2;
        break;
      case AbstractPane.SPECIFIED_LOCATION:
        item = 3;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JCheckBox)comps_[++i]).setSelected(pModel_.isPrintWhitePage());
  }

  private void processEvent(Object obj, String command) {
    String str;
    int item = -1;
    if(command.equals("Page Size")) {
      Dimension size = parseDimension(((JTextField)obj).getText());
      pModel_.setPageSize(size);
      if(pModel_.getPage() != null) pModel_.getPage().setSize(size);
   } else if(command.equals("Page Color")) {
     ColorDialog cd = new ColorDialog(getFrame(), "Select Axis Color", true);
     cd.setColor(pModel_.getPageBackgroundColor());
     cd.setVisible(true);
     Color newcolor = cd.getColor();
     if(newcolor != null) {
       pModel_.setPageBackgroundColor(newcolor);
       updateColor((JButton)obj, newcolor);
     }
   } else if(command.equals("Print Borders")) {
     pModel_.setPrintBorders(((JCheckBox)obj).isSelected());
   } else if(command.equals("Print HAlign")) {
     str = (String)((JComboBox)obj).getSelectedItem();
     item = -1;
     if(str.equals("Left")) {
       item = AbstractPane.LEFT;
     } else if(str.equals("Center")) {
       item = AbstractPane.CENTER;
     } else if(str.equals("Right")) {
       item = AbstractPane.RIGHT;
     } else if(str.equals("Specified Location")) {
       item = AbstractPane.SPECIFIED_LOCATION;
     }
     pModel_.setPrintHAlign(item);
   } else if(command.equals("Print Origin")) {
     Point pt = parsePoint(((JTextField)obj).getText());
     if(pt != null) pModel_.setPrintOrigin(pt);

   } else if(command.equals("Print Scale Mode")) {
     str = (String)((JComboBox)obj).getSelectedItem();
     item = -1;
     if(str.equals("Default")) {
       item = AbstractPane.DEFAULT_SCALE;
     } else if(str.equals("To Fit")) {
       item = AbstractPane.TO_FIT;
     } else if(str.equals("Shrink To Fit")) {
       item = AbstractPane.SHRINK_TO_FIT;
     }
     pModel_.setPrintScaleMode(item);
   } else if(command.equals("Print VAlign")) {
     str = (String)((JComboBox)obj).getSelectedItem();
     item = -1;
     if(str.equals("Top")) {
       item = AbstractPane.TOP;
     } else if(str.equals("Middle")) {
       item = AbstractPane.MIDDLE;
     } else if(str.equals("Bottom")) {
       item = AbstractPane.BOTTOM;
     } else if(str.equals("Specified Location")) {
       item = AbstractPane.SPECIFIED_LOCATION;
     }
     pModel_.setPrintVAlign(item);
   } else if(command.equals("Print White")) {
     pModel_.setPrintWhitePage(((JCheckBox)obj).isSelected());
   }
  }

  void resetFields() {  }

  public void setExpert(boolean expert) {
    expert_ = expert;
  }

  public boolean isExpert() {
    return expert_;
  }
  public void actionPerformed(ActionEvent e) {
    Object obj = e.getSource();
    String command = e.getActionCommand();
    processEvent(obj, command);
  }
}