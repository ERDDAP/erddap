/*
 * EDDTableFromFileNames Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.Script2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.handlers.EDDTableFromFileNamesHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import gov.noaa.pfel.erddap.variable.EDVDepth;
import gov.noaa.pfel.erddap.variable.EDVLat;
import gov.noaa.pfel.erddap.variable.EDVLon;
import gov.noaa.pfel.erddap.variable.EDVTime;
import gov.noaa.pfel.erddap.variable.EDVTimeStamp;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a table of file names.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2015-01-06
 */
@SaxHandlerClass(EDDTableFromFileNamesHandler.class)
public class EDDTableFromFileNames extends EDDTable {

  protected String fileDir; // has forward slashes and trailing slash
  protected final String fileNameRegex;
  protected String pathRegex;
  protected boolean recursive;
  protected String extractRegex[];
  protected byte extractGroup[];
  protected final Map<String, Set<String>> scriptNeedsColumns =
      new HashMap<>(); // <sourceName, otherSourceColumnNames>

  /**
   * from==fromLocalFiles if files are on a local hard drive. 1) A failure when reading a local
   * file, causes file to be marked as bad and dataset reloaded; but a remote failure doesn't. 2)
   * For remote files, the bad file list is rechecked every time dataset is reloaded.
   */
  public static final int fromLocalFiles = 0;

  /**
   * from==fromRemoteFiles if files are on a remote URL (including AWS S3). Info is cached (DNLS
   * info with directoriesToo=false).
   */
  public static final int fromRemoteFiles = 1;

  /** fromFiles indicates the data is coming ***fromFiles provided by admin. */
  public static final int fromFiles = 2;

  /**
   * "files" system will work correctly with on-the-fly info from a remote URL. But
   * stored/searchable "data" will be minimal: just first directory.
   */
  public static final int fromOnTheFly = 3;

  protected int from = -1; // not yet set

  /** This is the system for getting dir info from files. */
  String fromFilesFileType; // currently must be jsonlCSV  ***fromFiles spec[1]

  EDDTableFromFiles fromFilesEDDTable; // the child dataset holding all file dir info

  // standard variable names
  public static final String URL = FileVisitorDNLS.URL; // "url";
  public static final String DIRECTORY = FileVisitorDNLS.DIRECTORY; // "directory";
  public static final String NAME = FileVisitorDNLS.NAME; // "name";
  public static final String LASTMODIFIED = FileVisitorDNLS.LASTMODIFIED; // "lastModified";
  public static final String SIZE = FileVisitorDNLS.SIZE; // "size";

  /**
   * This constructs an EDDTableFromFileNames based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromFileNames"&gt;
   *     having just been read.
   * @return an EDDTableFromFileNames. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromFileNames fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableFromFileNames(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    LocalizedAttributes tGlobalAttributes = null;
    ArrayList<DataVariableInfo> tDataVariables = new ArrayList<>();
    int tReloadEveryNMinutes = Integer.MAX_VALUE;
    // this doesn't support updateEveryNMillis because (unless remote dir, which is cached)
    //  this always gets file info anew for every request.
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    String tFileDir = null;
    String tFileNameRegex = null;
    boolean tRecursive = false;
    String tPathRegex = ".*";
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    String tAddVariablesWhere = null;

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
      switch (localTags) {
        case "<addAttributes>" -> tGlobalAttributes = getAttributesFromXml(xmlReader);
        case "<altitudeMetersPerSourceUnit>" ->
            throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
        case "<dataVariable>" -> tDataVariables.add(getSDADVariableFromXml(xmlReader));
        case "<accessibleTo>",
            "<pathRegex>",
            "<recursive>",
            "<fileNameRegex>",
            "<fileDir>",
            "<addVariablesWhere>",
            "<defaultGraphQuery>",
            "<defaultDataQuery>",
            "<iso19115File>",
            "<fgdcFile>",
            "<onChange>",
            "<reloadEveryNMinutes>",
            "<graphsAccessibleTo>" -> {}
        case "</accessibleTo>" -> tAccessibleTo = content;
        case "</graphsAccessibleTo>" -> tGraphsAccessibleTo = content;
        case "</reloadEveryNMinutes>" -> tReloadEveryNMinutes = String2.parseInt(content);
        case "</onChange>" -> tOnChange.add(content);
        case "</fgdcFile>" -> tFgdcFile = content;
        case "</iso19115File>" -> tIso19115File = content;
        case "</defaultDataQuery>" -> tDefaultDataQuery = content;
        case "</defaultGraphQuery>" -> tDefaultGraphQuery = content;
        case "</addVariablesWhere>" -> tAddVariablesWhere = content;
        case "</fileDir>" -> tFileDir = content;
        case "</fileNameRegex>" -> tFileNameRegex = content;
        case "</recursive>" -> tRecursive = String2.parseBoolean(content);
        case "</pathRegex>" -> tPathRegex = content;
        default -> xmlReader.unexpectedTagException();
      }
    }

    return new EDDTableFromFileNames(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        tFileDir,
        tFileNameRegex,
        tRecursive,
        tPathRegex);
  }

  /**
   * The constructor.
   *
   * <p>Assumptions about what constraints the source can handle: <br>
   * Numeric variables: outer sequence (any operator except regex), inner sequence (no constraints).
   * <br>
   * String variables: <br>
   * outer sequence <br>
   * support for = and != is set by sourceCanConstrainStringEQNE (default=true) <br>
   * support for &lt; &lt;= &gt; &gt;= is set by sourceCanConstrainStringGTLT (default=true), <br>
   * regex support set by sourceCanConstrainStringRegex (default ""), <br>
   * inner sequence (no constraints ever).
   *
   * <p>Yes, lots of detailed information must be supplied here that is sometimes available in
   * metadata. If it is in metadata, make a subclass that extracts info from metadata and calls this
   * constructor.
   *
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
   *     Special case: if combinedGlobalAttributes name="license", any instance of
   *     value="[standard]" will be converted to the EDStatic.messages.standardLicense.
   * @param tDataVariables is an Object[nDataVariables][3]: <br>
   *     [0]=String sourceName (the name of the data variable in the dataset source, without the
   *     outer or inner sequence name), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories). Special case: value="null" causes that item to be removed from
   *     combinedAttributes. <br>
   *     The order of variables you define doesn't have to match the order in the source.
   *     <p>If there is a time variable, either tAddAttributes (read first) or tSourceAttributes
   *     must have "units" which is either
   *     <ul>
   *       <li>a UDUunits string (containing " since ") describing how to interpret source time
   *           values (which should always be numeric since they are a dimension of a grid) (e.g.,
   *           "seconds since 1970-01-01T00:00:00").
   *       <li>a java.time.format.DateTimeFormatter string (which is compatible with
   *           java.text.SimpleDateFormat) describing how to interpret string times (e.g., the
   *           ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   *           or
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html)).
   *     </ul>
   *
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data.
   * @throws Throwable if trouble
   */
  public EDDTableFromFileNames(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      LocalizedAttributes tAddGlobalAttributes,
      List<DataVariableInfo> tDataVariables,
      int tReloadEveryNMinutes,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromFileNames " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromFileNames(" + tDatasetID + ") constructor:\n";
    int language = 0;

    // save some of the parameters
    className = "EDDTableFromFileNames";
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
    PrimitiveArray pa = addGlobalAttributes.get(language, "sourceUrl");
    if (pa == null) {
      addGlobalAttributes.set(language, "sourceUrl", "(local files)");
    }
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    fileDir = tFileDir;
    fileNameRegex = tFileNameRegex;
    accessibleViaFiles =
        EDStatic.config.filesActive; // default for this dataset is 'true' and not changeable

    if (!String2.isSomething(fileDir))
      throw new IllegalArgumentException(errorInMethod + "fileDir wasn't specified.");

    // set 'from': where's the info coming from?
    from =
        fileDir.startsWith("***fromOnTheFly,")
            ? fromOnTheFly
            : fileDir.startsWith("***fromFiles,")
                ? fromFiles
                : String2.isRemote(fileDir) ? fromRemoteFiles : fromLocalFiles;

    // handle fileDir=***fromOnTheFly,fileDir(usually a URL)
    if (from == fromOnTheFly) {
      String parts[] = parseFromOnTheFly(fileDir);
      fileDir = parts[1];
    }

    // handle
    // fileDir=***fromFiles,fromFilesFileType,fromFilesFileDir,fromFilesFileNameRegex,fromFilesRealDir
    // assumes filesRecursive=true, filesPathRegex=".*
    if (from == fromFiles) {
      String parts[] = parseFromFiles(fileDir); // it checks that fromFilesFileType is valid
      fromFilesFileType = parts[1]; // currently, only jsonlCSV is valid
      // fromFilesFileDir = parts[2]; // baseDir of local jsonlCSV files with fileNames
      fileDir = parts[4]; // dir of referenced files, AKA fromFilesRealDir

      if (fromFilesFileType.equals("jsonlCSV")) {
        fromFilesEDDTable =
            new EDDTableFromJsonlCSVFiles(
                datasetID + "_child",
                null,
                null, // tDatasetID, tAccessibleTo, tGraphsAccessibleTo,
                new StringArray(),
                null,
                null, // tOnChange, tFgdcFile, tIso19115File,
                null, // tSosOfferingPrefix,
                null,
                null, // String tDefaultDataQuery, String tDefaultGraphQuery,
                new LocalizedAttributes() // LocalizedAttributes tAddGlobalAttributes,
                    .set(EDMessages.DEFAULT_LANGUAGE, "cdm_data_type", "other")
                    .set(
                        EDMessages.DEFAULT_LANGUAGE,
                        "infoUrl",
                        "https://erddap.github.io/docs/server-admin/datasets#eddtablefromfilenames")
                    .set(EDMessages.DEFAULT_LANGUAGE, "institution", "NOAA")
                    .set(EDMessages.DEFAULT_LANGUAGE, "license", "[standard]")
                    .set(EDMessages.DEFAULT_LANGUAGE, "sourceUrl", "(local files)")
                    .set(EDMessages.DEFAULT_LANGUAGE, "summary", "A child dataset.")
                    .set(EDMessages.DEFAULT_LANGUAGE, "title", datasetID + "_child"),
                new ArrayList<>(
                    List.of(
                        new DataVariableInfo(
                            "directory",
                            null,
                            new LocalizedAttributes()
                                .set(EDMessages.DEFAULT_LANGUAGE, "ioos_category", "Other"),
                            "String"),
                        new DataVariableInfo(
                            "name",
                            null,
                            new LocalizedAttributes()
                                .set(EDMessages.DEFAULT_LANGUAGE, "ioos_category", "Other"),
                            "String"),
                        new DataVariableInfo(
                            "lastModified",
                            null,
                            new LocalizedAttributes()
                                .set(EDMessages.DEFAULT_LANGUAGE, "ioos_category", "Time")
                                .set(
                                    EDMessages.DEFAULT_LANGUAGE,
                                    "units",
                                    "milliseconds since 1970-01-01T00:00:00Z"),
                            "long"),
                        new DataVariableInfo(
                            "size",
                            null,
                            new LocalizedAttributes()
                                .set(EDMessages.DEFAULT_LANGUAGE, "ioos_category", "Other")
                                .set(EDMessages.DEFAULT_LANGUAGE, "units", "bytes"),
                            "long"))),
                1000000000, // int tReloadEveryNMinutes, It will be reloaded when this dataset is
                // reloaded (here!)
                -1, // int tUpdateEveryNMillis,
                parts[2],
                parts[3],
                true,
                ".*", // String tFileDir, String tFileNameRegex, boolean tRecursive, String
                // tPathRegex,
                EDDTableFromFiles.MF_LAST,
                "UTF-8", // String tMetadataFrom, String tCharset,
                "",
                "",
                1,
                2,
                ",", // skipHeaderToRegex, skipLinesRegex, int tColumnNamesRow, int tFirstDataRow,
                // String tColumnSeparator,
                null,
                null,
                null, // String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
                null, // String tColumnNameForExtract,
                "",
                "directory,name", // String tSortedColumnSourceName, String tSortFilesBySourceNames,
                false,
                false, // boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory,
                true,
                true, // boolean tAccessibleViaFiles, boolean tRemoveMVRows,
                0,
                1, // int tStandardizeWhat, int tNThreads,
                null,
                -1,
                null, // String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex
                null); // addVariablesWhere
      } else {
        throw new SimpleException(
            String2.ERROR + ": Unexpected fromFilesFileType=" + fromFilesFileType);
      }
    }

    fileDir =
        File2.addSlash(
            String2.replaceAll(
                fileDir, '\\', '/')); // for consistency when generating urls for users
    Test.ensureNotNull(fileNameRegex, "fileNameRegex");

    recursive = tRecursive;
    pathRegex = tPathRegex == null || tPathRegex.length() == 0 ? ".*" : tPathRegex;

    // let standardizeResultsTable handle all constraints
    sourceCanConstrainNumericData = CONSTRAIN_NO;
    sourceCanConstrainStringData = CONSTRAIN_NO;
    sourceCanConstrainStringRegex = "";

    // quickRestart isn't needed

    // get global attributes
    sourceGlobalAttributes = new Attributes();
    combinedGlobalAttributes =
        new LocalizedAttributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    String tLicense = combinedGlobalAttributes.getString(language, "license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          language,
          "license",
          String2.replaceAll(tLicense, "[standard]", EDStatic.messages.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");

    // useCachedInfo?
    Table tCachedDNLSTable = null;
    if (from == fromOnTheFly
        || // just gets root dir
        from == fromRemoteFiles) {
      String qrName = quickRestartFullFileName();

      if (EDStatic.config.quickRestart && EDStatic.initialLoadDatasets() && File2.isFile(qrName)) {

        // try to do quickRestart
        // set creationTimeMillis to time of previous creation, so next time
        // to be reloaded will be same as if ERDDAP hadn't been restarted.
        long tCreationTime = File2.getLastModified(qrName); // 0 if trouble
        if (verbose)
          String2.log(
              "  quickRestart "
                  + tDatasetID
                  + " previous="
                  + Calendar2.millisToIsoStringTZ(tCreationTime));

        // use cached info
        tCachedDNLSTable = getCachedDNLSTable();
      }

      if (tCachedDNLSTable == null) {

        // get the info to be cached
        tCachedDNLSTable =
            FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
                fileDir,
                fileNameRegex,
                from != fromOnTheFly && recursive,
                pathRegex,
                from == fromOnTheFly); // tDirectoriesToo
        tCachedDNLSTable.setColumn(
            2, new DoubleArray(tCachedDNLSTable.getColumn(2))); // long -> double
        tCachedDNLSTable.setColumn(
            3, new DoubleArray(tCachedDNLSTable.getColumn(3))); // long -> double
        if (tCachedDNLSTable.nRows() == 0)
          throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (0 matching files)");

        // store it
        File2.makeDirectory(datasetDir());
        tCachedDNLSTable.saveAsFlatNc(qrName, "row"); // throws exceptions

        // prepare for below
        tCachedDNLSTable.setColumn(
            2, new LongArray(tCachedDNLSTable.getColumn(2))); // double -> long
        tCachedDNLSTable.setColumn(
            3, new LongArray(tCachedDNLSTable.getColumn(3))); // double -> long
      }

      filesInS3Bucket = String2.isAwsS3Url(fileDir);
      if (filesInS3Bucket) {
        // find file with size >0
        PrimitiveArray sizePA = tCachedDNLSTable.getColumn(3);
        int row = 0;
        int nRows = sizePA.size();
        while (row < nRows) {
          if (sizePA.getLong(row) <= 0) row++;
          else break;
        }
        if (row < nRows) {
          // an actual file was found (not just subdirs)
          filesInPrivateS3Bucket =
              SSR.awsS3FileIsPrivate(
                  tCachedDNLSTable.getStringData(0, row) + tCachedDNLSTable.getStringData(1, row));
          if (verbose)
            String2.log(
                "  For datasetID="
                    + datasetID
                    + ", filesInPrivateS3Bucket="
                    + filesInPrivateS3Bucket);
        } else {
          // safer to assume they it is a private bucket
          filesInPrivateS3Bucket = true;
        }
      }
    }

    // get a source sample table
    Table sourceSampleTable = FileVisitorDNLS.makeEmptyTableWithUrlsAndDoubles();

    // create dataVariables[]
    int ndv = tDataVariables.size();
    dataVariables = new EDV[ndv];
    extractRegex = new String[ndv];
    extractGroup = new byte[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      String sourceName = tDataVariables.get(dv).sourceName();
      String destName = tDataVariables.get(dv).destinationName();
      if (destName == null || destName.trim().length() == 0) destName = sourceName;
      int scol = sourceSampleTable.findColumnNumber(sourceName);
      Attributes sourceAtt =
          scol >= 0 ? sourceSampleTable.columnAttributes(scol) : new Attributes();
      LocalizedAttributes addAtt = tDataVariables.get(dv).attributes();
      String sourceType = tDataVariables.get(dv).dataType();
      // if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType="
      // + tSourceType);
      extractRegex[dv] = addAtt.getString(language, "extractRegex");
      extractGroup[dv] = Math2.narrowToByte(addAtt.getInt(language, "extractGroup"));
      addAtt.remove("extractRegex");
      addAtt.remove("extractGroup");
      if (extractGroup[dv] == Byte.MAX_VALUE) extractGroup[dv] = 1; // default
      if (sourceName.startsWith("=")) {
        // gather columns needed for this script
        scriptNeedsColumns.put(
            sourceName, Script2.jexlScriptNeedsColumns(sourceName)); // needsColumns.size() may be 0

      } else if (sourceName.equals(URL)
          || sourceName.equals(NAME)
          || sourceName.equals(LASTMODIFIED)
          || sourceName.equals(SIZE)) {
        // do nothing

      } else {
        // must be extractRegex
        Test.ensureTrue(
            String2.isSomething(extractRegex[dv]),
            "'extractRegex' attribute wasn't specified for sourceName=" + sourceName);
        if (extractRegex[dv].indexOf('(') < 0 || extractRegex[dv].indexOf(')') < 0)
          throw new RuntimeException(
              "extractRegex="
                  + extractRegex[dv]
                  + " for sourceName="
                  + sourceName
                  + " must have the form: \"preRegex(groupRegex)postRegex\".");
        if (extractGroup[dv] < 1)
          throw new RuntimeException(
              "extractGroup="
                  + extractGroup[dv]
                  + " for sourceName="
                  + sourceName
                  + " must be >= 1.");
      }

      if (sourceType == null)
        throw new IllegalArgumentException(
            errorInMethod + "dataVariable#" + dv + " <sourceType> wasn't specified.");

      if (EDV.LON_NAME.equals(destName)) {
        dataVariables[dv] =
            new EDVLon(
                datasetID,
                sourceName,
                sourceAtt,
                addAtt,
                sourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        lonIndex = dv;
      } else if (EDV.LAT_NAME.equals(destName)) {
        dataVariables[dv] =
            new EDVLat(
                datasetID,
                sourceName,
                sourceAtt,
                addAtt,
                sourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        latIndex = dv;
      } else if (EDV.ALT_NAME.equals(destName)) {
        dataVariables[dv] =
            new EDVAlt(
                datasetID,
                sourceName,
                sourceAtt,
                addAtt,
                sourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        altIndex = dv;
      } else if (EDV.DEPTH_NAME.equals(destName)) {
        dataVariables[dv] =
            new EDVDepth(
                datasetID,
                sourceName,
                sourceAtt,
                addAtt,
                sourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        depthIndex = dv;
      } else if (EDV.TIME_NAME.equals(
          destName)) { // look for TIME_NAME before check hasTimeUnits (next)
        dataVariables[dv] =
            new EDVTime(
                datasetID,
                sourceName,
                sourceAtt,
                addAtt,
                sourceType); // this constructor gets source / sets destination actual_range
        timeIndex = dv;
      } else if (EDVTimeStamp.hasTimeUnits(language, sourceAtt, addAtt)) {
        dataVariables[dv] =
            new EDVTimeStamp(
                datasetID,
                sourceName,
                destName,
                sourceAtt,
                addAtt,
                sourceType); // this constructor gets source / sets destination actual_range
      } else {
        dataVariables[dv] =
            new EDV(
                datasetID,
                sourceName,
                destName,
                sourceAtt,
                addAtt,
                sourceType); // the constructor that reads actual_range
        // dataVariables[dv].setActualRangeFromDestinationMinMax();
      }
    }

    // get the data and set variable min and max
    // this also verifies that fileDir is accessible.
    if (from == fromOnTheFly) {
      // get the level0 directory info???

    } else if (from == fromFiles) {
      // the local lastMod col
      int col = String2.indexOf(dataVariableSourceNames, "lastModified");
      if (col >= 0) {
        EDV edvLocal = dataVariables[col];
        EDV edvFromFilesEDDTable = fromFilesEDDTable.dataVariables[2]; // col #2=lastModified
        edvLocal.setDestinationMinMax(
            edvFromFilesEDDTable.destinationMin(), edvFromFilesEDDTable.destinationMax());
      }

      // the local size col
      col = String2.indexOf(dataVariableSourceNames, "size");
      if (col >= 0) {
        EDV edvLocal = dataVariables[col];
        EDV edvFromFilesEDDTable = fromFilesEDDTable.dataVariables[3]; // col #3=size
        edvLocal.setDestinationMinMax(
            edvFromFilesEDDTable.destinationMin(), edvFromFilesEDDTable.destinationMax());
      }

    } else { // from==fromRemoteFiles or fromLocalFiles
      File2.makeDirectory(cacheDirectory());
      TableWriterAllWithMetadata twawm =
          new TableWriterAllWithMetadata(
              language,
              null,
              null, // metadata is irrelevant here
              cacheDirectory(),
              "constructor");
      getDataForDapQuery(
          0, // language,  throws exception if 0 files
          EDStatic.loggedInAsSuperuser,
          "/erddap/tabledap/" + datasetID + ".nc",
          "", // userDapQuery
          twawm);
      for (int dv = 0; dv < ndv; dv++) {
        EDV edv = dataVariables[dv];
        if (edv.destinationDataPAType() != PAType.STRING) {
          // Min and max were gathered as the data was written.
          // How cool is that?!
          edv.setDestinationMinMax(twawm.columnMinValue(dv), twawm.columnMaxValue(dv));
        }
      }
      twawm.releaseResources();
    }

    // accessibleViaFiles
    if (accessibleViaFiles) {
      if (from == fromFiles) {
        writeFromFilesCache3LevelFileTable(getNLevelsOfInfo(language, 3)); // may be null
      }
    }

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid();

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + this : "")
              + "\n*** EDDTableFromFileNames "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");
  }

  /** fileDir has forward slashes and trailing slash */
  public String fileDir() {
    return fileDir;
  }

  public String fileNameRegex() {
    return fileNameRegex;
  }

  public boolean recursive() {
    return recursive;
  }

  public String pathRegex() {
    return pathRegex;
  }

  /**
   * This parses fileDir=***fromOnTheFly,fileDir(usually a URL).
   *
   * @param tFileDir the initial value of fileDir, starting with "***fromFiles,".
   * @return Object[] {eddTableFromJsonlCSVFiles, realFileDir}
   */
  public static String[] parseFromOnTheFly(String tFileDir) throws Exception {
    String errorInMethod = String2.ERROR + " while parsing <fileDir>***fromOnTheFly: ";
    String parts[] = StringArray.arrayFromCSV(tFileDir);
    if (parts.length != 2)
      throw new IllegalArgumentException(
          errorInMethod + "There are " + parts.length + " CSV parts. There must be 2.");
    return parts;
  }

  /**
   * This parses
   * fileDir=***fromFiles,fromFilesFileType,fromFilesFileDir,fromFilesFileNameRegex,fromFilesRealDir.
   *
   * @param tFileDir the initial value of fileDir, starting with "***fromFiles,".
   * @return Object[] {eddTableFromJsonlCSVFiles, realFileDir}
   */
  public static String[] parseFromFiles(String tFileDir) throws Exception {
    // handle
    // fileDir=***fromFiles,fromFilesFileType,fromFilesFileDir,fromFilesFileNameRegex,fromFilesRealDir
    // assumes filesRecursive=true, filesPathRegex=".*
    String errorInMethod = String2.ERROR + " while parsing <fileDir>***fromFiles: ";
    String parts[] = StringArray.arrayFromCSV(tFileDir);
    if (parts.length != 5)
      throw new IllegalArgumentException(
          errorInMethod + "There are " + parts.length + " CSV parts. There must be 5.");

    if (!parts[1].equals("jsonlCSV"))
      throw new IllegalArgumentException(
          errorInMethod + "part[1]=fromFilesFileType=" + parts[1] + " must be 'jsonlCSV'.");

    parts[2] =
        File2.addSlash(
            String2.replaceAll(
                parts[2], '\\', '/')); // for consistency when generating urls for users
    if (!File2.isDirectory(parts[2]))
      throw new IllegalArgumentException(
          errorInMethod
              + "part[2]=fromFilesFileDir="
              + parts[2]
              + " must be an existing directory.");

    if (!String2.isSomething(parts[3])) parts[3] = ".*";

    if (!String2.isSomething(parts[4]))
      throw new IllegalArgumentException(
          errorInMethod + "part[4]: the real fileDir must be something.");

    return parts;
  }

  /**
   * This gets the cached DNLS file table (last_mod and size are longs). Only call this if from ==
   * fromRemoteFiles.
   *
   * @throws Exception
   */
  public Table getCachedDNLSTable() throws Exception {
    Table table = new Table();
    table.readFlatNc(quickRestartFullFileName(), null, 0); // standardizeWhat=0
    table.setColumn(2, new LongArray(table.getColumn(2))); // double -> long
    table.setColumn(3, new LongArray(table.getColumn(3))); // double -> long
    return table;
  }

  /**
   * If fromFiles, for caching, this tries to get all the info for n levels of subdir of file
   * information. This is slow (1 minute?) because it must go through all the cached file info.
   *
   * @param language the index of the selected language
   * @return null if trouble. The table will have rows for the subdirs, too.
   */
  public Table getNLevelsOfInfo(int language, int nLevels) {
    try {
      // The code for this is very similar to the accessibleViaFilesFileTable() below.
      // MAKE SIMILAR CHANGES?
      String tDir = cacheDirectory(); // tDir is created by EDD.ensureValid
      String tFileName = suggestFileName(null, "tFileTable_NLevels_" + datasetID, ".twardt");
      TableWriterAllReduceDnlsTableNLevels twardt =
          new TableWriterAllReduceDnlsTableNLevels(
              language, fromFilesEDDTable, null, tDir, tFileName, fileDir, nLevels); // nLevels

      // query to twardt
      Table dnlsTable = null;
      try {
        fromFilesEDDTable.getDataForDapQuery(
            language,
            null,
            "/erddap/tabledap/" + datasetID, // for history        something/something/something
            "",
            twardt); // no constraints

        // if too many results rows, don't cache
        if (twardt.nRows() > 10000) return null;

        // clean up twardt results
        // The proper query above converts lastMod to double epochSeconds
        // so convert back to long epochMillis.
        dnlsTable = twardt.cumulativeTable();
        dnlsTable.getColumn(2).scaleAddOffset(1000, 0);
        dnlsTable.setColumn(2, new LongArray(dnlsTable.getColumn(2)));
        dnlsTable.columnAttributes(2).set("units", "milliseconds since 1970-01-01T00:00:00Z");
        dnlsTable.sortIgnoreCase(new int[] {1}, new boolean[] {true});

      } catch (Throwable t2) {
        if (t2.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0) throw t2;
        dnlsTable = FileVisitorDNLS.makeEmptyTable();
      }

      // and add subdirs
      String subdirs[] = twardt.subdirHash().toArray(new String[0]);
      Arrays.sort(subdirs, String2.STRING_COMPARATOR_IGNORE_CASE);
      StringArray dirSA = (StringArray) dnlsTable.getColumn(0);
      for (String subdir : subdirs) {
        dirSA.add(fileDir + subdir);
      }
      dnlsTable.makeColumnsSameSize();
      String2.log(
          datasetID + " getNLevelsOfInfo table:\n" + dnlsTable.dataToString(debugMode ? 100 : 5));

      return dnlsTable;
    } catch (Throwable t) {
      String2.log("Caught ERROR in getNLevelsOfInfo():\n" + MustBe.throwableToString(t));
      return null;
    }
  }

  /** This returns the full file name of the fromFilesCache3LevelFileTable stored file. */
  public String fromFilesCache3LevelFileTable_FileName() {
    return datasetDir() + "fromFilesCache3LevelFileTable.json";
  }

  /**
   * This writes the fromFilesCache3LevelFileTable to the datasetDirectory as a .jsonlCSV file.
   *
   * @param tTable if null, nothing is done
   */
  public void writeFromFilesCache3LevelFileTable(Table tTable) {
    try {
      if (tTable == null) {
        File2.delete(fromFilesCache3LevelFileTable_FileName()); // in case it exists
      } else {
        // constructor hasn't created dir, so ensure it exists
        File2.makeDirectory(datasetDir());
        tTable.writeJsonlCSV(fromFilesCache3LevelFileTable_FileName(), false); // append
      }
    } catch (Exception e) {
      String2.log("Caught:\n" + MustBe.throwableToString(e));
    }
  }

  /**
   * This reads the fromFilesCache3LevelFileTable (a .jsonlCSV file) from the datasetDirectory.
   *
   * @return the table (or null if trouble)
   */
  public Table readFromFilesCache3LevelFileTable() {
    try {
      String[] array = new String[FileVisitorDNLS.DNLS_COLUMN_TYPES_SSLL.size()];
      array = FileVisitorDNLS.DNLS_COLUMN_TYPES_SSLL.toArray(array);
      Table table3 = new Table();
      table3.readJsonlCSV(
          fromFilesCache3LevelFileTable_FileName(),
          new StringArray(FileVisitorDNLS.DNLS_COLUMN_NAMES),
          array,
          false); // simplify
      return table3;
    } catch (Exception e) {
      String2.log("Caught:\n" + MustBe.throwableToString(e));
      return null;
    }
  }

  private Table prepareFileTableToReturn(
      Table table, HttpServletRequest request, String loggedInAs, int language) {
    for (int i = 0; i < table.nRows(); i++) {
      String dir = table.getStringData(0, i).replace(fileDir, "").replace("\\", "/");
      String id = dir + table.getStringData(1, i);
      String url =
          EDStatic.erddapUrl(request, loggedInAs, language)
              + "/files/"
              + datasetID()
              + "/"
              + dir
              + table.getStringData(1, i);
      table.setStringData(0, i, id);
      table.setStringData(1, i, url);
    }
    return table;
  }

  @Override
  public Table getFilesUrlList(HttpServletRequest request, String loggedInAs, int language)
      throws Throwable {
    if (!accessibleViaFiles) return null;
    try {
      // fromOnTheFly
      if (from == fromOnTheFly) {
        // get it on-the-fly from source
        // check that nextPath matches pathRegex?
        Table dnlsTable =
            FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
                fileDir,
                fileNameRegex,
                recursive,
                pathRegex,
                true); // tRecursive, pathRegex, tDirectoriesToo
        return prepareFileTableToReturn(dnlsTable, request, loggedInAs, language);
      }

      // fromFiles
      if (from == fromFiles) {
        // is info in cache?
        if (String2.countAll("", '/') < 3) { // because I got 3 for cache
          Table dnlsTable =
              readFromFilesCache3LevelFileTable(); // It's always a copy from disk. May be null.
          if (dnlsTable != null) {
            return prepareFileTableToReturn(dnlsTable, request, loggedInAs, language);
          }
        }
        // The code for this is very similar to the getTwoLevelsOfInfo() above.
        // MAKE SIMILAR CHANGES?
        String tDir = cacheDirectory(); // tDir is created by EDD.ensureValid
        String tFileName =
            suggestFileName(
                null,
                "tFileTable_", // short name
                ".twardt");
        TableWriterAllReduceDnlsTable twardt =
            new TableWriterAllReduceDnlsTable(language, this, null, tDir, tFileName, fileDir);

        // query to twardt
        String shortened = fileDir;
        char lastCh = shortened.charAt(shortened.length() - 1);
        shortened = shortened.substring(0, shortened.length() - 1);
        Table dnlsTable = null;
        try {
          fromFilesEDDTable.getDataForDapQuery(
              language,
              null,
              "/erddap/tabledap/" + datasetID, // for history. Not relevant.
              // this is effectively: startsWith(fileDir + nextPath)           //e.g. find:
              // /foo/bar/[one thing]
              "&directory>="
                  + String2.toJson(fileDir)
                  + // e.g. get:  /foo/bar/  //reject some files
                  "&directory<"
                  + String2.toJson(
                      shortened + (char) (lastCh + 1)), // e.g. &get: /foo/bar0  //reject some files
              // "&directory=~" + String2.toJson(fileDir + nextPath + "[^/]%2B"),  //2B = +
              twardt);

          // clean up twardt results
          // The proper query above converts lastMod to double epochSeconds
          // so convert back to long epochMillis.
          dnlsTable = twardt.cumulativeTable();
          dnlsTable.getColumn(2).scaleAddOffset(1000, 0);
          dnlsTable.setColumn(2, new LongArray(dnlsTable.getColumn(2)));
          dnlsTable.columnAttributes(2).set("units", "milliseconds since 1970-01-01T00:00:00Z");
          dnlsTable.sortIgnoreCase(new int[] {1}, new boolean[] {true});
        } catch (Throwable t2) {
          if (t2.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0) throw t2;
          dnlsTable = FileVisitorDNLS.makeEmptyTable();
        }

        String subDirs[] = twardt.subdirHash().toArray(new String[0]);
        Arrays.sort(subDirs, String2.STRING_COMPARATOR_IGNORE_CASE);
        return prepareFileTableToReturn(dnlsTable, request, loggedInAs, language);
      }

      Table dnlsTable =
          from == fromRemoteFiles
              ? getCachedDNLSTable()
              : FileVisitorDNLS
                  .oneStep( // fromLocalFiles   //throws IOException if "Too many open files"
                      fileDir, fileNameRegex, recursive, pathRegex, false); // dirToo=false
      return prepareFileTableToReturn(dnlsTable, request, loggedInAs, language);

    } catch (Throwable t) {
      String2.log("Caught ERROR in getFileList():\n" + MustBe.throwableToString(t));
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
   * @return null if trouble (table.nRows + nSubdirs = 0 is not trouble) or Object[3] where [0] is a
   *     sorted table with file "Name" (String), "Last modified" (long millis), "Size" (long), and
   *     "Description" (String, but usually no content), [1] is a sorted String[] with the short
   *     names of directories that are 1 level lower, and [2] is the local directory corresponding
   *     to this (or null, if not a local dir).
   */
  @Override
  public Object[] accessibleViaFilesFileTable(int language, String nextPath) {
    if (!accessibleViaFiles) return null;
    try {

      // fromOnTheFly
      if (from == fromOnTheFly) {
        // get it on-the-fly from source
        // check that nextPath matches pathRegex?
        Table dnlsTable =
            FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
                fileDir + nextPath,
                fileNameRegex,
                false,
                pathRegex,
                true); // tRecursive, pathRegex, tDirectoriesToo
        if (dnlsTable == null) {
          return null;
        } else {
          String subDirs[] = FileVisitorDNLS.reduceDnlsTableToOneDir(dnlsTable, fileDir + nextPath);
          accessibleViaFilesMakeReadyForUser(dnlsTable);
          return new Object[] {dnlsTable, subDirs, fileDir + nextPath};
        }
      }

      // fromFiles
      if (from == fromFiles) {

        // is info in cache?
        if (String2.countAll(nextPath, '/') < 3) { // because I got 3 for cache
          Table dnlsTable =
              readFromFilesCache3LevelFileTable(); // It's always a copy from disk. May be null.
          if (dnlsTable != null) {
            String subDirs[] =
                FileVisitorDNLS.reduceDnlsTableToOneDir(dnlsTable, fileDir + nextPath);
            accessibleViaFilesMakeReadyForUser(dnlsTable);
            return new Object[] {dnlsTable, subDirs, fileDir + nextPath};
          }
        }

        // The code for this is very similar to the getTwoLevelsOfInfo() above.
        // MAKE SIMILAR CHANGES?
        String tDir = cacheDirectory(); // tDir is created by EDD.ensureValid
        String tFileName =
            suggestFileName(
                null,
                "tFileTable_"
                    + (nextPath.length() == 0
                        ? ""
                        : nextPath.substring(0, nextPath.length() - 1)), // short name
                ".twardt");
        TableWriterAllReduceDnlsTable twardt =
            new TableWriterAllReduceDnlsTable(
                language, this, null, tDir, tFileName, fileDir + nextPath);

        // query to twardt
        String shortened = fileDir + nextPath;
        char lastCh = shortened.charAt(shortened.length() - 1);
        shortened = shortened.substring(0, shortened.length() - 1);
        Table dnlsTable = null;
        try {
          fromFilesEDDTable.getDataForDapQuery(
              language,
              null,
              "/erddap/tabledap/" + datasetID, // for history. Not relevant.
              // this is effectively: startsWith(fileDir + nextPath)           //e.g. find:
              // /foo/bar/[one thing]
              "&directory>="
                  + String2.toJson(fileDir + nextPath)
                  + // e.g. get:  /foo/bar/  //reject some files
                  "&directory<"
                  + String2.toJson(
                      shortened + (char) (lastCh + 1)), // e.g. &get: /foo/bar0  //reject some files
              // "&directory=~" + String2.toJson(fileDir + nextPath + "[^/]%2B"),  //2B = +
              twardt);

          // clean up twardt results
          // The proper query above converts lastMod to double epochSeconds
          // so convert back to long epochMillis.
          dnlsTable = twardt.cumulativeTable();
          dnlsTable.getColumn(2).scaleAddOffset(1000, 0);
          dnlsTable.setColumn(2, new LongArray(dnlsTable.getColumn(2)));
          dnlsTable.columnAttributes(2).set("units", "milliseconds since 1970-01-01T00:00:00Z");
          dnlsTable.sortIgnoreCase(new int[] {1}, new boolean[] {true});
        } catch (Throwable t2) {
          if (t2.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0) throw t2;
          dnlsTable = FileVisitorDNLS.makeEmptyTable();
        }

        String subDirs[] = twardt.subdirHash().toArray(new String[0]);
        Arrays.sort(subDirs, String2.STRING_COMPARATOR_IGNORE_CASE);
        accessibleViaFilesMakeReadyForUser(dnlsTable);
        return new Object[] {dnlsTable, subDirs, fileDir + nextPath};
      }

      Table dnlsTable =
          from == fromRemoteFiles
              ? getCachedDNLSTable()
              : FileVisitorDNLS
                  .oneStep( // fromLocalFiles   //throws IOException if "Too many open files"
                      fileDir, fileNameRegex, recursive, pathRegex, false); // dirToo=false

      // remove files other than fileDir+nextPath and generate array of immediate subDir names
      String subDirs[] = FileVisitorDNLS.reduceDnlsTableToOneDir(dnlsTable, fileDir + nextPath);
      accessibleViaFilesMakeReadyForUser(dnlsTable);
      return new Object[] {dnlsTable, subDirs, fileDir + nextPath};

    } catch (Throwable t) {
      String2.log("Caught ERROR in getTwoLevelsOfInfo():\n" + MustBe.throwableToString(t));
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
    if (!accessibleViaFiles) return null;
    String msg = datasetID() + " accessibleViaFilesGetLocal(" + relativeFileName + "): ";
    try {
      // regardless of 'from' setting, get list of files for this nextPath,
      // and see if the relativeFileName is present
      String nextPath = File2.getDirectory(relativeFileName);
      String nameAndExt = File2.getNameAndExtension(relativeFileName);
      Object oar[] = accessibleViaFilesFileTable(language, nextPath);
      if (oar == null) {
        String2.log(msg + "accessibleViaFilesFileTable is null");
        return null;
      }
      Table dnlsTable = (Table) oar[0];
      // String2.log(">> " + msg + "\n" + dnlsTable.dataToString());
      StringArray nameSA = (StringArray) dnlsTable.getColumn(0);
      int which = nameSA.indexOf(nameAndExt);
      if (which < 0) {
        String2.log(msg + "That file isn't in the fileTable.");
        return null;
      }
      return fileDir + relativeFileName;

    } catch (Exception e) {
      String2.log(msg + ":\n" + MustBe.throwableToString(e));
      return null;
    }
  }

  /**
   * Get low level data: URL, NAME, LASTMODIFIED (as double epoch seconds), SIZE (as double)
   *
   * @param language the index of the selected language
   * @param recursive for file==fromOnTheFly, use false.
   * @throws Exception if trouble, e.g., 0 matching files
   */
  public static Table getBasicTable(
      int language,
      String fileDir,
      String fileNameRegex,
      boolean recursive,
      String pathRegex,
      String loggedInAs,
      String datasetID)
      throws Exception {
    Table table =
        FileVisitorDNLS.oneStepDoubleWithUrlsNotDirs(
            fileDir,
            fileNameRegex,
            recursive,
            pathRegex,
            EDStatic.erddapUrl(null, loggedInAs, language) + "/files/" + datasetID + "/");
    int nRows = table.nRows();
    if (nRows == 0) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (0 matching files)");
    return table;
  }

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be
   *     null.
   * @param tableWriter
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {

    // String2.log("\n*** getDataForDapQuery: who's calling:\n" + MustBe.stackTrace());
    // get the sourceDapQuery (a query that the source can handle)
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    getSourceQueryFromDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues); // timeStamp constraints other than regex are epochSeconds
    int nResultsVariables = resultsVariables.size();

    // gather info for columns that have =script sourceNames
    StringArray scriptNames = null;
    StringArray scriptTypes = null;
    for (int i = 0; i < nResultsVariables; i++) {
      String name = resultsVariables.get(i);
      if (name.startsWith("=")) {
        // content comes from a script or fixedValue
        if (scriptNames == null) {
          scriptNames = new StringArray();
          scriptTypes = new StringArray();
        }
        // find the corresponding dataVariable (dataVariableSourceNames hasn't been created when
        // this is called by constructor)
        int dv = 0;
        int ndv = dataVariables.length;
        while (dv < ndv && !dataVariables[dv].sourceName().equals(name)) dv++;
        if (dv == ndv) // shouldn't happen
        throw new RuntimeException("sourceName not found: " + name);
        scriptNames.add(name);
        scriptTypes.add(dataVariables[dv].sourceDataType());

        // ensure columns referenced in script are in sourceNamesSet
        // But that's not an issue here because source always supplies all columns.
        // needOtherSourceNames.addAll(scriptNeedsColumns.get(name));
      }
    }

    int nSubTables = 1; // usually just 1 table. fromFiles is the exception

    // prep for fromFiles
    StringArray fromFileFileNames = null;
    StringArray fromFileColNames = null;
    String[] fromFileColTypes = null;
    if (from == fromFiles) {
      // get a list of files
      Table dnlsTable = fromFilesEDDTable.getDnlsTable();
      StringArray dirPA = (StringArray) dnlsTable.getColumn(0);
      fromFileFileNames = (StringArray) dnlsTable.getColumn(1); // namesPA
      nSubTables = fromFileFileNames.size();
      // combine dir + name
      for (int row = 0; row < nSubTables; row++)
        fromFileFileNames.set(row, dirPA.get(row) + fromFileFileNames.get(row));
      // String2.log("\nfromFileFileNames=\n" + fromFileFileNames.toNewlineString());

      fromFileColNames = StringArray.fromCSV("directory, name, lastModified, size");
      fromFileColTypes = new String[] {"String", "String", "double", "double"};
    }

    for (int sti = 0; sti < nSubTables; sti++) {

      // get a chunk of low level data: ERDDAP URL, NAME, LASTMODIFIED (as double epoch seconds),
      // SIZE (as double)
      Table table;
      if (from == fromFiles) {
        // The problem is: it's hard/impossible to convert the userDapQuery for
        // url,name,lastMod,size
        //  into a temporary query to remoteDir,name,lastMod,size.
        //  Also, partial response might need too much memory.
        // Easy solution (but slow) is: for each file, convert to url below, then constrain below.
        // This isn't efficient, but doing it efficiently is hard and probably not worth the effort.
        // FUTURE: a simple optimization would be to pre-constrain name,lastMod,size
        if (fromFilesFileType.equals("jsonlCSV")) {
          table = new Table();
          table.readJsonlCSV(
              fromFileFileNames.get(sti), fromFileColNames, fromFileColTypes, false); // simplify

        } else {
          throw new SimpleException(
              String2.ERROR + ": Unexpected fromFilesFileType=" + fromFilesFileType);
        }

        // ensure cols found
        if (table.nColumns() != 4)
          throw new SimpleException(
              String2.ERROR
                  + " for fileName="
                  + fromFileFileNames.get(sti)
                  + ", unexpected columnNames="
                  + String2.toCSVString(table.getColumnNames())
                  + " .");

        // convert last mod from millis to seconds
        int lastModc = table.findColumnNumber("lastModified");
        PrimitiveArray lastModPA = table.getColumn(lastModc);
        lastModPA.scaleAddOffset(0.001, 0);
        table.columnAttributes(lastModc).set("units", Calendar2.SECONDS_SINCE_1970);

        // if (sti == 0) String2.log("\nbefore convert to urls:\n" + table.dataToString(5));

        // convert actual file url to ERDDAP URL, disregard others
        int tnRows = table.nRows();
        BitSet keep = new BitSet(tnRows); // initially all false
        StringArray dirSA = (StringArray) table.getColumn(0);
        int ffrdLength = fileDir.length();
        String tTo = EDStatic.erddapUrl(null, loggedInAs, language) + "/files/" + datasetID + "/";
        for (int row = 0; row < tnRows; row++) {
          String dir = dirSA.get(row);
          if (dir.startsWith(fileDir)) {
            dirSA.set(row, tTo + dir.substring(ffrdLength));
            keep.set(row);
          }
        }
        table.justKeep(keep);
        table.setColumnName(0, FileVisitorDNLS.URL);
        // if (sti == 0) String2.log("\nafter convert to urls:\n" + table.dataToString(5));

      } else if (from == fromOnTheFly || from == fromRemoteFiles) {
        table =
            FileVisitorDNLS.oneStepDoubleWithUrlsNotDirs(
                FileVisitorDNLS.oneStepDouble(getCachedDNLSTable()),
                fileDir,
                EDStatic.erddapUrl(null, loggedInAs, language) + "/files/" + datasetID + "/");

      } else { // from == fromLocalFiles
        table =
            getBasicTable(
                language,
                fileDir,
                fileNameRegex,
                recursive,
                pathRegex,
                loggedInAs,
                datasetID); // loggedInAs - irrelevant since just getting this for metadata, not
        // data
      }
      int nRows = table.nRows();

      // create other results variables as needed
      int namei = table.findColumnNumber(NAME);
      StringArray namePA = (StringArray) table.getColumn(namei);
      for (int rvi = 0; rvi < nResultsVariables; rvi++) {

        String sourceName = resultsVariables.get(rvi);
        if (sourceName.equals(URL)
            || sourceName.equals(NAME)
            || sourceName.equals(LASTMODIFIED)
            || sourceName.equals(SIZE)) continue;
        int dvi = String2.indexOf(dataVariableSourceNames(), sourceName);
        if (dvi < 0)
          throw new SimpleException(
              String2.ERROR + ": Unexpected resultsVariable sourceName=" + sourceName);
        EDV edv = dataVariables[dvi];

        if (edv.isFixedValue()) {
          // now let =script handle it below
          //// create this column from a fixed value EDV
          // PrimitiveArray pa = PrimitiveArray.factory(edv.sourceDataPAType(),
          //    nRows, edv.fixedValue());
          // table.addColumn(sourceName, pa);
          continue;
        }

        if (extractRegex[dvi] != null) {
          // create this column by extracting info from file name
          PrimitiveArray pa = PrimitiveArray.factory(edv.sourceDataPAType(), nRows, false);
          table.addColumn(sourceName, pa);
          String regex = extractRegex[dvi];
          Pattern pat = Pattern.compile(regex);
          for (int row = 0; row < nRows; row++) {
            Matcher matcher = pat.matcher(namePA.get(row));
            pa.addString(matcher.matches() ? matcher.group(extractGroup[dvi]) : "");
          }
        }

        // Remaining columns are script columns. Handle below.
      }

      // convert script columns into data columns
      if (scriptNames != null)
        convertScriptColumnsToDataColumns(
            "", // tFileDir + tFileName: there is no single source file name for this table
            table,
            scriptNames,
            scriptTypes,
            scriptNeedsColumns);

      if (debugMode) String2.log(">> getDataForDapQuery:\n" + table);
      if (table.nRows() > 0) { // should be
        standardizeResultsTable(language, requestUrl, userDapQuery, table);
        tableWriter.writeSome(table);
      }
    }

    tableWriter.finish();
  }

  /**
   * This does its best to generate a read-to-use datasets.xml entry for an EDDTableFromFileNames.
   *
   * @param tFileDir
   * @param tFileNameRegex
   * @param tRecursive
   * @param tPathRegex
   * @param externalGlobalAttributes globalAttributes gleaned from external sources, e.g., a THREDDS
   *     catalog.xml file. These have priority over other sourceGlobalAttributes. Okay to use null
   *     if none.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble. If no trouble, then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      int tReloadEveryNMinutes,
      String tInfoUrl,
      String tInstitution,
      String tSummary,
      String tTitle,
      Attributes externalGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromFileNames.generateDatasetsXml"
            + "\nfileDir="
            + tFileDir
            + " fileNameRegex="
            + tFileNameRegex
            + " recursive="
            + tRecursive
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
            + "\nexternalGlobalAttributes="
            + externalGlobalAttributes);

    if (!String2.isSomething(tFileDir))
      throw new IllegalArgumentException("fileDir wasn't specified.");

    // deal with ***fromOnTheFly and ***fromFiles
    boolean tFromOnTheFly = tFileDir.startsWith("***fromOnTheFly,");
    boolean tFromFiles = tFileDir.startsWith("***fromFiles,");
    String tFromFilesActualSource = null;
    if (tFromOnTheFly) {
      String parts[] = parseFromOnTheFly(tFileDir);
      tFromFilesActualSource = parts[1];
    } else if (tFromFiles) {
      String parts[] = parseFromFiles(tFileDir);
      tFromFilesActualSource = parts[4];
    } else {
      tFileDir =
          File2.addSlash(
              String2.replaceAll(
                  tFileDir, '\\', '/')); // important for consistency with urls for users
    }

    String tPathRegex = ".*";
    String tDatasetID =
        suggestDatasetID(
            // if awsS3, important that it start with tFileDir
            tFileDir
                + tFileNameRegex
                +
                // distinguish from e.g., EDDGridFromNcFiles for same files
                "(EDDTableFromFileNames)");

    boolean tFilesAreLocal = !String2.isRemote(tFileDir);
    if (tReloadEveryNMinutes < suggestReloadEveryNMinutesMin
        || tReloadEveryNMinutes > suggestReloadEveryNMinutesMax)
      tReloadEveryNMinutes =
          tFromOnTheFly
              ? DEFAULT_RELOAD_EVERY_N_MINUTES
              : tFromFiles ? 120 : tFilesAreLocal ? DEFAULT_RELOAD_EVERY_N_MINUTES : 120;

    // make the sourceTable and addTable
    Table sourceTable = null;
    if (tFromOnTheFly || tFromFiles) {
      sourceTable = FileVisitorDNLS.makeEmptyTableWithUrlsAndDoubles();
    } else {
      sourceTable =
          FileVisitorDNLS.oneStepDoubleWithUrlsNotDirs(
              tFileDir,
              tFileNameRegex,
              tRecursive,
              tPathRegex,
              EDStatic.preferredErddapUrl + "/files/" + tDatasetID + "/");
    }

    if (tFromOnTheFly) {
      String bro[] = String2.parseAwsS3Url(tFromFilesActualSource); // force trailing slash?
      String tBucket = "";
      String tOther = "";
      String tTitle2 = "";
      if (bro != null) {
        // bucket names are globally unique
        tBucket = "the AWS S3 " + bro[0] + " bucket at ";
        tTitle2 = "the AWS S3 " + bro[0] + " Bucket";
        tOther =
            " AWS S3 doesn't offer a simple way to browser the files in buckets. "
                + "This dataset is a solution to that problem for this bucket.";
      } else {
        tTitle2 = tFromFilesActualSource;
      }
      if (!String2.isSomething(tSummary))
        tSummary =
            "This dataset has file information from "
                + tBucket
                + tFromFilesActualSource
                + " . Use ERDDAP's \"files\" system for this dataset to browse and download the files. "
                + "The \"files\" information for this dataset is always perfectly up-to-date because ERDDAP gets it on-the-fly."
                + tOther;
      if (!String2.isSomething(tTitle)) tTitle = "File Names from " + tTitle2;
    }

    Table addTable = new Table();
    addTable
        .globalAttributes()
        .add("cdm_data_type", "Other")
        .add("Conventions", "COARDS, CF-1.10, ACDD-1.3")
        .add("creator_name", "null")
        .add("creator_email", "null")
        .add("creator_url", "null")
        .add("history", "null")
        .add("infoUrl", String2.isSomething(tInfoUrl) ? tInfoUrl : "???")
        .add("institution", String2.isSomething(tInstitution) ? tInstitution : "???")
        .add(
            "sourceUrl",
            tFromOnTheFly || tFromFiles
                ? tFromFilesActualSource
                : "(" + (tFilesAreLocal ? "local" : "remote") + " files)")
        .add("summary", String2.isSomething(tSummary) ? tSummary : "???")
        .add("title", String2.isSomething(tTitle) ? tTitle : "???");
    if (!tFromOnTheFly) addTable.globalAttributes().add("subsetVariables", "fileType");
    int nCols = sourceTable.nColumns();
    for (int col = 0; col < nCols; col++) {
      String sourceName = sourceTable.getColumnName(col);
      Attributes addAtts = new Attributes();
      addTable.addColumn(
          col, sourceName, (PrimitiveArray) sourceTable.getColumn(col).clone(), addAtts);
    }
    Set<String> keywords = suggestKeywords(sourceTable, addTable);
    cleanSuggestedKeywords(keywords);
    String keywordSar[] = keywords.toArray(new String[0]);
    Arrays.sort(keywordSar, String2.STRING_COMPARATOR_IGNORE_CASE);
    addTable.globalAttributes().add("keywords", String2.toCSSVString(keywordSar));

    // write the information
    String sb =
        "<dataset type=\"EDDTableFromFileNames\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <fileDir>"
            + XML.encodeAsXML(tFileDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(tFileNameRegex)
            + "</fileNameRegex>\n"
            + "    <recursive>"
            + tRecursive
            + "</recursive>\n"
            + "    <pathRegex>"
            + XML.encodeAsXML(tPathRegex)
            + "</pathRegex>\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + writeAttsForDatasetsXml(false, sourceTable.globalAttributes(), "    ")
            + writeAttsForDatasetsXml(true, addTable.globalAttributes(), "    ")
            + writeVariablesForDatasetsXml(sourceTable, addTable, "dataVariable", true, false)
            + // includeDataType, questionDestinationName
            """
                          <dataVariable>
                              <sourceName>fileType</sourceName>
                              <destinationName>fileType</destinationName>
                              <dataType>String</dataType>
                              <addAttributes>
                                  <att name="extractRegex">.*(\\..+?)</att>
                                  <att name="extractGroup" type="int">1</att>
                                  <att name="ioos_category">Identifier</att>
                                  <att name="long_name">File Type</att>
                              </addAttributes>
                          </dataVariable>
                          <!-- You can create other variables which are derived from extracts
                               from the file names.  Use an extractRegex attribute to specify a
                               regular expression with a capturing group (in parentheses). The
                               part of the file name which matches the specified capturing group
                               (usually group #1) will be extracted to make the new data variable.
                               fileType above shows how to extract a String. Below are examples
                               showing how to extract a date, and how to extract an integer.
                          <dataVariable>
                              <sourceName>time</sourceName>
                              <destinationName>time</destinationName>
                              <dataType>String</dataType>
                              <addAttributes>
                                  <att name="extractRegex">jplMURSST(.*)\\.png</att>
                                  <att name="extractGroup" type="int">1</att>
                                  <att name="units">yyyyMMddHHmmss</att>
                              </addAttributes>
                          </dataVariable>
                          <dataVariable>
                              <sourceName>day</sourceName>
                              <destinationName>day</destinationName>
                              <dataType>int</dataType>
                              <addAttributes>
                                  <att name="extractRegex">jplMURSST.{6}(..).{6}\\.png</att>
                                  <att name="extractGroup" type="int">1</att>
                                  <att name="ioos_category">Time</att>
                              </addAttributes>
                          </dataVariable>
                          -->
                      """
            + """
                      </dataset>

                      """;

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb;
  }
}
