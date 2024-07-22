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
import java.io.InputStream;
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
 * UByteArray is a thin shell over a byte[] with methods like ArrayList's methods; it extends
 * PrimitiveArray.
 *
 * <p>This class uses maxIsMV=true and Math2.UBYTE_MAX_VALUE (255) to represent a missing value
 * (NaN).
 */
public class UByteArray extends PrimitiveArray {

  /** This is the minimum unsigned value, stored as a signed short. */
  public static final short MIN_VALUE = Math2.UBYTE_MIN_VALUE;

  /**
   * This is the maximum unsigned value (the cohort missing value), stored/packed as a signed byte.
   */
  public static final byte PACKED_MAX_VALUE = (byte) -1;

  /** This is the maximum unsigned value (the cohort missing value), stored as a signed short. */
  public static final short MAX_VALUE = Math2.UBYTE_MAX_VALUE;

  /**
   * This indicates if this class' type (e.g., PAType.SHORT) is an unsigned integer type. The
   * unsigned integer type classes overwrite this.
   */
  @Override
  public boolean isUnsigned() {
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
  public int elementSize() {
    return 1;
  }

  /**
   * This returns for cohort missing value for this class (e.g., Integer.MAX_VALUE), expressed as a
   * double. FloatArray and StringArray return Double.NaN.
   */
  @Override
  public double missingValueAsDouble() {
    return MAX_VALUE;
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
  public boolean isMaxValue(final int index) {
    return getPacked(index) == PACKED_MAX_VALUE;
  }

  /**
   * This tests if the value at the specified index is a missing value. For integerTypes,
   * isMissingValue can only be true if maxIsMv is 'true'.
   *
   * @param index The index in question
   * @return true if the value is a missing value.
   */
  @Override
  public boolean isMissingValue(final int index) {
    return maxIsMV && isMaxValue(index);
  }

  /** This packs a signed int holding an unsigned byte as a byte. */
  public static byte pack(final int i) {
    return i < 0 || i >= MAX_VALUE ? PACKED_MAX_VALUE : (byte) (i & MAX_VALUE);
  }

  /** This packs a signed long holding an unsigned byte as a byte. */
  public static byte pack(final long i) {
    return (byte) (i & 0xff);
  }

  /** This unpacks a ubyte stored in a byte as a short. */
  public static short unpack(final byte b) {
    return b >= 0 ? b : (short) (b + MAX_VALUE + 1);
  }

  /**
   * This is the main data structure. This should be private, but is public so you can manipulate it
   * if you promise to be careful. Note that if the PrimitiveArray's capacity is increased, the
   * PrimitiveArray will use a different array for storage.
   */
  public byte[] array;

  /**
   * This indicates if this class' type (e.g., PAType.SHORT) is an integer (in the math sense) type.
   * The integer type classes overwrite this.
   */
  @Override
  public boolean isIntegerType() {
    return true;
  }

  /** A constructor for a capacity of 8 elements. The initial 'size' will be 0. */
  public UByteArray() {
    array = new byte[8];
  }

  /**
   * This constructs a UByteArray by copying elements from the incoming PrimitiveArray (using
   * append()).
   *
   * @param primitiveArray a primitiveArray of any type
   */
  public UByteArray(final PrimitiveArray primitiveArray) {
    Math2.ensureMemoryAvailable(1L * primitiveArray.size(), "UByteArray");
    array = new byte[primitiveArray.size()]; // exact size
    append(primitiveArray);
  }

  /**
   * A constructor for a specified capacity. The initial 'size' will be 0.
   *
   * @param capacity creates an UByteArray with the specified initial capacity.
   * @param active if true, size will be set to capacity and all elements will equal 0; else size =
   *     0.
   */
  public UByteArray(final int capacity, final boolean active) {
    Math2.ensureMemoryAvailable(1L * capacity, "UByteArray");
    array = new byte[capacity];
    if (active) size = capacity;
  }

  /**
   * A constructor which (at least initially) uses the array and all its elements ('size' will equal
   * anArray.length).
   *
   * @param anArray the array with already packed values.
   */
  public UByteArray(final byte[] anArray) {
    array = anArray;
    size = anArray.length;
  }

  /**
   * A constructor which does NOT use the array and all its elements ('size' will equal
   * anArray.length).
   *
   * @param anArray the array with not-yet-packed values.
   */
  public UByteArray(final short[] anArray) {
    size = anArray.length;
    Math2.ensureMemoryAvailable(1L * size, "UByteArray");
    array = new byte[size];
    for (int i = 0; i < size; i++) array[i] = pack(anArray[i]);
  }

  /**
   * A special constructor for UByteArray: first, last, with an increment of 1.
   *
   * @param first the value of the first element.
   * @param last the value of the last element (inclusive!).
   */
  public UByteArray(final int first, final int last) {
    size = last - first + 1;
    Math2.ensureMemoryAvailable(1L * size, "UByteArray");
    array = new byte[size];
    for (int i = 0; i < size; i++) array[i] = pack(first + i);
  }

  /**
   * A constructor which reads/adds the byte contents of a file EXACTLY.
   *
   * @param fileName is the (usually canonical) path (dir+name) for the file
   * @throws Exception if trouble
   */
  public static UByteArray fromFile(final String fileName) throws Exception {
    UByteArray uba = new UByteArray();
    final InputStream stream = File2.getDecompressedBufferedInputStream(fileName);
    try {
      int available = stream.available();
      while (available > 0) {
        uba.ensureCapacity(uba.size + (long) available);
        uba.size += stream.read(uba.array, uba.size, available);
        available = stream.available();
      }
    } finally {
      stream.close();
    }
    return uba;
  }

  /**
   * This constructs a UByteArray from the values of another PrimitiveArray by considering the
   * incoming pa as boolean which needs to be converted to bytes.
   *
   * <ul>
   *   <li>StringArray uses String2.parseBooleanToInteger.
   *   <li>CharArray uses StandardMissingValue-&gt;StandardMissingValue, [0fF]-&gt;false,
   *       others-&gt;true.
   *   <li>numeric uses StandardMissingValue-&gt;StandardMissingValue, 0-&gt;false, others-&gt;true.
   * </ul>
   *
   * @param pa the values of pa are interpreted as boolean, which are then converted to bytes.
   * @return a UByteArray
   */
  public static UByteArray toBooleanToUByte(final PrimitiveArray pa) {
    final int size = pa.size();
    final boolean paMaxIsMV = pa.getMaxIsMV();
    final UByteArray ba = new UByteArray(size, true); // active
    final byte bar[] = ba.array;
    final byte zero = 0;
    final byte one = 1;
    if (pa.elementType() == PAType.STRING) {
      for (int i = 0; i < size; i++) {
        final int ti =
            String2.parseBooleanToInt(pa.getString(i)); // returns 0, 1, or Integer.MAX_VALUE
        if (ti == Integer.MAX_VALUE) {
          ba.setMaxIsMV(true);
          bar[i] = Byte.MAX_VALUE;
        } else {
          bar[i] = (byte) ti;
        }
      }
    } else if (pa instanceof CharArray ca) {
      for (int i = 0; i < size; i++) {
        final char c = ca.get(i);
        if (paMaxIsMV && c == Character.MAX_VALUE) {
          ba.setMaxIsMV(true);
          bar[i] = PACKED_MAX_VALUE;
        } else {
          bar[i] = "0fF".indexOf(ca.get(i)) >= 0 ? zero : one;
        }
      }
    } else if (pa instanceof LongArray la) {
      for (int i = 0; i < size; i++) {
        final long tl = la.get(i);
        if (paMaxIsMV && tl == Long.MAX_VALUE) {
          ba.setMaxIsMV(true);
          bar[i] = PACKED_MAX_VALUE;
        } else {
          bar[i] = tl == 0 ? zero : one;
        }
      }
    } else if (pa instanceof ULongArray ua) {
      for (int i = 0; i < size; i++) {
        final BigInteger tul = ua.get(i);
        if (paMaxIsMV && tul.equals(ULongArray.MAX_VALUE)) {
          ba.setMaxIsMV(true);
          bar[i] = PACKED_MAX_VALUE;
        } else {
          bar[i] = tul.equals(BigInteger.ZERO) ? zero : one;
        }
      }
    } else { // byte, ubyte, short, ushort, int, uint, float, double
      for (int i = 0; i < size; i++) {
        final double td = pa.getDouble(i);
        if (Double.isNaN(td)) {
          ba.setMaxIsMV(true);
          bar[i] = PACKED_MAX_VALUE;
        } else {
          bar[i] = td == 0 ? zero : one;
        }
      }
    }
    return ba;
  }

  /**
   * If this is an unsigned integer type, this makes a signed variant (e.g., PAType.UBYTE returns a
   * PAType.BYTE). The values from pa are then treated as unsigned, e.g., 255 in UByteArray becomes
   * -1 in a ByteArray.
   *
   * @return a new unsigned PrimitiveArray, or this pa.
   */
  @Override
  public PrimitiveArray makeSignedPA() {
    Math2.ensureMemoryAvailable(1L * size, "UByteArray");
    final byte ar[] = new byte[size];
    System.arraycopy(array, 0, ar, 0, size);
    return new ByteArray(ar);
  }

  /**
   * This makes a ByteArray from the comma-separated values. <br>
   * null becomes pa.length() == 0. <br>
   * "" becomes pa.length() == 0. <br>
   * " " becomes pa.length() == 1. <br>
   * See also PrimitiveArray.csvFactory(paType, csv);
   *
   * @param csv the comma-separated-value string
   * @return a UByteArray from the comma-separated values.
   */
  public static UByteArray fromCSV(final String csv) {
    return (UByteArray) PrimitiveArray.csvFactory(PAType.UBYTE, csv);
  }

  /**
   * This returns a new PAOne with the minimum value that can be held by this class.
   *
   * @return a new PAOne with the minimum value that can be held by this class, e.g., -128b for
   *     ByteArray.
   */
  @Override
  public PAOne MINEST_VALUE() {
    return new PAOne(PAType.UBYTE).setInt(MIN_VALUE);
  }

  /**
   * This returns a new PAOne with the maximum value that can be held by this class (not including
   * the cohort missing value).
   *
   * @return a new PAOne with the maximum value that can be held by this class, e.g., 126 for
   *     ByteArray.
   */
  @Override
  public PAOne MAXEST_VALUE() {
    return new PAOne(PAType.UBYTE).setInt(MAX_VALUE - 1);
  }

  /**
   * This returns the current capacity (number of elements) of the internal data array.
   *
   * @return the current capacity (number of elements) of the internal data array.
   */
  @Override
  public int capacity() {
    return array.length;
  }

  /**
   * This returns the hashcode for this UByteArray (dependent only on values, not capacity).
   * WARNING: the algorithm used may change in future versions.
   *
   * @return the hashcode for this UByteArray (dependent only on values, not capacity)
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
          ? new UByteArray(new byte[0])
          : pa; // no need to call .setMaxIsMV(maxIsMV) since size=0

    final int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
    UByteArray ba = null;
    if (pa == null) {
      ba = new UByteArray(willFind, true);
    } else {
      ba = (UByteArray) pa;
      ba.ensureCapacity(willFind);
      ba.size = willFind;
    }
    final byte tar[] = ba.array;
    if (stride == 1) {
      System.arraycopy(array, startIndex, tar, 0, willFind);
    } else {
      int po = 0;
      for (int i = startIndex; i <= stopIndex; i += stride) tar[po++] = array[i];
    }
    return ba.setMaxIsMV(maxIsMV);
  }

  /**
   * This returns the PAType (PAType.BYTE) of the element type.
   *
   * @return the PAType (PAType.BYTE) of the element type.
   */
  @Override
  public PAType elementType() {
    return PAType.UBYTE;
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
      case UBYTE -> PAType.UBYTE;

        // if sideways
      case CHAR -> PAType.STRING;
      case BYTE -> PAType.SHORT;

        // if tPAType is bigger. SHORT, USHORT, INT, UINT, LONG, ULONG, FLOAT, DOUBLE, STRING
      default -> tPAType;
    };
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array
   */
  public void add(final short value) {
    if (size == array.length) // if we're at capacity
    ensureCapacity(size + 1L);
    if (value < 0) maxIsMV = true;
    array[size++] = pack(value);
  }

  /**
   * This adds an already packed value to the array (increasing 'size' by 1).
   *
   * @param packedValue the already packed value to be added to the array
   */
  public void addPacked(final byte packedValue) {
    if (size == array.length) // if we're at capacity
    ensureCapacity(size + 1L);
    array[size++] = packedValue;
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array. If value instanceof Number, this uses
   *     Number.longValue(). (If you want a more sophisticated conversion, save to DoubleArray, then
   *     convert DoubleArray to UByteArray.) If null or not a Number, this adds MAX_VALUE.
   */
  @Override
  public void addObject(final Object value) {
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
    addPacked(sd.getScalarByte(memberName));
  }

  /**
   * This adds all the values from ar.
   *
   * @param ar an array
   */
  public void add(final byte ar[]) {
    add(ar, 0, ar.length);
  }

  /**
   * This adds some of the values from ar.
   *
   * @param ar an array
   * @param offset the first value to be added
   * @param nBytes the number of values to be added
   */
  public void add(final byte ar[], final int offset, final int nBytes) {
    ensureCapacity(size + (long) nBytes);
    System.arraycopy(ar, offset, array, size, nBytes);
    size += nBytes;
  }

  /**
   * This adds n copies of value to the array (increasing 'size' by n).
   *
   * @param n if less than 0, this throws Exception.
   * @param value the value to be added to the array. n &lt; 0 throws an Exception.
   */
  public void addN(final int n, final short value) {
    if (n == 0) return;
    if (n < 0)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayAddN, getClass().getSimpleName(), "" + n));
    ensureCapacity(size + (long) n);
    Arrays.fill(array, size, size + n, pack(value));
    size += n;
  }

  /**
   * This inserts an item into the array at the specified index, pushing subsequent items to
   * oldIndex+1 and increasing 'size' by 1.
   *
   * @param index the position where the value should be inserted.
   * @param value the value to be inserted into the array
   */
  public void atInsert(final int index, final short value) {
    if (index < 0 || index > size)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayAtInsert, getClass().getSimpleName(), "" + index, "" + size));
    if (size == array.length) // if we're at capacity
    ensureCapacity(size + 1L);
    System.arraycopy(array, index, array, index + 1, size - index);
    size++;
    array[index] = pack(value);
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
    if (ti < MIN_VALUE || ti > MAX_VALUE) {
      maxIsMV = true;
      atInsert(index, MAX_VALUE);
    } else {
      atInsert(index, (short) ti);
    }
  }

  /**
   * This adds n PAOne's to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a PAOne (or null).
   */
  @Override
  public void addNPAOnes(final int n, final PAOne value) {
    addNInts(n, value == null ? Integer.MAX_VALUE : value.getInt()); // handles NaN and MV
  }

  /**
   * This adds n Strings to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a String.
   */
  @Override
  public void addNStrings(final int n, final String value) {
    addNInts(n, String2.parseInt(value)); // handles NaN and MV
  }

  /**
   * This adds n floats to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a float.
   */
  @Override
  public void addNFloats(final int n, final float value) {
    if (!maxIsMV && (!Float.isFinite(value) || value < MIN_VALUE || value > MAX_VALUE))
      maxIsMV = true;
    addN(n, Math2.roundToUByte(value));
  }

  /**
   * This adds n doubles to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a double.
   */
  @Override
  public void addNDoubles(final int n, final double value) {
    if (!maxIsMV && (!Double.isFinite(value) || value < MIN_VALUE || value > MAX_VALUE))
      maxIsMV = true;
    addN(n, Math2.roundToUByte(value));
  }

  /**
   * This adds n ints to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public void addNInts(final int n, final int value) {
    if (value < MIN_VALUE || value > MAX_VALUE) {
      maxIsMV = true;
      addN(n, MAX_VALUE);
    } else {
      addN(n, (short) value);
    }
  }

  /**
   * This adds n longs to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public void addNLongs(final int n, final long value) {
    if (value < MIN_VALUE || value > MAX_VALUE) {
      maxIsMV = true;
      addN(n, MAX_VALUE);
    } else {
      addN(n, (short) value);
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
                + " in UByteArray.addFromPA: otherIndex="
                + otherIndex
                + " + nValues="
                + nValues
                + " > otherPA.size="
                + otherPA.size);
      ensureCapacity(size + nValues);
      System.arraycopy(((UByteArray) otherPA).array, otherIndex, array, size, nValues);
      size += nValues;
      if (otherPA.getMaxIsMV()) maxIsMV = true;
      return this;
    }

    // add from different type
    for (int i = 0; i < nValues; i++)
      addInt(otherPA.getInt(otherIndex++)); // does error checking and handles maxIsMV
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
    setInt(index, otherPA.getInt(otherIndex)); // handles maxIsMV
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
          String2.ERROR + " in UByteArray.removeRange: to (" + to + ") > size (" + size + ").");
    if (from >= to) {
      if (from == to) return;
      throw new IllegalArgumentException(
          String2.ERROR + " in UByteArray.removeRange: from (" + from + ") > to (" + to + ").");
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
    final String errorIn = String2.ERROR + " in UByteArray.move:\n";

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
    final byte[] temp = new byte[nToMove];
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
      Math2.ensureArraySizeOkay(minCapacity, "UByteArray");
      // caller may know exact number needed, so don't double above 2x current size
      int newCapacity = (int) Math.min(Integer.MAX_VALUE - 1, array.length + (long) array.length);
      if (newCapacity < minCapacity) newCapacity = (int) minCapacity; // safe since checked above
      Math2.ensureMemoryAvailable(newCapacity, "UByteArray");
      final byte[] newArray = new byte[newCapacity];
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
  public byte[] toArray() {
    if (array.length == size) return array;
    Math2.ensureMemoryAvailable(1L * size, "UByteArray.toArray");
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
   * @return a double[] (perhaps 'array') which has 'size' elements. If maxIsMV, MAX_VALUE is
   *     converted to Double.NaN.
   */
  @Override
  public double[] toDoubleArray() {
    Math2.ensureMemoryAvailable(8L * size, "UByteArray.toDoubleArray");
    final double dar[] = new double[size];
    for (int i = 0; i < size; i++) {
      final short b = unpack(array[i]);
      dar[i] = maxIsMV && b == MAX_VALUE ? Double.NaN : b;
    }
    return dar;
  }

  /**
   * This returns a String[] which has 'size' elements.
   *
   * @return a String[] which has 'size' elements. MAX_VALUE appears as "".
   */
  @Override
  public String[] toStringArray() {
    Math2.ensureMemoryAvailable(
        8L * size, "UByteArray.toStringArray"); // 8L is feeble minimal estimate
    final String sar[] = new String[size];
    for (int i = 0; i < size; i++) {
      final short b = unpack(array[i]);
      sar[i] = maxIsMV && b == MAX_VALUE ? "" : String.valueOf(b);
    }
    return sar;
  }

  /**
   * This gets a specified element.
   *
   * @param index 0 ... size-1
   * @return the specified element
   */
  public short get(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in UByteArray.get: index (" + index + ") >= size (" + size + ").");
    return unpack(array[index]);
  }

  /**
   * This gets a specified element as a packed value.
   *
   * @param index 0 ... size-1
   * @return the specified element
   */
  public byte getPacked(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in UByteArray.get: index (" + index + ") >= size (" + size + ").");
    return array[index];
  }

  /**
   * This sets a specified element.
   *
   * @param index 0 ... size-1
   * @param value the value for that element
   */
  public void set(final int index, final short value) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in UByteArray.set: index (" + index + ") >= size (" + size + ").");
    array[index] = pack(value);
  }

  /**
   * Return a value from the array as an int.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int. If maxIsMV, MAX_VALUE is returned as Integer.MAX_VALUE.
   */
  @Override
  public int getInt(final int index) {
    final short b = get(index);
    return maxIsMV && b == MAX_VALUE ? Integer.MAX_VALUE : b;
  }

  /**
   * Return a value from the array as an int. This "raw" variant leaves missingValue from smaller
   * data types (e.g., UByteArray missingValue=255) AS IS (even if maxIsMV=true). Floating point
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
   *     Math2.narrowToUByte(i).
   */
  @Override
  public void setInt(final int index, final int i) {
    if (i < MIN_VALUE || i > MAX_VALUE) {
      maxIsMV = true;
      set(index, MAX_VALUE);
    } else {
      set(index, (short) i);
    }
  }

  /**
   * Return a value from the array as a long.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a long. If maxIsMV, MAX_VALUE is returned as Long.MAX_VALUE.
   */
  @Override
  public long getLong(final int index) {
    final short b = get(index);
    return maxIsMV && b == MAX_VALUE ? Long.MAX_VALUE : b;
  }

  /**
   * Set a value in the array as a long.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToUByte(long).
   */
  @Override
  public void setLong(final int index, final long i) {
    if (i < MIN_VALUE || i > MAX_VALUE) {
      maxIsMV = true;
      set(index, MAX_VALUE);
    } else {
      set(index, (short) i);
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
    final short b = get(index);
    return maxIsMV && b == MAX_VALUE ? null : new BigInteger("" + b);
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
   *     return Float.NaN. If maxIsMV, MAX_VALUE is returned as Float.NaN.
   */
  @Override
  public float getFloat(final int index) {
    final short b = get(index);
    return maxIsMV && b == MAX_VALUE ? Float.NaN : b;
  }

  /**
   * Set a value in the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. For numeric PrimitiveArray, it is narrowed if needed by methods like
   *     Math2.roundToUByte(d).
   */
  @Override
  public void setFloat(final int index, final float d) {
    if (!maxIsMV && (!Float.isFinite(d) || d < MIN_VALUE || d > MAX_VALUE)) maxIsMV = true;
    set(index, Math2.roundToUByte(d));
  }

  /**
   * Return a value from the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN. If maxIsMV, MAX_VALUE is returned as Double.NaN.
   */
  @Override
  public double getDouble(final int index) {
    final short b = get(index);
    return maxIsMV && b == MAX_VALUE ? Double.NaN : b;
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
    // or see
    // https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/faq.html#Unsigned
    return getDouble(index); // already unsigned
  }

  /**
   * Return a value from the array as a double. This "raw" variant leaves missingValue from integer
   * data types (e.g., UByteArray missingValue=127) AS IS (even if maxIsMV=true).
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
   *     Math2.roundToUByte(d).
   */
  @Override
  public void setDouble(final int index, final double d) {
    if (!maxIsMV && (!Double.isFinite(d) || d < MIN_VALUE || d > MAX_VALUE)) maxIsMV = true;
    set(index, Math2.roundToUByte(d));
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
    final byte b = getPacked(index);
    return maxIsMV && b == PACKED_MAX_VALUE ? "" : String.valueOf(unpack(b));
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
    final byte b = getPacked(index);
    return maxIsMV && b == PACKED_MAX_VALUE ? "null" : String.valueOf(unpack(b));
  }

  /**
   * Return a value from the array as a String. This "raw" variant leaves missingValue from integer
   * data types (e.g., UByteArray missingValue=127) AS IS, regardless of maxIsMV. FloatArray and
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
   * @param s the value. For numeric PrimitiveArray's, it is parsed with String2.parseInt and
   *     narrowed by Math2.narrowToUByte(i).
   */
  @Override
  public void setString(final int index, final String s) {
    setInt(index, String2.parseInt(s)); // handles mv
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final short lookFor) {
    return indexOf(lookFor, 0);
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final short lookFor, final int startIndex) {
    final byte packedLookFor = pack(lookFor);
    for (int i = startIndex; i < size; i++) if (array[i] == packedLookFor) return i;
    return -1;
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  @Override
  public int indexOf(final String lookFor, final int startIndex) {
    if (startIndex >= size) return -1;
    return indexOf(Math2.roundToUByte(String2.parseInt(lookFor)), startIndex);
  }

  /**
   * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int lastIndexOf(final short lookFor, final int startIndex) {
    if (startIndex >= size)
      throw new IllegalArgumentException(
          String2.ERROR
              + " in UByteArray.get: startIndex ("
              + startIndex
              + ") >= size ("
              + size
              + ").");
    final byte packedLookFor = pack(lookFor);
    for (int i = startIndex; i >= 0; i--) if (array[i] == packedLookFor) return i;
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
    return lastIndexOf(Math2.roundToUByte(String2.parseInt(lookFor)), startIndex);
  }

  /** If size != capacity, this makes a new 'array' of size 'size' so capacity will equal size. */
  @Override
  public void trimToSize() {
    array = toArray();
  }

  /**
   * Test if o is an UByteArray with the same size and values.
   *
   * @param o the object that will be compared to this UByteArray
   * @return true if equal. o=null returns false.
   */
  @Override
  public boolean equals(final Object o) {
    return testEquals(o).length() == 0;
  }

  /**
   * Test if o is an UByteArray with the same size and values, but returns a String describing the
   * difference (or "" if equal).
   *
   * @param o The other object
   * @return a String describing the difference (or "" if equal). o=null doesn't throw an exception.
   */
  @Override
  public String testEquals(final Object o) {
    if (!(o instanceof UByteArray))
      return "The two objects aren't equal: this object is a UByteArray; the other is a "
          + (o == null ? "null" : o.getClass().getName())
          + ".";
    final UByteArray other = (UByteArray) o;
    if (other.size() != size)
      return "The two UByteArrays aren't equal: one has "
          + size
          + " value(s); the other has "
          + other.size()
          + " value(s).";
    for (int i = 0; i < size; i++)
      if (array[i] != other.array[i]
          || (array[i] == PACKED_MAX_VALUE && maxIsMV != other.maxIsMV)) // handles mv
      return "The two UByteArrays aren't equal: this["
            + i
            + "]="
            + getInt(i)
            + "; other["
            + i
            + "]="
            + other.getInt(i)
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
    // estimate 5 bytes/element
    final StringBuilder sb = new StringBuilder(5 * Math.min(size, (Integer.MAX_VALUE - 8192) / 5));
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(", ");
      sb.append(unpack(array[i]));
    }
    return sb.toString();
  }

  /**
   * This converts the elements into an NCCSV attribute String, e.g.,: -128b, 127b Integer types
   * show MAX_VALUE numbers (not "").
   *
   * @return an NCCSV attribute String
   */
  @Override
  public String toNccsvAttString() {
    final StringBuilder sb = new StringBuilder(size * 6);
    for (int i = 0; i < size; i++) sb.append((i == 0 ? "" : ",") + unpack(array[i]) + "ub");
    return sb.toString();
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
      final int b = array[i];
      array[i] = (byte) (b < 0 ? b + 128 : b - 128);
    }
  }

  /**
   * This sorts the elements in ascending order. To get the elements in reverse order, just read
   * from the end of the list to the beginning.
   */
  @Override
  public void sort() {
    // Sorting unsigned PrimitiveArrays is more complicated.
    // First, sort the signed values in array:
    // see switchover point and speed comparison in
    //  https://www.baeldung.com/java-arrays-sort-vs-parallelsort
    if (size < 8192) Arrays.sort(array, 0, size);
    else Arrays.parallelSort(array, 0, size);

    // Then find the first value >=0, and move it and subsequent to beginning of array.
    // You can't use PrimitiveArray.binarySearch because it works on unsigned values
    //  (via PAOne) and the array is sorted according to the signed values.
    // This is not ideal, but this is rarely used.
    // [Future: you could use Arrays.binarySearch() with extra effort to find *first* value >=0.]
    int which = 0;
    while (which < size && array[which] < 0) which++;
    move(which, size, 0);
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
    return Integer.compare(getInt(index1), otherPA.getInt(index2)); // int handles mv
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
    Math2.ensureMemoryAvailable(1L * array.length, "UByteArray");
    final byte newArray[] = new byte[array.length];
    for (int i = 0; i < n; i++) newArray[i] = array[rank[i]];
    array = newArray;
  }

  /**
   * This reverses the order of the bytes in each value, e.g., if the data was read from a
   * little-endian source.
   */
  @Override
  public void reverseBytes() {
    // UByteArray does nothing
  }

  /**
   * This writes 'size' elements to a DataOutputStream.
   *
   * @param dos the DataOutputStream
   * @return the number of bytes used per element
   * @throws Exception if trouble
   */
  @Override
  public int writeDos(final DataOutputStream dos) throws Exception {
    dos.write(array, 0, size); // special case
    return size == 0 ? 0 : 1;
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
    dos.writeByte(array[i]);
    return 1;
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
    dis.readFully(array, size, n);
    size += n;
  }

  /**
   * This writes all the data to a DataOutputStream in the DODS Array format (see www.opendap.org
   * DAP 2.0 standard, section 7.3.2.1). See also the XDR standard
   * (http://tools.ietf.org/html/rfc4506#section-4.11).
   *
   * @param dos the DataOutputStream
   * @throws Exception if trouble
   */
  @Override
  public void externalizeForDODS(final DataOutputStream dos) throws Exception {
    super.externalizeForDODS(dos); // writes as bytes

    // pad to 4 bytes boundary at end
    int tSize = size;
    while (tSize++ % 4 != 0) dos.writeByte(0);
  }

  /**
   * This writes one element to a DataOutputStream in the DODS Atomic-type format (see
   * www.opendap.org DAP 2.0 standard, section 7.3.2). See also the XDR standard
   * (http://tools.ietf.org/html/rfc4506#section-4.11).
   *
   * @param dos the DataOutputStream
   * @param i the index of the element to be written
   * @throws Exception if trouble
   */
  @Override
  public void externalizeForDODS(final DataOutputStream dos, final int i) throws Exception {
    dos.writeInt(array[i] << 24); // as if byte + 3 padding bytes
  }

  /**
   * This reads/appends byte values to this PrimitiveArray from a DODS DataInputStream, and is thus
   * the complement of externalizeForDODS.
   *
   * @param dis the DataInputStream
   * @throws IOException if trouble
   */
  @Override
  public void internalizeFromDODS(final DataInputStream dis) throws java.io.IOException {
    int nValues = dis.readInt();
    dis.readInt(); // skip duplicate of nValues
    ensureCapacity(size + (long) nValues);
    dis.readFully(array, size, nValues);
    size += nValues;

    // read the padding bytes
    while (nValues++ % 4 != 0) dis.readByte();
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
    raf.writeByte(array[index]);
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
    addPacked(raf.readByte());
  }

  /**
   * This appends the data in another pa to the current data. WARNING: information may be lost from
   * the incoming pa if this primitiveArray is of a smaller type; see needPAType().
   *
   * @param pa pa must be the same or a narrower or the same data type, or the data will be narrowed
   *     with Math2.narrowToUByte.
   */
  @Override
  public void append(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof UByteArray ua) {
      if (pa.getMaxIsMV()) setMaxIsMV(true);
      System.arraycopy(ua.array, 0, array, size, otherSize);
      size += otherSize;
    } else {
      for (int i = 0; i < otherSize; i++)
        addInt(pa.getInt(i)); // this converts mv's and handles maxIsMV
    }
  }

  /**
   * This appends the data in another pa to the current data. This "raw" variant leaves missingValue
   * from smaller data types (e.g., UByteArray missingValue=127) AS IS. WARNING: information may be
   * lost from the incoming pa if this primitiveArray is of a simpler type.
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     Math2.narrowToUByte.
   */
  @Override
  public void rawAppend(final PrimitiveArray pa) {
    // since there are no smaller data types than byte, rawAppend() = append()
    append(pa);
  }

  /**
   * This populates 'indices' with the indices (ranks) of the values in this UByteArray (ties get
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
      return new UByteArray();
    }

    // make a hashMap with all the unique values (associated values are initially all dummy)
    // (actually bytes could be done more efficiently with a boolean array -128 to 127... )
    final Integer dummy = Integer.valueOf(-1);
    final HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
    short lastValue = unpack(array[0]); // since lastValue often equals currentValue, cache it
    hashMap.put(Short.valueOf(lastValue), dummy);
    boolean alreadySorted = true;
    for (int i = 1; i < size; i++) {
      short currentValue = unpack(array[i]);
      if (currentValue != lastValue) {
        if (currentValue < lastValue) alreadySorted = false;
        lastValue = currentValue;
        hashMap.put(Short.valueOf(lastValue), dummy);
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
          "UByteArray.makeRankArray nUnique(" + nUnique + ") != count(" + count + ")!");

    // sort them
    Arrays.sort(unique);

    // put the unique values back in the hashMap with the ranks as the associated values
    // and make tUnique
    final short tUnique[] = new short[nUnique];
    for (int i = 0; i < count; i++) {
      hashMap.put(unique[i], Integer.valueOf(i));
      tUnique[i] = ((Short) unique[i]).shortValue();
    }

    // convert original values to ranks
    final int ranks[] = new int[size];
    lastValue = unpack(array[0]);
    ranks[0] = ((Integer) hashMap.get(Short.valueOf(lastValue))).intValue();
    int lastRank = ranks[0];
    for (int i = 1; i < size; i++) {
      if (array[i] == lastValue) {
        ranks[i] = lastRank;
      } else {
        lastValue = unpack(array[i]);
        ranks[i] = ((Integer) hashMap.get(Short.valueOf(lastValue))).intValue();
        lastRank = ranks[i];
      }
    }

    // store the results in ranked
    indices.append(new IntArray(ranks));

    return new UByteArray(tUnique);
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
    final byte packedFrom = pack(Math2.roundToUByte(String2.parseDouble(tFrom)));
    final double d = String2.parseDouble(tTo);
    final byte packedTo = pack(Math2.roundToUByte(d));
    // String2.log(">> UByteArray.switchFromTo " + tFrom + "=" + packedFrom + " to " + tTo + "=" +
    // packedTo);
    if (packedFrom == packedTo) return 0;
    int count = 0;
    for (int i = 0; i < size; i++) {
      if (array[i] == packedFrom) {
        array[i] = packedTo;
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
    short tmin = MAX_VALUE;
    short tmax = MIN_VALUE;
    for (int i = 0; i < size; i++) {
      if (maxIsMV && array[i] == PACKED_MAX_VALUE) {
      } else {
        short v = unpack(array[i]);
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
