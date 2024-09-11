/*
 * HdfVDataDescription Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.File2;
import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents an HdfVDataDescription tag(tagType = 0x07aa).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfVDataDescription extends HdfTag {

  public short interlacescheme = 0;
  public int nvert = 1; // Number of entries in Vdata
  public short ivsize = 4; // Size of one Vdata entry (16-bit integer)
  public short nfields = 1; // Number of fields per entry in the Vdata (16-bit integer)
  public short type_n[] = {
    (short) HdfConstants.DFNT_INT32
  }; // Constant indicating the data type of the nth field of the Vdata (16-bit integer)
  public short isize_n[] = {
    (short) 4
  }; // Size in bytes of the nth field of the Vdata (16-bit integer)
  public short offset_n[] = {
    (short) 0
  }; // Offset of the nth field within the Vdata (16-bit integer)
  public short order_n[] = {(short) 1}; // Order of the nth field of the Vdata (16-bit integer)
  // order seems to be number of items: nchars if a string, or 1 for one data value
  // public short fldnmlen_n; //Length of the nth field name string (16-bit integer)
  // see hdf documentation (HDF41r5_SpecDG.pdf, section 7.6.2, pg 62): use the name: "Values"
  public String fldnm_n[] = {
    "Values"
  }; // Non-null terminated ASCII string (length given by corresponding fldnmlen_n)
  // public short namelen; //Length of the name field (16-bit integer)
  public String name; // Non-null terminated ASCII string (length given by namelen)
  // public short classlen; //Length of the class field (16-bit integer)
  public String className; // Non-null terminated ASCII string (length given by classlen)
  public short extag = 0; // Extension tag (16-bit integer)
  public short exref = 0; // Extension reference number (16-bit integer)
  public short version = 3; // Version number of DFTAG_VH information (16-bit integer)
  public short unused = 0; // more Unused (2 zero bytes)
  public byte[] junk = {0, 3, 0, 0, 0}; // + 5 undocumented bytes: 0 3 0 0 0

  /**
   * The constructor used to prepare to write a vData which has one field (nfields=1) to a file.
   * [One could make other constructors to write VData with multiple fields.]
   *
   * @param referenceNumber
   * @param name
   * @param className
   * @param fldnm the name of the field
   * @param type_n one of the HdfConstants.DFNT constants (for String data use
   *     HdfConstants.DFNT_CHAR8)
   * @param nvert the number of rows of data. Actually, nvert must be a short, but this checks that
   *     it is not too big.
   * @param isize_n size in bytes of the nth field of the Vdata (for String data, supply the number
   *     of characters). Actually, isize_n must be a short, but this checks that it is not too big.
   * @throws Exception if trouble
   */
  public HdfVDataDescription(
      short referenceNumber,
      String name,
      String className,
      String fldnm,
      short type_n,
      int nvert,
      int isize_n)
      throws Exception {

    if (name.length() >= Short.MAX_VALUE)
      throw new RuntimeException(
          String2.ERROR
              + " in HdfVDataDescription constructor: 'name' is too long ("
              + name.length()
              + " characters).");
    if (className.length() >= Short.MAX_VALUE)
      throw new RuntimeException(
          String2.ERROR
              + " in HdfVDataDescription constructor: 'className' is too long ("
              + className.length()
              + " characters).");
    if (fldnm.length() >= Short.MAX_VALUE)
      throw new RuntimeException(
          String2.ERROR
              + " in HdfVDataDescription constructor: 'fldnm' is too long ("
              + fldnm.length()
              + " characters).");
    if (isize_n >= Short.MAX_VALUE)
      throw new RuntimeException(
          String2.ERROR
              + " in HdfVDataDescription constructor: too much data (isize_n="
              + isize_n
              + " bytes) for name="
              + name
              + ".");
    if (nvert >= Short.MAX_VALUE)
      throw new RuntimeException(
          String2.ERROR
              + " in HdfVDataDescription constructor: too much data (nvert="
              + nvert
              + ") for name="
              + name
              + ".");

    tagType = 0x07aa;
    this.referenceNumber = referenceNumber;
    this.name = name;
    this.className = className;
    this.fldnm_n = new String[] {fldnm};
    this.type_n = new short[] {type_n};
    this.nvert = (short) nvert;
    this.ivsize = (short) isize_n; // since nfields = 1, ivsize = isize_n
    this.isize_n = new short[] {(short) isize_n};
    this.order_n = new short[] {(short) (type_n == HdfConstants.DFNT_CHAR8 ? isize_n : 1)};
  }

  /**
   * This returns the length of the data (in bytes)
   *
   * @return length
   */
  @Override
  public int getLength() {
    int length = 0;
    for (int i = 0; i < nfields; i++)
      length += 5 * 2 + fldnm_n[i].length(); // 5 includes length of strings
    return length
        + // which accounts for the arrays
        9 * 2
        + // short   (9 includes length of strings)
        1 * 4
        + // int
        name.length()
        + className.length()
        + // char8
        junk.length;
  }

  /**
   * This writes the class's information to the dataOutputSream.
   *
   * @param stream
   */
  @Override
  public void writeData(DataOutputStream stream) throws Exception {
    stream.writeShort(interlacescheme);
    stream.writeInt(nvert); // Number of entries in Vdata
    stream.writeShort(ivsize); // Size of one Vdata entry (16-bit integer)
    stream.writeShort(nfields); // Number of fields per entry in the Vdata (16-bit integer)
    for (int i = 0; i < nfields; i++) {
      stream.writeShort(
          type_n[i]); // Constant indicating the data type of the nth field of the Vdata (16-bit
      // integer)
      stream.writeShort(isize_n[i]); // Size in bytes of the nth field of the Vdata (16-bit integer)
      stream.writeShort(offset_n[i]); // Offset of the nth field within the Vdata (16-bit integer)
      stream.writeShort(order_n[i]); // Order of the nth field of the Vdata (16-bit integer)
      stream.writeShort(
          fldnm_n[i].length()); // Length of the nth field name string (16-bit integer)
      stream.write(
          String2.toByteArray(
              fldnm_n[i])); // Non-null terminated ASCII string (length given by corresponding
      // fldnmlen_n)
    }
    stream.writeShort(name.length()); // Length of the name field (16-bit integer)
    stream.write(
        String2.toByteArray(name)); // Non-null terminated ASCII string (length given by namelen)
    stream.writeShort(className.length()); // Length of the class field (16-bit integer)
    stream.write(
        String2.toByteArray(
            className)); // Non-null terminated ASCII string (length given by classlen)
    stream.writeShort(extag); // Extension tag (16-bit integer)
    stream.writeShort(exref); // Extension reference number (16-bit integer)
    stream.writeShort(version); // Version number of DFTAG_VH information (16-bit integer)
    stream.writeShort(unused); // more Unused (2 zero bytes)
    stream.write(junk); // 5 undocumented bytes: 0 3 0 0 0
  }

  /**
   * The constructor used to read data from a file.
   *
   * @param referenceNumber
   * @param length the number of bytes of data
   * @param stream
   * @throws Exception
   */
  public HdfVDataDescription(short referenceNumber, int length, DataInputStream stream)
      throws Exception {

    int len;
    tagType = 0x07aa;
    this.referenceNumber = referenceNumber;
    interlacescheme = stream.readShort();
    nvert = stream.readInt(); // Number of entries in Vdata
    ivsize = stream.readShort(); // Size of one Vdata entry (16-bit integer)
    nfields = stream.readShort(); // Number of fields per entry in the Vdata (16-bit integer)
    type_n = new short[nfields];
    isize_n = new short[nfields];
    offset_n = new short[nfields];
    order_n = new short[nfields];
    fldnm_n = new String[nfields];
    for (int i = 0; i < nfields; i++) {
      type_n[i] =
          stream.readShort(); // Constant indicating the data type of the nth field of the Vdata
      // (16-bit integer)
      isize_n[i] =
          stream.readShort(); // Size in bytes of the nth field of the Vdata (16-bit integer)
      offset_n[i] = stream.readShort(); // Offset of the nth field within the Vdata (16-bit integer)
      order_n[i] = stream.readShort(); // Order of the nth field of the Vdata (16-bit integer)
      length = stream.readShort(); // Length of the nth field name string (16-bit integer)
      fldnm_n[i] =
          new String(
              File2.readFully(
                  stream,
                  length)); // Non-null terminated ASCII string (length given by corresponding
      // fldnmlen_n)
    }
    length = stream.readShort(); // Length of the name field (16-bit integer)
    name =
        new String(
            File2.readFully(
                stream, length)); // Non-null terminated ASCII string (length given by namelen)
    length = stream.readShort(); // Length of the class field (16-bit integer)
    className =
        new String(
            File2.readFully(
                stream, length)); // Non-null terminated ASCII string (length given by classlen)
    extag = stream.readShort(); // Extension tag (16-bit integer)
    exref = stream.readShort(); // Extension reference number (16-bit integer)
    version = stream.readShort(); // Version number of DFTAG_VH information (16-bit integer)
    unused = stream.readShort(); // more Unused (2 zero bytes)
    junk = File2.readFully(stream, 5); // 5 undocumented bytes: 0 3 0 0 0
  }

  /**
   * This replaces the default toString with a better toString.
   *
   * @return a string describing this object
   */
  @Override
  public String toString() {

    return "HdfVDataDescription"
        + "[tagType=0x"
        + Integer.toHexString(tagType)
        + ", referenceNumber="
        + referenceNumber
        + ", interlacescheme="
        + interlacescheme
        + ", nvert="
        + nvert
        + ", ivsize="
        + ivsize
        + ", nfields="
        + nfields
        + ", type_n="
        + String2.toCSSVString(type_n)
        + ", isize_n="
        + String2.toCSSVString(isize_n)
        + ", offset_n="
        + String2.toCSSVString(offset_n)
        + ", order_n="
        + String2.toCSSVString(order_n)
        + ", fldnm_n="
        + String2.annotatedString(String2.toCSSVString(fldnm_n))
        + ", name="
        + String2.annotatedString(name)
        + ", className="
        + String2.annotatedString(className)
        + ", extag="
        + extag
        + ", exref="
        + exref
        + ", version="
        + version
        + ", unused="
        + unused
        + ", junk="
        + String2.toCSSVString(junk)
        + "]";
  }
}
