/*
 * $Id: Selectable.java,v 1.3 2001/01/31 23:41:05 dwd Exp $
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

import java.awt.Rectangle;

/**
 * Interface indicates that object can be selected with a mouse click. To be moveable the object
 * must implement the <code>Moveable</code> interface.
 *
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2001/01/31 23:41:05 $
 * @since 1.0
 * @see Moveable
 */
public interface Selectable {
  /**
   * Sets the selected property.
   *
   * @param sel true if selected, false if not.
   */
  public void setSelected(boolean sel);

  /**
   * Returns true if the object's selected property is set.
   *
   * @return true if selected, false if not.
   */
  public boolean isSelected();

  /**
   * Gets the bounding rectangle in device coordinates.
   *
   * @return bounding rectangle
   */
  public Rectangle getBounds();

  /**
   * Returns true if the current state is selectable.
   *
   * @return true if selectable
   */
  public boolean isSelectable();

  /**
   * Set the Selectable property.
   *
   * @param select if true object is selectable
   */
  public void setSelectable(boolean select);
  /**
   * Change the selected objects bounding rectangle in device coordinates. The object will move to
   * the new bounding rectangle.
   *
   * @param bnds new bounding rectangle
   */
}
