/*
 * HdfScientificData Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.File2;
import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents an HdfScientificData tag (tagType = 0x02be).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfScientificData extends HdfTag {
  // FUTURE: this would be more flexible if data[] could be any primitive array or a String.
  // Since Java has bigendian order, values could be written to outstream value
  // by value, without generating an entire byte[].

  public byte[] data;

  /**
   * The constructor used to prepare to write the data to a file.
   *
   * @param referenceNumber
   * @param data
   */
  public HdfScientificData(short referenceNumber, byte data[]) throws Exception {

    tagType = 0x02be;
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
  public HdfScientificData(short referenceNumber, int length, DataInputStream stream)
      throws Exception {

    tagType = 0x02be;
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
    String s = String2.toHexCSSVString(data);
    return "HdfScientificData"
        + "[tagType=0x"
        + Integer.toHexString(tagType)
        + ", referenceNumber="
        + referenceNumber
        + ", data="
        + s
        + // s.substring(0, Math.min(200, s.length())) +
        "]";
  }
}
