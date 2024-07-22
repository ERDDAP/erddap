/*
 * EDDTableFromAsciiServiceNOS Copyright 2010, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedReader;
import java.util.BitSet;
import java.util.HashMap;

/**
 * This is a subclass of EDDTableFromAsciiService for the NOS Ascii services.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2010-11-12
 */
public class EDDTableFromAsciiServiceNOS extends EDDTableFromAsciiService {

  // stationTable created in constructor; col numbers == -1 if not present
  protected Table stationTable;
  protected int stationIDCol,
      stationNameCol,
      stationStateCol,
      stationLonCol,
      stationLatCol,
      stationDateEstCol,
      stationShefIDCol,
      stationDeploymentCol;

  protected boolean datumIsFixedValue;

  /** The constructor. */
  public EDDTableFromAsciiServiceNOS(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      Attributes tAddGlobalAttributes,
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      String tLocalSourceUrl,
      String tBeforeData[],
      String tAfterData,
      String tNoData)
      throws Throwable {

    super(
        "EDDTableFromAsciiServiceNOS",
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tAddGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        tLocalSourceUrl,
        tBeforeData,
        tAfterData,
        tNoData);

    // datumIsFixedValue
    datumIsFixedValue =
        tAddGlobalAttributes.getString("sourceUrl").indexOf("datum=") > 0
            || // url specifies the datum
            String2.indexOf(dataVariableDestinationNames(), "datum")
                < 0; // dataset doesn't use datum

    // find a user who is authorized to access this dataset
    String user = (accessibleTo == null || accessibleTo.length == 0) ? null : accessibleTo[0];

    // make the stationTable (expected columns and no others)
    // which is a subset of the subsetVariables table (which may have extra cols like 'datum')
    stationTable = subsetVariablesDataTable(0, user); // exception if trouble
    int nCols = stationTable.nColumns();
    String keepCols[] =
        new String[] {
          "stationID",
          "stationName",
          "state",
          "dateEstablished",
          "shefID",
          "deployment",
          "longitude",
          "latitude"
        };
    for (int col = nCols - 1; col >= 0; col--) {
      String colName = stationTable.getColumnName(col);
      if (String2.indexOf(keepCols, colName) < 0) stationTable.removeColumn(col);
    }
    stationIDCol = stationTable.findColumnNumber("stationID");
    stationNameCol = stationTable.findColumnNumber("stationName");
    stationStateCol = stationTable.findColumnNumber("state");
    stationLonCol = stationTable.findColumnNumber("longitude");
    stationLatCol = stationTable.findColumnNumber("latitude");
    stationDateEstCol = stationTable.findColumnNumber("dateEstablished"); // epochSeconds
    stationShefIDCol = stationTable.findColumnNumber("shefID");
    stationDeploymentCol = stationTable.findColumnNumber("deployment");
    // String2.log(">>dateEstablished=" + stationTable.getColumn("dateEstablished"));
    stationTable.leftToRightSort(1); // stationID
    // remove duplicate rows  (e.g., perhaps caused by removing datum column)
    stationTable.removeDuplicates();
    // String2.log("\nstationTable=\n" + stationTable.getNCHeader("row") +
    //    stationTable.dataToString(5));

    // Most/all of these datasets are SOS-able
    // Gather information to serve this dataset via ERDDAP's SOS server.
    // This has an advantage over the generic gathering of SOS data:
    //  it can determine the min/max lon/lat/time of each station.
    // SOS datasets always have actual lon,lat values and stationTable time is always epochSeconds,
    //  so I can just use source station info directly (without conversion).
    // Note that times are often too wide a range because they are for all observedProperties,
    //  not just the one used by this dataset.
    // This is not tested!
    if (EDStatic.sosActive && stationDateEstCol >= 0) {
      sosOfferingPrefix = "urn:ioos:station:NOAA.NOS.CO-OPS:";
      sosOfferingType = "Station";
      // The index of the dataVariable with the sosOffering outer var (e.g. with
      // cf_role=timeseries_id).
      sosOfferingIndex = String2.indexOf(dataVariableDestinationNames(), "stationID");
      int nSosOfferings = stationTable.nColumns();
      sosMinLon = stationTable.getColumn(stationLonCol);
      sosMaxLon = sosMinLon;
      sosMinLat = stationTable.getColumn(stationLatCol);
      sosMaxLat = sosMinLat;
      sosMinTime = stationTable.getColumn(stationDateEstCol); // epochSeconds
      sosMaxTime = PrimitiveArray.factory(PAType.DOUBLE, nSosOfferings, "");
      sosOfferings = stationTable.getColumn(stationIDCol);
    }
  }

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be
   *     null.
   * @param tableWriter
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {

    // make the sourceQuery
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    getSourceQueryFromDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues); // timeStamp constraints other than regex are epochSeconds

    // go through the station table to find the stations of interest
    // and find datum= beginTime>= endTime<=
    int nStations = stationTable.nRows();
    BitSet keep = new BitSet();
    keep.set(0, nStations, true);
    String datum = null;
    double beginSeconds = Double.NaN;
    // default endTime is an hour ahead of now (for safety)
    double endSeconds = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu()) + 3600;
    int nConstraints = constraintVariables.size();
    for (int c = 0; c < nConstraints; c++) {
      String conVar = constraintVariables.get(c);
      String conOp = constraintOps.get(c);
      char conOp0 = conOp.charAt(0);
      String conVal = constraintValues.get(c);
      if (conVar.equals("datum")) {
        if (conOp.equals("=")) datum = constraintValues.get(c);
        continue;
      }
      if (conVar.equals("time")) {
        if (conOp0 == '>') beginSeconds = String2.parseDouble(conVal);
        else if (conOp0 == '<') endSeconds = String2.parseDouble(conVal);
        continue;
      }

      // is this constraint for a column that's in the stationTable?
      int nKeep =
          stationTable.tryToApplyConstraint(
              stationIDCol, // stationID is used for log messages only
              conVar,
              conOp,
              conVal,
              keep);
      if (nKeep == 0)
        throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (There are no matching stations.)");
    }

    // ensure the required constraints were specified
    if (!datumIsFixedValue && (datum == null || datum.length() == 0))
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.queryErrorAr[0]
                  + "For this dataset, all queries must include a \"datum=\" constraint.",
              EDStatic.queryErrorAr[language]
                  + "For this dataset, all queries must include a \"datum=\" constraint."));
    if (Double.isNaN(beginSeconds))
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.queryErrorAr[0] + "Missing time>= constraint.",
              EDStatic.queryErrorAr[language] + "Missing time>= constraint."));
    if (Double.isNaN(endSeconds))
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.queryErrorAr[0] + "If present, the time<= constraint must be valid.",
              EDStatic.queryErrorAr[language]
                  + "If present, the time<= constraint must be valid."));
    String beginTime =
        Calendar2.epochSecondsToIsoStringTZ(beginSeconds).substring(0, 16); // no seconds
    String endTime = Calendar2.epochSecondsToIsoStringTZ(endSeconds).substring(0, 16);
    if (beginSeconds > endSeconds)
      throw new SimpleException("time>=" + beginTime + " must be before time<=" + endTime);
    if (endSeconds - beginSeconds > 30 * Calendar2.SECONDS_PER_DAY)
      throw new SimpleException(
          "You must request a time range <= 30 days (begin="
              + beginTime
              + ", end="
              + endTime
              + ").");
    beginTime = String2.replaceAll(beginTime, "-", "");
    endTime = String2.replaceAll(endTime, "-", "");
    beginTime = String2.replaceAll(beginTime, 'T', '+'); // percent encode T -> space -> +
    endTime = String2.replaceAll(endTime, 'T', '+');

    // get the data for all of the valid stations
    int stationRow = keep.nextSetBit(0);
    boolean errorPrinted = false;
    while (stationRow >= 0) {

      String tStationID = stationTable.getStringData(stationIDCol, stationRow);

      // apply time constraint's endSeconds to stationDateEstablished
      // i.e., reject this station if the request is for a time range
      //  before the station was established
      if (Double.isFinite(endSeconds) && stationDateEstCol >= 0) {
        double secondsEstablished = stationTable.getDoubleData(stationDateEstCol, stationRow);
        // String2.log(">> stationID=" + tStationID + " secondsEstablished=" + secondsEstablished);
        if (Double.isFinite(secondsEstablished) && endSeconds < secondsEstablished) {
          if (reallyVerbose)
            String2.log(
                "skip "
                    + tStationID
                    + " reqEnd="
                    + endSeconds
                    + " < secondsEstablished="
                    + secondsEstablished);
          stationRow = keep.nextSetBit(stationRow + 1);
          continue;
        }
      }

      // format the query
      // https://opendap.co-ops.nos.noaa.gov/axis/webservices/waterlevelrawsixmin/
      // plain/response.jsp?stationId=9414290&beginDate=20101110+05:00&endDate=20101110+06:00
      // &datum=MLLW&unit=0&timeZone=0&metadata=yes&Submit=Submit
      String encodedSourceUrl =
          localSourceUrl
              + "&stationId="
              + SSR.minimalPercentEncode(tStationID)
              + "&beginDate="
              + beginTime
              + "&endDate="
              + endTime
              + (datumIsFixedValue ? "" : "&datum=" + SSR.minimalPercentEncode(datum));

      try {
        String stationName = null;
        Table table = null;
        double tLongitude = Double.NaN;
        double tLatitude = Double.NaN;
        // Open the file
        BufferedReader in = SSR.getBufferedUrlReader(encodedSourceUrl);
        try {
          String s = in.readLine();

          while (s != null) {
            // custom find() because two options
            int po = s.indexOf("StationName");
            if (po < 0) po = s.indexOf("Station Name");
            if (po >= 0) {
              po = s.indexOf(":", po + 11);
              if (po < 0) throw new SimpleException("':' not found after \"Station Name\"");
              stationName = s.substring(po + 1).trim();
              // if (reallyVerbose) String2.log("  found stationName=" + stationName);
              s = in.readLine();
              break;
            }
            s = in.readLine();
          }
          if (stationName == null) throw new SimpleException("\"Station Name\" not found");

          // read latitude
          s = find(in, s, "Latitude", "\"Latitude\" wasn't found");
          s = find(in, s, ":", "\":\" wasn't found after \"Latitude\"");
          tLatitude = String2.parseDouble(s);
          s = in.readLine();

          // read longitude
          s = find(in, s, "Longitude", "\"Longitude\" wasn't found");
          s = find(in, s, ":", "\":\" wasn't found after \"Longitude\"");
          tLongitude = String2.parseDouble(s);
          s = in.readLine();

          // read beforeData
          s = findBeforeData(in, s);

          // read the data
          table = getTable(in, s); // table PA's have different sizes!
        } finally {
          in.close();
        }

        // table.makeColumnsSameSize();
        // String2.log("\npre table=\n" + table.dataToString());

        if (datasetID.equals("nosCoopsWLTPHL")) {

          // make newTable by breaking waterLevel into rows,
          //   each with time=HH:mm, WaterLevel=-0.4, type=L
          // time has 11/24/2010
          // waterLevel = 02:24   -0.4  L     09:24    5.2  H     15:08    0.0  L     21:59    4.4
          // H
          Table newTable = makeEmptySourceTable(dataVariables, 32);
          int timeCol = table.findColumnNumber("time");
          int wlCol = table.findColumnNumber("waterLevel");
          int typeCol = table.findColumnNumber("type");
          PrimitiveArray oTimePa = table.getColumn(timeCol);
          PrimitiveArray oTypePa =
              table.getColumn(typeCol); // stored in 'type' because it's a string (WL isn't)
          PrimitiveArray nTimePa = newTable.getColumn(timeCol);
          PrimitiveArray nWLPa = newTable.getColumn(wlCol);
          PrimitiveArray nTypePa = newTable.getColumn(typeCol);
          int nRows = oTimePa.size();
          for (int row = 0; row < nRows; row++) {
            String oTime = oTimePa.getString(row);
            String oType = oTypePa.getString(row);
            StringArray parts = StringArray.wordsAndQuotedPhrases(oType);
            // part is the third value (so we know all 3 are available)
            for (int part = 2; part < parts.size(); part += 3) {
              nTimePa.addString(oTime + " " + parts.get(part - 2));
              nWLPa.addString(parts.get(part - 1));
              nTypePa.addString(parts.get(part));
            }
          }
          // newTable.makeColumnsSameSize();
          // String2.log("\nnewTable=\n" + newTable.dataToString());

          // swap newTable into place
          table = newTable;
        }

        // figure out nRows (some cols have data, some don't)
        int nRows = 0;
        int nDV = table.nColumns();
        for (int dv = 0; dv < nDV; dv++) {
          nRows = table.getColumn(dv).size();
          if (nRows > 0) break;
        }

        if (nRows > 0) {
          DoubleArray da;

          // do standard processing
          // fill the stationID column
          StringArray sa = (StringArray) table.getColumn("stationID");
          sa.clear();
          sa.addN(nRows, tStationID);

          // fill the stationName column
          sa = (StringArray) table.getColumn("stationName");
          sa.clear();
          sa.addN(nRows, stationName);

          // fill the state column
          int col = table.findColumnNumber("state");
          if (col >= 0 && stationStateCol >= 0) {
            sa = (StringArray) table.getColumn(col);
            sa.clear();
            sa.addN(nRows, stationTable.getStringData(stationStateCol, stationRow));
          }

          // fill the dateEstablished column
          col = table.findColumnNumber("dateEstablished");
          if (col >= 0 && stationDateEstCol >= 0) {
            da = (DoubleArray) table.getColumn(col);
            da.clear();
            da.addN(nRows, stationTable.getDoubleData(stationDateEstCol, stationRow));
          }

          // fill the shefID column
          col = table.findColumnNumber("shefID");
          if (col >= 0 && stationShefIDCol >= 0) {
            sa = (StringArray) table.getColumn(col);
            sa.clear();
            sa.addN(nRows, stationTable.getStringData(stationShefIDCol, stationRow));
          }

          // fill the deployment column
          col = table.findColumnNumber("deployment");
          if (col >= 0 && stationDeploymentCol >= 0) {
            sa = (StringArray) table.getColumn(col);
            sa.clear();
            sa.addN(nRows, stationTable.getStringData(stationDeploymentCol, stationRow));
          }

          // fill the longitude column
          da = (DoubleArray) table.getColumn("longitude");
          da.clear();
          da.addN(nRows, tLongitude);

          // fill the latitude column
          da = (DoubleArray) table.getColumn("latitude");
          da.clear();
          da.addN(nRows, tLatitude);

          int datumCol = String2.indexOf(dataVariableDestinationNames(), "datum");
          if (datumIsFixedValue) {
            // if it exists, remove it.  standardizeResultsTable will add it
            if (datumCol >= 0) table.removeColumn(datumCol);
          } else {
            // fill the datum column
            sa = (StringArray) table.getColumn(datumCol);
            sa.clear();
            sa.addN(nRows, datum);
          }

          // String2.log("\npost table=\n" + table.dataToString());
          standardizeResultsTable(language, requestUrl, userDapQuery, table);

          if (table.nRows() > 0) {
            tableWriter.writeSome(table);
            // it isn't a loop, so no need to call makeEmptySourceTable again
          }
        }

      } catch (Throwable t) {
        EDStatic.rethrowClientAbortException(t); // first thing in catch{}

        // basically ignore errors?!  (Or should a single error stop the request?)
        if (verbose) {
          String2.log(
              String2.ERROR
                  + " for stationID="
                  + tStationID
                  + " "
                  + (errorPrinted ? "" : encodedSourceUrl + "\n" + MustBe.throwableToString(t)));
          errorPrinted = true;
        }
      }

      stationRow = keep.nextSetBit(stationRow + 1);
      if (tableWriter.noMoreDataPlease) tableWriter.logCaughtNoMoreDataPlease(datasetID);
      break;
    }

    tableWriter.finish();
  }

  /**
   * This returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles)
   * or false if it doesn't (e.g., EDDTableFromDatabase).
   *
   * @returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles) or
   *     false if it doesn't (e.g., EDDTableFromDatabase).
   */
  @Override
  public boolean knowsActualRange() {
    return false;
  } // because this gets info from a remote service

  /* *
   * This does its best to generate a read-to-use datasets.xml entry for an
   * EDDTableFromAsciiServiceNOS.
   * <br>The XML can then be edited by hand and added to the datasets.xml file.
   * <br>This uses the first outerSequence (and if present, first innerSequence) found.
   * <br>Other sequences are skipped.
   *
   * @param tLocalSourceUrl
   * @param tReloadEveryNMinutes  must be a valid value, e.g., 1440 for once per day.
   *    Use, e.g., 1000000000, for never reload.
   * @param externalGlobalAttributes globalAttributes gleaned from external
   *    sources, e.g., a THREDDS catalog.xml file.
   *    These have priority over other sourceGlobalAttributes.
   *    Okay to use null if none.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
   *    If no trouble, then a valid dataset.xml chunk has been returned.
   */
  /*    public static String generateDatasetsXml(String tLocalSourceUrl,
      int tReloadEveryNMinutes, Attributes externalGlobalAttributes)
      //String outerSequenceName, String innerSequenceName, boolean sortColumnsByName)
      throws Throwable {

      String2.log("EDDTableFromAsciiServiceNOS.generateDatasetsXml" +
          "\n  tLocalSourceUrl=" + tLocalSourceUrl);
      String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);


      String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
      return sb.toString();
  } */

  private static String stationsFileName = "c:/programs/nos/stations.xml";

  /**
   * Bob uses this to reload the Water Level and Meteorological Capabilities document and store it
   * in stationsFileName.
   */
  public static void reloadStationsFile() throws Throwable {
    String2.log("reloadStationsFile()");

    // get WaterLevel and Meteorological station list from SOAP list at
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/activestations/response.jsp
    // from Active Water Level Stations Try Me / XML at
    // https://opendap.co-ops.nos.noaa.gov/axis/
    SSR.downloadFile(
        "https://opendap.co-ops.nos.noaa.gov/axis/webservices/activestations/response.jsp?v=2&format=xml&Submit=Submit",
        stationsFileName,
        true);
  }

  /**
   * Bob uses this to get the lookFor (e.g., "Water Level" or "" for any) stations from the
   * stationsFileName.
   *
   * @return a table with stationID, stationName, longitude, latitude
   */
  public static Table lookForStations(String lookFor) throws Exception {
    String2.log("lookForStations(" + lookFor + ")");
    StringArray stationID = new StringArray();
    StringArray stationName = new StringArray();
    DoubleArray longitude = new DoubleArray();
    DoubleArray latitude = new DoubleArray();
    StringArray state = new StringArray();
    StringArray date = new StringArray();
    StringArray shef = new StringArray();
    StringArray deployment = new StringArray();
    Table table = new Table();
    table.addColumn("stationID", stationID);
    table.addColumn("stationName", stationName);
    table.addColumn("longitude", longitude);
    table.addColumn("latitude", latitude);
    table.addColumn("state", state);
    table.addColumn("dateEstablished", date);
    table.addColumn("shefID", shef);
    table.addColumn("deployment", deployment);
    String s2[] = File2.readFromFileUtf8(stationsFileName);
    String stationsXML = s2[1];

    // was
    //        <parameter DCP="1" name="Water Level" sensorID="V1" status="1"/>
    // </station>
    // <station ID="8410140" name="Eastport">
    // <lat>21.9544</lat>
    // <long>-159.3561</long>

    // 2014-10-29 is (but without newlines) (note that ID now points to the name, and name now has
    // ID!
    // <?xml version="1.0" encoding="utf-8"?><soapenv:Envelope
    // xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope
    /// " xmlns:xsd="https://www.w3.org/2001/XMLSchema"
    // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><soapenv:
    // Body><ActiveStations
    // xmlns="https://opendap.co-ops.nos.noaa.gov/axis/webservices/activestations/wsdl"><stations>
    // <station ID="Nawiliwili" name="1611400">
    // <metadata><location><lat>21.9544</lat><long>-159.3561</long>
    // <state>HI</state></location><date_established>1954-11-24</date_established></metadata>
    // <parameter DCP="1" name="Water Level" sensorID="A1" status="1"/>
    // <parameter DCP="1" name="Water Temp" sensorID="E1" status="1"/>
    // </station>

    // 2015-02-02  XML changed:
    // <stationV2 ID="1611400" name="Nawiliwili">
    //  <metadataV2>
    //    <location>
    //      <lat>21.9544</lat>
    //      <long>-159.3561</long>
    //      <state>HI</state>
    //    </location>
    //    <date_established>1954-11-24</date_established>
    //    <shef_id>NWWH1</shef_id>
    //    <deployment_designation>NWLON</deployment_designation>
    //  </metadataV2>
    //  <parameter DCP="1" name="Water Level" sensorID="A1" status="1"/>
    //  <parameter DCP="1" name="Water Temp" sensorID="E1" status="1"/>
    // </stationV2>

    // 2014-10-29 note that ID now points to the name, and name now has ID!
    // 2015-02-02 As originally, ID now points to ID, and name points to name
    int stationEndPo = 0;
    while (true) {
      int stationIDPo = stationsXML.indexOf("<stationV2 ID=\"", stationEndPo + 12);
      if (stationIDPo < 0) break;
      stationIDPo += 15;
      int stationIDEnd = stationsXML.indexOf('\"', stationIDPo);
      int stationNamePo = stationsXML.indexOf("name=\"", stationIDEnd + 1);
      stationNamePo += 6;
      int stationNameEnd = stationsXML.indexOf('\"', stationNamePo);
      int latPo = stationsXML.indexOf("<lat>", stationNameEnd);
      int latEnd = stationsXML.indexOf("</lat>", Math.max(0, latPo));
      int lonPo = stationsXML.indexOf("<long>", stationNameEnd);
      int lonEnd = stationsXML.indexOf("</long>", Math.max(0, lonPo));
      int statePo = stationsXML.indexOf("<state>", stationNameEnd);
      int stateEnd = stationsXML.indexOf("</state>", Math.max(0, statePo));
      int datePo = stationsXML.indexOf("<date_established>", stationNameEnd);
      int dateEnd = stationsXML.indexOf("</date_established>", Math.max(0, datePo));
      int shefPo = stationsXML.indexOf("<shef_id>", stationNameEnd);
      int shefEnd = stationsXML.indexOf("</shef_id>", Math.max(0, shefPo));
      int deploymentPo = stationsXML.indexOf("<deployment_designation>", stationNameEnd);
      int deploymentEnd =
          stationsXML.indexOf("</deployment_designation>", Math.max(0, deploymentPo));
      stationEndPo = stationsXML.indexOf("</stationV2>", stationNameEnd);
      if (stationEndPo < 0) throw new RuntimeException("</stationV2> po<0!");
      int lookForPo =
          lookFor.length() == 0
              ? stationNameEnd
              : stationsXML.indexOf("name=\"" + lookFor + "\"", stationNameEnd);
      if (lookForPo < 0 || lookForPo > stationEndPo) {
        // String2.log("  reject " + stationsXML.substring(stationIDPo, stationIDEnd));
        continue;
      }

      // keep it
      stationID.add(stationsXML.substring(stationIDPo, stationIDEnd).trim());
      stationName.add(stationsXML.substring(stationNamePo, stationNameEnd).trim());
      latitude.add(
          latPo < 0 || latPo > stationEndPo
              ? Double.NaN
              : String2.parseDouble(stationsXML.substring(latPo + 5, latEnd).trim()));
      longitude.add(
          lonPo < 0 || lonPo > stationEndPo
              ? Double.NaN
              : String2.parseDouble(stationsXML.substring(lonPo + 6, lonEnd).trim()));
      state.add(
          statePo < 0 || statePo > stationEndPo
              ? ""
              : stationsXML.substring(statePo + 7, stateEnd).trim());
      date.add(
          datePo < 0 || datePo > stationEndPo
              ? ""
              : stationsXML.substring(datePo + 18, dateEnd).trim());
      shef.add(
          shefPo < 0 || shefPo > stationEndPo
              ? ""
              : stationsXML.substring(shefPo + 9, shefEnd).trim());
      deployment.add(
          deploymentPo < 0 || deploymentPo > stationEndPo
              ? ""
              : stationsXML.substring(deploymentPo + 24, deploymentEnd).trim());
    }
    // String2.log(stationID1.toString());
    String2.log("n" + lookFor + "=" + table.nRows() + "\n" + table.dataToString());
    return table;
  }

  /** Bob uses this to make the NOS COOPS Active Currents subset file. */
  public static void makeNosActiveCurrentsSubsetFile(boolean reloadCurrentsStationsFile)
      throws Exception {
    String2.log("makeNosActiveCurrentsSubsetTable");

    StringArray stationID = new StringArray();
    StringArray stationName = new StringArray();
    DoubleArray longitude = new DoubleArray();
    DoubleArray latitude = new DoubleArray();
    StringArray dateEstablished = new StringArray();
    Table table = new Table();
    table.addColumn("stationID", stationID);
    table.addColumn("stationName", stationName);
    table.addColumn("longitude", longitude);
    table.addColumn("latitude", latitude);
    table.addColumn("dateEstablished", dateEstablished);
    String tStationID = null;
    String tStationName = null;
    Double tLongitude = Double.NaN;
    Double tLatitude = Double.NaN;
    String tDateEstablished = null;

    String fileName = "c:/programs/nos/ActiveCurrentsStations.xml";
    if (reloadCurrentsStationsFile)
      SSR.downloadFile(
          "https://opendap.co-ops.nos.noaa.gov/axis/webservices/activecurrentstations/response.jsp?format=xml&Submit=Submit",
          fileName,
          true);

    String2.log(File2.readFromFileUtf8(fileName)[1].substring(0, 4000));

    // read from file
    SimpleXMLReader xmlReader =
        new SimpleXMLReader(File2.getDecompressedBufferedInputStream(fileName), "soapenv:Envelope");
    try {
      while (true) {
        xmlReader.nextTag();
        String tag = xmlReader.topTag();
        // String2.log("  tag=" + tag);
        if (tag.equals("/soapenv:Envelope")) break;

        /*
        ...
        <station ID="cb0102" name="Cape Henry LB 2CH">
          <metadata><
            project>Chesapeake Bay South PORTS</project>
            <deploymentHistory>
              <deployment long="-76.01278" deployed="2004-05-14 00:00:00.0" lat="36.95917" recovered="2005-02-08 00:00:00.0"/>
              <deployment long="-76.01278" deployed="2005-02-09 00:00:00.0" lat="36.95917" recovered="2005-10-17 23:54:00.0"/>
              <deployment long="-76.01278" deployed="2005-10-18 00:00:00.0" lat="36.95917" recovered="2005-11-07 23:54:00.0"/>
              ...
              <deployment long="-76.01302" deployed="2013-08-07 14:00:00.0" lat="36.95922" recovered="2014-01-06 13:00:00.0"/>
              <deployment long="-76.01302" deployed="2014-01-06 14:00:00.0" lat="36.95922" recovered="2014-06-24 23:00:00.0"/>
              <deployment long="-76.01302" deployed="2014-06-25 15:00:00.0" lat="36.95922"/>
            </deploymentHistory>
          </metadata>
        </station>
        <station ID="cb0301" name="Thimble Shoal LB 18">
        */
        if (tag.equals("station")) {
          tStationID = xmlReader.attributeValue("ID");
          tStationName = xmlReader.attributeValue("name");
        } else if (tag.equals("/station")) {
          String2.log(tStationID + " " + tStationName + " " + tLongitude + " " + tLatitude);
          if (tStationID != null
              && tStationName != null
              && !Double.isNaN(tLongitude)
              && !Double.isNaN(tLatitude)) stationID.add(tStationID);
          stationName.add(tStationName);
          longitude.add(tLongitude);
          latitude.add(tLatitude);
          dateEstablished.add(tDateEstablished == null ? "" : tDateEstablished);
          tDateEstablished = null;
        } else if (tag.equals("deployment")) {
          // there are usually several deployments,
          //  so get first deployed date, but last long,lat
          if (!String2.isSomething(tDateEstablished)) {
            tDateEstablished = xmlReader.attributeValue("deployed");
            if (tDateEstablished != null && tDateEstablished.length() > 10)
              tDateEstablished = tDateEstablished.substring(0, 10);
          }
          tLongitude = String2.parseDouble(xmlReader.attributeValue("long"));
          tLatitude = String2.parseDouble(xmlReader.attributeValue("lat"));
        }
      }
    } finally {
      xmlReader.close();
    }
    table.leftToRightSort(1);

    String dir = "c:/programs/_tomcat/content/erddap/subset/";
    table.saveAsJson(
        dir + "nosCoopsCA.json", -1, false); // timeColumn=-1 since already ISO String, writeUnits
    String2.log(table.dataToString());

    String2.pressEnterToContinue(
        "\n*** EDDTableFromAsciiServiceNOS.makeNosActiveCurrentsSubsetTable done.\n"
            + "nStations="
            + table.nRows());
  }

  /**
   * This makes the Meteorological station table for many nosCoops M datasets on Bob's computer.
   *
   * @throws Throwable if trouble
   */
  public static void makeNosCoopsMetSubsetFiles(boolean reloadStationsFile) throws Throwable {
    String2.log("\n****************** EDDTableFromAsciiServiceNOS.makeNosCoopsMetSubsetFiles\n");

    // reload the Capabilities document
    if (reloadStationsFile) reloadStationsFile();
    String dir = "c:/programs/_tomcat/content/erddap/subset/";
    Table table;
    int c;

    table = lookForStations("Air Temp");
    int nMAT = table.nRows();
    table.saveAsJson(
        dir + "nosCoopsMAT.json", -1, false); // timeColumn=-1 since already ISO String, writeUnits

    table = lookForStations("Air Pressure");
    int nMBP = table.nRows();
    table.saveAsJson(dir + "nosCoopsMBP.json", -1, false);

    table = lookForStations("Conductivity");
    int nMC = table.nRows();
    table.saveAsJson(dir + "nosCoopsMC.json", -1, false);

    table = lookForStations("Humidity");
    int nMRH = table.nRows();
    table.saveAsJson(dir + "nosCoopsMRH.json", -1, false);

    table = lookForStations("Water Temp");
    int nMWT = table.nRows();
    table.saveAsJson(dir + "nosCoopsMWT.json", -1, false);

    table = lookForStations("Winds");
    int nMW = table.nRows();
    table.saveAsJson(dir + "nosCoopsMW.json", -1, false);

    table = lookForStations("Visibility");
    int nMV = table.nRows();
    table.saveAsJson(dir + "nosCoopsMV.json", -1, false);

    // rainfall has no related property
    // so create from entire station file and just keep the 6 stations listed at
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/rainfall/index.jsp
    table = lookForStations("");
    int nRows =
        table.oneStepApplyConstraint(
            0, "stationID", "=~", "(8762484|9752619|9753216|9754228|9757809)");
    table.saveAsJson(dir + "nosCoopsMRF.json", -1, false);
    try {
      Test.ensureEqual(nRows, 6, "Rain Fall");
    } catch (Throwable t) {
      String2.log(table.dataToString());
      String2.pressEnterToContinue(
          MustBe.throwableToString(t)
              +
              // 2018-07-05 9759394 disappeared from web page, but another was added
              // "\n2015-02-02 2 of the 6 stations aren't in the stations file:\n" +
              // "97557809 9757112, but they are still on the web page.\n" +
              "\n2015-05-04 1 of the 6 stations isn't in the stations file:\n"
              + "9757112, but it is still on the web page.");
    }

    String2.pressEnterToContinue(
        "\n*** EDDTableFromAsciiServiceNOS.makeNosCoopsMetSubsetFiles done.\n"
            + "nMAT="
            + nMAT
            + " nMBP="
            + nMBP
            + " nMC="
            + nMC
            + " nMRH="
            + nMRH
            + " nMWT="
            + nMWT
            + " nMW="
            + nMW
            + " n=MV="
            + nMV
            + "\n"
            + " nMRF="
            + nRows);
  }

  /**
   * This makes the WaterLevel station table for many nosCoops WL datasets on Bob's computer.
   *
   * @throws Throwable if trouble
   */
  public static void makeNosCoopsWLSubsetFiles(boolean reloadStationsFile) throws Throwable {
    String2.log("\n*********** EDDTableFromAsciiServiceNOS.makeNosCoopsWLSubsetFiles\n");

    // reload the Capabilities document
    if (reloadStationsFile) reloadStationsFile();

    Table fromTable = lookForStations("Water Level");
    StringArray fromID = (StringArray) fromTable.getColumn("stationID");
    StringArray fromName = (StringArray) fromTable.getColumn("stationName");
    DoubleArray fromLon = (DoubleArray) fromTable.getColumn("longitude");
    DoubleArray fromLat = (DoubleArray) fromTable.getColumn("latitude");
    StringArray fromState = (StringArray) fromTable.getColumn("state");
    StringArray fromDate = (StringArray) fromTable.getColumn("dateEstablished");
    StringArray fromShef = (StringArray) fromTable.getColumn("shefID");
    StringArray fromDeployment = (StringArray) fromTable.getColumn("deployment");

    // was based on soap response
    // BufferedInputStream bis = SSR.getUrlBufferedInputStream(
    //
    // "https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS?service=SOS&request=GetCapabilities");
    //    //File2.getDecompressedBufferedInputStream("c:/programs/nos/stations.xml"));  //for
    // testing
    // Table table1 = EDDTableFromSOS.getStationTable(bis,
    //    "http://mmisw.org/ont/cf/parameter/water_level");
    // bis.close();
    // int nRows1 = table1.nRows();
    // StringArray stationID1 = (StringArray)table1.getColumn(0);

    // for each station, get available datums from the datum service
    Table toTable = new Table();
    StringArray toID = new StringArray();
    StringArray toName = new StringArray();
    DoubleArray toLon = new DoubleArray();
    DoubleArray toLat = new DoubleArray();
    StringArray toState = new StringArray();
    StringArray toDate = new StringArray();
    StringArray toDatum = new StringArray();
    StringArray toShef = new StringArray();
    StringArray toDeployment = new StringArray();
    // column names are dataset destinationNames
    toTable.addColumn("stationID", toID);
    toTable.addColumn("stationName", toName);
    toTable.addColumn("longitude", toLon);
    toTable.addColumn("latitude", toLat);
    toTable.addColumn("state", toState);
    toTable.addColumn("dateEstablished", toDate);
    toTable.addColumn("datum", toDatum);
    toTable.addColumn("shefID", toShef);
    toTable.addColumn("deployment", toDeployment);

    HashMap datumsHash = new HashMap();
    int nStations = fromID.size();
    int noName = 0,
        wrongName = 0,
        wrongLat = 0,
        wrongLon = 0,
        noPre = 0,
        noDatumHeader = 0,
        noDatum = 0;
    int nGoodStations = 0;
    for (int row = 0; row < nStations; row++) {
      try {
        String tStationID = fromID.get(row);
        // https://opendap.co-ops.nos.noaa.gov/axis/webservices/datums/plain/response.jsp?stationId=8454000&epoch=A&unit=0&metadata=yes&Submit=Submit
        String url =
            "https://opendap.co-ops.nos.noaa.gov/axis/webservices/datums/"
                + "plain/response.jsp?"
                + "epoch=A&unit=0&metadata=yes&Submit=Submit&stationId="
                + tStationID;
        String content[] = SSR.getUrlResponseLines(url);
        if (row == 0) String2.log(String2.toNewlineString(content));
        /* changed somewhat 2015-02-02:
        <pre>
        ...
           Station Id         :   9414290
           Station Name       :   San Francisco
           State              :   CA
           Latitude           :   37.8067
           Longitude          :   -122.465
           Unit               :   Meters
           Status             :   Accepted
           Epoch              :   Current (1983-2001)
        ...
        <strong>Datum          Value          Description</strong>
        ==========================================================================================================
        MHHW           3.602          Mean Higher-High Water
        MHW            3.416          Mean High Water
        HWI            0.48           Greenwich High Water Interval (in Hours)
        LWI            5.92           Greenwich Low  Water Interval (in Hours)
        NAVD           1.818          North American Vertical Datum

        Maximum        6.401                 Highest Water Level on Station Datum
        Max Date       1938-09-21 00:00      Date and Time Of Highest Water Level
        Minimum        -0.030                Lowest  Water Level on Station Datum
        Min Date       1959-01-05 21:36      Date and Time Of Lowest Water Level
        </pre>
        </pre>
        */
        // no datums? e.g., 8311062
        int po =
            String2.lineContaining(content, "but no Datums data is available from this station", 0);
        if (po >= 0) {
          noDatum++;
          String2.log("  No datums data for this station.");
          continue;
        }

        int snRow = String2.lineContaining(content, "Station Name       :", 0);
        if (snRow < 0) {
          noName++;
          String2.log("  No name for this station.");
          continue;
        }
        String tName = content[snRow].substring(26).trim();
        if (!tName.equals(fromName.get(row))) {
          wrongName++;
          String2.log("  wrong name");
          continue;
        }

        po = String2.lineContaining(content, "Latitude", snRow);
        double tLat = String2.parseDouble(po < 0 ? "" : content[po].substring(26));
        if (Math2.roundToInt(tLat * 1000) != Math2.roundToInt(fromLat.get(row) * 1000)) {
          wrongLat++;
          String2.log("  wrong lat " + tLat + " " + fromLat.get(row));
          continue;
        }

        po = String2.lineContaining(content, "Longitude", snRow);
        double tLon = String2.parseDouble(po < 0 ? "" : content[po].substring(26));
        if (Math2.roundToInt(tLon * 1000) != Math2.roundToInt(fromLon.get(row) * 1000)) {
          wrongLon++;
          String2.log("  wrong lat " + tLon + " " + fromLon.get(row));
          continue;
        }

        po = String2.lineContaining(content, "Datum", snRow);
        if (po < 0) {
          noDatumHeader++;
          String2.log("Datum header not found");
          continue;
        }
        po += 2;
        boolean foundSome = false;
        while (po < content.length && content[po].length() > 1) {
          content[po] += "                                        ";
          if (content[po].startsWith("<pre>")) content[po] = content[po].substring(5);
          String tDatum = content[po].substring(0, 10).trim();
          String tValue = content[po].substring(15, 20).trim();
          if (tDatum.startsWith("<") || tDatum.startsWith("Please")) {
          } else if (tValue.length() > 0) {
            String description = content[po].substring(30).trim();
            datumsHash.put(tDatum, description);
            toID.add(fromID.get(row));
            toName.add(fromName.get(row));
            toLon.add(fromLon.get(row));
            toLat.add(fromLat.get(row));
            toState.add(fromState.get(row));
            toDate.add(fromDate.get(row));
            toShef.add(fromShef.get(row));
            toDeployment.add(fromDeployment.get(row));
            toDatum.add(tDatum);
            foundSome = true;
          }
          po++;
        }
        // 2010-11-18 email from Mohamed.Chaouchi@noaa.gov says
        // "The majority of the station will support STND but not all of them."
        if (foundSome) {
          toID.add(fromID.get(row));
          toName.add(fromName.get(row));
          toLon.add(fromLon.get(row));
          toLat.add(fromLat.get(row));
          toState.add(fromState.get(row));
          toDate.add(fromDate.get(row));
          toShef.add(fromShef.get(row));
          toDeployment.add(fromDeployment.get(row));
          toDatum.add("STND");
          nGoodStations++;
        } else {
          noDatum++;
        }

      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
      }
    }

    // sort
    toTable.leftToRightSort(5);

    // print a little of toTable
    String2.log(toTable.dataToString(30));

    // print datumDescriptions
    Table datumDesc = new Table();
    datumDesc.readMap(datumsHash, "Datum", "Description");
    String2.log(datumDesc.dataToString());

    String dir = "c:/programs/_tomcat/content/erddap/subset/";
    toTable.saveAsJson(
        dir + "nosCoopsWLR6.json", -1, false); // timeColumn=-1 since already ISO String, writeUnits
    toTable.saveAsJson(dir + "nosCoopsWLR1.json", -1, false);
    toTable.saveAsJson(dir + "nosCoopsWLV6.json", -1, false);
    toTable.saveAsJson(dir + "nosCoopsWLV60.json", -1, false);
    toTable.saveAsJson(dir + "nosCoopsWLVHL.json", -1, false);
    toTable.saveAsJson(dir + "nosCoopsWLVDM.json", -1, false);
    String2.log("nStation+Datum combos with any datum=" + toTable.nRows());

    // remove datum other than MLLW
    toTable.oneStepApplyConstraint(0, "datum", "=", "MLLW");
    toTable.saveAsJson(dir + "nosCoopsWLTPHL.json", -1, false);
    toTable.saveAsJson(dir + "nosCoopsWLTP6.json", -1, false);
    toTable.saveAsJson(dir + "nosCoopsWLTP60.json", -1, false);

    String2.pressEnterToContinue(
        "\n*** EDDTableFromAsciiServiceNOS.makeNosCoopsWLSubsetFiles Done.\n"
            + "Of nStations="
            + nStations
            + ", failures: noName="
            + noName
            + " wrongName="
            + wrongName
            + " wrongLat="
            + wrongLat
            + " wrongLon="
            + wrongLon
            + " noPre="
            + noPre
            + " noDatumHeader="
            + noDatumHeader
            + " noDatum="
            + noDatum
            + "\n"
            + "nGoodStations="
            + nGoodStations
            + "\n"
            + "nStation+Datum combos with datum=MLLW ="
            + toTable.nRows());
  }

  /**
   * This tests the methods in this class.
   *
   * @throws Throwable if trouble
   */
  public static void makeSubsetFiles(boolean reloadSF) throws Throwable {
    String2.log("\n*** EDDTableFromAsciiServiceNOS.makeSubsetFiles()\n");
    testVerboseOn();

    // update nosCoops datasets every 3 months
    // then copy [tomcat]/content/erddap/subset/nosCoops*.json files
    //  to coastwatch ERDDAP /subset
    //  and UAF       ERDDAP /subset
    // then flag all the nosCoops datasets on coastwatch (use the list of flags)
    if (reloadSF) reloadStationsFile();
    makeNosCoopsWLSubsetFiles(false); // re-download the stations file
    makeNosCoopsMetSubsetFiles(false); // re-download the stations file
    makeNosActiveCurrentsSubsetFile(
        reloadSF); // re-download the currents stations file (a different file)
  }
}
