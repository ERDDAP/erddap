/*
 * TableWriterOrderByCount Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.util.Arrays;
import java.util.BitSet;

/**
 * TableWriterDistinct provides a way to gather all rows, sort them, then write to some other
 * TableWriter. This functions like SQL's ORDER BY.
 *
 * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
 *
 * <p>This doesn't do anything to missing values and doesn't assume they are stored as NaN or fake
 * missing values.
 *
 * <p>Unlike TableWriterAllWithMetadata, this doesn't keep track of min,max for actual_range or
 * update metadata at end. It is assumed that this is like a filter, and that a subsequent
 * TableWriter will handle that if needed.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2017-07-12
 */
public class TableWriterOrderByCount extends TableWriterAll {

  // set by constructor
  protected TableWriter otherTableWriter;
  public String orderBy[];

  /**
   * The constructor.
   *
   * @param language the index of the selected language
   * @param tDir a private cache directory for storing the intermediate files, usually
   *     cacheDirectory(datasetID)
   * @param tFileNameNoExt is the fileName without dir or extension (used as basis for temp files).
   *     A random number will be added to it for safety.
   * @param tOtherTableWriter the tableWriter that will receive the unique rows found by this
   *     tableWriter.
   * @param tOrderByCountCsv the names of the columns to sort by (most to least important)
   */
  public TableWriterOrderByCount(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      String tDir,
      String tFileNameNoExt,
      TableWriter tOtherTableWriter,
      String tOrderByCsv) {

    super(tLanguage, tEdd, tNewHistory, tDir, tFileNameNoExt);
    otherTableWriter = tOtherTableWriter;
    orderBy =
        String2.isSomething(tOrderByCsv)
            ? String2.split(tOrderByCsv, ',')
            : new String[0]; // size==0 is okay
  }

  /**
   * This adds the current contents of table (a chunk of data) to the OutputStream. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., call OutputStreamSource.outputStream() and write file header). The
   * number of columns, the column names, and the types of columns must be the same each time this
   * is called.
   *
   * @param table with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues. This implementation converts them to
   *     NaNs for processing, then back to destinationMV and FV when finished.
   * @throws Throwable if trouble
   */
  @Override
  public void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;

    // to save time and disk space, this just does a partial job
    //  (remove non-max rows from this partial table)
    //  and leaves perfect job to finish()
    table.orderByCount(orderBy); // this handles missingValues and _FillValues permanently

    // ensure the table's structure is the same as before
    // and write to dataOutputStreams
    super.writeSome(table);
  }

  /**
   * This processes the cumulativeTable, then writes it to otherTableWriter If ignoreFinish=true,
   * nothing will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    super.finish();

    Table cumTable = cumulativeTable();
    releaseResources();

    // combine results
    // missing_value and _FillValue are all done. All data are counts.
    int nRows = cumTable.nRows();
    int nCols = cumTable.nColumns();
    int keyCols[] =
        cumTable.keyColumnNamesToNumbers(
            "orderByCount",
            // just get the column names, ignoring rounding e.g. time not time/1day
            Arrays.stream(orderBy).map((s) -> s.split("/")[0]).toArray(size -> new String[size]));
    int nKeyCols = keyCols.length;

    // sort based on keys
    if (nKeyCols > 0) cumTable.ascendingSort(keyCols);
    // String2.log(dataToString());

    // note which are keyCol
    boolean isKeyCol[] = new boolean[nCols]; // all false
    for (int kc = 0; kc < nKeyCols; kc++) isKeyCol[keyCols[kc]] = true;

    // get pas and set units to "count"
    PrimitiveArray pas[] = new PrimitiveArray[nCols];
    for (int col = 0; col < nCols; col++) {
      pas[col] = cumTable.getColumn(col);
      if (!isKeyCol[col]) {
        Attributes atts = cumTable.columnAttributes(col);
        atts.set("_FillValue", Integer.MAX_VALUE);
        atts.remove("actual_range");
        atts.remove("cf_role");
        atts.remove("colorBarMinimum");
        atts.remove("colorBarMaximum");
        atts.remove("colorBarPalette");
        atts.remove("colorBarScale");
        atts.remove("missing_value");
        String s = atts.getString("standard_name");
        if (s != null) atts.set("standard_name", s + " number_of_observations");
        atts.set("units", "count");
      }
    }

    // walk through the table
    int resultsRow = -1;
    BitSet keep = new BitSet(nRows); // all false
    for (int row = 0; row < nRows; row++) {

      // isNewGroup?
      boolean isNewGroup = true;
      if (row > 0) {
        isNewGroup = false;
        if (nKeyCols > 0) {
          for (int kc = nKeyCols - 1;
              kc >= 0;
              kc--) { // count down more likely to find change sooner
            if (pas[keyCols[kc]].compare(row - 1, row) != 0) {
              isNewGroup = true;
              break;
            }
          }
        }
      }
      if (isNewGroup) {
        resultsRow = row; // results for this group will be merged onto this row
        keep.set(row);
        continue;
      }

      // increment count?
      for (int col = 0; col < nCols; col++) {
        // String2.log("row=" + row + " col=" + col + "value=\"" + columns.get(col).getString(row) +
        // "\"");
        if (!isKeyCol[col]) {
          PrimitiveArray pa = pas[col];
          pa.setInt(resultsRow, pa.getInt(resultsRow) + pa.getInt(row));
        }
      }
    }

    // just keep new group
    cumTable.justKeep(keep);

    // send results to otherTableWriter;
    otherTableWriter.writeAllAndFinish(cumTable);
    otherTableWriter = null;
  }

  // This uses super.writeAllAndFinish() because writeSome stores processed info

}
