/*
 * $Id: ContourLevelsAccess.java,v 1.2 2001/01/31 23:41:03 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt;

/**
 * Defines the functionality for accessing contour level information for color maps.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2001/01/31 23:41:03 $
 * @since 2.0
 */
public interface ContourLevelsAccess {

  ContourLevels getContourLevels();

  void setContourLevels(ContourLevels contourLevels);
}
