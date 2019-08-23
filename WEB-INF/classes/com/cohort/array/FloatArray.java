/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * FloatArray is a thin shell over a float[] with methods like ArrayList's 
 * methods; it extends PrimitiveArray.
 */
public class FloatArray extends PrimitiveArray {

    public final static FloatArray MV9 = new FloatArray(DoubleArray.MV9);

    /**
     * This is the main data structure.
     * This should be private, but is public so you can manipulate it if you 
     * promise to be careful.
     * Note that if the PrimitiveArray's capacity is increased,
     * the PrimitiveArray will use a different array for storage.
     */
    public float[] array;

    /**
     * A constructor for a capacity of 8 elements. The initial 'size' will be 0.
     */
    public FloatArray() {
        array = new float[8];
    }

    /**
     * This constructs a FloatArray by copying elements from the incoming
     * PrimitiveArray (using append()).
     *
     * @param primitiveArray a primitiveArray of any type 
     */
    public FloatArray(PrimitiveArray primitiveArray) {
        array = new float[primitiveArray.size()]; //exact size
        append(primitiveArray);
    }

    /**
     * A constructor for a specified number of elements. The initial 'size' will be 0.
     *
     * @param capacity creates an FloatArray with the specified initial capacity.
     * @param active if true, size will be set to capacity and all elements 
     *    will equal 0; else size = 0.
     */
    public FloatArray(int capacity, boolean active) {
        Math2.ensureMemoryAvailable(4L * capacity, "FloatArray");
        array = new float[capacity];
        if (active) 
            size = capacity;
    }

    /**
     * A constructor which (at least initially) uses the array and all 
     * its elements ('size' will equal anArray.length).
     *
     * @param anArray the array to be used as this object's array.
     */
    public FloatArray(float[] anArray) {
        array = anArray;
        size = anArray.length;
    }

    /** The minimum value that can be held by this class. */
    public String MINEST_VALUE() {return "" + -Float.MAX_VALUE;}

    /** The maximum value that can be held by this class 
        (not including the cohort missing value). */
    public String MAXEST_VALUE() {return "" + Float.MAX_VALUE;}

    /**
     * This returns the current capacity (number of elements) of the internal data array.
     * 
     * @return the current capacity (number of elements) of the internal data array.
     */
    public int capacity() {
        return array.length;
    }

    /** This indicates if this class' type is float.class or double.class. 
     */
    public boolean isFloatingPointType() {
        return true;
    }

    /**
     * This returns the hashcode for this byteArray (dependent only on values,
     * not capacity).
     * WARNING: the algorithm used may change in future versions.
     *
     * @return the hashcode for this byteArray (dependent only on values,
     * not capacity)
     */
    public int hashCode() {
        //see https://docs.oracle.com/javase/8/docs/api/java/util/List.html#hashCode()
        //and https://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
        int code = 0;
        for (int i = 0; i < size; i++)
            code = 31*code + Float.floatToIntBits(array[i]);
        return code;
    }

    /**
     * This makes a new subset of this PrimitiveArray based on startIndex, stride,
     * and stopIndex.
     *
     * @param pa the pa to be filled (may be null). If not null, must be of same type as this class. 
     * @param startIndex must be a valid index
     * @param stride   must be at least 1
     * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
     * @return The same pa (or a new PrimitiveArray if it was null) with the desired subset.
     *    If new, it will have a backing array with a capacity equal to its size.
     *    If stopIndex &lt; startIndex, this returns PrimitiveArray with size=0;
     */
    public PrimitiveArray subset(PrimitiveArray pa, int startIndex, int stride, int stopIndex) {
        if (pa != null)
            pa.clear();
        if (startIndex < 0)
            throw new IndexOutOfBoundsException(MessageFormat.format(
                ArraySubsetStart, getClass().getSimpleName(), "" + startIndex));
        if (stride < 1)
            throw new IllegalArgumentException(MessageFormat.format(
                ArraySubsetStride, getClass().getSimpleName(), "" + stride));
        if (stopIndex >= size)
            stopIndex = size - 1;
        if (stopIndex < startIndex)
            return pa == null? new FloatArray(new float[0]) : pa;

        int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
        FloatArray fa = null;
        if (pa == null) {
            fa = new FloatArray(willFind, true);
        } else {
            fa = (FloatArray)pa;
            fa.ensureCapacity(willFind);
            fa.size = willFind;
        }
        float tar[] = fa.array;
        if (stride == 1) {
            System.arraycopy(array, startIndex, tar, 0, willFind);
        } else {
            int po = 0;
            for (int i = startIndex; i <= stopIndex; i+=stride) 
                tar[po++] = array[i];
        }
        return fa;
    }

    /**
     * This returns the class (float.class) of the element type.
     *
     * @return the class (float.class) of the element type.
     */
    public Class elementClass() {
        return float.class;
    }

    /**
     * This returns the class index (CLASS_INDEX_FLOAT) of the element type.
     *
     * @return the class index (CLASS_INDEX_FLOAT) of the element type.
     */
    public int elementClassIndex() {
        return CLASS_INDEX_FLOAT;
    }

    /**
     * This adds an item to the array (increasing 'size' by 1).
     *
     * @param value the value to be added to the array
     */
    public void add(float value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        array[size++] = value;
    }

    /**
     * This adds all the values from ar.
     *
     * @param ar an array
     */
    public void add(float ar[]) {
        int arSize = ar.length; 
        ensureCapacity(size + (long)arSize);
        System.arraycopy(ar, 0, array, size, arSize);
        size += arSize;
    }    

    /**
     * This adds n copies of value to the array (increasing 'size' by n).
     *
     * @param n  if less than 0, this throws Exception
     * @param value the value to be added to the array.
     *    n &lt; 0 throws an Exception.
     */
    public void addN(int n, float value) {
        if (n == 0) return;
        if (n < 0)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayAddN, getClass().getSimpleName(), "" + n));
        ensureCapacity(size + (long)n);
        Arrays.fill(array, size, size + n, value);
        size += n;
    }

    /**
     * This inserts an item into the array at the specified index, 
     * pushing subsequent items to oldIndex+1 and increasing 'size' by 1.
     *
     * @param index the position where the value should be inserted.
     * @param value the value to be inserted into the array
     */
    public void atInsert(int index, float value) {
        if (index < 0 || index > size)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayAtInsert, getClass().getSimpleName(), "" + index, "" + size));
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        System.arraycopy(array, index, array, index + 1, size - index);
        size++;
        array[index] = value;
    }

    /**
     * This inserts an item into the array at the specified index, 
     * pushing subsequent items to oldIndex+1 and increasing 'size' by 1.
     *
     * @param index 0..
     * @param value the value, as a String.
     */
    public void atInsertString(int index, String value) {
        atInsert(index, String2.parseFloat(value));
    }

    /**
     * This adds n Strings to the array.
     *
     * @param n the number of times 'value' should be added.
     *    If less than 0, this throws Exception.
     * @param value the value, as a String.
     */
    public void addNStrings(int n, String value) {
        addN(n, String2.parseFloat(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a String.
     */
    public void addString(String value) {
        add(String2.parseFloat(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the float value
     */
    public void addFloat(float value) {
        add(value);
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a double.
     */
    public void addDouble(double value) {
        add(Math2.doubleToFloatNaN(value));
    }

    /**
     * This adds n doubles to the array.
     *
     * @param n the number of times 'value' should be added.
     *    If less than 0, this throws Exception.
     * @param value the value, as a double.
     */
    public void addNDoubles(int n, double value) {
        addN(n, Math2.doubleToFloatNaN(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as an int.
     */
    public void addInt(int value) {
        add(value == Integer.MAX_VALUE? Float.NaN : value);
    }

    /**
     * This adds n ints to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as an int.
     */
    public void addNInts(int n, int value) {
        addN(n, value == Integer.MAX_VALUE? Float.NaN : value);
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a long.
     */
    public void addLong(long value) {
        add(value == Long.MAX_VALUE? Float.NaN : value);
    }

    /**
     * This adds an element from another PrimitiveArray.
     *
     * @param otherPA the source PA
     * @param otherIndex the start index in otherPA
     * @param nValues the number of values to be added
     * @return 'this' for convenience
     */
    public PrimitiveArray addFromPA(PrimitiveArray otherPA, int otherIndex, int nValues) {

        //add from same type
        if (otherPA.elementClass() == elementClass()) {
            if (otherIndex + nValues > otherPA.size)
                throw new IllegalArgumentException(String2.ERROR + 
                    " in FloatArray.addFromPA: otherIndex=" + otherIndex + 
                    " + nValues=" + nValues + 
                    " > otherPA.size=" + otherPA.size);
            ensureCapacity(size + nValues);            
            System.arraycopy(((FloatArray)otherPA).array, otherIndex, array, size, nValues);
            size += nValues;
            return this;
        }

        //add from different type
        for (int i = 0; i < nValues; i++)
            add(otherPA.getFloat(otherIndex++)); //does error checking
        return this;
    }

    /**
     * This sets an element from another PrimitiveArray.
     *
     * @param index the index to be set
     * @param otherPA
     * @param otherIndex
     */
    public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex) {
        set(index, otherPA.getFloat(otherIndex));
    }

    /**
     * This removes the specified element.
     *
     * @param index the element to be removed, 0 ... size-1
     */
    public void remove(int index) {
        if (index >= size)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayRemove, getClass().getSimpleName(), "" + index, "" + size));
        System.arraycopy(array, index + 1, array, index, size - index - 1);
        size--;

        //for object types, nullify the object at the end
    }

    /**
     * This removes the specified range of elements.
     *
     * @param from the first element to be removed, 0 ... size
     * @param to one after the last element to be removed, from ... size
     */
    public void removeRange(int from, int to) {
        if (to > size)
            throw new IllegalArgumentException(String2.ERROR + " in FloatArray.removeRange: to (" + 
                to + ") > size (" + size + ").");
        if (from >= to) {
            if (from == to) 
                return;
            throw new IllegalArgumentException(String2.ERROR + " in FloatArray.removeRange: from (" + 
                from + ") > to (" + to + ").");
        }
        System.arraycopy(array, to, array, from, size - to);
        size -= to - from;

        //for object types, nullify the objects at the end
    }

    /**
     * Moves elements 'first' through 'last' (inclusive)
     *   to 'destination'.
     *
     * @param first  the first to be move
     * @param last  (exclusive)
     * @param destination the destination, can't be in the range 'first+1..last-1'.
     */
    public void move(int first, int last, int destination) {
        String errorIn = String2.ERROR + " in FloatArray.move:\n";

        if (first < 0) 
            throw new RuntimeException(errorIn + "first (" + first + ") must be >= 0.");
        if (last < first || last > size)
            throw new RuntimeException( 
                errorIn + "last (" + last + ") must be >= first (" + first + 
                ") and <= size (" + size + ").");
        if (destination < 0 || destination > size)
            throw new RuntimeException( 
                errorIn + "destination (" + destination + 
                ") must be between 0 and size (" + size + ").");
        if (destination > first && destination < last)
            throw new RuntimeException(
              errorIn + "destination (" + destination + ") must be <= first (" + 
              first + ") or >= last (" + last + ").");
        if (first == last || destination == first || destination == last) 
            return; //nothing to do
        //String2.log("move first=" + first + " last=" + last + " dest=" + destination);
        //String2.log("move initial " + String2.toCSSVString(array));

        //store the range to be moved
        int nToMove = last - first;
        float[] temp = new float[nToMove];
        System.arraycopy(array, first, temp, 0, nToMove);

        //if moving to left...    (draw diagram to visualize this)
        if (destination < first) {
            System.arraycopy(array, destination, array, destination + nToMove, first - destination);
            //String2.log("move after shift " + String2.toCSSVString(array));

            //copy temp data into place
            System.arraycopy(temp, 0, array, destination, nToMove);
        } else {
            //moving to right
            System.arraycopy(array, last, array, first, destination - last);
            //String2.log("move after shift " + String2.toCSSVString(array));

            //copy temp data into place
            System.arraycopy(temp, 0, array, destination - nToMove, nToMove);
        }
        //String2.log("move done " + String2.toCSSVString(array));


    }

    /**
     * This just keeps the rows for the 'true' values in the bitset.
     * Rows that aren't kept are removed.
     * The resulting PrimitiveArray is compacted (i.e., it has a smaller size()).
     *
     * @param bitset
     */
    public void justKeep(BitSet bitset) {
        int newSize = 0;
        for (int row = 0; row < size; row++) {
            if (bitset.get(row)) 
                array[newSize++] = array[row];
        }
        removeRange(newSize, size);
    }    

    /**
     * This ensures that the capacity is at least 'minCapacity'.
     *
     * @param minCapacity the minimum acceptable capacity.
     *    minCapacity is type long, but &gt;= Integer.MAX_VALUE will throw exception.
     */
    public void ensureCapacity(long minCapacity) {
        if (array.length < minCapacity) {
            //ensure minCapacity is < Integer.MAX_VALUE
            Math2.ensureArraySizeOkay(minCapacity, "FloatArray");  
            //caller may know exact number needed, so don't double above 2x current size
            int newCapacity = (int)Math.min(Integer.MAX_VALUE - 1, array.length + (long)array.length); 
            if (newCapacity < minCapacity) 
                newCapacity = (int)minCapacity; //safe since checked above
            Math2.ensureMemoryAvailable(4L * newCapacity, "FloatArray");
            float[] newArray = new float[newCapacity];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray; //do last to minimize concurrency problems
        }
    }

    /**
     * This returns an array (perhaps 'array') which has 'size' elements.
     *
     * @return an array (perhaps 'array') which has 'size' elements.
     */
    public float[] toArray() {
        if (array.length == size)
            return array;
        Math2.ensureMemoryAvailable(4L * size, "FloatArray.toArray");
        float[] tArray = new float[size];
        System.arraycopy(array, 0, tArray, 0, size);
        return tArray;
    }
   
    /**
     * This returns a primitive[] (perhaps 'array') which has 'size' 
     * elements.
     *
     * @return a primitive[] (perhaps 'array') which has 'size' elements.
     */
    public Object toObjectArray() {
        return toArray();
    }

    /**
     * This returns a double[] (perhaps 'array') which has 'size' elements.
     * Note that the floats will be bruised, so you may want to use
     * Math2.niceDouble(f, 7) on all of the values.
     *
     * @return a double[] (perhaps 'array') which has 'size' elements.
     *   Float.MAX_VALUE is converted to Double.NaN.
     */
    public double[] toDoubleArray() {
        Math2.ensureMemoryAvailable(8L * size, "FloatArray.toDoubleArray");
        double dar[] = new double[size];
        for (int i = 0; i < size; i++)
            dar[i] = array[i];
        return dar;
    }

    /**
     * This returns a String[] which has 'size' elements.
     *
     * @return a String[] which has 'size' elements.
     *    If a value isn't finite, it appears as "".
     */
    public String[] toStringArray() {
        Math2.ensureMemoryAvailable(12L * size, "FloatArray.toStringArray"); //12L is feeble minimal estimate
        String sar[] = new String[size];
        for (int i = 0; i < size; i++) 
            sar[i] = getString(i);
        return sar;
    }

    /**
     * This gets a specified element.
     *
     * @param index 0 ... size-1
     */
    public float get(int index) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in FloatArray.get: index (" + 
                index + ") >= size (" + size + ").");
        return array[index];
    }

    /**
     * This sets a specified element.
     *
     * @param index 0 ... size-1
     * @param value the value for that element
     */
    public void set(int index, float value) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in FloatArray.set: index (" + 
                index + ") >= size (" + size + ").");
        array[index] = value;
    }


    /**
     * Return a value from the array as an int.
     * Floating point values are rounded.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as an int. This may return Integer.MAX_VALUE.
     */
    public int getInt(int index) {
        return Math2.roundToInt(get(index));
    }

    //getRawInt(index) uses default getInt(index) since missingValue must be converted

    /**
     * Set a value in the array as an int.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. Integer.MAX_VALUE is converted
     *   to this type's missing value.
     */
    public void setInt(int index, int i) {
        set(index, i == Integer.MAX_VALUE? Float.NaN : i);
    }

    /**
     * Return a value from the array as a long.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a long. 
     *   This may return Long.MAX_VALUE.
     */
    public long getLong(int index) {
        return Math2.roundToLong(get(index));
    }

    /**
     * Set a value in the array as a long.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. Long.MAX_VALUE is converted
     *   to Float.NaN.
     */
    public void setLong(int index, long i) {
        set(index, i == Long.MAX_VALUE? Float.NaN : i);
    }
    /**
     * Return a value from the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a float. String values are parsed
     *   with String2.parseFloat and so may return Float.NaN.
     */
    public float getFloat(int index) {
        return get(index);
    }

    /**
     * Set a value in the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value.
     */
    public void setFloat(int index, float d) {
        set(index, d);
    }

    /**
     * Return a value from the array as a double.
     * FloatArray converts float to double in a simplistic way.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     */
    public double getDouble(int index) {
        return get(index);
    }

    /**
     * Return a value from the array as a double.
     * FloatArray converts float to double via Math2.floatToDouble.
     * This is fine if e.g., 32.0000000001 becomes 32.0,
     * but not great if 32.83333 becomes 32.8333.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     */
    public double getNiceDouble(int index) {
        return Math2.floatToDouble(get(index));
    }

    /**
     * Return a value from the array as a double.
     * FloatArray converts float to double via Math2.floatToDouble.
     * This is fine if e.g., 32.0000000001 becomes 32.0,
     * but not great if 32.83333 becomes 32.8333.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     */
    public double getRawNiceDouble(int index) {
        return getNiceDouble(index);
    }

    /**
     * Set a value in the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. It is narrowed by Math2.doubleToFloatNaN(d),
     *   which converts INFINITY and large values to NaN.
     */
    public void setDouble(int index, double d) {
        set(index, Math2.doubleToFloatNaN(d));
    }

    /**
     * Return a value from the array as a String.
     * 
     * @param index the index number 0 .. 
     * @return For numeric types, this returns (String.valueOf(ar[index])), or "" for NaN or infinity.
     */
    public String getString(int index) {
        float b = get(index);
        return Float.isFinite(b)? String.valueOf(b) : "";
    }

    /**
     * Return a value from the array as a String suitable for a JSON file. 
     * char returns a String with 1 character.
     * String returns a json String with chars above 127 encoded as \\udddd.
     * 
     * @param index the index number 0 ... size-1 
     * @return For numeric types, this returns ("" + ar[index]), or null for NaN or infinity.
     */
    public String getJsonString(int index) {
        return String2.toJson(get(index));
    }

    /**
     * Set a value in the array as a String.
     * 
     * @param index the index number 0 .. 
     * @param s the value. For numeric PrimitiveArray's, it is parsed
     *   with String2.parse and narrowed if needed by methods like
     *   Math2.roundToFloat(d).
     */
    public void setString(int index, String s) {
        set(index, String2.parseFloat(s));
    }

    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for.
     *   This correctly handles NaN.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(float lookFor) {
        return indexOf(lookFor, 0);
    }


    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for.
     *   This correctly handles NaN.
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(float lookFor, int startIndex) {
        if (Float.isNaN(lookFor)) {
            for (int i = startIndex; i < size; i++) 
                if (Float.isNaN(array[i])) 
                    return i;
            return -1;
        }

        for (int i = startIndex; i < size; i++) 
            if (array[i] == lookFor) 
                return i;
        return -1;
    }

    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(String lookFor, int startIndex) {
        if (startIndex >= size)
            return -1;
        return indexOf(String2.parseFloat(lookFor), startIndex);
    }

    /**
     * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1. The search progresses towards 0.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int lastIndexOf(float lookFor, int startIndex) {
        if (startIndex >= size)
            throw new IllegalArgumentException(String2.ERROR + " in FloatArray.get: startIndex (" + 
                startIndex + ") >= size (" + size + ").");
        for (int i = startIndex; i >= 0; i--) 
            if (array[i] == lookFor) 
                return i;
        return -1;
    }

    /**
     * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1. The search progresses towards 0.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int lastIndexOf(String lookFor, int startIndex) {
        return lastIndexOf(String2.parseFloat(lookFor), startIndex);
    }

    /**
     * If size != capacity, this makes a new 'array' of size 'size'
     * so capacity will equal size.
     */
    public void trimToSize() {
        array = toArray();
    }

    /**
     * Test if o is an FloatArray with the same size and values.
     *
     * @param o
     * @return true if equal.  o=null returns false.
     */
    public boolean equals(Object o) {
        if (!(o instanceof FloatArray)) //handles o==null
            return false;
        FloatArray other = (FloatArray)o;
        if (other.size() != size)
            return false;
        for (int i = 0; i < size; i++)
            if (!Math2.equalsIncludingNanOrInfinite(array[i], other.array[i]))
                return false;
        return true;
    }

    /**
     * Test if o is an FloatArray with the same size and values,
     * but returns a String describing the difference (or "" if equal).
     *
     * @param o
     * @return a String describing the difference (or "" if equal).
     *   o=null throws an exception.
     */
    public String testEquals(Object o) {
        if (!(o instanceof FloatArray))
            return "The two objects aren't equal: this object is a FloatArray; the other is a " + 
                o.getClass().getName() + ".";
        FloatArray other = (FloatArray)o;
        if (other.size() != size)
            return "The two FloatArrays aren't equal: one has " + size + 
               " value(s); the other has " + other.size() + " value(s).";
        for (int i = 0; i < size; i++)
            if (!Math2.equalsIncludingNanOrInfinite(array[i], other.array[i]))
                return "The two FloatArrays aren't equal: this[" + i + "]=" + array[i] + 
                                                      "; other[" + i + "]=" + other.array[i] + ".";
        return "";
    }

    /** 
     * This converts the elements into a Comma-Space-Separated-Value (CSSV) String.
     *
     * @return a Comma-Space-Separated-Value (CSSV) String representation 
     */
    public String toString() {
        return String2.toCSSVString(toArray()); //toArray() get just 'size' elements
    }

    /** 
     * This converts the elements into an NCCSV attribute String, e.g.,: -128b, 127b
     *
     * @return an NCCSV attribute String
     */
    public String toNccsvAttString() {
        StringBuilder sb = new StringBuilder(size * 11);
        for (int i = 0; i < size; i++) 
            sb.append((i == 0? "" : ",") + array[i] + "f");
        return sb.toString();
    }

    /** 
     * This sorts the elements in ascending order.
     * To get the elements in reverse order, just read from the end of the list
     * to the beginning.
     */
    public void sort() {
        Arrays.sort(array, 0, size);
    }

    /**
     * This compares the values in row1 and row2 for SortComparator,
     * and returns a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.
     * Currently, this does not checking of the range of index1 and index2,
     * so the caller should be careful.
     *
     * @param index1 an index number 0 ... size-1
     * @param index2 an index number 0 ... size-1
     * @return returns a negative integer, zero, or a positive integer if the 
     *   value at index1 is less than, equal to, or greater than 
     *   the value at index2.  
     *   Think "array[index1] - array[index2]".
     */
    public int compare(int index1, int index2) {
        return Float.compare(array[index1], array[index2]);
    }

    /**
     * This copies the value in row 'from' to row 'to'.
     * This does not check that 'from' and 'to' are valid;
     * the caller should be careful.
     * The value for 'from' is unchanged.
     *
     * @param from an index number 0 ... size-1
     * @param to an index number 0 ... size-1
     */
    public void copy(int from, int to) {
        array[to] = array[from];
    }

    /**
     * This reorders the values in 'array' based on rank.
     *
     * @param rank is an int with values (0 ... size-1) 
     * which points to the row number for a row with a specific 
     * rank (e.g., rank[0] is the row number of the first item 
     * in the sorted list, rank[1] is the row number of the
     * second item in the sorted list, ...).
     */
    public void reorder(int rank[]) {
        int n = rank.length;
        //new length could be n, but I'll keep it the same array.length as before
        float newArray[] = new float[array.length]; 
        for (int i = 0; i < n; i++)
            newArray[i] = array[rank[i]];
        array = newArray;
    }

    /**
     * This reverses the order of the bytes in each value,
     * e.g., if the data was read from a little-endian source.
     */
    public void reverseBytes() {
        for (int i = 0; i < size; i++)
            //this probably fails for some values since not all bit combos are valid floats
            array[i] = Float.intBitsToFloat(Integer.reverseBytes(
                Float.floatToIntBits(array[i])));
    }

    /**
     * This writes 'size' elements to a DataOutputStream.
     *
     * @param dos the DataOutputStream
     * @return the number of bytes used per element (for Strings, this is
     *    the size of one of the strings, not others, and so is useless;
     *    for other types the value is consistent).
     *    But if size=0, this returns 0.
     * @throws Exception if trouble
     */
    public int writeDos(DataOutputStream dos) throws Exception {
        for (int i = 0; i < size; i++)
            dos.writeFloat(array[i]);
        return size == 0? 0 : 4;
    }

    /**
     * This writes one element to a DataOutputStream.
     *
     * @param dos the DataOutputStream
     * @param i the index of the element to be written
     * @return the number of bytes used for this element
     *    (for Strings, this varies; for others it is consistent)
     * @throws Exception if trouble
     */
    public int writeDos(DataOutputStream dos, int i) throws Exception {
        dos.writeFloat(array[i]);
        return 4;
    }

    /**
     * This reads/adds n elements from a DataInputStream.
     *
     * @param dis the DataInputStream
     * @param n the number of elements to be read/added
     * @throws Exception if trouble
     */
    public void readDis(DataInputStream dis, int n) throws Exception {
        ensureCapacity(size + (long)n);
        for (int i = 0; i < n; i++)
            array[size++] = dis.readFloat();
    }

    /**
     * This reads/appends float values to this PrimitiveArray from a DODS DataInputStream,
     * and is thus the complement of externalizeForDODS.
     *
     * @param dis
     * @throws IOException if trouble
     */
    public void internalizeFromDODS(DataInputStream dis) throws java.io.IOException {
        int nValues = dis.readInt();
        dis.readInt(); //skip duplicate of nValues
        ensureCapacity(size + (long)nValues);
        for (int i = 0; i < nValues; i++) 
            array[size++] = dis.readFloat();
    }

    /**
     * This reads one value from a randomAccessFile.
     *
     * @param raf the RandomAccessFile
     * @param start the raf offset of the start of the array (nBytes)
     * @param index the index of the desired value (0..)
     * @return the requested value as a double
     * @throws Exception if trouble
     */
    public static double rafReadDouble(RandomAccessFile raf, long start, long index) 
        throws Exception {
 
        raf.seek(start + 4*index);
        return raf.readFloat();
    }

    /**
     * This writes one value to a randomAccessFile at the current position.
     *
     * @param raf the RandomAccessFile
     * @param value the value which will be converted to this PrimitiveArray's 
     *    type and then stored
     * @throws Exception if trouble
     */
    public static void rafWriteDouble(RandomAccessFile raf, double value) throws Exception {
        raf.writeFloat(Math2.doubleToFloatNaN(value));
    }

    /**
     * This writes one value to a randomAccessFile.
     *
     * @param raf the RandomAccessFile
     * @param start the raf offset of the start of the array (nBytes)
     * @param index the index of the desired value (0..)
     * @param value the value which will be converted to this PrimitiveArray's 
     *    type and then stored
     * @throws Exception if trouble
     */
    public static void rafWriteDouble(RandomAccessFile raf, long start, long index,
        double value) throws Exception {
 
        raf.seek(start + 4*index);
        raf.writeFloat(Math2.doubleToFloatNaN(value));
    }

    /**
     * This appends the data in another pa to the current data.
     * WARNING: information may be lost from the incoming pa if this
     * primitiveArray is of a simpler type.
     *
     * @param pa pa must be the same or a narrower 
     *  data type, or the data will be narrowed with Math2.doubleToFloatNaN(pa.getDouble).
     */
    public void append(PrimitiveArray pa) {
        int otherSize = pa.size(); 
        ensureCapacity(size + (long)otherSize);
        if (pa instanceof FloatArray) {
            System.arraycopy(((FloatArray)pa).array, 0, array, size, otherSize);
        } else {
            for (int i = 0; i < otherSize; i++)
                array[size + i] = Math2.doubleToFloatNaN(pa.getDouble(i)); //this converts mv's
        }
        size += otherSize; //do last to minimize concurrency problems
    }    

    /**
     * This appends the data in another pa to the current data.
     * This "raw" variant leaves missingValue from smaller data types 
     * (e.g., ByteArray missingValue=127) AS IS.
     * WARNING: information may be lost from the incoming pa if this
     * primitiveArray is of a simpler type.
     *
     * @param pa pa must be the same or a narrower 
     *  data type, or the data will be narrowed with Math2.doubleToFloatNaN(pa.getDouble).
     */
    public void rawAppend(PrimitiveArray pa) {
        int otherSize = pa.size(); 
        ensureCapacity(size + (long)otherSize);
        if (pa instanceof FloatArray) {
            System.arraycopy(((FloatArray)pa).array, 0, array, size, otherSize);
        } else {
            for (int i = 0; i < otherSize; i++)
                array[size + i] = Math2.doubleToFloatNaN(pa.getRawDouble(i)); //this DOESN'T convert mv's
        }
        size += otherSize; //do last to minimize concurrency problems
    }    

    /**
     * This populates 'indices' with the indices (ranks) of the values in this FloatArray
     * (ties get the same index). For example, 10,10,25,3 returns 1,1,2,0.
     *
     * @param indices the intArray that will capture the indices of the values 
     *  (ties get the same index). For example, 10,10,25,3 returns 1,1,2,0.
     * @return a PrimitveArray (the same type as this class) with the unique values, sorted.
     *     If all the values are unique and already sorted, this returns 'this'.
     */
    public PrimitiveArray makeIndices(IntArray indices) {
        indices.clear();
        if (size == 0) {
            return new FloatArray();
        }

        //make a hashMap with all the unique values (associated values are initially all dummy)
        Integer dummy = new Integer(-1);
        HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
        float lastValue = array[0]; //since lastValue often equals currentValue, cache it
        hashMap.put(new Float(lastValue), dummy);
        boolean alreadySorted = true;
        for (int i = 1; i < size; i++) {
            float currentValue = array[i];
            if (currentValue != lastValue) {
                if (currentValue < lastValue) 
                    alreadySorted = false;
                lastValue = currentValue;
                hashMap.put(new Float(lastValue), dummy);
            }
        }

        //quickly deal with: all unique and already sorted
        Set keySet = hashMap.keySet();
        int nUnique = keySet.size();
        if (nUnique == size && alreadySorted) {
            indices.ensureCapacity(size);
            for (int i = 0; i < size; i++)
                indices.add(i);           
            return this; //the PrimitiveArray with unique values
        }

        //store all the elements in an array
        Object unique[] = new Object[nUnique];
        Iterator iterator = keySet.iterator();
        int count = 0;
        while (iterator.hasNext())
            unique[count++] = iterator.next();
        if (nUnique != count)
            throw new RuntimeException("FloatArray.makeRankArray nUnique(" + nUnique +
                ") != count(" + count + ")!");

        //sort them
        Arrays.sort(unique);

        //put the unique values back in the hashMap with the ranks as the associated values
        //and make tUnique 
        float tUnique[] = new float[nUnique];
        for (int i = 0; i < count; i++) {
            hashMap.put(unique[i], new Integer(i));
            tUnique[i] = ((Float)unique[i]).floatValue();
        }

        //convert original values to ranks
        int ranks[] = new int[size];
        lastValue = array[0];
        ranks[0] = ((Integer)hashMap.get(new Float(lastValue))).intValue();
        int lastRank = ranks[0];
        for (int i = 1; i < size; i++) {
            if (array[i] == lastValue) {
                ranks[i] = lastRank;
            } else {
                lastValue = array[i];
                ranks[i] = ((Integer)hashMap.get(new Float(lastValue))).intValue();
                lastRank = ranks[i];
            }
        }

        //store the results in ranked
        indices.append(new IntArray(ranks));

        return new FloatArray(tUnique);

    }

    /**
     * This changes all instances of the first value to the second value.
     *
     * @param tFrom the original value (use "" or "NaN"  for standard missingValue)
     * @param tTo   the new value (use "" or "NaN"  for standard missingValue)
     * @return the number of values switched
     */
    public int switchFromTo(String tFrom, String tTo) {
        float from = String2.parseFloat(tFrom);
        float to   = String2.parseFloat(tTo);
        if ((Float.isNaN(from) && Float.isNaN(to)) ||
            (from == to))
            return 0;
        int count = 0;
        if (Float.isNaN(from)) {
            for (int i = 0; i < size; i++) {
                if (Float.isNaN(array[i])) {
                    array[i] = to;
                    count++;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                //String2.log(">> float.switchFromTo from=" + from + " to=" + to + " i=" + i + " a[i]=" + array[i] + " eq5=" + Math2.almostEqual(5, array[i], from));
                if (Math2.almostEqual(5, array[i], from)) {
                    array[i] = to;
                    count++;
                }
            }
        }
        //String2.log("FloatArray.switch from=" + tFrom + " to=" + tTo + " n=" + count);
        return count;
    }

    /** 
     * This converts a double[] to a float[].
     *
     * @param dar
     * @return the corresponding float[]
     */
    public static float[] toArrayOfFloats(double dar[]) {
        int n = dar.length;
        float far[] = new float[n];
        for (int i = 0; i < n; i++) 
            far[i] = (float)dar[i];
        return far;
    }

    /**
     * This tests if the values in the array are sorted in ascending order (tied is ok).
     * The details of this test are geared toward determining if the 
     * values are suitable for binarySearch.
     *
     * @return "" if the values in the array are sorted in ascending order (or tied);
     *   or an error message if not (i.e., if descending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     *   A missing value returns an error message.
     */
    public String isAscending() {
        if (size == 0)
            return "";
        if (!Float.isFinite(array[0]))
            return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                "[0]=" + array[0]);
        for (int i = 1; i < size; i++) {
            if (!Float.isFinite(array[i]))
                return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                    "[" + i + "]=" + array[i]);
            if (array[i - 1] > array[i]) {
                return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                    "[" + (i-1) + "]=" + array[i-1] + " > [" + i + "]=" + array[i]);
            }
        }
        return "";
    }

    /**
     * This tests if the values in the array are sorted in descending order (tied is ok).
     *
     * @return "" if the values in the array are sorted in descending order (or tied);
     *   or an error message if not (i.e., if ascending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     *   A missing value returns an error message.
     */
    public String isDescending() {
        if (size == 0)
            return "";
        if (!Float.isFinite(array[0]))
            return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                "[0]=" + array[0]);
        for (int i = 1; i < size; i++) {
            if (!Float.isFinite(array[i]))
                return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                    "[" + i + "]=" + array[i]);
            if (array[i - 1] < array[i]) {
                return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                    "[" + (i-1) + "]=" + array[i-1] + 
                     " < [" + i + "]=" + array[i]);
            }
        }
        return "";
    }

    /**
     * This tests for adjacent tied values and returns the index of the first tied value.
     * Adjacent NaNs are treated as ties.
     *
     * @return the index of the first tied value (or -1 if none).
     */
    public int firstTie() {
        for (int i = 1; i < size; i++) {
            if (Double.isNaN(array[i - 1])) {
                if (Double.isNaN(array[i]))
                    return i - 1;
            } else if (array[i - 1] == array[i]) {
                return i - 1;
            }
        }
        return -1;
    }

    /**
     * This tests if the values in the array are evenly spaced (ascending or descending)
     * (via Math2.almostEqual4, or easier test if first 6 digits are same).
     *
     * @return "" if the values in the array are evenly spaced;
     *   or an error message if not.
     *   If size is 0 or 1, this returns "".
     */
    public String isEvenlySpaced() {
        if (size <= 2)
            return "";
        //average is closer to exact than first diff
        //and usually detects not-evenly-spaced anywhere in the array on first test!
        float average = (array[size-1] - array[0]) / (size - 1); 
        for (int i = 1; i < size; i++) {
            //This is a difficult test to do well. See tests below.
            if (Math2.almostEqual( 4, array[i] - array[i - 1], average)) { 
                //String2.log(i + " passed first test");
            } else if (
                //do easier test if first 6 digits are same
                Math2.almostEqual(6, array[i - 1] + average, array[i]) && 
                Math2.almostEqual(2, array[i] - array[i - 1], average)) { 
                //String2.log(i + " passed second test " + (array[i] - array[i - 1]) + " " + diff);
            } else {
                return MessageFormat.format(ArrayNotEvenlySpaced, getClass().getSimpleName(),
                    "" + (i - 1), "" + array[i - 1], "" + i, "" + array[i],
                    "" + (array[i] - array[i-1]), "" + average);
            }
        }

        return "";
    }

    /**
     * This tests if the values in the array are crudely evenly spaced (ascending or descending).
     *
     * @return "" if the values in the array are evenly spaced;
     *   or an error message if not.
     *   If size is 0 or 1, this returns "".
     */
    public String isCrudelyEvenlySpaced() {
        if (size <= 2)
            return "";
        float diff = array[1] - array[0];
        for (int i = 2; i < size; i++) {
            //This is a difficult test to do well. See tests below.
            //1e7 avoids fEps test in almostEqual
            if (
                //do easier test if first 3 digits are same
                Math2.almostEqual(3, array[i - 1] + diff, array[i]) && 
                Math2.almostEqual(2, (array[i] - array[i - 1]) * 1e7, diff * 1e7)) { 
                //String2.log(i + " passed second test " + (array[i] - array[i - 1]) + " " + diff);
            } else {
                return MessageFormat.format(ArrayNotEvenlySpaced, getClass().getSimpleName(),
                    "" + (i - 1), "" + array[i - 1], "" + i, "" + array[i],
                    "" + (array[i] - array[i-1]), "" + diff);
            }
        }

        return "";
    }


    /**
     * This finds the number of non-missing values, and the index of the min and
     *    max value.
     *
     * @return int[3], [0]=the number of non-missing values, 
     *    [1]=index of min value (if tie, index of last found; -1 if all mv),
     *    [2]=index of max value (if tie, index of last found; -1 if all mv).
     */
    public int[] getNMinMaxIndex() {
        int n = 0, tmini = -1, tmaxi = -1;
        float tmin =  Float.MAX_VALUE;
        float tmax = -Float.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            float v = array[i];
            if (Float.isFinite(v)) {
                n++;
                if (v <= tmin) {tmini = i; tmin = v; }
                if (v >= tmax) {tmaxi = i; tmax = v; }
            }
        }
        return new int[]{n, tmini, tmaxi};
    }


    /**
     * This tests the methods of this class.
     *
     * @throws Throwable if trouble.
     */
    public static void test() throws Throwable{
        String2.log("*** Testing FloatArray");
/* for releases, this line should have open/close comment */

        //** test default constructor and many of the methods
        FloatArray anArray = new FloatArray();
        Test.ensureEqual(anArray.isIntegerType(), false, "");
        Test.ensureEqual(anArray.missingValue(), Double.NaN, "");
        anArray.addString("");
        Test.ensureEqual(anArray.get(0),               Float.NaN, "");
        Test.ensureEqual(anArray.getRawInt(0),         Integer.MAX_VALUE, "");
        Test.ensureEqual(anArray.getRawDouble(0),      Double.NaN, "");
        Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN, "");
        Test.ensureEqual(anArray.getRawNiceDouble(0),  Float.NaN, "");
        Test.ensureEqual(anArray.getRawString(0),      "", "");
        Test.ensureEqual(anArray.getInt(0),            Integer.MAX_VALUE, "");
        Test.ensureEqual(anArray.getDouble(0),         Double.NaN, "");
        Test.ensureEqual(anArray.getString(0), "", "");
        anArray.clear();

        anArray.add(0.1f);
        Test.ensureEqual(anArray.getDouble(0),         0.10000000149011612, "");
        Test.ensureEqual(anArray.getNiceDouble(0),     0.1, "");
        Test.ensureEqual(anArray.getRawNiceDouble(0),  0.1, "");
        anArray.clear();

        //unsignedFactory, which uses unsignedAppend
        anArray = (FloatArray)unsignedFactory(float.class, 
            new FloatArray(new float[] 
            {0, 1, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN, -1}));
        Test.ensureEqual(anArray.toString(), 
            "0.0, 1.0, 3.4028235E38, -3.4028235E38, 1.4E-45, NaN, -1.0", ""); // -> mv
        anArray.clear();        

        anArray = (FloatArray)unsignedFactory(float.class, 
            new ByteArray(new byte[] {0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), "0.0, 1.0, 127.0, 128.0, 255.0", "");
        anArray.clear();        

        anArray = (FloatArray)unsignedFactory(float.class, 
            new CharArray(new char[] {(char)0, (char)1, '\u7FFF', '\u8000', '\uFFFF'}));
        Test.ensureEqual(anArray.toString(), "0.0, 1.0, 32767.0, 32768.0, 65535.0", "");
        anArray.clear();        

        anArray = (FloatArray)unsignedFactory(float.class, 
            new ShortArray(new short[] {0, 1, Short.MAX_VALUE, Short.MIN_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), "0.0, 1.0, 32767.0, 32768.0, 65535.0", "");
        anArray.clear();        

        anArray = (FloatArray)unsignedFactory(float.class, 
            new IntArray(new int[] {0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), 
            // 0, 1,    2147483647,   2147483648,   4294967295
            "0.0, 1.0, 2.14748365E9, 2.14748365E9, 4.2949673E9", ""); //rounded/imprecise
        anArray.clear();        

        anArray = (FloatArray)unsignedFactory(float.class, 
            new LongArray(new long[] {0, 1, Long.MAX_VALUE, Long.MIN_VALUE, -1}));
        Test.ensureEqual(anArray.toString(), 
            "0.0, 1.0, 9.223372E18, 9.223372E18, 1.8446744E19", ""); //rounded/imprecise
        anArray.clear();        

        Test.ensureEqual(anArray.size(), 0, "");
        anArray.add(1e34f);
        Test.ensureEqual(anArray.size(), 1, "");
        Test.ensureEqual(anArray.get(0), 1e34f, ""); //'f' bruises it so they match
        Test.ensureEqual(anArray.getInt(0), Integer.MAX_VALUE, "");
        Test.ensureEqual(anArray.getFloat(0), 1e34f, ""); //'f' defines it as a float
        Test.ensureEqual(anArray.getDouble(0), 1e34f, ""); //'f' bruises it so they match
        Test.ensureEqual(anArray.getString(0), "1.0E34", "");
        Test.ensureEqual(anArray.elementClass(), float.class, "");
        float tArray[] = anArray.toArray();
        Test.ensureEqual(tArray, new float[]{1e34f}, "");

        //intentional errors
        try {anArray.get(1);              throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).", "");
        }
        try {anArray.set(1, 100);         throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getInt(1);           throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setInt(1, 100);      throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getLong(1);          throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setLong(1, 100);     throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getFloat(1);         throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setFloat(1, 100);    throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getDouble(1);        throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setDouble(1, 100);   throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getString(1);        throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setString(1, "100"); throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in FloatArray.set: index (1) >= size (1).", "");
        }

        //set NaN returned as NaN
        anArray.setDouble(0, Double.NaN);   Test.ensureEqual(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, -1e300);       Test.ensureEqual(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, 2.2);          Test.ensureEqual(anArray.getDouble(0), 2.200000047683716,"");//not ideal 
        anArray.setFloat( 0, Float.NaN);    Test.ensureEqual(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, -1e33f);       Test.ensureEqual(anArray.getFloat(0),  -1e33f, ""); 
        anArray.setFloat( 0, 3.3f);         Test.ensureEqual(anArray.getFloat(0),  3.3f,     ""); 
        anArray.setLong(0, Long.MAX_VALUE); Test.ensureEqual(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 9123456789L);    Test.ensureEqual(anArray.getLong(0),   9123457024L, ""); //not ideal  (loss of precision)
        anArray.setLong(0, 4);              Test.ensureEqual(anArray.getLong(0),   4, ""); 
        anArray.setInt(0,Integer.MAX_VALUE);Test.ensureEqual(anArray.getInt(0),    Integer.MAX_VALUE, ""); 
        anArray.setInt(0, 1123456789);      Test.ensureEqual(anArray.getInt(0),    1123456768, ""); //not ideal (loss of precision)
        anArray.setInt(0, 5);               Test.ensureEqual(anArray.getInt(0),    5, ""); 


        //** test capacity constructor, test expansion, test clear
        anArray = new FloatArray(2, false);
        Test.ensureEqual(anArray.size(), 0, "");
        for (int i = 0; i < 10; i++) {
            anArray.add(i);   
            Test.ensureEqual(anArray.get(i), i, "");
            Test.ensureEqual(anArray.size(), i+1, "");
        }
        Test.ensureEqual(anArray.size(), 10, "");
        anArray.clear();
        Test.ensureEqual(anArray.size(), 0, "");

        //active
        anArray = new FloatArray(3, true);
        Test.ensureEqual(anArray.size(), 3, "");
        Test.ensureEqual(anArray.get(2), 0, "");


        //** test array constructor
        anArray = new FloatArray(new float[]{0,2,4,6,8});
        Test.ensureEqual(anArray.size(), 5, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1), 2, "");
        Test.ensureEqual(anArray.get(2), 4, "");
        Test.ensureEqual(anArray.get(3), 6, "");
        Test.ensureEqual(anArray.get(4), 8, "");

        //test compare
        Test.ensureEqual(anArray.compare(1, 3), -1, "");
        Test.ensureEqual(anArray.compare(1, 1),  0, "");
        Test.ensureEqual(anArray.compare(3, 1),  1, "");

        //test toString
        Test.ensureEqual(anArray.toString(), "0.0, 2.0, 4.0, 6.0, 8.0", "");

        //test calculateStats
        anArray.addString("");
        double stats[] = anArray.calculateStats();
        anArray.remove(5);
        Test.ensureEqual(stats[STATS_N], 5, "");
        Test.ensureEqual(stats[STATS_MIN], 0, "");
        Test.ensureEqual(stats[STATS_MAX], 8, "");
        Test.ensureEqual(stats[STATS_SUM], 20, "");

        //test indexOf(int) indexOf(String)
        Test.ensureEqual(anArray.indexOf(0, 0),  0, "");
        Test.ensureEqual(anArray.indexOf(0, 1), -1, "");
        Test.ensureEqual(anArray.indexOf(8, 0),  4, "");
        Test.ensureEqual(anArray.indexOf(9, 0), -1, "");

        Test.ensureEqual(anArray.indexOf("0", 0),  0, "");
        Test.ensureEqual(anArray.indexOf("0", 1), -1, "");
        Test.ensureEqual(anArray.indexOf("8", 0),  4, "");
        Test.ensureEqual(anArray.indexOf("9", 0), -1, "");

        //test remove
        anArray.remove(1);
        Test.ensureEqual(anArray.size(), 4, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1), 4, "");
        Test.ensureEqual(anArray.get(3), 8, "");

        //test atInsert(index, value)
        anArray.atInsert(1, 22);
        Test.ensureEqual(anArray.size(), 5, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1),22, "");
        Test.ensureEqual(anArray.get(2), 4, "");
        Test.ensureEqual(anArray.get(4), 8, "");
        anArray.remove(1);

        //test removeRange
        anArray.removeRange(4, 4); //make sure it is allowed
        anArray.removeRange(1, 3);
        Test.ensureEqual(anArray.size(), 2, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1), 8, "");

        //test (before trimToSize) that toString, toDoubleArray, and toStringArray use 'size'
        Test.ensureEqual(anArray.toString(), "0.0, 8.0", "");
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");
        Test.ensureEqual(anArray.toStringArray(), new String[]{"0.0", "8.0"}, "");

        //test trimToSize
        anArray.trimToSize();
        Test.ensureEqual(anArray.array.length, 2, "");

        //test equals
        FloatArray anArray2 = new FloatArray();
        anArray2.add(0); 
        Test.ensureEqual(anArray.testEquals("A String"), 
            "The two objects aren't equal: this object is a FloatArray; the other is a java.lang.String.", "");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two FloatArrays aren't equal: one has 2 value(s); the other has 1 value(s).", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.addString("7");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two FloatArrays aren't equal: this[1]=8.0; other[1]=7.0.", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.setString(1, "8");
        Test.ensureEqual(anArray.testEquals(anArray2), "", "");
        Test.ensureTrue(anArray.equals(anArray2), "");

        //test toObjectArray
        Test.ensureEqual(anArray.toArray(), anArray.toObjectArray(), "");

        //test toDoubleArray
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");

        //test reorder
        int rank[] = {1, 0};
        anArray.reorder(rank);
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{8, 0}, "");


        //** test append and clone
        anArray = new FloatArray(new float[]{1});
        anArray.append(new ByteArray(new byte[]{5, -5}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, -5}, "");
        anArray.append(new StringArray(new String[]{"a", "9"}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");
        anArray2 = (FloatArray)anArray.clone();
        Test.ensureEqual(anArray2.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");

        //test sort: ensure mv sorts high
        anArray = new FloatArray(new float[]{-1, 1, Float.NaN});
        anArray.sort();
        Test.ensureEqual(anArray.toString(), "-1.0, 1.0, NaN", "");

        //test move
        anArray = new FloatArray(new float[]{0,1,2,3,4});
        anArray.move(1,3,0);
        Test.ensureEqual(anArray.toArray(), new float[]{1,2,0,3,4}, "");

        anArray = new FloatArray(new float[]{0,1,2,3,4});
        anArray.move(1,2,4);
        Test.ensureEqual(anArray.toArray(), new float[]{0,2,3,1,4}, "");

        //move does nothing, but is allowed
        anArray = new FloatArray(new float[]{0,1,2,3,4});
        anArray.move(1,1,0);
        Test.ensureEqual(anArray.toArray(), new float[]{0,1,2,3,4}, "");
        anArray.move(1,2,1);
        Test.ensureEqual(anArray.toArray(), new float[]{0,1,2,3,4}, "");
        anArray.move(1,2,2);
        Test.ensureEqual(anArray.toArray(), new float[]{0,1,2,3,4}, "");
        anArray.move(5,5,0);
        Test.ensureEqual(anArray.toArray(), new float[]{0,1,2,3,4}, "");
        anArray.move(3,5,5);
        Test.ensureEqual(anArray.toArray(), new float[]{0,1,2,3,4}, "");

        //makeIndices
        anArray = new FloatArray(new float[] {25,1,1,10});
        IntArray indices = new IntArray();
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "1.0, 10.0, 25.0", "");
        Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

        anArray = new FloatArray(new float[] {35,35,Float.NaN,1,2});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "1.0, 2.0, 35.0, NaN", "");
        Test.ensureEqual(indices.toString(), "2, 2, 3, 0, 1", "");

        anArray = new FloatArray(new float[] {10,20,30,40});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "10.0, 20.0, 30.0, 40.0", "");
        Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

        //switchFromTo   switchToFakeMissingValue
        anArray = new FloatArray(new float[] {Float.NaN,1,2,Float.NaN,3,Float.NaN});
        Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
        Test.ensureEqual(anArray.toString(), "75.0, 1.0, 2.0, 75.0, 3.0, 75.0", "");
        anArray.switchFromTo("75", "");
        Test.ensureEqual(anArray.toString(), "NaN, 1.0, 2.0, NaN, 3.0, NaN", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 1, 4}, "");

        //addN
        anArray = new FloatArray(new float[] {25});
        anArray.addN(2, 5.0f);
        Test.ensureEqual(anArray.toString(), "25.0, 5.0, 5.0", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 2, 0}, "");

        //add array
        anArray.add(new float[]{17, 19});
        Test.ensureEqual(anArray.toString(), "25.0, 5.0, 5.0, 17.0, 19.0", "");

        //subset
        PrimitiveArray ss = anArray.subset(1, 3, 4);
        Test.ensureEqual(ss.toString(), "5.0, 19.0", "");
        ss = anArray.subset(0, 1, 0);
        Test.ensureEqual(ss.toString(), "25.0", "");
        ss = anArray.subset(0, 1, -1);
        Test.ensureEqual(ss.toString(), "", "");
        ss = anArray.subset(1, 1, 0);
        Test.ensureEqual(ss.toString(), "", "");

        ss.trimToSize();
        anArray.subset(ss, 1, 3, 4);
        Test.ensureEqual(ss.toString(), "5.0, 19.0", "");
        anArray.subset(ss, 0, 1, 0);
        Test.ensureEqual(ss.toString(), "25.0", "");
        anArray.subset(ss, 0, 1, -1);
        Test.ensureEqual(ss.toString(), "", "");
        anArray.subset(ss, 1, 1, 0);
        Test.ensureEqual(ss.toString(), "", "");

        //evenlySpaced
        String2.log("\nevenlySpaced test #1");
        anArray = new FloatArray(new float[] {10,20,30});
        Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
        String2.log("\nevenlySpaced test #2");
        anArray.set(2, 30.1f);
        Test.ensureEqual(anArray.isEvenlySpaced(), 
            "FloatArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.05.", "");
        Test.ensureEqual(anArray.smallestBiggestSpacing(),
            "  smallest spacing=10.0: [0]=10.0, [1]=20.0\n" +
            "  biggest  spacing=10.100000381469727: [1]=20.0, [2]=30.100000381469727", "");

        //these are unevenly spaced, but the secondary precision test allows it
        //should fail first test, but pass second test
        String2.log("\nevenlySpaced test #3");
        anArray = new FloatArray(new float[] {1.2306f, 1.2307f, 1.230801f});
        Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
        //but this should fail first and second test
        String2.log("\nevenlySpaced test #4");
        anArray.set(2, 1.23081f); 
        Test.ensureEqual(anArray.isEvenlySpaced(), 
            "FloatArray isn't evenly spaced: [0]=1.2306, [1]=1.2307, spacing=1.00016594E-4, average spacing=1.05023384E-4.", "");

        //isAscending
        anArray = new FloatArray(new float[] {10,10,30});
        Test.ensureEqual(anArray.isAscending(), "", "");
        anArray.set(2, Float.NaN);
        Test.ensureEqual(anArray.isAscending(), 
            "FloatArray isn't sorted in ascending order: [2]=NaN.", "");
        anArray.set(1, 9);
        Test.ensureEqual(anArray.isAscending(), 
            "FloatArray isn't sorted in ascending order: [0]=10.0 > [1]=9.0.", "");

        //isDescending
        anArray = new FloatArray(new float[] {30,10,10});
        Test.ensureEqual(anArray.isDescending(), "", "");
        anArray.set(2, Float.NaN);
        Test.ensureEqual(anArray.isDescending(), 
            "FloatArray isn't sorted in descending order: [2]=NaN.", "");
        anArray.set(1, 35f);
        Test.ensureEqual(anArray.isDescending(), 
            "FloatArray isn't sorted in descending order: [0]=30.0 < [1]=35.0.", "");

        //firstTie
        anArray = new FloatArray(new float[] {30,35,10});
        Test.ensureEqual(anArray.firstTie(), -1, "");
        anArray.set(1, 30f);
        Test.ensureEqual(anArray.firstTie(), 0, "");
        anArray.set(1, Float.NaN);
        Test.ensureEqual(anArray.firstTie(), -1, "");
        anArray.set(2, Float.NaN);
        Test.ensureEqual(anArray.firstTie(), 1, "");

        //hashcode
        anArray = new FloatArray();
        for (int i = 5; i < 1000; i++)
            anArray.add(i / 100.0f);
        String2.log("hashcode1=" + anArray.hashCode());
        anArray2 = (FloatArray)anArray.clone();
        Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
        anArray.atInsert(0, (float)2);
        Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

        //justKeep
        BitSet bitset = new BitSet();
        anArray = new FloatArray(new float[] {0, 11, 22, 33, 44});
        bitset.set(1);
        bitset.set(4);
        anArray.justKeep(bitset);
        Test.ensureEqual(anArray.toString(), "11.0, 44.0", "");

        //min max
        anArray = new FloatArray();
        anArray.addString(anArray.MINEST_VALUE());
        anArray.addString(anArray.MAXEST_VALUE());
        Test.ensureEqual(anArray.getString(0), anArray.MINEST_VALUE(), "");
        Test.ensureEqual(anArray.getString(0), "-3.4028235E38", "");
        Test.ensureEqual(anArray.getString(1), anArray.MAXEST_VALUE(), "");

        //tryToFindNumericMissingValue() 
        Test.ensureEqual((new FloatArray(new float[] {       })).tryToFindNumericMissingValue(), Double.NaN, "");
        Test.ensureEqual((new FloatArray(new float[] {1, 2   })).tryToFindNumericMissingValue(), Double.NaN, "");
        Test.ensureEqual((new FloatArray(new float[] {-1e37f })).tryToFindNumericMissingValue(), -1e37f, "");
        Test.ensureEqual((new FloatArray(new float[] { 1e37f })).tryToFindNumericMissingValue(),  1e37f, "");
        Test.ensureEqual((new FloatArray(new float[] {1, 99  })).tryToFindNumericMissingValue(),   99, "");
    }

}

