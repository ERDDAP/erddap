package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterDods;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.Writer;

@FileTypeClass(
    fileTypeExtension = ".dods",
    fileTypeName = ".dods",
    infoUrl = "https://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Data_Transmission",
    versionAdded = "1.0.0",
    contentType = "application/octet-stream",
    contentDescription = "dods-data",
    addContentDispositionHeader = false)
public class DodsFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterDods(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream(),
        EDDTable.SEQUENCE_NAME);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsDODS(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_dodsAr[language];
  }

  /**
   * This gets the data for the userDapQuery and writes the grid data to the outputStream in the
   * DODS DataDDS format (OPeNDAP 2.0, 7.2.3).
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  private void saveAsDODS(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid eddGrid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsDODS");
    long time = System.currentTimeMillis();

    // handle axisDapQuery
    if (eddGrid.isAxisDapQuery(userDapQuery)) {
      // get axisDataAccessor first, in case of error when parsing query
      AxisDataAccessor ada = new AxisDataAccessor(language, eddGrid, requestUrl, userDapQuery);
      int nRAV = ada.nRequestedAxisVariables();

      // write the dds    //OPeNDAP 2.0, 7.2.3
      OutputStream outputStream = outputStreamSource.outputStream(File2.ISO_8859_1);
      Writer writer =
          File2.getBufferedWriter88591(
              outputStream); // OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go
      // for compatible common 8bit
      try {
        eddGrid.writeDDS(language, requestUrl, userDapQuery, writer); // writer is flushed

        // write the connector  //OPeNDAP 2.0, 7.2.3
        // see EOL definition for comments
        writer.write(OpendapHelper.EOL + "Data:" + OpendapHelper.EOL);
        writer.flush(); // essential
        // don't close the writer. Leave it hanging. dos.close below closes the outputStream.

        // write the data  //OPeNDAP 2.0, 7.3.2.4
        // write elements of the array, in dds order
        try (DataOutputStream dos = new DataOutputStream(outputStream)) {
          for (int av = 0; av < nRAV; av++) ada.axisValues(av).externalizeForDODS(dos);
          dos.flush(); // essential
        } finally {
          writer = null;
        }
      } finally {
        if (writer != null)
          try {
            writer.close();
          } catch (Exception e) {
          }
      }

      // diagnostic
      if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsDODS axis done.\n");
      return;
    }

    Writer writer = null;
    // get gridDataAccessor first, in case of error when parsing query
    try (GridDataAccessor gridDataAccessor =
        new GridDataAccessor(
            language, eddGrid, requestUrl, userDapQuery, true, false)) { // rowMajor, convertToNaN
      String arrayQuery = EDDGrid.buildDapArrayQuery(gridDataAccessor.constraints());
      EDV tDataVariables[] = gridDataAccessor.dataVariables();
      boolean entireDataset =
          userDapQuery == null || SSR.percentDecode(userDapQuery).trim().length() == 0;

      // get partial gridDataAccessor, in case of size error
      try (GridDataAccessor partialGda =
          new GridDataAccessor(
              language,
              eddGrid,
              requestUrl,
              tDataVariables[0].destinationName() + arrayQuery,
              true,
              false)) {
        long tSize = partialGda.totalIndex().size();
        Math2.ensureArraySizeOkay(tSize, "OPeNDAP limit");
      }

      // write the dds    //OPeNDAP 2.0, 7.2.3
      OutputStream outputStream = outputStreamSource.outputStream(File2.ISO_8859_1);
      writer =
          File2.getBufferedWriter88591(
              outputStream); // OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go
      // for compatible common 8bit
      eddGrid.writeDDS(language, requestUrl, userDapQuery, writer);

      // write the connector  //OPeNDAP 2.0, 7.2.3
      // see EOL definition for comments
      writer.write(OpendapHelper.EOL + "Data:" + OpendapHelper.EOL);
      writer.flush(); // essential
      // don't close the writer. Leave it hanging. dos.close below closes the outputStream.

      // make the dataOutputStream
      try (DataOutputStream dos = new DataOutputStream(outputStream)) {
        // write the axis variables
        int nAxisVariables = eddGrid.axisVariables().length;
        if (entireDataset) {
          for (int av = 0; av < nAxisVariables; av++)
            gridDataAccessor.axisValues(av).externalizeForDODS(dos);
        }

        // write the data  //OPeNDAP 2.0, 7.3.2.4
        // write elements of the array, in dds order
        for (EDV tDataVariable : tDataVariables) {
          try (GridDataAccessor partialGda =
              new GridDataAccessor(
                  language,
                  eddGrid,
                  requestUrl,
                  tDataVariable.destinationName() + arrayQuery,
                  true,
                  false)) { // rowMajor, convertToNaN

            long tSize = partialGda.totalIndex().size();

            // send the array size (twice)  //OPeNDAP 2.0, 7.3.2.1
            dos.writeInt((int) tSize); // safe since checked above
            dos.writeInt((int) tSize); // safe since checked above

            // send the array data   (Note that DAP doesn't have exact match for some Java data
            // types.)
            PAType type = tDataVariable.destinationDataPAType();

            PrimitiveArray[] pas = partialGda.getPartialDataValues();
            if (type == PAType.BYTE) {
              while (partialGda.incrementChunk()) pas[0].writeDos(dos);
              // pad byte array to 4 byte boundary
              long tn = partialGda.totalIndex().size();
              while (tn++ % 4 != 0) dos.writeByte(0);
            } else if (type == PAType.SHORT
                || // no exact DAP equivalent
                type == PAType.CHAR
                || // no exact DAP equivalent
                type == PAType.INT) {
              while (partialGda.incrementChunk())
                (type == PAType.INT ? pas[0] : new IntArray(pas[0])).writeDos(dos);
            } else if (type == PAType.FLOAT) {
              while (partialGda.incrementChunk()) pas[0].writeDos(dos);
            } else if (type == PAType.LONG
                || // no exact DAP equivalent
                type == PAType.DOUBLE) {
              while (partialGda.incrementChunk())
                (type == PAType.DOUBLE ? pas[0] : new DoubleArray(pas[0])).writeDos(dos);
            } else if (type == PAType.STRING) {
              while (partialGda.incrementChunk()) pas[0].externalizeForDODS(dos);
            } else {
              throw new RuntimeException(
                  EDStatic.messages.errorInternalAr[0] + "unsupported source data type=" + type);
            } /* */

            for (int av = 0; av < nAxisVariables; av++)
              gridDataAccessor.axisValues(av).externalizeForDODS(dos);

            dos.flush();
          }
        }

        dos.flush(); // essential
      } catch (Exception e) {
        String2.log(e.getMessage());
      } finally {
        if (writer != null) {
          try {
            writer.close();
          } catch (Exception e) {
          }
          writer = null;
        }
      }
    } finally {
      if (writer != null)
        try {
          writer.close();
        } catch (Exception e) {
        }
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsDODS done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
