/*
 * $Id: LayerStack.java,v 1.8 2003/09/15 22:05:41 dwd Exp $
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

import java.awt.Container;
import java.awt.Graphics;

/**
 * <code>LayerStack</code> is used to manage a group of layers together.
 *
 * @since 2.x
 */
public class LayerStack extends Container implements LayerControl {
  public LayerStack() {
    setLayout(new StackedLayout());
  }

  @Override
  public void setPane(AbstractPane pane) {
    /**
     * @todo Implement this gov.noaa.pmel.sgt.LayerControl method
     */
    throw new UnsupportedOperationException("Method setPane() not yet implemented.");
  }

  @Override
  public void draw(Graphics g) {
    /**
     * @todo Implement this gov.noaa.pmel.sgt.LayerControl method
     */
    throw new UnsupportedOperationException("Method draw() not yet implemented.");
  }

  @Override
  public void drawDraggableItems(Graphics g) {
    /**
     * @todo Implement this gov.noaa.pmel.sgt.LayerControl method
     */
    throw new UnsupportedOperationException("Method drawDraggableItems() not yet implemented.");
  }

  @Override
  public String getId() {
    return getName();
  }
}
