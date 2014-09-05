/* 
 * HdfTag Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.String2;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This is the superclass of all HdfTags.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-09-01
 *
 */
public abstract class HdfTag  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    //private variables
    public short tagType;
    public short referenceNumber;


    /**
     * This returns the length of the data (in bytes)
     *
     * @return length
     */ 
    public abstract int getLength();

    /**
     * This writes the class's information to the dataOutputSream.
     *
     * @param stream
     */ 
    public abstract void writeData(DataOutputStream stream) throws Exception;




}
