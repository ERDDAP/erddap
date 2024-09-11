/*
 * $Id: JLayoutDemo.java,v 1.6 2001/02/06 00:14:35 dwd Exp $
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

import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.PlainAxis;
import gov.noaa.pmel.sgt.LineKey;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.TimeAxis;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.LineCartesianRenderer;
import gov.noaa.pmel.sgt.StackedLayout;
import gov.noaa.pmel.sgt.Axis;

import gov.noaa.pmel.sgt.dm.SimpleLine;

import gov.noaa.pmel.sgt.swing.JClassTree;

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.TimePoint;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.IllegalTimeValue;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTPoint;

import java.util.Enumeration;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EtchedBorder;

/**
 * Example demonstrating how to use <code>setLayout()</code> with
 * <code>JPane</code> to change how <code>Layer</code>s are placed on
 * a <code>JPane</code>.  <code>JLayoutDemo</code> constructs the
 * plots from basic <code>sgt</code> objects.
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2001/02/06 00:14:35 $
 * @since 2.0
 */
public class JLayoutDemo extends JApplet {
  JPane mainPane_;
  JClassTree tree_ = null;
  boolean isApplet_ = true;
  JFrame frame = null;

  public void init() {
    /*
     * Create a JLayoutDemo within a JApplet
     */
    setLayout(new BorderLayout(0,0));
    setBackground(Color.white);
    setSize(426,712);

    makeControlPanel();
    add(controlPanel, BorderLayout.SOUTH);

    makeGraph();
    add(mainPane_, BorderLayout.CENTER);
  }

  public static void main(String[] args) {
    /*
     * Create a JLayoutDemo as an application.
     */
    JLayoutDemo ld = new JLayoutDemo();
    ld.isApplet_ = false;
    ld.frame = new JFrame("Layout Demo");
    ld.frame.setSize(426,712);
    ld.frame.getContentPane().setLayout(new BorderLayout());
    ld.frame.addWindowListener(new java.awt.event.WindowAdapter() {
  public void windowClosing(java.awt.event.WindowEvent event) {
    JFrame fr = (JFrame)event.getSource();
    fr.setVisible(false);
    fr.dispose();
    System.exit(0);
  }
      });

    ld. makeControlPanel();
    ld.frame.getContentPane().add(ld.controlPanel, BorderLayout.SOUTH);

    ld.makeGraph();
    ld.mainPane_.setBackground(Color.white);
    ld.mainPane_.setBatch(true);
    ld.frame.getContentPane().add(ld.mainPane_, BorderLayout.CENTER);

    ld.frame.setVisible(true);
    ld.mainPane_.setBatch(false);
  }

  void makeControlPanel() {
    controlPanel.setLayout(new GridBagLayout());
//    controlPanel.setBackground(new java.awt.Color(200,200,200));
    controlPanel.setBounds(0,679,426,33);
    controlPanel.setBorder(new EtchedBorder());
    gridtype.add(stacked);
    stacked.setSelected(true);
    stacked.setText("Overlayed");

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(5,15,5,5);
    gbc.ipadx = 0;
    gbc.ipady = 0;
    controlPanel.add(stacked, gbc);
    stacked.setBounds(15,5,84,23);
    gridtype.add(grid);
    grid.setText("Grid");

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.5;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.VERTICAL;
    gbc.insets = new Insets(5,5,5,0);
    gbc.ipadx = 0;
    gbc.ipady = 0;
    controlPanel.add(grid, gbc);
    grid.setBounds(109,5,53,23);
    showTree.setText("Show Class Tree");

    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.VERTICAL;
    gbc.insets = new Insets(5,5,5,15);
    gbc.ipadx = 0;
    gbc.ipady = 0;
    controlPanel.add(showTree, gbc);
    showTree.setBackground(Color.yellow.brighter());
    showTree.setBounds(302,5,109,23);

    SymItem lSymItem = new SymItem();
    stacked.addItemListener(lSymItem);
    grid.addItemListener(lSymItem);
    SymAction lSymAction = new SymAction();
    showTree.addActionListener(lSymAction);
  }

  void makeGraph() {
    /*
     * This example explicitly creates the JPane, Layers, Axes, and SGLabels.
     */
    /*
     * Create JPane, place in the center of the frame
     * and set the layout to use the StackedLayout.  StackedLayout is
     * a custom layout manager designed to place Components directly
     * over one another.
     */
    mainPane_ = new JPane("Layout Demo", new Dimension(426, 400));
    mainPane_.setLayout(new StackedLayout());
    /*
     * Create the two random time series using the TestData class and
     * the SimpleLine classes from sgt.dm
     */
    SimpleLine line;
    SimpleLine line2;
    GeoDate start = null;
    GeoDate stop = null;
    TimeRange tr;
    TestData td;
    try {
      start = new GeoDate("1992-11-01", "yyyy-MM-dd");
      stop  = new GeoDate("1993-02-20", "yyyy-MM-dd");
    } catch (IllegalTimeValue e) {}
    tr = new TimeRange(start, stop);
    td = new TestData(TestData.TIME_SERIES, tr, 1.0f,
                      TestData.RANDOM, 1.2f, 0.0f, 20.0f);
    line = (SimpleLine)td.getSGTData();
    //
    try {
      start = new GeoDate("1992-11-01", "yyyy-MM-dd");
      stop  = new GeoDate("1993-02-20", "yyyy-MM-dd");
    } catch (IllegalTimeValue e) {}
    tr = new TimeRange(start, stop);
    td = new TestData(TestData.TIME_SERIES, tr, 1.0f,
                      TestData.RANDOM, 1.2f, 0.5f, 30.0f);
    line2 = (SimpleLine)td.getSGTData();
    /*
     * Get the axis ranges from SGTLine
     */
    SoTRange ynRange, yRange;
    SoTRange tnRange;
    String yLabel;
    yRange = line.getYRange();
    yRange.add(line2.getYRange());
    tnRange = line.getXRange();
    /*
     * compute the range for the y and time axes
     * and get the y axis label from line's metadata
     */
    ynRange = Graph.computeRange(yRange, 6);
    yLabel = line.getYMetaData().getName();
    /*
     * LayoutDemo will have two layers.
     * One layer for each line to be drawn.
     * The first layer will contain the axes and labels
     * and the first set of data. The second layer will
     * contain the second set of data.
     */
    /*
     * xsize, ysize are the width and height in physical units
     * of the Layer graphics region.
     *
     * xstart, xend are the start and end points for the TimeAxis
     * ystart, yend are the start and end points for the Y axis
     */
    double xsize  = 4.0;
    double xstart = 0.6;
    double xend   = 3.25;
    double ysize  = 3.0;
    double ystart = 0.6;
    double yend   = 2.50;

    Layer layer, layer2;
    SGLabel label, title, ytitle;
    CartesianGraph graph, graph2;
    LinearTransform xt, yt;
    PlainAxis yleft;
    TimeAxis xbot;
    LineKey lkey;
    GeoDate stime;
    /*
     * create the first layer
     */
    layer = new Layer("First Layer", new Dimension2D(xsize, ysize));
    /*
     * create a time stamp label for the plot
     * position the label at the lower left corner
     * and add to the first layer
     * (NOTE: the time will be displayed for the GMT time zone)
     */
    stime = new GeoDate();
    label = new SGLabel("Date Stamp", stime.toString(),
                        new Point2D.Double(0.05, 0.05));
    label.setAlign(SGLabel.BOTTOM, SGLabel.LEFT);
    label.setColor(Color.magenta);
    label.setHeightP(0.15);
    label.setFont(new Font("Dialog", Font.PLAIN, 10));
    layer.addChild(label);
    /*
     * create a title for the plot
     * position the label centered on the graph
     * and add to the first layer
     */
    title = new SGLabel("Title", "Layout Demo",
                        new Point2D.Double(xsize/2.0, ysize));
    title.setAlign(SGLabel.TOP, SGLabel.CENTER);
    title.setHeightP(0.20);
    title.setFont(new Font("Helvetica", Font.BOLD, 14));
    layer.addChild(title);
    /*
     * create a LineKey
     * the LineKey will be a legend for the two lines created
     * position the key in the upper right corner
     * and add to the first layer
     */
    lkey = new LineKey();
    lkey.setId("Legend");
    lkey.setLocationP(new Point2D.Double(xsize - 0.01, ysize));
    lkey.setVAlign(LineKey.TOP);
    lkey.setHAlign(LineKey.RIGHT);
    layer.addChild(lkey);
    /*
     * add the first layer to the Pane
     */
    mainPane_.add(layer);
    /*
     * create first CartesianGraph and transforms
     */
    graph = new CartesianGraph("First Graph");
    xt = new LinearTransform(new Range2D(xstart, xend), tnRange);
    graph.setXTransform(xt);
    yt = new LinearTransform(new Range2D(ystart, yend), ynRange);
    graph.setYTransform(yt);
    /*
     * Create the time axis, set its range in user units
     * and its origin. Add the axis to the first graph.
     */
    SoTPoint point = new SoTPoint(ynRange.getStart(), tnRange.getStart());
    xbot = new TimeAxis("Bottom Axis", TimeAxis.MONTH_YEAR);
    xbot.setRangeU(tnRange);
    xbot.setLocationU(point);
    Font xbfont = new Font("Helvetica", Font.ITALIC, 14);
    xbot.setLabelFont(xbfont);
    xbot.setMinorLabelInterval(1);
    graph.addXAxis(xbot);
    /*
     * Create the vertical axis, set its range in user units
     * and its origin.  Create the axis title and add the
     * axis to the first graph.
     */
    yleft = new PlainAxis("Left Axis");
    yleft.setRangeU(ynRange);
    yleft.setLocationU(point);
    yleft.setLabelFont(xbfont);
    ytitle = new SGLabel("Y-Axis Title", yLabel,
                         new Point2D.Double(0.0, 0.0));
    Font ytfont = new Font("Helvetica", Font.PLAIN, 14);
    ytitle.setFont(ytfont);
    ytitle.setHeightP(0.2);
    yleft.setTitle(ytitle);
    graph.addYAxis(yleft);
    /*
     * Add the first graph to the first layer.
     */
    layer.setGraph(graph);
    /*
     * Create a LineAttribute for the display of the first
     * line. Associate the attribute and the line with the
     * first graph.  Add the line to the LineKey.
     */
    LineAttribute attr;

    attr = new LineAttribute(LineAttribute.MARK, 20, Color.red);
    attr.setMarkHeightP(0.1);
    graph.setData(line, attr);
    lkey.addLineGraph((LineCartesianRenderer)graph.getRenderer(),
                      new SGLabel("1st line", "Red Data",
                                  new Point2D.Double(0.0, 0.0)));
    /*
     * Create the second layer and add it the the Pane.
     * Create the second graph and associate it with the
     * second layer.
     */
    layer2 = new Layer("Second Layer", new Dimension2D(xsize, ysize));
    mainPane_.add(layer2);
    graph2 = new CartesianGraph("Second Graph", xt, yt);
    layer2.setGraph(graph2);
    /*
     * Create a LineAttribute for the display of the second
     * line. Associate the attribute and the line with the
     * second graph.  Add the line to the LineKey.
     */
    LineAttribute attr2;
    attr2 = new LineAttribute(LineAttribute.MARK, 2, Color.blue);
    attr2.setMarkHeightP(0.1);
    graph2.setData(line2, attr2);
    lkey.addLineGraph((LineCartesianRenderer)graph2.getRenderer(),
                      new SGLabel("2nd line", "Blue Data",
                                  new Point2D.Double(0.0, 0.0)));

  }

  JPanel controlPanel = new JPanel();
  JCheckBox stacked = new JCheckBox();
  ButtonGroup gridtype = new ButtonGroup();
  JCheckBox grid = new JCheckBox();
  JButton showTree = new JButton();

  class SymItem implements java.awt.event.ItemListener {
    public void itemStateChanged(java.awt.event.ItemEvent event) {
      Object object = event.getSource();
      if (object == stacked)
        stacked_itemStateChanged(event);
      else if (object == grid)
        grid_itemStateChanged(event);
    }
  }

  /**
   * Change the Pane layout to StackedLayout.
   *
   * @param event
   */

  void stacked_itemStateChanged(java.awt.event.ItemEvent event) {
    /*
     * Get the component list for mainPane_ and change
     * the layout to StackedLayout.
     */
    Component[] comps = mainPane_.getComponents();
    mainPane_.setBatch(true);
    mainPane_.setLayout(new StackedLayout());
    /*
     * Remove any axes that have been associated with
     * the second graph.  With the layers overlayed it
     * is not necessary to have duplicate axes.
     */
    Graph gr2 = ((Layer)comps[1]).getGraph();
    ((CartesianGraph)gr2).removeAllXAxes();
    ((CartesianGraph)gr2).removeAllYAxes();
    /*
     * Tell the Applet that the mainPane_ needs to
     * be layed out and re-draw the mainPane_.
     */
    if(isApplet_) {
      validate();
    } else {
      frame.validate();
    }
    mainPane_.setBatch(false);
    if(tree_ != null) {
      if(tree_.isVisible()) {
        tree_.setJPane(mainPane_);
        tree_.expandTree();
      }
    }
  }

  /**
   * Change the Pane layout to GridLayout.
   *
   * @param event
   */

  void grid_itemStateChanged(java.awt.event.ItemEvent event) {
    /*
     * Get the component list for mainPane_ and change
     * the layout to GridLayout.
     */
    Component[] comps = mainPane_.getComponents();
    mainPane_.setBatch(true);
    mainPane_.setLayout(new GridLayout(2,0));
    /*
     * Get the first and second graphs from the first
     * and second layers, respectively.
     */
    Graph gr = ((Layer)comps[0]).getGraph();
    Graph gr2 = ((Layer)comps[1]).getGraph();
    /*
     * Create copies of all X-Axes associated with the first
     * graph for the second graph. If the axes are not copied then
     * the second graph will have the second line plotted, but without
     * any axes.
     */
    for(Enumeration xa = ((CartesianGraph)gr).xAxisElements(); xa.hasMoreElements();) {
      ((CartesianGraph)gr2).addXAxis(((Axis)xa.nextElement()).copy());
    }
    /*
     * Create copies of all Y-Axes associated with the first
     * graph for the second graph.
     */
    for(Enumeration ya = ((CartesianGraph)gr).yAxisElements(); ya.hasMoreElements();) {
      ((CartesianGraph)gr2).addYAxis(((Axis)ya.nextElement()).copy());
    }
    /*
     * Tell the Applet that the mainPane_ needs to
     * be layed out and re-draw the mainPane_.
     */
    if(isApplet_) {
      validate();
    } else {
      frame.validate();
    }
    //    mainPane_.draw();
    mainPane_.setBatch(false);
    if(tree_ != null) {
      if(tree_.isVisible()) {
        tree_.setJPane(mainPane_);
        tree_.expandTree();
      }
    }
  }

  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == showTree)
        showTree_ActionPerformed(event);
    }
  }

  void showTree_ActionPerformed(java.awt.event.ActionEvent event) {
    /*
     * Create the ClassTree dialog to display the classes used
     * in the mainPane_ and allow editing.
     */
    if(tree_ == null) {
      tree_ = new JClassTree("Classes for LayoutDemo");
    }
    tree_.setJPane(mainPane_);
    tree_.show();
  }

}
