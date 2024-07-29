/*
 * EDDTableFromNcFiles Copyright 2009, NOAA.
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
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.Tally;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import ucar.ma2.*;
import ucar.nc2.*;

/**
 * This class represents a table of data from a collection of n-dimensional (1,2,3,4,...) .nc data
 * files. The dimensions are e.g., time,depth,lat,lon. <br>
 * A given file may have multiple values for each of the dimensions and the values may be different
 * in different files. <br>
 * [Was: only the leftmost dimension (e.g., time) could have multiple values.]
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-02-13
 */
public class EDDTableFromNcFiles extends EDDTableFromFiles {

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
  public EDDTableFromNcFiles(
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
        "EDDTableFromNcFiles",
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
  public EDDTableFromNcFiles(
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

    // read the file
    Table table = new Table();
    String decompFullName =
        FileVisitorDNLS.decompressIfNeeded(
            tFileDir + tFileName,
            fileDir,
            decompressedDirectory(),
            EDStatic.decompressedCacheMaxGB,
            true); // reuseExisting
    if (mustGetData) {
      table.readNDNc(
          decompFullName,
          sourceDataNames.toArray(),
          standardizeWhat,
          sortedSpacing >= 0 && !Double.isNaN(minSorted) ? sortedColumnSourceName : null,
          minSorted,
          maxSorted);
      // String2.log("  EDDTableFromNcFiles.lowGetSourceDataFromFile table.nRows=" + table.nRows());
      // table.saveAsDDS(System.out, "s");
    } else {
      // Just return a table with globalAtts, columns with atts, but no rows.
      table.readNcMetadata(
          decompFullName, sourceDataNames.toArray(), sourceDataTypes, standardizeWhat);
    }

    return table;
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromNcFiles. The XML can then
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
      String useDimensionsCSV,
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
        "\n*** EDDTableFromNcFiles.generateDatasetsXml"
            + "\nfileDir="
            + tFileDir
            + " fileNameRegex="
            + tFileNameRegex
            + "\nsampleFileName="
            + sampleFileName
            + "\nuseDimensionsCSV="
            + useDimensionsCSV
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
    String[] useDimensions = StringArray.arrayFromCSV(useDimensionsCSV);
    tColumnNameForExtract =
        String2.isSomething(tColumnNameForExtract) ? tColumnNameForExtract.trim() : "";
    tSortedColumnSourceName =
        String2.isSomething(tSortedColumnSourceName) ? tSortedColumnSourceName.trim() : "";
    if (!String2.isSomething(sampleFileName))
      String2.log(
          "Found/using sampleFileName="
              + (sampleFileName =
                  FileVisitorDNLS.getSampleFileName(
                      tFileDir, tFileNameRegex, true, ".*"))); // recursive, pathRegex

    // show structure of sample file
    String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
    String2.log(NcHelper.ncdump(sampleFileName, "-h"));

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();

    // new way
    StringArray varNames = new StringArray();
    double maxTimeES = Double.NaN;
    if (useDimensions.length > 0) {
      // find the varNames
      NetcdfFile ncFile = NcHelper.openFile(sampleFileName);
      try {

        Group rootGroup = ncFile.getRootGroup();
        List rootGroupVariables = rootGroup.getVariables();
        for (int v = 0; v < rootGroupVariables.size(); v++) {
          Variable var = (Variable) rootGroupVariables.get(v);
          boolean isChar = var.getDataType() == DataType.CHAR;
          if (var.getRank() + (isChar ? -1 : 0) == useDimensions.length) {
            boolean matches = true;
            for (int d = 0; d < useDimensions.length; d++) {
              if (!var.getDimension(d).getName().equals(useDimensions[d])) { // the full name
                matches = false;
                break;
              }
            }
            if (matches) varNames.add(var.getFullName());
          }
        }

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      } finally {
        try {
          if (ncFile != null) ncFile.close();
        } catch (Exception e9) {
        }
      }
      Test.ensureTrue(
          varNames.size() > 0, "The file has no variables with dimensions: " + useDimensionsCSV);
    }

    // then read the file
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;
    dataSourceTable.readNDNc(
        sampleFileName, varNames.toStringArray(), tStandardizeWhat, null, 0, 0);
    for (int c = 0; c < dataSourceTable.nColumns(); c++) {
      String colName = dataSourceTable.getColumnName(c);
      Attributes sourceAtts = dataSourceTable.columnAttributes(c);
      PrimitiveArray destPA = makeDestPAForGDX(dataSourceTable.getColumn(c), sourceAtts);
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

      // if a variable has timeUnits, files are likely sorted by time
      // and no harm if files aren't sorted that way
      String tUnits = sourceAtts.getString("units");
      if (tSortedColumnSourceName.length() == 0 && Calendar2.isTimeUnits(tUnits))
        tSortedColumnSourceName = colName;

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
    }
    // String2.log("SOURCE COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());
    // String2.log("DEST   COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());

    // add missing_value and/or _FillValue if needed
    addMvFvAttsIfNeeded(dataSourceTable, dataAddTable);

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

    // subsetVariables
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
    if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440; // 1440 works well with suggestedUpdateEveryNMillis

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
    if (tSortFilesBySourceNames.length() == 0) {
      if (tColumnNameForExtract.length() > 0
          && tSortedColumnSourceName.length() > 0
          && !tColumnNameForExtract.equals(tSortedColumnSourceName))
        tSortFilesBySourceNames = tColumnNameForExtract + ", " + tSortedColumnSourceName;
      else if (tColumnNameForExtract.length() > 0) tSortFilesBySourceNames = tColumnNameForExtract;
      else tSortFilesBySourceNames = tSortedColumnSourceName;
    }
    sb.append(
        "<dataset type=\"EDDTableFromNcFiles\" datasetID=\""
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

  // private static void metadataToData(
  //     Table table, String colName, String attName, String newColName, PAType tPAType)
  //     throws Exception {

  //   int col = table.findColumnNumber(colName);
  //   if (col < 0)
  //     throw new RuntimeException(
  //         "col=" + colName + " not found in " + table.getColumnNamesCSSVString());
  //   String value = table.columnAttributes(col).getString(attName);
  //   table.columnAttributes(col).remove(attName);
  //   if (value == null) value = "";
  //   table.addColumn(
  //       table.nColumns(),
  //       newColName,
  //       PrimitiveArray.factory(tPAType, table.nRows(), value),
  //       new Attributes());
  // }
  /**
   * NOT FOR GENERAL USE. Bob uses this to consolidate the individual WOD data files into 45° x 45°
   * x 1 month files (tiles). 45° x 45° leads to 8x4=32 files for a given time point, so a request
   * for a short time but entire world opens ~32 files. There are ~240 months worth of data, so a
   * request for a small lon lat range for all time opens ~240 files.
   *
   * <p>Why tile? Because there are ~10^6 profiles/year now, so ~10^7 total. And if 100 bytes of
   * info per file for EDDTableFromFiles fileTable, that's 1 GB!. So there needs to be fewer files.
   * We want to balance number of files for 1 time point (all region tiles), and number of time
   * point files (I'll stick with their use of 1 month). The tiling size selected is ok, but
   * searches for single profile (by name) are slow since a given file may have a wide range of
   * station_ids.
   *
   * @param type the 3 letter WOD file type e.g., APB
   * @param previousDownloadDate iso date after previous download finished (e.g., 2011-05-15)
   */
  /*    public static void bobConsolidateWOD(String type, String previousDownloadDate)
          throws Throwable {

          //constants
          int chunkSize = 45;  //lon width, lat height of a tile, in degrees
          String sourceDir   = "c:/data/wod/monthly/" + type + "/";
          String destDir     = "c:/data/wod/consolidated/" + type + "/";
          String logFile     = "c:/data/wod/bobConsolidateWOD.log";

          //derived
          double previousDownload = Calendar2.isoStringToEpochSeconds(previousDownloadDate);
          int nLon = 360 / chunkSize;
          int nLat = 180 / chunkSize;
          Table.verbose = false;
          Table.reallyVerbose = false;
          NcHelper.verbose = false;
          String2.setupLog(true, false, logFile, false, 1000000000);
          String2.log("*** starting bobConsolidateWOD(" + type + ", " + previousDownloadDate + ") " +
              Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n" +
              "logFile=" + String2.logFileName() + "\n" +
              String2.standardHelpAboutMessage());

          //go through the source dirs
          String sourceMonthDirs[] = RegexFilenameFilter.list(sourceDir, ".*");
          for (int sd = 0; sd < sourceMonthDirs.length; sd++) {
              String2.log("\n*** Look for new files in " + sourceDir + sourceMonthDirs[sd]);
              String sourceFiles[] = RegexFilenameFilter.list(
                  sourceDir + sourceMonthDirs[sd], ".*.nc");
              int nSourceFiles =
  100; //sourceFiles.length;

              //are any files newer than lastDownload?
              boolean newer = false;
              for (int sf = 0; sf < nSourceFiles; sf++) {
                  if (File2.getLastModified(sourceDir + sourceMonthDirs[sd] + "/" + sourceFiles[sf]) >
                      previousDownload) {
                      newer = true;
                      break;
                  }
              }
              if (!newer)
                  continue;

              //make/empty the destDirectory for this sourceMonths
              File2.makeDirectory( destDir + sourceMonthDirs[sd]);
              File2.deleteAllFiles(destDir + sourceMonthDirs[sd]);

              //read source files
              Table cumTable = new Table();
              for (int sf = 0; sf < nSourceFiles; sf++) {
                  String tFileName = sourceDir + sourceMonthDirs[sd] + "/" + sourceFiles[sf];
                  try {
                      if (sf % 100 == 0)
                          String2.log("reading file #" + sf + " of " + nSourceFiles);
                      Table tTable = new Table();
                      tTable.readFlat0Nc(tFileName, null, -1, -1);  //standardizeWhat=-1  -1=read all rows
                      int tNRows = tTable.nRows();

                      //ensure expected columns
                      if (type.equals("APB")) {
                          tTable.justKeepColumns(
                                  PAType.STRING, //id
                                    PAType.INT,
                                    PAType.FLOAT,
                                    PAType.FLOAT,
                                    double.class,
                                  PAType.INT, //date
                                    PAType.FLOAT,
                                    PAType.INT,
                                    PAType.CHAR,
                                    PAType.FLOAT,
                                  PAType.CHAR, //dataset
                                    PAType.FLOAT,
                                    PAType.FLOAT, //Temp
                                    PAType.INT,
                                    PAType.INT,
                                  PAType.FLOAT, //Sal
                                    PAType.INT,
                                    PAType.INT,
                                    PAType.FLOAT, //Press
                                    PAType.INT,
                                  PAType.INT,
                                    PAType.INT, //crs
                                    PAType.INT,
                                    PAType.INT,
                                    PAType.INT,
                                  PAType.INT},  //WODfd
                                new String[]{
                                  "WOD_cruise_identifier",
                                    "wod_unique_cast",
                                    "lat",
                                    "lon",
                                    "time",
                                  "date",
                                    "GMT_time",
                                    "Access_no",
                                    "Institute",
                                    "Orig_Stat_Num",
                                  "dataset",
                                    "z",
                                    "Temperature",
                                    "Temperature_sigfigs",
                                    "Temperature_WODflag",
                                  "Salinity",
                                    "Salinity_sigfigs",
                                    "Salinity_WODflag",
                                    "Pressure",
                                  "Pressure_sigfigs",
                                    "Pressure_WODflag",
                                    "crs",
                                    "profile",
                                    "WODf",
                                    "WODfp",
                                  "WODfd"},
                              new Class[]{
                              });



                          if (!tTable.getColumnName(0).equals("WOD_cruise_identifier")) {
                              tTable.addColumn(0, "WOD_cruise_identifier",
                                  PrimitiveArray.factory(PAType.STRING, tNRows, ""),
                                  new Attributes());
                          }
                          if (!tTable.getColumnName(6).equals("GMT_time")) {
                              tTable.addColumn(0, "GMT_time",
                                  PrimitiveArray.factory(PAType.FLOAT, tNRows, ""),
                                  new Attributes());
                          }

  float GMT_time ;
          GMT_time:long_name = "GMT_time" ;
  int Access_no ;
          Access_no:long_name = "NODC_accession_number" ;
          Access_no:units = "NODC_code" ;
          Access_no:comment = "used to find original data at NODC" ;
  char Platform(strnlen) ;
          Platform:long_name = "Platform_name" ;
          Platform:comment = "name of platform from which measurements were taken" ;
  float Orig_Stat_Num ;
          Orig_Stat_Num:long_name = "Originators_Station_Number" ;
          Orig_Stat_Num:comment = "number assigned to a given station by data originator" ;
  char Cast_Direction(strnlen) ;
          Cast_Direction:long_name = "Cast_Direction" ;
  char dataset(strnlen) ;
          dataset:long_name = "WOD_dataset" ;
  char Recorder(strnlen) ;
          Recorder:long_name = "Recorder" ;
          Recorder:units = "WMO code 4770" ;
          Recorder:comment = "Device which recorded measurements" ;
  char real_time(strnlen) ;
          real_time:long_name = "real_time_data" ;
          real_time:comment = "set if data are from the global telecommuncations system" ;
  char dbase_orig(strnlen) ;
          dbase_orig:long_name = "database_origin" ;
          dbase_orig:comment = "Database from which data were extracted" ;
  float z(z) ;

                          if (!tTable.getColumnName(18).equals("Salinity")) {
                              tTable.addColumn(18, "Salinity",
                                  PrimitiveArray.factory(PAType.FLOAT, tNRows, ""),
                                  new Attributes());
                              tTable.addColumn(19, "Salinity_sigfigs",
                                  PrimitiveArray.factory(PAType.INT, tNRows, ""),
                                  new Attributes());
                              tTable.addColumn(20, "Salinity_WODflag",
                                  PrimitiveArray.factory(PAType.INT, tNRows, ""),
                                  (new Attributes()));
                          }
                          if (!tTable.getColumnName(21).equals("Pressure")) {
                              tTable.addColumn(21, "Pressure",
                                  PrimitiveArray.factory(PAType.FLOAT, tNRows, ""),
                                  new Attributes());
                              tTable.addColumn(22, "Pressure_sigfigs",
                                  PrimitiveArray.factory(PAType.INT, tNRows, ""),
                                  new Attributes());
                              tTable.addColumn(23, "Pressure_WODflag",
                                  PrimitiveArray.factory(PAType.INT, tNRows, ""),
                                  (new Attributes()));
                          }
                      }


                      //convert metadata to data
                      if (type.equals("APB")) {

                          //WOD_cruise_identifier
                          metadataToData(tTable, "WOD_cruise_identifier", "country",
                              "WOD_cruise_country", PAType.STRING);
                          metadataToData(tTable, "WOD_cruise_identifier", "originators_cruise_identifier",
                              "WOD_cruise_originatorsID", PAType.STRING);
                          metadataToData(tTable, "WOD_cruise_identifier", "Primary_Investigator",
                              "WOD_cruise_Primary_Investigator", PAType.STRING);

                          //Temperature
                          metadataToData(tTable, "Temperature", "Instrument_(WOD_code)",
                              "Temperature_Instrument", PAType.STRING);
                          metadataToData(tTable, "Temperature", "WODprofile_flag",
                              "Temperature_WODprofile_flag", PAType.INT);
                      }

                      //validate
                      double stats[];
                      int tNCols = tTable.nColumns();
                      PrimitiveArray col;

                      col = tTable.getColumn("lon");
                      stats = col.calculateStats();
                      if (stats[PrimitiveArray.STATS_MIN] < -180)
                          String2.log("  ! minLon=" + stats[PrimitiveArray.STATS_MIN]);
                      if (stats[PrimitiveArray.STATS_MAX] > 180)
                          String2.log("  ! maxLon=" + stats[PrimitiveArray.STATS_MAX]);

                      col = tTable.getColumn("lat");
                      stats = col.calculateStats();
                      if (stats[PrimitiveArray.STATS_MIN] < -90)
                          String2.log("  ! minLat=" + stats[PrimitiveArray.STATS_MIN]);
                      if (stats[PrimitiveArray.STATS_MAX] > 90)
                          String2.log("  ! maxLat=" + stats[PrimitiveArray.STATS_MAX]);

                      //append
                      if (sf == 0) {
                          cumTable = tTable;
                      } else {
                          //ensure colNames same
                          Test.ensureEqual(
                              tTable.getColumnNamesCSSVString(),
                              cumTable.getColumnNamesCSSVString(),
                              "Different column names.");

                          //append
                          cumTable.append(tTable);
                      }

                  } catch (Throwable t) {
                      String2.log("ERROR: when processing " + tFileName + "\n" +
                          MustBe.throwableToString(t));
                  }
              }

              //sort
              String2.log("sorting");
              int timeCol = cumTable.findColumnNumber("time");
              int idCol   = cumTable.findColumnNumber("wod_unique_cast");
              Test.ensureNotEqual(timeCol, -1, "time column not found in " +
                  cumTable.getColumnNamesCSSVString());
              Test.ensureNotEqual(idCol, -1, "wod_unique_cast column not found in " +
                  cumTable.getColumnNamesCSSVString());
              cumTable.ascendingSort(new int[]{timeCol, idCol});

              //write consolidated data as tiles
              int cumNRows = cumTable.nRows();
              PrimitiveArray lonCol = cumTable.getColumn("lon");
              PrimitiveArray latCol = cumTable.getColumn("lat");
              for (int loni = 0; loni < nLon; loni++) {
                  double minLon = -180 + loni * chunkSize;
                  double maxLon = minLon + chunkSize + (loni == nLon-1? 0.1 : 0);
                  for (int lati = 0; lati < nLat; lati++) {
                      double minLat = -90 + lati * chunkSize;
                      double maxLat = minLat + chunkSize + (lati == nLat-1? 0.1 : 0);
                      Table tTable = (Table)cumTable.clone();
                      BitSet keep = new BitSet(cumNRows);
                      for (int row = 0; row < cumNRows; row++) {
                          double lon = lonCol.getDouble(row);
                          double lat = latCol.getDouble(row);
                          keep.set(row,
                              lon >= minLon && lon < maxLon &&
                              lat >= minLat && lat < maxLat);
                      }
                      tTable.justKeep(keep);
                      if (tTable.nRows() == 0) {
                          String2.log("No data for minLon=" + minLon + " minLat=" + minLat);
                      } else {
                          tTable.saveAsFlatNc(
                              destDir + sourceMonthDirs[sd] + "/" +
                                  sourceMonthDirs[sd] + "_" +
                                  Math.round(minLon) + "E_" +
                                  Math.round(minLat) + "N",
                              "row", false); //convertToFakeMissingValues
                      }
                  }
              }
          }
          String2.returnLoggingToSystemOut();
      }
  */

  /** For WOD, get all source variable names and file they are in. */
  public static void getAllSourceVariableNames(String dir, String fileNameRegex) {
    HashSet<String> hashset = new HashSet();
    String2.log(
        "\n*** EDDTableFromNcFiles.getAllsourceVariableNames from " + dir + " " + fileNameRegex);
    Table.verbose = false;
    Table.reallyVerbose = false;
    String sourceFiles[] = RegexFilenameFilter.recursiveFullNameList(dir, fileNameRegex, false);
    int nSourceFiles = sourceFiles.length;

    Table table = new Table();
    for (int sf = 0; sf < nSourceFiles; sf++) {
      try {
        table.readNDNc(
            sourceFiles[sf],
            null,
            0, // standardizeWhat=0
            null,
            0,
            0);
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
      }
      int nCols = table.nColumns();
      for (int c = 0; c < nCols; c++) {
        String colName = table.getColumnName(c);
        if (hashset.add(colName)) {
          String2.log(
              "colName="
                  + colName
                  + "  "
                  + table.getColumn(c).elementTypeString()
                  + "\n  file="
                  + sourceFiles[sf]
                  + "\n  attributes=\n"
                  + table.columnAttributes(c).toString());
        }
      }
    }
  }

  /*  rare in APB:
  colName=Salinity
    file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
    attributes=
      coordinates="time lat lon z"
      flag_definitions="WODfp"
      grid_mapping="crs"
      long_name="Salinity"
      standard_name="sea_water_salinity"
      WODprofile_flag=6

  colName=Salinity_sigfigs
    file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
    attributes=

  colName=Salinity_WODflag
    file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
    attributes=
      flag_definitions="WODf"

  colName=Pressure
    file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
    attributes=
      coordinates="time lat lon z"
      grid_mapping="crs"
      long_name="Pressure"
      standard_name="sea_water_pressure"
      units="dbar"

  colName=Pressure_sigfigs
    file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
    attributes=

  colName=Pressure_WODflag
    file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
    attributes=
      flag_definitions="WODf"

  colName=Orig_Stat_Num
    file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
    attributes=
      comment="number assigned to a given station by data originator"
      long_name="Originators_Station_Number"

  colName=Bottom_Depth
    file=f:/data/wod/monthly/APB/200904-200906/wod_012999458O.nc
    attributes=
      long_name="Bottom_Depth"
      units="meters"
  */

  /**
   * This tests if e.g., long_names are consistent for several variables for all the files in a
   * collection (e.g., an fsuResearch ship).
   *
   * @param regex e.g., ".*\\.nc"
   * @param vars e.g., ["DIR2", "T2", "RAIN2"]
   * @param attribute e.g., long_name
   */
  public static void displayAttributeFromFiles(
      String dir, String regex, String vars[], String attribute) {
    ArrayList arrayList = new ArrayList();
    RegexFilenameFilter.recursiveFullNameList(arrayList, dir, regex, true); // recursive?
    Table table = new Table();
    Tally tally = new Tally();
    for (int i = 0; i < arrayList.size(); i++) {
      table.clear();
      try {
        table.readNDNc(
            (String) arrayList.get(i),
            vars,
            0, // standardizeWhat=0
            null,
            Double.NaN,
            Double.NaN);
        for (int v = 0; v < table.nColumns(); v++) {
          tally.add(table.getColumnName(v), table.columnAttributes(v).getString(attribute));
        }
      } catch (Throwable t) {
        // String2.log(t.toString());
      }
    }
    String2.log("\n" + tally.toString());
  }
}
