package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterAll;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.HashSet;

@FileTypeClass(
    fileTypeExtension = ".txt",
    fileTypeName = ".odvTxt",
    infoUrl = "https://odv.awi.de/en/documentation/",
    versionAdded = "1.24.0",
    contentType = "text/plain")
public class OdvFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) throws Throwable {
    // ensure there is longitude, latitude, time data in the request (else it is useless in
    // ODV)
    StringArray resultsVariables = new StringArray();
    requestInfo
        .getEDDTable()
        .parseUserDapQuery(
            requestInfo.language(),
            requestInfo.userDapQuery(),
            resultsVariables,
            new StringArray(),
            new StringArray(),
            new StringArray(),
            false);
    if (resultsVariables.indexOf(EDV.LON_NAME) < 0
        || resultsVariables.indexOf(EDV.LAT_NAME) < 0
        || resultsVariables.indexOf(EDV.TIME_NAME) < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              requestInfo.language(),
              EDStatic.messages.get(Message.QUERY_ERROR, 0)
                  + EDStatic.messages.get(Message.ERROR_ODV_LLT_TABLE, 0),
              EDStatic.messages.get(Message.QUERY_ERROR, requestInfo.language())
                  + EDStatic.messages.get(Message.ERROR_ODV_LLT_TABLE, requestInfo.language())));
    return new TableWriterAllWithMetadata(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.dir(),
        requestInfo.fileName()); // used after getDataForDapQuery below...
  }

  @Override
  public void writeTableToFileFormat(DapRequestInfo requestInfo, TableWriter tableWriter)
      throws Throwable {
    if (tableWriter instanceof TableWriterAllWithMetadata) {
      saveAsODV(
          requestInfo.language(),
          requestInfo.outputStream(),
          (TableWriterAllWithMetadata) tableWriter,
          requestInfo.edd().datasetID(),
          requestInfo.edd().publicSourceUrl(requestInfo.language()),
          requestInfo.edd().infoUrl(requestInfo.language()));
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsODV(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_TABLE_ODV_TXT, language);
  }

  @Override
  public String getGridHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_GRID_ODV_TXT, language);
  }

  /**
   * Save the TableWriterAllWithMetadata data as an ODV .tsv file. This writes a few attributes.
   * <br>
   * See the User's Guide section 16.3 etc. https://odv.awi.de/en/documentation/ (or Bob's
   * c:/programs/odv/odv4Guide.pdf). <br>
   * The data must have longitude, latitude, and time columns. <br>
   * Longitude can be any values (e.g., -180 or 360).
   *
   * <p>ODV user who is willing to review sample files: shaun.bell at noaa.gov .
   *
   * @param language the index of the selected language
   * @param outputStreamSource
   * @param twawm all the results data, with missingValues stored as destinationMissingValues or
   *     destinationFillValues (they are converted to NaNs)
   * @param tDatasetID
   * @param tPublicSourceUrl
   * @param tInfoUrl
   * @throws Throwable
   */
  private void saveAsODV(
      int language,
      OutputStreamSource outputStreamSource,
      TableWriterAll twawm,
      String tDatasetID,
      String tPublicSourceUrl,
      String tInfoUrl)
      throws Throwable {

    if (EDDTable.reallyVerbose) String2.log("EDDTable.saveAsODV");
    long time = System.currentTimeMillis();

    // make sure there is data
    long tnRows = twawm.nRows();
    if (tnRows == 0)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (at start of saveAsODV)");
    // the other params are all required by EDD, so it's a programming error if they are missing
    if (!String2.isSomething(tDatasetID))
      throw new SimpleException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "saveAsODV error: datasetID wasn't specified.");
    if (!String2.isSomething(tPublicSourceUrl))
      throw new SimpleException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "saveAsODV error: publicSourceUrl wasn't specified.");
    if (!String2.isSomething(tInfoUrl))
      throw new SimpleException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "saveAsODV error: infoUrl wasn't specified.");

    // make sure there isn't too much data before getting outputStream
    Table table = twawm.cumulativeTable(); // it checks memory usage
    // String2.log(">> odv after twawm:\n" + table.dataToString());
    twawm.releaseResources();
    int nRows = table.nRows();
    Attributes globalAtts = table.globalAttributes();
    // convert numeric missing values to NaN
    table.convertToStandardMissingValues();

    // ensure there is longitude, latitude, time data in the request (else it is useless in ODV)
    if (table.findColumnNumber(EDV.LON_NAME) < 0
        || table.findColumnNumber(EDV.LAT_NAME) < 0
        || table.findColumnNumber(EDV.TIME_NAME) < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.get(Message.QUERY_ERROR, 0)
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.QUERY_ERROR_LLT, 0), ".odvTxt"),
              EDStatic.messages.get(Message.QUERY_ERROR, language)
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.QUERY_ERROR_LLT, language), ".odvTxt")));

    // Move columns into preferred order, see table 3-1, 3-2, 3-3, 3-4
    // This would be very complicated if you worked forwards, because some vars are in a couple of
    // categories.
    // Easiest way: work backwards, moving vars to col#0.

    // move the "primary variable" (the first DATAVAR) into place
    // 2010-07-07 email from Stephan Heckendorff says altitude (or similar) if present MUST be
    // primaryVar
    int tCol = table.findColumnNumber(EDV.ALT_NAME);
    if (tCol < 0) tCol = table.findColumnNumberIgnoreCase("depth");
    if (tCol < 0) tCol = table.findColumnNumberIgnoreCase("pressure");
    if (tCol < 0) tCol = table.findColumnNumberIgnoreCase("sigma");
    if (tCol > 0) {
      table.moveColumn(tCol, 0); // move it to col#0
    } else {
      // if it isn't altitude/depth, then it is time ("time_ISO8601")
      tCol = table.findColumnNumber(EDV.TIME_NAME);
      table.moveColumn(tCol, 0); // move it to col#0
      table.setColumnName(0, "time_ISO8601");
    }
    String primaryVar = table.getColumnName(0);

    // move METAVAR columns into preferred order (backwards)
    // Remember all these metavariables
    //  The set will have a few old names, e.g., latitude, time.
    //  That's okay. Move them again and change names later.
    HashSet<String> metavariables = new HashSet<>();
    // start with outer table colums
    for (int i = 2; i >= 0; i--) { // work backwards
      String att =
          "cdm_" + (i == 0 ? "timeseries" : i == 1 ? "trajectory" : "profile") + "_variables";
      String value = globalAtts.getString(att);
      if (!String2.isSomething(value)) continue;
      StringArray colNames = StringArray.fromCSVNoBlanks(value);
      for (int i2 = colNames.size() - 1; i2 >= 0; i2--) { // work backwards
        tCol = table.findColumnNumber(colNames.get(i2));
        if (tCol > 0) {
          table.moveColumn(tCol, 0); // move it to col#0
          metavariables.add(table.getColumnName(0));
        }
      }
    }

    // ERDDAP datasets rarely have Bot. Depth [m] column, and hard to identify,
    //  and not required, so skip it

    // now move LLT (and give them the preferred names)
    table.moveColumn(table.findColumnNumber(EDV.LAT_NAME), 0);
    table.setColumnName(0, "Latitude [degrees_north]");
    metavariables.add(table.getColumnName(0));
    table.moveColumn(table.findColumnNumber(EDV.LON_NAME), 0);
    table.setColumnName(0, "Longitude [degrees_east]");
    metavariables.add(table.getColumnName(0));
    tCol = table.findColumnNumber(EDV.TIME_NAME);
    if (tCol >= 0) { // it may have become "time_ISO8601" above)
      table.moveColumn(tCol, 0);
      table.setColumnName(0, "yyyy-mm-ddThh:mm:ss.sss");
      table.columnAttributes(0).add("long_name", "Time");
    } else {
      // make empty column
      table.addColumn(
          0,
          "yyyy-mm-ddThh:mm:ss.sss",
          PrimitiveArray.factory(PAType.DOUBLE, nRows, ""),
          new Attributes().add("long_name", "Time"));
    }
    metavariables.add(table.getColumnName(0));

    // now add new Type column with '*' data (* means let ODV chose, see section 16.3.3)
    tCol = table.findColumnNumber("Type");
    if (tCol >= 0) table.setColumnName(tCol, "OriginalType"); // so not 2 cols named Type
    table.addColumn(0, "Type", PrimitiveArray.factory(PAType.CHAR, nRows, "*"), new Attributes());
    metavariables.add(table.getColumnName(0));

    // required Station column
    tCol = table.findColumnNumberWithAttributeValue("cf_role", "timeseries_id");
    if (tCol >= 0) {
      table.moveColumn(tCol, 0);
      int tCol2 = table.findColumnNumber("Station");
      if (tCol2 > 0) table.setColumnName(tCol, "OriginalStation"); // so not 2 cols named Station
      table.setColumnName(0, "Station");
    } else {
      // make empty column since this is required
      table.addColumn(0, "Station", new StringArray(nRows, true), new Attributes());
    }
    metavariables.add(table.getColumnName(0));

    // required Cruise column
    tCol = table.findColumnNumberWithAttributeValue("cf_role", "trajectory_id");
    if (tCol >= 0) {
      table.moveColumn(tCol, 0);
      int tCol2 = table.findColumnNumber("Cruise");
      if (tCol2 > 0) table.setColumnName(tCol, "OriginalCruise"); // so not 2 cols named Cruise
      table.setColumnName(0, "Cruise");
    } else {
      // make empty column since this is required
      table.addColumn(0, "Cruise", new StringArray(nRows, true), new Attributes());
    }
    metavariables.add(table.getColumnName(0));

    // open an OutputStream
    Writer writer =
        File2.getBufferedWriterUtf8(
            outputStreamSource.outputStream(
                File2.UTF_8)); // ODV User's Guide 5.2.1 allows for UTF-8

    // figure out DataType
    String cdm = globalAtts.getString("cdm_data_type");
    cdm = cdm == null ? "" : cdm.toLowerCase();
    String dataType =
        cdm.equals("timeseries")
            ? "TimeSeries"
            : cdm.equals("trajectory")
                ? "Trajectories"
                : cdm.indexOf("profile") >= 0
                    ? "Profiles"
                    : // so also TimeseriesProfile and TrajectoryProfile
                    "GeneralType";

    // write header.  see Table 16-6 in /programs/odv/odvGuide5.2.1.pdf
    // ODV says linebreaks can be \n or \r\n (see 2010-06-15 notes)
    String creator = tPublicSourceUrl;
    if (creator.startsWith("(")) // (local files) or (local database)
    creator = tInfoUrl; // a required attribute
    // ODV says not to encodeAsXML (e.g., < as &lt;) (see 2010-06-15 notes),
    // but he may have misunderstood. Encoding seems necessary.  (And affects only the creator)
    writer.write(
        "//<Creator>"
            + XML.encodeAsXML(creator)
            + "</Creator>\n"
            + // the way to get to the original source
            "//<CreateTime>"
            + Calendar2.getCurrentISODateTimeStringZulu()
            + "</CreateTime>\n"
            + // nowZ
            "//<Encoding>UTF-8</Encoding>\n"
            + "//<Software>ERDDAP - Version "
            + EDStatic.erddapVersion
            + "</Software>\n"
            + // ERDDAP
            "//<Source>"
            + EDStatic.preferredErddapUrl
            + "/tabledap/"
            + tDatasetID
            + ".html</Source>\n"
            + // Data Access Form
            // "//<SourceLastModified>???</SourceLastModified>\n" + //not available
            "//<Version>ODV Spreadsheet V4.6</Version>\n"
            + // of ODV Spreadsheet file       //???proper version number  4.6 is from Table 16-6
            // "//<MissingValueIndicators></MissingValueIndicators>\n" + //only use for non-empty
            // cell, non-NaN, e.g., -9999, but I've standardized, so ""
            "//<DataField>GeneralField</DataField>\n"
            + // !!! better if Ocean|Atmosphere|Land|IceSheet|SeaIce|Sediment
            "//<DataType>"
            + dataType
            + "</DataType>\n");

    // write column names comment lines  (see 5.2.1)
    // primaryVariable see 3.1.2 a data var (by default, the first data var)
    //  which determines sort order within station
    // (e.g., depth for profiles, or a decimal time var for timeseries and trajectory)
    int nCols = table.nColumns();
    PrimitiveArray pas[] = new PrimitiveArray[nCols];
    StringBuilder colNameLine = new StringBuilder();
    for (int col = 0; col < nCols; col++) {
      Attributes atts = table.columnAttributes(col);
      pas[col] = table.getColumn(col);
      String colName = table.getColumnName(col);
      boolean isMeta = metavariables.contains(colName); // note this before adding units to name
      String units = atts.getString("units");

      if (colName.indexOf('[') < 0
          && String2.isSomething(units)
          && !colName.equals("Cruise")
          && !colName.equals("Station")
          && !colName.equals("Type")
          && !colName.equals("yyyy-mm-ddThh:mm:ss.sss")
          && !colName.equals("time_ISO8601")) {
        // try to add units
        // ODV doesn't care about units standards. UDUNITS or UCUM are fine
        // ODV doesn't allow internal brackets; 2010-06-15 they say use parens
        units = String2.replaceAll(units, '[', '(');
        units = String2.replaceAll(units, ']', ')');
        colName += " [" + units + "]";
      }
      colNameLine.append(colName + (col < nCols - 1 ? "\t" : "\n"));

      String tag = isMeta ? "MetaVariable" : "DataVariable";
      String comment = atts.getString("comment");
      if (comment == null) {
        comment = atts.getString("long_name");
        if (comment == null) // vars created above don't have long_name attributes
        comment = "";
      }

      // See Table 3-5 /programs/odv/odvGuide5.2.1.pdf : make ODV type  BYTE, SHORT, ... , TEXT:81
      // 16.3.3 says station labels can be numeric or TEXT
      PAType paType = table.getColumn(col).elementType();
      String odvType = null;
      if (paType == PAType.BYTE) odvType = "SIGNED_BYTE";
      else if (paType == PAType.UBYTE) odvType = "BYTE"; // the original byte type
      else if (paType == PAType.CHAR) odvType = "TEXT:2";
      else if (paType == PAType.SHORT) odvType = "SHORT";
      else if (paType == PAType.USHORT) odvType = "UNSIGNED_SHORT";
      else if (paType == PAType.INT) odvType = "INTEGER";
      else if (paType == PAType.UINT) odvType = "UNSIGNED_INTEGER";
      else if (paType == PAType.LONG) odvType = "DOUBLE"; // no long!  so promote
      else if (paType == PAType.ULONG) odvType = "DOUBLE"; // no ulong! so promote
      else if (paType == PAType.FLOAT) odvType = "FLOAT";
      else if (paType == PAType.DOUBLE) odvType = "DOUBLE";
      else if (paType == PAType.STRING) odvType = "INDEXED_TEXT";
      else
        throw new SimpleException(
            EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
                + "No odvDataType specified for type="
                + pas[col].elementTypeString()
                + ".");

      writer.write(
          "//<"
              + tag
              + ">label="
              + String2.toJson65536(colName)
              + " value_type=\""
              + odvType
              + "\" "
              +
              // "qf_schema=\"\" " +  //!!! I don't support the ODV system of quality flag
              // variables. ODV can't read the file if ="".
              // "significant_digits=\"???\" " +  //I don't reliably have that information
              "is_primary_variable=\""
              + (colName.equals(primaryVar) ? "T" : "F")
              + "\" "
              + (String2.isSomething(comment)
                  ? "comment=" + String2.toJson65536(comment) + " "
                  : "")
              + "</"
              + tag
              + ">\n");
    }
    writer.write(colNameLine.toString());

    // write data
    int iso8601Col = table.findColumnNumber("time_ISO8601");
    int yyyyCol = table.findColumnNumber("yyyy-mm-ddThh:mm:ss.sss");
    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nCols; col++) {
        writer.write(
            col == yyyyCol || col == iso8601Col
                ?
                // !!!was use variable's time_precision (may be greater than seconds or may be .001
                // seconds).
                // 2020-04-14 now this matches format promised above (to ensure ODV can parse it)
                // ODV ignores time zone info, but okay to specify, e.g., Z (see 2010-06-15 notes)
                Calendar2.epochSecondsToLimitedIsoStringT(
                    "1970-01-01T00:00:00.000Z", pas[col].getDouble(row), "")
                :
                // missing numeric will be empty cell; that's fine
                // Now UTF-8, so leave all chars as is
                pas[col].getUtf8TsvString(row)); // a json-like string without surrounding "'s
        writer.write(col < nCols - 1 ? '\t' : '\n');
      }
    }

    // done!
    writer.flush(); // essential

    if (EDDTable.reallyVerbose)
      String2.log(
          "  EDDTable.saveAsODV done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * This writes grid data (not just axis data) to the outputStream in an ODV Generic Spreadsheet
   * Format .txt file. If no exception is thrown, the data was successfully written.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  public void saveAsODV(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid grid)
      throws Throwable {
    // FUTURE: it might be nice if this prevented a user from getting
    // a very high resolution subset (wasted in ODV) by reducing the
    // resolution automatically.

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsODV");
    long time = System.currentTimeMillis();

    // do quick error checking
    if (grid.isAxisDapQuery(userDapQuery))
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "You can't save just axis data in on ODV .txt file. Please select a subset of a data variable.");
    if (grid.lonIndex() < 0 || grid.latIndex() < 0 || grid.timeIndex() < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.get(Message.QUERY_ERROR, 0)
                  + EDStatic.messages.get(Message.ERROR_ODV_LLT_GRID, 0),
              EDStatic.messages.get(Message.QUERY_ERROR, language)
                  + EDStatic.messages.get(Message.ERROR_ODV_LLT_GRID, language)));
    // lon can be +-180 or 0-360. See EDDTable.saveAsODV

    // get dataAccessor first, in case of error when parsing query
    try (GridDataAccessor gda =
        new GridDataAccessor(
            language, grid, requestUrl, userDapQuery, true, false)) { // rowMajor, convertToNaN

      // write the data to the tableWriterAllWithMetadata
      TableWriterAllWithMetadata twawm =
          new TableWriterAllWithMetadata(
              language,
              grid,
              grid.getNewHistory(language, requestUrl, userDapQuery),
              grid.cacheDirectory(),
              "ODV"); // A random number will be added to it for safety.
      grid.saveAsTableWriter(gda, twawm);

      // write the ODV .txt file
      saveAsODV(
          language,
          outputStreamSource,
          twawm,
          grid.datasetID(),
          grid.publicSourceUrl(language),
          grid.infoUrl(language));
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log("  EDDGrid.saveAsODV done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
