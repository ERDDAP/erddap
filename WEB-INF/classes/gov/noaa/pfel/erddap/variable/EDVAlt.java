/* 
 * EDVAlt Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;


/** 
 * This class holds information about an altitude variable, 
 * which is like EDV, but the destinationName, long_name, and units
 * are standardized, and you need to specify tMetersPerSourceUnit to 
 * convert source altitude/depth values to meters above sea level in the results.
 *
 * <p>The order of conversion is: source data &gt; scale_factor and add_offset &gt;
 * altitudeMetersPerSourceUnit.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDVAlt extends EDV { 

    protected double metersPerSourceUnit = Double.NaN;

    /**
     * The constructor -- like EDV, but the destinationName, long_name, and units
     * are standardized, and you need to specify tMetersPerSourceUnit to 
     * convert source altitude/depth values to meters above sea level in the results.
     *
     * @param tSourceMin  is pre-scale_factor and add_offset.
     *   This takes precedence over actual_range, data_min, or data_max metadata.
     * @param tSourceMax  is pre-scale_factor and add_offset.
     *   This takes precedence over actual_range, data_min, or data_max metadata.
     * @param tMetersPerSourceUnit the conversion factor needed to convert
     *    the de-scaled/offset source altitude values to/from meters above sea level.
     *    A positive value indicates the source is positive is up.
     *    A negative value indicates the source is positive is down.
     * @throws Throwable if trouble
     */
    public EDVAlt(String tSourceName, 
        Attributes tSourceAttributes, Attributes tAddAttributes, 
        String tSourceDataType, double tSourceMin, double tSourceMax, 
        double tMetersPerSourceUnit) 
        throws Throwable {

        super(tSourceName, EDV.ALT_NAME, tSourceAttributes, tAddAttributes,
            tSourceDataType, tSourceMin, tSourceMax); 

        units = EDV.ALT_UNITS; 
        combinedAttributes.set("_CoordinateAxisType", "Height");   //unidata
        combinedAttributes.set("_CoordinateZisPositive", "up");  //unidata
        combinedAttributes.set("axis", "Z");
        combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
        longName = combinedAttributes.getString("long_name");
        if (longName == null) {
            longName = EDV.ALT_LONGNAME;
            combinedAttributes.set("long_name", longName);
        }
        combinedAttributes.set("positive", "up"); //cf
        combinedAttributes.set("standard_name", ALT_STANDARD_NAME);
        combinedAttributes.set("units", units);        
        metersPerSourceUnit = tMetersPerSourceUnit; 

        //set destinationMin max  if not set by tSourceMin,Max
        double mm[] = extractActualRange(); //always extract 
        if (Double.isNaN(destinationMin)) destinationMin = mm[0] * scaleFactor + addOffset;
        if (Double.isNaN(destinationMax)) destinationMax = mm[1] * scaleFactor + addOffset;

        //convert destinationMin,max to meters,up
        destinationMin = sourceAltToMetersUp(destinationMin);  
        destinationMax = sourceAltToMetersUp(destinationMax);
        if (destinationMin > destinationMax) {
            double d1 = destinationMin; destinationMin = destinationMax; destinationMax = d1;
        }
        setActualRangeFromDestinationMinMax();

        //convert destinationMissingValue and destinationFillValue to meters,up
        //they have already been adjusted for scaleAddOffset (including destinationDataType)
        destinationMissingValue     = sourceAltToMetersUp(destinationMissingValue);
        destinationFillValue        = sourceAltToMetersUp(destinationFillValue);
        safeDestinationMissingValue = sourceAltToMetersUp(safeDestinationMissingValue);       
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
        return
            "EDVAlt/" + super.toString() + 
            "  metersPerSourceUnit=" + metersPerSourceUnit +
            "\n"; 
    }

    /**
     * This is used by the EDD constructor to determine if this
     * EDV is valid.
     *
     * @throws Throwable if this EDV is not valid
     */
    public void ensureValid(String errorInMethod) throws Throwable {
        super.ensureValid(errorInMethod);
        errorInMethod += "\ndatasets.xml/EDVAlt.ensureValid error for soureName=" + sourceName + ":\n";
        Test.ensureTrue(!Double.isNaN(metersPerSourceUnit), 
            errorInMethod + "'metersPerSourceUnit' wasn't set.");

    }

    /** 
     * This returns the factor needed to convert source units to/from meters 
     * above sea level.
     *
     * @return the factor needed to convert source units to/from meters
     *  above sea level.
     */
    public double metersPerSourceUnit() {return metersPerSourceUnit;}

    /**
     * This converts a source altitude value to meters above sea level.
     *
     * @param sourceAlt
     * @return meters above sea level.
     *  If sourceAlt is NaN or Math2.almostEqual(5, sourceAlt, sourceMissingValue) 
     *  (which is very lenient), this returns NaN.
     */
    public double sourceAltToMetersUp(double sourceAlt) {
        if (Double.isNaN(sourceAlt))
            return Double.NaN;
        if (!Double.isNaN(sourceMissingValue) && Math2.almostEqual(5, sourceAlt, sourceMissingValue))
            return Double.NaN;
        return sourceAlt == 0? 0: sourceAlt * metersPerSourceUnit;  //avoid making -0
    }

    /**
     * This converts meters above sea level to a source altitude value.
     *
     * @param meters above sea level.
     * @return a source altitude value. 
     *  If metersUp is NaN, this returns sourceMissingValue.
     */
    public double metersUpToSourceAlt(double metersUp) {
        if (Double.isNaN(metersUp))
            return sourceMissingValue;
        return metersUp == 0? 0: metersUp / metersPerSourceUnit; //avoid making -0
    }

    /**
     * This returns a PrimitiveArray (the original if the data type wasn't changed)
     * with source values converted to destinationValues.
     * This doesn't change the order of the values.
     *
     * <p>This version deals with scaleAddOffset and metersPerSourceUnit.
     * 
     * @param source
     * @return a PrimitiveArray (the original if the data type wasn't changed)
     * with source values converted to destinationValues.
     */
    public PrimitiveArray toDestination(PrimitiveArray source) {
        PrimitiveArray pa = super.toDestination(source);        
        pa.scaleAddOffset(metersPerSourceUnit, 0);
        return pa;
    }

    /**
     * This returns a PrimitiveArray (the original if the data type wasn't changed)
     * with destination values converted to sourceValues.
     * This doesn't change the order of the values.
     *
     * <p>This version deals with scaleAddOffset and metersPerSourceUnit.
     * 
     * @param destination
     * @return a PrimitiveArray (the original if the data type wasn't changed)
     * with destination values converted to sourceValues.
     */
    public PrimitiveArray toSource(PrimitiveArray destination) {
        destination.scaleAddOffset(1/metersPerSourceUnit, 0);
        return super.toSource(destination);
    }


}
