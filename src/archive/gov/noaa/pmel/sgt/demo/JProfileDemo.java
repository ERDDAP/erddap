/*
 * $Id: JProfileDemo.java,v 1.3 2001/02/06 00:14:35 dwd Exp $
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
import gov.noaa.pmel.sgt.swing.JPlotLayout;
import gov.noaa.pmel.sgt.dm.SGTData;

import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Point2D;

import java.awt.*;
import javax.swing.*;
/**
 * Example demonstrating how to use <code>JPlotLayout</code> to create
 * a profile plot. 
 * 
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2001/02/06 00:14:35 $
 * @since 2.0
 */


public class JProfileDemo extends JApplet {
  public void init() {
    setLayout(null);
    setSize(450,600);
    add("Center", makeGraph());
  }

  public static void main(String[] args) {
    JProfileDemo pd = new JProfileDemo();
    JFrame frame = new JFrame("Profile Demo");
    JPane graph;
    frame.getContentPane().setLayout(new BorderLayout());
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent event) {
	JFrame fr = (JFrame)event.getSource();
	fr.setVisible(false);
	fr.dispose();
	System.exit(0);
      }
    });
    frame.setSize(450, 600);
    graph = pd.makeGraph();
    graph.setBatch(true);
    frame.getContentPane().add(graph, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);
    graph.setBatch(false);
  }

  JPlotLayout makeGraph() {
    /*
     * This example uses a pre-created "Layout" for profile
     * data to simplify the construction of a plot. The
     * LineProfileLayout can plot multiple lines, with
     * a legend and provides zooming and line hi-lighting
     * capabilities.
     */
    SGTData newData;
    TestData td;
    JPlotLayout lpl;
    /*
     * Create a test profile with random data.
     */
    Range2D zrange = new Range2D(0.0, 495.0, 10.0);
    td = new TestData(TestData.PROFILE, zrange,
                      TestData.RANDOM, 1.2f, 0.5f, 30.0f);
    newData = td.getSGTData();
    /*
     * Create the layout without a Logo image and with the
     * LineKey on the main Pane object.  Data object is used
     * to automatically determine the type of plot to create.
     */
    lpl = new JPlotLayout(newData, "Profile Demo", null, false);
    /*
     * Add first profile.
     */
    lpl.addData(newData, "First Line");
    /*
     * Create a second profile.
     */
    td = new TestData(TestData.PROFILE, zrange,
                      TestData.RANDOM, 2.0f, 0.25f, 30.0f);
    lpl.addData(td.getSGTData(), "Second Line");
    /*
     * Change the layout's three title lines and place the Pane
     * on the Applet.
     */
    lpl.setTitles("Profile Demo", 
                  "using a sgt.swing class", 
                  "JPlotLayout");
                  
    lpl.setSize(new Dimension(450,600));
    lpl.setLayerSizeP(new Dimension2D(6.0, 8.0));
    lpl.setKeyLocationP(new Point2D.Double(6.0, 8.0));
    return lpl;
  }
        
}
