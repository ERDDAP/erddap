package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterWav;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

@FileTypeClass(
    fileTypeExtension = ".wav",
    fileTypeName = ".wav",
    infoUrl = "https://en.wikipedia.org/wiki/WAV",
    versionAdded = "1.82.0")
public class WavFiles extends CacheLockFiles {

  public WavFiles() {
    super(false);
  }

  @Override
  protected void generateTableFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable {
    // This is a weird file type. EDDTable uses a TableWriter, but EDDGrid uses a CacheLockFile
    // approach.
    throw new UnsupportedOperationException("Unimplemented method 'generateTableFile'");
  }

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    if (File2.isFile(requestInfo.dir() + requestInfo.fileName() + ".wav")) {
      try (OutputStream out = requestInfo.outputStream().outputStream("")) {
        if (!File2.copy(requestInfo.dir() + requestInfo.fileName() + ".wav", out))
          throw new SimpleException(String2.ERROR + " while transmitting file.");
      }
    }
    EDDTable eddTable = requestInfo.getEDDTable();
    if (eddTable == null) {
      throw new SimpleException(
          EDStatic.bilingual(
              requestInfo.language(),
              EDStatic.messages.queryErrorAr[0] + EDStatic.messages.errorInternalAr[0],
              EDStatic.messages.queryErrorAr[requestInfo.language()]
                  + EDStatic.messages.errorInternalAr[requestInfo.language()]));
    }
    TableWriter tableWriter =
        new TableWriterWav(
            requestInfo.language(),
            requestInfo.edd(),
            requestInfo.newHistory(),
            requestInfo.outputStream(),
            requestInfo.dir(),
            requestInfo.fileName() + ".wav");

    if (tableWriter != null) {
      TableWriter tTableWriter =
          eddTable.encloseTableWriter(
              requestInfo.language(),
              true, // alwaysDoAll
              requestInfo.dir(),
              requestInfo.fileName(),
              tableWriter,
              requestInfo.requestUrl(),
              requestInfo.userDapQuery());
      if (eddTable.handleViaFixedOrSubsetVariables(
          requestInfo.language(),
          requestInfo.loggedInAs(),
          requestInfo.requestUrl(),
          requestInfo.userDapQuery(),
          tTableWriter)) {
      } else {
        if (tTableWriter != tableWriter)
          tTableWriter =
              eddTable.encloseTableWriter(
                  requestInfo.language(),
                  false, // alwaysDoAll
                  requestInfo.dir(),
                  requestInfo.fileName(),
                  tableWriter,
                  requestInfo.requestUrl(),
                  requestInfo.userDapQuery());
        eddTable.getDataForDapQuery(
            requestInfo.language(),
            requestInfo.loggedInAs(),
            requestInfo.requestUrl(),
            requestInfo.userDapQuery(),
            tTableWriter);
      }
      tTableWriter.close();
      tableWriter.close();
    }
  }

  @Override
  protected void generateGridFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable {
    saveAsWav(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        cacheFullName,
        requestInfo.getEDDGrid());
    File2.isFile(
        cacheFullName,
        5); // for possible waiting thread, wait till file is visible via operating system
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_wavAr[language];
  }

  /**
   * This creates a .wav file with the requested grid (not axis) data. If no exception is thrown,
   * the data was successfully written.
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but it unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160].
   * @param fullOutName is dir + UniqueName + ".wav"
   * @throws Throwable if trouble.
   */
  private void saveAsWav(
      int language, String requestUrl, String userDapQuery, String fullOutName, EDDGrid grid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsWav");
    long time = System.currentTimeMillis();
    int randomInt = Math2.random(Integer.MAX_VALUE);
    String fullDosName = fullOutName + ".dos" + randomInt;
    String errorWhile =
        EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
            + " while writing .wav file: ";

    // .kml not available for axis request
    if (grid.isAxisDapQuery(userDapQuery))
      throw new SimpleException(errorWhile + "The .wav format is for data requests only.");

    DataOutputStream dos = null;
    // ** create gridDataAccessor first,
    // to check for error when parsing query or getting data
    try (GridDataAccessor gda =
        new GridDataAccessor(
            language, grid, requestUrl, userDapQuery, true, false)) { // rowMajor, convertToNaN
      EDV tDataVariables[] = gda.dataVariables();
      int nDV = tDataVariables.length;

      // ensure all same type of data (and not char or string)
      String tPATypeString = tDataVariables[0].destinationDataType();

      Test.ensureTrue(
          !tPATypeString.equals("String") && !tPATypeString.equals("char"),
          errorWhile + "All data columns must be numeric.");
      boolean java8 = System.getProperty("java.version").startsWith("1.8.");
      if (java8 && (tPATypeString.equals("float") || tPATypeString.equals("double")))
        throw new SimpleException(
            errorWhile + "Until Java 9, float and double values can't be written to .wav files.");
      for (int dvi = 1; dvi < nDV; dvi++) {
        Test.ensureEqual(
            tPATypeString,
            tDataVariables[dvi].destinationDataType(),
            errorWhile + "All data columns must be of the same data type.");
      }

      // write data to dos
      dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fullDosName)));

      // send the data to dos
      PrimitiveArray[] pdv = gda.getPartialDataValues();
      NDimensionalIndex partialIndex = gda.partialIndex();
      if (tPATypeString.equals("long")) {
        while (gda.increment()) {
          int i =
              (int)
                  partialIndex.getIndex(); // safe since partialIndex size checked when constructed
          for (int dvi = 0; dvi < nDV; dvi++)
            dos.writeInt((int) (pdv[dvi].getLong(i) >> 32)); // as int
        }
      } else {
        // all other types
        while (gda.increment()) {
          int i =
              (int)
                  partialIndex.getIndex(); // safe since partialIndex size checked when constructed
          for (int dvi = 0; dvi < nDV; dvi++) pdv[dvi].writeDos(dos, i);
        }
      }
      dos.close();

      // create the wav file
      Table.writeWaveFile(
          fullDosName,
          nDV,
          gda.totalIndex().size(),
          tPATypeString.equals("long") ? "int" : tPATypeString,
          gda.globalAttributes(),
          randomInt,
          fullOutName,
          time);
    } finally {
      if (dos != null) {
        try {
          dos.close();
        } catch (Exception e) {
          String2.log("Error closing stream, log and continue: " + e.getMessage());
        }
      }
    }
    File2.delete(fullDosName);

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log("  EDDGrid.saveAsWav done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
