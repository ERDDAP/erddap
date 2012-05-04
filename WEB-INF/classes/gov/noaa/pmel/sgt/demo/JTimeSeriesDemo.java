/*
 * $Id: JTimeSeriesDemo.java,v 1.8 2003/08/22 23:02:38 dwd Exp $
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
import gov.noaa.pmel.sgt.swing.prop.LineAttributeDialog;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.LineCartesianRenderer;
import gov.noaa.pmel.sgt.LineAttribute;

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.util.IllegalTimeValue;

import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTLine;

import java.awt.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
/**
 * Example demonstrating how to use <code>JPlotLayout</code>
 * to create a time series plot.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2003/08/22 23:02:38 $
 * @since 2.0
 */

public class JTimeSeriesDemo extends JApplet {
  JButton tree_;
  JButton space_ = null;
  JPane pane_;
  MyMouse myMouse_;
  LineAttributeDialog lad_;

  public void init() {
    /*
     * init is used when run as an JApplet
     */
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(600,500);
    pane_ = makeGraph();
    pane_.setBatch(true);
    JPanel button = makeButtonPanel(false);
    getContentPane().add(pane_, BorderLayout.CENTER);
    getContentPane().add(button, "South");
    pane_.setBatch(false);
  }

  JPanel makeButtonPanel(boolean mark) {
    JPanel button = new JPanel();
    button.setLayout(new FlowLayout());
    tree_ = new JButton("Tree View");
    MyAction myAction = new MyAction();
    tree_.addActionListener(myAction);
    button.add(tree_);
    /*
     * optionally include "mark" button
     */
    if(mark) {
      space_ = new JButton("Add Mark");
      space_.addActionListener(myAction);
      button.add(space_);
    }
    return button;
  }

  public static void main(String[] args) {
    /*
     * main() is used when run as an application
     */
    JTimeSeriesDemo tsd = new JTimeSeriesDemo();
    /*
     * Create a JFrame to run JTimeSeriesDemo in.
     */
    JFrame frame = new JFrame("Time Series Demo");
    JPanel button = tsd.makeButtonPanel(true);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent event) {
          JFrame fr = (JFrame)event.getSource();
          fr.setVisible(false);
          fr.dispose();
          System.exit(0);
        }
      });
    tsd.pane_ = tsd.makeGraph();
    tsd.pane_.setBatch(true);
    frame.setSize(600, 500);
    frame.getContentPane().add(tsd.pane_, BorderLayout.CENTER);
    frame.getContentPane().add(button, BorderLayout.SOUTH);
    frame.setVisible(true);
    tsd.pane_.setBatch(false);
  }

  JPlotLayout makeGraph() {
    /*
     * This example uses a pre-created "Layout" for time
     * series to simplify the construction of a plot. The
     * LineTimeSeriesLayout can plot multiple lines, with
     * a legend and provides zooming and line hi-lighting
     * capabilities.
     */
    SGTData newData;
    TestData td;
    JPlotLayout ltsl;
    /*
     * Create a test time series with random data.
     */
    GeoDate start = new GeoDate();
    GeoDate stop = new GeoDate();
    try {
      start = new GeoDate("1968-11-01", "yyyy-MM-dd");
      stop  = new GeoDate("2001-02-20", "yyyy-MM-dd");
    } catch (IllegalTimeValue e) {}
    TimeRange tr = new TimeRange(start, stop);
    td = new TestData(TestData.TIME_SERIES, tr, 10.0f,
                      TestData.RANDOM, 1.2f, 0.5f, 30.0f);
    newData = td.getSGTData();
    System.out.println("series length = " + ((SGTLine)newData).getYArray().length);
    /*
     * Create the layout without a Logo image and with the
     * LineKey on the main Pane object.
     */
    ltsl = new JPlotLayout(newData, "Time Series Demo", null, false);
    /*
     * Add the time series to the layout and give a label for
     * the legend.
     */
    ltsl.addData(newData, "Random Data");
    /*
     * Change the layout's three title lines and place the Pane
     * on the Applet.
     */
    ltsl.setTitles("Time Series Demo",
                   "using JPlotLayout",
                   "");

    myMouse_ = new MyMouse();
    ltsl.addMouseListener(myMouse_);

    return ltsl;
  }

  void tree_actionPerformed(java.awt.event.ActionEvent e) {
    /*
     * Create JClassTree showing object tree.
     */
    JClassTree ct = new JClassTree();
    ct.setModal(false);
    ct.setJPane(pane_);
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

  class MyMouse extends MouseAdapter {
    /*
     * process mouse events.
     */
    public void mouseReleased(MouseEvent event) {
      Object object = event.getSource();
      if(object == pane_)
        maybeShowLineAttributeDialog(event);
    }

    void maybeShowLineAttributeDialog(MouseEvent e) {
      if(e.isPopupTrigger() || e.getClickCount() == 2) {
        Object obj = pane_.getObjectAt(e.getX(), e.getY());
        pane_.setSelectedObject(obj);
        if(obj instanceof LineCartesianRenderer) {
          LineAttribute attr = ((LineCartesianRenderer)obj).getLineAttribute();
          if(lad_ == null) {
            lad_ = new LineAttributeDialog();
          }
          lad_.setLineAttribute(attr);
          if(!lad_.isShowing())
            lad_.setVisible(true);
        }
      }
    }
  }
}
