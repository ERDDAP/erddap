package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.OutputStream;

@FileTypeClass(
    fileTypeExtension = ".xml",
    fileTypeName = ".fgdc",
    infoUrl =
        "https://www.fgdc.gov/standards/projects/FGDC-standards-projects/metadata/base-metadata/index_html",
    versionAdded = "1.0.0",
    contentType = "application/xml")
public class FgdcFiles extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    EDD edd = requestInfo.edd();
    if (edd.accessibleViaFGDC().length() == 0) {
      try (OutputStream out = requestInfo.outputStream().outputStream(File2.UTF_8)) {
        if (!File2.copy(edd.datasetDir() + edd.datasetID() + EDD.fgdcSuffix + ".xml", out))
          throw new SimpleException(String2.ERROR + " while transmitting file.");
      }
      // downloads of e.g., erddap2.css don't work right if not closed. (just if gzip'd?)
    } else {
      throw new SimpleException(
          EDStatic.simpleBilingual(requestInfo.language(), Message.QUERY_ERROR)
              + edd.accessibleViaFGDC());
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    writeTableToStream(requestInfo);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_FGDC, language);
  }
}
