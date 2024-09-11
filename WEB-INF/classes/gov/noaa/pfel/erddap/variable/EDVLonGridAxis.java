/*
 * EDVLonGridAxis Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;

/**
 * This class holds information about the longitude grid axis variable.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVLonGridAxis extends EDVGridAxis {

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
   *     from combinedAttributes. If this is null, an empty addAttributes will be created.
   * @param tSourceValues has the values from the source. This can't be a StringArray. There must be
   *     at least one element.
   * @throws Throwable if trouble
   */
  public EDVLonGridAxis(
      String tParentDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      Attributes tAddAttributes,
      PrimitiveArray tSourceValues)
      throws Throwable {

    super(
        tParentDatasetID, tSourceName, LON_NAME, tSourceAttributes, tAddAttributes, tSourceValues);

    if (destinationDataType().equals("String"))
      throw new RuntimeException(
          "datasets.xml error: "
              + "The destination dataType for the longitude variable must be a numeric dataType.");

    longName = LON_LONGNAME;
    units = LON_UNITS;
    combinedAttributes.set("_CoordinateAxisType", "Lon"); // unidata-related
    combinedAttributes.set("axis", "X");
    combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
    combinedAttributes.set("long_name", longName);
    combinedAttributes.set("standard_name", LON_STANDARD_NAME);
    combinedAttributes.set("units", units);

    // remember that gridAxes get min max from actual axis tSourceValues
    setActualRangeFromDestinationMinMax();
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVLonGridAxis/" + super.toString();
  }
}
