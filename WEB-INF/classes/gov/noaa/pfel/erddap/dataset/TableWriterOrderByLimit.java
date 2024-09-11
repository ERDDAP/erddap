/*
 * TableWriterOrderByLimit Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;

/**
 * TableWriterOrderByLimit provides a way to sort the response table's rows and just keep the rows
 * where values of the last sort column (often a time variable) are Limit to the specified interval
 * (e.g., every 10 minutes). For example, you could use orderBy(\"stationID,time,10 minutes\") to
 * just get the rows of data for each station as close as possible to 10 minutes apart.
 *
 * <p>For the last sort column (time?), there usually aren't missing values. But if missing values
 * are stored as NaNs, those rows are removed. And if missing values are stored as values, then this
 * will keep the Limit row.
 *
 * <p>Unlike TableWriterAllWithMetadata, this doesn't keep track of min,max for actual_range or
 * update metadata at end. It is assumed that this is like a filter, and that a subsequent
 * TableWriter will handle that if needed.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2017-02-21
 */
public class TableWriterOrderByLimit extends TableWriterAll {

  // set by constructor
  protected TableWriter otherTableWriter;
  public String orderBy[]; // the orderBy var names. The last one will be a numeric column.
  protected int limitN; // eg 1000 rows

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
   * @param tOrderByCsv the names of the columns to sort by (most to least important), with the time
   *     Interval as the last item.
   */
  public TableWriterOrderByLimit(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      String tDir,
      String tFileNameNoExt,
      TableWriter tOtherTableWriter,
      String tOrderByCsv) {

    super(tLanguage, tEdd, tNewHistory, tDir, tFileNameNoExt);
    otherTableWriter = tOtherTableWriter;
    if (tOrderByCsv == null || tOrderByCsv.trim().length() == 0)
      throw new SimpleException(
          EDStatic.bilingual(language, EDStatic.queryErrorAr, EDStatic.queryErrorOrderByLimitAr)
              + (language == 0 ? " " : "\n")
              + "No CSV.");
    String csv[] = String2.split(tOrderByCsv, ',');
    if (csv.length == 0)
      throw new SimpleException(
          EDStatic.bilingual(language, EDStatic.queryErrorAr, EDStatic.queryErrorOrderByLimitAr)
              + (language == 0 ? " " : "\n")
              + "CSV.length=0.");

    // ensure the next to last value is numeric -- done later: by table.orderByLimit()

    // extract the interval (10 minutes -> [10, 60]) from the last value
    orderBy = new String[csv.length - 1];
    System.arraycopy(csv, 0, orderBy, 0, csv.length - 1);
    limitN = String2.parseInt(csv[csv.length - 1]);
    if (limitN <= 0 || limitN == Integer.MAX_VALUE)
      throw new SimpleException(
          EDStatic.bilingual(language, EDStatic.queryErrorAr, EDStatic.queryErrorOrderByLimitAr)
              + (language == 0 ? " " : "\n")
              + "limit="
              + csv[csv.length - 1]
              + " must be a positive integer.");
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
    //  (remove the beyond limitN rows from this partial table)
    //  and leaves perfect job to finish()
    table.orderByLimit(orderBy, limitN); // this handles missingValues and _FillValues temporarily

    // ensure the table's structure is the same as before
    // and write to dataOutputStreams
    super.writeSome(table);
  }

  /**
   * This finishes orderByMax and writes results to otherTableWriter If ignoreFinish=true, nothing
   * will be done.
   *
   * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    super.finish();

    Table cumulativeTable = cumulativeTable();
    releaseResources();
    cumulativeTable.orderByLimit(
        orderBy, limitN); // this handles missingValues and _FillValues temporarily
    otherTableWriter.writeAllAndFinish(cumulativeTable);

    // clean up
    otherTableWriter = null;
  }

  /**
   * If caller has the entire table, use this instead of repeated writeSome() + finish(). This
   * overwrites the superclass method.
   *
   * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void writeAllAndFinish(Table tCumulativeTable) throws Throwable {
    if (ignoreFinish) {
      writeSome(tCumulativeTable);
      tCumulativeTable.removeAllRows();
      return;
    }
    tCumulativeTable.orderByLimit(
        orderBy, limitN); // this handles missingValues and _FillValues temporarily
    otherTableWriter.writeAllAndFinish(tCumulativeTable);
    otherTableWriter = null;
  }
}
