public class PrimitiveArray {
    /**
   * This makes a PrimitiveArray by reading contiguous values from a RandomAccessFile. This doesn't
   * work for PAType.STRING. endIndex-startIndex must be less than Integer.MAX_VALUE.
   *
   * @param raf the RandomAccessFile
   * @param type the elementType of the original PrimitiveArray
   * @param start the raf offset of the start of the array (nBytes)
   * @param startIndex the index of the desired value (0..)
   * @param endIndex the index after the last desired value (0..)
   * @return a PrimitiveArray
   * @throws Exception if trouble
   */
  public static PrimitiveArray rafFactory(
    RandomAccessFile raf, PAType type, long start, long startIndex, long endIndex)
    throws Exception {

  long longN = endIndex - startIndex;
  String cause = "PrimitiveArray.rafFactory";
  Math2.ensureArraySizeOkay(longN, cause);
  int n = (int) longN;
  PrimitiveArray pa = factory(type, n, true); // active?
  raf.seek(start + pa.elementSize() * startIndex);

  // byte
  if (type == PAType.BYTE) {
    byte tar[] = ((ByteArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readByte();
    return pa;
  }

  // ubyte
  if (type == PAType.UBYTE) {
    byte tar[] = ((UByteArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readByte();
    return pa;
  }

  // short
  if (type == PAType.SHORT) {
    short tar[] = ((ShortArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readShort();
    return pa;
  }

  // ushort
  if (type == PAType.USHORT) {
    short tar[] = ((UShortArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readShort();
    return pa;
  }

  // int
  if (type == PAType.INT) {
    int tar[] = ((IntArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readInt();
    return pa;
  }

  // uint
  if (type == PAType.UINT) {
    int tar[] = ((UIntArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readInt();
    return pa;
  }

  // long
  if (type == PAType.LONG) {
    long tar[] = ((LongArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readLong();
    return pa;
  }

  // ulong
  if (type == PAType.ULONG) {
    long tar[] = ((ULongArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readLong();
    return pa;
  }

  // char
  if (type == PAType.CHAR) {
    char tar[] = ((CharArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readChar();
    return pa;
  }

  // double
  if (type == PAType.DOUBLE) {
    double tar[] = ((DoubleArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readDouble();
    return pa;
  }

  // float
  if (type == PAType.FLOAT) {
    float tar[] = ((FloatArray) pa).array;
    for (int i = 0; i < n; i++) tar[i] = raf.readFloat();
    return pa;
  }

  // no support for String
  throw new Exception("PrimitiveArray.rafFactory type '" + type + "' not supported.");
}

/**
   * Given a sorted (plain or sortIgnoreCase) PrimitiveArray List, this counts adjacent identical
   * rows.
   *
   * @param logDuplicates if true, this prints duplicates to String2.log
   * @return the number of duplicates. Specifically, this is the number that would be removed by
   *     removeDuplicates.
   */
  public int countDuplicates(boolean logDuplicates, boolean isEpochSeconds) {

    int nRows = size;
    if (nRows <= 1) return 0;
    int nDuplicates = 0;
    for (int row = 1; row < nRows; row++) { // start at 1; compare to previous row
      // does it equal row above?
      if (compare(row - 1, row) == 0) {
        ++nDuplicates;
        if (logDuplicates) {
          String s =
              isEpochSeconds
                  ? Calendar2.safeEpochSecondsToIsoStringTZ(getDouble(row), "NaN")
                  : getString(row);
          String2.log(
              "  duplicate #" + nDuplicates + ": [" + (row - 1) + "] and [" + row + "] = " + s);
        }
      }
    }
    return nDuplicates;
  }

  /**
   * This compares this PrimitiveArray's values to anothers, string representation by string
   * representation, and throws Exception if different. this.get(i)=null and other.get(i)==null is
   * treated as same value.
   *
   * @param other
   * @throws Exception if different
   */
  public void diff(PrimitiveArray other) throws Exception {
    int diffi = diffIndex(other);
    if (diffi == -1) return;
    String s1 = diffi == size ? null : getString(diffi);
    String s2 = diffi == other.size() ? null : other.getString(diffi);
    if (!Test.equal(s1, s2))
      throw new RuntimeException(
          String2.ERROR + ": The PrimitiveArrays differ at [" + diffi + "]:\n" + s1 + "\n" + s2);
  }
}
