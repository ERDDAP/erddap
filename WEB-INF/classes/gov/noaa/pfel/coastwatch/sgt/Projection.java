/*
 * Projection Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.DoubleObject;

/**
 * Projections convert mapX,mapY (think: longitude, latitude) to/from deviceX,deviceY (think:
 * Graphics2D, isotropic, 0,0 at upper left). This defines the methods that every Projection must
 * provide.
 */
public interface Projection {

  /**
   * This converts graphX,graphY (think: longitude, latitude) to deviceX,deviceY (think: Graphics2D,
   * isotropic, 0,0 at upper left). This does not do any clipping; resulting values may be outside
   * of the graph's range.
   *
   * <p>double precision comes in handy sometimes, so it is used for graph and device coordinates.
   *
   * @param graphX e.g., longitude
   * @param graphY e.g., latitude
   * @param deviceX the corresponding (e.g., Graphics2D) x device coordinate which is set by this
   *     method
   * @param deviceY the corresponding (e.g., Graphics2D) y device coordinate which is set by this
   *     method
   */
  public void graphToDevice(
      double graphX, double graphY, DoubleObject deviceX, DoubleObject deviceY);

  /**
   * This converts deviceX,deviceY (think: Graphics2D, isotropic, 0,0 at upper left) to
   * graphX,graphY (think: longitude, latitude). This does not presume any clipping; values may be
   * outside of the graph's range.
   *
   * <p>double precision comes in handy sometimes, so it is used for graph and device coordinates.
   *
   * @param deviceX the corresponding (e.g., Graphics2D) x device coordinate
   * @param deviceY the corresponding (e.g., Graphics2D) y device coordinate
   * @param graphX e.g., longitude, which will be set by this method
   * @param graphY e.g., latitude, which will be set by this method
   */
  public void deviceToGraph(
      double deviceX, double deviceY, DoubleObject graphX, DoubleObject graphY);
}
