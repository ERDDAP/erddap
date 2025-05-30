package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".tsv",
    fileTypeName = ".tsv0",
    infoUrl = "https://jkorpela.fi/TSV.html",
    versionAdded = "1.48.0",
    contentType = "text/tab-separated-values")
public class Tsv0Files extends SeparatedValue {

  public Tsv0Files() {
    super(new SeparatedValuesConfig("\t", false, false, '0'));
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_tsv0Ar[language];
  }
}
