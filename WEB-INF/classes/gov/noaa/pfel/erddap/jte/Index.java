package gov.noaa.pfel.erddap.jte;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/** Data object for the the JTE template for the home page/index.html. */
public class Index {
  public int language;
  public String erddapUrl;
  public YouAreHere youAreHere;
  public String endOfRequest;
  public String loggedInAs;
  public HttpServletRequest request;

  // Main HTML blocks (pre-rendered in Java to handle legacy helper methods)
  public String shortDescriptionHtml;
  public String searchFormHtml;
  public String categorizeOptionsHtml;

  // Dynamic Links and Titles
  public String viewAllDatasetsUrl;
  public String viewAllDatasetsTitle;
  public String advancedSearchLink;

  public String searchMultipleDescription;
  public List<ConverterLink> converterLinks;
  public String wafMessage; // Combined formatted message for FGDC/ISO
  public String subscriptionsDescription;

  // Helper record for the converter table rows
  public static class ConverterLink {
    public String url;
    public String text;
    public String description;

    public ConverterLink(String url, String text, String description) {
      this.url = url;
      this.text = text;
      this.description = description;
    }
  }
}
