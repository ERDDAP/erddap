package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".txt",
    fileTypeName = ".ncCFHeader",
    infoUrl = "https://linux.die.net/man/1/ncdump",
    versionAdded = "1.44.0",
    availableGrid = false,
    contentType = "text/plain",
    addContentDispositionHeader = false)
public class NcCFHeaderFiles extends NcCFFiles {
  public NcCFHeaderFiles() {
    super(true);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_ncCFHeaderAr[language];
  }
}
