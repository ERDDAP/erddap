/*
 * EDVTimeGridAxis Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;

/**
 * This class holds information about *the* time grid axis variable, which is like other
 * EDVTimeStampGridAxis variables, but has destinationName="time".
 *
 * <p>[STRING TIMES NOT FINISHED: The handling of String times in incomplete and probably not a good
 * approach. Probably better is: really encapsulate the strings, so that any users of this class
 * just see/deal with numeric values (epochSoconds). There are just too many places where it is
 * assumed that all axes are numeric.]
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVTimeGridAxis extends EDVTimeStampGridAxis {

  /** The constructor. */
  public EDVTimeGridAxis(
      String tParentDatasetID,
      String tSourceName,
      Attributes tSourceAttributes,
      Attributes tAddAttributes,
      PrimitiveArray tSourceValues)
      throws Throwable {

    super(
        tParentDatasetID,
        tSourceName,
        EDV.TIME_NAME,
        tSourceAttributes,
        tAddAttributes,
        tSourceValues);
  }

  /**
   * This returns a string representation of this EDV.
   *
   * @return a string representation of this EDV.
   */
  @Override
  public String toString() {
    return "EDVTimeGridAxis/" + super.toString();
  }
}
