/*
 * $Id: Transform.java,v 1.5 2002/06/14 17:12:25 dwd Exp $
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
import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * <code>Transform</code> defines an interface for transformations between user and physical
 * coordinates.
 *
 * @see AxisTransform
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2002/06/14 17:12:25 $
 * @since 1.0
 */
public interface Transform extends Serializable {

  public void releaseResources() throws Exception;

  /**
   * Set physical coordinate range.
   *
   * @param p1 minimum value, physical coordinates
   * @param p2 maximum value, physical coordinates
   * @see LinearTransform
   */
  public void setRangeP(double p1, double p2);

  /**
   * Set physical coordinate range.
   *
   * @param prange physcial coordinate range
   * @see Range2D
   * @see LinearTransform
   */
  public void setRangeP(Range2D prange);

  /**
   * Get the physical coordinate range.
   *
   * @return physcial coordinate range
   * @see Range2D
   */
  public Range2D getRangeP();

  /**
   * Set the user coordinate range for double values.
   *
   * @param u1 minimum value, user coordinates
   * @param u2 maximum value, user coordinates
   * @see LinearTransform
   */
  public void setRangeU(double u1, double u2);

  /**
   * Set the user coordinate range for double values.
   *
   * @param urange user coordinate range
   * @see Range2D
   * @see LinearTransform
   */
  public void setRangeU(Range2D urange);

  /**
   * Get the user coordinate range for double values.
   *
   * @return user range
   * @see Range2D
   */
  public Range2D getRangeU();

  /**
   * Transform from user to physical coordinates.
   *
   * @param u user value
   * @return physical value
   */
  abstract double getTransP(double u);

  /**
   * Transform from physical to user coordinates.
   *
   * @param p physical value
   * @return user value
   */
  abstract double getTransU(double p);

  /**
   * Add listener for changes to transform properties.
   *
   * @since 2.0
   */
  public void addPropertyChangeListener(PropertyChangeListener listener);

  /**
   * Remove listener.
   *
   * @since 2.0
   */
  public void removePropertyChangeListener(PropertyChangeListener listener);
}
