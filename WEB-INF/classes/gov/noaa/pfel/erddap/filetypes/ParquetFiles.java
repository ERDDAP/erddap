package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.OutputStream;
import java.nio.file.Path;

@FileTypeClass(
    fileTypeExtension = ".parquet",
    fileTypeName = ".parquet",
    infoUrl = "https://parquet.apache.org/",
    versionAdded = "2.25.0",
    contentType = "application/parquet")
public class ParquetFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterAllWithMetadata(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.dir(),
        requestInfo.fileName());
  }

  @Override
  public void writeTableToFileFormat(DapRequestInfo requestInfo, TableWriter tableWriter)
      throws Throwable {
    if (tableWriter instanceof TableWriterAllWithMetadata) {
      saveAsParquet(
          requestInfo.language(),
          requestInfo.outputStream(),
          (TableWriterAllWithMetadata) tableWriter,
          requestInfo.edd().datasetID(),
          false);
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsParquet(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        false,
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_parquetAr[language];
  }

  protected void saveAsParquet(
      int language,
      OutputStreamSource outputStreamSource,
      TableWriterAllWithMetadata twawm,
      String datasetID,
      boolean fullMetadata)
      throws Throwable {
    Table table = twawm.cumulativeTable();
    twawm.releaseResources();
    String parquetTempFileName =
        Path.of(
                EDStatic.config.fullTestCacheDirectory,
                datasetID + Math2.random(Integer.MAX_VALUE) + ".parquet")
            .toString();
    table.writeParquet(parquetTempFileName, fullMetadata);

    try (OutputStream out = outputStreamSource.outputStream(File2.UTF_8)) {
      if (!File2.copy(parquetTempFileName, out)) {
        // outputStream contentType already set,
        // so I can't go back to html and display error message
        // note than the message is thrown if user cancels the transmission; so don't email to me
        throw new SimpleException(String2.ERROR + " while transmitting file.");
      }
    }
    // downloads of e.g., erddap2.css don't work right if not closed. (just if gzip'd?)
    File2.delete(parquetTempFileName);
  }

  /**
   * This writes grid data (not just axis data) to the outputStream in a parquet file Format
   * .parquet file. If no exception is thrown, the data was successfully written.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  protected void saveAsParquet(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      boolean fullMetadata,
      EDDGrid grid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsParquet");
    long time = System.currentTimeMillis();

    boolean isAxisDapQuery = grid.isAxisDapQuery(userDapQuery);
    // write the data to the tableWriterAllWithMetadata
    TableWriterAllWithMetadata twawm =
        new TableWriterAllWithMetadata(
            language,
            grid,
            grid.getNewHistory(requestUrl, userDapQuery),
            grid.cacheDirectory(),
            "parquet"); // A random number will be added to it for safety.
    if (isAxisDapQuery) {
      AxisDataAccessor ada = new AxisDataAccessor(language, grid, requestUrl, userDapQuery);
      grid.saveAsTableWriter(ada, twawm);
    } else {
      try (GridDataAccessor gda =
          new GridDataAccessor(
              language, grid, requestUrl, userDapQuery, true, false)) { // rowMajor, convertToNaN
        grid.saveAsTableWriter(gda, twawm);
      }
    }

    // write the .parquet file
    saveAsParquet(language, outputStreamSource, twawm, grid.datasetID(), fullMetadata);

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsParquet done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
