/*
 * HdfNumericDataGroup Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents an HdfNumericDataGroup tag (DFTAG_NDG tagType = 0x02d0).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfNumericDataGroup extends HdfTag {

  public short[] tag_n; // Tag number of nth member of the group
  public short[] ref_n; // Reference number of nth member of the group

  /**
   * The constructor used to prepare to write the data to a file.
   *
   * @param referenceNumber
   * @param tag_n Tag numbers of members of the group.
   * @param ref_n Reference numbers of members of the group.
   */
  public HdfNumericDataGroup(short referenceNumber, short[] tag_n, short[] ref_n) {

    tagType = 0x02d0;
    this.referenceNumber = referenceNumber;
    this.tag_n = tag_n;
    this.ref_n = ref_n;
  }

  /**
   * This returns the length of the data (in bytes)
   *
   * @return length
   */
  @Override
  public int getLength() {
    return 2 * tag_n.length + 2 * ref_n.length;
  }

  /**
   * This writes the class's information to the dataOutputSream.
   *
   * @param stream
   */
  @Override
  public void writeData(DataOutputStream stream) throws Exception {
    for (int i = 0; i < tag_n.length; i++) {
      stream.writeShort(tag_n[i]);
      stream.writeShort(ref_n[i]);
    }
  }

  /**
   * The constructor used to read data from a file.
   *
   * @param referenceNumber
   * @param length the number of bytes of data
   * @param stream
   * @throws Exception
   */
  public HdfNumericDataGroup(short referenceNumber, int length, DataInputStream stream)
      throws Exception {

    tagType = 0x02d0;
    this.referenceNumber = referenceNumber;
    int n = length / 4;
    tag_n = new short[n];
    ref_n = new short[n];
    for (int i = 0; i < n; i++) {
      tag_n[i] = stream.readShort();
      ref_n[i] = stream.readShort();
    }
  }

  /**
   * This replaces the default toString with a better toString.
   *
   * @return a string describing this object
   */
  @Override
  public String toString() {

    return "HdfNumericDataGroup"
        + "[tagType=0x"
        + Integer.toHexString(tagType)
        + ", referenceNumber="
        + referenceNumber
        + ", tag_n="
        + String2.toHexCSSVString(tag_n)
        + ", ref_n="
        + String2.toCSSVString(ref_n)
        + "]";
  }
}
