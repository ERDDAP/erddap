/* 
 * EDVGridAxis Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.erddap.util.EDStatic;


/** 
 * This class (or a subclass like EDVTimeGridAxis) holds information about a 
 * grid axis variable.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDVGridAxis extends EDV { 

    protected PrimitiveArray sourceValues;
    protected boolean isAscending = false;
    protected boolean isEvenlySpaced = false;
    protected double averageSpacing = Double.NaN;
    /** The destination coarse minimum and maximum values (in standardized destination units) 
     * of this variable. 
     * destinationCoarseMin/Max defines the slightly larger range 
     * (by averageSpacing/2 on each end) of valid requests.
     */
    protected double destinationCoarseMin = Double.NaN;
    protected double destinationCoarseMax = Double.NaN;

    /**
     * The constructor.
     * The "units" attribute must be in tSourceAttributes or tAddAttributes.
     *
     * <p>Call setActualRangeFromDestinationMinMax() sometime after this returns.
     *
     * @param tSourceName the name of the axis variable in the dataset source
     *    (usually with no spaces).
     *    Currently, this doesn't support fixedValue-style names.
     * @param tDestinationName is the name to be used in the results.
     *    If null or "", tSourceName will be used.
     * @param tSourceAttributes are the attributes for the variable
     *    in the source
     * @param tAddAttributes the attributes which will be added when data is 
     *    extracted and which have precedence over sourceAttributes.
     *    Special case: value="null" causes that item to be removed from combinedAttributes.
     *    If this is null, an empty addAttributes will be created.
     * @param tSourceValues has the values from the source.
     *    This can't be a StringArray.
     *    There must be at least one element.
     *    They must be sorted in ascending (recommended) or descending order.  Unsorted is not allowed.
     *    There can't be any missing values (or NaN).
     * @throws Throwable if trouble
     */
    public EDVGridAxis(String tSourceName, String tDestinationName,
        Attributes tSourceAttributes, Attributes tAddAttributes, 
        PrimitiveArray tSourceValues) 
        throws Throwable {

        super(tSourceName, tDestinationName,
            tSourceAttributes, tAddAttributes, 
            tSourceValues.elementClassString(), 
            tSourceValues.getNiceDouble(0), 
            tSourceValues.getNiceDouble(tSourceValues.size() - 1));
        
        sourceValues = tSourceValues;

        //test if ascending
        //Note that e.g., altitude might be flipped, so destination might be descending. That's ok.
        String error = sourceValues.isAscending(); 
        if (verbose && error.length() > 0)
            String2.log("  " + destinationName + ": " + error);
        isAscending = error.length() == 0;

        //if !isAscending, test that it is descending sorted
        if (!isAscending) {
            String error2 = sourceValues.isDescending();
            if (error2.length() > 0) 
                throw new RuntimeException("AxisVariable=" + destinationName + " isn't sorted.  " + 
                    error + "  " + error2);
        }

        //test for ties (after isAscending and isDescending)
        int firstTie = sourceValues.firstTie();
        if (firstTie >= 0)
            throw new RuntimeException("AxisVariable=" + destinationName + 
                " has tied values: #" + firstTie + " and #" + (firstTie + 1) + 
                " both equal " + sourceValues.getNiceDouble(firstTie) + ".");

        //test if evenly spaced
        error = sourceValues.isEvenlySpaced();
        if (verbose && error.length() > 0)
            String2.log("  " + destinationName + ": " + error);
        isEvenlySpaced = error.length() == 0;

        initializeAverageSpacingAndCoarseMinMax();
    }

    /** Some constructors call this to set destinationCoarseMin/Max
     * based on destinationMin/Max and averageSpacing.
     */
    public void initializeAverageSpacingAndCoarseMinMax() {
        int n = sourceValues.size();
        double rough;
        if (n >= 2) {  //averageSpacing may be negative (if axis is high to low)
            averageSpacing = (lastDestinationValue() - firstDestinationValue()) / (n - 1);
            rough = Math.abs(averageSpacing) / 2;
        } else {
            //avoid single value e.g., .01, fails to match .01000000001
            rough = Math.max(Math.abs(destinationMin) / 100, 0.01); //very arbitrary
        }
        destinationCoarseMin = destinationMin - rough;   
        destinationCoarseMax = destinationMax + rough;
    }

    /** 
     * This overwrites EDV superclass method to use firstDestinationValue and lastDestinationValue.
     * "actual_range" is defined in [CDC COARDS] as
     * "actual data range for variable. Same type as unpacked values."
     * Later, it says "The range values are used to indicate order of storage 
     * (e.g., 90,-90 would indicate the latitudes started with 90 and ended with -90)."
     */
    public void setActualRangeFromDestinationMinMax() {

        //actual_range is useful information for .das and will be replaced by actual_range of data subset.
        combinedAttributes.remove("data_min");
        combinedAttributes.remove("data_max");
        if (Double.isNaN(destinationMin) && Double.isNaN(destinationMax)) {
            combinedAttributes.remove("actual_range");
        } else {
            PrimitiveArray pa = PrimitiveArray.factory(destinationDataTypeClass(), 2, false);
            pa.addDouble(firstDestinationValue());
            pa.addDouble(lastDestinationValue());
            combinedAttributes.set("actual_range", pa);
        }
    }

    /**
     * This returns a string representation of this EDVGridAxis.
     *
     * @return a string representation of this EDVGridAxis.
     */
    public String toString() {
        return "EDVGridAxis/" + super.toString() + //has trailing newline
              "  nValues=" + sourceValues.size() +
            "\n  isAscending=" + isAscending +
            "\n  isEvenlySpaced=" + isEvenlySpaced +
            "\n  averageSpacing=" + averageSpacing +
            "\n  destinationCoarseMin=" + destinationCoarseMin +
            "\n  destinationCoarseMax=" + destinationCoarseMax + 
            "\n"; 
        //show sourceValues?
    }

    /**
     * This is used by the EDVGridAxis constructor to determine if this
     * EDV is valid.
     *
     * @param errorInMethod the start string for an error message
     * @throws Throwable if this EDV is not valid
     */
    public void ensureValid(String errorInMethod) throws Throwable {
        super.ensureValid(errorInMethod);
        Test.ensureTrue(sourceValues != null && sourceValues.size() > 0,
            errorInMethod + "'sourceValues' is null or has 0 values.");
        //ensure no null values???
    }

    /**
     * This is used by the EDVGridAxis constructor to determine if this
     * EDVGridAxis is valid.
     *
     * @throws Throwable if this EDVGridAxis is not valid
     */
    public void ensureValid() throws Throwable {
        String errorInMethod = "datasets.xml/EDVGridAxis.ensureValid error for sourceName=" + sourceName + ":\n";
        ensureValid(errorInMethod);
    }


    /** This returns the PrimitiveArray with the values for this axis 
     * as stored in the source. 
     * Don't change these values.
     */
    public PrimitiveArray sourceValues() {return sourceValues;}

    /**
     * This returns the PrimitiveArray with the destination values for this axis. 
     * Don't change these values.
     * This returns the sourceValues (with scaleFactor and 
     * addOffset if active; alt is special; time is special). 
     * This doesn't change the order of the values (even if source is depth and 
     * dest is altitude).
     */
    public PrimitiveArray destinationValues() {
        return toDestination(sourceValues); //alt and time may modify the values, so use sourceValues.clone()
    }

    /**
     * This returns one of the destination values for this axis 
     * (with scaleFactor and addOffset if active; alt is special; time is special). 
     * This returns a number.
     * This relies on alt and time overriding toDestination().
     */
    public PrimitiveArray destinationValue(int which) {
        PrimitiveArray sourceVal = PrimitiveArray.factory(destinationDataTypeClass, 1, false);

        sourceVal.addDouble(sourceValues.getNiceDouble(which)); 
        return toDestination(sourceVal); 
    }

    /**
     * This returns the PrimitiveArray with the destination values for this axis
     * which will return nice Strings if you call pa.getString(i). 
     * Don't change these values.
     * For most EDVGridAxis, this returns destinationValues (which equal
     * the String destination values). The Time subclass overrides this.
     */
    public PrimitiveArray destinationStringValues() {return destinationValues();}

    /**
     * This returns a JSON-style csv String with a subset of destinationStringValues
     * suitable for use on a slider with SLIDER_PIXELS.
     * This overwrites the superclass version so that it just presents valid values.
     * 
     * <p>Because there are always numbers for EDVGridAxes, this always returns a valid list.
     * <b>If the values range from high to low, this returns a high to low list.
     */
    public String sliderCsvValues() {
        if (sliderCsvValues != null) 
            return String2.utf8ToString(sliderCsvValues);
        
        //one time: generate the sliderCsvValues   since time and memory intensive
        try {
            PrimitiveArray destStrings = destinationStringValues();
            int nValues = destStrings.size();
            int start = 0;
            int stride = 1;
            boolean isTime = this instanceof EDVTimeGridAxis;

            if (nValues <= SLIDER_MAX_NVALUES) {
                //use start=0 and stride=1
            } else {
                //need to find a subset
                //find base with nice round number
                int base = 0;
                //spans 0?
                if (destinationMin <= 0 && destinationMax >= 0) {
                    base = destinationToClosestSourceIndex(0);
                } else if (isTime) {
                    //time strings are all of same length
                } else {
                    //look for shortest string in first 50 values
                    int shortestLength = toSliderString(destStrings.getString(0), isTime).length();
                    for (int i = 1; i < 50; i++) { //if nValues <= 50, then it was handled above
                        int tLength = toSliderString(destStrings.getString(i), isTime).length();
                        if (tLength < shortestLength) { //not <=, look for improvement
                            base = i;
                            shortestLength = tLength;
                        }
                    }
                }

                //figure out a good stride to give <=SLIDER_MAX_NVALUES values
                stride = Math2.hiDiv(nValues, SLIDER_MAX_NVALUES);
                int shortestLength = toSliderString(destStrings.getString(base + stride), isTime).length();
                int oStride2 = 2 * stride;

                //make stride bigger (up to oStride2) to catch shorter strings?
                for (int tStride = stride; tStride < oStride2; tStride++) { 
                    int i = base + tStride;
                    if (i >= destStrings.size()) break;
                    int tLength = toSliderString(destStrings.getString(i), isTime).length();
                    if (tLength < shortestLength) {  //not <=, look for improvement
                        stride = tStride;
                        shortestLength = tLength;
                    }
                }

                //next best: check 2*stride (and fall back to stride)
                for (int tStride = stride; tStride < oStride2; tStride++) { 
                    int i = base + 2*tStride;
                    if (i >= destStrings.size()) break;
                    int tLength = toSliderString(destStrings.getString(i), isTime).length();
                    if (tLength < shortestLength) {  //not <=, look for improvement
                        stride = tStride;
                        shortestLength = tLength;
                    }
                }

                //work back to start
                start = base % stride;
            }

            //gather the values
            StringBuilder sb = new StringBuilder();
            String s;
            int count = 0;
            //include the first value if start!=0
            if (start != 0) {
                sb.append(toSliderString(destStrings.getString(0), isTime));
                count++;
            }
            while (start < nValues) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(toSliderString(destStrings.getString(start), isTime));
                start += stride;
                count++;
            }
            //include the last value (if not done already)
            if (start - stride != nValues - 1) { 
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(toSliderString(destStrings.getString(nValues - 1), isTime));
                count++;
            }

            //store in compact utf8 format
            String csv = sb.toString();
            sliderCsvValues = String2.getUTF8Bytes(csv);
            if (reallyVerbose) String2.log("EDVGridAxis.sliderCsvValues nValues=" + nValues + 
                " start=" + start + " stride=" + stride + " nValues=" + count);
            sliderNCsvValues = count; //do last       
            return csv;
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return null;
        }
    }

    /**
     * This converts a value to the nearest slider position (0 .. EDV.SLIDER_PIXELS-1).
     * Out-of-range values (even far off) are converted to nearest, but NaN -> -1.
     * If only one destination value, this returns 0.
     *
     * <p>For EDVGridAxis (which overwrites the superclass version), this works whether isAscending or not.
     * 
     * 
     * @param destinationValue
     * @return the nearest slider position (0 .. EDV.SLIDER_PIXELS-1)
     *   (or -1 if trouble, e.g., sliderCsvValues can't be constructed (e.g., no min + max values)).
     */
    public int closestSliderPosition(double destinationValue) {
        int index = destinationToClosestSourceIndex(destinationValue);
        if (index == -1)
            return index;

        //it's a valid index
        int safeSourceSize1 = Math.max(1, sourceValues.size() - 1);
        return Math2.roundToInt((index * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
    }

    /**
     * This converts a destination double value to a string
     * (time variable override this to make an iso string).
     * NaN returns "";
     *
     * @param destD
     * @return destination String
     */
    public String destinationToString(double destD) {
        if (Double.isNaN(destD)) return "";
        //destinationDataTypeClass won't be String.class
        if (destinationDataTypeClass == double.class)  return "" + destD;
        if (destinationDataTypeClass == float.class)   return "" + (float)destD;
        return "" + Math.rint(destD);  //ints are nicer without trailing ".0"
    }

    /**
     * This converts a destination String value to a destination double
     * (time variable overrides this to catch iso 8601 strings).
     * "" or null returns NaN.
     *
     * @param destS
     * @return destination double
     */
    public double destinationToDouble(String destS) {
        return String2.parseDouble(destS);
    }

    /** 
     * This returns the nice double representation of the first destination value for this axis.
     */
    public double firstDestinationValue() {
        return destinationValue(0).getNiceDouble(0);
    }

    /** 
     * This returns the nice double representation of the last destination value for this axis.
     */
    public double lastDestinationValue() {
        return destinationValue(sourceValues.size() - 1).getNiceDouble(0);
    }

    /** 
     * This returns the destinationCoarseMin value (in standardized units) for this axis (e.g., 
     * altitude values are in meters, positive=up 
     * and time values are in seconds since 1970-01-01T00:00:00Z).
     * destinationCoarseMin/Max defines the slightly larger range of valid requests.
     * 
     * @return the cleaned up destinationCoarseMin value for this axis.
     */
    public double destinationCoarseMin() {return destinationCoarseMin;}

    /** 
     * This returns the destinationCoarseMax value (in standardized units) for this axis (e.g., 
     * altitude values are in meters, positive=up 
     * and time values are in seconds since 1970-01-01).
     * destinationCoarseMin/Max defines the slightly larger range of valid requests.
     * 
     * @return the cleaned up destinationCoarseMax value for this axis.
     */
    public double destinationCoarseMax() {return destinationCoarseMax;}

    public void setDestinationCoarseMin(double tMin) {destinationCoarseMin = tMin;}
    public void setDestinationCoarseMax(double tMax) {destinationCoarseMax = tMax;}

    /** 
     * This returns true if the values are ascending (tied is ok); 
     * otherwise, it returns false (descending or unordered).
     *
     * @return true if the values are ascending (tied is ok); 
     * otherwise, it returns false (descending or unordered).
     */
    public boolean isAscending() {return isAscending;}

    /** 
     * If there are 2 or more values and the values are evenly spaced, 
     * this returns true; else it returns false.
     *
     * @return If there are 2 or more values and the values are evenly spaced, 
     * this returns true; else it returns false.
     */
    public boolean isEvenlySpaced() {return isEvenlySpaced;}

    /** 
     * If there are 2 or more values, this returns the average spacing between values 
     * (will be negative if axis is descending!).
     * If isEvenlySpaced, then these are evenly spaced.
     * For EDVTimeGridAxis, this is in epochSeconds.
     *
     * @return If there are 2 or more values, 
     * this returns the average spacing between values.
     */
    public double averageSpacing() {return averageSpacing;}

    /**
     * This returns a human-oriented description of the spacing of this EDVGridAxis. (May be negative.)
     */
    public String spacingDescription() {
        boolean isTime = destinationName.equals(EDV.TIME_NAME);
        if (sourceValues.size() == 1) 
            return "(" + EDStatic.EDDGridJustOneValue + ")";
        String s = isTime? 
            Calendar2.elapsedTimeString(Math.rint(averageSpacing()) * 1000) : 
            "" + Math2.floatToDouble(averageSpacing());
        return s + " (" +
            (isEvenlySpaced()? EDStatic.EDDGridEven : EDStatic.EDDGridUneven) +
            ")";
    }

    /**
     * This returns HTML suitable for a tooltip for this dimension.
     * The range will be from firstDestinationValue to lastDestinationValue 
     * (which is different from min to max if !ascending).
     */
    public String htmlRangeTooltip() {
        String tUnits = units();
        boolean isTime = destinationName.equals(EDV.TIME_NAME);
        if (tUnits == null || isTime)
            tUnits = "";
        if (sourceValues.size() == 1)
            return destinationName + " has 1 value: " + destinationToString(firstDestinationValue()) + 
                " " + tUnits; 

        String tSpacing = isTime? 
            Calendar2.elapsedTimeString(Math.rint(averageSpacing()) * 1000) : 
            "" + Math2.floatToDouble(averageSpacing()) + " " + tUnits;
        return 
            destinationName + " has " + sourceValues.size() + " values<br>" +
            "ranging from " + destinationToString(firstDestinationValue()) + 
                     " to " + destinationToString(lastDestinationValue()) + " " + tUnits + "<br>" +
            "with " + 
                (isEvenlySpaced()? EDStatic.EDDGridEven : EDStatic.EDDGridUneven) + 
                " spacing " + (isEvenlySpaced()? "" : "~") +
                "= " + tSpacing;
    }

    /**
     * This converts a destination value (time must be in epoch seconds)
     * to the closest source index.
     * Out of range values are converted to closest source index (even if way off).
     * NaN returns -1.
     * This works whether isAscending or not.
     * !!!If there are ties, this doesn't specify which of the tied values will be found
     *   (which is part of why EDVGridAxis doesn't allow ties).
     *
     * @param destinationD
     * @return the closest source index
     */
    public int destinationToClosestSourceIndex(double destinationD) {
        if (Double.isNaN(destinationD))
            return -1;

        DoubleArray destDA = new DoubleArray(new double[]{destinationD});
        PrimitiveArray sourcePA = toSource(destDA);
        double sourceD = sourcePA.getNiceDouble(0); //all grid sources are numeric
        if (isAscending)
            return sourceValues.binaryFindClosest(sourceD);
        return sourceValues.linearFindClosest(sourceD);
    }


}
