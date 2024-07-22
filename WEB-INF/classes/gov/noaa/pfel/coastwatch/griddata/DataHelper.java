/*
 * DataHelper Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.*;

/**
 * This class has some static convenience methods related to the other Data classes.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-12-07
 */
public class DataHelper {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  // These Strings are NOT final so they can be changed at runtime.

  /**
   * The string for no units. There doesn't seem to be a udUnits standard. But LAS uses "unitless".
   */
  public static String UNITLESS = "unitless";

  /** The creatorEmail for CoastWatch */
  public static String CW_CREATOR_EMAIL = "erd.data@noaa.gov";

  /** The creatorName for CoastWatch */
  public static String CW_CREATOR_NAME = "NOAA CoastWatch, West Coast Node";

  /** The creatorUrl for CoastWatch */
  public static String CW_CREATOR_URL = "https://coastwatch.pfeg.noaa.gov";

  /** The project for CoastWatch */
  public static String CW_PROJECT = "CoastWatch (https://coastwatch.noaa.gov/)";

  /** The creatorEmail for ERD */
  public static String ERD_CREATOR_EMAIL = "erd.data@noaa.gov";

  /** The creatorName for ERD */
  public static String ERD_CREATOR_NAME = "NOAA NMFS SWFSC ERD";

  /** The creatorUrl for ERD */
  public static String ERD_CREATOR_URL = "https://www.pfeg.noaa.gov";

  /** The project for ERD */
  public static String ERD_PROJECT = "NOAA NMFS SWFSC ERD (https://www.pfeg.noaa.gov/)";

  /**
   * The standard variable names of the first 5 columns in the TableDataSet.makeSubset and
   * PointDataSet.makeSubset results table: {"LON", "LAT", "DEPTH", "TIME", "ID"}. Dapper and DChart
   * like these exact names.
   */
  public static final String[] TABLE_VARIABLE_NAMES = {"LON", "LAT", "DEPTH", "TIME", "ID"};

  /**
   * The standard long names of the first 5 columns in the TableDataSet.makeSubset and
   * PointDataSet.makeSubset results table: {"Longitude", "Latitude", "Depth", "Time",
   * "Identifier"}. Dapper and DChart like these exact names.
   */
  public static final String[] TABLE_LONG_NAMES = {
    "Longitude", "Latitude", "Depth", "Time", "Identifier"
  };

  /**
   * The standard UD units for the first 5 columns in the TableDataSet.makeSubset and
   * PointDataSet.makeSubset results table, e.g., "degrees_east". Dapper and DChart like these exact
   * names.
   */
  public static final String[] TABLE_UNITS = {
    "degrees_east", "degrees_north", "m", Calendar2.SECONDS_SINCE_1970, UNITLESS
  };

  /**
   * The returns a new, revised, history attribute. This avoids adding a second entries for today
   * (which would occur if data is generated, manipulated, and saved).
   *
   * @param oldHistory If null, this will be converted to "unknown"
   * @return a new, revised, history attribute.
   */
  public static String addBrowserToHistory(String oldHistory) {
    String dateTime = "\n" + Calendar2.getCurrentISODateTimeStringZulu() + "Z";
    String cwID = " NOAA CoastWatch (West Coast Node) and NOAA SWFSC ERD";
    if (oldHistory == null) oldHistory = "unknown";

    // avoid two cwID entries for today if table is manipulated then saved
    int po = oldHistory.indexOf(dateTime.substring(0, 11)); // e.g., \n2006-04-07
    if (po > 0 && oldHistory.endsWith(cwID)) return oldHistory;
    return oldHistory + dateTime + cwID;
  }

  /**
   * The value is sometimes used as an alternative to NaN in DoubleArray and FloatArray. (for
   * example in hdfFiles and netCDF files). It must be same type as the data; saveAsNetCDF saves
   * data as floats, saveAsHdf saves data as doubles. Some file readers are not very clever -- so it
   * is best if the float and double versions of this are exactly equal. So in 12/2006, we switched
   * from -1e32 to -9999999 (7 9's). (8 9's can't be exactly represented in a float.)
   */
  public static final int FAKE_MISSING_VALUE = -9999999;

  /**
   * This gets a regularly spaced array of n values, starting at min, and separated by spacing.
   *
   * @param n
   * @param min
   * @param spacing
   * @return a regularly spaced array
   */
  public static double[] getRegularArray(int n, double min, double spacing) {
    double da[] = new double[n];
    for (int i = 0; i < n; i++) da[i] = min + i * spacing;
    return da;
  }

  /**
   * This gets a copy (subset) of a double array (even if stride = 1).
   *
   * @param dar the original data array
   * @param start the first element to be copied
   * @param end the last element (inclusive) to be copied (if stride hits it)
   * @param stride says: just take every stride'th value from dar, starting with dar[start]
   */
  public static double[] copy(double dar[], int start, int end, int stride) {
    int n;
    if (stride == 1) {
      n = end - start + 1;
      double tdar[] = new double[n];
      System.arraycopy(dar, start, tdar, 0, n);
      return tdar;
    }

    n = Math2.hiDiv(end - start + 1, stride);
    double tdar[] = new double[n];
    int po = 0;
    for (int i = start; i <= end; i += stride) tdar[po++] = dar[i];
    return tdar;
  }

  /**
   * Given an ascending sorted double[], this finds the index of the closest index to 'start'.
   *
   * <p>Note that by finding the closest index (not the &gt;= index), this (combinded with
   * findEndIndex) always returns at least one index and may include relevant data slightly outside
   * the desired range. While technically NOT what is being asked for, this is deemed the desirable
   * behavior (e.g., when searching for the single index nearest to a single point).
   *
   * @param dar the double array
   * @param start If start is NaN, this returns 0.
   * @return the index of the last value less than or equal to start. If start < firstValue, this
   *     returns 0 (there may be no appropriate values, but can't tell). If start > lastValue, this
   *     returns -1 (no appropriate values).
   */
  public static int binaryFindStartIndex(double dar[], double start) {
    // A lot of thought went into the details of this method.
    // Think twice before modifying it.
    int nDar = dar.length;
    if (nDar == 0) return -1;
    if (Double.isNaN(start)) return 0;

    // no valid values?
    if (start > dar[nDar - 1]) {
      double spacing = nDar == 1 ? 1 : dar[1] - dar[0];
      if (start > dar[nDar - 1] + spacing / 2) return -1;
    }
    return Math2.binaryFindClosest(dar, start);
  }

  /**
   * Given an ascending sorted double[], this finds the index of the closest index to 'end'.
   *
   * <p>Note that by finding the closest index (not the &gt;= index), this (combinded with
   * findStartIndex) always returns at least one index and may include relevant data slightly
   * outside the desired range. While technically NOT what is being asked for, this is deemed the
   * desirable behavior (e.g., when searching for the single index nearest to a single point).
   *
   * @param dar the double array
   * @param end If end is NaN, this returns dar.length - 1.
   * @return the index of the last value greater than or equal to end. If end < firstValue, this
   *     returns -1 (no appropriate values). If end > lastValue, this returns dar.length-1 (there
   *     may be no appropriate values, but can't tell).
   */
  public static int binaryFindEndIndex(double dar[], double end) {
    // A lot of thought went into the details of this method.
    // Think twice before modifying it.
    int nDar = dar.length;
    if (nDar == 0) return -1;
    if (Double.isNaN(end)) return nDar - 1;

    // no valid values?
    if (end < dar[0]) {
      double spacing = nDar == 1 ? 1 : dar[1] - dar[0];
      if (end < dar[0] - spacing / 2) return -1;
    }
    return Math2.binaryFindClosest(dar, end);
  }

  /**
   * Given an ascending sorted double[], this finds the index of the closest value.
   *
   * @param dar the double array
   * @param d If d is NaN, this returns -1.
   * @return the index of the closest value, 0 .. dar.length-1. If d < dar[0]-spacing/2 or d >
   *     dar[dar.length-1]+spacing/2, this returns -1. If nDar=1, spacing is assumed to be 1. If
   *     nDar = 0, this returns -1;
   */
  public static int binaryFindClosestIndex(double dar[], double d) {
    int nDar = dar.length;
    if (Double.isNaN(d) || nDar == 0) return -1;
    if (d < dar[0] || d > dar[nDar - 1]) {
      double spacing = nDar == 1 ? 1 : dar[1] - dar[0];
      if (d < dar[0] - spacing / 2 || d > dar[nDar - 1] + spacing / 2) return -1;
    }
    return Math2.binaryFindClosest(dar, d);
  }

  /**
   * Adjust nPointsNeeded if axis range has changed. E.g., If axis range available is smaller, you
   * don't need so many points.
   *
   * @param nPointsNeeded
   * @param oldAxisRange max - min
   * @param newAxisRange max - min
   * @return the modified nPointsNeeded
   */
  public static int adjustNPointsNeeded(
      int nPointsNeeded, double oldAxisRange, double newAxisRange) {
    if (nPointsNeeded == Integer.MAX_VALUE) return nPointsNeeded;
    if (Math2.almost0(oldAxisRange)) return 1;
    if (Math2.almostEqual(5, oldAxisRange, newAxisRange)) return nPointsNeeded;
    double dn = nPointsNeeded * newAxisRange / oldAxisRange;
    int in = Math2.roundToInt(dn);
    if (Math2.almostEqual(5, dn, in)) return in;
    return in + 1;
  }

  /**
   * Given a startIndex, endIndex and nPointsNeeded, this determines the maximum stride you can use
   * and still get at least nPoints.
   *
   * @param startIndex
   * @param endIndex
   * @param nPointsNeeded If nPointsNeeded is really big, this still returns 1.
   * @return the optimal stride
   */
  // public static int findStride(int startIndex, int endIndex, int nPointsNeeded) {
  //    //why -1?  to catch the first and last point,
  //    //    consider 0,900,301  ->  900/300 = 3
  //    return Math.max(1, (endIndex - startIndex) / (nPointsNeeded - 1));
  // }

  /**
   * This calculates the lowest stride value to get nPointsNeeded.
   *
   * @param nLons the number of lon points you have, lastLonIndex - firstLonIndex + 1
   * @param nPointsNeeded the number of lon points you need
   * @return the lowest stride value to get nPointsNeeded
   */
  public static int findStride(int nLons, int nPointsNeeded) {
    nLons = Math.max(2, nLons);
    if (nPointsNeeded <= 1) return nLons;

    return Math.max(1, (nLons - 1) / Math.max(1, nPointsNeeded - 1));
  }

  /**
   * Given a actualLonSpacing, desiredMinLon, desiredMaxLon, and nPointsNeeded, this determines the
   * maximum stride you can use and still get at least nPoints. The parameters use "lon" so easier
   * to think about, but it works for "lat" too.
   *
   * @param actualLonSpacing
   * @param desiredMinLon One of the values from the lon array. If NaN, stride is 1.
   * @param desiredMaxLon One of the values from the lon array. If NaN, stride is 1.
   * @param nPointsNeeded If nPointsNeeded is really big, this still returns 1.
   * @return the optimal stride
   */
  public static int findStride(
      double actualLonSpacing, double desiredMinLon, double desiredMaxLon, int nPointsNeeded) {

    if (Double.isNaN(desiredMinLon) || Double.isNaN(desiredMaxLon)) return 1;

    // roundToInt is useful because desired and spacing may be bruised
    // imagine                              ((10 - 5)             / .25
    int nLons = Math2.roundToInt((desiredMaxLon - desiredMinLon) / actualLonSpacing) + 1;
    return findStride(nLons, nPointsNeeded);
  }

  /**
   * Given nHave and stride, this returns the actual number of points that will be found.
   *
   * @param nHave the size of the array (or sub array, e.g., stop-start+1)
   * @param stride (must be >= 1)
   * @return the actual number of points that will be found.
   */
  public static int strideWillFind(int nHave, int stride) {
    return 1 + (nHave - 1) / stride;
  }

  /**
   * This multiplies the values in 'doubleArray' by the 'scale' and adds 'offset'. If scale = 1 and
   * offset = 0, nothing is done.
   *
   * @param doubleArray
   * @param scale
   * @param offset
   * @return the same pointer to doubleArray (for convenience)
   */
  public static double[] scale(double doubleArray[], double scale, double offset) {
    if (scale == 1 && offset == 0) return doubleArray;
    int n = doubleArray.length;
    for (int i = 0; i < n; i++) doubleArray[i] = doubleArray[i] * scale + offset;
    return doubleArray;
  }

  /**
   * This adds '^' in appropriate places to a units string and replaces '_' with ' '.
   *
   * @param udunits
   * @return units string with '^' in appropriate places
   */
  public static String makeUdUnitsReadable(String udunits) {
    StringBuilder sb = new StringBuilder(udunits);

    // replace '_' with ' '
    String2.replaceAll(sb, "_", " ");

    // replace <letter>-<digit> with <letter>^-<digit>
    int po = sb.indexOf("-");
    while (po >= 0) {
      if (po > 0
          && String2.isLetter(sb.charAt(po - 1))
          && // preceded by letter
          po < sb.length() - 1
          && String2.isDigit(sb.charAt(po + 1))) // followed by digit
      sb.insert(po++, '^');
      po = sb.indexOf("-", po + 1);
    }

    // replace <letter><digit> with <letter>^<digit>
    po = 0;
    // find next digit   (sb.length() must be checked dynamically)
    while (po < sb.length() && !String2.isDigit(sb.charAt(po))) po++;
    while (po < sb.length()) {
      if (po > 0 && String2.isLetter(sb.charAt(po - 1))) // preceded by letter
      sb.insert(po++, '^');
      po++;
      // find next digit
      while (po < sb.length() && !String2.isDigit(sb.charAt(po))) po++;
    }

    return sb.toString();
  }

  /**
   * This returns true if min/MaxX specify a range that needs to be pm180. A given min/MaxX may need
   * to be PM180, or 0,360, or neither (but not both).
   *
   * @param minX the desired min longitude value
   * @param maxX the desired max longitude value
   * @return true if min/MaxX specify a range that needs to be pm180.
   */
  public static boolean lonNeedsToBePM180(double minX, double maxX) {
    return minX < 0;
  }

  /**
   * This returns true if min/MaxX specify a range that needs to be 0 .. 360. A given min/MaxX may
   * need to be PM180, or 0,360, or neither (but not both).
   *
   * @param minX the desired min longitude value
   * @param maxX the desired max longitude value
   * @return true if min/MaxX specify a range that needs to be 0 .. 360.
   */
  public static boolean lonNeedsToBe0360(double minX, double maxX) {
    return maxX > 180;
  }

  /**
   * Given a start day and end day (for a composite) this calculates the centered time to the
   * nearest second. Don't give an hday time to this routine!
   *
   * @param isoStartDate e.g., 2006-02-14
   * @param isoEndDate the Dave-style last date in the composite. Inclusive! e.g., 2006-02-16 (for a
   *     3 day composite)
   * @return the iso centered time (to nearest second, with ' ' as connector).
   * @throws Exception if trouble (dates are null, invalid, or hh:mm:ss!=0).
   */
  public static String centerOfStartDateAndInclusiveEndDate(
      String isoStartDate, String isoEndDate) {

    double startSeconds =
        Calendar2.isoStringToEpochSeconds(isoStartDate); // throws exception if trouble
    double endSeconds =
        Calendar2.isoStringToEpochSeconds(isoEndDate); // throws exception if trouble
    if (startSeconds % Calendar2.SECONDS_PER_DAY != 0)
      Test.error(
          String2.ERROR
              + " in DataHelper.centerOfStartDateAndInclusiveEndDate:\n"
              + "isoStartDate="
              + isoStartDate
              + " has non-zero hh:mm:ss info!");
    if (endSeconds % Calendar2.SECONDS_PER_DAY != 0)
      Test.error(
          String2.ERROR
              + " in DataHelper.centerOfStartDateAndInclusiveEndDate:\n"
              + "isoEndDate="
              + isoEndDate
              + " has non-zero hh:mm:ss info!");
    double centerSeconds = (startSeconds + (endSeconds + Calendar2.SECONDS_PER_DAY)) / 2;
    String s = Calendar2.epochSecondsToIsoStringT(centerSeconds);
    s = String2.replaceAll(s, 'T', ' ');
    return s;
  }

  /**
   * This ensures that the values in the array are evenly spaced (within Math2.almostEqual9).
   *
   * @param lon a double array usually of of lon or lat values
   * @param msg e.g., "The longitude values aren't evenly spaced (as required by ESRI's .asc
   *     format):\n"
   * @throws Exception if the values in the array are not evenly spaced (within Math2.almostEqual7).
   */
  public static void ensureEvenlySpaced(double lon[], String msg) {
    if (lon.length <= 2) return;
    double lonSpacing = lon[1] - lon[0];
    for (int i = 2; i < lon.length; i++)
      if (!Math2.almostEqual(7, lon[i] - lon[i - 1], lonSpacing))
        Test.error(
            msg
                + "array["
                + (i - 1)
                + "]="
                + lon[i - 1]
                + ", array["
                + i
                + "]="
                + lon[i]
                + ", expected spacing="
                + lonSpacing);
  }
}
