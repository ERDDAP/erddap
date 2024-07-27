/*
 * EDVTimeStampGridAxis Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * This class holds information about a timestamp grid axis variable.
 *
 * <p>[STRING TIMES NOT FINISHED: The handling of String times in incomplete and probably not a good
 * approach. Probably better is: really encapsulate the strings, so that any users of this class
 * just see/deal with numeric values (epochSoconds). There are just too many places where it is
 * assumed that all axes are numeric.]
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2014-10-07
 */
public class EDVTimeStampGridAxis extends EDVGridAxis {

  /** Set by the constructor. */
  protected String sourceTimeFormat;

  /** These are set automatically. */
  protected boolean sourceTimeIsNumeric = true;

  protected double sourceTimeBase = Double.NaN; // set if sourceTimeIsNumeric
  protected double sourceTimeFactor = Double.NaN;
  protected boolean parseISOWithCalendar2;
  protected String dateTimeFormat; // only used if !sourceTimeIsNumeric, which currently is never
  protected DateTimeFormatter
      dateTimeFormatter; // currently never used: for generating source time if !sourceTimeIsNumeric
  protected String time_precision; // see Calendar2.epochSecondsToLimitedIsoStringT
  protected boolean superConstructorIsFinished = false;
  protected String timeZoneString; // if not specified, will be Zulu
  protected TimeZone timeZone; // if not specified, will be Zulu

  /**
   * The constructor.
   *
   * <p>Either tAddAttributes (read first) or tSourceAttributes must have "units" which is a
   * UDUunits string (containing " since ") describing how to interpret source time values (which
   * should always be numeric since they are a dimension of a grid) (e.g., "seconds since
   * 1970-01-01T00:00:00"), where the base time is an ISO 8601 formatted date time string
   * (YYYY-MM-DDThh:mm:ss).
   *
   * <p>scale_factor and add_offset are allowed for numeric time variables. This constructor removes
   * any scale_factor and add_offset attributes and stores the resulting information so that
   * destination data has been converted to destinationDataType with scaleFactor and addOffset
   * applied.
   *
   * @param tParentDatasetID This is needed if dimensionValuesInMemory is false, so sourceValues
   *     sometimes need to be read from [cacheDirectory(tParentDatasetID)]/dimensionSourceValues.nc
   * @param tSourceName the name of the axis variable in the dataset source (usually with no
   *     spaces).
   * @param tDestinationName should be "time" for *the* destination variable (type=EDVTimeGridAxis),
   *     otherwise some other name. If null or "", tSourceName will be used.
   * @param tSourceAttributes are the attributes for the variable in the source
   * @param tAddAttributes the attributes which will be added when data is extracted and which have
   *     precedence over sourceAttributes. Special case: value="null" causes that item to be removed
   *     from combinedAttributes. If this is null, an empty addAttributes will be created.
   * @param tSourceValues has the values from the source. This can't be a StringArray. There must be
   *     at least one element.
   * @throws Throwable if trouble
   */
  public EDVTimeStampGridAxis(
      String tParentDatasetID,
      String tSourceName,
      String tDestinationName,
      Attributes tSourceAttributes,
      Attributes tAddAttributes,
      PrimitiveArray tSourceValues)
      throws Throwable {

    super(
        tParentDatasetID,
        tSourceName,
        tDestinationName,
        tSourceAttributes,
        tAddAttributes,
        tSourceValues);
    superConstructorIsFinished = true;

    // time_precision e.g., 1970-01-01T00:00:00Z
    time_precision = combinedAttributes.getString(EDV.TIME_PRECISION);
    if (time_precision != null) {
      // ensure not just year (can't distinguish user input a year vs. epochSeconds)
      if (time_precision.equals("1970")) time_precision = null;
      // ensure Z at end of time
      if (time_precision.length() >= 13 && !time_precision.endsWith("Z")) time_precision = null;
    }

    // currently, EDVTimeStampGridAxis doesn't support String sourceValues
    String errorInMethod =
        "datasets.xml/EDVTimeStampGridAxis constructor error for sourceName=" + tSourceName + ":\n";
    if (tSourceValues instanceof StringArray)
      throw new RuntimeException(
          errorInMethod
              + "Currently, EDVTimeStampGridAxis doesn't support String source "
              + "values for the time axis.");

    // read units before it is changed below
    sourceTimeFormat = units();
    Test.ensureNotNothing(
        sourceTimeFormat, errorInMethod + "'units' wasn't found."); // match name in datasets.xml

    timeZoneString = combinedAttributes.getString("time_zone");
    combinedAttributes.remove("time_zone");
    if (!String2.isSomething(timeZoneString)) timeZoneString = "Zulu";
    timeZone = TimeZone.getTimeZone(timeZoneString);

    if (Calendar2.isNumericTimeUnits(sourceTimeFormat)) {
      sourceTimeIsNumeric = true;
      double td[] = Calendar2.getTimeBaseAndFactor(sourceTimeFormat);
      sourceTimeBase = td[0];
      sourceTimeFactor = td[1];
      if (!"Zulu".equals(timeZoneString) && !"UTC".equals(timeZoneString))
        throw new RuntimeException(
            "Currently, ERDDAP doesn't support time_zone's other than Zulu "
                + "and UTC for numeric axis timestamp variables.");

    } else {
      sourceTimeIsNumeric = false;
      throw new RuntimeException(
          "Currently, String time axes are not supported. "
              + "The source units for the time axis must include \" since \".");
      /*  If Strings are ever supported...
      //deal with timeZoneString! see EDVTimeStamp
      //ensure scale_factor=1 and add_offset=0
      if (scaleAddOffset)
          throw new RuntimeException(errorInMethod +
              "For String source times, scale_factor and add_offset MUST NOT be used.");

      dateTimeFormat = sourceTimeFormat;
      if (dateTimeFormat.equals(ISO8601T_FORMAT) ||
          dateTimeFormat.equals(ISO8601TZ_FORMAT)) {
          if (verbose) String2.log("parseISOWithCalendar2=true");
          dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis().withZone(ZoneId.of(timeZoneString));
          parseISOWithCalendar2 = true;
      } else if (sourceTimeFormat.equals(ISO8601T3_FORMAT) ||
                 sourceTimeFormat.equals(ISO8601T3Z_FORMAT)) {
          if (verbose) String2.log("parseISOWithCalendar2=true");
          dateTimeFormatter = ISODateTimeFormat.dateTime().withZone(ZoneId.of(timeZoneString));
          parseISOWithCalendar2 = true;
      } else {
          //FUTURE: support time zones
          dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat, timeZoneString);
          parseISOWithCalendar2 = false;
      }
      */
    }

    units = TIME_UNITS;
    if (destinationName.equals(EDV.TIME_NAME)) {
      combinedAttributes.set("_CoordinateAxisType", "Time"); // unidata-related
      combinedAttributes.set("axis", "T");
    }
    combinedAttributes.set("ioos_category", TIME_CATEGORY);
    combinedAttributes.set("standard_name", TIME_STANDARD_NAME);
    combinedAttributes.set("time_origin", "01-JAN-1970 00:00:00");
    combinedAttributes.set("units", units);
    longName = combinedAttributes.getString("long_name");
    if (longName == null
        || longName.toLowerCase().equals("time")) // catch nothing or alternate case
    combinedAttributes.set("long_name", TIME_LONGNAME);
    longName = combinedAttributes.getString("long_name");

    // previously computed evenSpacing is fine
    // since source must be numeric, isEvenlySpaced is fine.
    // If "months since", it's better than recalculating since recalc will reflect
    //  different number of days in months.

    // set destinationMin max and actual_range
    // (they were temporarily source values)
    // (they will be destination values in epochSeconds)
    // (simpler than EDVTimeStamp because always numeric and range known from axis values)
    destinationDataType = "double";
    destinationDataPAType = PAType.DOUBLE;
    int n = tSourceValues.size();
    setDestinationMinMaxFromSource(
        tSourceValues.getNiceDouble(0), tSourceValues.getNiceDouble(n - 1));
    if (destinationMin.isMissingValue())
      throw new RuntimeException(
          "ERROR related to time values and/or time source units: "
              + "[0]="
              + tSourceValues.getString(0)
              + " => NaN epochSeconds.");
    if (destinationMax.isMissingValue())
      throw new RuntimeException(
          "ERROR related to time values and/or time source units: "
              + "[n-1]="
              + tSourceValues.getString(n - 1)
              + " => NaN epochSeconds.");

    setActualRangeFromDestinationMinMax();
    initializeAverageSpacingAndCoarseMinMax();
    if (reallyVerbose)
      String2.log(
          "\nEDVTimeStampGridAxis created, "
              + "sourceTimeFormat="
              + sourceTimeFormat
              + "\n "
              + " destMin="
              + destinationMin
              + "="
              + Calendar2.safeEpochSecondsToIsoStringTZ(destinationMin.getDouble(), "")
              + " destMax="
              + destinationMax
              + "="
              + Calendar2.safeEpochSecondsToIsoStringTZ(destinationMax.getDouble(), "")
              + "\n");
  }

  /**
   * This overwrites the EDV method of the same name in order to deal with numeric source time other
   * than "seconds since 1970-01-01T00:00:00Z".
   */
  public void setDestinationMinMaxFromSource(double sourceMin, double sourceMax) {
    // scaleAddOffset is allowed!! and applied by superclass' setDestinationMinMax!
    //   ??? I that correct order???
    setDestinationMinMax(
        PAOne.fromDouble(sourceTimeToEpochSeconds(sourceMin)),
        PAOne.fromDouble(sourceTimeToEpochSeconds(sourceMax)));
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVTimeStampGridAxis/"
        + super.toString()
        + "  sourceTimeFormat="
        + sourceTimeFormat
        + "\n";
  }

  /**
   * This is used by the EDD constructor to determine if this EDV is valid.
   *
   * @param errorInMethod the start string for an error message
   * @throws Throwable if this EDV is not valid
   */
  @Override
  public void ensureValid(String errorInMethod) throws Throwable {
    super.ensureValid(errorInMethod);
    // sourceTimeFormat is validated in constructor
  }

  /**
   * sourceTimeFormat is either a udunits string describing how to interpret numbers (e.g., "seconds
   * since 1970-01-01T00:00:00") or a java.text.SimpleDateFormat string describing how to interpret
   * string times (see
   * https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html)).
   * Examples: <br>
   * Date and Time Pattern Result <br>
   * "yyyy.MM.dd G 'at' HH:mm:ss z" 2001.07.04 AD at 12:08:56 PDT <br>
   * "EEE, MMM d, ''yy" Wed, Jul 4, '01 <br>
   * "yyyyy.MMMMM.dd GGG hh:mm aaa" 02001.July.04 AD 12:08 PM <br>
   * "yyMMddHHmmssZ" 010704120856-0700 <br>
   * "yyyy-MM-dd'T'HH:mm:ss.SSSZ" 2001-07-04T12:08:56.235-0700
   *
   * @return the source time's units
   */
  public String sourceTimeFormat() {
    return sourceTimeFormat;
  }

  /**
   * This returns true if the source time is numeric.
   *
   * @return true if the source time is numeric.
   */
  public boolean sourceTimeIsNumeric() {
    return sourceTimeIsNumeric;
  }

  /**
   * This returns true if the destinationValues equal the sourceValues (e.g., scaleFactor = 1 and
   * addOffset = 0). <br>
   * Some subclasses overwrite this to cover other situations: <br>
   * EDVTimeStamp only returns true if sourceTimeIsNumeric and sourceTimeBase = 0 and
   * sourceTimeFactor = 1.
   *
   * @return true if the destinationValues equal the sourceValues.
   */
  @Override
  public boolean destValuesEqualSourceValues() {
    return sourceTimeIsNumeric
        && sourceTimeBase == 0.0
        && sourceTimeFactor == 1.0
        && !scaleAddOffset;
  }

  /**
   * This converts a destination double value to a string (time variable overwrite this to make an
   * iso string). NaN returns "".
   *
   * @param destD epochSeconds
   * @return destination String
   */
  @Override
  public String destinationToString(double destD) {
    return Calendar2.epochSecondsToLimitedIsoStringT(time_precision, destD, "");
  }

  /**
   * This converts a destination String value to a destination double (time variable overwrites this
   * to catch iso 8601 strings). "" or null returns NaN.
   *
   * @param destS
   * @return destination double
   */
  @Override
  public double destinationToDouble(String destS) {
    if (destS == null || destS.length() == 0) return Double.NaN;
    if (Calendar2.isIsoDate(destS))
      return Calendar2.isoStringToEpochSeconds(destS); // to millis precision
    return String2.parseDouble(destS);
  }

  /**
   * This is the destinationMin time value in the dataset (as an ISO date/time string, e.g.,
   * "1990-01-01T00:00:00Z").
   *
   * @return the destinationMin time
   */
  @Override
  public String destinationMinString() {
    return destinationToString(
        destinationMin
            .getDouble()); // time always full precision, not "niceDouble", but it is already a
    // double
  }

  /**
   * This is the destinationMax time value in the dataset (an ISO date/time string, e.g.,
   * "2005-12-31T23:59:59Z").
   *
   * @return the destinationMax time
   */
  @Override
  public String destinationMaxString() {
    return destinationToString(
        destinationMax
            .getDouble()); // time always full precision, not "niceDouble", but it is already a
    // double
  }

  /**
   * An indication of the precision of the time values, e.g., "1970-01-01T00:00:00Z" (default) or
   * null (goes to default). See Calendar2.epochSecondsToLimitedIsoStringT()
   */
  public String time_precision() {
    return time_precision;
  }

  /**
   * If sourceTimeIsNumeric, this converts a source time to an ISO T time.
   *
   * @param sourceTime a numeric sourceTime
   * @return seconds since 1970-01-01T00:00:00. If sourceTime is NaN, this returns NaN (but there
   *     shouldn't ever be missing values).
   */
  public double sourceTimeToEpochSeconds(double sourceTime) {
    if (scaleAddOffset) sourceTime = sourceTime * scaleFactor + addOffset;
    double sec = Calendar2.unitsSinceToEpochSeconds(sourceTimeBase, sourceTimeFactor, sourceTime);
    // if (reallyVerbose)
    //    String2.log("    EDVTimeStampGridAxis stBase=" + sourceTimeBase +
    //        " scale=" + scaleFactor + " addOffset=" + addOffset +
    //        " stFactor=" + sourceTimeFactor + " sourceTime=" + sourceTime +
    //        " result=" + sec + " = " + Calendar2.epochSecondsToIsoStringTZ(sec));
    return sec;
  }

  /**
   * If sourceTimeIsNumeric or not, this converts a source time to seconds since
   * 1970-01-01T00:00:00Z.
   *
   * @param sourceTime either a number (as a string) or a string
   * @return the source time converted to seconds since 1970-01-01T00:00:00Z. This returns NaN if
   *     trouble (sourceMissingValue, "", or invalid format).
   */
  public double sourceTimeToEpochSeconds(String sourceTime) {
    // This method is called twice by the EDVGridAxis constructor (needlessly).
    // Apparently, sourceTimeIsNumeric hasn't yet been set to true,
    //  so this throws Exception.   So just avoid this error.
    // String2.log(">>sttes sourceTimeIsNumeric=" + sourceTimeIsNumeric);
    if (!superConstructorIsFinished) return Double.NaN;

    // sourceTime is numeric
    if (sourceTimeIsNumeric) return sourceTimeToEpochSeconds(String2.parseDouble(sourceTime));

    // time is a string
    try {
      double d =
          parseISOWithCalendar2
              ?
              // parse with Calendar2.parseISODateTime
              Calendar2.isoStringToEpochSeconds(sourceTime)
              :
              // parse
              Calendar2.parseToEpochSeconds(sourceTime, dateTimeFormat, timeZone); // thread safe
      // String2.log("  EDVTimeStampGridAxis sourceTime=" + sourceTime +
      //    " epSec=" + d + " Calendar2=" + Calendar2.epochSecondsToIsoStringTZ(d));
      return d;
    } catch (Throwable t) {
      if (verbose && sourceTime != null && sourceTime.length() > 0)
        String2.log(
            "  EDVTimeStampGridAxis.sourceTimeToEpochSeconds: "
                + "Invalid sourceTime="
                + sourceTime
                + "\n"
                + (reallyVerbose ? MustBe.throwableToString(t) : t.toString()));
      return Double.NaN;
    }
  }

  /**
   * This returns a PrimitiveArray (the original if the data type wasn't changed) with source values
   * converted to destinationValues.
   *
   * <p>Time variables will return a DoubleArray.
   *
   * @return a PrimitiveArray (the original if the data type wasn't changed) with source values
   *     converted to destinationValues.
   */
  @Override
  public PrimitiveArray toDestination(PrimitiveArray source) {
    int size = source.size();
    DoubleArray destPa = source instanceof DoubleArray da ? da : new DoubleArray(size, true);
    if (sourceTimeIsNumeric) {
      if (setSourceMaxIsMV) source.setMaxIsMV(true);
      for (int i = 0; i < size; i++)
        destPa.set(i, sourceTimeToEpochSeconds(source.getNiceDouble(i)));
    } else {
      for (int i = 0; i < size; i++) destPa.set(i, sourceTimeToEpochSeconds(source.getString(i)));
    }
    return destPa;
  }

  /**
   * This returns a new StringArray with source values converted to String destinationValues.
   *
   * @return a StringArray (the original if the data type wasn't changed) with source values
   *     converted to destinationValues.
   */
  public PrimitiveArray toDestinationStrings(PrimitiveArray source) {
    // memory is an issue! always generate this on-the-fly
    int n = source.size();
    StringArray sa = source instanceof StringArray tsa ? tsa : new StringArray(n, true);
    if (sourceTimeIsNumeric) {
      for (int i = 0; i < n; i++)
        sa.set(
            i,
            Calendar2.epochSecondsToLimitedIsoStringT(
                time_precision, sourceTimeToEpochSeconds(source.getNiceDouble(i)), ""));
    } else {
      for (int i = 0; i < n; i++)
        sa.set(
            i,
            Calendar2.epochSecondsToLimitedIsoStringT(
                time_precision, sourceTimeToEpochSeconds(source.getString(i)), ""));
    }
    return sa;
  }

  /**
   * This returns a PrimitiveArray (the original if the data type wasn't changed) with destination
   * values converted to sourceValues. This doesn't change the order of the values.
   *
   * <p>This version currently doesn't support scaleAddOffset.
   *
   * @param destination epochSecond double values
   * @return a PrimitiveArray (the original if the data type wasn't changed) with destination values
   *     converted to sourceValues.
   */
  @Override
  public PrimitiveArray toSource(PrimitiveArray destination) {
    // this doesn't support scaleAddOffset
    int size = destination.size();
    PrimitiveArray source =
        sourceDataPAType == destination.elementType()
            ? destination
            : PrimitiveArray.factory(sourceDataPAType, size, true);
    if (source instanceof StringArray) {
      for (int i = 0; i < size; i++)
        source.setString(i, epochSecondsToSourceTimeString(destination.getDouble(i)));
    } else {
      for (int i = 0; i < size; i++)
        source.setDouble(i, epochSecondsToSourceTimeDouble(destination.getNiceDouble(i)));
    }
    return source;
  }

  /**
   * This returns the PrimitiveArray with the destination values for this axis. Don't change these
   * values. This returns the sourceValues (with scaleFactor and addOffset if active; alt is
   * special; time is special). This doesn't change the order of the values (even if source is depth
   * and dest is altitude).
   */
  @Override
  public PrimitiveArray destinationValues() {
    // alt and time may modify the values, so use sourceValues.clone()
    return toDestination((PrimitiveArray) sourceValues().clone());
  }

  /** This returns one of this axis' source values as epochSeconds. */
  @Override
  public double destinationDouble(int which) {
    PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
    return sourceTimeIsNumeric
        ? sourceTimeToEpochSeconds(tSourceValues.getNiceDouble(which))
        : sourceTimeToEpochSeconds(tSourceValues.getString(which));
  }

  /**
   * This returns one of this axis' source values as a nice String destination value. For most
   * EDVGridAxis, this returns destinationValues (which equal the String destination values). The
   * Time subclass overwrites this.
   */
  @Override
  public String destinationString(int which) {
    return destinationToString(destinationDouble(which));
  }

  /**
   * This returns a PrimitiveArray with the destination values for this axis. Don't change these
   * values. If destination=source, this may return the sourceValues PrimitiveArray. The alt and
   * time subclasses overwrite this. The time subclass returns these as ISO 8601 'T' strings (to
   * facilitate displaying options to users). !!!For time, if lots of values (e.g., 10^6), this is
   * SLOW (e.g., 30 seconds)!!!
   */
  @Override
  public PrimitiveArray destinationStringValues() {
    return toDestinationStrings(sourceValues());
  }

  /**
   * This converts epochSeconds to a numeric sourceTime.
   *
   * @param epochSeconds seconds since 1970-01-01T00:00:00Z.
   * @return sourceTime. If sourceTime is NaN, this returns sourceMissingValue (but there shouldn't
   *     ever be missing values).
   */
  public double epochSecondsToSourceTimeDouble(double epochSeconds) {
    if (Double.isNaN(epochSeconds)) return sourceMissingValue;
    double source =
        Calendar2.epochSecondsToUnitsSince(sourceTimeBase, sourceTimeFactor, epochSeconds);
    if (scaleAddOffset) source = (source - addOffset) / scaleFactor;
    return source;
  }

  /**
   * Call this whether or not sourceTimeIsNumeric to convert epochSeconds to sourceTime (numeric, or
   * via dateTimeFormatter).
   *
   * @param epochSeconds seconds since 1970-01-01T00:00:00.
   * @return the corresponding sourceTime (numeric, or via dateTimeFormatter). If epochSeconds is
   *     NaN, this returns sourceMissingValue (if sourceTimeIsNumeric) or "".
   */
  public String epochSecondsToSourceTimeString(double epochSeconds) {
    if (Double.isNaN(epochSeconds)) return sourceTimeIsNumeric ? "" + sourceMissingValue : "";
    if (sourceTimeIsNumeric) return "" + epochSecondsToSourceTimeDouble(epochSeconds);
    return Calendar2.format(epochSeconds, dateTimeFormatter);
  }

  /**
   * This converts a source time to a (limited) destination ISO TZ time.
   *
   * @param sourceTime
   * @return a (limited) ISO T Time (e.g., "1993-12-31T23:59:59Z"). If sourceTime is invalid, this
   *     returns "" (but there shouldn't ever be missing values).
   */
  public String sourceTimeToIsoStringT(double sourceTime) {
    double destD = sourceTimeToEpochSeconds(sourceTime);
    return destinationToString(destD);
  }

  /**
   * This converts a destination ISO time to a source time.
   *
   * @param isoString an ISO T Time (e.g., "1993-12-31T23:59:59").
   * @return sourceTime
   * @throws Throwable if ISO time is invalid
   */
  public double isoStringToSourceTime(String isoString) {
    return epochSecondsToSourceTimeDouble(Calendar2.isoStringToEpochSeconds(isoString));
  }

  /**
   * This determines if a variable is a TimeStamp variable by looking for " since " (used for
   * UDUNITS numeric times). Currently, this does not look for String time units ("yyyy" or "YYYY",
   * a formatting string which has the year designator) in the units attribute because this class
   * currently doesn't support String times.
   */
  public static boolean hasTimeUnits(Attributes sourceAttributes, Attributes addAttributes) {
    String tUnits = null;
    if (addAttributes != null) // priority
    tUnits = addAttributes.getString("units");
    if (tUnits == null && sourceAttributes != null) tUnits = sourceAttributes.getString("units");
    return hasTimeUnits(tUnits);
  }

  /**
   * This determines if a variable is a TimeStamp variable by looking for " since " (used for
   * UDUNITS numeric times). Currently, this does not look for String time units ("yyyy" or "YYYY",
   * a formatting string which has the year designator) in the units attribute because this class
   * currently doesn't support String times.
   */
  public static boolean hasTimeUnits(String tUnits) {
    return Calendar2.isNumericTimeUnits(tUnits);
  }
}
