package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".txt",
    fileTypeName = ".ncCFMAHeader",
    infoUrl = "https://linux.die.net/man/1/ncdump",
    versionAdded = "1.44.0",
    availableGrid = false,
    contentType = "text/plain",
    addContentDispositionHeader = false)
public class NcCFMAHeaderFiles extends NcCFFiles {
  public NcCFMAHeaderFiles() {
    super(true);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_ncCFMAHeaderAr[language];
  }
}
