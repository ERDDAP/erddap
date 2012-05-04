/*
 * $Id: AddDataFrame.java,v 1.2 2003/08/22 23:02:38 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt.demo;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.Iterator;

import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.SGTVector;
import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.VectorAttribute;
import gov.noaa.pmel.sgt.beans.*;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.IndexedColorMap;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.IllegalTimeValue;
import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.sgt.beans.Page;

import javax.swing.event.*;

/**
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:38 $
 * @since 3.0
 **/
class AddDataFrame extends JFrame {
  private BorderLayout borderLayout1 = new BorderLayout();
  private JPanel jPanel1 = new JPanel();
  private JPanel jPanel2 = new JPanel();
  private JButton okButton = new JButton();
  private JButton cancelButton = new JButton();
  private JPanel sourcePanel = new JPanel();
  private JPanel targetPanel = new JPanel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private TitledBorder sourceBorder;
  private TitledBorder targetBorder;
  private Page page_ = null;
  private JScrollPane jScrollPane1 = new JScrollPane();
  private GridBagLayout gridBagLayout2 = new GridBagLayout();
  private JScrollPane jScrollPane2 = new JScrollPane();
  private JScrollPane jScrollPane3 = new JScrollPane();
  private JList panelList = new JList();
  private JList axisGroupList = new JList();
  private JList legendList = new JList();
  private TitledBorder panelBorder;
  private TitledBorder axisGroupBorder;
  private TitledBorder legendBorder;
  private BorderLayout borderLayout2 = new BorderLayout();
  private JPanel jPanel3 = new JPanel();
  private JLabel jLabel1 = new JLabel();
  private JComboBox dataTypeCB = new JComboBox();
  private JPanel cardPanel = new JPanel();
  private CardLayout cardLayout1 = new CardLayout();
  private JPanel oneDSpatialPanel = new JPanel();
  private JPanel twoDSpatialPanel = new JPanel();
  private JPanel oneDTimePanel = new JPanel();
  private JPanel twoDTimePanel = new JPanel();
  private JPanel pointPanel = new JPanel();
  private JLabel jLabel2 = new JLabel();
  private JComboBox dir1SCB = new JComboBox();
  private GridBagLayout gridBagLayout3 = new GridBagLayout();
  private JLabel jLabel3 = new JLabel();
  private JPanel jPanel5 = new JPanel();
  private JTextField delta1STF = new JTextField();
  private JTextField max1STF = new JTextField();
  private JTextField min1STF = new JTextField();
  private JLabel jLabel4 = new JLabel();
  private JComboBox type1SCB = new JComboBox();
  private JLabel jLabel5 = new JLabel();
  private JTextField amp1STF = new JTextField();
  private JLabel jLabel6 = new JLabel();
  private JTextField off1STF = new JTextField();
  private JLabel jLabel7 = new JLabel();
  private JTextField per1STF = new JTextField();
  private GridBagLayout gridBagLayout4 = new GridBagLayout();
  private Component component1;
  private String[] args = {"1-d Spatial", "2-d Spatial",
      "1-d Time", "2-d Time", "Point", "Vector"};
  private DefaultComboBoxModel dataTypeModel = new DefaultComboBoxModel(args);

  private String[] dir1Sargs = {"X_SERIES", "Y_SERIES", "PROFILE", "LOG_LOG"};
  private String[] dir2Sargs = {"XY_GRID", "XZ_GRID", "YZ_GRID"};
  private DefaultComboBoxModel dir1SModel = new DefaultComboBoxModel(dir1Sargs);
  private DefaultComboBoxModel dir2SModel = new DefaultComboBoxModel(dir2Sargs);

  private String[] typeargs = {"SINE", "RANDOM"};
  private DefaultComboBoxModel typeModel = new DefaultComboBoxModel(typeargs);
  private GridBagLayout gridBagLayout5 = new GridBagLayout();
  private JLabel jLabel8 = new JLabel();
  private JComboBox dir2SCB = new JComboBox();
  private JLabel jLabel9 = new JLabel();
  private JPanel jPanel4 = new JPanel();
  private JTextField delta12STF = new JTextField();
  private JTextField max12STF = new JTextField();
  private JTextField min12STF = new JTextField();
  private JLabel jLabel10 = new JLabel();
  private JPanel jPanel6 = new JPanel();
  private JTextField delta22STF = new JTextField();
  private JTextField max22STF = new JTextField();
  private JTextField min22STF = new JTextField();
  private JLabel jLabel11 = new JLabel();
  private JComboBox type2SCB = new JComboBox();
  private JLabel jLabel12 = new JLabel();
  private JTextField amp2STF = new JTextField();
  private JLabel jLabel13 = new JLabel();
  private JTextField off2STF = new JTextField();
  private JLabel jLabel14 = new JLabel();
  private JTextField per2STF = new JTextField();
  private GridBagLayout gridBagLayout6 = new GridBagLayout();
  private GridBagLayout gridBagLayout7 = new GridBagLayout();
  private Component component2;
  private Component component3;
  private GridBagLayout gridBagLayout8 = new GridBagLayout();
  private JTextField amp1TTF = new JTextField();
  private Component component4;
  private JComboBox type1TCB = new JComboBox();
  private GridBagLayout gridBagLayout9 = new GridBagLayout();
  private JTextField max1TTF = new JTextField();
  private JLabel jLabel15 = new JLabel();
  private JTextField off1TTF = new JTextField();
  private JLabel jLabel16 = new JLabel();
  private JTextField per1TTF = new JTextField();
  private JLabel jLabel17 = new JLabel();
  private JTextField min1TTF = new JTextField();
  private JLabel jLabel18 = new JLabel();
  private JPanel jPanel7 = new JPanel();
  private JLabel jLabel19 = new JLabel();
  private JTextField delta1TTF = new JTextField();
  private JLabel jLabel20 = new JLabel();
  private GridBagLayout gridBagLayout10 = new GridBagLayout();
  private JLabel jLabel110 = new JLabel();
  private JLabel jLabel111 = new JLabel();
  private JLabel jLabel112 = new JLabel();
  private GridBagLayout gridBagLayout11 = new GridBagLayout();
  private JTextField delta22TTF = new JTextField();
  private Component component5;
  private JTextField amp2TTF = new JTextField();
  private JComboBox type2TCB = new JComboBox();
  private JTextField max22TTF = new JTextField();
  private JPanel jPanel8 = new JPanel();
  private JTextField off2TTF = new JTextField();
  private JLabel jLabel113 = new JLabel();
  private JTextField min22TTF = new JTextField();
  private JLabel jLabel114 = new JLabel();
  private JTextField per2TTF = new JTextField();
  private JLabel jLabel21 = new JLabel();
  private JLabel jLabel22 = new JLabel();
  private Component component6;
  private GridBagLayout gridBagLayout12 = new GridBagLayout();
  private JTextField max12TTF = new JTextField();
  private JTextField min12TTF = new JTextField();
  private JPanel jPanel9 = new JPanel();
  private JTextField delta12TTF = new JTextField();
  private GridBagLayout gridBagLayout13 = new GridBagLayout();
  private GridBagLayout gridBagLayout14 = new GridBagLayout();
  private JPanel jPanel10 = new JPanel();
  private JTextField delta2PTF = new JTextField();
  private JPanel jPanel11 = new JPanel();
  private JTextField max2PTF = new JTextField();
  private JLabel jLabel119 = new JLabel();
  private JTextField min1PTF = new JTextField();
  private JTextField delta1PTF = new JTextField();
  private JTextField max1PTF = new JTextField();
  private JLabel jLabel23 = new JLabel();
  private Component component7;
  private Component component8;
  private JTextField min2PTF = new JTextField();
  private GridBagLayout gridBagLayout15 = new GridBagLayout();
  private JLabel jLabel24 = new JLabel();
  private JTextField numPTF = new JTextField();
  private JPanel vectorPanel = new JPanel();
  private GridBagLayout gridBagLayout16 = new GridBagLayout();

  public AddDataFrame() {
    this(null);
  }

  public AddDataFrame(Page page) {
    page_ = page;
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    this.setSize(450, 600);
    this.setLocation(200, 200);
    ((CardLayout)cardPanel.getLayout()).show(cardPanel, "oneDSpatial");
    init();
  }

  public Page getPage() {
    return page_;
  }

  public void setPage(Page page) {
    page_ = page;
    init();
  }

  private void jbInit() throws Exception {
    sourceBorder = new TitledBorder("");
    targetBorder = new TitledBorder("");
    panelBorder = new TitledBorder("");
    axisGroupBorder = new TitledBorder("");
    legendBorder = new TitledBorder("");
    component1 = Box.createHorizontalStrut(8);
    component2 = Box.createHorizontalStrut(8);
    component3 = Box.createHorizontalStrut(8);
    component4 = Box.createHorizontalStrut(8);
    component5 = Box.createHorizontalStrut(8);
    component6 = Box.createHorizontalStrut(8);
    component7 = Box.createHorizontalStrut(8);
    component8 = Box.createHorizontalStrut(8);
    this.getContentPane().setLayout(borderLayout1);
    okButton.setText("OK");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    jPanel2.setBorder(BorderFactory.createEtchedBorder());
    jPanel1.setLayout(gridBagLayout1);
    sourcePanel.setBorder(sourceBorder);
    sourcePanel.setMinimumSize(new Dimension(163, 300));
    sourcePanel.setPreferredSize(new Dimension(167, 300));
    sourcePanel.setLayout(borderLayout2);
    sourceBorder.setTitle("Source");
    targetPanel.setBorder(targetBorder);
    targetPanel.setLayout(gridBagLayout2);
    targetBorder.setTitle("Target");
    this.setTitle("Add Data");
    jScrollPane1.setBorder(panelBorder);
    panelBorder.setTitle("Panels");
    jScrollPane2.setBorder(axisGroupBorder);
    axisGroupBorder.setTitle("AxisGroups");
    jScrollPane3.setBorder(legendBorder);
    legendBorder.setTitle("Legends");
    jLabel1.setText("Data Type");
    cardPanel.setLayout(cardLayout1);
    jLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel2.setText("Direction:");
    oneDSpatialPanel.setLayout(gridBagLayout3);
    jLabel3.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel3.setText("Range:");
    delta1STF.setText("0.5");
    delta1STF.setColumns(8);
    max1STF.setText("10.0");
    max1STF.setColumns(8);
    min1STF.setText("0.0");
    min1STF.setColumns(8);
    jLabel4.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel4.setText("Type:");
    jLabel5.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel5.setText("Amplitude:");
    amp1STF.setText("1.0");
    amp1STF.setColumns(8);
    jLabel6.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel6.setText("Offset:");
    off1STF.setText("0.0");
    off1STF.setColumns(8);
    jLabel7.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel7.setText("Period:");
    per1STF.setText("5.0");
    per1STF.setColumns(8);
    jPanel5.setLayout(gridBagLayout4);
    dataTypeCB.setModel(dataTypeModel);
    dataTypeCB.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dataTypeCB_actionPerformed(e);
      }
    });
    dir1SCB.setModel(dir1SModel);
    type1SCB.setModel(typeModel);
    panelList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        panelList_valueChanged(e);
      }
    });
    twoDSpatialPanel.setLayout(gridBagLayout5);
    jLabel8.setText("Direction:");
    dir2SCB.setModel(dir2SModel);
    jLabel9.setText("Range1:");
    delta12STF.setText("0.02");
    delta12STF.setColumns(8);
    max12STF.setText("1.0");
    max12STF.setColumns(8);
    min12STF.setText("0.0");
    min12STF.setColumns(8);
    jLabel10.setText("Range2:");
    delta22STF.setText("0.02");
    delta22STF.setColumns(8);
    max22STF.setText("1.0");
    max22STF.setColumns(8);
    min22STF.setText("0.0");
    min22STF.setColumns(8);
    jLabel11.setText("Type:");
    type2SCB.setModel(typeModel);
    jLabel12.setText("Amplitude:");
    amp2STF.setText("0.5");
    amp2STF.setColumns(8);
    jLabel13.setText("Offset:");
    off2STF.setText("0.5");
    off2STF.setColumns(8);
    jLabel14.setText("Period:");
    per2STF.setText("0.2");
    per2STF.setColumns(8);
    jPanel4.setLayout(gridBagLayout6);
    jPanel6.setLayout(gridBagLayout7);
    oneDTimePanel.setLayout(gridBagLayout8);
    amp1TTF.setColumns(8);
    amp1TTF.setText("1.0");
    type1TCB.setModel(typeModel);
    max1TTF.setColumns(12);
    max1TTF.setText("2001-06-12 00:00");
    jLabel15.setText("Period:");
    jLabel15.setHorizontalAlignment(SwingConstants.RIGHT);
    off1TTF.setColumns(8);
    off1TTF.setText("0.0");
    jLabel16.setText("Offset:");
    jLabel16.setHorizontalAlignment(SwingConstants.RIGHT);
    per1TTF.setColumns(8);
    per1TTF.setText("5.0");
    jLabel17.setText("Amplitude:");
    jLabel17.setHorizontalAlignment(SwingConstants.RIGHT);
    min1TTF.setColumns(12);
    min1TTF.setText("2001-02-11 00:00");
    jLabel18.setText("Type:");
    jLabel18.setHorizontalAlignment(SwingConstants.RIGHT);
    jPanel7.setLayout(gridBagLayout9);
    jLabel19.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel19.setText("Range:");
    delta1TTF.setText("2");
    delta1TTF.setColumns(4);
    jLabel20.setText("Delta:");
    twoDTimePanel.setLayout(gridBagLayout10);
    jLabel110.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel110.setText("Amplitude:");
    jLabel111.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel111.setText("Offset:");
    jLabel112.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel112.setText("Period:");
    delta22TTF.setColumns(4);
    delta22TTF.setText("2");
    amp2TTF.setText("1.0");
    amp2TTF.setColumns(8);
    type2TCB.setModel(typeModel);
    max22TTF.setText("1990-06-02 00:00");
    max22TTF.setColumns(12);
    jPanel8.setLayout(gridBagLayout11);
    off2TTF.setColumns(8);
    off2TTF.setText("0.0");
    jLabel113.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel113.setText("Time Range:");
    min22TTF.setColumns(12);
    min22TTF.setText("1990-01-01 00:00");
    jLabel114.setText("Type:");
    jLabel114.setHorizontalAlignment(SwingConstants.RIGHT);
    per2TTF.setColumns(8);
    per2TTF.setText("5.0");
    jLabel21.setText("Time Delta:");
    jLabel22.setText("Depth Range:");
    max12TTF.setColumns(8);
    max12TTF.setText("10.0");
    min12TTF.setColumns(8);
    min12TTF.setText("0.0");
    jPanel9.setLayout(gridBagLayout12);
    delta12TTF.setText("0.5");
    delta12TTF.setColumns(8);
    pointPanel.setLayout(gridBagLayout13);
    jPanel10.setLayout(gridBagLayout15);
    delta2PTF.setText("0.1");
    delta2PTF.setColumns(8);
    jPanel11.setLayout(gridBagLayout14);
    max2PTF.setText("1.0");
    max2PTF.setColumns(8);
    jLabel119.setText("Range2:");
    min1PTF.setText("0.0");
    min1PTF.setColumns(8);
    delta1PTF.setText("0.1");
    delta1PTF.setColumns(8);
    max1PTF.setText("1.0");
    max1PTF.setColumns(8);
    jLabel23.setText("Range1:");
    min2PTF.setText("0.0");
    min2PTF.setColumns(8);
    jLabel24.setText("Number Pts:");
    numPTF.setText("25");
    numPTF.setColumns(5);
    vectorPanel.setLayout(gridBagLayout16);
    this.getContentPane().add(jPanel1, BorderLayout.CENTER);
    this.getContentPane().add(jPanel2,  BorderLayout.SOUTH);
    jPanel2.add(okButton, null);
    jPanel2.add(cancelButton, null);
    jPanel1.add(sourcePanel,   new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    sourcePanel.add(jPanel3, BorderLayout.NORTH);
    jPanel3.add(jLabel1, null);
    jPanel3.add(dataTypeCB, null);
    jPanel1.add(targetPanel,     new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    targetPanel.add(jScrollPane1,       new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    jScrollPane1.getViewport().add(panelList, null);
    targetPanel.add(jScrollPane2,    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    jScrollPane2.getViewport().add(axisGroupList, null);
    targetPanel.add(jScrollPane3,   new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    jScrollPane3.getViewport().add(legendList, null);
    sourcePanel.add(cardPanel, BorderLayout.CENTER);
    cardPanel.add(oneDSpatialPanel,  "oneDSpatial");
    cardPanel.add(twoDSpatialPanel, "twoDSpatial");
    twoDSpatialPanel.add(jLabel8,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(dir2SCB,   new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(jLabel9,   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(jPanel4,    new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    jPanel4.add(min12STF,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel4.add(max12STF,     new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    jPanel4.add(delta12STF,     new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    jPanel4.add(component2,   new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    twoDSpatialPanel.add(jLabel10,   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(jPanel6,    new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    jPanel6.add(min22STF,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel6.add(max22STF,     new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel6.add(delta22STF,    new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel6.add(component3,  new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    twoDSpatialPanel.add(jLabel11,   new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(type2SCB,   new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(jLabel12,   new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(amp2STF,   new GridBagConstraints(1, 4, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(jLabel13,   new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(off2STF,   new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(jLabel14,   new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDSpatialPanel.add(per2STF,   new GridBagConstraints(1, 6, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    cardPanel.add(oneDTimePanel, "oneDTime");
    jPanel7.add(min1TTF,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    jPanel7.add(max1TTF,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel7.add(component4,  new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    oneDTimePanel.add(jLabel20,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(delta1TTF,  new GridBagConstraints(1, 1, 1, 1, 0.01, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(jLabel19,               new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(amp1TTF,               new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(type1TCB,               new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(jLabel15,            new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(off1TTF,           new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(jLabel16,          new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(per1TTF,         new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(jLabel17,        new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(jLabel18,       new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDTimePanel.add(jPanel7,      new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    cardPanel.add(twoDTimePanel, "twoDTime");
    jPanel8.add(min22TTF, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    jPanel8.add(max22TTF, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel8.add(component5, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    twoDTimePanel.add(off2TTF,                     new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(jLabel113,                     new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(jLabel114,                   new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(per2TTF,                  new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(jLabel21,                   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(jLabel22,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(jPanel9,     new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    jPanel9.add(min12TTF, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    jPanel9.add(max12TTF, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel9.add(delta12TTF, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel9.add(component6, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    twoDTimePanel.add(jLabel110,                new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(jLabel111,               new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(jLabel112,              new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(delta22TTF,             new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(amp2TTF,            new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(type2TCB,           new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    twoDTimePanel.add(jPanel8,           new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    cardPanel.add(pointPanel, "point");
    jPanel10.add(min2PTF, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel10.add(max2PTF, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel10.add(delta2PTF, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel10.add(component7, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    pointPanel.add(jPanel11,                                   new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
    jPanel11.add(min1PTF, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel11.add(max1PTF, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    jPanel11.add(delta1PTF, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    jPanel11.add(component8, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    pointPanel.add(jLabel119,                             new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pointPanel.add(jLabel23,                          new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pointPanel.add(jPanel10,                      new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
    oneDSpatialPanel.add(jLabel2,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(dir1SCB,       new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(jLabel3,    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(jPanel5,   new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    jPanel5.add(min1STF,        new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    jPanel5.add(max1STF,      new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel5.add(delta1STF,     new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    jPanel5.add(component1,   new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    oneDSpatialPanel.add(jLabel4,   new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(type1SCB,   new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(jLabel5,   new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(amp1STF,   new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(jLabel6,    new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(off1STF,   new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(jLabel7,   new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    oneDSpatialPanel.add(per1STF,   new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pointPanel.add(jLabel24,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pointPanel.add(numPTF,   new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    cardPanel.add(vectorPanel,   "vector");
  }

  void init() {
    PanelModel pModel = page_.getPanelModel();
    Iterator pItor = pModel.panelIterator();
    String[] listData = new String[pModel.getPanelCount()];
    int i=0;
    while(pItor.hasNext()) {
      PanelHolder panel = (PanelHolder)pItor.next();
      listData[i++] = panel.getId();
    }
    panelList.setListData(listData);
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
    dispose();
  }

  int getDirection(String dirValue) {
    int dir = TestData.X_SERIES;
    if(dirValue.equals("X_SERIES")) {
      dir = TestData.X_SERIES;
    } else if(dirValue.equals("Y_SERIES")) {
      dir = TestData.Y_SERIES;
    } else if(dirValue.equals("PROFILE")) {
      dir = TestData.PROFILE;
    } else if(dirValue.equals("LOG_LOG")) {
      dir = TestData.LOG_LOG;
    } else if(dirValue.equals("XY_GRID")) {
      dir = TestData.XY_GRID;
    } else if(dirValue.equals("XZ_GRID")) {
      dir = TestData.XZ_GRID;
    } else if(dirValue.equals("YZ_GRID")) {
      dir = TestData.YZ_GRID;
    } else if(dirValue.equals("TIME_SERIES")) {
      dir = TestData.TIME_SERIES;
    } else if(dirValue.equals("ZT_GRID")) {
      dir = TestData.ZT_GRID;
    }
    return dir;
  }

  int getType(String typeValue) {
    int type = TestData.RANDOM;
    if(typeValue.equals("SINE")) {
      type = TestData.SINE;
    } else if(typeValue.equals("RANDOM")) {
      type = TestData.RANDOM;
    }
    return type;
  }

  void okButton_actionPerformed(ActionEvent e) {
    //
    // define default colormap
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

    SGTData data = null;
    Attribute attr = null;
    String dataType = (String)dataTypeCB.getSelectedItem();
    if(dataType.equals("1-d Spatial")) {
      int dir = getDirection((String)dir1SCB.getSelectedItem());
      int type = getType((String)type1SCB.getSelectedItem());
      double min = Double.parseDouble(min1STF.getText());
      double max = Double.parseDouble(max1STF.getText());
      double delta = Double.parseDouble(delta1STF.getText());
      Range2D range = new Range2D(min, max, delta);

      float amp = Float.parseFloat(amp1STF.getText());
      float off = Float.parseFloat(off1STF.getText());
      float per = Float.parseFloat(per1STF.getText());

      TestData td = new TestData(dir, range, type, amp, off, per);
      data = td.getSGTData();
      attr = new LineAttribute(LineAttribute.SOLID, Color.blue);
    } else if(dataType.equals("2-d Spatial")) {
      ColorMap cmap;
      int dir = getDirection((String)dir2SCB.getSelectedItem());
      int type = getType((String)type2SCB.getSelectedItem());
      double min1 = Double.parseDouble(min12STF.getText());
      double max1 = Double.parseDouble(max12STF.getText());
      double delta1 = Double.parseDouble(delta12STF.getText());
      Range2D range1 = new Range2D(min1, max1, delta1);

      double min2 = Double.parseDouble(min22STF.getText());
      double max2 = Double.parseDouble(max22STF.getText());
      double delta2 = Double.parseDouble(delta22STF.getText());
      Range2D range2 = new Range2D(min2, max2, delta2);

      float amp = Float.parseFloat(amp2STF.getText());
      float off = Float.parseFloat(off2STF.getText());
      float per = Float.parseFloat(per2STF.getText());

      TestData td = new TestData(dir, range1, range2, type, amp, off, per);
      data = td.getSGTData();
      cmap = new IndexedColorMap(red, green, blue);
      LinearTransform ctrans =
        new LinearTransform(0.0, (double)red.length, 0.0, 1.0);
      ((IndexedColorMap)cmap).setTransform(ctrans);
      attr = new GridAttribute(GridAttribute.RASTER, cmap);
//      attr = new GridAttribute();
    }else if(dataType.equals("1-d Time")) {
      int dir = TestData.TIME_SERIES;
      int type = getType((String)type1TCB.getSelectedItem());

      GeoDate start = null;
      GeoDate end = null;
      String format = "yyyy-MM-dd'T'HH:mm";
      String min1 = min1TTF.getText();
      String max1 = max1TTF.getText();
      try {
        start = new GeoDate(min1, format);
        end = new GeoDate(max1, format);
      } catch (IllegalTimeValue itf) {
        String message = "Illegal time string " + "'" + min1 + "' or '" + max1 + "'" +
                         "\nshould be of the form " + format;
        JOptionPane.showMessageDialog(this, message,
                                      "Error in Time Value", JOptionPane.ERROR_MESSAGE);
        return;
      }

      float delta = Float.parseFloat(delta1TTF.getText());

      float amp = Float.parseFloat(amp1TTF.getText());
      float off = Float.parseFloat(off1TTF.getText());
      float per = Float.parseFloat(per1TTF.getText());
      TestData td = new TestData(dir, new TimeRange(start, end), delta,
                                 type, amp, off, per);
      data = td.getSGTData();
      attr = new LineAttribute(LineAttribute.SOLID, Color.blue.brighter());
    } else if(dataType.equals("2-d Time")) {
      ColorMap cmap;
      int dir = TestData.ZT_GRID;
      int type = getType((String)type2TCB.getSelectedItem());

      double smin1 = Double.parseDouble(min12TTF.getText());
      double smax1 = Double.parseDouble(max12TTF.getText());
      double sdelta1 = Double.parseDouble(delta12TTF.getText());
      Range2D range1 = new Range2D(smin1, smax1, sdelta1);

      GeoDate start = null;
      GeoDate end = null;
      //bob changed space to 'T' (and hh to HH?)
      String format = "yyyy-MM-dd'T'HH:mm";
      String min1 = min22TTF.getText();
      String max1 = max22TTF.getText();
      try {
        start = new GeoDate(min1, format);
        end = new GeoDate(max1, format);
      } catch (IllegalTimeValue itf) {
        String message = "Illegal time string " + "'" + min1 + "' or '" + max1 + "'" +
                         "\nshould be of the form " + format;
        JOptionPane.showMessageDialog(this, message,
                                      "Error in Time Value", JOptionPane.ERROR_MESSAGE);
        return;
      }

      float delta = Float.parseFloat(delta22TTF.getText());

      float amp = Float.parseFloat(amp2TTF.getText());
      float off = Float.parseFloat(off2TTF.getText());
      float per = Float.parseFloat(per2TTF.getText());

      TestData td = new TestData(dir, range1, new TimeRange(start, end), delta,
                                 type, amp, off, per);
      data = td.getSGTData();
      cmap = new IndexedColorMap(red, green, blue);
      LinearTransform ctrans =
        new LinearTransform(0.0, (double)red.length, 0.0, 1.0);
      ((IndexedColorMap)cmap).setTransform(ctrans);
      attr = new GridAttribute(GridAttribute.RASTER, cmap);
    } else if(dataType.equals("Point")) {
      double min1 = Double.parseDouble(min1PTF.getText());
      double max1 = Double.parseDouble(max1PTF.getText());
      double delta1 = Double.parseDouble(delta1PTF.getText());
      Range2D range1 = new Range2D(min1, max1, delta1);

      double min2 = Double.parseDouble(min2PTF.getText());
      double max2 = Double.parseDouble(max2PTF.getText());
      double delta2 = Double.parseDouble(delta2PTF.getText());
      Range2D range2 = new Range2D(min2, max2, delta2);

      int num = Integer.parseInt(numPTF.getText());

      TestData td = new TestData(range1, range2, num);
      data = td.getCollection();
      attr = new PointAttribute(10, Color.red);
      ((PointAttribute)attr).setMarkHeightP(0.20);
    } else if(dataType.equals("Vector")) {
      SGTGrid uComp;
      SGTGrid vComp;
//      SGTVector vector;
      TestData td;
      /*
       * Create a test grid with sinasoidal-ramp data.
       */
      Range2D xr = new Range2D(190.0f, 250.0f, 3.0f);
      Range2D yr = new Range2D(0.0f, 45.0f, 3.0f);
      td = new TestData(TestData.XY_GRID, xr, yr,
                        TestData.SINE_RAMP, 20.0f, 10.f, 5.0f);
      uComp = (SGTGrid)td.getSGTData();
      td = new TestData(TestData.XY_GRID, xr, yr,
                        TestData.SINE_RAMP, 20.0f, 0.f, 3.0f);
      vComp = (SGTGrid)td.getSGTData();
      data = new SGTVector(uComp, vComp);
      attr = new VectorAttribute(0.0075, Color.red);
      ((VectorAttribute)attr).setHeadScale(0.5);
    }
    PanelHolder pHolder = null;
    if(!panelList.isSelectionEmpty())
      pHolder = page_.getPanelModel().findPanelHolder((String)panelList.getSelectedValue());
    DataGroup axisGroup = null;
    if(!axisGroupList.isSelectionEmpty())
      axisGroup = pHolder.findDataGroup((String)axisGroupList.getSelectedValue());
    Legend legend = null;
    if(!legendList.isSelectionEmpty())
      legend = pHolder.findLegend((String)legendList.getSelectedValue());
    //
    if((pHolder == null) || (axisGroup == null)) {
      JOptionPane.showMessageDialog(this, "A Panel and DataGroup must be selected.\n\n" +
                                    "Please select a Panel and DataGroup.",
                                    "Panel/DataGroup Selection Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    page_.getDataModel().addData(data, attr, pHolder, axisGroup, legend);
    setVisible(false);
    dispose();
  }

  void dataTypeCB_actionPerformed(ActionEvent e) {
    if(Page.DEBUG) System.out.println("ActionPerformed: new value = " + dataTypeCB.getSelectedItem());
    int index = dataTypeCB.getSelectedIndex();
    switch(index) {
      case 0:   // oneDSpatial
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "oneDSpatial");
        break;
      case 1:   // twoDSpatial
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "twoDSpatial");
        break;
      case 2:   // oneDTime
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "oneDTime");
        break;
      case 3:   // twoDTime
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "twoDTime");
        break;
      case 4:  //Point
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "point");
        break;
      case 5:  //Vector
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "vector");
        break;
      default:
        JOptionPane.showMessageDialog(this, "Selection Not Yet Implemented",
                                      "Not Implemented", JOptionPane.ERROR_MESSAGE);
        dataTypeCB.setSelectedIndex(0);
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "oneDSpatial");
    }
  }

  void panelList_valueChanged(ListSelectionEvent e) {
    String pid = (String)panelList.getSelectedValue();
    PanelHolder pHolder = page_.getPanelModel().findPanelHolder(pid);
    Iterator agItor = pHolder.dataGroupIterator();
    String[] agData = new String[pHolder.getDataGroupSize()];
    int i=0;
    while(agItor.hasNext()) {
      agData[i++] = ((DataGroup)agItor.next()).getId();
    }
    axisGroupList.setListData(agData);
    Iterator lgItor = pHolder.legendIterator();
    String[] lgData = new String[pHolder.getLegendSize()];
    i=0;
    while(lgItor.hasNext()) {
      lgData[i++] = ((Legend)lgItor.next()).getId();
    }
    legendList.setListData(lgData);
  }


}
