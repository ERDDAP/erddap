/* 
 * EDVTimeStamp Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

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
import gov.noaa.pfel.erddap.util.EDStatic;

import java.util.GregorianCalendar;

import org.joda.time.*;
import org.joda.time.format.*;

/** 
 * This class holds information about *a* (not *the*) time variable,
 * which is like EDV, but the destinationName, long_name, and units
 * are standardized, and you need to specify the sourceTimeFormat so
 * that source values can be converted to seconds since 1970-01-01 in the results.
 *
 * <p>There is the presumption, not requirement, that if there are two time-related
 * variables, the main one will be EDVTime and the secondary will be EDVTimeStamp
 * (not both EDVTimeStamp). 
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 *   Converted from EDVTime 2009-06-29.
 */
public class EDVTimeStamp extends EDV { 

    /** Format for ISO date time without a suffix (assumed to be UTC) */
    public final static String ISO8601T_FORMAT  = "yyyy-MM-dd'T'HH:mm:ss"; 
    public final static String ISO8601T3_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS"; 

    /** special case format supports suffix 'Z' or +/-HH:MM */
    public final static String ISO8601TZ_FORMAT  = "yyyy-MM-dd'T'HH:mm:ssZ"; 
    public final static String ISO8601T3Z_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; 

    /** Set by the constructor. */
    protected String sourceTimeFormat; 

    /** These are set automatically. */
    protected boolean sourceTimeIsNumeric;
    protected double sourceTimeBase = Double.NaN;  //set if sourceTimeIsNumeric
    protected double sourceTimeFactor = Double.NaN;
    protected DateTimeFormatter dateTimeFormatter; //set if !sourceTimeIsNumeric
    protected String time_precision;  //see Calendar2.limitedEpochSecondsToIsoStringT
 
    /**
     * This class holds information about the time variable,
     * which is like EDV, but the destinationName is forced to be "time" 
     * and the destination units are standardized to seconds since 1970-01-01 
     * in the results.
     *
     * <p>Either tAddAttributes (read first) or tSourceAttributes must have "units"
     *    which is either <ul>
     *    <li> a UDUunits string (containing " since ")
     *      describing how to interpret source time values 
     *      (e.g., "seconds since 1970-01-01T00:00:00")
     *      where the base time is an 
     *      ISO 8601 formatted date time string (YYYY-MM-DDThh:mm:ss).
     *    <li> a org.joda.time.format.DateTimeFormat string
     *      (which is compatible with java.text.SimpleDateFormat) describing how to interpret 
     *      string times  (e.g., the ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ss.SSSZ", see 
     *      http://joda-time.sourceforge.net/api-release/index.html or 
     *      http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html),
     *    </ul>
     * actual_range, data_min, or data_max metadata (if any) should have the min and max times
     * in 'units' format.
     *
     * @param tDestinationName should be "time" for *the* destination variable (type=EDVTime),
     *   otherwise some other name.
     * @throws Throwable if trouble
     */
    public EDVTimeStamp(String tSourceName, String tDestinationName,
        Attributes tSourceAttributes, Attributes tAddAttributes,
        String tSourceDataType) 
        throws Throwable {

        super(tSourceName, tDestinationName, tSourceAttributes, tAddAttributes,
            tSourceDataType, 
            Double.NaN, Double.NaN); //destinationMin and max are set below (via actual_range)

        //time_precision e.g., 1970-01-01T00:00:00Z
        time_precision = combinedAttributes.getString(EDV.time_precision);
        if (time_precision != null) {
            //ensure not just year (can't distinguish user input a year vs. epochSeconds)
            if (time_precision.equals("1970"))
               time_precision = null;
            //ensure Z at end of time
            if (time_precision.length() >= 13 && !time_precision.endsWith("Z"))
               time_precision = null;
        }
        
        //currently, EDVTimeStamp doesn't support scaleAddOffset
        String errorInMethod = "datasets.xml/EDVTimeStamp error for sourceName=" + tSourceName + ":\n";
        if (scaleAddOffset)
            throw new RuntimeException(errorInMethod +
                "Currently, EDVTimeStamp doesn't support scale_factor and add_offset.");

        //special processing of sourceTimeFormat  (before change units below)
        sourceTimeFormat = units();  
        Test.ensureNotNothing(sourceTimeFormat, 
            errorInMethod + "'units' wasn't set."); 
        if (!hasTimeUnits(sourceTimeFormat)) 
            throw new RuntimeException(errorInMethod +
                "units=" + sourceTimeFormat + " isn't a valid time format.");

        if (destinationName.equals(TIME_NAME)) { //*the* time variable
            combinedAttributes.set("_CoordinateAxisType", "Time"); //unidata-related
            combinedAttributes.set("axis", "T");
            String sn = combinedAttributes.getString("standard_name");
            if (sn == null || sn.length() == 0)
                combinedAttributes.set("standard_name", TIME_STANDARD_NAME);
        }
        combinedAttributes.set("ioos_category", TIME_CATEGORY);
        combinedAttributes.set("time_origin", "01-JAN-1970 00:00:00");
        units = EDV.TIME_UNITS; 
        combinedAttributes.set("units", units);
        longName = combinedAttributes.getString("long_name");
        if (longName == null || longName.toLowerCase().equals("time")) //catch nothing or alternate case
            combinedAttributes.set("long_name", TIME_LONGNAME);
        longName = combinedAttributes.getString("long_name");

        if (sourceTimeFormat.indexOf(" since ") > 0) {
            sourceTimeIsNumeric = true;
            double td[] = Calendar2.getTimeBaseAndFactor(sourceTimeFormat);
            sourceTimeBase = td[0];
            sourceTimeFactor = td[1];
        } else {
            sourceTimeIsNumeric = false;
            if (sourceTimeFormat.equals(ISO8601TZ_FORMAT)) {
                String2.log("Using special ISO8601TZ_FORMAT.");
                dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);
            } else if (sourceTimeFormat.equals(ISO8601T3Z_FORMAT)) {
                String2.log("Using special ISO8601T3Z_FORMAT.");
                dateTimeFormatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);
            } else {
                //future: support time zones  
                dateTimeFormatter = DateTimeFormat.forPattern(sourceTimeFormat).withZone(DateTimeZone.UTC);
            }
        }

        //extract fixedValue (must be epochSeconds)
        //*** I think this has never been used so never tested !!!
        if (sourceName != null && sourceName.length() >= 2 &&
            sourceName.charAt(0) == '=') {

            fixedValue = extractFixedValue(sourceName);
            sourceTimeIsNumeric = true;
            sourceTimeBase = 0;
            sourceTimeFactor = 1;
        }

        //then set missing_value  (as double.class)
        destinationDataType = "double";
        destinationDataTypeClass = double.class;
        destinationMissingValue     = sourceTimeToEpochSeconds(destinationMissingValue);
        destinationFillValue        = sourceTimeToEpochSeconds(destinationFillValue);
        safeDestinationMissingValue = sourceTimeToEpochSeconds(safeDestinationMissingValue);       
        PrimitiveArray pa = combinedAttributes.get("missing_value"); 
        if (pa != null) 
            combinedAttributes.set("missing_value", new DoubleArray(new double[]{destinationMissingValue}));
        pa = combinedAttributes.get("_FillValue"); 
        if (pa != null) 
            combinedAttributes.set("_FillValue", new DoubleArray(new double[]{destinationFillValue}));

        //actual_range may be strings(???), so can't use extractActualRange();
        if (isFixedValue()) {
            destinationMin = String2.parseDouble(fixedValue);  //epochSeconds
            destinationMax = destinationMin;
        } else {
            String tMin = combinedAttributes.getString("data_min");
            String tMax = combinedAttributes.getString("data_max");
            if (Double.isNaN(destinationMin) && tMin != null && tMin.length() > 0) destinationMin = sourceTimeToEpochSeconds(tMin);
            if (Double.isNaN(destinationMax) && tMax != null && tMax.length() > 0) destinationMax = sourceTimeToEpochSeconds(tMax);

            PrimitiveArray actualRange = combinedAttributes.get("actual_range");
            if (actualRange != null) {
                //if (verbose) String2.log("  actual_range metadata for " + destinationName + ": " + actualRange);
                if (actualRange.size() == 2) {
                    if (Double.isNaN(destinationMin)) destinationMin = sourceTimeToEpochSeconds(actualRange.getString(0));
                    if (Double.isNaN(destinationMax)) destinationMax = sourceTimeToEpochSeconds(actualRange.getString(1));
                    if (!Double.isNaN(destinationMin) && 
                        !Double.isNaN(destinationMax) &&
                        destinationMin > destinationMax) {
                        double d = destinationMin; 
                        destinationMin = destinationMax; 
                        destinationMax = d; 
                    }
                }
            }
        }
        combinedAttributes.remove("data_min");
        combinedAttributes.remove("data_max");
        combinedAttributes.remove("actual_range");
        setActualRangeFromDestinationMinMax();
        //if (reallyVerbose) String2.log("\nEDVTimeStamp created, sourceTimeFormat=" + sourceTimeFormat);  
    }

    /**
     * This determines if a variable is a TimeStamp variable by looking
     * for " since " (used for UDUNITS numeric times) or 
     * "yy" or "YY" (a formatting string which has the year designator) in the units attribute.
     */
    public static boolean hasTimeUnits(Attributes sourceAttributes, Attributes addAttributes) {
        String tUnits = null;
        if (addAttributes != null) //priority
            tUnits = addAttributes.getString("units");
        if (tUnits == null && sourceAttributes != null)
            tUnits = sourceAttributes.getString("units");
        return hasTimeUnits(tUnits);
    }

    /**
     * This determines if a variable is a TimeStamp variable by looking
     * for " since " (used for numeric times) or 
     * "yy" or "YY" (a formatting string which has the year designator) in the units attribute.
     */
    public static boolean hasTimeUnits(String tUnits) {
        if (tUnits == null)
            return false;
        return tUnits.indexOf(" since ") > 0 ||
               tUnits.indexOf("yy") >= 0 ||
               tUnits.indexOf("YY") >= 0;
    }

    /**
     * This returns a string representation of this EDV.
     *
     * @return a string representation of this EDV.
     */
    public String toString() {
        return
            "EDVTimeStamp/" + super.toString() +
            "  sourceTimeFormat=" + sourceTimeFormat + "\n"; 
    }

    /**
     * This is used by the EDD constructor to determine if this
     * EDV is valid.
     *
     * @param errorInMethod the start string for an error message
     * @throws Throwable if this EDV is not valid
     */
    public void ensureValid(String errorInMethod) throws Throwable {
        super.ensureValid(errorInMethod);
        //errorInMethod += "\ndatasets.xml/EDVTimeStamp.ensureValid error for sourceName=" + sourceName + ":\n";
        //sourceTimeFormat is checked in constructor
    }

    /**
     * This converts a destination double value to an ISO string with "Z".
     * NaN returns "".
     *
     * @param destD
     * @return destination String
     */
    public String destinationToString(double destD) {
        return Calendar2.limitedEpochSecondsToIsoStringT(
            time_precision, destD, "");
    }

    /** 
     * This is the destinationMin time value in the dataset (as an ISO date/time string, 
     * e.g., "1990-01-01T00:00:00Z").  
     *
     * @return the destinationMin time (or "" if unknown)
     */
    public String destinationMinString() {
        return destinationToString(destinationMin); 
    }

    /** 
     * This is the destinationMax time value in the dataset (an ISO date/time string, 
     * e.g., "2005-12-31T23:59:59Z").  
     *
     * @return the destinationMax time (or "" if unknown)
     */
    public String destinationMaxString() {
        return destinationToString(destinationMax); 
    }

    /**
     * An indication of the precision of the time values, e.g., 
     * "1970-01-01T00:00:00Z" (default) or null (goes to default).  
     * See Calendar2.limitedEpochSecondsToIsoStringT()
     */
    public String time_precision() {
        return time_precision; 
    }

    /** 
     * @param tSourceTimeFormat is either<ul>
     *    <li> a udunits string (containing " since ")
     *      describing how to interpret numbers 
     *      (e.g., "seconds since 1970-01-01T00:00:00"),
     *    <li> a org.joda.time.format.DateTimeFormat string
     *      (which is compatible with java.text.SimpleDateFormat) describing how to interpret 
     *      string times  (e.g., the ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see 
     *      http://joda-time.sourceforge.net/api-release/index.html or 
     *      http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html),
     *    <li> null if this can be procured from the "units" source metadata.
     *    </ul>
     * Examples: 
     * <br>Date and Time Pattern    Result 
     * <br>"yyyy.MM.dd G 'at' HH:mm:ss z"    2001.07.04 AD at 12:08:56 PDT 
     * <br>"EEE, MMM d, ''yy"    Wed, Jul 4, '01 
     * <br>"yyyyy.MMMMM.dd GGG hh:mm aaa"    02001.July.04 AD 12:08 PM 
     * <br>"yyMMddHHmmssZ"    010704120856-0700 
     * <br>"yyyy-MM-dd'T'HH:mm:ss.SSSZ"    2001-07-04T12:08:56.235-0700     
     *
     * @return the source time's units
     */
    public String sourceTimeFormat() {return sourceTimeFormat;}


    /**
     * This returns true if the source time is numeric or if fixedValue is in use.
     *
     * @return true if the source time is numeric or if fixedValue is in use.
     */
    public boolean sourceTimeIsNumeric() {
        return sourceTimeIsNumeric;
    }

    /**
     * If sourceTimeIsNumeric, this converts a source time to a destination ISO T time.
     *
     * @param sourceTime a numeric sourceTime
     * @return seconds since 1970-01-01T00:00:00.
     *  If sourceTime is NaN or Math2.almostEqual(5, sourceTime, sourceMissingValue) 
     *  (which is very lenient), this returns NaN.
     */
    public double sourceTimeToEpochSeconds(double sourceTime) {
        if (!Double.isNaN(sourceMissingValue) && Math2.almostEqual(5, sourceTime, sourceMissingValue))
            return Double.NaN;
        return Calendar2.unitsSinceToEpochSeconds(sourceTimeBase, sourceTimeFactor, sourceTime);
    }

    /**
     * If sourceTimeIsNumeric or not, this converts a source time to 
     * seconds since 1970-01-01T00:00:00Z.
     *
     * @param sourceTime either a number (as a string) or a string
     * @return the source time converted to seconds since 1970-01-01T00:00:00Z.
     *   This returns NaN if trouble (sourceMissingValue, "", or invalid format).
     */
    public double sourceTimeToEpochSeconds(String sourceTime) {
        //sourceTime is numeric
        if (sourceTimeIsNumeric) {
            return sourceTimeToEpochSeconds(String2.parseDouble(sourceTime));
        }

        //time is a string
        //parse with Joda
        try {
            double d = dateTimeFormatter.parseMillis(sourceTime) / 1000.0; //thread safe
            //String2.log("  EDVTimeStamp sourceTime=" + sourceTime + " epSec=" + d + " Calendar2=" + Calendar2.epochSecondsToIsoStringT(d));
            return d;
        } catch (Throwable t) {
            if (verbose && sourceTime != null && sourceTime.length() > 0)
                String2.log("  EDVTimeStamp.sourceTimeToEpochSeconds: Invalid sourceTime=" + 
                    sourceTime + "\n" + t.toString());
            return Double.NaN;
        }

/*
        //parse with Java -- this wasn't working right for iso format!
        synchronized (simpleDateFormat) {
            //if error, date will be null
            ParsePosition pp = new ParsePosition(0);
            Date date = simpleDateFormat.parse(sourceTime, pp); //returns null if trouble
String2.log("sourceTime=" + sourceTime + 
    " format=" + sourceTimeFormat + " parsedAs=" + date + " pp=" + pp.getIndex());
            return date == null? Double.NaN : date.getTime() / 1000.0; 
        }
*/
    }

    /**
     * This returns a PrimitiveArray (the original if the data type wasn't changed)
     * with source values converted to destinationValues.
     * This doesn't change the order of the values.
     *
     * <p>This version currently doesn't support scaleAddOffset.
     * 
     * @param source
     * @return a PrimitiveArray (the original if the data type wasn't changed)
     * with source values converted to destinationValues.
     * Here, destination will be double epochSecond values.
     */
    public PrimitiveArray toDestination(PrimitiveArray source) {
        //this doesn't support scaleAddOffset
        int size = source.size();
        PrimitiveArray pa;
        if (source instanceof StringArray) {
            pa = new DoubleArray(size, true);
            for (int i = 0; i < size; i++)
                pa.setDouble(i, sourceTimeToEpochSeconds(source.getString(i)));
        } else {
            pa = source instanceof DoubleArray?
                source :
                new DoubleArray(size, true);
            for (int i = 0; i < size; i++)
                pa.setDouble(i, sourceTimeToEpochSeconds(source.getDouble(i)));
        }
        return pa;
    }

    /**
     * This returns a PrimitiveArray (the original if the data type wasn't changed)
     * with destination values converted to sourceValues.
     * This doesn't change the order of the values.
     *
     * <p>This version currently doesn't support scaleAddOffset.
     * 
     * @param destination   epochSecond double values
     * @return a PrimitiveArray (the original if the data type wasn't changed)
     * with destination values converted to sourceValues.
     */
    public PrimitiveArray toSource(PrimitiveArray destination) {
        //this doesn't support scaleAddOffset
        int size = destination.size();
        PrimitiveArray source = sourceDataTypeClass == destination.elementClass()?
            destination :
            PrimitiveArray.factory(sourceDataTypeClass, size, true);
        if (source instanceof StringArray) {
            for (int i = 0; i < size; i++)
                source.setString(i, epochSecondsToSourceTimeString(destination.getDouble(i)));
        } else {
            for (int i = 0; i < size; i++)
                source.setDouble(i, epochSecondsToSourceTimeDouble(destination.getDouble(i)));
        }
        return source;
    }


    /**
     * This converts a source time to a destination ISO TZ time.
     *
     * @param sourceTime either a number (as a string) or a string
     * @return an ISO T Time (e.g., 1993-12-31T23:59:59Z).
     *   If sourceTime is invalid or is sourceMissingValue, this returns "".
     */
    public String sourceTimeToIsoStringT(String sourceTime) {
        double d = sourceTimeToEpochSeconds(sourceTime);
        if (Double.isNaN(d) || Math2.almostEqual(5, sourceMissingValue, d))
            return "";
        return Calendar2.limitedEpochSecondsToIsoStringT(
            time_precision, sourceTimeToEpochSeconds(d), "");
    }

    /**
     * Call this if sourceTimeIsNumeric to convert epochSeconds to a numeric 
     * sourceTime.
     *
     * @param epochSeconds seconds since 1970-01-01T00:00:00.
     * @return sourceTime 
     *  If epochSeconds is NaN, this returns sourceMissingValue
     */
    public double epochSecondsToSourceTimeDouble(double epochSeconds) {
        if (Double.isNaN(epochSeconds))
            return sourceMissingValue;
        return Calendar2.epochSecondsToUnitsSince(sourceTimeBase, sourceTimeFactor, epochSeconds);
    }

    /**
     * Call this whether or not sourceTimeIsNumeric to convert epochSeconds to 
     * sourceTime (numeric, or via dateTimeFormatter).
     *
     * @param epochSeconds seconds since 1970-01-01T00:00:00.
     * @return the corresponding sourceTime (numeric, or via dateTimeFormatter).
     *    If epochSeconds is NaN, this returns sourceMissingValue (if sourceTimeIsNumeric)
     *    or "".
     */
    public String epochSecondsToSourceTimeString(double epochSeconds) {
        if (Double.isNaN(epochSeconds))
            return sourceTimeIsNumeric? "" + sourceMissingValue : "";
        return sourceTimeIsNumeric?
            "" + epochSecondsToSourceTimeDouble(epochSeconds) :
            dateTimeFormatter.print(Math.round(epochSeconds * 1000)); //round to long
    }


    /**
     * This returns a JSON-style csv String with a subset of destinationStringValues
     * suitable for use on a slider with SLIDER_PIXELS.
     * This overwrites the superclass version.
     *
     * <p>If destinationMin or destinationMax (except time) aren't finite,
     * this returns null.
     */
    public String sliderCsvValues() throws Throwable {
        if (sliderCsvValues != null) 
            return String2.utf8ToString(sliderCsvValues);

        try {
            boolean isTime = true;        
            double tMin = destinationMin;
            double tMax = destinationMax;
            if (!Math2.isFinite(tMin)) return null;
            if (!Math2.isFinite(tMax)) {
                //next midnight Z
                GregorianCalendar gc = Calendar2.newGCalendarZulu();
                Calendar2.clearSmallerFields(gc, Calendar2.DATE);
                gc.add(Calendar2.DATE, 1);
                tMax = Calendar2.gcToEpochSeconds(gc);
            }

            //get the values from Calendar2
            double values[] = Calendar2.getNEvenlySpaced(tMin, tMax, SLIDER_MAX_NVALUES);
            StringBuilder sb = new StringBuilder(toSliderString( //first value
                Calendar2.limitedEpochSecondsToIsoStringT(time_precision, tMin, ""), 
                isTime)); 
            int nValues = values.length;
            for (int i = 1; i < nValues; i++) { 
                sb.append(", ");
                sb.append(toSliderString(
                    Calendar2.limitedEpochSecondsToIsoStringT(time_precision, values[i], ""),
                    isTime));
            }

            //store in compact utf8 format
            String csv = sb.toString();
            sliderCsvValues = String2.getUTF8Bytes(csv);
            sliderNCsvValues = nValues; //do last
            if (reallyVerbose) String2.log("EDVTimeStamp.sliderCsvValues nValues=" + nValues);
            return csv;
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            String2.log(MustBe.throwableToString(t));
            return null;
        }
    }


    /**
     * This is a unit test.
     */
    public static void test() throws Throwable {
        verbose = true;

        //***with Z
        String2.log("\n*** test with Z");
        EDVTimeStamp eta = new EDVTimeStamp("sourceName", "time",
            null, 
            (new Attributes()).add("units", ISO8601TZ_FORMAT).
                add("actual_range", new StringArray(new String[]{"1970-01-01T00:00:00Z", "2007-01-01T00:00:00Z"})),
            "String");

        //test 'Z'
        String t1 = "2007-01-02T03:04:05Z";
        double d = eta.sourceTimeToEpochSeconds(t1);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(d)+"Z", t1, "a1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t1, "a2");

        //test -01:00
        String t2 = "2007-01-02T02:04:05-01:00";
        d = eta.sourceTimeToEpochSeconds(t2);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(d)+"Z", t1, "b1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t1, "b2");


        //***with 3Z
        String2.log("\n*** test with 3Z");
        eta = new EDVTimeStamp("sourceName", "time",
            null, 
            (new Attributes()).add("units", ISO8601T3Z_FORMAT).
                add("actual_range", new StringArray(new String[]{"1970-01-01T00:00:00.000Z", "2007-01-01T00:00:00.000Z"})),
            "String");

        //test 'Z'
        String t13 = "2007-01-02T03:04:05.123Z";
        d = eta.sourceTimeToEpochSeconds(t13);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT3(d)+"Z", t13, "a1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t13, "a2");

        //test -01:00
        String t23 = "2007-01-02T02:04:05.123-01:00";
        d = eta.sourceTimeToEpochSeconds(t23);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT3(d)+"Z", t13, "b1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t13, "b2");


        //*** no Z
        String2.log("\n*** test no Z");
        eta = new EDVTimeStamp("sourceName", "myTimeStamp",
            null, (new Attributes()).add("units", ISO8601T_FORMAT).  //without Z
                add("actual_range", new StringArray(new String[]{
                    "1970-01-01T00:00:00", "2007-01-01T00:00:00"})),  //without Z
            "String");

        //test no suffix    
        String t4 = "2007-01-02T03:04:05"; //without Z
        d = eta.sourceTimeToEpochSeconds(t4);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(d)+"Z", t1, "b1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString(d)+"Z", t1, "b2");


        //*** 3, no Z
        String2.log("\n*** test 3, no Z");
        eta = new EDVTimeStamp("sourceName", "myTimeStamp",
            null, (new Attributes()).add("units", ISO8601T3_FORMAT).  //without Z
                add("actual_range", new StringArray(new String[]{
                    "1970-01-01T00:00:00.000", "2007-01-01T00:00:00.000"})),  //without Z
            "String");

        //test no suffix    
        t4 = "2007-01-02T03:04:05.123"; //without Z
        d = eta.sourceTimeToEpochSeconds(t4);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT3(d)+"Z", t13, "b1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString( d)+"Z", t13, "b2");

    }
}
