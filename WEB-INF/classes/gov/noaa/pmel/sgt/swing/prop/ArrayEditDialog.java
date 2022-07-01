/*
 * $Id: ArrayEditDialog.java,v 1.7 2001/02/08 00:29:38 dwd Exp $
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
import javax.swing.event.*;
import java.awt.*;
import java.util.Enumeration;
/**
 * This dialog accepts an array of float's then via a graphical
 * interface allows this array to be modified.  For example, this
 * dialog is used to edit the dash array of the <code>LineAttribute</code>.
 *
 * <p> Example of <code>ArrayEditDialog</code> use:
 * <pre>
 *
 * public float[] editArray(float[] inArray) {
 *   ArrayEditDialog aed = new ArrayEditDialog();
 *   aed.setTitle("ArrayEdit");
 *   aed.setArray(inArray);
 *   if(aed.showDialog() == ArrayEditDialog.CANCEL_RESPONSE) {
 *     return inArray;
 *   } else {
 *     return aed.getFloatArray();
 *   }
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2001/02/08 00:29:38 $
 * @since 2.0
 * @LineAttribute
 */
public class ArrayEditDialog extends JDialog implements ListSelectionListener {
  private DefaultListModel model_;
  private int result_;
  /** OK button was selected */
  public static int OK_RESPONSE = 1;
  /** Cancel button was selected */
  public static int CANCEL_RESPONSE = 2;
  /**
   * Constructor.
   */
  public ArrayEditDialog(Frame parent) {
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
    setSize(322,349);
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    getContentPane().add(buttonPanel, "South");
    okButton.setText("OK");
    okButton.setActionCommand("OK");
    buttonPanel.add(okButton);
    cancelButton.setText("Cancel");
    cancelButton.setActionCommand("Cancel");
    buttonPanel.add(cancelButton);
    //$$ etchedBorder1.move(48,396);
    mainPanel.setLayout(new GridBagLayout());
    getContentPane().add(mainPanel, "Center");
    mainPanel.add(JScrollPane1, new GridBagConstraints(0,0,1,3,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,5,5,5),155,0));
    arrayList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    JScrollPane1.getViewport().add(arrayList);
    arrayList.setBounds(0,0,173,295);
    JPanel2.setBorder(titledBorder2);
    JPanel2.setLayout(new GridBagLayout());
    mainPanel.add(JPanel2, new GridBagConstraints(1,0,1,1,0.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,5),0,0));
    JPanel2.add(editTextField,new GridBagConstraints(0,0,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,0,5),0,0));
    editButton.setToolTipText("Change value of selected element.");
    editButton.setText("Change Value");
    JPanel2.add(editButton,new GridBagConstraints(0,1,1,1,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0));
    JPanel3.setBorder(titledBorder3);
    JPanel3.setLayout(new GridBagLayout());
    mainPanel.add(JPanel3, new GridBagConstraints(1,2,1,1,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,0,0,0),0,0));
    deleteButton.setToolTipText("Delete selected element.");
    deleteButton.setText("Delete");
    JPanel3.add(deleteButton,new GridBagConstraints(0,0,1,1,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0));
    JPanel1.setBorder(titledBorder1);
    JPanel1.setLayout(new GridBagLayout());
    mainPanel.add(JPanel1, new GridBagConstraints(1,1,1,1,1.0,1.0,
    GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,0,0,0),0,0));
    JPanel1.add(insertTextField, new GridBagConstraints(0,0,1,1,1.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,0,5),0,0));
    beforeButton.setToolTipText("Insert new item before selected element.");
    beforeButton.setText("Before");
    JPanel1.add(beforeButton, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,0,5),0,0));
    afterButton.setToolTipText("Insert new item after selected element.");
    afterButton.setText("After");
    JPanel1.add(afterButton, new GridBagConstraints(0,2,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,5,5,5),0,0));
    setTitle("Edit Array");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    editButton.addActionListener(lSymAction);
    beforeButton.addActionListener(lSymAction);
    afterButton.addActionListener(lSymAction);
    deleteButton.addActionListener(lSymAction);

    arrayList.addListSelectionListener(this);
  }
  /**
   * Used internally.
   */
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
  public ArrayEditDialog(String title) {
    this();
    setTitle(title);
  }
  /** Default constructor */
  public ArrayEditDialog() {
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
      if (object == ArrayEditDialog.this)
  ArrayEditDialog_WindowClosing(event);
    }
  }

  void ArrayEditDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JPanel mainPanel = new javax.swing.JPanel();
  javax.swing.JScrollPane JScrollPane1 = new javax.swing.JScrollPane();
  javax.swing.JList arrayList = new javax.swing.JList();
  javax.swing.JPanel JPanel2 = new javax.swing.JPanel();
  javax.swing.JTextField editTextField = new javax.swing.JTextField();
  javax.swing.JButton editButton = new javax.swing.JButton();
  javax.swing.JPanel JPanel3 = new javax.swing.JPanel();
  javax.swing.JButton deleteButton = new javax.swing.JButton();
  javax.swing.JPanel JPanel1 = new javax.swing.JPanel();
  javax.swing.JTextField insertTextField = new javax.swing.JTextField();
  javax.swing.JButton beforeButton = new javax.swing.JButton();
  javax.swing.JButton afterButton = new javax.swing.JButton();
  javax.swing.border.TitledBorder titledBorder1 = new javax.swing.border.TitledBorder("Insert Element");
  javax.swing.border.TitledBorder titledBorder2 = new javax.swing.border.TitledBorder("Edit Element");
  javax.swing.border.TitledBorder titledBorder3 = new javax.swing.border.TitledBorder("Delete Element");


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
  cancelButton_actionPerformed(event);
      else if (object == okButton)
  okButton_actionPerformed(event);
      else if (object == editButton)
  editButton_actionPerformed(event);
      else if (object == beforeButton)
  beforeButton_actionPerformed(event);
      else if (object == afterButton)
  afterButton_actionPerformed(event);
      if (object == deleteButton)
  deleteButton_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
    result_ = CANCEL_RESPONSE;
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    result_ = OK_RESPONSE;
    this.setVisible(false);
  }
  /**
   * Test entry point.
   */
  public static void main(String[] args) {
    float[] array = {0.0f, 2.0f, 2.5f, 3.0f, 4.5f};
    ArrayEditDialog aed = new ArrayEditDialog();
    aed.setTitle("Test ArrayEdit Dialog");
    aed.setArray(array);
    if(aed.showDialog() == CANCEL_RESPONSE) {
      System.out.println("Dialog Cancelled");
    } else {
      float[] out = aed.getFloatArray();
      for(int i=0; i < out.length; i++) {
	System.out.println("x["+i+"] = " + out[i]);
      }
    }
    aed.setVisible(false);
    aed.dispose();
    System.exit(0);
  }
  /**
   * Show the dialog and wait for a response.
   *
   * @return CANCEL_RESPONSE or OK_RESPONSE
   */
  public int showDialog() {
    result_ = CANCEL_RESPONSE;
    setModal(true);
    super.setVisible(true);
    return result_;
  }
  /**
   * Initialize the array.
   */
  public void setArray(float[] array) {
    model_ = new DefaultListModel();
    for(int i=0; i < array.length; i++) {
      model_.addElement(Float.toString(array[i]));
    }
    arrayList.setModel(model_);
  }
  /**
   * Get the edited array.
   */
  public float[] getFloatArray() {
    float[] array = new float[model_.size()];
    Enumeration e = model_.elements();
    int index = 0;
    while(e.hasMoreElements()) {
      array[index] = Float.valueOf((String)e.nextElement()).floatValue();
      index++;
    }
    return array;
  }

  void editButton_actionPerformed(java.awt.event.ActionEvent event) {
    for(int i=0; i < model_.size(); i++) {
      if(arrayList.isSelectedIndex(i)) {
  model_.set(i, editTextField.getText());
  return;
      }
    }
  }

  void beforeButton_actionPerformed(java.awt.event.ActionEvent event) {
    for(int i=0; i < model_.size(); i++) {
      if(arrayList.isSelectedIndex(i)) {
  model_.insertElementAt(insertTextField.getText(), i);
  return;
      }
    }
  }

  void afterButton_actionPerformed(java.awt.event.ActionEvent event) {
    for(int i=0; i < model_.size(); i++) {
      if(arrayList.isSelectedIndex(i)) {
  model_.insertElementAt(insertTextField.getText(), i+1);
  return;
      }
    }
  }
  /**
   * Internal event listener
   */
  public void valueChanged(ListSelectionEvent e) {
    if(e.getValueIsAdjusting()) return;
    int first = e.getFirstIndex();
    int last = e.getLastIndex();
    for(int i=first; i <= last; i++) {
      if(arrayList.isSelectedIndex(i))
  editTextField.setText((String)model_.get(i));
    }
  }

  void deleteButton_actionPerformed(java.awt.event.ActionEvent event) {
    for(int i=0; i < model_.size(); i++) {
      if(arrayList.isSelectedIndex(i)) {
  model_.removeElementAt(i);
  return;
      }
    }
  }
}
