 package gov.noaa.pfel.erddap.monitoring;

  /**
   * Constants used by the metrics system
   */
  public final class MetricsConstants {

      // Metric names
      public static final String REQUEST_DURATION = "http_request_duration_seconds";
      public static final String DATASET_COUNT = "dataset_count";
      public static final String DATASET_FAILED_COUNT = "dataset_failed_load_count";
      public static final String DATASET_ACCESS = "dataset_access_total";
      public static final String CACHE_OPERATIONS = "cache_operations_total";
      public static final String MEMORY_USAGE = "memory_usage_bytes";
      public static final String ERROR_COUNT = "error_count_total";

      // Label names
      public static final String LABEL_DATASET_ID = "dataset_id";
      public static final String LABEL_REQUEST_TYPE = "request_type";
      public static final String LABEL_FILE_TYPE = "file_type";
      public static final String LABEL_STATUS_CODE = "status_code";
      public static final String LABEL_CATEGORY = "category";
      public static final String LABEL_ACCESS_TYPE = "access_type";
      public static final String LABEL_CACHE_TYPE = "cache_type";
      public static final String LABEL_ACTION = "action";
      public static final String LABEL_ERROR_TYPE = "error_type";
      public static final String LABEL_SEVERITY = "severity";

      // Access types
      public static final String ACCESS_VIEW = "view";
      public static final String ACCESS_DOWNLOAD = "download";
      public static final String ACCESS_SUBSCRIBE = "subscribe";
      public static final String ACCESS_SEARCH = "search";

      // Cache actions
      public static final String CACHE_HIT = "hit";
      public static final String CACHE_MISS = "miss";
      public static final String CACHE_EVICTION = "eviction";

      // Private constructor to prevent instantiation
      private MetricsConstants() {}
  }

