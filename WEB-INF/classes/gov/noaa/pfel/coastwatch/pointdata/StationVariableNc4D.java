/* 
 * StationVariableNc4D Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Calendar;
//import java.util.GregorianCalendar;
import java.util.List;
//import java.util.Vector;

// from netcdfAll-x.jar
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** The Java DAP classes.  */
//import dods.dap.*;

/** 
 * This class provides access to one variable from one station,
 * where the data is available via a 4D .nc file (where the
 * 4D variables have dimensions: time, depth/altitude, lat, lon.
 * 
 * The constructor searches for available data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-07-20
 */
public class StationVariableNc4D extends GroupVariableWithDepthLevels { 


    private String fullFileName;
    private String inFileVariableName;


    
    /**
     * The constructor.
     * Since this is a station, the following will be true: minX=maxX, minY=maxY, minDepth=maxDepth.
     *
     * @param fullFileName
     * @param inFileVariableName the name of the variable, e.g., "AIR_TEMPERATURE_HR"
     * @param variableName the nice variable name that will be displayed to users 
     *    (when groupName is added), e.g., "Air Temperature, Near Real Time"
     * @param groupName the nice groupName that will be displayed to users, e.g., "MBARI M2adcp"
     * @param standardUnitsFactor the factor needed to convert raw data to udUnits.
     * @param stationX the longitude of the station (as in the file)
     * @param stationY the latitude of the station
     * @param stationDepthLevels the depth levels of the station (in meters, positive = down)
     * @param stationZUp is true if positive depth is "up" for the source data, false if positive depth is "down"
     * @param timeBaseSeconds  the base time (seconds since 1970-01-01Z)
     * @param timeFactorToGetSeconds  e.g., if time stored in hours, this is 3600
     * @param timeIncrementInSeconds is the time between readings 
     *    (e.g., 3600 = every hour or Double.NaN if not evenly spaced times)
     * @param minT the seconds since 1970-01-01Z of the first observation
     * @param maxT the seconds since 1970-01-01Z of the last observation
     * @param sourceFillValue the fill value in the source (or NaN). 
     * @param sourceMissingValue the (fake) missing value in the source (or NaN). 
     *    The sourceMissingValue and sourceFillValue may be the same, different,
     *    or both NaN.
     * @throws Exception if trouble or no data
     */
    public StationVariableNc4D(String fullFileName, String inFileVariableName, 
            String variableName, String groupName, double standardUnitsFactor, 
            double stationX, double stationY, DoubleArray stationDepthLevels, boolean stationZUp,
            double timeBaseSeconds, double timeFactorToGetSeconds,
            double timeIncrementInSeconds, double minT, double maxT,
            float sourceFillValue, float sourceMissingValue) throws Exception {

        //store all the information
        //FUTURE: you could store all this in a Station class 
        //   and that could be shared by all variables in that station.
        this.fullFileName = fullFileName;
        this.inFileVariableName = inFileVariableName;
        this.variableName = variableName;
        this.groupName = groupName;
        this.standardUnitsFactor = standardUnitsFactor;
        this.minX = stationX;
        this.maxX = stationX;
        this.minY = stationY;
        this.maxY = stationY;
        this.depthLevels = stationDepthLevels;
        double depthStats[] = depthLevels.calculateStats();
        this.minDepth = depthStats[PrimitiveArray.STATS_MIN]; 
        this.maxDepth = depthStats[PrimitiveArray.STATS_MAX];
        this.sourceFillValue = sourceFillValue;
        this.sourceMissingValue = sourceMissingValue;
        this.sourceZUp = stationZUp;
        this.timeBaseSeconds = timeBaseSeconds;
        this.timeFactorToGetSeconds = timeFactorToGetSeconds;
        if (timeIncrementInSeconds <= 0) timeIncrementInSeconds = Double.NaN;
        this.timeIncrementInSeconds = timeIncrementInSeconds;
        this.minT = minT;
        this.maxT = maxT;

        //if (verbose) 
        //    String2.log("StationVariableNc4D(inFile=" + inFileVariableName + 
        //        "\n  group=" + groupName + " variable=" + variableName + ")");

        //ensure valid
        ensureValid();  //this prints toString if verbose      
    }

    /**
     * This generates a string representation of this GroupVariable.
     *
     * @throws Exception if trouble
     */
    public String toString() {
        return super.toString() + 
            "\n  fileName=" + fullFileName +
            "\n  inFileVariableName=" + inFileVariableName;
    }

    /**
     * This adds relevant data from this GroupVariable to the table.
     * <p>If this station is in the x,y,z range, but not the t range,
     *   a row with data=mv is added for this station.
     *
     * <p> This uses FUDGE to be a little lax in determining if a 
     * station is in range or not.
     *
     * @param minX the minimum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param maxX the maximum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param minY the minimum acceptable latitude (degrees_north)
     * @param maxY the maximum acceptable latitude (degrees_north)
     * @param minDepth the minimum acceptable depth (meters, down is positive)
     * @param maxDepth the maximum acceptable depth (meters, down is positive)
     * @param minT the minimum acceptable time in seconds since 1970-01-01T00:00:00Z.
     *   This method just works literally with minT and maxT.
     *   If this GroupVariable has evenly spaced times, minT and maxT
     *   should already be rounded to a multiple of timeIncrementSeconds.
     * @param maxT the maximum acceptable time in seconds since 1970-01-01T00:00:00Z
     * @param table the Table with 6 columns to be appended: 
     *    "LON" (units=degrees_east, with values made relevant to the desired minX maxX), 
     *    "LAT" (units=degrees_north), 
     *    "DEPTH" (units=meters, positive=down), 
     *    "TIME" (units=seconds since 1970-01-01T00:00:00Z), 
     *    "ID" (String data), 
     *    data (unpacked, in standard units).
     *   <ul>
     *   <li>LON, LAT, DEPTH and TIME will be DoubleArrays; ID will be a StringArray; 
     *      the data column will be a numeric PrimitiveArray (not necessarily DoubleArray).
     *   <li>Rows with missing values are NOT removed.
     *   <li>No metadata will be added to the column variables. 
     *   </ul>
     * @throws Exception if trouble
     */
    public void addToSubset(double minX, double maxX,
            double minY, double maxY, double minDepth, double maxDepth,
            double minT, double maxT, Table table) throws Exception {

        //String2.log("StationVariableNc4D.addToSubset " + variableName + " (" + groupName + ")");
        String errorInMethod = String2.ERROR + " in StationVariableNc4D.addToSubset " + 
            variableName + " (" + groupName + "):\n";

        //quick reject?
        //since this subclass is for stations, this is the only test needed for x,y,z
        //quick reject based on y, z?
        if (minY - FUDGE > this.maxY || 
            maxY + FUDGE < this.minY ||
            minDepth - FUDGE > this.maxDepth || 
            maxDepth + FUDGE < this.minDepth) {
            //reject, and don't add mv row since out of range
            //String2.log("  reject " + groupName + " based on y or depth" +
            //    "\n  minY=" + minY + " maxY=" + maxY + " this.maxY=" + this.maxY + 
            //    "\n  minDepth=" + minDepth + " maxDepth=" + maxDepth + " this.maxDepth=" + this.maxDepth); 
            return;
        }
        
        //quick reject based on x?
        //see if x is in range with various adjustments
        double xAdjust = Double.NaN;
        if      (this.maxX       >= minX - FUDGE && this.minX       <= maxX + FUDGE) xAdjust = 0;
        else if (this.maxX + 360 >= minX - FUDGE && this.minX + 360 <= maxX + FUDGE) xAdjust = 360;
        else if (this.maxX - 360 >= minX - FUDGE && this.minX - 360 <= maxX + FUDGE) xAdjust = -360;
   
        if (Double.isNaN(xAdjust)) {
            //reject, and don't add mv row since out of range
            //String2.log("  reject " + groupName + " based on x" +
            //    "\n  minX=" + minX + " maxX=" + maxX + " this.maxX=" + this.maxX); 
            return;
        }

        //find firstZ and lastZ
        int firstZ = depthLevels.binaryFindFirstGAE(0, depthLevels.size() - 1, PAOne.fromDouble(minDepth), 5);
        int lastZ  = depthLevels.binaryFindLastLAE( 0, depthLevels.size() - 1, PAOne.fromDouble(maxDepth), 5);

        //reject because z range is between 2 z levels or no valid depths?
        if (firstZ > lastZ) {
            //reject, and don't add mv row
            return;
        }

        //reject based on time
        if (this.maxT < minT || this.minT > maxT) {
            //in xyz range, but no data for this time or z, so add mv row
            //This can be plotted as hollow marker.
            //Message to user is: there is a station here (at x,y,z) but no data for the specified time (so try another time).
            PointDataSet.addSubsetRow(table, this.minX + xAdjust, this.minY, 
                depthLevels.get(firstZ), maxT, groupName, Double.NaN);
            return;
        }


        //get the data from the 4D ncFile
        NetcdfFile ncFile;
        
        try {
            ncFile = NcHelper.openFile(fullFileName);
        } catch (Exception e) {
            //if error, like file not found, it is silent error
            //(perhaps CacheOpendapStation.update failed and the cache is being recreated)
            return;
        }

        try {
            //get the data variable
            Variable dataVariable = ncFile.findVariable(inFileVariableName);  //null if not found

            //get the time variable
            Dimension timeDimension = (Dimension)dataVariable.getDimensions().get(0);
            int timeDimensionLength = timeDimension.getLength();
            String timeName = timeDimension.getName();
            Variable timeVariable = ncFile.findVariable(timeName);  //null if not found

            //trim to valid t range
            if (maxT > this.maxT) maxT = this.maxT;
            if (minT < this.minT) minT = this.minT;
            
            //find the first and last rows (based on minT and maxT)  -- round to nearest
            int firstTimeRow, lastTimeRow;
            if (Double.isNaN(timeIncrementInSeconds) || timeIncrementInSeconds <= 0) {
                //not evenly spaced data   (!!! better if it could round to nearest
                long tTime = System.currentTimeMillis();
                firstTimeRow = NcHelper.binaryFindFirstGE(timeVariable, 0, timeDimensionLength - 1, minT);
                lastTimeRow  = NcHelper.binaryFindLastLE(timeVariable, firstTimeRow, timeDimensionLength - 1, maxT);

                //I know it is in this.minT this.maxT range (or rejected above)
                //so if between times, binaryFindClosest times
                if (firstTimeRow > lastTimeRow) {
                    firstTimeRow = NcHelper.binaryFindClosest(timeVariable, 0, timeDimensionLength - 1, minT);
                    lastTimeRow  = NcHelper.binaryFindClosest(timeVariable, 0, timeDimensionLength - 1, maxT);
                }
                //String2.log("  binarySearch to find first,lastTimeRow, time=" + 
                //    (System.currentTimeMillis() - tTime) + "ms");  // ~4 ms
            } else {
                //evenly spaced data
                firstTimeRow = Math2.roundToInt((minT - this.minT) / timeIncrementInSeconds);
                lastTimeRow  = Math2.roundToInt((maxT - this.minT) / timeIncrementInSeconds);
            }
            //String2.log("  firstTimeRow=" + firstTimeRow + " lastTimeRow=" + lastTimeRow);

            //verify time dimension data;  make timePa
            PrimitiveArray timePa = NcHelper.getPrimitiveArray(timeVariable, firstTimeRow, lastTimeRow); 
            Test.ensureEqual(timePa.size(), lastTimeRow - firstTimeRow + 1, errorInMethod + "Unexpected timePa.size.");

            //for each zLevel
            //!!! It is important to get the data with time varying faster then z:
            //  PointDataSet.getAveragedTimeSeries averages adjacent rows with same x,y,z,
            //  so this way, the z's will get averaged.
            for (int zLevel = firstZ; zLevel <= lastZ; zLevel++) {
                PrimitiveArray data = NcHelper.get4DValues(dataVariable, 0, 0, zLevel, firstTimeRow, lastTimeRow); 
                int n = data.size();
                Test.ensureEqual(n, lastTimeRow - firstTimeRow + 1, errorInMethod + "Unexpected data n.");

                //make the time data
                //safer to look it up, but faster to generate it
                //DoubleArray timeArray = new DoubleArray();
                //for (int i = 0; i < n; i++)
                //    timeArray.add(minT + i * timeIncrementInSeconds);

                //convert mv data to NaN
                if (!Float.isNaN(sourceFillValue))
                    for (int i = 0; i < n; i++)
                        if (data.getFloat(i) == sourceFillValue)
                            data.setDouble(i, Double.NaN);
                if (!Float.isNaN(sourceMissingValue)) {
                    if (!Float.isNaN(sourceFillValue) && sourceMissingValue == sourceFillValue) {
                        //fv and mv are finite and mv=fv, do nothing more
                    } else {
                        for (int i = 0; i < n; i++)
                            if (data.getFloat(i) == sourceMissingValue)
                                data.setDouble(i, Double.NaN);
                    }
                }
                //diagnostic:
                //double[] stats = data.calculateStats();
                //if (stats[PrimitiveArray.STATS_MIN] < -1e20)
                //    String2.log("StationVariableNc4D.addToSubset min=" + stats[PrimitiveArray.STATS_MIN] +
                //        " sourceMV=" + sourceMissingValue + " sourceFV=" + sourceFillValue);

                //convert data to standard units
                data.scaleAddOffset(standardUnitsFactor, 0);

                //add the data to the columns
                PrimitiveArray column;
                column = table.getColumn(0); column.append(PrimitiveArray.factory(column.elementType(), n, String2.genEFormat10(this.minX + xAdjust)));
                column = table.getColumn(1); column.append(PrimitiveArray.factory(column.elementType(), n, String2.genEFormat10(this.minY)));
                column = table.getColumn(2); column.append(PrimitiveArray.factory(column.elementType(), n, String2.genEFormat10(depthLevels.get(zLevel))));
                column = table.getColumn(3); column.append(timePa);
                column = table.getColumn(4); column.append(PrimitiveArray.factory(column.elementType(), n, groupName));
                column = table.getColumn(5); column.append(data);
            }
        } finally {
            try {if (ncFile != null) ncFile.close(); } catch (Exception e9) {}
        }
    }



//see test in PointDataSetStationVariables
}
