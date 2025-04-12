package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".png",
    fileTypeName = ".png",
    infoUrl = "http://www.libpng.org/pub/png/",
    versionAdded = "1.0.0")
public class PngFiles extends ImageFiles {

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_pngAr[language];
  }
}
