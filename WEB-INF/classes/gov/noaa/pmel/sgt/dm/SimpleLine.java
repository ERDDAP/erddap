/**
 * $Id: SimpleLine.java,v 1.15 2003/08/22 23:02:38 dwd Exp $
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
import gov.noaa.pmel.util.GeoDateArray;
import gov.noaa.pmel.util.SoTRange;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * <code>SimpleLine</code> provides an implementation of the <code>SGTLine</code> and <code>
 * Cartesian</code> interfaces.
 *
 * @author Donald Denbo
 * @version $Revision: 1.15 $, $Date: 2003/08/22 23:02:38 $
 * @since 1.0
 * @see SGTLine
 * @see Cartesian
 */
public class SimpleLine implements SGTLine, Cartesian, Serializable, Cloneable {
  protected double[] xloc_;
  protected double[] yloc_;
  //  protected GeoDate[] tloc_;
  protected GeoDateArray tloc_;
  protected boolean xTime_;
  protected boolean yTime_;
  protected String title_;
  protected SGLabel keyTitle_ = null;
  protected String id_ = null;

  /**
   * @shapeType AggregationLink
   * @clientRole x
   */
  protected SGTMetaData xMetaData_ = null;

  /**
   * @shapeType AggregationLink
   * @clientRole y
   */
  protected SGTMetaData yMetaData_ = null;

  /**
   * @shapeType AggregationLink
   * @clientRole associated data
   */
  protected SGTLine associatedData_ = null;

  private SoTRange xRange_ = null;
  private SoTRange yRange_ = null;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      xloc_ = null;
      yloc_ = null;
      tloc_ = null;
      keyTitle_ = null;
      xMetaData_ = null;
      yMetaData_ = null;
      associatedData_ = null;
      xRange_ = null;
      yRange_ = null;
      changes_ = null;
      if (JPane.debug) String2.log("sgt.dm.SimpleLine.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constuctor. */
  public SimpleLine() {
    this((double[]) null, (double[]) null, "");
  }

  /**
   * Constructor for X and Y double.
   *
   * @param xloc X coordinates
   * @param yloc Y coordinates
   * @param title the Title
   */
  public SimpleLine(double[] xloc, double[] yloc, String title) {
    xloc_ = xloc;
    yloc_ = yloc;
    title_ = title;
    xTime_ = false;
    yTime_ = false;
    xRange_ = computeSoTRange(xloc);
    yRange_ = computeSoTRange(yloc);
  }

  /**
   * Constructor for X Time and Y double.
   *
   * @param tloc Time coordinates
   * @param yloc Y coordinates
   * @param title the Title
   */
  public SimpleLine(GeoDate[] tloc, double[] yloc, String title) {
    tloc_ = new GeoDateArray(tloc);
    yloc_ = yloc;
    title_ = title;
    xTime_ = true;
    yTime_ = false;
    xRange_ = computeSoTRange(tloc_);
    yRange_ = computeSoTRange(yloc);
  }

  /**
   * Constructor for X Time and Y double.
   *
   * @since 3.0
   * @param tloc Time coordinates
   * @param yloc Y coordinates
   * @param title the Title
   */
  public SimpleLine(GeoDateArray tloc, double[] yloc, String title) {
    tloc_ = tloc;
    yloc_ = yloc;
    title_ = title;
    xTime_ = true;
    yTime_ = false;
    xRange_ = computeSoTRange(tloc_);
    yRange_ = computeSoTRange(yloc);
  }

  /**
   * Constructor for X double and Y Time.
   *
   * @since 3.0
   * @param xloc X coordinates
   * @param tloc Time coordinates
   * @param title the Title
   */
  public SimpleLine(double[] xloc, GeoDateArray tloc, String title) {
    xloc_ = xloc;
    tloc_ = tloc;
    title_ = title;
    xTime_ = false;
    yTime_ = true;
    xRange_ = computeSoTRange(xloc);
    yRange_ = computeSoTRange(tloc_);
  }

  /**
   * Constructor for X double and Y Time.
   *
   * @param xloc X coordinates
   * @param tloc Time coordinates
   * @param title the Title
   */
  public SimpleLine(double[] xloc, GeoDate[] tloc, String title) {
    xloc_ = xloc;
    tloc_ = new GeoDateArray(tloc);
    title_ = title;
    xTime_ = false;
    yTime_ = true;
    xRange_ = computeSoTRange(xloc);
    yRange_ = computeSoTRange(tloc_);
  }

  /**
   * Create a shallow copy.
   *
   * @since 2.0
   * @see SGTData
   */
  @Override
  public SGTData copy() {
    SGTLine newLine;
    try {
      newLine = (SGTLine) clone();
    } catch (CloneNotSupportedException e) {
      newLine = new SimpleLine();
    }
    return (SGTData) newLine;
  }

  /** Get the X coordinate array. */
  @Override
  public double[] getXArray() {
    return xloc_;
  }

  /** Get the Y coordinate array. */
  @Override
  public double[] getYArray() {
    return yloc_;
  }

  /** Get the Time coordinate array. */
  @Override
  public GeoDate[] getTimeArray() {
    return tloc_.getGeoDate();
  }

  /**
   * Get the <code>GeoDateArray</code> object.
   *
   * @since 3.0
   */
  @Override
  public GeoDateArray getGeoDateArray() {
    return tloc_;
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

  /** Get the X coordinate metadata. */
  @Override
  public SGTMetaData getXMetaData() {
    return xMetaData_;
  }

  /** Get the Y coordinate metadata */
  @Override
  public SGTMetaData getYMetaData() {
    return yMetaData_;
  }

  /** Get the Title. */
  @Override
  public String getTitle() {
    return title_;
  }

  /**
   * Get the unique identifier. The presence of the identifier is optional, but if it is present it
   * should be unique. This field is used to search for the layer that contains the data.
   *
   * @since 2.0
   * @return unique identifier
   * @see gov.noaa.pmel.sgt.JPane
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

  /**
   * Set the data that will be associated with <code>SGTLine</code> <br>
   * <strong>Property Change:</strong> <code>associatedDataModified</code>.
   *
   * @since 2.0
   */
  public void setAssociatedData(SGTLine assoc) {
    associatedData_ = assoc;
    changes_.firePropertyChange("associatedDataModified", null, assoc);
  }

  /** Get the associated data. */
  @Override
  public SGTLine getAssociatedData() {
    return associatedData_;
  }

  /** Is there associated data? */
  @Override
  public boolean hasAssociatedData() {
    return (associatedData_ != null);
  }

  /** Set the X coordinate metadata. */
  public void setXMetaData(SGTMetaData md) {
    xMetaData_ = md;
  }

  /** Set the Y coordinate metadata. */
  public void setYMetaData(SGTMetaData md) {
    yMetaData_ = md;
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
   * Set the X coordinate array. <br>
   * <strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setXArray(double[] xloc) {
    xloc_ = xloc;
    xTime_ = false;
    xRange_ = computeSoTRange(xloc);
    changes_.firePropertyChange("dataModified", Integer.valueOf(0), Integer.valueOf(xloc.length));
  }

  /**
   * Set the Y coordinate array <br>
   * <strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setYArray(double[] yloc) {
    yloc_ = yloc;
    yTime_ = false;
    yRange_ = computeSoTRange(yloc);
    changes_.firePropertyChange("dataModified", Integer.valueOf(0), Integer.valueOf(yloc.length));
  }

  /**
   * Set the Time coordinate array <br>
   * <strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setTimeArray(GeoDate[] tloc) {
    setTimeArray(new GeoDateArray(tloc));
  }

  /**
   * @since 3.0
   */
  public void setTimeArray(GeoDateArray tarray) {
    tloc_ = tarray;
    if (xTime_) {
      xRange_ = computeSoTRange(tarray);
    } else if (yTime_) {
      yRange_ = computeSoTRange(tarray);
    }
    changes_.firePropertyChange(
        "dataModified", Integer.valueOf(0), Integer.valueOf(tarray.getLength()));
  }

  @Override
  public SoTRange getXRange() {
    return xRange_.copy();
  }

  @Override
  public SoTRange getYRange() {
    return yRange_.copy();
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }

  private SoTRange computeSoTRange(double[] array) {
    double dstart = Double.POSITIVE_INFINITY;
    double dend = Double.NEGATIVE_INFINITY;
    int count = 0;
    for (int i = 0; i < array.length; i++) {
      if (!Double.isNaN(array[i])) {
        dstart = Math.min(dstart, array[i]);
        dend = Math.max(dend, array[i]);
        count++;
      }
    }
    if (count == 0) {
      return new SoTRange.Double(Double.NaN, Double.NaN);
    } else {
      return new SoTRange.Double(dstart, dend);
    }
  }

  private SoTRange computeSoTRange(GeoDateArray tarray) {
    long tstart = Long.MAX_VALUE;
    long tend = Long.MIN_VALUE;
    long[] tar = tarray.getTime();
    int count = 0;
    for (int i = 0; i < tar.length; i++) {
      if (!(tar[i] == Long.MAX_VALUE)) {
        tstart = Math.min(tstart, tar[i]);
        tend = Math.max(tend, tar[i]);
        count++;
      }
    }
    if (count == 0) {
      return new SoTRange.Time(Long.MAX_VALUE, Long.MAX_VALUE);
    } else {
      return new SoTRange.Time(tstart, tend);
    }
  }
}
