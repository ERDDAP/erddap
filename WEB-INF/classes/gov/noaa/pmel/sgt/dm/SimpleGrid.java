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
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.GeoDateArray;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
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
  protected String title_;
  protected SGLabel keyTitle_ = null;
  protected String id_ = null;
  protected boolean xTime_;
  protected boolean yTime_;

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
   * @clientRole z
   */
  protected SGTMetaData zMetaData_ = null;

  /**
   * @shapeType AggregationLink
   * @clientRole associated data
   */
  protected SGTGrid associatedData_;

  private SoTRange xRange_ = null;
  private SoTRange yRange_ = null;
  private SoTRange xEdgesRange_ = null;
  private SoTRange yEdgesRange_ = null;
  private Range2D zRange_ = null;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

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
      keyTitle_ = null;
      xMetaData_ = null;
      yMetaData_ = null;
      zMetaData_ = null;
      associatedData_ = null;
      if (JPane.debug) String2.log("sgt.dm.SimpleGrid.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Default constructor. */
  public SimpleGrid() {
    this(null, (double[]) null, (double[]) null, "");
  }

  /**
   * Constructor for X and Y coordinates as double.
   *
   * @param grid Z values
   * @param xloc X coordinates
   * @param yloc Y coordinates
   * @param title the title
   */
  public SimpleGrid(double[] grid, double[] xloc, double[] yloc, String title) {
    grid_ = grid;
    xloc_ = xloc;
    yloc_ = yloc;
    title_ = title;
    xTime_ = false;
    yTime_ = false;
    hasXEdges_ = false;
    hasYEdges_ = false;
    xRange_ = computeSoTRange(xloc);
    yRange_ = computeSoTRange(yloc);
    zRange_ = computeRange2D(grid);
  }

  /**
   * Constructor for X time and Y double.
   *
   * @param grid Z values
   * @param tloc Time coordinates
   * @param yloc Y coordinates
   * @param title the title
   */
  public SimpleGrid(double[] grid, GeoDate[] tloc, double[] yloc, String title) {
    grid_ = grid;
    tloc_ = new GeoDateArray(tloc);
    yloc_ = yloc;
    title_ = title;
    xTime_ = true;
    yTime_ = false;
    hasXEdges_ = false;
    hasYEdges_ = false;
    xRange_ = computeSoTRange(tloc_);
    yRange_ = computeSoTRange(yloc);
    zRange_ = computeRange2D(grid);
  }

  /**
   * Constructor for X double and Y time.
   *
   * @param grid Z values
   * @param xloc X coordinates
   * @param tloc Time coordinates
   * @param title the title
   */
  public SimpleGrid(double[] grid, double[] xloc, GeoDate[] tloc, String title) {
    grid_ = grid;
    xloc_ = xloc;
    tloc_ = new GeoDateArray(tloc);
    title_ = title;
    xTime_ = false;
    yTime_ = true;
    hasXEdges_ = false;
    hasYEdges_ = false;
    xRange_ = computeSoTRange(xloc);
    yRange_ = computeSoTRange(tloc_);
    zRange_ = computeRange2D(grid);
  }

  /**
   * Create a copy of the grid.
   *
   * @since 2.0
   * @see SGTData
   */
  @Override
  public SGTData copy() {
    SGTGrid newGrid;
    try {
      newGrid = (SGTGrid) clone();
    } catch (CloneNotSupportedException e) {
      newGrid = new SimpleGrid();
    }
    return (SGTData) newGrid;
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
   * Get the <code>GeoDateArray</code> object.
   *
   * @since 3.0
   */
  @Override
  public GeoDateArray getGeoDateArray() {
    return tloc_;
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
  public SGTMetaData getXMetaData() {
    return xMetaData_;
  }

  @Override
  public SGTMetaData getYMetaData() {
    return yMetaData_;
  }

  @Override
  public SGTMetaData getZMetaData() {
    return zMetaData_;
  }

  @Override
  public String getTitle() {
    return title_;
  }

  /**
   * Set the associated data grid. <br>
   * <strong>Property Change:</strong> <code>associatedDataModified</code>.
   *
   * @since 2.0
   */
  public void setAssociatedData(SGTGrid assoc) {
    associatedData_ = assoc;
    changes_.firePropertyChange("associatedDataModified", null, assoc);
  }

  @Override
  public SGTGrid getAssociatedData() {
    return associatedData_;
  }

  @Override
  public boolean hasAssociatedData() {
    return (associatedData_ != null);
  }

  @Override
  public boolean hasXEdges() {
    return hasXEdges_;
  }

  @Override
  public double[] getXEdges() {
    return xEdges_;
  }

  /** Set the values for the x grid edges. */
  public void setXEdges(double[] edge) {
    xEdges_ = edge;
    hasXEdges_ = true;
    xEdgesRange_ = computeSoTRange(edge);
  }

  @Override
  public boolean hasYEdges() {
    return hasYEdges_;
  }

  @Override
  public double[] getYEdges() {
    return yEdges_;
  }

  /** Set the values for the y grid edges. */
  public void setYEdges(double[] edge) {
    yEdges_ = edge;
    hasYEdges_ = true;
    yEdgesRange_ = computeSoTRange(edge);
  }

  @Override
  public GeoDate[] getTimeEdges() {
    return tEdges_.getGeoDate();
  }

  /**
   * Get the <code>GeoDateArray</code> object.
   *
   * @since 3.0
   */
  @Override
  public GeoDateArray getGeoDateArrayEdges() {
    return tEdges_;
  }

  /** Set the values for the temporal grid edges. */
  public void setTimeEdges(GeoDate[] edge) {
    setTimeEdges(new GeoDateArray(edge));
  }

  /**
   * @since 3.0
   */
  public void setTimeEdges(GeoDateArray tarray) {
    tEdges_ = tarray;
    if (xTime_) {
      hasXEdges_ = true;
      xEdgesRange_ = computeSoTRange(tarray);
    } else if (yTime_) {
      hasYEdges_ = true;
      yEdgesRange_ = computeSoTRange(tarray);
    }
  }

  /** Set the <code>SGTMetaData</code> associated with the x coordinate. */
  public void setXMetaData(SGTMetaData md) {
    xMetaData_ = md;
  }

  /** Set the <code>SGTMetaData</code> associated with the y coordinate. */
  public void setYMetaData(SGTMetaData md) {
    yMetaData_ = md;
  }

  /** Set the <code>SGTMetaData</code> associated with the z coordinate. */
  public void setZMetaData(SGTMetaData md) {
    zMetaData_ = md;
  }

  /** Set the grid title */
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

  /**
   * Set the x coordinate grid centers <br>
   * <strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setXArray(double[] xloc) {
    xloc_ = xloc;
    xTime_ = false;
    xRange_ = computeSoTRange(xloc);
    changes_.firePropertyChange("dataModified", Integer.valueOf(0), Integer.valueOf(xloc.length));
  }

  /**
   * Set the y coordinate grid centers <br>
   * <strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setYArray(double[] yloc) {
    yloc_ = yloc;
    yTime_ = false;
    yRange_ = computeSoTRange(yloc);
    changes_.firePropertyChange("dataModified", Integer.valueOf(0), Integer.valueOf(yloc.length));
  }

  /**
   * Set the z grid values. <br>
   * <strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setZArray(double[] grid) {
    grid_ = grid;
    zRange_ = computeRange2D(grid);
    changes_.firePropertyChange("dataModified", Integer.valueOf(0), Integer.valueOf(grid.length));
  }

  /**
   * set the temporal grid centers <br>
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
  public Range2D getZRange() {
    return zRange_;
  }

  /**
   * Return the range of the x edges
   *
   * @since 2.0
   */
  @Override
  public SoTRange getXEdgesRange() {
    return xEdgesRange_;
  }

  /**
   * Return the range of the y edges
   *
   * @since 2.0
   */
  @Override
  public SoTRange getYEdgesRange() {
    return yEdgesRange_;
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
}
