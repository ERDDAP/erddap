/* 
 * GridDataAllAccessor Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;


/** 
 * This class gets all of the grid data requested by a 
 * grid data query to an EDDGrid and makes it accessible one variable at a time
 * as a PrimitiveArray or DataInputStream.
 * This works with all data types (even Strings).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2010-09-03
 */
public class GridDataAllAccessor { 

    
    /**
     * Set this to true (by calling verbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    //things passed into the constructor
    protected GridDataAccessor gridDataAccessor;
    
    //things the constructor sets
    protected String baseFileName; //to which the dv number is added
    protected Class dataClass[]; //1 per data variable  e.g., float.class

    /**
     * This sets everything up (i.e., gets all the data and stores it in 
     * Files).
     *
     * @param tGridDataAccessor a rowMajor gridDataAccessor
     * @throws Throwable if trouble
     */
    public GridDataAllAccessor(GridDataAccessor tGridDataAccessor) throws Throwable {

        gridDataAccessor = tGridDataAccessor;
        if (!gridDataAccessor.rowMajor())
            throw new Exception("GridDataAllAccessor.constructor requires the gridDataAccessor to be rowMajor.");

        //make the dataFiles
        //This is set up to delete the cached files when the creator/owner is done.
        //It could be changed to keep the files in the cache (which is cleared periodically).
        EDV dataVars[] = gridDataAccessor.dataVariables();
        int nDv = dataVars.length;
        String tQuery = gridDataAccessor.userDapQuery();
        baseFileName = gridDataAccessor.eddGrid().cacheDirectory() + //dir created by EDD.ensureValid
            String2.md5Hex12(tQuery == null? "" : tQuery) + "_" +
            Math2.random(Integer.MAX_VALUE) + "_"; //so two identical queries don't interfere with each other

        dataClass = new Class[nDv];
        DataOutputStream dos[] = new DataOutputStream[nDv]; //1 per data variable        
        for (int dv = 0; dv < nDv; dv++) {
            dataClass[dv] = dataVars[dv].destinationDataTypeClass();
            dos[dv] = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(baseFileName + dv)));            
        }

        //get all the data
        while (gridDataAccessor.incrementChunk()) {
            for (int dv = 0; dv < nDv; dv++) {
                gridDataAccessor.getPartialDataValues(dv).writeDos(dos[dv]);
            }
        }
        for (int dv = 0; dv < nDv; dv++) 
            dos[dv].close();
    }


    /**
     * Get all of the destination values for one dataVariable as a DataInputStream.
     *
     * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
     * @return a DataInputStream
     * @param throws RuntimeException if trouble
     */
    public DataInputStream getDataInputStream(int dv) throws Exception {
        return new DataInputStream(new BufferedInputStream(
                new FileInputStream(baseFileName + dv)));            
    }

/* Future: This could allow access to all dataStreams in a 
  synchronous way (e.g., the first value for all dv, then the second, ...).
*/

    /**
     * Get all of the destination values for one dataVariable as a PrimitiveArray.
     * Note that may require a lot of memory!
     *
     * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
     * @return a PrimitiveArray
     * @param throws RuntimeException if trouble, 
     *    e.g., if gdaTotalIndex.size() is &gt;= Integer.MAX_VALUE.
     */
    public PrimitiveArray getPrimitiveArray(int dv) throws Exception {
        long n = gridDataAccessor.totalIndex.size();
        EDStatic.ensureArraySizeOkay(n, "GridDataAllAccessor");
        PrimitiveArray pa = PrimitiveArray.factory(dataClass[dv], (int)n, false);
        pa.readDis(getDataInputStream(dv), (int)n);
        return pa;
    }

    /** 
     * This deletes the files.
     * It is recommended, but not required, that users of this class call this 
     * when they are done using this instance.
     * This won't throw an Exception.
     * 
     */
    public void deleteFiles() {
        int nDv = dataClass.length;
        for (int dv = 0; dv < nDv; dv++) {
            try {
                File2.delete(baseFileName + dv);
            } catch (Throwable t2) {
                String2.log("ERROR in GridDataAllAccessor.deleteFiles: " + 
                    MustBe.throwableToString(t2));
            }
        }
    }

    /** 
     * Users of this class shouldn't call this -- use deleteFiles() instead.
     * Java calls this when an object is no longer used, just before garbage collection. 
     * 
     */
    protected void finalize() throws Throwable {
        try {  //extra insurance
            deleteFiles();
        } catch (Throwable t) {
        }
        super.finalize();
    }
}
