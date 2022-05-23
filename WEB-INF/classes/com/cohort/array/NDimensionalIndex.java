/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.util.Arrays;
 
/** 
 * This class lets you treat a 1D array (e.g., a PrimitiveArray) as an 
 * nDimensional array.  For example, think of new int[]{2,3}
 * as an array[2][3] where the [3] index (the rightmost) varies the fastest.
 * This is C and Java's "row-major order" storage, 
 * not "column-major order" storage typical of Fortran.
 *
 * <p>For random access to the nDimensional array, use setIndex or setCurrent.
 * 
 * <p>For iterating over all the elements, create an object, then 
 * repeatedly use increment().
 * Then use getCurrent (for the nDimensional position) and/or 
 * getIndex (for the 1 dimensional position)
 */
public class NDimensionalIndex { 

    /** The original shape[] set by the constructor. */
    protected int[] shape;

    /** The factors[] of each element (each is the product of the elements to the right). */
    protected long[] factors;

    /** Set by the constructor (shape.length). */
    protected int nDimensions;

    /** Set by the constructor (all of the shape values multiplied together). */
    protected long size;

    /** This holds the current position (as if 1D array); initially -1. 
     * This will only be a valid value if increment() returned true.
     */
    protected long index;

    /** This holds the current position in the nDimensional array, 
     * initially corresponding to index = -1. 
     * This is the value incremented by increment(), so don't modify it. 
     * This will only be a valid value if increment() returned true.
     */
    protected int[] current;

    /** This indicates how many dimensions were changed by the last increment().
     * Note that a dimension with size=1 may have been changed but still have the same value. 
     */
    protected int nDimensionsChanged;

    /**
     * The constructor.
     *
     * @param tShape an int[] with an element for each dimension indicating
     *    the size of that dimension. For example, think of new int[]{2,3}
     *    as an array[2][3] where the [3] index varies the fastest.
     *    index and current[] are set to 1 element before the first element
     *    (ready for the first call to increment()).
     *    This is C and Java's "row-major order" storage, 
     *    not "column-major order" storage typical of Fortran.
     */
    public NDimensionalIndex(int[] tShape) {

        shape = tShape;
        nDimensions = shape.length;
        if (nDimensions == 0)
            throw new RuntimeException(String2.ERROR + 
                " in NDimensionalIndex constructor: nDimensions=0.");
        factors = new long[nDimensions]; 
        size = 1;
        for (int i = nDimensions - 1; i >= 0; i--) {
            if (shape[i] < 1)
                throw new RuntimeException(String2.ERROR + 
                    " in NDimensionalIndex constructor: shape=[" + 
                    String2.toCSSVString(shape) + "] has a value less than 1.");
            factors[i] = size;
            size *= shape[i];
        }
        current = new int[nDimensions]; //all 0's
        reset();
    }

    /**
     * This returns a String representation of this nDimensionalIndex.
     *
     * @return a String representation of this nDimensionalIndex.
     */
    public String toString() {
        return "NDimensionalIndex([" + String2.toCSSVString(shape) + "])";
    }

    /**
     * This resets the index to -1 and the current[] to the corresponding values
     * in preparation for restarting the increment() system.
     */
    public void reset() {
        index = -1;
        Arrays.fill(current, 0);
        current[nDimensions  - 1] = -1;
        nDimensionsChanged = 0;
    }



    /**
     * This increments index and current (with the rightmost dimension, dim=nDimensions-1, varying fastest).
     * This is C and Java's "row-major order" storage, 
     * not "column-major order" storage typical of Fortran.
     * Afterwards, use getIndex() and/or getCurrent() to find out the current position.
     * 
     * <p>With this version of increment, the index increases by 1 each time.
     *
     * @return true if increment was successful (and index still points to a valid value).
     */
    public boolean increment() {
        index++;
        int tDimension = nDimensions - 1;
        while (tDimension >= 0) {
            current[tDimension]++;
            if (current[tDimension] < shape[tDimension]) {
                nDimensionsChanged = nDimensions - tDimension;
                return true;
            }
            current[tDimension] = 0;
            tDimension--;
        }

        //increment failed,  set to one past data range
        current[0] = shape[0]; //not perfect, but reflects index and is an invalid position
        nDimensionsChanged = nDimensions;
        return false;
    }

    /**
     * This increments index and current (with the leftmost dimension, dim=0, varying fastest).
     * This is Fortran-style "column-major order" storage, 
     * not "row-major order" storage typical of C and Java.
     * Afterwards, use getIndex() and/or getCurrent() to find out the current position.
     * 
     * <p>With this version of increment, the index value jumps around a lot --
     * it doesn't just increase by 1 each time.
     * 
     * <p>Note that the data is still in row-major order and the index and current
     * still relect that order. incrementCM just iterates through the values
     * in a different order than increment().
     *
     * @return true if increment was successful (and index still points to a valid value).
     */
    public boolean incrementCM() {
        if (index < 0) {
            index = 0;
            Arrays.fill(current, 0);
            return true;
        }
        int tDimension = 0;
        while (tDimension < nDimensions) {
            current[tDimension]++;
            if (current[tDimension] < shape[tDimension]) {
                if (tDimension == 0) {
                    index += factors[0];
                } else { 
                    index = 0;
                    for (int i = 0; i < nDimensions; i++) 
                        index += current[i] * factors[i];
                }
                nDimensionsChanged = tDimension + 1;
                return true;
            }
            current[tDimension] = 0;
            tDimension++;
        }

        //increment failed
        current[0] = shape[0]; //not perfect, but reflects index and is an invalid position
        index = size;
        nDimensionsChanged = nDimensions;
        return false;
    }

    /**
     * This returns the shape of the nDimensional array.
     *
     * @return the shape of the nDimensional array
     *   (the internal array, so don't change it).
     */
    public int[] shape() {return shape; }
       
    /**
     * This returns the factors of each element (each is the product of the 
     * elements to the right). 
     *
     * @return the factors of each element (each is the product of the 
     *   elements to the right) 
     *   (the internal array, so don't change it).
     */
    public long[] factors() {return factors; }
       
    /**
     * This returns the size of the 1 dimensional array.
     *
     * @return the size of the 1 dimensional array
     */
    public long size() {return size; }
       
    /**
     * This returns the number of dimensions.
     *
     * @return the number of dimensions
     */
    public int nDimensions() {return nDimensions; }
       
    /**
     * This returns the current position in the 1 dimensional array.
     *
     * @return the current position in the 1 dimensional array
     */
    public long getIndex() {return index; }
       
    /** This indicates how many dimensions were changed by the last increment().
     * Note that a dimension with size=1 may have been changed but still have the same value. 
     *
     * @return the number of dimensions that were changed by the last increment().
     */
    public int nDimensionsChanged() {return nDimensionsChanged; }
       
    /**
     * This returns the current position in the n dimensional array.
     *
     * @return the current position in the n dimensional array
     *   (the internal array, so don't change it, except to call setCurrent()).
     *   So you can call this once (and hang on to this object) and
     *   call increment() repeatedly to change the values in this array.
     */
    public int[] getCurrent() {return current; }
       
    /**
     * This defines the new position in the 1 dimensional array.
     * This changes the internal index and current.
     *
     * @param tIndex the new position in the 1 dimensional array
     * @return the new current position in the nDimensional array
     *   (the internal array, so don't change it).
     * @throws RuntimeException if tIndex &lt; 0 or &gt;= size 
     */
    public int[] setIndex(long tIndex) {
        if (tIndex < 0 || tIndex >= size)
            throw new RuntimeException(String2.ERROR + " in NDimensionalIndex.setIndex: tIndex=" + 
                tIndex + " is less than 0 or greater than or equal to size=" + size);

        index = tIndex;
        for (int i = 0; i < nDimensions; i++) {
            current[i] = (int)(tIndex / factors[i]); //safe since tIndex is valid, this will be valid int since each dimension size < Integer.MAX_VALUE
            tIndex %= factors[i];
        }
        return current; 
    }

    /**
     * This defines the new position in the n dimensional array.
     * This changes the internal index and current.
     *
     * @param tCurrent the new position in the n dimensional array
     *   (the tCurrent array isn't kept; the values are copied).
     * @return the new current position in the 1 dimensional array
     * @throws RuntimeException if any index is invalid
     */
    public long setCurrent(int[] tCurrent) {
        index = 0;
        if (tCurrent.length != nDimensions)
            throw new RuntimeException(String2.ERROR + " in NDimensionalIndex.setCurrent: tCurrent.length=" + 
                tCurrent.length + " isn't " + nDimensions + ".");
        System.arraycopy(tCurrent, 0, current, 0, nDimensions);
        for (int i = 0; i < nDimensions; i++) {
            if (tCurrent[i] < 0 || tCurrent[i] >= shape[i])
                throw new RuntimeException(String2.ERROR + " in NDimensionalIndex.setCurrent: tCurrent=[" + 
                    String2.toCSSVString(tCurrent) + "] is invalid for shape=[" +
                    String2.toCSSVString(shape) + "].");
            index += current[i] * factors[i];
        }
        return index; 
    }

    public Object clone() {
        int[] tShape = new int[nDimensions];
        System.arraycopy(shape, 0, tShape, 0, nDimensions);
        return new NDimensionalIndex(tShape);
    }


    /**
     * Given tConstraints, this calculates the number of values in the subset.
     *
     * @param tConstraints 
     *   For each axis variable, there will be 3 numbers (startIndex, stride, stopIndex).
     *   !!! If there is a special axis0, this will not include constraints for axis0.
     * @return the number of values in the subset.
     */
    public int subsetSize(IntArray tConstraints) {
        int n = 1;
        int nDimensions = tConstraints.size() / 3;
        for (int d = 0; d < nDimensions; d++) {
            int base = d * 3;
            int start  = tConstraints.get(base);
            int stride = tConstraints.get(base + 1);
            int stop   = tConstraints.get(base + 2);
            int dSize = ((stop+1 - start)/stride) + 1;
            n *= dSize;
        }
        return n;
    }

    /**
     * Given a subset selection for this index, this creates a subsetIndex.
     *
     * @param variableName
     * @param tConstraints 
     *   For each axis variable, there will be 3 numbers (startIndex, stride, stopIndex).
     *   !!! If there is a special axis0, this will not include constraints for axis0.
     * @return int[nDim] with the start values for all dimensions.
     * @throws RuntimeException if something is wrong.
     */
    public int[] makeSubsetIndex(String variableName, IntArray tConstraints) {
        if (nDimensions * 3 != tConstraints.size())
            throw new RuntimeException("Variable=" + variableName + " has nDimensions=" + nDimensions + 
                " but the subset constraints have nDimensions=" + (tConstraints.size()/3) + ".");
        int subsetIndex[] = new int[nDimensions];
        for (int d = 0; d < nDimensions; d++) {
            int base = d * 3;
            int start  = tConstraints.get(base);
            int stride = tConstraints.get(base + 1);
            int stop   = tConstraints.get(base + 2);
            subsetIndex[d] = start;
            if (start < 0)
                throw new RuntimeException("The subset constraints start[" + d + "]=" + start + " is < 0.");
            if (start >= shape[d])
                throw new RuntimeException("The subset constraints start[" + d + "]=" + start + " is >= shape[" + d + "].");
            if (stride <= 0)
                throw new RuntimeException("The subset constraints stride[" + d + "]=" + stride + " is <= 0.");
            if (stop < 0)
                throw new RuntimeException("The subset constraints stop[" + d + "]=" + stop + " is < 0.");
            if (stop >= shape[d])
                throw new RuntimeException("The subset constraints stop[" + d + "]=" + stop + " is >= shape[" + d + "].");
        }
        return subsetIndex;
    }

    /**
     * This tests if a subset selection for this index will get all values.
     *
     * @param tConstraints 
     *   For each axis variable, there will be 3 numbers (startIndex, stride, stopIndex).
     *   !!! If there is a special axis0, this will not include constraints for axis0.
     * @return true if it will get all values.
     */
    public boolean willGetAllValues(IntArray tConstraints) {
        for (int d = 0; d < nDimensions; d++) {
            int base = d * 3;
            int stop = tConstraints.get(base + 2);
            if (tConstraints.get(base + 0) != 0 ||        //start
                tConstraints.get(base + 1) != 1 ||        //stride
                stop                       != shape[d]-1) //stop
                return false;
            if (stop >= shape[d]) 
                throw new RuntimeException("Requested stop[" + d + "]=" + stop + " >= size[" + d + "]=" + shape[d] + ".");
        }
        return true;
    }

    
    /**
     * Given a subset selection for this index, this increments (by stride[nDim-1])
     * the subsetIndex (and other dimensions if needed).
     * 
     * @param subsetIndex as created by getSubsetIndex().
     * @param tConstraints 
     *   For each axis variable, there will be 3 numbers (startIndex, stride, stopIndex).
     *   !!! If there is a special axis0, this will not include constraints for axis0.
     * @return true if increment was successful and still points to a valid value.
     *   So false means we're done with the entire subset selection.
     */
    public boolean incrementSubsetIndex(int subsetIndex[], IntArray tConstraints) {
        for (int d = nDimensions-1; d >= 0; d--) {
            int base = d*3;
            subsetIndex[d] += tConstraints.get(base+1);     //stride
            if (subsetIndex[d] <= tConstraints.get(base+2)) //stop
                return true;
            subsetIndex[d] = tConstraints.get(base);        //start
        }  
        return false;
    }

    /**
     * This tests the subsetIndex system.
     */
    public static void testSubsetIndex() {
        String2.log("\n*** NDimensionalIndex.testSubsetIndex");

        //test get all 
        NDimensionalIndex index = new NDimensionalIndex(new int[]{4,5});
        int current[] = index.getCurrent();
        IntArray tConstraints = IntArray.fromCSV("0, 1, 3, 0, 1, 4"); //get all
        int subsetIndex[] = index.makeSubsetIndex("myVarName", tConstraints);
        Test.ensureEqual(index.willGetAllValues(tConstraints), true, "");
        for (int i = 0; i < 20; i++) {
            Test.ensureEqual(index.increment(), true, "i=" + i);
            Test.ensureEqual(current, subsetIndex,   "i=" + i);

            Test.ensureEqual(index.incrementSubsetIndex(subsetIndex, tConstraints), i < 19, "i=" + i);
        }
        Test.ensureEqual(index.increment(), false, "at the end");

        //test get subset
        index.reset();
        tConstraints = IntArray.fromCSV("1, 2, 3, 0, 3, 4");  //not that 2nd stop is beyond last matching value
        subsetIndex = index.makeSubsetIndex("myVarName", tConstraints);
        Test.ensureEqual(index.willGetAllValues(tConstraints), false, "");
        StringBuilder results = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            Test.ensureEqual(index.increment(), true, "i=" + i);

            if (Test.testEqual(String2.toCSSVString(current), String2.toCSSVString(subsetIndex), "msg").equals("")) {
                results.append("equal at i=" + i + " current=" + String2.toCSSVString(current) + "\n");
                if (!index.incrementSubsetIndex(subsetIndex, tConstraints)) {
                    results.append("done at i=" + i + "\n");
                    break;
                }
            }
        }
        String expected = 
"equal at i=5 current=1, 0\n" +
"equal at i=8 current=1, 3\n" +
"equal at i=15 current=3, 0\n" +
"equal at i=18 current=3, 3\n" +
"done at i=18\n";
        Test.ensureEqual(results.toString(), expected, "results=\n" + results.toString());

        //test get subset
        index.reset();
        tConstraints = IntArray.fromCSV("0, 3, 3, 1, 2, 4");
        subsetIndex = index.makeSubsetIndex("myVarName", tConstraints);
        Test.ensureEqual(index.willGetAllValues(tConstraints), false, "");
        results = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            Test.ensureEqual(index.increment(), true, "i=" + i);

            if (Test.testEqual(String2.toCSSVString(current), String2.toCSSVString(subsetIndex), "msg").equals("")) {
                results.append("equal at i=" + i + " current=" + String2.toCSSVString(current) + "\n");
                if (!index.incrementSubsetIndex(subsetIndex, tConstraints)) {
                    results.append("done at i=" + i + "\n");
                    break;
                }
            }
        }
        expected = 
"equal at i=1 current=0, 1\n" +
"equal at i=3 current=0, 3\n" +
"equal at i=16 current=3, 1\n" +
"equal at i=18 current=3, 3\n" +
"done at i=18\n";
        Test.ensureEqual(results.toString(), expected, "results=\n" + results.toString());

    }



    /**
     * This tests this class.
     * @throws Exception if trouble
     */
    public static void basicTest() throws Exception {
        String2.log("*** NDimensionalIndex.basicTest");

        //test increment
        NDimensionalIndex a = new NDimensionalIndex(new int[]{2,2,3});
        Test.ensureEqual(a.size, 12, "");

        Test.ensureTrue(a.increment(), "0");
        Test.ensureEqual(a.getIndex(), 0, "0");
        Test.ensureEqual(a.getCurrent(), new int[]{0,0,0}, "0");

        Test.ensureTrue(a.increment(), "1");
        Test.ensureEqual(a.getIndex(), 1, "1");
        Test.ensureEqual(a.getCurrent(), new int[]{0,0,1}, "1");

        Test.ensureTrue(a.increment(), "2");
        Test.ensureEqual(a.getIndex(), 2, "2");
        Test.ensureEqual(a.getCurrent(), new int[]{0,0,2}, "2");

        Test.ensureTrue(a.increment(), "3");
        Test.ensureEqual(a.getIndex(), 3, "3");
        Test.ensureEqual(a.getCurrent(), new int[]{0,1,0}, "3");

        Test.ensureTrue(a.increment(), "4");
        Test.ensureEqual(a.getIndex(), 4, "4");
        Test.ensureEqual(a.getCurrent(), new int[]{0,1,1}, "4");

        Test.ensureTrue(a.increment(), "5");
        Test.ensureEqual(a.getIndex(), 5, "5");
        Test.ensureEqual(a.getCurrent(), new int[]{0,1,2}, "5");

        Test.ensureTrue(a.increment(), "6");
        Test.ensureEqual(a.getIndex(), 6, "6");
        Test.ensureEqual(a.getCurrent(), new int[]{1,0,0}, "6");

        Test.ensureTrue(a.increment(), "7");
        Test.ensureEqual(a.getIndex(), 7, "7");
        Test.ensureEqual(a.getCurrent(), new int[]{1,0,1}, "7");

        Test.ensureTrue(a.increment(), "8");
        Test.ensureEqual(a.getIndex(), 8, "8");
        Test.ensureEqual(a.getCurrent(), new int[]{1,0,2}, "8");

        Test.ensureTrue(a.increment(), "9");
        Test.ensureEqual(a.getIndex(), 9, "9");
        Test.ensureEqual(a.getCurrent(), new int[]{1,1,0}, "9");

        Test.ensureTrue(a.increment(), "10");
        Test.ensureEqual(a.getIndex(), 10, "10");
        Test.ensureEqual(a.getCurrent(), new int[]{1,1,1}, "10");

        Test.ensureTrue(a.increment(), "11");
        Test.ensureEqual(a.getIndex(), 11, "11");
        Test.ensureEqual(a.getCurrent(), new int[]{1,1,2}, "11");

        Test.ensureTrue(!a.increment(), "12");
        Test.ensureEqual(a.getIndex(), 12, "12");

        a.reset();
        Test.ensureTrue(a.increment(), "0");
        Test.ensureEqual(a.getIndex(), 0, "0");
        Test.ensureEqual(a.getCurrent(), new int[]{0,0,0}, "0");

        //test incrementCM    //shape = 2,2,3   factors  6,3,1
        a.reset();
        Test.ensureTrue(a.incrementCM(), "0");
        Test.ensureEqual(a.getIndex(), 0, "0");
        Test.ensureEqual(a.getCurrent(), new int[]{0,0,0}, "0");

        Test.ensureTrue(a.incrementCM(), "1");
        Test.ensureEqual(a.getIndex(), 6, "1");
        Test.ensureEqual(a.getCurrent(), new int[]{1,0,0}, "1");

        Test.ensureTrue(a.incrementCM(), "2");
        Test.ensureEqual(a.getIndex(), 3, "2");
        Test.ensureEqual(a.getCurrent(), new int[]{0,1,0}, "2");

        Test.ensureTrue(a.incrementCM(), "3");
        Test.ensureEqual(a.getIndex(), 9, "3");
        Test.ensureEqual(a.getCurrent(), new int[]{1,1,0}, "3");

        Test.ensureTrue(a.incrementCM(), "4");
        Test.ensureEqual(a.getIndex(), 1, "4");
        Test.ensureEqual(a.getCurrent(), new int[]{0,0,1}, "4");

        Test.ensureTrue(a.incrementCM(), "5");
        Test.ensureEqual(a.getIndex(), 7, "5");
        Test.ensureEqual(a.getCurrent(), new int[]{1,0,1}, "5");

        Test.ensureTrue(a.incrementCM(), "6");
        Test.ensureEqual(a.getIndex(), 4, "6");
        Test.ensureEqual(a.getCurrent(), new int[]{0,1,1}, "6");

        Test.ensureTrue(a.incrementCM(), "7");
        Test.ensureEqual(a.getIndex(), 10, "7");
        Test.ensureEqual(a.getCurrent(), new int[]{1,1,1}, "7");

        Test.ensureTrue(a.incrementCM(), "8");
        Test.ensureEqual(a.getIndex(), 2, "8");
        Test.ensureEqual(a.getCurrent(), new int[]{0,0,2}, "8");

        Test.ensureTrue(a.incrementCM(), "9");
        Test.ensureEqual(a.getIndex(), 8, "9");
        Test.ensureEqual(a.getCurrent(), new int[]{1,0,2}, "9");

        Test.ensureTrue(a.incrementCM(), "10");
        Test.ensureEqual(a.getIndex(), 5, "10");
        Test.ensureEqual(a.getCurrent(), new int[]{0,1,2}, "10");

        Test.ensureTrue(a.incrementCM(), "11");
        Test.ensureEqual(a.getIndex(), 11, "11");
        Test.ensureEqual(a.getCurrent(), new int[]{1,1,2}, "11");

        Test.ensureTrue(!a.incrementCM(), "12");
        Test.ensureEqual(a.getIndex(), 12, "12");

        //test some invalid requests
        try {
            NDimensionalIndex b = new NDimensionalIndex(new int[0]);
            throw new Exception("");            
        } catch (Exception e) {
            if (e.toString().indexOf("nDimensions=0") < 0) 
                throw e;
        }
        try {
            NDimensionalIndex b = new NDimensionalIndex(new int[]{2,0});
            throw new Exception("");            
        } catch (Exception e) {
            if (e.toString().indexOf("value less than 1") < 0) 
                throw e;
        }

        try {
            a.setIndex(-1);
            throw new Exception("");            
        } catch (Exception e) {
            if (e.toString().indexOf("less than 0") < 0) 
                throw e;
        }
        try {
            a.setCurrent(new int[]{1,1});
            throw new Exception("");            
        } catch (Exception e) {
            if (e.toString().indexOf("isn't 3") < 0) 
                throw e;
        }

        try {
            a.setCurrent(new int[]{1,-1,1});
            throw new Exception("");            
        } catch (Exception e) {
            if (e.toString().indexOf("is invalid") < 0) 
                throw e;
        }

        //test get/set index/current
        Test.ensureEqual(a.setCurrent(new int[]{1,1,2}), 11, "");
        Test.ensureEqual(a.getIndex(), 11, "");
        Test.ensureEqual(a.setIndex(9), new int[]{1,1,0}, "");
        Test.ensureEqual(a.getCurrent(), new int[]{1,1,0}, "");
     
    }

    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 1;
        String msg = "\n^^^ NDimensionalIndex.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) basicTest();
                    if (test ==  1) testSubsetIndex();
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }

}
