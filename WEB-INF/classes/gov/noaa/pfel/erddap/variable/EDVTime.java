/*
 * EDVTime Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;

/**
 * This class holds information about *the* main time variable, which is like EDVTimeStamp, but has
 * destinationName="time".
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVTime extends EDVTimeStamp {

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
