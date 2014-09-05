/* 
 * EDVLon Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.util.Test;


/** 
 * This class holds information about the longitude variable,
 * which is like EDV, but the destinationName, and units
 * are standardized.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDVLon extends EDV { 


    /**
     * The constructor -- like EDV, but the destinationName, and units
     * are standardized.
     *
     * @param tSourceMin  is pre-scale_factor and add_offset.
     *   This takes precedence over actual_range, data_min, or data_max metadata.
     * @param tSourceMax  is pre-scale_factor and add_offset.
     *   This takes precedence over actual_range, data_min, or data_max metadata.
     */
    public EDVLon(String tSourceName, 
        Attributes tSourceAttributes, Attributes tAddAttributes, 
        String tSourceDataType, double tSourceMin, double tSourceMax) 
        throws Throwable {

        super(tSourceName, LON_NAME, tSourceAttributes, tAddAttributes,
            tSourceDataType, tSourceMin, tSourceMax);

        combinedAttributes.set("_CoordinateAxisType", "Lon");  //unidata-related
        combinedAttributes.set("axis", "X");
        combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
        combinedAttributes.set("standard_name", LON_STANDARD_NAME);

        longName = combinedAttributes.getString("long_name");
        if (longName == null ||  //catch nothing
            longName.toLowerCase().equals("lon") ||
            longName.toLowerCase().equals("longitude")) { //catch alternate case
            longName = LON_LONGNAME;
            combinedAttributes.set("long_name", longName);
        }

        units = LON_UNITS; 
        combinedAttributes.set("units", units);

        extractAndSetActualRange();
    }

    /**
     * This returns a string representation of this EDVLon.
     *
     * @return a string representation of this EDVLon.
     */
    public String toString() {
        return "EDVLon/" + super.toString();
    }

}
