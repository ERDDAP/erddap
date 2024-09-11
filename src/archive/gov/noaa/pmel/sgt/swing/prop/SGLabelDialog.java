/*
 * $Id: SGLabelDialog.java,v 1.9 2003/08/22 23:02:39 dwd Exp $
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


import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Debug;
import gov.noaa.pmel.swing.ThreeDotsButton;
import gov.noaa.pmel.swing.MRJUtil;

import javax.swing.*;
import java.awt.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Edits a <code>SGLabel</code>. This dialog does not
 * make a copy of the object so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new properties.
 *
 * <p> Example of <code>SGLabelDialog</code> use:
 * <pre>
 * public void editSGLabel(SGLabel label, JPane pane) {
 *   SGLabelDialog sgld = new SGLabelDialog();
 *   sgld.setSGLabel(label, pane);
 *   sgld.setVisible(true);
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.9 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 */
public class SGLabelDialog extends JDialog implements PropertyChangeListener {
  private SGLabel label_;
  private JPane pane_;
  private Font labelFont_;
  private String[] styleNames_ = {"plain", "bold", "italic", "bold-italic"};
  private boolean ignoreEvent_ = false;
//  private ThreeDotsIcon dotsIcon_ = new ThreeDotsIcon(Color.black);
  /**
   * Constructor.
   */
  public SGLabelDialog(Frame parent) {
    super(parent);
    try {
      jbInit();
      pack();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  void jbInit() throws Exception {
    //{{INIT_CONTROLS
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(457, 320);
//    setSize(457,294);
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    fontEditor.setAlignmentY((float) 0.0);
//    fontEditor.setIcon(dotsIcon_);
    fontLabel.setAlignmentY((float) 0.0);
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
    //$$ etchedBorder1.move(0,300);
    getContentPane().add(TabbedPane, "Center");
    textPanel.setLayout(new GridBagLayout());
    TabbedPane.add(textPanel, "textPanel");
    textPanel.setBounds(2,27,452,225);
    textPanel.setVisible(false);
    JLabel9.setText("Text:");
    textPanel.add(JLabel9, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelTextField.setColumns(20);
    textPanel.add(labelTextField, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,5,5,5),0,0));
    JLabel11.setText("Color:");
    textPanel.add(JLabel11, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    textColorPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    textPanel.add(textColorPanel, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    JLabel15.setText("Font:");
    textPanel.add(JLabel15, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    fontPanel.setLayout(flowLayout1);
    textPanel.add(fontPanel,   new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    fontLabel.setText("Dialog, 12, Bold");
    fontPanel.add(fontLabel, null);
    fontLabel.setForeground(java.awt.Color.black);
    fontEditor.setToolTipText("Edit font.");
    fontEditor.setActionCommand("...");
    fontPanel.add(fontEditor, null);
    JLabel16.setText("HeightP:");
    textPanel.add(JLabel16, new GridBagConstraints(0,3,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    heightTextField.setColumns(10);
    textPanel.add(heightTextField, new GridBagConstraints(1,3,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel7.setText("Visible:");
    textPanel.add(JLabel7, new GridBagConstraints(0,4,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelVisibleCheckBox.setSelected(true);
    textPanel.add(labelVisibleCheckBox, new GridBagConstraints(1,4,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
    JLabel6.setText("Selectable:");
    textPanel.add(JLabel6, new GridBagConstraints(0,5,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelSelectableCheckBox.setSelected(true);
    textPanel.add(labelSelectableCheckBox, new GridBagConstraints(1,5,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
    locationPanel.setLayout(new GridBagLayout());
    TabbedPane.add(locationPanel, "locationPanel");
    locationPanel.setBounds(2,27,452,225);
    locationPanel.setVisible(false);
    positionPanel.setBorder(positionBorder);
    positionPanel.setLayout(new GridBagLayout());
    locationPanel.add(positionPanel, new GridBagConstraints(0,0,2,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,5,0),20,15));
    JLabel1.setText("X Position:");
    positionPanel.add(JLabel1, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    xPosTextField.setColumns(20);
    positionPanel.add(xPosTextField, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel2.setText("Y Position:");
    positionPanel.add(JLabel2, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    yPosTextField.setColumns(20);
    positionPanel.add(yPosTextField, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    alignPanel.setBorder(alignBorder);
    alignPanel.setLayout(new GridBagLayout());
    locationPanel.add(alignPanel, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.VERTICAL,new Insets(5,0,5,0),20,15));
    JLabel3.setText("Horizontal:");
    alignPanel.add(JLabel3,new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    horizComboBox.setModel(horizCBModel);
    alignPanel.add(horizComboBox,new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,5,5,5),0,0));
    JLabel4.setText("Vertical:");
    alignPanel.add(JLabel4,new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    vertComboBox.setModel(vertCBModel);
    alignPanel.add(vertComboBox,new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,5,5,5),0,0));
    orientPanel.setBorder(orientBorder);
    orientPanel.setLayout(new GridBagLayout());
    locationPanel.add(orientPanel, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.VERTICAL,new Insets(5,10,5,0),20,15));
    orientPanel.setBackground(new java.awt.Color(204,204,204));
    horizRadioButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    horizRadioButton.setText("Horizontal");
    horizRadioButton.setActionCommand("Horizontal");
    orientPanel.add(horizRadioButton, new GridBagConstraints(0,0,1,1,1.0,1.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,0),0,0));
    vertRadioButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    vertRadioButton.setText("Vertical");
    vertRadioButton.setActionCommand("Vertical");
    orientPanel.add(vertRadioButton,new GridBagConstraints(0,1,1,1,1.0,1.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,0),0,0));
    TabbedPane.setSelectedComponent(textPanel);
    TabbedPane.setSelectedIndex(0);
    TabbedPane.setTitleAt(0,"Text");
    TabbedPane.setTitleAt(1,"Position");
    {
      String[] tempString = new String[3];
      tempString[0] = "LEFT";
      tempString[1] = "CENTER";
      tempString[2] = "RIGHT";
      for(int i=0; i < tempString.length; i++) {
  horizCBModel.addElement(tempString[i]);
      }
      //      horizCBModel.setItems(tempString);
    }
    //$$ horizCBModel.move(72,300);
    {
      String[] tempString = new String[3];
      tempString[0] = "TOP";
      tempString[1] = "MIDDLE";
      tempString[2] = "BOTTOM";
      for(int i=0; i < tempString.length; i++) {
  vertCBModel.addElement(tempString[i]);
      }
      //      vertCBModel.setItems(tempString);
    }
    vertComboBox.setSelectedIndex(2);
    horizComboBox.setSelectedIndex(0);
    setTitle("SGLabel");
    //}}

    //{{REGISTER_LISTENERS
    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    fontEditor.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);
    //}}

    if(!MRJUtil.isAquaLookAndFeel()) {
      Insets pup = new Insets(0, 0, 0, 0);
      fontEditor.setMargin(pup);
    }

    ButtonGroup orientBG = new ButtonGroup();
    orientBG.add(horizRadioButton);
    orientBG.add(vertRadioButton);

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
  public SGLabelDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public SGLabelDialog() {
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
      if (object == SGLabelDialog.this)
        SGLabelDialog_WindowClosing(event);
    }
  }

  void SGLabelDialog_WindowClosing(java.awt.event.WindowEvent event) {
    finish();
    dispose();
  }

  //{{DECLARE_CONTROLS
  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JTabbedPane TabbedPane = new javax.swing.JTabbedPane();
  javax.swing.JPanel textPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel9 = new javax.swing.JLabel();
  javax.swing.JTextField labelTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel11 = new javax.swing.JLabel();
  ColorEntryPanel textColorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel15 = new javax.swing.JLabel();
  javax.swing.JPanel fontPanel = new javax.swing.JPanel();
  javax.swing.JLabel fontLabel = new javax.swing.JLabel();
  ThreeDotsButton fontEditor = new ThreeDotsButton();
  javax.swing.JLabel JLabel16 = new javax.swing.JLabel();
  javax.swing.JTextField heightTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelVisibleCheckBox = new javax.swing.JCheckBox();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelSelectableCheckBox = new javax.swing.JCheckBox();
  javax.swing.JPanel locationPanel = new javax.swing.JPanel();
  javax.swing.JPanel positionPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel1 = new javax.swing.JLabel();
  javax.swing.JTextField xPosTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel2 = new javax.swing.JLabel();
  javax.swing.JTextField yPosTextField = new javax.swing.JTextField();
  javax.swing.JPanel alignPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel3 = new javax.swing.JLabel();
  javax.swing.JComboBox horizComboBox = new javax.swing.JComboBox();
  javax.swing.JLabel JLabel4 = new javax.swing.JLabel();
  javax.swing.JComboBox vertComboBox = new javax.swing.JComboBox();
  javax.swing.JPanel orientPanel = new javax.swing.JPanel();
  javax.swing.JRadioButton horizRadioButton = new javax.swing.JRadioButton();
  javax.swing.JRadioButton vertRadioButton = new javax.swing.JRadioButton();
  javax.swing.border.TitledBorder alignBorder = new javax.swing.border.TitledBorder("Alignment");
  javax.swing.border.TitledBorder positionBorder = new javax.swing.border.TitledBorder("Position");
  DefaultComboBoxModel horizCBModel = new DefaultComboBoxModel();
  DefaultComboBoxModel vertCBModel = new DefaultComboBoxModel();
  javax.swing.border.TitledBorder orientBorder = new javax.swing.border.TitledBorder("Orientation");
  private FlowLayout flowLayout1 = new FlowLayout();
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
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    finish();
    this.setVisible(false);
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    finish();
    updateSGLabel();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateSGLabel();
  }
  /**
   * Test entry point
   */
  public static void main(String[] args) {
    SGLabelDialog la = new SGLabelDialog();
    la.setFont(null);
    la.setTitle("Test SGLabel Dialog");
    la.setVisible(true);
  }
  /**
   * Set the <code>SGLabel</code> to be edited and the
   * <code>JPane</code>.
   */
  public void setSGLabel(SGLabel label, JPane pane) {
    setJPane(pane);
    setSGLabel(label);
  }
  /**
   * Set the <code>SGLabel</code> to be edited.
   */
  public void setSGLabel(SGLabel label) {
    label_ = label;
    label_.addPropertyChangeListener(this);
    ignoreEvent_ = false;
    setSGLabel();
  }
  /**
   * Get the edited object
   */
  public SGLabel getSGLabel() {
    return label_;
  }
  /**
   * Set the parent <code>JPane</code>
   */
  public void setJPane(JPane pane) {
    pane_ = pane;
  }
  /**
   * Get the parent <code>JPane</code>
   */
  public JPane getJPane() {
    return pane_;
  }

  private void setSGLabel() {
    //
    // label Id
    //
    setTitle("SGLabel - " + label_.getId());
    //
    // text
    //
    labelTextField.setText(label_.getText());

    Color col = label_.getColor();
    if(col == null && pane_ != null) col = pane_.getComponent().getForeground();
    textColorPanel.setColor(col);

    labelFont_ = label_.getFont();
    if(labelFont_ == null && pane_ != null) labelFont_ = pane_.getComponent().getFont();
    fontLabel.setText(fontString(labelFont_));

    heightTextField.setText(String.valueOf(label_.getHeightP()));
    labelVisibleCheckBox.setSelected(label_.isVisible());
    labelSelectableCheckBox.setSelected(label_.isSelectable());
    //
    // location
    //
    Point2D.Double locp = label_.getLocationP();
    xPosTextField.setText(String.valueOf(locp.x));
    yPosTextField.setText(String.valueOf(locp.y));

    horizComboBox.setSelectedIndex(label_.getHAlign());
    vertComboBox.setSelectedIndex(label_.getVAlign());

    if(label_.getOrientation() == SGLabel.HORIZONTAL) {
      horizRadioButton.setSelected(true);
    } else {
      vertRadioButton.setSelected(true);
    }
  }

  private void updateSGLabel() {
    ignoreEvent_ = true;
    if(pane_ != null) pane_.setBatch(true, "SGLabelDialog");
    //
    // text
    //
    label_.setText(labelTextField.getText());

    label_.setColor(textColorPanel.getColor());

    if(labelFont_ != null) label_.setFont(labelFont_);

    label_.setHeightP(Double.valueOf(heightTextField.getText()).doubleValue());
    label_.setVisible(labelVisibleCheckBox.isSelected());
    label_.setSelectable(labelSelectableCheckBox.isSelected());
    //
    // Location
    //
    double x = Double.valueOf(xPosTextField.getText()).doubleValue();
    double y = Double.valueOf(yPosTextField.getText()).doubleValue();
    Point2D.Double locp = new Point2D.Double(x, y);
    label_.setLocationP(locp);

    label_.setHAlign(horizComboBox.getSelectedIndex());
    label_.setVAlign(vertComboBox.getSelectedIndex());

    boolean horiz = horizRadioButton.isSelected();
    if(horiz) {
      label_.setOrientation(SGLabel.HORIZONTAL);
    } else {
      label_.setOrientation(SGLabel.VERTICAL);
    }

    if(pane_ != null) pane_.setBatch(false, "SGLabelDialog");
    ignoreEvent_ = false;
  }

  void fontEditor_actionPerformed(java.awt.event.ActionEvent event)
  {
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

  private void finish() {
    label_.removePropertyChangeListener(this);
  }
  /**
   * Used internally to track changes to a <code>SGLabel</code>
   */
  public void propertyChange(PropertyChangeEvent event) {
    if(ignoreEvent_) {
      if(Debug.EVENT) System.out.println("SGLabel: ignore event");
      return;
    }
    if(Debug.EVENT) {
      System.out.println("SGLabelDialog: " + event);
      System.out.println("               " + event.getPropertyName());
    }
    if(event.getPropertyName().equals("location")) {
      Point2D.Double locp = label_.getLocationP();
      xPosTextField.setText(String.valueOf(locp.x));
      yPosTextField.setText(String.valueOf(locp.y));
    }
  }
}
