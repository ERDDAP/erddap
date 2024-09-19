/*
 * EDDTableFromThreddsFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.LongArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

/**
 * This class downloads data from a THREDDS data server with lots of files into .nc files in the
 * [bigParentDirectory]/copy/datasetID, and then uses superclass EDDTableFromFiles methods to
 * read/serve data from the .nc files. So don't wrap this class in EDDTableCopy.
 *
 * <p>The TDS files can be n-dimensional (1,2,3,4,...) DArray or DGrid OPeNDAP files, each of which
 * is flattened into a table. For example,
 * http://biloxi-bay.ssc.hpc.msstate.edu/dods-bin/nph-dods/WCOS/nmsp/wcos/ (the four dimensions
 * there are e.g., time,depth,lat,lon).
 *
 * <p>This class is very similar to EDDTableFromHyraxFiles.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) originally 2009-06-08;
 *     modified extensively (copy the files first) 2012-02-21.
 */
public class EDDTableFromThreddsFiles extends EDDTableFromFiles {

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
   * <p>The sortedColumnSourceName can't be for a char/String variable because NcHelper binary
   * searches are currently set up for numeric vars only.
   *
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   * @param tFgdcFile This should be the fullname of a file with the FGDC that should be used for
   *     this dataset, or "" (to cause ERDDAP not to try to generate FGDC metadata for this
   *     dataset), or null (to allow ERDDAP to try to generate FGDC metadata for this dataset).
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   */
  public EDDTableFromThreddsFiles(
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
        "EDDTableFromThreddsFiles",
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
   * Create tasks to download files. If getThreddsFileInfo is completelySuccessful, local files that
   * aren't mentioned on the server will be renamed [fileName].ncRemoved . <br>
   * This won't throw an exception.
   *
   * @param catalogUrl should have /catalog/ in the middle and catalog.html or catalog.xml at the
   *     end
   * @param specialMode e.g., "SAMOS" adds special restrictions to accept the file
   */
  public static void makeDownloadFileTasks(
      String tDatasetID,
      String catalogUrl,
      String fileNameRegex,
      boolean recursive,
      String pathRegex,
      String specialMode) {

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
      // catalogUrl = https://data.nodc.noaa.gov/thredds/catalog /nmsp/wcos/ catalog.xml
      // remoteBase = https://data.nodc.noaa.gov/thredds/dodsC   /nmsp/wcos/
      // e.g., a URL  https://data.nodc.noaa.gov/thredds/dodsC   /nmsp/wcos/
      // WES001/2008/WES001_030MTBD029R00_20080429.nc
      if (catalogUrl == null || catalogUrl.length() == 0)
        throw new RuntimeException(
            "ERROR: <sourceUrl>http://.../catalog.html</sourceUrl> "
                + "must be in the addGlobalAttributes section of the datasets.xml "
                + "for datasetID="
                + tDatasetID);
      if (catalogUrl.endsWith("/catalog.html"))
        catalogUrl = File2.forceExtension(catalogUrl, ".xml");
      String lookFor = File2.getDirectory(catalogUrl); // e.g., /nmsp/wcos/ , always at least /
      int po = lookFor.indexOf("/catalog/");
      if (po >= 0) {
        lookFor = lookFor.substring(po + 8); // 8, not 9, so starts with /
      } else {
        po = lookFor.indexOf("/dodsC/");
        if (po >= 0) lookFor = lookFor.substring(po + 6); // 6, not 7, so starts with /
        else
          throw new RuntimeException(
              "ERROR: <sourceUrl>"
                  + catalogUrl
                  + "</sourceUrl> "
                  + "in datasets.xml for datasetID="
                  + tDatasetID
                  + " must have /catalog/ in the middle.");
      }
      int lookForLength = lookFor.length();

      // mimic the remote dir structure in baseDir
      String baseDir = EDStatic.fullCopyDirectory + tDatasetID + "/";
      // e.g. localFile EDStatic.fullCopyDirectory + tDatasetID +  /
      // WES001/2008/WES001_030MTBD029R00_20080429.nc
      File2.makeDirectory(baseDir);

      // gather all sourceFile info
      StringArray sourceFileDir = new StringArray();
      StringArray sourceFileName = new StringArray();
      LongArray sourceFileLastMod = new LongArray();
      boolean completelySuccessful =
          getThreddsFileInfo(
              catalogUrl,
              fileNameRegex,
              recursive,
              pathRegex,
              sourceFileDir,
              sourceFileName,
              sourceFileLastMod);

      // samos-specific:
      // Given file names like KAQP_20120103v30001.nc
      // and                   KAQP_20120103v30101.nc
      // this just keeps the file with the last version number.
      if ("SAMOS".equals(specialMode)) {
        int n = sourceFileName.size();
        if (n > 1) {
          // 1) sort by sourceFileName
          ArrayList<PrimitiveArray> tfTable = new ArrayList();
          tfTable.add(sourceFileDir);
          tfTable.add(sourceFileName);
          tfTable.add(sourceFileLastMod);
          PrimitiveArray.sort(tfTable, new int[] {1}, new boolean[] {true});

          // 2) just keep the last version file
          BitSet keep = new BitSet();
          keep.set(0, n);
          int vpo = sourceFileName.get(0).lastIndexOf('v'), ovpo;
          if (vpo < 0) keep.clear(0);
          for (int i = 1; i < n; i++) { // 1.. since looking back to previous
            ovpo = vpo;
            vpo = sourceFileName.get(i).lastIndexOf('v');
            if (vpo < 0) {
              keep.clear(i);
            } else if (ovpo >= 0) {
              if (sourceFileName
                  .get(i - 1)
                  .substring(0, ovpo)
                  .equals(sourceFileName.get(i).substring(0, vpo))) keep.clear(i - 1);
            }
          }
          sourceFileDir.justKeep(keep);
          sourceFileName.justKeep(keep);
          sourceFileLastMod.justKeep(keep);
        }
        // String2.log(sourceFileName.toNewlineString());
      }

      // Rename (make inactive) local files that shouldn't exist?
      // If getThreddsFileInfo is completelySuccessful and found some files,
      // local files that aren't mentioned on the server will be renamed
      // [fileName].ncRemoved .
      // If not completelySuccessful, perhaps the server is down temporarily,
      // no files will be renamed.
      // !!! This is imperfect. If a remote sub webpage always fails, then
      //  no local files will ever be deleted.
      if (completelySuccessful && sourceFileName.size() > 0) {
        // make a hashset of theoretical local fileNames that will exist
        //  after copying based on getThreddsFileInfo
        HashSet<String> hashset = new HashSet();
        int nFiles = sourceFileName.size();
        for (int f = 0; f < nFiles; f++) {
          String sourceDir = sourceFileDir.get(f);
          po = sourceDir.lastIndexOf(lookFor);
          if (po >= 0) {
            // String2.log("po=" + po + " lookForLength=" + lookForLength + " sourceDir.length=" +
            // sourceDir.length());
            String willExist =
                baseDir + sourceDir.substring(po + lookForLength) + sourceFileName.get(f);
            hashset.add(willExist);
            // String2.log("  willExist=" + willExist);
          } // else continue;
        }

        // get all the existing local files
        String localFiles[] =
            recursive
                ?
                // pathRegex was applied when downloading, no need for it here.
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
        String sourceDir = sourceFileDir.get(f);
        String sourceName = sourceFileName.get(f);
        po = sourceDir.lastIndexOf(lookFor);
        if (po < 0) {
          if (!remoteErrorLogged) {
            String2.log("ERROR! lookFor=" + lookFor + " wasn't in sourceDir=" + sourceDir);
            remoteErrorLogged = true;
          }
          continue;
        }

        // see if up-to-date localFile exists  (keep name identical; don't add .nc)
        String localFile = baseDir + sourceDir.substring(po + lookForLength) + sourceName;
        String reason = "";
        try {
          // don't use File2 so more efficient for current purpose
          File file = new File(localFile);
          if (!file.isFile()) reason = "new file";
          else if (file.lastModified() != sourceFileLastMod.get(f)) reason = "lastModified changed";
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
        taskOA[1] = sourceDir + sourceName;
        taskOA[2] = localFile;
        taskOA[3] = Long.valueOf(sourceFileLastMod.get(f));
        int tTaskNumber = EDStatic.addTask(taskOA);
        if (tTaskNumber >= 0) {
          nTasksCreated++;
          taskNumber = tTaskNumber;
          if (reallyVerbose)
            String2.log(
                "  task#"
                    + taskNumber
                    + " TASK_DAP_TO_NC reason="
                    + reason
                    + "\n    from="
                    + sourceDir
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
   * This gathers file information from a THREDDS file-directory-like catalog that shows directories
   * with lists of files, each of which MUST have a lastModified date (or it is ignored ). This
   * calls itself recursively, adding info to fileDir, fileName and fileLastMod as files are found.
   *
   * @param catalogUrl the url of the current Thredds catalog xml, e.g.,
   *     https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/catalog.xml which leads to
   *     unaggregated datasets (each from a file) like
   *     https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080429.nc(.html)
   *     catalogUrl SHOULD have /catalog/, but sometimes that can be worked around.
   * @param fileNameRegex to be accepted, a fileName (without dir) must match this regex, e.g.,
   *     ".*\\.nc"
   * @param recursive if the method whould descend to subdirectories
   * @param fileDir receives fileDir Url for each accepted file
   * @param fileName receives fileName for each accepted file
   * @param fileLastMod receives lastModified time of each accepted file
   * @return true if the search was completely successful (no failure to get any page).
   * @throws RuntimeException if trouble. Url not responding is not an error.
   */
  public static boolean getThreddsFileInfo(
      String catalogUrl,
      String fileNameRegex,
      boolean recursive,
      String pathRegex,
      StringArray fileDir,
      StringArray fileName,
      LongArray fileLastMod) {

    if (reallyVerbose)
      String2.log("\n<<< getThreddsFileInfo catalogUrl=" + catalogUrl + " regex=" + fileNameRegex);
    boolean completelySuccessful = true;
    if (pathRegex == null || pathRegex.length() == 0) pathRegex = ".*";
    long time = System.currentTimeMillis();

    try {
      int catPo = catalogUrl.indexOf("/catalog/");
      if (catPo < 0) {
        if (verbose)
          String2.log(
              "  WARNING: '/catalog/' not found in"
                  +
                  // e.g., https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/
                  //        ncep.reanalysis.dailyavgs/surface/catalog.xml
                  "\n    catalogUrl="
                  + catalogUrl);
        int tPod = catalogUrl.indexOf("/thredds/dodsC/");
        int tPo = catalogUrl.indexOf("/thredds/");
        if (tPod > 0)
          catalogUrl =
              catalogUrl.substring(0, tPo + 9) + "catalog/" + catalogUrl.substring(tPo + 15);
        else if (tPo > 0)
          catalogUrl =
              catalogUrl.substring(0, tPo + 9) + "catalog/" + catalogUrl.substring(tPo + 9);
        else
          catalogUrl =
              File2.getDirectory(catalogUrl) + "catalog/" + File2.getNameAndExtension(catalogUrl);
        // e.g., https://www.esrl.noaa.gov/psd/thredds/catalog/Datasets/
        //        ncep.reanalysis.dailyavgs/surface/catalog.xml
        if (verbose) String2.log("    so trying catalogUrl=" + catalogUrl);
        catPo = catalogUrl.indexOf("/catalog/");
      }
      String catalogBase = catalogUrl.substring(0, catPo + 9); // ends in "/catalog/";
      if (reallyVerbose) String2.log("  catalogBase=" + catalogBase);

      // e.g., threddsName is usually "thredds"
      String threddsName = File2.getNameAndExtension(catalogUrl.substring(0, catPo));
      int ssPo = catalogUrl.indexOf("//");
      if (ssPo < 0) throw new SimpleException("'//' not found in catalogUrl=" + catalogUrl);
      int sPo = catalogUrl.indexOf('/', ssPo + 2);
      if (sPo < 0) throw new SimpleException("'/' not found in catalogUrl=" + catalogUrl);
      // e.g., threddsBase=https://www.esrl.noaa.gov
      String threddsBase = catalogUrl.substring(0, sPo);
      if (reallyVerbose) String2.log("  threddsBase=" + threddsBase);

      String serviceBase = "/" + threddsName + "/dodsC/"; // default
      if (reallyVerbose)
        String2.log(
            "threddsName="
                + threddsName
                + " threddsBase="
                + threddsBase
                + "\ncatalogBase="
                + catalogBase);
      int nLogged = 0;

      String datasetTags[] = {
        "won't match",
        "<catalog><dataset>",
        "<catalog><dataset><dataset>",
        "<catalog><dataset><dataset><dataset>",
        "<catalog><dataset><dataset><dataset><dataset>",
        "<catalog><dataset><dataset><dataset><dataset><dataset>",
        "<catalog><dataset><dataset><dataset><dataset><dataset><dataset>",
        "<catalog><dataset><dataset><dataset><dataset><dataset><dataset><dataset>"
      };
      String endDatasetTags[] = {
        "won't match",
        "<catalog></dataset>",
        "<catalog><dataset></dataset>",
        "<catalog><dataset><dataset></dataset>",
        "<catalog><dataset><dataset><dataset></dataset>",
        "<catalog><dataset><dataset><dataset><dataset></dataset>",
        "<catalog><dataset><dataset><dataset><dataset><dataset></dataset>",
        "<catalog><dataset><dataset><dataset><dataset><dataset><dataset></dataset>"
      };
      String endDateTags[] = {
        "won't match",
        "<catalog><dataset></date>",
        "<catalog><dataset><dataset></date>",
        "<catalog><dataset><dataset><dataset></date>",
        "<catalog><dataset><dataset><dataset><dataset></date>",
        "<catalog><dataset><dataset><dataset><dataset><dataset></date>",
        "<catalog><dataset><dataset><dataset><dataset><dataset><dataset></date>",
        "<catalog><dataset><dataset><dataset><dataset><dataset><dataset><dataset></date>"
      };
      int nNames = datasetTags.length;
      String names[] = new String[nNames];

      // I could get inputStream from catalogUrl, but then (via recursion) perhaps lots of streams
      // open.
      // I think better to get the entire response (succeed or fail *now*).
      // String2.log(">> catalogUrl=" + catalogUrl);
      byte bytes[] = SSR.getUrlResponseBytes(catalogUrl);
      // String2.log(">> bytes=" + new String(bytes));
      SimpleXMLReader xmlReader = new SimpleXMLReader(new ByteArrayInputStream(bytes));
      // String2.log(">> after bytes");
      try {
        while (true) {
          xmlReader.nextTag();
          String tags = xmlReader.allTags();
          int whichDatasetTag = String2.indexOf(datasetTags, tags);
          int whichEndDatasetTag = String2.indexOf(endDatasetTags, tags);
          int whichEndDateTag = String2.indexOf(endDateTags, tags);

          // <catalogRef xlink:href="2008/catalog.xml" xlink:title="2008" ID="nmsp/wcos/WES001/2008"
          // name=""/>
          if (recursive && tags.endsWith("<catalogRef>")) {
            String href = xmlReader.attributeValue("xlink:href");
            if (href != null) { // look for /...
              if (!href.startsWith("http")) { // if not a complete catalogUrl
                if (href.startsWith("/" + threddsName + "/catalog/")) href = threddsBase + href;
                else if (href.startsWith("./"))
                  href = File2.getDirectory(catalogUrl) + href.substring(2);
                else if (!href.startsWith("/")) href = File2.getDirectory(catalogUrl) + href;
                else href = catalogBase + href.substring(1); // href starts with /
              }
              if (href.matches(pathRegex))
                if (!getThreddsFileInfo(
                    href, fileNameRegex, recursive, pathRegex, fileDir, fileName, fileLastMod))
                  completelySuccessful = false;
            }

            // <dataset name="WES001_030MTBD029R00_20080613.nc"
            // ID="nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc"
            // urlPath="nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc">
            //  <date type="modified">2010-01-09 21:37:24Z</date>
            // </dataset>
          } else if (whichDatasetTag > 0) {
            String tName = xmlReader.attributeValue("name");
            if (tName != null) {
              boolean matches = tName.matches(fileNameRegex);
              if ((verbose && nLogged < 5) || reallyVerbose) {
                String2.log("  tName=" + tName + " matches=" + matches);
                nLogged++;
              }
              if (matches) names[whichDatasetTag] = tName;
            }

          } else if (whichEndDatasetTag > 0) {
            names[whichEndDatasetTag] = null;

          } else if (whichEndDateTag > 0 && names[whichEndDateTag] != null) {
            // "<catalog><dataset><dataset></date>"
            String isoTime = xmlReader.content();
            double epochSeconds = Calendar2.safeIsoStringToEpochSeconds(isoTime);
            if (Double.isNaN(epochSeconds)) {
              if ((verbose && nLogged < 5) || reallyVerbose)
                String2.log("    isoTime=" + isoTime + " evaluates to NaN");
            } else {

              // add to file list
              String dodsUrl =
                  String2.replaceAll(
                      File2.getDirectory(catalogUrl),
                      "/" + threddsName + "/catalog/",
                      "/" + threddsName + "/dodsC/");
              if (reallyVerbose)
                String2.log("    found " + dodsUrl + names[whichEndDateTag] + "   " + isoTime);

              fileDir.add(dodsUrl);
              fileName.add(names[whichEndDateTag]);
              fileLastMod.add(Math2.roundToLong(epochSeconds * 1000));
            }

          } else if (tags.equals("</catalog>")) { // end of file
            break;
          }
        }
      } finally {
        xmlReader.close();
      }
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      completelySuccessful = false;
    }
    if (reallyVerbose)
      String2.log(
          "\n>> leaving getThreddsFileInfo"
              + " nFiles="
              + fileName.size()
              + " completelySuccessful="
              + completelySuccessful
              + " time="
              + (System.currentTimeMillis() - time)
              + "ms");
    return completelySuccessful;
  }

  /**
   * This gets source data from one copied .nc file. See documentation in EDDTableFromFiles.
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
      // String2.log("  EDDTableFromThreddsFiles.lowGetSourceDataFromFile table.nRows=" +
      // table.nRows());
    } else {
      // Just return a table with globalAtts, columns with atts, but no rows.
      table.readNcMetadata(
          decompFullName, sourceDataNames.toArray(), sourceDataTypes, standardizeWhat);
    }

    return table;
  }

  /**
   * This generates a ready-to-use datasets.xml entry for an EDDTableFromThreddsFiles. The XML can
   * then be edited by hand and added to the datasets.xml file.
   *
   * @param tLocalDirUrl the base/starting URL with a Thredds (sub-)catalog, usually ending in
   *     catalog.xml (but sometimes other file names).
   * @param tFileNameRegex the regex that each filename (no directory info) must match (e.g.,
   *     ".*\\.nc") (usually only 1 backslash; 2 here since it is Java code). If null or "", it is
   *     generated to catch the same extension as the sampleFileName (".*" if no extension or e.g.,
   *     ".*\\.nc").
   * @param oneFileDapUrl url for one file, without ending .das or .html
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

    tLocalDirUrl = EDStatic.updateUrls(tLocalDirUrl); // http: to https:
    oneFileDapUrl = EDStatic.updateUrls(oneFileDapUrl); // http: to https:
    String2.log(
        "\n*** EDDTableFromThreddsFiles.generateDatasetsXml"
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
    tColumnNameForExtract =
        String2.isSomething(tColumnNameForExtract) ? tColumnNameForExtract.trim() : "";
    tSortedColumnSourceName =
        String2.isSomething(tSortedColumnSourceName) ? tSortedColumnSourceName.trim() : "";
    if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440; // 1440 works well with suggestedUpdateEveryNMillis

    if (!String2.isSomething(tLocalDirUrl))
      throw new IllegalArgumentException("localDirUrl wasn't specified.");
    if (!String2.isSomething(oneFileDapUrl))
      String2.log(
          "Found/using sampleFileName="
              + (oneFileDapUrl =
                  FileVisitorDNLS.getSampleFileName(
                      tLocalDirUrl, tFileNameRegex, true, ".*"))); // recursive, pathRegex

    String tPublicDirUrl = convertToPublicSourceUrl(tLocalDirUrl);
    String tPublicDirUrlHtml = tPublicDirUrl;
    String tDatasetID = suggestDatasetID(tPublicDirUrl + tFileNameRegex);
    String dir = EDStatic.fullTestCacheDirectory;
    int po1, po2;

    // download the 1 file
    // URL may not have .nc at end.  I think that's okay.  Keep exact file name from URL.
    String ncFileName = File2.getNameAndExtension(oneFileDapUrl);
    OpendapHelper.allDapToNc(oneFileDapUrl, dir + ncFileName);

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    tStandardizeWhat =
        tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE
            ? DEFAULT_STANDARDIZEWHAT
            : tStandardizeWhat;
    dataSourceTable.readNDNc(
        dir + ncFileName, null, tStandardizeWhat, "", Double.NaN, Double.NaN); // constraints

    Table dataAddTable = new Table();
    double maxTimeES = Double.NaN;
    for (int c = 0; c < dataSourceTable.nColumns(); c++) {
      String colName = dataSourceTable.getColumnName(c);
      Attributes sourceAtts = dataSourceTable.columnAttributes(c);
      PrimitiveArray sourcePA = dataSourceTable.getColumn(c);
      PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
      Attributes addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              dataSourceTable.globalAttributes(),
              sourceAtts,
              null,
              colName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true); // tryToFindLLAT
      dataAddTable.addColumn(c, colName, destPA, addAtts);

      // if a variable has timeUnits, files are likely sorted by time
      // and no harm if files aren't sorted that way
      String tUnits = sourceAtts.getString("units");
      if (tSortedColumnSourceName.length() == 0 && Calendar2.isTimeUnits(tUnits))
        tSortedColumnSourceName = colName;

      if (!Double.isFinite(maxTimeES) && Calendar2.isTimeUnits(tUnits)) {
        try {
          if (Calendar2.isNumericTimeUnits(tUnits)) {
            double tbf[] = Calendar2.getTimeBaseAndFactor(tUnits); // throws exception
            maxTimeES =
                Calendar2.unitsSinceToEpochSeconds(
                    tbf[0], tbf[1], destPA.getDouble(destPA.size() - 1));
          } else { // string time units
            maxTimeES =
                Calendar2.tryToEpochSeconds(destPA.getString(destPA.size() - 1)); // NaN if trouble
          }
        } catch (Throwable t) {
          String2.log("caught while trying to get maxTimeES: " + MustBe.throwableToString(t));
        }
      }

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(colName, sourcePA, sourceAtts, addAtts); // sourcePA since strongly typed
    }

    if (tFileNameRegex == null || tFileNameRegex.length() == 0) {
      String tExt = File2.getExtension(oneFileDapUrl);
      if (tExt == null || tExt.length() == 0) tFileNameRegex = ".*";
      else tFileNameRegex = ".*\\" + tExt;
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

    // global metadata
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

    // gather the information
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
        "<dataset type=\"EDDTableFromThreddsFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>0</updateEveryNMillis>\n"
            + // files are only added by full reload
            "    <fileDir></fileDir>  <!-- automatically set to [bigParentDirectory]/copy/"
            + tDatasetID
            + "/ -->\n"
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

  /*
  Time              DArray[Time]
  Depth             DArray[Depth]
  Latitude          DArray[Latitude]
  Longitude         DArray[Longitude]
  Height            DGrid [Depth,Latitude,Longitude]
  Height_flag       DGrid [Depth,Latitude,Longitude]
  Pressure          DGrid [Time,Latitude,Longitude]
  Pressure_flag     DGrid [Time,Latitude,Longitude]
  Temperature       DGrid [Time,Latitude,Longitude]
  Temperature_flag  DGrid [Time,Latitude,Longitude]
  WaterDepth        DGrid [Time,Latitude,Longitude]
  WaterDepth_flag   DGrid [Time,Latitude,Longitude]
  YearDay           DGrid [Time,Latitude,Longitude]
  YearDay_flag      DGrid [Time,Latitude,Longitude]
  DataQuality       DGrid [Time,Depth,Latitude,Longitude]
  DataQuality_flag  DGrid [Time,Depth,Latitude,Longitude]
  Eastward          DGrid [Time,Depth,Latitude,Longitude]
  Eastward_flag     DGrid [Time,Depth,Latitude,Longitude]
  ErrorVelocity     DGrid [Time,Depth,Latitude,Longitude]
  ErrorVelocity_flag DGrid [Time,Depth,Latitude,Longitude]
  Intensity         DGrid [Time,Depth,Latitude,Longitude]
  Intensity_flag    DGrid [Time,Depth,Latitude,Longitude]
  Northward         DGrid [Time,Depth,Latitude,Longitude]
  Northward_flag    DGrid [Time,Depth,Latitude,Longitude]
  Upwards_flag      DGrid [Time,Depth,Latitude,Longitude]
  Upwards           DGrid [Time,Depth,Latitude,Longitude]
  */
}
