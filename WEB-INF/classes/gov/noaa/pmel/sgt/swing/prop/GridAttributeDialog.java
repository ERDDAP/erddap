/*
 * $Id: GridAttributeDialog.java,v 1.19 2003/08/22 23:02:39 dwd Exp $
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.event.TableModelEvent;
import java.util.Vector;
import java.io.File;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StreamTokenizer;

import gov.noaa.pmel.sgt.GridCartesianRenderer;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.ContourLineAttribute;
import gov.noaa.pmel.sgt.DefaultContourLineAttribute;
import gov.noaa.pmel.sgt.ContourLevels;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.IndexedColor;
import gov.noaa.pmel.sgt.swing.ColorSwatchIcon;
import gov.noaa.pmel.sgt.IndexedColorMap;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.ContourLevelNotFoundException;
import gov.noaa.pmel.sgt.dm.SGTGrid;

import gov.noaa.pmel.util.Range2D;
import java.awt.event.*;

/**
 * Edits the <code>GridAttribute</code>. This dialog does not make a
 * copy of the attribute so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the properties
 * immediately unless a <code>JPane</code> was supplied.
 *
 * <p> Example of <code>GridAttributeDialog</code> use:
 * <pre>
 *  JPane pane_;
 *  CartesianRenderer rend = ((CartesianGraph)pane_.getFirstLayer().getGraph()).getRenderer();
 *  ...
 *  GridAttributeDialog gad = new GridAttributeDialog();
 *  gad.setJPane(pane_);
 *  gad.setGridCartesianRenderer((GridCartesianRenderer)rend);
 *  gad.setVisible(true);
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.19 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 * @see NewLevelsDialog
 * @see ContourLineAttributeDialog
 * @see DefaultContourLineAttributeDialog
 */
public class GridAttributeDialog extends JDialog {
  private GridAttribute attr_;
  private ContourLevels conLevels_;
  private ColorMap colorMap_;
  private JPane[] paneList_ = null;
  private int contourLevelIndex_ = 0;
  private int colorMapIndex_ = 1;
  private JTable conLevelTable_;
  private ConLevelTableModel conLevelModel_;
  private SGTGrid grid_ = null;
  private JToggleButton[] colorButtons_ = new JToggleButton[256];
  /**
   * Constructor.
   */
  public GridAttributeDialog(Frame parent) {
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
    setSize(516,374);
    setVisible(false);
    mainPanel.setPreferredSize(new Dimension(516, 36));
    getContentPane().add(TabbedPane, "Center");
    ContourLevelsPanel.setLayout(new BorderLayout(0,0));
    TabbedPane.add(ContourLevelsPanel, "ContourLevelsPanel");
    ContourLevelsPanel.setBounds(2,27,511,271);
    ContourLevelsPanel.setVisible(false);
    ContourLevelsPanel.add(gridScrollPane, "Center");
    controlPanel.setLayout(new GridBagLayout());
    ContourLevelsPanel.add(controlPanel, "East");
    JPanel1.setBorder(titledBorder1);
    JPanel1.setLayout(new GridBagLayout());
    controlPanel.add(JPanel1, new GridBagConstraints(0,1,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,0,0,0),0,0));
    editButton.setToolTipText("Edit attribute of selected level.");
    editButton.setText("Edit Attribute");
    editButton.setActionCommand("Change Value");
    JPanel1.add(editButton, new GridBagConstraints(0,0,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,0,5),0,0));
    aboveButton.setToolTipText("Insert level above selected level.");
    aboveButton.setText("Insert Level Above");
    aboveButton.setActionCommand("Before Item");
    JPanel1.add(aboveButton, new GridBagConstraints(0,1,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(2,5,0,5),0,0));
    belowButton.setToolTipText("Insert level below selected level.");
    belowButton.setText("Insert Level Below");
    belowButton.setActionCommand("After Item");
    JPanel1.add(belowButton, new GridBagConstraints(0,2,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(2,5,0,5),0,0));
    deleteButton.setToolTipText("Delete the selected level.");
    deleteButton.setText("Delete Level");
    deleteButton.setActionCommand("Delete Item");
    JPanel1.add(deleteButton, new GridBagConstraints(0,3,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(2,5,2,5),0,0));
    JPanel4.setBorder(titledBorder4);
    JPanel4.setLayout(new GridBagLayout());
    controlPanel.add(JPanel4, new GridBagConstraints(0,3,1,1,0.0,0.0,
        GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,0,0,0),0,0));
    defaultButton.setToolTipText("Edit default attributes.");
    defaultButton.setText("Edit Default Attributes");
    defaultButton.setActionCommand("Edit Default Attributes");
    JPanel4.add(defaultButton, new GridBagConstraints(0,0,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(2,5,2,5),0,0));
    sortButton.setToolTipText("Sort levels by value.");
    sortButton.setText("Sort Levels");
    sortButton.setActionCommand("Sort");
    controlPanel.add(sortButton, new GridBagConstraints(0,4,1,1,0.0,0.0,
        GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0));
    newConLevelButton.setToolTipText("Create new contour level set.");
    newConLevelButton.setText("New...");
    newConLevelButton.setActionCommand("New...");
    controlPanel.add(newConLevelButton, new GridBagConstraints(0,0,1,1,1.0,0.0,
        GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,5,0,5),0,0));
    ColorMapPanel.setLayout(new BorderLayout(0,0));
    TabbedPane.add(ColorMapPanel, "ColorMapPanel");
    ColorMapPanel.setBounds(2,27,511,271);
    ColorMapPanel.setVisible(false);
    colorControlPanel.setLayout(new GridBagLayout());
    ColorMapPanel.add(colorControlPanel, "East");
    colorMapPanel.setBorder(titledBorder2);
    colorMapPanel.setLayout(new GridBagLayout());
    colorControlPanel.add(colorMapPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    newColorMapButton.setToolTipText("Create new color map.");
    newColorMapButton.setText("New...");
    newColorMapButton.setActionCommand("New...");
    colorMapPanel.add(newColorMapButton, new GridBagConstraints(0,0,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
    loadColorMapButton.setToolTipText("Load color map from disk.");
    loadColorMapButton.setText("Load...");
    loadColorMapButton.setActionCommand("Load...");
    colorMapPanel.add(loadColorMapButton, new GridBagConstraints(0,1,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
    saveColorMapButton.setToolTipText("Save color map to disk.");
    saveColorMapButton.setText("Save...");
    saveColorMapButton.setActionCommand("Save...");
    colorMapPanel.add(saveColorMapButton, new GridBagConstraints(0,2,1,1,0.0,0.0,
        GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
    colorPanel.setLayout(new CardLayout(0,0));
    ColorMapPanel.add(colorPanel, "Center");
    CLIndexedPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    colorPanel.add("CLIndexed", CLIndexedPanel);
    CLTransformPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    colorPanel.add("CLTransform", CLTransformPanel);
    CLTransformPanel.setVisible(false);
    IndexedPanel.setLayout(new GridBagLayout());
    colorPanel.add("Indexed", IndexedPanel);
    IndexedPanel.setVisible(false);
    colorButtonsPanel.setLayout(new GridLayout(16,16,1,1));
    IndexedPanel.add(colorButtonsPanel, new GridBagConstraints(0,0,1,1,1.0,1.0,
        GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
    TransformPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    colorPanel.add("Transform", TransformPanel);
    TransformPanel.setVisible(false);
    TabbedPane.setSelectedComponent(ContourLevelsPanel);
    TabbedPane.setSelectedIndex(0);
    TabbedPane.setTitleAt(0,"Contour Levels");
    TabbedPane.setTitleAt(1,"Color Map");
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
    mainPanel.setLayout(new GridBagLayout());
    getContentPane().add(mainPanel, "North");
    JLabel5.setText("Grid Style:");
    mainPanel.add(JLabel5, new GridBagConstraints(0,0,1,1,0.0,0.0,
        GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(5,5,0,5),0,0));
    gridStyleComboBox.setModel(stringComboBoxModel1);
    mainPanel.add(gridStyleComboBox, new GridBagConstraints(1,0,1,1,0.0,0.0,
        GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0));
    {
      String[] tempString = new String[5];
      tempString[0] = "RASTER";
      tempString[1] = "AREA_FILL";
      tempString[2] = "CONTOUR";
      tempString[3] = "RASTER_CONTOUR";
      tempString[4] = "AREA_FILL_CONTOUR";
      for(int i=0; i < tempString.length; i++) {
        stringComboBoxModel1.addElement(tempString[i]);
      }
    }
    gridStyleComboBox.setSelectedIndex(0);
    setTitle("GridAttribute Properties");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    applyButton.addActionListener(lSymAction);
    gridStyleComboBox.addActionListener(lSymAction);
    newConLevelButton.addActionListener(lSymAction);
    newColorMapButton.addActionListener(lSymAction);
    loadColorMapButton.addActionListener(lSymAction);
    editButton.addActionListener(lSymAction);
    aboveButton.addActionListener(lSymAction);
    belowButton.addActionListener(lSymAction);
    deleteButton.addActionListener(lSymAction);
    sortButton.addActionListener(lSymAction);
    saveColorMapButton.addActionListener(lSymAction);
    defaultButton.addActionListener(lSymAction);

    makeColorToggleButtons();
  }

  private void makeColorToggleButtons() {
    Insets insets = new Insets(0,0,0,0);
    ButtonGroup bg = new ButtonGroup();
    ColorSwatchIcon csi = null;
    for(int i=0; i < 256; i++) {
      JToggleButton tb = new JToggleButton("");
      if(System.getProperty("mrj.version") == null ||
         !UIManager.getSystemLookAndFeelClassName().equals(UIManager.getLookAndFeel().getClass().getName()))
        tb.setMargin(insets);
      csi = new ColorSwatchIcon(Color.white, 8, 8);
      tb.setIcon(csi);
      colorButtons_[i] = tb;
      colorButtonsPanel.add(tb);
      bg.add(tb);
    }
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
  public GridAttributeDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public GridAttributeDialog() {
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
      if (object == GridAttributeDialog.this)
        GridAttributeDialog_WindowClosing(event);
    }
  }

  void GridAttributeDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  //{{DECLARE_CONTROLS
  javax.swing.JTabbedPane TabbedPane = new javax.swing.JTabbedPane();
  javax.swing.JPanel ContourLevelsPanel = new javax.swing.JPanel();
  javax.swing.JScrollPane gridScrollPane = new javax.swing.JScrollPane();
  javax.swing.JPanel controlPanel = new javax.swing.JPanel();
  javax.swing.JPanel JPanel1 = new javax.swing.JPanel();
  javax.swing.JButton editButton = new javax.swing.JButton();
  javax.swing.JButton aboveButton = new javax.swing.JButton();
  javax.swing.JButton belowButton = new javax.swing.JButton();
  javax.swing.JButton deleteButton = new javax.swing.JButton();
  javax.swing.JPanel JPanel4 = new javax.swing.JPanel();
  javax.swing.JButton defaultButton = new javax.swing.JButton();
  javax.swing.JButton sortButton = new javax.swing.JButton();
  javax.swing.JButton newConLevelButton = new javax.swing.JButton();
  javax.swing.JPanel ColorMapPanel = new javax.swing.JPanel();
  javax.swing.JPanel colorControlPanel = new javax.swing.JPanel();
  javax.swing.JPanel colorMapPanel = new javax.swing.JPanel();
  javax.swing.JButton newColorMapButton = new javax.swing.JButton();
  javax.swing.JButton loadColorMapButton = new javax.swing.JButton();
  javax.swing.JButton saveColorMapButton = new javax.swing.JButton();
  javax.swing.JPanel colorPanel = new javax.swing.JPanel();
  javax.swing.JPanel CLIndexedPanel = new javax.swing.JPanel();
  javax.swing.JPanel CLTransformPanel = new javax.swing.JPanel();
  javax.swing.JPanel IndexedPanel = new javax.swing.JPanel();
  javax.swing.JPanel colorButtonsPanel = new javax.swing.JPanel();
  javax.swing.JPanel TransformPanel = new javax.swing.JPanel();
  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton applyButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.JPanel mainPanel = new javax.swing.JPanel();
  javax.swing.JLabel JLabel5 = new javax.swing.JLabel();
  javax.swing.JComboBox gridStyleComboBox = new javax.swing.JComboBox();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  DefaultComboBoxModel stringComboBoxModel1 = new DefaultComboBoxModel();
  javax.swing.border.EtchedBorder etchedBorder2 = new javax.swing.border.EtchedBorder();
  javax.swing.border.TitledBorder titledBorder1 = new javax.swing.border.TitledBorder("Contour Level");
  javax.swing.border.EmptyBorder emptyBorder1 = new javax.swing.border.EmptyBorder(5,0,0,0);
  javax.swing.border.TitledBorder titledBorder4 = new javax.swing.border.TitledBorder("Default Attributes");
  javax.swing.border.TitledBorder titledBorder2 = new javax.swing.border.TitledBorder("Color Map");
  //}}


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
        cancelButton_actionPerformed(event);
      else if (object == okButton)
        okButton_actionPerformed(event);
      else if (object == applyButton)
        applyButton_actionPerformed(event);
      else if (object == gridStyleComboBox)
        gridStyleComboBox_actionPerformed(event);
      if (object == newConLevelButton)
        newConLevelButton_actionPerformed(event);
      if (object == newColorMapButton)
        newColorMapButton_actionPerformed(event);
      else if (object == loadColorMapButton)
        loadColorMapButton_actionPerformed(event);
      else if (object == editButton)
        editButton_actionPerformed(event);
      else if (object == aboveButton)
        aboveButton_actionPerformed(event);
      else if (object == belowButton)
        belowButton_actionPerformed(event);
      else if (object == deleteButton)
        deleteButton_actionPerformed(event);
      else if (object == sortButton)
        sortButton_actionPerformed(event);
      else if (object == saveColorMapButton)
        saveColorMapButton_actionPerformed(event);
      else if (object == defaultButton)
        defaultButton_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateGridAttribute();
    this.setVisible(false);
  }

  void applyButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateGridAttribute();
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
   * Get the first parent pane.
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
   * <code>JPane</code> is often used for a <code>ColorKey</code>.
   */
  public void setJPaneList(JPane[] list) {
    paneList_ = list;
  }
  /** Get an array of parent panes. */
  public JPane[] getJPaneList() {
    return paneList_;
  }

  /**
   * Set the <code>GridCartesianRenderer</code>.  Specifying the
   * renderer give <code>GridAttributeDialog</code> a reference to the
   * data and <code>GridAttribute</code> allowing automated
   * computation of <code>ColorMap</code> and
   * <code>ContourLevels</code> ranges.
   *
   * @see #setGridAttribute(GridAttribute)
   */
  public void setGridCartesianRenderer(GridCartesianRenderer render) {
    grid_ = render.getGrid();
    setGridAttribute((GridAttribute)render.getAttribute());
  }
  /**
   * Set the <code>GridAttribute</code>.
   */
  public void setGridAttribute(GridAttribute attr) {
    attr_ = attr;
    colorMap_ = attr.getColorMap();
    conLevels_ = attr.getContourLevels();
    //
    // style
    //
    int style = attr_.getStyle();
    gridStyleComboBox.setSelectedIndex(style);
    //
    // contour ?
    //
    enableContourLevels(style);
    initContourLevels();
    //
    // raster ?
    //
    enableColorMap(style);
    initColorMap();
    //
    setCurrentTab();
  }

  private void enableContourLevels(int style) {
    boolean isContour = (style == GridAttribute.CONTOUR ||
                         style == GridAttribute.RASTER_CONTOUR ||
                         style == GridAttribute.AREA_FILL_CONTOUR);
    TabbedPane.setEnabledAt(contourLevelIndex_, isContour);
    Component[] list = ContourLevelsPanel.getComponents();
    boolean clExists = conLevels_ != null;
    for(int i=0; i < list.length; i++) {
      list[i].setEnabled(clExists);
    }
    newConLevelButton.setEnabled(true);
  }

  private void enableColorMap(int style) {
    boolean isRaster = style != GridAttribute.CONTOUR;
    TabbedPane.setEnabledAt(colorMapIndex_, isRaster);
    Component[] list = ColorMapPanel.getComponents();
    boolean cmExists = colorMap_ != null;
    for(int i=0; i < list.length; i++) {
      list[i].setEnabled(cmExists);
    }
    newColorMapButton.setEnabled(true);
  }

  private void setCurrentTab() {
    if(!TabbedPane.isEnabledAt(TabbedPane.getSelectedIndex())) {
      //
      // change to other tab
      //
      if(TabbedPane.getSelectedIndex() == colorMapIndex_) {
        TabbedPane.setSelectedIndex(contourLevelIndex_);
      } else {
        TabbedPane.setSelectedIndex(colorMapIndex_);
      }
    }
  }

  private void initContourLevels() {
    createConLevelTable();
  }

  private void initColorMap() {
    ColorSwatchIcon csi;
    int i;
    if(colorMap_ instanceof IndexedColor) {
      int maxindx = ((IndexedColor)colorMap_).getMaximumIndex();
      for(i=0; i <= maxindx; i++) {
        csi = new ColorSwatchIcon((IndexedColor)colorMap_, i, 8);
        colorButtons_[i].setIcon(csi);
        colorButtons_[i].setEnabled(true);
      }
      for(i=maxindx+1; i < 256; i++) {
        csi = new ColorSwatchIcon(Color.white, 8, 8);
        colorButtons_[i].setIcon(csi);
        colorButtons_[i].setEnabled(false);
      }
      ((CardLayout)colorPanel.getLayout()).show(colorPanel, "Indexed");
    }
  }

  void updateGridAttribute() {
    if(paneList_ != null) {
      for(int i=0; i < paneList_.length; i++) {
        paneList_[i].setBatch(true, "GridAttributeDialog");
      }
    }
    updateConLevels();
    attr_.setContourLevels(conLevels_);
    attr_.setColorMap(colorMap_);
    attr_.setStyle(gridStyleComboBox.getSelectedIndex());
    if(paneList_ != null) {
      for(int i=0; i < paneList_.length; i++) {
        paneList_[i].setBatch(false, "GridAttributeDialog");
      }
    }
  }
  /**
   * Test entry point.
   */
  public static void main(String[] args) {
    Range2D range = new Range2D(-20.0f, 45.0f, 5.0f);
    ContourLevels clevels = ContourLevels.getDefault(range);
    GridAttribute attr = new GridAttribute(clevels);
    GridAttributeDialog la = new GridAttributeDialog();
    la.setGridAttribute(attr);
    la.setTitle("Test GridAttribute Dialog");
    la.setVisible(true);
  }

  void gridStyleComboBox_actionPerformed(java.awt.event.ActionEvent event) {
    int style = gridStyleComboBox.getSelectedIndex();
    //
    enableContourLevels(style);
    enableColorMap(style);
    setCurrentTab();
    //
  }

  void newConLevelButton_actionPerformed(java.awt.event.ActionEvent event) {
    NewLevelsDialog nld = new NewLevelsDialog();
    int result = nld.showDialog(grid_);
    if(result == NewLevelsDialog.OK_RESPONSE) {
      Range2D range = nld.getRange();
      conLevels_ = ContourLevels.getDefault(range);
      initContourLevels();
    }
  }

  void newColorMapButton_actionPerformed(java.awt.event.ActionEvent event) {
    //
    // this will be replaced by a specialized dialog
    //
    //
    // define default colormap (ps.64)
    //
    int[] red =
    {  0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  7, 23, 39, 55, 71, 87,103,
       119,135,151,167,183,199,215,231,
       247,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,246,228,211,193,175,158,140};
    int[] green =
    {  0,  0,  0,  0,  0,  0,  0,  0,
       0, 11, 27, 43, 59, 75, 91,107,
       123,139,155,171,187,203,219,235,
       251,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,247,231,215,199,183,167,151,
       135,119,103, 87, 71, 55, 39, 23,
       7,  0,  0,  0,  0,  0,  0,  0};
    int[] blue =
    {  0,143,159,175,191,207,223,239,
       255,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,247,231,215,199,183,167,151,
       135,119,103, 87, 71, 55, 39, 23,
       7,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0};
    //
    colorMap_ = new IndexedColorMap(red, green, blue);
    ((IndexedColorMap)colorMap_).setTransform(new LinearTransform(0.0,
                                              (double)red.length,
                                               0.0, 1.0));
    initColorMap();
  }

  void loadColorMapButton_actionPerformed(ActionEvent event) {
    int[] r = new int[256];
    int[] g = new int[256];
    int[] b = new int[256];
    int lastindx = -1;
    File file = null;
    StreamTokenizer st = null;
    JFileChooser chooser = new JFileChooser("C:/local/pal");
    int returnVal = chooser.showOpenDialog(this);
    if(returnVal == JFileChooser.APPROVE_OPTION) {
      file = chooser.getSelectedFile();
      try {
        Reader rdr = new BufferedReader(new FileReader(file));
        st = new StreamTokenizer(rdr);
      } catch (java.io.FileNotFoundException e) {
        System.out.println(e);
        return;
      }
      try {
        st.nextToken();
        while(st.ttype != StreamTokenizer.TT_EOF) {
          lastindx++;
          if(st.ttype == StreamTokenizer.TT_NUMBER) {
            r[lastindx] = (int)st.nval;
            st.nextToken();
            g[lastindx] = (int)st.nval;
            st.nextToken();
            b[lastindx] = (int)st.nval;
          }
          if(st.nextToken() == StreamTokenizer.TT_EOL) st.nextToken();
        }
      } catch (java.io.IOException e) {
        System.out.println(e);
      }
      int[] red = new int[lastindx+1];
      int[] green = new int[lastindx+1];
      int[] blue = new int[lastindx+1];
      for(int i=0; i <= lastindx; i++) {
        red[i] = r[i];
        green[i] = g[i];
        blue[i] = b[i];
      }
      colorMap_ = new IndexedColorMap(red, green, blue);
      ((IndexedColorMap)colorMap_).setTransform(new LinearTransform(0.0,
                                                                    (double)red.length,
                                                                    0.0, 1.0));
      initColorMap();
    }
  }

  void editButton_actionPerformed(java.awt.event.ActionEvent event) {
    ContourLineAttribute attr;
    int index = conLevelTable_.getSelectedRow();
    if(index < 0) return;
    ContourLineAttributeDialog clad = new ContourLineAttributeDialog();
    attr = (ContourLineAttribute)
      ((ContourLineAttribute)conLevelModel_.getValueAt(index,1)).copy();
    int result = clad.showDialog(attr);
    if(result == ContourLineAttributeDialog.OK_RESPONSE) {
      attr = clad.getContourLineAttribute();
      conLevelModel_.setValueAt(attr, index, 1);
    }
  }

  void aboveButton_actionPerformed(java.awt.event.ActionEvent event) {
    int index = conLevelTable_.getSelectedRow();
    if(index < 0) return;
    conLevelModel_.insert(index,
                          Double.valueOf(0.0),
                          new ContourLineAttribute(ContourLineAttribute.SOLID));
  }

  void belowButton_actionPerformed(java.awt.event.ActionEvent event) {
    int index = conLevelTable_.getSelectedRow();
    if(index < 0) return;
    conLevelModel_.insert(index + 1,
                          Double.valueOf(0.0),
                          new ContourLineAttribute(ContourLineAttribute.SOLID));
  }

  void deleteButton_actionPerformed(java.awt.event.ActionEvent event) {
    int index = conLevelTable_.getSelectedRow();
    if(index < 0) return;
    conLevelModel_.remove(index);
  }

  void sortButton_actionPerformed(java.awt.event.ActionEvent event) {
    conLevelModel_.sort();
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

  void saveColorMapButton_actionPerformed(java.awt.event.ActionEvent event) {
    // to do: code goes here.
  }

  void createConLevelTable() {
    Double val;
    ContourLineAttribute attr;
    conLevelModel_ = new ConLevelTableModel();
    conLevelTable_ = new JTable(conLevelModel_);
    conLevelTable_.setSize(1000,1000);
    ListSelectionModel lsm = conLevelTable_.getSelectionModel();
    lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    TableColumn tc;
    tc = conLevelTable_.getColumnModel().getColumn(0);
    tc.setPreferredWidth(250);
    tc = conLevelTable_.getColumnModel().getColumn(1);
    tc.setPreferredWidth(750);
    gridScrollPane.getViewport().add(conLevelTable_);
    //
    if(conLevels_ == null) return;
    int size = conLevels_.size();
    for(int i=0; i < size; i++) {
      try {
        val = Double.valueOf(conLevels_.getLevel(i));
        attr = conLevels_.getContourLineAttribute(i);
        conLevelModel_.add(val, attr);
      } catch (ContourLevelNotFoundException e) {
        System.out.println(e);
      }
    }
  }

  private void updateConLevels() {
    if(conLevels_ == null) return;
    ContourLevels cl = new ContourLevels();
    Double val;
    ContourLineAttribute attr;
    conLevelModel_.sort();
    int size = conLevelModel_.getRowCount();
    for(int i=0; i < size; i++) {
      val = (Double)conLevelModel_.getValueAt(i,0);
      attr = (ContourLineAttribute)conLevelModel_.getValueAt(i,1);
      cl.addLevel(val.doubleValue(), attr);
    }
    cl.setDefaultContourLineAttribute(conLevels_.getDefaultContourLineAttribute());
    conLevels_ = cl;
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
            //    if(a.compareTo(b) > 0) { // jdk1.2
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
