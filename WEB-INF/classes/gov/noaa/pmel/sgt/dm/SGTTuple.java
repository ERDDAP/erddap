/*
 * $Id: SGTTuple.java,v 1.4 2003/08/22 23:02:38 dwd Exp $
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

/**
 * Provides access to tuple organized data of either 2 or 3 dimensions.
 * All arrays that are provided must be of equal length.
 * Tuples can be used to provide un-structured 3-D data that can then
 * be trianglulated to enable area fill or contouring.  3-d tuples
 * are also useful in the construction of vectors.
 * @since 2.x
 */
public interface SGTTuple extends SGTData {
    public double[] getXArray();
    public double[] getYArray();
    public double[] getZArray();
    public int getSize();
    public GeoDate[] getTimeArray();
  /**
   * Get the <code>GeoDateArray</code> object.
   */
    public GeoDateArray getGeoDateArray();
    public double[] getAssociatedData();
    public boolean hasAssociatedData();
    public SGTMetaData getZMetaData();
    public Range2D getZRange();
}
