/*
 * $Id: ContourLevelNotFoundException.java,v 1.1.1.1 1999/12/21 19:23:55 dwd Exp $
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
 * Contour level does not exist.
 *
 * @author Donald Denbo
 * @version $Revision: 1.1.1.1 $, $Date: 1999/12/21 19:23:55 $
 */
public class ContourLevelNotFoundException extends SGException {
  public ContourLevelNotFoundException() {
    super();
  }

  public ContourLevelNotFoundException(String s) {
    super(s);
  }
}
