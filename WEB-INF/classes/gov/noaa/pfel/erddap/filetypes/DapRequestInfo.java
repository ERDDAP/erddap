package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record DapRequestInfo(
    int language,
    EDD edd,
    String newHistory,
    OutputStreamSource outputStream,
    String requestUrl,
    String userDapQuery,
    String loggedInAs,
    String endOfRequest,
    String fileName,
    String dir,
    String fileTypeName,
    String ipAddress,
    HttpServletRequest request,
    HttpServletResponse response) {
  public EDDTable getEDDTable() {
    return (EDDTable) edd;
  }

  public EDDGrid getEDDGrid() {
    return (EDDGrid) edd;
  }
}
