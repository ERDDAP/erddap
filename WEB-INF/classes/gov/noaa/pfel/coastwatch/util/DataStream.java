/* 
 * DataStream Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;


/**
 * This class contains static methods related to DataInputStream
 * and DataOutputStream.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-05-18
 *
 */
public class DataStream  {

    /** 
     * This creates a buffered file DataOutputStream for writing
     * data to a file.
     *
     * @param fullFileName
     * @return a buffered file dataOutputStream
     */
    public static DataOutputStream getDataOutputStream(String fullFileName) throws Exception {
        return new DataOutputStream(new BufferedOutputStream(
            new FileOutputStream(fullFileName)));
    }

    /** 
     * This creates a buffered file DataInputStream for reading data
     * from a file.
     *
     * @param fullFileName
     * @return a buffered file dataInputStream
     */
    public static DataInputStream getDataInputStream(String fullFileName) throws Exception {
        return new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullFileName)));
    }

    /**
     * Read a short from the stream.
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 2 bytes
     * @return a short
     * @throws exception if trouble
     */
    public static short readShort(boolean littleEndian, DataInputStream stream, 
            byte[] buffer) throws Exception {
        if (littleEndian) {
            //stream.readFully(buffer, 0, 2);
            //return (short)(((buffer[1]) << 8) | (buffer[0] & 255));
            return Short.reverseBytes(stream.readShort());
        } else return stream.readShort();
    }


    /**
     * Write a short to the stream.
     *
     * @param littleEndian if true, the component bytes are written
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be written to
     * @param value the value to be written (the low 16 bits)
     * @throws exception if trouble
     */
    public static void writeShort(boolean littleEndian, 
            DataOutputStream stream, int value) throws Exception {
        if (littleEndian) {
            stream.write(value); //write() writes low 8 bits of value
            stream.write(value >> 8);
        } else stream.writeShort(value);
    }

    /**
     * Read an int from the stream.
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 4 bytes
     * @return an int
     * @throws exception if trouble
     */
    public static int readInt(boolean littleEndian, DataInputStream stream, 
            byte[] buffer) throws Exception {
        if (littleEndian) {
            stream.readFully(buffer, 0, 4);
            return (((((buffer[3] << 8) | 
                       (buffer[2] & 255)) << 8) | 
                       (buffer[1] & 255)) << 8) | 
                       (buffer[0] & 255);
        } else return stream.readInt();
    }

    /**
     * Write an int to the stream.
     *
     * @param littleEndian if true, the component bytes are written
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be written to
     * @param value the value to be written
     * @throws exception if trouble
     */
    public static void writeInt(boolean littleEndian, 
            DataOutputStream stream, int value) throws Exception {
        if (littleEndian) {
            stream.write(value); value >>= 8; //write(value) writes low 8 bits of value
            stream.write(value); value >>= 8;
            stream.write(value); value >>= 8;
            stream.write(value); 
        } else stream.writeInt(value);
    }

    /**
     * Read a long from the stream.
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 8 bytes
     * @return a long
     * @throws exception if trouble
     */
    public static long readLong(boolean littleEndian, DataInputStream stream, 
            byte[] buffer) throws Exception {
        if (littleEndian) {
            stream.readFully(buffer, 0, 8);
            int i0 = ((((    //do most of the calculations as int (faster)
                (buffer[7] << 8) | 
                (buffer[6] & 255)) << 8) | 
                (buffer[5] & 255)) << 8) | 
                (buffer[4] & 255);
            int i1 = (((( 
                (buffer[3] << 8) | 
                (buffer[2] & 255)) << 8) | 
                (buffer[1] & 255)) << 8) | 
                (buffer[0] & 255);
            return (((long)i0) << 32) | (i1 & 0xFFFFFFFFL); //then do one long calculation
        } else return stream.readLong();
    }

    /**
     * Write a long to the stream.
     *
     * @param littleEndian if true, the component bytes are written
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be written to
     * @param value the value to be written
     * @throws exception if trouble
     */
    public static void writeLong(boolean littleEndian, 
            DataOutputStream stream, long value) throws Exception {
        if (littleEndian) {
            //do most of the calculations as int (faster)
            int i = (int)value; //safe (just lower 32 bits)
            stream.write(i); i >>= 8;  //write(i) writes low 8 bits of i
            stream.write(i); i >>= 8;
            stream.write(i); i >>= 8;
            stream.write(i); 
            i = (int)(value >> 32); //safe (just 32 bits)
            stream.write(i); i >>= 8;
            stream.write(i); i >>= 8;
            stream.write(i); i >>= 8;
            stream.write(i); 
        } else stream.writeLong(value);
    }

    /**
     * Read a float from the stream.
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 4 bytes
     * @return a float
     * @throws exception if trouble
     */
    public static float readFloat(boolean littleEndian, DataInputStream stream, 
            byte[] buffer) throws Exception {
        return littleEndian?
            Float.intBitsToFloat(readInt(littleEndian, stream, buffer)) :
            stream.readFloat();
    }

    /**
     * Write a float from the stream.
     *
     * @param littleEndian if true, the component bytes are written
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be written to
     * @param value the value to be written
     * @throws exception if trouble
     */
    public static void writeFloat(boolean littleEndian, 
            DataOutputStream stream, float value) throws Exception {
        if (littleEndian)
            writeInt(littleEndian, stream, Float.floatToIntBits(value));
        else stream.writeFloat(value);
    }

    /**
     * Read a double from the stream.
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 8 bytes
     * @return a double
     * @throws exception if trouble
     */
    public static double readDouble(boolean littleEndian, DataInputStream stream, 
            byte[] buffer) throws Exception {
        return littleEndian?
            Double.longBitsToDouble(readLong(littleEndian, stream, buffer)) :
            stream.readDouble();
    }

    /**
     * Write a double from the stream.
     *
     * @param littleEndian if true, the component bytes are written
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be written to
     * @param value the value to be written
     * @throws exception if trouble
     */
    public static void writeDouble(boolean littleEndian, 
            DataOutputStream stream, double value) throws Exception {
        if (littleEndian)
            writeLong(littleEndian, stream, Double.doubleToLongBits(value));
        else stream.writeDouble(value);
    }

    /**
     * Read a String from bytes (terminated by #0) from the stream. 
     *
     * @param stream the stream to be read from
     * @throws exception if trouble
     */
    public static String readZString(DataInputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte b = stream.readByte();
        while (b != 0) {
            sb.append((char)b);
            b = stream.readByte();
        }
        return sb.toString();
    }

    /**
     * Write a String to the stream as bytes (and add a terminating #0). 
     *
     * @param stream the stream to be written to
     * @param value the String to be written
     * @throws exception if trouble
     */
    public static void writeZString(DataOutputStream stream, String value) 
            throws Exception {
        for (int i = 0; i < value.length(); i++)
            stream.writeByte(value.charAt(i));
        stream.writeByte(0);
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
        for (int i = 0; i < n; i++)
            a[i] = stream.readByte();
        return a;
    }

    /**
     * Read a short array from the stream. 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 2 bytes
     * @param n the number of shorts to be read
     * @return a short array
     * @throws exception if trouble
     */
    public static short[] readShortArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n) throws Exception {
        short a[] = new short[n];
        for (int i = 0; i < n; i++)
            a[i] = readShort(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read a int array from the stream. 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 4 bytes
     * @param n the number of ints to be read
     * @return an int array
     * @throws exception if trouble
     */
    public static int[] readIntArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n) throws Exception {
        int a[] = new int[n];
        for (int i = 0; i < n; i++)
            a[i] = readInt(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read a long array from the stream. 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 8 bytes
     * @param n the number of longs to be read
     * @return a long array
     * @throws exception if trouble
     */
    public static long[] readLongArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n) throws Exception {
        long a[] = new long[n];
        for (int i = 0; i < n; i++)
            a[i] = readLong(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read a float array from the stream. 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 4 bytes
     * @param n the number of floats to be read
     * @return a float array
     * @throws exception if trouble
     */
    public static float[] readFloatArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n) throws Exception {
        float a[] = new float[n];
        for (int i = 0; i < n; i++)
            a[i] = readFloat(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read a double array from the stream. 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 8 bytes
     * @param n the number of doubles to be read
     * @return a double array
     * @throws exception if trouble
     */
    public static double[] readDoubleArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n) throws Exception {
        double a[] = new double[n];
        for (int i = 0; i < n; i++)
            a[i] = readDouble(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read byte[n1][n2] from the stream 
     * (filling all of a[][0], then all of a[][1], ...). 
     * (This is "column-major order" storage, typical of Fortran,
     * but the reverse of C and Java's "row-major order" storage.)
     *
     * @param stream the stream to be read from
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @return a byte[][]
     * @throws exception if trouble
     */
    public static byte[][] read2DCMByteArray(DataInputStream stream, 
            int n1, int n2) throws Exception {
        byte a[][] = new byte[n1][n2];
        for (int j = 0; j < n2; j++)
            for (int i = 0; i < n1; i++)
                a[i][j] = stream.readByte();
        return a;
    }

    /**
     * Read short[n1][n2] from the stream 
     * (filling all of a[][0], then all of a[][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 2 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @return a short[][]
     * @throws exception if trouble
     */
    public static short[][] read2DCMShortArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2) throws Exception {
        short a[][] = new short[n1][n2];
        for (int j = 0; j < n2; j++)
            for (int i = 0; i < n1; i++)
                a[i][j] = readShort(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read int[n1][n2] from the stream 
     * (filling all of a[][0], then all of a[][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 4 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @return an int[][]
     * @throws exception if trouble
     */
    public static int[][] read2DCMIntArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2) throws Exception {
        int a[][] = new int[n1][n2];
        for (int j = 0; j < n2; j++)
            for (int i = 0; i < n1; i++)
                a[i][j] = readInt(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read long[n1][n2] from the stream 
     * (filling all of a[][0], then all of a[][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 8 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @return a long[][]
     * @throws exception if trouble
     */
     public static long[][] read2DCMLongArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2) throws Exception {
        long a[][] = new long[n1][n2];
        for (int j = 0; j < n2; j++)
            for (int i = 0; i < n1; i++)
                a[i][j] = readLong(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read float[n1][n2] from the stream 
     * (filling all of a[][0], then all of a[][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 4 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @return a float[][]
     * @throws exception if trouble
     */
    public static float[][] read2DCMFloatArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2) throws Exception {
        float a[][] = new float[n1][n2];
        for (int j = 0; j < n2; j++)
            for (int i = 0; i < n1; i++)
                a[i][j] = readFloat(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read double[n1][n2] from the stream 
     * (filling all of a[][0], then all of a[][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 8 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @return a double[][]
     * @throws exception if trouble
     */
    public static double[][] read2DCMDoubleArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2) throws Exception {
        double a[][] = new double[n1][n2];
        for (int j = 0; j < n2; j++)
            for (int i = 0; i < n1; i++)
                a[i][j] = readDouble(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read byte[n1][n2][n3] from the stream 
     * (filling all of a[][][0], then all of a[][][1], ...). 
     *
     * @param stream the stream to be read from
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @param n3 the size of the third dimension
     * @return a byte[][][]
     * @throws exception if trouble
     */
    public static byte[][][] read3DByteArray(DataInputStream stream, 
            int n1, int n2, int n3) throws Exception {
        byte a[][][] = new byte[n1][n2][n3];
        for (int k = 0; k < n3; k++)
            for (int j = 0; j < n2; j++)
                for (int i = 0; i < n1; i++)
                    a[i][j][k] = stream.readByte();
        return a;
    }

    /**
     * Read short[n1][n2][n3] from the stream 
     * (filling all of a[][][0], then all of a[][][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 2 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @param n3 the size of the third dimension
     * @return a short[][][]
     * @throws exception if trouble
     */
    public static short[][][] read3DShortArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2, int n3) throws Exception {
        short a[][][] = new short[n1][n2][n3];
        for (int k = 0; k < n3; k++)
            for (int j = 0; j < n2; j++)
                for (int i = 0; i < n1; i++)
                    a[i][j][k] = readShort(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read int[n1][n2][n3] from the stream 
     * (filling all of a[][][0], then all of a[][][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 4 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @param n3 the size of the third dimension
     * @return an int[][][]
     * @throws exception if trouble
     */
    public static int[][][] read3DIntArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2, int n3) throws Exception {
        int a[][][] = new int[n1][n2][n3];
        for (int k = 0; k < n3; k++)
            for (int j = 0; j < n2; j++)
                for (int i = 0; i < n1; i++)
                    a[i][j][k] = readInt(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read long[n1][n2][n3] from the stream 
     * (filling all of a[][][0], then all of a[][][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 8 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @param n3 the size of the third dimension
     * @return a long[][][]
     * @throws exception if trouble
     */
    public static long[][][] read3DLongArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2, int n3) throws Exception {
        long a[][][] = new long[n1][n2][n3];
        for (int k = 0; k < n3; k++)
            for (int j = 0; j < n2; j++)
                for (int i = 0; i < n1; i++)
                    a[i][j][k] = readLong(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read float[n1][n2][n3] from the stream 
     * (filling all of a[][][0], then all of a[][][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 4 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @param n3 the size of the third dimension
     * @return a float[][][]
     * @throws exception if trouble
     */
    public static float[][][] read3DFloatArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2, int n3) throws Exception {
        float a[][][] = new float[n1][n2][n3];
        for (int k = 0; k < n3; k++)
            for (int j = 0; j < n2; j++)
                for (int i = 0; i < n1; i++)
                    a[i][j][k] = readFloat(littleEndian, stream, buffer);
        return a;
    }

    /**
     * Read double[n1][n2][n3] from the stream 
     * (filling all of a[][][0], then all of a[][][1], ...). 
     *
     * @param littleEndian if true, the component bytes are read
     *    lsb to msb, not the normal Java way (msb to lsb).
     * @param stream the stream to be read from
     * @param buffer a temporary buffer which can be used to read 8 bytes
     * @param n1 the size of the first dimension
     * @param n2 the size of the second dimension
     * @param n3 the size of the third dimension
     * @return a double[][][]
     * @throws exception if trouble
     */
    public static double[][][] read3DDoubleArray(boolean littleEndian, DataInputStream stream, 
            byte[] buffer, int n1, int n2, int n3) throws Exception {
        double a[][][] = new double[n1][n2][n3];
        for (int k = 0; k < n3; k++)
            for (int j = 0; j < n2; j++)
                for (int i = 0; i < n1; i++)
                    a[i][j][k] = readDouble(littleEndian, stream, buffer);
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

    /**
     * This runs a small test of the methods in this class.
     */
    public static void main(String args[]) throws Exception {
        //write bytes to a file
        String fileName = String2.getClassPath() + "TestDataStream";
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
            new FileOutputStream(fileName)));
        byte buffer[] = new byte[128];
        dos.writeByte( 1);
        dos.writeByte(-1);
        DataStream.writeShort(false, dos,  30003);
        DataStream.writeShort(false, dos, -30003);
        DataStream.writeShort(true, dos,  30003);
        DataStream.writeShort(true, dos, -30003);
        DataStream.writeInt(false, dos,  1000000003);
        DataStream.writeInt(false, dos, -1000000003);
        DataStream.writeInt(true, dos,  1000000003);
        DataStream.writeInt(true, dos, -1000000003);
        DataStream.writeLong(false, dos,  1000000003006007L);
        DataStream.writeLong(false, dos, -1000000003006007L);
        DataStream.writeLong(true, dos,  1000000003006007L);
        DataStream.writeLong(true, dos, -1000000003006007L);
        DataStream.writeFloat(false, dos, 123.456e29f);
        DataStream.writeFloat(false, dos, -123.456e29f);
        DataStream.writeFloat(true, dos,  123.456e29f);
        DataStream.writeFloat(true, dos, -123.456e29f);
        DataStream.writeDouble(false, dos,  123.45678987e294);
        DataStream.writeDouble(false, dos, -123.45678987e294);
        DataStream.writeDouble(true, dos,  123.45678987e294);
        DataStream.writeDouble(true, dos, -123.45678987e294);
        String s = "This is a\ntest!";
        DataStream.writeZString(dos, s);
        byte ba[] = {(byte)1, (byte)91, (byte)-5};
        dos.write(ba, 0, ba.length);
        dos.close();

        //read bytes from a file
        String2.log("hex dump of " + fileName + "\n" + File2.hexDump(fileName, 500));

        DataInputStream dis = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fileName)));
        Test.ensureEqual( 1, dis.readByte(), "a");
        Test.ensureEqual(-1, dis.readByte(), "b");
        Test.ensureEqual( 30003, DataStream.readShort(false, dis, buffer), "e");
        Test.ensureEqual(-30003, DataStream.readShort(false, dis, buffer), "f");
        Test.ensureEqual( 30003, DataStream.readShort(true, dis, buffer), "c");
        Test.ensureEqual(-30003, DataStream.readShort(true, dis, buffer), "d");
        Test.ensureEqual( 1000000003, DataStream.readInt(false, dis, buffer), "i");
        Test.ensureEqual(-1000000003, DataStream.readInt(false, dis, buffer), "j");
        Test.ensureEqual( 1000000003, DataStream.readInt(true, dis, buffer), "g");
        Test.ensureEqual(-1000000003, DataStream.readInt(true, dis, buffer), "h");
        Test.ensureEqual( 1000000003006007L, DataStream.readLong(false, dis, buffer), "m");
        Test.ensureEqual(-1000000003006007L, DataStream.readLong(false, dis, buffer), "n");
        Test.ensureEqual( 1000000003006007L, DataStream.readLong(true, dis, buffer), "k");
        Test.ensureEqual(-1000000003006007L, DataStream.readLong(true, dis, buffer), "l");
        Test.ensureEqual( 123.456e29f, DataStream.readFloat(false, dis, buffer), "q");
        Test.ensureEqual(-123.456e29f, DataStream.readFloat(false, dis, buffer), "r");
        Test.ensureEqual( 123.456e29f, DataStream.readFloat(true,  dis, buffer), "o");
        Test.ensureEqual(-123.456e29f, DataStream.readFloat(true,  dis, buffer), "p");
        Test.ensureEqual( 123.45678987e294, DataStream.readDouble(false, dis, buffer), "u");
        Test.ensureEqual(-123.45678987e294, DataStream.readDouble(false, dis, buffer), "v");
        Test.ensureEqual( 123.45678987e294, DataStream.readDouble(true,  dis, buffer), "s");
        Test.ensureEqual(-123.45678987e294, DataStream.readDouble(true,  dis, buffer), "t");
        Test.ensureEqual(s, DataStream.readZString(dis), "w");
        byte ba2[] = new byte[ba.length];
        dis.readFully(ba2, 0, ba.length);
        Test.ensureEqual(ba.length, ba2.length, "x");
        for (int i = 0; i < ba.length; i++) 
            Test.ensureEqual(ba[i], ba2[i], "y" + i);
        dis.close();

        //do speed tests    array vs. individual reads
        //This is important because many methods in this class read several 
        //bytes at a time.
        dos = new DataOutputStream(new BufferedOutputStream(
            new FileOutputStream(fileName)));
        ba2 = new byte[]{1, 27, 86, 44};
        int myByte = 87;
        for (int i = 0; i < 100000; i++) { //warmup
            dos.write(myByte);         
            dos.write(ba2);
        }

        //write individual bytes   typical = 562ms 
        long time = System.currentTimeMillis(); 
        for (int i = 0; i < 4000000; i++) 
            dos.write(myByte);         
        String2.log("4000000 byte writes, time=" + (System.currentTimeMillis() -  time));

        //write individual bytes   typical = 562ms 
        time = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            dos.write(ba2);
        String2.log("1000000 byte[4] writes, time=" + (System.currentTimeMillis() -  time));
        dos.close();

        //delete the temp file
        Test.ensureTrue(File2.delete(fileName),  
            "error while deleting " + fileName);

    }
}
