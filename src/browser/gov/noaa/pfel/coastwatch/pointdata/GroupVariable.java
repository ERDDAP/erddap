/* 
 * GroupVariable Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/** 
 * This class defines a way to access to one variable's data from one group
 * (a station or trajectory).
 * Subclasses may get the data in various ways (e.g., via a local file
 * or via opendap).
 * 
 * The constructor searches for available data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-07-20
 */
public abstract class GroupVariable  { 

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * FUDGE determines the fudge factor used by addToSubset to determine
     * if a station's lat, lon, and depth are within the
     * bounding box specified by the users.
     * This avoids problems with requests for one point (e.g., 24.58), but specified
     * at different resolution than the station location is stored (e.g., 24.5805).
     */
    public static final double FUDGE = 0.001;

    //ALL PROTECTED VARIABLES ARE SET BY THE CONSTRUCTOR   

    /** The groupName (think stationName, e.g., "MBARI M2" or "NDBC 32101"). */
    protected String groupName;

    /** The min/max/X/Y/Z/T values define a bounding box around available data. */
    protected double 
        minX = Double.NaN, maxX = Double.NaN, 
        minY = Double.NaN, maxY = Double.NaN, 
        minDepth = Double.NaN, maxDepth = Double.NaN, 
        minT = Double.NaN, maxT = Double.NaN;

    /** The fill value in the source. 
     * It is a float to make it less sensitive to bruising.
     */
    protected float sourceFillValue = Float.NaN;

    /** The (fake) missing value in the source. 
     * It is a float to make it less sensitive to bruising.
     */
    protected float sourceMissingValue = Float.NaN;

    /** 'true' if source's Z data is "up"; 'false if source's z data is "down". 
      Regardless of this, minDepth, maxDepth and all z's in queries work as though
      sourceZUp=false. */
    protected boolean sourceZUp = true;

    /** Used to convert raw data to standardUnits. */
    protected double standardUnitsFactor = 1;

    /** Used for conversion of native time values to seconds since 1970-01-01. */
    protected double timeBaseSeconds;

    /** Used for conversion of native time values to seconds since 1970-01-01. */
    protected double timeFactorToGetSeconds;

    /** If observations are evenly spaced in time, this is set; otherwise NaN. */
    protected double timeIncrementInSeconds = Double.NaN;

    /** The data variable's name (e.g., SST). */
    protected String variableName;

    /**
     * This resets everything so that the stationVariable contains no data.
     */
    public void reset() {
        groupName = null;
        minX = Double.NaN; maxX = Double.NaN; 
        minY = Double.NaN; maxY = Double.NaN; 
        minDepth = Double.NaN; maxDepth = Double.NaN; 
        minT = Double.NaN; maxT = Double.NaN;
        sourceFillValue = Float.NaN;
        sourceMissingValue = Float.NaN;
        sourceZUp = true;
        standardUnitsFactor = 1;
        timeBaseSeconds = Double.NaN;
        timeFactorToGetSeconds = Double.NaN;
        timeIncrementInSeconds = Double.NaN;
        variableName = null;
    }

    /**
     * This ensures that all required values have been set.
     *
     * @throws Exception if trouble
     */
    public void ensureValid() {
        String errorInMethod = String2.ERROR + " in GroupVariable(group=" + groupName + 
            " variable=" + variableName + ").ensureValid:\n";
        Test.ensureNotNull(groupName, errorInMethod + "groupName is null.");
        Test.ensureNotEqual(groupName.length(), 0, errorInMethod + "groupName is \"\".");
        Test.ensureNotEqual(minX, Double.NaN, errorInMethod + "minX wasn't set.");
        Test.ensureNotEqual(maxX, Double.NaN, errorInMethod + "maxX wasn't set.");
        Test.ensureNotEqual(minY, Double.NaN, errorInMethod + "minY wasn't set.");
        Test.ensureNotEqual(maxY, Double.NaN, errorInMethod + "maxY wasn't set.");
        Test.ensureNotEqual(minDepth, Double.NaN, errorInMethod + "minDepth wasn't set.");
        Test.ensureNotEqual(maxDepth, Double.NaN, errorInMethod + "maxDepth wasn't set.");
        Test.ensureNotEqual(minT, Double.NaN, errorInMethod + "minT wasn't set.");
        Test.ensureNotEqual(maxDepth, Double.NaN, errorInMethod + "maxT wasn't set.");
        //sourceFillValue can be anything
        //sourceMissingValue can be anything
        //sourceZUp can be true or false
        Test.ensureNotEqual(standardUnitsFactor, Double.NaN, errorInMethod + "standardUnitsFactor is NaN.");
        Test.ensureNotEqual(timeBaseSeconds, Double.NaN, errorInMethod + "timeBaseSeconds wasn't set.");
        Test.ensureNotEqual(timeFactorToGetSeconds, Double.NaN, errorInMethod + "timeFactorToGetSeconds wasn't set.");
        Test.ensureTrue(Double.isNaN(timeIncrementInSeconds) || timeIncrementInSeconds > 0,
            errorInMethod + "timeIncrementInSeconds must be NaN or >0: " + timeIncrementInSeconds);
        Test.ensureNotNull(variableName, errorInMethod + "variableName is null.");
        Test.ensureNotEqual(variableName.length(), 0, errorInMethod + "variableName is \"\".");

        //if (verbose) 
        //    String2.log("  GroupVariable(group=" + groupName + 
        //        " variable=" + variableName + ") is valid." 
        //        //+ "\n" + toString()
        //        );
    }

    /**
     * This generates a string representation of this GroupVariable.
     *
     * @throws Exception if trouble
     */
    public String toString() {
        return 
            "GroupVariable groupName=" + groupName + " variableName=" + variableName +
            "\n  minX=" + minX + " maxX=" + maxX + " minY=" + minY + " maxY=" + maxY +
               " minDepth=" + minDepth + " maxDepth=" + maxDepth + " minT=" + minT + " maxT=" + maxT + 
            "\n  sourceFV=" + sourceFillValue + " sourceMV=" + sourceMissingValue + 
                " sourceZUp=" + sourceZUp +
                " timeBaseSeconds=" + timeBaseSeconds + 
            "\n  timeFactorToGetSeconds=" + timeFactorToGetSeconds +
                " timeIncrementInSeconds=" + timeIncrementInSeconds;
    }

    /**
     * This returns the groupName (think stationName, e.g., "MBARI MO", which sorts nicely)
     *
     * @return the groupName (think stationName, e.g., "MBARI MO", which sorts nicely)
     */
    public String groupName() {return groupName;  }

    /**
     * This returns the data variable's name.
     *
     * @return the data variable's name.
     */
    public String variableName() {return variableName; }

    /**
     * This returns the (in-the-file) minimum X (longitude) value held by this group variable.
     *
     * @return the minimum X (longitude) value held by this group variable.
     */
    public double minX() {return minX;  }

    /**
     * This returns the (in-the-file) maximum X (longitude) value held by this group variable.
     *
     * @return the maximum X (longitude) value held by this group variable.
     */
    public double maxX() {return maxX;  }

    /**
     * This returns the minimum Y (latitude) value held by this group variable.
     *
     * @return the minimum Y (latitude) value held by this group variable.
     */
    public double minY() {return minY;  }

    /**
     * This returns the maximum Y (latitude) value held by this group variable.
     *
     * @return the maximum Y (latitude) value held by this group variable.
     */
    public double maxY() {return maxY;  }

    /**
     * This returns the minimum Z (depth, up is down) value held by this group variable.
     *
     * @return the minimum Z (depth, up is down) value held by this group variable.
     */
    public double minDepth() {return minDepth;  }

    /**
     * This returns the maximum Z (depth, up is down) value held by this group variable.
     *
     * @return the maximum Z (depth, up is down) value held by this group variable.
     */
    public double maxDepth() {return maxDepth;  }

    /**
     * This returns the minimum T (time in seconds since 1970-01-01T00:00:00Z)
     * value held by this group variable.
     *
     * @return the minimum T (time in seconds since 1970-01-01T00:00:00Z)
     *   value held by this group variable.
     */
    public double minT() {return minT;  }

    /**
     * This returns the maximum T (time in seconds since 1970-01-01T00:00:00Z)
     * value held by this group variable.
     *
     * @return the maximum T (time in seconds since 1970-01-01T00:00:00Z)
     *   value held by this group variable.
     */
    public double maxT() {return maxT;  }

    /** 
     * This returns the fill value in the source.
     * It is a float to make it less sensitive to bruising.
     * See also sourceMissingValue.
     *
     * @return the fill value in the source (it may be NaN). 
     */
    public float sourceFillValue() {return sourceFillValue;}

    /** 
     * This returns the (fake) missing value in the source.
     * It is a float to make it less sensitive to bruising.
     * See also sourceFillValue.
     *
     * @return the missing value in the source (it may be NaN). 
     */
    public float sourceMissingValue() {return sourceMissingValue;}

    /** 'true' if source's Z data is "up"; 'false if source's z data is "down". 
     * Regardless of this, minDepth, maxDepth and all z's in queries work as though
     *  zUp=true. 
     *
     * return 'true' if source's Z data is "up"; 'false if source's z data is "down". 
     */
    public boolean sourceZUp() {return sourceZUp; }

    /** 
     * This returns factor needed to convert raw data to standard units. 
     * This is usually not needed outside this class and subclasses.
     *
     * @return the factor needed to convert raw data to standard units. 
     */
    public double standardUnitsFactor() {return standardUnitsFactor; }

    /** 
     * This returns the base time (number of seconds since 1970-01-01). 
     * @return the base time (number of seconds since 1970-01-01).  
     */
    public double timeBaseSeconds() {return timeBaseSeconds; }

    /** 
     * This returns the factor to convert raw data file times to seconds.
     * For months and years, this returns a special value.
     * @return the factor to convert raw data file times to seconds.
     */
    public double timeFactorToGetSeconds() {return timeFactorToGetSeconds; }

    /** 
     * If observations are perfectly evenly spaced in time, this is the number of seconds
     * between observations; otherwise NaN. 
     * @return If observations are perfectly evenly spaced in time, this returns the number of seconds
     *    between observations; otherwise NaN. 
     */
    public double timeIncrementInSeconds() {return timeIncrementInSeconds; }

    /**
     * This adds relevant data from this GroupVariable to the table.
     * This won't throw an exception.
     *
     * <p> This uses FUDGE to be a little lax in determining if a 
     * station is in range or not.
     *
     * @param minX the minimum acceptable longitude 
     * @param maxX the maximum acceptable longitude 
     * @param minY the minimum acceptable latitude 
     * @param maxY the maximum acceptable latitude 
     * @param minDepth the minimum acceptable depth (down is positive)
     * @param maxDepth the maximum acceptable depth (down is positive)
     * @param minT the minimum acceptable time in seconds since 1970-01-01T00:00:00Z.
     *   If this GroupVariable has evenly spaced times, minT and maxT
     *   should already be rounded to a multiple of timeIncrementSeconds.
     *   This method just works literally with minT and maxT.
     * @param maxT the maximum acceptable time in seconds since 1970-01-01T00:00:00Z
     * @param table a Table with 6 columns (LON, LAT, DEPTH, TIME, ID, data).
     *   The data will be unpacked, in the standard units.
     *   LON, LAT, DEPTH and TIME should be DoubleArrays; ID should be a String column; 
     *   data must be a numeric PrimitiveArray (not necessarily DoubleArray).
     *   Rows with missing values are NOT removed.
     *   No metadata will be added to the column variables. 
     * @throws Exception if trouble
     */
    public abstract void addToSubset(double minX, double maxX,
            double minY, double maxY, double minDepth, double maxDepth,
            double minT, double maxT, Table table) throws Exception;

}
