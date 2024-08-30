/*
 * $Id: SGTData.java,v 1.9 2001/10/10 19:05:01 dwd Exp $
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
import gov.noaa.pmel.util.SoTRange;
import java.beans.PropertyChangeListener;

/**
 * Base class for sgt datamodel rank information. The <code>SGTData</code> class and its children
 * are used by sgt to determine the rank (point, line, grid) of the data. Data values can be either
 * <code>double</code> or <code>GeoDate</code>, which extends <code>Date</code>. Missing values are
 * indicated by <code>Double.NaN</code> for type <code>double</code> and by <code>null</code> or by
 * <code>Long.MIN_VALUE</code> milliseconds after (before) January 1, 1970 00:00:00 GMT for <code>
 * GeoDate</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.9 $, $Date: 2001/10/10 19:05:01 $
 * @since 1.0
 * @see SGTPoint
 * @see SGTLine
 * @see SGTGrid
 */
public interface SGTData {

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception;

  /** Get the title. */
  public String getTitle();

  /**
   * Get a title formatted for a Key. <code>JPlotLayout</code> will use this if an explicit Key
   * title is not given in the <code>addData</code> method.
   *
   * @see gov.noaa.pmel.sgt.SGLabel
   * @see gov.noaa.pmel.sgt.ColorKey
   * @see gov.noaa.pmel.sgt.LineKey
   * @see gov.noaa.pmel.sgt.PointCollectionKey
   * @see gov.noaa.pmel.sgt.VectorKey
   */
  public SGLabel getKeyTitle();

  /**
   * Get the unique identifier. The presence of the identifier is optional, but if it is present it
   * should be unique. This field is used to search for the layer that contains the data.
   *
   * @return unique identifier
   * @see gov.noaa.pmel.sgt.Pane
   * @see gov.noaa.pmel.sgt.Layer
   */
  public String getId();

  /**
   * Create a shallow copy. User should implement using the clone() method, which requires the
   * Cloneable interface be inherited. If clone() is used, then references to objects are copied NOT
   * the object itself.
   *
   * <p>For example,
   *
   * <pre>
   * public SGTData copy() {
   *   SGTData newData;
   *   try {
   *     newData = (SGTData)clone();
   *   } catch (CloneNotSupportedException e) {
   *     newData = null;
   *   }
   *   return newData;
   * }
   * </pre>
   *
   * @return shallow copy
   * @see java.lang.Object
   */
  public SGTData copy();

  /** Returns true if the X coordinate is Time. */
  public boolean isXTime();

  /** Returns true if the Y coordinate is Time. */
  public boolean isYTime();

  /** Returns the X SGTMetaData. */
  public SGTMetaData getXMetaData();

  /** Returns the Y SGTMetaData. */
  public SGTMetaData getYMetaData();

  /**
   * Returns the range of the X coordinates. If all the data in the array is missing, this method
   * will return <code>Double.NaN</code> as the start and end values for data of type <code>double
   * </code> and return <code>GeoDate(Long.MIN_VALUE)</code> for data of type <code>GeoDate</code>.
   *
   * @see gov.noaa.pmel.util.GeoDate#isMissing()
   */
  public SoTRange getXRange();

  /**
   * Returns the range of the Y coordinates.
   *
   * @see #getXRange()
   */
  public SoTRange getYRange();

  /** Add a PropertyChangeListener to the listener list. */
  public void addPropertyChangeListener(PropertyChangeListener l);

  /** Remove a PropertyChangeListener from the listener list. */
  public void removePropertyChangeListener(PropertyChangeListener l);
}
