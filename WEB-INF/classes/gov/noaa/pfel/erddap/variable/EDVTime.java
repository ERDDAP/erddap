/*
 * EDVTime Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;

/**
 * This class holds information about *the* main time variable, which is like EDVTimeStamp, but has
 * destinationName="time".
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVTime extends EDVTimeStamp {

  /**
   * The constructor -- like EDV, but the destinationName, and units are standardized.
   *
   * @param tSourceMin is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_min, or data_min metadata.
   * @param tSourceMax is pre-scale_factor and add_offset. This takes precedence over actual_range,
   *     actual_max, or data_max metadata.
   */
  public EDVTime(
      String tDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      LocalizedAttributes tAddAttributes,
      String tSourceDataType)
      throws Throwable {
    this(
        tDatasetID,
        tSourceName,
        tSourceAttributes,
        tAddAttributes.toAttributes(EDMessages.DEFAULT_LANGUAGE),
        tSourceDataType);
  }

  /** The constructor. This constructor gets source / sets destination actual_range. */
  public EDVTime(
      String tDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      Attributes tAddAttributes,
      String tSourceDataType)
      throws Throwable {

    super(
        tDatasetID, tSourceName, EDV.TIME_NAME, tSourceAttributes, tAddAttributes, tSourceDataType);
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVTime/" + super.toString();
  }
}
