/*
 * $Id: LayerContainer.java,v 1.6 2003/09/15 22:05:41 dwd Exp $
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

import java.awt.*;
import java.beans.*;

/**
 * A <code>Container</code> designed hold <code>Layer</code>s. The <code>LayerContainer</code>
 * improves the flexiblity in laying out multiple stacked <code>Layer</code>s on a <code>Pane</code>
 * .
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2003/09/15 22:05:41 $
 * @since 1.0
 * @see StackedLayout
 * @see Pane
 * @see Layer
 */
public class LayerContainer extends java.awt.Container implements LayerControl {
  AbstractPane pane_;

  public LayerContainer() {
    super();
  }

  /** Used internally by sgt. */
  @Override
  public void setPane(AbstractPane pane) {
    pane_ = pane;
  }

  /** Used internally by sgt. */
  @Override
  public void draw(Graphics g) throws PaneNotFoundException {
    throw new MethodNotImplementedError();
  }

  /**
   * Used internally by sgt.
   *
   * @since 2.0
   */
  @Override
  public void drawDraggableItems(Graphics g) throws PaneNotFoundException {
    /**
     * @todo Implement this gov.noaa.pmel.sgt.LayerControl method
     */
    throw new UnsupportedOperationException("Method drawDraggableItems() not yet implemented.");
  }

  /**
   * Get identifier.
   *
   * @return identifier/name
   */
  @Override
  public String getId() {
    return getName();
  }
}
