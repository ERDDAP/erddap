/*
 * HdfVGroup Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.File2;
import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents an HdfVGroup tag (tagType = 0x07ad).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfVGroup extends HdfTag {

  public short nelt; // Number of elements in the Vgroup (16-bit integer)
  public short tag_n[]; // Tag of the nth member of the Vgroup (16-bit integer)
  public short ref_n[]; // Reference number of the nth member of the Vgroup (16-bit integer)
  // public short namelen Length of the name field (16-bit integer)
  public String name; // Non-null terminated ASCII string (length given by namelen)
  // public short classlen Length of the class field (16-bit integer)
  public String className; // Non-null terminated ASCII string (length given by classlen)
  public short extag = 0; // Extension tag (16-bit integer)
  public short exref = 0; // Extension reference number (16-bit integer)
  public short version = 3; // Version number of DFTAG_VG information (16-bit integer)
  public short unused = 0; // more Unused (2 zero bytes)
  public byte junk = (byte) 0; // + 1 undocumented byte: 0

  /**
   * The constructor used to prepare to write the data to a file.
   *
   * @param referenceNumber
   * @param tag_n tag of the nth member of the Vgroup (16-bit integer)
   * @param ref_n reference number of the nth member of the Vgroup (16-bit integer)
   * @param name the variable name for data and dimensions; "RIG0.0" for the top VGroup
   * @param className "Var0.0" for data and dimensions; "RIG0.0" for the top VGroup
   */
  public HdfVGroup(
      short referenceNumber, short[] tag_n, short[] ref_n, String name, String className) {

    if (name.length() >= Short.MAX_VALUE)
      throw new RuntimeException(
          String2.ERROR
              + " in HdfVGroup constructor: 'name' is too long ("
              + name.length()
              + " characters).");
    if (className.length() >= Short.MAX_VALUE)
      throw new RuntimeException(
          String2.ERROR
              + " in HdfVGroup constructor: 'name' is too long ("
              + className.length()
              + " characters).");

    tagType = 0x07ad;
    this.referenceNumber = referenceNumber;
    nelt = (short) tag_n.length;
    this.tag_n = tag_n;
    this.ref_n = ref_n;
    this.name = name;
    this.className = className;
  }

  /**
   * This returns the length of the data (in bytes)
   *
   * @return length
   */
  @Override
  public int getLength() {
    return 7 * 2
        + // short
        nelt * (2 + 2)
        + // arrays
        name.length()
        + className.length()
        + // char8
        1; // junk
  }

  /**
   * This writes the class's information to the dataOutputSream.
   *
   * @param stream
   */
  @Override
  public void writeData(DataOutputStream stream) throws Exception {
    stream.writeShort(nelt); // Number of elements in the Vgroup (16-bit integer)
    for (int i = 0; i < nelt; i++)
      stream.writeShort(tag_n[i]); // Tag of the nth member of the Vgroup (16-bit integer)
    for (int i = 0; i < nelt; i++)
      stream.writeShort(
          ref_n[i]); // Reference number of the nth member of the Vgroup (16-bit integer)
    stream.writeShort(name.length()); // Length of the name field (16-bit integer)
    stream.write(
        String2.toByteArray(name)); // Non-null terminated ASCII string (length given by namelen)
    stream.writeShort(className.length()); // Length of the class field (16-bit integer)
    stream.write(
        String2.toByteArray(
            className)); // Non-null terminated ASCII string (length given by classlen)
    stream.writeShort(extag); // Extension tag (16-bit integer)
    stream.writeShort(exref); // Extension reference number (16-bit integer)
    stream.writeShort(version); // Version number of DFTAG_VG information (16-bit integer)
    stream.writeShort(unused); // more Unused (2 zero bytes)
    stream.write(junk); // 1 undocumented byte: 0
  }

  /**
   * The constructor used to read data from a file.
   *
   * @param referenceNumber
   * @param length the number of bytes of data
   * @param stream
   * @throws Exception
   */
  public HdfVGroup(short referenceNumber, int length, DataInputStream stream) throws Exception {

    tagType = 0x07ad;
    this.referenceNumber = referenceNumber;
    nelt = stream.readShort(); // Number of elements in the Vgroup
    tag_n = new short[nelt];
    for (int i = 0; i < nelt; i++)
      tag_n[i] = stream.readShort(); // Tag of the nth member of the Vgroup
    ref_n = new short[nelt];
    for (int i = 0; i < nelt; i++)
      ref_n[i] = stream.readShort(); // Reference number of the nth member of the Vgroup
    int len = stream.readShort(); // Length of the name field
    name = new String(File2.readFully(stream, len)); // Non-null terminated ASCII string
    len = stream.readShort(); // Length of the class field
    className = new String(File2.readFully(stream, len)); // Non-null terminated ASCII string
    extag = stream.readShort(); // Extension tag
    exref = stream.readShort(); // Extension reference number
    version = stream.readShort(); // Version number of DFTAG_VG information
    unused = stream.readShort(); // more Unused (2 zero bytes)
    junk = (byte) stream.read(); // 1 undocumented byte: 0
  }

  /**
   * This replaces the default toString with a better toString.
   *
   * @return a string describing this object
   */
  @Override
  public String toString() {

    return "HdfVGroup"
        + "[tagType=0x"
        + Integer.toHexString(tagType)
        + ", referenceNumber="
        + referenceNumber
        + ", nelt="
        + nelt
        + ", tag_n="
        + String2.toHexCSSVString(tag_n)
        + ", ref_n="
        + String2.toCSSVString(ref_n)
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
        + junk
        + "]";
  }
}
