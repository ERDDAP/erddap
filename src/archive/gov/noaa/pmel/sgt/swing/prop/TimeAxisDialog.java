/*
 * $Id: TimeAxisDialog.java,v 1.9 2003/08/22 23:02:40 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt.swing.prop;

import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.TimeAxis;
import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.util.TimePoint;

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.IllegalTimeValue;
import gov.noaa.pmel.swing.ThreeDotsButton;

import java.util.Enumeration;
import javax.swing.*;
import java.awt.*;

/**
 * Edits a <code>TimeAxis</code>. This dialog does not
 * make a copy of the object so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new properties.
 *
 * <p> Example of <code>TimeAxisDialog</code> use:
 * <pre>
 * public void editTimeAxis(TimeAxis axis, JPane pane) {
 *   TimeAxisDialog tad = new TimeAxisDialog();
 *   tad.setTimeAxis(axis, pane);
 *   tad.setVisible(true);
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.9 $, $Date: 2003/08/22 23:02:40 $
 * @since 2.0
 */
public class TimeAxisDialog extends JDialog {
  private JPane pane_;
  private TimeAxis ta_;
  private Font labelFont_;
  private GeoDate startDate_;
  private GeoDate endDate_;
  private String[] styleNames_ = {"plain", "bold", "italic", "bold-italic"};
  //2011-12-15 Bob Simons changed space to 'T'
  private String dateFormat_ = "yyyy-MM-dd'T'HH:mm:ss";
  /**
   * Constructor.
   */
  public TimeAxisDialog(Frame parent) {
    super(parent);
    try {
      jbInit();
      pack();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  void jbInit() throws Exception {
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(new Dimension(457, 354));
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    jLabel1.setText("Line Color:");
    getContentPane().add(buttonPanel, "South");
    okButton.setText("OK");
    okButton.setActionCommand("OK");
    buttonPanel.add(okButton);
    applyButton.setText("Apply");
    applyButton.setActionCommand("Apply");
    buttonPanel.add(applyButton);
    cancelButton.setText("Cancel");
    cancelButton.setActionCommand("Cancel");
    buttonPanel.add(cancelButton);
    //$$ etchedBorder1.move(0,348);
    getContentPane().add(TabbedPane, "Center");
    labelPanel.setLayout(new GridBagLayout());
    TabbedPane.add(labelPanel, "labelPanel");
    labelPanel.setBounds(2,27,452,275);
    labelPanel.setVisible(false);
    JLabel4.setText("format");
    labelPanel.add(JLabel4, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    JLabel5.setText("interval");
    labelPanel.add(JLabel5, new GridBagConstraints(2,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    JLabel2.setText("Minor:");
    labelPanel.add(JLabel2, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    majorFormatTextField.setColumns(5);
    labelPanel.add(majorFormatTextField, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,5,5,5),0,0));
    majorIntervalTextField.setColumns(3);
    labelPanel.add(majorIntervalTextField, new GridBagConstraints(2,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel3.setText("Major:");
    labelPanel.add(JLabel3, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelPanel.add(minorFormatTextField, new GridBagConstraints(1,2,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,5,5,5),0,0));
    minorIntervalTextField.setColumns(3);
    labelPanel.add(minorIntervalTextField, new GridBagConstraints(2,2,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel11.setText("Color:");
    labelPanel.add(JLabel11, new GridBagConstraints(0,3,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(15,5,0,5),0,0));
    textColorPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(textColorPanel, new GridBagConstraints(1,3,2,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(15,5,0,5),0,0));
    JLabel15.setText("Font:");
    labelPanel.add(JLabel15, new GridBagConstraints(0,4,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    fontPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(fontPanel, new GridBagConstraints(1,4,2,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    fontLabel.setText("Dialog, 12, Bold");
    fontPanel.add(fontLabel);
    fontLabel.setForeground(java.awt.Color.black);
    fontEditor.setToolTipText("Edit font.");
    fontEditor.setActionCommand("...");
    fontPanel.add(fontEditor);
    JLabel16.setText("Height:");
    labelPanel.add(JLabel16, new GridBagConstraints(0,5,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(15,5,0,5),0,0));
    heightTextField.setColumns(10);
    labelPanel.add(heightTextField, new GridBagConstraints(1,5,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(15,5,5,5),0,0));
    JLabel1.setText("Position:");
    labelPanel.add(JLabel1, new GridBagConstraints(0,6,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    positionComboBox.setModel(positionCBModel);
    labelPanel.add(positionComboBox, new GridBagConstraints(1,6,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    rangePanel.setLayout(new GridBagLayout());
    TabbedPane.add(rangePanel, "rangePanel");
    rangePanel.setBounds(2,27,452,275);
    rangePanel.setVisible(false);
    userPanel.setBorder(userBorder);
    userPanel.setLayout(new GridBagLayout());
    rangePanel.add(userPanel, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,5,0),20,15));
    JLabel8.setText("Minimum:");
    userPanel.add(JLabel8, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    minUserTextField.setColumns(25);
    userPanel.add(minUserTextField, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    minDateEditor.setToolTipText("Edit minimum date.");
    minDateEditor.setActionCommand("...");
    userPanel.add(minDateEditor, new GridBagConstraints(2,0,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel9.setText("Maximum:");
    userPanel.add(JLabel9, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    maxUserTextField.setColumns(25);
    userPanel.add(maxUserTextField, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    maxDateEditor.setToolTipText("Edit max date.");
    maxDateEditor.setActionCommand("...");
    userPanel.add(maxDateEditor, new GridBagConstraints(2,1,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    physicalPanel.setBorder(physicalBorder);
    physicalPanel.setLayout(new GridBagLayout());
    rangePanel.add(physicalPanel, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,5,0),20,15));
    JLabel10.setText("Minimum:");
    physicalPanel.add(JLabel10, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    minPhysicalTextField.setColumns(20);
    physicalPanel.add(minPhysicalTextField, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel17.setText("Maximum:");
    physicalPanel.add(JLabel17, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    maxPhysicalTextField.setColumns(20);
    physicalPanel.add(maxPhysicalTextField, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    originLabel.setText("Y Origin:");
    physicalPanel.add(originLabel, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    originTextField.setColumns(20);
    physicalPanel.add(originTextField, new GridBagConstraints(1,2,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    ticsStylePanel.setLayout(new GridBagLayout());
    TabbedPane.add(ticsStylePanel, "ticsStylePanel");
    ticsStylePanel.setBounds(2,27,452,275);
    ticsStylePanel.setVisible(false);
    ticsPanel.setBorder(ticsBorder);
    ticsPanel.setLayout(new GridBagLayout());
    ticsStylePanel.add(ticsPanel,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 10, 10));
    JLabel18.setText("Large Tic Height:");
    ticsPanel.add(JLabel18,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    largeTicTextField.setColumns(15);
    ticsPanel.add(largeTicTextField,   new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel19.setText("Small Tic Height:");
    ticsPanel.add(JLabel19,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    smallTicTextField.setColumns(15);
    ticsPanel.add(smallTicTextField,   new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel20.setText("Number of Small Tics:");
    ticsPanel.add(JLabel20,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    numSmallTicsTextField.setColumns(5);
    ticsPanel.add(numSmallTicsTextField,   new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel21.setText("Tic Position:");
    ticsPanel.add(JLabel21,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    ticPositionComboBox.setModel(ticPositionCBModel);
    ticsPanel.add(ticPositionComboBox,  new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    ticsPanel.add(jLabel1,    new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    ticsPanel.add(lineColorPanel,   new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    stylePanel.setBorder(styleBorder);
    stylePanel.setLayout(new GridBagLayout());
    ticsStylePanel.add(stylePanel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 10, 10));
    JLabel22.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    JLabel22.setText("Time Axis Style:");
    stylePanel.add(JLabel22, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),34,0));
    axisStyleComboBox.setModel(styleCBModel);
    axisStyleComboBox.setEnabled(false);
    stylePanel.add(axisStyleComboBox,  new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel7.setText("Visible:");
    stylePanel.add(JLabel7, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    axislVisibleCheckBox.setSelected(true);
    stylePanel.add(axislVisibleCheckBox, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
    JLabel6.setText("Selectable:");
    stylePanel.add(JLabel6,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    axisSelectableCheckBox.setSelected(true);
    stylePanel.add(axisSelectableCheckBox,  new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    attachPanel.setLayout(new GridBagLayout());
    TabbedPane.add(attachPanel, "attachPanel");
    attachPanel.setBounds(2,27,452,275);
    attachPanel.setVisible(false);
    JLabel23.setText("Attach Transform to Axis:");
    attachPanel.add(JLabel23,new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    attachPanel.add(transformCheckBox,new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    axisLabel.setText("Attach X Axis to Axis:");
    attachPanel.add(axisLabel,new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    attachPanel.add(axisCheckBox,new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));

    TabbedPane.setTitleAt(0,"Label");
    TabbedPane.setTitleAt(1,"Range");
    TabbedPane.setTitleAt(2,"Tics/Style");
    TabbedPane.setTitleAt(3,"Attach");

    {
      String[] horizString = new String[3];
      horizString[0] = "LEFT";
      horizString[1] = "CENTER";
      horizString[2] = "RIGHT";
      for(int i=0; i < horizString.length; i++) {
         horizCBModel.addElement(horizString[i]);
      }
    }
    {
      String[] tempString = new String[3];
      tempString[0] = "TOP";
      tempString[1] = "MIDDLE";
      tempString[2] = "BOTTOM";
      for(int i=0; i < tempString.length; i++) {
        vertCBModel.addElement(tempString[i]);
      }
    }
    {
      String[] tempString = new String[3];
      tempString[0] = "POSITIVE_SIDE";
      tempString[1] = "NEGATIVE_SIDE";
      tempString[2] = "NO_LABEL";
      for(int i=0; i < tempString.length; i++) {
  positionCBModel.addElement(tempString[i]);
      }
    }
   {
      String[] tempString = new String[3];
      tempString[0] = "POSITIVE_SIDE";
      tempString[1] = "NEGATIVE_SIDE";
      tempString[2] = "BOTH_SIDES";
      for(int i=0; i < tempString.length; i++) {
        ticPositionCBModel.addElement(tempString[i]);
      }
    }
    {
      String[] tempString = new String[6];
      tempString[0] = "Auto";
      tempString[1] = "Year/Decade";
      tempString[2] = "Month/Year";
      tempString[3] = "Day/Month";
      tempString[4] = "Hour/Day";
      tempString[5] = "Minute/Hour";
      for(int i=0; i < tempString.length; i++) {
        styleCBModel.addElement(tempString[i]);
      }
    }

    setTitle("TimeAxis");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    fontEditor.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);
    minDateEditor.addActionListener(lSymAction);
    maxDateEditor.addActionListener(lSymAction);
    minUserTextField.addActionListener(lSymAction);
    maxUserTextField.addActionListener(lSymAction);

  }
  /** Used internally */
  public void addNotify() {
    // Record the size of the window prior to calling parents addNotify.
    Dimension d = getSize();

    super.addNotify();

    if (fComponentsAdjusted)
      return;

    // Adjust components according to the insets
    Insets ins = getInsets();
    setSize(ins.left + ins.right + d.width, ins.top + ins.bottom + d.height);
    Component components[] = getContentPane().getComponents();
    for (int i = 0; i < components.length; i++) {
      Point p = components[i].getLocation();
      p.translate(ins.left, ins.top);
      components[i].setLocation(p);
    }
    fComponentsAdjusted = true;
  }

  // Used for addNotify check.
  boolean fComponentsAdjusted = false;
  /**
   * Constructor.
   */
  public TimeAxisDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor
   */
  public TimeAxisDialog() {
    this((Frame)null);
  }
  /**
   * Make the dialog visible
   */
  public void setVisible(boolean b) {
    if(b) {
      setLocation(50, 50);
    }
    super.setVisible(b);
  }

  class SymWindow extends java.awt.event.WindowAdapter {
    public void windowClosing(java.awt.event.WindowEvent event) {
      Object object = event.getSource();
      if (object == TimeAxisDialog.this)
        FontDialog_WindowClosing(event);
    }
  }

  void FontDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  //{{DECLARE_CONTROLS
  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JTabbedPane TabbedPane = new javax.swing.JTabbedPane();
  javax.swing.JPanel labelPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel4 = new javax.swing.JLabel();
  javax.swing.JLabel JLabel5 = new javax.swing.JLabel();
  javax.swing.JLabel JLabel2 = new javax.swing.JLabel();
  javax.swing.JTextField majorFormatTextField = new javax.swing.JTextField();
  javax.swing.JTextField majorIntervalTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel3 = new javax.swing.JLabel();
  javax.swing.JTextField minorFormatTextField = new javax.swing.JTextField();
  javax.swing.JTextField minorIntervalTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel11 = new javax.swing.JLabel();
  ColorEntryPanel textColorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel15 = new javax.swing.JLabel();
  javax.swing.JPanel fontPanel = new javax.swing.JPanel();
  javax.swing.JLabel fontLabel = new javax.swing.JLabel();
  ThreeDotsButton fontEditor = new ThreeDotsButton();
  javax.swing.JLabel JLabel16 = new javax.swing.JLabel();
  javax.swing.JTextField heightTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel1 = new javax.swing.JLabel();
  javax.swing.JComboBox positionComboBox = new javax.swing.JComboBox();
  javax.swing.JPanel rangePanel = new javax.swing.JPanel();
  javax.swing.JPanel userPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel8 = new javax.swing.JLabel();
  javax.swing.JTextField minUserTextField = new javax.swing.JTextField();
  ThreeDotsButton minDateEditor = new ThreeDotsButton();
  javax.swing.JLabel JLabel9 = new javax.swing.JLabel();
  javax.swing.JTextField maxUserTextField = new javax.swing.JTextField();
  ThreeDotsButton maxDateEditor = new ThreeDotsButton();
  javax.swing.JPanel physicalPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel10 = new javax.swing.JLabel();
  javax.swing.JTextField minPhysicalTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel17 = new javax.swing.JLabel();
  javax.swing.JTextField maxPhysicalTextField = new javax.swing.JTextField();
  javax.swing.JLabel originLabel = new javax.swing.JLabel();
  javax.swing.JTextField originTextField = new javax.swing.JTextField();
  javax.swing.JPanel ticsStylePanel = new javax.swing.JPanel();
  javax.swing.JPanel ticsPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel18 = new javax.swing.JLabel();
  javax.swing.JTextField largeTicTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel19 = new javax.swing.JLabel();
  javax.swing.JTextField smallTicTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel20 = new javax.swing.JLabel();
  javax.swing.JTextField numSmallTicsTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel21 = new javax.swing.JLabel();
  javax.swing.JComboBox ticPositionComboBox = new javax.swing.JComboBox();
  javax.swing.JPanel stylePanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel22 = new javax.swing.JLabel();
  javax.swing.JComboBox axisStyleComboBox = new javax.swing.JComboBox();
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JCheckBox axislVisibleCheckBox = new javax.swing.JCheckBox();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JCheckBox axisSelectableCheckBox = new javax.swing.JCheckBox();
  javax.swing.JPanel attachPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel23 = new javax.swing.JLabel();
  javax.swing.JCheckBox transformCheckBox = new javax.swing.JCheckBox();
  javax.swing.JLabel axisLabel = new javax.swing.JLabel();
  javax.swing.JCheckBox axisCheckBox = new javax.swing.JCheckBox();
  DefaultComboBoxModel horizCBModel = new DefaultComboBoxModel();
  DefaultComboBoxModel vertCBModel = new DefaultComboBoxModel();
  DefaultComboBoxModel positionCBModel = new DefaultComboBoxModel();
  javax.swing.border.TitledBorder userBorder = new javax.swing.border.TitledBorder("User Range");
  javax.swing.border.TitledBorder physicalBorder = new javax.swing.border.TitledBorder("Physical Range");
  javax.swing.border.TitledBorder ticsBorder = new javax.swing.border.TitledBorder("Tics");
  javax.swing.border.TitledBorder styleBorder = new javax.swing.border.TitledBorder("Axis Style");
  DefaultComboBoxModel ticPositionCBModel = new DefaultComboBoxModel();
  DefaultComboBoxModel styleCBModel = new DefaultComboBoxModel();
  private JLabel jLabel1 = new JLabel();
  private ColorEntryPanel lineColorPanel = new ColorEntryPanel();
  //}}


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
        cancelButton_actionPerformed(event);
      else if (object == okButton)
        okButton_actionPerformed(event);
      else if (object == fontEditor)
        fontEditor_actionPerformed(event);
      else if (object == applyButton)
        applyButton_actionPerformed(event);
      else if (object == minDateEditor)
        minDateEditor_actionPerformed(event);
      else if (object == maxDateEditor)
        maxDateEditor_actionPerformed(event);
      else if (object == minUserTextField)
        minUserTextField_actionPerformed(event);
      else if (object == maxUserTextField)
        maxUserTextField_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateTimeAxis();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateTimeAxis();
  }
  /**
   * Test entry point
   */
  public static void main(String[] args) {
    TimeAxisDialog la = new TimeAxisDialog();
    la.setFont(null);
    la.setTitle("Test TimeAxis Dialog");
    la.setVisible(true);
  }
  /**
   * Set the <code>TimeAxis</code> to be edited and the
   * <code>JPane</code>
   */
  public void setTimeAxis(TimeAxis ta, JPane pane) {
    setJPane(pane);
    setTimeAxis(ta);
  }
  /**
   * Set the <code>TimeAxis</code> to be edited
   */
  public void setTimeAxis(TimeAxis ta) {
    ta_ = ta;
    setTimeAxis();
  }
  /**
   * Get the edited <code>TimeAxis</code>
   */
  public TimeAxis getTimeAxis() {
    return ta_;
  }
  /**
   * Set the parent <code>JPane</code>.
   */
  public void setJPane(JPane pane) {
    pane_ = pane;
  }
  /**
   * Get the parent <code>JPane</code>.
   */
  public JPane getJPane() {
    return pane_;
  }

  private void setTimeAxis() {
    //
    // time axis ID
    //
    setTitle("TimeAxis - " + ta_.getId());
    //
    // label
    //
    majorFormatTextField.setText(ta_.getMajorLabelFormat());
    majorIntervalTextField.setText(Integer.toString(ta_.getMajorLabelInterval()));
    minorFormatTextField.setText(ta_.getMinorLabelFormat());
    minorIntervalTextField.setText(Integer.toString(ta_.getMinorLabelInterval()));

    Color col = ta_.getLabelColor();
    if(col == null) col = pane_.getComponent().getForeground();
    textColorPanel.setColor(col);

    labelFont_ = ta_.getLabelFont();
    if(labelFont_ == null) labelFont_ = pane_.getComponent().getFont();
    fontLabel.setText(fontString(labelFont_));

    heightTextField.setText(String.valueOf(ta_.getLabelHeightP()));

    positionComboBox.setSelectedIndex(ta_.getLabelPosition());
    //
    // range
    //
    TimeRange trange = ta_.getTimeRangeU();
    startDate_ = trange.start;
    endDate_ = trange.end;
    minUserTextField.setText(startDate_.toString());
    maxUserTextField.setText(endDate_.toString());
    Range2D range = ta_.getRangeP();
    minPhysicalTextField.setText(String.valueOf(range.start));
    maxPhysicalTextField.setText(String.valueOf(range.end));

    TimePoint pt = ta_.getLocationU();

    if(ta_.getOrientation() == Axis.HORIZONTAL) {
      originLabel.setText("Y Origin:");
      originTextField.setText(String.valueOf(pt.x));
    } else {
      originLabel.setText("X Origin:");
      originTextField.setText(String.valueOf(pt.x));
    }
    //
    // tics
    //
    largeTicTextField.setText(String.valueOf(ta_.getLargeTicHeightP()));
    smallTicTextField.setText(String.valueOf(ta_.getSmallTicHeightP()));
    numSmallTicsTextField.setText(String.valueOf(ta_.getNumberSmallTics()));
    ticPositionComboBox.setSelectedIndex(ta_.getTicPosition());
    col = ta_.getLineColor();
    if(col == null) col = pane_.getComponent().getForeground();
    lineColorPanel.setColor(col);
    //
    // axis style
    //
    axisStyleComboBox.setSelectedIndex(ta_.getStyle());
    axislVisibleCheckBox.setSelected(ta_.isVisible());
    axisSelectableCheckBox.setSelected(ta_.isSelectable());
    //
    // attachments
    //
    boolean test = ta_.getNumberRegisteredTransforms() > 0;
    transformCheckBox.setSelected(test);

    if(ta_.getOrientation() == Axis.HORIZONTAL) {
      test = ta_.getGraph().getNumberXAxis() >= 2;
      axisLabel.setEnabled(test);
      axisCheckBox.setEnabled(test);
      axisLabel.setText("Attach X Axis to Axis:");
      test = ta_.getNumberRegisteredAxes() > 0;
      axisCheckBox.setSelected(test);
    } else {
      test = ta_.getGraph().getNumberYAxis() >= 2;
      axisLabel.setEnabled(test);
      axisCheckBox.setEnabled(test);
      axisLabel.setText("Attach Y Axis to Axis:");
      test = ta_.getNumberRegisteredAxes() > 0;
      axisCheckBox.setSelected(test);
    }

  }

  private void updateTimeAxis() {
    pane_.setBatch(true, "TimeAxisDialog");
    //
    // label
    //
    ta_.setMajorLabelFormat(majorFormatTextField.getText());
    ta_.setMajorLabelInterval(Integer.parseInt(majorIntervalTextField.getText()));
    ta_.setMinorLabelFormat(minorFormatTextField.getText());
    ta_.setMinorLabelInterval(Integer.parseInt(minorIntervalTextField.getText()));

    ta_.setLabelColor(textColorPanel.getColor());
    if(labelFont_ != null) ta_.setLabelFont(labelFont_);

    ta_.setLabelHeightP(Double.valueOf(heightTextField.getText()).doubleValue());
    ta_.setLabelPosition(positionComboBox.getSelectedIndex());
    //
    // range
    //
    ta_.setRangeU(new TimeRange(startDate_, endDate_));
    double min = Double.valueOf(minPhysicalTextField.getText()).doubleValue();
    double max = Double.valueOf(maxPhysicalTextField.getText()).doubleValue();
    ta_.setRangeP(new Range2D(min, max));
    TimePoint pt = ta_.getLocationU();
    pt.x = Double.valueOf(originTextField.getText()).doubleValue();
    ta_.setLocationU(pt);
    //
    // tics
    //
    ta_.setLargeTicHeightP(Double.valueOf(largeTicTextField.getText()).doubleValue());
    ta_.setSmallTicHeightP(Double.valueOf(smallTicTextField.getText()).doubleValue());
    ta_.setNumberSmallTics(Integer.parseInt(numSmallTicsTextField.getText()));
    ta_.setTicPosition(ticPositionComboBox.getSelectedIndex());
    ta_.setLineColor(lineColorPanel.getColor());
    //
    // axis style
    //
    ta_.setVisible(axislVisibleCheckBox.isSelected());
    ta_.setSelectable(axisSelectableCheckBox.isSelected());
    //
    // attach
    //
    boolean test;
    if(transformCheckBox.isSelected() && (ta_.getNumberRegisteredTransforms() < 1)) {
      if(ta_.getOrientation() == Axis.HORIZONTAL) {
        ta_.register(ta_.getGraph().getXTransform());
      } else {
        ta_.register(ta_.getGraph().getYTransform());
      }
    } else {
      if(ta_.getNumberRegisteredTransforms() > 0) ta_.clearAllRegisteredTransforms();
    }
    if(ta_.getOrientation() == Axis.HORIZONTAL) {
      test = (ta_.getGraph().getNumberXAxis() >= 2) &&
        (ta_.getNumberRegisteredAxes() < 1);
      if(axisCheckBox.isSelected() && test) {
        Axis ax;
        for(Enumeration it = ta_.getGraph().xAxisElements();
            it.hasMoreElements();) {
          ax = (Axis)it.nextElement();
          if(ax.getId() != ta_.getId()) ta_.register(ax);
        }
      } else {
        if(ta_.getNumberRegisteredAxes() > 0) ta_.clearAllRegisteredAxes();
      }
    } else {   // vertical axis
      test = (ta_.getGraph().getNumberYAxis() >= 2) &&
        (ta_.getNumberRegisteredAxes() < 1);
      if(axisCheckBox.isSelected() && test) {
        Axis ax;
        for(Enumeration it = ta_.getGraph().yAxisElements();
            it.hasMoreElements();) {
          ax = (Axis)it.nextElement();
          if(ax.getId() != ta_.getId()) ta_.register(ax);
        }
      } else  {
        if(ta_.getNumberRegisteredAxes() > 0) ta_.clearAllRegisteredAxes();
      }
    }

    pane_.setBatch(false, "TimeAxisDialog");
  }

  void fontEditor_actionPerformed(java.awt.event.ActionEvent event) {
    FontDialog fd = new FontDialog();
    int result = fd.showDialog(labelFont_);
    if(result == FontDialog.OK_RESPONSE) {
      labelFont_ = fd.getFont();
      fontLabel.setText(fontString(labelFont_));
      fontLabel.setFont(labelFont_);
    }
  }

  String fontString(Font font) {
    int style = (font.isBold()?1:0) + (font.isItalic()?2:0);
    return font.getName() + " " + styleNames_[style];
  }


  void minDateEditor_actionPerformed(java.awt.event.ActionEvent event) {
    GeoDateDialog gd = new GeoDateDialog();
    Point loc = minDateEditor.getLocationOnScreen();
    int result = gd.showDialog(startDate_, loc.x, loc.y);
    if(result == GeoDateDialog.OK_RESPONSE) {
      startDate_ = gd.getGeoDate();
      minUserTextField.setText(startDate_.toString());
    }
  }

  void maxDateEditor_actionPerformed(java.awt.event.ActionEvent  event) {
    GeoDateDialog gd = new GeoDateDialog();
    Point loc = maxDateEditor.getLocationOnScreen();
    int result = gd.showDialog(endDate_, loc.x, loc.y);
    if(result == GeoDateDialog.OK_RESPONSE) {
      endDate_ = gd.getGeoDate();
      maxUserTextField.setText(endDate_.toString());
    }
  }

  void minUserTextField_actionPerformed(java.awt.event.ActionEvent event) {
    try {
      startDate_ = new GeoDate(minUserTextField.getText(), dateFormat_);
    } catch (IllegalTimeValue e) {
      minUserTextField.setText(startDate_.toString());
    }
  }

  void maxUserTextField_actionPerformed(java.awt.event.ActionEvent event) {
    try {
      endDate_ = new GeoDate(maxUserTextField.getText(), dateFormat_);
    } catch (IllegalTimeValue e) {
      maxUserTextField.setText(endDate_.toString());
    }
  }
}
