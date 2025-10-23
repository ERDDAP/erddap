package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.Writer;

@FileTypeClass(
    fileTypeExtension = ".dds",
    fileTypeName = ".dds",
    infoUrl =
        "https://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Dataset_Descriptor_Structure",
    versionAdded = "1.0.0",
    contentType = "text/plain",
    contentDescription = "dods-dds",
    addContentDispositionHeader = false)
public class DdsFiles extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    Table table =
        requestInfo
            .getEDDTable()
            .makeEmptyDestinationTable(
                requestInfo.language(),
                requestInfo.requestUrl(),
                requestInfo.userDapQuery(),
                false);

    // DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common
    // 8bit
    table.saveAsDDS(
        requestInfo.outputStream().outputStream(File2.ISO_8859_1), EDDTable.SEQUENCE_NAME);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {

    // OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might
    // as well go for compatible common 8bit
    try (Writer writer =
        File2.getBufferedWriter88591(requestInfo.outputStream().outputStream(File2.ISO_8859_1))) {
      requestInfo
          .getEDDGrid()
          .writeDDS(
              requestInfo.language(), requestInfo.requestUrl(), requestInfo.userDapQuery(), writer);
    }
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_DDS, language);
  }
}
