/*
 * EDDTableFromAsciiFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.BufferedReader;
import java.util.BitSet;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * This class represents a table of data from a collection of ASCII CSV or TSV data files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-02-13
 */
public class EDDTableFromAsciiFiles extends EDDTableFromFiles {

  /** Used to ensure that all non-axis variables in all files have the same leftmost dimension. */
  protected String dim0Name = null;

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
   * The constructor just calls the super constructor.
   *
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   *     <p>The sortedColumnSourceName isn't utilized.
   */
  public EDDTableFromAsciiFiles(
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
        "EDDTableFromAsciiFiles",
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

  /** The constructor for subclasses. */
  public EDDTableFromAsciiFiles(
      String tClassName,
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
        tClassName,
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

    Table table = new Table();
    table.allowRaggedRightInReadASCII = true;
    table.readASCII(
        tFileDir + tFileName,
        charset,
        skipHeaderToRegex,
        skipLinesRegex,
        columnNamesRow - 1,
        firstDataRow - 1,
        columnSeparator,
        null,
        null,
        null, // testColumns, testMin, testMax,
        sourceDataNames.toArray(), // loadColumns,
        false); // don't simplify; just get the strings

    // convert to desired sourceDataTypes
    int nCols = table.nColumns();
    for (int tc = 0; tc < nCols; tc++) {
      int sd = sourceDataNames.indexOf(table.getColumnName(tc));
      if (sd >= 0) {
        PrimitiveArray pa = table.getColumn(tc);
        String tType = sourceDataTypes[sd];
        if (tType.equals("String")) { // do nothing
        } else if (tType.equals("boolean")) {
          table.setColumn(tc, ByteArray.toBooleanToByte(pa));
        } else {
          PrimitiveArray newPa;
          if (tType.equals("char")) {
            CharArray ca = new CharArray();
            int n = pa.size();
            for (int i = 0; i < n; i++) ca.addString(pa.getString(i));
            newPa = ca;
          } else {
            newPa = PrimitiveArray.factory(PAType.fromCohortString(sourceDataTypes[sd]), 1, false);
            newPa.append(pa);
          }
          table.setColumn(tc, newPa);
        }
      }
    }

    // unpack
    table.standardize(standardizeWhat);

    return table;
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromAsciiFiles. The XML can
   * then be edited by hand and added to the datasets.xml file.
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
   *     to "" if not needed.
   * @param tSortedColumnSourceName use "" if not known or not needed.
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
      String columnSeparator,
      int tReloadEveryNMinutes,
      String tPreExtractRegex,
      String tPostExtractRegex,
      String tExtractRegex,
      String tColumnNameForExtract,
      String tSortedColumnSourceName,
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
        "EDDTableFromAsciiFiles.generateDatasetsXml"
            + "\nsampleFileName="
            + sampleFileName
            + "\ncharset="
            + charset
            + " colNamesRow="
            + columnNamesRow
            + " firstDataRow="
            + firstDataRow
            + " columnSeparator="
            + String2.annotatedString(columnSeparator)
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
            + "\nsortedColumn="
            + tSortedColumnSourceName
            + " sortFilesBy="
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
    tColumnNameForExtract =
        String2.isSomething(tColumnNameForExtract) ? tColumnNameForExtract.trim() : "";
    tSortedColumnSourceName =
        String2.isSomething(tSortedColumnSourceName) ? tSortedColumnSourceName.trim() : "";
    if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440; // 1440 works well with suggestedUpdateEveryNMillis
    if (!String2.isSomething(sampleFileName))
      String2.log(
          "Found/using sampleFileName="
              + (sampleFileName =
                  FileVisitorDNLS.getSampleFileName(
                      tFileDir, tFileNameRegex, true, ".*"))); // recursive, pathRegex
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();
    if (charset == null || charset.length() == 0) charset = File2.ISO_8859_1;
    dataSourceTable.readASCII(
        sampleFileName,
        charset,
        "",
        "", // skipHeaderToRegex, skipLinesRegex,
        columnNamesRow - 1,
        firstDataRow - 1,
        columnSeparator,
        null,
        null,
        null,
        null,
        false); // simplify
    dataSourceTable.convertIsSomething2(); // convert e.g., "N/A" to ""
    dataSourceTable.simplify();
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
      PrimitiveArray sourcePA = dataSourceTable.getColumn(col);

      // dateTime?
      Attributes addAtts = new Attributes();
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

      Attributes sourceAtts = dataSourceTable.columnAttributes(col);
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

      // add to dataAddTable
      dataAddTable.addColumn(col, colName, destPA, addAtts);

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(colName, destPA, sourceAtts, addAtts);

      // files are likely sorted by first date time variable
      // and no harm if files aren't sorted that way
      if (tSortedColumnSourceName.length() == 0 && isDateTime && !dateTimeAlreadyFound) {
        dateTimeAlreadyFound = true;
        tSortedColumnSourceName = colName;
      }
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
    if (tSortFilesBySourceNames.length() == 0) {
      if (tColumnNameForExtract.length() > 0
          && tSortedColumnSourceName.length() > 0
          && !tColumnNameForExtract.equals(tSortedColumnSourceName))
        tSortFilesBySourceNames = tColumnNameForExtract + ", " + tSortedColumnSourceName;
      else if (tColumnNameForExtract.length() > 0) tSortFilesBySourceNames = tColumnNameForExtract;
      else tSortFilesBySourceNames = tSortedColumnSourceName;
    }
    sb.append(
        "<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n"
            + "  below, notably 'units' for each of the dataVariables. -->\n"
            + "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\""
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
            + "    <standardizeWhat>"
            + tStandardizeWhat
            + "</standardizeWhat>\n"
            + "    <charset>"
            + charset
            + "</charset>\n"
            + "    <columnSeparator>"
            + XML.encodeAsXML(columnSeparator)
            + "</columnSeparator>\n"
            + "    <columnNamesRow>"
            + columnNamesRow
            + "</columnNamesRow>\n"
            + "    <firstDataRow>"
            + firstDataRow
            + "</firstDataRow>\n"
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
                    + tColumnNameForExtract
                    + "</columnNameForExtract>\n"
                : "")
            + "    <sortedColumnSourceName>"
            + XML.encodeAsXML(tSortedColumnSourceName)
            + "</sortedColumnSourceName>\n"
            + "    <sortFilesBySourceNames>"
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

  /**
   * This generates a chunk of datasets.xml for one or more ERDDAP datasets from the main entity (or
   * for all of the children) in an inport.xml file. This will not throw an exception. 2017-08-09 I
   * switched from old /inport/ to new /inport-xml/ .
   *
   * @param xmlFileName the URL or full file name of an InPort XML file. If it's a URL, it will be
   *     stored in tInputXmlDir.
   * @param tInputXmlDir The directory that is/will be used to store the input-xml file. If
   *     specified and if it doesn't exist, it will be created.
   * @return content for datasets.xml. Error messages will be included as comments.
   */
  public static String generateDatasetsXmlFromInPort(
      String xmlFileName, String tInputXmlDir, String typeRegex, int tStandardizeWhat) {

    String main = null;
    try {
      main =
          generateDatasetsXmlFromInPort(
                  xmlFileName, tInputXmlDir, typeRegex, 0, "", "", tStandardizeWhat)
              + "\n";
    } catch (Throwable t) {
      String msg = MustBe.throwableToString(t);
      return "<!-- " + String2.replaceAll(msg, "--", "- - ") + " -->\n\n";
    }

    StringBuilder children = new StringBuilder();
    for (int whichChild = 1; whichChild < 10000; whichChild++) {
      try {
        children.append(
            generateDatasetsXmlFromInPort(
                    xmlFileName, tInputXmlDir, typeRegex, whichChild, "", "", tStandardizeWhat)
                + "\n");
      } catch (Throwable t) {
        String msg = MustBe.throwableToString(t);
        if (msg.indexOf("ERROR: whichChild=") < 0 || msg.indexOf(" not found as ") < 0) msg = "";
        else msg = "<!-- " + String2.replaceAll(msg, "--", "- - ") + " -->\n\n";
        return whichChild > 1 ? children.toString() + msg : main + msg;
      }
    }
    return children.toString();
  }

  public static String convertInportTimeToIso8601(String time) {
    // Field: start-date-time (just below this:
    // https://inport.nmfs.noaa.gov/inport/help/xml-loader#time-frames )
    // Says for time: "The value must be specified in the following format: YYYYMMDDTHHMMSS.FFFZ"
    // "A time zone component is optional. If no time zone is provided, UTC is assumed."
    time = Calendar2.expandCompactDateTime(time);
    // now e.g., 2017-08-23T00:00:00.000  (23 chars)
    if (time.length() >= 13
        && // has hour value
        time.charAt(10) == 'T'
        && time.charAt(time.length() - 1) != 'Z'
        && // no Z
        time.substring(11).indexOf('-') < 0
        && // no trailing e.g., -05:00 time zone after T
        time.substring(11).indexOf('+') < 0
        && // no trailing e.g., +05:00 time zone after T
        time.length() <= 23) // not longer than example above
    time += "Z";
    return time;
  }

  /**
   * This generates a chunk of datasets.xml for one ERDDAP dataset from the info for the main info
   * (or one of the children) in an inport.xml file. 2017: The inport-xml loader documentation
   * (which is related by not identical to inport-xml) is at
   * https://inport.nmfs.noaa.gov/inport/help/xml-loader#inport-metadata Because the
   * &lt;downloads&gt;&lt;download&gt;&lt;/download-url&gt;'s are unreliable even when present, this
   * just notes the URL in &gt;sourceUrl&lt;, but doesn't attempt to download the file.
   *
   * @param xmlFileName the URL or full file name of an InPort XML file. If it's a URL, it will be
   *     stored in tInputXmlDir.
   * @param tInputXmlDir The directory that is/will be used to store the input-xml file. If
   *     specified and if it doesn't exist, it will be created.
   * @param typeRegex e.g., "(Entity|Data Set)". Other types are rarer and not useful for ERDDAP:
   *     Document, Procedure, Project.
   * @param whichChild If whichChild=0, this will create a dataVariables for columns described by
   *     the high-level data-attributes (if any). If whichChild is &gt;0, this method will include
   *     dataVariable definitions from the specified entity-attribute, distribution, and/or
   *     child-item (1, 2, 3, ...). IF &gt;1 IS USED, THIS ASSUMES THAT THEY ARE DEFINED IN
   *     PARALLEL! If the specified whichChild doesn't exist, this will throw a RuntimeException. In
   *     all cases, the "sourceUrl" will specify the URL from where the data can be downloaded (if
   *     specified in the XML).
   * @param tBaseDataDir the base directory, to which item-id/ will be added. It that dir doesn't
   *     exist, it will be created. The dataFile, if specified, should be in that directory.
   * @param tDataFileName The name.ext of the data file name for this child, if known. It is okay if
   *     there is no entity-attribute info for this child.
   */
  public static String generateDatasetsXmlFromInPort(
      String xmlFileName,
      String tInputXmlDir,
      String typeRegex,
      int whichChild,
      String tBaseDataDir,
      String tDataFileName,
      int tStandardizeWhat)
      throws Throwable {

    String2.log(
        "\n*** inPortGenerateDatasetsXml(" + xmlFileName + ", whichChild=" + whichChild + ")");

    // check parameters
    Test.ensureTrue(whichChild >= 0, "whichChild must be >=0.");
    // whichChild can be found as an entity-attribute, distribution, and/or child-item
    boolean whichChildFound = false;

    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;

    if (String2.isSomething(tBaseDataDir)) {
      tBaseDataDir = File2.addSlash(File2.forwardSlashDir(tBaseDataDir));
      File2.makeDirectory(tBaseDataDir);
    }
    String tDataDir = null; // it should be set below

    // make tInputXmlDir
    if (String2.isSomething(tInputXmlDir)) File2.makeDirectory(tInputXmlDir);
    else tInputXmlDir = "";

    // if xmlFileName is truly remote, download it
    if (String2.isTrulyRemote(xmlFileName)) {
      if (tInputXmlDir.equals(""))
        throw new RuntimeException(
            "When the xmlFileName is a URL (but not an AWS S3 URL), you must specify the tInputXmlDir to store it in.");
      String destName = tInputXmlDir + File2.getNameAndExtension(xmlFileName);
      SSR.downloadFile(xmlFileName, destName, true); // tryToUseCompression
      String2.log("xmlFile saved as " + destName);
      xmlFileName = destName;
    }

    { // display what's in the .xml file
      String readXml[] = File2.readFromFile(xmlFileName, File2.UTF_8, 1);
      if (readXml[0].length() > 0) throw new RuntimeException(readXml[0]);
      if (whichChild == 0) {
        String2.log("Here's what is in the InPort .xml file:");
        String2.log(readXml[1]);
      }
    }

    int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    String catID = File2.getNameNoExtension(xmlFileName);
    if (!String2.isSomething(tInputXmlDir)) tInputXmlDir = "???";
    if (!String2.isSomething(tDataFileName)) tDataFileName = "???";

    // create tables to hold results
    Table sourceTable = new Table();
    Attributes sourceAtts = sourceTable.globalAttributes();
    boolean isCreator2 = false, isCreator3 = false;
    String creatorName2 = null, creatorName3 = null;
    String creatorEmail2 = null, creatorEmail3 = null;
    String creatorOrg2 = null, creatorOrg3 = null;
    String creatorUrl2 = null, creatorUrl3 = null;
    ;
    String metaCreatedBy = "???";
    String metaCreated = "???";
    String metaLastModBy = "???";
    String metaLastMod = "???";
    String acronym = "???"; // institution acronym
    String title = "???";
    String securityClass = "";
    // accumulate results from some tags
    StringBuilder background = new StringBuilder();
    int nEntities = 0;
    String entityID = null;
    String entityPre = "";
    int nChildItems = 0;
    StringBuilder childItems = new StringBuilder();
    String childItemsPre = "";
    StringBuilder dataQuality = new StringBuilder();
    String dataQualityPre = "InPort_data_quality_";
    int nDistributions = 0;
    StringBuilder distribution = new StringBuilder();
    String distPre = "";
    // String distID = null;
    String distUrl = null;
    String distName = null;
    String distType = null;
    String distStatus = null;
    int nFaqs = 0;
    StringBuilder faqs = new StringBuilder();
    String faqsPre = "";
    StringBuilder history = new StringBuilder();
    int nIssues = 0;
    StringBuilder issues = new StringBuilder();
    String issuesPre = "";
    HashSet<String> keywords = new HashSet();
    StringBuilder license = new StringBuilder();
    int lineageSourceN = 0;
    String lineageStepN = "", lineageName = "", lineageEmail = "", lineageDescription = "";
    String sep = ","; // a common separator between items on a line
    String tSourceUrl = "(local files)";
    StringBuilder summary = new StringBuilder();
    int nSupportRoles = 0;
    String supportRolesPre = "";
    int nUrls = 0;
    String pendingUrl = "?";
    StringBuilder urls = new StringBuilder();
    String urlsPre = "";

    // HashMap<String,String> child0RelationHM = new HashMap(); //for whichChild=0
    // child0RelationHM.put("HI", "child");
    // child0RelationHM.put("RI", "other");

    // HashMap<String,String> childNRelationHM = new HashMap(); //for whichChild>0
    // childNRelationHM.put("HI", "sibling");
    // childNRelationHM.put("RI", "other");

    // attributes that InPort doesn't help with
    Attributes gAddAtts = new Attributes();

    // This used to assume xml files were stored in "/u00/data/points/inportXml/"
    // try to have this work if files are not in that directory.
    String metadataPath;
    if (xmlFileName.startsWith("/u00/data/points/inportXml/")) {
      metadataPath = xmlFileName.substring("/u00/data/points/inportXml/".length());
    } else {
      metadataPath = xmlFileName.substring(tInputXmlDir.length());
    }
    String inportXmlUrl = "https://inport.nmfs.noaa.gov/inport-metadata/" + metadataPath;
    gAddAtts.add("cdm_data_type", "Other");
    gAddAtts.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
    gAddAtts.add("infoUrl", inportXmlUrl);
    gAddAtts.add("InPort_xml_url", inportXmlUrl);
    gAddAtts.add("keywords_vocabulary", "GCMD Science Keywords");
    gAddAtts.add("standard_name_vocabulary", "CF Standard Name Table v55");

    // process the inport.xml file
    SimpleXMLReader xmlReader =
        new SimpleXMLReader(File2.getDecompressedBufferedInputStream(xmlFileName));
    xmlReader.nextTag();
    String tags = xmlReader.allTags();
    String startTag = "<inport-metadata>"; // 2017-08-09 version 0.9
    Test.ensureEqual(tags, startTag, "Unexpected first tag");
    int startTagLength = startTag.length();

    // process the tags
    while (true) {
      xmlReader.nextTag();
      tags = xmlReader.allTags();
      int nTags = xmlReader.stackSize();
      String content = xmlReader.content();
      if (xmlReader.stackSize() == 1) break; // the startTag
      String topTag = xmlReader.tag(nTags - 1);
      boolean hasContent = String2.isSomething2(content);
      tags = tags.substring(startTagLength);
      if (debugMode) String2.log(">>  tags=" + tags + content);
      String attTags =
          tags.startsWith("<entity-attribute-information><entity><data-attributes><data-attribute>")
              ? tags.substring(71)
              : null;

      // special cases: convert some InPort names to ERDDAP names
      // The order here matches the order in the files.

      // item-identification
      if (tags.startsWith("<item-identification>")) {
        if (tags.endsWith("</catalog-item-id>")) {
          Test.ensureEqual(content, catID, "catalog-item-id != fileName");
          gAddAtts.add("InPort_item_id", content);
          if (String2.isSomething(tBaseDataDir)) tDataDir = tBaseDataDir + content + "/";

        } else if (tags.endsWith("</title>") && hasContent) {
          title = content;
          // </short-name>
        } else if (tags.endsWith("</catalog-item-type>") && hasContent) {
          // tally: Entity: 4811, Data Set: 2065, Document: 168,
          //  Procedure: 113, Project: 29
          if (!content.matches(typeRegex)) {
            String2.log(
                String2.ERROR
                    + ": Skipping this item because "
                    + "the catalog-item-type doesn't match the typeRegex.");
            return "";
          }
          gAddAtts.add("InPort_item_type", content); // e.g., Data Set
        } else if (tags.endsWith("</metadata-workflow-state>") && hasContent) {
          // this may be overwritten by child below
          gAddAtts.add("InPort_metadata_workflow_state", content); // e.g., Published / External
        } else if (tags.endsWith("</parent-catalog-item-id>") && hasContent) {
          gAddAtts.add("InPort_parent_item_id", content);
        } else if (tags.endsWith("</parent-title>") && hasContent) {
          // parent-title is useful as precursor to title
          title = content + (title.endsWith("???") ? "" : ", " + title);
        } else if (tags.endsWith("</status>") && hasContent) {
          // this may be overwritten by child below
          gAddAtts.add("InPort_status", content); // e.g., Complete
        } else if (tags.endsWith("</abstract>") && hasContent) {
          String2.ifSomethingConcat(summary, "\n\n", content);
        } else if (tags.endsWith("</purpose>") && hasContent) {
          String2.ifSomethingConcat(summary, "\n\n", content);
          // </notes>  inport editing notes
        }

        // keywords see 10657
      } else if (tags.equals("<keywords><keyword></keyword>") && hasContent) {
        chopUpCsvAddAllAndParts(content, keywords);

        // physical-location
        //   <physical-location>
        //      <organization>National Marine Mammal Laboratory</organization>
        //      <city>Seattle</city>
        //      <state-province>WA</state-province>
        //      <country>United States</country>
        //   </physical-location>
        // ???            } else if (tags.equals("<physical-location></organization>") &&
        // hasContent) {
        //   not really useful.  use as institution?!

        // data-set-information
        //  see /u00/data/points/inportXml/NOAA/NMFS/AFSC/inport-xml/xml/17275.xml
      } else if (tags.startsWith("<data-set-information>")) {
        if (tags.endsWith("</data-set-type>") && hasContent) {
          // Tally: Database: 176, CSV Files: 128, Oracle Database: 98,
          //  Mixed: 79, MS Excel Spreadsheet: 76, Files: 73,  Other: 47,
          //  GIS: 22, Access Database, spreadsheets: 21, SAS files: 19,
          //  Binary: 18, MS Access Database: 18, MS Excel : 15, JPG Files: 12,
          //  GIS dataset of raster files: 11, Excel and SAS Dataset: 10,
          //  Files (Word, Excel, PDF, etc.): 7, Website (url): 7,
          //  Excel, SAS, and Stata data sets: 5, Text files: 5, GIS database: 4,
          //  SAS data sets (version 7): 4, SQL Server Database: 4 ...
          background.append("> data-set type=" + content + "\n");
          gAddAtts.add("InPort_dataset_type", content);
        } else if (tags.endsWith("</maintenance-frequency>") && hasContent) {
          background.append("> data-set maintenance-frequency=" + content + "\n");
          gAddAtts.add("InPort_dataset_maintenance_frequency", content);
        } else if (tags.endsWith("</data-set-publication-status>") && hasContent) {
          background.append("> data-set publication-status=" + content + "\n");
          gAddAtts.add("InPort_dataset_publication_status", content);
        } else if (tags.endsWith("</publish-date>") && hasContent) {
          content = convertInportTimeToIso8601(content);
          background.append("> data-set publish-date=" + content + "\n");
          gAddAtts.add("InPort_dataset_publish_date", content);
        } else if (tags.endsWith("</data-presentation-form>") && hasContent) {
          background.append(
              "> data-set presentation-form=" + content + "\n"); // e.g., Table (digital)
          gAddAtts.add("InPort_dataset_presentation_form", content);
        } else if (tags.endsWith("</source-media-type>") && hasContent) {
          // Tally: online: 146, disc: 101, electronic mail system: 59,
          //  electronically logged: 48, computer program: 38, paper: 29,
          //  CD-ROM: 11, physical model: 3, chart: 2, videotape: 2, audiocassette: 1
          background.append("> data-set source-media-type=" + content + "\n");
          gAddAtts.add("InPort_dataset_source_media_type", content);
        } else if (tags.endsWith("</distribution-liability>") && hasContent) {
          license.append("Distribution Liability: " + content + "\n");
        } else if (tags.endsWith("</data-set-credit>") && hasContent) {
          gAddAtts.add("acknowledgment", content); // e.g., BOEM funded this research.
        } else if (tags.endsWith("</instrument>") && hasContent) {
          gAddAtts.add("instrument", content);
        } else if (tags.endsWith("</platform>") && hasContent) {
          gAddAtts.add("platform", content);
        } else if (tags.endsWith("</physical-collection-fishing-gear>") && hasContent) {
          gAddAtts.add("InPort_fishing_gear", content);
        }

        // entity-attribute-information
        //  see /u00/data/points/inportXml/NOAA/NMFS/AFSC/inport-xml/xml/36615.xml
      } else if (tags.startsWith("<entity-attribute-information><entity>")) {

        if (tags.equals("<entity-attribute-information><entity>")) {
          nEntities++;
          if (reallyVerbose)
            String2.log(
                "Since whichChild="
                    + whichChild
                    + ", I'm "
                    + (whichChild == 0 || nEntities == whichChild ? "processing" : "skipping")
                    + " <entity-attribute-information> for entity #"
                    + nEntities);
          if (whichChild == 0) {
            entityPre = "InPort_entity_" + nEntities + "_";
          } else if (nEntities == whichChild) {
            whichChildFound = true;
          } else {
            // skip this child
            xmlReader.skipToStackSize(xmlReader.stackSize());
          }

        } else if (attTags != null && whichChild > 0 && nEntities == whichChild) {
          // atts has tags after
          //  <entity-attribute-information><entity><data-attributes><data-attribute>
          // String2.log(">>attTags=" + attTags);
          int col = sourceTable.nColumns() - 1; // 0.. for actual columns
          Attributes varAddAtts = col >= 0 ? sourceTable.columnAttributes(col) : null;
          if (attTags.equals("")) {
            // the start: add the column
            varAddAtts = new Attributes();
            col++; // 0..
            String tName = "column" + col; // a placeholder
            sourceTable.addColumn(col, tName, new StringArray(), varAddAtts);

          } else if (attTags.equals("</name>") && hasContent) {
            sourceTable.setColumnName(col, content);

          } else if (attTags.equals("</data-storage-type>") && hasContent) {
            // not reliable or useful. Use simplify.

          } else if (attTags.equals("</null-value-meaning>") && hasContent) {
            // ??? Does this identify the null value (e.g., -999)
            //    or describe what is meant if there is no value???
            // from Tally:     0: 53, YES: 37, blank: 36, space or 0: 34,
            //  blank or 0: 31, null or: 30, Yes: 20, NO: 19, NA: 18,
            //  blank or -, space/null: 15, ...
            double imv = String2.parseInt(content);
            double dmv = String2.parseDouble(content);
            // if numeric, only 0 or other numeric values matter
            if (content.endsWith(" or 0")) varAddAtts.set("missing_value", 0);
            else if (imv < Integer.MAX_VALUE) varAddAtts.set("missing_value", imv);
            else if (Double.isFinite(dmv)) varAddAtts.set("missing_value", dmv);
            // for strings, it doesn't really matter
            // else if (content.indexOf("NA")   >= 0) varAddAtts.set("missing_value", "NA");
            // else if (content.indexOf("NULL") >= 0) varAddAtts.set("missing_value", "NULL");
            // else if (content.indexOf("null") >= 0) varAddAtts.set("missing_value", "null");

            // } else if (attTags.equals("</scale>") && hasContent) {
            //    //What is this? It isn't scale_factor.
            //    //from Tally: 0, 2, 1, 3, 5, 6, 9, 14, 8, 13, 12, 15, 9, ...
            //    varAddAtts.set("scale", content);

          } else if (attTags.equals("</max-length>") && hasContent) {
            // e.g., -1 (?!), 0(?!), 22, 1, 8, 100, 4000 (longest)
            int maxLen = String2.parseInt(content);
            if (maxLen > 0 && maxLen < 10000) varAddAtts.add("max_length", "" + maxLen);

            // } else if (attTags.equals("</is-pkey>") && hasContent) {
            // I think this loses its meaning in ERDDAP.
            //    varAddAtts.set("isPrimaryKey", content);

          } else if (attTags.equals("</units>") && hasContent) {
            // e.g., Decimal degrees, animal, KM, degrees celcius(sic), AlphaNumeric
            // <units>micromoles per kilogram</units>
            // These will be fixed up by makeReadyToUseAddVariableAttributes.
            varAddAtts.set("units", content);

          } else if (attTags.equals("</format-mask>") && hasContent) {
            // Thankfully, format-mask appears after units, so format-mask has precedence.
            // e.g., $999,999.99, MM/DD/YYYY, HH:MM:SS, mm/dd/yyyy, HHMM

            // if it's a date time format, convertToJavaDateTimeFormat e.g., yyyy-MM-dd'T'HH:mm:ssZ
            String newContent = Calendar2.convertToJavaDateTimeFormat(content);
            if (!newContent.equals(content)
                || // it was changed, so it is a dateTime format
                Calendar2.isStringTimeUnits(newContent)) {

              // These will be fixed up by makeReadyToUseAddVariableAttributes.
              varAddAtts.set("units", content);

              if (newContent.indexOf("yyyy") >= 0
                  && // has years
                  newContent.indexOf("M") >= 0
                  && // has month
                  newContent.indexOf("d") >= 0
                  && // has days
                  newContent.indexOf("H") < 0) // doesn't have hours
              varAddAtts.set("time_precision", "1970-01-01");
            } else {
              varAddAtts.set("format_mask", content);
            }

          } else if (attTags.equals("</description>") && hasContent) {
            // description -> comment
            // widely used  (Is <description> used another way?)
            if (content.toLowerCase().equals("month/day/year")) { // date format
              varAddAtts.set("units", "M/d/yyyy");
              varAddAtts.set("time_precision", "1970-01-01");
            } else {
              varAddAtts.set("comment", content);
            }

          } else if (attTags.equals("</allowed-values>") && hasContent) {
            // e.g., No domain defined., unknown, Free entry text field., text, "1, 2, 3", "False,
            // True"
            varAddAtts.set("allowed_values", content);

          } else if (attTags.equals("</derivation>") && hasContent) {
            // there are some
            varAddAtts.set("derivation", content);

          } else if (attTags.equals("</validation-rules>") && hasContent) {
            // there are some
            varAddAtts.set("validation_rules", content);
          }

          // *after* attTags processing, get <entity-attribute-information><entity></...> info
        } else if (xmlReader.stackSize() == 4) {
          if (tags.endsWith("</catalog-item-id>") && hasContent) {
            if (whichChild == 0) {
              gAddAtts.add(entityPre + "item_id", content);
              background.append("> entity #" + nEntities + " catalog-item-id=" + content + "\n");
            } else if (nEntities == whichChild) {
              entityID = content;
            }

          } else if (tags.endsWith("</title>") && hasContent) {
            if (whichChild == 0) gAddAtts.add(entityPre + "title", content);
            else if (nEntities == whichChild) title += ", " + content;

          } else if (tags.endsWith("</metadata-workflow-state>") && hasContent) {
            // overwrite parent info
            if (whichChild == 0) gAddAtts.add(entityPre + "metadata_workflow_state", content);
            else if (nEntities == whichChild)
              gAddAtts.add("InPort_metadata_workflow_state", content); // e.g., Published / External

          } else if (tags.endsWith("</status>") && hasContent) {
            // overwrite parent info
            if (whichChild == 0) gAddAtts.add(entityPre + "status", content);
            else if (nEntities == whichChild)
              gAddAtts.add("InPort_status", content); // e.g., Complete

          } else if (tags.endsWith("</abstract>") && hasContent) {
            if (whichChild == 0) gAddAtts.add(entityPre + "abstract", content);
            else if (nEntities == whichChild)
              String2.ifSomethingConcat(summary, "\n\n", "This sub-dataset has: " + content);

            // <notes> is InPort info, e.g., when/how uploaded
          }
        }
        // skip <entity-information><entity-type>Spreadsheet
        // skip <entity-information><description>...   same/similar to abstract

        // support-roles
        // Use role=Originator as backup for creator_name, creator_email
      } else if (tags.startsWith("<support-roles>")) {
        if (tags.equals("<support-roles>")) { // opening tag
          isCreator2 = false;
          isCreator3 = false;
          nSupportRoles++;
          supportRolesPre = "InPort_support_role_" + nSupportRoles + "_";
        } else if (tags.equals("<support-roles><support-role></support-role-type>") && hasContent) {
          isCreator2 = "Originator".equals(content); // often e.g., organization e.g., AFSC
          isCreator3 = "Point of Contact".equals(content); // often a person
          gAddAtts.add(supportRolesPre + "type", content);
        } else if (tags.equals("<support-roles><support-role></person>") && hasContent) {
          int po = content.indexOf(", "); // e.g., Clapham, Phillip
          if (po > 0) content = content.substring(po + 2) + " " + content.substring(0, po);
          if (isCreator2) creatorName2 = content;
          else if (isCreator3) creatorName3 = content;
          gAddAtts.add(supportRolesPre + "person", content);
        } else if (tags.equals("<support-roles><support-role></person-email>") && hasContent) {
          if (isCreator2) creatorEmail2 = content;
          else if (isCreator3) creatorEmail3 = content;
          gAddAtts.add(supportRolesPre + "person_email", content);
        } else if (tags.equals("<support-roles><support-role></organization>") && hasContent) {
          if (isCreator2) creatorOrg2 = content;
          else if (isCreator3) creatorOrg3 = content;
          gAddAtts.add(supportRolesPre + "organization", content);
        } else if (tags.equals("<support-roles><support-role></organization-url>") && hasContent) {
          if (isCreator2) creatorUrl2 = content;
          else if (isCreator3) creatorUrl3 = content;
          gAddAtts.add(supportRolesPre + "organization_url", content);
        }

        // extent  geo
      } else if (tags.startsWith("<extents><extent><geographic-areas><geographic-area>")) {
        if (tags.endsWith("</west-bound>") && hasContent)
          gAddAtts.add("geospatial_lon_min", String2.parseDouble(content));
        else if (tags.endsWith("</east-bound>") && hasContent)
          gAddAtts.add("geospatial_lon_max", String2.parseDouble(content));
        else if (tags.endsWith("</north-bound>") && hasContent)
          gAddAtts.add("geospatial_lat_max", String2.parseDouble(content));
        else if (tags.endsWith("</south-bound>") && hasContent)
          gAddAtts.add("geospatial_lat_min", String2.parseDouble(content));

        // extent time-frame
      } else if (tags.startsWith("<extents><extent><time-frames><time-frame>")) {
        if (tags.endsWith("</start-date-time>") && hasContent) {
          gAddAtts.add("time_coverage_begin", convertInportTimeToIso8601(content));
        } else if (tags.endsWith("</end-date-time>") && hasContent) {
          gAddAtts.add("time_coverage_end", convertInportTimeToIso8601(content));
        }

        // access-information
      } else if (tags.startsWith("<access-information>")) {
        if (tags.endsWith("</security-class") && hasContent) {
          license.append("Security class: " + content + "\n");
          gAddAtts.add("InPort_security_class", content);
          securityClass = content;
        } else if (tags.endsWith("</security-classification") && hasContent) {
          license.append("Security classification: " + content + "\n");
          gAddAtts.add("InPort_security_classification", content);
        } else if (tags.endsWith("</security-handling-description") && hasContent) {
          license.append("Security handling description: " + content + "\n");
        } else if (tags.endsWith("</data-access-policy>") && hasContent) {
          license.append("Data access policy: " + content + "\n");
        } else if (tags.endsWith("</data-access-procedure>") && hasContent) {
          license.append("Data access procedure: " + content + "\n");
        } else if (tags.endsWith("</data-access-constraints>") && hasContent) {
          license.append("Data access constraints: " + content + "\n");
        } else if (tags.endsWith("</data-use-constraints>") && hasContent) {
          license.append("Data use constraints: " + content + "\n");
        } else if (tags.endsWith("</security-classification-system>") && hasContent) {
          license.append(
              "Security classification system: "
                  + content
                  + "\n"); // all kinds of content and e.g., None
        } else if (tags.endsWith("</metadata-access-constraints>") && hasContent) {
          license.append("Metadata access constraints: " + content + "\n");
        } else if (tags.endsWith("</metadata-use-constraints>") && hasContent) {
          license.append("Metadata use constraints: " + content + "\n");
        }

        // distribution-information
        //  see /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
      } else if (tags.startsWith("<distribution-information>")) {
        if (tags.endsWith("<distribution>")) {
          // start of a distribution
          nDistributions++;

          if (reallyVerbose)
            String2.log(
                "Since whichChild="
                    + whichChild
                    + ", I'm "
                    + (whichChild == 0 || nDistributions == whichChild ? "processing" : "skipping")
                    + " <distribution-information> for entity #"
                    + nDistributions);
          if (whichChild == 0 || nDistributions == whichChild) {
            if (whichChild > 0) whichChildFound = true;
            distPre = "InPort_distribution_" + (whichChild > 0 ? "" : nDistributions + "_");
            // distID = xmlReader.attributeValue("cc-id"); //skip cc-id: it's an internalDB
            // identifier
            distUrl = null;
            distName = null;
            distType = null;
            distStatus = null;
            // gAddAtts.add(distPre + "cc_id", distID);
          } else {
            // skip this child
            xmlReader.skipToStackSize(xmlReader.stackSize());
          }
        } else if (tags.endsWith("</download-url>") && hasContent) {
          distUrl = content;
          if (nDistributions == whichChild) tSourceUrl = content;
          gAddAtts.add(distPre + "download_url", content);

        } else if (tags.endsWith("</file-name>") && hasContent) {
          distName = content;
          gAddAtts.add(distPre + "file_name", content);
        } else if (tags.endsWith("</file-type>") && hasContent) {
          distType = content;
          gAddAtts.add(distPre + "file_type", content);
          // seip fgdc-content-type, file-size (in MB?)
        } else if (tags.endsWith("</review-status>") && hasContent) {
          distStatus = content;
          gAddAtts.add(distPre + "review_status", content);
        } else if (tags.endsWith("</distribution>") && distUrl != null) {
          // end of a distribution
          String msg =
              "Distribution"
                  + // " cc-id=" + distID +
                  (distName == null ? "" : sep + " file-name=" + distName)
                  + (distType == null ? "" : sep + " file-type=" + distType)
                  + (distStatus == null ? "" : sep + " review-status=" + distStatus)
                  + sep
                  + " download-url="
                  + distUrl
                  + "\n";
          distribution.append(msg);
          background.append("> #" + nDistributions + ": " + msg);
        }

        // urls
        //  see /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
        //   <urls>
        //      <url cc-id="223838">
        //
        // <url>https://alaskafisheries.noaa.gov/arcgis/rest/services/bathy_40m/MapServer</url>
        //         <url-type>Online Resource</url-type>
        //         <description>REST Service</description>
        //      </url>
        //   </urls>
      } else if (tags.startsWith("<urls>")) {
        if (tags.equals("<urls><url>")) {
          urls.append("URL #" + ++nUrls);
          // skip cc-id: it's an internal DB identifier
          pendingUrl = "?";
          urlsPre = "InPort_url_" + nUrls + "_";
        } else if (tags.equals("<urls><url></url>") && hasContent) {
          pendingUrl = content;
          gAddAtts.add(urlsPre + "url", content);
        } else if (tags.equals("<urls><url></url-type>") && hasContent) {
          urls.append(sep + " type=" + content);
          gAddAtts.add(urlsPre + "type", content);
        } else if (tags.equals("<urls><url></description>") && hasContent) {
          urls.append(sep + " description=" + content);
          gAddAtts.add(urlsPre + "description", content);
        } else if (tags.equals("<urls></url>")) {
          urls.append(sep + " url=" + pendingUrl + "\n");
        }

        // activity-logs  /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
        // just inport activity?

        // issues see /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
        //   <issues>
        //      <issue cc-id="223840">
        //         <issue-date>2013</issue-date>
        //         <author>Lewis, Steve</author>
        //         <issue>Outlier removal processes</issue>
        //      </issue>
        //   </issues>
      } else if (tags.startsWith("<issues><")) {
        if (tags.equals("<issues><issue>")) {
          issues.append("Issue #" + ++nIssues);
          issuesPre = "InPort_issue_" + nIssues + "_";
          // skip cc-id: it's an internal DB identifier
        } else if (tags.equals("<issues><issue></issue-date>") && hasContent) {
          content = convertInportTimeToIso8601(content);
          issues.append(": date=" + content);
          gAddAtts.add(issuesPre + "date", content);
        } else if (tags.equals("<issues><issue></author>") && hasContent) {
          int po = content.indexOf(", "); // e.g., Clapham, Phillip
          if (po > 0) content = content.substring(po + 2) + " " + content.substring(0, po);
          issues.append(", author=" + content);
          gAddAtts.add(issuesPre + "author", content);
        } else if (tags.equals("<issues><issue></issue>") && hasContent) {
          issues.append(sep + " issue=" + content);
          gAddAtts.add(issuesPre + "issue", content);
        } else if (tags.equals("<issues></issue>")) {
          String2.addNewlineIfNone(issues);
        }

        // technical-environment
        // /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
        //   <technical-environment>
        //      <description>In progress.</description>
        //   </technical-environment>
      } else if (tags.equals("<technical-environment></description>") && hasContent) {
        gAddAtts.add("InPort_technical_environment", content);

        // data-quality   /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
      } else if (tags.startsWith("<data-quality>")) {
        if (tags.endsWith("</representativeness>") && hasContent) {
          dataQuality.append("* Representativeness: " + content + "\n");
          gAddAtts.add(dataQualityPre + "representativeness", content);
        } else if (tags.endsWith("</accuracy>") && hasContent) {
          dataQuality.append("* Accuracy: " + content + "\n");
          gAddAtts.add(dataQualityPre + "accuracy", content);
        } else if (tags.endsWith("</analytical-accuracy>") && hasContent) {
          dataQuality.append("* Analytical-accuracy: " + content + "\n");
          gAddAtts.add(dataQualityPre + "analytical_accuracy", content);
        } else if (tags.endsWith("</completeness-measure>") && hasContent) {
          dataQuality.append("* Completeness-measure: " + content + "\n");
          gAddAtts.add(dataQualityPre + "completeness_measure", content);
        } else if (tags.endsWith("</field-precision>") && hasContent) {
          dataQuality.append("* Field-precision: " + content + "\n");
          gAddAtts.add(dataQualityPre + "field_precision", content);
        } else if (tags.endsWith("</sensitivity>") && hasContent) {
          dataQuality.append("* Sensitivity: " + content + "\n");
          gAddAtts.add(dataQualityPre + "sensitivity", content);
        } else if (tags.endsWith("</quality-control-procedures>") && hasContent) {
          dataQuality.append("* Quality-control-procedures: " + content + "\n");
          gAddAtts.add(dataQualityPre + "control_procedures", content);
        }

        // data-management /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
      } else if (tags.startsWith("<data-management>")) {
        // there are several other attributes, but none of general interest
        //  <resources-identified>Yes</resources-identified>
        //  <resources-budget-percentage>Unknown</resources-budget-percentage>
        //  <data-access-directive-compliant>Yes</data-access-directive-compliant>
        //  <data-access-directive-waiver>No</data-access-directive-waiver>
        //  <hosting-service-needed>No</hosting-service-needed>
        //  <delay-collection-dissemination>1 year</delay-collection-dissemination>
        //
        // <delay-collection-dissemination-explanation>NA</delay-collection-dissemination-explanation>
        //  <archive-location>Other</archive-location>
        //  <archive-location-explanation-other>yes</archive-location-explanation-other>
        //  <delay-collection-archive>NA</delay-collection-archive>
        //  <data-protection-plan>NA</data-protection-plan>
        if (tags.equals("<data-management></archive-location>") && hasContent) {
          gAddAtts.add("archive_location", content);
          history.append("archive_location=" + content + "\n"); // e.g. NCEI
        }

        // lineage-statement   /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
      } else if (tags.startsWith("<lineage></lineage-statement>") && hasContent) {
        history.append("Lineage Statement: " + content + "\n");

        // lineage-sources, good example:
        // /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/25229.xml
      } else if (tags.startsWith("<lineage><lineage-sources>")) {
        if (tags.endsWith("<lineage-source>")) { // start of
          history.append("Lineage Source #" + ++lineageSourceN);
        } else if (tags.endsWith("</citation-title>") && hasContent) {
          history.append(sep + " title=" + content);
        } else if (tags.endsWith("</originator-publisher>") && hasContent) {
          history.append(sep + " publisher=" + content);
        } else if (tags.endsWith("</publish-date>") && hasContent) {
          history.append(sep + " date published=" + convertInportTimeToIso8601(content));
        } else if (tags.endsWith("</citation>") && hasContent) {
          history.append(sep + " citation=" + content);
        } else if (tags.endsWith("</lineage-source>")) {
          history.append("\n");
        }

        // lineage-process-steps
      } else if (tags.startsWith("<lineage><lineage-process-steps>")) {
        if (tags.endsWith("<lineage-process-step>")) { // start of step
          lineageStepN = null;
          lineageName = null;
          lineageEmail = null;
          lineageDescription = null;
        } else if (tags.endsWith("</sequence-number>") && hasContent) {
          lineageStepN = content;
        } else if (tags.endsWith("</description>") && hasContent) {
          lineageDescription = content;
        } else if (tags.endsWith("</process-contact>") && hasContent) {
          lineageName = content;
        } else if (tags.endsWith("</email-address>") && hasContent) {
          lineageEmail = content;
        } else if (tags.endsWith("</lineage-process-step>")
            && // end of step
            (lineageName != null || lineageEmail != null || lineageDescription != null)) {
          history.append(
              "Lineage Step #"
                  + (lineageStepN == null ? "?" : lineageStepN)
                  + (lineageName == null ? "" : ", " + lineageName)
                  + (lineageEmail == null ? "" : " <" + lineageEmail + ">")
                  + (lineageDescription == null ? "" : ": " + lineageDescription)
                  + "\n");
        }

        // child-items
        // If whichChild == 0, add this info to childItems.
      } else if (tags.startsWith("<child-items>") && whichChild == 0) {
        if (tags.equals("<child-items><child-item>")) {
          // a new child-item
          nChildItems++;
          if (reallyVerbose)
            String2.log(
                "Since whichChild="
                    + whichChild
                    + ", I'm "
                    + (whichChild == 0 || nChildItems == whichChild ? "processing" : "skipping")
                    + " <child-item> for entity #"
                    + nChildItems);
          if (whichChild == 0 || nChildItems == whichChild) {
            if (whichChild > 0) whichChildFound = true;
            childItemsPre = "InPort_child_item_" + (whichChild > 0 ? "" : nChildItems + "_");
          } else {
            // skip this child
            xmlReader.skipToStackSize(xmlReader.stackSize());
          }

        } else if (tags.equals("<child-items><child-item></catalog-item-id>")) {
          childItems.append(
              "Child Item #" + nChildItems + ": item-id=" + (hasContent ? content : "?"));
          background.append("> child-item #" + nChildItems + " catalog-item-id=" + content + "\n");
          if (hasContent) gAddAtts.add(childItemsPre + "catalog_id", content);
        } else if (tags.equals("<child-items><child-item></catalog-item-type>") && hasContent) {
          childItems.append(sep + " item-type=" + content + "\n"); // e.g., Entity
          gAddAtts.add(childItemsPre + "item_type", content);
        } else if (tags.equals("<child-items><child-item></title>") && hasContent) {
          childItems.append("Title: " + content + "\n");
          gAddAtts.add(childItemsPre + "title", content);
        } else if (tags.equals("<child-items></child-item>")) {
          childItems.append('\n');
        }

        // faqs    /u00/data/points/inportXml/NOAA/NMFS/AKRO/inport-xml/xml/27377.xml
        //   <faqs>
        //      <faq cc-id="223844">
        //         <date>20150922</date>
        //         <author>Lewis, Steve </author>
        //         <question>can this dataset be used for navigation.</question>
        //         <answer>No.</answer>
        //      </faq>
        //   </faqs>
      } else if (tags.startsWith("<faqs>")) {
        if (tags.equals("<faqs><faq>")) {
          faqs.append("FAQ #" + ++nFaqs);
          faqsPre = "InPort_faq_" + nFaqs + "_";
          // skip cc-id: it's an internal DB identifier
        } else if (tags.equals("<faqs><faq></date>") && hasContent) {
          content = convertInportTimeToIso8601(content);
          faqs.append(": date=" + content);
          gAddAtts.add(faqsPre + "date", content);
        } else if (tags.equals("<faqs><faq></author>") && hasContent) {
          int po = content.indexOf(", "); // e.g., Clapham, Phillip
          if (po > 0) content = content.substring(po + 2) + " " + content.substring(0, po);
          faqs.append(", author=" + content + "\n");
          gAddAtts.add(faqsPre + "author", content);
        } else if (tags.equals("<faqs><faq></question>") && hasContent) {
          keywords.add("faq");
          String2.addNewlineIfNone(faqs).append("Question: " + content + "\n");
          // insert extra _ so it sorts before "answer"
          gAddAtts.add(faqsPre + "_question", content);
        } else if (tags.equals("<faqs><faq></answer>") && hasContent) {
          String2.addNewlineIfNone(faqs).append("Answer: " + content + "\n");
          gAddAtts.add(faqsPre + "answer", content);
        } else if (tags.equals("<faqs></faq>")) {
          String2.addNewlineIfNone(faqs).append("\n");
        }

        // catalog-details
      } else if (tags.startsWith("<catalog-details>")) {

        if (tags.endsWith("</metadata-record-created-by>") && hasContent) {
          metaCreatedBy = content; // e.g., SysAdmin ...
          gAddAtts.add("InPort_metadata_record_created_by", content);
        } else if (tags.endsWith("</metadata-record-created>") && hasContent) {
          content = convertInportTimeToIso8601(content); // e.g., 20160518T185232
          metaCreated = content;
          gAddAtts.add("InPort_metadata_record_created", content);
        } else if (tags.endsWith("</metadata-record-last-modified-by>") && hasContent) {
          metaLastModBy = content; // e.g., Renold Narita
          gAddAtts.add("InPort_metadata_record_last_modified_by", content);
        } else if (tags.endsWith("</metadata-record-last-modified>") && hasContent) {
          content = convertInportTimeToIso8601(content); // e.g., 20160518T185232
          metaLastMod = content;
          gAddAtts.add("InPort_metadata_record_last_modified", content);
        } else if (tags.endsWith("</owner-organization-acronym>") && hasContent) {
          // Tally: AFSC: 382, NWFSC: 295, SEFSC: 292, PIFSC: 275, NEFSC: 120,
          //  SWFSC: 109, OST: 43, PIRO: 31, AKRO: 30, GARFO: 23, SERO: 14,
          //  WCRO: 11, OHC: 10, GSMFC: 8, OPR: 1, OSF: 1
          gAddAtts.add(
              "institution",
              xmlFileName.indexOf("/NOAA/NMFS/") > 0
                  ? "NOAA NMFS " + content
                  : // e.g., SWFSC
                  xmlFileName.indexOf("/NOAA/") > 0 ? "NOAA " + content : content);
          acronym = content;
          gAddAtts.add("InPort_owner_organization_acronym", content);
        } else if (tags.endsWith("</publication-status>")) {
          Test.ensureEqual(content, "Public", "Unexpected <publication-status> content.");
          gAddAtts.add("InPort_publication_status", content);
        } else if (tags.endsWith("</is-do-not-publish>")) {
          // Tally: N: 3953 (100%) (probably because I harvested Public records)
          Test.ensureEqual(content, "No", "Unexpected <is-do-not-publish> content.");
        }

      } else {
        // log things not handled?
        // if (hasContent)
        // String2.log(" not handled: " + tags + " = " content);
      }
    }

    // desired whichChild not found?
    if (whichChild > 0 && !whichChildFound)
      throw new RuntimeException(
          "ERROR: whichChild="
              + whichChild
              + " not found as <entity-attribute-information>, "
              + "<distribution>, and/or <child-item>.");

    // cleanup creator info
    // String2.pressEnterToContinue(
    //    "creator_name=" + gAddAtts.get("creator_name") + ", " + creatorName2 + ", " + creatorName3
    // + "\n" +
    //    "creator_email=" + gAddAtts.get("creator_email") + ", " + creatorEmail2 + ", " +
    // creatorEmail3 + "\n");
    if (gAddAtts.get("creator_name") == null) {
      if (creatorName2 != null) {
        gAddAtts.set("creator_name", creatorName2);
        gAddAtts.set("creator_type", "person");
      } else if (creatorName3 != null) {
        gAddAtts.set("creator_name", creatorName3);
        gAddAtts.set("creator_type", "person");
      } else if (creatorOrg2 != null) {
        gAddAtts.set("creator_name", creatorOrg2);
        gAddAtts.set("creator_type", "institution");
      } else if (creatorOrg3 != null) {
        gAddAtts.set("creator_name", creatorOrg3);
        gAddAtts.set("creator_type", "institution");
      }
    }

    if (gAddAtts.get("creator_email") == null) {
      if (creatorEmail2 != null) gAddAtts.set("creator_email", creatorEmail2);
      else if (creatorEmail3 != null) gAddAtts.set("creator_email", creatorEmail3);
    }

    if (gAddAtts.get("creator_url") == null) {
      String cu = null;
      if (creatorUrl2 != null) {
        cu = creatorUrl2;
      } else if (creatorUrl3 != null) {
        cu = creatorUrl3;
      } else if (!acronym.equals("???")) {
        cu =
            "AFSC".equals(acronym)
                ? "https://www.fisheries.noaa.gov/region/alaska#science"
                : "AKRO".equals(acronym)
                    ? "https://www.fisheries.noaa.gov/region/alaska"
                    : "GARFO".equals(acronym)
                        ? "https://www.fisheries.noaa.gov/region/new-england-mid-atlantic"
                        : "GSMFC".equals(acronym)
                            ? "http://www.gsmfc.org/"
                            : "NEFSC".equals(acronym)
                                ? "https://www.fisheries.noaa.gov/region/new-england-mid-atlantic#science"
                                : "NWFSC".equals(acronym)
                                    ? "https://www.fisheries.noaa.gov/region/west-coast#northwest-science"
                                    : "OHC".equals(acronym)
                                        ? "https://www.fisheries.noaa.gov/topic/habitat-conservation"
                                        : "OPR".equals(acronym)
                                            ? "https://www.fisheries.noaa.gov/about/office-protected-resources"
                                            : "OSF".equals(acronym)
                                                ? "https://www.fisheries.noaa.gov/about/office-sustainable-fisheries"
                                                : "OST".equals(acronym)
                                                    ? "https://www.fisheries.noaa.gov/about/office-science-and-technology"
                                                    : "PIFSC".equals(acronym)
                                                        ? "https://www.fisheries.noaa.gov/region/pacific-islands#science"
                                                        : "PIRO".equals(acronym)
                                                            ? "https://www.fisheries.noaa.gov/region/pacific-islands"
                                                            : "SEFSC".equals(acronym)
                                                                ? "https://www.fisheries.noaa.gov/about/southeast-fisheries-science-center"
                                                                : "SERO".equals(acronym)
                                                                    ? "https://www.fisheries.noaa.gov/region/southeast"
                                                                    : "SWFSC".equals(acronym)
                                                                        ? "https://www.fisheries.noaa.gov/about/southwest-fisheries-science-center"
                                                                        : "WCRO".equals(acronym)
                                                                            ? "https://www.fisheries.noaa.gov/region/west-coast"
                                                                            : null;
      }
      if (cu != null) gAddAtts.add("creator_url", cu);
    }

    // dataQuality -- now done separately
    // if (dataQuality.length() > 0)
    //    gAddAtts.add("processing_level", dataQuality.toString().trim());  //an ACDD att

    // distribution -- now done separately
    // if (distribution.length() > 0)
    //    gAddAtts.add("InPort_distribution_information", distribution.toString().trim());

    // faqs -- now done separately
    // if (faqs.length() > 0)
    //    gAddAtts.add("InPort_faqs", faqs.toString().trim());

    // cleanup history (add to lineage info gathered above)
    if (!metaCreatedBy.equals("???"))
      history.append(
          metaCreated
              + " "
              + metaCreatedBy
              + " originally created InPort catalog-item-id #"
              + catID
              + ".\n");
    if (!metaLastModBy.equals("???"))
      history.append(
          metaLastMod
              + " "
              + metaLastModBy
              + " last modified InPort catalog-item-id #"
              + catID
              + ".\n");
    history.append(
        Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10)
            + " GenerateDatasetsXml in ERDDAP v"
            + EDStatic.erddapVersion
            + " (contact: erd.data@noaa.gov) converted "
            + "inport-xml metadata from "
            + inportXmlUrl
            + " into an ERDDAP dataset description.\n");
    gAddAtts.add("history", history.toString().trim());

    // issues -- now done separately
    // if (issues.length() > 0)
    //    gAddAtts.add("InPort_issues", issues.toString().trim());

    // cleanup keywords
    gAddAtts.add("keywords", String2.toCSSVString(keywords));

    // cleanup license
    if (license.indexOf("Security class: Unclassified") >= 0
        && // if Unclassified
        license.indexOf("Data access constraints: ") < 0
        && // and no other info
        license.indexOf("Data access policy: ") < 0
        && license.indexOf("Data use constraints: ") < 0) license.append("[standard]");
    else if (license.length() == 0) license.append("???");
    gAddAtts.add("license", license.toString().trim());

    // childItems -- now done separately
    // if (childItems.length() > 0)
    //    gAddAtts.add("InPort_child_items", childItems.toString().trim());

    gAddAtts.add("sourceUrl", tSourceUrl);

    gAddAtts.add("summary", summary.length() == 0 ? title : summary.toString().trim());

    // urls -- now done separately
    // if (urls.length() > 0)
    //    gAddAtts.add("InPort_urls", urls.toString().trim());

    // *** match to specified file?
    if (String2.isSomething(tDataDir)) {
      String msg;
      try {
        // ensure dir exists
        if (File2.isDirectory(tDataDir)) {
          msg = "> dataDir     =" + tDataDir + " already exists.";
          String2.log(msg);
          background.append(msg + "\n");
        } else {
          msg = "> creating dataDir=" + tDataDir;
          String2.log(msg);
          background.append(msg + "\n");
          File2.makeDirectory(tDataDir); // throws exception if trouble
        }

        // if a dataFileName was specified, read it
        if (!"???".equals(tDataFileName)) {
          msg = "> dataFileName=" + tDataDir + tDataFileName;
          String2.log(msg);
          background.append(msg + "\n");
          Table fileTable = new Table();
          fileTable.readASCII(
              tDataDir + tDataFileName,
              File2.ISO_8859_1,
              "",
              "",
              0,
              1,
              null,
              null,
              null,
              null,
              null,
              false); // simplify?  (see below)
          msg = "> dataFileTable columnNames=" + fileTable.getColumnNamesCSSVString();
          String2.log(msg);
          background.append(msg + "\n");

          if (sourceTable.nColumns() == 0) {
            // inport-xml had no entity-attributes, so just use the file as is
            for (int fcol = 0; fcol < fileTable.nColumns(); fcol++) {
              String colName = fileTable.getColumnName(fcol);
              Attributes atts = fileTable.columnAttributes(fcol);
              PrimitiveArray pa = fileTable.getColumn(fcol);
              sourceTable.addColumn(
                  fcol, colName, (PrimitiveArray) pa.clone(), (Attributes) atts.clone());
            }

          } else {
            // inport-xml had entity-attributes, try to match to names in ascii file
            BitSet matched = new BitSet();
            for (int icol = 0; icol < sourceTable.nColumns(); icol++) {
              String colName = sourceTable.getColumnName(icol);
              for (int fcol = 0; fcol < fileTable.nColumns(); fcol++) {
                String tryName = fileTable.getColumnName(fcol);
                if (String2.looselyEquals(colName, tryName)) {
                  matched.set(icol);
                  sourceTable.setColumn(icol, fileTable.getColumn(fcol));
                  if (!colName.equals(tryName)) {
                    msg =
                        "> I changed InPort entity attribute colName="
                            + colName
                            + " into ascii file colName="
                            + tryName;
                    String2.log(msg);
                    background.append(msg + "\n");
                    sourceTable.setColumnName(icol, tryName);
                  }
                  fileTable.removeColumn(fcol);
                  break;
                }
              }
            }
            if (sourceTable.nColumns() > 0 && matched.nextClearBit(0) == sourceTable.nColumns()) {
              msg = "> Very Good! All InPort columnNames matched columnNames in the fileTable.";
              String2.log(msg);
              background.append(msg + "\n");
            }

            // for colNames not matched, get admin to try to make a match
            for (int icol = 0; icol < sourceTable.nColumns(); icol++) {
              if (!matched.get(icol)) {
                String colName = sourceTable.getColumnName(icol);
                String actual =
                    String2.getStringFromSystemIn(
                        "Column name #"
                            + icol
                            + "="
                            + colName
                            + " isn't in the ASCII file.\n"
                            + "Enter one of these names:\n"
                            + fileTable.getColumnNamesCSSVString()
                            + "\n"
                            + "or press Enter to append '?' to the column name to signify it is unmatched.");
                if (actual.length() == 0) {
                  sourceTable.setColumnName(icol, colName + "?");
                } else {
                  int fcol = fileTable.findColumnNumber(actual);
                  if (fcol >= 0) {
                    fileTable.removeColumn(fcol);
                    sourceTable.setColumn(icol, fileTable.getColumn(fcol));
                  }
                  sourceTable.setColumnName(icol, actual);
                }
              }
            }
          }
        }
      } catch (Throwable t) {
        throw new RuntimeException(
            String2.ERROR + " while working with " + tDataDir + tDataFileName, t);
      }

      // ensure all column have same number of values
      // they won't be same size if not all columns matched above
      sourceTable.makeColumnsSameSize();
    }

    // *** end stuff
    boolean dateTimeAlreadyFound = false;
    String tSortedColumnSourceName = "";
    String tSortFilesBySourceNames = "";
    String tColumnNameForExtract = "";

    // clean up sourceTable
    sourceTable.convertIsSomething2(); // convert e.g., "N/A" to ""
    sourceTable.simplify();
    sourceTable.standardize(tStandardizeWhat);

    // make addTable
    Table addTable = new Table();
    for (int col = 0; col < sourceTable.nColumns(); col++) {
      String colName = String2.modifyToBeVariableNameSafe(sourceTable.getColumnName(col));
      // if (colName.matches("[A-Z0-9_]+"))  //all uppercase
      //    colName = colName.toLowerCase();
      addTable.addColumn(
          col,
          colName,
          (PrimitiveArray) sourceTable.getColumn(col).clone(),
          (Attributes) sourceTable.columnAttributes(col).clone()); // move from source to add
      sourceTable.columnAttributes(col).clear();
    }

    for (int col = 0; col < addTable.nColumns(); col++) {
      String colName = addTable.getColumnName(col);
      PrimitiveArray destPA = addTable.getColumn(col);
      // sourceAtts already in use as source global atts
      Attributes addAtts = addTable.columnAttributes(col);

      // then look for date columns
      String tUnits = addTable.columnAttributes(col).getString("units");
      if (tUnits == null) tUnits = "";
      if (tUnits.toLowerCase().indexOf("yy") >= 0 && destPA.elementType() != PAType.STRING) {
        // convert e.g., yyyyMMdd columns from int to String
        destPA = new StringArray(destPA);
        addTable.setColumn(col, destPA);
      }
      if (destPA.elementType() == PAType.STRING) {
        tUnits =
            Calendar2.suggestDateTimeFormat((StringArray) destPA, false); // evenIfPurelyNumeric
        if (tUnits.length() > 0) addAtts.set("units", tUnits);
        // ??? and if tUnits = "", set to ""???
      }
      boolean isDateTime = Calendar2.isTimeUnits(tUnits);

      addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              gAddAtts,
              sourceTable.columnAttributes(col),
              addAtts,
              sourceTable.getColumnName(col),
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true); // tryToFindLLAT

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(colName, destPA, sourceTable.columnAttributes(col), addAtts);

      // files are likely sorted by first date time variable
      // and no harm if files aren't sorted that way
      if (tSortedColumnSourceName.length() == 0 && isDateTime && !dateTimeAlreadyFound) {
        dateTimeAlreadyFound = true;
        tSortedColumnSourceName = colName;
      }
    }

    // tryToFindLLAT
    tryToFindLLAT(sourceTable, addTable);

    // *** makeReadyToUseGlobalAtts
    addTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                sourceAtts,
                hasLonLatTime(addTable) ? "Point" : "Other",
                "(local files)", // ???
                gAddAtts,
                suggestKeywords(sourceTable, addTable)));
    gAddAtts = addTable.globalAttributes();

    // subsetVariables
    if (sourceTable.globalAttributes().getString("subsetVariables") == null
        && addTable.globalAttributes().getString("subsetVariables") == null)
      gAddAtts.add(
          "subsetVariables", suggestSubsetVariables(sourceTable, addTable, true)); // 1file/dataset?

    StringBuilder defaultDataQuery = new StringBuilder();
    StringBuilder defaultGraphQuery = new StringBuilder();
    if (addTable.findColumnNumber(EDV.TIME_NAME) >= 0) {
      defaultDataQuery.append("&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
      defaultGraphQuery.append("&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
    }
    defaultGraphQuery.append("&amp;.marker=1|5");

    // use original title, with InPort # added
    gAddAtts.add(
        "title",
        title
            + " (InPort #"
            + catID
            + (whichChild == 0 ? "" : entityID != null ? "ce" + entityID : "c" + whichChild)
            + ")"); // catID ensures it is unique

    // fgdc and iso19115
    String fgdcFile = String2.replaceAll(xmlFileName, "/inport-xml/", "/fgdc/");
    String iso19115File = String2.replaceAll(xmlFileName, "/inport-xml/", "/iso19115/");
    if (!File2.isFile(fgdcFile)) fgdcFile = ""; // if so, don't serve an fgdc file
    if (!File2.isFile(iso19115File)) iso19115File = ""; // if so, don't serve an iso19115 file

    // write datasets.xml
    StringBuilder results = new StringBuilder();
    tDataDir = File2.addSlash(tDataDir);
    tDataDir = String2.replaceAll(tDataDir, "\\", "/");
    tDataDir = String2.replaceAll(tDataDir, ".", "\\.");
    tDataFileName = String2.replaceAll(tDataFileName, ".", "\\.");

    if (addTable.nColumns() == 0) {
      Attributes tAddAtts = new Attributes();
      tAddAtts.set("ioos_category", "Unknown");
      tAddAtts.set("missing_value", "???");
      tAddAtts.set("units", "???");
      sourceTable.addColumn(0, "noVariablesDefinedInInPort", new DoubleArray(), new Attributes());
      addTable.addColumn(0, "sampleDataVariable", new DoubleArray(), tAddAtts);
    }

    results.append(
        "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\""
            + acronym.toLowerCase()
            + "InPort"
            + catID
            + (whichChild == 0 ? "" : entityID != null ? "ce" + entityID : "c" + whichChild)
            + "\" active=\"true\">\n"
            + (defaultDataQuery.length() > 0
                ? "    <defaultDataQuery>" + defaultDataQuery + "</defaultDataQuery>\n"
                : "")
            + (defaultGraphQuery.length() > 0
                ? "    <defaultGraphQuery>" + defaultGraphQuery + "</defaultGraphQuery>\n"
                : "")
            + "    <fileDir>"
            + XML.encodeAsXML(tDataDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(tDataFileName)
            + "</fileNameRegex>\n"
            + "    <charset>ISO-8859-1</charset>\n"
            + "    <columnNamesRow>1</columnNamesRow>\n"
            + "    <firstDataRow>2</firstDataRow>\n"
            + "    <standardizeWhat>"
            + tStandardizeWhat
            + "</standardizeWhat>\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>-1</updateEveryNMillis>\n"
            + "    <accessibleViaFiles>true</accessibleViaFiles>\n"
            + "    <fgdcFile>"
            + fgdcFile
            + "</fgdcFile>\n"
            + "    <iso19115File>"
            + iso19115File
            + "</iso19115File>\n");
    results.append(writeAttsForDatasetsXml(false, sourceTable.globalAttributes(), "    "));
    results.append(cdmSuggestion());
    results.append(writeAttsForDatasetsXml(true, addTable.globalAttributes(), "    "));

    results.append(
        writeVariablesForDatasetsXml(
            sourceTable,
            addTable,
            "dataVariable",
            true,
            false)); // includeDataType, questionDestinationName
    results.append("</dataset>\n" + "\n");

    // background
    String2.log("\n-----");
    if (background.length() > 0) String2.log("Background for ERDDAP:\n" + background.toString());
    if (whichChild == 0)
      String2.log(
          "> nChildItems (with little info)="
              + nChildItems
              + ", nDistributions="
              + nDistributions
              + ", nEntities (with attribute info)="
              + nEntities
              + ", nUrls="
              + nUrls);
    if (!"Unclassified".equals(securityClass))
      String2.log("> WARNING! <security-class>=" + securityClass);
    String2.log("\n* generateDatasetsXmlFromInPort finished successfully.\n-----\n");
    return results.toString();
  }

  // Adam says "if there are non 7-bit ASCII chars in our JSON,
  //  they will be encoded as \\uxxxx"
  //  and tsv are "us-ascii".
  // so safe to use ISO_8859_1 or UTF_8 to decode them.
  public static final String bcodmoCharset = File2.ISO_8859_1;

  /**
   * This is a helper for generateDatasetsXmlFromBCODMO.
   *
   * @param ds
   * @param dsDir
   * @param name e.g., "parameters"
   * @param useLocalFilesIfPossible
   * @return a JSONArray (or null if 'name'_service) not defined
   */
  public static JSONArray getBCODMOSubArray(
      JSONObject ds, String dsDir, String name, boolean useLocalFilesIfPossible) throws Exception {

    try {
      String serviceName = name + "_service";
      if (!ds.has(serviceName)) return null;
      String subUrl = ds.getString(serviceName);
      if (!String2.isSomething(subUrl)) return null;
      String subFileName = dsDir + name + ".json";
      if (!useLocalFilesIfPossible || !File2.isFile(subFileName))
        SSR.downloadFile(subUrl, subFileName, true); // tryToCompress
      String subContent[] = File2.readFromFile(subFileName, bcodmoCharset);
      Test.ensureEqual(subContent[0], "", "");
      JSONTokener subTokener = new JSONTokener(subContent[1]);
      JSONObject subOverallObject = new JSONObject(subTokener);
      if (!subOverallObject.has(name)) return null;
      return subOverallObject.getJSONArray(name);
    } catch (Exception e) {
      String2.log("ERROR while getting " + name + "_service:\n" + MustBe.throwableToString(e));
      return null;
    }
  }

  /**
   * This makes chunks of datasets.xml for datasets from BCO-DMO. It gets info from a BCO-DMO JSON
   * service that Adam Shepherd (ashepherd at whoi.edu) set up for Bob Simons.
   *
   * @param useLocalFilesIfPossible
   * @param catalogUrl e.g., https://www.bco-dmo.org/erddap/datasets
   * @param baseDir e.g, /u00/data/points/bcodmo/
   * @param datasetNumberRegex .* for all or e.g., (549122|549123)
   * @throws Throwable if trouble with outer catalog, but individual dataset exceptions are caught
   *     and logged.
   */
  public static String generateDatasetsXmlFromBCODMO(
      boolean useLocalFilesIfPossible,
      String catalogUrl,
      String baseDir,
      String datasetNumberRegex,
      int tStandardizeWhat)
      throws Throwable {

    baseDir = File2.addSlash(baseDir); // ensure trailing slash
    String2.log(
        "\n*** EDDTableFromAsciiFiles.generateDatasetsXmlFromBCODMO\n"
            + "url="
            + catalogUrl
            + "\ndir="
            + baseDir);
    long time = System.currentTimeMillis();
    File2.makeDirectory(baseDir);
    StringBuffer results = new StringBuffer();
    StringArray noTime = new StringArray();
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;

    // for the sub files
    JSONArray subArray;
    JSONObject subObject;

    // get the main catalog and parse it
    String catalogName = baseDir + "catalog.json";
    if (!useLocalFilesIfPossible || !File2.isFile(catalogName))
      SSR.downloadFile(catalogUrl, catalogName, true); // tryToCompress
    String catalogContent[] = File2.readFromFile(catalogName, bcodmoCharset);
    Test.ensureEqual(catalogContent[0], "", "");
    JSONTokener tokener = new JSONTokener(catalogContent[1]);
    JSONObject catalogObject = new JSONObject(tokener);
    JSONArray datasetsArray = catalogObject.getJSONArray("datasets");

    int nMatching = 0;
    int nSucceeded = 0;
    int nFailed = 0;
    // Tally charTally = new Tally();
    String2.log("datasetsArray has " + datasetsArray.length() + " datasets.");
    for (int dsi = 0; dsi < datasetsArray.length(); dsi++) {
      try {
        JSONObject ds = datasetsArray.getJSONObject(dsi);

        // "dataset":"http:\/\/lod.bco-dmo.org\/id\/dataset\/549122",
        // Do FIRST to see if it matches regex
        String dsNumber = File2.getNameAndExtension(ds.getString("dataset")); // 549122
        if (!dsNumber.matches(datasetNumberRegex)) continue;
        String2.log("\nProcessing #" + dsi + ": bcodmo" + dsNumber);
        nMatching++;
        Attributes gatts = new Attributes();
        gatts.add("BCO_DMO_dataset_ID", dsNumber);

        // "version_date":"2015-02-17",
        String versionDate = "";
        String compactVersionDate = "";
        if (ds.has("version_date")) {
          versionDate = ds.getString("version_date");
          gatts.add("date_created", versionDate);
          gatts.add("version_date", versionDate);
          compactVersionDate = "v" + String2.replaceAll(versionDate, "-", "");
        }

        // Adam says dsNumber+compactVersionDate is a unique dataset identifier.
        String tDatasetID = "bcodmo" + dsNumber + compactVersionDate;
        String dsDir = baseDir + dsNumber + compactVersionDate + "/";
        File2.makeDirectory(dsDir);

        // standard things
        gatts.add("id", tDatasetID);
        gatts.add("naming_authority", "org.bco-dmo");
        gatts.add("institution", "BCO-DMO");
        gatts.add(
            "keywords",
            "Biological, Chemical, Oceanography, Data, Management, Office, " + "BCO-DMO, NSF");
        gatts.add("publisher_name", "BCO-DMO");
        gatts.add("publisher_email", "info@bco-dmo.org");
        gatts.add("publisher_type", "institution");
        gatts.add("publisher_url", "http://www.bco-dmo.org/");

        // "doi":"10.1575\/1912\/bco-dmo.641155",
        // Adam says "The uniqueness of records will be by this DOI
        //  (which is a proxy for the 'dataset' and 'version_date' keys combined)."
        if (ds.has("doi")) gatts.add("doi", ds.getString("doi"));

        // "landing_page":"http:\/\/www.bco-dmo.org\/dataset\/549122",
        if (ds.has("landing_page")) {
          gatts.add("infoUrl", ds.getString("landing_page"));
          // gatts.add("landing_page", ds.getString("landing_page"));
        }

        // "title":"Cellular elemental content of individual phytoplankton cells collected during US
        // GEOTRACES North Atlantic Transect cruises in the Subtropical western and eastern North
        // Atlantic Ocean during Oct and Nov, 2010 and Nov. 2011.",
        String tTitle =
            "BCO-DMO "
                + dsNumber
                + (versionDate.length() == 0 ? "" : " " + compactVersionDate)
                + (ds.has("title")
                    ? ": " + ds.getString("title")
                    : ds.has("dataset_name")
                        ? ": " + ds.getString("dataset_name")
                        : ds.has("brief_desc") ? ": " + ds.getString("brief_desc") : "");
        gatts.add("title", tTitle);

        // "description" is info from landing_page
        //  ??? I need to extract "Related references" and "related image files"
        //  from html tags in "description"
        // "abstract": different from Description on landing_page
        //  "Phytoplankton contribute significantly to global C
        //  cycling and serve as the base of ocean food webs.
        //  Phytoplankton require trace metals for growth and also mediate
        //  the vertical distributions of many metals in the ocean. This
        //  dataset provides direct measurements of metal quotas in
        //  phytoplankton from across the North Atlantic Ocean, known to
        //  be subjected to aeolian Saharan inputs and anthropogenic inputs from North America and
        // Europe. Bulk particulate material and individual phytoplankton cells were collected from
        // the upper water column (\u003C150 m) as part of the US GEOTRACES North Atlantic Zonal
        // Transect cruises (KN199-4, KN199-5, KN204-1A,B). The cruise tracks spanned several ocean
        // biomes and geochemical regions. Chemical leaches (to extract biogenic and otherwise
        // labile particulate phases) are combined together with synchrotron X-ray fluorescence
        // (SXRF) analyses of individual micro and nanophytoplankton to discern spatial trends
        // across the basin. Individual phytoplankton cells were analyzed for elemental content
        // using SXRF (Synchrotron radiation X-Ray Fluorescence). Carbon was calculated from
        // biovolume using the relationships of Menden-Deuer \u0026 Lessard (2000).",
        gatts.add(
            "summary",
            ds.has("description")
                ? XML.removeHTMLTags(ds.getString("description"))
                : ds.has("abstract") ? ds.getString("abstract") : tTitle);

        // iso_19115_2
        String iso19115File = null;
        if (ds.has("dataset_iso")) {
          try {
            iso19115File = dsDir + "iso_19115_2.xml";
            if (!useLocalFilesIfPossible || !File2.isFile(iso19115File))
              SSR.downloadFile(
                  ds.getString("dataset_iso"), iso19115File, true); // tryToUseCompression)
          } catch (Exception e) {
            iso19115File = null;
            String2.log(
                "ERROR while getting iso_19115_2.xml file:\n" + MustBe.throwableToString(e));
          }
        }

        // "license":"http:\/\/creativecommons.org\/licenses\/by\/4.0\/",
        gatts.add(
            "license",
            (ds.has("license") ? ds.getString("license") + "\n" : "")
                +
                // modified slightly from Terms of Use at http://www.bco-dmo.org/
                "This data set is freely available as long as one follows the\n"
                + "terms of use (http://www.bco-dmo.org/terms-use), including\n"
                + "the understanding that any such use will properly acknowledge\n"
                + "the originating Investigator. It is highly recommended that\n"
                + "anyone wishing to use portions of this data should contact\n"
                + "the originating principal investigator (PI).");

        // "filename":"GT10_11_cellular_element_quotas.tsv",
        // "download_url":"http:\/\/darchive.mblwhoilibrary.org\/bitstream\/handle\/
        //  1912\/7908\/1\/GT10_11_cellular_element_quotas.tsv",
        // "file_size_in_bytes":"70567",
        String sourceUrl = ds.getString("download_url");
        gatts.add("sourceUrl", sourceUrl);
        String fileName = File2.getNameAndExtension(sourceUrl);
        String tsvName = dsDir + fileName;
        if (!useLocalFilesIfPossible || !File2.isFile(tsvName))
          SSR.downloadFile(sourceUrl, tsvName, true); // tryToCompress
        Table sourceTable = new Table();

        // look for colNamesRow after rows starting with "# ".
        // see /u00/data/points/bcodmo/488871_20140127/data_ctdmocness1.tsv
        // Adam says tsv files are US-ASCII chars only
        BufferedReader br = File2.getDecompressedBufferedFileReader(tsvName, bcodmoCharset);

        // skip "# " comment rows to get to columnNames row
        int colNamesRow = -1;
        while (true) {
          colNamesRow++;
          br.mark(10000); // max read-ahead bytes
          String s = br.readLine();
          if (s == null) throw new Exception("The file contains only comment lines.");
          if (s.startsWith("# ")) continue;
          br.reset(); // go back to mark
          break;
        }

        // read the data
        sourceTable.readASCII(
            tsvName, br, "", "", 0, 1, "\t", null, null, null, null,
            false); // don't simplify until "nd" removed
        // custom alternative to sourceTable.convertIsSomething2(); //convert e.g., "nd" to ""
        for (int col = 0; col < sourceTable.nColumns(); col++)
          sourceTable.getColumn(col).switchFromTo("nd", ""); // the universal BCO-DMO missing value?
        sourceTable.simplify();
        sourceTable.standardize(tStandardizeWhat);

        Table addTable = (Table) sourceTable.clone();
        addTable.globalAttributes().add(gatts);
        gatts = addTable.globalAttributes();

        if (ds.has("current_state")) // "Final no updates expected"
        gatts.add("current_state", ds.getString("current_state"));

        if (ds.has("validated"))
          gatts.add("validated", "" + String2.parseBoolean(ds.getString("validated"))); // 0|1

        if (ds.has("restricted"))
          gatts.add("restricted", "" + String2.parseBoolean(ds.getString("restricted"))); // 0|1

        // "dataset_name":"GT10-11 - cellular element quotas",
        if (ds.has("dataset_name")) gatts.add("dataset_name", ds.getString("dataset_name"));

        // "acquisition_desc":"\u003Cdiv xmlns=\u0022http:\/\/www.w3.org\/1999\/xhtml\u0022
        // lang=\u0022en\u0022\u003E\u003Cp\u003ESXRF samples were prepared ...
        // ... of Twining et al. (2011).\u003C\/p\u003E\u003C\/div\u003E",
        if (ds.has("acquisition_desc"))
          gatts.add(
              "acquisition_description", XML.removeHTMLTags(ds.getString("acquisition_desc")));

        // "brief_desc":"Element quotas of individual phytoplankton cells",
        if (ds.has("brief_desc")) gatts.add("brief_description", ds.getString("brief_desc"));

        // "processing_desc":"\u003Cdiv xmlns=\u0022http:\/\/www.w3.org\/1999\/xhtml\u0022
        // lang=\u0022en\u0022\u003E\u003Cp\u003EData were processed as ...
        // ... via the join method.\u003C\/p\u003E\u003C\/div\u003E",
        if (ds.has("processing_desc"))
          gatts.add("processing_description", XML.removeHTMLTags(ds.getString("processing_desc")));

        // "parameters_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/parameters",
        subArray = getBCODMOSubArray(ds, dsDir, "parameters", useLocalFilesIfPossible);
        if (subArray != null) {
          for (int sai = 0; sai < subArray.length(); sai++) {
            subObject = subArray.getJSONObject(sai);

            // "parameter_name":"cruise_id",
            String colName = subObject.getString("parameter_name");
            int col = addTable.findColumnNumber(colName);
            if (col < 0) {
              String2.log("WARNING: parameter_name=" + colName + " not found in " + tsvName);
              continue;
            }
            Attributes colAtts = addTable.columnAttributes(col);

            // "parameter":"http:\/\/lod.bco-dmo.org\/id\/dataset-parameter\/550520",
            // SKIP since web page has ID# and info
            // if (subObject.has(      "parameter"))
            //    colAtts.add(        "BCO_DMO_dataset_parameter_ID",
            //    File2.getNameAndExtension(
            //    subObject.getString("parameter")));

            // "units":"unitless",
            if (subObject.has("units")) {
              // will be cleaned up by makeReadyToUseAddVariableAttributes
              String s = subObject.getString("units");
              if (s != null || s.length() > 0) colAtts.add("units", s);
            }

            // "data_type":"",  //always "". Adam says this is just a placeholder for now

            // "desc":"cruise identification", //often long
            if (subObject.has("desc"))
              colAtts.add(
                  "description",
                  XML.removeHTMLTags( // some are, some aren't
                      subObject.getString("desc")));

            // "bcodmo_webpage":"http:\/\/www.bco-dmo.org\/dataset-parameter\/550520",
            if (subObject.has("bcodmo_webpage"))
              colAtts.add(
                  "webpage", // ??? "BCO_DMO_webpage",
                  subObject.getString("bcodmo_webpage"));

            // "master_parameter":"http:\/\/lod.bco-dmo.org\/id\/parameter\/1102",
            // "master_parameter_name":"cruise_id",
            // "master_parameter_desc":"cruise designation; name", //often long

            // "no_data_value":"nd",
            // "master_parameter_no_data_value":"nd"},
            // switchFromTo? no.  Leave source file unchanged
            if (subObject.has("no_data_value")) {
              String s = subObject.getString("no_data_value");
              if (!"nd".equals(s) && !"".equals(s))
                String2.log("WARNING: " + colName + " no_data_value=" + String2.toJson(s));
            }
          }
        }

        // "instruments_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/instruments",
        // 550520 has only "self" and "previous"
        subArray = getBCODMOSubArray(ds, dsDir, "instruments", useLocalFilesIfPossible);
        if (subArray != null) {
          for (int sai = 0; sai < subArray.length(); sai++) {
            subObject = subArray.getJSONObject(sai);
            String pre = "instrument_" + (sai + 1) + "_";

            // "instrument":"http:\/\/lod.bco-dmo.org\/id\/dataset-instrument\/643392",
            // SKIP since web page has ID# and info
            // if (subObject.has(      "instrument"))
            //    gatts.add(pre +     "BCO_DMO_dataset_instrument_ID",
            //    File2.getNameAndExtension(
            //    subObject.getString("instrument")));

            // "instrument_name":"",
            if (subObject.has("instrument_name"))
              gatts.add(pre + "name", subObject.getString("instrument_name"));

            // "desc":"",
            if (subObject.has("desc"))
              gatts.add(
                  pre + "description",
                  XML.removeHTMLTags( // some are, some aren't
                      subObject.getString("desc")));

            // "bcodmo_webpage":"http:\/\/www.bco-dmo.org\/dataset-instrument\/643392",
            if (subObject.has("bcodmo_webpage"))
              gatts.add(
                  pre + "webpage", // ??? "BCO_DMO_webpage",
                  subObject.getString("bcodmo_webpage"));

            // "instrument_type":"http:\/\/lod.bco-dmo.org\/id\/instrument\/411",

            // "type_name":"GO-FLO Bottle",
            if (subObject.has("type_name"))
              gatts.add(pre + "type_name", subObject.getString("type_name"));

            // "type_desc":"GO-FLO bottle cast used to collect water samples for pigment, nutrient,
            // plankton, etc. The GO-FLO sampling bottle is specially designed to avoid sample
            // contamination at the surface, internal spring contamination, loss of sample on deck
            // (internal seals), and exchange of water from different depths."},
            if (subObject.has("type_desc"))
              gatts.add(
                  pre + "type_description",
                  XML.removeHTMLTags( // some are, some aren't
                      subObject.getString("type_desc")));
          }
        }

        // "people_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/people",
        subArray = getBCODMOSubArray(ds, dsDir, "people", useLocalFilesIfPossible);
        if (subArray != null) {
          for (int sai = 0; sai < subArray.length(); sai++) {
            subObject = subArray.getJSONObject(sai);
            String pre = "person_" + (sai + 1) + "_";

            // "person":"http:\/\/lod.bco-dmo.org\/id\/person\/51087",
            // SKIP since web page has ID# and info
            // if (subObject.has(      "person"))
            //    gatts.add(pre +     "BCO_DMO_person_ID",
            //    File2.getNameAndExtension(
            //    subObject.getString("person")));

            // "person_name":"Dr Benjamin Twining",
            if (subObject.has("person_name"))
              gatts.add(pre + "name", subObject.getString("person_name"));

            // "bcodmo_webpage":"http:\/\/www.bco-dmo.org\/person\/51087",
            if (subObject.has("bcodmo_webpage"))
              gatts.add(
                  pre + "webpage", // ??? "BCO_DMO_webpage",
                  subObject.getString("bcodmo_webpage"));

            // "institution":"http:\/\/lod.bco-dmo.org\/id\/affiliation\/94",
            // SKIP: the webpage, http://www.bco-dmo.org/affiliation/94
            //  just has institution_name and list of people affiliated with it.
            //  Institution name is the important thing.
            // if (subObject.has(      "institution"))
            //    gatts.add(pre +     "BCO_DMO_affiliation_ID",
            //    File2.getNameAndExtension(
            //    subObject.getString("institution")));

            // "institution_name":"Bigelow Laboratory for Ocean Sciences (Bigelow)",
            if (subObject.has("institution_name"))
              gatts.add(pre + "institution_name", subObject.getString("institution_name"));

            // "role_name":"Principal Investigator"},
            if (subObject.has("role_name")) {
              String role = subObject.getString("role_name");
              gatts.add(pre + "role", role);

              if (gatts.getString("creator_name") == null
                  && "Principal Investigator".equals(role)) {
                gatts.add("creator_name", subObject.getString("person_name"));
                gatts.add("creator_url", subObject.getString("bcodmo_webpage"));
                gatts.add("creator_type", "person");
              }
            }
          }
        }

        // "deployments_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/deployments",
        subArray = getBCODMOSubArray(ds, dsDir, "deployments", useLocalFilesIfPossible);
        if (subArray != null) {
          for (int sai = 0; sai < subArray.length(); sai++) {
            subObject = subArray.getJSONObject(sai);
            String pre = "deployment_" + (sai + 1) + "_";

            // "deployment":"http:\/\/lod.bco-dmo.org\/id\/deployment\/58066",
            // SKIP since web page has ID# and info
            // if (subObject.has(      "deployment"))
            //    gatts.add(pre +     "BCO_DMO_deployment_ID",
            //    File2.getNameAndExtension(
            //    subObject.getString("deployment")));

            // "title":"KN199-04",
            if (subObject.has("title")) gatts.add(pre + "title", subObject.getString("title"));

            // "bcodmo_webpage":"http:\/\/www.bco-dmo.org\/deployment\/58066",
            if (subObject.has("bcodmo_webpage"))
              gatts.add(
                  pre + "webpage", // ??? "BCO_DMO_webpage",
                  subObject.getString("bcodmo_webpage"));

            // "description":"\u003Cdiv xmlns=\u0022http:\/\/www.w3.org\/1999\/xhtml\u0022
            // lang=\u0022en\u0022\u003E\u003Cp\u003EKN199-04 is the US GEOTRACES Zonal North
            // Atlantic Survey Section cruise planned for late Fall 2010 from Lisboa, Portugal to
            // Woods Hole, MA, USA.\u003C\/p\u003E\n\u003Cp\u003E4 November 2010 update: Due to
            // engine failure, the scheduled science activities were canceled on 2 November 2010. On
            // 4 November the R\/V KNORR put in at Porto Grande, Cape Verde and is scheduled to
            // depart November 8, under the direction of Acting Chief Scientist Oliver Wurl of Old
            // Dominion University. The objective of this leg is to carry the vessel in transit to
            // Charleston, SC while conducting science activities modified from the original
            // plan.\u003C\/p\u003E\n\u003Cp\u003EPlanned scientific activities and operations area
            // during this transit will be as follows: the ship\u0027s track will cross from the
            // highly productive region off West Africa into the oligotrophic central subtropical
            // gyre waters, then across the western boundary current (Gulf Stream), and into the
            // productive coastal waters of North America. During this transit, underway surface
            // sampling will be done using the towed fish for trace metals, nanomolar nutrients, and
            // arsenic speciation. In addition, a port-side high volume pumping system will be used
            // to acquire samples for radium isotopes. Finally, routine aerosol and rain sampling
            // will be done for trace elements. This section will provide important information
            // regarding atmospheric deposition, surface transport, and transformations of many
            // trace elements.\u003C\/p\u003E\n\u003Cp\u003EThe vessel is scheduled to arrive at the
            // port of Charleston, SC, on 26 November 2010. The original cruise was intended to be
            // 55 days duration with arrival in Norfolk, VA on 5 December
            // 2010.\u003C\/p\u003E\n\u003Cp\u003Efunding: NSF OCE award
            // 0926423\u003C\/p\u003E\n\u003Cp\u003E\u003Cstrong\u003EScience
            // Objectives\u003C\/strong\u003E are to obtain state of the art trace metal and isotope
            // measurements on a suite of samples taken on a mid-latitude zonal transect of the
            // North Atlantic. In particular sampling will target the oxygen minimum zone extending
            // off the west African coast near Mauritania, the TAG hydrothermal field, and the
            // western boundary current system along Line W. In addition, the major biogeochemical
            // provinces of the subtropical North Atlantic will be characterized. For additional
            // information, please refer to the GEOTRACES program Web site (\u003Ca
            // href=\u0022http:\/\/www.GEOTRACES.org\u0022\u003EGEOTRACES.org\u003C\/a\u003E) for
            // overall program objectives and a summary of properties to be
            // measured.\u003C\/p\u003E\n\u003Cp\u003E\u003Cstrong\u003EScience
            // Activities\u003C\/strong\u003E include seawater sampling via GoFLO and Niskin
            // carousels, in situ pumping (and filtration), CTDO2 and transmissometer sensors,
            // underway pumped sampling of surface waters, and collection of aerosols and
            // rain.\u003C\/p\u003E\n\u003Cp\u003EHydrography, CTD and nutrient measurements will be
            // supported by the Ocean Data Facility (J. Swift) at Scripps Institution of
            // Oceanography and funded through NSF Facilities. They will be providing an additional
            // CTD rosette system along with nephelometer and LADCP. A trace metal clean Go-Flo
            // Rosette and winch will be provided by the group at Old Dominion University (G.
            // Cutter) along with a towed underway pumping system.\u003C\/p\u003E\n\u003Cp\u003EList
            // of cruise participants: [ \u003Ca
            // href=\u0022http:\/\/data.bcodmo.org\/US_GEOTRACES\/AtlanticSection\/GNAT_2010_cruiseParticipants.pdf\u0022\u003EPDF \u003C\/a\u003E]\u003C\/p\u003E\n\u003Cp\u003ECruise track: \u003Ca href=\u0022http:\/\/data.bcodmo.org\/US_GEOTRACES\/AtlanticSection\/KN199-04_crtrk.jpg\u0022 target=\u0022_blank\u0022\u003EJPEG image\u003C\/a\u003E (from Woods Hole Oceanographic Institution, vessel operator)\u003C\/p\u003E\n\u003Cp\u003EAdditional information may still be available from the vessel operator: \u003Ca href=\u0022http:\/\/www.whoi.edu\/cruiseplanning\/synopsis.do?id=581\u0022 target=\u0022_blank\u0022\u003EWHOI cruise planning synopsis\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003ECruise information and original data are available from the \u003Ca href=\u0022http:\/\/www.rvdata.us\/catalog\/KN199-04\u0022 target=\u0022_blank\u0022\u003ENSF R2R data catalog\u003C\/a\u003E.\u003C\/p\u003E\n\u003Cp\u003EADCP data are available from the Currents ADCP group at the University of Hawaii: \u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2010.html#kn199_4\u0022 target=\u0022_blank\u0022\u003EKN199-04 ADCP\u003C\/a\u003E\u003C\/p\u003E\u003C\/div\u003E",
            if (subObject.has("description"))
              gatts.add(
                  pre + "description", XML.removeHTMLTags(subObject.getString("description")));

            // "location":"Subtropical northern Atlantic Ocean",
            if (subObject.has("location"))
              gatts.add(pre + "location", subObject.getString("location"));

            // "start_date":"2010-10-15",
            if (subObject.has("start_date"))
              gatts.add(pre + "start_date", subObject.getString("start_date"));

            // "end_date":"2010-11-04"},
            if (subObject.has("end_date"))
              gatts.add(pre + "end_date", subObject.getString("end_date"));
          }
        }

        // "projects_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/projects"},
        subArray = getBCODMOSubArray(ds, dsDir, "projects", useLocalFilesIfPossible);
        if (subArray != null) {
          for (int sai = 0; sai < subArray.length(); sai++) {
            subObject = subArray.getJSONObject(sai);
            String pre = "project_" + (sai + 1) + "_";

            // "project":"http:\/\/lod.bco-dmo.org\/id\/project\/2066",
            // SKIP since web page has ID# and info
            // if (subObject.has(      "project"))
            //    gatts.add(pre +     "BCO_DMO_project_ID",
            //    File2.getNameAndExtension(
            //    subObject.getString("project")));

            // "created_date":"2010-06-09T17:40:05-04:00",
            // "desc":"\u003Cdiv xmlns=\u0022http:\/\/www.w3.org\/1999\/xhtml\u0022
            // lang=\u0022en\u0022\u003E\u003Cp\u003E\u003Cem\u003EMuch of this text appeared in an
            // article published in OCB News, October 2008, by the OCB Project
            // Office.\u003C\/em\u003E\u003C\/p\u003E\n\u003Cp\u003EThe first U.S. GEOTRACES
            // Atlantic Section will be specifically centered around a sampling cruise to be carried
            // out in the North Atlantic in 2010. Ed Boyle (MIT) and Bill Jenkins (WHOI) organized a
            // three-day planning workshop that was held September 22-24, 2008 at the Woods Hole
            // Oceanographic Institution. The main goal of the workshop, sponsored by the National
            // Science Foundation and the U.S. GEOTRACES Scientific Steering Committee, was to
            // design the implementation plan for the first U.S. GEOTRACES Atlantic Section. The
            // primary cruise design motivation was to improve knowledge of the sources, sinks and
            // internal cycling of Trace Elements and their Isotopes (TEIs) by studying their
            // distributions along a section in the North Atlantic (Figure 1). The North Atlantic
            // has the full suite of processes that affect TEIs, including strong meridional
            // advection, boundary scavenging and source effects, aeolian deposition, and the salty
            // Mediterranean Outflow. The North Atlantic is particularly important as it lies at the
            // \u0022origin\u0022 of the global Meridional Overturning
            // Circulation.\u003C\/p\u003E\n\u003Cp\u003EIt is well understood that many trace
            // metals play important roles in biogeochemical processes and the carbon cycle, yet
            // very little is known about their large-scale distributions and the regional scale
            // processes that affect them. Recent advances in sampling and analytical techniques,
            // along with advances in our understanding of their roles in enzymatic and catalytic
            // processes in the open ocean provide a natural opportunity to make substantial
            // advances in our understanding of these important elements. Moreover, we are motivated
            // by the prospect of global change and the need to understand the present and future
            // workings of the ocean\u0027s biogeochemistry. The GEOTRACES strategy is to measure a
            // broad suite of TEIs to constrain the critical biogeochemical processes that influence
            // their distributions. In addition to these \u0022exotic\u0022 substances, more
            // traditional properties, including macronutrients (at micromolar and nanomolar
            // levels), CTD, bio-optical parameters, and carbon system characteristics will be
            // measured. The cruise starts at Line W, a repeat hydrographic section southeast of
            // Cape Cod, extends to Bermuda and subsequently through the North Atlantic oligotrophic
            // subtropical gyre, then transects into the African coast in the northern limb of the
            // coastal upwelling region. From there, the cruise goes northward into the
            // Mediterranean outflow. The station locations shown on the map are for the
            // \u0022fulldepth TEI\u0022 stations, and constitute approximately half of the stations
            // to be ultimately occupied.\u003C\/p\u003E\n\u003Cp\u003E\u003Cem\u003EFigure 1. The
            // proposed 2010 Atlantic GEOTRACES cruise track plotted on dissolved oxygen at 400 m
            // depth. Data from the World Ocean Atlas (Levitus et al., 2005) were plotted using
            // Ocean Data View (courtesy Reiner Schlitzer). [click on the image to view a larger
            // version]\u003C\/em\u003E\u003Cbr \/\u003E\u003Ca
            // href=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/GEOTRACES_Atl_stas.jpg\u0022 target=\u0022_blank\u0022\u003E\u003Cimg alt=\u0022\u0022 src=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/GEOTRACES_Atl_stas.jpg\u0022 style=\u0022width:350px\u0022 \/\u003E\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003EHydrography, CTD and nutrient measurements will be supported by the Ocean Data Facility (J. Swift) at Scripps Institution of Oceanography and funded through NSF Facilities. They will be providing an additional CTD rosette system along with nephelometer and LADCP. A trace metal clean Go-Flo Rosette and winch will be provided by the group at Old Dominion University (G. Cutter) along with a towed underway pumping system.\u003C\/p\u003E\n\u003Cp\u003EThe North Atlantic Transect cruise began in 2010 with KN199 leg 4 (station sampling) and leg 5 (underway sampling only) (Figure 2).\u003C\/p\u003E\n\u003Cp\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/\/US_GEOTRACES\/AtlanticSection\/Cruise_Report_for_Knorr_199_Final_v3.pdf\u0022 target=\u0022_blank\u0022\u003EKN199-04 Cruise Report (PDF)\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003E\u003Cem\u003EFigure 2. The red line shows the cruise track for the first leg of the US Geotraces North Atlantic Transect on the R\/V Knorr in October 2010.\u00a0 The rest of the stations (beginning with 13) will be completed in October-December 2011 on the R\/V Knorr (courtesy of Bill Jenkins, Chief Scientist, GNAT first leg). [click on the image to view a larger version]\u003C\/em\u003E\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/GNAT_stationPlan.jpg\u0022 target=\u0022_blank\u0022\u003E\u003Cimg alt=\u0022Atlantic Transect Station location map\u0022 src=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/GNAT_stationPlan_sm.jpg\u0022 style=\u0022width:350px\u0022 \/\u003E\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003EThe section completion effort resumed again in November 2011 with KN204-01A,B (Figure 3).\u003C\/p\u003E\n\u003Cp\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/\/US_GEOTRACES\/AtlanticSection\/Submitted_Preliminary_Cruise_Report_for_Knorr_204-01.pdf\u0022 target=\u0022_blank\u0022\u003EKN204-01A,B Cruise Report (PDF)\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003E\u003Cem\u003EFigure 3. Station locations occupied on the US Geotraces North Atlantic Transect on the R\/V Knorr in November 2011.\u00a0 [click on the image to view a larger version]\u003C\/em\u003E\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/KN204-01_Stations.png\u0022 target=\u0022_blank\u0022\u003E\u003Cimg alt=\u0022Atlantic Transect\/Part 2 Station location map\u0022 src=\u0022http:\/\/bcodata.whoi.edu\/US_GEOTRACES\/AtlanticSection\/KN204-01_Stations.png\u0022 style=\u0022width:350px\u0022 \/\u003E\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003EData from the North Atlantic Transect cruises are available under the Datasets heading below, and consensus values for the SAFe and North Atlantic GEOTRACES Reference Seawater Samples are available from the GEOTRACES Program Office: \u003Ca href=\u0022http:\/\/www.geotraces.org\/science\/intercalibration\/322-standards-and-reference-materials?acm=455_215\u0022 target=\u0022_blank\u0022\u003EStandards and Reference Materials\u003C\/a\u003E\u003C\/p\u003E\n\u003Cp\u003E\u003Cstrong\u003EADCP data\u003C\/strong\u003E are available from the Currents ADCP group at the University of Hawaii at the links below:\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2010.html#kn199_4\u0022 target=\u0022_blank\u0022\u003EKN199-04\u003C\/a\u003E\u00a0\u00a0 (leg 1 of 2010 cruise; Lisbon to Cape Verde)\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2010.html#kn199_5\u0022 target=\u0022_blank\u0022\u003EKN199-05\u003C\/a\u003E\u00a0\u00a0 (leg 2 of 2010 cruise; Cape Verde to Charleston, NC)\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2011.html#kn204_01\u0022 target=\u0022_blank\u0022\u003EKN204-01A\u003C\/a\u003E (part 1 of 2011 cruise; Woods Hole, MA to Bermuda)\u003Cbr \/\u003E\u003Ca href=\u0022http:\/\/currents.soest.hawaii.edu\/uhdas_adcp\/year2011.html#kn204_02\u0022 target=\u0022_blank\u0022\u003EKN204-01B\u003C\/a\u003E (part 2 of 2011 cruise; Bermuda to Cape Verde)\u003C\/p\u003E\u003C\/div\u003E",
            if (subObject.has("desc")) {
              String s = XML.removeHTMLTags(subObject.getString("desc"));
              s = String2.replaceAll(s, "[click on the image to view a larger version]", "");
              gatts.add(pre + "description", s);
            }

            // "last_modified_date":"2016-02-17T11:37:46-05:00",
            // "project_title":"U.S. GEOTRACES North Atlantic Transect",
            if (subObject.has("project_title"))
              gatts.add(pre + "title", subObject.getString("project_title"));

            // "bcodmo_webpage":"http:\/\/www.bco-dmo.org\/project\/2066",
            if (subObject.has("bcodmo_webpage"))
              gatts.add(
                  pre + "webpage", // ??? "BCO_DMO_webpage",
                  subObject.getString("bcodmo_webpage"));

            // "project_acronym":"U.S. GEOTRACES NAT"}],
            if (subObject.has("project_acronym"))
              gatts.add(pre + "acronym", subObject.getString("project_acronym"));
          }
        }

        // "funding_service":"https:\/\/www.bco-dmo.org\/erddap\/dataset\/549122\/funding"},
        subArray = getBCODMOSubArray(ds, dsDir, "funding", useLocalFilesIfPossible);
        if (subArray != null) {
          for (int sai = 0; sai < subArray.length(); sai++) {
            subObject = subArray.getJSONObject(sai);
            String pre = "funding_" + (sai + 1) + "_";

            // "award":"http:\/\/lod.bco-dmo.org\/id\/award\/55138",
            // SKIP since web page has ID# and info
            // if (subObject.has(      "award"))
            //    gatts.add(pre +     "BCO_DMO_award_ID",
            //    File2.getNameAndExtension(
            //    subObject.getString("award")));

            // "award_number":"OCE-0928289",
            if (subObject.has("award_number"))
              gatts.add(pre + "award_number", subObject.getString("award_number"));

            // "award_url":"http:\/\/www.nsf.gov\/awardsearch\/showAward?AWD_ID=0928289\u0026HistoricalAwards=false"
            if (subObject.has("award_url"))
              gatts.add(pre + "award_url", subObject.getString("award_url"));

            // "funding":"http:\/\/lod.bco-dmo.org\/id\/funding\/355",
            // SKIP since funding_source and fundref_doi have info
            // if (subObject.has(      "funding"))
            //    gatts.add(pre +     "BCO_DMO_funding_ID",
            //    File2.getNameAndExtension(
            //    subObject.getString("funding")));

            // "funding_source":"NSF Division of Ocean Sciences (NSF OCE)",
            if (subObject.has("funding_source"))
              gatts.add(pre + "source", subObject.getString("funding_source"));

            // "fundref_doi":"http:\/\/dx.doi.org\/10.13039\/100000141"}
            if (subObject.has("fundref_doi"))
              gatts.add(pre + "doi", subObject.getString("fundref_doi"));
          }
        }

        // cleanup
        boolean dateTimeAlreadyFound = false;
        String tSortedColumnSourceName = "";
        String tSortFilesBySourceNames = "";
        String tColumnNameForExtract = "";

        for (int col = 0; col < addTable.nColumns(); col++) {
          String colName = addTable.getColumnName(col);
          PrimitiveArray destPA = addTable.getColumn(col);

          // look for date columns
          String tUnits = addTable.columnAttributes(col).getString("units");
          if (tUnits == null) tUnits = "";
          if (tUnits.toLowerCase().indexOf("yy") >= 0 && destPA.elementType() != PAType.STRING)
            // convert e.g., yyyyMMdd columns from int to String
            addTable.setColumn(col, new StringArray(destPA));
          if (destPA.elementType() == PAType.STRING) {
            tUnits =
                Calendar2.suggestDateTimeFormat((StringArray) destPA, false); // evenIfPurelyNumeric
            if (tUnits.length() > 0) addTable.columnAttributes(col).set("units", tUnits);
            // ??? and if tUnits = "", set to ""???
          }
          boolean isDateTime = Calendar2.isTimeUnits(tUnits);

          Attributes sourceAtts = sourceTable.columnAttributes(col); // none
          Attributes addAtts = addTable.columnAttributes(col);
          addAtts =
              makeReadyToUseAddVariableAttributesForDatasetsXml(
                  gatts,
                  sourceAtts,
                  addAtts,
                  sourceTable.getColumnName(col),
                  destPA.elementType() != PAType.STRING, // tryToAddStandardName
                  destPA.elementType() != PAType.STRING, // addColorBarMinMax
                  true); // tryToFindLLAT

          // add missing_value and/or _FillValue if needed
          addMvFvAttsIfNeeded(colName, destPA, sourceAtts, addAtts);

          // files are likely sorted by first date time variable
          // and no harm if files aren't sorted that way
          if (tSortedColumnSourceName.length() == 0 && isDateTime && !dateTimeAlreadyFound) {
            dateTimeAlreadyFound = true;
            tSortedColumnSourceName = colName;
          }
        }

        // tryToFindLLAT
        tryToFindLLAT(sourceTable, addTable);

        // after dataVariables known, add global attributes in the addTable
        addTable
            .globalAttributes()
            .set(
                makeReadyToUseAddGlobalAttributesForDatasetsXml(
                    sourceTable.globalAttributes(),
                    // another cdm_data_type could be better; this is ok
                    hasLonLatTime(addTable) ? "Point" : "Other",
                    dsDir,
                    addTable.globalAttributes(), // externalAddGlobalAttributes,
                    suggestKeywords(sourceTable, addTable)));

        // tally for char > #255
        /*
        String s = addTable.globalAttributes().getString("summary");
        if (s != null)
            for (int i = 0; i < s.length(); i++)
                if (s.charAt(i) > 255)
                    charTally.add("charTally", String2.annotatedString("" + s.charAt(i)));
        s = addTable.globalAttributes().getString("acquisition_description");
        if (s != null)
            for (int i = 0; i < s.length(); i++)
                if (s.charAt(i) > 255)
                    charTally.add("charTally", String2.annotatedString("" + s.charAt(i)));
        */

        // subsetVariables
        if (sourceTable.globalAttributes().getString("subsetVariables") == null
            && addTable.globalAttributes().getString("subsetVariables") == null)
          addTable
              .globalAttributes()
              .add(
                  "subsetVariables",
                  suggestSubsetVariables(sourceTable, addTable, true)); // 1file/dataset?

        StringBuilder defaultDataQuery = new StringBuilder();
        StringBuilder defaultGraphQuery = new StringBuilder();
        if (addTable.findColumnNumber(EDV.TIME_NAME) >= 0) {
          defaultDataQuery.append("&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
          defaultGraphQuery.append("&amp;time&gt;=min(time)&amp;time&lt;=max(time)");
        }
        defaultGraphQuery.append("&amp;.marker=1|5");

        // write the information
        StringBuilder sb = new StringBuilder();
        if (tSortFilesBySourceNames.length() == 0) {
          if (tColumnNameForExtract.length() > 0
              && tSortedColumnSourceName.length() > 0
              && !tColumnNameForExtract.equals(tSortedColumnSourceName))
            tSortFilesBySourceNames = tColumnNameForExtract + ", " + tSortedColumnSourceName;
          else if (tColumnNameForExtract.length() > 0)
            tSortFilesBySourceNames = tColumnNameForExtract;
          else tSortFilesBySourceNames = tSortedColumnSourceName;
        }
        sb.append(
            // "<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n"
            // +
            // "  below, notably 'units' for each of the dataVariables. -->\n" +
            "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\""
                + tDatasetID
                + "\" active=\"true\">\n"
                + "    <!--  <accessibleTo>bcodmo</accessibleTo>  -->\n"
                + "    <reloadEveryNMinutes>10000</reloadEveryNMinutes>\n"
                + "    <updateEveryNMillis>-1</updateEveryNMillis>\n"
                + (defaultDataQuery.length() > 0
                    ? "    <defaultDataQuery>" + defaultDataQuery + "</defaultDataQuery>\n"
                    : "")
                + (defaultGraphQuery.length() > 0
                    ? "    <defaultGraphQuery>" + defaultGraphQuery + "</defaultGraphQuery>\n"
                    : "")
                + "    <fileDir>"
                + XML.encodeAsXML(dsDir)
                + "</fileDir>\n"
                + "    <fileNameRegex>"
                + XML.encodeAsXML(String2.plainTextToRegex(fileName))
                + "</fileNameRegex>\n"
                + "    <recursive>false</recursive>\n"
                + "    <pathRegex>.*</pathRegex>\n"
                + "    <metadataFrom>last</metadataFrom>\n"
                + "    <charset>"
                + bcodmoCharset
                + "</charset>\n"
                + "    <columnNamesRow>"
                + (colNamesRow + 1)
                + "</columnNamesRow>\n"
                + "    <firstDataRow>"
                + (colNamesRow + 2)
                + "</firstDataRow>\n"
                + "    <standardizeWhat>"
                + tStandardizeWhat
                + "</standardizeWhat>\n"
                +
                // "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) +
                // "</preExtractRegex>\n" +
                // "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) +
                // "</postExtractRegex>\n" +
                // "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
                // "    <columnNameForExtract>" + tColumnNameForExtract +
                // "</columnNameForExtract>\n" +
                "    <sortedColumnSourceName>"
                + XML.encodeAsXML(tSortedColumnSourceName)
                + "</sortedColumnSourceName>\n"
                + "    <sortFilesBySourceNames>"
                + XML.encodeAsXML(tSortFilesBySourceNames)
                + "</sortFilesBySourceNames>\n"
                + "    <fileTableInMemory>false</fileTableInMemory>\n"
                + "    <accessibleViaFiles>true</accessibleViaFiles>\n"
                + (iso19115File == null
                    ? ""
                    : "    <iso19115File>" + iso19115File + "</iso19115File>\n"));
        sb.append(writeAttsForDatasetsXml(false, sourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true, addTable.globalAttributes(), "    "));

        // last 2 params: includeDataType, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(sourceTable, addTable, "dataVariable", true, false));
        sb.append("</dataset>\n" + "\n");

        // success
        results.append(sb.toString());
        if (addTable.findColumnNumber("time") < 0) noTime.add(dsNumber);
        nSucceeded++;

      } catch (Exception e) {
        nFailed++;
        String2.log(
            String2.ERROR
                + " while processing dataset #"
                + dsi
                + "\n"
                + MustBe.throwableToString(e));
      }
    }
    // String2.log(charTally.toString());
    String2.log(">> noTime: " + noTime);
    String2.log(
        "\n*** EDDTableFromAsciiFiles.generateDatasetsXmlFromBCODMO finished in "
            + ((System.currentTimeMillis() - time) / 1000)
            + " seconds\n"
            + "nDatasets: total="
            + datasetsArray.length()
            + " matching="
            + nMatching
            + " (succeeded="
            + nSucceeded
            + " failed="
            + nFailed
            + ")");
    return results.toString();
  }
}
