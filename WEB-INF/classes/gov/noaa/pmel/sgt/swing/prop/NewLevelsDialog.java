/*
 * $Id: NewLevelsDialog.java,v 1.4 2001/02/08 00:29:39 dwd Exp $
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

import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.sgt.Graph;

import javax.swing.*;
import java.awt.*;

import gov.noaa.pmel.swing.JSlider2Double;

/**
 * Computes the range for creating a <code>ContourLevels</code>
 * object. If a <code>SGTGrid</code> object is provided
 * <code>NewLevelsDialog</code> can use the actual data range to help
 * in creating the contour levels.
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2001/02/08 00:29:39 $
 * @since 2.0
 */
public class NewLevelsDialog extends JDialog {
  private int result_;
  private SGTGrid grid_ = null;
  private boolean useSpacing_ = false;
  private boolean rangeComputed_ = false;
  /** OK button selected */
  public static int OK_RESPONSE = 1;
  /** Cancel button selected */
  public static int CANCEL_RESPONSE = 2;
  /**
   * Constructor.
   */
  public NewLevelsDialog(Frame parent) {
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
    setSize(282,366);
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
    //$$ etchedBorder1.move(0,444);
    mainPanel.setLayout(new GridBagLayout());
    getContentPane().add(mainPanel, "Center");
    mainPanel.setBackground(new java.awt.Color(204,204,204));
    JPanel1.setBorder(titledBorder1);
    JPanel1.setLayout(new GridBagLayout());
    mainPanel.add(JPanel1, new GridBagConstraints(0,0,1,1,0.0,0.0,
						  GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0));
    JLabel1.setText("Minimum:");
    JPanel1.add(JLabel1,new GridBagConstraints(0,0,1,1,0.0,0.0,
					       GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,10,5,2),0,0));
    minTextField.setColumns(15);
    JPanel1.add(minTextField,new GridBagConstraints(1,0,1,1,1.0,1.0,
						    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,5,10),0,0));
    JLabel2.setText("Maximum:");
    JPanel1.add(JLabel2,new GridBagConstraints(0,1,1,1,0.0,0.0,
					       GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,5,5,2),0,0));
    maxTextField.setColumns(15);
    JPanel1.add(maxTextField,new GridBagConstraints(1,1,1,1,1.0,1.0,
						    GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,5,10),0,0));
    JPanel3.setBorder(titledBorder3);
    JPanel3.setLayout(new GridBagLayout());
    mainPanel.add(JPanel3, new GridBagConstraints(0,1,1,1,0.0,0.0,
						  GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,5,0,5),0,0));
    JLabel3.setText("Number:");
    JPanel3.add(JLabel3,new GridBagConstraints(0,0,1,1,0.0,0.0,
					       GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,10,0,2),0,0));
    nlevelsTextField.setColumns(5);
    JPanel3.add(nlevelsTextField,new GridBagConstraints(1,0,1,1,0.0,1.0,
							GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,0,10),0,0));
    JLabel5.setText("or");
    JPanel3.add(JLabel5,new GridBagConstraints(0,1,1,1,0.0,0.0,
					       GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,5),0,0));
    JLabel5.setBackground(new java.awt.Color(204,204,204));
    JLabel5.setForeground(new java.awt.Color(102,102,153));
    JLabel4.setText("Spacing:");
    JPanel3.add(JLabel4,new GridBagConstraints(0,2,1,1,0.0,0.0,
					       GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,10,5,2),0,0));
    spacingTextField.setColumns(15);
    JPanel3.add(spacingTextField,new GridBagConstraints(1,2,1,1,0.0,1.0,
							GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,5,10),0,0));
    JPanel4.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    mainPanel.add(JPanel4, new GridBagConstraints(0,2,1,1,0.0,0.0,
						  GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
    computeButton.setText("Compute \"nice\" Range");
    JPanel4.add(computeButton);
    JPanel2.setBorder(titledBorder2);
    JPanel2.setLayout(new GridBagLayout());
    mainPanel.add(JPanel2, new GridBagConstraints(0,3,1,1,0.0,0.0,
						  GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
    JLabel6.setText("Minimum:");
    JPanel2.add(JLabel6,new GridBagConstraints(0,0,1,1,0.0,0.0,
					       GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,10,5,2),0,0));
    minCompTextField.setEditable(false);
    minCompTextField.setColumns(15);
    minCompTextField.setEnabled(false);
    JPanel2.add(minCompTextField,new GridBagConstraints(1,0,1,1,1.0,1.0,
							GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,5,10),0,0));
    JLabel7.setText("Minimum:");
    JPanel2.add(JLabel7,new GridBagConstraints(0,1,1,1,0.0,0.0,
					       GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,5,5,2),0,0));
    maxCompTextField.setEditable(false);
    maxCompTextField.setColumns(15);
    maxCompTextField.setEnabled(false);
    JPanel2.add(maxCompTextField,new GridBagConstraints(1,1,1,1,0.0,1.0,
							GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,0,5,10),0,0));

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    minTextField.addActionListener(lSymAction);
    maxTextField.addActionListener(lSymAction);
    nlevelsTextField.addActionListener(lSymAction);
    spacingTextField.addActionListener(lSymAction);
    SymPropertyChange lSymPropertyChange = new SymPropertyChange();
    computeButton.addActionListener(lSymAction);
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
  public NewLevelsDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public NewLevelsDialog() {
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
      if (object == NewLevelsDialog.this)
	NewLevelsDialog_WindowClosing(event);
    }
  }

  void NewLevelsDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JPanel mainPanel = new javax.swing.JPanel();
  javax.swing.JPanel JPanel1 = new javax.swing.JPanel();
  javax.swing.JLabel JLabel1 = new javax.swing.JLabel();
  javax.swing.JTextField minTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel2 = new javax.swing.JLabel();
  javax.swing.JTextField maxTextField = new javax.swing.JTextField();
  javax.swing.JPanel JPanel3 = new javax.swing.JPanel();
  javax.swing.JLabel JLabel3 = new javax.swing.JLabel();
  javax.swing.JTextField nlevelsTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel5 = new javax.swing.JLabel();
  javax.swing.JLabel JLabel4 = new javax.swing.JLabel();
  javax.swing.JTextField spacingTextField = new javax.swing.JTextField();
  javax.swing.JPanel JPanel4 = new javax.swing.JPanel();
  javax.swing.JButton computeButton = new javax.swing.JButton();
  javax.swing.JPanel JPanel2 = new javax.swing.JPanel();
  javax.swing.JLabel JLabel6 = new javax.swing.JLabel();
  javax.swing.JTextField minCompTextField = new javax.swing.JTextField();
  javax.swing.JLabel JLabel7 = new javax.swing.JLabel();
  javax.swing.JTextField maxCompTextField = new javax.swing.JTextField();
  javax.swing.border.TitledBorder titledBorder1 = new javax.swing.border.TitledBorder("Data Range");
  javax.swing.border.TitledBorder titledBorder3 = new javax.swing.border.TitledBorder("Select");
  javax.swing.border.TitledBorder titledBorder2 = new javax.swing.border.TitledBorder("Computed Range");


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
	cancelButton_actionPerformed(event);
      else if (object == okButton)
	okButton_actionPerformed(event);
      else if (object == minTextField)
	minTextField_actionPerformed(event);
      else if (object == maxTextField)
	maxTextField_actionPerformed(event);
      else if (object == nlevelsTextField)
	nlevelsTextField_actionPerformed(event);
      else if (object == spacingTextField)
	spacingTextField_actionPerformed(event);
      else if (object == computeButton)
        computeButton_actionPerformed(event);
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
   * Test entry point
   */
  public static void main(String[] args) {
    NewLevelsDialog la = new NewLevelsDialog();
    la.setTitle("Test New Levels Dialog");
    la.setVisible(true);
  }
  /**
   * Show the dialog and wait for a response.
   *
   * @param grid the data grid
   * @return result, either CANCEL_RESPONSE or OK_RESPONSE
   */
  public int showDialog(SGTGrid grid) {
    setGrid(grid);
    result_ = CANCEL_RESPONSE;
    setModal(true);
    super.setVisible(true);
    return result_;
  }
  /**
   * Return the computed or entered range.
   */
  public Range2D getRange() {
    double min;
    double max;
    if(rangeComputed_) {
      min = (Double.valueOf(minCompTextField.getText())).doubleValue();
      max = (Double.valueOf(maxCompTextField.getText())).doubleValue();
    } else {
      min = (Double.valueOf(minTextField.getText())).doubleValue();
      max = (Double.valueOf(maxTextField.getText())).doubleValue();
    }
    double delta = (Double.valueOf(spacingTextField.getText())).doubleValue();
    Range2D range = new Range2D(min, max, delta);
    return range;
  }
  /**
   * Set the data grid for the computed range.
   */
  public void setGrid(SGTGrid grid) {
    double zmin, zmax;
    grid_ = grid;
    if(grid_ == null) {
      titledBorder1.setTitle("Enter Range");
      zmin = 0.0;
      zmax = 10.0;
    } else {
      zmin = Double.MAX_VALUE;
      zmax = -Double.MAX_VALUE;
      double[] z = grid_.getZArray();
      for(int i=0; i < z.length; i++) {
	if(Double.isNaN(z[i])) continue;
	zmin = Math.min(zmin, z[i]);
	zmax = Math.max(zmax, z[i]);
      }
    }
    minTextField.setText(Double.toString(zmin));
    maxTextField.setText(Double.toString(zmax));
    useSpacing_ = false;
    nlevelsTextField.setText("10");
    updateLevSpac();
  }

  private void updateLevSpac() {
    int levels;
    double delta = (Double.valueOf(maxTextField.getText())).doubleValue() -
      (Double.valueOf(minTextField.getText())).doubleValue();
    double spacing;
    if(useSpacing_) {
      levels = (int)(delta/(Double.valueOf(spacingTextField.getText())).doubleValue());
      nlevelsTextField.setText(Integer.toString(levels));
    } else {
      spacing = delta/(Integer.valueOf(nlevelsTextField.getText())).intValue();
      spacingTextField.setText(Double.toString(spacing));
    }
  }

  void minTextField_actionPerformed(java.awt.event.ActionEvent event) {
    updateLevSpac();
  }

  void maxTextField_actionPerformed(java.awt.event.ActionEvent event) {
    updateLevSpac();
  }

  void nlevelsTextField_actionPerformed(java.awt.event.ActionEvent event) {
    useSpacing_ = false;
    updateLevSpac();
  }

  void spacingTextField_actionPerformed(java.awt.event.ActionEvent event) {
    useSpacing_ = true;
    updateLevSpac();
  }

  class SymPropertyChange implements java.beans.PropertyChangeListener {
    public void propertyChange(java.beans.PropertyChangeEvent event) {
    }
  }

  void computeButton_actionPerformed(java.awt.event.ActionEvent event)	{
    rangeComputed_ = true;
    minCompTextField.setEnabled(true);
    maxCompTextField.setEnabled(true);
    double min = (Double.valueOf(minTextField.getText())).doubleValue();
    double max = (Double.valueOf(maxTextField.getText())).doubleValue();
    Range2D range = new Range2D(min,max);
    int levels = Integer.parseInt(nlevelsTextField.getText());
    Range2D newRange = Graph.computeRange(range, levels);
    spacingTextField.setText(Double.toString(newRange.delta));
    minCompTextField.setText(Double.toString(newRange.start));
    maxCompTextField.setText(Double.toString(newRange.end));
  }
}
