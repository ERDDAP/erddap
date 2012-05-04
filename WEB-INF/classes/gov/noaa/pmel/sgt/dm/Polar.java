/*
 * $Id: Polar.java,v 1.2 2001/02/06 20:05:51 dwd Exp $
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
 * The <code>Polar</code> interface indicates to the <code>sgt</code> graphics
 * toolkit that the X and Y coordinates are to be
 * interpreted as R (radius) and Theta (angle), 
 * respectively.  Neither X nor Y can be a time axis 
 * when using the <code>Polar</code> interface.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2001/02/06 20:05:51 $
 * @since 1.0
 * @see CoordinateSystem
 */
public interface Polar extends CoordinateSystem {
}
