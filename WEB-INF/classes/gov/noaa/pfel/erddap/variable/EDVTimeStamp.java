/*
 * EDVTimeStamp Copyright 2007, NOAA.
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
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class holds information about *a* (not *the*) time variable, which is like EDV, but the
 * units must be timestamp units, and you need to specify the sourceTimeFormat so that source values
 * can be converted to seconds since 1970-01-01 in the results.
 *
 * <p>There is the presumption, not requirement, that if there are two time-related variables, the
 * main one will be EDVTime and the secondary will be EDVTimeStamp (not both EDVTimeStamp).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04 Converted
 *     from EDVTime 2009-06-29.
 */
public class EDVTimeStamp extends EDV {

  /** Set by the constructor. */
  protected String sourceTimeFormat;

  /** These are set automatically. */
  protected boolean sourceTimeIsNumeric;

  protected double sourceTimeBase = Double.NaN; // set if sourceTimeIsNumeric
  protected double sourceTimeFactor = Double.NaN;
  // protected boolean parseISOWithCalendar2; //specifically, Calendar2.parseISODateTimeZulu(); else
  // parse with java.time (was Joda).
  protected String dateTimeFormat; // only used if !sourceTimeIsNumeric
  protected DateTimeFormatter
      dateTimeFormatter; // for generating source time if !sourceTimeIsNumeric
  protected String time_precision; // see Calendar2.epochSecondsToLimitedIsoStringT
  protected String time_zone; // if not specified, will be Zulu
  protected TimeZone timeZone = null; // for Java   null=Zulu

  /**
   * This class holds information about the time variable, which is like EDV, but the
   * destinationName is forced to be "time" and the destination units are standardized to seconds
   * since 1970-01-01 in the results.
   *
   * <p>Either tAddAttributes (read first) or tSourceAttributes must have "units" which is either
   *
   * <ul>
   *   <li>a UDUunits string (containing " since ") describing how to interpret source time values
   *       (e.g., "seconds since 1970-01-01T00:00:00") where the base time is an ISO 8601 formatted
   *       date time string (YYYY-MM-DDThh:mm:ss).
   *   <li>a java.time.format.DateTimeFormatter string (which is compatible with
   *       java.text.SimpleDateFormat) describing how to interpret string times. Any format that
   *       starts with "yyyy-MM", e.g., Calendar2.ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", will be
   *       parsed with Calendar2.parseISODateTimeZulu(). See
   *       https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   *       or
   *       https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html)).
   * </ul>
   *
   * This constructor gets/sets actual_range from actual_range, actual_min, actual_max, data_min, or
   * data_max metadata. If present, they should have the source min and max times in 'units' format.
   *
   * <p>scale_factor an dadd_offset are allowed for numeric time variables. This constructor removes
   * any scale_factor and add_offset attributes and stores the resulting information so that
   * destination data has been converted to destinationDataType with scaleFactor and addOffset
   * applied.
   *
   * @param tDestinationName should be "time" for *the* destination variable (type=EDVTime),
   *     otherwise some other name.
   * @throws Throwable if trouble
   */
  public EDVTimeStamp(
      String tDatasetID,
      String tSourceName,
      String tDestinationName,
      Attributes tSourceAttributes,
      Attributes tAddAttributes,
      String tSourceDataType)
      throws Throwable {

    super(
        tDatasetID,
        tSourceName,
        tDestinationName,
        tSourceAttributes,
        tAddAttributes,
        tSourceDataType,
        PAOne.fromDouble(Double.NaN),
        PAOne.fromDouble(Double.NaN)); // destinationMin and max are set below (via actual_range)

    // time_precision e.g., 1970-01-01T00:00:00Z
    time_precision = combinedAttributes.getString(EDV.TIME_PRECISION);
    if (time_precision != null) {
      // ensure not just year (can't distinguish user input a year vs. epochSeconds)
      if (time_precision.equals("1970")) time_precision = null;
      // ensure Z at end of time
      if (time_precision.length() >= 13 && !time_precision.endsWith("Z")) time_precision = null;
    }

    String errorInMethod = "datasets.xml/EDVTimeStamp error for sourceName=" + tSourceName + ":\n";

    // special processing of sourceTimeFormat  (before change units below)
    sourceTimeFormat = units();
    Test.ensureNotNothing(sourceTimeFormat, errorInMethod + "'units' wasn't set.");
    if (!Calendar2.isTimeUnits(sourceTimeFormat))
      throw new RuntimeException(
          errorInMethod + "units=" + sourceTimeFormat + " isn't a valid time format.");

    if (destinationName.equals(TIME_NAME)) { // *the* time variable
      combinedAttributes.set("_CoordinateAxisType", "Time"); // unidata-related
      combinedAttributes.set("axis", "T");
      String sn = combinedAttributes.getString("standard_name");
      if (sn == null || sn.length() == 0)
        combinedAttributes.set("standard_name", TIME_STANDARD_NAME);
    }
    combinedAttributes.set("ioos_category", TIME_CATEGORY);
    combinedAttributes.set("time_origin", "01-JAN-1970 00:00:00");

    units = TIME_UNITS;
    combinedAttributes.set("units", units);

    longName = combinedAttributes.getString("long_name");
    if (longName == null) { // catch nothing
      longName =
          suggestLongName(longName, destinationName, combinedAttributes.getString("standard_name"));
      combinedAttributes.set("long_name", longName);
    } else if (longName.toLowerCase().equals("time")) { // catch alternate case
      longName = TIME_LONGNAME;
      combinedAttributes.set("long_name", longName);
    }

    time_zone = combinedAttributes.getString("time_zone");
    combinedAttributes.remove("time_zone");
    if (!String2.isSomething(time_zone) || time_zone.equals("UTC")) time_zone = "Zulu";

    fixedValue = extractFixedValue(sourceName);
    if (isFixedValue()) {
      // extract fixedValue (must be epochSeconds)

      sourceTimeIsNumeric = true;
      sourceTimeBase = 0;
      sourceTimeFactor = 1;

      stringMissingValue = "";
      stringFillValue = "";
      safeStringMissingValue = "";
      combinedAttributes.remove("missing_value");
      combinedAttributes.remove("_FillValue");

      if (!"Zulu".equals(time_zone)) // UTC -> Zulu above
      throw new RuntimeException(
            "Currently, ERDDAP doesn't support time_zone's other than Zulu "
                + "and UTC for fixedValue numeric timestamp variables.");

    } else if (Calendar2.isNumericTimeUnits(sourceTimeFormat)) {
      // sourceTimeIsNumeric
      sourceTimeIsNumeric = true;

      stringMissingValue = "";
      stringFillValue = "";
      safeStringMissingValue = "";
      combinedAttributes.remove("missing_value");
      combinedAttributes.remove("_FillValue");

      if (!"Zulu".equals(time_zone)) // UTC -> Zulu above
      throw new RuntimeException(
            "Currently, ERDDAP doesn't support time_zone's other than Zulu "
                + "and UTC for fixedValue numeric timestamp variables.");

      double td[] = Calendar2.getTimeBaseAndFactor(sourceTimeFormat); // timeZone
      sourceTimeBase = td[0];
      sourceTimeFactor = td[1];

    } else {
      // source time is String
      sourceTimeIsNumeric = false;

      // For String source values, ensure scale_factor=1 and add_offset=0
      if (scaleAddOffset)
        throw new RuntimeException(
            errorInMethod
                + "For String source times, scale_factor and add_offset MUST NOT be used.");

      stringMissingValue = addAttributes.getString("missing_value");
      if (stringMissingValue == null)
        stringMissingValue = sourceAttributes.getString("missing_value");
      stringMissingValue = String2.canonical(stringMissingValue == null ? "" : stringMissingValue);
      combinedAttributes.remove("missing_value");

      stringFillValue = addAttributes.getString("_FillValue");
      if (stringFillValue == null) stringFillValue = sourceAttributes.getString("_FillValue");
      stringFillValue = String2.canonical(stringFillValue == null ? "" : stringFillValue);
      combinedAttributes.remove("_FillValue");

      safeStringMissingValue =
          String2.isSomething(stringFillValue) ? stringFillValue : stringMissingValue;

      // parseISOWithCalendar2 = Calendar2.parseWithCalendar2IsoParser(sourceTimeFormat);
      // if (verbose) String2.log("parseISOWithCalendar2=" + parseISOWithCalendar2);

      // dateTimeFormatter: for writing to source format
      dateTimeFormat = sourceTimeFormat;
      if ( // parseISOWithCalendar2 && //so pattern is only for writing
      dateTimeFormat.endsWith("Z")) // not 'Z'
        // force write 'Z' not +0000
        dateTimeFormat = String2.replaceAll(dateTimeFormat, "Z", "'Z'");

      if (time_zone.equals("Zulu")) { // UTC -> Zulu above
        dateTimeFormatter = Calendar2.makeDateTimeFormatter(dateTimeFormat, time_zone);
      } else {
        timeZone =
            TimeZone.getTimeZone(
                time_zone); // VERY BAD: if failure, no exception and it returns GMT timeZone!!!
        dateTimeFormatter = Calendar2.makeDateTimeFormatter(dateTimeFormat, time_zone);

        // NO EASY TEST IN JAVA VERSION OF java.time (was Joda)
        // verify that timeZone matches zoneId  (to deal with VERY BAD above)
        // Test.ensureEqual(
        //    timeZone.getRawOffset(), zoneId.getStandardOffset(0),
        //    errorInMethod +
        //    "The Java and Joda time_zone objects have different standard offsets, " +
        //    "probably because the time_zone is supported by Joda but not Java.");
      }
    }

    // then set missing_value  (as PAType.DOUBLE)
    destinationDataType = "double";
    destinationDataPAType = PAType.DOUBLE;
    destinationMissingValue = sourceTimeToEpochSeconds(destinationMissingValue);
    destinationFillValue = sourceTimeToEpochSeconds(destinationFillValue);
    safeDestinationMissingValue = sourceTimeToEpochSeconds(safeDestinationMissingValue);
    PrimitiveArray pa = combinedAttributes.get("missing_value");
    if (pa != null)
      combinedAttributes.set(
          "missing_value", new DoubleArray(new double[] {destinationMissingValue}));
    pa = combinedAttributes.get("_FillValue");
    if (pa != null)
      combinedAttributes.set("_FillValue", new DoubleArray(new double[] {destinationFillValue}));

    {
      // String2.log(">> combinedAtts=\n" + combinedAttributes.toString());

      // 1st priority: actual_range
      PrimitiveArray actualRange = combinedAttributes.get("actual_range");
      if (actualRange != null) {
        // String2.log(">>destMin=" + destinationMin + " max=" + destinationMax + "
        // sourceTimeIsNumeric=" + sourceTimeIsNumeric);
        // String2.log(">>actual_range metadata for " + destinationName + " (size=" +
        // actualRange.size() + "): " + actualRange);
        if (actualRange.elementType() == PAType.STRING && actualRange.size() == 1)
          actualRange = new StringArray(String2.split(actualRange.getString(0), '\n'));
        if (actualRange.size() == 2) {
          if (destinationMin.isMissingValue())
            destinationMin = PAOne.fromDouble(sourceTimeToEpochSeconds(actualRange.getString(0)));
          if (destinationMax.isMissingValue())
            destinationMax = PAOne.fromDouble(sourceTimeToEpochSeconds(actualRange.getString(1)));
        }
      }

      // 2nd priority: actual_min actual_max
      String tMin = combinedAttributes.getString("actual_min");
      String tMax = combinedAttributes.getString("actual_max");
      if (destinationMin.isMissingValue() && tMin != null && tMin.length() > 0)
        destinationMin = PAOne.fromDouble(sourceTimeToEpochSeconds(tMin));
      if (destinationMax.isMissingValue() && tMax != null && tMax.length() > 0)
        destinationMax = PAOne.fromDouble(sourceTimeToEpochSeconds(tMax));

      // 3rd priority: data_min data_max
      tMin = combinedAttributes.getString("data_min");
      tMax = combinedAttributes.getString("data_max");
      if (destinationMin.isMissingValue() && tMin != null && tMin.length() > 0)
        destinationMin = PAOne.fromDouble(sourceTimeToEpochSeconds(tMin));
      if (destinationMax.isMissingValue() && tMax != null && tMax.length() > 0)
        destinationMax = PAOne.fromDouble(sourceTimeToEpochSeconds(tMax));

      // swap if wrong order
      if (!destinationMin.isMissingValue()
          && !destinationMax.isMissingValue()
          && destinationMin.compareTo(destinationMax) > 0) {
        PAOne d = destinationMin;
        destinationMin = destinationMax;
        destinationMax = d;
      }
    }

    setActualRangeFromDestinationMinMax();
    if (reallyVerbose)
      String2.log(
          "\nEDVTimeStamp created, sourceTimeFormat="
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
    // this works because scaleAddOffset is always false (guaranteed in constructor)
    setDestinationMinMax(
        PAOne.fromDouble(sourceTimeToEpochSeconds(sourceMin)),
        PAOne.fromDouble(sourceTimeToEpochSeconds(sourceMax)));
  }

  /**
   * This determines if a variable is a TimeStamp variable by looking for " since " (used for
   * UDUNITS numeric times) or "yyyy" or "YYYY" (a formatting string which has the year designator)
   * in the units attribute.
   */
  public static boolean hasTimeUnits(Attributes sourceAttributes, Attributes addAttributes) {
    String tUnits = null;
    if (addAttributes != null) // priority
    tUnits = addAttributes.getString("units");
    if (tUnits == null && sourceAttributes != null) tUnits = sourceAttributes.getString("units");
    return Calendar2.isTimeUnits(tUnits);
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVTimeStamp/" + super.toString() + "  sourceTimeFormat=" + sourceTimeFormat + "\n";
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
    // errorInMethod += "\ndatasets.xml/EDVTimeStamp.ensureValid error for sourceName=" + sourceName
    // + ":\n";
    // sourceTimeFormat is checked in constructor
  }

  /**
   * This converts a destination double value to an ISO string with "Z". NaN returns "".
   *
   * @param destD
   * @return destination String
   */
  public String destinationToString(double destD) {
    return Calendar2.epochSecondsToLimitedIsoStringT(time_precision, destD, "");
  }

  /**
   * This is the destinationMin time value in the dataset (as an ISO date/time string, e.g.,
   * "1990-01-01T00:00:00Z").
   *
   * @return the destinationMin time (or "" if unknown)
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
   * @return the destinationMax time (or "" if unknown (and sometimes if ~now))
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
   * @param tSourceTimeFormat is either
   *     <ul>
   *       <li>a udunits string (containing " since ") describing how to interpret numbers (e.g.,
   *           "seconds since 1970-01-01T00:00:00"),
   *       <li>a java.time.format.DateTimeFormatter string (which is compatible with
   *           java.text.SimpleDateFormat) describing how to interpret string times (e.g., the
   *           ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   *           or
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html)).
   *       <li>null if this can be procured from the "units" source metadata.
   *     </ul>
   *     Examples: <br>
   *     Date and Time Pattern Result <br>
   *     "yyyy.MM.dd G 'at' HH:mm:ss z" 2001.07.04 AD at 12:08:56 PDT <br>
   *     "EEE, MMM d, ''yy" Wed, Jul 4, '01 <br>
   *     "yyyyy.MMMMM.dd GGG hh:mm aaa" 02001.July.04 AD 12:08 PM <br>
   *     "yyMMddHHmmssZ" 010704120856-0700 <br>
   *     "yyyy-MM-dd'T'HH:mm:ss.SSSZ" 2001-07-04T12:08:56.235-0700
   * @return the source time's units
   */
  public String sourceTimeFormat() {
    return sourceTimeFormat;
  }

  /**
   * This returns true if the source time is numeric or if fixedValue is in use.
   *
   * @return true if the source time is numeric or if fixedValue is in use.
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
   * If sourceTimeIsNumeric, this converts a source time to a destination ISO T time.
   *
   * @param sourceTime a numeric sourceTime
   * @return seconds since 1970-01-01T00:00:00. If sourceTime is NaN or Math2.almostEqual(5,
   *     sourceTime, sourceMissingValue) (which is very lenient), this returns NaN.
   */
  public double sourceTimeToEpochSeconds(double sourceTime) {
    // String2.log(">>sourceTimeToEpochSeconds(" + sourceTime + ") sourceTimeBase=" + sourceTimeBase
    // + " sourceTimeFactor=" + sourceTimeFactor);
    if ((Double.isNaN(sourceMissingValue) && Double.isNaN(sourceTime))
        || Math2.almostEqual(9, sourceTime, sourceMissingValue)
        || Math2.almostEqual(9, sourceTime, sourceFillValue)) return Double.NaN;
    if (scaleAddOffset) sourceTime = sourceTime * scaleFactor + addOffset;
    double d = Calendar2.unitsSinceToEpochSeconds(sourceTimeBase, sourceTimeFactor, sourceTime);
    // String2.log(">> sourceTimeToEp " + destinationName + " src=" + sourceTime + " ep=" + d +
    // " = " + Calendar2.safeEpochSecondsToIsoStringTZ(d, ""));
    return d;
  }

  /**
   * If sourceTimeIsNumeric or not, this converts a source time to seconds since
   * 1970-01-01T00:00:00Z. This non-raw version converts a stringMissingValue to NaN epochSeconds.
   *
   * @param sourceTime either a number (as a string) or a string
   * @return the source time converted to seconds since 1970-01-01T00:00:00Z. This returns NaN if
   *     trouble (sourceMissingValue, "", or invalid format).
   */
  public double sourceTimeToEpochSeconds(String sourceTime) {
    if (!String2.isSomething(sourceTime)
        || stringMissingValue.equals(sourceTime)
        || stringFillValue.equals(sourceTime)) return Double.NaN;
    // String2.log(">> sourceTime=" + sourceTime + " stringMV=" + stringMissingValue + " stringFV="
    // + stringFillValue);
    return rawSourceTimeToEpochSeconds(sourceTime);
  }

  /**
   * If sourceTimeIsNumeric or not, this converts a source time to seconds since
   * 1970-01-01T00:00:00Z. The raw version converts a stringMissingValue to epochSeconds (not NaN).
   *
   * @param sourceTime either a number (as a string) or a string
   * @return the source time converted to seconds since 1970-01-01T00:00:00Z. This returns NaN if
   *     trouble (sourceMissingValue, "", or invalid format).
   */
  public double rawSourceTimeToEpochSeconds(String sourceTime) {
    // sourceTime is numeric
    if (sourceTimeIsNumeric) return sourceTimeToEpochSeconds(String2.parseDouble(sourceTime));

    // time is a string
    try {
      double d = // parseISOWithCalendar2?
          // parse with Calendar2.parseISODateTime
          // Calendar2.isoStringToEpochSeconds(sourceTime, timeZone) :
          // parse sourceTime
          Calendar2.parseToEpochSeconds(sourceTime, dateTimeFormat, timeZone);
      // String2.log("  EDVTimeStamp sourceTime=" + sourceTime + " epSec=" + d + " Calendar2=" +
      // Calendar2.epochSecondsToIsoStringTZ(d));
      return d;
    } catch (Throwable t) {
      if (verbose && sourceTime != null && sourceTime.length() > 0)
        String2.log(
            "  EDVTimeStamp.sourceTimeToEpochSeconds: Invalid sourceTime="
                + sourceTime
                + "\n"
                + t.toString());
      return Double.NaN;
    }
  }

  /**
   * This returns a PrimitiveArray (the original if the data type wasn't changed) with source values
   * converted to destinationValues. This doesn't change the order of the values.
   *
   * <p>This version currently doesn't support scaleAddOffset.
   *
   * @param source
   * @return a PrimitiveArray (the original if the data type wasn't changed) with source values
   *     converted to destinationValues. Here, destination will be double epochSecond values.
   */
  @Override
  public PrimitiveArray toDestination(PrimitiveArray source) {

    // this doesn't support scaleAddOffset
    int size = source.size();
    DoubleArray destPa =
        source instanceof DoubleArray da
            ? da
            : // make changes in place
            new DoubleArray(size, true); // make a new array
    if (sourceTimeIsNumeric) {
      if (setSourceMaxIsMV) source.setMaxIsMV(true);
      for (int i = 0; i < size; i++) destPa.set(i, sourceTimeToEpochSeconds(source.getDouble(i)));
    } else {
      String lastS = "";
      double lastD = Double.NaN;
      for (int i = 0; i < size; i++) {
        // Conversion can be slow. So use previous result if same previous source value.
        // This commonly happens for audio files when source is string from file name.
        String s = source.getString(i);
        if (s.equals(lastS)) destPa.set(i, lastD);
        else destPa.set(i, lastD = sourceTimeToEpochSeconds(lastS = s));
      }
    }
    return destPa;
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
    if (sourceTimeIsNumeric) {
      for (int i = 0; i < size; i++)
        source.setDouble(i, epochSecondsToSourceTimeDouble(destination.getDouble(i)));
    } else {
      for (int i = 0; i < size; i++)
        source.setString(i, epochSecondsToSourceTimeString(destination.getDouble(i)));
    }
    return source;
  }

  /**
   * This converts a source time to a (limited) destination ISO TZ time.
   *
   * @param sourceTime either a number (as a string) or a string
   * @return a (limited) ISO T Time (e.g., 1993-12-31T23:59:59Z). If sourceTime is invalid or is
   *     sourceMissingValue, this returns "".
   */
  public String sourceTimeToIsoStringT(String sourceTime) {
    return Calendar2.epochSecondsToLimitedIsoStringT(
        time_precision, sourceTimeToEpochSeconds(sourceTime), "");
  }

  /**
   * Call this if sourceTimeIsNumeric to convert epochSeconds to a numeric sourceTime.
   *
   * @param epochSeconds seconds since 1970-01-01T00:00:00.
   * @return sourceTime If epochSeconds is NaN, this returns sourceMissingValue
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
    if (Double.isNaN(epochSeconds))
      return sourceTimeIsNumeric ? "" + sourceMissingValue : safeStringMissingValue;
    if (sourceTimeIsNumeric) return "" + epochSecondsToSourceTimeDouble(epochSeconds);
    return Calendar2.format(epochSeconds, dateTimeFormatter);
  }

  /**
   * This returns a JSON-style csv String with a subset of destinationStringValues suitable for use
   * on a slider with SLIDER_PIXELS. This overwrites the superclass version.
   *
   * <p>If destinationMin or destinationMax (except time) aren't finite, this returns null.
   */
  @Override
  public String sliderCsvValues() throws Throwable {
    if (sliderCsvValues != null) return String2.utf8BytesToString(sliderCsvValues);

    try {
      boolean isTime = true;
      double tMin = destinationMinDouble();
      double tMax = destinationMaxDouble();
      if (!Double.isFinite(tMin)) return null;
      if (!Double.isFinite(tMax)) {
        // next midnight Z
        GregorianCalendar gc = Calendar2.newGCalendarZulu();
        Calendar2.clearSmallerFields(gc, Calendar2.DATE);
        gc.add(Calendar2.DATE, 1);
        tMax = Calendar2.gcToEpochSeconds(gc);
      }

      // get the values from Calendar2
      double values[] = Calendar2.getNEvenlySpaced(tMin, tMax, SLIDER_MAX_NVALUES);
      StringBuilder sb =
          new StringBuilder(
              toSliderString( // first value
                  Calendar2.epochSecondsToLimitedIsoStringT(time_precision, tMin, ""), isTime));
      int nValues = values.length;
      for (int i = 1; i < nValues; i++) {
        sb.append(", ");
        sb.append(
            toSliderString(
                Calendar2.epochSecondsToLimitedIsoStringT(time_precision, values[i], ""), isTime));
      }

      // store in compact utf8 format
      String csv = sb.toString();
      if (reallyVerbose) String2.log("EDVTimeStamp.sliderCsvValues nValues=" + nValues);
      sliderCsvValues = String2.stringToUtf8Bytes(csv); // do last
      return csv;
    } catch (Throwable t) {
      EDStatic.rethrowClientAbortException(t); // first thing in catch{}
      String2.log(MustBe.throwableToString(t));
      return null;
    }
  }
}
