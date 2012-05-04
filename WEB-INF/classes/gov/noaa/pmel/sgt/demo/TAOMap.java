/*
 * $Id: TAOMap.java,v 1.16 2002/06/14 21:43:10 dwd Exp $
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
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.PointCollectionKey;
import gov.noaa.pmel.sgt.PointCartesianRenderer;
import gov.noaa.pmel.sgt.StackedLayout;
import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.swing.JClassTree;
import gov.noaa.pmel.sgt.Logo;

import gov.noaa.pmel.sgt.swing.ValueIcon;
import gov.noaa.pmel.sgt.swing.ValueIconFormat;
import gov.noaa.pmel.sgt.swing.prop.PointAttributeDialog;

import gov.noaa.pmel.sgt.dm.Collection;
import gov.noaa.pmel.sgt.dm.SimplePoint;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.dm.SimpleLine;
import gov.noaa.pmel.sgt.dm.SGTMetaData;

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.SoTPoint;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Example demonstrating the creation of a graph that displays point
 * data from files and includes a coastline.  <code>TAOMap</code>
 * constructs the plot from basic <code>sgt</code> objects and uses
 * the <code>JPane</code> <code>PropertyChangeEvent</code>s to notify
 * <code>TAOMap</code> of zoom requests and object selections.
 *
 * @author Donald Denbo
 * @version $Revision: 1.16 $, $Date: 2002/06/14 21:43:10 $
 * @since 2.0
 */
public class TAOMap extends JApplet implements PropertyChangeListener {
  JButton tree_;
  JButton space_ = null;
  JButton reset_;
  JPane mainPane_;
  SGTLine coastLine_ = null;
  CartesianGraph graph_;
  LinearTransform xt_, yt_;
  Layer layer_;
  PlainAxis xbot_;
  PlainAxis yleft_;
  Range2D xrange_, yrange_;
  PointAttributeDialog pAttrDialog_ = null;

  public void init() {
    /*
     * init() is called when TAOMap is run as an JApplet.
     */
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(553,438);
    JPane graph = makeGraph();
    graph.setBatch(true);

    getContentPane().add(graph, "Center");
    getContentPane().add(makeButtonPanel(false), "South");
    addValueIcon();
    graph.setBatch(false);
  }

  JPanel makeButtonPanel(boolean mark) {
    /*
     * Create the buttonPanel.  Leave the "mark" button off when
     * creating the panel for a JApplet.  The mark button is used for
     * debugging events.
     */
    JPanel button = new JPanel();
    button.setLayout(new FlowLayout());
    /*
     * Create button to open a JClassTree dialog
     */
    tree_ = new JButton("Tree View");
    MyAction myAction = new MyAction();
    tree_.addActionListener(myAction);
    button.add(tree_);
    if(mark) {
      /*
       * Create the <<mark>> button
       */
      space_ = new JButton("Add Mark");
      space_.addActionListener(myAction);
      button.add(space_);
    }
    /*
     * Create the zoom reset button.
     */
    reset_ = new JButton("Reset Zoom");
    reset_.addActionListener(myAction);
    button.add(reset_);
    return button;
  }

  public static void main(String[] args) {
    /*
     * main(String[] args) is called when TAOMap is run as an
     * application
     */
    TAOMap pd = new TAOMap();
    /*
     * Create a JFrame to place TAOMap into.
     */
    JFrame frame = new JFrame("TAO Mooring Map");
    JPane graph;
    JPanel button;
    frame.getContentPane().setLayout(new BorderLayout());
    /*
     * Add listener to properly dispose of window when closed.
     */
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
    /*
     * make buttonPanel with "mark" button.
     */
    button = pd.makeButtonPanel(true);
    /*
     * set batch to true.  Changes to graph will not cause updates of
     * the display.
     */
    graph.setBatch(true);
    frame.getContentPane().add(graph, BorderLayout.CENTER);
    frame.getContentPane().add(button, BorderLayout.SOUTH);
    frame.pack();
    frame.setVisible(true);
    pd.addValueIcon();
    /*
     * set batch to false. Any changes to graph will now cause graph
     * to redraw.
     */
    graph.setBatch(false);

  }

  JPane makeGraph() {
    /*
     * This example creates a very simple plot from
     * scratch (not using one of the gov.noaa.pmel.sgt.swing classes)
     * to display a Collection of points.
     */
    coastLine_ = getCoastLine("finerezcoast.bin", 50200);
    /*
     * Create a Pane, place in the center of the Applet
     * and set the layout to be StackedLayout.
     */
    mainPane_ = new JPane("Point Plot Demo", new Dimension(553,438));
    mainPane_.setLayout(new StackedLayout());
    mainPane_.setBackground(Color.white);
    /*
     * Read two point collections, TAO.dat and TRITON.dat.  These
     * files contain coordinate and labelling information.
     *
     */
    Collection TAO;
    Collection TRITON;
    TAO = readPointCollection("TAO.dat");
    TRITON = readPointCollection("TRITON.dat");
    /*
     * Although Collection has methods to query the x and y range of
     * the coordinates we set the range to produce a "nice" graph.
     */
    xrange_ = new Range2D(130.0, 270., 20.0);
    yrange_ = new Range2D(-10.0, 14.0, 2.0);
    /*
     * xsize, ysize are the width and height in physical units
     * of the Layer graphics region.
     *
     * xstart, xend are the start and end points for the X axis
     * ystart, yend are the start and end points for the Y axis
     */
    double xsize = 4.0;
    double xstart = 0.45;
    double xend = 3.75;
    double ysize = 3.0;
    double ystart = 0.45;
    double yend = 2.95;

    /*
     * Create the transforms that will be used for all layers
     */
    xt_ = new LinearTransform(xstart, xend, xrange_.start, xrange_.end);
    yt_ = new LinearTransform(ystart, yend, yrange_.start, yrange_.end);

    if(coastLine_ != null) {
      /*
       * Create the layer that will hold the coastline and add to the
       * JPane.
       */
      Layer layer = new Layer("CoastLine", new Dimension2D(xsize, ysize));
      mainPane_.add(layer);
      /*
       * Create the CartesianGraph, attach to the layer, set the
       * transforms and add the coastline data.
       */
      CartesianGraph cstgraph = new CartesianGraph("CoastLine Graph");
      layer.setGraph(cstgraph);
      cstgraph.setXTransform(xt_);
      cstgraph.setYTransform(yt_);
      LineAttribute lattr = new LineAttribute();
      lattr.setColor(new Color(165, 42, 42));
      cstgraph.setData(coastLine_, lattr);
      /*
       * Since the coastline is for the entire world we clip the
       * display to the current axes.
       */
      cstgraph.setClip(xrange_.start, xrange_.end,
                      yrange_.start, yrange_.end);
      cstgraph.setClipping(true);
    }
    /*
     * Create the first "data" layer.  This object reference is made
     * class wide for convience later.
     */
    layer_ = new Layer("Layer 1", new Dimension2D(xsize, ysize));
    mainPane_.add(layer_);
    /*
     * Create a CartesianGraph and set the transforms.
     */
    graph_ = new CartesianGraph("Point Graph");
    layer_.setGraph(graph_);
    graph_.setXTransform(xt_);
    graph_.setYTransform(yt_);
    /*
     * This Graph contain the plots axes.
     * Create the bottom axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    String xLabel = "Longitude";

    xbot_ = new PlainAxis("Botton Axis");
    xbot_.setRangeU(xrange_);
    xbot_.setLocationU(new Point2D.Double(xrange_.start, yrange_.start));
    Font xbfont = new Font("Helvetica", Font.ITALIC, 14);
    xbot_.setLabelFont(xbfont);
    SGLabel xtitle = new SGLabel("xaxis title", xLabel,
                                 new Point2D.Double(0.0, 0.0));
    Font xtfont = new Font("Helvetica", Font.PLAIN, 14);
    xtitle.setFont(xtfont);
    xtitle.setHeightP(0.2);
    xbot_.setTitle(xtitle);
    graph_.addXAxis(xbot_);
    /*
     * Create the left axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    String yLabel = "Latitude";

    yleft_ = new PlainAxis("Left Axis");
    yleft_.setRangeU(yrange_);
    yleft_.setLocationU(new Point2D.Double(xrange_.start, yrange_.start));
    yleft_.setLabelFont(xbfont);
    SGLabel ytitle = new SGLabel("yaxis title", yLabel,
                                 new Point2D.Double(0.0, 0.0));
    Font ytfont = new Font("Helvetica", Font.PLAIN, 14);
    ytitle.setFont(ytfont);
    ytitle.setHeightP(0.2);
    yleft_.setTitle(ytitle);
    graph_.addYAxis(yleft_);
    /*
     * create the point collection key
     */
    PointCollectionKey key = new PointCollectionKey();
    key.setId("Mooring Key");
    key.setVAlign(PointCollectionKey.TOP);
    key.setHAlign(PointCollectionKey.LEFT);
    key.setBorderStyle(PointCollectionKey.PLAIN_LINE);
    key.setLocationP(new Point2D.Double(xstart+0.1, yend));
    layer_.addChild(key);
    /*
     * Create a PointAttribute for the display of the
     * Collection of points. The points will have a color or rgb
     * (200,0,255) with the label at the NE corner and in a dark red.
     */
    PointAttribute pattr;
    Color markColor = new Color(200, 0, 255);
    pattr = new PointAttribute(44, markColor);
    pattr.setLabelPosition(PointAttribute.NE);
    Font pfont = new Font("Helvetica", Font.PLAIN, 12);
    pattr.setLabelFont(pfont);
    pattr.setLabelColor(Color.red.darker());
    pattr.setLabelHeightP(0.08);
    pattr.setDrawLabel(true);
    /*
     * Associate the attribute and the point Collection
     * with the graph.
     */
    graph_.setData(TAO, pattr);
    SGLabel pointTitle = new SGLabel("TAO title",
                                     "TAO Moorings",
                                     new Point2D.Double(0.0, 0.0));
    pointTitle.setHeightP(0.16);
    key.addPointGraph((PointCartesianRenderer)graph_.getRenderer(),
                      pointTitle);
    /*
     * create second layer for second file
     */
    Layer layer2 = new Layer("Layer 2", new Dimension2D(xsize, ysize));
    mainPane_.add(layer2);
    /*
     * Create the graph and set the shared transforms.  NOTE: this
     * graph will not contain any axes.
     */
    CartesianGraph graph2 = new CartesianGraph("Point Graph2");
    layer2.setGraph(graph2);
    graph2.setXTransform(xt_);
    graph2.setYTransform(yt_);

    PointAttribute pattr2;
    pattr2 = new PointAttribute(13, markColor);
    pattr2.setLabelPosition(PointAttribute.NE);
    pattr2.setLabelFont(pfont);
    pattr2.setLabelColor(Color.blue.darker());
    pattr2.setLabelHeightP(0.08);
    pattr2.setDrawLabel(true);

    graph2.setData(TRITON, pattr2);
    SGLabel pointTitle2 = new SGLabel("TRITON title",
                                      "TRITON Moorings",
                                      new Point2D.Double(0.0, 0.0));
    pointTitle2.setHeightP(0.16);
    key.addPointGraph((PointCartesianRenderer)graph2.getRenderer(),
                      pointTitle2);
    mainPane_.addPropertyChangeListener(this);

    return mainPane_;
  }

  private void addValueIcon() {
    /*
     * add a ValueIcon.  The value icon is used to show the current
     * value for the user coordinates.
     */
    ValueIcon vi = new ValueIcon(getClass().getResource("query.gif"), "value");

    layer_.addChild(vi);
    vi.setId("local query");
    try {
      vi.setLocationU(new SoTPoint(220.0, -5.0));
    } catch (java.beans.PropertyVetoException e) {
      e.printStackTrace();
    }
    vi.setVisible(true);
    vi.setSelectable(true);
    vi.setValueFormat(new GeoFormat());

  }

  void tree_actionPerformed(ActionEvent e) {
    /*
     * Create a JClassTree to display a graph of the sgt objects
     */
    JClassTree ct = new JClassTree();
    ct.setModal(false);
    ct.setJPane(mainPane_);
    ct.show();
  }

  class MyAction implements java.awt.event.ActionListener {
    public void actionPerformed(ActionEvent event) {
      Object obj = event.getSource();
      if(obj == space_) {
        System.out.println("  <<Mark>>");
      }
      if(obj == tree_)
        tree_actionPerformed(event);
      if(obj == reset_)
        reset_actionPerformed(event);
    }
  }

  Collection readPointCollection(String file) {
    /*
     * method used to parse the station information.
     * The file format is
     * lat;lon;label
     */
    SimplePoint sp = null;
    BufferedReader in = null;
    String line = null;
    String title;
    int lat, lon;
    Collection pc = new Collection(file);
    /*
     * Get data file.  The getResourceAsStream enables java to find
     * the data inside of a jar file.
     */
    InputStream is = getClass().getResourceAsStream(file);
    /*
     * Open reader and get first line
     */
    try {
      in = new BufferedReader(new InputStreamReader(is));
      line = in.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
    while(line != null) {
      StringTokenizer st = new StringTokenizer(line, ";");
      String token1 = st.nextToken();
      String token2 = st.nextToken();
      title = st.nextToken();
      lat = Integer.parseInt(token1);
      lon = Integer.parseInt(token2);
      if(lon < 0) lon = lon + 360;
      lon = lon%360;
      /*
       * Create new SimplePoint with coordinates and title and
       * add to Collection.
       */
      sp = new SimplePoint((double)lon, (double)lat, title);
      pc.addElement(sp);
      /*
       * Read the next line
       */
      try {
        line = in.readLine();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return pc;
  }
  SGTLine getCoastLine(String coastFileName, int bufsize) {
    /*
     * Read coastline file
     */
    SimpleLine line;
    if(coastFileName.length() == 0) return null;

    double[] lat = new double[bufsize];
    double[] lon = new double[bufsize];
    int cpt = 0;
    int cpt_save = 0;
    int cut_count = 0;
    InputStream is = getClass().getResourceAsStream(coastFileName);
    try {
      BufferedInputStream bis;
      bis = new BufferedInputStream(is, 1000000);
      DataInputStream inData = new DataInputStream(bis);
      while (true) {
        // get the number of entries
        int numEntries = inData.readShort();
        if (numEntries == -1)
          break;

        // get the lats and lons for this segment
        cpt_save = cpt;
        for (int i=0; i<numEntries; i++) {
          lat[cpt] = inData.readDouble();
          cpt++;
        }

        cpt = cpt_save;
        for (int i=0; i<numEntries; i++) {
          lon[cpt] = (inData.readDouble() + 360)%360.0;
          if(cpt > 0 && Math.abs(lon[cpt] - lon[cpt-1]) > 50.0) {
            cut_count++;
          }
          cpt++;
        }

        lat[cpt] = Double.NaN;
        lon[cpt] = Double.NaN;
        cpt++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println(cpt + " points read");
    System.out.println(cut_count + " cuts added");
    /*
     * The coastline file is setup for +180/-180 range for
     * longitudes.  Since TAO and TRITON span the pacific we
     * need to remap longitude to 0-360. Additional points need
     * to be added so that lines don't wrap strangely.
     */
    int length = cpt;
    double[] latVal = new double[cpt + cut_count];
    double[] lonVal = new double[cpt + cut_count];
    cpt = 0;
    for(int i=0; i < length; i++) {
      if(i > 0 && Math.abs(lon[i] - lon[i-1]) > 50.0) {
        latVal[cpt] = Double.NaN;
        lonVal[cpt] = Double.NaN;
        cpt++;
      }
      latVal[cpt] = lat[i];
      lonVal[cpt] = lon[i];
      cpt++;
    }

    line = new SimpleLine(lonVal, latVal, "CoastLine");
    SGTMetaData yMeta = new SGTMetaData("Latitude", "degrees_N",
                                        false, false);
    SGTMetaData xMeta = new SGTMetaData("Longitude", "degrees_E",
                                        false, true);
    line.setXMetaData(xMeta);
    line.setYMetaData(yMeta);

    return line;
  }

  public void propertyChange(PropertyChangeEvent event) {
    /*
     * Listen for propery change events from JPane.
     */
    String name = event.getPropertyName();
    if(name.equals("zoomRectangle")) {
      /*
       * compute zoom rectangle in user units
       */
      Range2D xr = new Range2D();
      Range2D yr = new Range2D();
      Rectangle zm = (Rectangle)event.getNewValue();
      if(zm.width <= 1 || zm.height <= 1) return;
      xr.start = graph_.getXPtoU(layer_.getXDtoP(zm.x));
      xr.end = graph_.getXPtoU(layer_.getXDtoP(zm.x + zm.width));
      if(xr.start > xr.end) {
        double temp = xr.start;
        xr.start = xr.end;
        xr.end = temp;
      }
      yr.start = graph_.getYPtoU(layer_.getYDtoP(zm.y));
      yr.end = graph_.getYPtoU(layer_.getYDtoP(zm.y + zm.height));
      if(yr.start > yr.end) {
        double temp = yr.start;
        yr.start = yr.end;
        yr.end = temp;
      }
      mainPane_.setBatch(true);
      /*
       * set range for transforms
       */
      xt_.setRangeU(xr);
      yt_.setRangeU(yr);
      /*
       * set range and origin for axes
       */
      Point2D.Double orig = new Point2D.Double(xr.start, yr.start);
      xbot_.setRangeU(xr);
      xbot_.setLocationU(orig);

      yleft_.setRangeU(yr);
      yleft_.setLocationU(orig);
      /*
       * set clipping on all graphs
       */
      Component[] comps = mainPane_.getComponents();
      Layer ly;
      for(int i=0; i < comps.length; i++) {
        if(comps[i] instanceof Layer) {
          ly = (Layer)comps[i];
          ((CartesianGraph)ly.getGraph()).setClip(xr.start, xr.end,
                                                  yr.start, yr.end);
        }
      }
      mainPane_.setBatch(false);
    } else if(name.equals("objectSelected")) {
      /*
       * An sgt object has been selected.
       * If it is a PointCartesianRenderer that means the key has been
       * selected and so open a dialog to modified the PointAttribute.
       */
      if(event.getNewValue() instanceof PointCartesianRenderer) {
        PointAttribute pattr =
          ((PointCartesianRenderer)event.getNewValue()).getPointAttribute();
        if(pAttrDialog_ == null) {
          pAttrDialog_ = new PointAttributeDialog();
        }
        pAttrDialog_.setPointAttribute(pattr, mainPane_);
        pAttrDialog_.setVisible(true);
      } else {
        /*
         * Print the name of the object selected.
         */
        System.out.println("objectSelected = " + event.getNewValue());
      }
    }
  }

  void reset_actionPerformed(ActionEvent e) {
    mainPane_.setBatch(true);
    Layer ly;
    /*
     * clear clipping on all graphs but coast line.
     */
    Component[] comps = mainPane_.getComponents();
    for(int i=0; i < comps.length; i++) {
      if(comps[i] instanceof Layer) {
        ly = (Layer)comps[i];
        if(ly.getId().equals("CoastLine")) {
          ((CartesianGraph)ly.getGraph()).setClip(xrange_.start,
                                                  xrange_.end,
                                                  yrange_.start,
                                                  yrange_.end);
        } else {
          ((CartesianGraph)ly.getGraph()).setClipping(false);
        }
      }
    }
    /*
     * reset range for transform
     */
    xt_.setRangeU(xrange_);
    yt_.setRangeU(yrange_);
    /*
     * reset range for axes and origin
     */
    Point2D.Double orig = new Point2D.Double(xrange_.start,
                                             yrange_.start);
    xbot_.setRangeU(xrange_);
    xbot_.setLocationU(orig);
    yleft_.setRangeU(yrange_);
    yleft_.setLocationU(orig);

    mainPane_.setBatch(false);
  }

  class GeoFormat extends ValueIconFormat {
    /*
     * Create a specialized format for ValueIcon so that it handles
     * longitude wrapping properly and hemisphere labelling.
     */
    public GeoFormat() {
      super("#####.##;#####.##W", "#####.##N;#####.##S");
      xfrm_.setPositiveSuffix("E");
    }
    public String format(double x, double y) {
      double xt = (x + 360.0) % 360.0;
      if(x > 180.0) x = x - 360.0;
      return "(" + xfrm_.format(x) + ", " + yfrm_.format(y) + ")";
    }
  }

}
