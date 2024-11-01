package gov.noaa.pfel.erddap.http;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

/** Add CORS headers to the response if EDStatic.enableCors is true. */
@WebFilter("/*")
public class CorsResponseFilter implements Filter {
  public static final String DEFAULT_ALLOW_HEADERS =
      "Accept,Authorization,Cache-Control,Content-Type,DNT,If-Modified-Since,Keep-Alive,Origin,User-Agent";

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    if (EDStatic.enableCors) {
      HttpServletRequest request = (HttpServletRequest) servletRequest;
      HttpServletResponse response = (HttpServletResponse) servletResponse;
      String requestOrigin = StringUtils.trim(request.getHeader("Origin"));
      if (requestOrigin != null && requestOrigin.equalsIgnoreCase("null")) {
        requestOrigin = null;
      }

      if (EDStatic.corsAllowOrigin == null || EDStatic.corsAllowOrigin.length == 0) {
        // If corsAllowOrigin is not set, any origin is allowed
        if (String2.isSomething(requestOrigin)) {
          response.setHeader("Access-Control-Allow-Origin", requestOrigin);
        } else {
          response.setHeader("Access-Control-Allow-Origin", "*");
        }
      } else {
        // If corsAllowedOrigin is set, make sure the request origin was provided and is in the
        // corsAllowedOrigin list
        if (String2.isSomething(requestOrigin)) {
          if (Arrays.asList(EDStatic.corsAllowOrigin).contains(requestOrigin.toLowerCase())) {
            response.setHeader("Access-Control-Allow-Origin", requestOrigin);
          } else {
            response.setHeader(
                "Access-Control-Allow-Origin", requestOrigin + ".origin-not-allowed.invalid");
          }
        } else {
          response.setHeader("Access-Control-Allow-Origin", "https://origin-not-provided.invalid");
        }
      }

      response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      response.setHeader("Access-Control-Allow-Headers", EDStatic.corsAllowHeaders);

      if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setHeader("Access-Control-Max-Age", "7200");
        return;
      }
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }
}
