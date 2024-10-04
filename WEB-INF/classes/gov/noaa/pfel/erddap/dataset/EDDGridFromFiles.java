/*
 * EDDGridFromFiles Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.Units2;
import com.google.common.base.Strings;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SharedWatchService;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.WatchDirectory;
import gov.noaa.pfel.coastwatch.util.WatchUpdateHandler;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridFromFilesHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.ThreadedWorkManager;
import gov.noaa.pfel.erddap.variable.*;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a virtual table of data from by aggregating the existing outer dimension of
 * a collection of data files. <br>
 * In a given file, if the outer dimension has more than one value, the values must be sorted
 * ascending, with no ties. <br>
 * The outer dimension values in different files can't overlap. <br>
 * The presumption is that the entire dataset can be read reasonable quickly (if the files are
 * remote, access will obviously be slower) and all variable's min and max info can be gathered (for
 * each file) and cached (facilitating handling constraints in data requests). <br>
 * And file data can be cached and reused because each file has a lastModified time and size which
 * can be used to detect if file is unchanged.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-11-26
 */
@SaxHandlerClass(EDDGridFromFilesHandler.class)
public abstract class EDDGridFromFiles extends EDDGrid implements WatchUpdateHandler {

  public static final String MF_FIRST = "first", MF_LAST = "last";
  public static int suggestedUpdateEveryNMillis = 10000;

  public static int suggestUpdateEveryNMillis(String tFileDir) {
    return String2.isTrulyRemote(tFileDir) ? 0 : suggestedUpdateEveryNMillis;
  }

  /** Don't set this to true here. Some test methods set this to true temporarily. */
  protected static boolean testQuickRestart = false;

  /** Columns in the File Table */
  protected static final int
      FT_DIR_INDEX_COL = 0, // useful that it is #0   (tFileTable uses same positions)
      FT_FILE_LIST_COL = 1, // useful that it is #1
      FT_LAST_MOD_COL = 2,
      FT_SIZE_COL = 3,
      FT_N_VALUES_COL = 4,
      FT_MIN_COL = 5,
      FT_MAX_COL = 6,
      FT_CSV_VALUES_COL = 7,
      FT_START_INDEX_COL = 8;

  // set by constructor
  protected String fileDir;
  protected boolean recursive;
  protected String fileNameRegex, pathRegex;
  protected String metadataFrom;
  protected StringArray sourceDataNames;
  protected StringArray sourceAxisNames;
  protected StringArray sourceAxisNamesNoAxis0; // only has content if axis0 is special
  protected String sourceDataTypes[];

  protected static final int AXIS0_REGULAR = 0, // uses existing axis0
      AXIS0_FILENAME = 1, // provides new axis0 with value from fileName
      AXIS0_GLOBAL = 2, // provides new axis0 with value from global atts
      AXIS0_REPLACE_FROM_FILENAME = 3, // replaces existing axis0 with value from fileName
      AXIS0_PATHNAME = 4; // provides new axis0 with value from absolute pathName

  protected int axis0Type = AXIS0_REGULAR;
  protected String axis0GlobalAttName = null; // used if AXIS0_GLOBAL
  protected String axis0TimeFormat = null; // used if sourceValue is a time
  protected String axis0TimeZoneString = null;
  protected TimeZone axis0TimeZone = null;
  protected DateTimeFormatter axis0DateTimeFormatter = null; // java.time (was Joda)
  protected PAType axis0PAType = PAType.DOUBLE; // the default
  protected String axis0Regex = "(.*)"; // the default
  protected Pattern axis0RegexPattern = null; // from regex
  protected int axis0CaptureGroup = 1; // the default

  /**
   * filesAreLocal true if files are on a local hard drive or false if files are remote. 1) A
   * failure when reading a local file, causes file to be marked as bad and dataset reloaded; but a
   * remote failure doesn't. 2) For remote files, the bad file list is rechecked every time dataset
   * is reloaded.
   */
  protected boolean filesAreLocal;

  protected boolean haveValidSourceInfo =
      false; // if true, following 3 are correctly set, and sourceGlobalAttributes
  protected Attributes sourceAxisAttributes[];
  protected PrimitiveArray sourceAxisValues[];
  protected Attributes sourceDataAttributes[];

  /**
   * This is used to test equality of axis values. 0=no testing (not recommended). &gt;18 does exact
   * test. default=20. 1-18 tests that many digits for doubles and hidiv(n,2) for floats.
   */
  protected int matchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

  protected WatchDirectory watchDirectory;

  // dirTable and fileTable inMemory (default=false)
  protected boolean fileTableInMemory = false;
  protected Table dirTable; // one column with dir names
  protected Table fileTable;

  protected String cacheFromUrl = null; // null if inactive
  protected long cacheMaxSizeB = -1; // cache threshold size in B, <=0 = copy the entire dataset
  protected String cachePartialPathRegex = null; // null if inactive

  /** When threshold size is reached, prune cache to fraction*threshold. */
  protected double cacheFraction = FileVisitorDNLS.PRUNE_CACHE_DEFAULT_FRACTION;

  // a system for deriving the source axis values without opening the file
  protected String sourceAxisValuesDataType;
  protected String sourceAxisValuesExtractRegex;
  protected int sourceAxisValuesCaptureGroupNumber;

  /**
   * This constructs an EDDGridFromFiles based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="[subclassName]"&gt; having
   *     just been read.
   * @return an EDDGridFromFiles. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridFromFiles fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDGridFromFiles(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    String tType = xmlReader.attributeValue("type");
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
    StringArray tOnChange = new StringArray();
    boolean tFileTableInMemory = false;
    String tFgdcFile = null;
    String tIso19115File = null;
    Attributes tGlobalAttributes = null;
    ArrayList tAxisVariables = new ArrayList();
    ArrayList tDataVariables = new ArrayList();
    int tReloadEveryNMinutes = Integer.MAX_VALUE;
    int tUpdateEveryNMillis = 0;
    String tFileDir = null;
    String tFileNameRegex = ".*";
    boolean tRecursive = false;
    String tPathRegex = ".*";
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    String tMetadataFrom = MF_LAST;
    int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
    boolean tDimensionValuesInMemory = true;
    String tCacheFromUrl = null;
    int tCacheSizeGB = -1;
    String tCachePartialPathRegex = null;

    // process the tags
    String startOfTags = xmlReader.allTags();
    int startOfTagsN = xmlReader.stackSize();
    int startOfTagsLength = startOfTags.length();
    while (true) {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String content = xmlReader.content();
      // if (reallyVerbose) String2.log("  tags=" + tags + content);
      if (xmlReader.stackSize() == startOfTagsN) break; // the </dataset> tag
      String localTags = tags.substring(startOfTagsLength);

      // try to make the tag names as consistent, descriptive and readable as possible
      if (localTags.equals("<addAttributes>")) tGlobalAttributes = getAttributesFromXml(xmlReader);
      else if (localTags.equals("<altitudeMetersPerSourceUnit>"))
        throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
      else if (localTags.equals("<axisVariable>"))
        tAxisVariables.add(getSDAVVariableFromXml(xmlReader));
      else if (localTags.equals("<dataVariable>"))
        tDataVariables.add(getSDADVariableFromXml(xmlReader));
      else if (localTags.equals("<accessibleTo>")) {
      } else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<accessibleViaWMS>")) {
      } else if (localTags.equals("</accessibleViaWMS>"))
        tAccessibleViaWMS = String2.parseBoolean(content);
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<updateEveryNMillis>")) {
      } else if (localTags.equals("</updateEveryNMillis>"))
        tUpdateEveryNMillis = String2.parseInt(content);
      else if (localTags.equals("<fileDir>")) {
      } else if (localTags.equals("</fileDir>")) tFileDir = content;
      else if (localTags.equals("<fileNameRegex>")) {
      } else if (localTags.equals("</fileNameRegex>")) tFileNameRegex = content;
      else if (localTags.equals("<recursive>")) {
      } else if (localTags.equals("</recursive>")) tRecursive = String2.parseBoolean(content);
      else if (localTags.equals("<pathRegex>")) {
      } else if (localTags.equals("</pathRegex>")) tPathRegex = content;
      else if (localTags.equals("<accessibleViaFiles>")) {
      } else if (localTags.equals("</accessibleViaFiles>"))
        tAccessibleViaFiles = String2.parseBoolean(content);
      else if (localTags.equals("<metadataFrom>")) {
      } else if (localTags.equals("</metadataFrom>")) tMetadataFrom = content;
      else if (localTags.equals("<fileTableInMemory>")) {
      } else if (localTags.equals("</fileTableInMemory>"))
        tFileTableInMemory = String2.parseBoolean(content);
      else if (localTags.equals("<matchAxisNDigits>")) {
      } else if (localTags.equals("</matchAxisNDigits>"))
        tMatchAxisNDigits = String2.parseInt(content, DEFAULT_MATCH_AXIS_N_DIGITS);
      else if (localTags.equals("<ensureAxisValuesAreEqual>")) {
      } // deprecated
      else if (localTags.equals("</ensureAxisValuesAreEqual>"))
        tMatchAxisNDigits = String2.parseBoolean(content) ? 20 : 0;
      else if (localTags.equals("<onChange>")) {
      } else if (localTags.equals("</onChange>")) tOnChange.add(content);
      else if (localTags.equals("<fgdcFile>")) {
      } else if (localTags.equals("</fgdcFile>")) tFgdcFile = content;
      else if (localTags.equals("<iso19115File>")) {
      } else if (localTags.equals("</iso19115File>")) tIso19115File = content;
      else if (localTags.equals("<defaultDataQuery>")) {
      } else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content;
      else if (localTags.equals("<defaultGraphQuery>")) {
      } else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content;
      else if (localTags.equals("<nThreads>")) {
      } else if (localTags.equals("</nThreads>")) tnThreads = String2.parseInt(content);
      else if (localTags.equals("<dimensionValuesInMemory>")) {
      } else if (localTags.equals("</dimensionValuesInMemory>"))
        tDimensionValuesInMemory = String2.parseBoolean(content);
      else if (localTags.equals("<cacheFromUrl>")) {
      } else if (localTags.equals("</cacheFromUrl>")) tCacheFromUrl = content;
      else if (localTags.equals("<cacheSizeGB>")) {
      } else if (localTags.equals("</cacheSizeGB>")) tCacheSizeGB = String2.parseInt(content);
      else if (localTags.equals("<cachePartialPathRegex>")) {
      } else if (localTags.equals("</cachePartialPathRegex>")) tCachePartialPathRegex = content;
      else xmlReader.unexpectedTagException();
    }
    int nav = tAxisVariables.size();
    Object ttAxisVariables[][] = new Object[nav][];
    for (int i = 0; i < tAxisVariables.size(); i++)
      ttAxisVariables[i] = (Object[]) tAxisVariables.get(i);

    int ndv = tDataVariables.size();
    Object ttDataVariables[][] = new Object[ndv][];
    for (int i = 0; i < tDataVariables.size(); i++)
      ttDataVariables[i] = (Object[]) tDataVariables.get(i);

    if (tType == null) tType = "";
    if (tType.equals("EDDGridFromAudioFiles"))
      return new EDDGridFromAudioFiles(
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
          ttAxisVariables,
          ttDataVariables,
          tReloadEveryNMinutes,
          tUpdateEveryNMillis,
          tFileDir,
          tFileNameRegex,
          tRecursive,
          tPathRegex,
          tMetadataFrom,
          tMatchAxisNDigits,
          tFileTableInMemory,
          tAccessibleViaFiles,
          tnThreads,
          tDimensionValuesInMemory,
          tCacheFromUrl,
          tCacheSizeGB,
          tCachePartialPathRegex);
    else if (tType.equals("EDDGridFromNcFiles"))
      return new EDDGridFromNcFiles(
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
          ttAxisVariables,
          ttDataVariables,
          tReloadEveryNMinutes,
          tUpdateEveryNMillis,
          tFileDir,
          tFileNameRegex,
          tRecursive,
          tPathRegex,
          tMetadataFrom,
          tMatchAxisNDigits,
          tFileTableInMemory,
          tAccessibleViaFiles,
          tnThreads,
          tDimensionValuesInMemory,
          tCacheFromUrl,
          tCacheSizeGB,
          tCachePartialPathRegex);
    else if (tType.equals("EDDGridFromNcFilesUnpacked"))
      return new EDDGridFromNcFilesUnpacked(
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
          ttAxisVariables,
          ttDataVariables,
          tReloadEveryNMinutes,
          tUpdateEveryNMillis,
          tFileDir,
          tFileNameRegex,
          tRecursive,
          tPathRegex,
          tMetadataFrom,
          tMatchAxisNDigits,
          tFileTableInMemory,
          tAccessibleViaFiles,
          tnThreads,
          tDimensionValuesInMemory,
          tCacheFromUrl,
          tCacheSizeGB,
          tCachePartialPathRegex);
    else if (tType.equals("EDDGridFromMergeIRFiles"))
      return new EDDGridFromMergeIRFiles(
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
          ttAxisVariables,
          ttDataVariables,
          tReloadEveryNMinutes,
          tUpdateEveryNMillis,
          tFileDir,
          tFileNameRegex,
          tRecursive,
          tPathRegex,
          tMetadataFrom,
          tMatchAxisNDigits,
          tFileTableInMemory,
          tAccessibleViaFiles,
          tnThreads,
          tDimensionValuesInMemory,
          tCacheFromUrl,
          tCacheSizeGB,
          tCachePartialPathRegex);
    else
      throw new Exception(
          "type=\"" + tType + "\" needs to be added to EDDGridFromFiles.fromXml at end.");
  }

  /**
   * The constructor.
   *
   * @param tClassName e.g., EDDGridFromNcFiles
   * @param tDatasetID is a very short string identifier (recommended: [A-Za-z][A-Za-z0-9_]* ) for
   *     this dataset. See EDD.datasetID().
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: ) to be done
   *     whenever the dataset changes significantly
   * @param tFgdcFile This should be the fullname of a file with the FGDC that should be used for
   *     this dataset, or "" (to cause ERDDAP not to try to generate FGDC metadata for this
   *     dataset), or null (to allow ERDDAP to try to generate FGDC metadata for this dataset).
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   * @param tAddGlobalAttributes are global attributes which will be added to (and take precedence
   *     over) the data source's global attributes. This may be null if you have nothing to add. The
   *     combined global attributes must include:
   *     <ul>
   *       <li>"title" - the short (&lt; 80 characters) description of the dataset
   *       <li>"summary" - the longer description of the dataset. It may have newline characters
   *           (usually at &lt;= 72 chars per line).
   *       <li>"institution" - the source of the data (best if &lt; 50 characters so it fits in a
   *           graph's legend).
   *       <li>"infoUrl" - the url with information about this data set
   *       <li>"cdm_data_type" - one of the EDD.CDM_xxx options
   *     </ul>
   *     Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
   *     Special case: if combinedGlobalAttributes name="license", any instance of "[standard]" will
   *     be converted to the EDStatic.standardLicense
   * @param tAxisVariables is an Object[nAxisVariables][3]: <br>
   *     [0]=String sourceName (the name of the data variable in the dataset source), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories, although they are added automatically for lon, lat, alt, and time).
   *     Special case: value="null" causes that item to be removed from combinedAttributes. <br>
   *     If there are longitude, latitude, altitude, or time variables, they must have that name as
   *     the destinationName (or sourceName) to be identified as such. <br>
   *     Or, use tAxisVariables=null if the axis variables need no addAttributes and the
   *     longitude,latitude,altitude,time variables (if present) all have their correct names in the
   *     source. <br>
   *     The order of variables you define must match the order in the source. <br>
   *     A time variable must have "units" specified in addAttributes (read first) or
   *     sourceAttributes. "units" must be a udunits string (containing " since ") describing how to
   *     interpret numbers (e.g., "seconds since 1970-01-01T00:00:00Z").
   * @param tDataVariables is an Object[nDataVariables][4]: <br>
   *     [0]=String sourceName (the name of the data variable in the dataset source, without the
   *     outer or inner sequence name), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories). Special case: value="null" causes that item to be removed from
   *     combinedAttributes. <br>
   *     [3]=String the source dataType (e.g., "int", "float", "String"). Some data sources have
   *     ambiguous data types, so it needs to be specified here. <br>
   *     This class is unusual: it is okay if different source files have different dataTypes. <br>
   *     All will be converted to the dataType specified here. <br>
   *     The order of variables you define doesn't have to match the order in the source.
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data.
   * @param tFileDir the base directory where the files are located
   * @param tFileNameRegex the regex which determines which files in the directories are to be read.
   *     <br>
   *     You can use .* for all, but it is better to be more specific. For example, .*\.nc will get
   *     all files with the extension .nc. <br>
   *     All files must have all of the axisVariables and all of the dataVariables.
   * @param tRecursive if true, this class will look for files in the fileDir and all subdirectories
   * @param tMetadataFrom this indicates the file to be used to extract source metadata (first/last
   *     based on file list sorted by minimum axis #0 value). Valid values are "first",
   *     "penultimate", "last". If invalid, "last" is used.
   * @param tMatchAxisNDigits 0=no test, 1-18 tests 1-18 digits for doubles and hiDiv(n,2) for
   *     floats, &gt;18 does an exact test. Default is 20.
   * @throws Throwable if trouble
   */
  public EDDGridFromFiles(
      String tClassName,
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      Attributes tAddGlobalAttributes,
      Object[][] tAxisVariables,
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tMetadataFrom,
      int tMatchAxisNDigits,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      int tnThreads,
      boolean tDimensionValuesInMemory,
      String tCacheFromUrl,
      int tCacheSizeGB,
      String tCachePartialPathRegex)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridFromFiles " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridFromFiles(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = tClassName;
    datasetID = tDatasetID;
    // ensure valid for creation of datasetInfo files below
    if (!String2.isFileNameSafe(datasetID))
      throw new IllegalArgumentException(
          errorInMethod + "datasetID=" + datasetID + " isn't fileNameSafe.");
    File2.makeDirectory(datasetDir()); // based on datasetID
    String dirTableFileName = datasetDir() + DIR_TABLE_FILENAME;
    String fileTableFileName = datasetDir() + FILE_TABLE_FILENAME;

    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    if (!tAccessibleViaWMS)
      accessibleViaWMS = String2.canonical(MessageFormat.format(EDStatic.noXxxAr[0], "WMS"));
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    if (tAddGlobalAttributes == null) tAddGlobalAttributes = new Attributes();
    addGlobalAttributes = tAddGlobalAttributes;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    setUpdateEveryNMillis(tUpdateEveryNMillis);
    fileTableInMemory = tFileTableInMemory;
    fileDir = File2.addSlash(tFileDir);
    fileNameRegex = tFileNameRegex;
    recursive = tRecursive;
    pathRegex = tPathRegex == null || tPathRegex.length() == 0 ? ".*" : tPathRegex;
    metadataFrom = tMetadataFrom;
    matchAxisNDigits = tMatchAxisNDigits;
    int nav = tAxisVariables.length;
    int ndv = tDataVariables.length;
    accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads
    dimensionValuesInMemory = tDimensionValuesInMemory;

    if (String2.isSomething(tCacheFromUrl) && !String2.isRemote(tCacheFromUrl))
      throw new IllegalArgumentException(errorInMethod + "'cacheFromUrl' must be a URL.");
    tCacheFromUrl = File2.addSlash(tCacheFromUrl);
    cacheFromUrl = String2.isRemote(tCacheFromUrl) ? tCacheFromUrl : null;
    cacheMaxSizeB =
        tCacheSizeGB <= 0 || tCacheSizeGB == Integer.MAX_VALUE
            ? -1
            : // <=0 = copy all
            tCacheSizeGB * Math2.BytesPerGB;
    cachePartialPathRegex =
        String2.isSomething(tCachePartialPathRegex) ? tCachePartialPathRegex : null;

    if (!String2.isSomething(fileDir))
      throw new IllegalArgumentException(errorInMethod + "fileDir wasn't specified.");
    filesAreLocal = !String2.isTrulyRemote(fileDir);
    if (filesAreLocal) fileDir = File2.addSlash(fileDir);
    if (fileNameRegex == null || fileNameRegex.length() == 0) fileNameRegex = ".*";
    if (metadataFrom == null) metadataFrom = "";
    if (metadataFrom.length() == 0) metadataFrom = MF_LAST;
    if (!metadataFrom.equals(MF_FIRST) && !metadataFrom.equals(MF_LAST))
      throw new IllegalArgumentException(
          "metadataFrom=" + metadataFrom + " must be " + MF_FIRST + " or " + MF_LAST + ".");

    // note sourceAxisNames and special axis0
    if (tAxisVariables.length == 0)
      throw new IllegalArgumentException("No axisVariables were specified.");
    sourceAxisNames = new StringArray();
    for (int av = 0; av < nav; av++) {
      String sn = (String) tAxisVariables[av][0];
      if (!String2.isSomething(sn))
        throw new IllegalArgumentException("axisVariable[" + av + "].sourceName wasn't specified.");
      sn = sn.trim();
      sourceAxisNames.add(sn);

      // special axis0?  ***fileName, timeFormat=YYYYMMDD, regex, captureGroup
      if (av == 0 && sn.startsWith("***")) {
        try {
          StringArray parts = StringArray.fromCSV(sn);
          int nParts = parts.size(); // >=1
          String part0 = parts.get(0);
          if (part0.equals("***replaceFromFileName")
              || part0.equals("***fileName")
              || part0.equals("***pathName")
              || part0.startsWith("***global:")) {

            // part0
            if (part0.equals("***replaceFromFileName")) {
              axis0Type = AXIS0_REPLACE_FROM_FILENAME;

            } else if (part0.equals("***fileName")) {
              axis0Type = AXIS0_FILENAME;

            } else if (part0.equals("***pathName")) {
              axis0Type = AXIS0_PATHNAME;

            } else {
              axis0Type = AXIS0_GLOBAL;
              axis0GlobalAttName = part0.substring(10);
              if (!String2.isSomething(axis0GlobalAttName))
                throw new RuntimeException(
                    "Attribute name not specified for axis[0] after \"***global:\".");
            }

            // timeFormat or element class
            if (nParts > 1 && String2.isSomething(parts.get(1))) {
              String tp = parts.get(1);
              // String2.log(">>> tp=" + tp);
              if (tp.startsWith("timeFormat=")) {
                axis0TimeFormat = tp.substring(11);
                axis0TimeZoneString = "Zulu";
                axis0TimeZone = TimeZone.getTimeZone(axis0TimeZoneString);
                axis0DateTimeFormatter =
                    Calendar2.makeDateTimeFormatter(axis0TimeFormat, axis0TimeZoneString);
                tp = "double";
              } // otherwise it should be a primitive type, e.g., double
              axis0PAType = PAType.fromCohortString(tp);
              if (axis0PAType == PAType.STRING)
                throw new IllegalArgumentException("Axis variables can't be Strings.");
            }

            // regex
            if (nParts > 2 && String2.isSomething(parts.get(2))) axis0Regex = parts.get(2);
            axis0RegexPattern = Pattern.compile(axis0Regex);

            // capture group
            if (nParts > 3 && String2.isSomething(parts.get(3))) {
              axis0CaptureGroup = String2.parseInt(parts.get(3));
              if (axis0CaptureGroup < 0 || axis0CaptureGroup == Integer.MAX_VALUE)
                throw new IllegalArgumentException("Invalid captureGroup=" + parts.get(3));
            }
            if (verbose)
              String2.log(
                  "axis0 "
                      + parts.get(0)
                      + " format="
                      + axis0TimeFormat
                      + " class="
                      + axis0PAType
                      + " regex="
                      + axis0Regex
                      + " captureGroup="
                      + axis0CaptureGroup);
          } else {
            throw new IllegalArgumentException("Invalid =...");
          }

        } catch (Throwable t) {
          throw new IllegalArgumentException(
              "axisVariable[0] special sourceName isn't valid: " + sn, t);
        }
      }
    }
    if (reallyVerbose) String2.log("sourceAxisNames=" + sourceAxisNames);
    if (axis0Type != AXIS0_REGULAR)
      sourceAxisNamesNoAxis0 =
          (StringArray) sourceAxisNames.subset(1, 1, sourceAxisNames.size() - 1);

    // note sourceDataNames, sourceDataTypes
    sourceDataNames = new StringArray();
    sourceDataTypes = new String[ndv];
    sourceDataAttributes = new Attributes[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      sourceDataNames.add((String) tDataVariables[dv][0]);
      sourceDataTypes[dv] = (String) tDataVariables[dv][3];
      if (sourceDataTypes[dv] == null || sourceDataTypes[dv].length() == 0)
        throw new IllegalArgumentException("Unspecified data type for var#" + dv + ".");
      sourceDataAttributes[dv] = new Attributes();
    }
    if (reallyVerbose)
      String2.log(
          "sourceDataNames="
              + sourceDataNames
              + "\nsourceDataTypes="
              + String2.toCSSVString(sourceDataTypes));

    // load cached dirTable->dirList
    dirTable = tryToLoadDirFileTable(dirTableFileName); // may be null
    if (dirTable != null) {
      if (verbose) String2.log(dirTable.nRows() + " rows in old dirTable");
      if (reallyVerbose) String2.log("first 5 rows=\n" + dirTable.dataToString(5));
    }

    // load cached fileTable
    fileTable = tryToLoadDirFileTable(fileTableFileName); // may be null
    if (fileTable != null) {
      if (verbose) String2.log(fileTable.nRows() + " rows in old fileTable");
      if (reallyVerbose) String2.log("first 5 rows=\n" + fileTable.dataToString(5));
    }

    // ensure fileTable has correct columns and data types
    if (fileTable != null) {
      boolean ok = true;
      if (fileTable.findColumnNumber("dirIndex") != FT_DIR_INDEX_COL) ok = false;
      else if (fileTable.findColumnNumber("fileList") != FT_FILE_LIST_COL) ok = false;
      else if (fileTable.findColumnNumber("lastMod") != FT_LAST_MOD_COL) ok = false;
      else if (fileTable.findColumnNumber("size") != FT_SIZE_COL) ok = false;
      else if (fileTable.findColumnNumber("nValues") != FT_N_VALUES_COL) ok = false;
      else if (fileTable.findColumnNumber("min") != FT_MIN_COL) ok = false;
      else if (fileTable.findColumnNumber("max") != FT_MAX_COL) ok = false;
      else if (fileTable.findColumnNumber("csvValues") != FT_CSV_VALUES_COL) ok = false;
      else if (fileTable.findColumnNumber("startIndex") != FT_START_INDEX_COL) ok = false;
      else if (!(fileTable.getColumn(FT_DIR_INDEX_COL) instanceof ShortArray)) ok = false;
      else if (!(fileTable.getColumn(FT_FILE_LIST_COL) instanceof StringArray)) ok = false;
      else if (!(fileTable.getColumn(FT_LAST_MOD_COL) instanceof LongArray)) ok = false;
      else if (!(fileTable.getColumn(FT_SIZE_COL) instanceof LongArray)) ok = false;
      else if (!(fileTable.getColumn(FT_N_VALUES_COL) instanceof IntArray)) ok = false;
      else if (!(fileTable.getColumn(FT_MIN_COL) instanceof DoubleArray)) ok = false;
      else if (!(fileTable.getColumn(FT_MAX_COL) instanceof DoubleArray)) ok = false;
      else if (!(fileTable.getColumn(FT_CSV_VALUES_COL) instanceof StringArray)) ok = false;
      else if (!(fileTable.getColumn(FT_START_INDEX_COL) instanceof IntArray)) ok = false;
      if (!ok) {
        String2.log(
            "Old fileTable discarded because of incorrect column arrangement (first 2 rows):\n"
                + fileTable.toString(2));
        fileTable = null;
      }
    }

    // load badFileMap
    ConcurrentHashMap badFileMap = readBadFileMap();

    // if trouble reading any, recreate all
    if (dirTable == null || fileTable == null || badFileMap == null) {
      if (verbose)
        String2.log(
            "creating new dirTable and fileTable "
                + "(dirTable=null?"
                + (dirTable == null)
                + " fileTable=null?"
                + (fileTable == null)
                + " badFileMap=null?"
                + (badFileMap == null)
                + ")");

      dirTable = new Table();
      dirTable.addColumn("dirName", new StringArray());

      fileTable = new Table();
      Test.ensureEqual(
          fileTable.addColumn(FT_DIR_INDEX_COL, "dirIndex", new ShortArray()),
          FT_DIR_INDEX_COL,
          "FT_DIR_INDEX_COL is wrong.");
      Test.ensureEqual(
          fileTable.addColumn(FT_FILE_LIST_COL, "fileList", new StringArray()),
          FT_FILE_LIST_COL,
          "FT_FILE_LIST_COL is wrong.");
      Test.ensureEqual(
          fileTable.addColumn(FT_LAST_MOD_COL, "lastMod", new LongArray()),
          FT_LAST_MOD_COL,
          "FT_LAST_MOD_COL is wrong.");
      Test.ensureEqual(
          fileTable.addColumn(FT_SIZE_COL, "size", new LongArray()),
          FT_SIZE_COL,
          "FT_SIZE is wrong.");
      Test.ensureEqual(
          fileTable.addColumn(FT_N_VALUES_COL, "nValues", new IntArray()),
          FT_N_VALUES_COL,
          "FT_N_VALUES_COL is wrong.");
      Test.ensureEqual(
          fileTable.addColumn(FT_MIN_COL, "min", new DoubleArray()),
          FT_MIN_COL,
          "FT_MIN_COL is wrong.");
      Test.ensureEqual(
          fileTable.addColumn(FT_MAX_COL, "max", new DoubleArray()),
          FT_MAX_COL,
          "FT_MAX_COL is wrong.");
      Test.ensureEqual(
          fileTable.addColumn(FT_CSV_VALUES_COL, "csvValues", new StringArray()),
          FT_CSV_VALUES_COL,
          "FT_CSV_VALUES_COL is wrong.");
      Test.ensureEqual(
          fileTable.addColumn(FT_START_INDEX_COL, "startIndex", new IntArray()),
          FT_START_INDEX_COL,
          "FT_START_INDEX_COL is wrong.");

      badFileMap = newEmptyBadFileMap();
    }

    // skip loading until after intial loadDatasets?
    if (!EDStatic.forceSynchronousLoading
        && fileTable.nRows() == 0
        && EDStatic.initialLoadDatasets()) {
      requestReloadASAP();
      throw new RuntimeException(DEFER_LOADING_DATASET_BECAUSE + "fileTable.nRows=0.");
    }

    // get the dirTable and fileTable PrimitiveArrays
    StringArray dirList = (StringArray) dirTable.getColumn(0);
    ShortArray ftDirIndex = (ShortArray) fileTable.getColumn(FT_DIR_INDEX_COL);
    StringArray ftFileList = (StringArray) fileTable.getColumn(FT_FILE_LIST_COL);
    LongArray ftLastMod = (LongArray) fileTable.getColumn(FT_LAST_MOD_COL);
    LongArray ftSize = (LongArray) fileTable.getColumn(FT_SIZE_COL);
    IntArray ftNValues = (IntArray) fileTable.getColumn(FT_N_VALUES_COL);
    DoubleArray ftMin = (DoubleArray) fileTable.getColumn(FT_MIN_COL);
    DoubleArray ftMax = (DoubleArray) fileTable.getColumn(FT_MAX_COL);
    StringArray ftCsvValues = (StringArray) fileTable.getColumn(FT_CSV_VALUES_COL);
    IntArray ftStartIndex = (IntArray) fileTable.getColumn(FT_START_INDEX_COL);

    // get sourceAxisValues and sourceAxisAttributes from an existing file (if any)
    // Last one should succeed and usually has newest (most?) data variables.
    for (int i = ftFileList.size() - 1; i >= 0; i--) {
      String tDir = dirList.get(ftDirIndex.get(i));
      String tName = ftFileList.get(i);

      // ensure file size and lastMod are unchanged (i.e., file is unchanged)
      // and fileDir and fileName match the current settings.
      // should I also do this if files are remote???
      if (filesAreLocal) {
        // Zarr files are actually directories and so their last modified and size
        // will provide inacurate results below.
        boolean isZarr = tName.contains("zarr") || tDir.contains("zarr");
        long lastMod = File2.getLastModified(tDir + tName);

        // 0=trouble: unavailable or changed
        if (!isZarr && (lastMod == 0 || ftLastMod.get(i) != lastMod)) {
          continue;
        }
        long size = File2.length(tDir + tName);
        // -1=touble: unavailable or changed
        if (!isZarr
            && (size < 0 || size == Long.MAX_VALUE || (filesAreLocal && ftSize.get(i) != size))) {
          continue;
        }
        if (!tDir.startsWith(fileDir)) {
          continue;
        }
        // Since a zarr file is a directory, it might be entirely in the tDir variable.
        // Add matching for characters before (the path to the directory) and after (usually
        // trailing slash).
        if (!(tName.matches(fileNameRegex)
            || (isZarr && tDir.matches("(.*)" + fileNameRegex + "(.*)")))) {
          continue;
        }
      }

      Attributes tSourceGlobalAttributes = new Attributes();
      Attributes tSourceAxisAttributes[] = new Attributes[nav];
      Attributes tSourceDataAttributes[] = new Attributes[ndv];
      for (int avi = 0; avi < nav; avi++) tSourceAxisAttributes[avi] = new Attributes();
      for (int dvi = 0; dvi < ndv; dvi++) tSourceDataAttributes[dvi] = new Attributes();
      try {
        getSourceMetadata(
            tDir,
            tName,
            sourceAxisNames,
            sourceDataNames,
            sourceDataTypes,
            tSourceGlobalAttributes,
            tSourceAxisAttributes,
            tSourceDataAttributes);
        PrimitiveArray tSourceAxisValues[] =
            getSourceAxisValues(tDir, tName, sourceAxisNames, sourceDataNames);
        // sets haveValidSourceInfo=true if okay; throws Exception if not
        validateCompareSet(
            tDir,
            tName,
            tSourceGlobalAttributes,
            tSourceAxisAttributes,
            tSourceAxisValues,
            tSourceDataAttributes);
        if (verbose)
          String2.log(
              "got metadata from previously good file #"
                  + (ftFileList.size() - 1 - i)
                  + " of "
                  + ftDirIndex.size()
                  + ": "
                  + tDir
                  + tName);
        break; // successful, no need to continue
      } catch (Throwable t) {
        String reason = MustBe.throwableToShortString(t);
        String2.log(
            String2.ERROR
                + " in "
                + datasetID
                + " constructor while getting metadata for "
                + tDir
                + tName
                + "\n"
                + reason);
        if (Thread.currentThread().isInterrupted()
            || t instanceof InterruptedException
            || reason.indexOf(Math2.TooManyOpenFiles) >= 0) throw t; // stop loading this dataset
        if (!(t instanceof TimeoutException)
            && !(t
                instanceof
                FileNotFoundException)) // occurs when a RAID unmounts itself. If really gone,
          // removing from file list is enough.                     )
          addBadFile(badFileMap, ftDirIndex.get(i), tName, ftLastMod.get(i), reason);
      }
    }
    // initially there are no files, so haveValidSourceInfo will still be false
    String msg = "";

    // set up watchDirectory
    if (updateEveryNMillis > 0) {
      try {
        if (EDStatic.useSharedWatchService) {
          SharedWatchService.watchDirectory(fileDir, recursive, pathRegex, this, datasetID);
        } else {
          watchDirectory = WatchDirectory.watchDirectoryAll(fileDir, recursive, pathRegex);
        }
      } catch (Throwable t) {
        updateEveryNMillis = 0; // disable the inotify system for this instance
        String subject = String2.ERROR + " in " + datasetID + " constructor (inotify)";
        String tmsg = MustBe.throwableToString(t);
        if (tmsg.indexOf("inotify instances") >= 0) tmsg += EDStatic.inotifyFixAr[0];
        EDStatic.email(EDStatic.adminEmail, subject, tmsg);
      }
    }

    // doQuickRestart?
    boolean doQuickRestart =
        haveValidSourceInfo
            && (testQuickRestart || (EDStatic.quickRestart && EDStatic.initialLoadDatasets()));
    if (verbose) String2.log("doQuickRestart=" + doQuickRestart);

    if (doQuickRestart) {
      msg = "\nQuickRestart";

    } else {
      // !doQuickRestart

      // if copy all remote files via taskThread, start those tasks now
      if (cacheFromUrl != null && cacheMaxSizeB <= 0) {
        String cPathRegex = pathRegex;
        if (cachePartialPathRegex != null) {
          // if this is same month, use cachePartialPathRegex
          String fileTableMonth =
              Calendar2.millisToIsoDateString(File2.getLastModified(fileTableFileName))
                  .substring(0, 7); // 0 if trouble
          String currentMonth = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 7);
          if (reallyVerbose)
            String2.log("fileTableMonth=" + fileTableMonth + " currentMonth=" + currentMonth);
          if (currentMonth.equals(fileTableMonth)) cPathRegex = cachePartialPathRegex;
        }
        EDStatic.makeCopyFileTasks(
            className,
            EDStatic.DefaultMaxMakeCopyFileTasks,
            datasetID,
            cacheFromUrl,
            fileNameRegex,
            recursive,
            cPathRegex,
            fileDir);
      }

      // get tAvailableFiles with available data files
      // and make tDirIndex and tFileList
      long elapsedTime = System.currentTimeMillis();
      // was tAvailableFiles with dir+name
      Table tFileTable = getFileInfo(fileDir, fileNameRegex, recursive, pathRegex);
      StringArray tFileDirPA = (StringArray) tFileTable.getColumn(FileVisitorDNLS.DIRECTORY);
      StringArray tFileNamePA = (StringArray) tFileTable.getColumn(FileVisitorDNLS.NAME);
      LongArray tFileLastModPA = (LongArray) tFileTable.getColumn(FileVisitorDNLS.LASTMODIFIED);
      LongArray tFileSizePA = (LongArray) tFileTable.getColumn(FileVisitorDNLS.SIZE);
      tFileTable.removeColumn(FileVisitorDNLS.SIZE);
      int ntft = tFileNamePA.size();
      msg =
          ntft
              + " files found in "
              + fileDir
              + "\nregex="
              + fileNameRegex
              + " recursive="
              + recursive
              + " pathRegex="
              + pathRegex
              + " time="
              + (System.currentTimeMillis() - elapsedTime)
              + "ms";
      if (ntft == 0)
        // Just exit. Don't delete the dirTable and fileTable files!
        // The problem may be that a drive isn't mounted.
        throw new RuntimeException(msg);
      if (verbose) String2.log(msg);
      msg = "";

      // switch to dir indexes
      ShortArray tFileDirIndexPA = new ShortArray(ntft, false);
      tFileTable.removeColumn(0); // tFileDirPA col
      tFileTable.addColumn(0, "dirIndex", tFileDirIndexPA); // col 0, matches fileTable
      tFileTable.setColumnName(1, "fileList"); // col 1, matches fileTable
      String lastDir = "\u0000";
      int lastPo = -1;
      for (int i = 0; i < ntft; i++) {
        String tDir = tFileDirPA.get(i);
        int po = lastPo;
        if (!tDir.equals(lastDir)) { // rare
          po = dirList.indexOf(tDir); // linear search, but should be short list
          if (po < 0) {
            po = dirList.size();
            dirList.add(tDir);
          }
          lastDir = tDir;
          lastPo = po;
        }
        tFileDirIndexPA.addInt(po);
      }
      tFileDirPA = null; // allow gc

      // remove "badFiles" if they no longer exist (in tAvailableFiles)
      if (badFileMap.size() > 0) {
        // make hashset with all tAvailableFiles as dirIndex/fileName
        HashSet<String> tFileSet = new HashSet(Math2.roundToInt(1.4 * ntft));
        for (int i = 0; i < ntft; i++) {
          tFileSet.add(tFileDirIndexPA.get(i) + "/" + tFileNamePA.get(i)); // dirIndex/fileName
          // String2.log("tFileSet add: " +   tFileDirIndexPA.get(i) + "/" + tFileNamePA.get(i));
        }

        Object badFileNames[] = badFileMap.keySet().toArray();
        int nMissing = 0;
        int nbfn = badFileNames.length;
        for (int i = 0; i < nbfn; i++) {
          Object name = badFileNames[i];
          if (!tFileSet.contains(name)) {
            if (reallyVerbose) String2.log("previously bad file now missing: " + name);
            nMissing++;
            badFileMap.remove(name);
          }
        }
        if (verbose) String2.log("old nBadFiles size=" + nbfn + "   nMissing=" + nMissing);

      } else {
        if (verbose) String2.log("old nBadFiles size=0");
      }

      // sort fileTable and tFileTable based on dirIndex and file names
      elapsedTime = System.currentTimeMillis();
      fileTable.leftToRightSort(2); // lexical sort so can walk through below
      tFileTable.leftToRightSort(2); // lexical sort so can walk through below
      if (reallyVerbose)
        String2.log("sortTime=" + (System.currentTimeMillis() - elapsedTime) + "ms");

      // remove any files in fileTable not in tFileTable  (i.e., the file was deleted)
      // I can step through fileTable and tFileTable since both sorted same way
      {
        int nft = ftFileList.size();
        BitSet keepFTRow = new BitSet(nft); // all false
        int nFilesMissing = 0;
        int tPo = 0;
        for (int ftPo = 0; ftPo < nft; ftPo++) {
          int dirI = ftDirIndex.get(ftPo);
          String fileS = ftFileList.get(ftPo);

          // skip through tDir until it is >= ftDir
          while (tPo < ntft && tFileDirIndexPA.get(tPo) < dirI) tPo++;

          // if dirs match, skip through tFile until it is >= ftFile
          boolean keep;
          if (tPo < ntft && tFileDirIndexPA.get(tPo) == dirI) {
            while (tPo < ntft
                && tFileDirIndexPA.get(tPo) == dirI
                && tFileNamePA.get(tPo).compareTo(fileS) < 0) tPo++;
            keep =
                tPo < ntft
                    && tFileDirIndexPA.get(tPo) == dirI
                    && tFileNamePA.get(tPo).equals(fileS);
          } else {
            keep = false;
          }

          // deal with keep
          if (keep) keepFTRow.set(ftPo, true);
          else {
            nFilesMissing++;
            if (reallyVerbose)
              String2.log("previously valid file now missing: " + dirList.get(dirI) + fileS);
          }
        }
        if (verbose) String2.log("old fileTable size=" + nft + "   nFilesMissing=" + nFilesMissing);
        fileTable.justKeep(keepFTRow);
      }

      // update fileTable  by processing tFileTable
      int fileListPo = 0; // next one to look at
      int tFileListPo = 0; // next one to look at
      int nReadFile = 0, nNoLastMod = 0, nNoSize = 0;
      long readFileCumTime = 0;
      long removeCumTime = 0;
      int nUnchanged = 0, nRemoved = 0, nDifferentModTime = 0, nNew = 0;
      elapsedTime = System.currentTimeMillis();
      while (tFileListPo < tFileNamePA.size()) {
        if (Thread.currentThread().isInterrupted())
          throw new SimpleException("EDDGridFromFiles.init" + EDStatic.caughtInterruptedAr[0]);

        int tDirI = tFileDirIndexPA.get(tFileListPo);
        String tFileS = tFileNamePA.get(tFileListPo);
        if (Strings.isNullOrEmpty(tFileS)) {
          boolean isZarr =
              tFileNameRegex.contains("zarr")
                  || (tPathRegex != null && tPathRegex.contains("zarr"));
          if (isZarr) {
            if (!isZarr || tDirI == Integer.MAX_VALUE) {
              tFileListPo++;
              // Skipping file name that is null or empty string and not in zarr.
              continue;
            }
            String dirName = Path.of(dirList.get(tDirI)).getFileName().toString();
            if (!dirName.matches(fileNameRegex)) {
              // If the file name is empty and we're in a zarr file, that means effectively
              // the last dirname is the file name, so make sure it matches the fileNameRegex.
              tFileListPo++;
              continue;
            }
          }
        }
        int dirI = fileListPo < ftFileList.size() ? ftDirIndex.get(fileListPo) : Integer.MAX_VALUE;
        String fileS = fileListPo < ftFileList.size() ? ftFileList.get(fileListPo) : "\uFFFF";
        long lastMod = fileListPo < ftFileList.size() ? ftLastMod.get(fileListPo) : Long.MAX_VALUE;
        double size = fileListPo < ftFileList.size() ? ftSize.get(fileListPo) : Long.MAX_VALUE;
        if (reallyVerbose) String2.log("#" + tFileListPo + " file=" + dirList.get(tDirI) + tFileS);

        // is tLastMod available for tFile?
        long tLastMod = tFileLastModPA.get(tFileListPo);
        if (tLastMod == 0 || tLastMod == Long.MAX_VALUE) { // 0=trouble
          nNoLastMod++;
          String2.log(
              "#"
                  + tFileListPo
                  + " reject because unable to get lastMod time: "
                  + dirList.get(tDirI)
                  + tFileS);
          tFileListPo++;
          addBadFile(badFileMap, tDirI, tFileS, tLastMod, "Unable to get lastMod time.");
          continue;
        }

        // is tSize available for tFile?
        long tSize = tFileSizePA.get(tFileListPo);
        if (tSize < 0 || tSize == Long.MAX_VALUE) { // -1=trouble
          nNoSize++;
          String2.log(
              "#"
                  + tFileListPo
                  + " reject because unable to get size: "
                  + dirList.get(tDirI)
                  + tFileS);
          tFileListPo++;
          addBadFile(badFileMap, tDirI, tFileS, tLastMod, "Unable to get size.");
          continue;
        }

        // is tFile in badFileMap?
        Object bfi = badFileMap.get(tDirI + "/" + tFileS);
        if (bfi != null) {
          // tFile is in badFileMap
          Object bfia[] = (Object[]) bfi;
          long bfLastMod = ((Long) bfia[0]).longValue();
          if (bfLastMod == tLastMod) {
            // file hasn't been changed; it is still bad
            tFileListPo++;
            if (tDirI == dirI && tFileS.equals(fileS)) {
              // remove it from cache   (Yes, a file may be marked bad (recently) and so still be in
              // cache)
              nRemoved++;
              removeCumTime -= System.currentTimeMillis();
              fileTable.removeRow(fileListPo);
              removeCumTime += System.currentTimeMillis();
            }
            // go on to next tFile
            continue;
          } else {
            // file has been changed since being marked as bad; remove from badFileMap
            badFileMap.remove(tDirI + "/" + tFileS);
            // and continue processing this file
          }
        }

        // is tFile already in cache?
        if (tDirI == dirI
            && tFileS.equals(fileS)
            && tLastMod == lastMod
            && (tSize == size
                || !filesAreLocal)) { // remote file's size may be approximate, e.g., 11K
          if (reallyVerbose) String2.log("#" + tFileListPo + " already in cache");
          nUnchanged++;
          tFileListPo++;
          fileListPo++;
          continue;
        }

        // file in cache no longer exists: remove from fileTable
        if (dirI < tDirI || (dirI == tDirI && fileS.compareTo(tFileS) < 0)) {
          if (verbose)
            String2.log(
                "#"
                    + tFileListPo
                    + " file no longer exists: remove from cache: "
                    + dirList.get(dirI)
                    + fileS);
          nRemoved++;
          removeCumTime -= System.currentTimeMillis();
          fileTable.removeRow(fileListPo);
          removeCumTime += System.currentTimeMillis();
          // tFileListPo isn't incremented, so it will be considered again in next iteration
          continue;
        }

        // tFile is new, or tFile is in ftFileList but time is different
        if (dirI == tDirI && fileS.equals(tFileS)) {
          if (verbose)
            String2.log(
                "#"
                    + tFileListPo
                    + " already in cache (but time changed): "
                    + dirList.get(tDirI)
                    + tFileS);
          nDifferentModTime++;
        } else {
          // if new, add row to fileTable
          if (verbose) String2.log("#" + tFileListPo + " inserted in cache");
          nNew++;
          fileTable.insertBlankRow(fileListPo);
        }

        // gather file's info
        try {
          ftDirIndex.setInt(fileListPo, tDirI);
          ftFileList.set(fileListPo, tFileS);
          ftLastMod.set(fileListPo, tLastMod);
          ftSize.set(fileListPo, tSize);

          // read axis values
          nReadFile++;
          long rfcTime = System.currentTimeMillis();
          PrimitiveArray[] tSourceAxisValues =
              getSourceAxisValues(dirList.get(tDirI), tFileS, sourceAxisNames, sourceDataNames);
          readFileCumTime += System.currentTimeMillis() - rfcTime;

          // test that all axisVariable and dataVariable units are identical
          // this also tests if all dataVariables are present
          Attributes tSourceGlobalAttributes = new Attributes();
          Attributes tSourceAxisAttributes[] = new Attributes[nav];
          Attributes tSourceDataAttributes[] = new Attributes[ndv];
          for (int avi = 0; avi < nav; avi++) tSourceAxisAttributes[avi] = new Attributes();
          for (int dvi = 0; dvi < ndv; dvi++) tSourceDataAttributes[dvi] = new Attributes();
          getSourceMetadata(
              dirList.get(tDirI),
              tFileS,
              sourceAxisNames,
              sourceDataNames,
              sourceDataTypes,
              tSourceGlobalAttributes,
              tSourceAxisAttributes,
              tSourceDataAttributes);
          validateCompareSet( // throws Exception if not
              dirList.get(tDirI),
              tFileS,
              tSourceGlobalAttributes,
              tSourceAxisAttributes,
              tSourceAxisValues,
              tSourceDataAttributes);

          // store n, min, max, values
          int tnValues = tSourceAxisValues[0].size();
          ftNValues.set(fileListPo, tnValues);
          ftMin.set(fileListPo, tSourceAxisValues[0].getNiceDouble(0));
          ftMax.set(fileListPo, tSourceAxisValues[0].getNiceDouble(tnValues - 1));
          ftCsvValues.set(fileListPo, tSourceAxisValues[0].toString());

          tFileListPo++;
          fileListPo++;

        } catch (Throwable t) {
          String fullName = dirList.get(tDirI) + tFileS;
          msg =
              "#"
                  + tFileListPo
                  + " bad file: removing fileTable row for "
                  + fullName
                  + "\n"
                  + MustBe.throwableToString(t);
          String2.log(msg);
          if (Thread.currentThread().isInterrupted()
              || t instanceof InterruptedException
              || t instanceof TimeoutException
              || msg.indexOf(Math2.TooManyOpenFiles) >= 0) throw t; // stop loading this dataset
          msg = "";
          nRemoved++;
          removeCumTime -= System.currentTimeMillis();
          fileTable.removeRow(fileListPo);
          removeCumTime += System.currentTimeMillis();
          tFileListPo++;
          if (System.currentTimeMillis() - tLastMod > 30 * Calendar2.MILLIS_PER_MINUTE)
            // >30 minutes old, so not still being ftp'd, so add to badFileMap
            addBadFile(badFileMap, tDirI, tFileS, tLastMod, MustBe.throwableToShortString(t));
        }
      }
      if (verbose)
        String2.log("fileTable updated; time=" + (System.currentTimeMillis() - elapsedTime) + "ms");

      // sort fileTable by FT_MIN_COL
      elapsedTime = System.currentTimeMillis();
      fileTable.sort(new int[] {FT_MIN_COL}, new boolean[] {true});
      if (reallyVerbose)
        String2.log("2nd sortTime=" + (System.currentTimeMillis() - elapsedTime) + "ms");

      msg =
          "\n  tFileNamePA.size="
              + tFileNamePA.size()
              + "\n  dirTable.nRows="
              + dirTable.nRows()
              + "\n  fileTable.nRows="
              + fileTable.nRows()
              + "\n    fileTableInMemory="
              + fileTableInMemory
              + "\n    nUnchanged="
              + nUnchanged
              + "\n    nRemoved="
              + nRemoved
              + " (nNoLastMod="
              + nNoLastMod
              + ", nNoSize="
              + nNoSize
              + ")"
              + "\n    nReadFile="
              + nReadFile
              + " (nDifferentModTime="
              + nDifferentModTime
              + " nNew="
              + nNew
              + ")"
              + " readFileCumTime="
              + Calendar2.elapsedTimeString(readFileCumTime)
              + " avg="
              + (readFileCumTime / Math.max(1, nReadFile))
              + "ms";
      if (verbose || fileTable.nRows() == 0) String2.log(msg);
      if (fileTable.nRows() == 0) throw new RuntimeException("No valid files!");

      // end !doQuickRestart
    }

    // finish up, validate, and (if !quickRestart) save dirTable, fileTable, badFileMap
    PrimitiveArray sourceAxisValues0 =
        PrimitiveArray.factory(
            sourceAxisValues[0].elementType(),
            ftDirIndex.size(), // size is a guess: the minimum possible, but usually correct
            false); // not active
    updateValidateFileTable(
        dirList,
        ftDirIndex,
        ftFileList,
        ftMin,
        ftMax,
        ftStartIndex,
        ftNValues,
        ftCsvValues,
        sourceAxisValues0);
    if (!doQuickRestart)
      saveDirTableFileTableBadFiles(-1, dirTable, fileTable, badFileMap); // throws Throwable

    // set creationTimeMillis to fileTable lastModified
    // (either very recent or (if quickRestart) from previous full restart)
    creationTimeMillis = File2.getLastModified(datasetDir() + FILE_TABLE_FILENAME);

    if (!badFileMap.isEmpty()) {
      StringBuilder emailSB = new StringBuilder();
      emailSB.append(badFileMapToString(badFileMap, dirList));
      emailSB.append(msg + "\n\n");
      EDStatic.email(
          EDStatic.emailEverythingToCsv, errorInMethod + "Bad Files", emailSB.toString());
    }

    // get source metadataFrom and axis values from FIRST|LAST file (lastModifiedTime)
    sourceGlobalAttributes = new Attributes();
    sourceAxisAttributes = new Attributes[nav];
    sourceDataAttributes = new Attributes[ndv];
    for (int avi = 0; avi < nav; avi++) sourceAxisAttributes[avi] = new Attributes();
    for (int dvi = 0; dvi < ndv; dvi++) sourceDataAttributes[dvi] = new Attributes();
    int nMinMaxIndex[] = ftLastMod.getNMinMaxIndex();
    int tFileI = metadataFrom.equals(MF_FIRST) ? nMinMaxIndex[1] : nMinMaxIndex[2];
    if (verbose)
      String2.log(
          "getting metadataFrom "
              + dirList.get(ftDirIndex.get(tFileI))
              + ftFileList.get(tFileI)
              + "\n  ftLastMod"
              + " first="
              + Calendar2.millisToIsoStringTZ(ftLastMod.get(nMinMaxIndex[1]))
              + " last="
              + Calendar2.millisToIsoStringTZ(ftLastMod.get(nMinMaxIndex[2])));
    boolean tHave = haveValidSourceInfo; // ensure getSourceMetadata actually reads the file
    haveValidSourceInfo = false;
    String mdFromDir = dirList.get(ftDirIndex.get(tFileI));
    String mdFromName = ftFileList.get(tFileI);
    getSourceMetadata(
        mdFromDir,
        mdFromName,
        sourceAxisNames,
        sourceDataNames,
        sourceDataTypes,
        sourceGlobalAttributes,
        sourceAxisAttributes,
        sourceDataAttributes);
    // 2020-03-09 added this so source axis values from FIRST|LAST too
    sourceAxisValues =
        getSourceAxisValues(
            dirList.get(ftDirIndex.get(tFileI)),
            ftFileList.get(tFileI),
            sourceAxisNames,
            sourceDataNames);
    haveValidSourceInfo = tHave;

    // if accessibleViaFiles=true and filesInS3Bucket, test if files are in a private bucket
    // and thus /files/ access must be handles by ERDDAP acting as go between
    // (not just redirect, which works for public bucket)
    filesInS3Bucket = String2.isAwsS3Url(mdFromDir);
    if (accessibleViaFiles && filesInS3Bucket) {
      filesInPrivateS3Bucket = SSR.awsS3FileIsPrivate(mdFromDir + mdFromName);
      if (verbose)
        String2.log(
            "  For datasetID=" + datasetID + ", filesInPrivateS3Bucket=" + filesInPrivateS3Bucket);
    }

    // make combinedGlobalAttributes
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    String tLicense = combinedGlobalAttributes.getString("license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          "license", String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");
    if (combinedGlobalAttributes.getString("cdm_data_type") == null)
      combinedGlobalAttributes.add("cdm_data_type", "Grid");
    if (combinedGlobalAttributes.get("sourceUrl") == null) {
      localSourceUrl =
          "(" + (filesAreLocal ? "local" : "remote") + " files)"; // keep location private
      addGlobalAttributes.set("sourceUrl", localSourceUrl);
      combinedGlobalAttributes.set("sourceUrl", localSourceUrl);
    }

    // set combined sourceAxisValues[0]
    sourceAxisValues[0] = sourceAxisValues0;
    // String2.log("\n>>> sourceAxisValues sav0=" + sourceAxisValues[0].toString() + "\n");

    // make the axisVariables[]
    axisVariables = new EDVGridAxis[nav];
    for (int av = 0; av < nav; av++) {
      String tSourceName = sourceAxisNames.get(av);
      String tDestName = (String) tAxisVariables[av][1];
      Attributes tAddAtt = (Attributes) tAxisVariables[av][2];
      Attributes tSourceAtt = sourceAxisAttributes[av];
      if (tDestName == null || tDestName.trim().length() == 0) tDestName = tSourceName;
      axisVariables[av] =
          makeAxisVariable(
              tDatasetID, av, tSourceName, tDestName, tSourceAtt, tAddAtt, sourceAxisValues[av]);
    }

    // if aggregating time index, fix time_coverage_start/end global metadata
    if (timeIndex == 0) {
      EDVTimeGridAxis tga = (EDVTimeGridAxis) axisVariables[0];
      combinedGlobalAttributes.add(
          "time_coverage_start", tga.destinationToString(tga.destinationMinDouble()));
      combinedGlobalAttributes.add(
          "time_coverage_end", tga.destinationToString(tga.destinationMaxDouble()));
    }

    // make the dataVariables[]
    dataVariables = new EDV[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      String tSourceName = sourceDataNames.get(dv);
      String tDestName = (String) tDataVariables[dv][1];
      if (tDestName == null || tDestName.length() == 0) tDestName = tSourceName;
      Attributes tSourceAtt = sourceDataAttributes[dv];
      Attributes tAddAtt = (Attributes) tDataVariables[dv][2];
      // PrimitiveArray taa = tAddAtt.get("_FillValue");
      // String2.log(">>taa " + tSourceName + " _FillValue=" + taa);
      String tSourceType = sourceDataTypes[dv];
      // if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType="
      // + tSourceType);

      if (tDestName.equals(EDV.TIME_NAME))
        throw new RuntimeException(
            errorInMethod + "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
      else if (EDVTime.hasTimeUnits(tSourceAtt, tAddAtt))
        dataVariables[dv] =
            new EDVTimeStamp(datasetID, tSourceName, tDestName, tSourceAtt, tAddAtt, tSourceType);
      else
        dataVariables[dv] =
            new EDV(
                datasetID,
                tSourceName,
                tDestName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
      dataVariables[dv].setActualRangeFromDestinationMinMax();
    }

    // ensure the setup is valid
    ensureValid();

    // if cacheFromUrl is remote ERDDAP /files/, subscribe to the dataset
    // This is like code in EDDTableFromFiles but "/griddap/"
    if (!doQuickRestart
        && EDStatic.subscribeToRemoteErddapDataset
        && cacheFromUrl != null
        && cacheFromUrl.startsWith("http")
        && cacheFromUrl.indexOf("/erddap/files/") > 0) {

      // convert cacheFromUrl from .../files/datasetID/... url into .../griddap/datasetID
      int po1 = cacheFromUrl.indexOf("/erddap/files/");
      int po2 = cacheFromUrl.indexOf('/', po1 + 14); // next / in cacheFromUrl
      if (po2 < 0) po2 = cacheFromUrl.length();
      String remoteUrl =
          cacheFromUrl.substring(0, po1)
              + "/erddap/griddap/"
              + cacheFromUrl.substring(po1 + 14, po2); // datasetID
      tryToSubscribeToRemoteErddapDataset(true, remoteUrl); // logs errors. Won't throw exception.
    }

    // dirTable and fileTable InMemory?
    if (!fileTableInMemory) {
      dirTable = null;
      fileTable = null;
    }

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDGridFromFiles "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");

    // very last thing: saveDimensionValuesInFile
    if (!dimensionValuesInMemory) saveDimensionValuesInFile();
  }

  /**
   * This ensures that ftMin and ftMax don't overlap, recalculates ftStartIndex, and creates the
   * cumulative sourceAxisValues[0].
   *
   * @param sourceAxisValues0 must be of the correct type
   * @throws RuntimeException if trouble
   */
  protected void updateValidateFileTable(
      StringArray dirList,
      ShortArray ftDirIndex,
      StringArray ftFileList,
      DoubleArray ftMin,
      DoubleArray ftMax,
      IntArray ftStartIndex,
      IntArray ftNValues,
      StringArray ftCsvValues,
      PrimitiveArray sourceAxisValues0) {

    int nFiles = ftDirIndex.size();
    if (nFiles == 0) throw new RuntimeException("No valid data files were found.");
    for (int f = 1; f < nFiles; f++) { // 1 since looking backward
      // min max overlap?
      if (ftMax.get(f - 1) > ftMin.get(f))
        throw new RuntimeException(
            "Outer axis overlap between files.\n"
                + "max="
                + ftMax.get(f - 1)
                + " for "
                + dirList.get(ftDirIndex.get(f - 1))
                + ftFileList.get(f - 1)
                + "\n"
                + "is greater than\n"
                + "min="
                + ftMin.get(f)
                + " for "
                + dirList.get(ftDirIndex.get(f))
                + ftFileList.get(f)
                + "\n");
    }

    int tStart = 0;
    sourceAxisValues0.clear();
    for (int f = 0; f < nFiles; f++) {
      // startIndex
      ftStartIndex.set(f, tStart);
      tStart += ftNValues.get(f);

      // sourceAxisValues
      StringArray sa = StringArray.fromCSV(ftCsvValues.get(f));
      if (sa.size() != ftNValues.get(f))
        throw new RuntimeException(
            "Data source error: Observed nCsvValues="
                + sa.size()
                + " != expected="
                + ftNValues.get(f)
                + "\nfor file #"
                + f
                + "="
                + dirList.get(ftDirIndex.get(f))
                + ftFileList.get(f)
                + "\ncsv="
                + ftCsvValues.get(f));

      sourceAxisValues0.append(sa);
    }
    // String2.log("\n>>> sourceAxisValues sav0=" + sourceAxisValues0.toString() + "\n");
  }

  /**
   * This is used by the constructor and lowUpdate to: ensure the incoming values are valid, and if
   * so: compare the incoming values with expected to ensure compatible, or set expected if not
   * already set.
   *
   * @param dirName
   * @param fileName
   * @param tSourceGlobalAttributes from the new file
   * @param tSourceAxisAttributes from the new file
   * @param tSourceAxisValues from the new file
   * @param tSourceDataAttributes from the new file
   * @throws RuntimeException if not compatible
   */
  protected void validateCompareSet(
      String dirName,
      String fileName,
      Attributes tSourceGlobalAttributes,
      Attributes tSourceAxisAttributes[],
      PrimitiveArray tSourceAxisValues[],
      Attributes tSourceDataAttributes[]) {

    String emsg1 = "For " + dirName + fileName + ", the observed and expected values of ";

    // test axis values
    // test if ascending or descending
    String ascError = tSourceAxisValues[0].isAscending();
    if (ascError.length() > 0) {
      String desError = tSourceAxisValues[0].isDescending();
      if (desError.length() > 0)
        throw new RuntimeException(
            "AxisVariable="
                + sourceAxisNames.get(0)
                + "\nisn't ascending sorted ("
                + ascError
                + ")\n"
                + "or descending sorted ("
                + desError
                + ").");
    }

    // test for ties
    StringBuilder sb = new StringBuilder();
    if (tSourceAxisValues[0].removeDuplicates(false, sb) > 0)
      throw new RuntimeException(
          "AxisVariable=" + sourceAxisNames.get(0) + " has tied values:\n" + sb.toString());

    if (haveValidSourceInfo) {
      // compare incoming to expected

      // compare globalAttributes
      // currently no tests

      // compare sourceAxisValues[1..]
      int nav = sourceAxisAttributes.length;
      for (int av = 1; av < nav; av++) {
        // be less strict?
        PrimitiveArray exp = sourceAxisValues[av];
        PrimitiveArray obs = tSourceAxisValues[av];
        String eqError = obs.almostEqual(exp, matchAxisNDigits);
        if (eqError.length() > 0)
          throw new RuntimeException(
              "axis["
                  + av
                  + "]="
                  + sourceAxisNames.get(av)
                  + " is different than expected (matchAxisNDigits="
                  + matchAxisNDigits
                  + "):\n"
                  + eqError);
      }

      // compare axisAttributes
      for (int avi = 0; avi < nav; avi++) {
        Attributes tsaAtt = tSourceAxisAttributes[avi];
        Attributes saAtt = sourceAxisAttributes[avi];
        String emsg2 = " for sourceName=" + sourceAxisNames.get(avi) + " are different.";
        double d1, d2;

        d1 = tsaAtt.getDouble("add_offset");
        d2 = saAtt.getDouble("add_offset");
        Test.ensureEqual(
            Double.isNaN(d1) ? 0 : d1, Double.isNaN(d2) ? 0 : d2, emsg1 + "add_offset" + emsg2);

        d1 = tsaAtt.getDouble("scale_factor");
        d2 = saAtt.getDouble("scale_factor");
        Test.ensureEqual(
            Double.isNaN(d1) ? 1 : d1, Double.isNaN(d2) ? 1 : d2, emsg1 + "scale_factor" + emsg2);

        Test.ensureEqual(
            tsaAtt.getDouble("_FillValue"),
            saAtt.getDouble("_FillValue"),
            emsg1 + "_FillValue" + emsg2);

        Test.ensureEqual(
            tsaAtt.getDouble("missing_value"),
            saAtt.getDouble("missing_value"),
            emsg1 + "missing_value" + emsg2);

        String observedUnits = tsaAtt.getString("units");
        String expectedUnits = saAtt.getString("units");
        if (!Units2.udunitsAreEquivalent(observedUnits, expectedUnits))
          Test.ensureEqual(observedUnits, expectedUnits, emsg1 + "units" + emsg2);
      }

      // compare sourceDataAttributes
      int ndv = sourceDataAttributes.length;
      for (int dvi = 0; dvi < ndv; dvi++) {
        Attributes tsdAtt = tSourceDataAttributes[dvi];
        Attributes sdAtt = sourceDataAttributes[dvi];
        String emsg2 = " for sourceName=" + sourceDataNames.get(dvi) + " are different.";

        // Deal with obs or exp file not having a variable,
        //  but I still want to allow this file in collection.
        // A not perfect solution: if 0 attributes, assume no var in file.
        //  So copy atts from other.
        //  This way, a file missing a var can be first or subsequent file read.
        int ntsdAtt = tsdAtt.size();
        int nsdAtt = sdAtt.size();
        if (ntsdAtt == 0 && nsdAtt > 0) tsdAtt.set(sdAtt);
        else if (nsdAtt == 0 && ntsdAtt > 0) sdAtt.set(tsdAtt);

        Test.ensureEqual(
            tsdAtt.getDouble("add_offset"),
            sdAtt.getDouble("add_offset"),
            emsg1 + "add_offset" + emsg2);
        Test.ensureEqual(
            tsdAtt.getDouble("_FillValue"),
            sdAtt.getDouble("_FillValue"),
            emsg1 + "_FillValue" + emsg2);
        Test.ensureEqual(
            tsdAtt.getDouble("missing_value"),
            sdAtt.getDouble("missing_value"),
            emsg1 + "missing_value" + emsg2);
        Test.ensureEqual(
            tsdAtt.getDouble("scale_factor"),
            sdAtt.getDouble("scale_factor"),
            emsg1 + "scale_factor" + emsg2);
        String observedUnits = tsdAtt.getString("units");
        String expectedUnits = sdAtt.getString("units");
        if (!Units2.udunitsAreEquivalent(observedUnits, expectedUnits))
          Test.ensureEqual(observedUnits, expectedUnits, emsg1 + "units" + emsg2);
      }
    }

    // it passed!
    // set instance info if not already set
    if (!haveValidSourceInfo) {
      sourceGlobalAttributes = tSourceGlobalAttributes;
      sourceAxisAttributes = tSourceAxisAttributes;
      sourceAxisValues = tSourceAxisValues;
      sourceDataAttributes = tSourceDataAttributes;
      // String2.log("sourceAxisValues=" + sourceAxisValues);
      haveValidSourceInfo = true;
    }
  }

  @Override
  public void doReload() {
    requestReloadASAP();
  }

  @Override
  public void handleUpdates(StringArray contexts) throws Throwable {
    handleEventContexts(contexts, "update(" + datasetID + "): ");
  }

  private boolean handleEventContexts(StringArray contexts, String msg) throws Throwable {
    // Don't try to sort out multiple events or event order, just note which files
    // changed.
    long startLowUpdate = System.currentTimeMillis();
    contexts.sort();
    contexts.removeDuplicates();
    int nEvents = contexts.size();

    // remove events for files that don't match fileNameRegex or pathRegex
    BitSet keep = new BitSet(nEvents); // initially all false
    for (int evi = 0; evi < nEvents; evi++) {
      String fullName = contexts.get(evi);
      String dirName = File2.getDirectory(fullName);
      String fileName = File2.getNameAndExtension(fullName);

      // if not a directory and fileName matches fileNameRegex, keep it
      if (fileName.length() > 0
          && fileName.matches(fileNameRegex)
          && (!recursive || dirName.matches(pathRegex))) keep.set(evi);
    }
    contexts.justKeep(keep);
    nEvents = contexts.size();
    if (nEvents == 0) {
      if (verbose)
        String2.log(
            msg + "found 0 events related to files matching fileNameRegex+recursive+pathRegex.");
      return false; // no changes
    }

    // If too many events, call for reload.
    // This method isn't as nearly as efficient as full reload.
    if (nEvents > EDStatic.updateMaxEvents) {
      if (verbose)
        String2.log(
            msg
                + nEvents
                + ">"
                + EDStatic.updateMaxEvents
                + " file events, so I called requestReloadASAP() instead of making changes here.");
      requestReloadASAP();
      return false;
    }

    // get BadFile and FileTable info and make local copies
    ConcurrentHashMap badFileMap = readBadFileMap(); // already a copy of what's in file
    Table tDirTable = getDirTableCopy(); // not null, throws Throwable
    Table tFileTable = getFileTableCopy(); // not null, throws Throwable
    if (debugMode)
      String2.log(
          msg
              + "\n"
              + tDirTable.nRows()
              + " rows in old dirTable.  first 5 rows=\n"
              + tDirTable.dataToString(5)
              + tFileTable.nRows()
              + " rows in old fileTable.  first 5 rows=\n"
              + tFileTable.dataToString(5));

    StringArray dirList = (StringArray) tDirTable.getColumn(0);
    ShortArray ftDirIndex = (ShortArray) tFileTable.getColumn(FT_DIR_INDEX_COL);
    StringArray ftFileList = (StringArray) tFileTable.getColumn(FT_FILE_LIST_COL);
    LongArray ftLastMod = (LongArray) tFileTable.getColumn(FT_LAST_MOD_COL);
    LongArray ftSize = (LongArray) tFileTable.getColumn(FT_SIZE_COL);
    IntArray ftNValues = (IntArray) tFileTable.getColumn(FT_N_VALUES_COL);
    DoubleArray ftMin = (DoubleArray) tFileTable.getColumn(FT_MIN_COL); // sorted by
    DoubleArray ftMax = (DoubleArray) tFileTable.getColumn(FT_MAX_COL);
    StringArray ftCsvValues = (StringArray) tFileTable.getColumn(FT_CSV_VALUES_COL);
    IntArray ftStartIndex = (IntArray) tFileTable.getColumn(FT_START_INDEX_COL);

    // for each changed file
    int nChanges = 0; // BadFiles or FileTable
    int nav = sourceAxisAttributes.length;
    int ndv = sourceDataAttributes.length;
    for (int evi = 0; evi < nEvents; evi++) {
      if (Thread.currentThread().isInterrupted())
        throw new SimpleException("EDDGridFromFiles.lowUpdate" + EDStatic.caughtInterruptedAr[0]);

      String fullName = contexts.get(evi);
      String dirName = File2.getDirectory(fullName);
      String fileName = File2.getNameAndExtension(fullName); // matched to fileNameRegex above

      // dirIndex (dirName may not be in dirList!)
      int dirIndex = dirList.indexOf(dirName); // linear search, but should be short list

      // if it is an existing file, see if it is valid
      if (File2.isFile(fullName)) {
        // test that all axisVariable and dataVariable units are identical
        // this also tests if all dataVariables are present
        PrimitiveArray tSourceAxisValues[] = null;
        Attributes tSourceGlobalAttributes = new Attributes();
        Attributes tSourceAxisAttributes[] = new Attributes[nav];
        Attributes tSourceDataAttributes[] = new Attributes[ndv];
        for (int avi = 0; avi < nav; avi++) tSourceAxisAttributes[avi] = new Attributes();
        for (int dvi = 0; dvi < ndv; dvi++) tSourceDataAttributes[dvi] = new Attributes();
        String reasonBad = null;
        try {
          getSourceMetadata(
              dirName,
              fileName,
              sourceAxisNames,
              sourceDataNames,
              sourceDataTypes,
              tSourceGlobalAttributes,
              tSourceAxisAttributes,
              tSourceDataAttributes);
          tSourceAxisValues =
              getSourceAxisValues(dirName, fileName, sourceAxisNames, sourceDataNames);
          validateCompareSet( // throws Exception (with fileName) if not compatible
              dirName,
              fileName,
              tSourceGlobalAttributes,
              tSourceAxisAttributes,
              tSourceAxisValues,
              tSourceDataAttributes);
        } catch (Exception e) {
          reasonBad = e.getMessage();
        }

        if (reasonBad == null) {
          // File exists and is good/compatible.
          nChanges++;

          // ensure dirIndex is valid
          boolean wasInFileTable = false;
          if (dirIndex < 0) {
            // dir isn't in dirList, so file can't be in BadFileMap or tFileTable.
            // But I do need to add dir to dirList.
            dirIndex = dirList.size();
            dirList.add(dirName);
            if (verbose) String2.log(msg + "added a new dir to dirList (" + dirName + ") and ...");
            // another msg is always for this file printed below
          } else {
            // Remove from BadFileMap if it is present
            if (badFileMap.remove(dirIndex + "/" + fileName) != null) {
              // It was in badFileMap
              if (verbose)
                String2.log(
                    msg + "removed from badFileMap a file that now exists and is valid, and ...");
              // another msg is always for this file printed below
            }

            // If file name already in tFileTable, remove it.
            // Don't take shortcut, e.g., by searching with tMin.
            // It is possible file had wrong name/wrong value before.
            wasInFileTable =
                removeFromFileTable(dirIndex, fileName, tFileTable, ftDirIndex, ftFileList);
          }

          // Insert row in tFileTable for this valid file.
          // Use exact binary search. AlmostEquals isn't a problem.
          // If file was in tFileTable, it is gone now (above).
          int fileListPo =
              ftMin.binaryFindFirstGE(0, ftMin.size() - 1, new PAOne(tSourceAxisValues[0], 0));
          if (verbose)
            String2.log(
                msg
                    + (wasInFileTable ? "updated a file in" : "added a file to")
                    + " fileTable:\n  "
                    + fullName);
          tFileTable.insertBlankRow(fileListPo);
          int tnValues = tSourceAxisValues[0].size();
          ftDirIndex.setInt(fileListPo, dirIndex);
          ftFileList.set(fileListPo, fileName);
          ftLastMod.set(fileListPo, File2.getLastModified(fullName));
          ftSize.set(fileListPo, File2.length(fullName));
          ftNValues.set(fileListPo, tnValues);
          ftMin.set(fileListPo, tSourceAxisValues[0].getNiceDouble(0));
          ftMax.set(fileListPo, tSourceAxisValues[0].getNiceDouble(tnValues - 1));
          ftCsvValues.set(fileListPo, tSourceAxisValues[0].toString());
          // ftStartIndex is updated when file is saved

        } else {
          // File exists and is bad.

          // Remove from tFileTable if it is there.
          if (dirIndex >= 0) { // it might be in tFileTable
            if (removeFromFileTable(dirIndex, fileName, tFileTable, ftDirIndex, ftFileList)) {
              nChanges++;
              if (verbose)
                String2.log(
                    msg
                        + "removed from fileTable a file that is now bad/incompatible:\n  "
                        + fullName
                        + "\n  "
                        + reasonBad);
            } else {
              if (verbose)
                String2.log(
                    msg
                        + "found a bad file (but it wasn't in fileTable):\n  "
                        + fullName
                        + "\n  "
                        + reasonBad);
            }
          }

          // add to badFileMap
          // No don't. Perhaps file is half written.
          // Let main reload be the system to addBadFile
        }
      } else if (dirIndex >= 0) {
        // File now doesn't exist, but it might be in badFile or tFileTable.

        // Remove from badFileMap if it's there.
        if (badFileMap.remove(dirIndex + "/" + fileName) != null) {
          // Yes, it was in badFileMap
          nChanges++;
          if (verbose)
            String2.log(msg + "removed from badFileMap a now non-existent file:\n  " + fullName);
        } else {
          // If it wasn't in badFileMap, it might be in tFileTable.
          // Remove it from tFileTable if it's there.
          // Don't take shortcut, e.g., binary search with tMin.
          // It is possible file had wrong name/wrong value before.
          if (removeFromFileTable(dirIndex, fileName, tFileTable, ftDirIndex, ftFileList)) {
            nChanges++;
            if (verbose)
              String2.log(
                  msg + "removed from fileTable a file that now doesn't exist:\n  " + fullName);
          } else {
            if (verbose)
              String2.log(
                  msg
                      + "a file that now doesn't exist wasn't in badFileMap or fileTable(!):\n  "
                      + fullName);
          }
        }
      } // else file doesn't exist and dir is not in dirList
      // so file can't be in badFileMap or tFileTable
      // so nothing needs to be done.
    }

    // if changes observed, make the changes to the dataset (as fast/atomically as
    // possible)
    if (nChanges > 0) {
      // first, change local info only
      // finish up, validate, and save dirTable, fileTable, badFileMap
      PrimitiveArray sourceAxisValues0 =
          PrimitiveArray.factory(
              axisVariables[0].sourceValues().elementType(),
              ftDirIndex.size(), // size is a guess: the minimum possible, but usually correct
              false); // not active
      updateValidateFileTable(
          dirList,
          ftDirIndex,
          ftFileList,
          ftMin,
          ftMax,
          ftStartIndex,
          ftNValues,
          ftCsvValues,
          sourceAxisValues0); // sourceAxisValues0 is filled

      // then, change secondary parts of instance variables
      // update axisVariables[0] (atomic: make new axisVar, then swap into place)
      EDVGridAxis av0 = axisVariables[0];
      axisVariables[0] =
          makeAxisVariable(
              datasetID,
              0,
              av0.sourceName(),
              av0.destinationName(),
              av0.sourceAttributes(),
              av0.addAttributes(),
              sourceAxisValues0);
      av0 = axisVariables[0]; // the new one
      // EDDTable tests for LLAT, but here/EDDGrid only time is likely to be outer
      // dimension and change
      if (av0.destinationName().equals(EDV.TIME_NAME)) {
        combinedGlobalAttributes().set("time_coverage_start", av0.destinationMinString());
        combinedGlobalAttributes().set("time_coverage_end", av0.destinationMaxString());
      }

      // finally: make the important instance changes that use the changes above
      // (eg fileTable leads to seeing changed axisVariables[0])
      saveDirTableFileTableBadFiles(-1, tDirTable, tFileTable, badFileMap); // throws Throwable
      if (fileTableInMemory) {
        // quickly swap into place
        dirTable = tDirTable;
        fileTable = tFileTable;
      }

      // after changes all in place
      // Currently, update() doesn't trigger these changes.
      // The problem is that some datasets might update every second, others every
      // day.
      // Even if they are done, perhaps do them in ERDDAP ((low)update return
      // changes?)
      // ?update rss?
      // ?subscription and onchange actions?

    }

    if (verbose)
      String2.log(
          msg
              + "succeeded. "
              + Calendar2.getCurrentISODateTimeStringLocalTZ()
              + " nFileEvents="
              + nEvents
              + " nChangesMade="
              + nChanges
              + " time="
              + (System.currentTimeMillis() - startLowUpdate)
              + "ms");
    return nChanges > 0;
  }

  /**
   * This does the actual incremental update of this dataset (i.e., for real time datasets).
   *
   * <p>Concurrency issue: The changes here are first prepared and then applied as quickly as
   * possible (but not atomically!). There is a chance that another thread will get inconsistent
   * information (from some things updated and some things not yet updated). But I don't want to
   * synchronize all activities of this class.
   *
   * @param language the index of the selected language
   * @param msg the start of a log message, e.g., "update(thisDatasetID): ".
   * @param startUpdateMillis the currentTimeMillis at the start of this update.
   * @return true if a change was made
   * @throws Throwable if serious trouble. For simple failures, this writes info to log.txt but
   *     doesn't throw an exception. If the dataset has changed in a serious / incompatible way and
   *     needs a full reload, this throws WaitThenTryAgainException (usually, catcher calls
   *     LoadDatasets.tryToUnload(...) and EDD.requestReloadASAP(tDatasetID)).. If the changes
   *     needed are probably fine but are too extensive to deal with here, this calls
   *     requestReloadASAP() and returns without doing anything.
   */
  @Override
  public boolean lowUpdate(int language, String msg, long startUpdateMillis) throws Throwable {
    if (EDStatic.useSharedWatchService) {
      SharedWatchService.processEvents();
      return false;
    }

    // Most of this lowUpdate code is identical in EDDGridFromFiles and EDDTableFromFiles
    if (watchDirectory == null) return false; // no changes

    // get the file events
    ArrayList<WatchEvent.Kind> eventKinds = new ArrayList();
    StringArray contexts = new StringArray();
    int nEvents = watchDirectory.getEvents(eventKinds, contexts);
    if (nEvents == 0) {
      if (verbose) String2.log(msg + "found 0 events.");
      return false; // no changes
    }

    // if any OVERFLOW, reload this dataset
    for (int evi = 0; evi < nEvents; evi++) {
      if (eventKinds.get(evi) == WatchDirectory.OVERFLOW) {
        if (verbose)
          String2.log(
              msg
                  + "caught OVERFLOW event in "
                  + contexts.get(evi)
                  + ", so I called requestReloadASAP() instead of making changes here.");
        requestReloadASAP();
        return false;
      }
    }

    return handleEventContexts(contexts, msg);
  }

  /**
   * This is the default implementation of getFileInfo, which gets file info from a locally
   * accessible directory. This is called in the middle of the constructor. Some subclasses
   * overwrite this.
   *
   * @param recursive true if the file search should also search subdirectories
   * @return a table with columns with DIR, NAME, LASTMOD, and SIZE columns;
   * @throws Throwable if trouble
   */
  public Table getFileInfo(
      String fileDir, String fileNameRegex, boolean recursive, String pathRegex) throws Throwable {
    // String2.log("EDDTableFromFiles getFileInfo");

    boolean includeDirectories =
        (fileNameRegex != null && fileNameRegex.contains("zarr"))
            || (pathRegex != null && pathRegex.contains("zarr"));

    // if temporary cache system active, make it look like all remote files are in local dir
    if (cacheFromUrl != null && cacheMaxSizeB > 0) {
      Table table =
          FileVisitorDNLS.oneStepCache(
              cacheFromUrl, // throws IOException
              fileDir,
              fileNameRegex,
              recursive,
              pathRegex,
              includeDirectories); // dirsToo
      if (table.nRows() == 0) throw new Exception("No matching files at " + cacheFromUrl);
      return table;
    }

    return FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
        fileDir, fileNameRegex, recursive, pathRegex, includeDirectories); // dirsToo
  }

  /**
   * This gets the dirTable (perhaps the private copy) for read-only use.
   *
   * @throw Throwable if trouble
   */
  @Override
  public Table getDirTable() throws Throwable {
    Table tDirTable =
        fileTableInMemory
            ? dirTable
            : tryToLoadDirFileTable(datasetDir() + DIR_TABLE_FILENAME); // may be null
    Test.ensureNotNull(tDirTable, "dirTable");
    return tDirTable;
  }

  /**
   * This gets the fileTable (perhaps the private copy) for read-only use.
   *
   * @throw Throwable if trouble
   */
  @Override
  public Table getFileTable() throws Throwable {
    Table tFileTable =
        fileTableInMemory
            ? fileTable
            : tryToLoadDirFileTable(datasetDir() + FILE_TABLE_FILENAME); // may be null
    Test.ensureNotNull(tFileTable, "fileTable");
    return tFileTable;
  }

  /**
   * This gets a copy of the dirTable (not the private copy) for read/write use.
   *
   * @throw Throwable if trouble
   */
  public Table getDirTableCopy() throws Throwable {
    Table tDirTable =
        fileTableInMemory
            ? (Table) dirTable.clone()
            : tryToLoadDirFileTable(datasetDir() + DIR_TABLE_FILENAME); // may be null
    Test.ensureNotNull(tDirTable, "dirTable");
    return tDirTable;
  }

  /**
   * This gets a copy of the fileTable (not the private copy) for read/write use.
   *
   * @throw Throwalbe if trouble
   */
  public Table getFileTableCopy() throws Throwable {
    Table tFileTable =
        fileTableInMemory
            ? (Table) fileTable.clone()
            : tryToLoadDirFileTable(datasetDir() + FILE_TABLE_FILENAME); // may be null
    Test.ensureNotNull(tFileTable, "fileTable");
    return tFileTable;
  }

  /**
   * Try to load the dirTable or fileTable.
   *
   * @param fileName datasetDir() + DIR_TABLE_FILENAME or FILE_TABLE_FILENAME
   * @return the dirTable fileTable (null if minor trouble)
   * @throws Throwable if serious trouble (e.g., Too many open files, out of memory)
   */
  protected Table tryToLoadDirFileTable(String fileName) throws Throwable {
    try {
      if (File2.isFile(fileName)) {
        Table table = new Table();
        //  table.readFlatNc(fileName, null, 0); //standardizeWhat=0
        Test.ensureEqual(
            table.readEnhancedFlatNc(fileName, null), // it logs fileName and nRows=
            Table.ENHANCED_VERSION,
            "old/unsupported enhancedVersion");
        Test.ensureEqual(
            table.globalAttributes().getInt(_dirFileTableVersion_),
            DIR_FILE_TABLE_VERSION,
            "old/unsupported " + _dirFileTableVersion_);
        return table;

      } else {
        if (verbose) String2.log("dir/file table doesn't exist: " + fileName);
        return null;
      }
    } catch (Throwable t) {
      String msg = MustBe.throwableToString(t);
      String2.log(String2.ERROR + " reading dir/file table " + fileName + "\n" + msg);

      // serious problem?
      if (Thread.currentThread().isInterrupted()
          || t instanceof InterruptedException
          || msg.indexOf(Math2.TooManyOpenFiles) >= 0
          || msg.toLowerCase().indexOf(Math2.memory) >= 0) throw t;

      // if minor problem
      File2.delete(datasetDir() + DIR_TABLE_FILENAME);
      File2.delete(datasetDir() + FILE_TABLE_FILENAME);
      return null;
    }
  }

  /**
   * This returns a fileTable with valid files (or null if unavailable or any trouble). This is a
   * copy of any internal data, so client can modify the contents.
   *
   * @param language the index of the selected language
   * @param nextPath is the partial path (with trailing slash) to be appended onto the local fileDir
   *     (or wherever files are, even url).
   * @return null if trouble, or Object[3] where [0] is a sorted table with file "Name" (String),
   *     "Last modified" (long millis), "Size" (long), and "Description" (String, but usually no
   *     content), [1] is a sorted String[] with the short names of directories that are 1 level
   *     lower, and [2] is the local directory corresponding to this (or null, if not a local dir).
   */
  @Override
  public Object[] accessibleViaFilesFileTable(int language, String nextPath) {
    if (!accessibleViaFiles) return null;
    try {
      // get a copy of the source file information
      Table tDirTable = getDirTableCopy(); // not null, throws Throwable
      Table tFileTable = getFileTableCopy(); // not null, throws Throwable

      // make the results Table
      Table dnlsTable = FileVisitorDNLS.makeEmptyTable();
      dnlsTable.setColumn(0, tFileTable.getColumn(FT_DIR_INDEX_COL));
      dnlsTable.setColumn(1, tFileTable.getColumn(FT_FILE_LIST_COL));
      dnlsTable.setColumn(2, tFileTable.getColumn(FT_LAST_MOD_COL));
      dnlsTable.setColumn(3, tFileTable.getColumn(FT_SIZE_COL));
      // convert dir Index to dir names
      tDirTable.addColumn(0, "dirIndex", new IntArray(0, tDirTable.nRows() - 1));
      dnlsTable.join(1, 0, "", tDirTable);
      dnlsTable.removeColumn(0);
      dnlsTable.setColumnName(0, FileVisitorDNLS.DIRECTORY);

      // remove files other than fileDir+nextPath and generate array of immediate subDir names
      String subDirs[] = FileVisitorDNLS.reduceDnlsTableToOneDir(dnlsTable, fileDir + nextPath);
      accessibleViaFilesMakeReadyForUser(dnlsTable);
      return new Object[] {dnlsTable, subDirs, fileDir + nextPath};

    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      return null;
    }
  }

  /**
   * This converts a relativeFileName into a full localFileName (which may be a url).
   *
   * @param language the index of the selected language
   * @param relativeFileName (for most EDDTypes, just offset by fileDir)
   * @return full localFileName or null if any error (including, file isn't in list of valid files
   *     for this dataset)
   */
  @Override
  public String accessibleViaFilesGetLocal(int language, String relativeFileName) {
    // identical code in EDDGridFromFiles and EDDTableFromFiles
    if (!accessibleViaFiles) return null;
    String msg = datasetID() + " accessibleViaFilesGetLocal(" + relativeFileName + "): ";

    try {
      String fullName = fileDir + relativeFileName;
      String localDir = File2.getDirectory(fullName);
      String nameAndExt = File2.getNameAndExtension(fullName);

      // ensure that fullName is in file list

      // get dir index
      Table dirTable = getDirTable(); // no need to get copy since not changing it
      Table fileTable = getFileTable(); // no need to get copy since not changing it
      PrimitiveArray dirNames = dirTable.getColumn(0); // the only column
      int dirIndex = FileVisitorDNLS.indexOfDirectory(dirNames, localDir);
      if (dirIndex < 0) {
        String2.log(msg + "localDir=" + localDir + " not in dirTable.");
        return null;
      }

      // get file index
      ShortArray dirIndexCol = (ShortArray) fileTable.getColumn(FT_DIR_INDEX_COL);
      StringArray fileNameCol = (StringArray) fileTable.getColumn(FT_FILE_LIST_COL);
      int n = dirIndexCol.size();
      for (int i = 0; i < n; i++) {
        if (dirIndexCol.get(i) == dirIndex && fileNameCol.get(i).equals(nameAndExt))
          return fullName; // it's a valid file in the fileTable
      }
      String2.log(msg + "fullName=" + localDir + " not in dirTable+fileTable.");
      return null;
    } catch (Throwable t) {
      String2.log(msg + "\n" + MustBe.throwableToString(t));
      return null;
    }
  }

  /**
   * If using temporary cache system, this ensure file is in cache or throws Exception.
   *
   * @throws Exception if trouble
   */
  void ensureInCache(String localFullName) throws Exception {
    if (cacheFromUrl != null && cacheMaxSizeB > 0) { // cache system is active
      // If desired file is in cache, we're done.
      if (RegexFilenameFilter.touchFileAndRelated(
          localFullName)) // returns true if localFullName exists
      return;

      // Normally this does nothing and takes ~0 time.
      // When it does something, it takes time, so it's safer to prune first
      //  (if needed) then download (so as not to download a file, then prune it)
      //  even though new file may put it over the threshold.
      FileVisitorDNLS.pruneCache(fileDir, cacheMaxSizeB, cacheFraction);
      // then ensureInCache
      FileVisitorDNLS.ensureInCache( // it sets lastMod to 'now'
          cacheFromUrl, fileDir, localFullName); // throws Exception
    }
  }

  /**
   * This gets sourceGlobalAttributes and sourceDataAttributes from the specified source file (or
   * does nothing if that isn't possible). This is a high-level request that handles axis
   * manipulation.
   *
   * @param tFileDir
   * @param tFileName
   * @param sourceAxisNames If there is a special axis0, this will still be the full list.
   * @param sourceDataNames the names of the desired source data columns.
   * @param sourceDataTypes the data types of the desired source columns (e.g., "String" or "float")
   * @param sourceGlobalAttributes should be an empty Attributes. It will be populated by this
   *     method
   * @param sourceAxisAttributes should be an array of empty Attributes. It will be populated by
   *     this method
   * @param sourceDataAttributes should be an array of empty Attributes. It will be populated by
   *     this method
   * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or sourceDataName not
   *     found). If there is trouble, this doesn't call addBadFile or requestReloadASAP().
   */
  public void getSourceMetadata(
      String tFileDir,
      String tFileName,
      StringArray tSourceAxisNames,
      StringArray tSourceDataNames,
      String tSourceDataTypes[],
      Attributes tSourceGlobalAttributes,
      Attributes tSourceAxisAttributes[],
      Attributes tSourceDataAttributes[])
      throws Throwable {

    // ordinary case
    if (axis0Type == AXIS0_REGULAR) {

      // if using temporary cache system, ensure file is in cache
      ensureInCache(tFileDir + tFileName); // throws Exception
      String decompFullName =
          FileVisitorDNLS.decompressIfNeeded(
              tFileDir + tFileName,
              fileDir,
              decompressedDirectory(),
              EDStatic.decompressedCacheMaxGB,
              true); // reuseExisting
      lowGetSourceMetadata(
          decompFullName,
          tSourceAxisNames,
          tSourceDataNames,
          tSourceDataTypes,
          tSourceGlobalAttributes,
          tSourceAxisAttributes,
          tSourceDataAttributes);
      return;
    }

    int nAxes = tSourceAxisAttributes.length;
    int nDV = tSourceDataAttributes.length;

    // special case: don't read the file!
    if (matchAxisNDigits == 0
        && (axis0Type == AXIS0_FILENAME
            || axis0Type == AXIS0_PATHNAME
            || axis0Type == AXIS0_REPLACE_FROM_FILENAME)
        && haveValidSourceInfo) {

      if (debugMode)
        String2.log(
            ">> EDDGridFromFiles.getSourceMetadata   just using known info, not reading the file.");
      tSourceGlobalAttributes.set(sourceGlobalAttributes);
      for (int av = 0; av < nAxes; av++) tSourceAxisAttributes[av].set(sourceAxisAttributes[av]);
      for (int dv = 0; dv < nDV; dv++) tSourceDataAttributes[dv].set(sourceDataAttributes[dv]);
      return;
    }

    // special axis0?  ***fileName,            timeFormat=YYYYMMDD, regex, captureGroup
    // special axis0?  ***pathName,            timeFormat=YYYYMMDD, regex, captureGroup
    // special axis0?  ***global:attName,      timeFormat=YYYYMMDD, regex, captureGroup
    // special axis0?  ***replaceFromFileName, timeFormat=YYYYMMDD, regex, captureGroup
    if (axis0Type == AXIS0_FILENAME
        || axis0Type == AXIS0_PATHNAME
        || axis0Type == AXIS0_GLOBAL
        || axis0Type == AXIS0_REPLACE_FROM_FILENAME) {

      Attributes tSAAtts[] = new Attributes[nAxes - 1];
      System.arraycopy(tSourceAxisAttributes, 1, tSAAtts, 0, nAxes - 1);
      // if using temporary cache system, ensure file is in cache
      ensureInCache(tFileDir + tFileName); // throws Exception
      String decompFullName =
          FileVisitorDNLS.decompressIfNeeded(
              tFileDir + tFileName,
              fileDir,
              decompressedDirectory(),
              EDStatic.decompressedCacheMaxGB,
              true); // reuseExisting
      lowGetSourceMetadata(
          decompFullName,
          sourceAxisNamesNoAxis0,
          tSourceDataNames,
          tSourceDataTypes,
          tSourceGlobalAttributes,
          tSAAtts,
          tSourceDataAttributes);
      if (axis0TimeFormat != null) {
        Attributes saa0 = tSourceAxisAttributes[0];
        saa0.set("standard_name", EDV.TIME_STANDARD_NAME);
        saa0.set("units", EDV.TIME_UNITS);
      }
      return;
    }

    throw new RuntimeException("Invalid axis0Type=" + axis0Type);
  }

  /**
   * This is the low-level request corresponding to what is actually in the file.
   *
   * @param tFullName the name of the decompressed data file
   * @param tSourceAxisNames If there is a special axis0, this list will be the instances list[1 ...
   *     n-1].
   */
  public abstract void lowGetSourceMetadata(
      String tFullName,
      StringArray tSourceAxisNames,
      StringArray tSourceDataNames,
      String tSourceDataTypes[],
      Attributes tSourceGlobalAttributes,
      Attributes tSourceAxisAttributes[],
      Attributes tSourceDataAttributes[])
      throws Throwable;

  /**
   * This gets source axis values from one file. This is a high-level request that handles axis
   * manipulation.
   *
   * @param tFileDir
   * @param tFileName
   * @param sourceAxisNames the names of the desired source axis variables. If there is a special
   *     axis0, this will still be the full list.
   * @param sourceDataNames When there are unnamed dimensions, this is to find out the shape of the
   *     variable to make index values 0, 1, size-1.
   * @return a PrimitiveArray[] with the results (with the requested sourceDataTypes). It needn't
   *     set sourceGlobalAttributes or sourceDataAttributes (but see getSourceMetadata).
   * @throws Throwable if trouble (e.g., invalid file). If there is trouble, this doesn't call
   *     addBadFile or requestReloadASAP().
   */
  public PrimitiveArray[] getSourceAxisValues(
      String tFileDir, String tFileName, StringArray sourceAxisNames, StringArray sourceDataNames)
      throws Throwable {

    // common case: nothing special
    if (axis0Type == AXIS0_REGULAR) {
      // if using temporary cache system, ensure file is in cache
      ensureInCache(tFileDir + tFileName); // throws Exception
      String decompFullName =
          FileVisitorDNLS.decompressIfNeeded(
              tFileDir + tFileName,
              fileDir,
              decompressedDirectory(),
              EDStatic.decompressedCacheMaxGB,
              true); // reuseExisting
      return lowGetSourceAxisValues(decompFullName, sourceAxisNames, sourceDataNames);
    }

    // special axis0?  ***fileName,       timeFormat=YYYYMMDD, regex, captureGroup
    // special axis0?  ***pathName,       timeFormat=YYYYMMDD, regex, captureGroup
    // special axis0?  ***global:attName, timeFormat=YYYYMMDD, regex, captureGroup
    if (axis0Type == AXIS0_FILENAME
        || axis0Type == AXIS0_PATHNAME
        || axis0Type == AXIS0_GLOBAL
        || axis0Type == AXIS0_REPLACE_FROM_FILENAME) {

      int nAxes = sourceAxisNames.size();
      PrimitiveArray nsav[] = new PrimitiveArray[nAxes];
      nsav[0] = PrimitiveArray.factory(axis0PAType, 1, false);

      // special case: don't read the file!
      if (matchAxisNDigits == 0
          && (axis0Type == AXIS0_FILENAME
              || axis0Type == AXIS0_PATHNAME
              || axis0Type == AXIS0_REPLACE_FROM_FILENAME)
          && haveValidSourceInfo) {

        if (debugMode)
          String2.log(
              ">> EDDGridFromFiles.getSourceAxisValues just using known info, not reading the file.");
        for (int av = 1; av < nAxes; av++) // [0] created above. Value will be set below.
        nsav[av] = (PrimitiveArray) sourceAxisValues[av].clone(); // no need to clone???

      } else {

        // get the axisValues for dimensions[1+]
        // if using temporary cache system, ensure file is in cache
        ensureInCache(tFileDir + tFileName); // throws Exception
        String decompFullName =
            FileVisitorDNLS.decompressIfNeeded(
                tFileDir + tFileName,
                fileDir,
                decompressedDirectory(),
                EDStatic.decompressedCacheMaxGB,
                true); // reuseExisting
        PrimitiveArray tsav[] =
            lowGetSourceAxisValues(decompFullName, sourceAxisNamesNoAxis0, sourceDataNames);
        System.arraycopy(tsav, 0, nsav, 1, nAxes - 1);
      }

      // get the sourceString
      String sourceString;
      if (axis0Type == AXIS0_FILENAME || axis0Type == AXIS0_REPLACE_FROM_FILENAME) {
        sourceString = tFileName;
      } else if (axis0Type == AXIS0_PATHNAME) {
        sourceString = String2.replaceAll(tFileDir + tFileName, '\\', '/');
      } else {
        Attributes sGlobalAtts = new Attributes();
        ensureInCache(
            tFileDir + tFileName); // throws Exception   //probably already there (from above)
        String decompFullName =
            FileVisitorDNLS.decompressIfNeeded(
                tFileDir + tFileName,
                fileDir,
                decompressedDirectory(),
                EDStatic.decompressedCacheMaxGB,
                true); // reuseExisting
        lowGetSourceMetadata(
            decompFullName,
            new StringArray(), // sourceAxisNames,
            new StringArray(), // sourceDataNames
            new String[0], // sourceDataTypes[],
            sGlobalAtts,
            new Attributes[0],
            new Attributes[0]); // sourceAxisAttributes[], sourceDataAttributes[]
        sourceString = sGlobalAtts.getString(axis0GlobalAttName);
        if (!String2.isSomething(sourceString))
          throw new RuntimeException("globalAttribute " + axis0GlobalAttName + "=" + sourceString);
      }

      // use the sourceString
      Matcher m = axis0RegexPattern.matcher(sourceString);
      if (!m.matches())
        throw new RuntimeException(
            "sourceString=" + sourceString + " doesn't match axis0Regex=" + axis0Regex);
      String dateTimeString = m.group(axis0CaptureGroup);
      nsav[0].addDouble(
          axis0DateTimeFormatter == null
              ? String2.parseDouble(dateTimeString)
              : Calendar2.parseToEpochSeconds(dateTimeString, axis0TimeFormat, axis0TimeZone));
      return nsav;
    }

    throw new RuntimeException("Invalid axis0Type=" + axis0Type);
  }

  /**
   * This is the low-level request corresponding to what is actually in the file.
   *
   * @param tFullName the name of the decompressed data file
   * @param sourceAxisNames If there is a special axis0, this will not include axis0's name.
   * @param sourceDataNames When there are unnamed dimensions, this is to find out the shape of the
   *     variable to make index values 0, 1, size-1.
   */
  public abstract PrimitiveArray[] lowGetSourceAxisValues(
      String tFullName, StringArray sourceAxisNames, StringArray sourceDataNames) throws Throwable;

  /**
   * This gets source data from one file. This is a high-level request that handles axis
   * manipulation.
   *
   * @param tFileDir
   * @param tFileName
   * @param tDataVariables the desired data variables
   * @param tConstraints where the first axis variable's constraints have been customized for this
   *     file.
   * @return a PrimitiveArray[] with an element for each tDataVariable with the dataValues. <br>
   *     The dataValues are straight from the source, not modified. <br>
   *     The primitiveArray dataTypes are usually the sourceDataPAType, but can be any type.
   *     EDDGridFromFiles will convert to the sourceDataPAType. <br>
   *     Note the lack of axisVariable values!
   * @throws Throwable if trouble (e.g., invalid file). If there is trouble, this doesn't call
   *     addBadFile or requestReloadASAP().
   */
  public PrimitiveArray[] getSourceDataFromFile(
      String tFileDir, String tFileName, EDV tDataVariables[], IntArray tConstraints)
      throws Throwable {

    // if using temporary cache system, ensure file is in cache
    ensureInCache(tFileDir + tFileName); // throws Exception
    String decompFullName =
        FileVisitorDNLS.decompressIfNeeded(
            tFileDir + tFileName,
            fileDir,
            decompressedDirectory(),
            EDStatic.decompressedCacheMaxGB,
            true); // reuseExisting

    if (axis0Type == AXIS0_REGULAR || axis0Type == AXIS0_REPLACE_FROM_FILENAME)
      return lowGetSourceDataFromFile(decompFullName, tDataVariables, tConstraints);

    // special axis0?  ***fileName, time=YYYYMMDD, regex, captureGroup
    if (axis0Type == AXIS0_FILENAME || axis0Type == AXIS0_PATHNAME || axis0Type == AXIS0_GLOBAL) {
      return lowGetSourceDataFromFile(
          decompFullName,
          tDataVariables, // start, stride, stop
          (IntArray)
              tConstraints.subset(3, 1, tConstraints.size() - 1)); // remove the axis0 constraints
    }

    throw new RuntimeException("Invalid axis0Type=" + axis0Type);
  }

  /**
   * This is the low-level request corresponding to what is actually in the file.
   *
   * @param tFullName the name of the decompressed data file
   * @param tConstraints For each axis variable, there will be 3 numbers (startIndex, stride,
   *     stopIndex). !!! If there is a special axis0, this will not include constraints for axis0.
   */
  public abstract PrimitiveArray[] lowGetSourceDataFromFile(
      String tFullName, EDV tDataVariables[], IntArray tConstraints) throws Throwable;

  /**
   * This gets data (not yet standardized) from the data source for this EDDGrid. Because this is
   * called by GridDataAccessor, the request won't be the full user's request, but will be a partial
   * request (for less than EDStatic.partialRequestMaxBytes).
   *
   * @param language the index of the selected language
   * @param tDirTable If EDDGridFromFiles, this MAY be the dirTable, else null.
   * @param tFileTable If EDDGridFromFiles, this MAY be the fileTable, else null.
   * @param tDataVariables EDV[] with just the requested data variables
   * @param tConstraints int[nAxisVariables*3] where av*3+0=startIndex, av*3+1=stride,
   *     av*3+2=stopIndex. AxisVariables are counted left to right, e.g., sst[0=time][1=lat][2=lon].
   * @return a PrimitiveArray[] where the first axisVariables.length elements are the axisValues and
   *     the next tDataVariables.length elements are the dataValues (using the sourceDataPAType).
   *     Both the axisValues and dataValues are straight from the source, not modified.
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public PrimitiveArray[] getSourceData(
      int language, Table tDirTable, Table tFileTable, EDV tDataVariables[], IntArray tConstraints)
      throws Throwable {

    // get a local reference to dirTable and fileTable
    try {
      if (tDirTable == null) tDirTable = getDirTable(); // throw exception if trouble
      if (tFileTable == null) tFileTable = getFileTable();
    } catch (Exception e) {
      throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n(Details: unable to read fileTable.)");
    }

    // get the tDirTable and tFileTable PrimitiveArrays
    StringArray dirList = (StringArray) tDirTable.getColumn(0);
    ShortArray ftDirIndex = (ShortArray) tFileTable.getColumn(FT_DIR_INDEX_COL);
    StringArray ftFileList = (StringArray) tFileTable.getColumn(FT_FILE_LIST_COL);
    LongArray ftLastMod = (LongArray) tFileTable.getColumn(FT_LAST_MOD_COL);
    LongArray ftSize = (LongArray) tFileTable.getColumn(FT_SIZE_COL);
    IntArray ftNValues = (IntArray) tFileTable.getColumn(FT_N_VALUES_COL);
    DoubleArray ftMin = (DoubleArray) tFileTable.getColumn(FT_MIN_COL);
    DoubleArray ftMax = (DoubleArray) tFileTable.getColumn(FT_MAX_COL);
    StringArray ftCsvValues = (StringArray) tFileTable.getColumn(FT_CSV_VALUES_COL);
    IntArray ftStartIndex = (IntArray) tFileTable.getColumn(FT_START_INDEX_COL);

    // make results[]
    int nav = axisVariables.length;
    int ndv = tDataVariables.length;
    PrimitiveArray results[] = new PrimitiveArray[nav + ndv];
    for (int avi = 0; avi < nav; avi++)
      results[avi] =
          axisVariables[avi]
              .sourceValues()
              .subset(
                  tConstraints.get(avi * 3 + 0),
                  tConstraints.get(avi * 3 + 1),
                  tConstraints.get(avi * 3 + 2));
    for (int dvi = 0; dvi < ndv; dvi++) {
      // String2.log("!dvi#" + dvi + " " + tDataVariables[dvi].destinationName() + " " +
      // tDataVariables[dvi].sourceDataPAType().toString());
      results[nav + dvi] =
          PrimitiveArray.factory(tDataVariables[dvi].sourceDataPAType(), 64, false);
    }
    IntArray ttConstraints = (IntArray) tConstraints.clone();
    int nFiles = ftStartIndex.size();
    int axis0Start = tConstraints.get(0);
    int axis0Stride = tConstraints.get(1);
    int axis0Stop = tConstraints.get(2);
    int ftRow = 0;

    int tnThreads =
        nThreads >= 1 && nThreads < Integer.MAX_VALUE ? nThreads : EDStatic.nGridThreads;
    // reduce tnThreads based on memory available
    tnThreads = adjustNThreads(tnThreads);
    ThreadedWorkManager<PrimitiveArray[]> workManager =
        new ThreadedWorkManager<>(
            tnThreads,
            result -> {
              // merge dataVariables   (converting to sourceDataPAType if needed)
              for (int dv = 0; dv < ndv; dv++) {
                results[nav + dv].append(result[dv]);
                result[dv].clear();
              }
              // String2.log("!merged tResults[1stDV]=" + results[nav].toString());
            });

    while (axis0Start <= axis0Stop) {
      if (Thread.currentThread().isInterrupted()) {
        if (workManager != null) workManager.forceShutdown();
        throw new SimpleException(
            "EDDGridFromFiles.getDataForDapQuery" + EDStatic.caughtInterruptedAr[0]);
      }

      // find next relevant file
      ftRow = ftStartIndex.binaryFindLastLE(ftRow, nFiles - 1, PAOne.fromInt(axis0Start));
      int tNValues = ftNValues.get(ftRow);
      int tStart = axis0Start - ftStartIndex.get(ftRow);
      int tStop = tStart;
      // get as many axis0 values as possible from this file
      //                    (in this file, if this file had all the remaining values)
      int lookMax = Math.min(tNValues - 1, axis0Stop - ftStartIndex.get(ftRow));
      while (tStop + axis0Stride <= lookMax) tStop += axis0Stride;
      // String2.log("!tStart=" + tStart + " stride=" + axis0Stride + " tStop=" + tStop + "
      // tNValues=" + tNValues);

      // set ttConstraints
      ttConstraints.set(0, tStart);
      ttConstraints.set(2, tStop);
      String tFileDir = dirList.get(ftDirIndex.get(ftRow));
      String tFileName = ftFileList.get(ftRow);
      if (reallyVerbose)
        String2.log(
            "ftRow="
                + ftRow
                + " axis0Start="
                + axis0Start
                + " local="
                + tStart
                + ":"
                + axis0Stride
                + ":"
                + tStop
                + " "
                + tFileDir
                + tFileName);

      workManager.addTask(
          new GetGridFromFileCallable(
              this,
              tFileDir,
              tFileName, // it calls ensureInCache()
              tDataVariables,
              ttConstraints,
              ftDirIndex.get(ftRow),
              ftLastMod.get(ftRow)));

      // set up for next while-iteration
      axis0Start += (tStop - tStart) + axis0Stride;
      ftRow++; // first possible file is next file
    }

    workManager.finishedEnqueing();
    // Make sure all of the work has been processed.
    workManager.processResults();

    return results;
  }

  private static class GetGridFromFileCallable implements Callable<PrimitiveArray[]> {
    private final EDDGridFromFiles caller;
    private final String tFileDir;
    private final String tFileName;
    private final EDV[] tDataVariables;
    private final IntArray tConstraints;
    private final int dirIndex;
    private final long modIndex;

    public GetGridFromFileCallable(
        EDDGridFromFiles caller,
        String tFileDir,
        String tFileName,
        EDV[] tDataVariables,
        IntArray tConstraints,
        int dirIndex,
        long modIndex) {
      this.caller = caller;
      this.tFileDir = tFileDir;
      this.tFileName = tFileName;
      this.tDataVariables = tDataVariables;
      this.tConstraints = tConstraints;
      this.dirIndex = dirIndex;
      this.modIndex = modIndex;
    }

    @Override
    public PrimitiveArray[] call() throws Exception {
      try {
        return caller.getSourceDataFromFile(
            tFileDir,
            tFileName, // it calls ensureInCache()
            tDataVariables,
            tConstraints);
      } catch (Throwable t) {

        // if OutOfMemory or too much data or Too many open files, rethrow t so request fails
        String tToString = t.toString();
        if (Thread.currentThread().isInterrupted()
            || t instanceof InterruptedException
            || t instanceof TimeoutException
            || t instanceof OutOfMemoryError
            || tToString.indexOf(Math2.memoryTooMuchData) >= 0
            || tToString.indexOf(Math2.TooManyOpenFiles) >= 0) throw new ExecutionException(t);

        // sleep and give it one more try
        try {
          Thread.sleep(1000); // not Math2.sleep(1000);
          if (Thread.currentThread().interrupted()) // consume the interrupted status
          throw new InterruptedException();
          return caller.getSourceDataFromFile(tFileDir, tFileName, tDataVariables, tConstraints);
        } catch (Throwable t2) {
          // if OutOfMemory or too much data (or some other reasons), rethrow t so request fails
          String t2String = t2.toString();
          String2.log("caught while reading file=" + tFileDir + tFileName + ": " + t2String);
          if (Thread.currentThread().isInterrupted()
              || t2 instanceof InterruptedException
              || t2 instanceof TimeoutException
              || t2 instanceof OutOfMemoryError
              || EDStatic.isClientAbortException(t)
              || t2String.indexOf(Math2.memoryTooMuchData) >= 0
              || t2String.indexOf(Math2.TooManyOpenFiles) >= 0) throw new ExecutionException(t2);

          // mark the file as bad   and reload the dataset
          caller.addBadFileToTableOnDisk(
              dirIndex, tFileName, modIndex, MustBe.throwableToShortString(t));
          // an exception here will cause data request to fail (as it should)
          String2.log(MustBe.throwableToString(t));
          throw t instanceof WaitThenTryAgainException
              ? new ExecutionException(t)
              : // original exception
              new WaitThenTryAgainException(t);
        }
      }
    }
  }
}
