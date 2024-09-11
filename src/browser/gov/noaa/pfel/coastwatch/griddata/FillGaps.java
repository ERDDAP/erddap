/* 
 * FillGaps Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

/**
 * THIS WASN'T EVER FINISHED.
 * This class can fill in gaps in a data set by searching for nearest
 * neighbors in time and space.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-03-28
 *
 */
public class FillGaps  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /** The number of grids to be held at once while searching for nearest neighbors. 
     * It should be an odd number so there are an equal number of 
     * grids on either side of the time point in question. */
    public int nGrids = 25;

    /** Weights are used to determine the relative preference of nearest
     * neighbors in the latLon dimensions vs. the time dimensions.
     * Weights are numbers between 0 (no weight) and 1 (highest priority).
     */
    public double nearestLatLonWeight = 1;  
    public double nearestTimeWeight = 1;


    /**
     * This makes a new grid file with all the data from specific grid, but
     * with the gaps filled in.
     *
     * @param gridDataSet Holds the information about the original data (with gaps). 
     * @param timePeriod the timePeriod you want to work on (e.g., "1 Observation").
     *    See TimePeriods.
     * @param dateTime the date you want to work on (an ISO date/time e.g., "2006-01-03T12:00:00")
     *
     */
    public static void fillGaps(GridDataSet gridDataSet, String timePeriod, 
        String dateTime) {

/* commented out to avoid trouble with javadoc
        //load the grids of gappy data  
        int center = nGrids / 2; //holds the grid for the time point we're going to fill in
        Grid grid[] = new Grid[nGrids];
        int first = 0;  //all the grids from first to last must be valid
        int last = nGrids - 1;
        for (int i = 0; i < nGrids; i++) 
            ...
        int nX = grid[center].lon.length;
        int nY = grid[center].lat.length;

        //ensure all grids cover the same range at the same resolution
        for (int i = first; i <= last; i++) {
            Test.ensureEqual(grid[i].lon, grid[center].lon, 
                errorInMethod + "lon for grid[" + i + "] not same as grid[center].");
            Test.ensureEqual(grid[i].lat, grid[center].lat, 
                errorInMethod + "lat for grid[" + i + "] not same as grid[center].");
        }
            
        //make the grid for the new data
        Grid newGrid = (Grid)grid[center].clone();
        
        //go through the points looking for gaps
        for (int x = 0; x < nX; x++) {
            for (int y = 0; y < nY; y++) {
                

                //is it a gap
                if (Float.isNull()) {
                    //find nearest neighbors
                }
            }
*/
        }
    

    /**
     * A main method -- used to run this class.
     *
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {

        String2.log("\n***** FillGaps.main finished successfully");

    }


}
