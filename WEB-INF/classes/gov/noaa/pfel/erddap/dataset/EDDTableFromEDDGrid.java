/*
 * EDDTableFromEDDGrid Copyright 2013, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableFromEDDGridHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.BufferedReader;
import java.text.MessageFormat;

/**
 * This class creates an EDDTable from an EDDGrid.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2013-03-08
 */
@SaxHandlerClass(EDDTableFromEDDGridHandler.class)
public class EDDTableFromEDDGrid extends EDDTable {

  public static final int DEFAULT_MAX_AXIS0 = 10;
  protected int maxAxis0; // if <=0, no limit

  // If childDataset is a fromErddap dataset from this ERDDAP,
  // childDataset will be kept as null and always refreshed from
  // erddap.gridDatasetHashMap.get(localChildDatasetID).
  protected EDDGrid childDataset;
  private Erddap erddap;
  private String localChildDatasetID;

  /**
   * This constructs an EDDTableFromEDDGrid based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromEDDGrid"&gt;
   *     having just been read.
   * @return an EDDTableFromEDDGrid. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromEDDGrid fromXml(Erddap tErddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableFromEDDGrid(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    EDDGrid tChildDataset = null;
    Attributes tAddGlobalAttributes = null;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tSosOfferingPrefix = null;
    int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    int tUpdateEveryNMillis = 0;
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
      if (localTags.equals("<dataset>")) {
        if ("false".equals(xmlReader.attributeValue("active"))) {
          // skip it - read to </dataset>
          if (verbose)
            String2.log(
                "  skipping datasetID="
                    + xmlReader.attributeValue("datasetID")
                    + " because active=\"false\".");
          while (xmlReader.stackSize() != startOfTagsN + 1
              || !xmlReader.allTags().substring(startOfTagsLength).equals("</dataset>")) {
            xmlReader.nextTag();
            // String2.log("  skippping tags: " + xmlReader.allTags());
          }

        } else {
          if (tChildDataset == null) {
            String tType = xmlReader.attributeValue("type");
            if (tType == null || !tType.startsWith("EDDGrid"))
              throw new SimpleException(
                  "type=\""
                      + tType
                      + "\" is not allowed for the dataset within the EDDTableFromEDDGrid. "
                      + "The type MUST start with \"EDDGrid\".");
            tChildDataset = (EDDGrid) EDD.fromXml(tErddap, tType, xmlReader);
          } else {
            throw new RuntimeException(
                "Datasets.xml error: "
                    + "There can be only one <dataset> defined within an "
                    + "EDDGridFromEDDTableo <dataset>.");
          }
        }

      } else if (localTags.equals("<accessibleTo>")) {
      } else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<accessibleViaFiles>")) {
      } else if (localTags.equals("</accessibleViaFiles>"))
        tAccessibleViaFiles = String2.parseBoolean(content);
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      // updateEveryNMillis isn't supported (ever?). Rely on EDDGrid's update system.
      //            else if (localTags.equals( "<updateEveryNMillis>")) {}
      //            else if (localTags.equals("</updateEveryNMillis>")) tUpdateEveryNMillis =
      // String2.parseInt(content);
      else if (localTags.equals("<onChange>")) {
      } else if (localTags.equals("</onChange>")) tOnChange.add(content);
      else if (localTags.equals("<fgdcFile>")) {
      } else if (localTags.equals("</fgdcFile>")) tFgdcFile = content;
      else if (localTags.equals("<iso19115File>")) {
      } else if (localTags.equals("</iso19115File>")) tIso19115File = content;
      else if (localTags.equals("<sosOfferingPrefix>")) {
      } else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content;
      else if (localTags.equals("<defaultDataQuery>")) {
      } else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content;
      else if (localTags.equals("<defaultGraphQuery>")) {
      } else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content;
      else if (localTags.equals("<addVariablesWhere>")) {
      } else if (localTags.equals("</addVariablesWhere>")) tAddVariablesWhere = content;
      else if (localTags.equals("<addAttributes>")) {
        tAddGlobalAttributes = getAttributesFromXml(xmlReader);
      } else {
        xmlReader.unexpectedTagException();
      }
    }

    return new EDDTableFromEDDGrid(
        tErddap,
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
        tAddGlobalAttributes,
        tReloadEveryNMinutes, // tUpdateEveryNMillis,
        tChildDataset);
  }

  /**
   * The constructor.
   *
   * @throws Throwable if trouble
   */
  public EDDTableFromEDDGrid(
      Erddap tErddap,
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
      Attributes tAddGlobalAttributes,
      int tReloadEveryNMinutes, // int tUpdateEveryNMillis,
      EDDGrid oChildDataset)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromEDDGrid " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromEDDGrid(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDTableFromEDDGrid";
    datasetID = tDatasetID;
    Test.ensureFileNameSafe(datasetID, "datasetID");
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    sosOfferingPrefix = tSosOfferingPrefix;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    // setUpdateEveryNMillis(tUpdateEveryNMillis); //not supported yet (ever?). Rely on EDDGrid's
    // update system.

    if (oChildDataset == null)
      throw new SimpleException(errorInMethod + "The child EDDGrid dataset is missing.");
    if (datasetID.equals(oChildDataset.datasetID()))
      throw new RuntimeException(
          errorInMethod
              + "This dataset's datasetID must not be the same as the child's datasetID.");

    // is oChildDataset a fromErddap from this erddap?
    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = null;
    if (tErddap != null && oChildDataset instanceof EDDGridFromErddap tFromErddap) {
      String tSourceUrl = tFromErddap.getPublicSourceErddapUrl();
      if (EDStatic.urlIsThisComputer(tSourceUrl)) {
        String lcdid = File2.getNameNoExtension(tSourceUrl);
        if (datasetID.equals(lcdid))
          throw new RuntimeException(
              errorInMethod
                  + "This dataset's datasetID must not be the same as the localChild's datasetID.");
        EDDGrid lcd = tErddap.gridDatasetHashMap.get(lcdid);
        if (lcd != null) {
          // yes, use child dataset from this erddap
          // The local dataset will be re-gotten from erddap.gridDatasetHashMap
          //  whenever it is needed.
          if (reallyVerbose) String2.log("  found/using localChildDatasetID=" + lcdid);
          tChildDataset = lcd;
          erddap = tErddap;
          localChildDatasetID = lcdid;
        }
      }
    }
    if (tChildDataset == null) {
      // no, use the oChildDataset supplied to this constructor
      childDataset = oChildDataset; // set instance variable
      tChildDataset = oChildDataset;
    }
    // for rest of constructor, use temporary, stable tChildDataset reference.
    accessibleViaFiles =
        EDStatic.filesActive && tAccessibleViaFiles && tChildDataset.accessibleViaFiles;

    // global attributes
    localSourceUrl = tChildDataset.localSourceUrl;
    sourceGlobalAttributes = tChildDataset.combinedGlobalAttributes;
    addGlobalAttributes = tAddGlobalAttributes == null ? new Attributes() : tAddGlobalAttributes;
    // cdm_data_type=TimeSeries would be nice, but there is no timeseries_id variable
    addGlobalAttributes.add(
        "cdm_data_type",
        (tChildDataset.lonIndex() >= 0 && tChildDataset.latIndex() >= 0) ? "Point" : "Other");
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    String tLicense = combinedGlobalAttributes.getString("license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          "license", String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");

    maxAxis0 = combinedGlobalAttributes.getInt("maxAxis0");
    if (maxAxis0 == Integer.MAX_VALUE) maxAxis0 = DEFAULT_MAX_AXIS0;

    // specify what sourceCanConstrain
    sourceCanConstrainNumericData =
        CONSTRAIN_PARTIAL; // axisVars yes, dataVars no; best to have it doublecheck
    sourceCanConstrainStringData = CONSTRAIN_NO;
    sourceCanConstrainStringRegex = "";

    // quickRestart is handled by contained tChildDataset

    // dataVariables  (axisVars in reverse order, then dataVars)
    int tChildDatasetNAV = tChildDataset.axisVariables.length;
    int tChildDatasetNDV = tChildDataset.dataVariables.length;
    dataVariables = new EDV[tChildDatasetNAV + tChildDatasetNDV];
    for (int dv = 0; dv < dataVariables.length; dv++) {
      // variables in this class just see the destination name/type/... of the tChildDataset
      // variable
      EDV gridVar =
          dv < tChildDatasetNAV
              ? tChildDataset.axisVariables[tChildDatasetNAV - 1 - dv]
              : tChildDataset.dataVariables[dv - tChildDatasetNAV];
      String tSourceName = gridVar.destinationName();
      Attributes tSourceAtts = gridVar.combinedAttributes();
      Attributes tAddAtts = new Attributes();
      String tDataType = gridVar.destinationDataType();
      PAOne tMin = new PAOne(gridVar.destinationMin()); // make/use a copy
      PAOne tMax = new PAOne(gridVar.destinationMax());
      EDV newVar = null;
      if (tSourceName.equals(EDV.LON_NAME)) {
        newVar = new EDVLon(datasetID, tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
        lonIndex = dv;
      } else if (tSourceName.equals(EDV.LAT_NAME)) {
        newVar = new EDVLat(datasetID, tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
        latIndex = dv;
      } else if (tSourceName.equals(EDV.ALT_NAME)) {
        newVar = new EDVAlt(datasetID, tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
        altIndex = dv;
      } else if (tSourceName.equals(EDV.DEPTH_NAME)) {
        newVar = new EDVDepth(datasetID, tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
        depthIndex = dv;
      } else if (tSourceName.equals(EDV.TIME_NAME)) {
        tAddAtts.add("data_min", "" + tMin); // data_min/max have priority
        tAddAtts.add("data_max", "" + tMax); // tMin tMax are epochSeconds
        newVar =
            new EDVTime(
                datasetID,
                tSourceName,
                tSourceAtts,
                tAddAtts,
                tDataType); // this constructor gets source / sets destination actual_range
        timeIndex = dv;
        // currently, there is no EDVTimeStampGridAxis
      } else
        newVar = new EDV(datasetID, tSourceName, "", tSourceAtts, tAddAtts, tDataType, tMin, tMax);

      dataVariables[dv] = newVar;
    }

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid();

    // If the child is a FromErddap, try to subscribe to the remote dataset.
    if (tChildDataset instanceof FromErddap) tryToSubscribeToChildFromErddap(tChildDataset);

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableFromEDDGrid "
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
    return false;
  } // because child EDDGrid usually doesn't know ranges of dataVariables

  /**
   * This returns the childDataset (if not null) or the localChildDataset.
   *
   * @param language the index of the selected language
   */
  public EDDGrid getChildDataset(int language) {
    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = childDataset;
    if (tChildDataset == null) {
      tChildDataset = erddap.gridDatasetHashMap.get(localChildDatasetID);
      if (tChildDataset == null) {
        EDD.requestReloadASAP(localChildDatasetID);
        throw new WaitThenTryAgainException(
            EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                + "\n(underlying local datasetID="
                + localChildDatasetID
                + " not found)");
      }
    }
    return tChildDataset;
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

    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = getChildDataset(language);

    // update the internal childDataset
    return tChildDataset.lowUpdate(language, msg, startUpdateMillis);
  }

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
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

    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = getChildDataset(language);
    // String2.log("\n>> getSourceData tChildDataset type=" + tChildDataset.className);

    // parse the userDapQuery into a DESTINATION query
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    // parseUserDapQuery, not getSourceQueryFromDapQuery.  Get DESTINATION resultsVars+constraints.
    parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables, // doesn't catch constraintVariables that aren't in users requested
        // results
        constraintVariables,
        constraintOps,
        constraintValues,
        false); // repair
    // Non-regex EDVTimeStamp constraintValues will be returned as epochSeconds

    // *** generate the gridDapQuery

    // find (non-regex) min and max of constraints on axis variables
    // work backwards since deleting some
    EDVGridAxis childDatasetAV[] = tChildDataset.axisVariables();
    EDV childDatasetDV[] = tChildDataset.dataVariables();
    int childDatasetNAV = childDatasetAV.length;
    int childDatasetNDV = childDatasetDV.length;
    // min and max desired axis destination values
    double avMin[] = new double[childDatasetNAV];
    double avMax[] = new double[childDatasetNAV];
    for (int av = 0; av < childDatasetNAV; av++) {
      avMin[av] = childDatasetAV[av].destinationMinDouble(); // time is epochSeconds
      avMax[av] = childDatasetAV[av].destinationMaxDouble();
    }
    boolean hasAvConstraints = false; // only true if constraints are more than av min max
    StringArray constraintsDvNames = new StringArray(); // unique
    for (int c = 0; c < constraintVariables.size(); c++) {
      String conVar = constraintVariables.get(c);
      String conOp = constraintOps.get(c);
      String conVal = constraintValues.get(c);
      int av = String2.indexOf(tChildDataset.axisVariableDestinationNames(), conVar);
      if (av < 0) {
        if (constraintsDvNames.indexOf(conVar) < 0) // if not already in the list
        constraintsDvNames.add(conVar);
        continue;
      }
      EDVGridAxis edvga = childDatasetAV[av];
      if (conOp.equals(PrimitiveArray.REGEX_OP)) {
        // FUTURE: this could be improved to find the range of matching axis values
      } else {
        boolean avIsTimeStamp = edvga instanceof EDVTimeStampGridAxis;
        double oldAvMin = avMin[av];
        double oldAvMax = avMax[av];
        double conValD = String2.parseDouble(conVal);
        boolean passed = true; // look for some aspect that doesn't pass
        if (!conOp.equals("!=") && Double.isNaN(conValD)) {
          passed = false;
        } else {
          // > >= < <= (and =somethingOutOfRange) were tested in EDDTable.parseUserDapQuery
          if (conOp.equals("=")) {
            int si = edvga.destinationToClosestIndex(conValD);
            if (si < 0) passed = false;
            else {
              double destVal = edvga.destinationValue(si).getDouble(0);
              passed =
                  avIsTimeStamp
                      ? conValD == destVal
                      : // exact
                      Math2.almostEqual(5, conValD, destVal); // fuzzy, biased towards passing
              avMin[av] = Math.max(avMin[av], conValD);
              avMax[av] = Math.min(avMax[av], conValD);
            }
          } else if (conOp.equals("!=")) {
            // not very useful
          }
        }
        if (!passed)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  MustBe.THERE_IS_NO_DATA
                      + " ("
                      + MessageFormat.format(
                          EDStatic.queryErrorNeverTrueAr[0], conVar + conOp + conValD)
                      + ")",
                  MustBe.THERE_IS_NO_DATA
                      + " ("
                      + MessageFormat.format(
                          EDStatic.queryErrorNeverTrueAr[language], conVar + conOp + conValD)
                      + ")"));

        if (oldAvMin != avMin[av] || oldAvMax != avMax[av]) hasAvConstraints = true;
      }
    }

    // generate the griddap avConstraints
    String avConstraints[] = new String[childDatasetNAV];
    StringBuilder allAvConstraints = new StringBuilder();
    for (int av = 0; av < childDatasetNAV; av++) {
      EDVGridAxis edvga = childDatasetAV[av];
      // are the constraints impossible?
      if (avMin[av] > avMax[av])
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                MustBe.THERE_IS_NO_DATA
                    + " "
                    + MessageFormat.format(
                        EDStatic.queryErrorNeverTrueAr[0],
                        edvga.destinationName()
                            + ">="
                            + avMin[av]
                            + " and "
                            + edvga.destinationName()
                            + "<="
                            + avMax[av]),
                MustBe.THERE_IS_NO_DATA
                    + " "
                    + MessageFormat.format(
                        EDStatic.queryErrorNeverTrueAr[language],
                        edvga.destinationName()
                            + ">="
                            + avMin[av]
                            + " and "
                            + edvga.destinationName()
                            + "<="
                            + avMax[av])));
      if (edvga.isAscending()) avConstraints[av] = "[(" + avMin[av] + "):(" + avMax[av] + ")]";
      else avConstraints[av] = "[(" + avMax[av] + "):(" + avMin[av] + ")]";
      allAvConstraints.append(avConstraints[av]);
    }
    if (debugMode) String2.log(">>allAvConstraints=" + allAvConstraints);

    // does resultsVariables include any axis or data variables?
    StringArray resultsAvNames = new StringArray();
    StringArray resultsDvNames = new StringArray();
    for (int rv = 0; rv < resultsVariables.size(); rv++) {
      String rvName = resultsVariables.get(rv);
      if (String2.indexOf(tChildDataset.axisVariableDestinationNames(), rvName) >= 0)
        resultsAvNames.add(rvName);
      if (String2.indexOf(tChildDataset.dataVariableDestinationNames(), rvName) >= 0)
        resultsDvNames.add(rvName);
    }

    StringBuilder gridDapQuery = new StringBuilder();
    String gridRequestUrl =
        "/griddap/" + tChildDataset.datasetID() + ".dods"; // after EDStatic.baseUrl, before '?'

    if (resultsDvNames.size() > 0 || constraintsDvNames.size() > 0) {
      // handle a request for 1+ data variables (in results or constraint variables)
      // e.g. lat,lon,time,sst&sst>37    or lat,lon,time&sst>37
      for (int dvi = 0; dvi < resultsDvNames.size(); dvi++) { // the names are unique
        if (gridDapQuery.length() > 0) gridDapQuery.append(',');
        gridDapQuery.append(resultsDvNames.get(dvi) + allAvConstraints);
      }
      for (int dvi = 0; dvi < constraintsDvNames.size(); dvi++) { // the names are unique
        if (resultsDvNames.indexOf(constraintsDvNames.get(dvi)) < 0) {
          if (gridDapQuery.length() > 0) gridDapQuery.append(',');
          gridDapQuery.append(constraintsDvNames.get(dvi) + allAvConstraints);
        }
      }
      if (debugMode) String2.log(">>nDvNames > 0, gridDapQuery=" + gridDapQuery);
      GridDataAccessor gda =
          new GridDataAccessor(
              language,
              tChildDataset,
              gridRequestUrl,
              gridDapQuery.toString(),
              true, // rowMajor
              false); // convertToNaN (would be true, but TableWriterSeparatedValue will do it)

      // test maxAxis0
      if (maxAxis0 >= 1) {
        int nAxis0 = gda.totalIndex().shape()[0];
        if (maxAxis0 < nAxis0) {
          String ax0Name = tChildDataset.axisVariableDestinationNames()[0];
          throw new SimpleException(
              Math2.memoryTooMuchData
                  + ": Your request for data from "
                  + nAxis0
                  + " axis[0] ("
                  + ax0Name
                  + ") values exceeds the maximum allowed for this dataset ("
                  + maxAxis0
                  + "). Please add tighter constraints on the "
                  + ax0Name
                  + " variable.");
        }
      }

      EDV queryDV[] = gda.dataVariables();
      int nQueryDV = queryDV.length;
      EDV sourceTableVars[] = new EDV[childDatasetNAV + nQueryDV];
      for (int av = 0; av < childDatasetNAV; av++)
        sourceTableVars[av] = dataVariables[childDatasetNAV - av - 1];
      for (int dv = 0; dv < nQueryDV; dv++)
        sourceTableVars[childDatasetNAV + dv] =
            findDataVariableByDestinationName(queryDV[dv].destinationName());

      // make a table to hold a chunk of the results
      int chunkNRows = EDStatic.partialRequestMaxCells / (childDatasetNAV + nQueryDV);
      Table tTable =
          makeEmptySourceTable(
              sourceTableVars,
              chunkNRows); // source table, but source here is tChildDataset's destination
      PrimitiveArray paAr[] = new PrimitiveArray[tTable.nColumns()];
      PAOne paOne[] = new PAOne[tTable.nColumns()];
      for (int col = 0; col < tTable.nColumns(); col++) {
        paAr[col] = tTable.getColumn(col);
        paOne[col] = new PAOne(paAr[col]);
      }

      // walk through it, periodically saving to tableWriter
      int cumNRows = 0;
      while (gda.increment()) {
        for (int av = 0; av < childDatasetNAV; av++)
          gda.getAxisValueAsPAOne(av, paOne[av]).addTo(paAr[av]);
        for (int dv = 0; dv < nQueryDV; dv++)
          gda.getDataValueAsPAOne(dv, paOne[childDatasetNAV + dv])
              .addTo(paAr[childDatasetNAV + dv]);
        if (++cumNRows >= chunkNRows) {
          if (debugMode) String2.log(tTable.dataToString(5));
          if (Thread.currentThread().isInterrupted())
            throw new SimpleException(
                "EDDTableFromEDDGrid.getDataForDapQuery" + EDStatic.caughtInterruptedAr[0]);

          standardizeResultsTable(
              language,
              requestUrl, // applies all constraints
              userDapQuery,
              tTable);
          tableWriter.writeSome(tTable);
          tTable = makeEmptySourceTable(sourceTableVars, chunkNRows);
          for (int col = 0; col < tTable.nColumns(); col++) paAr[col] = tTable.getColumn(col);
          cumNRows = 0;
          if (tableWriter.noMoreDataPlease) {
            tableWriter.logCaughtNoMoreDataPlease(datasetID);
            break;
          }
        }
      }
      gda.releaseResources();

      // finish
      if (tTable.nRows() > 0) {
        standardizeResultsTable(
            language,
            requestUrl, // applies all constraints
            userDapQuery,
            tTable);
        tableWriter.writeSome(tTable);
      }
      tableWriter.finish();

    } else if (resultsAvNames.size() >= 1) {
      // handle a request for multiple axis variables (no data variables anywhere)
      // gather the active edvga
      int nActiveEdvga = resultsAvNames.size();
      EDVGridAxis activeEdvga[] = new EDVGridAxis[nActiveEdvga];
      for (int aav = 0; aav < nActiveEdvga; aav++) {
        int av =
            String2.indexOf(tChildDataset.axisVariableDestinationNames(), resultsAvNames.get(aav));
        activeEdvga[aav] = childDatasetAV[av];
        if (aav > 0) gridDapQuery.append(',');
        gridDapQuery.append(resultsAvNames.get(aav) + avConstraints[av]);
      }

      // make modifiedUserDapQuery, resultsAvNames + just constraints on results axis vars
      StringBuilder modifiedUserDapQuery = new StringBuilder(resultsAvNames.toCSVString());
      for (int c = 0; c < constraintVariables.size(); c++) {
        String conVar = constraintVariables.get(c);
        String conOp = constraintOps.get(c);
        String conVal = constraintValues.get(c);
        int rv = resultsAvNames.indexOf(conVar);
        if (rv < 0) continue;
        int av = String2.indexOf(tChildDataset.axisVariableDestinationNames(), conVar);
        if (av < 0) continue;
        modifiedUserDapQuery.append(
            "&"
                + conVar
                + conOp
                + (conOp.equals(PrimitiveArray.REGEX_OP) ? String2.toJson(conVal) : conVal));
      }
      if (debugMode)
        String2.log(
            ">>resultsAvNames.size() > 1, gridDapQuery="
                + gridDapQuery
                + "\n  modifiedUserDapQuery="
                + modifiedUserDapQuery);

      // parse the gridDapQuery
      StringArray tDestinationNames = new StringArray();
      IntArray tConstraints = new IntArray(); // start,stop,stride for each tDestName
      tChildDataset.parseAxisDapQuery(
          language, gridDapQuery.toString(), tDestinationNames, tConstraints, false);

      // figure out the shape and make the NDimensionalIndex
      int ndShape[] = new int[nActiveEdvga]; // an array with the size of each active av
      for (int aav = 0; aav < nActiveEdvga; aav++)
        ndShape[aav] = tConstraints.get(aav * 3 + 2) - tConstraints.get(aav * 3 + 0) + 1;
      NDimensionalIndex ndIndex = new NDimensionalIndex(ndShape);

      // make a table to hold a chunk of the results
      int chunkNRows = EDStatic.partialRequestMaxCells / nActiveEdvga;
      Table tTable = new Table(); // source table, but source here is tChildDataset's destination
      PrimitiveArray paAr[] = new PrimitiveArray[nActiveEdvga];
      for (int aav = 0; aav < nActiveEdvga; aav++) {
        // this class only sees tChildDataset dest type and destName
        paAr[aav] =
            PrimitiveArray.factory(activeEdvga[aav].destinationDataPAType(), chunkNRows, false);
        tTable.addColumn(activeEdvga[aav].destinationName(), paAr[aav]);
      }

      // walk through it, periodically saving to tableWriter
      int cumNRows = 0;
      int current[] = ndIndex.getCurrent();
      while (ndIndex.increment()) {
        for (int aav = 0; aav < nActiveEdvga; aav++)
          paAr[aav].addDouble(
              activeEdvga[aav].destinationDouble(
                  tConstraints.get(aav * 3 + 0) + current[aav])); // baseIndex + offsetIndex
        if (++cumNRows >= chunkNRows) {
          if (Thread.currentThread().isInterrupted())
            throw new SimpleException(
                "EDDTableFromDatabase.getDataForDapQuery" + EDStatic.caughtInterruptedAr[0]);

          standardizeResultsTable(
              language,
              requestUrl, // applies all constraints
              modifiedUserDapQuery.toString(),
              tTable);
          tableWriter.writeSome(tTable);
          // ??? no need for make a new table, colOrder may have changed, but PaAr hasn't
          tTable.removeAllRows();
          cumNRows = 0;
          if (tableWriter.noMoreDataPlease) {
            tableWriter.logCaughtNoMoreDataPlease(datasetID);
            break;
          }
        }
      }

      // finish
      if (tTable.nRows() > 0) {
        standardizeResultsTable(
            language,
            requestUrl, // applies all constraints
            modifiedUserDapQuery.toString(),
            tTable);
        tableWriter.writeSome(tTable);
      }
      tableWriter.finish();

    } else {
      // if get here, no results variables?! error should have been thrown before
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.queryErrorAr[0] + " No results variables?!",
              EDStatic.queryErrorAr[language] + " No results variables?!"));
    }
  }

  /**
   * This returns a fileTable with valid files (or null if unavailable or any trouble). This is a
   * copy of any internal data, so client can modify the contents.
   *
   * @param language the index of the selected language
   * @param nextPath is the partial path (with trailing slash) to be appended onto the local fileDir
   *     (or wherever files are, even url).
   * @return null if trouble, or Object[3] [0] is a sorted table with file "Name" (String), "Last
   *     modified" (long millis), "Size" (long), and "Description" (String, but usually no content),
   *     [1] is a sorted String[] with the short names of directories that are 1 level lower, and
   *     [2] is the local directory corresponding to this (or null, if not a local dir).
   */
  @Override
  public Object[] accessibleViaFilesFileTable(int language, String nextPath) {
    if (!accessibleViaFiles) return null;
    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = getChildDataset(language);
    return tChildDataset.accessibleViaFilesFileTable(language, nextPath);
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
    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = getChildDataset(language);
    return tChildDataset.accessibleViaFilesGetLocal(language, relativeFileName);
  }

  /**
   * This runs generateDatasetsXmlFromErddapCatalog.
   *
   * @param language the index of the selected language
   * @param oPublicSourceUrl A complete URL of an ERDDAP (ending with "/erddap/").
   * @param datasetIDRegex E.g., ".*" for all dataset Names.
   * @return a String with the results
   */
  public static String generateDatasetsXml(
      int language, String oPublicSourceUrl, String datasetIDRegex, int tMaxAxis0)
      throws Throwable {

    String2.log(
        "*** EDDTableFromEDDGrid.generateDatasetsXmlFromErddapCatalog(" + oPublicSourceUrl + ")");
    if (tMaxAxis0 <= -1 || tMaxAxis0 == Integer.MAX_VALUE) tMaxAxis0 = DEFAULT_MAX_AXIS0;

    if (oPublicSourceUrl == null)
      throw new RuntimeException(String2.ERROR + ": 'localSourceUrl' is null.");
    if (!oPublicSourceUrl.endsWith("/erddap/") && !oPublicSourceUrl.endsWith("/cwexperimental/"))
      throw new RuntimeException(String2.ERROR + ": 'localSourceUrl' doesn't end with '/erddap/'.");

    // get table with datasetID minLon, maxLon
    String query =
        oPublicSourceUrl
            + "tabledap/allDatasets.csv"
            + "?datasetID,title,summary,griddap"
            + "&dataStructure="
            + SSR.minimalPercentEncode("\"grid\"")
            + (String2.isSomething(datasetIDRegex)
                ? "&datasetID=" + SSR.minimalPercentEncode("~\"" + datasetIDRegex + "\"")
                : "")
            + "&orderBy(%22datasetID%22)";
    BufferedReader lines = null;
    try {
      lines = SSR.getBufferedUrlReader(query);
    } catch (Throwable t) {
      if (t.toString().indexOf("no data") >= 0)
        return "<!-- No griddap datasets at that ERDDAP match that regex. -->\n";
      throw t;
    }
    Table table = new Table();
    table.readASCII(query, lines, "", "", 0, 2, "", null, null, null, null, false); // simplify
    // String2.log(table.dataToString());
    PrimitiveArray datasetIDPA = table.findColumn("datasetID");
    PrimitiveArray titlePA = table.findColumn("title");
    PrimitiveArray summaryPA = table.findColumn("summary");
    PrimitiveArray griddapPA = table.findColumn("griddap");
    StringBuilder sb = new StringBuilder();
    int nRows = table.nRows();
    for (int row = 0; row < nRows; row++) {
      String tDatasetID = datasetIDPA.getString(row);
      String tTitle = titlePA.getString(row) + ", (As A Table)";
      String tSummary =
          (tMaxAxis0 > 0
                  ? MessageFormat.format(
                          EDStatic.EDDTableFromEDDGridSummaryAr[language], tDatasetID, tMaxAxis0)
                      + "\n"
                  : "")
              + summaryPA.getString(row);
      sb.append(
          "<dataset type=\"EDDTableFromEDDGrid\" datasetID=\""
              + tDatasetID
              + "_AsATable\" active=\"true\">\n"
              + "    <addAttributes>\n"
              + "        <att name=\"maxAxis0\" type=\"int\">"
              + tMaxAxis0
              + "</att>\n"
              + "        <att name=\"title\">"
              + XML.encodeAsXML(tTitle)
              + "</att>\n"
              + "        <att name=\"summary\">"
              + XML.encodeAsXML(tSummary)
              + "</att>\n"
              + "    </addAttributes>\n"
              + "    <dataset type=\"EDDGridFromErddap\" datasetID=\""
              + datasetIDPA.getString(row)
              + "_AsATableChild\">\n"
              +
              // set updateEveryNMillis?  frequent if local ERDDAP, less frequent if remote?
              "        <sourceUrl>"
              + XML.encodeAsXML(griddapPA.getString(row))
              + "</sourceUrl>\n"
              + "    </dataset>\n"
              + "</dataset>\n\n");
    }
    return sb.toString();
  }
}
