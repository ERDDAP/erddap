/*
 * $Id: JPointDemo.java,v 1.7 2001/02/06 00:14:35 dwd Exp $
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
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.LineCartesianRenderer;
import gov.noaa.pmel.sgt.StackedLayout;
import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.swing.JClassTree;
import gov.noaa.pmel.sgt.Logo;

import gov.noaa.pmel.sgt.dm.Collection;

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Dimension2D;

import java.awt.*;
import javax.swing.*;

/**
 * Example demonstrating the creation of a simple 
 * graph of many points.
 * 
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2001/02/06 00:14:35 $
 * @since 2.0
 */

public class JPointDemo extends JApplet {
    JButton tree_;
    JButton space_;
    JPane mainPane_;
  
  public void init() {
    setLayout(new BorderLayout(0,0));
    setSize(553,438);

    add(makeGraph(), BorderLayout.CENTER);
  }

  public static void main(String[] args) {
    JPointDemo pd = new JPointDemo();
    JFrame frame = new JFrame("Point Demo");
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
     * to display a Collection of points.
     */
    /*
     * Create a Pane, place in the center of the Applet
     * and set the layout to be StackedLayout.
     */
    mainPane_ = new JPane("Point Plot Demo", new Dimension(553,438));
    mainPane_.setLayout(new StackedLayout());
    mainPane_.setBackground(Color.white);
    /*
     * Create a Collection of points using the TestData class.
     */
    Range2D xrange, yrange;
    TestData td;
    Collection col;
    xrange = new Range2D(50.0, 150., 10.0);
    yrange = new Range2D(-20.0, 20.0, 5.0);
    td = new TestData(xrange, yrange, 50);
    col = td.getCollection();
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
    * create and add image as a Logo to the layer
    */
    Image img = this.getToolkit().getImage(getClass().getResource("ncBrowse48.gif"));
    //
    // wait for image to be loaded
    //
    if(img != null) {
      MediaTracker mt = new MediaTracker(this);
      try {
	mt.addImage(img, 0);
	mt.waitForAll();
	if(mt.isErrorAny())
	  System.err.println("JPointDemo: Error loading image");
      } catch (InterruptedException e) {}
    }
    Logo logo = new Logo(new Point2D.Double(0.0, 0.0), Logo.BOTTOM, Logo.LEFT);
    logo.setId("ncBrowse logo");
    logo.setImage(img);
    layer.addChild(logo);
    /*
     * Create a CartesianGraph and transforms.
     */
    CartesianGraph graph;
    LinearTransform xt, yt;

    graph = new CartesianGraph("Point Graph");
    layer.setGraph(graph);
    xt = new LinearTransform(xstart, xend, xrange.start, xrange.end);
    yt = new LinearTransform(ystart, yend, yrange.start, yrange.end);
    graph.setXTransform(xt);
    graph.setYTransform(yt);
    /*
     * Create the bottom axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    PlainAxis xbot;
    String xLabel = "X Label";

    xbot = new PlainAxis("Botton Axis");
    xbot.setRangeU(xrange);
    xbot.setLocationU(new Point2D.Double(xrange.start, yrange.start));
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
    PlainAxis yleft;
    String yLabel = "Y Label";

    yleft = new PlainAxis("Left Axis");
    yleft.setRangeU(yrange);
    yleft.setLocationU(new Point2D.Double(xrange.start, yrange.start));
    yleft.setLabelFont(xbfont);
    SGLabel ytitle = new SGLabel("yaxis title", yLabel, 
                                 new Point2D.Double(0.0, 0.0));
    Font ytfont = new Font("Helvetica", Font.PLAIN, 14);
    ytitle.setFont(ytfont);
    ytitle.setHeightP(0.2);
    yleft.setTitle(ytitle);
    graph.addYAxis(yleft);
    /*
     * Create a PointAttribute for the display of the
     * Collection of points. The points will be red with
     * the label at the NE corner and in blue.
     */
    PointAttribute pattr;

    pattr = new PointAttribute(20, Color.red);
    pattr.setLabelPosition(PointAttribute.NE);
    Font pfont = new Font("Helvetica", Font.PLAIN, 12);
    pattr.setLabelFont(pfont);
    pattr.setLabelColor(Color.blue);
    pattr.setLabelHeightP(0.1);
    pattr.setDrawLabel(true);
    /*
     * Associate the attribute and the point Collection
     * with the graph.
     */
    graph.setData(col, pattr);
    
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
