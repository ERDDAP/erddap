package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterSeparatedValue;

public abstract class SeparatedValue extends TableWriterFileType {
  protected final SeparatedValuesConfig config;

  public SeparatedValue(SeparatedValuesConfig config) {
    this.config = config;
  }

  public record SeparatedValuesConfig(
      String separator, boolean twoQuotes, boolean writeColumns, char writeUnits) {}

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterSeparatedValue(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream(),
        config.separator(),
        config.twoQuotes(),
        config.writeColumns(),
        config.writeUnits(),
        "NaN");
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsSeparatedAscii(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        config.separator(),
        config.twoQuotes(),
        config.writeColumns(),
        config.writeUnits(),
        requestInfo.getEDDGrid());
  }

  /**
   * This writes the axis or grid data to the outputStream in a separated-value ASCII format. If no
   * exception is thrown, the data was successfully written.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @param separator e.g., tab or comma (without space)
   * @param twoQuotes if true, internal double quotes are converted to 2 double quotes.
   * @param writeColumnNames
   * @param writeUnits '0'=no, '('=on the first line as "variableName (units)" (if present), 2=on
   *     the second line.
   * @throws Throwable if trouble.
   */
  private void saveAsSeparatedAscii(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      String separator,
      boolean twoQuotes,
      boolean writeColumnNames,
      char writeUnits,
      EDDGrid eddGrid)
      throws Throwable {

    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsSeparatedAscii separator=\""
              + String2.annotatedString(separator)
              + "\"");
    long time = System.currentTimeMillis();

    // get dataAccessor first, in case of error when parsing query
    boolean isAxisDapQuery = eddGrid.isAxisDapQuery(userDapQuery);
    AxisDataAccessor ada = null;
    GridDataAccessor gda = null;
    if (isAxisDapQuery) ada = new AxisDataAccessor(language, eddGrid, requestUrl, userDapQuery);
    else
      gda =
          new GridDataAccessor(
              language, eddGrid, requestUrl, userDapQuery, true, false); // rowMajor, convertToNaN

    // write the data to the tableWriter
    TableWriter tw =
        new TableWriterSeparatedValue(
            language,
            eddGrid,
            eddGrid.getNewHistory(language, requestUrl, userDapQuery),
            outputStreamSource,
            separator,
            twoQuotes,
            writeColumnNames,
            writeUnits,
            "NaN");
    if (isAxisDapQuery) {
      eddGrid.saveAsTableWriter(ada, tw);
    } else {
      eddGrid.saveAsTableWriter(gda, tw);
      gda.close();
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsSeparatedAscii done. TIME="
              + (System.currentTimeMillis() - time)
              + "ms\n");
  }
}
