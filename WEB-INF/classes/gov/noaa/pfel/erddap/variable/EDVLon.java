/*
 * EDVLon Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;

/**
 * This class holds information about the longitude variable, which is like EDV, but the
 * destinationName, and units are standardized.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVLon extends EDV {

  /**
   * The constructor -- like EDV, but the destinationName, and units are standardized.
   *
   * @param tSourceMin is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_min, or data_min metadata.
   * @param tSourceMax is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_max, or data_max metadata.
   */
  public EDVLon(
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
        LON_NAME,
        tSourceAttributes,
        tAddAttributes,
        tSourceDataType,
        tSourceMin,
        tSourceMax);

    // The attributes this gets/sets should not need to be localized (max/min
    // value for example). Just use the default language.
    int language = EDMessages.DEFAULT_LANGUAGE;
    if (destinationDataType().equals("String"))
      throw new RuntimeException(
          "datasets.xml error for datasetID="
              + tDatasetID
              + ": "
              + "The destination dataType for the longitude variable must be a numeric dataType.");

    combinedAttributes.set(language, "_CoordinateAxisType", "Lon"); // unidata-related
    combinedAttributes.set(language, "axis", "X");
    combinedAttributes.set(language, "ioos_category", LOCATION_CATEGORY);
    combinedAttributes.set(language, "standard_name", LON_STANDARD_NAME);

    longName = combinedAttributes.getString(language, "long_name");
    if (longName == null
        || // catch nothing
        longName.equalsIgnoreCase("lon")
        || longName.equalsIgnoreCase("longitude")) { // catch alternate case
      longName = LON_LONGNAME;
      combinedAttributes.set(language, "long_name", longName);
    }

    units = LON_UNITS;
    combinedAttributes.set(language, "units", units);

    extractAndSetActualRange(language);
  }

  /**
   * This returns a string representation of this EDVLon.
   *
   * @return a string representation of this EDVLon.
   */
  @Override
  public String toString() {
    return "EDVLon/" + super.toString();
  }
}
