/**
 * $Id: ThreeDGrid.java,v 1.3 2003/02/11 01:47:40 oz Exp $
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
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * <code>ThreeDGrid</code> provides an implementation of the
 * <code>SGT3DGrid</code> and <code>Cartesian</code> interfaces.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/02/11 01:47:40 $
 * @since 1.0
 * @see SGTGrid
 * @see Cartesian
 */
public class ThreeDGrid implements SGT3DGrid, Cartesian, Cloneable, Serializable {
  protected double[] xloc_;
  protected double[] yloc_;
  protected double[] zloc_;
  protected GeoDate[] tloc_;
  protected double[] grid_;
  protected double[] xEdges_;
  protected double[] yEdges_;
  protected double[] zEdges_;
  protected GeoDate[] tEdges_;
  protected boolean hasXEdges_;
  protected boolean hasYEdges_;
  protected boolean hasZEdges_;
  protected String title_;
  protected SGLabel keyTitle_ = null;
  protected String id_ = null;
  protected boolean xTime_;
  protected boolean yTime_;
  protected boolean zTime_;
    /**@shapeType AggregationLink
  * @clientRole x*/
    protected SGTMetaData xMetaData_ = null;
    /**@shapeType AggregationLink
  * @clientRole y*/
    protected SGTMetaData yMetaData_ = null;
    /**@shapeType AggregationLink
  * @clientRole z*/
    protected SGTMetaData zMetaData_ = null;
    /**@shapeType AggregationLink
  * @clientRole associated data*/
    protected SGTMetaData valMetaData_ = null;
    protected SGTGrid associatedData_;
  private SoTRange xRange_ = null;
  private SoTRange yRange_ = null;
  private SoTRange zRange_ = null;
  private SoTRange xEdgesRange_ = null;
  private SoTRange yEdgesRange_ = null; 
  private SoTRange zEdgesRange_ = null;
  private Range2D valRange_ = null;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

    /** 
     * Bob Simons added this to avoid memory leak problems.
     */
    public void releaseResources() throws Exception {
        try {  
            xMetaData_ = null;
            yMetaData_ = null;
            zMetaData_ = null;
            valMetaData_ = null;
            associatedData_ = null;
            if (JPane.debug) String2.log("sgt.dm.ThreeDGrid.releaseResources() finished");
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            if (JPane.debug) 
                String2.pressEnterToContinue(); 
        }
    }

  /**
   * Default constructor.
   */
  public ThreeDGrid() {
    //this(null, (double[])null, (double[])null, (double[])null, "");
  }
  
  /**
   * Constructor for X, Y, and Z coordinates as double.
   *
   * @param grid Z values
   * @param xloc X coordinates
   * @param yloc Y coordinates
   * @param zloc Z coordinates
   * @param title the title
   */
  public ThreeDGrid(double[] grid, double[] xloc,
                    double[] yloc, double[] zloc, String title) {
    grid_ = grid;
    xloc_ = xloc;
    yloc_ = yloc;
    zloc_ = zloc;
    title_ = title;
    xTime_ = false;
    yTime_ = false;
    zTime_ = false;
    hasXEdges_ = false;
    hasYEdges_ = false;
    hasZEdges_ = false;
    xRange_ = computeSoTRange(xloc);
    yRange_ = computeSoTRange(yloc);
    zRange_ = computeSoTRange(yloc);
    valRange_ = computeRange2D(grid);
  }
  
  /**
   * Constructor for X time and Y, Z double.
   *
   * @param grid values
   * @param tloc Time coordinates
   * @param yloc Y coordinates
   * @param zloc Z coordinates
   * @param title the title
   */
  public ThreeDGrid(double[] grid, GeoDate[] tloc,
                    double[] yloc, double[] zloc, String title) {
    grid_ = grid;
    tloc_ = tloc;
    yloc_ = yloc;
    zloc_ = zloc;
    title_ = title;
    xTime_ = true;
    yTime_ = false;
    zTime_ = false;
    hasXEdges_ = false;
    hasYEdges_ = false;
    hasZEdges_ = false;
    xRange_ = computeSoTRange(tloc);
    yRange_ = computeSoTRange(yloc);
    zRange_ = computeSoTRange(zloc);
    valRange_ = computeRange2D(grid);
  }
  /**
   * Constructor for X, Z double and Y time.
   *
   * @param grid values
   * @param xloc X coordinates
   * @param xloc Z coordinates
   * @param tloc Time coordinates
   * @param title the title
   */
  public ThreeDGrid(double[] grid, double[] xloc,
                    GeoDate[] tloc,double[] zloc, String title) {
    grid_ = grid;
    xloc_ = xloc;
    tloc_ = tloc;
    zloc_ = zloc;
    title_ = title;
    xTime_ = false;
    yTime_ = true;
    zTime_ = false;
    hasXEdges_ = false;
    hasYEdges_ = false;
    hasZEdges_ = false;
    xRange_ = computeSoTRange(xloc);
    yRange_ = computeSoTRange(tloc);
    zRange_ = computeSoTRange(zloc);
    valRange_ = computeRange2D(grid);
  }
  
  /**
   * Constructor for X double and Y double, and Z time.
   *
   * @param grid Z values
   * @param xloc X coordinates
   * @param tloc Time coordinates
   * @param title the title
   */
  public ThreeDGrid(double[] grid, double[] xloc,
                    double[] yloc, GeoDate[] tloc, String title) {
    grid_ = grid;
    xloc_ = xloc;
    yloc_ = yloc;
    tloc_ = tloc;
    title_ = title;
    xTime_ = false;
    yTime_ = false;
    zTime_ = true;
    hasXEdges_ = false;
    hasYEdges_ = false;
    hasZEdges_ = false;
    xRange_ = computeSoTRange(xloc);
    yRange_ = computeSoTRange(yloc);
    zRange_ = computeSoTRange(tloc);
    valRange_ = computeRange2D(grid);
  }
  
  /**
   * Create a copy of the grid.
   *
   * @since 2.0
   * @see SGTData
   */
  public SGTData copy() {
    SGT3DGrid newGrid;
    try {
      newGrid = (SGT3DGrid)clone();
    } catch (CloneNotSupportedException e) {
      newGrid = new ThreeDGrid();
    }
    return (SGTData)newGrid;
  }
  
  public double[] getXArray() {
    return xloc_;
  }
  
  /**
   * Get the length of the x axis
   *
   * @since 2.0
   */
  public int getXSize() {
    return xloc_.length;
  }
  
  public double[] getYArray() {
    return yloc_;
  }
  /**
   * Get the length of the y axis
   *
   * @since 2.0
   */
   
  public int getYSize() {
    return yloc_.length;
  }
  
  public double[] getZArray() {
    return zloc_;
  }
  
  public int getZSize() {
    return zloc_.length;
  }
  
  public double[] getValArray() {
    return grid_;
  }
  
  public int getValArraySize() {
    return grid_.length;
  }
  
  public GeoDate[] getTimeArray() {
    return tloc_;
  }
  /**
   * Get the length of the Time axis
   *
   * @since 2.0
   */
  public int getTSize() {
    return tloc_.length;
  }
  public boolean isXTime() {
    return xTime_;
  }
  public boolean isYTime() {
    return yTime_;
  }
  public boolean isZTime() {
    return zTime_;
  }
  public void setXTime(boolean flag) {
    xTime_ = flag;
  }
  public void setYTime(boolean flag) {
    yTime_= flag;
  }
  public void setZTime(boolean flag) {
    zTime_= flag;
  }
  public SGTMetaData getXMetaData() {
    return xMetaData_;
  }
  public SGTMetaData getYMetaData() {
    return yMetaData_;
  }
  public SGTMetaData getZMetaData() {
    return zMetaData_;
  }
  public SGTMetaData getValMetaData() {
    return valMetaData_;
  }
  public String getTitle() {
    return title_;
  }
  /**
   * Set the associated data grid.
   * <BR><strong>Property Change:</strong> <code>associatedDataModified</code>.
   *
   * @since 2.0
   */
  public void setAssociatedData(SGTGrid assoc) {
    associatedData_ = assoc;
    changes_.firePropertyChange("associatedDataModified",
                                null,
                                assoc);
  }
  public SGTGrid getAssociatedData() {
    return associatedData_;
  }
  public boolean hasAssociatedData() {
    return (associatedData_ != null);
  }
  public boolean hasXEdges() {
    return hasXEdges_;
  }
  public double[] getXEdges() {
    return xEdges_;
  }
  
  public boolean hasZEdges() {
    return hasZEdges_;
  }
  /**
   * Set the values for the z grid edges.
   */
  public void setZEdges(double[] edge) {
    zEdges_ = edge;
    hasZEdges_ = true;
    zEdgesRange_ = computeSoTRange(edge);
  }
  
  public double[] getZEdges() {
    return zEdges_;
  }
  /**
   * Set the values for the x grid edges.
   */
  public void setXEdges(double[] edge) {
    xEdges_ = edge;
    hasXEdges_ = true;
    xEdgesRange_ = computeSoTRange(edge);
  }
  public boolean hasYEdges() {
    return hasYEdges_;
  }
  public double[] getYEdges() {
    return yEdges_;
  }
  /**
   * Set the values for the y grid edges.
   */
  public void setYEdges(double[] edge) {
    yEdges_ = edge;
    hasYEdges_ = true;
    yEdgesRange_ = computeSoTRange(edge);
  }
  public GeoDate[] getTimeEdges() {
    return tEdges_;
  }
  /**
   * Set the values for the temporal grid edges.
   */
  public void setTimeEdges(GeoDate[] edge) {
    tEdges_ = edge;
    if (xTime_) {
      hasXEdges_ = true;
      xEdgesRange_ = computeSoTRange(edge);
    } 
    else if(yTime_){
      hasYEdges_ = true;
      yEdgesRange_ = computeSoTRange(edge);
    }
    else if(zTime_){
      hasZEdges_ = true;
      zEdgesRange_ = computeSoTRange(edge);
    }
  }
  /**
   * Set the <code>SGTMetaData</code> associated with the x
   * coordinate.
   */
  public void setXMetaData(SGTMetaData md) {
    xMetaData_ = md;
  }
  /**
   * Set the <code>SGTMetaData</code> associated with the y
   * coordinate.
   */
  public void setYMetaData(SGTMetaData md) {
    yMetaData_ = md;
  }
  /**
   * Set the <code>SGTMetaData</code> associated with the z
   * coordinate.
   */
  public void setZMetaData(SGTMetaData md) {
    zMetaData_ = md;
  }
  /**
   * Set the <code>SGTMetaData</code> associated with the z
   * coordinate.
   */
  public void setValMetaData(SGTMetaData md) {
    valMetaData_ = md;
  }
  /**
   * Set the grid title
   */
  public void setTitle(String title) {
    title_ = title;
  }
  public SGLabel getKeyTitle() {
    return keyTitle_;
  }
  /** Set the title formatted for the <code>VectorKey</code>. */
  public void setKeyTitle(SGLabel title) {
    keyTitle_ = title;
  }
  /**
   * Get the unique identifier.  The presence of the identifier
   * is optional, but if it is present it should be unique.  This
   * field is used to search for the layer that contains the data.
   *
   * @since 2.0
   * @return unique identifier
   * @see gov.noaa.pmel.sgt.Pane
   * @see gov.noaa.pmel.sgt.Layer
   */
  public String getId() {
    return id_;
  }
  /**
   * Set the unique identifier.
   */
  public void setId(String ident) {
    id_ = ident;
  }
  /**
   * Set the x coordinate grid centers
   * <BR><strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setXArray(double[] xloc) {
    xloc_ = xloc;
    xTime_ = false;
    xRange_ = computeSoTRange(xloc);
    changes_.firePropertyChange("dataModified",
                                Integer.valueOf(0),
                                Integer.valueOf(xloc.length));
  }
  /**
   * Set the y coordinate grid centers
   * <BR><strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setYArray(double[] yloc) {
    yloc_ = yloc;
    yTime_ = false;
    yRange_ = computeSoTRange(yloc);
    changes_.firePropertyChange("dataModified",
                                Integer.valueOf(0),
                                Integer.valueOf(yloc.length));
  }
  /**
   * Set the z coordinate grid centers
   * <BR><strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setZArray(double[] zloc) {
    zloc_ = zloc;
    zTime_ = false;
    zRange_ = computeSoTRange(zloc);
    changes_.firePropertyChange("dataModified",
                                Integer.valueOf(0),
                                Integer.valueOf(zloc.length));
  }
  /**
   * Set the z grid values.
   * <BR><strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setValArray(double[] grid) {
    grid_ = grid;
    valRange_ = computeRange2D(grid);
    changes_.firePropertyChange("dataModified",
                                Integer.valueOf(0),
                                Integer.valueOf(grid.length));
  }
  /**
   * set the temporal grid centers
   * <BR><strong>Property Change:</strong> <code>dataModified</code>.
   */
  public void setTimeArray(GeoDate[] tloc) {
    tloc_ = tloc;
    if (xTime_) {
      xRange_ = computeSoTRange(tloc);
    } 
    else if(yTime_) {
      yRange_ = computeSoTRange(tloc);
    }
    else if(zTime_) {
      zRange_ = computeSoTRange(tloc);
    }
    changes_.firePropertyChange("dataModified",
                                Integer.valueOf(0),
                                Integer.valueOf(tloc.length));
  }
  public SoTRange getXRange() {
    return xRange_.copy();
  }
  public SoTRange getYRange() {
    return yRange_.copy();
  }
  public SoTRange getZRange() {
    return zRange_.copy();
  }
  public Range2D getValRange() {
    return valRange_;
  }
  /**
   * Return the range of the x edges
   *
   * @since 2.0
   */
  public SoTRange getXEdgesRange() {
    return xEdgesRange_;
  }
  /**
   * Return the range of the y edges
   *
   * @since 2.0
   */
  public SoTRange getYEdgesRange() {
    return yEdgesRange_;
  }
  
  public SoTRange getZEdgesRange() {
    return zEdgesRange_;
  }

  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }

  private SoTRange computeSoTRange(double[] array) {
    Range2D range = computeRange2D(array);
    return new SoTRange.Double(range.start, range.end);
  }
  private SoTRange computeSoTRange(GeoDate[] tarray) {
    long start = Long.MAX_VALUE;
    long end = Long.MIN_VALUE;
    long value;
    int count = 0;
    for(int i=0; i < tarray.length; i++) {
      if(!(tarray[i] == null || tarray[i].isMissing())) {
        value = tarray[i].getTime();
        start = Math.min(start, value);
        end = Math.max(end, value);
        count++;
      }
    }
    if(count == 0) {
      return new SoTRange.GeoDate(new GeoDate(Long.MIN_VALUE),
                                  new GeoDate(Long.MAX_VALUE));
    } else {
      return new SoTRange.GeoDate(new GeoDate(start), new GeoDate(end));
    }
  }
  private Range2D computeRange2D(double[] array) {
    double start = Double.POSITIVE_INFINITY;
    double end = Double.NEGATIVE_INFINITY;
    int count = 0;
    for(int i=0; i < array.length; i++) {
      if(!Double.isNaN(array[i])) {
        start = Math.min(start, array[i]);
        end = Math.max(end, array[i]);
        count++;
      }
    }
    if(count == 0) {
      return new Range2D(Double.NaN, Double.NaN);
    } else {
      return new Range2D(start, end);
    }
  }
}
