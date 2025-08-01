/*
 * LoadDatasets Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.sun.management.UnixOperatingSystemMXBean;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.handlers.SaxHandler;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.variable.EDV;
import io.prometheus.metrics.model.snapshots.Unit;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is run in a separate thread to load datasets for ERDDAP. !!!A lot of possible thread
 * synchronization issues in this class don't arise because of the assumption that only one
 * LoadDatasets thread will be running at once (coordinated by RunLoadDatasets).
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Different datasets can have different reloadEveryNMinutes settings.
 *   <li>This doesn't load datasets if they exist and are young, so don't need to be reloaded.
 *   <li>Erddap starts up quickly (although without all the datasets). (The CWBrowsers started up
 *       slowly.)
 *   <li>Datasets are made available in Erddap one-by-one as they are loaded (not in batch mode).
 *   <li>Loading datasets takes time, but is done in a separate thread so it never slows down
 *       requests for a dataset.
 *   <li>Only one thread is used to load all the datasets, so loading datasets never becomes a drain
 *       of computer resources.
 *   <li>The datasets.xml file is read anew each time this is run, so you can make changes to the
 *       file (e.g., add datasets or change metadata) and the results take effect without restarting
 *       Erddap.
 * </ul>
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-02-12
 */
public class LoadDatasets extends Thread {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  // *** things set by constructor
  private final Erddap erddap;
  private final String datasetsRegex;
  private InputStream inputStream;
  private final boolean majorLoad;
  private long lastLuceneUpdate = System.currentTimeMillis();

  private static final boolean ADD = true;
  private static final boolean REMOVE = false;

  /* This is set by run if there is an unexpected error. */
  public String unexpectedError = "";

  /**
   * This is a collection of all the exceptions from all the datasets that didn't load successfully
   * and other warnings from LoadDatasets. It will be length=0 if no warnings.
   */
  public final StringBuilder warningsFromLoadDatasets = new StringBuilder();

  /**
   * The constructor.
   *
   * @param erddap Calling run() places results back in erddap as they become available
   * @param datasetsRegex usually either EDStatic.config.datasetsRegex or a custom regex for flagged
   *     datasets.
   * @param inputStream with the datasets.xml information. There is no need to wrap this in a
   *     buffered InputStream -- that will be done here. If null, run() will make a copy of
   *     [EDStatic.config.contentDirectory]/datasets.xml and make an inputStream from the copy.
   * @param majorLoad if true, this does time-consuming garbage collection, logs memory usage
   *     information, and checks if Daily report should be sent.
   */
  public LoadDatasets(
      Erddap erddap, String datasetsRegex, InputStream inputStream, boolean majorLoad) {
    this.erddap = erddap;
    this.datasetsRegex = datasetsRegex;
    this.inputStream = inputStream;
    this.majorLoad = majorLoad;
    setName("LoadDatasets");
  }

  /**
   * This reads datasets[2].xml and loads all the datasets, placing results back in erddap as they
   * become available.
   */
  @Override
  public void run() {
    StringArray changedDatasetIDs = new StringArray();
    try {
      String2.log(
          "\n"
              + String2.makeString('*', 80)
              + "\nLoadDatasets.run EDStatic.config.developmentMode="
              + EDStatic.config.developmentMode
              + " "
              + Calendar2.getCurrentISODateTimeStringLocalTZ()
              + "\n  datasetsRegex="
              + datasetsRegex
              + " inputStream="
              + (inputStream == null ? "null" : "something")
              + " majorLoad="
              + majorLoad);
      long memoryInUse = 0;
      if (majorLoad) {
        // gc so getMemoryInUse more accurate
        // don't use Math2.sleep which catches/ignores interrupt
        System.gc();
        Thread.sleep(Math2.shortSleep); // before get memoryString
        System.gc();
        Thread.sleep(Math2.shortSleep); // before get memoryString
        memoryInUse = Math2.getMemoryInUse();
        String2.log(Math2.memoryString() + " " + Math2.xmxMemoryString());
        // delete decompressed files if not used in last nMinutes (to keep cumulative size down)
        String2.log(
            "After deleting decompressed files not used in the last "
                + EDStatic.decompressedCacheMaxMinutesOld
                + " minutes, nRemain="
                + File2.deleteIfOld(
                    EDStatic.config.fullDecompressedDirectory,
                    System.currentTimeMillis()
                        - EDStatic.decompressedCacheMaxMinutesOld * Calendar2.MILLIS_PER_MINUTE,
                    true,
                    false)); // recursive, deleteEmptySubdirectories
      }
      long startTime = System.currentTimeMillis();
      int oldNGrid = erddap.gridDatasetHashMap.size();
      int oldNTable = erddap.tableDatasetHashMap.size();
      HashSet<String> orphanIDSet = null;
      if (majorLoad) {
        orphanIDSet = new HashSet<>(erddap.gridDatasetHashMap.keySet());
        orphanIDSet.addAll(erddap.tableDatasetHashMap.keySet());
        orphanIDSet.remove(EDDTableFromAllDatasets.DATASET_ID);
      }
      EDStatic.cldMajor = majorLoad;
      EDStatic.cldNTry = 0; // that alone says none is currently active
      Map<String, Object[]> tUserHashMap =
          new HashMap<>(); // no need for thread-safe, all puts are here (1 thread); future gets are
      // thread safe
      StringBuilder datasetsThatFailedToLoadSB = new StringBuilder();
      StringBuilder failedDatasetsWithErrorsSB = new StringBuilder();
      Set<String> datasetIDSet =
          new HashSet<>(); // to detect duplicates, just local use, no need for thread-safe
      StringArray duplicateDatasetIDs = new StringArray(); // list of duplicates
      synchronized (EDStatic.suggestAddFillValueCSV) {
        EDStatic.suggestAddFillValueCSV.setLength(0);
      }

      // ensure EDDTableFromAllDatasets exists
      // If something causes it to not exist, this will recreate it soon.
      try {
        if (!erddap.tableDatasetHashMap.containsKey(EDDTableFromAllDatasets.DATASET_ID))
          erddap.tableDatasetHashMap.put(
              EDDTableFromAllDatasets.DATASET_ID,
              new EDDTableFromAllDatasets(erddap.gridDatasetHashMap, erddap.tableDatasetHashMap));
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
      }

      // decision: how to process xml dataset description
      //  XPath - a standard, but sometimes slow and takes tons of memory
      //    Good: very concise code for parsing
      //    Severe problem: without making schemas, hard to catch/report mistyped tag names
      //    Maybe I should be writing schemas...
      //  SimpleXMLReader - not a standard, but fast
      //    Good: easy to catch/report mistyped tag Names
      //    Low memory use.
      // I went with SimpleXMLReader
      inputStream = getInputStream(inputStream);
      boolean useSaxParser = EDStatic.config.useSaxParser;
      int[] nTryAndDatasets = new int[2];
      if (useSaxParser) {
        SaxHandler.parse(
            inputStream,
            nTryAndDatasets,
            changedDatasetIDs,
            orphanIDSet,
            datasetIDSet,
            duplicateDatasetIDs,
            datasetsThatFailedToLoadSB,
            failedDatasetsWithErrorsSB,
            warningsFromLoadDatasets,
            tUserHashMap,
            majorLoad,
            erddap,
            lastLuceneUpdate,
            datasetsRegex,
            reallyVerbose);
      } else {
        parseUsingSimpleXmlReader(
            nTryAndDatasets,
            changedDatasetIDs,
            orphanIDSet,
            datasetIDSet,
            duplicateDatasetIDs,
            datasetsThatFailedToLoadSB,
            failedDatasetsWithErrorsSB,
            tUserHashMap);
      }
      int nTry = nTryAndDatasets[0];
      int nDatasets = nTryAndDatasets[1];

      validateDatasetsXmlResults();

      erddap.updateLucene(changedDatasetIDs);
      lastLuceneUpdate = System.currentTimeMillis();

      // atomic swap into place
      EDStatic.setUserHashMap(tUserHashMap);
      // datasetsThatFailedToLoad only swapped into place if majorLoad (see below)
      datasetsThatFailedToLoadSB =
          String2.noLongLinesAtSpace(datasetsThatFailedToLoadSB, 100, "    ");
      String dtftl = datasetsThatFailedToLoadSB.toString();
      int ndf = String2.countAll(dtftl, ","); // all have ',' at end (even if just 1)
      String datasetsThatFailedToLoad =
          "n Datasets Failed To Load (in the last "
              + (majorLoad ? "major" : "minor")
              + " LoadDatasets) = "
              + ndf
              + "\n"
              + (datasetsThatFailedToLoadSB.isEmpty() ? "" : "    " + dtftl + "(end)\n");
      StringBuilder errorsDuringMajorReload =
          majorLoad && duplicateDatasetIDs.size() > 0
              ? new StringBuilder(
                  String2.ERROR
                      + ": Duplicate datasetIDs in datasets.xml:\n    "
                      + String2.noLongLinesAtSpace(duplicateDatasetIDs.toString(), 100, "    ")
                      + "\n")
              : new StringBuilder();
      String failedDatasetsWithErrors =
          "Reasons for failing to load datasets: \n" + failedDatasetsWithErrorsSB;
      if (majorLoad && orphanIDSet.size() > 0) {
        // unload them
        emailOrphanDatasetsRemoved(orphanIDSet, changedDatasetIDs, errorsDuringMajorReload);
      }
      int newGridCount = erddap.gridDatasetHashMap.size();
      int newTableCount = erddap.tableDatasetHashMap.size();
      EDStatic.metrics
          .datasetsCount
          .labelValues(Metrics.DatasetCategory.grid.name())
          .set(newGridCount);
      EDStatic.metrics
          .datasetsCount
          .labelValues(Metrics.DatasetCategory.table.name())
          .set(newTableCount);
      EDStatic.metrics.datasetsFailedCount.set(ndf);

      // *** print lots of useful information
      long loadDatasetsTime = System.currentTimeMillis() - startTime;
      String cDateTimeLocal = Calendar2.getCurrentISODateTimeStringLocalTZ();
      String2.log(
          "\n"
              + String2.makeString('*', 80)
              + "\n"
              + "LoadDatasets.run finished at "
              + cDateTimeLocal
              + "  TOTAL TIME="
              + loadDatasetsTime
              + "ms\n"
              + "  nGridDatasets active="
              + newGridCount
              + " change="
              + (newGridCount - oldNGrid)
              + "\n"
              + "  nTableDatasets active="
              + newTableCount
              + " change="
              + (newTableCount - oldNTable)
              + "\n"
              + "  nDatasets in datasets.xml="
              + nDatasets
              + " (nTry="
              + nTry
              + ")\n"
              + "  nUsers="
              + tUserHashMap.size());

      EDStatic.metrics
          .loadDatasetsDuration
          .labelValues(
              majorLoad
                  ? Metrics.LoadDatasetsType.major.name()
                  : Metrics.LoadDatasetsType.minor.name())
          .observe(Unit.millisToSeconds(loadDatasetsTime));
      // minorLoad?
      if (!majorLoad) {
        String2.distributeTime(loadDatasetsTime, EDStatic.minorLoadDatasetsDistribution24);
        String2.distributeTime(loadDatasetsTime, EDStatic.minorLoadDatasetsDistributionTotal);
        String2.log(datasetsThatFailedToLoad);
      }

      // majorLoad?
      if (majorLoad) {
        String2.distributeTime(loadDatasetsTime, EDStatic.majorLoadDatasetsDistribution24);
        String2.distributeTime(loadDatasetsTime, EDStatic.majorLoadDatasetsDistributionTotal);
        // gc so getMemoryInUse more accurate
        // don't use Math2.sleep which catches/ignores interrupt
        System.gc();
        Thread.sleep(Math2.shortSleep); // aggressive, before get memoryString()
        System.gc();
        Thread.sleep(Math2.shortSleep); // aggressive, before get memoryString()
        String memoryString = Math2.memoryString();
        long using = Math2.getMemoryInUse();

        int nResponseSucceeded =
            String2.getTimeDistributionN(EDStatic.responseTimesDistributionLoadDatasets);
        int medianResponseSucceeded =
            Math.max(
                0,
                String2.getTimeDistributionMedian(
                    EDStatic.responseTimesDistributionLoadDatasets, nResponseSucceeded));

        int nResponseFailed =
            String2.getTimeDistributionN(EDStatic.failureTimesDistributionLoadDatasets);
        int medianResponseFailed =
            Math.max(
                0,
                String2.getTimeDistributionMedian(
                    EDStatic.failureTimesDistributionLoadDatasets, nResponseFailed));

        // get thread info
        String threadList = MustBe.allStackTraces(true, true);
        String threadSummary = null;
        String threadCounts = String2.right("", 23);
        int po = threadList.indexOf('\n');
        if (po > 0) {
          // e.g., "Number of threads: Tomcat-waiting=9, inotify=1, other=22"
          threadSummary = threadList.substring(0, po);

          Pattern p = Pattern.compile(".*waiting=(\\d+), inotify=(\\d+), other=(\\d+).*");
          Matcher m = p.matcher(threadSummary);
          if (m.matches()) {
            threadCounts =
                String2.right(m.group(1), 9)
                    + String2.right(m.group(2), 8)
                    + String2.right(m.group(3), 6);
          }
        }

        // get OpenFiles
        String openFiles = getOpenFiles("     ?");

        String2.log(
            "  "
                + memoryString
                + " "
                + Math2.xmxMemoryString()
                + "\n  change for this run of major Load Datasets (MB) = "
                + ((Math2.getMemoryInUse() - memoryInUse) / Math2.BytesPerMB)
                + "\n");

        if (EDStatic.initialLoadDatasets()) handleAfva();

        EDStatic.datasetsThatFailedToLoad = datasetsThatFailedToLoad; // swap into place
        EDStatic.failedDatasetsWithErrors = failedDatasetsWithErrors;
        EDStatic.errorsDuringMajorReload = errorsDuringMajorReload.toString(); // swap into place
        synchronized (EDStatic.majorLoadDatasetsTimeSeriesSB) {
          EDStatic.majorLoadDatasetsTimeSeriesSB.insert(
              0, // header in EDStatic
              // "Major LoadDatasets Time Series: MLD    Datasets Loaded               Requests
              // (median times in ms)                Number of Threads      MB    gc   Open\n" +
              // "  timestamp                    time   nTry nFail nTotal  nSuccess (median) nFail
              // (median) shed memFail tooMany  tomWait inotify other  inUse Calls Files\n" +
              // "----------------------------  -----   -----------------
              // -----------------------------------------------------  ---------------------  -----
              // ----- -----\n"
              "  "
                  + cDateTimeLocal
                  + String2.right("" + (loadDatasetsTime / 1000 + 1), 7)
                  + "s"
                  + // time
                  String2.right("" + nTry, 7)
                  + String2.right("" + ndf, 6)
                  + String2.right(
                      ""
                          + (EDStatic.metrics
                                  .datasetsCount
                                  .labelValues(Metrics.DatasetCategory.grid.name())
                                  .get()
                              + EDStatic.metrics
                                  .datasetsCount
                                  .labelValues(Metrics.DatasetCategory.table.name())
                                  .get()),
                      7)
                  + // nTotal
                  String2.right("" + nResponseSucceeded, 10)
                  + " ("
                  + String2.right("" + Math.min(999999, medianResponseSucceeded), 6)
                  + ")"
                  + String2.right("" + Math.min(999999, nResponseFailed), 6)
                  + " ("
                  + String2.right("" + Math.min(999999, medianResponseFailed), 6)
                  + ")"
                  + String2.right("" + Math.min(99999, EDStatic.requestsShed.get()), 5)
                  + String2.right("" + Math.min(9999999, EDStatic.dangerousMemoryFailures.get()), 8)
                  + String2.right("" + Math.min(9999999, EDStatic.tooManyRequests), 8)
                  + threadCounts
                  + String2.right("" + using / Math2.BytesPerMB, 7)
                  + // memory using
                  String2.right("" + Math2.gcCallCount, 6)
                  + openFiles
                  + "\n");
        }
        // reset  since last majorReload
        Math2.gcCallCount.set(0);
        EDStatic.requestsShed.set(0);
        EDStatic.dangerousMemoryEmails.set(0);
        EDStatic.dangerousMemoryFailures.set(0);
        EDStatic.tooManyRequests = 0;

        // email daily report?, threadSummary-String,
        GregorianCalendar reportCalendar = Calendar2.newGCalendarLocal();
        String reportDate = Calendar2.formatAsISODate(reportCalendar);
        int hour = reportCalendar.get(Calendar2.HOUR_OF_DAY);

        if (!reportDate.equals(erddap.lastReportDate) && hour >= 7) {
          // major reload after 7 of new day, so do daily report!
          emailDailyReport(threadSummary, threadList, reportDate);
        } else {
          // major load, but not daily report
          if (EDStatic.config.unusualActivityFailPercent != -1) {
            emailUnusualActivity(threadSummary, threadList);
          }
        }

        // after every major loadDatasets
        EDStatic.actionsAfterEveryMajorLoadDatasets();
        int tpo = 13200; // 132 char/line * 100 lines
        synchronized (EDStatic.majorLoadDatasetsTimeSeriesSB) {
          if (EDStatic.majorLoadDatasetsTimeSeriesSB.length() > tpo) {
            // hopefully, start looking at exact desired \n location
            int apo = EDStatic.majorLoadDatasetsTimeSeriesSB.indexOf("\n", tpo - 1);
            if (apo >= 0) EDStatic.majorLoadDatasetsTimeSeriesSB.setLength(apo + 1);
          }
        }

        String2.flushLog(); // useful to have this info ASAP and ensure log is flushed periodically
      }

    } catch (Exception e) {
      String2.log(e.toString());
      e.printStackTrace();
    } finally {
      synchronized (EDStatic.suggestAddFillValueCSV) {
        EDStatic.suggestAddFillValueCSV.setLength(0);
      }
    }
  }

  /** Validation checks post the parsing of the datasets.xml file */
  private void validateDatasetsXmlResults() {
    // Check for all languages
    for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
      if (EDStatic.displayAttributeAr.length != EDStatic.displayInfoAr.get(i).length) {
        String2.log("Incorrect input to the displayAttribute and displayInfo tags");
        EDStatic.displayAttributeAr = EDStatic.DEFAULT_displayAttributeAr;
        EDStatic.displayInfoAr = EDStatic.DEFAULT_displayInfoAr;
      }
    }
  }

  private void parseUsingSimpleXmlReader(
      int[] nTryAndDatasets,
      StringArray changedDatasetIDs,
      Set<String> orphanIDSet,
      Set<String> datasetIDSet,
      StringArray duplicateDatasetIDs,
      StringBuilder datasetsThatFailedToLoadSB,
      StringBuilder failedDatasetsWithErrorsSB,
      Map<String, Object[]> tUserHashMap) {
    SimpleXMLReader xmlReader = null;
    int nTry = 0, nDatasets = 0;
    try {
      xmlReader = new SimpleXMLReader(inputStream, "erddapDatasets");
      String startError = "datasets.xml error on line #";
      label:
      while (true) {
        // check for interruption
        if (isInterrupted()) {
          String2.log(
              "*** The LoadDatasets thread was interrupted at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ());
          erddap.updateLucene(changedDatasetIDs);
          return;
        }

        xmlReader.nextTag();
        String tags = xmlReader.allTags();
        switch (tags) {
          case "</erddapDatasets>":
            break label;
          case "<erddapDatasets><dataset>":
            // just load minimal datasets?
            nDatasets++;
            String tId = xmlReader.attributeValue("datasetID");
            if (!String2.isSomething(
                tId)) // "" is trouble. It leads to flagDir being deleted below.
            throw new RuntimeException(
                  startError
                      + xmlReader.lineNumber()
                      + ": "
                      + "This <dataset> doesn't have a datasetID!");
            if (majorLoad) orphanIDSet.remove(tId);

            // Looking for reasons to skip loading this dataset.
            // Test first: skip dataset because it is a duplicate datasetID?
            //  If isDuplicate, act as if this doesn't even occur in datasets.xml.
            //  This is imperfect. It just tests top-level datasets,
            //  not lower level, e.g., within EDDGridCopy.
            boolean skip = false;
            boolean isDuplicate = !datasetIDSet.add(tId);
            if (isDuplicate) {
              skip = true;
              duplicateDatasetIDs.add(tId);
              if (reallyVerbose)
                String2.log("*** skipping datasetID=" + tId + " because it's a duplicate.");
            }

            // Test second: skip dataset because of datasetsRegex?
            if (!skip && !tId.matches(datasetsRegex)) {
              skip = true;
              if (reallyVerbose)
                String2.log("*** skipping datasetID=" + tId + " because of datasetsRegex.");
            }

            // Test third: look at flag/age  or active=false
            long MAX_MILLIS_BEFORE_LUCENE_UPDATE = 5 * Calendar2.MILLIS_PER_MINUTE;
            if (!skip) {
              // always check both flag locations
              boolean isFlagged = File2.delete(EDStatic.config.fullResetFlagDirectory + tId);
              boolean isBadFilesFlagged =
                  File2.delete(EDStatic.config.fullBadFilesFlagDirectory + tId);
              boolean isHardFlagged = File2.delete(EDStatic.config.fullHardFlagDirectory + tId);
              if (isFlagged) {
                String2.log(
                    "*** reloading datasetID=" + tId + " because it was in the flag directory.");

              } else if (isBadFilesFlagged) {
                String2.log(
                    "*** reloading datasetID="
                        + tId
                        + " because it was in the badFilesFlag directory.");
                EDD oldEdd = erddap.gridDatasetHashMap.get(tId);
                if (oldEdd == null) oldEdd = erddap.tableDatasetHashMap.get(tId);
                if (oldEdd != null) {
                  StringArray childDatasetIDs = oldEdd.childDatasetIDs();
                  for (int cd = 0; cd < childDatasetIDs.size(); cd++) {
                    String cid = childDatasetIDs.get(cd);
                    EDD.deleteBadFilesFile(cid); // delete the children's info
                  }
                }
                EDD.deleteBadFilesFile(tId); // the important difference

              } else if (isHardFlagged) {
                String2.log(
                    "*** reloading datasetID="
                        + tId
                        + " because it was in the hardFlag directory.");
                EDD oldEdd = erddap.gridDatasetHashMap.get(tId);
                if (oldEdd == null) oldEdd = erddap.tableDatasetHashMap.get(tId);
                if (oldEdd != null) {
                  StringArray childDatasetIDs = oldEdd.childDatasetIDs();
                  for (int cd = 0; cd < childDatasetIDs.size(); cd++) {
                    String cid = childDatasetIDs.get(cd);
                    EDD.deleteCachedDatasetInfo(cid); // delete the children's info
                    FileVisitorDNLS.pruneCache(
                        EDD.decompressedDirectory(cid), 2, 0.5); // remove as many files as possible
                  }
                }
                tryToUnload(erddap, tId, new StringArray(), true); // needToUpdateLucene

                EDD.deleteCachedDatasetInfo(tId); // the important difference

                FileVisitorDNLS.pruneCache(
                    EDD.decompressedDirectory(tId), 2, 0.5); // remove as many files as possible

              } else {
                // does the dataset already exist and is young?
                EDD oldEdd = erddap.gridDatasetHashMap.get(tId);
                if (oldEdd == null) oldEdd = erddap.tableDatasetHashMap.get(tId);
                if (oldEdd != null) {
                  long minutesOld =
                      oldEdd.creationTimeMillis() <= 0
                          ? // see edd.setCreationTimeTo0
                          Long.MAX_VALUE
                          : (System.currentTimeMillis() - oldEdd.creationTimeMillis()) / 60000;
                  if (minutesOld < oldEdd.getReloadEveryNMinutes()) {
                    // it exists and is young
                    if (reallyVerbose)
                      String2.log(
                          "*** skipping datasetID="
                              + tId
                              + ": it already exists and minutesOld="
                              + minutesOld
                              + " is less than reloadEvery="
                              + oldEdd.getReloadEveryNMinutes());
                    skip = true;
                  }
                }
              }

              // active="false"?  (very powerful)
              String tActiveString = xmlReader.attributeValue("active");
              boolean tActive = tActiveString == null || !tActiveString.equals("false");
              if (!tActive) {
                // marked not active now; was it active?
                boolean needToUpdateLucene =
                    System.currentTimeMillis() - lastLuceneUpdate > MAX_MILLIS_BEFORE_LUCENE_UPDATE;
                if (tryToUnload(erddap, tId, changedDatasetIDs, needToUpdateLucene)) {
                  // yes, it was unloaded
                  String2.log("*** unloaded datasetID=" + tId + " because active=\"false\".");
                  if (needToUpdateLucene)
                    lastLuceneUpdate = System.currentTimeMillis(); // because Lucene was updated
                }

                skip = true;
              }
            }

            // To test just EDDTable datasets...
            // if (xmlReader.attributeValue("type").startsWith("EDDGrid") &&
            //    !tId.startsWith("etopo"))
            //    skip = true;

            if (skip) {
              // skip over the tags for this dataset
              while (!tags.equals("<erddapDatasets></dataset>")) {
                xmlReader.nextTag();
                tags = xmlReader.allTags();
              }
            } else {
              // try to load this dataset
              nTry++;
              String change = "";
              EDD dataset = null, oldDataset = null;
              boolean oldCatInfoRemoved = false;
              long timeToLoadThisDataset = System.currentTimeMillis();
              EDStatic.cldNTry = nTry;
              EDStatic.cldStartMillis = timeToLoadThisDataset;
              EDStatic.cldDatasetID = tId;
              try {
                dataset = EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);

                // check for interruption right before making changes to Erddap
                if (isInterrupted()) { // this is a likely place to catch interruption
                  String2.log(
                      "*** The LoadDatasets thread was interrupted at "
                          + Calendar2.getCurrentISODateTimeStringLocalTZ());
                  erddap.updateLucene(changedDatasetIDs);
                  lastLuceneUpdate = System.currentTimeMillis();
                  return;
                }

                // do several things in quick succession...
                // (??? synchronize on (?) if really need avoid inconsistency)

                // was there a dataset with the same datasetID?
                oldDataset = erddap.gridDatasetHashMap.get(tId);
                if (oldDataset == null) oldDataset = erddap.tableDatasetHashMap.get(tId);

                // if oldDataset existed, remove its info from categoryInfo
                // (check now, before put dataset in place, in case EDDGrid <--> EDDTable)
                if (oldDataset != null) {
                  erddap.addRemoveDatasetInfo(REMOVE, erddap.categoryInfo, oldDataset);
                  oldCatInfoRemoved = true;
                }

                // put dataset in place
                // (hashMap.put atomically replaces old version with new)
                if ((oldDataset == null || oldDataset instanceof EDDGrid)
                    && dataset instanceof EDDGrid eddGrid) {
                  erddap.gridDatasetHashMap.put(tId, eddGrid); // was/is grid

                } else if ((oldDataset == null || oldDataset instanceof EDDTable)
                    && dataset instanceof EDDTable eddTable) {
                  erddap.tableDatasetHashMap.put(tId, eddTable); // was/is table

                } else if (dataset instanceof EDDGrid eddGrid) {
                  if (oldDataset != null) erddap.tableDatasetHashMap.remove(tId); // was table
                  erddap.gridDatasetHashMap.put(tId, eddGrid); // now grid

                } else if (dataset instanceof EDDTable eddTable) {
                  if (oldDataset != null) erddap.gridDatasetHashMap.remove(tId); // was grid
                  erddap.tableDatasetHashMap.put(tId, eddTable); // now table
                }

                // add new info to categoryInfo
                erddap.addRemoveDatasetInfo(ADD, erddap.categoryInfo, dataset);

                // clear the dataset's cache
                // since axis values may have changed and "last" may have changed
                File2.deleteAllFiles(dataset.cacheDirectory());

                change = dataset.changed(oldDataset);
                if (change.isEmpty() && dataset instanceof EDDTable)
                  change = "The dataset was reloaded.";

              } catch (Throwable t) {
                dataset = null;
                timeToLoadThisDataset = System.currentTimeMillis() - timeToLoadThisDataset;

                // check for interruption right before making changes to Erddap
                if (isInterrupted()) { // this is a likely place to catch interruption
                  String tError2 =
                      "*** The LoadDatasets thread was interrupted at "
                          + Calendar2.getCurrentISODateTimeStringLocalTZ();
                  String2.log(tError2);
                  warningsFromLoadDatasets.append(tError2 + "\n\n");
                  erddap.updateLucene(changedDatasetIDs);
                  lastLuceneUpdate = System.currentTimeMillis();
                  return;
                }

                // actually remove old dataset (if any existed)
                EDD tDataset =
                    erddap.gridDatasetHashMap.remove(tId); // always ensure it was removed
                if (tDataset == null) tDataset = erddap.tableDatasetHashMap.remove(tId);
                if (oldDataset == null) oldDataset = tDataset;

                // if oldDataset existed, remove it from categoryInfo
                if (oldDataset != null && !oldCatInfoRemoved)
                  erddap.addRemoveDatasetInfo(REMOVE, erddap.categoryInfo, oldDataset);

                String tError =
                    startError
                        + xmlReader.lineNumber()
                        + "\n"
                        + "While trying to load datasetID="
                        + tId
                        + " (after "
                        + timeToLoadThisDataset
                        + " ms)\n"
                        + MustBe.throwableToString(t);
                String2.log(tError);
                warningsFromLoadDatasets.append(tError + "\n\n");
                datasetsThatFailedToLoadSB.append(tId + ", ");
                failedDatasetsWithErrorsSB.append(tId).append(": ").append(tError).append("\n");

                // stop???
                if (!xmlReader.isOpen()) { // error was really serious
                  throw new RuntimeException(startError + xmlReader.lineNumber() + ": " + t, t);
                }

                // skip over the remaining tags for this dataset
                try {
                  while (!xmlReader.allTags().equals("<erddapDatasets></dataset>"))
                    xmlReader.nextTag();
                } catch (Throwable t2) {
                  throw new RuntimeException(startError + xmlReader.lineNumber() + ": " + t2, t2);
                }

                // change      (if oldDataset=null and new one failed to load, no change)
                if (oldDataset != null) change = tError;
              }
              if (verbose) String2.log("change=" + change);
              EDStatic.cldNTry = nTry;
              EDStatic.cldStartMillis = 0;
              EDStatic.cldDatasetID = null;

              // whether succeeded (new or swapped in) or failed (removed), it was changed
              changedDatasetIDs.add(tId);
              if (System.currentTimeMillis() - lastLuceneUpdate > MAX_MILLIS_BEFORE_LUCENE_UPDATE) {
                erddap.updateLucene(changedDatasetIDs);
                lastLuceneUpdate = System.currentTimeMillis();
              }

              // trigger subscription and dataset.onChange actions (after new dataset is in place)
              EDD cooDataset = dataset == null ? oldDataset : dataset; // currentOrOld, may be null

              Erddap.tryToDoActions(
                  tId,
                  cooDataset,
                  startError + xmlReader.lineNumber() + " with Subscriptions",
                  change);
            }

            break;
          case "<erddapDatasets><angularDegreeUnits>",
              "<erddapDatasets><updateMaxEvents>",
              "<erddapDatasets><unusualActivityFailPercent>",
              "<erddapDatasets><unusualActivity>",
              "<erddapDatasets><convertInterpolateDatasetIDVariableList>",
              "<erddapDatasets><convertInterpolateRequestCSVExample>",
              "<erddapDatasets><endBodyHtml5>",
              "<erddapDatasets><theShortDescriptionHtml>",
              "<erddapDatasets><startBodyHtml5>",
              "<erddapDatasets><startHeadHtml5>",
              "<erddapDatasets><standardPrivacyPolicy>",
              "<erddapDatasets><standardGeneralDisclaimer>",
              "<erddapDatasets><standardDisclaimerOfExternalLinks>",
              "<erddapDatasets><standardDisclaimerOfEndorsement>",
              "<erddapDatasets><standardDataLicenses>",
              "<erddapDatasets><standardContact>",
              "<erddapDatasets><standardLicense>",
              "<erddapDatasets><subscriptionEmailBlacklist>",
              "<erddapDatasets><slowDownTroubleMillis>",
              "<erddapDatasets><requestBlacklist>",
              "<erddapDatasets><partialRequestMaxCells>",
              "<erddapDatasets><partialRequestMaxBytes>",
              "<erddapDatasets><palettes>",
              "<erddapDatasets><nTableThreads>",
              "<erddapDatasets><nGridThreads>",
              "<erddapDatasets><logLevel>",
              "<erddapDatasets><loadDatasetsMaxMinutes>",
              "<erddapDatasets><loadDatasetsMinMinutes>",
              "<erddapDatasets><ipAddressUnlimited>",
              "<erddapDatasets><ipAddressMaxRequestsActive>",
              "<erddapDatasets><ipAddressMaxRequests>",
              "<erddapDatasets><graphBackgroundColor>",
              "<erddapDatasets><emailDiagnosticsToErdData>",
              "<erddapDatasets><drawLandMask>",
              "<erddapDatasets><decompressedCacheMaxMinutesOld>",
              "<erddapDatasets><decompressedCacheMaxGB>",
              "<erddapDatasets></convertToPublicSourceUrl>",
              "<erddapDatasets><commonStandardNames>",
              "<erddapDatasets><cacheMinutes>",
              "<erddapDatasets><cacheClearMinutes>",
              "<erddapDatasets><awsS3OutputBucketUrl>",
              "<erddapDatasets><angularDegreeTrueUnits>":
            break;
          case "<erddapDatasets></angularDegreeUnits>":
            {
              String ts = xmlReader.content();
              if (!String2.isSomething(ts)) ts = EDStatic.DEFAULT_ANGULAR_DEGREE_UNITS;
              EDStatic.angularDegreeUnitsSet =
                  new HashSet<>(
                      String2.toArrayList(
                          StringArray.fromCSVNoBlanks(ts).toArray())); // so canonical

              String2.log(
                  "angularDegreeUnits=" + String2.toCSVString(EDStatic.angularDegreeUnitsSet));

              break;
            }
          case "<erddapDatasets></angularDegreeTrueUnits>":
            {
              String ts = xmlReader.content();
              if (!String2.isSomething(ts)) ts = EDStatic.DEFAULT_ANGULAR_DEGREE_TRUE_UNITS;
              EDStatic.angularDegreeTrueUnitsSet =
                  new HashSet<>(
                      String2.toArrayList(
                          StringArray.fromCSVNoBlanks(ts).toArray())); // so canonical

              String2.log(
                  "angularDegreeTrueUnits="
                      + String2.toCSVString(EDStatic.angularDegreeTrueUnitsSet));

              break;
            }
          case "<erddapDatasets></awsS3OutputBucketUrl>":
            {
              String ts = xmlReader.content();
              if (!String2.isSomething(ts)) ts = null;
              EDStatic.config.awsS3OutputBucketUrl = ts;
              String2.log("awsS3OutputBucketUrl=" + ts);

              break;
            }
          case "<erddapDatasets></cacheMinutes>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.cacheMillis =
                  (tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.config.DEFAULT_cacheMinutes : tnt)
                      * Calendar2.MILLIS_PER_MINUTE;
              String2.log(
                  "cacheMinutes=" + EDStatic.config.cacheMillis / Calendar2.MILLIS_PER_MINUTE);

              break;
            }
          case "<erddapDatasets></cacheClearMinutes>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.cacheClearMillis =
                  (tnt < 1 || tnt == Integer.MAX_VALUE ? EDConfig.DEFAULT_cacheMinutes / 4 : tnt)
                      * Calendar2.MILLIS_PER_MINUTE;

              if (reallyVerbose) {
                String2.log(
                    "cacheClearMinutes="
                        + EDStatic.config.cacheClearMillis / Calendar2.MILLIS_PER_MINUTE);
              }
              break;
            }
          case "<erddapDatasets></commonStandardNames>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.commonStandardNames =
                  String2.isSomething(ts)
                      ? String2.canonical(StringArray.arrayFromCSV(ts))
                      : EDStatic.messages.DEFAULT_commonStandardNames;
              String2.log(
                  "commonStandardNames="
                      + String2.toCSSVString(EDStatic.messages.commonStandardNames));

              break;
            }
          case "<erddapDatasets><convertToPublicSourceUrl>":
            String tFrom = xmlReader.attributeValue("from");
            String tTo = xmlReader.attributeValue("to");
            int spo = EDStatic.convertToPublicSourceUrlFromSlashPo(tFrom);
            if (tFrom != null && tFrom.length() > 3 && spo == tFrom.length() - 1 && tTo != null)
              EDStatic.convertToPublicSourceUrl.put(tFrom, tTo);
            break;
          case "<erddapDatasets></decompressedCacheMaxGB>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.decompressedCacheMaxGB =
                  tnt < 1 || tnt == Integer.MAX_VALUE
                      ? EDStatic.DEFAULT_decompressedCacheMaxGB
                      : tnt;
              String2.log("decompressedCacheMaxGB=" + EDStatic.decompressedCacheMaxGB);

              break;
            }
          case "<erddapDatasets></decompressedCacheMaxMinutesOld>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.decompressedCacheMaxMinutesOld =
                  tnt < 1 || tnt == Integer.MAX_VALUE
                      ? EDStatic.DEFAULT_decompressedCacheMaxMinutesOld
                      : tnt;
              String2.log(
                  "decompressedCacheMaxMinutesOld=" + EDStatic.decompressedCacheMaxMinutesOld);

              break;
            }
          case "<erddapDatasets></drawLandMask>":
            {
              String ts = xmlReader.content();
              int tnt = SgtMap.drawLandMask_OPTIONS.indexOf(ts);
              EDStatic.config.drawLandMask =
                  tnt < 1
                      ? EDStatic.config.DEFAULT_drawLandMask
                      : SgtMap.drawLandMask_OPTIONS.get(tnt);
              String2.log("drawLandMask=" + EDStatic.config.drawLandMask);

              break;
            }
          case "<erddapDatasets></emailDiagnosticsToErdData>":
            {
              String ts = xmlReader.content();
              boolean ted = !String2.isSomething(ts) || String2.parseBoolean(ts); // the default

              EDStatic.config.emailDiagnosticsToErdData = ted;
              String2.log("emailDiagnosticsToErdData=" + ted);

              break;
            }
          case "<erddapDatasets></graphBackgroundColor>":
            {
              String ts = xmlReader.content();
              int tnt =
                  String2.isSomething(ts)
                      ? String2.parseInt(ts)
                      : EDStatic.config.DEFAULT_graphBackgroundColorInt;
              EDStatic.config.graphBackgroundColor = new Color(tnt, true); // hasAlpha

              String2.log("graphBackgroundColor=" + String2.to0xHexString(tnt, 8));

              break;
            }
          case "<erddapDatasets></ipAddressMaxRequests>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              tnt = tnt < 6 || tnt > 1000 ? EDStatic.DEFAULT_ipAddressMaxRequests : tnt;
              EDStatic.ipAddressMaxRequests = tnt;
              String2.log("ipAddressMaxRequests=" + tnt);

              break;
            }
          case "<erddapDatasets></ipAddressMaxRequestsActive>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              tnt = tnt < 1 || tnt > 100 ? EDStatic.DEFAULT_ipAddressMaxRequestsActive : tnt;
              EDStatic.ipAddressMaxRequestsActive = tnt;
              String2.log("ipAddressMaxRequestsActive=" + tnt);

              break;
            }
          case "<erddapDatasets></ipAddressUnlimited>":
            {
              String ts = xmlReader.content();
              String sar[] =
                  StringArray.fromCSVNoBlanks(ts + EDStatic.DEFAULT_ipAddressUnlimited).toArray();
              EDStatic.ipAddressUnlimited =
                  new HashSet<>(String2.toArrayList(sar)); // atomically swap into place

              // then remove all these from ipAddressQueue
              // This also offers a way to solve problem where a user has a
              // request permanently in the ipAddressQueue so s/he only receives
              // "Timeout waiting for your other requests to process.":
              // This clears his/her ipAddressQueue.
              for (String s : sar) EDStatic.ipAddressQueue.remove(s);
              String2.log("ipAddressUnlimited=" + String2.toCSVString(EDStatic.ipAddressUnlimited));

              break;
            }
          case "<erddapDatasets></loadDatasetsMinMinutes>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.loadDatasetsMinMillis =
                  (tnt < 1 || tnt == Integer.MAX_VALUE
                          ? EDStatic.config.DEFAULT_loadDatasetsMinMinutes
                          : tnt)
                      * Calendar2.MILLIS_PER_MINUTE;
              String2.log(
                  "loadDatasetsMinMinutes="
                      + EDStatic.config.loadDatasetsMinMillis / Calendar2.MILLIS_PER_MINUTE);

              break;
            }
          case "<erddapDatasets></loadDatasetsMaxMinutes>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.loadDatasetsMaxMillis =
                  (tnt < 1 || tnt == Integer.MAX_VALUE
                          ? EDStatic.config.DEFAULT_loadDatasetsMaxMinutes
                          : tnt)
                      * Calendar2.MILLIS_PER_MINUTE;
              String2.log(
                  "loadDatasetsMaxMinutes="
                      + EDStatic.config.loadDatasetsMaxMillis / Calendar2.MILLIS_PER_MINUTE);

              break;
            }
          case "<erddapDatasets></logLevel>":
            EDStatic.setLogLevel(
                xmlReader.content()); // ""->"info".  It prints diagnostic to log.txt.

            break;
          case "<erddapDatasets></nGridThreads>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.nGridThreads =
                  tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_nGridThreads : tnt;
              String2.log("nGridThreads=" + EDStatic.nGridThreads);

              break;
            }
          case "<erddapDatasets></nTableThreads>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.nTableThreads =
                  tnt < 1 || tnt == Integer.MAX_VALUE ? EDStatic.DEFAULT_nTableThreads : tnt;
              String2.log("nTableThreads=" + EDStatic.nTableThreads);

              break;
            }
          case "<erddapDatasets></palettes>":
            String tContent = xmlReader.content();
            String tPalettes[] =
                String2.isSomething(tContent)
                    ? String2.split(tContent, ',')
                    : EDStatic.messages.DEFAULT_palettes;
            // ensure that all of the original palettes are present
            Set<String> newPaletteSet = String2.stringArrayToSet(tPalettes);
            // String2.log(">>> newPaletteSet=" + String2.toCSSVString(newPaletteSet));
            // String2.log(">>> defPaletteSet=" +
            // String2.toCSSVString(EDStatic.messages.DEFAULT_palettes_set));

            if (!newPaletteSet.containsAll(EDStatic.messages.DEFAULT_palettes_set))
              throw new RuntimeException(
                  "The <palettes> tag MUST include all of the palettes listed in the <palettes> tag in messages.xml.");
            String tPalettes0[] = new String[tPalettes.length + 1];
            tPalettes0[0] = "";
            System.arraycopy(tPalettes, 0, tPalettes0, 1, tPalettes.length);
            // then copy into place
            EDStatic.messages.palettes = tPalettes;
            EDStatic.messages.palettes0 = tPalettes0;
            String2.log("palettes=" + String2.toCSSVString(tPalettes));

            break;
          case "<erddapDatasets></partialRequestMaxBytes>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.partialRequestMaxBytes =
                  tnt < 1000000 || tnt == Integer.MAX_VALUE
                      ? EDStatic.config.DEFAULT_partialRequestMaxBytes
                      : tnt;
              String2.log("partialRequestMaxBytes=" + EDStatic.config.partialRequestMaxBytes);

              break;
            }
          case "<erddapDatasets></partialRequestMaxCells>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.partialRequestMaxCells =
                  tnt < 1000 || tnt == Integer.MAX_VALUE
                      ? EDStatic.config.DEFAULT_partialRequestMaxCells
                      : tnt;
              String2.log("partialRequestMaxCells=" + EDStatic.config.partialRequestMaxCells);

              break;
            }
          case "<erddapDatasets></requestBlacklist>":
            EDStatic.setRequestBlacklist(xmlReader.content());

            break;
          case "<erddapDatasets></slowDownTroubleMillis>":
            int tms = String2.parseInt(xmlReader.content());
            EDStatic.config.slowDownTroubleMillis = tms < 0 || tms > 1000000 ? 1000 : tms;
            String2.log("slowDownTroubleMillis=" + EDStatic.config.slowDownTroubleMillis);

            break;
          case "<erddapDatasets></subscriptionEmailBlacklist>":
            if (EDStatic.config.subscriptionSystemActive)
              EDStatic.subscriptions.setEmailBlacklist(xmlReader.content());

            break;
          case "<erddapDatasets></standardLicense>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.standardLicense =
                  String2.isSomething(ts) ? ts : EDStatic.messages.DEFAULT_standardLicense;
              String2.log("standardLicense was set.");

              break;
            }
          case "<erddapDatasets></standardContact>":
            {
              String ts = xmlReader.content();
              ts =
                  String2.isSomething(ts)
                      ? ts
                      : EDStatic.messages.get(Message.DEFAULT_STANDARD_CONTACT, 0);
              ts =
                  String2.replaceAll(
                      ts, "&adminEmail;", SSR.getSafeEmailAddress(EDStatic.config.adminEmail));
              EDStatic.messages.setDefault(
                  Message.STANDARD_CONTACT, ts, Message.DEFAULT_STANDARD_CONTACT);

              String2.log("standardContact was set.");

              break;
            }
          case "<erddapDatasets></standardDataLicenses>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.setDefault(
                  Message.STANDARD_DATA_LICENSES, ts, Message.DEFAULT_STANDARD_DATA_LICENSES);
              String2.log("standardDataLicenses was set.");

              break;
            }
          case "<erddapDatasets></standardDisclaimerOfEndorsement>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.setDefault(
                  Message.STANDARD_DISCLAIMER_OF_ENDORSEMENT,
                  ts,
                  Message.DEFAULT_STANDARD_DISCLAIMER_OF_ENDORSEMENT);
              String2.log("standardDisclaimerOfEndorsement was set.");

              break;
            }
          case "<erddapDatasets></standardDisclaimerOfExternalLinks>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.setDefault(
                  Message.STANDARD_DISCLAIMER_OF_EXTERNAL_LINKS,
                  ts,
                  Message.DEFAULT_STANDARD_DISCLAIMER_OF_EXTERNAL_LINKS);
              String2.log("standardDisclaimerOfExternalLinks was set.");

              break;
            }
          case "<erddapDatasets></standardGeneralDisclaimer>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.setDefault(
                  Message.STANDARD_GENERAL_DISCLAIMER,
                  ts,
                  Message.DEFAULT_STANDARD_GENERAL_DISCLAIMER);
              String2.log("standardGeneralDisclaimer was set.");

              break;
            }
          case "<erddapDatasets></standardPrivacyPolicy>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.setDefault(
                  Message.STANDARD_PRIVACY_POLICY, ts, Message.DEFAULT_STANDARD_PRIVACY_POLICY);
              String2.log("standardPrivacyPolicy was set.");

              break;
            }
          case "<erddapDatasets></startHeadHtml5>":
            {
              String ts = xmlReader.content();
              ts = String2.isSomething(ts) ? ts : EDStatic.messages.DEFAULT_startHeadHtml;
              if (!ts.startsWith("<!DOCTYPE html>")) {
                String2.log(
                    String2.ERROR
                        + " in datasets.xml: <startHeadHtml> must start with \"<!DOCTYPE html>\". Using default <startHeadHtml> instead.");
                ts = EDStatic.messages.DEFAULT_startHeadHtml;
              }
              EDStatic.messages.startHeadHtml = ts; // swap into place

              String2.log("startHeadHtml5 was set.");

              break;
            }
          case "<erddapDatasets></startBodyHtml5>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.setDefault(
                  Message.START_BODY_HTML, ts, Message.DEFAULT_START_BODY_HTML);

              String2.log("startBodyHtml5 was set.");

              break;
            }
          case "<erddapDatasets></theShortDescriptionHtml>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.setDefault(
                  Message.THE_SHORT_DESCRIPTION_HTML,
                  ts,
                  Message.DEFAULT_THE_SHORT_DESCRIPTION_HTML);
              String2.log("theShortDescriptionHtml was set.");

              break;
            }
          case "<erddapDatasets></endBodyHtml5>":
            {
              String ts = xmlReader.content();
              EDStatic.messages.setDefault(
                  Message.END_BODY_HTML,
                  String2.replaceAll(
                      String2.isSomething(ts)
                          ? ts
                          : EDStatic.messages.get(Message.DEFAULT_END_BODY_HTML, 0),
                      "&erddapVersion;",
                      EDStatic.erddapVersion.getVersion()),
                  Message.DEFAULT_END_BODY_HTML);
              String2.log("endBodyHtml5 was set.");

              break;
            }
          case "<erddapDatasets></convertInterpolateRequestCSVExample>":
            EDStatic.convertInterpolateRequestCSVExample = xmlReader.content();
            String2.log("convertInterpolateRequestCSVExample=" + xmlReader.content());

            break;
          case "<erddapDatasets></convertInterpolateDatasetIDVariableList>":
            {
              String sar[] = StringArray.arrayFromCSV(xmlReader.content());
              EDStatic.convertInterpolateDatasetIDVariableList = sar;
              String2.log("convertInterpolateDatasetIDVariableList=" + String2.toCSVString(sar));

              break;
            }
          case "<erddapDatasets></unusualActivity>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.unusualActivity =
                  tnt < 1 || tnt == Integer.MAX_VALUE
                      ? EDStatic.config.DEFAULT_unusualActivity
                      : tnt;
              String2.log("unusualActivity=" + EDStatic.config.unusualActivity);

              break;
            }
          case "<erddapDatasets></unusualActivityFailPercent>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.unusualActivityFailPercent =
                  tnt < 0 || tnt > 100 || tnt == Integer.MAX_VALUE
                      ? EDStatic.config.DEFAULT_unusualActivityFailPercent
                      : tnt;
              String2.log(
                  "unusualActivityFailPercent=" + EDStatic.config.unusualActivityFailPercent);

              break;
            }
          case "<erddapDatasets></updateMaxEvents>":
            {
              int tnt = String2.parseInt(xmlReader.content());
              EDStatic.config.updateMaxEvents =
                  tnt < 1 || tnt == Integer.MAX_VALUE
                      ? EDStatic.config.DEFAULT_updateMaxEvents
                      : tnt;
              String2.log("updateMaxEvents=" + EDStatic.config.updateMaxEvents);

              // <user username="bsimons" password="..." roles="admin, role1" />
              // this mimics tomcat syntax
              break;
            }
          case "<erddapDatasets><user>":
            String tUsername = xmlReader.attributeValue("username");
            String tPassword = xmlReader.attributeValue("password");
            if (tUsername != null) tUsername = tUsername.trim();
            if (tPassword != null)
              tPassword =
                  tPassword.trim().toLowerCase(); // match Digest Authentication standard case

            String ttRoles = xmlReader.attributeValue("roles");
            String tRoles[] =
                StringArray.arrayFromCSV(
                    (ttRoles == null ? "" : ttRoles + ",") + EDStatic.anyoneLoggedIn,
                    ",",
                    true,
                    false); // splitChars, trim, keepNothing. Result may be String[0].

            // is username nothing?
            if (!String2.isSomething(tUsername)) {
              warningsFromLoadDatasets.append(
                  "datasets.xml error: A <user> tag in datasets.xml had no username=\"someName\" attribute.\n\n");

              // is username reserved?
            } else if (EDStatic.loggedInAsHttps.equals(tUsername)
                || EDStatic.anyoneLoggedIn.equals(tUsername)
                || EDStatic.loggedInAsSuperuser.equals(
                    tUsername)) { // shouldn't be possible because \t would be trimmed above, but
              // double check
              warningsFromLoadDatasets.append(
                  "datasets.xml error: <user> username=\""
                      + String2.annotatedString(tUsername)
                      + "\" is a reserved username.\n\n");

              // is username invalid?
            } else if (!String2.isPrintable(tUsername)) {
              warningsFromLoadDatasets.append(
                  "datasets.xml error: <user> username=\""
                      + String2.annotatedString(tUsername)
                      + "\" has invalid characters.\n\n");

              // is password invalid?
            } else if (EDStatic.config.authentication.equals("custom")
                && // others in future
                !String2.isHexString(tPassword)) {
              warningsFromLoadDatasets.append(
                  "datasets.xml error: The password for <user> username="
                      + tUsername
                      + " in datasets.xml isn't a hexadecimal string.\n\n");

              // a role is not allowed?
            } else if (String2.indexOf(tRoles, EDStatic.loggedInAsSuperuser)
                >= 0) { // not possible because \t would be trimmed, but be doubly sure
              warningsFromLoadDatasets.append(
                  "datasets.xml error: For <user> username="
                      + tUsername
                      + ", the superuser role isn't allowed for any user.\n\n");

              // add user info to tUserHashMap
            } else {
              Arrays.sort(tRoles);
              if ("email".equals(EDStatic.config.authentication)
                  || "google".equals(EDStatic.config.authentication))
                tUsername = tUsername.toLowerCase(); // so case insensitive, to avoid trouble

              if (reallyVerbose)
                String2.log("user=" + tUsername + " roles=" + String2.toCSSVString(tRoles));
              Object o = tUserHashMap.put(tUsername, new Object[] {tPassword, tRoles});
              if (o != null)
                warningsFromLoadDatasets.append(
                    "datasets.xml error: There are two <user> tags in datasets.xml with username="
                        + tUsername
                        + "\nChange one of them.\n\n");
            }

            break;
          case "<erddapDatasets></user>": // do nothing
            break;
          default:
            xmlReader.unexpectedTagException();
            break;
        }
      }
      nTryAndDatasets[0] = nTry;
      nTryAndDatasets[1] = nDatasets;
      xmlReader.close();
      xmlReader = null;
    } catch (Throwable t) {
      if (!isInterrupted()) {
        String subject =
            String2.ERROR
                + " while processing "
                + (xmlReader == null ? "" : "line #" + xmlReader.lineNumber() + " ")
                + "datasets.xml";
        EDStatic.errorsDuringMajorReload =
            subject + ": see log.txt for details.\n"; // swap into place
        String content = MustBe.throwableToString(t);
        unexpectedError = subject + ": " + content;
        String2.log(unexpectedError);
        EDStatic.email(EDStatic.config.emailEverythingToCsv, subject, content);
      }
    } finally {
      if (xmlReader != null)
        try {
          xmlReader.close();
        } catch (Exception e) {
        }
    }
  }

  private void emailOrphanDatasetsRemoved(
      Set<String> orphanIDSet,
      StringArray changedDatasetIDs,
      StringBuilder errorsDuringMajorReload) {
    for (String s : orphanIDSet)
      tryToUnload(erddap, s, changedDatasetIDs, false); // needToUpdateLucene
    erddap.updateLucene(changedDatasetIDs);

    String msg =
        String2.ERROR
            + ": n Orphan Datasets removed (datasets in ERDDAP but not in datasets.xml) = "
            + orphanIDSet.size()
            + "\n"
            + "    "
            + String2.noLongLinesAtSpace(String2.toCSSVString(orphanIDSet), 100, "    ")
            + "(end)\n";
    errorsDuringMajorReload.append(msg);
    String2.log(msg);
    EDStatic.email(EDStatic.config.emailEverythingToCsv, "Orphan Datasets Removed", msg);
  }

  private void emailUnusualActivity(String threadSummary, String threadList) {
    StringBuilder sb = new StringBuilder();
    EDStatic.addIntroStatistics(sb, true /* includeErrors */, erddap);

    if (threadSummary != null) sb.append(threadSummary + "\n");

    sb.append(
        Math2.gcCallCount
            + " gc calls, "
            + EDStatic.requestsShed
            + " requests shed, and "
            + EDStatic.dangerousMemoryEmails
            + " dangerousMemoryEmails since last major LoadDatasets\n");
    sb.append(Math2.memoryString() + " " + Math2.xmxMemoryString() + "\n\n");
    EDStatic.addCommonStatistics(sb);
    sb.append(
        EDStatic.tally.toString("Large Request, IP address (since last Major LoadDatasets)", 50));
    sb.append(
        EDStatic.tally.toString(
            "OutOfMemory (Array Size), IP Address (since last Major LoadDatasets)", 50));
    sb.append(
        EDStatic.tally.toString(
            "OutOfMemory (Too Big), IP Address (since last Major LoadDatasets)", 50));
    sb.append(
        EDStatic.tally.toString(
            "OutOfMemory (Way Too Big), IP Address (since last Major LoadDatasets)", 50));
    sb.append(
        EDStatic.tally.toString(
            "Request refused: not authorized (since last Major LoadDatasets)",
            50)); // datasetID (not IP address)
    sb.append(
        EDStatic.tally.toString(
            "Requester's IP Address (Allowed) (since last Major LoadDatasets)", 50));
    sb.append(
        EDStatic.tally.toString(
            "Requester's IP Address (Blacklisted) (since last Major LoadDatasets)", 50));
    sb.append(
        EDStatic.tally.toString(
            "Requester's IP Address (Failed) (since last Major LoadDatasets)", 50));
    sb.append(
        EDStatic.tally.toString(
            "Requester's IP Address (Too Many Requests) (since last Major LoadDatasets)", 50));

    sb.append(threadList);
    String2.log(sb.toString());

    // email if some threshold is surpassed???
    int nFailed = String2.getTimeDistributionN(EDStatic.failureTimesDistributionLoadDatasets);
    int nSucceeded = String2.getTimeDistributionN(EDStatic.responseTimesDistributionLoadDatasets);
    if (nFailed + nSucceeded > EDStatic.config.unusualActivity) { // high activity level
      EDStatic.email(
          EDStatic.config.emailEverythingToCsv,
          "Unusual Activity: lots of requests",
          sb.toString());
    } else if (nFailed > 10
        && nFailed
            > nSucceeded
                * EDStatic.config.unusualActivityFailPercent
                / 100) { // >25% of requests fail
      EDStatic.email(
          EDStatic.config.emailEverythingToCsv,
          "Unusual Activity: >"
              + EDStatic.config.unusualActivityFailPercent
              + "% of requests failed",
          sb.toString());
    }
  }

  private void emailDailyReport(String threadSummary, String threadList, String reportDate) {
    erddap.lastReportDate = reportDate;
    String stars = String2.makeString('*', 70);
    String subject = "Daily Report";
    StringBuilder contentSB = new StringBuilder(subject + "\n\n");
    EDStatic.addIntroStatistics(contentSB, true /* includeErrors */, erddap);

    // append number of active threads
    if (threadSummary != null) contentSB.append(threadSummary + "\n");

    contentSB.append(
        Math2.gcCallCount
            + " gc calls, "
            + EDStatic.requestsShed
            + " requests shed, and "
            + EDStatic.dangerousMemoryEmails
            + " dangerousMemoryEmails since last major LoadDatasets\n");
    contentSB.append(Math2.memoryString() + " " + Math2.xmxMemoryString() + "\n\n");
    contentSB.append(stars + "\nTallied Usage Information\n\n");
    contentSB.append(EDStatic.tally.toString(50));
    EDStatic.addCommonStatistics(contentSB);

    contentSB.append("\n" + stars + "\nWarnings from LoadDatasets\n\n");
    contentSB.append(warningsFromLoadDatasets);

    contentSB.append("\n" + stars + "\n");
    contentSB.append(threadList);

    // clear all the "since last daily report" tallies
    EDStatic.clearDailyTallies();
    // reset these "since last daily report" time distributions
    EDStatic.resetDailyDistributions();

    String2.log("\n" + stars);
    String2.log(contentSB.toString());
    String2.log(
        "\n"
            + String2.javaInfo()
            + // Sort of confidential. This info would simplify attacks on ERDDAP.
            "\n"
            + stars
            + "\nEnd of Daily Report\n");

    // after write to log (before email), add URLs to setDatasetFlag (so only in email to admin)
    contentSB.append(
        "\n"
            + stars
            + "\nsetDatasetFlag URLs can be used to force a dataset to be reloaded (treat as confidential information)\n\n");
    StringArray datasetIDs = erddap.allDatasetIDs();
    datasetIDs.sortIgnoreCase();
    for (int ds = 0; ds < datasetIDs.size(); ds++) {
      contentSB.append(EDD.flagUrl(datasetIDs.get(ds)));
      contentSB.append('\n');
    }

    // after write to log (before email), add subscription info (so only in email to admin)
    if (EDStatic.config.subscriptionSystemActive) {
      try {
        contentSB.append("\n\n" + stars + "\nTreat Subscription Information as Confidential:\n");
        contentSB.append(EDStatic.subscriptions.listSubscriptions());
      } catch (Throwable lst) {
        contentSB.append("LoadDatasets Error: " + MustBe.throwableToString(lst));
      }
    } else {
      contentSB.append("\n\n" + stars + "\nThe email/URL subscription system is not active.\n");
    }

    // write to email
    contentSB.append(
        "\n"
            + String2.javaInfo()
            + // Sort of confidential. This info would simplify attacks on ERDDAP.
            "\n"
            + stars
            + "\nEnd of Daily Report\n");
    String content = contentSB.toString();
    String2.log(subject + ":");
    String2.log(content);
    EDStatic.email(
        String2.ifSomethingConcat(
            EDStatic.config.emailEverythingToCsv, ",", EDStatic.config.emailDailyReportToCsv),
        subject,
        content);

    // once a day, at daily report, empty activeRequests in case some weren't properly removed
    EDStatic.activeRequests.clear();
  }

  private void handleAfva() {
    if (EDStatic.suggestAddFillValueCSV.length() > 0) {
      String tFileName =
          EDStatic.config.fullLogsDirectory
              + "addFillValueAttributes"
              + Calendar2.getCompactCurrentISODateTimeStringLocal()
              + ".csv";
      String contents =
          "datasetID,variableSourceName,attribute\n" + EDStatic.suggestAddFillValueCSV;
      File2.writeToFileUtf8(tFileName, contents);
      String afva =
          "ADD _FillValue ATTRIBUTES?\n"
              + "The datasets/variables in the table below have integer source data, but no\n"
              + "_FillValue or missing_value attribute. We recommend adding the suggested\n"
              + "attributes to the variable's <addAttributes> in datasets.xml to identify\n"
              + "the default _FillValue used by ERDDAP. You can do this by hand or with the\n"
              + "addFillValueAttributes option in GenerateDatasetsXml.\n"
              + "If you don't make these changes, ERDDAP will treat those values (e.g., 127\n"
              + "for byte variables), if any, as valid data values (for example, on graphs\n"
              + "and when calculating statistics).\n"
              + "Or, if you decide a variable should not have a _FillValue attribute, you can add\n"
              + "  <att name=\"_FillValue\">null</att>\n"
              + "instead, which will suppress this message for that datasetID+variable\n"
              + "combination in the future.\n"
              + "The list below is created each time you start up ERDDAP.\n"
              + "The list was just written to a UTF-8 CSV file\n"
              + tFileName
              + "\n"
              + "and is shown here:\n"
              + contents
              + "\n";
      String2.log("\n" + afva);
      EDStatic.email(
          String2.ifSomethingConcat(
              EDStatic.config.emailEverythingToCsv, ",", EDStatic.config.emailDailyReportToCsv),
          "ADD _FillValue ATTRIBUTES?", // this exact string is in setupDatasetsXml.html
          afva);

    } else {
      String2.log("ADD _FillValue ATTRIBUTES?  There are none to report.\n");
    }
  }

  private InputStream getInputStream(InputStream inputStream) throws Exception {
    if (inputStream == null) {
      // make a copy of datasets.xml so administrator can change file whenever desired
      //  and not affect simpleXMLReader
      String oldFileName =
          EDStatic.config.contentDirectory
              + "datasets"
              + (EDStatic.config.developmentMode ? "2" : "")
              + ".xml";
      String newFileName = EDStatic.config.bigParentDirectory + "currentDatasets.xml";
      if (!File2.copy(oldFileName, newFileName))
        throw new RuntimeException("Unable to copy " + oldFileName + " to " + newFileName);
      return File2.getBufferedInputStream(
          newFileName); // not File2.getDecompressedBufferedInputStream(). Read file as is.
    } else {
      return new BufferedInputStream(inputStream);
    }
  }

  protected String getOpenFiles(String openFiles) {
    try {
      OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
      if (osBean instanceof UnixOperatingSystemMXBean uBean) {
        long nF = uBean.getOpenFileDescriptorCount();
        long maxF = uBean.getMaxFileDescriptorCount();
        int percent = Math2.narrowToInt((nF * 100) / maxF);
        openFiles = String2.right(percent + "%", 6);
        String msg = "openFileCount=" + nF + " of max=" + maxF + " %=" + percent;
        String2.log(msg);
        if (percent > 50)
          EDStatic.email(
              String2.ifSomethingConcat(
                  EDStatic.config.emailEverythingToCsv, ",", EDStatic.config.emailDailyReportToCsv),
              "Too many open files!!!",
              msg
                  + "\n"
                  + "Anything >50%, is risky.\n"
                  + "Don't let this reach 100% or it will cause horrible problems.\n"
                  + "A (basically) ever-increasing number indicates a file handle leak in ERDDAP.\n"
                  + "In any case, you should increase the maximum number of open files allowed\n"
                  + "and restart ERDDAP. See\n"
                  + "https://erddap.github.io/docs/server-admin/additional-information#too-many-open-files\n");
      }
    } catch (Throwable t) {
      String2.log("Caught: " + MustBe.throwableToString(t));
    }
    return openFiles;
  }

  /** Given a newline separated string in sb, this keeps the newest approximately keepLines. */
  static void removeOldLines(StringBuffer sb, int keepLines, int lineLength) {
    if (sb.length() > (keepLines + 1) * lineLength) {
      int po = sb.indexOf("\n", sb.length() - keepLines * lineLength);
      if (po > 0) sb.delete(0, po + 1);
    }
  }

  /**
   * This unloads a dataset with tId if it was active.
   *
   * <p>Since all the data structures are threadsafe, this is thread-safe to the extent that the
   * data structures won't be corrupted; however, it is still susceptible to incorrect information
   * if 2+ thredds work with the same datasetID at the same time (if one adding and one removing)
   * because of race conditions.
   *
   * @param tId a datasetID
   * @param changedDatasetIDs is a list of <strong>other</strong> changedDatasetIDs that need to be
   *     updated by Lucene if updateLucene is actually called
   * @param needToUpdateLucene if true and if a dataset is actually unloaded, this calls
   *     updateLucene (i.e., commits the changes). Even if updateLucene() is called, it doesn't set
   *     lastLuceneUpdate = System.currentTimeMillis().
   * @return true if tId existed and was unloaded.
   */
  public static boolean tryToUnload(
      Erddap erddap, String tId, StringArray changedDatasetIDs, boolean needToUpdateLucene) {

    EDD oldEdd = erddap.gridDatasetHashMap.remove(tId);
    if (oldEdd == null) {
      oldEdd = erddap.tableDatasetHashMap.remove(tId);
      if (oldEdd == null) return false;
    }

    // it was active; finish removing it
    // do in quick succession...   (???synchronized on ?)
    String2.log("*** unloading datasetID=" + tId);
    erddap.addRemoveDatasetInfo(REMOVE, erddap.categoryInfo, oldEdd);
    File2.deleteAllFiles(EDD.cacheDirectory(tId));
    changedDatasetIDs.add(tId);
    if (needToUpdateLucene) erddap.updateLucene(changedDatasetIDs);
    // do dataset actions so subscribers know it is gone
    Erddap.tryToDoActions(
        tId,
        oldEdd,
        null, // default subject
        "This dataset is currently unavailable.");

    return true;
  }

  /**
   * This low level method adds/removes the global attribute categories of an EDD.
   *
   * <p>"global:keywords" is treated specially: the value is split by "\n" or ",", then each keyword
   * is added separately.
   *
   * @param add determines whether datasetID references will be ADDed or REMOVEd
   * @param catInfo the new categoryInfo
   * @param edd
   * @param id the edd.datasetID()
   */
  protected static void categorizeGlobalAtts(
      boolean add, ConcurrentHashMap catInfo, EDD edd, String id) {

    LocalizedAttributes atts = edd.combinedGlobalAttributes();
    int nCat = EDStatic.config.categoryAttributes.length;
    for (int cat = 0; cat < nCat; cat++) {
      if (EDStatic.config.categoryIsGlobal[cat]) {
        String catName = EDStatic.config.categoryAttributes[cat]; // e.g., global:institution
        String value = atts.getString(EDMessages.DEFAULT_LANGUAGE, catName);
        // String2.log("catName=" + catName + " value=" + String2.toJson(value));

        if (value != null && catName.equals("keywords")) {
          // split keywords, then add/remove
          StringArray vals = StringArray.fromCSVNoBlanks(value);
          for (int i = 0; i < vals.size(); i++) {
            // remove outdated "earth science > "
            String s = vals.get(i).toLowerCase();
            if (s.startsWith("earth science > ")) s = s.substring(16);
            // String2.log("  " + i + "=" + vals[i]);
            addRemoveIdToCatInfo(add, catInfo, catName, String2.modifyToBeFileNameSafe(s), id);
          }
        } else {
          // add/remove the single value
          addRemoveIdToCatInfo(
              add, catInfo, catName, String2.modifyToBeFileNameSafe(value).toLowerCase(), id);
        }
      }
    }
  }

  /**
   * This low level method adds/removes the attributes category references of an EDV.
   *
   * @param add determines whether datasetID references will be ADDed or REMOVEd
   * @param catInfo the new categoryInfo
   * @param edv
   * @param id the edd.datasetID()
   */
  protected static void categorizeVariableAtts(
      boolean add, ConcurrentHashMap catInfo, EDV edv, String id) {

    LocalizedAttributes atts = edv.combinedAttributes();
    int nCat = EDStatic.config.categoryAttributes.length;
    for (int cat = 0; cat < nCat; cat++) {
      if (!EDStatic.config.categoryIsGlobal[cat]) {
        String catName = EDStatic.config.categoryAttributes[cat]; // e.g., standard_name
        String catAtt =
            cat == EDStatic.config.variableNameCategoryAttributeIndex
                ? // special case
                edv.destinationName()
                : atts.getString(EDMessages.DEFAULT_LANGUAGE, catName);
        addRemoveIdToCatInfo(
            add,
            catInfo,
            catName,
            String2.modifyToBeFileNameSafe(catAtt).toLowerCase(), // e.g., sea_water_temperature
            id);
      }
    }
  }

  /**
   * This low level method adds/removes a datasetID to a categorization.
   *
   * @param add determines whether datasetID references will be ADDed or REMOVEd
   * @param catInfo the new categoryInfo
   * @param catName e.g., institution
   * @param catAtt e.g., NDBC
   * @param id the edd.datasetID() e.g., ndbcCWind41002
   */
  protected static void addRemoveIdToCatInfo(
      boolean add, ConcurrentHashMap catInfo, String catName, String catAtt, String id) {

    if (catAtt.length() == 0) return;

    ConcurrentHashMap hm = (ConcurrentHashMap) catInfo.get(catName); // e.g., for institution
    ConcurrentHashMap hs = (ConcurrentHashMap) hm.get(catAtt); // e.g., for NDBC,  acts as hashset
    if (hs == null) {
      if (!add) // remove mode and reference isn't there, so we're done
      return;
      hs = new ConcurrentHashMap(16, 0.75f, 4);
      hm.put(catAtt, hs);
    }
    if (add) {
      hs.put(id, Boolean.TRUE); // Boolean.TRUE is just something to fill the space
    } else {
      if (hs.remove(id) != null && hs.size() == 0) hm.remove(catAtt);
    }
  }
}
