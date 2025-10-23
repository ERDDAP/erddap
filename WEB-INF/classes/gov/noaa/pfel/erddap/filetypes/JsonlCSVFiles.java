package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".jsonl",
    fileTypeName = ".jsonlCSV",
    infoUrl = "https://jsonlines.org/",
    versionAdded = "1.82.0",
    contentType = "application/x-jsonlines")
public class JsonlCSVFiles extends Jsonl {

  public JsonlCSVFiles() {
    super(false, false);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_JSONL_CSV, language);
  }
}
