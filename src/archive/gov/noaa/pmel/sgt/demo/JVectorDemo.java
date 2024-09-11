/*
 * $Id: JVectorDemo.java,v 1.6 2001/12/11 21:31:43 dwd Exp $
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

import gov.noaa.pmel.sgt.swing.JPlotLayout;
import gov.noaa.pmel.sgt.swing.JClassTree;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.AbstractPane;
import gov.noaa.pmel.sgt.VectorAttribute;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.SGTVector;
import gov.noaa.pmel.sgt.swing.prop.VectorAttributeDialog;


import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.IllegalTimeValue;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionEvent;

import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;

/**
 * Example demonstrating how to use <code>JPlotLayout</code>
 * to create a raster-contour plot.
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2001/12/11 21:31:43 $
 * @since 2.1
 */
public class JVectorDemo extends JApplet {
  static JPlotLayout rpl_;
  private VectorAttribute vectorAttr_;
  JButton edit_;
  JButton space_ = null;
  JButton tree_;
  JButton print_ = null;

  public void init() {
    /*
     * Create the demo in the JApplet environment.
     */
    getContentPane().setLayout(new BorderLayout(0,0));
    setBackground(java.awt.Color.white);
    setSize(600,430);
    JPanel main = new JPanel();
    rpl_ = makeGraph();
    JPanel button = makeButtonPanel(false);
    rpl_.setBatch(true);
    main.add(rpl_, BorderLayout.CENTER);
    getContentPane().add(main, "Center");
    getContentPane().add(button, "South");
    rpl_.setBatch(false);
  }

  JPanel makeButtonPanel(boolean app) {
    JPanel button = new JPanel();
    button.setLayout(new FlowLayout());
    MyAction myAction = new MyAction();
    if(app) {
      print_ = new JButton("Print...");
      print_.addActionListener(myAction);
      button.add(print_);
    }
    tree_ = new JButton("Tree View");
    tree_.addActionListener(myAction);
    button.add(tree_);
    edit_ = new JButton("Edit VectorAttribute");
    edit_.addActionListener(myAction);
    button.add(edit_);
    /*
     * Optionally leave the "mark" button out of the button panel
     */
    if(app) {
      space_ = new JButton("Add Mark");
      space_.addActionListener(myAction);
      button.add(space_);
    }
    return button;
  }
  public static void main(String[] args) {
    /*
     * Create the demo as an application
     */
    JVectorDemo vd = new JVectorDemo();
    /*
     * Create a new JFrame to contain the demo.
     */
    JFrame frame = new JFrame("Vector Demo");
    JPanel main = new JPanel();
    main.setLayout(new BorderLayout());
    frame.setSize(600,400);
    frame.getContentPane().setLayout(new BorderLayout());
    /*
     * Listen for windowClosing events and dispose of JFrame
     */
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent event) {
        JFrame fr = (JFrame)event.getSource();
        fr.setVisible(false);
        fr.dispose();
        System.exit(0);
      }
    });
    /*
     * Create button panel with "mark" button
     */
    JPanel button = vd.makeButtonPanel(true);
    /*
     * Create JPlotLayout and turn batching on.  With batching on the
     * plot will not be updated as components are modified or added to
     * the plot tree.
     */
    rpl_ = vd.makeGraph();
    rpl_.setBatch(true);
    /*
     * Layout the plot and buttons.
     */
    main.add(rpl_, BorderLayout.CENTER);
    frame.getContentPane().add(main, BorderLayout.CENTER);
    frame.getContentPane().add(button, BorderLayout.SOUTH);
    frame.pack();
    /*
     * Turn batching off. JPlotLayout will redraw if it has been
     * modified since batching was turned on.
     */
    rpl_.setBatch(false);

    frame.setVisible(true);
  }

  void print_actionPerformed(ActionEvent e) {
    Color saveColor;

    PrinterJob printJob = PrinterJob.getPrinterJob();
    printJob.setPrintable(rpl_);
    printJob.setJobName("Vector Demo");
    if(printJob.printDialog()) {
      try {
        saveColor = rpl_.getBackground();
        if(!saveColor.equals(Color.white)) {
          rpl_.setBackground(Color.white);
        }
        rpl_.setPageAlign(AbstractPane.TOP,
                          AbstractPane.CENTER);
        RepaintManager currentManager = RepaintManager.currentManager(rpl_);
        currentManager.setDoubleBufferingEnabled(false);
        printJob.print();
        currentManager.setDoubleBufferingEnabled(true);
        rpl_.setBackground(saveColor);
      } catch (PrinterException pe) {
        System.out.println("Error printing: " + pe);
      }
    }

  }

  void edit_actionPerformed(ActionEvent e) {
    /*
     * Create a GridAttributeDialog and set the renderer.
     */
     VectorAttributeDialog vad = new VectorAttributeDialog();
     vad.setJPane(rpl_);
     vad.setVectorAttribute(vectorAttr_);
     vad.setVisible(true);
  }

    void tree_actionPerformed(ActionEvent e) {
      /*
       * Create a JClassTree for the JPlotLayout objects
       */
        JClassTree ct = new JClassTree();
        ct.setModal(false);
        ct.setJPane(rpl_);
        ct.show();
    }

  JPlotLayout makeGraph() {
    /*
     * This example uses a pre-created "Layout" for raster time
     * series to simplify the construction of a plot. The
     * JPlotLayout can plot a single grid with
     * a ColorKey, time series with a LineKey, point collection with a
     * PointCollectionKey, and general X-Y plots with a
     * LineKey. JPlotLayout supports zooming, object selection, and
     * object editing.
     */
    SGTGrid uComp;
    SGTGrid vComp;
    SGTVector vector;
    TestData td;
    JPlotLayout rpl;
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
    vector = new SGTVector(uComp, vComp);
    /*
     * Create the layout without a Logo image and with the
     * VectorKey on the graph Pane.
     */
    rpl = new JPlotLayout(JPlotLayout.VECTOR,
                          false, false, "test layout", null, false);
    rpl.setEditClasses(false);
    vectorAttr_ = new VectorAttribute(0.0075, Color.red);
    vectorAttr_.setHeadScale(0.5);
    /*
     * Add the grid to the layout and give a label for
     * the VectorKey.
     */
    rpl.addData(vector, vectorAttr_, "First Data");
    /*
     * Change the layout's three title lines.
     */
    rpl.setTitles("Vector Plot Demo",
                  "using a JPlotLayout",
                  "");
    /*
     * Resize the graph  and place in the "Center" of the frame.
     */
    rpl.setSize(new Dimension(600, 400));
    return rpl;
  }

  class MyAction implements java.awt.event.ActionListener {
        public void actionPerformed(java.awt.event.ActionEvent event) {
           Object obj = event.getSource();
           if(obj == edit_) {
             edit_actionPerformed(event);
           } else if(obj == space_) {
             System.out.println("  <<Mark>>");
           } else if(obj == tree_) {
               tree_actionPerformed(event);
           } else if(obj == print_) {
               print_actionPerformed(event);
           }
        }
    }
}



