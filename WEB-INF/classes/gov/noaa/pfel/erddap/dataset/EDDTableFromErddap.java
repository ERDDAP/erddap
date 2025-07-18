/*
 * EDDTableFromErddap Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.LongArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.handlers.EDDTableFromErddapHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Queue;
import org.semver4j.Semver;

/**
 * This class represents a table of data from an opendap sequence source.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-08
 */
@SaxHandlerClass(EDDTableFromErddapHandler.class)
public class EDDTableFromErddap extends EDDTable implements FromErddap {

  // default = last version before /version service was added
  protected Semver sourceErddapVersion = EDStatic.getSemver("1.22");
  boolean useNccsv; // when requesting data from the remote ERDDAP

  /**
   * Indicates if data can be transmitted in a compressed form. It is unlikely anyone would want to
   * change this.
   */
  public static boolean acceptDeflate = true;

  protected String publicSourceErddapUrl;
  protected boolean subscribeToRemoteErddapDataset;
  private boolean redirect = true;
  private boolean knowsActualRange;

  /**
   * This constructs an EDDTableFromErddap based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromErddap"&gt;
   *     having just been read.
   * @return an EDDTableFromErddap. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromErddap fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableFromErddap(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    int tReloadEveryNMinutes = Integer.MAX_VALUE;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaFiles = EDStatic.config.defaultAccessibleViaFiles;
    StringArray tOnChange = new StringArray();
    boolean tSubscribeToRemoteErddapDataset = EDStatic.config.subscribeToRemoteErddapDataset;
    boolean tRedirect = true;
    String tFgdcFile = null;
    String tIso19115File = null;
    String tSosOfferingPrefix = null;
    String tLocalSourceUrl = null;
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
        case "<reloadEveryNMinutes>",
            "<redirect>",
            "<subscribeToRemoteErddapDataset>",
            "<addVariablesWhere>",
            "<defaultGraphQuery>",
            "<defaultDataQuery>",
            "<sosOfferingPrefix>",
            "<iso19115File>",
            "<fgdcFile>",
            "<onChange>",
            "<sourceUrl>",
            "<accessibleViaFiles>",
            "<graphsAccessibleTo>" -> {}
        case "</reloadEveryNMinutes>" -> tReloadEveryNMinutes = String2.parseInt(content);

          // Since this erddap can never be logged in to the remote ERDDAP,
          // it can never get dataset info from the remote erddap dataset (which should have
          // restricted
          // access).
          // Plus there is no way to pass accessibleTo info between ERDDAP's (but not to users).
          // So there is currently no way to make this work.
        case "<accessibleTo>" -> {}
        case "</accessibleTo>" -> tAccessibleTo = content;
        case "</graphsAccessibleTo>" -> tGraphsAccessibleTo = content;
        case "</accessibleViaFiles>" -> tAccessibleViaFiles = String2.parseBoolean(content);
        case "</sourceUrl>" -> tLocalSourceUrl = content;
        case "</onChange>" -> tOnChange.add(content);
        case "</fgdcFile>" -> tFgdcFile = content;
        case "</iso19115File>" -> tIso19115File = content;
        case "</sosOfferingPrefix>" -> tSosOfferingPrefix = content;
        case "</defaultDataQuery>" -> tDefaultDataQuery = content;
        case "</defaultGraphQuery>" -> tDefaultGraphQuery = content;
        case "</addVariablesWhere>" -> tAddVariablesWhere = content;
        case "</subscribeToRemoteErddapDataset>" ->
            tSubscribeToRemoteErddapDataset = String2.parseBoolean(content);
        case "</redirect>" -> tRedirect = String2.parseBoolean(content);
        default -> xmlReader.unexpectedTagException();
      }
    }

    return new EDDTableFromErddap(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaFiles,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tReloadEveryNMinutes,
        tLocalSourceUrl,
        tSubscribeToRemoteErddapDataset,
        tRedirect);
  }

  /**
   * The constructor.
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
   * @param tIso19115File This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data.
   * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
   * @throws Throwable if trouble
   */
  public EDDTableFromErddap(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaFiles,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      int tReloadEveryNMinutes,
      String tLocalSourceUrl,
      boolean tSubscribeToRemoteErddapDataset,
      boolean tRedirect)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromErddap " + tDatasetID);
    int language = EDMessages.DEFAULT_LANGUAGE;
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromErddap(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDTableFromErddap";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    sosOfferingPrefix = tSosOfferingPrefix;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    addGlobalAttributes = new LocalizedAttributes();
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    localSourceUrl = tLocalSourceUrl;
    if (tLocalSourceUrl.indexOf("/griddap/") > 0)
      throw new RuntimeException(
          "For datasetID="
              + tDatasetID
              + ", use type=\"EDDGridFromErddap\", not EDDTableFromErddap, in datasets.xml.");
    publicSourceErddapUrl = convertToPublicSourceUrl(localSourceUrl);
    subscribeToRemoteErddapDataset = tSubscribeToRemoteErddapDataset;
    redirect = tRedirect;
    accessibleViaFiles = EDStatic.config.filesActive && tAccessibleViaFiles; // tentative. see below

    // erddap support all constraints:
    sourceNeedsExpandedFP_EQ = false;
    sourceCanConstrainNumericData = CONSTRAIN_YES;
    sourceCanConstrainStringData = CONSTRAIN_YES;
    sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP;

    // try quickRestart?
    Table sourceTable = new Table();
    sourceGlobalAttributes = sourceTable.globalAttributes();
    boolean qrMode =
        EDStatic.config.quickRestart
            && EDStatic.initialLoadDatasets()
            && File2.isFile(
                quickRestartFullFileName()); // goofy: name is .nc but contents are NCCSV
    if (qrMode) {
      // try to do quick initialLoadDatasets()
      // If this fails anytime during construction, the dataset will be loaded
      //  during the next major loadDatasets,
      //  which is good because it allows quick loading of other datasets to continue.
      // This will fail (good) if dataset has changed significantly and
      //  quickRestart file has outdated information.

      if (verbose) String2.log("  using info from quickRestartFile");

      // starting with 1.76, use nccsv for quick restart info
      sourceTable.readNccsv(
          quickRestartFullFileName(), false); // goofy: name is .nc but contents are NCCSV

      // set creationTimeMillis to time of previous creation, so next time
      // to be reloaded will be same as if ERDDAP hadn't been restarted.
      creationTimeMillis = sourceGlobalAttributes.getLong("creationTimeMillis");
      sourceGlobalAttributes.remove("creationTimeMillis");

      double sourceVersion = sourceGlobalAttributes.getDouble("sourceErddapVersion");
      sourceGlobalAttributes.remove("sourceErddapVersion");
      if (Double.isNaN(sourceVersion)) {
        sourceErddapVersion = getRemoteErddapVersion(localSourceUrl);
      } else {
        sourceErddapVersion = EDStatic.getSemver(String.valueOf(sourceVersion));
      }
      useNccsv = sourceErddapVersion.isGreaterThanOrEqualTo(EDStatic.getSemver("1.76"));

    } else {
      // !qrMode

      sourceErddapVersion = getRemoteErddapVersion(localSourceUrl);

      // For version 1.76+, this uses .nccsv to communicate
      // For version 1.75-, this uses DAP
      useNccsv = sourceErddapVersion.isGreaterThanOrEqualTo(EDStatic.getSemver("1.76"));

      if (useNccsv) {
        // get sourceTable from remote ERDDAP nccsv
        if (verbose) String2.log("  using info from remote dataset's .nccsvMetadata");

        sourceTable.readNccsv(localSourceUrl + ".nccsvMetadata", false); // readData?

      } else { // if !useNccsv
        // get sourceTable from remote DAP
        if (verbose) String2.log("  using info from remote dataset's DAP services");

        DAS das = new DAS();
        das.parse(
            new ByteArrayInputStream(
                SSR.getUrlResponseBytes(
                    localSourceUrl + ".das"))); // has timeout and descriptive error
        DDS dds = new DDS();
        dds.parse(
            new ByteArrayInputStream(
                SSR.getUrlResponseBytes(
                    localSourceUrl + ".dds"))); // has timeout and descriptive error

        // get global attributes
        OpendapHelper.getAttributes(das, "GLOBAL", sourceGlobalAttributes);

        // delve into the outerSequence
        BaseType outerVariable = dds.getVariable(SEQUENCE_NAME);
        if (!(outerVariable instanceof DSequence))
          throw new IllegalArgumentException(
              errorInMethod
                  + "outerVariable not a DSequence: name="
                  + outerVariable.getName()
                  + " type="
                  + outerVariable.getTypeName());
        DSequence outerSequence = (DSequence) outerVariable;
        int nOuterColumns = outerSequence.elementCount();
        AttributeTable outerAttributeTable = das.getAttributeTable(SEQUENCE_NAME);
        for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {

          // look at the variables in the outer sequence
          BaseType obt = outerSequence.getVar(outerCol);
          String tSourceName = obt.getName();

          // get the data sourcePAType
          PAType tSourcePAType = OpendapHelper.getElementPAType(obt.newPrimitiveVector());

          // get the attributes
          Attributes tSourceAtt = new Attributes();
          // note use of getName in this section
          // if (reallyVerbose) String2.log("try getting attributes for outer " + tSourceName);
          dods.dap.Attribute attribute = outerAttributeTable.getAttribute(tSourceName);
          // it should be a container with the attributes for this column
          if (attribute == null) {
            String2.log("WARNING!!! Unexpected: no attribute for outerVar=" + tSourceName + ".");
          } else if (attribute.isContainer()) {
            OpendapHelper.getAttributes(attribute.getContainer(), tSourceAtt);
          } else {
            String2.log(
                "WARNING!!! Unexpected: attribute for outerVar="
                    + tSourceName
                    + " not a container: "
                    + attribute.getName()
                    + "="
                    + attribute.getValueAt(0));
          }

          sourceTable.addColumn(
              outerCol, tSourceName, PrimitiveArray.factory(tSourcePAType, 8, false), tSourceAtt);
        }
      }
    }

    combinedGlobalAttributes =
        new LocalizedAttributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    combinedGlobalAttributes.removeValue("\"null\"");

    // make the dataVariables
    ArrayList<EDV> tDataVariables = new ArrayList<>();
    knowsActualRange = false;
    for (int col = 0; col < sourceTable.nColumns(); col++) {

      String tSourceName = sourceTable.getColumnName(col);
      Attributes tSourceAtt = sourceTable.columnAttributes(col);
      String tSourceType = sourceTable.getColumn(col).elementTypeString();

      // deal with remote not having ioos_category, but this ERDDAP requiring it
      LocalizedAttributes tAddAtt = new LocalizedAttributes();
      if (EDStatic.config.variablesMustHaveIoosCategory
          && tSourceAtt.getString("ioos_category") == null) {

        // guess ioos_category   (alternative is always assign "Unknown")
        Attributes tAtts =
            EDD.makeReadyToUseAddVariableAttributesForDatasetsXml(
                sourceGlobalAttributes,
                tSourceAtt,
                null,
                tSourceName,
                false, // tryToAddStandardName since just getting ioos_category
                false,
                false); // tryToAddColorBarMinMax, tryToFindLLAT
        // if put it in tSourceAtt, it will be available for quick restart
        tSourceAtt.add("ioos_category", tAtts.getString("ioos_category"));
      }

      // make the variable
      EDV edv = null;
      if (EDV.LON_NAME.equals(tSourceName)) {
        lonIndex = tDataVariables.size();
        edv =
            new EDVLon(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
      } else if (EDV.LAT_NAME.equals(tSourceName)) {
        latIndex = tDataVariables.size();
        edv =
            new EDVLat(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
      } else if (EDV.ALT_NAME.equals(tSourceName)) {
        altIndex = tDataVariables.size();
        edv =
            new EDVAlt(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
      } else if (EDV.DEPTH_NAME.equals(tSourceName)) {
        depthIndex = tDataVariables.size();
        edv =
            new EDVDepth(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
      } else if (EDV.TIME_NAME.equals(
          tSourceName)) { // look for TIME_NAME before check hasTimeUnits (next)
        timeIndex = tDataVariables.size();
        edv =
            new EDVTime(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // this constructor gets source / sets destination actual_range
      } else if (EDVTimeStamp.hasTimeUnits(language, tSourceAtt, tAddAtt)) {
        edv =
            new EDVTimeStamp(
                datasetID,
                tSourceName,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // this constructor gets source / sets destination actual_range
      } else {
        edv =
            new EDV(
                datasetID,
                tSourceName,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // the constructor that reads actual_range
        edv.setActualRangeFromDestinationMinMax(language);
      }
      tDataVariables.add(edv);
      if (!edv.destinationMin().isMissingValue() || !edv.destinationMax().isMissingValue())
        knowsActualRange = true; // if any min or max is know, say that in general they are known
    }
    dataVariables = new EDV[tDataVariables.size()];
    for (int dv = 0; dv < tDataVariables.size(); dv++) dataVariables[dv] = tDataVariables.get(dv);

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // finalize accessibleViaFiles
    if (accessibleViaFiles) {
      if (sourceErddapVersion.isLowerThan(EDStatic.getSemver("2.10"))) {
        accessibleViaFiles = false;
        String2.log(
            "accessibleViaFiles=false because remote ERDDAP version is <v2.10, so no support for /files/.csv .");

      } else {

        try {
          // this will only work if remote ERDDAP is v2.10+
          int po = localSourceUrl.indexOf("/tabledap/");
          Test.ensureTrue(po > 0, "localSourceUrl doesn't have /tabledap/.");
          InputStream is =
              SSR.getUrlBufferedInputStream(
                  String2.replaceAll(localSourceUrl, "/tabledap/", "/files/") + "/.csv");
          try {
            is.close();
          } catch (Exception e2) {
          }
        } catch (Exception e) {
          String2.log(
              "accessibleViaFiles=false because remote ERDDAP dataset isn't accessible via /files/ :\n"
                  + MustBe.throwableToString(e));
          accessibleViaFiles = false;
        }
      }
    }

    // ensure the setup is valid
    ensureValid(); // this ensures many things are set, e.g., sourceUrl

    // save quickRestart info
    if (!qrMode) { // i.e., there is new info
      try {
        File2.makeDirectory(
            File2.getDirectory(
                quickRestartFullFileName())); // goofy: name is .nc but contents are NCCSV
        sourceGlobalAttributes.set("creationTimeMillis", "" + creationTimeMillis);
        sourceGlobalAttributes.set("sourceErddapVersion", sourceErddapVersion);
        sourceTable.saveAsNccsvFile(
            false,
            true,
            0,
            Integer.MAX_VALUE,
            quickRestartFullFileName()); // goofy: name is .nc but contents are NCCSV
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
      }
    }

    // try to subscribe to the remote ERDDAP dataset
    tryToSubscribeToRemoteErddapDataset(subscribeToRemoteErddapDataset, localSourceUrl);

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + this : "")
              + "\n*** EDDTableFromErddap "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");
  }

  /**
   * This returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles)
   * or false if it doesn't (e.g., EDDTableFromDatabase).
   *
   * @returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles) or
   *     false if it doesn't (e.g., EDDTableFromDatabase).
   */
  @Override
  public boolean knowsActualRange() {
    return knowsActualRange;
  } // depends on the type of remote dataset

  /** This returns the source ERDDAP's version number, e.g., 1.22 */
  @Override
  public Semver sourceErddapVersion() {
    return sourceErddapVersion;
  }

  /** This returns the local version of the source ERDDAP's url. */
  @Override
  public String getLocalSourceErddapUrl() {
    return localSourceUrl;
  }

  /** This returns the public version of the source ERDDAP's url. */
  @Override
  public String getPublicSourceErddapUrl() {
    return publicSourceErddapUrl;
  }

  /** This indicates whether user requests should be redirected. */
  @Override
  public boolean redirect() {
    return redirect;
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

    // don't getSourceQueryFromDapQuery
    // in order to bypass removal of numeric regex.
    // ERDDAP can handle anything (by definition).

    // Read all data, then write to tableWriter.
    Table table = new Table();
    String udq = String2.isSomething(userDapQuery) ? "?" + userDapQuery : "";

    if (useNccsv) {
      // FUTURE: could repeatedly: read part/ write part
      table.readNccsv(localSourceUrl + ".nccsv" + udq, true); // readData?

    } else {
      // Very unfortunate: JDAP reads all rows when it deserializes
      // (see java docs for DSequence)
      // (that's why it can return getRowCount)
      // so there is no real way to read an opendapSequence in chunks (or row by row).
      // I can't split into subsets because I don't know which variable
      //  to constrain or how to constrain it (it would change with different
      //  userDapQuery's).
      // I could write my own procedure to read DSequence (eek!).
      table.readOpendapSequence(localSourceUrl + udq, false);
    }

    // String2.log(table.toString());
    standardizeResultsTable(language, requestUrl, userDapQuery, table); // not necessary?
    tableWriter.writeAllAndFinish(table);
  }

  private void getFilesForSubdir(String subDir, Table resultsTable, Queue<String> subdirs)
      throws Exception {
    String url =
        String2.replaceAll(localSourceUrl, "/tabledap/", "/files/")
            + "/"
            + subDir
            + (subDir.length() > 0 ? "/" : "")
            + ".csv";
    BufferedReader reader = SSR.getBufferedUrlReader(url);
    Table table = new Table();
    table.readASCII(
        url, reader, "", "", 0, 1, ",", null, null, null, null,
        false); // testColumns[], testMin[], testMax[], loadColumns[], simplify)
    table.setColumn(1, new LongArray(table.getColumn(1)));
    table.setColumn(2, new LongArray(table.getColumn(2)));
    StringArray names = (StringArray) table.getColumn(0);
    for (int row = 0; row < table.nRows(); row++) {
      String name = names.get(row);
      if (name.endsWith("/")) {
        subdirs.add(
            subDir + (subDir.length() > 0 ? "/" : "") + name.substring(0, name.length() - 1));
      } else {
        resultsTable.addStringData(0, subDir + name);
        resultsTable.addStringData(
            1,
            String2.replaceAll(localSourceUrl, "/tabledap/", "/files/")
                + "/"
                + subDir
                + "/"
                + name);
        resultsTable.addLongData(2, table.getLongData(1, row));
        resultsTable.addLongData(3, table.getLongData(2, row));
      }
    }
  }

  @Override
  public Table getFilesUrlList(HttpServletRequest request, String loggedInAs, int language)
      throws Throwable {
    Table resultsTable = FileVisitorDNLS.makeEmptyTable();
    Queue<String> subdirs = new ArrayDeque<>();
    subdirs.add("");
    while (subdirs.size() > 0) {
      String subdir = subdirs.remove();
      getFilesForSubdir(subdir, resultsTable, subdirs);
    }
    return resultsTable;
  }

  @Override
  public String getFilesetUrl(HttpServletRequest request, String loggedInAs, int language) {
    return String2.replaceAll(localSourceUrl, "/tabledap/", "/files/") + "/";
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
    // almost identical code in EDDGridFromFiles and EDDTableFromFiles ("grid" vs "table")
    if (!accessibleViaFiles) return null;
    try {

      // get the .csv table from remote fromErddap dataset
      String url =
          String2.replaceAll(localSourceUrl, "/tabledap/", "/files/") + "/" + nextPath + ".csv";
      BufferedReader reader = SSR.getBufferedUrlReader(url);
      Table table = new Table();
      table.readASCII(
          url, reader, "", "", 0, 1, ",", null, null, null, null,
          false); // testColumns[], testMin[], testMax[], loadColumns[], simplify)
      String colNames = table.getColumnNamesCSVString();
      Test.ensureEqual(colNames, "Name,Last modified,Size,Description", "");
      table.setColumn(1, new LongArray(table.getColumn(1)));
      table.setColumn(2, new LongArray(table.getColumn(2)));

      // separate out the subdirs
      StringArray subdirs = new StringArray();
      BitSet keep = new BitSet(); // all false
      int nRows = table.nRows();
      StringArray names = (StringArray) table.getColumn(0);
      for (int row = 0; row < nRows; row++) {
        String name = names.get(row);
        if (name.endsWith("/")) {
          subdirs.add(name.substring(0, name.length() - 1));
        } else {
          keep.set(row);
        }
      }
      table.justKeep(keep);
      return new Object[] {table, subdirs.toStringArray(), null}; // not a local dir

    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
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
    // almost identical code in EDDGridFromFiles and EDDTableFromFiles ("grid" vs "table")
    if (!accessibleViaFiles) return null;
    return String2.replaceAll(publicSourceErddapUrl, "/tabledap/", "/files/")
        + "/"
        + relativeFileName;
  }

  /**
   * This generates datasets.xml entries for all EDDTable from a remote ERDDAP. The XML can then be
   * edited by hand and added to the datasets.xml file.
   *
   * @param tLocalSourceUrl the base url for the dataset, e.g.,
   *     "https://coastwatch.pfeg.noaa.gov/erddap". This is a localSourceUrl since it has to be
   *     accessible, but usually it is also a publicSourceUrl.
   * @param keepOriginalDatasetIDs
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(String tLocalSourceUrl, boolean keepOriginalDatasetIDs)
      throws Throwable {

    tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); // http: to https:
    String2.log(
        "\n*** EDDTableFromErddap.generateDatasetsXml"
            + "\ntLocalSourceUrl="
            + tLocalSourceUrl
            + " keepOriginalDatasetIDs="
            + keepOriginalDatasetIDs);

    // make the StringBuilder to hold the results and add documentation
    StringBuilder sb = new StringBuilder();
    /*        sb.append(  //there is very similar text in EDDGridFromErddap
    "<!-- Directions:\n" +
    " * The ready-to-use XML below includes information for all of the EDDTable datasets\n" +
    "   at the remote ERDDAP " + XML.encodeAsXML(tLocalSourceUrl) + "\n" +
    " * If you want to add all of these datasets to your ERDDAP, just paste the XML\n" +
    "   into your datasets.xml file.\n" +
    " * The datasetIDs listed below are not the same as the remote datasets' datasetIDs.\n" +
    "   They are generated automatically from the sourceURLs in a way that ensures that they are unique.\n" +
    " * !!!reloadEveryNMinutes is left as the default 10080=oncePerWeek on the assumption\n" +
    "   that the remote ERDDAP will accept your ERDDAP's request to subscribe to the dataset.\n" +
    "   If you don't get emails from the remote ERDDAP asking you to validate your subscription\n" +
    "   requests (perhaps because the remote ERDDAP has the subscription system turned off),\n" +
    "   send an email to the admin asking that s/he add onChange tags to the datasets.\n" +
    "   See the EDDTableFromErddap documentation.\n" +
    " * The XML needed for EDDTableFromErddap in datasets.xml has few options.  See\n" +
    "   https://erddap.github.io/docs/server-admin/datasets#eddfromerddap .\n" +
    "   If you want to alter a dataset's metadata or make other changes to a dataset,\n" +
    "   use EDDTableFromDapSequence to access the dataset instead of EDDTableFromErddap.\n" +
    " * If the remote ERDDAP is version 1.12 or below, this will generate incorrect, useless results.\n" +
    "-->\n");
    */
    // get the tabledap datasets in a json table
    String jsonUrl = tLocalSourceUrl + "/tabledap/index.json?page=1&itemsPerPage=1000000";
    Table table = new Table();
    table.readJson(jsonUrl, SSR.getBufferedUrlReader(jsonUrl)); // they are sorted by title
    if (keepOriginalDatasetIDs) table.ascendingSort(new String[] {"Dataset ID"});

    PrimitiveArray urlCol = table.findColumn("tabledap");
    PrimitiveArray titleCol = table.findColumn("Title");
    PrimitiveArray datasetIdCol = table.findColumn("Dataset ID");

    // go through the rows of the table
    int nRows = table.nRows();
    for (int row = 0; row < nRows; row++) {
      String id = datasetIdCol.getString(row);
      if (EDDTableFromAllDatasets.DATASET_ID.equals(id)) continue;
      // localSourceUrl isn't available (and we generally don't want it)
      String tPublicSourceUrl = urlCol.getString(row);
      // Use unchanged tPublicSourceUrl or via suggestDatasetID?
      // I guess suggestDatasetID because it ensures a unique name for use in local ERDDAP.
      // ?? Does it cause trouble to use a different datasetID here?
      String newID = keepOriginalDatasetIDs ? id : suggestDatasetID(tPublicSourceUrl);
      sb.append(
          "<dataset type=\"EDDTableFromErddap\" datasetID=\""
              + newID
              + "\" active=\"true\">\n"
              + "    <!-- "
              + XML.encodeAsXML(String2.replaceAll(titleCol.getString(row), "--", "- - "))
              + " -->\n"
              + "    <sourceUrl>"
              + XML.encodeAsXML(tPublicSourceUrl)
              + "</sourceUrl>\n"
              + "</dataset>\n");
    }

    // get the EDDTableFromErddap datasets
    try {
      jsonUrl = tLocalSourceUrl + "/search/index.json?searchFor=EDDTableFromErddap";
      table = new Table();
      table.readJson(jsonUrl, SSR.getBufferedUrlReader(jsonUrl)); // throws exception if trouble
      datasetIdCol = table.findColumn("Dataset ID"); // throws exception if trouble

      sb.append(
          """

                      <!-- Of the datasets above, the following datasets are EDDTableFromErddap's at the remote ERDDAP.
                      It would be best if you contacted the remote ERDDAP's administrator and requested the dataset XML
                      that is being using for these datasets so your ERDDAP can access the original ERDDAP source.
                      The remote EDDTableFromErddap datasets are:
                      """);
      if (datasetIdCol.size() == 0) sb.append("(none)");
      else sb.append(String2.noLongLinesAtSpace(datasetIdCol.toString(), 80, ""));
      sb.append("\n-->\n");
    } catch (Throwable t) {
      String2.log("The remote erddap has no EDDGridFromErddap's.");
    }

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }
}
