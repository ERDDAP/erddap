package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.Writer;

@FileTypeClass(
    fileTypeExtension = ".jsonld",
    fileTypeName = ".croissant",
    infoUrl = "https://docs.mlcommons.org/croissant/docs/croissant-spec.html",
    versionAdded = "2.28.0",
    contentType = "application/ld+json")
public class CroissantSchemaFiles extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {

    try (Writer writer =
        File2.getBufferedWriter88591(requestInfo.outputStream().outputStream(File2.ISO_8859_1))) {
      Erddap.theSchemaDotOrgDatasetJson(
          requestInfo.request(),
          requestInfo.loggedInAs(),
          requestInfo.language(),
          writer,
          requestInfo.edd(),
          true /* useCroissant */);
      writer.flush();
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    writeTableToStream(requestInfo);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_CROISSANT, language);
  }
}
