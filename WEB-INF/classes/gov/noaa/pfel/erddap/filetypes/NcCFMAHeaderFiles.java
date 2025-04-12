package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".txt",
    fileTypeName = ".ncCFMAHeader",
    infoUrl = "https://linux.die.net/man/1/ncdump",
    versionAdded = "1.0.0",
    availableGrid = false)
public class NcCFMAHeaderFiles extends NcCFFiles {
  public NcCFMAHeaderFiles() {
    super(true);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_ncCFMAHeaderAr[language];
  }
}
