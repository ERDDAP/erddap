/**
 * $Id: SimpleGrid.java,v 1.19 2003/08/22 23:02:38 dwd Exp $
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
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.GeoDateArray;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * <code>SimpleGrid</code> provides an implementation of the <code>SGTGrid</code> and <code>
 * Cartesian</code> interfaces.
 *
 * @author Donald Denbo
 * @version $Revision: 1.19 $, $Date: 2003/08/22 23:02:38 $
 * @since 1.0
 * @see SGTGrid
 * @see Cartesian
 */
public class SimpleGrid implements SGTGrid, Cartesian, Cloneable, Serializable {
  protected double[] xloc_;
  protected double[] yloc_;
  //  protected GeoDate[] tloc_;
  protected GeoDateArray tloc_;
  protected double[] grid_;
  protected double[] xEdges_;
  protected double[] yEdges_;
  //  protected GeoDate[] tEdges_;
  protected GeoDateArray tEdges_;
  protected boolean hasXEdges_;
  protected boolean hasYEdges_;
  protected String id_ = null;
  protected boolean xTime_;
  protected boolean yTime_;

  /**
   * @shapeType AggregationLink
   * @clientRole associated data
   */
  protected SGTGrid associatedData_;

  private final PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      xloc_ = null;
      yloc_ = null;
      tloc_ = null;
      grid_ = null;
      xEdges_ = null;
      yEdges_ = null;
      tEdges_ = null;
      associatedData_ = null;
      if (JPane.debug) String2.log("sgt.dm.SimpleGrid.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public SimpleGrid() {
    this(null, (double[]) null, (double[]) null);
  }

  /**
   * Constructor for X and Y coordinates as double.
   *
   * @param grid Z values
   * @param xloc X coordinates
   * @param yloc Y coordinates
   * @param title the title
   */
  public SimpleGrid(double[] grid, double[] xloc, double[] yloc) {
    grid_ = grid;
    xloc_ = xloc;
    yloc_ = yloc;
    xTime_ = false;
    yTime_ = false;
    hasXEdges_ = false;
    hasYEdges_ = false;
  }

  /**
   * Constructor for X time and Y double.
   *
   * @param grid Z values
   * @param tloc Time coordinates
   * @param yloc Y coordinates
   * @param title the title
   */
  public SimpleGrid(double[] grid, GeoDate[] tloc, double[] yloc) {
    grid_ = grid;
    tloc_ = new GeoDateArray(tloc);
    yloc_ = yloc;
    xTime_ = true;
    yTime_ = false;
    hasXEdges_ = false;
    hasYEdges_ = false;
  }

  /**
   * Constructor for X double and Y time.
   *
   * @param grid Z values
   * @param xloc X coordinates
   * @param tloc Time coordinates
   * @param title the title
   */
  public SimpleGrid(double[] grid, double[] xloc, GeoDate[] tloc) {
    grid_ = grid;
    xloc_ = xloc;
    tloc_ = new GeoDateArray(tloc);
    xTime_ = false;
    yTime_ = true;
    hasXEdges_ = false;
    hasYEdges_ = false;
  }

  @Override
  public double[] getXArray() {
    return xloc_;
  }

  /**
   * Get the length of the x axis
   *
   * @since 2.0
   */
  @Override
  public int getXSize() {
    return xloc_.length;
  }

  @Override
  public double[] getYArray() {
    return yloc_;
  }

  /**
   * Get the length of the y axis
   *
   * @since 2.0
   */
  @Override
  public int getYSize() {
    return yloc_.length;
  }

  @Override
  public double[] getZArray() {
    return grid_;
  }

  @Override
  public GeoDate[] getTimeArray() {
    return tloc_.getGeoDate();
  }

  /**
   * Get the length of the Time axis
   *
   * @since 2.0
   */
  @Override
  public int getTSize() {
    return tloc_.getLength();
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
  public boolean hasXEdges() {
    return hasXEdges_;
  }

  @Override
  public double[] getXEdges() {
    return xEdges_;
  }

  @Override
  public boolean hasYEdges() {
    return hasYEdges_;
  }

  @Override
  public double[] getYEdges() {
    return yEdges_;
  }

  @Override
  public GeoDate[] getTimeEdges() {
    return tEdges_.getGeoDate();
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

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
}
