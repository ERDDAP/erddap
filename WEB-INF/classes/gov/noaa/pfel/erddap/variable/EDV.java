/* 
 * EDV Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.EDUnits;

import java.util.GregorianCalendar;
import java.util.HashMap;


/** 
 * This class holds information about an ErdDap axis or data Variable (EDV).
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDV { 

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
     * if you want lots and lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    /**
     * Set this to true (by calling debug=true in your program, not by changing the code here)
     * if you want lots and lots of diagnostic messages sent to String2.log.
     */
    public static boolean debug = false; 

    /** 
     * These are the standardized variable names, long names, CF standard names, 
     * and units for the lon, lat, alt, and time axes in the results. 
     * These names are suggested by
     * http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html 
     * and match the CF standard names
     * (see http://cfconventions.org/Data/cf-standard-names/27/build/cf-standard-name-table.html).
     */
    public final static String
        LON_NAME  = "longitude", LON_LONGNAME  = "Longitude", LON_STANDARD_NAME  = "longitude", LON_UNITS  = "degrees_east",        
        LAT_NAME  = "latitude",  LAT_LONGNAME  = "Latitude",  LAT_STANDARD_NAME  = "latitude",  LAT_UNITS  = "degrees_north",
        ALT_NAME  = "altitude",  ALT_LONGNAME  = "Altitude",  ALT_STANDARD_NAME  = "altitude",  ALT_UNITS  = "m",
        DEPTH_NAME= "depth",     DEPTH_LONGNAME= "Depth",     DEPTH_STANDARD_NAME= "depth",     DEPTH_UNITS= "m",
        TIME_NAME = "time",      TIME_LONGNAME = "Time",      TIME_STANDARD_NAME = "time",      TIME_UNITS = Calendar2.SECONDS_SINCE_1970;
    public static String[] LON_UNITS_VARIANTS = {
        LON_UNITS, "degree_east", "degreeE", "degree_E", "degreesE", "degrees_E"};                
    public static String[] LAT_UNITS_VARIANTS = {
        LAT_UNITS, "degree_north", "degreeN", "degree_N", "degreesN", "degrees_N"};
    public static String[] METERS_VARIANTS = {
        ALT_UNITS, "meter", "meters", "metre", "metres"};

    /** */
    public static String TIME_UCUM_UNITS = EDUnits.udunitsToUcum(TIME_UNITS);

    /** The optional string for no units. 
     * There doesn't seem to be a udUnits standard. But LAS uses "unitless".*/
    public final static String UNITLESS = DataHelper.UNITLESS;

    /** The IOOS category for longitude, latitude, altitude, and other location-related variables. 
     * "Location" is better than "LonLatAlt" because it is more general. */
    public final static String LOCATION_CATEGORY = "Location"; 

    /** The IOOS category for time variables. */
    public final static String TIME_CATEGORY = "Time";

    /** 
     * These are the valid values for ioosCategory, in alphabetical order.
     * The core names are from Zdenka Willis' .ppt "Integrated Ocean Observing System 
     * (IOOS) NOAA's Approach to Building an Initial Operating Capability".
     * Bob(!) added many of these categories to deal with other types of data.
     * And updated 2011-05-19 with the 26 variables listed on pg 17 of
     * http://www.ioos.gov/library/us_ioos_blueprint_ver1.pdf (marked 
     * "added 2011-05-19")
     * <ul>
     * <li>"Other" is not one of the original categories, but is a fallback
     *   option if the category is not one of the pre-defined standards.
     * <li>"Unknown" is not one of the original categories, but is a fallback
     *   option if the category is not yet categorized (and needs to be).
     * <li>"Location" is used for longitude, latitude, and altitude variables.
     * <li>"Time" is used for the time variable.
     * </ul>
     */
    public final static String[] IOOS_CATEGORIES = {
        //!!! MAKING CHANGES?  Make the changes to the list in setupDatasetsXml.html, too.
        //??? need categories processing paramaters,
        "Bathymetry", 
        "Biology", //bob added
        "Bottom Character", 
        "CO2", //bob added pCO2 2011-05-19, 2011-10-11 changed to CO2
        "Colored Dissolved Organic Matter", //added 2011-05-19
        "Contaminants", "Currents", //was "Surface Currents" 
        "Dissolved Nutrients", "Dissolved O2",
        "Ecology", //bob added
        "Fish Abundance", "Fish Species", 
        "Heat Flux", 
        "Hydrology", //bob added 2011-02-07
        "Ice Distribution", "Identifier", 
        LOCATION_CATEGORY,  //bob added
        "Meteorology", //bob added; use if not Temperature or Wind
        "Ocean Color", "Optical Properties",  //what is dividing line?  OptProp is for atmosphere, too
        "Other", //bob added
        "Pathogens", 
        "Physical Oceanography", //Bob added 2011-10-11
        "Phytoplankton Species", //??the species name? better to use Taxonomy??  Add "Phytoplankton Abundance"?
        "Pressure", //bob added
        "Productivity", //bob added
        "Quality", //bob added 2010-11-10
        "Salinity", "Sea Level", 
        "Soils",   //bob added 2011-10-06
        "Statistics", //bob added 2010-12-24
        "Stream Flow", //added 2011-05-19
        "Surface Waves", 
        "Taxonomy", //bob added
        "Temperature",            
        TIME_CATEGORY, //bob added
        "Total Suspended Matter", //added 2011-05-19
        "Unknown", 
        "Wind", //had Wind. 2011-05-19 has "Wind Speed and Direction", but that seems unnecessarily limited
        "Zooplankton Species", //??the species name? better to use Taxonomy??
        "Zooplankton Abundance"};

    /**
     * The variable metadata attribute that indicates the name of the
     * observedProperty that is needed to access this variable via SOS.
     */
    public final static String observedProperty = "observedProperty";

    /**
     * The start of the url for observedProperty for cf standardNames.
     */
    public final static String cfObservedPropertyUrl = "http://marinemetadata.org/cf#";
    
    /** The valid options for colorBarScale. 
     * A given scale's index may change when new scales are added. */
    public final static String VALID_SCALES[] = {"Linear", "Log"};     
    
    /** This is the same as VALID_SCALES, but with option0="". */
    public final static String VALID_SCALES0[] = {"", "Linear", "Log"};     

    /** The time variable attribute that has the precision specification for 
        Calendar2.epochSecondsToLimitedIsoStringT. */
    public final static String TIME_PRECISION = "time_precision"; 

    /** This is the standard slider size. */
    public final static int SLIDER_PIXELS = 501;
    /** This is the desired maximum number of values for a slider. 
     * It is smaller than SLIDER_PIXELS because 2 pixels/value makes it easier for users to pick a value.
     */
    public final static int SLIDER_MAX_NVALUES = SLIDER_PIXELS / 2;



    //*************** END OF STATIC VARIABLES ***********************

    /** These variables are always set by the constructor. */
    protected String sourceName, destinationName, longName, sourceDataType, destinationDataType;
    /** e.g., float.class or String.class. */
    protected Class sourceDataTypeClass, destinationDataTypeClass;
    /** "units" are set by the constructor if they are available in 
     * sourceAttributes or addAtributes. It is highly recommended that you set "units".
     * At ERD, we always set "ioos_category".
     * May be null. */
    protected String units;
    /** ucumUnits are set by ucumUnits() when first needed. */
    private String ucumUnits = "\u0000"; //u0000=not yet set
    /** Attributes straight from the source. Set by the constructor. */
    protected Attributes sourceAttributes = new Attributes();
    /** Attributes which supercede sourceAttributes. Set by the constructor. */
    protected Attributes addAttributes = new Attributes();
    /** Attributes made from sourceAtt and addAtt, then revised (e.g., remove "null" values) */
    protected Attributes combinedAttributes;
    /** The constructor sets this to length>0 string if the
     * variable isn't in the data source and so is represented by 
     * a fixed value.
     * Define this so sourceValue=destinationValue (if time: epoch seconds; 
     *   no altUnits=-1 or odd time format or scale addOffset).
     */ 
    protected String fixedValue = "";
    /** The destination minimum and maximum values (in standardized destination units) 
     * of this variable. 
     * These are set if the information is available; else they remain NaN. */
    protected double destinationMin = Double.NaN;
    protected double destinationMax = Double.NaN;
    /** This is the value of the source's missing value stand-in. 
     * It may remain NaN.
     * It is pre-scaleFactor and addOffset.
     * Grid axis variables should never have missing values.
     */
    protected double sourceMissingValue = Double.NaN;
    protected double sourceFillValue = Double.NaN;
    protected double destinationMissingValue = Double.NaN;
    protected double destinationFillValue = Double.NaN;
    protected double safeDestinationMissingValue = Double.NaN;
    protected boolean hasColorBarMinMax = false;
    protected byte[] sliderCsvValues = null;

    protected boolean isBoolean = false;
    protected boolean scaleAddOffset = false;
    //used for scaleAddOffset. Only true, if scaleAddOffset is true, too.
    //Thus 'true' also indicates: And this class is unpacking to to a larger datatype.
    protected boolean sourceIsUnsigned = false; 
    protected double scaleFactor = 1, addOffset = 0;

    /**
     * The constructor.
     * In general, subclasses call this as the first step in construction.
     * Non-Lon,Lat,Alt,Time variables use the other constructor.
     * This constructor DOESN'T look for actual_range, data_min, or data_max attributes, 
     * assuming that the subclasses' constructor will do that! 
     *
     * <p> This removes any scale_factor and add_offset attributes
     *   and stores the resulting information so that destination data
     *   has been converted to destinationDataType with scaleFactor and addOffset 
     *   applied.
     * 
     * <p> sourceAtt or addAtt can have missing_value and/or _FillValue.
     *   They will be adjusted by scale_factor and add_offset (if present).
     *
     * <p>Call setActualRangeFromDestinationMinMax() sometime after this returns.
     *
     * @param tSourceName the name of the variable in the dataset source
     *    (usually with no spaces)
     *    or a derived variable (e.g., "=0").
     * @param tDestinationName is the name to be used in the results.
     *    If null or "", tSourceName will be used.
     * @param tSourceAttributes are the attributes for the variable
     *    in the source.
     *    If this is null, an empty Attributes will be created.
     * @param tAddAttributes the attributes which will be added when data is 
     *    extracted and which have precedence over sourceAttributes.
     *    Special case: value="null" causes that item to be removed from combinedAttributes.
     *    If this is null, an empty Attributes will be created.
     * @param tSourceDataType the type of data (e.g., "boolean", "byte", "int", "float", "String", ...).
     *    If tSourceName specifies a fixed value, you can set this to 
     *    null and the sourceDataType will automatically be set to 
     *    int or double.
     *    <p>(Special case) For the boolean database type, use "boolean".
     *    ERDDAP doesn't support a boolean type (because booleans can't store nulls).
     *    Using "boolean" will cause boolean values to be stored and represented as bytes: 0=false, 1=true.
     *    Clients can specify constraints by using the numbers.
     *    But you need to use the "boolean" data type to tell ERDDAP how to 
     *    interact with the database.     
     * @param tSourceMin is the minimum value of the source variable
     *    (scale_factor and add_offset, if any, haven't been applied).     
     *    <br>If unknown, or tSourceName is a fixed value, you can just use Double.NaN here.
     *    <br>This constructor DOESN'T look for actual_range, data_min, or data_max attribute! 
     * @param tSourceMax is the maximum value of the source variable
     *    (scale_factor and add_offset, if any, haven't been applied).
     *    <br>If unknown, or tSourceName is a fixed value, you can just use Double.NaN here.
     *    <br>This constructor DOESN'T look for actual_range, data_min, or data_max attribute! 
     * @throws Throwable if trouble
     */
    public EDV(String tSourceName, String tDestinationName,
        Attributes tSourceAttributes, Attributes tAddAttributes, 
        String tSourceDataType, double tSourceMin, double tSourceMax) 
        throws Throwable {

        //String2.log("*EDV " + tDestinationName);        
        setSourceName(String2.canonical(tSourceName)); //sets fixedValue
        destinationName = String2.canonical(
            tDestinationName == null || tDestinationName.length() == 0? 
                tSourceName : tDestinationName);
        sourceAttributes = tSourceAttributes == null? new Attributes() : tSourceAttributes;
        addAttributes    = tAddAttributes    == null? new Attributes() : tAddAttributes;

        sourceDataType = tSourceDataType;
        if (sourceDataType != null && sourceDataType.equals("boolean")) {
            sourceDataType = "byte";
            isBoolean = true;
        }
        if (sourceDataType == null && isFixedValue()) {
            //test with Java's strict parsing (not String2 loose parsing)
            try {
                //test most restrictive first
                Integer.parseInt(fixedValue); 
                sourceDataType = "int";  //if no error, it's an int
            } catch (Exception e1) {
                //2011-02-09 Bob Simons added to avoid Java hang bug.
                //But now, latest version of Java is fixed.
                //if (String2.isDoubleTrouble(fixedValue)) {
                //    sourceDataType = "double";
                //} else {
                    try {
                        Double.parseDouble(fixedValue); 
                        sourceDataType = "double";  //if no error, it's a double
                    } catch (Exception e2) {
                        sourceDataType = "String";
                    }
                //}
            }
        }
        sourceDataType = String2.canonical(sourceDataType);
        try {
            sourceDataTypeClass = PrimitiveArray.elementStringToClass(sourceDataType);
        } catch (Throwable t) {
            throw new IllegalArgumentException("datasets.xml error: Invalid source dataType=" + 
                sourceDataType + " for sourceName=" + sourceName);
        }

        if (isFixedValue()) {
            destinationMin = String2.parseDouble(fixedValue); //if String, will be NaN (as it should be)
            destinationMax = destinationMin;
        } else if (sourceDataType.equals("float")) {  //destinationDataType not known yet
            destinationMin = Math2.floatToDouble(tSourceMin);  //unbruise them
            destinationMax = Math2.floatToDouble(tSourceMax);
        } else {
            destinationMin = tSourceMin;  
            destinationMax = tSourceMax;
        }
        //String2.pressEnterToContinue("!!!sourceName=" + sourceName + " type=" + sourceDataType + " min=" + destinationMin);

        //makeCombinedAttributes
        makeCombinedAttributes(); 

        //after makeCombinedAttributes 
        //get longName for erddap use; the approach below is more conservative than suggestLongName
        longName = combinedAttributes().getString("long_name"); //all sources are already canonical
        if (longName == null)
            longName = combinedAttributes().getString("standard_name");
        if (longName == null)
            longName = destinationName;
        units = combinedAttributes().getString("units"); //may be null; already canonical

        //extractScaleAddOffset     It sets destinationDataType
        extractScaleAddOffset(); 
        setDestinationMinMaxFromSource(destinationMin, destinationMax); //ensures order is correct
        //after extractScaleAddOffset, get sourceMissingValue and sourceFillValue
        //and convert to destinationDataType (from scaleAddOffset)
        //???eek!!! can there be String missing_value or _FillValue?
        //
        //ERDDAP policy: a variable can have one/both/neither of 
        //missing_value (CF deprecated) and _FillValue metadata. 
        //ERDDAP doesn't care, doesn't change and just passes through whatever 
        //the metadata specifies.
        PrimitiveArray pa = combinedAttributes.get("missing_value"); 
        if (pa != null) {
            //attributes are supposed to be unsigned if _Unsigned=true, but sometimes aren't
            sourceMissingValue = sourceIsUnsigned? 
                pa.getUnsignedDouble(0) : 
                pa.getNiceDouble(0); 
            destinationMissingValue = sourceMissingValue * scaleFactor + addOffset;
            PrimitiveArray pa2 = PrimitiveArray.factory(destinationDataTypeClass, 1, false);
            pa2.addDouble(destinationMissingValue);
            combinedAttributes.set("missing_value", pa2);
        }
        pa = combinedAttributes.get("_FillValue"); 
        if (pa != null) {
            //attributes are supposed to be unsigned if _Unsigned=true, but sometimes aren't
            sourceFillValue = sourceIsUnsigned?
                pa.getUnsignedDouble(0) :
                pa.getNiceDouble(0);
            destinationFillValue = sourceFillValue * scaleFactor + addOffset;
            PrimitiveArray pa2 = PrimitiveArray.factory(destinationDataTypeClass, 1, false);
            pa2.addDouble(destinationFillValue);
            combinedAttributes.set("_FillValue", pa2);
            //String2.log(">>EDV " + tSourceName + " _FillValue pa2=" + pa2.toString());
        }
        safeDestinationMissingValue = Double.isNaN(destinationFillValue)? //fill has precedence
            destinationMissingValue : destinationFillValue;

        //after extractScaleAddOffset, adjust valid_range
        PrimitiveArray vr = combinedAttributes.remove("unpacked_valid_range"); 
        if (vr == null) {
            vr = combinedAttributes.remove("valid_range"); 
            if (vr != null && vr.size() == 2) {
                //adjust valid_range
                //attributes are supposed to be unsigned if _Unsigned=true, but sometimes aren't
                if (sourceIsUnsigned)
                    vr = PrimitiveArray.unsignedFactory(destinationDataTypeClass, vr);
                vr = vr.scaleAddOffset(destinationDataTypeClass, scaleFactor, addOffset);
                combinedAttributes.set("valid_range", vr);
            }
        } else {
            //save unpacked_valid_range as valid_range (overwriting any previous version)
            combinedAttributes.set("valid_range", vr);
        }
        //adjust valid_min and valid_max?
        PrimitiveArray vMin = combinedAttributes.get("valid_min"); //attributes are never unsigned
        if (vMin != null) {
            if (sourceIsUnsigned)
                vMin = PrimitiveArray.unsignedFactory(destinationDataTypeClass, vMin);
            vMin = vMin.scaleAddOffset(destinationDataTypeClass, scaleFactor, addOffset);
            combinedAttributes.set("valid_min", vMin);
        }
        PrimitiveArray vMax = combinedAttributes.get("valid_max"); //attributes are never unsigned
        if (vMax != null) {
            if (sourceIsUnsigned)
                vMax = PrimitiveArray.unsignedFactory(destinationDataTypeClass, vMax);
            vMax = vMax.scaleAddOffset(destinationDataTypeClass, scaleFactor, addOffset);
            combinedAttributes.set("valid_max", vMax);
        }

    }


    /** 
     * This variant constructor is only used for non-Lon,Lat,Alt,Time
     * variables where the sourceMin and sourceMax are not
     * explicitly defined.
     * This constructor tries to set destinationMin and destinationMax by looking for
     * actual_range, data_min, or data_max metadata.
     *
     * <p>For EDVGridAxis, actual_range should indicate order of storage (first, last).
     *  Sometimes latitude is max,min.
     *
     * <p>Call setActualRangeFromDestinationMinMax() sometime after this returns.
     *
     * <p>See the other constructor for more information.
     */
    public EDV(String tSourceName, String tDestinationName,
        Attributes tSourceAttributes, Attributes tAddAttributes, 
        String tSourceDataType) 
        throws Throwable {

        this(tSourceName, tDestinationName, 
            tSourceAttributes, tAddAttributes, tSourceDataType,
            Double.NaN, Double.NaN);

        //min max  from actual_range, data_min, or data_max
        double mm[] = extractActualRange();  //may be low,high or high,low
        if (Double.isNaN(destinationMin) && Double.isNaN(destinationMax)) 
            setDestinationMinMax(
                mm[0] * scaleFactor + addOffset,
                mm[1] * scaleFactor + addOffset); 
    }


    /**
     * This generates combined attributes from addAttributes and sourceAttributes.
     */
    protected void makeCombinedAttributes() throws Throwable {
        combinedAttributes = new Attributes(addAttributes, sourceAttributes); //order is important
        combinedAttributes.removeValue("null");

        //test presence and validity of colorBar attributes
        //ERDDAP.doWmsGetMap relies on these tests.
        String tMinS = combinedAttributes.getString("colorBarMinimum");
        String tMaxS = combinedAttributes.getString("colorBarMaximum");
        double tMin = String2.parseDouble(tMinS);
        double tMax = String2.parseDouble(tMaxS);
        String tPalette    = combinedAttributes.getString("colorBarPalette");
        String tContinuous = combinedAttributes.getString("colorBarContinuous");
        String tScale      = combinedAttributes.getString("colorBarScale");
        if (tMinS != null && !Math2.isFinite(tMin))
            throw new IllegalArgumentException("colorBarMinimum=" + tMin + " must be a valid number.");
        if (tMaxS != null && !Math2.isFinite(tMax))
            throw new IllegalArgumentException("colorBarMaximum=" + tMax + " must be a valid number.");
        hasColorBarMinMax = Math2.isFinite(tMin) && Math2.isFinite(tMax);  
        if (hasColorBarMinMax && tMin >= tMax) //this may change if flipped range is allowed
            throw new IllegalArgumentException("colorBarMinimum=" + tMin + 
               " must be less than colorBarMaximum=" + tMax + ".");
        if (tPalette != null && String2.indexOf(EDStatic.palettes, tPalette) < 0)
            throw new IllegalArgumentException("colorBarPalette=" + tPalette + " must be one of " + 
                String2.toCSSVString(EDStatic.palettes) + " (default='Rainbow').");
        if (tContinuous != null && !tContinuous.equals("true") && !tContinuous.equals("false"))
            throw new IllegalArgumentException("colorBarContinuous=" + tPalette + " must be 'true' (the default) or 'false'.");
        if (tScale != null && String2.indexOf(VALID_SCALES, tScale) < 0)
            throw new IllegalArgumentException("colorBarScale=" + tScale + " must be one of " + 
                String2.toCSSVString(VALID_SCALES) + " (default='Linear').");
        if (tScale != null && tScale.equals("Log") && tMin <= 0) 
            throw new IllegalArgumentException("If colorBarScale=Log, colorBarMinimum=" + 
                tMin + " must be > 0.");
    }

    /** 
     * This tries to extract scale_factor and add_offset attributes values from combinedAttributes
     * and set scaleAddOffset accordingly.
     * This removes the scale and addOffset attributes from source- add- and combinedAttributes.
     * This sets destinationDataType and destinationDataTypeClass.
     *
     */
    protected void extractScaleAddOffset() {

        PrimitiveArray sf = combinedAttributes.remove("scale_factor");
        PrimitiveArray ao = combinedAttributes.remove("add_offset");
        if (sf == null && ao == null) {
            destinationDataType = sourceDataType;
            destinationDataTypeClass = sourceDataTypeClass;
            return;
        }

        //scaleAddOffset will be used
        scaleAddOffset = true;
        PrimitiveArray un = combinedAttributes.remove("_Unsigned");
        sourceIsUnsigned = 
            un != null && "true".equals(un.toString()) &&
            PrimitiveArray.isIntegerType(sourceDataTypeClass);

        if (sf != null) {
            scaleFactor = sf.getNiceDouble(0);
            if (Double.isNaN(scaleFactor))
                scaleFactor = 1;
            destinationDataType = sf.elementClassString();
            destinationDataTypeClass = sf.elementClass();
        }
        if (ao != null) {
            addOffset = ao.getNiceDouble(0);
            if (Double.isNaN(addOffset))
                addOffset = 0;
            destinationDataType = ao.elementClassString();
            destinationDataTypeClass = ao.elementClass();
        }
        if (scaleFactor == 1 && addOffset == 0) {
            scaleAddOffset = false;
            sourceIsUnsigned = false;
            if (un != null && "true".equals(un.toString()) &&
                //if floating type, '_Unsigned'=true is nonsense
                PrimitiveArray.isIntegerType(sourceDataTypeClass)) 
                combinedAttributes.set("_Unsigned", un); //re-set it
        }
        if (verbose && scaleAddOffset)
            String2.log("EDV sourceName=" + sourceName + 
                " (unsigned=" + sourceIsUnsigned + 
                ") will be unpacked via scale_factor=" + scaleFactor + 
                ", then add_offset=" + addOffset);
    }

    /** 
     * This tries to get the actual_range, data_min, or data_max attribute values from combinedAttributes.
     * This removes the actual_range, data_min, or data_max attribute from source- add- and combinedAttributes.
     *
     * @return a double[2] (always) with sourceMin and sourceMax values from 
     *    actual_range, data_min, or data_max metadata (or NaNs).
     *    NOTE: for EDVGridAxis this indicates order or storage, so may be low,high or high,low.
     */
    protected double[] extractActualRange() {

        double mm[] = {combinedAttributes.getDouble("data_min"),   //NaN if not found
                       combinedAttributes.getDouble("data_max")};
        combinedAttributes.remove("data_min");
        combinedAttributes.remove("data_max");

        PrimitiveArray actualRange = combinedAttributes.get("actual_range");
        if (actualRange != null) {
            //if (reallyVerbose) String2.log("  actual_range metadata for " + destinationName + ": " + actualRange);
            if (actualRange.size() == 2) {
                mm[0] = actualRange.getNiceDouble(0); //sourceMin
                mm[1] = actualRange.getNiceDouble(1); //sourceMax
            }
            combinedAttributes.remove("actual_range");
        }

        return mm;
    }

    /** 
     * This sets the actual_range attribute in addAttributes and combinedAttributes
     * based on the destinationMin and destinationMax value.
     * destinationDataTypeClass must be already set correctly.
     * "actual_range" is defined in [CDC COARDS] 
     * http://www.cdc.noaa.gov/cdc/conventions/cdc_netcdf_standard.shtml 
     * as "actual data range for variable. Same type as unpacked values."
     * Later, it says "The range values are used to indicate order of storage 
     * (e.g., 90,-90 would indicate the latitudes started with 90 and ended with -90)."
     *
     * <p>EDVGridAxis overwrites this to use firstDestinationValue and lastDestinationValue.
     */
    public void setActualRangeFromDestinationMinMax() {

        //actual_range is useful information for .das and will be replaced by actual_range of data subset.
        combinedAttributes.remove("data_min");
        combinedAttributes.remove("data_max");
        if (Double.isNaN(destinationMin) && Double.isNaN(destinationMax)) {
            combinedAttributes.remove("actual_range");
        } else {
            PrimitiveArray pa = PrimitiveArray.factory(destinationDataTypeClass(), 2, false);
            pa.addDouble(destinationMin);
            pa.addDouble(destinationMax);
            combinedAttributes.set("actual_range", pa);
        }
    }

    /**
     * This does the most common thing with extractActualRange and 
     * setActualRangeFromDestinationMinMax().
     * This must be done after scaleFactor and addOffset have be determined.
     */
    public void extractAndSetActualRange() {
        double mm[] = extractActualRange(); 
        if (Double.isNaN(destinationMin)) 
            setDestinationMin(mm[0] * scaleFactor + addOffset);
        if (Double.isNaN(destinationMax)) 
            setDestinationMax(mm[1] * scaleFactor + addOffset);
        if (destinationMin > destinationMax) { //in Java, only true if neither if NaN
            double d = destinationMin; destinationMin = destinationMax; destinationMax = d; }
        setActualRangeFromDestinationMinMax();
    }


    /**
     * This is used by the EDD constructor to determine if this
     * EDV is valid.
     * Subclasses should extend this by calling super.ensureValid() and by 
     * ensuring that their instance variables are valid.
     *
     * @param errorInMethod the start string for an error message
     * @throws Throwable if this EDV is not valid
     */
    public void ensureValid(String errorInMethod) throws Throwable {
        errorInMethod += "\ndatasets.xml/EDV.ensureValid error for variable destinationName=" + 
            destinationName + ":\n";
        Test.ensureSomethingUtf8(sourceName,      errorInMethod + "sourceName");
        Test.ensureFileNameSafe( destinationName, errorInMethod + "destinationName");
        if (destinationName.indexOf(".") >= 0 || destinationName.indexOf("-") >= 0)
            throw new IllegalArgumentException(errorInMethod + 
                "destinationName=\"" + destinationName + "\" must contain only A-Z, a-z, 0-9, or '_'.");
        char firstCh = destinationName.charAt(0);
        if ((firstCh >= 'A' && firstCh <= 'Z') || (firstCh >= 'a' && firstCh <= 'z')) {
            //so valid variable name in Matlab and ...
        } else throw new IllegalArgumentException(errorInMethod + 
            "destinationName=\"" + destinationName + "\" must start with a letter (A-Z, a-z).");
        Test.ensureSomethingUtf8(longName,        errorInMethod + "longName");
        try {
            //should already by set, but ensure consistent and valid
            sourceDataTypeClass = PrimitiveArray.elementStringToClass(sourceDataType); 
        } catch (Throwable t) {
            throw new IllegalArgumentException(errorInMethod + 
                "sourceDataType=" + sourceDataType + " isn't supported.");
        }
        try {
            //should already by set, but ensure consistent and valid
            destinationDataTypeClass = PrimitiveArray.elementStringToClass(destinationDataType); 
        } catch (Throwable t) {
            throw new IllegalArgumentException(errorInMethod + 
                "destinationDataType=" + destinationDataType + " isn't supported.");
        }
        //units may be null
        if (EDStatic.variablesMustHaveIoosCategory) {
            String ic = combinedAttributes().getString("ioos_category");
            Test.ensureSomethingUtf8(ic, errorInMethod + "ioos_category");
            Test.ensureTrue(String2.indexOf(IOOS_CATEGORIES, ic) >= 0,
                errorInMethod + "ioos_category=\"" + ic + "\" isn't a valid category.");
        }

        //Don't test Test.ensureSomethingUtf8(sourceAttributes,    errorInMethod + "sourceAttributes");
        //Admin can't control source and addAttributes may override offending characters.
        Test.ensureSomethingUtf8(addAttributes,       errorInMethod + "addAttributes");
        Test.ensureSomethingUtf8(combinedAttributes,  
            errorInMethod + "combinedAttributes (but probably caused by the source attributes)");
        if (scaleAddOffset && destinationDataTypeClass == String.class)
            throw new IllegalArgumentException(errorInMethod +
                "scale_factor and add_offset can't be active for String variables.");

    }

    /**
     * This returns a string representation of this EDV (mostly
     * to be used by the subclasses).
     *
     * @return a string representation of this EDV.
     */
    public String toString() {
        return "EDV" + 
            "\n  sourceName=" + sourceName + 
            "\n  destinationName=" + destinationName + 
            "\n  longName=" + longName + 
            "\n  units=" + units +
            "\n  ioosCategory=" + combinedAttributes().getString("ioos_category") +
            "\n  sourceDataType=" + sourceDataType +
            "\n  fixedValue=" + fixedValue + 
            "\n  destinationDataType=" + destinationDataType +
            "\n  destinationMin=" + destinationMin + " max=" + destinationMax + 
            "\n  scaleAddOffset=" + scaleAddOffset + " sf=" + scaleFactor + 
                " ao=" + addOffset + " unsigned=" + sourceIsUnsigned +
            "\n  hasColorBarMinMax=" + hasColorBarMinMax +
            "\n  sourceAttributes=\n" + sourceAttributes.toString() + //it has trailing newline
              "  addAttributes=\n" + addAttributes.toString(); //it has trailing newline
    }

    /** 
     * This is used by generateDatasetsXml to suggests a long_name 
     * (based on the sourceame or standard_name) if the long_name wasn't provided.
     * 
     * @param oLongName the original long_name (may be null or "");
     * @param tSourceName must be valid
     * @param tStandardName  preferred starting point, but may be null or ""
     * @return the suggested longName
     * @throws Exception if trouble
     */
    public static String suggestLongName(String oLongName, String tSourceName, 
        String tStandardName) throws Exception {
        if (oLongName == null)
            oLongName = "";
        if (tSourceName == null)
            tSourceName = "";
        if (tStandardName == null)
            tStandardName = "";
        String fromAbbrev = EDStatic.gdxVariableNamesHashMap().get( //may be null
            tSourceName.toLowerCase()); 
        if (fromAbbrev == null) {
            //handle e.g., SOG (knots)
            int po = tSourceName.indexOf(" (");
            if (po > 0)
                fromAbbrev = EDStatic.gdxVariableNamesHashMap().get(
                    tSourceName.substring(0, po).toLowerCase());
        }
        String ttName = 
            //prefer tStandardName
            //standard names are great, but not always better than oLongName; tough call
            tStandardName.equals("time") && oLongName.length() > 0? oLongName :
            tStandardName.equals("time") && tSourceName.length() > 0? tSourceName :
            tStandardName.length() > 0? tStandardName : 
            oLongName.length()     > 0? oLongName :
            fromAbbrev    != null? fromAbbrev :
            tSourceName;
        if ("pH".equals(ttName))
            return ttName;
        StringBuilder tName = new StringBuilder(ttName.trim());
        String2.replaceAll(tName, "_", " ");
        while (tName.length() > 0 && "*_".indexOf("" + tName.charAt(tName.length() - 1)) >= 0)
            tName.setLength(tName.length() - 1);
        String result = tName.toString().trim();  //default

        String tNameLC = result.toLowerCase();
        if (tNameLC.startsWith("eta ") ||
            tNameLC.startsWith("cs ") ||
            tNameLC.startsWith("s ") ||
            tNameLC.startsWith("xi ") ||
            tNameLC.startsWith("rho ") ||
            tNameLC.startsWith("tau ") ||
            tNameLC.endsWith(" rho")) {
            //don't change case

        } else {

            //change "camelCase" to spaced "camel Case"
            //  but don't space out an acronym, e.g., E T O P O
            //  and don't split hyphenated words, e.g.,   Real- Time
            for (int i = tName.length() - 1; i > 0; i--) {
                char chi  = tName.charAt(i);
                char chi1 = tName.charAt(i - 1);
                if (chi  != Character.toLowerCase(chi)  &&
                    chi1 == Character.toLowerCase(chi1) && 
                    Character.isLetterOrDigit(chi1)) {
                    tName.insert(i, ' ');
                }
            }

            //no vowels? 
            String ucName = tName.toString().toUpperCase();
            if (ucName.indexOf('A') < 0 &&
                ucName.indexOf('E') < 0 &&
                ucName.indexOf('I') < 0 &&
                ucName.indexOf('O') < 0 &&
                ucName.indexOf('U') < 0) {
                //capitalize all
                result = ucName;  
            } else {
                //capitalize 1st char and after spaces
                for (int i = 0; i < tName.length(); i++) 
                    if (i == 0 || tName.charAt(i - 1) == ' ')
                        tName.setCharAt(i, Character.toUpperCase(tName.charAt(i)));
                result = tName.toString();
            }
        }

        //shorten the name?
        String seek = "aasg:"; //special case
        int po = -1;
        if (result.length() > 6)
            po = result.substring(0, result.length() - 1).lastIndexOf(seek);
        //NOT YET. Most sourceNames aren't too long. aasg is the only known exception.
        //look for last '/', but not at very end
        //  and avoid e.g., several something_quality -> quality
        //if (po < 0 && result.length() > 60) 
        //    po = result.substring(0, result.length() - 30).lastIndexOf(seek = "/");
        if (po >= 0)
            result = result.substring(po + seek.length());

        //return
        //String2.log(">> suggestLongName o=" + oLongName + " sourceName=" + tSourceName + 
        //    " stdName=" + tStandardName + " result=" + result);
        return result;

    }

    /**
     * This is used by constructors to set the sourceName and fixedValue
     * (if souceName starts with "=".
     *
     * @param tSourceName
     * @throws Throwable if trouble (e.g., if fixed value parses to NaN).
     */
    protected void setSourceName(String tSourceName) throws Throwable {
        sourceName = tSourceName;
        if (sourceName != null && sourceName.length() >= 2 &&
            sourceName.charAt(0) == '=') {

            fixedValue = extractFixedValue(sourceName);
        }
    }

    /**
     * This extracts the fixedValue from a sourceName that starts with "=".
     *
     * @param sourceName
     * @return the numeric source fixed value after "="
     * @throws Throwable if name isn't of correct format or fixed value parses to NaN.
     */
    public static String extractFixedValue(String sourceName) throws Throwable {
        if (sourceName != null && sourceName.length() >= 2 &&
            sourceName.charAt(0) == '=') {
            
            return sourceName.substring(1);
        }
        throw new IllegalArgumentException( 
            "datasets.xml error in EDV.extractFixedValue:\n" +
            "Invalid sourceName=" + sourceName);
    }

    /**
     * The name of the variable in the source dataset.
     * 
     * @return the name of the variable in the source dataset.
     */
    public String sourceName() {return sourceName;}

    /**
     * The name for the variable in the destination dataset.
     * 
     * @return the name of the variable in the destination dataset.
     */
    public String destinationName() {return destinationName;}

    /**
     * The long (more descriptive) name of the variable 
     * suitable for labeling the axis of a graph.
     * It may have spaces.
     * It will never null or "".
     * For the alt,lat,lon,time variables, this is fixed and set by the constructor.
     * For data variables, this is from "long_name" or "standard_name" in 
     *    sourceAttributes or addAttributes.
     * 
     * @return the long name of the variable.
     */
    public String longName() {return longName;}

    /**
     * The destination units for this variable (presumably using the 
     * EDStatic.units_standard, e.g., UDUNITS).
     * 
     * @return the destination units for this variable (e.g., "m") (may be null).
     */
    public String units() {return units;}


    /**
     * The destination OGC-style UCUM (UOM) units for this variable.
     * 
     * @return the UCUM units for this variable (e.g., "m") (may be null).
     */
    public String ucumUnits() {
        //not yet set?
        if ("\u0000".equals(ucumUnits)) {
            if ("UDUNITS".equals(EDStatic.units_standard)) {
                try {
                    ucumUnits = EDUnits.udunitsToUcum(units()); //null returns null
                } catch (Throwable t) {
                    String2.log(String2.ERROR + " while converting udunits=" + units() + " to ucum:\n" +
                        MustBe.throwableToString(t));
                    ucumUnits = units();
                }
            } else {
                ucumUnits = units(); //no conversion
            }
        }

        return ucumUnits;
    }


    /**
     * The alphabetical list of the IOOS categories options for variables.
     * 
     * @return the alphabetical list of valid categories for variables.
     *    This is the internal String[], so don't change it.
     */
    public String[] ioosCategories() {return IOOS_CATEGORIES;}

    /**
     * This returns true if sourceDataType was originally boolean.
     * Remember that if sourceDataType was originally boolean, 
     * the sourceDataType was converted to byte.
     * 
     * @return true if the sourceDataType was originally boolean.
     */
    public boolean isBoolean() {return isBoolean;}

    /**
     * The source Java data type for this variable.
     * 
     * @return the source Java data type for this variable (e.g., "float" or "String").
     * Remember that if sourceDataType was originally boolean, 
     * the sourceDataType was converted to byte (see isBoolean()).
     */
    public String sourceDataType() {return sourceDataType;}

    /**
     * The source Java data type class for this variable.
     * The destination data type is the same as the source data type 
     * in all cases except time variables, where the destination type is always
     * double.class.
     * 
     * @return the source Java data type class for this variable (e.g., float.class 
     *    or String.class).
     */
    public Class sourceDataTypeClass() {return sourceDataTypeClass; }

    /**
     * The destination Java data type for this variable.
     * The destination data type is different than the source data type 
     * for time variables (where the destination type is always
     * "double" internally and for some fileTypes, and "String" for other fileTypes)
     * and if scaleAddOffset is true.
     * 
     * @return the destination Java data type for this variable (e.g., "float" or "String").
     */
    public String destinationDataType() {return destinationDataType;}

    /**
     * The destination Java data type class for this variable.
     * The destination data type class is different than the source data type class
     * for time variables (where the destination type is always
     * "double.class" internally and for some fileTypes, and "String.class" for other fileTypes)
     * and if scaleAddOffset is true.
     * 
     * @return the destination Java data type class for this variable (e.g., float.class 
     *    or String.class).
     */
    public Class destinationDataTypeClass() {return destinationDataTypeClass; }

    /**
     * The number of bytes per element of source data (Strings arbitrarily return 20).
     * 
     * @return the number of bytes per element of source data (Strings arbitrarily return 20)
     */
    public int sourceBytesPerElement() {return PrimitiveArray.elementSize(sourceDataTypeClass);}

    /**
     * The number of bytes per element of destination data (Strings arbitrarily return 20).
     * 
     * @return the number of bytes per element of destination data (Strings arbitrarily return 20)
     */
    public int destinationBytesPerElement() {return PrimitiveArray.elementSize(destinationDataTypeClass);}

    /** 
     * This returns true if this is a fixedValue variable.
     * 
     * @return true if this is a fixedValue variable.
     */
    public boolean isFixedValue() {return fixedValue.length() > 0;}

    /** 
     * This returns a non "" value if the
     * axis isn't in the data source and so is represented by 
     * a fixed value. 
     * This is always defined so source = destination
     * (if time, this is epochSeconds).
     * 
     * @return the fixedValue for this axis (or "" if not fixed).
     */
    public String fixedValue() {return fixedValue;}

    /** 
     * This returns the destinationMin value (in standardized units) for this axis (e.g., 
     * altitude values are in meters, positive=up 
     * and time values are in seconds since 1970-01-01T00:00:00Z).
     * scaleFactor() and addOffset() have been applied.
     * 
     * @return the cleaned up destinationMin value for this axis.
     */
    public double destinationMin() {return destinationMin;}

    /** 
     * This returns the destinationMax value (in standardized units) for this axis (e.g., 
     * altitude values are in meters, positive=up 
     * and time values are in seconds since 1970-01-01).
     * scaleFactor() and addOffset() have been applied.
     *
     * <p>For time in near-real-time EDDTable datasets, destinationMax should be NaN 
     * to indicate that the roughly NOW.  For example, see cwwcNDBCMet: data is from files,
     * but presumption is data in files may change before next time file is read.
     * 
     * @return the cleaned up destinationMax value for this axis.
     */
    public double destinationMax() {return destinationMax;}

    public void setDestinationMinMaxFromSource(double sourceMin, double sourceMax) {
        if (scaleAddOffset) 
            setDestinationMinMax(
                sourceMin * scaleFactor + addOffset,
                sourceMax * scaleFactor + addOffset);
        else setDestinationMinMax(sourceMin, sourceMax);
    }

    /**
     * This lets you setDestinationMin and setDestinationMax in one step.
     * If tMin &gt; tMax, this will swap them.
     */
    public void setDestinationMinMax(double tMin, double tMax) {
        if (tMin > tMax) { //if either is NaN, result in Java is false
            double d = tMin; tMin = tMax; tMax = d;}
        setDestinationMin(tMin);
        setDestinationMax(tMax);
    }

    public void setDestinationMin(double tMin) {
        destinationMin = destinationDataTypeClass() == float.class?
            Math2.floatToDouble(tMin) : tMin;  //store unbruised
    }

    public void setDestinationMax(double tMax) {
        destinationMax = destinationDataTypeClass() == float.class?
            Math2.floatToDouble(tMax) : tMax;  //store unbruised
    }

    /** 
     * This is the destinationMin value (time overrides this to format as ISO string).  
     *
     * @return the destinationMin (or "" if unknown)
     */
    public String destinationMinString() {
        return Double.isNaN(destinationMin)? "" : 
            destinationDataTypeClass == float.class? "" + (float)destinationMin :
            destinationDataTypeClass == double.class?
                "" + Math2.niceDouble(destinationMin, 15) :  //was "" + destinationMin
                "" + Math2.roundToLong(destinationMin);  //ints are nicer without trailing ".0"
    }

    /** 
     * This is the destinationMax value (time overrides this to format as ISO string).  
     *
     * @return the destinationMax  (or "" if unknown or time=~now)
     */
    public String destinationMaxString() {
        return Double.isNaN(destinationMax)? "" : 
            destinationDataTypeClass == float.class? "" + (float)destinationMax :
            destinationDataTypeClass == double.class?
                "" + Math2.niceDouble(destinationMax, 15) :
                "" + Math2.roundToLong(destinationMax);  //ints are nicer without trailing ".0"
    }

    /** 
     * This returns true if scaleFactor and/or addOffset are active.
     * <br>destinationValue = sourceValue * scaleFactor + addOffset;
     * <br>sourceValue = (destintationValue - addOffset) / scaleFactor;
     *
     * @return true if scaleFactor and/or addOffset are active.
     */
    public boolean scaleAddOffset() {return scaleAddOffset;}

    /** 
     * This returns true if the source if scaleAddOffset is true and _Unsigned="true".
     *
     * @return true if the source has _Unsigned="true".
     */
    public boolean sourceIsUnsigned() {return sourceIsUnsigned;}

    /** 
     * This returns true if the destinationValues equal the sourceValues 
     *   (e.g., scaleFactor = 1 and addOffset = 0). 
     * <br>Some subclasses overwrite this to cover other situations:
     * <br>EDVTimeStamp only returns true if sourceTimeIsNumeric and
     *   sourceTimeBase = 0 and sourceTimeFactor = 1.
     *
     * @return true if the destinationValues equal the sourceValues.
     */
    public boolean destValuesEqualSourceValues() {
        return !scaleAddOffset;
    }

    /** 
     * This returns the scaleFactor.
     * <br>destinationValue = sourceValue * scaleFactor + addOffset;
     * <br>sourceValue = (destintationValue - addOffset) / scaleFactor;
     * 
     * @return the scaleFactor.
     */
    public double scaleFactor() {return scaleFactor;}

    /** 
     * This returns the addOffset.
     * <br>destinationValue = sourceValue * scaleFactor + addOffset;
     * <br>sourceValue = (destintationValue - addOffset) / scaleFactor;
     * 
     * @return the addOffset.
     */
    public double addOffset() {return addOffset;}

    /** This returns the value of drawLandMask (false=under, true=over)
     * for this variable 
     * (or eddDefaultDrawLandMask if drawLandMask not specified in combinedAttributes).
     */
    public boolean drawLandMask(boolean eddDefaultDrawLandMask) {
        String dlm = combinedAttributes().getString("drawLandMask"); 
        if (dlm != null) {
            if (dlm.equals("under")) return false;
            if (dlm.equals("over"))  return true;
        }
        return eddDefaultDrawLandMask;
    }

    /**
     * This returns a PrimitiveArray (the original if the data type wasn't changed)
     * with source values converted to destinationValues.
     * This doesn't change the order of the values.
     *
     * <p>This version of this method just deals with scaleAddOffset.
     * Some subclasses overwrite this.   (Time variables will return a DoubleArray.)
     * 
     * @param source
     * @return a PrimitiveArray 
     *   (the same Primitive array if the data type wasn't changed)
     * with source values converted to destinationValues.
     */
    public PrimitiveArray toDestination(PrimitiveArray source) {
        return scaleAddOffset?
            source.scaleAddOffset(sourceIsUnsigned, destinationDataTypeClass, scaleFactor, addOffset):
            source;        
    }

    /**
     * This returns a PrimitiveArray (the original if the data type wasn't changed)
     * with destination values converted to sourceValues.
     * This doesn't change the order of the values.
     *
     * <p>This version of this method just deals with scaleAddOffset.
     * Some subclasses overwrite this.   
     * 
     * @param destination
     * @return a PrimitiveArray 
     *   (the same Primitive array if the data type wasn't changed)
     * with destination values converted to sourceValues.
     */
    public PrimitiveArray toSource(PrimitiveArray destination) {
        return scaleAddOffset?
//sourceIsUnsigned?
            destination.addOffsetScale(sourceDataTypeClass, -addOffset, 1/scaleFactor): //note different method
            destination;        
    }


    /**
     * This returns a JSON-style csv String with a subset of destinationStringValues
     * suitable for use on a slider with SLIDER_PIXELS.
     * EDVTimeStamp and EDVGridAxis overwrite this.
     *
     * <p>If destinationMin or destinationMax (except time) aren't finite,
     * this returns null.
     */
    public String sliderCsvValues() throws Throwable {
        byte scv[] = sliderCsvValues; //local copy avoids concurrency problems
        if (scv != null) 
            return String2.utf8ToString(scv);

        try {
            boolean isTimeStamp = false; //EDVTimeStamp overwrites this method
            double tMin = destinationMin;
            double tMax = destinationMax;
            if (!Math2.isFinite(tMin)) return null;  //quick rejection is important
            if (!Math2.isFinite(tMax)) return null;
            boolean isFloat = destinationDataTypeClass == float.class;
            double dVal;
            String sVal;

            //one value
            if (Math2.almostEqual(8, tMin, tMax)) {
                dVal = tMin;
                sVal = isFloat? "" + (float)dVal : "" + dVal;
                String csv = toSliderString(sVal, isTimeStamp);
                sliderCsvValues = String2.getUTF8Bytes(csv); //do last
                return csv;
            }

            //one time: generate the sliderCsvValues
            dVal = tMin;
            sVal = isFloat? "" + (float)dVal : "" + dVal;
            StringBuilder sb = new StringBuilder(toSliderString(sVal, isTimeStamp)); //first value
            double stride = Math2.suggestMaxDivisions(tMax - tMin, SLIDER_MAX_NVALUES);    
            int nDiv = Math2.roundToInt(Math.abs((tMax - tMin) / stride));
            double base = Math.floor(tMin / stride) * stride;
            for (int i = 1; i < nDiv; i++) { 
                sb.append(", ");
                dVal = base + i * stride;
                sVal = Math2.almost0(dVal)? "0" :
                       isFloat || Math.abs(dVal) < 1e37? "" + (float)dVal : "" + dVal;
                sb.append(toSliderString(sVal, isTimeStamp));
            }
            sb.append(", ");
            dVal = tMax;
            sVal = isFloat? "" + (float)dVal : "" + dVal;
            sb.append(toSliderString(sVal, isTimeStamp)); //last value

            //store in compact utf8 format
            if (reallyVerbose) String2.log("EDV.sliderCsvValues nDiv=" + nDiv + 
                " destMin=" + destinationMin + " destMax=" + destinationMax + 
                " tMin=" + tMin + " tMax=" + tMax + " stride=" + stride + 
                " base=" + base + " nValues=" + (nDiv + 1));
            String csv = sb.toString();
            sliderCsvValues = String2.getUTF8Bytes(csv); //do last
            return csv;
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            String2.log(MustBe.throwableToString(t));
            return null;
        }
    }

    //for use by sliderCsvValues()
    protected String toSliderString(String s, boolean isTimeStamp) {
        if (isTimeStamp) {
            if (s.endsWith("T00:00:00Z")) s = s.substring(0, s.length() - 10); 
            s = String2.toJson(s);
        } else {
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2); 
        }
        return s;
    }

    /**
     * This converts a value to the nearest slider position (0 .. EDV.SLIDER_PIXELS-1).
     * Out-of-range values (even far off) are converted to nearest, but NaN -> -1.
     * If only one destination value, this returns 0.
     *
     * <p>For EDVGridAxis (which overwrites the superclass version), this works whether isAscending or not.
     * 
     * @param destinationValue
     * @return the nearest slider position (0 .. EDV.SLIDER_PIXELS-1)
     *   (or -1 if trouble, e.g., sliderCsvValues can't be constructed (e.g., no min + max values)).
     */
    public int closestSliderPosition(double destinationValue) {

        double tMin = destinationMin;
        double tMax = destinationMax;
        if (!Math2.isFinite(destinationValue)) return -1;
        if (!Math2.isFinite(tMin)) return -1;
        if (!Math2.isFinite(tMax)) {
            if (this instanceof EDVTimeStamp) {
                //next midnight Z
                GregorianCalendar gc = Calendar2.newGCalendarZulu();
                try {
                    Calendar2.clearSmallerFields(gc, Calendar2.DATE);
                } catch (Throwable t) {
                    String2.log(MustBe.throwableToString(t));
                }
                gc.add(Calendar2.DATE, 1);
                tMax = Calendar2.gcToEpochSeconds(gc);
            } else return -1;
        }
        if (tMax == tMin) return 0;

        double frac = (destinationValue - tMin) / (tMax - tMin);
        if (frac <= 0) return 0;
        if (frac >= 1) return EDV.SLIDER_PIXELS - 1;
        return Math2.roundToInt(Math.floor(frac * EDV.SLIDER_PIXELS));
    }

    /** 
     * This is the value of the source's missing value stand-in (e.g., -9999999.0). 
     * It may be NaN.
     * Grid axis variables should never have missing values.
     * 
     * @return source's missing value stand-in (e.g., -9999999.0).
     */
    public double sourceMissingValue() {return sourceMissingValue;}

    /** 
     * This is the value of the source's fill value stand-in (e.g., -9999999.0). 
     * It may be NaN.
     * Grid axis variables should never have missing values.
     * 
     * @return source's fill value stand-in (e.g., -9999999.0).
     */
    public double sourceFillValue() {return sourceFillValue;}

    /** 
     * This is the value of the destination's missing value stand-in (e.g., -9999999.0)
     * (dest = source * scaleFactor + addOffset).
     * It may be NaN.
     * Grid axis variables should never have missing values.
     * 
     * @return destination's missing value stand-in (e.g., -9999999.0).
     */
    public double destinationMissingValue() {return destinationMissingValue;}

    /** 
     * This is the value of the destination's fill value stand-in (e.g., -9999999.0)
     * (dest = source * scaleFactor + addOffset). 
     * It may be NaN.
     * Grid axis variables should never have fill values.
     * 
     * @return destination's fill value stand-in (e.g., -9999999.0).
     */
    public double destinationFillValue() {return destinationFillValue;}

    /** 
     * If you have to use one value for destinationMissingValue or destinationFillValue,
     * use this one. It may be NaN.
     * Grid axis variables should never have missing values.
     * 
     * @return destination's safe missing value stand-in (e.g., -9999999.0).
     */
    public double safeDestinationMissingValue() {return safeDestinationMissingValue;}


    /** 
     * This returns true if the variable has valid combinedAttributes for 
     * colorBarMinimum and colorBarMaximum.
     * 
     * @return true if the variable has valid colorBarMinimum/Maximum attributes.
     */
    public boolean hasColorBarMinMax() {return hasColorBarMinMax;}


    /**
     * The raw attributes from the source.
     * 
     * @return the raw attributes from the source.
     */
    public Attributes sourceAttributes() {return sourceAttributes;}

    /**
     * The attributes which will be added when data is extracted
     * and which have precedence over sourceAttributes.
     * 
     * @return the attributes which will be added when data is extracted.
     */
    public Attributes addAttributes() {return addAttributes;}

    /**
     * The source+add attributes.
     * 
     * @return the source+add attributes.
     */
    public Attributes combinedAttributes() {return combinedAttributes; }


    /**
     * This converts a deg°[min'[sec"]][D] into decimal degrees.
     * deg, min, or sec can be a decimal value.
     * [min'[sec"]], [sec"], '[D]' is optional.
     * A 'D'irection value of E or N is ignored, but W or S is treated as *-1.
     *
     * @param location deg°[min'[sec"]][D]
     * @return the location as decimal degrees (or NaN if invalid)
     */
    public static double toDecimalDegrees(String location) {
        if (location == null) 
            return Double.NaN;
        location = location.trim();
        if (location.length() == 0)
            return Double.NaN;

        //deal with 'D'irection
        char end = location.charAt(location.length() - 1);
        double factor = 1;
        if (end == 'E' || end == 'N') {
            location = location.substring(0, location.length() - 1);
        } else if (end == 'W' || end == 'S') {
            factor = -1;
            location = location.substring(0, location.length() - 1);
        }; 
        int len = location.length();

        //just degrees?
        int degPo = location.indexOf('°');
        if (degPo < 0)
            degPo = len;
        if (degPo >= len - 1) 
            return factor * String2.parseDouble(location.substring(0, degPo));

        //just deg min?
        int minPo = location.indexOf('\'');
        if (minPo < 0)
            minPo = len;
        if (minPo >= len - 1) 
            return factor * 
                (String2.parseDouble(location.substring(0, degPo)) +
                 String2.parseDouble(location.substring(degPo + 1, minPo)) / 60);

        //deg min sec
        int secPo = location.indexOf('"');
        if (secPo < 0)
            secPo = len;
        return factor * 
            (String2.parseDouble(location.substring(0, degPo)) +
             String2.parseDouble(location.substring(degPo + 1, minPo)) / 60 +
             String2.parseDouble(location.substring(minPo + 1, secPo)) / 3600);
    }

    /**
     * This tests the methods of this class.
     * @throws Throwable if trouble.
     */
    public static void test() throws Throwable {
        String2.log("\n*** EDV.test()");
        Test.ensureEqual(toDecimalDegrees("1.1W"), -1.1, "");
        Test.ensureEqual(toDecimalDegrees("2.2E"), 2.2, "");
        Test.ensureEqual(toDecimalDegrees("3.3S"), -3.3, "");
        Test.ensureEqual(toDecimalDegrees("4.4N"), 4.4, "");
        Test.ensureEqual(toDecimalDegrees("1°2.3'W"), -(1 + 2.3/60.0), "");
        Test.ensureEqual(toDecimalDegrees("4°5.6'"),    4 + 5.6/60.0, "");
        Test.ensureEqual(toDecimalDegrees("1°2'3.4\"S"), -(1 + 2/60.0 + 3.4/3600.0), "");
        Test.ensureEqual(toDecimalDegrees("4°5'6.7\""),    4 + 5/60.0 + 6.7/3600.0, "");

        Test.ensureEqual(suggestLongName("real-time temp", "rt", null), "Real-time Temp", "");
        Test.ensureEqual(suggestLongName("real_time_temp", "rt", null), "Real Time Temp", "");
        Test.ensureEqual(suggestLongName("real.time.temp", "rt", null), "Real.time.temp", "");
        Test.ensureEqual(suggestLongName("RealTimeTemp",   "rt", null), "Real Time Temp", "");
        Test.ensureEqual(suggestLongName(null, "rhum", null), "Relative Humidity", "");
    }
}