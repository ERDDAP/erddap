/*
 * GridDataRandomAccessor Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.RandomAccessFile;

/**
 * This class provides random access to the grid data requested by a grid data query to an EDDGrid.
 * It first stores all of the data in RandomAccessFiles. Then it allows random access to those
 * files. This doesn't work for String data (the Strings are treated as doubles internally, so
 * become the values from String2.parseDouble(s)).
 *
 * <p>[FUTURE: This could be revised to work with Strings: 1) make isString[nDv] 2) store isString
 * columns to 2 files: bytes byte[] with utf8 bytes from the string, appended one after another
 * (StringArray.writeDos) offset long[] with offset of end of string. (beginning of first string is
 * offset=0) 3) add public String getDataValueAsPAOne(int current[], int dv) which uses a variant of
 * StringArray.readDis (but from RandomAccessFile). ]
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-07-06
 */
public class GridDataRandomAccessor {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  // things the constructor generates
  protected NDimensionalIndex gdaTotalIndex;
  protected String rafName;
  protected PAType dataPAType[]; // 1 per data variable
  protected RandomAccessFile dataRaf[]; // 1 per data variable

  /**
   * This sets everything up (i.e., gets all the data and stores it in RandomAccessFiles).
   *
   * @param tGridDataAccessor a rowMajor gridDataAccessor
   * @throws Throwable if trouble
   */
  public GridDataRandomAccessor(GridDataAccessor gridDataAccessor) throws Throwable {
    try {
      if (!gridDataAccessor.rowMajor())
        throw new Exception(
            "GridDataRandomAccessor.constructor requires the gridDataAccessor to be rowMajor.");

      // make the dataRaf's
      EDV dataVars[] = gridDataAccessor.dataVariables();
      int nDv = dataVars.length;
      dataRaf = new RandomAccessFile[nDv];
      dataPAType = new PAType[nDv];
      String tQuery = gridDataAccessor.userDapQuery();
      rafName =
          gridDataAccessor.eddGrid().cacheDirectory()
              + // dir created by EDD.ensureValid
              String2.md5Hex12(tQuery == null ? "" : tQuery)
              + "_"
              + Math2.random(Integer.MAX_VALUE)
              + "_";
      for (int dv = 0; dv < nDv; dv++) {
        dataPAType[dv] = dataVars[dv].destinationDataPAType();
        dataRaf[dv] = new RandomAccessFile(rafName + dv, "rw");
      }

      // get all the data
      while (gridDataAccessor.increment()) {
        for (int dv = 0; dv < nDv; dv++)
          gridDataAccessor.writeToRAF(dv, dataRaf[dv]); // this doesn't work for Strings
        // 2020-03-12 was PrimitiveArray.rafWriteDouble( //this doesn't work for Strings
        //   dataRaf[dv], dataPAType[dv], gridDataAccessor.getDataValueAsDouble(dv));
      }
      gdaTotalIndex = gridDataAccessor.totalIndex();
    } finally {
      gridDataAccessor.releaseGetResources();
    }
  }

  /** This returns the PAType of the specified data variable. */
  public PAType dataPAType(int i) {
    return dataPAType[i];
  }

  /**
   * Call this to get the specified (current) data value (as a PAOne) from the specified
   * dataVariable.
   *
   * @param current from gridDataAccessor.totalIndex().getCurrent() (or compatible), but with values
   *     changed to what you want.
   * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
   * @return the PAOne (for convenience)
   * @param throws Throwable if trouble
   */
  public PAOne getDataValueAsPAOne(int current[], int dv, PAOne paOne) throws Throwable {
    return paOne.readFromRAF(dataRaf[dv], 0, gdaTotalIndex.setCurrent(current));
  }

  /**
   * Call this after increment() to get a data value (as a double) from the specified dataVariable
   * and add it to the specified PrimitiveArray.
   *
   * @param current from gridDataAccessor.totalIndex().getCurrent() (or compatible), but with values
   *     changed to what you want.
   * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
   * @param throws Throwable if trouble
   */
  /* public void getDataValue(int current[], int dv, PrimitiveArray pa) throws Throwable {
      gdaTotalIndex.setCurrent(current);
      dataRaf[dv].seek(gdaTotalIndex.getIndex() * (long)pa.elementSize());
      pa.readFromRAF(dataRaf[dv]);
  } */

  /**
   * This closes the files. It is recommended, but not required, that users of this class call this
   * (or closeAndDelete) when they are done using this instance. This is also called by finalize.
   * This won't throw an Exception.
   */
  public void close() {
    // ??? Or don't use random number in rafName,
    // leave files for the fullCacheDirectory cleaner to catch,
    // and reuse the data (File2.touch() the files)
    // if another identical request arrives before files are deleted.
    try {
      if (dataRaf != null) {
        int nDv = dataRaf.length;
        for (int dv = 0; dv < nDv; dv++) {
          try {
            if (dataRaf[dv] != null)
              dataRaf[dv].close(); // will already by closed if this method already called
            dataRaf[dv] = null;
          } catch (Throwable t2) {
            // String2.log(MustBe.throwableToString(t2));
          }
        }
      }
    } catch (Throwable t) {
    }
  }

  /**
   * This closes and deletes all resources. It is recommended, but not required, that users of this
   * class call this (or close) when they are done using this instance. This won't throw an
   * Exception.
   */
  public void releaseResources() {
    close();
    try {
      if (dataRaf != null) {
        int nDv = dataRaf.length;
        for (int dv = 0; dv < nDv; dv++) {
          try {
            File2.delete(rafName + dv);
          } catch (Throwable t2) {
            // String2.log(MustBe.throwableToString(t2));
          }
        }
        dataRaf = null;
      }
    } catch (Throwable t) {
    }
  }

  /**
   * Users of this class shouldn't call this -- use releaseResources() instead. Java calls this when
   * an object is no longer used, just before garbage collection.
   */
  protected void finalize() throws Throwable {
    releaseResources();
    super.finalize();
  }
}
