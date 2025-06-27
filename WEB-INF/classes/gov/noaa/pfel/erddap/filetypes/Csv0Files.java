package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".csv",
    fileTypeName = ".csv0",
    infoUrl = "https://en.wikipedia.org/wiki/Comma-separated_values",
    versionAdded = "1.48.0",
    contentType = "text/csv")
public class Csv0Files extends SeparatedValue {

  public Csv0Files() {
    super(new SeparatedValuesConfig(",", true, false, '0'));
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_csv0Ar[language];
  }
}
