package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".png",
    fileTypeName = ".smallPng",
    infoUrl = "http://www.libpng.org/pub/png/",
    versionAdded = "1.0.0")
public class SmallPngFiles extends ImageFiles {

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_smallPngAr[language];
  }
}
