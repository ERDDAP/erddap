/*
 * $Id: AxisHolder.java,v 1.3 2003/09/18 21:01:14 dwd Exp $
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

import gov.noaa.pmel.sgt.*;
import gov.noaa.pmel.util.*;
import java.awt.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.event.*;

/**
 * Contains the data necessary to instantiate an axis. This class is used with <code>DataGroup
 * </code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/09/18 21:01:14 $
 * @since 3.0
 */
public class AxisHolder implements Serializable {
  private Vector changeListeners;

  /**
   * @label dataGroup
   */
  private DataGroup dataGroup_;

  private transient ChangeEvent changeEvent_ = new ChangeEvent(this);

  // general axis properties
  /** axisType = DataGroup.PLAIN, DataGroup.TIME, or DataGroup.LOG */
  private int axisType = -1; // must be defined

  /** axisOrientation = DataGroup.X_DIR or DataGroup.Y_DIR */
  private int axisOrientation = -1; // must be defined

  /** transformType = DataGroup.LINEAR, DataGroup.LOG, DataGroup.REFERENCE */
  private int transformType = DataGroup.LINEAR;

  private String transformGroup = "";

  /**
   *
   *
   * <pre>
   * axisPosition = DataGroup.TOP, DataGroup.BOTTOM, (for x axes)
   *                DataGroup.LEFT, DataGroup.RIGHT, (for y axes)
   *                DataGroup.MANUAL
   * </pre>
   */
  private int axisPosition = -1;

  /** used if axisPosition = MANUAL, always computed */
  private Point2D.Double axisOriginP = new Point2D.Double(0.0, 0.0);

  private boolean locationAtOrigin = false;
  private Color axisColor = Color.black;
  private boolean autoRange = true;

  /** used if axisPosition = MANUAL, always computed */
  private Rectangle2D boundsP = new Rectangle2D.Double(0.0, 0.0, 0.0, 0.0);

  //  private double originP = 0.0;  //  y origin for X_DIR, x origin for Y_DIR if axisPosition =
  // MANUAL
  private SoTRange userRange = new SoTRange.Double(1.0, 10.0, 1.0);
  private Color labelColor = Color.black;
  private Font labelFont = new Font("Helvetica", Font.ITALIC, 10);
  private double labelHeightP = 0.15;

  /**
   *
   *
   * <pre>
   * labelPosition = Axis.AUTO, Axis.POSITIVE_SIDE,
   *                 Axis.NEGATIVE_SIDE, Axis.NO_LABEL
   * </pre>
   */
  private int labelPosition = Axis.AUTO; // Expert

  private double largeTicHeightP = 0.1; // Expert
  private double smallTicHeightP = 0.05; // Expert
  private int numSmallTics = 0;
  private double thickTicWidth = 0.025; // Expert

  /**
   *
   *
   * <pre>
   * ticPosition = Axis.AUTO, Axis.POSITIVE_SIDE,
   *               Axis.NEGATIVE_SIDE, Axis.BOTH_SIDES
   * </pre>
   */
  private int ticPosition = Axis.AUTO; // Expert

  private SGLabel title =
      new SGLabel("title", "", 0.20, new Point2D.Double(0.0, 0.0), SGLabel.BOTTOM, SGLabel.LEFT);
  private boolean titleAuto = true;
  private boolean selectable = true;
  private boolean visible = true;
  // SpaceAxis properties
  private String labelFormat = ""; // Expert
  private int labelInterval = 2;
  private int labelSignificantDigits = 2;
  // TimeAxis properties defaults appropriate for MONTH_YEAR
  private String minorFormat = "MMM"; // Expert
  private String majorFormat = "yyyy"; // Expert
  private int minorInterval = 2; // Expert
  private int majorInterval = 1; // Expert

  /**
   *
   *
   * <pre>
   * timeAxisStyle = TimeAxis.AUTO, TimeAxis.DAY_MONTH, TimeAxis.HOUR_DAY,
   *                 TimeAxis.MINUTE_HOUR, TimeAxis.MONTH_YEAR,
   *                 TimeAxis.YEAR_DECADE
   * </pre>
   */
  private int timeAxisStyle = TimeAxis.AUTO;

  private boolean suppressEvent_ = false;

  /** Static code to make sure that dataGroup doesn't get serialized by XMLEncode */
  static {
    try {
      BeanInfo info = Introspector.getBeanInfo(AxisHolder.class);
      PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
      for (int i = 0; i < descriptors.length; i++) {
        PropertyDescriptor pd = descriptors[i];
        if (pd.getName().equals("dataGroup")) {
          pd.setValue("transient", Boolean.TRUE);
        }
      }
    } catch (IntrospectionException ie) {
      ie.printStackTrace();
    }
  }

  /**
   * Default constructor. The methods setAxisType, setAxisOrientation, and setDataGroup must be
   * called.
   */
  public AxisHolder() {
    title.setColor(Color.black);
  }

  /**
   * Construct a new <code>AxisHolder</code>. This constructor includes the necessary fields.
   *
   * @param type Type of axis
   * @param dir Direction of axis
   * @param dataGroup DataGroup parent
   * @see #setAxisType setAxisType
   * @see #setAxisOrientation setAxisOrientation
   * @see #setDataGroup setDataGroup
   */
  public AxisHolder(int type, int dir, DataGroup dataGroup) {
    axisType = type;
    axisOrientation = dir;
    dataGroup_ = dataGroup;
    if (axisOrientation == DataGroup.X_DIR) {
      title.setText("X Axis");
      axisPosition = DataGroup.BOTTOM;
    } else {
      title.setText("Y Axis");
      axisPosition = DataGroup.LEFT;
    }
    transformGroup = dataGroup.getId();
    title.setColor(Color.black);
  }

  /**
   * Get the <code>DataGroup</code> parent.
   *
   * @return DataGroup parent
   */
  public DataGroup getDataGroup() {
    return dataGroup_;
  }

  /**
   * Set the parent <code>DataGroup</code>. No default.
   *
   * @param dataGroup Parent object to AxisHolder
   */
  public void setDataGroup(DataGroup dataGroup) {
    dataGroup_ = dataGroup;
    transformGroup = dataGroup_.getId();
  }

  /**
   * Test if the axis time.
   *
   * @return True if axis is time.
   */
  public boolean isTime() {
    return axisType == DataGroup.TIME;
  }

  /**
   * Remove a <code>ChangeListener</code>.
   *
   * @param l ChangeListener to remove
   */
  public synchronized void removeChangeListener(ChangeListener l) {
    if (changeListeners != null && changeListeners.contains(l)) {
      Vector v = (Vector) changeListeners.clone();
      v.removeElement(l);
      changeListeners = v;
    }
  }

  /**
   * Add a <code>ChangeListener</code>. Listener will be notified if a change occurs.
   *
   * @param l ChangeListener to add.
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

  /** Remove all <code>ChangeListener</code>s. */
  public void removeAllChangeListeners() {
    changeListeners.removeAllElements();
  }

  void fireStateChanged() {
    if (suppressEvent_) return;
    if (changeListeners != null) {
      Vector listeners = changeListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((ChangeListener) listeners.elementAt(i)).stateChanged(changeEvent_);
      }
    }
  }

  // general axis properties

  /**
   * Set the axis type. The possible types include:
   *
   * <pre>
   * axisType = DataGroup.PLAIN, DataGroup.TIME, or DataGroup.LOG
   * </pre>
   *
   * No default.
   *
   * @param axisType (see above)
   */
  public void setAxisType(int axisType) {
    int saved = this.axisType;
    this.axisType = axisType;
    if (saved != this.axisType) fireStateChanged();
  }

  /**
   * Get the axis type.
   *
   * @return axis type
   */
  public int getAxisType() {
    return axisType;
  }

  /**
   * Get the axis orientation.
   *
   * @return axis orientation.
   */
  public int getAxisOrientation() {
    return axisOrientation;
  }

  /**
   * Set the axis orientation. Orientations are:
   *
   * <pre>
   * axisOrientation = DataGroup.X_DIR or DataGroup.Y_DIR
   * </pre>
   *
   * No default.
   *
   * @param dir axis orientation
   */
  public void setAxisOrientation(int dir) {
    axisOrientation = dir;
    if (axisOrientation == DataGroup.X_DIR) {
      title.setText("X Axis");
      axisPosition = DataGroup.BOTTOM;
    } else {
      title.setText("Y Axis");
      axisPosition = DataGroup.LEFT;
    }
  }

  /**
   * Set axis color. Default = black.
   *
   * @param axisColor axis color
   */
  public void setAxisColor(Color axisColor) {
    Color saved =
        new Color(
            this.axisColor.getRed(),
            this.axisColor.getGreen(),
            this.axisColor.getBlue(),
            this.axisColor.getAlpha());
    this.axisColor = axisColor;
    if (!saved.equals(this.axisColor)) fireStateChanged();
  }

  /**
   * Get the axis color.
   *
   * @return axis color
   */
  public Color getAxisColor() {
    return axisColor;
  }

  /**
   * Set autoRange property. True to automatically compute the axis range from the data. Default =
   * true.
   *
   * @param autoRange auto range
   */
  public void setAutoRange(boolean autoRange) {
    boolean saved = this.autoRange;
    this.autoRange = autoRange;
    if (saved != autoRange) fireStateChanged();
  }

  /**
   * Test if the axis in autoRange mode.
   *
   * @return True, if in autoRange mode.
   */
  public boolean isAutoRange() {
    return autoRange;
  }

  /**
   * Set the user range. User range is only used if autoRange is false. Default = (1, 10, 1)
   *
   * @param userRange user supplied range
   */
  public void setUserRange(SoTRange userRange) {
    SoTRange saved = userRange.copy();
    this.userRange = userRange;
    if (!saved.equals(this.userRange)) fireStateChanged();
  }

  /**
   * Get the user range.
   *
   * @return user range
   */
  public SoTRange getUserRange() {
    return userRange;
  }

  /**
   * Set axis label color. Default = black.
   *
   * @param labelColor label color
   */
  public void setLabelColor(Color labelColor) {
    Color saved =
        new Color(
            this.labelColor.getRed(),
            this.labelColor.getGreen(),
            this.labelColor.getBlue(),
            this.labelColor.getAlpha());
    this.labelColor = labelColor;
    if (!saved.equals(this.labelColor)) fireStateChanged();
  }

  /**
   * Get the axis label color.
   *
   * @return axis label color
   */
  public Color getLabelColor() {
    return labelColor;
  }

  /**
   * Set the axis label font. Default = ("Helvetica", ITALIC, 10).
   *
   * @param labelFont label font
   */
  public void setLabelFont(Font labelFont) {
    Font saved =
        new Font(this.labelFont.getName(), this.labelFont.getStyle(), this.labelFont.getSize());
    this.labelFont = labelFont;
    if (!saved.equals(this.labelFont)) fireStateChanged();
  }

  /**
   * Get the axis label font.
   *
   * @return axis label font
   */
  public Font getLabelFont() {
    return labelFont;
  }

  /**
   * Set label height in physical coordinates (inches). Default = 0.15.
   *
   * @param labelHeightP label height
   */
  public void setLabelHeightP(double labelHeightP) {
    double saved = this.labelHeightP;
    this.labelHeightP = labelHeightP;
    if (saved != this.labelHeightP) fireStateChanged();
  }

  /**
   * Get label height.
   *
   * @return label height in physical coordinates
   */
  public double getLabelHeightP() {
    return labelHeightP;
  }

  /**
   * Set the label position. Label position can be:
   *
   * <pre>
   * labelPosition = Axis.AUTO, Axis.POSITIVE_SIDE,
   *                 Axis.NEGATIVE_SIDE, Axis.NO_LABEL
   * </pre>
   *
   * Default = AUTO.
   *
   * @param labelPosition label position
   */
  public void setLabelPosition(int labelPosition) {
    int saved = this.labelPosition;
    this.labelPosition = labelPosition;
    if (saved != this.labelPosition) fireStateChanged();
  }

  /**
   * Test if label position in auto mode.
   *
   * @return true if in auto mode
   */
  public boolean isLabelPositionAuto() {
    return labelPosition == Axis.AUTO;
  }

  /**
   * If labelPosition is AUTO then return computed labelPosition, otherwise return stored value
   *
   * @return label position
   */
  public int getLabelPosition() {
    int labPos = Axis.NEGATIVE_SIDE;
    if (isLabelPositionAuto()) {
      switch (axisPosition) {
        case DataGroup.MANUAL:
        case DataGroup.BOTTOM:
        case DataGroup.LEFT:
          labPos = Axis.NEGATIVE_SIDE;
          break;
        case DataGroup.TOP:
        case DataGroup.RIGHT:
          labPos = Axis.POSITIVE_SIDE;
          break;
      }
    } else {
      labPos = labelPosition;
    }
    return labPos;
  }

  /**
   * Set bounds of axis in physical coordinates. Default = (0, 0, 0, 0).
   *
   * @param boundsP axis bounds
   */
  public void setBoundsP(Rectangle2D boundsP) {
    Rectangle2D saved = getBoundsP();
    //    if(this.boundsP != null) saved = this.boundsP.copy();
    this.boundsP = boundsP;
    boolean changed = true;
    if (saved != null) changed = !saved.equals(this.boundsP);
    if (changed) fireStateChanged();
  }

  /**
   * Get axis bounds.
   *
   * @return axis bounds in physical coordinates.
   */
  public Rectangle2D getBoundsP() {
    return boundsP;
    /**
     * @todo bounds isn't cloned should be see note
     */
    //    return boundsP == null? null: boundsP.copy();
  }

  /**
   * Get range of axis (long direction) in physical coordinates. The bounds are used to determine
   * the axis range.
   *
   * @return axis range (long direction)
   */
  public Range2D getRangeP() {
    Rectangle2D.Double dbl = (Rectangle2D.Double) boundsP;
    if (axisOrientation == DataGroup.X_DIR) {
      return new Range2D(dbl.x, dbl.x + dbl.width);
    } else {
      return new Range2D(dbl.y, dbl.y + dbl.height);
    }
  }

  /**
   * Set large tic height in physical coordinates. Default = 0.1.
   *
   * @param largeTicHeightP large tic height
   */
  public void setLargeTicHeightP(double largeTicHeightP) {
    double saved = this.largeTicHeightP;
    this.largeTicHeightP = largeTicHeightP;
    if (saved != this.largeTicHeightP) fireStateChanged();
  }

  /**
   * Get large tic height.
   *
   * @return large tic height in physical coordinates.
   */
  public double getLargeTicHeightP() {
    return largeTicHeightP;
  }

  /**
   * Set small tic height in physical coordinates. Default = 0.05.
   *
   * @param smallTicHeightP small tic height
   */
  public void setSmallTicHeightP(double smallTicHeightP) {
    double saved = this.smallTicHeightP;
    this.smallTicHeightP = smallTicHeightP;
    if (saved != this.smallTicHeightP) fireStateChanged();
  }

  /**
   * Get small tic height.
   *
   * @return small tic height in physical coordinates
   */
  public double getSmallTicHeightP() {
    return smallTicHeightP;
  }

  /**
   * Set the number of small tics between the large tics. This should be one less than the number of
   * intervals you want. Default = 0.
   *
   * @param numSmallTics number of small tics
   */
  public void setNumSmallTics(int numSmallTics) {
    int saved = this.numSmallTics;
    this.numSmallTics = numSmallTics;
    if (saved != this.numSmallTics) fireStateChanged();
  }

  /**
   * Get the number of small tics.
   *
   * @return number of small tics between large tics
   */
  public int getNumSmallTics() {
    return numSmallTics;
  }

  /**
   * Set the thick tic width (for Time axes). Default = 0.025.
   *
   * @param thickTicWidth thick tic width
   */
  public void setThickTicWidth(double thickTicWidth) {
    double saved = this.thickTicWidth;
    this.thickTicWidth = thickTicWidth;
    if (saved != this.thickTicWidth) fireStateChanged();
  }

  /**
   * Get the thick tic width. Valid for Time axes
   *
   * @return thick tic width
   */
  public double getThickTicWidth() {
    return thickTicWidth;
  }

  /**
   * Set the tic position. Tic position include:
   *
   * <pre>
   * ticPosition = Axis.AUTO, Axis.POSITIVE_SIDE,
   *               Axis.NEGATIVE_SIDE, Axis.BOTH_SIDES
   * </pre>
   *
   * Default = AUTO.
   *
   * @param ticPosition tic position
   */
  public void setTicPosition(int ticPosition) {
    int saved = this.ticPosition;
    this.ticPosition = ticPosition;
    if (saved != this.ticPosition) fireStateChanged();
  }

  /**
   * Test if tic position in auto mode.
   *
   * @return true if in auto mode
   */
  public boolean isTicPositionAuto() {
    return ticPosition == Axis.AUTO;
  }

  /**
   * If ticPosition is AUTO then returns computed position, otherwise returns stored value.
   *
   * @return tic position
   */
  public int getTicPosition() {
    int ticPos = Axis.NEGATIVE_SIDE;
    if (isTicPositionAuto()) {
      switch (axisPosition) {
        case DataGroup.MANUAL:
        case DataGroup.BOTTOM:
        case DataGroup.LEFT:
          ticPos = Axis.NEGATIVE_SIDE;
          break;
        case DataGroup.TOP:
        case DataGroup.RIGHT:
          ticPos = Axis.POSITIVE_SIDE;
          break;
      }
    } else {
      ticPos = ticPosition;
    }
    return ticPos;
  }

  /**
   * Set the axis title. Axis title is a <code>SGLabel</code> enabling the Color, Font, size to be
   * set.
   *
   * @param title axis title
   */
  public void setTitle(SGLabel title) {
    SGLabel saved = (SGLabel) this.title.copy();
    this.title = title;
    if (!saved.equals(this.title)) fireStateChanged();
  }

  /**
   * Get the axis title.
   *
   * @return axis title
   */
  public SGLabel getTitle() {
    return title;
  }

  /**
   * Set the selecatability of the axis. If true, axis can be selected with the mouse. Default =
   * true.
   *
   * @param selectable selectable
   */
  public void setSelectable(boolean selectable) {
    boolean saved = this.selectable;
    this.selectable = selectable;
    if (saved != this.selectable) fireStateChanged();
  }

  /**
   * Test if the axis selectable.
   *
   * @return true, if the axis can be selected
   */
  public boolean isSelectable() {
    return selectable;
  }

  /**
   * Set/unset the axis visibility. If true, the axis will be displayed. Default = true.
   *
   * @param visible visible
   */
  public void setVisible(boolean visible) {
    boolean saved = this.visible;
    this.visible = visible;
    if (saved != this.visible) fireStateChanged();
  }

  /**
   * Test if the axis visible.
   *
   * @return true, if axis is set visible
   */
  public boolean isVisible() {
    return visible;
  }

  // spaceaxis properties

  /**
   * Set the axis label format. Not used with time axes. Default = ""
   *
   * @param labelFormat axis label format
   */
  public void setLabelFormat(String labelFormat) {
    String saved = this.labelFormat;
    this.labelFormat = labelFormat;
    if (!saved.equals(this.labelFormat)) fireStateChanged();
  }

  /**
   * Get the label format.
   *
   * @return label format.
   */
  public String getLabelFormat() {
    return labelFormat;
  }

  /**
   * Set the label interval. Not used with time axes. Default = 2.
   *
   * @param labelInterval axis label interval
   */
  public void setLabelInterval(int labelInterval) {
    int saved = this.labelInterval;
    this.labelInterval = labelInterval;
    if (saved != this.labelInterval) fireStateChanged();
  }

  /**
   * Get the label interval.
   *
   * @return label interval
   */
  public int getLabelInterval() {
    return labelInterval;
  }

  /**
   * Set the axis label significant digits. Not used with time axes. Default = 2.
   *
   * @param labelSignificantDigits axis label significant digits
   */
  public void setLabelSignificantDigits(int labelSignificantDigits) {
    int saved = this.labelSignificantDigits;
    this.labelSignificantDigits = labelSignificantDigits;
    if (saved != this.labelSignificantDigits) fireStateChanged();
  }

  /**
   * Get axis label significant digits
   *
   * @return significant digits
   */
  public int getLabelSignificantDigits() {
    return labelSignificantDigits;
  }

  // timeaxis properties

  /**
   * Set the time axis minor label format. Default = "MMM", appropriate for MONTH_YEAR.
   *
   * @param minorFormat time axis minor format
   */
  public void setMinorFormat(String minorFormat) {
    String saved = this.minorFormat;
    this.minorFormat = minorFormat;
    if (!saved.equals(this.minorFormat)) fireStateChanged();
  }

  /**
   * Get time axis minor label format.
   *
   * @return minor label format
   */
  public String getMinorFormat() {
    return minorFormat;
  }

  /**
   * Get the time axis major label format. Default = "yyyy", appropriate for MONTH_YEAR.
   *
   * @param majorFormat time axis major format
   */
  public void setMajorFormat(String majorFormat) {
    String saved = this.majorFormat;
    this.majorFormat = majorFormat;
    if (!saved.equals(this.majorFormat)) fireStateChanged();
  }

  /**
   * Get time axis major label format
   *
   * @return major label format
   */
  public String getMajorFormat() {
    return majorFormat;
  }

  /**
   * Set time axis minor label interval. Default = 2.
   *
   * @param minorInterval time axis minor interval
   */
  public void setMinorInterval(int minorInterval) {
    int saved = this.minorInterval;
    this.minorInterval = minorInterval;
    if (saved != this.minorInterval) fireStateChanged();
  }

  /**
   * Get time axis minor label interval
   *
   * @return minor label interval
   */
  public int getMinorInterval() {
    return minorInterval;
  }

  /**
   * Set time axis major label interval. Default = 1.
   *
   * @param majorInterval time axis major interval
   */
  public void setMajorInterval(int majorInterval) {
    int saved = this.majorInterval;
    this.majorInterval = majorInterval;
    if (saved != this.majorInterval) fireStateChanged();
  }

  /**
   * Get time axis major label interval
   *
   * @return major label interval
   */
  public int getMajorInterval() {
    return majorInterval;
  }

  /**
   * Set the time axis style. Styles include:
   *
   * <pre>
   * timeAxisStyle = TimeAxis.AUTO, TimeAxis.DAY_MONTH, TimeAxis.HOUR_DAY,
   *                 TimeAxis.MINUTE_HOUR, TimeAxis.MONTH_YEAR,
   *                 TimeAxis.YEAR_DECADE
   * </pre>
   *
   * Default = AUTO.
   *
   * @param timeAxisStyle time axis style
   */
  public void setTimeAxisStyle(int timeAxisStyle) {
    int saved = this.timeAxisStyle;
    this.timeAxisStyle = timeAxisStyle;
    if (saved != this.timeAxisStyle) fireStateChanged();
  }

  /**
   * Get the time axis style.
   *
   * @return time axis style
   */
  public int getTimeAxisStyle() {
    return timeAxisStyle;
  }

  /**
   * Set the axis transform type. Transform types include:
   *
   * <pre>
   * transformType = DataGroup.LINEAR, DataGroup.LOG, DataGroup.REFERENCE
   * </pre>
   *
   * Default = LINEAR.
   *
   * @param transformType axis transform type
   */
  public void setTransformType(int transformType) {
    int saved = this.transformType;
    this.transformType = transformType;
    if (saved != this.transformType) fireStateChanged();
  }

  /**
   * Get the axis transform type.
   *
   * @return transform type
   */
  public int getTransformType() {
    return transformType;
  }

  /**
   * Set the transform group. The transform group is used when the transformType =
   * DataGroup.REFERENCE. The transformGroup is the <code>DataGroup</code> id containing the
   * transform to be referenced. No default.
   *
   * @param transformGroup axis transform group name
   */
  public void setTransformGroup(String transformGroup) {
    String saved = this.transformGroup;
    this.transformGroup = transformGroup;
    if (saved == null || !saved.equals(this.transformGroup)) fireStateChanged();
  }

  /**
   * Get transform group name.
   *
   * @return transform group
   */
  public String getTransformGroup() {
    return transformGroup;
  }

  /**
   * Set the axis position. Axis positions include:
   *
   * <pre>
   * axisPosition = DataGroup.TOP, DataGroup.BOTTOM, (for x axes)
   *                DataGroup.LEFT, DataGroup.RIGHT, (for y axes)
   *                DataGroup.MANUAL
   * </pre>
   *
   * No default.
   *
   * @param axisPosition axis position
   */
  public void setAxisPosition(int axisPosition) {
    int saved = this.axisPosition;
    this.axisPosition = axisPosition;
    if (saved != this.axisPosition) fireStateChanged();
  }

  /**
   * Get the axis position
   *
   * @return axis position
   */
  public int getAxisPosition() {
    return axisPosition;
  }

  /**
   * Set the axis origin in physical coordinates. This is used when axisPosition = MANUAL. Default =
   * (0, 0).
   *
   * @param axisOriginP axis origin
   */
  public void setAxisOriginP(Point2D.Double axisOriginP) {
    Point2D saved = getAxisOriginP();
    //    if(this.axisOriginP != null) saved = this.axisOriginP.copy();
    this.axisOriginP = axisOriginP;
    boolean changed = true;
    if (saved != null) changed = !saved.equals(this.axisOriginP);
    //    if(Page.DEBUG) System.out.println("AxisHolder.setAxisOriginP: " + changed +
    //                                      ", " + saved + ", " + this.axisOriginP);
    if (changed) fireStateChanged();
  }

  /**
   * Get axis origin.
   *
   * @return axis origin
   */
  public Point2D.Double getAxisOriginP() {
    return axisOriginP == null ? null : (Point2D.Double) axisOriginP.copy();
  }

  /**
   * Set axis at origin of perpendicular axis. Not implemented. Default = false.
   *
   * @param locationAtOrigin set location at origin
   */
  public void setLocationAtOrigin(boolean locationAtOrigin) {
    boolean saved = this.locationAtOrigin;
    this.locationAtOrigin = locationAtOrigin;
    if (saved != this.locationAtOrigin) fireStateChanged();
  }

  /**
   * Test if the axis at the origin. Not presently implemented.
   *
   * @return true, if axis will be at origin
   */
  public boolean isLocationAtOrigin() {
    return locationAtOrigin;
  }

  /**
   * Set the title auto property. Set true to determine the title from the data. Default = true.
   *
   * @param titleAuto auto title property
   */
  public void setTitleAuto(boolean titleAuto) {
    boolean saved = this.titleAuto;
    this.titleAuto = titleAuto;
    if (saved != this.titleAuto) fireStateChanged();
  }

  /**
   * Test if the title in auto mode.
   *
   * @return true, if title is automatically generated
   */
  public boolean isTitleAuto() {
    return titleAuto;
  }
}
