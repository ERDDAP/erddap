/*
 * EDDTableCopy Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableCopyHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;

/**
 * This class makes and maintains a local copy of the data from a remote source. This class serves
 * data from the local copy.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-05-19
 */
@SaxHandlerClass(EDDTableCopyHandler.class)
public class EDDTableCopy extends EDDTable {

  protected EDDTable sourceEdd;
  protected EDDTableFromFiles localEdd;

  /** Some tests set EDDTableCopy.defaultCheckSourceData = false; Don't set it here. */
  public static boolean defaultCheckSourceData = true;

  protected static int maxChunks = Integer.MAX_VALUE; // some test methods reduce this

  /**
   * This returns the default value for standardizeWhat for this subclass. See
   * Attributes.unpackVariable for options. The default was chosen to mimic the subclass' behavior
   * from before support for standardizeWhat options was added.
   */
  public int defaultStandardizeWhat() {
    return DEFAULT_STANDARDIZEWHAT;
  }

  public static int DEFAULT_STANDARDIZEWHAT = 0;
  protected int standardizeWhat = Integer.MAX_VALUE; // =not specified by user
  protected int nThreads = -1; // interpret invalid values (like -1) as EDStatic.nTableThreads

  /**
   * This constructs an EDDTableCopy based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableCopy"&gt; having just
   *     been read.
   * @return an EDDTableCopy. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableCopy fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableCopy(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    EDDTable tSourceEdd = null;
    int tReloadEveryNMinutes = Integer.MAX_VALUE;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tSosOfferingPrefix = null;
    String tExtractDestinationNames = "";
    String tOrderExtractBy = "";
    boolean checkSourceData = defaultCheckSourceData;
    boolean tSourceNeedsExpandedFP_EQ = true;
    boolean tFileTableInMemory = false;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    String tAddVariablesWhere = null;
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    int tStandardizeWhat = Integer.MAX_VALUE; // not specified by user
    int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nTableThreads

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
      else if (localTags.equals("<onChange>")) {
      } else if (localTags.equals("</onChange>")) tOnChange.add(content);
      else if (localTags.equals("<fgdcFile>")) {
      } else if (localTags.equals("</fgdcFile>")) tFgdcFile = content;
      else if (localTags.equals("<iso19115File>")) {
      } else if (localTags.equals("</iso19115File>")) tIso19115File = content;
      else if (localTags.equals("<sosOfferingPrefix>")) {
      } else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content;
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<extractDestinationNames>")) {
      } else if (localTags.equals("</extractDestinationNames>")) tExtractDestinationNames = content;
      else if (localTags.equals("<orderExtractBy>")) {
      } else if (localTags.equals("</orderExtractBy>")) tOrderExtractBy = content;
      else if (localTags.equals("<checkSourceData>")) {
      } else if (localTags.equals("</checkSourceData>"))
        checkSourceData = String2.parseBoolean(content);
      else if (localTags.equals("<sourceNeedsExpandedFP_EQ>")) {
      } else if (localTags.equals("</sourceNeedsExpandedFP_EQ>"))
        tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content);
      else if (localTags.equals("<fileTableInMemory>")) {
      } else if (localTags.equals("</fileTableInMemory>"))
        tFileTableInMemory = String2.parseBoolean(content);
      else if (localTags.equals("<defaultDataQuery>")) {
      } else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content;
      else if (localTags.equals("<defaultGraphQuery>")) {
      } else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content;
      else if (localTags.equals("<addVariablesWhere>")) {
      } else if (localTags.equals("</addVariablesWhere>")) tAddVariablesWhere = content;
      else if (localTags.equals("<accessibleViaFiles>")) {
      } else if (localTags.equals("</accessibleViaFiles>"))
        tAccessibleViaFiles = String2.parseBoolean(content);
      else if (localTags.equals("<standardizeWhat>")) {
      } else if (localTags.equals("</standardizeWhat>"))
        tStandardizeWhat = String2.parseInt(content);
      else if (localTags.equals("<nThreads>")) {
      } else if (localTags.equals("</nThreads>")) tnThreads = String2.parseInt(content);
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
                  (EDDTable) EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);
            } else {
              String2.log(
                  "WARNING!!! checkSourceData is false, so EDDTableCopy datasetID="
                      + tDatasetID
                      + " is not checking the source dataset!");
              int stackSize = xmlReader.stackSize();
              do { // will throw Exception if trouble (e.g., unexpected end-of-file
                xmlReader.nextTag();
              } while (xmlReader.stackSize() != stackSize);
              tSourceEdd = null;
            }
          } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
          }
        }
      } else xmlReader.unexpectedTagException();
    }

    return new EDDTableCopy(
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
        tReloadEveryNMinutes,
        tStandardizeWhat,
        tExtractDestinationNames,
        tOrderExtractBy,
        tSourceNeedsExpandedFP_EQ,
        tSourceEdd,
        tFileTableInMemory,
        tAccessibleViaFiles,
        tnThreads);
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
   * @param tExtractDestinationNames a space separated list of destination names (at least one) that
   *     describe how chunks of source data will be extracted/copied. For example, "pi commonName
   *     surgeryId" indicates that for each distinct combination of pi+surgeryId values, data will
   *     be extracted and put in a file:
   *     [bigParentDirectory]/copiedData/datasetID/piValue/commonNameValue/surgeryIdValue.nc
   *     <p>This is also used as EDDTableFromNcFiles tSortFilesByDestinationNames: a space-separated
   *     list of destination variable names (because it's in localEdd) specifying how the internal
   *     list of files should be sorted (in ascending order). <br>
   *     When a data request is filled, data is obtained from the files in this order. <br>
   *     Thus it largely determines the overall order of the data in the response.
   * @param tOrderExtractBy are the space-separated destination (because it's in localEdd) names
   *     specifying how to sort each extract subset file. This is optional (use null or ""), but
   *     almost always used. Ideally, the first name is a numeric variable (this can greatly speed
   *     up some data requests).
   *     <p>Note that the combination of extractDestinationNames+orderExtractBy should fully define
   *     the desired sort order for the dataset.
   * @param tSourceNeedsExpandedFP_EQ
   * @param tSourceEdd the remote dataset to be copied. After the first time (to generate tasks to
   *     copy data), there will be local files so it's okay if tSourceEdd is null (unavailable).
   * @throws Throwable if trouble
   */
  public EDDTableCopy(
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
      int tReloadEveryNMinutes,
      int tStandardizeWhat,
      String tExtractDestinationNames,
      String tOrderExtractBy,
      Boolean tSourceNeedsExpandedFP_EQ,
      EDDTable tSourceEdd,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      int tnThreads)
      throws Throwable {

    if (verbose)
      String2.log(
          "\n*** constructing EDDTableCopy " + tDatasetID + " reallyVerbose=" + reallyVerbose);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableCopy(" + tDatasetID + ") constructor:\n";

    // save the parameters
    int language = 0;
    className = "EDDTableCopy";
    datasetID = tDatasetID;
    sourceEdd = tSourceEdd;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    sosOfferingPrefix = tSosOfferingPrefix;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    standardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? defaultStandardizeWhat()
            : tStandardizeWhat;
    accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nTableThreads

    // check some things
    if (tSourceEdd instanceof EDDTableFromThreddsFiles)
      throw new IllegalArgumentException(
          "datasets.xml error: "
              + "EDDTableFromThreddsFiles makes its own local copy of "
              + "the data, so it MUST NEVER be enclosed by EDDTableCopy ("
              + datasetID
              + ").");
    if (tSourceEdd instanceof EDDTableFromHyraxFiles)
      throw new IllegalArgumentException(
          "datasets.xml error: "
              + "EDDTableFromHyraxFiles makes its own local copy of "
              + "the data, so it MUST NEVER be enclosed by EDDTableCopy ("
              + datasetID
              + ").");
    if (tExtractDestinationNames.indexOf(',') >= 0)
      throw new IllegalArgumentException(
          "datasets.xml error: "
              + "extractDestinationNames should be space separated, not comma separated.");
    if (tOrderExtractBy != null && tOrderExtractBy.indexOf(',') >= 0)
      throw new IllegalArgumentException(
          "datasets.xml error: "
              + "orderExtractBy should be space separated, not comma separated.");
    StringArray orderExtractBy = null;
    if (tOrderExtractBy != null && tOrderExtractBy.length() > 0)
      orderExtractBy = StringArray.wordsAndQuotedPhrases(tOrderExtractBy);
    if (reallyVerbose) String2.log("orderExtractBy=" + orderExtractBy);

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

          // get the distinct() combination of values for tExtractDestinationNames
          StringArray extractNames = StringArray.wordsAndQuotedPhrases(tExtractDestinationNames);
          if (extractNames.size() == 0)
            throw new RuntimeException(
                "datasets.xml error: " + "There are no extractDestinationNames.");
          if (reallyVerbose) String2.log("extractNames=" + extractNames);
          String query =
              SSR.minimalPercentEncode(String2.replaceAll(extractNames.toString(), " ", ""))
                  + "&distinct()";
          String cacheDir = cacheDirectory();
          File2.makeDirectory(cacheDir); // ensure it exists
          TableWriterAll twa =
              new TableWriterAll(
                  language, null, null, // metadata not relevant
                  cacheDir, "extract");
          TableWriter tw =
              encloseTableWriter(
                  0,
                  true,
                  cacheDir,
                  "extractDistinct",
                  twa,
                  "", // metadata not relevant
                  query); // leads to enclosing in TableWriterDistinct
          sourceEdd.getDataForDapQuery(
              0,
              EDStatic.loggedInAsSuperuser,
              "",
              query,
              tw); // "" is requestUrl, not relevant here
          Table table = twa.cumulativeTable(); // has the distinct results
          tw = null;
          twa.releaseResources();
          int nRows = table.nRows(); // nRows = 0 will throw an exception above
          if (verbose) String2.log("source nChunks=" + nRows);
          nRows = Math.min(maxChunks, nRows);
          int nCols = table.nColumns();
          boolean isString[] = new boolean[nCols];
          for (int col = 0; col < nCols; col++)
            isString[col] = table.getColumn(col).elementType() == PAType.STRING;

          // make a task for each row (if the file doesn't already exist)
          for (int row = 0; row < nRows; row++) {
            // gather the query, fileDir, fileName, ... for the task
            // [fullCopyDirectory]/datasetID/piValue/commonNameValue/surgeryIdValue.nc
            StringBuilder fileDir = new StringBuilder(copyDatasetDir);
            for (int col = 0;
                col < nCols - 1;
                col++) { // -1 since last part is for file name, not dir
              String s = table.getStringData(col, row);
              fileDir.append(String2.encodeFileNameSafe(s) + "/");
            }

            StringBuilder tQuery = new StringBuilder();
            for (int col = 0; col < nCols; col++) {
              String s = table.getStringData(col, row);
              tQuery.append(
                  "&"
                      + table.getColumnName(col)
                      + "="
                      + (isString[col]
                          ? SSR.minimalPercentEncode(String2.toJson(s))
                          : "".equals(s) ? "NaN" : s));
            }

            // does the file already exist
            String fileName = String2.encodeFileNameSafe(table.getStringData(nCols - 1, row));
            if (File2.isFile(fileDir.toString() + fileName + ".nc")) {
              if (reallyVerbose)
                String2.log("  file already exists: " + fileDir + fileName + ".nc");
              continue;
            }
            File2.makeDirectory(fileDir.toString());
            if (orderExtractBy != null)
              tQuery.append(
                  "&"
                      + SSR.minimalPercentEncode(
                          "orderBy(\""
                              + String2.replaceAll(orderExtractBy.toString(), " ", "")
                              + "\")"));

            // make the task
            Object taskOA[] = new Object[6];
            taskOA[0] = TaskThread.TASK_MAKE_A_DATAFILE;
            taskOA[1] = sourceEdd;
            taskOA[2] = tQuery.toString(); // string, not StringBuilder
            taskOA[3] = fileDir.toString(); // string, not StringBuilder
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
                        + fileDir.toString()
                        + fileName
                        + ".nc");
            }
          }

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
    int nDataVariables;
    Object[][] tDataVariables;
    if (sourceEdd == null) {
      // get info from existing copied datafiles, which is a standard EDDTable)
      // get a list of copied files
      String tFileNames[] =
          RegexFilenameFilter.recursiveFullNameList(copyDatasetDir, ".*\\.nc", false);
      if (tFileNames.length == 0)
        throw new RuntimeException(
            "Warning: There are no copied files in "
                + copyDatasetDir
                + ",\nso localEdd can't be made yet for datasetID="
                + datasetID
                + ".\n"
                + "But it will probably succeed in next loadDatasets (15 minutes?),\n"
                + "after some files are copied.");

      // load the table
      String getFromName = File2.getYoungest(tFileNames);
      Table table = new Table();
      String2.log(
          "!!! sourceEDD is unavailable, so getting dataVariable info from youngest file\n"
              + getFromName);
      table.readFlatNc(
          getFromName,
          null,
          0); // null=allVars, standardizeWhat=0 because data is already unpacked.
      nDataVariables = table.nColumns();
      tDataVariables = new Object[nDataVariables][];
      for (int dv = 0; dv < nDataVariables; dv++) {
        tDataVariables[dv] =
            new Object[] {
              table.getColumnName(dv),
              table.getColumnName(dv),
              new Attributes(),
              table.getColumn(dv).elementTypeString()
            };
      }
    } else {
      // get info from sourceEdd, which is a standard EDDTable
      nDataVariables = sourceEdd.dataVariables.length;
      tDataVariables = new Object[nDataVariables][];
      for (int dv = 0; dv < nDataVariables; dv++) {
        EDV edv = sourceEdd.dataVariables[dv];
        tDataVariables[dv] =
            new Object[] {
              edv.destinationName(),
              edv.destinationName(),
              new Attributes(),
              edv.destinationDataType()
            }; // 2012-07-26 e.g., var with scale_factor will be destType in the copied files
      }
    }
    // if the first orderExtractBy column is numeric, it can be used as
    //  EDDTableFromFiles sortedColumn (the basis of faster searches within a file);
    //  otherwise it can't be used (not a big deal)
    String sortedColumn = orderExtractBy == null ? "" : orderExtractBy.get(0); // the first column
    if (sortedColumn.length() > 0) {
      for (int dv = 0; dv < nDataVariables; dv++) {
        if (sortedColumn.equals((String) tDataVariables[dv][0])
            && // columnName
            "String".equals((String) tDataVariables[dv][3])) { // columnType
          if (verbose)
            String2.log(
                "orderExtractBy #0="
                    + sortedColumn
                    + " can't be used as EDDTableFromFiles sortedColumn because it isn't numeric.");
          sortedColumn = "";
          break;
        }
      }
    }

    // make localEdd
    // It will fail if 0 local files -- that's okay, TaskThread will continue to work
    //  and constructor will try again in 15 min.
    boolean recursive = true;
    String fileNameRegex = ".*\\.nc";
    localEdd =
        makeLocalEdd(
            datasetID,
            tAccessibleTo,
            tGraphsAccessibleTo, // irrelevant
            tOnChange,
            tFgdcFile,
            tIso19115File,
            new Attributes(), // addGlobalAttributes
            tDataVariables,
            tReloadEveryNMinutes,
            copyDatasetDir,
            fileNameRegex,
            recursive,
            ".*", // pathRegex is for original source files
            EDDTableFromFiles.MF_LAST,
            "",
            "",
            "",
            1,
            2,
            "", // columnNamesRow and firstDataRow are irrelevant for .nc files, but must be valid
            // values
            null,
            null,
            null,
            null, // extract from fileNames
            sortedColumn,
            tExtractDestinationNames,
            tSourceNeedsExpandedFP_EQ,
            tFileTableInMemory,
            tAccessibleViaFiles,
            standardizeWhat,
            nThreads);

    // copy things from localEdd
    sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
    sourceCanConstrainNumericData = localEdd.sourceCanConstrainNumericData;
    sourceCanConstrainStringData = localEdd.sourceCanConstrainStringData;
    sourceCanConstrainStringRegex = localEdd.sourceCanConstrainStringRegex;

    sourceGlobalAttributes = localEdd.combinedGlobalAttributes;
    addGlobalAttributes = new Attributes();
    combinedGlobalAttributes =
        localEdd.combinedGlobalAttributes; // new Attributes(addGlobalAttributes,
    // sourceGlobalAttributes); //order is important

    // copy data variables
    dataVariables = localEdd.dataVariables;
    lonIndex = localEdd.lonIndex;
    latIndex = localEdd.latIndex;
    altIndex = localEdd.altIndex;
    depthIndex = localEdd.depthIndex;
    timeIndex = localEdd.timeIndex;

    // copy sos info
    sosOfferingType = localEdd.sosOfferingType;
    sosOfferingIndex = localEdd.sosOfferingIndex;
    sosOfferings = localEdd.sosOfferings;
    sosMinTime = localEdd.sosMinTime;
    sosMaxTime = localEdd.sosMaxTime;
    sosMinLat = localEdd.sosMinLat;
    sosMaxLat = localEdd.sosMaxLat;
    sosMinLon = localEdd.sosMinLon;
    sosMaxLon = localEdd.sosMaxLon;

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid(); // this ensures many things are set, e.g., sourceUrl

    // If the child is a FromErddap, try to subscribe to the remote dataset.
    if (sourceEdd instanceof FromErddap) tryToSubscribeToChildFromErddap(sourceEdd);

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableCopy "
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
    return true;
  } // because this gets info from cached local files

  /**
   * This is used by the constructor to make localEDD. This version makes an EDDTableFromNcFiles,
   * but subclasses use it to make subsclasses of EDDTableFromFiles, e.g., EDDTableFromPostNcFiles.
   *
   * <p>It will fail if 0 local files -- that's okay, TaskThread will continue to work and
   * constructor will try again in 15 min.
   */
  EDDTableFromFiles makeLocalEdd(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      Attributes tAddGlobalAttributes,
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tMetadataFrom,
      String tCharset,
      String skipHeaderToRegex,
      String skipLinesRegex,
      int tColumnNamesRow,
      int tFirstDataRow,
      String tColumnSeparator,
      String tPreExtractRegex,
      String tPostExtractRegex,
      String tExtractRegex,
      String tColumnNameForExtract,
      String tSortedColumnSourceName,
      String tSortFilesBySourceNames,
      boolean tSourceNeedsExpandedFP_EQ,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      int tStandardizeWhat,
      int tnThreads)
      throws Throwable {

    return new EDDTableFromNcFiles(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        "",
        "",
        "", // tSosOfferingPrefix, tDefaultDataQuery, tDefaultGraphQuery,
        tAddGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        0, // tUpdateEveryNMillis,
        tFileDir,
        tFileNameRegex,
        tRecursive,
        tPathRegex,
        tMetadataFrom,
        tCharset,
        skipHeaderToRegex,
        skipLinesRegex,
        tColumnNamesRow,
        tFirstDataRow,
        tColumnSeparator,
        tPreExtractRegex,
        tPostExtractRegex,
        tExtractRegex,
        tColumnNameForExtract,
        tSortedColumnSourceName,
        tSortFilesBySourceNames,
        tSourceNeedsExpandedFP_EQ,
        tFileTableInMemory,
        tAccessibleViaFiles,
        false, // removeMVrows is irrelevant for EDDTableFromNcFiles
        tStandardizeWhat,
        tnThreads,
        "",
        -1,
        "", // cacheFromUrl, cacheSizeGB, cachePartialPathRegex
        null); // addVariablesWhere
  }

  /** This gets changed info from localEdd.changed. */
  @Override
  public String changed(EDD old) {
    return localEdd.changed(old);
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

    localEdd.getDataForDapQuery(language, loggedInAs, requestUrl, userDapQuery, tableWriter);
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
