/*
 * $Id: DataGroup.java,v 1.2 2003/08/22 23:02:33 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.beans;

import gov.noaa.pmel.util.SoTRange;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.event.*;

/**
 * A holder for the X and Y transforms and optionally references to the X and Y axes for a <code>
 * CartesianGraph</code>. This class is used with <code>DataModel</code> and <code>Panel</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:33 $
 * @since 3.0
 */
public class DataGroup implements Serializable, ChangeListener {
  // axis position
  /** X axis placed at top of <code>DataGroup</code> area. */
  public static final int TOP = 0;

  /** X axis placed at bottom of <code>DataGroup</code> area. */
  public static final int BOTTOM = 1;

  /** Y axis placed left of <code>DataGroup</code> area. */
  public static final int LEFT = 2;

  /** Y axis placed right of <code>DataGroup</code> area. */
  public static final int RIGHT = 3;

  /** Axes placed in default locations. */
  public static final int MANUAL = 4;

  // axis transform / axis type
  /** Linear transform type. */
  public static final int LINEAR = 0;

  /** Log transform type. */
  public static final int LOG = 1;

  /** Refer to a transform in another <code>DataGroup</code>. */
  public static final int REFERENCE = 2;

  /** Time axis type. */
  public static final int TIME = 3;

  /** Plain linear axis type. */
  public static final int PLAIN = 4;

  //
  /** X direction. */
  public static final int X_DIR = 0;

  /** Y direction. */
  public static final int Y_DIR = 1;

  //
  private String id = "";

  //
  /**
   * @label margin
   */
  private Margin margin = new Margin(0.25f, 0.5f, 0.5f, 0.25f);

  private boolean zoomable = true;

  //
  /**
   * @label xAxisHolder
   * @supplierCardinality 1
   * @undirected
   * @link aggregation
   */
  private AxisHolder xAxisHolder_ = new AxisHolder(PLAIN, X_DIR, this);

  /**
   * @label yAxisHolder
   * @link aggregation
   * @undirected
   * @supplierCardinality 1
   */
  private AxisHolder yAxisHolder_ = new AxisHolder(PLAIN, Y_DIR, this);

  //
  private boolean zAutoScale = true;
  private SoTRange zRangeU = new SoTRange.Double(0.0, 1.0, 0.1);
  private int numberAutoContourLevels = 10;

  //
  /**
   * @label pHolder
   */
  private PanelHolder pHolder_ = null;

  private transient Vector changeListeners;
  private transient ChangeEvent changeEvent_ = new ChangeEvent(this);
  private transient boolean instantiated = false;

  static {
    try {
      BeanInfo info = Introspector.getBeanInfo(DataGroup.class);
      PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
      for (int i = 0; i < descriptors.length; i++) {
        PropertyDescriptor pd = descriptors[i];
        if (pd.getName().equals("instantiated")) {
          pd.setValue("transient", Boolean.TRUE);
        } else if (pd.getName().equals("panelHolder")) {
          pd.setValue("transient", Boolean.TRUE);
        }
      }
    } catch (IntrospectionException ie) {
      ie.printStackTrace();
    }
  }

  /**
   * Default constructor. <code>PanelHodler</code> is set to <code>null</code>. X and Y transforms
   * are LINEAR and axes PLAIN.
   */
  public DataGroup() {
    this("Default Id", null);
  }

  /**
   * Simple constructor. X and Y transforms are LINEAR and axes PLAIN.
   *
   * @param id data group id
   * @param ph panelholder parent
   */
  public DataGroup(String id, PanelHolder ph) {
    this(
        id, ph,
        LINEAR, PLAIN,
        LINEAR, PLAIN);
  }

  /**
   * Full constructor.
   *
   * @param id data group id
   * @param ph panelholder parent
   * @param xt x transform type
   * @param xAxis x axis type
   * @param yt y transform type
   * @param yAxis y axis type
   */
  public DataGroup(String id, PanelHolder ph, int xt, int xAxis, int yt, int yAxis) {
    this.id = id;
    pHolder_ = ph;
    if (xAxisHolder_ == null) {
      xAxisHolder_ = new AxisHolder(xAxis, X_DIR, this);
    } else {
      xAxisHolder_.setAxisType(xAxis);
      xAxisHolder_.setAxisOrientation(X_DIR);
      xAxisHolder_.setDataGroup(this);
    }
    xAxisHolder_.setTransformType(xt);
    xAxisHolder_.addChangeListener(this);
    if (yAxisHolder_ == null) {
      yAxisHolder_ = new AxisHolder(yAxis, Y_DIR, this);
    } else {
      yAxisHolder_.setAxisType(yAxis);
      yAxisHolder_.setAxisOrientation(Y_DIR);
      yAxisHolder_.setDataGroup(this);
    }
    yAxisHolder_.setTransformType(yt);
    yAxisHolder_.addChangeListener(this);
  }

  /**
   * Set panelhodler parent.
   *
   * @param ph panelholder parent
   */
  public void setPanelHolder(PanelHolder ph) {
    if (pHolder_ != null) removeChangeListener(pHolder_);
    pHolder_ = ph;
    addChangeListener(pHolder_);
  }

  /**
   * Get parent.
   *
   * @return panelholder parent
   */
  public PanelHolder getPanelHolder() {
    return pHolder_;
  }

  /**
   * Get X axisholder
   *
   * @return x axisholder
   */
  public AxisHolder getXAxisHolder() {
    return xAxisHolder_;
  }

  /**
   * Set X axisholder
   *
   * @param xah x axisholder
   */
  public void setXAxisHolder(AxisHolder xah) {
    xAxisHolder_ = xah;
    xAxisHolder_.addChangeListener(this);
  }

  /**
   * Get Y axisholder.
   *
   * @return y axisholder
   */
  public AxisHolder getYAxisHolder() {
    return yAxisHolder_;
  }

  /**
   * Set Y axisholder
   *
   * @param yah y axisholder
   */
  public void setYAxisHolder(AxisHolder yah) {
    yAxisHolder_ = yah;
    yAxisHolder_.addChangeListener(this);
  }

  /**
   * Get datagroup id.
   *
   * @return datagroup id
   */
  public String getId() {
    return id;
  }

  /**
   * Set datagroup id.
   *
   * @param id datagroup id
   */
  public void setId(String id) {
    String saved = this.id;
    this.id = id;
    if (!saved.equals(this.id)) fireStateChanged();
  }

  /**
   * Get the margin.
   *
   * @return margin
   */
  public Margin getMargin() {
    return (Margin) margin.copy();
  }

  /**
   * Set the margin. The margin is used to automatically place the axes in a <code>Panel</code>. The
   * margin is the distance from each edge of the <code>Panel</code> to the <code>DataGroup</code>.
   * Default is (0.25f, 0.5f, 0.5f, 0.25f)
   *
   * @param margin margin
   */
  public void setMargin(Margin margin) {
    Margin saved = this.margin;
    this.margin = margin;
    if (!saved.equals(this.margin)) fireStateChanged();
  }

  /**
   * Set the Margin. Default is (0.25f, 0.5f, 0.5f, 0.25f)
   *
   * @param top top margin
   * @param left left margin
   * @param bottom bottom margin
   * @param right right margin
   */
  public void setMargin(float top, float left, float bottom, float right) {
    Margin saved = margin;
    margin = new Margin(top, left, bottom, right);
    if (!saved.equals(margin)) fireStateChanged();
  }

  /** Remove all <code>ChangeListener</code>s. */
  public void removeAllChangeListeners() {
    changeListeners = null;
  }

  /**
   * Set auto Z scale. Auto Z scale is only effective for grids. Default = true.
   *
   * @param zAutoScale autoscale if true
   */
  public void setZAutoScale(boolean zAutoScale) {
    boolean saved = this.zAutoScale;
    this.zAutoScale = zAutoScale;
    if (saved != zAutoScale) fireStateChanged();
  }

  /**
   * Is auto Z scale?
   *
   * @return true, if auto z scale
   */
  public boolean isZAutoScale() {
    return zAutoScale;
  }

  /**
   * Set Z range, user units. Only used if Auto Z Scale is false. Default = (0, 1, 0.1).
   *
   * @param zRange Z range
   */
  public void setZRangeU(SoTRange zRange) {
    zRangeU = zRange;
  }

  /**
   * Get Z range.
   *
   * @return Z range
   */
  public SoTRange getZRangeU() {
    return zRangeU;
  }

  /**
   * Set datagroup zoomable. If true, datagroup can be zoomed using the mouse. Default = true.
   *
   * @param zoomable datagroup zoomable
   */
  public void setZoomable(boolean zoomable) {
    boolean saved = this.zoomable;
    this.zoomable = zoomable;
    if (saved != this.zoomable) fireStateChanged();
  }

  /**
   * Is datagroup zoomable?
   *
   * @return true, if datagroup zoomable
   */
  public boolean isZoomable() {
    return zoomable;
  }

  /**
   * Remove changelistener.
   *
   * @param l changelistener
   */
  public synchronized void removeChangeListener(ChangeListener l) {
    if (changeListeners != null && changeListeners.contains(l)) {
      Vector v = (Vector) changeListeners.clone();
      v.removeElement(l);
      changeListeners = v;
    }
  }

  /**
   * Add changelistener
   *
   * @param l changelistener
   */
  public synchronized void addChangeListener(ChangeListener l) {
    Vector v = changeListeners == null ? new Vector(2) : (Vector) changeListeners.clone();
    if (!v.contains(l)) {
      v.addElement(l);
      changeListeners = v;
    }
  }

  /**
   * Remove all <code>ChangeListener</code>s that implement the <code>DesignListener</code>
   * interface.
   *
   * @see DesignListener
   */
  public synchronized void removeDesignChangeListeners() {
    if (changeListeners != null) {
      Vector v = (Vector) changeListeners.clone();
      Iterator iter = v.iterator();
      while (iter.hasNext()) {
        Object obj = iter.next();
        if (obj instanceof DesignListener) changeListeners.removeElement(obj);
      }
    }
  }

  protected void fireStateChanged() {
    if (changeListeners != null) {
      Vector listeners = changeListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((ChangeListener) listeners.elementAt(i)).stateChanged(changeEvent_);
      }
    }
  }

  /**
   * Set instantiated. Once associated <code>DataGroupLayer</code> object has been created this
   * property is set true. Used internally.
   *
   * @param instantiated true if instantiated
   */
  public void setInstantiated(boolean instantiated) {
    this.instantiated = instantiated;
  }

  /**
   * Is datagrouplayer instantiated?
   *
   * @return true, if datagrouplayer instantiated
   */
  public boolean isInstantiated() {
    return instantiated;
  }

  /**
   * <code>ChangeListner</code> callback.
   *
   * @param e ChangeEvent
   */
  @Override
  public void stateChanged(ChangeEvent e) {
    fireStateChanged();
  }

  /**
   * Get number of auto contour levels. Valid for grid type data only.
   *
   * @return number of auto contour levels
   */
  public int getNumberAutoContourLevels() {
    return numberAutoContourLevels;
  }

  /**
   * Set number of auto contour levels. Valid for grid type data only.
   *
   * @param numberAutoContourLevels number of contour levels
   */
  public void setNumberAutoContourLevels(int numberAutoContourLevels) {
    int saved = this.numberAutoContourLevels;
    this.numberAutoContourLevels = numberAutoContourLevels;
    if (saved != this.numberAutoContourLevels) fireStateChanged();
  }
}
