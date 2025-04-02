 package gov.noaa.pfel.erddap.monitoring.config;

  /**
   * Configuration options for metrics collection
   */
  public class MetricsConfig {

      private boolean enabled;
      private String implementationType;
      private String endpoint;
      private int scrapeIntervalSeconds;
      private String[] labelsToInclude;

      /**
       * Default constructor
       */
      public MetricsConfig() {
          this.enabled = false;
          this.implementationType = "noop";
          this.endpoint = "/metrics";
          this.scrapeIntervalSeconds = 15;
          this.labelsToInclude = new String[0];
      }

      // Getters and setters

      public boolean isEnabled() {
          return enabled;
      }

      public void setEnabled(boolean enabled) {
          this.enabled = enabled;
      }

      public String getImplementationType() {
          return implementationType;
      }

      public void setImplementationType(String implementationType) {
          this.implementationType = implementationType;
      }

      public String getEndpoint() {
          return endpoint;
      }

      public void setEndpoint(String endpoint) {
          this.endpoint = endpoint;
      }

      public int getScrapeIntervalSeconds() {
          return scrapeIntervalSeconds;
      }

      public void setScrapeIntervalSeconds(int scrapeIntervalSeconds) {
          this.scrapeIntervalSeconds = scrapeIntervalSeconds;
      }

      public String[] getLabelsToInclude() {
          return labelsToInclude;
      }

      public void setLabelsToInclude(String[] labelsToInclude) {
          this.labelsToInclude = labelsToInclude;
      }
  }
