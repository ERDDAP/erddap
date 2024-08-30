/*
 * Matlab Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.DataStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * This class contains constants related to reading and writing Matlab files.
 *
 * <p>See "MATLAB The Language of Technical Computing, MAT-File Format, Version 7", which is
 * available from www.matworks.com. Bob has a printout. Note that Matlab files don't have global or
 * variable attributes.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-05-18
 */
public class Matlab {

  /* Useful commands in Matlab/Octave:
  //Matlab is case-sensitive
  //; at end of line suppresses feedback
  //type variable's name to print the variable's value
  //Matrix is 2D: [rows,columns]   (Matlab likes these a lot.)
  //Array is any number of dimensions.
  load 'c:\temp\temp.mat'
  load('c:\temp\temp.mat')

  //make type-specific vectors
  //definition makes row vector; ' transposes it to column vector
  //Different data types can be used, but only doubles can be used for some calculations.
  ints = int32([1, 2, 3])'
  floats = single([1.1, 2.2, 3.3])'
  strings = char('a', 'bb', 'ccc')'
  doubles = [1.111, 2.222, 3.333]'

  //transpose
  ints = ints'

  //make a structure
  sst2.ints = ints
  sst2.floats = floats
  sst2.strings = strings
  sst2.doubles = doubles

  //list fields in a structure
  fieldnames(sst2)

  save 'c:\temp\sst2.mat' sst2

  //scatter plot
  plot(double(ints), double(floats))
  xlabel('My X Axis')
  ylabel('My Y Axis')
  title('My Title')

  //surface plot    numbers at end set range (no effect in Octave?); 'set' makes it upright
  //  squeeze removes dimensions of size=1
  imagesc(AG_ssta_8day.longitude, AG_ssta_8day.latitude, squeeze(AG_ssta_8day.AGssta), [0 35])
  set(gca, 'YDir', 'normal')

  //remove trailing spaces
  deblank(strings(1,:))

  //remove a variable from user space
  clear sst2

  */

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /** Data Types: see MAT File Format, Ver 7, Table 1-1, pg 1-9 */
  public static final int miINT8 = 1;

  public static final int miUINT8 = 2;
  public static final int miINT16 = 3;
  public static final int miUINT16 = 4;
  public static final int miINT32 = 5;
  public static final int miUINT32 = 6;
  public static final int miSINGLE = 7;
  public static final int miDOUBLE = 9;
  public static final int miINT64 = 12;
  public static final int miUINT64 = 13;
  public static final int miMATRIX = 14;
  public static final int miCOMPRESSED = 15;
  public static final int miUTF8 = 16;
  public static final int miUTF16 = 17;
  public static final int miUTF32 = 18;

  /** The number of bytes occupied by each of the mi types (0 if not applicable). */
  public static final int miNBytes[] = {
    0, 1, 1, 2, 2, 4, 4, 4, 0, 8, 0, 0, 8, 8, 0, 0, 1, 2, 4
  }; // I am not sure if UTF8=1 is appropriate

  /** The name associated with the mi constants. */
  public static final String[] miNames = {
    "mi0", "miINT8", "miUINT8", "miINT16", "miUINT16", "miINT32", "miUINT32",
    "miSINGLE", "mi8", "miDOUBLE", "mi10", "mi11", "miINT64", "miUINT64",
    "miMATRIX", "miCOMPRESSED", "miUTF8", "miUTF16", "miUTF32"
  };

  /** ArrayTypes: see MAT File Format, Ver 7, Table 1-3, pg 1-17 */
  public static final int mxCELL_CLASS = 1;

  public static final int mxSTRUCT_CLASS = 2;
  public static final int mxOBJECT_CLASS = 3;
  public static final int mxCHAR_CLASS = 4;
  public static final int mxSPARSE_CLASS = 5;
  public static final int mxDOUBLE_CLASS = 6;
  public static final int mxSINGLE_CLASS = 7;
  public static final int mxINT8_CLASS = 8;
  public static final int mxUINT8_CLASS = 9;
  public static final int mxINT16_CLASS = 10;
  public static final int mxUINT16_CLASS = 11;
  public static final int mxINT32_CLASS = 12;
  public static final int mxUINT32_CLASS = 13;
  public static final int mxINT64_CLASS = 14;
  public static final int mxUINT64_CLASS = 15;

  /** The name associated with the mx constants. */
  public static final String[] mxNames = {
    "mx0",
    "mxCELL_CLASS",
    "mxSTRUCT_CLASS",
    "mxOBJECT_CLASS",
    "mxCHAR_CLASS",
    "mxSPARSE_CLASS",
    "mxDOUBLE_CLASS",
    "mxSINGLE_CLASS",
    "mxINT8_CLASS",
    "mxUINT8_CLASS",
    "mxINT16_CLASS",
    "mxUINT16_CLASS",
    "mxINT32_CLASS",
    "mxUINT32_CLASS",
    "mxINT64_CLASS",
    "mxUINT64_CLASS"
  };

  /**
   * This writes a Matlab header to the file. Page number references are for MAT File Format, Ver 7.
   *
   * @param stream the stream for the Matlab file
   * @throws Exception if trouble
   */
  public static void writeMatlabHeader(DataOutputStream stream) throws Exception {
    // write the header text field (116 bytes, padded with spaces)  (see pg 1-6)
    // e.g., MATLAB 5.0 MAT-file, Platform: GLNX86, Created on: Tue Feb 15 01:19:17 2005
    if (verbose) String2.log("writeMatlabHeader");
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
    StringBuilder sb =
        new StringBuilder(
            "MATLAB 5.0 MAT-file, Created by: gov.noaa.pfel.coastwatch.Matlab, Created on: "
                + sdf.format(date));
    if (verbose) String2.log("header=" + sb);
    while (sb.length() < 116) // pad with spaces
    sb.append(' ');
    stream.write(String2.toByteArray(sb.toString()));

    // write header subsystem data offset field (pg 1-6)  (may be all 0's or all spaces)
    stream.writeLong(0);

    // write version -- always 0x0100 (pg 1-7)
    stream.writeShort((short) 0x0100);

    // write endian indicator
    stream.writeShort((short) 0x4D49); // corresponding to characters: MI
  }

  /**
   * This writes a double array to the Matlab. The array is actually written as a double[1][], since
   * Matlab requires a matrix to have at least 2 dimensions and wants first dim to be 1. Page number
   * references are for MAT File Format, Ver 7.
   *
   * @param stream the destination stream
   * @param name the name for the matrix
   * @param da a double[]
   * @throws Exception if trouble
   */
  public static void writeDoubleArray(DataOutputStream stream, String name, double[] da)
      throws Exception {

    writeNDimensionalArray(
        stream, name, PrimitiveArray.factory(da), new NDimensionalIndex(new int[] {1, da.length}));
  }

  /**
   * This writes a float array to the Matlab. The array is actually written as a float[1][], since
   * Matlab requires a matrix to have at least 2 dimensions and wants first dim to be 1. Page number
   * references are for MAT File Format, Ver 7.
   *
   * @param stream the destination stream
   * @param name the name for the matrix
   * @param fa a float[]
   * @throws Exception if trouble
   */
  public static void writeFloatArray(DataOutputStream stream, String name, float[] fa)
      throws Exception {

    writeNDimensionalArray(
        stream, name, PrimitiveArray.factory(fa), new NDimensionalIndex(new int[] {1, fa.length}));
  }

  /**
   * This writes a double[row][col] as a "matrix" to the Matlab stream. All of the da[][0] is
   * written, then da[][1], ... (This is "column-major order" storage, typical of Fortran, but the
   * reverse of C and Java's "row-major order" storage.) Page number references are for MAT File
   * Format, Ver 7. The size of the sub arrays (da[0], da[1], ...) must all be equal!
   *
   * @param stream the stream for the Matlab file
   * @param name the name for the matrix
   * @param da a double[row][col]
   * @throws Exception if trouble
   */
  public static void write2DDoubleArray(DataOutputStream stream, String name, double[][] da)
      throws Exception {

    // write the dataType and nBytes
    int nameLength8 = 8 * (name.length() / 8 + (name.length() % 8 == 0 ? 0 : 1));
    int nRows = da.length;
    int nCols = da[0].length;
    int dataSize = 8;
    stream.writeInt(miMATRIX); // dataType
    stream.writeInt(16 + 16 + 8 + nameLength8 + 8 + dataSize * nRows * nCols); // nBytes

    // write array flags sub element  pg 1-17
    stream.writeInt(miUINT32); // dataType
    stream.writeInt(8); // nBytes of data
    stream.writeInt(mxDOUBLE_CLASS); // array flags  (complex/global/logical not set)
    stream.writeInt(0); // reserved; ends on 8 byte boundary

    // write dimensions array sub element  pg 1-17
    stream.writeInt(miINT32); // dataType
    stream.writeInt(8); // nBytes = 2 * 4 byte dimensions
    stream.writeInt(nRows);
    stream.writeInt(nCols); // ends on 8 byte boundary

    // write array name sub element
    stream.writeInt(miINT8); // dataType
    stream.writeInt(name.length()); // nBytes
    for (int i = 0; i < name.length(); i++) stream.write((byte) name.charAt(i));
    for (int i = name.length(); i < nameLength8; i++)
      stream.write(0); // 0 padded to 8 byte boundary

    // write data sub element
    stream.writeInt(miDOUBLE); // dataType
    stream.writeInt(8 * nCols * nRows); // nBytes
    for (int col = 0; col < nCols; col++)
      for (int row = 0; row < nRows; row++)
        stream.writeDouble(da[row][col]); // always ends on 8 byte boundary
  }

  /**
   * This writes a float[row][col] as a "matrix" to the Matlab stream. All of the da[][0] is
   * written, then da[][1], ... (This is "column-major order" storage, typical of Fortran, but the
   * reverse of C and Java's "row-major order" storage.) Page number references are for MAT File
   * Format, Ver 7. The size of the sub arrays (fa[0], fa[1], ...) must all be equal!
   *
   * @param stream the stream for the Matlab file
   * @param name the name for the matrix
   * @param fa a float[row][col]
   * @throws Exception if trouble
   */
  public static void write2DFloatArray(DataOutputStream stream, String name, float[][] fa)
      throws Exception {

    // write the dataType and nBytes
    int nameLength8 = 8 * (name.length() / 8 + (name.length() % 8 == 0 ? 0 : 1));
    int nRows = fa.length;
    int nCols = fa[0].length;
    int dataSize = 4;
    int dataPaddingNBytes = ((nRows * nCols) % 2) * 4; // if odd#, pad with 4 bytes
    stream.writeInt(miMATRIX);
    stream.writeInt(16 + 16 + 8 + nameLength8 + 8 + dataSize * nRows * nCols + dataPaddingNBytes);

    // write array flags sub element  pg 1-17
    stream.writeInt(miUINT32); // dataType
    stream.writeInt(8); // nBytes
    stream.writeInt(mxSINGLE_CLASS); // array flags  (complex/global/logical not set)
    stream.writeInt(0); // reserved; ends on 8 byte boundary

    // write dimensions array sub element  pg 1-17
    stream.writeInt(miINT32); // dataType
    stream.writeInt(8); // nBytes = 2 * four byte dimensions
    stream.writeInt(nRows);
    stream.writeInt(nCols); // ends on 8 byte boundary

    // write array name sub element
    stream.writeInt(miINT8); // dataType
    stream.writeInt(name.length()); // nBytes
    for (int i = 0; i < name.length(); i++) stream.write((byte) name.charAt(i));
    for (int i = name.length(); i < nameLength8; i++)
      stream.write(0); // 0 padded to 8 byte boundary

    // write data sub element
    stream.writeInt(miSINGLE); // dataType
    stream.writeInt(dataSize * nRows * nCols); // nBytes
    for (int col = 0; col < nCols; col++)
      for (int row = 0; row < nRows; row++) stream.writeFloat(fa[row][col]);
    for (int i = 0; i < dataPaddingNBytes; i++) stream.write(0); // 0 padded to 8 byte boundary
  }

  /**
   * This creates the byte[] to write a name (short form if possible)
   *
   * @param name the name for the matrix
   * @return the byte[] to write a name (short form if possible, because Octave requires short form
   *     if eligible)
   * @throws Exception if trouble
   */
  public static byte[] nameInfo(String name) throws Exception {
    if (name.length() > 31) name = name.substring(0, 31); // Matlab's limit   pg 1-30
    int nameLength = name.length();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      DataOutputStream dos = new DataOutputStream(baos);
      try {
        int n0;
        if (nameLength <= 4) { // see 1-20
          // short form
          dos.writeShort(nameLength); // nBytes  see 1-20
          dos.writeShort(miINT8); // dataType
          n0 = 4 - nameLength;
        } else {
          // long form
          dos.writeInt(miINT8); // dataType
          dos.writeInt(nameLength); // nBytes  see 1-20
          int nBytes = Math2.hiDiv(nameLength, 8) * 8; // round up to 8-byte boundary
          n0 = nBytes - nameLength;
        }
        for (int i = 0; i < nameLength; i++) dos.write(name.charAt(i));
        for (int i = 0; i < n0; i++) dos.write(0);
      } finally {
        dos.close();
      }
      return baos.toByteArray();
    } finally {
      baos.close();
    }
  }

  /**
   * This makes 2D NDimensionalIndex from the 1D data (pa.size,1). Special case: for StringArray,
   * make as if 2D charArray: (pa.size(), maxStringLength).
   *
   * @param pa
   * @return an NDimensionalIndex
   */
  public static NDimensionalIndex make2DNDIndex(PrimitiveArray pa) {
    if (pa instanceof StringArray sa) {
      // make ndIndex as if 2DCharArray
      int maxStringLength = sa.maxStringLength();
      // String2.log("maxStringLength = " + maxStringLength);
      return make2DNDIndex(sa.size(), maxStringLength);
    }
    return make2DNDIndex(pa.size());
  }

  /**
   * This makes 2D NDimensionalIndex for a PrimitiveArray other than a StringArray.
   *
   * @param size
   * @return an NDimensionalIndex
   */
  public static NDimensionalIndex make2DNDIndex(int size) {
    return new NDimensionalIndex(
        new int[] {size, 1}); // rows, columns    so n,1 makes a column vector
  }

  /**
   * This makes 2D NDimensionalIndex for a StringArray as if char[][].
   *
   * @param nStrings
   * @param maxStringLength
   * @return an NDimensionalIndex
   */
  public static NDimensionalIndex make2DNDIndex(int nStrings, int maxStringLength) {
    return new NDimensionalIndex(
        new int[] {
          nStrings, // String array order
          Math.max(1, maxStringLength)
        }); // ndIndex doesn't like 0; 1 is fine.
  }

  /**
   * This calculates the size of an nDimensionalArray (not counting the initial 8 bytes for type and
   * size).
   *
   * @param name the name for the matrix (name will be truncated if >31 char)
   * @param pa is a PrimitiveArray
   * @param ndIndex allows access to pa as if from make2DNDIndex().
   * @throws Exception if trouble (e.g., &gt;= Integer.MAX_VALUE bytes)
   */
  public static int sizeOfNDimensionalArray(
      String name, PrimitiveArray pa, NDimensionalIndex ndIndex) throws Exception {

    return sizeOfNDimensionalArray(name, pa.elementType(), ndIndex);
  }

  /**
   * This calculates the size of an nDimensionalArray (not counting the initial 8 bytes for type and
   * size).
   *
   * @param name the name for the matrix (name will be truncated if >31 char)
   * @param type is from pa.getElementType (e.g., PAType.FLOAT or PAType.STRING)
   * @param ndIndex allows nDimensional (at least 2) access to pa as if from make2DNDIndex().
   * @throws Exception if trouble (e.g., &gt;= Integer.MAX_VALUE bytes)
   */
  public static int sizeOfNDimensionalArray(String name, PAType type, NDimensionalIndex ndIndex)
      throws Exception {

    int nDimensions = ndIndex.nDimensions();
    int nElements = (int) ndIndex.size(); // safe since data is in pa, nElements must be an int
    int elementSize = type == PAType.STRING ? 2 : PAType.elementSize(type);
    long nDataBytes = elementSize * (long) nElements;
    long nBytes =
        16
            + // array flags nBytes       see pg 1-20
            Math2.hiDiv(8 + nDimensions * 4, 8) * 8L
            + // dimensions nBytes
            nameInfo(name).length
            + // name nBytes
            8
            + Math2.hiDiv(nDataBytes, 8) * 8L; // data nBytes
    if (nBytes >= Integer.MAX_VALUE)
      throw new RuntimeException(
          "Too much data: Matlab variables must be < Integer.MAX_VALUE bytes.");
    return (int) nBytes; // safe since checked above
  }

  /**
   * This writes an nDimensionalArray as an "miMatrix" to the Matlab stream. E.g., for da[row][col],
   * all of da[][0] is written, then all of da[][1], ... (This is "column-major order" storage,
   * typical of Fortran, but the reverse of C and Java's "row-major order" storage.) Page number
   * references are for MAT File Format, Ver 7.
   *
   * @param stream the stream for the Matlab file
   * @param name the name for the matrix (name will be truncated if >31 char)
   * @param pa is a PrimitiveArray (StringArray will be saved as char[][])
   * @param ndIndex describes access the data structure. It must have at least 2 dimensions, as if
   *     from make2DNDIndex().
   * @throws Exception if trouble
   */
  public static void writeNDimensionalArray(
      DataOutputStream stream, String name, PrimitiveArray pa, NDimensionalIndex ndIndex)
      throws Exception {

    // do the 2 parts:
    int nDataBytes = writeNDimensionalArray1(stream, name, pa.elementType(), ndIndex);
    writeNDimensionalArray2(stream, pa, ndIndex, nDataBytes);
  }

  /**
   * This is the first part of writeNDimensionalArray
   *
   * @return nDataBytes
   * @throws Exception if trouble
   */
  public static int writeNDimensionalArray1(
      DataOutputStream stream, String name, PAType paElementType, NDimensionalIndex ndIndex)
      throws Exception {

    if (name.length() > 31) name = name.substring(0, 31); // Matlab's limit pg 1-30
    byte nameInfo[] = nameInfo(name);

    // ensure charNDIndex was used for StringArrays
    boolean isStringArray = paElementType == PAType.STRING;

    int shape[] = ndIndex.shape();
    int nDimensions = shape.length;
    int elementSize = isStringArray ? 2 : PAType.elementSize(paElementType);
    int nElements = (int) ndIndex.size(); // safe since data is in pa, nElements must be an int
    int arrayType, dataType;
    if (paElementType == PAType.DOUBLE) {
      arrayType = mxDOUBLE_CLASS;
      dataType = miDOUBLE;
    } else if (paElementType == PAType.FLOAT) {
      arrayType = mxSINGLE_CLASS;
      dataType = miSINGLE;
    } else if (paElementType == PAType.LONG) {
      arrayType = mxINT64_CLASS;
      dataType = miINT64;
    } else if (paElementType == PAType.ULONG) {
      arrayType = mxUINT64_CLASS;
      dataType = miUINT64;
    } else if (paElementType == PAType.INT) {
      arrayType = mxINT32_CLASS;
      dataType = miINT32;
    } else if (paElementType == PAType.UINT) {
      arrayType = mxUINT32_CLASS;
      dataType = miUINT32;
    } else if (paElementType == PAType.SHORT) {
      arrayType = mxINT16_CLASS;
      dataType = miINT16;
    } else if (paElementType == PAType.USHORT) {
      arrayType = mxUINT16_CLASS;
      dataType = miUINT16;
    } else if (paElementType == PAType.BYTE) {
      arrayType = mxINT8_CLASS;
      dataType = miINT8;
    } else if (paElementType == PAType.UBYTE) {
      arrayType = mxUINT8_CLASS;
      dataType = miUINT8;
    } else if (paElementType == PAType.CHAR) {
      arrayType = mxCHAR_CLASS;
      dataType = miUINT16;
    } // pg 1-18
    else if (paElementType == PAType.STRING) {
      arrayType = mxCHAR_CLASS;
      dataType = miUINT16;
    } // pg 1-18
    else
      throw new Exception(
          String2.ERROR
              + " in Matlab.writeNDimensionalArray: "
              + "unsupported type="
              + paElementType);

    // write the miMatrix dataType and nBytes
    int nDataBytes = elementSize * nElements;
    stream.writeInt(miMATRIX); // dataType
    stream.writeInt(sizeOfNDimensionalArray(name, paElementType, ndIndex));

    // write array flags sub element  pg 1-17
    stream.writeInt(miUINT32); // dataType=6
    stream.writeInt(8); // fixed nBytes of data
    stream.writeInt(arrayType); // array flags  (complex/global/logical not set)
    stream.writeInt(0); // reserved; ends on 8 byte boundary

    // write dimensions array sub element  pg 1-17
    stream.writeInt(miINT32); // dataType=5
    stream.writeInt(nDimensions * 4); // nBytes
    for (int i = 0; i < nDimensions; i++) stream.writeInt(shape[i]);
    if (Math2.odd(nDimensions)) stream.writeInt(0); // end on 8 byte boundary

    // write array name sub element
    stream.write(nameInfo, 0, nameInfo.length);

    // write data sub element
    // incrementCM (Column Major - data is written with leftmost index varying fastest),
    //  e.g., for [row][column], 'row' varies fastest
    stream.writeInt(dataType); // data's dataType
    stream.writeInt(nDataBytes); // nBytes
    return nDataBytes;
  }

  /**
   * This is the second part of writeNDimensionalArray
   *
   * @throws Exception if trouble
   */
  public static void writeNDimensionalArray2(
      DataOutputStream stream, PrimitiveArray pa, NDimensionalIndex ndIndex, int nDataBytes)
      throws Exception {

    PAType paElementType = pa.elementType();

    if (paElementType == PAType.STRING) {
      // isStringArray, so write strings padded to maxLength
      int n = pa.size();
      int shape[] = ndIndex.shape();
      int longestString = shape[shape.length - 1]; // String array order
      for (int po = 0; po < longestString; po++) {
        for (int row = 0; row < n; row++) {
          String s = pa.getString(row);
          stream.writeChar(po < s.length() ? s.charAt(po) : ' ');
        }
      }
    } else {
      PAOne paOne = new PAOne(paElementType);
      while (ndIndex.incrementCM()) paOne.readFrom(pa, (int) ndIndex.getIndex()).writeToDOS(stream);
    }

    // pad data to 8 byte boundary
    int i = nDataBytes % 8;
    while ((i++ % 8) != 0) stream.write(0); // 0 padded to 8 byte boundary
  }

  /**
   * This is a first stab at reading a matlab file. Currently, this can read files which contain
   * only numeric arrays, and which don't use compression. Page number references are for MAT File
   * Format, Ver 7. First 256 bytes of a sample .mat file: AT2005032_2005045_ssta_westus.mat This
   * initial text is: MATLAB 5.0 MAT-file, Platform: GLNX86, Created on: Tue Feb 15 01:19:17 2005
   *
   * <p>4D 41 54 4C 41 42 20 35 2E 30 20 4D 41 54 2D 66 0 text, space padded 69 6C 65 2C 20 50 6C 61
   * 74 66 6F 72 6D 3A 20 47 16 4C 4E 58 38 36 2C 20 43 72 65 61 74 65 64 20 6F 32 6E 3A 20 54 75 65
   * 20 46 65 62 20 31 35 20 30 31 48 3A 31 39 3A 31 37 20 32 30 30 35 20 20 20 20 20 64 20 20 20 20
   * 20 20 20 20 20 20 20 20 20 20 20 20 80 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 96 20 20
   * 20 20 20 20 20 20 20 20 20 20 00 01 49 4D 112 4 spaces; 8byte sybsysten; 2 byte ver; byte
   * order: I M 0E 00 00 00 38 37 00 00 06 00 00 00 08 00 00 00 128 matrix id; array flags id, 8
   * bytes 06 00 00 00 00 00 00 00 05 00 00 00 08 00 00 00 144 array flags; dimensions id, 8 bytes
   * 01 00 00 00 E1 06 00 00 01 00 03 00 6C 6F 6E 00 160 dim0, dim1; array name id, 3 bytes, short:
   * l o n #0 09 00 00 00 08 37 00 00 00 00 00 00 00 20 6C 40 176 9=miDouble, many bytes; data... 66
   * 66 66 66 66 20 6C 40 CD CC CC CC CC 20 6C 40 33 33 33 33 33 21 6C 40 9A 99 99 99 99 21 6C 40 00
   * 00 00 00 00 22 6C 40 66 66 66 66 66 22 6C 40 CD CC CC CC CC 22 6C 40 33 33 33 33 33 23 6C 40 9A
   *
   * @param fullFileName the full name of the file
   * @return a Vector with the objects from the Matlab file. Currently, this alternates a matrix
   *     name (a String) and an array with the matrix's data.
   * @throws Exception if trouble
   */
  public static Vector readMatlabFile(String fullFileName) throws Exception {

    String methodName = "Matlab.readMatlabFile:\n";

    // open the file
    if (verbose) String2.log("readMatlabFile: " + fullFileName);
    DataInputStream stream =
        new DataInputStream(File2.getDecompressedBufferedInputStream(fullFileName));
    try {
      if (verbose) String2.log("bytes available=" + stream.available());

      // read the header
      byte buffer[] = new byte[256];
      stream.readFully(buffer, 0, 116);
      String headerText = new String(buffer, 0, 116);
      if (verbose) String2.log("headerText=" + headerText);

      // skip the 8 byte subsystem-specific offset
      stream.readFully(buffer, 0, 8);

      // read the version
      short version = stream.readShort(); // when writing, write 0x0100
      if (verbose) String2.log("version (should be 256 here)=" + version);

      // read the endian indicator; if "MI", we need to swap byte order when reading
      String endianChars = (char) stream.readByte() + "" + (char) stream.readByte();
      if (verbose) String2.log("endian chars=" + endianChars);
      boolean littleEndian = endianChars.equals("IM"); // vs "MI"   //is the logic of this correct?
      if (verbose) String2.log("littleEndian=" + littleEndian);

      // read the data
      Vector vector = new Vector();
      while (stream.available() > 0) {
        // read data type (4 bytes)
        int miDataType = DataStream.readInt(littleEndian, stream, buffer);

        // is it a small data element? see pg 1-10 and 1-11
        if (miDataType >> 16 != 0) {
          int smallData = DataStream.readInt(littleEndian, stream, buffer);
          if (verbose)
            String2.log(
                "small data element(nBytes="
                    + (miDataType >> 16)
                    + "): "
                    + miNames[miDataType & 0xFFFF]
                    + " = "
                    + smallData);
          continue;
        }

        // read nBytes
        int nBytes = DataStream.readInt(littleEndian, stream, buffer);
        if (verbose) String2.log("data element: " + miNames[miDataType] + "  nBytes=" + nBytes);

        // read miMATRIX data
        int subMIDataType;
        int subNBytes;
        boolean subSmallDataFormat;
        if (miDataType == miMATRIX) {
          // miMATRIX is the only type where nBytes includes the padding bytes

          // read array flags sub element  pg 1-17
          subMIDataType = DataStream.readInt(littleEndian, stream, buffer);
          Test.ensureEqual(
              subMIDataType,
              miUINT32,
              methodName + "miMATRIX arrayFlags subMIDataType != miUINT32.");
          subNBytes = DataStream.readInt(littleEndian, stream, buffer);
          Test.ensureEqual(subNBytes, 8, methodName + "miMATRIX arrayFlags subNBytes != 8.");
          int arrayFlags0 = DataStream.readInt(littleEndian, stream, buffer);
          int arrayFlags1 =
              DataStream.readInt(
                  littleEndian, stream, buffer); // undefined; this fills to 8 byte boundary
          int mxClass = arrayFlags0 & 255;
          boolean complex = ((arrayFlags0 >> 8) & 8) == 8;
          if (verbose) String2.log("  mxClass=" + mxNames[mxClass] + " complex=" + complex);

          // read dimensions array sub element  pg 1-17
          subMIDataType = DataStream.readInt(littleEndian, stream, buffer);
          Test.ensureEqual(
              subMIDataType, miINT32, methodName + "miMATRIX dimensions subMIDataType != miINT32.");
          subNBytes = DataStream.readInt(littleEndian, stream, buffer);
          int nDim = subNBytes / 4; // 4 bytes per miINT32
          int dim[] = DataStream.readIntArray(littleEndian, stream, buffer, nDim);
          DataStream.fullySkip(stream, (8 - (subNBytes % 8)) % 8); // read to 8 byte boundary
          if (verbose)
            String2.log(
                "  dim="
                    + dim[0]
                    + ", "
                    + dim[1]
                    + (dim.length > 2 ? ", " + dim[2] : "")); // this only support 2 or 3 dimensions

          // read array name sub element
          subMIDataType = DataStream.readInt(littleEndian, stream, buffer);
          subNBytes = subMIDataType >> 16;
          subSmallDataFormat = subNBytes != 0;
          if (subSmallDataFormat) subMIDataType &= 0xFFFF;
          else subNBytes = DataStream.readInt(littleEndian, stream, buffer);
          Test.ensureEqual(
              subMIDataType, miINT8, methodName + "miMATRIX array name subMIDataType != miINT8.");
          String arrayName = new String(DataStream.readByteArray(stream, subNBytes));
          if (subSmallDataFormat)
            DataStream.fullySkip(stream, 4 - subNBytes); // read to 8 byte boundary
          else DataStream.fullySkip(stream, (8 - (subNBytes % 8)) % 8); // read to 8 byte boundary
          vector.add(arrayName);
          if (verbose) String2.log("  arrayName=" + arrayName);

          // read data sub element
          subMIDataType = DataStream.readInt(littleEndian, stream, buffer);
          subNBytes = subMIDataType >> 16;
          subSmallDataFormat = subNBytes != 0;
          if (subSmallDataFormat) subMIDataType &= 0xFFFF;
          else subNBytes = DataStream.readInt(littleEndian, stream, buffer);
          if (verbose)
            String2.log(
                "  data subMIDataType=" + miNames[subMIDataType] + " subNBytes=" + subNBytes);
          if (subMIDataType == miDOUBLE) {
            if (dim.length == 2) {
              double a[][] =
                  DataStream.read2DCMDoubleArray(littleEndian, stream, buffer, dim[0], dim[1]);
              if (verbose)
                String2.log(
                    "  a[0][0]=" + a[0][0] + " a[0][" + (dim[1] - 1) + "]=" + a[0][dim[1] - 1]);
              if (verbose)
                String2.log(
                    "  a["
                        + (dim[0] - 1)
                        + "][0]="
                        + a[dim[0] - 1][0]
                        + " a["
                        + (dim[0] - 1)
                        + "]["
                        + (dim[1] - 1)
                        + "]="
                        + a[dim[0] - 1][dim[1] - 1]);
              vector.add(a);
            } else if (dim.length == 3) {
              double a[][][] =
                  DataStream.read3DDoubleArray(
                      littleEndian, stream, buffer, dim[0], dim[1], dim[2]);
              vector.add(a);
            } else Test.ensureEqual(dim.length, 2, methodName + "miMATRIX dim.length != 2 or 3.");
          } else if (subMIDataType == miSINGLE) {
            if (dim.length == 2) {
              float a[][] =
                  DataStream.read2DCMFloatArray(littleEndian, stream, buffer, dim[0], dim[1]);
              if (verbose)
                String2.log(
                    "  first: a[0][0]="
                        + a[0][0]
                        + " a[0]["
                        + (dim[1] - 1)
                        + "]="
                        + a[0][dim[1] - 1]);
              if (dim[0] > 1)
                if (verbose)
                  String2.log(
                      "  last:  a["
                          + (dim[0] - 1)
                          + "][0]="
                          + a[dim[0] - 1][0]
                          + " a["
                          + (dim[0] - 1)
                          + "]["
                          + (dim[1] - 1)
                          + "]="
                          + a[dim[0] - 1][dim[1] - 1]);
              vector.add(a);
            } else if (dim.length == 3) {
              float a[][][] =
                  DataStream.read3DFloatArray(littleEndian, stream, buffer, dim[0], dim[1], dim[2]);
              vector.add(a);
            } else Test.ensureEqual(dim.length, 2, methodName + "miMATRIX dim.length != 2 or 3.");
          } else if (subMIDataType == miINT64 || subMIDataType == miUINT64) { // trouble
            if (dim.length == 2) {
              long a[][] =
                  DataStream.read2DCMLongArray(littleEndian, stream, buffer, dim[0], dim[1]);
              vector.add(a);
            } else if (dim.length == 3) {
              long a[][][] =
                  DataStream.read3DLongArray(littleEndian, stream, buffer, dim[0], dim[1], dim[2]);
              vector.add(a);
            } else Test.ensureEqual(dim.length, 2, methodName + "miMATRIX dim.length != 2 or 3.");
          } else if (subMIDataType == miINT32 || subMIDataType == miUINT32) { // trouble
            if (dim.length == 2) {
              int a[][] = DataStream.read2DCMIntArray(littleEndian, stream, buffer, dim[0], dim[1]);
              vector.add(a);
            } else if (dim.length == 3) {
              int a[][][] =
                  DataStream.read3DIntArray(littleEndian, stream, buffer, dim[0], dim[1], dim[2]);
              vector.add(a);
            } else Test.ensureEqual(dim.length, 2, methodName + "miMATRIX dim.length != 2 or 3.");
          } else if (subMIDataType == miINT16 || subMIDataType == miUINT16) { // trouble
            if (dim.length == 2) {
              short a[][] =
                  DataStream.read2DCMShortArray(littleEndian, stream, buffer, dim[0], dim[1]);
              vector.add(a);
            } else if (dim.length == 3) {
              short a[][][] =
                  DataStream.read3DShortArray(littleEndian, stream, buffer, dim[0], dim[1], dim[2]);
              vector.add(a);
            } else Test.ensureEqual(dim.length, 2, methodName + "miMATRIX dim.length != 2 or 3.");
          } else if (subMIDataType == miINT8 || subMIDataType == miUINT8) { // trouble
            if (dim.length == 2) {
              byte a[][] = DataStream.read2DCMByteArray(stream, dim[0], dim[1]);
              vector.add(a);
            } else if (dim.length == 3) {
              byte a[][][] = DataStream.read3DByteArray(stream, dim[0], dim[1], dim[2]);
              vector.add(a);
            } else Test.ensureEqual(dim.length, 2, methodName + "miMATRIX dim.length != 2 or 3.");
          } else
            Test.ensureEqual(
                subMIDataType,
                0,
                methodName
                    + "miMATRIX subMIDataType not supported ("
                    + miNames[subMIDataType]
                    + ").");

          if (subSmallDataFormat)
            DataStream.fullySkip(stream, 4 - subNBytes); // read to 8 byte boundary
          else DataStream.fullySkip(stream, (8 - (subNBytes % 8)) % 8); // read to 8 byte boundary

          // read complex data sub element
          Test.ensureEqual(
              complex, false, methodName + "miMATRIX complex data is currently not supported.");

        } else {
          // temp: skip over the data
          if (verbose) String2.log("  skip over that data -- element type not supported yet");
          DataStream.fullySkip(stream, nBytes);
          DataStream.fullySkip(stream, (8 - (nBytes % 8)) % 8); // read to 8 byte boundary
        }
      }

      if (verbose) String2.log("end of Matlab file\n");
      return vector;
    } finally {
      stream.close();
    }
  }
}
