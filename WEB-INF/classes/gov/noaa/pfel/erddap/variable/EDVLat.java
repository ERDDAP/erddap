/* 
 * EDVLat Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.util.Test;


/** 
 * This class holds information about the latitude variable,
 * which is like EDV, but the destinationName, long_name, and units
 * are standardized.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDVLat extends EDV { 


    /**
     * The constructor -- like EDV, but the destinationName, long_name, and units
     * are standardized.
     *
     * @param tSourceMin  is pre-scale_factor and add_offset.
     *   This takes precedence over actual_range, data_min, or data_max metadata.
     * @param tSourceMax  is pre-scale_factor and add_offset.
     *   This takes precedence over actual_range, data_min, or data_max metadata.
     */
    public EDVLat(String tSourceName, 
        Attributes tSourceAttributes, Attributes tAddAttributes, 
        String tSourceDataType, double tSourceMin, double tSourceMax) 
        throws Throwable {

        super(tSourceName, EDV.LAT_NAME, tSourceAttributes, tAddAttributes,
            tSourceDataType, tSourceMin, tSourceMax);

        longName = EDV.LAT_LONGNAME;
        units = EDV.LAT_UNITS; 
        combinedAttributes.set("_CoordinateAxisType", "Lat");  //unidata-related
        combinedAttributes.set("axis", "Y");
        combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
        combinedAttributes.set("long_name", longName);
        combinedAttributes.set("standard_name", LAT_STANDARD_NAME);
        combinedAttributes.set("units", units);

        extractAndSetActualRange();
    }

    /**
     * This returns a string representation of this EDVLat.
     *
     * @return a string representation of this EDVLat.
     */
    public String toString() {
        return "EDVLat/" + super.toString();
    }

}
