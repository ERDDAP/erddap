/*
 * HdfVData Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.File2;
import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents a HdfVData tag (tagType = 0x07ab).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfVData extends HdfTag {

  public byte[] data;

  /**
   * The constructor used to prepare to write the data to a file.
   *
   * @param referenceNumber
   * @param data
   * @throws Exception
   */
  public HdfVData(short referenceNumber, byte data[]) throws Exception {
    tagType = 0x07ab;
    this.referenceNumber = referenceNumber;
    this.data = data;
  }

  /**
   * This returns the length of the data (in bytes)
   *
   * @return length
   */
  @Override
  public int getLength() {
    return data.length;
  }

  /**
   * This writes the class's information to the dataOutputSream.
   *
   * @param stream
   */
  @Override
  public void writeData(DataOutputStream stream) throws Exception {
    stream.write(data);
  }

  /**
   * The constructor used to read data from a file.
   *
   * @param referenceNumber
   * @param length the number of bytes of data
   * @param stream
   * @throws Exception
   */
  public HdfVData(short referenceNumber, int length, DataInputStream stream) throws Exception {

    tagType = 0x07ab;
    this.referenceNumber = referenceNumber;
    data = File2.readFully(stream, length);
  }

  /**
   * This replaces the default toString with a better toString.
   *
   * @return a string describing this object
   */
  @Override
  public String toString() {

    return "HdfVData"
        + "[tagType=0x"
        + Integer.toHexString(tagType)
        + ", referenceNumber="
        + referenceNumber
        + ", data="
        + String2.toHexCSSVString(data)
        + "]";
  }
}
