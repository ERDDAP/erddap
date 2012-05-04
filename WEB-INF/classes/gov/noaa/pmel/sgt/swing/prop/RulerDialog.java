/*
 * $Id: RulerDialog.java,v 1.2 2003/08/22 23:02:39 dwd Exp $
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

import gov.noaa.pmel.sgt.*;
import gov.noaa.pmel.util.*;
import gov.noaa.pmel.swing.ThreeDotsButton;
import java.awt.*;
import javax.swing.*;

/**
 * Edits a <code>Ruler</code>. This dialog does not
 * make a copy of the object so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new properties.
 *
 * <p> Example of <code>RulerDialog</code> use:
 * <pre>
 * public void editRuler(Ruler axis, JPane pane) {
 *   RulerDialog sad = new RulerDialog();
 *   sad.setRuler(axis, pane);
 *   sad.setVisible(true);
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:39 $
 * @since 3.0
 */
public class RulerDialog extends JDialog {
  private JPane pane_;
  private Ruler scale_;
  private Font labelFont_;
  private String[] styleNames_ = {"plain", "bold", "italic", "bold-italic"};
  /**
   * Constructor.
   */
  public RulerDialog(Frame parent) {
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
    setSize(new Dimension(531, 358));
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    jLabel1.setText("Line Color:");
    physicalBorder.setTitle("Physical Bounds");
    jLabel5.setText("Height:");
    heightPhysicalTextField.setColumns(10);
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
    JLabel3.setText("Interval:");
    labelPanel.add(JLabel3, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    intervalTextField.setColumns(5);
    labelPanel.add(intervalTextField, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel4.setText("Sig Digits:");
    labelPanel.add(JLabel4, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    sigDigitsTextField.setColumns(5);
    labelPanel.add(sigDigitsTextField, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel5.setText("Format:");
    labelPanel.add(JLabel5, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    formatTextField.setColumns(10);
    labelPanel.add(formatTextField, new GridBagConstraints(1,2,1,1,0.0,0.0,
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
    JLabel9.setText("Maximum:");
    userPanel.add(JLabel9, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    maxUserTextField.setColumns(25);
    userPanel.add(maxUserTextField, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel2.setText("Delta:");
    userPanel.add(JLabel2, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    incUserTextField.setColumns(25);
    userPanel.add(incUserTextField, new GridBagConstraints(1,2,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    physicalPanel.setBorder(physicalBorder);
    physicalPanel.setLayout(new GridBagLayout());
    rangePanel.add(physicalPanel, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,5,0),20,15));
    JLabel10.setText("X:");
    physicalPanel.add(JLabel10, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    xPhysicalTextField.setColumns(10);
    physicalPanel.add(xPhysicalTextField,    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel17.setText("Y:");
    physicalPanel.add(JLabel17,  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    yPhysicalTextField.setColumns(10);
    physicalPanel.add(yPhysicalTextField,    new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    originLabel.setText("Width:");
    physicalPanel.add(originLabel, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    widthPhysicalTextField.setColumns(10);
    physicalPanel.add(widthPhysicalTextField,   new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    physicalPanel.add(jLabel5,    new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    physicalPanel.add(heightPhysicalTextField,     new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    ticsStylePanel.setLayout(new GridBagLayout());
    TabbedPane.add(ticsStylePanel, "ticsStylePanel");
    ticsStylePanel.setBounds(2,27,452,275);
    ticsStylePanel.setVisible(false);
    ticsPanel.setBorder(ticsBorder);
    ticsPanel.setLayout(new GridBagLayout());
    ticsStylePanel.add(ticsPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 10, 10));
    JLabel18.setText("Large Tic Height:");
    ticsPanel.add(JLabel18,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    largeTicTextField.setColumns(15);
    ticsPanel.add(largeTicTextField,  new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel19.setText("Small Tic Height:");
    ticsPanel.add(JLabel19,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    smallTicTextField.setColumns(15);
    ticsPanel.add(smallTicTextField,  new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel20.setText("Number of Small Tics:");
    ticsPanel.add(JLabel20,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    numSmallTicsTextField.setColumns(5);
    ticsPanel.add(numSmallTicsTextField,  new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel21.setText("Tic Position:");
    ticsPanel.add(JLabel21,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    ticPositionComboBox.setModel(ticPositionCBModel);
    ticsPanel.add(ticPositionComboBox,  new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    ticsPanel.add(lineColorPanel,   new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    stylePanel.setBorder(styleBorder);
    stylePanel.setLayout(new GridBagLayout());
    ticsStylePanel.add(stylePanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 20, 10));
    JLabel7.setText("Visible:");
    stylePanel.add(JLabel7, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    axislVisibleCheckBox.setSelected(true);
    stylePanel.add(axislVisibleCheckBox, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
    JLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    JLabel6.setText("Selectable:");
    stylePanel.add(JLabel6,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 61, 0));
    axisSelectableCheckBox.setSelected(true);
    stylePanel.add(axisSelectableCheckBox,   new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    ticsPanel.add(jLabel1,     new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    TabbedPane.setSelectedComponent(labelPanel);
    TabbedPane.setSelectedIndex(0);
    TabbedPane.setTitleAt(0,"Label");
    TabbedPane.setTitleAt(1,"Range");
    TabbedPane.setTitleAt(2,"Tics/Style");
    TabbedPane.setTitleAt(3,"Attach");
    {
      String[] tempString = new String[3];
      tempString[0] = "LEFT";
      tempString[1] = "CENTER";
      tempString[2] = "RIGHT";
      for(int i=0; i < tempString.length; i++) {
        horizCBModel.addElement(tempString[i]);
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
    positionComboBox.setSelectedIndex(1);
    ticPositionComboBox.setSelectedIndex(1);
    setTitle("TimeAxis");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    fontEditor.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);
    minUserTextField.addActionListener(lSymAction);
    maxUserTextField.addActionListener(lSymAction);
    widthPhysicalTextField.addActionListener(lSymAction);

//    Insets pup = new Insets(0, 0, 0, 0);
//    fontEditor.setMargin(pup);
  }
  /** Used internally. */
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
  public RulerDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public RulerDialog() {
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
      if (object == RulerDialog.this)
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
  javax.swing.JLabel JLabel3 = new javax.swing.JLabel();
  javax.swing.JTextField intervalTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel4 = new javax.swing.JLabel();
  javax.swing.JTextField sigDigitsTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel5 = new javax.swing.JLabel();
  javax.swing.JTextField formatTextField = new javax.swing.JTextField();
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
  javax.swing.JLabel JLabel9 = new javax.swing.JLabel();
  javax.swing.JTextField maxUserTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel2 = new javax.swing.JLabel();
  javax.swing.JTextField incUserTextField = new javax.swing.JTextField();
  javax.swing.JPanel physicalPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel10 = new javax.swing.JLabel();
  javax.swing.JTextField xPhysicalTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel17 = new javax.swing.JLabel();
  javax.swing.JTextField yPhysicalTextField = new javax.swing.JTextField();
  javax.swing.JLabel originLabel = new javax.swing.JLabel();
  javax.swing.JTextField widthPhysicalTextField = new javax.swing.JTextField();
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
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JCheckBox axislVisibleCheckBox = new javax.swing.JCheckBox();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JCheckBox axisSelectableCheckBox = new javax.swing.JCheckBox();
  DefaultComboBoxModel horizCBModel = new DefaultComboBoxModel();
  DefaultComboBoxModel vertCBModel = new DefaultComboBoxModel();
  DefaultComboBoxModel positionCBModel = new DefaultComboBoxModel();
  javax.swing.border.TitledBorder userBorder = new javax.swing.border.TitledBorder("User Range");
  javax.swing.border.TitledBorder physicalBorder = new javax.swing.border.TitledBorder("Physical Range");
  javax.swing.border.TitledBorder ticsBorder = new javax.swing.border.TitledBorder("Tics");
  javax.swing.border.TitledBorder styleBorder = new javax.swing.border.TitledBorder("Axis Style");
  DefaultComboBoxModel ticPositionCBModel = new DefaultComboBoxModel();
  private ColorEntryPanel lineColorPanel = new ColorEntryPanel();
  private JLabel jLabel1 = new JLabel();
  private JLabel jLabel5 = new JLabel();
  private JTextField heightPhysicalTextField = new JTextField();

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
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateRuler();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateRuler();
  }
  /**
   * Test entry point
   */
  public static void main(String[] args) {
    RulerDialog la = new RulerDialog();
    la.setFont(null);
    la.setTitle("Test Ruler Dialog");
    la.setVisible(true);
  }
  /**
   * Set the <code>Ruler</code> to be edited and the
   * <code>JPane</code>
   */
  public void setRuler(Ruler scale, JPane pane) {
    setJPane(pane);
    setRuler(scale);
  }
  /**
   * Set the <code>Ruler</code> to be edited
   */
  public void setRuler(Ruler scale) {
    scale_ = scale;
    setRuler();
  }
  /**
   * Get the edited <code>Ruler</code>
   */
  public Ruler getRuler() {
    return scale_;
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

  private void setRuler() {
    //
    // time axis ID
    //
    setTitle("Ruler - " + scale_.getId());
    //
    // label
    //
    intervalTextField.setText(Integer.toString(scale_.getLabelInterval()));
    sigDigitsTextField.setText(Integer.toString(scale_.getSignificantDigits()));
    formatTextField.setText(scale_.getLabelFormat());

    Color col = scale_.getLabelColor();
    if(col == null) col = pane_.getComponent().getForeground();
    textColorPanel.setColor(col);

    labelFont_ = scale_.getLabelFont();
    if(labelFont_ == null) labelFont_ = pane_.getComponent().getFont();
    fontLabel.setText(fontString(labelFont_));

    heightTextField.setText(String.valueOf(scale_.getLabelHeightP()));
    //
    // range
    //
    Range2D urange = scale_.getRangeU();
    minUserTextField.setText(String.valueOf(urange.start));
    maxUserTextField.setText(String.valueOf(urange.end));
    incUserTextField.setText(String.valueOf(urange.delta));
    Rectangle2D.Double bounds = scale_.getBoundsP();
//    Range2D range = sa_.getRangeP();
    xPhysicalTextField.setText(String.valueOf(bounds.x));
    yPhysicalTextField.setText(String.valueOf(bounds.y));
    widthPhysicalTextField.setText(String.valueOf(bounds.width));
    heightPhysicalTextField.setText(String.valueOf(bounds.height));
//    Point2D.Double pt = sa_.getLocationU();
//    TimePoint tt = null;
//    double point = 0.0;

//    if(pt == null) {
//      originIsGeoDate_ = true;
//      tt = sa_.getTimeLocationU();
//      tOrigin_ = tt.t;
//    } else {
//      originIsGeoDate_ = false;
//      if(sa_.getOrientation() == Axis.HORIZONTAL) {
//        point = pt.y;
//      } else {
//        point = pt.x;
//      }
//    }

    if(scale_.getOrientation() == Axis.HORIZONTAL) {
      originLabel.setText("Y Origin:");
    } else {
      originLabel.setText("X Origin:");
    }
//    if(originIsGeoDate_) {
//      originDateEditor.setEnabled(true);
//      originTextField.setText(tOrigin_.toString());
//    } else {
//      originDateEditor.setEnabled(false);
//      originTextField.setText(String.valueOf(point));
//    }
    //
    // tics
    //
    largeTicTextField.setText(String.valueOf(scale_.getLargeTicHeightP()));
    smallTicTextField.setText(String.valueOf(scale_.getSmallTicHeightP()));
    numSmallTicsTextField.setText(String.valueOf(scale_.getNumberSmallTics()));
    ticPositionComboBox.setSelectedIndex(scale_.getTicPosition());

    Color lcol = scale_.getLineColor();
    if(lcol == null) col = pane_.getComponent().getForeground();
    lineColorPanel.setColor(col);

    //
    // axis style
    //
    axislVisibleCheckBox.setSelected(scale_.isVisible());
    axisSelectableCheckBox.setSelected(scale_.isSelectable());
    //
    // attachments
    //
//    boolean test = sa_.getNumberRegisteredTransforms() > 0;
//    transformCheckBox.setSelected(test);

//    if(sa_.getOrientation() == Axis.HORIZONTAL) {
////      test = sa_.getGraph().getNumberXAxis() >= 2;
//      axisLabel.setEnabled(test);
//      axisCheckBox.setEnabled(test);
//      axisLabel.setText("Attach X Axis to Axis:");
//      test = sa_.getNumberRegisteredAxes() > 0;
//      axisCheckBox.setSelected(test);
//    } else {
//      test = sa_.getGraph().getNumberYAxis() >= 2;
//      axisLabel.setEnabled(test);
//      axisCheckBox.setEnabled(test);
//      axisLabel.setText("Attach Y Axis to Axis:");
//      test = sa_.getNumberRegisteredAxes() > 0;
//      axisCheckBox.setSelected(test);
//    }

  }

  private void updateRuler() {
    pane_.setBatch(true, "RulerDialog");
    //
    // label
    //
    scale_.setLabelInterval(Integer.parseInt(intervalTextField.getText()));
    scale_.setSignificantDigits(Integer.parseInt(sigDigitsTextField.getText()));
    scale_.setLabelFormat(formatTextField.getText());

    scale_.setLabelColor(textColorPanel.getColor());
    if(labelFont_ != null) scale_.setLabelFont(labelFont_);

    scale_.setLabelHeightP(Double.valueOf(heightTextField.getText()).doubleValue());
    scale_.setLabelPosition(positionComboBox.getSelectedIndex());
    //
    // range
    //
    double min = Double.valueOf(minUserTextField.getText()).doubleValue();
    double max = Double.valueOf(maxUserTextField.getText()).doubleValue();
    double inc = Double.valueOf(incUserTextField.getText()).doubleValue();
    scale_.setRangeU(new Range2D(min, max, inc));
    double x = Double.valueOf(xPhysicalTextField.getText()).doubleValue();
    double y = Double.valueOf(yPhysicalTextField.getText()).doubleValue();
    double width = Double.valueOf(widthPhysicalTextField.getText()).doubleValue();
    double height = Double.valueOf(heightPhysicalTextField.getText()).doubleValue();
    scale_.setBoundsP(new Rectangle2D.Double(x, y, width, height));
//    sa_.setRangeP(new Range2D(min, max));
//    if(originIsGeoDate_) {
//      TimePoint pt = sa_.getTimeLocationU();
//      pt.t = tOrigin_;
//      sa_.setLocationU(pt);
//    } else {
//      Point2D.Double pt = sa_.getLocationU();
//      if(sa_.getOrientation() == Axis.HORIZONTAL) {
//        pt.y = Double.valueOf(originTextField.getText()).doubleValue();
//      } else {
//        pt.x = Double.valueOf(originTextField.getText()).doubleValue();
//      }
//      sa_.setLocationU(pt);
//    }
    //
    // tics
    //
    scale_.setLargeTicHeightP(Double.valueOf(largeTicTextField.getText()).doubleValue());
    scale_.setSmallTicHeightP(Double.valueOf(smallTicTextField.getText()).doubleValue());
    scale_.setNumberSmallTics(Integer.parseInt(numSmallTicsTextField.getText()));
    scale_.setTicPosition(ticPositionComboBox.getSelectedIndex());

    scale_.setLineColor(lineColorPanel.getColor());
    //
    // axis style
    //
    scale_.setVisible(axislVisibleCheckBox.isSelected());
    scale_.setSelectable(axisSelectableCheckBox.isSelected());
    //
    // attach
    //
//    boolean test;
/*    if(transformCheckBox.isSelected() && (sa_.getNumberRegisteredTransforms() < 1)) {
      if(sa_.getOrientation() == Axis.HORIZONTAL) {
        sa_.register(sa_.getGraph().getXTransform());
      } else {
        sa_.register(sa_.getGraph().getYTransform());
      }
    } else {
      if(sa_.getNumberRegisteredTransforms() > 0) sa_.clearAllRegisteredTransforms();
    }
    if(sa_.getOrientation() == Axis.HORIZONTAL) {
      test = (sa_.getGraph().getNumberXAxis() >= 2) &&
        (sa_.getNumberRegisteredAxes() < 1);
      if(axisCheckBox.isSelected() && test) {
        Axis ax;
        for(Enumeration it = sa_.getGraph().xAxisElements();
            it.hasMoreElements();) {
          ax = (Axis)it.nextElement();
          if(ax.getId() != sa_.getId()) sa_.register(ax);
        }
      } else {
        if(sa_.getNumberRegisteredAxes() > 0) sa_.clearAllRegisteredAxes();
      }
    } else {   // vertical axis
      test = (sa_.getGraph().getNumberYAxis() >= 2) &&
        (sa_.getNumberRegisteredAxes() < 1);
      if(axisCheckBox.isSelected() && test) {
        Axis ax;
        for(Enumeration it = sa_.getGraph().yAxisElements();
            it.hasMoreElements();) {
          ax = (Axis)it.nextElement();
          if(ax.getId() != sa_.getId()) sa_.register(ax);
        }
      } else  {
        if(sa_.getNumberRegisteredAxes() > 0) sa_.clearAllRegisteredAxes();
      }
    } */

    pane_.setBatch(false, "RulerDialog");
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


//  void widthPhysicalTextField_actionPerformed(java.awt.event.ActionEvent event) {
/*    if(originIsGeoDate_) {
      try {
        tOrigin_ = new GeoDate(originTextField.getText(), dateFormat_);
      } catch (IllegalTimeValue e) {
        originTextField.setText(tOrigin_.toString());
      }
    } */
//  }

//  void originDateEditor_actionPerformed(java.awt.event.ActionEvent event)       {
/*    GeoDateDialog gd = new GeoDateDialog();
    Point loc = originDateEditor.getLocationOnScreen();
    int result = gd.showDialog(tOrigin_, loc.x, loc.y);
    if(result == GeoDateDialog.OK_RESPONSE) {
      tOrigin_ = gd.getGeoDate();
      originTextField.setText(tOrigin_.toString());
    } */
//  }
}
