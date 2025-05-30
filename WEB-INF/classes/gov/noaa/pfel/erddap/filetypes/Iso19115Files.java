package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.OutputStream;

@FileTypeClass(
    fileTypeExtension = ".xml",
    fileTypeName = ".iso19115",
    infoUrl = "https://en.wikipedia.org/wiki/Geospatial_metadata",
    versionAdded = "1.0.0",
    contentType = "application/xml")
public class Iso19115Files extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    EDD edd = requestInfo.edd();
    if (edd.accessibleViaISO19115().length() == 0) {
      try (OutputStream out = requestInfo.outputStream().outputStream(File2.UTF_8)) {
        if (!File2.copy(edd.datasetDir() + edd.datasetID() + EDD.iso19115Suffix + ".xml", out))
          throw new SimpleException(String2.ERROR + " while transmitting file.");
      }
      // downloads of e.g., erddap2.css don't work right if not closed. (just if gzip'd?)
    } else {
      throw new SimpleException(
          EDStatic.simpleBilingual(requestInfo.language(), EDStatic.messages.queryErrorAr)
              + edd.accessibleViaISO19115());
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    writeTableToStream(requestInfo);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_iso19115Ar[language];
  }
}
