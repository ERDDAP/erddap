/*
 * $Id: SimpleTuple.java,v 1.7 2003/08/22 23:02:38 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.dm;

import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.GeoDateArray;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * <code>SimpleTuple</code> provides an implementation of the <code>SGTTuple</code> and <code>
 * Cartesian</code> interfaces.
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/08/22 23:02:38 $
 * @since 2.x
 * @see SGTTuple
 * @see Cartesian
 */
public class SimpleTuple implements Cloneable, SGTTuple, Cartesian, Serializable {
  private boolean xTime_ = false;
  private boolean yTime_ = false;
  private String title_ = null;
  private SGLabel keyTitle_ = null;
  private String id_ = null;
  private double[] xArray_ = null;
  private double[] yArray_ = null;
  //  private GeoDate[] tArray_ = null;
  private GeoDateArray tArray_ = null;
  private double[] zArray_ = null;
  private double[] assocArray_ = null;

  /**
   * @link aggregation
   * @clientRole x
   */
  protected SGTMetaData xMetaData_ = null;

  /**
   * @link aggregation
   * @clientRole y
   */
  protected SGTMetaData yMetaData_ = null;

  /**
   * @link aggregation
   * @clientRole z
   */
  protected SGTMetaData zMetaData_ = null;

  private SoTRange xRange_ = null;
  private SoTRange yRange_ = null;
  private Range2D zRange_ = null;

  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  public SimpleTuple(double[] xArray, double[] yArray, String title) {
    xArray_ = xArray;
    yArray_ = yArray;
    xTime_ = false;
    yTime_ = false;
    title_ = title;
    xRange_ = computeSoTRange(xArray);
    yRange_ = computeSoTRange(yArray);
  }

  public SimpleTuple(GeoDate[] xArray, double[] yArray, String title) {
    tArray_ = new GeoDateArray(xArray);
    yArray_ = yArray;
    xTime_ = true;
    yTime_ = false;
    title_ = title;
    xRange_ = computeSoTRange(tArray_);
    yRange_ = computeSoTRange(yArray);
  }

  public SimpleTuple(double[] xArray, GeoDate[] yArray, String title) {
    xArray_ = xArray;
    tArray_ = new GeoDateArray(yArray);
    xTime_ = false;
    yTime_ = true;
    title_ = title;
    xRange_ = computeSoTRange(xArray);
    yRange_ = computeSoTRange(tArray_);
  }

  public SimpleTuple(double[] xArray, double[] yArray, double[] zArray, String title) {
    xArray_ = xArray;
    yArray_ = yArray;
    zArray_ = zArray;
    xTime_ = false;
    yTime_ = false;
    title_ = title;
    xRange_ = computeSoTRange(xArray);
    yRange_ = computeSoTRange(yArray);
    zRange_ = computeRange2D(zArray);
  }

  public SimpleTuple(GeoDate[] xArray, double[] yArray, double[] zArray, String title) {
    tArray_ = new GeoDateArray(xArray);
    yArray_ = yArray;
    zArray_ = zArray;
    xTime_ = true;
    yTime_ = false;
    title_ = title;
    xRange_ = computeSoTRange(tArray_);
    yRange_ = computeSoTRange(yArray);
    zRange_ = computeRange2D(zArray);
  }

  public SimpleTuple(double[] xArray, GeoDate[] yArray, double[] zArray, String title) {
    xArray_ = xArray;
    tArray_ = new GeoDateArray(yArray);
    zArray_ = zArray;
    xTime_ = false;
    yTime_ = true;
    title_ = title;
    xRange_ = computeSoTRange(xArray);
    yRange_ = computeSoTRange(tArray_);
    zRange_ = computeRange2D(zArray);
  }

  public SimpleTuple() {
    xTime_ = false;
    yTime_ = false;
  }

  @Override
  public double[] getXArray() {
    return xArray_;
  }

  public void setXArray(double[] xArray) {
    xArray_ = xArray;
    xTime_ = false;
    xRange_ = computeSoTRange(xArray);
    changes_.firePropertyChange("dataModified", Integer.valueOf(0), Integer.valueOf(xArray.length));
  }

  public void setXArray(GeoDate[] tArray) {
    setXArray(new GeoDateArray(tArray));
  }

  /**
   * @since 3.0
   */
  public void setXArray(GeoDateArray tArray) {
    tArray_ = tArray;
    xTime_ = true;
    xArray_ = null;
    xRange_ = computeSoTRange(tArray);
    changes_.firePropertyChange(
        "dataModified", Integer.valueOf(0), Integer.valueOf(tArray.getLength()));
  }

  @Override
  public double[] getYArray() {
    return yArray_;
  }

  public void setYArray(double[] yArray) {
    yArray_ = yArray;
    yTime_ = false;
    yRange_ = computeSoTRange(yArray);
    changes_.firePropertyChange("dataModified", Integer.valueOf(0), Integer.valueOf(yArray.length));
  }

  public void setYArray(GeoDate[] tArray) {
    setYArray(new GeoDateArray(tArray));
  }

  /**
   * @since 3.0
   */
  public void setYArray(GeoDateArray tArray) {
    tArray_ = tArray;
    yTime_ = true;
    yArray_ = null;
    yRange_ = computeSoTRange(tArray);
    changes_.firePropertyChange(
        "dataModified", Integer.valueOf(0), Integer.valueOf(tArray.getLength()));
  }

  @Override
  public double[] getZArray() {
    return zArray_;
  }

  public void setZArray(double[] zArray) {
    zArray_ = zArray;
    zRange_ = computeRange2D(zArray);
    changes_.firePropertyChange("dataModified", Integer.valueOf(0), Integer.valueOf(zArray.length));
  }

  @Override
  public int getSize() {
    if (xTime_) {
      return tArray_.getLength();
    } else {
      return xArray_.length;
    }
  }

  @Override
  public GeoDate[] getTimeArray() {
    return tArray_.getGeoDate();
  }

  /**
   * @since 3.0
   */
  @Override
  public GeoDateArray getGeoDateArray() {
    return tArray_;
  }

  @Override
  public double[] getAssociatedData() {
    return assocArray_;
  }

  public void setAssociatedData(double[] assocArray) {
    assocArray_ = assocArray;
  }

  @Override
  public boolean hasAssociatedData() {
    return (assocArray_ != null);
  }

  @Override
  public SGTMetaData getZMetaData() {
    return zMetaData_;
  }

  public void setZMetaData(SGTMetaData zMeta) {
    zMetaData_ = zMeta;
  }

  @Override
  public String getTitle() {
    return title_;
  }

  public void setTitle(String title) {
    title_ = title;
  }

  @Override
  public SGLabel getKeyTitle() {
    return keyTitle_;
  }

  public void setKeyTitle(SGLabel title) {
    keyTitle_ = title;
  }

  @Override
  public String getId() {
    return id_;
  }

  public void setId(String id) {
    id_ = id;
  }

  @Override
  public SGTData copy() {
    SGTTuple newTuple;
    try {
      newTuple = (SGTTuple) clone();
    } catch (CloneNotSupportedException e) {
      newTuple = new SimpleTuple();
    }
    return (SGTData) newTuple;
  }

  @Override
  public boolean isXTime() {
    return xTime_;
  }

  @Override
  public boolean isYTime() {
    return yTime_;
  }

  @Override
  public SGTMetaData getXMetaData() {
    return xMetaData_;
  }

  public void setXMetaData(SGTMetaData xMeta) {
    xMetaData_ = xMeta;
  }

  @Override
  public SGTMetaData getYMetaData() {
    return yMetaData_;
  }

  public void setYMetaData(SGTMetaData yMeta) {
    yMetaData_ = yMeta;
  }

  @Override
  public SoTRange getXRange() {
    return xRange_;
  }

  @Override
  public SoTRange getYRange() {
    return yRange_;
  }

  @Override
  public Range2D getZRange() {
    return zRange_;
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
    Range2D range = computeRange2D(array);
    return new SoTRange.Double(range.start, range.end);
  }

  private SoTRange computeSoTRange(GeoDateArray tarray) {
    long start = Long.MAX_VALUE;
    long end = Long.MIN_VALUE;
    long[] tar = tarray.getTime();
    int count = 0;
    for (int i = 0; i < tar.length; i++) {
      if (!(tar[i] == Long.MAX_VALUE)) {
        start = Math.min(start, tar[i]);
        end = Math.max(end, tar[i]);
        count++;
      }
    }
    if (count == 0) {
      return new SoTRange.Time(Long.MAX_VALUE, Long.MAX_VALUE);
    } else {
      return new SoTRange.Time(start, end);
    }
  }

  private Range2D computeRange2D(double[] array) {
    double start = Double.POSITIVE_INFINITY;
    double end = Double.NEGATIVE_INFINITY;
    int count = 0;
    for (int i = 0; i < array.length; i++) {
      if (!Double.isNaN(array[i])) {
        start = Math.min(start, array[i]);
        end = Math.max(end, array[i]);
        count++;
      }
    }
    if (count == 0) {
      return new Range2D(Double.NaN, Double.NaN);
    } else {
      return new Range2D(start, end);
    }
  }

  @Override
  public void releaseResources() throws Exception { // Kyle and Bob added
    title_ = null;
    keyTitle_ = null;
    id_ = null;
    xArray_ = null;
    yArray_ = null;
    tArray_ = null;
    zArray_ = null;
    assocArray_ = null;
  }
}
