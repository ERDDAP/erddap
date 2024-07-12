/*
 * $Id: VectorAttributeDialog.java,v 1.7 2003/08/22 23:02:40 dwd Exp $
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

import javax.swing.*;
import java.awt.*;
import java.util.StringTokenizer;

import gov.noaa.pmel.sgt.VectorAttribute;
import gov.noaa.pmel.sgt.swing.PlotMarkIcon;
import gov.noaa.pmel.swing.ThreeDotsButton;
import javax.swing.border.*;
import java.awt.event.*;

/**
 * Edits a <code>VectorAttribute</code>. This dialog does not
 * make a copy of the attribute so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new properties unless
 * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching}
 * has been turned on.
 *
 * <p> Example of <code>VectorAttributeDialog</code> use:
 * <pre>
 * public void editVectorAttribute(VectorAttribute attr) {
 *   VectorAttributeDialog lad = new VectorAttributeDialog();
 *   lad.setVectorAttribute(attr);
 *   lad.setVisible(true);
 * }
 * </pre>
 * *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/08/22 23:02:40 $
 * @since 2.1
 * @see PlotMarkDialog
 * @see ArrayEditDialog
 */
public class VectorAttributeDialog extends JDialog {
  private VectorAttribute attr_;
  private PlotMarkIcon pmIcon_;
  private int mark_;
  private JPane[] paneList_ = null;
  /**
   * Constructor.
   */
  public VectorAttributeDialog(Frame parent) {
    super(parent);
    try {
      jbInit();
      pack();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  void jbInit() throws Exception {
    //
    pmIcon_ = new PlotMarkIcon(1);
    {
      String[] tempString = new String[3];
      tempString[0] = "NO_HEAD";
      tempString[1] = "HEAD";
      tempString[2] = "SCALED_HEAD";
      for(int i=0; i < tempString.length; i++) {
        vectorStyleCBM.addElement(tempString[i]);
      }
    }
    {
      String[] tempString = new String[2];
      tempString[0] = "NO_MARK";
      tempString[1] = "MARK";
      for(int i=0; i < tempString.length; i++) {
        originStyleCBM.addElement(tempString[i]);
      }
    }
    {
      String[] tempString = new String[3];
      tempString[0] = "BUTT";
      tempString[1] = "ROUND";
      tempString[2] = "SQUARE";
      for(int i=0; i < tempString.length; i++) {
        capStyleCBM.addElement(tempString[i]);
      }
    }
    {
      String[] tempString = new String[3];
      tempString[0] = "MITER";
      tempString[1] = "ROUND";
      tempString[2] = "BEVEL";
      for(int i=0; i < tempString.length; i++) {
        miterStyleCBM.addElement(tempString[i]);
      }
    }
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(500,490);
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    vectorMaxSizeTextField.setColumns(8);
    vectorScaleTextField.setColumns(8);
    JLabel5.setText("Style:");
    colorPanel.setBorder(etchedBorder1);
    colorPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    JLabel1.setText("Color:");
    offsetAngleTextField.setColumns(8);
    vectorStyleComboBox.setModel(vectorStyleCBM);
    vectorStyleComboBox.addActionListener(new VectorAttributeDialog_vectorStyleComboBox_actionAdapter(this));
    jLabel7.setText("Offset Angle:");
    jLabel6.setText("Max Size:");
    jLabel5.setText("Scale:");
    vectorPanel.setLayout(gridBagLayout2);
    jLabel11.setText("Fixed Size:");
    jLabel10.setText("Max Size:");
    headMinSizeTextField.setColumns(8);
    jLabel9.setText("Min Size:");
    jLabel8.setText("Scale:");
    headScaleTextField.setColumns(8);
    headMaxSizeTextField.setColumns(8);
    headFixedSizeTextField.setColumns(8);
    headPanel.setLayout(gridBagLayout4);
    markHeightTextField.setColumns(8);
    originStyleComboBox.setModel(originStyleCBM);
    JLabel12.setText("Color:");
    JLabel7.setText("Mark Height:");
    JLabel6.setText("Mark:");
    markPanel.setLayout(new GridBagLayout());
    markEditor.addActionListener(new VectorAttributeDialog_markEditor_actionAdapter(this));
    markEditor.setActionCommand("...");
//    markEditor.setMargin(new Insets(0, 0, 0, 0));
    markColorPanel.setBorder(etchedBorder1);
    Mark.setLayout(gridBagLayout3);
    jLabel4.setText("Style:");
    plotMarkIconLabel.setForeground(java.awt.Color.black);
    plotMarkIconLabel.setIcon(pmIcon_);
    miterLimitTextField.setColumns(8);
    JLabel10.setText(" Width:");
    strokePanel.setLayout(gridBagLayout1);
    miterStyleComboBox.setModel(miterStyleCBM);
    jLabel3.setText("Miter Limit:");
    jLabel2.setText("Miter Style:");
    jLabel1.setText("Cap Style:");
    capStyleComboBox.setModel(capStyleCBM);
    widthTextField.setColumns(8);
    getContentPane().add(buttonPanel, "South");
    buttonPanel.setBounds(0,263,430,39);
    okButton.setText("OK");
    okButton.setActionCommand("OK");
    buttonPanel.add(okButton);
    okButton.setBounds(115,7,51,25);
    applyButton.setText("Apply");
    applyButton.setActionCommand("Apply");
    buttonPanel.add(applyButton);
    applyButton.setBounds(171,7,65,25);
    cancelButton.setText("Cancel");
    cancelButton.setActionCommand("Cancel");
    buttonPanel.add(cancelButton);
    cancelButton.setBounds(241,7,73,25);
    //$$ etchedBorder1.move(0,300);
    this.getContentPane().add(jTabbedPane1, BorderLayout.CENTER);
    jTabbedPane1.add(vectorPanel, "Vector");
    vectorPanel.add(vectorStyleComboBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(15, 5, 5, 5), 0, 0));
    vectorPanel.add(colorPanel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 15), 0, 0));
    vectorPanel.add(JLabel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    vectorPanel.add(jLabel5, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 6, 0, 6), 0, 0));
    vectorPanel.add(jLabel6, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    vectorPanel.add(jLabel7, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 15, 10, 5), 0, 0));
    vectorPanel.add(vectorScaleTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    vectorPanel.add(vectorMaxSizeTextField, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    vectorPanel.add(offsetAngleTextField, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 15, 5), 0, 0));
    vectorPanel.add(JLabel5, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 5, 0, 5), 0, 0));
    jTabbedPane1.add(headPanel, "Head");
    headPanel.add(jLabel8, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    headPanel.add(headScaleTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    headPanel.add(jLabel9, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    headPanel.add(jLabel10, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    headPanel.add(headMinSizeTextField, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    headPanel.add(headMaxSizeTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    headPanel.add(jLabel11, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    headPanel.add(headFixedSizeTextField, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    jTabbedPane1.add(Mark, "Mark");
    Mark.add(markPanel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    markPanel.add(plotMarkIconLabel, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,5),0,0));
    markPanel.add(markEditor, new GridBagConstraints(2,0,1,1,0.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.VERTICAL,new Insets(0,0,0,0),0,0));
    Mark.add(JLabel6, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    Mark.add(JLabel7, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 15, 10, 5), 0, 0));
    Mark.add(markHeightTextField, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 15, 5), 0, 0));
    Mark.add(markColorPanel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 15), 0, 0));
    Mark.add(JLabel12, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    Mark.add(jLabel4, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 5, 0, 5), 0, 0));
    Mark.add(originStyleComboBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(15, 5, 5, 5), 0, 0));
    jTabbedPane1.add(strokePanel, "Line Style");
    strokePanel.add(JLabel10, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(widthTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(jLabel1, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(jLabel2, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(capStyleComboBox, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(miterStyleComboBox, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(jLabel3, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(miterLimitTextField, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    //$$ stringComboBoxModel1.move(24,300);
    setTitle("VectorAttribute Properties");

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
  /**
   * Constructor.
   */
  public VectorAttributeDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public VectorAttributeDialog() {
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
      if (object == VectorAttributeDialog.this)
  VectorAttributeDialog_WindowClosing(event);
    }
  }

  void VectorAttributeDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  DefaultComboBoxModel vectorStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel originStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel capStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel miterStyleCBM = new DefaultComboBoxModel();
  JTabbedPane jTabbedPane1 = new JTabbedPane();
  JTextField vectorMaxSizeTextField = new JTextField();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  JTextField vectorScaleTextField = new JTextField();
  JLabel JLabel5 = new javax.swing.JLabel();
  ColorEntryPanel colorPanel = new ColorEntryPanel();
  JLabel JLabel1 = new javax.swing.JLabel();
  JTextField offsetAngleTextField = new JTextField();
  JComboBox vectorStyleComboBox = new javax.swing.JComboBox();
  JLabel jLabel7 = new JLabel();
  JLabel jLabel6 = new JLabel();
  JLabel jLabel5 = new JLabel();
  JPanel vectorPanel = new JPanel();
  JLabel jLabel11 = new JLabel();
  JLabel jLabel10 = new JLabel();
  GridBagLayout gridBagLayout4 = new GridBagLayout();
  JTextField headMinSizeTextField = new JTextField();
  JLabel jLabel9 = new JLabel();
  JLabel jLabel8 = new JLabel();
  JTextField headScaleTextField = new JTextField();
  JTextField headMaxSizeTextField = new JTextField();
  JTextField headFixedSizeTextField = new JTextField();
  JPanel headPanel = new JPanel();
  JTextField markHeightTextField = new javax.swing.JTextField();
  GridBagLayout gridBagLayout3 = new GridBagLayout();
  JComboBox originStyleComboBox = new JComboBox();
  JLabel JLabel12 = new javax.swing.JLabel();
  JLabel JLabel7 = new javax.swing.JLabel();
  JLabel JLabel6 = new javax.swing.JLabel();
  JPanel markPanel = new javax.swing.JPanel();
  ThreeDotsButton markEditor = new ThreeDotsButton();
  ColorEntryPanel markColorPanel = new ColorEntryPanel();
  JPanel Mark = new JPanel();
  JLabel jLabel4 = new JLabel();
  JLabel plotMarkIconLabel = new javax.swing.JLabel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JTextField miterLimitTextField = new JTextField();
  JLabel JLabel10 = new javax.swing.JLabel();
  JPanel strokePanel = new JPanel();
  JComboBox miterStyleComboBox = new JComboBox();
  JLabel jLabel3 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JLabel jLabel1 = new JLabel();
  JComboBox capStyleComboBox = new JComboBox();
  JTextField widthTextField = new javax.swing.JTextField();


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
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateVectorAttribute();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateVectorAttribute();
  }
  /**
   * Set the <code>VectorAttribute</code> for the dialog.
   */
  public void setVectorAttribute(VectorAttribute attr) {
    attr_ = attr;
    //
    // Vector
    //
    // Color
    //
    colorPanel.setColor(attr_.getVectorColor());
    //
    // style
    //
    vectorStyleComboBox.setSelectedIndex(attr_.getVectorStyle());
    vectorStyle(attr_.getVectorStyle());
    //
    // vectorScale
    //
    vectorScaleTextField.setText(Double.toString(attr_.getVectorScale()));
    //
    // vector max size
    //
    vectorMaxSizeTextField.setText(Double.toString(attr_.getVectorMaxSize()));
    //
    // offset angle
    //
    offsetAngleTextField.setText(Double.toString(attr_.getOffsetAngle()));
    //
    // head
    //
    //
    // scale
    //
    headScaleTextField.setText(Double.toString(attr_.getHeadScale()));
    //
    // head max size
    //
    headMaxSizeTextField.setText(Double.toString(attr_.getHeadMaxSize()));
    //
    // head min size
    //
    headMinSizeTextField.setText(Double.toString(attr_.getHeadMinSize()));
    //
    // head fixed size
    //
    headFixedSizeTextField.setText(Double.toString(attr_.getHeadFixedSize()));
    //
    // origin
    //
    // style
    //
    originStyleComboBox.setSelectedIndex(attr_.getOriginStyle());
    //
    // mark
    //
    int mark = attr_.getMark();
    changeMark(mark);
//    System.out.println(" mark code = " + mark);
    //
    // Color
    //
    markColorPanel.setColor(attr_.getMarkColor());
    //
    // mark height
    //
    markHeightTextField.setText(Double.toString(attr_.getMarkHeightP()));
    //
    // Stroke line attributes
    //
    // width
    //
    widthTextField.setText(Float.toString(attr_.getWidth()));
    //
    // cap style
    //
    capStyleComboBox.setSelectedIndex(attr_.getCapStyle());
    //
    // miter style
    //
    miterStyleComboBox.setSelectedIndex(attr_.getMiterStyle());
    //
    // miter limit
    //
    miterLimitTextField.setText(Float.toString(attr_.getMiterLimit()));
  }

  void updateVectorAttribute() {
    if(paneList_ != null) {
      for(int i=0; i < paneList_.length; i++) {
        paneList_[i].setBatch(true, "VectorAttributeDialog");
      }
    }
    attr_.setBatch(true);
    //
    // Vector
    //
    //
    // Color
    //
    attr_.setVectorColor(colorPanel.getColor());
    //
    // style
    //
    attr_.setVectorStyle(vectorStyleComboBox.getSelectedIndex());
    //
    // vectorScale
    //
    attr_.setVectorScale(Double.parseDouble(vectorScaleTextField.getText()));
    //
    // vector max size
    //
    attr_.setVectorMaxSize(Double.parseDouble(vectorMaxSizeTextField.getText()));
    //
    // offset angle
    //
    attr_.setOffsetAngle(Double.parseDouble(offsetAngleTextField.getText()));
    //
    // head
    //
    //
    // scale
    //
    attr_.setHeadScale(Double.parseDouble(headScaleTextField.getText()));
    //
    // max head size
    //
    attr_.setHeadMaxSize(Double.parseDouble(headMaxSizeTextField.getText()));
    //
    // head min size
    //
    attr_.setHeadMinSize(Double.parseDouble(headMinSizeTextField.getText()));
    //
    // head fixed size
    //
    attr_.setHeadFixedSize(Double.parseDouble(headFixedSizeTextField.getText()));
    //
    // origin
    //
    // style
    //
    attr_.setOriginStyle(originStyleComboBox.getSelectedIndex());
    //
    // mark
    //
    attr_.setMark(mark_);
    //
    // color
    //
    attr_.setMarkColor(markColorPanel.getColor());
    //
    // mark height
    //
    attr_.setMarkHeightP(Double.valueOf(markHeightTextField.getText()).doubleValue());
    //
    // stroke attributes
    //
    //
    // width
    //
    attr_.setWidth(Float.valueOf(widthTextField.getText()).floatValue());
    //
    // cap style
    //
    attr_.setCapStyle(capStyleComboBox.getSelectedIndex());
    //
    // miter style
    //
    attr_.setMiterStyle(miterStyleComboBox.getSelectedIndex());
    //
    // miter limit
    //
    attr_.setMiterLimit(Float.valueOf(miterLimitTextField.getText()).floatValue());

    attr_.setBatch(false);
    //
    if(paneList_ != null) {
      for(int i=0; i < paneList_.length; i++) {
        paneList_[i].setBatch(false, "VectorAttributeDialog");
      }
    }
  }

  private void vectorStyle(int style) {
    switch(style) {
      case VectorAttribute.NO_HEAD:
        headScaleTextField.setEnabled(false);
        headMinSizeTextField.setEnabled(false);
        headMaxSizeTextField.setEnabled(false);
        headFixedSizeTextField.setEnabled(false);
      break;
      case VectorAttribute.HEAD:
        headScaleTextField.setEnabled(false);
        headMinSizeTextField.setEnabled(false);
        headMaxSizeTextField.setEnabled(false);
        headFixedSizeTextField.setEnabled(true);
      break;
      case VectorAttribute.SCALED_HEAD:
        headScaleTextField.setEnabled(true);
        headMinSizeTextField.setEnabled(true);
        headMaxSizeTextField.setEnabled(true);
        headFixedSizeTextField.setEnabled(false);
    }
  }
  /**
   * Dialog test entry.
   */
  public static void main(String[] args) {
    VectorAttribute attr = new VectorAttribute();
    VectorAttributeDialog la = new VectorAttributeDialog();
    la.setVectorAttribute(attr);
    la.setTitle("Test VectorAttribute Dialog");
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
  /** Get the first parent pane. */
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
   * <code>JPane</code> is often used for a <code>VectorKey</code>.
   */
  public void setJPaneList(JPane[] list) {
    paneList_ = list;
  }
  /** Get an array of parent panes. */
  public JPane[] getJPaneList() {
    return paneList_;
  }

  void vectorStyleComboBox_actionPerformed(ActionEvent e) {
    int style = vectorStyleComboBox.getSelectedIndex();
    vectorStyle(style);
  }
}

class VectorAttributeDialog_vectorStyleComboBox_actionAdapter implements java.awt.event.ActionListener {
  VectorAttributeDialog adaptee;

  VectorAttributeDialog_vectorStyleComboBox_actionAdapter(VectorAttributeDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.vectorStyleComboBox_actionPerformed(e);
  }
}

class VectorAttributeDialog_markEditor_actionAdapter implements java.awt.event.ActionListener {
  VectorAttributeDialog adaptee;

  VectorAttributeDialog_markEditor_actionAdapter(VectorAttributeDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.markEditor_actionPerformed(e);
  }
}
