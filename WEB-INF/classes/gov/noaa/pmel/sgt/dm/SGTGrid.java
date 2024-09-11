/*
 * $Id: SGTGrid.java,v 1.10 2003/08/22 23:02:38 dwd Exp $
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

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.GeoDateArray;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;

/**
 * Defines a data object to be of Grid type. Interpretation of X and Y is determined by the <code>
 * CoordinateSystem</code>. For <code>Cartesian</code>, X and Y are the Cartesian coordinates. For
 * <code>Polar</code>, X and Y are R (radius) and Theta (angle), respectively.
 *
 * <p>The <code>SGTGrid</code> interface only specifies the methods required to access information.
 * The methods used to construct an object that implements <code>SGTGrid</code> is left to the
 * developer.
 *
 * @author Donald Denbo
 * @version $Revision: 1.10 $, $Date: 2003/08/22 23:02:38 $
 * @since 1.0
 * @see SGTData
 * @see CoordinateSystem
 * @see Cartesian
 * @see Polar
 * @see SimpleGrid
 */
public interface SGTGrid extends SGTData {
  /** Get the array of X values. */
  public double[] getXArray();

  /** Get the length of X value array. */
  public int getXSize();

  /** Get the array of Y values. */
  public double[] getYArray();

  /** Get the length of Y value array. */
  public int getYSize();

  /** Get the array of Z values. */
  public double[] getZArray();

  /** Get the range of Z values. */
  public Range2D getZRange();

  /** Get the array of temporal values. */
  public GeoDate[] getTimeArray();

  /**
   * Get the <code>GeoDateArray</code> object.
   *
   * @since 3.0
   */
  public GeoDateArray getGeoDateArray();

  /** Get the length of temporal value array. */
  public int getTSize();

  /** Get the Z SGTMetaData. */
  public SGTMetaData getZMetaData();

  /** Get the associated data. The associated data must be of the same type (SGTGrid) and shape. */
  public SGTGrid getAssociatedData();

  /** Is there associated data available? */
  public boolean hasAssociatedData();

  /** Are X edges available? */
  public boolean hasXEdges();

  /** Get the X coordinate edges. The XEdge length will be one greater than the XArray length. */
  public double[] getXEdges();

  /** Get the range of X coordinate edges. */
  public SoTRange getXEdgesRange();

  /** Are Y edges available? */
  public boolean hasYEdges();

  /** Get the Y coordinate edges. The YEdge length will be one greater than the YArray length. */
  public double[] getYEdges();

  /** Get the range of Y coordinate edges. */
  public SoTRange getYEdgesRange();

  /** Get the Time edges. The TimeEdge length will be one greater than the TimeArray length. */
  public GeoDate[] getTimeEdges();

  /**
   * Get the <code>GeoDateArray</code> object.
   *
   * @since 3.0
   */
  public GeoDateArray getGeoDateArrayEdges();
}
