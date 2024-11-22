/**
 * $Id: SimplePoint.java,v 1.14 2003/08/22 23:02:38 dwd Exp $
 *
 * <p>This software is provided by NOAA for full, free and open release. It is understood by the
 * recipient/user that NOAA assumes no liability for any errors contained in the code. Although this
 * software is released without conditions or restrictions in its use, it is expected that
 * appropriate credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an element in other product
 * development.
 */
package gov.noaa.pmel.sgt.dm;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.SoTRange;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * <code>SimplePoint</code> provides an implementation of the <code>SGTPoint</code> and <code>
 * Cartesian</code> interfaces.
 *
 * @author Donald Denbo
 * @version $Revision: 1.14 $, $Date: 2003/08/22 23:02:38 $
 * @since 1.0
 * @see SGTPoint
 * @see Cartesian
 */
public class SimplePoint implements SGTPoint, Cartesian, Cloneable, Serializable {
  protected double xloc_ = Double.NaN;
  protected double yloc_ = Double.NaN;
  protected long tloc_;
  protected boolean xTime_ = false;
  protected boolean yTime_ = false;
  protected double value_;
  protected String title_;
  protected SGLabel keyTitle_ = null;
  protected String id_ = null;

  /**
   * @shapeType AggregationLink
   * @clientRole value
   */
  protected SGTMetaData valueMetaData_;

  /**
   * @link aggregation
   * @clientRole x
   */
  protected SGTMetaData xMetaData_;

  /**
   * @link aggregation
   * @clientRole y
   */
  protected SGTMetaData yMetaData_;

  protected boolean hasValue_ = false;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      keyTitle_ = null;
      id_ = null;
      valueMetaData_ = null;
      xMetaData_ = null;
      yMetaData_ = null;
      changes_ = null;
      if (JPane.debug) String2.log("sgt.dm.SimplePoint.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public SimplePoint() {}

  /**
   * Simple Point constructor.
   *
   * @param xloc X coordinate
   * @param yloc Y coordinate
   * @param title the title
   */
  public SimplePoint(double xloc, double yloc, String title) {
    xloc_ = xloc;
    yloc_ = yloc;
    title_ = title;
  }

  /**
   * Simple Point constructor.
   *
   * @since 3.0
   * @param loc SoTPoint
   * @param title the title
   */
  public SimplePoint(SoTPoint loc, String title) {
    xTime_ = loc.isXTime();
    yTime_ = loc.isYTime();
    if (xTime_) {
      tloc_ = loc.getX().getLongTime();
    } else {
      xloc_ = ((Number) loc.getX().getObjectValue()).doubleValue();
    }
    if (yTime_) {
      tloc_ = loc.getY().getLongTime();
    } else {
      yloc_ = ((Number) loc.getY().getObjectValue()).doubleValue();
    }
    title_ = title;
  }

  /**
   * Create a copy.
   *
   * @since 2.0
   * @see SGTData
   */
  @Override
  public SGTData copy() {
    SGTPoint newPoint;
    try {
      newPoint = (SGTPoint) clone();
    } catch (CloneNotSupportedException e) {
      newPoint = new SimplePoint();
    }
    return (SGTData) newPoint;
  }

  /** Get the X coordinate. */
  @Override
  public double getX() {
    return xloc_;
  }

  /** Get the Y coordinate */
  @Override
  public double getY() {
    return yloc_;
  }

  /** Get the associated value. */
  @Override
  public double getValue() {
    return value_;
  }

  /** Is there an associated value? */
  @Override
  public boolean hasValue() {
    return hasValue_;
  }

  /** Get the time coordinate. */
  @Override
  public GeoDate getTime() {
    return new GeoDate(tloc_);
  }

  /**
   * Get the time in <code>long</code> referenced to 1970-01-01
   *
   * @since 3.0
   */
  @Override
  public long getLongTime() {
    return tloc_;
  }

  /**
   * Set the time coordinate
   *
   * @since 3.0
   */
  public void setTime(GeoDate date) {
    setTime(date.getTime());
  }

  /**
   * @since 3.0
   */
  public void setTime(long t) {
    long old = tloc_;
    tloc_ = t;
    changes_.firePropertyChange("dataModified", Long.valueOf(old), Long.valueOf(tloc_));
  }

  /** Is the X coordinate Time? */
  @Override
  public boolean isXTime() {
    return xTime_;
  }

  /** Is the Y coordinate Time? */
  @Override
  public boolean isYTime() {
    return yTime_;
  }

  /** Get the title. */
  @Override
  public String getTitle() {
    return title_;
  }

  /** Set the title. */
  public void setTitle(String title) {
    title_ = title;
  }

  @Override
  public SGLabel getKeyTitle() {
    return keyTitle_;
  }

  /** Set the title formatted for the <code>VectorKey</code>. */
  public void setKeyTitle(SGLabel title) {
    keyTitle_ = title;
  }

  /**
   * Get the unique identifier. The presence of the identifier is optional, but if it is present it
   * should be unique. This field is used to search for the layer that contains the data.
   *
   * @since 2.0
   * @return unique identifier
   * @see gov.noaa.pmel.sgt.Pane
   * @see gov.noaa.pmel.sgt.Layer
   */
  @Override
  public String getId() {
    return id_;
  }

  /** Set the unique identifier. */
  public void setId(String ident) {
    id_ = ident;
  }

  /** Get the associated value SGTMetaData. */
  @Override
  public SGTMetaData getValueMetaData() {
    return valueMetaData_;
  }

  /**
   * Set the X coordinate. <br>
   * <strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setX(double xloc) {
    double old = xloc_;
    xloc_ = xloc;
    changes_.firePropertyChange("dataModified", Double.valueOf(old), Double.valueOf(xloc_));
  }

  /**
   * Set the Y coordinate. <br>
   * <strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setY(double yloc) {
    double old = yloc_;
    yloc_ = yloc;
    changes_.firePropertyChange("dataModified", Double.valueOf(old), Double.valueOf(yloc_));
  }

  /**
   * The the associated value and basic metadata. <br>
   * <strong>Property Change:</strong> <code>associatedDataModified</code>.
   *
   * @param value associated data
   * @param name values name
   * @param units values units
   */
  public void setValue(double value, String name, String units) {
    double old = value_;
    value_ = value;
    valueMetaData_ = new SGTMetaData(name, units);
    hasValue_ = true;
    changes_.firePropertyChange(
        "associatedDataModified", Double.valueOf(old), Double.valueOf(value_));
  }

  /**
   * Set the <code>SGTMetaData</code> associated with the x coordinate
   *
   * @since 2.0
   */
  public void setXMetaData(SGTMetaData md) {
    xMetaData_ = md;
  }

  /**
   * Set the <code>SGTMetaData</code> associated with the y coordinate
   *
   * @since 2.0
   */
  public void setYMetaData(SGTMetaData md) {
    yMetaData_ = md;
  }

  @Override
  public SGTMetaData getXMetaData() {
    return xMetaData_;
  }

  @Override
  public SGTMetaData getYMetaData() {
    return yMetaData_;
  }

  @Override
  public SoTRange getXRange() {
    if (xTime_) {
      return new SoTRange.Time(tloc_, tloc_);
    } else {
      return new SoTRange.Double(xloc_, xloc_);
    }
  }

  @Override
  public SoTRange getYRange() {
    if (yTime_) {
      return new SoTRange.Time(tloc_, tloc_);
    } else {
      return new SoTRange.Double(yloc_, yloc_);
    }
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
