/*
 * EDDTableFromAwsXmlFiles Copyright 2012, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.erddap.variable.*;

/**
 * This class represents a table of data from a collection of AWS (Automatic Weather Station) XML
 * data files (namespace http://www.aws.com/aws).
 * http://developer.weatherbug.com/docs/read/WeatherBug_Rest_XML_API 2017-12-08 weatherbug.com is
 * INACTIVE
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2012-11-05
 */
public class EDDTableFromAwsXmlFiles extends EDDTableFromFiles {

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
  public EDDTableFromAwsXmlFiles(
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
        "EDDTableFromAwsXmlFiles",
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

    // Future: more efficient if !mustGetData is handled differently

    Table table = new Table();
    table.readAwsXmlFile(tFileDir + tFileName);

    // convert to desired sourceDataTypes
    int nCols = table.nColumns();
    for (int tc = 0; tc < nCols; tc++) {
      int sd = sourceDataNames.indexOf(table.getColumnName(tc));
      if (sd >= 0) {
        PrimitiveArray pa = table.getColumn(tc);
        if (!sourceDataTypes[sd].equals(pa.elementTypeString())) {
          PrimitiveArray newPa =
              PrimitiveArray.factory(PAType.fromCohortString(sourceDataTypes[sd]), 1, false);
          newPa.append(pa);
          table.setColumn(tc, newPa);
        }
      }
    }

    // unpack
    table.standardize(standardizeWhat);

    return table;
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromAwsXmlFiles. The XML can
   * then be edited by hand and added to the datasets.xml file.
   *
   * <p>This can't be made into a web service because it would allow any user to looks at (possibly)
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
      int columnNamesRow,
      int firstDataRow,
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
        "EDDTableFromAwsXmlFiles.generateDatasetsXml"
            + "\nsampleFileName="
            + sampleFileName
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

    // *** basically, make a table to hold the sourceAttributes
    Table dataSourceTable = new Table();
    dataSourceTable.readAwsXmlFile(sampleFileName);

    // simplify dataSourceTable to make a guess for data types
    // but prevent zip from becoming a String
    int zipCol = dataSourceTable.findColumnNumber("city-state-zip");
    if (zipCol >= 0)
      dataSourceTable.setStringData(zipCol, 0, "A" + dataSourceTable.getStringData(zipCol, 0));
    dataSourceTable.simplify();
    if (zipCol >= 0)
      dataSourceTable.setStringData(
          zipCol, 0, dataSourceTable.getStringData(zipCol, 0).substring(1));

    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;
    dataSourceTable.standardize(tStandardizeWhat);

    // and make a parallel table to hold addAttributes
    Table dataAddTable = new Table();

    // globalAttributes
    if (externalAddGlobalAttributes == null) externalAddGlobalAttributes = new Attributes();
    String keywords = externalAddGlobalAttributes.getString("keywords");
    if (keywords == null) keywords = "";
    keywords = (keywords.length() == 0 ? "" : ", ") + "Atmosphere > Altitude > Station Height";
    externalAddGlobalAttributes.set("keywords", keywords);
    if (tInfoUrl != null && tInfoUrl.length() > 0)
      externalAddGlobalAttributes.add("infoUrl", tInfoUrl);
    if (tInstitution != null && tInstitution.length() > 0)
      externalAddGlobalAttributes.add("institution", tInstitution);
    if (tSummary != null && tSummary.length() > 0)
      externalAddGlobalAttributes.add("summary", tSummary);
    if (tTitle != null && tTitle.length() > 0) externalAddGlobalAttributes.add("title", tTitle);
    externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");
    // externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");

    for (int col = 0; col < dataSourceTable.nColumns(); col++) {
      String colName = dataSourceTable.getColumnName(col);
      Attributes sourceAtts = dataSourceTable.columnAttributes(col);
      PrimitiveArray sourcePA = (PrimitiveArray) dataSourceTable.getColumn(col).clone();
      PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
      dataAddTable.addColumn(
          col,
          colName,
          destPA,
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              null, // no source global attributes
              sourceAtts,
              null,
              colName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true)); // tryToFindLLAT
      Attributes addAtts = dataAddTable.columnAttributes(col);

      // if a variable has timeUnits, files are likely sorted by time
      // and no harm if files aren't sorted that way
      boolean hasTimeUnits = EDVTimeStamp.hasTimeUnits(sourceAtts, null);
      if (tSortedColumnSourceName.length() == 0 && hasTimeUnits)
        tSortedColumnSourceName = dataSourceTable.getColumnName(col);

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(colName, destPA, sourceAtts, addAtts);
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
            + "<dataset type=\"EDDTableFromAwsXmlFiles\" datasetID=\""
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
}
