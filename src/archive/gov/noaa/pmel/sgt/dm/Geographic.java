/*
 * $Id: Geographic.java,v 1.2 2001/02/06 20:05:51 dwd Exp $
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
 
/**
 * The <code>Geographic</code> interface indicates to the
 * <code>sgt</code> classes that
 * X and Y coordinates are to be interpreted as longitude
 * and latitude, respectively. The <code>Geographic</code> interface does not
 * support X or Y being a time coordinate.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2001/02/06 20:05:51 $
 * @since 1.0
 */
public interface Geographic extends CoordinateSystem {
}
