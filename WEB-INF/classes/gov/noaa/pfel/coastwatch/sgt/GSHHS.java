/*
 * GSHHS Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.IntArray;
import com.cohort.util.File2;
import com.cohort.util.LRUCache;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.awt.geom.GeneralPath;
import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class has methods related to GSHHS - A Global Self-consistent, Hierarchical, High-resolution
 * Shoreline Database created by the GMT authors Wessel and Smith
 * (https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html and bob's
 * c:/programs/gshhs/2009v7/readme.txt). GPL License
 * http://www.soest.hawaii.edu/pwessel/gshhs/README.TXT
 */
public class GSHHS {

  /**
   * The characters of the RESOLUTIONS string represent the 5 resolutions in order of decreasing
   * accuracy: "fhilc": 'f'ull, 'h'igh, 'i'ntermediate, 'l'ow, 'c'rude.
   */
  public static final String RESOLUTIONS =
      "fhilc"; // 'f'ull, 'h'igh, 'i'ntermediate, 'l'ow, 'c'rude.

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
   * The GSHHS files must be in the refDirectory. "gshhs_?.b" (?=f|h|i|l|c) files. The files are
   * from the GSHHS project (https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html). landMaskDir
   * should have slash at end.
   */
  public static String gshhsDirectory =
      EDStatic.getWebInfParentDirectory()
          + // with / separator and / at the end
          "WEB-INF/ref/";

  /**
   * Since GeneralPaths are time-consuming to contruct, getGeneralPath caches the last CACHE_SIZE
   * used GeneralPaths. <br>
   * Memory for each cached GP (CWBrowser typical use) is 2KB to 500KB (whole world, crude), (20KB
   * is typical). <br>
   * Note that I originally tried to cache the float lat lons to cache files. <br>
   * Reading/writing the files is fast, but creating the GeneralPath is slow, so I switched to
   * caching the GeneralPaths in memory. <br>
   * Suggested CACHE_SIZE is nPredefinedRegions + 5 (remember that 0-360 regions are different from
   * +/-180 regions). <br>
   * Remember that land and lakes are requested/cached separately.
   */
  public static final int CACHE_SIZE = 100;

  private static Map cache = Collections.synchronizedMap(new LRUCache(CACHE_SIZE));
  private static int nCoarse = 0;
  private static int nSuccesses = 0;
  private static int nTossed = 0;

  /**
   * This limits the number of lakes that appear at lower resolutions. Smaller numbers lead to more
   * lakes. 0=all
   */
  public static int lakeMinN = 12;

  /**
   * This gets the GeneralPath with the relevant shoreline information. This is thread-safe because
   * the cache is thread-safe.
   *
   * @param resolution 'f'ull, 'h'igh, 'i'ntermediate, 'l'ow, 'c'rude.
   * @param desiredLevel determines the specific level of data: 1=land, 2=lake, 3=islandInLake,
   *     4=pondInIslandInLake
   * @param westDeg 0..360 or +/-180,
   * @param eastDeg 0..360 or +/-180
   * @param southDeg +/-90
   * @param northDeg +/-90
   * @param addAntarcticCorners If true, corners are added to the antarctic polygon (for y=-90), so
   *     the polygon plots correctly. This is true for most projections, but not some polar
   *     projections.
   * @return a GeneralPath with the requested polygons, with lat and lon stored as ints (in
   *     millionths of a degree).
   * @throws exception if trouble
   */
  public static GeneralPath getGeneralPath(
      char resolution,
      int desiredLevel,
      double westDeg,
      double eastDeg,
      double southDeg,
      double northDeg,
      boolean addAntarcticCorners)
      throws Exception {

    String cachedName =
        String2.canonical(
            "GSHHS"
                + resolution
                + desiredLevel
                + "W"
                + String2.genEFormat10(westDeg)
                + "E"
                + String2.genEFormat10(eastDeg)
                + "S"
                + String2.genEFormat10(southDeg)
                + "N"
                + String2.genEFormat10(northDeg)
                + "A"
                + (addAntarcticCorners ? "1" : "0"));
    if (reallyVerbose) String2.log("  GSHHS.getGeneralPath request=" + cachedName);
    long time = System.currentTimeMillis();
    String tCoarse = "";
    String tSuccess = "";
    String tTossed = "";
    GeneralPath path;

    // if lowRes, don't ever cache
    if (resolution == 'l' || resolution == 'c') {

      // Don't cache it. Coarse resolutions are always fast because source file is small.
      // And the request is usually a large part of whole world. Most paths will be used.
      tCoarse = "*";
      nCoarse++;

      // make GeneralPath
      path =
          rawGetGeneralPath(
              resolution, desiredLevel, westDeg, eastDeg, southDeg, northDeg, addAntarcticCorners);

    } else {

      // Cache it. Higher resolutions are slower because source file is bigger.
      //  Request is a very small part of whole world. Most paths will be rejected.
      // Thread-safe creation of the GeneralPath
      //  If there are almost simultaneous requests for the same one,
      //  only one thread will make it.
      // Cache is thread-safe so synch on cachedName, not cache.
      ReentrantLock lock = String2.canonicalLock(cachedName);
      if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
        throw new TimeoutException("Timeout waiting for lock on GSHHS cachedName.");
      try {

        // *** is GeneralPath in cache?
        path = (GeneralPath) cache.get(cachedName);
        if (path == null) {

          // not in cache, so make GeneralPath
          path =
              rawGetGeneralPath(
                  resolution,
                  desiredLevel,
                  westDeg,
                  eastDeg,
                  southDeg,
                  northDeg,
                  addAntarcticCorners);

          // cache full?
          if (cache.size() == CACHE_SIZE) {
            tTossed = "*";
            nTossed++;
          } else {
            tSuccess = "*"; // if cache wasn't full, treat as success
            nSuccesses++;
          }

          // put new path in the cache
          cache.put(cachedName, path);

        } else {
          // yes, it is in cache.
          tSuccess = "*(already in cache)";
          nSuccesses++;
        }
      } finally {
        lock.unlock();
      }
    }

    // return path
    if (reallyVerbose)
      String2.log(
          "    GSHHS.getGeneralPath res="
              + resolution
              + " level="
              + (desiredLevel == 1 ? "land" : desiredLevel == 2 ? "lake" : "" + desiredLevel)
              + " done,"
              + " nCoarse="
              + nCoarse
              + tCoarse
              + " nSuccesses="
              + nSuccesses
              + tSuccess
              + " nTossed="
              + nTossed
              + tTossed
              + " totalTime="
              + (System.currentTimeMillis() - time));
    return path;
  }

  /** This returns a stats string for GSHHS. */
  public static String statsString() {
    return "GSHHS: nCached="
        + cache.size()
        + " of "
        + CACHE_SIZE
        + ", nCoarse="
        + nCoarse
        + ", nSuccesses="
        + nSuccesses
        + ", nTossed="
        + nTossed;
  }

  /** This is like getGeneralPath, but with no caching. */
  public static GeneralPath rawGetGeneralPath(
      char resolution,
      int desiredLevel,
      double westDeg,
      double eastDeg,
      double southDeg,
      double northDeg,
      boolean addAntarcticCorners)
      throws Exception {

    // *** else need to make GeneralPath
    GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD); // winding rule is important
    IntArray lon = new IntArray();
    IntArray lat = new IntArray();
    getPathInfo(
        gshhsDirectory,
        resolution,
        desiredLevel,
        westDeg,
        eastDeg,
        southDeg,
        northDeg,
        addAntarcticCorners,
        lon,
        lat);

    // fill path object
    int n = lon.size();
    int lonAr[] = lon.array;
    int latAr[] = lat.array;
    int nObjects = 0;
    for (int i = 0; i < n; i++) {
      if (lonAr[i] == Integer.MAX_VALUE) {
        i++; // move to next point
        path.moveTo(lonAr[i], latAr[i]);
        nObjects++;
      } else {
        path.lineTo(lonAr[i], latAr[i]);
      }
    }
    return path;
  }

  /**
   * This actually reads the GSHHS files and populates lon and lat with info for a GeneralPath. This
   * has nothing to do with the cache system.
   *
   * @param gshhsDir the directory with the gshhs_[fhilc].b data files, with a slash at the end.
   * @param resolution 'f'ull, 'h'igh, 'i'ntermediate, 'l'ow, 'c'rude.
   * @param desiredLevel 1=land, 2=lake, 3=islandInLake, 4=pondInIslandInLake
   * @param westDeg 0..360 or +/-180,
   * @param eastDeg 0..360 or +/-180
   * @param southDeg +/-90
   * @param northDeg +/-90
   * @param addAntarcticCorners If true, corners are added to the antarctic polygon (for y=-90), so
   *     the polygon plots correctly. This is true for most projections, but not some polar
   *     projections.
   * @param lon the IntArray which will receive the lon values (Integer.MAX_VALUE) precedes each
   *     moveTo value. The values are in the appropriate range (0 - 360e6, or +/-180e6).
   * @param lat the IntArray which will receive the lat values (Integer.MAX_VALUE) precedes each
   *     moveTo value.
   * @throws exception if trouble
   */
  public static void getPathInfo(
      String gshhsDir,
      char resolution,
      int desiredLevel,
      double westDeg,
      double eastDeg,
      double southDeg,
      double northDeg,
      boolean addAntarcticCorners,
      IntArray lon,
      IntArray lat)
      throws Exception {

    long time = System.currentTimeMillis();
    lon.clear();
    lat.clear();

    int desiredWest = (int) (westDeg * 1000000); // safe if value input to method is ok
    int desiredEast = (int) (eastDeg * 1000000); // safe if value input to method is ok
    int desiredSouth = (int) (southDeg * 1000000); // safe if value input to method is ok
    int desiredNorth = (int) (northDeg * 1000000); // safe if value input to method is ok

    // the files work in lon 0..360, so adjust if desired
    // The adjustment is crude: either keep original values
    // (generally 0..360, but not always) or subtract 360
    // (so generally -360..360).
    boolean lonPM180 = westDeg < 0;
    int intShift = 360 * 1000000;
    int shift[] = {-2 * intShift, -intShift, 0, intShift};
    boolean doShift[] = new boolean[4];
    byte buffer[] = new byte[4];

    // read the records
    // the xArrays and yArrays grow as needed
    int xArray[] = new int[1];
    int yArray[] = new int[1];
    int xArray2[] = new int[1];
    int yArray2[] = new int[1];
    boolean aMsgDisplayed = false;
    boolean gMsgDisplayed = false;
    int count = 0;

    // open the file
    // String2.log(File2.hexDump(dir + "gshhs_" + resolution + ".b", 10000));
    DataInputStream dis =
        new DataInputStream(
            File2.getDecompressedBufferedInputStream(gshhsDir + "gshhs_" + resolution + ".b"));
    try {
      while (dis.available() > 0) {
        // read the header
        /* old GSHHS v 1.x
        int id        = dis.readInt();
        int n         = dis.readInt();
        int level     = dis.readInt();
        int west      = dis.readInt();
        int east      = dis.readInt();
        int south     = dis.readInt();
        int north     = dis.readInt();
        int area      = dis.readInt();
        int greenwich = dis.readShort(); //1 == it crosses greenwich
        int source    = dis.readShort();
        */

        // GSHHS v2.1.1  2011-03-14
        // GPL License http://www.soest.hawaii.edu/pwessel/gshhs/README.TXT
        // int id        = DataStream.readInt(true, dis, buffer); // Unique polygon id number,
        // starting at 0
        int id = dis.readInt(); // Unique polygon id number, starting at 0
        int n = dis.readInt(); // Number of points in this polygon
        int flag =
            dis.readInt(); // = level + version << 8 + greenwich << 16 + source << 24 + river << 25
        // flag contains 5 items, as follows:
        // low byte:    level = flag & 255: Values: 1 land, 2 lake, 3 island_in_lake, 4
        // pond_in_island_in_lake
        // 2nd byte:    version = (flag >> 8) & 255: Values: Should be 7 for GSHHS release 7 (i.e.,
        // version 2.0)
        // 3rd byte:    greenwich = (flag >> 16) & 1: Values: Greenwich is 1 if Greenwich is crossed
        // 4th byte:    source = (flag >> 24) & 1: Values: 0 = CIA WDBII, 1 = WVS
        // 4th byte:    river = (flag >> 25) & 1: Values: 0 = not set, 1 = river-lake and level = 2
        //
        int west = dis.readInt(); // min/max extent in micro-degrees    0 - 360 deg
        int east = dis.readInt();
        int south = dis.readInt();
        int north = dis.readInt();
        int area = dis.readInt(); // Area of polygon in 1/10 km^2
        int area_full = dis.readInt(); // Area of original full-resolution polygon in 1/10 km^2
        int container =
            dis.readInt(); // Id of container polygon that encloses this polygon (-1 if none)
        int ancestor =
            dis.readInt(); // Id of ancestor polygon in the full resolution set that was the source
        // of this polygon (-1 if none)

        int level = flag & 255;
        int greenwich = (flag >> 16) & 1; // Values: Greenwich is 1 if Greenwich is crossed

        // if (debug && west > east) String2.pressEnterToContinue("  west=" + west + "east=" + east
        // + " greenwich=" + greenwich);

        // if (greenwich == 1)
        // String2.log("id=" + id + " level=" + level + " gwch=" + greenwich + " ver=" + ((flag >>
        // 8) & 255) +
        //  " n=" + n + " wesn=" + west + "/" + east + "/" + south + "/" + north);

        // tests show greenwich objects have a negative west bound (e.g., -1deg)
        // even though <0 lon values are stored +360
        // if (!gMsgDisplayed && greenwich == 1) {
        //    String2.log("greenwich n=" + n + " west=" + west + " east=" + east + " south=" + south
        // + " north=" + north);
        //    gMsgDisplayed = true;
        // }

        // tests show antarctic object has
        // bounds (degrees) are west=0 east=360 south=-90 north=-63
        // if (!aMsgDisplayed && south < -60000000) {
        //    String2.log("antartic n=" + n + " west=" + west + " east=" + east + " south=" + south
        // + " north=" + north);
        //    aMsgDisplayed = true;
        // }

        // Do the tests for the 4 possible independent uses of this data.
        boolean levelAndLatOK =
            level == desiredLevel
                && // was <=
                south < desiredNorth
                && north > desiredSouth;
        boolean doSomething = false;
        for (int i = 0; i < 4; i++) {
          doShift[i] =
              levelAndLatOK && west + shift[i] < desiredEast && east + shift[i] > desiredWest;
          if (doShift[i]) doSomething = true;
        }

        // skip small lakes
        // @param resolution 0='f'ull, 1='h'igh, 2='i'ntermediate, 3='l'ow, 4='c'rude.
        boolean skip =
            desiredLevel >= 2
                && // lakes
                ((resolution == 'c' && n < lakeMinN) || (resolution == 'l' && n < lakeMinN / 2));

        // can I use the object?
        if (doSomething && !skip) {

          // read the data
          if (n + 4 > xArray.length) {
            xArray = new int[n + 4]; // +4 for addAntarticCorners
            yArray = new int[n + 4];
          }
          for (int i = 0; i < n; i++) {
            xArray[i] = dis.readInt();
            yArray[i] = dis.readInt();
            // String2.log("xarray=" +String2.toCSSVString(xArray));
            // String2.log("yarray=" +String2.toCSSVString(yArray));
          }

          // for addAntarcticCorners, insert points at corners of map.
          // antarctic object bounds (degrees) are west=0 east=360 south=-90 north=-63
          // search for lon=0
          if (south == -90000000) { // catches antarctic polygon
            // this shows first x=360 (exact), x decreases to 0 (exact)
            //  and y's are the perimeter (not to south pole)
            // String2.log("antarctic n=" + n + " x[0]=" + xArray[0] +
            //    " x[1]=" + xArray[1] + " x[n-2]=" + xArray[n-2] +
            //    " x[n-1]=" + xArray[n-1] + "\n" +
            //    "    y[0]=" + yArray[0] +
            //    " y[1]=" + yArray[1] + " y[n-2]=" + yArray[n-2] +
            //    " y[n-1]=" + yArray[n-1]);
            if (addAntarcticCorners) {
              // add the 3 antarctic corner points to make a polygon (1st pt = last)
              // this leaves seam at x=0 ... x=360
              xArray[n] = 0;
              yArray[n] = -90000000;
              xArray[n + 1] = 360000000;
              yArray[n + 1] = -90000000;
              xArray[n + 2] = 360000000;
              yArray[n + 2] = yArray[0];
              n += 3;
            }
          }

          // if polygon crosses greenwich, x's < 0 are stored +360 degrees
          // see https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html  where is new gshhs.c?
          // so shift left so points are continguous and match west/east of the polygon
          if (greenwich == 1) {
            for (int i = 0; i < n; i++)
              if (xArray[i] > east) {
                xArray[i] -= intShift;
                // String2.log("greenwich left");
              }
          }

          // test/do each doShift
          for (int ds = 0; ds < 4; ds++) {
            if (doShift[ds]) {
              int tShift = shift[ds];

              // copy the data into x/yArray2's
              // so source data is undisturbed for other doShift
              if (n > xArray2.length) {
                xArray2 = new int[n];
                yArray2 = new int[n];
              }
              System.arraycopy(xArray, 0, xArray2, 0, n);
              System.arraycopy(yArray, 0, yArray2, 0, n);

              // reduce and draw
              int tn =
                  reduce(
                      n,
                      xArray2,
                      yArray2,
                      desiredWest - tShift,
                      desiredEast - tShift, // faster to shift desired the opposite way
                      desiredSouth,
                      desiredNorth); //  than to shift xArray2 the correct way
              if (tn > 0) {
                lon.add(Integer.MAX_VALUE); // indicates moveTo next point
                lat.add(Integer.MAX_VALUE);
                for (int i = 0; i < tn; i++) {
                  lon.add(xArray2[i] + tShift); // then shift xArray2
                  lat.add(yArray2[i]);
                }
              }
            }
          }

        } else {
          // skip over the data
          long remain = 8L * n; // 2 (x,y) * 4 (bytes/int)
          while (remain > 0) remain -= dis.skip(remain);
        }
      }
    } finally {
      dis.close();
    }
    if (reallyVerbose)
      String2.log(
          "  GSHHS.getPathInfo done. res="
              + resolution
              + " level="
              + (desiredLevel == 1 ? "land" : desiredLevel == 2 ? "lake" : "" + desiredLevel)
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /**
   * This reduces the points outside of the desired bounds. This is simpler than clipping but serves
   * my purpose well -- far fewer points to store and draw.
   *
   * @param n the number of active points in xa and ya. They must all be non-NaN.
   * @param xa the array with x values
   * @param ya the array with y values
   * @param west appropriate for xa
   * @param east appropriate for xa
   * @param south appropriate for ya
   * @param north appropriate for ya
   * @return n the new active number of points in the arrays (may be 0)
   */
  public static int reduce(int n, int x[], int y[], int west, int east, int south, int north) {

    if (west > east) {
      String2.log("ERROR in GSHHS.reduce: west=" + west + " > east=" + east);
      return 0;
    }
    if (south > north) {
      String2.log("ERROR in GSHHS.reduce: south=" + south + " > north=" + north);
      return 0;
    }
    if (n == 0 || x == null || y == null) return 0;

    // algorithm assigns a point to a sector:
    //         north
    //       6 | 7 | 8
    //       ---------
    // west  3 | 4 | 5  east   4 is within desired bounds
    //       ---------
    //       0 | 1 | 2
    //         south
    // * keep all points in sector 4
    // * keep 1st and last points of a sequence in another sector
    int nGood = 0; // temp n good points
    int sequenceStarti = 0;
    int sequenceSector = -1; // unknown
    boolean sectorUsed[] = new boolean[9]; // all false
    for (int i = 0; i <= n; i++) { // yes, n deals with post code

      // what sector is this point?
      int sector = -1; // for when i==n
      if (i < n) {
        sector =
            (y[i] > north ? 6 : y[i] >= south ? 3 : 0) + (x[i] > east ? 2 : x[i] >= west ? 1 : 0);
        if (i == 0) {
          sequenceSector = sector;
          continue;
        }
      }

      // did the sector change?
      if (sector != sequenceSector) {
        sectorUsed[sequenceSector] = true;
        if (sequenceSector == 4) {
          // keep all of the points in the sequence
          if (nGood == 0) {
            nGood = i; // already in place
          } else {
            System.arraycopy(x, sequenceStarti, x, nGood, i - sequenceStarti);
            System.arraycopy(y, sequenceStarti, y, nGood, i - sequenceStarti);
            nGood += i - sequenceStarti;
          }
        } else {
          // just keep the first and last points in the sequence
          x[nGood] = x[sequenceStarti];
          y[nGood++] = y[sequenceStarti];
          if (i - 1 > sequenceStarti) { // if >1 point in sequence
            x[nGood] = x[i - 1];
            y[nGood++] = y[i - 1];
          }
        }
        sequenceStarti = i;
        sequenceSector = sector;
      }
    }

    // if (n > 1000) String2.log("GSHHS.reduce n=" + n + " nGood=" + nGood);

    if (!sectorUsed[4]) {
      // Can we reject all points because
      // all points too far N?
      if (!sectorUsed[0]
          && !sectorUsed[1]
          && !sectorUsed[2]
          && !sectorUsed[3]
          && !sectorUsed[4]
          && !sectorUsed[5]) return 0;
      // all points too far S?
      if (!sectorUsed[3]
          && !sectorUsed[4]
          && !sectorUsed[5]
          && !sectorUsed[6]
          && !sectorUsed[7]
          && !sectorUsed[8]) return 0;
      // all points too far E?
      if (!sectorUsed[0]
          && !sectorUsed[1]
          && !sectorUsed[3]
          && !sectorUsed[4]
          && !sectorUsed[6]
          && !sectorUsed[7]) return 0;
      // all points too far W?
      if (!sectorUsed[1]
          && !sectorUsed[2]
          && !sectorUsed[4]
          && !sectorUsed[5]
          && !sectorUsed[7]
          && !sectorUsed[8]) return 0;
    }

    return nGood;
  }

  /**
   * This is perfectly identical to the int version of reduce() above, but for double parameters.
   *
   * @param n the number of active points in xa and ya. They must all be non-NaN.
   * @param xa the array with x values
   * @param ya the array with y values
   * @param west appropriate for xa
   * @param east appropriate for xa
   * @param south appropriate for ya
   * @param north appropriate for ya
   * @return n the new active number of points in the arrays (may be 0)
   */
  public static int reduce(
      int n, double x[], double y[], double west, double east, double south, double north) {

    if (west > east) {
      String2.log("ERROR in GSHHS.reduce: west=" + west + " > east=" + east);
      return 0;
    }
    if (south > north) {
      String2.log("ERROR in GSHHS.reduce: south=" + south + " > north=" + north);
      return 0;
    }
    if (n == 0 || x == null || y == null) return 0;

    // algorithm assigns a point to a sector:
    //         north
    //       6 | 7 | 8
    //       ---------
    // west  3 | 4 | 5  east   4 is within desired bounds
    //       ---------
    //       0 | 1 | 2
    //         south
    // * keep all points in sector 4
    // * keep 1st and last points of a sequence in another sector
    int nGood = 0; // temp n good points
    int sequenceStarti = 0;
    int sequenceSector = -1; // unknown
    boolean sectorUsed[] = new boolean[9]; // all false
    for (int i = 0; i <= n; i++) { // yes, n deals with post code

      // what sector is this point?
      int sector = -1; // for when i==n
      if (i < n) {
        sector =
            (y[i] > north ? 6 : y[i] >= south ? 3 : 0) + (x[i] > east ? 2 : x[i] >= west ? 1 : 0);
        if (i == 0) {
          sequenceSector = sector;
          continue;
        }
      }

      // did the sector change?
      if (sector != sequenceSector) {
        sectorUsed[sequenceSector] = true;
        if (sequenceSector == 4) {
          // keep all of the points in the sequence
          if (nGood == 0) {
            nGood = i; // already in place
          } else {
            System.arraycopy(x, sequenceStarti, x, nGood, i - sequenceStarti);
            System.arraycopy(y, sequenceStarti, y, nGood, i - sequenceStarti);
            nGood += i - sequenceStarti;
          }
        } else {
          // just keep the first and last points in the sequence
          x[nGood] = x[sequenceStarti];
          y[nGood++] = y[sequenceStarti];
          if (i - 1 > sequenceStarti) { // if >1 point in sequence
            x[nGood] = x[i - 1];
            y[nGood++] = y[i - 1];
          }
        }
        sequenceStarti = i;
        sequenceSector = sector;
      }
    }

    // if (n > 1000) String2.log("GSHHS.reduce n=" + n + " nGood=" + nGood);

    if (!sectorUsed[4]) {
      // Can we reject all points because
      // all points too far N?
      if (!sectorUsed[0]
          && !sectorUsed[1]
          && !sectorUsed[2]
          && !sectorUsed[3]
          && !sectorUsed[4]
          && !sectorUsed[5]) return 0;
      // all points too far S?
      if (!sectorUsed[3]
          && !sectorUsed[4]
          && !sectorUsed[5]
          && !sectorUsed[6]
          && !sectorUsed[7]
          && !sectorUsed[8]) return 0;
      // all points too far E?
      if (!sectorUsed[0]
          && !sectorUsed[1]
          && !sectorUsed[3]
          && !sectorUsed[4]
          && !sectorUsed[6]
          && !sectorUsed[7]) return 0;
      // all points too far W?
      if (!sectorUsed[1]
          && !sectorUsed[2]
          && !sectorUsed[4]
          && !sectorUsed[5]
          && !sectorUsed[7]
          && !sectorUsed[8]) return 0;
    }

    return nGood;
  }
}
