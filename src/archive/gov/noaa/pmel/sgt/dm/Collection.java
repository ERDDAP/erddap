/*
 * $Id: Collection.java,v 1.8 2001/10/10 19:05:01 dwd Exp $
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

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.SoTRange;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <code>Collection</code> is an extension to <code>Vector</code> designed to hold <code>SGTData
 * </code> objects. These objects must have consistent x and y coordinate types. Otherwise, the
 * <code>isXTime()</code>, <code>isYTime()</code>, <code>getXMetaData()</code>, <code>getYMetaData()
 * </code>, and <code>get?Ranges()</code> methods will fail.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2001/10/10 19:05:01 $
 * @since 1.0
 * @see SGTData
 * @see SGTPoint
 * @see SGTLine
 * @see SGTGrid
 * @see SGTVector
 */
public class Collection extends Vector implements SGTData, Cloneable {
  private String title_;
  private SGLabel keyTitle_ = null;
  private String id_ = null;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  private SoTRange xRange_ = null;
  private SoTRange yRange_ = null;
  private int colLen_ = 0;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      keyTitle_ = null;
      changes_ = null;
      clear();
      if (JPane.debug) String2.log("sgt.dm.Collection.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  public Collection() {
    this("");
  }

  public Collection(String title) {
    super();
    title_ = title;
  }

  public Collection(String title, int initialCapacity) {
    super(initialCapacity);
    title_ = title;
  }

  public Collection(String title, int initialCapacity, int increment) {
    super(initialCapacity, increment);
    title_ = title;
  }

  /**
   * Create a copy.
   *
   * @see SGTData
   */
  @Override
  public SGTData copy() {
    Collection newCollection;
    newCollection = (Collection) clone();
    return (SGTData) newCollection;
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

  public void setKeyTitle(SGLabel title) {
    keyTitle_ = title;
  }

  /**
   * Get the unique identifier. The presence of the identifier is optional, but if it is present it
   * should be unique. This field is used to search for the layer that contains the data.
   *
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

  @Override
  public boolean isXTime() {
    return ((SGTData) firstElement()).isXTime();
  }

  @Override
  public boolean isYTime() {
    return ((SGTData) firstElement()).isYTime();
  }

  @Override
  public SGTMetaData getXMetaData() {
    return ((SGTData) firstElement()).getXMetaData();
  }

  @Override
  public SGTMetaData getYMetaData() {
    return ((SGTData) firstElement()).getYMetaData();
  }

  @Override
  public SoTRange getXRange() {
    computeRange();
    return xRange_.copy();
  }

  @Override
  public SoTRange getYRange() {
    computeRange();
    return yRange_.copy();
  }

  private void computeRange() {
    if (colLen_ == size()) return;
    colLen_ = size();
    xRange_ = ((SGTData) firstElement()).getXRange();
    yRange_ = ((SGTData) firstElement()).getYRange();

    Enumeration en = elements();
    while (en.hasMoreElements()) {
      SGTData data = (SGTData) en.nextElement();
      xRange_.add(data.getXRange());
      yRange_.add(data.getYRange());
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
