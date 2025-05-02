package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.SimpleException;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.util.EDStatic;

public abstract class TableWriterFileType extends FileTypeInterface {
  protected abstract TableWriter generateTableWriter(DapRequestInfo requestInfo) throws Throwable;

  protected void writeTableToFileFormat(DapRequestInfo requestInfo, TableWriter tableWriter)
      throws Throwable {}

  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    EDDTable eddTable = requestInfo.getEDDTable();
    if (eddTable == null) {
      throw new SimpleException(
          EDStatic.bilingual(
              requestInfo.language(),
              EDStatic.messages.queryErrorAr[0] + EDStatic.messages.errorInternalAr[0],
              EDStatic.messages.queryErrorAr[requestInfo.language()]
                  + EDStatic.messages.errorInternalAr[requestInfo.language()]));
    }
    TableWriter tableWriter = generateTableWriter(requestInfo);
    if (tableWriter != null) {
      TableWriter tTableWriter =
          eddTable.encloseTableWriter(
              requestInfo.language(),
              true, // alwaysDoAll
              requestInfo.dir(),
              requestInfo.fileName(),
              tableWriter,
              requestInfo.requestUrl(),
              requestInfo.userDapQuery());
      if (eddTable.handleViaFixedOrSubsetVariables(
          requestInfo.language(),
          requestInfo.loggedInAs(),
          requestInfo.requestUrl(),
          requestInfo.userDapQuery(),
          tTableWriter)) {
      } else {
        if (tTableWriter != tableWriter)
          tTableWriter =
              eddTable.encloseTableWriter(
                  requestInfo.language(),
                  false, // alwaysDoAll
                  requestInfo.dir(),
                  requestInfo.fileName(),
                  tableWriter,
                  requestInfo.requestUrl(),
                  requestInfo.userDapQuery());
        eddTable.getDataForDapQuery(
            requestInfo.language(),
            requestInfo.loggedInAs(),
            requestInfo.requestUrl(),
            requestInfo.userDapQuery(),
            tTableWriter);
      }
      // special case: for these, tableWriter=twawm
      //  so (unlike e.g., tableWriter is a TableWriterJson), we aren't quite finished.
      //  Data is in twawm and we need to save it to file.
      writeTableToFileFormat(requestInfo, tTableWriter);
      tTableWriter.close();
      tableWriter.close();
    }
  }
}
