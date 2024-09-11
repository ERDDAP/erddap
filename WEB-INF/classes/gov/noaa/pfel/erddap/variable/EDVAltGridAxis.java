/*
 * EDVAltGridAxis Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;

/**
 * This class holds information about an altitude grid axis variable.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVAltGridAxis extends EDVGridAxis {

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
  public EDVAltGridAxis(
      String tParentDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      Attributes tAddAttributes,
      PrimitiveArray tSourceValues)
      throws Throwable {

    super(
        tParentDatasetID, tSourceName, ALT_NAME, tSourceAttributes, tAddAttributes, tSourceValues);

    if (destinationDataType().equals("String"))
      throw new RuntimeException(
          "datasets.xml error: "
              + "The destination dataType for the altitude variable must be a numeric dataType.");

    longName = ALT_LONGNAME;
    units = ALT_UNITS;
    combinedAttributes.set("_CoordinateAxisType", "Height"); // unidata
    combinedAttributes.set("_CoordinateZisPositive", "up"); // unidata
    combinedAttributes.set("axis", "Z");
    combinedAttributes.set("ioos_category", LOCATION_CATEGORY);
    combinedAttributes.set("long_name", longName);
    combinedAttributes.set("positive", "up"); // cf
    combinedAttributes.set("standard_name", ALT_STANDARD_NAME);
    EDVAlt.ensureUnitsAreM(combinedAttributes.getString("units"), "altitude", "up");
    combinedAttributes.set("units", units);

    if (destinationMin.compareTo(destinationMax) > 0) {
      PAOne d1 = destinationMin;
      destinationMin = destinationMax;
      destinationMax = d1;
    }
    setActualRangeFromDestinationMinMax();
    initializeAverageSpacingAndCoarseMinMax();
    // no need to deal with missingValue stuff, since gridAxis can't have mv's
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @param errorInMethod the start string for an error message
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVAltGridAxis/" + super.toString();
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
        "\ndatasets.xml/EDVAltGridAxis.ensureValid error for sourceName=" + sourceName + ":\n";
  }
}
