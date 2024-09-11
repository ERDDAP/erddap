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
 * CharArray is a thin shell over a char[] with methods like ArrayList's methods; it extends
 * PrimitiveArray.
 *
 * <p>Unicode \\uffff (65535) means "not a character". This class always uses maxIsMV=true, so
 * Character.MAX_VALUE always represents a missing value (NaN).
 */
public class CharArray extends PrimitiveArray {

  /**
   * This is the main data structure. This should be private, but is public so you can manipulate it
   * if you promise to be careful. Note that if the PrimitiveArray's capacity is increased, the
   * PrimitiveArray will use a different array for storage.
   */
  public char[] array;

  /** A constructor for a capacity of 8 elements. The initial 'size' will be 0. */
  public CharArray() {
    array = new char[8];
    maxIsMV = true; // always true for CharArray, so users shouldn't ever need to test it
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
    return 2;
  }

  /**
   * This returns for cohort missing value for this class (e.g., Integer.MAX_VALUE), expressed as a
   * double. FloatArray and StringArray return Double.NaN.
   */
  @Override
  public final double missingValueAsDouble() {
    return Character.MAX_VALUE;
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
    return get(index) == Character.MAX_VALUE;
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

  /**
   * This constructs a CharArray by copying elements from the incoming PrimitiveArray (using
   * append()).
   *
   * @param primitiveArray a primitiveArray of any type
   */
  public CharArray(final PrimitiveArray primitiveArray) {
    Math2.ensureMemoryAvailable(2L * primitiveArray.size(), "CharArray");
    array = new char[primitiveArray.size()]; // exact size
    maxIsMV = true; // always true for CharArray, so users shouldn't ever need to test it
    append(primitiveArray);
  }

  /**
   * A constructor for a specified number of elements. The initial 'size' will be 0.
   *
   * @param capacity creates an CharArray with the specified initial capacity.
   * @param active if true, size will be set to capacity and all elements will equal 0; else size =
   *     0.
   */
  public CharArray(final int capacity, final boolean active) {
    Math2.ensureMemoryAvailable(2L * capacity, "CharArray");
    array = new char[capacity];
    maxIsMV = true; // always true for CharArray, so users shouldn't ever need to test it
    if (active) size = capacity;
  }

  /**
   * A constructor which (at least initially) uses the array and all its elements ('size' will equal
   * anArray.length).
   *
   * @param anArray the array to be used as this object's array.
   */
  public CharArray(final char[] anArray) {
    array = anArray;
    maxIsMV = true; // always true for CharArray, so users shouldn't ever need to test it
    size = anArray.length;
  }

  /**
   * This makes a CharArray from the comma-separated values. <br>
   * null becomes pa.length() == 0. <br>
   * "" becomes pa.length() == 0. <br>
   * " " becomes pa.length() == 1. <br>
   * See also PrimitiveArray.csvFactory(paType, csv);
   *
   * @param csv the comma-separated-value string
   * @return a CharArray from the comma-separated values.
   */
  public static CharArray fromCSV(final String csv) {
    return (CharArray) PrimitiveArray.csvFactory(PAType.CHAR, csv);
  }

  /**
   * A special method which encodes all the Unicode chars in this to ISO_8859_1.
   *
   * @return this for convenience
   */
  public CharArray toIso88591() {
    for (int i = 0; i < size; i++) array[i] = String2.toIso88591Char(array[i]);
    return this;
  }

  /**
   * A special constructor which encodes all short values as char values via <tt>ch[i] =
   * (char)sh[i]</tt>. Thus negative short values become large positive char values. Note that the
   * cohort 'missingValue' of a CharArray is different from the missingValue of a ShortArray. 'size'
   * will equal anArray.length.
   *
   * @param shortArray
   */
  public CharArray(final short[] shortArray) {
    size = shortArray.length;
    array = new char[size];
    maxIsMV = true; // always true for CharArray, so users shouldn't ever need to test it
    for (int i = 0; i < size; i++) array[i] = (char) shortArray[i];
  }

  /**
   * A special method which decodes all short values as char values via <tt>ch[i] =
   * (char)sa.array[i]</tt>. Thus negative short values become large positive char values. Note that
   * the cohort 'missingValue' of a CharArray is different from the missingValue of a ShortArray and
   * this method does nothing special for those values. This method does nothing special for the
   * missingValues. 'capacity' and 'size' will equal sa.size. See ShortArray.decodeFromCharArray().
   *
   * @param sa ShortArray
   */
  public static CharArray fromShortArrayBytes(final ShortArray sa) {
    final int size = sa.size();
    final CharArray ca = new CharArray(size, true); // active
    final char carray[] = ca.array;
    final short sarray[] = sa.array;
    for (int i = 0; i < size; i++) carray[i] = (char) sarray[i];
    return ca;
  }

  /**
   * This is an alternative way to convert a String to a char: by getting the first char (else
   * Character.MAX_VALUE). BEWARE: using this loses the distinction between "" becoming missing
   * value and "" becoming \\uffff!
   */
  public static final char firstChar(final String s) {
    return s == null || s.length() == 0 ? Character.MAX_VALUE : s.charAt(0);
  }

  /**
   * This returns a new PAOne with the minimum value that can be held by this class.
   *
   * @return a new PAOne with the minimum value that can be held by this class, e.g., -128b for
   *     ByteArray.
   */
  @Override
  public final PAOne MINEST_VALUE() {
    return new PAOne(PAType.CHAR).setString("\u0000");
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
    return new PAOne(PAType.CHAR).setString("\uFFFE");
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
   * This returns the hashcode for this charArray (dependent only on values, not capacity). WARNING:
   * the algorithm used may change in future versions.
   *
   * @return the hashcode for this charArray (dependent only on values, not capacity)
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
    if (stopIndex < startIndex) return pa == null ? new CharArray(new char[0]) : pa;

    int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
    CharArray ca = null;
    if (pa == null) {
      ca = new CharArray(willFind, true);
    } else {
      ca = (CharArray) pa;
      ca.ensureCapacity(willFind);
      ca.size = willFind;
    }
    final char tar[] = ca.array;
    if (stride == 1) {
      System.arraycopy(array, startIndex, tar, 0, willFind);
    } else {
      int po = 0;
      for (int i = startIndex; i <= stopIndex; i += stride) tar[po++] = array[i];
    }
    return ca;
  }

  /**
   * This returns the PAType (PAType.CHAR) of the element type.
   *
   * @return the PAType (PAType.CHAR) of the element type.
   */
  @Override
  public PAType elementType() {
    return PAType.CHAR;
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
    // if tPAType is smaller or same, return this.PAType
    if (tPAType == PAType.CHAR) return PAType.CHAR;

    // if sideways
    return PAType.STRING;
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array
   */
  public final void add(final char value) {
    if (size == array.length) // if we're at capacity
    ensureCapacity(size + 1L);
    array[size++] = value;
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array. This uses value.toString().charAt(0) (or
   *     Character.MAX_VALUE if trouble).
   */
  @Override
  public final void addObject(final Object value) {
    // double is good intermediate because it has the idea of NaN
    addDouble(
        value != null && value instanceof Number ? ((Number) value).doubleValue() : Double.NaN);
  }

  /**
   * This reads one value from the StrutureData and adds it to this PA.
   *
   * @param sd from an .nc file
   * @param memberName
   */
  @Override
  public void add(final StructureData sd, final String memberName) {
    add(sd.getScalarChar(memberName));
  }

  /**
   * This adds all the values from ar.
   *
   * @param ar an array
   */
  public final void add(final char ar[]) {
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
  public final void addN(final int n, final char value) {
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
  public void atInsert(final int index, final char value) {
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
    final int ti = String2.parseInt(value); // NaN -> Integer.MAX_VALUE
    atInsert(
        index,
        ti < Character.MIN_VALUE || ti > Character.MAX_VALUE ? Character.MAX_VALUE : (char) ti);
  }

  /**
   * This adds n PAOne's to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a PAOne (or null).
   */
  @Override
  public final void addNPAOnes(final int n, final PAOne value) {
    if (value == null) {
      addNInts(n, Integer.MAX_VALUE);
    } else {
      final String s = value.getString();
      addN(n, s.length() == 0 ? Character.MAX_VALUE : s.charAt(0));
    }
  }

  /**
   * This adds n Strings to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a String.
   */
  @Override
  public final void addNStrings(final int n, final String value) {
    addN(n, value == null || value.length() == 0 ? Character.MAX_VALUE : value.charAt(0));
  }

  /**
   * This adds n floats to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a float.
   */
  @Override
  public final void addNFloats(final int n, final float value) {
    addN(n, Math2.roundToChar(value));
  }

  /**
   * This adds n doubles to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a double.
   */
  @Override
  public final void addNDoubles(final int n, final double value) {
    addN(n, Math2.roundToChar(value));
  }

  /**
   * This adds n ints to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public final void addNInts(final int n, final int value) {
    addN(
        n,
        value < Character.MIN_VALUE || value > Character.MAX_VALUE
            ? Character.MAX_VALUE
            : (char) value);
  }

  /**
   * This adds n longs to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public final void addNLongs(final int n, final long value) {
    addN(
        n,
        value < Character.MIN_VALUE || value > Character.MAX_VALUE
            ? Character.MAX_VALUE
            : (char) value);
  }

  /**
   * This adds elements from another PrimitiveArray.
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
                + " in CharArray.addFromPA: otherIndex="
                + otherIndex
                + " + nValues="
                + nValues
                + " > otherPA.size="
                + otherPA.size);
      ensureCapacity(size + nValues);
      System.arraycopy(((CharArray) otherPA).array, otherIndex, array, size, nValues);
      size += nValues;

      // add from different type
    } else if (otherPA.elementType() == PAType.STRING) {
      for (int i = 0; i < nValues; i++)
        addString(otherPA.getString(otherIndex++)); // add and get do checking and handle maxIsMV

    } else {
      for (int i = 0; i < nValues; i++)
        addInt(otherPA.getInt(otherIndex++)); // add and get do checking and handles maxIsMV
    }
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
    if (otherPA.elementType() == PAType.STRING)
      setString(index, otherPA.getString(otherIndex)); // add and do checking of maxIsMV
    else setInt(index, otherPA.getInt(otherIndex)); // handles maxIsMV
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
          String2.ERROR + " in CharArray.removeRange: to (" + to + ") > size (" + size + ").");
    if (from >= to) {
      if (from == to) return;
      throw new IllegalArgumentException(
          String2.ERROR + " in CharArray.removeRange: from (" + from + ") > to (" + to + ").");
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
    final String errorIn = String2.ERROR + " in CharArray.move:\n";

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
    final char[] temp = new char[nToMove];
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
      Math2.ensureArraySizeOkay(minCapacity, "CharArray");
      // caller may know exact number needed, so don't double above 2x current size
      int newCapacity = (int) Math.min(Integer.MAX_VALUE - 1, array.length + (long) array.length);
      if (newCapacity < minCapacity) newCapacity = (int) minCapacity; // safe since checked above
      Math2.ensureMemoryAvailable(2L * newCapacity, "CharArray");
      final char[] newArray = new char[newCapacity];
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
  public char[] toArray() {
    if (array.length == size) return array;
    Math2.ensureMemoryAvailable(2L * size, "CharArray.toArray");
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
   * This returns a double[] (perhaps 'array') which has 'size' elements.
   *
   * @return a double[] (perhaps 'array') which has 'size' elements. Character.MAX_VALUE is
   *     converted to Double.NaN.
   */
  @Override
  public double[] toDoubleArray() {
    Math2.ensureMemoryAvailable(8L * size, "CharArray.toDoubleArray");
    final double dar[] = new double[size];
    for (int i = 0; i < size; i++) {
      char c = array[i];
      dar[i] = c == Character.MAX_VALUE ? Double.NaN : c;
    }
    return dar;
  }

  /**
   * This returns a String[] which has 'size' elements.
   *
   * @return a String[] which has 'size' elements. This treats chars as lenth=1 strings.
   *     Character.MAX_VALUE appears as "".
   */
  @Override
  public String[] toStringArray() {
    Math2.ensureMemoryAvailable(6L * size, "CharArray.toStringArray");
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
  public char get(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in CharArray.get: index (" + index + ") >= size (" + size + ").");
    return array[index];
  }

  /**
   * This sets a specified element.
   *
   * @param index 0 ... size-1
   * @param value the value for that element
   */
  public void set(final int index, final char value) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in CharArray.set: index (" + index + ") >= size (" + size + ").");
    array[index] = value;
  }

  /**
   * Return a value from the array as an int.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int. Character.MAX_VALUE is returned as Integer.MAX_VALUE.
   */
  @Override
  public int getInt(final int index) {
    final int i = get(index);
    return i == Character.MAX_VALUE ? Integer.MAX_VALUE : i;
  }

  /**
   * Return a value from the array as an int. This "raw" variant leaves missingValue from smaller
   * data types (e.g., ByteArray missingValue=127) AS IS (even if maxIsMV=true). Floating point
   * values are rounded.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int. String values are parsed with String2.parseInt and so may return
   *     Integer.MAX_VALUE.
   */
  @Override
  public int getRawInt(final int index) {
    return get(index);
  }

  /**
   * Set a value in the array as an int.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToChar(i).
   */
  @Override
  public void setInt(final int index, final int i) {
    set(index, i < Character.MIN_VALUE || i > Character.MAX_VALUE ? Character.MAX_VALUE : (char) i);
  }

  /**
   * Return a value from the array as a long.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a long. Character.MAX_VALUE is returned as Long.MAX_VALUE.
   */
  @Override
  public long getLong(final int index) {
    final int i = get(index);
    return i == Character.MAX_VALUE ? Long.MAX_VALUE : i;
  }

  /**
   * Set a value in the array as a long.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToChar(long).
   */
  @Override
  public void setLong(final int index, final long i) {
    set(index, i < Character.MIN_VALUE || i > Character.MAX_VALUE ? Character.MAX_VALUE : (char) i);
  }

  /**
   * Return a value from the array as a ulong.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a ulong. If maxIsMV, MAX_VALUE is returned as null.
   */
  @Override
  public BigInteger getULong(final int index) {
    final char b = get(index);
    return b == Character.MAX_VALUE ? null : new BigInteger("" + (int) b);
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
   *     return Float.NaN. Character.MAX_VALUE is returned as Float.NaN.
   */
  @Override
  public float getFloat(final int index) {
    final char c = get(index);
    return c == Character.MAX_VALUE ? Float.NaN : c;
  }

  /**
   * Set a value in the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. For numeric PrimitiveArray, it is narrowed if needed by methods like
   *     Math2.roundToChar(d).
   */
  @Override
  public void setFloat(final int index, final float d) {
    set(index, Math2.roundToChar(d));
  }

  /**
   * Return a value from the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN. Character.MAX_VALUE is returned as Double.NaN.
   */
  @Override
  public double getDouble(final int index) {
    final char c = get(index);
    return c == Character.MAX_VALUE ? Double.NaN : c;
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
    return get(index);
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
   *     Math2.roundToChar(d).
   */
  @Override
  public void setDouble(final int index, final double d) {
    set(index, Math2.roundToChar(d));
  }

  /**
   * Return a value from the array as a String (where the cohort missing value appears as "", not a
   * value).
   *
   * @param index the index number 0 ..
   * @return This returns (int)(ar[index]), or "" for NaN or infinity. If this PA is unsigned, this
   *     method returns the unsigned value.
   */
  @Override
  public String getString(final int index) {
    final char ch = get(index);
    // String2.log(">> CharArray.getString index=" + index + " ch=" + ch);
    return ch == Character.MAX_VALUE ? "" : "" + ch;
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
    final char ch = get(index);
    return ch == Character.MAX_VALUE ? "null" : String2.toJson("" + ch);
  }

  /**
   * Return a value from the array as a String suitable for the data section of an NCCSV file, e.g.,
   * z \t \u0000 , \", but perhaps (e.g., for chars in ",\" ") surrounded by "'[char]'".
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity. CharArray
   *     and StringArray overwrite this.
   */
  @Override
  public String getNccsvDataString(final int index) {
    final char ch = get(index);
    return ch == '\uFFFF' ? "" : String2.toNccsvDataString("" + ch);
  }

  /**
   * This is like getNccsvDataString, but encodes chars &gt;=127.
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity. CharArray
   *     and StringArray overwrite this.
   */
  @Override
  public String getNccsv127DataString(final int index) {
    final char ch = get(index);
    return ch == '\uFFFF' ? "" : String2.toNccsv127DataString("" + ch);
  }

  /**
   * Return a value from the array as a String suitable for the data section of an ASCII csv or tsv
   * string, e.g., z "\t" "\u0000" , "\"".
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity. CharArray
   *     and StringArray overwrite this.
   */
  @Override
  public String getSVString(final int index) {
    final char ch = get(index);
    if (ch == '\uFFFF') return "";
    return String2.toSVString("" + ch, 127);
  }

  /**
   * Return a value from the array as a String suitable for the data section of a UTF-8 tsv file,
   * e.g., z \t \u0000 , \".
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity. CharArray
   *     and StringArray overwrite this.
   */
  @Override
  public String getUtf8TsvString(final int index) {
    final char ch = get(index);
    if (ch == '\uFFFF') return "";
    final String s = String2.toJson65536("" + ch);
    return s.substring(1, s.length() - 1); // remove enclosing quotes
  }

  /**
   * This returns a JSON-style comma-separated-value list of the elements. CharArray and StringArray
   * overwrite this.
   *
   * @return a csv string of the elements.
   */
  @Override
  public String toJsonCsvString() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(", ");
      final char ch = get(i); // write each as a separate json string
      sb.append(ch == '\uFFFF' ? "null" : String2.toJson("" + ch));
    }
    return sb.toString();
  }

  /**
   * Return a value from the array as a String. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS, regardless of maxIsMV. FloatArray and
   * DoubleArray return "" if the stored value is NaN.
   *
   * <p>All integerTypes overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  @Override
  public String getRawString(final int index) {
    return "" + get(index);
  }

  /**
   * Set a value in the array from a String.
   *
   * @param index the index number 0 ..
   * @param s the value. For numeric PrimitiveArray's, it is parsed with String2.parseInt and
   *     narrowed by Math2.narrowToChar(i).
   */
  @Override
  public void setString(final int index, final String s) {
    set(index, s == null || s.length() == 0 ? Character.MAX_VALUE : s.charAt(0));
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final char lookFor) {
    return indexOf(lookFor, 0);
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final char lookFor, final int startIndex) {
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
    return indexOf(firstChar(lookFor), startIndex);
  }

  /**
   * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int lastIndexOf(final char lookFor, final int startIndex) {
    if (startIndex >= size)
      throw new IllegalArgumentException(
          String2.ERROR
              + " in CharArray.get: startIndex ("
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
    return lastIndexOf(firstChar(lookFor), startIndex);
  }

  /** If size != capacity, this makes a new 'array' of size 'size' so capacity will equal size. */
  @Override
  public void trimToSize() {
    array = toArray();
  }

  /**
   * Test if o is an CharArray with the same size and values.
   *
   * @param o the object that will be compared to this CharArray
   * @return true if equal. o=null returns false.
   */
  @Override
  public boolean equals(final Object o) {
    return testEquals(o).length() == 0;
  }

  /**
   * Test if o is an CharArray with the same size and values, but returns a String describing the
   * difference (or "" if equal).
   *
   * @param o
   * @return a String describing the difference (or "" if equal). o=null doesn't throw an exception.
   */
  @Override
  public String testEquals(final Object o) {
    if (!(o instanceof CharArray))
      return "The two objects aren't equal: this object is a CharArray; the other is a "
          + (o == null ? "null" : o.getClass().getName())
          + ".";
    final CharArray other = (CharArray) o;
    if (other.size() != size)
      return "The two CharArrays aren't equal: one has "
          + size
          + " value(s); the other has "
          + other.size()
          + " value(s).";
    for (int i = 0; i < size; i++)
      if (getInt(i) != other.getInt(i)) // handles mv
      return "The two CharArrays aren't equal: this["
            + i
            + "]="
            + getNccsv127DataString(i)
            + // safe char to int type conversion
            "; other["
            + i
            + "]="
            + other.getNccsv127DataString(i)
            + "."; // safe char to int type conversion
    return "";
  }

  /**
   * This converts the elements into a Comma-Space-Separated-Value (CSSV) String. This is just used
   * for diagnostic messages (e.g., to a DOS window).
   *
   * @return a Comma-Space-Separated-Value (CSSV) String representation.
   */
  @Override
  public String toString() {
    return String2.toCSSVString(toArray()); // toArray() gets just 'size' elements
  }

  /**
   * This converts the elements into an NCCSV attribute String, e.g.,: -128b, 127b
   *
   * @return an NCCSV attribute String
   */
  @Override
  public String toNccsvAttString() {
    final StringBuilder sb = new StringBuilder(size * 7);
    for (int i = 0; i < size; i++)
      sb.append((i == 0 ? "" : ",") + "\"'" + String2.toNccsvChar(array[i]) + "'\"");
    // String2.log(">> CharArray " + toString() + "  >>  " + sb.toString());
    return sb.toString();
  }

  /**
   * This is like toNccsvAttString, but chars &gt;127 are \\uhhhh encoded.
   *
   * @return an NCCSV attribute String
   */
  @Override
  public String toNccsv127AttString() {
    final StringBuilder sb = new StringBuilder(size * 7);
    for (int i = 0; i < size; i++)
      sb.append((i == 0 ? "" : ",") + "\"'" + String2.toNccsv127Char(array[i]) + "'\"");
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
    // String2.log(">> compare a=" + String2.annotatedString(getString(index1)) + " b=" +
    // String2.annotatedString(otherPA.getString(index2)));
    return getString(index1).compareTo(otherPA.getString(index2)); // String handles maxIsMV
  }

  /**
   * This is like compare(), except for CharArray and StringArray it is caseInsensitive.
   *
   * @param index1 an index number 0 ... size-1
   * @param otherPA the other PrimitiveArray which must be the same (or close) PAType.
   * @param index2 an index number 0 ... size-1
   * @return a negative integer, zero, or a positive integer if the value at index1 is less than,
   *     equal to, or greater than the value at index2.
   */
  @Override
  public int compareIgnoreCase(final int index1, final PrimitiveArray otherPA, final int index2) {
    return String2.STRING_COMPARATOR_IGNORE_CASE.compare(
        getString(index1), otherPA.getString(index2));
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
    Math2.ensureMemoryAvailable(2L * array.length, "CharArray");
    final char newArray[] = new char[array.length];
    for (int i = 0; i < n; i++) newArray[i] = array[rank[i]];
    array = newArray;
  }

  /**
   * This reverses the order of the bytes in each value, e.g., if the data was read from a
   * little-endian source.
   */
  @Override
  public void reverseBytes() {
    for (int i = 0; i < size; i++) array[i] = Character.reverseBytes(array[i]);
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
  public int writeDos(DataOutputStream dos) throws Exception {
    for (int i = 0; i < size; i++) dos.writeChar(array[i]);
    return size == 0 ? 0 : 2;
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
    dos.writeChar(array[i]);
    return 2;
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
    for (int i = 0; i < n; i++) array[size++] = dis.readChar();
  }

  /**
   * This writes one String to a DataOutputStream in the format DODS wants (see www.opendap.org DAP
   * 2.0 standard, section 7.3.2.1). See also the XDR standard
   * (http://tools.ietf.org/html/rfc4506#section-4.11). Just 8 bits are stored: there is no utf or
   * other unicode support. See DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for
   * compatible common 8bit. Ah: dods.dap.DString reader assumes ISO-8859-1, which is first page of
   * unicode (is it?!).
   *
   * @param dos
   * @param c
   * @throws Exception if trouble
   */
  public static void externalizeForDODS(final DataOutputStream dos, final char c) throws Exception {
    dos.writeInt(1); // for Strings, just write size once
    dos.writeByte(
        c < 256
            ? c
            : '?'); // dods.dap.DString reader assumes ISO-8859-1, which is first page of unicode

    // pad to 4 bytes boundary at end
    for (int i = 0; i < 3; i++) dos.writeByte(0);
  }

  /**
   * This writes all the data to a DataOutputStream in the DODS Array format (see www.opendap.org
   * DAP 2.0 standard, section 7.3.2.1). See also the XDR standard
   * (http://tools.ietf.org/html/rfc4506#section-4.11).
   *
   * @param dos
   * @throws Exception if trouble
   */
  @Override
  public void externalizeForDODS(final DataOutputStream dos) throws Exception {
    dos.writeInt(size);
    dos.writeInt(size); // yes, a second time
    for (int i = 0; i < size; i++) externalizeForDODS(dos, array[i]);
  }

  /**
   * This writes one element to a DataOutputStream in the DODS Atomic-type format (see
   * www.opendap.org DAP 2.0 standard, section 7.3.2). See also the XDR standard
   * (http://tools.ietf.org/html/rfc4506#section-4.11).
   *
   * @param dos
   * @param i the index of the element to be written
   * @throws Exception if trouble
   */
  @Override
  public void externalizeForDODS(final DataOutputStream dos, final int i) throws Exception {
    externalizeForDODS(dos, array[i]);
  }

  /**
   * This reads/appends String values from a StringArray from a DODS DataInputStream, and is thus
   * the complement of externalizeForDODS.
   *
   * @param dis
   * @throws IOException if trouble
   */
  @Override
  public void internalizeFromDODS(final DataInputStream dis) throws java.io.IOException {
    final int nStrings = dis.readInt();
    ensureCapacity(size + (long) nStrings);
    dis.readInt(); // skip duplicate of nStrings
    final byte buffer[] = new byte[80];
    for (int i = 0; i < nStrings; i++) {
      int nChar = dis.readInt(); // always 1
      dis.readFully(buffer, 0, nChar);
      add((char) buffer[0]);

      // pad to 4 bytes boundary at end
      while (nChar++ % 4 != 0) dis.readByte();
    }
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
    raf.writeChar(get(index));
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
    add(raf.readChar());
  }

  /**
   * This appends the data in another pa to the current data. WARNING: information may be lost from
   * the incoming pa if this primitiveArray is of a smaller type; see needPAType().
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     Math2.narrowToChar.
   */
  @Override
  public void append(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof CharArray ca) {
      System.arraycopy(ca.array, 0, array, size, otherSize);
      size += otherSize;
    } else if (pa.elementType() == PAType.STRING) {
      for (int i = 0; i < otherSize; i++) addString(pa.getString(i));
    } else {
      for (int i = 0; i < otherSize; i++)
        addInt(pa.getInt(i)); // this converts mv's and handles maxIsMV
    }
  }

  /**
   * This appends the data in another pa to the current data. This "raw" variant leaves missingValue
   * from smaller data types (e.g., ByteArray missingValue=127) AS IS. WARNING: information may be
   * lost from the incoming pa if this primitiveArray is of a simpler type.
   *
   * @param pa if pa is a bigger data type, the data will be narrowed with Math2.narrowToChar. If pa
   *     is numeric, the values are treated as character numbers.
   */
  @Override
  public void rawAppend(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof CharArray ca) {
      System.arraycopy(ca.array, 0, array, size, otherSize);
      size += otherSize; // do last to minimize concurrency problems
    } else if (pa.elementType() == PAType.STRING) {
      for (int i = 0; i < otherSize; i++) addString(pa.getString(i)); // this DOES convert mv's
    } else {
      for (int i = 0; i < otherSize; i++)
        array[size + i] = Math2.narrowToChar(pa.getRawInt(i)); // this DOESN'T convert mv's
      size += otherSize; // do last to minimize concurrency problems
    }
  }

  /**
   * This populates 'indices' with the indices (ranks) of the values in this CharArray (ties get the
   * same index). For example, b,b,c,a returns 1,1,2,0.
   *
   * @param indices the intArray that will capture the indices of the values (ties get the same
   *     index). For example, b,b,c,a returns 1,1,2,0.
   * @return a PrimitveArray (the same type as this class) with the unique values, sorted. If all
   *     the values are unique and already sorted, this returns 'this'.
   */
  @Override
  public PrimitiveArray makeIndices(final IntArray indices) {
    indices.clear();
    if (size == 0) {
      return new CharArray();
    }

    // make a hashMap with all the unique values (associated values are initially all dummy)
    final Integer dummy = Integer.valueOf(-1);
    final HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
    char lastValue = array[0]; // since lastValue often equals currentValue, cache it
    hashMap.put(Character.valueOf(lastValue), dummy);
    boolean alreadySorted = true;
    for (int i = 1; i < size; i++) {
      char currentValue = array[i];
      if (currentValue != lastValue) {
        if (currentValue < lastValue) alreadySorted = false;
        lastValue = currentValue;
        hashMap.put(Character.valueOf(lastValue), dummy);
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
          "CharArray.makeRankArray nUnique(" + nUnique + ") != count(" + count + ")!");

    // sort them
    Arrays.sort(unique);

    // put the unique values back in the hashMap with the ranks as the associated values
    // and make tUnique
    final char tUnique[] = new char[nUnique];
    for (int i = 0; i < count; i++) {
      hashMap.put(unique[i], Integer.valueOf(i));
      tUnique[i] = ((Character) unique[i]).charValue();
    }

    // convert original values to ranks
    final int ranks[] = new int[size];
    lastValue = array[0];
    ranks[0] = ((Integer) hashMap.get(Character.valueOf(lastValue))).intValue();
    int lastRank = ranks[0];
    for (int i = 1; i < size; i++) {
      if (array[i] == lastValue) {
        ranks[i] = lastRank;
      } else {
        lastValue = array[i];
        ranks[i] = ((Integer) hashMap.get(Character.valueOf(lastValue))).intValue();
        lastRank = ranks[i];
      }
    }

    // store the results in ranked
    indices.append(new IntArray(ranks));

    return new CharArray(tUnique);
  }

  /**
   * This changes all instances of the first value to the second value.
   *
   * @param tFrom the original value (use "" for standard missingValue)
   * @param tTo the new value (use "" for standard missingValue)
   * @return the number of values switched
   */
  @Override
  public int switchFromTo(final String tFrom, final String tTo) {
    final char from = firstChar(tFrom);
    // final boolean toIsMV = tTo.length() == 0;
    final char to = firstChar(tTo);
    if (from == to) return 0;
    int count = 0;
    for (int i = 0; i < size; i++) {
      if (array[i] == from) {
        array[i] = to;
        count++;
      }
    }
    return count;
  }

  /**
   * This tests if the values in the array are sorted in ascending order (tied is ok). The details
   * of this test are geared toward determining if the values are suitable for binarySearch.
   *
   * @return "" if the values in the array are sorted in ascending order (or tied); or an error
   *     message if not (i.e., if descending or unordered). If size is 0 or 1 (non-missing value),
   *     this returns "". A missing value returns an error message.
   */
  @Override
  public String isAscending() {
    if (size == 0) return "";
    for (int i = 1; i < size; i++) {
      if (array[i - 1] > array[i]) {
        return MessageFormat.format(
            ArrayNotAscending,
            getClass().getSimpleName(),
            "[" + (i - 1) + "]=#" + (int) array[i - 1] + " > [" + i + "]=#" + (int) array[i]);
        // safe char to int type conversion
      }
    }
    if (isMissingValue(size - 1))
      return MessageFormat.format(
          ArrayNotAscending,
          getClass().getSimpleName(),
          "[" + (size - 1) + "]=(" + ArrayMissingValue + ")");
    return "";
  }

  /**
   * This tests if the values in the array are sorted in descending order (tied is ok).
   *
   * @return "" if the values in the array are sorted in descending order (or tied); or an error
   *     message if not (i.e., if ascending or unordered). If size is 0 or 1 (non-missing value),
   *     this returns "". A missing value returns an error message.
   */
  @Override
  public String isDescending() {
    if (size == 0) return "";
    if (isMissingValue(0))
      return MessageFormat.format(
          ArrayNotDescending, getClass().getSimpleName(), "[0]=(" + ArrayMissingValue + ")");
    for (int i = 1; i < size; i++) {
      if (array[i - 1] < array[i]) {
        return MessageFormat.format(
            ArrayNotDescending,
            getClass().getSimpleName(),
            "["
                + (i - 1)
                + "]=#"
                + (int) array[i - 1]
                + " < ["
                + i
                + "]=#"
                + (int) array[i]); // safe char to int type conversion
      }
    }
    return "";
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
    int tmin = Character.MAX_VALUE;
    int tmax = 0;
    for (int i = 0; i < size; i++) {
      int v = array[i];
      if (v == Character.MAX_VALUE) {
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
}
