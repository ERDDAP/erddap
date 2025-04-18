package gov.noaa.pfel.erddap.util;

import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.sgt.GSHHS;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.core.metrics.StateSet;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.Unit;
import java.awt.ImageCapabilities;
import java.awt.image.BufferedImage;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Metrics {

  public enum ThreadStatus {
    success,
    fail,
  }

  public enum DatasetCategory {
    grid,
    table,
  }

  public enum LoadDatasetsType {
    major,
    minor,
  }

  public enum Cache {
    cached,
    not_cached,
  }

  public enum BoundaryRequest {
    coarse,
    success,
    tossed,
  }

  public Histogram responseDuration =
      Histogram.builder()
          .name("http_request_duration_seconds")
          .help("HTTP request service time in seconds")
          .unit(Unit.SECONDS)
          .labelNames(
              "request_type", "dataset_id", "dataset_type", "file_type", "lang_code", "status_code")
          .nativeOnly()
          .build();

  public Histogram touchThreadDuration =
      Histogram.builder()
          .name("touch_thread_duration_seconds")
          .help("Touch thread service time in seconds")
          .unit(Unit.SECONDS)
          .labelNames("success")
          .nativeOnly()
          .build();

  public Histogram taskThreadDuration =
      Histogram.builder()
          .name("task_thread_duration_seconds")
          .help("Task thread service time in seconds")
          .unit(Unit.SECONDS)
          .labelNames("success", "task_type")
          .nativeOnly()
          .build();

  public Histogram loadDatasetsDuration =
      Histogram.builder()
          .name("load_datasets_duration_seconds")
          .help("Load datasets service time in seconds")
          .unit(Unit.SECONDS)
          .labelNames("major")
          .nativeOnly()
          .build();

  public Histogram emailsCountDistribution =
      Histogram.builder()
          .name("email_count_distribution")
          .help("Number of emails sent per session")
          .nativeOnly()
          .build();

  public Histogram emailThreadDuration =
      Histogram.builder()
          .name("email_thread_duration_seconds")
          .help("Email thread service time in seconds")
          .unit(Unit.SECONDS)
          .labelNames("success")
          .nativeOnly()
          .build();

  public Gauge datasetsCount =
      Gauge.builder()
          .name("dataset_count")
          .help("Count of datasets, set at the end of loadDatasets")
          .labelNames("category")
          .build();
  public Gauge datasetsFailedCount =
      Gauge.builder()
          .name("dataset_failed_load_count")
          .help("Count of datasets that failed to load, set at the end of loadDatasets")
          .build();

  public Counter shedRequests =
      Counter.builder()
          .name("shed_requests_total")
          .help("Count of requests that have been shed")
          .build();

  public Counter dangerousMemoryEmails =
      Counter.builder()
          .name("dangerous_memory_emails_total")
          .help("Count of dangerous memory emails")
          .build();

  public Counter dangerousMemoryFailures =
      Counter.builder()
          .name("dangerous_memory_failures_total")
          .help("Count of failed requests due to memory")
          .build();

  public Counter sgtMapTopoRequest =
      Counter.builder()
          .name("topo_request_total")
          .help("Count of topo requests")
          .labelNames("cache")
          .build();

  public void initialize(boolean registerPrometheus) {
    if (registerPrometheus) {
      JvmMetrics.builder().register(); // initialize the out-of-the-box JVM metrics
      addInfoMetrics();
      PrometheusRegistry.defaultRegistry.register(loadDatasetsDuration);
      PrometheusRegistry.defaultRegistry.register(emailThreadDuration);
      PrometheusRegistry.defaultRegistry.register(taskThreadDuration);
      PrometheusRegistry.defaultRegistry.register(touchThreadDuration);
      PrometheusRegistry.defaultRegistry.register(responseDuration);
      PrometheusRegistry.defaultRegistry.register(emailsCountDistribution);
      PrometheusRegistry.defaultRegistry.register(datasetsCount);
      PrometheusRegistry.defaultRegistry.register(datasetsFailedCount);
      PrometheusRegistry.defaultRegistry.register(shedRequests);
      PrometheusRegistry.defaultRegistry.register(dangerousMemoryEmails);
      PrometheusRegistry.defaultRegistry.register(dangerousMemoryFailures);
      PrometheusRegistry.defaultRegistry.register(sgtMapTopoRequest);
      GSHHS.requestStatus.register(PrometheusRegistry.defaultRegistry);
      SgtMap.nationalBoundaries.counter.register(PrometheusRegistry.defaultRegistry);
      SgtMap.stateBoundaries.counter.register(PrometheusRegistry.defaultRegistry);
      SgtMap.rivers.counter.register(PrometheusRegistry.defaultRegistry);
    }
    datasetsCount.initLabelValues(DatasetCategory.grid.name());
    datasetsCount.initLabelValues(DatasetCategory.table.name());
  }

  private void addInfoMetrics() {
    Info info =
        Info.builder()
            .name("ERDDAP_build_info")
            .help("Information about the ERDDAP build")
            .labelNames("version", "version_full", "deployment_info")
            .register();
    String doubleVersion =
        EDStatic.erddapVersion.getMajor() + "." + EDStatic.erddapVersion.getMinor();
    info.setLabelValues(
        doubleVersion,
        EDStatic.erddapVersion.getVersion(),
        EDStatic.config.deploymentInfo != null ? EDStatic.config.deploymentInfo : "");

    Info graphicsInfo =
        Info.builder()
            .name("bufferedImage")
            .help("Information about the graphics state")
            .labelNames("isAccelerated")
            .register();
    try {
      BufferedImage bi = SgtUtil.getBufferedImage(10, 10);
      ImageCapabilities imCap = bi.getCapabilities(null);
      graphicsInfo.setLabelValues(
          imCap == null ? "unknown" : imCap.isAccelerated() ? "true" : "false");

    } catch (Throwable t) {
      graphicsInfo.setLabelValues("unknown");
    }

    addFeatureFlagMetrics();
  }

  public void addFeatureFlagMetrics() {
    List<String> featureFlags = new ArrayList<>();
    Field[] fields = EDStatic.config.getClass().getFields();
    for (Field field : fields) {
      if (field.isAnnotationPresent(FeatureFlag.class)) {
        featureFlags.add(field.getName());
      }
    }
    String[] flags = featureFlags.toArray(new String[featureFlags.size()]);

    StateSet stateSet =
        StateSet.builder().name("feature_flags").help("Feature flags").states(flags).register();

    for (Field field : fields) {
      if (field.isAnnotationPresent(FeatureFlag.class)) {
        try {
          if (field.getBoolean(EDStatic.config)) {
            stateSet.setTrue(field.getName());
          } else {
            stateSet.setFalse(field.getName());
          }
        } catch (Exception e) {
          String2.log("Error making feature_flags metric with feature: " + field.getName());
        }
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface FeatureFlag {}
}
