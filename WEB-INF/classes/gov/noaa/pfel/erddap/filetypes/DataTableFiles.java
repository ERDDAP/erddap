package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterDataTable;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".json",
    fileTypeName = ".dataTable",
    infoUrl = "https://developers.google.com/chart/interactive/docs/reference#dataparam",
    versionAdded = "1.84.0",
    availableGrid = false)
public class DataTableFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterDataTable(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream(),
        true);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_dataTableAr[language];
  }
}
