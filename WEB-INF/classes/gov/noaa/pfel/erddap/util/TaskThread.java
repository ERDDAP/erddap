/*
 * TaskThread Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.EDD;
import io.prometheus.metrics.model.snapshots.Unit;

/**
 * This does a series of tasks.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-05-20
 */
public class TaskThread extends Thread {

  // PLEASE: make parameters from objects that have nice .toString(),
  // e.g., StringArray instead of String[].

  /**
   * If taskOA[0].equals(TASK_MAKE_A_DATAFILE), then make taskOA[1]=edd, taskOA[2]=query,
   * taskOA[3]=fileDir, taskOA[4]=fileName, taskOA[5]=fileType
   */
  public static final Integer TASK_MAKE_A_DATAFILE = 0;

  /** If taskOA[0].equals(TASK_SET_FLAG), then make taskOA[1]=datasetID */
  public static final Integer TASK_SET_FLAG = 1;

  /**
   * If taskOA[0].equals(TASK_DAP_TO_NC), then make taskOA[1]=dapUrl, taskOA[2]=StringArray(vars),
   * taskOA[3]=projection, taskOA[4]=fullFileName, taskOA[5]=jplMode (Boolean.TRUE|FALSE),
   * taskOA[6]=lastModified (Long)
   */
  public static final Integer TASK_DAP_TO_NC = 2;

  /**
   * If taskOA[0].equals(TASK_ALL_DAP_TO_NC), then make taskOA[1]=dapUrl, taskOA[2]=fullFileName,
   * taskOA[3]=lastModified (Long)
   */
  public static final Integer TASK_ALL_DAP_TO_NC = 3;

  /**
   * If taskOA[0].equals(TASK_DOWNLOAD), then make taskOA[1]=remoteUrl, taskOA[2]=fullFileName,
   * taskOA[3]=lastModified (Long) if MAX_VALUE, will be ignored
   */
  public static final Integer TASK_DOWNLOAD = 4;

  /** If taskOA[0].equals(TASK_CLEAR_CACHE) */
  public static final Integer TASK_CLEAR_CACHE = 5;

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  // set while running
  private long lastStartTime;

  /** The constructor. TaskThread uses task variables in EDStatic. */
  public TaskThread(int tNextTask) {
    EDStatic.nextTask.set(tNextTask);
    EDStatic.lastFinishedTask.set(tNextTask - 1);
    setName("TaskThread");
  }

  /** This returns elapsed time for the current task (or -1 if no task is running). */
  public long elapsedTime() {
    return System.currentTimeMillis() - lastStartTime;
  }

  /** This does any pending tasks, then exits. */
  @Override
  public void run() {
    while (EDStatic.nextTask.get() < EDStatic.taskList.size()) {
      String taskSummary = null;
      try {
        // check isInterrupted
        if (isInterrupted()) {
          String2.log(
              "%%% TaskThread was interrupted at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ());
          return;
        }

        // start to do the task
        // do these things quickly to keep internal consistency
        lastStartTime = System.currentTimeMillis();
        String2.log(
            "\n%%% TaskThread started task #"
                + EDStatic.nextTask.get()
                + " of "
                + (EDStatic.taskList.size() - 1)
                + " at "
                + Calendar2.getCurrentISODateTimeStringLocalTZ());
        EDStatic.nextTask.incrementAndGet();

        // get the task settings
        Object taskOA[] = EDStatic.taskList.get(EDStatic.nextTask.get() - 1);
        if (taskOA == null) {
          String2.log("task #" + (EDStatic.nextTask.get() - 1) + " was null.");
          continue;
        }
        Integer taskType = (Integer) taskOA[0];

        // TASK_MAKE_A_DATAFILE
        if (taskType.equals(TASK_MAKE_A_DATAFILE)) {

          EDD edd = (EDD) taskOA[1];
          String query = (String) taskOA[2];
          String fileDir = (String) taskOA[3];
          String fileName = (String) taskOA[4];
          String fileType = (String) taskOA[5];
          taskSummary =
              "  TASK_MAKE_A_DATAFILE datasetID="
                  + edd.datasetID()
                  + "\n"
                  + "    query="
                  + query
                  + "\n"
                  + "    file="
                  + fileDir
                  + fileName
                  + fileType;
          String2.log(taskSummary);

          edd.reuseOrMakeFileForDapQuery(
              0,
              null,
              EDStatic.loggedInAsSuperuser, // language=English!, request, loggedInAs
              query,
              fileDir,
              fileName,
              fileType);

          // TASK_SET_FLAG
        } else if (taskType.equals(TASK_SET_FLAG)) {
          String datasetID = (String) taskOA[1];
          taskSummary = "  TASK_SET_FLAG datasetID=" + datasetID;
          String2.log(taskSummary);

          // It doesn't really matter if lastFinishedTask is set below before construction starts.
          // All of the files are copied, so all will be detected.
          EDD.requestReloadASAP(datasetID);

          // TASK_DAP_TO_NC
        } else if (taskType.equals(TASK_DAP_TO_NC)) {

          String dapUrl = (String) taskOA[1];
          StringArray vars = (StringArray) taskOA[2];
          String projection = (String) taskOA[3];
          String fullFileName = (String) taskOA[4];
          Boolean jplMode = (Boolean) taskOA[5];
          Long lastModified = (Long) taskOA[6];
          taskSummary =
              "  TASK_DAP_TO_NC \n"
                  + "    dapUrl="
                  + dapUrl
                  + "    vars="
                  + vars
                  + " projection="
                  + projection
                  + "    file="
                  + fullFileName
                  + " lastMod="
                  + Calendar2.safeEpochSecondsToIsoStringTZ(lastModified / 1000.0, "NaN");
          String2.log(taskSummary);

          OpendapHelper.dapToNc(dapUrl, vars.toArray(), projection, fullFileName, jplMode);
          File2.setLastModified(fullFileName, lastModified);

          // TASK_ALL_DAP_TO_NC
        } else if (taskType.equals(TASK_ALL_DAP_TO_NC)) {

          String dapUrl = (String) taskOA[1];
          String fullFileName = (String) taskOA[2];
          Long lastModified = (Long) taskOA[3];
          taskSummary =
              "  TASK_ALL_DAP_TO_NC \n"
                  + "    dapUrl="
                  + dapUrl
                  + "    file="
                  + fullFileName
                  + " lastMod="
                  + Calendar2.safeEpochSecondsToIsoStringTZ(lastModified / 1000.0, "NaN");
          String2.log(taskSummary);

          OpendapHelper.allDapToNc(dapUrl, fullFileName);
          File2.setLastModified(fullFileName, lastModified);

          // TASK_DOWNLOAD
        } else if (taskType.equals(TASK_DOWNLOAD)) {

          String sourceUrl = (String) taskOA[1];
          String fullFileName = (String) taskOA[2];
          long lastMod = (Long) taskOA[3];
          taskSummary =
              "  TASK_DOWNLOAD sourceUrl="
                  + sourceUrl
                  + "\n"
                  + "    fullName="
                  + fullFileName
                  + "\n"
                  + "    lastMod="
                  + lastMod;
          String2.log(taskSummary);

          SSR.downloadFile(
              "TASK_DOWNLOAD",
              sourceUrl,
              fullFileName,
              true); // tryToUseCompression, throws Exception
          if (lastMod < Long.MAX_VALUE) File2.setLastModified(fullFileName, lastMod);

        } else if (taskType.equals(TASK_CLEAR_CACHE)) {
          EDStatic.clearCache("TASK_CLEAR_CACHE", false);

          // UNKNOWN taskType
        } else {
          String2.log(
              "TaskThread error: Unknown taskType="
                  + taskType
                  + " for task #"
                  + (EDStatic.nextTask.get() - 1)
                  + ".");
        }

        // task finished successfully
        long tElapsedTime = elapsedTime();
        String2.log(
            "%%% TaskThread task #"
                + (EDStatic.nextTask.get() - 1)
                + " of "
                + (EDStatic.taskList.size() - 1)
                + " succeeded.  elapsedTime = "
                + Calendar2.elapsedTimeString(tElapsedTime));
        String2.distributeTime(tElapsedTime, EDStatic.taskThreadSucceededDistribution24);
        String2.distributeTime(tElapsedTime, EDStatic.taskThreadSucceededDistributionTotal);
        EDStatic.metrics
            .taskThreadDuration
            .labelValues(Metrics.ThreadStatus.success.name(), "" + taskType)
            .observe(Unit.millisToSeconds(tElapsedTime));

      } catch (Throwable t) {
        long tElapsedTime = elapsedTime();
        String2.distributeTime(tElapsedTime, EDStatic.taskThreadFailedDistribution24);
        String2.distributeTime(tElapsedTime, EDStatic.taskThreadFailedDistributionTotal);
        Object taskOA[] = EDStatic.taskList.get(EDStatic.nextTask.get() - 1);
        Integer taskType = (Integer) taskOA[0];
        EDStatic.metrics
            .taskThreadDuration
            .labelValues(Metrics.ThreadStatus.fail.name(), "" + taskType)
            .observe(Unit.millisToSeconds(tElapsedTime));
        String subject =
            "TaskThread error: task #"
                + (EDStatic.nextTask.get() - 1)
                + " failed after "
                + Calendar2.elapsedTimeString(tElapsedTime);
        String content = taskSummary + "\n" + MustBe.throwableToString(t);
        String2.log("%%% " + subject + "\n" + content);
        EDStatic.email(EDStatic.config.emailEverythingToCsv, subject, content);
      }

      // whether succeeded or failed
      synchronized (EDStatic.taskList) {
        EDStatic.lastFinishedTask.set(EDStatic.nextTask.get() - 1);
        EDStatic.taskList.set(EDStatic.nextTask.get() - 1, null); // throw away the task info (gc)
      }
    }
  }
}
