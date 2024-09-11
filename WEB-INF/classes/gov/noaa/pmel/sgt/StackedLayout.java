/*
 * $Id: StackedLayout.java,v 1.5 2003/08/22 23:02:32 dwd Exp $
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

import gov.noaa.pmel.sgt.beans.Panel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * <code>StackedLayout</code> works with <code>Pane</code> to position multiple <code>Layer</code>s
 * directly over each other.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/08/22 23:02:32 $
 * @since 1.0
 * @see Pane#setLayout
 * @see Layer
 */
public class StackedLayout implements LayoutManager {
  @Override
  public Dimension preferredLayoutSize(Container parent) {
    synchronized (parent.getTreeLock()) {
      return parent.getSize();
    }
  }

  @Override
  public Dimension minimumLayoutSize(Container parent) {
    synchronized (parent.getTreeLock()) {
      return parent.getSize();
    }
  }

  @Override
  public void layoutContainer(Container parent) {
    synchronized (parent.getTreeLock()) {
      JPane pane = null;
      boolean batch = false;
      if (parent instanceof JPane) {
        pane = (JPane) parent;
        batch = pane.isBatch();
        pane.setBatch(true, "StackedLayout");
      } else if (parent instanceof Panel) {
        pane = ((Panel) parent).getPane();
        batch = pane.isBatch();
        pane.setBatch(true, "StackedLayout");
      }
      Insets insets = parent.getInsets();
      Rectangle rect = parent.getBounds();
      int ncomponents = parent.getComponentCount();
      int x, y, w, h;
      x = rect.x + insets.left;
      y = rect.y + insets.top;
      w = rect.width - (insets.left + insets.right);
      h = rect.height - (insets.top + insets.bottom);
      for (int i = 0; i < ncomponents; i++) {
        parent.getComponent(i).setBounds(x, y, w, h);
      }
      if (!batch) pane.setBatch(false, "StackedLayout");
    }
  }

  @Override
  public void removeLayoutComponent(Component comp) {}

  @Override
  public void addLayoutComponent(String name, Component comp) {}
}
