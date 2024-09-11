/*
 * EDVAlt Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;

/**
 * This class holds information about an altitude variable, which is like EDV, but the
 * destinationName, long_name, and units are standardized, and you need to specify scale_factor to
 * convert source altitude/depth values to meters above sea level in the results.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVAlt extends EDV {

  public static String stopUsingAltitudeMetersPerSourceUnit =
      "Please stop using <altitudeMetersPerSourceUnit>.  "
          + "When the value is 1, just delete it.  "
          + "For other values, set the altitude variable's <scale_factor> instead.";

  /**
   * The constructor -- like EDV, but the destinationName, long_name, and units are standardized.
   *
   * @param tSourceMin is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_min, or data_min metadata.
   * @param tSourceMax is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_max, or data_max metadata.
   * @throws Throwable if trouble
   */
  public EDVAlt(
      String tDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      Attributes tAddAttributes,
      String tSourceDataType,
      PAOne tSourceMin,
      PAOne tSourceMax)
      throws Throwable {

    super(
        tDatasetID,
        tSourceName,
        ALT_NAME,
        tSourceAttributes,
        tAddAttributes,
        tSourceDataType,
        tSourceMin,
        tSourceMax);

    if (destinationDataType().equals("String"))
      throw new RuntimeException(
          "datasets.xml error: "
              + "The destination dataType for the altitude variable must be a numeric dataType.");

    units = ALT_UNITS;
    combinedAttributes.set("_CoordinateAxisType", "Height"); // unidata
    combinedAttributes.set("_CoordinateZisPositive", "up"); // unidata
    combinedAttributes.set("axis", "Z");
    combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
    longName = combinedAttributes.getString("long_name");
    if (longName == null
        || // catch nothing
        longName.toLowerCase().equals("alt")
        || longName.toLowerCase().equals("altitude")) { // catch alternate case
      longName = ALT_LONGNAME;
      combinedAttributes.set("long_name", longName);
    }
    combinedAttributes.set("positive", "up"); // cf
    combinedAttributes.set("standard_name", ALT_STANDARD_NAME);
    ensureUnitsAreM(combinedAttributes.getString("units"), "altitude", "up");
    combinedAttributes.set("units", units);

    // set destinationMin max  if not set by tSourceMin,Max
    PAOne mm[] = extractActualRange(); // always extract
    setDestinationMinMax(mm[0], mm[1]);
    setActualRangeFromDestinationMinMax();

    // destinationMissingValue and destinationFillValue have already been
    // adjusted for scaleAddOffset (including destinationDataType)
    PrimitiveArray pa = combinedAttributes.get("missing_value");
    if (pa != null) pa.setDouble(0, destinationMissingValue);
    pa = combinedAttributes.get("_FillValue");
    if (pa != null) pa.setDouble(0, destinationFillValue);
  }

  /**
   * This ensures that cUnits is "m" (or an alias) or throws a SimpleException.
   *
   * @param cUnits units from combinedAttributes (may be null)
   * @param altitudeDepth "altitude" or "depth" for the message
   * @param upDown "up" or "down" for the message.
   * @throws a SimpleException if cUnits isn't "m" or an alias.
   */
  public static void ensureUnitsAreM(String cUnits, String altitudeDepth, String upDown) {
    if (cUnits != null && String2.indexOf(EDV.METERS_VARIANTS, cUnits) >= 0) return;

    throw new SimpleException(
        "When a variable's destinationName is \""
            + altitudeDepth
            + "\", "
            + "the sourceAttributes or addAttributes \"units\" MUST be \"m\" (not \""
            + cUnits
            + "\").\n"
            + "If needed, use \"scale_factor\" to convert the source values to meters (positive="
            + upDown
            + "),\n"
            + "use a different destinationName for this variable.");
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @param errorInMethod the start string for an error message
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVAlt/" + super.toString();
  }

  /**
   * This is used by the EDD constructor to determine if this EDV is valid.
   *
   * @throws Throwable if this EDV is not valid
   */
  @Override
  public void ensureValid(String errorInMethod) throws Throwable {
    super.ensureValid(errorInMethod);
    errorInMethod += "\ndatasets.xml/EDVAlt.ensureValid error for soureName=" + sourceName + ":\n";
  }
}
