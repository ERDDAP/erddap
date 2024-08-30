/*
 * $Id: SGTLine.java,v 1.7 2003/08/22 23:02:38 dwd Exp $
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

/**
 * Defines a data object to be of Line type. Interpretation of X and Y is determined by the <code>
 * CoordinateSystem</code>. For <code>Cartesian</code>, X and Y are the Cartesian coordinates. For
 * <code>Polar</code>, X and Y are R (radius) and Theta (angle), respectively.
 *
 * <p>The <code>SGTLine</code> interfaces only specifies the methods required to access information.
 * The methods used to construct an object that implements <code>SGTLine</code> is left to the
 * developer.
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/08/22 23:02:38 $
 * @since 1.0
 * @see SGTData
 * @see CoordinateSystem
 * @see Cartesian
 * @see Polar
 * @see SimpleLine
 */
public interface SGTLine extends SGTData {
  /** Get the array of X values. */
  public double[] getXArray();

  /** Get the array of Y values. */
  public double[] getYArray();

  /** Get the array of Time values. */
  public GeoDate[] getTimeArray();

  /**
   * Get the <code>GeoDateArray</code> object.
   *
   * @since 3.0
   */
  public GeoDateArray getGeoDateArray();

  /**
   * Get the associated data. The associated data must be of the same type (SGTLine) and length. The
   * Y array will be used.
   */
  public SGTLine getAssociatedData();

  /** Is there associated data available? */
  public boolean hasAssociatedData();
}
