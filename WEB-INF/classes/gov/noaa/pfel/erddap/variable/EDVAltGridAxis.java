/* 
 * EDVAltGridAxis Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Test;

/** 
 * This class holds information about an altitude grid axis variable.
 * 
 * <p>The order of conversion is: source data &gt; scale_factor and add_offset &gt;
 * altitudeMetersPerSourceUnit.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDVAltGridAxis extends EDVGridAxis { 
   
    protected double metersPerSourceUnit = Double.NaN;

    /**
     * The constructor.
     *
     * @param tSourceName the name of the axis variable in the dataset source
     *    (usually with no spaces).
     * @param tSourceAttributes are the attributes for the variable
     *    in the source
     * @param tAddAttributes the attributes which will be added when data is 
     *    extracted and which have precedence over sourceAttributes.
     *    Special case: value="null" causes that item to be removed from combinedAttributes.
     *    If this is null, an empty addAttributes will be created.
     * @param tSourceValues has the values from the source.
     *    This can't be a StringArray.
     *    There must be at least one element.
     * @param tMetersPerSourceUnit the conversion factor needed to convert
     *    the de-scaled/offset source altitude values to/from meters above sea level.
     *    A positive value indicates the source is positive is up.
     *    A negative value indicates the source is positive is down.
     * @throws Throwable if trouble
     */
    public EDVAltGridAxis(String tSourceName, 
        Attributes tSourceAttributes, Attributes tAddAttributes, 
        PrimitiveArray tSourceValues, double tMetersPerSourceUnit) 
        throws Throwable {

        super(tSourceName, ALT_NAME, tSourceAttributes, tAddAttributes, tSourceValues); 

        longName = ALT_LONGNAME;
        units = ALT_UNITS;
        combinedAttributes.set("_CoordinateAxisType", "Height");   //unidata
        combinedAttributes.set("_CoordinateZisPositive", "up");  //unidata
        combinedAttributes.set("axis", "Z");
        combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
        combinedAttributes.set("long_name", longName);
        combinedAttributes.set("positive", "up"); //cf
        combinedAttributes.set("standard_name", ALT_STANDARD_NAME);
        combinedAttributes.set("units", units);

        //remember that gridAxes get min max from actual axis tSourceValues
        metersPerSourceUnit = tMetersPerSourceUnit; 
        destinationMin = sourceAltToMetersUp(destinationMin);
        destinationMax = sourceAltToMetersUp(destinationMax);
        if (destinationMin > destinationMax) {
            double d1 = destinationMin; destinationMin = destinationMax; destinationMax = d1; 
        }
        setActualRangeFromDestinationMinMax();
        initializeAverageSpacingAndCoarseMinMax();
        //no need to deal with missingValue stuff, since gridAxis can't have mv's
    }

    /**
     * This returns a string representation of this EDV.
     *
     * @param errorInMethod the start string for an error message
     * @return a string representation of this EDV.
     */
    public String toString() {
        return
            "Alt " + super.toString() + 
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
        errorInMethod += "\ndatasets.xml/EDVAltGridAxis.ensureValid error for sourceName=" + sourceName + ":\n";
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
     *  If sourceAlt is NaN, this returns NaN (but there shouldn't ever be missing values).
     */
    public double sourceAltToMetersUp(double sourceAlt) {
        return sourceAlt == 0? 0 : sourceAlt * metersPerSourceUnit; //avoid creating -0
    }

    /**
     * This converts a PrimitiveArray with source altitude value to meters above sea level.
     *
     * @param sourceAlt
     */
    public void sourceAltToMetersUp(PrimitiveArray sourceAlt) {
        int n = sourceAlt.size();
        for (int i = 0; i < n; i++) {
            double a = sourceAlt.getDouble(i);
            if (a != 0) //avoid creating -0
                sourceAlt.setDouble(i, a * metersPerSourceUnit); 
        }
    }

    /**
     * This converts meters above sea level to a source altitude value.
     *
     * @param meters above sea level.
     * @return a source altitude value. 
     *  If metersUp is NaN, this returns NaN (but there shouldn't ever be missing values).
     */
    public double metersUpToSourceAlt(double metersUp) {
        return metersUp == 0? 0 : metersUp / metersPerSourceUnit; //avoid creating -0
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

    /**
     * This returns the PrimitiveArray with the destination values for this axis. 
     * Don't change these values.
     * This returns the sourceValues (with scaleFactor and 
     * addOffset if active; alt is special; time is special). 
     * This doesn't change the order of the values (even if source is depth and 
     * dest is altitude).
     */
    public PrimitiveArray destinationValues() {
        //alt and time may modify the values, so use sourceValues.clone()
        return toDestination((PrimitiveArray)sourceValues.clone()); 
    }

}
