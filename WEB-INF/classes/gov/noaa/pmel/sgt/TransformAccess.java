/*
 * $Id: TransformAccess.java,v 1.3 2001/02/01 17:22:32 dwd Exp $
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

import gov.noaa.pmel.util.Range2D;

/**
 * Interface indicates that the color map uses a transform to map levels.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2001/02/01 17:22:32 $
 * @since 2.0
 */
public interface TransformAccess {
  void setRange(Range2D range);
}
