/*
 * $Id: AxisHolderPropertyPanel.java,v 1.3 2003/09/02 22:40:39 dwd Exp $
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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.Vector;
import java.util.Iterator;

import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.IllegalTimeValue;

import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.TimeAxis;
import gov.noaa.pmel.sgt.swing.prop.SGLabelDialog;
import gov.noaa.pmel.sgt.swing.prop.FontDialog;
import gov.noaa.pmel.sgt.swing.prop.ColorDialog;

/**
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/09/02 22:40:39 $
 * @since 3.0
 **/
class AxisHolderPropertyPanel extends PropertyPanel
    implements ActionListener, FocusListener, ChangeListener {
  private boolean expert_ = false;
  private String[] pNames_ =
  { "AutoScale",               "AxisColor",       "Axis Position",
    "Axis Location",           "AxisType",        "Bounds",
    "Label Color",             "Label Font",      "Label Format",
    "Label Height",            "Label Interval",  "Label Position",
    "Label SignificantDigits", "Large TicHeight", "Location At Origin",
    "Major Format",            "Major Interval",  "Minor Format",
    "Minor Interval",          "Num Small Tics",   "OriginP",
    "Selectable",
    "Small TicHeight",         "Tic Position",    "Time AxisStyle",
    "Title Auto",              "Title",           "Transform Type",
    "User Range",              "Visible"};
  private boolean[] expertItem =
  { false,                     true,              false,  //autoScale
    true,                      false,              true,   //axisLocation
    true,                      false,              true,   //labelColor
    false,                     false,              true,   //labelHeight
    false,                     true,               true,  //sigDigits
    true,                      true,               true,   //majorFormat
    true,                      false,              true,   //minorInterval
    true,  //selectable
    true,                      true,               false,  //smallTicheight
    false,                     false,              false,  //titleAuto
    false,                      false};                     //userRange
  private JComponent[] comps_ = new JComponent[pNames_.length];
  private AxisHolder axHolder_;
  private PanelHolder pHolder_;
  private boolean suppressEvent_ = false;
  private int autoScale, userRange;
  private int titleAuto, title;
  //2011-12-15 Bob Simons changed space to 'T'  and hh to HH (24 hour)
  private String format_ = "yyyy-MM-dd'T'HH:mm";
  private String[] xAxisPosition = {"Bottom", "Top", "Manual"};
  private String[] yAxisPosition = {"Left", "Right", "Manual"};

  public AxisHolderPropertyPanel(AxisHolder axHolder, boolean expert) {
    axHolder_ = axHolder;
    axHolder_.addChangeListener(this);
    expert_ = expert;
    pHolder_ = axHolder_.getDataGroup().getPanelHolder();
    create();
  }

  public void setAxisHolder(AxisHolder axHolder, boolean expert) {
    if(axHolder_ != null) axHolder_.removeChangeListener(this);
    axHolder_ = axHolder;
    axHolder_.addChangeListener(this);
    expert_ = expert;
    reset();
  }

  void update() {
    int i = -1;
    suppressEvent_ = true;
    int item = -1;
    ((JCheckBox)comps_[++i]).setSelected(axHolder_.isAutoRange());
    updateColor((JButton)comps_[++i], axHolder_.getAxisColor());
    switch(axHolder_.getAxisPosition()) {
      default:
      case DataGroup.BOTTOM:
        item = 0;
        break;
      case DataGroup.TOP:
        item = 1;
        break;
      case DataGroup.LEFT:
        item = 0;
        break;
      case DataGroup.RIGHT:
        item = 1;
        break;
      case DataGroup.MANUAL:
        item = 2;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JLabel)comps_[++i]).setText(format(axHolder_.getAxisOriginP(), true));
    switch(axHolder_.getAxisType()) {
      default:
      case DataGroup.PLAIN:
        item = 0;
        break;
      case DataGroup.TIME:
        item = 1;
        break;
      case DataGroup.LOG:
        item = 2;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JLabel)comps_[++i]).setText(format((Rectangle2D.Double)axHolder_.getBoundsP(), true));  // bounds?
    updateColor((JButton)comps_[++i], axHolder_.getLabelColor());
    updateFont((JButton)comps_[++i], axHolder_.getLabelFont());
    ((JTextField)comps_[++i]).setText(axHolder_.getLabelFormat());
    ((JTextField)comps_[++i]).setText(format(axHolder_.getLabelHeightP()));
    ((JTextField)comps_[++i]).setText(format(axHolder_.getLabelInterval()));
    if(axHolder_.isLabelPositionAuto()) {
      item = 0;
    } else {
      switch(axHolder_.getLabelPosition()) {
        default:
          break;
        case Axis.NEGATIVE_SIDE:
          item = 1;
          break;
        case Axis.POSITIVE_SIDE:
          item = 2;
          break;
        case Axis.NO_LABEL:
          item = 3;
          break;
      }
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JTextField)comps_[++i]).setText(format(axHolder_.getLabelSignificantDigits()));
    ((JTextField)comps_[++i]).setText(format(axHolder_.getLargeTicHeightP()));
    ((JCheckBox)comps_[++i]).setSelected(axHolder_.isLocationAtOrigin());
    ((JTextField)comps_[++i]).setText(axHolder_.getMajorFormat());
    ((JTextField)comps_[++i]).setText(format(axHolder_.getMajorInterval()));
    ((JTextField)comps_[++i]).setText(axHolder_.getMajorFormat());
    ((JTextField)comps_[++i]).setText(format(axHolder_.getMinorInterval()));
    ((JTextField)comps_[++i]).setText(format(axHolder_.getNumSmallTics()));
    ((JTextField)comps_[++i]).setText(format(axHolder_.getAxisOriginP(), true));
    ((JCheckBox)comps_[++i]).setSelected(axHolder_.isSelectable());
    ((JTextField)comps_[++i]).setText(format(axHolder_.getSmallTicHeightP()));
    if(axHolder_.isTicPositionAuto()) {
      item = 0;
    } else {
      switch(axHolder_.getTicPosition()) {
        default:
          break;
        case Axis.NEGATIVE_SIDE:
          item = 1;
          break;
        case Axis.POSITIVE_SIDE:
          item = 2;
          break;
        case Axis.BOTH_SIDES:
          item = 3;
          break;
      }
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    switch(axHolder_.getTimeAxisStyle()) {
      default:
      case TimeAxis.AUTO:
        item = 0;
        break;
      case TimeAxis.DAY_MONTH:
        item = 1;
        break;
      case TimeAxis.HOUR_DAY:
        item = 2;
        break;
      case TimeAxis.MINUTE_HOUR:
        item = 3;
        break;
      case TimeAxis.MONTH_YEAR:
        item = 4;
        break;
      case TimeAxis.YEAR_DECADE:
        item = 5;
        break;
    }
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JCheckBox)comps_[++i]).setSelected(axHolder_.isTitleAuto());
    updateSGLabel((JButton)comps_[++i], axHolder_.getTitle());
    Vector transItems = new Vector(5);
    transItems.add("LinearTransform");
    transItems.add("LogTransform");
    addOtherDataGroupTransforms(transItems);
    item = findTransformItem(transItems, axHolder_.getTransformType(),
                             axHolder_.getTransformGroup());
    ((JComboBox)comps_[++i]).setSelectedIndex(item);
    ((JTextField)comps_[++i]).setText(format(axHolder_.getUserRange(), false));
    ((JCheckBox)comps_[++i]).setSelected(axHolder_.isVisible());

    suppressEvent_ = false;
  }

  void create() {
    int i = -1;
    int item = -1;
    comps_[++i] = createCheckBox(axHolder_.isAutoRange(), pNames_[i], this);
    autoScale = i;
    comps_[++i] = createColor(axHolder_.getAxisColor(), pNames_[i], this);
    String[] axisPosition;
    if(axHolder_.getAxisOrientation() == DataGroup.X_DIR) {
      axisPosition = xAxisPosition;
    } else {
      axisPosition = yAxisPosition;
    }
    switch(axHolder_.getAxisPosition()) {
      default:
      case DataGroup.BOTTOM:
        item = 0;
        break;
      case DataGroup.TOP:
        item = 1;
        break;
      case DataGroup.LEFT:
        item = 0;
        break;
      case DataGroup.RIGHT:
        item = 1;
        break;
      case DataGroup.MANUAL:
        item = 2;
        break;
    }
    comps_[++i] = createComboBox(axisPosition, item, pNames_[i], this, true);
    comps_[++i] = createLabel(format(axHolder_.getAxisOriginP(), true));
    String[] axisItems = {"PlainAxis", "TimeAxis", "LogAxis"};
    switch(axHolder_.getAxisType()) {
      default:
      case DataGroup.PLAIN:
        item = 0;
        break;
      case DataGroup.TIME:
        item = 1;
        break;
      case DataGroup.LOG:
        item = 2;
        break;
    }
    comps_[++i] = createComboBox(axisItems, item, pNames_[i], this, true);
    comps_[++i] = createLabel(format((Rectangle2D.Double)axHolder_.getBoundsP(), true));  // bounds ?
    comps_[++i] = createColor(axHolder_.getLabelColor(), pNames_[i], this);
    comps_[++i] = createFont(axHolder_.getLabelFont(), pNames_[i], this);
    comps_[++i] = createTextField(axHolder_.getLabelFormat(), pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getLabelHeightP()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getLabelInterval()), pNames_[i], this, true);
    String[] labelPos = {"Auto", "Negative Side", "Positive Side", "No Label"};
    if(axHolder_.isLabelPositionAuto()) {
      item = 0;
    } else {
      switch(axHolder_.getLabelPosition()) {
        default:
          break;
        case Axis.NEGATIVE_SIDE:
          item = 1;
          break;
        case Axis.POSITIVE_SIDE:
          item = 2;
          break;
        case Axis.NO_LABEL:
          item = 3;
          break;
      }
    }
    comps_[++i] = createComboBox(labelPos, item, pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getLabelSignificantDigits()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getLargeTicHeightP()), pNames_[i], this, true);
    comps_[++i] = createCheckBox(axHolder_.isLocationAtOrigin(), pNames_[i], this);
    comps_[++i] = createTextField(axHolder_.getMajorFormat(), pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getMajorInterval()), pNames_[i], this, true);
    comps_[++i] = createTextField(axHolder_.getMinorFormat(), pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getMinorInterval()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getNumSmallTics()), pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getAxisOriginP(), true), pNames_[i], this, true);
    comps_[++i] = createCheckBox(axHolder_.isSelectable(), pNames_[i], this);
    comps_[++i] = createTextField(format(axHolder_.getSmallTicHeightP()), pNames_[i], this, true);
    String[] ticPos = {"Auto", "Negative Side", "Positive Side", "Both Sides"};
    if(axHolder_.isTicPositionAuto()) {
      item = 0;
    } else {
      switch(axHolder_.getTicPosition()) {
        default:
          break;
        case Axis.NEGATIVE_SIDE:
          item = 1;
          break;
        case Axis.POSITIVE_SIDE:
          item = 2;
          break;
        case Axis.BOTH_SIDES:
          item = 3;
          break;
      }
    }
    comps_[++i] = createComboBox(ticPos, item, pNames_[i], this, true);
    String[] timeStyle = {"Auto", "Day-Month", "Hour-Day",
      "Minute-Hour", "Month-Year", "Year-Decade"};
    switch(axHolder_.getTimeAxisStyle()) {
      default:
      case TimeAxis.AUTO:
        item = 0;
        break;
      case TimeAxis.DAY_MONTH:
        item = 1;
        break;
      case TimeAxis.HOUR_DAY:
        item = 2;
        break;
      case TimeAxis.MINUTE_HOUR:
        item = 3;
        break;
      case TimeAxis.MONTH_YEAR:
        item = 4;
        break;
      case TimeAxis.YEAR_DECADE:
        item = 5;
        break;
    }
    comps_[++i] = createComboBox(timeStyle, item, pNames_[i], this, true);
    comps_[++i] = createCheckBox(axHolder_.isTitleAuto(), pNames_[i], this);
    titleAuto = i;
    comps_[++i] = createSGLabel(axHolder_.getTitle(), pNames_[i], this);
    title = i;

    Vector transItems = new Vector(5);
    transItems.add("LinearTransform");
    transItems.add("LogTransform");
    addOtherDataGroupTransforms(transItems);
    item = findTransformItem(transItems, axHolder_.getTransformType(),
                             axHolder_.getTransformGroup());
    comps_[++i] = createComboBox(transItems, item, pNames_[i], this, true);
    comps_[++i] = createTextField(format(axHolder_.getUserRange(), false), pNames_[i], this, true);
    userRange = i;
    comps_[++i] = createCheckBox(axHolder_.isVisible(),  pNames_[i], this);
    setFieldsEnabled();
//
    int row = 0;
    for(i=0; i < comps_.length; i++) {
      if(expert_ || ! expertItem[i]) {
        addProperty(++row, pNames_[i], comps_[i], false);
      }
    }
    addProperty(row + 1, " ", new JLabel(" "), true);
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
    if(suppressEvent_) return;
    Object obj = e.getSource();
//    String str = null;
    String command = e.getActionCommand();
    processEvent(obj, command);
    setFieldsEnabled();
//    System.out.println(e.paramString() + ",rslt=" + str);
  }

  private void setFieldsEnabled() {
    ((JTextField)comps_[userRange]).setEnabled(!((JCheckBox)comps_[autoScale]).isSelected());
//    ((JButton)comps_[title]).setEnabled(!((JCheckBox)comps_[titleAuto]).isSelected());
  }

  private void processEvent(Object obj, String command) {
    if(Page.DEBUG) System.out.println("AxisHolderPropertyPanel.processEvent(" + obj + ", " + command + ")");
    int item = -1;
    String str = null;
    SoTRange range = null;
    if(command.equals("AutoScale")) {
      axHolder_.setAutoRange(((JCheckBox)obj).isSelected());
    } else if(command.equals("AxisColor")) {
      ColorDialog cd = new ColorDialog(getFrame(), "Select Axis Color", true);
      cd.setColor(axHolder_.getAxisColor());
      cd.setVisible(true);
      Color newcolor = cd.getColor();
      if(newcolor != null) axHolder_.setAxisColor(newcolor);
    } else if(command.equals("Axis Position")) {
      str = (String)((JComboBox)obj).getSelectedItem();
      item = -1;
      if(str.equals("Bottom")) {
        item = DataGroup.BOTTOM;
      } else if(str.equals("Top")) {
        item = DataGroup.TOP;
      } else if(str.equals("Left")) {
        item = DataGroup.LEFT;
      } else if(str.equals("Right")) {
        item = DataGroup.RIGHT;
      } else if(str.equals("Manual")) {
        item = DataGroup.MANUAL;
      }
      axHolder_.setAxisPosition(item);
    } else if(command.equals("Axis Location")) {
/** @todo SoTPoint axisLocation */
    } else if(command.equals("AxisType")) {
      str = (String)((JComboBox)obj).getSelectedItem();
      axHolder_.setAxisType(axis(str));
      if(str.equals("PlainAxis") || str.equals("TimeAxis")) {
        axHolder_.setTransformType(DataGroup.LINEAR);
      } else if(str.equals("LogAxis")) {
        axHolder_.setTransformType(DataGroup.LOG);
      }
      if(str.equals("TimeAxis") && !axHolder_.getUserRange().isTime()) {
        try {
          range = new SoTRange.Time(new GeoDate("2000-01-01 00:00", format_),
                                    new GeoDate("2001-01-01 00:00", format_),
                                    new GeoDate(172800000));
        } catch (IllegalTimeValue itv) {
          itv.printStackTrace();
        }
        axHolder_.setUserRange(range);
      }
    } else if(command.equals("Bounds")) {
      Rectangle2D bounds = parseBounds(((JTextField)obj).getText());
      if(bounds != null) axHolder_.setBoundsP(bounds);
    } else if(command.equals("Label Color")) {
      ColorDialog cd = new ColorDialog(getFrame(), "Select Label Color", true);
      cd.setColor(axHolder_.getAxisColor());
      cd.setVisible(true);
      Color newcolor = cd.getColor();
      if(newcolor != null) axHolder_.setLabelColor(newcolor);
    } else if(command.equals("Label Font")) {
      FontDialog fd = new FontDialog("Label Font");
      int result = fd.showDialog(axHolder_.getLabelFont());
      if(result == fd.OK_RESPONSE) {
        axHolder_.setLabelFont(fd.getFont());
      }
    } else if(command.equals("Label Format")) {
      axHolder_.setLabelFormat(((JTextField)obj).getText());
    } else if(command.equals("Label Height")) {
      axHolder_.setLabelHeightP(Double.parseDouble(((JTextField)obj).getText()));
    } else if(command.equals("Label Interval")) {
      axHolder_.setLabelInterval(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Label Position")) {
      str = (String)((JComboBox)obj).getSelectedItem();
      item = -1;
      if(str.equals("Auto")) {
        item = Axis.AUTO;
      } else if(str.equals("Negative Side")) {
        item = Axis.NEGATIVE_SIDE;
      } else if(str.equals("Positive Side")) {
        item = Axis.POSITIVE_SIDE;
      } else if(str.equals("No Label")) {
        item = Axis.NO_LABEL;
      }
      axHolder_.setLabelPosition(item);
    } else if(command.equals("Label SignificantDigits")) {
      axHolder_.setLabelSignificantDigits(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Large TicHeight")) {
      axHolder_.setLargeTicHeightP(Double.parseDouble(((JTextField)obj).getText()));
    } else if(command.equals("Location At Origin")) {
      axHolder_.setLocationAtOrigin(((JCheckBox)obj).isSelected());
    } else if(command.equals("Major Format")) {
      axHolder_.setMajorFormat(((JTextField)obj).getText());
    } else if(command.equals("Major Interval")) {
      axHolder_.setMajorInterval(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Minor Format")) {
      axHolder_.setMinorFormat(((JTextField)obj).getText());
    } else if(command.equals("Minor Interval")) {
      axHolder_.setMinorInterval(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("Num Small Tics")) {
      axHolder_.setNumSmallTics(Integer.parseInt(((JTextField)obj).getText()));
    } else if(command.equals("OriginP")) {
      axHolder_.setAxisOriginP(parsePoint2D(((JTextField)obj).getText()));
    } else if(command.equals("Selectable")) {
      axHolder_.setSelectable(((JCheckBox)obj).isSelected());
    } else if(command.equals("Small TicHeight")) {
      axHolder_.setSmallTicHeightP(Double.parseDouble(((JTextField)obj).getText()));
    } else if(command.equals("Tic Position")) {
      str = (String)((JComboBox)obj).getSelectedItem();
      item = -1;
      if(str.equals("Auto")) {
        item = Axis.AUTO;
      } else if(str.equals("Negative Side")) {
        item = Axis.NEGATIVE_SIDE;
      } else if(str.equals("Positive Side")) {
        item = Axis.POSITIVE_SIDE;
      } else if(str.equals("Both Sides")) {
        item = Axis.BOTH_SIDES;
      }
      axHolder_.setTicPosition(item);
    } else if(command.equals("Time AxisStyle")) {
        str = (String)((JComboBox)obj).getSelectedItem();
        item = -1;
        if(str.equals("Auto")) {
          item = TimeAxis.AUTO;
        } else if(str.equals("Day-Month")) {
          item = TimeAxis.DAY_MONTH;
        } else if(str.equals("Hour-Day")) {
          item = TimeAxis.HOUR_DAY;
        } else if(str.equals("Minute-Hour")) {
          item = TimeAxis.MINUTE_HOUR;
        } else if(str.equals("Month-Year")) {
          item = TimeAxis.MONTH_YEAR;
        } else if(str.equals("Year-Decade")) {
          item = TimeAxis.YEAR_DECADE;
        }
        axHolder_.setTimeAxisStyle(item);
    } else if(command.equals("Title Auto")) {
      axHolder_.setTitleAuto(((JCheckBox)obj).isSelected());
    } else if(command.equals("Title")) {
      SGLabelDialog sgd = new SGLabelDialog("Axis Title");
      sgd.setSGLabel(axHolder_.getTitle());
      sgd.setModal(true);
      sgd.setVisible(true);
      axHolder_.fireStateChanged();
    } else if(command.equals("Transform Type")) {
      str = (String)((JComboBox)obj).getSelectedItem();
      int trans = transform(str);
      if(trans == DataGroup.REFERENCE) {
        axHolder_.setTransformGroup(str.substring(5));
      }
      axHolder_.setTransformType(trans);
      if(trans == DataGroup.REFERENCE &&  circularReference(DataGroup.X_DIR, str.substring(5))) {
        JOptionPane.showMessageDialog(this, "Creates a circular reference in DataGroup transform",
                                      "Error Selecting Transform", JOptionPane.ERROR_MESSAGE);
        axHolder_.setTransformType(-1);
        axHolder_.setTransformGroup(null);
        ((JComboBox)obj).setSelectedIndex(-1);
        return;
      }
    } else if(command.equals("User Range")) {
      range = parseRange(((JTextField)obj).getText(), axHolder_.isTime());
      if(range != null) axHolder_.setUserRange(range);
    } else if(command.equals("Visible")) {
      axHolder_.setVisible(((JCheckBox)obj).isSelected());
    }
    update();
  }

  private int axis(String value) {
    if(value.equals("PlainAxis")) {
      return DataGroup.PLAIN;
    } else if(value.equals("TimeAxis")) {
      return DataGroup.TIME;
    } else if(value.equals("LogAxis")) {
      return DataGroup.LOG;
    }
    return -1;
  }

  private int transform(String value) {
    if(value.equals("LinearTransform")) {
      return DataGroup.LINEAR;
    } else if(value.equals("LogTransform")) {
      return DataGroup.LOG;
    } else {
      return DataGroup.REFERENCE;
    }
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

  public void stateChanged(ChangeEvent e) {
    update();
  }

  void addOtherDataGroupTransforms(Vector list) {
//    PanelHolder ph = axHolder_.getDataGroup().getPanelHolder();
    if(pHolder_.getDataGroupSize() <= 1) return;
    Iterator iter = pHolder_.dataGroupIterator();
    while(iter.hasNext()) {
      DataGroup dg = (DataGroup)iter.next();
      if(dg.getId().equals(axHolder_.getDataGroup().getId())) continue;
      list.add("Use: " + dg.getId());
    }
  }

  int findTransformItem(Vector transItems, int transType,
                        String transformGroup) {
    int item = -1;
    switch(transType) {
      default:
      case DataGroup.LINEAR:
        item = 0;
        break;
      case DataGroup.LOG:
        item = 1;
        break;
      case DataGroup.REFERENCE:
        if(transItems.size() >= 3) {
          for(int i = 2; i < transItems.size(); i++) {
            if(((String)transItems.get(i)).endsWith(transformGroup)) {
              return i;
            }
          }
        }
        break;
    }
    return item;
  }

  boolean circularReference(int dir, String datagroup) {
//    PanelHolder ph = axHolder_.getDataGroup().getPanelHolder();
    String dgLast = datagroup;
    boolean stillLooking = true;
    int tType = -1;
    String newDG = null;
    while(stillLooking) {
      DataGroup dg = pHolder_.findDataGroup(dgLast);
      if(dir == DataGroup.X_DIR) {
        tType = dg.getXAxisHolder().getTransformType();
      } else {
        tType = dg.getYAxisHolder().getTransformType();
      }
      if(tType != DataGroup.REFERENCE) return false;
      if(dir == DataGroup.X_DIR) {
        newDG = dg.getXAxisHolder().getTransformGroup();
      } else {
        newDG = dg.getYAxisHolder().getTransformGroup();
      }
      if(datagroup.equals(newDG)) return true;
      dgLast = newDG;
    }
    return false;
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