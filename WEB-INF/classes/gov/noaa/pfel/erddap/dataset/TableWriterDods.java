/*
 * TableWriterDods Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PrimitiveArray;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.io.DataOutputStream;
import java.io.OutputStream;

/**
 * TableWriterDods provides a way to write a table to a DAP .dods format (1-level sequence) (see
 * www.opendap.org, DAP 2.0, 7.2.3) outputStream in chunks so that the whole table doesn't have to
 * be in memory at one time. This is used by EDDTable. The outputStream isn't obtained until the
 * first call to writeSome().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-24
 */
public class TableWriterDods extends TableWriter {

  // set by constructor
  protected String sequenceName;

  // set by firstTime
  protected DataOutputStream dos;

  public long totalNRows = 0;

  /**
   * The constructor.
   *
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   * @param tSequenceName e.g., "erd_opendap_globec_bottle"
   */
  public TableWriterDods(
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

    // do firstTime stuff
    int nColumns = table.nColumns();
    if (firstTime) {

      // write the dds    //DAP 2.0, 7.2.3
      OutputStream outputStream = outputStreamSource.outputStream("");
      table.saveAsDDS(outputStream, sequenceName);

      // write the connector  //DAP 2.0, 7.2.3
      // see OpendapHelper.EOL for comments
      outputStream.write((OpendapHelper.EOL + "Data:" + OpendapHelper.EOL).getBytes());

      dos = new DataOutputStream(outputStream);
    }

    // do everyTime stuff
    // leave missing values as destinationMissingValues or destinationFillValues

    // avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
    int nRows = table.nRows();
    totalNRows += nRows;
    Math2.ensureArraySizeOkay(totalNRows, "DODS sequence");

    // write the data  //DAP 2.0, 7.3.2.3
    // write elements of the sequence, in dds order
    PrimitiveArray pas[] = new PrimitiveArray[nColumns];
    for (int col = 0; col < nColumns; col++) pas[col] = table.getColumn(col);

    for (int row = 0; row < nRows; row++) {
      dos.writeInt(0x5A << 24); // start of instance
      for (int col = 0; col < nColumns; col++) pas[col].externalizeForDODS(dos, row);
    }

    // so data gets to user right away
    dos.flush();
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
    if (dos == null) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    // end of data
    dos.writeInt(0xA5 << 24); // end of sequence; so if nRows=0, this is all that is sent
    dos.flush(); // essential

    // diagnostic
    if (verbose)
      String2.log("TableWriterDods done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
