/*
 * EDDTableFromHttpGet Copyright 2016, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import com.google.common.collect.ImmutableList;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a table of data from a collection of jsonlCSV data files which are created
 * by HTTP GET calls to ERDDAP.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2016-06-14
 */
public class EDDTableFromHttpGet extends EDDTableFromFiles {

  // special column names   //DON'T EVER CHANGE ANY OF THESE!!!
  public static final String TIMESTAMP = "timestamp"; // epic seconds as double
  public static final String AUTHOR = "author"; // String
  public static final String COMMAND = "command"; // byte
  public static final ImmutableList<String> SPECIAL_VAR_NAMES =
      ImmutableList.of(TIMESTAMP, AUTHOR, COMMAND);
  public static final ImmutableList<String> SPECIAL_VAR_TYPES =
      ImmutableList.of("double", "String", "byte");
  public static final ImmutableList<String> SPECIAL_VAR_UNITS =
      ImmutableList.of(Calendar2.SECONDS_SINCE_1970, "", "");
  // COMMAND needs CF FLAG attributes 0=insert, 1=delete
  public static final byte INSERT_COMMAND = 0;
  public static final byte DELETE_COMMAND = 1;
  public static final ImmutableList<String> NO_PROCESS_COMMANDS = ImmutableList.of(">", ">=", "=");

  public static final String NUMERIC_TIMESTAMP_REGEX =
      "numericTimestamp\":(\\d\\.\\d{2,12}E9),?\\n";
  public static final Pattern NUMERIC_TIMESTAMP_PATTERN = Pattern.compile(NUMERIC_TIMESTAMP_REGEX);

  protected String columnNames[]; // all, not just NEC
  protected String columnUnits[];
  protected PAType columnPATypes[];
  protected PrimitiveArray columnMvFv[];

  protected long lastSaveDirTableFileTableBadFiles = 0; // System.currentTimeMillis
  // insertOrDelete calls saveDirTableFileTable if &gt;=5 seconds since last save
  // This works quite well because
  //  infrequently changed datasets (&gt; every 5 seconds) will save the fileTable to disk every
  // time there is a change,
  //  but frequently changed datasets will just save fileTable every 5 seconds
  //  (and shouldn't ever be much more than 5 seconds out-of-date,
  //  except for weird case where the changes stop arriving for a while).
  // The big issue/danger here is:
  //  if there are frequent changes,
  //  and a new .insert leads to a new max or min for a variable,
  //  that change to the fileTable will be lost
  //  if the dataset is reloaded (using fileTable info from disk)
  //  if the fileTable info on disk is out-of-date.
  //  It is only at that handoff that very recent fileTable changes can be lost.
  //  Having a smaller value here minimizes that risk,
  //  but at the cost of slowing down .insert and .delete.
  // A better solution: find a way for fileTableInMemory to be
  //  passed directly to the new EDDTableFromHttpGet when the dataset is reloaded
  //  or to force the old version of the dataset to save fileTable info to disk
  //  right before reading it for new dataset.
  public static final long saveDirTableFileTableBadFilesEveryMS = 5000;

  /**
   * EDDTableFromHttpGet DOESN'T SUPPORT UNPACKWHAT OPTIONS (other than 0). This returns the default
   * value for standardizeWhat for this subclass. See Attributes.unpackVariable for options. The
   * default was chosen to mimic the subclass' behavior from before support for standardizeWhat
   * options was added.
   */
  @Override
  public int defaultStandardizeWhat() {
    return DEFAULT_STANDARDIZEWHAT;
  }

  public static final int DEFAULT_STANDARDIZEWHAT = 0;

  /**
   * This extracts the numericTimestamp from the results.
   *
   * @return the numericTimestamp (else NaN if trouble).
   */
  public static double extractTimestamp(String results) {
    Matcher matcher = NUMERIC_TIMESTAMP_PATTERN.matcher(results);
    if (matcher.find()) return String2.parseDouble(matcher.group(1));
    return Double.NaN;
  }

  /** For testing, insert/delete results will match this. */
  public static String resultsRegex(int nRows) {
    return "\\{\n"
        + "\"status\":\"success\",\n"
        + "\"nRowsReceived\":"
        + nRows
        + ",\n"
        + "\"stringTimestamp\":\"....-..-..T..:..:..\\....Z\",\n"
        + "\"numericTimestamp\":.\\..{2,12}E9\n"
        + "\\}\n";
  }

  /**
   * The constructor.
   *
   * <p>The sortedColumnSourceName can't be for a char/String variable because NcHelper binary
   * searches are currently set up for numeric vars only.
   *
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   * @param tFgdcFile This should be the fullname of a file with the FGDC that should be used for
   *     this dataset, or "" (to cause ERDDAP not to try to generate FGDC metadata for this
   *     dataset), or null (to allow ERDDAP to try to generate FGDC metadata for this dataset).
   * @param tIso19115File This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   */
  public EDDTableFromHttpGet(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      LocalizedAttributes tAddGlobalAttributes,
      List<DataVariableInfo> tDataVariables,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tMetadataFrom,
      String tCharset,
      String tSkipHeaderToRegex,
      String tSkipLinesRegex,
      int tColumnNamesRow,
      int tFirstDataRow,
      String tColumnSeparator,
      String tPreExtractRegex,
      String tPostExtractRegex,
      String tExtractRegex,
      String tColumnNameForExtract,
      String tSortedColumnSourceName,
      String tSortFilesBySourceNames,
      boolean tSourceNeedsExpandedFP_EQ,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      boolean tRemoveMVRows,
      int tStandardizeWhat,
      int tNThreads,
      String tCacheFromUrl,
      int tCacheSizeGB,
      String tCachePartialPathRegex,
      String tAddVariablesWhere)
      throws Throwable {

    super(
        "EDDTableFromHttpGet",
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tFileDir,
        tFileNameRegex,
        tRecursive,
        tPathRegex,
        tMetadataFrom,
        tCharset,
        tSkipHeaderToRegex,
        tSkipLinesRegex,
        tColumnNamesRow,
        tFirstDataRow,
        tColumnSeparator,
        tPreExtractRegex,
        tPostExtractRegex,
        tExtractRegex,
        tColumnNameForExtract,
        tSortedColumnSourceName,
        tSortFilesBySourceNames,
        tSourceNeedsExpandedFP_EQ,
        tFileTableInMemory,
        tAccessibleViaFiles,
        tRemoveMVRows,
        tStandardizeWhat,
        tNThreads,
        tCacheFromUrl,
        tCacheSizeGB,
        tCachePartialPathRegex,
        tAddVariablesWhere);

    // standardizeWhat must be 0 or absent
    String msg =
        String2.ERROR + " in EDDTableFromHttpGet constructor for datasetID=" + datasetID + ": ";
    if (standardizeWhat != 0) throw new RuntimeException(msg + "'standardizeWhat' MUST be 0.");

    // The attributes this gets/sets should not need to be localized (max/min
    // value for example). Just use the default language.
    int language = EDMessages.DEFAULT_LANGUAGE;
    // cacheFromUrl must be null
    msg = String2.ERROR + " in EDDTableFromHttpGet constructor for datasetID=" + datasetID + ": ";
    if (cacheFromUrl != null) throw new RuntimeException(msg + "'cacheFromUrl' MUST be null.");

    // columnNameForExtract can't be used
    if (String2.isSomething(columnNameForExtract))
      throw new RuntimeException(
          msg + "'columnNameForExtract' can't be used for EDDTableFromHttpGet datasets.");

    // sortedColumnSourceName must be ""
    if (!"".equals(sortedColumnSourceName))
      throw new RuntimeException(msg + "'sortedColumnSourceName' MUST be nothing.");

    // Check last 3 var SPECIAL_VAR_NAMES: TIMESTAMP, AUTHOR, COMMAND
    for (int i = 0; i < 3; i++) {
      int which = String2.indexOf(dataVariableSourceNames, SPECIAL_VAR_NAMES.get(i));
      if (which < 0)
        throw new SimpleException(
            msg
                + "One of the variables must have the name \""
                + SPECIAL_VAR_NAMES.get(which)
                + "\".");

      if (!SPECIAL_VAR_TYPES.get(i).equals(dataVariables[which].sourceDataType()))
        throw new SimpleException(
            msg
                + "Variable="
                + SPECIAL_VAR_NAMES.get(i)
                + " must have dataType="
                + SPECIAL_VAR_TYPES.get(i)
                + ", not \""
                + dataVariables[which].sourceDataType()
                + "\".");

      if (!SPECIAL_VAR_UNITS.get(i).isEmpty()
          && !SPECIAL_VAR_UNITS.get(i).equals(dataVariables[which].units()))
        throw new SimpleException(
            msg
                + "Variable="
                + SPECIAL_VAR_NAMES.get(i)
                + " must have units="
                + SPECIAL_VAR_UNITS.get(i)
                + ", not \""
                + dataVariables[which].units()
                + "\".");
    }

    int nDV = dataVariables.length;
    columnNames = new String[nDV];
    columnUnits = new String[nDV];
    columnPATypes = new PAType[nDV];
    columnMvFv = new PrimitiveArray[nDV];
    for (int dvi = 0; dvi < nDV; dvi++) {
      EDV edv = dataVariables[dvi];
      String sourceName = edv.sourceName();
      String destName = edv.destinationName();
      columnNames[dvi] = sourceName;
      columnUnits[dvi] =
          edv.addAttributes()
              .getString(language, "units"); // here, "source" units are in addAttributes!
      columnPATypes[dvi] = edv.sourceDataPAType();

      // No char variables
      if (columnPATypes[dvi] == PAType.CHAR)
        throw new SimpleException(msg + "No dataVariable can have dataType=char.");

      // ! column sourceNames must equal destinationNames (unless "=something")
      if (!sourceName.startsWith("=") && !sourceName.equals(destName))
        throw new SimpleException(
            msg
                + "Every variable sourceName and destinationName must match ("
                + sourceName
                + " != "
                + destName
                + ").");

      // columnMvFv
      if (columnPATypes[dvi] == PAType.STRING) {
        StringArray tsa = new StringArray(2, false);
        if (edv.stringMissingValue().length() > 0) tsa.add(edv.stringMissingValue());
        if (edv.stringFillValue().length() > 0) tsa.add(edv.stringFillValue());
        columnMvFv[dvi] = tsa.size() == 0 ? null : tsa;
      } else if (columnPATypes[dvi] == PAType.LONG || columnPATypes[dvi] == PAType.ULONG) {
        StringArray tsa = new StringArray(2, false);
        String ts = edv.combinedAttributes().getString(language, "missing_value");
        if (ts != null) tsa.add(ts);
        ts = edv.combinedAttributes().getString(language, "_FillValue");
        if (ts != null) tsa.add(ts);
        columnMvFv[dvi] = tsa.size() == 0 ? null : tsa;
      } else {
        DoubleArray tda = new DoubleArray(2, false);
        if (!Double.isNaN(edv.sourceMissingValue())) tda.add(edv.sourceMissingValue());
        if (!Double.isNaN(edv.sourceFillValue())) tda.add(edv.sourceFillValue());
        columnMvFv[dvi] = tda.size() == 0 ? null : tda;
      }
    }

    if (verbose)
      String2.log(
          "*** EDDTableFromHttpGet constructor for datasetID="
              + datasetID
              + " finished successfully.");
  }

  @Override
  protected void earlyInitialization() {
    setHttpGetRequiredVariableNames(
        addGlobalAttributes.getString(EDMessages.DEFAULT_LANGUAGE, HTTP_GET_REQUIRED_VARIABLES));
    setHttpGetDirectoryStructure(
        addGlobalAttributes.getString(EDMessages.DEFAULT_LANGUAGE, HTTP_GET_DIRECTORY_STRUCTURE));
    setHttpGetKeys(addGlobalAttributes.getString(EDMessages.DEFAULT_LANGUAGE, HTTP_GET_KEYS));
    addGlobalAttributes.remove(HTTP_GET_KEYS);
  }

  /** The constructor for EDDTableFromHttpGet calls this to set httpGetRequiredVariableNames. */
  private void setHttpGetRequiredVariableNames(String tRequiredVariablesCSV) {
    if (!String2.isSomething(tRequiredVariablesCSV))
      throw new RuntimeException(
          String2.ERROR
              + " in EDDTableFromHttpGet constructor for datasetID="
              + datasetID
              + ": "
              + HTTP_GET_REQUIRED_VARIABLES
              + " MUST be in globalAttributes.");
    httpGetRequiredVariableNames = StringArray.fromCSV(tRequiredVariablesCSV).toStringArray();
    if (verbose)
      String2.log(
          "  "
              + HTTP_GET_REQUIRED_VARIABLES
              + "="
              + String2.toCSSVString(httpGetRequiredVariableNames));
  }

  /**
   * The constructor for EDDTableFromHttpGet calls this to set httpGetDirectoryStructure variables.
   */
  private void setHttpGetDirectoryStructure(String tDirStructure) {

    if (!String2.isSomething(tDirStructure))
      throw new RuntimeException(
          String2.ERROR
              + " in EDDTableFromHttpGet constructor for datasetID="
              + datasetID
              + ": "
              + HTTP_GET_DIRECTORY_STRUCTURE
              + " MUST be in globalAttributes.");
    httpGetDirectoryStructureColumnNames = new StringArray();
    httpGetDirectoryStructureNs = new IntArray();
    httpGetDirectoryStructureCalendars = new IntArray();
    EDDTableFromHttpGet.parseHttpGetDirectoryStructure(
        tDirStructure,
        httpGetDirectoryStructureColumnNames,
        httpGetDirectoryStructureNs,
        httpGetDirectoryStructureCalendars);
    if (verbose)
      String2.log(
          "  httpGetDirectoryStructureColumnNames="
              + httpGetDirectoryStructureColumnNames.toString()
              + "\n"
              + "  httpGetDirectoryStructureNs="
              + httpGetDirectoryStructureNs.toString()
              + "\n"
              + "  httpGetDirectoryStructureCalendars="
              + httpGetDirectoryStructureCalendars.toString());
  }

  /**
   * The constructor for EDDTableFromHttpGet calls this to set HttpGetKeys.
   *
   * @param tHttpGetKeys a CSV of author_key values.
   */
  private void setHttpGetKeys(String tHttpGetKeys) {

    String msg =
        String2.ERROR + " in EDDTableFromHttpGet constructor for datasetID=" + datasetID + ": ";
    String inForm =
        "Each of the httpGetKeys must be in the form author_key, with only ASCII characters (but no space, ', \", or comma), and where the key is at least 8 characters long.";
    if (tHttpGetKeys == null
        || tHttpGetKeys.indexOf('\"') >= 0
        || // be safe, avoid trickery
        tHttpGetKeys.indexOf('\'') >= 0) // be safe, avoid trickery
    throw new RuntimeException(msg + inForm);
    httpGetKeys = new HashSet<>();
    String keyAr[] = StringArray.arrayFromCSV(tHttpGetKeys);
    for (int i = 0; i < keyAr.length; i++) {
      if (String2.isSomething(keyAr[i])) {
        keyAr[i] = keyAr[i].trim();
        int po = keyAr[i].indexOf('_');
        if (po <= 0
            || // can't be 0: so author must be something
            po >= keyAr[i].length() - 8
            || // key must be 8+ chars
            !String2.isAsciiPrintable(keyAr[i])
            || keyAr[i].indexOf(' ') >= 0
            || // isAsciiPrintable allows ' ' (be safe, avoid trickery)
            keyAr[i].indexOf(',') >= 0) { // isAsciiPrintable allows , (be safe, avoid trickery)
          throw new RuntimeException(msg + inForm + " (key #" + i + ")");
        } else {
          httpGetKeys.add(keyAr[i]); // not String2.canonical, because then publicly accessible
        }
      }
    }
    if (httpGetKeys.size() == 0)
      throw new RuntimeException(msg + HTTP_GET_KEYS + " MUST be in globalAttributes.");
  }

  /**
   * This gets source data from one file. See documentation in EDDTableFromFiles.
   *
   * @param sourceDataNames must be specified (not null or size=0)
   * @param sourceDataTypes must be specified (not null or size=0)
   * @throws Throwable if too much data. This won't throw an exception if no data.
   */
  @Override
  public Table lowGetSourceDataFromFile(
      String tFileDir,
      String tFileName,
      StringArray sourceDataNames,
      String sourceDataTypes[],
      double sortedSpacing,
      double minSorted,
      double
          maxSorted, // maxSorted is adulterated if time and close to NOW. Can't use for timestamp.
      StringArray sourceConVars,
      StringArray sourceConOps,
      StringArray sourceConValues,
      boolean getMetadata,
      boolean mustGetData)
      throws Throwable {

    if (!mustGetData)
      // Just return a table with columns but no rows. There is never any metadata in the underlying
      // jsonlCSV files.
      return Table.makeEmptyTable(sourceDataNames.toArray(), sourceDataTypes);

    boolean process = true;
    double maxTimestampSeconds = Double.MAX_VALUE; // i.e., don't constrain timestamp
    if (sourceConVars != null) {
      for (int i = 0; i < sourceConVars.size(); i++) {
        String scVar = sourceConVars.get(i);
        if (TIMESTAMP.equals(scVar)) {
          String tOp = sourceConOps.get(i);
          if (NO_PROCESS_COMMANDS.indexOf(tOp) >= 0) { // timestamp> >= or = leads to no processing
            process = false;
            break;
          } else if (tOp.equals(PrimitiveArray.REGEX_OP)) {
            throw new SimpleException(
                MustBe.THERE_IS_NO_DATA
                    + " ("
                    + TIMESTAMP
                    + PrimitiveArray.REGEX_OP
                    + " isn't allowed)");
          } else {
            double td = String2.parseDouble(sourceConValues.get(i));
            if (Double.isNaN(td))
              throw new SimpleException(
                  MustBe.THERE_IS_NO_DATA + " (" + TIMESTAMP + " constraint)");
            // <1000 is the same as <=999.999 since timestamp precision is 0.001
            if (tOp.equals("<")) td -= 0.001;
            maxTimestampSeconds = Math.min(maxTimestampSeconds, td);
          }
        }
      }
    }

    return readFile(
        tFileDir + tFileName,
        sourceDataNames,
        sourceDataTypes,
        httpGetRequiredVariableNames,
        httpGetRequiredVariableTypes,
        process,
        maxTimestampSeconds);
  }

  /**
   * This gets the data from one file and perhaps processes it (edits and deletes are applied) up to
   * and including rows with the specified timestampSeconds value.
   *
   * @param fullFileName the full file name.
   * @param sourceDataNames must be fully specified (not null or length=0)
   * @param sourceDataPATypes must be fully specified (not null or length=0)
   * @param tRequiredVariableNames are the dataset's requiredVariableNames, e.g., stationID, time
   * @param tRequiredVariableTypes are the types for each of the tRequiredVariableNames.
   * @param process If true, the log is processed. If false, the raw data is returned.
   * @param maxTimestampSeconds This is the maximum timestampSeconds to be kept (regardless of
   *     process setting). Use Double.MAX_VALUE or Double.NaN to keep all rows.
   * @return the processed or unprocessed data table from one file. Char vars are stored as shorts
   *     in the file, but returned as chars here.
   */
  public static Table readFile(
      String fullFileName,
      StringArray sourceDataNames,
      String sourceDataTypes[],
      String[] tRequiredVariableNames,
      String[] tRequiredVariableTypes,
      boolean process,
      double maxTimestampSeconds)
      throws Throwable {

    // String2.log(">> EDDTableFromHttpGet.readFile process=" + process + " timestampSeconds=" +
    // timestampSeconds);
    // File2.directReadFrom88591File(fullFileName));

    // ensure required columns are included (if needed)
    int nRCN = tRequiredVariableNames.length;
    boolean removeLaterTimestampRows = maxTimestampSeconds < Double.MAX_VALUE; // NaN -> false
    if (removeLaterTimestampRows || process) {
      StringArray sourceDataTypesSA = new StringArray(sourceDataTypes);

      // need required columns and timestamp and command to know which rows are for the same data
      // final row / to apply add/delete
      if (sourceDataNames.indexOf(TIMESTAMP) < 0) {
        sourceDataNames.add(TIMESTAMP);
        sourceDataTypesSA.add("double");
      }
      if (process) {
        if (sourceDataNames.indexOf(COMMAND) < 0) {
          sourceDataNames.add(COMMAND);
          sourceDataTypesSA.add("byte");
        }
        for (int i = 0; i < nRCN; i++) {
          String tName = tRequiredVariableNames[i];
          if (sourceDataNames.indexOf(tName) < 0) {
            sourceDataNames.add(tName);
            sourceDataTypesSA.add(tRequiredVariableTypes[i]);
          }
        }
      }

      sourceDataTypes = sourceDataTypesSA.toArray();
    }

    // read needed columns of the file (so UPDATE's and DELETE's can be processed)
    Table table = new Table();

    // 2021-03-05 CHANGE to no lock.
    //  Since file is add-only, it doesn't matter if another thread is writing to the end of this
    // file.
    //  Worst case: there's a partial line at end of file and readJsonlCSV will stop at that line.

    //// synchronized: don't read from file during a file write in insertOrDelete
    fullFileName = String2.canonical(fullFileName);
    ReentrantLock lock = String2.canonicalLock(fullFileName);
    if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
      throw new TimeoutException(
          "Timeout waiting for lock on fullFileName in EDDTableFromHttpGet.");
    try {
      table.readJsonlCSV(fullFileName, sourceDataNames, sourceDataTypes, false);
    } finally {
      lock.unlock();
    }
    // String2.log(">> table in " + fullFileName + " :\n" + table.dataToString());
    // table.saveAsDDS(System.out, "s");

    // gather info about the table
    int nCols = table.nColumns();
    int nRows = table.nRows();
    int timestampColi = table.findColumnNumber(TIMESTAMP); // may be -1 if not needed so not present

    // remove rows with timestamp > requested timestamp (whether process=true or not)
    if (removeLaterTimestampRows) {

      // timestampPA should be sorted in ascending order (perhaps with ties)
      // BUT if computer's clock is rolled back, there is possibility of out-of-order.
      // OR if different threads doing changes processed at different speeds could finish
      // out-of-order.
      // so sort by timestampColi first
      table.ascendingSort(new int[] {timestampColi});
      // String2.log(">>  timestamp values=" + table.getColumn(timestampColi));

      int row =
          table
              .getColumn(timestampColi)
              .binaryFindFirstGE(
                  0,
                  nRows - 1,
                  PAOne.fromDouble(
                      maxTimestampSeconds
                          + 0.0005)); // 1/2 milli later to avoid rounding problems, and because I
      // want to remove > (not GE)
      // String2.log("  timestamp constraint removed " + (nRows-row) + " rows. Now nRows=" + row);
      if (row < nRows) {
        table.removeRows(row, nRows); // exclusive
        nRows = row;
      }
    }

    if (!process || nRows == 0) return table;

    // process the file (apply add/delete for groups of rows that have same required variable
    // values)
    // sort based on requiredVariableNames+timestamp  (e.g., stationID, time, timestamp)
    // Except for revisions, the data should already be in this order or very close to it.
    int commandColi = table.findColumnNumber(COMMAND);
    PrimitiveArray pas[] = new PrimitiveArray[nCols];
    for (int col = 0; col < nCols; col++) pas[col] = table.getColumn(col);
    int sortBy[] = new int[nRCN + 1];
    for (int i = 0; i < nRCN; i++) {
      sortBy[i] = table.findColumnNumber(tRequiredVariableNames[i]);
      if (sortBy[i] < 0)
        throw new SimpleException(
            String2.ERROR
                + " while reading "
                + fullFileName
                + ": "
                + "columnName="
                + tRequiredVariableNames[i]
                + " not found in "
                + table.getColumnNamesCSVString()
                + ".");
    }
    sortBy[nRCN] = timestampColi;
    // I am confident that sorting by timestamp will be correct/exact.
    // Although the numbers are 0.001, if bruised (0.000999999999),
    //  they should be bruised the same way on different rows.
    // If that is incorrect, then a solution is:
    //  multiply time and timestamp by 1000 and round to integer before sorting
    //  then divide by 1000 after justKeep() below.
    table.ascendingSort(sortBy);

    // Just keep last row of each group of rows where HttpGetRequiredVariables are same.
    // But if that row is DELETE, then don't keep it either.
    BitSet justKeep = new BitSet(nRows); // all false
    for (int row = 0; row < nRows; row++) { // look at row and row+1
      if (row < nRows - 1) {
        boolean allSame = true; // not considering timestamp
        for (int i = 0; i < nRCN; i++) { // not considering timestamp
          if (pas[sortBy[i]].compare(row, row + 1) != 0) {
            allSame = false;
            break;
          }
        }
        if (allSame) // all requiredVariableNames are same as next row
        continue; // don't keep this row
      }

      // this row is last of a group
      // if last command is DELETE, then delete this row
      if (pas[commandColi].getInt(row) == DELETE_COMMAND) continue; // don't keep this row

      // else keep this row
      justKeep.set(row);
    }
    table.justKeep(justKeep);

    return table;
  }

  /**
   * This parses the httpGetDirectoryStructure specification.
   *
   * @param specification e.g, stationID/10years/7days
   * @param dsSourceName will be filled, with [i] = a var sourceName or "". sourceNames haven't been
   *     tested to see if they are in the dataset.
   * @param dsN will be filled, with [i] = the number of Calendar items, or -1
   * @param dsCalendar will be filled, with [i] = the e.g., Calendar.MONTH, or -1
   * @throws RuntimeException if trouble
   */
  public static void parseHttpGetDirectoryStructure(
      String specification, StringArray dsSourceName, IntArray dsN, IntArray dsCalendar) {

    dsSourceName.clear();
    dsN.clear();
    dsCalendar.clear();
    String parts[] = String2.split(specification, '/');
    Pattern pattern = Pattern.compile("(\\d+)([A-Za-z]+)");
    for (int p = 0; p < parts.length; p++) {
      Matcher matcher = pattern.matcher(parts[p]);
      boolean isNUnits = matcher.matches(); // e.g., 5days
      int cal = -1;
      if (isNUnits) { // well, probably isNUnits
        // e.g., 5days
        // but it may be a column name which matches the pattern accidentally,
        // so try/catch
        try {
          String units = matcher.group(2);
          cal = Calendar2.unitsToConstant(units); // throws exception
          if (cal == Calendar.WEEK_OF_YEAR)
            throw new RuntimeException(
                String2.ERROR
                    + " parsing httpGetDirectoryStructure: "
                    + "units="
                    + units
                    + " is invalid.");
        } catch (Exception e) {
          isNUnits = false;
          String2.log(
              "Treating httpGetDirectoryStructure part#"
                  + p
                  + "="
                  + parts[p]
                  + " as a columnName ("
                  + e
                  + ").");
        }
      }

      if (isNUnits) {
        dsSourceName.add("");
        dsN.add(String2.parseInt(matcher.group(1)));
        dsCalendar.add(cal);
      } else {
        dsSourceName.add(parts[p]);
        dsN.add(-1);
        dsCalendar.add(-1);
      }
    }
    dsSourceName.trimToSize();
    dsN.trimToSize();
    dsCalendar.trimToSize();
  }

  /**
   * This figures out the name of the relevant file (which may or may not exist.
   *
   * @param startDir with trailing slash
   * @param tDirStructureColumnNames For each part, the variable's source column name or "" if not
   *     used for this part. Any column names here should be in requiredVariableNames.
   * @param tDirStructureNs For each part, the number of YEAR, MONTH, ... or -1 if not used for this
   *     part
   * @param tDirStructureCalendars For each part, Calendar.YEAR, MONTH, DATE, HOUR_OF_DAY, MINUTE,
   *     SECOND, MILLISECOND, or -1 if not used for this part
   * @param tColumnNames the source names of the columns
   * @param tColumnValues the source values associated with the tColumnNames. All should have the
   *     same size(). Only values on the specified row will be used.
   * @param row the value of the rightmost array of tColumnSourceValues
   * @param timeEpSec the time value, in epoch seconds. It is usually a requiredVariable, but not
   *     always. It is an error if it is needed here, but timeEpSec is NaN.
   * @return the full file dir+name, starting with startDir. The file may not exist yet.
   */
  public static String whichFile(
      String startDir,
      StringArray tDirStructureColumnNames,
      IntArray tDirStructureNs,
      IntArray tDirStructureCalendars,
      String tColumnNames[],
      PrimitiveArray tColumnValues[],
      int row,
      double timeEpSec) {

    StringBuilder dirSB = new StringBuilder(startDir);
    StringBuilder nameSB = new StringBuilder();
    int nParts = tDirStructureColumnNames.size();
    for (int i = 0; i < nParts; i++) {
      if (i > 0) {
        dirSB.append('/');
        nameSB.append('_');
      }
      int cal = tDirStructureCalendars.get(i);
      if (cal == -1) {
        // Find the var. Add its value.
        int sni = String2.indexOf(tColumnNames, tDirStructureColumnNames.get(i));
        if (sni < 0)
          throw new SimpleException(
              String2.ERROR
                  + " in httpGetDirectoryStructure part#"
                  + i
                  + ": column="
                  + tDirStructureColumnNames.get(i)
                  + " isn't in columnNames="
                  + String2.toCSSVString(tColumnNames)
                  + ".");
        // data value of "" is a valid value. It will be converted to something.
        String tp = String2.encodeFileNameSafe(tColumnValues[sni].getString(row));
        if (i < nParts - 1) dirSB.append(tp);
        nameSB.append(tp);

      } else {
        // Find the time part. Truncate to n'th precision.
        // e.g., 17 seconds to 5seconds precision is 15 seconds.
        // (MONTH is 0-based, so that works correctly as is.)
        if (timeEpSec >= 253402300800.0
            || // 10000-01-01
            timeEpSec <= -377711769600.0) // -10000-01-01
        throw new SimpleException(
              String2.ERROR
                  + " in httpGetDirectoryStructure part#"
                  + i
                  + ": invalid time value (timeEpSec="
                  + timeEpSec
                  + ")!");
        // need a new gc for each part since gc is modified
        ZonedDateTime dt = Calendar2.epochSecondsToZdt(timeEpSec);
        int n = tDirStructureNs.get(i);
        ChronoField field = Calendar2.getChronoFieldFromCalendarField(cal);
        if (cal == Calendar.MONTH) {
          dt = dt.with(field, ((dt.get(field) - 1) / n) * n + 1);
        } else {
          dt = dt.with(field, ((long) dt.get(field) / n) * n);
        }
        // Get the ISO 8601 date/time string just to that precision/field.
        String s = Calendar2.formatAsISODateTimeT3Z(dt); // to millis
        int nChar = s.length();
        if (cal == Calendar.YEAR) nChar = 4;
        else if (cal == Calendar.MONTH) nChar = 7;
        else if (cal == Calendar.DATE) nChar = 10;
        else if (cal == Calendar.HOUR_OF_DAY) nChar = 13;
        else if (cal == Calendar.MINUTE) nChar = 16;
        else if (cal == Calendar.SECOND) nChar = 19;
        else nChar--; // to millis precision
        String tp = s.substring(0, nChar);
        tp = String2.replaceAll(tp, ':', '-'); // make fileNameSafe
        if (i < nParts - 1) dirSB.append(tp);
        nameSB.append(tp);
      }
    }

    return dirSB.toString() + nameSB + ".jsonl";
  }

  /**
   * This is the non-static insertOrDelete method which calls the static insertOrDelete method.
   *
   * @param language the index of the selected language
   * @return the response String
   * @throws Throwable if any kind of trouble
   */
  public String insertOrDelete(int language, byte command, String userDapQuery) throws Throwable {

    if (cacheFromUrl != null) {
      throw new SimpleException(
          "For EDDTableFromHttpGet datasets, if cacheFromUrl is active, "
              + ".insert and .delete are not allowed for this local dataset "
              + "because any changes would be lost the next time the local dataset "
              + "checks for files from the remote dataset.");
    }

    Table tDirTable = dirTable; // succeeds if fileTableInMemory (which it should always be)
    Table tFileTable = fileTable;
    if (tDirTable == null)
      tDirTable = tryToLoadDirFileTable(datasetDir() + DIR_TABLE_FILENAME); // may be null
    if (tFileTable == null)
      tFileTable = tryToLoadDirFileTable(datasetDir() + FILE_TABLE_FILENAME); // may be null
    if (tDirTable == null || tFileTable == null) {
      requestReloadASAP();
      throw new SimpleException("dirTable and/or fileTable are null!");
    }

    String response =
        insertOrDelete(
            language,
            fileDir,
            httpGetDirectoryStructureColumnNames,
            httpGetDirectoryStructureNs,
            httpGetDirectoryStructureCalendars,
            httpGetKeys,
            combinedGlobalAttributes,
            columnNames,
            columnUnits,
            columnPATypes,
            columnMvFv,
            httpGetRequiredVariableNames,
            command,
            userDapQuery,
            tDirTable,
            tFileTable);

    // do more with badFileMap?

    // do slow / background thing first:
    // save dirTableFileTable to disk?
    // there is always a change to fileTable and min max (e.g., timeStamp)
    long tTime = System.currentTimeMillis();
    if (!fileTableInMemory
        || tTime - lastSaveDirTableFileTableBadFiles >= saveDirTableFileTableBadFilesEveryMS) {
      saveDirTableFileTableBadFiles(
          standardizeWhat,
          tDirTable,
          tFileTable, // throws Throwable
          null); // null so ignore badFilesMap
      lastSaveDirTableFileTableBadFiles = tTime;
    }

    // then faster things
    Table tMinMaxTable = makeMinMaxTable((StringArray) tDirTable.getColumn(0), tFileTable);

    // then, change secondary parts of instance variables
    // e.g., update all variable destinationMinMax
    updateDestinationMinMax(tMinMaxTable);

    // then put in place as quickly/atomically as possible
    minMaxTable = tMinMaxTable; // swap into place quickly
    if (fileTableInMemory) { // it will always be true
      // quickly swap into place
      dirTable = tDirTable;
      fileTable = tFileTable;
    }

    return response;
  }

  /**
   * This is used to add insert or delete commands into a data file of this dataset. This is
   * EDDTableFromHttpGet overwriting the default implementation.
   *
   * <p>The key should be author_secret. So keys are specific to specific people/actors. The author
   * will be kept and added to the 'author' column in the dataset.
   *
   * <p>INSERT works like SQL's INSERT and UPDATE. Effectively, if the info matches existing values
   * of sortColumnSourceNames, the previous data is updated/overwritten. Otherwise, it is inserted.
   * (In reality, the info is just added to the log.)
   *
   * <p>DELETE works like SQL's DELETE, but just enters the deletion info in the log. The original
   * info isn't actually deleted.
   *
   * <p>Timings on my pathetic laptop, with Win 7 (2018-06-27): <br>
   * nRows/insert ms/insert <br>
   * 1 1.0025 <br>
   * 10 1.835 <br>
   * 100 2.9075 <br>
   * 1000 12.69 (but sometimes longer) <br>
   * So clearly, you can push vastly more data in if you do it in batches. <br>
   * The limit is probably Tomcat query length, which you can change (default is 8KB?)
   *
   * <p>Having this static method do the actual work makes the system easier to test.
   *
   * @param language the index of the selected language
   * @param tDirStructureColumnNames the column names for the parts of the dir and file names. All
   *     of these names must be in requiredVariableNames.
   * @param keys the valid values of author= (to authenticate the author)
   * @param tGlobalAttributes used when creating a new file
   * @param columnNames the names of ALL of the dataset's source variables, in the dataset's order,
   *     including timestamp, author, or command. The time variable, if any, must be named time. For
   *     a given dataset, this must not change over time.
   * @param columnUnits any of them may be null or "". All timestamp columns (in the general sense)
   *     should have UDUNITS String time units (e.g., "yyyy-MM-dd'T'HH:mm:ss'Z'") or numeric time
   *     units (e.g., "days since 1985-01-01"). For INSERT and DELETE calls, the time values must be
   *     in that format (you can't revert to ISO 8601 format as with data requests in the rest of
   *     ERDDAP).
   * @param columnPATypes the Java types (e.g., PAType.DOUBLE, PAType.LONG, PAType.CHAR,
   *     PAType.STRING). The missing values are the default missing values for PrimitiveArrays. All
   *     timestamp columns (in the general sense) MUST be PAType.DOUBLE.
   * @param columnMvFv a PrimitiveArray of any suitable type (all are used via pa.indexOf(String)).
   *     with the missing_value and/or _FillValue for each column (or null for each). If mv or fv is
   *     the PrimitiveArray standard missing_value, it needn't be included in this PA.
   * @param requiredVariableNames the variable names which identify a unique row. All
   *     requiredVariableNames MUST be in columnNames. Insert requests MUST have all of the
   *     requiredVariableNames and usually have all columnNames. Missing columns will get (standard
   *     PrimitiveArray) missing values. Delete requests MUST have all of the requiredVariableNames
   *     and, in addition, usually have just author. Other columns are irrelevant. This should be as
   *     minimal as possible, and always includes time: For TimeSeries: stationID, time. For
   *     Trajectory: trajectoryID, time. For Profile: stationID, time, depth. For TimeSeriesProfile:
   *     stationID, time, depth. For TrajectoryProfile: trajectoryID, time, depth.
   * @param command INSERT_COMMAND or DELETE_COMMAND
   * @param userDapQuery the param string, still percent-encoded
   * @param dirTable a copy of the dirTable (changes may be made to it) or null.
   * @param fileTable a copy of the fileTable (changes may be made to it) or null.
   * @return the response string
   * @throws Throwable if any kind of trouble
   */
  public static String insertOrDelete(
      int language,
      String startDir,
      StringArray tDirStructureColumnNames,
      IntArray tDirStructureNs,
      IntArray tDirStructureCalendars,
      Set<String> keys,
      LocalizedAttributes tGlobalAttributes,
      String columnNames[],
      String columnUnits[],
      PAType columnPATypes[],
      PrimitiveArray columnMvFv[],
      String requiredVariableNames[],
      byte command,
      String userDapQuery,
      Table dirTable,
      Table fileTable)
      throws Throwable {

    double timestampSeconds = System.currentTimeMillis() / 1000.0;
    if (dirTable == null || fileTable == null) { // ensure both or neither
      dirTable = null;
      fileTable = null;
    }

    // store things in data structures paralleling columnNames (i.e., [col])
    int nColumns = columnNames.length;
    if (nColumns == 0) throw new SimpleException(String2.ERROR + ": columnNames not specified.");
    PrimitiveArray columnValues[] = new PrimitiveArray[nColumns];
    boolean columnIsFixed[] = new boolean[nColumns];
    int timeColumn = -1;
    String timeFormat = null; // used if time variable is string
    double timeBaseAndFactor[] = null; // used if time variable is numeric
    int timestampColumn = -1;
    int authorColumn = -1;
    int commandColumn = -1;
    for (int col = 0; col < nColumns; col++) {
      columnIsFixed[col] = columnNames[col].charAt(0) == '=';

      if (!String2.isSomething(columnUnits[col])) columnUnits[col] = "";

      switch (columnNames[col]) {
        case EDV.TIME_NAME -> {
          timeColumn = col;
          if (columnPATypes[col] == PAType.STRING) {
            // string times
            if (!Calendar2.isStringTimeUnits(columnUnits[col])) {
              String2.log("columnUnits[" + col + "]=" + columnUnits[col]);
              throw new SimpleException(
                  EDStatic.bilingual(
                      language,
                      EDStatic.messages.get(Message.QUERY_ERROR, 0)
                          + "Invalid units for the string time variable. Units MUST specify the format of the time values.",
                      EDStatic.messages.get(Message.QUERY_ERROR, language)
                          + "Invalid units for the string time variable. Units MUST specify the format of the time values."));
            }
            timeFormat = columnUnits[col];
          } else {
            // numeric times
            timeBaseAndFactor =
                Calendar2.getTimeBaseAndFactor(
                    columnUnits[col]); // throws RuntimeException if trouble
          }
        }
        case TIMESTAMP -> timestampColumn = col;
        case AUTHOR -> authorColumn = col;
        case COMMAND -> commandColumn = col;
      }
    }
    columnValues[timestampColumn] = new DoubleArray(new double[] {timestampSeconds});
    columnValues[commandColumn] = new ByteArray(new byte[] {command});

    // parse the userDapQuery's parts. Ensure it is valid.
    String parts[] = String2.split(userDapQuery, '&');
    int nParts = parts.length;
    String author = null; // the part before '_'
    int arraySize = -1; // until an array is found
    BitSet requiredVariablesFound = new BitSet();
    for (int p = 0; p < nParts; p++) {
      int eqPo = parts[p].indexOf('=');
      if (eqPo <= 0
          || // no '=' or no name
          "<>~!".indexOf(parts[p].charAt(eqPo - 1)) >= 0) // <= >= != ~=
      throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.get(Message.QUERY_ERROR, 0)
                    + "The \""
                    + parts[p]
                    + "\" parameter isn't in the form name=value.",
                EDStatic.messages.get(Message.QUERY_ERROR, language)
                    + "The \""
                    + parts[p]
                    + "\" parameter isn't in the form name=value."));
      String tName = parts[p].substring(0, eqPo); // names should be varNames so not percent encoded
      String tValue = SSR.percentDecode(parts[p].substring(eqPo + 1));
      // String2.log(">> tName=" + tName + " tValue=" + String2.annotatedString(tValue));

      // catch and verify author=
      if (tName.equals(AUTHOR)) {
        if (tValue.startsWith("\"") && tValue.endsWith("\"")) tValue = String2.fromJson(tValue);

        if (p != nParts - 1)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.get(Message.QUERY_ERROR, 0)
                      + "author= must be the last parameter.",
                  EDStatic.messages.get(Message.QUERY_ERROR, language)
                      + "author= must be the last parameter."));
        if (!keys.contains(
            tValue)) // this tests validity of author_key (since checked when created)
        throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.get(Message.QUERY_ERROR, 0) + "Invalid author_key.",
                  EDStatic.messages.get(Message.QUERY_ERROR, language) + "Invalid author_key."));
        int po = Math.max(0, tValue.indexOf('_'));
        author = tValue.substring(0, po);
        columnValues[authorColumn] = new StringArray(new String[] {author});

      } else {
        // is it a requiredVariable?
        int whichRC = String2.indexOf(requiredVariableNames, tName);
        if (whichRC >= 0) requiredVariablesFound.set(whichRC);

        // whichColumn?
        int whichCol = String2.indexOf(columnNames, tName);
        if (whichCol < 0) {
          String2.log("columnNames=" + String2.toCSSVString(columnNames));
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.get(Message.QUERY_ERROR, 0) + "Unknown variable name=" + tName,
                  EDStatic.messages.get(Message.QUERY_ERROR, language)
                      + "Unknown variable name="
                      + tName));
        } else if (whichCol == timestampColumn) {
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.get(Message.QUERY_ERROR, 0)
                      + "An .insert or .delete request must not include "
                      + TIMESTAMP
                      + " as a parameter.",
                  EDStatic.messages.get(Message.QUERY_ERROR, language)
                      + "An .insert or .delete request must not include "
                      + TIMESTAMP
                      + " as a parameter."));
        } else if (whichCol == commandColumn) {
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.get(Message.QUERY_ERROR, 0)
                      + "An .insert or .delete request must not include "
                      + COMMAND
                      + " as a parameter.",
                  EDStatic.messages.get(Message.QUERY_ERROR, language)
                      + "An .insert or .delete request must not include "
                      + COMMAND
                      + " as a parameter."));
        }

        if (columnValues[whichCol] != null)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.get(Message.QUERY_ERROR, 0)
                      + "There are two parameters with variable name="
                      + tName
                      + ".",
                  EDStatic.messages.get(Message.QUERY_ERROR, language)
                      + "There are two parameters with variable name="
                      + tName
                      + "."));

        // get the values
        if (tValue.startsWith("[") && tValue.endsWith("]")) {
          // deal with array of values: name=[valuesCSV]

          StringArray sa =
              new StringArray(
                  StringArray.arrayFromCSV(
                      tValue.substring(1, tValue.length() - 1), ",", false)); // trim?
          columnValues[whichCol] =
              PrimitiveArray.factory(
                  columnPATypes[whichCol], sa); // does nothing if desired class is String
          // if (columnPATypes[whichCol] == PAType.CHAR || columnPATypes[whichCol] == PAType.STRING)
          // String2.log(">> writing var=" + tName + " pa=" + columnValues[whichCol]);
          if (arraySize < 0) arraySize = columnValues[whichCol].size();
          else if (arraySize != columnValues[whichCol].size())
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.get(Message.QUERY_ERROR, 0)
                        + "Different parameters with arrays have different sizes: "
                        + arraySize
                        + "!="
                        + columnValues[whichCol].size()
                        + ".",
                    EDStatic.messages.get(Message.QUERY_ERROR, language)
                        + "Different parameters with arrays have different sizes: "
                        + arraySize
                        + "!="
                        + columnValues[whichCol].size()
                        + "."));

        } else {
          // deal with single value: name=value
          if (tValue.startsWith("\"") && tValue.endsWith("\"")) tValue = String2.fromJson(tValue);
          StringArray sa =
              new StringArray(
                  // do it this way to deal with quotes, special chars, etc.
                  StringArray.arrayFromCSV(tValue, ",", false)); // trim?
          if (sa.size() > 1)
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.get(Message.QUERY_ERROR, 0)
                        + "One value (not "
                        + sa.size()
                        + ") expected for columnName="
                        + tName
                        + ". (missing [ ] ?)",
                    EDStatic.messages.get(Message.QUERY_ERROR, language)
                        + "One value (not "
                        + sa.size()
                        + ") expected for columnName="
                        + tName
                        + ". (missing [ ] ?)"));
          if (sa.size() == 0) sa.add("");
          columnValues[whichCol] =
              PrimitiveArray.factory(
                  columnPATypes[whichCol], sa); // does nothing if desired class is String

          // if (columnPATypes[whichCol] == PAType.STRING &&
          //    (tValue.length() < 2 ||
          //     tValue.charAt(0) != '"' ||
          //     tValue.charAt(tValue.length() - 1) != '"'))
          //    throw new SimpleException(EDStatic.simpleBilingual(language,
          // EDStatic.messages.queryErrorAr)
          // +
          //        "The String value for columnName=" + tName + " must start and end with \"'s.");
        }

        // ensure required var has valid value
        if (whichRC >= 0) {
          PrimitiveArray pa = columnValues[whichCol];
          if (pa.size() == 0 || pa.getString(0).length() == 0) // string="" number=NaN
          throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "requiredVariable="
                    + tName
                    + " must have a valid value.");
        }
      }
    }

    // ensure required parameters were specified
    if (author == null)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.get(Message.QUERY_ERROR, 0) + "author= was not specified.",
              EDStatic.messages.get(Message.QUERY_ERROR, language) + "author= was not specified."));
    int notFound = requiredVariablesFound.nextClearBit(0);
    if (notFound < requiredVariableNames.length)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.get(Message.QUERY_ERROR, 0)
                  + "requiredVariableName="
                  + requiredVariableNames[notFound]
                  + " wasn't specified.",
              EDStatic.messages.get(Message.QUERY_ERROR, language)
                  + "requiredVariableName="
                  + requiredVariableNames[notFound]
                  + " wasn't specified."));

    // make all columnValues the same size
    int maxSize = Math.max(1, arraySize);
    for (int col = 0; col < nColumns; col++) {
      PrimitiveArray pa = columnValues[col];

      // If this var wasn't in the command, so use mv's
      if (pa == null)
        pa =
            columnValues[col] =
                PrimitiveArray.factory(
                    columnPATypes[col], maxSize, ""); // if strings, "" is already UTF-8

      // duplicate scalar n=maxSize times
      if (pa.size() == 1 && maxSize > 1)
        columnValues[col] = PrimitiveArray.factory(columnPATypes[col], maxSize, pa.getString(0));
    }

    // figure out the fullFileName for each row
    StringArray fullFileNames = new StringArray(maxSize, false);
    for (int row = 0; row < maxSize; row++) {
      // figure out the epochSeconds time value
      double tTime =
          timeColumn < 0
              ? Double.NaN
              : // no time column
              timeBaseAndFactor == null
                  ? Calendar2.parseToEpochSeconds(
                      columnValues[timeColumn].getString(row), timeFormat)
                  : Calendar2.unitsSinceToEpochSeconds( // numeric time
                      timeBaseAndFactor[0],
                      timeBaseAndFactor[1],
                      columnValues[timeColumn].getDouble(row));

      fullFileNames.add(
          whichFile(
              startDir,
              tDirStructureColumnNames,
              tDirStructureNs,
              tDirStructureCalendars,
              columnNames,
              columnValues,
              row,
              tTime));
    }

    // EVERYTHING SHOULD BE VALIDATED BY NOW. NO ERRORS AFTER HERE!
    // append each input row to the appropriate file
    int row = 0;
    ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
    while (row < maxSize) {
      // figure out which file
      // EFFICIENT: Code below handles all rows that use this fullFileName.
      String fullFileName = fullFileNames.get(row);
      // String2.log(">> writing to " + fullFileName);

      // figure out which rows go to this fullFileName
      int startRow = row++;
      while (row < maxSize && fullFileNames.get(row).equals(fullFileName)) row++;
      int stopRow = row; // 1 past end

      // connect info for this file
      baos.reset();
      boolean fileIsNew = false;

      try {

        if (!File2.isFile(fullFileName)) {
          // if file doesn't exist, create it
          fileIsNew = true; // first
          File2.makeDirectory(File2.getDirectory(fullFileName)); // throws exception if trouble
        }
        Writer writer = File2.getBufferedWriterUtf8(baos);

        if (fileIsNew) {
          // write the column names to the writer
          boolean somethingWritten = false;
          for (int col = 0; col < nColumns; col++) {
            if (!columnIsFixed[col]) {
              writer.write(somethingWritten ? ',' : '[');
              writer.write(String2.toJson(columnNames[col]));
              somethingWritten = true;
            }
          }
          writer.write("]\n");
        }

        // write the data to the writer
        for (int tRow = startRow; tRow < stopRow; tRow++) {
          boolean somethingWritten = false;
          for (int col = 0; col < nColumns; col++) {
            if (!columnIsFixed[col]) {
              writer.write(somethingWritten ? ',' : '[');
              writer.write(columnValues[col].getJsonString(tRow));
              somethingWritten = true;
            }
          }
          writer.write("]\n");
        }

        // prepare to write everything to the file
        writer.flush(); // should do nothing because already done
        byte bar[] = baos.toByteArray();

        // As much as possible has been done ahead of time
        //  so write info to file is 1 blast
        // synchronized is ESSENTIAL: avoid problems with 2+ threads
        //  writing or reading same file at same time
        // synchronized is ESSENTIAL: fullFileName is canonical
        //  (since from StringArray) so same object in different threads
        // There were rare problems when writing to file with 4+ threads
        //  before switching to this system of full prep, then full write.
        fullFileName = String2.canonical(fullFileName);
        ReentrantLock lock = String2.canonicalLock(fullFileName);
        if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
          throw new TimeoutException(
              "Timeout waiting for lock on fullFileName in EDDTableFromHttpGet.");
        try {
          BufferedOutputStream fos =
              new BufferedOutputStream(new FileOutputStream(fullFileName, !fileIsNew)); // append?
          try {
            fos.write(bar, 0, bar.length); // entire write in 1 low level command
            fos.close(); // explicitly now, not by finalize() at some time in future
          } catch (Exception e) {
            try {
              fos.close();
            } catch (Exception e2) {
            }
            String2.log(
                String2.ERROR
                    + " in EDDTableFromHttpGet while writing to "
                    + fullFileName
                    + ":\n"
                    + MustBe.throwableToString(e));
            throw e;
          }
        } finally {
          lock.unlock();
        }

        EDDTableFromFiles.updateFileTableWithStats(
            fileTable,
            fullFileName,
            dirTable,
            nColumns,
            columnIsFixed,
            columnNames,
            columnPATypes,
            columnMvFv,
            columnValues,
            startRow,
            stopRow);

      } catch (Throwable t) {
        if (fileIsNew) File2.delete(fullFileName);
        String2.log(
            String2.ERROR
                + " while "
                + (fileIsNew ? "creating" : "adding to")
                + " "
                + fullFileName);
        throw t;
      }
    }

    // Don't ever change any of this (except adding something new to the end).
    // Clients rely on it.
    return "{\n"
        + "\"status\":\"success\",\n"
        + "\"nRowsReceived\":"
        + maxSize
        + ",\n"
        + "\"stringTimestamp\":\""
        + Calendar2.epochSecondsToIsoStringT3Z(timestampSeconds)
        + "\",\n"
        + "\"numericTimestamp\":"
        + timestampSeconds
        + "\n"
        + "}\n";
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromHttpGet. The XML can then
   * be edited by hand and added to the datasets.xml file.
   *
   * <p>This can't be made into a web service because it would allow any user to looks at (possibly)
   * private .nc files on the server.
   *
   * @param tFileDir the starting (parent) directory for searching for files
   * @param sampleFileName the full file name of one of the files in the collection
   * @param tHttpGetRequiredVariables
   * @param tHttpGetDirectoryStructure
   * @param tHttpGetKeys
   * @param tInfoUrl or "" if in externalAddGlobalAttributes or if not available
   * @param tInstitution or "" if in externalAddGlobalAttributes or if not available
   * @param tSummary or "" if in externalAddGlobalAttributes or if not available
   * @param tTitle or "" if in externalAddGlobalAttributes or if not available
   * @param externalAddGlobalAttributes These attributes are given priority. Use null in none
   *     available.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      int language,
      String tFileDir,
      String sampleFileName,
      String tHttpGetRequiredVariables,
      String tHttpGetDirectoryStructure,
      String tHttpGetKeys,
      String tInfoUrl,
      String tInstitution,
      String tSummary,
      String tTitle,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromHttpGet.generateDatasetsXml"
            + "\nfileDir="
            + tFileDir
            + "\nsampleFileName="
            + sampleFileName);

    String tFileNameRegex = ".*\\.jsonl";
    int tReloadEveryNMinutes = 1440;

    if (!String2.isSomething(tFileDir))
      throw new IllegalArgumentException("fileDir wasn't specified.");
    tFileDir = File2.addSlash(tFileDir); // ensure it has trailing slash
    // tSortedColumnSourceName = String2.isSomething(tSortedColumnSourceName)?
    //    tSortedColumnSourceName.trim() : "";
    if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440;
    if (!String2.isSomething(sampleFileName))
      String2.log(
          "Found/using sampleFileName="
              + (sampleFileName =
                  FileVisitorDNLS.getSampleFileName(
                      tFileDir, tFileNameRegex, true, ".*"))); // recursive, pathRegex

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    dataSourceTable.readJsonlCSV(sampleFileName, null, null, true); // read all and simplify
    // EDDTableFromHttpGet doesn't support standardizeWhat.
    int tnCol = dataSourceTable.nColumns();

    // 3 required columns: TIMESTAMP, AUTHOR, COMMAND
    for (int i = 0; i < 3; i++) {
      int which = dataSourceTable.findColumnNumber(SPECIAL_VAR_NAMES.get(i));
      if (which < 0)
        throw new SimpleException(
            "One of the variables must have the name \"" + SPECIAL_VAR_NAMES.get(which) + "\".");
    }

    Table dataAddTable = new Table();
    for (int c = 0; c < tnCol; c++) {
      String colName = dataSourceTable.getColumnName(c);
      PrimitiveArray sourcePA = dataSourceTable.getColumn(c);
      Attributes sourceAtts = dataSourceTable.columnAttributes(c);
      Attributes destAtts = new Attributes();
      PrimitiveArray destPA;
      // String2.log(">> colName=" + colName + " sourcePAType=" + sourcePA.elementType());
      if (colName.equals("time")) {
        if (sourcePA.elementType() == PAType.STRING) {
          String tFormat =
              Calendar2.suggestDateTimeFormat(
                  sourcePA, true); // evenIfPurelyNumeric?   true since String data
          destAtts.add(
              "units",
              tFormat.length() > 0
                  ? tFormat
                  : "yyyy-MM-dd'T'HH:mm:ss'Z'"); // default, so valid, so var name remains 'time'
          destPA = new StringArray(sourcePA);
        } else {
          destAtts.add("units", Calendar2.SECONDS_SINCE_1970); // a guess
          destPA = new DoubleArray(sourcePA);
        }
      } else if (colName.equals("latitude")) {
        destAtts.add("units", "degrees_north");
        destPA = new DoubleArray(sourcePA);
      } else if (colName.equals("longitude")) {
        destAtts.add("units", "degrees_east");
        destPA = new DoubleArray(sourcePA);
      } else if (colName.equals("depth")) {
        destAtts.add("units", "m");
        destPA = new DoubleArray(sourcePA);
      } else if (colName.equals("altitude")) {
        destAtts.add("units", "m");
        destPA = new DoubleArray(sourcePA);
      } else if (colName.equals("timestamp")) {
        destAtts.add(
            "comment",
            EDStatic.messages.EDDTableFromHttpGetTimestampDescription
                + " "
                + EDStatic.messages.get(Message.NOTE, 0)
                + " "
                + EDStatic.messages.EDDTableFromHttpGetDatasetDescription);
        destAtts.add("units", Calendar2.SECONDS_SINCE_1970);
        destAtts.add("time_precision", "1970-01-01T00:00:00.000Z");
        destPA = new DoubleArray(sourcePA);
      } else if (colName.equals("author")) {
        destAtts.add("comment", EDStatic.messages.EDDTableFromHttpGetAuthorDescription);
        destAtts.add("ioos_category", "Identifier");
        destPA = new StringArray(sourcePA);
      } else if (colName.equals("command")) {
        destAtts.add("comment", EDStatic.messages.EDDTableFromHttpGetDatasetDescription);
        destAtts.add("flag_values", new byte[] {0, 1});
        destAtts.add("flag_meanings", "insert delete");
        destAtts.add("ioos_category", "Other");
        destPA = new ByteArray(sourcePA);
      } else if (sourcePA.elementType() == PAType.STRING) {
        destPA = new StringArray(sourcePA);
      } else { // non-StringArray
        destAtts.add("units", "_placeholder");
        destPA = (PrimitiveArray) sourcePA.clone();
      }

      if (destPA.elementType() != PAType.STRING)
        destAtts.add(
            "missing_value",
            PrimitiveArray.factory(destPA.elementType(), 1, "" + destPA.missingValue()));

      // String2.log(">> in  colName= " + colName + " type=" + sourcePA.elementTypeString() + "
      // units=" + destAtts.get("units"));
      destAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              dataSourceTable.globalAttributes(),
              sourceAtts,
              destAtts,
              colName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              false); // tryToFindLLAT
      // String2.log(">> out colName= " + colName + " units=" + destAtts.get("units"));

      if ("_placeholder".equals(destAtts.getString("units"))) destAtts.add("units", "???");
      dataAddTable.addColumn(c, colName, destPA, destAtts);

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(colName, destPA, sourceAtts, destAtts);
    }
    // String2.log(">> SOURCE COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());
    // String2.log(">> DEST   COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());

    // globalAttributes
    if (externalAddGlobalAttributes == null) externalAddGlobalAttributes = new Attributes();
    if (String2.isSomething(tHttpGetRequiredVariables))
      externalAddGlobalAttributes.add(HTTP_GET_REQUIRED_VARIABLES, tHttpGetRequiredVariables);
    if (String2.isSomething(tHttpGetDirectoryStructure))
      externalAddGlobalAttributes.add(HTTP_GET_DIRECTORY_STRUCTURE, tHttpGetDirectoryStructure);
    if (String2.isSomething(tHttpGetKeys))
      externalAddGlobalAttributes.add(HTTP_GET_KEYS, tHttpGetKeys);
    if (String2.isSomething(tInfoUrl)) externalAddGlobalAttributes.add("infoUrl", tInfoUrl);
    if (String2.isSomething(tInstitution))
      externalAddGlobalAttributes.add("institution", tInstitution);
    if (String2.isSomething(tSummary)) externalAddGlobalAttributes.add("summary", tSummary);
    if (String2.isSomething(tTitle)) externalAddGlobalAttributes.add("title", tTitle);
    externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");

    // after dataVariables known, add global attributes in the dataAddTable
    Attributes addGlobalAtts = dataAddTable.globalAttributes();
    addGlobalAtts.set(
        makeReadyToUseAddGlobalAttributesForDatasetsXml(
            dataSourceTable.globalAttributes(),
            // another cdm_data_type could be better; this is ok
            hasLonLatTime(dataAddTable) ? "Point" : "Other",
            tFileDir,
            externalAddGlobalAttributes,
            suggestKeywords(dataSourceTable, dataAddTable)));

    String ttSummary =
        getAddOrSourceAtt(addGlobalAtts, dataSourceTable.globalAttributes(), "summary", "");
    addGlobalAtts.set(
        "summary",
        String2.ifSomethingConcat(
            ttSummary,
            "\n\n",
            EDStatic.messages.get(Message.NOTE, 0)
                + " "
                + EDStatic.messages.EDDTableFromHttpGetDatasetDescription));
    if (String2.isSomething(tHttpGetRequiredVariables)) {
      StringArray sa = StringArray.fromCSV(tHttpGetRequiredVariables);
      if (sa.size() > 0) addGlobalAtts.add("subsetVariables", sa.get(0));
    } else {
      addGlobalAtts.add(HTTP_GET_REQUIRED_VARIABLES, "??? e.g., stationID, time");
    }
    if (!String2.isSomething(tHttpGetDirectoryStructure))
      addGlobalAtts.add(HTTP_GET_DIRECTORY_STRUCTURE, "??? e.g., stationID/2months");
    if (!String2.isSomething(tHttpGetKeys))
      addGlobalAtts.add(HTTP_GET_KEYS, "??? a CSV list of author_key");

    addGlobalAtts.add("testOutOfDate", "now-1day");

    // write the information

    String sb =
        "<!-- NOTE! Since JSON Lines CSV files have no metadata, you MUST edit the chunk\n"
            + "  of datasets.xml below to add all of the metadata (especially \"units\"). -->\n"
            + "<dataset type=\"EDDTableFromHttpGet\" datasetID=\""
            + suggestDatasetID(
                tFileDir
                    + // dirs can't be made public
                    String2.replaceAll(tFileNameRegex, '\\', '|')
                    + // so escape chars not treated as subdirs
                    "_EDDTableFromHttpGet")
            + // so different dataset types -> different md5
            "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>-1</updateEveryNMillis>\n"
            + "    <fileDir>"
            + XML.encodeAsXML(tFileDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(tFileNameRegex)
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            +
            // "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n"
            // +
            // "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) +
            // "</postExtractRegex>\n" +
            // "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            // "    <columnNameForExtract>" + XML.encodeAsXML(tColumnNameForExtract) +
            // "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName></sortedColumnSourceName>\n"
            + // always nothing
            "    <sortFilesBySourceNames>"
            + (String2.isSomething(tHttpGetRequiredVariables)
                ? XML.encodeAsXML(tHttpGetRequiredVariables)
                : "???")
            + "</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + // safer. good for all except super frequent updates
            "    <accessibleViaFiles>true</accessibleViaFiles>\n"
            + writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    ")
            + cdmSuggestion()
            + writeAttsForDatasetsXml(true, dataAddTable.globalAttributes(), "    ")
            + writeVariablesForDatasetsXml(
                dataSourceTable, dataAddTable, "dataVariable", true, false)
            + // includeDataType, questionDestinationName
            """
                      </dataset>

                      """;

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb;
  }
}
