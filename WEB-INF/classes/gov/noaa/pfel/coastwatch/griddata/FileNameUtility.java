/*
 * FileNameUtility Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.ResourceBundle2;
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
  public static boolean verbose = false;

  public String fullClassName;
  private ResourceBundle2 classRB2;
  private ResourceBundle2 dataSetRB2;

  public double regionMinX, regionMaxX, regionMinY, regionMaxY;

  public static URL STANDARD_REGIONS_FILE_NAME =
      Resources.getResource("gov/noaa/pfel/coastwatch/griddata/regions");

  public static String getAcknowledgement() {
    return "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD";
  }

  public static String getCDMDataType() {
    return "Grid";
  }

  public static String getContributorRole() {
    return "Source of level 2 data.";
  }

  public static String getConventions() {
    return "COARDS, CF-1.6, ACDD-1.3, CWHDF";
  }

  public static String getMetadataConventions() {
    return "COARDS, CF-1.6, ACDD-1.3, CWHDF";
  }

  public static String getCreatorEmail() {
    return DataHelper.CW_CREATOR_EMAIL;
  }

  public static String getCreatorName() {
    return DataHelper.CW_CREATOR_NAME;
  }

  public static String getCreatorURL() {
    return DataHelper.CW_CREATOR_URL;
  }

  public static String getDateCreated() {
    return Calendar2.formatAsISODate(Calendar2.newGCalendarZulu());
  }

  public static String getKeywordsVocabulary() {
    return "GCMD Science Keywords";
  }

  public static String getLatUnits() {
    return "degrees_north";
  }

  public static String getLicense() {
    return "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.";
  }

  public static String getLonUnits() {
    return "degrees_east";
  }

  public static String getNamingAuthority() {
    return "gov.noaa.pfeg.coastwatch";
  }

  public static String getProcessingLevel() {
    return "3 (projected)";
  }

  public static String getProject() {
    return DataHelper.CW_PROJECT;
  }

  public static String getStandardNameVocabulary() {
    // 2015-04-17 new, longer name format is from ACDD 1.3
    return "CF Standard Name Table v70";
  } // was CF-1.0 and CF-11, 2015-04-17 was CF-12, 2015-07-23 was v27 2018-06-20 was v29 2019-12-10

  // was 55

  private String categoryLetters;
  private String[] categoryNames;

  /**
   * This uses the class name to find the <classname>.properties and DataSet.properties file (in
   * same directory) in order to look up information about the datasets.
   *
   * @param fullClassName e.g., gov.noaa.pfel.coastwatch.CWBrowser
   */
  public FileNameUtility(String fullClassName) throws Exception {
    String errorInMethod = String2.ERROR + " in FileNameUtility.constructor: \n";
    this.fullClassName = fullClassName;
    String defaultName = File2.getNameNoExtension(fullClassName) + ".BrowserDefault";
    classRB2 =
        new ResourceBundle2(fullClassName, defaultName); // primary and secondary resourceBundle2's
    dataSetRB2 = new ResourceBundle2(File2.getNameNoExtension(fullClassName) + ".DataSet");

    String tRegionInfo[] = String2.split(classRB2.getString("regionInfo", null), '\f');
    String regionInfo[][] = new String[tRegionInfo.length][];
    boolean trouble;
    for (int i = 0; i < tRegionInfo.length; i++) {
      regionInfo[i] = String2.split(tRegionInfo[i], ',');
      trouble = regionInfo[i].length != 8;
      if (verbose || trouble) {
        String2.log(
            (trouble ? errorInMethod : "")
                + "tRegionInfo["
                + i
                + "]="
                + String2.toCSSVString(regionInfo[i]));
        Test.ensureEqual(regionInfo[i].length, 8, tRegionInfo[i]);
      }
    }
    double regionsImageMinXDegrees = String2.parseDouble(regionInfo[0][1]);
    double regionsImageMaxXDegrees = String2.parseDouble(regionInfo[0][2]);
    double regionsImageMinYDegrees =
        String2.parseDouble(regionInfo[0][4]); // y's reversed: they correspond to min/maxY pixels
    double regionsImageMaxYDegrees = String2.parseDouble(regionInfo[0][3]);
    regionMinX = Math.min(regionsImageMinXDegrees, regionsImageMaxXDegrees);
    regionMaxX = Math.max(regionsImageMinXDegrees, regionsImageMaxXDegrees);
    regionMinY = Math.min(regionsImageMinYDegrees, regionsImageMaxYDegrees);
    regionMaxY = Math.max(regionsImageMinYDegrees, regionsImageMaxYDegrees);

    // categories
    categoryLetters = dataSetRB2.getString("categoryLetters", null);
    String tcNames = dataSetRB2.getString("categoryNames", null);
    Test.ensureNotNull(
        categoryLetters, errorInMethod + "categoryLetters wasn't found in DataSet.properties.");
    Test.ensureNotNull(
        categoryLetters, errorInMethod + "categoryNames wasn't found in DataSet.properties.");
    categoryNames = String2.split(tcNames, '`');
    Test.ensureEqual(
        categoryLetters.length(),
        categoryNames.length,
        errorInMethod + "categoryLetters and categoryNames lengths are different.");
  }

  /** The classRB2 with properties for this program. */
  public ResourceBundle2 classRB2() {
    return classRB2;
  }

  /** The dataSetRB2 with properties for all dataSets. */
  public ResourceBundle2 dataSetRB2() {
    return dataSetRB2;
  }

  /**
   * This ensures that the data in DataSet.properties for seven is valid.
   *
   * @param seven the 7 char name e.g., LATssta.
   * @param thoroughlyCheckThreddsData if seven.char(0) is 'U', this determines whether the
   *     extensive testing is done. (The info is needed for GridSaveAs, but not for the browsers.)
   * @throws Exception if invalid
   */
  public void ensureValidDataSetProperties(String seven, boolean thoroughlyCheckThreddsData) {
    String errorInMethod = String2.ERROR + " in FileNameUtility.ensureValid(" + seven + "): \n";
    Test.ensureTrue(seven.length() > 0, errorInMethod + "sevenName is \"\".");
    String six = seven.substring(1);
    Test.ensurePrintable(
        dataSetRB2.getString(seven + "FileInfo", null),
        errorInMethod + seven + "FileInfo wasn't in DataSet.properties or is not printable.");

    // these things aren't normally needed for 'U' datasets (but GridSaveAs uses them)
    if (seven.charAt(0) == 'A') {
      // do nothing since Anomaly data sets never need this info
    } else if (seven.charAt(0) == 'T') {
    } else if (thoroughlyCheckThreddsData || seven.charAt(0) != 'U') {
      String infoString = dataSetRB2.getString(six + "Info", null);
      Test.ensureNotNull(infoString, six + "Info wasn't in DataSet.properties.");
      Test.ensureASCII(infoString, six + "Info is not ASCII.");
      String sar[] = String2.split(infoString, '`');
      Test.ensureEqual(sar.length, 20, six + "Info.length wasn't 20.");
      Test.ensureEqual(
          sar[0].length(),
          1,
          errorInMethod + seven + " Info[0].length() isn't 1 in DataSet.properties.");
      Test.ensureTrue(
          categoryLetters.indexOf(sar[0].charAt(0)) >= 0,
          errorInMethod
              + seven
              + " Info[0] isn't a standard categoryLetter in DataSet.properties.");
      Test.ensureASCII(
          dataSetRB2.getString(six + "Satellite", null),
          errorInMethod + six + "Satellite wasn't in DataSet.properties or is not ASCII.");
      Test.ensureASCII(
          dataSetRB2.getString(six + "Sensor", null),
          errorInMethod + six + "Sensor wasn't in DataSet.properties or is not ASCII.");
      String boldTitle = dataSetRB2.getString(six + "BoldTitle", null);
      Test.ensurePrintable(
          boldTitle,
          errorInMethod + six + "BoldTitle wasn't in DataSet.properties or is not printable.");
      Test.ensureTrue(
          boldTitle.indexOf('<') == -1, errorInMethod + six + "BoldTitle contains '<'.");
      Test.ensureTrue(
          boldTitle.indexOf('>') == -1, errorInMethod + six + "BoldTitle contains '>'.");
      Test.ensureTrue(
          boldTitle.indexOf('&') == -1, errorInMethod + six + "BoldTitle contains '&'.");
      Test.ensureASCII(
          dataSetRB2.getString(six + "Courtesy", null),
          errorInMethod + six + "Courtesy wasn't in DataSet.properties or is not ASCII.");
      Test.ensureASCII(
          dataSetRB2.getString(six + "Keywords", null),
          errorInMethod + six + "Keywords wasn't in DataSet.properties or is not ASCII.");
      Test.ensureASCII(
          dataSetRB2.getString(six + "StandardName", null),
          errorInMethod + six + "StandardName wasn't in DataSet.properties or is not ASCII.");
      Test.ensureASCII(
          dataSetRB2.getString(six + "References", null),
          errorInMethod + six + "References wasn't in DataSet.properties or is not ASCII.");
      String fgdcString = dataSetRB2.getString(six + "FGDC", null);
      Test.ensureASCII(
          fgdcString, errorInMethod + six + "FGDC wasn't in DataSet.properties or is not ASCII.");
      Test.ensureTrue(
          fgdcString.indexOf("<resdesc>") > 0,
          errorInMethod
              + six
              + "FGDC in DataSet.properties is incomplete (missing 'resdesc') (missing slash n?).");
      Test.ensureTrue(
          fgdcString.indexOf("& ") == -1,
          errorInMethod + six + "FGDC in DataSet.properties contains unencoded &amp;.");
      String s = dataSetRB2.getString(seven + "InfoUrl", null); // don't allow null for InfoUrl
      if (s != null)
        Test.ensureASCII(s, errorInMethod + seven + "InfoUrl in DataSet.properties isn't ASCII.");
    }
  }

  /**
   * This ensures that the HTML file with the html info page from the <internalName>InfoUrl from the
   * classRB2 file actually exists.
   *
   * <p>XXXxxxxInfoUrl may be "". <br>
   * If XXXxxxxInfoUrl starts with http:// , it must be a file on a remote server. <br>
   * If you accidentally, use http:// for a file on this computer, the Shared constructor
   * (specifically, calls to ensureInfoUrlExists) will fail after a long timeout because it tries to
   * access a web page on this computer (which is forbidden for some reason). <br>
   * Otherwise, XXXxxxxInfoUrl must be a file on this computer, in infoUrlBaseDir, accessible to
   * users as infoUrlBaseUrl + XXXxxxxInfoUrl
   *
   * @param internal7Name the 7 character internal dataset name, e.g., LATssta.
   * @throws Exception if the file doesn't exist
   */
  public void ensureInfoUrlExists(String internal7Name) throws Exception {
    String infoUrl = dataSetRB2.getString(internal7Name + "InfoUrl", null);
    Test.ensureNotNull(infoUrl, internal7Name + "InfoUrl not in DataSet.properties file.");
    if (infoUrl.equals("")) {
      return;
    } else if (String2.isUrl(infoUrl)) {
      try {
        SSR.getUrlResponseArrayList(infoUrl);
        return;
      } catch (Exception e) {
        throw new Exception(
            String2.ERROR
                + " in FileNameUtility.ensureInfoUrlExists: error while reading InfoUrl for "
                + internal7Name
                + ":\n"
                + e);
      }
    } else {
      String infoUrlBaseDir = dataSetRB2.getString("infoUrlBaseDir", null);
      Test.ensureTrue(
          File2.isFile(infoUrlBaseDir + infoUrl),
          String2.ERROR
              + " in FileNameUtility.ensureInfoUrlExists: file="
              + infoUrlBaseDir
              + infoUrl
              + " doesn't exist (for "
              + internal7Name
              + ").");
    }
  }

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
        timePeriodInFileName = TimePeriods.IN_FILE_NAMES[TimePeriods._25HOUR_INDEX];
        centeredIsoDateTime =
            Calendar2.formatAsISODateTimeT(
                Calendar2.isoDateTimeAdd(fileIsoDateTime, -25 * 60 / 2, Calendar2.MINUTE));
      } else if (fourNameIs33Hour(fourName)) {
        timePeriodInFileName = TimePeriods.IN_FILE_NAMES[TimePeriods._33HOUR_INDEX];
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
              Math2.roundToLong(
                  ((startGC.getTimeInMillis() + endGC.getTimeInMillis()) / 2) / 1000.0));
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
            TimePeriods.N_HOURS[timePeriodIndex] / 24,
            errorIn + nDays + "-day time period not yet supported.");
      }
      timePeriodInFileName = TimePeriods.IN_FILE_NAMES[timePeriodIndex];
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
    String cwName =
        "L"
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
            + wesnSB.toString();
    // String2.log("convertDaveNameToCWBrowserName " + daveName + " -> " + cwName);
    return cwName;
  }

  /**
   * This converts a CWBrowser file name (e.g., (days 31 + 28 + n)
   * "LAHsstaS3day_20030310120000_x-135_X-105_y22_Y50" for composites or
   * "LAHsstaSpass_20050310044200_x-135_X-105_y22_Y50" for single passes). into a Dave-style file
   * name (e.g., "AH2001068_2001070_ssta_westus" for composites, or "AH2005069_044200h_ssta_westus"
   * for single passes) It is ok if cwName doesn't have WESN info; if so, the resulting daveName
   * won't have region info. But if it does have WESN info, there must be a matching region in
   * STANDARD_REGIONS_FILE_NAME.
   *
   * @param cwName the optional .extension is ignored.
   * @return Dave-style name
   * @throws Exception if trouble
   */
  public String convertCWBrowserNameToDaveName(String cwName) throws Exception {

    String errorIn = String2.ERROR + " in FileNameUtility.convertCWBrowserNameToDaveName: ";

    // create the regionInfo string I will search for, e.g., 225/255/22/50
    boolean hasRegionInfo = cwName.indexOf("_x") > 0;
    String regionName = "";
    if (hasRegionInfo) {
      String regionInfo =
          String2.genEFormat10(Math2.looserAngle0360(getMinX(cwName)))
              + "/"
              + String2.genEFormat10(Math2.looserAngle0360(getMaxX(cwName)))
              + "/"
              + String2.genEFormat10(getMinY(cwName))
              + "/"
              + String2.genEFormat10(getMaxY(cwName));

      // get the region name from the matching line in the regions file
      String line =
          SSR.getFirstLineMatching(
              STANDARD_REGIONS_FILE_NAME,
              File2.ISO_8859_1,
              "[^#].*?\\s" + regionInfo + "\\s.*"); // # = not a comment line
      Test.ensureNotNull(
          line,
          errorIn
              + "no region with range \""
              + regionInfo
              + "\" found in "
              + STANDARD_REGIONS_FILE_NAME
              + ".");
      regionName = "_" + line.split("\\s")[0];
    }

    // get the calendar info
    GregorianCalendar endDateTimeGC = getEndCalendar(cwName); // e.g., ending in 00:00:00
    String calendarString;
    int nHours = getTimePeriodNHours(cwName);
    if (nHours <= 1) {
      calendarString =
          Calendar2.formatAsYYYYDDD(endDateTimeGC)
              + "_"
              + Calendar2.formatAsCompactDateTime(endDateTimeGC).substring(8)
              + // "20040102030405"
              "h";
    } else if (nHours % 24 != 0) { // handle 25hour and 33hour
      String cleanCompactDateTime = Calendar2.formatAsCompactDateTime(endDateTimeGC);
      calendarString =
          Calendar2.formatAsYYYYDDD(endDateTimeGC)
              + "_"
              + cleanCompactDateTime.substring(8, 14)
              + "h";
      // String2.log("cwName=" + cwName + " cleanCompactEnd=" + cleanCompactDateTime + "
      // calendarString=" + calendarString);
    } else {
      GregorianCalendar endDateTimeGCM1 = (GregorianCalendar) endDateTimeGC.clone();
      endDateTimeGCM1.add(Calendar2.SECOND, -1); // e.g., ending in 23:59:59 of last day
      // String2.log("cwName=" + cwName + " endDateTimeGC=" +
      // Calendar2.formatAsISODateTimeT(endDateTimeGC));
      String daveEndDate = Calendar2.formatAsYYYYDDD(endDateTimeGCM1);
      calendarString = Calendar2.formatAsYYYYDDD(getStartCalendar(cwName)) + "_" + daveEndDate;
    }

    return cwName.substring(1, 3)
        + // AT
        calendarString
        + "_"
        + cwName.substring(3, 7)
        + // e.g., ssta
        regionName;
  }

  /**
   * This returns the 6 character dataset designator (e.g., ATssta).
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the 6 character dataset designator (e.g., ATssta). [2006-11-15 In the past there were
   *     special cases: GAt24h, GAt25h, and GAt33h returned GAssta. But now: no special cases.]
   */
  public static String get6CharName(String fileName) {
    String name6 = fileName.substring(1, 7);
    // deal with special cases  where dave uses >1 variable names
    // if (name6.equals("GAt24h") || name6.equals("GAt25h") || name6.equals("GAt33h")) return
    // "GAssta";
    return name6;
  }

  /**
   * This returns the 7 character dataset designator (e.g., LATssta).
   *
   * @param fileName a base or custom file name.
   * @return the 7 character dataset designator (e.g., LATssta). [2006-11-15 In the past there were
   *     special cases: XGAt24h, XGAt25h, and XGAt33h returned XGAssta. But now: no special cases.]
   */
  public static String get7CharName(String fileName) {
    return fileName.charAt(0) + get6CharName(fileName);
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
   * This indicates if this is a 25 hour file, which is often handled as a special case in this
   * class. Yes, it is an anomaly that Dave's 24h variable names represent 25 hours, but 33h
   * variable names represent 33 hours. Dave changed all use of 24h to 25h around 2006-06-01. [No,
   * still GAt24h.]
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return true if this is a special 25 hour file.
   */
  public static boolean is25Hour(String fileName) {
    return fourNameIs25Hour(fileName.substring(3, 7));
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
   * This indicates if this is a 33 hour file, which is often handled as a special case in this
   * class.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return true if this is a special 33 hour file.
   */
  public static boolean is33Hour(String fileName) {
    return fourNameIs33Hour(fileName.substring(3, 7));
  }

  /**
   * This indicates if the file uses alternate units.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return true if the file uses alternate units.
   */
  public static boolean getAlternateUnits(String fileName) {
    return fileName.charAt(7) == 'A';
  }

  /**
   * This returns the in-file-name time period String (e.g., 1day).
   *
   * @param fileName a base or custom file name.
   * @return the time period String.
   */
  public static String getTimePeriodString(String fileName) {
    int po = fileName.indexOf('_');
    return fileName.substring(8, po);
  }

  /**
   * This returns an index of TimePeriods.IN_FILE_NAMES.
   *
   * @param fileName a base or custom file name with one of TimePeriods.IN_FILE_NAMES options.
   * @return an index from TimePeriods.IN_FILE_NAMES corresponding to the exact match of the time
   *     period in the file name
   * @throws Exception if trouble
   */
  public static int getTimePeriodIndex(String fileName) throws Exception {
    String s = getTimePeriodString(fileName);
    int i = String2.indexOf(TimePeriods.IN_FILE_NAMES, s);
    if (i == -1)
      throw new RuntimeException(
          String2.ERROR
              + " in FileNameUtility.getTimePeriodIndex("
              + fileName
              + "):\ntimePeriod '"
              + s
              + "' not recognized.");
    return i;
  }

  /**
   * This returns the generic number of hours in the time period (e.g., 1 month returns 30*24,
   * regardless of number of days in the month).
   *
   * @param fileName a base or custom file name with one of TimePeriods.IN_FILE_NAMES options.
   * @return number of hours in the time period (from TimePeriods.N_HOURS)
   * @throws Exception if trouble
   */
  public static int getTimePeriodNHours(String fileName) throws Exception {
    return TimePeriods.N_HOURS[getTimePeriodIndex(fileName)];
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
    String d = fileName.substring(po1 + 1, po2);
    // String2.log("getRawDateString fileName=" + fileName + " -> " + d);
    return d;
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
   * This returns the end dateTime as a GregorianCalendar object. The date for all composites will
   * indicate the end time of 00:00.
   *
   * @param fileName a base or custom file name.
   * @return the GregorianCalendar object
   */
  public static GregorianCalendar getEndCalendar(String fileName) throws Exception {
    return TimePeriods.getEndCalendar(
        TimePeriods.OPTIONS[getTimePeriodIndex(fileName)],
        Calendar2.formatAsISODateTimeT(getCenteredCalendar(fileName)),
        null);
  }

  /**
   * This returns the start dateTime as a GregorianCalendar object. The startTime for all composites
   * will be 00:00. Yes, this is precise for 25hour and 33hour.
   *
   * @param fileName a base or custom file name.
   * @return the GregorianCalendar object.
   * @throws Exception if trouble
   */
  public static GregorianCalendar getStartCalendar(String fileName) throws Exception {
    return TimePeriods.getStartCalendar(
        TimePeriods.OPTIONS[getTimePeriodIndex(fileName)],
        Calendar2.formatAsISODateTimeT(getCenteredCalendar(fileName)),
        null);
  }

  /**
   * This returns the passDate array for CW HDF metadata.
   *
   * @param fileName a base (assumes full region) or custom file name.
   * @return int[] with the days since Jan 1, 1970, e.g., {12806, 12807, 12808} for the pass or for
   *     each day in the composite
   * @throws Exception if trouble
   */
  public static int[] getPassDate(String fileName) throws Exception {
    long startMillis = getStartCalendar(fileName).getTimeInMillis();
    long endMillis = getEndCalendar(fileName).getTimeInMillis();
    // avoid problems with negative times (i.e., dates before 1970, e.g., climatology)
    if (startMillis != endMillis) {
      startMillis += 1000;
      endMillis -= 1000; // so 59:59
    }

    // int division truncates result
    int startDay = (int) (startMillis / Calendar2.MILLIS_PER_DAY); // safe
    int endDay = (int) (endMillis / Calendar2.MILLIS_PER_DAY); // safe
    int nDays = endDay - startDay + 1;

    int pass_date[] = new int[nDays];
    for (int i = 0; i < nDays; i++) pass_date[i] = startDay + i;
    return pass_date;
  }

  /**
   * This returns the startTime array for CW HDF metadata.
   *
   * @param fileName a base (assumes full region) or custom file name.
   * @return double[] with seconds since midnight, for a single pass (e.g., {3124}) or for each day
   *     in the composite (e.g., {0, 0, 0}). 25Hour and 33Hour time periods start at specific time
   *     on first of the 2 or 3 days (even though timePeriod may be hday).
   * @throws Exception if trouble
   */
  public static double[] getStartTime(String fileName) throws Exception {

    long startMillis = getStartCalendar(fileName).getTimeInMillis();
    long endMillis = getEndCalendar(fileName).getTimeInMillis();
    // this is crude, but avoid problems with negative times (i.e., dates before 1970, e.g.,
    // climatology)
    if (startMillis != endMillis) {
      startMillis += 1000;
      endMillis -= 1000; // so 59:59
    }

    // single pass
    if (startMillis == endMillis)
      // int division 1000 truncates to second; % discards nDays
      return new double[] {(endMillis / 1000) % Calendar2.SECONDS_PER_DAY};

    // composite
    // int division truncates result
    int startDays = (int) (startMillis / Calendar2.MILLIS_PER_DAY); // safe
    int endDays = (int) (endMillis / Calendar2.MILLIS_PER_DAY); // safe
    int nDays = endDays - startDays + 1;
    double dar[] = new double[nDays]; // filled with 0's

    // special cases: 25Hour and 33Hour start at some specific time on first day
    //  other days start at 00:00
    if (is25Hour(fileName) || is33Hour(fileName))
      // -1000 just adjusts for +1000 above
      dar[0] = ((startMillis - 1000) / 1000) % Calendar2.SECONDS_PER_DAY;

    return dar;
  }

  /**
   * This returns minX (e.g., -135).
   *
   * @param fileName a base (assumes full region) or custom file name.
   * @return minX.
   */
  public double getMinX(String fileName) {
    int pox = fileName.indexOf("_x");
    if (pox < 0) return regionMinX;

    int poX = fileName.indexOf("_X", pox + 2);
    return String2.parseDouble(fileName.substring(pox + 2, poX));
  }

  /**
   * This returns maxX (e.g., -105).
   *
   * @param fileName a base (assumes full region) or custom file name.
   * @return maxX.
   */
  public double getMaxX(String fileName) {
    int poX = fileName.indexOf("_X");
    if (poX < 0) return regionMaxX;

    int poy = fileName.indexOf("_y", poX + 2);
    return String2.parseDouble(fileName.substring(poX + 2, poy));
  }

  /**
   * This returns minY (e.g., 22).
   *
   * @param fileName a base (assumes full region) or custom file name.
   * @return minY.
   */
  public double getMinY(String fileName) {
    int poy = fileName.indexOf("_y");
    if (poy < 0) return regionMinY;

    int poY = fileName.indexOf("_Y", poy + 2);
    return String2.parseDouble(fileName.substring(poy + 2, poY));
  }

  /**
   * This returns maxY (e.g., 50).
   *
   * @param fileName a base (assumes full region) or custom file name.
   * @return maxY.
   */
  public double getMaxY(String fileName) {
    int poY = fileName.indexOf("_Y");
    if (poY < 0) return regionMaxY;

    int poEnd = fileName.indexOf('_', poY + 2);
    if (poEnd < 0) {
      poEnd = fileName.indexOf('.', poY + 2);
      // is that "." followed by decimal digits or e.g., .grd?
      if (poEnd > 0) { // append any decimal digits
        poEnd++;
        while (poEnd < fileName.length() && String2.isDigit(fileName.charAt(poEnd))) poEnd++;
      }
    }
    if (poEnd < 0) poEnd = fileName.length();
    return String2.parseDouble(fileName.substring(poY + 2, poEnd));
  }

  /**
   * This returns one piece (e.g., &lt;metadata&gt;&lt;idinfo&gt;&lt;descript&gt;&lt;abstract&gt;)
   * of dataset-specific FGDC info.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @param infoName e.g., <metadata><idinfo><descript><abstract>
   * @return the related info (or null if not available)
   */
  public String getFGDCInfo(String fileName, String infoName) {
    String attName = get6CharName(fileName) + "FGDC";
    String fgdcInfo = dataSetRB2.getString(attName, null);
    if (fgdcInfo == null) return null;
    String sar[] = String2.splitNoTrim(fgdcInfo, '\n');
    for (int i = 0; i < sar.length; i++)
      if (sar[i].startsWith(infoName)) return sar[i].substring(infoName.length());
    return null;
  }

  /**
   * This returns the XXxxxxInfo array from DataSet.properties specific to a dataset (e.g., LATssta)
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the array of info related to the dataset
   */
  public String[] getInfo(String fileName) {
    // FUTURE: replace this with Shared which stores the info
    Test.ensureNotNull(fileName, "getInfo fileName=null");
    // special cases
    String name6 = get6CharName(fileName);
    String infoString = dataSetRB2.getString(name6 + "Info", null);
    if (infoString == null)
      Test.error(
          String2.ERROR
              + " in getInfo("
              + fileName
              + "):\n"
              + "No entry for "
              + name6
              + "Info in DataSet.properties file.");
    String sar[] = String2.split(infoString, '`');
    Test.ensureNotNull(sar, "sar info for fileName=" + fileName);
    Test.ensureEqual(sar.length, 20, "info length for fileName=" + fileName);
    return sar;
  }

  /**
   * This returns the categoryName for the related dataset.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the categoryName (e.g., Color, Height, Temperature, or ""; never null)
   */
  public String getCategory(String fileName) {
    try {
      char ch = getInfo(fileName)[0].charAt(0);
      int po = categoryLetters.indexOf(ch);
      return categoryNames[po];
    } catch (Exception e) {
      String2.log(
          String2.ERROR + " in FileNameUtility.getCategory: no category found for " + fileName);
      return "";
    }
  }

  /**
   * This returns the 'courtesy' information for the related dataset.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the courtesy info
   * @throws Exception of 6nameCourtesy isn't in the DataSet.properties file.
   */
  public String getCourtesy(String fileName) {
    String six = get6CharName(fileName);
    String s = dataSetRB2.getString(six + "Courtesy", null);
    Test.ensureNotNull(s, six + "Courtesy not found in DataSet.properties file.");
    return s;
  }

  /**
   * This returns the 'institution' information for the related dataset.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return getCreatorName() (was getCourtesy())
   */
  public String getInstitution(String fileName) {
    return getCreatorName(); // 2008-02-21 was getCourtesy(fileName);
  }

  /**
   * This returns the history of this data set (courtesy + newline + dateTime + coastwatch).
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the history info
   */
  public String getHistory(String fileName) {
    return DataHelper.addBrowserToHistory(getCourtesy(fileName));
  }

  /**
   * This returns the satellite info for the related dataset for CW HDF metadata.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the satellite info
   * @throws Exception if 6nameSatellite isn't in DataSet.properties.
   */
  public String getSatellite(String fileName) {
    String six = get6CharName(fileName);
    String s = dataSetRB2.getString(six + "Satellite", null);
    Test.ensureNotNull(s, six + "Satellite not found in DataSet.properties file.");
    return s;
  }

  /**
   * This returns the sensor info for the related dataset for CW HDF metadata.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the sensor info
   * @throws Exception if 6nameSensor isn't in DataSet.properties.
   */
  public String getSensor(String fileName) {
    String six = get6CharName(fileName);
    String s = dataSetRB2.getString(six + "Sensor", null);
    Test.ensureNotNull(s, six + "Sensor not found in DataSet.properties file.");
    return s;
  }

  /**
   * This returns "true" if the dataset is a composite, else "false", for CW HDF metadata.
   *
   * @param fileName a base (assumes full region) or custom file name.
   * @return the composite info
   * @throws Exception if trouble
   */
  public static String getComposite(String fileName) throws Exception {
    return getTimePeriodNHours(fileName) > 0 ? "true" : "false";
  }

  /**
   * This returns the boldTitle info for the related dataset.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the boldTitle info
   * @throws Exception if 6nameBoldTitle isn't in DataSet.properties.
   */
  public String getBoldTitle(String fileName) {
    String six = get6CharName(fileName);
    String s = dataSetRB2.getString(six + "BoldTitle", null);
    Test.ensureNotNull(s, six + "BoldTitle not found in DataSet.properties file.");
    return s;
  }

  /**
   * This returns the latLonFractionDigits info for the related dataset for CW HDF metadata.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the latLonFractionDigits info
   */
  public int getLatLonFractionDigits(String fileName) {
    return String2.parseInt(getInfo(fileName)[8]);
  }

  /**
   * This returns the dataFractionDigits (alt or standard) info for the related dataset for CW HDF
   * metadata.
   *
   * @param fileName the CW fileName
   * @return the dataFractionDigits info
   */
  public int getDataFractionDigits(String fileName) {
    boolean altUnits = getAlternateUnits(fileName);
    return String2.parseInt(getInfo(fileName)[altUnits ? 18 : 9]);
  }

  /**
   * This returns the units (alt or standard) info for the related dataset formatted for
   * compatibility with udunits.
   *
   * @param fileName the CW fileName
   * @return the udUnits info
   */
  public String getUdUnits(String fileName) {
    boolean altUnits = getAlternateUnits(fileName);
    return getInfo(fileName)[altUnits ? 13 : 4];
  }

  /**
   * This returns the standard udUnits for the dataset.
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the standard udUnits info
   */
  public String getStandardUdUnits(String fileName) {
    return getInfo(fileName)[4];
  }

  /**
   * This returns the units (alt or standard) info for the related dataset formatted for human
   * readability (e.g., for use in GUI or in map legends).
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the units info
   */
  public String getReadableUnits(String fileName) {
    return DataHelper.makeUdUnitsReadable(getUdUnits(fileName));
  }

  /**
   * This returns the fgdc abstract string e.g., "NOAA CoastWatch provides sea surface temperature
   * (SST) products derived..." (also used for netCDF summary) (see MetaMetadata.txt).
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the fgdc abstract string (or null if not available)
   */
  public String getAbstract(String fileName) {
    return getFGDCInfo(fileName, "<metadata><idinfo><descript><abstract>");
  }

  /**
   * This returns the keywords, e.g., "Oceans > Ocean Temperature > Sea Surface Temperature" (see
   * MetaMetadata.txt). Usually these are GCMD Science keywords: see
   * http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html .
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the keywords
   */
  public String getKeywords(String fileName) {
    String six = get6CharName(fileName);
    String s = dataSetRB2.getString(six + "Keywords", null);
    Test.ensureNotNull(s, six + "Keywords not found in DataSet.properties file.");
    return s;
  }

  /**
   * This returns the dataset id (e.g., LATsstaS1day) [pre 2019-05-07 was getUniqueID(fileName):
   * fileName minus extension, e.g., "LATsstaS1day_20030304_x-135_X-134.25_y22_Y23"] (see
   * MetaMetadata.txt).
   */
  public static String getID(String fileName) {

    fileName = File2.getNameNoExtension(fileName);
    int po = fileName.indexOf('_');
    return po > 0 ? fileName.substring(0, po) : fileName;

    /* pre 2019-05-07 this
    //remove extension (if present) at end
    //This is fancy because filename may end in e.g., _Y33.0 or _Y33.0.grd
    //   so can't just search for last "."
    int poY = fileName.lastIndexOf("_Y");
    if (poY < 0) return fileName; //hopefully, this doesn't happen
    int po = poY + 2;
    while (po < fileName.length() && "0123456789.".indexOf(fileName.charAt(po)) >= 0)
        po++;
    if (fileName.charAt(po - 1) == '.')  //. is start of extension
        return fileName.substring(0, po - 1);
    return fileName.substring(0, po);
    */
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
   * This generates the standard file name for a grid file. FILE_NAME_RELATED_CODE.
   *
   * @param internalName e.g., LGAssta
   * @param unitsChar is either 'S' or 'A'
   * @param timePeriodValue is one of the TimePeriod.OPTIONS.
   * @param isoCenteredTimeValue is the iso formatted date time, e.g., 2006-02-07 12:00:00
   * @return the base name for the file (obviously without dir, lat, lon, or extension)
   */
  public static String makeBaseName(
      String internalName, char unitsChar, String timePeriodValue, String isoCenteredTimeValue) {

    String baseName = // e.g., LATsstaS1day_20060207120000
        internalName
            + unitsChar
            + TimePeriods.getInFileName(timePeriodValue)
            + "_"
            + Calendar2.removeSpacesDashesColons(isoCenteredTimeValue);

    return baseName;
  }

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

  /**
   * This generates the standard file name for a grid timeSeries file. FILE_NAME_RELATED_CODE.
   *
   * @param internalName e.g., LGAssta
   * @param unitsChar is either 'S' or 'A'
   * @param lon is the longitude in decimal degrees east
   * @param lat is the latitude in decimal degrees north
   * @param isoBeginDateValue is the iso formatted date time, e.g., 2006-02-03
   * @param isoEndDateValue is the iso formatted date time, e.g., 2006-02-07
   * @param timePeriodValue is one of the TimePeriod.OPTIONS.
   * @return the name for the file (obviously without dir or extension)
   */
  public static String makeAveragedGridTimeSeriesName(
      String internalName,
      char unitsChar,
      double lon,
      double lat,
      String isoBeginDateValue,
      String isoEndDateValue,
      String timePeriodValue) {

    return "TS_"
        + internalName
        + unitsChar
        + TimePeriods.getInFileName(timePeriodValue)
        + (TimePeriods.getNHours(timePeriodValue) == 0 ? "" : "Averages")
        + "_x"
        + String2.genEFormat10(lon)
        + "_y"
        + String2.genEFormat10(lat)
        + "_t"
        + Calendar2.removeSpacesDashesColons(isoBeginDateValue)
        + "_T"
        + Calendar2.removeSpacesDashesColons(isoEndDateValue);
  }

  /**
   * This generates the standard file name for a station averaged timeSeries file.
   * FILE_NAME_RELATED_CODE
   *
   * @param internalName e.g., LGAssta
   * @param unitsChar is either 'S' or 'A'
   * @param west is the minimum longitude in decimal degrees east
   * @param east is the maximum longitude in decimal degrees east
   * @param south is the minimum latitude in decimal degrees north
   * @param north is the maximum latitude in decimal degrees north
   * @param minDepth is the minimum depth (meters, positive = down)
   * @param maxDepth is the maximum depth (meters, positive = down)
   * @param isoBeginDateValue is the iso formatted date time, e.g., 2006-02-03
   * @param isoEndDateValue is the iso formatted date time, e.g., 2006-02-07
   * @param timePeriodValue is one of the TimePeriod.OPTIONS.
   * @return the name for the file (obviously without dir or extension)
   */
  public static String makeAveragedPointTimeSeriesName(
      String internalName,
      char unitsChar,
      double west,
      double east,
      double south,
      double north,
      double minDepth,
      double maxDepth,
      String isoBeginDateValue,
      String isoEndDateValue,
      String timePeriodValue) {

    return internalName
        + unitsChar
        + TimePeriods.getInFileName(timePeriodValue)
        + (TimePeriods.getNHours(timePeriodValue) == 0 ? "" : "Averages")
        + makeWESNString(west, east, south, north)
        + "_z"
        + String2.genEFormat6(minDepth)
        + "_Z"
        + String2.genEFormat6(maxDepth)
        + "_t"
        + Calendar2.removeSpacesDashesColons(isoBeginDateValue)
        + "_T"
        + Calendar2.removeSpacesDashesColons(isoEndDateValue);
  }

  /**
   * This generates the standard file name for a trajectory file. FILE_NAME_RELATED_CODE
   *
   * @param internalName
   * @param individuals
   * @param dataVariables
   * @return the standard file name
   */
  public static String makeTrajectoryName(
      String internalName, String individuals[], String dataVariables[]) {

    String dv = String2.toSVString(dataVariables, "_", false);
    if (dv.length() > 50) // needs to be shortened
    dv = String2.md5Hex12(dv);
    String name = internalName + "__I" + String2.toSVString(individuals, "_", false) + "__D" + dv;
    return String2.replaceAll(name, " ", "");
  }

  /**
   * This returns the contributorName, e.g., "NOAA NESDIS ..." (see MetaMetadata.txt).
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the contributorName.
   */
  public String getContributorName(String fileName) {
    return getCourtesy(fileName);
  }

  /**
   * This returns the references, e.g., "NOAA POES satellites information:
   * http://coastwatch.noaa.gov/poes_sst_overview.html . Processing information:
   * https://www.ospo.noaa.gov/PSB/EPS/CW/coastwatch.html" (see MetaMetadata.txt).
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the reverences
   */
  public String getReferences(String fileName) {
    String six = get6CharName(fileName);
    String s = dataSetRB2.getString(six + "References", null);
    Test.ensureNotNull(s, six + "References not found in DataSet.properties file.");
    return s;
  }

  /**
   * This returns the source, e.g., "satellite observation: <satellite> <sensor>" (see
   * MetaMetadata.txt). If the data is not from a satellite (known because getSatellite will return
   * ""), this returns "observation from <sensor>"
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the source
   */
  public String getSource(String fileName) {
    return getSource(getSatellite(fileName), getSensor(fileName));
  }

  /**
   * This returns the source, e.g., "satellite observation: <satellite> <sensor>" (see
   * MetaMetadata.txt). If the data is not from a satellite (known because getSatellite will return
   * ""), this returns "observation from <sensor>"
   *
   * @param satellite the name of the satellite
   * @param sensor the name of the sensor
   * @return the source
   */
  public static String getSource(String satellite, String sensor) {
    return satellite.length() > 0
        ? "satellite observation: " + satellite + ", " + sensor
        : "observation from " + sensor;
  }

  /**
   * This returns the standardName, e.g., "sea_surface_temperature" (see MetaMetadata.txt).
   *
   * @param fileName the CW fileName or the 7 charName (since only 6 char name is extracted)
   * @return the standardName
   */
  public String getStandardName(String fileName) {
    String six = get6CharName(fileName);
    String s = dataSetRB2.getString(six + "StandardName", null);
    Test.ensureNotNull(s, six + "StandardName not found in DataSet.properties file.");
    return s;
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
