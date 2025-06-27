package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".tsv",
    fileTypeName = ".tsv",
    infoUrl = "https://jkorpela.fi/TSV.html",
    versionAdded = "1.0.0",
    contentType = "text/tab-separated-values")
public class TsvFiles extends SeparatedValue {

  public TsvFiles() {
    super(new SeparatedValuesConfig("\t", false, true, '2'));
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_tsvAr[language];
  }
}
