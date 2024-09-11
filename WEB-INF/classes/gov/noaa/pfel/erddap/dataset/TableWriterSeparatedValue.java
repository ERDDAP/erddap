/*
 * TableWriterSeparatedValue Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TableWriterSeparatedValue provides a way to write a table to comma or tab separated value ASCII
 * outputStream in chunks so that the whole table doesn't have to be in memory at one time. This is
 * used by EDDTable. The outputStream isn't obtained until the first call to writeSome().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-24
 */
public class TableWriterSeparatedValue extends TableWriter {

  // set by constructor
  protected String separator;
  protected boolean twoQuotes, writeColumnNames;
  protected char writeUnits;
  protected String nanString;

  // set by firstTime
  protected volatile boolean isStringOrChar[];
  protected volatile boolean isTimeStamp[];
  protected volatile String time_precision[];
  protected volatile BufferedWriter writer;

  public volatile AtomicLong totalNRows = new AtomicLong(0);

  /**
   * The constructor.
   *
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   * @param tSeparator usually a tab or a comma (without space. space causes problems with Excel)
   * @param tTwoQuotes if true, internal double quotes become two double quotes.
   * @param tWriteColumnNames if false, data starts on line 0.
   * @param tWriteUnits '0'=no, '('=on the first line as "variableName (units)" (if present), 2=on
   *     the second line.
   * @param tNanString the String to use for missing numeric values (e.g., "NaN" or ""). ERDDAP .csv
   *     and .tsv use "NaN" because they did since the beginning and because it is easier/safer to
   *     replace "NaN" with "" than replace nothing with "NaN".
   */
  public TableWriterSeparatedValue(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      OutputStreamSource tOutputStreamSource,
      String tSeparator,
      boolean tTwoQuotes,
      boolean tWriteColumnNames,
      char tWriteUnits,
      String tNanString) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
    separator = tSeparator;
    twoQuotes = tTwoQuotes;
    writeColumnNames = tWriteColumnNames;
    writeUnits = tWriteUnits;
    nanString = tNanString;
    if ("0(2".indexOf(writeUnits) < 0)
      throw new SimpleException(
          "Internal error: TableWriterSeparatedValue writeUnits="
              + writeUnits
              + " must be 0, (, or 2.");
  }

  /**
   * This adds the current contents of table (a chunk of data) to the OutputStream. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., call OutputStreamSource.outputStream() and write file header). The
   * number of columns, the column names, and the types of columns must be the same each time this
   * is called.
   *
   * @param table with destinationValues The table should have missing values stored as
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

    int nColumns = table.nColumns();
    PrimitiveArray pas[] = new PrimitiveArray[nColumns];
    for (int col = 0; col < nColumns; col++) pas[col] = table.getColumn(col);

    // do firstTime stuff
    if (firstTime) {
      isTimeStamp = new boolean[nColumns];
      time_precision = new String[nColumns];
      for (int col = 0; col < nColumns; col++) {
        Attributes catts = table.columnAttributes(col);
        String u = catts.getString("units");
        isTimeStamp[col] = u != null && (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
        if (isTimeStamp[col]) {
          // just keep time_precision if it includes fractional seconds
          String tp = catts.getString(EDV.TIME_PRECISION);
          if (tp != null && !tp.startsWith("1970-01-01T00:00:00.0")) tp = null; // default
          time_precision[col] = tp;
        }
      }

      // write the header
      writer = File2.getBufferedWriter88591(outputStreamSource.outputStream(File2.ISO_8859_1));

      // write the column names
      isStringOrChar = new boolean[nColumns];
      for (int col = 0; col < nColumns; col++) {
        isStringOrChar[col] =
            pas[col].elementType() == PAType.STRING || pas[col].elementType() == PAType.CHAR;

        if (writeColumnNames) {
          String units = "";
          if (writeUnits == '[' || writeUnits == '(') {
            units = table.columnAttributes(col).getString("units");
            if (units == null) units = "";
            if (isTimeStamp[col]) units = "UTC"; // "seconds since 1970-01-01..." is no longer true
            if (units.length() > 0) {
              if (writeUnits == '[') units = " [" + units + "]";
              else units = " (" + units + ")";
            }
          }

          // convert troublesome chars to \\encoding
          String s = String2.toSVString(table.getColumnName(col) + units, 127);
          if (twoQuotes) s = String2.replaceAll(s, "\\\"", "\"\"");
          writer.write(s + (col == nColumns - 1 ? "\n" : separator));
        }
      }

      // write the units on 2nd line of header
      if (writeColumnNames && writeUnits == '2') {
        for (int col = 0; col < nColumns; col++) {
          String units = table.columnAttributes(col).getString("units");
          if (units == null) units = "";
          if (isTimeStamp[col]) units = "UTC"; // no longer true: "seconds since 1970-01-01..."
          String s = String2.toSVString(units, 127);
          if (twoQuotes) s = String2.replaceAll(s, "\\\"", "\"\"");
          writer.write(s + (col == nColumns - 1 ? "\n" : separator));
        }
      }
    }

    // *** do everyTime stuff
    table.convertToStandardMissingValues(); // to NaNs

    // avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
    int nRows = table.nRows();
    boolean flushAfterward =
        totalNRows.get() == 0; // flush initial chunk so info gets to user quickly
    totalNRows.addAndGet(nRows);
    Math2.ensureArraySizeOkay(totalNRows.get(), "Separated Value");

    // write the data
    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nColumns; col++) {
        if (isTimeStamp[col]) {
          writer.write(
              Calendar2.epochSecondsToLimitedIsoStringT(
                  time_precision[col], pas[col].getDouble(row), ""));
        } else if (isStringOrChar[col]) {
          String s = pas[col].getSVString(row);
          if (twoQuotes) s = String2.replaceAll(s, "\\\"", "\"\"");
          writer.write(s);
        } else {
          String s = pas[col].getString(row);
          writer.write(s.length() == 0 ? nanString : s);
        }
        writer.write(col == nColumns - 1 ? "\n" : separator);
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

    writer.flush(); // essential

    // diagnostic
    if (verbose)
      String2.log(
          "TableWriterSeparatedValue done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
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
      OutputStreamSource tOutputStreamSource,
      String tSeparator,
      boolean tTwoQuotes,
      boolean tWriteColumnNames,
      char tWriteUnits,
      String tNanString)
      throws Throwable {

    TableWriterSeparatedValue twsv =
        new TableWriterSeparatedValue(
            language,
            tEdd,
            tNewHistory,
            tOutputStreamSource,
            tSeparator,
            tTwoQuotes,
            tWriteColumnNames,
            tWriteUnits,
            tNanString);
    twsv.writeAllAndFinish(table);
  }
}
