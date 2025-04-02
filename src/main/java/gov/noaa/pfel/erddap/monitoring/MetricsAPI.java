 package gov.noaa.pfel.erddap.monitoring;

  /**
   * Generic API interface for ERDDAP metrics collection
   * Provides abstraction from specific metrics implementation
   */
  public interface MetricsAPI {

      /**
       * Initialize the metrics system
       *
       * @param enabled Whether metrics collection is enabled
       */
      void initialize(boolean enabled);

      /**
       * Record the duration of a request
       *
       * @param requestType Type of request (e.g., "grid", "table", "metadata")
       * @param datasetId Dataset identifier
       * @param fileType Response file type
       * @param statusCode HTTP status code
       * @param durationSeconds Request duration in seconds
       */
      void recordRequestDuration(String requestType, String datasetId, String fileType,
                                int statusCode, double durationSeconds);

      /**
       * Update dataset count metrics
       *
       * @param gridCount Number of grid datasets
       * @param tableCount Number of table datasets
       * @param failedCount Number of datasets that failed to load
       */
      void updateDatasetCounts(int gridCount, int tableCount, int failedCount);

      /**
       * Record a dataset access
       *
       * @param datasetId Dataset identifier
       * @param accessType Type of access (e.g., "view", "download", "subscribe")
       */
      void recordDatasetAccess(String datasetId, String accessType);

      /**
       * Record cache operations
       *
       * @param cacheType Type of cache (e.g., "dataset", "file", "image")
       * @param action Action performed ("hit", "miss", "eviction")
       */
      void recordCacheOperation(String cacheType, String action);

      /**
       * Record system resource usage
       *
       * @param memoryUsed Memory used in bytes
       * @param threadCount Active thread count
       */
      void recordResourceUsage(long memoryUsed, int threadCount);

      /**
       * Record an error or exceptional condition
       *
       * @param errorType Type of error
       * @param severity Severity level
       */
      void recordError(String errorType, String severity);
  }
