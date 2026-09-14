package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.BufferedFileChannel;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.BitSet;
import ucar.ma2.StructureData;

/** PrimitiveView provides a zero-copy virtual view over a PrimitiveArray. */
public class PrimitiveView extends PrimitiveArray {

  public PrimitiveArray source;
  public int offset;
  public int stride;
  public volatile PrimitiveArray materialized;

  /**
   * Constructs a PrimitiveView over a source PrimitiveArray.
   *
   * @param source the source PrimitiveArray
   * @param offset the starting offset in source
   * @param stride the step size between elements
   * @param length the number of elements in this view
   */
  public PrimitiveView(PrimitiveArray source, int offset, int stride, int length) {
    if (source == null) {
      throw new IllegalArgumentException("PrimitiveView source cannot be null.");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("PrimitiveView offset (" + offset + ") must be >= 0.");
    }
    if (stride <= 0) {
      throw new IllegalArgumentException("PrimitiveView stride (" + stride + ") must be > 0.");
    }
    if (length < 0) {
      throw new IllegalArgumentException("PrimitiveView length (" + length + ") must be >= 0.");
    }
    if (offset > source.size()) {
      throw new IndexOutOfBoundsException(
          "PrimitiveView offset (" + offset + ") > source size (" + source.size() + ").");
    }
    if (length > 0 && (offset + (long) (length - 1) * stride) >= source.size()) {
      throw new IndexOutOfBoundsException("PrimitiveView range exceeds source size.");
    }

    while (source instanceof PrimitiveView pv) {
      if (pv.materialized != null) {
        source = pv.materialized;
        break;
      }
      offset = pv.offset + (offset * pv.stride);
      stride = pv.stride * stride;
      source = pv.source;
    }

    this.source = source;
    this.offset = offset;
    this.stride = stride;
    this.size = length;
    this.materialized = null;
    this.setMaxIsMV(source.getMaxIsMV());
  }

  /**
   * Materializes this view into a concrete PrimitiveArray if not already materialized.
   *
   * @return the materialized PrimitiveArray
   */
  public synchronized PrimitiveArray materialize() {
    if (materialized == null) {
      PrimitiveArray newArray = PrimitiveArray.factory(elementType(), size, false);
      newArray.setMaxIsMV(getMaxIsMV());
      for (int i = 0; i < size; i++) {
        newArray.addFromPA(source, offset + i * stride, 1);
      }
      materialized = newArray;
    }
    return materialized;
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException(
          String2.ERROR
              + " in PrimitiveView: index ("
              + index
              + ") out of bounds for view size ("
              + size
              + ").");
    }
  }

  private void updateStateFromMaterialized() {
    this.source = this.materialized;
    this.size = this.materialized.size();
    this.offset = 0;
    this.stride = 1;
  }

  @Override
  public PrimitiveArray setMaxIsMV(boolean tMaxIsMV) {
    super.setMaxIsMV(tMaxIsMV);
    PrimitiveArray m = materialized;
    if (m != null) {
      m.setMaxIsMV(tMaxIsMV);
    }
    return this;
  }

  @Override
  public PAType elementType() {
    return source.elementType();
  }

  @Override
  public int elementSize() {
    return source.elementSize();
  }

  @Override
  public double missingValueAsDouble() {
    return source.missingValueAsDouble();
  }

  @Override
  public PAOne MINEST_VALUE() {
    return source.MINEST_VALUE();
  }

  @Override
  public PAOne MAXEST_VALUE() {
    return source.MAXEST_VALUE();
  }

  @Override
  public int capacity() {
    PrimitiveArray m = materialized;
    return m != null ? m.capacity() : size;
  }

  @Override
  public PAType needPAType(PAType tPAType) {
    return source.needPAType(tPAType);
  }

  @Override
  public boolean isUnsigned() {
    return source.isUnsigned();
  }

  @Override
  public boolean isIntegerType() {
    return source.isIntegerType();
  }

  @Override
  public boolean isFloatingPointType() {
    return source.isFloatingPointType();
  }

  @Override
  public boolean supportsMaxIsMV() {
    return source.supportsMaxIsMV();
  }

  // Getters
  @Override
  public int getInt(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getInt(index) : source.getInt(offset + index * stride);
  }

  @Override
  public int getRawInt(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getRawInt(index) : source.getRawInt(offset + index * stride);
  }

  @Override
  public long getLong(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getLong(index) : source.getLong(offset + index * stride);
  }

  @Override
  public BigInteger getULong(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getULong(index) : source.getULong(offset + index * stride);
  }

  @Override
  public float getFloat(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getFloat(index) : source.getFloat(offset + index * stride);
  }

  @Override
  public double getDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getDouble(index) : source.getDouble(offset + index * stride);
  }

  @Override
  public double getUnsignedDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null
        ? m.getUnsignedDouble(index)
        : source.getUnsignedDouble(offset + index * stride);
  }

  @Override
  public double getRawDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getRawDouble(index) : source.getRawDouble(offset + index * stride);
  }

  @Override
  public double getNiceDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getNiceDouble(index) : source.getNiceDouble(offset + index * stride);
  }

  @Override
  public double getRawNiceDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getRawNiceDouble(index) : source.getRawNiceDouble(offset + index * stride);
  }

  @Override
  public String getString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getString(index) : source.getString(offset + index * stride);
  }

  @Override
  public String getJsonString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getJsonString(index) : source.getJsonString(offset + index * stride);
  }

  @Override
  public String getNccsvDataString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null
        ? m.getNccsvDataString(index)
        : source.getNccsvDataString(offset + index * stride);
  }

  @Override
  public String getNccsv127DataString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null
        ? m.getNccsv127DataString(index)
        : source.getNccsv127DataString(offset + index * stride);
  }

  @Override
  public String getSVString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getSVString(index) : source.getSVString(offset + index * stride);
  }

  @Override
  public String getUtf8TsvString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getUtf8TsvString(index) : source.getUtf8TsvString(offset + index * stride);
  }

  @Override
  public String getRawString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getRawString(index) : source.getRawString(offset + index * stride);
  }

  @Override
  public String getRawestString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getRawestString(index) : source.getRawestString(offset + index * stride);
  }

  @Override
  public PAOne getPAOne(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getPAOne(index) : source.getPAOne(offset + index * stride);
  }

  @Override
  public PAOne getPAOne(int index, PAOne paOne) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.getPAOne(index, paOne) : source.getPAOne(offset + index * stride, paOne);
  }

  @Override
  public boolean isMaxValue(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.isMaxValue(index) : source.isMaxValue(offset + index * stride);
  }

  @Override
  public boolean isMissingValue(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    return m != null ? m.isMissingValue(index) : source.isMissingValue(offset + index * stride);
  }

  @Override
  public int indexOf(String lookFor, int startIndex) {
    PrimitiveArray m = materialized;
    if (m != null) return m.indexOf(lookFor, startIndex);

    if (stride == 1) {
      int sourceStart = offset + Math.max(0, startIndex);
      if (sourceStart >= offset + size) return -1;
      int found = source.indexOf(lookFor, sourceStart);
      return (found >= 0 && found < offset + size) ? (found - offset) : -1;
    }

    for (int i = Math.max(0, startIndex); i < size; i++) {
      String s = getString(i);
      if (s == null ? lookFor == null : s.equals(lookFor)) return i;
    }
    return -1;
  }

  @Override
  public int lastIndexOf(String lookFor, int startIndex) {
    PrimitiveArray m = materialized;
    if (m != null) return m.lastIndexOf(lookFor, startIndex);

    if (stride == 1) {
      int sourceStart = offset + Math.min(size - 1, startIndex);
      if (sourceStart < offset) return -1;
      int found = source.lastIndexOf(lookFor, sourceStart);
      return (found >= offset) ? (found - offset) : -1;
    }

    for (int i = Math.min(size - 1, startIndex); i >= 0; i--) {
      String s = getString(i);
      if (s == null ? lookFor == null : s.equals(lookFor)) return i;
    }
    return -1;
  }

  // Point Setters & Mutating Methods
  @Override
  public void setInt(int index, int i) {
    checkIndex(index);
    materialize().setInt(index, i);
  }

  @Override
  public void setLong(int index, long i) {
    checkIndex(index);
    materialize().setLong(index, i);
  }

  @Override
  public void setULong(int index, BigInteger i) {
    checkIndex(index);
    materialize().setULong(index, i);
  }

  @Override
  public void setFloat(int index, float d) {
    checkIndex(index);
    materialize().setFloat(index, d);
  }

  @Override
  public void setDouble(int index, double d) {
    checkIndex(index);
    materialize().setDouble(index, d);
  }

  @Override
  public void setString(int index, String s) {
    checkIndex(index);
    materialize().setString(index, s);
  }

  @Override
  public void setPAOne(int index, PAOne paOne) {
    checkIndex(index);
    materialize().setPAOne(index, paOne);
  }

  @Override
  public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex) {
    checkIndex(index);
    materialize().setFromPA(index, otherPA, otherIndex);
  }

  @Override
  public void atInsertString(int index, String value) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException(
          "Index (" + index + ") out of bounds for insertion into size (" + size + ").");
    }
    materialize().atInsertString(index, value);
    updateStateFromMaterialized();
  }

  @Override
  public void addObject(Object value) {
    materialize().addObject(value);
    updateStateFromMaterialized();
  }

  @Override
  public void add(StructureData sd, String memberName) {
    materialize().add(sd, memberName);
    updateStateFromMaterialized();
  }

  @Override
  public void addN(StructureData sd, String memberName, int n) {
    materialize().addN(sd, memberName, n);
    updateStateFromMaterialized();
  }

  @Override
  public void addNPAOnes(int n, PAOne value) {
    materialize().addNPAOnes(n, value);
    updateStateFromMaterialized();
  }

  @Override
  public void addNStrings(int n, String value) {
    materialize().addNStrings(n, value);
    updateStateFromMaterialized();
  }

  @Override
  public void addNFloats(int n, float value) {
    materialize().addNFloats(n, value);
    updateStateFromMaterialized();
  }

  @Override
  public void addNDoubles(int n, double value) {
    materialize().addNDoubles(n, value);
    updateStateFromMaterialized();
  }

  @Override
  public void addNInts(int n, int value) {
    materialize().addNInts(n, value);
    updateStateFromMaterialized();
  }

  @Override
  public void addNLongs(int n, long value) {
    materialize().addNLongs(n, value);
    updateStateFromMaterialized();
  }

  @Override
  public PrimitiveArray addFromPA(PrimitiveArray otherPA, int otherIndex, int nValues) {
    materialize().addFromPA(otherPA, otherIndex, nValues);
    updateStateFromMaterialized();
    return this;
  }

  @Override
  public void append(PrimitiveArray primitiveArray) {
    materialize().append(primitiveArray);
    updateStateFromMaterialized();
  }

  @Override
  public void rawAppend(PrimitiveArray primitiveArray) {
    materialize().rawAppend(primitiveArray);
    updateStateFromMaterialized();
  }

  @Override
  public void copy(int from, int to) {
    materialize().copy(from, to);
  }

  @Override
  public void move(int first, int last, int destination) {
    materialize().move(first, last, destination);
  }

  @Override
  public void sort() {
    materialize().sort();
  }

  @Override
  public void sortIgnoreCase() {
    materialize().sortIgnoreCase();
  }

  @Override
  public void reverseBytes() {
    materialize().reverseBytes();
  }

  @Override
  public void reorder(int[] rank) {
    materialize().reorder(rank);
  }

  @Override
  public void trimToSize() {
    materialize().trimToSize();
  }

  @Override
  public void ensureCapacity(long minCapacity) {
    materialize().ensureCapacity(minCapacity);
  }

  // Optimized Mutators (Rule 5)
  @Override
  public void remove(int index) {
    checkIndex(index);
    int targetSize = size - 1;
    PrimitiveArray target = PrimitiveArray.factory(elementType(), targetSize, false);
    target.setMaxIsMV(getMaxIsMV());
    PrimitiveArray m = materialized;
    if (m != null) {
      if (index > 0) target.addFromPA(m, 0, index);
      if (index < targetSize) target.addFromPA(m, index + 1, targetSize - index);
    } else {
      for (int i = 0; i < index; i++) {
        target.addFromPA(source, offset + i * stride, 1);
      }
      for (int i = index + 1; i < size; i++) {
        target.addFromPA(source, offset + i * stride, 1);
      }
    }
    this.materialized = target;
    this.source = target;
    this.size = targetSize;
    this.offset = 0;
    this.stride = 1;
  }

  @Override
  public void removeRange(int from, int to) {
    if (from < 0 || to < from || to > size) {
      throw new IllegalArgumentException(
          "Invalid range in removeRange: from=" + from + ", to=" + to + ", size=" + size);
    }
    int numToRemove = to - from;
    if (numToRemove == 0) return;
    int targetSize = size - numToRemove;
    PrimitiveArray target = PrimitiveArray.factory(elementType(), targetSize, false);
    target.setMaxIsMV(getMaxIsMV());
    PrimitiveArray m = materialized;
    if (m != null) {
      if (from > 0) target.addFromPA(m, 0, from);
      if (to < size) target.addFromPA(m, to, size - to);
    } else {
      for (int i = 0; i < from; i++) {
        target.addFromPA(source, offset + i * stride, 1);
      }
      for (int i = to; i < size; i++) {
        target.addFromPA(source, offset + i * stride, 1);
      }
    }
    this.materialized = target;
    this.source = target;
    this.size = targetSize;
    this.offset = 0;
    this.stride = 1;
  }

  @Override
  public void justKeep(BitSet bitset) {
    int keepCount = 0;
    for (int i = bitset.nextSetBit(0); i >= 0 && i < size; i = bitset.nextSetBit(i + 1)) {
      keepCount++;
    }
    PrimitiveArray target = PrimitiveArray.factory(elementType(), keepCount, false);
    target.setMaxIsMV(getMaxIsMV());
    PrimitiveArray m = materialized;
    if (m != null) {
      for (int i = bitset.nextSetBit(0); i >= 0 && i < size; i = bitset.nextSetBit(i + 1)) {
        target.addFromPA(m, i, 1);
      }
    } else {
      for (int i = bitset.nextSetBit(0); i >= 0 && i < size; i = bitset.nextSetBit(i + 1)) {
        target.addFromPA(source, offset + i * stride, 1);
      }
    }
    this.materialized = target;
    this.source = target;
    this.size = keepCount;
    this.offset = 0;
    this.stride = 1;
  }

  // State Re-Initialization (Rule 6)
  @Override
  public void clear() {
    super.clear();
    this.source = PrimitiveArray.factory(elementType(), 0, false);
    this.materialized = null;
    this.offset = 0;
    this.stride = 1;
  }

  // Subsets
  @Override
  public PrimitiveArray subset(PrimitiveArray pa, int startIndex, int stride, int stopIndex) {
    if (startIndex < 0) {
      throw new IllegalArgumentException(
          String2.ERROR
              + " in PrimitiveView.subset: startIndex="
              + startIndex
              + " must be at least 0.");
    }
    if (stride < 1) {
      throw new IllegalArgumentException(
          String2.ERROR
              + " in PrimitiveView.subset: stride="
              + stride
              + " must be greater than 0.");
    }
    if (stopIndex < startIndex) {
      if (pa == null) return new PrimitiveView(this, 0, 1, 0);
      pa.clear();
      return pa;
    }
    int effectiveStop = Math.min(stopIndex, size - 1);
    int subLength = PrimitiveArray.strideWillFind(effectiveStop - startIndex + 1, stride);
    if (pa == null) {
      return new PrimitiveView(this, startIndex, stride, subLength);
    }

    pa.clear();
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.subset(pa, startIndex, stride, effectiveStop);
    }
    for (int i = 0; i < subLength; i++) {
      pa.addFromPA(this, startIndex + i * stride, 1);
    }
    return pa;
  }

  // Formatters
  @Override
  public String toString() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toString();
    StringBuilder sb = new StringBuilder(size * 8);
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(", ");
      if (elementType() == PAType.CHAR) {
        sb.append(String2.toNccsv127DataString(getRawestString(i)));
      } else {
        sb.append(getRawestString(i));
      }
    }
    return sb.toString();
  }

  @Override
  public String toCSVString() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toCSVString();
    StringBuilder sb = new StringBuilder(size * 8);
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(",");
      sb.append(getSVString(i));
    }
    return sb.toString();
  }

  @Override
  public String toNccsvAttString() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toNccsvAttString();

    PAType type = elementType();
    if (type == PAType.STRING) {
      StringBuilder sb = new StringBuilder(size * 12);
      for (int i = 0; i < size; i++) {
        if (i > 0) sb.append(",");
        sb.append(String2.toNccsvAttString(getString(i)));
      }
      return sb.toString();
    }

    String suffix =
        switch (type) {
          case BYTE, UBYTE -> "b";
          case SHORT, USHORT -> "s";
          case INT, UINT -> "i";
          case LONG, ULONG -> "L";
          case FLOAT -> "f";
          case DOUBLE -> "d";
          default -> "";
        };

    StringBuilder sb = new StringBuilder(size * 10);
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(",");
      if (type == PAType.CHAR) {
        sb.append("\"'").append(String2.toNccsvChar((char) getRawInt(i))).append("'\"");
      } else {
        sb.append(getRawestString(i)).append(suffix);
      }
    }
    return sb.toString();
  }

  @Override
  public String toNccsv127AttString() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toNccsv127AttString();

    if (elementType() == PAType.CHAR) {
      StringBuilder sb = new StringBuilder(size * 10);
      for (int i = 0; i < size; i++) {
        if (i > 0) sb.append(",");
        sb.append("\"'").append(String2.toNccsv127Char((char) getRawInt(i))).append("'\"");
      }
      return sb.toString();
    }
    return toNccsvAttString();
  }

  @Override
  public String toJsonCsvString() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toJsonCsvString();
    StringBuilder sb = new StringBuilder(size * 8);
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(", ");
      sb.append(getJsonString(i));
    }
    return sb.toString();
  }

  // Array conversion methods
  /**
   * Returns a 1D Java array (e.g., double[], float[], String[]) containing all elements in this
   * view. If materialized, delegates to the backing PrimitiveArray. If unmaterialized, populates a
   * raw primitive array directly without triggering copy-on-write materialization.
   *
   * @return A 1D primitive or object array matching this view's element type.
   */
  public Object toObjectArray() {
    // 1. Delegate to PrimitiveArray if already materialized
    PrimitiveArray m = materialized;
    if (m != null) return m.toObjectArray();

    int size = size();
    PAType type = elementType(); // or paType() / elementClass() depending on your class contract

    // 2. Unmaterialized path: Allocate and populate raw Java primitive array directly
    switch (type) {
      case DOUBLE:
        {
          double[] array = new double[size];
          for (int i = 0; i < size; i++) {
            array[i] = getDouble(i);
          }
          return array;
        }
      case FLOAT:
        {
          float[] array = new float[size];
          for (int i = 0; i < size; i++) {
            array[i] = getFloat(i);
          }
          return array;
        }
      case INT:
      case UINT:
        {
          int[] array = new int[size];
          for (int i = 0; i < size; i++) {
            array[i] = getInt(i);
          }
          return array;
        }
      case LONG:
      case ULONG:
        {
          long[] array = new long[size];
          for (int i = 0; i < size; i++) {
            array[i] = getLong(i);
          }
          return array;
        }
      case SHORT:
      case USHORT:
        {
          short[] array = new short[size];
          for (int i = 0; i < size; i++) {
            array[i] = (short) getInt(i);
          }
          return array;
        }
      case BYTE:
      case UBYTE:
        {
          byte[] array = new byte[size];
          for (int i = 0; i < size; i++) {
            array[i] = (byte) getInt(i);
          }
          return array;
        }
      case CHAR:
        {
          char[] array = new char[size];
          for (int i = 0; i < size; i++) {
            array[i] = (char) getRawInt(i);
          }
          return array;
        }
      case STRING:
      default:
        {
          String[] array = new String[size];
          for (int i = 0; i < size; i++) {
            array[i] = getString(i);
          }
          return array;
        }
    }
  }

  @Override
  public double[] toDoubleArray() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toDoubleArray();
    double[] dar = new double[size];
    for (int i = 0; i < size; i++) {
      dar[i] = getDouble(i);
    }
    return dar;
  }

  @Override
  public String[] toStringArray() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toStringArray();
    String[] sar = new String[size];
    for (int i = 0; i < size; i++) {
      sar[i] = getString(i);
    }
    return sar;
  }

  // Comparison
  @Override
  public int compare(int index1, PrimitiveArray otherPA, int index2) {
    checkIndex(index1);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.compare(index1, otherPA, index2);
    }
    return source.compare(offset + index1 * stride, otherPA, index2);
  }

  @Override
  public int compare(int index1, int index2) {
    checkIndex(index1);
    checkIndex(index2);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.compare(index1, index2);
    }
    return source.compare(offset + index1 * stride, offset + index2 * stride);
  }

  // Write I/O Methods
  @Override
  public long writeToChannel(BufferedFileChannel channel) throws Exception {
    return writeToChannel(channel, 0, size);
  }

  @Override
  public long writeToChannel(BufferedFileChannel channel, int off, int len) throws Exception {
    if (off < 0 || len < 0 || (long) off + len > size) {
      throw new IndexOutOfBoundsException(
          "Invalid range: offset=" + off + ", len=" + len + ", view size=" + size);
    }
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.writeToChannel(channel, off, len);
    }
    if (stride == 1) {
      return source.writeToChannel(channel, offset + off, len);
    }
    return materialize().writeToChannel(channel, off, len);
  }

  @Override
  public int writeDos(DataOutputStream dos) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.writeDos(dos);
    }
    int bytesPerElem = 0;
    for (int i = 0; i < size; i++) {
      bytesPerElem = writeDos(dos, i);
    }
    return size == 0 ? 0 : bytesPerElem;
  }

  @Override
  public int writeDos(DataOutputStream dos, int i) throws Exception {
    checkIndex(i);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.writeDos(dos, i);
    }
    return source.writeDos(dos, offset + i * stride);
  }

  @Override
  public void writeToRAF(RandomAccessFile raf, int index) throws Exception {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      m.writeToRAF(raf, index);
    } else {
      source.writeToRAF(raf, offset + index * stride);
    }
  }

  @Override
  public void externalizeForDODS(DataOutputStream dos) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) {
      m.externalizeForDODS(dos);
    } else {
      dos.writeInt(size);
      dos.writeInt(size);
      writeDos(dos);
    }
  }

  @Override
  public void externalizeForDODS(DataOutputStream dos, int i) throws Exception {
    checkIndex(i);
    PrimitiveArray m = materialized;
    if (m != null) {
      m.externalizeForDODS(dos, i);
    } else {
      source.externalizeForDODS(dos, offset + i * stride);
    }
  }

  // Read I/O Methods (Materialize then delegate)
  @Override
  public void readFromChannel(FileChannel channel, int n) throws Exception {
    materialize().readFromChannel(channel, n);
    updateStateFromMaterialized();
  }

  @Override
  public void readDis(DataInputStream dis, int n) throws Exception {
    materialize().readDis(dis, n);
    updateStateFromMaterialized();
  }

  @Override
  public void readFromRAF(RandomAccessFile raf) throws Exception {
    materialize().readFromRAF(raf);
    updateStateFromMaterialized();
  }

  @Override
  public void internalizeFromDODS(DataInputStream dis) throws java.io.IOException {
    materialize().internalizeFromDODS(dis);
    updateStateFromMaterialized();
  }

  // Analysis / Helper Methods
  @Override
  public String isEvenlySpaced() {
    PrimitiveArray m = materialized;
    if (m != null) return m.isEvenlySpaced();
    if (size <= 2) return "";

    // Adjust precision digits based on underlying type
    int sigDigits = (elementType() == PAType.FLOAT) ? 4 : 9;

    double first = getDouble(0);
    double last = getDouble(size - 1);
    double diff = (last - first) / (size - 1);

    for (int i = 1; i < size; i++) {
      double prev = getDouble(i - 1);
      double curr = getDouble(i);
      double step = curr - prev;

      // Match DoubleArray/FloatArray's two-stage check using getDouble()
      if (Math2.almostEqual(sigDigits, step * 1e7, diff * 1e7)) {
        continue;
      }
      if (Math2.almostEqual(sigDigits + 3, prev + diff, curr)
          && Math2.almostEqual(2, step * 1e7, diff * 1e7)) {
        continue;
      }

      return MessageFormat.format(
          ArrayNotEvenlySpaced,
          getClass().getSimpleName(),
          "" + (i - 1),
          "" + prev,
          "" + i,
          "" + curr,
          "" + step,
          "" + diff);
    }
    return "";
  }

  @Override
  public double[] calculateStats(Attributes atts) {
    // 1. Delegate if already materialized
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.calculateStats(atts);
    }

    int n = size();
    int count = 0;
    double min = Double.NaN;
    double max = Double.NaN;
    double sum = 0;
    double sumSqr = 0;

    // 2. Unmaterialized path: Single-pass stats gathering via getDouble()
    for (int i = 0; i < n; i++) {
      double d = getDouble(i);
      if (Double.isNaN(d)) {
        continue;
      }

      if (count == 0) {
        min = d;
        max = d;
      } else {
        if (d < min) min = d;
        if (d > max) max = d;
      }

      count++;
      sum += d;
      sumSqr += d * d;
    }

    // 3. Populate standard 5-element array [count, min, max, sum, sumSqr]
    double[] stats = new double[5];
    stats[0] = count;
    stats[1] = min;
    stats[2] = max;
    stats[3] = sum;
    stats[4] = sumSqr;
    return stats;
  }

  @Override
  public PAOne[] calculatePAOneStats(Attributes atts) {
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.calculatePAOneStats(atts);
    }
    return materialize().calculatePAOneStats(atts);
  }

  @Override
  public double[] calculateStats2(Attributes atts) {
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.calculateStats2(atts);
    }
    return materialize().calculateStats2(atts);
  }

  @Override
  public double calculateMedian(Attributes atts) {
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.calculateMedian(atts);
    }
    return materialize().calculateMedian(atts);
  }

  @Override
  public PrimitiveArray simplify(String colName) {
    return materialize().simplify(colName);
  }

  @Override
  public int switchFakeMissingValueToNaN(double fakeMissingValue) {
    return materialize().switchFakeMissingValueToNaN(fakeMissingValue);
  }

  @Override
  public PrimitiveArray makeIndices(IntArray indices) {
    return materialize().makeIndices(indices);
  }

  @Override
  public int switchFromTo(String from, String to) {
    return materialize().switchFromTo(from, to);
  }

  @Override
  public int firstTie() {
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.firstTie();
    }
    for (int i = 1; i < size; i++) {
      if (compare(i - 1, i) == 0) {
        return i - 1;
      }
    }
    return -1;
  }

  @Override
  public int[] getNMinMaxIndex() {
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getNMinMaxIndex();
    }
    int nValid = 0;
    int minIndex = -1;
    int maxIndex = -1;

    for (int i = 0; i < size; i++) {
      if (isMissingValue(i)) continue;
      nValid++;
      if (minIndex == -1) {
        minIndex = i;
        maxIndex = i;
      } else {
        if (compare(i, minIndex) < 0) minIndex = i;
        if (compare(i, maxIndex) > 0) maxIndex = i;
      }
    }
    return new int[] {nValid, minIndex, maxIndex};
  }

  @Override
  @SuppressWarnings("ReferenceEquality")
  public String testEquals(Object other) {
    if (this == other) return "";
    if (other == null || !(other instanceof PrimitiveArray otherPA)) {
      return "The other object is null or not a PrimitiveArray.";
    }
    if (elementType() != otherPA.elementType()) {
      return "The other PrimitiveArray is a different type ("
          + otherPA.elementType()
          + " != "
          + elementType()
          + ").";
    }
    if (size != otherPA.size()) {
      return String2.ERROR
          + " in PrimitiveView.testEquals: size ("
          + size
          + ") != other.size ("
          + otherPA.size()
          + ").";
    }
    for (int i = 0; i < size; i++) {
      if (compare(i, otherPA, i) != 0) {
        return "The PrimitiveArrays differ at ["
            + i
            + "] ("
            + getRawestString(i)
            + " != "
            + otherPA.getRawestString(i)
            + ").";
      }
    }
    return "";
  }
}
