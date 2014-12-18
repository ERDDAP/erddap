/* 
 * GridDataRandomAccessor Copyright 2007, NOAA.
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

import java.io.RandomAccessFile;


/** 
 * This class provides random access to the grid data requested by a 
 * grid data query to an EDDGrid.
 * It first stores all of the data in RandomAccessFiles.
 * Then it allows random access to those files.
 * This doesn't work for String data (the Strings are treated as doubles internally,
 * so become the values from String2.parseDouble(s)).
 *
 * <p>[FUTURE: This could be revised to work with Strings:
 * 1) make isString[nDv]
 * 2) store isString columns to 2 files: 
 *   bytes byte[] with utf8 bytes from the string, appended one after another 
 *      (StringArray.writeDos)
 *   offset long[] with offset of end of string. (beginning of first string is offset=0)
 * 3) add  public String getDataValueAsString(int current[], int dv)
 *   which uses a variant of StringArray.readDis (but from RandomAccessFile).
 * ]
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-07-06
 */
public class GridDataRandomAccessor { 

    
    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    //things passed into the constructor
    protected GridDataAccessor gridDataAccessor;
    
    //things the constructor generates
    protected NDimensionalIndex gdaTotalIndex;
    protected String rafName;
    protected Class dataClass[]; //1 per data variable
    protected RandomAccessFile dataRaf[]; //1 per data variable

    /**
     * This sets everything up (i.e., gets all the data and stores it in 
     * RandomAccessFiles).
     *
     * @param tGridDataAccessor a rowMajor gridDataAccessor
     * @throws Throwable if trouble
     */
    public GridDataRandomAccessor(GridDataAccessor tGridDataAccessor) throws Throwable {

        gridDataAccessor = tGridDataAccessor;
        if (!gridDataAccessor.rowMajor())
            throw new Exception("GridDataRandomAccessor.constructor requires the gridDataAccessor to be rowMajor.");

        //make the dataRaf's
        EDV dataVars[] = gridDataAccessor.dataVariables();
        int nDv = dataVars.length;
        dataRaf = new RandomAccessFile[nDv];
        dataClass = new Class[nDv];
        String tQuery = gridDataAccessor.userDapQuery();
        rafName = gridDataAccessor.eddGrid().cacheDirectory() + //dir created by EDD.ensureValid
            String2.md5Hex12(tQuery == null? "" : tQuery) + "_" +
            Math2.random(Integer.MAX_VALUE) + "_";
        for (int dv = 0; dv < nDv; dv++) {
            dataClass[dv] = dataVars[dv].destinationDataTypeClass();
            dataRaf[dv] = new RandomAccessFile(rafName + dv, "rw");            
        }

        //get all the data
        while (gridDataAccessor.increment()) {
            for (int dv = 0; dv < nDv; dv++)
               PrimitiveArray.rafWriteDouble( //this doesn't work for Strings
                   dataRaf[dv], dataClass[dv], gridDataAccessor.getDataValueAsDouble(dv));
        }
        gdaTotalIndex = gridDataAccessor.totalIndex();

    }


    /**
     * Call this after increment() to get a data value (as a double) 
     * from the specified dataVariable.
     *
     * @param current  from gridDataAccessor.totalIndex().getCurrent() (or compatible),
     *   but with values changed to what you want.
     * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
     * @return the data value
     * @param throws Throwable if trouble
     */
    public double getDataValueAsDouble(int current[], int dv) throws Throwable {
        return PrimitiveArray.rafReadDouble(dataRaf[dv], dataClass[dv], 0,
            gdaTotalIndex.setCurrent(current));
    }

    /** 
     * This closes the files.
     * It is recommended, but not required, that users of this class call this 
     * (or closeAndDelete) when they are done using this instance.
     * This is also called by finalize. 
     * This won't throw an Exception.
     * 
     */
    public void close() {
        //??? Or don't use random number in rafName, 
        //leave files for the fullCacheDirectory cleaner to catch,
        //and reuse the data (File2.touch() the files) 
        //if another identical request arrives before files are deleted.
        try {
            int nDv = dataRaf.length;
            for (int dv = 0; dv < nDv; dv++) {
                try {
                    dataRaf[dv].close(); //will already by closed if this method already called           
                } catch (Throwable t2) {
                    //String2.log(MustBe.throwableToString(t2));
                }
            }
        } catch (Throwable t) {
        }
    }

    /** 
     * This closes and deletes the files.
     * It is recommended, but not required, that users of this class call this 
     * (or close) when they are done using this instance.
     * This won't throw an Exception.
     * 
     */
    public void closeAndDelete() {
        close();
        try {
            int nDv = dataRaf.length;
            for (int dv = 0; dv < nDv; dv++) {
                try {
                    File2.delete(rafName + dv);
                } catch (Throwable t2) {
                    //String2.log(MustBe.throwableToString(t2));
                }
            }
        } catch (Throwable t) {
        }
    }

    /** 
     * Users of this class shouldn't call this -- use close() instead.
     * Java calls this when an object is no longer used, just before garbage collection. 
     * 
     */
    protected void finalize() throws Throwable {
        try {  //extra insurance
            close();
        } catch (Throwable t) {
        }
        super.finalize();
    }
}
