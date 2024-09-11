/*
 * RunLoadDatasets Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.*;
import java.io.File;
import java.nio.file.FileSystems;
import org.apache.lucene.store.NIOFSDirectory;

/**
 * This class is in charge of creating and monitoring LoadDatasets threads.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-02-14
 */
public class RunLoadDatasets extends Thread {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  // *** things set by constructor
  protected Erddap erddap;

  // *** things set by run()
  public LoadDatasets loadDatasets;
  public long lastMajorLoadDatasetsStartTimeMillis = 0;
  public long lastMajorLoadDatasetsStopTimeMillis = 0;

  /**
   * The constructor for RunLoadDatasets to prepare for run().
   *
   * @param erddap LoadDatasets.run() places results back in erddap as they become available
   */
  public RunLoadDatasets(Erddap erddap) {
    this.erddap = erddap;
    setName("RunLoadDatasets");

    if (EDStatic.useLuceneSearchEngine) {
      try {
        // delete old index files
        // Index will be recreated, and Lucense throws exception if it tries to read from old
        // indices.
        File2.deleteAllFiles(EDStatic.fullLuceneDirectory);

        // Since I recreate index when erddap restarted, I can change anything
        //  (e.g., Directory type, Version) any time
        //  (no worries about compatibility with existing index).
        // ??? For now, use NIOFSDirectory,
        //  See NIOFSDirectory javadocs (I need to stop using thread.interrupt).
        EDStatic.luceneDirectory =
            new NIOFSDirectory(FileSystems.getDefault().getPath(EDStatic.fullLuceneDirectory));

        // At start of ERDDAP, always create a new index.  Never re-use existing index.
        // Do it here to use true and also to ensure it can be done.
        EDStatic.createLuceneIndexWriter(true); // throws exception if trouble
      } catch (Throwable t) {
        EDStatic.useLuceneSearchEngine = false;
        throw new RuntimeException(t);
      }
    }
  }

  /**
   * Every loadDatasetsMinMinutes (from setup.xml), this checks on the LoadDatasets thread
   * (interrupting stalled threads and starting new threads when needed).
   */
  @Override
  public void run() {
    // The try/catch situation here is tricky.
    // I don't want any normal exception to cause RunLoadDatasets to stop;
    //   otherwise datasets will never be reloaded.
    // But I don't want to hinder application (including RunLoadDatasets) from
    //   being shut down (via Thread.interrupt or ThreadDeath).

    whileNotInterrupted:
    while (!isInterrupted()) {
      // this loop runs roughly every loadDatasetsMinMinutes
      // at start of loop loadDatasets == null (any previous run has finished or been stopped)

      // ********** Main loop
      try {
        String2.log(
            "\n*** RunLoadDatasets is starting a new MAJOR LoadDatasets thread at "
                + Calendar2.getCurrentISODateTimeStringLocalTZ());

        // delete old files in cache
        int nCacheFiles =
            File2.deleteIfOld(
                EDStatic.fullCacheDirectory, // won't throw exception
                System.currentTimeMillis() - EDStatic.cacheMillis,
                true,
                false); // false: important not to delete empty dirs
        int nPublicFiles =
            File2.deleteIfOld(
                EDStatic.fullPublicDirectory,
                System.currentTimeMillis() - EDStatic.cacheMillis,
                true,
                false); // false: important not to delete empty dirs
        String2.log(
            nPublicFiles
                + " files remain in "
                + EDStatic.fullPublicDirectory
                + "\n"
                + nCacheFiles
                + " files remain in "
                + EDStatic.fullCacheDirectory
                + " and subdirectories.");

        // start a new loadDatasets thread
        lastMajorLoadDatasetsStartTimeMillis = System.currentTimeMillis();
        EDStatic.lastMajorLoadDatasetsStartTimeMillis = lastMajorLoadDatasetsStartTimeMillis;
        loadDatasets = new LoadDatasets(erddap, EDStatic.datasetsRegex, null, true);
        // make a lower priority
        // [commented out: why lower priority?  It may be causing infrequent problems with a dataset
        // not available in a CWBrowser
        // -2 since on some OS's, adjacent priority levels map to same internal level.
        // loadDatasets.setPriority(Math.max(Thread.MIN_PRIORITY,
        // Thread.currentThread().getPriority() - 2));
        EDStatic.runningThreads.put("loadDatasets", loadDatasets);
        loadDatasets.start(); // starts the thread and calls run()

      } catch (Throwable t) {
        if (Thread.currentThread().isInterrupted() || t instanceof InterruptedException) {
          break whileNotInterrupted;
        }
        try {
          String subject =
              "RunLoadDatasets(main loop) error at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ();
          String content = MustBe.throwableToString(t);
          String2.log(subject + ": " + content);
          EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
        } catch (Throwable t2) {
          if (Thread.currentThread().isInterrupted() || t2 instanceof InterruptedException)
            break whileNotInterrupted;
        }
      }

      // ********** Flag loop waits (not longer than 3/4 loadDatasetsMaxMillis)
      try {
        whileWait:
        while (System.currentTimeMillis() - lastMajorLoadDatasetsStartTimeMillis
            < EDStatic.loadDatasetsMaxMillis * 3 / 4) {

          // isInterrupted?
          if (isInterrupted()) break whileNotInterrupted;

          // is loadDatasets just now done?
          if (loadDatasets != null && !loadDatasets.isAlive()) {

            String2.log(
                "\n*** RunLoadDatasets notes that LoadDatasets has finished running as of "
                    + Calendar2.getCurrentISODateTimeStringLocalTZ());

            // get rid of reference
            loadDatasets = null;
            EDStatic.runningThreads.remove("loadDatasets");
            if (lastMajorLoadDatasetsStopTimeMillis < lastMajorLoadDatasetsStartTimeMillis) {
              lastMajorLoadDatasetsStopTimeMillis = System.currentTimeMillis();
              EDStatic.lastMajorLoadDatasetsStopTimeMillis = lastMajorLoadDatasetsStopTimeMillis;
            }
          }

          if (loadDatasets == null) {
            if (System.currentTimeMillis() - lastMajorLoadDatasetsStartTimeMillis
                > EDStatic.loadDatasetsMinMillis) {
              // this is a good time to jump out of whileWait loop
              break whileWait;

            } else {

              // main load datasets finished early; we have free time;
              // so check hardFlag, flag, and badFilesFlag directories
              String fDir[] = {
                EDStatic.fullHardFlagDirectory, // order is used below. see "hs =="
                EDStatic
                    .fullResetFlagDirectory, // so safer to add rather than insert new option before
                // end of list
                EDStatic.fullBadFilesFlagDirectory
              };
              String fDirName[] = {"hardFlag", "flag", "badFilesFlag"};

              for (int hs = 0; hs < fDir.length; hs++) {

                StringArray tFlagNames = new StringArray();
                try {
                  if (File2.isDirectory(fDir[hs])) {
                    tFlagNames = new StringArray(new File(fDir[hs]).list());
                  } else {
                    File2.makeDirectory(fDir[hs]); // throws exception
                  }
                } catch (Exception et) {
                  String2.log(MustBe.throwableToString(et));
                }

                // check flag names
                for (int i = tFlagNames.size() - 1;
                    i >= 0;
                    i--) { // work backwards since deleting some from list
                  String ttName = tFlagNames.get(i);
                  if (File2.isDirectory(fDir[hs] + ttName)) {
                    // It's a directory! It shouldn't be. Ignore it
                    tFlagNames.remove(i);
                  } else {
                    // It's a file.
                    // I don't want odd-named files lying around triggering useless reloads
                    // so delete the flag file.
                    File2.delete(fDir[hs] + ttName);

                    if (String2.isFileNameSafe(ttName)) {
                      EDD edd = (EDD) erddap.gridDatasetHashMap.get(ttName);
                      if (edd == null) edd = (EDD) erddap.tableDatasetHashMap.get(ttName);

                      // if hardFlag, delete cached dataset info
                      //  (whether the dataset is live or not)
                      if (hs == 0) {
                        if (edd != null) {
                          StringArray childDatasetIDs = edd.childDatasetIDs();
                          for (int cd = 0; cd < childDatasetIDs.size(); cd++)
                            EDD.deleteCachedDatasetInfo(
                                childDatasetIDs.get(cd)); // delete the children's info
                        }
                        LoadDatasets.tryToUnload(
                            erddap, ttName, new StringArray(), true); // needToUpdateLucene
                        EDD.deleteCachedDatasetInfo(ttName); // the important difference

                      } else if (hs == 2) {

                        // if badFilesFlag, delete badFiles.nc info
                        //  (whether the dataset is live or not)
                        if (edd != null) {
                          StringArray childDatasetIDs = edd.childDatasetIDs();
                          for (int cd = 0; cd < childDatasetIDs.size(); cd++)
                            EDD.deleteBadFilesFile(
                                childDatasetIDs.get(cd)); // delete the children's badFiles.nc
                        }
                        LoadDatasets.tryToUnload(
                            erddap, ttName, new StringArray(), true); // needToUpdateLucene
                        EDD.deleteBadFilesFile(ttName); // the important difference
                      }

                      if (ttName.matches(EDStatic.datasetsRegex)) {
                        // name is okay

                        // if edd exists, setCreationTimeTo0 so loadDatasets will reload it
                        // if edd doesn't exist (and is valid datasetID), loadDatasets will try to
                        // load it
                        // if datasetID isn't defined in datasets.xml, loadDatasets will ignore it
                        if (edd != null) edd.setCreationTimeTo0();

                        // prepare ttName for regex: encode -, .  ('_' doesn't need encoding)
                        ttName = String2.replaceAll(ttName, "-", "\\x2D");
                        ttName = String2.replaceAll(ttName, ".", "\\.");
                        tFlagNames.set(i, ttName);

                      } else {
                        // file name doesn't match EDStatic.datasetsRegex, so ignore it
                        String2.log(
                            "RunloadDatasets is deleting "
                                + ttName
                                + " from "
                                + fDirName[hs]
                                + " directory because it doesn't match EDStatic.datasetsRegex.");
                        tFlagNames.remove(i);
                      }

                    } else {
                      // file name may be valid for this OS, but tName isn't a valid erddap
                      // datasetID
                      // otherwise the file will stay in dir forever
                      String2.log(
                          "RunloadDatasets is deleting "
                              + ttName
                              + " from "
                              + fDirName[hs]
                              + " directory because it isn't a valid datasetID.");
                      tFlagNames.remove(i);
                    }
                  }
                }

                // if files, run loadDatasets with just those datasetIDs
                if (tFlagNames.size() > 0) {
                  String tRegex = "(" + String2.toSVString(tFlagNames.toArray(), "|", false) + ")";
                  String2.log(
                      "\n*** RunLoadDatasets is starting a new "
                          + fDirName[hs]
                          + " LoadDatasets thread at "
                          + Calendar2.getCurrentISODateTimeStringLocalTZ());
                  // ...StartTimeMillis = System.currentTimeMillis();
                  loadDatasets = new LoadDatasets(erddap, tRegex, null, false);
                  // make a lower priority
                  // [commented out: why lower priority?  It may be causing infrequent problems with
                  // a dataset not available in a CWBrowser
                  // -2 since on some OS's, adjacent priority levels map to same internal level.
                  // loadDatasets.setPriority(Math.max(Thread.MIN_PRIORITY,
                  // Thread.currentThread().getPriority() - 2));
                  EDStatic.runningThreads.put("loadDatasets", loadDatasets);
                  loadDatasets.start(); // starts the thread and calls run()
                }
              }
            }
          }

          // sleep for 5 seconds
          // (make this adjustable? only penalty to smaller is frequency of checking flag dir)
          try {
            // don't use Math2.sleep since want to detect InterruptedException
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            break whileNotInterrupted;
          }
        }

      } catch (Throwable t) {
        if (Thread.currentThread().isInterrupted() || t instanceof InterruptedException) {
          break whileNotInterrupted;
        }
        try {
          String subject =
              "RunLoadDatasets(flag loop) error at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ();
          String content = MustBe.throwableToString(t);
          String2.log(subject + ": " + content);
          EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
        } catch (Throwable t2) {
          if (Thread.currentThread().isInterrupted() || t2 instanceof InterruptedException)
            break whileNotInterrupted;
        }
      }

      // isInterrupted?
      if (isInterrupted()) break whileNotInterrupted;

      // ********* need to call loadDatasets.stop???
      try {
        // first: if loadDatasets finished normally above, wait 10 seconds for
        // !loadDatasets.isAlive()
        while (loadDatasets != null
            && loadDatasets.isAlive()
            && (System.currentTimeMillis() - lastMajorLoadDatasetsStartTimeMillis
                < EDStatic.loadDatasetsMaxMillis * 3 / 4)) {

          // isInterrupted?
          if (isInterrupted()) break whileNotInterrupted;

          // sleep for 10 seconds
          try {
            // don't use Math2.sleep since want to detect InterruptedException
            Thread.sleep(10000);
          } catch (InterruptedException e) {
            break whileNotInterrupted;
          }
        }
        if (loadDatasets != null && !loadDatasets.isAlive()) {
          loadDatasets = null;
          EDStatic.runningThreads.remove("loadDatasets");
          if (lastMajorLoadDatasetsStopTimeMillis < lastMajorLoadDatasetsStartTimeMillis) {
            lastMajorLoadDatasetsStopTimeMillis = System.currentTimeMillis();
            EDStatic.lastMajorLoadDatasetsStopTimeMillis = lastMajorLoadDatasetsStopTimeMillis;
          }
        }

        // is loadDatasets still running???  (interrupt it, and stop if longer than
        // loadDatasetsMaxMillis!!!)
        if (loadDatasets != null) {
          // loadDatasets is stalled; interrupt it

          String tError =
              "RunLoadDatasets is interrupting a long running LoadDatasets thread ("
                  + Calendar2.elapsedTimeString(
                      System.currentTimeMillis() - lastMajorLoadDatasetsStartTimeMillis)
                  + " > 3/4 "
                  + Calendar2.elapsedTimeString(EDStatic.loadDatasetsMaxMillis)
                  + ") at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ();
          String2.log("\n*** " + tError);

          // wait the final 1/4 loadDatasetsMax for !loadDatasets.isAlive
          if (EDStatic.stopThread(
              loadDatasets,
              Math2.narrowToInt(EDStatic.loadDatasetsMaxMillis / 1000 / 4))) { // seconds
            tError =
                "RunLoadDatasets stopped a stalled LoadDatasets thread ("
                    + Calendar2.elapsedTimeString(
                        System.currentTimeMillis() - lastMajorLoadDatasetsStartTimeMillis)
                    + " > "
                    + Calendar2.elapsedTimeString(EDStatic.loadDatasetsMaxMillis)
                    + ") at "
                    + Calendar2.getCurrentISODateTimeStringLocalTZ();
            String2.log("\n*** " + tError);
            EDStatic.email(EDStatic.emailEverythingToCsv, "RunLoadDatasets Stalled", tError);
          }

          loadDatasets = null;
          EDStatic.runningThreads.remove("loadDatasets");
          if (lastMajorLoadDatasetsStopTimeMillis < lastMajorLoadDatasetsStartTimeMillis) {
            lastMajorLoadDatasetsStopTimeMillis = System.currentTimeMillis();
            EDStatic.lastMajorLoadDatasetsStopTimeMillis = lastMajorLoadDatasetsStopTimeMillis;
          }
        }

      } catch (Throwable t) {
        if (Thread.currentThread().isInterrupted() || t instanceof InterruptedException) {
          break whileNotInterrupted;
        }
        try {
          String subject =
              "RunLoadDatasets(loadDatasets.stop loop) error at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ();
          String content = MustBe.throwableToString(t);
          String2.log(subject + ": " + content);
          EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
        } catch (Throwable t2) {
          if (Thread.currentThread().isInterrupted() || t2 instanceof InterruptedException)
            break whileNotInterrupted;
        }
      }
    } // end of   while (!isInterrupted())

    // erddap is shutting down; deal with interruption
    String2.log(
        "\n*** RunLoadDatasets noticed that it was interrupted at "
            + Calendar2.getCurrentISODateTimeStringLocalTZ());
    if (loadDatasets != null && loadDatasets.isAlive()) {
      EDStatic.stopThread(loadDatasets, 60);
      loadDatasets = null;
      EDStatic.runningThreads.remove("loadDatasets");
      if (lastMajorLoadDatasetsStopTimeMillis < lastMajorLoadDatasetsStartTimeMillis) {
        lastMajorLoadDatasetsStopTimeMillis = System.currentTimeMillis();
        EDStatic.lastMajorLoadDatasetsStopTimeMillis = lastMajorLoadDatasetsStopTimeMillis;
      }
    }
    erddap = null;
  }
}
