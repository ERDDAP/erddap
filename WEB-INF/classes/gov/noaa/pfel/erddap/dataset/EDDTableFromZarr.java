/*
 * EDDTableFromZarr Copyright 2026, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
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
import dev.zarr.zarrjava.ZarrException;
import dev.zarr.zarrjava.core.Array;
import dev.zarr.zarrjava.core.ArrayMetadata;
import dev.zarr.zarrjava.core.Group;
import dev.zarr.zarrjava.core.Node;
import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.store.HttpStore;
import dev.zarr.zarrjava.store.ReadOnlyZipStore;
import dev.zarr.zarrjava.store.S3Store;
import dev.zarr.zarrjava.store.Store;
import dev.zarr.zarrjava.store.StoreHandle;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.handlers.EDDTableFromZarrHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import gov.noaa.pfel.erddap.variable.EDVDepth;
import gov.noaa.pfel.erddap.variable.EDVLat;
import gov.noaa.pfel.erddap.variable.EDVLon;
import gov.noaa.pfel.erddap.variable.EDVTime;
import gov.noaa.pfel.erddap.variable.EDVTimeStamp;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * This class represents a tabular dataset backed by a Zarr store using the zarr-java library.
 *
 * @author ERDDAP Development Team
 */
@SaxHandlerClass(EDDTableFromZarrHandler.class)
public class EDDTableFromZarr extends EDDTable {

  // Private instance variables for Zarr configuration and handles
  private String zarrStorePath;
  private String zarrGroupName;
  private String rowDimensionName;
  private long chunkCacheSize;
  private String awsRegion;
  private String awsEndpoint;
  private String cacheFromUrl;

  private volatile Store zarrStore;
  private volatile Group zarrGroup;
  private volatile long numRows = -1;
  private volatile Map<String, ZarrArrayInfo> zarrArrayInfoMap;
  private final ReentrantReadWriteLock zarrLock = new ReentrantReadWriteLock();

  /**
   * Static factory method to construct an EDDTableFromZarr dataset from an XML configuration.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader SimpleXMLReader pointing to the dataset XML element
   * @return EDDTableFromZarr dataset instance
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromZarr fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromZarr(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    LocalizedAttributes tGlobalAttributes = null;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tSosOfferingPrefix = null;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    String tAddVariablesWhere = null;
    ArrayList<DataVariableInfo> tDataVariables = new ArrayList<>();
    int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    String tZarrStorePath = null;
    String tZarrGroupName = "";
    String tRowDimensionName = null;
    long tChunkCacheSize = -1;
    String tAwsRegion = null;
    String tAwsEndpoint = null;
    String tCacheFromUrl = null;

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
        case "<dataVariable>" -> tDataVariables.add(getSDADVariableFromXml(xmlReader));
        case "<accessibleTo>",
            "<graphsAccessibleTo>",
            "<onChange>",
            "<fgdcFile>",
            "<iso19115File>",
            "<sosOfferingPrefix>",
            "<defaultDataQuery>",
            "<defaultGraphQuery>",
            "<addVariablesWhere>",
            "<reloadEveryNMinutes>",
            "<zarrStorePath>",
            "<sourceUrl>",
            "<zarrGroupName>",
            "<groupName>",
            "<rowDimensionName>",
            "<rowDimension>",
            "<chunkCacheSize>",
            "<awsRegion>",
            "<awsEndpoint>",
            "<cacheFromUrl>" -> {}
        case "</accessibleTo>" -> tAccessibleTo = content;
        case "</graphsAccessibleTo>" -> tGraphsAccessibleTo = content;
        case "</onChange>" -> tOnChange.add(content);
        case "</fgdcFile>" -> tFgdcFile = content;
        case "</iso19115File>" -> tIso19115File = content;
        case "</sosOfferingPrefix>" -> tSosOfferingPrefix = content;
        case "</defaultDataQuery>" -> tDefaultDataQuery = content;
        case "</defaultGraphQuery>" -> tDefaultGraphQuery = content;
        case "</addVariablesWhere>" -> tAddVariablesWhere = content;
        case "</reloadEveryNMinutes>" -> tReloadEveryNMinutes = String2.parseInt(content);
        case "</zarrStorePath>", "</sourceUrl>" -> tZarrStorePath = content;
        case "</zarrGroupName>", "</groupName>" -> tZarrGroupName = content;
        case "</rowDimensionName>", "</rowDimension>" -> tRowDimensionName = content;
        case "</chunkCacheSize>" -> tChunkCacheSize = String2.parseLong(content);
        case "</awsRegion>" -> tAwsRegion = content;
        case "</awsEndpoint>" -> tAwsEndpoint = content;
        case "</cacheFromUrl>" -> tCacheFromUrl = content;
        default -> xmlReader.unexpectedTagException();
      }
    }

    return new EDDTableFromZarr(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        tZarrStorePath,
        tZarrGroupName,
        tRowDimensionName,
        tChunkCacheSize,
        tAwsRegion,
        tAwsEndpoint,
        tCacheFromUrl);
  }

  /**
   * Primary constructor for EDDTableFromZarr.
   *
   * @param tDatasetID unique dataset ID
   * @param tAccessibleTo roles allowed to access
   * @param tGraphsAccessibleTo roles allowed to access graphs
   * @param tOnChange onChange triggers
   * @param tFgdcFile FGDC metadata file path
   * @param tIso19115File ISO 19115 metadata file path
   * @param tSosOfferingPrefix SOS offering prefix
   * @param tDefaultDataQuery default data query string
   * @param tDefaultGraphQuery default graph query string
   * @param tAddVariablesWhere addVariablesWhere CSV string
   * @param tAddGlobalAttributes attributes to add to source global attributes
   * @param tDataVariables list of data variables
   * @param tReloadEveryNMinutes reload interval in minutes
   * @param tZarrStorePath Zarr store URI or file path
   * @param tZarrGroupName Zarr subgroup path
   * @param tRowDimensionName name of main row dimension
   * @param tChunkCacheSize chunk cache size
   * @param tAwsRegion AWS region name
   * @param tAwsEndpoint AWS custom endpoint
   * @throws Throwable if initialization fails
   */
  public EDDTableFromZarr(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      LocalizedAttributes tAddGlobalAttributes,
      List<DataVariableInfo> tDataVariables,
      int tReloadEveryNMinutes,
      String tZarrStorePath,
      String tZarrGroupName,
      String tRowDimensionName,
      long tChunkCacheSize,
      String tAwsRegion,
      String tAwsEndpoint)
      throws Throwable {
    this(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tAddGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        tZarrStorePath,
        tZarrGroupName,
        tRowDimensionName,
        tChunkCacheSize,
        tAwsRegion,
        tAwsEndpoint,
        null);
  }

  /** Constructs an EDDTableFromZarr instance with all settings including cacheFromUrl. */
  public EDDTableFromZarr(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      LocalizedAttributes tAddGlobalAttributes,
      List<DataVariableInfo> tDataVariables,
      int tReloadEveryNMinutes,
      String tZarrStorePath,
      String tZarrGroupName,
      String tRowDimensionName,
      long tChunkCacheSize,
      String tAwsRegion,
      String tAwsEndpoint,
      String tCacheFromUrl)
      throws Throwable {

    super();

    if (verbose) String2.log("\n*** constructing EDDTableFromZarr " + tDatasetID);
    String errorInMethod = "Error in EDDTableFromZarr(" + tDatasetID + ") constructor:\n";
    int language = 0;

    className = "EDDTableFromZarr";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    if (tAddGlobalAttributes == null) tAddGlobalAttributes = new LocalizedAttributes();
    addGlobalAttributes = tAddGlobalAttributes;
    setReloadEveryNMinutes(tReloadEveryNMinutes);

    sourceCanConstrainNumericData = CONSTRAIN_NO;
    sourceCanConstrainStringData = CONSTRAIN_NO;
    sourceCanConstrainStringRegex = "";

    this.zarrStorePath = tZarrStorePath;
    this.zarrGroupName = tZarrGroupName == null ? "" : tZarrGroupName.trim();
    this.rowDimensionName = tRowDimensionName;
    this.chunkCacheSize = tChunkCacheSize;
    this.awsRegion = tAwsRegion;
    this.awsEndpoint = tAwsEndpoint;
    this.cacheFromUrl = tCacheFromUrl;

    if (!String2.isSomething(this.zarrStorePath)) {
      throw new IllegalArgumentException(errorInMethod + "zarrStorePath wasn't specified.");
    }

    addGlobalAttributes.set(language, "sourceUrl", convertToPublicSourceUrl(this.zarrStorePath));
    localSourceUrl = this.zarrStorePath;

    // 1. Initialize zarr-java Store reader for local, HTTP, or S3 URIs
    try {
      this.zarrStore =
          createZarrStore(this.zarrStorePath, this.cacheFromUrl, this.awsRegion, this.awsEndpoint);
    } catch (Exception e) {
      throw new RuntimeException(
          errorInMethod + "Failed to initialize Zarr store at path: " + this.zarrStorePath, e);
    }

    // 2. Open specified Zarr root or subgroup
    try {
      this.zarrGroup = openZarrGroup(this.zarrStore, this.zarrGroupName);
    } catch (Exception e) {
      throw new RuntimeException(
          errorInMethod
              + "Failed to open Zarr group '"
              + this.zarrGroupName
              + "' in store: "
              + this.zarrStorePath,
          e);
    }

    // 3. Extract global attributes into sourceGlobalAttributes and combinedGlobalAttributes
    sourceGlobalAttributes = new Attributes();
    try {
      dev.zarr.zarrjava.core.Attributes zattrs = this.zarrGroup.metadata().attributes();
      if (zattrs != null) {
        populateAttributesFromZarr(zattrs, sourceGlobalAttributes);
      }
    } catch (ZarrException ze) {
      String2.log(errorInMethod + "Warning: Could not read .zattrs metadata: " + ze.getMessage());
    }

    combinedGlobalAttributes = new LocalizedAttributes(addGlobalAttributes, sourceGlobalAttributes);
    String tLicense = combinedGlobalAttributes.getString(language, "license");
    if (tLicense != null) {
      combinedGlobalAttributes.set(
          language,
          "license",
          String2.replaceAll(tLicense, "[standard]", EDStatic.messages.standardLicense));
    }
    combinedGlobalAttributes.removeValue("\"null\"");

    // 4. Determine main row dimension name and array size
    try {
      determineRowDimensionAndSize();
    } catch (Exception e) {
      String2.log(errorInMethod + "Warning when determining row dimension: " + e.getMessage());
    }

    // 5. Discover metadata, parse DSG structure, and initialize dataVariables
    Map<String, ZarrArrayInfo> arrayMap = parseZarrMetadata();
    this.zarrArrayInfoMap = arrayMap;
    dataVariables = buildTableVariables(tDataVariables, arrayMap);
    detectAndSetCdmDataType(arrayMap);

    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);
    ensureValid();
  }

  /**
   * Helper method to instantiate appropriate zarr-java Store for local, HTTP, or S3 URIs.
   *
   * @param path store URI or path
   * @param awsRegion AWS region name if applicable
   * @param awsEndpoint custom AWS S3 endpoint if applicable
   * @return initialized Store
   * @throws IOException if error
   */
  public static Store createZarrStore(String path, String awsRegion, String awsEndpoint)
      throws IOException {
    return createZarrStore(path, null, awsRegion, awsEndpoint);
  }

  /**
   * Initializes a Zarr Store handle supporting local files, zip archives, HTTP, S3 URIs, and
   * optional cacheFromUrl fallback.
   *
   * @param path store URI or path
   * @param cacheFromUrl local caching directory or fallback URL
   * @param awsRegion AWS region name if applicable
   * @param awsEndpoint custom AWS S3 endpoint if applicable
   * @return initialized Store
   * @throws IOException if error
   */
  public static Store createZarrStore(
      String path, String cacheFromUrl, String awsRegion, String awsEndpoint) throws IOException {
    if (!String2.isSomething(path) && String2.isSomething(cacheFromUrl)) {
      path = cacheFromUrl;
    }
    if (path == null) throw new IllegalArgumentException("Zarr store path cannot be null.");

    if (String2.isRemote(path)
        && String2.isSomething(cacheFromUrl)
        && !String2.isRemote(cacheFromUrl)) {
      if (File2.isDirectory(cacheFromUrl) || File2.isFile(cacheFromUrl)) {
        path = cacheFromUrl;
      }
    }

    if (path.startsWith("http://") || path.startsWith("https://")) {
      return new HttpStore(path);
    } else if (path.startsWith("s3://") || path.startsWith("s3a://")) {
      String s3Path = path.substring(path.indexOf("://") + 3);
      int firstSlash = s3Path.indexOf('/');
      String bucket = firstSlash > 0 ? s3Path.substring(0, firstSlash) : s3Path;
      String keyPrefix = firstSlash > 0 ? s3Path.substring(firstSlash + 1) : "";

      S3ClientBuilder builder = S3Client.builder();
      if (String2.isSomething(awsRegion)) {
        builder.region(Region.of(awsRegion));
      }
      if (String2.isSomething(awsEndpoint)) {
        try {
          builder.endpointOverride(new java.net.URI(awsEndpoint));
        } catch (Exception e) {
          throw new IOException("Invalid awsEndpoint URI: " + awsEndpoint, e);
        }
      }
      S3Client s3Client = builder.build();
      return new S3Store(s3Client, bucket, keyPrefix);
    } else if (path.toLowerCase().endsWith(".zip")) {
      return new ReadOnlyZipStore(Paths.get(path));
    } else {
      return new FilesystemStore(Paths.get(path));
    }
  }

  /**
   * Opens the targeted Zarr root or sub-group.
   *
   * @param store initialized Zarr store
   * @param groupName sub-group path or empty string for root
   * @return opened Group handle
   * @throws Exception if group opening fails
   */
  public static Group openZarrGroup(Store store, String groupName) throws Exception {
    StoreHandle handle;
    if (groupName == null || groupName.isEmpty() || "/".equals(groupName.trim())) {
      handle = store.resolve();
      try {
        return Group.open(handle);
      } catch (Exception e) {
        if (store instanceof Store.ListableStore listable) {
          boolean rootHasZarrDescriptor =
              handle.resolve(".zgroup").exists()
                  || handle.resolve("zarr.json").exists()
                  || handle.resolve(".zarray").exists();
          if (!rootHasZarrDescriptor) {
            Set<String> topDirs = new HashSet<>();
            listable
                .listChildren()
                .forEach(
                    c -> {
                      String clean = c.startsWith("/") ? c.substring(1) : c;
                      int slash = clean.indexOf('/');
                      String top = slash > 0 ? clean.substring(0, slash) : clean;
                      if (!top.isEmpty()) topDirs.add(top);
                    });
            if (topDirs.size() == 1) {
              String singleChild = topDirs.iterator().next();
              StoreHandle childHandle = handle.resolve(singleChild);
              if (childHandle.resolve(".zgroup").exists()
                  || childHandle.resolve("zarr.json").exists()
                  || childHandle.resolve(".zarray").exists()) {
                return Group.open(childHandle);
              }
            }
          }
        }
        throw e;
      }
    } else {
      String cleanGroup = groupName.trim();
      if (cleanGroup.startsWith("/")) cleanGroup = cleanGroup.substring(1);
      if (cleanGroup.endsWith("/")) cleanGroup = cleanGroup.substring(0, cleanGroup.length() - 1);
      handle = store.resolve(cleanGroup);
      return Group.open(handle);
    }
  }

  /**
   * Populates ERDDAP Attributes object from Zarr metadata attributes map.
   *
   * @param zattrs Zarr attributes map
   * @param erddapAtts target ERDDAP Attributes object
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
   * Determines the main row dimension name and array size following the 3-step fallback priority.
   *
   * @throws Exception if error
   */
  private void determineRowDimensionAndSize() throws Exception {
    if (zarrGroup == null) return;

    Map<String, Long> dimToSizeMap = new LinkedHashMap<>();
    Map<String, Integer> dimFrequencyMap = new LinkedHashMap<>();
    Map<String, Long> array1DLengthMap = new LinkedHashMap<>();

    Map<String, ZarrArrayInfo> arrayMap = null;
    try {
      arrayMap = parseZarrMetadata(zarrGroup);
      for (ZarrArrayInfo info : arrayMap.values()) {
        if (info == null || info.isUnsupportedCodec) continue;
        if (info.dimensionNames != null && info.shape != null) {
          for (int d = 0; d < info.dimensionNames.length; d++) {
            String dimName = info.dimensionNames[d];
            long dimSize = d < info.shape.length ? info.shape[d] : 0;
            dimToSizeMap.putIfAbsent(dimName, dimSize);
            dimFrequencyMap.put(dimName, dimFrequencyMap.getOrDefault(dimName, 0) + 1);
          }
        }
        if (info.shape != null && info.shape.length == 1) {
          array1DLengthMap.put(info.name, info.shape[0]);
        }
      }
    } catch (Throwable t) {
      String2.log("Warning in determineRowDimensionAndSize: " + t.getMessage());
    }

    // Step 1: Explicit XML tag
    if (String2.isSomething(this.rowDimensionName)) {
      if (dimToSizeMap.containsKey(this.rowDimensionName)) {
        this.numRows = dimToSizeMap.get(this.rowDimensionName);
        return;
      } else if (arrayMap != null && arrayMap.containsKey(this.rowDimensionName)) {
        ZarrArrayInfo dimArrInfo = arrayMap.get(this.rowDimensionName);
        if (dimArrInfo != null && dimArrInfo.shape != null && dimArrInfo.shape.length > 0) {
          this.numRows = dimArrInfo.shape[0];
          return;
        }
      }
    }

    // Step 2: Standard Conventions ("obs", "row", "time", "index", "record", "i")
    String[] standardCandidates = new String[] {"obs", "row", "time", "index", "record", "i"};
    for (String cand : standardCandidates) {
      if (dimToSizeMap.containsKey(cand)) {
        this.rowDimensionName = cand;
        this.numRows = dimToSizeMap.get(cand);
        return;
      }
    }

    // Step 3: Auto-Discovery
    if (!dimToSizeMap.isEmpty()) {
      String dominantDim = null;
      int maxFreq = -1;
      for (Map.Entry<String, Integer> entry : dimFrequencyMap.entrySet()) {
        if (entry.getValue() > maxFreq) {
          maxFreq = entry.getValue();
          dominantDim = entry.getKey();
        }
      }
      if (dominantDim != null) {
        this.rowDimensionName = dominantDim;
        this.numRows = dimToSizeMap.get(dominantDim);
        return;
      }
    }

    if (!array1DLengthMap.isEmpty()) {
      Map.Entry<String, Long> first1D = array1DLengthMap.entrySet().iterator().next();
      this.rowDimensionName = "obs";
      this.numRows = first1D.getValue();
      return;
    }

    this.numRows = 0;
  }

  private static String getArrayName(Array zarray) {
    if (zarray != null
        && zarray.storeHandle != null
        && zarray.storeHandle.keys != null
        && zarray.storeHandle.keys.length > 0) {
      return zarray.storeHandle.keys[zarray.storeHandle.keys.length - 1];
    }
    return "";
  }

  private static String[] extractDimensionNames(ArrayMetadata metadata, int rank) {
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
    return null;
  }

  /**
   * Data structure holding metadata and handles for a Zarr array in an EDDTableFromZarr dataset.
   */
  protected static class ZarrArrayInfo {
    public String name;
    public Array array;
    public long[] shape;
    public int[] chunkShape;
    public PAType paType;
    public Attributes attributes;
    public String[] dimensionNames;
    public boolean isUnsupportedCodec = false;

    public boolean is1D() {
      return shape != null && shape.length == 1;
    }

    public boolean isScalar() {
      return shape == null || shape.length == 0;
    }

    public boolean isScalar(long totalNumRows) {
      if (shape == null || shape.length == 0) return true;
      if (shape.length == 1 && shape[0] == 1 && totalNumRows > 1) return true;
      return false;
    }

    public boolean is2DStringOrChar() {
      if (shape == null || shape.length != 2) return false;

      // 1. Explicit string/char data types
      if (paType == PAType.STRING || paType == PAType.CHAR) return true;

      // 2. For 2D byte arrays, check if one of the dimensions explicitly indicates string length
      if (paType == PAType.BYTE || paType == PAType.UBYTE) {
        if (dimensionNames != null) {
          for (String d : dimensionNames) {
            if (String2.isSomething(d)) {
              String dLower = d.toLowerCase();
              if (dLower.contains("str")
                  || dLower.contains("nchar")
                  || dLower.contains("char")
                  || dLower.equals("string")
                  || dLower.equals("string_len")
                  || dLower.equals("string_length")) {
                return true;
              }
            }
          }
        }
      }

      return false;
    }
  }

  /**
   * Helper method to open/load a ZarrArrayInfo from a Zarr group.
   *
   * @param zarrGroup target Zarr group
   * @param name array name or relative path
   * @param arrayMap map to populate/cache
   * @return populated ZarrArrayInfo or null if error
   */
  protected static ZarrArrayInfo getOrOpenZarrArrayInfo(
      Group zarrGroup, String name, Map<String, ZarrArrayInfo> arrayMap) {
    if (arrayMap.containsKey(name)) return arrayMap.get(name);
    try {
      StoreHandle childHandle = zarrGroup.storeHandle.resolve(name);
      Array zarray = Array.open(childHandle);
      if (zarray != null && zarray.metadata() != null) {
        ZarrArrayInfo info = createZarrArrayInfo(name, zarray);
        if (info != null) {
          arrayMap.put(name, info);
          return info;
        }
      }
    } catch (Throwable t) {
      if (verbose)
        String2.log("Warning in getOrOpenZarrArrayInfo for '" + name + "': " + t.getMessage());
    }
    return null;
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
    try {
      dev.zarr.zarrjava.core.Attributes zattrs = metadata.attributes();
      if (zattrs != null) {
        populateAttributesFromZarr(zattrs, erddapAtts);
      }
    } catch (Throwable t) {
      // array has no .zattrs
    }

    if (zType != null) {
      String zTypeName = zType.toString().toLowerCase();
      if (zTypeName.startsWith("u") || zTypeName.startsWith("uint")) {
        erddapAtts.set("_Unsigned", "true");
      }
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

  private void detectAndSetCdmDataType(Map<String, ZarrArrayInfo> arrayMap) {
    int language = 0;
    String cdmDataType = combinedGlobalAttributes.getString(language, "cdm_data_type");
    String featureType = combinedGlobalAttributes.getString(language, "featureType");
    if (!String2.isSomething(featureType)) {
      featureType = combinedGlobalAttributes.getString(language, "CF:featureType");
    }
    if (!String2.isSomething(featureType)) {
      featureType = combinedGlobalAttributes.getString(language, "CF:feature_type");
    }

    if (!String2.isSomething(featureType) && dataVariables != null) {
      for (EDV edv : dataVariables) {
        if (edv != null) {
          String cfRole = edv.combinedAttributes().getString(language, "cf_role");
          if (String2.isSomething(cfRole)) {
            if ("timeseries_id".equalsIgnoreCase(cfRole)) {
              featureType = "TimeSeries";
              break;
            } else if ("trajectory_id".equalsIgnoreCase(cfRole)) {
              featureType = "Trajectory";
              break;
            } else if ("profile_id".equalsIgnoreCase(cfRole)) {
              featureType = "Profile";
              break;
            }
          }
        }
      }
    }

    if (!String2.isSomething(featureType) && arrayMap != null) {
      for (ZarrArrayInfo info : arrayMap.values()) {
        if (info != null && info.attributes != null) {
          String vFt = info.attributes.getString("cf_role");
          if ("timeseries_id".equalsIgnoreCase(vFt)) {
            featureType = "TimeSeries";
            break;
          } else if ("trajectory_id".equalsIgnoreCase(vFt)) {
            featureType = "Trajectory";
            break;
          } else if ("profile_id".equalsIgnoreCase(vFt)) {
            featureType = "Profile";
            break;
          }
        }
      }
    }

    if (String2.isSomething(featureType)) {
      String ftLower = featureType.toLowerCase();
      String cdmType = "Point";
      if (ftLower.contains("timeseriesprofile")) {
        cdmType = "TimeSeriesProfile";
      } else if (ftLower.contains("timeseries")) {
        cdmType = "TimeSeries";
      } else if (ftLower.contains("trajectory")) {
        cdmType = "Trajectory";
      } else if (ftLower.contains("profile")) {
        cdmType = "Profile";
      } else if (ftLower.contains("point")) {
        cdmType = "Point";
      } else if (ftLower.contains("other")) {
        cdmType = "Other";
      }
      combinedGlobalAttributes.set(language, "cdm_data_type", cdmType);
      combinedGlobalAttributes.set(language, "featureType", cdmType);

      // Auto-set cdm_<type>_variables if not already set and a cf_role variable exists
      String roleVar = null;
      if (dataVariables != null) {
        for (EDV edv : dataVariables) {
          if (edv != null) {
            String cfRole = edv.combinedAttributes().getString(language, "cf_role");
            if (String2.isSomething(cfRole)) {
              roleVar = edv.destinationName();
              break;
            }
          }
        }
      }
      if (roleVar != null) {
        if ("TimeSeries".equalsIgnoreCase(cdmType)
            && combinedGlobalAttributes.getString(language, "cdm_timeseries_variables") == null) {
          combinedGlobalAttributes.set(language, "cdm_timeseries_variables", roleVar);
        } else if ("Trajectory".equalsIgnoreCase(cdmType)
            && combinedGlobalAttributes.getString(language, "cdm_trajectory_variables") == null) {
          combinedGlobalAttributes.set(language, "cdm_trajectory_variables", roleVar);
        } else if ("Profile".equalsIgnoreCase(cdmType)
            && combinedGlobalAttributes.getString(language, "cdm_profile_variables") == null) {
          combinedGlobalAttributes.set(language, "cdm_profile_variables", roleVar);
        }
      }
    } else if (String2.isSomething(cdmDataType)) {
      combinedGlobalAttributes.set(language, "cdm_data_type", cdmDataType);
    } else {
      if (lonIndex >= 0 && latIndex >= 0) {
        combinedGlobalAttributes.set(language, "cdm_data_type", "Point");
      } else {
        combinedGlobalAttributes.set(language, "cdm_data_type", "Other");
      }
    }
  }

  /**
   * Builds the EDV table variables based on XML configuration (if present) or auto-discovery.
   *
   * @param tDataVariables XML variable definitions or null
   * @param arrayMap map of Zarr array metadata
   * @return array of initialized EDV variables
   * @throws Throwable if error
   */
  public EDV[] buildTableVariables(
      List<DataVariableInfo> tDataVariables, Map<String, ZarrArrayInfo> arrayMap) throws Throwable {
    int language = 0;
    List<EDV> edvList = new ArrayList<>();

    if (tDataVariables != null && !tDataVariables.isEmpty()) {
      int ndv = tDataVariables.size();
      for (int dv = 0; dv < ndv; dv++) {
        DataVariableInfo dvi = tDataVariables.get(dv);
        String tSourceName = dvi.sourceName();
        String tDestName = dvi.destinationName();
        if (!String2.isSomething(tDestName)) tDestName = tSourceName;
        LocalizedAttributes tAddAtt = dvi.attributes();
        if (tAddAtt == null) tAddAtt = new LocalizedAttributes();
        String tSourceType = dvi.dataType();
        Attributes tSourceAtt = new Attributes();

        ZarrArrayInfo info = arrayMap != null ? arrayMap.get(tSourceName) : null;
        if (info == null && zarrGroup != null && arrayMap != null) {
          info = getOrOpenZarrArrayInfo(zarrGroup, tSourceName, arrayMap);
        }

        if (info != null && info.attributes != null) {
          info.attributes.copyTo(tSourceAtt);
        }

        if (!String2.isSomething(tSourceType) && info != null && info.paType != null) {
          tSourceType = PAType.toCohortString(info.paType);
        }
        if (!String2.isSomething(tSourceType)) tSourceType = "double";

        if (tSourceAtt.getString("ioos_category") == null
            && tAddAtt.getString(language, "ioos_category") == null) {
          tAddAtt.set(language, "ioos_category", "Unknown");
        }

        EDV edv = createEdvInstance(dv, tSourceName, tDestName, tSourceAtt, tAddAtt, tSourceType);
        edvList.add(edv);
      }
    } else if (arrayMap != null && !arrayMap.isEmpty()) {
      int dv = 0;
      for (ZarrArrayInfo info : arrayMap.values()) {
        if (info == null || info.isUnsupportedCodec) continue;

        String name = info.name;
        if (!String2.isSomething(name)) continue;

        // Skip hidden arrays or metadata descriptor keys
        if (name.startsWith(".") || name.startsWith("_")) continue;

        // Skip auxiliary bounds, QC, flag, and ancillary count arrays
        String lowerName = name.toLowerCase();
        if (lowerName.endsWith("_bnds")
            || lowerName.endsWith("_bounds")
            || lowerName.endsWith("_qc")
            || lowerName.endsWith("_flags")
            || lowerName.endsWith("_flag")
            || lowerName.endsWith("_status")
            || lowerName.endsWith("_count")) {
          if (verbose)
            String2.log("EDDTableFromZarr auto-discovery skipping auxiliary array: " + name);
          continue;
        }

        Attributes tSourceAtt = new Attributes();
        if (info.attributes != null) {
          info.attributes.copyTo(tSourceAtt);
        }
        String stdName = tSourceAtt.getString("standard_name");

        boolean isDsgRoleOrAxis =
            "latitude".equalsIgnoreCase(name)
                || "lat".equalsIgnoreCase(name)
                || "latitude".equalsIgnoreCase(stdName)
                || "longitude".equalsIgnoreCase(name)
                || "lon".equalsIgnoreCase(name)
                || "longitude".equalsIgnoreCase(stdName)
                || "altitude".equalsIgnoreCase(name)
                || "alt".equalsIgnoreCase(name)
                || "altitude".equalsIgnoreCase(stdName)
                || "depth".equalsIgnoreCase(name)
                || "depth".equalsIgnoreCase(stdName)
                || "time".equalsIgnoreCase(name)
                || "time".equalsIgnoreCase(stdName)
                || "station_id".equalsIgnoreCase(name)
                || "station".equalsIgnoreCase(name)
                || "station_id".equalsIgnoreCase(stdName)
                || "trajectory_id".equalsIgnoreCase(name)
                || "trajectory".equalsIgnoreCase(name)
                || "trajectory_id".equalsIgnoreCase(stdName)
                || "profile_id".equalsIgnoreCase(name)
                || "profile".equalsIgnoreCase(name)
                || "profile_id".equalsIgnoreCase(stdName)
                || tSourceAtt.getString("cf_role") != null;

        boolean isEligible = false;
        if (info.is1D()) {
          if (numRows <= 0 || info.shape[0] == numRows || isDsgRoleOrAxis) {
            isEligible = true;
          } else if (info.dimensionNames != null
              && info.dimensionNames.length > 0
              && String2.isSomething(rowDimensionName)
              && rowDimensionName.equals(info.dimensionNames[0])) {
            isEligible = true;
          } else {
            if (verbose) {
              String2.log(
                  "EDDTableFromZarr auto-discovery skipping 1D array '"
                      + name
                      + "' with length "
                      + info.shape[0]
                      + " (does not match dataset numRows="
                      + numRows
                      + ")");
            }
          }
        } else if (info.isScalar(numRows)) {
          isEligible = true;
        } else if (info.is2DStringOrChar()) {
          if (info.shape != null && info.shape.length == 2) {
            if (numRows <= 0
                || info.shape[0] == numRows
                || info.shape[1] == numRows
                || isDsgRoleOrAxis) {
              isEligible = true;
            }
          }
        }

        if (!isEligible) continue;

        String tSourceName = info.name;
        String tDestName = String2.replaceAll(tSourceName, "/", "_");

        PAType paType = info.paType;
        if (info.is2DStringOrChar()) {
          paType = PAType.STRING;
        }

        String tSourceType = paType != null ? PAType.toCohortString(paType) : "double";

        Attributes addAtts =
            makeReadyToUseAddVariableAttributesForDatasetsXml(
                sourceGlobalAttributes,
                tSourceAtt,
                null,
                tSourceName,
                paType != PAType.STRING,
                paType != PAType.STRING,
                false);

        LocalizedAttributes tAddAtt = new LocalizedAttributes(addAtts);

        // Standard DSG cf_role inferrence during auto-discovery
        if ("station_id".equalsIgnoreCase(tSourceName)
            || "station_id".equalsIgnoreCase(stdName)
            || "station".equalsIgnoreCase(tSourceName)) {
          tAddAtt.set(language, "cf_role", "timeseries_id");
          tSourceAtt.set("cf_role", "timeseries_id");
          if (info.attributes != null) info.attributes.set("cf_role", "timeseries_id");
        } else if ("trajectory_id".equalsIgnoreCase(tSourceName)
            || "trajectory_id".equalsIgnoreCase(stdName)
            || "trajectory".equalsIgnoreCase(tSourceName)) {
          tAddAtt.set(language, "cf_role", "trajectory_id");
          tSourceAtt.set("cf_role", "trajectory_id");
          if (info.attributes != null) info.attributes.set("cf_role", "trajectory_id");
        } else if ("profile_id".equalsIgnoreCase(tSourceName)
            || "profile_id".equalsIgnoreCase(stdName)
            || "profile".equalsIgnoreCase(tSourceName)) {
          tAddAtt.set(language, "cf_role", "profile_id");
          tSourceAtt.set("cf_role", "profile_id");
          if (info.attributes != null) info.attributes.set("cf_role", "profile_id");
        }

        EDV edv = createEdvInstance(dv, tSourceName, tDestName, tSourceAtt, tAddAtt, tSourceType);
        edvList.add(edv);
        dv++;
      }
    }

    return edvList.toArray(new EDV[0]);
  }

  private EDV createEdvInstance(
      int dv,
      String tSourceName,
      String tDestName,
      Attributes tSourceAtt,
      LocalizedAttributes tAddAtt,
      String tSourceType)
      throws Throwable {
    int language = 0;
    EDV edv;
    String stdName = tSourceAtt.getString("standard_name");
    if (stdName == null) stdName = tAddAtt.getString(language, "standard_name");

    if (EDV.LON_NAME.equals(tDestName)
        || "longitude".equalsIgnoreCase(stdName)
        || "lon".equalsIgnoreCase(tSourceName)) {
      if (tSourceAtt.getString("units") == null && tAddAtt.getString(language, "units") == null) {
        tAddAtt.set(language, "units", "degrees_east");
      }
      edv =
          new EDVLon(
              datasetID,
              tSourceName,
              tSourceAtt,
              tAddAtt,
              tSourceType,
              PAOne.fromDouble(Double.NaN),
              PAOne.fromDouble(Double.NaN));
      lonIndex = dv;
    } else if (EDV.LAT_NAME.equals(tDestName)
        || "latitude".equalsIgnoreCase(stdName)
        || "lat".equalsIgnoreCase(tSourceName)) {
      if (tSourceAtt.getString("units") == null && tAddAtt.getString(language, "units") == null) {
        tAddAtt.set(language, "units", "degrees_north");
      }
      edv =
          new EDVLat(
              datasetID,
              tSourceName,
              tSourceAtt,
              tAddAtt,
              tSourceType,
              PAOne.fromDouble(Double.NaN),
              PAOne.fromDouble(Double.NaN));
      latIndex = dv;
    } else if (EDV.ALT_NAME.equals(tDestName)
        || "altitude".equalsIgnoreCase(stdName)
        || "alt".equalsIgnoreCase(tSourceName)) {
      if (tSourceAtt.getString("units") == null && tAddAtt.getString(language, "units") == null) {
        tAddAtt.set(language, "units", "m");
      }
      edv =
          new EDVAlt(
              datasetID,
              tSourceName,
              tSourceAtt,
              tAddAtt,
              tSourceType,
              PAOne.fromDouble(Double.NaN),
              PAOne.fromDouble(Double.NaN));
      altIndex = dv;
    } else if (EDV.DEPTH_NAME.equals(tDestName)
        || "depth".equalsIgnoreCase(stdName)
        || "depth".equalsIgnoreCase(tSourceName)) {
      if (tSourceAtt.getString("units") == null && tAddAtt.getString(language, "units") == null) {
        tAddAtt.set(language, "units", "m");
      }
      edv =
          new EDVDepth(
              datasetID,
              tSourceName,
              tSourceAtt,
              tAddAtt,
              tSourceType,
              PAOne.fromDouble(Double.NaN),
              PAOne.fromDouble(Double.NaN));
      depthIndex = dv;
    } else if (EDV.TIME_NAME.equals(tDestName)
        || "time".equalsIgnoreCase(stdName)
        || "time".equalsIgnoreCase(tSourceName)) {
      edv = new EDVTime(datasetID, tSourceName, tSourceAtt, tAddAtt, tSourceType);
      timeIndex = dv;
    } else if (EDVTimeStamp.hasTimeUnits(language, tSourceAtt, tAddAtt)) {
      edv = new EDVTimeStamp(datasetID, tSourceName, tDestName, tSourceAtt, tAddAtt, tSourceType);
    } else {
      edv = new EDV(datasetID, tSourceName, tDestName, tSourceAtt, tAddAtt, tSourceType);
    }

    return edv;
  }

  /**
   * Discovers variables, 1D array column mappings, and CF Discrete Sampling Geometry (DSG)
   * structure from Zarr metadata.
   *
   * @return Map of array names to ZarrArrayInfo objects
   * @throws Throwable if error
   */
  public Map<String, ZarrArrayInfo> parseZarrMetadata() throws Throwable {
    return parseZarrMetadata(this.zarrGroup);
  }

  /**
   * Parses Zarr metadata from a given Zarr group.
   *
   * @param zarrGroup target Zarr group
   * @return Map of array name to ZarrArrayInfo
   * @throws Throwable if error
   */
  /** Helper to recursively traverse group nodes in non-listable or fallback Zarr stores. */
  protected static void traverseGroupNodes(
      Group group, String prefix, Map<String, ZarrArrayInfo> arrayMap) {
    if (group == null) return;
    try {
      Node[] nodes = group.listAsArray();
      if (nodes != null) {
        for (Node node : nodes) {
          if (node instanceof Array zarray) {
            String name = getArrayName(zarray);
            if (String2.isSomething(name)) {
              String fullName = prefix.isEmpty() ? name : prefix + "/" + name;
              getOrOpenZarrArrayInfo(group, fullName, arrayMap);
            }
          } else if (node instanceof Group childGroup) {
            String groupName =
                (childGroup.storeHandle != null
                        && childGroup.storeHandle.keys != null
                        && childGroup.storeHandle.keys.length > 0)
                    ? childGroup.storeHandle.keys[childGroup.storeHandle.keys.length - 1]
                    : "";
            if (String2.isSomething(groupName)) {
              String fullGroupPrefix = prefix.isEmpty() ? groupName : prefix + "/" + groupName;
              traverseGroupNodes(childGroup, fullGroupPrefix, arrayMap);
            }
          }
        }
      }
    } catch (Throwable t) {
      if (verbose) String2.log("Warning in traverseGroupNodes: " + t.getMessage());
    }
  }

  /**
   * Parses Zarr metadata recursively from a given Zarr group.
   *
   * @param zarrGroup target Zarr group
   * @return Map of array name to ZarrArrayInfo
   * @throws Throwable if error
   */
  public static Map<String, ZarrArrayInfo> parseZarrMetadata(Group zarrGroup) throws Throwable {
    Map<String, ZarrArrayInfo> arrayMap = new LinkedHashMap<>();
    if (zarrGroup == null || zarrGroup.storeHandle == null) return arrayMap;

    Store store = zarrGroup.storeHandle.store;
    String[] groupKeys = zarrGroup.storeHandle.keys;

    if (store instanceof Store.ListableStore listable) {
      try {
        List<String[]> entries = listable.list(groupKeys).toList();
        for (String[] entryKeys : entries) {
          if (entryKeys.length >= 2) {
            String lastPart = entryKeys[entryKeys.length - 1];
            if (".zarray".equals(lastPart) || "zarr.json".equals(lastPart)) {
              String[] relParts = new String[entryKeys.length - 1];
              System.arraycopy(entryKeys, 0, relParts, 0, entryKeys.length - 1);
              String relName = String.join("/", relParts);
              if (groupKeys != null && groupKeys.length > 0) {
                String groupPrefix = String.join("/", groupKeys) + "/";
                if (relName.startsWith(groupPrefix)) {
                  relName = relName.substring(groupPrefix.length());
                }
              }
              getOrOpenZarrArrayInfo(zarrGroup, relName, arrayMap);
            }
          }
        }
      } catch (Throwable t) {
        traverseGroupNodes(zarrGroup, "", arrayMap);
      }
    } else {
      traverseGroupNodes(zarrGroup, "", arrayMap);
    }
    return arrayMap;
  }

  /**
   * Core tabular query & constraint evaluator for EDDTableFromZarr.
   *
   * @param language the language code (0=English)
   * @param loggedInAs user authorization info
   * @param requestUrl requested URL
   * @param userDapQuery user DAP query
   * @param tableWriter destination TableWriter
   * @throws Throwable if error
   */
  @Override
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {

    // 1. Parse user DAP query into requested results variables and constraints
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();

    parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues,
        false);

    String[] requestedVarNames = resultsVariables.toArray();
    if (requestedVarNames == null || requestedVarNames.length == 0) {
      if (String2.isSomething(defaultDataQuery)) {
        StringArray defaultResultsVars = new StringArray();
        parseUserDapQuery(
            language,
            defaultDataQuery,
            defaultResultsVars,
            new StringArray(),
            new StringArray(),
            new StringArray(),
            false);
        requestedVarNames = defaultResultsVars.toArray();
      }
      if (requestedVarNames == null || requestedVarNames.length == 0) {
        requestedVarNames = dataVariableDestinationNames();
      }
    }

    StringArray[] constraintVarsAndOps =
        new StringArray[] {constraintVariables, constraintOps, constraintValues};

    // Hold read lock across entire query execution to ensure oldStore is not closed mid-read
    zarrLock.readLock().lock();
    try {
      Group activeGroup = this.zarrGroup;
      long activeNumRows = this.numRows;
      Map<String, ZarrArrayInfo> metadataMap =
          this.zarrArrayInfoMap != null ? this.zarrArrayInfoMap : parseZarrMetadata(activeGroup);

      // Map source array names to Array handles
      Map<String, Array> zarrArrayMap = new LinkedHashMap<>();
      for (EDV edv : dataVariables) {
        if (edv != null) {
          String sName = edv.sourceName();
          ZarrArrayInfo info = metadataMap.get(sName);
          if (info == null && activeGroup != null) {
            info = getOrOpenZarrArrayInfo(activeGroup, sName, metadataMap);
          }
          if (info != null && info.array != null) {
            zarrArrayMap.put(sName, info.array);
          }
        }
      }

      // 2. Determine chunk batch size along main row dimension
      int chunkSize = 10000; // default buffer size
      if (metadataMap != null && !metadataMap.isEmpty()) {
        for (ZarrArrayInfo info : metadataMap.values()) {
          if (info != null && info.chunkShape != null && info.chunkShape.length > 0) {
            int c0 = info.chunkShape[0];
            if (c0 > 0) {
              chunkSize = c0;
              break;
            }
          }
        }
      }
      if (chunkSize < 100) chunkSize = 100;
      if (chunkSize > 100000) chunkSize = 100000;

      long totalMatchingRows = 0;
      Table cumulativeBatchTable = null;

      // 3. Main chunk scanning loop over dataset rows
      for (long startRow = 0; startRow < activeNumRows; startRow += chunkSize) {
        int currentChunkSize = (int) Math.min(chunkSize, activeNumRows - startRow);

        // Pass 1: Evaluate constraints against chunk data
        BitSet rowMask =
            evaluateChunkConstraints(
                startRow, currentChunkSize, constraintVarsAndOps, zarrArrayMap, metadataMap, activeGroup);

        int cardinal = rowMask.cardinality();
        if (cardinal == 0) {
          continue;
        }

        totalMatchingRows += cardinal;

        // Pass 2: Extract requested variable row batch
        Table batchTable =
            extractRowBatch(
                startRow, currentChunkSize, rowMask, requestedVarNames, zarrArrayMap, metadataMap, activeGroup);

        if (cumulativeBatchTable == null) {
          cumulativeBatchTable = batchTable;
        } else {
          cumulativeBatchTable.append(batchTable);
        }

        // Stream sub-table batch into tableWriter if buffer size threshold met.
        // Note: writeChunkToTableWriter calls standardizeResultsTable, which converts
        // source column names to destination names and re-orders columns to match the query.
        if (writeChunkToTableWriter(
            language, requestUrl, userDapQuery, cumulativeBatchTable, tableWriter, false)) {
          cumulativeBatchTable = null;
        }
      }

      // 4. Handle empty results or finalize tableWriter
      if (totalMatchingRows == 0) {
        Table emptyTable = new Table();
        for (String varName : requestedVarNames) {
          EDV edv = findEDV(varName);
          PAType paType = edv != null ? edv.sourceDataPAType() : PAType.DOUBLE;
          emptyTable.addColumn(
              edv != null ? edv.sourceName() : varName, PrimitiveArray.factory(paType, 0, false));
        }
        writeChunkToTableWriter(language, requestUrl, userDapQuery, emptyTable, tableWriter, true);
      } else if (cumulativeBatchTable != null) {
        writeChunkToTableWriter(
            language, requestUrl, userDapQuery, cumulativeBatchTable, tableWriter, true);
      } else {
        tableWriter.finish();
      }
    } finally {
      zarrLock.readLock().unlock();
    }
  }

  /** Safe helper method to find EDV by destination name or source name. */
  public EDV findEDV(String varName) {
    if (varName == null) return null;
    int dv = String2.indexOf(dataVariableDestinationNames(), varName);
    if (dv >= 0) return dataVariables[dv];
    dv = String2.indexOf(dataVariableSourceNames(), varName);
    if (dv >= 0) return dataVariables[dv];
    return null;
  }

  /**
   * Helper method for tabular query & constraint evaluation.
   *
   * @param userDapQuery user DAP query
   * @param loggedInAs user identity
   * @param tableWriter table writer output sink
   * @throws Throwable if error
   */
  public void getDataForQuery(String userDapQuery, String loggedInAs, TableWriter tableWriter)
      throws Throwable {
    getDataForDapQuery(0, loggedInAs, "", userDapQuery, tableWriter);
  }

  /** Evaluates query constraints on a chunk batch using filter-first BitSet strategy. */
  protected BitSet evaluateChunkConstraints(
      long startRow,
      int currentChunkSize,
      StringArray[] constraintVarsAndOps,
      Map<String, Array> zarrArrayMap)
      throws Throwable {
    return evaluateChunkConstraints(
        startRow, currentChunkSize, constraintVarsAndOps, zarrArrayMap, parseZarrMetadata());
  }

  protected BitSet evaluateChunkConstraints(
      long startRow,
      int currentChunkSize,
      StringArray[] constraintVarsAndOps,
      Map<String, Array> zarrArrayMap,
      Map<String, ZarrArrayInfo> metadataMap)
      throws Throwable {
    return evaluateChunkConstraints(
        startRow, currentChunkSize, constraintVarsAndOps, zarrArrayMap, metadataMap, this.zarrGroup);
  }

  protected BitSet evaluateChunkConstraints(
      long startRow,
      int currentChunkSize,
      StringArray[] constraintVarsAndOps,
      Map<String, Array> zarrArrayMap,
      Map<String, ZarrArrayInfo> metadataMap,
      Group activeGroup)
      throws Throwable {

    BitSet rowMask = new BitSet(currentChunkSize);
    rowMask.set(0, currentChunkSize);

    if (constraintVarsAndOps == null || constraintVarsAndOps.length < 3) {
      return rowMask;
    }

    StringArray constraintVars = constraintVarsAndOps[0];
    StringArray constraintOps = constraintVarsAndOps[1];
    StringArray constraintValues = constraintVarsAndOps[2];

    if (constraintVars == null || constraintVars.size() == 0) {
      return rowMask;
    }

    if (metadataMap == null) {
      metadataMap = parseZarrMetadata(activeGroup);
    }

    int nConstraints = constraintVars.size();
    for (int c = 0; c < nConstraints; c++) {
      String varName = constraintVars.get(c);
      String op = constraintOps.get(c);
      String valStr = constraintValues.get(c);

      EDV edv = findEDV(varName);
      if (edv == null) {
        if (verbose)
          String2.log("evaluateChunkConstraints could NOT find EDV for varName=" + varName);
        continue;
      }

      String sourceName = edv.sourceName();
      ZarrArrayInfo info = metadataMap.get(sourceName);
      if (info == null && activeGroup != null) {
        info = getOrOpenZarrArrayInfo(activeGroup, sourceName, metadataMap);
      }
      Array zarray = zarrArrayMap.get(sourceName);
      if (zarray == null && info != null) {
        zarray = info.array;
        if (zarray != null) zarrArrayMap.put(sourceName, zarray);
      }

      PrimitiveArray pa =
          readChunkDataForVar(sourceName, zarray, info, edv, startRow, currentChunkSize, true);

      pa.applyConstraint(
          edv.destinationDataPAType() == PAType.STRING || edv.sourceDataPAType() == PAType.STRING,
          rowMask,
          op,
          valStr);

      if (rowMask.cardinality() == 0) {
        break; // Early exit if no rows satisfy constraints in this chunk
      }
    }

    return rowMask;
  }

  /**
   * Extracts requested variable columns for rows matching rowMask in a chunk batch. Note: Columns
   * are added using edv.sourceName() so that downstream call to writeChunkToTableWriter ->
   * standardizeResultsTable converts source names to destination names and applies destination
   * variable attributes.
   */
  protected Table extractRowBatch(
      long startRow, BitSet rowMask, String[] requestedVarNames, Map<String, Array> zarrArrayMap)
      throws Throwable {
    int currentChunkSize =
        (numRows > 0 && numRows > startRow)
            ? (int) Math.min(10000, numRows - startRow)
            : rowMask.size();
    return extractRowBatch(
        startRow, currentChunkSize, rowMask, requestedVarNames, zarrArrayMap, parseZarrMetadata());
  }

  protected Table extractRowBatch(
      long startRow,
      int currentChunkSize,
      BitSet rowMask,
      String[] requestedVarNames,
      Map<String, Array> zarrArrayMap,
      Map<String, ZarrArrayInfo> metadataMap)
      throws Throwable {
    return extractRowBatch(
        startRow,
        currentChunkSize,
        rowMask,
        requestedVarNames,
        zarrArrayMap,
        metadataMap,
        this.zarrGroup);
  }

  protected Table extractRowBatch(
      long startRow,
      int currentChunkSize,
      BitSet rowMask,
      String[] requestedVarNames,
      Map<String, Array> zarrArrayMap,
      Map<String, ZarrArrayInfo> metadataMap,
      Group activeGroup)
      throws Throwable {

    Table batchTable = new Table();
    int nMatchingRows = rowMask.cardinality();

    if (metadataMap == null) {
      metadataMap = parseZarrMetadata(activeGroup);
    }

    for (String varName : requestedVarNames) {
      EDV edv = findEDV(varName);
      if (edv == null) continue;

      String sourceName = edv.sourceName();
      ZarrArrayInfo info = metadataMap.get(sourceName);
      if (info == null && activeGroup != null) {
        info = getOrOpenZarrArrayInfo(activeGroup, sourceName, metadataMap);
      }
      Array zarray = zarrArrayMap.get(sourceName);
      if (zarray == null && info != null) {
        zarray = info.array;
        if (zarray != null) zarrArrayMap.put(sourceName, zarray);
      }

      PrimitiveArray pa =
          readChunkDataForVar(sourceName, zarray, info, edv, startRow, currentChunkSize, false);

      PrimitiveArray filteredPa = PrimitiveArray.factory(pa.elementType(), nMatchingRows, false);
      for (int r = rowMask.nextSetBit(0); r >= 0; r = rowMask.nextSetBit(r + 1)) {
        filteredPa.addFromPA(pa, r);
      }

      // Add column with edv.sourceName() for writeChunkToTableWriter -> standardizeResultsTable
      batchTable.addColumn(sourceName, filteredPa);
    }

    return batchTable;
  }

  private static boolean isMissingChunkException(Throwable t) {
    if (t == null) return true;
    if (t instanceof java.io.FileNotFoundException
        || t instanceof java.nio.file.NoSuchFileException) {
      return true;
    }
    String msg = t.getMessage();
    if (msg != null) {
      String lower = msg.toLowerCase();
      if (lower.contains("not found")
          || lower.contains("404")
          || lower.contains("nosuchkey")
          || lower.contains("does not exist")
          || lower.contains("missing chunk")) {
        return true;
      }
    }
    return false;
  }

  private PrimitiveArray makeMissingPrimitiveArray(
      PAType paType, int currentChunkSize, ZarrArrayInfo info, EDV edv) {
    PrimitiveArray missingPa = PrimitiveArray.factory(paType, currentChunkSize, false);

    if (edv != null) {
      if (paType == PAType.STRING || paType == PAType.CHAR) {
        String sf = edv.stringFillValue();
        if (String2.isSomething(sf)) {
          missingPa.addNStrings(currentChunkSize, sf);
          return missingPa;
        }
      } else {
        double sf = edv.sourceFillValue();
        if (Double.isNaN(sf)) sf = edv.sourceMissingValue();
        if (!Double.isNaN(sf)) {
          missingPa.addNDoubles(currentChunkSize, sf);
          return missingPa;
        }
      }
    }

    if (info != null && info.attributes != null) {
      PrimitiveArray fillPa = info.attributes.get("_FillValue");
      if (fillPa == null) fillPa = info.attributes.get("missing_value");
      if (fillPa != null && fillPa.size() > 0) {
        PAOne fvOne = fillPa.getPAOne(0);
        if (fvOne != null && !fvOne.isMissingValue()) {
          missingPa.addNPAOnes(currentChunkSize, fvOne);
          return missingPa;
        }
      }
    }

    missingPa.addNPAOnes(currentChunkSize, missingPa.missingValue());
    return missingPa;
  }

  /**
   * Helper method to read chunk data for a single variable, applying scale/offset, missing values,
   * 2D string extraction, or scalar broadcasting.
   */
  private PrimitiveArray readChunkDataForVar(
      String sourceName,
      Array zarray,
      ZarrArrayInfo info,
      EDV edv,
      long startRow,
      int currentChunkSize,
      boolean unpackToDestination)
      throws Throwable {

    PAType paType = edv != null ? edv.sourceDataPAType() : PAType.DOUBLE;

    // Handle missing or unwritten Zarr array
    if (zarray == null || info == null) {
      PrimitiveArray missingPa = makeMissingPrimitiveArray(paType, currentChunkSize, info, edv);
      if (unpackToDestination && edv != null) {
        missingPa = edv.toDestination(missingPa);
      }
      return missingPa;
    }

    boolean isUnsigned = false;
    if (info.attributes != null) {
      isUnsigned = "true".equalsIgnoreCase(info.attributes.getString("_Unsigned"));
    }

    // 0D or DSG Global Scalar Variable Broadcasting
    if (info.isScalar(numRows)) {
      ucar.ma2.Array nc2Array = null;
      try {
        nc2Array = zarray.read();
      } catch (Throwable t) {
        if (!isMissingChunkException(t)) throw t;
        nc2Array = null;
      }

      PrimitiveArray scalarPa =
          nc2Array != null ? NcHelper.getPrimitiveArray(nc2Array, true, isUnsigned) : null;

      PrimitiveArray broadcastPa = PrimitiveArray.factory(paType, currentChunkSize, false);
      if (scalarPa != null && scalarPa.size() > 0) {
        PAOne scalarVal = scalarPa.getPAOne(0);
        broadcastPa.addNPAOnes(currentChunkSize, scalarVal);
      } else {
        broadcastPa = makeMissingPrimitiveArray(paType, currentChunkSize, info, edv);
      }
      if (unpackToDestination && edv != null) {
        broadcastPa = edv.toDestination(broadcastPa);
      }
      return broadcastPa;
    }

    // 2D Character or Byte Matrix String Variable
    if (info.is2DStringOrChar()) {
      boolean rowDimIsZero = true;
      if (info.shape != null && info.shape.length == 2) {
        if (numRows > 0 && info.shape[1] == numRows && info.shape[0] != numRows) {
          rowDimIsZero = false;
        }
      }

      int stringLength = rowDimIsZero ? (int) info.shape[1] : (int) info.shape[0];
      long[] offset = rowDimIsZero ? new long[] {startRow, 0} : new long[] {0, startRow};
      long[] shape =
          rowDimIsZero
              ? new long[] {currentChunkSize, stringLength}
              : new long[] {stringLength, currentChunkSize};

      ucar.ma2.Array nc2Array = null;
      try {
        nc2Array = zarray.read(offset, shape);
      } catch (Throwable t) {
        if (!isMissingChunkException(t)) throw t;
        nc2Array = null;
      }

      StringArray sa = new StringArray(currentChunkSize, false);
      if (nc2Array == null) {
        sa.addNStrings(currentChunkSize, "");
      } else if (nc2Array instanceof ucar.ma2.ArrayChar ac) {
        ucar.ma2.ArrayObject ao = ac.make1DStringArray();
        Object[] oa = (Object[]) ao.get1DJavaArray(ao.getDataType());
        for (Object o : oa) {
          sa.add(o == null ? "" : String2.trimEnd(o.toString()));
        }
      } else {
        // Fallback for 2D byte/numeric string matrices
        PrimitiveArray rawPa = NcHelper.getPrimitiveArray(nc2Array, false, isUnsigned);
        if (rowDimIsZero) {
          for (int row = 0; row < currentChunkSize; row++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < stringLength; c++) {
              int idx = row * stringLength + c;
              if (idx < rawPa.size()) {
                int b = rawPa.getInt(idx);
                if (b == 0) break; // Null terminator
                sb.append((char) (b & 0xff));
              }
            }
            sa.add(sb.toString().trim());
          }
        } else {
          for (int row = 0; row < currentChunkSize; row++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < stringLength; c++) {
              int idx = c * currentChunkSize + row;
              if (idx < rawPa.size()) {
                int b = rawPa.getInt(idx);
                if (b == 0) break; // Null terminator
                sb.append((char) (b & 0xff));
              }
            }
            sa.add(sb.toString().trim());
          }
        }
      }
      return sa;
    }

    // Standard 1D or multi-dimensional Variable
    int rank = 1;
    if (info != null && info.shape != null && info.shape.length > 0) {
      rank = info.shape.length;
    }

    int cSize =
        (info != null && info.chunkShape != null && info.chunkShape.length > 0)
            ? info.chunkShape[0]
            : currentChunkSize;
    if (cSize <= 0) cSize = currentChunkSize;

    long minChunk = startRow / cSize;
    long maxChunk = (startRow + currentChunkSize - 1) / cSize;

    PrimitiveArray pa = null;

    if (minChunk < maxChunk) {
      // Chunk-by-chunk reading across chunk boundaries
      for (long c = minChunk; c <= maxChunk; c++) {
        long cStartRow = Math.max(startRow, c * cSize);
        long cEndRow = Math.min(startRow + currentChunkSize, (c + 1) * cSize);
        int cLen = (int) (cEndRow - cStartRow);
        int cOffsetInBatch = (int) (cStartRow - startRow);

        long[] offset = new long[rank];
        long[] shape = new long[rank];
        offset[0] = cStartRow;
        shape[0] = cLen;
        for (int i = 1; i < rank; i++) {
          offset[i] = 0;
          shape[i] =
              (info != null && info.shape != null && i < info.shape.length) ? info.shape[i] : 1;
        }

        ucar.ma2.Array nc2Array = null;
        try {
          nc2Array = zarray.read(offset, shape);
        } catch (Throwable t) {
          if (!isMissingChunkException(t)) throw t;
          nc2Array = null;
        }

        PrimitiveArray chunkPa;
        if (nc2Array != null) {
          chunkPa = NcHelper.getPrimitiveArray(nc2Array, true, isUnsigned);
        } else {
          chunkPa = makeMissingPrimitiveArray(paType, cLen, info, edv);
        }

        if (pa == null) {
          pa = PrimitiveArray.factory(chunkPa.elementType(), currentChunkSize, false);
          pa.addNPAOnes(currentChunkSize, pa.missingValue());
        }

        for (int i = 0; i < cLen; i++) {
          pa.setFromPA(cOffsetInBatch + i, chunkPa, i);
        }
      }
    } else {
      // Single chunk slice read
      long[] offset = new long[rank];
      long[] shape = new long[rank];
      offset[0] = startRow;
      shape[0] = currentChunkSize;
      for (int i = 1; i < rank; i++) {
        offset[i] = 0;
        shape[i] =
            (info != null && info.shape != null && i < info.shape.length) ? info.shape[i] : 1;
      }

      ucar.ma2.Array nc2Array = null;
      try {
        nc2Array = zarray.read(offset, shape);
      } catch (Throwable t) {
        if (!isMissingChunkException(t)) throw t;
        nc2Array = null;
      }

      if (nc2Array == null) {
        pa = makeMissingPrimitiveArray(paType, currentChunkSize, info, edv);
      } else {
        pa = NcHelper.getPrimitiveArray(nc2Array, true, isUnsigned);
      }
    }

    if (unpackToDestination) {
      if (edv != null) {
        pa = edv.toDestination(pa);
      } else if (info != null && info.attributes != null) {
        String fillValStr = info.attributes.getString("_FillValue");
        if (!String2.isSomething(fillValStr)) {
          fillValStr = info.attributes.getString("missing_value");
        }
        if (String2.isSomething(fillValStr)) {
          pa.switchFromTo(fillValStr, "");
        }
        double scale = info.attributes.getDouble("scale_factor");
        double offsetVal = info.attributes.getDouble("add_offset");
        if ((!Double.isNaN(scale) && scale != 1.0)
            || (!Double.isNaN(offsetVal) && offsetVal != 0.0)) {
          if (Double.isNaN(scale)) scale = 1.0;
          if (Double.isNaN(offsetVal)) offsetVal = 0.0;
          pa.scaleAddOffset(scale, offsetVal);
        }
      }
    } else {
      String fillValStr = edv != null ? edv.combinedAttributes().getString(0, "_FillValue") : null;
      if (!String2.isSomething(fillValStr) && edv != null) {
        fillValStr = edv.combinedAttributes().getString(0, "missing_value");
      }
      if (!String2.isSomething(fillValStr) && info != null && info.attributes != null) {
        fillValStr = info.attributes.getString("_FillValue");
        if (!String2.isSomething(fillValStr)) {
          fillValStr = info.attributes.getString("missing_value");
        }
      }
      if (String2.isSomething(fillValStr)) {
        pa.switchFromTo(fillValStr, "");
      }
    }

    return pa;
  }

  /**
   * Incremental update method for real-time dataset growth in tabular Zarr stores.
   *
   * @param language user language
   * @param msg log prefix message
   * @param startUpdateMillis start timestamp of update
   * @return true if updated, false if no update or deferred
   * @throws Throwable if unhandled error
   */
  @Override
  public boolean lowUpdate(int language, String msg, long startUpdateMillis) throws Throwable {
    Store freshStore = null;
    try {
      // 1. Store Re-inspection & Shape Detection
      freshStore =
          createZarrStore(
              this.zarrStorePath, this.cacheFromUrl, this.awsRegion, this.awsEndpoint);
      Group freshGroup = openZarrGroup(freshStore, this.zarrGroupName);
      Map<String, ZarrArrayInfo> freshArrayMap = parseZarrMetadata(freshGroup);

      String targetRowDimName = this.rowDimensionName;
      long newNumRowsL = -1;

      Map<String, Long> dimToSizeMap = new LinkedHashMap<>();
      Map<String, Integer> dimFrequencyMap = new LinkedHashMap<>();
      Map<String, Long> array1DLengthMap = new LinkedHashMap<>();

      for (ZarrArrayInfo info : freshArrayMap.values()) {
        if (info == null || info.isUnsupportedCodec) continue;
        if (info.dimensionNames != null && info.shape != null) {
          for (int d = 0; d < info.dimensionNames.length; d++) {
            String dimName = info.dimensionNames[d];
            long dimSize = d < info.shape.length ? info.shape[d] : 0;
            dimToSizeMap.putIfAbsent(dimName, dimSize);
            dimFrequencyMap.put(dimName, dimFrequencyMap.getOrDefault(dimName, 0) + 1);
          }
        }
        if (info.shape != null && info.shape.length == 1) {
          array1DLengthMap.put(info.name, info.shape[0]);
        }
      }

      if (String2.isSomething(targetRowDimName)) {
        if (dimToSizeMap.containsKey(targetRowDimName)) {
          newNumRowsL = dimToSizeMap.get(targetRowDimName);
        } else if (freshArrayMap.containsKey(targetRowDimName)) {
          ZarrArrayInfo dimArrInfo = freshArrayMap.get(targetRowDimName);
          if (dimArrInfo != null && dimArrInfo.shape != null && dimArrInfo.shape.length > 0) {
            newNumRowsL = dimArrInfo.shape[0];
          }
        } else {
          if (reallyVerbose) {
            String2.log(
                msg
                    + "EDDTableFromZarr.lowUpdate: Configured row dimension '"
                    + targetRowDimName
                    + "' not found in fresh store. Deferring update.");
          }
          return false;
        }
      } else {
        String[] standardCandidates = new String[] {"obs", "row", "time", "index", "record", "i"};
        for (String cand : standardCandidates) {
          if (dimToSizeMap.containsKey(cand)) {
            targetRowDimName = cand;
            newNumRowsL = dimToSizeMap.get(cand);
            break;
          }
        }

        if (newNumRowsL < 0 && !dimToSizeMap.isEmpty()) {
          String dominantDim = null;
          int maxFreq = -1;
          for (Map.Entry<String, Integer> entry : dimFrequencyMap.entrySet()) {
            if (entry.getValue() > maxFreq) {
              maxFreq = entry.getValue();
              dominantDim = entry.getKey();
            }
          }
          if (dominantDim != null) {
            targetRowDimName = dominantDim;
            newNumRowsL = dimToSizeMap.get(dominantDim);
          }
        }

        if (newNumRowsL < 0 && !array1DLengthMap.isEmpty()) {
          Map.Entry<String, Long> first1D = array1DLengthMap.entrySet().iterator().next();
          targetRowDimName = "obs";
          newNumRowsL = first1D.getValue();
        }
      }

      if (newNumRowsL <= 0) {
        if (reallyVerbose) {
          String2.log(
              msg
                  + "EDDTableFromZarr.lowUpdate: Could not determine valid row count from fresh store.");
        }
        return false;
      }

      if (newNumRowsL <= this.numRows) {
        if (reallyVerbose) {
          String2.log(
              msg
                  + "EDDTableFromZarr.lowUpdate: No change to row count (newNumRows="
                  + newNumRowsL
                  + " <= currentNumRows="
                  + this.numRows
                  + ").");
        }
        return false;
      }

      long newNumRows = newNumRowsL;

      // 2. Column Synchronization Check
      if (dataVariables != null) {
        for (EDV edv : dataVariables) {
          if (edv == null) continue;
          String sName = edv.sourceName();
          ZarrArrayInfo info = getOrOpenZarrArrayInfo(freshGroup, sName, freshArrayMap);
          if (info == null || info.isUnsupportedCodec) continue;

          if (info.is1D()) {
            if (info.shape != null && info.shape.length > 0) {
              long colLen = info.shape[0];
              if (colLen < newNumRows) {
                String2.log(
                    msg
                        + " Column ["
                        + sName
                        + "] row count ("
                        + colLen
                        + ") lags behind primary dimension ("
                        + newNumRows
                        + "). Deferring update.");
                return false;
              }
            }
          } else if (info.is2DStringOrChar()) {
            if (info.shape != null && info.shape.length == 2) {
              long colLen = info.shape[0];
              if (this.numRows > 0 && info.shape[1] == this.numRows && info.shape[0] != this.numRows) {
                colLen = info.shape[1];
              }
              if (colLen < newNumRows) {
                String2.log(
                    msg
                        + " Column ["
                        + sName
                        + "] row count ("
                        + colLen
                        + ") lags behind primary dimension ("
                        + newNumRows
                        + "). Deferring update.");
                return false;
              }
            }
          }
        }
      }

      // 3. State & Attribute Synchronization
      String newTimeEndIso = null;
      double newTimeEndSec = Double.NaN;
      double timeStartSec = Double.NaN;

      if (timeIndex >= 0 && timeIndex < dataVariables.length) {
        EDV timeEdv = dataVariables[timeIndex];
        if (timeEdv != null) {
          String sName = timeEdv.sourceName();
          ZarrArrayInfo timeInfo = freshArrayMap.get(sName);
          if (timeInfo == null) {
            timeInfo = getOrOpenZarrArrayInfo(freshGroup, sName, freshArrayMap);
          }
          if (timeInfo != null && timeInfo.array != null) {
            PrimitiveArray timePa =
                readChunkDataForVar(
                    sName, timeInfo.array, timeInfo, timeEdv, newNumRows - 1, 1, true);
            if (timePa != null && timePa.size() > 0) {
              newTimeEndSec = timePa.getDouble(0);
              if (!Double.isNaN(newTimeEndSec)) {
                newTimeEndIso = Calendar2.epochSecondsToIsoStringTZ(newTimeEndSec);
              }
            }
          }
        }
      }

      Store oldStore = null;
      zarrLock.writeLock().lock();
      try {
        oldStore = this.zarrStore;
        this.zarrStore = freshStore;
        this.zarrGroup = freshGroup;
        this.zarrArrayInfoMap = freshArrayMap;
        this.numRows = newNumRows;
        if (String2.isSomething(targetRowDimName)) {
          this.rowDimensionName = targetRowDimName;
        }

        if (newTimeEndIso != null) {
          combinedGlobalAttributes.set(language, "time_coverage_end", newTimeEndIso);
          String timeStartStr =
              combinedGlobalAttributes.getString(language, "time_coverage_start");
          if (timeStartStr != null) {
            timeStartSec = Calendar2.isoStringToEpochSeconds(timeStartStr);
            if (!Double.isNaN(timeStartSec)
                && !Double.isNaN(newTimeEndSec)
                && newTimeEndSec >= timeStartSec) {
              long durationIntSec = Math.round(newTimeEndSec - timeStartSec);
              combinedGlobalAttributes.set(
                  language, "time_coverage_duration", "PT" + durationIntSec + "S");
            }
          }
        }
      } finally {
        zarrLock.writeLock().unlock();
      }

      if (oldStore != null && oldStore != freshStore) {
        if (oldStore instanceof AutoCloseable ac) {
          try {
            ac.close();
          } catch (Throwable ignore) {
          }
        }
      }

      if (verbose) {
        String2.log(
            msg
                + "EDDTableFromZarr.lowUpdate: Successfully updated dataset '"
                + datasetID
                + "' row count to "
                + newNumRows
                + " rows.");
      }

      return true;

    } catch (Throwable t) {
      String2.log(msg + " Error during lowUpdate: " + MustBe.throwableToString(t));
      return false;
    } finally {
      if (freshStore != null && freshStore != this.zarrStore) {
        if (freshStore instanceof AutoCloseable ac) {
          try {
            ac.close();
          } catch (Throwable ignore) {
          }
        }
      }
    }
  }

  /**
   * Constructs a sibling dataset for a new Zarr store source URL.
   *
   * @param tLocalSourceUrl new Zarr store path or URL
   * @param firstAxisToMatch unused for EDDTable
   * @param matchAxisNDigits unused for EDDTable
   * @param shareInfo whether to share data variable objects
   * @return EDDTableFromZarr sibling dataset instance
   * @throws Throwable if error
   */
  public EDDTableFromZarr sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable {
    if (verbose) String2.log("EDDTableFromZarr.sibling " + tLocalSourceUrl);

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

    EDDTableFromZarr newEDDTable =
        new EDDTableFromZarr(
            tDatasetID,
            String2.toSSVString(accessibleTo),
            "auto",
            shareInfo ? onChange : (StringArray) onChange.clone(),
            fgdcFile,
            iso19115File,
            sosOfferingPrefix,
            defaultDataQuery,
            defaultGraphQuery,
            null,
            addGlobalAttributes,
            tDataVariables,
            getReloadEveryNMinutes(),
            tLocalSourceUrl,
            zarrGroupName,
            rowDimensionName,
            chunkCacheSize,
            awsRegion,
            awsEndpoint,
            cacheFromUrl);

    if (shareInfo) {
      newEDDTable.dataVariables = dataVariables;
      newEDDTable.dataVariableSourceNames = dataVariableSourceNames();
      newEDDTable.dataVariableDestinationNames = dataVariableDestinationNames();
      newEDDTable.sourceGlobalAttributes = sourceGlobalAttributes();
      newEDDTable.addGlobalAttributes = addGlobalAttributes();
      newEDDTable.combinedGlobalAttributes = combinedGlobalAttributes();
      newEDDTable.addVariablesWhereAttNames = addVariablesWhereAttNames;
      newEDDTable.addVariablesWhereAttValues = addVariablesWhereAttValues;
    }

    return newEDDTable;
  }

  // Getters for configuration and state
  public String zarrStorePath() {
    return zarrStorePath;
  }

  public String zarrGroupName() {
    return zarrGroupName;
  }

  public String rowDimensionName() {
    return rowDimensionName;
  }

  public long numRows() {
    return numRows;
  }

  public long chunkCacheSize() {
    return chunkCacheSize;
  }

  public String awsRegion() {
    return awsRegion;
  }

  public String awsEndpoint() {
    return awsEndpoint;
  }

  public Store zarrStore() {
    return zarrStore;
  }

  public Group zarrGroup() {
    return zarrGroup;
  }

  /**
   * Helper method to derive a valid datasetID for an EDDTableFromZarr dataset.
   *
   * @param prefix optional dataset ID prefix
   * @param zarrStorePath Zarr store path or URL
   * @param zarrGroupName Zarr group name
   * @return sanitized dataset ID string
   */
  public static String suggestZarrDatasetID(
      String prefix, String zarrStorePath, String zarrGroupName) {
    String source = zarrStorePath;
    if (String2.isSomething(zarrGroupName) && !"/".equals(zarrGroupName.trim())) {
      source = File2.addSlash(zarrStorePath) + zarrGroupName;
    }
    String suggested = EDD.suggestDatasetID(source);
    if (String2.isSomething(prefix)) {
      String cleanPrefix = String2.modifyToBeFileNameSafe(prefix).replaceAll("_+", "_");
      if (cleanPrefix.endsWith("_")) {
        cleanPrefix = cleanPrefix.substring(0, cleanPrefix.length() - 1);
      }
      if (String2.isSomething(cleanPrefix)) {
        return cleanPrefix + "_" + suggested;
      }
    }
    return suggested;
  }

  /**
   * Generates a suggested datasets.xml configuration block for a Zarr store using default settings.
   *
   * @param zarrStorePath path or URL to the Zarr store
   * @param zarrGroupName group name within the store (or "" for root)
   * @return suggested XML string
   * @throws Throwable if error
   */
  public static String generateDatasetsXml(String zarrStorePath, String zarrGroupName)
      throws Throwable {
    return generateDatasetsXml(
        zarrStorePath,
        zarrGroupName,
        null,
        "",
        DEFAULT_RELOAD_EVERY_N_MINUTES,
        null,
        null,
        null,
        null);
  }

  /**
   * Generates a suggested datasets.xml configuration block for a Zarr store with custom settings.
   *
   * @param zarrStorePath path or URL to the Zarr store
   * @param zarrGroupName group name within the store (or "" for root)
   * @param rowDimensionName target row dimension name or null
   * @param datasetIDPrefix optional prefix for generated dataset ID
   * @param reloadEveryNMinutes dataset reload interval in minutes
   * @param cacheFromUrl cache directory/URL for remote store files
   * @return suggested XML string
   * @throws Throwable if error
   */
  public static String generateDatasetsXml(
      String zarrStorePath,
      String zarrGroupName,
      String rowDimensionName,
      String datasetIDPrefix,
      int reloadEveryNMinutes,
      String cacheFromUrl)
      throws Throwable {
    return generateDatasetsXml(
        zarrStorePath,
        zarrGroupName,
        rowDimensionName,
        datasetIDPrefix,
        reloadEveryNMinutes,
        cacheFromUrl,
        null,
        null,
        null);
  }

  /**
   * Generates a suggested datasets.xml configuration block for a Zarr store with full AWS and
   * external global attribute customization.
   *
   * @param zarrStorePath path or URL to the Zarr store
   * @param zarrGroupName group name within the store
   * @param rowDimensionName target row dimension name or null for auto-detect
   * @param datasetIDPrefix optional prefix for dataset ID
   * @param reloadEveryNMinutes dataset reload interval in minutes
   * @param cacheFromUrl cache URL/directory
   * @param awsRegion AWS region name if applicable
   * @param awsEndpoint custom AWS S3 endpoint if applicable
   * @param externalAddGlobalAttributes external global attributes to merge
   * @return suggested XML string
   * @throws Throwable if error
   */
  public static String generateDatasetsXml(
      String zarrStorePath,
      String zarrGroupName,
      String rowDimensionName,
      String datasetIDPrefix,
      int reloadEveryNMinutes,
      String cacheFromUrl,
      String awsRegion,
      String awsEndpoint,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromZarr.generateDatasetsXml"
            + "\nzarrStorePath="
            + zarrStorePath
            + "\nzarrGroupName="
            + zarrGroupName
            + "\nrowDimensionName="
            + rowDimensionName
            + "\ndatasetIDPrefix="
            + datasetIDPrefix
            + "\nreloadEveryNMinutes="
            + reloadEveryNMinutes
            + "\ncacheFromUrl="
            + cacheFromUrl
            + "\nawsRegion="
            + awsRegion
            + "\nawsEndpoint="
            + awsEndpoint);

    if (!String2.isSomething(zarrStorePath)) {
      throw new IllegalArgumentException("zarrStorePath wasn't specified.");
    }
    if (zarrGroupName == null) {
      zarrGroupName = "";
    }
    if (reloadEveryNMinutes <= 0 || reloadEveryNMinutes == Integer.MAX_VALUE) {
      reloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES; // 1440
    }

    Store zarrStore = createZarrStore(zarrStorePath, cacheFromUrl, awsRegion, awsEndpoint);
    Group zarrGroup = openZarrGroup(zarrStore, zarrGroupName);

    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();

    try {
      dev.zarr.zarrjava.core.Attributes zattrs = zarrGroup.metadata().attributes();
      if (zattrs != null) {
        populateAttributesFromZarr(zattrs, dataSourceTable.globalAttributes());
      }
    } catch (ZarrException ze) {
      String2.log("Warning: Could not read Zarr global attributes: " + ze.getMessage());
    }

    Map<String, ZarrArrayInfo> arrayMap = parseZarrMetadata(zarrGroup);

    Map<String, Long> dimToSizeMap = new LinkedHashMap<>();
    Map<String, Integer> dimFrequencyMap = new LinkedHashMap<>();

    for (ZarrArrayInfo info : arrayMap.values()) {
      if (info == null || info.isUnsupportedCodec) continue;
      if (info.dimensionNames != null && info.shape != null) {
        for (int d = 0; d < info.dimensionNames.length; d++) {
          String dimName = info.dimensionNames[d];
          long dimLen = d < info.shape.length ? info.shape[d] : 0;
          dimToSizeMap.putIfAbsent(dimName, dimLen);
          dimFrequencyMap.put(dimName, dimFrequencyMap.getOrDefault(dimName, 0) + 1);
        }
      }
    }

    String targetRowDimensionName = rowDimensionName;
    long targetNumRows = -1;

    if (String2.isSomething(targetRowDimensionName)) {
      if (dimToSizeMap.containsKey(targetRowDimensionName)) {
        targetNumRows = dimToSizeMap.get(targetRowDimensionName);
      } else {
        ZarrArrayInfo dimArrInfo = arrayMap.get(targetRowDimensionName);
        if (dimArrInfo != null && dimArrInfo.shape != null && dimArrInfo.shape.length == 1) {
          targetNumRows = dimArrInfo.shape[0];
        }
      }
    } else {
      String[] standardCandidates = new String[] {"obs", "row", "time", "index", "record", "i"};
      for (String cand : standardCandidates) {
        if (dimToSizeMap.containsKey(cand)) {
          targetRowDimensionName = cand;
          targetNumRows = dimToSizeMap.get(cand);
          break;
        }
      }
      if (!String2.isSomething(targetRowDimensionName) && !dimToSizeMap.isEmpty()) {
        String dominantDim = null;
        int maxFreq = -1;
        for (Map.Entry<String, Integer> entry : dimFrequencyMap.entrySet()) {
          if (entry.getValue() > maxFreq) {
            maxFreq = entry.getValue();
            dominantDim = entry.getKey();
          }
        }
        if (dominantDim != null) {
          targetRowDimensionName = dominantDim;
          targetNumRows = dimToSizeMap.get(dominantDim);
        }
      }
    }

    String featureType = dataSourceTable.globalAttributes().getString("featureType");
    if (!String2.isSomething(featureType)) {
      featureType = dataSourceTable.globalAttributes().getString("CF:featureType");
    }

    int dvCount = 0;
    for (ZarrArrayInfo info : arrayMap.values()) {
      if (info == null || info.isUnsupportedCodec) continue;

      if (info.is1D()) {
        boolean matchesRowDim = false;
        if (info.dimensionNames != null && info.dimensionNames.length > 0) {
          if (String2.isSomething(targetRowDimensionName)
              && targetRowDimensionName.equals(info.dimensionNames[0])) {
            matchesRowDim = true;
          }
        }
        if (!matchesRowDim
            && targetNumRows > 0
            && info.shape != null
            && info.shape[0] == targetNumRows) {
          matchesRowDim = true;
        }
        if (!matchesRowDim && !String2.isSomething(targetRowDimensionName)) {
          matchesRowDim = true;
        }
        if (!matchesRowDim) {
          String2.log(
              "EDDTableFromZarr generateDatasetsXml skipping 1D array '"
                  + info.name
                  + "' (does not match target row dimension '"
                  + targetRowDimensionName
                  + "' or numRows "
                  + targetNumRows
                  + ")");
          continue;
        }
      } else if (info.shape != null && info.shape.length > 1 && !info.is2DStringOrChar()) {
        continue;
      }

      String varName = info.name;
      Attributes sourceAtts = new Attributes();
      if (info.attributes != null) {
        info.attributes.copyTo(sourceAtts);
      }

      if (!String2.isSomething(featureType)) {
        String vft = sourceAtts.getString("featureType");
        if (String2.isSomething(vft)) {
          featureType = vft;
        }
      }

      PAType paType = info.paType != null ? info.paType : PAType.DOUBLE;
      if (info.is2DStringOrChar()) {
        paType = PAType.STRING;
      }

      PrimitiveArray sourcePA = PrimitiveArray.factory(paType, 1, false);
      dataSourceTable.addColumn(dvCount, varName, sourcePA, sourceAtts);

      Attributes addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              dataSourceTable.globalAttributes(),
              sourceAtts,
              null,
              varName,
              paType != PAType.STRING,
              paType != PAType.STRING,
              false);

      PrimitiveArray destPA = PrimitiveArray.factory(paType, 1, false);
      dataAddTable.addColumn(dvCount, varName, destPA, addAtts);
      dvCount++;
    }

    if (dataSourceTable.nColumns() == 0) {
      throw new SimpleException(
          "No tabular variables matching row dimension '"
              + targetRowDimensionName
              + "' were found in Zarr group '"
              + zarrGroupName
              + "' at "
              + zarrStorePath);
    }

    tryToFindLLAT(dataSourceTable, dataAddTable);
    ensureValidNames(dataSourceTable, dataAddTable);

    Attributes globalAddAtts = dataAddTable.globalAttributes();
    String defaultCdmDataType = "Point";
    if (String2.isSomething(featureType)) {
      String ftLower = featureType.trim().toLowerCase();
      if (ftLower.equals("point")) defaultCdmDataType = "Point";
      else if (ftLower.equals("timeseries")) defaultCdmDataType = "TimeSeries";
      else if (ftLower.equals("trajectory")) defaultCdmDataType = "Trajectory";
      else if (ftLower.equals("profile")) defaultCdmDataType = "Profile";
      else if (ftLower.equals("timeseriesprofile")) defaultCdmDataType = "TimeSeriesProfile";
    }

    globalAddAtts.set(
        makeReadyToUseAddGlobalAttributesForDatasetsXml(
            dataSourceTable.globalAttributes(),
            defaultCdmDataType,
            zarrStorePath,
            externalAddGlobalAttributes,
            suggestKeywords(dataSourceTable, dataAddTable)));

    String tDatasetID = suggestZarrDatasetID(datasetIDPrefix, zarrStorePath, zarrGroupName);

    StringBuilder sb = new StringBuilder();
    sb.append(
        "<dataset type=\"EDDTableFromZarr\" datasetID=\""
            + XML.encodeAsXML(tDatasetID)
            + "\" active=\"true\">\n");
    sb.append("    <reloadEveryNMinutes>" + reloadEveryNMinutes + "</reloadEveryNMinutes>\n");

    if (String2.isSomething(cacheFromUrl)) {
      sb.append("    <cacheFromUrl>" + XML.encodeAsXML(cacheFromUrl) + "</cacheFromUrl>\n");
    }

    sb.append("    <zarrStorePath>" + XML.encodeAsXML(zarrStorePath) + "</zarrStorePath>\n");
    if (String2.isSomething(zarrGroupName)) {
      sb.append("    <zarrGroupName>" + XML.encodeAsXML(zarrGroupName) + "</zarrGroupName>\n");
    }
    if (String2.isSomething(targetRowDimensionName)) {
      sb.append(
          "    <rowDimensionName>"
              + XML.encodeAsXML(targetRowDimensionName)
              + "</rowDimensionName>\n");
    }
    if (String2.isSomething(awsRegion)) {
      sb.append("    <awsRegion>" + XML.encodeAsXML(awsRegion) + "</awsRegion>\n");
    }
    if (String2.isSomething(awsEndpoint)) {
      sb.append("    <awsEndpoint>" + XML.encodeAsXML(awsEndpoint) + "</awsEndpoint>\n");
    }

    sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
    sb.append(writeAttsForDatasetsXml(true, globalAddAtts, "    "));

    sb.append(
        writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true, false));

    sb.append("</dataset>\n\n");

    return sb.toString();
  }
}
