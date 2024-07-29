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
 * FloatArray is a thin shell over a float[] with methods like ArrayList's methods; it extends
 * PrimitiveArray.
 */
public class FloatArray extends PrimitiveArray {

  public static final FloatArray MV9 = new FloatArray(DoubleArray.MV9);

  /**
   * This is the main data structure. This should be private, but is public so you can manipulate it
   * if you promise to be careful. Note that if the PrimitiveArray's capacity is increased, the
   * PrimitiveArray will use a different array for storage.
   */
  public float[] array;

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
    return Float.NaN;
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
    return !Float.isFinite(get(index));
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
    return isMaxValue(index);
  }

  /** A constructor for a capacity of 8 elements. The initial 'size' will be 0. */
  public FloatArray() {
    array = new float[8];
  }

  /**
   * This constructs a FloatArray by copying elements from the incoming PrimitiveArray (using
   * append()).
   *
   * @param primitiveArray a primitiveArray of any type
   */
  public FloatArray(final PrimitiveArray primitiveArray) {
    Math2.ensureMemoryAvailable(4L * primitiveArray.size(), "FloatArray");
    array = new float[primitiveArray.size()]; // exact size
    append(primitiveArray);
  }

  /**
   * A constructor for a specified number of elements. The initial 'size' will be 0.
   *
   * @param capacity creates an FloatArray with the specified initial capacity.
   * @param active if true, size will be set to capacity and all elements will equal 0; else size =
   *     0.
   */
  public FloatArray(final int capacity, final boolean active) {
    Math2.ensureMemoryAvailable(4L * capacity, "FloatArray");
    array = new float[capacity];
    if (active) size = capacity;
  }

  /**
   * A constructor which (at least initially) uses the array and all its elements ('size' will equal
   * anArray.length).
   *
   * @param anArray the array to be used as this object's array.
   */
  public FloatArray(final float[] anArray) {
    array = anArray;
    size = anArray.length;
  }

  /**
   * This makes a FloatArray from the comma-separated values. <br>
   * null becomes pa.length() == 0. <br>
   * "" becomes pa.length() == 0. <br>
   * " " becomes pa.length() == 1. <br>
   * See also PrimitiveArray.csvFactory(paType, csv);
   *
   * @param csv the comma-separated-value string
   * @return a FloatArray from the comma-separated values.
   */
  public static final FloatArray fromCSV(final String csv) {
    return (FloatArray) PrimitiveArray.csvFactory(PAType.FLOAT, csv);
  }

  /**
   * This returns a new PAOne with the minimum value that can be held by this class.
   *
   * @return a new PAOne with the minimum value that can be held by this class, e.g., -128b for
   *     ByteArray.
   */
  @Override
  public final PAOne MINEST_VALUE() {
    return PAOne.fromFloat(-Float.MAX_VALUE);
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
    return PAOne.fromFloat(Float.MAX_VALUE);
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

  /** This indicates if this class' type is PAType.FLOAT or PAType.DOUBLE. */
  @Override
  public final boolean isFloatingPointType() {
    return true;
  }

  /**
   * This returns the hashcode for this FloatArray (dependent only on values, not capacity).
   * WARNING: the algorithm used may change in future versions.
   *
   * @return the hashcode for this FloatArray (dependent only on values, not capacity)
   */
  @Override
  public int hashCode() {
    // see
    // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html#hashCode()
    // and
    // https://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
    int code = 0;
    for (int i = 0; i < size; i++) code = 31 * code + Float.floatToIntBits(array[i]);
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
    if (stopIndex < startIndex) return pa == null ? new FloatArray(new float[0]) : pa;

    final int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
    FloatArray fa = null;
    if (pa == null) {
      fa = new FloatArray(willFind, true);
    } else {
      fa = (FloatArray) pa;
      fa.ensureCapacity(willFind);
      fa.size = willFind;
    }
    final float tar[] = fa.array;
    if (stride == 1) {
      System.arraycopy(array, startIndex, tar, 0, willFind);
    } else {
      int po = 0;
      for (int i = startIndex; i <= stopIndex; i += stride) tar[po++] = array[i];
    }
    return fa;
  }

  /**
   * This returns the PAType (PAType.FLOAT) of the element type.
   *
   * @return the PAType (PAType.FLOAT) of the element type.
   */
  @Override
  public PAType elementType() {
    return PAType.FLOAT;
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
      case BYTE, UBYTE, SHORT, USHORT, FLOAT -> PAType.FLOAT;

        // if sideways
      case INT, UINT, DOUBLE -> PAType.DOUBLE;

        // LONG, ULONG, CHAR, STRING
      default -> PAType.STRING;
    };
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array
   */
  public void add(final float value) {
    if (size == array.length) // if we're at capacity
    ensureCapacity(size + 1L);
    array[size++] = value;
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array. If value instanceof Number, this uses
   *     Number.floatValue(). If null or not a Number, this adds Float.NaN.
   */
  @Override
  public void addObject(final Object value) {
    if (size == array.length) // if we're at capacity
    ensureCapacity(size + 1L);
    array[size++] = value != null && value instanceof Number na ? na.floatValue() : Float.NaN;
  }

  /**
   * This reads one value from the StrutureData and adds it to this PA.
   *
   * @param sd from an .nc file
   * @param memberName
   */
  @Override
  public void add(final StructureData sd, final String memberName) {
    add(sd.getScalarFloat(memberName));
  }

  /**
   * This adds all the values from ar.
   *
   * @param ar an array
   */
  public void add(final float ar[]) {
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
  public void addN(final int n, final float value) {
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
  public void atInsert(final int index, final float value) {
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
    atInsert(index, String2.parseFloat(value));
  }

  /**
   * This adds n PAOne's to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a PAOne (or null).
   */
  @Override
  public void addNPAOnes(final int n, final PAOne value) {
    addN(n, value == null ? Float.NaN : value.getFloat());
  }

  /**
   * This adds n Strings to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a String.
   */
  @Override
  public void addNStrings(final int n, final String value) {
    addN(n, String2.parseFloat(value));
  }

  /**
   * This adds n floats to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a float.
   */
  @Override
  public void addNFloats(final int n, final float value) {
    addN(n, value);
  }

  /**
   * This adds n doubles to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a double.
   */
  @Override
  public void addNDoubles(final int n, final double value) {
    addN(n, Math2.doubleToFloatNaN(value));
  }

  /**
   * This adds n ints to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public void addNInts(final int n, final int value) {
    addN(n, value); // ! assumes value=Integer.MAX_VALUE isn't maxIsMV
  }

  /**
   * This adds n longs to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public void addNLongs(final int n, final long value) {
    addN(n, (float) value); // ! assumes value=Integer.MAX_VALUE isn't maxIsMV
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
                + " in FloatArray.addFromPA: otherIndex="
                + otherIndex
                + " + nValues="
                + nValues
                + " > otherPA.size="
                + otherPA.size);
      ensureCapacity(size + nValues);
      System.arraycopy(((FloatArray) otherPA).array, otherIndex, array, size, nValues);
      size += nValues;
      return this;
    }

    // add from different type
    for (int i = 0; i < nValues; i++) add(otherPA.getFloat(otherIndex++)); // does error checking
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
    set(index, otherPA.getFloat(otherIndex));
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
          String2.ERROR + " in FloatArray.removeRange: to (" + to + ") > size (" + size + ").");
    if (from >= to) {
      if (from == to) return;
      throw new IllegalArgumentException(
          String2.ERROR + " in FloatArray.removeRange: from (" + from + ") > to (" + to + ").");
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
    final String errorIn = String2.ERROR + " in FloatArray.move:\n";

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
    final float[] temp = new float[nToMove];
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
      Math2.ensureArraySizeOkay(minCapacity, "FloatArray");
      // caller may know exact number needed, so don't double above 2x current size
      int newCapacity = (int) Math.min(Integer.MAX_VALUE - 1, array.length + (long) array.length);
      if (newCapacity < minCapacity) newCapacity = (int) minCapacity; // safe since checked above
      Math2.ensureMemoryAvailable(4L * newCapacity, "FloatArray");
      final float[] newArray = new float[newCapacity];
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
  public float[] toArray() {
    if (array.length == size) return array;
    Math2.ensureMemoryAvailable(4L * size, "FloatArray.toArray");
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
   * This returns a double[] (perhaps 'array') which has 'size' elements. Note that the floats will
   * be bruised, so you may want to use Math2.niceDouble(f, 7) on all of the values.
   *
   * @return a double[] (perhaps 'array') which has 'size' elements. Float.MAX_VALUE is converted to
   *     Double.NaN.
   */
  @Override
  public double[] toDoubleArray() {
    Math2.ensureMemoryAvailable(8L * size, "FloatArray.toDoubleArray");
    final double dar[] = new double[size];
    for (int i = 0; i < size; i++) dar[i] = array[i];
    return dar;
  }

  /**
   * This returns a String[] which has 'size' elements.
   *
   * @return a String[] which has 'size' elements. If a value isn't finite, it appears as "".
   */
  @Override
  public String[] toStringArray() {
    Math2.ensureMemoryAvailable(
        12L * size, "FloatArray.toStringArray"); // 12L is feeble minimal estimate
    final String sar[] = new String[size];
    for (int i = 0; i < size; i++) sar[i] = getString(i);
    return sar;
  }

  /**
   * This gets a specified element.
   *
   * @param index 0 ... size-1
   * @return the specified element
   */
  public float get(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in FloatArray.get: index (" + index + ") >= size (" + size + ").");
    return array[index];
  }

  /**
   * This sets a specified element.
   *
   * @param index 0 ... size-1
   * @param value the value for that element
   */
  public void set(final int index, final float value) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in FloatArray.set: index (" + index + ") >= size (" + size + ").");
    array[index] = value;
  }

  /**
   * Return a value from the array as an int. Floating point values are rounded.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int. This may return Integer.MAX_VALUE.
   */
  @Override
  public int getInt(final int index) {
    return Math2.roundToInt(get(index));
  }

  // getRawInt(index) uses default getInt(index) since missingValue must be converted

  /**
   * Set a value in the array as an int.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. Integer.MAX_VALUE is NOT converted to this type's missing value.
   */
  @Override
  public void setInt(final int index, final int i) {
    set(index, i);
  }

  /**
   * Return a value from the array as a long.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a long. This may return Long.MAX_VALUE.
   */
  @Override
  public long getLong(final int index) {
    return Math2.roundToLong(get(index));
  }

  /**
   * Set a value in the array as a long.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. Long.MAX_VALUE is NOT converted to Float.NaN.
   */
  @Override
  public void setLong(final int index, final long i) {
    set(index, (float) i);
  }

  /**
   * Return a value from the array as a ulong.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a ulong. NaN is returned as null.
   */
  @Override
  public BigInteger getULong(final int index) {
    final float b = get(index);
    return Float.isFinite(b) ? Math2.roundToULongOrNull(b) : null;
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
    setDouble(index, Math2.ulongToDoubleNaN(i));
  }

  /**
   * Return a value from the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a float. String values are parsed with String2.parseFloat and so may
   *     return Float.NaN.
   */
  @Override
  public float getFloat(final int index) {
    return get(index);
  }

  /**
   * Set a value in the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @param d the value.
   */
  @Override
  public void setFloat(final int index, final float d) {
    set(index, d);
  }

  /**
   * Return a value from the array as a double. FloatArray converts float to double in a simplistic
   * way.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  @Override
  public double getDouble(final int index) {
    return get(index);
  }

  /**
   * Return a value from the array as a double. FloatArray converts float to double via
   * Math2.floatToDouble. This is fine if e.g., 32.0000000001 becomes 32.0, but not great if
   * 32.83333 becomes 32.8333.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  @Override
  public double getNiceDouble(final int index) {
    return Math2.floatToDouble(get(index));
  }

  /**
   * Return a value from the array as a double. FloatArray converts float to double via
   * Math2.floatToDouble. This is fine if e.g., 32.0000000001 becomes 32.0, but not great if
   * 32.83333 becomes 32.8333.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  @Override
  public double getRawNiceDouble(final int index) {
    return getNiceDouble(index);
  }

  /**
   * Set a value in the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. It is narrowed by Math2.doubleToFloatNaN(d), which converts INFINITY and
   *     large values to NaN.
   */
  @Override
  public void setDouble(final int index, final double d) {
    set(index, Math2.doubleToFloatNaN(d));
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
    float b = get(index);
    return Float.isFinite(b) ? String.valueOf(b) : "";
  }

  /**
   * Return a value from the array as a String suitable for a JSON file. char returns a String with
   * 1 character. String returns a json String with chars above 127 encoded as \\udddd.
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or null for NaN or infinity.
   */
  @Override
  public String getJsonString(final int index) {
    return String2.toJson(get(index));
  }

  /**
   * Return a value from the array as a String. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS, regardless of maxIsMV. FloatArray and
   * DoubleArray return "NaN" if the stored value is NaN. That's different than getRawString!!!
   *
   * <p>Float and DoubleArray overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a String.
   */
  @Override
  public String getRawestString(final int index) {
    return String.valueOf(get(index));
  }

  /**
   * Set a value in the array as a String.
   *
   * @param index the index number 0 ..
   * @param s the value. For numeric PrimitiveArray's, it is parsed with String2.parse and narrowed
   *     if needed by methods like Math2.roundToFloat(d).
   */
  @Override
  public void setString(final int index, final String s) {
    set(index, String2.parseFloat(s));
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for. This correctly handles NaN.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final float lookFor) {
    return indexOf(lookFor, 0);
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for. This correctly handles NaN.
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final float lookFor, final int startIndex) {
    if (Float.isNaN(lookFor)) {
      for (int i = startIndex; i < size; i++) if (Float.isNaN(array[i])) return i;
      return -1;
    }

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
    return indexOf(String2.parseFloat(lookFor), startIndex);
  }

  /**
   * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int lastIndexOf(final float lookFor, final int startIndex) {
    if (startIndex >= size)
      throw new IllegalArgumentException(
          String2.ERROR
              + " in FloatArray.get: startIndex ("
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
    return lastIndexOf(String2.parseFloat(lookFor), startIndex);
  }

  /** If size != capacity, this makes a new 'array' of size 'size' so capacity will equal size. */
  @Override
  public void trimToSize() {
    array = toArray();
  }

  /**
   * Test if o is an FloatArray with the same size and values.
   *
   * @param o the object that will be compared to this FloatArray
   * @return true if equal. o=null returns false.
   */
  @Override
  public boolean equals(final Object o) {
    return testEquals(o).length() == 0;
  }

  /**
   * Test if o is an FloatArray with the same size and values, but returns a String describing the
   * difference (or "" if equal).
   *
   * @param o
   * @return a String describing the difference (or "" if equal). o=null doesn't throw an exception.
   */
  @Override
  public String testEquals(final Object o) {
    if (!(o instanceof FloatArray))
      return "The two objects aren't equal: this object is a FloatArray; the other is a "
          + (o == null ? "null" : o.getClass().getName())
          + ".";
    final FloatArray other = (FloatArray) o;
    if (other.size() != size)
      return "The two FloatArrays aren't equal: one has "
          + size
          + " value(s); the other has "
          + other.size()
          + " value(s).";
    for (int i = 0; i < size; i++)
      if (!Math2.equalsIncludingNanOrInfinite(array[i], other.array[i]))
        return "The two FloatArrays aren't equal: this["
            + i
            + "]="
            + array[i]
            + "; other["
            + i
            + "]="
            + other.array[i]
            + ".";
    return "";
  }

  /**
   * This converts the elements into a Comma-Space-Separated-Value (CSSV) String.
   *
   * @return a Comma-Space-Separated-Value (CSSV) String representation
   */
  @Override
  public String toString() {
    return String2.toCSSVString(toArray()); // toArray() get just 'size' elements
  }

  /**
   * This converts the elements into an NCCSV attribute String, e.g.,: -128b, 127b
   *
   * @return an NCCSV attribute String
   */
  @Override
  public String toNccsvAttString() {
    final StringBuilder sb = new StringBuilder(size * 11);
    for (int i = 0; i < size; i++) sb.append((i == 0 ? "" : ",") + String.valueOf(array[i]) + "f");
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
   * index2. NaN sorts highest. Currently, this does not range check index1 and index2, so the
   * caller should be careful.
   *
   * @param index1 an index number 0 ... size-1
   * @param otherPA the other PrimitiveArray which must be the same (or close) PAType.
   * @param index2 an index number 0 ... size-1
   * @return returns a negative integer, zero, or a positive integer if the value at index1 is less
   *     than, equal to, or greater than the value at index2. Think "array[index1] - array[index2]".
   */
  @Override
  public int compare(final int index1, final PrimitiveArray otherPA, final int index2) {
    // this is approximate when other is bigger (int, uint, long, ulong, double)
    return Float.compare(getFloat(index1), otherPA.getFloat(index2));
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
    Math2.ensureMemoryAvailable(4L * array.length, "FloatArray");
    final float newArray[] = new float[array.length];
    for (int i = 0; i < n; i++) newArray[i] = array[rank[i]];
    array = newArray;
  }

  /**
   * This reverses the order of the bytes in each value, e.g., if the data was read from a
   * little-endian source.
   */
  @Override
  public void reverseBytes() {
    for (int i = 0; i < size; i++)
      // this probably fails for some values since not all bit combos are valid floats
      array[i] = Float.intBitsToFloat(Integer.reverseBytes(Float.floatToIntBits(array[i])));
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
    for (int i = 0; i < size; i++) dos.writeFloat(array[i]);
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
  @Override
  public void readDis(final DataInputStream dis, final int n) throws Exception {
    ensureCapacity(size + (long) n);
    for (int i = 0; i < n; i++) array[size++] = dis.readFloat();
  }

  /**
   * This reads/appends float values to this PrimitiveArray from a DODS DataInputStream, and is thus
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
    for (int i = 0; i < nValues; i++) array[size++] = dis.readFloat();
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
    raf.writeFloat(get(index));
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
    add(raf.readFloat());
  }

  /**
   * This appends the data in another pa to the current data. WARNING: information may be lost from
   * the incoming pa if this primitiveArray is of a smaller type; see needPAType().
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     Math2.doubleToFloatNaN(pa.getDouble).
   */
  @Override
  public void append(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof FloatArray fa) {
      System.arraycopy(fa.array, 0, array, size, otherSize);
    } else {
      for (int i = 0; i < otherSize; i++)
        array[size + i] = Math2.doubleToFloatNaN(pa.getDouble(i)); // this converts mv's
    }
    size += otherSize; // do last to minimize concurrency problems
  }

  /**
   * This appends the data in another pa to the current data. This "raw" variant leaves missingValue
   * from smaller data types (e.g., ByteArray missingValue=127) AS IS (even if maxIsMV=true).
   * WARNING: information may be lost from the incoming pa if this primitiveArray is of a simpler
   * type.
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     Math2.doubleToFloatNaN(pa.getDouble).
   */
  @Override
  public void rawAppend(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof FloatArray fa) {
      System.arraycopy(fa.array, 0, array, size, otherSize);
    } else {
      for (int i = 0; i < otherSize; i++)
        array[size + i] = Math2.doubleToFloatNaN(pa.getRawDouble(i)); // this DOESN'T convert mv's
    }
    size += otherSize; // do last to minimize concurrency problems
  }

  /**
   * This populates 'indices' with the indices (ranks) of the values in this FloatArray (ties get
   * the same index). For example, 10,10,25,3 returns 1,1,2,0.
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
      return new FloatArray();
    }

    // make a hashMap with all the unique values (associated values are initially all dummy)
    final Integer dummy = Integer.valueOf(-1);
    final HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
    float lastValue = array[0]; // since lastValue often equals currentValue, cache it
    hashMap.put(Float.valueOf(lastValue), dummy);
    boolean alreadySorted = true;
    for (int i = 1; i < size; i++) {
      final float currentValue = array[i];
      if (currentValue != lastValue) {
        if (currentValue < lastValue) alreadySorted = false;
        lastValue = currentValue;
        hashMap.put(Float.valueOf(lastValue), dummy);
      }
    }

    // quickly deal with: all unique and already sorted
    final Set keySet = hashMap.keySet();
    final int nUnique = keySet.size();
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
          "FloatArray.makeRankArray nUnique(" + nUnique + ") != count(" + count + ")!");

    // sort them
    Arrays.sort(unique);

    // put the unique values back in the hashMap with the ranks as the associated values
    // and make tUnique
    final float tUnique[] = new float[nUnique];
    for (int i = 0; i < count; i++) {
      hashMap.put(unique[i], Integer.valueOf(i));
      tUnique[i] = ((Float) unique[i]).floatValue();
    }

    // convert original values to ranks
    final int ranks[] = new int[size];
    lastValue = array[0];
    ranks[0] = ((Integer) hashMap.get(Float.valueOf(lastValue))).intValue();
    int lastRank = ranks[0];
    for (int i = 1; i < size; i++) {
      if (array[i] == lastValue) {
        ranks[i] = lastRank;
      } else {
        lastValue = array[i];
        ranks[i] = ((Integer) hashMap.get(Float.valueOf(lastValue))).intValue();
        lastRank = ranks[i];
      }
    }

    // store the results in ranked
    indices.append(new IntArray(ranks));

    return new FloatArray(tUnique);
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
    final float from = String2.parseFloat(tFrom);
    final float to = String2.parseFloat(tTo);
    if ((Float.isNaN(from) && Float.isNaN(to)) || (from == to)) return 0;
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
        // String2.log(">> float.switchFromTo from=" + from + " to=" + to + " i=" + i + " a[i]=" +
        // array[i] + " eq5=" + Math2.almostEqual(5, array[i], from));
        if (Math2.almostEqual(5, array[i], from)) {
          array[i] = to;
          count++;
        }
      }
    }
    // String2.log("FloatArray.switch from=" + tFrom + " to=" + tTo + " n=" + count);
    return count;
  }

  /**
   * This converts a double[] to a float[].
   *
   * @param dar
   * @return the corresponding float[]
   */
  public static float[] toArrayOfFloats(final double dar[]) {
    final int n = dar.length;
    final float far[] = new float[n];
    for (int i = 0; i < n; i++) far[i] = (float) dar[i];
    return far;
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
      if (Double.isNaN(array[i - 1])) {
        if (Double.isNaN(array[i])) return i - 1;
      } else if (array[i - 1] == array[i]) {
        return i - 1;
      }
    }
    return -1;
  }

  /**
   * This tests if the values in the array are evenly spaced (ascending or descending) (via
   * Math2.almostEqual4, or easier test if first 6 digits are same).
   *
   * @return "" if the values in the array are evenly spaced; or an error message if not. If size is
   *     0 or 1, this returns "".
   */
  @Override
  public String isEvenlySpaced() {
    if (size <= 2) return "";
    // average is closer to exact than first diff
    // and usually detects not-evenly-spaced anywhere in the array on first test!
    final float average = (array[size - 1] - array[0]) / (size - 1);
    for (int i = 1; i < size; i++) {
      // This is a difficult test to do well. See tests below.
      if (Math2.almostEqual(4, array[i] - array[i - 1], average)) {
        // String2.log(i + " passed first test");
      } else if (
      // do easier test if first 6 digits are same
      Math2.almostEqual(6, array[i - 1] + average, array[i])
          && Math2.almostEqual(2, array[i] - array[i - 1], average)) {
        // String2.log(i + " passed second test " + (array[i] - array[i - 1]) + " " + diff);
      } else {
        return MessageFormat.format(
            ArrayNotEvenlySpaced,
            getClass().getSimpleName(),
            "" + (i - 1),
            "" + array[i - 1],
            "" + i,
            "" + array[i],
            "" + (array[i] - array[i - 1]),
            "" + average);
      }
    }

    return "";
  }

  /**
   * This tests if the values in the array are crudely evenly spaced (ascending or descending).
   *
   * @return "" if the values in the array are evenly spaced; or an error message if not. If size is
   *     0 or 1, this returns "".
   */
  public String isCrudelyEvenlySpaced() {
    if (size <= 2) return "";
    final float diff = array[1] - array[0];
    for (int i = 2; i < size; i++) {
      // This is a difficult test to do well. See tests below.
      // 1e7 avoids fEps test in almostEqual
      if (
      // do easier test if first 3 digits are same
      Math2.almostEqual(3, array[i - 1] + diff, array[i])
          && Math2.almostEqual(2, (array[i] - array[i - 1]) * 1e7, diff * 1e7)) {
        // String2.log(i + " passed second test " + (array[i] - array[i - 1]) + " " + diff);
      } else {
        return MessageFormat.format(
            ArrayNotEvenlySpaced,
            getClass().getSimpleName(),
            "" + (i - 1),
            "" + array[i - 1],
            "" + i,
            "" + array[i],
            "" + (array[i] - array[i - 1]),
            "" + diff);
      }
    }

    return "";
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
    float tmin = Float.MAX_VALUE;
    float tmax = -Float.MAX_VALUE;
    for (int i = 0; i < size; i++) {
      float v = array[i];
      if (Float.isFinite(v)) {
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
}
