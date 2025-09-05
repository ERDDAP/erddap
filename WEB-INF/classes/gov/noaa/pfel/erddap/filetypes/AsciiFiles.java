package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterDodsAscii;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.Writer;

@FileTypeClass(
    fileTypeExtension = ".asc",
    fileTypeName = ".asc",
    infoUrl = "https://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#ASCII_Service",
    versionAdded = "1.0.0",
    contentType = "text/plain",
    contentDescription = "dods-data",
    addContentDispositionHeader = false)
public class AsciiFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterDodsAscii(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream(),
        EDDTable.SEQUENCE_NAME);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsAsc(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_ASC, language);
  }

  /**
   * This gets the data for the userDapQuery and writes the grid data to the outputStream in the
   * DODS ASCII data format, which is not defined in OPeNDAP 2.0, but which is very close to
   * saveAsDODS below. This mimics
   * https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day.asc?MHchla[1477][0][2080:2:2082][4940]
   * .
   *
   * @param language the index of the selected language
   * @param requestUrl
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  private void saveAsAsc(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid eddGrid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsAsc");
    long time = System.currentTimeMillis();

    // handle axis request
    if (eddGrid.isAxisDapQuery(userDapQuery)) {
      // get AxisDataAccessor first, in case of error when parsing query
      AxisDataAccessor ada = new AxisDataAccessor(language, eddGrid, requestUrl, userDapQuery);
      int nRAV = ada.nRequestedAxisVariables();

      // write the dds    //OPeNDAP 2.0, 7.2.3
      // OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as
      // well go for compatible common 8bit
      try (Writer writer =
          File2.getBufferedWriter88591(outputStreamSource.outputStream(File2.ISO_8859_1))) {
        eddGrid.writeDDS(language, requestUrl, userDapQuery, writer);

        // write the connector  //OPeNDAP 2.0, 7.2.3
        writer.write(
            "---------------------------------------------"
                + OpendapHelper.EOL
                + "Data:"
                + OpendapHelper.EOL); // see EOL definition for comments

        // write the data  //OPeNDAP 2.0, 7.3.2.4
        for (int av = 0; av < nRAV; av++) {
          writer.write(
              ada.axisVariables(av).destinationName()
                  + "["
                  + ada.axisValues(av).size()
                  + "]"
                  + OpendapHelper.EOL);
          writer.write(ada.axisValues(av).toString());
          writer.write(OpendapHelper.EOL);
        }

        writer.flush(); // essential
      }

      // diagnostic
      if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsAsc axis done.\n");
      return;
    }

    Writer writer = null;
    // get full gridDataAccessor first, in case of error when parsing query
    try (GridDataAccessor gridDataAccessor =
        new GridDataAccessor(
            language, eddGrid, requestUrl, userDapQuery, true, false)) { // rowMajor, convertToNaN
      EDV tDataVariables[] = gridDataAccessor.dataVariables();
      String arrayQuery = EDDGrid.buildDapArrayQuery(gridDataAccessor.constraints());
      // get partial gridDataAccessor, to test for size error
      try (GridDataAccessor partialGda =
          new GridDataAccessor(
              language,
              eddGrid,
              requestUrl,
              tDataVariables[0].destinationName() + arrayQuery,
              true,
              false)) { // rowMajor, convertToNaN

        long tSize = partialGda.totalIndex().size();
        Math2.ensureArraySizeOkay(tSize, "OPeNDAP limit");
      }
      boolean entireDataset = userDapQuery.trim().length() == 0;
      // write the dds    //OPeNDAP 2.0, 7.2.3
      writer =
          File2.getBufferedWriter88591(
              outputStreamSource.outputStream(
                  File2.ISO_8859_1)); // OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as
      // well go for compatible common 8bit
      eddGrid.writeDDS(language, requestUrl, userDapQuery, writer);

      // write the connector  //OPeNDAP 2.0, 7.2.3
      writer.write(
          "---------------------------------------------"
              + OpendapHelper.EOL); // see EOL definition for comments

      // write the axis variables
      int nAxisVariables = eddGrid.axisVariables().length;
      if (entireDataset) {
        // send the axis data
        int tShape[] = gridDataAccessor.totalIndex().shape();
        for (int av = 0; av < nAxisVariables; av++) {
          writer.write(
              eddGrid.axisVariables()[av].destinationName()
                  + "["
                  + tShape[av]
                  + "]"
                  + OpendapHelper.EOL); // see EOL definition for comments
          writer.write(gridDataAccessor.axisValues(av).toString());
          writer.write(OpendapHelper.EOL); // see EOL definition for comments
        }
        writer.write(OpendapHelper.EOL); // see EOL definition for comments
      }

      // write the data  //OPeNDAP 2.0, 7.3.2.4
      // write elements of the array, in dds order
      for (EDV tDataVariable : tDataVariables) {
        String dvDestName = tDataVariable.destinationName();
        try (GridDataAccessor partialGda =
            new GridDataAccessor(
                language,
                eddGrid,
                requestUrl,
                dvDestName + arrayQuery,
                true,
                false)) { // rowMajor, convertToNaN
          int shape[] = partialGda.totalIndex().shape();
          int current[] = partialGda.totalIndex().getCurrent();

          // identify the array
          writer.write(dvDestName + "." + dvDestName);
          int nAv = eddGrid.axisVariables().length;
          for (int av = 0; av < nAv; av++) writer.write("[" + shape[av] + "]");

          // send the array data
          while (partialGda.increment()) {
            // if last dimension's value is 0, start a new row
            if (current[nAv - 1] == 0) {
              writer.write(OpendapHelper.EOL); // see EOL definition for comments
              for (int av = 0; av < nAv - 1; av++) writer.write("[" + current[av] + "]");
            }
            writer.write(", " + partialGda.getDataValueAsString(0));
          }

          // send the axis data
          for (int av = 0; av < nAxisVariables; av++) {
            writer.write(
                OpendapHelper.EOL
                    + OpendapHelper.EOL
                    + dvDestName
                    + "."
                    + eddGrid.axisVariables()[av].destinationName()
                    + "["
                    + shape[av]
                    + "]"
                    + OpendapHelper.EOL); // see EOL definition for comments
            writer.write(partialGda.axisValues(av).toString());
          }
          writer.write(OpendapHelper.EOL); // see EOL definition for comments
        }
      }

      writer.flush(); // essential
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (Exception e) {
          String2.log("Error closing writer, log and continue: " + e.getMessage());
        }
      }
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log("  EDDGrid.saveAsAsc done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
