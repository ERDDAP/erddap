/* 
 * HdfWriter Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.File2;
//import com.cohort.util.Math2;
import com.cohort.util.String2;
//import com.cohort.util.Test;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
//import java.util.ArrayList;

/**
 * THIS WAS NEVER FINISHED.  SEE SdsWriter INSTEAD.
 * Create an HDF version 4 SDS file. This is a replacement for the
 * (for me) hard to use,
 * finicky (the jar files are tied to specific out-of-date versions of Java) 
 * native libraries (which are different for each OS) from NCSA.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-09-01
 *
 */
public class HdfWriter  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    private boolean open = false;

    //private variables
    private int openFileID;
    private int openSdsID;
    private int openSelectVariable;
    private DataOutputStream stream;
    private int nVariables = 0;
    private int currentRank = -1;

    /**
     * This opens a new hdfFile.
     *
     * @param hdfFileName 
     * @param mode Currently, only DFACC_CREATE (which throws an exception if the file already exists)
     *    is supported
     * @return fileID a reference to the file
     * @throws Exception if error
     */
    public int sdStart(String hdfFileName, int mode) throws Exception {
  
        //validate parameters
        String errorIn = String2.ERROR + " in HDFWriter.sdStart: ";
        if (mode != HdfConstants.DFACC_CREATE)
            throw new RuntimeException(errorIn + "unsupported mode (" + mode + ").");
        if (openFileID != 0)
            throw new RuntimeException(errorIn + "a file has already been started.");
        if (File2.isFile(hdfFileName))
            throw new RuntimeException(errorIn + "file (" + hdfFileName + ") already exists.");

        stream = new DataOutputStream( new BufferedOutputStream(
            new FileOutputStream(hdfFileName)));

        //from hdf4.2r1/hdf/src/hfile.h 
        int CDFTYPE = 6;

        //since this Java version is always constrained to work with
        //one hdf file per hdfWriter object, this can just always return 0
        //see SDstart in hdf4.2r1/mfhdf/libsrc/mfsd.c
        //see ncopen and nccreate in hdf4.2r1/mfhdf/libsrc/file.c
        int cdfid = 0;

        //see SDstart in hdf4.2r1/mfhdf/libsrc/mfsd.c
        openFileID = (cdfid << 24) + (CDFTYPE << 16) + cdfid;

        return openFileID;
    }
    
    /**
     * This creates a variable in the hdfFile.
     *
     * @param fileID the fileID returned from sdStart.
     * @param varName the name of the variable
     * @param rank the number of dimensions, e.g., 2
     * @param dimSizes an int[] with the length of each of the dimensions,
     *    e.g., new int[]{lat.length, lon.length}
     * @return sdsID a reference to this variable
     * @throws Exception if error
     */
    public int sdCreate(int fileID, String varName, int rank, int dimSizes[]) {
        //see SDcreate in hdf4.2r1/mfhdf/libsrc/mfsd.c
        
        //validate parameters
        String errorIn = String2.ERROR + " in HDFWriter.sdCreate: ";
        if (openFileID == 0)
            throw new RuntimeException(errorIn + "use sdStart first.");
        if (fileID != openFileID)
            throw new RuntimeException(errorIn + "invalid fileID (" + fileID + ").");
        if (openSdsID != 0)
            throw new RuntimeException(errorIn + "a variable is already open.");
        if (openSelectVariable != 0)
            throw new RuntimeException(errorIn + "a variable is already selected.");
        if (varName == null || varName.length() == 0) //ncsa does this
            varName = "DataSet";
        if (rank < 1 || rank > 5) //I should look up correct max
            throw new RuntimeException(errorIn + "rank (" + rank + ") must be between 1 and 5.");
        if (dimSizes == null || dimSizes.length != rank) //I should look up correct max
            throw new RuntimeException(errorIn + "dimSizes.length must equal rank.");
        for (int i = 0; i < rank; i++)
            if (dimSizes[i] <= 0) 
                throw new RuntimeException(errorIn + "dimSizes[" + i + "] must be greater than 0.");

        currentRank = rank;

        //from hdf4.2r1/hdf/src/hfile.h 
        int SDSTYPE = 4;
        openSdsID = (fileID << 24) + (SDSTYPE << 16) + nVariables; 
        nVariables++;
        return openSdsID;
    }

    /**
     * This writes data to a variable.
     *
     * @param sdsID the sdsID returned from sdCreate
     * @param start an int array with the starting point for reading data
     *   from the data array
     * @param stride an int array with the stride values for reading data
     *   from the data array (e.g., 1=read every value, 2=read every other value,
     *   3=read every third value, ...).
     *   If stride is null, it will be interpreted as an array of 1's.
     * @param size an int array specifying the size of the data that will be read
     * @param dataArray the array with the data (stored as a 1 dimensional array,
     *   ???order)
     * @return true if successful; otherwise false
     * @throws Exception if error
     */
    public boolean sdWriteData(int sdsID, int[] start, int[] stride, int[]size,
        Object dataArray) {
        
        //validate parameters
        String errorIn = String2.ERROR + " in HDFWriter.sdWriteData: ";
        if (openFileID == 0)
            throw new RuntimeException(errorIn + "use sdStart first.");
        if (openSdsID == 0)
            throw new RuntimeException(errorIn + "no variable is open.");
        if (sdsID != openSdsID)
            throw new RuntimeException(errorIn + "invalid sdsID (" + sdsID + ").");
        if (openSelectVariable != 0)
            throw new RuntimeException(errorIn + "a variable is already selected.");
        if (start == null || start.length != currentRank)
            throw new RuntimeException(errorIn + "invalid start array.");
        if (stride == null) {
            stride = new int[currentRank];
            Arrays.fill(stride, 1);
        }
        if (stride.length != currentRank)
            throw new RuntimeException(errorIn + "invalid stride array.");
        if (size == null || start.length != currentRank)
            throw new RuntimeException(errorIn + "invalid start array.");


        return true;
    }

    /**
     * This is used after sdWriteData to close a variable.
     * 
     * @param sdsID the sdsID returned from sdCreate
     * @throws Exception if error
     */
    public boolean sdEndAccess(int sdsID) {

        //validate parameters
        String errorIn = String2.ERROR + " in HDFWriter.sdEndAccess: ";
        if (openFileID == 0)
            throw new RuntimeException(errorIn + "use sdStart first.");
        if (openSdsID == 0)
            throw new RuntimeException(errorIn + "no variable is open.");
        if (sdsID != openSdsID)
            throw new RuntimeException(errorIn + "invalid sdsID (" + sdsID + ").");

        openSdsID = 0;
        return true;
    }

    /**
     * This is used to select an existing variable so that you can add 
     * attributes to it.
     *
     * @param fileID the fileID returned from sdStart.
     * @param varNumber the number of the variable (0, 1, 2, ...)
     *   referring to the order the variables were created.
     * @return sdsID a reference to this variable
     * @throws Exception if error
     */ 
    public int sdSelect(int fileID, int varNumber) {

        //validate parameters
        String errorIn = String2.ERROR + " in HDFWriter.sdSelect: ";
        if (openFileID == 0)
            throw new RuntimeException(errorIn + "use sdCreate first.");
        if (fileID != openFileID)
            throw new RuntimeException(errorIn + "invalid fileID (" + fileID + ").");
        if (openSelectVariable != 0)
            throw new RuntimeException(errorIn + "a variable is already selected.");
        if (varNumber < 0 || varNumber >= nVariables)
            throw new RuntimeException(errorIn + "invalid fileID (" + fileID + ").");

        openSelectVariable = varNumber;
        return openSelectVariable;
    }

    /**
     * This sets a global attribute or variable attribute.
     * 
     * @param id the fileID or sdsID of the file or variable to which you want
     *    to add an attribute.
     * @param name the name for the attribute
     * @param o The value of the attribute.
     *   This must be an array of primitive values. 
     *   To store a String, use myString.getBytes();
     * @return true if successful; otherwise false
     * @throws Exception if error
     */
    public boolean sdSetAttr(int id, String name, Object o) {

        //validate parameters
        String errorIn = String2.ERROR + " in HDFWriter.sdSetAttr: ";
        if (openFileID == 0)
            throw new RuntimeException(errorIn + "use sdStart first.");
        if (id != openFileID && id != openSdsID)
            throw new RuntimeException(errorIn + "invalid id (" + id + ").");
        if (name == null || name.length() == 0)
            throw new RuntimeException(errorIn + "no name provided.");
        if (o == null)
            throw new RuntimeException(errorIn + "object is null for '" + name + "'.");


        return true;
    }

    /**
     * This is a convenience method to sets a global attribute or variable attribute.
     * 
     * @param id the fileID or sdsID of the file or variable to which you want
     *    to add an attribute.
     * @param name the name for the attribute
     * @param o The value of the attribute.
     *   This must be an array of primitive values. 
     *   To store a String, use myString.getBytes();
     * @return true if successful; otherwise false
     * @throws Exception if error
     */
    public boolean sdSetAattr(int id, String name, Object o) throws Exception {

        //validate parameters
        String errorIn = String2.ERROR + " in HDFWriter.sdSetAttr: ";
        if (openFileID == 0)
            throw new RuntimeException(errorIn + "use sdStart first.");
        if (id != openFileID && id != openSdsID)
            throw new RuntimeException(errorIn + "invalid id (" + id + ").");
        if (name == null || name.length() == 0)
            throw new RuntimeException(errorIn + "no name provided.");
        if (o == null)
            throw new RuntimeException(errorIn + "object is null for '" + name + "'.");

        boolean result;
        if (o instanceof String) {
            byte bar[] = ((String)o).getBytes();
            result = sdSetAttr(id, name, HdfConstants.DFNT_CHAR8, bar.length, bar);
        } else if (o instanceof byte[])
            result = sdSetAttr(id, name, HdfConstants.DFNT_INT8, ((byte[])o).length, o);
        else if (o instanceof short[])
            result = sdSetAttr(id, name, HdfConstants.DFNT_INT16, ((short[])o).length, o);
        else if (o instanceof int[])
            result = sdSetAttr(id, name, HdfConstants.DFNT_INT32, ((int[])o).length, o);
        else if (o instanceof long[])
            result = sdSetAttr(id, name, HdfConstants.DFNT_INT64, ((long[])o).length, o);
        else if (o instanceof float[])
            result = sdSetAttr(id, name, HdfConstants.DFNT_FLOAT32, ((float[])o).length, o);
        else if (o instanceof double[])
            result = sdSetAttr(id, name, HdfConstants.DFNT_FLOAT64, ((double[])o).length, o);
        else throw new ClassNotFoundException (errorIn + "unsupported object type for '" + name + "'.");

        //result = HDFLibrary.SDsetattr(fileID, name, HDFConstants.DFNT_CHAR8, data.length, data);
        return true;
    }
    

    /**
     * This closes the file
     * 
     * @param fileID the fileID or sdsID of the file or variable to which you want
     *    to add an attribute.
     * @return true if successful; otherwise false
     * @throws Exception if error
     */
    public boolean sdEnd(int fileID) {

        //validate parameters
        String errorIn = String2.ERROR + " in HDFWriter.sdEnd: ";
        if (openFileID == 0)
            throw new RuntimeException(errorIn + "use sdStart first.");
        if (openSdsID != 0)
            throw new RuntimeException(errorIn + "use sdEndAccess first.");
        if (fileID != openFileID)
            throw new RuntimeException(errorIn + "invalid fileID (" + fileID + ").");


        openFileID = 0;
        return true;
    } 
}
