package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".pdf",
    fileTypeName = ".smallPdf",
    infoUrl = "https://www.adobe.com/acrobat/about-adobe-pdf.html",
    versionAdded = "1.0.0",
    isImage = true,
    contentType = "application/pdf")
public class SmallPdfFiles extends ImageFiles {

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_smallPdfAr[language];
  }
}
