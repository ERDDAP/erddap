/*
 * CartesianProjection Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.DoubleObject;

/**
 * This projection converts a Cartesian graph's x,y coordinates to/from deviceX,deviceY (think:
 * Graphics2D, isotropic, 0,0 at upper left).
 */
public class CartesianProjection implements Projection {

  private double graphMinX,
      graphMaxX,
      graphMinY,
      graphMaxY,
      lgraphMinX,
      lgraphMaxX,
      lgraphMinY,
      lgraphMaxY, // log or linear applied
      deviceMinX,
      deviceMaxX,
      deviceMinY,
      deviceMaxY,
      lgraphXRange,
      lgraphYRange,
      deviceXRange,
      deviceYRange,
      deviceOverLGraphXRange,
      deviceOverLGraphYRange;
  private boolean xIsLogAxis, yIsLogAxis;
  public static final String cantBecauseLog =
      "CartesianProjection.graphToDeviceX/YDistance() can't be used when the x and/or y axis is a log axis.";

  /**
   * This sets up a projection which converts a Cartesian graph's x,y coordinates to/from
   * deviceX,deviceY (think: Graphics2D, isotropic, 0,0 at upper left).
   *
   * @param graphMinX the leftmost X value on the graph (usually min, but not always)
   * @param graphMaxX the rightmost X value on the graph (usually max, but not always)
   * @param graphMinY the lowest Y value on the graph (usually min, but not always)
   * @param graphMaxY the highest Y value on the graph (usually max, but not always)
   * @param deviceMinX the device X corresponding to graphMinX
   * @param deviceMaxX the device X corresponding to graphMaxX
   * @param deviceMinY the device Y corresponding to graphMinY
   * @param deviceMaxY the device Y corresponding to graphMaxY
   */
  public CartesianProjection(
      double graphMinX,
      double graphMaxX,
      double graphMinY,
      double graphMaxY,
      double deviceMinX,
      double deviceMaxX,
      double deviceMinY,
      double deviceMaxY,
      boolean xIsLogAxis,
      boolean yIsLogAxis) {

    this.graphMinX = graphMinX;
    this.graphMaxX = graphMaxX;
    this.graphMinY = graphMinY;
    this.graphMaxY = graphMaxY;
    this.deviceMinX = deviceMinX;
    this.deviceMaxX = deviceMaxX;
    this.deviceMinY = deviceMinY;
    this.deviceMaxY = deviceMaxY;

    this.xIsLogAxis =
        xIsLogAxis && graphMinX > 0 && graphMaxX > 0; // silently turn off logAxis if not allowed
    this.yIsLogAxis = yIsLogAxis && graphMinY > 0 && graphMaxY > 0;
    lgraphMinX = xIsLogAxis ? Math.log(graphMinX) : graphMinX;
    lgraphMaxX = xIsLogAxis ? Math.log(graphMaxX) : graphMaxX;
    lgraphMinY = yIsLogAxis ? Math.log(graphMinY) : graphMinY;
    lgraphMaxY = yIsLogAxis ? Math.log(graphMaxY) : graphMaxY;

    lgraphXRange = lgraphMaxX - lgraphMinX;
    lgraphYRange = lgraphMaxY - lgraphMinY;
    deviceXRange = deviceMaxX - deviceMinX;
    deviceYRange = deviceMaxY - deviceMinY;
    deviceOverLGraphXRange = deviceXRange / lgraphXRange;
    deviceOverLGraphYRange = deviceYRange / lgraphYRange;
  }

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
  @Override
  public void graphToDevice(
      double graphX, double graphY, DoubleObject deviceX, DoubleObject deviceY) {
    if ((xIsLogAxis && graphX <= 0) || (yIsLogAxis && graphY <= 0)) {
      // not an exception.
      deviceX.d = Double.NaN;
      deviceY.d = Double.NaN;
      return;
    }

    if (xIsLogAxis) graphX = Math.log(graphX);
    if (yIsLogAxis) graphY = Math.log(graphY);
    deviceX.d =
        deviceMinX
            + (graphX - lgraphMinX)
                * deviceOverLGraphXRange; // doing like this minimizes roundoff errors
    deviceY.d = deviceMinY + (graphY - lgraphMinY) * deviceOverLGraphYRange;
  }

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
  @Override
  public void deviceToGraph(
      double deviceX, double deviceY, DoubleObject graphX, DoubleObject graphY) {
    graphX.d =
        lgraphMinX
            + (deviceX - deviceMinX)
                / deviceOverLGraphXRange; // doing like this minimizes roundoff errors
    graphY.d = lgraphMinY + (deviceY - deviceMinY) / deviceOverLGraphYRange;
    if (xIsLogAxis) graphX.d = Math.exp(graphX.d);
    if (yIsLogAxis) graphY.d = Math.exp(graphY.d);
  }

  /**
   * This converts a graphXDistance into a deviceXDistance. THIS DOESN'T WORK WITH xIsLogAxis!
   *
   * @param graphXDistance
   * @return deviceXDistance
   */
  public double graphToDeviceXDistance(double graphXDistance) {
    if (xIsLogAxis) throw new RuntimeException(cantBecauseLog);
    return graphXDistance * deviceOverLGraphXRange;
  }

  /**
   * This converts a graphYDistance into a deviceYDistance. In the typical setup, a positive
   * graphYDistance generates a positive deviceYDistance. THIS DOESN'T WORK WITH yIsLogAxis!
   *
   * @param graphYDistance
   * @return deviceYDistance
   */
  public double graphToDeviceYDistance(double graphYDistance) {
    if (yIsLogAxis) throw new RuntimeException(cantBecauseLog);
    return graphYDistance * -deviceOverLGraphYRange;
  }

  /**
   * This prints a string representation of this class.
   *
   * @return a string representation of this class.
   */
  @Override
  public String toString() {
    return "CartesianProjection("
        + "graphMinX="
        + graphMinX
        + " maxX="
        + graphMaxX
        + " xIsLogAxis="
        + xIsLogAxis
        + " minY="
        + graphMinY
        + " maxY="
        + graphMaxY
        + " yIsLogAxis="
        + yIsLogAxis
        + "\n  deviceMinX="
        + deviceMinX
        + " maxX="
        + deviceMaxX
        + " minY="
        + deviceMinY
        + " maxY="
        + deviceMaxY
        + ")";
  }
}
