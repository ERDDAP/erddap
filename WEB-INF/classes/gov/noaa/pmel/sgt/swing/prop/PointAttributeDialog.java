/*
 * $Id: PointAttributeDialog.java,v 1.7 2003/08/22 23:02:39 dwd Exp $
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

import javax.swing.*;
import java.awt.*;
import java.util.StringTokenizer;


import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.swing.PlotMarkIcon;
import gov.noaa.pmel.swing.ThreeDotsButton;

/**
 * Edits a <code>PointAttribute</code>. This dialog does not
 * make a copy of the attribute so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new properties unless
 * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching}
 * has been turned on.
 *
 * <p> Example of <code>PointAttributeDialog</code> use:
 * <pre>
 * public void editPointAttribute(PointAttribute attr, JPane pane) {
 *   PointAttributeDialog pad = new PointAttributeDialog();
 *   pad.setPointAttribute(attr, pane);
 *   pad.setVisible(true);
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 */
public class PointAttributeDialog extends JDialog {
  private JPane pane_ = null;
  private JPane[] paneList_ = null;
  private PointAttribute attr_;
  private PlotMarkIcon pmIcon_;
  private int mark_;
  private Font labelFont_;
  private String[] styleNames_ = {"plain", "bold", "italic", "bold-italic"};
  /**
   * Constructor.
   */
  public PointAttributeDialog(Frame parent) {
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
    setSize(new Dimension(504, 320));
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
    //$$ etchedBorder1.move(0,372);
    getContentPane().add(JTabbedPane1, "Center");
    markAttrPanel.setLayout(new GridBagLayout());
    JTabbedPane1.add(markAttrPanel, "markAttrPanel");
    JLabel1.setText("Color:");
    markAttrPanel.add(JLabel1, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    colorPanel.setBorder(etchedBorder1);
    markAttrPanel.add(colorPanel, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.BOTH,new Insets(0,5,10,5),0,0));
    JLabel6.setText("Mark:");
    markAttrPanel.add(JLabel6, new GridBagConstraints(0,1,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    markPanel.setLayout(new GridBagLayout());
    markAttrPanel.add(markPanel, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,10,5),0,0));
    markPanel.add(plotMarkIconLabel, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,5),0,0));
    plotMarkIconLabel.setForeground(java.awt.Color.black);
    markEditor.setActionCommand("...");
    markPanel.add(markEditor, new GridBagConstraints(2,0,1,1,0.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.VERTICAL,new Insets(0,0,0,0),0,0));
    JLabel7.setText("Height:");
    markAttrPanel.add(JLabel7, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    markHeightTextField.setColumns(10);
    markAttrPanel.add(markHeightTextField, new GridBagConstraints(1,2,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    labelPanel.setLayout(new GridBagLayout());
    JTabbedPane1.add(labelPanel, "labelPanel");
    JLabel5.setText("Position:");
    labelPanel.add(JLabel5, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    positionComboBox.setModel(positionCBModel);
    labelPanel.add(positionComboBox, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel8.setText("Font:");
    labelPanel.add(JLabel8, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    fontPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(fontPanel, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    fontLabel.setText("Dialog, 12, Bold");
    fontPanel.add(fontLabel);
    fontLabel.setForeground(java.awt.Color.black);
    fontEditor.setToolTipText("Edit font.");
    fontEditor.setActionCommand("...");
    fontPanel.add(fontEditor);
    JLabel9.setText("Color:");
    labelPanel.add(JLabel9, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelColorPanel.setBorder(etchedBorder1);
    labelColorPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(labelColorPanel, new GridBagConstraints(1,2,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.BOTH,new Insets(0,5,5,5),0,0));
    JLabel10.setText("Height:");
    labelPanel.add(JLabel10, new GridBagConstraints(0,3,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelHeightTextField.setColumns(10);
    labelPanel.add(labelHeightTextField, new GridBagConstraints(1,3,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel14.setText("Draw Label?");
    labelPanel.add(JLabel14, new GridBagConstraints(0,4,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelPanel.add(drawLabelCheckBox, new GridBagConstraints(1,4,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JTabbedPane1.setSelectedIndex(0);
    JTabbedPane1.setTitleAt(0,"Mark");
    JTabbedPane1.setTitleAt(1,"Label");
    {
      String[] tempString = new String[9];
      tempString[0] = "Centered";
      tempString[1] = "North";
      tempString[2] = "NorthEast";
      tempString[3] = "East";
      tempString[4] = "SouthEast";
      tempString[5] = "South";
      tempString[6] = "SouthWest";
      tempString[7] = "West";
      tempString[8] = "NorthWest";
      for(int i=0; i < tempString.length; i++) {
        positionCBModel.addElement(tempString[i]);
      }
    }
    markAttrPanel.setBounds(2,27,425,203);
    labelPanel.setBounds(2,27,425,203);
    positionComboBox.setSelectedIndex(2);
    setTitle("PointAttribute Properties");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);
    markEditor.addActionListener(lSymAction);
    fontEditor.addActionListener(lSymAction);
    //
    pmIcon_ = new PlotMarkIcon(1);
    plotMarkIconLabel.setIcon(pmIcon_);
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
   * Constructor
   */
  public PointAttributeDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor
   */
  public PointAttributeDialog() {
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
      if (object == PointAttributeDialog.this)
  PointAttributeDialog_WindowClosing(event);
    }
  }

  void PointAttributeDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JTabbedPane JTabbedPane1 = new javax.swing.JTabbedPane();
  javax.swing.JPanel markAttrPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel1 = new javax.swing.JLabel();
  ColorEntryPanel colorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JPanel markPanel = new javax.swing.JPanel();
  javax.swing.JLabel plotMarkIconLabel = new javax.swing.JLabel();
  ThreeDotsButton markEditor = new ThreeDotsButton();
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JTextField markHeightTextField = new javax.swing.JTextField();
  javax.swing.JPanel labelPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel5 = new javax.swing.JLabel();
  javax.swing.JComboBox positionComboBox = new javax.swing.JComboBox();
  javax.swing.JLabel JLabel8 = new javax.swing.JLabel();
  javax.swing.JPanel fontPanel = new javax.swing.JPanel();
  javax.swing.JLabel fontLabel = new javax.swing.JLabel();
  ThreeDotsButton fontEditor = new ThreeDotsButton();
  javax.swing.JLabel JLabel9 = new javax.swing.JLabel();
  ColorEntryPanel labelColorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel10 = new javax.swing.JLabel();
  javax.swing.JTextField labelHeightTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel14 = new javax.swing.JLabel();
  javax.swing.JCheckBox drawLabelCheckBox = new javax.swing.JCheckBox();
  DefaultComboBoxModel positionCBModel = new DefaultComboBoxModel();


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
        cancelButton_actionPerformed(event);
      else if (object == okButton)
        okButton_actionPerformed(event);
      else if (object == applyButton)
        applyButton_actionPerformed(event);
      else if (object == markEditor)
        markEditor_actionPerformed(event);
      else if (object == fontEditor)
        fontEditor_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    updatePointAttribute();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    updatePointAttribute();
  }
  /**
   * Set the <code>PointAttribute</code> to be edited and the
   * <code>JPane</code>.
   */
  public void setPointAttribute(PointAttribute attr, JPane pane) {
    pane_ = pane;
    attr_ = attr;
    //
    // Mark attributes
    //
    // Color
    //
    colorPanel.setColor(attr_.getColor());
    //
    // mark
    //
    int mark = attr_.getMark();
    changeMark(mark);
    //
    // mark height
    //
    markHeightTextField.setText(Double.toString(attr_.getMarkHeightP()));
    //
    // Label attributes
    //
    // position
    //
    positionComboBox.setSelectedIndex(attr_.getLabelPosition());
    //
    // font
    //
    labelFont_ = attr_.getLabelFont();
    if(pane_ != null && labelFont_ == null) labelFont_ = pane_.getComponent().getFont();
    fontLabel.setText(fontString(labelFont_));
    //
    // color
    //
    labelColorPanel.setColor(attr_.getLabelColor());
    //
    // width
    //
    labelHeightTextField.setText(Double.toString(attr_.getLabelHeightP()));
    //
    // draw label?
    //
    drawLabelCheckBox.setSelected(attr_.isDrawLabel());
  }
  /**
   * Set the parent <code>JPane</code>.  This reference to
   * <code>JPane</code> is used to enable/disable
   * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching} so
   * multiple property changes are made at one time.
   */
  public void setJPane(JPane pane) {
    paneList_ = new JPane[1];
    paneList_[0] = pane;
  }
  /**
   * Get the first parent.
   */
  public JPane getJPane() {
    if(paneList_ != null) {
      return paneList_[0];
    } else {
      return null;
    }
  }
  /**
   * Set the parent <code>JPane</code>s.  These references to
   * <code>JPane</code> are used to enable/disable
   * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching} so
   * multiple property changes are made at one time. A second
   * <code>JPane</code> is often used for a <code>PointCollectionKey</code>.
   */
  public void setJPaneList(JPane[] list) {
    paneList_ = list;
  }
  /** Get an array of parent panes. */
  public JPane[] getJPaneList() {
    return paneList_;
  }

  void updatePointAttribute() {
    if(pane_ != null) pane_.setBatch(true, "PointAttributeDialog");
    if(paneList_ != null) {
      for(int i=0; i < paneList_.length; i++) {
        paneList_[i].setBatch(true, "PointAttributeDialog");
      }
    }
    attr_.setBatch(true);
    //
    // mark attributes
    //
    // Color
    //
    attr_.setColor(colorPanel.getColor());
    //
    // mark
    //
    attr_.setMark(mark_);
    //
    // mark height
    //
    attr_.setMarkHeightP(Double.valueOf(markHeightTextField.getText()).doubleValue());
    //
    // label attributes
    //
    // position
    //
    attr_.setLabelPosition(positionComboBox.getSelectedIndex());
    //
    // font
    //
    attr_.setLabelFont(labelFont_);
    //
    // color
    //
    attr_.setLabelColor(labelColorPanel.getColor());
    //
    // height
    //
    attr_.setLabelHeightP(Double.valueOf(labelHeightTextField.getText()).doubleValue());
    //
    // draw label?
    //
    attr_.setDrawLabel(drawLabelCheckBox.isSelected());

    attr_.setBatch(false);
    //
    if(pane_ != null) pane_.setBatch(false, "PointAttributeDialog");
    if(paneList_ != null) {
      for(int i=0; i < paneList_.length; i++) {
        paneList_[i].setBatch(false, "PointAttributeDialog");
      }
    }
  }
  /**
   * Test entry point
   */
  public static void main(String[] args) {
    PointAttribute attr = new PointAttribute();
    PointAttributeDialog la = new PointAttributeDialog();
    la.setPointAttribute(attr, null);
    la.setTitle("Test PointAttribute Dialog");
    la.setVisible(true);
  }

  void markEditor_actionPerformed(java.awt.event.ActionEvent event) {
    PlotMarkDialog pmd = new PlotMarkDialog();
    Point loc = markEditor.getLocationOnScreen();
    pmd.setLocation(loc.x, loc.y);
    int result = pmd.showDialog(mark_);
    if(result == PlotMarkDialog.OK_RESPONSE) {
      changeMark(pmd.getMark());
    }
  }

  private void changeMark(int mark) {
    mark_ = mark;
    pmIcon_.setMark(mark);
    plotMarkIconLabel.repaint();
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
}
