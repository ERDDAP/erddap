/*
 * $Id: LogoDialog.java,v 1.6 2001/11/08 00:21:10 dwd Exp $
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


import gov.noaa.pmel.sgt.Logo;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Debug;

import javax.swing.*;
import java.awt.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Edits a <code>Logo</code> object.  This dialog does not make a copy
 * of the object so changes "Applied" will cause <code>sgt</code> to
 * redraw the plot using the new <code>Logo</code> properties.
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2001/11/08 00:21:10 $
 * @since 2.0
 */
public class LogoDialog extends JDialog implements PropertyChangeListener {
  private Logo logo_;
  private JPane pane_ = null;
  private boolean ignoreEvent_ = false;
  /**
   * Constructor.
   */
  public LogoDialog(Frame parent) {
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
    setSize(457,294);
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
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
    //$$ etchedBorder1.move(0,324);
    mainPanel.setLayout(new GridBagLayout());
    getContentPane().add(mainPanel, "Center");
    positionPanel.setBorder(positionBorder);
    positionPanel.setLayout(new GridBagLayout());
    mainPanel.add(positionPanel, new GridBagConstraints(0,0,2,1,0.0,0.0,
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
    mainPanel.add(alignPanel, new GridBagConstraints(0,1,1,1,0.0,0.0,
                                                     GridBagConstraints.WEST,GridBagConstraints.VERTICAL,new Insets(5,0,5,0),20,15));
    JLabel3.setText("Horizontal:");
    alignPanel.add(JLabel3, new GridBagConstraints(0,0,1,1,0.0,0.0,
                                                   GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    horizComboBox.setModel(horizCBModel);
    alignPanel.add(horizComboBox, new GridBagConstraints(1,0,1,1,0.0,0.0,
                                                         GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,5,5,5),0,0));
    JLabel4.setText("Vertical:");
    alignPanel.add(JLabel4, new GridBagConstraints(0,1,1,1,0.0,0.0,
                                                   GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    vertComboBox.setModel(vertCBModel);
    alignPanel.add(vertComboBox, new GridBagConstraints(1,1,1,1,0.0,0.0,
                                                        GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,5,5,5),0,0));
    optionPanel.setBorder(optionBorder);
    optionPanel.setLayout(new GridBagLayout());
    mainPanel.add(optionPanel, new GridBagConstraints(1,1,1,1,0.0,0.0,
                                                      GridBagConstraints.CENTER,GridBagConstraints.VERTICAL,new Insets(5,10,5,0),20,15));
    JLabel7.setText("Visible:");
    optionPanel.add(JLabel7, new GridBagConstraints(0,4,1,1,0.0,0.0,
                                                    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelVisibleCheckBox.setSelected(true);
    optionPanel.add(labelVisibleCheckBox, new GridBagConstraints(1,4,1,1,0.0,0.0,
                                                                 GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
    JLabel6.setText("Selectable:");
    optionPanel.add(JLabel6, new GridBagConstraints(0,5,1,1,0.0,0.0,
                                                    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelSelectableCheckBox.setSelected(true);
    optionPanel.add(labelSelectableCheckBox, new GridBagConstraints(1,5,1,1,0.0,0.0,
                                                                    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
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
    vertComboBox.setSelectedIndex(2);
    horizComboBox.setSelectedIndex(0);
    setTitle("SGLabel");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);

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

  public LogoDialog(String title) {
    this();
    setTitle(title);
  }

  public LogoDialog() {
    this((Frame)null);
  }
  /**
   * Make the dialog visible.
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
      if (object == LogoDialog.this)
        FontDialog_WindowClosing(event);
    }
  }

  void FontDialog_WindowClosing(java.awt.event.WindowEvent event) {
    finish();
    dispose();
  }

  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JPanel mainPanel = new javax.swing.JPanel();
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
  javax.swing.JPanel optionPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelVisibleCheckBox = new javax.swing.JCheckBox();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelSelectableCheckBox = new javax.swing.JCheckBox();
  javax.swing.border.TitledBorder alignBorder = new javax.swing.border.TitledBorder("Alignment");
  javax.swing.border.TitledBorder positionBorder = new javax.swing.border.TitledBorder("Position");
  DefaultComboBoxModel horizCBModel = new DefaultComboBoxModel();
  DefaultComboBoxModel vertCBModel = new DefaultComboBoxModel();
  javax.swing.border.TitledBorder optionBorder = new javax.swing.border.TitledBorder("Options");


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
        cancelButton_actionPerformed(event);
      else if (object == okButton)
        okButton_actionPerformed(event);
      if (object == applyButton)
        applyButton_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    finish();
    this.setVisible(false);
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    finish();
    updateLogo();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateLogo();
  }
  /**
   * Test entry point.
   */
  public static void main(String[] args) {
    LogoDialog la = new LogoDialog();
    la.setTitle("Test Logo Dialog");
    la.setVisible(true);
  }
  /**
   * Set the <code>Logo</code> to be edited and <code>JPane</code>.  With
   * <code>JPane</code> set the dialog will use
   * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching} when
   * updating the <code>Logo</code> properties.
   */
  public void setLogo(Logo logo, JPane pane) {
    setJPane(pane);
    setLogo(logo);
  }
  /**
   * Set the <code>Logo</code> to be edited.
   */
  public void setLogo(Logo logo) {
    logo_ = logo;
    logo_.addPropertyChangeListener(this);
    ignoreEvent_ = false;
    setLogo();
  }
  /**
   * Get the edited <code>Logo</code>
   */
  public Logo getLogo() {
    return logo_;
  }
  /**
   * Set the parent <code>JPane</code>. With
   * <code>JPane</code> set the dialog will use
   * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching} when
   * updating the <code>Logo</code> properties.
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

  private void setLogo() {
    //
    // label Id
    //
    setTitle("Logo - " + logo_.getId());
    //
    // options
    //
    labelVisibleCheckBox.setSelected(logo_.isVisible());
    labelSelectableCheckBox.setSelected(logo_.isSelectable());
    //
    // location
    //
    Point2D.Double locp = logo_.getLocationP();
    xPosTextField.setText(String.valueOf(locp.x));
    yPosTextField.setText(String.valueOf(locp.y));
    //
    // alignment
    //
    horizComboBox.setSelectedIndex(logo_.getHAlign());
    vertComboBox.setSelectedIndex(logo_.getVAlign());
  }

  private void updateLogo() {
    ignoreEvent_ = true;
    if(pane_ != null) pane_.setBatch(true, "LogoDialog");
    //
    // options
    //
    logo_.setVisible(labelVisibleCheckBox.isSelected());
    logo_.setSelectable(labelSelectableCheckBox.isSelected());
    //
    // Location
    //
    double x = Double.valueOf(xPosTextField.getText()).doubleValue();
    double y = Double.valueOf(yPosTextField.getText()).doubleValue();
    Point2D.Double locp = new Point2D.Double(x, y);
    logo_.setLocationP(locp);
    //
    // alignment
    //
    logo_.setHAlign(horizComboBox.getSelectedIndex());
    logo_.setVAlign(vertComboBox.getSelectedIndex());

    if(pane_ != null) pane_.setBatch(false, "LogoDialog");
    ignoreEvent_ = false;
  }
  private void finish() {
    logo_.removePropertyChangeListener(this);
  }
  /** Used internally for property changes  */
  public void propertyChange(PropertyChangeEvent event) {
    if(ignoreEvent_) {
      if(Debug.EVENT) System.out.println("Logo: ignore event");
      return;
    }
    if(Debug.EVENT) {
      System.out.println("LogoDialog: " + event);
      System.out.println("            " + event.getPropertyName());
    }
    if(event.getPropertyName().equals("location")) {
      Point2D.Double locp = logo_.getLocationP();
      xPosTextField.setText(String.valueOf(locp.x));
      yPosTextField.setText(String.valueOf(locp.y));
    }
  }

}
