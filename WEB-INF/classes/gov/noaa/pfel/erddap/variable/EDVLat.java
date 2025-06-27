/*
 * EDVLat Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;

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
      LocalizedAttributes tAddAttributes,
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

    // The attributes this gets/sets should not need to be localized (max/min
    // value for example). Just use the default language.
    int language = EDMessages.DEFAULT_LANGUAGE;
    combinedAttributes.set(language, "_CoordinateAxisType", "Lat"); // unidata-related
    combinedAttributes.set(language, "axis", "Y");
    combinedAttributes.set(language, "ioos_category", LOCATION_CATEGORY);
    combinedAttributes.set(language, "long_name", longName);
    combinedAttributes.set(language, "standard_name", LAT_STANDARD_NAME);

    longName = combinedAttributes.getString(language, "long_name");
    if (longName == null
        || // catch nothing
        longName.equalsIgnoreCase("lat")
        || longName.equalsIgnoreCase("latitude")) { // catch alternate case
      longName = LAT_LONGNAME;
      combinedAttributes.set(language, "long_name", longName);
    }

    units = LAT_UNITS;
    combinedAttributes.set(language, "units", units);

    extractAndSetActualRange(language);
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
