/*
 * TableWriterDataTable Copyright 2018, NOAA.
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
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * TableWriterDataTable provides a way to write a table to JSON which follows the conventions of a
 * Google Charts DataTable
 * (https://developers.google.com/chart/interactive/docs/reference#dataparam) outputStream in chunks
 * so that the whole table doesn't have to be in memory at one time. This is used by EDDTable. The
 * outputStream isn't obtained until the first call to write().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-24
 * @author Roland Schweitzer (roland.schweitzer@noaa.gov) May 2018
 */
public class TableWriterDataTable extends TableWriter {

  // set by constructor
  protected boolean writeUnits;

  // set by firstTime
  protected boolean isCharOrString[];
  protected boolean isTimeStamp[];
  protected String time_precision[];
  protected BufferedWriter writer;

  // other
  protected boolean rowsWritten = false;
  public long totalNRows = 0;

  /**
   * The constructor.
   *
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   * @param tWriteUnits if true, the units information will be written to the file
   */
  public TableWriterDataTable(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      OutputStreamSource tOutputStreamSource,
      boolean tWriteUnits) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
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
      writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));
      writer.write("{\"cols\":[");
      isTimeStamp = new boolean[nColumns];
      isCharOrString = new boolean[nColumns];
      time_precision = new String[nColumns];

      time_precision = new String[nColumns];
      for (int col = 0; col < nColumns; col++) {
        Attributes catts = table.columnAttributes(col);
        String u = catts.getString("units");
        isTimeStamp[col] = u != null && (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
        isCharOrString[col] =
            pas[col].elementType() == PAType.CHAR || pas[col].elementType() == PAType.STRING;
        if (isTimeStamp[col]) {
          // just keep time_precision if it includes fractional seconds
          String tp = catts.getString(EDV.TIME_PRECISION);
          if (tp != null && !tp.startsWith("1970-01-01T00:00:00.0")) tp = null; // default
          time_precision[col] = tp;
        }
      }

      for (int col = 0; col < nColumns; col++) {
        Attributes catts = table.columnAttributes(col);
        PAType type = pas[col].elementType();
        String name = table.getColumnName(col);
        String u = catts.getString("units");
        if (col > 0) {
          writer.write(",");
        }
        if (isTimeStamp[col]) {
          writer.write(
              "{\"id\":\""
                  + name
                  + "\",\"label\":\""
                  + name
                  + "\",\"pattern\":\"\",\"type\":\"datetime\"}");
        } else {
          if (type == PAType.STRING) {
            if (writeUnits && u != null) {
              writer.write(
                  "{\"id\":\""
                      + name
                      + "\",\"label\":\""
                      + name
                      + " ("
                      + u
                      + ") "
                      + "\",\"pattern\":\"\",\"type\":\"string\"}");
            } else {
              writer.write(
                  "{\"id\":\""
                      + name
                      + "\",\"label\":\""
                      + name
                      + "\",\"pattern\":\"\",\"type\":\"string\"}");
            }
          } else if (type == PAType.FLOAT) {
            if (writeUnits && u != null) {
              writer.write(
                  "{\"id\":\""
                      + name
                      + "\",\"label\":\""
                      + name
                      + " ("
                      + u
                      + ") "
                      + "\",\"pattern\":\"\",\"type\":\"number\"}");
            } else {
              writer.write(
                  "{\"id\":\""
                      + name
                      + "\",\"label\":\""
                      + name
                      + "\",\"pattern\":\"\",\"type\":\"number\"}");
            }
          } else if (type == PAType.DOUBLE) {
            if (writeUnits && u != null) {
              writer.write(
                  "{\"id\":\""
                      + name
                      + "\",\"label\":\""
                      + name
                      + " ("
                      + u
                      + ") "
                      + "\",\"pattern\":\"\",\"type\":\"number\"}");
            } else {
              writer.write(
                  "{\"id\":\""
                      + name
                      + "\",\"label\":\""
                      + name
                      + "\",\"pattern\":\"\",\"type\":\"number\"}");
            }
          } else { // Assume numeric, will be long at this point
            if (writeUnits && u != null) {
              writer.write(
                  "{\"id\":\""
                      + name
                      + "\",\"label\":\""
                      + name
                      + " ("
                      + u
                      + ") "
                      + "\",\"pattern\":\"\",\"type\":\"number\"}");
            } else {
              writer.write(
                  "{\"id\":\""
                      + name
                      + "\",\"label\":\""
                      + name
                      + "\",\"pattern\":\"\",\"type\":\"number\"}");
            }
          }
        }
      }
      writer.write("],\n\"rows\": [\n");
    }

    // *** do everyTime stuff
    table.convertToStandardMissingValues(); // to NaNs

    // avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
    int nRows = table.nRows();
    boolean flushAfterward = totalNRows == 0; // flush initial chunk so info gets to user quickly
    totalNRows += nRows;
    Math2.ensureArraySizeOkay(totalNRows, "json");

    if (rowsWritten) {
      // Some rows already written. Need a comma.
      writer.write(",");
    }
    // Add the new rows to the data table.
    for (int row = 0; row < nRows; row++) {

      writer.write("{\"c\":[");

      for (int col = 0; col < nColumns; col++) {
        if (col > 0) writer.write(",");
        if (isTimeStamp[col]) {
          double d = pas[col].getDouble(row);

          if (!Double.isNaN(d)) {

            GregorianCalendar gc = Calendar2.epochSecondsToGc(d);

            int year = gc.get(Calendar.YEAR);
            int month = gc.get(Calendar.MONTH);
            int day = gc.get(Calendar.DAY_OF_MONTH);
            int hour = gc.get(Calendar.HOUR_OF_DAY);
            int minute = gc.get(Calendar.MINUTE);
            int second = gc.get(Calendar.SECOND);
            int milli = gc.get(Calendar.MILLISECOND);
            writer.write(
                "{\"v\":\"Date("
                    + year
                    + ", "
                    + month
                    + ", "
                    + day
                    + ", "
                    + hour
                    + ", "
                    + minute
                    + ", "
                    + second
                    + ", "
                    + milli
                    + ")\",\"f\":null}");

          } else {
            String s = pas[col].getString(row);
            writeNumber(s, pas[col].elementType());
          }
        } else if (isCharOrString[col]) {
          String value = pas[col].getString(row);
          writer.write("{\"v\":\"" + value + "\",\"f\":null}");
        } else {
          String s = pas[col].getString(row);
          writeNumber(s, pas[col].elementType());
        }
      }
      if (row < nRows - 1) {
        writer.write("]},\n");
      } else {
        writer.write("]}\n");
      }
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
        "    ]\n" + // end of rows array
            "  }\n");
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
      boolean writeUnits)
      throws Throwable {

    TableWriterDataTable tdt =
        new TableWriterDataTable(language, tEdd, tNewHistory, outputStreamSource, writeUnits);
    tdt.writeAllAndFinish(table);
  }

  protected void writeNumber(String s, PAType elementPAType) throws IOException {
    if (s.length() == 0) {
      writer.write("{\"v\":null,\"f\":null}");
    } else {
      if (elementPAType == PAType.DOUBLE) {
        double dv = Double.valueOf(s).doubleValue();
        writer.write("{\"v\":" + dv + ",\"f\":null}");
      } else if (elementPAType == PAType.FLOAT) {
        float f = Float.valueOf(s).floatValue();
        writer.write("{\"v\":" + f + ",\"f\":null}");
      } else if (elementPAType == PAType.LONG) {
        long f = Long.valueOf(s).longValue();
        writer.write("{\"v\":" + f + ",\"f\":null}");
      } else {
        int f = Integer.valueOf(s).intValue();
        writer.write("{\"v\":" + f + ",\"f\":null}");
      }
    }
  }
}
