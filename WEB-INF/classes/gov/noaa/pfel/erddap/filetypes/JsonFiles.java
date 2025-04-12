package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterJson;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".json",
    fileTypeName = ".json",
    infoUrl = "https://www.json.org/",
    versionAdded = "1.0.0")
public class JsonFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) throws Exception {
    return new TableWriterJson(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream(),
        EDStatic.getJsonpFromQuery(requestInfo.language(), requestInfo.userDapQuery()),
        true);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsJson(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_jsonAr[language];
  }

  /**
   * This writes the requested axis or grid data to the outputStream in JSON (https://www.json.org/)
   * format. If no exception is thrown, the data was successfully written.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]. This method extracts the jsonp
   *     text to be prepended to the results (or null if none). See
   *     https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/ and
   *     https://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/ and
   *     http://www.insideria.com/2009/03/what-in-the-heck-is-jsonp-and.html .
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  private void saveAsJson(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid grid)
      throws Throwable {

    // currently, this writes a table.
    // Perhaps better to write nDimensional array?
    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsJson");
    long time = System.currentTimeMillis();

    String jsonp = EDStatic.getJsonpFromQuery(language, userDapQuery);

    // get dataAccessor first, in case of error when parsing query
    boolean isAxisDapQuery = grid.isAxisDapQuery(userDapQuery);
    AxisDataAccessor ada = null;
    GridDataAccessor gda = null;
    if (isAxisDapQuery) ada = new AxisDataAccessor(language, grid, requestUrl, userDapQuery);
    else
      gda =
          new GridDataAccessor(
              language, grid, requestUrl, userDapQuery, true, false); // rowMajor, convertToNaN

    // write the data to the tableWriter
    TableWriter tw =
        new TableWriterJson(
            language,
            grid,
            grid.getNewHistory(requestUrl, userDapQuery),
            outputStreamSource,
            jsonp,
            true); // writeUnits
    if (isAxisDapQuery) {
      grid.saveAsTableWriter(ada, tw);
    } else {
      grid.saveAsTableWriter(gda, tw);
      gda.close();
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsJson done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
