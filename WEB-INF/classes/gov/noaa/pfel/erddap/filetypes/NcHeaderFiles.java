package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".txt",
    fileTypeName = ".ncHeader",
    infoUrl = "https://linux.die.net/man/1/ncdump",
    versionAdded = "1.0.0",
    contentType = "text/plain",
    addContentDispositionHeader = false)
public class NcHeaderFiles extends NcFiles {
  public NcHeaderFiles() {
    super(true);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NC3_HEADER, language);
  }

  @Override
  public String getGridHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NC3_HEADER, language);
  }
}
