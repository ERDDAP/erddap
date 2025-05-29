/*
 * EDVLatGridAxis Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;

/**
 * This class holds information about the latitude grid axis variable.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVLatGridAxis extends EDVGridAxis {

  /**
   * The constructor.
   *
   * @param tParentDatasetID This is needed if dimensionValuesInMemory is false, so sourceValues
   *     sometimes need to be read from [cacheDirectory(tParentDatasetID)]/dimensionSourceValues.nc
   * @param tSourceName the name of the axis variable in the dataset source (usually with no
   *     spaces).
   * @param tSourceAttributes are the attributes for the variable in the source
   * @param tAddAttributes the attributes which will be added when data is extracted and which have
   *     precedence over sourceAttributes. Special case: value="null" causes that item to be removed
   *     from combinedAttributes. If this is null, an empty Attributes will be created.
   * @param tSourceValues has the values from the source. This can't be a StringArray. There must be
   *     at least one element.
   * @throws Throwable if trouble
   */
  public EDVLatGridAxis(
      String tParentDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      LocalizedAttributes tAddAttributes,
      PrimitiveArray tSourceValues)
      throws Throwable {

    super(
        tParentDatasetID, tSourceName, LAT_NAME, tSourceAttributes, tAddAttributes, tSourceValues);

    if (destinationDataType().equals("String"))
      throw new RuntimeException(
          "datasets.xml error: "
              + "The destination dataType for the latitude variable must be a numeric dataType.");

    // The attributes this gets/sets should not need to be localized (max/min
    // value for example). Just use the default language.
    int language = EDMessages.DEFAULT_LANGUAGE;
    longName = LAT_LONGNAME;
    units = LAT_UNITS;
    combinedAttributes.set(language, "_CoordinateAxisType", "Lat"); // unidata-related
    combinedAttributes.set(language, "axis", "Y");
    combinedAttributes.set(language, "ioos_category", LOCATION_CATEGORY);
    combinedAttributes.set(language, "long_name", longName);
    combinedAttributes.set(language, "standard_name", LAT_STANDARD_NAME);
    combinedAttributes.set(language, "units", units);

    // remember that gridAxes get min max from actual axis tSourceValues
    setActualRangeFromDestinationMinMax(language);
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVLatGridAxis/" + super.toString();
  }
}
