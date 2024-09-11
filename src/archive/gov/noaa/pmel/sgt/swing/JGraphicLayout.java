/*
 * $Id: JGraphicLayout.java,v 1.24 2003/08/22 23:02:39 dwd Exp $
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

import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.Pane;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.AxisTransform;
import gov.noaa.pmel.sgt.LineCartesianRenderer;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.PlainAxis;
import gov.noaa.pmel.sgt.TimeAxis;
import gov.noaa.pmel.sgt.Logo;
import gov.noaa.pmel.sgt.DataNotFoundException;

import gov.noaa.pmel.util.Domain;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Units;
import gov.noaa.pmel.util.SoTRange;

import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.Collection;
import gov.noaa.pmel.sgt.dm.SGTVector;

import gov.noaa.pmel.sgt.swing.prop.SGLabelDialog;
import gov.noaa.pmel.sgt.swing.prop.TimeAxisDialog;
import gov.noaa.pmel.sgt.swing.prop.SpaceAxisDialog;
import gov.noaa.pmel.sgt.swing.prop.LogoDialog;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.awt.Image;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.text.DecimalFormat;

import java.beans.PropertyVetoException;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeSupport;
import java.beans.VetoableChangeListener;

/**
 * <code>JGraphicLayout</code> is a abstract class that provides
 * the basis for pre-defined layouts using the
 * <code>CartesianGraph</code> class. <code>JGraphicLayout</code>
 * extends <code>JPane</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.24 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 * @see CartesianGraph
 * @see JPlotLayout
**/
public abstract class JGraphicLayout extends JPane {
  /**
   * Use X array from <code>SGTData</code>.
   */
  public static final int X_AXIS = 1;
  /**
   * Use Y array from <code>SGTData</code>.
   */
  public static final int Y_AXIS = 2;
  /**
   * Use Z array from <code>SGTData</code>.
   */
  public static final int Z_AXIS = 3;
  /** Width of graph in physical units */
  protected static double XSIZE_ = 6.00;
  /** Start of X axis in physical units */
  protected static double XMIN_  = 0.60;
  /** End of X axis in physical units */
  protected static double XMAX_  = 5.40;
  /** Height of graph in physical units */
  protected static double YSIZE_ = 4.50;
  /** Start of Y axis in physical units */
  protected static double YMIN_  = 0.75;
  /** End of Y axis in physical units */
  protected static double YMAX_  = 3.30;
  //
  /** Height of main title in physical units */
  protected static double MAIN_TITLE_HEIGHT_ = 0.25;
  /** Height of axis title in physical units */
  protected static double TITLE_HEIGHT_ = 0.22;
  /** Height of axis labels in physical units */
  protected static double LABEL_HEIGHT_ = 0.18;
  /** Height of 2nd and 3rd main titles */
  protected static double WARN_HEIGHT_ = 0.15;
  /** Height of line or color key labels */
  protected static double KEY_HEIGHT_ = 0.16;
  //  protected static double KEY_HEIGHT_ = 0.20;
  //
  /** Width of key if in separate pane */
  protected static double XKEYSIZE_ =  6.00;
  /** Height of key if in separate pane */
  protected static double YKEYSIZE_ = 12.00;
  //
  /** Main pane color */
  protected static Color PANE_COLOR = Color.white;
  /** Key pane color */
  protected static Color KEYPANE_COLOR = Color.white;
  //
  /** Base units of data */
  protected int base_units_ = Units.NONE;
  private JGraphicLayout me_;
  /** Key pane reference */
  protected JPane keyPane_;
  /** <code>SGTData</code> storage */
  protected Vector data_;
  /** Mapping of data to attributes */
  protected Hashtable dataAttrMap_ = new Hashtable();
  /** Identification of graph */
  protected String ident_;
  /** Layers are overlayed */
  protected boolean overlayed_;
  /** Data is clipped to axes */
  protected boolean clipping_ = false;
  /** Optional image */
  protected Image iconImage_ = null;

  /**
   * Titles for graph
   * @link aggregation
   * @undirected
   * @label titles
   */
  protected SGLabel mainTitle_, title2_, title3_;
  /** Reference to Mouse event handler */
  protected SymMouse aSymMouse_;
  //
  /** Allow editing of <code>sgt</code> object properties */
  protected boolean editClasses_ = true;
  /** Reference to <code>SGLabelDialog</code> */
  protected SGLabelDialog sg_props_;
  /** Reference to <code>SpaceAxisDialog</code> */
  protected SpaceAxisDialog pa_props_;
  /** Reference to <code>TimeAxisDialog</code> */
  protected TimeAxisDialog ta_props_;
  /** Reference to <code>LogoDialog</code> */
  protected LogoDialog lo_props_;
  //
  /** Reference to <code>PropertyChangeSupport</code> */
  protected PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  /** Reference to <code>VetoableChangeSupport</code> */
  protected VetoableChangeSupport vetos_ = new VetoableChangeSupport(this);
  /**
   * Default constructor
   */
  public JGraphicLayout() {
    this("", null, new Dimension(50,50));
  }
  /**
   * <code>JGraphicLayout</code> constructor.
   *
   * @param id identifier
   * @param img icon image
   * @see JPlotLayout
   */
  public JGraphicLayout(String id, Image img) {
    this(id, img, new Dimension(50,50));
  }
  /**
   * <code>JGraphicLayout</code> constructor.
   *
   * @param id identifier
   * @param img icon image
   * @param size graph size in device units
   * @see JPlotLayout
   */
  public JGraphicLayout(String id, Image img, Dimension size) {
    super(id, size);
    ident_ = id;
    overlayed_ = true;
    data_ = new Vector(10);
    iconImage_ = img;
    mainTitle_ = null;
    title2_ = null;
    title3_ = null;
    me_ = this;
    if(aSymMouse_ == null) aSymMouse_ = new SymMouse();
    addMouseListener(aSymMouse_);
  }
  /**
   * Set the identifier
   *
   * @param id layout identifier
   */
  public void setId(String id) {
    ident_ = id;
  }
  /**
   * Get the identifier
   *
   * @return layout identifier
   */
  public String getId() {
    return ident_;
  }
  /**
   * Set the plot titles.
   *
   * @param title main plot title
   * @param title2 secondary plot title
   * @param title3 tertiary plot title
   */
  public void setTitles(String title, String title2, String title3) {
    if(mainTitle_ != null) mainTitle_.setText(title);
    if(title2_ != null) title2_.setText(title2);
    if(title3_ != null) title3_.setText(title3);
  }
  /**
   * Set the base units. The base units are set automatically based
   * on the first <code>SGTData</code> added to the list. Other
   * <code>SGTData</code> objects added
   * thereafter will be converted to the standard display units for each
   * base unit type.  (TEMPERATURE, default units are "degC"; VELOCITY,
   * default units are "m/s"; DISTANCE, default unis are "m"). NOTE: Presently
   * the units supported are very limited.
   *
   * @see gov.noaa.pmel.util.Units#NONE
   * @see gov.noaa.pmel.util.Units#TEMPERATURE
   * @see gov.noaa.pmel.util.Units#VELOCITY
   * @see gov.noaa.pmel.util.Units#DISTANCE
   */
  public void setBaseUnit(int base) {
    base_units_ = base;
  }
  /**
   * Get the base units
   *
   * @return the current base units for the layout
   */
  public int getBaseUnit() {
    return base_units_;
  }
  /**
   * Set flag to overlay the layers.
   *
   * @param over if true overlay layers if false stack
   */
  public void setOverlayed(boolean over) {
    overlayed_ = over;
  }
  /**
   * Layer overlay flag.
   *
   * @return true if layers will be overlayed
   */
  public boolean isOverlayed() {
    return overlayed_;
  }
  /**
   * Get icon image
   *
   * @return icon image
   */
  public Image getIconImage() {
    return iconImage_;
  }
  /**
   * Get KeyPane object
   *
   * @return pane
   */
  public JPane getKeyPane() {
    return keyPane_;
  }
  /**
   * Is there a key pane?
   */
  public boolean isKeyPane() {
    return (keyPane_ != null);
  }
  /**
   * Add data to the layout.  Where data is added is dependent on the
   * specific layout. Additional layers will be created as required.
   *
   * @param data data to be added
   */
  public void addData(SGTData data) {
    data_.addElement(data);
  }
  /**
   * Associate <code>SGTData</code> with an
   * <code>Attribute</code>. The associations are managed by a
   * <code>Hashtable</code> object.
   */
  public void addAttribute(SGTData data, Attribute attr) {
    dataAttrMap_.put(data, attr);
  }
  /**
   * Find an <code>Attribute</code> given a <code>SGTData</code>
   * object.
   */
  public Attribute getAttribute(SGTData data)
    throws DataNotFoundException {
    Attribute attr = (Attribute)dataAttrMap_.get(data);
    if(attr == null) {
      throw new DataNotFoundException();
    }
    return attr;
  }
  /**
   * Find an <code>Attribute</code> given an id.
   *
   * @since 3.0
   */
  public Attribute findAttribute(String id) {
    Attribute attr = null;
    Enumeration e = dataAttrMap_.elements();
    while(e.hasMoreElements()) {
      attr = (Attribute)e.nextElement();
      if(attr.getId().equals(id)) return attr;
    }
    return attr;
  }
  /**
   * Add data to the plot
   */
  public abstract void addData(SGTData data, String label);
  /**
   * Construct a string that summarizes the location of the data.
   */
  public abstract String getLocationSummary(SGTData grid);
  /**
   * Find the range of the <code>SGTLine</code> object in the specific
   * direction.
   *
   * @param data SGTLine object
   * @param dir direction
   * @see CartesianGraph
   */
  public Range2D findRange(SGTLine data, int dir) {
    int num, i, first=0;
    double amin=0.0, amax=0.0;
    double[] values;
    boolean good = false;

    switch(dir) {
    case X_AXIS:
      values = data.getXArray();
      num = values.length;
      break;
    default:
    case Y_AXIS:
      values = data.getYArray();
      num = values.length;
    }
    for(i=0; i < num; i++) {
      if(!Double.isNaN(values[i])) {
        amin = (double)values[i];
        amax = (double)values[i];
        good = true;
        first = i+1;
        break;
      }
    }
    if(!good) {
      return new Range2D(Double.NaN, Double.NaN);
    } else {
      for(i=first; i < num; i++) {
        if(!Double.isNaN(values[i])) {
          amin = Math.min(amin, (double)values[i]);
          amax = Math.max(amax, (double)values[i]);
        }
      }
    }
    return new Range2D(amin, amax);
  }
  /**
   * Find the range of the <code>SGTLine</code> object in the specific
   * direction.
   *
   * @param data SGTLine object
   * @param dir direction
   * @return range as an <code>SoTRange</code> object
   * @see CartesianGraph
   */
  public SoTRange findSoTRange(SGTLine line, int dir) {
    switch(dir) {
    case X_AXIS:
      return line.getXRange();
    case Y_AXIS:
      return line.getYRange();
    default:
      return null;
    }
  }
  /**
   * Find the range of the <code>SGTVector</code> object in the
   * specified direction.  Uses the U component to find X, Y ranges.
   *
   * @param data the data vector
   * @param dir the direction
   * @return range as an <code>SoTRange</code> object
   */
  public SoTRange findSoTRange(SGTVector data, int dir) {
    double[] veclen;
    int num, i, first = 0;
    double amin = 0.0, amax = 0.0;
    boolean good = false;

    switch(dir) {
    case X_AXIS:
      return data.getU().getXRange();
    case Y_AXIS:
      return data.getU().getYRange();
    default:
    case Z_AXIS:
      double[] ucomp = data.getU().getZArray();
      double[] vcomp = data.getV().getZArray();
      veclen = new double[ucomp.length];
      for(i=0; i < veclen.length; i++) {
        veclen[i] = Math.sqrt(ucomp[i]*ucomp[i] + vcomp[i]*vcomp[i]);
      }
      num = veclen.length;
    }
    for(i=0; i < num; i++) {
      if(!Double.isNaN(veclen[i])) {
        amin = (double)veclen[i];
        amax = (double)veclen[i];
        good = true;
        first = i+1;
        break;
      }
    }
    if(!good) {
      return new SoTRange.Double(Double.NaN, Double.NaN);
    } else {
      for(i=first; i < num; i++) {
        if(!Double.isNaN(veclen[i])) {
          amin = Math.min(amin, (double)veclen[i]);
          amax = Math.max(amax, (double)veclen[i]);
        }
      }
    }
    return new SoTRange.Double(amin, amax);
  }
  /**
   * Find the range of the <code>SGTGrid</code> object in the
   * specified direction.
   *
   * @param data the data grid
   * @param attr the grid attribute
   * @param dir the direction
   * @return range as an <code>SoTRange</code> object
   */
  public SoTRange findSoTRange(SGTGrid data, GridAttribute attr, int dir) {
    int num, onum, i, first=0;
    double amin=0.0, amax=0.0;
    GeoDate tmin, tmax;
    double[] values, orig;
    GeoDate[] taxis, torig;
    boolean good = false;

    if(attr.isRaster() &&
       ((data.isXTime() && (dir == X_AXIS) && !data.hasXEdges()) ||
            (data.isYTime() && (dir == Y_AXIS) && !data.hasYEdges()))) {
        torig = data.getTimeArray();
        onum = torig.length;
        taxis = new GeoDate[onum+1];
        taxis[0] = torig[0].subtract(
                   (torig[1].subtract(torig[0])).divide(2.0));
        for(i=1; i < onum; i++) {
          taxis[i] = (torig[i-1].add(torig[i])).divide(2.0);
        }
        taxis[onum] = torig[onum-1].add(
                      (torig[onum-1].subtract(torig[onum-2])).divide(2.0));
        num = taxis.length;
        return new SoTRange.Time(taxis[0].getTime(),
                                 taxis[num-1].getTime());
    }

    switch(dir) {
    case X_AXIS:
      if(attr.isRaster()) {
        if(data.hasXEdges()) {
          return data.getXEdgesRange();
        } else {
          orig = data.getXArray();
          onum = orig.length;
          values = new double[onum+1];
          values[0] = orig[0]-(orig[1]-orig[0])*0.5;
          for(i=1; i < onum; i++) {
            values[i] = (orig[i-1]+orig[i])*0.5;
          }
          values[onum] = orig[onum-1]+(orig[onum-1]-orig[onum-2])*0.5;
          num = values.length;
        }
      } else {
        return data.getXRange();
      }
      break;
    case Y_AXIS:
      if(attr.isRaster()) {
        if(data.hasYEdges()) {
          return data.getYEdgesRange();
        } else {
          orig = data.getYArray();
          onum = orig.length;
          values = new double[onum+1];
          values[0] = orig[0]-(orig[1]-orig[0])*0.5;
          for(i=1; i < onum; i++) {
            values[i] = (orig[i-1]+orig[i])*0.5;
          }
          values[onum] = orig[onum-1]+(orig[onum-1]-orig[onum-2])*0.5;
          num = values.length;
        }
      } else {
        return data.getYRange();
      }
      break;
    default:
    case Z_AXIS:
      values = data.getZArray();
      num = values.length;
    }
    for(i=0; i < num; i++) {
      if(!Double.isNaN(values[i])) {
        amin = (double)values[i];
        amax = (double)values[i];
        good = true;
        first = i+1;
        break;
      }
    }
    if(!good) {
      return new SoTRange.Double(Double.NaN, Double.NaN);
    } else {
      for(i=first; i < num; i++) {
        if(!Double.isNaN(values[i])) {
          amin = Math.min(amin, (double)values[i]);
          amax = Math.max(amax, (double)values[i]);
        }
      }
    }
    return new SoTRange.Double(amin, amax);
  }
  /**
   * Find the range of the <code>SGTGrid</code> object in the
   * specified direction.
   *
   * @param data the data grid
   * @param attr the grid attribute
   * @param dir the direction
   */
  public Range2D findRange(SGTGrid data, GridAttribute attr, int dir) {
    int num, onum, i, first=0;
    double amin=0.0, amax=0.0;
    double[] values, orig;
    boolean good = false;

    switch(dir) {
    case X_AXIS:
      if(attr.isRaster()) {
        if(data.hasXEdges()) {
          values = data.getXEdges();
          num = values.length;
        } else {
          orig = data.getXArray();
          onum = orig.length;
          values = new double[onum+1];
          values[0] = orig[0]-(orig[1]-orig[0])*0.5;
          for(i=1; i < onum; i++) {
            values[i] = (orig[i-1]+orig[i])*0.5;
          }
          values[onum] = orig[onum-1]+(orig[onum-1]-orig[onum-2])*0.5;
          num = values.length;
        }
      } else {
        values = data.getXArray();
        num = values.length;
      }
      break;
    case Y_AXIS:
      if(attr.isRaster()) {
        if(data.hasYEdges()) {
          values = data.getYEdges();
          num = values.length;
        } else {
          orig = data.getYArray();
          onum = orig.length;
          values = new double[onum+1];
          values[0] = orig[0]-(orig[1]-orig[0])*0.5;
          for(i=1; i < onum; i++) {
            values[i] = (orig[i-1]+orig[i])*0.5;
          }
          values[onum] = orig[onum-1]+(orig[onum-1]-orig[onum-2])*0.5;
          num = values.length;
        }
      } else {
        values = data.getYArray();
        num = values.length;
      }
      break;
    default:
    case Z_AXIS:
      values = data.getZArray();
      num = values.length;
    }
    for(i=0; i < num; i++) {
      if(!Double.isNaN(values[i])) {
        amin = (double)values[i];
        amax = (double)values[i];
        good = true;
        first = i+1;
        break;
      }
    }
    if(!good) {
      return new Range2D(Double.NaN, Double.NaN);
    } else {
      for(i=first; i < num; i++) {
        if(!Double.isNaN(values[i])) {
          amin = Math.min(amin, (double)values[i]);
          amax = Math.max(amax, (double)values[i]);
        }
      }
    }
    return new Range2D(amin, amax);
  }
  /**
   * Find the time range of the <code>SGTLine</code> object.
   *
   * @param data SGTLine object
   * @see CartesianGraph
   */
  public TimeRange findTimeRange(SGTLine data) {
    long taxis[];
    int num;

    taxis = data.getGeoDateArray().getTime();
    num = taxis.length;
    return new TimeRange(taxis[0], taxis[num-1]);
  }
  /**
   * Find the <code>TimeRange</code> of the <code>SGTGrid</code> object.
   *
   * @param data the data grid
   * @param attr the grid attribute
   * @param dir the direction
   */
  public TimeRange findTimeRange(SGTGrid data, GridAttribute attr) {
    long tmin, tmax;
    long taxis[], orig[];
    int num, onum, i;

    if(attr.isRaster() && (data.isXTime() || data.isYTime())) {
      if(data.hasXEdges() || data.hasYEdges()) {
        taxis = data.getGeoDateArrayEdges().getTime();
      } else {
        orig = data.getGeoDateArray().getTime();
        onum = orig.length;
        taxis = new long[onum+1];
        taxis[0] = orig[0] - (orig[1]-orig[0])/2;
        for(i=1; i < onum; i++) {
          taxis[i] = (orig[i-1] + orig[i])/2;
        }
        taxis[onum] = orig[onum-1] + (orig[onum-1] - orig[onum-2])/2;
      }
    } else {
      taxis = data.getGeoDateArray().getTime();
    }
    num = taxis.length;
    return new TimeRange(taxis[0], taxis[num-1]);
  }
  /**
   * Set clipping on or off. If clipping is on, clip to the
   * axes range.
   *
   * @param clip true if clipping is on
   */
  public void setClipping(boolean clip) {
    clipping_ = clip;
  }
  /**
   * Returns true if clipping is on.
   *
   * @return true if clipping is on
   */
  public boolean isClipping() {
    return clipping_;
  }
  /**
   * Set the axes to the range of the <code>SGTData</code> objects.
   */
  abstract public void resetZoom();
  /**
   * Set the axes to to range specified by the <code>Domain</code>
   * object.
   */
  abstract public void setRange(Domain domain) throws PropertyVetoException;
  /**
   * Get the current <code>Domain</code>
   */
  public Domain getRange() {
    Domain domain = new Domain();
    Range2D xr = new Range2D();
    Range2D yr = new Range2D();
//    TimeRange tr = new TimeRange();
    Layer layer = getFirstLayer();
    Graph graph = layer.getGraph();
    if(graph instanceof CartesianGraph) {
      CartesianGraph cg = (CartesianGraph)graph;
      AxisTransform xt = cg.getXTransform();
      AxisTransform yt = cg.getYTransform();
      if(xt.isTime()) {
        domain.setXRange(xt.getTimeRangeU());
      } else {
        domain.setXRange(xt.getRangeU());
      }
      if(yt.isTime()) {
        domain.setYRange(yt.getTimeRangeU());
      } else {
        domain.setYRange(yt.getRangeU());
      }
    }
    return domain;
  }
  /**
   * Get the zoom bounds in user units
   */
  public Domain getZoomBoundsU() {
    Domain domain = new Domain();
    Range2D xr = new Range2D();
    Range2D yr = new Range2D();
    long start, end;

    Rectangle zoom = getZoomBounds();
    Layer layer = getFirstLayer();
    Graph graph = layer.getGraph();

    if(graph instanceof CartesianGraph) {
      CartesianGraph cg = (CartesianGraph)graph;
      AxisTransform xt = cg.getXTransform();
      AxisTransform yt = cg.getYTransform();
      if(xt.isSpace()) {
        xr.start = cg.getXPtoU(layer.getXDtoP(zoom.x));
        xr.end = cg.getXPtoU(layer.getXDtoP(zoom.x + zoom.width));
        if(xr.start > xr.end) {
          double temp = xr.start;
          xr.start = xr.end;
          xr.end = temp;
        }
        domain.setXRange(xr);
      } else {
        start = cg.getXPtoLongTime(layer.getXDtoP(zoom.x));
        end = cg.getXPtoLongTime(layer.getXDtoP(zoom.x + zoom.width));
        if(start > end) {
          long tmp = start;
          start = end;
          end = tmp;
        }
        domain.setXRange(new TimeRange(new GeoDate(start), new GeoDate(end)));
      }
      if(yt.isSpace()) {
        yr.start = cg.getYPtoU(layer.getYDtoP(zoom.y));
        yr.end = cg.getYPtoU(layer.getYDtoP(zoom.y + zoom.height));
        if(yr.start > yr.end) {
          double temp = yr.start;
          yr.start = yr.end;
          yr.end = temp;
        }
        domain.setYRange(yr);
      } else {
        start = cg.getYPtoLongTime(layer.getYDtoP(zoom.y));
        end = cg.getYPtoLongTime(layer.getYDtoP(zoom.y + zoom.height));
        if(start > end) {
          long tmp = start;
          start = end;
          end = tmp;
        }
        domain.setYRange(new TimeRange(new GeoDate(start), new GeoDate(end)));
      }
    }
    return domain;
  }
  /**
   * return a formated string summarizing the latitude
   */
  protected String getLatString(double lat) {
    if(lat == 0.0) return "Eq";
    DecimalFormat dfLat = new DecimalFormat("##.##N;##.##S");
    return dfLat.format(lat);
  }
  /**
   * return a formated string summarizing the longitude
   */
  protected String getLonString(double lond) {
    /* positive west */
    String str;
    DecimalFormat dfLon = new DecimalFormat("###.##W;###.##");
    double lon = (lond + 360.0) % 360.0;
    dfLon.setNegativeSuffix("E");
    if(lon > 180.0) {
      lon = 360.0 - lon;
    }
    return dfLon.format(lon);
  }
  /**
   * Return data associated with the plot.
   *
   * @return data in a <code>Collection</code>
   */
  public Collection getData() {
    Collection col = new Collection(ident_ + " Data Collection",
                                    data_.size());
    for(Enumeration e = data_.elements(); e.hasMoreElements(); ) {
      col.addElement(e.nextElement());
    }
    return col;
  }

  class SymMouse extends java.awt.event.MouseAdapter  {
    public void mousePressed(java.awt.event.MouseEvent event) {
      if(!isMouseEventsEnabled()) return;
      Object object = event.getSource();
      if (object == me_)
        Layout_MousePress(event);
      else if (object == keyPane_)
        KeyPane_MousePress(event);
    }

    public void mouseClicked(java.awt.event.MouseEvent event) {
      if(!isMouseEventsEnabled()) return;
      Object object = event.getSource();
      if (object == me_)
        Layout_MouseClicked(event);
      else if (object == keyPane_)
        KeyPane_MouseClicked(event);
    }

    public void mouseReleased(java.awt.event.MouseEvent event) {
      if(!isMouseEventsEnabled()) return;
      Object object = event.getSource();
      if (object == me_)
        Layout_MouseRelease(event);
    }
  }

  void Layout_MouseRelease(java.awt.event.MouseEvent event) {
    //
    // continue only if button1 is pressed
    //
    if((event.getModifiers()&InputEvent.BUTTON1_MASK) == 0) return;

    Rectangle zm = getZoomBounds();
    if(zm.width <= 1 || zm.height <= 1) return;

    Domain zoom = getZoomBoundsU();

    setClipping(true);

    if(getFirstLayer().getGraph() instanceof CartesianGraph) {
      try {
        setRange(zoom);
      } catch (PropertyVetoException e) {
        System.out.println("Zoom denied! " + e);
      }
    }
    //    draw();
  }

  void Layout_MousePress(java.awt.event.MouseEvent event) {
    if(event.isControlDown()) {
      resetZoom();
      setClipping(false);
      //      draw();
    }
  }

  void Layout_MouseClicked(java.awt.event.MouseEvent event) {
    if(event.isControlDown()) return;
    Object obj = getSelectedObject();
    if(obj instanceof LineCartesianRenderer) {
      LineCartesianRenderer line = (LineCartesianRenderer)obj;
      LineAttribute attr = line.getLineAttribute();
      if(attr.getStyle() == LineAttribute.SOLID) {
        attr.setStyle(LineAttribute.HIGHLIGHT);
      } else if(attr.getStyle() == LineAttribute.HIGHLIGHT) {
        attr.setStyle(LineAttribute.SOLID);
      }
      //      draw();
    }

    if(((event.getModifiers()&InputEvent.BUTTON3_MASK) != 0) &&
       editClasses_) {
      showProperties(obj);
    }
  }

  void KeyPane_MousePress(java.awt.event.MouseEvent event) {
  }

  void KeyPane_MouseClicked(java.awt.event.MouseEvent event) {
    if(keyPane_ == null) return;
    Object obj = keyPane_.getSelectedObject();
    if(obj instanceof LineCartesianRenderer) {
      LineCartesianRenderer line = (LineCartesianRenderer)obj;
      LineAttribute attr = line.getLineAttribute();
      if(attr.getStyle() == LineAttribute.SOLID) {
        attr.setStyle(LineAttribute.HIGHLIGHT);
      } else if(attr.getStyle() == LineAttribute.HIGHLIGHT) {
        attr.setStyle(LineAttribute.SOLID);
      }
      //      draw();
      //      keyPane_.draw();
    }
    if(editClasses_) {
      showProperties(obj);
    }
  }
  /**
   * Enable <code>sgt</code> object property editing
   */
  public void setEditClasses(boolean b) {
    editClasses_ = b;
  }
  /**
   * Are <code>sgt</code> objects editable?
   */
  public boolean isEditClasses() {
    return editClasses_;
  }
  /**
   * Determine if the object selected is an SGLabel, PlainAxis, or
   * TimeAxis. Depending on the answer create the appropriate dialog.
   */
  void showProperties(Object obj) {
    if(obj instanceof SGLabel) {
      if(sg_props_ == (SGLabelDialog) null) {
        //
        // create the SGLabelDialog
        //
        sg_props_ = new SGLabelDialog();
      }
      sg_props_.setSGLabel((SGLabel) obj, this);
      if(!sg_props_.isShowing())
        sg_props_.show();
    } else if(obj instanceof PlainAxis) {
      if(pa_props_ == (SpaceAxisDialog) null) {
        //
        // create the PlainAxis dialog
        //
        pa_props_ = new SpaceAxisDialog();
      }
      pa_props_.setSpaceAxis((PlainAxis) obj, this);
      if(!pa_props_.isShowing())
        pa_props_.show();
    } else if(obj instanceof TimeAxis) {
      if(ta_props_ == (TimeAxisDialog) null) {
        //
        // create the TimeAxis Dialog
        //
        ta_props_ = new TimeAxisDialog();
      }
      ta_props_.setTimeAxis((TimeAxis) obj, this);
      if(!ta_props_.isShowing())
        ta_props_.show();
    } else if (obj instanceof Logo) {
      if(lo_props_ == (LogoDialog) null) {
        //
        // create the LogoProperties dialog
        //
        lo_props_ = new LogoDialog();
      }
      lo_props_.setLogo((Logo) obj, this);
      if(!lo_props_.isShowing())
        lo_props_.show();
    }
  }

  private Frame getFrame() {
    Container theFrame = this;
    do {
      theFrame = theFrame.getParent();
    } while ((theFrame != null) && !(theFrame instanceof Frame));
    if (theFrame == null)
      theFrame = new Frame();
    return (Frame) theFrame;
  }
  /**
   * Set the clip range for all <code>Layer</code>s.
   */
  protected void setAllClip(SoTRange xr, SoTRange yr) {
    if(yr.isTime()) {
      setAllClip(yr.getStart().getLongTime(),
                 yr.getEnd().getLongTime(),
                 ((SoTRange.Double)xr).start,
                 ((SoTRange.Double)xr).end);
    } else {
      if(xr.isTime()) {
        setAllClip(xr.getStart().getLongTime(),
                   xr.getEnd().getLongTime(),
                   ((SoTRange.Double)yr).start,
                   ((SoTRange.Double)yr).end);
      } else {
        setAllClip(((SoTRange.Double)xr).start,
                   ((SoTRange.Double)xr).end,
                   ((SoTRange.Double)yr).start,
                   ((SoTRange.Double)yr).end);
      }
    }
  }

  /**
   * Set the clip range for all <code>Layer</code>s.
   */
  protected void setAllClip(double xmin, double xmax, double ymin, double ymax) {
    Layer ly;
    Component[] comps = getComponents();
    for(int i=0; i < comps.length; i++) {
      if(comps[i] instanceof Layer) {
        ly = (Layer)comps[i];
        ((CartesianGraph)ly.getGraph()).setClip(xmin, xmax, ymin, ymax);
      }
    }
  }
  /**
   * Set the clip range for all <code>Layer</code>s.
   */
  protected void setAllClip(GeoDate tmin, GeoDate tmax, double min, double max) {
//    System.out.println("setAllClip(" + tmin + ", " + tmax + ", " + min + ", " + max + ")");
    setAllClip(tmin.getTime(), tmax.getTime(), min, max);
/*    Layer ly;
    Component[] comps = getComponents();
    for(int i=0; i < comps.length; i++) {
      if(comps[i] instanceof Layer) {
        ly = (Layer)comps[i];
        ((CartesianGraph)ly.getGraph()).setClip(tmin, tmax, min, max);
      }
    } */
  }
  /**
   * @since 3.0
   */
  protected void setAllClip(long tmin, long tmax, double min, double max) {
//    System.out.println("setAllClip(" + tmin + ", " + tmax + ", " + min + ", " + max + ")");
    Layer ly;
    Component[] comps = getComponents();
    for(int i=0; i < comps.length; i++) {
      ly = (Layer)comps[i];
      ((CartesianGraph)ly.getGraph()).setClip(tmin, tmax, min, max);
    }
  }
  /**
   * Turn on clipping for all <code>Layer</code>s.
   */
  protected void setAllClipping(boolean clip) {
    Layer ly;
    Component[] comps = getComponents();
    for(int i=0; i < comps.length; i++) {
      if(comps[i] instanceof Layer) {
        ly = (Layer)comps[i];
        ((CartesianGraph)ly.getGraph()).setClipping(clip);
      }
    }
  }
  /**
   * Get the bounds for the line or color key.
   */
  public abstract Rectangle2D.Double getKeyBoundsP();
  /**
   * Set the bounds for the line or color key.
   */
  public abstract void setKeyBoundsP(Rectangle2D.Double r);
  /**
   * Get the size of the key layer in physical coordinates.
   */
  public Dimension2D getKeyLayerSizeP() {
    if(keyPane_ != null) {
      return keyPane_.getFirstLayer().getSizeP();
    }
    return null;
  }
  /**
   * Set the size of the key layer in physical coordinates.
   */
  public void setKeyLayerSizeP(Dimension2D d) {
    if(keyPane_ != null) {
      keyPane_.getFirstLayer().setSizeP(d);
    }
  }
  public void addVetoableChangeListener(VetoableChangeListener l) {
    vetos_.addVetoableChangeListener(l);
  }
  public void removeVetoableChangeListener(VetoableChangeListener l) {
    vetos_.removeVetoableChangeListener(l);
  }
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
