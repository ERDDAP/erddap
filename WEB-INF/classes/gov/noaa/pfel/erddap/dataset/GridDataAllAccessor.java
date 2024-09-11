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
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

/**
 * This class gets all of the grid data requested by a grid data query to an EDDGrid and makes it
 * accessible one variable at a time as a PrimitiveArray or DataInputStream. This works with all
 * data types (even Strings).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2010-09-03
 */
public class GridDataAllAccessor {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  // things passed into the constructor
  protected GridDataAccessor gridDataAccessor;

  // things the constructor sets
  protected String baseFileName; // to which the dv number is added
  protected PAType dataPAType[]; // 1 per data variable  e.g., float.class

  /**
   * This sets everything up (i.e., gets all the data and stores it in Files).
   *
   * @param tGridDataAccessor a rowMajor gridDataAccessor
   * @throws Throwable if trouble
   */
  public GridDataAllAccessor(GridDataAccessor tGridDataAccessor) throws Throwable {

    int nDv = 0;
    DataOutputStream dos[] = null;
    gridDataAccessor = tGridDataAccessor;
    try {
      if (!gridDataAccessor.rowMajor())
        throw new Exception(
            "GridDataAllAccessor.constructor requires the gridDataAccessor to be rowMajor.");

      // make the dataFiles
      // This is set up to delete the cached files when the creator/owner is done.
      // It could be changed to keep the files in the cache (which is cleared periodically).
      EDV dataVars[] = gridDataAccessor.dataVariables();
      nDv = dataVars.length;
      String tQuery = gridDataAccessor.userDapQuery();
      baseFileName =
          gridDataAccessor.eddGrid().cacheDirectory()
              + // dir created by EDD.ensureValid
              String2.md5Hex12(tQuery == null ? "" : tQuery)
              + "_"
              + Math2.random(Integer.MAX_VALUE)
              + "_"; // so two identical queries don't interfere with each other

      dataPAType = new PAType[nDv];
      dos = new DataOutputStream[nDv]; // 1 per data variable
      for (int dv = 0; dv < nDv; dv++) {
        dataPAType[dv] = dataVars[dv].destinationDataPAType();
        dos[dv] =
            new DataOutputStream(new BufferedOutputStream(new FileOutputStream(baseFileName + dv)));
      }

      // get all the data
      while (gridDataAccessor.incrementChunk()) {
        for (int dv = 0; dv < nDv; dv++) {
          gridDataAccessor.getPartialDataValues(dv).writeDos(dos[dv]);
        }
      }
    } finally {
      if (dos != null) {
        for (int dv = 0; dv < nDv; dv++)
          try {
            if (dos[dv] != null) dos[dv].close();
          } catch (Exception e) {
          }
      }
      gridDataAccessor.releaseGetResources();
    }
  }

  /**
   * Get all of the destination values for one dataVariable as a DataInputStream. IT IS THE CALLERS
   * RESPONSIBILITY TO CLOSE THESE!
   *
   * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
   * @return a DataInputStream
   * @param throws RuntimeException if trouble
   */
  public DataInputStream getDataInputStream(int dv) throws Exception {
    return new DataInputStream(File2.getDecompressedBufferedInputStream(baseFileName + dv));
  }

  /* Future: This could allow access to all dataStreams in a
    synchronous way (e.g., the first value for all dv, then the second, ...).
  */

  /**
   * Get all of the destination values for one dataVariable as a PrimitiveArray. Note that may
   * require a lot of memory!
   *
   * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
   * @return a PrimitiveArray
   * @param throws RuntimeException if trouble, e.g., if gdaTotalIndex.size() is &gt;=
   *     Integer.MAX_VALUE.
   */
  public PrimitiveArray getPrimitiveArray(int dv) throws Exception {
    long n = gridDataAccessor.totalIndex.size();
    Math2.ensureArraySizeOkay(n, "GridDataAllAccessor");
    PrimitiveArray pa = PrimitiveArray.factory(dataPAType[dv], (int) n, false);
    DataInputStream dis = getDataInputStream(dv);
    try {
      pa.readDis(dis, (int) n);
    } finally {
      dis.close();
    }
    return pa;
  }

  public void releaseGetResources() {
    try {
      if (gridDataAccessor != null) gridDataAccessor.releaseGetResources();
    } catch (Throwable t) {
    }
  }

  /**
   * This releases all resources (e.g., files and threads). It is recommended, but not required,
   * that users of this class call this when they are done using this instance. This won't throw an
   * Exception.
   */
  public void releaseResources() {
    releaseGetResources();
    try {
      if (dataPAType != null) {
        int nDv = dataPAType.length;
        for (int dv = 0; dv < nDv; dv++) {
          try {
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

  /**
   * Users of this class shouldn't call this -- use releaseResources() instead. Java calls this when
   * an object is no longer used, just before garbage collection.
   */
  protected void finalize() throws Throwable {
    releaseResources();
    super.finalize();
  }
}
