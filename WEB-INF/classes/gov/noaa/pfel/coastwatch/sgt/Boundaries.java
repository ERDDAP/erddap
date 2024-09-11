/*
 * Boundaries Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.LRUCache;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pmel.sgt.dm.*;
import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/** This class has methods related to National and State Boundaries. */
public class Boundaries {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
   * if you want lots and lots of diagnostic messages sent to String2.log.
   */
  public static boolean reallyVerbose = false;

  /**
   * Set this to true (by calling debug=true in your program, not by changing the code here) if you
   * want lots and lots and lots of diagnostic messages sent to String2.log.
   */
  public static boolean debug = false;

  public static final String REF_DIRECTORY =
      EDStatic.getWebInfParentDirectory()
          + // with / separator and / at the end
          "WEB-INF/ref/";

  /**
   * The nationalBoundary and stateBoundary files must be in the refDirectory. "gshhs_?.b"
   * (?=f|h|i|l|c) files. The files are from the GSHHS project
   * (https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html 2009-10-28 v2.0). GPL license:
   * http://www.soest.hawaii.edu/pwessel/gshhs/README.TXT landMaskDir should have slash at end.
   */
  public String directory =
      EDStatic.getWebInfParentDirectory()
          + // with / separator and / at the end
          "WEB-INF/ref/";

  /**
   * Since boundary SGTLines are time-consuming to contruct, this caches the last cacheSize used
   * SgtLines. <br>
   * Memory for each cached GP (CWBrowser typical use) is 2KB to 500KB (whole world, crude), (20KB
   * is typical). <br>
   * Crude estimate: 100 takes 2 MB per type. <br>
   * There are 3 types of boundaries (National, State, Rivers).
   */
  public static final int CACHE_SIZE = 100;

  private Map cache = Collections.synchronizedMap(new LRUCache(CACHE_SIZE));
  private int nCoarse = 0;
  private int nSuccesses = 0;
  private int nTossed = 0;

  /** This identifies the subclass as National or State. */
  private String id = "";

  // national, state, and river Boundaries are nulls until needed
  // public access via getNationalBoundary...
  // I got national, state, river boundaries from GMT:
  // In general: pscoast -Df -N1 -M -Rwest/east/south/north >! file
  // I used:     pscoast -Df -N1 -M -R-180/180/-90/90 >! nationalBoundariesf.asc
  // where "f"ull can be changed to "h"igh, "i"ntermediate, "l"ow, "c"rude.
  // where -Rd is shorthand
  // where "N1" (national) can be changed to "N2" (state - only Americas are available)
  //   e.g., String2.getContextDirectory() + "WEB-INF/ref/nationalBoundariesf.asc"
  // The resulting files use the GMT_FORMAT
  // The source files must be in fullRefDirectory.
  // ??? Are there newer national boundary files (not showing USSR)???
  // 2009-10-28 Bob regenerated all files (and added riverBoundariesx.asc) with GMT 4.5.1
  // 2011-03-14 Bob regenerated all files with GMT 4.5.6
  // I used f:/programs/GMT/bin/:
  //   pscoast -Df -N1 -m -Rd > f:\projects\boundaries\nationalBoundariesf.asc   //and h i l c
  //   pscoast -Df -N2 -m -Rd > f:\projects\boundaries\stateBoundariesf.asc      //and h i l c
  //   pscoast -Df -Ir -m -Rd > f:\projects\boundaries\riversf.asc
  //   pscoast -Dh -Ir -m -Rd > f:\projects\boundaries\riversh.asc
  //   pscoast -Di -Ir -m -Rd > f:\projects\boundaries\riversi.asc
  //   pscoast -Dl -I1 -I2 -I3 -m -Rd > f:\projects\boundaries\riversl.asc
  //   pscoast -Dc -I1 -m -Rd > f:\projects\boundaries\riversc.asc
  // States includes Americas and Australia
  // !!!EEK!!! no Sacramento River! gshhs is based on 2 datasets:
  //  World Vector Shorelines (WVS) and CIA World Data Bank II (WDBII).
  //  Perhaps each thinks the other is responsible for it.
  private String[] fileNames;

  public static final String NATIONAL_FILE_NAMES[] = {
    "nationalBoundariesf.double",
    "nationalBoundariesh.double",
    "nationalBoundariesi.double",
    "nationalBoundariesl.double",
    "nationalBoundariesc.double"
  };
  // ??? Why are there state boundaries for all of Americas, but not Australia? Mistake on my
  // part???
  public static final String STATE_FILE_NAMES[] = {
    "stateBoundariesf.double",
    "stateBoundariesh.double",
    "stateBoundariesi.double",
    "stateBoundariesl.double",
    "stateBoundariesc.double"
  };
  public static final String RIVER_FILE_NAMES[] = {
    "riversf.double", "riversh.double", "riversi.double", "riversl.double", "riversc.double"
  };

  public static final int MATLAB_FORMAT = 0;
  public static final int GMT_FORMAT = 1;
  private int fileFormat = GMT_FORMAT;

  /**
   * The constructor.
   *
   * @param id e.g., "National", "State", "River"
   * @param directory the directory with the fileNames
   * @param fileNames the 5 file names
   * @param fileFormat MATLAB_FORMAT or GMT_FORMAT
   */
  public Boundaries(String id, String directory, String fileNames[], int fileFormat) {
    this.id = id;
    this.directory = directory;
    this.fileNames = fileNames;
    this.fileFormat = fileFormat;
  }

  /**
   * This gets the SGTLine with the relevant boundary information. This is thread-safe because the
   * cache is thread-safe.
   *
   * @param resolution 0='f'ull, 1='h'igh, 2='i'ntermediate, 3='l'ow, 4='c'rude.
   * @param west usually 0..360 or +/-180, but -720 to 720 is supported
   * @param east usually 0..360 or +/-180, but -720 to 720 is supported
   * @param south +/-90
   * @param north +/-90
   * @return a SgtLine with the requested boundaries.
   * @throws exception if trouble
   */
  public SGTLine getSgtLine(int resolution, double west, double east, double south, double north)
      throws Exception {

    String cachedName =
        String2.canonical(
            resolution
                + "W"
                + String2.genEFormat10(west)
                + "E"
                + String2.genEFormat10(east)
                + "S"
                + String2.genEFormat10(south)
                + "N"
                + String2.genEFormat10(north));
    if (reallyVerbose) String2.log("  Boundaries.getSgtLine " + id + " request=" + cachedName);
    long time = System.currentTimeMillis();
    String tCoarse = "";
    String tSuccess = "";
    String tTossed = "";
    SGTLine sgtLine;

    if (resolution >= 3) {

      // Don't cache it. Coarse resolutions are always fast because source file is small.
      // And the request is usually a large part of whole world. Most paths will be used.
      nCoarse++;
      tCoarse = "*";
      sgtLine = readSgtLineDouble(directory + fileNames[resolution], west, east, south, north);

    } else {

      // Cache it. Higher resolutions are slower because source file is bigger.
      //  Request is a very small part of whole world. Most paths will be rejected.
      // Thread-safe creation of the SGTLine
      //  If there are almost simultaneous requests for the same one,
      //  only one thread will make it.
      // Cache is thread-safe so synch on cachedName, not cache.
      ReentrantLock lock = String2.canonicalLock(cachedName);
      if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
        throw new TimeoutException("Timeout waiting for lock on Boundaries cachedName.");
      try {

        // *** is SGTLine in cache?
        sgtLine = (SGTLine) cache.get(cachedName);
        if (sgtLine == null) {

          // not in cache, make SgtLine
          sgtLine = readSgtLineDouble(directory + fileNames[resolution], west, east, south, north);

          // cache full?
          if (cache.size() == CACHE_SIZE) {
            nTossed++;
            tTossed = "*";
          } else {
            // if cache wasn't full, treat as success
            nSuccesses++;
            tSuccess = "*";
          }

          // add new path to cache
          cache.put(cachedName, sgtLine);

        } else {

          // yes, it is in cache;
          nSuccesses++;
          tSuccess = "*(alreadyInCache)";
        }
      } finally {
        lock.unlock();
      }
    }

    // return sgtLine
    int thisKB = sgtLine.getXArray().length / 64; // *16/1024  //16 because x,y, both doubles
    if (reallyVerbose)
      String2.log(
          "    Boundaries.getSgtLine "
              + id
              + " notInCache done thisKB="
              + thisKB
              + " time="
              + (System.currentTimeMillis() - time)
              + "ms\n"
              + "      nCoarse="
              + nCoarse
              + tCoarse
              + " nSuccesses="
              + nSuccesses
              + tSuccess
              + " nTossed="
              + nTossed
              + tTossed);
    return sgtLine;
  }

  /**
   * This creates an SGTLine from the line (e.g., boundary) info in an SGTLine object. This is the
   * newer version that reads from .double files.
   *
   * @param fullFileName the full name of the .double format source file
   * @param requestMinX usually 0..360 or +/-180, but -720 to 720 is supported (e.g., 74 to 434, see
   *     EDDGridFromDap.testMap74to434() and SgtMap.testBathymetry() and SgtMap.testTopography() )
   * @param requestMaxX usually 0..360 or +/-180, but -720 to 720 is supported
   * @param requestMinY
   * @param requestMaxY
   * @return an SGTLine object
   * @throws Exception if trouble
   */
  public static SGTLine readSgtLineDouble(
      String fullFileName,
      double requestMinX,
      double requestMaxX,
      double requestMinY,
      double requestMaxY)
      throws Exception {

    // if (reallyVerbose) String2.log("    readSGTLine");
    DoubleArray lat = new DoubleArray(); // accumlate the results
    DoubleArray lon = new DoubleArray();
    double shift[] = {-720, -360, 0, 360};
    boolean doShift[] = new boolean[4];
    DoubleArray tempLat[] = new DoubleArray[4]; // shift [left720,left360,0,right360]
    DoubleArray tempLon[] = new DoubleArray[4];
    for (int i = 0; i < 4; i++) {
      tempLat[i] = new DoubleArray();
      tempLon[i] = new DoubleArray();
    }
    int nObjects = 0, nLatSkip = 0, nLonSkip = 0, nKeep = 0;

    DataInputStream dis =
        new DataInputStream(File2.getDecompressedBufferedInputStream(fullFileName));
    try {
      // double minMinLon = 1e10, maxMaxLon = -1e10;   //file has minMinLon=6.10360875868E-4
      // maxMaxLon=359.99969482
      while (true) {
        // read a path

        // first value, first row should be NaN
        if (!Double.isNaN(dis.readDouble())) {
          // dis will be closed in finally{} below
          throw new RuntimeException(
              "Unexpected finite value at beginning of path in " + fullFileName);
        }

        // second value, first row is nPoints
        int nPoints = Math2.roundToInt(dis.readDouble());
        if (nPoints <= 0 || nPoints == Integer.MAX_VALUE) {
          // end of file
          break;
        }

        // read path's minLon minLat
        double minLon = dis.readDouble();
        double minLat = dis.readDouble();
        double maxLon = dis.readDouble();
        double maxLat = dis.readDouble();

        // if (debug) {
        //    minMinLon = Math.min(minMinLon, minLon);
        //    maxMaxLon = Math.max(maxMaxLon, maxLon);
        // }

        // lat test is easy
        if (minLat > requestMaxY || maxLat < requestMinY) {
          // skip this path
          nLatSkip++;
          dis.skipBytes(nPoints * 16);
          continue;
        }

        // lon test: always check for no overlap of request (min/requestMaxX)
        //   and this path's original position, and shifted right
        // |requestHere?| |-720|     |requestHere?|
        // |requestHere?| |-360|     |requestHere?|
        // |requestHere?| |original| |requestHere?|
        // |requestHere?| |+360|     |requestHere?|
        boolean displayIt = false;
        for (int i = 0; i < 4; i++) {
          doShift[i] = !(minLon + shift[i] >= requestMaxX || maxLon + shift[i] <= requestMinX);
          if (doShift[i]) displayIt = true;
        }
        if (debug)
          String2.log(
              "> doShift -720="
                  + doShift[0]
                  + " -360="
                  + doShift[1]
                  + " 0="
                  + doShift[2]
                  + " 360="
                  + doShift[3]
                  + " requestX="
                  + requestMinX
                  + " "
                  + requestMaxX
                  + " polyLon="
                  + minLon
                  + " "
                  + maxLon);
        if (!displayIt) {
          // skip this path
          nLonSkip++;
          dis.skipBytes(nPoints * 16);
          continue;
        }
        nKeep++;

        // read the points
        double oLon = 0; // irrelevant
        double oLat = 0;
        double tLon = 0;
        double tLat = 0;
        double polyMinLon = 1e10,
            polyMaxLon = -1e10; // see if actualy poly lon range is as promised
        for (int point = 0; point < nPoints; point++) {
          oLon = tLon;
          oLat = tLat;
          tLon = dis.readDouble();
          tLat = dis.readDouble();
          if (debug) {
            polyMinLon = Math.min(polyMinLon, tLon);
            polyMaxLon = Math.max(polyMaxLon, tLon);
          }
          // cut lines going from one edge of world to the other
          for (int i = 0; i < 4; i++) {
            if (doShift[i]) {
              // does this polyline wrap around 0 <--> 360?
              if (tempLon[i].size() > 0 && Math.abs(oLon - tLon) > 180.0) {
                // try to add this subpath
                if (oLon < tLon) // to make not disjoint,
                lon.add(tLon + shift[i] - 360); //  shift this pt to left
                else lon.add(tLon + shift[i] + 360); //  shift this pt to right
                lat.add(tLat);
                int tn =
                    GSHHS.reduce(
                        tempLat[i].size(),
                        tempLon[i].array,
                        tempLat[i].array,
                        requestMinX,
                        requestMaxX,
                        requestMinY,
                        requestMaxY);
                tempLat[i].removeRange(tn, tempLat[i].size());
                tempLon[i].removeRange(tn, tempLon[i].size());
                if (tn > 0) {
                  lon.append(tempLon[i]);
                  lat.append(tempLat[i]);
                  lon.add(Double.NaN); // break in line
                  lat.add(Double.NaN);
                  nObjects++;
                }
                tempLon[i].clear();
                tempLat[i].clear();
              }
              tempLon[i].add(tLon + shift[i]);
              tempLat[i].add(tLat);
            }
          }
        }
        if (debug) {
          if (polyMinLon < minLon || polyMaxLon > maxLon)
            String2.pressEnterToContinue(
                //                    String2.log(
                "> Trouble: promisedLon="
                    + minLon
                    + " "
                    + maxLon
                    + " actualLon="
                    + polyMinLon
                    + " "
                    + polyMaxLon);
        }

        // try to add this subpath
        for (int i = 0; i < 4; i++) {
          if (tempLat[i].size() > 0) {
            int tn =
                GSHHS.reduce(
                    tempLat[i].size(),
                    tempLon[i].array,
                    tempLat[i].array,
                    requestMinX,
                    requestMaxX,
                    requestMinY,
                    requestMaxY);
            if (tn > 0) {
              tempLat[i].removeRange(tn, tempLat[i].size());
              tempLon[i].removeRange(tn, tempLon[i].size());
              lon.append(tempLon[i]);
              lat.append(tempLat[i]);
              lon.add(Double.NaN); // break in line
              lat.add(Double.NaN);
              nObjects++;
            }
            tempLon[i].clear();
            tempLat[i].clear();
          }
        }
      }
    } finally {
      dis.close();
    }
    if (reallyVerbose)
      String2.log(
          "    Boundaries.readSgtLine nLatSkip="
              + nLatSkip
              + " nLonSkip="
              + nLonSkip
              + " nKeep="
              + nKeep
              + " nObjects="
              + nObjects);
    // if (debug) String2.log(">>>      minMinLon=" + minMinLon + " maxMaxLon=" + maxMaxLon);

    lon.trimToSize();
    lat.trimToSize();

    SimpleLine line = new SimpleLine(lon.array, lat.array, "Boundary");
    line.setXMetaData(new SGTMetaData("Longitude", "degrees_E", false, true));
    line.setYMetaData(new SGTMetaData("Latitude", "degrees_N", false, false));

    // for (int i = 0; i < lon.size(); i++)
    //    String2.log(String2.left("" + i, 5) +
    //        String2.left("" + lon.get(i), 18) + String2.left("" + lat.get(i), 18));

    return line;
  }

  /**
   * This is the older version that reads from .asc data files. [This probably doesn't work for
   * min/requestMaxX&gt;360
   */
  public static SGTLine readSgtLineAsc(
      String fullFileName,
      int format,
      double requestMinX,
      double requestMaxX,
      double requestMinY,
      double requestMaxY)
      throws Exception {

    // if (reallyVerbose) String2.log("    readSGTLine");

    boolean lonPM180 = requestMinX < 0;
    DoubleArray lat = new DoubleArray();
    DoubleArray lon = new DoubleArray();
    DoubleArray tempLat = new DoubleArray();
    DoubleArray tempLon = new DoubleArray();
    String startGapLine1 = format == MATLAB_FORMAT ? "nan nan" : ">";
    String startGapLine2 = format == MATLAB_FORMAT ? "nan nan" : "#";
    int nObjects = 0;

    BufferedReader bufferedReader = File2.getDecompressedBufferedFileReader88591(fullFileName);
    try {
      String s = bufferedReader.readLine();
      while (s != null) { // null = end-of-file
        if (s.startsWith(startGapLine1) || s.startsWith(startGapLine2)) {
          // try to add this subpath
          int tn =
              GSHHS.reduce(
                  tempLat.size(),
                  tempLon.array,
                  tempLat.array,
                  requestMinX,
                  requestMaxX,
                  requestMinY,
                  requestMaxY);
          tempLat.removeRange(tn, tempLat.size());
          tempLon.removeRange(tn, tempLon.size());
          if (tn > 0) {
            lon.append(tempLon);
            lat.append(tempLat);
            lon.add(Double.NaN);
            lat.add(Double.NaN);
            nObjects++;
          }
          tempLon.clear();
          tempLat.clear();
        } else {
          // each line: x\ty
          String[] items = String2.split(s, '\t');
          if (items.length == 2) {
            double tLon = String2.parseDouble(items[0]);
            double tLat = String2.parseDouble(items[1]);
            if (lonPM180) {
              if (tLon >= 180) tLon -= 360;
            } else {
              if (tLon < 0) tLon += 360;
            }
            // cut lines going from one edge of world to the other
            if (tempLon.size() > 0 && Math.abs(tLon - tempLon.get(tempLon.size() - 1)) > 50.0) {
              // try to add this subpath
              int tn =
                  GSHHS.reduce(
                      tempLat.size(),
                      tempLon.array,
                      tempLat.array,
                      requestMinX,
                      requestMaxX,
                      requestMinY,
                      requestMaxY);
              tempLat.removeRange(tn, tempLat.size());
              tempLon.removeRange(tn, tempLon.size());
              if (tn > 0) {
                lon.append(tempLon);
                lat.append(tempLat);
                lon.add(Double.NaN);
                lat.add(Double.NaN);
                nObjects++;
              }
              tempLon.clear();
              tempLat.clear();
            }
            tempLon.add(tLon);
            tempLat.add(tLat);
          } else
            String2.log(
                String2.ERROR + " at readSGTLine items.length!=2 (" + items.length + ") s=" + s);
        }
        s = bufferedReader.readLine();
      }
    } finally {
      bufferedReader.close();
    }
    if (reallyVerbose) String2.log("    Boundaries.readSgtLine nObjects=" + nObjects);

    lon.trimToSize();
    lat.trimToSize();

    SimpleLine line = new SimpleLine(lon.array, lat.array, "Boundary");
    line.setXMetaData(new SGTMetaData("Longitude", "degrees_E", false, true));
    line.setYMetaData(new SGTMetaData("Latitude", "degrees_N", false, false));

    return line;
  }

  /**
   * This converts an .asc SGTLine datafile into a .double SGTLine datafile. The .double file has
   * groups of pairs of doubles: <br>
   * NaN, nPoints <br>
   * minLon, minLat <br>
   * maxLon, maxLat <br>
   * nPoints pairs of lon, lat
   *
   * <p>The end of the files has NaN, NaN.
   *
   * @throws Exception if trouble
   */
  public static void convertSgtLine(String sourceName, String destName) throws Exception {
    int format = GMT_FORMAT;

    String2.log("convertSgtLine\n in:" + sourceName + "\nout:" + destName);
    DoubleArray lat = new DoubleArray();
    DoubleArray lon = new DoubleArray();
    DoubleArray tempLat = new DoubleArray();
    DoubleArray tempLon = new DoubleArray();
    String startGapLine1 = format == MATLAB_FORMAT ? "nan nan" : ">";
    String startGapLine2 = format == MATLAB_FORMAT ? "nan nan" : "#";
    int nObjects = 0;

    BufferedReader bufferedReader = File2.getDecompressedBufferedFileReader88591(sourceName);
    try {
      DataOutputStream dos =
          new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destName)));
      try {
        String s = bufferedReader.readLine();
        while (true) {
          if (s == null
              || // null = end-of-file
              s.startsWith(startGapLine1)
              || s.startsWith(startGapLine2)) {
            // try to add this subpath
            if (tempLat.size() > 0) {
              double lonStats[] = tempLon.calculateStats();
              double latStats[] = tempLat.calculateStats();
              dos.writeDouble(Double.NaN);
              dos.writeDouble(tempLat.size());
              dos.writeDouble(lonStats[PrimitiveArray.STATS_MIN]);
              dos.writeDouble(latStats[PrimitiveArray.STATS_MIN]);
              dos.writeDouble(lonStats[PrimitiveArray.STATS_MAX]);
              dos.writeDouble(latStats[PrimitiveArray.STATS_MAX]);
              for (int i = 0; i < tempLat.size(); i++) {
                dos.writeDouble(tempLon.get(i));
                dos.writeDouble(tempLat.get(i));
              }
              nObjects++;
            }
            tempLon.clear();
            tempLat.clear();
            if (s == null) break;
          } else {
            // each line: x\ty
            String[] items = String2.split(s, '\t');
            if (items.length == 2) {
              double tLon = String2.parseDouble(items[0]);
              double tLat = String2.parseDouble(items[1]);
              if (!Double.isNaN(tLon) && !Double.isNaN(tLat)) {
                tempLon.add(tLon);
                tempLat.add(tLat);
              } else {
                // bufferedReader and dos closed by finally{} below
                throw new RuntimeException(String2.ERROR + " in convertSGTLine, s=" + s);
              }
            }
          }
          s = bufferedReader.readLine();
        }
        // mark of file
        dos.writeDouble(Double.NaN);
        dos.writeDouble(Double.NaN);
      } finally {
        dos.close();
      }
    } finally {
      bufferedReader.close();
    }
    String2.log("    Boundaries.convertSgtLine nObjects=" + nObjects);
  }

  /** Bob reruns this whenever there is new data to convert all of the data files. */
  public static void bobConvertAll() throws Exception {
    String dir = "c:/programs/boundaries/";
    String types[] = {"nationalBoundaries", "stateBoundaries", "rivers"};
    String ress[] = {"c", "f", "h", "i", "l"};
    for (int type = 0; type < types.length; type++) {
      for (int res = 0; res < ress.length; res++) {
        convertSgtLine(
            dir + types[type] + ress[res] + ".asc", dir + types[type] + ress[res] + ".double");
      }
    }
  }

  /** This returns a stats string for Boundaries. */
  public String statsString() {
    return id
        + ": nCached="
        + cache.size()
        + " of "
        + CACHE_SIZE
        + ",  nCoarse="
        + nCoarse
        + ", nSuccesses="
        + nSuccesses
        + ", nTossed="
        + nTossed;
  }

  /** This is a convenience method to construct a national boundaries object. */
  public static Boundaries getNationalBoundaries() {
    return new Boundaries("NationalBoundaries", REF_DIRECTORY, NATIONAL_FILE_NAMES, GMT_FORMAT);
  }

  /** This is a convenience method to construct a national boundaries object. */
  public static Boundaries getStateBoundaries() {
    return new Boundaries("StateBoundaries", REF_DIRECTORY, STATE_FILE_NAMES, GMT_FORMAT);
  }

  /** This is a convenience method to construct a rivers object. */
  public static Boundaries getRivers() {
    return new Boundaries("Rivers", REF_DIRECTORY, RIVER_FILE_NAMES, GMT_FORMAT);
  }
}
