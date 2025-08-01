/*
 * FileNameUtility Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.google.common.io.Resources;
import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.util.SSR;
import java.net.URL;
import java.util.GregorianCalendar;

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
   * This converts a a Dave-style file name (e.g., "AH2001068_2001070_ssta_westus" for composites,
   * or "AH2005069_044200h_ssta_westus" for single passes) into a CWBrowser file name (e.g., (days
   * 31 + 28 + n) "LAHsstaS3day_20030310120000_x-135_X-105_y22_Y50" for composites or
   * "LAHsstaSpass_20050310044200_x-135_X-105_y22_Y50" for single passes). It is ok if daveName
   * doesn't have "_<region>"; if so, the resulting cwName won't have WESN info. But if it does have
   * a region, it must be in the STANDARD_REGIONS_FILE_NAME. The time period implied by the begin
   * and end dates must be one of the TimePeriod options.
   *
   * @param daveName the .extension is ignored and not required
   * @return cwBrowserStyleName Source is always 'L'ocal. Units are always 'S'tandard. There is
   *     extension in the file name.
   * @throws Exception if trouble
   */
  public static String convertDaveNameToCWBrowserName(String daveName) throws Exception {

    String errorIn =
        String2.ERROR
            + " in FileNameUtility.convertDaveNameToCWBrowserName\n  daveName="
            + daveName
            + "\n  ";

    // determine the timePeriod, centeredDate and centeredTime
    String timePeriodInFileName, centeredIsoDateTime = "";
    if (daveName.charAt(16) == 'h') {
      String yyyyddd = daveName.substring(2, 9);
      String fileIsoDateTime =
          Calendar2.yyyydddToIsoDate(yyyyddd)
              + // may throw exception
              "T"
              + daveName.substring(10, 12)
              + ":"
              + daveName.substring(12, 14)
              + ":"
              + daveName.substring(14, 16);

      // center 25 and 33hour files  GA2005069_120000h_t24h
      String fourName = daveName.substring(18, 22);
      if (fourNameIs25Hour(fourName)) {
        timePeriodInFileName = TimePeriods.IN_FILE_NAMES.get(TimePeriods._25HOUR_INDEX);
        centeredIsoDateTime =
            Calendar2.formatAsISODateTimeT(
                Calendar2.isoDateTimeAdd(fileIsoDateTime, -25 * 60 / 2, Calendar2.MINUTE));
      } else if (fourNameIs33Hour(fourName)) {
        timePeriodInFileName = TimePeriods.IN_FILE_NAMES.get(TimePeriods._33HOUR_INDEX);
        centeredIsoDateTime =
            Calendar2.formatAsISODateTimeT(
                Calendar2.isoDateTimeAdd(fileIsoDateTime, -33 * 60 / 2, Calendar2.MINUTE));
      } else {
        // hday
        timePeriodInFileName = "pass";
        centeredIsoDateTime = fileIsoDateTime;
      }

    } else { // 2nd date is end date
      GregorianCalendar startGC =
          Calendar2.parseYYYYDDDZulu(daveName.substring(2, 9)); // throws Exception if trouble
      GregorianCalendar endGC =
          Calendar2.parseYYYYDDDZulu(daveName.substring(10, 17)); // throws Exception if trouble
      // add 1 day to endGC so precise end time
      endGC.add(Calendar2.DATE, 1);
      centeredIsoDateTime =
          Calendar2.epochSecondsToIsoStringT(
              (double)
                  Math2.roundToLong(
                      ((startGC.getTimeInMillis() + endGC.getTimeInMillis()) / 2.0) / 1000.0));
      int nDays =
          Math2.roundToInt(
              (endGC.getTimeInMillis() - startGC.getTimeInMillis() + 0.0)
                  / Calendar2.MILLIS_PER_DAY);
      int timePeriodIndex;
      if (nDays >= 28) {
        // must be 1 month; ensure start date is 1 and rawEnd date is last in same month
        GregorianCalendar rawEndGC =
            Calendar2.parseYYYYDDDZulu(daveName.substring(10, 17)); // throws Exception if trouble
        timePeriodIndex = TimePeriods.exactTimePeriod(TimePeriods.MONTHLY_OPTION);
        Test.ensureEqual(
            Calendar2.getYear(startGC),
            Calendar2.getYear(rawEndGC),
            "Monthly file: Begin and end year not the same.");
        Test.ensureEqual(
            startGC.get(Calendar2.MONTH),
            rawEndGC.get(Calendar2.MONTH),
            "Monthly file: Begin and end month not the same.");
        Test.ensureEqual(startGC.get(Calendar2.DATE), 1, "Monthly file: Begin date isn't 1.");
        Test.ensureEqual(
            rawEndGC.get(Calendar2.DATE),
            rawEndGC.getActualMaximum(Calendar2.DATE),
            "Monthly file: End date isn't last date in month.");
      } else {
        // EEEK! "closest" is useful to do the match
        // BUT if there is an unknown time period (e.g., 7 days),
        //  this will find the closest and not complain.
        // So at least insist on exact match
        // BUT!!! no way to tell if Dave intended e.g., 5 day and file name is e.g., 4 day.
        timePeriodIndex = TimePeriods.closestTimePeriod(nDays * 24, TimePeriods.OPTIONS);
        Test.ensureEqual(
            nDays,
            TimePeriods.N_HOURS.get(timePeriodIndex) / 24,
            errorIn + nDays + "-day time period not yet supported.");
      }
      timePeriodInFileName = TimePeriods.IN_FILE_NAMES.get(timePeriodIndex);
    }
    String centeredDateTime = Calendar2.removeSpacesDashesColons(centeredIsoDateTime);

    // convert Dave's region name to WESN info
    int po = daveName.indexOf('.', 21); // 21 must exist
    if (po < 0) po = daveName.length();
    String region = po < 23 ? "" : daveName.substring(23, po);
    StringBuilder wesnSB = new StringBuilder();
    if (region.length() > 0) {
      // get the line in the regions file with the region
      String line =
          SSR.getFirstLineStartsWith(STANDARD_REGIONS_FILE_NAME, File2.ISO_8859_1, region);
      Test.ensureNotNull(
          line,
          errorIn + "region \"" + region + "\" not found in " + STANDARD_REGIONS_FILE_NAME + ".");

      // split at whitespace
      String fields[] = line.split("\\s+"); // s = whitespace regex
      Test.ensureTrue(
          fields.length >= 2,
          errorIn + "no range defined for " + region + " in " + STANDARD_REGIONS_FILE_NAME + ".");
      wesnSB.append("_x" + fields[1]); // e.g., now _x225/255/22/50
      po = wesnSB.indexOf("/");
      wesnSB.replace(po, po + 1, "_X");
      po = wesnSB.indexOf("/");
      wesnSB.replace(po, po + 1, "_y");
      po = wesnSB.indexOf("/");
      wesnSB.replace(po, po + 1, "_Y"); // e.g., "_x225_X255_y22_Y50";
    }

    // return the desired file name
    // always the 'L'ocal variant
    // e.g., AT
    // e.g., ssta
    // always the standard units   (all files now stored that way)
    // String2.log("convertDaveNameToCWBrowserName " + daveName + " -> " + cwName);
    return "L"
        + // always the 'L'ocal variant
        daveName.substring(0, 2)
        + // e.g., AT
        daveName.substring(18, 22)
        + // e.g., ssta
        "S"
        + // always the standard units   (all files now stored that way)
        timePeriodInFileName
        + "_"
        + centeredDateTime
        + wesnSB;
  }

  /**
   * This indicates if a 4 letter variable name is from a 25 hour file, which is often handled as a
   * special case in this class. Yes, it is an anomaly that Dave's 24h variable names represent 25
   * hours, but 33h variable names represent 33 hours. Dave changed all use of 24h to 25h around
   * 2006-06-01. [No, still GAt24h.]
   *
   * @param fourName e.g., u24h or u25h (true) or ssta (false)
   * @return true if this is a special 25 hour file.
   */
  public static boolean fourNameIs25Hour(String fourName) {
    String s = fourName.substring(1, 4);
    return s.equals("24h") || s.equals("25h");
  }

  /**
   * This indicates if a 4 letter variable name is from a 33 hour file, which is often handled as a
   * special case in this class.
   *
   * @param fourName e.g., u33h or u33h (true) or ssta (false)
   * @return true if this is a special 33 hour file.
   */
  public static boolean fourNameIs33Hour(String fourName) {
    String s = fourName.substring(1, 4);
    return s.equals("33h");
  }

  /**
   * This returns the centered date String (e.g., 20030304 or 20030304hhmmss) in the compact form
   * straight from the file name.
   *
   * @param fileName a base or custom file name.
   * @return the compact end date String.
   */
  public static String getRawDateString(String fileName) {
    int po1 = fileName.indexOf('_');
    int po2 = fileName.indexOf('_', po1 + 1);
    if (po2 < 0) po2 = fileName.indexOf('.', po1 + 1);
    if (po2 < 0) po2 = fileName.length();
    // String2.log("getRawDateString fileName=" + fileName + " -> " + d);
    return fileName.substring(po1 + 1, po2);
  }

  /**
   * This returns the centered dateTime (straight from getRawDateString) as a GregorianCalendar
   * object.
   *
   * @param fileName a base or custom file name.
   * @return the GregorianCalendar object
   */
  public static GregorianCalendar getCenteredCalendar(String fileName) throws Exception {
    return Calendar2.parseCompactDateTimeZulu(
        getRawDateString(fileName)); // throws Exception if trouble
  }

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
