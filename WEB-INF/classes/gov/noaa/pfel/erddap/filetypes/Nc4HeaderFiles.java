package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

// The annotation is commented out because this is not ready for use.
// @FileTypeClass(
//     fileTypeExtension = ".txt",
//     fileTypeName = ".nc4Header",
//     infoUrl = "https://linux.die.net/man/1/ncdump",
//     versionAdded = "1.0.0")
public class Nc4HeaderFiles extends Nc4Files {
  public Nc4HeaderFiles() {
    super(true);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_nc4HeaderAr[language];
  }

  @Override
  public String getGridHelpText(int language) {
    return EDStatic.messages.fileHelp_nc4HeaderAr[language];
  }
}
