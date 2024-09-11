/*
 * $Id: LayerChild.java,v 1.6 2001/01/31 23:41:04 dwd Exp $
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
 * <code>LayerChild</code> defines an interface to allow classes to be associated with a <code>Layer
 * </code>. The interface is sufficient to provide scaling, translation, and mouse selection.
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2001/01/31 23:41:04 $
 * @since 1.0
 */
public interface LayerChild extends Selectable {

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception;

  /**
   * Draw the <code>LayerChild</code>.
   *
   * @param g Graphics context
   * @exception LayerNotFoundException No layer is associated with the <code>LayerChild</code>.
   */
  public void draw(Graphics g) throws LayerNotFoundException;

  /**
   * Get the associated <code>Layer</code>.
   *
   * @return Associated layer
   */
  public Layer getLayer();

  /**
   * Associate a <code>Layer</code> with the <code>LayerChild</code>.
   *
   * @param l Parent layer.
   */
  public void setLayer(Layer l);

  /**
   * Get the identifier.
   *
   * @return <code>LayerChild</code> identification.
   */
  public String getId();

  /**
   * Set the identifier.
   *
   * @param id <code>LayerChild</code> identification.
   */
  public void setId(String id);

  /**
   * Create a copy of the <code>LayerChild</code>.
   *
   * @return A copy of the <code>LayerChild</code>.
   */
  public LayerChild copy();

  /**
   * Return a string that represents the <code>LayerChild</code>.
   *
   * @return Stringified <code>LayerChild</code> representation.
   */
  @Override
  public String toString();

  /**
   * Check if <code>LayerChild</code> is visible.
   *
   * @since 2.0
   * @return true if visible
   */
  public boolean isVisible();

  /**
   * Set visibility for a <code>LayerChild</code>.
   *
   * @since 2.0
   * @param visible visible if true
   */
  public void setVisible(boolean visible);

  /**
   * Get <code>AbstractPane</code> of the <code>LayerChild</code>.
   *
   * @since 2.0
   */
  public AbstractPane getPane();

  /**
   * Used by sgt internally.
   *
   * @since 2.0
   */
  public void modified(String mess);
}
