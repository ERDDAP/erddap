package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.IntArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.util.DataStream;
import java.io.DataOutputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

class MatlabTests {

  @TempDir private static Path TEMP_DIR;

  /** This tests the methods in this class. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    Matlab.verbose = true;
    String dir = TEMP_DIR.toAbsolutePath().toString() + "/";

    // write a double matlab file
    String tempFile = dir + "MatlabDouble.mat";
    DataOutputStream dos = DataStream.getDataOutputStream(tempFile);
    try {
      Matlab.writeMatlabHeader(dos);
      double dLat[] = {1.1, 2.2, 3.3};
      double dLon[] = {44.4, 55.5};
      double dData[][] = /* new double[3][2] */ {{1.11, 2.22}, {33.33, 44.44}, {555.55, 666.66}};
      Matlab.writeDoubleArray(dos, "lon", dLon);
      Matlab.writeDoubleArray(dos, "lat", dLat);
      Matlab.write2DDoubleArray(dos, "MyData", dData);
    } finally {
      dos.close();
    }
    if (Matlab.verbose) String2.log(File2.hexDump(tempFile, 256)); // uses an external class!
    Matlab.readMatlabFile(tempFile);

    // write a float matlab file
    tempFile = dir + "MatlabFloat.mat";
    dos = DataStream.getDataOutputStream(tempFile);
    try {
      Matlab.writeMatlabHeader(dos);
      float fLat[] = {1.1f, 2.2f, 3.3f};
      float fLon[] = {44.4f, 55.5f};
      float fData[][] = /* new float[3][2] */ {
        {1.11f, 2.22f}, {33.33f, 44.44f}, {555.55f, 666.66f}
      };
      Matlab.writeFloatArray(dos, "lon", fLon);
      Matlab.writeFloatArray(dos, "lat", fLat);
      Matlab.write2DFloatArray(dos, "MyData", fData);
    } finally {
      dos.close();
    }
    if (Matlab.verbose) String2.log(File2.hexDump(tempFile, 256));
    Matlab.readMatlabFile(tempFile);

    // write an NDimensional int array matlab file
    tempFile = dir + "MatlabInt.mat";
    dos = DataStream.getDataOutputStream(tempFile);
    Matlab.writeMatlabHeader(dos);
    IntArray ia = (IntArray) PrimitiveArray.csvFactory(PAType.INT, "1,2,3,4,5,6");
    NDimensionalIndex ndIndex = new NDimensionalIndex(new int[] {2, 3});
    Matlab.writeNDimensionalArray(dos, "MyInts", ia, ndIndex);
    dos.close();
    if (Matlab.verbose) String2.log(File2.hexDump(tempFile, 256));
    Matlab.readMatlabFile(tempFile);

    // write an NDimensional String array matlab file
    tempFile = dir + "MatlabString.mat";
    dos = DataStream.getDataOutputStream(tempFile);
    Matlab.writeMatlabHeader(dos);
    StringArray sa = (StringArray) PrimitiveArray.csvFactory(PAType.STRING, "a, simple, test");
    ndIndex = Matlab.make2DNDIndex(sa);
    Matlab.writeNDimensionalArray(dos, "MyStrings", sa, ndIndex);
    dos.close();
    if (Matlab.verbose) String2.log(File2.hexDump(tempFile, 512));
    Matlab.readMatlabFile(tempFile);

    // in use in SSR (only works on computer with GMT)
    // SSR.makeDataFile(
    // SSR.getTempDirectory(), "OpenDAPAGssta1day20050308.W-135.E-113.S30.N50",
    // //without .grd
    // SSR.MakeMat, "ssta", true, false);

  }
}
