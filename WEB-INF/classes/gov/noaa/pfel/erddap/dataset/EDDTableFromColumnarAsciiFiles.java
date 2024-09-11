/*
 * EDDTableFromColumnarAsciiFiles Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.PAType;
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
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class represents a table of data from a collection of Columnar / Fixed Length / Fixed Format
 * ASCII data files. I.e., each data variable is stored in a specific, fixed substring of each row.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2014-11-07
 */
public class EDDTableFromColumnarAsciiFiles extends EDDTableFromFiles {

  /** Used to ensure that all non-axis variables in all files have the same leftmost dimension. */
  // protected String dim0Name = null;

  /**
   * This returns the default value for standardizeWhat for this subclass. See
   * Attributes.unpackVariable for options. The default was chosen to mimic the subclass' behavior
   * from before support for standardizeWhat options was added.
   */
  @Override
  public int defaultStandardizeWhat() {
    return DEFAULT_STANDARDIZEWHAT;
  }

  public static int DEFAULT_STANDARDIZEWHAT = 0;

  /**
   * The constructor.
   *
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   *     <p>The sortedColumnSourceName isn't utilized.
   */
  public EDDTableFromColumnarAsciiFiles(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      Attributes tAddGlobalAttributes,
      Object[][] tDataVariables,
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
        "EDDTableFromColumnarAsciiFiles",
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
  }

  /**
   * This gets source data from one file. See documentation in EDDTableFromFiles.
   *
   * @throws an exception if too much data. This won't throw an exception if no data.
   */
  @Override
  public Table lowGetSourceDataFromFile(
      String tFileDir,
      String tFileName,
      StringArray sourceDataNames,
      String sourceDataTypes[],
      double sortedSpacing,
      double minSorted,
      double maxSorted,
      StringArray sourceConVars,
      StringArray sourceConOps,
      StringArray sourceConValues,
      boolean getMetadata,
      boolean mustGetData)
      throws Throwable {

    if (!mustGetData)
      // Just return a table with columns but no rows. There is never any metadata.
      return Table.makeEmptyTable(sourceDataNames.toArray(), sourceDataTypes);

    // gather the info needed to read the file
    String tLoadCol[] = sourceDataNames.toArray();
    int nCol = tLoadCol.length;
    int tStartColumn[] = new int[nCol];
    int tStopColumn[] = new int[nCol];
    PAType tColPAType[] = new PAType[nCol];
    String sourceNames[] = dataVariableSourceNames();
    // String2.log(">> sourceDataTypes=" + String2.toCSSVString(sourceDataTypes));
    for (int col = 0; col < nCol; col++) {
      int dv = String2.indexOf(sourceNames, tLoadCol[col]);
      if (dv < 0)
        throw new SimpleException(
            "sourceName=" + tLoadCol[col] + " not found in " + String2.toCSSVString(sourceNames));
      tStartColumn[col] = startColumn[dv];
      tStopColumn[col] = stopColumn[dv];
      tColPAType[col] =
          sourceDataTypes[col].equals("boolean")
              ? PAType.BOOLEAN
              : PAType.fromCohortString(sourceDataTypes[col]);
    }

    Table table = new Table();
    table.readColumnarASCIIFile(
        tFileDir + tFileName,
        charset,
        skipHeaderToRegex,
        skipLinesRegex,
        firstDataRow - 1,
        tLoadCol,
        tStartColumn,
        tStopColumn,
        tColPAType);
    // String2.log(">> lowGetSourceData:\n" + table.dataToString(5));

    // unpack
    table.standardize(standardizeWhat);

    return table;
  }

  /**
   * This makes a guess at the columnNames, start (character) column (0..), and stop (character)
   * column (exclusive, 0..) in a columnar ASCII data file. It assumes column names on the first row
   * and data on subsequent rows.
   *
   * @param sampleFileName
   * @param charset ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
   * @param columnNamesRow first row of file is called 1.
   * @param firstDataRow first row of file is called 1.
   * @param colNames which will receive the columnNames
   * @param start which will receive the start character number (0..) for each column
   * @param stop which will receive the exclusive stop character number (0..) for each column
   * @throws Exception if trouble
   */
  public static void getColumnInfo(
      String sampleFileName,
      String charset,
      int columnNamesRow,
      int firstDataRow,
      StringArray colNames,
      IntArray start,
      IntArray stop)
      throws Exception {

    // read the lines of the sample file
    ArrayList<String> lines = File2.readLinesFromFile(sampleFileName, charset, 2);

    // hueristic: col with low usage then col with high usage (or vice versa)
    //  indicates new column
    int nLines = lines.size();
    int longest = 0;
    if (columnNamesRow >= 1) longest = lines.get(columnNamesRow - 1).length();
    for (int i = firstDataRow - 1; i < nLines; i++)
      longest = Math.max(longest, lines.get(i).length());
    longest++; // ensure at least one empty col at end
    int nCharsInCol[] = new int[longest];
    if (columnNamesRow >= 1) {
      String s = lines.get(columnNamesRow - 1);
      int len = s.length();
      for (int po = 0; po < len; po++) if (s.charAt(po) != ' ') nCharsInCol[po]++;
    }
    for (int i = firstDataRow - 1; i < nLines; i++) {
      String s = lines.get(i);
      int len = s.length();
      for (int po = 0; po < len; po++) if (s.charAt(po) != ' ') nCharsInCol[po]++;
    }
    // for (int po = 0; po < longest; po++)
    //    String2.log(po + " n=" + nCharsInCol[po]);
    int firstEmptyPo = longest - 1; // first empty col at end of line
    while (firstEmptyPo > 0 && nCharsInCol[firstEmptyPo - 1] == 0) firstEmptyPo--;
    int lowThresh = Math.max(2, (nLines - firstDataRow) / 10);
    int highThresh = (nLines - firstDataRow) / 2;
    int po = 0;
    start.add(po);
    while (po < firstEmptyPo) {

      // seek col > highThresh
      while (po < firstEmptyPo && nCharsInCol[po] < highThresh) po++;
      if (po == firstEmptyPo) {
        stop.add(longest);
        break;
      }

      // seek col <= lowThresh
      while (po < firstEmptyPo && nCharsInCol[po] > lowThresh) po++;
      // seek lowest point of columns < lowThresh
      int lowestPo = po;
      int lowestN = nCharsInCol[po];
      while (po <= firstEmptyPo && nCharsInCol[po] <= lowThresh) {
        if (nCharsInCol[po] <= lowestN) {
          lowestPo = po;
          lowestN = nCharsInCol[po];
        }
        po++;
      }
      stop.add(lowestPo + 1);
      if (lowestPo == firstEmptyPo) break;

      start.add(lowestPo + 1); // it has >0 chars
    }
    int nCols = start.size();

    // read column names
    String namesLine =
        columnNamesRow >= 1 && columnNamesRow < nLines ? lines.get(columnNamesRow - 1) : "";
    int namesLineLength = namesLine.length();
    for (int col = 0; col < nCols; col++) {
      String cn =
          start.get(col) < namesLineLength
              ? namesLine.substring(start.get(col), Math.min(stop.get(col), namesLineLength)).trim()
              : "";
      if (cn.length() == 0) cn = "column" + (col + 1);
      colNames.add(cn);
    }
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromColumnarAsciiFiles. The XML
   * can then be edited by hand and added to the datasets.xml file.
   *
   * <p>This can't be made into a web service because it would allow any user to look at (possibly)
   * private ascii files on the server.
   *
   * @param tFileDir the starting (parent) directory for searching for files
   * @param tFileNameRegex the regex that each filename (no directory info) must match (e.g.,
   *     ".*\\.nc") (usually only 1 backslash; 2 here since it is Java code).
   * @param sampleFileName one of the files in the collection
   * @param columnNamesRow first row of file is called 1.
   * @param firstDataRow first row if file is called 1.
   * @param tReloadEveryNMinutes
   * @param tPreExtractRegex part of info for extracting e.g., stationName from file name. Set to ""
   *     if not needed.
   * @param tPostExtractRegex part of info for extracting e.g., stationName from file name. Set to
   *     "" if not needed.
   * @param tExtractRegex part of info for extracting e.g., stationName from file name. Set to "" if
   *     not needed.
   * @param tColumnNameForExtract part of info for extracting e.g., stationName from file name. Set
   *     to "" if not needed. No: SortedColumnSourceName use "" if not known or not needed.
   * @param tSortFilesBySourceNames This is useful, because it ultimately determines default results
   *     order.
   * @param tInfoUrl or "" if in externalAddGlobalAttributes or if not available (but try hard!)
   * @param tInstitution or "" if in externalAddGlobalAttributes or if not available (but try hard!)
   * @param tSummary or "" if in externalAddGlobalAttributes or if not available (but try hard!)
   * @param tTitle or "" if in externalAddGlobalAttributes or if not available (but try hard!)
   * @param externalAddGlobalAttributes These attributes are given priority. Use null in none
   *     available.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String tFileDir,
      String tFileNameRegex,
      String sampleFileName,
      String charset,
      int columnNamesRow,
      int firstDataRow,
      int tReloadEveryNMinutes,
      String tPreExtractRegex,
      String tPostExtractRegex,
      String tExtractRegex,
      String tColumnNameForExtract, // String tSortedColumnSourceName,
      String tSortFilesBySourceNames,
      String tInfoUrl,
      String tInstitution,
      String tSummary,
      String tTitle,
      int tStandardizeWhat,
      String tCacheFromUrl,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromColumnarAsciiFiles.generateDatasetsXml"
            + "\nsampleFileName="
            + sampleFileName
            + "\ncharset="
            + charset
            + " columnNamesRow="
            + columnNamesRow
            + " firstDataRow="
            + firstDataRow
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\nextract pre="
            + tPreExtractRegex
            + " post="
            + tPostExtractRegex
            + " regex="
            + tExtractRegex
            + " colName="
            + tColumnNameForExtract
            + "\nsortFilesBy="
            + tSortFilesBySourceNames
            + "\ninfoUrl="
            + tInfoUrl
            + "\ninstitution="
            + tInstitution
            + "\nsummary="
            + tSummary
            + "\ntitle="
            + tTitle
            + "\nexternalAddGlobalAttributes="
            + externalAddGlobalAttributes);
    if (!String2.isSomething(tFileDir))
      throw new IllegalArgumentException("fileDir wasn't specified.");
    tFileDir = File2.addSlash(tFileDir); // ensure it has trailing slash
    tFileNameRegex = String2.isSomething(tFileNameRegex) ? tFileNameRegex.trim() : ".*";
    if (String2.isRemote(tCacheFromUrl))
      FileVisitorDNLS.sync(
          tCacheFromUrl, tFileDir, tFileNameRegex, true, ".*", false); // not fullSync
    firstDataRow = Math.max(1, firstDataRow); // 1..
    if (charset == null || charset.length() == 0) charset = File2.ISO_8859_1;
    tColumnNameForExtract =
        String2.isSomething(tColumnNameForExtract) ? tColumnNameForExtract.trim() : "";
    // tSortedColumnSourceName = String2.isSomething(tSortedColumnSourceName)?
    //    tSortedColumnSourceName.trim() : "";
    if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440; // 1440 works well with suggestedUpdateEveryNMillis
    if (!String2.isSomething(sampleFileName))
      String2.log(
          "Found/using sampleFileName="
              + (sampleFileName =
                  FileVisitorDNLS.getSampleFileName(
                      tFileDir, tFileNameRegex, true, ".*"))); // recursive, pathRegex

    // get info from the sampleFile
    if (charset == null || charset.length() == 0) charset = File2.ISO_8859_1;
    StringArray colNames = new StringArray();
    IntArray start = new IntArray();
    IntArray stop = new IntArray();
    getColumnInfo(sampleFileName, charset, columnNamesRow, firstDataRow, colNames, start, stop);

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();
    dataSourceTable.readColumnarASCIIFile(
        sampleFileName,
        charset,
        "",
        "", // skipHeaderToRegex, skipLinesRegex,
        firstDataRow - 1,
        colNames.toArray(),
        start.toArray(),
        stop.toArray(),
        null); // null = simplify

    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;
    dataSourceTable.standardize(tStandardizeWhat);

    // globalAttributes
    if (externalAddGlobalAttributes == null) externalAddGlobalAttributes = new Attributes();
    if (tInfoUrl != null && tInfoUrl.length() > 0)
      externalAddGlobalAttributes.add("infoUrl", tInfoUrl);
    if (tInstitution != null && tInstitution.length() > 0)
      externalAddGlobalAttributes.add("institution", tInstitution);
    if (tSummary != null && tSummary.length() > 0)
      externalAddGlobalAttributes.add("summary", tSummary);
    if (tTitle != null && tTitle.length() > 0) externalAddGlobalAttributes.add("title", tTitle);
    externalAddGlobalAttributes.setIfNotAlreadySet(
        "sourceUrl", "(" + (String2.isTrulyRemote(tFileDir) ? "remote" : "local") + " files)");
    // externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");

    boolean dateTimeAlreadyFound = false;
    double maxTimeES = Double.NaN;
    for (int col = 0; col < dataSourceTable.nColumns(); col++) {
      String colName = dataSourceTable.getColumnName(col);
      PrimitiveArray sourcePA =
          (PrimitiveArray)
              dataSourceTable.getColumn(col).clone(); // clone because going into addTable

      Attributes sourceAtts = dataSourceTable.columnAttributes(col);
      Attributes addAtts = new Attributes();

      // dateTime?
      boolean isDateTime = false;
      if (sourcePA instanceof StringArray sa) {
        String dtFormat = Calendar2.suggestDateTimeFormat(sa, false); // evenIfPurelyNumeric
        if (dtFormat.length() > 0) {
          isDateTime = true;
          addAtts.set("units", dtFormat);
        }

        if (!Double.isFinite(maxTimeES) && Calendar2.isTimeUnits(dtFormat))
          maxTimeES =
              Calendar2.tryToEpochSeconds(
                  sourcePA.getString(sourcePA.size() - 1)); // NaN if trouble
      }

      PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);

      addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              null, // no source global attributes
              sourceAtts,
              addAtts,
              colName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true); // tryToFindLLAT
      addAtts.add("startColumn", start.get(col));
      addAtts.add("stopColumn", stop.get(col));

      // add to dataAddTable
      dataAddTable.addColumn(col, colName, destPA, addAtts);

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(colName, destPA, sourceAtts, addAtts);

      // files are likely sorted by first date time variable
      // and no harm if files aren't sorted that way
      // if (tSortedColumnSourceName.length() == 0 &&
      //    isDateTime && !dateTimeAlreadyFound) {
      //    dateTimeAlreadyFound = true;
      //    tSortedColumnSourceName = colName;
      // }
    }

    // add the columnNameForExtract variable
    if (tColumnNameForExtract.length() > 0) {
      Attributes atts = new Attributes();
      atts.add("ioos_category", "Identifier");
      atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
      // no units or standard_name
      dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
      dataAddTable.addColumn(0, tColumnNameForExtract, new StringArray(), atts);
    }

    // tryToFindLLAT
    tryToFindLLAT(dataSourceTable, dataAddTable);

    // use maxTimeES
    String tTestOutOfDate =
        EDD.getAddOrSourceAtt(
            dataSourceTable.globalAttributes(),
            dataAddTable.globalAttributes(),
            "testOutOfDate",
            null);
    if (Double.isFinite(maxTimeES) && !String2.isSomething(tTestOutOfDate)) {
      tTestOutOfDate = suggestTestOutOfDate(maxTimeES);
      if (String2.isSomething(tTestOutOfDate))
        dataAddTable.globalAttributes().set("testOutOfDate", tTestOutOfDate);
    }

    // after dataVariables known, add global attributes in the dataAddTable
    dataAddTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(),
                // another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable) ? "Point" : "Other",
                tFileDir,
                externalAddGlobalAttributes,
                suggestKeywords(dataSourceTable, dataAddTable)));

    // subsetVariables
    if (dataSourceTable.globalAttributes().getString("subsetVariables") == null
        && dataAddTable.globalAttributes().getString("subsetVariables") == null)
      dataAddTable
          .globalAttributes()
          .add("subsetVariables", suggestSubsetVariables(dataSourceTable, dataAddTable, false));

    // write the information
    StringBuilder sb = new StringBuilder();
    if (tSortFilesBySourceNames.length() == 0) tSortFilesBySourceNames = tColumnNameForExtract;
    sb.append(
        "<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n"
            + "  below, notably 'units' for each of the dataVariables. -->\n"
            + "<dataset type=\"EDDTableFromColumnarAsciiFiles\" datasetID=\""
            + suggestDatasetID(tFileDir + tFileNameRegex)
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + (String2.isUrl(tCacheFromUrl)
                ? "    <cacheFromUrl>" + XML.encodeAsXML(tCacheFromUrl) + "</cacheFromUrl>\n"
                : "    <updateEveryNMillis>"
                    + suggestUpdateEveryNMillis(tFileDir)
                    + "</updateEveryNMillis>\n")
            + "    <fileDir>"
            + XML.encodeAsXML(tFileDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(tFileNameRegex)
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <charset>"
            + charset
            + "</charset>\n"
            + "    <columnNamesRow>"
            + columnNamesRow
            + "</columnNamesRow>\n"
            + "    <firstDataRow>"
            + firstDataRow
            + "</firstDataRow>\n"
            + "    <standardizeWhat>"
            + tStandardizeWhat
            + "</standardizeWhat>\n"
            + (String2.isSomething(tColumnNameForExtract)
                ? // Discourage Extract. Encourage sourceName=***fileName,...
                "    <preExtractRegex>"
                    + XML.encodeAsXML(tPreExtractRegex)
                    + "</preExtractRegex>\n"
                    + "    <postExtractRegex>"
                    + XML.encodeAsXML(tPostExtractRegex)
                    + "</postExtractRegex>\n"
                    + "    <extractRegex>"
                    + XML.encodeAsXML(tExtractRegex)
                    + "</extractRegex>\n"
                    + "    <columnNameForExtract>"
                    + XML.encodeAsXML(tColumnNameForExtract)
                    + "</columnNameForExtract>\n"
                : "")
            +
            // "    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) +
            // "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>"
            + XML.encodeAsXML(tSortFilesBySourceNames)
            + "</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n");
    sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
    sb.append(cdmSuggestion());
    sb.append(writeAttsForDatasetsXml(true, dataAddTable.globalAttributes(), "    "));

    // last 2 params: includeDataType, questionDestinationName
    sb.append(
        writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true, false));
    sb.append("</dataset>\n" + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }

  /** special version of String2.isSomething */
  private static boolean emlIsSomething(String content) {
    return String2.isSomething(content) && !"NA".equals(content) && !"NULL".equals(content);
  }

  /**
   * This is like generateDatasetsXmlFromEML, but works on a batch of EML files.
   *
   * @param startDir e.g.,
   *     https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/ 2020-01-08 was
   *     http://sbc.lternet.edu/data/eml/files/
   * @param emlFileNameRegex e.g., "knb-lter-sbc\\.\\d+",
   */
  public static String generateDatasetsXmlFromEMLBatch(
      String emlDir,
      String startUrl,
      String emlFileNameRegex,
      boolean useLocalFilesIfPresent,
      String tAccessibleTo,
      String localTimeZone,
      int tStandardizeWhat)
      throws Throwable {

    boolean pauseForErrors = false;
    String resultsFileName =
        EDStatic.fullLogsDirectory
            + "fromEML_"
            + Calendar2.getCompactCurrentISODateTimeStringLocal()
            + ".log";
    String2.log(
        "\n*** generateDatasetsXmlFromEMLBatch\n"
            + "The results will also be in "
            + resultsFileName);
    emlDir = File2.addSlash(emlDir);
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;

    Table table =
        FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
            startUrl,
            emlFileNameRegex, // "knb-lter-sbc\\.\\d+",
            false,
            ".*",
            false); // tRecursive, tPathRegex, tDirectoriesToo
    StringArray names = (StringArray) table.getColumn(FileVisitorDNLS.NAME);

    // String2.log("names=\n" + names.toString());
    StringBuilder results = new StringBuilder();
    for (int i = 0; i < names.size(); i++) {
      // if (names.get(i).compareTo("knb-lter-sbc.59") < 0)
      //    continue;

      if (false) {
        // just download the files
        SSR.downloadFile(
            startUrl + names.get(i),
            emlDir + names.get(i),
            true); // tryToUseCompression; throws Exception

      } else {
        String result =
            generateDatasetsXmlFromEML(
                    pauseForErrors,
                    emlDir,
                    startUrl + names.get(i),
                    useLocalFilesIfPresent,
                    tAccessibleTo,
                    localTimeZone,
                    tStandardizeWhat)
                + "\n"; // standardizeWhat
        results.append(result);
        File2.appendFileUtf8(resultsFileName, result);
        String2.log(result);
      }
    }

    String2.log(
        "\n*** generateDatasetsXmlFromEMLBatch finished successfully.\n"
            + "The results are also in "
            + resultsFileName);
    return results.toString();
  }

  /**
   * This generates one or more ready-to-use datasets.xml entries (1 per dataTable in the EML file)
   * for an EDDTableFromColumnarAsciiFiles or EDDTableFromAsciiFiles based on the information in an
   * Ecological Metadata Language (EML) file and usually also a sample data file in the same
   * directory (and referenced in the EML file). The XML can then be edited by hand and added to the
   * datasets.xml file.
   *
   * @param dir directory that has or will be used to store the EML file and data file.
   * @param emlFileName one of the files in the collection This can be a URL or just the file name
   *     of the file in the dir.
   * @param useLocalFilesIfPresent
   * @param tAccessibleTo may be null (public access), "" (no one access), or CSV of groups that
   *     have access (e.g., lterSbc),
   * @param localTimeZone is a time zone name from the TZ column at
   *     https://en.wikipedia.org/wiki/List_of_tz_database_time_zones which will be used whenever a
   *     column has "local" times.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   */
  public static String generateDatasetsXmlFromEML(
      boolean pauseForErrors,
      String emlDir,
      String emlFileName,
      boolean useLocalFilesIfPresent,
      String tAccessibleTo,
      String localTimeZone,
      int tStandardizeWhat) {

    emlDir = File2.addSlash(emlDir);
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;
    int whichDataTable = 1;
    StringBuilder results = new StringBuilder();
    while (true) {
      try {
        String result =
            generateDatasetsXmlFromEML(
                emlDir,
                emlFileName,
                whichDataTable++,
                useLocalFilesIfPresent,
                tAccessibleTo,
                localTimeZone,
                tStandardizeWhat);
        String2.log(result);
        results.append(result);
        results.append('\n');

      } catch (Throwable t) {
        try {
          String msg = MustBe.throwableToString(t);
          if (msg.indexOf("There is no <dataTable> #") >= 0) break;
          String result =
              "<!-- fromEML ERROR for "
                  + emlFileName
                  + " dataTable #"
                  + whichDataTable
                  + "\n"
                  + String2.replaceAll(msg, "--", " - - ")
                  + " -->\n\n";
          if (pauseForErrors) String2.pressEnterToContinue(result);
          else String2.log(result);

          results.append(result);
          if (msg.indexOf("Big ERROR:") >= 0
              || msg.indexOf("java.io.IOException: ERROR while downloading") >= 0) break;
        } catch (Throwable t2) {
          String2.log(MustBe.throwableToString(t2));
        }
      }
    }
    return results.toString();
  }

  /**
   * This is the underlying generateDatasetsXmlFromEML that just gets data from one of the
   * dataTables in the EML file. EML 2.1.1 documentation:
   * https://knb.ecoinformatics.org/external//emlparser/docs/eml-2.1.1/eml-attribute.html Info about
   * 2.0.1 to 2.1.0 transition: 2020-01-08 gone. Was
   * http://sbc.lternet.edu/external/InformationManagement/EML/docs/eml-2.1.0/eml-210info.html
   *
   * <p>See the documentation for this in /downloads/EDDTableFromEML.html.
   *
   * @param tAccessibleTo If null or "null", there will be no &lt;accessibleTo&gt; tag in the
   *     output.
   * @param localTimeZone is a time zone name from the TZ column at
   *     https://en.wikipedia.org/wiki/List_of_tz_database_time_zones which will be used whenever a
   *     column has "local" times.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned with a dataset or a comment which has an
   *     error message. If the requested dataTable doesn't exist, this throws an exception saying
   *     "There is no &lt;dataTable&gt; #...".
   */
  public static String generateDatasetsXmlFromEML(
      String emlDir,
      String emlFileName,
      int whichDataTable,
      boolean useLocalFilesIfPresent,
      String tAccessibleTo,
      String localTimeZone,
      int tStandardizeWhat)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromEML.generateDatasetsXmlFromEML "
            + "whichDataTable="
            + whichDataTable
            + "\ndir="
            + emlDir
            + "\nemlFileName="
            + emlFileName);
    if (!String2.isSomething(emlDir))
      throw new IllegalArgumentException("Big ERROR: emlDir wasn't specified.");
    emlDir = File2.addSlash(emlDir);
    File2.makeDirectory(emlDir);
    if (!String2.isSomething(emlFileName))
      throw new IllegalArgumentException("Big ERROR: emlFileName wasn't specified.");
    if (!String2.isSomething(localTimeZone)) localTimeZone = "";
    String charset = null; // for the sample data file
    String defaultDatafileCharset = File2.ISO_8859_1; // for the sample data file
    int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    Table addTable = new Table();
    Attributes addGlobalAtts = addTable.globalAttributes();
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;

    // if emlFileName is URL, download it
    // e.g.,
    if (String2.isUrl(emlFileName)) {
      String tName = File2.getNameAndExtension(emlFileName);
      if (useLocalFilesIfPresent && File2.isFile(emlDir + tName)) {
      } else {
        SSR.downloadFile(
            emlFileName, emlDir + tName, true); // tryToUseCompression; throws Exception
      }
      addGlobalAtts.set("infoUrl", emlFileName);
      addGlobalAtts.set("metadata_link", emlFileName);
      emlFileName = tName;
    }

    // keep info from higher-up tags to know where we are
    String accessPrincipal = "";
    String creatorType = "";
    String system = "";

    // gather/generate
    StringBuilder address = new StringBuilder();
    String altitudeUnits = "";
    StringBuilder boundingCoordinates = new StringBuilder("");
    StringBuilder coverage = new StringBuilder();
    String dataFileName = "";
    String dataFileDelimiter = "";
    String dataFileUrl = "";
    String datasetID =
        emlFileName.endsWith(".xml")
            ?
            // first guess here. Replaced below by <alternateIdentifier> if possible.
            File2.getNameNoExtension( // remove e.g., .28.xml  (version #?)
                File2.getNameNoExtension(emlFileName))
            : // needs cleanup to make safe
            emlFileName;
    datasetID = String2.simpleMatlabNameSafe(datasetID);
    datasetID += (datasetID.endsWith("_") ? "" : "_") + "t" + whichDataTable;
    int dataTablei = 0;
    StringBuilder individualName = new StringBuilder();
    HashSet<String> keywords = new HashSet();
    StringBuilder license = new StringBuilder();
    StringBuilder licenseOther = new StringBuilder("");
    StringBuilder methods = new StringBuilder();
    int methodNumber = 0;
    String methodsDescription = "";
    String methodsHeader = "";
    int numHeaderLines = 1; // sometimes 0
    double westernmost = Double.MAX_VALUE;
    double easternmost = -Double.MAX_VALUE;
    double southernmost = Double.MAX_VALUE;
    double northernmost = -Double.MAX_VALUE;
    double maxAltitude = -Double.MAX_VALUE;
    double minAltitude = Double.MAX_VALUE;
    int projectPerson = 0;
    String varName = "";
    String varLongName = "";
    StringBuilder varComment = new StringBuilder();
    String varType = "";
    String varUnits = "";
    String varTimePrecision = "";
    StringArray varMV = new StringArray();

    // parse the EML
    if (!File2.isFile(emlDir + emlFileName))
      throw new IllegalArgumentException(
          "Big ERROR: eml fileName=" + emlDir + emlFileName + " doesn't exist.");
    SimpleXMLReader xmlReader =
        new SimpleXMLReader(
            File2.getDecompressedBufferedInputStream(emlDir + emlFileName), "eml:eml");
    try {

      while (true) {
        xmlReader.nextTag();
        String tags = xmlReader.allTags();
        String tagsLC = tags.toLowerCase();
        if (tags.equals("</eml:eml>")) break;

        // some reusable code
        if (tags.endsWith("<individualName>")) {
          individualName.setLength(0);

        } else if (tags.endsWith("<individualName></salutation>")
            || tags.endsWith("<individualName></givenName>")
            || tags.endsWith("<individualName></surName>")) {
          // assume parts are in order. There is no perfect alternative.
          if (emlIsSomething(xmlReader.content()))
            String2.ifSomethingConcat(individualName, " ", xmlReader.content());

        } else if (tags.endsWith("<address>")) {
          address.setLength(0);

        } else if (tags.endsWith("<address></deliveryPoint>")
            || // 1+ instances
            tags.endsWith("<address></city>")
            || tags.endsWith("<address></administrativeArea>")
            || tags.endsWith("<address></postalCode>")
            || tags.endsWith("<address></country>")) {
          if (emlIsSomething(xmlReader.content()))
            String2.ifSomethingConcat(address, ", ", xmlReader.content());

          // access allow/deny
          // I'm not catching/dealing with <access order="allowFirst">
        } else if (tags.equals("<eml:eml><access><allow></principal>")) {
          accessPrincipal = xmlReader.content();

          // ??? OTHERS PROJECT NAMES?
          String program = xmlReader.content().indexOf("=LTER,") >= 0 ? "LTER" : "";
          if (program.length() > 0) {
            addGlobalAtts.add("program", "LTER");
            keywords.add("LTER");
          }

        } else if (tags.equals("<eml:eml><access><allow></permission>")) {
          // read, write, changePermission are listed separately
          license.append(
              "Metadata \""
                  + xmlReader.content()
                  + "\" access is allowed for principal=\""
                  + accessPrincipal
                  + "\".\n");

        } else if (tags.equals("<eml:eml><access><deny></principal>")) {
          accessPrincipal = xmlReader.content();

        } else if (tags.equals("<eml:eml><access><deny></permission>")) {
          // read, write, changePermission
          license.append(
              "Metadata \""
                  + xmlReader.content()
                  + "\" access is denied for principal=\""
                  + accessPrincipal
                  + "\".\n");

        } else if (tags.endsWith("<access></allow>") || tags.endsWith("<access></deny>")) {
          // this works for this access content and the content below
          accessPrincipal = "";

          // dataset
        } else if (tags.equals("<eml:eml><dataset><alternateIdentifier>")) {
          system = xmlReader.attributeValue("system");

        } else if (tags.equals("<eml:eml><dataset></alternateIdentifier>")
            && emlIsSomething(xmlReader.content())) {

          // doi or knb-lter-sbc.17
          // ??? other altID's to avoid?
          if (xmlReader.content().indexOf("/pasta/") >= 0) { // altID with doi URL
            addGlobalAtts.add("doi", xmlReader.content());
            if (String2.isSomething(system)) addGlobalAtts.add("doi_authority", system);

          } else {
            datasetID = xmlReader.content();

            // just a number?
            if (Double.isFinite(String2.parseDouble(datasetID))) {
              if (String2.isSomething(system)) datasetID = system + "_" + datasetID;
              else
                datasetID =
                    File2.getNameNoExtension(emlDir.substring(0, emlDir.length() - 1))
                        + "_"
                        + datasetID;
            }

            // cleanup to make safe
            datasetID = String2.simpleMatlabNameSafe(datasetID);

            // add whichDataTable?
            datasetID += (datasetID.endsWith("_") ? "" : "_") + "t" + whichDataTable;
            addGlobalAtts.add("id", datasetID);
          }

        } else if (tags.equals("<eml:eml><dataset></title>")) {
          // SBC LTER: Reef: Kelp Forest Community Dynamics: Fish abundance
          if (emlIsSomething(xmlReader.content())) addGlobalAtts.add("title", xmlReader.content());

          // dataset creator
        } else if (tags.equals("<eml:eml><dataset><creator></organizationName>")) {
          // Santa Barbara Coastal LTER
          if (emlIsSomething(addGlobalAtts.getString("institution"))) {
            creatorType = ""; // just get the first organization
          } else if (emlIsSomething(xmlReader.content())) {
            addGlobalAtts.add("institution", xmlReader.content());
            addGlobalAtts.add("publisher_name", xmlReader.content());
            addGlobalAtts.add("publisher_type", "institution");
            creatorType = "institution";
          }

        } else if (tags.equals("<eml:eml><dataset><creator></individualName>")) {
          // Daniel C Reed
          if (emlIsSomething(addGlobalAtts.getString("creator_name"))
              || !emlIsSomething(individualName.toString())) {
            creatorType = ""; // just get the first creator
          } else {
            addGlobalAtts.add("creator_name", individualName.toString().trim());
            addGlobalAtts.add("creator_type", "person");
            creatorType = "person";
          }

        } else if (tags.equals("<eml:eml><dataset><creator></address>")) {
          if (address.length() > 0) {
            if (creatorType.equals("institution"))
              addGlobalAtts.add("publisher_address", address.toString().trim());
            else if (creatorType.equals("person"))
              addGlobalAtts.add("creator_address", address.toString().trim());
          }

        } else if (tags.equals("<eml:eml><dataset><creator></electronicMailAddress>")) {
          if (emlIsSomething(xmlReader.content())) {
            if (creatorType.equals("institution"))
              addGlobalAtts.add("publisher_email", xmlReader.content());
            else if (creatorType.equals("person"))
              addGlobalAtts.add("creator_email", xmlReader.content());
          }

          // dataset pubDate
        } else if (tags.equals("<eml:eml><dataset></pubDate>")) {
          // 2014-09-03
          if (emlIsSomething(xmlReader.content()))
            addGlobalAtts.add("date_created", xmlReader.content());

          // dataset language
        } else if (tags.equals("<eml:eml><dataset></language>")) {
          // english
          if (emlIsSomething(xmlReader.content()))
            addGlobalAtts.add("language", xmlReader.content());

          // abstract
        } else if (tags.equals("<eml:eml><dataset><abstract>")) {

          String s = addGlobalAtts.getString("summary");
          addGlobalAtts.set(
              "summary", (s == null ? "" : s + "\n\n" + xmlReader.readDocBookAsPlainText()));

          // keywords
        } else if (tags.equals("<eml:eml><dataset><keywordSet></keyword>")) {
          chopUpAndAdd(xmlReader.content(), keywords);

          // intellectualRights
        } else if (tags.equals("<eml:eml><dataset><intellectualRights>")) {
          String s = xmlReader.readDocBookAsPlainText();
          // String2.pressEnterToContinue(">> intellectualRights=" + String2.annotatedString(s));
          if (s.startsWith("other\n\n")) s = s.substring(7);
          if (String2.isSomething(s)) s = "Intellectual Rights:\n" + s.trim();
          String2.ifSomethingConcat(licenseOther, "\n", s);

          // online distribute is too general, so not very useful
          // <distribution><online><url function="information">https://sbclter.msi.ucsb.edu/</url>

          // geographicCoverage
        } else if (tags.equals("<eml:eml><dataset><coverage><geographicCoverage>")) {
          String av = xmlReader.attributeValue("id");
          if (emlIsSomething(av)) coverage.append(av + ": ");

        } else if (tags.equals(
            "<eml:eml><dataset><coverage><geographicCoverage></geographicDescription>")) {
          if (emlIsSomething(xmlReader.content())) coverage.append(xmlReader.content());

        } else if (tags.startsWith(
            "<eml:eml><dataset><coverage><geographicCoverage><boundingCoordinates>")) {

          if (tags.endsWith("<boundingCoordinates>")) {
            boundingCoordinates.setLength(0);

          } else if (tags.endsWith("</westBoundingCoordinate>")) {
            double d = String2.parseDouble(xmlReader.content());
            if (Double.isFinite(d)) {
              String2.ifSomethingConcat(
                  boundingCoordinates, ", ", "westLongitude=" + xmlReader.content());
              westernmost = Math.min(westernmost, d);
            }
          } else if (tags.endsWith("</eastBoundingCoordinate>")) {
            double d = String2.parseDouble(xmlReader.content());
            if (Double.isFinite(d)) {
              String2.ifSomethingConcat(
                  boundingCoordinates, ", ", "eastLongitude=" + xmlReader.content());
              easternmost = Math.max(easternmost, d);
            }
          } else if (tags.endsWith("</southBoundingCoordinate>")) {
            double d = String2.parseDouble(xmlReader.content());
            if (Double.isFinite(d)) {
              String2.ifSomethingConcat(
                  boundingCoordinates, ", ", "southLatitude=" + xmlReader.content());
              southernmost = Math.min(southernmost, d);
            }
          } else if (tags.endsWith("</northBoundingCoordinate>")) {
            double d = String2.parseDouble(xmlReader.content());
            if (Double.isFinite(d)) {
              String2.ifSomethingConcat(
                  boundingCoordinates, ", ", "northLatitude=" + xmlReader.content());
              northernmost = Math.max(northernmost, d);
            }
          } else if (tags.endsWith("<altitudeMinimum>")) {
            double d = String2.parseDouble(xmlReader.content());
            if (Double.isFinite(d)) {
              String2.ifSomethingConcat(
                  boundingCoordinates, ", ", "altitudeMinimum=" + xmlReader.content());
              minAltitude = Math.min(minAltitude, d);
            }
          } else if (tags.endsWith("<altitudeMaximum>")) {
            double d = String2.parseDouble(xmlReader.content());
            if (Double.isFinite(d)) {
              String2.ifSomethingConcat(
                  boundingCoordinates, ", ", "altitudeMaximum=" + xmlReader.content());
              maxAltitude = Math.max(maxAltitude, d);
            }
          } else if (tags.endsWith("<altitudeUnits>")) {
            if (emlIsSomething(xmlReader.content())) {
              String2.ifSomethingConcat(
                  boundingCoordinates, ", ", "altitudeUnits=" + xmlReader.content());
              altitudeUnits = xmlReader.content();
            }
          }

        } else if (tags.startsWith(
            "<eml:eml><dataset><coverage><geographicCoverage></boundingCoordinates>")) {
          if (boundingCoordinates.length() > 0) {
            String ts =
                String2.periodSpaceConcat(
                    coverage.toString(), "BoundingCoordinates(" + boundingCoordinates + ")");
            coverage.setLength(0);
            coverage.append(ts);
          }
          boundingCoordinates.setLength(0);

        } else if (tags.equals("<eml:eml><dataset><coverage></geographicCoverage>")) {
          coverage.append("\n\n");

          // temporalCoverage
        } else if (tags.equals(
            "<eml:eml><dataset><coverage><temporalCoverage>"
                + "<rangeOfDates><beginDate></calendarDate>")) {
          // 2000-08-01
          if (emlIsSomething(xmlReader.content()))
            addGlobalAtts.add("time_coverage_start", xmlReader.content());

        } else if (tags.equals(
            "<eml:eml><dataset><coverage><temporalCoverage>"
                + "<rangeOfDates><endDate></calendarDate>")) {
          // 2000-08-01
          if (emlIsSomething(xmlReader.content()))
            addGlobalAtts.add("time_coverage_end", xmlReader.content());

          // <contact> has additional people. Skip.
          // <publisher> already handled above with more info.

          // <methods>
        } else if (tags.equals("<eml:eml><dataset><method><methodStep>")
            || // EML 2.0.1
            tags.equals("<eml:eml><dataset><methods><methodStep>")) { // EML 2.1.0
          methodNumber++;
          methodsDescription = "";
          methodsHeader = "*** Method #" + methodNumber + ":\n";

        } else if (tags.equals("<eml:eml><dataset><method><methodStep><description>")
            || // EML 2.0.1
            tags.equals("<eml:eml><dataset><methods><methodStep><description>")) { // EML 2.1.0
          methodsDescription = "* Description: " + xmlReader.readDocBookAsPlainText();

        } else if (tags.equals("<eml:eml><dataset><method><methodStep><protocol></title>")
            || // EML 2.0.1
            tags.equals("<eml:eml><dataset><methods><methodStep><protocol></title>")) { // EML 2.1.1
          if (emlIsSomething(xmlReader.content())) {
            String2.ifSomethingConcat(
                methods,
                String2.isSomething(methodsHeader) ? "\n\n" : "\n",
                methodsHeader + "* Title: " + xmlReader.content() + "\n" + methodsDescription);
            methodsHeader = "";
          }

        } else if (tags.equals(
                "<eml:eml><dataset><method><methodStep><protocol><distribution><online></url>")
            || // EML 2.0.1
            tags.equals(
                "<eml:eml><dataset><methods><methodStep><protocol><distribution><online></url>")) { // EML 2.1.1
          if (emlIsSomething(xmlReader.content())) {
            String2.ifSomethingConcat(
                methods,
                String2.isSomething(methodsHeader) ? "\n\n" : "\n",
                methodsHeader + "* URL: " + xmlReader.content());
            methodsHeader = "";
          }

        } else if (tags.equals(
                "<eml:eml><dataset><method><methodStep><protocol><creator></individualName>")
            || // EML 2.0.1
            tags.equals(
                "<eml:eml><dataset><methods><methodStep><protocol><creator></individualName>")) { // EML 2.1.1
          if (individualName.length() > 0) { // accumulated above
            String2.ifSomethingConcat(
                methods,
                String2.isSomething(methodsHeader) ? "\n\n" : "\n",
                methodsHeader + "* Creator: " + individualName.toString());
            methodsHeader = "";
          }

          // project
        } else if (tags.equals("<eml:eml><dataset><project></title>")) {
          if (emlIsSomething(xmlReader.content()))
            addGlobalAtts.add("project", xmlReader.content()); // an ACDD attribute

        } else if (tags.equals("<eml:eml><dataset><project><personnel>")) {
          projectPerson++;

        } else if (tags.equals("<eml:eml><dataset><project><personnel></individualName>")) {
          if (individualName.length() > 0) // accumulated above
          addGlobalAtts.add(
                "project_personnel_" + projectPerson + "_name", individualName.toString().trim());

        } else if (tags.equals("<eml:eml><dataset><project><personnel></address>")) {
          if (address.length() > 0)
            addGlobalAtts.add(
                "project_personnel_" + projectPerson + "_address", address.toString().trim());

        } else if (tags.equals("<eml:eml><dataset><project><personnel></electronicMailAddress>")) {
          if (emlIsSomething(xmlReader.content()))
            addGlobalAtts.add("project_personnel_" + projectPerson + "_email", xmlReader.content());

        } else if (tags.equals("<eml:eml><dataset><project><personnel></role>")) {
          if (emlIsSomething(xmlReader.content()))
            addGlobalAtts.add("project_personnel_" + projectPerson + "_role", xmlReader.content());

        } else if (tags.equals("<eml:eml><dataset><project><abstract>")) {
          addGlobalAtts.add(
              "project_abstract",
              String2.repeatedlyReplaceAll(
                  xmlReader.readDocBookAsPlainText(), "\t\t\t", "\t\t", false)); // max 2 tabs

        } else if (tags.equals("<eml:eml><dataset><project><funding>")) {
          // NSF Awards OCE-9982105, OCE-0620276, OCE-1232779
          String tf = xmlReader.readDocBookAsPlainText();
          addGlobalAtts.add("project_funding", tf);
          addGlobalAtts.add("acknowledgement", "Funding: " + tf);

          // dataTable
        } else if (tags.equals("<eml:eml><dataset><dataTable>")) {
          dataTablei++;
          if (whichDataTable != dataTablei) {
            if (debugMode) String2.log("skipping dataTable#" + dataTablei);
            xmlReader.skipToStackSize(xmlReader.stackSize());
          }

        } else if (tags.equals("<eml:eml><dataset><dataTable></entityName>")) {
          addGlobalAtts.add(
              "title",
              String2.periodSpaceConcat(addGlobalAtts.getString("title"), xmlReader.content()));

        } else if (tags.equals("<eml:eml><dataset><dataTable></entityDescription>")) {
          String ts = addGlobalAtts.getString("summary");
          if (!String2.isSomething(ts)) ts = addGlobalAtts.getString("title");
          addGlobalAtts.add("summary", String2.periodSpaceConcat(ts, xmlReader.content()));

        } else if (tags.equals("<eml:eml><dataset><dataTable><physical></objectName>")) {
          if (emlIsSomething(xmlReader.content())) dataFileName = xmlReader.content();

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical><dataFormat>"
                + "<textFormat></numHeaderLines>")) {

          int nhl = String2.parseInt(xmlReader.content());
          if (nhl < Integer.MAX_VALUE) numHeaderLines = nhl;

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical><dataFormat>"
                + "<textFormat></numPhysicalLinesPerRecord>")) {

          int nfl = String2.parseInt(xmlReader.content());
          if (nfl < Integer.MAX_VALUE && nfl != 1)
            throw new SimpleException(
                "<numPhysicalLinesPerRecord> ="
                    + nfl
                    + ", but ERDDAP only supports <numPhysicalLinesPerRecord> =1.");

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical><dataFormat>"
                + "<textFormat><simpleDelimited></fieldDelimiter>")) {
          // ,
          if (emlIsSomething(xmlReader.content())) dataFileDelimiter = xmlReader.content();

          // data access allow/deny
          // I'm not catching/dealing with <access order="allowFirst">
        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical>" + "<distribution><access>")) {
          // license.append("\nData Access Rights:\n");

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical>"
                + "<distribution><access><allow></principal>")) {
          if (emlIsSomething(xmlReader.content())) accessPrincipal = xmlReader.content();

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical>"
                + "<distribution><access><allow></permission>")) {
          // read, write, changePermission are listed separately
          license.append(
              "Data \""
                  + xmlReader.content()
                  + "\" access is allowed for principal=\""
                  + accessPrincipal
                  + "\".\n");

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical>"
                + "<distribution><access><deny></principal>")) {
          accessPrincipal = xmlReader.content();

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical>"
                + "<distribution><access><deny></permission>")) {
          // read, write, changePermission
          license.append(
              "Data \""
                  + xmlReader.content()
                  + "\" access is denied for principal='"
                  + accessPrincipal
                  + "\".\n");

          // <access></allow> and <access></deny> are handled above
          //    accessPrincipal = "";

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><physical>" + "<distribution><online></url>")) {
          // https://pasta.lternet.edu/package/data/eml/knb-lter-sbc/17/28/a7899f2e57ea29a240be2c00cce7a0d4
          // When download in browser, it appears as actual file name for download: ...csv
          if (emlIsSomething(xmlReader.content())) dataFileUrl = xmlReader.content();

        } else if (tags.equals("<eml:eml><dataset><dataTable><attributeList>" + "<attribute>")) {

          varName = "column" + (addTable.nColumns() + 1); // default
          varLongName = "";
          varComment.setLength(0);
          varType = ""; // defaults to String
          varUnits = "";
          varTimePrecision = null;
          varMV.clear();

        } else if (tags.equals("<eml:eml><dataset><dataTable><attributeList>" + "</attribute>")) {

          // varType can use different standards.
          // They recommend: https://www.w3.org/2001/XMLSchema-datatypes
          //  and it is specified in each tag via
          //  typeSystem="https://www.w3.org/2001/XMLSchema-datatypes"
          // e.g., lterSbc storageType just uses float, string, decimal, integer, date, "", dateTime
          //      lterSbc NumberType uses real, whole, integer, natural (positive integer)
          PAType tPAType =
              varType == null
                  ? PAType.STRING
                  : varType.equals("boolean")
                          || // ???
                          varType.equals("byte")
                      ? PAType.BYTE
                      : varType.equals("unsignedByte") || varType.equals("short")
                          ? PAType.SHORT
                          : varType.equals("integer")
                                  || varType.equals("int")
                                  || varType.equals("gYear")
                                  || varType.equals("gDay")
                                  || varType.equals("gMonth")
                                  || varType.equals("natural")
                                  || // positive integer
                                  varType.equals("nonNegativeInteger")
                                  || varType.equals("nonPositiveInteger")
                                  || varType.equals("negativeInteger")
                                  || varType.equals("positiveInteger")
                                  || varType.equals("unsignedShort")
                                  || varType.equals("whole")
                                  || varType.equals("hexBinary")
                              ? PAType.INT
                              : varType.equals("real")
                                      || // Fortran and sql "real" -> float
                                      varType.equals("float")
                                  ? PAType.FLOAT
                                  : varType.equals("unsignedInt")
                                          || varType.equals("long")
                                          || // longs are trouble -> double
                                          varType.equals("unsignedLong")
                                          || varType.equals("decimal")
                                          || varType.equals("double")
                                      ? PAType.DOUBLE
                                      : PAType.STRING; // the default

          // special case: "real" lat/lon -> double
          String varNameLC = varName.toLowerCase();
          if ((varNameLC.startsWith("lat") || varNameLC.startsWith("lon"))
              && (varUnits.toLowerCase().indexOf("deg") >= 0)
              && "real".equals(varType)) tPAType = PAType.DOUBLE;

          PrimitiveArray pa = PrimitiveArray.factory(tPAType, 1, false);

          // special case: gtime_GMT
          if ("mtime_GMT".equals(varName) && "nominal day".equals(varUnits))
            varUnits = "days since 0000-01-01T00:00:00Z";

          // create Attributes for the column
          Attributes atts = new Attributes();
          String2.trim(varComment);
          if (varComment.length() > 0) atts.set("comment", varComment.toString().trim());
          // mv and fv data types will be set correctly below
          if (varMV.size() >= 1) atts.set("missing_value", varMV.getString(0));
          if (varMV.size() >= 2) atts.set("_FillValue", varMV.getString(1));
          if (emlIsSomething(varLongName)) atts.set("long_name", varLongName.trim());
          if (emlIsSomething(varUnits)) atts.set("units", varUnits.trim());
          if (emlIsSomething(varTimePrecision)) atts.set("time_precision", varTimePrecision);

          // add the column to addTable
          addTable.addColumn(addTable.nColumns(), varName, pa, atts);

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><attributeList>" + "<attribute></attributeName>")) {
          if (emlIsSomething(xmlReader.content())) varName = xmlReader.content();

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><attributeList>" + "<attribute></attributeLabel>")) {
          if (emlIsSomething(xmlReader.content())) varLongName = xmlReader.content();

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><attributeList>" + "<attribute></attributeDefinition>")) {
          if (emlIsSomething(xmlReader.content())) varComment.append(xmlReader.content() + "\n");

        } else if (tags.equals(
                "<eml:eml><dataset><dataTable><attributeList>"
                    + "<attribute><measurementScale><nominal><nonNumericDomain>"
                    + // nominal
                    "<enumeratedDomain><codeDefinition></code>")
            || tags.equals(
                "<eml:eml><dataset><dataTable><attributeList>"
                    + "<attribute><measurementScale><ordinal><nonNumericDomain>"
                    + // ordinal
                    "<enumeratedDomain><codeDefinition></code>")) {
          // ABUR
          varComment.append(xmlReader.content() + " = "); // will be followed by definition...

        } else if (tags.equals(
                "<eml:eml><dataset><dataTable><attributeList>"
                    + "<attribute><measurementScale><nominal><nonNumericDomain>"
                    + // nominal
                    "<enumeratedDomain><codeDefinition></definition>")
            || tags.equals(
                "<eml:eml><dataset><dataTable><attributeList>"
                    + "<attribute><measurementScale><ordinal><nonNumericDomain>"
                    + // ordinal
                    "<enumeratedDomain><codeDefinition></definition>")) {
          // Arroyo Burro
          varComment.append(xmlReader.content() + "\n");

        } else if (
        // This is the preferred source of the varType because it is fine-grained.
        // recommended: https://www.w3.org/2001/XMLSchema-datatypes
        tags.equals("<eml:eml><dataset><dataTable><attributeList><attribute>" + "</storageType>")) {

          String tc = xmlReader.content();
          if (emlIsSomething(tc)
              && !tc.toLowerCase().equals("string")) // too many are erroneously marked 'string'
          varType = tc;

        } else if (
        // tagsLC because EML 2.0.1 had <datetime> , 2.1.1 has <dateTime>
        // knb-lter-sbc.5 has <datetime>
        tagsLC.equals(
            "<eml:eml><dataset><datatable><attributelist><attribute>"
                + "<measurementscale><datetime>")) {

          varType = "string";

        } else if (
        // EML numberType (coarse):  real, whole, integer, natural (positive integer)
        // <interval> in sbc 1015
        tags.equals(
                "<eml:eml><dataset><dataTable><attributeList><attribute>"
                    + "<measurementScale><interval><numericDomain></numberType>")
            ||
            // <ratio> in sbc 1
            tags.equals(
                "<eml:eml><dataset><dataTable><attributeList><attribute>"
                    + "<measurementScale><ratio><numericDomain></numberType>")) {

          if (emlIsSomething(xmlReader.content()) && !emlIsSomething(varType)) {
            varType = xmlReader.content();
            // The only floating point EML numberType is "real".
            // ??? Treat real as float or double ???
            // Very few measured values (other than time) need double precision.
            // Lat and lon are handled as special case above (-> double).
            // So here, leave as "real" which usually becomes "float" above.
          }

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><attributeList>"
                + "<attribute><measurementScale><dateTime></formatString>")) {
          // dateTimes, e.g. YYYY, MM, MM/DD/YYYY
          if (emlIsSomething(xmlReader.content())) {
            varUnits = xmlReader.content();

            // convert DateTime formatting strings to java.time (was Joda):
            // deal with little things
            // file:///C:/programs/joda-time-2.1/docs/index.html?overview-summary.html
            varUnits =
                String2.replaceAll(
                    varUnits, "YYYY-MM-DDT-8hh:mm", "local time"); // invalid UDUNITS but let it go
            varUnits = String2.replaceAll(varUnits, "[", "");
            varUnits = String2.replaceAll(varUnits, "]", "");
            varUnits = String2.replaceAllIgnoreCase(varUnits, "mon", "MMM");
            varUnits = String2.replaceAll(varUnits, "WWW", "MMM"); // typo
            if (varUnits.endsWith("-h"))
              varUnits =
                  varUnits.substring(0, varUnits.length() - 2)
                      + "Z"; // timezone will be interpreted

            // convertToJavaDateTimeFormat e.g., yyyy-MM-dd'T'HH:mm:ssZ
            varUnits = Calendar2.convertToJavaDateTimeFormat(varUnits);
            if (varUnits.indexOf("y") >= 0
                && // has years
                varUnits.indexOf("M") >= 0
                && // has month
                varUnits.indexOf("d") >= 0
                && // has days
                varUnits.indexOf("H") < 0) // doesn't have hours
            varTimePrecision = "1970-01-01";
            // String2.pressEnterToContinue(">> varName=" + varName + " varUnits=" + varUnits);
          }

        } else if (tags.equals(
                "<eml:eml><dataset><dataTable><attributeList>"
                    + "<attribute><measurementScale><interval><unit></customUnit>")
            || // interval custom: g/0.09m2
            tags.equals(
                "<eml:eml><dataset><dataTable><attributeList>"
                    + "<attribute><measurementScale><interval><unit></standardUnit>")
            || // interval standard: dimensionless
            tags.equals(
                "<eml:eml><dataset><dataTable><attributeList>"
                    + "<attribute><measurementScale><ratio><unit></customUnit>")
            || // ratio custom: gramsPerSquareMeter
            tags.equals(
                "<eml:eml><dataset><dataTable><attributeList>"
                    + "<attribute><measurementScale><ratio><unit></standardUnit>")) { // ratio
          // standard:
          // number
          // non-dates: centimeter, number, metersquared, meter
          if (emlIsSomething(xmlReader.content())) {
            varUnits = xmlReader.content();

            // convert to UDUnits
            // No good solution for some, e.g., biomassDensityUnitPerAbundanceUnit
            // I think it is reasonable to just break into words.

            // special cases
            varUnits = String2.replaceAll(varUnits, "permil", "per 1000");
            varUnits = String2.replaceAll(varUnits, "milliMoles", "millimoles");

            // most: gramPerMeterSquaredPerDay becomes gram per meter^2 per day
            // I checked: these are lowercase in UDUNITS: knot, siemens,
            //  sievert, steradian, watt
            varUnits = String2.camelCaseToTitleCase(varUnits).toLowerCase();
            varUnits = String2.replaceAll(varUnits, " cubed", "^3");
            varUnits = String2.replaceAll(varUnits, " squared", "^2");
            varUnits = String2.replaceAll(varUnits, "dimensionless", "1");
            varUnits =
                String2.replaceAll(
                    varUnits, "einstein", "mole"); // https://en.wikipedia.org/wiki/Einstein_(unit)
            varUnits = String2.replaceAll(varUnits, "million", "1000000"); // perMillion
            varUnits = String2.replaceAll(varUnits, "number", "count");
            varUnits = String2.replaceAll(varUnits, "reciprocal", "per"); // reciprocalMeter
            varUnits = String2.replaceAll(varUnits, "thousand", "1000"); // perThousand
          }

        } else if (tags.equals(
            "<eml:eml><dataset><dataTable><attributeList>"
                + "<attribute><missingValueCode></code>")) {
          if (String2.isSomething(xmlReader.content())) // NULL are sometimes used and is legit
          varMV.add(xmlReader.content());
        }
      }
    } catch (Throwable t) {
      String2.log(
          String2.ERROR
              + " in "
              + emlFileName
              + " on line #"
              + (xmlReader == null ? -1 : xmlReader.lineNumber())
              + ":\n"
              + MustBe.throwableToString(t));
    } finally {
      try {
        if (xmlReader != null) xmlReader.close();
      } catch (Throwable t2) {
      }
    }

    if (dataTablei < whichDataTable)
      throw new SimpleException(
          "There is no <dataTable> #" + whichDataTable + " in this EML file.");

    // cleanup
    addGlobalAtts.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");

    if (!emlIsSomething(addGlobalAtts.getString("creator_name"))
        && emlIsSomething(addGlobalAtts.getString("publisher_name"))) {
      addGlobalAtts.set("creator_name", addGlobalAtts.getString("publisher_name"));
      addGlobalAtts.set("creator_type", "institution");
    }
    if (!emlIsSomething(addGlobalAtts.getString("creator_email")))
      addGlobalAtts.set("creator_email", addGlobalAtts.getString("publisher_email"));

    String2.ifSomethingConcat(license, "\n", licenseOther.toString().trim());
    String2.repeatedlyReplaceAll(license, "\t\t\t", "\t\t", false); // max 2 tabs
    addGlobalAtts.set(
        "license", license.length() == 0 ? "[standard]" : String2.trim(license).toString());

    String2.trim(methods);
    String2.repeatedlyReplaceAll(methods, "\t\t\t", "\t\t", false); // max 2 tabs
    if (methods.length() > 0) addGlobalAtts.set("methods", methods.toString());

    String tSummary = addGlobalAtts.getString("summary");
    if (!String2.isSomething(tSummary)) tSummary = addGlobalAtts.getString("title");
    if (!String2.isSomething(tSummary)) tSummary = datasetID;
    addGlobalAtts.set(
        "summary", String2.repeatedlyReplaceAll(tSummary, "\t\t\t", "\t\t", false)); // max 2 tabs

    // cleanup geospatial
    if (coverage.length() > 0)
      addGlobalAtts.set("geographicCoverage", String2.trim(coverage).toString());

    if (westernmost < Double.MAX_VALUE) {
      addGlobalAtts.set("geospatial_lon_min", westernmost);
      addGlobalAtts.set("geospatial_lon_units", "degrees_east");
    }
    if (easternmost > -Double.MAX_VALUE) {
      addGlobalAtts.set("geospatial_lon_max", easternmost);
      addGlobalAtts.set("geospatial_lon_units", "degrees_east");
    }
    if (southernmost < Double.MAX_VALUE) {
      addGlobalAtts.set("geospatial_lat_min", southernmost);
      addGlobalAtts.set("geospatial_lat_units", "degrees_north");
    }
    if (northernmost > -Double.MAX_VALUE) {
      addGlobalAtts.set("geospatial_lat_max", northernmost);
      addGlobalAtts.set("geospatial_lat_units", "degrees_north");
    }
    if (minAltitude < Double.MAX_VALUE) {
      addGlobalAtts.set("geospatial_vertical_min", minAltitude);
      addGlobalAtts.set("geospatial_vertical_units", altitudeUnits);
      addGlobalAtts.set("geospatial_vertical_positive", "up");
    }
    if (maxAltitude > -Double.MAX_VALUE) {
      addGlobalAtts.set("geospatial_vertical_max", maxAltitude);
      addGlobalAtts.set("geospatial_vertical_units", altitudeUnits);
      addGlobalAtts.set("geospatial_vertical_positive", "up");
    }

    // ??? references

    // String2.log(addTable.toString());
    String2.log(
        "\nid="
            + datasetID
            + "\ndataFileName="
            + dataFileName
            + "\nnumHeaderLines="
            + numHeaderLines
            + "\ndataFileDelimiter="
            + dataFileDelimiter
            + "\ndataFileUrl="
            + dataFileUrl); // sometimes goofy file name (for tracking?)

    // download dataFileUrl and save it as dataFileName
    if (String2.isSomething(dataFileUrl)) {
      dataFileName =
          emlIsSomething(dataFileName) ? File2.getNameAndExtension(dataFileName) : datasetID;
      if (useLocalFilesIfPresent && File2.isFile(emlDir + dataFileName)) {
      } else {
        SSR.downloadFile(
            dataFileUrl, emlDir + dataFileName, true); // tryToUseCompression; throws Exception
      }
    }

    // deal with .zip if someone has unzipped it by hand
    // and if dataFileName is .csv
    if (dataFileName.endsWith(".zip")) {
      StringArray unzippedNames = new StringArray();
      SSR.unzip(emlDir + dataFileName, emlDir, true, 100, unzippedNames);
      if (unzippedNames.size() != 1)
        throw new SimpleException(
            "ZIP:\n"
                + "dataFileName="
                + dataFileName
                + " has "
                + unzippedNames.size()
                + " files inside! "
                + "ERDDAP currently just allows 1. The fileNames are:\n"
                + unzippedNames);
      dataFileName = File2.getNameAndExtension(unzippedNames.get(0));
    }

    // *** read the sample file: make a table to hold the sourceAttributes
    Table sourceTable = new Table();
    if (charset == null || charset.length() == 0) charset = defaultDatafileCharset;
    boolean columnar = false; // are there any? how detect?

    IntArray colStart = new IntArray();
    IntArray colStop = new IntArray();
    if (columnar) {
      // get info from the sampleFile
      StringArray colNames = new StringArray();
      getColumnInfo(
          emlDir + dataFileName,
          charset,
          numHeaderLines, // namesRow (1..)  -1 for none
          numHeaderLines + 1, // dataRow  (1..)
          colNames,
          colStart,
          colStop);

      sourceTable.readColumnarASCIIFile(
          emlDir + dataFileName,
          charset,
          "",
          "", // not auto detected: skipHeaderToRegex, skipLinesRegex,
          numHeaderLines - 1, // firstDataRow  (0..)
          colNames.toArray(),
          colStart.toArray(),
          colStop.toArray(),
          null); // null = dest classes
      sourceTable.convertIsSomething2();
      sourceTable.simplify();
      sourceTable.standardize(tStandardizeWhat);

    } else {
      // read comma, space, or tab separated
      sourceTable.readASCII(
          emlDir + dataFileName,
          charset,
          "",
          "", // skipHeaderToRegex, skipLinesRegex,
          numHeaderLines - 1, // namesRow (0..)  -1 for none
          numHeaderLines,
          "", // dataRow  (0..)
          null,
          null,
          null,
          null,
          false); // simplify
      sourceTable.convertIsSomething2();
      sourceTable.simplify();
      sourceTable.standardize(tStandardizeWhat);
    }
    if (verbose)
      String2.log(
          "\nlocal data file="
              + emlDir
              + dataFileName
              + "\n"
              + sourceTable.dataToString(3)
              + "\n\n"
              + "EML colNames="
              + addTable.getColumnNamesCSSVString()
              + "\n");

    // globalAttributes
    addGlobalAtts.setIfNotAlreadySet("sourceUrl", "(local files)");

    // clean up attributes
    // make a new sourceTable (with columns in addTable order)
    boolean dateTimeAlreadyFound = false;
    String lcSourceColNames[] = sourceTable.getColumnNames().clone();
    for (int col = 0; col < sourceTable.nColumns(); col++)
      lcSourceColNames[col] = lcSourceColNames[col].toLowerCase();

    // compare column names, generate list of significantly different, ask for okay
    StringBuilder differ = new StringBuilder();
    int minNC = Math.min(sourceTable.nColumns(), addTable.nColumns());
    boolean differentNC = sourceTable.nColumns() != addTable.nColumns();
    if (differentNC)
      throw new SimpleException(
          "DIFFERENT NUMBER OF COLUMNS for datasetID="
              + datasetID
              + " dataFileNme="
              + dataFileName
              + ":\n"
              + "nColumns="
              + sourceTable.nColumns()
              + " in datafile="
              + dataFileName
              + ":\n"
              + sourceTable.getColumnNamesCSSVString()
              + "\n"
              + "but nColumns="
              + addTable.nColumns()
              + " in EML file="
              + emlFileName
              + ":\n"
              + addTable.getColumnNamesCSSVString()
              + ".");
    for (int col = 0; col < minNC; col++) {
      String oSourceName = sourceTable.getColumnName(col);
      String oAddName = addTable.getColumnName(col);
      String tSourceName = oSourceName.toLowerCase();
      String tAddName = oAddName.toLowerCase();
      int po;
      if (tSourceName.endsWith(")")) {
        po = tSourceName.lastIndexOf("(");
        if (po > 1) tSourceName = tSourceName.substring(0, po);
      } else if (tSourceName.endsWith("]")) {
        po = tSourceName.lastIndexOf("[");
        if (po > 1) tSourceName = tSourceName.substring(0, po);
      }
      if (tAddName.endsWith(")")) {
        po = tAddName.lastIndexOf("(");
        if (po > 1) tAddName = tAddName.substring(0, po);
      } else if (tAddName.endsWith("]")) {
        po = tAddName.lastIndexOf("[");
        if (po > 1) tAddName = tAddName.substring(0, po);
      }
      String punc = " ._-:"; // '/' is used in mm/dd/yy and as "per" so leave it
      for (po = 0; po < punc.length(); po++) {
        tSourceName = String2.replaceAll(tSourceName, punc.substring(po, po + 1), "");
        tAddName = String2.replaceAll(tAddName, punc.substring(po, po + 1), "");
      }
      if (tSourceName.equals(tAddName)
          || (tSourceName + "btl").equals(tAddName)
          || (tSourceName + "number").equals(tAddName)
          || (tSourceName + "s").equals(tAddName)
          || ("ctd" + tSourceName).equals(tAddName)
          || ("functional" + tSourceName).equals(tAddName)
          || ("odv" + tSourceName).equals(tAddName)
          || ("taxon" + tSourceName).equals(tAddName)
          || tSourceName.equals(tAddName + "s")
          || tSourceName.equals(tAddName + "wt")
          || tSourceName.equals("taxon" + tAddName)) continue;
      differ.append("  " + String2.left(oSourceName, 20) + " = " + oAddName + "\n");
    }
    boolean equate = false;
    if (differentNC) {
      equate = false;
    } else if (differ.length() == 0) {
      equate = true;
    } else {
      String te = "zz";
      while (!te.equals("") && !te.equals("y")) {
        String msg =
            "datasetID="
                + datasetID
                + "\n"
                + "dataFile="
                + dataFileName
                + "\n"
                + "The data file and EML file have different column names.\n"
                + "ERDDAP would like to equate these pairs of names:\n"
                + differ;
        if (!EDStatic.developmentMode) {
          te =
              String2.getStringFromSystemIn(
                  "WARNING for"
                      + msg
                      + "Enter 'y' or (nothing) for yes, 's' for skip this dataTable: ");
          if (te.equals("s"))
            return "<!-- fromEML SKIPPED (USUALLY BECAUSE THE COLUMN NAMES IN THE DATAFILE ARE IN\n"
                + "A DIFFERENT ORDER OR HAVE DIFFERENT UNITS THAN IN THE EML file):\n"
                + msg
                + "-->\n";
        }
      }
      equate = te.length() == 0 || te.equals("y");
    }

    // !!!USER CHOSE TO EQUATE COLS in SOURCE FILE with COLUMNS in EML, 1 to 1, SAME ORDER!!!
    addGlobalAtts.trimAndMakeValidUnicode();
    for (int col = 0; col < addTable.nColumns(); col++) { // nCols changes, so always check
      String colName = addTable.getColumnName(col);
      String sourceVarName = sourceTable.getColumnName(col);
      Attributes sourceVarAtts = sourceTable.columnAttributes(col);
      sourceVarAtts.trimAndMakeValidUnicode();
      PAType sourcePAType = sourceTable.getColumn(col).elementType(); // from file
      PAType destPAType = addTable.getColumn(col).elementType(); // as defined

      // make and apply revisions to the variable's addAtts
      Attributes addVarAtts = addTable.columnAttributes(col);
      addVarAtts.set(
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              addGlobalAtts,
              sourceVarAtts,
              addVarAtts,
              sourceVarName,
              destPAType != PAType.STRING, // tryToAddStandardName
              destPAType != PAType.STRING, // addColorBarMinMax
              true)); // tryToFindLLAT
      if (columnar) {
        addVarAtts.add("startColumn", colStart.get(col));
        addVarAtts.add("stopColumn", colStop.get(col));
      }

      // state columnNameInSourceFile
      addVarAtts.add("columnNameInSourceFile", sourceVarName);

      // get units from sourceName, e.g., [C]?
      String tUnits = sourceVarAtts.getString("units");
      if (tUnits == null) tUnits = addVarAtts.getString("units");
      if (tUnits == null) {
        int po = -1;
        if (sourceVarName.endsWith("]")) po = sourceVarName.lastIndexOf("[");
        else if (sourceVarName.endsWith(")")) po = sourceVarName.lastIndexOf("(");
        if (po > 1) {
          String2.log("col #" + col + " units are from sourceVarName=" + sourceVarName);
          tUnits = sourceVarName.substring(po + 1, sourceVarName.length() - 1);
          addVarAtts.set("units", tUnits);
        }
      }
      if (tUnits == null) tUnits = "";

      // dataType
      if (Calendar2.isStringTimeUnits(tUnits)) {
        // force to be String
        sourcePAType = PAType.STRING;
        destPAType = PAType.STRING;
        sourceTable.setColumn(
            col, PrimitiveArray.factory(sourcePAType, sourceTable.getColumn(col)));
        addTable.setColumn(col, PrimitiveArray.factory(destPAType, addTable.getColumn(col)));
      } else if (sourcePAType == destPAType) {
        // don't change anything
      } else if (destPAType == PAType.STRING) {
        // go with type found in file (from simplify)
        String2.log(
            "!!! WARNING: For datasetID="
                + datasetID
                + ", for destinationColName="
                + colName
                + ", ERDDAP is changing the data type from String "
                + "(as specified in EML) to "
                + sourcePAType
                + ". [observed in file]");
        addTable.setColumn(col, PrimitiveArray.factory(sourcePAType, addTable.getColumn(col)));
        destPAType = sourcePAType;
      } else {
        // varType specified? use addTable type
        sourceTable.setColumn(col, PrimitiveArray.factory(destPAType, sourceTable.getColumn(col)));
        sourcePAType = destPAType;
      }

      if (destPAType == PAType.STRING) {
        // for String vars, remove any colorBar info
        addVarAtts.remove("colorBarMinimum");
        addVarAtts.remove("colorBarMaximum");
        addVarAtts.remove("colorBarScale");
      }

      // last
      addVarAtts.trimAndMakeValidUnicode();
    }

    // add missing_value or _FillValue to numeric columns if needed
    addMvFvAttsIfNeeded(sourceTable, addTable);

    // look for LLAT
    // The regular ERDDAP system to look for LLAT doesn't work because:
    //  When addTable names different from sourceTable (as is common here),
    //  the regular system refuses to change the addTable colName.
    //  So need to do it here.
    {
      String sourceColNames[] = sourceTable.getColumnNames();
      String addColNames[] = addTable.getColumnNames();
      int col;

      col = String2.lineStartsWithIgnoreCase(addColNames, "latitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "latitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(addColNames, "north_latitude"); // exact
      if (col < 0)
        col = String2.lineStartsWithIgnoreCase(sourceColNames, "north_latitude"); // exact
      if (col < 0)
        col =
            String2.lineStartsWithIgnoreCase(
                addColNames, "north latitude"); // exact  knb_lter_sbc_1107_t3
      if (col < 0)
        col = String2.lineStartsWithIgnoreCase(sourceColNames, "north latitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(addColNames, "lat"); // close
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "lat"); // close
      if (col >= 0) {
        // String2.log(">> lat col=" + col + " sourceName=" + sourceColNames[col]);
        addTable.setColumnName(col, "latitude");
        addTable.columnAttributes(col).set("long_name", "Latitude"); // not e.g., "lat"
        // force to be doubles
        String destPATypeString = addTable.getColumn(col).elementTypeString();
        if (destPATypeString.equals("String")) {
          String2.log(
              "!!! WARNING: For datasetID="
                  + datasetID
                  + ", for destinationColName="
                  + addTable.getColumnName(col)
                  + ", I'm changing the data type from String "
                  + "(as specified in EML) to double. [latitude]");
          addTable.setColumn(col, PrimitiveArray.factory(PAType.DOUBLE, addTable.getColumn(col)));
        }

        // no other column can be called latitude
        int count = 1;
        while ((col = String2.indexOf(addColNames, "latitude", col + 1)) > 0)
          addTable.setColumnName(col, "latitude" + ++count);
      }

      col = String2.lineStartsWithIgnoreCase(addColNames, "longitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "longitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(addColNames, "east_longitude"); // exact
      if (col < 0)
        col = String2.lineStartsWithIgnoreCase(sourceColNames, "east_longitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(addColNames, "east longitude"); // exact
      if (col < 0)
        col = String2.lineStartsWithIgnoreCase(sourceColNames, "east longitude"); // exact
      if (col < 0)
        col =
            String2.lineStartsWithIgnoreCase(
                addColNames, "west_longitude"); // exact //this name is misleading
      if (col < 0)
        col = String2.lineStartsWithIgnoreCase(sourceColNames, "west_longitude"); // exact
      if (col < 0)
        col =
            String2.lineStartsWithIgnoreCase(
                addColNames, "west longitude"); // exact //this name is misleading
      if (col < 0)
        col = String2.lineStartsWithIgnoreCase(sourceColNames, "west longitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(addColNames, "lon"); // close
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "lon"); // close
      if (col >= 0) {
        // String2.log(">> lon col=" + col + " sourceName=" + sourceColNames[col]);
        addTable.setColumnName(col, "longitude");
        addTable.columnAttributes(col).set("long_name", "Longitude");
        // force to be doubles
        String destPATypeString = addTable.getColumn(col).elementTypeString();
        if (destPATypeString.equals("String")) {
          String2.log(
              "!!! WARNING: For datasetID="
                  + datasetID
                  + ", for destinationColName="
                  + addTable.getColumnName(col)
                  + ", I'm changing the data type from String "
                  + "(as specified in EML) to double. [longitude]");
          addTable.setColumn(col, PrimitiveArray.factory(PAType.DOUBLE, addTable.getColumn(col)));
        }

        // no other column can be called longitude
        int count = 1;
        while ((col = String2.indexOf(addColNames, "longitude", col + 1)) > 0)
          addTable.setColumnName(col, "longitude" + ++count);
      }

      col = String2.lineStartsWithIgnoreCase(addColNames, "altitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "altitude"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(addColNames, "elevation"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "elevation"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(addColNames, "alti"); // close
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "alti"); // close
      if (col < 0) col = String2.lineStartsWithIgnoreCase(addColNames, "elev"); // close
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "elev"); // close
      if (col >= 0) {
        addTable.setColumnName(col, "altitude");
        addTable.columnAttributes(col).set("long_name", "Altitude");

        // force to be floats
        String destPATypeString = addTable.getColumn(col).elementTypeString();
        if (destPATypeString.equals("String")) {
          String2.log(
              "!!! WARNING: For datasetID="
                  + datasetID
                  + ", for destinationColName="
                  + addTable.getColumnName(col)
                  + ", I'm changing the data type from String "
                  + "(as specified in EML) to float. [altitude]");
          addTable.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, addTable.getColumn(col)));
        }

        // no other column can be called altitude
        int count = 1;
        while ((col = String2.indexOf(addColNames, "altitude", col + 1)) > 0)
          addTable.setColumnName(col, "altitude" + ++count);
      }

      col = String2.lineStartsWithIgnoreCase(addColNames, "depth"); // exact
      if (col < 0) col = String2.lineStartsWithIgnoreCase(sourceColNames, "depth"); // exact
      if (col >= 0) {
        addTable.setColumnName(col, "depth");
        addTable.columnAttributes(col).set("long_name", "Depth");

        // force to be floats
        String destPATypeString = addTable.getColumn(col).elementTypeString();
        if (destPATypeString.equals("String")) {
          String2.log(
              "!!! WARNING: For datasetID="
                  + datasetID
                  + ", for destinationColName="
                  + addTable.getColumnName(col)
                  + ", I'm changing the data type from String "
                  + "(as specified in EML) to float. [depth]");
          addTable.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, addTable.getColumn(col)));
        }

        // no other column can be called depth
        int count = 1;
        while ((col = String2.indexOf(addColNames, "depth", col + 1)) > 0)
          addTable.setColumnName(col, "depth" + ++count);
      }

      // clean up time columns
      // get rid of not needed time-related columns
      // !!! look for *the* timeCol (with dateTime and known time zone, or just date data)

      // * Look for goodTimeName
      // These names found via TestAll's use of FileVisitorDNLS.tallyXml()
      //  to look at <attributeName>, then sorted in EditPlus, then look near "time"
      String goodZuluNumericTimeName = null; // ideal
      String goodZuluStringDateTimeName = null; // next best
      String goodLocalNumericTimeName = null; // next best
      String goodLocalStringDateTimeName = null; // next best
      String goodStringDateName = null; // next best: just date
      String goodStringMonthName = null; // next best: just yyyy-MM
      String goodStringYearName = null; // next best: just yyyy
      int yyCol = -1; // will be >=0 if found
      int MMCol = -1;
      int ddCol = -1;
      int HHCol = -1;
      StringArray timeUnitsList = new StringArray();
      for (col = 0; col < sourceTable.nColumns(); col++) {
        String tColName = addTable.getColumnName(col);
        String tColNameLC = tColName.toLowerCase();
        Attributes tAtts = addTable.columnAttributes(col);
        String tUnits = tAtts.getString("units");
        if (tUnits == null) tUnits = "";
        // some vars don't qualify as isTimeUnits, but do have time info
        if (tUnits.indexOf("yyyy") >= 0
            || tUnits.indexOf("uuuu") >= 0
            || // was "yy"
            tColNameLC.indexOf("year") >= 0) yyCol = col;
        if (tUnits.indexOf("MM") >= 0 || tColNameLC.indexOf("month") >= 0) MMCol = col;
        if (tUnits.indexOf("dd") >= 0
            || (tColNameLC.indexOf("day") >= 0
                && tColNameLC.indexOf("per day") < 0
                && tUnits.indexOf("per day") < 0)
            || // knb_lter_sbc_58_t1
            tColNameLC.indexOf("date") >= 0) ddCol = col;
        if (tUnits.indexOf("HH") >= 0) HHCol = col;

        if (tUnits.indexOf("yyyy") >= 0
            || tUnits.indexOf("uuuu") >= 0
            || // was "yy"
            tUnits.indexOf("MM") >= 0
            || tUnits.indexOf("dd") >= 0
            || tUnits.indexOf("HH") >= 0
            || tColNameLC.indexOf("year") >= 0
            || tColNameLC.indexOf("month") >= 0
            || (tColNameLC.indexOf("day") >= 0
                && tColNameLC.indexOf("per day") < 0
                && tUnits.indexOf("per day") < 0)
            || // knb_lter_sbc_58_t1
            tColNameLC.indexOf("date") >= 0) timeUnitsList.add(tColName + "(" + tUnits + ")");

        String sourceName = sourceColNames[col];
        String addName = addColNames[col];
        String sourceNameLC = sourceName.toLowerCase();
        String addNameLC = addName.toLowerCase();
        String tComment = tAtts.getString("comment");
        if (tComment == null) tComment = "";
        String tCommentLC = tComment.toLowerCase();
        String infoLC =
            sourceNameLC + "|" + addNameLC + "|" + tCommentLC + "|" + tUnits.toLowerCase();

        // matlab days since 0000-01-01?   e.g., knb-lter-sbc.2002
        if (infoLC.indexOf("matlab") >= 0
            && (infoLC.indexOf("time") >= 0
                || // knb-lter-sbc.1113
                infoLC.indexOf("day") >= 0
                || infoLC.indexOf("date") >= 0)
            && Calendar2.isStringTimeUnits(infoLC)
            && infoLC.indexOf("mm") < 0
            && infoLC.indexOf("dd") < 0
            && infoLC.indexOf("hh") < 0) {
          goodZuluNumericTimeName = addName;
          tAtts.set("units", "days since 0000-01-01T00:00:00Z");
          // force to be double
          sourceTable.setColumn(
              col, PrimitiveArray.factory(PAType.DOUBLE, sourceTable.getColumn(col)));
          addTable.setColumn(col, PrimitiveArray.factory(PAType.DOUBLE, addTable.getColumn(col)));
          break; // it is ideal, so look no further
        }

        // is it not suitable for my purposes?
        if (!Calendar2.isTimeUnits(tUnits)) continue;

        // it has time units (numeric or String)
        String tTimeZone = tAtts.getString("time_zone"); // caught above?
        if (tTimeZone == null)
          // time_zone specified in comment (as I suggested)?
          String2.extractCaptureGroup(
              tComment, // not LC
              "time_zone=\"(.*)\"",
              1);
        if (tTimeZone == null) tTimeZone = "";
        else if (tTimeZone.toLowerCase().equals("gmt") || tTimeZone.toLowerCase().equals("utc"))
          tTimeZone = "Zulu";
        // test for local first, since some say "local time, -8:00 from UTC"
        if (sourceNameLC.indexOf("local") >= 0
            || addNameLC.indexOf("local") >= 0
            || tCommentLC.indexOf("local") >= 0) tTimeZone = localTimeZone;
        else if (tUnits.endsWith("Z")
            || sourceNameLC.indexOf("gmt") >= 0
            || addNameLC.indexOf("gmt") >= 0
            || tCommentLC.indexOf("gmt") >= 0
            || sourceNameLC.indexOf("utc") >= 0
            || addNameLC.indexOf("utc") >= 0
            || tCommentLC.indexOf("utc") >= 0) tTimeZone = "Zulu";
        // String2.log(">> addName=" + addName + " units=" + tUnits + " tTimeZone=" + tTimeZone);

        // look for numeric dateTime (seconds since ...) and known timezone
        if (Calendar2.isNumericTimeUnits(tUnits) && String2.isSomething(tTimeZone)) {
          tAtts.set("time_zone", tTimeZone);
          if (tTimeZone.equals("Zulu")) {
            goodZuluNumericTimeName = addName;
            break; // that's first choice / all we need
          } else if (goodLocalNumericTimeName == null) {
            goodLocalNumericTimeName = addName;
          }
        }

        // look for String date, or dateTime and known timezone
        if (tUnits.indexOf("HH") >= 0) {
          if (String2.isSomething(tTimeZone)) tAtts.set("time_zone", tTimeZone);
        }
        // was if (tUnits.startsWith("yyyy-MM-dd") ||
        //        tUnits.startsWith("yyyyMMdd")) {
        // because ERDDAP required that when searching minMaxTable.
        // But that was fixed in ERDDAP v1.74.
        if (Calendar2.isStringTimeUnits(tUnits)
            && tUnits.indexOf("MM") >= 0
            && tUnits.indexOf("dd") >= 0) {
          if (tUnits.indexOf("HH") < 0) { // no HH, just date
            // time zone is irrelevant
            if (goodStringDateName == null) goodStringDateName = addName;
          } else if (String2.isSomething(tTimeZone)) { // has HH and timeZone
            if (tTimeZone.equals("Zulu")) {
              if (goodZuluStringDateTimeName == null) goodZuluStringDateTimeName = addName;
            } else {
              if (goodLocalStringDateTimeName == null) goodLocalStringDateTimeName = addName;
            }
          }
        } else if (Calendar2.isStringTimeUnits(tUnits) && tUnits.indexOf("MM") >= 0) {
          if (goodStringMonthName == null) goodStringMonthName = addName;
        } else if (Calendar2.isStringTimeUnits(tUnits)) {
          if (goodStringYearName == null) goodStringYearName = addName;
        }
      }
      sourceColNames = null;
      addColNames = null;
      // a winner?
      String goodTimeName =
          goodZuluNumericTimeName != null
              ? goodZuluNumericTimeName
              : goodZuluStringDateTimeName != null
                  ? goodZuluStringDateTimeName
                  : goodLocalNumericTimeName != null
                      ? goodLocalNumericTimeName
                      : goodLocalStringDateTimeName != null
                          ? goodLocalStringDateTimeName
                          : goodStringDateName != null && HHCol < 0
                              ? goodStringDateName
                              : // date  col and no time col
                              goodStringMonthName != null && HHCol < 0 && ddCol < 0
                                  ? goodStringMonthName
                                  : // month col and no date/time col
                                  goodStringYearName != null && HHCol < 0 && ddCol < 0 && MMCol < 0
                                      ? goodStringYearName
                                      : // year  col and no month/date/time col
                                      null;

      // 2020-11-30 was: Reject tables with date or time but no goodTimeName
      // But now, admin can create a unified column by hand via Derived Variables
      // if (goodTimeName == null && (yyCol>=0 || hasHHCol>=0))
      //    throw new SimpleException(
      //        "NO GOOD DATE(TIME) VARIABLE in datasetID=" + datasetID + " dataFileNme=" +
      // dataFileName + ":\n" +
      //        "ERDDAP is rejecting this dataTable because it only seems to have dateTime variables
      // with\n" +
      //        "unknown time zone or only seperate date and time variables. The file has variables
      // with name (units):\n" +
      //        timeUnitsList.toString());
      // !!! Ideally, this method could make the Derived Variable,
      //  but it is hard because lots of possibilites for what is in various
      //  date/month/day/year/time/hour/minute/second columns (e.g., numbers vs text, formats...)
      //  Leave it to the admin to sort out.
      //  Note that dataset may not load in ERDDAP if a column is called "time" but isn't date+time.

      // If we have an goodTimeName, remove any other columns with numeric time
      // or string time (yy, HH, mm) time units.
      // These files often have 3 columns: dateTime, date, time (but with various names).
      if (goodTimeName != null) {
        for (col = addTable.nColumns() - 1; col >= 0; col--) { // backwards since deleting
          String tName = addTable.getColumnName(col);
          if (tName.equals(goodTimeName)) continue;
          String tNameLC = tName.toLowerCase();
          String tUnits = addTable.columnAttributes(col).getString("units");
          if ((tUnits != null
                  && (Calendar2.isTimeUnits(tUnits)
                      || tUnits.indexOf("MM") >= 0
                      || tUnits.indexOf("dd") >= 0
                      || tUnits.indexOf("HH") >= 0
                      || tUnits.indexOf("mm") >= 0
                      || tUnits.indexOf("ss") >= 0))
              || tNameLC.equals("year")
              || tNameLC.equals("month")
              || (tNameLC.indexOf("day") >= 0
                  && tNameLC.indexOf("per day") < 0
                  && tUnits.indexOf("per day") < 0)
              || // knb_lter_sbc_58_t1
              tNameLC.equals("date")) {
            String2.log(
                "REMOVING not needed date/time column=" + tName + " with revised units=" + tUnits);
            sourceTable.removeColumn(col);
            addTable.removeColumn(col);
          }
        }

        // rename goodTimeName to ERDDAP's preferred "time"
        col = addTable.findColumnNumber(goodTimeName);
        addTable.setColumnName(col, "time");
        Attributes tAtts = addTable.columnAttributes(col);
        tAtts.set("long_name", "Time");
        if (tAtts.getString("comment") != null)
          tAtts.set("comment", "In the source file: " + tAtts.getString("comment"));
      }
    }

    // tryToFindLLAT
    tryToFindLLAT(sourceTable, addTable);

    // after LLAT found
    // *** This fails for knb_lter_sbc_85_t1 where it finds "time2" instead of time
    for (int col = 0; col < addTable.nColumns(); col++) {
      Attributes addAtts = addTable.columnAttributes(col);
      String destName = addTable.getColumnName(col);
      PAType destPAType = addTable.getColumn(col).elementType();

      // if numeric, set colorBarMin Max
      // Note: doing it here overrides naive suggestions in makeReadyToUseAddVariableAttributes
      if (destPAType != PAType.STRING
          && "|longitude|latitude|altitude|depth|time|".indexOf("|" + destName + "|") < 0) {
        double[] stats = sourceTable.getColumn(col).calculateStats(addTable.columnAttributes(col));
        if (stats[PrimitiveArray.STATS_N] > 0) {
          double lh[] =
              Math2.suggestLowHigh(
                  destName.endsWith("_uM") ? 0 : stats[PrimitiveArray.STATS_MIN],
                  stats[PrimitiveArray.STATS_MAX]);
          addAtts.set("colorBarMinimum", lh[0]);
          addAtts.set("colorBarMaximum", lh[1]);
          if (!Double.isNaN(lh[0])
              && lh[0] <= 0
              && "Log".equals(addAtts.getString("colorBarScale"))) addAtts.remove("colorBarScale");
          if (verbose)
            String2.log(
                "  destName="
                    + destName
                    + " destPAType="
                    + destPAType
                    + " data min="
                    + stats[PrimitiveArray.STATS_MIN]
                    + " max="
                    + stats[PrimitiveArray.STATS_MAX]
                    + " -> colorBar Min="
                    + lh[0]
                    + " Max="
                    + lh[1]);
        }
      }
    }

    // after dataVariables known, add global attributes in the addTable
    addGlobalAtts.set(
        makeReadyToUseAddGlobalAttributesForDatasetsXml(
            addGlobalAtts, // unusual
            // another cdm_data_type could be better; this is ok
            hasLonLatTime(addTable) ? "Point" : "Other",
            emlDir,
            null,
            suggestKeywords(sourceTable, addTable)));

    // subsetVariables (do near end so addTable column names are the final names)
    if (sourceTable.globalAttributes().getString("subsetVariables") == null
        && addTable.globalAttributes().getString("subsetVariables") == null)
      addGlobalAtts.add(
          "subsetVariables",
          suggestSubsetVariables(sourceTable, addTable, true)); // 1 datafile / eml file

    // make fileNameRegex from dataFileName.  Quote any special regex characters.
    String from = ".^$*+-?()[]{}\\|";
    StringBuilder tFileNameRegex = new StringBuilder();
    for (int i = 0; i < dataFileName.length(); i++) {
      char ch = dataFileName.charAt(i);
      tFileNameRegex.append(from.indexOf(ch) >= 0 ? "\\" + ch : "" + ch);
    }

    // default query
    StringBuilder defaultDataQuery = new StringBuilder();
    StringBuilder defaultGraphQuery = new StringBuilder();
    if (addTable.findColumnNumber(EDV.TIME_NAME) >= 0) {
      defaultDataQuery.append("&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
      defaultGraphQuery.append("&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
    }
    defaultGraphQuery.append("&amp;.marker=1|5");

    // write the information
    StringBuilder sb = new StringBuilder();
    String tSortFilesBySourceNames = "";

    sb.append(
        "<dataset type=\"EDDTableFrom"
            + (columnar ? "Columnar" : "")
            + "AsciiFiles\" "
            + "datasetID=\""
            + datasetID
            + "\" active=\"true\">\n"
            + (tAccessibleTo == null || tAccessibleTo == "null"
                ? ""
                : "    <accessibleTo>" + tAccessibleTo + "</accessibleTo>\n")
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>-1</updateEveryNMillis>\n"
            + (defaultDataQuery.length() > 0
                ? "    <defaultDataQuery>" + defaultDataQuery + "</defaultDataQuery>\n"
                : "")
            + (defaultGraphQuery.length() > 0
                ? "    <defaultGraphQuery>" + defaultGraphQuery + "</defaultGraphQuery>\n"
                : "")
            + "    <fileDir>"
            + XML.encodeAsXML(emlDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(tFileNameRegex.toString())
            + "</fileNameRegex>\n"
            + "    <recursive>false</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <charset>"
            + charset
            + "</charset>\n"
            + "    <columnNamesRow>"
            + numHeaderLines
            + "</columnNamesRow>\n"
            + "    <firstDataRow>"
            + (numHeaderLines + 1)
            + "</firstDataRow>\n"
            + "    <standardizeWhat>"
            + tStandardizeWhat
            + "</standardizeWhat>\n"
            +
            // "    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) +
            // "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>"
            + XML.encodeAsXML(tSortFilesBySourceNames)
            + "</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <accessibleViaFiles>true</accessibleViaFiles>\n");
    sb.append(writeAttsForDatasetsXml(false, sourceTable.globalAttributes(), "    "));
    sb.append(cdmSuggestion());
    sb.append(writeAttsForDatasetsXml(true, addTable.globalAttributes(), "    "));

    // last 2 params: includeDataType, questionDestinationName
    sb.append(writeVariablesForDatasetsXml(sourceTable, addTable, "dataVariable", true, false));
    sb.append("</dataset>\n" + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }

  /**
   * This does a batch of generateDatasetsXmlFromEML and sends the results to a log file.
   *
   * @param mode e.g., lterSbc, lterNtl
   * @throws Throwable if trouble
   */
  public static void batchFromEML(
      boolean useLocalFilesIfPresent, boolean pauseForErrors, String mode, int tStandardizeWhat)
      throws Throwable {
    String2.log("\n*** EDDTableFromColumnarAsciiFiles.batchFromEML()\n");
    testVerboseOn();
    String baseDataDir = "/u00/data/points/";
    String tAccessibleTo,
        emlDir,
        startUrl,
        localTimeZone; // from TZ at https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
    StringArray names;
    String resultsFileName =
        EDStatic.fullLogsDirectory
            + "fromEML_"
            + Calendar2.getCompactCurrentISODateTimeStringLocal()
            + ".log";

    if ("lterSbc".equals(mode)) {
      tAccessibleTo = "lterSbc";
      emlDir = baseDataDir + "lterSbc/";
      startUrl = "https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/";
      // 2020-01-08 gone. was "http://sbc.lternet.edu/data/eml/files/";
      Table table =
          FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
              startUrl,
              "knb-lter-sbc\\.\\d+",
              false,
              ".*",
              false); // tRecursive, tPathRegex, tDirectoriesToo
      names = (StringArray) table.getColumn(FileVisitorDNLS.NAME);
      localTimeZone = "US/Pacific";

    } else if ("lterNtl".equals(mode)) {
      // LTER NTL: https://lter.limnology.wisc.edu/datacatalog/search
      tAccessibleTo = "lterNtl";
      emlDir = baseDataDir + "lterNtl/";
      startUrl = "https://lter.limnology.wisc.edu/eml_view/";
      // ids = new int[]{31881};
      Table table =
          FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
              startUrl,
              "???r-ntl\\.\\d+",
              false,
              ".*",
              false); // tRecursive, tPathRegex, tDirectoriesToo
      names = (StringArray) table.getColumn(FileVisitorDNLS.NAME);
      localTimeZone = "US/Central";

    } else {
      throw new SimpleException("ERROR: unsupported mode=" + mode);
    }

    String2.log("names=\n" + names.toString());
    for (int i = 0; i < names.size(); i++) {
      // if (names.get(i).compareTo("knb-lter-sbc.59") < 0)
      //    continue;

      if (false) {
        // just download the files
        SSR.downloadFile(
            startUrl + names.get(i),
            emlDir + names.get(i),
            true); // tryToUseCompression; throws Exception

      } else {
        String result =
            generateDatasetsXmlFromEML(
                pauseForErrors,
                emlDir,
                startUrl + names.get(i),
                useLocalFilesIfPresent,
                tAccessibleTo,
                localTimeZone,
                tStandardizeWhat);
        File2.appendFileUtf8(resultsFileName, result);
        String2.log(result);
      }
    }

    String2.log(
        "\n*** batchFromEML finished successfully.\n" + "The results are in " + resultsFileName);

    //        Test.displayInBrowser("file://" + resultsFileName);
  }

  /**
   * This generates a datasets.xml chunk (with an ERDDAP dataset for each table in the EML file)
   * from one EML file in a known collection.
   *
   * @param tAccessibleTo also identifies the collection, e.g., lterSbc or lterNtl.
   * @param which identifies the number assigned to the EML in that collection.
   * @return the datasets.xml chunk (with an ERDDAP dataset for each table in the EML file) from one
   *     EML file in a known collection
   * @throws Throwable if trouble
   */
  public static String generateDatasetsXmlFromOneInEMLCollection(
      String tAccessibleTo, int which, int tStandardizeWhat) throws Throwable {
    String2.log(
        "\n*** EDDTableFromColumnarAsciiFiles.generateDatasetsXmlFromOneInEMLCollection()\n");
    testVerboseOn();
    String name, tName, results, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String emlDir, startUrl, localTimeZone;

    if (tAccessibleTo.equals("lterSbc")) {
      // SBC LTER: 2020-01-09 was http://sbc.lternet.edu/data/eml/files/
      emlDir = "/u00/data/points/lterSbc/";
      startUrl =
          "https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/knb-lter-sbc."
              + which; // original test: .17
      localTimeZone = "US/Pacific";

    } else if (tAccessibleTo.equals("lterNtl")) {
      // source dir url?
      emlDir = "/u00/data/points/lterNtl/";
      startUrl = emlDir + "129.xml";
      localTimeZone = "US/Pacific";

    } else {
      throw new RuntimeException("Unsupported accessibleTo=" + tAccessibleTo);
    }

    results =
        generateDatasetsXmlFromEML(
            false,
            emlDir,
            startUrl,
            true,
            tAccessibleTo, // reuse local files if present
            localTimeZone,
            tStandardizeWhat);
    String2.setClipboardString(results);
    String2.log(results);
    String2.log("\n *** generateDatasetsXmlFromOneInEMLCollection finished successfully.");
    return results;
  }
}
