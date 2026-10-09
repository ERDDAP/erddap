package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".json",
    fileTypeName = ".ncoJsonHeader",
    infoUrl = "https://nco.sourceforge.net/nco.html#json",
    versionAdded = "2.31",
    contentType = "application/json",
    addContentDispositionHeader = false)
public class NcoJsonHeaderFiles extends NcoJsonFiles {

  @Override
  public void writeTableToFileFormat(DapRequestInfo requestInfo, TableWriter tableWriter)
      throws Throwable {
    if (tableWriter instanceof TableWriterAllWithMetadata twalwm) {
      String jsonp = EDStatic.getJsonpFromQuery(requestInfo.language(), requestInfo.userDapQuery());
      saveTableAsNcoJson(
          requestInfo.outputStream(),
          new NcoJsonTableWriterTableMetadataProvider(twalwm),
          null /* don't write data */,
          jsonp);
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    String jsonp = EDStatic.getJsonpFromQuery(requestInfo.language(), requestInfo.userDapQuery());

    AxisDataAccessor axisDataAccessor = null;
    GridDataAccessor gridDataAccessor = null;
    if (requestInfo.getEDDGrid().isAxisDapQuery(requestInfo.userDapQuery())) {
      // get axisDataAccessor first, in case of error when parsing query
      axisDataAccessor =
          new AxisDataAccessor(
              requestInfo.language(),
              requestInfo.getEDDGrid(),
              requestInfo.requestUrl(),
              requestInfo.userDapQuery());
    } else {
      gridDataAccessor =
          new GridDataAccessor(
              requestInfo.language(),
              requestInfo.getEDDGrid(),
              requestInfo.requestUrl(),
              requestInfo.userDapQuery(),
              true,
              false);
    }

    saveGridAsNcoJson(
        requestInfo.outputStream(),
        requestInfo.getEDDGrid(),
        axisDataAccessor,
        gridDataAccessor,
        jsonp,
        false);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NCO_JSON_HEADER, language);
  }
}
