package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".tsv",
    fileTypeName = ".tsvp",
    infoUrl = "https://jkorpela.fi/TSV.html",
    versionAdded = "1.24.0",
    contentType = "text/tab-separated-values")
public class TsvpFiles extends SeparatedValue {

  public TsvpFiles() {
    super(new SeparatedValuesConfig("\t", false, true, '('));
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_tsvpAr[language];
  }
}
