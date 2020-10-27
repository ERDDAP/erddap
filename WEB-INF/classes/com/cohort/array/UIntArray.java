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
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * UIntArray is a thin shell over an int[] with methods like ArrayList's 
 * methods; it extends PrimitiveArray.
 *
 * <p>This class uses maxIsMV=true and MAX_VALUE to represent a missing value (NaN).
 */
public class UIntArray extends PrimitiveArray {

    /** 
     * This is the minimum unsigned value, stored as a signed long.
     */
    public final static long MIN_VALUE = Math2.UINT_MIN_VALUE;

    /** 
     * This is the maximum unsigned value (the CoHort missing value), stored/packed as a signed int.
     */
    public final static int PACKED_MAX_VALUE = -1;

    /** 
     * This is the maximum unsigned value (the CoHort missing value), stored as a signed long.
     */
    public final static long MAX_VALUE = Math2.UINT_MAX_VALUE;

    /** This indicates if this class' type (e.g., PAType.SHORT) is an unsigned integer type. 
     * The unsigned integer type classes overwrite this.
     */
    public boolean isUnsigned() {
        return true;
    }

    /**
     * This returns the number of bytes per element for this PrimitiveArray.
     * The value for "String" isn't a constant, so this returns 20.
     *
     * @return the number of bytes per element for this PrimitiveArray.
     * The value for "String" isn't a constant, so this returns 20.
     */
    public int elementSize() {
        return 4;
    }

    /** 
     * This returns for cohort missing value for this class (e.g., Integer.MAX_VALUE), 
     * expressed as a double. FloatArray and StringArray return Double.NaN. 
     */
    public double missingValueAsDouble() {
        return MAX_VALUE;
    }

    /**
     * This tests if the value at the specified index equals the data type's MAX_VALUE 
     * (for integerTypes, which may or may not indicate a missing value,
     * depending on maxIsMV), NaN (for Float and Double), \\uffff (for CharArray),
     * or "" (for StringArray).
     *
     * @param index The index in question
     * @return true if the value is a missing value.
     */
    public boolean isMaxValue(int index) {
        return getPacked(index) == PACKED_MAX_VALUE;
    }

    /**
     * This tests if the value at the specified index is a missing value.
     * For integerTypes, isMissingValue can only be true if maxIsMv is 'true'.
     *
     * @param index The index in question
     * @return true if the value is a missing value.
     */
    public boolean isMissingValue(int index) {
        return maxIsMV && isMaxValue(index);
    }

    /**
     * This packs a signed long holding an unsigned int as a int.
     */
    public static int pack(long i) {
        return i < 0 || i >= MAX_VALUE? PACKED_MAX_VALUE : (int)(i & MAX_VALUE);
    }

    /**
     * This unpacks a uint stored in an int as a long.
     */
    public static long unpack(int i) {
        return i >= 0? i : i + MAX_VALUE + 1;
    }

    /**
     * This is the main data structure.
     * This should be private, but is public so you can manipulate it if you 
     * promise to be careful.
     * Note that if the PrimitiveArray's capacity is increased,
     * the PrimitiveArray will use a different array for storage.
     */
    public int[] array;

    /** This indicates if this class' type (e.g., PAType.SHORT) is an integer (in the math sense) type. 
     * The integer type classes overwrite this.
     */
    public boolean isIntegerType() {
        return true;
    }

    /**
     * A constructor for a capacity of 8 elements. The initial 'size' will be 0.
     */
    public UIntArray() {
        array = new int[8];
    }

    /**
     * This constructs a UIntArray by copying elements from the incoming
     * PrimitiveArray (using append()).
     *
     * @param primitiveArray a primitiveArray of any type 
     */
    public UIntArray(PrimitiveArray primitiveArray) {
        array = new int[primitiveArray.size()]; //exact size
        append(primitiveArray);
    }

    /**
     * A constructor for a specified number of elements. The initial 'size' will be 0.
     *
     * @param capacity creates an UIntArray with the specified initial capacity.
     * @param active if true, size will be set to capacity and all elements 
     *    will equal 0; else size = 0.
     */
    public UIntArray(int capacity, boolean active) {
        Math2.ensureMemoryAvailable(4L * capacity, "UIntArray");
        array = new int[capacity];
        if (active) 
            size = capacity;
    }

    /**
     * A special constructor for UIntArray: first, last, with an increment of 1.
     *
     * @param first the value of the first element.
     * @param last the value of the last element (inclusive!).
     */
    public UIntArray(int first, int last) {
        size = last - first + 1;
        array = new int[size];
        for (int i = 0; i < size; i++) 
            array[i] = pack(first + i);
    }

    /**
     * A constructor which (at least initially) uses the array and all its 
     * elements ('size' will equal anArray.length).
     *
     * @param anArray the array with already packed values to be used as this object's array.
     */
    public UIntArray(int[] anArray) {
        array = anArray;
        size = anArray.length;
    }

    /**
     * A constructor which does NOT use the array and all its 
     * elements ('size' will equal anArray.length).
     *
     * @param anArray the array with not-yet-packed values.
     */
    public UIntArray(long[] anArray) {
        size = anArray.length;
        Math2.ensureMemoryAvailable(4L * size, "UIntArray");
        array = new int[size];
        for (int i = 0; i < size; i++)
            array[i] = pack(anArray[i]);
    }
    
    /**
     * If this is an unsigned integer type, this makes a signed variant 
     * (e.g., PAType.UBYTE returns a PAType.BYTE).
     * The values from pa are then treated as unsigned, e.g., 255 in UByteArray  
     * becomes -1 in a ByteArray.
     *
     * @return a new unsigned PrimitiveArray, or this pa.
     */
    public PrimitiveArray makeSignedPA() {
        Math2.ensureMemoryAvailable(4L * size, "UIntArray");
        int ar[] = new int[size];
        System.arraycopy(array, 0, ar, 0, size);
        return new IntArray(ar);
    }

    /**
     * This makes a UIntArray from the comma-separated values.
     * <br>null becomes pa.length() == 0.
     * <br>"" becomes pa.length() == 0.
     * <br>" " becomes pa.length() == 1.
     * <br>See also PrimitiveArray.csvFactory(paType, csv);
     *
     * @param csv the comma-separated-value string
     * @return a UIntArray from the comma-separated values.
     */
    public static UIntArray fromCSV(String csv) {
        return (UIntArray)PrimitiveArray.csvFactory(PAType.UINT, csv);
    }

    /** This returns a new PAOne with the minimum value that can be held by this class. 
     *
     * @return a new PAOne with the minimum value that can be held by this class, e.g., -128b for ByteArray. 
     */
    public PAOne MINEST_VALUE() {return new PAOne(PAType.UINT).setLong(MIN_VALUE);}

    /** This returns a new PAOne with the maximum value that can be held by this class 
     *   (not including the cohort missing value).      
     *
     * @return a new PAOne with the maximum value that can be held by this class, e.g., 126 for ByteArray. 
     */
    public PAOne MAXEST_VALUE() {return new PAOne(PAType.UINT).setLong(MAX_VALUE - 1);}


    /**
     * This returns the current capacity (number of elements) of the internal data array.
     * 
     * @return the current capacity (number of elements) of the internal data array.
     */
    public int capacity() {
        return array.length;
    }

    /**
     * This returns the hashcode for this UIntArray (dependent only on values,
     * not capacity).
     * WARNING: the algorithm used may change in future versions.
     *
     * @return the hashcode for this UIntArray (dependent only on values,
     * not capacity)
     */
    public int hashCode() {
        //see https://docs.oracle.com/javase/8/docs/api/java/util/List.html#hashCode()
        //and https://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
        int code = 0;
        for (int i = 0; i < size; i++)
            code = 31*code + array[i];
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
            return pa == null? new UIntArray(new int[0]) : pa;  //no need to call .setMaxIsMV(maxIsMV) since size=0

        int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
        UIntArray ia = null;
        if (pa == null) {
            ia = new UIntArray(willFind, true);
        } else {
            ia = (UIntArray)pa;
            ia.ensureCapacity(willFind);
            ia.size = willFind;
        }
        int tar[] = ia.array;
        if (stride == 1) {
            System.arraycopy(array, startIndex, tar, 0, willFind);
        } else {
            int po = 0;
            for (int i = startIndex; i <= stopIndex; i+=stride) 
                tar[po++] = array[i];
        }
        return ia.setMaxIsMV(maxIsMV);
    }

    /**
     * This returns the PAType (PAType.INT) of the element type.
     *
     * @return the PAType (PAType.INT) of the element type.
     */
    public PAType elementType() {
        return PAType.UINT;
    }

    /**
     * This returns the minimum PAType needed to completely and precisely contain
     * the values in this PA's PAType and tPAType (e.g., when merging two PrimitiveArrays).
     *
     * @return the minimum PAType needed to completely and precisely contain
     * the values in this PA's PAType and tPAType (e.g., when merging two PrimitiveArrays).
     */
    public PAType needPAType(PAType tPAType) {
        //if tPAType is smaller or same, return this.PAType
        if (tPAType == PAType.UBYTE ||
            tPAType == PAType.USHORT ||
            tPAType == PAType.UINT)  return PAType.UINT;

        //if sideways
        if (tPAType == PAType.CHAR)  return PAType.STRING;
        if (tPAType == PAType.BYTE ||
            tPAType == PAType.SHORT ||
            tPAType == PAType.INT)   return PAType.LONG;
        if (tPAType == PAType.FLOAT) return PAType.STRING;

        //if tPAType is bigger.    LONG, ULONG, DOUBLE, STRING
        return tPAType;
    }

    /**
     * This adds an item to the array (increasing 'size' by 1).
     *
     * @param value the value to be added to the array
     */
    public void add(long value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        if (value < 0)
            maxIsMV = true;
        array[size++] = pack(value);
    }

    /**
     * This adds an already packed value to the array (increasing 'size' by 1).
     *
     * @param packedValue the already packed value to be added to the array
     */
    public void addPacked(int packedValue) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        array[size++] = packedValue;
    }

    /**
     * This adds an item to the array (increasing 'size' by 1).
     *
     * @param value the value to be added to the array.
     *    If value instanceof Number, this uses Number.intValue().
     *    (If you want a more sophisticated conversion, save to DoubleArray,
     *    then convert DoubleArray to UIntArray.)
     *    If null or not a Number, this adds MAX_VALUE.
     */
    public void addObject(Object value) {
        //double is good intermediate because it has the idea of NaN
        addDouble(value != null && value instanceof Number?
            ((Number)value).doubleValue() : Double.NaN); 
    }

    /**
     * This adds all the values from ar.
     *
     * @param ar an array
     */
    public void add(int ar[]) {
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
    public void addN(int n, long value) {
        if (n == 0) return;
        if (n < 0)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayAddN, getClass().getSimpleName(), "" + n));
        ensureCapacity(size + (long)n);
        Arrays.fill(array, size, size + n, pack(value));
        size += n;
    }

    /**
     * This inserts an item into the array at the specified index, 
     * pushing subsequent items to oldIndex+1 and increasing 'size' by 1.
     *
     * @param index the position where the value should be inserted.
     * @param value the value to be inserted into the array
     */
    public void atInsert(int index, long value) {
//String2.log(">>UIntArray index=" + index + " value=" + value + " size=" + size + " al=" + array.length);
        if (index < 0 || index > size)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayAtInsert, getClass().getSimpleName(), "" + index, "" + size));
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        System.arraycopy(array, index, array, index + 1, size - index);
        size++;
        array[index] = pack(value);
    }

    /**
     * This inserts an item into the array at the specified index, 
     * pushing subsequent items to oldIndex+1 and increasing 'size' by 1.
     *
     * @param index 0..
     * @param value the value, as a String.
     */
    public void atInsertString(int index, String value) {
        long tl = String2.parseLong(value); //NaN -> Long.MAX_VALUE
        if (tl < MIN_VALUE || tl > MAX_VALUE) {
            maxIsMV = true;
            atInsert(index, MAX_VALUE);
        } else {
            atInsert(index, tl);
        }
    }

    /**
     * This adds n PAOne's to the array.
     *
     * @param n the number of times 'value' should be added.
     *    If less than 0, this throws Exception.
     * @param value the value, as a PAOne (or null).
     */
    public void addNPAOnes(int n, PAOne value) {
        addNLongs(n, value == null? Long.MAX_VALUE : value.getLong());  //handles NaN and MV
    }

    /**
     * This adds n Strings to the array.
     *
     * @param n the number of times 'value' should be added.
     *    If less than 0, this throws Exception.
     * @param value the value, as a String.
     */
    public void addNStrings(int n, String value) {
        addNLongs(n, String2.parseLong(value)); //handles NaN and MV
    }

    /**
     * This adds n floats to the array.
     *
     * @param n the number of times 'value' should be added.
     *    If less than 0, this throws Exception.
     * @param value the value, as a float.
     */
    public void addNFloats(int n, float value) {
        if (!maxIsMV && (!Float.isFinite(value) || value < MIN_VALUE || value > MAX_VALUE)) 
            maxIsMV = true;
        addN(n, Math2.roundToUInt(value));
    }

    /**
     * This adds n doubles to the array.
     *
     * @param n the number of times 'value' should be added.
     *    If less than 0, this throws Exception.
     * @param value the value, as a double.
     */
    public void addNDoubles(int n, double value) {
        if (!maxIsMV && (!Double.isFinite(value) || value < MIN_VALUE || value > MAX_VALUE)) 
            maxIsMV = true;
        addN(n, Math2.roundToUInt(value));
    }

    /**
     * This adds n ints to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as an int.
     */
    public void addNInts(int n, int value) {
        if (value < MIN_VALUE) {
            maxIsMV = true;
            addN(n, MAX_VALUE);
        } else {
            addN(n, value);
        }
    }

    /**
     * This adds n longs to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as an int.
     */
    public void addNLongs(int n, long value) {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            maxIsMV = true;
            addN(n, MAX_VALUE);
        } else {
            addN(n, value);
        }
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
        if (otherPA.elementType() == elementType()) {
            if (otherIndex + nValues > otherPA.size)
                throw new IllegalArgumentException(String2.ERROR + 
                    " in UIntArray.addFromPA: otherIndex=" + otherIndex + 
                    " + nValues=" + nValues + 
                    " > otherPA.size=" + otherPA.size);
            ensureCapacity(size + nValues);            
            System.arraycopy(((UIntArray)otherPA).array, otherIndex, array, size, nValues);
            size += nValues;
            if (otherPA.getMaxIsMV()) 
                maxIsMV = true;
            return this;
        }

        //add from different type
        for (int i = 0; i < nValues; i++)
            addLong(otherPA.getLong(otherIndex++)); //does error checking and handles maxIsMV
        return this;
    }

    /**
     * This sets an element from another PrimitiveArray.
     *
     * @param index the index to be set
     * @param otherPA the other PrimitiveArray
     * @param otherIndex the index of the item in otherPA
     */
    public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex) {
        set(index, otherPA.getLong(otherIndex)); //handles maxIsMV
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
            throw new IllegalArgumentException(String2.ERROR + " in UIntArray.removeRange: to (" + 
                to + ") > size (" + size + ").");
        if (from >= to) {
            if (from == to) 
                return;
            throw new IllegalArgumentException(String2.ERROR + " in UIntArray.removeRange: from (" + 
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
        String errorIn = String2.ERROR + " in UIntArray.move:\n";

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
        int[] temp = new int[nToMove];
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
     * @param bitset The BitSet indicating which rows (indices) should be kept.
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
            Math2.ensureArraySizeOkay(minCapacity, "UIntArray");  
            //caller may know exact number needed, so don't double above 2x current size
            int newCapacity = (int)Math.min(Integer.MAX_VALUE - 1, array.length + (long)array.length); 
            if (newCapacity < minCapacity) 
                newCapacity = (int)minCapacity; //safe since checked above
            Math2.ensureMemoryAvailable(4L * newCapacity, "UIntArray");
            int[] newArray = new int[newCapacity];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray; //do last to minimize concurrency problems
        }
    }

    /**
     * This returns an array (perhaps 'array') which has 'size' elements.
     *
     * @return an array (perhaps 'array') which has 'size' elements.
     *   Unsigned integer types will return an array with their storage type
     *   e.g., ULongArray returns a long[].
     */
    public int[] toArray() {
        if (array.length == size)
            return array;
        Math2.ensureMemoryAvailable(4L * size, "UIntArray.toArray");
        int[] tArray = new int[size];
        System.arraycopy(array, 0, tArray, 0, size);
        return tArray;
    }
   
    /**
     * This returns a primitive[] (perhaps 'array') which has 'size' 
     * elements.
     *
     * @return a primitive[] (perhaps 'array') which has 'size' elements.
     *   Unsigned integer types will return an array with their storage type
     *   e.g., ULongArray returns a long[].
     */
    public Object toObjectArray() {
        return toArray();
    }

    /**
     * This returns a BitSet with 
     * the bit associated with each element in this UIntArray is set.
     * Negative numbers are skipped.
     * The bitSet will be sized so that that last bit is set.
     * For example, {1,4,5} will return 0,1,0,0,1,1
     *
     * @return an array (perhaps 'array') which has 'size' elements.
     */
    public BitSet toBitSet() {
        double stats[] = calculateStats();
        int max = Math2.roundToInt(stats[PrimitiveArray.STATS_MAX]);
        BitSet bitSet = new BitSet(max + 1);  //+1 so 0..max
        for (int i = 0; i < size; i++) {
            int ti = array[i];
            if (ti >= 0)
                bitSet.set(ti);
        }
        return bitSet;
    }
   
    /**
     * This returns a double[] (perhaps 'array') which has 'size' elements.
     *
     * @return a double[] (perhaps 'array') which has 'size' elements.
     *   If maxIsMV, MAX_VALUE is converted to Double.NaN.
     */
    public double[] toDoubleArray() {
        Math2.ensureMemoryAvailable(8L * size, "UIntArray.toDoubleArray");
        double dar[] = new double[size];
        for (int i = 0; i < size; i++) {
            long j = unpack(array[i]);
            dar[i] = maxIsMV && j == MAX_VALUE? Double.NaN : j;
        }
        return dar;
    }

    /**
     * This returns a String[] which has 'size' elements.
     *
     * @return a String[] which has 'size' elements.
     *    MAX_VALUE appears as "".
     */
    public String[] toStringArray() {
        Math2.ensureMemoryAvailable(8L * size, "UIntArray.toStringArray"); //8L is feeble minimal estimate
        String sar[] = new String[size];
        for (int i = 0; i < size; i++) {
            long j = unpack(array[i]);
            sar[i] = maxIsMV && j == MAX_VALUE? "" : String.valueOf(j);
        }
        return sar;
    }

    /**
     * This gets a specified element.
     *
     * @param index 0 ... size-1
     * @return the specified element
     */
    public long get(int index) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in UIntArray.get: index (" + 
                index + ") >= size (" + size + ").");
        return unpack(array[index]);
    }

    /**
     * This gets a specified element as a packed value.
     *
     * @param index 0 ... size-1
     * @return the specified element
     */
    public int getPacked(int index) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in UIntArray.get: index (" + 
                index + ") >= size (" + size + ").");
        return array[index];
    }

    /**
     * This sets a specified element.
     *
     * @param index 0 ... size-1
     * @param value the value for that element
     */
    public void set(int index, long value) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in UIntArray.set: index (" + 
                index + ") >= size (" + size + ").");
        array[index] = pack(value);
    }


    /**
     * Return a value from the array as an int.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as an int. 
     */
    public int getInt(int index) {
        long i = get(index);
        return i >= Integer.MAX_VALUE? Integer.MAX_VALUE : (int)i;
    }

    //getRawUInt(index) uses default getUInt(index) 

    /**
     * Set a value in the array as an int.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value.
     */
    public void setInt(int index, int i) {
        if (i < MIN_VALUE) {
            maxIsMV = true;
            set(index, MAX_VALUE);
        } else {
            set(index, i); 
        }
    }

    /**
     * Return a value from the array as a long.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a long. 
     *   If maxIsMV, MAX_VALUE is returned as Long.MAX_VALUE.
     */
    public long getLong(int index) {
        long i = get(index);
        return maxIsMV && i == MAX_VALUE? Long.MAX_VALUE : i;
    }

    /**
     * Set a value in the array as a long.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. For numeric PrimitiveArray's, it is narrowed 
     *   if needed by methods like Math2.narrowToUInt(long).
     */
    public void setLong(int index, long i) {
        if (i < MIN_VALUE || i > MAX_VALUE) {
            maxIsMV = true;
            set(index, MAX_VALUE);
        } else {
            set(index, i); 
        }
    }

    /**
     * Return a value from the array as a ulong.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a ulong. 
     *   If maxIsMV, MAX_VALUE is returned as null.
     */
    public BigInteger getULong(int index) {
        long b = get(index);
        return maxIsMV && b == MAX_VALUE? null : new BigInteger("" + b);
    }

    /**
     * Set a value in the array as a ulong.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. For numeric PrimitiveArray's, it is narrowed 
     *   if needed by methods like Math2.narrowToByte(long).
     */
    public void setULong(int index, BigInteger i) {
        setDouble(index, i == null? Double.NaN : i.doubleValue()); //easier to work with. handles NaN. wide range
    }


    /**
     * Return a value from the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a float. 
     *   String values are parsed
     *   with String2.parseFloat and so may return Float.NaN.
     *   If maxIsMV, MAX_VALUE is returned as Float.NaN.
     */
    public float getFloat(int index) {
        long i = get(index);
        return maxIsMV && i == MAX_VALUE? Float.NaN : i;
    }

    /**
     * Set a value in the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. For numeric PrimitiveArray, it is narrowed 
     *   if needed by methods like Math2.roundToUInt(d).
     */
    public void setFloat(int index, float d) {
        if (!maxIsMV && (!Float.isFinite(d) || d < MIN_VALUE || d > MAX_VALUE)) 
            maxIsMV = true;
        set(index, Math2.roundToUInt(d));
    }

    /**
     * Return a value from the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a double. 
     *   String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     *   If maxIsMV, MAX_VALUE is returned as Double.NaN.
     */
    public double getDouble(int index) {
        long i = get(index);
        return maxIsMV && i == MAX_VALUE? Double.NaN : i;
    }

    /**
     * Return a value from the array as a double.
     * FloatArray converts float to double in a simplistic way.
     * For this variant: Integer source values will be treated as unsigned
     * (e.g., a ByteArray with -1 returns 255).
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     */
    public double getUnsignedDouble(int index) {
        return getDouble(index); //already unsigned
    }

    /**
     * Return a value from the array as a double.
     * This "raw" variant leaves missingValue from integer data types 
     * (e.g., ByteArray missingValue=127) AS IS (even if maxIsMV=true).
     *
     * <p>All integerTypes overwrite this.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     */
    public double getRawDouble(int index) {
        return get(index);
    }

    /**
     * Set a value in the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. For numeric PrimitiveArray, it is narrowed 
     *   if needed by methods like Math2.roundToUInt(d).
     */
    public void setDouble(int index, double d) {
        if (!maxIsMV && (!Double.isFinite(d) || d < MIN_VALUE || d > MAX_VALUE)) 
            maxIsMV = true;
        set(index, Math2.roundToUInt(d));
    }

    /**
     * Return a value from the array as a String (where the cohort missing value
     * appears as "", not a value).
     * 
     * @param index the index number 0 .. 
     * @return For numeric types, this returns (String.valueOf(ar[index])), 
     *   or "" for NaN or infinity.
     *   If this PA is unsigned, this method returns the unsigned value.
     */
    public String getString(int index) {
        int b = getPacked(index);
        return maxIsMV && b == PACKED_MAX_VALUE? "" : String.valueOf(unpack(b));
    }

    /**
     * Return a value from the array as a String suitable for a JSON file. 
     * char returns a String with 1 character.
     * String returns a json String with chars above 127 encoded as \\udddd.
     * 
     * @param index the index number 0 ... size-1 
     * @return For numeric types, this returns ("" + ar[index]), or "null" for NaN or infinity.
     *   If this PA is unsigned, this method returns the unsigned value (never "null").
     */
    public String getJsonString(int index) {
        int b = getPacked(index);
        return maxIsMV && b == PACKED_MAX_VALUE? "null" : String.valueOf(unpack(b));
    }

    /**
     * Return a value from the array as a String.
     * This "raw" variant leaves missingValue from integer data types 
     * (e.g., ByteArray missingValue=127) AS IS, regardless of maxIsMV.
     * FloatArray and DoubleArray return "" if the stored value is NaN. 
     *
     * <p>All integerTypes overwrite this.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a String. 
     */
    public String getRawString(int index) {
        return String.valueOf(get(index));
    }

    /**
     * Set a value in the array as a String.
     * 
     * @param index the index number 0 .. 
     * @param s the value. For numeric PrimitiveArray's, it is parsed
     *   with String2.parseLong.
     */
    public void setString(int index, String s) {
        set(index, String2.parseLong(s)); //handles mv
    }


    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(long lookFor) {
        return indexOf(lookFor, 0);
    }

    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(long lookFor, int startIndex) {
        int packedLookFor = pack(lookFor);
        for (int i = startIndex; i < size; i++) 
            if (array[i] == packedLookFor) 
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
        return indexOf(String2.parseLong(lookFor), startIndex);
    }

    /**
     * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1. The search progresses towards 0.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int lastIndexOf(long lookFor, int startIndex) {
        if (startIndex >= size)
            throw new IllegalArgumentException(String2.ERROR + " in UIntArray.get: startIndex (" + 
                startIndex + ") >= size (" + size + ").");
        int packedLookFor = pack(lookFor);
        for (int i = startIndex; i >= 0; i--) 
            if (array[i] == packedLookFor) 
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
        return lastIndexOf(String2.parseLong(lookFor), startIndex);
    }

    /**
     * If size != capacity, this makes a new 'array' of size 'size'
     * so capacity will equal size.
     */
    public void trimToSize() {
        array = toArray();
    }

    /**
     * Test if o is an UIntArray with the same size and values.
     *
     * @param o the object that will be compared to this UIntArray
     * @return true if equal.  o=null returns false.
     */
    public boolean equals(Object o) {
        return testEquals(o).length() == 0;
    }

    /**
     * Test if o is an UIntArray with the same size and values,
     * but returns a String describing the difference (or "" if equal).
     *
     * @param o
     * @return a String describing the difference (or "" if equal).
     *   o=null doesn't throw an exception.
     */
    public String testEquals(Object o) {
        if (!(o instanceof UIntArray))
            return "The two objects aren't equal: this object is a UIntArray; the other is a " + 
                (o == null? "null" : o.getClass().getName()) + ".";
        UIntArray other = (UIntArray)o;
        if (other.size() != size)
            return "The two UIntArrays aren't equal: one has " + size + 
               " value(s); the other has " + other.size() + " value(s).";
        for (int i = 0; i < size; i++)
            if (array[i] != other.array[i] || (array[i] == PACKED_MAX_VALUE && maxIsMV != other.maxIsMV)) //handles mv
                return "The two UIntArrays aren't equal: this[" + i + "]=" + getLong(i) + 
                                                     "; other[" + i + "]=" + other.getLong(i) + ".";
        //if (maxIsMV != other.maxIsMV)
        //     return "The two ByteArrays aren't equal: this.maxIsMV=" + maxIsMV + 
        //                                          "; other.maxIsMV=" + other.maxIsMV + ".";
        return "";
    }

    /** 
     * This converts the elements into a Comma-Space-Separated-Value (CSSV) String.
     * Integer types show MAX_VALUE numbers (not "").
     *
     * @return a Comma-Space-Separated-Value (CSSV) String representation 
     */
    public String toString() {
        //estimate 9 bytes/element
        StringBuilder sb = new StringBuilder(9 * Math.min(size, (Integer.MAX_VALUE-8192) / 9));
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(unpack(array[i]));
        }
        return sb.toString();
    }

    /** 
     * This converts the elements into an NCCSV attribute String, e.g.,: -128b, 127b
     * Integer types show MAX_VALUE numbers (not "").
     *
     * @return an NCCSV attribute String
     */
    public String toNccsvAttString() {
        StringBuilder sb = new StringBuilder(size * 10);
        for (int i = 0; i < size; i++) 
            sb.append((i == 0? "" : ",") + unpack(array[i]) + "ui"); 
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
     * This compares the values in this.row1 and otherPA.row2
     * and returns a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.
     * The cohort missing value sorts highest.
     * Currently, this does not range check index1 and index2,
     * so the caller should be careful.
     *
     * @param index1 an index number 0 ... size-1
     * @param otherPA the other PrimitiveArray which must be the same (or close) PAType.
     * @param index2 an index number 0 ... size-1
     * @return returns a negative integer, zero, or a positive integer if the 
     *   value at index1 is less than, equal to, or greater than 
     *   the value at index2.  
     *   Think "array[index1] - array[index2]".
     */
    public int compare(int index1, PrimitiveArray otherPA, int index2) {
        return Long.compare(getLong(index1), otherPA.getLong(index2));  //long handles mv
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
        int newArray[] = new int[array.length]; 
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
            array[i] = Integer.reverseBytes(array[i]);
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
            dos.writeInt(array[i]);
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
        dos.writeInt(array[i]);
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
            array[size++] = dis.readInt();
    }

    /**
     * This reads/adds n 24-bit elements from a DataInputStream.
     *
     * @param dis the DataInputStream
     * @param n the number of elements to be read/added
     * @throws Exception if trouble
     */
    public void read24BitDis(DataInputStream dis, int n, boolean bigEndian) throws Exception {
        ensureCapacity(size + (long)n);
        byte bar[] = new byte[3];
        for (int i = 0; i < n; i++) {
            dis.readFully(bar);
            array[size++] = bigEndian?
                ((((bar[0] & 0xFF) << 24) | ((bar[1] & 0xFF) << 16) | ((bar[2] & 0xFF) << 8)) >> 8) :  
                ((((bar[2] & 0xFF) << 24) | ((bar[1] & 0xFF) << 16) | ((bar[0] & 0xFF) << 8)) >> 8);
        }
    }

    /**
     * This is like read24BitDis, but doesn't do &gt;&gt;8.
     *
     * @param dis the DataInputStream
     * @param n the number of elements to be read/added
     * @throws Exception if trouble
     */
    public void read24BitDisAudio(DataInputStream dis, int n, boolean bigEndian) throws Exception {
        ensureCapacity(size + (long)n);
        byte bar[] = new byte[3];
        for (int i = 0; i < n; i++) {
            dis.readFully(bar);
            array[size++] = bigEndian?
                (((bar[0] & 0xFF) << 24) | ((bar[1] & 0xFF) << 16) | ((bar[2] & 0xFF) << 8)) :  
                (((bar[2] & 0xFF) << 24) | ((bar[1] & 0xFF) << 16) | ((bar[0] & 0xFF) << 8));
        }
    }

    /**
     * This reads/appends int values to this PrimitiveArray from a DODS DataInputStream,
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
            array[size++] = dis.readInt();
    }

    /** 
     * This writes array[index] to a randomAccessFile at the current position.
     *
     * @param raf the RandomAccessFile
     * @param index
     * @throws Exception if trouble
     */
    public void writeToRAF(RandomAccessFile raf, int index) throws Exception {
        raf.writeInt(array[index]);
    }

    /** 
     * This reads one value from a randomAccessFile at the current position
     * and adds it to the PrimitiveArraay.
     *
     * @param raf the RandomAccessFile
     * @throws Exception if trouble
     */
    public void readFromRAF(RandomAccessFile raf) throws Exception {
        addPacked(raf.readInt());
    }

    /**
     * This fully skips 'n' elements in the dataInputStream.
     *
     * @param dis
     * @param n the number of elements to skip
     * @throws Exception if trouble
     */
    public static void disSkip(DataInputStream dis, int n) throws Exception {
        int nBytes = 4 * n;
        while (nBytes > 0) nBytes -= dis.skipBytes(nBytes);
    }
    
    /**
     * This reads an elements from the dataInputStream.
     *
     * @param dis
     * @return param n the number of elements to skip
     * @throws Exception if trouble
     */
    public static double readDisAsDouble(DataInputStream dis) throws Exception {
        int i = dis.readInt();
        return i == MAX_VALUE? Double.NaN : i;
    }
    
    /**
     * This appends the data in another pa to the current data.
     * WARNING: information may be lost from the incoming pa if this
     * primitiveArray is of a smaller type; see needPAType().
     *
     * @param pa pa must be the same or a narrower 
     *  data type, or the data will be narrowed with pa.getUInt.
     */
    public void append(PrimitiveArray pa) {
        int otherSize = pa.size(); 
        ensureCapacity(size + (long)otherSize);
        if (pa instanceof UIntArray) {
            if (pa.getMaxIsMV())
                setMaxIsMV(true);
            System.arraycopy(((UIntArray)pa).array, 0, array, size, otherSize);
            size += otherSize;
        } else {
            for (int i = 0; i < otherSize; i++)
                add(pa.getLong(i)); //this converts mv's and handles maxIsMV
        }
    }    

    /**
     * This appends the data in another pa to the current data.
     * This "raw" variant leaves missingValue from smaller data types 
     * (e.g., ByteArray missingValue=127) AS IS.
     * WARNING: information may be lost from the incoming pa if this
     * primitiveArray is of a simpler type.
     *
     * @param pa pa must be the same or a narrower 
     *  data type, or the data will be narrowed with pa.getUInt.
     */
    public void rawAppend(PrimitiveArray pa) {
        int otherSize = pa.size(); 
        ensureCapacity(size + (long)otherSize);
        if (pa instanceof UIntArray) {
            System.arraycopy(((UIntArray)pa).array, 0, array, size, otherSize);
        } else {
            for (int i = 0; i < otherSize; i++)
                array[size + i] = pack(Math2.roundToUInt(pa.getRawDouble(i)));  //this DOESN'T convert mv's
        }
        size += otherSize; //do last to minimize concurrency problems
    }    

    /**
     * This populates 'indices' with the indices (ranks) of the values in this UIntArray
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
            return new UIntArray();
        }

        //make a hashMap with all the unique values (associated values are initially all dummy)
        Integer dummy = new Integer(-1);
        HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
        long lastValue = unpack(array[0]); //since lastValue often equals currentValue, cache it
        hashMap.put(new Long(lastValue), dummy);
        boolean alreadySorted = true;
        for (int i = 1; i < size; i++) {
            long currentValue = unpack(array[i]);
            if (currentValue != lastValue) {
                if (currentValue < lastValue) 
                    alreadySorted = false;
                lastValue = currentValue;
                hashMap.put(new Long(lastValue), dummy);
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
            throw new RuntimeException("UIntArray.makeRankArray nUnique(" + nUnique +
                ") != count(" + count + ")!");

        //sort them
        Arrays.sort(unique);

        //put the unique values back in the hashMap with the ranks as the associated values
        //and make tUnique 
        long tUnique[] = new long[nUnique];
        for (int i = 0; i < count; i++) {
            hashMap.put(unique[i], new Integer(i));
            tUnique[i] = ((Long)unique[i]).longValue();
        }

        //convert original values to ranks
        int ranks[] = new int[size];
        lastValue = unpack(array[0]);
        ranks[0] = ((Integer)hashMap.get(new Long(lastValue))).intValue();
        int lastRank = ranks[0];
        for (int i = 1; i < size; i++) {
            if (array[i] == lastValue) {
                ranks[i] = lastRank;
            } else {
                lastValue = unpack(array[i]);
                ranks[i] = ((Integer)hashMap.get(new Long(lastValue))).intValue();
                lastRank = ranks[i];
            }
        }

        //store the results in ranked
        indices.append(new UIntArray(ranks));

        return new UIntArray(tUnique);

    }

    /**
     * This changes all instances of the first value to the second value.
     *
     * @param tFrom the original value (use "" or "NaN"  for standard missingValue)
     * @param tTo   the new value (use "" or "NaN"  for standard missingValue)
     * @return the number of values switched
     */
    public int switchFromTo(String tFrom, String tTo) {
        int packedFrom = pack(Math2.roundToUInt(String2.parseDouble(tFrom)));
        double d = String2.parseDouble(tTo);
        int packedTo   = pack(Math2.roundToUInt(d));
        if (packedFrom == packedTo)
            return 0;
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (array[i] == packedFrom) {
                array[i] =  packedTo;
                count++;
            }
        }
        if (count > 0 && Double.isNaN(d))
            maxIsMV = true;
        return count;
    }



    /**
     * This tests for adjacent tied values and returns the index of the first tied value.
     * Adjacent NaNs are treated as ties.
     *
     * @return the index of the first tied value (or -1 if none).
     */
    public int firstTie() {
        for (int i = 1; i < size; i++) {
            if (array[i - 1] == array[i]) {
                return i - 1;
            }
        }
        return -1;
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
        long tmin = MAX_VALUE;
        long tmax = MIN_VALUE;
        for (int i = 0; i < size; i++) {
            if (maxIsMV && array[i] == PACKED_MAX_VALUE) {
            } else {
                long v = unpack(array[i]);
                n++;
                if (v <= tmin) {tmini = i; tmin = v; }
                if (v >= tmax) {tmaxi = i; tmax = v; }
            }
        }
        return new int[]{n, tmini, tmaxi};
    }

    /**
     * For integer types, this fixes unsigned bytes that were incorrectly read as signed
     * so that they have the correct ordering of values (0 to 255 becomes -128 to 127).
     * <br>What were read as signed:    0  127 -128  -1
     * <br>should become   unsigned: -128   -1    0 255
     * <br>This also does the reverse.
     * <br>For non-integer types, this does nothing.
     */
    public void changeSignedToFromUnsigned() {
        for (int i = 0; i < size; i++) {
            int i2 = array[i];
            array[i] = (int)(i2 < 0? i2 + MAX_VALUE + 1 : 
                                     i2 - MAX_VALUE - 1); //order of ops is important 
        }
    }

    /**
     * This tests the methods of this class.
     *
     * @throws Throwable if trouble.
     */
    public static void basicTest() throws Throwable{
        String2.log("*** UIntArray.basicTest");

        Test.ensureEqual(pack(                  0L  ),                 0, "");
        Test.ensureEqual(pack(Integer.MAX_VALUE     ), Integer.MAX_VALUE, "");
        Test.ensureEqual(pack(Integer.MAX_VALUE+1L  ), Integer.MIN_VALUE, "");
        Test.ensureEqual(pack(Integer.MAX_VALUE*2L  ),                -2, "");
        Test.ensureEqual(pack(Integer.MAX_VALUE*2L+1),                -1, "");

        Test.ensureEqual(unpack(                  0),                     0L, "");
        Test.ensureEqual(unpack(Integer.MAX_VALUE  ), Integer.MAX_VALUE     , "");
        Test.ensureEqual(unpack(Integer.MIN_VALUE  ), Integer.MAX_VALUE+1L  , "");
        Test.ensureEqual(unpack(                 -2), Integer.MAX_VALUE*2L  , "");
        Test.ensureEqual(unpack(                 -1), Integer.MAX_VALUE*2L+1, "");

        UIntArray anArray = new UIntArray(new int[]{-2147483648, -1,         0, 1, 2147483647}); //packed values
        Test.ensureEqual(anArray.toString(),        "2147483648, 4294967295, 0, 1, 2147483647", "");
        anArray = UIntArray.fromCSV(                   "0, 1, 2147483647, 2147483648, 4294967295, 4294967295, 9999999999");
        Test.ensureEqual(anArray.toString(),           "0, 1, 2147483647, 2147483648, 4294967295, 4294967295, 4294967295", "");
        Test.ensureEqual(anArray.toNccsvAttString(), "0ui,1ui,2147483647ui,2147483648ui,4294967295ui,4294967295ui,4294967295ui", "");

        //** test default constructor and many of the methods
        anArray = new UIntArray();
        Test.ensureEqual(anArray.isIntegerType(), true, "");
        Test.ensureEqual(anArray.missingValue().getRawDouble(), MAX_VALUE, "");
        anArray.addString("");
        Test.ensureEqual(anArray.get(0),               MAX_VALUE, "");
        Test.ensureEqual(anArray.getRawInt(0),         Integer.MAX_VALUE, "");  //getRawInt->int is too small to hold correct answer
        Test.ensureEqual(anArray.getRawDouble(0),      MAX_VALUE, "");
        Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN, "");
        Test.ensureEqual(anArray.getRawString(0), "" + MAX_VALUE, "");
        Test.ensureEqual(anArray.getRawNiceDouble(0),  MAX_VALUE, "");
        Test.ensureEqual(anArray.getInt(0),            Integer.MAX_VALUE, "");  //getInt->int is too small to hold correct answer
        Test.ensureEqual(anArray.getDouble(0),         Double.NaN, "");
        Test.ensureEqual(anArray.getString(0), "", "");

        anArray.set(0, 0);                    Test.ensureEqual(anArray.getUnsignedDouble(0),                    0, "");
        anArray.set(0, Integer.MAX_VALUE);    Test.ensureEqual(anArray.getUnsignedDouble(0), Integer.MAX_VALUE   , "");
        anArray.set(0, Integer.MAX_VALUE+1L); Test.ensureEqual(anArray.getUnsignedDouble(0), Integer.MAX_VALUE+1L, "");
        anArray.set(0, unpack(-2));           Test.ensureEqual(anArray.getUnsignedDouble(0), 4294967295.0,         "");
        anArray.set(0, unpack(-1));           Test.ensureEqual(anArray.getUnsignedDouble(0), Double.NaN,           "");

        anArray.clear();


        Test.ensureEqual(anArray.size(), 0, "");
        anArray.add(2000000000);
        Test.ensureEqual(anArray.size(), 1, "");
        Test.ensureEqual(anArray.get(0), 2000000000, "");
        Test.ensureEqual(anArray.getInt(0), 2000000000, "");
        Test.ensureEqual(anArray.getFloat(0), 2000000000, "");
        Test.ensureEqual(anArray.getDouble(0), 2000000000, "");
        Test.ensureEqual(anArray.getString(0), "2000000000", "");
        Test.ensureEqual(anArray.elementType(), PAType.UINT, "");
        int tArray[] = anArray.toArray();
        Test.ensureEqual(tArray, new int[]{2000000000}, "");

        //intentional errors
        try {anArray.get(1);              throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.get: index (1) >= size (1).", "");
        }
        try {anArray.set(1, 100);         throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getInt(1);           throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setInt(1, 100);      throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getLong(1);          throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setLong(1, 100);     throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getFloat(1);         throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setFloat(1, 100);    throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getDouble(1);        throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setDouble(1, 100);   throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getString(1);        throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setString(1, "100"); throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.set: index (1) >= size (1).", "");
        }

        //set NaN returned as NaN
        anArray.setDouble(0, Double.NaN);    Test.ensureEqual(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, -1e300);        Test.ensureEqual(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, 2.2);           Test.ensureEqual(anArray.getDouble(0), 2,          ""); 
        anArray.setFloat( 0, Float.NaN);     Test.ensureEqual(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, -1e33f);        Test.ensureEqual(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, 3.3f);          Test.ensureEqual(anArray.getFloat(0),  3,          ""); 
        anArray.setLong(0, Long.MAX_VALUE);  Test.ensureEqual(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 9123456789L);     Test.ensureEqual(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 4);               Test.ensureEqual(anArray.getLong(0),   4, ""); 
        anArray.setInt(0, Integer.MAX_VALUE);Test.ensureEqual(anArray.getInt(0),    Integer.MAX_VALUE, ""); 
        anArray.setInt(0, 1123456789);       Test.ensureEqual(anArray.getInt(0),    1123456789, ""); 
        anArray.setInt(0, 5);                Test.ensureEqual(anArray.getInt(0),    5, ""); 


        //** test capacity constructor, test expansion, test clear
        anArray = new UIntArray(2, false);
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
        anArray = new UIntArray(3, true);
        Test.ensureEqual(anArray.size(), 3, "");
        Test.ensureEqual(anArray.get(2), 0, "");


        //** test array constructor
        anArray = new UIntArray(new int[]{0,2,4,6,8});
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
        Test.ensureEqual(anArray.toString(), "0, 2, 4, 6, 8", "");

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

        //intentional errors
        try {anArray.atInsert(-1, 99);   throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.atInsert: index (-1) is < 0 or > size (4).", "");
        }
        //insert at 4 should work
        anArray.atInsert(4, 99);
        anArray.remove(4);
        Test.ensureEqual(anArray.size(), 4, "");
        //but insert at 5 should fail
        try {anArray.atInsert(5, 99);   throw new Throwable("It should have failed.");
        } catch (Exception e) {
            Test.ensureEqual(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in UIntArray.atInsert: index (5) is < 0 or > size (4).", "");
        }

        //test removeRange
        anArray.removeRange(4, 4); //make sure it is allowed
        anArray.removeRange(1, 3);
        Test.ensureEqual(anArray.size(), 2, "");
        Test.ensureEqual(anArray.get(0), 0, "");
        Test.ensureEqual(anArray.get(1), 8, "");

        //test (before trimToSize) that toString, toDoubleArray, and toStringArray use 'size'
        Test.ensureEqual(anArray.toString(), "0, 8", "");
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");
        Test.ensureEqual(anArray.toStringArray(), new String[]{"0", "8"}, "");

        //test trimToSize
        anArray.trimToSize();
        Test.ensureEqual(anArray.array.length, 2, "");

        //test equals
        UIntArray anArray2 = new UIntArray();
        anArray2.add(0); 
        Test.ensureEqual(anArray.testEquals(null), 
            "The two objects aren't equal: this object is a UIntArray; the other is a null.", "");
        Test.ensureEqual(anArray.testEquals("A String"), 
            "The two objects aren't equal: this object is a UIntArray; the other is a java.lang.String.", "");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two UIntArrays aren't equal: one has 2 value(s); the other has 1 value(s).", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.addString("7");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two UIntArrays aren't equal: this[1]=8; other[1]=7.", "");
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
        anArray = new UIntArray(new int[]{1});
        anArray.append(new ByteArray(new byte[]{5, -5}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, Double.NaN}, "");
        anArray.append(new StringArray(new String[]{"a", "9"}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, Double.NaN, Double.NaN, 9}, "");
        anArray2 = (UIntArray)anArray.clone();
        Test.ensureEqual(anArray2.toDoubleArray(), new double[]{1, 5, Double.NaN, Double.NaN, 9}, "");

        //test constructor(first, last)
        anArray = new UIntArray(10, 15);
        Test.ensureEqual(anArray.toString(), "10, 11, 12, 13, 14, 15", "");

        //test move
        anArray = new UIntArray(new int[]{0,1,2,3,4});
        anArray.move(1,3,0);
        Test.ensureEqual(anArray.toArray(), new int[]{1,2,0,3,4}, "");

        anArray = new UIntArray(new int[]{0,1,2,3,4});
        anArray.move(1,2,4);
        Test.ensureEqual(anArray.toArray(), new int[]{0,2,3,1,4}, "");

        //move does nothing, but is allowed
        anArray = new UIntArray(new int[]{0,1,2,3,4});
        anArray.move(1,1,0);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");
        anArray.move(1,2,1);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");
        anArray.move(1,2,2);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");
        anArray.move(5,5,0);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");
        anArray.move(3,5,5);
        Test.ensureEqual(anArray.toArray(), new int[]{0,1,2,3,4}, "");

        //makeIndices
        anArray = new UIntArray(new int[] {25,1,1,10});
        IntArray indices = new IntArray();
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 10, 25", "");
        Test.ensureEqual(indices.toString(), "2, 0, 0, 1", "");

        anArray = new UIntArray(new long[] {35,35,MAX_VALUE,1,2});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "1, 2, 35, 4294967295", "");
        Test.ensureEqual(indices.toString(), "2, 2, 3, 0, 1", "");

        anArray = new UIntArray(new long[] {10,20,30,40});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "10, 20, 30, 40", "");
        Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

        //switchToFakeMissingValue
        anArray = new UIntArray(new long[] {MAX_VALUE,1,2,MAX_VALUE,3,MAX_VALUE});
        Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
        Test.ensureEqual(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
        anArray.switchFromTo("75", "");
        Test.ensureEqual(anArray.toString(), "4294967295, 1, 2, 4294967295, 3, 4294967295", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 1, 4}, "");

        //addN
        anArray = new UIntArray(new int[] {25});
        anArray.addN(2, 5);
        Test.ensureEqual(anArray.toString(), "25, 5, 5", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 2, 0}, "");

        //add array
        anArray.add(new int[]{17, 19});
        Test.ensureEqual(anArray.toString(), "25, 5, 5, 17, 19", "");

        //subset
        PrimitiveArray ss = anArray.subset(1, 3, 4);
        Test.ensureEqual(ss.toString(), "5, 19", "");
        ss = anArray.subset(0, 1, 0);
        Test.ensureEqual(ss.toString(), "25", "");
        ss = anArray.subset(0, 1, -1);
        Test.ensureEqual(ss.toString(), "", "");
        ss = anArray.subset(1, 1, 0);
        Test.ensureEqual(ss.toString(), "", "");

        ss.trimToSize();
        anArray.subset(ss, 1, 3, 4);
        Test.ensureEqual(ss.toString(), "5, 19", "");
        anArray.subset(ss, 0, 1, 0);
        Test.ensureEqual(ss.toString(), "25", "");
        anArray.subset(ss, 0, 1, -1);
        Test.ensureEqual(ss.toString(), "", "");
        anArray.subset(ss, 1, 1, 0);
        Test.ensureEqual(ss.toString(), "", "");

        //evenlySpaced
        anArray = new UIntArray(new int[] {10,20,30});
        Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
        anArray.set(2, 31);
        Test.ensureEqual(anArray.isEvenlySpaced(), 
            "UIntArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.5.", "");
        Test.ensureEqual(anArray.smallestBiggestSpacing(),
            "    smallest spacing=10.0: [0]=10.0, [1]=20.0\n" +
            "    biggest  spacing=11.0: [1]=20.0, [2]=31.0", "");

        //isAscending
        anArray = new UIntArray(new int[] {10,10,30});
        Test.ensureEqual(anArray.isAscending(), "", "");
        anArray.set(2, MAX_VALUE);
        Test.ensureEqual(anArray.isAscending(), "", "");
        anArray.setMaxIsMV(true);
        Test.ensureEqual(anArray.isAscending(), 
            "UIntArray isn't sorted in ascending order: [2]=(missing value).", "");
        anArray.set(1, 9);
        Test.ensureEqual(anArray.isAscending(), 
            "UIntArray isn't sorted in ascending order: [0]=10 > [1]=9.", "");

        //isDescending
        anArray = new UIntArray(new int[] {30,10,10});
        Test.ensureEqual(anArray.isDescending(), "", "");
        anArray.set(2, MAX_VALUE);
        anArray.setMaxIsMV(true);
        Test.ensureEqual(anArray.isDescending(), 
            "UIntArray isn't sorted in descending order: [1]=10 < [2]=4294967295.", "");
        anArray.set(1, 35);
        Test.ensureEqual(anArray.isDescending(), 
            "UIntArray isn't sorted in descending order: [0]=30 < [1]=35.", "");

        //firstTie
        anArray = new UIntArray(new int[] {30,35,10});
        Test.ensureEqual(anArray.firstTie(), -1, "");
        anArray.set(1, 30);
        Test.ensureEqual(anArray.firstTie(), 0, "");

        //hashcode
        anArray = new UIntArray();
        for (int i = 5; i < 1000; i++)
            anArray.add(i);
        String2.log("hashcode1=" + anArray.hashCode());
        anArray2 = (UIntArray)anArray.clone();
        Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
        anArray.atInsert(0, 2);
        Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

        //justKeep
        BitSet bitset = new BitSet();
        anArray = new UIntArray(new int[] {0, 11, 22, 33, 44});
        bitset.set(1);
        bitset.set(4);
        anArray.justKeep(bitset);
        Test.ensureEqual(anArray.toString(), "11, 44", "");

        //min max
        anArray = new UIntArray();
        anArray.addPAOne(anArray.MINEST_VALUE());
        anArray.addPAOne(anArray.MAXEST_VALUE());
        String2.log("MAXEST_VALUE=" + anArray.MAXEST_VALUE());
        Test.ensureEqual(anArray.getPacked(1), -2, "");
        Test.ensureEqual(anArray.getString(0), anArray.MINEST_VALUE().toString(), "");
        Test.ensureEqual(anArray.getString(0), "0", "");
        Test.ensureEqual(anArray.getString(1), anArray.MAXEST_VALUE().toString(), "");
        Test.ensureEqual(anArray.getString(1), "" + (MAX_VALUE-1), "");

        //tryToFindNumericMissingValue() 
        Test.ensureEqual((new UIntArray(new int[] {                 })).tryToFindNumericMissingValue(), null, "");
        Test.ensureEqual((new UIntArray(new int[] {1, 2             })).tryToFindNumericMissingValue(), null, "");
        Test.ensureEqual((new UIntArray(new int[] {0                })).tryToFindNumericMissingValue(), null, "");
        Test.ensureEqual((new UIntArray(new int[] {Integer.MAX_VALUE})).tryToFindNumericMissingValue(), null, "");
        Test.ensureEqual((new UIntArray(new long[]{MAX_VALUE        })).tryToFindNumericMissingValue(),  MAX_VALUE, "");
        Test.ensureEqual((new UIntArray(new int[] {1, 99            })).tryToFindNumericMissingValue(),         99, "");

        /* */
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
            lastTest = interactive? -1 : 0;
        String msg = "\n^^^ UIntArray.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) basicTest();
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

