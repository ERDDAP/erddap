/*
 * EDDTableFromHyraxFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.XML;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;
import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;

/**
 * This class downloads data from a Hyrax data server with lots of files into .nc files in the
 * [bigParentDirectory]/copy/datasetID, and then uses superclass EDDTableFromFiles methods to
 * read/serve data from the .nc files. So don't wrap this class in EDDTableCopy.
 *
 * <p>The Hyrax files can be n-dimensional (1,2,3,4,...) DArray or DGrid OPeNDAP files, each of
 * which is flattened into a table.
 *
 * <p>This class is very similar to EDDTableFromThreddsFiles.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-06-08
 */
public class EDDTableFromHyraxFiles extends EDDTableFromFiles {

  /**
   * Indicates if data can be transmitted in a compressed form. It is unlikely anyone would want to
   * change this.
   */
  public static boolean acceptDeflate = true;

  /**
   * This returns the default value for standardizeWhat for this subclass. See
   * Attributes.unpackVariable for options. The default was chosen to mimic the subclass' behavior
   * from before support for standardizeWhat options was added.
   */
  @Override
  public int defaultStandardizeWhat() {
    return DEFAULT_STANDARDIZEWHAT;
  }

  public static int DEFAULT_STANDARDIZEWHAT = 0;

  /**
   * The constructor just calls the super constructor.
   *
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   *     <p>The sortedColumnSourceName can't be for a char/String variable because NcHelper binary
   *     searches are currently set up for numeric vars only.
   */
  public EDDTableFromHyraxFiles(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      Attributes tAddGlobalAttributes,
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tMetadataFrom,
      String tCharset,
      String tSkipHeaderToRegex,
      String tSkipLinesRegex,
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
      boolean tRemoveMVRows,
      int tStandardizeWhat,
      int tNThreads,
      String tCacheFromUrl,
      int tCacheSizeGB,
      String tCachePartialPathRegex,
      String tAddVariablesWhere)
      throws Throwable {

    super(
        "EDDTableFromHyraxFiles",
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        EDStatic.fullCopyDirectory + tDatasetID + "/", // force fileDir to be the copyDir
        tFileNameRegex,
        tRecursive,
        tPathRegex,
        tMetadataFrom,
        tCharset,
        tSkipHeaderToRegex,
        tSkipLinesRegex,
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
        tRemoveMVRows,
        tStandardizeWhat,
        tNThreads,
        tCacheFromUrl,
        tCacheSizeGB,
        tCachePartialPathRegex,
        tAddVariablesWhere);
  }

  /**
   * Create tasks to download files. If addToHyraxUrlList is completelySuccessful, local files that
   * aren't mentioned on the server will be renamed [fileName].ncRemoved .
   *
   * @param catalogUrl should have /catalog/ in the middle and / or contents.html at the end e.g.,
   *     https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/contents.html This won't
   *     throw an exception.
   */
  public static void makeDownloadFileTasks(
      String tDatasetID,
      String catalogUrl,
      String fileNameRegex,
      boolean recursive,
      String pathRegex) {

    if (verbose)
      String2.log(
          "* "
              + tDatasetID
              + " makeDownloadFileTasks from "
              + catalogUrl
              + "\nfileNameRegex="
              + fileNameRegex);
    long startTime = System.currentTimeMillis();
    int taskNumber = -1; // unused

    try {
      // if previous tasks are still running, return
      EDStatic.ensureTaskThreadIsRunningIfNeeded(); // ensure info is up-to-date
      Integer lastAssignedTask = (Integer) EDStatic.lastAssignedTask.get(tDatasetID);
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
      if (pendingTasks) return;

      // mimic the remote directory structure (there may be 10^6 files in many dirs)
      // catalogUrl https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/contents.html
      if (catalogUrl == null || catalogUrl.length() == 0)
        throw new RuntimeException(
            "ERROR: <sourceUrl>http://.../contents.html</sourceUrl> "
                + "must be in the addGlobalAttributes section of the datasets.xml "
                + "for datasetID="
                + tDatasetID);
      if (!catalogUrl.endsWith("/") && !catalogUrl.endsWith("/contents.html")) catalogUrl += "/";
      String lookFor = File2.getDirectory(catalogUrl);
      int lookForLength = lookFor.length();

      // mimic the remote dir structure in baseDir
      String baseDir = EDStatic.fullCopyDirectory + tDatasetID + "/";
      // e.g. localFile EDStatic.fullCopyDirectory + tDatasetID +  /
      // 1987/M07/pentad_19870710_v11l35flk.nc.gz
      File2.makeDirectory(baseDir);

      // gather all sourceFile info
      StringArray sourceFileName = new StringArray();
      DoubleArray sourceFileLastMod = new DoubleArray();
      LongArray fSize = new LongArray();
      String errorMessages =
          FileVisitorDNLS.addToHyraxUrlList(
              catalogUrl,
              fileNameRegex,
              recursive,
              pathRegex,
              false, // dirsToo
              sourceFileName,
              sourceFileLastMod,
              fSize);

      // Rename local files that shouldn't exist?
      // If completelySuccessful and found some files,
      // local files that aren't mentioned on the server will be renamed
      // [fileName].ncRemoved .
      // If not completelySuccessful, perhaps the server is down temporarily,
      // no files will be renamed.
      // !!! This is imperfect. If a remote sub webpage always fails, then
      //  no local files will ever be renamed.
      if (errorMessages.length() == 0 && sourceFileName.size() > 0) {
        // make a hashset of theoretical local fileNames that will exist
        //  after copying based on getHyraxFileInfo
        HashSet<String> hashset = new HashSet();
        int nFiles = sourceFileName.size();
        for (int f = 0; f < nFiles; f++) {
          String sourceName = sourceFileName.get(f);
          if (sourceName.startsWith(lookFor)) {
            String willExist = baseDir + sourceName.substring(lookForLength);
            hashset.add(willExist);
            // String2.log("  willExist=" + willExist);
          }
        }

        // get all the existing local files
        String localFiles[] =
            recursive
                ?
                // pathRegex was applied to get files, so no need to apply here
                RegexFilenameFilter.recursiveFullNameList(baseDir, fileNameRegex, false)
                : // directoriesToo
                RegexFilenameFilter.fullNameList(baseDir, fileNameRegex);

        // rename local files not in the hashset of files that will exist to [fileName].ncRemoved
        int nLocalFiles = localFiles.length;
        int nRemoved = 0;
        if (reallyVerbose)
          String2.log(
              "Looking for local files to rename 'Removed' because the "
                  + "datasource no longer has the corresponding file...");
        for (int f = 0; f < nLocalFiles; f++) {
          // if a localFile isn't in hashset of willExist files, it shouldn't exist
          if (!hashset.remove(localFiles[f])) {
            nRemoved++;
            if (reallyVerbose) String2.log("  renaming to " + localFiles[f] + "Removed");
            File2.rename(localFiles[f], localFiles[f] + "Removed");
          }
          localFiles[f] = null; // allow gc    (as does remove() above)
        }
        if (verbose)
          String2.log(
              nRemoved
                  + " local files were renamed to [fileName].ncRemoved because the datasource no longer has "
                  + "the corresponding file.\n"
                  + (nLocalFiles - nRemoved)
                  + " files remain.");

        /* /if 0 files remain (e.g., from significant change), delete empty subdir
        if (nLocalFiles - nRemoved == 0) {
            try {
                String err = RegexFilenameFilter.recursiveDelete(baseDir);
                if (err.length() == 0) {
                    if (verbose) String2.log(tDatasetID + " copyDirectory is completely empty.");
                } else {
                    String2.log(err); //or email it to admin?
                }
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }*/
      }

      // make tasks to download files
      int nTasksCreated = 0;
      boolean remoteErrorLogged = false; // just display 1st offender
      boolean fileErrorLogged = false; // just display 1st offender
      int nFiles = sourceFileName.size();
      for (int f = 0; f < nFiles; f++) {
        String sourceName = sourceFileName.get(f);
        if (!sourceName.startsWith(lookFor)) {
          if (!remoteErrorLogged) {
            String2.log(
                "ERROR! lookFor=" + lookFor + " wasn't at start of sourceName=" + sourceName);
            remoteErrorLogged = true;
          }
          continue;
        }

        // see if up-to-date localFile exists  (keep name identical; don't add .nc)
        String localFile = baseDir + sourceName.substring(lookForLength);
        String reason = "";
        try {
          // don't use File2 so more efficient for current purpose
          File file = new File(localFile);
          if (!file.isFile()) reason = "new file";
          else if (file.lastModified() != Math2.roundToLong(sourceFileLastMod.get(f) * 1000))
            reason = "lastModified changed";
          else continue; // up-to-date file already exists
        } catch (Exception e) {
          if (!fileErrorLogged) {
            String2.log(
                "ERROR checking localFile=" + localFile + "\n" + MustBe.throwableToString(e));
            fileErrorLogged = true;
          }
        }

        // make a task to download sourceFile to localFile
        // taskOA[1]=dapUrl, taskOA[2]=fullFileName, taskOA[3]=lastModified (Long)
        Object taskOA[] = new Object[7];
        taskOA[0] = TaskThread.TASK_ALL_DAP_TO_NC;
        taskOA[1] = sourceName;
        taskOA[2] = localFile;
        taskOA[3] = Long.valueOf(Math2.roundToLong(sourceFileLastMod.get(f) * 1000));
        int tTaskNumber = EDStatic.addTask(taskOA);
        if (tTaskNumber >= 0) {
          nTasksCreated++;
          taskNumber = tTaskNumber;
          if (reallyVerbose)
            String2.log(
                "  task#"
                    + taskNumber
                    + " TASK_ALL_DAP_TO_NC reason="
                    + reason
                    + "\n    from="
                    + sourceName
                    + "\n    to="
                    + localFile);
        }
      }

      // create task to flag dataset to be reloaded
      if (taskNumber > -1) {
        Object taskOA[] = new Object[2];
        taskOA[0] = TaskThread.TASK_SET_FLAG;
        taskOA[1] = tDatasetID;
        taskNumber = EDStatic.addTask(taskOA); // TASK_SET_FLAG will always be added
        nTasksCreated++;
        if (reallyVerbose) String2.log("  task#" + taskNumber + " TASK_SET_FLAG " + tDatasetID);
      }

      if (verbose)
        String2.log(
            "* "
                + tDatasetID
                + " makeDownloadFileTasks finished."
                + " nTasksCreated="
                + nTasksCreated
                + " time="
                + (System.currentTimeMillis() - startTime)
                + "ms");

    } catch (Throwable t) {
      if (verbose)
        String2.log(
            "ERROR in makeDownloadFileTasks for datasetID="
                + tDatasetID
                + "\n"
                + MustBe.throwableToString(t));
    }

    if (taskNumber > -1) {
      EDStatic.lastAssignedTask.put(tDatasetID, Integer.valueOf(taskNumber));
      EDStatic.ensureTaskThreadIsRunningIfNeeded(); // ensure info is up-to-date

      if (EDStatic.forceSynchronousLoading) {
        boolean interrupted = false;
        while (!interrupted && EDStatic.lastFinishedTask < taskNumber) {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      }
    }
  }

  /**
   * This gets source data from one copied .nc (perhaps .gz) file. See documentation in
   * EDDTableFromFiles.
   *
   * @throws an exception if too much data. This won't throw an exception if no data.
   */
  @Override
  public Table lowGetSourceDataFromFile(
      String tFileDir,
      String tFileName,
      StringArray sourceDataNames,
      String sourceDataTypes[],
      double sortedSpacing,
      double minSorted,
      double maxSorted,
      StringArray sourceConVars,
      StringArray sourceConOps,
      StringArray sourceConValues,
      boolean getMetadata,
      boolean mustGetData)
      throws Throwable {

    // read the file
    Table table = new Table();
    String decompFullName =
        FileVisitorDNLS.decompressIfNeeded(
            tFileDir + tFileName,
            fileDir,
            decompressedDirectory(),
            EDStatic.decompressedCacheMaxGB,
            true); // reuseExisting
    if (mustGetData) {
      table.readNDNc(
          decompFullName,
          sourceDataNames.toArray(),
          standardizeWhat,
          sortedSpacing >= 0 && !Double.isNaN(minSorted) ? sortedColumnSourceName : null,
          minSorted,
          maxSorted);
      // String2.log("  EDDTableFromHyraxFiles.lowGetSourceDataFromFile table.nRows=" +
      // table.nRows());
    } else {
      // Just return a table with globalAtts, columns with atts, but no rows.
      table.readNcMetadata(
          decompFullName, sourceDataNames.toArray(), sourceDataTypes, standardizeWhat);
    }

    return table;
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromHyraxFiles. The XML can
   * then be edited by hand and added to the datasets.xml file.
   *
   * @param tLocalDirUrl the locally useful starting (parent) directory with a Hyrax sub-catalog for
   *     searching for files e.g.,
   *     https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/
   * @param tFileNameRegex the regex that each filename (no directory info) must match (e.g.,
   *     ".*\\.nc") (usually only 1 backslash; 2 here since it is Java code). e.g,
   *     "pentad.*\\.nc\\.gz"
   * @param oneFileDapUrl the locally useful url for one file, without ending .das or .html e.g.,
   *     https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/pentad/flk/1987/M09/pentad_19870908_v11l35flk.nc.gz
   * @param tReloadEveryNMinutes
   * @param tPreExtractRegex part of info for extracting e.g., stationName from file name. Set to ""
   *     if not needed.
   * @param tPostExtractRegex part of info for extracting e.g., stationName from file name. Set to
   *     "" if not needed.
   * @param tExtractRegex part of info for extracting e.g., stationName from file name. Set to "" if
   *     not needed.
   * @param tColumnNameForExtract part of info for extracting e.g., stationName from file name. Set
   *     to "" if not needed.
   * @param tSortedColumnSourceName use "" if not known or not needed.
   * @param tSortFilesBySourceNames This is useful, because it ultimately determines default results
   *     order.
   * @param externalAddGlobalAttributes These attributes are given priority. Use null in none
   *     available.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String tLocalDirUrl,
      String tFileNameRegex,
      String oneFileDapUrl,
      int tReloadEveryNMinutes,
      String tPreExtractRegex,
      String tPostExtractRegex,
      String tExtractRegex,
      String tColumnNameForExtract,
      String tSortedColumnSourceName,
      String tSortFilesBySourceNames,
      int tStandardizeWhat,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromHyraxFiles.generateDatasetsXml"
            + "\nlocalDirUrl="
            + tLocalDirUrl
            + " fileNameRegex="
            + tFileNameRegex
            + "\noneFileDapUrl="
            + oneFileDapUrl
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\nextract pre="
            + tPreExtractRegex
            + " post="
            + tPostExtractRegex
            + " regex="
            + tExtractRegex
            + " colName="
            + tColumnNameForExtract
            + "\nsortedColumn="
            + tSortedColumnSourceName
            + " sortFilesBy="
            + tSortFilesBySourceNames
            + "\nexternalAddGlobalAttributes="
            + externalAddGlobalAttributes);
    if (!String2.isSomething(tLocalDirUrl))
      throw new IllegalArgumentException("localDirUrl wasn't specified.");
    String tPublicDirUrl = convertToPublicSourceUrl(tLocalDirUrl);
    tColumnNameForExtract =
        String2.isSomething(tColumnNameForExtract) ? tColumnNameForExtract.trim() : "";
    tSortedColumnSourceName =
        String2.isSomething(tSortedColumnSourceName) ? tSortedColumnSourceName.trim() : "";
    if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440; // 1440 works well with suggestedUpdateEveryNMillis
    if (!String2.isSomething(oneFileDapUrl))
      String2.log(
          "Found/using sampleFileName="
              + (oneFileDapUrl =
                  FileVisitorDNLS.getSampleFileName(
                      tLocalDirUrl, tFileNameRegex, true, ".*"))); // recursive, pathRegex
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();
    DConnect dConnect = new DConnect(oneFileDapUrl, acceptDeflate, 1, 1);
    DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    ;
    DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

    // get source global attributes
    OpendapHelper.getAttributes(das, "GLOBAL", dataSourceTable.globalAttributes());

    // variables
    Enumeration en = dds.getVariables();
    double maxTimeES = Double.NaN;
    Attributes gridMappingAtts = null;
    while (en.hasMoreElements()) {
      BaseType baseType = (BaseType) en.nextElement();
      String varName = baseType.getName();
      Attributes sourceAtts = new Attributes();
      OpendapHelper.getAttributes(das, varName, sourceAtts);

      // Is this the pseudo-data var with CF grid_mapping (projection) information?
      if (gridMappingAtts == null) gridMappingAtts = NcHelper.getGridMappingAtts(sourceAtts);

      PrimitiveVector pv = null; // for determining data type
      if (baseType instanceof DGrid dGrid) { // for multidim vars
        BaseType bt0 = dGrid.getVar(0); // holds the data
        pv = bt0 instanceof DArray tbt0 ? tbt0.getPrimitiveVector() : bt0.newPrimitiveVector();
      } else if (baseType instanceof DArray dArray) { // for the dimension vars
        pv = dArray.getPrimitiveVector();
      } else {
        if (verbose) String2.log("  baseType=" + baseType.toString() + " isn't supported yet.\n");
      }
      if (pv != null) {
        PrimitiveArray sourcePA =
            PrimitiveArray.factory(OpendapHelper.getElementPAType(pv), 2, false);
        dataSourceTable.addColumn(dataSourceTable.nColumns(), varName, sourcePA, sourceAtts);
        PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
        dataAddTable.addColumn(
            dataAddTable.nColumns(),
            varName,
            destPA,
            makeReadyToUseAddVariableAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(),
                sourceAtts,
                null,
                varName,
                destPA.elementType() != PAType.STRING, // tryToAddStandardName
                destPA.elementType() != PAType.STRING, // addColorBarMinMax
                true)); // tryToFindLLAT

        // if a variable has timeUnits, files are likely sorted by time
        // and no harm if files aren't sorted that way
        String tUnits = sourceAtts.getString("units");
        if (tSortedColumnSourceName.length() == 0 && Calendar2.isTimeUnits(tUnits))
          tSortedColumnSourceName = varName;

        if (!Double.isFinite(maxTimeES) && Calendar2.isTimeUnits(tUnits)) {
          try {
            if (Calendar2.isNumericTimeUnits(tUnits)) {
              double tbf[] = Calendar2.getTimeBaseAndFactor(tUnits); // throws exception
              maxTimeES =
                  Calendar2.unitsSinceToEpochSeconds(
                      tbf[0], tbf[1], destPA.getDouble(destPA.size() - 1));
            } else { // string time units
              maxTimeES =
                  Calendar2.tryToEpochSeconds(
                      destPA.getString(destPA.size() - 1)); // NaN if trouble
            }
          } catch (Throwable t) {
            String2.log("caught while trying to get maxTimeES: " + MustBe.throwableToString(t));
          }
        }
      }
    }

    // add the columnNameForExtract variable
    if (tColumnNameForExtract.length() > 0) {
      Attributes atts = new Attributes();
      atts.add("ioos_category", "Identifier");
      atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
      // no units or standard_name
      dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
      dataAddTable.addColumn(0, tColumnNameForExtract, new StringArray(), atts);
    }

    // add missing_value and/or _FillValue if needed
    addMvFvAttsIfNeeded(dataSourceTable, dataAddTable);

    // global attributes
    if (externalAddGlobalAttributes == null) externalAddGlobalAttributes = new Attributes();
    externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", tPublicDirUrl);

    // tryToFindLLAT
    tryToFindLLAT(dataSourceTable, dataAddTable);

    // externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
    // after dataVariables known, add global attributes in the dataAddTable
    dataAddTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(),
                // another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable) ? "Point" : "Other",
                tLocalDirUrl,
                externalAddGlobalAttributes,
                suggestKeywords(dataSourceTable, dataAddTable)));
    if (gridMappingAtts != null) dataAddTable.globalAttributes().add(gridMappingAtts);

    // subsetVariables
    if (dataSourceTable.globalAttributes().getString("subsetVariables") == null
        && dataAddTable.globalAttributes().getString("subsetVariables") == null)
      dataAddTable
          .globalAttributes()
          .add("subsetVariables", suggestSubsetVariables(dataSourceTable, dataAddTable, false));

    // use maxTimeES
    String tTestOutOfDate =
        EDD.getAddOrSourceAtt(
            dataSourceTable.globalAttributes(),
            dataAddTable.globalAttributes(),
            "testOutOfDate",
            null);
    if (Double.isFinite(maxTimeES) && !String2.isSomething(tTestOutOfDate)) {
      tTestOutOfDate = suggestTestOutOfDate(maxTimeES);
      if (String2.isSomething(tTestOutOfDate))
        dataAddTable.globalAttributes().set("testOutOfDate", tTestOutOfDate);
    }

    // write the information
    StringBuilder sb = new StringBuilder();
    if (tSortFilesBySourceNames.length() == 0) {
      if (tColumnNameForExtract.length() > 0
          && tSortedColumnSourceName.length() > 0
          && !tColumnNameForExtract.equals(tSortedColumnSourceName))
        tSortFilesBySourceNames = tColumnNameForExtract + ", " + tSortedColumnSourceName;
      else if (tColumnNameForExtract.length() > 0) tSortFilesBySourceNames = tColumnNameForExtract;
      else tSortFilesBySourceNames = tSortedColumnSourceName;
    }
    sb.append(
        "<dataset type=\"EDDTableFromHyraxFiles\" datasetID=\""
            + suggestDatasetID(tPublicDirUrl + tFileNameRegex)
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>0</updateEveryNMillis>\n"
            + // files are only added by full reload
            "    <fileDir></fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(tFileNameRegex)
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>"
            + tStandardizeWhat
            + "</standardizeWhat>\n"
            + (String2.isSomething(tColumnNameForExtract)
                ? // Discourage Extract. Encourage sourceName=***fileName,...
                "    <preExtractRegex>"
                    + XML.encodeAsXML(tPreExtractRegex)
                    + "</preExtractRegex>\n"
                    + "    <postExtractRegex>"
                    + XML.encodeAsXML(tPostExtractRegex)
                    + "</postExtractRegex>\n"
                    + "    <extractRegex>"
                    + XML.encodeAsXML(tExtractRegex)
                    + "</extractRegex>\n"
                    + "    <columnNameForExtract>"
                    + XML.encodeAsXML(tColumnNameForExtract)
                    + "</columnNameForExtract>\n"
                : "")
            + "    <sortedColumnSourceName>"
            + XML.encodeAsXML(tSortedColumnSourceName)
            + "</sortedColumnSourceName>\n"
            + "    <sortFilesBySourceNames>"
            + XML.encodeAsXML(tSortFilesBySourceNames)
            + "</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n");
    sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
    sb.append(cdmSuggestion());
    sb.append(writeAttsForDatasetsXml(true, dataAddTable.globalAttributes(), "    "));

    // last 2 params: includeDataType, questionDestinationName
    sb.append(
        writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true, false));
    sb.append("</dataset>\n" + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }
}
