/*
 * TableWriterAllReduceDnlsTableNLevels Copyright 2019, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.util.BitSet;
import java.util.HashSet;

/**
 * TableWriterAllReduceDnlsTableNLevels is a subclass of TableWriterAll that just keeps the files
 * and dirs to n levels.
 *
 * <p>This is for INTERNAL ERDDAP use (for accessibleViaFilesFileTable). This is different from most
 * TableWriters in that finish() doesn't write the data anywhere (to an outputStream or to another
 * tableWriter), it just makes the resulting data table and subdirectory names available.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2019-08-06
 */
public class TableWriterAllReduceDnlsTableNLevels extends TableWriterAll {

  String baseDir;
  int nLevels;
  HashSet<String> subdirHash = new HashSet();

  /**
   * The constructor.
   *
   * @param language the index of the selected language
   * @param tDir a private cache directory for storing the intermediate files, usually
   *     cacheDirectory(datasetID)
   * @param tFileNameNoExt is the fileName-safe fileName without dir or extension (used as basis for
   *     temp files). A random number will be added to it for safety.
   * @param tnLevels The number of dir levels beyond baseDir (0+)
   */
  public TableWriterAllReduceDnlsTableNLevels(
      int language,
      EDD tEdd,
      String tNewHistory,
      String tDir,
      String tFileNameNoExt,
      String tBaseDir,
      int tnLevels) {
    super(language, tEdd, tNewHistory, tDir, tFileNameNoExt);
    baseDir = tBaseDir;
    nLevels = tnLevels;
  }

  /**
   * This adds the current contents of table (a chunk of data) to the columnStreams. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., open the columnStreams). The number of columns, the column names, and
   * the types of columns must be the same each time this is called.
   *
   * @param table a DNLS table with destinationValues. The table should have missing values stored
   *     as destinationMissingValues or destinationFillValues. This TableWriter doesn't change them.
   * @throws Throwable if trouble
   */
  @Override
  public void writeSome(Table table) throws Throwable {
    // preprocess the table
    int nRows = table.nRows();
    if (table.nRows() == 0) return;
    BitSet keep = new BitSet(nRows);
    StringArray dirSA = (StringArray) table.getColumn(0);
    for (int row = 0; row < nRows; row++) {
      String s = dirSA.get(row);
      if (!s.startsWith(baseDir))
        throw new RuntimeException(
            "All of the directories in the DNLS table should start with "
                + baseDir
                + " (failed="
                + s
                + ").");
      String ts = s.substring(baseDir.length());
      int po = String2.findNth(ts, '/', nLevels);
      if (po < 0) {
        // a file, e.g., a, or a/b for nLevels=2
        po = ts.length();
        keep.set(row);
      } else {
        // extract a dir, e.g., a/b/c/d becomes a/b/ for nLevels=2
        subdirHash.add(ts.substring(0, po + 1));
        // keep.clear(row); //already false
      }
    }
    table.justKeep(keep);

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
   * subdirectory names (after baseDir).
   *
   * @return the subdirHashmap of immediate subdirectory short names.
   */
  public HashSet<String> subdirHash() {
    return subdirHash;
  }
}
