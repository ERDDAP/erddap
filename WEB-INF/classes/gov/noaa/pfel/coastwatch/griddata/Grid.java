/*
 * Grid Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.hdf.HdfConstants;
import gov.noaa.pfel.coastwatch.hdf.HdfScientificData;
import gov.noaa.pfel.coastwatch.hdf.SdsReader;
import gov.noaa.pfel.coastwatch.hdf.SdsWriter;
import gov.noaa.pfel.coastwatch.util.DataStream;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * import ncsa.... See the comments for initialize and saveAsHDFViaNCSA. The file was jhdf.jar
 * (Windows and Linux variants and related jhdf.dll for Windows and libjhdf.so for Linux) in
 * <context>/WEB-INF/lib.
 */
// comment these out if not using saveAsHDFViaNCSA
// import ncsa.hdf.hdflib.HDFConstants;
// import ncsa.hdf.hdflib.HDFException;
// import ncsa.hdf.hdflib.HDFLibrary;

/**
 * This class holds actual grid data (e.g., from a .grd file -- a GMT-style NetCDF file). Since
 * there is often a lot of data, these objects are usually short-lived. Currently it can read .grd
 * and .nc files. In the future it may read and write other types of files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-10
 */
public class Grid {

  /**
   * A 1D array, column by column, from the lower left (the way SGT wants it). Missing values are
   * stored as NaN's. Note that this could/should be a PrimitiveArray (to conserve memory), but SVG
   * always wants this as a double[].
   */
  public double data[];

  /** (Almost always) evenly spaced latitude values, in ascending order. */
  public double lat[];

  /** (Almost always) evenly spaced longitude values, in ascending order. */
  public double lon[];

  /**
   * The distance between values in the lat array. For now, all files must have even lat and lon
   * spacing. [Someday, remove this and modify code to allow uneven spacing.]
   */
  public double latSpacing;

  /**
   * The distance between values in the lon array. For now, all files must have even lat and lon
   * spacing. [Someday, remove this and modify code to allow uneven spacing.]
   */
  public double lonSpacing;

  /**
   * The Data range. Some procedures that set this just do a simple job, e.g., readGrd just reports
   * minData,maxData from the original data, while the current subset's minData,maxData may be
   * different. If you need these values calculated, use calculateStats.
   */
  public double minData = Double.NaN, maxData = Double.NaN;

  /**
   * The number of valid points in the current grid. Some procedures that load the data set this to
   * Integer.MIN_VALUE. If so, use calculateStats to set this.
   */
  public int nValidPoints = Integer.MIN_VALUE;

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Set this to true (by calling doExtraErrorChecking=true in your program, not by changing the
   * code here) if you want extra error checking to be done.
   */
  public static boolean doExtraErrorChecking = false;

  /* A string used by climatology datasets. */
  public static final String SINCE_111 = "since 0001-01-01";

  /** The attribute lists are used sporadically; see method documentation. */
  private Attributes globalAttributes = new Attributes();

  private Attributes latAttributes = new Attributes();
  private Attributes lonAttributes = new Attributes();
  private Attributes dataAttributes = new Attributes();

  /**
   * This returns a medium-deep clone of this Grid.
   *
   * @return a clone of this Grid.
   */
  @Override
  public Object clone() {
    Grid grid = new Grid();

    grid.latSpacing = latSpacing;
    grid.lonSpacing = lonSpacing;
    grid.minData = minData;
    grid.maxData = maxData;
    grid.nValidPoints = nValidPoints;
    if (lat != null) {
      grid.lat = new double[lat.length];
      System.arraycopy(lat, 0, grid.lat, 0, lat.length);
    }
    if (lon != null) {
      grid.lon = new double[lon.length];
      System.arraycopy(lon, 0, grid.lon, 0, lon.length);
    }
    if (data != null) {
      grid.data = new double[data.length];
      System.arraycopy(data, 0, grid.data, 0, data.length);
    }
    globalAttributes.copyTo(grid.globalAttributes());
    latAttributes.copyTo(grid.latAttributes());
    lonAttributes.copyTo(grid.lonAttributes());
    dataAttributes.copyTo(grid.dataAttributes());
    return grid;
  }

  /**
   * This returns the globalAttributes.
   *
   * @return globalAttributes
   */
  public Attributes globalAttributes() {
    return globalAttributes;
  }

  /**
   * This returns the latAttributes.
   *
   * @return latAttributes
   */
  public Attributes latAttributes() {
    return latAttributes;
  }

  /**
   * This returns the lonAttributes.
   *
   * @return lonAttributes
   */
  public Attributes lonAttributes() {
    return lonAttributes;
  }

  /**
   * This returns the dataAttributes.
   *
   * @return dataAttributes
   */
  public Attributes dataAttributes() {
    return dataAttributes;
  }

  /** This clears all information from this Grid. */
  public void clear() {
    latSpacing = 1; // I use 1 if not knowable
    lonSpacing = 1;
    minData = Double.NaN;
    maxData = Double.NaN;
    nValidPoints = Integer.MIN_VALUE;
    lat = null;
    lon = null;
    data = null;
    globalAttributes.clear();
    latAttributes.clear();
    lonAttributes.clear();
    dataAttributes.clear();
  }

  /** This makes it so that the data is valid, but not actual data. */
  public void noData() {
    minData = 0;
    maxData = 0;
    nValidPoints = 0;
    lat = new double[0];
    lon = new double[0];
    data = new double[0];
  }

  /** This sets the lon and lat spacing based on the lon and lat values. */
  public void setLonLatSpacing() {
    int nLon = lon.length;
    int nLat = lat.length;
    lonSpacing = nLon <= 1 ? 1 : (lon[nLon - 1] - lon[0]) / (nLon - 1);
    latSpacing = nLat <= 1 ? 1 : (lat[nLat - 1] - lat[0]) / (nLat - 1);

    // if just one .length is unknown, set other spacing to equal it
    // This is useful for saveAsASCII which requires lonSpacing = latSpacing
    if (nLon <= 1 && nLat > 1) lonSpacing = latSpacing;
    else if (nLat <= 1 && nLon > 1) latSpacing = lonSpacing;
  }

  /**
   * This returns a string with a summary of the lon array.
   *
   * @return a string with a summary of the lon array.
   */
  public String lonInfoString() {
    if (lon.length == 0) return "lon: n=0";
    // not (float) because I need to track exact values
    return "lon: min="
        + lon[0]
        + " max="
        + lon[lon.length - 1]
        + "\n    inc="
        + lonSpacing
        + " n="
        + lon.length;
  }

  /**
   * This returns a string with a summary of the lat array.
   *
   * @return a string with a summary of the lat array.
   */
  public String latInfoString() {
    if (lat.length == 0) return "lat: n=0";
    // not (float) because I need to track exact values
    return "lat: min="
        + lat[0]
        + " max="
        + lat[lat.length - 1]
        + "\n    inc="
        + latSpacing
        + " n="
        + lat.length;
  }

  /**
   * This returns a string with a summary of an axis.
   *
   * @param msg e.g., "lat: "
   * @param dar the axis values
   * @return a string with a summary of an axis.
   */
  public static String axisInfoString(String msg, double dar[]) {
    if (dar.length == 0) return msg + "n=0";
    return msg
        + "min="
        + dar[0]
        + " max="
        + dar[dar.length - 1]
        + "\n    inc="
        + ((dar[dar.length - 1] - dar[0]) / Math.max(1, dar.length - 1))
        + " n="
        + dar.length;
  }

  /**
   * This prints a header in a format that pretty closely mimics the C version of ncdump (starting
   * with the "{") and acts as if the file will be stored as an .nc file.
   *
   * @param variableName (usually from FileNameUtility.get6CharName(fileName))
   * @param centeredTimeGC the centered time (e.g., 2006-02-17T12:00:00, usually from
   *     FileNameUtility.getCenteredTimeGC(fileName)). If it is null, secondsSinceEpoch will appear
   *     as 0.
   * @return a string representation of this grid
   */
  public String getNCHeader(String variableName, GregorianCalendar centeredTimeGC) {
    int nLon = lon.length;
    int nLat = lat.length;
    double secondsSinceEpoch =
        centeredTimeGC == null ? 0 : centeredTimeGC.getTimeInMillis() / 1000.0;
    StringBuilder sb =
        new StringBuilder(
            // this pretty closely mimics the C version of ncdump
            // (number formats are a little different)
            // and acts as if the file will be stored as an .nc file
            "{\n"
                + "dimensions:\n"
                + "\ttime = 1 ;\n"
                + "\taltitude = 1 ;\n"
                + "\tlat = "
                + nLat
                + " ;\n"
                + "\tlon = "
                + nLon
                + " ;\n"
                + "variables:\n"
                + "\tdouble time(time) ;\n"
                + "\t\ttime:actual_range = "
                + secondsSinceEpoch
                + ", "
                + secondsSinceEpoch
                + ";\n"
                + "\t\ttime:long_name = \"Centered Time\" ;\n"
                + "\t\ttime:standard_name = \"time\" ;\n"
                + "\t\ttime:units = \""
                + Calendar2.SECONDS_SINCE_1970
                + "\" ;\n"
                + "\tdouble altitude(altitude) ;\n"
                + "\t\taltitude:actual_range = 0.0, 0.0;\n"
                + "\t\taltitude:long_name = \"Altitude\" ;\n"
                + "\t\taltitude:standard_name = \"altitude\" ;\n"
                + "\t\taltitude:units = \"m\" ;\n"
                + "\tdouble lat(lat) ;\n");
    sb.append(latAttributes.toNcString("\t\tlat:", " ;"));
    sb.append("\tdouble lon(lon) ;\n");
    sb.append(lonAttributes.toNcString("\t\tlon:", " ;"));
    sb.append("\tfloat " + variableName + "(time, altitude, lat, lon) ;\n");
    sb.append(dataAttributes.toNcString("\t\t" + variableName + ":", " ;"));
    sb.append("\n" + "// global attributes:\n");
    sb.append(globalAttributes.toNcString("\t\t:", " ;"));
    sb.append("}\n");

    return sb.toString();
  }

  /**
   * This gets a string representation of this grid.
   *
   * @param printAllData If false, it prints just the 4 corners.
   * @return a string representation of this grid
   */
  public String toString(boolean printAllData) {
    calculateStats();
    StringBuilder sb =
        new StringBuilder(getNCHeader("data", null)); // dummy info is sufficient for here
    sb.append(
        lonInfoString()
            + "\n"
            + latInfoString()
            + "\n"
            + "minData="
            + minData
            + ", maxData="
            + maxData
            + ", nValidPoints="
            + nValidPoints
            + "\n");
    if (printAllData) sb.append("gridData=\n" + String2.toCSSVString(data));
    sb.append("\n");

    return sb.toString();
  }

  /**
   * This returns a string with the values of the 4 corners of the grid.
   *
   * @return a string with the values of the 4 corners of the grid.
   */
  public String fourCorners() {
    StringBuilder sb = new StringBuilder();
    // for (int lati=0; lati<3; lati++)
    //    for (int loni=0; loni<3; loni++)
    //        sb.append("  lati=" + lati + " loni=" + loni + " data=" + getData(loni, lati) + "\n");
    int nLon = lon.length;
    int nLat = lat.length;
    if (nLon == 0 || nLat == 0) return "nLon=" + nLon + ", nLat=" + nLat;
    return sb.toString()
        + "lowerLeftData="
        + getData(0, 0)
        + ", upperLeftData="
        + getData(0, nLat - 1)
        + ", lowerRightData="
        + getData(nLon - 1, 0)
        + ", upperRightData="
        + getData(nLon - 1, nLat - 1);
  }

  /**
   * This tests if o is a Grid and has the same data. This doesn't test minData, maxData, or
   * nValidPoints, which will be equal if other things are equal and if calculateStats is called.
   * (Currently) This doesn't test globalAttribute, latAttribute, lonAttribute, or dataAttribute.
   *
   * @param o an object, presumably a Grid
   * @throws Exception if a difference is found.
   */
  @Override
  public boolean equals(Object o) {
    try {
      if (!(o instanceof Grid)) {
        return false;
      }
      Grid grid2 = (Grid) o;
      Test.ensureEqual(latSpacing, grid2.latSpacing, "latSpacing");
      Test.ensureEqual(lonSpacing, grid2.lonSpacing, "lonSpacing");

      int n = lat.length;
      Test.ensureEqual(n, grid2.lat.length, "latLength");
      for (int i = 0; i < n; i++)
        if (lat[i] != grid2.lat[i]) // avoid making tons of strings
        Test.ensureEqual(lat[i], grid2.lat[i], "lat[" + i + "]");

      n = lon.length;
      Test.ensureEqual(n, grid2.lon.length, "lonLength");
      for (int i = 0; i < n; i++)
        if (lon[i] != grid2.lon[i]) // avoid making tons of strings
        Test.ensureEqual(lon[i], grid2.lon[i], "lon[" + i + "]");

      n = data.length;
      for (int i = 0; i < n; i++)
        if (data[i] != grid2.data[i]) // avoid making tons of strings
        Test.ensureEqual(data[i], grid2.data[i], "data[" + i + "]");

      return true;
    } catch (Exception e) {
      String2.log(MustBe.throwable(String2.ERROR + " in Grid.equals", e));
      return false;
    }
  }

  @Override
  public int hashCode() {
    double hash = 31 * latSpacing;
    hash = 31 * hash + 31 * lonSpacing;
    int n = lat.length;
    for (int i = 0; i < n; i++) {
      hash = 31 * hash + lat[i];
    }

    n = lon.length;
    for (int i = 0; i < n; i++) {
      hash = 31 * hash + lon[i];
    }

    n = data.length;
    for (int i = 0; i < n; i++) {
      hash = 31 * hash + data[i];
    }
    return (int) hash;
  }

  /**
   * For each point in lon1, this finds the index of the closest point in lon2. If there is no
   * nearest lon2, the index is set to -1. If isLon, this also tries lon2+360 and lon2-360.
   *
   * @param lon1 a ascending sorted list of lat or lon values.
   * @param lon2 a ascending sorted list of lat or lon values.
   * @param isLon use true if these are indeed lon values; or false if lat values.
   * @return an array the same length as lon1, with the indexes of the closest lon2 values (or -1 if
   *     out of range).
   */
  public static int[] binaryFindClosestIndexes(double lon1[], double lon2[], boolean isLon) {

    // for each lon1 value, find the closest lon2 value
    int lon3[] = new int[lon1.length];
    int nLon2 = lon2.length;
    double lon2Spacing = (lon2[nLon2 - 1] - lon2[0]) / (nLon2 - 1);
    double minLon2 = lon2[0] - lon2Spacing / 2;
    double maxLon2 = lon2[nLon2 - 1] + lon2Spacing / 2;
    for (int i = 0; i < lon1.length; i++) {
      double tLon = lon1[i];
      if (tLon >= minLon2 && tLon <= maxLon2) lon3[i] = Math2.binaryFindClosest(lon2, tLon);
      else if (isLon && tLon + 360 >= minLon2 && tLon + 360 <= maxLon2)
        lon3[i] = Math2.binaryFindClosest(lon2, tLon + 360);
      else if (isLon && tLon - 360 >= minLon2 && tLon - 360 <= maxLon2)
        lon3[i] = Math2.binaryFindClosest(lon2, tLon - 360);
      else lon3[i] = -1;
      // String2.log("loni=" + i + " tLon=" + tLon +
      //    " minLon2="  + minLon2 +
      //    " maxLon2=" + maxLon2 +
      //    " closest=" + lon3[i]);
    }
    return lon3;
  }

  /**
   * For each point in the current grid, this subtracts the data from the nearest point in the
   * climatology grid. If there is no nearby point, the datum is set to NaN. !!!This blanks the
   * statistics information.
   *
   * @param climatology the grid which will be subtracted from the current grid. The climatology may
   *     have a different +/-180 range than this grid.
   */
  public void subtract(Grid climatology) {
    // blank the statistics information
    nValidPoints = Integer.MIN_VALUE;
    minData = Double.NaN;
    maxData = Double.NaN;

    // for each local lat value, find the closest climatology lat value
    int closestLat[] = binaryFindClosestIndexes(lat, climatology.lat, false);
    int closestLon[] = binaryFindClosestIndexes(lon, climatology.lon, true);

    // for each point in the current grid, subtract the data from the nearest
    // point in the climatology grid
    for (int loni = 0; loni < lon.length; loni++) {
      int closestLoni = closestLon[loni];
      for (int lati = 0; lati < lat.length; lati++) {
        int closestLati = closestLat[lati];
        // String2.log("loni=" + loni + " lati=" + lati + " closestLoni=" + closestLoni + "
        // closestLati=" + closestLati);
        if (closestLoni < 0 || closestLati < 0) {
          setData(loni, lati, Double.NaN);
          continue;
        }
        // subtract   (if either is NaN, result will be NaN)
        setData(loni, lati, getData(loni, lati) - climatology.getData(closestLoni, closestLati));
      }
    }
  }

  /**
   * Given grid2, with the same lat and lon values, this changes each of the pixels in the current
   * grid to be 'fraction' of the way between this grid and grid2. ???If just one of the grid or
   * grid2 pixels is NaN, this just uses the non-NaN value. !!!This blanks the statistics
   * information.
   *
   * @param grid2 a grid with the same lat lon values as this grid
   * @param fraction is the fraction of the way between this grid and grid2 to be interpolated
   *     (e.g., 0 will just use this grid's values; 1 will just use grid2's values).
   * @throws Exception if trouble
   */
  public void interpolate(Grid grid2, double fraction) {
    // blank the statistics information
    nValidPoints = Integer.MIN_VALUE;
    minData = Double.NaN;
    maxData = Double.NaN;

    // ensure the grid2 lat and lon values match this grid's lat lon values
    String errorInMethod = String2.ERROR + " in Grid.interpolate:\n";
    Test.ensureEqual(lat, grid2.lat, errorInMethod + "lat values not equal.");
    Test.ensureEqual(lon, grid2.lon, errorInMethod + "lat values not equal.");

    // for each point, interpolate
    int n = data.length;
    for (int i = 0; i < n; i++) {
      double d1 = data[i];
      double d2 = grid2.data[i];
      if (Double.isNaN(d1)) {
        data[i] = d2;
      } else if (Double.isNaN(d2)) {
        // leave data[i] unchanged
      } else {
        // interpolate
        data[i] = d1 + fraction * (d2 - d1);
      }
    }
  }

  /**
   * CURRENTLY, THIS DOES NOTHING SINCE THE NATIVE HDFLIBRARY ISN'T BEING USED. This loads the
   * native library that is used by ncsa.hdf.hdflib.HDFLibrary, which is imported above. Currently,
   * this works with Windows and Linux only. For other OS's, this won't fail, but saveAsHDF will
   * fail because no library will have been loaded. Also, the jhdf.jar file referred to in the class
   * path must be the appropriate version (from the Windows or Linux distributions of HDFView).
   *
   * @param dir the directory with the jhdf.dll and libjhdf.so files, with a slash at the end
   */
  /*public static void initialize(String dir) {
      if (String2.OSIsWindows)
          System.load(dir + "jhdf.dll");
      else if (String2.OSIsLinux)
          System.load(dir + "libjhdf.so");
      else String2.log("Grid.initialize currently only works with Windows and Linux.");
  }
  */

  /**
   * This reads a grid from a binary file which just has the data values (stored as little-endian 2
   * byte signed integers) and populates the public variables. Currently, the data must be row by
   * row, with the data top row at the start of the file (like an image). This sets minData,
   * maxData, and nValidPoints. This is tested by SgtMap.testCreateTopographyGrid. !!!This assumes
   * that the lon and lat values are evenly spaced!!!
   *
   * <p>This method works by finding the closest min,max Lon,Lat and calculating the smallest
   * possible lat and lon stride.
   *
   * @param fullFileName .../ref/etopo1_ice_g_i2.bin
   * @param fileMinLon the minimum longitude value in the file. Since the file just has data values,
   *     the fileXxx params tell this method how to interpret those values (ETOPO1g: -180).
   * @param fileMaxLon the maximum longitude value in the file (ETOPO1g: 180).
   * @param fileMinLat the minimum latitude value in the file (ETOPO1g: -90).
   * @param fileMaxLat the maximum latitude value in the file (ETOPO1g: 90).
   * @param fileNLonPoints the number of points in the file in the x direction (ETOPO1g: 10801)
   * @param fileNLatPoints the number of points in the file in the y direction (ETOPO1g: 5401)
   * @param desiredMinLon the minimum desired longitude. This can be -180 to 180, or 0 to 360.
   * @param desiredMaxLon the maximum desired longitude. This can be -180 to 180, or 0 to 360.
   * @param desiredMinLat the minimum desired latitude. This can range from almost -90 to 90.
   * @param desiredMaxLat the maximum desired latitude. This can range from almost -90 to 90.
   * @param desiredNLonPoints the desired number of points in the x direction. Or use
   *     Integer.MAX_VALUE to get the maximum available points.
   * @param desiredNLatPoints the desired number of points in the y direction. Or use
   *     Integer.MAX_VALUE to get the maximum available points.
   * @throws Exception if trouble
   */
  public void readBinary(
      String fullFileName,
      double fileMinLon,
      double fileMaxLon,
      double fileMinLat,
      double fileMaxLat,
      int fileNLonPoints,
      int fileNLatPoints,
      double desiredMinLon,
      double desiredMaxLon,
      double desiredMinLat,
      double desiredMaxLat,
      int desiredNLonPoints,
      int desiredNLatPoints)
      throws Exception {
    // FUTURE: this cound accept data from files with different data types, e.g., float
    // FUTURE: this cound accept data in different order (stored col-by-col, from lower left

    // ensure desired range is acceptable
    if (verbose) String2.log("Grid.readBinary");
    clear();
    String errorInMethod = String2.ERROR + " in Grid.readBinary(" + fullFileName + "): ";
    if (desiredMinLon > desiredMaxLon)
      Test.error(
          errorInMethod
              + "desiredMinLon ("
              + desiredMinLon
              + ") must be <= desiredMaxLon ("
              + desiredMaxLon
              + ").");
    if (desiredMinLat > desiredMaxLat)
      Test.error(
          errorInMethod
              + "desiredMinLat ("
              + desiredMinLat
              + ") must be <= desiredMaxLat ("
              + desiredMaxLat
              + ").");
    long time = System.currentTimeMillis();

    // calculate file lon and values
    // file stores top row of data at start of file, but findClosest requires ascending array
    // so make array ascending, then adjust later
    double fileLonSpacing = (fileMaxLon - fileMinLon) / (fileNLonPoints - 1);
    double fileLatSpacing = (fileMaxLat - fileMinLat) / (fileNLatPoints - 1);
    double fileLon[] = DataHelper.getRegularArray(fileNLonPoints, fileMinLon, fileLonSpacing);
    double fileLat[] = DataHelper.getRegularArray(fileNLatPoints, fileMinLat, fileLatSpacing);
    // String2.log("fileLon=" + String2.toCSSVString(fileLon).substring(0, 70));
    // String2.log("fileLat=" + String2.toCSSVString(fileLat).substring(0, 70));

    // make the desired lon and lat arrays   (n, min, spacing)
    // String2.log("  desiredNLonPoints=" + desiredNLonPoints + " desiredNLatPoints=" +
    // desiredNLatPoints);
    double desiredLonRange = desiredMaxLon - desiredMinLon;
    double desiredLatRange = desiredMaxLat - desiredMinLat;
    lonSpacing = desiredLonRange / Math.max(1, desiredNLonPoints - 1);
    latSpacing = desiredLatRange / Math.max(1, desiredNLatPoints - 1);
    // spacing  (essentially stride)     (floor (not ceil) because it is inverse of stride)
    // String2.log("tlatSpacing=" + latSpacing + " fileLatSpacing=" + fileLatSpacing);
    lonSpacing = Math.max(1, Math.floor(lonSpacing / fileLonSpacing)) * fileLonSpacing;
    latSpacing = Math.max(1, Math.floor(latSpacing / fileLatSpacing)) * fileLatSpacing;
    if (verbose)
      String2.log(
          "  will get lonSpacing=" + (float) lonSpacing + " latSpacing=" + (float) latSpacing);

    // change desired to closest file points (if lat,lon arrays extended to appropriate range)
    // THIS ASSUMES FILE LONS AND LATS ARE AT MULTIPLES OF FileLon/LatSpacing.  (no offset)
    desiredMinLon = Math2.roundToInt(desiredMinLon / lonSpacing) * lonSpacing;
    desiredMaxLon = Math2.roundToInt(desiredMaxLon / lonSpacing) * lonSpacing;
    desiredMinLat = Math2.roundToInt(desiredMinLat / latSpacing) * latSpacing;
    desiredMaxLat = Math2.roundToInt(desiredMaxLat / latSpacing) * latSpacing;
    if (verbose)
      String2.log(
          "  will get"
              + " minLon="
              + (float) desiredMinLon
              + " maxLon="
              + (float) desiredMaxLon
              + " minLat="
              + (float) desiredMinLat
              + " maxLat="
              + (float) desiredMaxLat);

    // nLon nLat calculated as  lastLonIndex - firstLonIndex + 1
    // if (verbose) String2.log("  grid.readBinary lonSpacing=" + (float)lonSpacing + " latSpacing="
    // + (float)latSpacing);
    int nLon =
        Math2.roundToInt(desiredMaxLon / lonSpacing)
            - Math2.roundToInt(desiredMinLon / lonSpacing)
            + 1;
    int nLat =
        Math2.roundToInt(desiredMaxLat / latSpacing)
            - Math2.roundToInt(desiredMinLat / latSpacing)
            + 1;
    lon = DataHelper.getRegularArray(nLon, desiredMinLon, lonSpacing);
    lat = DataHelper.getRegularArray(nLat, desiredMinLat, latSpacing);
    if (verbose) String2.log("  will get nLon=" + nLon + " nLat=" + nLat);

    // find the offsets for the start of the rows closest to the desiredLon values
    int bytesPerValue = 2;
    int offsetLon[] = new int[nLon];
    for (int i = 0; i < nLon; i++) {
      double tLon = lon[i];
      while (tLon < fileMinLon) tLon += 360;
      while (tLon > fileMaxLon) tLon -= 360;
      int closestLon = Math2.binaryFindClosest(fileLon, tLon);
      offsetLon[i] = bytesPerValue * closestLon;
      // String2.log("tLon=" + tLon + " closestLon=" + closestLon + " offset=" + offsetLon[i]);
    }

    // find the offsets for the start of the columns closest to the desiredLon values
    int offsetLat[] = new int[nLat];
    for (int i = 0; i < nLat; i++) {
      double tLat = lat[i];
      while (tLat < fileMinLat) tLat += 90;
      while (tLat > fileMaxLat) tLat -= 90;
      int closestLat = Math2.binaryFindClosest(fileLat, tLat);
      // adjust lat, since fileLat is ascending, but file stores data top row at start of file
      closestLat = fileNLatPoints - 1 - closestLat;
      offsetLat[i] = bytesPerValue * closestLat * fileNLonPoints;
      // String2.log("tLat=" + tLat + " closestLat=" + closestLat + " offset=" + offsetLat[i]);
    }

    // open the file  (reading should be thread safe)
    RandomAccessFile raf = new RandomAccessFile(fullFileName, "r");
    try {

      // fill data array
      // lat is outer loop because file is lat major
      // and loop is backwards since stored top to bottom
      // (goal is to read basically from start to end of file)
      nValidPoints = nLon * nLat; // all points are valid
      data = new double[nValidPoints];
      int minSData = Integer.MAX_VALUE;
      int maxSData = Integer.MIN_VALUE;
      for (int tLat = nLat - 1; tLat >= 0; tLat--) {
        for (int tLon = 0; tLon < nLon; tLon++) {
          raf.seek(offsetLat[tLat] + offsetLon[tLon]);
          short ts = Short.reverseBytes(raf.readShort()); // reverseBytes since etopo1 is LSB
          setData(tLon, tLat, ts);
          minSData = Math.min(minSData, ts);
          maxSData = Math.max(maxSData, ts);
        }
      }
      minData = minSData;
      maxData = maxSData;
    } finally {
      raf.close();
    }
    if (verbose) String2.log("Grid.readBinary TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /* INACTIVE.  If reactivated, it needs to correctly deal with makeLonPM180.
   *
   * This just determines if a GMT .grd file has any data in the specified
   * region.
   *
   * @param fullFileName
   * @param makeLonPM180 If true, the lon values will be forced to be +-180.
   *     If false, the lon values will be forced to be 0..360.
   *     See makeLonPM180().
   * @param desiredMinLon the minimum desired longitude (use Double.NaN if no restriction)
   * @param desiredMaxLon the maximum desired longitude (use Double.NaN if no restriction)
   * @param desiredMinLat the minimum desired latitude (use Double.NaN if no restriction)
   * @param desiredMaxLat the maximum desired latitude (use Double.NaN if no restriction)
   * @return true if there is data for the specified region.
   *   Returns false for any other reason (including file doesn't exist).
   * @throws Exception if file not found
   */
  /*public static boolean grdHasData(String fullFileName, boolean makeLonPM180,
              double desiredMinLon, double desiredMaxLon,
              double desiredMinLat, double desiredMaxLat)
              throws Exception {

          //*** what is in the .grd netcdf file?
          if (verbose) String2.log("Grid.grdHasData: " + fullFileName);

          //open the file (before 'try'); if it fails, no temp file to delete
          NetcdfFile grdFile = NetcdfFiles.open(fullFileName);
          try {
              long time = System.currentTimeMillis();

              //get min/maxLon
              Variable variable = grdFile.findVariable("x_range");
              ArrayDouble.D1 add1 = (ArrayDouble.D1)variable.read();
              double minLon = add1.get(0);
              double maxLon = add1.get(1);

              //calculate minLon maxLon
              if ((minLon < 0   && maxLon > 0) ||
                  (minLon < 180 && maxLon > 180)) {
                  //do nothing; some points are in eastern hemisphere; some are in western.
              } else if (makeLonPM180 && maxLon > 180) {
                  minLon = Math2.looserAnglePM180(minLon);
                  maxLon = Math2.looserAnglePM180(maxLon);
              } else if (!makeLonPM180 && minLon < 0) {
                  minLon = Math2.looserAngle0360(minLon);
                  maxLon = Math2.looserAngle0360(maxLon);
              }

              //do lon test
              if (verbose) String2.log("  minLon=" + minLon + " maxLon=" + maxLon);
              if (minLon > desiredMaxLon || maxLon < desiredMinLon) {
                  grdFile.close();
                  return false;
              }

              //get min/maxLat
              variable = grdFile.findVariable("y_range");
              add1 = (ArrayDouble.D1)variable.read();
              double minLat = add1.get(0);
              double maxLat = add1.get(1);
              if (verbose) String2.log("  minLat=" + minLat + " maxLat=" + maxLat);
              if (minLat > desiredMaxLat || maxLat < desiredMinLat) {
                  grdFile.close();
                  return false;
              }

              //makeLonPM180
              makeLonPM180(makeLonPM180);
  switch to finally clause
              grdFile.close();
              if (lon[0] > desiredMaxLon || lon[lon.length - 1] < desiredMinLon ||
                  lat[0] > desiredMaxLat || lat[lat.length - 1] < desiredMinLat) return false;
              return true;
          } catch (Exception e) {
              if (verbose) String2.log(MustBe.throwable(String2.ERROR + " in Grid.grdHasData: " + fullFileName, e));
              if (grdFile != null)
                  grdFile.close();
              return false;
          }
      } */

  /**
   * This is like readGrd with more parameters, but uses NaN or MAX_VALUE for all of the desiredXxx
   * parameters, and so reads all of the data.
   *
   * @param fullFileName
   * @param makeLonPM180
   * @throws Exception if trouble (e.g., no data)
   */
  public void readGrd(String fullFileName, boolean makeLonPM180) throws Exception {
    readGrd(
        fullFileName,
        makeLonPM180 ? -180 : 0,
        makeLonPM180 ? 180 : 360,
        -90,
        90,
        Integer.MAX_VALUE,
        Integer.MAX_VALUE);
  }

  /**
   * This is like readGrd with more parameters, but uses NaN or MAX_VALUE for all of the desiredXxx
   * parameters, and so reads all of the data, as it is in the file.
   *
   * @param fullFileName
   * @throws Exception if trouble
   */
  public void readGrd(String fullFileName) throws Exception {
    readGrd(fullFileName, Double.NaN, Double.NaN, -90, 90, Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * This reads a GMT .grd file and populates the public variables. This just uses the .grd file's
   * minData and maxData to set minData and maxData; use calculateStats to get an exact range.
   *
   * <p>.grd (GMT-style NetCDF) files are read with code in netcdf-X.X.XX.jar which is part of the
   * <a href="https://www.unidata.ucar.edu/software/netcdf-java/" >NetCDF Java Library</a> renamed
   * as netcdf-latest.jar. Put it in the classpath for the compiler and for Java.
   *
   * @param fullFileName
   * @param desiredMinLon the minimum desired longitude. minLon and maxLon may imply +-180 or
   *     0..360; The native data will be converted to conform. Or use Double.NaN to use the file's
   *     minLon.
   * @param desiredMaxLon the maximum desired longitude
   * @param desiredMinLat the minimum desired latitude (use -90 if no restriction)
   * @param desiredMaxLat the maximum desired latitude (use 90 if no restriction)
   * @param desiredNLon the desired minimum number of points in the x direction (use
   *     Integer.MAX_VALUE).
   * @param desiredNLat the desired minimum number of points in the y direction (use
   *     Integer.MAX_VALUE).
   * @throws Exception if trouble
   */
  public void readGrd(
      String fullFileName,
      double desiredMinLon,
      double desiredMaxLon,
      double desiredMinLat,
      double desiredMaxLat,
      int desiredNLon,
      int desiredNLat)
      throws Exception {
    // if (verbose) String2.log(File2.hexDump(grdFileName, 300));

    if (verbose)
      String2.log(
          "Grid.readGrd: "
              + fullFileName
              + "\n  desired minX="
              + String2.genEFormat10(desiredMinLon)
              + " maxX="
              + String2.genEFormat10(desiredMaxLon)
              + " minY="
              + String2.genEFormat10(desiredMinLat)
              + " maxY="
              + String2.genEFormat10(desiredMaxLat)
              + "\n  desired nX="
              + desiredNLon
              + " nY="
              + desiredNLat);
    long time = System.currentTimeMillis();
    if (desiredMinLon > desiredMaxLon)
      Test.error(
          String2.ERROR
              + " in Grid.readGrd:\n"
              + "minX ("
              + desiredMinLon
              + ") must be <= maxX ("
              + desiredMaxLon
              + ").");
    if (desiredMinLat > desiredMaxLat)
      Test.error(
          String2.ERROR
              + " in Grid.readGrd:\n"
              + "minY ("
              + desiredMinLat
              + ") must be <= maxY ("
              + desiredMaxLat
              + ").");

    nValidPoints = Integer.MIN_VALUE;
    minData = Double.NaN;
    maxData = Double.NaN;

    // open the file (before 'try'); if it fails, no temp file to delete
    NetcdfFile grdFile = NcHelper.openFile(fullFileName);
    try {
      // if (verbose) String2.log(NcHelper.ncdump(fullFileName, "-h"));

      // get lon and lat
      int fileNLon = -1;
      int fileNLat = -1;
      double fileMinLon = Double.NaN;
      double fileMaxLon = Double.NaN;
      double fileMinLat = Double.NaN;
      double fileMaxLat = Double.NaN;
      double fileLon[], fileLat[];
      Variable variable = grdFile.findVariable("spacing");
      if (variable == null) {
        // gmt 4 files have the dimensions (but not dimension info)
        // so just read the fileLon and fileLat values directly.
        // !!!These files may not have evenly spaced lon lat values.
        // Many parts of Grid assume lat and lon are evenly spaced.
        // so, for now, ensureEvenlySpaced
        String2.log("  It's a GMT 4 file.");
        variable = grdFile.findVariable("x");
        DoubleArray da = NcHelper.getNiceDoubleArray(variable, 0, -1);
        String error = da.isEvenlySpaced();
        if (error.length() > 0) {
          // Dave says: see if it is at least crudely evenly spaced
          String2.log(error + "\n" + da.smallestBiggestSpacing());
          FloatArray fa = new FloatArray(da);
          String error2 = fa.isEvenlySpaced();
          if (error2.length() > 0) {
            throw new RuntimeException(error2);
          } else {
            // remake da from end points
            int nda = da.size();
            String2.log(
                "  remaking lon values. original: 0="
                    + da.get(0)
                    + " last="
                    + da.get(nda - 1)
                    + " n="
                    + nda);
            da =
                new DoubleArray(
                    DataHelper.getRegularArray(
                        nda, da.get(0), (da.get(nda - 1) - da.get(0)) / (nda - 1)));
            // String2.log("new da  0=" + da.get(0) + " last=" + da.get(nda-1) + " n=" + nda);
            // String error3 = da.isEvenlySpaced();
            // if (error3.length() > 0) throw new Exception(error3 + "\n" +
            // da.smallestBiggestSpacing());
          }
        }
        fileLon = da.toArray();
        fileNLon = fileLon.length;
        fileMinLon = fileLon[0];
        fileMaxLon = fileLon[fileNLon - 1];

        variable = grdFile.findVariable("y");
        da = NcHelper.getNiceDoubleArray(variable, 0, -1);
        error = da.isEvenlySpaced();
        if (error.length() > 0) {
          // Dave says: see if it is at least crudely evenly spaced
          String2.log(error + "\n" + da.smallestBiggestSpacing());
          FloatArray fa = new FloatArray(da);
          String error2 = fa.isCrudelyEvenlySpaced();
          if (error2.length() > 0) {
            throw new RuntimeException(error2);
          } else {
            // remake da from end points
            int nda = da.size();
            String2.log(
                "  remaking lat values. original: 0="
                    + da.get(0)
                    + " last="
                    + da.get(nda - 1)
                    + " n="
                    + nda);
            da =
                new DoubleArray(
                    DataHelper.getRegularArray(
                        nda, da.get(0), (da.get(nda - 1) - da.get(0)) / (nda - 1)));
            // String2.log("new y values 0=" + da.get(0) + " last=" + da.get(nda-1) + " n=" + nda);
            // String error3 = da.isEvenlySpaced();
            // if (error3.length() > 0) throw new Exception(error3 + da.smallestBiggestSpacing());
          }
        }
        fileLat = da.toArray();
        fileNLat = fileLat.length;
        fileMinLat = fileLat[0];
        fileMaxLat = fileLat[fileNLat - 1];

        variable = grdFile.findVariable("z");
        PrimitiveArray pa = NcHelper.getVariableAttribute(variable, "actual_range");
        minData = pa.getNiceDouble(0);
        maxData = pa.getNiceDouble(1);

        if (verbose)
          String2.log(
              "  fileNLon="
                  + fileNLon
                  + " fileNLat="
                  + fileNLat
                  + "  fileMinLon="
                  + fileMinLon
                  + " fileMaxLon="
                  + fileMaxLon
                  + "  fileMinLat="
                  + fileMinLat
                  + " fileMaxLat="
                  + fileMaxLat
                  + "  minData="
                  + minData
                  + " maxData="
                  + maxData);

      } else {
        // gmt 3 files store info about dimensions (e.g., lon,lat spacing), but not dimension values
        // These files *always* have evenly spaced lon lat values.
        if (verbose) String2.log("  It's a GMT 3 file.");
        DoubleArray da = NcHelper.getNiceDoubleArray(variable, 0, -1);
        double fileLonSpacing = da.get(0);
        double fileLatSpacing = da.get(1);
        if (Double.isNaN(fileLonSpacing)) // happens if only 1 value
        fileLonSpacing = 1;
        if (Double.isNaN(fileLatSpacing)) // happens if only 1 value
        fileLatSpacing = 1;

        // get nLon and nLat
        variable = grdFile.findVariable("dimension");
        ArrayInt.D1 aid1 = (ArrayInt.D1) variable.read();
        fileNLon = aid1.get(0);
        fileNLat = aid1.get(1);

        // get x_range
        variable = grdFile.findVariable("x_range");
        da = NcHelper.getNiceDoubleArray(variable, 0, -1);
        fileMinLon = da.get(0);
        fileMaxLon = da.get(1);

        // get y_range
        variable = grdFile.findVariable("y_range");
        da = NcHelper.getNiceDoubleArray(variable, 0, -1);
        fileMinLat = da.get(0);
        fileMaxLat = da.get(1);

        // get z_range
        variable = grdFile.findVariable("z_range");
        da = NcHelper.getNiceDoubleArray(variable, 0, -1);
        minData = da.get(0);
        maxData = da.get(1);

        if (verbose)
          String2.log(
              "  fileNLon="
                  + fileNLon
                  + " fileNLat="
                  + fileNLat
                  + "  fileMinLon="
                  + fileMinLon
                  + " fileMaxLon="
                  + fileMaxLon
                  + "  fileMinLat="
                  + fileMinLat
                  + " fileMaxLat="
                  + fileMaxLat
                  + "  fileLonSpacing="
                  + fileLonSpacing
                  + " fileLatSpacing="
                  + fileLatSpacing
                  + "  minData="
                  + minData
                  + " maxData="
                  + maxData);

        fileLon = DataHelper.getRegularArray(fileNLon, fileMinLon, fileLonSpacing);
        fileLat = DataHelper.getRegularArray(fileNLat, fileMinLat, fileLatSpacing);
      }

      // if desired were unknown, they can now be created
      if (Double.isNaN(desiredMinLon)) desiredMinLon = fileMinLon;
      if (Double.isNaN(desiredMaxLon)) desiredMaxLon = fileMaxLon;
      if (Double.isNaN(desiredMinLat)) desiredMinLat = fileMinLat;
      if (Double.isNaN(desiredMaxLat)) desiredMaxLat = fileMaxLat;

      double getMinLon = desiredMinLon;
      double getMaxLon = desiredMaxLon;
      double getMinLat = desiredMinLat;
      double getMaxLat = desiredMaxLat;

      // getMin/MaxLon are desiredMin/MaxLon adjusted to be in fileMin/MaxLon range.
      // need to get all lons if data and requests are different pm180.
      // Data request is often smaller than file,
      //  so think: is the desired data available?
      // !!This is complicated -- makes my head spin.
      //  Only do shift +-360 if data is all in one segment
      //    or unevenly spaced lon values will be created by makeLonPM180.
      //  Also, the file may be slightly <0 to slightly <360
      //     or slightly....
      //  But this is simpler than opendap code because I can just get
      //    all lon values, makeLonPM180 and THEN subset
      //    (and then there is no danger of unevenly spaced lons).
      // look for simple cases
      boolean gettingAllLons = false;
      if (getMaxLon < 0 && fileMaxLon > 180) {
        // desired is all low, so simple shift up will work
        getMinLon += 360;
        getMaxLon += 360;
      } else if (getMinLon > 180 && fileMinLon < 0) {
        // desired is all high, so simple shift down will work
        getMinLon -= 360;
        getMaxLon -= 360;
      } else if (getMinLon >= 0 && (getMaxLon <= 180 || fileMinLon >= 0)) {
        // all >= 0   (even if getMax > 180, there is no file data for it)
        // it will fail below
      } else if (getMaxLon <= 180 && (getMinLon >= 0 || fileMaxLon <= 180)) {
        // all <= 180 (even if getMin < 0, there is no file data for it)
        // it will fail below
      } else if (getMinLon <= fileMinLon && getMaxLon >= fileMaxLon) {
        // request and data are compatible
        // findXxx will succeed below
      } else if (getMinLon >= fileMinLon && getMaxLon <= fileMaxLon) {
        // request and data are compatible
        // findXxx will succeed below
      } else {
        // I give up. Just get all available lons.
        if (verbose) String2.log("  getting all lons!");
        gettingAllLons =
            true; // important sign of trouble. may need to align 2 sections of the data.
        getMinLon = fileMinLon;
        getMaxLon = fileMaxLon;
        // don't modify getNLon because it will still be applied to original desiredMin/MaxLon
      }

      // figure out lons and lats to get
      int getLonStart = DataHelper.binaryFindStartIndex(fileLon, getMinLon);
      int getLonEnd = DataHelper.binaryFindEndIndex(fileLon, getMaxLon);
      int getLatStart = DataHelper.binaryFindStartIndex(fileLat, getMinLat);
      int getLatEnd = DataHelper.binaryFindEndIndex(fileLat, getMaxLat);
      if (getLonStart < 0 || getLonEnd < 0 || getLatStart < 0 || getLatEnd < 0) {
        String2.log(
            "Failure: getMinLon="
                + getMinLon
                + "(i="
                + getLonStart
                + ") maxLon="
                + getMaxLon
                + "(i="
                + getLonEnd
                + ") getMinLat="
                + getMinLat
                + "(i="
                + getLatStart
                + ") maxLat="
                + getMaxLat
                + "(i="
                + getLatEnd
                + ")");

        Test.error(MustBe.THERE_IS_NO_DATA + " (out of range)");
      }
      getMinLat = fileLat[getLatStart];
      getMaxLat = fileLat[getLatEnd];

      // desiredNLon doesn't change because it is still used on original range (but in some cases it
      // could...)
      // but desiredNLat does change because file range may be less than desired range
      // and this is important optimization because it reduces the number of rows of data read
      int getNLon =
          DataHelper.adjustNPointsNeeded(
              desiredNLon, desiredMaxLon - desiredMinLon, getMaxLon - getMinLon);
      int getNLat =
          DataHelper.adjustNPointsNeeded(
              desiredNLat, desiredMaxLat - desiredMinLat, getMaxLat - getMinLat);
      if (verbose)
        String2.log(
            "  getMinLon="
                + getMinLon
                + " getMaxLon="
                + getMaxLon
                + " getNLon="
                + getNLon
                + "\n  getMinLat="
                + getMinLat
                + " getMaxLat="
                + getMaxLat
                + " getNLat="
                + getNLat);

      // calculate stride
      int lonStride =
          gettingAllLons
              ? 1
              : // 1 ensures that makeLonPM180 will be able to match up disparate lons
              DataHelper.findStride((getLonEnd - getLonStart) + 1, getNLon);
      int latStride = DataHelper.findStride((getLatEnd - getLatStart) + 1, getNLat);
      if (verbose) String2.log("  lonStride=" + lonStride + " latStride=" + latStride);

      lon = DataHelper.copy(fileLon, getLonStart, getLonEnd, lonStride);
      lat = DataHelper.copy(fileLat, getLatStart, getLatEnd, latStride);
      int nLon = lon.length;
      int nLat = lat.length;
      setLonLatSpacing();
      if (verbose) String2.log("  lonSpacing=" + lonSpacing + " latSpacing=" + latSpacing);

      long getInfoTime = System.currentTimeMillis() - time;

      // get raw data for desired lon and lat range
      data = new double[nLon * nLat]; // no need to set to NaN; it will all be filled
      variable = grdFile.findVariable("z");
      long readTime = System.currentTimeMillis();
      // Read lats backwards because highest lat row is at start of variable
      // and it is probably more efficient to read forward through the file,
      // but code below can handle lati in any order
      if (variable.getRank() == 2) {
        // gmt 4 file
        for (int lati = nLat - 1; lati >= 0; lati--) {
          // Read a row of data from z(y,x).
          int whichFileLat = getLatStart + lati * latStride;
          ArrayFloat.D2 afd2 =
              (ArrayFloat.D2)
                  variable.read( // sectionSpec
                      whichFileLat
                          + ":"
                          + whichFileLat
                          + ":1,"
                          + getLonStart
                          + ":"
                          + getLonEnd
                          + ":"
                          + lonStride);

          // store the data
          for (int loni = 0; loni < nLon; loni++) setData(loni, lati, afd2.get(0, loni));
        }
      } else {
        // gmt 3 file
        for (int lati = nLat - 1; lati >= 0; lati--) {
          // Read a row of data.
          // "(fileNLat-1)-" because values are stored with highest Lat row at
          //  the start of the variable.
          int whichFileLat = getLatStart + lati * latStride;
          // start position in array based on endLat since stored upside down
          int rowStart = ((fileNLat - 1) - whichFileLat) * fileNLon;
          ArrayFloat.D1 afd1 =
              (ArrayFloat.D1)
                  variable.read( // sectionSpec
                      (rowStart + getLonStart) + ":" + (rowStart + getLonEnd) + ":" + lonStride);

          // store the data
          for (int loni = 0; loni < nLon; loni++) setData(loni, lati, afd1.get(loni));
        }
      }
      readTime = System.currentTimeMillis() - readTime;
      if (verbose)
        String2.log(
            "  file portion read:" + "\n    " + lonInfoString() + "\n    " + latInfoString());

      // makeLonPM180 and subset (throws Exception if no data)
      makeLonPM180AndSubset(
          desiredMinLon, desiredMaxLon, getMinLat, getMaxLat, desiredNLon, getNLat);

      // if data has scale!=1 or offset!=0, deal with it     //what is attribute: node_offset?
      // Do it last so it is done to the fewest data points.
      Attribute scaleAtt = variable.findAttribute("scale_factor");
      double scale = scaleAtt == null ? 1 : NcHelper.getNiceDouble(scaleAtt);
      Attribute offsetAtt = variable.findAttribute("add_offset");
      double offset = scaleAtt == null ? 0 : NcHelper.getNiceDouble(offsetAtt);
      DataHelper.scale(data, scale, offset);

      if (verbose)
        String2.log(
            "  "
                + lonInfoString()
                + "\n  "
                + latInfoString()
                + "\n  data n="
                + data.length
                + " [0]="
                + (float) data[0]
                + "\n  "
                + fullFileName
                + "\n  Grid.readGrd done. getInfoTime="
                + getInfoTime
                + " readTime="
                + readTime
                + " TOTAL TIME="
                + (System.currentTimeMillis() - time)
                + "\n");

    } finally {
      try {
        if (grdFile != null) grdFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This calls makeLonPM180 and subset.
   *
   * @throws Exception e.g., if no data
   */
  public void makeLonPM180AndSubset(
      double desiredMinLon,
      double desiredMaxLon,
      double desiredMinLat,
      double desiredMaxLat,
      int desiredNLon,
      int desiredNLat)
      throws Exception {

    // This is done before subset() so that lon values will be evenly spaced
    // going into subset().
    makeLonPM180(DataHelper.lonNeedsToBePM180(desiredMinLon, desiredMaxLon));

    // avoid problems with subset() returning noData if nLon==1 and/or nLat==1
    // and match is closest, but not AE5
    if (lon.length == 1) {
      desiredMinLon = lon[0];
      desiredMaxLon = lon[0];
    }
    if (lat.length == 1) {
      desiredMinLat = lat[0];
      desiredMaxLat = lat[0];
    }

    // get desired subset (after makeLonPM180 so desired... are appropriate)
    if (!subset(
        desiredMinLon, desiredMaxLon, desiredMinLat, desiredMaxLat, desiredNLon, desiredNLat))
      Test.error(MustBe.THERE_IS_NO_DATA + " (after subset)");
  }

  /**
   * This reads a GMT .grd file and populates the public variables, EXCEPT data which is not
   * touched. NOT YET TESTED.
   *
   * @param fullFileName
   * @throws Exception if trouble
   */
  public void readGrdInfo(String fullFileName) throws Exception {

    if (verbose) String2.log("Grid.readGrdInfo");
    long time = System.currentTimeMillis();

    nValidPoints = Integer.MIN_VALUE;
    minData = Double.NaN;
    maxData = Double.NaN;

    // open the file (before 'try'); if it fails, no temp file to delete
    NetcdfFile grdFile = NcHelper.openFile(fullFileName);
    try {
      // if (verbose) String2.log(NcHelper.ncdump(fullFileName, "-h"));

      // get fileLonSpacing and fileLatSpacing
      Variable variable = grdFile.findVariable("spacing");
      DoubleArray da = NcHelper.getNiceDoubleArray(variable, 0, -1);
      double fileLonSpacing = da.get(0);
      double fileLatSpacing = da.get(1);
      if (Double.isNaN(fileLonSpacing)) // happens if only 1 value
      fileLonSpacing = 1;
      if (Double.isNaN(fileLatSpacing)) // happens if only 1 value
      fileLatSpacing = 1;
      if (verbose)
        String2.log("  fileLonSpacing=" + fileLonSpacing + " fileLatSpacing=" + fileLatSpacing);

      // get nLon and nLat
      variable = grdFile.findVariable("dimension");
      ArrayInt.D1 aid1 = (ArrayInt.D1) variable.read();
      int fileNLon = aid1.get(0);
      int fileNLat = aid1.get(1);
      if (verbose) String2.log("  fileNLon=" + fileNLon + " fileNLat=" + fileNLat);

      // get fileMinLon
      variable = grdFile.findVariable("x_range");
      da = NcHelper.getNiceDoubleArray(variable, 0, -1);
      double fileMinLon = da.get(0);
      double fileMaxLon = da.get(1);
      if (verbose) String2.log("  fileMinLon=" + fileMinLon + " fileMaxLon=" + fileMaxLon);

      // get fileMinLat
      variable = grdFile.findVariable("y_range");
      da = NcHelper.getNiceDoubleArray(variable, 0, -1);
      double fileMinLat = da.get(0);
      double fileMaxLat = da.get(1);
      if (verbose) String2.log("  fileMinLat=" + fileMinLat + " fileMaxLat=" + fileMaxLat);

      // get min/maxData
      variable = grdFile.findVariable("z_range");
      da = NcHelper.getNiceDoubleArray(variable, 0, -1);
      minData = da.get(0);
      maxData = da.get(1);
      if (verbose) String2.log("  minData=" + minData + " maxData=" + maxData);

      double fileLon[] = DataHelper.getRegularArray(fileNLon, fileMinLon, fileLonSpacing);
      double fileLat[] = DataHelper.getRegularArray(fileNLat, fileMinLat, fileLatSpacing);

      if (verbose)
        String2.log(
            "  "
                + lonInfoString()
                + "\n  "
                + latInfoString()
                + "\n  Grid.readGrdInfo done. TIME="
                + (System.currentTimeMillis() - time)
                + "\n");

    } finally {
      try {
        if (grdFile != null) grdFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This reads the data from an HDF file. This is not a very generalized reader, the data must be
   * in the standard CoastWatch format: with the first three HdfScientificData tags containing data,
   * lon, and lat, each with double or float data. This doesn't set minData, maxData, or
   * nValidObservations; use calculateStats to get these values.
   *
   * <p>This always reads all of the data and doesn't change it (e.g., makeLonPM180). Use
   * makeLonPM180 or subset afterwards to do those things.
   *
   * <p>This could/should set globalAttributes, latAttributes, lonAttributes, and dataAttributes,
   * but it currently doesn't.
   *
   * @param fullFileName
   * @param dfntType an HdfConstants dfntType (currently supported: DFNT_FLOAT32, DFNT_FLOAT64).
   *     Ideally, this procedure could detect this.
   * @throws Exception if trouble
   */
  public void readHDF(String fullFileName, int dfntType) throws Exception {
    if (verbose) String2.log("grid.readHDF(" + fullFileName + ")");

    // read the raw data
    SdsReader sdsReader = new SdsReader(fullFileName);

    // read backwards to get lat and lon before grid
    for (int count = 2; count >= 0; count--) {
      HdfScientificData sd = (HdfScientificData) sdsReader.sdList.get(count);
      double dar[];
      double tMissingValue = DataHelper.FAKE_MISSING_VALUE;
      if (dfntType == HdfConstants.DFNT_FLOAT32) {
        float tempValue = (float) tMissingValue;
        tMissingValue = tempValue; // to bruise it like bruised in far
        float[] far = SdsReader.toFloatArray(sd.data);
        int n = far.length;
        dar = new double[n];
        if (count > 0) {
          // don't make data nice
          for (int i = 0; i < n; i++) dar[i] = far[i];
        } else {
          // do make lat and lon nice
          for (int i = 0; i < n; i++) dar[i] = Math2.floatToDouble(far[i]);
        }
      } else if (dfntType == HdfConstants.DFNT_FLOAT64) {
        dar = SdsReader.toDoubleArray(sd.data);
      } else {
        throw new Exception("Grid.readHDF: Unsupported dfntType: " + dfntType + ".");
      }

      // String2.log("  count=" + count + " dar=" +
      // String2.noLongerThanDots(String2.toCSSVString(dar), 300));

      if (count == 0) {
        // save dar as data
        // look for otherMissingValue
        int otherMissingValue = -999;
        int n = dar.length;
        for (int i = 0; i < n; i++) {
          if (dar[i] == tMissingValue || dar[i] == otherMissingValue) dar[i] = Double.NaN;
        }

        // change the order of the data
        data = new double[dar.length];
        int nLat = lat.length;
        int nLon = lon.length;
        int po = 0;
        for (int tLat = nLat - 1; tLat >= 0; tLat--)
          for (int tLon = 0; tLon < nLon; tLon++) setData(tLon, tLat, dar[po++]);
      } else if (count == 1) {
        // save dar as lon
        lon = dar;
      } else if (count == 2) {
        // save dar as lat
        lat = dar;
      }
    }

    setLonLatSpacing();
  }

  /**
   * This is like readNetCDF with more parameters, but uses NaN or MAX_VALUE for all of the
   * desiredXxx parameters, and so reads all of the data.
   *
   * @param fullFileName
   * @param dataName is the name of the gridded data variable, or use null to read the first
   *     variable which uses the lat and lon dimensions.
   * @param makeLonPM180
   * @throws Exception if trouble (e.g., no data)
   */
  public void readNetCDF(String fullFileName, String dataName, boolean makeLonPM180)
      throws Exception {
    readNetCDF(
        fullFileName,
        dataName,
        makeLonPM180 ? -180 : 0,
        makeLonPM180 ? 180 : 360,
        -90,
        90,
        Integer.MAX_VALUE,
        Integer.MAX_VALUE);
  }

  /**
   * This is like readNetCDF with more parameters, but uses NaN or MAX_VALUE for all of the
   * desiredXxx parameters, and so reads all of the data.
   *
   * @param fullFileName
   * @param dataName is the name of the gridded data variable, or use null to read the first
   *     variable which uses the lat and lon dimensions.
   * @throws Exception if trouble (e.g., no data)
   */
  public void readNetCDF(String fullFileName, String dataName) throws Exception {
    readNetCDF(
        fullFileName,
        dataName,
        Double.NaN,
        Double.NaN,
        -90,
        90,
        Integer.MAX_VALUE,
        Integer.MAX_VALUE);
  }

  /**
   * This reads a NetCDF file and populates the public variables. This doesn't set minData, maxData,
   * or nValidPoints; use calculateStats to get these values. This is intended to work with COARDS
   * compatible files. [COARDS] refers to
   * https://ferret.pmel.noaa.gov/noaa_coop/coop_cdf_profile.html
   *
   * <p>The file must have a variable with attribute "units"="degrees_north" (or a variation) to
   * identify the latitude variable. See [COARDS] "Latitude Dimension". It can be of any numeric
   * data type. The attributes "scale_factor" and "add_offset" are supported.
   *
   * <p>The file must have a variable with attribute "units"="degrees_east" (or a variation) to
   * identify the longitude variable. See [COARDS] "Longitude Dimension". It can be of any numeric
   * data type. The attribues "scale_factor" and "add_offset" are supported.
   *
   * <p>The file must have a 2D variable with name=<gridName> to identify the grid variable. Files
   * may have multiple grids, so there is no automatic way to do it. It can be of any numeric data
   * type. The attribues "_FillValue", "missing_value", "scale_factor", and "add_offset" are
   * supported. The array must be a[lat][lon], or a[time][lat][lon] (only data for the time point if
   * read in). See [COARDS] "Order of Dimensions".
   *
   * <p>.nc files are read with code in netcdf-X.X.XX.jar which is part of the <a
   * href="https://www.unidata.ucar.edu/software/netcdf-java/">NetCDF Java Library</a> renamed as
   * netcdf-latest.jar. Put it in the classpath for the compiler and for Java.
   *
   * <p>This sets globalAttributes, latAttributes, lonAttributes, and dataAttributes.
   *
   * @param fullFileName
   * @param dataName is the name of the gridded data variable, or use null to read the first
   *     variable which uses the lat and lon dimensions.
   * @param desiredMinLon the minimum desired longitude. The resulting grid will conform to the
   *     desired min/maxLon, regardless of the underlying pm180 nature of the data in the file. Use
   *     -180 to 180 (or 0 to 360) for no restriction. Or use Double.NaN to get the file's
   *     desiredMinLon.
   * @param desiredMaxLon the maximum desired longitude
   * @param desiredMinLat the minimum desired latitude (use -90 for no restriction)
   * @param desiredMaxLat the maximum desired latitude (use 90 for no restriction)
   * @param desiredNLonPoints the desired minimum number of points in the x direction (use
   *     Integer.MAX_VALUE if no restriction).
   * @param desiredNLatPoints the desired minimum number of points in the y direction (use
   *     Integer.MAX_VALUE if no restriction).
   * @throws Exception if trouble (e.g., no data)
   */
  public void readNetCDF(
      String fullFileName,
      String dataName,
      double desiredMinLon,
      double desiredMaxLon,
      double desiredMinLat,
      double desiredMaxLat,
      int desiredNLonPoints,
      int desiredNLatPoints)
      throws Exception {
    // if (verbose) String2.log(File2.hexDump(fullFileName, 300));

    if (verbose) String2.log("Grid.readNetCDF " + dataName);
    if (desiredMinLon > desiredMaxLon)
      Test.error(
          String2.ERROR
              + " in Grid.readNetCDF:\n"
              + "minX ("
              + desiredMinLon
              + ") must be <= maxX ("
              + desiredMaxLon
              + ").");
    if (desiredMinLat > desiredMaxLat)
      Test.error(
          String2.ERROR
              + " in Grid.readNetCDF:\n"
              + "minY ("
              + desiredMinLat
              + ") must be <= maxY ("
              + desiredMaxLat
              + ").");

    nValidPoints = Integer.MIN_VALUE;
    minData = Double.NaN;
    maxData = Double.NaN;
    String errorInMethod = String2.ERROR + " in Grid.readNetCDF(" + fullFileName + "): ";

    // open the file (before 'try'); if it fails, no temp file to delete
    NetcdfFile ncFile = NcHelper.openFile(fullFileName);
    try {

      // *** ncdump
      // if (verbose) NcHelper.ncdump("Start of Grid.readNetCDF", fullFileName, "-h");

      long time = System.currentTimeMillis();
      Group rootGroup = ncFile.getRootGroup();

      // find the lat, lon, and data variables
      // [COARDS] says "A longitude coordinate variable is identifiable
      // from its units string, alone."
      List list = ncFile.getVariables();
      Variable latVariable = null, lonVariable = null;
      Index1D index0 = new Index1D(new int[] {1});
      index0.set(0);
      for (int i = 0; i < list.size(); i++) {
        Variable variable = (Variable) list.get(i);
        Attribute units = variable.findAttribute("units");
        if (units != null) {
          String value = units.getValues().getObject(index0).toString();
          if (latVariable == null
              && (value.equals("degrees_north")
                  || value.equals("degree_north")
                  || value.equals("degree_N")
                  || value.equals("degrees_N"))) latVariable = variable;
          if (lonVariable == null
              && (value.equals("degrees_east")
                  || value.equals("degree_east")
                  || value.equals("degree_E")
                  || value.equals("degrees_E"))) lonVariable = variable;
        }
      }
      Test.ensureNotNull(
          latVariable, errorInMethod + "no variable has units=degrees_north (or a variation).");
      Test.ensureNotNull(
          lonVariable, errorInMethod + "no variable has units=degrees_east (or a variation).");

      /*
      //get the time variable
      try {
          Variable timeVariable = ncFile.findVariable("time");
          Attributes timeAttributes = new Attributes();
          DataHelper.getNcVariableAttributes(timeVariable, timeAttributes);
          String2.log("time attributes=" + timeAttributes);

          PrimitiveArray pa = DataHelper.getPrimitiveArray(timeVariable.read());
          StringArray sa = new StringArray();
          for (int i = 0; i < pa.size(); i++)
              sa.add(Calendar2.epochSecondsToIsoStringTZ(pa.getDouble(i)));
          String2.log("time values=" + sa);

      } catch (Exception e) {
      }
      */
      // get the dataVariable
      Variable dataVariable = null;
      if (dataName == null) {
        // find first variable which uses lonVariable and latVariable
        List rootGroupVariables = rootGroup.getVariables();
        Dimension latDim = (Dimension) latVariable.getDimensions().get(0);
        Dimension lonDim = (Dimension) lonVariable.getDimensions().get(0);
        for (int var = 0; var < rootGroupVariables.size(); var++) {
          dataVariable = (Variable) rootGroupVariables.get(var);
          List tDimensions = dataVariable.getDimensions();
          int nDimensions = tDimensions.size();
          boolean latUsed = false;
          boolean lonUsed = false;
          for (int dim = 0; dim < nDimensions; dim++) {
            if (tDimensions.get(dim) == latDim) latUsed = true;
            if (tDimensions.get(dim) == lonDim) lonUsed = true;
          }
          if (latUsed && lonUsed) { // spaces are useful for GenerateThreddsXml
            if (verbose)
              String2.log("  Grid.readNetCDF grid variable found: " + dataVariable.getFullName());
            break;
          } else {
            dataVariable = null;
          }
        }
      } else {
        dataVariable = ncFile.findVariable(dataName);
      }
      Test.ensureNotNull(dataVariable, errorInMethod + "no variable has name=" + dataName + ".");

      // get file lat values (this works if unevenly spaced, and with float, double, ...)
      if (verbose)
        String2.log("  The file's lat values are " + latVariable.getDataType().toString());
      lat = NcHelper.getNiceDoubleArray(latVariable, 0, -1).toArray(); // this unbruises floats
      int nLat = lat.length;
      double minLat = lat[0];
      double maxLat = lat[nLat - 1];
      double fileLatSpacing = (maxLat - minLat) / (nLat - 1);
      if (verbose)
        String2.log(
            "  cleaned file lat min="
                + String2.genEFormat10(minLat)
                + " max="
                + String2.genEFormat10(maxLat)
                + " inc="
                + String2.genEFormat10(fileLatSpacing));

      // get file lon values (this works if unevenly spaced, and with float, double, ...)
      if (verbose)
        String2.log("  The file's lon values are " + lonVariable.getDataType().toString());
      lon = NcHelper.getNiceDoubleArray(lonVariable, 0, -1).toArray(); // this unbruises floats
      int nLon = lon.length;
      double minLon = lon[0];
      double maxLon = lon[nLon - 1];
      double fileLonSpacing = (maxLon - minLon) / (nLon - 1);
      if (verbose)
        String2.log(
            "  cleaned file lon min="
                + String2.genEFormat10(minLon)
                + " max="
                + String2.genEFormat10(maxLon)
                + " inc="
                + String2.genEFormat10(fileLonSpacing));

      /*{
          //what is largest and smallest change from one lat value to next?
          double rawLat[] = NcHelper.getPrimitiveArray(latVariable).toDoubleArray();
          double largest = -Double.MAX_VALUE;
          double smallest = Double.MAX_VALUE;
          int largestI = 1;
          int smallestI = 1;
          for (int i = 1; i < nLat; i++) {
              double d = rawLat[i] - rawLat[i-1];
              if (d > largest) {largest = d; largestI = i;}
              if (d < smallest) {smallest = d; smallestI = i;}
          }
          String2.log("  lat smallest delta=" + smallest + " i=" + smallestI +
              "\n    [i]=" + rawLat[smallestI] + " [i-1]=" + rawLat[smallestI-1] +
              "\n  lat largest  delta=" + largest  + " i=" + largestI  +
              "\n    [i]=" + rawLat[largestI]  + " [i-1]=" + rawLat[largestI-1]);

          //what is largest and smallest change from one lon value to next?
          double rawLon[] = NcHelper.getPrimitiveArray(lonVariable).toDoubleArray();
          largest = -Double.MAX_VALUE;
          smallest = Double.MAX_VALUE;
          largestI = 1;
          smallestI = 1;
          for (int i = 1; i < nLon; i++) {
              double d = rawLon[i] - rawLon[i-1];
              if (d > largest) {largest = d; largestI = i;}
              if (d < smallest) {smallest = d; smallestI = i;}
          }
          String2.log("  lon smallest delta=" + smallest + " i=" + smallestI +
              "\n    [i]=" + rawLon[smallestI] + " [i-1]=" + rawLon[smallestI-1] +
              "\n  lon largest  delta=" + largest  + " i=" + largestI  +
              "\n    [i]=" + rawLon[largestI]  + " [i-1]=" + rawLon[largestI-1]);
      }*/

      if (verbose) String2.log("  get info TIME=" + (System.currentTimeMillis() - time) + " ms");

      // read a subset of the data (just the rows and columns with the desired lat values)
      // binaryFindClosest is better than binaryFindStart binaryFindEnd because it gets
      //  all relevant data, even if a little beyond desired range.
      time = System.currentTimeMillis();
      if (Double.isNaN(desiredMinLon)) desiredMinLon = lon[0];
      if (Double.isNaN(desiredMaxLon)) desiredMaxLon = lon[nLon - 1];
      if (Double.isNaN(desiredMinLat)) desiredMinLat = lat[0];
      if (Double.isNaN(desiredMaxLat)) desiredMaxLat = lat[nLat - 1];
      int latStart = DataHelper.binaryFindStartIndex(lat, desiredMinLat);
      int latEnd = DataHelper.binaryFindEndIndex(lat, desiredMaxLat);
      if (latStart < 0 || latEnd < 0) {
        ncFile.close();
        Test.error(MustBe.THERE_IS_NO_DATA + " (out of latitude range)");
      }
      int lonStart = 0;
      int lonEnd = nLon - 1;
      if (verbose)
        String2.log(
            "  get indices lonStart="
                + lonStart
                + " lonEnd="
                + lonEnd
                + " latStart="
                + latStart
                + " latEnd="
                + latEnd);
      Array array;
      long readTime = System.currentTimeMillis();
      if (dataVariable.getRank() == 4) {
        // just read the data for the first time, first altitude
        array =
            dataVariable.read(
                new int[] {0, 0, latStart, lonStart}, // origin
                new int[] {1, 1, latEnd - latStart + 1, lonEnd - lonStart + 1}); // shape
        array = array.reshape(new int[] {latEnd - latStart + 1, lonEnd - lonStart + 1}); // shape
      } else if (dataVariable.getRank() == 3) {
        // just read the data for the first time
        array =
            dataVariable.read(
                new int[] {0, latStart, lonStart}, // origin
                new int[] {1, latEnd - latStart + 1, lonEnd - lonStart + 1}); // shape
        array = array.reshape(new int[] {latEnd - latStart + 1, lonEnd - lonStart + 1}); // shape
      } else { // read the 2D array
        array =
            dataVariable.read(
                new int[] {latStart, lonStart}, // origin
                new int[] {latEnd - latStart + 1, lonEnd - lonStart + 1}); // shape
      }
      readTime = System.currentTimeMillis() - readTime;

      // then create final lat
      lat = DataHelper.copy(lat, latStart, latEnd, 1);
      nLat = lat.length;
      minLat = lat[0];
      maxLat = lat[nLat - 1];

      // convert array into data
      // (column by column, left to right, bottom to top within the column)
      // (order discovered by trial and error)
      data = new double[nLon * nLat];
      int po = 0;
      // String2.log("  rank=" + array.getRank() + " size=" + array.getSize() +
      //    " shape=" + String2.toCSSVString(array.getShape()));

      // gather data in desired order
      // do directly so as not to waste memory which is precious here since array may be huge
      if (array instanceof ArrayDouble.D2 add2) {
        for (int tLon = 0; tLon < nLon; tLon++)
          for (int tLat = 0; tLat < nLat; tLat++)
            data[po++] =
                add2.get(
                    tLat,
                    tLon); // here and below were "nLat-1 - tLat"  when up side down in the .nc file
      } else if (array instanceof ArrayFloat.D2 afd2) {
        for (int tLon = 0; tLon < nLon; tLon++)
          for (int tLat = 0; tLat < nLat; tLat++) data[po++] = afd2.get(tLat, tLon);
      } else if (array instanceof ArrayLong.D2 ald2) {
        for (int tLon = 0; tLon < nLon; tLon++)
          for (int tLat = 0; tLat < nLat; tLat++) data[po++] = ald2.get(tLat, tLon);
      } else if (array instanceof ArrayInt.D2 aid2) {
        for (int tLon = 0; tLon < nLon; tLon++)
          for (int tLat = 0; tLat < nLat; tLat++) data[po++] = aid2.get(tLat, tLon);
      } else if (array instanceof ArrayShort.D2 asd2) {
        for (int tLon = 0; tLon < nLon; tLon++)
          for (int tLat = 0; tLat < nLat; tLat++) data[po++] = asd2.get(tLat, tLon);
      } else if (array instanceof ArrayByte.D2 abd2) {
        for (int tLon = 0; tLon < nLon; tLon++)
          for (int tLat = 0; tLat < nLat; tLat++) data[po++] = abd2.get(tLat, tLon);
      } else
        throw new RuntimeException(
            errorInMethod
                + "grid array is of unknown type: "
                + array
                + "\nrank="
                + array.getRank()
                + " size="
                + array.getSize()
                + " shape="
                + String2.toCSSVString(array.getShape()));
      Test.ensureEqual(po, data.length, "po");
      setLonLatSpacing();

      // makeLonPM180Subset (throws exception if no data)
      makeLonPM180AndSubset(
          desiredMinLon,
          desiredMaxLon,
          desiredMinLat,
          desiredMaxLat,
          desiredNLonPoints,
          desiredNLatPoints);

      // replace mv with NaN
      // treat mv as not exact until an mv is found (Lynn's files have bruised _FillValues
      double mv = Double.NaN;
      Attribute mvAtt =
          dataVariable.findAttribute("missing_value"); // "miising_value" is deprecated
      if (mvAtt != null) mv = mvAtt.getNumericValue().doubleValue();
      Attribute fvAtt = dataVariable.findAttribute("_FillValue");
      if (fvAtt != null) mv = fvAtt.getNumericValue().doubleValue();
      boolean mvIsExact = false;
      if (!Double.isNaN(mv)) {
        for (int i = data.length - 1; i >= 0; i--) {
          if (mvIsExact) {
            if (data[i] == mv) // a much faster test
            data[i] = Double.NaN;
          } else {
            if (Math2.almostEqual(5, data[i], mv)) {
              mvIsExact = true;
              mv = data[i];
              data[i] = Double.NaN;
            }
          }
        }
      }

      // deal with scale_factor and add_offset last, so it is done to the fewest values
      // if lat has scale!=1 or offset!=0, deal with it
      Attribute scaleAtt = latVariable.findAttribute("scale_factor");
      Attribute offsetAtt = latVariable.findAttribute("add_offset");
      double scale = scaleAtt == null ? 1 : NcHelper.getNiceDouble(scaleAtt);
      double offset = scaleAtt == null ? 0 : NcHelper.getNiceDouble(offsetAtt);
      DataHelper.scale(lat, scale, offset);

      // if lon has scale!=1 or offset!=0, deal with it
      scaleAtt = lonVariable.findAttribute("scale_factor");
      offsetAtt = lonVariable.findAttribute("add_offset");
      scale = scaleAtt == null ? 1 : NcHelper.getNiceDouble(scaleAtt);
      offset = scaleAtt == null ? 0 : NcHelper.getNiceDouble(offsetAtt);
      DataHelper.scale(lon, scale, offset);

      // if data has scale!=1 or offset!=0, deal with it
      scaleAtt = dataVariable.findAttribute("scale_factor");
      offsetAtt = dataVariable.findAttribute("add_offset");
      scale = scaleAtt == null ? 1 : NcHelper.getNiceDouble(scaleAtt);
      offset = offsetAtt == null ? 0 : NcHelper.getNiceDouble(offsetAtt);
      DataHelper.scale(data, scale, offset);

      // read the attributes
      NcHelper.getGroupAttributes(rootGroup, globalAttributes);
      NcHelper.getVariableAttributes(latVariable, latAttributes);
      NcHelper.getVariableAttributes(lonVariable, lonAttributes);
      NcHelper.getVariableAttributes(dataVariable, dataAttributes);

      if (verbose)
        String2.log(
            "  dataSize="
                + data.length
                + "\n  "
                + fullFileName
                + "\n  Grid.readNetCDF done. readTime="
                + readTime
                + ", Total TIME="
                + (System.currentTimeMillis() - time)
                + "\n");

    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This make the lon values be +-180 or 0..360. This rearranges the data in the grid, if
   * necessary.
   *
   * <p>NOTE that the rearranged lon values might not be evenly spaced (e.g., a 0 to 359.8 step 0.7
   * array may be rearranged to ... -1.6, -.9, -.2, 0, .7, 1.4 ...). (Info: SGT supports unevenly
   * spaced lon and lat values. It treats lat lon values as the center of the pixel, with the
   * 'pixel' extending to 1/2 way to the next pixel.)
   *
   * <p>If the original lat values aren't 0 - 360 (or -180 to 180), then rearranging initially
   * produces a big gap in the middle of the new lon values. This procedure adds lon values, spaced
   * lonSpacing apart, where needed to close the gap.
   *
   * <p>This method can use a lot of memory. If it needs more than Math2.maxSafeMemory, it throws an
   * exception, since Out-Of-Memory errors are serious and can get the jvm out of whack.
   *
   * @param pm180 If true, lon values are forced to be +-180. If false, lon values are forced to be
   *     0..360.
   * @throws Exception if trouble
   */
  public void makeLonPM180(boolean pm180) throws Exception {
    int nLon = lon.length;
    int nLat = lat.length;
    if (verbose) String2.log("Grid.makeLonPM180(" + pm180 + ") original grid." + lonInfoString());
    Test.ensureEqual(
        data.length, nLon * nLat, "data.length != nLon(" + nLon + ") * nLat(" + nLat + ")");

    double tLon[];
    if (pm180) {
      // make data -180 to 180

      // is data already compliant?
      if (Math2.lessThanAE(5, lon[nLon - 1], 180)) {
        if (verbose) String2.log("  done. already compliant\n");
        return;
      }

      // handle simple conversion of limited lon range
      if (Math2.greaterThanAE(5, lon[0], 180)) {
        for (int i = 0; i < nLon; i++) lon[i] -= 360;
        if (verbose) String2.log("  done. simple conversion\n");
        return;
      }

      // make a copy of lon with values in new range
      tLon = new double[nLon];
      System.arraycopy(lon, 0, tLon, 0, nLon);
      for (int i = 0; i < nLon; i++) if (tLon[i] >= 180) tLon[i] -= 360;

    } else {
      // make data 0..360

      // is data already compliant?
      if (Math2.greaterThanAE(5, lon[0], 0)) {
        if (verbose) String2.log("  done. already compliant\n");
        return;
      }

      // handle simple conversion of limited lon range
      if (Math2.lessThanAE(5, lon[nLon - 1], 0)) {
        for (int i = 0; i < nLon; i++) lon[i] += 360;
        if (verbose) String2.log("  done. simple conversion\n");
        return;
      }

      // make a copy of lon with values in new range
      tLon = new double[nLon];
      System.arraycopy(lon, 0, tLon, 0, nLon);
      for (int i = 0; i < nLon; i++) if (tLon[i] < 0) tLon[i] += 360;
    }

    // make new lon array
    // sort tLon and remove duplicates
    long time = System.currentTimeMillis();
    Arrays.sort(tLon);
    DoubleArray tLonAD = new DoubleArray(tLon);
    tLonAD.removeDuplicatesAE5();

    // insert values to fill gaps
    int ti = 1;
    double lonSpacing15 = 1.5 * lonSpacing;
    while (ti < tLonAD.size()) {
      if (tLonAD.get(ti) - tLonAD.get(ti - 1) > lonSpacing15)
        tLonAD.atInsert(ti, tLonAD.get(ti - 1) + lonSpacing);
      ti++;
    }
    // String2.log("  tLon after removeDuplicates: " + tLonAD.toString());
    tLon = tLonAD.toArray();
    int newNLon = tLon.length;
    tLonAD = null;

    // the tData object below can be huge, ensure there is enough memory
    Math2.ensureArraySizeOkay(newNLon * (long) nLat, "Grid.makeLonPM180");
    long tDataNBytes = newNLon * 8L * nLat; // calculation is done as longs
    Math2.ensureMemoryAvailable(tDataNBytes, "Grid.makeLonPM180"); // throws Exception if trouble

    // for each new tLon value, find which lon value matches it, and move that column's data to
    // tData
    double tData[] = new double[newNLon * nLat];
    int po = 0;
    for (int i = 0; i < newNLon; i++) {
      int which = Math2.binaryFindClosest(lon, tLon[i]);
      if (!Math2.almostEqual(5, lon[which], tLon[i])) {
        which = Math2.binaryFindClosest(lon, tLon[i] - 360);
        if (!Math2.almostEqual(5, lon[which], tLon[i] - 360)) {
          which = Math2.binaryFindClosest(lon, tLon[i] + 360);
          if (!Math2.almostEqual(5, lon[which], tLon[i] + 360)) {
            Arrays.fill(tData, po, po + nLat, Double.NaN);
            po += nLat;
            continue;
          }
        }
      }
      System.arraycopy(data, which * nLat, tData, po, nLat);
      po += nLat;
    }

    // swap new arrays into place
    lon = tLon;
    setLonLatSpacing();
    data = tData;
    if (verbose)
      String2.log(
          "  final "
              + lonInfoString()
              + "\n  makeLonPM180 done. rearranged data. TIME="
              + (System.currentTimeMillis() - time)
              + "\n");

    // leave this test in in an advisory role when I'm developing
    if (doExtraErrorChecking) {
      DataHelper.ensureEvenlySpaced(lon, "The lon values aren't evenly spaced:\n");
      DataHelper.ensureEvenlySpaced(lat, "The lat values aren't evenly spaced:\n");
    }
  }

  /*retired on 2007-01-09
      public void makeLonPM180(boolean pm180) throws Exception {
          int nLon = lon.length;
          int nLat = lat.length;
          String errorIn = String2.ERROR + " in Grid.makeLonPM180:\n";
          Test.ensureEqual(data.length, nLon * nLat,
              "data.length != nLon(" + nLon + ") * nLat(" + nLat + ")");

          if (pm180) {
              //is data already compliant?
              if (lon[nLon - 1] <= 180)
                  return;

              //handle simple conversion of limited lon range
              if (lon[0] >= 180 && lon[nLon - 1] <= 360) {
                  for (int i = 0; i < nLon; i++)
                      lon[i] -= 360;
                  return;
              }

              //handle conversion of (usually) whole-world data, from 0..360 to -180..180
              //by moving right ~half block to left and adjusting lon values.
              //Data is a 1D array, column by column, from the lower left (the way SGT wants it).
              Test.ensureTrue(lon[0] >= 0, errorIn + "lon[0] (" + lon[0] + ") must be >= 0.");
              if (Math2.almostEqual(5, lon[nLon - 1], 360))
                  lon[nLon - 1] = 360;
              Test.ensureTrue(lon[nLon - 1] <= 360, errorIn + "lon[nLon-1] (" + lon[nLon-1] + ") must be <= 360.");

              //If there is duplicate data for first and last lon (maybe more)
              //  (which will become 0 and 0, hence trouble)
              //  remove excess data
              int keepNLon = Math2.binaryFindFirstGAE(lon, lon[0] + 360, 5);
              if (keepNLon < nLon) {
                  //String2.log("Grid.makeLonPM180 lat range is -180 - 180!");
                  double tData[] = new double[nLat * keepNLon];
                  System.arraycopy(data, 0, tData, 0, nLat * keepNLon);
                  data = tData;

                  double tLon[] = new double[keepNLon];
                  System.arraycopy(lon, 0, tLon, 0, keepNLon);
                  lon = tLon;

                  nLon = keepNLon;
              }

              //expand to ~0..~360
              //Since there is data on each side of 180,
              //make sure data is ~0..~360, since resulting data
              //has to be -180 to 180 (draw it to see why this is so).
              if (lon[0] > 1 || lon[nLon - 1] <= 359) {
                  if (verbose) String2.log("grid.makeLonPM180 expand to 0..360:" +
                      " oldNLon=" + nLon +
                      " oldMinLon=" + lon[0] +
                      " oldMaxLon=" + lon[nLon - 1] +
                      " lonSpacing=" + lonSpacing);
                  int newNLon = Math2.roundToInt(360 / lonSpacing);

                  //calculate newLonMin //e.g. 100.25 % .5 -> .25
                  double newLonMin = lon[0] % lonSpacing;
                  double tLon[] = DataHelper.getRegularArray(newNLon, newLonMin, lonSpacing);
                  if (verbose) String2.log("grid.makeLonPM180 expand to 0..360:" +
                      " newNLon=" + newNLon +
                      " newMinLon=" + tLon[0] +
                      " newMaxLon=" + tLon[newNLon - 1]);
                  double tData[] = new double[nLat * newNLon];
                  Arrays.fill(tData, Double.NaN);
                  int destinationLonIndex = DataHelper.binaryFindClosestIndex(tLon, lon[0]);
                  if (destinationLonIndex == -1 || !Math2.almostEqual(5, tLon[destinationLonIndex], lon[0]))
                      Test.error(errorIn + "no suitable destinationLonIndex ([" +
                          destinationLonIndex + "]=" +
                          (destinationLonIndex == -1? "" : "" + tLon[destinationLonIndex]) +
                          " ?) for " + lon[0] +
                          " in " + tLon[0] + " to " + tLon[newNLon - 1] + " spacing=" + lonSpacing +
                          "\n  lon=" + String2.toCSSVString(tLon));
                  System.arraycopy(data, 0, tData, destinationLonIndex * nLat, nLat * nLon);
                  nLon = newNLon;
                  lon = tLon;
                  data = tData;
              }

              //move the right ~half block to the left
              int pivotCol = 0;
              while (pivotCol < nLon && lon[pivotCol] < 180)
                  pivotCol++;
              for (int i = pivotCol; i < nLon; i++)
                  lon[i] -= 360;
              DoubleArray da = new DoubleArray(lon);
              da.move(pivotCol, nLon, 0);
              da = new DoubleArray(data);
              da.move(pivotCol * nLat, nLon * nLat, 0);
              if (verbose) String2.log(
                  "grid.makeLonPM180 pm180=true move the right ~half block to the left:\n pivotCol=" +
                  pivotCol + " of nLon=" + nLon);

          } else {
              //make data 0..360

              //is data already compliant?
              if (lon[0] >= 0) {
                  return;
              }

              //handle simple conversion of limited lon range
              if (lon[0] >= -180 && lon[nLon - 1] <= 0) {
                  for (int i = 0; i < nLon; i++)
                      lon[i] += 360;
                  return;
              }

              //handle conversion of (usually) whole-world data, from -180..180 to 0..360
              //by moving right ~half block to left and adjusting lon values.
              //Data is a 1D array, column by column, from the lower left (the way SGT wants it).
              Test.ensureTrue(lon[0] >= -180, errorIn + "lon[0] (" + lon[0] + ") must be >= -180.");
              Test.ensureTrue(lon[nLon - 1] <= 180, errorIn + "lon[nLon-1] (" + lon[nLon-1] + ") must be <= 180.");

              //If there is duplicate data for first and last lon (maybe more)
              //  (which will become 0 and 0, hence trouble)
              //  remove excess data
              int keepNLon = Math2.binaryFindFirstGAE(lon, lon[0] + 360, 5);
              if (keepNLon < nLon) {
                  //String2.log("Grid.makeLonPM180 lat range is -180 - 180!");
                  double tData[] = new double[nLat * keepNLon];
                  System.arraycopy(data, 0, tData, 0, nLat * keepNLon);
                  data = tData;

                  double tLon[] = new double[keepNLon];
                  System.arraycopy(lon, 0, tLon, 0, keepNLon);
                  lon = tLon;

                  nLon = keepNLon;
              }

              //expand to ~-180..~180
              //Since there is data on each side of 0,
              //make sure the data is ~-180..~180, since resulting data
              //has to be 0 .. 360 (draw it to see why this is so).
              if (lon[0] > -179 || lon[nLon - 1] <= 179) {
                  if (verbose) String2.log("grid.makeLonPM180 expand to -180..180:" +
                      " oldNLon=" + nLon +
                      " oldMinLon=" + lon[0] +
                      " oldMaxLon=" + lon[nLon - 1] +
                      " lonSpacing=" + lonSpacing);
                  int newNLon = Math2.roundToInt(360 / lonSpacing);
                  //calculate newLonMin    e.g. 100.25 % .5 -> .25;
                  //draw it to see why  -180    +180  (+180 puts it in realm of 0..360,  -180 puts back to -180..180)
                  double newLonMin = -180 + ((lon[0] + 180) % lonSpacing);
                  double tLon[] = DataHelper.getRegularArray(newNLon, newLonMin, lonSpacing);
                  if (verbose) String2.log("grid.makeLonPM180 expand to -180..180:" +
                      " newNLon=" + newNLon +
                      " newMinLon=" + tLon[0] +
                      " newMaxLon=" + tLon[newNLon - 1]);
                  double tData[] = new double[nLat * newNLon];
                  Arrays.fill(tData, Double.NaN);
                  int destinationLonIndex = DataHelper.binaryFindClosestIndex(tLon, lon[0]);
                  if (destinationLonIndex == -1 || !Math2.almostEqual(5, tLon[destinationLonIndex], lon[0]))
                      Test.error(errorIn + "no suitable destinationLonIndex for " + lon[0] +
                          " in " + tLon[0] + " to " + tLon[newNLon - 1] + " spacing=" + lonSpacing);
                  System.arraycopy(data, 0, tData, destinationLonIndex * nLat, nLat * nLon);
                  nLon = newNLon;
                  lon = tLon;
                  data = tData;
              }

              //move the right ~half block to the left
              int pivotCol = 0;
              while (pivotCol < nLon && lon[pivotCol] < 0) {
                  lon[pivotCol] += 360;
                  pivotCol++;
              }
              DoubleArray da = new DoubleArray(lon);
              da.move(pivotCol, nLon, 0);
              da = new DoubleArray(data);
              da.move(pivotCol * nLat, nLon * nLat, 0);
              if (verbose) String2.log(
                  "grid.makeLonPM180 pm180=false move the right ~half block to the left:\n pivotCol=" +
                  pivotCol + " of nLon=" + nLon);
          }

      }
  */
  /**
   * This generates an array of contour levels from a String with a single interval value (which is
   * expanded into a series of values given minData and maxData) or a comma-separated list of values
   * (which is simply converted into a double[]).
   *
   * @param contourString
   * @param minData
   * @param maxData
   * @return an array of values (double[0] if trouble, including requests for &gt; 500
   *     contourLevels).
   */
  public static double[] generateContourLevels(
      String contourString, double minData, double maxData) {
    int MAX_NGOOD = 500;
    // is it a single value?
    if (contourString.indexOf(',') < 0) {
      // generate a csv list from the interval string
      double interval = Math.abs(String2.parseDouble(contourString));
      if (Double.isNaN(interval) || interval == 0) return new double[0];
      StringBuilder sb = new StringBuilder();
      int count = 0;
      double d = Math.rint(minData / interval) * interval;
      while (d < minData) d += interval;
      while (d <= maxData) {
        sb.append((sb.length() == 0 ? "" : ", ") + d);
        d += interval;
        if (count++ >= MAX_NGOOD) return new double[0];
      }
      contourString = sb.toString();
    }

    // convert the csv list
    double dar[] = String2.csvToDoubleArray(contourString);

    // remove values out of range
    double good[] = new double[Math.min(MAX_NGOOD, dar.length)];
    int nGood = 0;
    for (int i = 0; i < dar.length; i++) {
      double d = dar[i];
      if ((d >= minData) && (d <= maxData)) good[nGood++] = d;
      if (nGood >= MAX_NGOOD) return new double[0];
    }

    // copy to a final array
    double justGood[] = new double[nGood];
    System.arraycopy(good, 0, justGood, 0, nGood);
    return justGood;
  }

  /**
   * Get a data value given lon and lat index values. This does no checking of the validity of the
   * lon and lat values.
   *
   * @param lonIndex
   * @param latIndex
   * @return a double
   */
  public double getData(int lonIndex, int latIndex) {
    // data[] has data column by column, left to right, starting from lat0 lon0
    return data[lonIndex * lat.length + latIndex];
  }

  /**
   * Set a data value given lon and lat index values. This does no checking of the validity of the
   * lon and lat values.
   *
   * @param lonIndex
   * @param latIndex
   * @param datum
   */
  public void setData(int lonIndex, int latIndex, double datum) {
    // data has data column by column, left to right, starting from lat0 lon0
    data[lonIndex * lat.length + latIndex] = datum;
  }

  /**
   * This calculates and sets nValidPoints, minData, and maxData from the current data values. If
   * nValidPoints is 0, minData and maxData are set to Double.NaN.
   */
  public void calculateStats() {
    long time = System.currentTimeMillis();
    int n = data.length;
    minData = Double.MAX_VALUE; // any valid value will be smaller
    maxData = -Double.MAX_VALUE; // not Double.MIN_VALUE which ~= 0
    nValidPoints = 0;
    for (int i = 0; i < n; i++) {
      double d = data[i];
      if (!Double.isNaN(d)) {
        nValidPoints++;
        minData = Math.min(minData, d);
        maxData = Math.max(maxData, d);
      }
    }
    if (nValidPoints == 0) {
      minData = Double.NaN;
      maxData = Double.NaN;
    }
    // if (verbose) String2.log("Grid.calculateStats n=" + n + " time=" +
    //    (System.currentTimeMillis() - time));
  }

  /**
   * Subset this Grid. This does not revise minData, maxData, or nValidPoints. If result is no data,
   * then arrays will be length=0, but no Exceptions thrown.
   *
   * <p>This doesn't rearrange the data for lon pm180 vs 0 - 360 issues. So use makeLonPM180 before
   * calling this.
   *
   * @param desiredMinLon Use Double.NaN for min available. The data must already be in the
   *     appropriate range for this request.
   * @param desiredMaxLon Use Double.NaN for max available.
   * @param desiredMinLat Use Double.NaN for min available.
   * @param desiredMaxLat Use Double.NaN for max available.
   * @param desiredNLonPoints Use Integer.MAX_VALUE for max available. The result may have higher
   *     resolution. The result will have lower resolution if that is all that is available. If the
   *     range of data available is smaller than requested, this will be descreased proportionally.
   *     Use Integer.MAX_VALUE for maximum available resolution.
   * @param desiredNLatPoints Use Integer.MAX_VALUE for max available. (See desiredNLonPoints
   *     description.)
   * @return true if there is any data left
   * @throws Exception if trouble
   */
  public boolean subset(
      double desiredMinLon,
      double desiredMaxLon,
      double desiredMinLat,
      double desiredMaxLat,
      int desiredNLonPoints,
      int desiredNLatPoints)
      throws Exception {

    if (verbose)
      String2.log(
          "Grid.subset("
              + "minX="
              + String2.genEFormat10(desiredMinLon)
              + " maxX="
              + String2.genEFormat10(desiredMaxLon)
              + " minY="
              + String2.genEFormat10(desiredMinLat)
              + " maxY="
              + String2.genEFormat10(desiredMaxLat)
              + "\n    nLon="
              + desiredNLonPoints
              + " nLat="
              + desiredNLatPoints
              + ")"
              + "\n  initial "
              + lonInfoString()
              + "\n  initial "
              + latInfoString()
          // + "\n  stackTrace=" + MustBe.stackTrace()
          );

    // if lon or lat is not ascending, jump out
    // String2.log("lon=" + String2.toCSSVString(lon));
    // String2.log("lat=" + String2.toCSSVString(lat));
    if (lon.length > 1 && lon[0] > lon[1]) {
      String2.log("Grid.subset is doing nothing because lon is sorted in descending order.");
      return true;
    }
    if (lat.length > 1 && lat[0] > lat[1]) {
      String2.log("Grid.subset is doing nothing because lat is sorted in descending order.");
      return true;
    }

    long time = System.currentTimeMillis();

    // figure out what is needed
    // binaryFindStartIndex and binaryFindEndIndex use Math2.binaryFindClosest,
    //  which is better than findStart findEnd because it gets
    //  all relevant data (esp, tolerant of almostEqual),
    //  even if a little beyond desired range.
    int lonStart = DataHelper.binaryFindStartIndex(lon, desiredMinLon);
    int lonEnd = DataHelper.binaryFindEndIndex(lon, desiredMaxLon);
    int latStart = DataHelper.binaryFindStartIndex(lat, desiredMinLat);
    int latEnd = DataHelper.binaryFindEndIndex(lat, desiredMaxLat);
    if (lonStart < 0 || lonEnd < 0 || latStart < 0 || latEnd < 0) {
      if (verbose) String2.log("No Data.");
      noData();
      return false;
    }
    if (verbose)
      String2.log(
          "    minX["
              + lonStart
              + "]="
              + String2.genEFormat10(lon[lonStart])
              + " maxX["
              + lonEnd
              + "]="
              + String2.genEFormat10(lon[lonEnd])
              + "\n    minY["
              + latStart
              + "]="
              + String2.genEFormat10(lat[latStart])
              + " maxY["
              + latEnd
              + "]="
              + String2.genEFormat10(lat[latEnd]));

    // calculate stride   based on desired Min/Max Lon/Lat
    //  because actual range of data may be much smaller
    //  but still want stride as if for the whole large area
    // !!Anomaly creation depends on this logic exactly matching corresponding code in Opendap.
    int lonStride =
        DataHelper.findStride(lonSpacing, desiredMinLon, desiredMaxLon, desiredNLonPoints);
    int latStride =
        DataHelper.findStride(latSpacing, desiredMinLat, desiredMaxLat, desiredNLatPoints);

    // are we already done?
    if (lonStart == 0
        && latStart == 0
        && lonEnd == lon.length - 1
        && latEnd == lat.length - 1
        && lonStride == 1
        && latStride == 1) {
      if (verbose) String2.log("  Grid.subset done (no change)\n");
      return true; // use all the data
    }

    // gather data in desired order (column by column, left to right, bottom to top within the
    // column)
    double tLon[] = DataHelper.copy(lon, lonStart, lonEnd, lonStride);
    double tLat[] = DataHelper.copy(lat, latStart, latEnd, latStride);
    double tData[] = new double[tLon.length * tLat.length];
    int po = 0;
    for (int loni = lonStart; loni <= lonEnd; loni += lonStride) {
      for (int lati = latStart; lati <= latEnd; lati += latStride)
        tData[po++] = getData(loni, lati);
    }

    // update the related public values
    lon = tLon;
    lat = tLat;
    data = tData;
    setLonLatSpacing();
    if (verbose)
      String2.log(
          "  final "
              + lonInfoString()
              + "\n  final "
              + latInfoString()
              + "\n  Grid.subset done (rearranged). TIME="
              + (System.currentTimeMillis() - time)
              + "\n");
    return lon.length > 0 && lat.length > 0;
  }

  /**
   * Ensure there is data.
   *
   * @throws RuntimeException if no data
   */
  public void ensureThereIsData() {
    Test.ensureTrue(
        lat != null
            && lon != null
            && data != null
            && lat.length > 0
            && lon.length > 0
            && data.length > 0,
        MustBe.THERE_IS_NO_DATA + " (ensure)");
  }

  /**
   * Save this grid data as an ArcInfo ASCII grid format file that can be imported ArcGIS programs
   * -- lon values are forced to be within -180 to 180. This doesn't adjust attributes because .asc
   * doesn't store attributes.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".asc" will be
   *     added.
   * @throws Exception if trouble, notably if lonSpacing!=latSpacing
   */
  public void saveAsEsriASCII(String directory, String name) throws Exception {

    // for lon values to pm180
    makeLonPM180(true);

    // save as ascii
    saveAsASCII(
        directory,
        name,
        true,
        "" + DataHelper.FAKE_MISSING_VALUE); // esri likes fromTop=true and -9999 for mv
  }

  /**
   * This is an alias for saveAsAscii(directory, name, true, "-9999999");
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".asc" will be
   *     added.
   * @throws Exception if trouble, notably if lonSpacing!=latSpacing
   */
  public void saveAsASCII(String directory, String name) throws Exception {
    saveAsASCII(directory, name, true, "" + DataHelper.FAKE_MISSING_VALUE); // 12/2006 was "-9999"
  }

  /**
   * Save this grid data as an ASCII grid format file, with lon values as is. This writes the lon
   * values as they are currently in this grid (e.g., +-180 or 0..360). This overwrites any existing
   * file of the specified name. This makes an effort not to create a partial file if there is an
   * error. If no exception is thrown, the file was successfully created. See
   * https://en.wikipedia.org/wiki/Esri_grid for info about this format. **Currently, the values are
   * written as floats.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".asc" will be
   *     added.
   * @param fromTop write the data in rows, starting at the highest lat (vs the lowest lat). ArcInfo
   *     likes fromTop = true;
   * @param NaNString is the String to write for NaN's. ArcInfo doesn't like "NaN", and uses "-9999"
   *     as the suggested value.
   * @throws Exception if trouble, notably if lonSpacing!=latSpacing
   */
  public void saveAsASCII(String directory, String name, boolean fromTop, String NaNString)
      throws Exception {

    if (verbose) String2.log("Grid.saveAsASCII " + name);
    long time = System.currentTimeMillis();

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS[SAVE_AS_ASCII]; // fortunately same as ESRI_ASCII
    File2.delete(directory + name + ext);

    // make sure there is data
    ensureThereIsData();

    // ensure lat and lon values are evenly spaced
    // (they may not be and this format doesn't store values, just first and spacing)
    DataHelper.ensureEvenlySpaced(
        lon,
        String2.ERROR
            + " in Grid.saveAsASCII:\n"
            + "The longitude values aren't evenly spaced (as required by ESRI's .asc format):\n");
    DataHelper.ensureEvenlySpaced(
        lat,
        String2.ERROR
            + " in Grid.saveAsASCII:\n"
            + "The latitude values aren't evenly spaced (as required by ESRI's .asc format):\n");

    // ensure latSpacing=lonSpacing since ESRI .asc requires it
    // (only 1 "cellsize" parameter in header)
    setLonLatSpacing(); // double check that they are set nicely

    // for almostEqual(3, lonSpacing, latSpacing) DON'T GO BELOW 3!!!
    // For example: PHssta has 4096 lon points so spacing is ~.0878
    // But .0878 * 4096 = 359.6
    // and .0879 * 4096 = 360.0    (just beyond extreme test of 3 digit match)
    // That is unacceptable. So 2 would be abominable.  Even 3 is stretching the limits.
    if (!Math2.almostEqual(3, lonSpacing, latSpacing))
      Test.ensureEqual(
          lonSpacing,
          latSpacing,
          String2.ERROR
              + " in Grid.saveAsASCII:\n"
              + "ESRI's .asc format requires that the longitude spacing equal the latitude spacing.");

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the temp file
    BufferedWriter bufferedWriter = File2.getBufferedFileWriter88591(directory + randomInt);
    try {
      // write the data
      bufferedWriter.write("ncols " + lon.length + "\n");
      bufferedWriter.write("nrows " + lat.length + "\n");
      bufferedWriter.write(
          "xllcenter "
              + lon[0]
              + "\n"); // as precisely as possible; was xllcorner, but xllcenter is correct
      bufferedWriter.write("yllcenter " + lat[0] + "\n");
      // ArcGIS forces cellsize to be square; see test above
      bufferedWriter.write("cellsize " + latSpacing + "\n");
      bufferedWriter.write("nodata_value " + NaNString + "\n");
      if (fromTop) {
        // write values from row to row, top to bottom
        int last = lon.length - 1;
        for (int tLat = lat.length - 1; tLat >= 0; tLat--) {
          float f;
          for (int tLon = 0; tLon < last; tLon++) {
            f = (float) getData(tLon, tLat);
            bufferedWriter.write(Float.isNaN(f) ? NaNString + ' ' : f + " ");
          }
          f = (float) getData(last, tLat);
          bufferedWriter.write(Float.isNaN(f) ? NaNString + '\n' : f + "\n");
        }
      } else {
        // write values from row to row, bottom to top
        int nLat = lat.length;
        int last = lon.length - 1;
        for (int tLat = 0; tLat < nLat; tLat++) {
          float f;
          for (int tLon = 0; tLon < last; tLon++) {
            f = (float) getData(tLon, tLat);
            bufferedWriter.write(Float.isNaN(f) ? NaNString + ' ' : f + " ");
          }
          f = (float) getData(last, tLat);
          bufferedWriter.write(Float.isNaN(f) ? NaNString + '\n' : f + "\n");
        }
      }
    } finally {
      bufferedWriter.close();
    }

    // rename the file to the specified name
    File2.rename(directory, randomInt + "", name + ext);

    // diagnostic
    if (false) {
      String[] rff = File2.readFromFile88591(directory + name + ext);
      if (rff[0].length() > 0) throw new Exception(String2.ERROR + ":\n" + rff[0]);
      String2.log(
          "grid.saveAsASCII: "
              + directory
              + name
              + ext
              + " contains:\n"
              + String2.annotatedString(rff[1].substring(0, Math.min(rff[1].length(), 1000))));
    }

    if (verbose)
      String2.log("  Grid.saveAsASCII done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /**
   * This saves the current grid (which must have .nc-compatible attributes) as a grayscale GeoTIFF
   * file. This calls makeLonPM180 to ensure the data lons are +/-180 (which the GeotiffWriter
   * requires because ESRI only accepts that range).
   *
   * <p>Grayscale GeoTIFFs may not be very colorful, but they have an advantage over color GeoTIFFs:
   * the clear correspondence of the gray level of each pixel (0 - 255) to the original data allows
   * programs to reconstruct the original data values, something that is not possible with color
   * GeoTIFFS.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".tif" will be
   *     added to create the output file name.
   * @param dataName The name for the data variable (e.g., ATssta).
   * @throws Exception
   */
  public void saveAsGeotiff(String directory, String name, String dataName) throws Exception {

    if (verbose) String2.log("Grid.saveAsGeotiff " + name);
    long time = System.currentTimeMillis();

    // foce to be pm180
    makeLonPM180(true);

    // save as .nc file first
    saveAsNetCDF(directory, name, dataName);

    // attempt via java netcdf libraries
    GeotiffWriter writer = new GeotiffWriter(directory + name + ".tif");
    try {
      // 2013-08-28 new code to deal with GeotiffWritter in netcdf-java 4.3+
      GridDataset gridDataset = GridDataset.open(directory + name + ".nc");
      java.util.List grids = gridDataset.getGrids();
      // if (grids.size() == 0) ...
      GeoGrid geoGrid = (GeoGrid) grids.get(0);
      Array dataArray = geoGrid.readDataSlice(-1, -1, -1, -1); // get all
      writer.writeGrid(gridDataset, geoGrid, dataArray, true); // true=grayscale

      // 2013-08-28 pre 4.3.16, it was
      // LatLonRect latLonRect = new LatLonRect(
      //    new LatLonPointImpl(lat[0], lon[0]),
      //    new LatLonPointImpl(lat[lat.length - 1], lon[lon.length - 1]));
      // writer.writeGrid(directory + name + ".nc", dataName, 0, 0,
      //    true, //true=grayscale   color didn't work for me. and see javadocs above.
      //    latLonRect);
    } finally {
      writer.close();
    }

    if (verbose)
      String2.log("  Grid.saveAsGeotiff done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /*  //attempt based on gdal
  public void saveAsGeoTIFF(String directory, String name) throws Exception {
      //POLICY: because this procedure may be used in more than one thread,
      //do work on unique temp files names using randomInt, then rename to proper file name.
      //If procedure fails half way through, there won't be a half-finished file.
      int randomInt = Math2.random(Integer.MAX_VALUE));

      //delete any existing file
      String ext = SAVE_AS_EXTENSIONS[SAVE_AS_GEOTIFF];
      File2.delete(directory + name + ext);

      //make sure there is data
      ensureThereIsData();

      //save as grd first
      //saveAsGrd(directory, name);
      saveAsASCII(directory, name);

      //rename it as .nc
      //File2.rename(directory, name + ".grd", name + ".nc");

      //make the temp tiff file   (gdal is part of FWTools)
      //SSR.dosOrCShell("gdal_translate -ot Float32 -of GTiff " + directory + name + ".grd " +
      SSR.dosOrCShell("gdal_translate -of GTiff " + directory + name + ".asc " +
          directory + randomInt + ext, 60);

      //delete the grd file
      File2.delete(directory + name + ".grd");   //*** or .asc?

      //and rename to <name>.tif
      File2.rename(directory, randomInt + ext, name + ext);
  } */

  /**
   * Save this grid data as a .grd file. This writes the lon values as they are currently in this
   * grid (e.g., +-180 or 0..360). This ignores existing attributes and sets attributes on its own.
   * This overwrites any existing file of the specified name. This makes an effort not to create a
   * partial file if there is an error. If no exception is thrown, the file was successfully
   * created. The values are written as floats. !!!THE LON VALUES MUST BE EVENLY SPACED, or this
   * will throw an exception.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".grd" will be
   *     added.
   * @throws Exception
   */
  public void saveAsGrd(String directory, String name) throws Exception {

    if (verbose) String2.log("Grid.saveAsGrd " + directory + "\n  " + name + ".grd");
    long time = System.currentTimeMillis();

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS[SAVE_AS_GRD];
    File2.delete(directory + name + ext);

    // make sure there is data
    ensureThereIsData();

    // ensure the lat and lon values are evenly spaced
    DataHelper.ensureEvenlySpaced(
        lon,
        String2.ERROR
            + " in Grid.saveAsGrid("
            + name
            + "):\n"
            + "The lon values aren't evenly spaced:\n");
    DataHelper.ensureEvenlySpaced(
        lat,
        String2.ERROR
            + " in Grid.saveAsGrid("
            + name
            + "):\n"
            + "The lat values aren't evenly spaced:\n");

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // calculateStats so minData and maxData are correct
    calculateStats();

    // write the data
    // items determined by looking at a .grd file; items written in that order
    NetcdfFormatWriter ncWriter = null;
    boolean nc3Mode = true;
    boolean success = false;
    try {
      NetcdfFormatWriter.Builder grd = NetcdfFormatWriter.createNewNetcdf3(directory + randomInt);
      Group.Builder rootGroup = grd.getRootGroup();
      grd.setFill(false);
      int nLat = lat.length;
      int nLon = lon.length;

      Dimension sideDimension = NcHelper.addDimension(rootGroup, "side", 2);
      Dimension xysizeDimension = NcHelper.addDimension(rootGroup, "xysize", nLon * nLat);
      List<Dimension> sideDimList = Arrays.asList(sideDimension);
      List<Dimension> xysizeDimList = Arrays.asList(xysizeDimension);

      ArrayDouble.D1 x_range = new ArrayDouble.D1(2);
      x_range.set(0, lon[0]);
      x_range.set(1, lon[nLon - 1]);
      Variable.Builder xRangeVar =
          NcHelper.addVariable(rootGroup, "x_range", DataType.DOUBLE, sideDimList);

      ArrayDouble.D1 y_range = new ArrayDouble.D1(2);
      y_range.set(0, lat[0]);
      y_range.set(1, lat[nLat - 1]);
      Variable.Builder yRangeVar =
          NcHelper.addVariable(rootGroup, "y_range", DataType.DOUBLE, sideDimList);

      ArrayDouble.D1 z_range = new ArrayDouble.D1(2);
      z_range.set(0, minData);
      z_range.set(1, maxData);
      Variable.Builder zRangeVar =
          NcHelper.addVariable(rootGroup, "z_range", DataType.DOUBLE, sideDimList);

      ArrayDouble.D1 spacing = new ArrayDouble.D1(2);
      spacing.set(
          0, nLon <= 1 ? Double.NaN : lonSpacing); // if not really know, grd seems to use NaN
      spacing.set(1, nLat <= 1 ? Double.NaN : latSpacing);
      Variable.Builder spacingVar =
          NcHelper.addVariable(rootGroup, "spacing", DataType.DOUBLE, sideDimList);

      ArrayInt.D1 dimension = new ArrayInt.D1(2, false); // isUnsigned
      dimension.set(0, lon.length);
      dimension.set(1, lat.length);
      Variable.Builder dimensionVar =
          NcHelper.addVariable(rootGroup, "dimension", DataType.INT, sideDimList);

      // write values from left to right, starting with the top row
      ArrayFloat.D1 tData = new ArrayFloat.D1(nLon * nLat);
      int po = 0;
      for (int tLat = nLat - 1; tLat >= 0; tLat--)
        for (int tLon = 0; tLon < nLon; tLon++) tData.set(po++, (float) getData(tLon, tLat));
      Variable.Builder zVar =
          NcHelper.addVariable(
              rootGroup, "z", DataType.FLOAT, xysizeDimList); // grd files use "z" for the data

      xRangeVar.addAttribute(new Attribute("units", "user_x_unit"));
      yRangeVar.addAttribute(new Attribute("units", "user_y_unit"));
      zRangeVar.addAttribute(new Attribute("units", "user_z_unit"));
      zVar.addAttribute(new Attribute("scale_factor", Double.valueOf(1.0)));
      zVar.addAttribute(new Attribute("add_offset", Double.valueOf(0.0)));
      zVar.addAttribute(new Attribute("node_offset", Integer.valueOf(0)));

      rootGroup.addAttribute(new Attribute("title", ""));
      rootGroup.addAttribute(new Attribute("source", "CoastWatch West Coast Node"));

      // leave "define" mode
      ncWriter = grd.build();

      // then add data  (about 70% of time is here; so hard to speed up)
      long tTime = System.currentTimeMillis();
      ncWriter.write(xRangeVar.getFullName(), x_range);
      ncWriter.write(yRangeVar.getFullName(), y_range);
      ncWriter.write(zRangeVar.getFullName(), z_range);
      ncWriter.write(spacingVar.getFullName(), spacing);
      ncWriter.write(dimensionVar.getFullName(), dimension);
      ncWriter.write(zVar.getFullName(), tData);

      // make sure the file is closed
      ncWriter.close();
      ncWriter = null;

      String2.log("  write data time=" + (System.currentTimeMillis() - tTime));

      // rename the file to the specified name
      File2.rename(directory, randomInt + "", name + ext);

    } catch (Exception e) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(directory + randomInt);
        ncWriter = null;
      }
      throw e;
    }

    if (verbose)
      String2.log("  Grid.saveAsGrd done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /**
   * This saves the data in a Grid as an HDF file with whatever attributes are currently set (use
   * GridDataSet.setAttributes) This method is self-contained; it does not require external lookup
   * tables with data, but instead uses fileNameUtility to get information from the .grd file name.
   *
   * <p>The data is stored in the hdf file as doubles. Missing values are stored as
   * DataHelper.FAKE_MISSING_VALUE, not NaN.
   *
   * <p>This calls setStatsAttributes(true).
   *
   * <p>This relies on a pure Java implemtation, not NCSA's libraries. See comments in
   * hdf/SdsWriter.java for reasoning.
   *
   * <p>See documentation that comes with CoastWatch Utilities e.g.,
   * c:/programs/cwutilities/doc/Metadata.html.
   *
   * <p>This method is vastly faster (9 s web page time for SST 1km) than Dave's script (69 s). And
   * I think the Matlab approach is odd/flawed because it makes 2D lat and lon arrays, which appear
   * as datasets in CDAT.
   *
   * <p>The grid needs to hold gridded data with longitude +-180. (If not +-180, the data won't be
   * in the right place for CDAT to display coastline and political lines.) The full range and
   * resolution of the grid data will be saved in the hdf file.
   *
   * @param hdfFileName the full name for the new hdf file, but without the .hdf extension (it will
   *     be added by this method). Because fileNameUtility is used to generate the metadata based on
   *     the file name, the file name must follow the CWBrowser file naming convention (see
   *     FileNameUtility).
   * @param variableName the name to assign to the variable, usually the 6Name
   * @throws Exception if trouble. If there is trouble, there should be no partially created .hdf
   *     file.
   */
  public void saveAsHDF(String hdfFileName, String variableName) throws Exception {

    if (verbose) String2.log("Grid.saveAsHDF " + hdfFileName);
    long time = System.currentTimeMillis();
    File2.delete(hdfFileName + ".hdf");

    // make sure there is data
    ensureThereIsData();

    try {
      String errorIn = String2.ERROR + " in Grid.saveAsHDF: ";

      // gather the data
      // A new array is needed because of different order and different type.
      int nLon = lon.length;
      int nLat = lat.length;
      int nData = data.length;
      double tData[] = new double[nData];
      int po = 0;
      for (int tLat = 0; tLat < nLat; tLat++) {
        for (int tLon = 0; tLon < nLon; tLon++) {
          double d = getData(tLon, nLat - 1 - tLat); // up side down
          tData[po++] = Double.isNaN(d) ? DataHelper.FAKE_MISSING_VALUE : d;
        }
      }
      String2.log("globalAttributes=" + globalAttributes.toString());
      String2.log("et_affine=" + globalAttributes.get("et_affine"));

      // set the attributes
      setStatsAttributes(true); // save as double

      String name = File2.getNameAndExtension(hdfFileName);

      // create the file
      SdsWriter.create(
          hdfFileName + ".hdf", // must be the correct name, since it is stored in the file
          new double[][] {
            lon, lat
          }, // order is important and tied to desired order of data in array
          new String[] {"Longitude", "Latitude"},
          new Attributes[] {lonAttributes, latAttributes},
          tData,
          variableName,
          dataAttributes,
          globalAttributes);
    } catch (Exception e) {
      File2.delete(hdfFileName + ".hdf");
      throw e;
    }

    if (verbose)
      String2.log("  Grid.saveAsHDF done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /**
   * This tests saveAsHDF.
   *
   * @throws Exception if trouble
   */
  // public static void miniTestSaveAsHDF() {
  // test hdf    test in CoastWatch Utilities CDAT (it has the required metadata)
  // also use NCSA HDSView 2.2 to view the file
  // 7/14/05 this file was tested by (now) BobSimons2.00@gmail.com in CDAT
  //    String2.log("\n***** Grid.main miniJavaTestSaveAsHDF");
  //    time = System.currentTimeMillis();
  // miniJavaTestSaveAsHDF();  //different test than other files since more complicated to set up
  //    String2.log("Grid.miniJavaTestSaveAsHDF time=" + (System.currentTimeMillis() - time));
  // }

  /**
   * THIS WORKS BUT IS NOT CURRENTLY USED BECAUSE saveAsHDF IS BETTER SINCE IT DOESN'T NEED LINUX,
   * SHELL SCRIPTS, OR MATLAB. This saves the current grid data as a CoastWatch Utilities-compatible
   * HDF ver 4 file. Currently, this relies on several files in "c:/u00/chump/"
   * ("chump_make_grd2hdf", "lookup_data_id.m", "lookup_data_source.m") which rely on Matlab.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".hdf" will be
   *     added.
   * @param varName the name to use for the variable (e.g., ux10).
   * @throws Exception
   */
  public void saveAsHDFViaMatlab(String directory, String name, String varName) throws Exception {
    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS[SAVE_AS_HDF];
    File2.delete(directory + name + ext);

    // make sure there is data
    ensureThereIsData();

    // /* //old way
    // save as grd first
    saveAsGrd(directory, name);

    // MakeHdf via Matlab script
    // see copies of the files in <context>WEB-INF/ref/
    SSR.cShell(
        "/u00/chump/grd2hdf "
            + directory
            + name
            + ".grd "
            + directory
            + randomInt
            + ext
            + " "
            + varName,
        240);

    // delete the grd file
    File2.delete(directory + name + ".grd");

    // rename to final name
    File2.rename(directory, randomInt + ext, name + ext);
    // */

    /* //new way
    //save as xyz first
    saveAsXYZ(directory, name);

    //create matlab control script
    StringBuilder matlabControl = new StringBuilder();
    String binDir = "/u00/chump";
    matlabControl.append("path(path,'" + binDir + "')\n");
    matlabControl.append("cd " + directory + "\n");
    matlabControl.append("chump_make_grd2hdf(" +
        "'" + directory + name + ".xyz'," +
        "'" + directory + name + "_" + randomInt + ".hdf'," +
        "'" + varName + "'," + lonSpacing + "," + latSpacing + ")\n");
    matlabControl.append("quit\n");
    String error = File2.writeToFile88591(directory + randomInt + ".control", matlabControl.toString());
    if (error.length() > 0)
        throw new RuntimeException(error);

    //execute matlabControl
    String2.log("HDF matlabControl:\n" + matlabControl);
    SSR.runMatlab(directory + randomInt + ".control", directory + randomInt + ".out");
    String[] results = File2.readFromFile(directory + randomInt + ".out");
    String2.log("Grid.saveAsHDF matlabOutput:\n" + results[1] + "\n[end]");
    //File2.delete(directory + randomInt + ".control");
    //File2.delete(directory + randomInt + ".out");

    //delete the xyz file
    File2.delete(directory + name + ".xyz");

    //rename to final name
    File2.rename(directory, name + "_" + randomInt + ".hdf", name + ".hdf");
    // */

  }

  // private static Object saveAsHDFLock = Calendar2.newGCalendarLocal();

  /**
   * THIS METHOD WORKS ON COMPUTERS WHERE THE HDFLIBRARIES ARE INSTALLED AND ARE COMPATIBLE THE THE
   * CURRENT VERSION OF JAVA. (WHAT A PAIN!!!) USE saveAsHDF (A PURE JAVA VERSION) INSTEAD. My
   * mother said, "if you don't have anything nice to say, don't say anything at all".
   *
   * <p>This saves the data in a Grid as an HDF file with the metadata required by CDAT (in
   * CoastWatch Utilities). This method is self-contained; it does not require external lookup
   * tables with data, nor does it get information from the .grd file name.
   *
   * <p>This uses jhdf.jar which must be in the File2.webInfParentDirectory()+"WEB-INF\lib"
   * directory (for Tomcat) and on the javac's and java's classpath (for compiling and running
   * outside of Tomcat).
   *
   * <p>A native HDFLibrary must be installed for ncsa.hdf.hdflib.HDFLibrary to function within this
   * program. The easiest way to install it is to download and install HDFView from:
   * http://hdf.ncsa.uiuc.edu/hdf-java-html/hdfview/. See the JavaDocs for the HDFLibrary at
   * http://hdf.ncsa.uiuc.edu/hdf-java-html/javadocs/ncsa/hdf/hdflib/HDFLibrary.html.
   *
   * <p>See documentation that comes with CoastWatch Utilities e.g.,
   * c:/programs/cwutilities/doc/Metadata.html.
   *
   * <p>This is modified from Dave Foley's Matlab script chump_make_grd2hdf.m 13 Jan 2003, which was
   * modified 26 May 2005 by Luke Spence. This method is vastly faster (9 s web page time for SST
   * 1km) than Dave's script (69 s).
   *
   * <p>See also the Matlab (programming) book: "Exporting MATLAB Data to an HDF4 File", pg 6-94,
   * which had clues about how to make this work.
   *
   * <p>The grid needs to hold gridded data with longitude +-180. (If not +-180, the data won't be
   * in the right place for CDAT to display coastline and political lines.) The full range and
   * resolution of the grid data will be saved in the hdf file.
   *
   * @param hdfFileName the full name for the new hdf file
   * @param varName the name of the data variable (e.g., "sea surface temperature")
   * @param satellite the name of the satellite (e.g., "NOAA GOES spacecraft")
   * @param sensor the name of the sensor (e.g., "GOES SST")
   * @param origin the origin of the data (e.g., "NOAA/NESDIS")
   * @param startDate the start date/time (time should be 00:00:00 for composites). Note that this
   *     is a simple minded start date: where y-m-dTh:m:s are right even though a local time zone
   *     may be used. This procedure will interpret the day,hour as UTC time.
   * @param centeredTime the centered time (e.g., 2006-06-04 12:00:00 for composites). Note that
   *     this is a simple minded end date: where y-m-dTh:m:s are right even though a local time zone
   *     may be used. This procedure will interpret the day,hour as UTC time.
   * @param passType is "day" (e.g., for "sstd"), "night" (e.g., for "sstn"), or "day/night" (for
   *     all others)
   * @param dataUnits (e.g., "degrees Celsius")
   * @param dataFractionDigits digits to right of decimal place, for formatting data (e.g., 3)
   * @param latLonFractionDigits digits to right of decimal place, for formatting data (e.g., 3)
   * @throws Exception if trouble
   */
  /*    public void saveAsHDFViaNCSA(String hdfFileName,
              String varName, String satellite, String sensor, String origin,
              GregorianCalendar startDate, GregorianCalendar centeredTime,
              String passType, String dataUnits,
              int dataFractionDigits, int latLonFractionDigits)
              throws Exception {

          //This is synchronized because all calls to HDFLibrary are static.
          //So I need to ensure that only one thread uses it at once.
          synchronized(saveAsHDFLock) {
              if (verbose) String2.log("Grid.saveAsHDF " + hdfFileName);
              String errorIn = String2.ERROR + " in HdfWriter.grdToHdf: ";

              // create HDF file
              File2.delete(hdfFileName);

              //make sure there is data
              ensureThereIsData();

              int fileID = HDFLibrary.SDstart(hdfFileName, HDFConstants.DFACC_CREATE); //fails if file already exists
              if (verbose) String2.log("  create hdf fileID = " + fileID);

              // create SDS 0
              int sdsID = HDFLibrary.SDcreate(fileID, varName, HDFConstants.DFNT_FLOAT64, 2,
                  new int[]{lat.length, lon.length});
              if (verbose) String2.log("  sdsID create 0 = " + sdsID);
              int nLon = lon.length;
              int nLat = lat.length;
              int nData = data.length;
              if (verbose) String2.log("  nLon=" + nLon + " nLat=" + nLat);
              Test.ensureEqual(nLat * nLon, nData, "nLon(" + nLon + ") * nLat(" + nLat + ") != nData(" + nData + ")");
              double tData[] = new double[nData];
              int po = 0;
              for (int tLat = nLat - 1; tLat >= 0; tLat--) {
                  for (int tLon = 0; tLon < nLon; tLon++) {
                      tData[po] = getData(tLon, tLat);
                      if (Double.isNaN(tData[po]))
                          tData[po] = hdfMissingValue;
                      po++;
                  }
              }
              Test.ensureTrue(
                  HDFLibrary.SDwritedata(sdsID, new int[]{0,0}, new int[]{1,1}, //ds_start, ds_stride,
                      new int[]{nLat, nLon}, tData),   //ds_edges, var); //nLon,nLat works; I want nLat,nLon
                  errorIn + "SDwritedata for SDS 0.");
              tData = null; //allow garbage collection
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for SDS 0.");

              // create SDS 1
              //If I write lon and lat as 1D variables, CDAT doesn't ask if you
              //  want to import them (which it does for files created by Dave's script,
              //  because Matlab treats everything as a 2D (or more) array).
              //I think 1D is more appropriate. 2D (and importing) seems to imply they are data.
              sdsID = HDFLibrary.SDcreate(fileID, "Longitude", HDFConstants.DFNT_FLOAT64, 1, new int[]{nLon});
              if (verbose) String2.log("  sdsID create 1 = " + sdsID);
              Test.ensureTrue(
                  HDFLibrary.SDwritedata(sdsID, new int[]{0}, new int[]{1}, new int[]{nLon}, lon),
                  errorIn + "SDwritedata for SDS 1.");
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for SDS 1.");

              // create SDS 2
              sdsID = HDFLibrary.SDcreate(fileID, "Latitude", HDFConstants.DFNT_FLOAT64, 1, new int[]{nLat});
              if (verbose) String2.log("  sdsID create 2 = " + sdsID);
              Test.ensureTrue(
                  HDFLibrary.SDwritedata(sdsID, new int[]{0}, new int[]{1}, new int[]{nLat}, lat),
                  errorIn + "SDwritedata for SDS 2.");
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for SDS 2.");

              //------------------------------------------------------
              //write metadata for HDF files using CoastWatch Metadata Specifications

              //write Global Attributes
              setAttribute(fileID, "satellite", satellite);              //string
              setAttribute(fileID, "sensor", sensor);                    //string
              setAttribute(fileID, "origin", origin);                    //string
              setAttribute(fileID, "history", "unknown");                //string
              setAttribute(fileID, "cwhdf_version", "3.2");              //string
              setAttribute(fileID, "composite", startDate.equals(centeredTime)? "false" : "true"); //string

              //pass_date is days since Jan 1, 1970, e.g., {12806, 12807, 12808} for composite
              //start_time is seconds since midnight, e.g., {0, 0, 86399} for composite
              //int division truncates result
              long startMillis = Calendar2.utcToMillis(startDate);
              long endMillis   = Calendar2.utcToMillis(centeredTime);
              int startDays = (int)(startMillis / Calendar2.MILLIS_PER_DAY); //safe
              int endDays   = (int)(endMillis   / Calendar2.MILLIS_PER_DAY); //safe
              int nDays = endDays - startDays + 1;
              int pass_date[]     = new int[nDays];
              double start_time[] = new double[nDays]; //initially filled with 0's
              for (int i = 0; i < nDays; i++)
                  pass_date[i] = startDays + i;
              //int division 1000 truncates to second; % discards nDays
              start_time[nDays - 1] = (endMillis / 1000) % Calendar2.SECONDS_PER_DAY;
              setAttribute(fileID, "pass_date", pass_date);              //int32[nDays]
              setAttribute(fileID, "start_time", start_time);            //float64[nDays]

              //write map projection data
              setAttribute(fileID, "projection_type", "mapped");         //string
              setAttribute(fileID, "projection", "geographic");          //string
              setAttribute(fileID, "gctp_sys", new int[]{0});            //int32
              setAttribute(fileID, "gctp_zone", new int[]{0});           //int32
              setAttribute(fileID, "gctp_parm", new double[15]);         //float64[15 0's]
              setAttribute(fileID, "gctp_datum", new int[]{12});         //int32 12=WGS84

              //determine et_affine transformation
              // long = a*row + c*col + e
              // lat = b*row + d*col + f
              double matrix[] = {0, -latSpacing, lonSpacing,
                  0, lon[0], lat[lat.length-1]};
              setAttribute(fileID, "et_affine", matrix);                 //float64[] {a, b, c, d, e, f}

              //write row and column attributes
              setAttribute(fileID, "rows", new int[]{nLat});             //int32 number of rows
              setAttribute(fileID, "cols", new int[]{nLon});             //int32 number of columns

              //polygon attributes would be written here if needed

              //metadata for variable 0
              sdsID = HDFLibrary.SDselect(fileID, 0);
              if (verbose) String2.log("  sdsID select 0 = " + sdsID);
              setAttribute(sdsID, "long_name", varName);               //string e.g., "sea surface temperature"
              setAttribute(sdsID, "units", dataUnits);                 //string e.g., "degrees celsius"
              setAttribute(sdsID, "coordsys", "geographic");           //string
              setAttribute(sdsID, "_FillValue", new double[]{hdfMissingValue});   //same type as data as data
              setAttribute(sdsID, "missing_value", new double[]{hdfMissingValue});//same type as data as data
              setAttribute(sdsID, "scale_factor", new double[]{1});    //float64
              setAttribute(sdsID, "scale_factor_err", new double[]{0});//float64
              setAttribute(sdsID, "add_offset", new double[]{0});      //float64 calibration offset
              setAttribute(sdsID, "add_offset_err", new double[]{0});  //float64 calibration error
              setAttribute(sdsID, "calibrated_nt", new int[]{0});      //int32 hdf data type code for uncalibrated data
              setAttribute(sdsID, "fraction_digits", new int[]{dataFractionDigits}); //int32
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for variable 0.");

              //metadata for variable 1
              sdsID = HDFLibrary.SDselect(fileID, 1);
              if (verbose) String2.log("  sdsID select 1 = " + sdsID);
              setAttribute(sdsID, "long_name", "Longitude");           //string e.g., "sea surface temperature"
              setAttribute(sdsID, "units", "degrees");                 //string e.g., "degrees celsius"
              setAttribute(sdsID, "coordsys", "geographic");           //string
              setAttribute(sdsID, "_FillValue", new double[]{hdfMissingValue});   //same type as data as data
              setAttribute(sdsID, "missing_value", new double[]{hdfMissingValue});//same type as data as data
              setAttribute(sdsID, "scale_factor", new double[]{1});    //float64
              setAttribute(sdsID, "scale_factor_err", new double[]{0});//float64
              setAttribute(sdsID, "add_offset", new double[]{0});      //float64 calibration offset
              setAttribute(sdsID, "add_offset_err", new double[]{0});  //float64 calibration error
              setAttribute(sdsID, "calibrated_nt", new int[]{0});      //int32 hdf data type code for uncalibrated data
              setAttribute(sdsID, "fraction_digits", new int[]{latLonFractionDigits}); //int32
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for variable 1.");

              //metadata for variable 2
              sdsID = HDFLibrary.SDselect(fileID, 2);
              if (verbose) String2.log("  sdsID select 2 = " + sdsID);
              setAttribute(sdsID, "long_name", "Latitude");            //string e.g., "sea surface temperature"
              setAttribute(sdsID, "units", "degrees");                 //string e.g., "degrees celsius"
              setAttribute(sdsID, "coordsys", "geographic");           //string
              setAttribute(sdsID, "_FillValue", new double[]{hdfMissingValue});   //same type as data as data
              setAttribute(sdsID, "missing_value", new double[]{hdfMissingValue});//same type as data as data
              setAttribute(sdsID, "scale_factor", new double[]{1});    //float64
              setAttribute(sdsID, "scale_factor_err", new double[]{0});//float64
              setAttribute(sdsID, "add_offset", new double[]{0});      //float64 calibration offset
              setAttribute(sdsID, "add_offset_err", new double[]{0});  //float64 calibration error
              setAttribute(sdsID, "calibrated_nt", new int[]{0});      //int32 hdf data type code for uncalibrated data
              setAttribute(sdsID, "fraction_digits", new int[]{latLonFractionDigits}); //int32
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for variable 2.");

              //Close all open datasets and files and saves changes
              //Dave: hdfml("closeall");   //SDend is from Matlab book
              Test.ensureTrue(
                  HDFLibrary.SDend(fileID),
                  errorIn + "SDend.");
          }
      }
  */

  /**
   * Sets an HDF attribute value. This is PRIVATE because it isn't thread safe and should only be
   * used by saveAsHDF which is synchronized internally.
   *
   * @param id the HDF scientific dataset ID.
   * @param name the attribute name.
   * @param array an array of some primitive type or a String
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  /*    private static void setAttribute(int id, String name, Object array)
          throws HDFException, ClassNotFoundException {

          boolean result;
          if (array instanceof String sa) {
              byte bar[] = sa.getBytes();
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_CHAR8, bar.length, bar);
          } else if (array instanceof byte[] ba)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_INT8, ba.length, array);
          else if (array instanceof short[] sa)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_INT16, sa.length, array);
          else if (array instanceof int[] ia)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_INT32, ia.length, array);
          else if (array instanceof long[] la)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_INT64, la.length, array);
          else if (array instanceof float[] fa)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_FLOAT32, fa.length, array);
          else if (array instanceof double[] da)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_FLOAT64, da.length, array);
          else throw new ClassNotFoundException ("Unsupported signed type class for: " + name);

          if (!result)
              throw new HDFException ("Cannot set attribute value for '" + name + "'.");
      }
  */

  /**
   * Save this grid data as a Matlab .mat file. This writes the lon values as they are currently in
   * this grid (e.g., +-180 or 0..360). This overwrites any existing file of the specified name.
   * This makes an effort not to create a partial file if there is an error. If no exception is
   * thrown, the file was successfully created.
   *
   * @param directory with a slash at the end
   * @param name The file name without the extension (e.g., myFile). The extension ".mat" will be
   *     added.
   * @param varName the name to use for the variable (e.g., ux10). If it isn't variableNameSafe, it
   *     will be made so.
   * @throws Exception
   */
  public void saveAsMatlab(String directory, String name, String varName) throws Exception {

    if (verbose) String2.log("Grid.saveAsMatlab " + name);
    long time = System.currentTimeMillis();
    varName = String2.modifyToBeVariableNameSafe(varName);

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS[SAVE_AS_MATLAB];
    File2.delete(directory + name + ext);

    // make sure there is data
    ensureThereIsData();

    // open a dataOutputStream
    DataOutputStream dos = DataStream.getDataOutputStream(directory + randomInt);
    try {
      // write the header
      Matlab.writeMatlabHeader(dos);

      // first: write the lon array
      Matlab.writeDoubleArray(dos, "lon", lon);

      // second: make the lat array
      Matlab.writeDoubleArray(dos, "lat", lat);

      // make an array of the data[row][col]
      int nLat = lat.length;
      int nLon = lon.length;
      double ar[][] = new double[nLat][nLon];
      for (int row = 0; row < nLat; row++)
        for (int col = 0; col < nLon; col++) ar[row][col] = getData(col, row);
      Matlab.write2DDoubleArray(dos, varName, ar);

      // this doesn't write attributes because .mat files don't store attributes
      // setStatsAttributes(true); //true = double
      // write the attributes...

    } finally {
      dos.close();
    }

    // rename the file to the specified name
    File2.rename(directory, randomInt + "", name + ext);

    // Old way relies on script which calls Matlab.
    // This relies on a proprietary program, so good to remove it.
    // cShell("/u00/chump/grdtomatlab " + fullGrdName + " " +
    //    fullResultName + randomInt + ".mat " + varName);

    if (verbose)
      String2.log("  Grid.saveAsMatlab done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /**
   * This sets globalAttributes, latAttributes, lonAttributes, and dataAttributes so that the
   * attributes have COARDS, CF, THREDDS ACDD, and CWHDF-compliant metadata attributes. This also
   * calls calculateStats, so that information will be up-to-date. See MetaMetadata.txt for more
   * information.
   *
   * <p>This is used in some situations. In other situations, gridDataSet.setAttributes is used.
   *
   * @param name A CWBrowser-style file name (without directory info) so that fileNameUtility can
   *     generate the information.
   * @param fileNameUtility is used to generate all of the metadata based on the file name
   * @param saveMVAsDouble If true, _FillValue and missing_value are saved as doubles, else floats.
   */
  public void setAttributes(String name, FileNameUtility fileNameUtility, boolean saveMVAsDouble)
      throws Exception {
    Test.ensureNotNull(fileNameUtility, "fileNameUtility is null.");
    // should this clear existing attributes?

    // calculateStats
    calculateStats();

    // assemble the global metadata attributes
    int nLat = lat.length;
    int nLon = lon.length;
    globalAttributes.set("Conventions", FileNameUtility.getConventions());
    globalAttributes.set("title", fileNameUtility.getBoldTitle(name));
    globalAttributes.set("summary", fileNameUtility.getAbstract(name));
    globalAttributes.set("keywords", fileNameUtility.getKeywords(name));
    globalAttributes.set("id", FileNameUtility.getID(name));
    globalAttributes.set("naming_authority", FileNameUtility.getNamingAuthority());
    globalAttributes.set("keywords_vocabulary", FileNameUtility.getKeywordsVocabulary());
    globalAttributes.set("cdm_data_type", FileNameUtility.getCDMDataType());
    globalAttributes.set("date_created", FileNameUtility.getDateCreated());
    globalAttributes.set("creator_name", FileNameUtility.getCreatorName());
    globalAttributes.set("creator_url", FileNameUtility.getCreatorURL());
    globalAttributes.set("creator_email", FileNameUtility.getCreatorEmail());
    globalAttributes.set("institution", fileNameUtility.getInstitution(name));
    globalAttributes.set("project", FileNameUtility.getProject());
    globalAttributes.set("processing_level", FileNameUtility.getProcessingLevel());
    globalAttributes.set("acknowledgement", FileNameUtility.getAcknowledgement());
    globalAttributes.set(
        "geospatial_vertical_min", 0.0); // currently depth always 0.0 (not 0, which is int)
    globalAttributes.set("geospatial_vertical_max", 0.0);
    globalAttributes.set("geospatial_lat_min", Math.min(lat[0], lat[nLat - 1]));
    globalAttributes.set("geospatial_lat_max", Math.max(lat[0], lat[nLat - 1]));
    globalAttributes.set("geospatial_lon_min", Math.min(lon[0], lon[nLon - 1]));
    globalAttributes.set("geospatial_lon_max", Math.max(lon[0], lon[nLon - 1]));
    globalAttributes.set("geospatial_vertical_units", "m");
    globalAttributes.set("geospatial_vertical_positive", "up");
    globalAttributes.set("geospatial_lat_units", FileNameUtility.getLatUnits());
    globalAttributes.set("geospatial_lat_resolution", Math.abs(latSpacing));
    globalAttributes.set("geospatial_lon_units", FileNameUtility.getLonUnits());
    globalAttributes.set("geospatial_lon_resolution", Math.abs(lonSpacing));
    globalAttributes.set(
        "time_coverage_start",
        Calendar2.formatAsISODateTimeTZ(FileNameUtility.getStartCalendar(name)));
    globalAttributes.set(
        "time_coverage_end", Calendar2.formatAsISODateTimeTZ(FileNameUtility.getEndCalendar(name)));
    // globalAttributes.set("time_coverage_resolution", "P12H"));
    globalAttributes.set("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
    globalAttributes.set("license", FileNameUtility.getLicense());
    if (globalAttributes.get("contributor_name") == null) {
      globalAttributes.set("contributor_name", fileNameUtility.getContributorName(name));
      globalAttributes.set("contributor_role", FileNameUtility.getContributorRole());
    }
    globalAttributes.set("date_issued", FileNameUtility.getDateCreated());
    globalAttributes.set("references", fileNameUtility.getReferences(name));
    globalAttributes.set("source", fileNameUtility.getSource(name));
    // attributes for Google Earth
    globalAttributes.set("Southernmost_Northing", Math.min(lat[0], lat[nLat - 1]));
    globalAttributes.set("Northernmost_Northing", Math.max(lat[0], lat[nLat - 1]));
    globalAttributes.set("Westernmost_Easting", Math.min(lon[0], lon[nLon - 1]));
    globalAttributes.set("Easternmost_Easting", Math.max(lon[0], lon[nLon - 1]));

    // globalAttributes for HDF files using CoastWatch Metadata Specifications
    // required unless noted otherwise
    globalAttributes.set("cwhdf_version", "3.4"); // string
    String satellite = fileNameUtility.getSatellite(name);
    if (satellite.length() > 0) {
      globalAttributes.set("satellite", fileNameUtility.getSatellite(name)); // string
      globalAttributes.set("sensor", fileNameUtility.getSensor(name)); // string
    } else {
      globalAttributes.set("data_source", fileNameUtility.getSensor(name)); // string
    }
    globalAttributes.set("composite", FileNameUtility.getComposite(name)); // string (not required)

    globalAttributes.set(
        "pass_date", new IntArray(fileNameUtility.getPassDate(name))); // int32[nDays]
    globalAttributes.set(
        "start_time", new DoubleArray(fileNameUtility.getStartTime(name))); // float64[nDays]
    globalAttributes.set("origin", fileNameUtility.getCourtesy(name)); // string
    globalAttributes.set("history", fileNameUtility.getHistory(name)); // string

    // write map projection data
    globalAttributes.set("projection_type", "mapped"); // string
    globalAttributes.set("projection", "geographic"); // string
    globalAttributes.set("gctp_sys", 0); // int32
    globalAttributes.set("gctp_zone", 0); // int32
    globalAttributes.set("gctp_parm", new DoubleArray(new double[15])); // float64[15 0's]
    globalAttributes.set("gctp_datum", 12); // int32 12=WGS84

    // determine et_affine transformation
    // lon = a*row + c*col + e
    // lat = b*row + d*col + f
    double matrix[] = {0, -latSpacing, lonSpacing, 0, lon[0], lat[lat.length - 1]}; // up side down
    globalAttributes.set("et_affine", new DoubleArray(matrix)); // float64[] {a, b, c, d, e, f}

    // write row and column attributes
    globalAttributes.set("rows", nLat); // int32 number of rows
    globalAttributes.set("cols", nLon); // int32 number of columns
    globalAttributes.set(
        "polygon_latitude",
        new DoubleArray(
            new double[] { // not required
              lat[0], lat[nLat - 1], lat[nLat - 1], lat[0], lat[0]
            }));
    globalAttributes.set(
        "polygon_longitude",
        new DoubleArray(
            new double[] { // not required
              lon[0], lon[0], lon[nLon - 1], lon[nLon - 1], lon[0]
            }));

    // COARDS, CF, ACDD metadata attributes for latitude
    latAttributes.set("_CoordinateAxisType", "Lat");
    latAttributes.set("long_name", "Latitude");
    latAttributes.set("standard_name", "latitude");
    latAttributes.set("units", FileNameUtility.getLatUnits());

    // Lynn's metadata attributes
    latAttributes.set("point_spacing", "even");
    latAttributes.set("actual_range", new DoubleArray(new double[] {lat[0], lat[nLat - 1]}));

    // CWHDF metadata attributes for Latitude
    // latAttributes.set("long_name",             "Latitude")); //string
    // latAttributes.set("units",                 fileNameUtility.getLatUnits(name))); //string
    latAttributes.set("coordsys", "geographic"); // string
    latAttributes.set("fraction_digits", fileNameUtility.getLatLonFractionDigits(name)); // int32

    // COARDS, CF, ACDD metadata attributes for longitude
    lonAttributes.set("_CoordinateAxisType", "Lon");
    lonAttributes.set("long_name", "Longitude");
    lonAttributes.set("standard_name", "longitude");
    lonAttributes.set("units", FileNameUtility.getLonUnits());

    // Lynn's metadata attributes
    lonAttributes.set("point_spacing", "even");
    lonAttributes.set("actual_range", new DoubleArray(new double[] {lon[0], lon[nLon - 1]}));

    // CWHDF metadata attributes for Longitude
    // lonAttributes.set("long_name",             "Longitude"); //string
    // lonAttributes.set("units",                 fileNameUtility.getLonUnits(name));  //string
    lonAttributes.set("coordsys", "geographic"); // string
    lonAttributes.set("fraction_digits", fileNameUtility.getLatLonFractionDigits(name)); // int32

    // COARDS, CF, ACDD metadata attributes for data
    dataAttributes.set("long_name", fileNameUtility.getBoldTitle(name));
    String sn = fileNameUtility.getStandardName(name);
    if (sn.length() > 0) {
      dataAttributes.set("standard_name", sn);
    }
    dataAttributes.set("units", fileNameUtility.getUdUnits(name));
    PrimitiveArray mvAr;
    PrimitiveArray rangeAr;
    if (saveMVAsDouble) {
      mvAr = new DoubleArray(new double[] {(double) DataHelper.FAKE_MISSING_VALUE});
      rangeAr = new DoubleArray(new double[] {minData, maxData});
    } else {
      mvAr = new FloatArray(new float[] {(float) DataHelper.FAKE_MISSING_VALUE});
      rangeAr = new FloatArray(new float[] {(float) minData, (float) maxData});
    }
    dataAttributes.set("_FillValue", mvAr); // must be same type as data
    dataAttributes.set("missing_value", mvAr); // must be same type as data
    dataAttributes.set("numberOfObservations", nValidPoints);
    dataAttributes.set("actual_range", rangeAr);

    // CWHDF metadata attributes for the data: varName
    // dataAttributes.set("long_name",            fileNameUtility.getTitle(name))); //string
    // dataAttributes.set("units",                fileNameUtility.getUDUnits(name))); //string
    dataAttributes.set("coordsys", "geographic"); // string
    dataAttributes.set("fraction_digits", fileNameUtility.getDataFractionDigits(name)); // int32
  }

  /**
   * This sets the data attributes related to calculateStats, which change with the type of file the
   * data is being saved to: _FillValue, missing_value, actual_range, and numberOfObservations. See
   * MetaMetadata.txt for more information.
   *
   * <p>Generally, you will call this after setting other attributes.
   *
   * @param saveMVAsDouble If true, _FillValue and missing_value are saved as doubles, else floats.
   */
  public void setStatsAttributes(boolean saveMVAsDouble) throws Exception {

    // calculateStats
    calculateStats();

    // actual_range
    if (Double.isNaN(minData)) {
      dataAttributes.remove("actual_range");
    } else {
      if (saveMVAsDouble)
        dataAttributes.set("actual_range", new DoubleArray(new double[] {minData, maxData}));
      else
        dataAttributes.set(
            "actual_range", new FloatArray(new float[] {(float) minData, (float) maxData}));
    }

    // missing_value
    PrimitiveArray mvAr;
    if (saveMVAsDouble)
      mvAr = new DoubleArray(new double[] {(double) DataHelper.FAKE_MISSING_VALUE});
    else mvAr = new FloatArray(new float[] {(float) DataHelper.FAKE_MISSING_VALUE});
    dataAttributes.set("_FillValue", mvAr); // must be same type as data
    dataAttributes.set("missing_value", mvAr); // must be same type as data

    dataAttributes.set("numberOfObservations", nValidPoints);
    dataAttributes.set(
        "percentCoverage",
        lat.length + lon.length == 0 ? 0.0 : nValidPoints / (lat.length * (double) lon.length));
  }

  /**
   * Save this grid data as a 4D netCDF .nc file using the currently available globalAttributes,
   * latAttributes, lonAttributes, and dataAttributes. This writes the lon values as they are
   * currently in this grid (e.g., +-180 or 0..360). This overwrites any existing file of the
   * specified name. This makes an effort not to create a partial file if there is an error. If no
   * exception is thrown, the file was successfully created. The lat, lon, altitude, and data values
   * are written as floats (see comments for DataHelper.FAKE_MISSING_VALUE). The time data values
   * are written as doubles (seconds since 1970-01-01T00:00:00Z). The lat variable will always be
   * named "lat". The lon variable will always be named "lon". The altitude variable will always be
   * named "altitude". The time variable will always be named "time". The time value will be the
   * centered value calculated from the ISO 8601 strings in the global attribute
   * "time_coverage_start" and "time_coverage_end".
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".nc" will be
   *     added. The name does not have to be a CWBrowser-style name.
   * @param dataName The name for the data variable (e.g., ATssta).
   * @throws Exception
   */
  public void saveAsNetCDF(String directory, String name, String dataName) throws Exception {
    if (verbose) String2.log("Grid.saveAsNetCDF " + name);
    long time = System.currentTimeMillis();

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS[SAVE_AS_NETCDF];
    File2.delete(directory + name + ext);

    // make sure there is data
    ensureThereIsData();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // write the data
    // items determined by looking at a .nc file; items written in that order
    NetcdfFormatWriter ncWriter = null;
    boolean nc3Mode = true;

    try {
      NetcdfFormatWriter.Builder nc = NetcdfFormatWriter.createNewNetcdf3(directory + randomInt);
      Group.Builder rootGroup = nc.getRootGroup();
      nc.setFill(false);

      int nLat = lat.length;
      int nLon = lon.length;

      // define the dimensions
      Dimension timeDimension = NcHelper.addDimension(rootGroup, "time", 1);
      Dimension altitudeDimension = NcHelper.addDimension(rootGroup, "altitude", 1);
      Dimension latDimension = NcHelper.addDimension(rootGroup, "lat", nLat);
      Dimension lonDimension = NcHelper.addDimension(rootGroup, "lon", nLon);

      // create the variables (and gather the data)
      String endString = globalAttributes.getString("time_coverage_end");
      double centeredTimeDouble = Double.NaN;
      String centeredTimeUnits;
      String calendar = null; // if not null, set the attribute
      boolean hasTime = true;
      if (endString == null) {
        // e.g., bathymetry
        hasTime = false;
        calendar = "none";
        centeredTimeUnits = Calendar2.SECONDS_SINCE_1970;
      } else if (endString.indexOf(SINCE_111) > 0) {
        // it's a climatology, e.g., "2 months since 0001-01-01"
        int spPo = endString.indexOf(' ');
        Test.ensureNotEqual(spPo, -1, String2.ERROR + ": Unexpected endString=" + endString);
        centeredTimeDouble = String2.parseDouble(endString.substring(0, spPo)); // e.g., 2
        Test.ensureNotEqual(
            centeredTimeDouble, Double.NaN, String2.ERROR + ": Unexpected endString=" + endString);
        centeredTimeUnits = endString.substring(spPo + 1); // e.g., "months since 0001-01-01"
        calendar = "none";
      } else {
        // standard case: it's an iso datetime
        String startString = globalAttributes.getString("time_coverage_start");
        double startTimeDouble =
            Calendar2.isoStringToEpochSeconds(startString); // throws exception if trouble
        double endTimeDouble =
            Calendar2.isoStringToEpochSeconds(endString); // throws exception if trouble
        if (startTimeDouble == endTimeDouble) {
          // hday
          centeredTimeDouble = startTimeDouble;
        } else {
          // composite
          centeredTimeDouble = (startTimeDouble + endTimeDouble) / 2;
        }
        centeredTimeUnits = Calendar2.SECONDS_SINCE_1970;
      }
      if (hasTime)
        String2.log(
            "  start="
                + globalAttributes.getString("time_coverage_start")
                + " end="
                + endString
                + "\n  center="
                + Calendar2.epochSecondsToIsoStringTZ(centeredTimeDouble));
      else String2.log("  hasTime=false");

      ArrayDouble.D1 tTime = new ArrayDouble.D1(1);
      tTime.set(0, hasTime ? centeredTimeDouble : 0);
      Variable.Builder timeVar =
          NcHelper.addVariable(rootGroup, "time", DataType.DOUBLE, Arrays.asList(timeDimension));

      ArrayDouble.D1 tAltitude = new ArrayDouble.D1(1);
      tAltitude.set(0, 0); // I treat all as altitude=0 !!!!
      Variable.Builder altitudeVar =
          NcHelper.addVariable(
              rootGroup, "altitude", DataType.DOUBLE, Arrays.asList(altitudeDimension));

      ArrayDouble.D1 tLat = new ArrayDouble.D1(nLat);
      for (int i = 0; i < nLat; i++) tLat.set(i, lat[i]);
      Variable.Builder latVar =
          NcHelper.addVariable(rootGroup, "lat", DataType.DOUBLE, Arrays.asList(latDimension));

      ArrayDouble.D1 tLon = new ArrayDouble.D1(nLon);
      for (int i = 0; i < nLon; i++) tLon.set(i, lon[i]);
      Variable.Builder lonVar =
          NcHelper.addVariable(rootGroup, "lon", DataType.DOUBLE, Arrays.asList(lonDimension));

      // write values to ArrayFloat.D4
      ArrayFloat.D4 tGrid = new ArrayFloat.D4(1, 1, nLat, nLon);
      for (int iLat = 0; iLat < nLat; iLat++) {
        for (int iLon = 0; iLon < nLon; iLon++) {
          float tData = (float) getData(iLon, iLat); // for cdat, was nLat-1 - iLat); //up side down
          tGrid.set(
              0, 0, iLat, iLon, Float.isNaN(tData) ? (float) DataHelper.FAKE_MISSING_VALUE : tData);
        }
      }
      // order of dimensions is specified by the
      // coards standard (https://ferret.pmel.noaa.gov/noaa_coop/coop_cdf_profile.html)
      // see the topics "Number of dimensions" and "Order of dimensions"
      Variable.Builder dataVar =
          NcHelper.addVariable(
              rootGroup,
              dataName,
              DataType.FLOAT,
              Arrays.asList(timeDimension, altitudeDimension, latDimension, lonDimension));

      // setStatsAttributes
      setStatsAttributes(false); // false -> save as floats

      // write Attributes
      String names[] = globalAttributes.getNames();
      for (int i = 0; i < names.length; i++) {
        // any existing et_affine needs to be modified, since .nc has data right-side-up
        // I suspect CDAT georeferences this correctly but with coast and data upside down.
        if (names[i].equals("et_affine")) {
          // lon = a*row + c*col + e
          // lat = b*row + d*col + f
          double matrix[] = {0, latSpacing, lonSpacing, 0, lon[0], lat[0]}; // right side up
          rootGroup.addAttribute(
              NcHelper.newAttribute(
                  "et_affine", NcHelper.get1DArray(matrix, false))); // float64[] {a, b, c, d, e, f}
        } else {
          rootGroup.addAttribute(
              NcHelper.newAttribute(nc3Mode, names[i], globalAttributes.get(names[i])));
        }
      }

      // time attributes
      if (hasTime)
        timeVar.addAttribute(
            NcHelper.newAttribute(
                "actual_range",
                NcHelper.get1DArray(new double[] {centeredTimeDouble, centeredTimeDouble}, false)));
      timeVar.addAttribute(new Attribute("fraction_digits", Integer.valueOf(0)));
      timeVar.addAttribute(
          new Attribute("long_name", hasTime ? "Centered Time" : "Place Holder for Time"));
      timeVar.addAttribute(new Attribute("units", centeredTimeUnits));
      timeVar.addAttribute(new Attribute("standard_name", "time"));
      timeVar.addAttribute(new Attribute("axis", "T"));
      timeVar.addAttribute(new Attribute("_CoordinateAxisType", "Time"));
      if (calendar != null) timeVar.addAttribute(new Attribute("calendar", calendar));

      // altitude attributes
      altitudeVar.addAttribute(
          NcHelper.newAttribute("actual_range", NcHelper.get1DArray(new double[] {0, 0}, false)));
      altitudeVar.addAttribute(new Attribute("fraction_digits", Integer.valueOf(0)));
      altitudeVar.addAttribute(new Attribute("long_name", "Altitude"));
      altitudeVar.addAttribute(new Attribute("positive", "up"));
      altitudeVar.addAttribute(new Attribute("standard_name", "altitude"));
      altitudeVar.addAttribute(new Attribute("units", "m"));
      altitudeVar.addAttribute(new Attribute("axis", "Z"));
      altitudeVar.addAttribute(new Attribute("_CoordinateAxisType", "Height"));
      altitudeVar.addAttribute(new Attribute("_CoordinateZisPositive", "up"));

      // lat
      NcHelper.setAttributes(
          nc3Mode, latVar, latAttributes, false); // unsigned=false because it is a float
      latVar.addAttribute(new Attribute("axis", "Y"));

      // lon
      NcHelper.setAttributes(
          nc3Mode, lonVar, lonAttributes, false); // unsigned=false because it is a float
      lonVar.addAttribute(new Attribute("axis", "X"));

      // data
      NcHelper.setAttributes(
          nc3Mode, dataVar, dataAttributes, false); // unsigned=false because it is a float

      // leave "define" mode
      ncWriter = nc.build();

      // then add data
      ncWriter.write(timeVar.getFullName(), tTime);
      ncWriter.write(altitudeVar.getFullName(), tAltitude);
      ncWriter.write(latVar.getFullName(), tLat);
      ncWriter.write(lonVar.getFullName(), tLon);
      ncWriter.write(dataVar.getFullName(), tGrid);

      // if close throws exception, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      // rename the file to the specified name
      File2.rename(directory, randomInt + "", name + ext);

      // diagnostic
      if (verbose)
        String2.log(
            "  Grid.saveAsNetCDF done.  TIME=" + (System.currentTimeMillis() - time) + "\n");
      // String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

    } catch (Exception e) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(directory + randomInt);
        ncWriter = null;
      }
      throw e;
    }
  }

  /**
   * This is an alias for saveAsXYZ(directory, name, true, "NaN");
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".xyz" will be
   *     added.
   * @throws Exception
   */
  public void saveAsXYZ(String directory, String name) throws Exception {
    saveAsXYZ(
        directory, name, false, // was true=fromTop; I think to mimic what GMT did
        "NaN");
  }

  /**
   * Save this grid data as a tab-separated XYZ ASCII file. This writes the lon values as they are
   * currently in this grid (e.g., +-180 or 0..360). Note the GMT seems to want the values as
   * 0..360. This overwrites any existing file of the specified name. This makes an effort not to
   * create a partial file if there is an error. If no exception is thrown, the file was
   * successfully created. **Currently, the lat, lon, and data values are written as floats.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".xyz" will be
   *     added.
   * @param fromTop write the data in rows, starting at the highest lat (vs the lowest lat).
   * @param NaNString is the String to write for NaN's.
   * @throws Exception
   */
  public void saveAsXYZ(String directory, String name, boolean fromTop, String NaNString)
      throws Exception {

    if (verbose) String2.log("Grid.saveAsXYZ " + name);
    long time = System.currentTimeMillis();

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS[SAVE_AS_XYZ];
    File2.delete(directory + name);

    // make sure there is data
    ensureThereIsData();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the temp file
    // (I tried with Buffer/FileOutputStream. No faster.)
    BufferedWriter bufferedWriter = File2.getBufferedFileWriter88591(directory + randomInt);
    try {

      // write the data
      int nLat = lat.length;
      int nLon = lon.length;
      if (fromTop) {
        // write values from row to row, top to bottom
        for (int tLat = nLat - 1; tLat >= 0; tLat--) {
          for (int tLon = 0; tLon < nLon; tLon++) {
            float f = (float) getData(tLon, tLat);
            bufferedWriter.write(
                String2.genEFormat10(lon[tLon])
                    + "\t"
                    + String2.genEFormat10(lat[tLat])
                    + "\t"
                    + (Float.isNaN(f) ? NaNString + '\n' : f + "\n"));
          }
        }
      } else {
        // write values from row to row, bottom to top
        for (int tLat = 0; tLat < nLat; tLat++) {
          for (int tLon = 0; tLon < nLon; tLon++) {
            float f = (float) getData(tLon, tLat);
            bufferedWriter.write(
                String2.genEFormat10(lon[tLon])
                    + "\t"
                    + String2.genEFormat10(lat[tLat])
                    + "\t"
                    + (Float.isNaN(f) ? NaNString + '\n' : f + "\n"));
          }
        }
      }
    } finally {
      bufferedWriter.close();
    }

    // rename the file to the specified name
    File2.rename(directory, randomInt + "", name + ext);

    // diagnostic
    /*
    if (verbose) {
        String[] rff = File2.readFromFile(directory + name + ext);
        if (rff[0].length() > 0)
            throw new Exception(String2.ERROR + ":\n" + rff[0]);
        String2.log("grid.saveAsXYZ: " + directory + name + ext + " contains:\n" +
            String2.annotatedString(
                rff[1].substring(0, Math.min(rff[1].length(), 200))));
    }
    */

    if (verbose)
      String2.log("  Grid.saveAsXYZ done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  // the values here won't change as new file types are supported
  public static final int SAVE_AS_GRD = 0;
  // ESRI_ASCII is before ASCII so it is found first when finding .asc in list of extensions
  public static final int SAVE_AS_ESRI_ASCII = 1; // lon values always -180 to 180
  public static final int SAVE_AS_HDF = 2;
  public static final int SAVE_AS_MATLAB = 3;
  public static final int SAVE_AS_XYZ = 4;
  public static final int SAVE_AS_GEOTIFF = 5;
  public static final int SAVE_AS_NETCDF = 6;
  public static final int SAVE_AS_ASCII = 7; // lon values unchanged
  public static final String SAVE_AS_EXTENSIONS[] = {
    ".grd", ".asc", ".hdf", ".mat", ".xyz", ".tif", ".nc", ".asc"
  };

  /**
   * This saves the current grid in some type of file. If the file already exists, it is touched,
   * and nothing else is done. This does not add attributes (other than calling addStatsAttributes
   * ()).
   *
   * @param directory the directory for the resulting file (with a slash at the end)
   * @param fileName the name for the resulting file (without any extension)
   * @param variableName the name for the variable, usually FileNameUtility.get6CharName(fileName)
   * @param saveAsType one of the SAVE_AS constants
   * @param zipIt If true, creates a .zip file and deletes the intermediate file (e.g., .asc). If
   *     false, the specified saveAsType is created.
   * @throws Exception if trouble
   */
  public void saveAs(
      String directory, String fileName, String variableName, int saveAsType, boolean zipIt)
      throws Exception {

    if (verbose) String2.log("Grid.saveAs(name=" + fileName + " type=" + saveAsType + ")");
    if (saveAsType != SAVE_AS_ASCII
        && saveAsType != SAVE_AS_ESRI_ASCII
        && saveAsType != SAVE_AS_GEOTIFF
        && saveAsType != SAVE_AS_GRD
        && saveAsType != SAVE_AS_HDF
        && saveAsType != SAVE_AS_MATLAB
        && saveAsType != SAVE_AS_NETCDF
        && saveAsType != SAVE_AS_XYZ)
      throw new RuntimeException(
          String2.ERROR + " in Grid.saveAs: invalid saveAsType=" + saveAsType);

    String ext = SAVE_AS_EXTENSIONS[saveAsType];

    // does the file already exist?
    String finalName = directory + fileName + ext + (zipIt ? ".zip" : "");
    if (File2.touch(finalName)) {
      if (verbose) String2.log("  Grid.saveAs done. reusing " + finalName);
      return;
    }

    // save as ...
    long time = System.currentTimeMillis();
    if (saveAsType == SAVE_AS_ASCII) saveAsASCII(directory, fileName);
    else if (saveAsType == SAVE_AS_ESRI_ASCII) saveAsEsriASCII(directory, fileName);
    else if (saveAsType == SAVE_AS_GEOTIFF) saveAsGeotiff(directory, fileName, variableName);
    else if (saveAsType == SAVE_AS_GRD) saveAsGrd(directory, fileName);
    else if (saveAsType == SAVE_AS_HDF) saveAsHDF(directory + fileName, variableName);
    else if (saveAsType == SAVE_AS_MATLAB) saveAsMatlab(directory, fileName, variableName);
    else if (saveAsType == SAVE_AS_NETCDF) saveAsNetCDF(directory, fileName, variableName);
    else if (saveAsType == SAVE_AS_XYZ) saveAsXYZ(directory, fileName);

    if (zipIt) {
      // zip to a temporary zip file, -j: don't include dir info
      SSR.zip(
          directory + fileName + ext + ".temp.zip", new String[] {directory + fileName + ext}, 20);

      // delete the file that was zipped
      File2.delete(directory + fileName + ext);

      // if all successful, rename to final name
      File2.rename(directory, fileName + ext + ".temp.zip", fileName + ext + ".zip");
    }
  }

  /**
   * This method converts from one type of Grid data file to another, based on their file names. See
   * msg below for details.
   *
   * @param args must contain two parameters:
   *     <ol>
   *       <li>The full name of an existing Grid file with a Dave-style file name, with an optional
   *           .zip or .gz suffix.
   *       <li>The full name of the desired Grid file with a Dave-style file name, with an optional
   *           .zip or .gz suffix.
   *     </ol>
   *     Or,
   *     <ol>
   *       <li>The full name of an existing <directory>/.<extension> describing a directory with
   *           grid files with Dave-style file names.
   *       <li>The full name of an existing <directory>/.<extension> describing a directory to be
   *           filled with grid files with Dave-style file names.
   *     </ol>
   *
   * @param fileNameUtility is used to generate all of the metadata based on the file name
   * @return trouble StringArray with full names of files that failed to convert
   * @throws Exception if trouble in set up, but error while converting a file just displays error
   *     message.
   */
  public static StringArray davesSaveAs(String args[], FileNameUtility fileNameUtility)
      throws Exception {
    // note: I wanted to add -r recursive switch. But there is no way
    // to specify the different in and out directories (unless I make things
    // dependent on coastwatch directory structure).
    String methodName = "Grid.davesSaveAs";
    String msg =
        "This program loads a grid from one data file and saves it in another\n"
            + "data file. The input and output names must be Dave-style names, e.g.,\n"
            + "  \"<dir>/AH2001067_2001067_sstd_westus.grd\" for composites, or\n"
            + "  \"<dir>/AH2005060_044800h_sstd_westus.grd\" for single passes.\n"
            + "It is ok if the names don't have \"_<regionName>\".\n"
            + "\n"
            + "usage: java gov.noaa.pfel.coastwatch.griddata.GridSaveAs <in> <out>\n"
            + "'in' and 'out' must be the complete directory + name + extension.\n"
            + "For whole directories, don't supply the name part of the 'in' and 'out'.\n"
            + "'in' and 'out' may also have a .zip or .gz extension.\n"
            + "\n"
            + "The file extensions determine what type of file is read and written:\n"
            + "  * .asc (write) an ESRI ASCII grid data file (plain .asc is not selectable)\n"
            + "  * .grd (read and write) a GMT-style NetCDF file\n"
            + "  * .hdf (read and write) a Hierarchal Data Format SDS (NASA EOSDIS) file\n"
            + "  * .mat (write) a Matlab binary file\n"
            + "  * .nc  (read and write) a NetCDF binary file\n"
            + "  * .tif (write) a georeferenced tiff (GeoTIFF) file\n"
            + "  * .xyz (write) an ASCII file with 3 columns: longitude, latitude,\n"
            + "       and data value (missing value = \"NaN\").\n"
            + "New .nc and .hdf files will have COARDS, CF, ADCC, and CWHDF compliant\n"
            + "metadata.  See MetaMetadata.txt for metadata details.  Metadata can only\n"
            + "be generated for the datasets pre-defined in\n"
            + "gov/noaa/pfel/coastwatch/DataSet.properties.  So .hdf and .nc files\n"
            + "can only be generated for those datasets.\n"
            + "The lon values in the incoming file can be either +/-180 or 0 - 360\n"
            + "and will not be altered.\n"
            + "This program is picky about the input files having exactly the right format.\n"
            + "\n"
            + "Have a pleasant day.";

    long time = System.currentTimeMillis();
    // ensure args.length = 2
    if (args == null || args.length != 2) {
      throw new Exception(
          String2.ERROR
              + ": Incorrect number of input arguments.\nargs="
              + String2.toCSSVString(args)
              + "\n\n"
              + msg);
    }

    // pick apart the file names
    String inDirName = args[0];
    String outDirName = args[1];
    String2.log(methodName + "\n  in =" + inDirName + "\n  out=" + outDirName);
    String inDir = File2.getDirectory(inDirName);
    String outDir = File2.getDirectory(outDirName);
    String inDaveNameExt = File2.getNameAndExtension(inDirName);
    String outDaveNameExt = File2.getNameAndExtension(outDirName);
    boolean inZip = inDaveNameExt.toLowerCase().endsWith(".zip");
    boolean inGz = inDaveNameExt.toLowerCase().endsWith(".gz");
    if (inZip) inDaveNameExt = inDaveNameExt.substring(0, inDaveNameExt.length() - 4);
    if (inGz) inDaveNameExt = inDaveNameExt.substring(0, inDaveNameExt.length() - 3);
    boolean outZip = outDaveNameExt.toLowerCase().endsWith(".zip");
    boolean outGz = outDaveNameExt.toLowerCase().endsWith(".gz");
    if (outZip) outDaveNameExt = outDaveNameExt.substring(0, outDaveNameExt.length() - 4);
    if (outGz) outDaveNameExt = outDaveNameExt.substring(0, outDaveNameExt.length() - 3);
    String inExt = File2.getExtension(inDaveNameExt).toLowerCase(); // e.g., ".grd"
    String outExt = File2.getExtension(outDaveNameExt).toLowerCase(); // e.g., ".nc"
    String inDaveName =
        inDaveNameExt.substring(
            0, inDaveNameExt.length() - inExt.length()); // now just a name, no ext
    String outDaveName = outDaveNameExt.substring(0, outDaveNameExt.length() - outExt.length());
    Test.ensureEqual(
        inDaveName,
        outDaveName,
        String2.ERROR
            + ": The file names (not counting directories or extensions) must be the same.\n\n"
            + msg);

    // gather the file names
    String nameList[];
    if (inDaveName.equals("")) {
      // just get dave-style names: AH2001067_2001067_sstd_westus.grd //_westus not required
      String regex =
          "[A-Z0-9]{2}[0-9]{7}_[0-9]{6}[0-9h]_.{4}.*\\"
              + inExt
              + (inZip ? "\\.[Zz][Ii][Pp]" : "")
              + (inGz ? "\\.[Gg][Zz]" : "");
      nameList = RegexFilenameFilter.list(inDir, regex);
    } else nameList = new String[] {File2.getNameAndExtension(inDirName)};

    // process the files
    StringArray trouble = new StringArray();
    for (int i = 0; i < nameList.length; i++) {
      try {
        String tDaveNameExt = nameList[i];
        String2.log("  converting " + tDaveNameExt);

        // unzip
        if (inZip) {
          SSR.unzip(inDir + tDaveNameExt, inDir, true, 10, null); // 10=seconds time out
          tDaveNameExt = tDaveNameExt.substring(0, tDaveNameExt.length() - 4);
        }
        if (inGz) {
          SSR.unGzip(inDir + tDaveNameExt, inDir, true, 10); // 10=seconds time out
          tDaveNameExt = tDaveNameExt.substring(0, tDaveNameExt.length() - 3);
        }

        String tDaveName =
            tDaveNameExt.substring(
                0, tDaveNameExt.length() - inExt.length()); // now just a name, no ext
        String tInCWName = null;
        try {
          tInCWName = FileNameUtility.convertDaveNameToCWBrowserName(tDaveName);
        } catch (Exception e) {
          String2.log(
              MustBe.throwable(
                  String2.ERROR
                      + ": Unable to convert DaveName ("
                      + tDaveName
                      + ") to CWBrowserName.",
                  e));
        }
        String tOutCWName = tInCWName;
        if (verbose) String2.log("tInCWName=" + tInCWName);

        // ensure dataset info is valid
        fileNameUtility.ensureValidDataSetProperties(
            FileNameUtility.get7CharName(tInCWName),
            true); // ensure is very important here, since this is often the conversion to
        // thredds-format data

        // read the input file  (always get the full range, file's min/max lon/lat
        Grid grid = new Grid();
        // verbose = true;
        if (inExt.equals(".grd")) {
          grid.readGrd(inDir + tDaveName + inExt);
        } else if (inExt.equals(".hdf")) {
          grid.readHDF(inDir + tDaveName + inExt, HdfConstants.DFNT_FLOAT64);
        } else if (inExt.equals(".nc")) {
          grid.readNetCDF(
              inDir + tDaveName + inExt,
              FileNameUtility.get6CharName(tInCWName),
              Double.NaN,
              Double.NaN,
              Double.NaN,
              Double.NaN,
              Integer.MAX_VALUE,
              Integer.MAX_VALUE);
        } else
          throw new Exception(
              String2.ERROR + ": Input type \"" + inExt + "\" not supported.\n\n" + msg);

        if (inZip || inGz) File2.delete(inDir + tDaveName + inExt);

        // add the metadata
        if (tOutCWName != null) grid.setAttributes(tOutCWName, fileNameUtility, false);

        // prove it has clean lon and lat values
        String2.log(
            "  lon min="
                + grid.lon[0]
                + " max="
                + grid.lon[grid.lon.length - 1]
                + " inc="
                + grid.lonSpacing
                + " n="
                + grid.lon.length);
        String2.log(
            "  lat min="
                + grid.lat[0]
                + " max="
                + grid.lat[grid.lat.length - 1]
                + " inc="
                + grid.latSpacing
                + " n="
                + grid.lat.length);

        // write the output file (use outCWName since saveAsHDF requires that)
        int saveAsType = String2.indexOf(Grid.SAVE_AS_EXTENSIONS, outExt);
        grid.saveAs(
            outDir,
            tOutCWName,
            FileNameUtility.get6CharName(tOutCWName),
            saveAsType,
            false); // don't zip here; rename first
        File2.rename(outDir, tOutCWName + outExt, tDaveName + outExt);

        // outZip
        if (outZip) {
          SSR.zip(
              outDir + tDaveName + outExt + ".zip",
              new String[] {outDir + tDaveName + outExt},
              10); // 10 = seconds time out
          File2.delete(outDir + tDaveName + outExt);
          // String2.log("GridSaveAs deleted: " + outDir + tDaveName + outExt);
        }
        if (outGz) {
          SSR.gzip(
              outDir + tDaveName + outExt + ".gz",
              new String[] {outDir + tDaveName + outExt},
              10); // 10 = seconds time out
          File2.delete(outDir + tDaveName + outExt);
        }

      } catch (Exception e) {
        String2.log(String2.ERROR + ": " + MustBe.throwableToString(e));
        trouble.add(inDir + nameList[i]);
      }
    }

    // done
    String2.log(
        "GridSaveAs finished. "
            + (nameList.length - trouble.size())
            + " out of "
            + nameList.length
            + " file(s) were converted in "
            + (System.currentTimeMillis() - time)
            + " ms.");
    if (trouble.size() > 0)
      String2.log("The unsuccessful files were:\n" + trouble.toNewlineString());
    return trouble;
  }

  /**
   * Convert a .grd file with a Dave-style name (e.g., "<dir>/AH2001067_2001069_sstd_westus.grd" for
   * composites, or "<dir>/AH2005060_044800h_sstd_westus.grd" for single passes) into a .nc file
   * with the specified name (with any style name) and lots of metadata (see setAttributes()).
   *
   * @param fullDaveGrdFileName the complete Dave-style file name of the extant .grd input file
   * @param fullNetCDFFileName the complete Dave-style file name of the extant .grd input file
   * @param fileNameUtility is used to generate all of the metadata based on the file name
   * @param makeLonPM180 If true, the lon values will be forced to be +-180. If false, the lon
   *     values will be forced to be 0..360. However, if some points are in the western hemisphere
   *     and some are in the eastern hemisphere, this parameter is disregarded and the lon values
   *     are not modified.
   * @throws Exception
   */
  /*    public static void convertDaveGrdToNetCDF(String fullDaveGrdFileName,
          String fullNetCDFFileName,
          FileNameUtility fileNameUtility, boolean makeLonPM180) throws Exception {

          //generate the CWBrowser version of the grd name
          String daveName = File2.getNameAndExtension(fullDaveGrdFileName);
          Test.ensureTrue(daveName.toLowerCase().endsWith(".grd"),
              String2.ERROR + "in Grid.convertDaveGrdToNetCDF: the input file name (" +
              fullDaveGrdFileName + ") must end in \".grd\".");
          daveName = daveName.substring(0, daveName.length() - 4);
          String cwBrowserName = FileNameUtility.convertDaveNameToCWBrowserName(daveName);

          //split the netCDF file name
          String ncDir = File2.getDirectory(fullNetCDFFileName);
          String ncName = File2.getNameAndExtension(fullNetCDFFileName);
          Test.ensureTrue(ncName.toLowerCase().endsWith(".nc"),
              String2.ERROR + "in Grid.convertDaveGrdToNetCDF: the ouput file name (" +
              fullNetCDFFileName + ") must end in \".nc\".");
          ncName = ncName.substring(0, ncName.length() - 3);

          //load the .grd file
          Grid grid = new Grid();
          grid.readGrd(fullDaveGrdFileName, makeLonPM180);

          //add the metadata
          grid.setAttributes(cwBrowserName, fileNameUtility, false);

          //generate the .nc file
          grid.saveAsNetCDF(ncDir, ncName, FileNameUtility.get6CharName(cwBrowserName));

      }
  */

  /**
   * Convert a .nc file with a Dave-style name (e.g., "<dir>/AH2001067_2001069_sstd_westus.grd" for
   * composites, or "<dir>/AH2005060_044800h_sstd_westus.grd" for single passes) into the specified
   * .grd file (with any style name).
   *
   * @param fullDaveNcFileName the complete Dave-style file name of the extant .nc input file
   * @param fullGrdFileName the complete name (any style) for the .grd file.
   * @param makeLonPM180 If true, the lon values will be forced to be +-180. If false, the lon
   *     values will be forced to be 0..360. However, if some points are in the western hemisphere
   *     and some are in the eastern hemisphere, this parameter is disregarded and the lon values
   *     are not modified.
   * @throws Exception
   */
  /*  public static void convertDaveNetCDFToGrd(String fullDaveNcFileName,
          String fullGrdFileName, boolean makeLonPM180) throws Exception {

          //generate the CWBrowser version of the ncName
          String daveName = File2.getNameAndExtension(fullDaveNcFileName);
          Test.ensureTrue(daveName.toLowerCase().endsWith(".nc"),
              String2.ERROR + " in Grid.convertDaveNetCDFToGrd: the input file name (" +
              fullDaveNcFileName + ") must end in \".nc\".");
          daveName = daveName.substring(0, daveName.length() - 3);
          String cwBrowserName = FileNameUtility.convertDaveNameToCWBrowserName(daveName);

          //split the grd file name
          String grdDir = File2.getDirectory(fullGrdFileName);
          String grdName = File2.getNameAndExtension(fullGrdFileName);
          Test.ensureTrue(grdName.toLowerCase().endsWith(".grd"),
              String2.ERROR + "in Grid.convertDaveNetCDFToGrd: the ouput file name (" +
              fullGrdFileName + ") must end in \".grd\".");
          grdName = grdName.substring(0, grdName.length() - 4);

          //load the netCDF file
          Grid grid = new Grid();
          grid.readNetCDF(fullDaveNcFileName,
              FileNameUtility.get6CharName(cwBrowserName), makeLonPM180);

          //generate the .grd file
          grid.saveAsGrd(grdDir, grdName);

      }
  */
  /**
   * This tests convertGrdToNc.main and convertNcToGrd.main and the local methods
   * convertDaveGrdToDaveNetCDF and convertDaveNetCDFToDaveGrd.
   *
   * @param fileNameUtility
   * @throws Exception if trouble
   */
  /*    public static void testConvertGrdToFromNetCDF() throws Exception {

          String2.log("\n*** Grid.testConvertGrdToNc");
          String errorIn = String2.ERROR + " in Grid.testConvertGrdToNc: ";

          //copy the .grd file to a Dave-style name
          String cwName = testName;
          FileNameUtility fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser");
          String daveName = fnu.convertCWBrowserNameToDaveName(cwName);
          Test.ensureTrue(
              File2.copy(testDir + cwName + ".grd",  testDir + daveName + ".grd"),
              errorIn + "copy " + testDir + cwName + ".grd.");

          //read the .grd file
          Grid grid1 = new Grid();
          grid1.readGrd(testDir + daveName + ".grd", false); //false=not pm180

          //*** make the .nc file and delete the grd file
          File2.verbose = true;
          ConvertGrdToNc.main(new String[]{
              testDir + daveName + ".grd",
              testDir + daveName + ".nc"});
          Test.ensureTrue(File2.delete(testDir + daveName + ".grd"),
              errorIn + " while deleting " + testDir + daveName + ".grd");

          //read .nc file
          Grid grid2 = new Grid();
          grid2.readNetCDF(testDir + daveName + ".nc",
              FileNameUtility.get6CharName(cwName), false); //false=not pm180

          //are they the same?
          Test.ensureTrue(grid1.equals(grid2), errorIn);

          //*** make the .grd file and delete the .nc file
          ConvertNcToGrd.main(new String[]{
              testDir + daveName + ".nc",
              testDir + daveName + ".grd"});
          Test.ensureTrue(File2.delete(testDir + daveName + ".nc"),
              errorIn + " while deleting " + testDir + daveName + ".nc");

          //read .grd file
          Grid grid3 = new Grid();
          grid3.readGrd(testDir + daveName + ".grd", false); //false=not pm180

          //are they the same?
          Test.ensureTrue(grid1.equals(grid3), errorIn);

          //delete the .grd file
  //        Test.ensureTrue(File2.delete(testDir + daveName + ".grd"),
  //            errorIn + " while deleting " + testDir + daveName + ".grd");

          //FUTURE: check metadata
      }
  */

  /**
   * This converts a .nc file with a 3D grid data(time, lat, lon) (integers) into an .xyz file.
   * Missing data values will be written as "NaN".
   *
   * @param inName the full name of the input .nc file, e.g., "c:/temp/temp/sst.mnmean.nc".
   * @param outName the full name of the output .xyz file, e.g., "c:/temp/temp/sst.mnmean.xyz".
   * @param xName the name of the x variable in the .nc file, e.g., "lon".
   * @param yName the name of the y variable in the .nc file, e.g., "lat".
   * @param tName the name of the time variable in the .nc file, e.g., "time".
   * @param dataName the name of the data variable in the .nc file, e.g., "sst".
   * @throws Exception if touble
   */
  public static void ncXYTtoAsciiXYT(
      String inName, String outName, String xName, String yName, String tName, String dataName)
      throws Exception {

    String newline = String2.lineSeparator;

    // open the netcdf file
    String2.log("ncXYTtoAsciiXYT: " + inName + " to " + outName);

    NetcdfFile ncFile = NcHelper.openFile(inName);
    try {
      // read the dimensions
      double[] xArray = NcHelper.getNiceDoubleArray(ncFile.findVariable(xName), 0, -1).toArray();
      double[] yArray = NcHelper.getNiceDoubleArray(ncFile.findVariable(yName), 0, -1).toArray();
      double[] tArray = NcHelper.getNiceDoubleArray(ncFile.findVariable(tName), 0, -1).toArray();
      String2.log("xArray=" + xArray[0] + " to " + xArray[xArray.length - 1]);
      String2.log("yArray=" + yArray[0] + " to " + yArray[yArray.length - 1]);
      String2.log("tArray=" + tArray[0] + " to " + tArray[tArray.length - 1]);

      // convert t ("days since 1800-1-1 00:00:00") to iso
      String tStrings[] = new String[tArray.length];
      for (int t = 0; t < tArray.length; t++) {
        GregorianCalendar base = Calendar2.newGCalendarZulu(1800, 1, 1);
        base.add(Calendar2.DATE, Math2.roundToInt(tArray[t]));
        tStrings[t] = Calendar2.formatAsISODate(base);
      }
      String2.log("tStrings=" + tStrings[0] + " to " + tStrings[tStrings.length - 1]);

      // read the data   sst(time,lat,lon)
      Variable dataVariable = ncFile.findVariable(dataName);
      ArrayShort.D3 data = (ArrayShort.D3) dataVariable.read();

      // get the scaleFactor
      Attribute attribute = dataVariable.findAttribute("scale_factor");
      double scaleFactor = attribute == null ? 1 : NcHelper.getNiceDouble(attribute);

      // get the addOffset
      attribute = dataVariable.findAttribute("add_offset");
      double addOffset = attribute == null ? 0 : NcHelper.getNiceDouble(attribute);

      // get the missingValue
      attribute = dataVariable.findAttribute("missing_value");
      int missingValue =
          attribute == null
              ? Integer.MAX_VALUE
              : Math2.roundToInt(NcHelper.getNiceDouble(attribute));

      String2.log(
          "missingValue="
              + scaleFactor
              + " addOffset="
              + addOffset
              + " missingValue="
              + missingValue);

      // write to ascii
      Writer ascii = File2.getBufferedFileWriter88591(outName);
      try {
        String s = xName + "\t" + yName + "\t" + tName + "\t" + dataName + newline;
        ascii.write(s, 0, s.length());
        for (int t = 0; t < tArray.length; t++) {
          if ((t % 10) == 0) String2.log("writing t=" + t + " of " + tArray.length);
          for (int y = 0; y < yArray.length; y++) {
            for (int x = 0; x < xArray.length; x++) {
              int datum = data.get(t, y, x);
              s =
                  xArray[x]
                      + "\t"
                      + yArray[y]
                      + "\t"
                      + tStrings[t]
                      + "\t"
                      + (datum == missingValue
                          ? "NaN"
                          :
                          // treat like float; don't let scaling add excess digits
                          String2.genEFormat6(datum * scaleFactor + addOffset))
                      + newline;
              ascii.write(s, 0, s.length());
            }
          }
        }
      } finally {
        try {
          ascii.close();
        } catch (Exception e) {
        }
      }
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * Given an xytd land mask ascii file (0=land 1=sea) and an xytd ascii file with the same grid
   * (although the ascii file can be a subset), this replaces the data values in the ascii files
   * with NaN where there is land.
   *
   * @param landMaskFileName An ISO-8859-1 file, perhaps externally compressed.
   * @param dataFileName An ISO-8859-1 file, perhaps externally compressed.
   * @param newFileName
   */
  public static void maskAsciiXYT(String landMaskFileName, String dataFileName, String newFileName)
      throws Exception {

    String s;
    String newline = String2.lineSeparator;

    // read the land mask into a hash table
    HashSet<String> hashSet = new HashSet();
    BufferedReader maskFile = File2.getDecompressedBufferedFileReader88591(landMaskFileName);
    try {
      s = maskFile.readLine(); // skip col names
      s = maskFile.readLine();
      while (s != null) {
        // store land bits to the hashSet
        String fields[] = String2.split(s, '\t'); // x,y,t,d
        if (fields[3].equals("0")) // land
        hashSet.add(fields[0] + fields[1]);

        // read the next line
        s = maskFile.readLine();
      }
    } finally {
      maskFile.close();
    }

    // read the asciiFile and write new newFile
    BufferedReader in = File2.getDecompressedBufferedFileReader88591(dataFileName);
    try {
      BufferedWriter out = File2.getBufferedFileWriter88591(newFileName);
      try {
        s = in.readLine(); // skip col names
        s = in.readLine();
        while (s != null) {
          // replace land data with NaN
          String fields[] = String2.split(s, '\t'); // x,y,t,d
          if (hashSet.contains(fields[0] + fields[1])) fields[3] = "NaN";
          s = String2.toSVString(fields, "\t", false) + newline;
          out.write(s, 0, s.length());

          // read the next line
          s = in.readLine();
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }
}
