/*
 * TableWriterNccsv Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TableWriterNccsv provides a way to write a table to an NCCSV file (see
 * https://erddap.github.io/NCCSV.html ) outputStream in chunks so that the whole table doesn't have
 * to be in memory at one time. This is used by EDDTable. The outputStream isn't obtained until the
 * first call to writeSome().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2017-04-06
 */
public class TableWriterNccsv extends TableWriter {

  // set by constructor

  // set by firstTime
  protected volatile boolean isLong[];
  protected volatile boolean isULong[];
  protected volatile boolean isTimeStamp[];
  protected volatile String time_precision[];
  protected volatile BufferedWriter writer;

  public volatile AtomicLong totalNRows = new AtomicLong(0);

  /**
   * The constructor.
   *
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   */
  public TableWriterNccsv(
      int tLanguage, EDD tEdd, String tNewHistory, OutputStreamSource tOutputStreamSource) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
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
   *     NaNs.
   * @throws Throwable if trouble
   */
  @Override
  public void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;

    // ensure the table's structure is the same as before
    boolean firstTime = columnNames == null;
    ensureCompatible(table);

    // do firstTime stuff
    int nColumns = table.nColumns();
    if (firstTime) {

      // write the header
      writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));

      // write the global attributes   (ensureCompatibly added newHistory)
      writer.write(globalAttributes.toNccsvString(String2.NCCSV_GLOBAL));

      // write the column attributes
      isLong = new boolean[nColumns];
      isULong = new boolean[nColumns];
      isTimeStamp = new boolean[nColumns];
      time_precision = new String[nColumns];
      for (int col = 0; col < nColumns; col++) {

        String tClass = table.getColumn(col).elementTypeString();
        isLong[col] = tClass.equals("long");
        isULong[col] = tClass.equals("ulong");
        Attributes catts = table.columnAttributes(col);
        String u = catts.getString("units");
        isTimeStamp[col] = u != null && (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
        if (isTimeStamp[col]) {
          tClass = "String";
          time_precision[col] = catts.getString(EDV.TIME_PRECISION);
          catts.set("units", Calendar2.timePrecisionToTimeFormat(time_precision[col]));
          PrimitiveArray pa = catts.get("actual_range");
          if (pa != null && pa instanceof DoubleArray && pa.size() == 2) {
            StringArray sa = new StringArray();
            for (int i = 0; i < 2; i++)
              sa.add(
                  Calendar2.epochSecondsToLimitedIsoStringT(
                      time_precision[col], pa.getDouble(0), ""));
            catts.set("actual_range", sa);
          }
        }

        // this never detects *SCALAR* because it can't see all the data
        // so always write the *DATA_TYPE*
        writer.write(
            String2.toNccsvDataString(table.getColumnName(col))
                + ","
                + String2.NCCSV_DATATYPE
                + ","
                + tClass
                + "\n");

        // then write all the other attributes
        writer.write(catts.toNccsvString(table.getColumnName(col)));
      }

      // *END_METADATA*
      writer.write("\n" + String2.NCCSV_END_METADATA + "\n");

      // write the column names
      for (int col = 0; col < nColumns; col++) {
        writer.write(String2.toNccsvDataString(table.getColumnName(col))); // shouldn't need quotes
        writer.write(col == nColumns - 1 ? "\n" : ",");
      }
    }

    // *** do everyTime stuff
    // no: convertToStandardMissingValues(table);  //NaNs; not the method in Table, so metadata is
    // unchanged

    int nRows = table.nRows();
    boolean flushAfterward =
        totalNRows.get() == 0; // flush initial chunk so info gets to user quickly
    totalNRows.addAndGet(nRows);
    // no: avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
    // no: Math2.ensureArraySizeOkay(totalNRows, "NCCSV");

    // write the data
    PrimitiveArray pas[] = new PrimitiveArray[nColumns];
    for (int col = 0; col < nColumns; col++) pas[col] = table.getColumn(col);

    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nColumns; col++) {
        if (isTimeStamp[col]) {
          writer.write(
              Calendar2.epochSecondsToLimitedIsoStringT(
                  time_precision[col], pas[col].getDouble(row), ""));
        } else {
          String ts = pas[col].getNccsvDataString(row);
          // String2.log(">> row=" + row + " col=" + col + " ts=" + ts + " maxIsMV=" +
          // pas[col].getMaxIsMV());
          writer.write(ts);
          if (isLong[col]) {
            if (ts.length() > 0)
              writer.write('L'); // special case not handled by getNccsvDataString
          } else if (isULong[col]) {
            if (ts.length() > 0)
              writer.write("uL"); // special case not handled by getNccsvDataString
          }
        }
        writer.write(col == nColumns - 1 ? "\n" : ",");
      }
    }

    if (flushAfterward) writer.flush();
  }

  /**
   * This writes any end-of-file info to the stream and flush the stream. If ignoreFinish=true,
   * nothing will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    // check for MustBe.THERE_IS_NO_DATA
    if (writer == null) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    // *END_DATA*
    writer.write(String2.NCCSV_END_DATA + "\n");

    writer.flush(); // essential

    // diagnostic
    if (verbose)
      String2.log("TableWriterNccsv done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * This is a convenience method to write an entire table in one step.
   *
   * @throws Throwable if trouble (no columns is trouble; no rows is not trouble)
   */
  public static void writeAllAndFinish(
      int language,
      EDD tEdd,
      String tNewHistory,
      Table table,
      OutputStreamSource tOutputStreamSource)
      throws Throwable {

    TableWriterNccsv twn = new TableWriterNccsv(language, tEdd, tNewHistory, tOutputStreamSource);
    twn.writeAllAndFinish(table);
  }
}
