/*
 * EDVDepth Copyright 2012, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;

/**
 * This class holds information about a depth variable, which is like EDV, but the destinationName,
 * long_name, and units are standardized, and you need to specify scale_factor to convert source
 * altitude/depth values to meters below sea level in the results.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVDepth extends EDV {

  /**
   * The constructor -- like EDV, but the destinationName, and units are standardized.
   *
   * @param tSourceMin is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_min, or data_min metadata.
   * @param tSourceMax is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_max, or data_max metadata.
   */
  public EDVDepth(
      String tDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      LocalizedAttributes tAddAttributes,
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
    // The attributes this gets/sets should not need to be localized (max/min
    // value for example). Just use the default language.
    int language = EDMessages.DEFAULT_LANGUAGE;

    units = DEPTH_UNITS;
    combinedAttributes.set(language, "_CoordinateAxisType", "Height"); // unidata
    combinedAttributes.set(language, "_CoordinateZisPositive", "down"); // unidata
    combinedAttributes.set(language, "axis", "Z");
    combinedAttributes.set(language, "ioos_category", LOCATION_CATEGORY);
    longName = combinedAttributes.getString(language, "long_name");
    if (longName == null
        || // catch nothing
        longName.equalsIgnoreCase("depth")) { // catch alternate case
      longName = DEPTH_LONGNAME;
      combinedAttributes.set(language, "long_name", longName);
    }
    combinedAttributes.set(language, "positive", "down"); // cf
    combinedAttributes.set(language, "standard_name", DEPTH_STANDARD_NAME);
    EDVAlt.ensureUnitsAreM(combinedAttributes.getString(language, "units"), "depth", "down");
    combinedAttributes.set(language, "units", units);

    // set destinationMin max  if not set by tSourceMin,Max
    PAOne mm[] = extractActualRange(language); // always extract
    setDestinationMinMax(mm[0], mm[1]);
    setActualRangeFromDestinationMinMax(language);

    PrimitiveArray pa = combinedAttributes.get(language, "missing_value");
    if (pa != null) pa.setDouble(0, destinationMissingValue);
    pa = combinedAttributes.get(language, "_FillValue");
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
