/*
 * SdsWriter Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

import com.cohort.array.*;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.google.common.io.Resources;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Create an HDF version 4 SDS file. This is a replacement for the (for me) hard to use, finicky
 * (the jar files are tied to specific out-of-date versions of Java) native libraries (which are
 * different for each OS) from NCSA.
 *
 * <p>Why write my own hdf routines (vs. using Matlab script)?
 *
 * <ol>
 *   <li>Design goal: no use of commercial software.
 *   <li>The Matlab script saved Lat and Lon as 2D arrays (1,n) which seems odd and caused CDAT to
 *       think they were datasets.
 *   <li>If the Matlab script failed, the DISPLAY was not reset and future calls to generate hdf
 *       files failed until (?) system was rebooted.
 *   <li>The script was slow (~60 s for AT 1 km). This new system is roughly 10x faster.
 * </ol>
 *
 * <p>Why write my own hdf routines (vs. using NCSA libaries)?
 *
 * <ol>
 *   <li>Without a pure-Java solution, there would have to be separate installers for my program for
 *       different operating systems and different versions of Java. That many versions would be a
 *       testing nightmare -- I don't have the resources. And it would be an installation headache
 *       for users (as it has been for me).
 *   <li>Getting a C and JNI compiler to work to recompile the libraries for, e.g., Linux and Java
 *       1.5, would not be easy or fast (nor guaranteed to work). Nor would it be the final answer,
 *       e.g., what about Java 1.6?
 *   <li>I estimated (and I was right) that it wouldn't take a long time to write. One week is short
 *       in the grand scheme of things.
 *   <li>The routines may be of use to others.
 * </ol>
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class SdsWriter {
  // FUTURE:
  // * to save memory, HdfVScientificData could write on object (e.g., the data array)
  //  directly to the stream, value by value, without generating an entire byte[].
  //  Currently the largest files (SST 1km) need 2 * 50 MB of temporary space:
  //  50 MB for the reodered z and 50 MB for the conversion to byte[].
  // * remove/replace uses of com.cohort classes so hdf package can stand on its own
  // * support compression

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * In compatible mode, create() does things that exactly mimic the native HDF libraries, even
   * goofy unnecessary things like unnecessary null tags and trimming strings to 63 characters.
   */
  public static boolean compatibleMode = false;

  /**
   * This creates an HDF SDS file.
   *
   * @param hdfFileName
   * @param dims an array with the arrays for the values for each dimension e.g., dims[0] holds the
   *     first dimension's values (e.g., 0, 10, 20). Typically dims[0] will be Longitude and dims[1]
   *     will be Latitude.
   * @param dimNames holds the variable names for each dimension
   * @param dimAttributes the attributes for each dimension; dimAttributes.get(0) holds the first
   *     dimension's attributes. The attributes for "long_name" will not be created automatically
   *     with the dimNames.
   * @param data an array with the grid data. All of the dimensions are collapsed to one dimension.
   *     Write the data to this array by varying the first dimension's index varying fastest (e.g.,
   *     the values for min...max longitude at the min latitude, ... , the values for min...max
   *     longitude at the max latitude).
   * @param dataName is the name of the grid data variable
   * @param dataAttributes the data variable's attributes. The attribute for "long_name" will be
   *     created automatically with the dataName.
   * @param globalAttributes
   * @throws Exception if error
   */
  public static void create(
      String hdfFileName,
      double dims[][],
      String dimNames[],
      Attributes dimAttributes[],
      double data[],
      String dataName,
      Attributes dataAttributes,
      Attributes globalAttributes)
      throws Exception {

    String2.log("SdsWriter.create(" + hdfFileName + ")\n" + Math2.memoryString());
    if (dims.length >= 1000) // better limit?
    throw new RuntimeException(
          String2.ERROR + " in SdsWriter.create: rank (" + dims.length + ") is too big.");

    // delete any existing file
    File2.delete(hdfFileName);
    short rank = (short) dims.length;
    String fakeValuesString = "Values";
    // HDF41r5_SpecDG.pdf says it should be "Values", but NCSA HDFLibrary makes a file with "VALUES"
    // neither HDFView or CDAT seems to care
    String otherValuesString = "VALUES";

    // create HdfNull
    String2.log("SdsWriter.create creating tags");
    short nextReferenceNumber = 0;
    HdfNull hdfNull = new HdfNull(nextReferenceNumber++);

    // create hdfLibraryVersion
    HdfLibraryVersion hdfLibraryVersion =
        new HdfLibraryVersion(nextReferenceNumber++, compatibleMode);

    // create HdfNumericDataGroup and HdfScientificData for the data
    HdfNumericDataGroup dataHdfNumericDataGroup =
        new HdfNumericDataGroup(nextReferenceNumber++, null, null); // data will be supplied later
    HdfScientificData dataHdfScientificData =
        new HdfScientificData(nextReferenceNumber++, toByteArray(data));
    data = null; // data can be garbage collected if not used elsewhere

    // create an HdfScientificData for each dimension
    HdfNumericDataGroup dimsHdfNumericDataGroup[] = new HdfNumericDataGroup[rank];
    HdfScientificData dimsHdfScientificData[] = new HdfScientificData[rank];
    for (int i = 0; i < rank; i++) {
      dimsHdfNumericDataGroup[i] =
          new HdfNumericDataGroup(nextReferenceNumber++, null, null); // data will be supplied later
      dimsHdfScientificData[i] = new HdfScientificData(nextReferenceNumber++, toByteArray(dims[i]));
    }

    // ***** create fakeDims
    HdfVData fakeHdfVDatas[] = new HdfVData[rank * 2];
    HdfVDataDescription fakeHdfVDataDescriptions[] = new HdfVDataDescription[rank * 2];
    HdfVGroup fakeHdfVGroups[] = new HdfVGroup[rank * 2];
    for (int i = 0; i < rank * 2; i++) {
      fakeHdfVDatas[i] =
          new HdfVData(
              nextReferenceNumber++,
              toByteArray(
                  new int[] {
                    i < rank ? dims[rank - 1 - i].length : dims[i - rank].length
                  })); // count down...count up
      fakeHdfVDataDescriptions[i] =
          new HdfVDataDescription(
              (short)
                  (nextReferenceNumber
                      - 1), // refNumber should equal the refNumber for corresponding hdfVData
              "fakeDim" + i,
              "DimVal0.1",
              fakeValuesString,
              HdfConstants.DFNT_INT32,
              1,
              4); // 4=4bytes
      fakeHdfVGroups[i] =
          new HdfVGroup(
              nextReferenceNumber++,
              new short[] {fakeHdfVDataDescriptions[i].tagType},
              new short[] {fakeHdfVDataDescriptions[i].referenceNumber},
              "fakeDim" + i,
              "Dim0.0");
    }

    // ***** for data
    // create VData for the dataName and each data attribute
    String names[] = dataAttributes.getNames();
    HdfVData dataHdfVDataArray[] = new HdfVData[names.length];
    HdfVDataDescription dataHdfVDataDescriptionArray[] = new HdfVDataDescription[names.length];
    for (int i = 0; i < names.length; i++) {
      Object tValue = dataAttributes.get(names[i]).toObjectArray();
      byte tBytes[] = toByteArray(tValue);
      int nBytesPerItem = getNBytesPerItem(tValue);
      dataHdfVDataArray[i] = new HdfVData(nextReferenceNumber++, tBytes);
      dataHdfVDataDescriptionArray[i] =
          new HdfVDataDescription(
              (short)
                  (nextReferenceNumber
                      - 1), // refNumber should equal the refNumber for corresponding hdfVData
              names[i],
              "Attr0.0",
              otherValuesString,
              HdfConstants.getDfntType(tValue),
              tBytes.length / nBytesPerItem,
              nBytesPerItem);
    }

    // create hdfNumberType for dims and hdfScientificDataDimension for data
    HdfNumberType dataHdfNumberType =
        new HdfNumberType(nextReferenceNumber++, HdfConstants.DFNT_FLOAT64); // 32 64
    int dim_n[] = new int[rank];
    for (int i = 0; i < rank; i++)
      dim_n[i] = dims[rank - 1 - i].length; // rank-1- reverses the order
    HdfScientificDataDimension dataHdfScientificDataDimension =
        new HdfScientificDataDimension(
            (short)
                (nextReferenceNumber
                    - 1), // refNumber should equal the refNumber for corresponding hdfVNumberType
            rank,
            dim_n,
            (short) (nextReferenceNumber - 1),
            new short[] {(short) (nextReferenceNumber - 1), (short) (nextReferenceNumber - 1)});

    // create hdfNumericDataGroup for data
    dataHdfNumericDataGroup.tag_n =
        new short[] { // tagType
          dataHdfScientificData.tagType,
          dataHdfNumberType.tagType,
          dataHdfScientificDataDimension.tagType,
          0x2D1
        }; // always this number, but no info in docs and no object
    dataHdfNumericDataGroup.ref_n =
        new short[] { // referenceNumber
          dataHdfScientificData.referenceNumber,
          dataHdfNumberType.referenceNumber,
          dataHdfScientificDataDimension.referenceNumber,
          dataHdfScientificDataDimension.referenceNumber
        };

    // create hdfVGroup for data
    int tNDataAtt = dataHdfVDataDescriptionArray.length;
    short[] tDataTag_n = new short[rank + tNDataAtt + 4];
    short[] tDataRef_n = new short[rank + tNDataAtt + 4];
    for (int i = 0; i < rank; i++) {
      tDataTag_n[i] = fakeHdfVGroups[i].tagType;
      tDataRef_n[i] = fakeHdfVGroups[i].referenceNumber;
    }
    for (int i = 0; i < tNDataAtt; i++) {
      tDataTag_n[rank + i] = dataHdfVDataDescriptionArray[i].tagType;
      tDataRef_n[rank + i] = dataHdfVDataDescriptionArray[i].referenceNumber;
    }
    tDataTag_n[rank + tNDataAtt + 0] = dataHdfScientificData.tagType;
    tDataRef_n[rank + tNDataAtt + 0] = dataHdfScientificData.referenceNumber;
    tDataTag_n[rank + tNDataAtt + 1] = dataHdfNumberType.tagType;
    tDataRef_n[rank + tNDataAtt + 1] = dataHdfNumberType.referenceNumber;
    tDataTag_n[rank + tNDataAtt + 2] = dataHdfScientificDataDimension.tagType;
    tDataRef_n[rank + tNDataAtt + 2] = dataHdfScientificDataDimension.referenceNumber;
    tDataTag_n[rank + tNDataAtt + 3] = dataHdfNumericDataGroup.tagType;
    tDataRef_n[rank + tNDataAtt + 3] = dataHdfNumericDataGroup.referenceNumber;
    HdfVGroup dataHdfVGroup =
        new HdfVGroup(nextReferenceNumber++, tDataTag_n, tDataRef_n, dataName, "Var0.0");

    // ***** for each dimension
    HdfVGroup dimsHdfVGroup[] = new HdfVGroup[rank];
    HdfVData dimsHdfVDataArray[][] = new HdfVData[rank][];
    HdfVDataDescription dimsHdfVDataDescriptionArray[][] = new HdfVDataDescription[rank][];
    HdfNumberType dimsHdfNumberType[] = new HdfNumberType[rank];
    HdfScientificDataDimension dimsHdfScientificDataDimension[] =
        new HdfScientificDataDimension[rank];
    for (int dim = 0; dim < rank; dim++) {
      // create VData for each dim attribute
      names = dimAttributes[dim].getNames();
      dimsHdfVDataArray[dim] = new HdfVData[names.length];
      dimsHdfVDataDescriptionArray[dim] = new HdfVDataDescription[names.length];
      for (int i = 0; i < names.length; i++) {
        Object tValue = dimAttributes[dim].get(names[i]).toObjectArray();
        byte tBytes[] = toByteArray(tValue);
        int nBytesPerItem = getNBytesPerItem(tValue);
        dimsHdfVDataArray[dim][i] = new HdfVData(nextReferenceNumber++, tBytes);
        dimsHdfVDataDescriptionArray[dim][i] =
            new HdfVDataDescription(
                (short)
                    (nextReferenceNumber
                        - 1), // refNumber should equal the refNumber for corresponding hdfVData
                names[i],
                "Attr0.0",
                otherValuesString,
                HdfConstants.getDfntType(tValue), // the DfntType of data
                tBytes.length / nBytesPerItem,
                nBytesPerItem);
      }

      // create hdfNumberType for dim and hdfScientificDataDimension for dim
      dimsHdfNumberType[dim] =
          new HdfNumberType(nextReferenceNumber++, HdfConstants.DFNT_FLOAT64); // 32 64
      dimsHdfScientificDataDimension[dim] =
          new HdfScientificDataDimension(
              (short)
                  (nextReferenceNumber
                      - 1), // refNumber should equal the refNumber for corresponding hdfVNumberType
              (short) 1,
              new int[] {dims[dim].length},
              (short) (nextReferenceNumber - 1),
              new short[] {(short) (nextReferenceNumber - 1)});

      // create hdfNumericDataGroup for dim
      dimsHdfNumericDataGroup[dim].tag_n =
          new short[] { // tagType
            dimsHdfScientificData[dim].tagType,
            dimsHdfNumberType[dim].tagType,
            dimsHdfScientificDataDimension[dim].tagType,
            0x2D1
          }; // always this number, but no info in docs and no object
      dimsHdfNumericDataGroup[dim].ref_n =
          new short[] { // referenceNumber
            dimsHdfScientificData[dim].referenceNumber,
            dimsHdfNumberType[dim].referenceNumber,
            dimsHdfScientificDataDimension[dim].referenceNumber,
            dimsHdfScientificDataDimension[dim].referenceNumber
          };

      // create hdfVGroup for dim
      int tNAtt = dimsHdfVDataDescriptionArray[dim].length;
      short[] tDimTag_n = new short[1 + tNAtt + 4];
      short[] tDimRef_n = new short[1 + tNAtt + 4];
      tDimTag_n[0] = fakeHdfVGroups[rank + dim].tagType;
      tDimRef_n[0] = fakeHdfVGroups[rank + dim].referenceNumber;
      for (int i = 0; i < tNAtt; i++) {
        tDimTag_n[1 + i] = dimsHdfVDataDescriptionArray[dim][i].tagType;
        tDimRef_n[1 + i] = dimsHdfVDataDescriptionArray[dim][i].referenceNumber;
      }
      tDimTag_n[1 + tNAtt + 0] = dimsHdfScientificData[dim].tagType;
      tDimRef_n[1 + tNAtt + 0] = dimsHdfScientificData[dim].referenceNumber;
      tDimTag_n[1 + tNAtt + 1] = dimsHdfNumberType[dim].tagType;
      tDimRef_n[1 + tNAtt + 1] = dimsHdfNumberType[dim].referenceNumber;
      tDimTag_n[1 + tNAtt + 2] = dimsHdfScientificDataDimension[dim].tagType;
      tDimRef_n[1 + tNAtt + 2] = dimsHdfScientificDataDimension[dim].referenceNumber;
      tDimTag_n[1 + tNAtt + 3] = dimsHdfNumericDataGroup[dim].tagType;
      tDimRef_n[1 + tNAtt + 3] = dimsHdfNumericDataGroup[dim].referenceNumber;
      dimsHdfVGroup[dim] =
          new HdfVGroup(nextReferenceNumber++, tDimTag_n, tDimRef_n, dimNames[dim], "Var0.0");
    }

    // ***** for global attributes
    // create VData for each data attribute
    names = globalAttributes.getNames();
    int nGlobalAttributes = names.length;
    HdfVData globalHdfVDataArray[] = new HdfVData[nGlobalAttributes];
    HdfVDataDescription globalHdfVDataDescriptionArray[] =
        new HdfVDataDescription[nGlobalAttributes];
    for (int i = 0; i < nGlobalAttributes; i++) {
      Object tValue = globalAttributes.get(names[i]).toObjectArray();
      byte tBytes[] = toByteArray(tValue);
      int nBytesPerItem = getNBytesPerItem(tValue);
      globalHdfVDataArray[i] = new HdfVData(nextReferenceNumber++, tBytes);
      globalHdfVDataDescriptionArray[i] =
          new HdfVDataDescription(
              (short)
                  (nextReferenceNumber
                      - 1), // refNumber should equal the refNumber for corresponding hdfVData
              names[i],
              "Attr0.0",
              otherValuesString,
              HdfConstants.getDfntType(tValue),
              tBytes.length / nBytesPerItem,
              nBytesPerItem);
    }

    // create top hdfVGroup
    short[] tGlobalTag_n = new short[rank * 2 + 1 + rank + nGlobalAttributes];
    short[] tGlobalRef_n = new short[rank * 2 + 1 + rank + nGlobalAttributes];
    for (int i = 0; i < rank * 2; i++) {
      tGlobalTag_n[i] = fakeHdfVGroups[i].tagType; // ref to fake VGroups
      tGlobalRef_n[i] = fakeHdfVGroups[i].referenceNumber;
    }
    tGlobalTag_n[rank * 2] = dataHdfVGroup.tagType; // ref to data's VGroup
    tGlobalRef_n[rank * 2] = dataHdfVGroup.referenceNumber;
    for (int i = 0; i < rank; i++) {
      tGlobalTag_n[rank * 2 + 1 + i] = dimsHdfVGroup[i].tagType; // ref to dims VGroups
      tGlobalRef_n[rank * 2 + 1 + i] = dimsHdfVGroup[i].referenceNumber;
    }
    for (int i = 0; i < nGlobalAttributes; i++) { // global attributes
      tGlobalTag_n[rank * 2 + 1 + rank + i] = globalHdfVDataDescriptionArray[i].tagType;
      tGlobalRef_n[rank * 2 + 1 + rank + i] = globalHdfVDataDescriptionArray[i].referenceNumber;
    }
    String tempFileName =
        compatibleMode
            ? (hdfFileName.length() > 63 ? hdfFileName.substring(0, 63) : hdfFileName)
            : // clipped to 63 chars
            File2.getNameAndExtension(hdfFileName); // don't store the directory info
    HdfVGroup globalHdfVGroup =
        new HdfVGroup(nextReferenceNumber++, tGlobalTag_n, tGlobalRef_n, tempFileName, "CDF0.0");

    // create the end vgroup
    HdfVGroup endHdfVGroup =
        new HdfVGroup(nextReferenceNumber++, new short[0], new short[0], "RIG0.0", "RIG0.0");

    // ***** make a list of tags to be written
    String2.log("SdsWriter.create making list of tags");
    ArrayList tagList = new ArrayList();
    tagList.add(hdfLibraryVersion);
    tagList.add(dataHdfScientificData);
    for (int i = 0; i < rank; i++) tagList.add(dimsHdfScientificData[i]);

    // add fake dims
    for (int i = 0; i < rank * 2; i++) {
      tagList.add(fakeHdfVDatas[i]);
      tagList.add(fakeHdfVDataDescriptions[i]);
      tagList.add(fakeHdfVGroups[i]);
    }

    // add data tags
    for (int i = 0; i < dataHdfVDataArray.length; i++) {
      tagList.add(dataHdfVDataArray[i]);
      tagList.add(dataHdfVDataDescriptionArray[i]);
    }
    tagList.add(dataHdfNumberType);
    tagList.add(dataHdfScientificDataDimension);
    tagList.add(dataHdfNumericDataGroup);
    tagList.add(dataHdfVGroup);

    // add dim tags for each dimension
    for (int dim = 0; dim < rank; dim++) {
      for (int i = 0; i < dimsHdfVDataArray[dim].length; i++) {
        tagList.add(dimsHdfVDataArray[dim][i]);
        tagList.add(dimsHdfVDataDescriptionArray[dim][i]);
      }
      tagList.add(dimsHdfNumberType[dim]);
      tagList.add(dimsHdfScientificDataDimension[dim]);
      tagList.add(dimsHdfNumericDataGroup[dim]);
      tagList.add(dimsHdfVGroup[dim]);
    }

    // add global tags
    for (int i = 0; i < globalHdfVDataArray.length; i++) {
      tagList.add(globalHdfVDataArray[i]);
      tagList.add(globalHdfVDataDescriptionArray[i]);
    }
    tagList.add(globalHdfVGroup);

    // add the end vgroup
    tagList.add(endHdfVGroup);

    // pad with hdfNull until 200 tags
    // these are present but are not required by specs, HDFView, or CDAT
    if (compatibleMode) {
      while (tagList.size() < 200) tagList.add(hdfNull);
    }

    // ***** open the file and write the header
    String2.log("SdsWriter.create writing the file; " + Math2.memoryString());
    if (tagList.size() >= Short.MAX_VALUE)
      throw new RuntimeException(
          String2.ERROR + " in SdsWriter.create: too many tags (" + tagList.size() + ").");
    DataOutputStream stream =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(hdfFileName)));
    try {
      int dataOffset = 0;
      stream.writeInt(0x0e031301); // magic number
      stream.writeShort((short) tagList.size());
      stream.writeInt(0); // offset of next Data Descriptor Block
      dataOffset = 10;

      // write Data Descriptor block
      dataOffset += tagList.size() * (2 + 2 + 4 + 4); // tagType, ref#, offset, length
      for (int i = 0; i < tagList.size(); i++) {
        // String2.log("i=" + i + " tag=" + tagList.get(i).toString());
        HdfTag hdfTag = (HdfTag) tagList.get(i);
        boolean isHdfNull = hdfTag instanceof HdfNull;
        stream.writeShort(hdfTag.tagType);
        stream.writeShort(hdfTag.referenceNumber);
        stream.writeInt(isHdfNull ? -1 : dataOffset);
        int length = hdfTag.getLength();
        stream.writeInt(isHdfNull ? -1 : length);
        dataOffset += length;
      }

      // write the Data Blocks
      for (int i = 0; i < tagList.size(); i++) ((HdfTag) tagList.get(i)).writeData(stream);

      // write one extra 0 byte
      stream.write(0);
    } finally {
      stream.close();
    }
    String2.log("SdsWriter.create done");
  }

  /**
   * This converts a byte[], short[], int[], float[], double[], or String to a byte[].
   *
   * @param o
   * @return an array of bytes
   * @throws Exception if trouble
   */
  public static byte[] toByteArray(Object o) {
    if (o instanceof byte[]) return (byte[]) o;
    if (o instanceof short[]) return toByteArray((short[]) o);
    if (o instanceof int[]) return toByteArray((int[]) o);
    if (o instanceof float[]) return toByteArray((float[]) o);
    if (o instanceof double[]) return toByteArray((double[]) o);
    if (o instanceof String[]) o = String2.toNewlineString((String[]) o);
    if (o instanceof String) return toByteArray((String) o);

    throw new RuntimeException("SdsWriter.toByteArray unsupported object type: " + o.toString());
  }

  /**
   * This indicates the number of bytes per item (e.g., 4 for an int, 8 for a double,
   * string.length() for a string).
   *
   * @param o is a byte[], short[], int[], float[], double[], or String
   * @return an array of bytes
   * @throws Exception if trouble
   */
  public static int getNBytesPerItem(Object o) {
    if (o instanceof byte[]) return 1;
    if (o instanceof short[]) return 2;
    if (o instanceof int[]) return 4;
    if (o instanceof float[]) return 4;
    if (o instanceof double[]) return 8;
    if (o instanceof String[]) o = String2.toNewlineString((String[]) o);
    if (o instanceof String) return ((String) o).length();

    throw new RuntimeException(
        "SdsWriter.getNBytesPerItem unsupported object type: " + o.toString());
  }

  /**
   * Convert a String to an array of bytes containing just that low bytes of the characters.
   *
   * @param value
   * @return an array of bytes
   */
  public static byte[] toByteArray(String value) {
    return String2.toByteArray(value);
  }

  /**
   * Convert a byte to an array of bytes containing just that byte.
   *
   * @param value
   * @return an array of bytes
   */
  public static byte[] toByteArray(byte value) {
    return new byte[] {value};
  }

  /**
   * Convert a short to an array of bytes. The bytes for each value are stored MSB ... LSB.
   *
   * @param value
   * @return an array of bytes
   */
  public static byte[] toByteArray(short value) {
    byte byteArray[] = new byte[2];
    byteArray[1] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[0] = (byte) (value & 0xFF);
    return byteArray;
  }

  /**
   * Convert an int to an array of bytes. The bytes for each value are stored MSB ... LSB.
   *
   * @param value
   * @return an array of bytes
   */
  public static byte[] toByteArray(int value) {
    byte byteArray[] = new byte[4];
    byteArray[3] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[2] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[1] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[0] = (byte) (value & 0xFF);
    return byteArray;
  }

  /**
   * Convert a float to an array of bytes. The bytes for each value are stored MSB ... LSB.
   *
   * @param f
   * @return an array of bytes
   */
  public static byte[] toByteArray(float f) {
    byte byteArray[] = new byte[4];
    int value = Float.floatToRawIntBits(f);
    byteArray[3] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[2] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[1] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[0] = (byte) (value & 0xFF);
    return byteArray;
  }

  /**
   * Convert a double to an array of bytes. The bytes for each value are stored MSB ... LSB.
   *
   * @param d
   * @return an array of bytes
   */
  public static byte[] toByteArray(double d) {
    byte byteArray[] = new byte[8];
    long value = Double.doubleToRawLongBits(d);
    byteArray[7] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[6] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[5] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[4] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[3] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[2] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[1] = (byte) (value & 0xFF);
    value >>= 8;
    byteArray[0] = (byte) (value & 0xFF);
    return byteArray;
  }

  /**
   * Convert an array of short to an array of bytes (the bytes for array[0], the bytes for array[1],
   * ...). The bytes for each value are stored MSB ... LSB.
   *
   * @param array the array of shorts
   * @return an array of bytes
   */
  public static byte[] toByteArray(short array[]) {
    int length = array.length;
    byte byteArray[] = new byte[length * 2];
    int po = 0;
    for (int i = 0; i < length; i++) {
      int value = array[i];
      byteArray[po + 1] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po] = (byte) (value & 0xFF);
      po += 2;
    }
    return byteArray;
  }

  /**
   * Convert an array of int to an array of bytes (the bytes for array[0], the bytes for array[1],
   * ...). The bytes for each value are stored MSB ... LSB.
   *
   * @param array the array of ints
   * @return an array of bytes
   */
  public static byte[] toByteArray(int array[]) {
    int length = array.length;
    byte byteArray[] = new byte[length * 4];
    int po = 0;
    for (int i = 0; i < length; i++) {
      int value = array[i];
      byteArray[po + 3] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 2] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 1] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po] = (byte) (value & 0xFF);
      po += 4;
    }
    return byteArray;
  }

  /**
   * Convert an array of float to an array of bytes (the bytes for array[0], the bytes for array[1],
   * ...). The bytes for each value are stored MSB ... LSB.
   *
   * @param array the array of floats
   * @return an array of bytes
   */
  public static byte[] toByteArray(float array[]) {
    // to save memory, this does not create an intermediate array of ints
    int length = array.length;
    byte byteArray[] = new byte[length * 4];
    int po = 0;
    for (int i = 0; i < length; i++) {
      int value = Float.floatToRawIntBits(array[i]);
      byteArray[po + 3] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 2] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 1] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po] = (byte) (value & 0xFF);
      po += 4;
    }
    return byteArray;
  }

  /**
   * Convert an array of double to an array of bytes (the bytes for array[0], the bytes for
   * array[1], ...). The bytes for each value are stored MSB ... LSB.
   *
   * @param array the array of doubles
   * @return an array of bytes
   */
  public static byte[] toByteArray(double array[]) {
    // to save memory, this does not create an intermediate array of longs
    int length = array.length;
    byte byteArray[] = new byte[length * 8];
    int po = 0;
    for (int i = 0; i < length; i++) {
      long value = Double.doubleToRawLongBits(array[i]);
      byteArray[po + 7] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 6] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 5] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 4] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 3] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 2] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po + 1] = (byte) (value & 0xFF);
      value >>= 8;
      byteArray[po] = (byte) (value & 0xFF);
      po += 8;
    }
    return byteArray;
  }

  /**
   * THIS DID WORK BUT I AM NOT MAINTAINING IT because it sought to perfectly duplicate the native
   * library byte-for-byte which is unnecessary and goofy since the native library did goofy things,
   * so this method probably doesn't work now. This is a unit test of this class and the HdfXxx
   * classes.
   *
   * @param args is ignored
   * @throws Exception if trouble
   */
  public static void main(String args[]) throws Exception {

    // describe the data
    String2.log("SdsWriter.main gathering data and attributes");
    int NaN = -999;
    String resourcePath = Resources.getResource("gov/noaa/pfel/coastwatch/hdf/").toString();
    String dir = File2.accessResourceFile(resourcePath);
    double lonSpacing = 0.25;
    double latSpacing = 0.5;
    double lon[] = {0, .25, 0.5, 0.75};
    double lat[] = {0, 0.5, 1};
    int nLon = 4;
    int nLat = 3;
    // double z[] = {1, 2, 3, 4, NaN, 6, 7, 8, 9, 10, 11, 12};
    double z[] = {3, 6, 9, 12, 2, NaN, 8, 11, 1, 4, 7, 10}; // odd order to match original mini.hdf

    int passDate[] = new int[] {0x32b0}; // {0x32b0}; //{12806, 12807, 12808};
    double startTime[] =
        new double[] {Double.longBitsToDouble(0x40f517f000000000L)}; // {0, 0, 86399};

    // determine et_affine transformation
    // long = a*row + c*col + e
    // lat = b*row + d*col + f
    double matrix[] = {0, -latSpacing, lonSpacing, 0, lon[0], lat[nLat - 1]};

    Attributes globalAttributes = new Attributes();
    globalAttributes.set("satellite", "Quikscat spacecraft");
    globalAttributes.set("sensor", "SeaWinds Near Real Time");
    globalAttributes.set("origin", "NASA/JPL");
    globalAttributes.set("history", "unknown");
    globalAttributes.set("cwhdf_version", "3.2");
    globalAttributes.set("pass_type", "day/night");
    globalAttributes.set("composite", "true");
    globalAttributes.set("pass_date", new IntArray(passDate)); // int32[nDays]
    globalAttributes.set("start_time", new DoubleArray(startTime)); // float64[nDays]
    globalAttributes.set("projection_type", "mapped");
    globalAttributes.set("projection", "geographic");
    globalAttributes.set("gctp_sys", 0); // int32
    globalAttributes.set("gctp_zone", 0); // int32
    globalAttributes.set("gctp_parm", new DoubleArray(new double[15])); // float64[15 0's]
    globalAttributes.set("gctp_datum", 12); // int32 12=WGS84
    globalAttributes.set("et_affine", new DoubleArray(matrix)); // float64[] {a, b, c, d, e, f}
    globalAttributes.set("rows", nLat); // int32 number of rows
    globalAttributes.set("cols", nLon); // int32 number of columns

    // data attributes
    Attributes dataAttributes = new Attributes();
    dataAttributes.set("long_name", "Zonal Wind"); // string
    dataAttributes.set("units", "m s-1"); // string
    dataAttributes.set("coordsys", "geographic"); // string
    dataAttributes.set("_FillValue", Double.NaN); // same type as data as z
    dataAttributes.set("missing_value", Double.NaN); // same type as data as z
    dataAttributes.set("scale_factor", 1.0); // float64
    dataAttributes.set("scale_factor_err", 0.0); // float64
    dataAttributes.set("add_offset", 0.0); // float64 calibration offset
    dataAttributes.set("add_offset_err", 0.0); // float64 calibration error
    dataAttributes.set("calibrated_nt", 0); // int32 hdf data type code for uncalibrated data
    dataAttributes.set("fraction_digits", 2); // int32

    // dim0 attributes (lon)
    Attributes dim0Attributes = new Attributes();
    dim0Attributes.set("_CoordinateAxisType", "Lon");
    dim0Attributes.set("long_name", "Longitude"); // string
    dim0Attributes.set("units", "degrees_east"); // string
    dim0Attributes.set("coordsys", "geographic"); // string
    dim0Attributes.set("scale_factor", 1.0); // float64
    dim0Attributes.set("scale_factor_err", 0.0); // float64
    dim0Attributes.set("add_offset", 0.0); // float64 calibration offset
    dim0Attributes.set("add_offset_err", 0.0); // float64 calibration error
    dim0Attributes.set("calibrated_nt", 0); // int32 hdf data type code for uncalibrated data
    dim0Attributes.set("fraction_digits", 2); // int32

    // dim1 attributes (lat)
    // it adds long_name
    Attributes dim1Attributes = new Attributes();
    dim1Attributes.set("_CoordinateAxisType", "Lat");
    dim1Attributes.set("long_name", "Latitude"); // string
    dim1Attributes.set("units", "degrees_north"); // string
    dim1Attributes.set("coordsys", "geographic"); // string
    dim1Attributes.set("scale_factor", 1.0); // float64
    dim1Attributes.set("scale_factor_err", 0.0); // float64
    dim1Attributes.set("add_offset", 0.0); // float64 calibration offset
    dim1Attributes.set("add_offset_err", 0.0); // float64 calibration error
    dim1Attributes.set("calibrated_nt", 0); // int32 hdf data type code for uncalibrated data
    dim1Attributes.set("fraction_digits", 2); // int32

    // create the file
    String2.log("SdsWriter.main create mini2.hdf");
    SdsWriter.compatibleMode = false; // must be 'true' for comparisons to mini.hdf below
    SdsWriter.create(
        dir + "mini2.hdf",
        new double[][] {lon, lat},
        new String[] {"Longitude", "Latitude"},
        new Attributes[] {dim0Attributes, dim1Attributes},
        z,
        "Zonal Wind",
        dataAttributes,
        globalAttributes);

    // regenerate mini.hdf.dump  (e.g., if I changed the format of a HdfXxx.toString method)
    // String2.setupLog(false, false, dir + "mini.hdf.dump", false, String2.logFileDefaultMaxSize);
    // //capture results to file
    // SdsReader.verbose = true;  //so verbose results get sent to file
    // SdsReader.read(dir + "mini.hdf");

    // now try to read it
    String2.log("SdsWriter.main read mini2.hdf and generate mini2.hdf.dump file");
    String2.setupLog(
        false,
        false,
        dir + "mini2.hdf.dump",
        false,
        String2.logFileDefaultMaxSize); // capture results to file
    SdsReader.verbose = true; // so verbose results get sent to file
    SdsReader sdsReader = new SdsReader(dir + "mini2.hdf");

    // look for differences
    String2.setupLog(true, false, "", false, String2.logFileDefaultMaxSize);
    String2.log("SdsWriter.main look for dump differences"); // after close log files
    String miniDump[] = File2.readFromFile88591(dir + "mini.hdf.dump");
    String mini2Dump[] = File2.readFromFile88591(dir + "mini2.hdf.dump");
    Test.ensureEqual(miniDump[0].length(), 0, String2.ERROR + " reading mini.hdf.dump");
    Test.ensureEqual(mini2Dump[0].length(), 0, String2.ERROR + " reading mini2.hdf.dump");
    Test.ensureEqual(miniDump[1], mini2Dump[1], "mini.hdf.dump and mini2.hdf.dump are different");

    // are they the same byte for byte?
    String2.log("SdsWriter.main look byte-by-byte for differences");
    String miniHexDump = File2.hexDump(dir + "mini.hdf", Integer.MAX_VALUE);
    String mini2HexDump = File2.hexDump(dir + "mini2.hdf", Integer.MAX_VALUE);
    Test.ensureEqual(miniHexDump, mini2HexDump, "mini.hdf and mini2.hdf hex dumps are different");

    /*
    //read a standard file
    String2.setupLog(true, false, dir + "OQNux10.hdf.dump",
        false, String2.logFileDefaultMaxSize); //append
    SdsReader.verbose = true;
    SdsReader.read(File2.getClassPath() + //with / separator and / at the end
        "gov/noaa/pfel/coastwatch/griddata/OQNux101day_20050712_W-135E-105S22N50.hdf");
    */

    String2.log("SdsWriter.main finished successfully");
  }
}
