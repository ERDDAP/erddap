/* 
 * CartesianProjection Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.DoubleObject;
import com.cohort.util.String2;
import com.cohort.util.Test;

/**
 * This projection converts a Cartesian graph's x,y coordinates 
 * to/from deviceX,deviceY (think: Graphics2D, isotropic, 0,0 at upper left).
 */
public class CartesianProjection implements Projection {

    private double graphMinX, graphMaxX, graphMinY, graphMaxY,
           deviceMinX, deviceMaxX, deviceMinY, deviceMaxY,
        graphXRange, graphYRange, deviceXRange, deviceYRange,
        deviceOverGraphXRange, deviceOverGraphYRange;

    /**
     * This sets up a projection which converts a Cartesian graph's x,y coordinates 
     * to/from deviceX,deviceY (think: Graphics2D, isotropic, 0,0 at upper left).
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
        double graphMinX, double graphMaxX,
        double graphMinY, double graphMaxY,
        double deviceMinX, double deviceMaxX,
        double deviceMinY, double deviceMaxY) {

        this.graphMinX = graphMinX;
        this.graphMaxX = graphMaxX;
        this.graphMinY = graphMinY;
        this.graphMaxY = graphMaxY;
        this.deviceMinX = deviceMinX;
        this.deviceMaxX = deviceMaxX;
        this.deviceMinY = deviceMinY;
        this.deviceMaxY = deviceMaxY;
        graphXRange = graphMaxX - graphMinX;
        graphYRange = graphMaxY - graphMinY;
        deviceXRange = deviceMaxX - deviceMinX;
        deviceYRange = deviceMaxY - deviceMinY;
        deviceOverGraphXRange = deviceXRange / graphXRange;
        deviceOverGraphYRange = deviceYRange / graphYRange;

    }



    /**
     * This converts graphX,graphY (think: longitude, latitude) 
     * to deviceX,deviceY (think: Graphics2D, isotropic, 0,0 at upper left).
     * This does not do any clipping; resulting values may be outside of the
     * graph's range.
     *
     * <p>double precision comes in handy sometimes, so it is used 
     * for graph and device coordinates.
     * 
     * @param graphX  e.g., longitude
     * @param graphY  e.g., latitude
     * @param deviceX  the corresponding (e.g., Graphics2D) x device coordinate
     *    which is set by this method
     * @param deviceY  the corresponding (e.g., Graphics2D) y device coordinate
     *    which is set by this method
     */
    public void graphToDevice(double graphX, double graphY, DoubleObject deviceX,
        DoubleObject deviceY) {
        deviceX.d = deviceMinX + (graphX - graphMinX) * deviceOverGraphXRange; //doing like this minimizes roundoff errors
        deviceY.d = deviceMinY + (graphY - graphMinY) * deviceOverGraphYRange;
    }
    
    /**
     * This converts deviceX,deviceY (think: Graphics2D, isotropic, 0,0 at upper left)
     * to graphX,graphY (think: longitude, latitude).
     * This does not presume any clipping; values may be outside of the graph's range.
     *
     * <p>double precision comes in handy sometimes, so it is used 
     * for graph and device coordinates.
     *
     * @param deviceX  the corresponding (e.g., Graphics2D) x device coordinate
     * @param deviceY  the corresponding (e.g., Graphics2D) y device coordinate
     * @param graphX  e.g., longitude, which will be set by this method
     * @param graphY  e.g., latitude, which will be set by this method
     */
    public void deviceToGraph(double deviceX, double deviceY, DoubleObject graphX,
        DoubleObject graphY) {
        graphX.d = graphMinX + (deviceX - deviceMinX) / deviceOverGraphXRange; //doing like this minimizes roundoff errors
        graphY.d = graphMinY + (deviceY - deviceMinY) / deviceOverGraphYRange;
    }

    /** 
     * This converts a graphXDistance into a deviceXDistance.
     * 
     * @param graphXDistance
     * @return deviceXDistance
     */ 
    public double graphToDeviceXDistance(double graphXDistance) {
         return graphXDistance * deviceOverGraphXRange;
     }

    /** 
     * This converts a graphYDistance into a deviceYDistance.
     * In the typical setup, a positive graphYDistance generates a positive deviceYDistance.
     *
     * @param graphYDistance
     * @return deviceYDistance
     */ 
    public double graphToDeviceYDistance(double graphYDistance) {
         return graphYDistance * -deviceOverGraphYRange;
     }

    /**
     * This prints a string representation of this class.
     *
     * @return a string representation of this class.
     */
    public String toString() {
        return "CartesianProjection(" + 
            "graphMinX=" + graphMinX + " maxX=" + graphMaxX + " minY=" + graphMinY + " maxY=" + graphMaxY +
            "\n  deviceMinX=" + deviceMinX + " maxX=" + deviceMaxX + " minY=" + deviceMinY + " maxY=" + deviceMaxY + ")";
    }

    /** This tests this class. */
    public static void test() {
        CartesianProjection cp = new CartesianProjection(100, 200, 10, 20, 30, 50, 120, 80);
        String2.log("\nTest CartesianProjection\n" + cp);
        DoubleObject dox = new DoubleObject(0);
        DoubleObject doy = new DoubleObject(0);
        cp.graphToDevice(100, 10, dox, doy);
        Test.ensureEqual(dox.d, 30, "");
        Test.ensureEqual(doy.d, 120, "");
        cp.graphToDevice(200, 20, dox, doy);
        Test.ensureEqual(dox.d, 50, "");
        Test.ensureEqual(doy.d, 80, "");
        cp.graphToDevice(125, 10, dox, doy);
        Test.ensureEqual(dox.d, 35, "");
        Test.ensureEqual(doy.d, 120, "");
        cp.graphToDevice(100, 12.5, dox, doy);
        Test.ensureEqual(dox.d, 30, "");
        Test.ensureEqual(doy.d, 110, "");

        Test.ensureEqual(cp.graphToDeviceXDistance(25), 5, "");
        Test.ensureEqual(cp.graphToDeviceYDistance(2.5), 10, "");
    }


}
