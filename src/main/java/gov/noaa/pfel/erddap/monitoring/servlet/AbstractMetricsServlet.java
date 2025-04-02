 package gov.noaa.pfel.erddap.monitoring.servlet;

  import gov.noaa.pfel.erddap.monitoring.MetricsAPI;
  import gov.noaa.pfel.erddap.monitoring.MetricsFactory;
  import gov.noaa.pfel.erddap.monitoring.config.MetricsConfig;

  import javax.servlet.ServletException;
  import javax.servlet.http.HttpServlet;
  import javax.servlet.http.HttpServletRequest;
  import javax.servlet.http.HttpServletResponse;
  import java.io.IOException;

  /**
   * Abstract base class for metrics endpoint servlets
   */
  public abstract class AbstractMetricsServlet extends HttpServlet {

      protected MetricsAPI metricsAPI;
      protected MetricsConfig config;

      @Override
      public void init() throws ServletException {
          // Load configuration (in a real implementation, this would come from a file or other source)
          config = new MetricsConfig();
          config.setEnabled(true);
          config.setImplementationType("prometheus");

          // Initialize metrics API
          metricsAPI = MetricsFactory.getInstance(config.getImplementationType());
          metricsAPI.initialize(config.isEnabled());
      }

      @Override
      protected void doGet(HttpServletRequest req, HttpServletResponse resp)
              throws ServletException, IOException {
          if (!config.isEnabled()) {
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
              return;
          }

          resp.setContentType(getContentType());
          writeMetrics(resp);
      }

      /**
       * Get the content type for the metrics response
       *
       * @return The appropriate content type
       */
      protected abstract String getContentType();

      /**
       * Write metrics data to the response
       *
       * @param response The HTTP response to write to
       * @throws IOException If an I/O error occurs
       */
      protected abstract void writeMetrics(HttpServletResponse response) throws IOException;
  }

