package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".jsonl",
    fileTypeName = ".jsonlCSV1",
    infoUrl = "https://jsonlines.org/",
    versionAdded = "1.84.0",
    contentType = "application/x-jsonlines")
public class JsonlCSV1Files extends Jsonl {

  public JsonlCSV1Files() {
    super(true, false);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_JSONL_CSV1, language);
  }
}
