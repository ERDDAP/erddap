/* 
 * EDVDepth Copyright 2012, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;


/** 
 * This class holds information about a depth variable, 
 * which is like EDV, but the destinationName, long_name, and units
 * are standardized, and you need to specify scale_factor to 
 * convert source altitude/depth values to meters below sea level in the results.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDVDepth extends EDV { 


    /**
     * The constructor -- like EDV, but the destinationName, long_name, and units
     * are standardized, and you need to specify scale_factor to 
     * convert source altitude/depth values to meters below sea level in the results.
     *
     * @param tSourceMin  is pre-scale_factor and add_offset.
     *   This takes precedence over actual_range, data_min, or data_max metadata.
     * @param tSourceMax  is pre-scale_factor and add_offset.
     *   This takes precedence over actual_range, data_min, or data_max metadata.
     * @throws Throwable if trouble
     */
    public EDVDepth(String tSourceName, 
        Attributes tSourceAttributes, Attributes tAddAttributes, 
        String tSourceDataType, double tSourceMin, double tSourceMax) 
        throws Throwable {

        super(tSourceName, DEPTH_NAME, tSourceAttributes, tAddAttributes,
            tSourceDataType, tSourceMin, tSourceMax); 

        units = DEPTH_UNITS; 
        combinedAttributes.set("_CoordinateAxisType", "Height");   //unidata
        combinedAttributes.set("_CoordinateZisPositive", "down");  //unidata
        combinedAttributes.set("axis", "Z");
        combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
        longName = combinedAttributes.getString("long_name");
        if (longName == null ||  //catch nothing
            longName.toLowerCase().equals("depth")) { //catch alternate case
            longName = DEPTH_LONGNAME;
            combinedAttributes.set("long_name", longName);
        }
        combinedAttributes.set("positive", "down"); //cf
        combinedAttributes.set("standard_name", DEPTH_STANDARD_NAME);
        EDVAlt.ensureUnitsAreM(combinedAttributes.getString("units"), "depth" , "down");
        combinedAttributes.set("units", units);        

        //set destinationMin max  if not set by tSourceMin,Max
        double mm[] = extractActualRange(); //always extract 
        if (Double.isNaN(destinationMin)) destinationMin = mm[0] * scaleFactor + addOffset;
        if (Double.isNaN(destinationMax)) destinationMax = mm[1] * scaleFactor + addOffset;
        if (destinationMin > destinationMax) {
            double d1 = destinationMin; destinationMin = destinationMax; destinationMax = d1;
        }
        setActualRangeFromDestinationMinMax();

        PrimitiveArray pa = combinedAttributes.get("missing_value"); 
        if (pa != null) 
            pa.setDouble(0, destinationMissingValue);
        pa = combinedAttributes.get("_FillValue"); 
        if (pa != null) 
            pa.setDouble(0, destinationFillValue);

    }

    /**
     * This returns a string representation of this EDV.
     *
     * @param errorInMethod the start string for an error message
     * @return a string representation of this EDV.
     */
    public String toString() {
        return "EDVDepth/" + super.toString(); 
    }

    /**
     * This is used by the EDD constructor to determine if this
     * EDV is valid.
     *
     * @throws Throwable if this EDV is not valid
     */
    public void ensureValid(String errorInMethod) throws Throwable {
        super.ensureValid(errorInMethod);
        errorInMethod += "\ndatasets.xml/EDVDepth.ensureValid error for soureName=" + sourceName + ":\n";
    }



}
