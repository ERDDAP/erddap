package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".png",
    fileTypeName = ".transparentPng",
    infoUrl = "http://www.libpng.org/pub/png/",
    versionAdded = "2.17.0",
    isImage = true)
public class TransparentPngFiles extends ImageFiles {

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_transparentPngAr[language];
  }
}
