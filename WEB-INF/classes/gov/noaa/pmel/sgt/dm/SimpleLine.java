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
import gov.noaa.pmel.util.GeoDateArray;
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
  protected String id_ = null;

  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      xloc_ = null;
      yloc_ = null;
      tloc_ = null;
      changes_ = null;
      if (JPane.debug) String2.log("sgt.dm.SimpleLine.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constuctor. */
  public SimpleLine() {
    this((double[]) null, (double[]) null);
  }

  /**
   * Constructor for X and Y double.
   *
   * @param xloc X coordinates
   * @param yloc Y coordinates
   * @param title the Title
   */
  public SimpleLine(double[] xloc, double[] yloc) {
    xloc_ = xloc;
    yloc_ = yloc;
    xTime_ = false;
    yTime_ = false;
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

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
