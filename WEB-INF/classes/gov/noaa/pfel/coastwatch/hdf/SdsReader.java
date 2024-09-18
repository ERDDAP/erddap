/*
 * SdsReader Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.google.common.io.Resources;
import java.io.DataInputStream;
import java.util.ArrayList;

/**
 * This reads an HDF version 4 SDS file. This is a replacement for the (for me) hard to use, finicky
 * (the jar files are tied to specific out-of-date versions of Java) native libraries (which are
 * different for each OS) from NCSA.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class SdsReader {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /** This will be filled with all of the tags. */
  public ArrayList tagList = new ArrayList();

  /** This will be filled with the HdfScientificData tags. */
  public ArrayList sdList = new ArrayList();

  /**
   * This reads an hdfFile and puts the information into tagList and sdList so that it can be read
   * by Grid.readHDF().
   *
   * @param hdfFileName
   * @throws Exception if error
   */
  public SdsReader(String hdfFileName) throws Exception {
    String errorIn = String2.ERROR + " in SdsReader.read(" + hdfFileName + "): ";

    // first thing in file is magic number
    DataInputStream stream =
        new DataInputStream(File2.getDecompressedBufferedInputStream(hdfFileName));
    try {
      int offset = 0;
      int magicNumber = stream.readInt();
      if (magicNumber != 0x0e031301)
        Test.error(
            errorIn
                + "Incorrect 'magic number' (0x"
                + Integer.toHexString(magicNumber)
                + " should be 0x0E031301) at start of file.");
      offset += 4;

      // read the Descriptor Blocks
      int nDataDescriptors;
      int offsetOfNextDB;
      do {
        // Descriptor Block starts with nDataDescriptors and offset to next Descriptor Block
        nDataDescriptors = stream.readShort();
        offset += 2;
        if (verbose) String2.log("DescriptorBlock nDataDescriptors=" + nDataDescriptors);
        offsetOfNextDB = stream.readInt();
        offset += 4;
        ArrayList tagTypeList = new ArrayList();
        ArrayList tagRefNumberList = new ArrayList();
        ArrayList tagOffsetList = new ArrayList();
        ArrayList tagLengthList = new ArrayList();

        // read all of the Data Descriptors
        for (int descriptor = 0; descriptor < nDataDescriptors; descriptor++) {
          tagTypeList.add(Short.valueOf(stream.readShort()));
          tagRefNumberList.add(Short.valueOf(stream.readShort()));
          tagOffsetList.add(Integer.valueOf(stream.readInt()));
          tagLengthList.add(Integer.valueOf(stream.readInt()));
          offset += 2 * 2 + 2 * 4;
        }

        // read all of the data
        for (int descriptor = 0; descriptor < nDataDescriptors; descriptor++) {
          short tagType = ((Short) tagTypeList.get(descriptor)).shortValue();
          short refNumber = ((Short) tagRefNumberList.get(descriptor)).shortValue();
          int length = ((Integer) tagLengthList.get(descriptor)).intValue();
          int tagOffset = ((Integer) tagOffsetList.get(descriptor)).intValue();
          if (tagType == HdfNull.TYPE) {
            tagOffset = offset; // in file it is always -1, since no data
            length = 0; // in file it is always -1, since no data
          }
          if (tagOffset != offset)
            Test.error(
                errorIn
                    + "offsetList value="
                    + tagOffset
                    + " not equal to current offset="
                    + offset
                    + " for descriptor #"
                    + descriptor);
          if (verbose)
            String2.logNoNewline(
                "#"
                    + String2.right("" + descriptor, 3)
                    + ", line#"
                    + String2.right("" + lineNumber(offset), 3)
                    + " byte#="
                    + String2.right("" + byteNumberInLine(offset), 2)
                    + ", ");
          HdfTag tag;
          if (tagType == 0x001e) tag = new HdfLibraryVersion(refNumber, length, stream);
          else if (tagType == 0x0001) tag = new HdfNull(refNumber, length, stream);
          else if (tagType == 0x006a) tag = new HdfNumberType(refNumber, length, stream);
          else if (tagType == 0x02d0) tag = new HdfNumericDataGroup(refNumber, length, stream);
          else if (tagType == 0x02bd)
            tag = new HdfScientificDataDimension(refNumber, length, stream);
          else if (tagType == 0x07ab) tag = new HdfVData(refNumber, length, stream);
          else if (tagType == 0x07aa) tag = new HdfVDataDescription(refNumber, length, stream);
          else if (tagType == 0x07ad) tag = new HdfVGroup(refNumber, length, stream);
          else if (tagType == 0x02be) {
            tag = new HdfScientificData(refNumber, length, stream);
            sdList.add(tag);
          } else {
            throw new RuntimeException(
                errorIn
                    + "unsupported tag (type=0x"
                    + Integer.toHexString(tagType)
                    + " tag#="
                    + descriptor
                    + ")");
          }
          if (verbose) String2.log(tag.toString() + "\n");
          offset += length;
        }

      } while (offsetOfNextDB != 0);
    } finally {
      stream.close();
    }
  }

  public static int lineNumber(int byteNumber) {
    return byteNumber / 16;
  }

  public static int byteNumberInLine(int byteNumber) {
    return byteNumber % 16;
  } // was byteNumber()

  /**
   * This converts a byte[] into a float[]. Remember that missing values may be encoded as some
   * finite value -- that is not dealt with here.
   *
   * @param data the floats stored as bytes, most-significant-byte to least-significant-byte.
   * @return the corresponding float[]
   */
  public static float[] toFloatArray(byte[] data) {
    int n = data.length / 4;
    float far[] = new float[n];
    int i4 = 0;
    for (int i = 0; i < n; i++) {
      far[i] =
          Float.intBitsToFloat(
              (((((data[i4 + 0] << 8) | (data[i4 + 1] & 255)) << 8) | (data[i4 + 2] & 255)) << 8)
                  | (data[i4 + 3] & 255));
      i4 += 4;
    }
    return far;
  }

  /**
   * This converts a byte[] into a double[]. Remember that missing values may be encoded as some
   * finite value -- that is not dealt with here.
   *
   * @param data the doubles stored as bytes, most-significant-byte to least-significant-byte.
   * @return the corresponding double[]
   */
  public static double[] toDoubleArray(byte[] data) {
    int n = data.length / 8;
    double dar[] = new double[n];
    int i8 = 0;
    for (int i = 0; i < n; i++) {
      dar[i] =
          Double.longBitsToDouble(
              ((((((((((((((long) data[i8 + 0] << 8) | (data[i8 + 1] & 255)) << 8)
                                                          | (data[i8 + 2] & 255))
                                                      << 8)
                                                  | (data[i8 + 3] & 255))
                                              << 8)
                                          | (data[i8 + 4] & 255))
                                      << 8)
                                  | (data[i8 + 5] & 255))
                              << 8)
                          | (data[i8 + 6] & 255))
                      << 8)
                  | (data[i8 + 7] & 255));
      i8 += 8;
    }
    return dar;
  }

  /**
   * This is a unit test of this class and the HdfXxx classes.
   *
   * @param args is ignored
   * @throws Exception if trouble
   */
  public static void main(String args[]) throws Exception {

    // mini.hdf was made by gov.noaa.pfel.coastwatch/Grid with the HDF libraries
    String resourcePath = Resources.getResource("gov/noaa/pfel/coastwatch/hdf/").toString();
    String dir = File2.accessResourceFile(resourcePath);
    // Test.ensureEqual(
    //    File2.writeToFileUtf8(dir + "mini.hdf.hexDump", File2.hexDump(dir + "mini.hdf", 7262)),
    //    "", "Grid.miniTestSaveAsHDF error message");

    String2.setupLog(
        true, false, dir + "mini.hdf.dump", false, String2.logFileDefaultMaxSize); // append
    SdsReader.verbose = true;
    SdsReader sdsReader = new SdsReader(dir + "mini.hdf");
  }
}
