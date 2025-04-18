package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".png",
    fileTypeName = ".largePng",
    infoUrl = "http://www.libpng.org/pub/png/",
    versionAdded = "2.19.0",
    isImage = true)
public class LargePngFiles extends ImageFiles {

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_largePngAr[language];
  }
}
