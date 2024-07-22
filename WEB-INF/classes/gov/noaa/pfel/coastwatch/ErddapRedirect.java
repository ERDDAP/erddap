/*
 * ErddapRedirect Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;

public class ErddapRedirect extends HttpServlet {

  /**
   * This responds to a "post" request from the user by extending HttpServlet's doPost and passing
   * the request to doGet.
   *
   * @param request
   * @param response
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doGet(request, response);
  }

  /**
   * This just redirects all erddap/* requests to the new erddap location (a little crudely, so user
   * is forced to notice).
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("text/html");
    PrintWriter out = res.getWriter();
    res.setStatus(res.SC_MOVED_PERMANENTLY);
    res.setHeader("Location", "https://coastwatch.pfeg.noaa.gov/erddap/");
  }
}
