/*
 * Opendap.java Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.TimePeriods;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.GregorianCalendar;

/** This class holds information about an OPeNDAP grid data set for one time period. */
public class Opendap {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /** The following information about this OPeNDAP data set is set by the constructor. */
  public String url;

  public boolean acceptDeflate =
      true; // Dave got faster than possible response; ergo, compression is working(?)
  public String flagDirectory; // may be null

  /** true if the data has longitude values in -180 to 180; else false (set by the constructor). */
  public boolean lonIsPM180;

  /* *
  * INACTIVE
  //This is inactive because the information is stored as metadata in each file.
  //But thredds aggregates files and only shows metadata from the penultimate file.
  //So this information is no longer available.
  * This stores the number of satellite passes included in a composite for a given time.
  * Thus, a value of 0 means there is no data available for that time period.
  * Has 1 element for each element in gridDimensionData[gridTimeDimension].
  * Is null, if not in use.
  * Used by makeTimeOptions (if not null).
  */
  // public double[] numberOfObservations;

  /** These are set by getGridInfo. */
  public String gridName;

  public String[] gridDimensionNames;
  public double[][] gridDimensionData; // has time, altitude, lon, lat, dimension values
  public boolean gridDimensionAscending[];
  public int gridTimeDimension = -1;
  public int gridDepthDimension = -1;
  public int gridLatDimension = -1;
  public int gridLonDimension = -1;
  public int gridNLonValues = -1;
  public int gridNLatValues = -1;
  public double gridLatIncrement = -1;
  public double gridLonIncrement = -1;
  public String gridMissingValue;
  public double gridTimeBaseSeconds = Double.NaN; // e.g., "minutes since 1970-01-01" returns 0
  public double gridTimeFactorToGetSeconds = Double.NaN; // e.g., "minutes since ..." returns 60

  /**
   * If an exception is thrown that contains this String, you should recreate this OPeNDAP class
   * because the dataset has changed since the class was created.
   */
  public static final String WAIT_THEN_TRY_AGAIN =
      "That data set is being updated right now.  Wait two minutes, then try again.\n"
          + "If that fails, wait another two minutes and try again.";

  /**
   * timeLongName is used to determine if the times in the file are already centered (anything other
   * than "End Time", e.g., "Centered Time") or aren't yet centered ("End Time").
   */
  public String timeLongName;

  /**
   * This is an array of nicely formatted, valid, time strings for the user to choose from. This is
   * set by makeTimeOptions.
   */
  public String[] timeOptions;

  /**
   * This can be used to convert a timeOption index back to the related index of
   * gridDimensionData[gridTimeDimension]. This is set by makeTimeOptions.
   */
  public int[] timeOptionsIndex;

  /** Set by makeGrid */
  public double minLonInNewFile,
      maxLonInNewFile,
      minLatInNewFile,
      maxLatInNewFile,
      lonIncrementInNewFile,
      latIncrementInNewFile;

  /**
   * This constructs an OPeNDAP object related to some data set and some time period. No information
   * is read here.
   *
   * @param url the url of the OPeNDAP data set, e.g.,
   *     "http://las.pfeg.noaa.gov/cgi-bin/nph-dods/data/oceanwatch/nrt/gac/AG1day.nc".
   * @param acceptDeflate
   * @param flagDirectory If flagDirectory is not null and this dataset needs to be reindexed
   *     (because the time values have changed), a file will be put in flagDirectory to signal the
   *     need for a new index.
   * @throws Exception if trouble
   */
  public Opendap(String url, boolean acceptDeflate, String flagDirectory) throws Exception {
    this.url = url;
    this.acceptDeflate = acceptDeflate;

    long time = System.currentTimeMillis();
    if (verbose) String2.log("\nOpendap constructor for " + url);

    if (verbose)
      String2.log(
          "  Opendap constructor done.  TIME=" + (System.currentTimeMillis() - time) + "ms");
  }

  /**
   * Given a das, this gets globalAttributeTable.
   *
   * @param das from dConnect.getDAS()
   * @throws Exception if trouble (e.g., das attribute table with "GLOBAL" in name not found)
   */
  public AttributeTable getGlobalAttributeTable(DAS das) throws Exception {

    // find the GLOBAL attributes
    // this assumes that GLOBAL is in the name (I've see GLOBAL and NC_GLOBAL)
    Enumeration names = das.getNames();
    while (names.hasMoreElements()) {
      String s = (String) names.nextElement();
      if (s.indexOf("GLOBAL") >= 0) {
        return das.getAttributeTable(s);
      }
    }
    Test.error(
        String2.ERROR
            + " in Opendap.getGlobalAttributeTable:\n"
            + "'GLOBAL' attribute table not found for "
            + url);
    return null;
  }

  /**
   * Get the length of an array from the DDS info.
   *
   * @param dds from dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT)
   * @param dimensionName e.g. "lat". This searches top-level variables and variables within Grids.
   * @return int the length of the array (or -1 if not found)
   */
  public int getArrayLength(DDS dds, String dimensionName) {
    try {
      // search top-level variables
      // dds is read-only so no need to use sychronized(dds)
      int len;
      Enumeration e = dds.getVariables();
      while (e.hasMoreElements()) {
        BaseType bt = (BaseType) e.nextElement();
        len = getDArrayLength(bt, dimensionName);
        if (len > 0) return len;
        if (bt instanceof DGrid dg) {
          Enumeration e2 = dg.getVariables();
          while (e2.hasMoreElements()) {
            len = getDArrayLength((BaseType) e2.nextElement(), dimensionName);
            if (len > 0) {
              if (verbose) String2.log("Opendap.getArrayLength(" + dimensionName + ") = " + len);
              return len;
            }
          }
        }
      }
    } catch (Exception e) {
    }
    if (verbose) String2.log("Opendap.getArrayLength: '" + dimensionName + "' not found.");
    return -1;
  }

  /**
   * Get the name of the grids from the dds. A sample dds:
   *
   * <pre>
   * Dataset {
   * Float32 altitude[altitude = 1];
   * Float32 lat[lat = 2321];
   * Float32 lon[lon = 4001];
   * Float64 time[time = 171];
   * Grid {
   * ARRAY:
   * Float32 MOk490[time = 171][altitude = 1][lat = 2321][lon = 4001];
   * MAPS:
   * Float64 time[time = 171];
   * Float32 altitude[altitude = 1];
   * Float32 lat[lat = 2321];
   * Float32 lon[lon = 4001];
   * } MOk490;
   * } dodsC/satellite/MO/k490/hday;
   * </pre>
   *
   * @param dds from getDds(OpendapHelper.DEFAULT_TIMEOUT)
   * @return a StringArray with the names of the Grid variables (size 0 if no Grids).
   */
  public StringArray getGridNames(DDS dds) {
    String ddsString = OpendapHelper.getDdsString(dds);
    StringArray results = new StringArray();
    int po = ddsString.indexOf("Grid {");
    while (po > 0) {
      po = ddsString.indexOf('}', po + 6); // if not found, ddsString is ill-formed
      int semiPo = ddsString.indexOf(';', po + 2);
      results.add(ddsString.substring(po + 2, semiPo));

      // find the next grid
      po = ddsString.indexOf("Grid {", semiPo + 1);
    }
    return results;
  }

  /**
   * Get information about a grid. This sets gridDimensionNames, gridDimensionValues,
   * gridTimeDimension, gridDepthDimension, gridLatDimension, gridLonDimension, gridNLatValues,
   * gridNLonValues, gridLatIncrement, gridLonIncrement gridMissingValue.
   *
   * @param das from getDas(OpendapHelper.DEFAULT_TIMEOUT)
   * @param dds from getDds(OpendapHelper.DEFAULT_TIMEOUT)
   * @param gridName e.g., "ssta"
   * @param defaultMissingValue the missingValue string to be used if the attribute "missing_value"
   *     isn't found
   * @throws Exception if trouble (e.g., gridName not found)
   */
  public void getGridInfo(DAS das, DDS dds, String gridName, String defaultMissingValue)
      throws Exception {
    long time = System.currentTimeMillis();
    String errorInMethod = String2.ERROR + " in Opendap.getGridInfo(" + gridName + "):\n  ";

    if (verbose) String2.log("Opendap.getGridInfo for " + gridName);

    // clear variables (in case trouble)
    DConnect dConnect = new DConnect(url, acceptDeflate, 1, 1);
    this.gridName = gridName;
    gridDimensionNames = null;
    gridDimensionData = null;
    gridDimensionAscending = null;
    gridTimeDimension = -1;
    gridDepthDimension = -1;
    gridLatDimension = -1;
    gridLonDimension = -1;
    gridNLonValues = -1;
    gridNLatValues = -1;
    gridLatIncrement = -1;
    gridLonIncrement = -1;
    gridMissingValue = null;
    gridTimeBaseSeconds = Double.NaN;
    gridTimeFactorToGetSeconds = Double.NaN;

    // get the grid baseType
    BaseType bt = dds.getVariable(gridName); // throws exception if not found
    DArray da =
        (DArray) ((DGrid) bt).getVariables().nextElement(); // first element is always main array
    // if (verbose) String2.log("  da.getName()=" + da.getName()); //always(?) same as gridName

    // gridMissingValue:  get from _FillValue  (it is preferred over missing_value)
    gridMissingValue = OpendapHelper.getAttributeValue(das, gridName, "_FillValue");
    if (verbose) String2.log("  gridMissing value from _FillValue=" + gridMissingValue);

    // gridMissingValue:  get from missingValue is second best
    if (gridMissingValue == null) {
      gridMissingValue = OpendapHelper.getAttributeValue(das, gridName, "missing_value");
      if (verbose) String2.log("  gridMissing value from missing_value=" + gridMissingValue);
    }

    // gridMissingValue:  use default
    if (gridMissingValue == null) {
      gridMissingValue = defaultMissingValue;
      if (verbose) String2.log("  gridMissingValue from default: " + gridMissingValue + ".");
    }

    // investigate the dimensions
    int numDimensions = da.numDimensions();
    gridDimensionNames = new String[numDimensions];
    gridDimensionData = new double[numDimensions][];
    gridDimensionAscending = new boolean[numDimensions];
    int po = 0;
    Enumeration e2 = da.getDimensions();
    while (e2.hasMoreElements()) {
      long time1 = System.currentTimeMillis();
      DArrayDimension dad = (DArrayDimension) e2.nextElement();
      gridDimensionNames[po] = dad.getName();

      // get dimension info
      // I used to try to deduce the values from "point_spacing"="even" and "actual_range"?
      // This is only small time saver.
      //  Axis dimensions can be deduced, but are small and load quickly anyway.
      //  Time dimension can be large, but can't be deduced.
      // And it isn't safe to rely on metadata.
      // So always just download the values.
      gridDimensionData[po] = OpendapHelper.getDoubleArray(dConnect, "?" + dad.getName());

      // gather other information
      double range =
          Math.abs(
              gridDimensionData[po][gridDimensionData[po].length - 1] - gridDimensionData[po][0]);
      gridDimensionAscending[po] = range > 0;
      if (dad.getName().equals("time")
          || dad.getName().equals("time_series")) { // lynn's files use this
        gridTimeDimension = po;

        // interpret time_series units (e.g., "days since 1985-01-01" or "days since 1985-1-1")
        // it must be: <units> since <isoDate>   or exception thrown
        // FUTURE: need to catch time zone information
        String tsUnits = OpendapHelper.getAttributeValue(das, dad.getName(), "units");
        tsUnits = String2.replaceAll(tsUnits, "\"", "");
        double timeBaseAndFactor[] =
            Calendar2.getTimeBaseAndFactor(tsUnits); // throws exception if trouble
        gridTimeBaseSeconds = timeBaseAndFactor[0];
        gridTimeFactorToGetSeconds = timeBaseAndFactor[1];

        // timeLongName is used to determine if the times in the file are already
        // centered ("Centered Time" or anything other than "End Time")
        // or aren't yet centered ("End Time").
        timeLongName = OpendapHelper.getAttributeValue(das, dad.getName(), "long_name");

      } else if (dad.getName().equals("depth") || dad.getName().equals("altitude")) {
        gridDepthDimension = po;
      } else if (dad.getName().equals("lat") || dad.getName().equals("latitude")) {
        gridLatDimension = po;
        gridNLatValues = gridDimensionData[po].length;
        gridLatIncrement = range / (gridNLatValues - 1);
      } else if (dad.getName().equals("lon") || dad.getName().equals("longitude")) {
        gridLonDimension = po;
        gridNLonValues = gridDimensionData[po].length;
        gridLonIncrement = range / (gridNLonValues - 1);
        lonIsPM180 =
            !DataHelper.lonNeedsToBe0360(
                gridDimensionData[po][0], gridDimensionData[po][gridNLonValues - 1]);
      }
      Test.ensureEqual(
          dad.getStop() + 1,
          gridDimensionData[po].length,
          "Opendap.getGridInfo("
              + gridName
              + ") "
              + ") dad.getStop()+1!= gridDimensionData["
              + po
              + "].size().\n  (url = "
              + url
              + ")");
      if (verbose)
        String2.log(
            "  Dimension "
                + dad.getName()
                + " length="
                + (dad.getStop() + 1)
                + " time="
                + (System.currentTimeMillis() - time1)
                + "ms");
      po++;
    }
    Test.ensureEqual(
        numDimensions,
        po,
        "Opendap.getGridInfo(" + gridName + ") numDimensions != po.\n  (url = " + url + ")");
    if (verbose)
      String2.log(
          "  names="
              + String2.toCSSVString(gridDimensionNames)
              + "\n"
              + "  gridTimeDimension="
              + gridTimeDimension
              + " min="
              + gridDimensionData[gridTimeDimension][0]
              + " max="
              + gridDimensionData[gridTimeDimension][
                  gridDimensionData[gridTimeDimension].length - 1]
              + "\n"
              + "  gridDepthDimension="
              + gridDepthDimension
              + (gridDepthDimension >= 0
                  ? " min="
                      + gridDimensionData[gridDepthDimension][0]
                      + " max="
                      + gridDimensionData[gridDepthDimension][
                          gridDimensionData[gridDepthDimension].length - 1]
                  : "")
              + "\n"
              + "  gridLatDimension="
              + gridLatDimension
              + " nLat="
              + gridNLatValues
              + " inc="
              + gridLatIncrement
              + " min="
              + gridDimensionData[gridLatDimension][0]
              + " max="
              + gridDimensionData[gridLatDimension][gridDimensionData[gridLatDimension].length - 1]
              + "\n"
              + "  gridLonDimension="
              + gridLonDimension
              + " nLon="
              + gridNLonValues
              + " inc="
              + gridLonIncrement
              + " min="
              + gridDimensionData[gridLonDimension][0]
              + " max="
              + gridDimensionData[gridLonDimension][gridDimensionData[gridLonDimension].length - 1]
              + "\n"
              + "  missingValue="
              + gridMissingValue
              + "  gridName="
              + gridName
              + "\n"
              + "  Opendap.getGridInfo done. TIME="
              + (System.currentTimeMillis() - time)
              + "ms\n");
  }

  /**
   * If getGridInfo ran successfully, this returns one of the lon values.
   *
   * @param index
   * @return the lon value for the specified index
   * @throws Exception if trouble
   */
  public double getLon(int index) {
    return gridDimensionData[gridLonDimension][index];
  }

  /**
   * If getGridInfo ran successfully, this returns one of the lat values.
   *
   * @param index
   * @return the lat value for the specified index
   * @throws Exception if trouble
   */
  public double getLat(int index) {
    return gridDimensionData[gridLatDimension][index];
  }

  /**
   * Generate timeOptions based on the values in the primitiveVector
   * gridDimensionData[gridTimeDimension] (so getGridInfo must have been run successfully). This
   * uses numberOfObservations if it isn't null.
   *
   * @param formatAsDate if true, options are formatted as dates (e.g., 1996-05-04), otherwise they
   *     are formatted as dateTimes with a space between date and time (e.g., 1996-05-04 14:40:22).
   * @param factorToGetSeconds the factor needed to convert the time data to seconds (e.g., for
   *     "minutes since ..." use 60); usually this.gridTimeFactorToGetSeconds
   * @param baseSeconds the seconds since 1970-01-01 that is the offset for the time values in the
   *     files; usually this.gridTimeBaseSeconds.
   * @param timePeriodNHours one of the TimePeriod.N_HOURS
   * @throws Exception
   */
  public void getTimeOptions(
      boolean formatAsDate, double factorToGetSeconds, double baseSeconds, int timePeriodNHours)
      // * @param timeFormat for format of the times in the file: 0 = int days since timeOffset,
      // 1=double
      // * @param timeOffset the time0 time for time values in the OPeNDAP file.
      // int timeFormat, GregorianCalendar timeOffset)
      throws Exception {

    double timeArray[] = gridDimensionData[gridTimeDimension];

    // generateTimeOptions (convert time_series values into human readable date or datetimes)
    GregorianCalendar gcZ = Calendar2.newGCalendarZulu();
    StringArray timeOptionsSA = new StringArray();
    IntArray timeOptionsIndexIA = new IntArray();
    int nTimeValues = timeArray.length;
    String errorInMethod = String2.ERROR + " in Opendap.getTimeOptions\n  url = " + url + "\n  ";
    if (verbose) String2.log("timeLongName=" + timeLongName);
    for (int i = 0; i < nTimeValues; i++) {
      double d = timeArray[i];
      // deal with bad time values
      if (!Double.isFinite(d)) {
        // important for vector common dates: just keep valid dates
        String2.log(errorInMethod + "Bad time value.");
        // deal with nObservations = 0
        // see comments above about numberOfObservations
        // } else if (numberOfObservations != null && numberOfObservations[i] == 0) {
        //    if (verbose) String2.log("Reject timeIndex = " + i + ": numberOfObservations = 0.");
      } else {
        // format the value     (rounded to nearest second)
        gcZ.setTimeInMillis(
            1000
                * Math.round(
                    Calendar2.unitsSinceToEpochSeconds(baseSeconds, factorToGetSeconds, d)));

        // adjust erd's old-style time "End Time" to centered time
        if (timeLongName != null && timeLongName.equals("End Time")) {
          try {
            // fix old-style (pre-Dec 2006) nDay and 1 month end times  so 00:00
            if (timePeriodNHours > 1 && timePeriodNHours % 24 == 0) gcZ.add(Calendar2.SECOND, 1);
            TimePeriods.endCalendarToCenteredTime(timePeriodNHours, gcZ, errorInMethod);
          } catch (Exception e) {
            String2.log(e.toString());
            continue;
          }
        }

        String tOption =
            formatAsDate
                ? Calendar2.formatAsISODate(gcZ)
                : // time value, if any, is discarded
                Calendar2.formatAsISODateTimeSpace(gcZ);
        String previousOption =
            timeOptionsSA.size() > 0 ? timeOptionsSA.get(timeOptionsSA.size() - 1) : "";
        if (tOption.equals(previousOption)) {
          String2.log(errorInMethod + "Duplicate tOption (" + tOption + ").");
        } else {
          timeOptionsSA.add(tOption);
          timeOptionsIndexIA.add(i);
        }
      }
    }
    timeOptions = timeOptionsSA.toArray();
    timeOptionsIndex = timeOptionsIndexIA.toArray();
  }

  /**
   * This is a higher-level way to generate a grid with the specified data (for one time point) from
   * an OPeNDAP source. This relies on the information set by getGridInfo.
   *
   * <p>WARNING! This assumes the Dimension order is [optional: time_series,] lat, lon.
   *
   * <p>See the superclass' documentation.
   */
  public Grid makeGrid(
      String desiredTimeOption,
      double minX,
      double maxX,
      double minY,
      double maxY,
      int desiredNLon,
      int desiredNLat)
      throws Exception {

    // echo parameters
    long startTime = System.currentTimeMillis();
    DConnect dConnect = new DConnect(url, acceptDeflate, 1, 1);
    desiredNLon = Math.max(1, desiredNLon);
    desiredNLat = Math.max(1, desiredNLat);
    double lonDim[] = gridDimensionData[gridLonDimension];
    double latDim[] = gridDimensionData[gridLatDimension];
    String msg =
        "Opendap.makeGrid(desired: Time="
            + desiredTimeOption
            + "\n    minX="
            + minX
            + " maxX="
            + maxX
            + " minY="
            + minY
            + " maxY="
            + maxY
            + " nWide="
            + desiredNLon
            + " nHigh="
            + desiredNLat
            + "\n    url = "
            + url
            + "\n  dataset minX="
            + lonDim[0]
            + " maxX="
            + lonDim[lonDim.length - 1]
            + " minY="
            + latDim[0]
            + " maxY="
            + latDim[latDim.length - 1]
            + "\n    xInc="
            + gridLonIncrement
            + " yInc="
            + gridLatIncrement
            + ")";
    if (verbose) {
      String2.log(msg);
    }
    String errorInMethod = String2.ERROR + " in " + msg + ":\n";

    if (minX > maxX)
      Test.error(errorInMethod + "minX (" + minX + ") must be <= maxX (" + maxX + ").");
    if (minY > maxY)
      Test.error(errorInMethod + "minY (" + minY + ") must be <= maxY (" + maxY + ").");

    // getMinX and getMaxX are minX and maxX adjusted to be appropriate for the data
    // originalDesiredLonIncrement (e.g. .1 degrees) used to calculate new, larger desireNWide if
    // 360 degrees
    double getMinX = minX;
    double getMaxX = maxX;
    double getMinY = minY;
    double getMaxY = maxY;
    int getNLon = desiredNLon;
    int getNLat = desiredNLat;
    double originalDesiredLonIncrement =
        Math.max(gridLonIncrement, (maxX - minX) / (desiredNLon - 1));
    boolean getAllX = false;
    double dataMinX = getLon(0);
    double dataMaxX = getLon(gridNLonValues - 1);
    if (lonIsPM180) { // data is inherently -180 to 180
      if (getMinX < 180 && getMaxX > 180) {
        if (dataMinX >= 0) {
          getMaxX = 180; // there is no data <0 to be moved to >180
        } else if (dataMaxX <= 0) {
          getMinX = -180; // there is no data > 0
          getMaxX -= 360;
        } else {
          // get entire width, extract desired later with getGrid
          getMinX = -180;
          getMaxX = 180;
          getNLon =
              1
                  + Math.min(
                      Integer.MAX_VALUE - 2,
                      Math2.roundToInt(Math.ceil(360 / originalDesiredLonIncrement)));
          getAllX = true;
        }
      } else if (getMaxX <= 180) {
        // do nothing
      } else { // getMinX >=180
        getMinX -= 360;
        getMaxX -= 360;
      }
    } else { // data is inherently 0..360
      if (getMinX < 0 && getMaxX > 0) {
        if (dataMaxX <= 180) {
          getMinX = 0; // there is no data >180 to be moved to <0
        } else if (dataMinX >= 180) {
          getMinX += 360; // there is no data <180
          getMaxX = 360;
        } else {
          // get entire width, extract desired later with getGrid
          getMinX = 0;
          getMaxX = 360;
          getNLon =
              1
                  + Math.min(
                      Integer.MAX_VALUE - 2,
                      Math2.roundToInt(Math.ceil(360 / originalDesiredLonIncrement)));
          getAllX = true;
        }
      } else if (getMaxX <= 0) {
        getMinX += 360;
        getMaxX += 360;
      } else { // getMinX >= 0
        // do nothing
      }
    }
    if (verbose)
      String2.log(
          "  Opendap.makeGrid adjusted: lonIsPM180="
              + lonIsPM180
              + " getAllX="
              + getAllX
              + "\n    origDesiredLonInc="
              + originalDesiredLonIncrement
              + " getNLon="
              + getNLon
              + "\n    getMinX="
              + getMinX
              + " getMaxX="
              + getMaxX
              + " getMinY="
              + getMinY
              + " getMaxY="
              + getMaxY);

    int nDimensions = gridDimensionData.length;
    int minIndex[] = new int[nDimensions];
    int maxIndex[] = new int[nDimensions];
    int stride[] = new int[nDimensions];
    Arrays.fill(minIndex, 0); // altitide dimension uses the defaults
    Arrays.fill(maxIndex, 0);
    Arrays.fill(stride, 1);

    // find the desiredTimeOption and associated Index value
    if (desiredTimeOption != null) {
      // identify desired time index
      int timeIndex = String2.indexOf(timeOptions, desiredTimeOption);
      if (timeIndex < 0) {
        throw new RuntimeException(
            errorInMethod + "Requested end date (" + desiredTimeOption + ") is not available.");
      }
      // convert timeOption back to index in time dimension (since some dates are skipped)
      minIndex[gridTimeDimension] = timeOptionsIndex[timeIndex];
      maxIndex[gridTimeDimension] = timeOptionsIndex[timeIndex];
      stride[gridTimeDimension] = 1;

      // verify that the time value is the expected value
      // Every day, the OceanWatch datasets throw out the data for the oldest time point(s).
      // After that occurs, and before the next Shared is created,
      // the time values are off by one.
      // This checks for trouble and adjusts if possible.

      // get the current time value from Opendap
      long checkTime = System.currentTimeMillis();
      double observedTime =
          OpendapHelper.getDoubleArray(
              dConnect,
              "?"
                  + gridDimensionNames[gridTimeDimension]
                  + "["
                  + minIndex[gridTimeDimension]
                  + "]")[0]; // just get one value

      // compare observed and expected
      // (observed and expected are in whatever offset/units/centeredVs.end the file uses)
      double expectedTime = gridDimensionData[gridTimeDimension][minIndex[gridTimeDimension]];
      if (observedTime != expectedTime) {
        // trouble
        String2.log(
            "WARNING! "
                + msg
                + ":\n"
                + "ObservedTime ("
                + observedTime
                + ") != expectedTime ("
                + expectedTime
                + ").");

        // trigger a re-indexing
        if (flagDirectory != null)
          File2.writeToFile88591(flagDirectory + "OpendapTimeIndexTrouble", "a");

        // read all the time data
        double timesAr[] =
            OpendapHelper.getDoubleArray(dConnect, "?" + gridDimensionNames[gridTimeDimension]);
        DoubleArray times = new DoubleArray(timesAr);

        // look for expected
        int newIndex = times.indexOf(expectedTime, 0);

        // can't find it?
        if (newIndex < 0) {
          String2.log(
              errorInMethod
                  + "Unable to find desired time: "
                  + desiredTimeOption
                  + "\n  timeDimensionName="
                  + gridDimensionNames[gridTimeDimension]
                  + "\n  expectedTime="
                  + expectedTime
                  + "\n  times="
                  + times);
          throw new RuntimeException(
              String2.ERROR
                  + ":\n"
                  + WAIT_THEN_TRY_AGAIN); // this message encourages getting new Shared in
          // Browser.java
        }

        minIndex[gridTimeDimension] = newIndex;
        maxIndex[gridTimeDimension] = newIndex;
      }
      if (verbose)
        String2.log(
            "  Opendap.makeGrid time to check/adjust timeIndex time="
                + (System.currentTimeMillis() - checkTime)
                + "ms");
    }

    // find the appropriate minIndex and maxIndex for lat
    minIndex[gridLatDimension] = DataHelper.binaryFindStartIndex(latDim, getMinY);
    maxIndex[gridLatDimension] = DataHelper.binaryFindEndIndex(latDim, getMaxY);
    if (minIndex[gridLatDimension] == -1 || maxIndex[gridLatDimension] == -1) {
      throw new RuntimeException(
          errorInMethod
              + "  "
              + MustBe.THERE_IS_NO_DATA
              + "\n"
              + "  requested: minY="
              + String2.genEFormat10(minY)
              + " maxY="
              + String2.genEFormat10(maxY)
              + "\n"
              + "  available: minY="
              + String2.genEFormat10(getLat(0))
              + " maxY="
              + String2.genEFormat10(getLat(gridNLatValues - 1))
              + "\n"
              + "  minIndex[lat]="
              + minIndex[gridLatDimension]
              + "  maxIndex[lat]="
              + maxIndex[gridLatDimension]);
    }
    getMinY = latDim[minIndex[gridLatDimension]];
    getMaxY = latDim[maxIndex[gridLatDimension]];

    // getNLat changes because file range may be less than desired range
    //  and this is important optimization because it reduces the number of rows of data read
    // getNLon was modified above
    getNLat = DataHelper.adjustNPointsNeeded(desiredNLat, maxY - minY, getMaxY - getMinY);
    if (verbose && getNLat != desiredNLat)
      String2.log(
          "  getMinY="
              + String2.genEFormat10(getMinY)
              + " getMaxY="
              + String2.genEFormat10(getMaxY)
              + "  getNLat="
              + getNLat);

    // set the appropriate stride values;  based on desired Min/Max X/Y
    //  because actual range of data may be much smaller
    //  but still want stride as if for the whole large area
    // !!Anomaly creation depends on this logic exactly matching corresponding code in Opendap.
    stride[gridLonDimension] = DataHelper.findStride(gridLonIncrement, getMinX, getMaxX, getNLon);
    stride[gridLatDimension] = DataHelper.findStride(gridLatIncrement, getMinY, getMaxY, getNLat);

    // getAllX assumes incoming lon are evenly spaced
    try {
      // ensure that is true
      if (getAllX) DataHelper.ensureEvenlySpaced(lonDim, "");
    } catch (Exception e) {
      // just use the normal system. Resulting lons (after makeLonPM180) may be unevenly spaced.
      getAllX = false;
    }

    // make the grid
    Grid grid = new Grid();
    if (getAllX) {
      // Requested data will require two opendap requests, left and right, to be swapped.
      // Lon indexes will be selected so resulting lon's (after makeLonPM180) will be evenly spaced.
      // This takes a lot of thought. Make diagrams to see how it works.
      int lonStride = stride[gridLonDimension];
      if (verbose) String2.log("  !!!getAllX=true: lonStride=" + lonStride);
      // where is center lon?
      double centerAt = lonIsPM180 ? 0 : 180;
      if (verbose) String2.log(Grid.axisInfoString("  source lon: ", lonDim));
      int centerIndex = Math2.binaryFindFirstGAE(lonDim, centerAt, 5);
      // centerIndex sometimes not exactly as expected: -.75, -.25, .25, .75...
      // offset is usually e.g., 0, but perhaps e.g., .25
      double offset = lonDim[centerIndex] - centerAt;
      // makeLonPM180 will match up lowIndex and highIndex
      double lowAt = (lonIsPM180 ? -180 : 0) + offset;
      double highAt = lowAt + 360;
      int lowIndex = centerIndex - Math2.roundToInt(180 / gridLonIncrement); // may be theoretical
      int highIndex = lowIndex + Math2.roundToInt(360 / gridLonIncrement);
      // find first real index above lowIndex
      int realLowIndex = lowIndex;
      while (realLowIndex < 0) realLowIndex += lonStride;
      // find last real index below highIndex
      int realHighIndex = highIndex;
      while (realHighIndex >= gridNLonValues) realHighIndex -= lonStride;
      // find last index below center which is n*lonStride from realLowIndex
      int belowCenter = realLowIndex;
      while (belowCenter + lonStride < centerIndex) belowCenter += lonStride;
      // find first index above center which is n*lonStride from realHighIndex
      int aboveCenter = realHighIndex;
      while (aboveCenter - lonStride >= centerIndex) aboveCenter -= lonStride;
      if (verbose)
        String2.log(
            "    centerIndex="
                + centerIndex
                + " offset="
                + offset
                + " lowIndex="
                + lowIndex
                + " realLowIndex="
                + realLowIndex
                + "\n    highIndex="
                + highIndex
                + " realHighIndex="
                + realHighIndex
                + " belowCenter="
                + belowCenter
                + " aboveCenter="
                + aboveCenter
            // + "\n    final lon=" + String2.toCSSVString(grid.lon)
            );

      // get eventual left half of data
      minIndex[gridLonDimension] = aboveCenter;
      maxIndex[gridLonDimension] = realHighIndex;
      grid = makeGrid(minIndex, maxIndex, stride);

      // get eventual right half of data
      minIndex[gridLonDimension] = realLowIndex;
      maxIndex[gridLonDimension] = belowCenter;
      Grid rightGrid = makeGrid(minIndex, maxIndex, stride);

      // merge them
      Test.ensureEqual(
          grid.lat, rightGrid.lat, errorInMethod + "Unexpected: The lat arrays don't match.");
      DoubleArray lonDA = new DoubleArray(grid.lon);
      DoubleArray rightLonDA = new DoubleArray(rightGrid.lon);
      if (lonIsPM180) rightLonDA.scaleAddOffset(1, 360);
      else lonDA.scaleAddOffset(1, -360);

      DoubleArray dataDA = new DoubleArray(grid.data);
      DoubleArray rightDataDA = new DoubleArray(rightGrid.data);
      grid.lon = null;
      grid.data = null;

      double lastLeftLon = lonDA.get(lonDA.size() - 1);
      double firstRightLon = rightLonDA.get(0);
      if (verbose)
        String2.log(
            "  firstLeftLon="
                + String2.genEFormat10(lonDA.get(0))
                + " last="
                + String2.genEFormat10(lastLeftLon)
                + " firstRightLon="
                + String2.genEFormat10(firstRightLon)
                + " last="
                + String2.genEFormat10(rightLonDA.get(rightLonDA.size() - 1)));
      if (Math2.almostEqual(5, lastLeftLon, firstRightLon)) {
        // remove duplicate column: firstRightLon
        rightLonDA.remove(0);
        rightDataDA.removeRange(0, grid.lat.length);
      } else {
        // add empty space in middle
        double incr = lonStride * gridLonIncrement;
        double emptyDataColumn[] = new double[grid.lat.length];
        Arrays.fill(emptyDataColumn, Double.NaN);
        DoubleArray emptyDataColumnDA = new DoubleArray(emptyDataColumn);
        while (!Math2.greaterThanAE(5, lastLeftLon + incr, firstRightLon)) {
          lastLeftLon += incr;
          lonDA.add(lastLeftLon);
          dataDA.append(emptyDataColumnDA);
        }
        // leave this in???  (for now)
        if (!Math2.almostEqual(5, lastLeftLon + incr, firstRightLon))
          Test.error(
              errorInMethod
                  + "lastLeftLon="
                  + String2.genEFormat10(lastLeftLon)
                  + " + incr="
                  + String2.genEFormat10(incr)
                  + " != firstRightLon="
                  + String2.genEFormat10(firstRightLon));
      }
      lonDA.append(rightLonDA);
      dataDA.append(rightDataDA);
      grid.lon = lonDA.toArray();
      grid.data = dataDA.toArray();
      lonDA = null;
      dataDA = null;
      rightDataDA = null;
      // leave this in???   (for now)
      DataHelper.ensureEvenlySpaced(
          grid.lon, errorInMethod + "The lon values aren't evenly spaced:\n");

    } else {
      // the data can be read in 1 request
      // find the appropriate minIndex and maxIndex for lon and lat
      minIndex[gridLonDimension] = DataHelper.binaryFindStartIndex(lonDim, getMinX);
      maxIndex[gridLonDimension] = DataHelper.binaryFindEndIndex(lonDim, getMaxX);
      if (minIndex[gridLonDimension] == -1 || maxIndex[gridLonDimension] == -1)
        throw new RuntimeException(
            errorInMethod
                + "  "
                + MustBe.THERE_IS_NO_DATA
                + "\n"
                + "  requested: getMinX="
                + String2.genEFormat10(getMinX)
                + " getMaxX="
                + String2.genEFormat10(getMaxX)
                + "\n"
                + "  available: minX="
                + String2.genEFormat10(getLon(0))
                + " maxX="
                + String2.genEFormat10(getLon(gridNLonValues - 1))
                + "\n"
                + "  minIndex[lon]="
                + minIndex[gridLonDimension]
                + "  maxIndex[lon]="
                + maxIndex[gridLonDimension]
                + "\n"
                + "  (For url = "
                + url
                + ")");

      // adjust getNLon
      getMinX = lonDim[minIndex[gridLonDimension]];
      getMaxX = lonDim[maxIndex[gridLonDimension]];
      getNLon = DataHelper.adjustNPointsNeeded(desiredNLon, maxX - minX, getMaxX - getMinX);
      stride[gridLonDimension] = DataHelper.findStride(gridLonIncrement, getMinX, getMaxX, getNLon);
      if (verbose)
        String2.log(
            "  adjusted getNLon="
                + getNLon
                + " getMinX="
                + String2.genEFormat10(getMinX)
                + " getMaxX="
                + String2.genEFormat10(getMaxX));

      // call the lower-level makeGrid
      grid = makeGrid(minIndex, maxIndex, stride);
    }

    // further process the data to make it exactly as requested
    // (since we are saving in a file with a specific name)
    // makeLonPM180Subset (throws Exception if no data)
    grid.makeLonPM180AndSubset(minX, maxX, getMinY, getMaxY, desiredNLon, getNLat);

    /*
    //write the file
    long writeTime = System.currentTimeMillis();
    grid.saveAsGrd(newDir, newName.substring(0, newName.length() - 4)); //remove .grd from newName
    if (verbose)
        String2.log("  writeTime=" + (System.currentTimeMillis() - writeTime));
    */
    if (verbose)
      String2.log(
          "  opendap.makeGrid done. TOTAL TIME="
              + (System.currentTimeMillis() - startTime)
              + "ms\n");
    return grid;
  }

  /**
   * This is a lower-level way to generate the specified Grid with the specified subset of the grid
   * data (for one time point) from an OPeNDAP source. This sets: minLonInNewFile, maxLonInNewFile,
   * minLatInNewFile, maxLatInNewFile, lonIncrementInNewFile, latIncrementInNewFile.
   *
   * <p>WARNING! The index arrays should have items in the same order as used for the grid's Array.
   * Only 2 of the indexes (representing lon and lat) should have max > min (others should be
   * min=max).
   *
   * <p>If time is one of the dimensions: the timeOptions you used to determine the correct
   * timeIndex will be incorrect if the DataSet has been updated (e.g., OceanWatch throws out the
   * oldest time point every day) since getGridInfo was called. This attempts to adjust but throws
   * an exception if it can't.
   *
   * @param minIndex is the index of the first valid value for each of the nIndexes
   * @param maxIndex is the index of the last valid value for each of the nIndexes
   * @param stride says to get every stride-th value for each of the nIndexes
   * @throws Exception if serious error occurs
   */
  public Grid makeGrid(int minIndex[], int maxIndex[], int stride[]) throws Exception {

    long startTime = System.currentTimeMillis();
    DConnect dConnect = new DConnect(url, acceptDeflate, 1, 1);
    minLonInNewFile = getLon(minIndex[gridLonDimension]);
    maxLonInNewFile = getLon(maxIndex[gridLonDimension]);
    minLatInNewFile = getLat(minIndex[gridLatDimension]);
    maxLatInNewFile = getLat(maxIndex[gridLatDimension]);
    if (minLonInNewFile > maxLonInNewFile) {
      double d = minLonInNewFile;
      minLonInNewFile = maxLonInNewFile;
      maxLonInNewFile = d;
    }
    if (minLatInNewFile > maxLatInNewFile) {
      double d = minLatInNewFile;
      minLatInNewFile = maxLatInNewFile;
      maxLatInNewFile = d;
    }
    lonIncrementInNewFile = stride[gridLonDimension] * gridLonIncrement;
    latIncrementInNewFile = stride[gridLatDimension] * gridLatIncrement;
    if (verbose)
      String2.log(
          "Opendap.makeGrid/Index(gridName="
              + gridName
              +
              // " minIndex=" + String2.toCSSVString(minIndex) +
              // " maxIndex=" + String2.toCSSVString(maxIndex) +
              // " strides=" + String2.toCSSVString(stride) + "\n" +
              // "  newDir=" + newDir + "\n" +
              // "  newName=" + newName + "\n" +
              "\n  inNewFile minLon="
              + String2.genEFormat10(minLonInNewFile)
              + " maxLon="
              + String2.genEFormat10(maxLonInNewFile)
              + " minLat="
              + String2.genEFormat10(minLatInNewFile)
              + " maxLat="
              + String2.genEFormat10(maxLatInNewFile)
              + "\n  lonIncr="
              + String2.genEFormat10(lonIncrementInNewFile)
              + " latIncr="
              + String2.genEFormat10(latIncrementInNewFile)
              + "\n  url="
              + url
              + ")");

    // start getting the data
    StringBuilder sb = new StringBuilder("?" + gridName);
    for (int index = 0; index < minIndex.length; index++)
      sb.append("[" + minIndex[index] + ":" + stride[index] + ":" + maxIndex[index] + "]");
    String query = sb.toString();
    long getTime = System.currentTimeMillis();
    PrimitiveArray pa[] = null;
    try {
      // throw new Exception("test opendap exception"); //normally this line is commented out
      pa = OpendapHelper.getPrimitiveArrays(dConnect, query); // throws Exception if trouble
    } catch (Exception e) {
      Test.error(
          WAIT_THEN_TRY_AGAIN
              + // this message encourages getting new Shared in CWBrowser.java
              "\n(Opendap dataset not available:\n  "
              + query
              + "\n"
              + e
              + ")");
    }
    getTime = System.currentTimeMillis() - getTime;
    PrimitiveArray dataPA = pa[0];
    int dataPaSize = dataPA.size();
    /*
    //because of compression (and lots of NaNs), this isn't good measure of connection's KB/s
    if (verbose) {
        int nBytesPer = dataPA instanceof DoubleArray? 8 : 4;
        int bytes = dataPaSize * nBytesPer;                                   //Bytes/ms = KB/s
        String2.log("  after decompressed, KB read=" + (bytes / 1000.0) +
            " KB/s=" + (bytes / Math.max(1,getTime)));
        //Math2.sleep(3000);
    }*/
    int nLon =
        ((maxIndex[gridLonDimension] - minIndex[gridLonDimension]) / stride[gridLonDimension]) + 1;
    int nLat =
        ((maxIndex[gridLatDimension] - minIndex[gridLatDimension]) / stride[gridLatDimension]) + 1;
    int expectedLength = nLon * nLat;
    Test.ensureEqual(dataPaSize, expectedLength, "dataPaSize != expectedLength.");

    // do post-check that time value has not changed since pre-check
    if (gridTimeDimension >= 0) {
      PrimitiveArray timePA = pa[gridTimeDimension + 1]; // +1 since [0] is data
      double observedMinTime = timePA.getDouble(0);

      // compare observedMin and expectedMin     !!!you could check all observed and check x,y, too
      // (observed and expected are in whatever offset/units the file uses)
      double expectedMinTime = gridDimensionData[gridTimeDimension][minIndex[gridTimeDimension]];
      if (observedMinTime != expectedMinTime) {
        String2.log(
            String2.ERROR
                + ": Opendap.makeGrid post-check of time failed!"
                + "\n  url="
                + url
                + "\n  timeDimensionName="
                + gridDimensionNames[gridTimeDimension]
                + "\n  expectedMinTime="
                + expectedMinTime
                + "\n  observedMinTime="
                + observedMinTime);
        Test.error(
            WAIT_THEN_TRY_AGAIN
                + // this message encourages getting new Shared in CWBrowser.java
                " (post check)");
      }
    }

    // put the data in a Grid object
    // grid.data is a 1D array, column by column, from the lower right (the way SGT wants it).
    Grid grid = new Grid();
    grid.latSpacing = latIncrementInNewFile;
    grid.lonSpacing = lonIncrementInNewFile;
    grid.lat = DataHelper.getRegularArray(nLat, minLatInNewFile, latIncrementInNewFile);
    grid.lon = DataHelper.getRegularArray(nLon, minLonInNewFile, lonIncrementInNewFile);
    grid.data = new double[nLon * nLat];
    float missingValue = String2.parseFloat(gridMissingValue); // round to float is needed
    int po = 0; // gather data in desired order
    for (int tLon = 0; tLon < nLon; tLon++) {
      for (int tLat = 0; tLat < nLat; tLat++) {
        double tData = dataPA.getDouble(tLat * nLon + tLon);
        if ((float) tData == missingValue) { // (float) for fuzzy test
          grid.data[po++] = Double.NaN;
        } else {
          grid.data[po++] = tData;
        }
      }
    }
    Test.ensureEqual(po, expectedLength, "po != expectedLength.");

    // timings
    // for westus:
    // http://las.pfeg.noaa.gov/cgi-bin/nph-dods/data/oceanwatch/nrt/poes/AT1day.nc.ascii?ssta.ssta[59:1:59][640:1:2240][800:1:2560]
    // AT 1km has the smallest lon and gridLatIncrement, by far
    // this procedure reports totalTime=40673 split=458 write=3371
    // and opera reports 22 MB and took 52 s
    // which is ~400KB/s
    // (I note that my network connection is often 340 KB/s)
    // I note that data is floats, so pretty compact,
    //  each datum ~9 bytes: [, 16.8754], ~2x bigger than float (4)
    if (verbose)
      String2.log(
          "  nLon="
              + grid.lon.length
              + " nLat="
              + grid.lat.length
              + " lonInc="
              + String2.genEFormat10(grid.lonSpacing)
              + " latInc="
              + String2.genEFormat10(grid.latSpacing)
              + " lon0="
              + String2.genEFormat10(grid.lon[0])
              + " lat0="
              + String2.genEFormat10(grid.lat[0])
              +
              // "\n  missingValueCount=" + missingValueCount
              "\n  opendap.makeGrid/index done. getTime="
              + getTime
              + " TOTAL TIME="
              + (System.currentTimeMillis() - startTime)
              + "ms\n");

    return grid;
  }

  /**
   * If bt instanceOf DArray and one of the dimensions is named dimensionName, this returns the
   * length of that dimension.
   *
   * @param bt a BaseType that might be a DArray
   * @param dimensionName the desired dimensionName
   * @return the length of the matching dimension (or -1 if not found)
   */
  private static int getDArrayLength(BaseType bt, String dimensionName) {
    if (bt instanceof DArray da) {
      // bt is read-only so no need to use sychronized(bt)
      Enumeration e = da.getDimensions();
      while (e.hasMoreElements()) {
        DArrayDimension dam = (DArrayDimension) e.nextElement();
        if (dam.getName().equals(dimensionName)) {
          return dam.getStop() + 1; // start = 0; stop is inclusive; so nValues = getStop()+1
        }
      }
    }
    return -1;
  }

  /* *
   * Find the last element (if you go from min to max)
   * which is <= x in an ascending or descending sorted array.
   *
   * @param pv a numeric, sorted PrimitiveVector which may not have duplicate values
   * @param x
   * @return the last element which is <= x in a sorted array.
   *   If x < the smallest element, this returns -1  (no element is appropriate).
   *   If x > the largest element, this returns pv.length-1.
   */
  /*out of date and maybe not exactly right. see DataHelper.binaryFindStartIndex
   public static int findLastLE(PrimitiveVector pv, double x) {
      //since pv doubles are hard to get at, use a linear search

      //array is ascending?
      int i, n = pv.getLength();
      if (getDouble(pv, n - 1) > getDouble(pv, 0)) {
          i = n - 1;
          while (i >= 0) {
              double d = getDouble(pv, i);
              if (Math2.almostEqual(5, d, x) || d <= x)
                  return i;
              i--;
          }
      } else {
          i = 0;
          while (i < n) {
              double d = getDouble(pv, i);
              if (Math2.almostEqual(5, d, x) || d <= x)
                  return i;
              i++;
          }
      }

      return i;


  }


  /* *
   * Find the first element (if you go from min to max)
   * which is >= x in an ascending or descending sorted array.
   *
   * @param pv a numeric, sorted PrimitiveVector which may not have duplicate values
   * @param x
   * @return the first element which is >= x in a sorted array.
   *   If x < the smallest element, this returns 0.
   *   If x > the largest element, this returns pv.length (no element is appropriate).
   */
  /*out of date and maybe not exactly right. see DataHelper.findEndIndex
   public static int findFirstGE(PrimitiveVector pv, double x) {

       //array is ascending?
       int i, n = pv.getLength();
       if (getDouble(pv, n - 1) > getDouble(pv, 0)) {
           i = 0;
           while (i < n) {
               double d = getDouble(pv, i);
               if (Math2.almostEqual(5, d, x) || d >= x)
                   return i;
               i++;
           }
       } else {
           i = n - 1;
           while (i >= 0) {
               double d = getDouble(pv, i);
               if (Math2.almostEqual(5, d, x) || d >= x)
                   return i;
               i--;
           }
       }

       return i;
   }

  */

  /**
   * This performs some diagnostic tests of Opendap.
   *
   * @throws Exception if troube
   */
  public static void main(String args[]) throws Exception {
    // basicTest();
  }
}
