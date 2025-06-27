/*
 * HdfLibraryVersion Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.File2;
import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents an HdfLibraryVersion tag (0x001e).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfLibraryVersion extends HdfTag {

  public int majorVersion;
  public int minorVersion;
  public int releaseNumber;
  public String info;

  /**
   * The constructor used to prepare to write the data to a file.
   *
   * @param referenceNumber
   */
  public HdfLibraryVersion(short referenceNumber, boolean compatibleMode) {
    tagType = 0x1e;
    this.referenceNumber = referenceNumber;

    majorVersion = 4;
    minorVersion = compatibleMode ? 1 : 2;
    releaseNumber = compatibleMode ? 3 : 1;
    info =
        compatibleMode
            ? "NCSA HDF Version 4.1 Release 3, May 1999" + String2.makeString('\u0000', 40)
            : "NCSA HDF Version 4.2 Release 1, February 17, 2005"
                + String2.makeString('\u0000', 31);
  }

  /**
   * This returns the length of the data (in bytes)
   *
   * @return length
   */
  @Override
  public int getLength() {
    return 3 * 4 + info.length();
  }

  /**
   * This writes the class's information to the dataOutputSream.
   *
   * @param stream
   */
  @Override
  public void writeData(DataOutputStream stream) throws Exception {
    stream.writeInt(majorVersion);
    stream.writeInt(minorVersion);
    stream.writeInt(releaseNumber);
    stream.write(String2.toByteArray(info));
  }

  /**
   * The constructor used to read data from a file.
   *
   * @param referenceNumber
   * @param length the number of bytes of data
   * @param stream
   * @throws Exception
   */
  public HdfLibraryVersion(short referenceNumber, int length, DataInputStream stream)
      throws Exception {

    tagType = 0x1e;
    this.referenceNumber = referenceNumber;
    majorVersion = stream.readInt();
    minorVersion = stream.readInt();
    releaseNumber = stream.readInt();
    info = new String(File2.readFully(stream, length - 3 * 4));
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
        + ", majorVersion="
        + majorVersion
        + ", minorVersion="
        + minorVersion
        + ", releaseNumber="
        + releaseNumber
        + ", info="
        + String2.annotatedString(info)
        + "]";
  }
}
