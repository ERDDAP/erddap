package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import gov.noaa.pfel.erddap.dataset.EDD.ISO_VERSION;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedWriter;

@FileTypeClass(
    fileTypeExtension = ".xml",
    fileTypeName = ".iso19139_2007",
    infoUrl = "https://en.wikipedia.org/wiki/Geospatial_metadata",
    versionAdded = "2.29.0",
    contentType = "application/xml")
public class Iso191392007Files extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {

    try (BufferedWriter writer =
        File2.getBufferedWriterUtf8(requestInfo.outputStream().outputStream(File2.UTF_8))) {
      requestInfo
          .getEDDTable()
          .writeISO19115(requestInfo.language(), writer, ISO_VERSION.ISO19139_2007);
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    try (BufferedWriter writer =
        File2.getBufferedWriterUtf8(requestInfo.outputStream().outputStream(File2.UTF_8))) {
      requestInfo
          .getEDDGrid()
          .writeISO19115(requestInfo.language(), writer, ISO_VERSION.ISO19139_2007);
    }
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_ISO191392007, language);
  }
}
