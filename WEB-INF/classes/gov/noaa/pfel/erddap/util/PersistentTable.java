/*
 * PersistentTable Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * PersistentTable maintains a table of data in a random access file. <br>
 * The number and type of the columns is fixed. <br>
 * The number of rows can be increased. <br>
 * Rows can't be deleted, so define a boolean column to indicate if a row is active (and to later
 * find inactive rows so they can be reused). <br>
 * Rows and columns are numbered 0..
 *
 * <p>The data can be stored as text or in binary format. <br>
 * Text is nice since the file is readable/editable/fixable in a text editor. (Make sure line
 * lengths stay the same!) <br>
 * Strings are always converted to utf-8 then stored (so reserve extra space if chars above #128
 * expected).
 *
 * <p>PersistentTable is NOT synchronized. <br>
 * If more than 1 thread may be working with the table, use
 *
 * <pre>
 * synchronized(persistentTable) {
 * //do several things here
 * //...
 * //if "rw" mode and you wrote some changes, perhaps use persistentTable.flush() at end
 * }
 * </pre>
 *
 * That also has the advantage of making a group of changes atomically.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-12-02
 */
public class PersistentTable {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  /** When primitives are stored in file, these are the lengths (in bytes). */
  public static int BOOLEAN_LENGTH = 1;

  public static int BYTE_LENGTH = 4;
  public static int SHORT_LENGTH = 6;
  public static int INT_LENGTH = 11;
  public static int LONG_LENGTH = 20;
  public static int FLOAT_LENGTH = 16; // I think 15 (e.g., -1.09464165E-11), but add 1 for safety
  public static int DOUBLE_LENGTH =
      25; // I think 24 (e.g,. -2.4353007519111625E-151), but add 1 for safety

  public static int BINARY_BYTE_LENGTH = 1;
  public static int BINARY_CHAR_LENGTH = 2;
  public static int BINARY_SHORT_LENGTH = 2;
  public static int BINARY_INT_LENGTH = 4;
  public static int BINARY_LONG_LENGTH = 8;
  public static int BINARY_FLOAT_LENGTH = 4;
  public static int BINARY_DOUBLE_LENGTH = 8;

  // set once
  private String fullFileName;
  private RandomAccessFile raf;
  private int columnWidths[];
  private int columnStartAt[];
  private int nBytesPerRow;

  // changes
  private int nRows = 0;

  /**
   * The constructor.
   *
   * @param fullFileName if it exists, the data will be used. If not, it will be created.
   * @param mode This must be one of
   *     <ul>
   *       <li>"r" Open for reading only. Invoking any of the write methods of the resulting object
   *           will cause an IOException to be thrown.
   *       <li>"rw" Open for reading and writing. If the file does not already exist then an attempt
   *           will be made to create it.
   *       <li>"rws" Open for reading and writing, as with "rw", and also require that every update
   *           to the file's content or metadata be written synchronously to the underlying storage
   *           device.
   *       <li>"rwd" Open for reading and writing, as with "rw", and also require that every update
   *           to the file's content be written synchronously to the underlying storage device.
   *     </ul>
   *     See info about mode at
   *     https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/RandomAccessFile.html
   *     <br>
   *     Reading data is equally fast in all modes. <br>
   *     Writing data in "rw" is 10X to 40X faster than "rws" and "rwd" modes. <br>
   *     For writing data in "rw" mode, text and binary methods are equally fast (text is perhaps
   *     slightly faster!). <br>
   *     For writing data in "rws" and "rwd" modes, text is 5X to 10X FASTER than binary (!!!). <br>
   *     String read/write is surprisingly fast in all modes. <br>
   *     Using "rw" and flush() after each group of writes is slower than rw, but faster than rws
   *     and rwd, and closest to making groups of action atomic. <br>
   *     Advice: if file integrity is very important, use "rw"+flush or "rws".
   * @param columnWidths (in bytes) For numeric columns, use the XXX_LENGTH constants. <br>
   *     For Strings, use whatever value you want. Strings are always converted to utf-8 then
   *     stored, so reserve extra space if chars above #128 expected.
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
    nBytesPerRow++; // for newline automatically added to end of row

    // open the file;
    raf = new RandomAccessFile(fullFileName, mode);
    long longNRows =
        raf.length() / nBytesPerRow; // integer division causes partial row at end to be ignored
    Math2.ensureArraySizeOkay(longNRows, "PersistentTable");
    nRows = (int) longNRows; // save since checked above
    if (verbose)
      String2.log(
          "PersistentTable " + fullFileName + " is open.\n" + "mode=" + mode + " nRows=" + nRows);
  }

  /**
   * This flushes and closes the file. You don't have to do this -- it will be done automatically
   * when a program shuts down. Future attempts to read/write will throw null pointer exceptions.
   */
  public void close() throws IOException {
    if (raf != null) {
      try {
        flush();
      } catch (Exception e) {
      }
      try {
        raf.close();
      } catch (Exception e) {
      }
      raf = null;
    }
  }

  /**
   * If mode="rw", this flushes the all pending writes to the file. For "rws" and "rwd", flushing is
   * done automatically after every write. This is never needed for reads.
   */
  public void flush() throws IOException {
    raf.getChannel().force(true);
  }

  /**
   * Users of this class shouldn't call this -- use close() instead. Java calls this when an object
   * is no longer used, just before garbage collection.
   */
  protected void finalize() throws Throwable {
    try { // extra insurance
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
   * This adds n empty rows to the end of the file. The row is initialized with spaces, which is
   * fine for the write...asText methods, but odd for writeBinary... methods. This automatically
   * adds a newline at end of each row.
   *
   * @param n the number of rows to be added
   * @return the number of rows after the rows are added
   */
  public int addRows(int n) throws IOException {
    raf.seek(nRows * nBytesPerRow);
    byte ar[] = new byte[nBytesPerRow];
    Arrays.fill(ar, (byte) 32);
    ar[nBytesPerRow - 1] = (byte) '\n';
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
    byte ar[] = new byte[nBytesPerRow - 1]; // -1 since \n won't be changed
    Arrays.fill(ar, (byte) 32);
    raf.write(ar);
  }

  /**
   * This writes a value (stored as a byte ar) into the file. All text writes are funnelled through
   * this method.
   *
   * @throws RuntimeException if ar.length != columnWidths[col] or col is invalid or row is invalid.
   */
  public void write(int col, int row, byte ar[]) throws IOException {
    if (ar.length != columnWidths[col])
      throw new RuntimeException(
          "PersistentTable Error: Byte array length="
              + ar.length
              + " doesn't match columnWidth["
              + col
              + "]="
              + columnWidths[col]
              + ".");
    raf.seek(row * nBytesPerRow + columnStartAt[col]);
    raf.write(ar);
  }

  /**
   * This writes a string value to the file The string UTF-8 encoded then stored, so the
   * byteArray.length may be greater than s.length(), so make the column wider to be safe.
   *
   * @param s a String. If s is too long, it is truncated. If too short, it is space-padded at the
   *     end.
   * @param length
   * @return the corresponding byte[] (or null if s is null)
   */
  public void writeString(int col, int row, String s) throws IOException {
    int colWidth = columnWidths[col];
    if (s.length() > colWidth) s = s.substring(0, colWidth);
    byte ar[] = String2.stringToUtf8Bytes(s);
    // truncating is tricky because don't want to have 1/2 of a 2-byte char
    while (ar.length > colWidth) {
      if (reallyVerbose)
        String2.log("s=" + String2.annotatedString(s) + " will be shortened by 1 char.");
      s = s.substring(0, s.length() - 1); // remove last byte
      ar = String2.stringToUtf8Bytes(s);
    }
    if (ar.length < colWidth) {
      byte tar[] = new byte[colWidth];
      System.arraycopy(ar, 0, tar, 0, ar.length);
      Arrays.fill(tar, ar.length, colWidth, (byte) ' ');
      ar = tar;
    }
    write(col, row, ar);
  }

  /**
   * This converts the boolean to text (T|F) and then to byte[BOOLEAN_LENGTH] then writes it to the
   * file. Later, use read to read the value from the file.
   */
  public void writeBoolean(int col, int row, boolean b) throws IOException {
    write(col, row, b ? new byte[] {(byte) 'T'} : new byte[] {(byte) 'F'});
  }

  /**
   * This converts the byte to text and then to byte[BYTE_LENGTH] then writes it to the file. Later,
   * use readByte to read the value from the file.
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
   * This converts the int to text and then to byte[INT_LENGTH] then writes it to the file. Later,
   * use readInt to read the value from the file.
   */
  public void writeInt(int col, int row, int i) throws IOException {
    write(col, row, String2.toByteArray(String2.right("" + i, INT_LENGTH)));
  }

  /**
   * This converts the long to text and then to byte[LONG_LENGTH] then writes it to the file. Later,
   * use readLong to read the value from the file.
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
   * This writes the binary char to the file. Later, use readBinaryChar to read the value from the
   * file.
   */
  public void writeBinaryChar(int col, int row, char ch) throws IOException {
    raf.seek(row * nBytesPerRow + columnStartAt[col]);
    raf.writeChar(ch);
  }

  /**
   * This writes the binary short to the file. Later, use readBinaryShort to read the value from the
   * file.
   */
  public void writeBinaryShort(int col, int row, short s) throws IOException {
    raf.seek(row * nBytesPerRow + columnStartAt[col]);
    raf.writeShort(s);
  }

  /**
   * This writes the binary int to the file. Later, use readBinaryInt to read the value from the
   * file.
   */
  public void writeBinaryInt(int col, int row, int i) throws IOException {
    raf.seek(row * nBytesPerRow + columnStartAt[col]);
    raf.writeInt(i);
  }

  /**
   * This writes the binary long to the file. Later, use readBinaryLong to read the value from the
   * file.
   */
  public void writeBinaryLong(int col, int row, long i) throws IOException {
    raf.seek(row * nBytesPerRow + columnStartAt[col]);
    raf.writeLong(i);
  }

  /**
   * This writes the binary float to the file. Later, use readBinaryFloat to read the value from the
   * file.
   */
  public void writeBinaryFloat(int col, int row, float f) throws IOException {
    raf.seek(row * nBytesPerRow + columnStartAt[col]);
    raf.writeFloat(f);
  }

  /**
   * This writes the binary double to the file. Later, use readBinaryDouble to read the value from
   * the file.
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
    String s = new String(ar, StandardCharsets.UTF_8).trim();
    // if (reallyVerbose) String2.log("low level read col=" + col +
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
}
