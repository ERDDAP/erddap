/*
 * TableWriterDodsAscii Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.io.OutputStream;
import java.io.Writer;

/**
 * TableWriterDodsAscii provides a way to write a table to a DAP .asc format (see www.opendap.org,
 * DAP 2.0, 7.2.3) outputStream in chunks so that the whole table doesn't have to be in memory at
 * one time. This is used by EDDTable. The outputStream isn't obtained until the first call to
 * writeSome().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-24
 */
public class TableWriterDodsAscii extends TableWriter {

  // set by constructor
  protected String sequenceName;

  // set by firstTime
  protected boolean isCharOrString[];
  protected Writer writer;

  public long totalNRows = 0;

  /**
   * The constructor.
   *
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   * @param tSequenceName e.g., "erd_opendap_globec_bottle"
   */
  public TableWriterDodsAscii(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      OutputStreamSource tOutputStreamSource,
      String tSequenceName) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
    sequenceName = tSequenceName;
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
  public void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;

    // ensure the table's structure is the same as before
    boolean firstTime = columnNames == null;
    ensureCompatible(table);

    int nColumns = table.nColumns();
    PrimitiveArray pas[] = new PrimitiveArray[nColumns];
    for (int col = 0; col < nColumns; col++) pas[col] = table.getColumn(col);

    // do firstTime stuff
    int nRows = table.nRows();
    if (firstTime) {

      // write the dds    //DAP 2.0, 7.2.3
      OutputStream outputStream = outputStreamSource.outputStream(File2.ISO_8859_1);
      table.saveAsDDS(outputStream, sequenceName);

      // see OpendapHelper.EOL for comments
      writer =
          File2.getBufferedWriter88591(
              outputStream); // DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for
      // compatible common 8bit
      writer.write(
          "---------------------------------------------"
              + OpendapHelper.EOL); // this exactly mimics the example

      // write the column names
      isCharOrString = new boolean[nColumns];
      for (int col = 0; col < nColumns; col++) {
        isCharOrString[col] =
            pas[col].elementType() == PAType.CHAR || pas[col].elementType() == PAType.STRING;
        writer.write(
            sequenceName
                + "."
                + table.getColumnName(col)
                + (col == nColumns - 1 ? OpendapHelper.EOL : ", "));
      }
    }

    // do everyTime stuff
    // leave missing values as destinationMissingValues or destinationFillValues

    // avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
    boolean flushAfterward = totalNRows == 0; // flush initial chunk so info gets to user quickly
    totalNRows += nRows;
    Math2.ensureArraySizeOkay(totalNRows, "DODS Ascii sequence");

    // write the data  //DAP 2.0, 7.3.2.3
    // write elements of the sequence, in dds order
    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nColumns; col++) {
        String s =
            pas[col].getRawestString(
                row); // so Int.MAX_VALUE appears as the value and Double.NaN appears as NaN
        if (isCharOrString[col]) {
          // see DODS Appendix A, quoted-string, with \\ and \"
          // 2020-03-26 was just those 2 encoded chars. Now, I assume it implies json-like encoding
          // of special chars.
          s = String2.toJson(s);
        }
        writer.write(s);
        writer.write(col == nColumns - 1 ? OpendapHelper.EOL : ", ");
      }
    }

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

    // end of data
    writer.flush(); // essential

    // diagnostic
    if (verbose)
      String2.log(
          "TableWriterDodsAscii done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
