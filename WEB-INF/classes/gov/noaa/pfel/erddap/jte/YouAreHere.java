package gov.noaa.pfel.erddap.jte;

import gov.noaa.pfel.erddap.util.EDStatic;

public class YouAreHere {
  private final String erddapUrl;
  private final String erddapLinkTitle;
  private final String protocol;

  public YouAreHere(final String erddapUrl, final String erddapLinkTitle, final String protocol) {
    this.erddapUrl = erddapUrl;
    this.erddapLinkTitle = erddapLinkTitle;
    this.protocol = protocol;
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

  public String getProtocol() {
    return protocol;
  }
}
