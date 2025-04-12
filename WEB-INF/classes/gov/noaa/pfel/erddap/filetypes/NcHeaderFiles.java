package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".txt",
    fileTypeName = ".ncHeader",
    infoUrl = "https://linux.die.net/man/1/ncdump",
    versionAdded = "1.0.0")
public class NcHeaderFiles extends NcFiles {
  public NcHeaderFiles() {
    super(true);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_nc3HeaderAr[language];
  }

  @Override
  public String getGridHelpText(int language) {
    return EDStatic.messages.fileHelp_nc3HeaderAr[language];
  }
}
