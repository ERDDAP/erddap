/*
 * GridDataAllAccessor Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.BufferedFileChannel;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.DataInputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * This class gets all of the grid data requested by a grid data query to an EDDGrid and makes it
 * accessible one variable at a time as a PrimitiveArray or DataInputStream. This works with all
 * data types (even Strings).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2010-09-03
 */
public class GridDataAllAccessor implements AutoCloseable {

  public static boolean verbose = false;

  protected final GridDataAccessor gridDataAccessor;

  protected String baseFileName; // to which the dv number is added
  protected PAType dataPAType[]; // 1 per data variable e.g., float.class

  /**
   * This sets everything up (i.e., gets all the data and stores it in Files).
   *
   * @param tGridDataAccessor a rowMajor gridDataAccessor
   * @throws Throwable if trouble
   */
  public GridDataAllAccessor(GridDataAccessor tGridDataAccessor) throws Throwable {

    int nDv = 0;
    BufferedFileChannel channels[] = null;
    gridDataAccessor = tGridDataAccessor;
    try {
      if (!gridDataAccessor.rowMajor())
        throw new Exception(
            "GridDataAllAccessor.constructor requires the gridDataAccessor to be rowMajor.");

      EDV dataVars[] = gridDataAccessor.dataVariables();
      nDv = dataVars.length;
      String tQuery = gridDataAccessor.userDapQuery();
      String baseDir = gridDataAccessor.eddGrid().cacheDirectory();
      baseFileName =
          String2.md5Hex12(tQuery == null ? "" : tQuery)
              + "_"
              + Math2.random(Integer.MAX_VALUE)
              + "_";

      dataPAType = new PAType[nDv];
      channels = new BufferedFileChannel[nDv]; // 1 per data variable
      for (int dv = 0; dv < nDv; dv++) {
        dataPAType[dv] = dataVars[dv].destinationDataPAType();
        String rawPath = baseFileName + dv;
        String sanitizedPath = TableWriterAll.sanitizePath(rawPath, baseDir);
        FileChannel fc =
            FileChannel.open(
                Paths.get(sanitizedPath),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        channels[dv] = new BufferedFileChannel(fc);
      }

      // get all the data
      while (gridDataAccessor.incrementChunk()) {
        for (int dv = 0; dv < nDv; dv++) {
          gridDataAccessor.getPartialDataValues(dv).writeToChannel(channels[dv]);
        }
      }
    } finally {
      if (channels != null) {
        for (int dv = 0; dv < nDv; dv++) {
          try {
            if (channels[dv] != null) {
              channels[dv].flush();
              channels[dv].close();
            }
          } catch (Exception e) {
          }
        }
      }
      gridDataAccessor.close();
    }
  }

  public static void readDataChunk(
      int dv, FileChannel channel, PrimitiveArray destBuffer, long startElement, int maxElements)
      throws Exception {
    if (channel == null || destBuffer == null || maxElements <= 0 || startElement < 0) {
      return;
    }
    if (destBuffer instanceof com.cohort.array.StringArray sa) {
      if (startElement == 0) {
        channel.position(0);
      } else if (channel.position() == 0) {
        DataInputStream dis =
            new DataInputStream(java.nio.channels.Channels.newInputStream(channel));
        for (long i = 0; i < startElement; i++) {
          dis.readUTF();
        }
      }
      DataInputStream dis = new DataInputStream(java.nio.channels.Channels.newInputStream(channel));
      try {
        for (int i = 0; i < maxElements; i++) {
          sa.add(dis.readUTF());
        }
      } catch (java.io.EOFException eof) {
      }
    } else {
      int elementSize = destBuffer.elementSize();
      long byteOffset = startElement * (long) elementSize;
      if (byteOffset >= channel.size()) {
        return;
      }
      channel.position(byteOffset);
      long remainingBytes = channel.size() - byteOffset;
      long availableElements = remainingBytes / elementSize;
      int elementsToRead = (int) Math.min(maxElements, Math.max(0, availableElements));
      if (elementsToRead > 0) {
        destBuffer.readFromChannel(channel, elementsToRead);
      }
    }
  }

  /**
   * Get all of the destination values for one dataVariable as a FileChannel. IT IS THE CALLERS
   * RESPONSIBILITY TO CLOSE THESE!
   *
   * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
   * @return a FileChannel
   */
  public FileChannel openDataChannel(int dv) throws Exception {
    String baseDir = gridDataAccessor.eddGrid().cacheDirectory();
    String rawPath = baseFileName + dv;
    String sanitizedPath = TableWriterAll.sanitizePath(rawPath, baseDir);
    return FileChannel.open(Paths.get(sanitizedPath), StandardOpenOption.READ);
  }

  /**
   * Get all of the destination values for one dataVariable as a PrimitiveArray. Note that may
   * require a lot of memory!
   *
   * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
   * @return a PrimitiveArray
   */
  public PrimitiveArray getPrimitiveArray(int dv) throws Exception {
    long n = gridDataAccessor.totalIndex.size();
    Math2.ensureArraySizeOkay(n, "GridDataAllAccessor");
    PrimitiveArray pa = PrimitiveArray.factory(dataPAType[dv], (int) n, false);
    String baseDir = gridDataAccessor.eddGrid().cacheDirectory();
    String rawPath = baseFileName + dv;
    String sanitizedPath = TableWriterAll.sanitizePath(rawPath, baseDir);
    try (FileChannel channel =
        FileChannel.open(Paths.get(sanitizedPath), StandardOpenOption.READ)) {
      readDataChunk(dv, channel, pa, 0, (int) n);
    }
    return pa;
  }

  public void releaseGetResources() {
    try {
      if (gridDataAccessor != null) gridDataAccessor.close();
    } catch (Throwable t) {
    }
  }

  @Override
  public void close() {
    releaseGetResources();
    try {
      if (dataPAType != null) {
        int nDv = dataPAType.length;
        for (int dv = 0; dv < nDv; dv++) {
          try {
            // Prefer deleting the sanitized full path (baseDir + filename). If that fails,
            // fall back to deleting whatever baseFileName+dv resolves to.
            try {
              String baseDir = null;
              try {
                if (gridDataAccessor != null) baseDir = gridDataAccessor.eddGrid().cacheDirectory();
              } catch (Throwable t3) {
                baseDir = null;
              }
              if (baseDir != null) {
                String sanitized = TableWriterAll.sanitizePath(baseFileName + dv, baseDir);
                File2.delete(sanitized);
                continue;
              }
            } catch (Throwable t4) {
            }
            File2.delete(baseFileName + dv);
          } catch (Throwable t2) {
            String2.log(
                "ERROR in GridDataAllAccessor.deleteFiles: " + MustBe.throwableToString(t2));
          }
        }
        dataPAType = null;
      }
    } catch (Throwable t) {
    }
  }
}
