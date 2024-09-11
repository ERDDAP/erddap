/*
 * AxisDataAccessor Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

/**
 * This class provides access to the axis data corresponding to a axis data query to an EDDGrid.
 * This always deals with just 1 axis variable.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-10
 */
public class AxisDataAccessor {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
   * if you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean reallyVerbose = false;

  // things passed into the constructor
  protected EDDGrid eddGrid;
  protected String userDapQuery;

  // things the constructor generates
  protected EDVGridAxis[] rAxisVariables;
  protected IntArray constraints;
  protected Attributes globalAttributes;
  protected Attributes rAxisAttributes[];
  protected PrimitiveArray rAxisValues[];

  /**
   * This is the constructor.
   *
   * @param language the index of the selected language
   * @param tEDDGrid
   * @param tRequestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
   * @param tUserDapQuery the part after the '?', still percentEncoded, may be null.
   * @param tConstraints
   * @throws Throwable if trouble
   */
  public AxisDataAccessor(int language, EDDGrid tEDDGrid, String tRequestUrl, String tUserDapQuery)
      throws Throwable {

    eddGrid = tEDDGrid;
    userDapQuery = tUserDapQuery;
    if (reallyVerbose)
      String2.log(
          "\n    AxisDataAccessor constructor"
              + "\n      EDDGrid="
              + eddGrid.datasetID()
              + "\n      userDapQuery="
              + userDapQuery);

    // parse the query    -- always for just 1 axis variable
    StringArray destinationNames = new StringArray();
    constraints = new IntArray();
    eddGrid.parseAxisDapQuery(language, userDapQuery, destinationNames, constraints, false);
    if (reallyVerbose)
      String2.log(
          "      requestedAxisVariables="
              + destinationNames
              + "\n      constraints="
              + constraints);

    // make globalAttributes
    globalAttributes = new Attributes(eddGrid.combinedGlobalAttributes()); // a copy

    // fix up global attributes (always to a local COPY of global attributes)
    // remove acdd-style and google-style bounding box
    EDD.addToHistory(globalAttributes, eddGrid.publicSourceUrl());
    EDD.addToHistory(globalAttributes, EDStatic.baseUrl + tRequestUrl + "?" + tUserDapQuery);
    globalAttributes.remove("geospatial_lon_min");
    globalAttributes.remove("geospatial_lon_max");
    globalAttributes.remove("geospatial_lon_resolution");
    globalAttributes.remove("geospatial_lon_units");
    globalAttributes.remove("Westernmost_Easting");
    globalAttributes.remove("Easternmost_Easting");
    globalAttributes.remove("geospatial_lat_min");
    globalAttributes.remove("geospatial_lat_max");
    globalAttributes.remove("geospatial_lat_resolution");
    globalAttributes.remove("geospatial_lat_units");
    globalAttributes.remove("Southernmost_Northing");
    globalAttributes.remove("Northernmost_Northing");
    globalAttributes.remove("geospatial_vertical_min"); // unidata-related
    globalAttributes.remove("geospatial_vertical_max");
    globalAttributes.remove("geospatial_vertical_units");
    globalAttributes.remove("geospatial_vertical_positive");
    globalAttributes.remove("time_coverage_start"); // unidata-related
    globalAttributes.remove("time_coverage_end");

    // deal with the individual requested axisVariables
    int nRAV = destinationNames.size();
    rAxisVariables = new EDVGridAxis[nRAV];
    rAxisAttributes = new Attributes[nRAV];
    rAxisValues = new PrimitiveArray[nRAV];
    for (int av = 0; av < nRAV; av++) {
      rAxisVariables[av] =
          eddGrid.findAxisVariableByDestinationName(language, destinationNames.get(av));

      // make axisValues
      // start with source (since immediately available)
      // and subset always returns a new primitiveArray
      int av3 = av * 3;
      rAxisValues[av] =
          rAxisVariables[av]
              .sourceValues()
              .subset(constraints.get(av3), constraints.get(av3 + 1), constraints.get(av3 + 2));
      // convert source values to destination values
      rAxisValues[av] = rAxisVariables[av].toDestination(rAxisValues[av]);

      // make axisAttributes
      rAxisAttributes[av] = new Attributes(rAxisVariables[av].combinedAttributes()); // a copy

      // setActualRangeAndBoundingBox  (see comments in method javadocs above)
      // if no data, don't specify range
      // actual_range is type-specific
      double dMin = rAxisValues[av].getNiceDouble(0);
      double dMax = rAxisValues[av].getNiceDouble(rAxisValues[av].size() - 1);
      if (dMin > dMax) {
        double d = dMin;
        dMin = dMax;
        dMax = d;
      }
      PrimitiveArray minMax = PrimitiveArray.factory(rAxisValues[av].elementType(), 2, false);
      minMax.addDouble(dMin);
      minMax.addDouble(dMax);

      if (Double.isNaN(dMin)) rAxisAttributes[av].remove("actual_range");
      else rAxisAttributes[av].set("actual_range", minMax);

      // remove/set acdd-style and google-style bounding box
      float fMin = Math2.doubleToFloatNaN(dMin);
      float fMax = Math2.doubleToFloatNaN(dMax);
      int iMin = Math2.roundToInt(dMin);
      int iMax = Math2.roundToInt(dMax);
      if (rAxisVariables[av] instanceof EDVLonGridAxis) {
        globalAttributes.set("geospatial_lon_units", rAxisVariables[av].units());
        if (Double.isNaN(dMin)) {
        } else if (minMax instanceof FloatArray) {
          globalAttributes.set("geospatial_lon_min", fMin);
          globalAttributes.set("geospatial_lon_max", fMax);
          globalAttributes.set("Westernmost_Easting", fMin);
          globalAttributes.set("Easternmost_Easting", fMax);
        } else {
          globalAttributes.set("geospatial_lon_min", dMin);
          globalAttributes.set("geospatial_lon_max", dMax);
          globalAttributes.set("Westernmost_Easting", dMin);
          globalAttributes.set("Easternmost_Easting", dMax);
        }
      } else if (rAxisVariables[av] instanceof EDVLatGridAxis) {
        globalAttributes.set("geospatial_lat_units", rAxisVariables[av].units());
        if (Double.isNaN(dMin)) {
        } else if (minMax instanceof FloatArray) {
          globalAttributes.set("geospatial_lat_min", fMin);
          globalAttributes.set("geospatial_lat_max", fMax);
          globalAttributes.set("Southernmost_Northing", fMin);
          globalAttributes.set("Northernmost_Northing", fMax);
        } else {
          globalAttributes.set("geospatial_lat_min", dMin);
          globalAttributes.set("geospatial_lat_max", dMax);
          globalAttributes.set("Southernmost_Northing", dMin);
          globalAttributes.set("Northernmost_Northing", dMax);
        }
      } else if (rAxisVariables[av] instanceof EDVAltGridAxis) {
        globalAttributes.set("geospatial_vertical_units", rAxisVariables[av].units());
        globalAttributes.set("geospatial_vertical_positive", "up");
        if (Double.isNaN(dMin)) {
        } else if (minMax instanceof FloatArray) {
          globalAttributes.set("geospatial_vertical_min", fMin); // unidata-related
          globalAttributes.set("geospatial_vertical_max", fMax);
        } else if (minMax instanceof IntArray
            || minMax instanceof ShortArray
            || minMax instanceof ByteArray) {
          globalAttributes.set("geospatial_vertical_min", iMin); // unidata-related
          globalAttributes.set("geospatial_vertical_max", iMax);
        } else {
          globalAttributes.set("geospatial_vertical_min", dMin); // unidata-related
          globalAttributes.set("geospatial_vertical_max", dMax);
        }
      } else if (rAxisVariables[av] instanceof EDVDepthGridAxis) {
        globalAttributes.set("geospatial_vertical_units", rAxisVariables[av].units());
        globalAttributes.set("geospatial_vertical_positive", "down");
        if (Double.isNaN(dMin)) {
        } else if (minMax instanceof FloatArray) {
          globalAttributes.set("geospatial_vertical_min", fMin); // unidata-related
          globalAttributes.set("geospatial_vertical_max", fMax);
        } else if (minMax instanceof IntArray
            || minMax instanceof ShortArray
            || minMax instanceof ByteArray) {
          globalAttributes.set("geospatial_vertical_min", iMin); // unidata-related
          globalAttributes.set("geospatial_vertical_max", iMax);
        } else {
          globalAttributes.set("geospatial_vertical_min", dMin); // unidata-related
          globalAttributes.set("geospatial_vertical_max", dMax);
        }
      } else if (rAxisVariables[av] instanceof EDVTimeGridAxis) {
        String tp = rAxisVariables[av].combinedAttributes().getString(EDV.TIME_PRECISION);
        // "" unsets the attribute if dMin or dMax isNaN
        globalAttributes.set(
            "time_coverage_start", Calendar2.epochSecondsToLimitedIsoStringT(tp, dMin, ""));
        globalAttributes.set(
            "time_coverage_end", Calendar2.epochSecondsToLimitedIsoStringT(tp, dMax, ""));
      }
    }
  }

  /**
   * This returns the EDDGrid that is the source of this data.
   *
   * @return the EDDGrid that is the source of this data.
   */
  public EDDGrid eddGrid() {
    return eddGrid;
  }

  /**
   * This returns the userDapQuery used to make this.
   *
   * @return the userDapQuery used to make this, still percentEncoded, may be null.
   */
  public String userDapQuery() {
    return userDapQuery;
  }

  /**
   * This returns the constraints derived from the userDapQuery. This is the internal data
   * structure, so don't change it.
   *
   * @return the constraints derived from the userDapQuery.
   */
  public IntArray constraints() {
    return constraints;
  }

  /**
   * This returns the number of axisVariables requested in userDapQuery.
   *
   * @return the number of axisVariables.
   */
  public int nRequestedAxisVariables() {
    return rAxisVariables.length;
  }

  /**
   * This returns the axisVariables requested in userDapQuery.
   *
   * @param rav the number of the requested axisVariable
   * @return the axisVariables.
   */
  public EDVGridAxis axisVariables(int rav) {
    return rAxisVariables[rav];
  }

  /**
   * This returns the Attributes (source + add) for the requested axisVariables.
   *
   * @param rav the number of the requested axisVariable
   * @return the Attributes (source + add) for the requested axisVariables.
   */
  public Attributes axisAttributes(int rav) {
    return rAxisAttributes[rav];
  }

  /**
   * This returns the axis values (for the entire response) for the requested axisVariable. Axis
   * values never have missing values. The PrimitiveArray in rAxisValues[] is always a new PA (not
   * the original axisVariable's values).
   *
   * @param rav the number of the requested axisVariable
   * @return the axis values (for the entire response) for the requested axisVariable.
   */
  public PrimitiveArray axisValues(int rav) {
    return rAxisValues[rav];
  }

  /**
   * This returns the global attributes (source + add).
   *
   * @return the global attributes (source + add).
   */
  public Attributes globalAttributes() {
    return globalAttributes;
  }
}
