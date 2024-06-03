/* 
 * PointVectors Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/** 
 * This class has methods related to vector data from stations.
 * 
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-09-20
 */
public class PointVectors { 

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * This generates the standard file name for a station averaged timeSeries file.
     * FILE_NAME_RELATED_CODE
     *
     * @param internalName e.g., GAssta
     * @param unitsChar is either 'S' or 'A'
     * @param west is the minimum longitude in decimal degrees east
     * @param east is the maximum longitude in decimal degrees east
     * @param south is the minimum latitude in decimal degrees north
     * @param north is the maximum latitude in decimal degrees north
     * @param minDepth is the minimum depth (meters, positive = down)
     * @param maxDepth is the maximum depth (meters, positive = down)
     * @param isoBeginDateValue is the iso formatted date time, e.g., 2006-02-03 
     * @param isoEndDateValue is the iso formatted date time, e.g., 2006-02-07
     * @param timePeriodValue is one of the TimePeriod.OPTIONS.
     * @return the name for the file (obviously without dir or extension)
     */
    public static String getAveragedTimeSeriesName(String internalName, char unitsChar, 
        double west, double east, double south, double north, 
        double minDepth, double maxDepth, 
        String isoBeginDateValue, String isoEndDateValue, String timePeriodValue) {
        
        return FileNameUtility.makeAveragedPointTimeSeriesName(internalName, unitsChar, 
            west, east, south, north, minDepth, maxDepth, 
            isoBeginDateValue, isoEndDateValue, timePeriodValue);
    }

    /**
     * This generates a table with 7 columns (x, y, z, t, id, uData, vData)
     * with the requested data.
     * Currently, the units are unchanged from the u/vPointDataSets.
     *
     * <p>This can be saved to an .nc file with
     * table.saveAsFlatNc(fullAverageFileName, "row");
     * (if nRows &gt; 0).
     *
     * @param uPointDataSet
     * @param vPointDataSet
     * @param minX the minimum acceptable longitude
     * @param maxX the maximum acceptable longitude
     * @param minY the minimum acceptable latitude
     * @param maxY the maximum acceptable latitude
     * @param minDepth the minimum acceptable depth (in meters, positive = down)
     * @param maxDepth the maximum acceptable depth (in meters, positive = down)
     * @param minIsoTime the minimum acceptable time (an ISO formatted date time, UTC)
     * @param maxIsoTime the maximum acceptable time (an ISO formatted date time, UTC)
     * @param timePeriodValue one of the TimePeriod options.
     * @return a table with 7 columns (x, y, z, t, id, uData, vData)
     *   with the requested data, sorted by id, x, y, z, t.  It may have 0 rows.
     * @throws Exception if trouble
     */
    public static Table makeAveragedTimeSeries(PointDataSet uPointDataSet,
        PointDataSet vPointDataSet, double minX, double maxX,
        double minY, double maxY, double minDepth, double maxDepth,
        String minIsoTime, String maxIsoTime, String timePeriodValue) throws Exception {

        if (verbose) String2.log("\n////******** PointVectors.makeAveragedTimeSeries\n");
        long tTime = System.currentTimeMillis();

        //get the u data
        //make the table with 6 columns (x, y, z, t, id, data).
        long ttTime = System.currentTimeMillis();
        Table uTable = uPointDataSet.makeAveragedTimeSeries(minX, maxX,
            minY, maxY, minDepth, maxDepth, 
            minIsoTime, maxIsoTime, timePeriodValue);

        //get the v data
        Table vTable = vPointDataSet.makeAveragedTimeSeries(minX, maxX,
            minY, maxY, minDepth, maxDepth, 
            minIsoTime, maxIsoTime, timePeriodValue);

        //if uTable or vTable nRows == 0, it will fall through correctly 

        //convert to alt units
        //if (unitsIndex == 1) {
        //    Table.getColumn(4).scaleAddOffset(
        //        pointDataSet.altScaleFactor, pointDataSet.altOffset);
        //}

        //sort based on id,x,y,z,t
        ttTime = System.currentTimeMillis();
        uTable.sort(new int[]{4, 0, 1, 2, 3}, new boolean[]{true, true, true, true, true});
        vTable.sort(new int[]{4, 0, 1, 2, 3}, new boolean[]{true, true, true, true, true});
        //if (verbose) String2.log("  sort time=" + (System.currentTimeMillis() - ttTime) + "ms");

        //remove rows from either table which don't have comparable row in other table
        //they should be identical
        ttTime = System.currentTimeMillis();
        int nGood = 0, uRow = 0, vRow = 0;
        int uNCols = uTable.nColumns();
        int vNCols = vTable.nColumns();
        while (uRow < uTable.nRows() && vRow < vTable.nRows()) {
            //look for difference in id, x, y, z, or t
            double diff = uTable.getDoubleData(4, uRow) - vTable.getDoubleData(4, vRow); 
            int col = 0;
            while (col < 4 && diff == 0) { 
                diff = uTable.getDoubleData(col, uRow) - vTable.getDoubleData(col, vRow); 
                col++;
            }
            //if difference, ignore the lesser row (in whichever table it is in)
            if (diff < 0)      uRow++;
            else if (diff > 0) vRow++;
            else {
                //if same, keep the row from both tables
                uTable.copyRow(uRow, nGood);
                vTable.copyRow(vRow, nGood);
                nGood++;
                uRow++;
                vRow++;
            }
        }
        //if (verbose) String2.log("  pre  remove  u nRows=" + uTable.nRows() + " v nRows=" + vTable.nRows() + " nGood=" + nGood);
        if (uTable.nRows() > nGood) uTable.removeRows(nGood, uTable.nRows());
        if (vTable.nRows() > nGood) vTable.removeRows(nGood, vTable.nRows());
        //if (verbose) String2.log("  post remove  u nRows=" + uTable.nRows() + " v nRows=" + vTable.nRows() + " nGood=" + nGood);
        //if (verbose) String2.log("  makeIdentical time=" + (System.currentTimeMillis() - ttTime) + "ms");

        //move the v data column into the u table  
        //so table has 7 columns (x,y,z,t,id,uData,vData).
        uTable.addColumn(6, vTable.getColumnName(5), vTable.getColumn(5), 
            vTable.columnAttributes(5));

        //String2.log("PointVectorScreen average table=" + averageTable);
        if (verbose) String2.log(
            "\\\\\\\\******** PointVectors.makeAveragedTimeSeries done. nRows=" + uTable.nRows() +
            " TOTAL TIME=" + (System.currentTimeMillis() - tTime) + "ms\n");
        return uTable;
    }

    //for test of this class, see last part of 
    //PointDataSetFromStationVariables.testMakeCachesAndPointDataSets
}
