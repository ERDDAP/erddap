/*
 * EDDGridFromErddap Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridFromErddapHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * This class represents a grid dataset from an opendap DAP source.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
@SaxHandlerClass(EDDGridFromErddapHandler.class)
public class EDDGridFromErddap extends EDDGrid implements FromErddap {

  protected double sourceErddapVersion =
      1.22; // default = last version before /version service was added

  /**
   * Indicates if data can be transmitted in a compressed form. It is unlikely anyone would want to
   * change this.
   */
  public static boolean acceptDeflate = true;

  protected String publicSourceErddapUrl;
  protected boolean subscribeToRemoteErddapDataset;
  private boolean redirect = true;

  /**
   * This constructs an EDDGridFromErddap based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridFromErddap"&gt; having
   *     just been read.
   * @return an EDDGridFromErddap. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridFromErddap fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDGridFromErddap(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    int tUpdateEveryNMillis = 0;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    boolean tSubscribeToRemoteErddapDataset = EDStatic.subscribeToRemoteErddapDataset;
    boolean tRedirect = true;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tLocalSourceUrl = null;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
    boolean tDimensionValuesInMemory = true;

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
      if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<updateEveryNMillis>")) {
      } else if (localTags.equals("</updateEveryNMillis>"))
        tUpdateEveryNMillis = String2.parseInt(content);

      // Since this erddap can never be logged in to the remote ERDDAP,
      // it can never get dataset info from the remote erddap dataset (which should have restricted
      // access).
      // Plus there is no way to pass accessibleTo info between ERDDAP's (but not to users).
      // So there is currently no way to make this work.
      else if (localTags.equals("<accessibleTo>")) {
      } else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<accessibleViaWMS>")) {
      } else if (localTags.equals("</accessibleViaWMS>"))
        tAccessibleViaWMS = String2.parseBoolean(content);
      else if (localTags.equals("<accessibleViaFiles>")) {
      } else if (localTags.equals("</accessibleViaFiles>"))
        tAccessibleViaFiles = String2.parseBoolean(content);
      else if (localTags.equals("<subscribeToRemoteErddapDataset>")) {
      } else if (localTags.equals("</subscribeToRemoteErddapDataset>"))
        tSubscribeToRemoteErddapDataset = String2.parseBoolean(content);
      else if (localTags.equals("<sourceUrl>")) {
      } else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content;
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
      else if (localTags.equals("<redirect>")) {
      } else if (localTags.equals("</redirect>")) tRedirect = String2.parseBoolean(content);
      else xmlReader.unexpectedTagException();
    }
    return new EDDGridFromErddap(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tAccessibleViaFiles,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tLocalSourceUrl,
        tSubscribeToRemoteErddapDataset,
        tRedirect,
        tnThreads,
        tDimensionValuesInMemory);
  }

  /**
   * The constructor.
   *
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
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data (use
   *     Integer.MAX_VALUE for never).
   * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
   * @throws Throwable if trouble
   */
  public EDDGridFromErddap(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
      boolean tAccessibleViaFiles,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tLocalSourceUrl,
      boolean tSubscribeToRemoteErddapDataset,
      boolean tRedirect,
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridFromErddap " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridFromErddap(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDGridFromErddap";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    if (!tAccessibleViaWMS)
      accessibleViaWMS = String2.canonical(MessageFormat.format(EDStatic.noXxxAr[0], "WMS"));
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    localSourceUrl = tLocalSourceUrl;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    setUpdateEveryNMillis(tUpdateEveryNMillis);
    if (localSourceUrl.indexOf("/tabledap/") > 0)
      throw new RuntimeException(
          "For datasetID="
              + tDatasetID
              + ", use type=\"EDDTableFromErddap\", not EDDGridFromErddap, in datasets.xml.");
    publicSourceErddapUrl = convertToPublicSourceUrl(localSourceUrl);
    subscribeToRemoteErddapDataset = tSubscribeToRemoteErddapDataset;
    redirect = tRedirect;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads
    dimensionValuesInMemory = tDimensionValuesInMemory;
    accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles; // tentative. see below

    // quickRestart
    Attributes quickRestartAttributes = null;
    if (EDStatic.quickRestart
        && EDStatic.initialLoadDatasets()
        && File2.isFile(quickRestartFullFileName())) {
      // try to do quick initialLoadDatasets()
      // If this fails anytime during construction, the dataset will be loaded
      //  during the next major loadDatasets,
      //  which is good because it allows quick loading of other datasets to continue.
      // This will fail (good) if dataset has changed significantly and
      //  quickRestart file has outdated information.
      quickRestartAttributes = NcHelper.readAttributesFromNc3(quickRestartFullFileName());

      if (verbose) String2.log("  using info from quickRestartFile");

      // set creationTimeMillis to time of previous creation, so next time
      // to be reloaded will be same as if ERDDAP hadn't been restarted.
      creationTimeMillis = quickRestartAttributes.getLong("creationTimeMillis");
    }

    // open the connection to the opendap source
    DConnect dConnect = null;
    if (quickRestartAttributes == null)
      dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);

    // setup via info.json
    // source https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla5day
    // json   https://coastwatch.pfeg.noaa.gov/erddap/info/erdMHchla5day/index.json
    String jsonUrl = String2.replaceAll(localSourceUrl, "/griddap/", "/info/") + "/index.json";
    String sourceInfoString = null;
    if (quickRestartAttributes != null) {
      PrimitiveArray sourceInfoBytes = quickRestartAttributes.get("sourceInfoBytes");
      if (sourceInfoBytes != null && sourceInfoBytes instanceof ByteArray)
        sourceInfoString =
            new String(((ByteArray) sourceInfoBytes).toArray(), StandardCharsets.UTF_8);
    }
    if (sourceInfoString == null) sourceInfoString = SSR.getUrlResponseStringNewline(jsonUrl);
    Table table = new Table();
    table.readJson("sourceInfoString", new BufferedReader(new StringReader(sourceInfoString)));

    // go through the rows of table from bottom to top
    int nRows = table.nRows();
    Attributes tSourceAttributes = new Attributes();
    ArrayList tAxisVariables = new ArrayList();
    ArrayList tDataVariables = new ArrayList();
    for (int row = nRows - 1; row >= 0; row--) {

      // "columnNames": ["Row Type", "Variable Name", "Attribute Name", "Data Type", "Value"],
      // "columnTypes": ["String", "String", "String", "String", "String"],
      // "rows": [
      //     ["attribute", "NC_GLOBAL", "acknowledgement", "String", "NOAA NESDIS COASTWATCH, NOAA
      // SWFSC ERD"],
      //     ["dimension", "longitude", "", "double", "nValues=8640, evenlySpaced=true,
      // averageSpacing=0.04166667052552379"],
      //     atts...
      //     ["variable", "chlorophyll", "", "float", "time, altitude, latitude, longitude"],
      //     atts...
      String rowType = table.getStringData(0, row);
      String varName = table.getStringData(1, row);
      String attName = table.getStringData(2, row);
      String dataType = table.getStringData(3, row);
      String value = table.getStringData(4, row);

      if (rowType.equals("attribute")) {
        if (dataType.equals("String")) {
          tSourceAttributes.add(attName, value);
        } else {
          PAType tPAType = PAType.fromCohortString(dataType);
          PrimitiveArray pa = PrimitiveArray.csvFactory(tPAType, value);
          tSourceAttributes.add(attName, pa);
        }

      } else if (rowType.equals("dimension")) {
        PrimitiveArray tSourceValues =
            quickRestartAttributes == null
                ? OpendapHelper.getPrimitiveArray(dConnect, "?" + varName)
                : quickRestartAttributes.get(
                    "sourceValues_" + String2.encodeVariableNameSafe(varName));

        // deal with remote not having ioos_category, but this ERDDAP requiring it
        Attributes tAddAttributes = new Attributes();
        if (EDStatic.variablesMustHaveIoosCategory
            && tSourceAttributes.getString("ioos_category") == null) {

          // guess ioos_category   (alternative is always assign "Unknown")
          Attributes tAtts =
              EDD.makeReadyToUseAddVariableAttributesForDatasetsXml(
                  null, // sourceGlobalAtts not yet known
                  tSourceAttributes,
                  null,
                  varName,
                  true, // tryToAddStandardName
                  false,
                  true); // tryToAddColorBarMinMax, tryToFindLLAT
          tAddAttributes.add("ioos_category", tAtts.getString("ioos_category"));
        }

        // make an axisVariable
        tAxisVariables.add(
            makeAxisVariable(
                tDatasetID,
                -1,
                varName,
                varName,
                tSourceAttributes,
                tAddAttributes,
                tSourceValues));

        // make new tSourceAttributes
        tSourceAttributes = new Attributes();

        // a grid variable
      } else if (rowType.equals("variable")) {

        // deal with remote not having ioos_category, but this ERDDAP requiring it
        Attributes tAddAttributes = new Attributes();
        if (EDStatic.variablesMustHaveIoosCategory
            && tSourceAttributes.getString("ioos_category") == null) {

          // guess ioos_category   (alternative is always assign "Unknown")
          Attributes tAtts =
              EDD.makeReadyToUseAddVariableAttributesForDatasetsXml(
                  null, // sourceGlobalAtts not yet known
                  tSourceAttributes,
                  null,
                  varName,
                  false, // tryToAddStandardName  since just getting ioos_category
                  false,
                  false); // tryToAddColorBarMinMax, tryToFindLLAT
          tAddAttributes.add("ioos_category", tAtts.getString("ioos_category"));
        }

        // make a data variable
        EDV edv;
        if (varName.equals(EDV.TIME_NAME))
          throw new RuntimeException(
              errorInMethod + "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
        else if (EDVTime.hasTimeUnits(tSourceAttributes, tAddAttributes))
          edv =
              new EDVTimeStamp(
                  datasetID, varName, varName, tSourceAttributes, tAddAttributes, dataType);
        else
          edv =
              new EDV(
                  datasetID,
                  varName,
                  varName,
                  tSourceAttributes,
                  tAddAttributes,
                  dataType,
                  PAOne.fromDouble(Double.NaN),
                  PAOne.fromDouble(Double.NaN)); // hard to get min and max
        edv.extractAndSetActualRange();
        tDataVariables.add(edv);

        // make new tSourceAttributes
        tSourceAttributes = new Attributes();

        // unexpected type
      } else throw new RuntimeException("Unexpected rowType=" + rowType + ".");
    }
    if (tAxisVariables.size() == 0) throw new RuntimeException("No axisVariables found!");
    sourceGlobalAttributes = tSourceAttributes; // at the top of table, so collected last
    addGlobalAttributes = new Attributes();
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    combinedGlobalAttributes.removeValue("\"null\"");

    int nav = tAxisVariables.size();
    axisVariables = new EDVGridAxis[nav];
    for (int av = 0; av < nav; av++) {
      // reverse the order, since read (above) from bottom to top
      axisVariables[av] = (EDVGridAxis) tAxisVariables.get(nav - av - 1);
      String tName = axisVariables[av].destinationName();
      if (tName.equals(EDV.LON_NAME)) lonIndex = av;
      else if (tName.equals(EDV.LAT_NAME)) latIndex = av;
      else if (tName.equals(EDV.ALT_NAME)) altIndex = av;
      else if (tName.equals(EDV.DEPTH_NAME)) depthIndex = av;
      else if (tName.equals(EDV.TIME_NAME)) timeIndex = av;
    }

    int ndv = tDataVariables.size();
    dataVariables = new EDV[ndv];
    for (int dv = 0; dv < ndv; dv++)
      // reverse the order, since read (above) from bottom to top
      dataVariables[dv] = (EDV) tDataVariables.get(ndv - dv - 1);

    // ensure the setup is valid
    ensureValid(); // this ensures many things are set, e.g., sourceUrl

    // finalize accessibleViaFiles
    sourceErddapVersion = getRemoteErddapVersion(localSourceUrl);
    if (accessibleViaFiles) {
      if (sourceErddapVersion < 2.10) {
        accessibleViaFiles = false;
        String2.log(
            "accessibleViaFiles=false because remote ERDDAP version is <v2.10, so no support for /files/.csv .");

      } else {
        try {
          // this will only work if remote ERDDAP is v2.10+
          int po = localSourceUrl.indexOf("/griddap/");
          Test.ensureTrue(po > 0, "localSourceUrl doesn't have /griddap/.");
          InputStream is =
              SSR.getUrlBufferedInputStream(
                  String2.replaceAll(localSourceUrl, "/griddap/", "/files/") + "/.csv");
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

    // save quickRestart info
    if (quickRestartAttributes == null) { // i.e., there is new info
      try {
        quickRestartAttributes = new Attributes();
        quickRestartAttributes.set("creationTimeMillis", "" + creationTimeMillis);
        quickRestartAttributes.set(
            "sourceInfoBytes", ByteArray.fromString(sourceInfoString)); // String -> UTF-8 bytes
        for (int av = 0; av < axisVariables.length; av++) {
          quickRestartAttributes.set(
              "sourceValues_" + String2.encodeVariableNameSafe(axisVariables[av].sourceName()),
              axisVariables[av].sourceValues());
        }
        File2.makeDirectory(File2.getDirectory(quickRestartFullFileName()));
        NcHelper.writeAttributesToNc3(quickRestartFullFileName(), quickRestartAttributes);
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
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDGridFromErddap "
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
   *     EDD.requestReloadASAP(tDatasetID) and returns without doing anything.
   */
  @Override
  public boolean lowUpdate(int language, String msg, long startUpdateMillis) throws Throwable {

    // read dds
    DConnect dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);
    byte ddsBytes[] = SSR.getUrlResponseBytes(localSourceUrl + ".dds");
    DDS dds = new DDS();
    dds.parse(new ByteArrayInputStream(ddsBytes));

    // has edvga[0] changed size?
    EDVGridAxis edvga = axisVariables[0];
    EDVTimeStampGridAxis edvtsga = edvga instanceof EDVTimeStampGridAxis t ? t : null;
    PrimitiveArray oldValues = edvga.sourceValues();
    int oldSize = oldValues.size();

    // get mainDArray
    BaseType bt = dds.getVariable(dataVariables[0].sourceName()); // throws NoSuchVariableException
    DArray mainDArray = null;
    if (bt instanceof DGrid dgrid) {
      mainDArray = (DArray) dgrid.getVar(0); // first element is always main array
    } else if (bt instanceof DArray darray) {
      mainDArray = darray;
    } else {
      String2.log(
          msg
              + String2.ERROR
              + ": Unexpected "
              + dataVariables[0].destinationName()
              + " source type="
              + bt.getTypeName()
              + ".");
      // requestReloadASAP()+WaitThenTryAgain might lead to endless cycle of full reloads
      requestReloadASAP();
      return false;
    }

    // get the leftmost dimension
    DArrayDimension dad = mainDArray.getDimension(0);
    int newSize = dad.getSize();
    if (newSize < oldSize)
      throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n("
              + msg
              + "["
              + edvga.destinationName()
              + "] newSize="
              + newSize
              + " < oldSize="
              + oldSize
              + ")");
    if (newSize == oldSize) {
      if (reallyVerbose) String2.log(msg + "leftmost dimension size hasn't changed");
      return false; // finally{} below sets lastUpdate = startUpdateMillis
    }

    // newSize > oldSize, get last old value (for testing below) and new values
    PrimitiveArray newValues = null;
    if (edvga.sourceDataPAType() == PAType.INT
        && // not a perfect test
        "count".equals(edvga.sourceAttributes().getString("units")))
      newValues = new IntArray(oldSize - 1, newSize - 1); // 0 based
    else {
      try {
        newValues =
            OpendapHelper.getPrimitiveArray(
                dConnect,
                "?" + edvga.sourceName() + "[" + (oldSize - 1) + ":" + (newSize - 1) + "]");
      } catch (NoSuchVariableException nsve) {
        // hopefully avoided by testing for units=count and int datatype above
        String2.log(
            msg
                + "caught NoSuchVariableException for sourceName="
                + edvga.sourceName()
                + ". Using index numbers.");
        newValues = new IntArray(oldSize - 1, newSize - 1); // 0 based
      } // but other exceptions aren't caught
    }

    // ensure newValues is valid
    if (newValues == null || newValues.size() < (newSize - oldSize + 1)) {
      String2.log(
          msg
              + String2.ERROR
              + ": Too few "
              + edvga.destinationName()
              + " values were received (got="
              + (newValues == null ? "null" : "" + (newValues.size() - 1))
              + "expected="
              + (newSize - oldSize)
              + ").");
      return false;
    }
    if (oldValues.elementType() != newValues.elementType())
      throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n("
              + msg
              + edvga.destinationName()
              + " dataType changed: "
              + " new="
              + newValues.elementTypeString()
              + " != old="
              + oldValues.elementTypeString()
              + ")");

    // ensure last old value is unchanged
    if (oldValues.getDouble(oldSize - 1) != newValues.getDouble(0)) // they should be exactly equal
    throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n("
              + msg
              + edvga.destinationName()
              + "["
              + (oldSize - 1)
              + "] changed!  old="
              + oldValues.getDouble(oldSize - 1)
              + " != new="
              + newValues.getDouble(0));

    // prepare changes to update the dataset
    PAOne newMin = new PAOne(oldValues, 0);
    PAOne newMax = new PAOne(newValues, newValues.size() - 1);
    if (edvtsga != null) {
      newMin = PAOne.fromDouble(edvtsga.sourceTimeToEpochSeconds(newMin.getDouble()));
      newMax = PAOne.fromDouble(edvtsga.sourceTimeToEpochSeconds(newMax.getDouble()));
    } else if (edvga.scaleAddOffset()) {
      newMin = PAOne.fromDouble(newMin.getDouble() * edvga.scaleFactor() + edvga.addOffset());
      newMax = PAOne.fromDouble(newMax.getDouble() * edvga.scaleFactor() + edvga.addOffset());
    }

    // first, calculate newAverageSpacing (destination units, will be negative if isDescending)
    double newAverageSpacing = (newMax.getDouble() - newMin.getDouble()) / (newSize - 1);

    // second, test for min>max after extractScaleAddOffset, since order may have changed
    if (newMin.compareTo(newMax) > 0) {
      PAOne d = newMin;
      newMin = newMax;
      newMax = d;
    }

    // test isAscending  (having last old value is essential)
    String error = edvga.isAscending() ? newValues.isAscending() : newValues.isDescending();
    if (error.length() > 0)
      throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n("
              + edvga.destinationName()
              + " was "
              + (edvga.isAscending() ? "a" : "de")
              + "scending, but the newest values aren't ("
              + error
              + ").)");

    // if was isEvenlySpaced, test that new values are and have same averageSpacing
    // (having last old value is essential)
    boolean newIsEvenlySpaced = edvga.isEvenlySpaced(); // here, this is actually oldIsEvenlySpaced
    if (newIsEvenlySpaced) {
      error = newValues.isEvenlySpaced();
      if (error.length() > 0) {
        String2.log(
            msg
                + "changing "
                + edvga.destinationName()
                + ".isEvenlySpaced from true to false: "
                + error);
        newIsEvenlySpaced = false;

        // new spacing != old spacing ?  (precision=5, but times will be exact)
      } else if (!Math2.almostEqual(5, newAverageSpacing, edvga.averageSpacing())) {
        String2.log(
            msg
                + "changing "
                + edvga.destinationName()
                + ".isEvenlySpaced from true to false: newSpacing="
                + newAverageSpacing
                + " oldSpacing="
                + edvga.averageSpacing());
        newIsEvenlySpaced = false;
      }
    }

    // remove the last old value from newValues
    newValues.remove(0);

    // ensureCapacity of oldValues (may take time)
    oldValues.ensureCapacity(newSize); // so oldValues.append below is as fast as possible

    // right before making changes, make doubly sure another thread hasn't already (IMPERFECT TEST)
    if (oldValues.size() != oldSize) {
      String2.log(
          msg
              + "changes abandoned.  "
              + edvga.destinationName()
              + ".size changed (new="
              + oldValues.size()
              + " != old="
              + oldSize
              + ").  (By update() in another thread?)");
      return false;
    }

    // Swap changes into place quickly to minimize problems.  Better if changes were atomic.
    // Order of changes is important.
    // Other threads may be affected by some values being updated before others.
    // This is an imperfect alternative to synchronizing all uses of this dataset (which is far
    // worse).
    oldValues.append(
        newValues); // should be fast, and new size set at end to minimize concurrency problems
    edvga.setDestinationMinMax(newMin, newMax);
    edvga.setIsEvenlySpaced(newIsEvenlySpaced);
    edvga.initializeAverageSpacingAndCoarseMinMax();
    edvga.setActualRangeFromDestinationMinMax();
    if (edvga instanceof EDVTimeGridAxis)
      combinedGlobalAttributes.set(
          "time_coverage_end",
          Calendar2.epochSecondsToLimitedIsoStringT(
              edvga.combinedAttributes().getString(EDV.TIME_PRECISION), newMax.getDouble(), ""));
    edvga.clearSliderCsvValues(); // do last, to force recreation next time needed

    updateCount++;
    long thisTime = System.currentTimeMillis() - startUpdateMillis;
    cumulativeUpdateTime += thisTime;
    if (reallyVerbose)
      String2.log(
          msg
              + "succeeded. "
              + Calendar2.getCurrentISODateTimeStringLocalTZ()
              + " nValuesAdded="
              + newValues.size()
              + " time="
              + thisTime
              + "ms updateCount="
              + updateCount
              + " avgTime="
              + (cumulativeUpdateTime / updateCount)
              + "ms");
    return true;
  }

  /** This returns the source ERDDAP's version number, e.g., 1.22 */
  @Override
  public double sourceErddapVersion() {
    return sourceErddapVersion;
  }

  @Override
  public int intSourceErddapVersion() {
    return Math2.roundToInt(sourceErddapVersion * 100);
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
   * This makes a sibling dataset, based on the new sourceUrl.
   *
   * @param tLocalSourceUrl
   * @param firstAxisToMatch If 0, this tests if sourceValues for axis-variable #0+ are same. If 1,
   *     this tests if sourceValues for axis-variable #1+ are same.
   * @param shareInfo if true, this ensures that the sibling's axis and data variables are basically
   *     the same as this datasets, and then makes the new dataset point to this instance's data
   *     structures to save memory. (AxisVariable #0 isn't duplicated.) Saving memory is important
   *     if there are 1000's of siblings in ERDDAP.
   * @return EDDGrid
   * @throws Throwable if trouble (e.g., try to shareInfo, but datasets not similar)
   */
  @Override
  public EDDGrid sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable {
    if (verbose) String2.log("EDDGridFromErddap.sibling " + tLocalSourceUrl);

    int nAv = axisVariables.length;
    int nDv = dataVariables.length;

    // need a unique datasetID for sibling
    //  so cached .das .dds axis values are stored separately.
    // So make tDatasetID by putting md5Hex12 in the middle of original datasetID
    //  so beginning and ending for tDatasetID are same as original.
    int po = datasetID.length() / 2;
    String tDatasetID =
        datasetID.substring(0, po)
            + "_"
            + String2.md5Hex12(tLocalSourceUrl)
            + "_"
            + datasetID.substring(po);

    // make the sibling
    EDDGridFromErddap newEDDGrid =
        new EDDGridFromErddap(
            tDatasetID,
            String2.toSSVString(accessibleTo),
            "auto",
            false, // accessibleViaWMS
            accessibleViaFiles,
            shareInfo ? onChange : (StringArray) onChange.clone(),
            "",
            "",
            "",
            "", // fgdc, iso19115, defaultDataQuery, defaultGraphQuery,
            getReloadEveryNMinutes(),
            getUpdateEveryNMillis(),
            tLocalSourceUrl,
            subscribeToRemoteErddapDataset,
            redirect,
            nThreads,
            dimensionValuesInMemory);

    // if shareInfo, point to same internal data
    if (shareInfo) {

      // ensure similar
      boolean testAV0 = false;
      String results = similar(newEDDGrid, firstAxisToMatch, matchAxisNDigits, false);
      if (results.length() > 0) throw new RuntimeException("Error in EDDGrid.sibling: " + results);

      // shareInfo
      for (int av = 1; av < nAv; av++) // not av0
      newEDDGrid.axisVariables()[av] = axisVariables[av];
      newEDDGrid.dataVariables = dataVariables;

      // shareInfo  (the EDDGrid variables)
      newEDDGrid.axisVariableSourceNames = axisVariableSourceNames(); // () makes the array
      newEDDGrid.axisVariableDestinationNames = axisVariableDestinationNames();

      // shareInfo  (the EDD variables)
      newEDDGrid.dataVariableSourceNames = dataVariableSourceNames();
      newEDDGrid.dataVariableDestinationNames = dataVariableDestinationNames();
      newEDDGrid.title = title();
      newEDDGrid.summary = summary();
      newEDDGrid.institution = institution();
      newEDDGrid.infoUrl = infoUrl();
      newEDDGrid.cdmDataType = cdmDataType();
      newEDDGrid.searchBytes = searchBytes();
      // not sourceUrl, which will be different
      newEDDGrid.sourceGlobalAttributes = sourceGlobalAttributes();
      newEDDGrid.addGlobalAttributes = addGlobalAttributes();
      newEDDGrid.combinedGlobalAttributes = combinedGlobalAttributes();
    }

    return newEDDGrid;
  }

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
   *     the next tDataVariables.length elements are the dataValues. Both the axisValues and
   *     dataValues are straight from the source, not modified.
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public PrimitiveArray[] getSourceData(
      int language, Table tDirTable, Table tFileTable, EDV tDataVariables[], IntArray tConstraints)
      throws Throwable {

    // build String form of the constraint
    // String errorInMethod = "Error in EDDGridFromErddap.getSourceData for " + datasetID + ": ";
    String constraint = buildDapArrayQuery(tConstraints);

    // get results one var at a time (that's how OpendapHelper is set up)
    DConnect dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);
    PrimitiveArray results[] = new PrimitiveArray[axisVariables.length + tDataVariables.length];
    for (int dv = 0; dv < tDataVariables.length; dv++) {
      // get the data
      PrimitiveArray pa[] = null;
      try {
        pa =
            OpendapHelper.getPrimitiveArrays(
                dConnect, "?" + tDataVariables[dv].sourceName() + constraint);

      } catch (Throwable t) {
        EDStatic.rethrowClientAbortException(t); // first thing in catch{}

        // if OutOfMemoryError or too much data, rethrow t
        String tToString = t.toString();
        if (Thread.currentThread().isInterrupted()
            || t instanceof InterruptedException
            || t instanceof OutOfMemoryError
            || tToString.indexOf(Math2.memoryTooMuchData) >= 0
            || tToString.indexOf(Math2.TooManyOpenFiles) >= 0) throw t;

        // request should be valid, so any other error is trouble with dataset
        String2.log(MustBe.throwableToString(t));
        throw t instanceof WaitThenTryAgainException
            ? t
            : new WaitThenTryAgainException(
                EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                    + "\n("
                    + EDStatic.errorFromDataSource
                    + t.toString()
                    + ")",
                t);
      }
      if (pa.length != axisVariables.length + 1)
        throw new WaitThenTryAgainException(
            EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                + "\n(Details: An unexpected data structure was returned from the source.)");
      results[axisVariables.length + dv] = pa[0];
      if (dv == 0) {
        // I think GridDataAccessor compares observed and expected axis values
        for (int av = 0; av < axisVariables.length; av++) {
          results[av] = pa[av + 1];
        }
      } else {
        for (int av = 0; av < axisVariables.length; av++) {
          String tError = results[av].almostEqual(pa[av + 1]);
          if (tError.length() > 0)
            throw new WaitThenTryAgainException(
                EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                    + "\n(Details: The axis values for dataVariable=0,axis="
                    + av
                    + "\ndon't equal the axis values for dataVariable="
                    + dv
                    + ",axis="
                    + av
                    + ".\n"
                    + tError
                    + ")");
        }
      }
    }
    return results;
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
          String2.replaceAll(localSourceUrl, "/griddap/", "/files/") + "/" + nextPath + ".csv";
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
    return String2.replaceAll(publicSourceErddapUrl, "/griddap/", "/files/")
        + "/"
        + relativeFileName;
  }

  /**
   * This generates datasets.xml entries for all EDDGrid from a remote ERDDAP. The XML can then be
   * edited by hand (if desired) and added to the datasets.xml file.
   *
   * @param url the base url for the dataset, e.g., "https://coastwatch.pfeg.noaa.gov/erddap"
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(String url, boolean keepOriginalID) throws Throwable {

    url = EDStatic.updateUrls(url); // http: to https:
    String2.log(
        "\n*** EDDGridFromErddap.generateDatasetsXml"
            + "\nurl="
            + url
            + " keepOriginalID="
            + keepOriginalID);

    // make the StringBuilder to hold the results and add documentation
    StringBuilder sb = new StringBuilder();
    /*        sb.append(  //there is very similar text in EDDGridFromErddap
    "<!-- Directions:\n" +
    " * The ready-to-use XML below includes information for all of the EDDGrid datasets\n" +
    "   at the remote ERDDAP " + XML.encodeAsXML(url) + "\n" +
    "   (except for etopo180 and etopo360, which are built into every ERDDAP).\n" +
    " * If you want to add all of these datasets to your ERDDAP, just paste the XML\n" +
    "   into your datasets.xml file.\n" +

    " * The datasetIDs listed below are not the same as the remote datasets' datasetIDs.\n" +
    "   They are generated automatically from the sourceURLs in a way that ensures that they are unique.\n" +
    " * !!!reloadEveryNMinutes is left as the default 10080=oncePerWeek on the assumption\n" +
    "   that the remote ERDDAP will accept your ERDDAP's request to subscribe to the dataset.\n" +
    "   If you don't get emails from the remote ERDDAP asking you to validate your subscription\n" +
    "   requests (perhaps because the remote ERDDAP has the subscription system turned off),\n" +
    "   send an email to the admin asking that s/he add onChange tags to the datasets.\n" +
    "   See the EDDGridFromErddap documentation.\n" +
    " * The XML needed for EDDGridFromErddap in datasets.xml has few options.  See\n" +
    "   https://erddap.github.io/setupDatasetsXml.html#EDDGridFromErddap .\n" +
    "   If you want to alter a dataset's metadata or make other changes to a dataset,\n" +
    "   use EDDGridFromDap to access the dataset instead of EDDGridFromErddap.\n" +
    "-->\n");
    */
    // get the griddap datasets in a json table
    String jsonUrl = url + "/griddap/index.json?page=1&itemsPerPage=1000000";
    Table table = new Table();
    table.readJson(jsonUrl, SSR.getBufferedUrlReader(jsonUrl)); // they are sorted by title

    if (keepOriginalID) {
      // sort by datasetID
      table.ascendingSortIgnoreCase(new int[] {table.findColumnNumber("Dataset ID")});
    }

    PrimitiveArray urlCol = table.findColumn("griddap");
    PrimitiveArray titleCol = table.findColumn("Title");
    PrimitiveArray datasetIdCol = table.findColumn("Dataset ID");

    // go through the rows of the table
    int nRows = table.nRows();
    for (int row = 0; row < nRows; row++) {
      String id = datasetIdCol.getString(row);
      if (id.equals("etopo180") || id.equals("etopo360")) continue;
      // localSourceUrl isn't available (and we generally don't want it)
      String tPublicSourceUrl = urlCol.getString(row);
      if (!keepOriginalID) id = suggestDatasetID(tPublicSourceUrl);
      sb.append(
          "<dataset type=\"EDDGridFromErddap\" datasetID=\""
              + id
              + "\" active=\"true\">\n"
              + "    <!-- "
              + XML.encodeAsXML(String2.replaceAll(titleCol.getString(row), "--", "- - "))
              + " -->\n"
              + "    <sourceUrl>"
              + XML.encodeAsXML(tPublicSourceUrl)
              + "</sourceUrl>\n"
              + "</dataset>\n");
    }

    // get the EDDGridFromErddap datasets
    try {
      jsonUrl = url + "/search/index.json?searchFor=EDDGridFromErddap";
      table = new Table();
      table.readJson(jsonUrl, SSR.getBufferedUrlReader(jsonUrl)); // throws exception if none
      datasetIdCol = table.findColumn("Dataset ID"); // throws exception if none

      sb.append(
          "\n<!-- Of the datasets above, the following datasets are EDDGridFromErddap's at the remote ERDDAP.\n"
              + "It would be best if you contacted the remote ERDDAP's administrator and requested the dataset XML\n"
              + "that is being using for these datasets so your ERDDAP can access the original ERDDAP source.\n"
              + "The remote EDDGridFromErddap datasets are:\n");
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
