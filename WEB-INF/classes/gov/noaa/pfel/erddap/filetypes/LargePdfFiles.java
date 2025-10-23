package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".pdf",
    fileTypeName = ".largePdf",
    infoUrl = "https://www.adobe.com/acrobat/about-adobe-pdf.html",
    versionAdded = "1.0.0",
    isImage = true,
    contentType = "application/pdf")
public class LargePdfFiles extends ImageFiles {

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_LARGE_PDF, language);
  }
}
