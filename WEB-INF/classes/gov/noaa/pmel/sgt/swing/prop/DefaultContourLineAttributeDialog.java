/*
 * $Id: DefaultContourLineAttributeDialog.java,v 1.8 2003/08/22 23:02:39 dwd Exp $
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

import gov.noaa.pmel.sgt.DefaultContourLineAttribute;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.swing.ThreeDotsButton;
import javax.swing.border.*;
import java.awt.event.*;

/**
 * Edits a <code>DefaultContourLineAttribute</code>.  This dialog does not
 * make a copy of the attribute so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new properties unless
 * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching}
 * has been turned on.
 *
 * <p> Example of <code>DefaultContourLineAttributeDialog</code> use:
 * <pre>
 * public void editCLAttribute(DefaultContourLineAttribute cla) {
 *   DefaultContourLineAttributeDialog clad =
 *                        new DefaultContourLineAttributeDialog();
 *   clad.showDialog(cla);
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 */
public class DefaultContourLineAttributeDialog extends JDialog {
  private DefaultContourLineAttribute attr_;
  private Font labelFont_;
  private int result_;
  private String[] styleNames_ = {"plain", "bold", "italic", "bold-italic"};
  /** OK button was selected */
  public static int OK_RESPONSE = 1;
  /** Cancel button was selected */
  public static int CANCEL_RESPONSE = 2;
  /**
   * Construtor.
   */
  public DefaultContourLineAttributeDialog(Frame parent) {
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
    setSize(new Dimension(526, 362));
    setVisible(false);
    miterLimitTextField.setColumns(10);
    dashArrayPanel.setLayout(new GridBagLayout());
    strokePanel.setLayout(gridBagLayout1);
    arrayEditor.setActionCommand("...");
    arrayEditor.setToolTipText("Edit dash array.");
    JLabel28.setText("Dash Array:");
    JLabel27.setText("Dash Phase:");
    dashPhaseTextField.setColumns(10);
    jLabel3.setText("Miter Limit:");
    jLabel2.setText("Miter Style:");
    jLabel1.setText("Cap Style:");
    JLabel110.setText(" Width:");
    widthTextField.setColumns(10);
    strokePanel.setBorder(titledBorder1);
    lineStyleComboBox.addActionListener(new DefaultContourLineAttributeDialog_lineStyleComboBox_actionAdapter(this));
    capStyleComboBox.setModel(capStyleCBM);
    miterStyleComboBox.setModel(miterStyleCBM);
    getContentPane().add(TabbedPane, "Center");
    linePanel.setLayout(new GridBagLayout());
    TabbedPane.add(linePanel, "linePanel");
    JLabel1.setText("Color:");
    linePanel.add(JLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    lineColorPanel.setBorder(etchedBorder1);
    lineColorPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    linePanel.add(lineColorPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    JLabel5.setText("Line Style:");
    linePanel.add(JLabel5, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 25, 0, 5), 0, 0));
    lineStyleComboBox.setModel(lineStyleCBM);
    linePanel.add(lineStyleComboBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    linePanel.add(strokePanel, new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
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
    strokePanel.add(jLabel1, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(jLabel2, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(jLabel3, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    strokePanel.add(capStyleComboBox, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(miterStyleComboBox, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    strokePanel.add(miterLimitTextField, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    labelPanel.setLayout(new GridBagLayout());
    TabbedPane.add(labelPanel, "labelPanel");
    labelPanel.setBounds(2,27,425,258);
    labelPanel.setVisible(false);
    JLabel7.setText("Enabled:");
    labelPanel.add(JLabel7, new GridBagConstraints(0,6,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelEnabledCheckBox.setSelected(true);
    labelPanel.add(labelEnabledCheckBox, new GridBagConstraints(1,6,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
    JLabel11.setText("Color:");
    labelPanel.add(JLabel11, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    textColorPanel.setBorder(etchedBorder1);
    textColorPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(textColorPanel, new GridBagConstraints(1,1,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    JLabel15.setText("Font:");
    labelPanel.add(JLabel15, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    fontPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    labelPanel.add(fontPanel, new GridBagConstraints(1,2,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    fontLabel.setText("Dialog, 12, Bold");
    fontPanel.add(fontLabel);
    fontLabel.setForeground(java.awt.Color.black);
    fontEditor.setToolTipText("Edit font.");
    fontEditor.setActionCommand("...");
    fontPanel.add(fontEditor);
    JLabel16.setText("HeightP:");
    labelPanel.add(JLabel16, new GridBagConstraints(0,3,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    heightTextField.setColumns(10);
    labelPanel.add(heightTextField, new GridBagConstraints(1,3,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel6.setText("Significant Digits:");
    labelPanel.add(JLabel6, new GridBagConstraints(0,4,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    sigDigitsTextField.setColumns(10);
    labelPanel.add(sigDigitsTextField, new GridBagConstraints(1,4,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
    JLabel17.setText("Format:");
    labelPanel.add(JLabel17, new GridBagConstraints(0,5,1,1,0.0,0.0,
    GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    labelFormatTextField.setColumns(10);
    labelPanel.add(labelFormatTextField, new GridBagConstraints(1,5,1,1,0.0,0.0,
    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,5,5),0,0));
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
    //$$ etchedBorder1.move(0,324);
    linePanel.setBounds(2,27,425,258);
    linePanel.setVisible(false);
    TabbedPane.setTitleAt(0,"Line");
    TabbedPane.setTitleAt(1,"Label");

    lineStyleComboBox.setSelectedIndex(0);
    setTitle("DefaultContourLineAttribute Properties");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);
    fontEditor.addActionListener(lSymAction);
    labelEnabledCheckBox.addActionListener(lSymAction);
    arrayEditor.addActionListener(lSymAction);

//    Insets pup = new Insets(0, 0, 0, 0);
//    fontEditor.setMargin(pup);
//    arrayEditor.setMargin(pup);

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
  public DefaultContourLineAttributeDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor
   */
  public DefaultContourLineAttributeDialog() {
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
      if (object == DefaultContourLineAttributeDialog.this)
  DefaultContourLineAttributeDialog_WindowClosing(event);
    }
  }

  void DefaultContourLineAttributeDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JTabbedPane TabbedPane = new javax.swing.JTabbedPane();
  javax.swing.JPanel linePanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel1 = new javax.swing.JLabel();
  ColorEntryPanel lineColorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel5 = new javax.swing.JLabel();
  javax.swing.JComboBox lineStyleComboBox = new javax.swing.JComboBox();
  javax.swing.JPanel labelPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JCheckBox labelEnabledCheckBox = new javax.swing.JCheckBox();
  javax.swing.JLabel JLabel11 = new javax.swing.JLabel();
  ColorEntryPanel textColorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel15 = new javax.swing.JLabel();
  javax.swing.JPanel fontPanel = new javax.swing.JPanel();
  javax.swing.JLabel fontLabel = new javax.swing.JLabel();
  ThreeDotsButton fontEditor = new ThreeDotsButton();
  javax.swing.JLabel JLabel16 = new javax.swing.JLabel();
  javax.swing.JTextField heightTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JTextField sigDigitsTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel17 = new javax.swing.JLabel();
  javax.swing.JTextField labelFormatTextField = new javax.swing.JTextField();
  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  DefaultComboBoxModel lineStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel capStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel miterStyleCBM = new DefaultComboBoxModel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JTextField miterLimitTextField = new JTextField();
  JPanel dashArrayPanel = new javax.swing.JPanel();
  JPanel strokePanel = new JPanel();
  ThreeDotsButton arrayEditor = new ThreeDotsButton();
  JLabel JLabel28 = new javax.swing.JLabel();
  JLabel JLabel27 = new javax.swing.JLabel();
  JTextField dashArrayTextField = new javax.swing.JTextField();
  JTextField dashPhaseTextField = new javax.swing.JTextField();
  JComboBox miterStyleComboBox = new JComboBox();
  JLabel jLabel3 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JLabel jLabel1 = new JLabel();
  JLabel JLabel110 = new javax.swing.JLabel();
  JComboBox capStyleComboBox = new JComboBox();
  JTextField widthTextField = new javax.swing.JTextField();
  TitledBorder titledBorder1;


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
        cancelButton_actionPerformed(event);
      else if (object == okButton)
        okButton_actionPerformed(event);
      else if (object == applyButton)
        applyButton_actionPerformed(event);
      else if (object == arrayEditor)
        arrayEditor_actionPerformed(event);
      else if (object == fontEditor)
        fontEditor_actionPerformed(event);
      else if (object == labelEnabledCheckBox)
        labelEnabledCheckBox_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
    result_ = CANCEL_RESPONSE;
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    result_ = OK_RESPONSE;
    updateDefaultContourLineAttribute();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    result_ = OK_RESPONSE;
    updateDefaultContourLineAttribute();
  }
  /**
   * Show the dialog and wait for a response
   */
  public int showDialog(DefaultContourLineAttribute attr) {
    setDefaultContourLineAttribute(attr);
    result_ = CANCEL_RESPONSE;
    setModal(true);
    super.setVisible(true);
    return result_;
  }
  /**
   * Set the <code>DefaultContourLineAttribute</code> to be edited.
   */
  public void setDefaultContourLineAttribute(DefaultContourLineAttribute attr) {
    attr_ = attr;
    attr_.setContourLineAttribute(null);
    //
    // Line attributes
    //
    // Color
    //
    lineColorPanel.setColor(attr_.getColor());
    //
    // style
    //
    lineStyleComboBox.setSelectedIndex(attr_.getStyle());
    //
    // width
    //
    widthTextField.setText(Float.toString(attr_.getWidth()));
    //
    // dash Array
    //
    float[] da = attr_.getDashArray();
    dashArrayTextField.setText(dashArrayString(da));
    //
    // dash phase
    //
    dashPhaseTextField.setText(Float.toString(attr_.getDashPhase()));
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
    //
    // Label attributes
    //
    // color
    //
    textColorPanel.setColor(attr_.getLabelColor());
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
    //
    // heightP
    //
    heightTextField.setText(Double.toString(attr_.getLabelHeightP()));
    //
    // Significant Digits
    //
    sigDigitsTextField.setText(Integer.toString(attr_.getSignificantDigits()));
    //
    // format
    //
    labelFormatTextField.setText(attr_.getLabelFormat());
    //
    // enabled
    //
    labelEnabledCheckBox.setSelected(attr_.isLabelEnabled());
    labelEnabled();
  }

  private void labelEnabled() {
    boolean enabled = labelEnabledCheckBox.isSelected();
    textColorPanel.setEnabled(enabled);
    fontLabel.setEnabled(enabled);
    heightTextField.setEnabled(enabled);
    labelFormatTextField.setEnabled(enabled);
    sigDigitsTextField.setEnabled(enabled);
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

  void updateDefaultContourLineAttribute() {
    //
    // Line attributes
    //
    // Color
    //
    attr_.setColor(lineColorPanel.getColor());
    //
    // style
    //
    attr_.setStyle(lineStyleComboBox.getSelectedIndex());
    //
    // width
    //
    attr_.setWidth(Float.valueOf(widthTextField.getText()).floatValue());
    //
    // dash array
    //
    attr_.setDashArray(dashArray());
    //
    // dash phase
    //
    attr_.setDashPhase(Float.valueOf(dashPhaseTextField.getText()).floatValue());
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
    //
    // Label attributes
    //
    // enabled
    //
    attr_.setLabelEnabled(labelEnabledCheckBox.isSelected());
    //
    // color
    //
    attr_.setLabelColor(textColorPanel.getColor());
    //
    // font
    //
    if(labelFont_ != null) attr_.setLabelFont(labelFont_);
    //
    // heightP
    //
    attr_.setLabelHeightP(Double.valueOf(heightTextField.getText()).doubleValue());
    //
    // label format
    //
    attr_.setLabelFormat(labelFormatTextField.getText());
    //
    // significant digits
    //
    attr_.setSignificantDigits(Integer.valueOf(sigDigitsTextField.getText()).intValue());
  }
  /**
   * Get the modified attribute.
   */
  public DefaultContourLineAttribute getDefaultContourLineAttribute() {
    return attr_;
  }
  /**
   * Test entry point
   */
  public static void main(String[] args) {
    DefaultContourLineAttribute attr = new DefaultContourLineAttribute();
    DefaultContourLineAttributeDialog la = new DefaultContourLineAttributeDialog();
    la.setDefaultContourLineAttribute(attr);
    la.setTitle("Test DefaultContourLineAttribute Dialog");
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
    widthTextField.setEnabled(enabled);
    dashArrayTextField.setEnabled(enabled);
    arrayEditor.setEnabled(enabled);
    dashPhaseTextField.setEnabled(enabled);
    capStyleComboBox.setEnabled(enabled);
    miterStyleComboBox.setEnabled(enabled);
    miterLimitTextField.setEnabled(enabled);
  }
}

class DefaultContourLineAttributeDialog_lineStyleComboBox_actionAdapter implements java.awt.event.ActionListener {
  DefaultContourLineAttributeDialog adaptee;

  DefaultContourLineAttributeDialog_lineStyleComboBox_actionAdapter(DefaultContourLineAttributeDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.lineStyleComboBox_actionPerformed(e);
  }
}
