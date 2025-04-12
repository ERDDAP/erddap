package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterEsriCsv;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".csv",
    fileTypeName = ".esriCsv",
    infoUrl = "https://support.esri.com/technical-article/000012745",
    versionAdded = "1.0.0",
    availableGrid = false)
public class EsriCsvFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterEsriCsv(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream());
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelpTable_esriCsvAr[language];
  }
}
