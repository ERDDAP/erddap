package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".csv",
    fileTypeName = ".csv",
    infoUrl = "https://en.wikipedia.org/wiki/Comma-separated_values",
    versionAdded = "1.0.0",
    contentType = "text/csv")
public class CsvFiles extends SeparatedValue {

  public CsvFiles() {
    super(new SeparatedValuesConfig(",", true, true, '2'));
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_csvAr[language];
  }
}
