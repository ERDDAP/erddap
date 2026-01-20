package gov.noaa.pfel.erddap.jte;

import gov.noaa.pfel.erddap.util.EDStatic;
import java.util.List;

public class YouAreHere {
  private final String erddapUrl;
  private final String erddapLinkTitle;
  private final List<String> breadcrumbs;
  private final List<String> breadcrumbLinks;

  public YouAreHere(
      final String erddapUrl,
      final String erddapLinkTitle,
      final List<String> breadcrumbs,
      final List<String> breadcrumbLinks) {
    this.erddapUrl = erddapUrl;
    this.erddapLinkTitle = erddapLinkTitle;
    this.breadcrumbs = breadcrumbs;
    this.breadcrumbLinks = breadcrumbLinks;
  }

  public String getErddapUrl() {
    return erddapUrl;
  }

  public String getErddapLinkTitle() {
    return erddapLinkTitle;
  }

  public String getProgramName() {
    return EDStatic.ProgramName;
  }

  public List<String> getBreadcrumbs() {
    return breadcrumbs;
  }

  public List<String> getBreadcrumbLinks() {
    return breadcrumbLinks;
  }
}
