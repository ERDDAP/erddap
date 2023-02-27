/* 
 * PointDataSetNone Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/** 
 * This class represents the "(None)" point dataset for CWBrowser.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-05-01
 */
public class PointDataSetNone extends PointDataSet { 

    public PointDataSetNone() {
        option = "(None)"; 
        tooltip = "Select this if you don't want to view any Point Datasets"; 
        internalName = "PZZnone";
    }

    /**
     * This gets the minTime (seconds since epoch) for one of the stations.
     *
     * @param stationID e.g., "M2" or "31201"
     * @return This implementation always returns Double.NaN.
     */
    public double getStationMinTime(String stationID) {
        return Double.NaN;
    }

    /**
     * Make a Table with a specific subset of the data,
     * but this version always throws Exception, since there is never
     * any data for PointDataSetNone. 
     *
     * @param minX the minimum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param maxX the maximum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param minY the minimum acceptable latitude (degrees_north)
     * @param maxY the maximum acceptable latitude (degrees_north)
     * @param minZ the minimum acceptable depth (meters, down is positive)
     * @param maxZ the maximum acceptable depth (meters, down is positive)
     * @param isoMinT an ISO format date/time for the minimum ok time
     * @param isoMaxT an ISO format date/time for the maximum ok time
     * @return nothing. This always throws an Exception.
     * @throws Exception
     */
    public Table makeSubset(double minX, double maxX,
            double minY, double maxY, double minZ, double maxZ,
            String isoMinT, String isoMaxT) throws Exception {
        throw new Exception("PointDataSetNone doesn't support makeSubset().");
    }


    /**
     * This appends data about stations 
     * (which have data within an x,y,z,t bounding box)
     * to a table of stations.
     * This implementation does nothing.
     *
     * @param minX  the minimum acceptable longitude (degrees_east).
     *    minX and maxX may be -180 to 180, or 0 to 360.
     * @param maxX  the maximum acceptable longitude (degrees_east)
     * @param minY  the minimum acceptable latitude (degrees_north)
     * @param maxY  the maximum acceptable latitude (degrees_north)
     * @param minZ  the minumum acceptable altitude (meters, positive=up)
     * @param maxZ  the maxumum acceptable altitude (meters, positive=up)
     * @param isoMinT an ISO format date/time for the minimum acceptable time  
     * @param isoMaxT an ISO format date/time for the maximum acceptable time  
     * @param stations a Table with 4 columns (LON, LAT, DEPTH, ID),
     *    where lat is in degrees_east adjusted to be in the minX maxX range,
     *    lon is in degrees_north, depth is in meters down,
     *    and ID is a string suitable for sorting (e.g., MBARI MO).
     * @throws Exception if trouble (e.g., isoMinT is invalid)
     */
    public void addStationsToTable(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ, 
        String isoMinT, String isoMaxT, Table stations) throws Exception {
        //do nothing
    }


}
