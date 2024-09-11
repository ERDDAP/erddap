/*
 * EDVLat Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;

/**
 * This class holds information about the latitude variable, which is like EDV, but the
 * destinationName, and units are standardized.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVLat extends EDV {

  /**
   * The constructor -- like EDV, but the destinationName, and units are standardized.
   *
   * @param tSourceMin is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_min, or data_min metadata.
   * @param tSourceMax is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_max, or data_max metadata.
   */
  public EDVLat(
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
        LAT_NAME,
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
              + "The destination dataType for the latitude variable must be a numeric dataType.");

    combinedAttributes.set("_CoordinateAxisType", "Lat"); // unidata-related
    combinedAttributes.set("axis", "Y");
    combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
    combinedAttributes.set("long_name", longName);
    combinedAttributes.set("standard_name", LAT_STANDARD_NAME);

    longName = combinedAttributes.getString("long_name");
    if (longName == null
        || // catch nothing
        longName.toLowerCase().equals("lat")
        || longName.toLowerCase().equals("latitude")) { // catch alternate case
      longName = LAT_LONGNAME;
      combinedAttributes.set("long_name", longName);
    }

    units = LAT_UNITS;
    combinedAttributes.set("units", units);

    extractAndSetActualRange();
  }

  /**
   * This returns a string representation of this EDVLat.
   *
   * @return a string representation of this EDVLat.
   */
  @Override
  public String toString() {
    return "EDVLat/" + super.toString();
  }
}
