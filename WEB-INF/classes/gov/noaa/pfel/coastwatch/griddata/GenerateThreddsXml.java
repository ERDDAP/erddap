/*
 * GenerateThreddsXml Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class is designed to be a stand-alone program to generate the Thredds catalog.xml files for
 * all two-letter satellite datasets, using info in the gov/noaa/pfel/coastwatch/DataSet.properties
 * file.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-10-03
 */
public class GenerateThreddsXml {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /** The string in partialMainCatalog which will be replaced by all the dataset tags. */
  public static final String INSERT_DATASETS_HERE = "[Insert datasets here.]";

  /**
   * The desired (not absolute) maximum length for the shortTitles (which are made from boldTitles).
   * Currently, this is not really relevant. Shortening rules are applied regardless of shortTitle
   * length.
   */
  public static int MAX_TITLE_LENGTH = 60;

  /**
   * GenerateThreddsXml generates many of the files needed to configure THREDDS. It generates two
   * directories (Satellite and Hfradar) in xmlMainDir which which have the dataset catalog.xml
   * files for all two-letter satellite and HF Radar data sets, using info in the
   * gov/noaa/pfel/coastwatch/DataSet.properties file. It also generates the main catalog.xml in
   * xmlMainDir by inserting dataset tags into the incompleteMainCatalog. This uses info in
   * gov/noaa/pfel/coastwatch/DataSet.properties file.
   *
   * <p>This is specific to our setup here at ERD:
   *
   * <ul>
   *   <li>The data file names must use the "Dave" naming conventions.
   *   <li>The data files must be .nc files.
   *   <li>Sorting the file names leads to the first having the earliest data and the last having
   *       the latest data.
   *   <li>The data directory structure must be twoName/fourName/timePeriod, e.g., AT/ssta/1day.
   *       Some of this could be gotten around by making twoNameRegex and fourNameRegex parameters.
   *   <li>The xml directory structure must be [something]twoName/fourName, e.g., aggregsatAT/ssta.
   *   <li>Datasets with 'C' as the first letter of the twoName are assumed to be HF Radar datasets
   *       and their catalog.xml files are stored in the Hfradar/aggreghfradarXX directories. All
   *       other datasets are assumed to be Satellite datasets and their catalog.xml files are
   *       stored in the Satellite/aggregsatXX directories.
   *   <li>There must be twoNameFourName entries in DataSet.properties.
   *   <li>Sorting of timePeriod directories is very specific to our names.
   *   <li>The shortening of the dataset names which appear in the main catalog (so they don't wrap
   *       to 2 lines) is specific to my naming convention.
   * </ul>
   *
   * <p>Very terse catalog.xml documentation is at
   * https://www.unidata.ucar.edu/software/thredds/current/tds/catalog/InvCatalogSpec.html
   *
   * <p>The finished files are in, e.g., on otter
   * /opt/tomcat1/content/thredds/Satellite/aggregsatAG/ssta/catalog.xml
   *
   * <p>Bob has some of Jerome/Yao's handmade files e.g., c:/temp/oceanwatch new tomcat/
   * ...catalogHandMade70122.xml
   *
   * <p>Bob has some sample files from Motherload
   * (https://motherlode.ucar.edu/thredds/catalog.html): e.g.,
   * c:/temp/otterTomcat/MotherlodeDatasetCatalog.xml
   *
   * <p>It requires 4 parameters:
   *
   * <ul>
   *   <li><tt>dataMainDir</tt> the base data directory, e.g., /u00/ .
   *   <li><tt>dataSubDir</tt> the subdirectory of dataMainDir, e.g., <tt>satellite/</tt>, which has
   *       subdirectories like AT/ssta/1day with .nc data files. Only the dataSubDir (not the
   *       dataMainDir) is used for the catalog.xml file's ID and urlPath attributes of the
   *       'dataset' tag.
   *   <li><tt>incompleteMainCatalog</tt> is the full name of the incomplete main catalog.xml file
   *       which has \"[Insert datasets here.]\" where the satellite and HF Radar dataset tags will
   *       be inserted. For example,
   *       <tt>/usr/local/tomcat/content/thredds/GeneratedXml/incompleteMainCatalog.xml</tt>
   *   <li><tt>xmlMainDir</tt> the directory which will be created (if necessary) to hold the
   *       results: e.g., <tt>/usr/local/tomcat/content/thredds/GeneratedXml</tt> . It will receive
   *       a new main catalog.xml file and Satellite and Hfradar subdirectories with the new dataset
   *       catalog.xml files. Datasets with 'C' as the first letter of the twoName are assumed to be
   *       HF Radar datasets and their catalog.xml files are stored in the
   *       Hfradar/aggreghfradar'twoName' subdirectory of xmlMainDir. All other datasets are assumed
   *       to be Satellite datasets and their catalog.xml files are stored in the
   *       Satellite/aggregsat'twoName' subdirectory of xmlMainDir.
   *
   * @return a StringArray with the full names of all the created dataset .xml files.
   * @throws Exception if trouble.
   */
  public static StringArray generateThreddsXml(
      String dataMainDir, String dataSubDir, String incompleteMainCatalog, String xmlMainDir)
      throws Exception {

    if (verbose)
      String2.log(
          "GenerateThreddsXml"
              + "\n  dataMainDir="
              + dataMainDir
              + "\n  dataSubDir="
              + dataSubDir
              + "\n  incompleteMainCatalog="
              + incompleteMainCatalog
              + "\n  xmlMainDir="
              + xmlMainDir);
    String errorInMethod = String2.ERROR + " in GenerateThreddsXml:\n  ";
    long time = System.currentTimeMillis();
    if (!dataMainDir.endsWith("/")) dataMainDir += "/";
    if (!dataSubDir.endsWith("/")) dataSubDir += "/";
    if (!xmlMainDir.endsWith("/")) xmlMainDir += "/";

    // read incompleteMainCatalog and ensure it has INSERT_DATASETS_HERE
    String ts[] = File2.readFromFile(incompleteMainCatalog, null, 1);
    Test.ensureTrue(ts[0].length() == 0, errorInMethod + "incompleteMainCatalog file not found.");
    String incompleteMainCatalogText = ts[1];
    int insertDatasetsHerePo = incompleteMainCatalogText.indexOf(INSERT_DATASETS_HERE);
    Test.ensureNotEqual(
        insertDatasetsHerePo,
        -1,
        errorInMethod + "\"" + INSERT_DATASETS_HERE + "\" not found in incompleteMainCatalog.");

    // get the DataSet.properties file
    ResourceBundle2 dataSetRB2 = new ResourceBundle2("gov.noaa.pfel.coastwatch.DataSet");

    // categories
    String categoryLetters = dataSetRB2.getString("categoryLetters", null);
    String categoryNames[] = String2.split(dataSetRB2.getString("categoryNames", null), '`');
    StringArray categoryXmlFiles[] = new StringArray[categoryLetters.length()];
    StringArray categoryTwoFours[] = new StringArray[categoryLetters.length()];
    StringArray categoryShortTitles[] = new StringArray[categoryLetters.length()];
    StringArray categoryCourtesies[] = new StringArray[categoryLetters.length()];
    for (int i = 0; i < categoryLetters.length(); i++) {
      categoryXmlFiles[i] = new StringArray();
      categoryTwoFours[i] = new StringArray();
      categoryShortTitles[i] = new StringArray();
      categoryCourtesies[i] = new StringArray();
    }

    // generate the twoNames
    String twoName = null;
    File tDir = new File(dataMainDir + dataSubDir);
    String[] twoNames = tDir.list();
    Arrays.sort(twoNames);

    // go through the twoNames creating the xml files
    StringArray allXmlFiles = new StringArray();
    StringArray allShortTitles = new StringArray();
    StringArray longShortTitles = new StringArray();
    StringArray warnings = new StringArray();
    String skipTwoName[] = {"G1", "GR", "MW", "VN"}; // these are in ERDDAP, but not CWBrowsers

    for (int twoNameI = 0; twoNameI < twoNames.length; twoNameI++) {
      twoName = twoNames[twoNameI];
      if (verbose) String2.log("twoName=" + twoName);
      if (twoName.length() != 2) {
        String warning =
            "WARNING! Skipping directory '"
                + twoName
                + "'. It is not 2 uppercase letters or digits.";
        String2.log(warning);
        warnings.add(warning);
        continue;
      }
      char ch1 = twoName.charAt(0);
      char ch2 = twoName.charAt(1);
      if (String2.indexOf(skipTwoName, twoName) >= 0) {
        String warning = "WARNING! Skipping directory '" + twoName + "'. It is on the skip list.";
        String2.log(warning);
        warnings.add(warning);
        continue;
      } else if (String2.isDigitLetter(ch1)
          && ch1 == Character.toUpperCase(ch1)
          && String2.isDigitLetter(ch2)
          && ch2 == Character.toUpperCase(ch2)) {
        // ok
      } else {
        String warning =
            "WARNING! Skipping directory '"
                + twoName
                + "'. It is not 2 uppercase letters or digits.";
        String2.log(warning);
        warnings.add(warning);
        continue;
      }

      // get a list of all subdirectories of twoName,   e.g., asta, ssta
      File twoNameDir = new File(dataMainDir + dataSubDir + twoName);
      if (!twoNameDir.isDirectory()) {
        String warning = "WARNING! Skipping '" + twoName + "'. It isn't a directory.";
        String2.log(warning);
        warnings.add(warning);
        continue;
      }
      String[] fourNames = twoNameDir.list();
      Arrays.sort(fourNames);
      for (int fourNameI = 0; fourNameI < fourNames.length; fourNameI++) {

        // check that this is a directory
        String fourName = fourNames[fourNameI];
        if (verbose) String2.log("  twoName/fourName=" + twoName + "/" + fourName);
        File fourNameDir = new File(twoNameDir, fourName);
        if (!fourNameDir.isDirectory()) {
          String warning =
              "WARNING! Skipping '" + twoName + "/" + fourName + "'. It isn't a directory.";
          String2.log(warning);
          warnings.add(warning);
          continue;
        }

        // get the information from DataSet.properties
        // It is best to get latest information from DataSet.properties.
        // Information in data files may not be latest, e.g., Pathfinder files aren't made often.
        // !!!checking for whether these are null is below, when info really needed
        String boldTitle = dataSetRB2.getString(twoName + fourName + "BoldTitle", null);
        String fgdc = dataSetRB2.getString(twoName + fourName + "FGDC", null);
        String info = dataSetRB2.getString(twoName + fourName + "Info", null);
        String standardName = dataSetRB2.getString(twoName + fourName + "StandardName", null);
        String courtesy = dataSetRB2.getString(twoName + fourName + "Courtesy", null);
        String units = null;
        String summary = null;
        int categoryNumber = -1;
        String category = null;
        if (info != null) {
          String infoArray[] = String2.split(info, '`');
          units = infoArray[4];

          char catLetter = infoArray[0].charAt(0);
          categoryNumber = categoryLetters.indexOf(catLetter);
          category = categoryNames[categoryNumber];
        }
        if (fgdc != null) {
          String fgdcArray[] = String2.splitNoTrim(fgdc, '\n');
          String summaryStart = "<metadata><idinfo><descript><abstract>";
          for (int i = 0; i < fgdcArray.length; i++) {
            if (fgdcArray[i].startsWith(summaryStart)) {
              summary = fgdcArray[i].substring(summaryStart.length());
              break;
            }
          }
        }
        String shortTitle = shortenBoldTitle(boldTitle);
        if (shortTitle != null && shortTitle.length() > MAX_TITLE_LENGTH) {
          // still too long?
          // String2.log("    shortTitle too long (" +
          //    shortTitle.length() + "):\n      " + shortTitle);
          longShortTitles.add(shortTitle);
        }

        if (verbose) {
          // it is good if maxShow=MAX_TITLE_LENGTH, but more important that info fit without
          // wrapping
          int maxShow = Math.min(60, MAX_TITLE_LENGTH);
          String2.log(
              "    boldTitle ="
                  + boldTitle
                  + "\n    shortTitle="
                  + shortTitle
                  + "\n    summary="
                  + summary
                  + "\n    units="
                  + units
                  + "\n    standardName="
                  + standardName
                  + "\n    category="
                  + category);
        }

        // start the catalog.xml for this fourName (aka variable, ssta)
        StringBuilder xml = new StringBuilder();
        xml.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n"
                + "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" \n"
                + "         xmlns:xlink=\"https://www.w3.org/1999/xlink\" \n"
                + "         xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" \n"
                + "         xsi:schemaLocation=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0 http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.xsd\" \n"
                +
                // Yao AGssta has GLOBEC for name(!); others have Satellite Data Server; or CA Data
                // Server
                // documentation isn't clear.   2006/10/11 I'll try "Oceanwatch THREDDS Data Server"
                // for everything
                "         name=\"Oceanwatch THREDDS Data Server\" \n"
                +
                // docs say 'version' is deprecated, but recent example file shows it as 1.0.1.
                "         version=\"1.0.1\" >\n"
                + "\n"
                + "  <service name=\"all\" serviceType=\"Compound\" base=\"\">\n"
                + "    <service name=\"ncdods\" serviceType=\"OPENDAP\" base=\"/thredds/dodsC/\" />\n"
                + "    <service name=\"wcs\" serviceType=\"WCS\" base=\"/thredds/wcs/\" \n"
                + "      suffix=\"?request=GetCapabilities&amp;version=1.0.0&amp;service=WCS\" />\n"
                +
                // "    <!-- <service name=\"HTTPServer\" serviceType=\"HTTPServer\"
                // base=\"/thredds/fileServer/\" />\n" +
                // "    <service name=\"rdbmDods\" serviceType=\"OPENDAP\"
                // base=\"https://oceanwatch.pfeg.noaa.gov/dods/\" /> -->\n" +
                "  </service>\n"
                + "\n"
                + "  <dataset name=\""
                + shortTitle
                + "\">\n");

        // get timePeriods array
        int nTimePeriods = 0;
        String[] timePeriods = fourNameDir.list();

        // timePeriods need to be specially sorted: by length of time
        if (true) { // keep temp variables local
          StringArray timePeriodsSA = new StringArray(timePeriods);
          // String2.log("original timePeriodsSA: " + timePeriodsSA);

          // are there hday and mday timeperiods?
          int hdayIndex = timePeriodsSA.indexOf("hday", 0);
          if (hdayIndex >= 0) timePeriodsSA.remove(hdayIndex);
          int mdayIndex = timePeriodsSA.indexOf("mday", 0);
          if (mdayIndex >= 0) timePeriodsSA.remove(mdayIndex);

          // remove non-directories or non-'n'day directories (eg., 'badfiles')
          for (int i = timePeriodsSA.size() - 1;
              i >= 0;
              i--) { // work backwards to avoid trouble with removing an item
            File timePeriodDir = new File(fourNameDir, timePeriodsSA.get(i));
            if (timePeriodDir.isDirectory()) {
              // ensure it is an 'n'day directory
              int po = 0;
              String tp = timePeriodsSA.get(i);
              while (String2.isDigit(tp.charAt(po))) po++;
              if (po == 0) { // e.g., "badfiles"
                String warning =
                    "WARNING! '"
                        + twoName
                        + "/"
                        + fourName
                        + "/"
                        + timePeriodsSA.get(i)
                        + "' is not an hday, mday, or 'n'day directory.";
                String2.log(warning);
                warnings.add(warning);
                timePeriodsSA.remove(i);
              }
            } else {
              String warning =
                  "WARNING! '"
                      + twoName
                      + "/"
                      + fourName
                      + "/"
                      + timePeriodsSA.get(i)
                      + "' is not a directory.";
              String2.log(warning);
              warnings.add(warning);
              timePeriodsSA.remove(i);
            }
          }

          // only 'n'day entries remain; make parallel int array of n
          IntArray timePeriodsIA = new IntArray();
          for (int i = 0; i < timePeriodsSA.size(); i++) {
            int po = 0;
            String tp = timePeriodsSA.get(i);
            while (String2.isDigit(tp.charAt(po))) po++;
            timePeriodsIA.add(String2.parseInt(tp.substring(0, po)));
          }

          // sort based on 'n'days
          ArrayList<PrimitiveArray> al = new ArrayList();
          al.add(timePeriodsSA);
          al.add(timePeriodsIA);
          PrimitiveArray.sort(al, new int[] {1}, new boolean[] {true});

          // add hday and mday back in
          if (hdayIndex >= 0) timePeriodsSA.atInsert(0, "hday");
          if (mdayIndex >= 0) timePeriodsSA.atInsert(timePeriodsSA.size(), "mday");
          // String2.log("final sorted timePeriodsSA: " + timePeriodsSA);
          timePeriods = timePeriodsSA.toArray();
        }

        // for each time period
        for (int timePeriodI = 0; timePeriodI < timePeriods.length; timePeriodI++) {

          // check that this is a directory
          String timePeriodInFileName = timePeriods[timePeriodI]; // e.g., hday or 1day
          if (verbose)
            String2.log(
                "    timePeriod directory="
                    + twoName
                    + "/"
                    + fourName
                    + "/"
                    + timePeriodInFileName);
          File timePeriodDir = new File(fourNameDir, timePeriodInFileName);
          if (!timePeriodDir.isDirectory()) {
            String warning =
                "WARNING! '"
                    + twoName
                    + "/"
                    + fourName
                    + "/"
                    + timePeriodInFileName
                    + "' is not a directory.";
            String2.log(warning);
            warnings.add(warning);
            continue;
          }
          String timePeriodDirName = timePeriodDir.getCanonicalPath() + "/";

          // make the more verbose timePeriodThredds, e.g., Single Scan or 1 day
          String timePeriodThredds;
          if (timePeriodInFileName.equals("hday")) {
            timePeriodThredds = "Single Scan";
          } else if (timePeriodInFileName.equals("mday")) {
            timePeriodThredds = "Monthly";
          } else {
            int firstLetterPo = 0;
            while (firstLetterPo < timePeriodInFileName.length()
                && String2.isDigit(timePeriodInFileName.charAt(firstLetterPo))) firstLetterPo++;
            if (firstLetterPo == 0 || firstLetterPo == timePeriodInFileName.length()) {
              String warning =
                  "WARNING! Time period not <number><units>. Skipping directory="
                      + twoName
                      + "/"
                      + fourName
                      + "/"
                      + timePeriodInFileName;
              String2.log(warning);
              warnings.add(warning);
              continue;
            } else {
              timePeriodThredds =
                  timePeriodInFileName.substring(0, firstLetterPo)
                      + "-"
                      + timePeriodInFileName.substring(firstLetterPo);
            }
          }

          // gather dataset info
          // get all the dave-style .nc file names, e.g., AT2006001_2006001_ssta.nc
          String files[] = RegexFilenameFilter.list(timePeriodDirName, ".+\\.nc");
          if (files.length == 0) {
            if (verbose) String2.log("      No nc files for timePeriod=" + timePeriodInFileName);
            continue;
          }
          if (verbose) String2.log("      nFiles=" + files.length);

          // ok; there is data; I want to make an xml file; Is the
          // necessary DataSet.properties info available?
          Test.ensureNotNull(
              boldTitle,
              errorInMethod
                  + twoName
                  + fourName
                  + "BoldTitle not found in DataSet.properties file.");
          Test.ensureNotNull(
              courtesy,
              errorInMethod
                  + twoName
                  + fourName
                  + "Courtesy not found in DataSet.properties file.");
          Test.ensureNotNull(
              summary,
              errorInMethod
                  + twoName
                  + fourName
                  + "FGDC (hence 'summary') not found in DataSet.properties file.");
          Test.ensureNotNull(
              units,
              errorInMethod
                  + twoName
                  + fourName
                  + "Info (hence 'units') not found in DataSet.properties file.");
          Test.ensureNotNull(
              standardName,
              errorInMethod
                  + twoName
                  + fourName
                  + "StandardName not found in DataSet.properties file.");

          // pick the first and last files and generate cwNames
          String daveName0 = files[0];
          String daveNameN = files[files.length - 1];
          String cwName0 = FileNameUtility.convertDaveNameToCWBrowserName(daveName0);
          String cwNameN = FileNameUtility.convertDaveNameToCWBrowserName(daveNameN);
          String twoFour = twoName + "/" + fourName;
          String twoFourTime = twoFour + "/" + timePeriodInFileName;
          // if (verbose) String2.log("      daveNameN=" + daveNameN +
          //    "\n      cwNameN=" + cwNameN);

          // open the daveNameN file  (without changing lon values)
          // Important: use the latest file available: it has newest metadata.
          Grid grid = new Grid();
          grid.readNetCDF(timePeriodDirName + daveNameN, null);
          int nLon = grid.lon.length;
          double minLon = grid.lon[0];
          double maxLon = grid.lon[nLon - 1];
          double lonResolution = (maxLon - minLon) / (nLon - 1);
          int nLat = grid.lat.length;
          double minLat = grid.lat[0];
          double maxLat = grid.lat[nLat - 1];
          double latResolution = (maxLat - minLat) / (nLat - 1);
          grid = null; // so it can be garbage collected
          if (verbose)
            String2.log(
                "      minLon="
                    + String2.genEFormat10(minLon)
                    + " maxLon="
                    + String2.genEFormat10(maxLon)
                    + " lonRes="
                    + String2.genEFormat10(lonResolution)
                    + "\n      minLat="
                    + String2.genEFormat10(minLat)
                    + " maxLat="
                    + String2.genEFormat10(maxLat)
                    + " latRes="
                    + String2.genEFormat10(latResolution));

          // calculate time coverage
          String startIsoTime =
              Calendar2.formatAsISODateTimeSpace( // throws exception if trouble
                      FileNameUtility.getCenteredCalendar(cwName0))
                  + "Z";
          String endIsoTime =
              Calendar2.formatAsISODateTimeSpace( // throws exception if trouble
                      FileNameUtility.getCenteredCalendar(cwNameN))
                  + "Z";
          // endIsoTime is within last 40 days?
          if (Calendar2.isoStringToEpochSeconds(Calendar2.getCurrentISODateStringZulu())
                  - // throws exception if trouble
                  Calendar2.isoStringToEpochSeconds(endIsoTime)
              < // throws exception if trouble
              40 * Calendar2.SECONDS_PER_DAY) endIsoTime = "present";
          if (verbose) String2.log("      isoTime start=" + startIsoTime + " end=" + endIsoTime);

          // append dataset for this time period to xml
          nTimePeriods++;
          int recheckEvery = boldTitle.indexOf("Science Quality") >= 0 ? 720 : 60;
          xml.append(
              "\n"
                  + "    <dataset name=\""
                  + timePeriodThredds
                  + "\" "
                  + "ID=\""
                  + dataSubDir
                  + twoFourTime
                  + "\" "
                  + "urlPath=\""
                  + dataSubDir
                  + twoFourTime
                  + "\">\n"
                  + "      <serviceName>all</serviceName>\n"
                  +
                  // 2006/10/11 I added netcdf tag:
                  // documentation: https://oceanwatch.pfeg.noaa.gov/thredds/docs/NcML.htm
                  "      <netcdf xmlns=\"https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\"> \n"
                  + "        <aggregation dimName=\"time\" type=\"joinExisting\" recheckEvery=\""
                  + recheckEvery
                  + " min\"> \n"
                  + "          <variableAgg name=\""
                  + twoName
                  + fourName
                  + "\" /> \n"
                  +
                  // Eeek! Is dateFormatMark correct?  it catches start date and treats composite
                  // end date as HHmmss
                  // Is it even relevant? (I think it is for joinNew, not joinExisting)
                  // see https://www.unidata.ucar.edu/software/netcdf/ncml/v2.2/Aggregation.html
                  // 10/11/06 let's try not having it
                  "          <scan "
                  +
                  //             "dateFormatMark=\"" + twoName + "#yyyyDDD_HHmmss\" " +
                  "location=\""
                  + dataMainDir
                  + dataSubDir
                  + twoFourTime
                  + "/\" suffix=\".nc\" /> \n"
                  + "        </aggregation>\n"
                  + "      </netcdf>\n"
                  + "\n"
                  + "      <metadata inherited=\"true\">\n"
                  + "\n"
                  + "        <authority>gov.noaa.pfel.coastwatch</authority>\n"
                  + "        <dataType>Grid</dataType>\n"
                  + "        <dataFormat>NetCDF</dataFormat>\n"
                  + "\n"
                  + "        <documentation type=\"Summary\">\n"
                  + "          "
                  + summary
                  + "\n"
                  + "        </documentation>\n"
                  + "        <documentation type=\"Rights\">\n"
                  + "          The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\n"
                  + "        </documentation>\n"
                  + "        <documentation xlink:href=\""
                  + "https://coastwatch.pfeg.noaa.gov/infog/"
                  + twoName
                  + "_"
                  + fourName
                  + "_las.html\" \n"
                  + // Yao QNux10 had no info
                  "          xlink:title=\"Dataset Summary\" /> \n"
                  + "        <documentation xlink:href=\"https://oceanwatch.pfeg.noaa.gov\" \n"
                  + "          xlink:title=\"Oceanwatch Live Access Server\" /> \n"
                  +
                  // add link to CWBrowsers?
                  "\n"
                  +
                  // catalog.xml spec indicates creator "indicates who created the dataset" (my
                  // xxxCourtesy)
                  // and publisher "indicates who is responsible for serving the dataset"
                  // for format, see "Source Type" paragraph
                  // contact url and email are required, but may be ""
                  "        <creator>\n"
                  + "          <name>"
                  + courtesy
                  + "</name>\n"
                  + "          <contact url=\"\" email=\"\" />\n"
                  + "        </creator>\n"
                  + "        <publisher>\n"
                  + "          <name>"
                  + DataHelper.CW_CREATOR_NAME
                  + "</name>\n"
                  + "          <contact url=\""
                  + DataHelper.CW_CREATOR_URL
                  + "\" email=\""
                  + DataHelper.CW_CREATOR_EMAIL
                  + "\" />\n"
                  + "        </publisher>\n"
                  +
                  // originally Jerome had something like:
                  // "        <creator>\n" +
                  // "          <name vocabulary=\"DIF\">NOAA NESDIS CoastWatch West Coast Regional
                  // Node</name>\n" +
                  // "          <contact url=\"https://coastwatch.pfeg.noaa.gov\"
                  // email=\"erd.data@noaa.gov\" />\n" +
                  // "        </creator>\n" +
                  "\n"
                  + "        <geospatialCoverage>\n"
                  + "          <northsouth>\n"
                  + "            <start>"
                  + minLat
                  + "</start>\n"
                  + // e.g., 22
                  "            <size>"
                  + (maxLat - minLat)
                  + "</size>\n"
                  + // e.g., 29
                  "            <resolution>"
                  + latResolution
                  + "</resolution>\n"
                  + // Yao didn't have
                  "            <units>degrees_north</units>\n"
                  + "          </northsouth>\n"
                  + "          <eastwest>\n"
                  + "            <start>"
                  + minLon
                  + "</start>\n"
                  + "            <size>"
                  + (maxLon - minLon)
                  + "</size>\n"
                  + "            <resolution>"
                  + lonResolution
                  + "</resolution>\n"
                  + // Yao didn't have
                  "            <units>degrees_east</units>\n"
                  + "          </eastwest>\n"
                  + "        </geospatialCoverage>\n"
                  + "\n"
                  + "        <timeCoverage>\n"
                  + "          <start>"
                  + startIsoTime
                  + "</start>\n"
                  + "          <end>"
                  + endIsoTime
                  + "</end>\n"
                  + // or "present"
                  "        </timeCoverage>\n"
                  + "\n"
                  + "        <variables vocabulary=\"CF\">\n"
                  + "          <variable name=\"time\" vocabulary_name=\"time\" units=\""
                  + Calendar2.SECONDS_SINCE_1970
                  + "\">time</variable>\n"
                  + "          <variable name=\"altitude\" vocabulary_name=\"altitude\" units=\"m\">altitude</variable>\n"
                  + "          <variable name=\"lat\" vocabulary_name=\"latitude\" units=\"degrees_north\">lat</variable>\n"
                  + // Yao had degrees North
                  "          <variable name=\"lon\" vocabulary_name=\"longitude\" units=\"degrees_east\">lon</variable>\n"
                  + "          <variable name=\""
                  + twoName
                  + fourName
                  + "\" "
                  +
                  // vocabulary name is required, but CF standard name list (and hence
                  // DataSet.properties) often doesn't have an appropriate standardName!
                  // Yao had non-standard name, e.g., Wind Stress.
                  // Examples in
                  // https://www.unidata.ucar.edu/software/thredds/current/tds/catalog/InvCatalogSpec.html
                  //  are like Yao's -- not strict CF, but closer -- just humanized versions.
                  // Tests with thredds show that value = "" is fine with Thredds.
                  "vocabulary_name=\""
                  + standardName
                  + "\" "
                  + "units=\""
                  + units
                  + "\">"
                  + boldTitle
                  + "</variable>\n"
                  + "        </variables>\n"
                  + "\n"
                  + "      </metadata>\n"
                  + "\n"
                  + "    </dataset>\n");
        } // end timePeriod loop

        // ensure the twoName/FourName directory exists
        String xmlSubDir =
            twoName.charAt(0) == 'C' ? "Hfradar/aggreghfradar" : "Satellite/aggregsat";
        String xmlDirName = xmlSubDir + twoName + "/" + fourName;
        File xmlDir = new File(xmlMainDir + xmlDirName);
        if (!xmlDir.isDirectory()) {
          Test.ensureTrue(
              xmlDir.mkdirs(), errorInMethod + "Unable to create " + xmlMainDir + xmlDirName);
        }
        String xmlFileName = xmlDirName + "/catalog.xml";

        // generate the catalog.xml
        if (nTimePeriods == 0) {
          File2.delete(xmlMainDir + xmlFileName); // delete any existing file
          if (verbose)
            String2.log(
                "    No time period directories with data, so no catalog.xml file for "
                    + twoName
                    + fourName);
        } else {
          xml.append("\n" + "  </dataset>\n" + "\n" + "</catalog>\n");

          // write xml to file
          if (verbose) String2.log("    * Writing catalog for " + twoName + fourName);
          String error = File2.writeToFileUtf8(xmlMainDir + xmlFileName, xml.toString());
          if (error.length() > 0) Test.error(errorInMethod + error);

          // accumulate the allXxx information.
          allXmlFiles.add(xmlFileName);
          allShortTitles.add(boldTitle);

          // accumulate category information
          categoryXmlFiles[categoryNumber].add(xmlFileName);
          categoryTwoFours[categoryNumber].add(twoName + fourName);
          categoryShortTitles[categoryNumber].add(shortTitle);
          categoryCourtesies[categoryNumber].add(courtesy);
        }
      }
    }

    // generate the catalog.xml file
    // with entries for each dataset like:
    StringBuilder catalog = new StringBuilder();
    catalog.append(incompleteMainCatalogText.substring(0, insertDatasetsHerePo));
    catalog.append("    <!-- BEGIN INFO GENERATED BY GenerateThreddsXml. -->\n" + "\n");

    // satellite datasets
    catalog.append("    <dataset name=\"Satellite Datasets\">\n");
    int cat;
    for (cat = 0; cat < categoryLetters.length() - 1; cat++) {
      if (categoryShortTitles[cat].size() > 0) {

        // add the dataset tags for the datasets in the category
        ArrayList<PrimitiveArray> table = new ArrayList();
        table.add(categoryShortTitles[cat]);
        table.add(categoryTwoFours[cat]);
        table.add(categoryCourtesies[cat]);
        table.add(categoryXmlFiles[cat]);
        PrimitiveArray.sort(
            table, new int[] {0}, new boolean[] {true}); // sort col 0 (shortTitles), ascending
        catalog.append("      <dataset name=\"" + categoryNames[cat] + "\">\n");
        for (int i = 0; i < categoryShortTitles[cat].size(); i++) {
          // create the catalogRef, e.g.,
          // <catalogRef xlink:href="Satellite/aggregsatAT/ssta/catalog.xml" xlink:title="SST, NOAA
          // POES AVHRR, LAC, 0.0125 degrees, West Coast of US, Day and Night" name=""/>
          catalog.append(
              "        <catalogRef xlink:href=\""
                  + categoryXmlFiles[cat].get(i)
                  + "\"\n"
                  + "          xlink:title=\""
                  + categoryShortTitles[cat].get(i)
                  + "\"\n"
                  + "          name=\"\" />\n");
        }
        catalog.append("      </dataset>\n");
      }
    }
    catalog.append("    </dataset>\n" + "\n");

    // HF Radar datasets (last category)
    cat = categoryLetters.length() - 1;
    if (categoryShortTitles[cat].size() > 0) {
      catalog.append("    <dataset name=\"HF Radio-derived Currents Datasets\">\n");

      // add the dataset tags for the datasets in the category
      ArrayList<PrimitiveArray> table = new ArrayList();
      table.add(categoryShortTitles[cat]);
      table.add(categoryTwoFours[cat]);
      table.add(categoryCourtesies[cat]);
      table.add(categoryXmlFiles[cat]);
      PrimitiveArray.sort(
          table, new int[] {0}, new boolean[] {true}); // sort col 0 (shortTitles), ascending
      String oldTwoLetter = "";
      for (int i = 0; i < categoryShortTitles[cat].size(); i++) {
        String shortTitle = categoryShortTitles[cat].get(i);

        // new two letter?
        String newTwoLetter = categoryTwoFours[cat].get(i).substring(0, 2);
        if (!newTwoLetter.equals(oldTwoLetter)) {
          // close out old two Letter?
          if (!oldTwoLetter.equals("")) // not for the first HFR dataset
          catalog.append("      </dataset>\n");

          // open new two letter dataset
          // e.g., shortTitle = Currents, HFR, SF Bay, 25 hr, Merid., EXPERIMENTAL
          int regionStart = shortTitle.indexOf(','); // first comma
          regionStart = shortTitle.indexOf(',', regionStart + 1) + 2; // second comma
          int regionEnd = shortTitle.indexOf(',', regionStart + 1); // third comma
          String region = shortTitle.substring(regionStart, regionEnd);
          catalog.append(
              "      <dataset name=\""
                  + region
                  + " Data, courtesy of "
                  + categoryCourtesies[cat].get(i)
                  + "\">\n");
        }
        oldTwoLetter = newTwoLetter;

        // create the catalogRef, e.g.,
        // <catalogRef xlink:href="Satellite/aggregsatAT/ssta/catalog.xml" xlink:title="SST, NOAA
        // POES AVHRR, LAC, 0.0125 degrees, West Coast of US, Day and Night" name=""/>
        catalog.append(
            "        <catalogRef xlink:href=\""
                + categoryXmlFiles[cat].get(i)
                + "\"\n"
                + "          xlink:title=\""
                + shortTitle
                + "\"\n"
                + "          name=\"\" />\n");
      }
      catalog.append("      </dataset>\n" + "    </dataset>\n" + "\n");
    }

    catalog.append("    <!-- END INFO GENERATED BY GenerateThreddsXml. -->\n");
    catalog.append(
        incompleteMainCatalogText.substring(insertDatasetsHerePo + INSERT_DATASETS_HERE.length()));
    String error = File2.writeToFileUtf8(xmlMainDir + "catalog.xml", catalog.toString());
    Test.ensureTrue(error.length() == 0, errorInMethod + error);
    String2.log("***begin catalog.xml");
    String2.log(catalog.toString());
    String2.log("***end catalog.xml");
    if (verbose) String2.log("\nSee " + xmlMainDir + "catalog.xml");

    // done
    if (verbose)
      String2.log(
          "\nshortTitles that are longer than recommended:\n" + longShortTitles.toNewlineString());
    if (verbose) String2.log("\nList of all warnings:\n" + warnings.toNewlineString());
    for (int i = 0; i < allXmlFiles.size(); i++)
      allXmlFiles.set(i, xmlMainDir + allXmlFiles.get(i));
    if (verbose)
      String2.log(
          "\ngenerateThreddsXml finished successfully in "
              + Calendar2.elapsedTimeString(System.currentTimeMillis() - time)
              + ".  nFilesCreated="
              + allXmlFiles.size());
    return allXmlFiles;
  }

  /**
   * This shortens a boldTitle.
   *
   * @param boldTitle
   * @return a shortened boldTitle
   */
  public static String shortenBoldTitle(String boldTitle) {

    // If shortTitle too long, try to shorten it.
    // This is very specific to our names.
    // All were "if (shortTitle.length() > MAX_TITLE_LENGTH)",
    //   but better to be consistent, so shorten everything.
    String shortTitle = boldTitle;
    if (shortTitle == null) return null;
    int degPo = shortTitle.indexOf("degrees");
    if (degPo > 0) {
      int commaPo = shortTitle.substring(0, degPo).lastIndexOf(",");
      if (commaPo > 0) {
        shortTitle = shortTitle.substring(0, commaPo) + shortTitle.substring(degPo + 7);
      }
    }
    shortTitle = String2.replaceAll(shortTitle, ", EXPERIMENTAL PRODUCT", ", EXPERIMENTAL");
    shortTitle = String2.replaceAll(shortTitle, " SeaWinds", ""); // from QuikSCAT SeaWinds
    shortTitle = String2.replaceAll(shortTitle, " NOAA", ""); // from, NOAA POES AVHRR
    shortTitle =
        String2.replaceAll(shortTitle, " Coef.", ""); // from Diffuse Attenuation Coef. K490
    shortTitle = String2.replaceAll(shortTitle, " based on", ""); // from GhostNet
    shortTitle = String2.replaceAll(shortTitle, "Meridional", "Merid.");
    shortTitle = String2.replaceAll(shortTitle, "HF Radio-derived", "HFR");

    return shortTitle;
  }

  /**
   * This class generates the xml files for all two-letter satellite data sets, using info in the
   * gov/noaa/pfel/coastwatch/DataSet.properties file. It also generates catalog.xml in xmlMainDir
   * by inserting dataset tags into the incompleteMainCatalog. Run the program with no parameters
   * for more information or see the JavaDocs for the generateThreddsXml method.
   *
   * @param args must have the 4 Strings which will be used as the 4 paramaters for
   *     generateThreddsXml.
   */
  public static void main(String args[]) throws Exception {
    verbose = true;
    if (args == null || args.length < 4) {
      Test.error(
          "GenerateThreddsXml generates many of the files needed to configure THREDDS.\n"
              + "It generates two directories (Satellite and Hfradar) in xmlMainDir which\n"
              + "  which have the dataset catalog.xml files for all two-letter satellite\n"
              + "  and HF Radar data sets, using info in the\n"
              + "  gov/noaa/pfel/coastwatch/DataSet.properties file.\n"
              + "It also generates the main catalog.xml file in xmlMainDir.\n"
              + "It requires 4 parameters:\n"
              + " 'dataMainDir' the base data directory, e.g., /u00/ .\n"
              + " 'dataSubDir' the subdirectory of dataMainDir, \n"
              + "     e.g., satellite/, which has subdirectories\n"
              + "     like AT/ssta/1day with .nc data files.\n"
              + "     Only the dataSubDir (not the dataMainDir) is used for the \n"
              + "     catalog.xml file's ID and urlPath attributes of the 'dataset' tag.\n"
              + " 'incompleteMainCatalog' is the full name of the incomplete main catalog.xml\n"
              + "     file which has \"[Insert datasets here.]\" where the satellite\n"
              + "     and HF Radar dataset tags will be inserted. For example, \n"
              + "     /usr/local/tomcat/content/thredds/GeneratedXml/incompleteMainCatalog.xml\n"
              + " 'xmlMainDir' the directory which will be created (if necessary) to hold \n"
              + "     the results: e.g., /usr/local/tomcat/content/thredds/GeneratedXml .\n"
              + "     It will receive a new main catalog.xml file and Satellite and Hfradar\n"
              + "     subdirectories with the new dataset catalog.xml files. \n"
              + "     Datasets with 'C' as the first letter of the twoName\n"
              + "     are assumed to be HF Radar datasets and their catalog.xml files are \n"
              + "     stored in the Hfradar/aggreghfradar'twoName' subdirectory of xmlMainDir.\n"
              + "     All other datasets \n"
              + "     are assumed to be Satellite datasets and their catalog.xml files are \n"
              + "     stored in the Satellite/aggregsat'twoName' subdirectory of xmlMainDir.");
    }
    generateThreddsXml(args[0], args[1], args[2], args[3]);
  }
}
