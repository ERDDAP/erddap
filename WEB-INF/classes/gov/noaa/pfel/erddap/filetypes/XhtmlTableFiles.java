package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterHtmlTable;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".xhtml",
    fileTypeName = ".xhtml",
    infoUrl = "https://www.w3schools.com/html/html_tables.asp",
    versionAdded = "1.0.0",
    contentType = "application/xhtml+xml",
    addContentDispositionHeader = false)
// Old code had a workaround for IE that required setting contentType to "text/html".
// IE is now << 1% of worldwide browser usage, and I don't think the workaround is worth
// maintaining at this point.
public class XhtmlTableFiles extends HtmlTableFiles {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterHtmlTable(
        requestInfo.request(),
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.loggedInAs(),
        requestInfo.endOfRequest(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        true,
        requestInfo.fileName(),
        true,
        "",
        "",
        true,
        true,
        -1,
        EDStatic.imageDirUrl(
                requestInfo.request(), requestInfo.loggedInAs(), requestInfo.language())
            + EDStatic.messages.questionMarkImageFile);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsHtmlTable(
        requestInfo.request(),
        requestInfo.language(),
        requestInfo.loggedInAs(),
        requestInfo.requestUrl(),
        requestInfo.endOfRequest(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.fileName(),
        true,
        "",
        "",
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_xhtmlAr[language];
  }
}
