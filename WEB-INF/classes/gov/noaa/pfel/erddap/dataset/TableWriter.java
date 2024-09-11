/*
 * TableWriter Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.variable.EDV;

/**
 * TableWriter provides a way to write a table to an outputStream in chunks so that the whole table
 * doesn't have to be in memory at one time. This is used by EDDTable. The outputStream isn't
 * obtained until the first call to writeSome().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-23
 */
public abstract class TableWriter {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  /**
   * A subclass will set this to true when it doesn't want any more data (e.g., TableWriterHtmlTable
   * when showFirstNRows is reached)
   */
  public volatile boolean noMoreDataPlease = false;

  /**
   * The code that creates the TableWriter may set this to true to supress/delay the finish() method
   * (and the finish() part of writeAllAndFinish().
   */
  public boolean ignoreFinish = false;

  // these are set by the constructor
  protected long time;
  protected int language;
  protected EDD edd;
  protected String newHistory;
  protected OutputStreamSource outputStreamSource;

  // these are set the first time ensureCompatible is called
  protected String[] columnNames;
  protected PAType[] columnTypes;
  protected Attributes[] columnAttributes;
  protected Attributes globalAttributes;

  protected boolean[] columnMaxIsMV; // default is all false. Once 'true', it stays true.

  /**
   * The constructor.
   *
   * @param language the index of the selected language
   * @param tEdd will be used as the source of metadata if not null. If null (e.g., when Erddap.java
   *     uses TableWriters), metadata will be from the first table sent to writeSome().
   * @param tNewHistory usually from getNewHistory(requestUrl, userDapQuery). May be old history or
   *     null.
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   */
  public TableWriter(
      int tLanguage, EDD tEdd, String tNewHistory, OutputStreamSource tOutputStreamSource) {
    time = System.currentTimeMillis();
    language = tLanguage;
    edd = tEdd;
    newHistory = tNewHistory;
    outputStreamSource = tOutputStreamSource;
  }

  /**
   * This makes sure that the number of columns, the column names, and the types of columns are the
   * same as before (or that this is the first time this is called).
   *
   * @param table the table with a chunk of data
   * @throws Throwable if not compatible or other trouble
   */
  protected void ensureCompatible(Table table) throws Throwable {
    String[] tColumnNames = table.getColumnNames();
    int nColumns = tColumnNames.length;
    PAType[] tColumnTypes = new PAType[nColumns];
    for (int c = 0; c < nColumns; c++) tColumnTypes[c] = table.getColumn(c).elementType();

    // first time this is called? note column names, types, and metadata
    if (columnNames == null) {
      columnNames = tColumnNames;
      columnTypes = tColumnTypes;
      columnAttributes = new Attributes[nColumns];
      columnMaxIsMV = new boolean[nColumns];
      for (int col = 0; col < nColumns; col++) {
        Attributes colAttsClone = null;
        if (edd != null) {
          try { // findVar throws exception if not found
            EDV edv =
                edd.findVariableByDestinationName(columnNames[col]); // finds axis or dataVariable
            colAttsClone = new Attributes(edv.combinedAttributes());
          } catch (Throwable t) {
            // rare, e.g., happens with added "Count" and "Percent"
            // columns in countTable for "2 = viewDistinctDataCounts"
            // for EDDTable.respondToSubsetQuery.
            String2.log(
                "TableWriter.ensureCompatible didn't find colName="
                    + columnNames[col]
                    + " in dataset="
                    + edd.datasetID());
          }
        }
        if (colAttsClone == null) colAttsClone = new Attributes(table.columnAttributes(col));
        columnAttributes[col] = colAttsClone;

        // if maxIsMV is ever true for a column, it stays true
        // if ("testULong".equals(columnNames[col])) String2.log(">> ensureCompatible testULong
        // twawm=" + this + " maxIsMV=" + table.getColumn(col).getMaxIsMV());
        if (table.getColumn(col).getMaxIsMV()) columnMaxIsMV[col] = true;

        // String2.log("\nTableWriter attributes " + columnNames[col] + "\n" +
        // columnAttributes[col]);
      }
      if (edd == null) globalAttributes = table.globalAttributes(); // table already has deep clone
      else globalAttributes = new Attributes(edd.combinedGlobalAttributes()); // make deep clone
      globalAttributes.set("history", newHistory); // if null, it won't be set.
      return;
    }

    // not first time calling ensureCompatible, so ensure it's compatible
    // ensure columnNames are same
    if (columnNames.length != tColumnNames.length)
      throw new RuntimeException(
          "Internal error in TableWriter: newNColumns="
              + tColumnNames.length
              + " != oldNColumns="
              + columnNames.length
              + ".");
    for (int c = 0; c < nColumns; c++) {
      if (!columnNames[c].equals(tColumnNames[c]))
        throw new RuntimeException(
            "Internal error in TableWriter: for column="
                + c
                + ", newName="
                + tColumnNames[c]
                + " != oldName="
                + columnNames[c]
                + ".");
      if (!columnTypes[c].equals(tColumnTypes[c]))
        throw new RuntimeException(
            "Internal error in TableWriter: for column#"
                + c
                + "="
                + columnNames[c]
                + ", newType="
                + tColumnTypes[c]
                + " != oldType="
                + columnTypes[c]
                + ".");

      // if maxIsMV is ever true for a column, it stays true
      if (table.getColumn(c).getMaxIsMV()) columnMaxIsMV[c] = true;
      // set this column to be like previous
      table.getColumn(c).setMaxIsMV(columnMaxIsMV[c]);

      // restore missing_value and _FillValue attributes
      //  (if removed via convertToStandardMissingValues() and reuse of the table)
      Attributes originalAtts = columnAttributes[c];
      Attributes tableAtts = table.columnAttributes(c);
      PrimitiveArray pa = originalAtts.get("missing_value");
      if (pa != null) tableAtts.set("missing_value", pa);
      pa = originalAtts.get("_FillValue");
      if (pa != null) tableAtts.set("_FillValue", pa);
    }
  }

  /**
   * This adds the current contents of table (a chunk of data) to the outputSteam. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., call OutputStreamSource.outputStream() and write file header). The
   * number of columns, the column names, and the types of columns must be the same each time this
   * is called.
   *
   * @param table with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues.
   * @throws Throwable if trouble
   */
  public abstract void writeSome(Table table) throws Throwable;

  /**
   * This writes any end-of-file info to the stream and flushes the stream. If ignoreFinish=true,
   * nothing will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  public abstract void finish() throws Throwable;

  /**
   * If caller has the entire table, use this instead of repeated writeSome() + finish(). Some
   * subclasses may be able to do things more efficiently if the entire table is available. This
   * default implementation just calls writeSome() and finish().
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  public void writeAllAndFinish(Table table) throws Throwable {
    writeSome(table);
    if (ignoreFinish) {
      table.removeAllRows();
      return;
    }
    finish();
  }

  /**
   * This returns the number of columns. Call this after finish() is called as part of getting the
   * results.
   *
   * @return the number of columns.
   */
  public int nColumns() {
    return columnNames.length;
  }

  /**
   * This returns the internal list of columnNames, so don't change it.
   *
   * @return the column names.
   */
  public String[] columnNames() {
    return columnNames;
  }

  /**
   * This returns one of the destination column's names. Call this after finish() is called as part
   * of getting the results.
   *
   * @param col 0..
   * @return one of the destination column's names.
   */
  public String columnName(int col) {
    return columnNames[col];
  }

  /**
   * This returns one of the destination column's types. Call this after finish() is called as part
   * of getting the results.
   *
   * @param col 0..
   * @return one of the destination column's types.
   */
  public PAType columnType(int col) {
    return columnTypes[col];
  }

  /**
   * This returns maxIsMV for the specified column.
   *
   * @param col 0..
   * @return maxIsMV for the specified column.
   */
  public boolean columnMaxIsMV(int col) {
    return columnMaxIsMV[col];
  }

  /**
   * This returns one of the destination column's columnAttributes. Note that actual_range hasn't
   * been modified for this data subset.
   *
   * @param col 0..
   * @return one of the destination column's Attributes.
   */
  public Attributes columnAttributes(int col) {
    return columnAttributes[col];
  }

  /**
   * This returns the destination global Attributes. Note that the values (e.g.,
   * Northernmost_Northing) haven't been modified for this data subset.
   *
   * @return the global Attributes.
   */
  public Attributes globalAttributes() {
    return globalAttributes;
  }

  /**
   * Given the information available, this makes an empty table with the appropriate columns and
   * metadata.
   */
  public Table makeEmptyTable() {
    if (columnNames == null) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    int nColumns = columnNames.length;
    Table table = new Table();
    table.globalAttributes().set(globalAttributes);
    for (int col = 0; col < nColumns; col++) {
      PrimitiveArray pa = PrimitiveArray.factory(columnTypes[col], 1024, false);
      pa.setMaxIsMV(columnMaxIsMV[col]);
      table.addColumn(col, columnNames[col], pa, columnAttributes[col]);
    }
    return table;
  }

  public void logCaughtNoMoreDataPlease(String datasetID) {
    String2.log("datasetID=" + datasetID + " caught tableWriter.noMoreDataPlease.");
  }
}
