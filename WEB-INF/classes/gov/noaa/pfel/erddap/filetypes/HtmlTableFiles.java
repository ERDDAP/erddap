package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterHtmlTable;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".html",
    fileTypeName = ".htmlTable",
    infoUrl = "https://www.w3schools.com/html/html_tables.asp",
    versionAdded = "1.0.0")
public class HtmlTableFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterHtmlTable(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.loggedInAs(),
        requestInfo.endOfRequest(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        true,
        requestInfo.fileName(),
        false,
        "",
        "",
        true,
        true,
        -1,
        EDStatic.imageDirUrl(requestInfo.loggedInAs(), requestInfo.language())
            + EDStatic.messages.questionMarkImageFile);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsHtmlTable(
        requestInfo.language(),
        requestInfo.loggedInAs(),
        requestInfo.requestUrl(),
        requestInfo.endOfRequest(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.fileName(),
        false,
        "",
        "",
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_htmlTableAr[language];
  }

  /**
   * This gets the data for the userDapQuery and writes the data to the outputStream as an html or
   * xhtml table. See TableWriterHtml for details.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). for a axis data, e.g., time[40:45], or for a grid data, e.g.,
   *     ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @param fileName (no extension) used for the document title
   * @param xhtmlMode if true, the table is stored as an XHTML table. If false, it is stored as an
   *     HTML table.
   * @param preTableHtml is html or xhtml text to be inserted at the start of the body of the
   *     document, before the table tag (or "" if none).
   * @param postTableHtml is html or xhtml text to be inserted at the end of the body of the
   *     document, after the table tag (or "" if none).
   * @throws Throwable if trouble.
   */
  protected void saveAsHtmlTable(
      int language,
      String loggedInAs,
      String requestUrl,
      String endOfRequest,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      String fileName,
      boolean xhtmlMode,
      String preTableHtml,
      String postTableHtml,
      EDDGrid eddGrid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsHtmlTable");
    long time = System.currentTimeMillis();

    // get dataAccessor first, in case of error when parsing query
    boolean isAxisDapQuery = eddGrid.isAxisDapQuery(userDapQuery);
    AxisDataAccessor ada = null;
    GridDataAccessor gda = null;
    if (isAxisDapQuery) ada = new AxisDataAccessor(language, eddGrid, requestUrl, userDapQuery);
    else
      gda =
          new GridDataAccessor(
              language,
              eddGrid,
              requestUrl,
              userDapQuery,
              true,
              true); // rowMajor, convertToNaN  (better to do it here)

    // write the data to the tableWriter
    TableWriter tw =
        new TableWriterHtmlTable(
            language,
            eddGrid,
            eddGrid.getNewHistory(language, requestUrl, userDapQuery),
            loggedInAs,
            endOfRequest,
            userDapQuery,
            outputStreamSource,
            true,
            fileName,
            xhtmlMode,
            preTableHtml,
            postTableHtml,
            true,
            true,
            -1, // tencodeAsHTML, tWriteUnits
            EDStatic.imageDirUrl(loggedInAs, language) + EDStatic.messages.questionMarkImageFile);
    if (isAxisDapQuery) {
      eddGrid.saveAsTableWriter(ada, tw);
    } else {
      eddGrid.saveAsTableWriter(gda, tw);
      gda.close();
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsHtmlTable done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
