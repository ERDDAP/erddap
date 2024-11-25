package gov.noaa.pfel.erddap.http;

import gov.noaa.pfel.erddap.util.EDStatic;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Add CORS headers to the response if EDStatic.enableCors is true. */
@WebFilter("/*")
public class CorsResponseFilter implements Filter {
  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    if (EDStatic.enableCors) {
      HttpServletResponse response = (HttpServletResponse) servletResponse;
      response.setHeader("Access-Control-Allow-Origin", "*");
      response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }
}
