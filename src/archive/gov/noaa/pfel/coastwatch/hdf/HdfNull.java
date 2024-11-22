/*
 * HdfNull Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents an HdfNull tag (0x0001).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfNull extends HdfTag {

  public static final int TYPE = 0x0001;

  /**
   * The constructor used to prepare to write the data to a file.
   *
   * @param referenceNumber
   */
  public HdfNull(short referenceNumber) {
    tagType = TYPE;
    this.referenceNumber = referenceNumber;
  }

  /**
   * This returns the length of the data (in bytes)
   *
   * @return length
   */
  @Override
  public int getLength() {
    return 0;
  }

  /**
   * This writes the class's information to the dataOutputSream.
   *
   * @param stream
   */
  @Override
  public void writeData(DataOutputStream stream) {}

  /**
   * The constructor used to read data from a file.
   *
   * @param referenceNumber
   * @param length the number of bytes of data (must be 0)
   * @param stream
   * @throws Exception
   */
  public HdfNull(short referenceNumber, int length, DataInputStream stream) throws Exception {

    tagType = TYPE;
    this.referenceNumber = referenceNumber;
  }

  /**
   * This replaces the default toString with a better toString.
   *
   * @return a string describing this object
   */
  @Override
  public String toString() {

    return "HdfLibraryVersion"
        + "[tagType=0x"
        + Integer.toHexString(tagType)
        + ", referenceNumber="
        + referenceNumber
        + "]";
  }
}
