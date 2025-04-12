package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.OutputStreamSourceSimple;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDConfig;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVLatGridAxis;
import gov.noaa.pfel.erddap.variable.EDVLonGridAxis;
import gov.noaa.pfel.erddap.variable.EDVTime;
import gov.noaa.pfel.erddap.variable.EDVTimeGridAxis;
import java.io.BufferedWriter;
import java.text.MessageFormat;

@FileTypeClass(
    fileTypeExtension = ".kml",
    fileTypeName = ".kml",
    infoUrl = "https://developers.google.com/kml/",
    versionAdded = "1.0.0")
public class KmlFiles extends ImageTypes {

  @Override
  protected boolean tableToImage(DapRequestInfo requestInfo, OutputStreamSourceSimple osss)
      throws Throwable {

    return saveAsKml(
        requestInfo.language(),
        requestInfo.loggedInAs(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.dir(),
        requestInfo.fileName(),
        osss,
        requestInfo.getEDDTable());
  }

  @Override
  protected boolean gridToImage(DapRequestInfo requestInfo, OutputStreamSourceSimple osss)
      throws Throwable {

    return saveAsKml(
        requestInfo.language(),
        requestInfo.loggedInAs(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        osss,
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelpTable_kmlAr[language];
  }

  @Override
  public String getGridHelpText(int language) {
    return EDStatic.messages.fileHelpGrid_kmlAr[language];
  }

  /**
   * This makes a .kml file. The userDapQuery must include the EDV.LON_NAME and EDV.LAT_NAME columns
   * (and preferably also EDV.ALT_NAME and EDV.TIME_NAME column) in the results variables.
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but it unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param requestUrl I think it's currently just used to add to "history" metadata.
   * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
   * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
   * @param fileName the name for the 'file' (no dir, no extension), which is used to write the
   *     suggested name for the file to the response header.
   * @param outputStreamSource
   * @return true of written ok; false if exception occurred (and written on image)
   * @throws Throwable if trouble
   */
  private boolean saveAsKml(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      String dir,
      String fileName,
      OutputStreamSource outputStreamSource,
      EDDTable eddTable)
      throws Throwable {

    // before any work is done,
    //  ensure LON_NAME and LAT_NAME are among resultsVariables
    String tErddapUrl = EDStatic.erddapUrl(loggedInAs, language);
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    eddTable.parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
        false);
    if (resultsVariables.indexOf(EDV.LON_NAME) < 0 || resultsVariables.indexOf(EDV.LAT_NAME) < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(EDStatic.messages.queryErrorLLAr[0], ".kml"),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(EDStatic.messages.queryErrorLLAr[language], ".kml")));

    // get the table with all the data
    TableWriterAllWithMetadata twawm =
        eddTable.getTwawmForDapQuery(language, loggedInAs, requestUrl, userDapQuery);
    Table table = twawm.cumulativeTable();
    twawm.releaseResources();
    twawm.close();
    table.convertToStandardMissingValues(); // so stored as NaNs

    // double check that lon and lat were found
    int lonCol = table.findColumnNumber(EDV.LON_NAME);
    int latCol = table.findColumnNumber(EDV.LAT_NAME);
    int altCol = table.findColumnNumber(EDV.ALT_NAME);
    int timeCol = table.findColumnNumber(EDV.TIME_NAME);
    EDVTime edvTime =
        eddTable.timeIndex() < 0 ? null : (EDVTime) eddTable.dataVariables()[eddTable.timeIndex()];
    if (lonCol < 0 || latCol < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(EDStatic.messages.queryErrorLLAr[0], ".kml"),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(EDStatic.messages.queryErrorLLAr[language], ".kml")));

    // remember this may be many stations one time, or one station many times, or many/many
    // sort table by lat, lon, depth, then time (if possible)
    if (altCol >= 0 && timeCol >= 0)
      table.sort(
          new int[] {lonCol, latCol, altCol, timeCol}, new boolean[] {true, true, true, true});
    else if (timeCol >= 0)
      table.sort(new int[] {lonCol, latCol, timeCol}, new boolean[] {true, true, true});
    else if (altCol >= 0)
      table.sort(new int[] {lonCol, latCol, altCol}, new boolean[] {true, true, true});
    else table.sort(new int[] {lonCol, latCol}, new boolean[] {true, true});
    // String2.log(table.toString("row", 10));

    // get lat and lon range (needed to create icon size and ensure there is data to be plotted)
    double minLon = Double.NaN, maxLon = Double.NaN, minLat = Double.NaN, maxLat = Double.NaN;
    if (lonCol >= 0) {
      double stats[] = table.getColumn(lonCol).calculateStats();
      minLon = stats[PrimitiveArray.STATS_MIN];
      maxLon = stats[PrimitiveArray.STATS_MAX];
    }
    if (latCol >= 0) {
      double stats[] = table.getColumn(latCol).calculateStats();
      minLat = stats[PrimitiveArray.STATS_MIN];
      maxLat = stats[PrimitiveArray.STATS_MAX];
    }
    if (Double.isNaN(minLon) || Double.isNaN(minLat))
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              MustBe.THERE_IS_NO_DATA + " " + EDStatic.messages.noDataNoLLAr[0],
              MustBe.THERE_IS_NO_DATA + " " + EDStatic.messages.noDataNoLLAr[language]));
    double lonRange = maxLon - minLon;
    double latRange = maxLat - minLat;
    double maxRange = Math.max(lonRange, latRange);

    // get time range and prep moreTime constraints

    String moreTime = "";
    double minTime = Double.NaN, maxTime = Double.NaN;
    if (timeCol >= 0) {
      // time is in the response
      double stats[] = table.getColumn(timeCol).calculateStats();
      minTime = stats[PrimitiveArray.STATS_MIN];
      maxTime = stats[PrimitiveArray.STATS_MAX];
      if (!Double.isNaN(minTime)) { // there are time values
        // at least a week
        double tMinTime = Math.min(minTime, maxTime - 7 * Calendar2.SECONDS_PER_DAY);
        moreTime = // >  <
            "&time%3E="
                + Calendar2.epochSecondsToLimitedIsoStringT(
                    edvTime.time_precision(), tMinTime, "NaN")
                + "&time%3C="
                + Calendar2.epochSecondsToLimitedIsoStringT(
                    edvTime.time_precision(), maxTime, "NaN");
      }
    } else {
      // look for time in constraints
      for (int c = 0; c < constraintVariables.size(); c++) {
        if (EDV.TIME_NAME.equals(constraintVariables.get(c))) {
          double tTime = String2.parseDouble(constraintValues.get(c));
          char ch1 = constraintOps.get(c).charAt(0);
          if (ch1 == '>' || ch1 == '=')
            minTime = Double.isNaN(minTime) ? tTime : Math.min(minTime, tTime);
          if (ch1 == '<' || ch1 == '=')
            maxTime = Double.isNaN(maxTime) ? tTime : Math.max(maxTime, tTime);
        }
      }
      if (Double.isFinite(minTime))
        moreTime =
            "&time%3E="
                + Calendar2.epochSecondsToLimitedIsoStringT(
                    edvTime.time_precision(), minTime - 7 * Calendar2.SECONDS_PER_DAY, "NaN");
      if (Double.isFinite(maxTime))
        moreTime =
            "&time%3C="
                + Calendar2.epochSecondsToLimitedIsoStringT(
                    edvTime.time_precision(), maxTime + 7 * Calendar2.SECONDS_PER_DAY, "NaN");
    }

    // Google Earth .kml
    // (getting the outputStream was delayed until actually needed)
    BufferedWriter writer =
        File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));

    // collect the units
    String columnUnits[] = new String[table.nColumns()];
    boolean columnIsString[] = new boolean[table.nColumns()];
    boolean columnIsTimeStamp[] = new boolean[table.nColumns()];
    String columnTimePrecision[] = new String[table.nColumns()];
    for (int col = 0; col < table.nColumns(); col++) {
      String units = table.columnAttributes(col).getString("units");
      // test isTimeStamp before prepending " "
      columnIsTimeStamp[col] = EDV.TIME_UNITS.equals(units) || EDV.TIME_UCUM_UNITS.equals(units);
      // String2.log("col=" + col + " name=" + table.getColumnName(col) + " units=" + units + "
      // isTimestamp=" + columnIsTimeStamp[col]);
      units = (units == null || units.equals(EDV.UNITLESS)) ? "" : " " + units;
      columnUnits[col] = units;
      columnIsString[col] = table.getColumn(col) instanceof StringArray;
      columnTimePrecision[col] = table.columnAttributes(col).getString(EDV.TIME_PRECISION);
    }

    // based on kmz example from http://www.coriolis.eu.org/cdc/google_earth.htm
    // see copy in bob's c:/programs/kml/SE-LATEST-MONTH-STA.kml
    // kml docs: https://developers.google.com/kml/documentation/kmlreference
    // CDATA is necessary for url's with queries
    // kml/description docs recommend \n<br />
    String courtesy =
        eddTable.institution().length() == 0
            ? ""
            : MessageFormat.format(
                EDStatic.messages.imageDataCourtesyOfAr[language], eddTable.institution());
    double iconSize =
        maxRange > 90
            ? 1.2
            : maxRange > 45
                ? 1.0
                : maxRange > 20 ? .8 : maxRange > 10 ? .6 : maxRange > 5 ? .5 : .4;
    writer.write( // KML
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
            + "<Document>\n"
            +
            // human-friendly, but descriptive, <name>
            // name is used as link title -- leads to <description>
            "  <name>"
            + XML.encodeAsXML(eddTable.title())
            + "</name>\n"
            +
            // <description> appears in balloon
            "  <description><![CDATA["
            + XML.encodeAsXML(courtesy)
            + "\n<br />"
            + String2.replaceAll(XML.encodeAsXML(eddTable.summary()), "\n", "\n<br />")
            +
            // link to download this dataset
            "\n<br />"
            + "<a href=\""
            + XML.encodeAsHTMLAttribute(
                tErddapUrl
                    + "/tabledap/"
                    + // don't use \n for the following lines
                    eddTable.datasetID()
                    + ".html?"
                    + userDapQuery)
            + // already percentEncoded; XML.encodeAsXML isn't ok
            "\">View/download more data from this dataset.</a>\n"
            + "    ]]></description>\n"
            + "  <open>1</open>\n"
            + "  <Style id=\"BUOY ON\">\n"
            + "    <IconStyle>\n"
            + "      <color>ff0099ff</color>\n"
            + // abgr   orange
            "      <scale>"
            + (3 * iconSize)
            + "</scale>\n"
            + "      <Icon>\n"
            +
            // see list in Google Earth by right click on icon : Properties : the icon icon
            // 2011-12-15 was shaded_dot (too fuzzy), now placemark_circle
            "        <href>https://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n"
            + "      </Icon>\n"
            + "    </IconStyle>\n"
            + "  </Style>\n"
            + "  <Style id=\"BUOY OUT\">\n"
            + "    <IconStyle>\n"
            + "      <color>ff0099ff</color>\n"
            + "      <scale>"
            + (2 * iconSize)
            + "</scale>\n"
            + "      <Icon>\n"
            + "        <href>https://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n"
            + "      </Icon>\n"
            + "    </IconStyle>\n"
            + "    <LabelStyle><scale>0</scale></LabelStyle>\n"
            + "  </Style>\n"
            + "  <StyleMap id=\"BUOY\">\n"
            + "    <Pair><key>normal</key><styleUrl>#BUOY OUT</styleUrl></Pair>\n"
            + "    <Pair><key>highlight</key><styleUrl>#BUOY ON</styleUrl></Pair>\n"
            + "  </StyleMap>\n");

    // just one link for each station (same lat,lon/depth):
    // LON   LAT DEPTH   TIME    ID  WTMP
    // -130.36   42.58   0.0 1.1652336E9 NDBC 46002 met  13.458333174387613
    int nRows = table.nRows(); // there must be at least 1 row
    int startRow = 0;
    double startLon = table.getNiceDoubleData(lonCol, startRow);
    double startLat = table.getNiceDoubleData(latCol, startRow);
    for (int row = 1; row <= nRows; row++) { // yes, 1...n, since looking at previous row
      // look for a change in lastLon/Lat
      if (row == nRows
          || startLon != table.getNiceDoubleData(lonCol, row)
          || startLat != table.getNiceDoubleData(latCol, row)) {

        if (!Double.isNaN(startLon) && !Double.isNaN(startLat)) {

          // make a placemark for this station
          double startLon180 = Math2.anglePM180(startLon);
          writer.write(
              "  <Placemark>\n"
                  + "    <name>"
                  + "Lat="
                  + String2.genEFormat10(startLat)
                  + ", Lon="
                  + String2.genEFormat10(startLon180)
                  + "</name>\n"
                  + "    <description><![CDATA["
                  +
                  // kml/description docs recommend \n<br />
                  XML.encodeAsXML(eddTable.title())
                  + "\n<br />"
                  + XML.encodeAsXML(courtesy));

          // if timeCol exists, find last row with valid time
          // This solves problem with dapper data (last row for each station has just NaNs)
          int displayRow = row - 1;
          if (timeCol >= 0) {
            while (displayRow - 1 >= startRow
                && Double.isNaN(
                    table.getDoubleData(timeCol, displayRow))) // if displayRow is NaN...
            displayRow--;
          }

          // display the last row of data  (better than nothing)
          for (int col = 0; col < table.nColumns(); col++) {
            double td = table.getNiceDoubleData(col, displayRow);
            String ts = table.getStringData(col, displayRow);
            // String2.log("col=" + col + " name=" + table.getColumnName(col) + " units=" +
            // columnUnits[col] + " isTimestamp=" + columnIsTimeStamp[col]);
            writer.write(
                "\n<br />"
                    + XML.encodeAsXML(
                        table.getColumnName(col)
                            + " = "
                            + (columnIsTimeStamp[col]
                                ? Calendar2.epochSecondsToLimitedIsoStringT(
                                    columnTimePrecision[col], td, "")
                                : columnIsString[col]
                                    ? ts
                                    : (Double.isNaN(td) ? "NaN" : ts) + columnUnits[col])));
          }
          writer.write(
              "\n<br /><a href=\""
                  + XML.encodeAsHTMLAttribute(
                      tErddapUrl
                          + "/tabledap/"
                          + // don't use \n for the following lines
                          eddTable.datasetID()
                          + ".htmlTable?"
                          +
                          // was SSR.minimalPercentEncode   XML.encodeAsXML isn't ok
                          // ignore userDapQuery
                          // get just this station, all variables, at least 7 days
                          moreTime
                          + // already percentEncoded
                          // some data sources like dapper don't respond to lon=startLon
                          // lat=startLat
                          "&"
                          + EDV.LON_NAME
                          + SSR.minimalPercentEncode(">")
                          + (startLon - .01)
                          + // not startLon180
                          "&"
                          + EDV.LON_NAME
                          + SSR.minimalPercentEncode("<")
                          + (startLon + .01)
                          + "&"
                          + EDV.LAT_NAME
                          + SSR.minimalPercentEncode(">")
                          + (startLat - .01)
                          + "&"
                          + EDV.LAT_NAME
                          + SSR.minimalPercentEncode("<")
                          + (startLat + .01))
                  + "\">View tabular data for this location.</a>\n"
                  + "\n<br /><a href=\""
                  + XML.encodeAsHTMLAttribute(
                      tErddapUrl
                          + "/tabledap/"
                          + // don't use \n for the following lines
                          eddTable.datasetID()
                          + ".html?"
                          + userDapQuery)
                  + // already percentEncoded.  XML.encodeAsXML isn't ok
                  "\">View/download more data from this dataset.</a>\n"
                  + "]]></description>\n"
                  + "    <styleUrl>#BUOY</styleUrl>\n"
                  + "    <Point>\n"
                  + "      <coordinates>"
                  + startLon180
                  + ","
                  + startLat
                  + "</coordinates>\n"
                  + "    </Point>\n"
                  + "  </Placemark>\n");
        }

        // reset startRow...
        startRow = row;
        if (startRow == nRows) {
          // assist with LookAt
          // it has trouble with 1 point: zoom in forever
          //  and if lon range crossing dateline
          double tMaxRange =
              Math.min(90, maxRange); // 90 is most you can comfortably see, and useful fudge
          tMaxRange = Math.max(2, tMaxRange); // now it's 2 .. 90
          // fudge for smaller range
          if (tMaxRange < 45) tMaxRange *= 1.5;
          double eyeAt =
              14.0e6 * tMaxRange / 90.0; // meters, 14e6 shows whole earth (~90 deg comfortably)
          double lookAtX = Math2.anglePM180((minLon + maxLon) / 2);
          double lookAtY = (minLat + maxLat) / 2;
          if (EDDTable.reallyVerbose)
            String2.log(
                "KML minLon="
                    + minLon
                    + " maxLon="
                    + maxLon
                    + " minLat="
                    + minLat
                    + " maxLat="
                    + maxLat
                    + "\n  maxRange="
                    + maxRange
                    + " tMaxRange="
                    + tMaxRange
                    + "\n  lookAtX="
                    + lookAtX
                    + "  lookAtY="
                    + lookAtY
                    + "  eyeAt="
                    + eyeAt);
          writer.write(
              "  <LookAt>\n"
                  + "    <longitude>"
                  + lookAtX
                  + "</longitude>\n"
                  + "    <latitude>"
                  + lookAtY
                  + "</latitude>\n"
                  + "    <range>"
                  + eyeAt
                  + "</range>\n"
                  + // meters
                  "  </LookAt>\n");
        } else {
          startLon = table.getNiceDoubleData(lonCol, startRow);
          startLat = table.getNiceDoubleData(latCol, startRow);
          minLon =
              Double.isNaN(minLon)
                  ? startLon
                  : Double.isNaN(startLon) ? minLon : Math.min(minLon, startLon);
          maxLon =
              Double.isNaN(maxLon)
                  ? startLon
                  : Double.isNaN(startLon) ? maxLon : Math.max(maxLon, startLon);
          minLat =
              Double.isNaN(minLat)
                  ? startLat
                  : Double.isNaN(startLat) ? minLat : Math.min(minLat, startLat);
          maxLat =
              Double.isNaN(maxLat)
                  ? startLat
                  : Double.isNaN(startLat) ? maxLat : Math.max(maxLat, startLat);
        }
      } // end processing change in lon, lat, or depth
    } // end row loop

    // end of kml file
    writer.write(getKmlIconScreenOverlay() + "  </Document>\n" + "</kml>\n");
    writer.flush(); // essential
    return true;
  }

  /**
   * This returns the kml code for the screenOverlay (which is the KML code which describes
   * how/where to display the googleEarthLogoFile). This is used by EDD subclasses when creating KML
   * files.
   *
   * @return the kml code for the screenOverlay.
   */
  private String getKmlIconScreenOverlay() {
    return "  <ScreenOverlay id=\"Logo\">\n"
        + // generic id
        "    <description>"
        + EDStatic.preferredErddapUrl
        + "</description>\n"
        + "    <name>Logo</name>\n"
        + // generic name
        "    <Icon>"
        + "<href>"
        + EDStatic.preferredErddapUrl
        + "/"
        + EDConfig.IMAGES_DIR
        + // has trailing /
        EDStatic.config.googleEarthLogoFile
        + "</href>"
        + "</Icon>\n"
        + "    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n"
        + "    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n"
        + "    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n"
        + // 0=original size
        "  </ScreenOverlay>\n";
  }

  /**
   * This writes grid data (not axis data) to the outputStream in Google Earth's .kml format
   * (https://developers.google.com/kml/documentation/kmlreference ). If no exception is thrown, the
   * data was successfully written. For .kml, dataVariable queries can specify multiple longitude,
   * latitude, and time values, but just one value for other dimensions.
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but it unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160].
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @return true of written ok; false if exception occurred (and written on image)
   * @throws Throwable if trouble.
   */
  private boolean saveAsKml(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid eddGrid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsKml");
    long time = System.currentTimeMillis();
    String tErddapUrl = EDStatic.erddapUrl(loggedInAs, language);

    // check that request meets .kml restrictions.
    // .transparentPng does some of these tests, but better to catch problems
    //  here than in GoogleEarth.

    // .kml not available for axis request
    // lon and lat are required; time is not required
    if (eddGrid.isAxisDapQuery(userDapQuery) || eddGrid.lonIndex() < 0 || eddGrid.latIndex() < 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "The .kml format is for latitude longitude data requests only.");

    // parse the userDapQuery
    // this also tests for error when parsing query
    StringArray tDestinationNames = new StringArray();
    IntArray tConstraints = new IntArray();
    eddGrid.parseDataDapQuery(language, userDapQuery, tDestinationNames, tConstraints, false);
    if (tDestinationNames.size() != 1)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "The .kml format can only handle one data variable.");

    // find any &constraints (simplistic approach, but sufficient for here and hard to replace with
    // Table.getDapQueryParts)
    int ampPo = -1;
    if (userDapQuery != null) {
      ampPo = userDapQuery.indexOf('&');
      if (ampPo == -1)
        ampPo =
            userDapQuery.indexOf(
                "%26"); // shouldn't be.  but allow overly zealous percent encoding.
    }
    String percentEncodedAmpQuery =
        ampPo >= 0
            ? // so constraints can be used in reference urls in kml
            XML.encodeAsXML(userDapQuery.substring(ampPo))
            : "";

    EDVTimeGridAxis timeEdv = null;
    PrimitiveArray timePa = null;
    double timeStartd = Double.NaN, timeStopd = Double.NaN;
    int nTimes = 0;
    for (int av = 0; av < eddGrid.axisVariables().length; av++) {
      if (av == eddGrid.lonIndex()) {

      } else if (av == eddGrid.latIndex()) {

      } else if (av == eddGrid.timeIndex()) {
        timeEdv = (EDVTimeGridAxis) eddGrid.axisVariables()[eddGrid.timeIndex()];
        timePa =
            timeEdv
                .sourceValues()
                .subset(
                    tConstraints.get(av * 3 + 0),
                    tConstraints.get(av * 3 + 1),
                    tConstraints.get(av * 3 + 2));
        timePa = timeEdv.toDestination(timePa);
        nTimes = timePa.size();
        timeStartd = timePa.getNiceDouble(0);
        timeStopd = timePa.getNiceDouble(nTimes - 1);
        if (nTimes > 500) // arbitrary: prevents requests that would take too long to respond to
        throw new SimpleException(
              EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                  + "For .kml requests, the time dimension's size must be less than 500.");

      } else {
        if (tConstraints.get(av * 3 + 0) != tConstraints.get(av * 3 + 2))
          throw new SimpleException(
              EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                  + "For .kml requests, the "
                  + eddGrid.axisVariables()[av].destinationName()
                  + " dimension's size must be 1.");
      }
    }

    // lat lon info
    // lon and lat axis values don't have to be evenly spaced.
    // .transparentPng uses Sgt.makeCleanMap which projects data (even, e.g., Mercator)
    // so resulting .png will use a geographic projection.

    // although the Google docs say lon must be +-180, lon > 180 is sort of ok!
    EDVLonGridAxis lonEdv = (EDVLonGridAxis) eddGrid.axisVariables()[eddGrid.lonIndex()];
    EDVLatGridAxis latEdv = (EDVLatGridAxis) eddGrid.axisVariables()[eddGrid.latIndex()];

    int totalNLon = lonEdv.sourceValues().size();
    int lonStarti = tConstraints.get(eddGrid.lonIndex() * 3 + 0);
    int lonStopi = tConstraints.get(eddGrid.lonIndex() * 3 + 2);
    double lonStartd = lonEdv.destinationValue(lonStarti).getNiceDouble(0);
    double lonStopd = lonEdv.destinationValue(lonStopi).getNiceDouble(0);
    if (lonStopd <= -180 || lonStartd >= 360)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "For .kml requests, there must be some longitude values must be between -180 and 360.");
    if (lonStartd < -180) {
      lonStarti = lonEdv.destinationToClosestIndex(-180);
      lonStartd = lonEdv.destinationValue(lonStarti).getNiceDouble(0);
    }
    if (lonStopd > Math.min(lonStartd + 360, 360)) {
      lonStopi = lonEdv.destinationToClosestIndex(Math.min(lonStartd + 360, 360));
      lonStopd = lonEdv.destinationValue(lonStopi).getNiceDouble(0);
    }
    int lonMidi = (lonStarti + lonStopi) / 2;
    double lonMidd = lonEdv.destinationValue(lonMidi).getNiceDouble(0);

    int totalNLat = latEdv.sourceValues().size();
    int latStarti = tConstraints.get(eddGrid.latIndex() * 3 + 0);
    int latStopi = tConstraints.get(eddGrid.latIndex() * 3 + 2);
    double latStartd = latEdv.destinationValue(latStarti).getNiceDouble(0);
    double latStopd = latEdv.destinationValue(latStopi).getNiceDouble(0);
    if (latStartd < -90 || latStopd > 90)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "For .kml requests, the latitude values must be between -90 and 90.");
    int latMidi = (latStarti + latStopi) / 2;
    double latMidd = latEdv.destinationValue(latMidi).getNiceDouble(0);

    if (lonStarti == lonStopi || latStarti == latStopi)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "For .kml requests, the lon and lat dimension sizes must be greater than 1.");
    // request is ok and compatible with .kml request!

    String datasetUrl = tErddapUrl + "/" + eddGrid.dapProtocol() + "/" + eddGrid.datasetID();
    String timeString = "";
    if (nTimes >= 1)
      timeString +=
          Calendar2.epochSecondsToLimitedIsoStringT(
              timeEdv.combinedAttributes().getString(EDV.TIME_PRECISION),
              Math.min(timeStartd, timeStopd),
              "");
    if (nTimes >= 2)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "For .kml requests, the time dimension size must be 1.");
    // timeString += " through " + limitedIsoStringT ... Math.max(timeStartd, timeStopd), "");
    String brTimeString = timeString.length() == 0 ? "" : "Time: " + timeString + "<br />\n";

    // calculate doMax and get drawOrder
    int drawOrder = 1;
    int doMax = 1; // max value of drawOrder for this dataset
    double tnLon = totalNLon;
    double tnLat = totalNLat;
    int txPo = Math2.roundToInt(lonStarti / tnLon); // at this level, the txPo'th x tile
    int tyPo = Math2.roundToInt(latStarti / tnLat);
    while (Math.min(tnLon, tnLat) > 512) { // 256 led to lots of artifacts and gaps at seams
      // This determines size of all tiles.
      // 512 leads to smallest tile edge being >256.
      // 256 here relates to minLodPixels 256 below (although Google example used 128 below)
      // and Google example uses tile sizes of 256x256.

      // go to next level
      tnLon /= 2;
      tnLat /= 2;
      doMax++;

      // if user requested lat lon range < this level, drawOrder is at least this level
      // !!!THIS IS TRICKY if user starts at some wierd subset (not full image).
      if (EDDGrid.reallyVerbose)
        String2.log(
            "doMax="
                + doMax
                + "; cLon="
                + (lonStopi - lonStarti + 1)
                + " <= 1.5*tnLon="
                + (1.5 * tnLon)
                + "; cLat="
                + (latStopi - latStarti + 1)
                + " <= 1.5*tnLat="
                + (1.5 * tnLat));
      if (lonStopi - lonStarti + 1 <= 1.5 * tnLon
          && // 1.5 ~rounds to nearest drawOrder
          latStopi - latStarti + 1 <= 1.5 * tnLat) {
        drawOrder++;
        txPo = Math2.roundToInt(lonStarti / tnLon); // at this level, this is the txPo'th x tile
        tyPo = Math2.roundToInt(latStarti / tnLat);
        if (EDDGrid.reallyVerbose)
          String2.log(
              "    drawOrder="
                  + drawOrder
                  + " txPo="
                  + lonStarti
                  + "/"
                  + tnLon
                  + "+"
                  + txPo
                  + " tyPo="
                  + latStarti
                  + "/"
                  + tnLat
                  + "+"
                  + tyPo);
      }
    }

    // calculate lonLatStride: 1 for doMax, 2 for doMax-1
    int lonLatStride = 1;
    for (int i = drawOrder; i < doMax; i++) lonLatStride *= 2;
    if (EDDGrid.reallyVerbose)
      String2.log(
          "    final drawOrder="
              + drawOrder
              + " txPo="
              + txPo
              + " tyPo="
              + tyPo
              + " doMax="
              + doMax
              + " lonLatStride="
              + lonLatStride);

    // Based on https://code.google.com/apis/kml/documentation/kml_21tutorial.html#superoverlays
    // Was based on quirky example (but lots of useful info):
    // http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
    // kml docs: https://developers.google.com/kml/documentation/kmlreference
    // CDATA is necessary for url's with queries
    try (BufferedWriter writer =
        File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8))) {
      writer.write( // KML
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
              + "<Document>\n"
              +
              // human-friendly, but descriptive, <name>
              // name is used as link title -- leads to <description>
              "  <name>");
      if (drawOrder == 1)
        writer.write(
            XML.encodeAsXML(eddGrid.title())
                + "</name>\n"
                +
                // <description appears in help balloon
                // <br /> is what kml/description documentation recommends
                "  <description><![CDATA["
                + brTimeString
                + MessageFormat.format(
                    EDStatic.messages.imageDataCourtesyOfAr[language],
                    XML.encodeAsXML(eddGrid.institution()))
                + "<br />\n"
                +
                // link to download data
                "<a href=\""
                + datasetUrl
                + ".html?"
                + SSR.minimalPercentEncode(tDestinationNames.get(0))
                + // XML.encodeAsXML doesn't work
                "\">Download data from this dataset.</a><br />\n"
                + "    ]]></description>\n");
      else writer.write(drawOrder + "_" + txPo + "_" + tyPo + "</name>\n");

      // GoogleEarth says it just takes lon +/-180, but it does ok (not perfect) with 180.. 360.
      // If minLon>=180, it is easy to adjust the lon value references in the kml,
      //  but leave the userDapQuery for the .transparentPng unchanged.
      // lonAdjust is ESSENTIAL for proper work with lon > 180.
      // GoogleEarth doesn't select correct drawOrder region if lon > 180.
      double lonAdjust = Math.min(lonStartd, lonStopd) >= 180 ? -360 : 0;
      String llBox =
          "      <west>"
              + (Math.min(lonStartd, lonStopd) + lonAdjust)
              + "</west>\n"
              + "      <east>"
              + (Math.max(lonStartd, lonStopd) + lonAdjust)
              + "</east>\n"
              + "      <south>"
              + Math.min(latStartd, latStopd)
              + "</south>\n"
              + "      <north>"
              + Math.max(latStartd, latStopd)
              + "</north>\n";

      // is nTimes <= 1?
      StringBuilder tQuery;
      if (nTimes <= 1) {
        // the Region
        writer.write(
            // min Level Of Detail: minimum size (initially while zooming in) at which this region
            // is made visible
            // see https://code.google.com/apis/kml/documentation/kmlreference.html#lod
            "  <Region>\n"
                + "    <Lod><minLodPixels>"
                + (drawOrder == 1 ? 2 : 256)
                + "</minLodPixels>"
                +
                // "<maxLodPixels>" + (drawOrder == 1? -1 : 1024) + "</maxLodPixels>" + //doesn't
                // work as expected
                "</Lod>\n"
                + "    <LatLonAltBox>\n"
                + llBox
                + "    </LatLonAltBox>\n"
                + "  </Region>\n");

        if (drawOrder < doMax) {
          // NetworkLinks to subregions (quadrant)
          tQuery =
              new StringBuilder(tDestinationNames.get(0)); // limited chars, no need to URLEncode
          for (int nl = 0; nl < 4; nl++) {
            double tLonStartd = nl < 2 ? lonStartd : lonMidd;
            double tLonStopd = nl < 2 ? lonMidd : lonStopd;
            double tLatStartd = Math2.odd(nl) ? latMidd : latStartd;
            double tLatStopd = Math2.odd(nl) ? latStopd : latMidd;
            double tLonAdjust =
                Math.min(tLonStartd, tLonStopd) >= 180
                    ? -360
                    : 0; // see comments for lonAdjust above

            tQuery =
                new StringBuilder(tDestinationNames.get(0)); // limited chars, no need to URLEncode
            for (int av = 0; av < eddGrid.axisVariables().length; av++) {
              if (av == eddGrid.lonIndex())
                tQuery.append("[(" + tLonStartd + "):(" + tLonStopd + ")]");
              else if (av == eddGrid.latIndex())
                tQuery.append("[(" + tLatStartd + "):(" + tLatStopd + ")]");
              else if (av == eddGrid.timeIndex()) tQuery.append("[(" + timeString + ")]");
              else tQuery.append("[" + tConstraints.get(av * 3 + 0) + "]");
            }

            writer.write(
                "  <NetworkLink>\n"
                    + "    <name>"
                    + drawOrder
                    + "_"
                    + txPo
                    + "_"
                    + tyPo
                    + "_"
                    + nl
                    + "</name>\n"
                    + "    <Region>\n"
                    + "      <Lod><minLodPixels>256</minLodPixels>"
                    +
                    // "<maxLodPixels>1024</maxLodPixels>" + //doesn't work as expected.
                    "</Lod>\n"
                    + "      <LatLonAltBox>\n"
                    + "        <west>"
                    + (Math.min(tLonStartd, tLonStopd) + tLonAdjust)
                    + "</west>\n"
                    + "        <east>"
                    + (Math.max(tLonStartd, tLonStopd) + tLonAdjust)
                    + "</east>\n"
                    + "        <south>"
                    + Math.min(tLatStartd, tLatStopd)
                    + "</south>\n"
                    + "        <north>"
                    + Math.max(tLatStartd, tLatStopd)
                    + "</north>\n"
                    + "      </LatLonAltBox>\n"
                    + "    </Region>\n"
                    + "    <Link>\n"
                    + "      <href>"
                    + datasetUrl
                    + ".kml?"
                    + SSR.minimalPercentEncode(tQuery.toString())
                    + // XML.encodeAsXML doesn't work
                    percentEncodedAmpQuery
                    + "</href>\n"
                    + "      <viewRefreshMode>onRegion</viewRefreshMode>\n"
                    + "    </Link>\n"
                    + "  </NetworkLink>\n");
          }
        }

        // the GroundOverlay which shows the current image
        tQuery = new StringBuilder(tDestinationNames.get(0)); // limited chars, no need to URLEncode
        for (int av = 0; av < eddGrid.axisVariables().length; av++) {
          if (av == eddGrid.lonIndex())
            tQuery.append("[(" + lonStartd + "):" + lonLatStride + ":(" + lonStopd + ")]");
          else if (av == eddGrid.latIndex())
            tQuery.append("[(" + latStartd + "):" + lonLatStride + ":(" + latStopd + ")]");
          else if (av == eddGrid.timeIndex()) tQuery.append("[(" + timeString + ")]");
          else tQuery.append("[" + tConstraints.get(av * 3 + 0) + "]");
        }
        writer.write(
            "  <GroundOverlay>\n"
                +
                // "    <name>" + XML.encodeAsXML(title()) +
                //    (timeString.length() > 0? ", " + timeString : "") +
                //    "</name>\n" +
                "    <drawOrder>"
                + drawOrder
                + "</drawOrder>\n"
                + "    <Icon>\n"
                + "      <href>"
                + datasetUrl
                + ".transparentPng?"
                + SSR.minimalPercentEncode(tQuery.toString())
                + // XML.encodeAsXML doesn't work
                percentEncodedAmpQuery
                + "</href>\n"
                + "    </Icon>\n"
                + "    <LatLonBox>\n"
                + llBox
                + "    </LatLonBox>\n"
                +
                // "    <visibility>1</visibility>\n" +
                "  </GroundOverlay>\n");
      } /*else {
            //nTimes >= 2, so make a timeline in Google Earth
            //Problem: I don't know what time range each image represents.
            //  Because I don't know what the timePeriod is for the dataset (e.g., 8day).
            //  And I don't know if the images overlap (e.g., 8day composites, every day)
            //  And if the stride>1, it is further unknown.
            //Solution (crummy): assume an image represents -1/2 time to previous image until 1/2 time till next image

            //get all the .dotConstraints
            String parts[] = Table.getDapQueryParts(userDapQuery); //decoded.  always at least 1 part (may be "")
            StringBuilder dotConstraintsSB = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith(".")) {
                    if (dotConstraintsSB.size() > 0)
                        dotConstraintsSB.append("&");
                    dotConstraintsSB.append(parts[i]);
                }
            }
            String dotConstraints = dotConstraintsSB.toString();

            IntArray tConstraints = (IntArray)gridDataAccessor.constraints().clone();
            int startTimeIndex = tConstraints.get(timeIndex * 3);
            int timeStride     = tConstraints.get(timeIndex * 3 + 1);
            int stopTimeIndex  = tConstraints.get(timeIndex * 3 + 2);
            double preTime = Double.NaN;
            double nextTime = allTimeDestPa.getDouble(startTimeIndex);
            double currentTime = nextTime - (allTimeDestPa.getDouble(startTimeIndex + timeStride) - nextTime);
            for (int tIndex = startTimeIndex; tIndex <= stopTimeIndex; tIndex += timeStride) {
                preTime = currentTime;
                currentTime = nextTime;
                nextTime = tIndex + timeStride > stopTimeIndex?
                    currentTime + (currentTime - preTime) :
                    allTimeDestPa.getDouble(tIndex + timeStride);
                //String2.log("  tIndex=" + tIndex + " preT=" + preTime + " curT=" + currentTime + " nextT=" + nextTime);
                //just change the time constraints; leave all others unchanged
                tConstraints.set(timeIndex * 3, tIndex);
                tConstraints.set(timeIndex * 3 + 1, 1);
                tConstraints.set(timeIndex * 3 + 2, tIndex);
                String tDapQuery = buildDapQuery(tDestinationNames, tConstraints) + dotConstraints;
                writer.write(
                    //the kml link to the data
                    "  <GroundOverlay>\n" +
                    "    <name>" + Calendar2.epochSecondsToIsoStringTZ(currentTime) + "</name>\n" +
                    "    <Icon>\n" +
                    "      <href>" +
                        datasetUrl + ".transparentPng?" + I changed this: was minimalPercentEncode()... tDapQuery + //XML.encodeAsXML isn't ok
                        "</href>\n" +
                    "    </Icon>\n" +
                    "    <LatLonBox>\n" +
                    "      <west>" + west + "</west>\n" +
                    "      <east>" + east + "</east>\n" +
                    "      <south>" + south + "</south>\n" +
                    "      <north>" + north + "</north>\n" +
                    "    </LatLonBox>\n" +
                    "    <TimeSpan>\n" +
                    "      <begin>" + Calendar2.epochSecondsToIsoStringTZ((preTime + currentTime)  / 2.0) + "</begin>\n" +
                    "      <end>"   + Calendar2.epochSecondsToIsoStringTZ((currentTime + nextTime) / 2.0) + "</end>\n" +
                    "    </TimeSpan>\n" +
                    "    <visibility>1</visibility>\n" +
                    "  </GroundOverlay>\n");
            }
        }*/
      if (drawOrder == 1) writer.write(getKmlIconScreenOverlay());
      writer.write(
          """
                  </Document>
                  </kml>
                  """);
      writer.flush(); // essential
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log("  EDDGrid.saveAsKml done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
    return true;
  }
}
