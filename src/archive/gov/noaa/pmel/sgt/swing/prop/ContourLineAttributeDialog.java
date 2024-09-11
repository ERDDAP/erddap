/*
 * $Id: ContourLineAttributeDialog.java,v 1.11 2003/08/22 23:02:39 dwd Exp $
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

import gov.noaa.pmel.sgt.ContourLineAttribute;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.swing.ThreeDotsButton;
import javax.swing.border.*;
import java.awt.event.*;

/**
 * Edits a <code>ContourLineAttribute</code>.  This dialog does not
 * make a copy of the attribute so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new properties unless
 * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching}
 * has been turned on.
 *
 * <p> Example of <code>ContourLineAttributeDialog</code> use:
 * <pre>
 * public void editCLAttribute(ContourLineAttribute cla) {
 *   ContourLineAttributeDialog clad = new ContourLineAttributeDialog();
 *   clad.showDialog(cla);
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.11 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 */
public class ContourLineAttributeDialog extends JDialog {
  private ContourLineAttribute attr_;
  private Font labelFont_;
  private int result_;
  private String[] styleNames_ = {"plain", "bold", "italic", "bold-italic"};
  /** OK button was selected */
  public static int OK_RESPONSE = 1;
  /** Cancel button was selected */
  public static int CANCEL_RESPONSE = 2;
  /**
   * Constructor.
   */
  public ContourLineAttributeDialog(Frame parent) {
    super(parent);
    try {
      jbInit();
      pack();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  void jbInit() throws Exception {
    {
      String[] tempString = new String[7];
      tempString[0] = "SOLID";
      tempString[1] = "DASHED";
      tempString[2] = "HEAVY";
      tempString[3] = "HIGHLIGHT";
      tempString[4] = "MARK";
      tempString[5] = "MARK & SOLID";
      tempString[6] = "STROKE";
      for(int i=0; i < tempString.length; i++) {
        lineStyleCBM.addElement(tempString[i]);
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
    titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(142, 142, 142)),"Stroke Line Attributes");
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(new Dimension(587, 410));
    setVisible(false);
    strokePanel.setBorder(titledBorder1);
    strokePanel.setLayout(gridBagLayout1);
    dashArrayUseDefault.setActionCommand("dashArray");
    JLabel110.setText(" Width:");
    dashPhaseTextField.setColumns(10);
    JLabel27.setText("Dash Phase:");
    JLabel28.setText("Dash Array:");
    widthTextField.setColumns(10);
    arrayEditor.setActionCommand("...");
    arrayEditor.setToolTipText("Edit dash array.");
    dashArrayPanel.setLayout(new GridBagLayout());
    dashPhaseUseDefault.setActionCommand("dashPhase");
    widthUseDefault.setActionCommand("width");
    jLabel1.setText("Cap Style:");
    jLabel2.setText("Miter Style:");
    jLabel3.setText("Miter Limit:");
    miterLimitTextField.setColumns(10);
    capStyleComboBox.setModel(capStyleCBM);
    miterStyleComboBox.setModel(miterStyleCBM);
    lineStyleComboBox.addActionListener(new ContourLineAttributeDialog_lineStyleComboBox_actionAdapter(this));
    capStyleUseDefault.setActionCommand("capStyle");
    miterStyleUseDefault.setActionCommand("miterStyle");
    miterLimitUseDefault.setActionCommand("miterLimit");
    getContentPane().add(TabbedPane, "Center");
    linePanel.setLayout(new GridBagLayout());
    TabbedPane.add(linePanel, "linePanel");
    labelPanel.setLayout(new GridBagLayout());
    TabbedPane.add(labelPanel, "labelPanel");
    labelPanel.setBounds(2,27,476,260);
    labelPanel.setVisible(false);
    JLabel23.setText("Property");
    labelPanel.add(JLabel23, new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.SOUTH,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    JLabel23.setForeground(java.awt.Color.darkGray);
    JPanel3.setLayout(new GridBagLayout());
    labelPanel.add(JPanel3, new GridBagConstraints(1,0,1,1,0.0,0.0,
    GridBagConstraints.SOUTH,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    JLabel24.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    JLabel24.setText("Use");
    JPanel3.add(JLabel24, new GridBagConstraints(0,0,1,1,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,0,0,0),0,0));
    JLabel24.setForeground(java.awt.Color.darkGray);
    JLabel25.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    JLabel25.setText("Default");
    JPanel3.add(JLabel25, new GridBagConstraints(0,1,1,1,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
    JLabel25.setForeground(java.awt.Color.darkGray);
    JLabel26.setText("Value");
    labelPanel.add(JLabel26, new GridBagConstraints(2,0,1,1,0.0,0.0,
    GridBagConstraints.SOUTH,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    JLabel26.setForeground(java.awt.Color.darkGray);
    JPanel4.setBorder(etchedBorder1);
    JPanel4.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(JPanel4, new GridBagConstraints(0,1,3,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(2,5,7,5),0,-12));
    JLabel11.setText("Color:");
    labelPanel.add(JLabel11, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelColorUseDefault.setActionCommand("labelColor");
    labelPanel.add(labelColorUseDefault, new GridBagConstraints(1,2,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    textColorPanel.setBorder(etchedBorder1);
    textColorPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(textColorPanel, new GridBagConstraints(2,2,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    JLabel15.setText("Font:");
    labelPanel.add(JLabel15, new GridBagConstraints(0,3,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelFontUseDefault.setActionCommand("labelFont");
    labelPanel.add(labelFontUseDefault, new GridBagConstraints(1,3,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    fontPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(fontPanel, new GridBagConstraints(2,3,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    fontLabel.setText("Dialog, 12, Bold");
    fontPanel.add(fontLabel);
    fontLabel.setForeground(java.awt.Color.black);
    fontEditor.setToolTipText("Edit font.");
    fontEditor.setActionCommand("...");
    fontPanel.add(fontEditor);
    JLabel16.setText("HeightP:");
    labelPanel.add(JLabel16, new GridBagConstraints(0,4,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    heightPUseDefault.setActionCommand("heightP");
    labelPanel.add(heightPUseDefault, new GridBagConstraints(1,4,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    heightTextField.setColumns(10);
    labelPanel.add(heightTextField, new GridBagConstraints(2,4,1,1,1.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel6.setText("Text:");
    labelPanel.add(JLabel6, new GridBagConstraints(0,6,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelPanel.add(textTextField, new GridBagConstraints(2,6,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,5,2,5),0,0));
    JLabel22.setText("Format:");
    labelPanel.add(JLabel22, new GridBagConstraints(0,7,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelFormatUseDefault.setActionCommand("labelFormat");
    labelPanel.add(labelFormatUseDefault, new GridBagConstraints(1,7,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    labelFormatTextField.setColumns(10);
    labelPanel.add(labelFormatTextField, new GridBagConstraints(2,7,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel17.setText("Auto Label:");
    labelPanel.add(JLabel17, new GridBagConstraints(0,5,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelPanel.add(autoLabelCheckBox, new GridBagConstraints(2,5,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    JLabel7.setText("Enabled:");
    labelPanel.add(JLabel7, new GridBagConstraints(0,8,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(5,5,0,5),0,0));
    labelEnabledUseDefault.setActionCommand("labelEnabled");
    labelPanel.add(labelEnabledUseDefault, new GridBagConstraints(1,8,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    labelEnabledCheckBox.setSelected(true);
    labelPanel.add(labelEnabledCheckBox, new GridBagConstraints(2,8,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
    TabbedPane.setSelectedComponent(linePanel);
    TabbedPane.setSelectedIndex(0);
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
    //$$ etchedBorder1.move(0,384);
    linePanel.setBounds(2,27,476,260);
    linePanel.setVisible(false);
    JLabel1.setText("Color:");
    linePanel.add(JLabel1, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    colorUseDefault.setActionCommand("color");
    linePanel.add(colorUseDefault, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 15, 0, 10), 0, 0));
    lineColorPanel.setBorder(etchedBorder1);
    lineColorPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    linePanel.add(lineColorPanel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel5.setText("Line Style:");
    linePanel.add(JLabel5, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 28, 0, 5), 0, 0));
    styleUseDefault.setActionCommand("style");
    linePanel.add(styleUseDefault, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 15, 0, 10), 0, 0));
    lineStyleComboBox.setModel(lineStyleCBM);
    linePanel.add(lineStyleComboBox, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel18.setText("Property");
    linePanel.add(JLabel18, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    JLabel18.setForeground(java.awt.Color.darkGray);
    JPanel1.setLayout(new GridBagLayout());
    linePanel.add(JPanel1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    JLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    JLabel19.setText("Use");
    JPanel1.add(JLabel19, new GridBagConstraints(0,0,1,1,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,0,0,0),0,0));
    JLabel19.setForeground(java.awt.Color.darkGray);
    JLabel20.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    JLabel20.setText("Default");
    JPanel1.add(JLabel20, new GridBagConstraints(0,1,1,1,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
    JLabel20.setForeground(java.awt.Color.darkGray);
    JLabel21.setText("Value");
    linePanel.add(JLabel21, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    JLabel21.setForeground(java.awt.Color.darkGray);
    JPanel2.setBorder(etchedBorder1);
    JPanel2.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    linePanel.add(JPanel2, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 7, 0), 0, -12));
    linePanel.add(strokePanel, new GridBagConstraints(0, 5, 3, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(dashArrayUseDefault, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 15, 0, 10), 0, 0));
    strokePanel.add(JLabel110, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(dashPhaseTextField, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(JLabel27, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(JLabel28, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(widthTextField, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(dashArrayPanel, new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));
    dashArrayPanel.add(dashArrayTextField, new GridBagConstraints(0,0,1,1,1.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
    dashArrayPanel.add(arrayEditor, new GridBagConstraints(1,0,1,1,0.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.VERTICAL,new Insets(0,0,0,0),0,0));
    strokePanel.add(dashPhaseUseDefault, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 15, 0, 10), 0, 0));
    strokePanel.add(widthUseDefault, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 15, 0, 10), 0, 0));
    strokePanel.add(jLabel1, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(jLabel2, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(jLabel3, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(capStyleUseDefault, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 15, 0, 10), 0, 0));
    strokePanel.add(miterStyleUseDefault, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 15, 0, 10), 0, 0));
    strokePanel.add(miterLimitUseDefault, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 15, 0, 10), 0, 0));
    strokePanel.add(capStyleComboBox, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(miterStyleComboBox, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(miterLimitTextField, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    TabbedPane.setTitleAt(0,"Line");
    TabbedPane.setTitleAt(1,"Label");
    //$$ stringComboBoxModel1.move(24,384);
    lineStyleComboBox.setSelectedIndex(0);
    setTitle("ContourLineAttribute Properties");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);
    fontEditor.addActionListener(lSymAction);
    autoLabelCheckBox.addActionListener(lSymAction);
    labelEnabledCheckBox.addActionListener(lSymAction);
    arrayEditor.addActionListener(lSymAction);

    //
    // Use Default checkbox's have their own action listener
    //
    CBAction cbAction = new CBAction();
    colorUseDefault.addActionListener(cbAction);
    styleUseDefault.addActionListener(cbAction);
    labelColorUseDefault.addActionListener(cbAction);
    labelFontUseDefault.addActionListener(cbAction);
    heightPUseDefault.addActionListener(cbAction);
    labelFormatUseDefault.addActionListener(cbAction);
    labelEnabledUseDefault.addActionListener(cbAction);
    dashArrayUseDefault.addActionListener(cbAction);
    dashPhaseUseDefault.addActionListener(cbAction);
    widthUseDefault.addActionListener(cbAction);
    capStyleUseDefault.addActionListener(cbAction);
    miterStyleUseDefault.addActionListener(cbAction);
    miterLimitUseDefault.addActionListener(cbAction);

//    Insets pup = new Insets(0, 0, 0, 0);
//    fontEditor.setMargin(pup);
//    arrayEditor.setMargin(pup);
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
  public ContourLineAttributeDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public ContourLineAttributeDialog() {
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
      if (object == ContourLineAttributeDialog.this)
  ContourLineAttributeDialog_WindowClosing(event);
    }
  }

  void ContourLineAttributeDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JTabbedPane TabbedPane = new javax.swing.JTabbedPane();
  javax.swing.JPanel linePanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel1 = new javax.swing.JLabel();
  javax.swing.JCheckBox colorUseDefault = new javax.swing.JCheckBox();
  ColorEntryPanel lineColorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel5 = new javax.swing.JLabel();
  javax.swing.JCheckBox styleUseDefault = new javax.swing.JCheckBox();
  javax.swing.JComboBox lineStyleComboBox = new javax.swing.JComboBox();
  javax.swing.JLabel JLabel18 = new javax.swing.JLabel();
  javax.swing.JPanel JPanel1 = new javax.swing.JPanel();
  javax.swing.JLabel JLabel19 = new javax.swing.JLabel();
  javax.swing.JLabel JLabel20 = new javax.swing.JLabel();
  javax.swing.JLabel JLabel21 = new javax.swing.JLabel();
  javax.swing.JPanel JPanel2 = new javax.swing.JPanel();
  javax.swing.JPanel labelPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel23 = new javax.swing.JLabel();
  javax.swing.JPanel JPanel3 = new javax.swing.JPanel();
  javax.swing.JLabel JLabel24 = new javax.swing.JLabel();
  javax.swing.JLabel JLabel25 = new javax.swing.JLabel();
  javax.swing.JLabel JLabel26 = new javax.swing.JLabel();
  javax.swing.JPanel JPanel4 = new javax.swing.JPanel();
  javax.swing.JLabel JLabel11 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelColorUseDefault = new javax.swing.JCheckBox();
  ColorEntryPanel textColorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel15 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelFontUseDefault = new javax.swing.JCheckBox();
  javax.swing.JPanel fontPanel = new javax.swing.JPanel();
  javax.swing.JLabel fontLabel = new javax.swing.JLabel();
  ThreeDotsButton fontEditor = new ThreeDotsButton();
  javax.swing.JLabel JLabel16 = new javax.swing.JLabel();
  javax.swing.JCheckBox heightPUseDefault = new javax.swing.JCheckBox();
  javax.swing.JTextField heightTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JTextField textTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel22 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelFormatUseDefault = new javax.swing.JCheckBox();
  javax.swing.JTextField labelFormatTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel17 = new javax.swing.JLabel();
  javax.swing.JCheckBox autoLabelCheckBox = new javax.swing.JCheckBox();
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelEnabledUseDefault = new javax.swing.JCheckBox();
  javax.swing.JCheckBox labelEnabledCheckBox = new javax.swing.JCheckBox();
  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  DefaultComboBoxModel lineStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel capStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel miterStyleCBM = new DefaultComboBoxModel();
  JPanel strokePanel = new JPanel();
  TitledBorder titledBorder1;
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JCheckBox dashArrayUseDefault = new javax.swing.JCheckBox();
  JLabel JLabel110 = new javax.swing.JLabel();
  JTextField dashPhaseTextField = new javax.swing.JTextField();
  JLabel JLabel27 = new javax.swing.JLabel();
  JLabel JLabel28 = new javax.swing.JLabel();
  JTextField dashArrayTextField = new javax.swing.JTextField();
  JTextField widthTextField = new javax.swing.JTextField();
  ThreeDotsButton arrayEditor = new ThreeDotsButton();
  JPanel dashArrayPanel = new javax.swing.JPanel();
  JCheckBox dashPhaseUseDefault = new javax.swing.JCheckBox();
  JCheckBox widthUseDefault = new javax.swing.JCheckBox();
  JLabel jLabel1 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JLabel jLabel3 = new JLabel();
  JCheckBox capStyleUseDefault = new JCheckBox();
  JCheckBox miterStyleUseDefault = new JCheckBox();
  JCheckBox miterLimitUseDefault = new JCheckBox();
  JComboBox capStyleComboBox = new JComboBox();
  JComboBox miterStyleComboBox = new JComboBox();
  JTextField miterLimitTextField = new JTextField();


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
        cancelButton_actionPerformed(event);
      else if (object == okButton)
        okButton_actionPerformed(event);
      else if (object == applyButton)
        applyButton_actionPerformed(event);
      else if (object == fontEditor)
        fontEditor_actionPerformed(event);
      else if (object == autoLabelCheckBox)
        autoLabelCheckBox_actionPerformed(event);
      else if (object == labelEnabledCheckBox)
        labelEnabledCheckBox_actionPerformed(event);
      else if (object == arrayEditor)
        arrayEditor_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
    result_ = CANCEL_RESPONSE;
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    result_ = OK_RESPONSE;
    updateContourLineAttribute();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    result_ = OK_RESPONSE;
    updateContourLineAttribute();
  }

  class CBAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if(object instanceof JCheckBox)
  checkBox_actionPerformed(event);
    }
  }

  void checkBox_actionPerformed(java.awt.event.ActionEvent event) {
    String action = event.getActionCommand();
    useDefault(action);
  }

  private void useDefault(String action) {
    boolean enable;
    boolean stroke = lineStyleComboBox.getSelectedIndex() == LineAttribute.STROKE;
    if(action.equals("color")) {
      enable = !colorUseDefault.isSelected();
      lineColorPanel.setEnabled(enable);
    } else if(action.equals("style")) {
      lineStyleComboBox.setEnabled(!styleUseDefault.isSelected());
    } else if(action.equals("width")) {
      widthTextField.setEnabled(!widthUseDefault.isSelected() && stroke);
    } else if(action.equals("dashArray")) {
      enable = !dashArrayUseDefault.isSelected() && stroke;
      dashArrayTextField.setEnabled(enable);
      arrayEditor.setEnabled(enable);
    } else if(action.equals("dashPhase")) {
      dashPhaseTextField.setEnabled(!dashPhaseUseDefault.isSelected() && stroke);
    } else if(action.equals("capStyle")) {
      capStyleComboBox.setEnabled(!capStyleUseDefault.isSelected() && stroke);
    } else if(action.equals("miterStyle")) {
      miterStyleComboBox.setEnabled(!miterStyleUseDefault.isSelected() && stroke);
    } else if(action.equals("miterLimit")) {
      miterLimitTextField.setEnabled(!miterLimitUseDefault.isSelected() && stroke);
    } else if(action.equals("labelColor")) {
      enable = !labelColorUseDefault.isSelected();
      textColorPanel.setEnabled(enable);
    } else if(action.equals("labelFont")) {
      enable = !labelFontUseDefault.isSelected();
      fontLabel.setEnabled(enable);
      fontEditor.setEnabled(enable);
    } else if(action.equals("heightP")) {
      heightTextField.setEnabled(!heightPUseDefault.isSelected());
    } else if(action.equals("labelFormat")) {
      labelFormatTextField.setEnabled(!labelFormatUseDefault.isSelected());
    } else if(action.equals("labelEnabled")) {
      labelEnabledCheckBox.setEnabled(!labelEnabledUseDefault.isSelected());
    } else {
      System.out.println("Action " + action + " not found.");
    }
  }

  private void useDefaultAll() {
    useDefault("color");
    useDefault("style");
    useDefault("width");
    useDefault("dashArray");
    useDefault("dashPhase");
    useDefault("capStyle");
    useDefault("miterStyle");
    useDefault("miterLimit");
    useDefault("labelColor");
    useDefault("labelFont");
    useDefault("heightP");
    useDefault("labelFormat");
    useDefault("labelEnabled");
  }
  /**
   * Show the dialog and wait for a response
   *
   * @return result, either CANCE_RESPONSE or OK_RESPONSE
   */
  public int showDialog(ContourLineAttribute attr) {
    setContourLineAttribute(attr);
    result_ = CANCEL_RESPONSE;
    setModal(true);
    super.setVisible(true);
    return result_;
  }
  /**
   * Set the <code>ContourLineAttribute</code> for editing.
   */
  public void setContourLineAttribute(ContourLineAttribute attr) {
    attr_ = attr;
    //
    // Line attributes
    //
    // Color
    //
    lineColorPanel.setColor(attr_.getColor());
    colorUseDefault.setSelected(!attr_.isColorOverridden());
    //
    // style
    //
    lineStyleComboBox.setSelectedIndex(attr_.getStyle());
    styleUseDefault.setSelected(!attr_.isStyleOverridden());
    strokePanelEnabled(attr_.getStyle() == LineAttribute.STROKE);
    //
    // width
    //
    widthTextField.setText(Float.toString(attr_.getWidth()));
    widthUseDefault.setSelected(!attr_.isWidthOverridden());
    //
    // dash Array
    //
    float[] da = attr_.getDashArray();
    dashArrayTextField.setText(dashArrayString(da));
    dashArrayUseDefault.setSelected(!attr_.isDashArrayOverridden());
    //
    // dash phase
    //
    dashPhaseTextField.setText(Float.toString(attr_.getDashPhase()));
    dashPhaseUseDefault.setSelected(!attr_.isDashPhaseOverridden());
    //
    // cap style
    //
    capStyleComboBox.setSelectedIndex(attr_.getCapStyle());
    capStyleUseDefault.setSelected(!attr_.isCapStyleOverridden());
    //
    // miter style
    //
    miterStyleComboBox.setSelectedIndex(attr_.getMiterStyle());
    miterStyleUseDefault.setSelected(!attr_.isMiterStyleOverridden());
    //
    // miter limit
    //
    miterLimitTextField.setText(Float.toString(attr_.getMiterLimit()));
    miterLimitUseDefault.setSelected(!attr_.isMiterLimitOverridden());
    //
    // Label attributes
    //
    // color
    //
    textColorPanel.setColor(attr_.getLabelColor());
    labelColorUseDefault.setSelected(!attr_.isLabelColorOverridden());
    //
    // font
    //
    labelFont_ = attr_.getLabelFont();
    if(labelFont_ == null) {
      fontLabel.setText("Default Font");
    } else {
      fontLabel.setText(fontString(labelFont_));
      fontLabel.setFont(labelFont_);
    }
    labelFontUseDefault.setSelected(!attr_.isLabelFontOverridden());
    //
    // heightP
    //
    heightTextField.setText(Double.toString(attr_.getLabelHeightP()));
    heightPUseDefault.setSelected(!attr_.isLabelHeightPOverridden());
    //
    // autoLabel
    //
    autoLabelCheckBox.setSelected(attr_.isAutoLabel());
    autoLabel();
    //
    // text
    //
    textTextField.setText(attr_.getLabelText());
    //
    // label format
    labelFormatTextField.setText(attr_.getLabelFormat());
    labelFormatUseDefault.setSelected(!attr_.isLabelFormatOverridden());
    //
    // enabled
    //
    labelEnabledCheckBox.setSelected(attr_.isLabelEnabled());
    labelEnabledUseDefault.setSelected(!attr_.isLabelEnabledOverridden());
    labelEnabled();
    //
    useDefaultAll();
  }

  private void autoLabel() {
    textTextField.setEnabled(!autoLabelCheckBox.isSelected());
  }

  private void labelEnabled() {
    boolean enabled = labelEnabledCheckBox.isSelected();
    textTextField.setEnabled(enabled);
    textColorPanel.setEnabled(enabled);
    fontLabel.setEnabled(enabled);
    labelFontUseDefault.setEnabled(enabled);
    fontEditor.setEnabled(enabled);
    heightTextField.setEnabled(enabled);
    heightPUseDefault.setEnabled(enabled);
    autoLabelCheckBox.setEnabled(enabled);
    labelFormatTextField.setEnabled(enabled);
    labelFormatUseDefault.setEnabled(enabled);
    //
    useDefaultAll();
  }

  private String dashArrayString(float[] da) {
    if(da == null) {
      return "null";
    }
    StringBuffer sbuf = new StringBuffer("{");
    for(int i=0; i < da.length; i++) {
      sbuf.append(Float.toString(da[i]) + ", ");
    }
    sbuf.setLength(sbuf.length()-2);
    sbuf.append("}");
    return sbuf.toString();
  }

  private float[] dashArray() {
    String arrayString = dashArrayTextField.getText();
    int start = arrayString.indexOf("{");
    int stop = arrayString.indexOf("}");
    String sub = arrayString.substring(start+1,stop);
    //
    // parse array
    //
    StringTokenizer token = new StringTokenizer(sub, ",");
    int index = 0;
    float[] array = new float[token.countTokens()];
    while(token.hasMoreTokens()) {
      array[index] = Float.valueOf(token.nextToken()).floatValue();
      index++;
    }
    return array;
  }

  void updateContourLineAttribute() {
    //
    // Line attributes
    //
    // Color
    //
    attr_.setColor(lineColorPanel.getColor());
    attr_.setColorOverridden(!colorUseDefault.isSelected());
    //
    // style
    //
    attr_.setStyle(lineStyleComboBox.getSelectedIndex());
    attr_.setStyleOverridden(!styleUseDefault.isSelected());
    //
    // width
    //
    attr_.setWidth(Float.valueOf(widthTextField.getText()).floatValue());
    attr_.setWidthOverridden(!widthUseDefault.isSelected());
    //
    // dash array
    //
    attr_.setDashArray(dashArray());
    attr_.setDashArrayOverridden(!dashArrayUseDefault.isSelected());
    //
    // dash phase
    //
    attr_.setDashPhase(Float.valueOf(dashPhaseTextField.getText()).floatValue());
    attr_.setDashPhaseOverridden(!dashPhaseUseDefault.isSelected());
    //
    // cap style
    //
    attr_.setCapStyle(capStyleComboBox.getSelectedIndex());
    attr_.setCapStyleOverridden(!capStyleUseDefault.isSelected());
    //
    // miter style
    //
    attr_.setMiterStyle(miterStyleComboBox.getSelectedIndex());
    attr_.setMiterStyleOverridden(!miterStyleUseDefault.isSelected());
    //
    // miter limit
    //
    attr_.setMiterLimit(Float.valueOf(miterLimitTextField.getText()).floatValue());
    attr_.setMiterLimitOverridden(!miterLimitUseDefault.isSelected());
    //
    // Label attributes
    //
    // color
    //
    attr_.setLabelColor(textColorPanel.getColor());
    attr_.setLabelColorOverridden(!labelColorUseDefault.isSelected());
    //
    // font
    //
    if(labelFont_ != null) attr_.setLabelFont(labelFont_);
    attr_.setLabelFontOverridden(!labelFontUseDefault.isSelected());
    //
    // heightP
    //
    attr_.setLabelHeightP(Double.valueOf(heightTextField.getText()).doubleValue());
    attr_.setLabelHeightPOverridden(!heightPUseDefault.isSelected());
    //
    // autoLabel
    //
    attr_.setAutoLabel(autoLabelCheckBox.isSelected());
    //
    // text
    //
    attr_.setLabelText(textTextField.getText());
    //
    // label format
    //
    attr_.setLabelFormat(labelFormatTextField.getText());
    attr_.setLabelFormatOverridden(!labelFormatUseDefault.isSelected());
    //
    // enabled
    //
    attr_.setLabelEnabled(labelEnabledCheckBox.isSelected());
    attr_.setLabelEnabledOverridden(!labelEnabledUseDefault.isSelected());
  }
  /**
   * Get the modified attribute
   */
  public ContourLineAttribute getContourLineAttribute() {
    return attr_;
  }
  /**
   * Dialog test.
   */
  public static void main(String[] args) {
    ContourLineAttribute attr = new ContourLineAttribute();
    ContourLineAttributeDialog la = new ContourLineAttributeDialog();
    la.setContourLineAttribute(attr);
    la.setTitle("Test ContourLineAttribute Dialog");
    la.setVisible(true);
  }

  void arrayEditor_actionPerformed(java.awt.event.ActionEvent event) {
    ArrayEditDialog aed = new ArrayEditDialog();
    Point loc = arrayEditor.getLocationOnScreen();
    aed.setLocation(loc.x, loc.y);
    aed.setArray(dashArray());
    int result = aed.showDialog();
    if(result == ArrayEditDialog.OK_RESPONSE) {
      dashArrayTextField.setText(dashArrayString(aed.getFloatArray()));
    }
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


  void autoLabelCheckBox_actionPerformed(java.awt.event.ActionEvent event) {
    autoLabel();
  }

  void labelEnabledCheckBox_actionPerformed(java.awt.event.ActionEvent event) {
    labelEnabled();
  }

  String fontString(Font font) {
    int style = (font.isBold()?1:0) + (font.isItalic()?2:0);
    return font.getName() + " " + styleNames_[style];
  }

  void lineStyleComboBox_actionPerformed(ActionEvent e) {
    int index = lineStyleComboBox.getSelectedIndex();
    strokePanelEnabled(index == LineAttribute.STROKE);
    if(index == LineAttribute.MARK || index == LineAttribute.MARK_LINE) {
      JOptionPane.showMessageDialog(this,
      "Line stytle MARK or MARK & SOLID not valid for ContourLine",
      "Illegal Line Style", JOptionPane.ERROR_MESSAGE);
      lineStyleComboBox.setSelectedIndex(attr_.getStyle());
    }
  }

  public void strokePanelEnabled(boolean enabled) {
    widthUseDefault.setEnabled(enabled);
    dashArrayUseDefault.setEnabled(enabled);
    dashArrayUseDefault.setEnabled(enabled);
    dashPhaseUseDefault.setEnabled(enabled);
    capStyleUseDefault.setEnabled(enabled);
    miterStyleUseDefault.setEnabled(enabled);
    miterLimitUseDefault.setEnabled(enabled);
    useDefault("width");
    useDefault("dashArray");
    useDefault("dashPhase");
    useDefault("capStyle");
    useDefault("miterStyle");
    useDefault("miterLimit");
  }
}

class ContourLineAttributeDialog_lineStyleComboBox_actionAdapter implements java.awt.event.ActionListener {
  ContourLineAttributeDialog adaptee;

  ContourLineAttributeDialog_lineStyleComboBox_actionAdapter(ContourLineAttributeDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.lineStyleComboBox_actionPerformed(e);
  }
}
