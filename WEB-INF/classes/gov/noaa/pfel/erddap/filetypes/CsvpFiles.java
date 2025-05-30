package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".csv",
    fileTypeName = ".csvp",
    infoUrl = "https://en.wikipedia.org/wiki/Comma-separated_values",
    versionAdded = "1.24.0",
    contentType = "text/csv")
public class CsvpFiles extends SeparatedValue {

  public CsvpFiles() {
    super(new SeparatedValuesConfig(",", true, true, '('));
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_csvpAr[language];
  }
}
