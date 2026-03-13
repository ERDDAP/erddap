/*
 * $Id: AbstractPane.java,v 1.24 2003/09/18 21:21:33 dwd Exp $
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

import java.awt.Graphics;

/**
 * Defines the basic sgt Pane functionality. <code>Pane</code> and <code>JPane</code> implement the
 * <code>AbstractPane</code> interface.
 *
 * @author Donald Denbo
 * @version $Revision: 1.24 $, $Date: 2003/09/18 21:21:33 $
 * @since 2.0
 * @see Pane
 * @see JPane
 */
public interface AbstractPane {

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception;

  /**
   * The <code>AbstractPane</code> and all of the attached Classes will be drawn. Drawing will occur
   * using the supplied <code>Graphics</code> object.
   *
   * @param g User supplied <code>Graphics</code> object
   * @see java.awt.Graphics
   */
  public void draw(Graphics g);

  /**
   * Get the bounding rectangle in pixels (device units).
   *
   * @return Rectangle object containing the bounding box for the pane.
   */
  public java.awt.Rectangle getBounds();

  /** Get the <code>Component</code> associated with the pane. */
  public java.awt.Component getComponent();

  /*
   * methods to handle ChangeEvent and PropertyChangeEvent's
   */
  /**
   * Turn on/off batching of updates to the pane. While batching is <code>true</code> property
   * change events will <strong>not</strong> cause pane to redraw. When batching is turned back on
   * if the pane has been modified it will then redraw.
   */
  public void setBatch(boolean batch, String msg);

  /** Is batching turned on? */
  public boolean isBatch();

  /**
   * Notify the pane that something has changed and a redraw is required. Used internally by sgt.
   */
  public void setModified(boolean mod, String mess);
}
