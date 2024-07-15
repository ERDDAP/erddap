/*
 * $Id: PlotMarkDialog.java,v 1.7 2003/09/17 20:32:10 dwd Exp $
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

import gov.noaa.pmel.sgt.swing.PlotMarkIcon;

/**
 * Provides a dialog to graphically select a <code>PlotMark</code>
 * code.
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/09/17 20:32:10 $
 * @since 2.0
 */
public class PlotMarkDialog extends JDialog {
  private static final int numMarks = 51;
  private int result_;
  private int mark_;
  private JToggleButton[] buttons_ = new JToggleButton[numMarks];
  /** OK button selected */
  public static int OK_RESPONSE = 1;
  /** Cancel button selected */
  public static int CANCEL_RESPONSE = 2;
  /**
   * Constructor
   */
  public PlotMarkDialog(Frame parent) {
    super(parent);
    try {
      jbInit();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  void jbInit() throws Exception {
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(288,201);
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    getContentPane().add(buttonPanel, "South");
    buttonPanel.setBounds(0,162,288,39);
    okButton.setText("OK");
    okButton.setActionCommand("OK");
    buttonPanel.add(okButton);
    okButton.setBounds(79,7,51,25);
    cancelButton.setText("Cancel");
    cancelButton.setActionCommand("Cancel");
    buttonPanel.add(cancelButton);
    cancelButton.setBounds(135,7,73,25);
    //$$ etchedBorder1.move(0,300);
    mainPanel.setLayout(new GridLayout(4,11,0,0));
    getContentPane().add(mainPanel, "Center");
    mainPanel.setBounds(0,0,288,162);
    setTitle("Select a Mark");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
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
  public PlotMarkDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor
   */
  public PlotMarkDialog() {
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
      if (object == PlotMarkDialog.this)
        PlotMarkDialog_WindowClosing(event);
    }
  }

  void PlotMarkDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JPanel mainPanel = new javax.swing.JPanel();

  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
        cancelButton_actionPerformed(event);
      else if (object == okButton)
        okButton_actionPerformed(event);


    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
    result_ = CANCEL_RESPONSE;
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    result_ = OK_RESPONSE;
    for(int i=0; i < numMarks; i++) {
      if(buttons_[i].isSelected()) {
        mark_ = i+1;
      }
    }
    this.setVisible(false);
  }
  /**
   * Test entry point
   */
  public static void main(String[] args) {
    PlotMarkDialog la = new PlotMarkDialog();
    la.setTitle("Test PlotMark Dialog");
    la.setVisible(true);
  }
  /**
   * Show the dialog and wait for a response
   *
   * @param mark initial <code>PlotMark</code> code
   * @return result, either CANCEL_RESPONSE or OK_RESPONSE
   */
  public int showDialog(int mark) {
    mark_ = mark;
    createButtons();
    result_ = CANCEL_RESPONSE;
    setModal(true);
    super.setVisible(true);
    return result_;
  }
  /**
   * Set initial mark.
   */
  public void setMark(int mark) {
    mark_ = mark;
  }
  /**
   * Get the selected <code>PlotMark</code> code.
   */
  public int getMark() {
    return mark_;
  }

  private void createButtons() {
    PlotMarkIcon pmi;
    ButtonGroup group = new ButtonGroup();
    for(int i=0; i < numMarks; i++) {
      pmi = new PlotMarkIcon(i+1);
      buttons_[i] = new JToggleButton(pmi);
      buttons_[i].setName(Integer.toString(i+1));
      group.add(buttons_[i]);
      mainPanel.add(buttons_[i]);
    }
    buttons_[mark_-1].setSelected(true);
  }


}
