/*
 * $Id: JLineProfileLayout.java,v 1.8 2003/08/22 23:02:39 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package  gov.noaa.pmel.sgt.swing;

import gov.noaa.pmel.sgt.LineKey;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.LineCartesianRenderer;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.PlainAxis;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Logo;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.LayerNotFoundException;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.AxisNotFoundException;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.SGException;
import gov.noaa.pmel.sgt.StackedLayout;

import gov.noaa.pmel.util.Domain;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
//import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.util.Units;

import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.dm.SimpleLine;
import gov.noaa.pmel.sgt.dm.SGTMetaData;
import gov.noaa.pmel.sgt.dm.Collection;


import java.util.Enumeration;
import java.util.Vector;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Component;

import java.beans.PropertyVetoException;

/**
 * JLineProfileLayout creates a pre-defined graphics layout for
 * profile data using LineCartesianGraph. This layout is application specific.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2003/08/22 23:02:39 $
 * @see LineCartesianRenderer
 * @Deprecated As of v2.0, replaced by {@link gov.noaa.pmel.sgt.swing.JPlotLayout}
**/
/*oodE***********************************************/
public class JLineProfileLayout extends JGraphicLayout {
  //
  // save handles to unique components
  //
  Logo logo_;
  LineKey lineKey_;
  int layerCount_;
  boolean zUp_ = true;
  Layer firstLayer_;
  boolean inZoom_ = false;
  //
  // constants
  //
  double xSize_ = XSIZE_;
  double xMin_ = XMIN_;
  double xMax_ = XMAX_;
  double ySize_ = YSIZE_;
  double yMin_ = YMIN_;
  double yMax_ = YMAX_;
  //
  double mainTitleHeight_ = MAIN_TITLE_HEIGHT_;
  double titleHeight_ = TITLE_HEIGHT_;
  double labelHeight_ = LABEL_HEIGHT_;
  double warnHeight_ = WARN_HEIGHT_;
  double keyHeight_ = KEY_HEIGHT_;
  //
  double xKeySize_ = XKEYSIZE_;
  double yKeySize_ = YKEYSIZE_;
  //
  Color paneColor_ = PANE_COLOR;
  Color keyPaneColor_ = KEYPANE_COLOR;

  //
  private static final Color[] colorList_ =
  { Color.blue, Color.cyan, Color.green, Color.orange.darker(),
    Color.red, Color.magenta, Color.black, Color.gray};
  //  {Color.red, Color.green, Color.blue, Color.cyan,
  //   Color.magenta, Color.yellow, Color.orange, Color.pink};
  private static final int[] markList_ =
  {1, 2, 9, 15, 10, 24, 11, 44};
  /**
   * Default constructor. No Logo image is used and the LineKey
   * will be in the same Pane.
   */
  public JLineProfileLayout() {
    this("", null, false);
  }
  /**
   * JLineProfileLayout constructor.
   *
   * @param id identifier
   * @param img Logo image
   * @param is_key_pane if true LineKey is in separate pane
   */
  public JLineProfileLayout(String id, Image img, boolean is_key_pane) {
    super(id, img, new Dimension(400,300));
    Layer layer, key_layer;
    CartesianGraph graph;
    LinearTransform xt, yt;
    PlainAxis xbot, yleft;
    double xpos, ypos;
    int halign;
    //
    // create Pane and descendants for the LineProfile layout
    //
    setOpaque(true);
    setLayout(new StackedLayout());
    setBackground(paneColor_);
    layer = new Layer("Layer 1", new Dimension2D(xSize_, ySize_));
    firstLayer_ = layer;
    add(layer,0);
    //
    lineKey_ = new LineKey();
    lineKey_.setSelectable(false);
    lineKey_.setId("Line Key");
    lineKey_.setVAlign(LineKey.TOP);
    if(is_key_pane) {
      lineKey_.setHAlign(LineKey.LEFT);
      lineKey_.setBorderStyle(LineKey.NO_BORDER);
      lineKey_.setLocationP(new Point2D.Double(0.0, yKeySize_));
      int xdim = 400;
      int ydim = (int)((double)xdim/xKeySize_*yKeySize_);
      keyPane_ = new JPane("KeyPane", new Dimension(xdim,ydim));
      keyPane_.setOpaque(true);
      keyPane_.setLayout(new StackedLayout());
      keyPane_.setBackground(keyPaneColor_);
      key_layer = new Layer("Key Layer", new Dimension2D(xKeySize_, yKeySize_));
      keyPane_.add(key_layer);
      key_layer.addChild(lineKey_);
    } else {
      lineKey_.setHAlign(LineKey.RIGHT);
      lineKey_.setLocationP(new Point2D.Double(xSize_ - 0.01, ySize_));
      layer.addChild(lineKey_);
    }
    //
    // add Icon
    //
    if(iconImage_ != null) {
      logo_ = new Logo(new Point2D.Double(0.0, ySize_), Logo.TOP, Logo.LEFT);
      logo_.setImage(iconImage_);
      layer.addChild(logo_);
      Rectangle bnds = logo_.getBounds();
      xpos = layer.getXDtoP(bnds.x + bnds.width) + 0.05;
      halign = SGLabel.LEFT;
    } else {
      xpos = (xMin_ + xMax_)*0.5;
      halign = SGLabel.CENTER;
    }
    //
    // title
    //
    ypos = ySize_ - 1.2f*mainTitleHeight_;
    Font titleFont = new Font("Helvetica", Font.BOLD, 14);
    mainTitle_ = new SGLabel("Line Profile Title",
                             "Profile Plot",
                             mainTitleHeight_,
                             new Point2D.Double(xpos, ypos),
                             SGLabel.BOTTOM,
                             halign);
    mainTitle_.setFont(titleFont);
    layer.addChild(mainTitle_);
    ypos = ypos - 1.2f*warnHeight_;
    Font title2Font = new Font("Helvetica", Font.PLAIN, 10);
    title2_ = new SGLabel("Warning",
                          "Warning: Browse image only",
                          warnHeight_,
                          new Point2D.Double(xpos, ypos),
                          SGLabel.BOTTOM,
                          halign);
    title2_.setFont(title2Font);
    layer.addChild(title2_);
    ypos = ypos - 1.1f*warnHeight_;
    title3_ = new SGLabel("Warning 2",
                          "Verify accuracy of plot before research use",
                          warnHeight_,
                          new Point2D.Double(xpos, ypos),
                          SGLabel.BOTTOM,
                          halign);
    title3_.setFont(title2Font);
    layer.addChild(title3_);
    //
    title3_.setSelectable(false);

    layerCount_ = 0;
    //
    // create LineCartesianGraph and transforms
    //
    graph = new CartesianGraph("Profile Graph 1");
    xt = new LinearTransform(xMin_, xMax_, 10.0, 20.0);
    yt = new LinearTransform(yMin_, yMax_, 400.0, 0.0);
    graph.setXTransform(xt);
    graph.setYTransform(yt);
    //
    // create axes
    //
    Font axfont = new Font("Helvetica", Font.ITALIC, 14);
    xbot = new PlainAxis("Bottom Axis");
    xbot.setRangeU(new Range2D(10.0, 20.0));
    xbot.setDeltaU(2.0);
    xbot.setNumberSmallTics(0);
    xbot.setLabelHeightP(labelHeight_);
    xbot.setLocationU(new Point2D.Double(10.0, 400.0));
    xbot.setLabelFont(axfont);
    graph.addXAxis(xbot);
    //
    yleft = new PlainAxis("Left Axis");
    yleft.setRangeU(new Range2D(400.0, 0.0));
    yleft.setDeltaU(-50.0);
    yleft.setNumberSmallTics(0);
    yleft.setLabelHeightP(labelHeight_);
    yleft.setLocationU(new Point2D.Double(10.0, 400.0));
    yleft.setLabelFont(axfont);
    graph.addYAxis(yleft);
    //
    layer.setGraph(graph);
  }
  public String getLocationSummary(SGTData grid) {
    return "";
  }
  public void addData(Collection lines) {
    addData(lines, null);
  }
  public void addData(Collection lines, String descrip) {
    //    System.out.println("addData(Collection) called");
    for(int i=0; i < lines.size(); i++) {
      SGTLine line = (SGTLine)lines.elementAt(i);
      addData(line, line.getTitle());
    }
  }
  /**
   * Add data to the layout. LineKey descriptor will be
   * taken from the dependent variable name.
   *
   * @param data datum data to be added
   */
  public void addData(SGTData datum) {
    addData(datum, null);
  }
  /**
   * Add data to the layout.  Data will be added to X axis and Z_AXIS will be
   * assigned to Y axis. If this is not the first invocation of addData a new Layer
   * will be created. If overlayed, the transforms from the first layer will be
   * attached and <strong>no</strong> axes will be created. If not overlayed, new transforms
   * and axes will be created and adjusted so that the data is horizontally stacked.
   *
   * @param datum data to be added
   * @param descrip LineKey description for datum
   */
  public void addData(SGTData datum, String descrip) {
    //
    Layer layer, newLayer;
    CartesianGraph graph, newGraph;
    PlainAxis xbot = null;
    PlainAxis yleft = null;
    LinearTransform xt, yt;
    SGLabel xtitle, ytitle, lineTitle;
    SGTData data;
    LineAttribute lineAttr;
    String xLabel, yLabel;
    Range2D xRange, yRange;
    Range2D xnRange = null, ynRange = null;
    Point2D.Double origin = null;
    boolean data_good = true;
    double save;
    //
    if(data_.size() == 0) setBaseUnit(Units.getBaseUnit(((SGTLine)datum).getXMetaData()));
    datum = Units.convertToBaseUnit(datum, getBaseUnit(), Units.X_AXIS);
    //
    if(data_.size() == 0) {
      super.addData(datum);
      //
      // only one data set...
      // determine range and titles from data
      //
      data = (SGTData)data_.firstElement();
      xRange = findRange((SGTLine)data, X_AXIS);
      yRange = findRange((SGTLine)data, Y_AXIS);
      zUp_ = ((SGTLine)data).getYMetaData().isReversed();

      if(Double.isNaN(xRange.start) || Double.isNaN(yRange.start)) data_good = false;

      if(data_good) {
        if(!zUp_) {
          save = yRange.end;
          yRange.end = yRange.start;
          yRange.start = save;
        }
        //
        xnRange = Graph.computeRange(xRange, 6);
        ynRange = Graph.computeRange(yRange, 6);
        //
        origin = new Point2D.Double(xnRange.start, ynRange.start);
      }
      //
      xLabel =  " (" + ((SGTLine)data).getXMetaData().getUnits() + ")";
      yLabel =  " (" + ((SGTLine)data).getYMetaData().getUnits() + ")";
      //
      // attach information to pane and descendents
      //
      try {
        layer = getLayer("Layer 1");
      } catch (LayerNotFoundException e) {
        return;
      }
      graph = (CartesianGraph)layer.getGraph();
      //
      // axes
      //
      try {
        Font tfont = new Font("Helvetica", Font.PLAIN, 14);
        xbot = (PlainAxis)graph.getXAxis("Bottom Axis");
        if(data_good) {
          xbot.setRangeU(xnRange);
          xbot.setDeltaU(xnRange.delta);
          xbot.setLocationU(origin);
        }
        xtitle = new SGLabel("xaxis title", xLabel, new Point2D.Double(0.0, 0.0));
        xtitle.setFont(tfont);
        xtitle.setHeightP(titleHeight_);
        xbot.setTitle(xtitle);
        //
        yleft = (PlainAxis)graph.getYAxis("Left Axis");
        if(data_good) {
          yleft.setRangeU(ynRange);
          yleft.setDeltaU(ynRange.delta);
          yleft.setLocationU(origin);
        }
        ytitle = new SGLabel("yaxis title", yLabel, new Point2D.Double(0.0, 0.0));
        ytitle.setFont(tfont);
        ytitle.setHeightP(titleHeight_);
        yleft.setTitle(ytitle);
      } catch (AxisNotFoundException e) {}
      if(data_good) {
        //
        // transforms
        //
        xt = (LinearTransform)graph.getXTransform();
        xt.setRangeU(xnRange);
        //
        yt = (LinearTransform)graph.getYTransform();
        yt.setRangeU(ynRange);
      }
      //
      // attach data
      //
      if(((SGTLine)data).getYArray().length >= 2) {
        lineAttr = new LineAttribute(LineAttribute.SOLID,
                                     markList_[layerCount_%8],
                                     colorList_[layerCount_%8]);
      } else {
        lineAttr = new LineAttribute(LineAttribute.MARK,
                                     markList_[layerCount_%8],
                                     colorList_[layerCount_%8]);
      }
      graph.setData(data, lineAttr);
      //
      // add to lineKey
      //
      if(descrip == null) {
        lineTitle = new SGLabel("line title", xLabel, new Point2D.Double(0.0, 0.0));
      } else {
        lineTitle = new SGLabel("line title", descrip, new Point2D.Double(0.0, 0.0));
      }
      lineTitle.setHeightP(keyHeight_);
      lineKey_.addLineGraph((LineCartesianRenderer)graph.getRenderer(), lineTitle);
      if(keyPane_ != null) {
        Rectangle vRect = keyPane_.getVisibleRect();
        int nrow = vRect.height/lineKey_.getRowHeight();
        keyPane_.setScrollableUnitIncrement(1, lineKey_.getRowHeight());
        keyPane_.setScrollableBlockIncrement(vRect.width,
                                             lineKey_.getRowHeight()*nrow);
      }
    } else {
      //
      // more than one data set...
      // add new layer
      //
      if(((SGTLine)datum).getYMetaData().isReversed() != zUp_) {
        //        System.out.println("New datum has reversed ZUp!");
        SGTData modified = flipZ(datum);
        datum = modified;
      }
      super.addData(datum);

      data_good = false;
      layerCount_++;
      if(isOverlayed()) {
        try {
          layer = getLayer("Layer 1");
        } catch (LayerNotFoundException e) {
          return;
        }
        graph = (CartesianGraph)layer.getGraph();
        //
        // transforms
        //
        xt = (LinearTransform)graph.getXTransform();
        yt = (LinearTransform)graph.getYTransform();
        try {
          xbot = (PlainAxis)graph.getXAxis("Bottom Axis");
          yleft = (PlainAxis)graph.getYAxis("Left Axis");
        } catch (AxisNotFoundException e) {}

        if(!inZoom_) {
          //
          // loop over data sets, getting ranges
          //
          Range2D xTotalRange = new Range2D();
          Range2D yTotalRange = new Range2D();

          boolean first = true;
          for (Enumeration e = data_.elements() ; e.hasMoreElements() ;) {
            data = (SGTData)e.nextElement();
            xRange = findRange((SGTLine)data, X_AXIS);
            yRange = findRange((SGTLine)data, Y_AXIS);
            if(!((SGTLine)data).getYMetaData().isReversed()) {
              save = yRange.start;
              yRange.start = yRange.end;
              yRange.end = save;
            }
            if(first) {
              if(Double.isNaN(xRange.start) || Double.isNaN(yRange.start)) {
                first = true;
              } else {
                first = false;
                data_good = true;
                xTotalRange = new Range2D(xRange.start, xRange.end);
                yTotalRange = new Range2D(yRange.start, yRange.end);
              }
            } else {
              if(!Double.isNaN(xRange.start) && !Double.isNaN(yRange.start)) {
                data_good = true;
                xTotalRange.start = Math.min(xTotalRange.start, xRange.start);
                xTotalRange.end = Math.max(xTotalRange.end, xRange.end);
                if(!((SGTLine)data).getYMetaData().isReversed()) {
                  yTotalRange.start = Math.max(yTotalRange.start, yRange.start);
                  yTotalRange.end = Math.min(yTotalRange.end, yRange.end);
                } else {
                  yTotalRange.start = Math.min(yTotalRange.start, yRange.start);
                  yTotalRange.end = Math.max(yTotalRange.end, yRange.end);
                }
              }
            }
          }
          if(data_good) {
            xnRange = Graph.computeRange(xTotalRange, 6);
            ynRange = Graph.computeRange(yTotalRange, 6);
            origin = new Point2D.Double(xnRange.start, ynRange.start);
            //
            // axes
            //
            xbot.setRangeU(xnRange);
            xbot.setDeltaU(xnRange.delta);
            xbot.setLocationU(origin);
            //
            yleft.setRangeU(ynRange);
            yleft.setDeltaU(ynRange.delta);
            yleft.setLocationU(origin);
          }
          //
          if(data_good) {
            xt.setRangeU(xnRange);
            yt.setRangeU(ynRange);
          }
        }
        //
        // create new layer and graph
        //
        newLayer = new Layer("Layer " + (layerCount_+1), new Dimension2D(xSize_, ySize_));
        newGraph = new CartesianGraph("Graph " + (layerCount_+1), xt, yt);
        if(inZoom_) {
          Range2D xr, yr;
          xr = xbot.getRangeU();
          yr = yleft.getRangeU();
          newGraph.setClip(xr.start, xr.end, yr.start, yr.end);
          newGraph.setClipping(true);
        }
        add(newLayer,0);
        newLayer.setGraph(newGraph);
        newLayer.invalidate();
        validate();
        //
        // attach data
        //
        if(((SGTLine)datum).getXArray().length >= 2) {
          lineAttr = new LineAttribute(LineAttribute.SOLID,
                                       markList_[layerCount_%8],
                                       colorList_[layerCount_%8]);
        } else {
          lineAttr = new LineAttribute(LineAttribute.MARK,
                                       markList_[layerCount_%8],
                                       colorList_[layerCount_%8]);
        }
        newGraph.setData(datum, lineAttr);
        //
        // add to lineKey
        //
        if(descrip == null) {
          xLabel = ((SGTLine)datum).getXMetaData().getName();
          lineTitle = new SGLabel("line title", xLabel, new Point2D.Double(0.0, 0.0));
        } else {
          lineTitle = new SGLabel("line title", descrip, new Point2D.Double(0.0, 0.0));
        }
        lineTitle.setHeightP(keyHeight_);
        lineKey_.addLineGraph((LineCartesianRenderer)newGraph.getRenderer(), lineTitle);
        if(keyPane_ != null) {
          Rectangle vRect = keyPane_.getVisibleRect();
          int nrow = vRect.height/lineKey_.getRowHeight();
          keyPane_.setScrollableUnitIncrement(1, lineKey_.getRowHeight());
          keyPane_.setScrollableBlockIncrement(vRect.width,
                                               lineKey_.getRowHeight()*nrow);
        }
      }
    }
  }
  /**
   * Flip the zaxis.  Reverse the direction of the z axis by changing the sign
   * of the axis values and isBackward flag.
   */
  private SGTData flipZ(SGTData in) {
    SGTMetaData zmetaout;
    SGTMetaData zmetain;
    SimpleLine out = null;
    SGTLine line = (SGTLine) in;
    double[] values;
    double[] newValues;
    values = line.getYArray();
    newValues = new double[values.length];
    for(int i=0; i < values.length; i++) {
      newValues[i] = -values[i];
    }
    out = new SimpleLine(line.getXArray(), newValues, line.getTitle());
    zmetain = line.getYMetaData();
    zmetaout = new SGTMetaData(zmetain.getName(), zmetain.getUnits(),
                               !zmetain.isReversed(), zmetain.isModulo());
    zmetaout.setModuloValue(zmetain.getModuloValue());
    zmetaout.setModuloTime(zmetain.getModuloTime());
    out.setXMetaData(line.getXMetaData());
    out.setYMetaData(zmetaout);
    return (SGTData)out;
  }
  /**
   * Clear the current zoom.
   */
  public void resetZoom() {
    SGTData data;
    Range2D xRange, yRange;
    boolean data_good = false;
    double save;

    inZoom_ = false;
    //
    // loop over data sets, getting ranges
    //
    Range2D xTotalRange = new Range2D();
    Range2D yTotalRange = new Range2D();

    boolean first = true;

    for (Enumeration e = data_.elements() ; e.hasMoreElements() ;) {
      data = (SGTData)e.nextElement();
      xRange = findRange((SGTLine)data, X_AXIS);
      yRange = findRange((SGTLine)data, Y_AXIS);
      if(!((SGTLine)data).getYMetaData().isReversed()) {
        save = yRange.start;
        yRange.start = yRange.end;
        yRange.end = save;
      }
      if(first) {
        if(Double.isNaN(xRange.start) || Double.isNaN(yRange.start)) {
          first = true;
        } else {
          first = false;
          data_good = true;
          xTotalRange = new Range2D(xRange.start, xRange.end);
          yTotalRange = new Range2D(yRange.start, yRange.end);
        }
      } else {
        if(!Double.isNaN(xRange.start) && !Double.isNaN(yRange.start)) {
          data_good = true;
          xTotalRange.start = Math.min(xTotalRange.start, xRange.start);
          xTotalRange.end = Math.max(xTotalRange.end, xRange.end);
          if(!((SGTLine)data).getYMetaData().isReversed()) {
            yTotalRange.start = Math.max(yTotalRange.start, yRange.start);
            yTotalRange.end = Math.min(yTotalRange.end, yRange.end);
          } else {
            yTotalRange.start = Math.min(yTotalRange.start, yRange.start);
            yTotalRange.end = Math.max(yTotalRange.end, yRange.end);
          }
        }
      }
    }
    if(data_good) {
      try {
        setRange(new Domain(xTotalRange, yTotalRange), false);
      } catch (PropertyVetoException e) {
        System.out.println("zoom reset denied! " + e);
      }
    }
  }
  /**
   * Set the x and y range of the domain.
   *
   * @param range new domain
   */
  public void setRange(Domain domain) throws PropertyVetoException {
    setRange(domain, true);
  }
  public void setRange(Domain domain, boolean testZUp)
    throws PropertyVetoException {
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform xt = (LinearTransform)graph.getXTransform();
    LinearTransform yt = (LinearTransform)graph.getYTransform();
    Domain oldRange = new Domain(xt.getRangeU(),
                                           yt.getRangeU());
    if(!domain.equals(oldRange)) {
      setBatch(true, "JLineProfileLayout: setRange");
      vetos_.fireVetoableChange("domainRange", oldRange, domain);

      inZoom_ = true;

      if(!domain.isXTime()) {
        setXRange(domain.getXRange());
      }
      if(!domain.isYTime()) {
        setYRange(domain.getYRange(), testZUp);
      }
      changes_.firePropertyChange("domainRange", oldRange, domain);
      setBatch(false, "JLineProfileLayout: setRange");
    }
  }
  public void setRangeNoVeto(Domain domain) {
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform xt = (LinearTransform)graph.getXTransform();
    LinearTransform yt = (LinearTransform)graph.getYTransform();
    Domain oldRange = new Domain(xt.getRangeU(),
                                           yt.getRangeU());
    //
    // clipping?  hack fix.  how should clipping be done for
    // external range sets?
    //
    setBatch(true, "JLineProfileLayout: setRangeNoVeto");
    inZoom_ = true;
    setClipping(true);

    if(!domain.isXTime()) {
      setXRange(domain.getXRange());
    }
    if(!domain.isYTime()) {
      setYRange(domain.getYRange(),false);
    }
    setBatch(false, "JLineProfileLayout: setRangeNoVeto");
    //    changes_.firePropertyChange("domainRange", oldRange, domain);
  }
  /**
   * Reset the x range. This method is designed to provide
   * zooming functionality.
   *
   * @param rnge new x range
   */
  void setXRange(Range2D rnge) {
    Point2D.Double origin;
    PlainAxis xbot, yleft;
    Range2D xr, yr, xnRange;
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform xt = (LinearTransform)graph.getXTransform();
    xnRange = Graph.computeRange(rnge, 6);
    xt.setRangeU(xnRange);
    try {
      xbot = (PlainAxis)graph.getXAxis("Bottom Axis");
      yleft = (PlainAxis)graph.getYAxis("Left Axis");

      xbot.setRangeU(xnRange);
      xbot.setDeltaU(xnRange.delta);

      xr = xbot.getRangeU();
      yr = yleft.getRangeU();
      origin = new Point2D.Double(xr.start, yr.start);
      xbot.setLocationU(origin);

      yleft.setLocationU(origin);
      //
      // set clipping
      //
      if(clipping_) {
        setAllClip(xr.start, xr.end, yr.start, yr.end);
      } else {
        setAllClipping(false);
      }
    } catch (AxisNotFoundException e) {}
  }
  /**
   * Reset the y range. This method is designed to provide
   * zooming functionality.
   *
   * @param rnge new y range
   */
  void setYRange(Range2D rnge) {
    setYRange(rnge, true);
  }

  /**
   * Reset the y range. This method is designed to provide
   * zooming functionality.
   *
   * @param rnge new y range
   * @param testZUp test to see if Z is Up
   */
  void setYRange(Range2D rnge, boolean testZUp) {
    SGTData grid;
    Point2D.Double origin;
    PlainAxis xbot, yleft;
    Range2D xr, yr, ynRange;
    double save;
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform yt = (LinearTransform)graph.getYTransform();
    if(testZUp && data_.size() > 0) {
      grid = (SGTData)data_.elements().nextElement();
      if(!((SGTLine)grid).getYMetaData().isReversed()) {
        save = rnge.end;
        rnge.end = rnge.start;
        rnge.start = save;
      }
    }
    ynRange = Graph.computeRange(rnge, 6);
    yt.setRangeU(ynRange);
    try {
      xbot = (PlainAxis)graph.getXAxis("Bottom Axis");
      yleft = (PlainAxis)graph.getYAxis("Left Axis");

      yleft.setRangeU(ynRange);
      yleft.setDeltaU(ynRange.delta);

      xr = xbot.getRangeU();
      yr = yleft.getRangeU();
      origin = new Point2D.Double(xr.start, yr.start);
      yleft.setLocationU(origin);

      xbot.setLocationU(origin);
      //
      // set clipping
      //
      if(clipping_) {
        setAllClip(xr.start, xr.end, yr.start, yr.end);
      } else {
        setAllClipping(false);
      }
    } catch (AxisNotFoundException e) {}
  }
  private void setAllClip(JPane pane, double xmin, double xmax, double ymin, double ymax) {
    Layer ly;
    Component[] comps = pane.getComponents();
    for(int i=0; i < comps.length; i++) {
      if(comps[i] instanceof Layer) {
        ly = (Layer)comps[i];
        ((CartesianGraph)ly.getGraph()).setClip(xmin, xmax, ymin, ymax);
      }
    }
  }
  private void setAllClipping(JPane pane, boolean clip) {
    Layer ly;
    Component[] comps = pane.getComponents();
    for(int i=0; i < comps.length; i++) {
      if(comps[i] instanceof Layer) {
        ly = (Layer)comps[i];
        ((CartesianGraph)ly.getGraph()).setClipping(clip);
      }
    }
  }
  public void clear() {
    data_.removeAllElements();
    ((CartesianGraph)firstLayer_.getGraph()).setRenderer(null);
    removeAll();
    add(firstLayer_,0);   // restore first layer
    lineKey_.clearAll();
    //    draw();
    //    if(keyPane_ != null)keyPane_.draw();
    inZoom_ = false;
  }

  public void clear(String data_id) {
    Layer ly = null;
    SGTData dat;
    try {
      ly = getLayerFromDataId(data_id);
      remove(ly);
    } catch (LayerNotFoundException e) {}
    for(Enumeration it=data_.elements(); it.hasMoreElements();) {
      dat = (SGTData)it.nextElement();
      if(dat.getId().equals(data_id)) {
        data_.removeElement(dat);
      }
    }
    lineKey_.clear(data_id);
    if(getComponentCount() <= 0 || ly.equals(firstLayer_)) {
      ((CartesianGraph)firstLayer_.getGraph()).setRenderer(null);
      add(firstLayer_,0);  // restore first layer
    }
    //    draw();
    //    if(keyPane_ != null)keyPane_.draw();
  }

  public void setKeyBoundsP(Rectangle2D.Double bounds){
    if(lineKey_ != null) {
      lineKey_.setBoundsP(bounds);
    }
  }
  public Rectangle2D.Double getKeyBoundsP() {
    if(lineKey_ == null) {
      return null;
    } else {
      return lineKey_.getBoundsP();
    }
  }

  public Dimension2D getLayerSizeP() {
    return new Dimension2D(xSize_, ySize_);
  }
  public Layer getFirstLayer() {
    return firstLayer_;
  }
  public void setLayerSizeP(Dimension2D d) {
    Component[] comps = getComponents();
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform yt = (LinearTransform)graph.getYTransform();
    LinearTransform xt = (LinearTransform)graph.getXTransform();
    xMax_ = d.width - (xSize_ - xMax_);
    yMax_ = d.height - (ySize_ - yMax_);
    xSize_ = d.width;
    ySize_ = d.height;
    for(int i=0; i < comps.length; i++) {
      if(comps[i] instanceof Layer) {
        ((Layer)comps[i]).setSizeP(d);
      }
    }
    yt.setRangeP(new Range2D(yMin_, yMax_));
    xt.setRangeP(new Range2D(xMin_, xMax_));
    //
    double xpos;
    if(iconImage_ != null) {
      Rectangle bnds = logo_.getBounds();
      xpos = firstLayer_.getXDtoP(bnds.x + bnds.width) + 0.05;
    } else {
      xpos = (xMin_ + xMax_)*0.5;
    }
    double ypos = ySize_ - 1.2f*mainTitleHeight_;
    mainTitle_.setLocationP(new Point2D.Double(xpos, ypos));
    ypos = ypos - 1.2f*warnHeight_;
    title2_.setLocationP(new Point2D.Double(xpos, ypos));
    ypos = ypos - 1.1f*warnHeight_;
    title3_.setLocationP(new Point2D.Double(xpos, ypos));
    if(keyPane_ == null) {
      lineKey_.setLocationP(new Point2D.Double(xSize_ - 0.01, ySize_));
    }

  }
}
