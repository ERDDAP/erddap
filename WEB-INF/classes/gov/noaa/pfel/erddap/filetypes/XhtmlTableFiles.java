package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterHtmlTable;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".xhtml",
    fileTypeName = ".xhtml",
    infoUrl = "https://www.w3schools.com/html/html_tables.asp",
    versionAdded = "1.0.0")
public class XhtmlTableFiles extends HtmlTableFiles {

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
        true,
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
