/*
 * $Id: LineAttributeDialog.java,v 1.18 2003/08/22 23:02:39 dwd Exp $
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

import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.swing.PlotMarkIcon;
import gov.noaa.pmel.swing.ThreeDotsButton;
import javax.swing.border.*;
import java.awt.event.*;

/**
 * Edits a <code>LineAttribute</code>. This dialog does not
 * make a copy of the attribute so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new properties unless
 * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching}
 * has been turned on.
 *
 * <p> Example of <code>LineAttributeDialog</code> use:
 * <pre>
 * public void editLineAttribute(LineAttribute attr) {
 *   LineAttributeDialog lad = new LineAttributeDialog();
 *   lad.setLineAttribute(attr);
 *   lad.setVisible(true);
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.18 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 * @see PlotMarkDialog
 * @see ArrayEditDialog
 */
public class LineAttributeDialog extends JDialog {
  private LineAttribute attr_;
  private PlotMarkIcon pmIcon_;
  private int mark_;
  private JPane[] paneList_ = null;
  /**
   * Constructor.
   */
  public LineAttributeDialog(Frame parent) {
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
    strokeBorder = new TitledBorder("");
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(500,490);
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    strokePanel.setBorder(strokeBorder);
    strokePanel.setLayout(gridBagLayout1);
    strokeBorder.setTitle("Stroke Line Attributes");
    JLabel10.setText(" Width:");
    widthTextField.setColumns(10);
    JLabel8.setText("Dash Array:");
    dashArrayPanel.setLayout(flowLayout1);
    arrayEditor.setActionCommand("...");
    JLabel9.setText("Dash Phase:");
    dashPhaseTextField.setColumns(10);
    jLabel1.setText("Cap Style:");
    jLabel2.setText("Miter Style:");
    jLabel3.setText("Miter Limit:");
    miterLimitTextField.setColumns(10);
    capStyleComboBox.setModel(capStyleCBM);
    miterStyleComboBox.setModel(miterStyleCBM);
    lineStyleComboBox.addActionListener(new LineAttributeDialog_lineStyleComboBox_actionAdapter(this));
    dashArrayTextField.setColumns(28);
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
    mainPanel.setLayout(new GridBagLayout());
    getContentPane().add(mainPanel, "Center");
    mainPanel.setBounds(0,0,430,263);
    JLabel1.setText("Color:");
    mainPanel.add(JLabel1,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 15, 5, 5), 0, 0));
    JLabel1.setBounds(65,43,33,15);
    colorPanel.setBorder(etchedBorder1);
    mainPanel.add(colorPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(15, 5, 5, 15), 0, 0));
    colorPanel.setBounds(108,29,295,39);
    JLabel5.setText("Line Style:");
    mainPanel.add(JLabel5,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 15, 5, 5), 0, 0));
    JLabel5.setBounds(40,80,58,15);
    lineStyleComboBox.setModel(lineStyleCBM);
    mainPanel.add(lineStyleComboBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 15), 0, 0));
    lineStyleComboBox.setBounds(108,73,92,24);
    JLabel6.setText("Mark:");
    mainPanel.add(JLabel6,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 15, 5, 5), 0, 0));
    JLabel6.setBounds(66,133,32,15);
    markPanel.setLayout(flowLayout2);
    mainPanel.add(markPanel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 15), 0, 0));
    markPanel.setBounds(108,126,48,25);
    markPanel.add(plotMarkIconLabel, null);
    plotMarkIconLabel.setForeground(java.awt.Color.black);
    plotMarkIconLabel.setBounds(0,0,0,0);
    markEditor.setActionCommand("...");
    markPanel.add(markEditor, null);
    JLabel7.setText("MarkHeight:");
    mainPanel.add(JLabel7,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 22, 5, 5), 0, 0));
    JLabel7.setBounds(30,160,68,15);
    markHeightTextField.setColumns(10);
    mainPanel.add(markHeightTextField, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 5, 5, 15), 0, 0));
    markHeightTextField.setBounds(108,156,110,19);
    mainPanel.add(strokePanel, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 10, 15, 10), 0, 0));
    strokePanel.add(JLabel10,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(widthTextField,   new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(JLabel8,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(dashArrayPanel,   new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 5), 0, 0));
    dashArrayPanel.add(dashArrayTextField, null);
    dashArrayPanel.add(arrayEditor, null);
    strokePanel.add(JLabel9,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(dashPhaseTextField,  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(jLabel1,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(jLabel2,  new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(capStyleComboBox,  new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(miterStyleComboBox,  new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    strokePanel.add(jLabel3,  new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    strokePanel.add(miterLimitTextField,  new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    //$$ stringComboBoxModel1.move(24,300);
    lineStyleComboBox.setSelectedIndex(0);
    setTitle("LineAttribute Properties");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);
    markEditor.addActionListener(lSymAction);
    arrayEditor.addActionListener(lSymAction);

    //
    pmIcon_ = new PlotMarkIcon(1);
    plotMarkIconLabel.setIcon(pmIcon_);
    //
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
  public LineAttributeDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public LineAttributeDialog() {
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
      if (object == LineAttributeDialog.this)
  LineAttributeDialog_WindowClosing(event);
    }
  }

  void LineAttributeDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JPanel mainPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel1 = new javax.swing.JLabel();
  ColorEntryPanel colorPanel = new ColorEntryPanel();
  javax.swing.JLabel JLabel5 = new javax.swing.JLabel();
  javax.swing.JComboBox lineStyleComboBox = new javax.swing.JComboBox();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JPanel markPanel = new javax.swing.JPanel();
  javax.swing.JLabel plotMarkIconLabel = new javax.swing.JLabel();
  ThreeDotsButton markEditor = new ThreeDotsButton();
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JTextField markHeightTextField = new javax.swing.JTextField();
  DefaultComboBoxModel lineStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel capStyleCBM = new DefaultComboBoxModel();
  DefaultComboBoxModel miterStyleCBM = new DefaultComboBoxModel();
  JPanel strokePanel = new JPanel();
  TitledBorder strokeBorder;
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JLabel JLabel10 = new javax.swing.JLabel();
  JTextField widthTextField = new javax.swing.JTextField();
  JLabel JLabel8 = new javax.swing.JLabel();
  JTextField dashArrayTextField = new javax.swing.JTextField();
  JPanel dashArrayPanel = new javax.swing.JPanel();
  ThreeDotsButton arrayEditor = new ThreeDotsButton();
  JLabel JLabel9 = new javax.swing.JLabel();
  JTextField dashPhaseTextField = new javax.swing.JTextField();
  JLabel jLabel1 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JComboBox capStyleComboBox = new JComboBox();
  JComboBox miterStyleComboBox = new JComboBox();
  JLabel jLabel3 = new JLabel();
  JTextField miterLimitTextField = new JTextField();
  private FlowLayout flowLayout1 = new FlowLayout();
  private FlowLayout flowLayout2 = new FlowLayout();


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
      else if (object == arrayEditor)
        arrayEditor_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateLineAttribute();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateLineAttribute();
  }
  /**
   * Set the <code>LineAttribute</code> for the dialog.
   */
  public void setLineAttribute(LineAttribute attr) {
    attr_ = attr;
    //
    // Color
    //
    colorPanel.setColor(attr_.getColor());
    //
    // style
    //
    lineStyleComboBox.setSelectedIndex(attr_.getStyle());
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
    // Stroke line attributes
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
    // test for stroke panel enabled
    //
    strokePanelEnabled(lineStyleComboBox.getSelectedIndex() == LineAttribute.STROKE);
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

  void updateLineAttribute() {
    if(paneList_ != null) {
      for(int i=0; i < paneList_.length; i++) {
        paneList_[i].setBatch(true, "LineAttributeDialog");
      }
    }
    attr_.setBatch(true);
    //
    // Color
    //
    attr_.setColor(colorPanel.getColor());
    //
    // style
    //
    attr_.setStyle(lineStyleComboBox.getSelectedIndex());
    //
    // mark
    //
    attr_.setMark(mark_);
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

    attr_.setBatch(false);
    //
    if(paneList_ != null) {
      for(int i=0; i < paneList_.length; i++) {
        paneList_[i].setBatch(false, "LineAttributeDialog");
      }
    }
  }
  /**
   * Dialog test entry.
   */
  public static void main(String[] args) {
    LineAttribute attr = new LineAttribute();
    LineAttributeDialog la = new LineAttributeDialog();
    la.setLineAttribute(attr);
    la.setTitle("Test LineAttribute Dialog");
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

  void lineStyleComboBox_actionPerformed(ActionEvent e) {
    strokePanelEnabled(lineStyleComboBox.getSelectedIndex() == LineAttribute.STROKE);
  }

  void strokePanelEnabled(boolean enabled) {
    widthTextField.setEnabled(enabled);
    dashArrayTextField.setEnabled(enabled);
    arrayEditor.setEnabled(enabled);
    dashPhaseTextField.setEnabled(enabled);
    capStyleComboBox.setEnabled(enabled);
    miterStyleComboBox.setEnabled(enabled);
    miterLimitTextField.setEnabled(enabled);
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
   * <code>JPane</code> is often used for a <code>LineKey</code>.
   */
  public void setJPaneList(JPane[] list) {
    paneList_ = list;
  }
  /** Get an array of parent panes. */
  public JPane[] getJPaneList() {
    return paneList_;
  }
}

class LineAttributeDialog_lineStyleComboBox_actionAdapter implements java.awt.event.ActionListener {
  LineAttributeDialog adaptee;

  LineAttributeDialog_lineStyleComboBox_actionAdapter(LineAttributeDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.lineStyleComboBox_actionPerformed(e);
  }
}
