package gov.noaa.pfel.erddap.jte;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.sun.management.UnixOperatingSystemMXBean;
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
    // load datasets stop time > start time means the current load hasn't finished
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
    EmailThread emailThread = EDStatic.getEmailThread();
    long tElapsedTime = emailThread == null ? -1 : emailThread.elapsedTime();
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
}
