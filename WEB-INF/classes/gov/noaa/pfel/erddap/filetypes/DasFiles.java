package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.Writer;

@FileTypeClass(
    fileTypeExtension = ".das",
    fileTypeName = ".das",
    infoUrl =
        "https://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Dataset_Attribute_Structure",
    versionAdded = "1.0.0",
    contentType = "text/csv",
    contentDescription = "dods-das",
    addContentDispositionHeader = false)
public class DasFiles extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    // .das is always the same regardless of the userDapQuery.
    // (That's what THREDDS does -- DAP 2.0 7.2.1 is vague.
    // THREDDs doesn't even object if userDapQuery is invalid.)
    Table table =
        requestInfo
            .getEDDTable()
            .makeEmptyDestinationTable(
                requestInfo.language(),
                requestInfo.requestUrl(),
                "",
                true); // as if userDapQuery was for everything

    // DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common
    // 8bit
    table.saveAsDAS(
        requestInfo.outputStream().outputStream(File2.ISO_8859_1), EDDTable.SEQUENCE_NAME);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsDAS");
    long time = System.currentTimeMillis();

    // get the modified outputStream
    // OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as
    // well go for compatible common 8bit
    try (Writer writer =
        File2.getBufferedWriter88591(requestInfo.outputStream().outputStream(File2.ISO_8859_1))) {
      // write the DAS
      requestInfo
          .getEDDGrid()
          .writeDAS(File2.forceExtension(requestInfo.requestUrl(), ".das"), "", writer, false);
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log("  EDDGrid.saveAsDAS done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_dasAr[language];
  }
}
