/*
 * EDDTableFromNcCFFiles Copyright 2011, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

/**
 * This class represents a table of data from a collection of FeatureDatasets using CF Discrete
 * Sampling Geometries (was Point Observation Conventions),
 * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#discrete-sampling-geometries
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2011-01-27
 */
public class EDDTableFromNcCFFiles extends EDDTableFromFiles {

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
   * @param tFgdcFile This should be the fullname of a file with the FGDC that should be used for
   *     this dataset, or "" (to cause ERDDAP not to try to generate FGDC metadata for this
   *     dataset), or null (to allow ERDDAP to try to generate FGDC metadata for this dataset).
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   * @param tFileDir the base URL or file directory. See
   *     https://www.unidata.ucar.edu/software/netcdf-java/v4.2/javadoc/index.html
   *     FeatureDatasetFactoryManager open(). This may be a
   *     <ul>
   *       <li>local file (.nc or compatible)
   *       <li>thredds catalog#dataset (with a thredds: prefix)
   *       <li>cdmremote dataset (with an cdmremote: prefix)
   *       <li>collection dataset (with a collection: prefix)
   *     </ul>
   *
   * @param tSortedColumnSourceName can't be for a char/String variable because NcHelper binary
   *     searches are currently set up for numeric vars only.
   */
  public EDDTableFromNcCFFiles(
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
        "EDDTableFromNcCFFiles",
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
        tColumnSeparator, // irrelevant
        tPreExtractRegex,
        tPostExtractRegex,
        tExtractRegex,
        tColumnNameForExtract,
        tSortedColumnSourceName, // irrelevant
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
   * <p>For this class, sortedColumn is always time. See constructor.
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

    // FUTURE: when !mustGetData, much better if get metadata quickly,
    // e.g., via: table.readNcMetadata, but can't because readNcCF
    // adds/changes some global attributes when it reads the file
    // (see testGenerateDatasetsXml).

    // get the data from the source file
    Table table = new Table();
    String decompFullName =
        FileVisitorDNLS.decompressIfNeeded(
            tFileDir + tFileName,
            fileDir,
            decompressedDirectory(),
            EDStatic.decompressedCacheMaxGB,
            true); // reuseExisting
    // if (mustGetData) {
    table.readNcCF(
        decompFullName,
        sourceDataNames,
        standardizeWhat,
        sourceConVars,
        sourceConOps,
        sourceConValues);
    // } else {
    //    //Just return a table with globalAtts, columns with atts, but no rows.
    //    table.readNcMetadata(decompFullName, sourceDataNames.toArray(), sourceDataTypes,
    //        standardizeWhat);
    // }
    return table;
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromNcCFFiles. The XML can then
   * be edited by hand and added to the datasets.xml file.
   *
   * <p>This can't be made into a web service because it would allow any user to looks at (possibly)
   * private .nc files on the server.
   *
   * @param tFileDir the starting (parent) directory for searching for files
   * @param tFileNameRegex the regex that each filename (no directory info) must match (e.g.,
   *     ".*\\.nc") (usually only 1 backslash; 2 here since it is Java code). If null or "", it is
   *     generated to catch the same extension as the sampleFileName (usually ".*\\.nc").
   * @param sampleFileName the full file name of one of the files in the collection
   * @param tReloadEveryNMinutes e.g., 10080 for weekly
   * @param tPreExtractRegex part of info for extracting e.g., stationName from file name. Set to ""
   *     if not needed.
   * @param tPostExtractRegex part of info for extracting e.g., stationName from file name. Set to
   *     "" if not needed.
   * @param tExtractRegex part of info for extracting e.g., stationName from file name. Set to "" if
   *     not needed.
   * @param tColumnNameForExtract part of info for extracting e.g., stationName from file name. Set
   *     to "" if not needed.
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
        "\n*** EDDTableFromNcCFFiles.generateDatasetsXml"
            + "\nfileDir="
            + tFileDir
            + " fileNameRegex="
            + tFileNameRegex
            + "\nsampleFileName="
            + sampleFileName
            + "\nextract pre="
            + tPreExtractRegex
            + " post="
            + tPostExtractRegex
            + " regex="
            + tExtractRegex
            + " colNameForExtract="
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

    String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
    String2.log(NcHelper.ncdump(sampleFileName, "-h"));

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;
    dataSourceTable.readNcCF(sampleFileName, null, tStandardizeWhat, null, null, null);
    double maxTimeES = Double.NaN;
    for (int c = 0; c < dataSourceTable.nColumns(); c++) {
      String colName = dataSourceTable.getColumnName(c);
      Attributes sourceAtts = dataSourceTable.columnAttributes(c);
      PrimitiveArray sourcePA = dataSourceTable.getColumn(c);
      PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
      Attributes addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              dataSourceTable.globalAttributes(),
              sourceAtts,
              null,
              colName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true); // tryToFindLLAT
      dataAddTable.addColumn(c, colName, destPA, addAtts);

      // maxTimeES
      String tUnits = sourceAtts.getString("units");
      if (!Double.isFinite(maxTimeES) && Calendar2.isTimeUnits(tUnits)) {
        try {
          if (Calendar2.isNumericTimeUnits(tUnits)) {
            double tbf[] = Calendar2.getTimeBaseAndFactor(tUnits); // throws exception
            maxTimeES =
                Calendar2.unitsSinceToEpochSeconds(
                    tbf[0], tbf[1], destPA.getDouble(destPA.size() - 1));
          } else { // string time units
            maxTimeES =
                Calendar2.tryToEpochSeconds(destPA.getString(destPA.size() - 1)); // NaN if trouble
          }
        } catch (Throwable t) {
          String2.log("caught while trying to get maxTimeES: " + MustBe.throwableToString(t));
        }
      }

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(colName, sourcePA, sourceAtts, addAtts); // sourcePA since strongly typed
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

    // after dataVariables known, add global attributes in the dataAddTable
    dataAddTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(),
                dataSourceTable.globalAttributes().getString("cdm_data_type"),
                tFileDir,
                externalAddGlobalAttributes,
                suggestKeywords(dataSourceTable, dataAddTable)));

    // subsetVariables  (or get from outer variables in some file types?)
    if (dataSourceTable.globalAttributes().getString("subsetVariables") == null
        && dataAddTable.globalAttributes().getString("subsetVariables") == null)
      dataAddTable
          .globalAttributes()
          .add("subsetVariables", suggestSubsetVariables(dataSourceTable, dataAddTable, false));

    // add the columnNameForExtract variable
    if (tColumnNameForExtract.length() > 0) {
      Attributes atts = new Attributes();
      atts.add("ioos_category", "Identifier");
      atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
      // no units or standard_name
      dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
      dataAddTable.addColumn(0, tColumnNameForExtract, new StringArray(), atts);
    }

    // useMaxTimeES
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

    // write the information
    StringBuilder sb = new StringBuilder();
    String suggestedRegex =
        (tFileNameRegex == null || tFileNameRegex.length() == 0)
            ? ".*\\" + File2.getExtension(sampleFileName)
            : tFileNameRegex;
    if (tSortFilesBySourceNames.length() == 0) tSortFilesBySourceNames = tColumnNameForExtract;
    sb.append(
        "<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\""
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
