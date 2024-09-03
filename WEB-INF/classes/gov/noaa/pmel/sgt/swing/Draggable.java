/*
 * $Id: Draggable.java,v 1.7 2002/06/20 16:33:21 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.swing;

import java.awt.Point;

/**
 * <code>Draggable</code> defines an interface to allow classes to be imaged separately in a <code>
 * Layer</code> from other classes. The interface is sufficient to allow dragging in a <code>
 * JLayeredPane</code> (<code>JPane</code>).
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2002/06/20 16:33:21 $
 * @since 2.0
 */
public interface Draggable {
  /**
   * Set the location of the <code>Draggable</code> object. Change in location will not be vetoed.
   */
  public void setLocationNoVeto(int x, int y);

  /** Set the location of the <code>Draggable</code> object. */
  public void setLocation(Point loc);

  /**
   * Set the location of the <code>Draggable</code> object and optionally don't fire a <code>
   * PropertyChangeEvent</code>
   *
   * @since 3.0
   */
  public void setLocation(Point loc, boolean fireEvent);
}
