/*
 * GridDataRandomAccessorInMemory Copyright 2020, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Math2;
import gov.noaa.pfel.erddap.variable.EDV;

/**
 * This class provides random access to the grid data requested by a grid data query to an EDDGrid.
 * It first stores all of the data in memory. This does work for String data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2020-06-18
 */
public class GridDataRandomAccessorInMemory {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  // things the constructor generates
  protected NDimensionalIndex gdaTotalIndex;
  protected PAType dataPAType[]; // 1 per data variable
  protected PrimitiveArray dataPA[]; // 1 per data variable

  /**
   * This sets everything up (i.e., gets all the data and stores it in memory).
   *
   * @param tGridDataAccessor a rowMajor gridDataAccessor
   * @throws Throwable if trouble
   */
  public GridDataRandomAccessorInMemory(GridDataAccessor gridDataAccessor) throws Throwable {
    try {
      if (!gridDataAccessor.rowMajor())
        throw new RuntimeException(
            "GridDataRandomAccessorInMemory.constructor requires the gridDataAccessor to be rowMajor.");

      // make the dataPA's
      EDV dataVars[] = gridDataAccessor.dataVariables();
      int nDv = dataVars.length;
      dataPAType = new PAType[nDv];
      dataPA = new PrimitiveArray[nDv];
      PAOne tPAOne[] = new PAOne[nDv];
      String tQuery = gridDataAccessor.userDapQuery();
      long longCapacity = gridDataAccessor.totalIndex().size();
      if (longCapacity >= Integer.MAX_VALUE)
        throw new RuntimeException(
            "GridDataRandomAccessorInMemory.constructor requires totalIndex.size="
                + longCapacity
                + " to be <"
                + Integer.MAX_VALUE
                + ".");
      int capacity = Math2.narrowToInt(longCapacity);
      for (int dv = 0; dv < nDv; dv++) {
        dataPAType[dv] = dataVars[dv].destinationDataPAType();
        dataPA[dv] = PrimitiveArray.factory(dataPAType[dv], capacity, false); // active?
        tPAOne[dv] = new PAOne(dataPAType[dv]);
      }

      // get all the data
      while (gridDataAccessor.increment()) {
        for (int dv = 0; dv < nDv; dv++)
          dataPA[dv].addPAOne(gridDataAccessor.getDataValueAsPAOne(dv, tPAOne[dv]));
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
   * @param paOne which will receive the value
   * @return the PAOne (for convenience)
   * @param throws Throwable if trouble
   */
  public PAOne getDataValueAsPAOne(int current[], int dv, PAOne paOne) throws Throwable {
    // index will be int -- constructor checked totalIndex.size < Integer.MAX_VALUE
    return paOne.readFrom(dataPA[dv], (int) gdaTotalIndex.setCurrent(current));
  }
}
