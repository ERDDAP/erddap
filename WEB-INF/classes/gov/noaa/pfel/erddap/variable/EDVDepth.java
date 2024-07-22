/*
 * EDVDepth Copyright 2012, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;

/**
 * This class holds information about a depth variable, which is like EDV, but the destinationName,
 * long_name, and units are standardized, and you need to specify scale_factor to convert source
 * altitude/depth values to meters below sea level in the results.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVDepth extends EDV {

  /**
   * The constructor -- like EDV, but the destinationName, long_name, and units are standardized,
   * and you need to specify scale_factor to convert source altitude/depth values to meters below
   * sea level in the results.
   *
   * @param tSourceMin is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_min, or data_min metadata.
   * @param tSourceMax is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_max, or data_max metadata.
   * @throws Throwable if trouble
   */
  public EDVDepth(
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
        DEPTH_NAME,
        tSourceAttributes,
        tAddAttributes,
        tSourceDataType,
        tSourceMin,
        tSourceMax);

    if (destinationDataType().equals("String"))
      throw new RuntimeException(
          "datasets.xml error for datasetID="
              + tDatasetID
              + ": "
              + "The destination dataType for the depth variable must be a numeric dataType.");

    units = DEPTH_UNITS;
    combinedAttributes.set("_CoordinateAxisType", "Height"); // unidata
    combinedAttributes.set("_CoordinateZisPositive", "down"); // unidata
    combinedAttributes.set("axis", "Z");
    combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
    longName = combinedAttributes.getString("long_name");
    if (longName == null
        || // catch nothing
        longName.toLowerCase().equals("depth")) { // catch alternate case
      longName = DEPTH_LONGNAME;
      combinedAttributes.set("long_name", longName);
    }
    combinedAttributes.set("positive", "down"); // cf
    combinedAttributes.set("standard_name", DEPTH_STANDARD_NAME);
    EDVAlt.ensureUnitsAreM(combinedAttributes.getString("units"), "depth", "down");
    combinedAttributes.set("units", units);

    // set destinationMin max  if not set by tSourceMin,Max
    PAOne mm[] = extractActualRange(); // always extract
    setDestinationMinMax(mm[0], mm[1]);
    setActualRangeFromDestinationMinMax();

    PrimitiveArray pa = combinedAttributes.get("missing_value");
    if (pa != null) pa.setDouble(0, destinationMissingValue);
    pa = combinedAttributes.get("_FillValue");
    if (pa != null) pa.setDouble(0, destinationFillValue);
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @param errorInMethod the start string for an error message
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVDepth/" + super.toString();
  }

  /**
   * This is used by the EDD constructor to determine if this EDV is valid.
   *
   * @throws Throwable if this EDV is not valid
   */
  @Override
  public void ensureValid(String errorInMethod) throws Throwable {
    super.ensureValid(errorInMethod);
    errorInMethod +=
        "\ndatasets.xml/EDVDepth.ensureValid error for soureName=" + sourceName + ":\n";
  }
}
