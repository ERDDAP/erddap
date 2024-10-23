/*
 * EDDGridSideBySide Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridSideBySideHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class represents a grid dataset created by aggregating two or more datasets side by side. So
 * the resulting dataset has all the variables of the child datasets. This class makes a new axis[0]
 * with the union of all axis[0] values from all children. All children must have the same values
 * for axis[1+].
 *
 * <p>If there is an exception while creating the first child, this throws the exception. If there
 * is an exception while creating other children, this emails EDStatic.emailEverythingToCsv and
 * continues.
 *
 * <p>Children created by this method are held privately. They are not separately accessible
 * datasets (e.g., by queries or by flag files).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-02-04
 */
@SaxHandlerClass(EDDGridSideBySideHandler.class)
public class EDDGridSideBySide extends EDDGrid {

  protected EDDGrid childDatasets[];
  protected int childStopsAt[]; // the last valid dataVariables index for each childDataset
  protected IntArray indexOfAxis0Value[]; // an IntArray for each child; a row for each axis0 value

  /**
   * This is used to test equality of axis values. 0=no testing (not recommended). &gt;18 does exact
   * test. default=20. 1-18 tests that many digets for doubles and hidiv(n,2) for floats.
   */
  protected int matchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

  /**
   * This constructs an EDDGridSideBySide based on the information in an .xml file. Only the global
   * attributes from the first dataset are used for the composite dataset.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridSideBySide"&gt; having
   *     just been read.
   * @return an EDDGridSideBySide. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridSideBySide fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridSideBySide(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");

    // data to be obtained while reading xml
    ArrayList tChildDatasets = new ArrayList();
    StringBuilder messages = new StringBuilder();
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
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
      if (reallyVerbose) String2.log("  tags=" + tags + content);
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
          try {
            EDD edd = EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);
            if (edd instanceof EDDGrid) {
              tChildDatasets.add(edd);
            } else {
              throw new RuntimeException(
                  "The datasets defined in an "
                      + "EDDGridSideBySide must be a subclass of EDDGrid.");
            }
          } catch (Throwable t) {
            // exceptions for first child are serious (it has parent's metadata)
            if (tChildDatasets.size() == 0) throw t;

            // exceptions for others are noted, but construction continues
            String2.log(MustBe.throwableToString(t));
            messages.append(MustBe.throwableToString(t) + "\n");

            // read to </dataset>
            while (xmlReader.stackSize() != startOfTagsN + 1
                || !xmlReader.allTags().substring(startOfTagsLength).equals("</dataset>")) {
              xmlReader.nextTag();
              // String2.log("  skippping tags: " + xmlReader.allTags());
            }
          }
        }

      } else if (localTags.equals("<onChange>")) {
      } else if (localTags.equals("</onChange>")) tOnChange.add(content);
      else if (localTags.equals("<matchAxisNDigits>")) {
      } else if (localTags.equals("</matchAxisNDigits>"))
        tMatchAxisNDigits = String2.parseInt(content, DEFAULT_MATCH_AXIS_N_DIGITS);
      else if (localTags.equals("<ensureAxisValuesAreEqual>")) {
      } // deprecated
      else if (localTags.equals("</ensureAxisValuesAreEqual>"))
        tMatchAxisNDigits = String2.parseBoolean(content) ? 20 : 0;
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
      else xmlReader.unexpectedTagException();
    }
    if (messages.length() > 0) {
      EDStatic.email(
          EDStatic.emailEverythingToCsv,
          "Error in EDDGridSideBySide constructor for " + tDatasetID,
          messages.toString());
    }

    EDDGrid tcds[] = new EDDGrid[tChildDatasets.size()];
    for (int c = 0; c < tChildDatasets.size(); c++) tcds[c] = (EDDGrid) tChildDatasets.get(c);

    // make the main dataset based on the information gathered
    return new EDDGridSideBySide(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tAccessibleViaFiles,
        tMatchAxisNDigits,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tcds,
        tnThreads,
        tDimensionValuesInMemory);
  }

  /**
   * The constructor. The axisVariables must be the same and in the same order for each
   * dataVariable.
   *
   * @param tDatasetID
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
   * @param tChildDatasets
   * @throws Throwable if trouble
   */
  public EDDGridSideBySide(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
      boolean tAccessibleViaFiles,
      int tMatchAxisNDigits,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      EDDGrid tChildDatasets[],
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridSideBySide " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridGridSideBySide(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDGridSideBySide";
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
    childDatasets = tChildDatasets;
    int nChildren = tChildDatasets.length;
    childStopsAt = new int[nChildren];
    matchAxisNDigits = tMatchAxisNDigits;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads

    // ensure at least one child is accessibleViaFiles
    boolean cAccessibleViaFiles = false;
    for (int c = 0; c < nChildren; c++) {
      if (childDatasets[c].accessibleViaFiles) {
        cAccessibleViaFiles = true;
        break;
      }
    }
    accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles && cAccessibleViaFiles;

    // check the siblings and create childStopsAt
    EDDGrid firstChild = childDatasets[0];
    int nAV = firstChild.axisVariables.length;
    for (int c = 0; c < nChildren; c++) {
      if (c > 0) {
        String similar =
            firstChild.similarAxisVariables(
                childDatasets[c],
                1, // test axes 1+
                matchAxisNDigits,
                false);
        if (similar.length() > 0)
          throw new RuntimeException(
              "Error: Datasets #0 and #" + c + " are not similar: " + similar);

        // since they are similar, save memory by pointing to same datastructures
        // but just copy axisVariable 1+   (because axisVariables[0].sourceValues may be different)
        System.arraycopy(firstChild.axisVariables, 1, childDatasets[c].axisVariables, 1, nAV - 1);
        childDatasets[c].axisVariableSourceNames =
            firstChild.axisVariableSourceNames(); // () makes the array
        childDatasets[c].axisVariableDestinationNames = firstChild.axisVariableDestinationNames();
        // not id
        childDatasets[c].title = firstChild.title();
        childDatasets[c].summary = firstChild.summary();
        childDatasets[c].institution = firstChild.institution();
        childDatasets[c].infoUrl = firstChild.infoUrl();
        childDatasets[c].cdmDataType = firstChild.cdmDataType();
        childDatasets[c].searchBytes = firstChild.searchBytes();
        // not sourceUrl, which will be different
        childDatasets[c].sourceGlobalAttributes = firstChild.sourceGlobalAttributes();
        childDatasets[c].addGlobalAttributes = firstChild.addGlobalAttributes();
        childDatasets[c].combinedGlobalAttributes = firstChild.combinedGlobalAttributes();
      }
      childStopsAt[c] =
          (c == 0 ? -1 : childStopsAt[c - 1]) + childDatasets[c].dataVariables().length;
    }

    // create indexOfAxis0Value: an IntArray for each child; a row for each axis0 value
    int rowPo[] = new int[nChildren]; // all initially 0
    indexOfAxis0Value = new IntArray[nChildren];
    // gather all axis0SourceValues
    PrimitiveArray axis0SourceValues[] = new PrimitiveArray[nChildren];
    for (int c = 0; c < nChildren; c++) {
      indexOfAxis0Value[c] = new IntArray();
      axis0SourceValues[c] = childDatasets[c].axisVariables[0].sourceValues();
      if (verbose)
        String2.log("child[" + c + "].axisVariables[0].size()=" + axis0SourceValues[c].size());
    }
    PrimitiveArray newAxis0Values =
        PrimitiveArray.factory(
            axis0SourceValues[0].elementType(), axis0SourceValues[0].size() + 100, false);
    while (true) {
      // repeatedly:
      // find children which share the lowest axis0 value  (all axis values are numeric and not-NaN)
      double lowestValue = Double.MAX_VALUE;
      int firstLowestC = -1;
      boolean hasLowest[] = new boolean[nChildren]; // initially all false
      for (int c = 0; c < nChildren; c++) {
        if (rowPo[c] < axis0SourceValues[c].size()) {
          double td = axis0SourceValues[c].getDouble(rowPo[c]);
          if (td < lowestValue) {
            lowestValue = td;
            if (firstLowestC >= 0) Arrays.fill(hasLowest, firstLowestC, c, false);
            firstLowestC = c;
            hasLowest[c] = true;
          } else if (td == lowestValue) {
            hasLowest[c] = true;
          }
        }
      }
      if (firstLowestC == -1) // all children are done
      break;

      // note which row lowest is on
      newAxis0Values.addDouble(lowestValue);
      // StringBuilder tsb = new StringBuilder("lowestValue=" + lowestValue);
      for (int c = 0; c < nChildren; c++) {
        if (hasLowest[c]) {
          // tsb.append(" " + rowPo[c]);
          indexOfAxis0Value[c].add(rowPo[c]++); // and increment those rowPo's
        } else {
          // tsb.append(" NaN");
          indexOfAxis0Value[c].add(Integer.MAX_VALUE);
        }
      }
      // String2.log(tsb.toString());
    }

    // create the aggregate dataset
    setReloadEveryNMinutes(firstChild.getReloadEveryNMinutes());

    localSourceUrl = firstChild.localSourceUrl();
    addGlobalAttributes = firstChild.addGlobalAttributes();
    sourceGlobalAttributes = firstChild.sourceGlobalAttributes();
    combinedGlobalAttributes = firstChild.combinedGlobalAttributes();
    // and clear searchString of children?

    // duplicate firstChild.axisVariables
    lonIndex = firstChild.lonIndex;
    latIndex = firstChild.latIndex;
    altIndex = firstChild.altIndex;
    depthIndex = firstChild.depthIndex;
    timeIndex = firstChild.timeIndex;
    axisVariables = new EDVGridAxis[nAV];
    System.arraycopy(firstChild.axisVariables, 1, axisVariables, 1, nAV - 1);
    // but make new axisVariables[0] with newAxis0Values
    EDVGridAxis fav = firstChild.axisVariables[0];
    axisVariables[0] =
        makeAxisVariable(
            tDatasetID,
            0,
            fav.sourceName(),
            fav.destinationName(),
            fav.sourceAttributes(),
            fav.addAttributes(),
            newAxis0Values);

    // make combined dataVariables
    int nDv = childStopsAt[nChildren - 1] + 1;
    dataVariables = new EDV[nDv];
    for (int c = 0; c < nChildren; c++) {
      int start = c == 0 ? 0 : childStopsAt[c - 1] + 1;
      System.arraycopy(
          childDatasets[c].dataVariables(),
          0,
          dataVariables,
          start,
          childDatasets[c].dataVariables().length);
    }

    // right before ensureValid, make sure sourceNames are unique (by prefixing dv_)
    // This is kludgey, but it is simple and it solves the problem.  I think there are no side
    // effects.
    for (int dv = 0; dv < nDv; dv++)
      // using \n# at end avoids messing up searchString (e.g., "sourceName=sst\n0\n")
      dataVariables[dv].setSourceName(dataVariables[dv].sourceName() + "\n" + dv);

    // ensure the setup is valid
    ensureValid();

    // right after ensureValid, undo changes that made sourceNames unique
    for (int dv = 0; dv < nDv; dv++) {
      int po = dataVariables[dv].sourceName().lastIndexOf('\n');
      dataVariables[dv].setSourceName(dataVariables[dv].sourceName().substring(0, po));
    }

    // If any child is a FromErddap, try to subscribe to the remote dataset.
    for (int c = 0; c < childDatasets.length; c++)
      if (childDatasets[c] instanceof FromErddap) tryToSubscribeToChildFromErddap(childDatasets[c]);

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDGridSideBySide "
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

    // NOT FINISHED. NOT SIMPLE.
    boolean anyChanged = false;
    // for (int i = 0; i < childDatasets.length; i++) {
    //    if (childDatasets[i].update(language))
    //        anyChanged = true;
    //
    // rebuild indexOfAxis0Value
    // protected IntArray indexOfAxis0Value[]; //an IntArray for each child; a row for each axis0
    // value
    return anyChanged;
  }

  /**
   * creationTimeMillis indicates when this dataset was created. This overwrites the EDD version in
   * order to check if children need to be reloaded.
   *
   * @return when this dataset was created
   */
  @Override
  public long creationTimeMillis() {
    // return the oldest creation time of this or of a child
    long tCTM = creationTimeMillis;
    for (int c = 0; c < childDatasets.length; c++)
      tCTM = Math.min(tCTM, childDatasets[c].creationTimeMillis());
    return tCTM;
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
      for (int i = 0; i < childDatasets.length; i++) sa.add(childDatasets[i].datasetID());
    } catch (Exception e) {
      String2.log("Error caught in edd.childDatasetIDs(): " + MustBe.throwableToString(e));
    }
    return sa;
  }

  /**
   * This makes a sibling dataset, based on the new sourceUrl.
   *
   * @throws Throwable always (since this class doesn't support sibling())
   */
  @Override
  public EDDGrid sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable {
    throw new SimpleException("Error: " + "EDDGridSideBySide doesn't support method=\"sibling\".");
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

    // simple approach (not most efficient for tiny request, but fine for big requests):
    //  get results for each tDataVariable, one-by-one
    // FUTURE: more efficient to gang together all dataVariables from a given child
    int nAv = axisVariables.length;
    int nDv = dataVariables.length;
    int tnDv = tDataVariables.length;
    PrimitiveArray[] cumResults = new PrimitiveArray[nAv + tnDv];

    // get the array results
    int nValues = 1, nValues1 = 1;
    for (int av = 0; av < nAv; av++) {
      cumResults[av] =
          axisVariables[av]
              .sourceValues()
              .subset(
                  tConstraints.get(av * 3 + 0),
                  tConstraints.get(av * 3 + 1),
                  tConstraints.get(av * 3 + 2));
      nValues *= cumResults[av].size();
      if (av > 0) nValues1 *= cumResults[av].size();
    }

    // get the data results
    for (int tdv = 0; tdv < tnDv; tdv++) {

      // make a PrimitiveArray to hold the results for this dv
      PrimitiveArray dvResults =
          PrimitiveArray.factory(tDataVariables[tdv].sourceDataPAType(), nValues, false);
      cumResults[nAv + tdv] = dvResults;
      double tdvSourceMissingValue = tDataVariables[tdv].sourceMissingValue();

      // what is its dataVariable number in this aggregate dataset?
      // FUTURE: faster search with hash, but this is fast unless huge number of dataVars
      int dvn = 0;
      while (tDataVariables[tdv] != dataVariables[dvn]) dvn++;

      // which childDataset is that in?
      int cn = 0;
      while (dvn > childStopsAt[cn]) cn++;
      IntArray atIA = indexOfAxis0Value[cn];

      // step through constraints for combined axis0,
      //  finding sections in child of constant step size
      // !!!this is tricky code; think about it!!!
      IntArray ttConstraints = (IntArray) tConstraints.clone();
      int start = tConstraints.get(0);
      int stride = tConstraints.get(1);
      int stop = tConstraints.get(2);
      // String2.log("\n***sequence start=" + start + " stride=" + stride + " stop=" + stop);
      while (start <= stop) {
        // find first non-NaN
        while (start <= stop && atIA.array[start] == Integer.MAX_VALUE) {
          dvResults.addNDoubles(nValues1, tdvSourceMissingValue);
          start += stride;
        }
        if (start > stop) break;

        // start value is valid
        // find as many more valid values as possible with constant stride for the child
        int cStart = atIA.array[start];
        int cStride = -1;
        int po = start + stride;
        while (po <= stop) { // go until value at po is trouble
          int at = atIA.array[po];
          if (at == Integer.MAX_VALUE) {
            // String2.log("***sequence stopped because no corresponding av0 value for this child");
            break;
          }
          if (cStride == -1) {
            cStride = at - atIA.array[po - stride];
          } else if (at - atIA.array[po - stride] != cStride) {
            // String2.log("***sequence stopped because stride changed");
            break;
          }
          po += stride;
        }

        // get the data
        if (cStride == -1) cStride = 1;
        int cStop = atIA.array[po - stride]; // last valid value
        // String2.log("***sequence subsequence: cStart=" + cStart + " cStride=" + cStride + "
        // cStop=" + cStop);
        ttConstraints.set(0, cStart);
        ttConstraints.set(1, cStride);
        ttConstraints.set(2, cStop);
        PrimitiveArray[] tResults =
            childDatasets[cn].getSourceData(
                language, null, null, new EDV[] {tDataVariables[tdv]}, ttConstraints);
        dvResults.append(tResults[nAv]); // append the first (and only) data variable's results

        // increment start
        start = po;
      }

      // dvResults should be properly filled
      Test.ensureEqual(
          dvResults.size(),
          nValues,
          "Data source error in EDDGridSideBySide.getSourceData: " + "dvResults.size != nValues .");
    }

    return cumResults;
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
      int nChild = childDatasets.length;

      // if nextPath is nothing, return list of child id's as directories
      if (nextPath.length() == 0) {
        Table table = new Table();
        table.addColumn("Name", new StringArray());
        table.addColumn("Last modified", new LongArray().setMaxIsMV(true));
        table.addColumn("Size", new LongArray().setMaxIsMV(true));
        table.addColumn("Description", new StringArray());
        StringArray subDirs = new StringArray();
        for (int child = 0; child < nChild; child++) {
          if (childDatasets[child].accessibleViaFiles)
            subDirs.add(childDatasets[child].datasetID());
        }
        subDirs.sortIgnoreCase();
        return new Object[] {table, subDirs.toStringArray(), null};
      }

      // ensure start of nextPath is a child datasetID
      int po = nextPath.indexOf('/');
      if (po < 0) {
        String2.log("ERROR: no slash in nextPath.");
        return null;
      }

      // start of nextPath is a child datasetID
      String tID = nextPath.substring(0, po);
      nextPath = nextPath.substring(po + 1);
      for (int child = 0; child < nChild; child++) {
        EDD edd = childDatasets[child];
        if (tID.equals(edd.datasetID())) return edd.accessibleViaFilesFileTable(language, nextPath);
      }
      // or it isn't
      String2.log("ERROR: " + tID + " isn't a child's datasetID.");
      return null;

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
    if (!accessibleViaFiles) return null;
    String msg = datasetID() + " accessibleViaFilesGetLocal(" + relativeFileName + "): ";

    try {

      // first dir -> childDatasetName
      int po = relativeFileName.indexOf('/');
      if (po <= 0) {
        String2.log(msg + "no '/' in relatveFileName, so no child datasetID.");
        return null;
      }
      String childID = relativeFileName.substring(0, po);
      String relativeFileName2 = relativeFileName.substring(po + 1);

      // which child?
      int nChild = childDatasets.length;
      for (int c = 0; c < nChild; c++) {
        if (childID.equals(childDatasets[c].datasetID())) {
          // then redirect request to that child
          return childDatasets[c].accessibleViaFilesGetLocal(language, relativeFileName2);
        }
      }
      String2.log(msg + "childID=" + childID + " not found.");
      return null;

    } catch (Exception e) {
      String2.log(msg + ":\n" + MustBe.throwableToString(e));
      return null;
    }
  }
}
