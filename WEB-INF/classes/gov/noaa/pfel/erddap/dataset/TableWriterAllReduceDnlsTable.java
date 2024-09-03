/*
 * TableWriterAllReduceDnlsTable Copyright 2019, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import java.util.HashSet;

/**
 * TableWriterAllReduceDnlsTable is a subclass of TableWriterAll that just keeps the files in one
 * directory and accumulates a list of immediate subdirectory short names.
 *
 * <p>This is for INTERNAL ERDDAP use (for accessibleViaFilesFileTable). This is different from most
 * TableWriters in that finish() doesn't write the data anywhere (to an outputStream or to another
 * tableWriter), it just makes the resulting data table and subdirectory names available.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2019-08-06
 */
public class TableWriterAllReduceDnlsTable extends TableWriterAll {

  String oneDir;
  HashSet<String> subdirHash = new HashSet();

  /**
   * The constructor.
   *
   * @param language the index of the selected language
   * @param tDir a private cache directory for storing the intermediate files, usually
   *     cacheDirectory(datasetID)
   * @param tFileNameNoExt is the fileName-safe fileName without dir or extension (used as basis for
   *     temp files). A random number will be added to it for safety.
   */
  public TableWriterAllReduceDnlsTable(
      int language,
      EDD tEdd,
      String tNewHistory,
      String tDir,
      String tFileNameNoExt,
      String tOneDir) {
    super(language, tEdd, tNewHistory, tDir, tFileNameNoExt);
    oneDir = tOneDir;
  }

  /**
   * This adds the current contents of table (a chunk of data) to the columnStreams. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., open the columnStreams). The number of columns, the column names, and
   * the types of columns must be the same each time this is called.
   *
   * @param table with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues. This TableWriter doesn't change them.
   * @throws Throwable if trouble
   */
  @Override
  public void writeSome(Table table) throws Throwable {
    FileVisitorDNLS.reduceDnlsTableToOneDir(table, oneDir, subdirHash);
    // String2.log("nRows=" + table.nRows() + " subdirHash=" + String2.toCSSVString(subdirHash));
    if (table.nRows() == 0) return;
    super.writeSome(table);
  }

  /**
   * This is called to close the column streams and clean up the metadata. If ignoreFinish=true,
   * nothing will be done.
   *
   * @throws Throwable if trouble
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    // close the column streams
    super.finish();
  }

  /**
   * Call this anytime (but usually after finish()) to return the subdirHashmap of immediate
   * subdirectory short names.
   *
   * @return the subdirHashmap of immediate subdirectory short names.
   */
  public HashSet<String> subdirHash() {
    return subdirHash;
  }
}
