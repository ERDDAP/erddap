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
import com.google.common.collect.ImmutableList;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import ucar.ma2.*;
import ucar.nc2.*;
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
  public static final boolean doExtraErrorChecking = false;

  /* A string used by climatology datasets. */
  public static final String SINCE_111 = "since 0001-01-01";

  /** The attribute lists are used sporadically; see method documentation. */
  private final Attributes globalAttributes = new Attributes();

  private final Attributes latAttributes = new Attributes();
  private final Attributes lonAttributes = new Attributes();
  private final Attributes dataAttributes = new Attributes();

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
    try (RandomAccessFile raf = new RandomAccessFile(fullFileName, "r")) {

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
    }
    if (verbose) String2.log("Grid.readBinary TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

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
    try (NetcdfFile grdFile = NcHelper.openFile(fullFileName)) {
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
    try (NetcdfFile ncFile = NcHelper.openFile(fullFileName)) {

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
      for (Object o : list) {
        Variable variable = (Variable) o;
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
        List<Variable> rootGroupVariables = rootGroup.getVariables();
        Dimension latDim = latVariable.getDimensions().getFirst();
        Dimension lonDim = lonVariable.getDimensions().getFirst();
        for (Variable rootGroupVariable : rootGroupVariables) {
          dataVariable = rootGroupVariable;
          List<Dimension> tDimensions = dataVariable.getDimensions();
          boolean latUsed = false;
          boolean lonUsed = false;
          for (Dimension tDimension : tDimensions) {
            if (tDimension.equals(latDim)) latUsed = true;
            if (tDimension.equals(lonDim)) lonUsed = true;
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
      } else {
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
      }
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
        data.length, nLon * (long) nLat, "data.length != nLon(" + nLon + ") * nLat(" + nLat + ")");

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
    for (double v : tLon) {
      int which = Math2.binaryFindClosest(lon, v);
      if (!Math2.almostEqual(5, lon[which], v)) {
        which = Math2.binaryFindClosest(lon, v - 360);
        if (!Math2.almostEqual(5, lon[which], v - 360)) {
          which = Math2.binaryFindClosest(lon, v + 360);
          if (!Math2.almostEqual(5, lon[which], v + 360)) {
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
    for (double d : dar) {
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
    String ext = SAVE_AS_EXTENSIONS.get(SAVE_AS_NETCDF);
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
          NcHelper.addVariable(rootGroup, "time", DataType.DOUBLE, List.of(timeDimension));

      ArrayDouble.D1 tAltitude = new ArrayDouble.D1(1);
      tAltitude.set(0, 0); // I treat all as altitude=0 !!!!
      Variable.Builder altitudeVar =
          NcHelper.addVariable(rootGroup, "altitude", DataType.DOUBLE, List.of(altitudeDimension));

      ArrayDouble.D1 tLat = new ArrayDouble.D1(nLat);
      for (int i = 0; i < nLat; i++) tLat.set(i, lat[i]);
      Variable.Builder latVar =
          NcHelper.addVariable(rootGroup, "lat", DataType.DOUBLE, List.of(latDimension));

      ArrayDouble.D1 tLon = new ArrayDouble.D1(nLon);
      for (int i = 0; i < nLon; i++) tLon.set(i, lon[i]);
      Variable.Builder lonVar =
          NcHelper.addVariable(rootGroup, "lon", DataType.DOUBLE, List.of(lonDimension));

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
      for (String s : names) {
        // any existing et_affine needs to be modified, since .nc has data right-side-up
        // I suspect CDAT georeferences this correctly but with coast and data upside down.
        if (s.equals("et_affine")) {
          // lon = a*row + c*col + e
          // lat = b*row + d*col + f
          double matrix[] = {0, latSpacing, lonSpacing, 0, lon[0], lat[0]}; // right side up
          rootGroup.addAttribute(
              NcHelper.newAttribute(
                  "et_affine", NcHelper.get1DArray(matrix, false))); // float64[] {a, b, c, d, e, f}
        } else {
          rootGroup.addAttribute(NcHelper.newAttribute(nc3Mode, s, globalAttributes.get(s)));
        }
      }

      // time attributes
      if (hasTime)
        timeVar.addAttribute(
            NcHelper.newAttribute(
                "actual_range",
                NcHelper.get1DArray(new double[] {centeredTimeDouble, centeredTimeDouble}, false)));
      timeVar.addAttribute(new Attribute("fraction_digits", 0));
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
      altitudeVar.addAttribute(new Attribute("fraction_digits", 0));
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
    String ext = SAVE_AS_EXTENSIONS.get(SAVE_AS_GRD);
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
    try {
      NetcdfFormatWriter.Builder grd = NetcdfFormatWriter.createNewNetcdf3(directory + randomInt);
      Group.Builder rootGroup = grd.getRootGroup();
      grd.setFill(false);
      int nLat = lat.length;
      int nLon = lon.length;

      Dimension sideDimension = NcHelper.addDimension(rootGroup, "side", 2);
      Dimension xysizeDimension = NcHelper.addDimension(rootGroup, "xysize", nLon * nLat);
      List<Dimension> sideDimList = List.of(sideDimension);
      List<Dimension> xysizeDimList = List.of(xysizeDimension);

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
      zVar.addAttribute(new Attribute("scale_factor", 1.0));
      zVar.addAttribute(new Attribute("add_offset", 0.0));
      zVar.addAttribute(new Attribute("node_offset", 0));

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
  public static final ImmutableList<String> SAVE_AS_EXTENSIONS =
      ImmutableList.of(".grd", ".asc", ".hdf", ".mat", ".xyz", ".tif", ".nc", ".asc");
}
