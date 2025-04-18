package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterJsonl;
import gov.noaa.pfel.erddap.util.EDStatic;

public abstract class Jsonl extends TableWriterFileType {

  private boolean writeColNames;
  private boolean writeKVP;

  public Jsonl(boolean writeColNames, boolean writeKVP) {
    this.writeColNames = writeColNames;
    this.writeKVP = writeKVP;
  }

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) throws Exception {
    String jsonp = EDStatic.getJsonpFromQuery(requestInfo.language(), requestInfo.userDapQuery());
    return new TableWriterJsonl(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream(),
        writeColNames,
        writeKVP,
        jsonp);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsJsonl(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        writeColNames,
        writeKVP,
        requestInfo.getEDDGrid());
  }

  /**
   * This writes the axis or grid data to the outputStream in JSON Lines (https://jsonlines.org/)
   * KVP format. If no exception is thrown, the data was successfully written.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]. This method extracts the jsonp
   *     text to be prepended to the results (or null if none). See
   *     https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/ and
   *     https://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/ and
   *     https://www.raymondcamden.com/2014/03/12/Reprint-What-in-the-heck-is-JSONP-and-why-would-you-use-it/
   *     .
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  private void saveAsJsonl(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      boolean tWriteColNames,
      boolean tWriteKVP,
      EDDGrid grid)
      throws Throwable {

    // currently, this writes a table.
    // Perhaps better to write nDimensional array?
    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsJsonl");
    long time = System.currentTimeMillis();

    // NO: a jsonp constraint won't get this far. jsonp won't work with jsonl.
    // did query include &.jsonp= ?
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
        new TableWriterJsonl(
            language,
            grid,
            grid.getNewHistory(requestUrl, userDapQuery),
            outputStreamSource,
            tWriteColNames,
            tWriteKVP,
            jsonp);
    if (isAxisDapQuery) {
      grid.saveAsTableWriter(ada, tw);
    } else {
      grid.saveAsTableWriter(gda, tw);
      gda.close();
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsJsonl done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
