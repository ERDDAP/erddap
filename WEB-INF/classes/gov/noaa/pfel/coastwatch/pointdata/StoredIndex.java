/* 
 * StoredIndex Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.Math2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * This class stores the values for an index (an ascending sorted array) 
 * in a disk file and has facilities to do quick binary searches.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-07-10
 */
public class StoredIndex  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /* groups one row for each group and has columns for 
     *   "First Row", "N Finite", 
     *   then, for each index: indexName+" Min", indexName+" Max"
     */
    private String indexFileName;
    private int nValues, nFinite;
    private double lowestValue, highestValue;
    private Class elementClass;

    /**
     * A constructor stores the indexPA in indexFileName.
     * 
     *
     * @param indexFileName the full name of the file to be created to store the index values
     * @param indexPA  the index values (always numeric, usually time), already sorted in ascending order
     * @throws Exception if trouble
     */
    public StoredIndex(String indexFileName, PrimitiveArray indexPA) throws Exception {
        long time = System.currentTimeMillis();
        if (verbose) String2.log("StoredIndex.constructor for " + indexFileName); 

        this.indexFileName = indexFileName;
        nValues = indexPA.size();
        elementClass = indexPA.elementClass();

        //find nFinite value
        int lastFinite = nValues - 1;
        while (lastFinite >= 0 && !Math2.isFinite(indexPA.getDouble(lastFinite)))
            lastFinite--;
        nFinite = lastFinite + 1;
        lowestValue = indexPA.getDouble(0);
        highestValue = indexPA.getDouble(lastFinite);

        //save indexPA in file
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
            new FileOutputStream(indexFileName)));
        indexPA.writeDos(dos);
        dos.close();

        String2.log("StoredIndex time=" + (System.currentTimeMillis() - time));
    }

    /**
     * This deletes the index file created by the constructor.
     *
     */
    public void close() { 
        File2.delete(indexFileName);
    }

    /**
     * This returns [0]=the index of the first value Greater Than Or Equal to desiredMin
     * and [1]=the index of the last value Less Than Or Equal to desiredMax
     * via a binary search.
     *
     * @param desiredMin The minimum acceptable value.
     * @param desiredMax The maximum acceptable value.
     * @return an array with [0]=the index of the first value Greater Than Or Equal to desiredMin
     *     and [1]=the index of the last value Less Than Or Equal to desiredMax.
     *     If desiredMin/Max are between two indices, results[1] will be less than results[0].
     *     If desiredMax&lt;lowestValue or desiredMin&gt;greatestValue, there 
     *        are no matching indices, and this returns {-1, -1}.
     * @throws Exception if trouble
     */
    public int[] subset(double desiredMin, double desiredMax) throws Exception {

        if (verbose) String2.log("StoredIndex.subset " + 
            " desiredMin=" + desiredMin + 
            " desiredMax=" + desiredMax); 

        int results[] = {-1, -1};
        if (desiredMax < lowestValue ||
            desiredMin > highestValue)
            return results;

        //search sorted index in file for first and last rows in range
        long time = System.currentTimeMillis();
        RandomAccessFile raf = new RandomAccessFile(indexFileName, "r");
        results[0] = (int)PrimitiveArray.rafFirstGAE(raf, elementClass, //safe since reading an int
            0,  //byte in file that values start at 
            0, nFinite - 1, desiredMin, 5); //precision=5
        results[1] = (int)PrimitiveArray.rafLastLAE(raf, elementClass,
            0,  //byte in file that values start at 
            results[0], nFinite - 1, desiredMax, 5); //precision=5
        raf.close();

        if (verbose) String2.log("  first=" + results[0] + " last=" + results[1] +
            " time=" + (System.currentTimeMillis() - time));
        return results;
    }

    /**
     * A main method -- used to test the methods in this class.
     *
     * @param args is ignored  (use null)
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {
        StoredIndex.verbose = true;

        String dir = File2.getSystemTempDirectory();
        String name = "StoredIndexTest";
        int n = 1000000;
        DoubleArray pa = new DoubleArray(n, false);
        for (int i = 0; i < n; i++)
            pa.add(i * 0.1);
        StoredIndex index = new StoredIndex(dir + name, pa);

        //get all
        Test.ensureEqual(String2.toCSSVString(index.subset(0, n/.1)), "0, 999999", "");

        //get some
        Test.ensureEqual(String2.toCSSVString(index.subset(1, 2)), "10, 20", "");

        //between 2 indices
        Test.ensureEqual(String2.toCSSVString(index.subset(1.55, 1.56)), "16, 15", "");

        //get none
        Test.ensureEqual(String2.toCSSVString(index.subset(-.1, -.1)), "-1, -1", "");
        Test.ensureEqual(String2.toCSSVString(index.subset(100000, 100000)), "-1, -1", "");

        index.close();
        String2.log("\n***** StoredIndex.main finished successfully");

    }


}
