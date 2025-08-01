package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".parquet",
    fileTypeName = ".parquetWMeta",
    infoUrl = "https://parquet.apache.org/",
    versionAdded = "2.25.0",
    contentType = "application/parquet")
public class ParquetWMetaFiles extends ParquetFiles {
  @Override
  public void writeTableToFileFormat(DapRequestInfo requestInfo, TableWriter tableWriter)
      throws Throwable {
    if (tableWriter instanceof TableWriterAllWithMetadata) {
      saveAsParquet(
          requestInfo.language(),
          requestInfo.outputStream(),
          (TableWriterAllWithMetadata) tableWriter,
          requestInfo.edd().datasetID(),
          true);
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsParquet(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        true,
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_PARQUET_WITH_META, language);
  }
}
