/* 
 * PersistentTable Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * PersistentTable maintains a table of data in a random access file.
 * <br> The number and type of the columns is fixed.
 * <br> The number of rows can be increased.
 * <br> Rows can't be deleted, so define a boolean column to indicate if a row is active
 *   (and to later find inactive rows so they can be reused).
 * <br> Rows and columns are numbered 0.. 
 *
 * <p>The data can be stored as text or in binary format.
 * <br> Text is nice since the file is readable/editable/fixable in a text editor. 
 *   (Make sure line lengths stay the same!)
 * <br> Strings are always converted to utf-8 then stored (so reserve extra space if chars above #128 expected).
 *
 * <p>PersistentTable is NOT synchronized.
 * <br> If more than 1 thread may be working with the table, use 
 * <pre>
    synchronized (persistentTable) {  
        //do several things here
        //...
        //if "rw" mode and you wrote some changes, perhaps use persistentTable.flush() at end
    }
  </pre>
 * That also has the advantage of making a group of changes atomically.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2008-12-02
 */
public class PersistentTable {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 

    /** 
     * When primitives are stored in file, these are the lengths (in bytes). 
     */
    public static int BOOLEAN_LENGTH  = 1;
    public static int BYTE_LENGTH     = 4;
    public static int SHORT_LENGTH    = 6;
    public static int INT_LENGTH      = 11;
    public static int LONG_LENGTH     = 20;
    public static int FLOAT_LENGTH    = 16; //I think 15 (e.g., -1.09464165E-11), but add 1 for safety
    public static int DOUBLE_LENGTH   = 25; //I think 24 (e.g,. -2.4353007519111625E-151), but add 1 for safety

    public static int BINARY_BYTE_LENGTH   = 1;
    public static int BINARY_CHAR_LENGTH   = 2;
    public static int BINARY_SHORT_LENGTH  = 2;
    public static int BINARY_INT_LENGTH    = 4;
    public static int BINARY_LONG_LENGTH   = 8;
    public static int BINARY_FLOAT_LENGTH  = 4; 
    public static int BINARY_DOUBLE_LENGTH = 8; 

    //set once
    private String fullFileName;
    private RandomAccessFile raf;
    private int columnWidths[];
    private int columnStartAt[];
    private int nBytesPerRow;

    //changes 
    private int nRows = 0;

    /** 
     * The constructor. 
     *
     * @param fullFileName if it exists, the data will be used. If not, it will be created.
     * @param mode This must be one of<ul>
     *   <li> "r" Open for reading only. Invoking any of the write methods of the 
     *     resulting object will cause an IOException to be thrown. 
     *   <li> "rw" Open for reading and writing. If the file does not already exist 
     *     then an attempt will be made to create it. 
     *   <li> "rws" Open for reading and writing, as with "rw", and also require 
     *     that every update to the file's content or metadata be written synchronously 
     *     to the underlying storage device.   
     *   <li> "rwd" Open for reading and writing, as with "rw", and also require 
     *     that every update to the file's content be written synchronously to the 
     *     underlying storage device. 
     *   </ul>
     *   See http://docs.oracle.com/javase/7/docs/api/java/io/RandomAccessFile.html#mode
     *   <br> Reading data is equally fast in all modes.
     *   <br> Writing data in "rw" is 10X to 40X faster than "rws" and "rwd" modes.
     *   <br> For writing data in "rw" mode, text and binary methods are equally fast (text is perhaps slightly faster!).
     *   <br> For writing data in "rws" and "rwd" modes, text is 5X to 10X FASTER than binary (!!!).
     *   <br> String read/write is surprisingly fast in all modes.
     *   <br> Using "rw" and flush() after each group of writes is slower than rw, 
     *      but faster than rws and rwd, and closest to making groups of action atomic.
     *   <br> Advice: if file integrity is very important, use "rw"+flush or "rws".
     * @param columnWidths (in bytes) For numeric columns, use the XXX_LENGTH constants.
     *   <br>For Strings, use whatever value you want.
     *    Strings are always converted to utf-8 then stored, so reserve extra space if chars above #128 expected.
     */
    public PersistentTable(String fullFileName, String mode, int columnWidths[]) throws IOException {
        this.fullFileName = fullFileName;
        this.columnWidths = columnWidths;
        columnStartAt = new int[columnWidths.length];
        nBytesPerRow = 0;
        for (int c = 0; c < columnWidths.length; c++) {
            columnStartAt[c] = nBytesPerRow;
            nBytesPerRow += columnWidths[c];
        }
        nBytesPerRow++; //for newline automatically added to end of row

        //open the file;   
        raf = new RandomAccessFile(fullFileName, mode); 
        long longNRows = raf.length() / nBytesPerRow; //integer division causes partial row at end to be ignored 
        EDStatic.ensureArraySizeOkay(longNRows, "PersistentTable");
        nRows = (int)longNRows; //save since checked above
        if (verbose) String2.log("PersistentTable " + fullFileName + " is open.\n  mode=" + mode + " nRows=" + nRows);
    }

    /** 
     * This flushes and closes the file.
     * You don't have to do this -- it will be done automatically when a program shuts down.
     * Future attempts to read/write will throw null pointer exceptions. 
     */
    public void close() throws IOException {
        if (raf != null) {
            flush();
            raf.close();
            raf = null;
        }
    }

    /** 
     * If mode="rw", this flushes the all pending writes to the file.
     * For "rws" and "rwd", flushing is done automatically after every write.
     * This is never needed for reads.
     */
    public void flush() throws IOException {
        raf.getChannel().force(true);
    }

    /** 
     * Users of this class shouldn't call this -- use close() instead.
     * Java calls this when an object is no longer used, just before garbage collection. 
     * 
     */
    protected void finalize() throws Throwable {
        try {  //extra insurance
            close();
        } catch (Throwable t) {
        }
        super.finalize();
    }
    

    /** Returns the current number of rows. */
    public int nRows() {
        return nRows; 
    }

    /**
     * This adds n empty rows to the end of the file.
     * The row is initialized with spaces, which is fine for the 
     *   write...asText methods, but odd for writeBinary... methods.
     * This automatically adds a newline at end of each row.
     * 
     * @param n the number of rows to be added
     * @return the number of rows after the rows are added
     */
    public int addRows(int n) throws IOException {
        raf.seek(nRows * nBytesPerRow);
        byte ar[] = new byte[nBytesPerRow];
        Arrays.fill(ar, (byte)32);
        ar[nBytesPerRow - 1] = (byte)'\n';
        for (int i = 0; i < n; i++) {
            raf.write(ar);
            nRows++;
        }
        return nRows;
    }

    /**
     * This clears (fills with spaces) an existing row.
     * 
     * @param row
     * @return the number of rows after the rows are added
     */
    public void clearRow(int row) throws IOException {
        if (row < 0 || row >= nRows) 
            throw new RuntimeException("row=" + row + " must be between 0 and " + (nRows - 1));
        raf.seek(row * nBytesPerRow);
        byte ar[] = new byte[nBytesPerRow - 1]; //-1 since \n won't be changed
        Arrays.fill(ar, (byte)32);
        raf.write(ar);
    }

    /** 
     * This writes a value (stored as a byte ar) into the file.
     * All text writes are funnelled through this method.
     *
     * @throws RuntimeException if ar.length != columnWidths[col] 
     *   or col is invalid or row is invalid.
     */
    public void write(int col, int row, byte ar[]) throws IOException {
        if (ar.length != columnWidths[col])
            throw new RuntimeException("PersistentTable Error: Byte array length=" + ar.length + 
                " doesn't match columnWidth[" + col + "]=" + columnWidths[col] + "."); 
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.write(ar);
    }

    /** 
     * This writes a string value to the file
     * The string UTF-8 encoded then stored, so the byteArray.length may be greater
     * than s.length(), so make the column wider to be safe.
     *
     * @param s a String.  
     *   If s is too long, it is truncated. If too short, it is space-padded at the end.
     * @param length 
     * @return the corresponding byte[] (or null if s is null)
     */
    public void writeString(int col, int row, String s) throws IOException {
        int colWidth = columnWidths[col];
        if (s.length() > colWidth)
            s = s.substring(0, colWidth);
        byte ar[] = String2.getUTF8Bytes(s);
        //truncating is tricky because don't want to have 1/2 of a 2-byte char
        while (ar.length > colWidth) {
            if (reallyVerbose) String2.log("s=" + String2.annotatedString(s) + " will be shortened by 1 char.");
            s = s.substring(0, s.length() - 1); //remove last byte
            ar = String2.getUTF8Bytes(s);
        }
        if (ar.length < colWidth) {
            byte tar[] = new byte[colWidth];
            System.arraycopy(ar, 0, tar, 0, ar.length);
            Arrays.fill(tar, ar.length, colWidth, (byte)' ');
            ar = tar;
        }
        write(col, row, ar);
    }

    /**
     * This converts the boolean to text (T|F) and then to byte[BOOLEAN_LENGTH] then writes it to the file. 
     * Later, use read to read the value from the file.
     */
    public void writeBoolean(int col, int row, boolean b) throws IOException {
        write(col, row, b? new byte[]{(byte)'T'} : new byte[]{(byte)'F'});
    }

    /**
     * This converts the byte to text and then to byte[BYTE_LENGTH] then writes it to the file. 
     * Later, use readByte to read the value from the file.
     */
    public void writeByte(int col, int row, byte b) throws IOException {
        write(col, row, String2.toByteArray(String2.right("" + b, BYTE_LENGTH)));
    }

    /**
     * This converts the short to text and then to byte[SHORT_LENGTH] then writes it to the file. 
     * Later, use readShort to read the value from the file.
     */
    public void writeShort(int col, int row, short s) throws IOException {
        write(col, row, String2.toByteArray(String2.right("" + s, SHORT_LENGTH)));
    }

    /**
     * This converts the int to text and then to byte[INT_LENGTH] then writes it to the file. 
     * Later, use readInt to read the value from the file.
     */
    public void writeInt(int col, int row, int i) throws IOException {
        write(col, row, String2.toByteArray(String2.right("" + i, INT_LENGTH)));
    }

    /**
     * This converts the long to text and then to byte[LONG_LENGTH] then writes it to the file. 
     * Later, use readLong to read the value from the file.
     */
    public void writeLong(int col, int row, long i) throws IOException {
        write(col, row, String2.toByteArray(String2.right("" + i, LONG_LENGTH)));
    }

    /**
     * This converts the float to text and then to byte[FLOAT_LENGTH] then writes it to the file. 
     * Later, use readFloat to read the value from the file.
     */
    public void writeFloat(int col, int row, float f) throws IOException {
        write(col, row, String2.toByteArray(String2.right("" + f, FLOAT_LENGTH)));
    }

    /**
     * This converts the double to text and then to byte[DOUBLE_LENGTH] then writes it to the file. 
     * Later, use readDouble to read the value from the file.
     */
    public void writeDouble(int col, int row, double d) throws IOException {
        write(col, row, String2.toByteArray(String2.right("" + d, DOUBLE_LENGTH)));
    }

    /*******
     * This writes the binary byte to the file. 
     * Later, use readBinaryByte to read the value from the file.
     */
    public void writeBinaryByte(int col, int row, byte b) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.writeByte(b);
    }

    /**
     * This writes the binary char to the file. 
     * Later, use readBinaryChar to read the value from the file.
     */
    public void writeBinaryChar(int col, int row, char ch) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.writeChar(ch);
    }

    /**
     * This writes the binary short to the file. 
     * Later, use readBinaryShort to read the value from the file.
     */
    public void writeBinaryShort(int col, int row, short s) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.writeShort(s);
    }

    /**
     * This writes the binary int to the file. 
     * Later, use readBinaryInt to read the value from the file.
     */
    public void writeBinaryInt(int col, int row, int i) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.writeInt(i);
    }

    /**
     * This writes the binary long to the file. 
     * Later, use readBinaryLong to read the value from the file.
     */
    public void writeBinaryLong(int col, int row, long i) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.writeLong(i);
    }

    /**
     * This writes the binary float to the file. 
     * Later, use readBinaryFloat to read the value from the file.
     */
    public void writeBinaryFloat(int col, int row, float f) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.writeFloat(f);
    }

    /**
     * This writes the binary double to the file. 
     * Later, use readBinaryDouble to read the value from the file.
     */
    public void writeBinaryDouble(int col, int row, double d) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.writeDouble(d);
    }


    /***************************************************************
     * This reads a value (in its String form, trimmed) from the file.
     * All text reads are funnelled through this method.
     * If you spaces at beginning/end are important, perhaps put quotes around the strings.
     */
    public String readString(int col, int row) throws IOException {
        byte ar[] = new byte[columnWidths[col]];
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        raf.readFully(ar);
        String s = (new String(ar, "UTF-8")).trim();
        //if (reallyVerbose) String2.log("low level read col=" + col + 
        //    " row=" + row + " value=" + String2.annotatedString(s));
        return s;
    }

    /** This reads a boolean (stored as T|F) from the file (or false if trouble). */
    public boolean readBoolean(int col, int row) throws IOException {
        return readString(col, row).charAt(0) == 'T';
    }

    /** This reads a byte from the file (or Byte.MAX_VALUE if trouble). */
    public byte readByte(int col, int row) throws IOException {
        return Math2.narrowToByte(String2.parseInt(readString(col, row)));
    }

    /** This reads a short from the file (or Short.MAX_VALUE if trouble). */
    public short readShort(int col, int row) throws IOException {
        return Math2.narrowToShort(String2.parseInt(readString(col, row)));
    }

    /** This reads an int from the file (or Integer.MAX_VALUE if trouble). */
    public int readInt(int col, int row) throws IOException {
        return String2.parseInt(readString(col, row));
    }

    /** This reads a long from the file (or Long.MAX_VALUE if trouble). */
    public long readLong(int col, int row) throws IOException {
        return String2.parseLong(readString(col, row));
    }

    /** This reads a float from the file (or Float.NaN if trouble). */
    public float readFloat(int col, int row) throws IOException {
        return String2.parseFloat(readString(col, row));
    }

    /** This reads a double from the file (or Double.NaN if trouble). */
    public double readDouble(int col, int row) throws IOException {
        return String2.parseDouble(readString(col, row));
    }

    /****** This reads a binary byte from the file. */
    public byte readBinaryByte(int col, int row) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        return raf.readByte();
    }

    /** This reads a binary char from the file. */
    public char readBinaryChar(int col, int row) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        return raf.readChar();
    }

    /** This reads a binary short from the file. */
    public short readBinaryShort(int col, int row) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        return raf.readShort();
    }

    /** This reads a binary int from the file. */
    public int readBinaryInt(int col, int row) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        return raf.readInt();
    }

    /** This reads a binary long from the file. */
    public long readBinaryLong(int col, int row) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        return raf.readLong();
    }

    /** This reads a binary float from the file. */
    public float readBinaryFloat(int col, int row) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        return raf.readFloat();
    }

    /** This reads a binary double from the file. */
    public double readBinaryDouble(int col, int row) throws IOException {
        raf.seek(row * nBytesPerRow + columnStartAt[col]);
        return raf.readDouble();
    }

    /**
     * This tests this class.
     *
     * @throws Throwable if trouble.
     */
    public static void test() throws Throwable {
        String2.log("\nPersistentTable.test()");
        verbose = true;
        reallyVerbose = true;
        int n;
        long time;

        //find longest FLOAT_LENGTH
        String s = ("" + Float.MIN_VALUE * -4f/3f);
        int longest = s.length();
        String longestS = s;
        for (int i = 0; i < 1000; i++) {
            s = "" + ((float)Math.random() / -1e10f);
            if (s.length() > longest) {longest = s.length(); longestS = s;}
        }
        String2.log("float longestS=" + longestS + " length=" + longest);
        Test.ensureTrue(longest <= 15, "");

        //find longest DOUBLE_LENGTH
        s = ("" + Double.MIN_VALUE * -4.0/3.0);
        longest = s.length();
        longestS = s;
        for (int i = 0; i < 1000; i++) {
            s = "" + (Math.random() /  -1e150);
            if (s.length() > longest) {longest = s.length(); longestS = s;}
        }
        String2.log("double longestS=" + longestS + " length=" + longest);
        Test.ensureTrue(longest <= 24, "");

        //make a new table
        String name = EDStatic.fullTestCacheDirectory + "testPersistentTable.txt";
        File2.delete(name);
        int widths[] = {BOOLEAN_LENGTH,
            BYTE_LENGTH, BINARY_BYTE_LENGTH,   BINARY_CHAR_LENGTH, 
            SHORT_LENGTH, BINARY_SHORT_LENGTH,
            INT_LENGTH, BINARY_INT_LENGTH, 
            LONG_LENGTH, BINARY_LONG_LENGTH, 
            FLOAT_LENGTH, BINARY_FLOAT_LENGTH, 
            DOUBLE_LENGTH, BINARY_DOUBLE_LENGTH, 20};

        PersistentTable pt = new PersistentTable(name, "rw", widths);
        Test.ensureEqual(pt.nRows(), 0, "");
        Test.ensureEqual(pt.addRows(2), 2, "");
        String testS = "Now is the time f\u0F22r all good countrymen to come ...";
        pt.writeBoolean(      0, 0, true);
        pt.writeBoolean(      0, 1, false);
        pt.writeByte(         1, 0, Byte.MIN_VALUE);
        pt.writeByte(         1, 1, Byte.MAX_VALUE);
        pt.writeBinaryByte(   2, 0, Byte.MIN_VALUE);
        pt.writeBinaryByte(   2, 1, Byte.MAX_VALUE);
        pt.writeBinaryChar(   3, 0, ' ');  //hard because read will trim it to ""
        pt.writeBinaryChar(   3, 1, '\u0F22');
        pt.writeShort(        4, 0, Short.MIN_VALUE);
        pt.writeShort(        4, 1, Short.MAX_VALUE);
        pt.writeBinaryShort(  5, 0, Short.MIN_VALUE);
        pt.writeBinaryShort(  5, 1, Short.MAX_VALUE);
        pt.writeInt(          6, 0, Integer.MIN_VALUE);
        pt.writeInt(          6, 1, Integer.MAX_VALUE);
        pt.writeBinaryInt(    7, 0, Integer.MIN_VALUE);
        pt.writeBinaryInt(    7, 1, Integer.MAX_VALUE);
        pt.writeLong(         8, 0, Long.MIN_VALUE);
        pt.writeLong(         8, 1, Long.MAX_VALUE);
        pt.writeBinaryLong(   9, 0, Long.MIN_VALUE);
        pt.writeBinaryLong(   9, 1, Long.MAX_VALUE);
        pt.writeFloat(       10, 0, -Float.MAX_VALUE);
        pt.writeFloat(       10, 1, Float.NaN);
        pt.writeBinaryFloat( 11, 0, -Float.MAX_VALUE);
        pt.writeBinaryFloat( 11, 1, Float.NaN);
        pt.writeDouble(      12, 0, -Double.MAX_VALUE);
        pt.writeDouble(      12, 1, Double.NaN);
        pt.writeBinaryDouble(13, 0, -Double.MAX_VALUE);
        pt.writeBinaryDouble(13, 1, Double.NaN);
        pt.writeString(      14, 0, "");
        pt.writeString(      14, 1, testS);

        Test.ensureEqual(pt.readBoolean(      0, 0), true, "");
        Test.ensureEqual(pt.readBoolean(      0, 1), false, "");
        Test.ensureEqual(pt.readByte(         1, 0), Byte.MIN_VALUE, "");
        Test.ensureEqual(pt.readByte(         1, 1), Byte.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryByte(   2, 0), Byte.MIN_VALUE, "");
        Test.ensureEqual(pt.readBinaryByte(   2, 1), Byte.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryChar(   3, 0), ' ', "");
        Test.ensureEqual(pt.readBinaryChar(   3, 1), '\u0F22', ""); 
        Test.ensureEqual(pt.readShort(        4, 0), Short.MIN_VALUE, "");
        Test.ensureEqual(pt.readShort(        4, 1), Short.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryShort(  5, 0), Short.MIN_VALUE, "");
        Test.ensureEqual(pt.readBinaryShort(  5, 1), Short.MAX_VALUE, "");
        Test.ensureEqual(pt.readInt(          6, 0), Integer.MIN_VALUE, "");
        Test.ensureEqual(pt.readInt(          6, 1), Integer.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryInt(    7, 0), Integer.MIN_VALUE, "");
        Test.ensureEqual(pt.readBinaryInt(    7, 1), Integer.MAX_VALUE, "");
        Test.ensureEqual(pt.readLong(         8, 0), Long.MIN_VALUE, "");
        Test.ensureEqual(pt.readLong(         8, 1), Long.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryLong(   9, 0), Long.MIN_VALUE, "");
        Test.ensureEqual(pt.readBinaryLong(   9, 1), Long.MAX_VALUE, "");
        Test.ensureEqual(pt.readFloat(       10, 0), -Float.MAX_VALUE, "");
        Test.ensureEqual(pt.readFloat(       10, 1), Float.NaN, "");
        Test.ensureEqual(pt.readBinaryFloat( 11, 0), -Float.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryFloat( 11, 1), Float.NaN, "");
        Test.ensureEqual(pt.readDouble(      12, 0), -Double.MAX_VALUE, "");
        Test.ensureEqual(pt.readDouble(      12, 1), Double.NaN, "");
        Test.ensureEqual(pt.readBinaryDouble(13, 0), -Double.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryDouble(13, 1), Double.NaN, "");
        Test.ensureEqual(pt.readString(      14, 0), "", "");
        //only 18 char returned because one takes 3 bytes in UTF-8
        Test.ensureEqual(pt.readString(      14, 1), testS.substring(0, 18), "");
        pt.close();

        //reopen the file   data still there?
        pt = new PersistentTable(name, "rw", widths);
        Test.ensureEqual(pt.nRows(), 2, "");
        Test.ensureEqual(pt.readBoolean(      0, 0), true, "");
        Test.ensureEqual(pt.readBoolean(      0, 1), false, "");
        Test.ensureEqual(pt.readByte(         1, 0), Byte.MIN_VALUE, "");
        Test.ensureEqual(pt.readByte(         1, 1), Byte.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryByte(   2, 0), Byte.MIN_VALUE, "");
        Test.ensureEqual(pt.readBinaryByte(   2, 1), Byte.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryChar(   3, 0), ' ', "");
        Test.ensureEqual(pt.readBinaryChar(   3, 1), '\u0F22', ""); 
        Test.ensureEqual(pt.readShort(        4, 0), Short.MIN_VALUE, "");
        Test.ensureEqual(pt.readShort(        4, 1), Short.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryShort(  5, 0), Short.MIN_VALUE, "");
        Test.ensureEqual(pt.readBinaryShort(  5, 1), Short.MAX_VALUE, "");
        Test.ensureEqual(pt.readInt(          6, 0), Integer.MIN_VALUE, "");
        Test.ensureEqual(pt.readInt(          6, 1), Integer.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryInt(    7, 0), Integer.MIN_VALUE, "");
        Test.ensureEqual(pt.readBinaryInt(    7, 1), Integer.MAX_VALUE, "");
        Test.ensureEqual(pt.readLong(         8, 0), Long.MIN_VALUE, "");
        Test.ensureEqual(pt.readLong(         8, 1), Long.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryLong(   9, 0), Long.MIN_VALUE, "");
        Test.ensureEqual(pt.readBinaryLong(   9, 1), Long.MAX_VALUE, "");
        Test.ensureEqual(pt.readFloat(       10, 0), -Float.MAX_VALUE, "");
        Test.ensureEqual(pt.readFloat(       10, 1), Float.NaN, "");
        Test.ensureEqual(pt.readBinaryFloat( 11, 0), -Float.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryFloat( 11, 1), Float.NaN, "");
        Test.ensureEqual(pt.readDouble(      12, 0), -Double.MAX_VALUE, "");
        Test.ensureEqual(pt.readDouble(      12, 1), Double.NaN, "");
        Test.ensureEqual(pt.readBinaryDouble(13, 0), -Double.MAX_VALUE, "");
        Test.ensureEqual(pt.readBinaryDouble(13, 1), Double.NaN, "");
        Test.ensureEqual(pt.readString(      14, 0), "", "");
        //only 18 char returned because one takes 3 bytes in UTF-8
        Test.ensureEqual(pt.readString(      14, 1), testS.substring(0, 18), "");
        pt.close();

        String modes[] = {"rw", "rw", "rws", "rwd"};
        n = 1000;
        for (int mode = 0; mode < modes.length; mode++) {
            File2.delete(name);
            pt = new PersistentTable(name, modes[mode], 
                new int[]{80, BINARY_DOUBLE_LENGTH, DOUBLE_LENGTH, BINARY_INT_LENGTH, INT_LENGTH});
            pt.addRows(n);
            if (mode == 1) String2.log("*** Note: 2nd rw test uses flush()");

            //string speed test
            time = System.currentTimeMillis();
            long modeTime = System.currentTimeMillis();
            for (int i = 0; i < n; i++) 
                pt.writeString(0, i, testS + i);
            if (mode == 1) pt.flush();
            time = System.currentTimeMillis();
            String2.log("\n" + modes[mode] + " time to write " + n + " Strings=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{0,0,0,0}[mode] + "ms)");  //java 1.6 0,0,0,0

            for (int i = 0; i < n; i++) {
                int tRow = Math2.random(n);
                Test.ensureEqual(pt.readString(0, tRow), testS + tRow, "");
            }
            String2.log(modes[mode] + " time to read " + n + " Strings=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{15,16,47,15}[mode] + "ms)");  //java 1.6 15,16,47,15

            //double speed test
            time = System.currentTimeMillis();
            for (int i = 0; i < n; i++) 
                pt.writeDouble(2, i, i);
            if (mode == 1) pt.flush();
            String2.log(modes[mode] + " time to write " + n + " doubles=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{16,15,234,327}[mode] + "ms)"); //java 1.6 16,15,219,188

            time = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                int tRow = Math2.random(n);
                Test.ensureEqual(pt.readDouble(2, tRow), tRow, "");
            }
            String2.log(modes[mode] + " time to read " + n + " doubles=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{15,32,16,0}[mode] + "ms)");  //java 1.6 15,32,16,0

            //binary double speed test
            time = System.currentTimeMillis();
            for (int i = 0; i < n; i++) 
                pt.writeBinaryDouble(1, i, i);
            if (mode == 1) pt.flush();
            String2.log(modes[mode] + " time to write " + n + " binary doubles=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{16,31,1750,1872}[mode] + "ms)");  //java 1.6 16,31,1968,1687

            time = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                int tRow = Math2.random(n);
                Test.ensureEqual(pt.readBinaryDouble(1, tRow), tRow, "");
            }
            String2.log(modes[mode] + " time to read " + n + " binary doubles=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{16,16,31,31}[mode] + "ms)"); //java 1.6 16,31,16,16

            //int speed test
            time = System.currentTimeMillis();
            for (int i = 0; i < n; i++) 
                pt.writeInt(4, i, i);
            if (mode == 1) pt.flush();
            String2.log(modes[mode] + " time to write " + n + " ints=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{16,0,203,249}[mode] + "ms)"); //java 1.6 16,0,219,219

            time = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                int tRow = Math2.random(n);
                Test.ensureEqual(pt.readInt(4, tRow), tRow, "");
            }
            String2.log(modes[mode] + " time to read " + n + " ints=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{0,16,0,0}[mode] + "ms)"); //java 1.6 0,16,0,0

            //binary int speed test
            time = System.currentTimeMillis();
            for (int i = 0; i < n; i++) 
                pt.writeBinaryInt(3, i, i);
            if (mode == 1) pt.flush();
            String2.log(modes[mode] + " time to write " + n + " binary int=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{15,15,1029,1108}[mode] + "ms)"); //java 1.6 15,1531,922

            time = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                int tRow = Math2.random(n);
                Test.ensureEqual(pt.readBinaryInt(3, tRow), tRow, "");
            }
            String2.log(modes[mode] + " time to read " + n + " binary int=" + 
                (System.currentTimeMillis() - time) + "   (" + 
                new int[]{0,16,16,16}[mode] + "ms)");  //java 1.6 16,16,0


            int expected[] = {109,109,3401,3681};
            modeTime = System.currentTimeMillis() - modeTime;
            String2.log(modes[mode] + " TOTAL time to read " + n + " items=" + 
                modeTime + "   (" + expected[mode] + "ms)"); 
            Test.ensureTrue(modeTime < 2 * expected[mode], "That is too slow!"); 

            pt.close();
        }
        //String2.pressEnterToContinue("\n(considerable variation) Okay?"); 
    }


}



