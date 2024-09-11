/*
 * $Id: PanelModelCustomizer.java,v 1.3 2003/08/27 23:29:40 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.beans;

import java.beans.Customizer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.ListSelectionModel;
import javax.swing.border.*;
import java.util.Vector;
import java.util.Iterator;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.SimpleFileFilter;
import gov.noaa.pmel.swing.MRJUtil;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
/**
 * Used at design time to customize a <code>PanelModel</code>.  This class is also
 * used by <code>PanelModelEditor</code> for run-time changes to the <code>PanelModel</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/08/27 23:29:40 $
 * @since 3.0
 * @see PanelModelEditor
 * @see PanelModel
 */
public class PanelModelCustomizer extends JComponent implements Customizer, PropertyChangeListener {
  /**
   * @supplierCardinality 1
   */
  private PanelModel panelModel_ = null;
  private JFileChooser fileChooser_ = null;
  private BorderLayout borderLayout1 = new BorderLayout();
  private JPanel messagePanel = new JPanel();
  private JTextField message = new JTextField();
  private BorderLayout borderLayout2 = new BorderLayout();
  private JPanel mainPanel = new JPanel();
  private BorderLayout borderLayout3 = new BorderLayout();
  private JPanel layoutPanel = new JPanel();
  private BorderLayout borderLayout4 = new BorderLayout();
  private JPanel toolPanel = new JPanel();
  private JButton newPanelButton = new JButton();

  /**
     *@link aggregationByValue
     *     @associates <{PanelView}>
     * @supplierCardinality *
     */
  /* private Vector lnkPanelView; */
  private DesignPanel designPanel_ = new DesignPanel();
  private FlowLayout flowLayout1 = new FlowLayout();
  private JButton alignBottomButton = new JButton();
  private JButton alignTopButton = new JButton();
  private JButton alignLeftButton = new JButton();
  private JButton alignRightButton = new JButton();
  private JButton justVerticalButton = new JButton();
  private JButton justHorizontalButton = new JButton();
  private ImageIcon alignBottom_ = new ImageIcon(getClass().getResource("images/AlignBottom24.gif"),"Align Bottom");
  private ImageIcon alignTop_ = new ImageIcon(getClass().getResource("images/AlignTop24.gif"),"Align Top");
  private ImageIcon alignLeft_ = new ImageIcon(getClass().getResource("images/AlignLeft24.gif"),"Align Left");
  private ImageIcon alignRight_ = new ImageIcon(getClass().getResource("images/AlignRight24.gif"),"Align Right");
  private ImageIcon justifyVertical_ = new ImageIcon(getClass().getResource("images/AlignJustifyVertical24.gif"),"Justify Vertical");
  private ImageIcon justifyHorizontal_ = new ImageIcon(getClass().getResource("images/AlignJustifyHorizontal24.gif"),"Justify Horizontal");
  private ImageIcon newPanel_ = new ImageIcon(getClass().getResource("images/New24.gif"), "New Panel");
  private ImageIcon newDataGroup_ = new ImageIcon(getClass().getResource("images/NewDataGroup24.gif"), "New Data Group");
  private ImageIcon newLabel_ = new ImageIcon(getClass().getResource("images/NewLabel24.gif"), "New Label");
  private ImageIcon newLegend_ = new ImageIcon(getClass().getResource("images/NewLegend24.gif"), "New Legend");
  private ImageIcon removePanel_ = new ImageIcon(getClass().getResource("images/Remove24.gif"), "Remove Panel");
  private ImageIcon saveAsPanel_ = new ImageIcon(getClass().getResource("images/SaveAs24.gif"), "Save PanelModel");
  private ImageIcon openPanel_ = new ImageIcon(getClass().getResource("images/Open24.gif"), "Open PanelModel");
  private Component component2;
  private Component component3;
  private JButton removeButton = new JButton();
  private Component component1;
  private JPanel propertyPanel = new JPanel();
  private JPanel leftPanel = new JPanel();
  private TitledBorder propertyBorder;
  private JScrollPane jScrollPane1 = new JScrollPane();
  private BorderLayout borderLayout5 = new BorderLayout();
  private JButton newDataGroupButton = new JButton();
  private JButton newLabelButton = new JButton();
  private JButton newLegendButton = new JButton();
  private JScrollPane designScroller = new JScrollPane();

  private PanelHolderPropertyPanel phPropertyPanel_ = null;
  private LabelPropertyPanel labelPropertyPanel_ = null;
  private LegendPropertyPanel legendPropertyPanel_ = null;
  private DataGroupPropertyPanel agPropertyPanel_ = null;
  private AxisHolderPropertyPanel ahPropertyPanel_ = null;
  private PanelModelPropertyPanel pmPropertyPanel_ = null;
  private PropertyPanel currentPropertyPanel_ = null;

  private JPanel grayPanel = new JPanel();
  private BorderLayout borderLayout6 = new BorderLayout();
  private BorderLayout borderLayout7 = new BorderLayout();
  private JPanel jPanel1 = new JPanel();
  private JCheckBox expertCB = new JCheckBox();
  private GridBagLayout gridBagLayout2 = new GridBagLayout();
  private JButton openPanelButton = new JButton();
  private JButton saveAsPanelButton = new JButton();
  private Component component5;

  /**
   * <code>PanelModelCustomizer</code> constructor.
   */
  public PanelModelCustomizer() {
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    designPanel_.addPropertyChangeListener(this);
    setPanelButtons(0);
  }

  /**
   * Set a <code>PanelModel</code> to be customized.
   * @param panelModel PanelModel
   * @see PanelModel
   */
  public void setObject(Object panelModel) {
    panelModel_ = (PanelModel)panelModel;
    designPanel_.setPanelModel(panelModel_);
    pmPropertyPanel_ = new PanelModelPropertyPanel(panelModel_);
    setDefaultPanel();
    if(Page.DEBUG) System.out.println("Customizer setObject()");
  }

  private void jbInit() throws Exception {
    component2 = Box.createHorizontalStrut(8);
    component3 = Box.createHorizontalStrut(8);
    component1 = Box.createHorizontalStrut(8);
    propertyBorder = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153),2),"Parameter");
    component5 = Box.createHorizontalStrut(8);
    this.setLayout(borderLayout1);
    message.setText("Important information will go here.");
    messagePanel.setLayout(borderLayout2);
    messagePanel.setBorder(BorderFactory.createLoweredBevelBorder());
    mainPanel.setLayout(borderLayout3);
    layoutPanel.setLayout(borderLayout4);
    leftPanel.setLayout(borderLayout7);
    newPanelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newPanelButton_actionPerformed(e);
      }
    });
    toolPanel.setLayout(flowLayout1);
    toolPanel.setBorder(BorderFactory.createEtchedBorder());
    designPanel_.setLayout(null);
    alignBottomButton.setToolTipText("Align Bottom");
    alignBottomButton.setIcon(alignBottom_);
    alignBottomButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        alignBottomButton_actionPerformed(e);
      }
    });
    alignLeftButton.setToolTipText("Align Left");
    alignLeftButton.setIcon(alignLeft_);
    alignLeftButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        alignLeftButton_actionPerformed(e);
      }
    });
    alignTopButton.setToolTipText("Align Top");
    alignTopButton.setIcon(alignTop_);
    alignTopButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        alignTopButton_actionPerformed(e);
      }
    });
    alignRightButton.setToolTipText("Align Right");
    alignRightButton.setIcon(alignRight_);
    alignRightButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        alignRightButton_actionPerformed(e);
      }
    });
    newPanelButton.setToolTipText("New Panel");
    newPanelButton.setIcon(newPanel_);
    justVerticalButton.setToolTipText("Justify Vertically");
    justVerticalButton.setIcon(justifyVertical_);
    justVerticalButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        justVerticalButton_actionPerformed(e);
      }
    });
    justHorizontalButton.setToolTipText("Justify Horizontally");
    justHorizontalButton.setIcon(justifyHorizontal_);
    justHorizontalButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        justHorizontalButton_actionPerformed(e);
      }
    });
    removeButton.setToolTipText("Remove Panel");
    removeButton.setIcon(removePanel_);
    removeButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        removeButton_actionPerformed(e);
      }
    });
    this.setPreferredSize(new Dimension(800, 600));
    propertyPanel.setBorder(propertyBorder);
    propertyPanel.setMinimumSize(new Dimension(240, 10));
    propertyPanel.setPreferredSize(new Dimension(240, 10));
    propertyPanel.setLayout(borderLayout5);
    propertyBorder.setTitle("Properties");
    newDataGroupButton.setToolTipText("New Data Group");
    newDataGroupButton.setIcon(newDataGroup_);
    newDataGroupButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newDataGroupButton_actionPerformed(e);
      }
    });
    newLabelButton.setToolTipText("New Label");
    newLabelButton.setIcon(newLabel_);
    newLabelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newLabelButton_actionPerformed(e);
      }
    });
    newLegendButton.setToolTipText("New Legend");
    newLegendButton.setIcon(newLegend_);
    newLegendButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newLegendButton_actionPerformed(e);
      }
    });
    grayPanel.setBackground(Color.lightGray);
    grayPanel.setLayout(borderLayout6);
    expertCB.setText("Expert");
    expertCB.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        expertCB_actionPerformed(e);
      }
    });
    jPanel1.setLayout(gridBagLayout2);
    openPanelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        openPanelButton_actionPerformed(e);
      }
    });
    openPanelButton.setEnabled(false);
    openPanelButton.setToolTipText("Open Panel");
    openPanelButton.setIcon(openPanel_);
    saveAsPanelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveAsPanelButton_actionPerformed(e);
      }
    });
    saveAsPanelButton.setToolTipText("Save Panel");
    saveAsPanelButton.setIcon(saveAsPanel_);
    this.add(messagePanel, BorderLayout.SOUTH);
    messagePanel.add(message, BorderLayout.CENTER);
    this.add(mainPanel, BorderLayout.CENTER);
    mainPanel.add(layoutPanel, BorderLayout.CENTER);
    toolPanel.add(openPanelButton, null);
    toolPanel.add(saveAsPanelButton, null);
    toolPanel.add(component5, null);
    toolPanel.add(newPanelButton, null);
    toolPanel.add(newDataGroupButton, null);
    toolPanel.add(newLabelButton, null);
    toolPanel.add(newLegendButton, null);
    toolPanel.add(component3, null);
    toolPanel.add(removeButton, null);
    toolPanel.add(component1, null);
    toolPanel.add(alignBottomButton, null);
    toolPanel.add(alignTopButton, null);
    toolPanel.add(alignLeftButton, null);
    toolPanel.add(alignRightButton, null);
    toolPanel.add(component2, null);
    toolPanel.add(justVerticalButton, null);
    toolPanel.add(justHorizontalButton, null);
    layoutPanel.add(designScroller, BorderLayout.CENTER);
    designScroller.getViewport().add(grayPanel, null);
    grayPanel.add(designPanel_, BorderLayout.CENTER);
    leftPanel.add(propertyPanel, BorderLayout.CENTER);
    layoutPanel.add(leftPanel,  BorderLayout.WEST);
    propertyPanel.add(jScrollPane1,  BorderLayout.CENTER);
    leftPanel.add(jPanel1, BorderLayout.NORTH);
    jPanel1.add(expertCB,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    layoutPanel.add(toolPanel, BorderLayout.NORTH);

    if(!MRJUtil.isAquaLookAndFeel()) {
      alignBottomButton.setMargin(new Insets(0, 0, 0, 0));
      alignLeftButton.setMargin(new Insets(0, 0, 0, 0));
      alignTopButton.setMargin(new Insets(0, 0, 0, 0));
      alignRightButton.setMargin(new Insets(0, 0, 0, 0));
      newPanelButton.setMargin(new Insets(0, 0, 0, 0));
      justVerticalButton.setMargin(new Insets(0, 0, 0, 0));
      justHorizontalButton.setMargin(new Insets(0, 0, 0, 0));
      removeButton.setMargin(new Insets(0, 0, 0, 0));
      newDataGroupButton.setMargin(new Insets(0, 0, 0, 0));
      newLabelButton.setMargin(new Insets(0, 0, 0, 0));
      newLegendButton.setMargin(new Insets(0, 0, 0, 0));
      openPanelButton.setMargin(new Insets(0, 0, 0, 0));
      saveAsPanelButton.setMargin(new Insets(0,0,0,0));
    }
    openPanelButton.setFocusable(false);
    saveAsPanelButton.setFocusable(false);
  }

  public Dimension getMinimumSize() {
    return new Dimension(600, 400);
  }

  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  public Dimension getPreferredSize() {
    return new Dimension(800, 600);
  }

  void newPanelButton_actionPerformed(ActionEvent e) {
    PanelHolder ph = new PanelHolder("Panel" + panelModel_.getPanelCount(),
                                     panelModel_,
                                     new Rectangle(20, 20, 200, 300),
                                     null, null, null);
    PanelHolderDragBox pi = new PanelHolderDragBox(ph);
    panelModel_.addPanel(ph);
    designPanel_.addPanel(pi);
    designPanel_.repaint();
  }

  void newDataGroupButton_actionPerformed(ActionEvent e) {
    PanelHolder ph = designPanel_.getSelectedPanel();
    if(ph == null) return;
    DataGroup ag = new DataGroup("DataGroup" + ph.getDataGroupSize(), ph);
    designPanel_.addDataGroup(ag);
    designPanel_.repaint();
  }

  void newLabelButton_actionPerformed(ActionEvent e) {
    PanelHolder ph = designPanel_.getSelectedPanel();
    if(ph == null) return;
    Label label = new Label("Label" + ph.getLabelSize(),
                            new Point2D.Double(1.5, 1.5),
                            0.75, 0.25);
    designPanel_.addLabel(label);
    designPanel_.repaint();
  }

  void newLegendButton_actionPerformed(ActionEvent e) {
    PanelHolder ph = designPanel_.getSelectedPanel();
    if(ph == null) return;
    Legend legend = new Legend("Legend" + ph.getLegendSize(),
                               new Rectangle2D.Double(1.0, 1.0, 1.5, 0.25));
    designPanel_.addLegend(legend);
    designPanel_.repaint();

  }

  public void propertyChange(PropertyChangeEvent evt) {
    String pName = evt.getPropertyName();
    Object val = evt.getNewValue();
    if(Page.DEBUG) System.out.println("PanelModelCustomizer.propertyChange: " + pName + ", " + evt.getNewValue());
    setPanelButtons(designPanel_.getSelectedCount());
    if(designPanel_.getSelectedCount() > 1) {
      setDefaultPanel();
      return;
    }
    if(pName.equals("message")) {
      message.setText((String)evt.getNewValue());
    } else if(pName.equals("allUnselected")) {
      setDefaultPanel();
    } else if(pName.equals("panelSelected")) {
      if(val instanceof PanelHolderDragBox) {
        PanelHolderDragBox pdb = (PanelHolderDragBox)val;
        if(pdb.isSelected()) {
          setPanelPropertyPanel(pdb.getPanelHolder());
        } else {
          setDefaultPanel();
        }
      }
    } else if(pName.equals("dataGroupSelected")) {
      if(val instanceof DataGroupDragBox) {
        DataGroupDragBox ag = (DataGroupDragBox)val;
        if(ag.isSelected()) {
          setDataGroupPropertyPanel(ag.getDataGroup());
        } else {
          setDefaultPanel();
        }
      }
    } else if(pName.equals("axisSelected")) {
      if(val instanceof AxisHolderDragBox) {
        AxisHolderDragBox ah = (AxisHolderDragBox)val;
        if(ah.isSelected()) {
          setAxisHolderPropertyPanel(ah.getAxisHolder());
        } else {
          setDefaultPanel();
        }
      }
    } else if(pName.equals("labelSelected")) {
      if(val instanceof LabelDragBox) {
        LabelDragBox ldb = (LabelDragBox)val;
        if(ldb.isSelected()) {
          setLabelPropertyPanel(ldb.getLabel());
        } else {
          setDefaultPanel();
        }
      }
    } else if(pName.equals("legendSelected")) {
      if(val instanceof LegendDragBox) {
        LegendDragBox ldb = (LegendDragBox)val;
        if(ldb.isSelected()) {
          setLegendPropertyPanel(ldb.getLegend());
        } else {
          setDefaultPanel();
        }
      }
    } else if(pName.equals("ancestor")) {
/*      if(Page.DEBUG) {
        System.out.println("PanelModelCustomizer.propertyChange()");
        System.out.println("           propertyName = " + evt.getPropertyName());
        System.out.println("                 source = " + evt.getSource());
        System.out.println("               oldValue = " + evt.getOldValue());
        System.out.println("               newValue = " + evt.getNewValue());
        System.out.println("          propagationId = " + evt.getPropagationId());
      } */
//      panelModel_.getPage().setBackground(panelModel_.getPageBackgroundColor());
//      panelModel_.getPage().setSize(panelModel_.getPageSize());
      if(evt.getNewValue() == null) {
        // clean up ChangeListeners
        Iterator iter;
        if(panelModel_ == null) return;
        Iterator pIter = panelModel_.panelIterator();
        while(pIter.hasNext()) {
          PanelHolder ph = (PanelHolder)pIter.next();
          iter = ph.labelIterator();
          while(iter.hasNext()) {
            Label label = (Label)iter.next();
            label.removeDesignChangeListeners();
          }
          iter = ph.legendIterator();
          while(iter.hasNext()) {
            Legend legend = (Legend)iter.next();
            legend.removeDesignChangeListeners();
          }
          iter = ph.dataGroupIterator();
          while(iter.hasNext()) {
            DataGroup dg = (DataGroup)iter.next();
            dg.getXAxisHolder().removeDesignChangeListeners();
            dg.getYAxisHolder().removeDesignChangeListeners();
            dg.removeDesignChangeListeners();
          }
         ph.removeDesignChangeListeners();
        }
        panelModel_.removeDesignChangeListeners();
      } /* else {
        System.out.println("change focus here?");
        System.out.println("PanelModelCustomizer.isVisible() = " + isVisible());
        if(newPanelButton.requestFocusInWindow()) {
          System.out.println("focus may be granted");
        } else {
          System.out.println("focus wont be granted");
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
//        KeyboardFocusManager.getCurrentKeyboardFocusManager().upFocusCycle();
//        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();

      } */
    }
  }

  private void setDefaultPanel() {
    jScrollPane1.getViewport().removeAll();
    jScrollPane1.getViewport().add(pmPropertyPanel_, null);
    currentPropertyPanel_ = pmPropertyPanel_;
  }

  private void setPanelPropertyPanel(PanelHolder ph) {
    if(ph == null) {
      setDefaultPanel();
      return;
    }
    if(phPropertyPanel_ == null) {
      phPropertyPanel_ = new PanelHolderPropertyPanel(ph, expertCB.isSelected());
    } else {
      phPropertyPanel_.setPanelHolder(ph, expertCB.isSelected());
    }
    jScrollPane1.getViewport().removeAll();
    jScrollPane1.getViewport().add(phPropertyPanel_);
    currentPropertyPanel_ = phPropertyPanel_;
  }

  private void setDataGroupPropertyPanel(DataGroup dg) {
    if(dg == null) {
      setDefaultPanel();
      return;
    }
    if(agPropertyPanel_ == null) {
      agPropertyPanel_ = new DataGroupPropertyPanel(dg, expertCB.isSelected());
    } else {
      agPropertyPanel_.setDataGroup(dg, expertCB.isSelected());
    }
    jScrollPane1.getViewport().removeAll();
    jScrollPane1.getViewport().add(agPropertyPanel_);
    currentPropertyPanel_ = agPropertyPanel_;
  }

  private void setAxisHolderPropertyPanel(AxisHolder ah) {
    if(ah == null) {
      setDefaultPanel();
      return;
    }
    if(ahPropertyPanel_ == null) {
      ahPropertyPanel_ = new AxisHolderPropertyPanel(ah, expertCB.isSelected());
    } else {
      ahPropertyPanel_.setAxisHolder(ah, expertCB.isSelected());
    }
    jScrollPane1.getViewport().removeAll();
    jScrollPane1.getViewport().add(ahPropertyPanel_);
    currentPropertyPanel_ = ahPropertyPanel_;
  }

  private void setLabelPropertyPanel(Label lab) {
    if(lab == null) {
      setDefaultPanel();
      return;
    }
    if(labelPropertyPanel_ == null) {
      labelPropertyPanel_ = new LabelPropertyPanel(lab, expertCB.isSelected());
    } else {
      labelPropertyPanel_.setLabel(lab, expertCB.isSelected());
    }
    jScrollPane1.getViewport().removeAll();
    jScrollPane1.getViewport().add(labelPropertyPanel_);
    currentPropertyPanel_ = labelPropertyPanel_;
  }

  private void setLegendPropertyPanel(Legend legend) {
    if(legend == null) {
      setDefaultPanel();
      return;
    }
    if(legendPropertyPanel_ == null) {
      legendPropertyPanel_ = new LegendPropertyPanel(legend, expertCB.isSelected());
    } else {
      legendPropertyPanel_.setLegend(legend, expertCB.isSelected());
    }
    jScrollPane1.getViewport().removeAll();
    jScrollPane1.getViewport().add(legendPropertyPanel_);
    currentPropertyPanel_ = legendPropertyPanel_;
  }

  private void setPanelButtons(int enabledCount) {
    boolean enabled = enabledCount >= 2;

    alignBottomButton.setEnabled(enabled);
    alignTopButton.setEnabled(enabled);
    alignLeftButton.setEnabled(enabled);
    alignRightButton.setEnabled(enabled);

    enabled = enabledCount >= 1;
    justVerticalButton.setEnabled(enabled);
    justHorizontalButton.setEnabled(enabled);

    enabled = enabledCount == 1;
    removeButton.setEnabled(enabled);
    newDataGroupButton.setEnabled(enabled);
    newLabelButton.setEnabled(enabled);
    newLegendButton.setEnabled(enabled);

    enabled = designPanel_.isChildDragBoxSelected();
    if(enabled) {
      removeButton.setEnabled(!designPanel_.isAxisHolderDragBoxSelected());
    }
  }

  void alignBottomButton_actionPerformed(ActionEvent e) {
    designPanel_.alignBoxes(DesignPanel.ALIGN_BOTTOM);
  }

  void alignTopButton_actionPerformed(ActionEvent e) {
    designPanel_.alignBoxes(DesignPanel.ALIGN_TOP);
  }

  void alignLeftButton_actionPerformed(ActionEvent e) {
    designPanel_.alignBoxes(DesignPanel.ALIGN_LEFT);
  }

  void alignRightButton_actionPerformed(ActionEvent e) {
    designPanel_.alignBoxes(DesignPanel.ALIGN_RIGHT);
  }

  void justVerticalButton_actionPerformed(ActionEvent e) {
    designPanel_.alignBoxes(DesignPanel.JUSTIFY_VERTICAL);
  }

  void justHorizontalButton_actionPerformed(ActionEvent e) {
    designPanel_.alignBoxes(DesignPanel.JUSTIFY_HORIZONTAL);
  }

  void removeButton_actionPerformed(ActionEvent e) {
    designPanel_.removeSelected();
    setDefaultPanel();
    setPanelButtons(0);
  }

  void expertCB_actionPerformed(ActionEvent e) {
    currentPropertyPanel_.setExpert(expertCB.isSelected());
  }

  void openPanelButton_actionPerformed(ActionEvent e) {
    // problem with open since PanelModelCustomizer work on the original
    // object.  Right now there isn't a method for replacing the PanelModel
    // inside the customizer.  However, I can save it!
  }

  void saveAsPanelButton_actionPerformed(ActionEvent e) {
    File file;
    FileFilter filt;
    String ext;
    String outpath;
    String[] xmlFile = new String[]{".xml"};
    SimpleFileFilter xmlFilt = new SimpleFileFilter(xmlFile, "XML Serialized Bean");

    if(fileChooser_ == null) {
      fileChooser_ = new JFileChooser();
    }
    fileChooser_.setFileFilter(fileChooser_.getAcceptAllFileFilter());
    fileChooser_.resetChoosableFileFilters();
    fileChooser_.addChoosableFileFilter(xmlFilt);
    fileChooser_.setFileFilter(xmlFilt);

    int result = fileChooser_.showSaveDialog(this);
    if(result == JFileChooser.APPROVE_OPTION) {
      file = fileChooser_.getSelectedFile();
      filt = fileChooser_.getFileFilter();
      outpath = file.getPath();
      if(filt instanceof SimpleFileFilter) {
        ext = ((SimpleFileFilter)filt).getExtension();
        String name = file.getName().toLowerCase();
        if(!name.endsWith(ext)) {
          outpath += ext;
        }
      }
      try {
        panelModel_.saveToXML(new BufferedOutputStream(
            new FileOutputStream(outpath, false)));
      } catch (FileNotFoundException fnfe) {
        JOptionPane.showMessageDialog(this, "Error creating file, rename and try again",
                                      "File Save Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
    }
  }
}
