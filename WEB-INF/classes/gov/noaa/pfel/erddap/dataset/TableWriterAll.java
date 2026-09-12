/*
 * TableWriterAll Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.BufferedFileChannel;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.DataInputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * TableWriterAll provides a way to write a table to a series of BufferedFileChannels (one per
 * column) in chunks so that the whole table is available but doesn't have to be in memory at one
 * time. This is used by EDDTable.
 *
 * <p>This is different from most TableWriters in that finish() doesn't write the data anywhere (to
 * an outputStream or to another tableWriter), it just makes all of the data available.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-23
 */
public class TableWriterAll extends TableWriter {
  public static final String attributeTo = "gathering data in TableWriterAll";

  protected final int randomInt = Math2.random(Integer.MAX_VALUE);

  // set by constructor
  protected final String dir;
  protected final String fileNameNoExt;

  // set firstTime
  // POLICY: because this class may be used in more than one thread,
  // each instance makes unique temp files names by adding randomInt to name.
  protected volatile BufferedFileChannel[] columnStreams;
  protected volatile long totalNRows = 0;

  protected Table cumulativeTable; // set by writeAllAndFinish, if used
  private final CleanupTableWriterAction cleanupAction;

  /**
   * The constructor. TableWriterAll will create several temporary files using the dir+name as the
   * starting point. TableWriterAll will delete all of the files when garbage-collected.
   *
   * @param tDir a private cache directory for storing the intermediate files, usually
   *     cacheDirectory(datasetID)
   * @param tFileNameNoExt is the fileName-safe fileName without dir or extension (used as basis for
   *     temp files). A random number will be added to it for safety.
   */
  public TableWriterAll(
      int tLanguage, EDD tEdd, String tNewHistory, String tDir, String tFileNameNoExt) {
    super(tLanguage, tEdd, tNewHistory, null);
    dir = File2.addSlash(tDir);
    // Normally, this is cacheDirectory and it already exists,
    //  but my testing environment (2+ things running) may have removed it.
    File2.makeDirectory(dir);
    fileNameNoExt = tFileNameNoExt;
    cleanupAction = new CleanupTableWriterAction(dir, fileNameNoExt, randomInt);
    EDStatic.cleaner.register(this, cleanupAction);
  }

  public static String sanitizePath(String relativeOrFullPath, String baseDir)
      throws SecurityException {
    try {
      Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
      Path targetPath = basePath.resolve(relativeOrFullPath).toAbsolutePath().normalize();

      if (!targetPath.startsWith(basePath)) {
        throw new SecurityException(
            String2.ERROR + " in sanitizePath: Path traversal outside base directory");
      }
      return targetPath.toString();
    } catch (Exception e) {
      throw new SecurityException(
          String2.ERROR + " in sanitizePath: Invalid path " + relativeOrFullPath, e);
    }
  }

  private static final class CleanupTableWriterAction implements Runnable {

    private BufferedFileChannel[] columnStreams;
    private String[] columnNames;
    private final String dir;
    private final String fileNameNoExt;
    private final int randomInt;

    private CleanupTableWriterAction(String dir, String fileNameNoExt, int randomInt) {
      this.dir = dir;
      this.fileNameNoExt = fileNameNoExt;
      this.randomInt = randomInt;
    }

    private void setColumnStreams(BufferedFileChannel[] columnStreams) {
      this.columnStreams = columnStreams;
    }

    @Override
    public void run() {
      try {
        if (columnStreams != null) {
          for (int col = 0; col < columnStreams.length; col++) {
            try {
              if (columnStreams[col] != null) {
                columnStreams[col].flush();
                columnStreams[col].close();
              }
            } catch (Exception e) {
            }
            columnStreams[col] = null;
          }
          columnStreams = null;
        }

        if (columnNames == null) return;
        for (String columnName : columnNames) {
          File2.simpleDelete(
              dir
                  + fileNameNoExt
                  + "."
                  + randomInt
                  + "."
                  + String2.encodeFileNameSafe(columnName)
                  + ".temp");
        }
      } catch (Throwable t) {
        String2.log("TableWriterAll.releaseResources caught:\n" + MustBe.throwableToString(t));
      }
    }

    private void setColumnNames(String[] columnNames) {
      this.columnNames = columnNames;
    }
  }

  @Override
  public void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;

    boolean firstTime = columnNames == null;
    ensureCompatible(table);

    int nColumns = table.nColumns();
    if (firstTime) {
      columnStreams = new BufferedFileChannel[nColumns];
      cleanupAction.setColumnStreams(columnStreams);
      cleanupAction.setColumnNames(columnNames);
      for (int col = 0; col < nColumns; col++) {
        String tFileName = columnFileName(col);
        String sanitizedFileName = sanitizePath(tFileName, dir);
        FileChannel fc =
            FileChannel.open(
                Paths.get(sanitizedFileName),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        columnStreams[col] = new BufferedFileChannel(fc);
        if (col == 0 && reallyVerbose)
          String2.log(
              "TableWriterAll nColumns="
                  + nColumns
                  + " colNames="
                  + table.getColumnNamesCSVString()
                  + " col0 file="
                  + tFileName);
      }
    }

    long newTotalNRows = totalNRows + table.nRows();
    Math2.ensureArraySizeOkay(newTotalNRows, attributeTo);
    Math2.ensureMemoryAvailable(newTotalNRows * 8, attributeTo);
    Math2.ensureDiskAvailable(newTotalNRows * 8, EDStatic.config.fullCacheDirectory, attributeTo);

    for (int col = 0; col < nColumns; col++) {
      Test.ensureNotNull(
          columnStreams[col], "columnStreams[" + col + "] is null! nColumns=" + nColumns);
      PrimitiveArray pa = table.getColumn(col);
      pa.writeToChannel(columnStreams[col]);
    }
    totalNRows = newTotalNRows;
  }

  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    if (columnStreams == null) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");
    for (int col = 0; col < columnStreams.length; col++) {
      try {
        if (columnStreams[col] != null) {
          columnStreams[col].flush();
          columnStreams[col].close();
        }
      } catch (Exception e) {
      }
      columnStreams[col] = null;
    }
    columnStreams = null;

    if (verbose)
      String2.log("TableWriterAll done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  public static void readColumnChunk(
      int col, FileChannel channel, PrimitiveArray destBuffer, long startRow, int maxRows)
      throws Exception {
    if (channel == null || destBuffer == null || maxRows <= 0 || startRow < 0) {
      return;
    }
    if (destBuffer instanceof com.cohort.array.StringArray sa) {
      if (startRow == 0) {
        channel.position(0);
      } else if (channel.position() == 0) {
        DataInputStream dis =
            new DataInputStream(java.nio.channels.Channels.newInputStream(channel));
        for (long i = 0; i < startRow; i++) {
          dis.readUTF();
        }
      }
      DataInputStream dis = new DataInputStream(java.nio.channels.Channels.newInputStream(channel));
      try {
        for (int i = 0; i < maxRows; i++) {
          sa.add(dis.readUTF());
        }
      } catch (java.io.EOFException eof) {
      }
    } else {
      int elementSize = destBuffer.elementSize();
      long byteOffset = startRow * (long) elementSize;
      if (byteOffset >= channel.size()) {
        return;
      }
      channel.position(byteOffset);
      long remainingBytes = channel.size() - byteOffset;
      long availableRows = remainingBytes / elementSize;
      int rowsToRead = (int) Math.min(maxRows, Math.max(0, availableRows));
      if (rowsToRead > 0) {
        destBuffer.readFromChannel(channel, rowsToRead);
      }
    }
  }

  public PrimitiveArray column(int col) throws Throwable {
    if (cumulativeTable != null) return cumulativeTable.getColumn(col);

    Math2.ensureArraySizeOkay(totalNRows, "TableWriterAll");
    PrimitiveArray pa = PrimitiveArray.factory(columnType(col), (int) totalNRows, false);
    pa.setMaxIsMV(columnMaxIsMV[col]);
    String tFileName = columnFileName(col);
    String sanitizedFileName = sanitizePath(tFileName, dir);
    try (FileChannel channel =
        FileChannel.open(Paths.get(sanitizedFileName), StandardOpenOption.READ)) {
      readColumnChunk(col, channel, pa, 0, (int) totalNRows);
    }
    return pa;
  }

  public PrimitiveArray columnEmptyPA(int col) {
    return PrimitiveArray.factory(columnType(col), 1, false).setMaxIsMV(columnMaxIsMV[col]);
  }

  public FileChannel openColumnChannel(int col) throws Exception {
    String tFileName = columnFileName(col);
    String sanitizedFileName = sanitizePath(tFileName, dir);
    return FileChannel.open(Paths.get(sanitizedFileName), StandardOpenOption.READ);
  }

  public String columnFileName(int col) {
    return fileNameNoExt
        + "."
        + randomInt
        + "."
        + String2.encodeFileNameSafe(columnNames[col])
        + ".temp";
  }

  public long nRows() {
    return totalNRows;
  }

  public void ensureMemoryForCumulativeTable() {
    Table table = makeEmptyTable();
    Math2.ensureMemoryAvailable(
        nColumns() * nRows() * table.estimatedBytesPerRow(), "TableWriterAll.cumulativeTable");
  }

  public Table cumulativeTable() throws Throwable {
    if (cumulativeTable != null) return cumulativeTable;

    Table table = makeEmptyTable();

    int nColumns = nColumns();
    ensureMemoryForCumulativeTable();

    for (int col = 0; col < nColumns; col++) table.setColumn(col, column(col));

    cumulativeTable = table;
    return cumulativeTable;
  }

  public void releaseResources() {
    try {
      cumulativeTable = null;

      if (columnStreams != null) {
        for (int col = 0; col < columnStreams.length; col++) {
          try {
            if (columnStreams[col] != null) {
              columnStreams[col].flush();
              columnStreams[col].close();
            }
          } catch (Exception e) {
          }
          columnStreams[col] = null;
        }
        columnStreams = null;
      }

      if (columnNames == null) return;
      int nColumns = nColumns();
      for (int col = 0; col < nColumns; col++) {
        File2.simpleDelete(sanitizePath(columnFileName(col), dir));
      }
    } catch (Throwable t) {
      String2.log("TableWriterAll.releaseResources caught:\n" + MustBe.throwableToString(t));
    }
  }

  @Override
  public void close() throws Exception {
    releaseResources();
  }
}
