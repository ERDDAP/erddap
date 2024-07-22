/*
 * EDDTableFromAudioFiles Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

/**
 * This class represents a table of sampled sound data from a collection of audio files (e.g., WAV
 * or AU). See Table.readAudio file for info on which types of audio files can be read.
 * https://docs.oracle.com/javase/tutorial/sound/converters.html
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2017-08-29
 */
public class EDDTableFromAudioFiles extends EDDTableFromFiles {

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
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   */
  public EDDTableFromAudioFiles(
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
        "EDDTableFromAudioFiles",
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

    // String2.log(">> EDDTableFromAudioFiles end of constructor:\n" + toString());
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

    // read the file
    Table table = new Table();
    String decompFullName =
        FileVisitorDNLS.decompressIfNeeded(
            tFileDir + tFileName,
            fileDir,
            decompressedDirectory(),
            EDStatic.decompressedCacheMaxGB,
            true); // reuseExisting
    table.readAudioFile(decompFullName, mustGetData, true); // addElapsedTime

    // unpack
    table.standardize(standardizeWhat);
    // String2.log(">> EDDTableFromAudioFiles.lowGetSourceDataFromFile header after unpack (nCol=" +
    // table.nColumns() + "):\n" + table.getNCHeader("row"));

    return table;
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromAudioFiles. The XML can
   * then be edited by hand and added to the datasets.xml file.
   *
   * <p>This can't be made into a web service because it would allow any user to looks at (possibly)
   * private .nc files on the server.
   *
   * @param tFileDir the starting (parent) directory for searching for files
   * @param tFileNameRegex the regex that each filename (no directory info) must match (e.g.,
   *     ".*\\.nc") (usually only 1 backslash; 2 here since it is Java code). If null or "", it is
   *     generated to catch the same extension as the sampleFileName (usually ".*\\.nc").
   * @param sampleFileName the full file name of one of the files in the collection
   * @param useDimensionsCSV If null or "", this finds the group of variables sharing the highest
   *     number of dimensions. Otherwise, it find the variables using these dimensions (plus related
   *     char variables).
   * @param tReloadEveryNMinutes e.g., 10080 for weekly
   * @param tPreExtractRegex part of info for extracting e.g., stationName from file name. Set to ""
   *     if not needed.
   * @param tPostExtractRegex part of info for extracting e.g., stationName from file name. Set to
   *     "" if not needed.
   * @param tExtractRegex part of info for extracting e.g., stationName from file name. Set to "" if
   *     not needed.
   * @param tColumnNameForExtract part of info for extracting e.g., stationName from file name. Set
   *     to "" if not needed.
   * @param tExtractUnits e.g., yyyyMMdd'_'HHmmss, or "" if not needed.
   * @param tSortedColumnSourceName use "" if not known or not needed.
   * @param tSortFilesBySourceNames This is useful, because it ultimately determines default results
   *     order.
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
      String tFileDir,
      String tFileNameRegex,
      String sampleFileName,
      int tReloadEveryNMinutes,
      String tPreExtractRegex,
      String tPostExtractRegex,
      String tExtractRegex,
      String tColumnNameForExtract,
      String tExtractUnits, // String tSortedColumnSourceName,
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
        "\n*** EDDTableFromAudioFiles.generateDatasetsXml"
            + "\nfileDir="
            + tFileDir
            + " fileNameRegex="
            + tFileNameRegex
            + "\nsampleFileName="
            + sampleFileName
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
            +
            // "\nsortedColumn=" + tSortedColumnSourceName +
            " sortFilesBy="
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
    String tSortedColumnSourceName = Table.ELAPSED_TIME;
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
    dataSourceTable.readAudioFile(sampleFileName, true, true); // getMetadata, addElapsedTime

    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;
    dataSourceTable.standardize(tStandardizeWhat);

    Table dataAddTable = new Table();
    for (int c = 0; c < dataSourceTable.nColumns(); c++) {
      String colName = dataSourceTable.getColumnName(c);
      Attributes sourceAtts = dataSourceTable.columnAttributes(c);
      PrimitiveArray sourcePA = dataSourceTable.getColumn(c);
      PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
      dataAddTable.addColumn(
          c,
          colName,
          destPA,
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              dataSourceTable.globalAttributes(),
              sourceAtts,
              null,
              colName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true)); // tryToFindLLAT
      if (c > 0) {
        if (EDStatic.variablesMustHaveIoosCategory)
          dataAddTable.columnAttributes(c).set("ioos_category", "Other");
        if (sourcePA.isIntegerType()) {
          dataAddTable
              .columnAttributes(c)
              .add("colorBarMinimum", Math2.niceDouble(-sourcePA.missingValueAsDouble(), 2))
              .add("colorBarMaximum", Math2.niceDouble(sourcePA.missingValueAsDouble(), 2));
        }
      }

      // if a variable has timeUnits, files are likely sorted by time
      // and no harm if files aren't sorted that way
      // if (tSortedColumnSourceName.length() == 0 &&
      //    EDVTimeStamp.hasTimeUnits(sourceAtts, null))
      //    tSortedColumnSourceName = colName;
    }
    // String2.log("SOURCE COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());
    // String2.log("DEST   COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());

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

    // tryToFindLLAT
    tryToFindLLAT(dataSourceTable, dataAddTable);

    // externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
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
    if ("Data from a local source.".equals(dataAddTable.globalAttributes().getString("summary")))
      dataAddTable.globalAttributes().set("summary", "Audio data from a local source.");
    if ("Data from a local source.".equals(dataAddTable.globalAttributes().getString("title")))
      dataAddTable.globalAttributes().set("title", "Audio data from a local source.");

    // add the columnNameForExtract variable
    if (tColumnNameForExtract.length() > 0) {
      Attributes atts = new Attributes();
      atts.add("ioos_category", "Identifier");
      atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
      if (String2.isSomething(tExtractUnits)) atts.add("units", tExtractUnits);
      // no standard_name
      dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
      dataAddTable.addColumn(0, tColumnNameForExtract, new StringArray(), atts);

      // subsetVariables
      dataAddTable.globalAttributes().add("subsetVariables", tColumnNameForExtract);
    }

    // default queries
    String defGraph = "";
    String defData = "";
    String et = Table.ELAPSED_TIME;
    if ("time".equals(tColumnNameForExtract)) {
      defGraph = et + ",channel_1&time=min(time)&" + et + ">=0&" + et + "<=1&.draw=lines";
      defData = "&time=min(time)";
    } else {
      defGraph = et + ",channel_1&" + et + ">=0&" + et + "<=1&.draw=lines";
      defData = "";
    }

    // write the information
    StringBuilder sb = new StringBuilder();
    String suggestedRegex =
        (tFileNameRegex == null || tFileNameRegex.length() == 0)
            ? ".*\\" + File2.getExtension(sampleFileName)
            : tFileNameRegex;
    if (tSortFilesBySourceNames.length() == 0) {
      if (tColumnNameForExtract.length() > 0) tSortFilesBySourceNames = tColumnNameForExtract;
    }
    sb.append(
        "<dataset type=\"EDDTableFromAudioFiles\" datasetID=\""
            + suggestDatasetID(tFileDir + suggestedRegex)
            + // dirs can't be made public
            "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + (String2.isUrl(tCacheFromUrl)
                ? "    <cacheFromUrl>" + XML.encodeAsXML(tCacheFromUrl) + "</cacheFromUrl>\n"
                : "    <updateEveryNMillis>"
                    + suggestUpdateEveryNMillis(tFileDir)
                    + "</updateEveryNMillis>\n")
            + "    <defaultGraphQuery>"
            + XML.encodeAsXML(defGraph)
            + "</defaultGraphQuery>\n"
            + "    <defaultDataQuery>"
            + XML.encodeAsXML(defData)
            + "</defaultDataQuery>\n"
            + "    <fileDir>"
            + XML.encodeAsXML(tFileDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(suggestedRegex)
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
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
            + XML.encodeAsXML(Table.ELAPSED_TIME)
            + "</sortedColumnSourceName>\n"
            + "    <sortFilesBySourceNames>"
            + XML.encodeAsXML(tSortFilesBySourceNames)
            + "</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n");
    sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
    sb.append(writeAttsForDatasetsXml(true, dataAddTable.globalAttributes(), "    "));

    // last 2 params: includeDataType, questionDestinationName
    sb.append(
        writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true, false));
    sb.append("</dataset>\n" + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }
}
