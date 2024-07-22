/*
 * $Id: Attribute.java,v 1.5 2002/06/12 18:47:26 dwd Exp $
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

import java.beans.PropertyChangeListener;

/**
 * Defines an interface for classes that provide rendering information for <code>sgt.dm</code>
 * classes.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2002/06/12 18:47:26 $
 * @since 1.0
 */
public interface Attribute extends java.io.Serializable {

  /**
   * Return a printable representation of the object.
   *
   * @return Description
   */
  @Override
  public String toString();

  /**
   * Add listener for changes to attribute properties. The properties that can be listened for
   * depend on the implementing class.
   *
   * @since 2.0
   */
  public void addPropertyChangeListener(PropertyChangeListener listener);

  /**
   * Remove change listener.
   *
   * @since 2.0
   */
  public void removePropertyChangeListener(PropertyChangeListener listener);

  /**
   * Set attribute id.
   *
   * @since 3.0
   */
  public void setId(String id);

  /**
   * Get attribute id.
   *
   * @since 3.0
   */
  public String getId();

  /**
   * Batch the changes to the attribute.
   *
   * @since 3.0
   */
  public void setBatch(boolean batch);

  /**
   * Batch the changes to the attribute and set local flag. Determines whether <code>
   * AttributeChangeEvent</code> will be set local.
   *
   * @since 3.0
   */
  public void setBatch(boolean batch, boolean local);

  /**
   * Is the attribute in batch mode?
   *
   * @since 3.0
   */
  public boolean isBatch();
}
