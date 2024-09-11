/*
 * TableWriterUnits Copyright 2010, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.util.SimpleException;
import com.cohort.util.Units2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;

/**
 * TableWriterUnits provides a way to change the units standard (e.g., to UDUNITS or UCUM) of the
 * results.
 *
 * <p>This is a flow-through filter.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2010-05-05
 */
public class TableWriterUnits extends TableWriter {

  // set by constructor
  protected TableWriter otherTableWriter;
  public String fromUnits, toUnits;

  /**
   * The constructor.
   *
   * @param language the index of the selected language
   * @param tOtherTableWriter the tableWriter that will receive the unique rows found by this
   *     tableWriter.
   * @param tFromUnits e.g., UDUNITS or UCUM
   * @param tToUnits e.g., UDUNITS or UCUM
   */
  public TableWriterUnits(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      TableWriter tOtherTableWriter,
      String tFromUnits,
      String tToUnits) {

    super(tLanguage, tEdd, tNewHistory, null);
    otherTableWriter = tOtherTableWriter;
    fromUnits = tFromUnits;
    toUnits = tToUnits;
    if (otherTableWriter == null)
      throw new SimpleException("Internal error: " + "otherTableWriter is null!");
    checkFromToUnits(language, fromUnits, toUnits);
  }

  /**
   * This checks fromUnits and toUnits for validity.
   *
   * @param tFromUnits e.g., UDUNITS or UCUM
   * @param tToUnits e.g., UDUNITS or UCUM
   * @throws Exception if trouble
   */
  public static void checkFromToUnits(int language, String fromUnits, String toUnits) {
    if (fromUnits == null || !(fromUnits.equals("UDUNITS") || fromUnits.equals("UCUM")))
      throw new SimpleException(
          "Setup error: "
              + "fromUnits="
              + fromUnits
              + " (usually from units_standard in setup.xml) must be UDUNITS or UCUM.");

    if (toUnits == null || !(toUnits.equals("UDUNITS") || toUnits.equals("UCUM")))
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.queryErrorAr)
              + "toUnits="
              + fromUnits
              + " must be UDUNITS or UCUM.");
  }

  /**
   * This adds the current contents of table (a chunk of data) to the OutputStream. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., call OutputStreamSource.outputStream() and write file header). The
   * number of columns, the column names, and the types of columns must be the same each time this
   * is called.
   *
   * @param table with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues. This implementation doesn't change them.
   * @throws Throwable if trouble
   */
  @Override
  public synchronized void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;

    // to save memory, this just does a feeble job (remove non-max rows from this partial table)
    //  and leaves perfect job to finish()
    changeUnits(language, table, fromUnits, toUnits);

    // ensure the table's structure is the same as before
    ensureCompatible(table); // this stores everything that TableWriter needs

    // pass it on
    otherTableWriter.writeSome(table);
  }

  /**
   * This calls otherTableWriter.finish. If ignoreFinish=true, nothing will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public synchronized void finish() throws Throwable {
    if (ignoreFinish) return;

    // clean up
    otherTableWriter.finish();
    otherTableWriter = null;
  }

  /**
   * If caller has the entire table, use this instead of repeated writeSome() + finish(). This
   * overwrites the superclass method.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public synchronized void writeAllAndFinish(Table tCumulativeTable) throws Throwable {
    if (ignoreFinish) {
      writeSome(tCumulativeTable);
      tCumulativeTable.removeAllRows();
      return;
    }
    changeUnits(language, tCumulativeTable, fromUnits, toUnits);
    otherTableWriter.writeAllAndFinish(tCumulativeTable);
  }

  /**
   * This is a convenience method to write an entire table in one step.
   *
   * @throws Throwable if trouble (no columns is trouble; no rows is not trouble)
   */
  public static void writeAllAndFinish(
      int language,
      EDD tEdd,
      Table tTable,
      TableWriter tOtherTableWriter,
      String tFromUnits,
      String tToUnits)
      throws Throwable {

    changeUnits(language, tTable, tFromUnits, tToUnits);
    tOtherTableWriter.writeAllAndFinish(tTable);
  }

  /**
   * This changesUnits for this table.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  public static void changeUnits(int language, Table table, String fromUnits, String toUnits)
      throws Throwable {
    checkFromToUnits(language, fromUnits, toUnits);
    boolean toUcum = fromUnits.equals("UDUNITS") && toUnits.equals("UCUM");
    boolean toUdunits = fromUnits.equals("UCUM") && toUnits.equals("UDUNITS");
    if (!toUcum && !toUdunits) // e.g., from UDUNITS to UDUNITS
    return;
    int nColumns = table.nColumns();
    for (int col = 0; col < nColumns; col++) {
      Attributes atts = table.columnAttributes(col);
      String units = atts.getString("units");
      if (units == null || units.equals("")) continue;
      if (toUcum) atts.set("units", Units2.safeUdunitsToUcum(units));
      else if (toUdunits) atts.set("units", Units2.safeUcumToUdunits(units));
    }
  }
}
