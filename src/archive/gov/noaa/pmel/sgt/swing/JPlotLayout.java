/*
 * $Id: JPlotLayout.java,v 1.53 2003/09/15 22:05:41 dwd Exp $
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
import gov.noaa.pmel.sgt.PointCollectionKey;
import gov.noaa.pmel.sgt.ColorKey;
import gov.noaa.pmel.sgt.VectorKey;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.LineCartesianRenderer;
import gov.noaa.pmel.sgt.PointCartesianRenderer;
import gov.noaa.pmel.sgt.GridCartesianRenderer;
import gov.noaa.pmel.sgt.VectorCartesianRenderer;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.PlainAxis;
import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Logo;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.LayerNotFoundException;
import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.VectorAttribute;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.AxisNotFoundException;
import gov.noaa.pmel.sgt.StackedLayout;
import gov.noaa.pmel.sgt.TimeAxis;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.IndexedColorMap;
import gov.noaa.pmel.sgt.TransformAccess;
import gov.noaa.pmel.sgt.DataNotFoundException;
import gov.noaa.pmel.sgt.AttributeChangeEvent;

import gov.noaa.pmel.util.Debug;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTDomain;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Domain;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.IllegalTimeValue;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.TimeRange;

import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.SimpleLine;
import gov.noaa.pmel.sgt.dm.SGTMetaData;
import gov.noaa.pmel.sgt.dm.Collection;
import gov.noaa.pmel.sgt.dm.PointCollection;
import gov.noaa.pmel.sgt.dm.SGTVector;

import java.util.Enumeration;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.geom.AffineTransform;

import java.awt.Point;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.*;

import java.beans.PropertyVetoException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * <code>JPlotLayout</code> creates a pre-defined graphics layout for
 * <code>SGTLine</code>, <code>SGTGrid</code>,
 * <code>Collection</code>, <code>SGTVector</code>,
 * and <code>PointCollection</code> data.
 *
 * @author Donald Denbo
 * @version $Revision: 1.53 $, $Date: 2003/09/15 22:05:41 $
 * @since 2.0
 * @see LineCartesianRenderer
 * @see PointCartesianRenderer
 * @see GridCartesianRenderer
 * @see VectorCartesianRenderer
**/
public class JPlotLayout extends JGraphicLayout
  implements PropertyChangeListener {
  //
  // save handles to unique components
  //


  /**
   * @label coastline
   * @link aggregation
   * @undirected
   */
  CartesianGraph coastLine_ = null;
  Layer coastLayer_ = null;
  Logo logo_;

  /**
   * @label lineKey
   * @link aggregation
   * @undirected
   */
  LineKey lineKey_;

  /**
   * @label colorKey
   * @link aggregation
   * @undirected
   */
  ColorKey colorKey_;

  /**
   * @label pointKey
   * @link aggregation
   * @undirected
   */
  PointCollectionKey pointKey_;

  VectorKey vectorKey_;

  private boolean computeScroll_ = false;
  int layerCount_;
  boolean revXAxis_ = false;
  boolean revYAxis_ = false;
  Layer firstLayer_;
  boolean inZoom_ = false;
  GridAttribute gAttr_ = null;
  //
  private boolean isXTime_ = false;
  private boolean isYTime_ = false;
  //
  // constants
  //
  double xSize_ = XSIZE_;
  double xMin_ = XMIN_;
  double xMax_ = XMAX_;
  double ySize_ = YSIZE_;
  double yMin_ = YMIN_;
  double yMax_ = YSIZE_ - 1.0*MAIN_TITLE_HEIGHT_
                        - 2.0*WARN_HEIGHT_
                        - 0.5*WARN_HEIGHT_;
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
  boolean autoRangeX_ = false;
  boolean autoRangeY_ = false;
  int autoXIntervals_ = 10;
  int autoYIntervals_ = 10;
  //
  public static final int POINTS = 0;
  public static final int LINE = 1;
  public static final int GRID = 2;
  public static final int VECTOR = 3;
  //
  private int plotType_ = -1;
  //
  private static final Color[] colorList_ =
  { Color.blue, Color.cyan.darker(), Color.green, Color.orange.darker(),
    Color.red, Color.magenta, Color.black, Color.gray};
  //  {Color.red, Color.green, Color.blue, Color.cyan,
  //   Color.magenta, Color.yellow, Color.orange, Color.pink};
  private static final int[] markList_ =
  {1, 2, 9, 15, 10, 24, 11, 44};

  private static String LEFT_AXIS = "Left Axis";
  private static String BOTTOM_AXIS = "Bottom Axis";

  //  public JPlotLayout(SGTData dataset, String dataKey) {
  //    this(dataset, "", null, false);
  //  }
  /**
   * Default constructor. No Logo image is used and the <code>LineKey</code>
   * will be in the same <code>JPane</code>.
   */
  public JPlotLayout(SGTData dataset) {
    this(dataset, "", null, false);
  }
  /**
   * <code>JPlotLayout</code> constructor.  Whether the data is GRID,
   * POINTS, LINE, or VECTOR, isXtime, and isYTime is determined from the dataset.
   *
   * @param dataset the template data
   * @param id identifier
   * @param img Logo image
   * @param is_key_pane if true LineKey is in separate pane
   */
  public JPlotLayout(SGTData dataset, String id, Image img,
                     boolean is_key_pane) {
    this(dataset instanceof SGTGrid? GRID:
         (dataset instanceof PointCollection? POINTS: (dataset instanceof SGTVector? VECTOR: LINE)),
         dataset.isXTime(), dataset.isYTime(), id, img, is_key_pane);
  }
  /**
   * <code>JPlotLayout</code> constructor.  This constructor is
   * retained for backward compatability.
   *
   * @param isGrid if true data is grid
   * @param isXtime if true x coordinate is time
   * @param isYTime if true y coordinate is time
   * @param id identifier
   * @param img Logo image
   * @param is_key_pane if true LineKey is in separate pane
   */
  public JPlotLayout(boolean isGrid, boolean isXTime,
                     boolean isYTime, String id, Image img,
                     boolean is_key_pane) {
    this(isGrid? GRID: LINE,
         isXTime, isYTime, id, img, is_key_pane);
  }
  /**
   * <code>JPlotLayout</code> constructor.  All other constructors
   * call this one. Data is not plotted during construction of the
   * <code>JPlotLayout</code> object and the <code>addData</code>
   * method must be called to associated data with this object.
   *
   * @param isGrid if true data is grid
   * @param isPoints if true data is points
   * @param isXtime if true x coordinate is time
   * @param isYTime if true y coordinate is time
   * @param id identifier
   * @param img Logo image
   * @param is_key_pane if true LineKey is in separate pane
   */
  public JPlotLayout(boolean isGrid,
                     boolean isPoints,
                     boolean isXTime,
                     boolean isYTime,
                     String id,
                     Image img,
                     boolean is_key_pane) {
    this(isGrid? GRID:(isPoints? POINTS: LINE),
         isXTime, isYTime, id, img, is_key_pane);
  }
  /**
   * <code>JPlotLayout</code> constructor.  All other constructors
   * call this one. Data is not plotted during construction of the
   * <code>JPlotLayout</code> object and the <code>addData</code>
   * method must be called to associated data with this object.
   *
   * @param type type of plot , POINT, GRID, LINE, or VECTOR
   * @param isXtime if true x coordinate is time
   * @param isYTime if true y coordinate is time
   * @param id identifier
   * @param img Logo image
   * @param is_key_pane if true LineKey is in separate pane
   */
  public JPlotLayout(int type,
                     boolean isXTime,
                     boolean isYTime,
                     String id,
                     Image img,
                     boolean is_key_pane) {
    super(id, img, new Dimension(400,300));
    Layer layer, key_layer;
    CartesianGraph graph;
    LinearTransform xt, yt;
    PlainAxis xbot = null;
    PlainAxis yleft = null;
    TimeAxis tbot = null;
    TimeAxis tleft = null;
    double xpos, ypos;
    int halign;
    ColorMap cmap;
    //
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
    //
    isXTime_ = isXTime;
    isYTime_ = isYTime;
    plotType_ = type;
    //
    // create Pane and descendants for the LineProfile layout
    //
    setOpaque(true);
    setLayout(new StackedLayout());
    setBackground(paneColor_);
    layer = new Layer("Layer 1", new Dimension2D(xSize_, ySize_));
    firstLayer_ = layer;
    if(coastLine_ == null) {
      add(layer,0);
    } else {
      add(layer,1);
    }
    //
    if(plotType_ == GRID) {
      // SGTGrid
      colorKey_ = new ColorKey(new Rectangle2D.Double(0.01, 0.01,
                                                      xKeySize_ - 0.01, 1.0),
                               ColorKey.BOTTOM, ColorKey.LEFT);
      colorKey_.setId("Color Key");
      colorKey_.setVisible(false);
      if(is_key_pane) {
        colorKey_.setVAlign(ColorKey.TOP);
        colorKey_.setBorderStyle(ColorKey.NO_BORDER);
//        colorKey_.setBorderStyle(ColorKey.PLAIN_LINE);
        colorKey_.setLocationP(new Point2D.Double(0.0, yKeySize_));
        int xdim = 500;
        int ydim = (int)((double)xdim/xKeySize_*yKeySize_);
        keyPane_ = new JPane("KeyPane", new Dimension(xdim,ydim));
        keyPane_.setOpaque(true);
        keyPane_.setLayout(new StackedLayout());
        keyPane_.setBackground(keyPaneColor_);
        key_layer = new Layer("Key Layer", new Dimension2D(xKeySize_, yKeySize_));
        keyPane_.add(key_layer);
        key_layer.addChild(colorKey_);
      } else {
        colorKey_.setHAlign(ColorKey.CENTER);
        colorKey_.setLocationP(new Point2D.Double(xSize_*0.5, 0.01));
        layer.addChild(colorKey_);
      }
    } else if(plotType_ == POINTS){
      // SGTPoint
      pointKey_ = new PointCollectionKey();
      pointKey_.setSelectable(false);
      pointKey_.setId("Point Key");
      pointKey_.setVAlign(LineKey.TOP);
      if(is_key_pane) {
        pointKey_.setHAlign(PointCollectionKey.LEFT);
        pointKey_.setBorderStyle(PointCollectionKey.NO_BORDER);
        pointKey_.setLocationP(new Point2D.Double(0.0, yKeySize_));
        int xdim = 500;
        int ydim = (int)((double)xdim/xKeySize_*yKeySize_);
        keyPane_ = new JPane("KeyPane", new Dimension(xdim,ydim));
        keyPane_.setOpaque(true);
        keyPane_.setLayout(new StackedLayout());
        keyPane_.setBackground(keyPaneColor_);
        key_layer = new Layer("Key Layer", new Dimension2D(xKeySize_, yKeySize_));
        keyPane_.add(key_layer);
        key_layer.addChild(pointKey_);
      } else {
        pointKey_.setVAlign(PointCollectionKey.TOP);
        pointKey_.setHAlign(PointCollectionKey.RIGHT);
        pointKey_.setLocationP(new Point2D.Double(xSize_-0.02, ySize_-0.1));
        layer.addChild(pointKey_);
      }
    } else if(plotType_ == LINE) {
      // SGTLine
      lineKey_ = new LineKey();
      lineKey_.setSelectable(false);
      lineKey_.setId("Line Key");
      lineKey_.setVAlign(LineKey.TOP);
      if(is_key_pane) {
        lineKey_.setHAlign(LineKey.LEFT);
        lineKey_.setBorderStyle(LineKey.NO_BORDER);
        lineKey_.setLocationP(new Point2D.Double(0.0, yKeySize_));
        int xdim = 500;
        int ydim = (int)((double)xdim/xKeySize_*yKeySize_);
        keyPane_ = new JPane("KeyPane", new Dimension(xdim,ydim));
        keyPane_.setOpaque(true);
        keyPane_.setLayout(new StackedLayout());
        keyPane_.setBackground(keyPaneColor_);
        key_layer = new Layer("Key Layer", new Dimension2D(xKeySize_, yKeySize_));
        keyPane_.add(key_layer);
        key_layer.addChild(lineKey_);
      } else {
        lineKey_.setVAlign(LineKey.TOP);
        lineKey_.setHAlign(LineKey.RIGHT);
        lineKey_.setLocationP(new Point2D.Double(xSize_-0.02, ySize_-0.1));
        layer.addChild(lineKey_);
      }
    } else if(plotType_ == VECTOR) {
      // SGTVector
      vectorKey_ = new VectorKey();
      vectorKey_.setSelectable(false);
      vectorKey_.setId("Vector Key");
      vectorKey_.setVAlign(VectorKey.TOP);
      if(is_key_pane) {
        vectorKey_.setHAlign(VectorKey.LEFT);
        vectorKey_.setBorderStyle(VectorKey.NO_BORDER);
        vectorKey_.setLocationP(new Point2D.Double(0.0, yKeySize_));
        int xdim = 500;
        int ydim = (int)((double)xdim/xKeySize_*yKeySize_);
        keyPane_ = new JPane("KeyPane", new Dimension(xdim,ydim));
        keyPane_.setOpaque(true);
        keyPane_.setLayout(new StackedLayout());
        keyPane_.setBackground(keyPaneColor_);
        key_layer = new Layer("Key Layer", new Dimension2D(xKeySize_, yKeySize_));
        keyPane_.add(key_layer);
        key_layer.addChild(vectorKey_);
      } else {
        vectorKey_.setVAlign(VectorKey.TOP);
        vectorKey_.setHAlign(VectorKey.RIGHT);
        vectorKey_.setLocationP(new Point2D.Double(xSize_-0.02, ySize_-0.1));
        vectorKey_.setSelectable(true);
        layer.addChild(vectorKey_);
      }
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
    ypos = ySize_ - 1.0f*mainTitleHeight_;
    Font titleFont = new Font("Helvetica", Font.BOLD, 14);
    mainTitle_ = new SGLabel("Line Profile Title",
                             "Profile Plot",
                             mainTitleHeight_,
                             new Point2D.Double(xpos, ypos),
                             SGLabel.BOTTOM,
                             halign);
    mainTitle_.setFont(titleFont);
    layer.addChild(mainTitle_);
    ypos = ypos - 1.0f*warnHeight_;
    Font title2Font = new Font("Helvetica", Font.PLAIN, 10);
    title2_ = new SGLabel("Second Title",
                          "Warning: Browse image only",
                          warnHeight_,
                          new Point2D.Double(xpos, ypos),
                          SGLabel.BOTTOM,
                          halign);
    title2_.setFont(title2Font);
    layer.addChild(title2_);
    ypos = ypos - 1.0f*warnHeight_;
    title3_ = new SGLabel("Warning 2",
                          "Verify accuracy of plot before research use",
                          warnHeight_,
                          new Point2D.Double(xpos, ypos),
                          SGLabel.BOTTOM,
                          halign);
    title3_.setFont(title2Font);
    layer.addChild(title3_);

    layerCount_ = 0;
    //
    // create LineCartesianGraph and transforms
    //
    graph = new CartesianGraph("Profile Graph 1");
    GeoDate start = null;
    GeoDate end = null;
    try {
      start = new GeoDate("1992-11-01", "yyyy-MM-dd");
      end = new GeoDate("1993-02-20", "yyyy-MM-dd");
    } catch (IllegalTimeValue e) {}
    SoTRange xRange, yRange;
    if(isXTime_) {
      xRange = new SoTRange.Time(start.getTime(), end.getTime());
    } else {
      xRange = new SoTRange.Double(10.0, 20.0, 2.0);
    }
    if(isYTime_) {
      yRange = new SoTRange.Time(start.getTime(), end.getTime());
    } else {
      yRange = new SoTRange.Double(400.0, 0.0, -50.0);
    }
    SoTPoint origin;

    xt = new LinearTransform(new Range2D(xMin_, xMax_), xRange);
    yt = new LinearTransform(new Range2D(yMin_, yMax_), yRange);
    origin = new SoTPoint(xRange.getStart(), yRange.getStart());

    graph.setXTransform(xt);
    graph.setYTransform(yt);
    //
    // create axes
    //
    Font axfont = new Font("Helvetica", Font.ITALIC, 14);
    if(isXTime_) {
      tbot = new TimeAxis(BOTTOM_AXIS, TimeAxis.AUTO);
      tbot.setRangeU(xRange);
      tbot.setLabelHeightP(labelHeight_);
      tbot.setLocationU(origin);
      tbot.setLabelFont(axfont);
      graph.addXAxis(tbot);
    } else {
      xbot = new PlainAxis(BOTTOM_AXIS);
      xbot.setRangeU(xRange);
      xbot.setNumberSmallTics(0);
      xbot.setLocationU(origin);
      xbot.setLabelHeightP(labelHeight_);
      xbot.setLabelFont(axfont);
      graph.addXAxis(xbot);
    }
    if(isYTime_) {
      tleft = new TimeAxis(LEFT_AXIS, TimeAxis.AUTO);
      tleft.setRangeU(yRange);
      tleft.setLabelHeightP(labelHeight_);
      tleft.setLocationU(origin);
      tleft.setLabelFont(axfont);
      graph.addYAxis(tleft);
    } else {
      yleft = new PlainAxis(LEFT_AXIS);
      yleft.setRangeU(yRange);
      yleft.setNumberSmallTics(0);
      yleft.setLabelHeightP(labelHeight_);
      yleft.setLocationU(origin);
      yleft.setLabelFont(axfont);
      graph.addYAxis(yleft);
    }
    if(plotType_ == GRID) {
      //
      // create default GridAttribute, and ColorMap
      //
      cmap = new IndexedColorMap(red, green, blue);
      LinearTransform ctrans =
        new LinearTransform(0.0, (double)red.length, 0.0, 1.0);
      ((IndexedColorMap)cmap).setTransform(ctrans);
      gAttr_ = new GridAttribute(GridAttribute.RASTER, cmap);
      gAttr_.addPropertyChangeListener(this);
      colorKey_.setColorMap(cmap);
    }
    //
    layer.setGraph(graph);
  }
  /**
   * @todo implement getLocationSummary
   */
  public String getLocationSummary(SGTData grid) {
    return "";
  }
  /**
   * Add a <code>Collection</code> of lines using the default
   * attributes and description.  The description will be taken from
   * the dependent variable name
   */
  public void addData(Collection lines) {
    if(Debug.DEBUG) System.out.println("JPlotLayout.addData(Collection)");
    addData(lines, null, null);
  }
  /**
   * Add a <code>Collection</code> of lines using the default
   * description.  The description will be taken from
   * the dependent variable name
   */
  public void addData(Collection lines, Attribute attr) {
    if(Debug.DEBUG) System.out.println("JPlotLayout.addData(Collection, Attribute)");
    addData(lines, attr, null);
  }

  /**
   * Add a <code>PointCollection</code>.
   */
  public void addData(PointCollection points, String descrip) {
    if(Debug.DEBUG) System.out.println("JPlotLayout.addData(PointCollection, String)");
    addData((SGTData)points, descrip);
  }

  /**
   * Add a <code>Collection</code> of lines using the default
   * attributes.
   */
  public void addData(Collection lines, String descrip) {
    if(Debug.DEBUG) System.out.println("JPlotLayout.addData(Collection, String)");
    addData(lines, null, descrip);
  }
  /**
   * Add a <code>Collecdtion</code> of lines.
   */
  public void addData(Collection lines, Attribute attr, String descrip) {
    if(Debug.DEBUG) System.out.println("JPlotLayout.addData(Collection, Attribute, String)");
    //    System.out.println("addData(Collection) called");
    for(int i=0; i < lines.size(); i++) {
      SGTLine line = (SGTLine)lines.elementAt(i);
      addData(line, attr, line.getTitle());
    }
  }
  /**
   * Add data to the layout. LineKey descriptor will be
   * taken from the dependent variable name.
   *
   * @param data datum data to be added
   */
  public void addData(SGTData datum) {
    if(Debug.DEBUG) System.out.println("JPlotLayout.addData(SGTData)");
    addData(datum, null, null);
  }
  /**
   * Add data to the layout. LineKey descriptor will be
   * taken from the dependent variable name.
   *
   * @param data datum data to be added
   * @param attr attribute for graphics
   */
  public void addData(SGTData datum, Attribute attr) {
    if(Debug.DEBUG) System.out.println("JPlotLayout.addData(SGTData, Attribute)");
    addData(datum, attr, null);
  }
  /**
   * Add data to the layout.  <code>JPlotLayout</code> will use a
   * default attribute.
   */
  public void addData(SGTData datum, String descrip) {
    if(Debug.DEBUG) System.out.println("JPlotLayout.addData(SGTData, String)");
    addData(datum, null, descrip);
  }
  /**
   * Add data to the layout.  Data will be added to X axis and
   * <code>Z_AXIS</code> will be assigned to Y axis. If this is
   * not the first invocation of <code>addData</code> a new
   * <code>Layer</code> will be created. If overlayed, the
   * <code>Transform</code>s from the first <code>Layer</code>
   * will be attached and <strong>no</strong> axes will be created. If not
   * overlayed, new transforms and axes will be created and
   * adjusted so that the data is horizontally stacked.
   *
   * @param datum data to be added
   * @param descrip LineKey description for datum
   */
  public void addData(SGTData datum, Attribute attr, String descrip) {
    if(Debug.DEBUG) {
      System.out.println("JPlotLayout.addData(SGTData, Attribute, String)");
      if(attr != null && attr instanceof LineAttribute) {
        System.out.println("attr = LineAttribute");
      } else {
        System.out.println("attr = new LineAttribute");
      }
    }
    //
    Layer layer, newLayer;
    CartesianGraph graph, newGraph;
    Axis bottom = null;
    Axis left = null;
    LinearTransform xt, yt;
    SGLabel xtitle, ytitle, lineTitle;
    SGTData data;
    SGTGrid grid = null;
    SGTLine line = null;
    SGTVector vector = null;
    PointCollection points = null;
    LineAttribute lineAttr = null;
    GridAttribute gridAttr = null;
    PointAttribute pointAttr = null;
    VectorAttribute vectorAttr = null;
    String xLabel, yLabel;
    SoTRange xRange = null;
    SoTRange yRange = null;
    SoTRange xnRange = null;
    SoTRange ynRange = null;
    SoTPoint origin = null;
    Range2D vRange = null;
    boolean data_good = true;
    boolean flipX = false;
    boolean flipY = false;
    double save;
    GeoDate savet;
    int len;
    boolean showColorKey = false;
    //
    //      if(!isXTime_ && !(plotType_ == GRID)) {
    //        if(data_.size() == 0) setBaseUnit(Units.getBaseUnit(datum.getXMetaData()));
    //        datum = Units.convertToBaseUnit(datum, getBaseUnit(),
    //                                        Units.X_AXIS);
    //      }
    //
    if(data_.size() == 0) {
      super.addData(datum);
      //
      // only one data set...
      // determine range and titles from data
      //
      data = (SGTData)data_.firstElement();
      if(plotType_ == GRID) {
        // SGTGrid
        grid = (SGTGrid)data;
      } else if(plotType_ == POINTS){
        // SGTPoint
        points = (PointCollection)data;
      } else if(plotType_ == LINE){
        // SGTLine
        line = (SGTLine)data;
      } else if(plotType_ == VECTOR) {
        // SGTVector
        vector = (SGTVector)data;
      }
      //
      // find range of datum (grid, points, or line)
      //
      if(plotType_ == GRID) {
        // SGTGrid
        if(attr != null && attr instanceof GridAttribute) {
          gridAttr = (GridAttribute)attr;
          gridAttr.addPropertyChangeListener(this);
        } else {
          gridAttr = gAttr_;
        }
        addAttribute(datum, gridAttr);
        showColorKey = gridAttr.isRaster();
        vRange = findRange(grid, gridAttr, Z_AXIS);
        if(gridAttr.isRaster()) {
          ColorMap cmap = gridAttr.getColorMap();
          if(cmap instanceof TransformAccess) {
            ((TransformAccess)cmap).setRange(vRange);
          }
        }
        xRange = findSoTRange(grid, gridAttr, X_AXIS);
        yRange = findSoTRange(grid, gridAttr, Y_AXIS);
      } else if((plotType_ == LINE) || (plotType_ == POINTS)){
        // SGTPoint or SGTLine
        xRange = data.getXRange();
        yRange = data.getYRange();
      } else if(plotType_ == VECTOR) {
        // SGTVector
        xRange = findSoTRange(vector, X_AXIS);
        yRange = findSoTRange(vector, Y_AXIS);
      }
      flipX = data.getXMetaData().isReversed();
      flipY = data.getYMetaData().isReversed();
      //
      // check for good points
      //
      data_good = !(xRange.isStartOrEndMissing() ||
                    yRange.isStartOrEndMissing());

      revXAxis_ = flipX;
      revYAxis_ = flipY;

      if(data_good) {
        //
        // flip range if data_good and flipped
        //
        if(flipX) {
          xRange.flipStartAndEnd();
        }
        if(flipY) {
          yRange.flipStartAndEnd();
        }
        //
        // compute "Nice" range
        //
        if(isXTime_) {
          xnRange = xRange;
        } else {
          if(autoRangeX_) {
            xnRange = Graph.computeRange(xRange, autoXIntervals_);
          } else {
            xnRange = xRange;
            ((SoTRange.Double)xnRange).delta = ((SoTRange.Double)Graph.computeRange(xRange, autoXIntervals_)).delta;
          }
        }
        if(isYTime_) {
          ynRange = yRange;
        } else {
          if(autoRangeY_) {
            ynRange = Graph.computeRange(yRange, autoYIntervals_);
          } else {
            ynRange = yRange;
            ((SoTRange.Double)ynRange).delta = ((SoTRange.Double)Graph.computeRange(yRange, autoYIntervals_)).delta;
          }
        }
        //
        // test xnRange and ynRange
        //
        adjustRange(xnRange);
        adjustRange(ynRange);
        origin = new SoTPoint(xnRange.getStart(), ynRange.getStart());
      } // data_good
      //
      // create labels
      //
      xLabel =  data.getXMetaData().getName() +
        " (" + data.getXMetaData().getUnits() + ")";
      yLabel =  data.getYMetaData().getName() +
        " (" + data.getYMetaData().getUnits() + ")";
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
      // create axes
      //
      try {
        Font tfont = new Font("Helvetica", Font.PLAIN, 14);
        xtitle = new SGLabel("xaxis title", xLabel, new Point2D.Double(0.0, 0.0));
        xtitle.setFont(tfont);
        xtitle.setHeightP(titleHeight_);

        bottom = graph.getXAxis(BOTTOM_AXIS);
        bottom.setRangeU(xRange);
        bottom.setLocationU(origin);
        bottom.setTitle(xtitle);

        ytitle = new SGLabel("yaxis title", yLabel, new Point2D.Double(0.0, 0.0));
        ytitle.setFont(tfont);
        ytitle.setHeightP(titleHeight_);

        left = graph.getYAxis(LEFT_AXIS);
        left.setRangeU(ynRange);
        left.setLocationU(origin);
        left.setTitle(ytitle);
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
      if(plotType_ == GRID) {
        // SGTGrid
        graph.setData(grid, gridAttr);
      } else if(plotType_ == POINTS) {
        // SGTPoint
        if(attr != null && attr instanceof PointAttribute) {
          pointAttr = (PointAttribute)attr;
        } else {
          pointAttr = new PointAttribute(markList_[layerCount_%8],
                                         colorList_[layerCount_%8]);
          pointAttr.setMarkHeightP(0.15);
          pointAttr.setLabelHeightP(0.15);
          pointAttr.setDrawLabel(false);
          pointAttr.setLabelColor(Color.red);
          pointAttr.setLabelPosition(PointAttribute.NE);
        }
        pointAttr.addPropertyChangeListener(this);
        addAttribute(datum, pointAttr);
        graph.setData(points, pointAttr);
      } else if(plotType_ == LINE) {
        // SGTLine
        if(isYTime_) {
          len = line.getXArray().length;
        } else {
          len = line.getYArray().length;
        }
        if(attr != null && attr instanceof LineAttribute) {
          lineAttr = (LineAttribute)attr;
        } else {
          if(len >= 2) {
            lineAttr = new LineAttribute(LineAttribute.SOLID,
                                         markList_[layerCount_%8],
                                         colorList_[layerCount_%8]);
          } else {
            lineAttr = new LineAttribute(LineAttribute.MARK,
                                         markList_[layerCount_%8],
                                         colorList_[layerCount_%8]);
          }
        }
        lineAttr.addPropertyChangeListener(this);
        addAttribute(datum, lineAttr);
        graph.setData(line, lineAttr);
      } else if(plotType_ == VECTOR) {
        // SGTVector
        if(attr != null && attr instanceof VectorAttribute) {
          vectorAttr = (VectorAttribute)attr;
        } else {
          vectorAttr = new VectorAttribute(VectorAttribute.SCALED_HEAD,
                                           0.10,
                                           Color.black,
                                           0.10);
        }
        vectorAttr.addPropertyChangeListener(this);
        addAttribute(datum, vectorAttr);
        graph.setData(vector, vectorAttr);
      }
      //
      // add to lineKey
      //
      if(descrip == null) {
        lineTitle = datum.getKeyTitle();
        if(lineTitle == null) {
          lineTitle = new SGLabel("line title",
                                  xLabel,
                                  new Point2D.Double(0.0, 0.0));
        }
      } else {
        lineTitle = new SGLabel("line title",
                                descrip,
                                new Point2D.Double(0.0, 0.0));
      }
      lineTitle.setHeightP(keyHeight_);
      int rowHeight = 1;
      if(!isShowing()) computeScroll_ = true;
      if(plotType_ == GRID) {
        // SGTGrid
        colorKey_.setTitle(lineTitle);
        colorKey_.setColorMap(gridAttr.getColorMap());
        colorKey_.setVisible(showColorKey);
      } else if(plotType_ == POINTS) {
        // SGTPoint
        pointKey_.addPointGraph((PointCartesianRenderer)graph.getRenderer(), lineTitle);
        if(keyPane_ != null) {
          Rectangle vRect = keyPane_.getVisibleRect();
          if(isShowing()) rowHeight = pointKey_.getRowHeight();
          int nrow = vRect.height/rowHeight;
          keyPane_.setScrollableUnitIncrement(1, rowHeight);
          keyPane_.setScrollableBlockIncrement(vRect.width, rowHeight*nrow);
        }
      } else if(plotType_ == LINE) {
        // SGTLine
        lineKey_.addLineGraph((LineCartesianRenderer)graph.getRenderer(), lineTitle);
        if(keyPane_ != null) {
          Rectangle vRect = keyPane_.getVisibleRect();
          if(isShowing()) rowHeight = lineKey_.getRowHeight();
          int nrow = vRect.height/rowHeight;
          keyPane_.setScrollableUnitIncrement(1, rowHeight);
          keyPane_.setScrollableBlockIncrement(vRect.width, rowHeight*nrow);
        }
      } else if(plotType_ == VECTOR) {
        // SGTVector
        vectorKey_.addVectorGraph((VectorCartesianRenderer)graph.getRenderer(),
                                  lineTitle);
        if(keyPane_ != null) {
          Rectangle vRect = keyPane_.getVisibleRect();
          if(isShowing()) rowHeight = vectorKey_.getRowHeight();
          int nrow = vRect.height/rowHeight;
          keyPane_.setScrollableUnitIncrement(1, rowHeight);
          keyPane_.setScrollableBlockIncrement(vRect.width, rowHeight*nrow);
        }
      }
      //
      // update coast
      //
      updateCoastLine();
    } else {     // #of datasets
      //
      // grid can't have more than one data set!!!!
      // more than one data set...
      // add new layer
      //
      if(plotType_ == GRID) return;

      if(datum instanceof SGTLine) {
        if(datum.getYMetaData().isReversed() != revYAxis_) {
          if(Debug.DEBUG) System.out.println("New datum has reversed ZUp!");
          SGTData modified = flipY(datum);
          datum = modified;
        }
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
          bottom = graph.getXAxis(BOTTOM_AXIS);
          left = graph.getYAxis(LEFT_AXIS);
        } catch (AxisNotFoundException e) {}

        if(!inZoom_) {
          //
          // loop over data sets, getting ranges
          //
          SoTRange xTotalRange = null;
          SoTRange yTotalRange = null;

          boolean first = true;

          for (Enumeration e = data_.elements() ; e.hasMoreElements() ;) {
            data = (SGTData)e.nextElement();
            xRange = data.getXRange();
            yRange = data.getYRange();
            flipX = data.getXMetaData().isReversed();
            flipY = data.getYMetaData().isReversed();

            revXAxis_ = flipX;
            revYAxis_ = flipY;

            if(flipX) {
              xRange.flipStartAndEnd();
            }
            if(flipY) {
              yRange.flipStartAndEnd();
            }
            if(first) {
              data_good = !(xRange.isStartOrEndMissing() ||
                            yRange.isStartOrEndMissing());

              if(!data_good) {
                first = true;
              } else {
                first = false;
                data_good = true;
                xTotalRange = xRange;
                yTotalRange = yRange;
              }
            } else {
              //
              // not first
              //
              data_good = !(xRange.isStartOrEndMissing() ||
                            yRange.isStartOrEndMissing());
              if(data_good) {
                xTotalRange.add(xRange);
                yTotalRange.add(yRange);
              } // data_good
            } // first
          } // loop over data elements

          if(data_good) {
            if(isXTime_) {
              xnRange = xTotalRange;
            } else {
              if(autoRangeX_) {
                xnRange = Graph.computeRange(xTotalRange, autoXIntervals_);
              } else {
                xnRange = xTotalRange;
                ((SoTRange.Double)xnRange).delta =
                  ((SoTRange.Double)Graph.computeRange(xTotalRange, autoXIntervals_)).delta;
              }
            }
            if(isYTime_) {
              ynRange = yTotalRange;
            } else {
              if(autoRangeY_) {
                ynRange = Graph.computeRange(yTotalRange, autoYIntervals_);
              } else {
                ynRange = yTotalRange;
                ((SoTRange.Double)ynRange).delta =
                  ((SoTRange.Double)Graph.computeRange(yTotalRange, autoYIntervals_)).delta;
              }
            }
            //
            // fix xnRange and ynRange
            //
            adjustRange(xnRange);
            adjustRange(ynRange);
            origin = new SoTPoint(xnRange.getStart(), ynRange.getStart());
            //
            // axes
            //
            bottom.setRangeU(xnRange);
            bottom.setLocationU(origin);
            //
            left.setRangeU(ynRange);
            left.setLocationU(origin);
            //
            xt.setRangeU(xnRange);
            yt.setRangeU(ynRange);
            updateCoastLine();
          } // data_good
        } // end of !inZoom_
        //
        // create new layer and graph
        //
        newLayer = new Layer("Layer " + (layerCount_+1), new Dimension2D(xSize_, ySize_));
        newGraph = new CartesianGraph("Graph " + (layerCount_+1), xt, yt);
        if(inZoom_) {
          SoTRange xr = null;
          SoTRange yr = null;
          xr = bottom.getSoTRangeU();
          yr = left.getSoTRangeU();
          newGraph.setClip(xr, yr);
          newGraph.setClipping(true);
        } // inZoom_
        if(coastLine_ == null) {
          add(newLayer,0);
        } else {
          add(newLayer,1);
        }
        newLayer.setGraph(newGraph);
        newLayer.invalidate();
        validate();
        //
        // attach data
        //
        if(plotType_ == POINTS) {
          // SGTPoint
          if(attr != null && attr instanceof PointAttribute) {
            pointAttr = (PointAttribute)attr;
          } else {
            pointAttr = new PointAttribute(markList_[layerCount_%8],
                                           colorList_[layerCount_%8]);
            pointAttr.setMarkHeightP(0.15);
            pointAttr.setLabelHeightP(0.15);
            pointAttr.setDrawLabel(false);
            pointAttr.setLabelColor(Color.red);
            pointAttr.setLabelPosition(PointAttribute.NE);
          }
          pointAttr.addPropertyChangeListener(this);
          addAttribute(datum, pointAttr);
          newGraph.setData(datum, pointAttr);
        } else if(plotType_ == LINE) {
          // SGTLine
          if(isYTime_) {
            len = ((SGTLine)datum).getXArray().length;
          } else {
            len = ((SGTLine)datum).getYArray().length;
          }
          if(attr != null && attr instanceof LineAttribute) {
            lineAttr = (LineAttribute)attr;
          } else {
            if(Debug.DEBUG) System.out.println("Create new LineAttribute");
            if(len >= 2) {
              lineAttr = new LineAttribute(LineAttribute.SOLID,
                                           markList_[layerCount_%8],
                                           colorList_[layerCount_%8]);
            } else {
              lineAttr = new LineAttribute(LineAttribute.MARK,
                                           markList_[layerCount_%8],
                                           colorList_[layerCount_%8]);
            }
          }
          lineAttr.addPropertyChangeListener(this);
          addAttribute(datum, lineAttr);
          newGraph.setData(datum, lineAttr);
        } else if(plotType_ == VECTOR) {
          // SGTVector
          /**
           * @todo add vector data to graph
           */
        }
        //
        // add to lineKey
        //
        if(pointKey_ != null) {
          if(descrip == null) {
            xLabel = datum.getXMetaData().getName();
            lineTitle = new SGLabel("line title", xLabel, new Point2D.Double(0.0, 0.0));
          } else {
            lineTitle = new SGLabel("line title", descrip, new Point2D.Double(0.0, 0.0));
          }
          lineTitle.setHeightP(keyHeight_);
          pointKey_.addPointGraph((PointCartesianRenderer)newGraph.getRenderer(), lineTitle);
          if(keyPane_ != null) {
            Rectangle vRect = keyPane_.getVisibleRect();
            int nrow = vRect.height/pointKey_.getRowHeight();
            keyPane_.setScrollableUnitIncrement(1, pointKey_.getRowHeight());
            keyPane_.setScrollableBlockIncrement(vRect.width,
                                                 pointKey_.getRowHeight()*nrow);
          }
        }
        if(lineKey_ != null) {
          if(descrip == null) {
            xLabel = datum.getXMetaData().getName();
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
      } // overlayed
    } // # of datasets
  }
  /**
   * If start == end fix
   */
  private void adjustRange(SoTRange range) {
    if(range.isTime()) {
      long end = range.getEnd().getLongTime();
      long st = range.getStart().getLongTime();
      if(end == st) {
        end += 30*86400000;  // add 30 days
        st  -= 30*86400000;  // substract 30 days
        if(range instanceof SoTRange.Time) {
          ((SoTRange.Time)range).end = end;
          ((SoTRange.Time)range).start = st;
        } else {
          ((SoTRange.GeoDate)range).end = new GeoDate(end);
          ((SoTRange.GeoDate)range).start = new GeoDate(st);
        }
      }
    } else {
      double end = ((SoTRange.Double)range).end;
      double st = ((SoTRange.Double)range).start;
      double dlt = ((SoTRange.Double)range).delta;
      if(dlt == 0) { // delta computation failed
        end = st;
      }
      if(end == st) {
        if(end == 0.0) {
          st = -1.0;
          end =  1.0;
        } else {
          if(end > 0.0) {
            end = 1.1*end;
          } else {
            end = 0.9*end;
          }
          if(st > 0.0) {
            st = 0.9*st;
          } else {
            st = 1.1*st;
          }
        }
        ((SoTRange.Double)range).end = end;
        ((SoTRange.Double)range).start = st;
        ((SoTRange.Double)range).delta =
          ((SoTRange.Double)Graph.computeRange(range, 10)).delta;
      } // end == st
    } // isTime()
  }
  /**
   * Flip the yaxis.  Reverse the direction of the y axis by changing the sign
   * of the axis values and isReversed flag.
   */
  private SGTData flipY(SGTData in) {
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
                               zmetain.isReversed(), zmetain.isModulo());
    zmetaout.setModuloValue(zmetain.getModuloValue());
    zmetaout.setModuloTime(zmetain.getModuloTime());
    out.setXMetaData(line.getXMetaData());
    out.setYMetaData(zmetaout);
    return (SGTData)out;
  }

  public void resetZoom() {
    Attribute attr;
    GridAttribute gridAttr = null;
    SGTData data;
    SoTRange xRange = null;
    SoTRange yRange = null;
    SoTRange xTotalRange = null;
    SoTRange yTotalRange = null;
    boolean data_good = false;
    boolean flipY = false;
    boolean flipX = false;
    double save;
    GeoDate savet;
    boolean batch = isBatch();

    setBatch(true, "JPlotLayout: resetZoom");
    inZoom_ = false;
    setAllClipping(false);
    setClipping(false);
    //
    // loop over data sets, getting ranges
    //
    boolean first = true;
    Enumeration e = data_.elements();
    while (e.hasMoreElements()) {
      data = (SGTData)e.nextElement();
      try {
        attr = getAttribute(data);
      } catch(DataNotFoundException except) {
        System.out.println(except);
        attr = null;
      }
      if(plotType_ == GRID) {
        // SGTGrid
        if(attr != null && attr instanceof GridAttribute) {
          gridAttr = (GridAttribute)attr;
        } else {
          gridAttr = gAttr_;
        }

        xRange = findSoTRange((SGTGrid)data, gridAttr, X_AXIS);
        yRange = findSoTRange((SGTGrid)data, gridAttr, Y_AXIS);
      } else if((plotType_ == POINTS) || (plotType_ == LINE)) {
        // SGTPoint or SGTLine
        xRange = data.getXRange();
        yRange = data.getYRange();
      } else if(plotType_ == VECTOR) {
        // SGTVector
        xRange = findSoTRange((SGTVector)data, X_AXIS);
        yRange = findSoTRange((SGTVector)data, Y_AXIS);
      }
      flipX = data.getXMetaData().isReversed();
      flipY = data.getYMetaData().isReversed();

      revXAxis_ = flipX;
      revYAxis_ = flipY;

      if(flipX) {
        xRange.flipStartAndEnd();
      }
      if(flipY) {
        yRange.flipStartAndEnd();
      }
      if(first) {
        data_good = !(xRange.isStartOrEndMissing() ||
                      yRange.isStartOrEndMissing());

        if(!data_good) {
          first = true;
        } else {
          first = false;
          data_good = true;
          xTotalRange = xRange;
          yTotalRange = yRange;
        }
      } else {
        data_good = !(xRange.isStartOrEndMissing() ||
                      yRange.isStartOrEndMissing());
        if(data_good) {
          xTotalRange.add(xRange);
          yTotalRange.add(yRange);
        } // data_good
      } // first
    } // for loop
    //
    // fix ranges
    //
    if(xTotalRange != null && yTotalRange != null) {
      adjustRange(xTotalRange);
      adjustRange(yTotalRange);
      if(data_good) {
        try {
          setRange(new SoTDomain(xTotalRange, yTotalRange, flipX, flipY));
        } catch (PropertyVetoException ve) {
          System.out.println("zoom reset denied! " + ve);
        }
      }
    }
    // turn off clipping and clip coastline
    //
    inZoom_ = false;
    updateCoastLine();
    if(!batch) setBatch(false, "JPlotLayout: resetZoom");
  }

  public Domain getRange() {
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform xt = (LinearTransform)graph.getXTransform();
    LinearTransform yt = (LinearTransform)graph.getYTransform();
    Range2D xr = null;
    Range2D yr = null;
    TimeRange tr = null;
    if(xt.isTime()) {
      tr = xt.getTimeRangeU();
    } else {
      xr = xt.getRangeU();
    }
    if(yt.isTime()) {
      tr = yt.getTimeRangeU();
    } else {
      yr = yt.getRangeU();
    }
    if(xt.isTime()) {
      return new Domain(tr, yr);
    } else if(yt.isTime()) {
      return new Domain(xr, tr);
    } else {
      return new Domain(xr, yr);
    }
  }
  /**
   * Set the x and y range of the domain.
   *
   * @param reversed the y axis data is reversed
   */
  public void setRange(SoTDomain std)
    throws PropertyVetoException {
    Domain domain = new Domain();
    if(std.isXTime()) {
      SoTRange range = std.getXRange();
      if(range instanceof SoTRange.Time) {
        SoTRange.Time tgeo = (SoTRange.Time)range;
        domain.setXRange(new TimeRange(tgeo.start, tgeo.end));
      } else {
        SoTRange.GeoDate geo = (SoTRange.GeoDate)range;
        domain.setXRange(new TimeRange(geo.start, geo.end));
      }
    } else {
      SoTRange.Double dbl = (SoTRange.Double)std.getXRange();
      domain.setXRange(new Range2D(dbl.start, dbl.end, dbl.delta));
    }
    if(std.isYTime()) {
      SoTRange range = std.getYRange();
      if(range instanceof SoTRange.Time) {
        SoTRange.Time tgeo = (SoTRange.Time)range;
        domain.setYRange(new TimeRange(tgeo.start, tgeo.end));
      } else {
        SoTRange.GeoDate geo = (SoTRange.GeoDate)range;
        domain.setYRange(new TimeRange(geo.start, geo.end));
      }
    } else {
      SoTRange.Double dbl = (SoTRange.Double)std.getYRange();
      domain.setYRange(new Range2D(dbl.start, dbl.end, dbl.delta));
    }
    domain.setXReversed(std.isXReversed());
    domain.setYReversed(std.isYReversed());
    setRange(domain);
  }

  /**
   * Set the x and y range of the domain.
   * <BR><strong>Property Change:</strong> <code>domainRange</code>.
   *
   * @param reversed y axis data is reversed
   */
  public void setRange(Domain domain) throws PropertyVetoException {
    Domain oldRange = getRange();
    if(!domain.equals(oldRange)) {
      boolean batch = isBatch();
      setBatch(true, "JPlotLayout: setRange");
      vetos_.fireVetoableChange("domainRange", oldRange, domain);

      inZoom_ = true;

      if(!domain.isXTime()) {
        setXRange(domain.getXRange());
      } else {
        setXRange(domain.getTimeRange());
      }
      if(!domain.isYTime()) {
        setYRange(domain.getYRange(), domain.isYReversed());
      } else {
        setYRange(domain.getTimeRange());
      }
      changes_.firePropertyChange("domainRange", oldRange, domain);
      if(!batch) setBatch(false, "JPlotLayout: setRange");
      updateCoastLine();
    }
  }

  /**
   * Set the x and y range of the domain.
   */
  public void setRangeNoVeto(Domain domain) {
    //
    // clipping?  hack fix.  how should clipping be done for
    // external range sets?
    //
    if(Debug.DEBUG) System.out.println("setRangeNoVeto: " + domain.toString());
    boolean batch = isBatch();
    setBatch(true, "JPlotLayout: setRangeNoVeto");
    inZoom_ = true;
    setClipping(true);

    if(!domain.isXTime()) {
      setXRange(domain.getXRange());
    } else {
      setXRange(domain.getTimeRange());
    }
    if(!domain.isYTime()) {
      setYRange(domain.getYRange(), domain.isYReversed());
    } else {
      setYRange(domain.getTimeRange());
    }
    if(!batch) setBatch(false, "JPlotLayout: setRangeNoVeto");
    updateCoastLine();
    //    changes_.firePropertyChange("domainRange", oldRange, domain);
  }
  /**
   * Reset the x range. This method is designed to provide
   * zooming functionality.
   *
   * @param trnge new x range
   */
  void setXRange(TimeRange trnge) {
    Axis bottom;
    Axis left;
    SoTRange yr;
    SoTRange xr = new SoTRange.Time(trnge);
    SoTPoint origin;
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform xt = (LinearTransform)graph.getXTransform();
    xt.setRangeU(xr);
    try {
      bottom = graph.getXAxis(BOTTOM_AXIS);
      left = graph.getYAxis(LEFT_AXIS);
      bottom.setRangeU(xr);

      yr = left.getSoTRangeU();
      origin = new SoTPoint(xr.getStart(), yr.getStart());

      bottom.setLocationU(origin);
      left.setLocationU(origin);

      //
      // set clipping
      //
      if(clipping_) {
        setAllClip(xr, yr);
      } else {
        setAllClipping(false);
      }
    } catch (AxisNotFoundException e) {}
  }
  /**
   * Reset the x range. This method is designed to provide
   * zooming functionality.
   *
   * @param rnge new x range
   */
  void setXRange(Range2D rnge) {
    Axis bottom;
    Axis left;
    SoTRange xr = new SoTRange.Double(rnge);
    SoTRange yr;
    SoTPoint origin;
    SoTRange xnRange;
    double save;
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform xt = (LinearTransform)graph.getXTransform();
    if(Debug.DEBUG) System.out.println("setXRange start, end, delta = " +
                                       rnge.start + ", " + rnge.end + ", " + rnge.delta);
    if(autoRangeX_) {
      xnRange = Graph.computeRange(xr, autoXIntervals_);
    } else {
      xnRange = xr;
      ((SoTRange.Double)xnRange).delta =
        ((SoTRange.Double)Graph.computeRange(xr, autoXIntervals_)).delta;
    }
    xt.setRangeU(xnRange);
    try {
      bottom = graph.getXAxis(BOTTOM_AXIS);
      left = graph.getYAxis(LEFT_AXIS);
      yr = left.getSoTRangeU();

      bottom.setRangeU(xnRange);

      xr = bottom.getSoTRangeU();
      origin = new SoTPoint(xr.getStart(), yr.getStart());
      bottom.setLocationU(origin);
      left.setLocationU(origin);
      //
      // set clipping
      //
      if(clipping_) {
        setAllClip(xr, yr);
      } else {
        setAllClipping(false);
      }
    } catch (AxisNotFoundException e) {}
  }
  /**
   * Reset the y range. This method is designed to provide
   * zooming functionality.
   *
   * @param trnge new x range
   */
  void setYRange(TimeRange trnge) {
    Axis bottom;
    Axis left;
    SoTRange xr;
    SoTRange yr = new SoTRange.Time(trnge);
    SoTPoint origin;
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform yt = (LinearTransform)graph.getYTransform();
    yt.setRangeU(yr);
    try {
      bottom = graph.getXAxis(BOTTOM_AXIS);
      left = graph.getYAxis(LEFT_AXIS);
      left.setRangeU(yr);

      xr = bottom.getSoTRangeU();
      origin = new SoTPoint(xr.getStart(), yr.getStart());
      left.setLocationU(origin);

      bottom.setLocationU(origin);
      //
      // set clipping
      //
      if(clipping_) {
        setAllClip(xr, yr);
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
   * @param reversed data is reversed
   */
  void setYRange(Range2D rnge, boolean reversed) {
    SGTData grid;
    Axis bottom;
    Axis left;
    SoTRange xr;
    SoTRange yr = new SoTRange.Double(rnge);
    SoTRange ynRange;
    SoTPoint origin;
    double save;
    boolean flip;
    CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
    LinearTransform yt = (LinearTransform)graph.getYTransform();
/*    if(testZUp && data_.size() > 0) {
      grid = (SGTData)data_.elements().nextElement();
      if(grid.getYMetaData().isReversed()) {
        yr.flipStartAndEnd();
      }
    } */
    if(!data_.isEmpty()) {
      grid = (SGTData)data_.elements().nextElement();
      if(data_.size() > 0 && (reversed != grid.getYMetaData().isReversed())) {
        yr.flipStartAndEnd();
      }
    }
    if(autoRangeY_) {
      ynRange = Graph.computeRange(yr, autoYIntervals_);
    } else {
      ynRange = yr;
      ((SoTRange.Double)ynRange).delta =
        ((SoTRange.Double)Graph.computeRange(yr, autoYIntervals_)).delta;
    }
    //      if(revYAxis_) {
    //        save = ynRange.end;
    //        ynRange.end = ynRange.start;
    //        ynRange.start = save;
    //        ynRange.delta = -ynRange.delta;
    //      }
    yt.setRangeU(ynRange);
    try {
      bottom = graph.getXAxis(BOTTOM_AXIS);
      left = graph.getYAxis(LEFT_AXIS);
      xr = bottom.getSoTRangeU();
      left.setRangeU(ynRange);

      yr = left.getSoTRangeU();
      origin = new SoTPoint(xr.getStart(), yr.getStart());
      left.setLocationU(origin);
      bottom.setLocationU(origin);

      //
      // set clipping
      //
      if(clipping_) {
        setAllClip(xr, yr);
      } else {
        setAllClipping(false);
      }
    } catch (AxisNotFoundException e) {}
  }
  /**
   * Find a dataset from the data's id.
   *
   * @param data_id the id
   * @return <code>SGTData</code>
   */
  public SGTData getData(String data_id) {
    try {
      Layer ly = getLayerFromDataId(data_id);
      if(ly != null) {
        CartesianRenderer rend = ((CartesianGraph)ly.getGraph()).getRenderer();
        if(rend != null) {
          if(rend instanceof LineCartesianRenderer) {
            return (SGTData)((LineCartesianRenderer)rend).getLine();
          } else if(rend instanceof GridCartesianRenderer) {
            return (SGTData)((GridCartesianRenderer)rend).getGrid();
          }
        }
      }
    } catch (LayerNotFoundException e) {}
    return null;
  }
  /**
   * Find a dataset from the renderer.
   *
   * @param rend the renderer
   * @return <code>SGTData</code>
   */
  public SGTData getData(CartesianRenderer rend) {
    if(rend instanceof LineCartesianRenderer) {
      return (SGTData)((LineCartesianRenderer)rend).getLine();
    } else if(rend instanceof GridCartesianRenderer) {
      return (SGTData)((GridCartesianRenderer)rend).getGrid();
    }
    return null;
  }
  /**
   * Remove all data from the <code>JPlotLayout</code>
   */
  public void clear() {
    data_.removeAllElements();
    ((CartesianGraph)firstLayer_.getGraph()).setRenderer(null);
    removeAll();
    add(firstLayer_,0);   // restore first layer
    if(coastLine_ != null) add(coastLayer_, 0);
    if(lineKey_ != null)
      lineKey_.clearAll();
    if(pointKey_ != null)
      pointKey_.clearAll();
    inZoom_ = false;
  }
  /**
   * Remove a specific dataset from the <code>JPlotLayout</code>
   *
   * @param data_id the data id
   */
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
    if(lineKey_ != null)
      lineKey_.clear(data_id);
    if(pointKey_ != null)
      pointKey_.clear(data_id);
    if(getComponentCount() <= 0 || ly.equals(firstLayer_)) {
      ((CartesianGraph)firstLayer_.getGraph()).setRenderer(null);
      add(firstLayer_,0);  // restore first layer
    }
  }
  /**
   * Get the <code>JPlotLayout</code> layer size in physical
   * coordinates.
   */
  public Dimension2D getLayerSizeP() {
    return new Dimension2D(xSize_, ySize_);
  }

  public Layer getFirstLayer() {
    return firstLayer_;
  }
  /**
   * Set the axes origin in physical units
   */
  public void setAxesOriginP(Point2D.Double pt) {
    xMin_ = pt.x;
    yMin_ = pt.y;
  }
  /**
   * @since 3.0
   */
  public SoTDomain getGraphDomain() {
    SoTRange xRange = null;
    SoTRange yRange = null;
    CartesianGraph graph = null;
    try {
      Layer layer = getLayer("Layer 1");
      graph = (CartesianGraph)layer.getGraph();
    } catch (LayerNotFoundException e) {
      return null;
    }
    try {
      Axis bottom = graph.getXAxis(BOTTOM_AXIS);
      Axis left = graph.getYAxis(LEFT_AXIS);
      xRange = bottom.getSoTRangeU();
      yRange = left.getSoTRangeU();
    } catch (AxisNotFoundException e) {
      return null;
    }
    return new SoTDomain(xRange, yRange);
  }
  /**
   * Set the layer size in physical units
   */
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
    double ypos = ySize_ - 1.0f*mainTitleHeight_;
    mainTitle_.setLocationP(new Point2D.Double(xpos, ypos));
    ypos = ypos - 1.0f*warnHeight_;
    title2_.setLocationP(new Point2D.Double(xpos, ypos));
    ypos = ypos - 1.0f*warnHeight_;
    title3_.setLocationP(new Point2D.Double(xpos, ypos));
    //      if(plotType_ == GRID) {
    //        if(keyPane_ == null) {
    //          colorKey_.setLocationP(new Point2D.Double(xSize_*0.5, 0.0));
    //        }
    //      } else {
    //        if(keyPane_ == null) {
    //          lineKey_.setLocationP(new Point2D.Double(xSize_*0.5, 0.01));
    //        }
    //      }
  }
  /**
   * Set the main and secondary label heights in physical units
   *
   * @param main main label height
   * @param second second and third label height
   */
  public void setTitleHeightP(double main, double second) {
//    mainTitleHeight_ = main;
//    warnHeight_ = second;
    //
    // title
    //
    double ypos = ySize_ - 1.0f*main;
    double xpos = mainTitle_.getLocationP().x;
    boolean batch = isBatch();
    setBatch(true, "JPlotLayout: setTitleHeightP");
    if(main != mainTitleHeight_) {
      mainTitleHeight_ = main;
      mainTitle_.setHeightP(main);
      mainTitle_.setLocationP(new Point2D.Double(xpos, ypos));
    }
    if(second != warnHeight_) {
      warnHeight_ = second;
      ypos = ypos - 1.0f*second;
      title2_.setHeightP(second);
      title2_.setLocationP(new Point2D.Double(xpos, ypos));
      ypos = ypos - 1.0f*warnHeight_;
      title3_.setHeightP(second);
      title3_.setLocationP(new Point2D.Double(xpos, ypos));
    }
    if(!batch) setBatch(false, "JPlotLayout: setTitleHeightP");
  }
  /**
   * Get main label height in physical units
   */
  public double getMainTitleHeightP() {
    return mainTitleHeight_;
  }
  /**
   * Get second and third label heights in physical units
   */
  public double getSecondaryTitleHeightP() {
    return warnHeight_;
  }
  /**
   * Set the key size in physical units
   */
  public void setKeyBoundsP(Rectangle2D.Double bounds){
    if((plotType_ == GRID) && (colorKey_ != null)) {
      // SGTGrid
      colorKey_.setBoundsP(bounds);
    } else if((plotType_ == POINTS) && (pointKey_ != null)) {
      // SGTPoint
      pointKey_.setBoundsP(bounds);
    } else if((plotType_ == LINE) && (lineKey_ != null)) {
      // SGTLine
      lineKey_.setBoundsP(bounds);
    } else if((plotType_ == VECTOR) && (vectorKey_ != null)) {
      // SGTVector
      vectorKey_.setBoundsP(bounds);
    }
  }
  /**
   * Get the key size in physical units
   */
  public Rectangle2D.Double getKeyBoundsP() {
   if((plotType_ == GRID) && (colorKey_ != null)) {
      // SGTGrid
      return colorKey_.getBoundsP();
    } else if((plotType_ == POINTS) && (pointKey_ != null)) {
      // SGTPoint
      return pointKey_.getBoundsP();
    } else if((plotType_ == LINE) && (lineKey_ != null)) {
      // SGTLine
      return lineKey_.getBoundsP();
    } else if((plotType_ == VECTOR) && (vectorKey_ != null)) {
      // SGTVector
      return vectorKey_.getBoundsP();
    }
    return null;
  }
  /**
   * Set the key alignment
   *
   * @param vert vertical alignment
   * @param horz horizontal alignment
   *
   * @see ColorKey
   * @see LineKey
   * @see PointCollectionKey
   */
  public void setKeyAlignment(int vert, int horz) {
    if((plotType_ == GRID) && (colorKey_ != null)) {
      // SGTGrid
      colorKey_.setAlign(vert, horz);
    } else if((plotType_ == POINTS) && (pointKey_ != null)) {
      // SGTPoint
      pointKey_.setAlign(vert, horz);
    } else if((plotType_ == LINE) && (lineKey_ != null)) {
      // SGTLine
      lineKey_.setAlign(vert, horz);
    } else if((plotType_ == VECTOR) && (vectorKey_ != null)) {
      // SGTVector
      vectorKey_.setAlign(vert, horz);
    }
  }
  /**
   * Get the key position in physical units
   */
  public Point2D.Double getKeyPositionP() {
    Rectangle2D.Double bnds = getKeyBoundsP();
    double xp = bnds.x;
    double yp = bnds.y;
    return new Point2D.Double(xp, yp);
  }
  /**
   * Set the key position in physical units
   */
  public void setKeyLocationP(Point2D.Double loc) {
    if(keyPane_ == null) {
      if(plotType_ == GRID) {
        // SGTGrid
        colorKey_.setLocationP(loc);
      } else if(plotType_ == POINTS) {
        // SGTPoint
        pointKey_.setLocationP(loc);
      } else if(plotType_ == LINE) {
        // SGTLine
        lineKey_.setLocationP(loc);
      } else if(plotType_ == VECTOR) {
        // SGTVector
        vectorKey_.setLocationP(loc);
      }
    }
  }

  /* reversable axes not yet fully implemented */
  //  public boolean isXAxisReversed() {
  //    return revXAxis_;
  //  }

  //  public boolean isYAxisReversed() {
  //    return revYAxis_;
  //  }

  //  public void setXAxisReversed(boolean rev) {
  //    revXAxis_ = rev;
    //      resetAxes();
  //  }

  //  public void setYAxisReversed(boolean rev) {
  //    revYAxis_ = rev;
    //      resetAxes();
  //  }

  private void resetAxes() {
    Domain domain = getRange();
    if(domain.isXTime()) {
      setXRange(domain.getTimeRange());
    } else {
      setXRange(domain.getXRange());
    }
    if(domain.isYTime()) {
      setYRange(domain.getTimeRange());
    } else {
      setYRange(domain.getYRange());
    }
    //    draw();
  }
  /**
   * Used by <code>JPlotLayout</code> to listen for changes in line,
   * grid, vector, and point attributes.
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if(Debug.EVENT) {
      System.out.println("JPlotLayout: " + evt);
      System.out.println("                " + evt.getPropertyName());
    }
    if(evt.getSource() instanceof GridAttribute &&
       evt.getPropertyName() == "style" && (plotType_ == GRID)) {
      // SGTGrid
      SGTGrid grid = (SGTGrid)data_.firstElement();
      try{
        GridAttribute gridAttr = (GridAttribute)getAttribute(grid);
        Range2D vRange = findRange(grid, gridAttr, Z_AXIS);
        if(gridAttr.isRaster()) {
          ColorMap cmap = gridAttr.getColorMap();
          if(cmap instanceof TransformAccess) {
            ((TransformAccess)cmap).setRange(vRange);
          }
          colorKey_.setColorMap(cmap);
          colorKey_.setVisible(true);
        } else {
          colorKey_.setVisible(false);
        }
      } catch (DataNotFoundException e) {
        System.out.println(e);
      }
      if(keyPane_ != null) {
        keyPane_.setModified(true, "JPlotLayout: forced setModified");
        keyPane_.setBatch(false, "JPlotLayout: propertyChange");
      }
    }
    if(evt.getSource() instanceof Attribute) {
      boolean local = true;
      if(evt instanceof AttributeChangeEvent) {
        local = ((AttributeChangeEvent)evt).isLocal();
      }
      if(Debug.EVENT) System.out.println("JPlotLayout: Attribute change: " + evt.getPropertyName());
      changes_.firePropertyChange(new AttributeChangeEvent(this,
                                                           "attribute",
                                                           null,
                                                           evt.getSource(),
                                                           local));
    }
  }
  /**
   * Set the coastline.
   */
  public void setCoastLine(SGTLine coast) {
    if(coastLine_ == null) {
      CartesianGraph graph = (CartesianGraph)firstLayer_.getGraph();
      LinearTransform xt = (LinearTransform)graph.getXTransform();
      LinearTransform yt = (LinearTransform)graph.getYTransform();
      Range2D xrange = xt.getRangeU();
      Range2D yrange = yt.getRangeU();
      coastLayer_ = new Layer("CoastLine", new Dimension2D(xSize_, ySize_));
      add(coastLayer_, 0);
      coastLine_ = new CartesianGraph("CoastLine Graph");
      coastLayer_.setGraph(coastLine_);
      coastLine_.setXTransform(xt);
      coastLine_.setYTransform(yt);
      LineAttribute lattr = new LineAttribute();
      lattr.setColor(new Color(244,164, 96));
      lattr.addPropertyChangeListener(this);
      coastLine_.setData(coast, lattr);
      coastLine_.setClip(xrange.start, xrange.end, yrange.start, yrange.end);
      coastLine_.setClipping(true);
      //
      coastLayer_.invalidate();
      validate();
    }
  }

  void updateCoastLine() {
    if(coastLine_ != null) {
      CartesianGraph graph = (CartesianGraph)coastLayer_.getGraph();
      LinearTransform xt = (LinearTransform)graph.getXTransform();
      LinearTransform yt = (LinearTransform)graph.getYTransform();
      Range2D xrange = xt.getRangeU();
      Range2D yrange = yt.getRangeU();
      coastLine_.setClip(xrange.start, xrange.end, yrange.start, yrange.end);
      coastLine_.setClipping(true);
      coastLayer_.invalidate();
      validate();
    }
  }
  /**
   * Implements the <code>print</code> method in
   * <code>java.awt.print.Printable</code>.  Overrides <code>JPane</code> behavior.
   */
  public int print(Graphics g, PageFormat pf, int pageIndex) {
    if(pageIndex > 0) {
      return NO_SUCH_PAGE;
    } else {
      if(Debug.DEBUG) {
        System.out.println("Imageable(X,Y): " + pf.getImageableX() +
                           ", " + pf.getImageableY());
        System.out.println("Imageable(h,w): " + pf.getImageableHeight() +
                           ", " + pf.getImageableWidth());
        System.out.println("Paper(h,w): " + pf.getHeight() +
                           ", " + pf.getWidth());
      }
      Graphics2D g2 = (Graphics2D)g;
      drawPage(g2, pf);
      if(keyPane_ != null) {
        g2.setTransform(new AffineTransform());
        Point pt = keyPane_.getLocation();
        double scale = 72.0;
        double margin = 0.5; // 0.5 inches
        int layoutHeight = (int)((getLayerSizeP().getHeight() + margin)*scale)
                              - pt.y;
        int xoff = -pt.x;
        Point offset = new Point(xoff, layoutHeight);
        keyPane_.setPageOrigin(offset);
        keyPane_.setPageVAlign(SPECIFIED_LOCATION);
        keyPane_.setPageHAlign(CENTER);
        g2.setClip(-1000, -1000, 5000, 5000);
        keyPane_.drawPage(g2, pf, true);
      }
      return PAGE_EXISTS;
    }
  }
  /**
   * Turn on/off the auto range feature for the x axis. Auto range creates a
   * "nice" range with a delta of 1., 2., 5. or 10^n of these.  The start
   * and end of the range is extended to the next full delta.
   */
  public void setXAutoRange(boolean xauto) {
    autoRangeX_ = xauto;
  }
  /**
   * Turn on/off the auto range feature for the y axis. Auto range creates a
   * "nice" range with a delta of 1., 2., 5. or 10^n of these.  The start
   * and end of the range is extended to the next full delta.
   */
  public void setYAutoRange(boolean yauto) {
    autoRangeY_ = yauto;
  }
  /**
   * Turn on/off the auto range feature for the x and y axes. Auto range
   * creates a "nice" range with a delta of 1., 2., 5. or 10^n of these.
   * The start and end of the range is extended to the next full delta.
   */
  public void setAutoRange(boolean xauto, boolean yauto) {
    autoRangeX_ = xauto;
    autoRangeY_ = yauto;
  }
  /**
   * Tests if the auto range feature is enabled for the x axis.
   */
  public boolean isXAutoRange() {
    return autoRangeX_;
  }
  /**
   * Tests if the auto range feature is enabled for the y axis.
   */
  public boolean isYAutoRange() {
    return autoRangeY_;
  }
  /**
   * Set the approximate number of x axis intervals for auto range.
   */
  public void setXAutoIntervals(int xint) {
    autoXIntervals_ = xint;
  }
  /**
   * Set the approximate number of y axis intervals for auto range.
   */
  public void setYAutoIntervals(int yint) {
    autoYIntervals_ = yint;
  }
  /**
   * Set the approximate number of x and y axes intervals for auto range.
   */
  public void setAutoIntervals(int xint, int yint) {
    autoXIntervals_ = xint;
    autoYIntervals_ = yint;
  }
  /**
   * Return the number of intervals for the x axis.
   */
  public int getXAutoIntervals() {
    return autoXIntervals_;
  }
  /**
   * Return the number of intervals for the y axis.
   */
  public int getYAutoIntervals() {
    return autoYIntervals_;
  }
  /**
   * Override JPane init method.  The scrolling list parameters are computed
   * here if necessary.
   */
  public void init() {
    if(Debug.DEBUG) System.out.println("JPLotLayout: init()");
    if(computeScroll_) {
      computeScroll_ = false;
      int rowHeight = 1;
      if(plotType_ == GRID) {
        // SGTGrid
      } else if(plotType_ == POINTS) {
        // SGTPoint
        if(keyPane_ != null) {
          Rectangle vRect = keyPane_.getVisibleRect();
          rowHeight = pointKey_.getRowHeight();
          int nrow = vRect.height/rowHeight;
          keyPane_.setScrollableUnitIncrement(1, rowHeight);
          keyPane_.setScrollableBlockIncrement(vRect.width, rowHeight*nrow);
        }
      } else if(plotType_ == LINE) {
        // SGTLine
        if(keyPane_ != null) {
          Rectangle vRect = keyPane_.getVisibleRect();
          rowHeight = lineKey_.getRowHeight();
          int nrow = vRect.height/rowHeight;
          keyPane_.setScrollableUnitIncrement(1, rowHeight);
          keyPane_.setScrollableBlockIncrement(vRect.width, rowHeight*nrow);
        }
      } else if(plotType_ == VECTOR) {
        // SGTVector
        if(keyPane_ != null) {
          Rectangle vRect = keyPane_.getVisibleRect();
          rowHeight = vectorKey_.getRowHeight();
          int nrow = vRect.height/rowHeight;
          keyPane_.setScrollableUnitIncrement(1, rowHeight);
          keyPane_.setScrollableBlockIncrement(vRect.width, rowHeight*nrow);
        }
      }
    }
  }
}
