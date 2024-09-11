/*
 * $Id: JLogLogDemo.java,v 1.2 2003/08/22 23:02:38 dwd Exp $
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
//import gov.noaa.pmel.sgt.PlainAxis;
import gov.noaa.pmel.sgt.LogAxis;
import gov.noaa.pmel.sgt.LineKey;
//import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.LogTransform;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.SGLabel;
//import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.LineCartesianRenderer;
import gov.noaa.pmel.sgt.StackedLayout;
import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.swing.JClassTree;
//import gov.noaa.pmel.sgt.Logo;

//import gov.noaa.pmel.sgt.dm.Collection;
import gov.noaa.pmel.sgt.dm.SGTData;

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTValue;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.Dimension2D;

import java.awt.*;
import javax.swing.*;

/**
 * Example demonstrating the creation of a simple
 * graph using LogAxis.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:38 $
 * @since 3.0
 */

public class JLogLogDemo extends JApplet {
    JButton tree_;
    JButton space_;
    JPane mainPane_;

  public void init() {
    setLayout(new BorderLayout(0,0));
    setSize(553,438);

    add(makeGraph(), BorderLayout.CENTER);
  }

  public static void main(String[] args) {
    JLogLogDemo pd = new JLogLogDemo();
    JFrame frame = new JFrame("Log-Log Demo");
    JPanel button = new JPanel();
    JPane graph;
    button.setLayout(new FlowLayout());
    pd.tree_ = new JButton("Tree View");
    MyAction myAction = pd. new MyAction();
    pd.tree_.addActionListener(myAction);
    button.add(pd.tree_);
    pd.space_ = new JButton("Add Mark");
    pd.space_.addActionListener(myAction);
    button.add(pd.space_);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent event) {
	JFrame fr = (JFrame)event.getSource();
	fr.setVisible(false);
	fr.dispose();
	System.exit(0);
      }
    });
    frame.setSize(553,438);
    graph = pd.makeGraph();
    graph.setBatch(true);
    frame.getContentPane().add(graph, BorderLayout.CENTER);
    frame.getContentPane().add(button, BorderLayout.SOUTH);
    frame.pack();
    frame.setVisible(true);
    graph.setBatch(false);
  }

  JPane makeGraph() {
    /*
     * This example creates a very simple plot from
     * scratch (not using one of the sgt.awt classes)
     * to display log-log line.
     */
    /*
     * Create a Pane, place in the center of the Applet
     * and set the layout to be StackedLayout.
     */
    mainPane_ = new JPane("Point Plot Demo", new Dimension(553,438));
    mainPane_.setLayout(new StackedLayout());
    mainPane_.setBackground(Color.white);
    /*
     * Create a line using the TestData class.
     */
    Range2D xrange;
    SoTRange yrange;
    TestData td;
    SGTData data;
    xrange = new Range2D(50.0, 150000., 1.25);
    td = new TestData(TestData.LOG_LOG, xrange, TestData.RANDOM, 10000.0f, 1.0f, 10.0f);
    data = td.getSGTData();
    yrange = data.getYRange();
    /*
     * xsize, ysize are the width and height in physical units
     * of the Layer graphics region.
     *
     * xstart, xend are the start and end points for the X axis
     * ystart, yend are the start and end points for the Y axis
     */
    double xsize = 4.0;
    double xstart = 0.6;
    double xend = 3.5;
    double ysize = 3.0;
    double ystart = 0.6;
    double yend = 2.75;
    /*
     * Create the layer and add it to the Pane.
     */
    Layer layer;

    layer = new Layer("Layer 1", new Dimension2D(xsize, ysize));
    mainPane_.add(layer);
    /*
     * Create a CartesianGraph and transforms.
     */
    CartesianGraph graph;
    LogTransform xt, yt;

    graph = new CartesianGraph("Log-Log Graph");
    layer.setGraph(graph);
    xt = new LogTransform(xstart, xend, xrange.start, xrange.end);
    yt = new LogTransform(new Range2D(ystart, yend), yrange);
    graph.setXTransform(xt);
    graph.setYTransform(yt);
    /*
     * Create the bottom axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    LogAxis xbot;
    String xLabel = "X Label";

    xbot = new LogAxis("Botton Axis");
    xbot.setRangeU(xrange);
    xbot.setLocationU(new SoTPoint(new SoTValue.Double(xrange.start), yrange.getStart()));
    Font xbfont = new Font("Helvetica", Font.ITALIC, 14);
    xbot.setLabelFont(xbfont);
    SGLabel xtitle = new SGLabel("xaxis title", xLabel,
                                 new Point2D.Double(0.0, 0.0));
    Font xtfont = new Font("Helvetica", Font.PLAIN, 14);
    xtitle.setFont(xtfont);
    xtitle.setHeightP(0.2);
    xbot.setTitle(xtitle);
    graph.addXAxis(xbot);
    /*
     * Create the left axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    LogAxis yleft;
    String yLabel = "Y Label";

    yleft = new LogAxis("Left Axis");
    yleft.setRangeU(yrange);
    yleft.setLocationU(new SoTPoint(new SoTValue.Double(xrange.start), yrange.getStart()));
    yleft.setLabelFont(xbfont);
    SGLabel ytitle = new SGLabel("yaxis title", yLabel,
                                 new Point2D.Double(0.0, 0.0));
    Font ytfont = new Font("Helvetica", Font.PLAIN, 14);
    ytitle.setFont(ytfont);
    ytitle.setHeightP(0.2);
    yleft.setTitle(ytitle);
    graph.addYAxis(yleft);
    /*
     * Create a LineAttribute for the display of the
     * line.
     */
    LineAttribute lattr;
    lattr = new LineAttribute(LineAttribute.SOLID, Color.red);
    /*
     * Associate the attribute and the line
     * with the graph.
     */
    graph.setData(data, lattr);

    return mainPane_;
  }

    void tree_actionPerformed(java.awt.event.ActionEvent e) {
        JClassTree ct = new JClassTree();
        ct.setModal(false);
        ct.setJPane(mainPane_);
        ct.show();
    }

  class MyAction implements java.awt.event.ActionListener {
        public void actionPerformed(java.awt.event.ActionEvent event) {
           Object obj = event.getSource();
	   if(obj == space_) {
	     System.out.println("  <<Mark>>");
	   }
	   if(obj == tree_)
	       tree_actionPerformed(event);
        }
    }

}
