/*
 * $Id: ContourLevelsDialog.java,v 1.8 2001/02/08 00:29:38 dwd Exp $
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
import javax.swing.table.TableColumn;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Vector;
import java.util.Enumeration;

import gov.noaa.pmel.sgt.ContourLevels;
import gov.noaa.pmel.sgt.ContourLevelNotFoundException;
import gov.noaa.pmel.sgt.ContourLineAttribute;
import gov.noaa.pmel.sgt.DefaultContourLineAttribute;

/**
 * This dialog edits a <code>ContourLevels</code> object. This dialog
 * has the standalone functionality found in the      
 * <code>GridAttributeDialog</code> to edit <code>ContourLevels</code>
 * objects. 
 *
 * <p> Example of <code>ContourLevelsDialog</code> use:
 * <pre>
 *
 * public ContourLevels editLevels(ContourLevels inLevels) {
 *   ContourLevelsDialog cld = new ContourLevelsDialog(inLevels);
 *   if(cld.showDialog() == ContourLevelsDialog.CANCEL_RESPONSE) {
 *     return inLevels;
 *   } else {
 *     return cld.getContourLevels();
 *   }
 * }
 *
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2001/02/08 00:29:38 $
 * @since 2.0
 */
public class ContourLevelsDialog extends JDialog {
  private ContourLevels conLevels_;
  private JTable table_;
  private ConLevelTableModel model_;
  private int result_;
  /** OK button was selected */
  public static int OK_RESPONSE = 1;
  /** Cancel button was selected */
  public static int CANCEL_RESPONSE = 2;
  /**
   * Constructor.
   */
  public ContourLevelsDialog(Frame parent) {
    super(parent);
    try {
      jbInit();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  void jbInit() throws Exception {
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(529,307);
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5,5));
    gridScrollPane.setMinimumSize(new Dimension(350, 283));
    gridScrollPane.setPreferredSize(new Dimension(350, 283));
    getContentPane().add(buttonPanel, "South");
    okButton.setText("OK");
    okButton.setActionCommand("OK");
    buttonPanel.add(okButton);
    cancelButton.setText("Cancel");
    cancelButton.setActionCommand("Cancel");
    buttonPanel.add(cancelButton);
    controlPanel.setLayout(new GridBagLayout());
    getContentPane().add(controlPanel, "East");
    JPanel1.setBorder(titledBorder1);
    JPanel1.setLayout(new GridBagLayout());
    controlPanel.add(JPanel1, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
						     ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 0, 0, 0), 0, 0));
    editButton.setToolTipText("Edit attribute of selected level.");
    editButton.setText("Edit Attribute");
    editButton.setActionCommand("Change Value");
    JPanel1.add(editButton, new GridBagConstraints(0,0,1,1,1.0,1.0,
						   GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,0,5),0,0));
    aboveButton.setToolTipText("Insert level above selected level.");
    aboveButton.setText("Insert Level Above");
    aboveButton.setActionCommand("Before Item");
    JPanel1.add(aboveButton, new GridBagConstraints(0,1,1,1,1.0,1.0,
						    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,0,5),0,0));
    belowButton.setToolTipText("Insert level below selected level.");
    belowButton.setText("Insert Level Below");
    belowButton.setActionCommand("After Item");
    JPanel1.add(belowButton, new GridBagConstraints(0,2,1,1,1.0,1.0,
						    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,0,5),0,0));
    deleteButton.setToolTipText("Delete the selected level.");
    deleteButton.setText("Delete Level");
    deleteButton.setActionCommand("Delete Item");
    JPanel1.add(deleteButton, new GridBagConstraints(0,3,1,1,1.0,1.0,
						     GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0));
    JPanel4.setBorder(titledBorder4);
    JPanel4.setLayout(new GridBagLayout());
    controlPanel.add(JPanel4, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
						     ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));
    defaultButton.setToolTipText("Edit default attributes.");
    defaultButton.setText("Edit Default Attributes");
    JPanel4.add(defaultButton,new GridBagConstraints(0,0,1,1,1.0,1.0,
						     GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,5,5,5),0,0));
    sortButton.setToolTipText("Sort levels by value.");
    sortButton.setText("Sort Levels");
    sortButton.setActionCommand("Sort");
    controlPanel.add(sortButton, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
							,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    gridScrollPane.setBorder(compoundBorder1);
    getContentPane().add(gridScrollPane, "Center");
    setTitle("Edit Contour Levels");


    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    editButton.addActionListener(lSymAction);
    aboveButton.addActionListener(lSymAction);
    belowButton.addActionListener(lSymAction);
    deleteButton.addActionListener(lSymAction);
    sortButton.addActionListener(lSymAction);
    defaultButton.addActionListener(lSymAction);

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
  public ContourLevelsDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public ContourLevelsDialog() {
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
      if (object == ContourLevelsDialog.this)
	ContourLevelsDialog_WindowClosing(event);
    }
  }

  void ContourLevelsDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.JPanel controlPanel = new javax.swing.JPanel();
  javax.swing.JPanel JPanel1 = new javax.swing.JPanel();
  javax.swing.JButton editButton = new javax.swing.JButton();
  javax.swing.JButton aboveButton = new javax.swing.JButton();
  javax.swing.JButton belowButton = new javax.swing.JButton();
  javax.swing.JButton deleteButton = new javax.swing.JButton();
  javax.swing.JPanel JPanel4 = new javax.swing.JPanel();
  javax.swing.JButton defaultButton = new javax.swing.JButton();
  javax.swing.JButton sortButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JScrollPane gridScrollPane = new javax.swing.JScrollPane();
  javax.swing.border.TitledBorder titledBorder1 = new javax.swing.border.TitledBorder("Contour Levels");
  javax.swing.border.EmptyBorder emptyBorder1 = new javax.swing.border.EmptyBorder(5,0,0,0);
  javax.swing.border.CompoundBorder compoundBorder1 = new
    javax.swing.border.CompoundBorder(emptyBorder1, etchedBorder1);
  javax.swing.border.TitledBorder titledBorder4 = new javax.swing.border.TitledBorder("Default Attributes");


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
	cancelButton_actionPerformed(event);
      else if (object == okButton)
	okButton_actionPerformed(event);
      else if (object == editButton)
	editButton_actionPerformed(event);
      else if (object == aboveButton)
	aboveButton_actionPerformed(event);
      else if (object == belowButton)
	belowButton_actionPerformed(event);
      if (object == deleteButton)
	deleteButton_actionPerformed(event);
      else if (object == sortButton)
	sortButton_actionPerformed(event);
      else if (object == defaultButton)
        defaultButton_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
    result_ = CANCEL_RESPONSE;
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    update();
    result_ = OK_RESPONSE;
    this.setVisible(false);
  }

  private void update() {
    ContourLevels cl = new ContourLevels();
    Double val;
    ContourLineAttribute attr;
    int size = model_.getRowCount();
    for(int i=0; i < size; i++) {
      val = (Double)model_.getValueAt(i,0);
      attr = (ContourLineAttribute)model_.getValueAt(i,1);
      cl.addLevel(val.doubleValue(), attr);
    }
    cl.setDefaultContourLineAttribute(conLevels_.getDefaultContourLineAttribute());
    conLevels_ = cl;
  }
  /**
   * Dialog test entry point.
   */
  public static void main(String[] args) {
    ContourLevelsDialog cla = new ContourLevelsDialog();
    cla.setTitle("Test ContourLevels Dialog");
    cla.setVisible(true);
  }
  /**
   * Show the dialog and wait for a response.
   *
   * @param cl <code>ContourLevels</code> object to be edited
   * @return return code
   */
  public int showDialog(ContourLevels cl) {
    conLevels_ = cl;
    result_ = CANCEL_RESPONSE;
    createTable();
    setModal(true);
    super.setVisible(true);
    return result_;
  }

  void createTable() {
    Double val;
    ContourLineAttribute attr;
    int size = conLevels_.size();
    model_ = new ConLevelTableModel();
    for(int i=0; i < size; i++) {
      try {
	val = Double.valueOf(conLevels_.getLevel(i));
	attr = conLevels_.getContourLineAttribute(i);
	model_.add(val, attr);
      } catch (ContourLevelNotFoundException e) {
	System.out.println(e);
      }
    }
    table_ = new JTable(model_);
    table_.setSize(1000,1000);
    ListSelectionModel lsm = table_.getSelectionModel();
    lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    TableColumn tc;
    tc = table_.getColumnModel().getColumn(0);
    tc.setPreferredWidth(250);
    tc = table_.getColumnModel().getColumn(1);
    tc.setPreferredWidth(750);
    gridScrollPane.getViewport().add(table_);
  }
  /**
   * Get the edited <code>ContourLevels</code>.
   */
  public ContourLevels getContourLevels() {
    return conLevels_;
  }

  void editButton_actionPerformed(java.awt.event.ActionEvent event) {
    ContourLineAttribute attr;
    int index = table_.getSelectedRow();
    if(index < 0) return;
    ContourLineAttributeDialog clad = new ContourLineAttributeDialog();
    attr = (ContourLineAttribute)
      ((ContourLineAttribute)model_.getValueAt(index,1)).copy();
    int result = clad.showDialog(attr);
    if(result == ContourLineAttributeDialog.OK_RESPONSE) {
      attr = clad.getContourLineAttribute();
      model_.setValueAt(attr, index, 1);
    }
  }

  void aboveButton_actionPerformed(java.awt.event.ActionEvent event) {
    int index = table_.getSelectedRow();
    if(index < 0) return;
    model_.insert(index,
		  Double.valueOf(0.0),
		  new ContourLineAttribute(ContourLineAttribute.SOLID));
  }

  void belowButton_actionPerformed(java.awt.event.ActionEvent event) {
    int index = table_.getSelectedRow();
    if(index < 0) return;
    model_.insert(index + 1,
		  Double.valueOf(0.0),
		  new ContourLineAttribute(ContourLineAttribute.SOLID));
  }


  void deleteButton_actionPerformed(java.awt.event.ActionEvent event) {
    int index = table_.getSelectedRow();
    if(index < 0) return;
    model_.remove(index);
  }

  void sortButton_actionPerformed(java.awt.event.ActionEvent event) {
    model_.sort();
  }

  void defaultButton_actionPerformed(java.awt.event.ActionEvent event) {
    DefaultContourLineAttribute attr;
    DefaultContourLineAttributeDialog dclad = new DefaultContourLineAttributeDialog();
    attr = conLevels_.getDefaultContourLineAttribute();
    int result = dclad.showDialog((DefaultContourLineAttribute)attr.copy());
    if(result == DefaultContourLineAttributeDialog.OK_RESPONSE) {
      attr = dclad.getDefaultContourLineAttribute();
      conLevels_.setDefaultContourLineAttribute(attr);
    }
  }

  class ConLevelTableModel extends AbstractTableModel {
    Vector values = new Vector();
    Vector attr = new Vector();
    String[] titles = {"Value", "Attribute"};

    public void add(Double val, ContourLineAttribute cla) {
      values.addElement(val);
      attr.addElement(cla);
    }

    public void insert(int row, Double val, ContourLineAttribute cla) {
      values.insertElementAt(val, row);
      attr.insertElementAt(cla, row);
      fireTableChanged(new TableModelEvent(this, row, row,
					   TableModelEvent.ALL_COLUMNS,
					   TableModelEvent.INSERT));
    }

    public void remove(int row) {
      values.removeElementAt(row);
      attr.removeElementAt(row);
      fireTableChanged(new TableModelEvent(this, row, row,
					   TableModelEvent.ALL_COLUMNS,
					   TableModelEvent.DELETE));
    }

    public void sort() {
      //
      // use brain-dead bubble sort (there will be few lines)
      //
      int i, temp;
      int size = values.size();
      Double a, b;
      int[] index = new int[size];
      boolean flipped = true;
      for(i=0; i < size; i++) {
	index[i] = i;
      }
      while(flipped) {
	flipped = false;
	for(i=0; i < size-1; i++) {
	  a = (Double)values.elementAt(index[i]);
	  b = (Double)values.elementAt(index[i+1]);
	  if(a.doubleValue() > b.doubleValue()) {
	    //	  if(a.compareTo(b) > 0) {  // jdk1.2
	    temp = index[i];
	    index[i] = index[i+1];
	    index[i+1] = temp;
	    flipped = true;
	  }
	}
      }
      Vector oldValues = values;
      Vector oldAttr = attr;
      values = new Vector(size);
      attr = new Vector(size);
      for(i=0; i < size; i++) {
	values.addElement(oldValues.elementAt(index[i]));
	attr.addElement(oldAttr.elementAt(index[i]));
      }
      fireTableChanged(new TableModelEvent(this));
    }

    public Object getValueAt(int row, int col) {
      if(col == 0) {
	return values.elementAt(row);
      } else {
	return attr.elementAt(row);
      }
    }

    public void setValueAt(Object obj, int row, int col) {
      if(col == 0) {
	if(obj instanceof Double) {
	  values.setElementAt(obj, row);
	} else if(obj instanceof String) {
	  values.setElementAt(Double.valueOf((String)obj), row);
	}
      } else {
	attr.setElementAt(obj, row);
      }
      fireTableCellUpdated(row, col);
    }

    public int getRowCount() {
      return values.size();
    }

    public int getColumnCount() {
      return 2;
    }

    public String getColumnName(int col) {
      return titles[col];
    }

    public boolean isCellEditable(int row, int col) {
      return col == 0;
    }

  }

}
