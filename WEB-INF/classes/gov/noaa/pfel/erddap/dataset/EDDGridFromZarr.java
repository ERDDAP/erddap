/*
 * EDDGridFromZarr Copyright 2026, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import dev.zarr.zarrjava.ZarrException;
import dev.zarr.zarrjava.core.Group;
import dev.zarr.zarrjava.core.Node;
import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.store.HttpStore;
import dev.zarr.zarrjava.store.S3Store;
import dev.zarr.zarrjava.store.Store;
import dev.zarr.zarrjava.store.StoreHandle;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.handlers.EDDGridFromZarrHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.AxisVariableInfo;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import gov.noaa.pfel.erddap.variable.EDVTime;
import gov.noaa.pfel.erddap.variable.EDVTimeStamp;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * This class represents a gridded dataset backed by a Zarr store using the zarr-java library.
 *
 * @author ERDDAP Development Team
 */
@SaxHandlerClass(EDDGridFromZarrHandler.class)
public class EDDGridFromZarr extends EDDGrid {

  // Private instance variables for Zarr configuration and zarr-java handles
  private String zarrStorePath;
  private String zarrGroupName;
  private long chunkCacheSize;
  private Store zarrStore;
  private Group zarrGroup;

  /**
   * Static factory method to construct an EDDGridFromZarr dataset from an XML configuration.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader SimpleXMLReader pointing to the dataset XML element
   * @return EDDGridFromZarr dataset instance
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridFromZarr fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridFromZarr(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    LocalizedAttributes tGlobalAttributes = null;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    ArrayList<AxisVariableInfo> tAxisVariables = new ArrayList<>();
    ArrayList<DataVariableInfo> tDataVariables = new ArrayList<>();
    int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    int tUpdateEveryNMillis = 0;
    String tZarrStorePath = null;
    String tZarrGroupName = "";
    long tChunkCacheSize = -1;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    int tnThreads = -1;
    boolean tDimensionValuesInMemory = true;

    int startOfTagsN = xmlReader.stackSize();
    String startOfTags = xmlReader.allTags();
    int startOfTagsLength = startOfTags.length();

    while (true) {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String content = xmlReader.content();
      if (xmlReader.stackSize() == startOfTagsN) break;
      String localTags = tags.substring(startOfTagsLength);

      switch (localTags) {
        case "<addAttributes>" -> tGlobalAttributes = getAttributesFromXml(xmlReader);
        case "<axisVariable>" -> tAxisVariables.add(getSDAVVariableFromXml(xmlReader));
        case "<dataVariable>" -> tDataVariables.add(getSDADVariableFromXml(xmlReader));
        case "<accessibleTo>",
            "<dimensionValuesInMemory>",
            "<nThreads>",
            "<defaultGraphQuery>",
            "<defaultDataQuery>",
            "<iso19115File>",
            "<fgdcFile>",
            "<onChange>",
            "<zarrStorePath>",
            "<sourceUrl>",
            "<zarrGroupName>",
            "<groupName>",
            "<chunkCacheSize>",
            "<updateEveryNMillis>",
            "<reloadEveryNMinutes>",
            "<accessibleViaWMS>",
            "<graphsAccessibleTo>" -> {}
        case "</accessibleTo>" -> tAccessibleTo = content;
        case "</graphsAccessibleTo>" -> tGraphsAccessibleTo = content;
        case "</accessibleViaWMS>" -> tAccessibleViaWMS = String2.parseBoolean(content);
        case "</reloadEveryNMinutes>" -> tReloadEveryNMinutes = String2.parseInt(content);
        case "</updateEveryNMillis>" -> tUpdateEveryNMillis = String2.parseInt(content);
        case "</zarrStorePath>", "</sourceUrl>" -> tZarrStorePath = content;
        case "</zarrGroupName>", "</groupName>" -> tZarrGroupName = content;
        case "</chunkCacheSize>" -> tChunkCacheSize = String2.parseLong(content);
        case "</onChange>" -> tOnChange.add(content);
        case "</fgdcFile>" -> tFgdcFile = content;
        case "</iso19115File>" -> tIso19115File = content;
        case "</defaultDataQuery>" -> tDefaultDataQuery = content;
        case "</defaultGraphQuery>" -> tDefaultGraphQuery = content;
        case "</nThreads>" -> tnThreads = String2.parseInt(content);
        case "</dimensionValuesInMemory>" ->
            tDimensionValuesInMemory = String2.parseBoolean(content);
        default -> xmlReader.unexpectedTagException();
      }
    }

    return new EDDGridFromZarr(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tGlobalAttributes,
        tAxisVariables,
        tDataVariables,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tZarrStorePath,
        tZarrGroupName,
        tChunkCacheSize,
        tnThreads,
        tDimensionValuesInMemory);
  }

  /**
   * Primary constructor for EDDGridFromZarr.
   *
   * @param tDatasetID short unique ID for this dataset
   * @param tAccessibleTo comma separated roles
   * @param tGraphsAccessibleTo comma separated roles
   * @param tAccessibleViaWMS boolean flag for WMS access
   * @param tOnChange change triggers
   * @param tFgdcFile FGDC metadata file path
   * @param tIso19115File ISO 19115 metadata file path
   * @param tDefaultDataQuery default data request string
   * @param tDefaultGraphQuery default graph request string
   * @param tAddGlobalAttributes global attributes to be merged
   * @param tAxisVariables axis variable specifications
   * @param tDataVariables data variable specifications
   * @param tReloadEveryNMinutes reload interval in minutes
   * @param tUpdateEveryNMillis update interval in milliseconds
   * @param tZarrStorePath local file path, HTTP URL, or S3 URI for Zarr store
   * @param tZarrGroupName subgroup path within Zarr store (or "" / "/" for root)
   * @param tChunkCacheSize chunk cache size settings
   * @param tnThreads number of threads to use
   * @param tDimensionValuesInMemory whether axis values should be cached in memory
   * @throws Throwable if initialization or dataset structure validation fails
   */
  public EDDGridFromZarr(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      LocalizedAttributes tAddGlobalAttributes,
      List<AxisVariableInfo> tAxisVariables,
      List<DataVariableInfo> tDataVariables,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tZarrStorePath,
      String tZarrGroupName,
      long tChunkCacheSize,
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridFromZarr " + tDatasetID);
    int language = EDMessages.DEFAULT_LANGUAGE;
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridFromZarr(" + tDatasetID + ") constructor:\n";

    // Standard EDDGrid field setup
    className = "EDDGridFromZarr";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    if (!tAccessibleViaWMS)
      accessibleViaWMS =
          String2.canonical(MessageFormat.format(EDStatic.messages.get(Message.NO_XXX, 0), "WMS"));
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    if (tAddGlobalAttributes == null) tAddGlobalAttributes = new LocalizedAttributes();
    addGlobalAttributes = tAddGlobalAttributes;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    setUpdateEveryNMillis(tUpdateEveryNMillis);
    nThreads = tnThreads;
    dimensionValuesInMemory = tDimensionValuesInMemory;

    // Zarr parameters
    this.zarrStorePath = String2.isSomething(tZarrStorePath) ? tZarrStorePath : "";
    this.zarrGroupName = tZarrGroupName == null ? "" : tZarrGroupName.trim();
    this.chunkCacheSize = tChunkCacheSize;

    addGlobalAttributes.set(language, "sourceUrl", convertToPublicSourceUrl(this.zarrStorePath));
    localSourceUrl = this.zarrStorePath;

    if (axisVariables == null) {
      axisVariables = new EDVGridAxis[0];
    }

    // Initialize zarr-java Store reader
    try {
      this.zarrStore = createZarrStore(this.zarrStorePath);
    } catch (Exception e) {
      throw new RuntimeException(
          errorInMethod + "Failed to initialize Zarr store at path: " + this.zarrStorePath, e);
    }

    // Open specified Zarr root or subgroup
    try {
      StoreHandle handle;
      if (this.zarrGroupName.isEmpty() || "/".equals(this.zarrGroupName)) {
        handle = this.zarrStore.resolve();
      } else {
        String[] groupKeys = String2.split(this.zarrGroupName, '/');
        handle = this.zarrStore.resolve(groupKeys);
      }
      this.zarrGroup = Group.open(handle);
    } catch (Exception e) {
      throw new RuntimeException(
          errorInMethod
              + "Failed to open Zarr group '"
              + this.zarrGroupName
              + "' in store: "
              + this.zarrStorePath,
          e);
    }

    // Extract .zattrs and populate ERDDAP combinedGlobalAttributes
    sourceGlobalAttributes = new Attributes();
    try {
      dev.zarr.zarrjava.core.Attributes zattrs = this.zarrGroup.metadata().attributes();
      if (zattrs != null) {
        populateAttributesFromZarr(zattrs, sourceGlobalAttributes);
      }
    } catch (ZarrException ze) {
      String2.log(errorInMethod + "Warning: Could not read .zattrs metadata: " + ze.getMessage());
    }

    combinedGlobalAttributes =
        new LocalizedAttributes(addGlobalAttributes, sourceGlobalAttributes);
    String tLicense = combinedGlobalAttributes.getString(language, "license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          language,
          "license",
          String2.replaceAll(tLicense, "[standard]", EDStatic.messages.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");
    if (combinedGlobalAttributes.getString(language, "cdm_data_type") == null)
      combinedGlobalAttributes.set(language, "cdm_data_type", "Grid");

    // Initialize axes and data variables
    if (tDataVariables != null) {
      dataVariables = new EDV[tDataVariables.size()];
      for (int dv = 0; dv < tDataVariables.size(); dv++) {
        String tDataSourceName = tDataVariables.get(dv).sourceName();
        String tDataDestName = tDataVariables.get(dv).destinationName();
        if (!String2.isSomething(tDataDestName)) tDataDestName = tDataSourceName;

        Attributes tDataSourceAtts = new Attributes();
        LocalizedAttributes tDataAddAtts = tDataVariables.get(dv).attributes();
        if (tDataAddAtts == null) tDataAddAtts = new LocalizedAttributes();

        String dvSourceDataType = tDataVariables.get(dv).dataType();
        if (!String2.isSomething(dvSourceDataType)) dvSourceDataType = "double";

        if (tDataDestName.equals(EDV.TIME_NAME))
          throw new RuntimeException(
              errorInMethod + "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
        else if (EDVTime.hasTimeUnits(language, tDataSourceAtts, tDataAddAtts))
          dataVariables[dv] =
              new EDVTimeStamp(
                  datasetID,
                  tDataSourceName,
                  tDataDestName,
                  tDataSourceAtts,
                  tDataAddAtts,
                  dvSourceDataType);
        else
          dataVariables[dv] =
              new EDV(
                  datasetID,
                  tDataSourceName,
                  tDataDestName,
                  tDataSourceAtts,
                  tDataAddAtts,
                  dvSourceDataType,
                  PAOne.fromDouble(Double.NaN),
                  PAOne.fromDouble(Double.NaN));
        dataVariables[dv].extractAndSetActualRange(language);
      }
    }

    ensureValid();

    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + this : "")
              + "\n*** EDDGridFromZarr "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms\n");

    if (!dimensionValuesInMemory) saveDimensionValuesInFile();
  }

  /**
   * Helper method to instantiate the appropriate zarr-java Store for local, HTTP, or S3 URIs.
   */
  private static Store createZarrStore(String path) throws IOException {
    if (path == null) throw new IllegalArgumentException("Zarr store path cannot be null.");
    if (path.startsWith("http://") || path.startsWith("https://")) {
      return new HttpStore(path);
    } else if (path.startsWith("s3://") || path.startsWith("s3a://")) {
      String s3Path = path.substring(path.indexOf("://") + 3);
      int firstSlash = s3Path.indexOf('/');
      String bucket = firstSlash > 0 ? s3Path.substring(0, firstSlash) : s3Path;
      String keyPrefix = firstSlash > 0 ? s3Path.substring(firstSlash + 1) : "";
      S3Client s3Client = S3Client.create();
      return new S3Store(s3Client, bucket, keyPrefix);
    } else {
      return new FilesystemStore(Paths.get(path));
    }
  }

  /**
   * Helper method to map dev.zarr.zarrjava.core.Attributes entries into ERDDAP Attributes.
   */
  public static void populateAttributesFromZarr(
      dev.zarr.zarrjava.core.Attributes zattrs, Attributes erddapAtts) {
    if (zattrs == null || erddapAtts == null) return;
    for (Map.Entry<String, Object> entry : zattrs.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value == null) continue;
      if (value instanceof String s) {
        erddapAtts.set(key, s);
      } else if (value instanceof Number n) {
        if (value instanceof Double || value instanceof Float) {
          erddapAtts.set(key, n.doubleValue());
        } else if (value instanceof Long) {
          erddapAtts.set(key, n.longValue());
        } else {
          erddapAtts.set(key, n.intValue());
        }
      } else if (value instanceof Boolean b) {
        erddapAtts.set(key, b ? "true" : "false");
      } else if (value instanceof List<?> list) {
        PrimitiveArray pa = PrimitiveArray.factory(list);
        erddapAtts.set(key, pa);
      } else if (value.getClass().isArray()) {
        PrimitiveArray pa = PrimitiveArray.factory(value);
        erddapAtts.set(key, pa);
      } else {
        erddapAtts.set(key, value.toString());
      }
    }
  }

  /**
   * Constructs a sibling dataset for a new Zarr store source URL.
   */
  @Override
  public EDDGrid sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable {
    if (verbose) String2.log("EDDGridFromZarr.sibling " + tLocalSourceUrl);

    int nAv = axisVariables != null ? axisVariables.length : 0;
    ArrayList<AxisVariableInfo> tAxisVariables = new ArrayList<>(nAv);
    for (int av = 0; av < nAv; av++) {
      tAxisVariables.add(
          new AxisVariableInfo(
              axisVariables[av].sourceName(),
              axisVariables[av].destinationName(),
              axisVariables[av].addAttributes(),
              null));
    }

    int nDv = dataVariables != null ? dataVariables.length : 0;
    ArrayList<DataVariableInfo> tDataVariables = new ArrayList<>(nDv);
    for (int dv = 0; dv < nDv; dv++) {
      tDataVariables.add(
          new DataVariableInfo(
              dataVariables[dv].sourceName(),
              dataVariables[dv].destinationName(),
              dataVariables[dv].addAttributes(),
              dataVariables[dv].sourceDataType()));
    }

    int po = datasetID.length() / 2;
    String tDatasetID =
        datasetID.substring(0, po)
            + "_"
            + String2.md5Hex12(tLocalSourceUrl)
            + "_"
            + datasetID.substring(po);

    EDDGridFromZarr newEDDGrid =
        new EDDGridFromZarr(
            tDatasetID,
            String2.toSSVString(accessibleTo),
            "auto",
            false,
            shareInfo ? onChange : (StringArray) onChange.clone(),
            "",
            "",
            "",
            "",
            addGlobalAttributes,
            tAxisVariables,
            tDataVariables,
            getReloadEveryNMinutes(),
            getUpdateEveryNMillis(),
            tLocalSourceUrl,
            zarrGroupName,
            chunkCacheSize,
            nThreads,
            dimensionValuesInMemory);

    if (shareInfo) {
      boolean testAV0 = false;
      String results = similar(newEDDGrid, firstAxisToMatch, matchAxisNDigits, testAV0);
      if (results.length() > 0) throw new SimpleException("Error in EDDGrid.sibling: " + results);

      for (int av = 1; av < nAv; av++)
        newEDDGrid.axisVariables()[av] = axisVariables[av];
      newEDDGrid.dataVariables = dataVariables;

      newEDDGrid.axisVariableSourceNames = axisVariableSourceNames();
      newEDDGrid.axisVariableDestinationNames = axisVariableDestinationNames();

      newEDDGrid.dataVariableSourceNames = dataVariableSourceNames();
      newEDDGrid.dataVariableDestinationNames = dataVariableDestinationNames();
      newEDDGrid.sourceGlobalAttributes = sourceGlobalAttributes();
      newEDDGrid.addGlobalAttributes = addGlobalAttributes();
      newEDDGrid.combinedGlobalAttributes = combinedGlobalAttributes();
    }

    return newEDDGrid;
  }

  /**
   * Helper method stub to retrieve axis values from Zarr dataset.
   *
   * // TODO (Prompt 2) / // TODO (Prompt 3)
   */
  public PrimitiveArray getAxisData(int axisIndex) throws Throwable {
    // TODO (Prompt 2) / // TODO (Prompt 3): Implement axis variable data extraction from Zarr store
    throw new UnsupportedOperationException("getAxisData not yet implemented.");
  }

  /**
   * Gets source data (not yet converted to destination data) for this EDDGrid dataset.
   *
   * @param language user language
   * @param tDirTable directory table if applicable
   * @param tFileTable file table if applicable
   * @param tDataVariables requested data variables
   * @param tConstraints requested constraints (start, stride, stop)
   * @return PrimitiveArray[] containing axis values followed by data values
   * @throws Throwable if error
   *
   * // TODO (Prompt 2) / // TODO (Prompt 3)
   */
  @Override
  public PrimitiveArray[] getSourceData(
      int language, Table tDirTable, Table tFileTable, EDV tDataVariables[], IntArray tConstraints)
      throws Throwable {
    // TODO (Prompt 2) / // TODO (Prompt 3): Implement Zarr chunk data reading and array slice loading
    throw new UnsupportedOperationException("getSourceData for Zarr not yet implemented.");
  }

  /**
   * Incremental update method for real-time dataset growth.
   *
   * @param language user language
   * @param msg log prefix message
   * @param startUpdateMillis start timestamp of update
   * @return true if updated
   * @throws Throwable if error
   *
   * // TODO (Prompt 2) / // TODO (Prompt 3)
   */
  @Override
  public boolean lowUpdate(int language, String msg, long startUpdateMillis) throws Throwable {
    // TODO (Prompt 2) / // TODO (Prompt 3): Implement lowUpdate for checking growing dimensions in Zarr store
    return false;
  }

  /**
   * Generates a suggested datasets.xml configuration block for a Zarr store.
   *
   * @param zarrStorePath path or URL to the Zarr store
   * @param zarrGroupName group name within the store
   * @return suggested XML string
   * @throws Throwable if error
   *
   * // TODO (Prompt 2) / // TODO (Prompt 3)
   */
  public static String generateDatasetsXml(String zarrStorePath, String zarrGroupName)
      throws Throwable {
    // TODO (Prompt 2) / // TODO (Prompt 3): Implement datasets.xml generation for Zarr datasets
    throw new UnsupportedOperationException("generateDatasetsXml for Zarr not yet implemented.");
  }
}
