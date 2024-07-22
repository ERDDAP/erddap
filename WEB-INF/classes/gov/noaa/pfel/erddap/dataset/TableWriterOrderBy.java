/*
 * TableWriterOrderBy Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;

/**
 * TableWriterOrderBy provides a way to gather all rows, sort them, then write to some other
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
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-05-10
 */
public class TableWriterOrderBy extends TableWriterAll {

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
   * @param tOrderByCsv the names of the columns to sort by (most to least important)
   */
  public TableWriterOrderBy(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      String tDir,
      String tFileNameNoExt,
      TableWriter tOtherTableWriter,
      String tOrderByCsv) {

    super(tLanguage, tEdd, tNewHistory, tDir, tFileNameNoExt);
    otherTableWriter = tOtherTableWriter;
    String err =
        EDStatic.simpleBilingual(language, EDStatic.queryErrorAr)
            + "No column names were specified for 'orderBy'.";
    if (tOrderByCsv == null || tOrderByCsv.trim().length() == 0) throw new SimpleException(err);
    orderBy = String2.split(tOrderByCsv, ',');
    if (orderBy.length == 0) throw new SimpleException(err);
    for (int i = 0; i < orderBy.length; i++)
      if (orderBy[i].indexOf('/') >= 0)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, EDStatic.queryErrorAr)
                + "'orderBy' doesn't support '/' ("
                + orderBy[i]
                + ").");
  }

  /**
   * This sorts cumulativeTable, then writes it to otherTableWriter If ignoreFinish=true, nothing
   * will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    super.finish();

    Table cumulativeTable = cumulativeTable();
    releaseResources();
    writeAllAndFinish(cumulativeTable);
  }

  /**
   * If caller has the entire table, use this instead of repeated writeSome() + finish(). This
   * overwrites the superclass method.
   *
   * @param tCumulativeTable with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues. This implementation converts them to
   *     NaNs for processing, then back to destinationMV and FV when finished.
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void writeAllAndFinish(Table tCumulativeTable) throws Throwable {
    if (ignoreFinish) {
      writeSome(tCumulativeTable);
      tCumulativeTable.removeAllRows();
      return;
    }
    sort(tCumulativeTable);
    otherTableWriter.writeAllAndFinish(tCumulativeTable);
    otherTableWriter = null;
  }

  private void sort(Table table) {
    // ensure orderBy columns are present in results table
    int[] keys = new int[orderBy.length];
    boolean[] ascending = new boolean[orderBy.length];
    for (int ob = 0; ob < orderBy.length; ob++) {
      keys[ob] = table.findColumnNumber(orderBy[ob]);
      ascending[ob] = true;
      if (keys[ob] < 0)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, EDStatic.queryErrorAr)
                + "'orderBy' column="
                + orderBy[ob]
                + " isn't in the results table.");
    }

    table.sort(keys, ascending);
  }
}
