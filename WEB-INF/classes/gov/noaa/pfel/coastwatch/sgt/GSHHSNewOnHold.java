/* 
 * GSHHS Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.LRUCache;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.DataStream;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.*;
import java.util.Collections;
import java.util.Map;


2014-01-07 EEEEK! I made a lot of changes to GSHHS.java.
When it is time to resucitate this, merge changes from this into GSHHS.java.

/**
 *
 * <p>This class has methods related to GSHHS -
 * A Global Self-consistent, Hierarchical, High-resolution Shoreline Database
 * created by the GMT authors Wessel and Smith 
 * (https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html and 
 * bob's c:/programs/gshhs/2009v7/readme.txt).
 * GPL License http://www.soest.hawaii.edu/pwessel/gshhs/README.TXT
 */
public class GSHHSNewOnHold  {

    /** The characters of the RESOLUTIONS string represent the 5 resolutions
     * in order of decreasing accuracy: "fhilc": 'f'ull, 'h'igh, 'i'ntermediate, 'l'ow, 'c'rude.
     */
    public final static String RESOLUTIONS = "fhilc"; //'f'ull, 'h'igh, 'i'ntermediate, 'l'ow, 'c'rude.


    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /**
     * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
     * if you want lots and lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false;

    /** 
     * The GSHHS files must be in the refDirectory. 
     *    "gshhs_?.b" (?=f|h|i|l|c) files. 
     *    The files are from the GSHHS project
     *    (https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html).
     *    landMaskDir should have slash at end.
     */
    public static String gshhsDirectory = 
        SSR.getContextDirectory() + //with / separator and / at the end
        "WEB-INF/ref/";

    /**
     * Since GeneralPaths are time-consuming to contruct,
     *   getGeneralPath caches the last CACHE_SIZE used GeneralPaths.
     * <br>Memory for each cached GP (CWBrowser typical use) is 2KB to 
     *    500KB (whole world, crude), (20KB is typical).
     * <br>Note that I originally tried to cache the float lat lons to cache files.
     * <br>Reading/writing the files is fast, but creating the GeneralPath is slow, 
     *   so I switched to caching the GeneralPaths in memory.
     * <br>Suggested CACHE_SIZE is nPredefinedRegions + 5 (remember that 0-360 regions
     *   are different from +/-180 regions).
     * <br>Remember that land and lakes are requested/cached separately.
     */
    public final static int CACHE_SIZE = 100;
    private static Map cache = Collections.synchronizedMap(new LRUCache(CACHE_SIZE));
    private static int nCoarse = 0;
    private static int nSuccesses = 0;
    private static int nTossed = 0;

    /** This limits the number of lakes that appear at lower resolutions.
     *  Smaller numbers lead to more lakes.  0=all */
    public static int lakeMinN = 12;


    /**
     * This gets the GeneralPath with the relevant shoreline information.
     * This is thread-safe because the cache is thread-safe.
     *
     * @param resolution 'f'ull, 'h'igh, 'i'ntermediate, 'l'ow, 'c'rude.
     * @param desiredLevel determines the specific level of data:
     *      1=land, 2=lake, 3=islandInLake, 4=pondInIslandInLake
     * @param westDeg 0..360 or +/-180, 
     * @param eastDeg 0..360 or +/-180
     * @param southDeg +/-90
     * @param northDeg +/-90
     * @param addAntarcticCorners If true, corners are added to the 
     *    antarctic polygon (for y=-90), so the polygon plots correctly.
     *    This is true for most projections, but not some polar projections.
     * @return a GeneralPath with the requested polygons, with lat and lon
     *    stored as ints (in millionths of a degree).
     * @throws exception if trouble
     */
    public static GeneralPath getGeneralPath(  
        char resolution, int desiredLevel, double westDeg, double eastDeg, 
        double southDeg, double northDeg, boolean addAntarcticCorners) throws Exception {

        String cachedName = String2.canonical(
            "GSHHS" + resolution + desiredLevel +
            "W" + String2.genEFormat10(westDeg) +
            "E" + String2.genEFormat10(eastDeg) +
            "S" + String2.genEFormat10(southDeg) +
            "N" + String2.genEFormat10(northDeg) +
            "A" + (addAntarcticCorners? "1" : "0"));
        if (reallyVerbose) String2.log("  GSHHS.getGeneralPath request=" + cachedName);
        long time = System.currentTimeMillis();
        String tCoarse = "";
        String tSuccess = "";
        String tTossed = "";
        GeneralPath path;

        //if lowRes, don't ever cache
        if (resolution == 'l' || resolution == 'c') {

            //Don't cache it. Coarse resolutions are always fast because source file is small.
            //And the request is usually a large part of whole world. Most paths will be used.
            tCoarse = "*";
            nCoarse++;

            //make GeneralPath
            path = rawGetGeneralPath(resolution, desiredLevel, 
                westDeg, eastDeg, southDeg, northDeg, addAntarcticCorners);

        } else {

            //Cache it. Higher resolutions are slower because source file is bigger.
            //  Request is a very small part of whole world. Most paths will be rejected.
            //Thread-safe creation of the GeneralPath
            //  If there are almost simultaneous requests for the same one, 
            //  only one thread will make it.
            //Cache is thread-safe so synch on cachedName, not cache.
            synchronized(cachedName) {

                //*** is GeneralPath in cache?
                path = (GeneralPath)cache.get(cachedName);
                if (path == null) {

                    //not in cache, so make GeneralPath
                    path = rawGetGeneralPath(resolution, desiredLevel, 
                        westDeg, eastDeg, southDeg, northDeg, addAntarcticCorners);

                    //cache full?
                    if (cache.size() == CACHE_SIZE) {
                        tTossed = "*";
                        nTossed++;
                    } else {
                        tSuccess = "*";  //if cache wasn't full, treat as success
                        nSuccesses++;
                    }

                    //put new path in the cache
                    cache.put(cachedName, path);

                } else {
                    //yes, it is in cache.   
                    tSuccess = "*(already in cache)";
                    nSuccesses++;
                }
            }
        }

        //return path
        if (reallyVerbose) String2.log("    GSHHS.getGeneralPath res=" + resolution + 
            " level=" + (desiredLevel==1? "land" : desiredLevel==2? "lake" : "" + desiredLevel) + 
            " done," + 
            " nCoarse=" + nCoarse + tCoarse +
            " nSuccesses=" + nSuccesses + tSuccess +
            " nTossed=" + nTossed + tTossed +
            " totalTime=" + (System.currentTimeMillis() - time));
        return path;

    }

    /** This returns a stats string for GSHHS. */
    public static String statsString() {
        return 
            "GSHHS: nCached=" + cache.size() + " of " + CACHE_SIZE +
            ", nCoarse=" + nCoarse + ", nSuccesses=" + nSuccesses + ", nTossed=" + nTossed;
    }

    /** This is like getGeneralPath, but with no caching.
     */
    public static GeneralPath rawGetGeneralPath(  
        char resolution, int desiredLevel, double westDeg, double eastDeg, 
        double southDeg, double northDeg, boolean addAntarcticCorners) throws Exception {

        //*** else need to make GeneralPath
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD); //winding rule is important
        IntArray lon = new IntArray();
        IntArray lat = new IntArray();
        getPathInfo(gshhsDirectory, resolution, desiredLevel, westDeg, eastDeg, 
            southDeg, northDeg, addAntarcticCorners, lon, lat);

        //fill path object 
        int n = lon.size();
        int lonAr[] = lon.array;
        int latAr[] = lat.array;
        int nObjects = 0;
        for (int i = 0; i < n; i++) {
            if (lonAr[i] == Integer.MAX_VALUE) {
                i++; //move to next point
                path.moveTo(lonAr[i], latAr[i]); 
                nObjects++;
            } else {
                path.lineTo(lonAr[i], latAr[i]);
            }
        }
        return path;
    }

    /**
     * This actually reads the GSHHS files and populates lon and lat with info for a GeneralPath.
     * This has nothing to do with the cache system.
     *
     * @param gshhsDir the directory with the gshhs_[fhilc].b data files, with a slash at the end.
     * @param resolution 'f'ull, 'h'igh, 'i'ntermediate, 'l'ow, 'c'rude.
     * @param desiredLevel 1=land, 2=lake, 3=islandInLake, 4=pondInIslandInLake
     * @param westDeg 0..360 or +/-180, 
     * @param eastDeg 0..360 or +/-180
     * @param southDeg +/-90
     * @param northDeg +/-90
     * @param addAntarcticCorners If true, corners are added to the 
     *    antarctic polygon (for y=-90), so the polygon plots correctly.
     *    This is true for most projections, but not some polar projections.
     * @param lon the IntArray which will receive the lon values
     *    (Integer.MAX_VALUE) precedes each moveTo value.
     *    The values are in the appropriate range (0 - 360e6, or +/-180e6).
     * @param lat the IntArray which will receive the lat values
     *    (Integer.MAX_VALUE) precedes each moveTo value.
     * @throws exception if trouble
     */
    public static void getPathInfo(String gshhsDir, 
        char resolution, int desiredLevel, double westDeg, double eastDeg, 
        double southDeg, double northDeg, boolean addAntarcticCorners,
        IntArray lon, IntArray lat) throws Exception {

        long time = System.currentTimeMillis();
        lon.clear();
        lat.clear();

        int desiredWest  = (int)(westDeg  * 1000000); //safe if value input to method is ok
        int desiredEast  = (int)(eastDeg  * 1000000); //safe if value input to method is ok
        int desiredSouth = (int)(southDeg * 1000000); //safe if value input to method is ok
        int desiredNorth = (int)(northDeg * 1000000); //safe if value input to method is ok

        //the files work mostly in lon -180..180, so adjust if desired
        //The adjustment is crude: 
        //either keep original values (generally -180..180, but not always) 
        //or add 360 (so generally 180.. 540).
        //A few islands straddle 180 so need subtract 360 
        boolean lonPM180 = westDeg < 0;
        int intShift = 360 * 1000000;

        //open the file
        //String2.log(File2.hexDump(dir + "gshhs_" + resolution + ".b", 10000));
        DataInputStream dis = new DataInputStream(new BufferedInputStream(
            new FileInputStream(gshhsDir + "gshhs_" + resolution + ".b")));
        byte buffer[] = new byte[4];

        //read the records
        //the xArrays and yArrays grow as needed
        int xArray[] = new int[1];
        int yArray[] = new int[1];
        int xArray2[] = new int[1];
        int yArray2[] = new int[1];
        boolean aMsgDisplayed = false;
        boolean g1MsgDisplayed = false;
        boolean g2MsgDisplayed = false;
        boolean g3MsgDisplayed = false;
        int count = 0;
        while (dis.available() > 0) {
            //read the header

            //GSHHS v2.2.0  2011-07-15
            //GPL License http://www.soest.hawaii.edu/pwessel/gshhs/README.TXT
            int id        = dis.readInt(); // Unique polygon id number, starting at 0 
            int n         = dis.readInt(); // Number of points in this polygon 
            int flag      = dis.readInt(); // = level + version << 8 + greenwich << 16 + source << 24 + river << 25 
            // flag contains 5 items, as follows:
            // low byte:    level = flag & 255: Values: 1 land, 2 lake, 3 island_in_lake, 4 pond_in_island_in_lake
            // 2nd byte:    version = (flag >> 8) & 255: Values: Should be 7 for GSHHS release 7 (i.e., version 2.0)
            // 3rd byte:    greenwich = (flag >> 16) & 3: see below
            // 4th byte:    source = (flag >> 24) & 1: Values: 0 = CIA WDBII, 1 = WVS
            // 4th byte:    river = (flag >> 25) & 1: Values: 0 = not set, 1 = river-lake and level = 2
            //
            int west      = dis.readInt(); // min/max extent in micro-degrees 
            int east      = dis.readInt(); 
            int south     = dis.readInt(); 
            int north     = dis.readInt(); 
            int area      = dis.readInt(); // Area of polygon in 1/10 km^2 
            int area_full = dis.readInt(); // Area of original full-resolution polygon in 1/10 km^2 
            int container = dis.readInt(); // Id of container polygon that encloses this polygon (-1 if none) 
            int ancestor  = dis.readInt(); // Id of ancestor polygon in the full resolution set that was the source of this polygon (-1 if none) 

            int level = flag & 255;
            //greenwich: 1=Greenwich is crossed, 2= dateline is crossed, 3=both crossed
            int greenwich = (flag >> 16) & 3; 
                        
            //String2.log("id=" + id + " level=" + level + " gwch=" + greenwich + " ver=" + ((flag >> 8) & 255) + 
            //  " n=" + n + " " + west + "/" + east + "/" + south + "/" + north);

            /* 
            //tests show greenwich objects have a negative west bound
            //(even though <0 lon values are stored +360)
            if (!g1MsgDisplayed && greenwich == 1) { //crosses greenwich
               //e.g., greenwich1 n=221 west=-17533778 east=51415417 south=-34830444 north=37349556
                String2.log("greenwich1 n=" + n + " west=" + west + " east=" + east + " south=" + south + " north=" + north);
                g1MsgDisplayed = true;
            }
            if (!g2MsgDisplayed && greenwich == 2) { //crosses dateline 
                //e.g., greenwich2 n=7 west=178651222 east=182603694 south=70777056 north=71603750
                String2.log("greenwich2 n=" + n + " west=" + west + " east=" + east + " south=" + south + " north=" + north);
                g2MsgDisplayed = true;
            }
            if (!g3MsgDisplayed && greenwich == 3) { //crosses dateline and greenwich
                //e.g., greenwich3 n=1028 west=-9500444 east=190359111 south=1268694 north=77719583
                String2.log("greenwich3 n=" + n + " west=" + west + " east=" + east + " south=" + south + " north=" + north);
                g3MsgDisplayed = true;
            }
            */

            if (//!aMsgDisplayed && 
                south < -60000000) {
                //e.g., antartic n=197 west=-180000000 east=180000000 south=-90000000 north=-63220028
                String2.log("id=" + id + " antartic n=" + n + " west=" + west + " east=" + east + " south=" + south + " north=" + north);
                aMsgDisplayed = true;
            }

            //Do the tests for the 3 possible independent uses of this data.
            //Note that often 2 of the 3 are true.
            //can I use this object with standard coordinates?
            boolean useStandard = 
                level == desiredLevel &&  
                west  < desiredEast &&
                east  > desiredWest &&
                south < desiredNorth &&
                north > desiredSouth;
            
            //can I use this object with coordinates shifted left (e.g., pm 180)?
            boolean useShiftedLeft = 
                lonPM180 &&
                level == desiredLevel &&
                west-intShift < desiredEast &&
                east-intShift > desiredWest &&
                south < desiredNorth &&
                north > desiredSouth;
            
            //can I use this object with coordinates shifted right (for greenwich==1 objects when !pm180)?
            boolean useShiftedRight = 
                level == desiredLevel &&
                west+intShift < desiredEast &&
                east+intShift > desiredWest &&
                south < desiredNorth &&
                north > desiredSouth;

            //skip small lakes
            //@param resolution 0='f'ull, 1='h'igh, 2='i'ntermediate, 3='l'ow, 4='c'rude.
            boolean skip = 
                desiredLevel >= 2 && //lakes
                ((resolution == 'c' && n < lakeMinN) || 
                 (resolution == 'l' && n < lakeMinN / 2));
            
            //can I use the object?   
            if ((useStandard || useShiftedLeft || useShiftedRight) && !skip) {
                int cShift = 0;
                if (useShiftedLeft) cShift = -intShift;
                else if (useShiftedRight) cShift = intShift;

                //read the data
                if (n > xArray.length) {
                    xArray = new int[n + 4];  //+4 for addAntarticCorners
                    yArray = new int[n + 4];
                }
                for (int i = 0; i < n; i++) {
                    xArray[i] = dis.readInt();
                    yArray[i] = dis.readInt();
                    //String2.log("xarray=" +String2.toCSSVString(xArray));
                    //String2.log("yarray=" +String2.toCSSVString(yArray));
                }  

                //test: is xArray range as promised?
                {
                    IntArray ia = new IntArray(xArray);
                    ia.removeRange(n, ia.size());
                    int[] nmm = ia.getNMinMaxIndex();
                    if (Math.abs(west - xArray[nmm[1]]) > 500000) 
                        String2.log("id=" + id + " bad west " + west + " != " + xArray[nmm[1]]);
                    if (Math.abs(east - xArray[nmm[2]]) > 500000)
                        String2.log("id=" + id + " bad east " + east + " != " + xArray[nmm[2]]);
                }

                //for addAntarcticCorners, insert points at corners of map.
                //antarctic object bounds (degrees) are west=0 east=360 south=-90 north=-63
                //search for lon=0
                if (south == -90000000) { //catches antarctic polygon
                    if (desiredWest < 0 && desiredEast > 0) {
                        //Desired is e.g. -180 to 180. 
                        //To avoid seam in bad place, manually shift 1/2 of it left.
                        useShiftedLeft = false; 
                        for (int i = 0; i < n; i++) {
                            if (xArray[i] >= 180000000) { //in practice there is no 180000000 point
                                xArray[i] -= 360000000;
                                if (i < n-1 && xArray[i+1] < 180000000) {
                                    //We're crossing x=180, where we want the seam.
                                    //These tests show x=180 isn't first or last point
                                    //  and prev x is almost 0, next x is 360
                                    //String2.log("antarctic x crosses 180 at x[" + 
                                    //    i + "]=" + xArray[i] + " x[i+1]=" + xArray[i+1]);

                                    if (addAntarcticCorners) {
                                        //add the 4 antarctic corners  
                                        //this puts a seam at x=-180 ... x=180
                                        System.arraycopy(xArray, i + 1, xArray, i + 5, n - (i+1));
                                        System.arraycopy(yArray, i + 1, yArray, i + 5, n - (i+1));
                                        xArray[i + 1] = -180000000; yArray[i + 1] = yArray[i];
                                        xArray[i + 2] = -180000000; yArray[i + 2] = -90000000;
                                        xArray[i + 3] =  180000000; yArray[i + 3] = -90000000;
                                        xArray[i + 4] =  180000000; yArray[i + 4] = yArray[i];
                                        i += 4;
                                        n += 4;
                                    }
                                }
                            }
                        }
                    } else {
                        //probably just useStandard (e.g., 0..360)    
                        //  or useLeft (limited range e.g., -130.. -120)
                        if (addAntarcticCorners) {
                            //this puts a seam at x=0 ... x=360
                            for (int i = 0; i < n; i++) {
                                if (xArray[i] == 0) {
                                    //these tests show x=0 isn't first or last point
                                    //  and prev x is almost 0, next x is 360
                                    //String2.log("antarctic x=0 at i=" + i + " n=" + n);
                                    //if (i > 0) String2.log("  x[i-1]=" + xArray[i-1]);
                                    //if (i < n-1) String2.log("  x[i+1]=" + xArray[i+1]);

                                    //add the 2 antarctic corners  
                                    System.arraycopy(xArray, i + 1, xArray, i + 3, n - (i+1));
                                    System.arraycopy(yArray, i + 1, yArray, i + 3, n - (i+1));
                                    xArray[i + 1] =         0; yArray[i + 1] = -90000000;
                                    xArray[i + 2] = 360000000; yArray[i + 2] = -90000000;
                                    n += 2;
                                    break;
                                }
                            }
                        }
                    }
                }

                //if polygon crosses greenwich, x's < 0 are stored +360 degrees
                //see https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html  where is new gshhs.c?
                if ((greenwich & 1) == 1) {
                    for (int i = 0; i < n; i++) 
                        if (xArray[i] > east) xArray[i] -= intShift;
                }  
                

                //useShiftedLeft   (this is independent of useShiftedRight and useStandard)
                if (useShiftedLeft) {

                    //copy the data into x/yArray2's  
                    //so data is undisturbed for useShiftedRight and useStandard
                    if (n > xArray2.length) {
                        xArray2 = new int[n];
                        yArray2 = new int[n];
                    }
                    System.arraycopy(xArray, 0, xArray2, 0, n);
                    System.arraycopy(yArray, 0, yArray2, 0, n);

                    //reduce and draw
                    int tn = reduce(n, xArray2, yArray2, 
                        desiredWest + intShift, desiredEast + intShift, //shifting desired is faster than shifting all x,y
                        desiredSouth, desiredNorth);
                    if (tn > 0) {
                        lon.add(Integer.MAX_VALUE); //indicates moveTo next point
                        lat.add(Integer.MAX_VALUE);
                        for (int i = 0; i < tn; i++) {
                            lon.add(xArray2[i] - intShift);
                            lat.add(yArray2[i]);
                        }
                    }
                }

                //useShiftedRight   (this is independent of useShiftedLeft and useStandard)
                if (useShiftedRight) {
                    //copy the data into x/yArray2's
                    //so data is undisturbed for useShiftedRight and useStandard
                    if (n > xArray2.length) {
                        xArray2 = new int[n];
                        yArray2 = new int[n];
                    }
                    System.arraycopy(xArray, 0, xArray2, 0, n);
                    System.arraycopy(yArray, 0, yArray2, 0, n);

                    //reduce and draw
                    int tn = reduce(n, xArray2, yArray2, 
                        desiredWest - intShift, desiredEast - intShift, //shifting desired is faster than shifting all x,y
                        desiredSouth, desiredNorth);
                    if (tn > 0) {
                        lon.add(Integer.MAX_VALUE); //indicates moveTo next point
                        lat.add(Integer.MAX_VALUE);
                        for (int i = 0; i < tn; i++) {
                            lon.add(xArray2[i] + intShift);
                            lat.add(yArray2[i]);
                        }
                    }
                }

                //useStandard   (this is independent of useShiftedLeft and useShiftedRight)
                if (useStandard) {
                    //reduce and draw
                    int tn = reduce(n, xArray, yArray, 
                        desiredWest, desiredEast, 
                        desiredSouth, desiredNorth);
                    if (tn > 0) {
                        //add to lon and lat
                        lon.add(Integer.MAX_VALUE); //indicates moveTo next point
                        lat.add(Integer.MAX_VALUE);
                        for (int i = 0; i < tn; i++) {
                            lon.add(xArray[i]);
                            lat.add(yArray[i]);
                        }
                    }
                }

            } else {
                //skip over the data
                long remain = 8L * n;   //2 (x,y) * 4 (bytes/int)
                while (remain > 0) 
                    remain -= dis.skip(remain);
            }

        }
        dis.close();
        if (reallyVerbose) String2.log("  GSHHS.getPathInfo done. res=" + resolution +
            " level=" + (desiredLevel==1? "land" : desiredLevel==2? "lake" : "" + desiredLevel) + 
            " TIME=" + (System.currentTimeMillis() - time) + "ms");
    }

    /**
     * This reduces the points outside of the desired bounds.
     * This is simpler than clipping but serves my purpose well --
     * far fewer points to store and draw.
     *
     * @param n the number of active points in xa and ya (at least 2).
     *    They must all be non-NaN.
     * @param xa the array with x values
     * @param ya the array with y values
     * @param west   degrees * 1000000, in range (0 - 360 or +-180) appropriate for xa
     * @param east   degrees * 1000000, in range (0 - 360 or +-180) appropriate for xa
     * @param south  degrees * 1000000
     * @param north  degrees * 1000000
     * @return n the new active number of points in the arrays (may be 0)
     */
    public static int reduce(int n, int x[], int y[], 
        int west, int east, int south, int north) {
 
        //algorithm relies on: 
        //for a series of points which are out of bounds in the same direction,
        //  only the first and last points need to be kept.
        //But corners are a problem, so always permanentely save first and last 
        //  out in given direction.
        if (n < 2) return n;
        int tn = 2;  //temp n good points  
        int i = 2;  //next point to look at
        while (i < n) {
            //look for sequences of points all to beyond one border
            boolean caughtSequence = false;
            if (x[tn-2] < west  && x[tn-1] < west  && x[i] < west)  {
                i++;
                while (i < n && x[i] < west) 
                    i++;
                caughtSequence = true;
            } else if (x[tn-2] > east  && x[tn-1] > east  && x[i] > east)  {
                i++;
                while (i < n && x[i] > east) 
                    i++;
                caughtSequence = true;
            } else if (y[tn-2] < south && y[tn-1] < south && y[i] < south)  {
                i++;
                while (i < n && y[i] < south) 
                    i++;
                caughtSequence = true;
            } else if (y[tn-2] > north && y[tn-1] > north && y[i] > north)  {
                i++;
                while (i < n && y[i] > north) 
                    i++;
                caughtSequence = true;
            } 

            if (caughtSequence) {
                //save the point one back
                x[tn] = x[i-1];
                y[tn] = y[i-1];
                tn++;
            } 
            
            //always save this point
            if (i < n) {
                x[tn] = x[i];
                y[tn] = y[i];
                tn++;
                i++;
            }
        }
        //if (n > 1000) String2.log("GSHHS.reduce n=" + n + " newN=" + (n2+1)); 

        //are the remaining points out of range?
        if (tn <= 4) {
            for (i = 0; i < tn; i++) if (x[i] >= west) break; 
            if (i == tn) return 0;
            for (i = 0; i < tn; i++) if (x[i] <= east) break; 
            if (i == tn) return 0;
            for (i = 0; i < tn; i++) if (y[i] >= south) break; 
            if (i == tn) return 0;
            for (i = 0; i < tn; i++) if (y[i] <= north) break; 
            if (i == tn) return 0;
        }
        return tn;
    }

    /**
     * This reduces the points outside of the desired bounds.
     * This is simpler than clipping but serves my purpose well --
     * far fewer points to store and draw.
     *
     * @param n the number of active points in xa and ya (at least 2).
     *    They must all be non-NaN. 
     * @param xa the array with x values
     * @param ya the array with y values
     * @param west
     * @param east
     * @param south
     * @param north
     * @return n the new active number of points in the arrays (may be 0)
     */
    public static int reduce(int n, double x[], double y[], 
        double west, double east, double south, double north) {
 
        //algorithm relies on: 
        //for a series of points which are out of bounds in the same direction,
        //  only the first and last points need to be kept.
        //But corners are a problem, so always permanentely save first and last 
        //  out in given direction.
        if (n < 2) return n;
        int tn = 2;  //temp n good points
        int i = 2;  //next point to look at
        while (i < n) {
            //look for sequences of points all to beyond one border
            boolean caughtSequence = false;
            if (x[tn-2] < west  && x[tn-1] < west  && x[i] < west)  {
                i++;
                while (i < n && x[i] < west) 
                    i++;
                caughtSequence = true;
            } else if (x[tn-2] > east  && x[tn-1] > east  && x[i] > east)  {
                i++;
                while (i < n && x[i] > east) 
                    i++;
                caughtSequence = true;
            } else if (y[tn-2] < south && y[tn-1] < south && y[i] < south)  {
                i++;
                while (i < n && y[i] < south) 
                    i++;
                caughtSequence = true;
            } else if (y[tn-2] > north && y[tn-1] > north && y[i] > north)  {
                i++;
                while (i < n && y[i] > north) 
                    i++;
                caughtSequence = true;
            } 

            if (caughtSequence) {
                //save the point one back
                x[tn] = x[i-1];
                y[tn] = y[i-1];
                tn++;
            } 
            
            //always save this point
            if (i < n) {
                x[tn] = x[i];
                y[tn] = y[i];
                tn++;
                i++;
            }
        }
        //if (n > 1000) String2.log("GSHHS.reduce n=" + n + " newN=" + (n2+1)); 

        //are the remaining points out of range?
        if (tn <= 4) {
            for (i = 0; i < tn; i++) if (x[i] >= west) break; 
            if (i == tn) return 0;
            for (i = 0; i < tn; i++) if (x[i] <= east) break; 
            if (i == tn) return 0;
            for (i = 0; i < tn; i++) if (y[i] >= south) break; 
            if (i == tn) return 0;
            for (i = 0; i < tn; i++) if (y[i] <= north) break; 
            if (i == tn) return 0;
        }
        return tn;
    }


    /**
     * This runs a unit test.
     */
    public static void test() throws Exception {
        verbose = true;

        //force creation of new file
        GeneralPath gp1 = getGeneralPath('h', 1, -135, -105, 22, 50, true);

        //read cached version
        long time = System.currentTimeMillis();
        GeneralPath gp2 = getGeneralPath('h', 1, -135, -105, 22, 50, true);
        time = System.currentTimeMillis() - time;

        //is it the same  (is GeneralPath.equals a deep test? probably not)

        //test speed
        Test.ensureTrue(time < 20, "time=" + time); 


    }


}
