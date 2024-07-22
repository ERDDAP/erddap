/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
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
import ucar.ma2.StructureData;

/**
 * IntArray is a thin shell over an int[] with methods like ArrayList's methods; it extends
 * PrimitiveArray.
 *
 * <p>This class uses maxIsMV=true and Integer.MAX_VALUE to represent a missing value (NaN).
 */
public class IntArray extends PrimitiveArray {

  /**
   * This is the main data structure. This should be private, but is public so you can manipulate it
   * if you promise to be careful. Note that if the PrimitiveArray's capacity is increased, the
   * PrimitiveArray will use a different array for storage.
   */
  public int[] array;

  /**
   * This indicates if this class' type (e.g., PAType.SHORT) is an integer (in the math sense) type.
   * The integer type classes overwrite this.
   */
  @Override
  public final boolean isIntegerType() {
    return true;
  }

  /**
   * This returns the number of bytes per element for this PrimitiveArray. The value for "String"
   * isn't a constant, so this returns 20.
   *
   * @return the number of bytes per element for this PrimitiveArray. The value for "String" isn't a
   *     constant, so this returns 20.
   */
  @Override
  public final int elementSize() {
    return 4;
  }

  /**
   * This returns for cohort missing value for this class (e.g., Integer.MAX_VALUE), expressed as a
   * double. FloatArray and StringArray return Double.NaN.
   */
  @Override
  public final double missingValueAsDouble() {
    return Integer.MAX_VALUE;
  }

  /**
   * This tests if the value at the specified index equals the data type's MAX_VALUE (for
   * integerTypes, which may or may not indicate a missing value, depending on maxIsMV), NaN (for
   * Float and Double), \\uffff (for CharArray), or "" (for StringArray).
   *
   * @param index The index in question
   * @return true if the value is a missing value.
   */
  @Override
  public final boolean isMaxValue(final int index) {
    return get(index) == Integer.MAX_VALUE;
  }

  /**
   * This tests if the value at the specified index is a missing value. For integerTypes,
   * isMissingValue can only be true if maxIsMv is 'true'.
   *
   * @param index The index in question
   * @return true if the value is a missing value.
   */
  @Override
  public final boolean isMissingValue(final int index) {
    return maxIsMV && isMaxValue(index);
  }

  /** A constructor for a capacity of 8 elements. The initial 'size' will be 0. */
  public IntArray() {
    array = new int[8];
  }

  /**
   * This constructs a IntArray by copying elements from the incoming PrimitiveArray (using
   * append()).
   *
   * @param primitiveArray a primitiveArray of any type
   */
  public IntArray(final PrimitiveArray primitiveArray) {
    Math2.ensureMemoryAvailable(4L * primitiveArray.size(), "IntArray");
    array = new int[primitiveArray.size()]; // exact size
    append(primitiveArray);
  }

  /**
   * A constructor for a specified number of elements. The initial 'size' will be 0.
   *
   * @param capacity creates an IntArray with the specified initial capacity.
   * @param active if true, size will be set to capacity and all elements will equal 0; else size =
   *     0.
   */
  public IntArray(final int capacity, final boolean active) {
    Math2.ensureMemoryAvailable(4L * capacity, "IntArray");
    array = new int[capacity];
    if (active) size = capacity;
  }

  /**
   * A special constructor for IntArray: first, last, with an increment of 1.
   *
   * @param first the value of the first element.
   * @param last the value of the last element (inclusive!).
   */
  public IntArray(final int first, final int last) {
    size = last - first + 1;
    Math2.ensureMemoryAvailable(4L * size, "IntArray");
    array = new int[size];
    for (int i = 0; i < size; i++) array[i] = first + i;
  }

  /**
   * A constructor which (at least initially) uses the array and all its elements ('size' will equal
   * anArray.length).
   *
   * @param anArray the array to be used as this object's array.
   */
  public IntArray(final int[] anArray) {
    array = anArray;
    size = anArray.length;
  }

  /**
   * This makes a IntArray from the comma-separated values. <br>
   * null becomes pa.length() == 0. <br>
   * "" becomes pa.length() == 0. <br>
   * " " becomes pa.length() == 1. <br>
   * See also PrimitiveArray.csvFactory(paType, csv);
   *
   * @param csv the comma-separated-value string
   * @return a IntArray from the comma-separated values.
   */
  public static final IntArray fromCSV(final String csv) {
    return (IntArray) PrimitiveArray.csvFactory(PAType.INT, csv);
  }

  /**
   * This returns a new PAOne with the minimum value that can be held by this class.
   *
   * @return a new PAOne with the minimum value that can be held by this class, e.g., -128b for
   *     ByteArray.
   */
  @Override
  public final PAOne MINEST_VALUE() {
    return PAOne.fromInt(Integer.MIN_VALUE);
  }

  /**
   * This returns a new PAOne with the maximum value that can be held by this class (not including
   * the cohort missing value).
   *
   * @return a new PAOne with the maximum value that can be held by this class, e.g., 126 for
   *     ByteArray.
   */
  @Override
  public final PAOne MAXEST_VALUE() {
    return PAOne.fromInt(Integer.MAX_VALUE - 1);
  }

  /**
   * This returns the current capacity (number of elements) of the internal data array.
   *
   * @return the current capacity (number of elements) of the internal data array.
   */
  @Override
  public final int capacity() {
    return array.length;
  }

  /**
   * This returns the hashcode for this IntArray (dependent only on values, not capacity). WARNING:
   * the algorithm used may change in future versions.
   *
   * @return the hashcode for this IntArray (dependent only on values, not capacity)
   */
  @Override
  public int hashCode() {
    // see
    // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html#hashCode()
    // and
    // https://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
    int code = 0;
    for (int i = 0; i < size; i++) code = 31 * code + array[i];
    return code;
    // return HashDigest.murmur32(array, size);
  }

  /**
   * This makes a new subset of this PrimitiveArray based on startIndex, stride, and stopIndex.
   *
   * @param pa the pa to be filled (may be null). If not null, must be of same type as this class.
   * @param startIndex must be a valid index
   * @param stride must be at least 1
   * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
   * @return The same pa (or a new PrimitiveArray if it was null) with the desired subset. If new,
   *     it will have a backing array with a capacity equal to its size. If stopIndex &lt;
   *     startIndex, this returns PrimitiveArray with size=0;
   */
  @Override
  public PrimitiveArray subset(
      final PrimitiveArray pa, final int startIndex, final int stride, int stopIndex) {
    if (pa != null) pa.clear();
    if (startIndex < 0)
      throw new IndexOutOfBoundsException(
          MessageFormat.format(ArraySubsetStart, getClass().getSimpleName(), "" + startIndex));
    if (stride < 1)
      throw new IllegalArgumentException(
          MessageFormat.format(ArraySubsetStride, getClass().getSimpleName(), "" + stride));
    if (stopIndex >= size) stopIndex = size - 1;
    if (stopIndex < startIndex)
      return pa == null
          ? new IntArray(new int[0])
          : pa; // no need to call .setMaxIsMV(maxIsMV) since size=0

    final int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
    IntArray ia = null;
    if (pa == null) {
      ia = new IntArray(willFind, true);
    } else {
      ia = (IntArray) pa;
      ia.ensureCapacity(willFind);
      ia.size = willFind;
    }
    final int tar[] = ia.array;
    if (stride == 1) {
      System.arraycopy(array, startIndex, tar, 0, willFind);
    } else {
      int po = 0;
      for (int i = startIndex; i <= stopIndex; i += stride) tar[po++] = array[i];
    }
    return ia.setMaxIsMV(maxIsMV);
  }

  /**
   * This returns the PAType (PAType.INT) of the element type.
   *
   * @return the PAType (PAType.INT) of the element type.
   */
  @Override
  public PAType elementType() {
    return PAType.INT;
  }

  /**
   * This returns the minimum PAType needed to completely and precisely contain the values in this
   * PA's PAType and tPAType (e.g., when merging two PrimitiveArrays).
   *
   * @return the minimum PAType needed to completely and precisely contain the values in this PA's
   *     PAType and tPAType (e.g., when merging two PrimitiveArrays).
   */
  @Override
  public PAType needPAType(final PAType tPAType) {
    return switch (tPAType) {
        // if tPAType is smaller or same, return this.PAType
      case BYTE, UBYTE, SHORT, USHORT, INT -> PAType.INT;

        // if sideways
      case CHAR -> PAType.STRING;
      case UINT -> PAType.LONG;
      case ULONG -> PAType.STRING;
      case FLOAT -> PAType.DOUBLE;

        // if tPAType is bigger.  LONG, DOUBLE, STRING
      default -> tPAType;
    };
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array
   */
  public final void add(final int value) {
    if (size == array.length) // if we're at capacity
    ensureCapacity(size + 1L);
    array[size++] = value;
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array. If value instanceof Number, this uses
   *     Number.intValue(). (If you want a more sophisticated conversion, save to DoubleArray, then
   *     convert DoubleArray to IntArray.) If null or not a Number, this adds Integer.MAX_VALUE.
   */
  @Override
  public final void addObject(final Object value) {
    // double is good intermediate because it has the idea of NaN
    addDouble(value != null && value instanceof Number nu ? nu.doubleValue() : Double.NaN);
  }

  /**
   * This reads one value from the StrutureData and adds it to this PA.
   *
   * @param sd from an .nc file
   * @param memberName
   */
  @Override
  public void add(final StructureData sd, final String memberName) {
    add(sd.getScalarInt(memberName));
  }

  /**
   * This adds all the values from ar.
   *
   * @param ar an array
   */
  public final void add(final int ar[]) {
    final int arSize = ar.length;
    ensureCapacity(size + (long) arSize);
    System.arraycopy(ar, 0, array, size, arSize);
    size += arSize;
  }

  /**
   * This adds n copies of value to the array (increasing 'size' by n).
   *
   * @param n if less than 0, this throws Exception
   * @param value the value to be added to the array. n &lt; 0 throws an Exception.
   */
  public final void addN(final int n, final int value) {
    if (n == 0) return;
    if (n < 0)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayAddN, getClass().getSimpleName(), "" + n));
    ensureCapacity(size + (long) n);
    Arrays.fill(array, size, size + n, value);
    size += n;
  }

  /**
   * This inserts an item into the array at the specified index, pushing subsequent items to
   * oldIndex+1 and increasing 'size' by 1.
   *
   * @param index the position where the value should be inserted.
   * @param value the value to be inserted into the array
   */
  public void atInsert(final int index, final int value) {
    // String2.log(">>IntArray index=" + index + " value=" + value + " size=" + size + " al=" +
    // array.length);
    if (index < 0 || index > size)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayAtInsert, getClass().getSimpleName(), "" + index, "" + size));
    if (size == array.length) // if we're at capacity
    ensureCapacity(size + 1L);
    System.arraycopy(array, index, array, index + 1, size - index);
    size++;
    array[index] = value;
  }

  /**
   * This inserts an item into the array at the specified index, pushing subsequent items to
   * oldIndex+1 and increasing 'size' by 1.
   *
   * @param index 0..
   * @param value the value, as a String.
   */
  @Override
  public void atInsertString(final int index, final String value) {
    final Integer io = String2.parseIntObject(value); // handles NaN and mv
    if (io == null) {
      maxIsMV = true;
      atInsert(index, Integer.MAX_VALUE);
    } else {
      atInsert(index, io.intValue());
    }
  }

  /**
   * This adds n PAOne's to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a PAOne (or null).
   */
  @Override
  public final void addNPAOnes(final int n, final PAOne value) {
    addNLongs(n, value == null ? Long.MAX_VALUE : value.getLong()); // handles NaN and MV
  }

  /**
   * This adds n Strings to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a String.
   */
  @Override
  public final void addNStrings(final int n, final String value) {
    final Integer io = String2.parseIntObject(value); // handles NaN and mv
    if (io == null) {
      maxIsMV = true;
      addNInts(n, Integer.MAX_VALUE);
    } else {
      addNInts(n, io.intValue());
    }
  }

  /**
   * This adds n floats to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a float.
   */
  @Override
  public final void addNFloats(final int n, final float value) {
    if (!maxIsMV
        && (!Float.isFinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE))
      maxIsMV = true;
    addN(n, Math2.roundToInt(value));
  }

  /**
   * This adds n doubles to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a double.
   */
  @Override
  public final void addNDoubles(final int n, final double value) {
    if (!maxIsMV
        && (!Double.isFinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE))
      maxIsMV = true;
    addN(n, Math2.roundToInt(value));
  }

  /**
   * This adds n ints to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public final void addNInts(final int n, final int value) {
    addN(n, value); // !!! assumes maxIsMV isn't affected
  }

  /**
   * This adds n longs to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public final void addNLongs(final int n, final long value) {
    if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
      maxIsMV = true;
      addN(n, Integer.MAX_VALUE);
    } else {
      addN(n, (int) value);
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
  @Override
  public PrimitiveArray addFromPA(final PrimitiveArray otherPA, int otherIndex, final int nValues) {

    // add from same type
    if (otherPA.elementType() == elementType()) {
      if (otherIndex + nValues > otherPA.size)
        throw new IllegalArgumentException(
            String2.ERROR
                + " in IntArray.addFromPA: otherIndex="
                + otherIndex
                + " + nValues="
                + nValues
                + " > otherPA.size="
                + otherPA.size);
      ensureCapacity(size + nValues);
      System.arraycopy(((IntArray) otherPA).array, otherIndex, array, size, nValues);
      size += nValues;
      if (otherPA.getMaxIsMV()) maxIsMV = true;
      return this;
    }

    // add from different type
    for (int i = 0; i < nValues; i++)
      addLong(otherPA.getLong(otherIndex++)); // does error checking and handles maxIsMV
    return this;
  }

  /**
   * This sets an element from another PrimitiveArray.
   *
   * @param index the index to be set
   * @param otherPA the other PrimitiveArray
   * @param otherIndex the index of the item in otherPA
   */
  @Override
  public void setFromPA(final int index, final PrimitiveArray otherPA, final int otherIndex) {
    setLong(index, otherPA.getLong(otherIndex)); // handles maxIsMV
  }

  /**
   * This removes the specified element.
   *
   * @param index the element to be removed, 0 ... size-1
   */
  @Override
  public void remove(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayRemove, getClass().getSimpleName(), "" + index, "" + size));
    System.arraycopy(array, index + 1, array, index, size - index - 1);
    size--;

    // for object types, nullify the object at the end
  }

  /**
   * This removes the specified range of elements.
   *
   * @param from the first element to be removed, 0 ... size
   * @param to one after the last element to be removed, from ... size
   */
  @Override
  public void removeRange(final int from, final int to) {
    if (to > size)
      throw new IllegalArgumentException(
          String2.ERROR + " in IntArray.removeRange: to (" + to + ") > size (" + size + ").");
    if (from >= to) {
      if (from == to) return;
      throw new IllegalArgumentException(
          String2.ERROR + " in IntArray.removeRange: from (" + from + ") > to (" + to + ").");
    }
    System.arraycopy(array, to, array, from, size - to);
    size -= to - from;

    // for object types, nullify the objects at the end
  }

  /**
   * Moves elements 'first' through 'last' (inclusive) to 'destination'.
   *
   * @param first the first to be move
   * @param last (exclusive)
   * @param destination the destination, can't be in the range 'first+1..last-1'.
   */
  @Override
  public void move(final int first, final int last, final int destination) {
    final String errorIn = String2.ERROR + " in IntArray.move:\n";

    if (first < 0) throw new RuntimeException(errorIn + "first (" + first + ") must be >= 0.");
    if (last < first || last > size)
      throw new RuntimeException(
          errorIn
              + "last ("
              + last
              + ") must be >= first ("
              + first
              + ") and <= size ("
              + size
              + ").");
    if (destination < 0 || destination > size)
      throw new RuntimeException(
          errorIn + "destination (" + destination + ") must be between 0 and size (" + size + ").");
    if (destination > first && destination < last)
      throw new RuntimeException(
          errorIn
              + "destination ("
              + destination
              + ") must be <= first ("
              + first
              + ") or >= last ("
              + last
              + ").");
    if (first == last || destination == first || destination == last) return; // nothing to do
    // String2.log("move first=" + first + " last=" + last + " dest=" + destination);
    // String2.log("move initial " + String2.toCSSVString(array));

    // store the range to be moved
    final int nToMove = last - first;
    final int[] temp = new int[nToMove];
    System.arraycopy(array, first, temp, 0, nToMove);

    // if moving to left...    (draw diagram to visualize this)
    if (destination < first) {
      System.arraycopy(array, destination, array, destination + nToMove, first - destination);
      // String2.log("move after shift " + String2.toCSSVString(array));

      // copy temp data into place
      System.arraycopy(temp, 0, array, destination, nToMove);
    } else {
      // moving to right
      System.arraycopy(array, last, array, first, destination - last);
      // String2.log("move after shift " + String2.toCSSVString(array));

      // copy temp data into place
      System.arraycopy(temp, 0, array, destination - nToMove, nToMove);
    }
    // String2.log("move done " + String2.toCSSVString(array));

  }

  /**
   * This just keeps the rows for the 'true' values in the bitset. Rows that aren't kept are
   * removed. The resulting PrimitiveArray is compacted (i.e., it has a smaller size()).
   *
   * @param bitset The BitSet indicating which rows (indices) should be kept.
   */
  @Override
  public void justKeep(final BitSet bitset) {
    int newSize = 0;
    for (int row = 0; row < size; row++) {
      if (bitset.get(row)) array[newSize++] = array[row];
    }
    removeRange(newSize, size);
  }

  /**
   * This ensures that the capacity is at least 'minCapacity'.
   *
   * @param minCapacity the minimum acceptable capacity. minCapacity is type long, but &gt;=
   *     Integer.MAX_VALUE will throw exception.
   */
  @Override
  public void ensureCapacity(final long minCapacity) {
    if (array.length < minCapacity) {
      // ensure minCapacity is < Integer.MAX_VALUE
      Math2.ensureArraySizeOkay(minCapacity, "IntArray");
      // caller may know exact number needed, so don't double above 2x current size
      int newCapacity = (int) Math.min(Integer.MAX_VALUE - 1, array.length + (long) array.length);
      if (newCapacity < minCapacity) newCapacity = (int) minCapacity; // safe since checked above
      Math2.ensureMemoryAvailable(4L * newCapacity, "IntArray");
      final int[] newArray = new int[newCapacity];
      System.arraycopy(array, 0, newArray, 0, size);
      array = newArray; // do last to minimize concurrency problems
    }
  }

  /**
   * This returns an array (perhaps 'array') which has 'size' elements.
   *
   * @return an array (perhaps 'array') which has 'size' elements. Unsigned integer types will
   *     return an array with their storage type e.g., ULongArray returns a long[].
   */
  public int[] toArray() {
    if (array.length == size) return array;
    Math2.ensureMemoryAvailable(4L * size, "IntArray.toArray");
    // this is faster than making array then arraycopy because it doesn't have to fill the initial
    // array with 0's
    return Arrays.copyOfRange(array, 0, size);
  }

  /**
   * This returns a primitive[] (perhaps 'array') which has 'size' elements.
   *
   * @return a primitive[] (perhaps 'array') which has 'size' elements. Unsigned integer types will
   *     return an array with their storage type e.g., ULongArray returns a long[].
   */
  @Override
  public Object toObjectArray() {
    return toArray();
  }

  /**
   * This returns a BitSet with the bit associated with each element in this IntArray is set.
   * Negative numbers are skipped. The bitSet will be sized so that that last bit is set. For
   * example, {1,4,5} will return 0,1,0,0,1,1
   *
   * @return an array (perhaps 'array') which has 'size' elements.
   */
  public BitSet toBitSet() {
    final double stats[] = calculateStats();
    final int max = Math2.roundToInt(stats[PrimitiveArray.STATS_MAX]);
    final BitSet bitSet = new BitSet(max + 1); // +1 so 0..max
    for (int i = 0; i < size; i++) {
      int ti = array[i];
      if (ti >= 0) bitSet.set(ti);
    }
    return bitSet;
  }

  /**
   * This returns a double[] (perhaps 'array') which has 'size' elements.
   *
   * @return a double[] (perhaps 'array') which has 'size' elements. If maxIsMV, Integer.MAX_VALUE
   *     is converted to Double.NaN.
   */
  @Override
  public double[] toDoubleArray() {
    Math2.ensureMemoryAvailable(8L * size, "IntArray.toDoubleArray");
    final double dar[] = new double[size];
    for (int i = 0; i < size; i++) {
      final int j = array[i];
      dar[i] = maxIsMV && j == Integer.MAX_VALUE ? Double.NaN : j;
    }
    return dar;
  }

  /**
   * This returns a String[] which has 'size' elements.
   *
   * @return a String[] which has 'size' elements. Integer.MAX_VALUE appears as "".
   */
  @Override
  public String[] toStringArray() {
    Math2.ensureMemoryAvailable(
        8L * size, "IntArray.toStringArray"); // 8L is feeble minimal estimate
    final String sar[] = new String[size];
    for (int i = 0; i < size; i++) {
      int j = array[i];
      sar[i] = maxIsMV && j == Integer.MAX_VALUE ? "" : String.valueOf(j);
    }
    return sar;
  }

  /**
   * This gets a specified element.
   *
   * @param index 0 ... size-1
   * @return the specified element
   */
  public int get(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in IntArray.get: index (" + index + ") >= size (" + size + ").");
    return array[index];
  }

  /**
   * This sets a specified element.
   *
   * @param index 0 ... size-1
   * @param value the value for that element
   */
  public void set(final int index, final int value) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in IntArray.set: index (" + index + ") >= size (" + size + ").");
    array[index] = value;
  }

  /**
   * Return a value from the array as an int.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int.
   */
  @Override
  public int getInt(final int index) {
    return get(index);
  }

  // getRawInt(index) uses default getInt(index)

  /**
   * Set a value in the array as an int.
   *
   * @param index the index number 0 .. size-1
   * @param i the value.
   */
  @Override
  public void setInt(final int index, final int i) {
    set(index, i);
  }

  /**
   * Return a value from the array as a long.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a long. If maxIsMV, Integer.MAX_VALUE is returned as Long.MAX_VALUE.
   */
  @Override
  public long getLong(final int index) {
    final int i = get(index);
    return maxIsMV && i == Integer.MAX_VALUE ? Long.MAX_VALUE : i;
  }

  /**
   * Set a value in the array as a long.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToInt(long).
   */
  @Override
  public void setLong(final int index, final long i) {
    if (i < Integer.MIN_VALUE || i > Integer.MAX_VALUE) {
      maxIsMV = true;
      set(index, Integer.MAX_VALUE);
    } else {
      set(index, (int) i);
    }
  }

  /**
   * Return a value from the array as a ulong.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a ulong. If maxIsMV, MAX_VALUE is returned as null.
   */
  @Override
  public BigInteger getULong(final int index) {
    final int b = get(index);
    return maxIsMV && b == Integer.MAX_VALUE ? null : new BigInteger("" + b);
  }

  /**
   * Set a value in the array as a ulong.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToByte(long).
   */
  @Override
  public void setULong(final int index, final BigInteger i) {
    setDouble(
        index,
        i == null ? Double.NaN : i.doubleValue()); // easier to work with. handles NaN. wide range
  }

  /**
   * Return a value from the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a float. String values are parsed with String2.parseFloat and so may
   *     return Float.NaN. Int.MAX_VALUE is returned as Float.NaN.
   */
  @Override
  public float getFloat(final int index) {
    final int i = get(index);
    return maxIsMV && i == Integer.MAX_VALUE ? Float.NaN : i;
  }

  /**
   * Set a value in the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. For numeric PrimitiveArray, it is narrowed if needed by methods like
   *     Math2.roundToInt(d).
   */
  @Override
  public void setFloat(final int index, final float d) {
    if (!maxIsMV && (!Float.isFinite(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE))
      maxIsMV = true;
    set(index, Math2.roundToInt(d));
  }

  /**
   * Return a value from the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN. Int.MAX_VALUE is returned as Double.NaN.
   */
  @Override
  public double getDouble(final int index) {
    final int i = get(index);
    return maxIsMV && i == Integer.MAX_VALUE ? Double.NaN : i;
  }

  /**
   * If this is a signed integer type, this makes an unsigned variant (e.g., PAType.BYTE returns a
   * PAType.UBYTE). The values from pa are then treated as unsigned, e.g., -1 in ByteArray becomes
   * 255 in a UByteArray.
   *
   * @return a new unsigned PrimitiveArray, or this pa.
   */
  @Override
  public PrimitiveArray makeUnsignedPA() {
    Math2.ensureMemoryAvailable(4L * size, "IntArray");
    final int ar[] = new int[size];
    System.arraycopy(array, 0, ar, 0, size);
    return new UIntArray(ar);
  }

  /**
   * Return a value from the array as a double. FloatArray converts float to double in a simplistic
   * way. For this variant: Integer source values will be treated as unsigned (e.g., a ByteArray
   * with -1 returns 255).
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  @Override
  public double getUnsignedDouble(final int index) {
    return Integer.toUnsignedLong(get(index));
  }

  /**
   * Return a value from the array as a double. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS (even if maxIsMV=true).
   *
   * <p>All integerTypes overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  @Override
  public double getRawDouble(final int index) {
    return get(index);
  }

  /**
   * Set a value in the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. For numeric PrimitiveArray, it is narrowed if needed by methods like
   *     Math2.roundToInt(d).
   */
  @Override
  public void setDouble(final int index, final double d) {
    if (!maxIsMV && (!Double.isFinite(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE))
      maxIsMV = true;
    set(index, Math2.roundToInt(d));
  }

  /**
   * Return a value from the array as a String (where the cohort missing value appears as "", not a
   * value).
   *
   * @param index the index number 0 ..
   * @return For numeric types, this returns (String.valueOf(ar[index])), or "" for NaN or infinity.
   *     If this PA is unsigned, this method returns the unsigned value.
   */
  @Override
  public String getString(final int index) {
    final int i = get(index);
    return maxIsMV && i == Integer.MAX_VALUE ? "" : String.valueOf(i);
  }

  /**
   * Return a value from the array as a String suitable for a JSON file. char returns a String with
   * 1 character. String returns a json String with chars above 127 encoded as \\udddd.
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "null" for NaN or infinity. If
   *     this PA is unsigned, this method returns the unsigned value (never "null").
   */
  @Override
  public String getJsonString(final int index) {
    final int i = get(index);
    return maxIsMV && i == Integer.MAX_VALUE ? "null" : String.valueOf(i);
  }

  /**
   * Return a value from the array as a String. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS, regardless of maxIsMV. FloatArray and
   * DoubleArray return "" if the stored value is NaN.
   *
   * <p>All integerTypes overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a String.
   */
  @Override
  public String getRawString(final int index) {
    return String.valueOf(get(index));
  }

  /**
   * Set a value in the array as a String.
   *
   * @param index the index number 0 ..
   * @param s the value. For numeric PrimitiveArray's, it is parsed with String2.parseInt.
   */
  @Override
  public void setString(final int index, final String s) {
    final Integer io = String2.parseIntObject(s); // handles NaN and mv
    if (io == null) {
      maxIsMV = true;
      set(index, Integer.MAX_VALUE);
    } else {
      set(index, io.intValue());
    }
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final int lookFor) {
    return indexOf(lookFor, 0);
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final int lookFor, final int startIndex) {
    for (int i = startIndex; i < size; i++) if (array[i] == lookFor) return i;
    return -1;
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  @Override
  public int indexOf(final String lookFor, final int startIndex) {
    if (startIndex >= size) return -1;
    return indexOf(String2.parseInt(lookFor), startIndex);
  }

  /**
   * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int lastIndexOf(final int lookFor, final int startIndex) {
    if (startIndex >= size)
      throw new IllegalArgumentException(
          String2.ERROR
              + " in IntArray.get: startIndex ("
              + startIndex
              + ") >= size ("
              + size
              + ").");
    for (int i = startIndex; i >= 0; i--) if (array[i] == lookFor) return i;
    return -1;
  }

  /**
   * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  @Override
  public int lastIndexOf(final String lookFor, final int startIndex) {
    return lastIndexOf(String2.parseInt(lookFor), startIndex);
  }

  /** If size != capacity, this makes a new 'array' of size 'size' so capacity will equal size. */
  @Override
  public void trimToSize() {
    array = toArray();
  }

  /**
   * Test if o is an IntArray with the same size and values.
   *
   * @param o the object that will be compared to this IntArray
   * @return true if equal. o=null returns false.
   */
  @Override
  public boolean equals(final Object o) {
    return testEquals(o).length() == 0;
  }

  /**
   * Test if o is an IntArray with the same size and values, but returns a String describing the
   * difference (or "" if equal).
   *
   * @param o
   * @return a String describing the difference (or "" if equal). o=null doesn't throw an exception.
   */
  @Override
  public String testEquals(final Object o) {
    if (!(o instanceof IntArray))
      return "The two objects aren't equal: this object is a IntArray; the other is a "
          + (o == null ? "null" : o.getClass().getName())
          + ".";
    final IntArray other = (IntArray) o;
    if (other.size() != size)
      return "The two IntArrays aren't equal: one has "
          + size
          + " value(s); the other has "
          + other.size()
          + " value(s).";
    for (int i = 0; i < size; i++)
      if (getLong(i) != other.getLong(i)) // getLong handles mv
      return "The two IntArrays aren't equal: this["
            + i
            + "]="
            + getLong(i)
            + "; other["
            + i
            + "]="
            + other.getLong(i)
            + ".";
    // if (maxIsMV != other.maxIsMV)
    //     return "The two ByteArrays aren't equal: this.maxIsMV=" + maxIsMV +
    //                                          "; other.maxIsMV=" + other.maxIsMV + ".";
    return "";
  }

  /**
   * This converts the elements into a Comma-Space-Separated-Value (CSSV) String. Integer types show
   * MAX_VALUE numbers (not "").
   *
   * @return a Comma-Space-Separated-Value (CSSV) String representation
   */
  @Override
  public String toString() {
    return String2.toCSSVString(toArray()); // toArray() get just 'size' elements
  }

  /**
   * This converts the elements into an NCCSV attribute String, e.g.,: -128b, 127b Integer types
   * show MAX_VALUE numbers (not "").
   *
   * @return an NCCSV attribute String
   */
  @Override
  public String toNccsvAttString() {
    final StringBuilder sb = new StringBuilder(size * 10);
    for (int i = 0; i < size; i++) sb.append((i == 0 ? "" : ",") + array[i] + "i");
    return sb.toString();
  }

  /**
   * This sorts the elements in ascending order. To get the elements in reverse order, just read
   * from the end of the list to the beginning.
   */
  @Override
  public void sort() {
    // see switchover point and speed comparison in
    //  https://www.baeldung.com/java-arrays-sort-vs-parallelsort
    if (size < 8192) Arrays.sort(array, 0, size);
    else Arrays.parallelSort(array, 0, size);
  }

  /**
   * This compares the values in this.row1 and otherPA.row2 and returns a negative integer, zero, or
   * a positive integer if the value at index1 is less than, equal to, or greater than the value at
   * index2. The cohort missing value sorts highest. Currently, this does not range check index1 and
   * index2, so the caller should be careful.
   *
   * @param index1 an index number 0 ... size-1
   * @param otherPA the other PrimitiveArray which must be the same (or close) PAType.
   * @param index2 an index number 0 ... size-1
   * @return returns a negative integer, zero, or a positive integer if the value at index1 is less
   *     than, equal to, or greater than the value at index2. Think "array[index1] - array[index2]".
   */
  @Override
  public int compare(final int index1, final PrimitiveArray otherPA, final int index2) {
    return Long.compare(getLong(index1), otherPA.getLong(index2)); // long handles mv
  }

  /**
   * This copies the value in row 'from' to row 'to'. This does not check that 'from' and 'to' are
   * valid; the caller should be careful. The value for 'from' is unchanged.
   *
   * @param from an index number 0 ... size-1
   * @param to an index number 0 ... size-1
   */
  @Override
  public void copy(final int from, final int to) {
    array[to] = array[from];
  }

  /**
   * This reorders the values in 'array' based on rank.
   *
   * @param rank is an int with values (0 ... size-1) which points to the row number for a row with
   *     a specific rank (e.g., rank[0] is the row number of the first item in the sorted list,
   *     rank[1] is the row number of the second item in the sorted list, ...).
   */
  @Override
  public void reorder(final int rank[]) {
    final int n = rank.length;
    // new length could be n, but I'll keep it the same array.length as before
    Math2.ensureMemoryAvailable(4L * array.length, "IntArray");
    final int newArray[] = new int[array.length];
    for (int i = 0; i < n; i++) newArray[i] = array[rank[i]];
    array = newArray;
  }

  /**
   * This reverses the order of the bytes in each value, e.g., if the data was read from a
   * little-endian source.
   */
  @Override
  public void reverseBytes() {
    for (int i = 0; i < size; i++) array[i] = Integer.reverseBytes(array[i]);
  }

  /**
   * This writes 'size' elements to a DataOutputStream.
   *
   * @param dos the DataOutputStream
   * @return the number of bytes used per element (for Strings, this is the size of one of the
   *     strings, not others, and so is useless; for other types the value is consistent). But if
   *     size=0, this returns 0.
   * @throws Exception if trouble
   */
  @Override
  public int writeDos(final DataOutputStream dos) throws Exception {
    for (int i = 0; i < size; i++) dos.writeInt(array[i]);
    return size == 0 ? 0 : 4;
  }

  /**
   * This writes one element to a DataOutputStream.
   *
   * @param dos the DataOutputStream
   * @param i the index of the element to be written
   * @return the number of bytes used for this element (for Strings, this varies; for others it is
   *     consistent)
   * @throws Exception if trouble
   */
  @Override
  public int writeDos(final DataOutputStream dos, final int i) throws Exception {
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
  @Override
  public void readDis(final DataInputStream dis, final int n) throws Exception {
    ensureCapacity(size + (long) n);
    for (int i = 0; i < n; i++) array[size++] = dis.readInt();
  }

  /**
   * This reads/adds n 24-bit elements from a DataInputStream.
   *
   * @param dis the DataInputStream
   * @param n the number of elements to be read/added
   * @throws Exception if trouble
   */
  public void read24BitDis(final DataInputStream dis, final int n, final boolean bigEndian)
      throws Exception {
    ensureCapacity(size + (long) n);
    final byte bar[] = new byte[3];
    for (int i = 0; i < n; i++) {
      dis.readFully(bar);
      array[size++] =
          bigEndian
              ? ((((bar[0] & 0xFF) << 24) | ((bar[1] & 0xFF) << 16) | ((bar[2] & 0xFF) << 8)) >> 8)
              : ((((bar[2] & 0xFF) << 24) | ((bar[1] & 0xFF) << 16) | ((bar[0] & 0xFF) << 8)) >> 8);
    }
  }

  /**
   * This is like read24BitDis, but doesn't do &gt;&gt;8.
   *
   * @param dis the DataInputStream
   * @param n the number of elements to be read/added
   * @throws Exception if trouble
   */
  public void read24BitDisAudio(final DataInputStream dis, final int n, final boolean bigEndian)
      throws Exception {
    ensureCapacity(size + (long) n);
    final byte bar[] = new byte[3];
    for (int i = 0; i < n; i++) {
      dis.readFully(bar);
      array[size++] =
          bigEndian
              ? (((bar[0] & 0xFF) << 24) | ((bar[1] & 0xFF) << 16) | ((bar[2] & 0xFF) << 8))
              : (((bar[2] & 0xFF) << 24) | ((bar[1] & 0xFF) << 16) | ((bar[0] & 0xFF) << 8));
    }
  }

  /**
   * This reads/appends int values to this PrimitiveArray from a DODS DataInputStream, and is thus
   * the complement of externalizeForDODS.
   *
   * @param dis
   * @throws IOException if trouble
   */
  @Override
  public void internalizeFromDODS(final DataInputStream dis) throws java.io.IOException {
    final int nValues = dis.readInt();
    dis.readInt(); // skip duplicate of nValues
    ensureCapacity(size + (long) nValues);
    for (int i = 0; i < nValues; i++) array[size++] = dis.readInt();
  }

  /**
   * This writes array[index] to a randomAccessFile at the current position.
   *
   * @param raf the RandomAccessFile
   * @param index
   * @throws Exception if trouble
   */
  @Override
  public void writeToRAF(final RandomAccessFile raf, final int index) throws Exception {
    raf.writeInt(get(index));
  }

  /**
   * This reads one value from a randomAccessFile at the current position and adds it to the
   * PrimitiveArraay.
   *
   * @param raf the RandomAccessFile
   * @throws Exception if trouble
   */
  @Override
  public void readFromRAF(final RandomAccessFile raf) throws Exception {
    add(raf.readInt());
  }

  /**
   * This fully skips 'n' elements in the dataInputStream.
   *
   * @param dis
   * @param n the number of elements to skip
   * @throws Exception if trouble
   */
  public static void disSkip(final DataInputStream dis, final int n) throws Exception {
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
  public static double readDisAsDouble(final DataInputStream dis) throws Exception {
    final int i = dis.readInt();
    return i == Integer.MAX_VALUE ? Double.NaN : i;
  }

  /**
   * This appends the data in another pa to the current data. WARNING: information may be lost from
   * the incoming pa if this primitiveArray is of a smaller type; see needPAType().
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     pa.getInt.
   */
  @Override
  public void append(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof IntArray ia) {
      if (pa.getMaxIsMV()) setMaxIsMV(true);
      System.arraycopy(ia.array, 0, array, size, otherSize);
      size += otherSize;
    } else {
      for (int i = 0; i < otherSize; i++)
        addLong(pa.getLong(i)); // this converts mv's and handles maxIsMV
    }
  }

  /**
   * This appends the data in another pa to the current data. This "raw" variant leaves missingValue
   * from smaller data types (e.g., ByteArray missingValue=127) AS IS. WARNING: information may be
   * lost from the incoming pa if this primitiveArray is of a simpler type.
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     pa.getInt.
   */
  @Override
  public void rawAppend(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof IntArray ia) {
      System.arraycopy(ia.array, 0, array, size, otherSize);
    } else {
      for (int i = 0; i < otherSize; i++)
        array[size + i] = pa.getRawInt(i); // this DOESN'T convert mv's
    }
    size += otherSize; // do last to minimize concurrency problems
  }

  /**
   * This populates 'indices' with the indices (ranks) of the values in this IntArray (ties get the
   * same index). For example, 10,10,25,3 returns 1,1,2,0.
   *
   * @param indices the intArray that will capture the indices of the values (ties get the same
   *     index). For example, 10,10,25,3 returns 1,1,2,0.
   * @return a PrimitveArray (the same type as this class) with the unique values, sorted. If all
   *     the values are unique and already sorted, this returns 'this'.
   */
  @Override
  public PrimitiveArray makeIndices(final IntArray indices) {
    indices.clear();
    if (size == 0) {
      return new IntArray();
    }

    // make a hashMap with all the unique values (associated values are initially all dummy)
    final Integer dummy = Integer.valueOf(-1);
    final HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
    int lastValue = array[0]; // since lastValue often equals currentValue, cache it
    hashMap.put(Integer.valueOf(lastValue), dummy);
    boolean alreadySorted = true;
    for (int i = 1; i < size; i++) {
      int currentValue = array[i];
      if (currentValue != lastValue) {
        if (currentValue < lastValue) alreadySorted = false;
        lastValue = currentValue;
        hashMap.put(Integer.valueOf(lastValue), dummy);
      }
    }

    // quickly deal with: all unique and already sorted
    final Set keySet = hashMap.keySet();
    int nUnique = keySet.size();
    if (nUnique == size && alreadySorted) {
      indices.ensureCapacity(size);
      for (int i = 0; i < size; i++) indices.add(i);
      return this; // the PrimitiveArray with unique values
    }

    // store all the elements in an array
    final Object unique[] = new Object[nUnique];
    final Iterator iterator = keySet.iterator();
    int count = 0;
    while (iterator.hasNext()) unique[count++] = iterator.next();
    if (nUnique != count)
      throw new RuntimeException(
          "IntArray.makeRankArray nUnique(" + nUnique + ") != count(" + count + ")!");

    // sort them
    Arrays.sort(unique);

    // put the unique values back in the hashMap with the ranks as the associated values
    // and make tUnique
    final int tUnique[] = new int[nUnique];
    for (int i = 0; i < count; i++) {
      hashMap.put(unique[i], Integer.valueOf(i));
      tUnique[i] = ((Integer) unique[i]).intValue();
    }

    // convert original values to ranks
    final int ranks[] = new int[size];
    lastValue = array[0];
    ranks[0] = ((Integer) hashMap.get(Integer.valueOf(lastValue))).intValue();
    int lastRank = ranks[0];
    for (int i = 1; i < size; i++) {
      if (array[i] == lastValue) {
        ranks[i] = lastRank;
      } else {
        lastValue = array[i];
        ranks[i] = ((Integer) hashMap.get(Integer.valueOf(lastValue))).intValue();
        lastRank = ranks[i];
      }
    }

    // store the results in ranked
    indices.append(new IntArray(ranks));

    return new IntArray(tUnique);
  }

  /**
   * This changes all instances of the first value to the second value.
   *
   * @param tFrom the original value (use "" or "NaN" for standard missingValue)
   * @param tTo the new value (use "" or "NaN" for standard missingValue)
   * @return the number of values switched
   */
  @Override
  public int switchFromTo(final String tFrom, final String tTo) {
    final int from = Math2.roundToInt(String2.parseDouble(tFrom));
    final double d = String2.parseDouble(tTo);
    final int to = Math2.roundToInt(d);
    if (from == to) return 0;
    int count = 0;
    for (int i = 0; i < size; i++) {
      if (array[i] == from) {
        array[i] = to;
        count++;
      }
    }
    if (count > 0 && Double.isNaN(d)) maxIsMV = true;
    return count;
  }

  /**
   * This tests for adjacent tied values and returns the index of the first tied value. Adjacent
   * NaNs are treated as ties.
   *
   * @return the index of the first tied value (or -1 if none).
   */
  @Override
  public int firstTie() {
    for (int i = 1; i < size; i++) {
      if (array[i - 1] == array[i]) {
        return i - 1;
      }
    }
    return -1;
  }

  /**
   * This finds the number of non-missing values, and the index of the min and max value.
   *
   * @return int[3], [0]=the number of non-missing values, [1]=index of min value (if tie, index of
   *     last found; -1 if all mv), [2]=index of max value (if tie, index of last found; -1 if all
   *     mv).
   */
  @Override
  public int[] getNMinMaxIndex() {
    int n = 0, tmini = -1, tmaxi = -1;
    int tmin = Integer.MAX_VALUE;
    int tmax = Integer.MIN_VALUE;
    for (int i = 0; i < size; i++) {
      int v = array[i];
      if (maxIsMV && v == Integer.MAX_VALUE) {
      } else {
        n++;
        if (v <= tmin) {
          tmini = i;
          tmin = v;
        }
        if (v >= tmax) {
          tmaxi = i;
          tmax = v;
        }
      }
    }
    return new int[] {n, tmini, tmaxi};
  }

  /**
   * For integer types, this fixes unsigned bytes that were incorrectly read as signed so that they
   * have the correct ordering of values (0 to 255 becomes -128 to 127). <br>
   * What were read as signed: 0 127 -128 -1 <br>
   * should become unsigned: -128 -1 0 255 <br>
   * This also does the reverse. <br>
   * For non-integer types, this does nothing.
   */
  @Override
  public void changeSignedToFromUnsigned() {
    for (int i = 0; i < size; i++) {
      final int i2 = array[i];
      array[i] =
          i2 < 0
              ? i2 + Integer.MAX_VALUE + 1
              : i2 - Integer.MAX_VALUE - 1; // order of ops is important
    }
  }
}
