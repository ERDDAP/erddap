package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterNccsv;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".csv",
    fileTypeName = ".nccsv",
    infoUrl = "https://erddap.github.io/docs/user/nccsv-1.20",
    versionAdded = "1.76.0",
    contentType = "text/csv")
public class NccsvFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterNccsv(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream());
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsNccsv(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NCCSV, language);
  }

  /**
   * This writes the requested axis or grid data to the outputStream in NCCSV
   * (https://erddap.github.io/docs/user/nccsv-1.20) format. If no exception is thrown, the data was
   * successfully written.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160].
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  private void saveAsNccsv(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid grid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsNccsv");
    long time = System.currentTimeMillis();

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
        new TableWriterNccsv(
            language,
            grid,
            grid.getNewHistory(language, requestUrl, userDapQuery),
            outputStreamSource);
    if (isAxisDapQuery) {
      grid.saveAsTableWriter(ada, tw);
    } else {
      grid.saveAsTableWriter(gda, tw);
      gda.close();
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsNccsv done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
