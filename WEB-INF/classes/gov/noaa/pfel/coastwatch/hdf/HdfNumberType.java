/*
 * HdfNumberType Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents an HdfNumberType tag (DFTAG_NT, tagType = 0x006a).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfNumberType extends HdfTag {

  public byte version = 1; // Version number of NT information
  public byte type; // one of the HdfConstants.DFNTF contants
  public byte width; // Number of bits, all of which are assumed to be significant
  public byte classType; // A generic value, with different interpretations

  // depending on type: floating point, integer, or character
  // See the HdfConstants.DFNTF DFNTI and DFNTC constants.

  /**
   * The constructor used to prepare to write the data to a file.
   *
   * @param referenceNumber
   * @param dfntType one of the HdfConstants.DFNT constants
   * @throws Exception
   */
  public HdfNumberType(short referenceNumber, byte dfntType) throws Exception {

    tagType = 0x006a;
    this.referenceNumber = referenceNumber;
    type = (byte) dfntType;
    if (type == HdfConstants.DFNT_CHAR8) {
      width = 8;
      classType = HdfConstants.DFNTC_ASCII1;
    } else if (type == HdfConstants.DFNT_FLOAT32) {
      width = 32;
      classType = HdfConstants.DFNTF_IEEE;
    } else if (type == HdfConstants.DFNT_FLOAT64) {
      width = 64;
      classType = HdfConstants.DFNTF_IEEE; // confirmed
    } else if (type == HdfConstants.DFNT_INT8) {
      width = 8;
      classType = HdfConstants.DFNTI_MBO; // MotorolaByteOrder
    } else if (type == HdfConstants.DFNT_INT16) {
      width = 16;
      classType = HdfConstants.DFNTI_MBO;
    } else if (type == HdfConstants.DFNT_INT32) {
      width = 32;
      classType = HdfConstants.DFNTI_MBO;
    } else
      throw new RuntimeException(
          "ERROR in HdfNumberTypeConstructor: " + "unsupported dfntType (" + dfntType + ").");
  }

  /**
   * This returns the length of the data (in bytes)
   *
   * @return length
   */
  @Override
  public int getLength() {
    return 4;
  }

  /**
   * This writes the class's information to the dataOutputSream.
   *
   * @param stream
   */
  @Override
  public void writeData(DataOutputStream stream) throws Exception {
    stream.writeByte(version); // Version number of NT information
    stream.writeByte(
        type); // Unsigned integer, signed integer, unsigned character, character, floating
    // point, double precision floating point
    stream.writeByte(width); // Number of bits, all of which are assumed to be significant
    stream.writeByte(classType); // A generic value, with different interpretations
  }

  /**
   * The constructor used to read data from a file.
   *
   * @param referenceNumber
   * @param length the number of bytes of data
   * @param stream
   * @throws Exception
   */
  public HdfNumberType(short referenceNumber, int length, DataInputStream stream) throws Exception {

    tagType = 0x006a;
    this.referenceNumber = referenceNumber;
    version = stream.readByte();
    type = stream.readByte();
    width = stream.readByte();
    classType = stream.readByte();
    ;
  }

  /**
   * This replaces the default toString with a better toString.
   *
   * @return a string describing this object
   */
  @Override
  public String toString() {

    return "HdfNumberType"
        + "[tagType=0x"
        + Integer.toHexString(tagType)
        + ", referenceNumber="
        + referenceNumber
        + ", version="
        + version
        + ", type="
        + type
        + ", width="
        + width
        + ", classType="
        + classType
        + "]";
  }
}
