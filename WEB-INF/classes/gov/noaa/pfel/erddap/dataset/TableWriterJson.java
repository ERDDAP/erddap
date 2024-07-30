/*
 * TableWriterJson Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TableWriterJson provides a way to write a table to JSON (https://www.json.org/) outputStream in
 * chunks so that the whole table doesn't have to be in memory at one time. This is used by
 * EDDTable. The outputStream isn't obtained until the first call to write().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-24
 */
public class TableWriterJson extends TableWriter {

  // set by constructor
  protected String jsonp;
  protected boolean writeUnits;

  // set by firstTime
  protected volatile boolean isTimeStamp[];
  protected volatile String time_precision[];
  protected volatile BufferedWriter writer;

  // other
  protected volatile boolean rowsWritten = false;
  public volatile AtomicLong totalNRows = new AtomicLong(0);

  /**
   * The constructor.
   *
   * @param language the index of the selected language
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   * @param tJsonp the not-percent-encoded jsonp functionName to be prepended to the results (or
   *     null if none). See https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/ and
   *     https://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/ and
   *     https://www.raymondcamden.com/2014/03/12/Reprint-What-in-the-heck-is-JSONP-and-why-would-you-use-it/
   *     . A SimpleException will be thrown if tJsonp is not null but isn't
   *     String2.isVariableNameSafe.
   * @param tWriteUnits if true, the units information will be written to the file
   */
  public TableWriterJson(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      OutputStreamSource tOutputStreamSource,
      String tJsonp,
      boolean tWriteUnits) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
    jsonp = tJsonp;
    if (jsonp != null && !String2.isJsonpNameSafe(jsonp))
      throw new SimpleException(
          EDStatic.bilingual(language, EDStatic.queryErrorAr, EDStatic.errorJsonpFunctionNameAr));
    writeUnits = tWriteUnits;
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
   *     NaNs and stores them as nulls.
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
      writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));
      if (jsonp != null) writer.write(jsonp + "(");

      // write the column names
      writer.write(
          "{\n"
              + "  \"table\": {\n"
              + // begin main structure
              "    \"columnNames\": [");
      for (int col = 0; col < nColumns; col++) {
        writer.write(String2.toJson(table.getColumnName(col)));
        writer.write(col == nColumns - 1 ? "],\n" : ", ");
      }

      // write the types
      writer.write("    \"columnTypes\": [");
      for (int col = 0; col < nColumns; col++) {
        String s = pas[col].elementTypeString();
        if (isTimeStamp[col]) s = "String"; // not "double"
        writer.write(String2.toJson(s)); // nulls written as: null
        writer.write(col == nColumns - 1 ? "],\n" : ", ");
      }

      // write the units
      if (writeUnits) {
        writer.write("    \"columnUnits\": [");
        for (int col = 0; col < nColumns; col++) {
          String s = table.columnAttributes(col).getString("units");
          if (isTimeStamp[col]) s = "UTC"; // no longer true: "seconds since 1970-01-01..."
          writer.write(String2.toJson(s)); // nulls written as: null
          writer.write(col == nColumns - 1 ? "],\n" : ", ");
        }
      }

      writer.write("    \"rows\": [\n");
    }

    // *** do everyTime stuff
    // String2.log(">> getNCHeader of chunk nRows=" + table.nRows() + ":\n" +
    // table.getNCHeader("row"));
    table.convertToStandardMissingValues(); // to NaNs

    // avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
    int nRows = table.nRows();
    boolean flushAfterward =
        totalNRows.get() == 0; // flush initial chunk so info gets to user quickly
    totalNRows.addAndGet(nRows);
    Math2.ensureArraySizeOkay(totalNRows.get(), "json");

    // write the data
    if (rowsWritten) writer.write(",\n"); // end previous row
    for (int row = 0; row < nRows; row++) {
      writer.write("      ["); // beginRow
      for (int col = 0; col < nColumns; col++) {
        if (col > 0) writer.write(", ");
        if (isTimeStamp[col]) {
          double d = pas[col].getDouble(row);
          writer.write(
              Double.isNaN(d)
                  ? "null"
                  : "\""
                      + Calendar2.epochSecondsToLimitedIsoStringT(time_precision[col], d, "")
                      + "\"");
        } else {
          writer.write(pas[col].getJsonString(row));
        }
      }
      writer.write(row < nRows - 1 ? "],\n" : "]"); // endRow
    }
    if (nRows > 0) rowsWritten = true;

    if (flushAfterward) writer.flush();
  }

  /**
   * This writes any end-of-file info to the stream and flushes the stream. If ignoreFinish=true,
   * nothing will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    // check for MustBe.THERE_IS_NO_DATA
    if (writer == null) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    // end of big array
    writer.write(
        "\n" + "    ]\n" + // end of rows array
            "  }\n" + // end of table
            "}\n"); // end of main structure
    if (jsonp != null) writer.write(")");
    writer.flush(); // essential

    // diagnostic
    if (verbose)
      String2.log("TableWriterJson done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
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
      OutputStreamSource outputStreamSource,
      String tJsonp,
      boolean writeUnits)
      throws Throwable {

    TableWriterJson twj =
        new TableWriterJson(language, tEdd, tNewHistory, outputStreamSource, tJsonp, writeUnits);
    twj.writeAllAndFinish(table);
  }
}
