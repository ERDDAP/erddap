package gov.noaa.pfel.erddap.jte;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.sun.management.UnixOperatingSystemMXBean;
import gov.noaa.pfel.coastwatch.sgt.GSHHS;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.lang.management.ManagementFactory;

public class Status {
  private static Erddap erddap;

  static {
    try {
      erddap = new Erddap();
    } catch (Throwable e) {
      throw new RuntimeException("Failed to initialize Erddap", e);
    }
  }

  public static String getCurrentTimeLocal() {
    return Calendar2.getCurrentISODateTimeStringLocalTZ();
  }

  public static String getStartupTimeLocal() {
    return EDStatic.startupLocalDateTime;
  }

  public static String getTimeSinceLastMajorLoadDatasetsStarted() {
    return Calendar2.elapsedTimeString(
        System.currentTimeMillis() - EDStatic.lastMajorLoadDatasetsStartTimeMillis);
  }

  public static long getLoadDatasetsElapsedTimeSeconds() {
    return (EDStatic.lastMajorLoadDatasetsStopTimeMillis
            - EDStatic.lastMajorLoadDatasetsStartTimeMillis)
        / 1000;
  }

  public static boolean isLoadDatasetsRunning() {
    // load datasets stop time < start time means the current load hasn't finished
    return getLoadDatasetsElapsedTimeSeconds() < 0;
  }

  public static boolean isDatasetLoading() {
    return EDStatic.cldNTry != 0 && EDStatic.cldDatasetID != null && EDStatic.cldStartMillis != 0;
  }

  public static boolean isDatasetLoadingMajor() {
    return EDStatic.cldMajor;
  }

  public static int getDatasetLoadingNumber() {
    return EDStatic.cldNTry;
  }

  public static String getDatasetLoadingID() {
    return EDStatic.cldDatasetID;
  }

  public static String getDatasetLoadingTime() {
    return Calendar2.elapsedTimeString(
        Math2.longToDoubleNaN(System.currentTimeMillis() - EDStatic.cldStartMillis));
  }

  /*
  public static ConcurrentHashMap<String, EDDGrid> getDatasetHashMap() {
      return erddap.gridDatasetHashMap;
  }
  public static ConcurrentHashMap<String, EDDTable> getTableDatasetHashMap() {
      return erddap.tableDatasetHashMap;
  }
  */
  public static int getGridDatasetCount() {
    return erddap.gridDatasetHashMap == null ? 0 : erddap.gridDatasetHashMap.size();
  }

  public static int getTableDatasetCount() {
    return erddap.tableDatasetHashMap == null ? 0 : erddap.tableDatasetHashMap.size();
  }

  public static boolean getShowLoadErrorsOnStatusPage() {
    return EDStatic.config.showLoadErrorsOnStatusPage;
  }

  public static String getdatasetsfailedtoload() {
    return EDStatic.datasetsThatFailedToLoad;
  }

  public static String getFailedDatasetErros() {
    return EDStatic.failedDatasetsWithErrors;
  }

  public static String getErrorsDuringMajorReload() {
    return EDStatic.errorsDuringMajorReload;
  }

  public static int getIpAddressQueue() {
    return EDStatic.ipAddressQueue.size();
  }

  public static String getFailureTimesDistributionLoadDatasets() {
    return String2.getBriefTimeDistributionStatistics(
        EDStatic.failureTimesDistributionLoadDatasets);
  }

  public static String getFailureTimesDistribution24() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.failureTimesDistribution24);
  }

  public static String getFailureTimesDistributionTotal() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.failureTimesDistributionTotal);
  }

  public static String getResponseTimesDistributionLoadDatasets() {
    return String2.getBriefTimeDistributionStatistics(
        EDStatic.responseTimesDistributionLoadDatasets);
  }

  public static String getResponseTimesDistribution24() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.responseTimesDistribution24);
  }

  public static String getResponseTimesDistributionTotal() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.responseTimesDistributionTotal);
  }

  public static int getLastFinishedTaskPlusOne() {
    return EDStatic.lastFinishedTask.get() + 1;
  }

  public static int getTaskListSize() {
    return EDStatic.taskList.size();
  }

  public static String getTaskThreadStatusMessage() {
    TaskThread thread = EDStatic.getTaskThread(); // use the getter
    long tElapsedTime = thread == null ? -1 : thread.elapsedTime();
    return tElapsedTime < 0
        ? "Currently, no task is running.\n"
        : "The current task has been running for "
            + Calendar2.elapsedTimeString(tElapsedTime)
            + ".\n";
  }

  public static String getTaskThreadFailedDistribution24() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.taskThreadFailedDistribution24);
  }

  public static String getTaskThreadFailedDistributionTotal() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.taskThreadFailedDistributionTotal);
  }

  public static String getTaskThreadSucceededDistribution24() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.taskThreadSucceededDistribution24);
  }

  public static String getTaskThreadSucceededDistributionTotal() {
    return String2.getBriefTimeDistributionStatistics(
        EDStatic.taskThreadSucceededDistributionTotal);
  }

  public static boolean isEmailSystemActive() {
    return EDStatic.config.emailIsActive;
  }

  public static int getLastFinishedEmailPlusOne() {
    return EDStatic.lastFinishedEmail.get() + 1;
  }

  public static int getEmailListSize() {
    return EDStatic.emailList.size();
  }

  public static String getEmailThreadStatusMessage() {
    long tElapsedTime = EDStatic.getEmailThread();
    return tElapsedTime < 0
        ? "Currently, the thread is sleeping.\n"
        : "The current email session has been running for "
            + Calendar2.elapsedTimeString(tElapsedTime)
            + ".\n";
  }

  public static String getEmailThreadFailedDistribution24() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.emailThreadFailedDistribution24);
  }

  public static String getEmailThreadSucceededDistribution24() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.emailThreadSucceededDistribution24);
  }

  public static int getLastFinishedTouchPlusOne() {
    return EDStatic.lastFinishedTouch.get() + 1;
  }

  public static int getTouchListSize() {
    return EDStatic.touchList.size();
  }

  public static String getTouchThreadStatusMessage() {
    TouchThread touchThread = EDStatic.getTouchThread();
    long tElapsedTime = touchThread == null ? -1 : touchThread.elapsedTime();
    return tElapsedTime < 0
        ? "Currently, the thread is sleeping.\n"
        : "The current touch has been running for "
            + Calendar2.elapsedTimeString(tElapsedTime)
            + ".\n";
  }

  public static String getTouchThreadFailedDistribution24() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.touchThreadFailedDistribution24);
  }

  public static String getTouchThreadSucceededDistribution24() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.touchThreadSucceededDistribution24);
  }

  public static float getTotalCpuLoad() {
    var osBean = ManagementFactory.getOperatingSystemMXBean();
    if (osBean instanceof UnixOperatingSystemMXBean uBean) {
      return Math2.doubleToFloatNaN(uBean.getCpuLoad());
    }
    return Float.NaN;
  }

  public static float getProcessCpuLoad() {
    var osBean = ManagementFactory.getOperatingSystemMXBean();
    if (osBean instanceof UnixOperatingSystemMXBean uBean) {
      return Math2.doubleToFloatNaN(uBean.getProcessCpuLoad());
    }
    return Float.NaN;
  }

  public static long getTotalMemoryMB() {
    var osBean = ManagementFactory.getOperatingSystemMXBean();
    if (osBean instanceof UnixOperatingSystemMXBean uBean) {
      return uBean.getTotalMemorySize() / Math2.BytesPerMB;
    }
    return -1;
  }

  public static long getFreeMemoryMB() {
    var osBean = ManagementFactory.getOperatingSystemMXBean();
    if (osBean instanceof UnixOperatingSystemMXBean uBean) {
      return uBean.getFreeMemorySize() / Math2.BytesPerMB;
    }
    return -1;
  }

  public static long getTotalSwapMB() {
    var osBean = ManagementFactory.getOperatingSystemMXBean();
    if (osBean instanceof UnixOperatingSystemMXBean uBean) {
      return uBean.getTotalSwapSpaceSize() / Math2.BytesPerMB;
    }
    return -1;
  }

  public static long getFreeSwapMB() {
    var osBean = ManagementFactory.getOperatingSystemMXBean();
    if (osBean instanceof UnixOperatingSystemMXBean uBean) {
      return uBean.getFreeSwapSpaceSize() / Math2.BytesPerMB;
    }
    return -1;
  }

  public static int getActiveRequestsCount() {
    return EDStatic.activeRequests.size();
  }

  public static String getStackTraceHeader() {
    String traces = MustBe.allStackTraces(true, true);
    int po = traces.indexOf('\n');
    return (po > 0) ? traces.substring(0, po + 1) : "";
  }

  public static String getGcAndRequestShedStatus() {
    return Math2.gcCallCount
        + " gc calls, "
        + EDStatic.requestsShed
        + " requests shed, and "
        + EDStatic.dangerousMemoryEmails
        + " dangerousMemoryEmails since last major LoadDatasets";
  }

  public static String getMemoryStatus() {
    return Math2.memoryString() + " " + Math2.xmxMemoryString();
  }

  public static String getLoadDatasetsTimeSeriesSB() {
    return EDStatic.majorLoadDatasetsTimeSeriesSB.toString();
  }

  public static boolean getLoadDatasetsTimeSeriesSBIsEmpty() {
    return EDStatic.majorLoadDatasetsTimeSeriesSB.length() > 0;
  }

  public static String getTimeDistributionStatistics() {
    return String2.getBriefTimeDistributionStatistics(EDStatic.majorLoadDatasetsDistribution24);
  }

  public static String getMajorLoadDatasetsTimeDistributionTotal() {
    return String2.getTimeDistributionStatistics(EDStatic.majorLoadDatasetsDistributionTotal);
  }

  public static String getMinorLoadDatasetsTimeDistribution24() {
    return String2.getTimeDistributionStatistics(EDStatic.minorLoadDatasetsDistribution24);
  }

  public static String getMinorLoadDatasetsTimeDistributionTotal() {
    return String2.getTimeDistributionStatistics(EDStatic.minorLoadDatasetsDistributionTotal);
  }

  public static String getFailureTimeDistributionLoadDatasets() {
    return String2.getTimeDistributionStatistics(EDStatic.failureTimesDistributionLoadDatasets);
  }

  public static String getFailureTimeDistribution24() {
    return String2.getTimeDistributionStatistics(EDStatic.failureTimesDistribution24);
  }

  public static String getFailureTimeDistributionTotal() {
    return String2.getTimeDistributionStatistics(EDStatic.failureTimesDistributionTotal);
  }

  public static String getResponseTimeDistribution24() {
    return String2.getTimeDistributionStatistics(EDStatic.responseTimesDistribution24);
  }

  public static String getEmailThreadFailedDistributions24() {
    return String2.getTimeDistributionStatistics(EDStatic.emailThreadFailedDistribution24);
  }

  public static String getEmailThreadFailedDistributions() {
    return String2.getTimeDistributionStatistics(EDStatic.emailThreadFailedDistributionTotal);
  }

  public static String getEmailThreadSucceededDistributions24() {
    return String2.getTimeDistributionStatistics(EDStatic.emailThreadSucceededDistribution24);
  }

  public static String getEmailThreadSucceededDistributions() {
    return String2.getTimeDistributionStatistics(EDStatic.emailThreadSucceededDistributionTotal);
  }

  public static String getEmailThreadNEmailsDistributions24() {
    return String2.getCountDistributionStatistics(EDStatic.emailThreadNEmailsDistribution24);
  }

  public static String getEmailThreadNEmailsDistributions() {
    return String2.getCountDistributionStatistics(EDStatic.emailThreadNEmailsDistributionTotal);
  }

  // TaskThread
  public static String getTaskThreadFailedDistributions24() {
    return String2.getTimeDistributionStatistics(EDStatic.taskThreadFailedDistribution24);
  }

  public static String getTaskThreadFailedDistributions() {
    return String2.getTimeDistributionStatistics(EDStatic.taskThreadFailedDistributionTotal);
  }

  public static String getTaskThreadSucceededDistributions24() {
    return String2.getTimeDistributionStatistics(EDStatic.taskThreadSucceededDistribution24);
  }

  public static String getTaskThreadSucceededDistributions() {
    return String2.getTimeDistributionStatistics(EDStatic.taskThreadSucceededDistributionTotal);
  }

  // TouchThread
  public static String getTouchThreadFailedDistributions24() {
    return String2.getTimeDistributionStatistics(EDStatic.touchThreadFailedDistribution24);
  }

  public static String getTouchThreadFailedDistributions() {
    return String2.getTimeDistributionStatistics(EDStatic.touchThreadFailedDistributionTotal);
  }

  public static String getTouchThreadSucceededDistributions24() {
    return String2.getTimeDistributionStatistics(EDStatic.touchThreadSucceededDistribution24);
  }

  public static String getTouchThreadSucceededDistributions() {
    return String2.getTimeDistributionStatistics(EDStatic.touchThreadSucceededDistributionTotal);
  }

  // Language and System Stats
  public static String getLanguageTallyDistributions24() {
    return EDStatic.tally.toString("Language (since last daily report)", 50);
  }

  public static String getLanguageTallyDistributions() {
    return EDStatic.tally.toString("Language (since startup)", 50);
  }

  public static String getTopographyStats() {
    return SgtMap.topographyStats();
  }

  public static String getGshhsStats() {
    return GSHHS.statsString();
  }

  public static String getNationalBoundariesStats() {
    return SgtMap.nationalBoundaries.statsString();
  }

  public static String getStateBoundariesStats() {
    return SgtMap.stateBoundaries.statsString();
  }

  public static String getRiverStats() {
    return SgtMap.rivers.statsString();
  }

  public static String getImageAccelerationStatus() {
    return SgtUtil.isBufferedImageAccelerated();
  }

  public static String getCanonicalStats() {
    return String2.canonicalStatistics();
  }

  public static String getTraces() {
    String traces = MustBe.allStackTraces(true, true);
    int po = traces.indexOf('\n');
    return traces;
  }
}
