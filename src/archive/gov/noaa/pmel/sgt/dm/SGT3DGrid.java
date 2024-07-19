/*
 * $Id: SGT3DGrid.java,v 1.2 2003/08/22 22:08:16 dwd Exp $
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
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
 
/**
 * Defines a data object to be of Grid type. Interpretation 
 * of X and Y is determined by the <code>CoordinateSystem</code>.  For
 * <code>Cartesian</code>, X and Y are the Cartesian coordinates. For
 * <code>Polar</code>, X and Y are R (radius) and Theta (angle), respectively.
 *
 * The <code>SGTGrid</code> interface only specifies the methods required
 * to access information. The methods used to construct an
 * object that implements <code>SGTGrid</code> is left to the developer.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 22:08:16 $
 * @since 1.0
 * @see SGTData
 * @see CoordinateSystem
 * @see Cartesian
 * @see Polar
 * @see SimpleGrid
 */
public interface SGT3DGrid extends SGTData {
	// additions to SGTData stuff

  /**
   * Returns true if the Z coordinate is Time.
   */
  public boolean isZTime();

  /**
   * Get the length of Z value array.
   */
  public int getZSize();
  /**
   * Get the range of measured values on this 3D grid.
   */
  public Range2D getValRange();
  /**
   * Get the array of temporal values.
   */
  /**
   * Get the Value SGTMetaData.
   */
  public SGTMetaData getValMetaData();
  /**
   * Z edges available?
   */
  public boolean hasZEdges();
  /**
   * Get the Y coordinate edges. The YEdge length will
   * be one greater than the YArray length.
   */
  public double[] getZEdges();
  /**
   * Get the range of Y coordinate edges.
   */
  public SoTRange getZEdgesRange();
}
