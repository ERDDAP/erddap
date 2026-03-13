/*
 * DataStream Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class contains static methods related to DataInputStream and DataOutputStream.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-05-18
 */
public class DataStream {

  /**
   * This creates a buffered file DataOutputStream for writing data to a file.
   *
   * @param fullFileName
   * @return a buffered file dataOutputStream
   */
  public static DataOutputStream getDataOutputStream(String fullFileName) throws Exception {
    return new DataOutputStream(
        new BufferedOutputStream(Files.newOutputStream(Paths.get(fullFileName))));
  }

  /**
   * Read a short from the stream.
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 2 bytes
   * @return a short
   * @throws exception if trouble
   */
  public static short readShort(boolean littleEndian, DataInputStream stream, byte[] buffer)
      throws Exception {
    if (littleEndian) {
      // stream.readFully(buffer, 0, 2);
      // return (short)(((buffer[1]) << 8) | (buffer[0] & 255));
      return Short.reverseBytes(stream.readShort());
    } else return stream.readShort();
  }

  /**
   * Read an int from the stream.
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 4 bytes
   * @return an int
   * @throws exception if trouble
   */
  public static int readInt(boolean littleEndian, DataInputStream stream, byte[] buffer)
      throws Exception {
    if (littleEndian) {
      stream.readFully(buffer, 0, 4);
      return (((((buffer[3] << 8) | (buffer[2] & 255)) << 8) | (buffer[1] & 255)) << 8)
          | (buffer[0] & 255);
    } else return stream.readInt();
  }

  /**
   * Read a long from the stream.
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 8 bytes
   * @return a long
   * @throws exception if trouble
   */
  public static long readLong(boolean littleEndian, DataInputStream stream, byte[] buffer)
      throws Exception {
    if (littleEndian) {
      stream.readFully(buffer, 0, 8);
      int i0 =
          (((( // do most of the calculations as int (faster)
                          (buffer[7] << 8) | (buffer[6] & 255))
                          << 8)
                      | (buffer[5] & 255))
                  << 8)
              | (buffer[4] & 255);
      int i1 =
          (((((buffer[3] << 8) | (buffer[2] & 255)) << 8) | (buffer[1] & 255)) << 8)
              | (buffer[0] & 255);
      return (((long) i0) << 32) | (i1 & 0xFFFFFFFFL); // then do one long calculation
    } else return stream.readLong();
  }

  /**
   * Read a float from the stream.
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 4 bytes
   * @return a float
   * @throws exception if trouble
   */
  public static float readFloat(boolean littleEndian, DataInputStream stream, byte[] buffer)
      throws Exception {
    return littleEndian
        ? Float.intBitsToFloat(readInt(littleEndian, stream, buffer))
        : stream.readFloat();
  }

  /**
   * Read a double from the stream.
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 8 bytes
   * @return a double
   * @throws exception if trouble
   */
  public static double readDouble(boolean littleEndian, DataInputStream stream, byte[] buffer)
      throws Exception {
    return littleEndian
        ? Double.longBitsToDouble(readLong(littleEndian, stream, buffer))
        : stream.readDouble();
  }

  /**
   * Read a byte array from the stream.
   *
   * @param stream the stream to be read from
   * @param n the number of bytes to be read
   * @return a byte array
   * @throws exception if trouble
   */
  public static byte[] readByteArray(DataInputStream stream, int n) throws Exception {
    byte a[] = new byte[n];
    for (int i = 0; i < n; i++) a[i] = stream.readByte();
    return a;
  }

  /**
   * Read a int array from the stream.
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 4 bytes
   * @param n the number of ints to be read
   * @return an int array
   * @throws exception if trouble
   */
  public static int[] readIntArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n) throws Exception {
    int a[] = new int[n];
    for (int i = 0; i < n; i++) a[i] = readInt(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read byte[n1][n2] from the stream (filling all of a[][0], then all of a[][1], ...). (This is
   * "column-major order" storage, typical of Fortran, but the reverse of C and Java's "row-major
   * order" storage.)
   *
   * @param stream the stream to be read from
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @return a byte[][]
   * @throws exception if trouble
   */
  public static byte[][] read2DCMByteArray(DataInputStream stream, int n1, int n2)
      throws Exception {
    byte a[][] = new byte[n1][n2];
    for (int j = 0; j < n2; j++) for (int i = 0; i < n1; i++) a[i][j] = stream.readByte();
    return a;
  }

  /**
   * Read short[n1][n2] from the stream (filling all of a[][0], then all of a[][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 2 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @return a short[][]
   * @throws exception if trouble
   */
  public static short[][] read2DCMShortArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2)
      throws Exception {
    short a[][] = new short[n1][n2];
    for (int j = 0; j < n2; j++)
      for (int i = 0; i < n1; i++) a[i][j] = readShort(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read int[n1][n2] from the stream (filling all of a[][0], then all of a[][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 4 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @return an int[][]
   * @throws exception if trouble
   */
  public static int[][] read2DCMIntArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2)
      throws Exception {
    int a[][] = new int[n1][n2];
    for (int j = 0; j < n2; j++)
      for (int i = 0; i < n1; i++) a[i][j] = readInt(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read long[n1][n2] from the stream (filling all of a[][0], then all of a[][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 8 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @return a long[][]
   * @throws exception if trouble
   */
  public static long[][] read2DCMLongArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2)
      throws Exception {
    long a[][] = new long[n1][n2];
    for (int j = 0; j < n2; j++)
      for (int i = 0; i < n1; i++) a[i][j] = readLong(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read float[n1][n2] from the stream (filling all of a[][0], then all of a[][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 4 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @return a float[][]
   * @throws exception if trouble
   */
  public static float[][] read2DCMFloatArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2)
      throws Exception {
    float a[][] = new float[n1][n2];
    for (int j = 0; j < n2; j++)
      for (int i = 0; i < n1; i++) a[i][j] = readFloat(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read double[n1][n2] from the stream (filling all of a[][0], then all of a[][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 8 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @return a double[][]
   * @throws exception if trouble
   */
  public static double[][] read2DCMDoubleArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2)
      throws Exception {
    double a[][] = new double[n1][n2];
    for (int j = 0; j < n2; j++)
      for (int i = 0; i < n1; i++) a[i][j] = readDouble(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read byte[n1][n2][n3] from the stream (filling all of a[][][0], then all of a[][][1], ...).
   *
   * @param stream the stream to be read from
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @param n3 the size of the third dimension
   * @return a byte[][][]
   * @throws exception if trouble
   */
  public static byte[][][] read3DByteArray(DataInputStream stream, int n1, int n2, int n3)
      throws Exception {
    byte a[][][] = new byte[n1][n2][n3];
    for (int k = 0; k < n3; k++)
      for (int j = 0; j < n2; j++) for (int i = 0; i < n1; i++) a[i][j][k] = stream.readByte();
    return a;
  }

  /**
   * Read short[n1][n2][n3] from the stream (filling all of a[][][0], then all of a[][][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 2 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @param n3 the size of the third dimension
   * @return a short[][][]
   * @throws exception if trouble
   */
  public static short[][][] read3DShortArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2, int n3)
      throws Exception {
    short a[][][] = new short[n1][n2][n3];
    for (int k = 0; k < n3; k++)
      for (int j = 0; j < n2; j++)
        for (int i = 0; i < n1; i++) a[i][j][k] = readShort(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read int[n1][n2][n3] from the stream (filling all of a[][][0], then all of a[][][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 4 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @param n3 the size of the third dimension
   * @return an int[][][]
   * @throws exception if trouble
   */
  public static int[][][] read3DIntArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2, int n3)
      throws Exception {
    int a[][][] = new int[n1][n2][n3];
    for (int k = 0; k < n3; k++)
      for (int j = 0; j < n2; j++)
        for (int i = 0; i < n1; i++) a[i][j][k] = readInt(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read long[n1][n2][n3] from the stream (filling all of a[][][0], then all of a[][][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 8 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @param n3 the size of the third dimension
   * @return a long[][][]
   * @throws exception if trouble
   */
  public static long[][][] read3DLongArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2, int n3)
      throws Exception {
    long a[][][] = new long[n1][n2][n3];
    for (int k = 0; k < n3; k++)
      for (int j = 0; j < n2; j++)
        for (int i = 0; i < n1; i++) a[i][j][k] = readLong(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read float[n1][n2][n3] from the stream (filling all of a[][][0], then all of a[][][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 4 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @param n3 the size of the third dimension
   * @return a float[][][]
   * @throws exception if trouble
   */
  public static float[][][] read3DFloatArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2, int n3)
      throws Exception {
    float a[][][] = new float[n1][n2][n3];
    for (int k = 0; k < n3; k++)
      for (int j = 0; j < n2; j++)
        for (int i = 0; i < n1; i++) a[i][j][k] = readFloat(littleEndian, stream, buffer);
    return a;
  }

  /**
   * Read double[n1][n2][n3] from the stream (filling all of a[][][0], then all of a[][][1], ...).
   *
   * @param littleEndian if true, the component bytes are read lsb to msb, not the normal Java way
   *     (msb to lsb).
   * @param stream the stream to be read from
   * @param buffer a temporary buffer which can be used to read 8 bytes
   * @param n1 the size of the first dimension
   * @param n2 the size of the second dimension
   * @param n3 the size of the third dimension
   * @return a double[][][]
   * @throws exception if trouble
   */
  public static double[][][] read3DDoubleArray(
      boolean littleEndian, DataInputStream stream, byte[] buffer, int n1, int n2, int n3)
      throws Exception {
    double a[][][] = new double[n1][n2][n3];
    for (int k = 0; k < n3; k++)
      for (int j = 0; j < n2; j++)
        for (int i = 0; i < n1; i++) a[i][j][k] = readDouble(littleEndian, stream, buffer);
    return a;
  }

  /**
   * This always skips nBytes (not "up to nbytes" like skip and skipBytes).
   *
   * @param stream the stream to be read from
   * @param nBytes the number of bytes to be skipped
   * @throws exception if trouble
   */
  public static void fullySkip(DataInputStream stream, int nBytes) throws Exception {
    while (nBytes > 0) nBytes -= stream.skipBytes(nBytes);
  }
}
