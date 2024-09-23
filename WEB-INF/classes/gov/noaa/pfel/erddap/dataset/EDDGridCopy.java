/*
 * EDDGridCopy Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridCopyHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;
import java.text.MessageFormat;
import java.util.List;
import ucar.nc2.*;

/**
 * This class makes and maintains a local copy of the data from a remote source. This class serves
 * data from the local copy.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-05-25
 */
@SaxHandlerClass(EDDGridCopyHandler.class)
public class EDDGridCopy extends EDDGrid {

  protected EDDGrid sourceEdd;
  protected EDDGridFromNcFiles localEdd;

  /**
   * This is used to test equality of axis values. 0=no testing (not recommended). &gt;18 does exact
   * test. default=20. 1-18 tests that many digets for doubles and hidiv(n,2) for floats.
   */
  protected int matchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

  /** Some tests set EDDGridCopy.defaultCheckSourceData = false; Don't set it here. */
  public static boolean defaultCheckSourceData = true;

  private static int maxChunks = Integer.MAX_VALUE; // some test methods reduce this

  protected String onlySince = null;

  /**
   * This constructs an EDDGridCopy based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridCopy"&gt; having just
   *     been read.
   * @return an EDDGridCopy. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridCopy fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDGridCopy(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    EDDGrid tSourceEdd = null;
    int tReloadEveryNMinutes = Integer.MAX_VALUE;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
    int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    boolean checkSourceData = defaultCheckSourceData;
    boolean tFileTableInMemory = false;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    boolean tDimensionValuesInMemory = true;
    String tOnlySince = null;

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
      if (localTags.equals("<accessibleTo>")) {
      } else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<accessibleViaWMS>")) {
      } else if (localTags.equals("</accessibleViaWMS>"))
        tAccessibleViaWMS = String2.parseBoolean(content);
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
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<checkSourceData>")) {
      } else if (localTags.equals("</checkSourceData>"))
        checkSourceData = String2.parseBoolean(content);
      else if (localTags.equals("<fileTableInMemory>")) {
      } else if (localTags.equals("</fileTableInMemory>"))
        tFileTableInMemory = String2.parseBoolean(content);
      else if (localTags.equals("<defaultDataQuery>")) {
      } else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content;
      else if (localTags.equals("<defaultGraphQuery>")) {
      } else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content;
      else if (localTags.equals("<nThreads>")) {
      } else if (localTags.equals("</nThreads>")) tnThreads = String2.parseInt(content);
      else if (localTags.equals("<dimensionValuesInMemory>")) {
      } else if (localTags.equals("</dimensionValuesInMemory>"))
        tDimensionValuesInMemory = String2.parseBoolean(content);
      else if (localTags.equals("<accessibleViaFiles>")) {
      } else if (localTags.equals("</accessibleViaFiles>"))
        tAccessibleViaFiles = String2.parseBoolean(content);
      else if (localTags.equals("<onlySince>")) {
      } else if (localTags.equals("</onlySince>")) tOnlySince = content;
      else if (localTags.equals("<dataset>")) {

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
            if (checkSourceData) {
              // after first time, it's ok if source dataset isn't available
              tSourceEdd =
                  (EDDGrid) EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);
            } else {
              String2.log(
                  "WARNING!!! checkSourceData is false, so EDDGridCopy datasetID="
                      + tDatasetID
                      + " is not checking the source dataset!");
              int stackSize = xmlReader.stackSize();
              do { // will throw Exception if trouble (e.g., unexpected end-of-file
                xmlReader.nextTag();
              } while (xmlReader.stackSize() != stackSize);
              tSourceEdd = null;
            }

            // was  (so xmlReader in right place)
            // if (!checkSourceData) {
            //    tSourceEdd = null;
            //    throw new RuntimeException("TESTING checkSourceData=false.");
            // }
          } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
          }
        }
      } else xmlReader.unexpectedTagException();
    }

    return new EDDGridCopy(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tMatchAxisNDigits,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tReloadEveryNMinutes,
        tSourceEdd,
        tFileTableInMemory,
        tAccessibleViaFiles,
        tOnlySince,
        tnThreads,
        tDimensionValuesInMemory);
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
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data.
   * @param tSourceEdd the remote dataset to be copied. After the very first time (to generate tasks
   *     to copy data), there will be local files so it's ok if tSourceEdd is null (unavailable).
   * @throws Throwable if trouble
   */
  public EDDGridCopy(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
      int tMatchAxisNDigits,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      int tReloadEveryNMinutes,
      EDDGrid tSourceEdd,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      String tOnlySince,
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridCopy " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridCopy(" + tDatasetID + ") constructor:\n";

    // save the parameters
    className = "EDDGridCopy";
    datasetID = tDatasetID;
    sourceEdd = tSourceEdd;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    if (!tAccessibleViaWMS)
      accessibleViaWMS = String2.canonical(MessageFormat.format(EDStatic.noXxxAr[0], "WMS"));
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    matchAxisNDigits = tMatchAxisNDigits;
    onlySince = tOnlySince;
    accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads
    dimensionValuesInMemory = tDimensionValuesInMemory;

    // ensure copyDatasetDir exists
    String copyDatasetDir = EDStatic.fullCopyDirectory + datasetID + "/";
    File2.makeDirectory(copyDatasetDir);

    // assign copy tasks to taskThread
    if (sourceEdd != null) {
      int taskNumber = -1; // i.e. unused
      try {

        // check if taskThread has finished previously assigned tasks for this dataset
        EDStatic.ensureTaskThreadIsRunningIfNeeded(); // ensure info is up-to-date
        Integer lastAssignedTask = (Integer) EDStatic.lastAssignedTask.get(datasetID);
        boolean pendingTasks =
            lastAssignedTask != null && EDStatic.lastFinishedTask < lastAssignedTask.intValue();
        if (verbose)
          String2.log(
              "  lastFinishedTask="
                  + EDStatic.lastFinishedTask
                  + " < lastAssignedTask("
                  + tDatasetID
                  + ")="
                  + lastAssignedTask
                  + "? pendingTasks="
                  + pendingTasks);
        if (!pendingTasks) {

          // make a task for each axis0 value (if the file doesn't already exist)
          PrimitiveArray tDestValues = sourceEdd.axisVariables[0].destinationValues();
          int nAV = sourceEdd.axisVariables.length;
          int nDV = sourceEdd.dataVariables.length;
          StringBuilder av1on = new StringBuilder();
          for (int av = 1; av < nAV; av++) av1on.append(SSR.minimalPercentEncode("[]"));
          int nValues = tDestValues.size();
          nValues = Math.min(maxChunks, nValues);
          double onlySinceDouble = Double.NaN; // usually epochSeconds
          if (String2.isSomething(onlySince)) {
            onlySinceDouble =
                onlySince.toLowerCase().startsWith("now")
                    ? Calendar2.nowStringToEpochSeconds(onlySince)
                    : // throws exception if trouble
                    Calendar2.isIsoDate(onlySince)
                        ? Calendar2.isoStringToEpochSeconds(onlySince)
                        : String2.parseDouble(onlySince);
            if (Double.isNaN(onlySinceDouble))
              throw new SimpleException(String2.ERROR + " while parsing onlySince=" + onlySince);
          }
          int onlySinceNSkipped = 0;
          for (int vi = 0; vi < nValues; vi++) {
            // if onlySince is active, skip this value?
            if (!Double.isNaN(onlySinceDouble) && tDestValues.getDouble(vi) < onlySinceDouble) {
              onlySinceNSkipped++;
              continue;
            }

            // [fullCopyDirectory]/datasetID/value.nc
            // does the file already exist?
            String tDestValue = tDestValues.getString(vi);
            String fileName = String2.encodeFileNameSafe(tDestValues.getString(vi));
            if (File2.isFile(copyDatasetDir + fileName + ".nc")) {
              if (reallyVerbose)
                String2.log("  file already exists: " + copyDatasetDir + fileName + ".nc");
              continue;
            }
            StringBuilder tQuery = new StringBuilder();
            sourceEdd.dataVariableDestinationNames(); // ensure [] has been created
            for (int dv = 0; dv < nDV; dv++) {
              if (dv > 0) tQuery.append(',');
              tQuery.append(
                  sourceEdd.dataVariableDestinationNames[dv]
                      + SSR.minimalPercentEncode("[(" + tDestValue + ")]")
                      + av1on);
            }

            // make the task
            Object taskOA[] = new Object[6];
            taskOA[0] = TaskThread.TASK_MAKE_A_DATAFILE;
            taskOA[1] = sourceEdd;
            taskOA[2] = tQuery.toString(); // String, not StringBuilder
            taskOA[3] = copyDatasetDir;
            taskOA[4] = fileName;
            taskOA[5] = ".nc";
            int tTaskNumber = EDStatic.addTask(taskOA);
            if (tTaskNumber >= 0) {
              taskNumber = tTaskNumber;
              if (reallyVerbose)
                String2.log(
                    "  task#"
                        + taskNumber
                        + " TASK_MAKE_A_DATAFILE "
                        + tQuery.toString()
                        + "\n    "
                        + copyDatasetDir
                        + fileName
                        + ".nc");
            }
          }

          if (onlySinceNSkipped > 0)
            String2.log(
                "  onlySince="
                    + onlySince
                    + "="
                    + onlySinceDouble
                    + " caused "
                    + onlySinceNSkipped
                    + " source values to be skipped.");

          // create task to flag dataset to be reloaded
          if (taskNumber > -1) {
            Object taskOA[] = new Object[2];
            taskOA[0] = TaskThread.TASK_SET_FLAG;
            taskOA[1] = datasetID;
            taskNumber = EDStatic.addTask(taskOA); // TASK_SET_FLAG will always be added
            if (reallyVerbose) String2.log("  task#" + taskNumber + " TASK_SET_FLAG " + datasetID);
          }
        }
      } catch (Throwable t) {
        String2.log(
            "Error while assigning "
                + datasetID
                + " copy tasks to taskThread:\n"
                + MustBe.throwableToString(t));
      }
      if (taskNumber >= 0) {
        EDStatic.lastAssignedTask.put(datasetID, Integer.valueOf(taskNumber));
        EDStatic
            .ensureTaskThreadIsRunningIfNeeded(); // clients (like this class) are responsible for
        // checking on it

        if (EDStatic.forceSynchronousLoading) {
          while (EDStatic.lastFinishedTask < taskNumber) {
            Thread.sleep(2000);
          }
        }
      }
    }

    // gather info about dataVariables to create localEdd
    Object[][] tAxisVariables = null;
    Object[][] tDataVariables = null;
    if (sourceEdd == null) {
      // get info from existing copied datafiles, which is a standard EDDGrid)
      // get a list of copied files
      String tFileNames[] =
          RegexFilenameFilter.fullNameList( // not recursiveFullNameList, since just 1 dir
              copyDatasetDir, ".*\\.nc");
      if (tFileNames.length == 0)
        throw new RuntimeException(
            "Warning: There are no copied files in "
                + copyDatasetDir
                + ",\nso localEdd can't be made yet for datasetID="
                + datasetID
                + ".\n"
                + "But it will probably succeed in next loadDatasets (15 minutes?),\n"
                + "after some files are copied.");

      // get the axisVariable and dataVariable info from the file
      String getFromName = File2.getYoungest(tFileNames);
      String2.log(
          "!!! sourceEDD is unavailable, so getting info from youngest file\n" + getFromName);
      StringArray ncDataVarNames = new StringArray();
      StringArray ncDataVarTypes = new StringArray();
      NetcdfFile ncFile = NcHelper.openFile(getFromName);
      try {
        // list all variables with dimensions
        List allVariables = ncFile.getVariables();
        for (int v = 0; v < allVariables.size(); v++) {
          Variable var = (Variable) allVariables.get(v);
          String varName = var.getShortName();
          List dimensions = var.getDimensions();
          if (dimensions != null && dimensions.size() > 1) {
            if (tAxisVariables == null) {
              // gather tAxisVariables
              tAxisVariables = new Object[dimensions.size()][];
              for (int avi = 0; avi < dimensions.size(); avi++) {
                String axisName = ((Dimension) dimensions.get(avi)).getName();
                tAxisVariables[avi] = new Object[] {axisName, axisName, new Attributes()};
              }
            }
            ncDataVarNames.add(varName);
            PAType tPAType = NcHelper.getElementPAType(var);
            if (tPAType == PAType.CHAR) tPAType = PAType.STRING;
            else if (tPAType == PAType.BOOLEAN) tPAType = PAType.BYTE;
            ncDataVarTypes.add(PAType.toCohortString(tPAType));
          }
        }

        // gather tDataVariables
        if (ncDataVarNames.size() == 0 || tAxisVariables == null)
          throw new RuntimeException(
              "Error: No multidimensional variables were found in " + getFromName);
        tDataVariables = new Object[ncDataVarNames.size()][];
        for (int dv = 0; dv < ncDataVarNames.size(); dv++) {
          tDataVariables[dv] =
              new Object[] {
                ncDataVarNames.get(dv),
                ncDataVarNames.get(dv),
                new Attributes(),
                ncDataVarTypes.get(dv)
              };
        }
      } finally {
        try {
          if (ncFile != null) ncFile.close();
        } catch (Exception e9) {
        }
      }
    } else {
      // get info from sourceEdd, which is a standard EDDGrid
      int nAxisVariables = sourceEdd.axisVariableDestinationNames().length;
      tAxisVariables = new Object[nAxisVariables][];
      for (int av = 0; av < nAxisVariables; av++) {
        String tName = sourceEdd.axisVariableDestinationNames[av];
        tAxisVariables[av] = new Object[] {tName, tName, new Attributes()};
      }
      int nDataVariables = sourceEdd.dataVariables.length;
      tDataVariables = new Object[nDataVariables][];
      for (int dv = 0; dv < nDataVariables; dv++) {
        EDV edv = sourceEdd.dataVariables[dv];
        tDataVariables[dv] =
            new Object[] {
              edv.destinationName(), edv.destinationName(), new Attributes(), edv.sourceDataType()
            };
      }
    }

    // make localEDD
    // It will fail if 0 local files -- that's okay, taskThread will continue to work
    //  and constructor will try again in 15 min.
    boolean recursive = false; // false=notRecursive   since always just 1 directory
    String fileNameRegex = ".*\\.nc";
    localEdd =
        new EDDGridFromNcFiles(
            datasetID,
            tAccessibleTo,
            tGraphsAccessibleTo,
            tAccessibleViaWMS,
            tOnChange,
            tFgdcFile,
            tIso19115File,
            tDefaultDataQuery,
            tDefaultGraphQuery,
            new Attributes(), // addGlobalAttributes
            tAxisVariables,
            tDataVariables,
            tReloadEveryNMinutes,
            0, // updateEveryNMillis
            copyDatasetDir,
            fileNameRegex,
            recursive,
            ".*", // true pathRegex is for remote site
            EDDGridFromFiles.MF_LAST,
            matchAxisNDigits, // sourceEdd should have made them consistent
            tFileTableInMemory,
            tAccessibleViaFiles,
            nThreads,
            dimensionValuesInMemory,
            "",
            -1,
            ""); // cacheFromUrl, cacheSizeGB, cachePartialPathRegex

    // copy things from localEdd
    // remove last 2 lines from history (will be redundant)
    String tHistory = localEdd.combinedGlobalAttributes.getString("history");
    if (tHistory != null) {
      StringArray tHistoryLines =
          (StringArray) PrimitiveArray.factory(String2.split(tHistory, '\n'));
      if (tHistoryLines.size() > 2) {
        tHistoryLines.removeRange(tHistoryLines.size() - 2, tHistoryLines.size());
        String ts = tHistoryLines.toNewlineString();
        localEdd.combinedGlobalAttributes.add(
            "history", ts.substring(0, ts.length() - 1)); // remove last \n
      }
    }
    sourceGlobalAttributes = localEdd.combinedGlobalAttributes;
    addGlobalAttributes = new Attributes();
    combinedGlobalAttributes =
        localEdd.combinedGlobalAttributes; // new Attributes(addGlobalAttributes,
    // sourceGlobalAttributes); //order is important

    // data variables
    axisVariables = localEdd.axisVariables;
    dataVariables = localEdd.dataVariables;
    lonIndex = localEdd.lonIndex;
    latIndex = localEdd.latIndex;
    altIndex = localEdd.altIndex;
    depthIndex = localEdd.depthIndex;
    timeIndex = localEdd.timeIndex;

    // ensure the setup is valid
    ensureValid(); // this ensures many things are set, e.g., sourceUrl

    // If the child is a FromErddap, try to subscribe to the remote dataset.
    if (sourceEdd instanceof FromErddap) tryToSubscribeToChildFromErddap(sourceEdd);

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDGridCopy "
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
   * If the subclass is EDDGridFromFiles or EDDGridCopy, this returns the dirTable (or throws
   * Throwalbe). Other subclasses return null.
   *
   * @throws Throwable if trouble
   */
  @Override
  public Table getDirTable() throws Throwable {
    return localEdd.getDirTable();
  }

  /**
   * If the subclass is EDDGridFromFiles or EDDGridCopy, this returns the fileTable (or throws
   * RuntimeException). Other subclasses return null.
   *
   * @throws Throwable if trouble
   */
  @Override
  public Table getFileTable() throws Throwable {
    return localEdd.getFileTable();
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

    return localEdd.getSourceData(language, tDirTable, tFileTable, tDataVariables, tConstraints);
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
    throw new SimpleException("Error: " + "EDDGridCopy doesn't support method=\"sibling\".");
  }

  /**
   * This returns a fileTable with valid files (or null if unavailable or any trouble). This is a
   * copy of any internal data, so client can modify the contents.
   *
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
    return localEdd.accessibleViaFilesFileTable(language, nextPath);
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
    return localEdd.accessibleViaFilesGetLocal(language, relativeFileName);
  }
}
