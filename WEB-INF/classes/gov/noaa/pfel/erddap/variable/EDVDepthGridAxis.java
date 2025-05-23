/*
 * EDVDepthGridAxis Copyright 2012, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;

/**
 * This class holds information about a depth grid axis variable.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVDepthGridAxis extends EDVGridAxis {

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
  public EDVDepthGridAxis(
      String tParentDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      LocalizedAttributes tAddAttributes,
      PrimitiveArray tSourceValues)
      throws Throwable {

    super(
        tParentDatasetID,
        tSourceName,
        DEPTH_NAME,
        tSourceAttributes,
        tAddAttributes,
        tSourceValues);

    if (destinationDataType().equals("String"))
      throw new RuntimeException(
          "datasets.xml error: "
              + "The destination dataType for the depth variable must be a numeric dataType.");

    // The attributes this gets/sets should not need to be localized (max/min
    // value for example). Just use the default language.
    int language = EDMessages.DEFAULT_LANGUAGE;
    longName = DEPTH_LONGNAME;
    units = DEPTH_UNITS;
    combinedAttributes.set(language, "_CoordinateAxisType", "Height"); // unidata
    combinedAttributes.set(language, "_CoordinateZisPositive", "down"); // unidata
    combinedAttributes.set(language, "axis", "Z");
    combinedAttributes.set(language, "ioos_category", LOCATION_CATEGORY);
    combinedAttributes.set(language, "long_name", longName);
    combinedAttributes.set(language, "positive", "down"); // cf
    combinedAttributes.set(language, "standard_name", DEPTH_STANDARD_NAME);
    EDVAlt.ensureUnitsAreM(combinedAttributes.getString(language, "units"), "depth", "down");
    combinedAttributes.set(language, "units", units);

    // remember that gridAxes get min max from actual axis tSourceValues
    if (destinationMin.compareTo(destinationMax) > 0) {
      PAOne d1 = destinationMin;
      destinationMin = destinationMax;
      destinationMax = d1;
    }
    setActualRangeFromDestinationMinMax(language);
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
    return "EDVDepthGridAxis/" + super.toString();
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
        "\ndatasets.xml/EDVDepthGridAxis.ensureValid error for sourceName=" + sourceName + ":\n";
  }
}
