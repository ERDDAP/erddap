/* 
 * GroupVariableWithZLevels Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
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
 * This extends a GroupVariable to include the idea of data at 
 * several Depth levels.
 * 
 * The constructor searches for available data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-07-20
 */
public abstract class GroupVariableWithDepthLevels extends GroupVariable { 

    /** The Depth levels in meters (positive is down).
     */
    protected DoubleArray depthLevels;

    /**
     * This resets everything so that the stationVariable contains no data.
     */
    public void reset() {
        super.reset();
        depthLevels = null;
    }

    /**
     * This ensures that all required values have been set.
     *
     * @throws Exception if trouble
     */
    public void ensureValid() {
        if (depthLevels == null) 
            Test.error(String2.ERROR + " in GroupVariable(group=" + groupName + 
                " variable=" + variableName + ").ensureValid:\n" + "depthLevels is null.");
        super.ensureValid();  //prints "is valid" to log
    }

    /**
     * This generates a string representation of this GroupVariable.
     *
     * @throws Exception if trouble
     */
    public String toString() {
        return super.toString() + 
            "\n  depthLevels=" + depthLevels;
    }

    /**
     * This returns the depthLevels (in meters, positive down)
     *
     * @return the depthLevels (in meters, positive down)
     */
    public DoubleArray depthLevels() {return depthLevels;  }

}
