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
import dev.zarr.zarrjava.core.Array;
import dev.zarr.zarrjava.core.ArrayMetadata;
import dev.zarr.zarrjava.core.Group;
import dev.zarr.zarrjava.core.Node;
import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.store.HttpStore;
import dev.zarr.zarrjava.store.S3Store;
import dev.zarr.zarrjava.store.Store;
import dev.zarr.zarrjava.store.StoreHandle;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // Discover Zarr metadata
    Map<String, ZarrArrayInfo> arrayMap = parseZarrMetadata();

    // Build grid axes
    this.axisVariables = buildGridAxes(tAxisVariables, arrayMap);

    // Collect axis source names
    Set<String> axisSourceNames = new HashSet<>();
    for (EDVGridAxis axis : this.axisVariables) {
      if (axis != null && axis.sourceName() != null) {
        axisSourceNames.add(axis.sourceName());
      }
    }

    // Build data variables
    this.dataVariables = buildDataVariables(tDataVariables, arrayMap, axisSourceNames);

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
   * Helper method to retrieve axis values from Zarr dataset.
   *
   * @param axisIndex index of axis variable
   * @return PrimitiveArray containing axis values
   * @throws Throwable if error
   */
  public PrimitiveArray getAxisData(int axisIndex) throws Throwable {
    if (axisVariables != null && axisIndex >= 0 && axisIndex < axisVariables.length) {
      return axisVariables[axisIndex].sourceValues();
    }
    return null;
  }

  /**
   * Fetches a strided multidimensional subset for a single data variable from the Zarr store.
   *
   * @param edv the data variable requested
   * @param start 0-based start indices for each dimension
   * @param stride step sizes for each dimension
   * @param stop 0-based stop indices (inclusive) for each dimension
   * @return flat 1D PrimitiveArray containing requested data subset in row-major order
   * @throws Throwable if error
   */
  public PrimitiveArray getSourceDataFromFile(
      EDV edv, IntArray start, IntArray stride, IntArray stop) throws Throwable {
    if (start == null || stride == null || stop == null) {
      throw new IllegalArgumentException("start, stride, and stop parameters cannot be null.");
    }
    return getSourceDataFromFile(edv, start.toArray(), stride.toArray(), stop.toArray());
  }

  /**
   * Fetches a strided multidimensional subset for a single data variable from the Zarr store.
   *
   * <p>Math behind chunk-to-index mapping and slicing:
   * 1. For a given dimension d with Zarr total length S_d and chunk size C_d:
   *    - Min chunk index overlapping [start_d, stop_d]: c_min,d = start_d / C_d
   *    - Max chunk index overlapping [start_d, stop_d]: c_max,d = stop_d / C_d
   * 2. For each chunk at chunk coordinates (c_0, c_1, ..., c_{rank-1}):
   *    - Physical chunk start offset in full Zarr array: P_start,d = c_d * C_d
   *    - Physical chunk end offset (exclusive): P_end,d = min((c_d + 1) * C_d, S_d)
   * 3. Requested output element index k_d in range [0, N_d - 1] corresponds to full Zarr index:
   *    - i_d = start_d + k_d * stride_d
   *    - Overlap with this chunk requires: P_start,d <= i_d < P_end,d
   * 4. Output index bounds k_min,d and k_max,d for dimension d in this chunk are:
   *    - k_min,d = max(0, ceilDiv(P_start,d - start_d, stride_d))
   *    - k_max,d = min(N_d - 1, floorDiv(P_end,d - 1 - start_d, stride_d))
   * 5. If k_min,d > k_max,d for any dimension d, the chunk does not overlap requested range.
   * 6. Otherwise, for each point (k_0, ..., k_{rank-1}) in range [k_min, k_max]:
   *    - Local chunk offset: localOffset_d = (start_d + k_d * stride_d) - P_start,d
   *    - Flat 1D output offset in C-order row-major layout:
   *      outIdx = sum(k_d * product_{m=d+1}^{rank-1} N_m)
   *
   * @param edv the data variable requested
   * @param start 0-based start indices for each dimension
   * @param stride step sizes for each dimension
   * @param stop 0-based stop indices (inclusive) for each dimension
   * @return flat 1D PrimitiveArray containing requested data subset in row-major order
   * @throws Throwable if error
   */
  public PrimitiveArray getSourceDataFromFile(
      EDV edv, int[] start, int[] stride, int[] stop) throws Throwable {

    if (edv == null) {
      throw new IllegalArgumentException("EDV data variable cannot be null.");
    }
    String sourceName = edv.sourceName();
    if (!String2.isSomething(sourceName)) {
      throw new IllegalArgumentException("EDV sourceName is empty.");
    }

    // Retrieve Zarr array handle
    ZarrArrayInfo info = getOrOpenZarrArrayInfo(sourceName, new LinkedHashMap<>());
    Array zarray = info != null ? info.array : null;
    if (zarray == null) {
      try {
        Node node = zarrGroup.get(sourceName);
        if (node instanceof Array za) {
          zarray = za;
        }
      } catch (Throwable t) {
        try {
          StoreHandle childHandle = zarrGroup.storeHandle.resolve(sourceName);
          zarray = Array.open(childHandle);
        } catch (Throwable t2) {
          throw new SimpleException("Data variable '" + sourceName + "' not found in Zarr store.", t2);
        }
      }
    }
    if (zarray == null) {
      throw new SimpleException("Data variable '" + sourceName + "' not found in Zarr store.");
    }

    ArrayMetadata metadata = zarray.metadata();
    long[] shape = metadata.shape;
    int[] chunkShape = metadata.chunkShape();
    int rank = shape.length;

    if (start.length != rank || stride.length != rank || stop.length != rank) {
      throw new IllegalArgumentException(
          "Constraint dimension count (" + start.length + ") does not match Zarr array rank (" + rank + ") for variable '" + sourceName + "'.");
    }

    // Validate request bounds and calculate output dimension lengths
    int[] nRequested = new int[rank];
    long totalSizeL = 1;
    for (int d = 0; d < rank; d++) {
      if (start[d] < 0 || stop[d] >= shape[d] || start[d] > stop[d] || stride[d] < 1) {
        throw new IllegalArgumentException(
            "Invalid slice bounds for dimension " + d + " on '" + sourceName + "': start=" + start[d] + ", stride=" + stride[d] + ", stop=" + stop[d] + ", shape=" + shape[d]);
      }
      nRequested[d] = (stop[d] - start[d]) / stride[d] + 1;
      totalSizeL *= nRequested[d];
    }

    if (totalSizeL > Integer.MAX_VALUE) {
      throw new SimpleException("Requested data size (" + totalSizeL + ") exceeds maximum allowed PrimitiveArray length.");
    }
    int totalSize = (int) totalSizeL;

    // Determine unpacking & target PrimitiveArray type
    double scaleFactor = edv.scaleFactor();
    double addOffset = edv.addOffset();
    boolean unpack = edv.scaleAddOffset();

    PAType destPAType = unpack ? edv.destinationDataPAType() : edv.sourceDataPAType();
    PrimitiveArray destPA = PrimitiveArray.factory(destPAType, totalSize, true);

    // Missing value definitions
    double sourceMissingDouble = edv.sourceMissingValue();
    double sourceFillDouble = edv.sourceFillValue();
    double destMissingDouble = edv.destinationMissingValue();

    // Check Zarr parsed fill value from metadata if available
    Object parsedFV = metadata.parsedFillValue();
    if (parsedFV instanceof Number n) {
      double fvDouble = n.doubleValue();
      if (Double.isNaN(sourceFillDouble)) {
        sourceFillDouble = fvDouble;
      }
    }

    // Compute chunk coordinate ranges for each dimension
    int[] minChunk = new int[rank];
    int[] maxChunk = new int[rank];
    int[] nChunks = new int[rank];
    int totalChunks = 1;

    for (int d = 0; d < rank; d++) {
      int cSize = chunkShape[d];
      minChunk[d] = start[d] / cSize;
      maxChunk[d] = stop[d] / cSize;
      nChunks[d] = maxChunk[d] - minChunk[d] + 1;
      totalChunks *= nChunks[d];
    }

    // Precompute dimension multiplier strides for C-order row-major output index mapping
    int[] outStrides = new int[rank];
    if (rank > 0) {
      outStrides[rank - 1] = 1;
      for (int d = rank - 2; d >= 0; d--) {
        outStrides[d] = outStrides[d + 1] * nRequested[d + 1];
      }
    }

    // Process overlapping chunks
    int[] chunkCoordOffset = new int[rank];
    long[] chunkCoords = new long[rank];
    int[] kMin = new int[rank];
    int[] kMax = new int[rank];

    for (int cIdx = 0; cIdx < totalChunks; cIdx++) {
      int temp = cIdx;
      for (int d = rank - 1; d >= 0; d--) {
        chunkCoordOffset[d] = temp % nChunks[d];
        temp /= nChunks[d];
        chunkCoords[d] = minChunk[d] + chunkCoordOffset[d];
      }

      // Calculate chunk physical bounds and output index overlap bounds [kMin, kMax]
      boolean hasOverlap = true;
      long[] physStart = new long[rank];
      long[] physEnd = new long[rank];

      for (int d = 0; d < rank; d++) {
        int cSize = chunkShape[d];
        physStart[d] = chunkCoords[d] * cSize;
        physEnd[d] = Math.min((chunkCoords[d] + 1) * cSize, shape[d]);

        long pStart = physStart[d];
        long pEnd = physEnd[d];

        // Smallest k >= 0 such that start_d + k * stride_d >= pStart
        int minK = 0;
        if (pStart > start[d]) {
          long num = pStart - start[d];
          minK = (int) ((num + stride[d] - 1) / stride[d]);
        }
        kMin[d] = Math.max(0, minK);

        // Largest k < nRequested such that start_d + k * stride_d < pEnd
        int maxK = nRequested[d] - 1;
        if (pEnd - 1 < start[d] + (long) (nRequested[d] - 1) * stride[d]) {
          long num = (pEnd - 1) - start[d];
          maxK = num < 0 ? -1 : (int) (num / stride[d]);
        }
        kMax[d] = Math.min(nRequested[d] - 1, maxK);

        if (kMin[d] > kMax[d]) {
          hasOverlap = false;
          break;
        }
      }

      if (!hasOverlap) continue;

      // Read chunk data via zarr-java
      ucar.ma2.Array chunkData = null;
      try {
        chunkData = zarray.readChunk(chunkCoords);
      } catch (Throwable t) {
        // Unwritten, missing, or empty chunk -> treated as missing fill region
        chunkData = null;
      }

      // Calculate total element iterations within overlap for this chunk
      int[] overlapShape = new int[rank];
      int chunkIterTotal = 1;
      for (int d = 0; d < rank; d++) {
        overlapShape[d] = kMax[d] - kMin[d] + 1;
        chunkIterTotal *= overlapShape[d];
      }

      int[] localK = new int[rank];
      int[] chunkLocalOffset = new int[rank];

      for (int iter = 0; iter < chunkIterTotal; iter++) {
        int tempIter = iter;
        int outIdx = 0;

        for (int d = rank - 1; d >= 0; d--) {
          int offsetInOverlap = tempIter % overlapShape[d];
          tempIter /= overlapShape[d];

          localK[d] = kMin[d] + offsetInOverlap;
          outIdx += localK[d] * outStrides[d];

          long fullIdx = start[d] + (long) localK[d] * stride[d];
          chunkLocalOffset[d] = (int) (fullIdx - physStart[d]);
        }

        if (chunkData == null) {
          // Fill missing chunk elements with missing value
          setMissingInDest(destPA, outIdx, destPAType, destMissingDouble);
        } else {
          ucar.ma2.Index ma2Idx = chunkData.getIndex();
          ma2Idx.set(chunkLocalOffset);

          if (destPAType == PAType.STRING || destPAType == PAType.CHAR) {
            Object obj = chunkData.getObject(ma2Idx);
            String sVal = obj != null ? obj.toString() : "";
            destPA.setString(outIdx, sVal);
          } else {
            double rawVal = chunkData.getDouble(ma2Idx);

            boolean isMissing = Double.isNaN(rawVal)
                || (!Double.isNaN(sourceMissingDouble) && rawVal == sourceMissingDouble)
                || (!Double.isNaN(sourceFillDouble) && rawVal == sourceFillDouble);

            if (isMissing) {
              setMissingInDest(destPA, outIdx, destPAType, destMissingDouble);
            } else {
              if (unpack) {
                double unpackedVal = rawVal * scaleFactor + addOffset;
                destPA.setDouble(outIdx, unpackedVal);
              } else {
                destPA.setDouble(outIdx, rawVal);
              }
            }
          }
        }
      }
    }

    return destPA;
  }

  private static void setMissingInDest(PrimitiveArray destPA, int index, PAType paType, double missingDouble) {
    if (paType == PAType.STRING || paType == PAType.CHAR) {
      destPA.setString(index, "");
    } else if (Double.isNaN(missingDouble)) {
      destPA.setDouble(index, Double.NaN);
    } else {
      destPA.setDouble(index, missingDouble);
    }
  }

  /**
   * Gets source data (not yet converted to destination data) for this EDDGrid dataset.
   *
   * @param language user language
   * @param tDirTable directory table if applicable
   * @param tFileTable file table if applicable
   * @param tDataVariables requested data variables
   * @param tConstraints requested constraints (start, stride, stop) for each axis variable
   * @return PrimitiveArray[] containing axis values followed by data values
   * @throws Throwable if error
   */
  @Override
  public PrimitiveArray[] getSourceData(
      int language, Table tDirTable, Table tFileTable, EDV tDataVariables[], IntArray tConstraints)
      throws Throwable {

    if (tConstraints == null) {
      throw new IllegalArgumentException("tConstraints cannot be null.");
    }
    int nav = axisVariables != null ? axisVariables.length : 0;
    if (tConstraints.size() != nav * 3) {
      throw new IllegalArgumentException(
          "tConstraints size (" + tConstraints.size() + ") must equal nav * 3 (" + (nav * 3) + ").");
    }

    int[] start = new int[nav];
    int[] stride = new int[nav];
    int[] stop = new int[nav];

    for (int av = 0; av < nav; av++) {
      start[av] = tConstraints.get(av * 3);
      stride[av] = tConstraints.get(av * 3 + 1);
      stop[av] = tConstraints.get(av * 3 + 2);
    }

    int ndv = tDataVariables != null ? tDataVariables.length : 0;
    PrimitiveArray[] results = new PrimitiveArray[nav + ndv];

    // 1. Subset axis variables
    for (int av = 0; av < nav; av++) {
      PrimitiveArray sourceValues = axisVariables[av].sourceValues();
      results[av] = sourceValues.subset(start[av], stride[av], stop[av]);
    }

    // 2. Extract data variables
    for (int dv = 0; dv < ndv; dv++) {
      EDV edv = tDataVariables[dv];
      results[nav + dv] = getSourceDataFromFile(edv, start, stride, stop);
    }

    return results;
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

  /**
   * Helper class to hold metadata for a Zarr array discovered in the store/group.
   */
  protected static class ZarrArrayInfo {
    public String name;
    public Array array;
    public long[] shape;
    public int[] chunkShape;
    public PAType paType;
    public Attributes attributes;
    public String[] dimensionNames;

    public boolean is1D() {
      return shape != null && shape.length == 1;
    }
  }

  /**
   * Parses Zarr metadata from the open Zarr group, discovering all array nodes,
   * extracting array shapes, chunk dimensions, data types, attributes, and dimension names.
   *
   * @return Map of array name to ZarrArrayInfo
   * @throws Throwable if error
   */
  protected Map<String, ZarrArrayInfo> parseZarrMetadata() throws Throwable {
    Map<String, ZarrArrayInfo> arrayMap = new LinkedHashMap<>();
    Node[] nodes;
    try {
      nodes = this.zarrGroup.listAsArray();
    } catch (Exception e) {
      nodes = new Node[0];
    }

    for (Node node : nodes) {
      if (node instanceof Array zarray) {
        String name = getArrayName(zarray);
        if (String2.isSomething(name)) {
          ZarrArrayInfo info = createZarrArrayInfo(name, zarray);
          if (info != null) {
            arrayMap.put(name, info);
          }
        }
      }
    }
    return arrayMap;
  }

  private ZarrArrayInfo getOrOpenZarrArrayInfo(String name, Map<String, ZarrArrayInfo> arrayMap) {
    if (arrayMap.containsKey(name)) {
      return arrayMap.get(name);
    }
    try {
      Node node = this.zarrGroup.get(name);
      if (node instanceof Array zarray) {
        ZarrArrayInfo info = createZarrArrayInfo(name, zarray);
        if (info != null) {
          arrayMap.put(name, info);
          return info;
        }
      }
    } catch (Throwable e) {
      try {
        StoreHandle childHandle = this.zarrGroup.storeHandle.resolve(name);
        Array zarray = Array.open(childHandle);
        ZarrArrayInfo info = createZarrArrayInfo(name, zarray);
        if (info != null) {
          arrayMap.put(name, info);
          return info;
        }
      } catch (Throwable e2) {
        // ignore
      }
    }
    return null;
  }

  private static String getArrayName(Array zarray) {
    if (zarray != null && zarray.storeHandle != null && zarray.storeHandle.keys != null && zarray.storeHandle.keys.length > 0) {
      return zarray.storeHandle.keys[zarray.storeHandle.keys.length - 1];
    }
    return "";
  }

  private static ZarrArrayInfo createZarrArrayInfo(String name, Array zarray) throws Throwable {
    ArrayMetadata metadata = zarray.metadata();
    if (metadata == null) return null;
    long[] shape = metadata.shape;
    if (shape == null) shape = new long[0];
    int[] chunkShape = metadata.chunkShape();
    dev.zarr.zarrjava.core.DataType zType = metadata.dataType();
    ucar.ma2.DataType ma2Type = zType != null ? zType.getMA2DataType() : ucar.ma2.DataType.DOUBLE;
    PAType paType = NcHelper.getElementPAType(ma2Type);

    Attributes erddapAtts = new Attributes();
    dev.zarr.zarrjava.core.Attributes zattrs = metadata.attributes();
    if (zattrs != null) {
      populateAttributesFromZarr(zattrs, erddapAtts);
    }

    if (erddapAtts.get("_FillValue") == null && metadata.parsedFillValue() != null) {
      Object fv = metadata.parsedFillValue();
      if (fv instanceof Number n) {
        if (fv instanceof Double || fv instanceof Float) {
          erddapAtts.set("_FillValue", n.doubleValue());
        } else if (fv instanceof Long) {
          erddapAtts.set("_FillValue", n.longValue());
        } else {
          erddapAtts.set("_FillValue", n.intValue());
        }
      } else if (fv instanceof String s) {
        erddapAtts.set("_FillValue", s);
      }
    }

    String[] dimNames = extractDimensionNames(metadata, shape.length);

    ZarrArrayInfo info = new ZarrArrayInfo();
    info.name = name;
    info.array = zarray;
    info.shape = shape;
    info.chunkShape = chunkShape;
    info.paType = paType;
    info.attributes = erddapAtts;
    info.dimensionNames = dimNames;
    return info;
  }

  private static String[] extractDimensionNames(ArrayMetadata metadata, int rank) {
    // 1. Check Zarr v3 dimensionNames
    if (metadata instanceof dev.zarr.zarrjava.v3.ArrayMetadata v3Meta) {
      if (v3Meta.dimensionNames != null && v3Meta.dimensionNames.length == rank) {
        boolean valid = true;
        for (String d : v3Meta.dimensionNames) {
          if (!String2.isSomething(d)) {
            valid = false;
            break;
          }
        }
        if (valid) return v3Meta.dimensionNames;
      }
    }

    // 2. Check _ARRAY_DIMENSIONS attribute (Zarr v2 / xarray convention)
    try {
      dev.zarr.zarrjava.core.Attributes zattrs = metadata.attributes();
      if (zattrs != null && zattrs.containsKey("_ARRAY_DIMENSIONS")) {
        Object obj = zattrs.get("_ARRAY_DIMENSIONS");
        String[] dims = parseStringArrayObject(obj);
        if (dims != null && dims.length == rank) {
          return dims;
        }
      }
    } catch (Exception e) {
      // ignore
    }

    // 3. Fallback to dim0, dim1, ...
    String[] defaultDims = new String[rank];
    for (int i = 0; i < rank; i++) {
      defaultDims[i] = "dim" + i;
    }
    return defaultDims;
  }

  private static String[] parseStringArrayObject(Object obj) {
    if (obj == null) return null;
    if (obj instanceof String[] sa) return sa;
    if (obj instanceof List<?> list) {
      String[] res = new String[list.size()];
      for (int i = 0; i < list.size(); i++) {
        Object item = list.get(i);
        res[i] = item != null ? item.toString().trim() : "";
      }
      return res;
    }
    if (obj instanceof Object[] oa) {
      String[] res = new String[oa.length];
      for (int i = 0; i < oa.length; i++) {
        res[i] = oa[i] != null ? oa[i].toString().trim() : "";
      }
      return res;
    }
    if (obj instanceof String s) {
      String trimmed = s.trim();
      if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
        trimmed = trimmed.substring(1, trimmed.length() - 1);
      }
      String[] parts = String2.split(trimmed, ',');
      for (int i = 0; i < parts.length; i++) {
        parts[i] = stripQuotes(parts[i]);
      }
      return parts;
    }
    return null;
  }

  private static String stripQuotes(String s) {
    if (s == null) return "";
    s = s.trim();
    if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
      s = s.substring(1, s.length() - 1);
    }
    return s.trim();
  }

  /**
   * Builds ERDDAP EDVGridAxis instances for each dimension.
   *
   * @param tAxisVariables explicit axis specifications from XML (or empty/null for auto-discovery)
   * @param arrayMap map of Zarr array metadata
   * @return EDVGridAxis[] array of constructed grid axes
   * @throws Throwable if error or missing/invalid axes
   */
  protected EDVGridAxis[] buildGridAxes(
      List<AxisVariableInfo> tAxisVariables, Map<String, ZarrArrayInfo> arrayMap)
      throws Throwable {

    List<EDVGridAxis> axesList = new ArrayList<>();

    if (tAxisVariables != null && !tAxisVariables.isEmpty()) {
      for (int av = 0; av < tAxisVariables.size(); av++) {
        AxisVariableInfo avi = tAxisVariables.get(av);
        String sourceName = avi.sourceName();
        String destName = avi.destinationName();
        if (!String2.isSomething(destName)) destName = sourceName;

        ZarrArrayInfo info = getOrOpenZarrArrayInfo(sourceName, arrayMap);
        PrimitiveArray pa = null;
        Attributes sourceAtts = new Attributes();

        if (info != null) {
          if (!info.is1D()) {
            throw new SimpleException(
                "Axis variable '" + sourceName + "' is not a 1D Zarr array (shape rank=" + info.shape.length + ").");
          }
          if (info.attributes != null) info.attributes.copyTo(sourceAtts);

          ucar.ma2.Array nc2Array = info.array.read();
          boolean isUnsigned = NcHelper.isUnsigned(nc2Array.getDataType());
          pa = NcHelper.getPrimitiveArray(nc2Array, true, isUnsigned);
        } else if (avi.values() != null && avi.values().size() > 0) {
          pa = avi.values();
        } else {
          throw new SimpleException(
              "Axis variable '" + sourceName + "' not found in Zarr store as 1D array.");
        }

        if (pa == null || pa.size() == 0) {
          throw new SimpleException("Axis variable '" + sourceName + "' contains no values.");
        }

        sourceAtts.remove("_FillValue");
        sourceAtts.remove("missing_value");

        LocalizedAttributes addAtts = avi.attributes() != null ? avi.attributes() : new LocalizedAttributes();

        EDVGridAxis edvga = makeAxisVariable(
            datasetID, av, sourceName, destName, sourceAtts, addAtts, pa);
        axesList.add(edvga);
      }
    } else {
      // Auto-discovery mode: gather dimension names in order from N-dimensional data arrays
      List<String> orderedDimNames = new ArrayList<>();
      Map<String, Long> dimLengths = new LinkedHashMap<>();

      for (ZarrArrayInfo info : arrayMap.values()) {
        if (!info.is1D() && info.dimensionNames != null) {
          for (int d = 0; d < info.dimensionNames.length; d++) {
            String dimName = info.dimensionNames[d];
            long len = (info.shape != null && d < info.shape.length) ? info.shape[d] : 0;
            if (!dimLengths.containsKey(dimName)) {
              orderedDimNames.add(dimName);
              dimLengths.put(dimName, len);
            }
          }
        }
      }

      // If no N-dimensional arrays, fallback to all 1D arrays
      if (orderedDimNames.isEmpty()) {
        for (ZarrArrayInfo info : arrayMap.values()) {
          if (info.is1D()) {
            orderedDimNames.add(info.name);
            dimLengths.put(info.name, info.shape[0]);
          }
        }
      }

      for (int av = 0; av < orderedDimNames.size(); av++) {
        String dimName = orderedDimNames.get(av);
        long dimLen = dimLengths.getOrDefault(dimName, 0L);

        ZarrArrayInfo info = getOrOpenZarrArrayInfo(dimName, arrayMap);
        PrimitiveArray pa = null;
        Attributes sourceAtts = new Attributes();

        if (info != null && info.is1D()) {
          if (info.attributes != null) info.attributes.copyTo(sourceAtts);
          ucar.ma2.Array nc2Array = info.array.read();
          boolean isUnsigned = NcHelper.isUnsigned(nc2Array.getDataType());
          pa = NcHelper.getPrimitiveArray(nc2Array, true, isUnsigned);
        } else {
          // Fallback: generate default index array 0, 1, ..., dimLen - 1
          pa = PrimitiveArray.factory(PAType.INT, (int) dimLen, false);
          for (int i = 0; i < dimLen; i++) {
            pa.addInt(i);
          }
        }

        if (pa == null || pa.size() == 0) {
          throw new SimpleException("Dimension '" + dimName + "' has size 0.");
        }

        sourceAtts.remove("_FillValue");
        sourceAtts.remove("missing_value");

        EDVGridAxis edvga = makeAxisVariable(
            datasetID, av, dimName, dimName, sourceAtts, new LocalizedAttributes(), pa);
        axesList.add(edvga);
      }
    }

    return axesList.toArray(new EDVGridAxis[0]);
  }

  /**
   * Builds ERDDAP EDV data variables for gridded data arrays.
   *
   * @param tDataVariables explicit data variable specifications from XML (or null/empty for auto-discovery)
   * @param arrayMap map of Zarr array metadata
   * @param axisSourceNames set of array names used as coordinate axes
   * @return EDV[] array of constructed data variables
   * @throws Throwable if error
   */
  protected EDV[] buildDataVariables(
      List<DataVariableInfo> tDataVariables,
      Map<String, ZarrArrayInfo> arrayMap,
      Set<String> axisSourceNames)
      throws Throwable {

    int language = EDMessages.DEFAULT_LANGUAGE;
    List<EDV> dvList = new ArrayList<>();

    if (tDataVariables != null && !tDataVariables.isEmpty()) {
      for (int dv = 0; dv < tDataVariables.size(); dv++) {
        DataVariableInfo dvi = tDataVariables.get(dv);
        String tDataSourceName = dvi.sourceName();
        String tDataDestName = dvi.destinationName();
        if (!String2.isSomething(tDataDestName)) tDataDestName = tDataSourceName;

        ZarrArrayInfo info = getOrOpenZarrArrayInfo(tDataSourceName, arrayMap);

        Attributes tDataSourceAtts = new Attributes();
        if (info != null && info.attributes != null) {
          info.attributes.copyTo(tDataSourceAtts);
        }

        LocalizedAttributes tDataAddAtts = dvi.attributes();
        if (tDataAddAtts == null) tDataAddAtts = new LocalizedAttributes();

        String dvSourceDataType = dvi.dataType();
        if (!String2.isSomething(dvSourceDataType) && info != null && info.paType != null) {
          dvSourceDataType = PAType.toCohortString(info.paType);
        }
        if (!String2.isSomething(dvSourceDataType)) dvSourceDataType = "double";

        if (tDataDestName.equals(EDV.TIME_NAME)) {
          throw new SimpleException(
              "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
        }

        EDV edv;
        if (EDVTime.hasTimeUnits(language, tDataSourceAtts, tDataAddAtts)) {
          edv = new EDVTimeStamp(
              datasetID,
              tDataSourceName,
              tDataDestName,
              tDataSourceAtts,
              tDataAddAtts,
              dvSourceDataType);
        } else {
          edv = new EDV(
              datasetID,
              tDataSourceName,
              tDataDestName,
              tDataSourceAtts,
              tDataAddAtts,
              dvSourceDataType,
              PAOne.fromDouble(Double.NaN),
              PAOne.fromDouble(Double.NaN));
        }
        edv.extractAndSetActualRange(language);
        dvList.add(edv);
      }
    } else {
      // Auto-discovery mode: find all N-dimensional arrays in group not used as axes
      for (ZarrArrayInfo info : arrayMap.values()) {
        if (axisSourceNames.contains(info.name)) continue;
        if (info.is1D() && isLikelyAxisArray(info)) continue;

        String tDataSourceName = info.name;
        String tDataDestName = info.name;

        Attributes tDataSourceAtts = new Attributes();
        if (info.attributes != null) {
          info.attributes.copyTo(tDataSourceAtts);
        }

        LocalizedAttributes tDataAddAtts = new LocalizedAttributes();
        String dvSourceDataType = info.paType != null ? PAType.toCohortString(info.paType) : "double";

        if (tDataDestName.equals(EDV.TIME_NAME)) continue;

        EDV edv;
        if (EDVTime.hasTimeUnits(language, tDataSourceAtts, tDataAddAtts)) {
          edv = new EDVTimeStamp(
              datasetID,
              tDataSourceName,
              tDataDestName,
              tDataSourceAtts,
              tDataAddAtts,
              dvSourceDataType);
        } else {
          edv = new EDV(
              datasetID,
              tDataSourceName,
              tDataDestName,
              tDataSourceAtts,
              tDataAddAtts,
              dvSourceDataType,
              PAOne.fromDouble(Double.NaN),
              PAOne.fromDouble(Double.NaN));
        }
        edv.extractAndSetActualRange(language);
        dvList.add(edv);
      }
    }

    if (dvList.isEmpty()) {
      throw new SimpleException("No gridded data variables found in Zarr store.");
    }

    return dvList.toArray(new EDV[0]);
  }

  private static boolean isLikelyAxisArray(ZarrArrayInfo info) {
    if (info == null || !info.is1D()) return false;
    String name = info.name.toLowerCase();
    if (name.equals("time") || name.equals("lat") || name.equals("latitude")
        || name.equals("lon") || name.equals("longitude") || name.equals("depth")
        || name.equals("alt") || name.equals("altitude") || name.equals("elevation")) {
      return true;
    }
    if (info.attributes != null) {
      if (info.attributes.get("axis") != null || info.attributes.get("_CoordinateAxisType") != null) {
        return true;
      }
    }
    return false;
  }
}
