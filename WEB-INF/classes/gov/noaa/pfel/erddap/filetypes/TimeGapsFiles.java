package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.OutputStream;
import java.io.Writer;

@FileTypeClass(
    fileTypeExtension = ".asc",
    fileTypeName = ".timeGaps",
    infoUrl = "https://coastwatch.pfeg.noaa.gov/erddap/griddap/documentation.html#timeGaps",
    versionAdded = "1.82.0",
    availableTable = false,
    contentType = "text/plain",
    contentDescription = "time_gap_information",
    addContentDispositionHeader = false)
public class TimeGapsFiles extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    String ts = requestInfo.getEDDGrid().findTimeGaps();
    OutputStream out = requestInfo.outputStream().outputStream(File2.UTF_8);
    try (Writer writer = File2.getBufferedWriterUtf8(out)) {
      writer.write(ts);
      writer.flush(); // essential
    }
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_timeGapsAr[language];
  }
}
