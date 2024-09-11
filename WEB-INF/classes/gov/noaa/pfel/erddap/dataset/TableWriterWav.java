/*
 * TableWriterWav Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * TableWriterWav provides a way to write a table to a .wav file (see Table.writeWav()) in chunks so
 * that the whole table doesn't have to be in memory at one time. This is used by EDDTable.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2017-09-13
 */
public class TableWriterWav extends TableWriter {

  // set by constructor

  // set by firstTime
  protected volatile int randomInt;
  protected volatile String fullDosName;
  protected volatile DataOutputStream dos;
  protected volatile String fullOutName;
  protected volatile String tClass;
  protected volatile boolean isLong; // if true, save as int (from high 4 bytes)
  public volatile long totalNRows = 0;
  protected volatile int nColumns;

  /**
   * The constructor.
   *
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until finish() is called.
   * @param tempDir for temporary and result files.
   * @param outFileName the unique name (for this request) with extension .wav
   */
  public TableWriterWav(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      OutputStreamSource tOutputStreamSource,
      String tempDir,
      String outFileName) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
    randomInt = Math2.random(Integer.MAX_VALUE);
    fullDosName = tempDir + outFileName + ".dos" + randomInt;
    fullOutName = tempDir + outFileName;
  }

  /**
   * This adds the current contents of table (a chunk of data) to the DataOutputStream. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., call OutputStreamSource.outputStream() and write file header). The
   * number of columns, the column names, and the types of columns must be the same each time this
   * is called.
   *
   * <p>For .wav files, the table should not have missing values.
   *
   * @param table with destinationValues
   * @throws Throwable if trouble
   */
  @Override
  public synchronized void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;

    // ensure the table's structure is the same as before
    boolean firstTime = columnNames == null;
    ensureCompatible(table);

    // do firstTime stuff
    nColumns = table.nColumns();
    if (firstTime) {

      tClass = table.getColumn(0).elementTypeString();
      Test.ensureTrue(
          !tClass.equals("String") && !tClass.equals("char"),
          String2.ERROR + ": For .wav files, all data columns must be numeric.");
      isLong = tClass.equals("long");
      boolean java8 = System.getProperty("java.version").startsWith("1.8.");
      if (java8 && (tClass.equals("float") || tClass.equals("double")))
        throw new SimpleException(
            EDStatic.simpleBilingual(language, EDStatic.queryErrorAr)
                + "Until Java 9, float and double values can't be written to .wav files.");
      for (int col = 0; col < nColumns; col++) {
        Test.ensureEqual(
            tClass,
            table.getColumn(col).elementTypeString(),
            String2.ERROR + ": For .wav files, all data columns must be of the same data type.");
      }

      // create the dos
      dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fullDosName)));
    }

    // *** do everyTime stuff
    // no: convertToStandardMissingValues(table);  //NaNs; not the method in Table, so metadata is
    // unchanged

    int nRows = table.nRows();
    totalNRows += nRows;

    // write the data
    if (isLong) {
      LongArray pas[] = new LongArray[nColumns];
      for (int col = 0; col < nColumns; col++) pas[col] = (LongArray) table.getColumn(col);
      for (int row = 0; row < nRows; row++)
        for (int col = 0; col < nColumns; col++) dos.writeInt((int) (pas[col].array[row] >> 32));
    } else {
      PrimitiveArray pas[] = new PrimitiveArray[nColumns];
      for (int col = 0; col < nColumns; col++) pas[col] = table.getColumn(col);
      for (int row = 0; row < nRows; row++)
        for (int col = 0; col < nColumns; col++) pas[col].writeDos(dos, row);
    }
  }

  /**
   * This writes any end-of-file info to the stream and flush the stream. If ignoreFinish=true,
   * nothing will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public synchronized void finish() throws Throwable {
    if (ignoreFinish) return;
    if (dos != null) dos.close();
    dos = null;

    // check for MustBe.THERE_IS_NO_DATA
    if (totalNRows == 0) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    // write the wav file
    Table.writeWaveFile(
        fullDosName,
        nColumns,
        totalNRows,
        isLong ? "int" : tClass,
        globalAttributes,
        randomInt,
        fullOutName,
        System.currentTimeMillis());
    File2.delete(fullDosName);

    // then send to outputStream
    OutputStream out = outputStreamSource.outputStream(""); // no character_encoding
    try {
      if (!File2.copy(fullOutName, out))
        throw new SimpleException(String2.ERROR + " while transmitting file.");
    } finally {
      try {
        out.close();
      } catch (Exception e) {
      } // downloads of e.g., erddap2.css don't work right if not closed. (just if gzip'd?)
    }

    // diagnostic
    if (verbose)
      String2.log("TableWriterWav done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
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
