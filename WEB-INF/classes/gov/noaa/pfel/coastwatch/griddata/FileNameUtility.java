/*
 * FileNameUtility Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.String2;
import com.google.common.io.Resources;
import java.net.URL;

/**
 * This class gets information related to the CoastWatch Browser's custom file names from
 * CWBrowser.properties. It is my hope that all FILE_NAME_RELATED_CODE be in this class.
 *
 * <p>Most of the methods in the class are related to metadata attributes. See MetaMetadata.txt in
 * this directory for details.
 *
 * <ul>
 *   <li>The base file name format is e.g., LATsstaS1day_20030304.<extension> for composites, or
 *       LATsstaSpass_20051006044200.<extension>" for single passes). When range information is
 *       missing (as it is with base file names), the regionMin/MaxX/Y is assumed.
 *   <li>The longer "custom" file name format which includes range information, e.g.,
 *       LATsstaS1day_20030304_x-135_X-105_y22_Y50.<extension> for composites (but
 *       LATsstaS1day_20030304_x-135_X-113_y30_Y50_<other stuff> is allowed)
 *   <li>Letter 0 is 'L'ocal or 'T'Opendap or 'U'Thredds.
 *   <li>Letter 7 is 'S'tandard (e.g., the units in the source file) or 'A'lternate units.
 *   <li>For a single pass (not a composite), the end date will be a dateTime with HHMMSS, e.g.,
 *       20030304140245.
 *   <li>Code that is sensitive to the file name format has "//FILE_NAME_RELATED_CODE" in a comment,
 *       e.g., externally, Luke's grd2Hdf script is relies on a specific file name format for files
 *       generated from CWBrowser.
 * </ul>
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-27
 */
public class FileNameUtility {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static final boolean verbose = false;

  public static final URL STANDARD_REGIONS_FILE_NAME =
      Resources.getResource("gov/noaa/pfel/coastwatch/griddata/regions");

  public static String getStandardNameVocabulary() {
    // 2015-04-17 new, longer name format is from ACDD 1.3
    return "CF Standard Name Table v70";
  } // was CF-1.0 and CF-11, 2015-04-17 was CF-12, 2015-07-23 was v27 2018-06-20 was v29 2019-12-10

  /**
   * This returns the unique id e.g., "LATsstaS1day_20030304_x-135_X-134.25_y22_Y23".
   *
   * @param internalName the 7 character name e.g. LATssta
   * @param standardUnits true if using the standard units
   * @param timePeriodValue one of the standard TimePeriods.OPTIONS, e.g., 1 day
   * @param isoCenteredTime e.g., 2003-03-04
   * @param minLon e.g., -135
   * @param maxLon e.g., -134.25
   * @param minLat e.g., 22
   * @param maxLat e.g., 23
   */
  /* 2019-05-07 see getID.
  public static String getUniqueID(String internalName,
      boolean standardUnits, String timePeriodValue,
      String isoCenteredTime, double minLon, double maxLon,
      double minLat, double maxLat) {

      String name =
          makeBaseName(internalName,
              standardUnits? 'S' : 'A',
              timePeriodValue, isoCenteredTime) +
          makeWESNString(minLon, maxLon, minLat, maxLat);
      return name;
  }*/

  /**
   * This generates the standard WESN string (now, e.g., _x-135_X-105_y22_Y50) which is added to
   * filenames.
   *
   * @param minLon is the minimum longitude in decimal degrees east
   * @param maxLon is the maximum longitude in decimal degrees east
   * @param minLat is the minimum latitude in decimal degrees north
   * @param maxLat is the maximum latitude in decimal degrees north
   * @return the WESN values in standard format.
   */
  public static String makeWESNString(double minLon, double maxLon, double minLat, double maxLat) {

    return "_x"
        + String2.genEFormat10(minLon)
        + // 10 decimal digits should be enough; I need some cleaning
        "_X"
        + String2.genEFormat10(maxLon)
        + "_y"
        + String2.genEFormat10(minLat)
        + "_Y"
        + String2.genEFormat10(maxLat);
  }

  /**
   * This adds formats nLon and nLat for addition to a filename.
   *
   * @param nLon is the number of lon values
   * @param nLat is the number of lat values
   * @return nLon, nLat values in standard format.
   */
  public static String makeNLonNLatString(int nLon, int nLat) {
    return "_nx" + nLon + "_ny" + nLat;
  }

  /*
  #   0=category name`  1=Palette Name` 2=scale (Linear or Log) ` 3=Unused (to get data to standard units)`
  #   4=Standard (SI) Units Name` 5=Min (in std units)` 6=Max (in std units)` 7=contourLinesAt (in std units)`
  #   8=latLonFractionDigits` 9=dataFractionDigits`
  #   10=defaultUnits (either 'S'= standard SI units (the units in the original file), 'A'=alternate units) `
  #   11=altScaleFactor (1st step to get data from standard units to altUnits)` 12=altOffset (2nd step)`
  #   13=altUnits name` 14=altMin` 15=altMax` 16=altContourLinesAt`
  #   17=(unused)` 18=altDataFractionDigits`
  #   19=daysTillDataAccessAllowed`
  #   20=satellite` 21=sensor` 22=BoldTitle` 23=Courtesy
  // `Rainbow`     Linear`1`degree_C`      8`32`4`           4`1`S`1.8`    32`degree_F`     45`90`10` `1`-1`POES`AVHRR HRPT`1.25 km SST, NOAA POES AVHRR, Local Area Coverage`NOAA NWS Monterey and NOAA CoastWatch
  */

}
