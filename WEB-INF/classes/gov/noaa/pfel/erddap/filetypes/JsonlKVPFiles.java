package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".jsonl",
    fileTypeName = ".jsonlKVP",
    infoUrl = "https://jsonlines.org/",
    versionAdded = "1.82.0",
    contentType = "application/x-jsonlines")
public class JsonlKVPFiles extends Jsonl {

  public JsonlKVPFiles() {
    super(false, true);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_jsonlKVPAr[language];
  }
}
