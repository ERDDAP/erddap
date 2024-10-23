/*
 * EDDTableAggregateRows Copyright 2016, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableAggregateRowsHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.util.ArrayList;

/**
 * This class creates an EDDTable by aggregating a list of EDDTables that have the same variables.
 *
 * <p>This doesn't have generateDatasetsXml. Use the appropriate EDDTableFromX.generateDatasetsXml
 * for each child, then wrap them up with EDDTableAggregateRows.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2016-02-19
 */
@SaxHandlerClass(EDDTableAggregateRowsHandler.class)
public class EDDTableAggregateRows extends EDDTable {

  protected int nChildren;
  protected EDDTable children[]; // [c] is null if local fromErddap
  // If oChildren[c] is a fromErddap dataset from this ERDDAP,
  // children[c] will be kept as null and always refreshed from
  // erddap.tableDatasetHashMap.get(localChildrenID).
  private Erddap erddap;
  private String localChildrenID[]; // [c] is null if NOT local fromErddap
  private boolean knowsActualRange;

  /**
   * This constructs an EDDTableAggregateRows based on the information in an .xml file.
   *
   * @param tErddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableAggregateRows"&gt;
   *     having just been read.
   * @return an EDDTableAggregateRows. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableAggregateRows fromXml(Erddap tErddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableAggregateRows(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    ArrayList<EDDTable> tChildren = new ArrayList();
    Attributes tAddGlobalAttributes = null;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
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
          String tType = xmlReader.attributeValue("type");
          if (tType == null || !tType.startsWith("EDDTable"))
            throw new SimpleException(
                "type=\""
                    + tType
                    + "\" is not allowed for the dataset within the EDDTableAggregateRows. "
                    + "The type MUST start with \"EDDTable\".");
          tChildren.add((EDDTable) EDD.fromXml(tErddap, tType, xmlReader));
        }

      } else if (localTags.equals("<accessibleTo>")) {
      }
      // accessibleTo overwrites any child accessibleTo
      else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<updateEveryNMillis>")) {
      } else if (localTags.equals("</updateEveryNMillis>"))
        tUpdateEveryNMillis = String2.parseInt(content);
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

    int nChildren = tChildren.size();
    EDDTable ttChildren[] = new EDDTable[nChildren];
    for (int i = 0; i < nChildren; i++) ttChildren[i] = tChildren.get(i);

    return new EDDTableAggregateRows(
        tErddap,
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
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        ttChildren);
  }

  /**
   * The constructor.
   *
   * @throws Throwable if trouble
   */
  public EDDTableAggregateRows(
      Erddap tErddap,
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
      Attributes tAddGlobalAttributes,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      EDDTable oChildren[])
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableAggregateRows " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableAggregateRows(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDTableAggregateRows";
    datasetID = tDatasetID;
    if (datasetID == null || datasetID.length() == 0)
      throw new RuntimeException(errorInMethod + "The datasetID wasn't specified.");
    if (oChildren == null || oChildren.length == 0)
      throw new RuntimeException(errorInMethod + "The EDDTable children datasets are missing.");
    nChildren = oChildren.length;
    children = new EDDTable[nChildren]; // the instance datasets if not local fromErddap
    localChildrenID = new String[nChildren]; // the instance datasetIDs if   local fromErddap
    EDDTable tChildren[] = new EDDTable[nChildren]; // stable local instance
    int ndv = -1;
    knowsActualRange = true; // only true if true for all children
    for (int c = 0; c < nChildren; c++) {
      if (oChildren[c] == null)
        throw new RuntimeException(errorInMethod + "The child dataset[" + c + "] is null.");
      if (datasetID.equals(oChildren[c].datasetID()))
        throw new RuntimeException(
            errorInMethod
                + "The child dataset["
                + c
                + "] datasetID="
                + datasetID
                + " must be different from the EDDTableAggregateRows's datasetID.");
      if (tErddap != null && oChildren[c] instanceof EDDTableFromErddap tFromErddap) {
        String tSourceUrl = tFromErddap.getPublicSourceErddapUrl();
        if (EDStatic.urlIsThisComputer(tSourceUrl)) {
          String lcdid = File2.getNameNoExtension(tSourceUrl);
          if (datasetID.equals(lcdid))
            throw new RuntimeException(
                errorInMethod
                    + ": The parent datasetID must not be the same as the localChild's datasetID.");
          EDDTable lcd = tErddap.tableDatasetHashMap.get(lcdid);
          if (lcd != null) {
            // yes, use child dataset from local Erddap
            // The local dataset will be re-gotten from erddap.gridDatasetHashMap
            //  whenever it is needed.
            if (reallyVerbose) String2.log("  found/using localChildDatasetID=" + lcdid);
            tChildren[c] = lcd;
            erddap = tErddap;
            localChildrenID[c] = lcdid;
          }
        }
      }
      if (tChildren[c] == null) {
        // no, use the  oChildren[c] supplied to this constructor
        children[c] = oChildren[c]; // set instance variable
        tChildren[c] = oChildren[c];
      }
      int cndv = tChildren[c].dataVariables.length;
      if (c == 0) {
        ndv = cndv;
      } else if (ndv != cndv) {
        throw new RuntimeException(
            errorInMethod
                + "Child dataset["
                + c
                + "] datasetID="
                + datasetID
                + " has a diffent number of variables ("
                + cndv
                + ") than the first child ("
                + ndv
                + ").");
      }

      if (!tChildren[c].knowsActualRange()) knowsActualRange = false;
    }
    // for rest of constructor, use temporary, stable tChildren reference.

    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    sosOfferingPrefix = tSosOfferingPrefix;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    setUpdateEveryNMillis(tUpdateEveryNMillis);

    // get most info from child0
    EDDTable child0 = tChildren[0];

    // global attributes
    sourceGlobalAttributes = child0.combinedGlobalAttributes;
    addGlobalAttributes = tAddGlobalAttributes == null ? new Attributes() : tAddGlobalAttributes;
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    String tLicense = combinedGlobalAttributes.getString("license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          "license", String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");

    // specify what sourceCanConstrain
    // but irrelevant for this class --
    //  each child will parse and deal with user request its own way
    sourceCanConstrainNumericData = child0.sourceCanConstrainNumericData;
    sourceCanConstrainStringData = child0.sourceCanConstrainStringData;
    sourceCanConstrainStringRegex = child0.sourceCanConstrainStringRegex;

    // quickRestart is handled by tChildren

    // dataVariables
    dataVariables = new EDV[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      // variables in this class just see the destination name/type/... of the child0 variable
      EDV childVar = child0.dataVariables[dv];
      String tSourceName = childVar.destinationName();
      Attributes tSourceAtts = childVar.combinedAttributes();
      Attributes tAddAtts = new Attributes();
      String tDataType = childVar.destinationDataType();
      String tUnits = childVar.units();
      double tMV = childVar.destinationMissingValue();
      double tFV = childVar.destinationFillValue();
      PAOne tMin = childVar.destinationMin();
      PAOne tMax = childVar.destinationMax();

      if (!knowsActualRange) {
        tMin = new PAOne(tMin.paType(), ""); // same type, but value=NaN
        tMax = new PAOne(tMax.paType(), "");
      }

      // ensure all tChildren are consistent
      for (int c = 1; c < nChildren; c++) {
        EDV cEdv = tChildren[c].dataVariables[dv];
        String cName = cEdv.destinationName();
        if (!Test.equal(tSourceName, cName))
          throw new RuntimeException(
              errorInMethod
                  + "child dataset["
                  + c
                  + "]="
                  + tChildren[c].datasetID()
                  + " variable #"
                  + dv
                  + " destinationName="
                  + cName
                  + " should have destinationName="
                  + tSourceName
                  + ".");

        String hasADifferent =
            errorInMethod
                + "child dataset["
                + c
                + "]="
                + tChildren[c].datasetID()
                + " variable="
                + tSourceName
                + " has a different ";
        String than =
            " than the same variable in child dataset[0]=" + tChildren[0].datasetID() + " ";

        String cDataType = cEdv.destinationDataType();
        if (!Test.equal(tDataType, cDataType))
          throw new RuntimeException(
              hasADifferent + "dataType=" + cDataType + than + "dataType=" + tDataType + ".");
        String cUnits = cEdv.units();
        if (!Test.equal(tUnits, cUnits))
          throw new RuntimeException(
              hasADifferent + "units=" + cUnits + than + "units=" + tUnits + ".");
        double cMV = cEdv.destinationMissingValue();
        if (!Test.equal(tMV, cMV)) // 9 digits
        throw new RuntimeException(
              hasADifferent + "missing_value=" + cMV + than + "missing_value=" + tMV + ".");
        double cFV = cEdv.destinationFillValue();
        if (!Test.equal(tFV, cFV)) // 9 digits
        throw new RuntimeException(
              hasADifferent + "_FillValue=" + cFV + than + "_FillValue=" + tFV + ".");

        // and get the minimum min and maximum max
        if (knowsActualRange) { // only gather if all children knowActualRange
          tMin = tMin.min(cEdv.destinationMin());
          tMax = tMax.max(cEdv.destinationMax());
        }
      }

      /// override actual_range with min/max calculated on all chidren
      if (!tMin.isMissingValue() && !tMax.isMissingValue()) {
        PrimitiveArray pa = PrimitiveArray.factory(childVar.destinationDataPAType(), 2, false);
        pa.addPAOne(tMin);
        pa.addPAOne(tMax);
        tSourceAtts.set("actual_range", pa);
      }

      // make the variable
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
      } else if (tSourceName.equals(EDV.TIME_NAME)) { // do time before timeStamp
        newVar =
            new EDVTime(
                datasetID,
                tSourceName,
                tSourceAtts,
                tAddAtts,
                tDataType); // this constructor gets source / sets destination actual_range
        timeIndex = dv;
      } else if (EDVTimeStamp.hasTimeUnits(tSourceAtts, tAddAtts)) {
        newVar =
            new EDVTimeStamp(
                datasetID,
                tSourceName,
                "",
                tSourceAtts,
                tAddAtts,
                tDataType); // this constructor gets source / sets destination actual_range
      } else
        newVar = new EDV(datasetID, tSourceName, "", tSourceAtts, tAddAtts, tDataType, tMin, tMax);

      newVar.setActualRangeFromDestinationMinMax();
      dataVariables[dv] = newVar;
    }

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid();

    // If the child is a FromErddap, try to subscribe to the remote dataset.
    for (int c = 0; c < nChildren; c++) {
      if (tChildren[c] instanceof FromErddap) tryToSubscribeToChildFromErddap(tChildren[0]);
    }

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableAggregateRows "
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
  } // depends on the children

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

    // update the children
    boolean anyChange = false;
    int ndv = dataVariables.length;

    PAOne tMin[] = new PAOne[ndv];
    PAOne tMax[] = new PAOne[ndv];
    EDDTable tChild = getChild(language, 0);
    for (int dvi = 0; dvi < ndv; dvi++) {
      EDV cEdv = tChild.dataVariables[dvi];
      tMin[dvi] = new PAOne(cEdv.destinationMin().paType(), ""); // NaN, but of correct type
      tMax[dvi] = new PAOne(cEdv.destinationMax().paType(), "");
    }

    for (int c = 0; c < nChildren; c++) {
      tChild = getChild(language, c);
      if (tChild.lowUpdate(language, msg, startUpdateMillis)) anyChange = true;

      if (knowsActualRange) {
        // update all dataVariable's destinationMin/Max
        for (int dvi = 0; dvi < ndv; dvi++) {
          EDV cEdv = tChild.dataVariables[dvi];
          tMin[dvi] = tMin[dvi].min(cEdv.destinationMin());
          tMax[dvi] = tMax[dvi].max(cEdv.destinationMax());
        }
      }
    }
    for (int dvi = 0; dvi < ndv; dvi++) {
      dataVariables[dvi].setDestinationMinMax(tMin[dvi], tMax[dvi]);
      dataVariables[dvi].setActualRangeFromDestinationMinMax();
    }

    return anyChange;
  }

  /**
   * This returns a list of childDatasetIDs. Most dataset types don't have any children. A few, like
   * EDDGridSideBySide do, so they overwrite this method to return the IDs.
   *
   * @return a new list of childDatasetIDs.
   */
  @Override
  public StringArray childDatasetIDs() {
    StringArray sa = new StringArray();
    try {
      for (int i = 0; i < nChildren; i++) {
        if (children[i] != null) sa.add(children[i].datasetID());
        else if (localChildrenID[i] != null) sa.add(localChildrenID[i]);
      }
    } catch (Exception e) {
      String2.log("Error caught in edd.childDatasetIDs(): " + MustBe.throwableToString(e));
    }
    return sa;
  }

  /** This gets the specified child dataset, whether local fromErddap or otherwise. */
  private EDDTable getChild(int language, int c) {
    EDDTable tChild = children[c];
    if (tChild == null) {
      tChild = erddap.tableDatasetHashMap.get(localChildrenID[c]);
      if (tChild == null) {
        EDD.requestReloadASAP(localChildrenID[c]);
        throw new WaitThenTryAgainException(
            EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                + "\n(underlying local child["
                + c
                + "] datasetID="
                + localChildrenID[c]
                + " not found)");
      }
    }
    return tChild;
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

    // tableWriter
    tableWriter.ignoreFinish = true;

    // pass the request to each child, and accumulate the results
    for (int c = 0; c < nChildren; c++) {
      try {
        // get data from this child. It's nice that:
        // * each child can parse and handle parts its own way
        //  e.g., sourceCanConstrainStringData
        // * each child handles standardizeResultsTable and applies constraints.
        // * each child has the same destination names and units for the same vars.
        // * this will call tableWriter.finish(), but it will be ignored
        getChild(language, c)
            .getDataForDapQuery(
                language, EDStatic.loggedInAsSuperuser, requestUrl, userDapQuery, tableWriter);

        // no more data?
        if (tableWriter.noMoreDataPlease) break;

      } catch (Throwable t) {
        // no results is okay
        String msg = t.toString();
        if (msg.indexOf(MustBe.THERE_IS_NO_DATA) >= 0) continue;

        // rethrow, including WaitThenTryAgainException
        throw t;
      }
    }

    // finish
    tableWriter.ignoreFinish = false;
    tableWriter.finish();
  }
}
