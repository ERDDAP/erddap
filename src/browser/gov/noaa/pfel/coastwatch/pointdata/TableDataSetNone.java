/* 
 * TableDataSetNone Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.ema.EmaColor;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/** 
 * This class represents the "None" TableDataSet.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-03-06
 */
public class TableDataSetNone extends TableDataSet { 

    /**
     * Constructor.
     *
     */
    public TableDataSetNone() {
        internalName = "4!!none";
        datasetName = "(None)";
    } 

    /** This throws Exception if called. */
    public Table makeSubset(String isoMinTime, String isoMaxTime, 
            String desiredIndividuals[], 
            String[] desiredDataVariables) throws Exception {
        throw new Exception("TableDataSetNone doesn't support makeSubset().");
    }

}
