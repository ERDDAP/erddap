/*
 * EDDTableFromWFSFiles Copyright 2013, NOAA.
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
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class represents a table of data from a file downloaded from a WFS server.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2013-01-16
 */
public class EDDTableFromWFSFiles extends EDDTableFromAsciiFiles {

  public static final String DefaultRowElementXPath = "/wfs:FeatureCollection/gml:featureMember";

  /** For testing, you may set this to true programmatically where needed, not here. */
  public static boolean developmentMode = false;

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
  public EDDTableFromWFSFiles(
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
        "EDDTableFromWFSFiles",
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
        // don't allow caching from remote site because this dataset already does caching from
        // remote site
        null,
        -1,
        null, // tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex,
        tAddVariablesWhere);
  }

  /**
   * This downloads data (as Strings) from a WFS server and saves to a UTF-8 ASCII TSV file. (ASCII
   * files have the advantage of supporting UTF-8, unlike .nc, and variable length strings are
   * stored space efficiently.) ColumnNames are unchanged from Table.readXml. Data is left as
   * Strings.
   *
   * @param tSourceUrl a request=GetFeatures URL which requests all of the data. See description and
   *     examples in JavaDocs for generateDatasetsXml.
   * @param tRowElementXPath the element (XPath style) identifying a row, If null or "", the default
   *     will be used: /wfs:FeatureCollection/gml:featureMember .
   * @param fullFileName recommended: EDStatic.fullCopyDirectory + datasetID + "/data.tsv"
   * @return error string ("" if no error)
   */
  public static String downloadData(
      String tSourceUrl, String tRowElementXPath, String fullFileName) {

    try {
      if (verbose) String2.log("EDDTableFromWFSFiles.downloadData");
      if (tRowElementXPath == null || tRowElementXPath.length() == 0)
        tRowElementXPath = DefaultRowElementXPath;

      // read the table
      Table table = new Table();
      InputStream is = SSR.getUrlBufferedInputStream(tSourceUrl);
      BufferedReader in = new BufferedReader(new InputStreamReader(is, File2.UTF_8));
      table.readXml(
          in,
          false, // no validate since no .dtd
          tRowElementXPath,
          null,
          false); // row attributes,  simplify=false

      // save as UTF-8 ASCII TSV file
      // This writes to temp file first and throws Exception if trouble.
      table.saveAsTabbedASCII(fullFileName, File2.UTF_8);
      return "";

    } catch (Exception e) {
      return String2.ERROR
          + " in EDDTableFromWFSFiles.downloadData"
          + "\n  sourceUrl="
          + tSourceUrl
          + "\n  rowElementXPath="
          + tRowElementXPath
          + "\n  "
          + MustBe.throwableToShortString(e);
    }
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromWFSFiles. The XML can then
   * be edited by hand and added to the datasets.xml file.
   *
   * @param tSourceUrl a request=GetFeatures URL which requests all of the data. See
   *     https://webhelp.esri.com/arcims/9.2/general/mergedProjects/wfs_connect/wfs_connector/get_feature.htm
   *     See (was)
   *     https://support.esri.com/arcims/9.2/general/mergedProjects/wfs_connect/wfs_connector/get_feature.htm
   *     For example, this GetCapabilities
   *     http://services.azgs.az.gov/ArcGIS/services/aasggeothermal/COBoreholeTemperatures/MapServer/WFSServer?
   *     request=GetCapabilities&service=WFS lead to this tSourceUrl
   *     https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?
   *     request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format="text/xml;
   *     subType=gml/3.1.1/profiles/gmlsf/1.0.0/0" See Bob's sample files in c:/data/mapserver
   * @param tRowElementXPath
   * @param tReloadEveryNMinutes
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
      String tSourceUrl,
      String tRowElementXPath,
      int tReloadEveryNMinutes,
      String tInfoUrl,
      String tInstitution,
      String tSummary,
      String tTitle,
      int tStandardizeWhat,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromWFSFiles.generateDatasetsXml"
            + "\nsourceUrl="
            + tSourceUrl
            + "\nrowElementXPath="
            + tRowElementXPath
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes
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
    if (!String2.isSomething(tSourceUrl))
      throw new IllegalArgumentException("sourceUrl wasn't specified.");
    if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440; // 1440 works well with suggestedUpdateEveryNMillis

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();

    if (tRowElementXPath == null || tRowElementXPath.length() == 0)
      tRowElementXPath = DefaultRowElementXPath;

    if (developmentMode) {
      dataSourceTable.readXml(
          File2.getDecompressedBufferedFileReaderUtf8(
              "c:/programs/mapserver/WVBoreholeResponse.xml"),
          false, // validate?  false since no .dtd
          tRowElementXPath,
          null,
          true); // simplify=true
    } else {
      dataSourceTable.readXml(
          File2.getDecompressedBufferedFileReaderUtf8(tSourceUrl),
          false, // validate?  false since no .dtd
          tRowElementXPath,
          null,
          true); // simplify=true
    }
    if (verbose)
      String2.log(
          "Table.readXml parsed the source xml to this table:\n" + dataSourceTable.toString(5));

    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;
    dataSourceTable.standardize(tStandardizeWhat);

    // remove col[0], OBJECTID, which is an internal number created within this WFS response.

    if (dataSourceTable.nColumns() > 1
        && dataSourceTable.getColumnName(0).toLowerCase().endsWith("objectid"))
      dataSourceTable.removeColumn(0);

    // globalAttributes
    if (externalAddGlobalAttributes == null) externalAddGlobalAttributes = new Attributes();
    if (tInfoUrl != null && tInfoUrl.length() > 0)
      externalAddGlobalAttributes.add("infoUrl", tInfoUrl);
    if (tInstitution != null && tInstitution.length() > 0)
      externalAddGlobalAttributes.add("institution", tInstitution);
    if (tSummary != null && tSummary.length() > 0)
      externalAddGlobalAttributes.add("summary", tSummary);
    if (tTitle != null && tTitle.length() > 0) externalAddGlobalAttributes.add("title", tTitle);
    externalAddGlobalAttributes.set("sourceUrl", tSourceUrl);
    // externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");

    String tSortedColumnSourceName = "";
    for (int col = 0; col < dataSourceTable.nColumns(); col++) {
      String colName = dataSourceTable.getColumnName(col);
      Attributes sourceAtts = dataSourceTable.columnAttributes(col);

      // isDateTime?
      PrimitiveArray sourcePA = (PrimitiveArray) dataSourceTable.getColumn(col).clone();
      String timeUnits = "";
      if (sourcePA instanceof StringArray sa) {
        timeUnits = Calendar2.suggestDateTimeFormat(sa, false); // evenIfPurelyNumeric
        if (timeUnits.length() > 0)
          sourceAtts.set("units", timeUnits); // just temporarily to trick makeReadyToUse...
      }
      PrimitiveArray destPA =
          makeDestPAForGDX(sourcePA, sourceAtts); // possibly with temporary time units

      // make addAtts
      Attributes addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              dataSourceTable.globalAttributes(),
              sourceAtts,
              null,
              colName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true); // tryToFindLLAT

      // put time units
      if (timeUnits.length() > 0) {
        sourceAtts.remove("units"); // undo above
        addAtts.set("units", timeUnits); // correct place
      }

      // add to dataAddTable
      dataAddTable.addColumn(col, colName, destPA, addAtts);

      // files are likely sorted by first date time variable
      // and no harm if files aren't sorted that way
      if (tSortedColumnSourceName.length() == 0 && timeUnits.length() > 0)
        tSortedColumnSourceName = colName;

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(colName, destPA, sourceAtts, addAtts);
    }

    // tryToFindLLAT
    tryToFindLLAT(dataSourceTable, dataAddTable);

    // after dataVariables known, add global attributes in the dataAddTable
    String tDatasetID = suggestDatasetID(tSourceUrl);
    dataAddTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(),
                // another cdm_data_type could be better; this is good for now
                hasLonLatTime(dataAddTable) ? "Point" : "Other",
                EDStatic.fullCopyDirectory + tDatasetID + "/",
                externalAddGlobalAttributes,
                suggestKeywords(dataSourceTable, dataAddTable)));
    dataAddTable.globalAttributes().set("rowElementXPath", tRowElementXPath);

    // subsetVariables
    if (dataSourceTable.globalAttributes().getString("subsetVariables") == null
        && dataAddTable.globalAttributes().getString("subsetVariables") == null)
      dataAddTable
          .globalAttributes()
          .add("subsetVariables", suggestSubsetVariables(dataSourceTable, dataAddTable, false));

    // write the information
    StringBuilder sb = new StringBuilder();
    sb.append(
        "<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n"
            + "  below, notably 'units' for each of the dataVariables. -->\n"
            + "<dataset type=\"EDDTableFromWFSFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>0</updateEveryNMillis>\n"
            + // files are only added by full reload
            // "    <fileNameRegex>.*\\.tsv</fileNameRegex>\n" + //irrelevant/overridden
            // "    <recursive>false</recursive>\n" +            //irrelevant/overridden
            // "    <pathRegex>.*</pathRegex>\n" +               //irrelevant/overridden
            // "    <fileDir>" + EDStatic.fullCopyDirectory + tDatasetID + "/</fileDir>\n" +
            "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>"
            + tStandardizeWhat
            + "</standardizeWhat>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n");
    // "    <charset>UTF-8</charset>\n" +
    // "    <columnNamesRow>1</columnNamesRow>\n" +
    // "    <firstDataRow>3</firstDataRow>\n" +
    // (String2.isSomething(tColumnNameForExtract)? //Discourage Extract. Encourage
    // sourceName=***fileName,...
    //  "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
    //  "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
    //  "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
    //  "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" : "") +
    // "    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
    // "    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n");
    sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
    sb.append(cdmSuggestion());
    sb.append(writeAttsForDatasetsXml(true, dataAddTable.globalAttributes(), "    "));

    sb.append(
        writeVariablesForDatasetsXml(
            dataSourceTable,
            dataAddTable,
            "dataVariable",
            true,
            false)); // includeDataType, questionDestinationName
    sb.append("</dataset>\n" + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }
}
