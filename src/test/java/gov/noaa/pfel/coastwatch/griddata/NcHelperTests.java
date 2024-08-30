package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.junit.jupiter.api.io.TempDir;
import tags.TagMissingFile;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

class NcHelperTests {

  @TempDir private static Path TEMP_DIR;

  /** This tests the methods in this class. */
  @org.junit.jupiter.api.Test
  void testBasic() throws Throwable {
    String2.log("\n*** NcHelper.testBasic...");
    String fullName;

    // getArray get1DArray, get1DArrayLength
    Object o;
    o = new String[] {"5.5", "7.77"};
    Array array = NcHelper.get1DArray(o, false);
    Test.ensureTrue(array instanceof ArrayChar.D2, "get1DArray a");
    Test.ensureEqual(NcHelper.get1DArrayLength(array), 2, "get1DArrayLength a");
    o = NcHelper.getPrimitiveArray(array);
    Test.ensureTrue(o instanceof StringArray, "getArray a; o=" + o.toString());
    StringArray sar = (StringArray) o;
    Test.ensureEqual(sar.size(), 2, "");
    Test.ensureEqual(sar.get(0), "5.5", "");
    Test.ensureEqual(sar.get(1), "7.77", "");

    /*
     * ArrayChar.D3 ac3 = new ArrayChar.D3(2, 3, 5);
     * Index index = ac3.getIndex();
     * index.set(0, 0); ac3.setString(index, "a");
     * index.set(0, 1); ac3.setString(index, "bb");
     * index.set(0, 2); ac3.setString(index, "ccc");
     * index.set(1, 0); ac3.setString(index, "dddd");
     * index.set(1, 1); ac3.setString(index, "e");
     * index.set(1, 2); ac3.setString(index, "f");
     * o = getArray(ac3);
     * Test.ensureTrue(o instanceof String[], "getArray a; o=" + o.toString());
     * sar = (String[])o;
     * Test.ensureEqual(sar.length, 6, "");
     * Test.ensureEqual(sar[0], "a", "");
     * Test.ensureEqual(sar[1], "bb", "");
     * Test.ensureEqual(sar[2], "ccc", "");
     * Test.ensureEqual(sar[3], "dddd", "");
     * Test.ensureEqual(sar[4], "e", "");
     * Test.ensureEqual(sar[5], "f", "");
     */

    o = new byte[] {(byte) 2, (byte) 9};
    array = NcHelper.get1DArray(o, false);
    Test.ensureTrue(array instanceof ArrayByte.D1, "get1DArray b");
    Test.ensureEqual(NcHelper.get1DArrayLength(array), 2, "get1DArrayLength a");
    o = NcHelper.getPrimitiveArray(array);
    Test.ensureTrue(o instanceof ByteArray, "getArray b");
    ByteArray bar = (ByteArray) o;
    Test.ensureEqual(bar.size(), 2, "");
    Test.ensureEqual(bar.get(0), 2, "");
    Test.ensureEqual(bar.get(1), 9, "");

    o = new double[] {2.2, 9.9};
    array = NcHelper.get1DArray(o, false);
    Test.ensureTrue(array instanceof ArrayDouble.D1, "get1DArray c");
    Test.ensureEqual(NcHelper.get1DArrayLength(array), 2, "get1DArrayLength a");
    o = NcHelper.getPrimitiveArray(array);
    Test.ensureTrue(o instanceof DoubleArray, "getArray c");
    DoubleArray dar = (DoubleArray) o;
    Test.ensureEqual(dar.size(), 2, "");
    Test.ensureEqual(dar.get(0), 2.2, "");
    Test.ensureEqual(dar.get(1), 9.9, "");

    // test readWritePAsInNc
    ByteArray ba = new ByteArray(new byte[] {1, 2, 4, 7});
    ShortArray sha = new ShortArray(new short[] {100, 200, 400, 700});
    IntArray ia = new IntArray(new int[] {-1, 0, 1});
    LongArray la = new LongArray(new long[] {-10L, 0, 10L});
    char charAr[] = new char[65536];
    for (int i = 0; i < 65536; i++) charAr[i] = (char) i;
    CharArray ca = new CharArray(charAr);
    FloatArray fa = new FloatArray(new float[] {-1.1f, 2.2f, 5.5f, Float.NaN});
    DoubleArray da = new DoubleArray(new double[] {1.1, 2.2, 9.9, Double.NaN});
    StringArray sa = new StringArray();
    for (int i = 0; i < 65536; i++)
      sa.add("a" + (i == 8 ? " " : (char) i) + "z"); // backspace not saved
    // write to file
    fullName = TEMP_DIR.toAbsolutePath().toString() + "/PAsInNc.nc";
    File2.delete(fullName); // for test, make double sure it doesn't already exist
    StringArray varNames =
        new StringArray(new String[] {"ba", "sha", "ia", "la", "ca", "fa", "da", "sa"});
    PrimitiveArray pas[] = new PrimitiveArray[] {ba, sha, ia, la, ca, fa, da, sa};
    NcHelper.writePAsInNc3(fullName, varNames, pas);
    // read from file
    StringArray varNames2 = new StringArray();
    PrimitiveArray pas2[] = NcHelper.readPAsInNc3(fullName, null, varNames2);
    Test.ensureEqual(varNames, varNames2, "");
    Test.ensureEqual(pas.length, pas2.length, "");
    for (int i = 0; i < pas.length; i++) {
      int which = varNames2.indexOf(varNames.get(i));
      PrimitiveArray pa2 = pas2[which];
      Test.ensureEqual(pas[i].elementTypeString(), pa2.elementTypeString(), "i=" + i);
      if (pas[i].elementType() == PAType.STRING) {
        for (int j = 0; j < pas[i].size(); j++)
          Test.ensureEqual(
              String2.toJson(pas[i].getString(j)),
              String2.toJson(pa2.getString(j)),
              "i=" + i + " j=" + j);
      } else {
        Test.ensureEqual(pas[i].toJsonCsvString(), pa2.toJsonCsvString(), "i=" + i);
      }
    }

    pas2 = NcHelper.readPAsInNc3(fullName, new String[] {"sa"}, varNames2);
    Test.ensureEqual(varNames2.size(), 1, "");
    Test.ensureEqual(varNames2.get(0), "sa", "");
    Test.ensureEqual(pas2.length, 1, "");
    Test.ensureEqual(pas[varNames.indexOf("sa")].toJsonCsvString(), pas2[0].toJsonCsvString(), "");

    // test writeAttributesToNc
    fullName = TEMP_DIR.toAbsolutePath().toString() + "/AttsInNc.nc";
    Attributes atts = new Attributes();
    for (int i = 0; i < pas.length; i++) atts.set(varNames.get(i), pas[i]);
    NcHelper.writeAttributesToNc3(fullName, atts);
    // read 1
    PrimitiveArray pa = NcHelper.readAttributeFromNc(fullName, "sa");
    Test.ensureEqual(sa.elementTypeString(), pa.elementTypeString(), "");
    Test.ensureEqual(sa, pa, "");
    pa = NcHelper.readAttributeFromNc(fullName, "ia");
    Test.ensureEqual(ia.elementTypeString(), pa.elementTypeString(), "");
    Test.ensureEqual(ia, pa, "");
    // read many
    pas2 = NcHelper.readAttributesFromNc3(fullName, varNames.toArray());
    for (int i = 0; i < pas.length; i++) {
      Test.ensureEqual(pas[i].elementTypeString(), pas2[i].elementTypeString(), "i=" + i);
      Test.ensureEqual(pas[i].toJsonCsvString(), pas2[i].toJsonCsvString(), "i=" + i);
    }
    // read all
    atts = NcHelper.readAttributesFromNc3(fullName);
    for (int i = 0; i < varNames.size(); i++) {
      pa = atts.get(varNames.get(i));
      Test.ensureEqual(pas[i].elementTypeString(), pa.elementTypeString(), "i=" + i);
      Test.ensureEqual(pas[i].toJsonCsvString(), pa.toJsonCsvString(), "i=" + i);
    }
    // test fail to read non-existent file
    String testPath =
        Paths.get(TEMP_DIR.toAbsolutePath().toString(), "AttsInNc.nczztop").toString();
    try {
      pa = NcHelper.readAttributeFromNc(fullName + "zztop", "sa");
      throw new RuntimeException("shouldn't get here");
    } catch (Exception e) {
      if (!e.toString().contains("java.io.FileNotFoundException: " + testPath)) {
        throw e;
      }
    }

    try {
      pas2 = NcHelper.readAttributesFromNc3(fullName + "zztop", varNames.toArray());
      throw new RuntimeException("shouldn't get here");
    } catch (Exception e) {
      if (!e.toString().contains("java.io.FileNotFoundException: " + testPath)) {
        throw e;
      }
    }

    try {
      atts = NcHelper.readAttributesFromNc3(fullName + "zztop");
      throw new RuntimeException("shouldn't get here");
    } catch (Exception e) {
      if (!e.toString().contains("java.io.FileNotFoundException: " + testPath)) {
        throw e;
      }
    }

    // test fail to read non-existent var
    try {
      pa = NcHelper.readAttributeFromNc(fullName, "zztop");
      throw new RuntimeException("shouldn't get here");
    } catch (Exception e) {
      if (e.toString()
              .indexOf(
                  String2.ERROR
                      + ": Expected variable #0 not found while reading "
                      + TEMP_DIR.toAbsolutePath().toString()
                      + "/AttsInNc.nc (loadVarNames=zztop).")
          < 0) throw e;
    }

    try {
      pas2 = NcHelper.readAttributesFromNc3(fullName, new String[] {"zztop"});
      throw new RuntimeException("shouldn't get here");
    } catch (Exception e) {
      if (e.toString()
              .indexOf(
                  String2.ERROR
                      + ": Expected variable #0 not found while reading "
                      + TEMP_DIR.toAbsolutePath().toString()
                      + "/AttsInNc.nc (loadVarNames=zztop).")
          < 0) throw e;
    }

    // test if defining >2GB throws exception
    fullName = TEMP_DIR.toAbsolutePath().toString() + "/TooBig.nc";
    File2.delete(fullName);
    NetcdfFormatWriter ncWriter = null;

    // *** test writing Strings to nc files
    fullName = TEMP_DIR.toAbsolutePath().toString() + "/StringsInNc.nc";
    File2.delete(fullName);
    ncWriter = null;
    try {
      NetcdfFormatWriter.Builder ncOut = NetcdfFormatWriter.createNewNetcdf3(fullName);
      // "define" mode
      Group.Builder rootGroup = ncOut.getRootGroup();
      ncOut.setFill(false);
      Dimension dim0 = NcHelper.addDimension(rootGroup, "dim0", 2);
      Dimension dim1 = NcHelper.addDimension(rootGroup, "dim1", 3);
      ArrayList<Dimension> dims = new ArrayList<Dimension>();
      dims.add(dim0);
      dims.add(dim1);
      NcHelper.addNc3StringVariable(
          rootGroup, "s1", dims, 4); // test strLength not long enough for all strings!
      Array ar;

      // "create" mode
      ncWriter = ncOut.build();
      String sa6[] = {"", "a", "abcd", "abc", "abcd", "abcd"};
      Variable s1Var = ncWriter.findVariable("s1");

      // this fails (as expected and desired):
      // so origin and ar must be correct nDimensions and size
      // ar = Array.factory(DataType.STRING, new int[]{6}, sa6);
      // ncOut.writeStringDataToChar(s1, new int[]{0}, ar);

      ar = Array.factory(DataType.STRING, new int[] {2, 3}, sa6);
      ncWriter.writeStringDataToChar(s1Var, new int[] {0, 0}, ar);
      ncWriter.close();
      ncWriter = null;
    } catch (Exception e) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(fullName);
        ncWriter = null;
      }
    }

    // Strings are truncated to maxCharLength specified in "define" mode.
    String results = NcHelper.ncdump(fullName, "");
    String expected =
        "netcdf StringsInNc.nc {\n"
            + "  dimensions:\n"
            + "    dim0 = 2;\n"
            + "    dim1 = 3;\n"
            + "    s1_strlen = 4;\n"
            + "  variables:\n"
            + "    char s1(dim0=2, dim1=3, s1_strlen=4);\n"
            + "\n"
            + "\n"
            + "  data:\n"
            + "    s1 = \n"
            + "      {  \"\",   \"a\",   \"abcd\",  \"abc\",   \"abcd\",   \"abcd\"\n"
            + "      }\n"
            + "}\n";
    // String2.log("results=\n" + results);
    Test.ensureEqual(results, expected, "");
    File2.delete(fullName);

    // *** test writing too-long Strings to nc files
    // Must the char[][] be the exact right size? What if too long?
    fullName = TEMP_DIR.toAbsolutePath().toString() + "/StringsInNc2.nc";
    File2.delete(fullName);
    ncWriter = null;
    try {
      NetcdfFormatWriter.Builder ncOut = NetcdfFormatWriter.createNewNetcdf3(fullName);
      // "define" mode
      Group.Builder rootGroup = ncOut.getRootGroup();
      ncOut.setFill(false);
      Dimension dim0 = NcHelper.addDimension(rootGroup, "dim0", 2);
      Dimension dim1 = NcHelper.addDimension(rootGroup, "dim1", 3);
      ArrayList<Dimension> dims = new ArrayList<Dimension>();
      dims.add(dim0);
      dims.add(dim1);
      NcHelper.addNc3StringVariable(
          rootGroup, "s1", dims, 4); // test strLength not long enough for all strings!
      Array ar;

      // "create" mode
      ncWriter = ncOut.build();
      String sa6[] = {"", "a", "abcde", "abc", "abcd", "abcde"};
      Variable s1Var = ncWriter.findVariable("s1");

      // this fails (as expected and desired):
      // so origin and ar must be correct nDimensions and size
      // ar = Array.factory(DataType.STRING, new int[]{6}, sa6);
      // ncOut.writeStringDataToChar(s1, new int[]{0}, ar);

      ar = Array.factory(DataType.STRING, new int[] {2, 3}, sa6);
      ncWriter.writeStringDataToChar(s1Var, new int[] {0, 0}, ar);
      ncWriter.close();
      ncWriter = null;
    } catch (Exception e) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(fullName);
        ncWriter = null;
      }
    }

    // Strings are truncated to maxCharLength specified in "define" mode.
    results = NcHelper.ncdump(fullName, "");
    expected =
        "netcdf StringsInNc2.nc {\n"
            + "  dimensions:\n"
            + "    dim0 = 2;\n"
            + "    dim1 = 3;\n"
            + "    s1_strlen = 4;\n"
            + "  variables:\n"
            + "    char s1(dim0=2, dim1=3, s1_strlen=4);\n"
            + "\n"
            + "\n"
            + "  data:\n"
            + "    s1 = \n"
            + "      {  \"\",   \"a\",   \"abcd\",  \"abc\",   \"abcd\",   \"abcd\"\n"
            + "      }\n"
            + "}\n";
    // String2.log("results=\n" + results);
    Test.ensureEqual(results, expected, "");

    // nc chars are essentially unsigned bytes!
    // String2.log(File2.hexDump(fullName, 1000000));
    File2.delete(fullName);

    // 2021-01-06 This worked (ie netcdf-java refused to create the file)
    // until netcdf-java v5.4.1. I reported to Sean Arms.
    // (Chris) I'm commenting out the below during the move to JUnit. Based on my
    // reading of the NetCDF Users Guide,
    // https://docs.unidata.ucar.edu/nug/current/file_structure_and_performance.html
    // I don't think this is really an error. Besides that, this is testing the
    // NetCDF
    // library, not the ERDDAP code.
    // if (true) { // this is normally true
    // try {
    // NetcdfFormatWriter.Builder ncOut = NetcdfFormatWriter.createNewNetcdf3(
    // fullName)
    // .setFormat(NetcdfFileFormat.NETCDF3) // this is default. I'm just testing.
    // .setFill(false);

    // // "define" mode 2vars * 3000*50000*8bytes = 2,400,000,000
    // Group.Builder rootGroup = ncOut.getRootGroup();

    // Dimension dim0 = NcHelper.addDimension(rootGroup, "dim0", 3000);
    // Dimension dim1 = NcHelper.addDimension(rootGroup, "dim1", 50000);
    // List<Dimension> dims = Arrays.asList(dim0, dim1);
    // NcHelper.addVariable(rootGroup, "d1", DataType.DOUBLE, dims);
    // NcHelper.addVariable(rootGroup, "d2", DataType.DOUBLE, dims);

    // // define a var above 2GB (This causes the exception.)
    // NcHelper.addVariable(rootGroup, "b3", DataType.BYTE, Arrays.asList(dim0));

    // // "create" mode (and error isn't triggered till here)
    // ncWriter = ncOut.build();

    // ncWriter.close(); // it calls flush() and doesn't like flush called
    // separately
    // ncWriter = null;
    // if (true)
    // throw new RuntimeException(
    // "Shouldn't get here (It let me create nc3 file >2GB!) . fileSize=" +
    // File2.length(fullName));

    // } catch (Throwable t) {
    // String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE +
    // MustBe.throwableToString(t));
    // if (ncWriter != null) {
    // try {
    // ncWriter.abort();
    // } catch (Exception e9) {
    // }
    // File2.delete(fullName);
    // ncWriter = null;
    // }

    // String msg = t.toString();
    // String2.log("Intentional error (should be: not allowed to create nc3 file
    // >2GB):\n" + msg);

    // if (!msg.equals(
    // "java.lang.IllegalArgumentException: Variable starting pos=2400000172 " +
    // "may not exceed 2147483647"))
    // Test.knownProblem("netcdf-java 5.4.1+ allows creation of nc3 files >2GB!\n" +
    // "I reported this to Sean Arms 2021-01-06.", t);
    // } finally {
    // File2.delete(fullName);
    // }
    // }

  }

  /**
   * Test findAllVariablesWithDims.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testFindAllVariablesWithDims() throws Exception {
    StringArray sa = new StringArray();
    NetcdfFile ncFile =
        NcHelper.openFile(
            NcHelperTests.class.getResource("/data/nodcTemplates/ncCFMA2a.nc").getFile());
    try {
      Variable vars[] = NcHelper.findAllVariablesWithDims(ncFile);
      for (int v = 0; v < vars.length; v++) sa.add(vars[v].getFullName());
      sa.sort();
    } finally {
      ncFile.close();
    }
    String results = sa.toString();
    String expected = "bottle_posn, cast, cruise_id, latitude, longitude, ship, temperature0, time";
    Test.ensureEqual(results, expected, "results=" + results);
  }

  /** This is the test that Bob sent to Sean. It only uses netcdf-java methods, not NcHelper. */
  @org.junit.jupiter.api.Test
  void testReadStructure() throws Throwable {
    String fileName = NcHelperTests.class.getResource("/data/nc/SDScompound.h5").getFile();
    // System.out.println(NcHelper.ncdump(fileName, "-v ArrayOfStructures"));
    NetcdfFile nc = NetcdfFiles.open(fileName);
    try {
      // System.out.println(nc.toString());
      Variable v = nc.findVariable("ArrayOfStructures");
      if (v instanceof Structure s) {
        // System.out.println("v=" + v);
        StructureMembers sm = s.makeStructureMembers();
        // System.out.println("sm=" + sm);
        StructureMembers.Member smma = sm.findMember("a_name");
        StructureMembers.Member smmb = sm.findMember("b_name");
        StructureMembers.Member smmc = sm.findMember("c_name");
        // boolean buildStringsFromChars = false;
        // boolean isUnsigned = false;
        StructureDataIterator it = s.getStructureIterator();
        int recNo = 0;
        try {
          while (it.hasNext()) {
            StructureData sd = it.next();
            // System.out.println("byName recNo=" + recNo +
            // " a_name=" + sd.getScalarInt("a_name") +
            // " b_name=" + sd.getScalarFloat("b_name") +
            // " c_name=" + sd.getScalarDouble("c_name"));
            // System.out.println("byMem recNo=" + recNo +
            // " a_name=" + sd.getScalarInt(smma) +
            // " b_name=" + sd.getScalarFloat(smmb) +
            // " c_name=" + sd.getScalarDouble(smmc));
            recNo++;
          }
        } finally {
          it.close();
        }
      }

    } finally {
      nc.close();
    }
  }

  /** ERDDAP: require that all vars be in same structure */
  @org.junit.jupiter.api.Test
  void testReadStructure2() throws Throwable {
    String fileName = NcHelperTests.class.getResource("/data/nc/SDScompound.h5").getFile();
    // System.out.println(NcHelper.ncdump(fileName, "-v ArrayOfStructures"));
    NetcdfFile nc = NetcdfFiles.open(fileName);
    try {
      String memberNames[] = new String[] {"a_name", "b_name", "c_name"};
      PrimitiveArray pa[] =
          NcHelper.readStructure(nc, "ArrayOfStructures", memberNames, IntArray.fromCSV("0,1,9"));
      Test.ensureEqual(pa[0].toString(), "0, 1, 2, 3, 4, 5, 6, 7, 8, 9", "a_name");
      Test.ensureEqual(
          pa[1].toString(), "0.0, 1.0, 4.0, 9.0, 16.0, 25.0, 36.0, 49.0, 64.0, 81.0", "b_name");
      Test.ensureEqual(
          pa[2].toString(),
          "1.0, 0.5, 0.3333333333333333, 0.25, 0.2, 0.16666666666666666, 0.14285714285714285, 0.125, 0.1111111111111111, 0.1",
          "c_name");

      pa = NcHelper.readStructure(nc, "ArrayOfStructures", memberNames, IntArray.fromCSV("2,3,9"));
      Test.ensureEqual(pa[0].toString(), "2, 5, 8", "a_name");
      Test.ensureEqual(pa[1].toString(), "4.0, 25.0, 64.0", "b_name");
      Test.ensureEqual(
          pa[2].toString(),
          "0.3333333333333333, 0.16666666666666666, 0.1111111111111111",
          "c_name");

      pa = NcHelper.readStructure(nc, "ArrayOfStructures", memberNames, IntArray.fromCSV("2,3,8"));
      Test.ensureEqual(pa[0].toString(), "2, 5, 8", "a_name");
      Test.ensureEqual(pa[1].toString(), "4.0, 25.0, 64.0", "b_name");
      Test.ensureEqual(
          pa[2].toString(),
          "0.3333333333333333, 0.16666666666666666, 0.1111111111111111",
          "c_name");
    } finally {
      nc.close();
    }
  }

  /** This is a test of unlimitedDimension */
  @org.junit.jupiter.api.Test
  void testUnlimited() throws Exception {
    String testUnlimitedFileName =
        Path.of(NcHelperTests.class.getResource("/data/unlimited.nc").toURI()).toString();
    String2.log("\n* Projects.testUnlimited() " + testUnlimitedFileName);
    int strlen = 6;
    int row = -1;
    String results, expected;
    NetcdfFormatWriter ncWriter = null;
    try {
      NetcdfFormatWriter.Builder file = NetcdfFormatWriter.createNewNetcdf3(testUnlimitedFileName);
      Group.Builder rootGroup = file.getRootGroup();

      // add the unlimited time dimension
      // Dimension timeDim = file.addUnlimitedDimension("time");

      // alternative way to add the unlimited dimension
      Dimension timeDim =
          Dimension.builder().setName("time").setIsUnlimited(true).setLength(0).build();
      rootGroup.addDimension(timeDim);

      ArrayList<Dimension> dims = new ArrayList<Dimension>();
      dims.add(timeDim);

      // define Variables
      Variable.Builder timeVar = NcHelper.addVariable(rootGroup, "time", DataType.DOUBLE, dims);
      timeVar.addAttribute(new Attribute("units", "seconds since 1970-01-01"));

      Variable.Builder latVar = NcHelper.addVariable(rootGroup, "lat", DataType.DOUBLE, dims);
      latVar.addAttribute(new Attribute("units", "degrees_north"));

      Variable.Builder lonVar = NcHelper.addVariable(rootGroup, "lon", DataType.DOUBLE, dims);
      lonVar.addAttribute(new Attribute("units", "degrees_east"));

      Variable.Builder sstVar = NcHelper.addVariable(rootGroup, "sst", DataType.DOUBLE, dims);
      sstVar.addAttribute(new Attribute("units", "degree_C"));

      Variable.Builder commentVar =
          NcHelper.addNc3StringVariable(rootGroup, "comment", dims, strlen);

      // create the file
      ncWriter = file.build();

      // optional
      ncWriter.close();
      ncWriter = null;

      // ncdump it
      // String2.log("\nInitially:\n" + NcHelper.ncdump(testUnlimitedFileName, ""));

      // write 1 row at a time to the file
      for (row = 0; row < 5; row++) {
        Math2.sleep(20);
        if (ncWriter == null)
          ncWriter = NetcdfFormatWriter.openExisting(testUnlimitedFileName).build();
        // String2.log("writing row=" + row);

        int[] origin1 = new int[] {row};
        int[] origin2 = new int[] {row, 0};
        Array array;
        ArrayChar.D2 ac = new ArrayChar.D2(1, strlen);

        double cTime = System.currentTimeMillis() / 1000.0;
        array = NcHelper.get1DArray(new double[] {row}, false);
        ncWriter.write(ncWriter.findVariable("time"), origin1, array);
        // String2.log(">> array=" + array.toString());
        array = NcHelper.get1DArray(new double[] {33.33}, false);
        ncWriter.write(ncWriter.findVariable("lat"), origin1, array);
        array = NcHelper.get1DArray(new double[] {-123.45}, false);
        ncWriter.write(ncWriter.findVariable("lon"), origin1, array);
        array = NcHelper.get1DArray(new double[] {10 + row / 10.0}, false);
        ncWriter.write(ncWriter.findVariable("sst"), origin1, array);
        ac.setString(0, row + " comment");
        ncWriter.write(ncWriter.findVariable("comment"), origin2, ac);
        ncWriter.close();
        ncWriter = null;

        // String2.log("\nrow=" + row + ":\n" + NcHelper.ncdump(testUnlimitedFileName,
        // ""));

        /*
         * if (row == 1) {
         * results = NcHelper.ncdump(testUnlimitedFileName, "");
         * String2.log(results);
         * expected =
         * "netcdf unlimited.nc {\n" + //2013-09-03 netcdf-java 4.3 added blank lines
         * "  dimensions:\n" +
         * "    time = UNLIMITED;   // (2 currently)\n" +
         * "    comment_strlen = 6;\n" +
         * "  variables:\n" +
         * "    double time(time=2);\n" +
         * "      :units = \"seconds since 1970-01-01\";\n" +
         * "\n" +
         * "    double lat(time=2);\n" +
         * "      :units = \"degrees_north\";\n" +
         * "\n" +
         * "    double lon(time=2);\n" +
         * "      :units = \"degrees_east\";\n" +
         * "\n" +
         * "    double sst(time=2);\n" +
         * "      :units = \"degree_C\";\n" +
         * "\n" +
         * "    char comment(time=2, comment_strlen=6);\n" +
         * "\n" +
         * "\n" +
         * "  data:\n" +
         * "    time = \n" +
         * "      {0.0, 1.0}\n" +
         * "    lat = \n" +
         * "      {33.33, 33.33}\n" +
         * "    lon = \n" +
         * "      {-123.45, -123.45}\n" +
         * "    sst = \n" +
         * "      {10.0, 10.1}\n" +
         * "    comment = \"0 comm\", \"1 comm\"\n" +
         * "}\n";
         * if (!results.equals(expected)) {
         * ncWriter.close();
         * ncWriter = null;
         * Test.ensureEqual(results, expected, "trouble when i==1");
         * }
         * }
         */
      }

      // NOTE: instead of closing the ncWriter to write changes to disk, you can use
      // ncWriter.flush().
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(testUnlimitedFileName);
        ncWriter = null;
      }
    } catch (Exception e9) {
      String2.log(
          "row="
              + row
              + " "
              + NcHelper.ERROR_WHILE_CREATING_NC_FILE
              + MustBe.throwableToString(e9));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e8) {
        }
        File2.delete(testUnlimitedFileName);
        ncWriter = null;
      }
      throw e9;
    }

    results = NcHelper.ncdump(testUnlimitedFileName, "");
    // String2.log(results);
    expected =
        "netcdf unlimited.nc {\n"
            + // 2013-09-03 netcdf-java 4.3 added blank lines
            "  dimensions:\n"
            + "    time = UNLIMITED;   // (5 currently)\n"
            + "    comment_strlen = 6;\n"
            + "  variables:\n"
            + "    double time(time=5);\n"
            + "      :units = \"seconds since 1970-01-01\";\n"
            + "\n"
            + "    double lat(time=5);\n"
            + "      :units = \"degrees_north\";\n"
            + "\n"
            + "    double lon(time=5);\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    double sst(time=5);\n"
            + "      :units = \"degree_C\";\n"
            + "\n"
            + "    char comment(time=5, comment_strlen=6);\n"
            + "\n"
            + "\n"
            + "  data:\n"
            + "    time = \n"
            + "      {0.0, 1.0, 2.0, 3.0, 4.0}\n"
            + "    lat = \n"
            + "      {33.33, 33.33, 33.33, 33.33, 33.33}\n"
            + "    lon = \n"
            + "      {-123.45, -123.45, -123.45, -123.45, -123.45}\n"
            + "    sst = \n"
            + "      {10.0, 10.1, 10.2, 10.3, 10.4}\n"
            + "    comment =   \"0 comm\",   \"1 comm\",   \"2 comm\",   \"3 comm\",   \"4 comm\"\n"
            + "}\n";
    Test.ensureEqual(results, expected, "");
  }

  /** Diagnose a problem */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void testJplG1SST() throws Exception {
    String dir = "c:/data/jplG1SST/";
    String request[] = new String[] {"SST"};
    StringArray varNames = new StringArray();
    NetcdfFile fi;
    Variable var; // read start:stop:stride

    fi = NcHelper.openFile(dir + "sst_20120214.nc");
    var = fi.findVariable("SST");
    PrimitiveArray pas14 =
        NcHelper.getPrimitiveArray(
            var.read("0,0:14000:200,0:28000:200"), true, NcHelper.isUnsigned(var));
    fi.close();
    String2.log(pas14.toString());

    fi = NcHelper.openFile(dir + "sst_20120212.nc");
    var = fi.findVariable("SST");
    PrimitiveArray pas13 =
        NcHelper.getPrimitiveArray(
            var.read("0,0:14000:200,0:28000:200"), true, NcHelper.isUnsigned(var));
    fi.close();
    String2.log(pas13.toString());

    String2.log("diffString=\n" + pas14.diffString(pas13));
  }
}
