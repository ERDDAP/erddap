/*
 * NcHelper Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.write.*;

/**
 * This class has some static convenience methods related to NetCDF files and the other Data
 * classes.
 *
 * <p>!!!netcdf classes use the Apache Jakarta Commons Logging project (Apache license). The files
 * are in the netcdfAll.jar. !!!The commons logging system and your logger must be initialized
 * before using this class. It is usually set by the main program (e.g., NetCheck). For a simple
 * solution, use String2.setupCommonsLogging(-1);
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-12-07
 */
public class NcHelper {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
   * if you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean reallyVerbose = false;

  /**
   * Set this to true (by calling debugMode=true in your program, not by changing the code here) if
   * you want lots and lots of diagnostic messages sent to String2.log.
   */
  public static boolean debugMode = false;

  /**
   * varName + StringLengthSuffix is used to create the name for the char dimension of a String
   * variable. "_strlen" is what netcdf-java uses.
   */
  public static final String StringLengthSuffix =
      "_strlen"; // pre 2012-06-05 was StringLength="StringLength";

  /**
   * Since .nc files can't store 16bit char or longs, those data types are stored as shorts or
   * doubles and these messages are added to the attributes.
   */
  public static final String originally_a_CharArray = "originally a CharArray";

  public static final String originally_a_LongArray = "originally a LongArray";
  public static final String originally_a_ULongArray = "originally a ULongArray";

  public static final String ERROR_WHILE_CREATING_NC_FILE = "Error while creating .nc file:\n";

  /** If saving longs or ulongs as Strings, this is the maxStringLength. */
  public static final int LONG_MAXSTRINGLENGTH = 20;

  /**
   * For my purposes (not part of NetCDF system), this is the character used to separate a
   * structureName|memberName so that structure members can be handled like other variables.
   */
  public static final char STRUCTURE_MEMBER_SEPARATOR = '|';

  /**
   * Tell netcdf-java to object if a file is truncated (e.g., didn't get completely copied over) See
   * email from Christian Ward-Garrison Nov 2, 2016 and new location in email from
   * support-netcdf-java (Sean Arms) 2021-01-07.
   */
  static {
    // 2021-01-07 was ucar.nc2.iosp.netcdf3.N3header.disallowFileTruncation = true;
    ucar.nc2.internal.iosp.netcdf3.N3headerNew.disallowFileTruncation = true;
  }

  /**
   * This is an ncdump-like method.
   *
   * @param cmd Use just one option: "-h" (header), "-c" (coord. vars), "-vall" (default), "-v
   *     var1;var2", "-v var1(0:1,:,12)" "-k" (type), "-t" (human-readable times) -- These 2 options
   *     don't work.
   */
  public static String ncdump(String fileName, String cmd) throws Exception {
    NetcdfFile nc = null;
    try {
      nc = openFile(fileName);
      return ncdump(nc, cmd);
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      return "Unable to open file or file not .nc-compatible.";
    } finally {
      try {
        nc.close();
      } catch (Throwable t2) {
      }
    }
  }

  public static String ncdump(NetcdfFile nc, String cmd) throws Exception {
    try {
      StringWriter sw = new StringWriter();
      // -vall is Ncdump version of default for ncdump
      Ncdump.ncdump(nc, String2.isSomething(cmd) ? cmd : "-vall", sw, null);
      return decodeNcDump(sw.toString());
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      return "Error while generating ncdump string.";
    }
  }

  /**
   * This is like String2.fromJson, but does less decoding and is made specifically to fix up
   * unnecessarily encoded attributes in NcDump String starting with netcdf-java 4.0. This is like
   * replaceAll, but smarter. null is returned as null.
   *
   * @param s
   * @return the decoded string
   */
  public static String decodeNcDump(String s) {
    StringBuilder sb = new StringBuilder();
    int sLength = s.length();

    // remove dir name from first line (since this is sometimes sent to ERDDAP users)
    // "netcdf /erddapBPD/cache/_test/EDDGridFromDap_Axis.nc {\n" +
    int po = 0;
    int brPo = s.indexOf(" {");
    if (brPo >= 0) {
      int slPo = s.substring(0, brPo).lastIndexOf('/');
      if (slPo < 0) slPo = s.substring(0, brPo).lastIndexOf('\\');
      if (slPo >= 0) {
        sb.append("netcdf ");
        po = slPo + 1;
      }
    }

    // process rest of s
    while (po < sLength) {
      char ch = s.charAt(po);
      if (ch == '\\') {
        if (po == sLength - 1) po--; // so reread \ and treat as \\
        po++;
        ch = s.charAt(po);
        if (ch == 'n') sb.append('\n');
        else if (ch == 'r') {
        } else if (ch == '\'') sb.append('\'');
        else sb.append("\\" + ch); // leave as backslash encoded
      } else if (ch
          == '\r') { // 2013-09-03 netcdf-java 4.3 started using \r\n on non-data lines. Remove \r.
      } else {
        sb.append(ch);
      }
      po++;
    }
    return sb.toString();
  }

  /**
   * This converts a ucar.nc2 numeric or char ArrayXxx.Dx into a PrimitiveArray.
   *
   * <p>For StringArray, this is like fromJson, but specifically designed to decode an attribute,
   * starting with netcdf-java 4.0 (which returns them in a backslash encoded form). null is
   * returned as null
   *
   * @param nc2Array an nc2Array
   * @return Decoded StringArray (from ArrayChar.D1, split at \n), or other PrimitiveArray (from
   *     numeric ArrayXxx.D1)
   */
  private static PrimitiveArray decodeAttributeToPrimitive(Array nc2Array) {
    // String[] from ArrayChar.Dn
    if (nc2Array instanceof ArrayChar ac) {
      ArrayObject ao = ac.make1DStringArray();
      Object[] oa = (Object[]) ao.copyTo1DJavaArray();
      StringArray sa = new StringArray(oa.length, false);
      for (int i = 0; i < oa.length; i++)
        sa.add(oa[i] == null ? null : String2.fromJson(String2.trimEnd(oa[i].toString())));
      return sa;
    }

    // byte[] from ArrayBoolean.Dn
    if (nc2Array instanceof ArrayBoolean ab) {
      boolean boolAr[] = (boolean[]) nc2Array.copyTo1DJavaArray();
      int n = boolAr.length;
      byte byteAr[] = new byte[n];
      for (int i = 0; i < n; i++) byteAr[i] = boolAr[i] ? (byte) 1 : (byte) 0;
      return PrimitiveArray.factory(byteAr, nc2Array.isUnsigned());
    }

    // ArrayXxxnumeric
    return PrimitiveArray.factory(nc2Array.copyTo1DJavaArray(), nc2Array.isUnsigned());
  }

  /**
   * This mimics the old way of creating and adding a dimension. The implied isUnlimited and
   * variableLength are false.
   *
   * @param name
   * @param size
   * @return the new dimension
   */
  public static Dimension addDimension(Group.Builder group, String name, int size) {
    Dimension dim = new Dimension(name, size);
    group.addDimension(dim);
    return dim;
  }

  /**
   * This mimics the pre-2021 way of creating and adding a dimension.
   *
   * @param name
   * @param size
   * @param isShared For me, usually true.
   * @param isUnlimited
   * @param isVariableLength
   * @return the new dimension
   */
  public static Dimension addDimension(
      Group.Builder group,
      String name,
      int size,
      boolean isShared,
      boolean isUnlimited,
      boolean isVariableLength) {
    Dimension dim = new Dimension(name, size, isShared, isUnlimited, isVariableLength);
    group.addDimension(dim);
    return dim;
  }

  /**
   * This mimics the pre-2021 way of creating a variable (if just one dimension).
   *
   * @param group the Group.Builder
   * @param shortName the short name for the var
   * @param dataType the data type
   * @param dim
   * @return the Variable.Builder
   */
  public static Variable.Builder addVariable(
      Group.Builder group, String shortName, DataType dataType, Dimension dim) {
    return addVariable(group, shortName, dataType, Arrays.asList(dim));
  }

  /**
   * This mimics the pre-2021 way of creating a variable.
   *
   * @param group the Group.Builder
   * @param shortName the short name for the var
   * @param dataType the data type
   * @param dims the list of dimensions
   * @return the Variable.Builder
   */
  public static Variable.Builder addVariable(
      Group.Builder group, String shortName, DataType dataType, List<Dimension> dims) {

    Variable.Builder varBuilder =
        Variable.builder().setName(shortName).setDataType(dataType).addDimensions(dims);
    group.addVariable(varBuilder);
    return varBuilder;
  }

  /**
   * This mimics the pre-2021 way of creating a String (actually char) variable in an .nc3 file.
   *
   * @param group the Group.Builder
   * @param shortName the short name for the var
   * @param dims the list of dimensions (not including a stringLength dim)
   * @param maxStringLength
   * @return the Variable.Builder
   */
  public static Variable.Builder addNc3StringVariable(
      Group.Builder group, String shortName, List<Dimension> dims, int maxStringLength) {

    // add a dimension for stringLength
    Dimension dim = new Dimension(shortName + StringLengthSuffix, maxStringLength);
    group.addDimension(dim);
    ArrayList<Dimension> al = new ArrayList(dims);
    al.add(dim);

    Variable.Builder varBuilder =
        Variable.builder().setName(shortName).setDataType(DataType.CHAR).addDimensions(al);
    group.addVariable(varBuilder);
    return varBuilder;
  }

  /**
   * This converts a ArrayXxx.D1 (of any numeric type) into a double[].
   *
   * @param array any numeric ArrayXxx.D1
   * @return a double[]
   * @throws Exception if trouble
   */
  public static double[] toDoubleArray(Array array) {
    return (double[]) array.get1DJavaArray(DataType.DOUBLE);
  }

  /**
   * This returns the size of the array. If n > Integer.MAX_VALUE, this throws an Exception.
   *
   * @param array an ArrayChar.D2 or any ArrayXxx.D1
   * @return the length of array (the number of Strings in the ArrayChar, or the number of elements
   *     in any other array type)
   */
  public static int get1DArrayLength(Array array) {
    long n;
    if (array instanceof ArrayChar.D2 da) {
      n = da.getShape()[0];
    } else {
      n = array.getSize();
    }
    Test.ensureTrue(n < Integer.MAX_VALUE, String2.ERROR + " in NcHelper.getSize; n = " + n);
    return (int) n; // safe since checked above
  }

  /**
   * This mimics the pre-2021 way of creating an attribute from a ucar Array.
   *
   * @param attName
   * @param value WARNING: for nc3 files, Array MUSTN'T be an unsigned array.
   * @return the new attribute
   */
  public static Attribute newAttribute(String attName, Array value) {
    return Attribute.builder().setName(attName).setValues(value).build();
  }

  /**
   * This creates an netcdf Attribute from a PrimitiveArray.
   *
   * @param name
   * @param pa
   * @return an Attribute
   */
  public static Attribute newAttribute(boolean nc3Mode, String name, PrimitiveArray pa) {
    if (pa.elementType() == PAType.STRING) {
      if (nc3Mode) {
        // String2.log("***getAttribute nStrings=" + pa.size());
        String ts = Attributes.valueToNcString(pa);
        // int maxLength = 32000; //pre 2010-10-13 8000 ok; 9000 not; now >32K ok; unclear what new
        // limit is
        // if (ts.length() > maxLength)
        //    ts = ts.substring(0, maxLength - 3) + "...";
        // String2.log("***getAttribute string=\"" + ts + "\"");
        return new Attribute(name, ts);
      } else {
        String s = ((StringArray) pa).toNewlineString();
        return new Attribute(name, s.length() == 0 ? "" : s.substring(0, s.length() - 1));
      }
    }
    Array ar = null;
    if (pa
        instanceof
        CharArray) // pass all (Unicode) chars through unchanged (not limited to ISO_8859_1)
    ar = Array.factory(DataType.CHAR, new int[] {pa.size()}, pa.toObjectArray());
    else if (nc3Mode && (pa.elementType() == PAType.LONG || pa.elementType() == PAType.ULONG))
      ar = get1DArray(PrimitiveArray.rawFactory(PAType.DOUBLE, pa));
    else if (nc3Mode && pa.isUnsigned())
      ar = get1DArray(pa.toObjectArray(), false); // 2020-04-10 attributes in nc3 can't be unsigned
    else ar = get1DArray(pa);

    Attribute att = Attribute.builder().setName(name).setValues(ar).build();
    // String2.log(">> NcHelper.newAttribute(" + name + ", " + pa.elementType() + ", " + pa + ") ->
    // " + att.toString());
    return att;
  }

  /** This makes an ArrayString.D1 for use with netcdf-4. */
  public static ArrayString.D1 getStringArrayD1(StringArray sa) {
    int n = sa.size();
    ArrayString.D1 asd1 = new ArrayString.D1(n);
    for (int i = 0; i < n; i++) asd1.set(i, sa.get(i));
    return asd1;
  }

  /**
   * This converts a String or array of primitives into a ucar.nc2.ArrayXxx.D1. The o array is used
   * as the storage for the Array.
   *
   * @param pa a PrimitiveArray
   */
  public static Array get1DArray(PrimitiveArray pa) {
    return get1DArray(pa.toObjectArray(), pa.isUnsigned());
  }

  /**
   * This converts a String or array of primitives into a ucar.nc2.ArrayXxx.D1. The o array is used
   * as the storage for the Array.
   *
   * @param o the String or array of primitives (e.g., int[])
   * @param isUnsigned Only used for byte, short, int, long; ignored for other data types.
   * @return an ArrayXxx.D1. A String is converted to a ArrayChar.D2. A long[] is converted to a
   *     String[] and then to ArrayChar.D2 (nc3 files don't support longs).
   */
  public static Array get1DArray(Object o, boolean isUnsigned) {
    if (o instanceof String) {
      o = ((String) o).toCharArray();
      // will be handled below
    }

    if (o instanceof char[] car1) {
      // netcdf-java just writes low byte, so use String2.toIso88591Chars()
      int n = car1.length;
      char[] car2 = new char[n];
      for (int i = 0; i < n; i++) car2[i] = String2.toIso88591Char(car1[i]);
      return Array.factory(DataType.CHAR, new int[] {n}, car2);
    }

    if (o instanceof byte[] ba)
      return Array.factory(isUnsigned ? DataType.UBYTE : DataType.BYTE, new int[] {ba.length}, ba);
    if (o instanceof short[] sa)
      return Array.factory(
          isUnsigned ? DataType.USHORT : DataType.SHORT, new int[] {sa.length}, sa);
    if (o instanceof int[] ia)
      return Array.factory(isUnsigned ? DataType.UINT : DataType.INT, new int[] {ia.length}, ia);
    if (o instanceof long[] lar) {
      // String2.log("\n>> long values=" + String2.toCSSVString((long[])o));
      o = null;
      int tSize = lar.length;
      double dar[] = new double[tSize];
      if (isUnsigned) { // ulong
        for (int i = 0; i < tSize; i++)
          dar[i] = Math2.ulongToDouble(lar[i]); // !!! possible loss of precision
      } else { // long
        for (int i = 0; i < tSize; i++) dar[i] = lar[i]; // !!! possible loss of precision
      }
      o = dar;
      // then falls through to Double handling
      // String2.log(">> as doubles=" + String2.toCSSVString((double[])o));
    }
    if (o instanceof float[] fa) return Array.factory(DataType.FLOAT, new int[] {fa.length}, fa);
    if (o instanceof double[] da) {
      // String2.log(">> double o = " + String2.toCSSVString((double[])o));
      return Array.factory(DataType.DOUBLE, new int[] {da.length}, da);
    }
    if (o instanceof String[] sar) {
      // make ArrayChar.D2
      // String2.log("NcHelper.get1DArray sar=" + String2.toCSSVString(sar));
      int max = 1; // nc wants at least 1
      for (int i = 0; i < sar.length; i++) {
        // if (sar[i].length() > max) String2.log("new max=" + sar[i].length() + " s=\"" + sar[i] +
        // "\"");
        max = Math.max(max, sar[i].length());
      }
      // String2.log("NcHelper.get1DArray String[] max=" + max);
      ArrayChar.D2 ac = new ArrayChar.D2(sar.length, max);
      for (int i = 0; i < sar.length; i++) {
        // setString just does low byte, so use String2.toIso88591String()
        ac.setString(i, String2.toIso88591String(sar[i]));
        // String s = sar[i];
        // int sLength = s.length();
        // for (int po = 0; po < sLength; po++)
        //    ac.set(i, po, s.charAt(po));
        // for (int po = sLength; po < max; po++)  //not necessary because filled with 0's?
        //    ac.set(i, po, '\u0000');
      }
      return ac;
    }

    Test.error(
        String2.ERROR
            + " in NcHelper.get1DArray: unexpected object type: "
            + o.getClass().getCanonicalName()
            + ": "
            + o);
    return null;
  }

  /**
   * This returns true if the variable's DataType isIntegral and isUnsigned. This works with nc3 and
   * nc4 files. With nc3 files, this looks for the _Unsigned=true attribute
   *
   * @return true if the variable's DataType isIntegral and isUnsigned.
   */
  public static boolean isUnsigned(Variable variable) {
    DataType dt = variable.getDataType();
    if (!dt.isIntegral()) return false;
    if (dt.isUnsigned()) return true; // vars in nc4 files return correct isUnsigned status
    PrimitiveArray pa = getVariableAttribute(variable, "_Unsigned");
    if (pa != null && "true".equals(pa.toString())) return true;
    return false;
  }

  /**
   * This reads all of the values from an nDimensional variable. The variable can be any shape. This
   * version uses buildStringsFromChars=true.
   *
   * @param variable
   * @return a suitable primitiveArray
   */
  public static PrimitiveArray getPrimitiveArray(Variable variable) throws Exception {
    return getPrimitiveArray(variable.read(), true, isUnsigned(variable));
  }

  /**
   * This reads all of the values from an nDimensional variable. The variable can be any shape.
   *
   * @param variable
   * @param buildStringsFromChars only applies to source DataType=char variables.
   * @return a suitable primitiveArray
   */
  public static PrimitiveArray getPrimitiveArray(Variable variable, boolean buildStringsFromChars)
      throws Exception {

    return getPrimitiveArray(variable.read(), buildStringsFromChars, isUnsigned(variable));
  }

  /**
   * This converts a ucar.nc2 numeric or char ArrayXxx.Dx into a PrimitiveArray.
   *
   * @param nc2Array an nc2Array
   * @param buildStringsFromChars only applies to source DataType=char variables.
   * @param isUnsigned if true and if the object type isIntegerType, the resulting PrimitiveArray
   *     will be an unsigned PAType. If false and object type isIntegerType, the resulting
   *     PrimitiveArrray will match the signedness(?) of nc2Array.
   * @return a PrimitiveArray
   */
  public static PrimitiveArray getPrimitiveArray(
      Array nc2Array, boolean buildStringsFromChars, boolean isUnsigned) {
    // String2.log(">> NcHelper.getPrimitiveArray nc2Array.isUnsigned=" + nc2Array.isUnsigned());
    // String[] from ArrayChar.Dn
    if (buildStringsFromChars && nc2Array instanceof ArrayChar na) {
      ArrayObject ao = na.make1DStringArray();
      Object[] oa = (Object[]) ao.copyTo1DJavaArray();
      StringArray sa = new StringArray(oa.length, false);
      for (int i = 0; i < oa.length; i++)
        sa.add(oa[i] == null ? null : String2.trimEnd(oa[i].toString()));
      return sa;
    }

    // byte[] from ArrayBoolean.Dn
    if (nc2Array instanceof ArrayBoolean ab) {
      boolean boolAr[] = (boolean[]) ab.copyTo1DJavaArray();
      int n = boolAr.length;
      byte byteAr[] = new byte[n];
      for (int i = 0; i < n; i++) byteAr[i] = boolAr[i] ? (byte) 1 : (byte) 0;
      return PrimitiveArray.factory(
          byteAr, false); // never unsigned (not needed, and unsigned is often trouble)
    }

    // ArrayXxxnumeric
    return PrimitiveArray.factory(
        nc2Array.copyTo1DJavaArray(), isUnsigned || nc2Array.isUnsigned());
  }

  // was
  //   * This converts a ucar.nc2 numeric ArrayXxx.D1, numeric ArrayXxx.D4,
  //   *   ArrayChar.D2, or ArrayChar.D5 into an array of primitives.

  /**
   * This converts a ucar.nc2 numeric or char ArrayXxx.Dx into a PrimitiveArray.
   *
   * @param nc2Array an nc2Array
   * @param buildStringsFromChars only applies to source DataType=char variables.
   * @param isUnsigned if true and if the object type isIntegerType, the resulting PrimitiveArray
   *     will be an unsigned PAType. If false and object type isIntegerType, the resulting
   *     PrimitiveArrray will match the signedness(?) of nc2Array.
   * @return StringArray (from ArrayChar.D1, split at \n), or ByteArray (from numeric ArrayXxx.D1)
   */
  public static PrimitiveArray getPrimitiveArray(Array nc2Array) {
    return getPrimitiveArray(nc2Array, true, false);
  }

  /**
   * This converts an netcdf DataType into a PrimitiveArray elementType (e.g., Datatype.UINT becomes
   * PAType.UINT). BEWARE: This returns the stated dataType (not overthinking it). .nc3 files store
   * strings as char arrays, so if variable.getRank()==1 it is a char variable, but if
   * variable.getRank()==2 it should later be converted to to a String variable. But .nc4 files have
   * true String dataType.
   *
   * @param dataType the Netcdf dataType
   * @return the corresponding PrimitiveArray elementPAType (e.g., PAType.INT for integer
   *     primitives)
   * @throws RuntimeException if dataType is null or unexpected.
   */
  public static PAType getElementPAType(Variable var) {
    DataType dataType = var.getDataType(); // nc3 doesn't return signed types
    boolean unsigned = isUnsigned(var); // so ask separately
    if (unsigned) { // and deal with
      if (dataType == DataType.BYTE) return PAType.UBYTE;
      if (dataType == DataType.SHORT) return PAType.USHORT;
      if (dataType == DataType.INT) return PAType.UINT;
      if (dataType == DataType.LONG) return PAType.LONG;
    }
    return getElementPAType(dataType);
  }

  /**
   * This SIMPLISTICALLY converts an netcdf DataType into a PAType (e.g., Datatype.UINT becomes
   * PAType.UINT). See the variant above to deal with unsigned variables in .nc3 files.
   *
   * @param dataType the Netcdf dataType
   * @return the corresponding PrimitiveArray elementPAType (e.g., PAType.INT for integer
   *     primitives)
   * @throws RuntimeException if dataType is null or unexpected.
   */
  public static PAType getElementPAType(DataType dataType) {
    if (dataType == DataType.BOOLEAN) return PAType.BOOLEAN;
    if (dataType == DataType.BYTE) return PAType.BYTE;
    if (dataType == DataType.UBYTE) return PAType.UBYTE;
    if (dataType == DataType.CHAR) return PAType.CHAR;
    if (dataType == DataType.DOUBLE) return PAType.DOUBLE;
    if (dataType == DataType.FLOAT) return PAType.FLOAT;
    if (dataType == DataType.INT) return PAType.INT;
    if (dataType == DataType.UINT) return PAType.UINT;
    if (dataType == DataType.LONG) return PAType.LONG;
    if (dataType == DataType.ULONG) return PAType.ULONG;
    if (dataType == DataType.SHORT) return PAType.SHORT;
    if (dataType == DataType.USHORT) return PAType.USHORT;
    if (dataType == DataType.STRING) return PAType.STRING;
    // STRUCTURE not converted
    Test.error(
        String2.ERROR
            + " in NcHelper.getElementType:\n"
            + " unexpected DataType: "
            + dataType.toString());
    return null;
  }

  /** This returns true if the dataType is one of the unsigned integer types. */
  public static boolean isUnsigned(DataType dataType) {
    return dataType == DataType.UINT
        || dataType == DataType.USHORT
        || dataType == DataType.UBYTE
        || dataType == DataType.ULONG;
  }

  /**
   * This converts an ElementType (e.g., PAType.INT for integer primitives) into an netcdf-3
   * DataType (so PAType.LONG returns DataType.DOUBLE). BEWARE: .nc files store strings as char
   * arrays, so if variable.getRank()==1 it is a char variable, but if variable.getRang()==2 it is a
   * String variable. [It isn't that simple!] This throws Exception if elementPAType not found.
   *
   * @param elementPAType the PrimitiveArray elementPAType (e.g., PAType.INT for integer
   *     primitives). longs are converted to doubles.
   * @return the corresponding netcdf dataType
   */
  public static DataType getNc3DataType(PAType elementPAType) {
    if (elementPAType == PAType.BOOLEAN) return DataType.BOOLEAN; // ?
    if (elementPAType == PAType.BYTE) return DataType.BYTE;
    if (elementPAType == PAType.CHAR) return DataType.CHAR;
    if (elementPAType == PAType.DOUBLE) return DataType.DOUBLE;
    if (elementPAType == PAType.FLOAT) return DataType.FLOAT;
    if (elementPAType == PAType.INT) return DataType.INT;
    if (elementPAType == PAType.LONG) return DataType.DOUBLE; // long -> double in .nc3
    if (elementPAType == PAType.SHORT) return DataType.SHORT;
    if (elementPAType == PAType.STRING) return DataType.STRING;

    // 2021-01-07 changed to signed for netcdf-java-5.4.1  (utypes worked in 5.3.3)
    //         if (elementPAType == PAType.UBYTE)   return DataType.UBYTE;
    //         if (elementPAType == PAType.USHORT)  return DataType.USHORT;
    //         if (elementPAType == PAType.UINT)    return DataType.UINT;
    if (elementPAType == PAType.UBYTE) return DataType.BYTE;
    if (elementPAType == PAType.USHORT) return DataType.SHORT;
    if (elementPAType == PAType.UINT) return DataType.INT;

    if (elementPAType == PAType.ULONG) return DataType.DOUBLE; // ulong -> double in .nc3
    // STRUCTURE not converted
    Test.error(
        String2.ERROR
            + " in NcHelper.getNc3DataType:\n"
            + " unrecognized ElementType: "
            + elementPAType);
    return null;
  }

  /**
   * This converts an ElementType (e.g., PAType.INT for integer primitives) into an netcdf-4
   * DataType. This throws Exception if elementPAType not found.
   *
   * @param elementPAType the PrimitiveArray elementPAType (e.g., PAType.INT for integer
   *     primitives). longs and ulongs are converted to doubles.
   * @return the corresponding netcdf dataType
   */
  public static DataType getNc4DataType(PAType elementPAType) {
    // just deal with things that are different than nc3
    if (elementPAType == PAType.LONG) return DataType.LONG;
    if (elementPAType == PAType.ULONG) return DataType.ULONG;
    return getNc3DataType(elementPAType);
  }

  /** This returns the appropriate DataType for nc3 or nc4 files. */
  public static DataType getDataType(boolean nc3Mode, PAType elementPAType) {
    return nc3Mode ? getNc3DataType(elementPAType) : getNc4DataType(elementPAType);
  }

  /**
   * From an arrayList which alternates attributeName (a String) and attributeValue (an object),
   * this generates a String with startLine+"<name>=<value>" on each line. If arrayList == null,
   * this returns startLine+"[null]\n".
   *
   * <p>This nc version is used to print netcdf header attributes. It puts quotes around String
   * attributeValues, converts \ to \\, and " to \", and appends 'f' to float values/
   *
   * @param arrayList
   * @return the desired string representation
   */
  public static String alternateToString(String prefix, ArrayList arrayList, String suffix) {
    if (arrayList == null) return prefix + "[null]\n";
    StringBuilder sb = new StringBuilder();
    for (int index = 0; index < arrayList.size(); index += 2) {
      sb.append(prefix);
      sb.append(arrayList.get(index).toString());
      sb.append(" = ");
      Object o = arrayList.get(index + 1);
      if (o instanceof StringArray sa) {
        int n = sa.size();
        String connect = "";
        for (int i = 0; i < n; i++) {
          sb.append(connect);
          connect = ", ";
          String s = String2.replaceAll(sa.get(i), "\\", "\\\\"); //  \ becomes \\
          s = String2.replaceAll(s, "\"", "\\\""); //  " becomes \"
          sb.append("\"" + s + "\"");
        }
      } else if (o instanceof FloatArray fa) {
        int n = fa.size();
        String connect = "";
        for (int i = 0; i < n; i++) {
          sb.append(connect);
          connect = ", ";
          sb.append(fa.get(i));
          sb.append("f");
        }
      } else {
        sb.append(o.toString());
      }
      sb.append(suffix);
      sb.append('\n');
    }
    return sb.toString();
  }

  /**
   * This gets a string representation of the header of a .nc file.
   *
   * <p>If the fullName is an http address, the name needs to start with "http:\\" (upper or lower
   * case) and the server needs to support "byte ranges" (see ucar.nc2.NetcdfFile documentation).
   *
   * @param fullName This may be a local file name, an "http:" address of a .nc file, or an opendap
   *     url.
   * @return the string representation of a .nc file header
   * @throws Exception
   */
  public static String readCDL(String fullName) throws Exception {

    // get information
    NetcdfFile netcdfFile = openFile(fullName);
    try {
      String results = netcdfFile.toString();
      return String2.replaceAll(results, "\r", ""); // 2013-09-03 netcdf-java 4.3 started using \r\n

    } finally {
      try {
        netcdfFile.close(); // make sure it is explicitly closed
      } catch (Exception e2) {
      }
    }
  }

  /** THIS IS IMPERFECT/UNFINISHED. This generates a .dds-like list of variables in a .nc file. */
  public static String dds(String fileName) throws Exception {
    String sar[] = String2.splitNoTrim(readCDL(fileName), '\n');
    int n = sar.length;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      String trimS = sar[i].trim();
      if (trimS.length() > 0 && !trimS.startsWith(":")) sb.append(sar[i] + "\n");
      sar[i] = null;
    }
    return sb.toString();
  }

  /**
   * This opens a local file name (or an "http:" address, but that is discouraged) of a .nc file.
   * ALWAYS explicitly call netcdfFile.close() when you are finished with it, preferably in a
   * "finally" clause.
   *
   * <p>This does not work for opendap urls. Use the opendap methods instead.
   *
   * <p>If the fullName is an http address, the name needs to start with "http://" (upper or lower
   * case) and the server needs to support "byte ranges" (see ucar.nc2.NetcdfFile documentation).
   *
   * @param fullName This may be a local file name, an "http:" address of a .nc file (discouraged),
   *     or an opendap url. If this is an .ncml file, the name must end in .ncml.
   * @return a NetcdfFile
   * @throws Exception if trouble
   */
  public static NetcdfFile openFile(String fullName) throws Exception {
    return fullName.endsWith(".ncml")
        ? NetcdfDatasets.openDataset(fullName)
        : // 's' is the new API
        NetcdfFiles.open(fullName); // 's' is the new API
  }

  /**
   * This converts a List&lt;variable&gt; to a Variable[].
   *
   * @param list
   * @return a Variable[] (or null if list is null)
   */
  public static Variable[] variableListToArray(List<Variable> list) {
    return (Variable[]) (list == null ? null : list.toArray(new Variable[0]));
  }

  /**
   * This converts a List of dimensions to a Dimension[].
   *
   * @param list
   * @return a Dimensione[] (or null if list is null)
   */
  public static Dimension[] dimensionListToArray(List<Dimension> list) {
    return (Dimension[]) (list == null ? null : list.toArray(new Dimension[0]));
  }

  /**
   * Get the list of variables from a netcdf file's Structure or, if there is no structure, a list
   * of variables using the largest dimension (a pseudo structure).
   *
   * @param netcdfFile
   * @param variableNames if null, this will search for the variables in a (pseudo)structure.
   * @return structure's variables
   */
  public static Variable[] findVariables(NetcdfFile netcdfFile, String variableNames[]) {
    // just use the variable names
    if (variableNames != null) {
      ArrayList<Variable> list = new ArrayList();
      for (int i = 0; i < variableNames.length; i++) {
        Variable variable = netcdfFile.findVariable(variableNames[i]);
        Test.ensureNotNull(
            variable,
            String2.ERROR + " in NcHelper.findVariables: '" + variableNames[i] + "' not found.");
        list.add(variable);
      }
      return variableListToArray(list);
    }

    // find a structure among the variables in the rootGroup
    // if no structure found, it falls through the loop
    Group rootGroup = netcdfFile.getRootGroup();
    List<Variable> rootGroupVariables = rootGroup.getVariables();
    // String2.log("rootGroup variables=" + String2.toNewlineString(rootGroupVariables.toArray()));
    for (int v = 0; v < rootGroupVariables.size(); v++) {
      if (rootGroupVariables.get(v) instanceof Structure structure) {
        if (reallyVerbose) String2.log("    NcHelper.findVariables found a Structure.");
        return variableListToArray(structure.getVariables());
      }
    }

    // is there an "observationDimension" global attribute?
    // observationDimension is from deprecated "Unidata Observation Dataset v1.0" conventions,
    // but if it exists, use it.
    Dimension mainDimension = null;
    ucar.nc2.Attribute gAtt =
        netcdfFile.findGlobalAttribute(
            "observationDimension"); // there is also a dods.dap.Attribute
    if (gAtt != null) {
      PrimitiveArray pa = getPrimitiveArray(gAtt.getValues());
      if (pa.size() > 0) {
        String dimName = pa.getString(0);
        if (reallyVerbose)
          String2.log("    NcHelper.findVariables observationDimension: " + dimName);
        mainDimension = netcdfFile.getRootGroup().findDimension(dimName);
      }
    }

    // look for unlimited dimension
    if (mainDimension == null) {
      List dimensions =
          rootGroup.getDimensions(); // next nc version: getRootGroup().getDimensions();  //was
      // netcdfFile.getDimensions()
      if (dimensions.size() == 0)
        Test.error(String2.ERROR + " in NcHelper.findVariables: the file has no dimensions.");
      for (int i = 0; i < dimensions.size(); i++) {
        Dimension tDimension = (Dimension) dimensions.get(i);
        if (tDimension.isUnlimited()) {
          mainDimension = tDimension;
          if (reallyVerbose)
            String2.log(
                "    NcHelper.findVariables found an unlimited dimension: "
                    + mainDimension.getName()); // the full name
          break;
        }
      }
    }

    // look for a time variable (units contain " since ")
    if (mainDimension == null) {
      for (int v = 0; v < rootGroupVariables.size(); v++) {
        Variable variable = (Variable) rootGroupVariables.get(v);
        List dimensions = variable.getDimensions();
        PrimitiveArray units = getVariableAttribute(variable, "units");
        if (units != null
            && units.size() > 0
            && Calendar2.isNumericTimeUnits(units.getString(0))
            && dimensions.size() > 0) {
          mainDimension = (Dimension) dimensions.get(0);
          if (reallyVerbose)
            String2.log(
                "    NcHelper.findVariables found a time variable with dimension: "
                    + mainDimension.getName()); // the full name
          break;
        }
      }
    }

    // use the first outer dimension  (look at last veriables first -- more likely to be the main
    // data variables)
    if (mainDimension == null) {
      for (int v = rootGroupVariables.size() - 1; v >= 0; v--) {
        Variable variable = (Variable) rootGroupVariables.get(v);
        List dimensions = variable.getDimensions();
        if (dimensions.size() > 0) {
          mainDimension = (Dimension) dimensions.get(0);
          if (reallyVerbose) {
            String fName = mainDimension.getName(); // the full name
            if (!"row".equals(fName)) // may be null
            String2.log("    NcHelper.findVariables found an outer dimension: " + fName);
          }
          break;
        }
      }
      // check for no dimensions
      if (mainDimension == null)
        Test.error(String2.ERROR + " in NcHelper.findVariables: the file doesn't use dimensions.");
    }

    // get a list of all variables which use just mainDimension
    List structureVariables = new ArrayList();
    for (int i = 0; i < rootGroupVariables.size(); i++) {
      // if (reallyVerbose) String2.log("  get all variables which use mainDimension, check " + i);
      Variable tVariable = (Variable) rootGroupVariables.get(i);
      List tDimensions = tVariable.getDimensions();
      int nDimensions = tDimensions.size();
      // if (reallyVerbose) String2.log("i=" + i + " name=" + tVariable.getFullName() +
      //    " type=" + tVariable.getNc3DataType());
      if ((nDimensions == 1 && tDimensions.get(0).equals(mainDimension))
          || (nDimensions == 2
              && tDimensions.get(0).equals(mainDimension)
              && tVariable.getDataType() == DataType.CHAR)) {
        structureVariables.add(tVariable);
      }
    }
    return variableListToArray(structureVariables);
  }

  /**
   * This finds the highest dimension variable in the rootGroup, and then finds all similar
   * variables.
   *
   * @param groupName the name of the group (e.g., "" for any group, "somegroup/someSubGroup", or
   *     "[root]" if you want to limit the search to the root group).
   * @return the similar variables
   * @throws RuntimeException if trouble (e.g., group doesn't exist or no vars with dimensions)
   */
  public static Variable[] findMaxDVariables(NetcdfFile netcdfFile, String groupName) {

    // find dimNames of highest dimension variable
    String dimNames[] = new String[0];
    List<Variable> allVariables;
    if (String2.isSomething(groupName)) {
      Group group =
          groupName.equals("[root]") ? netcdfFile.getRootGroup() : netcdfFile.findGroup(groupName);
      if (group == null)
        throw new RuntimeException(
            String2.ERROR
                + " in NcHelper.findMaxDVariables: group=\""
                + groupName
                + "\" isn't in the NetCDF file.");
      allVariables = group.getVariables();
    } else {
      allVariables = netcdfFile.getVariables();
    }
    List<Variable> loadVariables = null;
    for (int v = 0; v < allVariables.size(); v++) {
      Variable variable = allVariables.get(v);
      boolean isChar = variable.getDataType() == DataType.CHAR;
      int tnDim = variable.getRank() - (isChar ? 1 : 0);
      if (tnDim > dimNames.length) {
        // a new winner
        loadVariables = new ArrayList();
        loadVariables.add(variable);
        dimNames = new String[tnDim];
        for (int d = 0; d < tnDim; d++) {
          Dimension tDim = variable.getDimension(d);
          dimNames[d] =
              tDim == null || tDim.getName() == null
                  ? // the full name
                  "size=" + variable.getShape(d)
                  : tDim.getName(); // the full name
        }
      } else if (tnDim > 0 && tnDim == dimNames.length) {
        // a similar variable?
        boolean ok = true;
        for (int d = 0; d < tnDim; d++) {
          Dimension tDim = variable.getDimension(d);
          String tName =
              tDim == null || tDim.getName() == null
                  ? // the full name
                  "size=" + variable.getShape(d)
                  : tDim.getName(); // the full name
          if (!dimNames[d].equals(tName)) {
            ok = false;
            break;
          }
        }
        if (ok) loadVariables.add(variable);
      }
    }
    if (loadVariables == null)
      throw new RuntimeException(
          String2.ERROR
              + " in NcHelper.findMaxDVariables: None of the variables in group=\""
              + groupName
              + "\" have dimensions.");
    return variableListToArray(loadVariables);
  }

  /**
   * This finds all variables with dimensions in the rootGroup.
   *
   * @return all variables with dimensions in the rootGroup.
   */
  public static Variable[] findAllVariablesWithDims(NetcdfFile netcdfFile) {

    Group rootGroup = netcdfFile.getRootGroup();
    List rootGroupVariables = rootGroup.getVariables();
    List loadVariables = new ArrayList();
    for (int v = 0; v < rootGroupVariables.size(); v++) {
      Variable variable = (Variable) rootGroupVariables.get(v);
      boolean isChar = variable.getDataType() == DataType.CHAR;
      int tnDim = variable.getRank() - (isChar ? 1 : 0);
      if (tnDim > 0) loadVariables.add(variable);
    }
    return variableListToArray(loadVariables);
  }

  /**
   * Get the list of 4D numeric (or 5D char) variables from a netcdf file. Currently this does no
   * verification that the dimensions are t,z,y,x, but it could in the future.
   *
   * @param netcdfFile
   * @param variableNames if null, this will search for a 4D variable, then also include all other
   *     variables using the same axes.
   * @return the variables (list length 0 if none found)
   */
  public static Variable[] find4DVariables(NetcdfFile netcdfFile, String variableNames[]) {
    // just use the variable names
    if (variableNames != null) {
      ArrayList<Variable> list = new ArrayList();
      for (int i = 0; i < variableNames.length; i++) {
        Variable variable = netcdfFile.findVariable(variableNames[i]);
        Test.ensureNotNull(
            variable,
            String2.ERROR + " in NcHelper.find4DVariables: '" + variableNames[i] + "' not found.");
        list.add(variable);
      }
      return variableListToArray(list);
    }

    // find a 4D variable among the variables in the rootGroup
    List<Variable> allVariables = netcdfFile.getVariables();
    String foundDimensionNames[] = null;
    ArrayList<Variable> foundVariables = new ArrayList();
    for (int v = 0; v < allVariables.size(); v++) {
      Variable variable = allVariables.get(v);
      List<Dimension> dimensions = variable.getDimensions();

      if ((dimensions.size() == 4 && variable.getDataType() != DataType.CHAR)
          || (dimensions.size() == 5 && variable.getDataType() == DataType.CHAR)) {

        // first found?    (future: check that axes are t,z,y,x?)
        if (foundDimensionNames == null) {
          foundDimensionNames = new String[4];
          for (int i = 0; i < 4; i++)
            foundDimensionNames[i] = dimensions.get(i).getName(); // the full name
        }

        // does it match foundDimensions
        boolean matches = true;
        for (int i = 0; i < 4; i++) {
          if (!foundDimensionNames[i].equals(dimensions.get(i).getName())) { // the full name
            matches = false;
            break;
          }
        }
        if (matches) foundVariables.add(variable);
      }
    }

    return variableListToArray(foundVariables);
  }

  /**
   * Get the desired variable.
   *
   * @param netcdfFile
   * @param variableName (not null or "")
   * @return a Variable
   * @throws Exception if trouble, e.g., not found
   */
  public static Variable findVariable(NetcdfFile netcdfFile, String variableName) {
    Variable variable = netcdfFile.findVariable(variableName);
    Test.ensureNotNull(
        variable, String2.ERROR + " in NcHelper.findVariable: '" + variableName + "' not found.");
    return variable;
  }

  /**
   * This adds global (group) attributes to a netcdf file's group.
   *
   * @param groupBuilder usually the rootGroup Builder
   * @param attributes the Attributes that will be set
   */
  public static void setAttributes(
      boolean nc3Mode, Group.Builder groupBuilder, Attributes attributes) {
    String names[] = attributes.getNames();
    for (int ni = 0; ni < names.length; ni++) {
      String tName = names[ni];
      if (!String2.isSomething(tName)
          || tName.equals(
              "_NCProperties")) // If I write this, netcdf nc4 code later throws Exception when it
        // writes its own version
        continue;
      PrimitiveArray tValue = attributes.get(tName);
      if (tValue == null || tValue.size() == 0 || tValue.toString().length() == 0)
        continue; // do nothing
      groupBuilder.addAttribute(newAttribute(nc3Mode, tName, tValue));
    }
  }

  /**
   * This adds attributes to a variable in preparation for writing a .nc file.
   *
   * @param nc3Mode
   * @param var e.g., from addVariable()
   * @param attributes the Attributes that will be set
   * @param unsigned If nc3Mode and unsigned, this method adds an _Unsigned=true attribute
   */
  public static void setAttributes(
      boolean nc3Mode, Variable.Builder var, Attributes attributes, boolean unsigned) {
    String names[] = attributes.getNames();
    if (nc3Mode && unsigned)
      var.addAttribute(newAttribute(nc3Mode, "_Unsigned", new StringArray(new String[] {"true"})));
    for (int ni = 0; ni < names.length; ni++) {
      String tName = names[ni];
      if (!String2.isSomething(tName)) continue;
      PrimitiveArray tValue = attributes.get(tName);
      if (tValue == null
          || tValue.size() == 0
          || (tValue.elementType() == PAType.STRING && tValue.toString().length() == 0))
        continue; // do nothing
      var.addAttribute(newAttribute(nc3Mode, tName, tValue));
    }
  }

  /**
   * This gets the PA value from the attribute. If there is trouble, this lots a warning message and
   * returns null. This is low level and isn't usually called directly.
   *
   * @param varName the variable's name (or "global"), used for diagnostic messages only
   * @param att
   * @return a PrimitiveArray or null if trouble
   */
  public static PrimitiveArray getAttributePA(String varName, ucar.nc2.Attribute att) {
    if (att == null) {
      // if (debugMode)
      //    String2.log("Warning: NcHelper.getAttributePA varName=" + varName + " att=null"); // +
      // MustBe.stackTrace());
      return null;
    } else if (String2.isSomething(att.getName())) { // the full name
      try {
        // decodeAttribute added with switch to netcdf-java 4.0
        Array values = att.getValues();
        if (values == null) {
          String2.log(
              "Warning: varName="
                  + varName
                  + " attribute="
                  + att.getName()
                  + " has values=null"); // the full name
          return null;
        }
        return decodeAttributeToPrimitive(values);
      } catch (Throwable t) {
        String2.log(
            "Warning: NcHelper caught an exception while reading varName='"
                + varName
                + "' attribute="
                + att.getName()
                + "\n"
                + // the full name
                MustBe.throwableToString(t));
        return null;
      }
    } else {
      if (reallyVerbose)
        String2.log(
            "Warning: NcHelper.getAttributePA varName="
                + varName
                + " att.getName()="
                + String2.annotatedString(att.getName())); // the full name
      return null;
    }
  }

  /**
   * This gets one attribute from a netcdf variable.
   *
   * @param variable
   * @param attributeName
   * @return the attribute (or null if none)
   */
  public static PrimitiveArray getVariableAttribute(Variable variable, String attributeName) {
    return getAttributePA(variable.getFullName(), variable.findAttribute(attributeName));
  }

  /**
   * This gets one attribute from a netcdf group.
   *
   * @param group
   * @param attributeName
   * @return the attribute (or null if none)
   */
  public static PrimitiveArray getGroupAttribute(Group group, String attributeName) {
    return getAttributePA(group.getFullName(), group.findAttribute(attributeName));
  }

  /**
   * This gets the first element of one group attribute as a String.
   *
   * @param group
   * @param attributeName
   * @return the attribute (or null if none)
   */
  public static String getStringGroupAttribute(Group group, String attributeName) {
    PrimitiveArray pa = getGroupAttribute(group, attributeName);
    if (pa == null || pa.size() == 0) return null;
    return pa.getString(0);
  }

  /**
   * Given an attribute, this adds the value to the attributes. If there is trouble, this logs a
   * warning message and returns. This is low level and isn't usually called directly.
   *
   * @param varName use "Global" for global attributes. This is just used for diagnostic messages.
   * @param att an nc attribute. If the name is already present, this new att takes precedence.
   * @param attributes the Attributes that will be added to.
   */
  public static void addAttribute(String varName, ucar.nc2.Attribute att, Attributes attributes) {
    addAttribute(varName, att, attributes, true);
  }

  /**
   * Given an attribute, this adds the value to the attributes. If there is trouble, this logs a
   * warning message and returns. This is low level and isn't usually called directly.
   *
   * @param varName use "Global" for global attributes. This is just used for diagnostic messages.
   * @param att an nc attribute
   * @param attributes the Attributes that will be added to.
   * @param addIfAlreadyPresent
   */
  public static void addAttribute(
      String varName, ucar.nc2.Attribute att, Attributes attributes, boolean addIfAlreadyPresent) {
    if (att == null) {
      if (reallyVerbose) String2.log("Warning: NcHelper.addAttribute " + varName + " att=null");
      return;
    }
    String tName = att.getName(); // It is the short name! It doesn't include the groupName.
    if (!String2.isSomething(tName)) {
      if (reallyVerbose)
        String2.log(
            "Warning: NcHelper.addAttribute "
                + varName
                + " att.getFullName()="
                + String2.annotatedString(tName));
    } else if (!addIfAlreadyPresent && attributes.get(tName) != null) {
      // do nothing
    } else {
      // attributes.set calls String2.canonical (useful since many names are in many datasets)
      attributes.set(tName, getAttributePA(varName, att));
    }
  }

  /**
   * This reads the attributes for the group and all its parent groups in a netcdf file.
   *
   * @param group without trailing slash. Use "" for root group.
   * @param attributes the Attributes that will be added to. If group was specified but found, this
   *     just returns the root group atts. If group was specified, the group's (and parent groups)
   *     global attributes are not marked with the name of the group -- it's as if all the global
   *     atts from all levels were thrown in together (with preference for lower level).
   */
  public static void getGroupAttributes(Group group, Attributes attributes) {
    while (group != null) {
      getAttributes(group.attributes(), attributes);
      group = group.getParentGroup(); // returns null if this is root group
    }
  }

  /**
   * This is the low level method to add the attributes (as is) from ncAtts to attributes.
   *
   * @param ncAtts global or variable attributes from a nc file
   * @param attributes the attributes that will be added to.
   */
  public static void getAttributes(AttributeContainer ncAtts, Attributes attributes) {

    // read the sourcglobalAttributes
    if (ncAtts == null) return;
    String ncAttsName = ncAtts.getName();
    Iterator it = ncAtts.iterator();
    while (it.hasNext()) { // there is also a dods.dap.Attribute
      Attribute att = (Attribute) it.next();
      String name = att.getName();
      attributes.add(name, getAttributePA(ncAttsName, att));
    }
  }

  /**
   * This adds to the attributes for a netcdf variable.
   *
   * @param variable
   * @param attributes the Attributes that will be added to.
   * @return the same attributes object, for convenience
   */
  public static Attributes getVariableAttributes(Variable variable, Attributes attributes) {
    if (variable == null) return attributes;
    getAttributes(variable.attributes(), attributes);

    // in nc3 files, if variables has _Unsigned=true, convert some signed attributes to unsigned
    // String2.log(">> getVariableAttributes var=" + variable.getFullName() + " _Unsigned=" +
    // attributes.getString("_Unsigned"));
    PrimitiveArray us = attributes.remove("_Unsigned");
    if (us != null && "true".equals(us.toString())) attributes.convertSomeSignedToUnsigned();
    return attributes;
  }

  /**
   * Given a ncFile and the name of the pseudo-data Variable with the projection information, this
   * tries to get the attributes and then gatherGridMappingAtts.
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#grid-mappings-and-projections
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#rotated-pole-grid-ex
   *
   * @param gridMappingVarName eg from attributes.getString("grid_mapping")
   * @return gridMappingAttributes in a form suitable for addition to the global addAttributes (or
   *     null if trouble, e.g., varName is null).
   */
  public static Attributes getGridMappingAtts(NetcdfFile ncFile, String gridMappingVarName) {
    if (gridMappingVarName == null) return null;
    return getGridMappingAtts(ncFile.findVariable(gridMappingVarName));
  }

  /**
   * Given what might be the netcdf pseudo-data Variable with the projection information, this tries
   * to get the attributes and then gatherGridMappingAtts.
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#grid-mappings-and-projections
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#rotated-pole-grid-ex
   *
   * @param pseudoDataVariable
   * @return gridMappingAttributes in a form suitable for addition to the global addAttributes (or
   *     null if trouble, e.g., variable is null).
   */
  public static Attributes getGridMappingAtts(Variable pseudoDataVariable) {
    if (pseudoDataVariable == null) return null;
    Attributes sourceAtts = new Attributes();
    getVariableAttributes(pseudoDataVariable, sourceAtts);
    return getGridMappingAtts(sourceAtts);
  }

  /**
   * Call this with the sourceAtts that might be from the pseudo-variable with a grid_mapping_name
   * attribute in order to collect the grid_mapping attributes.
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#grid-mappings-and-projections
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#rotated-pole-grid-ex
   *
   * @param sourceAtts
   * @return gridMappingAttributes in a form suitable for addition to the global addAttributes (or
   *     null if trouble, e.g., sourceAtts is null)
   */
  public static Attributes getGridMappingAtts(Attributes sourceAtts) {
    if (sourceAtts == null
        || sourceAtts.getString("grid_mapping_name") == null) // it's the one required att
    return null;
    Attributes gridMappingAtts = new Attributes();
    String[] attNames = sourceAtts.getNames();
    if (attNames.length == 0) return null;
    for (int an = 0; an < attNames.length; an++) {
      String attName = attNames[an];
      boolean keep = true;
      if ("comment".equals(attName)
          && sourceAtts.getString(attName).startsWith("This is a container variable")) keep = false;
      if (!"DODS_strlen".equals(attName) && keep)
        gridMappingAtts.add(
            "grid_mapping_name".equals(attName) ? attNames[an] : "grid_mapping_" + attName,
            sourceAtts.get(attName)); // some PA type
    }
    return gridMappingAtts;
  }

  /**
   * If the var has time units, or scale_factor and add_offset, the values in the dataPa will be
   * unpacked (numeric times will be epochSeconds). If missing_value and/or _FillValue are used,
   * they are converted to PA standard mv.
   *
   * @param var this method gets the info it needs (_Unsigned, scale_factor, add_offset, units,
   *     _FillValue, missingvalue) from the var.
   * @param dataPa A data pa or an attribute value pa. It isn't an error if dataPa = null.
   * @param lookForUnsigned should be true for the main PA, but false when converting attribute
   *     PA's.
   * @return pa The same pa or a new pa or null (if the pa parameter = null). missing_value and
   *     _FillValue will be converted to PA standard mv for the the unpacked datatype (or current
   *     type if not packed).
   */
  public static PrimitiveArray unpackPA(
      Variable var, PrimitiveArray dataPa, boolean lookForStringTimes, boolean lookForUnsigned) {

    if (dataPa == null) return dataPa;
    Attributes atts = new Attributes();
    getVariableAttributes(var, atts);
    return atts.unpackPA(var.getFullName(), dataPa, lookForStringTimes, lookForUnsigned);
  }

  /**
   * This returns the double value of an attribute. If the attribute DataType is DataType.FLOAT,
   * this nicely converts the float to a double.
   *
   * @param attribute
   * @return the double value of an attribute.
   */
  public static double getNiceDouble(Attribute attribute) {
    double d = attribute.getNumericValue().doubleValue();
    if (attribute.getDataType() == DataType.FLOAT) return Math2.floatToDouble(d);
    return d;
  }

  /**
   * Given an ascending sorted .nc Variable, this finds the index of an instance of the value (not
   * necessarily the first or last instance) (or -index-1 where it should be inserted).
   *
   * @param variable an ascending sorted nc variable. The variable can have nDimensions, but the
   *     size of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (size - 1)
   * @param value the value you are searching for
   * @return the index of an instance of the value (not necessarily the first or last instance) (or
   *     -index-1 where it should be inserted, with extremes of -lowPo-1 and -(highPo+1)-1).
   * @throws Exception if trouble
   */
  public static int binarySearch(Variable variable, int lowPo, int highPo, double value)
      throws Exception {
    // String2.log("binSearch lowPo=" + lowPo + " highPo=" + highPo + " value=" + value);

    // ensure lowPo <= highPo
    // lowPo == highPo is handled by the following two chunks of code
    Test.ensureTrue(lowPo <= highPo, String2.ERROR + "in NcHelper.binarySearch: lowPo > highPo.");

    double tValue = getDouble(variable, lowPo);
    if (tValue == value) return lowPo;
    if (tValue > value) return -lowPo - 1;

    tValue = getDouble(variable, highPo);
    if (tValue == value) return highPo;
    if (tValue < value) return -(highPo + 1) - 1;

    // repeatedly look at midpoint
    // If no match, this always ends with highPo - lowPo = 1
    //  and desired value would be in between them.
    while (highPo - lowPo > 1) {
      int midPo = (highPo + lowPo) / 2;
      tValue = getDouble(variable, midPo);
      // String2.log("binSearch midPo=" + midPo + " tValue=" + tValue);
      if (tValue == value) return midPo;
      if (tValue < value) lowPo = midPo;
      else highPo = midPo;
    }

    // no exact match
    return -highPo - 1;
  }

  /**
   * Given an ascending sorted .nc variable and a row, this finds the first row that has that same
   * value (which may be different if there are ties).
   *
   * @param variable an ascending sorted nc Variable. The variable can have nDimensions, but the
   *     size of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param row the starting row
   * @return the first row that has the same value
   */
  public static int findFirst(Variable variable, int row) throws Exception {
    // would be much more efficient if read several values at once
    double value = getDouble(variable, row);
    while (row > 0 && getDouble(variable, row - 1) == value) row--;
    return row;
  }

  /**
   * Given an ascending sorted .nc variable and a row, this finds the last row that has that same
   * value (which may be different if there are ties).
   *
   * @param variable an ascending sorted nc Variable. The variable can have nDimensions, but the
   *     size of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param row the starting row
   * @return the last row that has the same value
   */
  public static int findLast(Variable variable, int row) throws Exception {
    // would be much more efficient if read several values at once
    double value = getDouble(variable, row);
    int size1 = variable.getDimension(0).getLength() - 1;
    while (row < size1 && getDouble(variable, row + 1) == value) row++;
    return row;
  }

  /**
   * Given an ascending sorted .nc Variable, this finds the index of the first element >= value.
   *
   * @param variable an ascending sorted nc Variable. The variable can have nDimensions, but the
   *     size of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (size - 1)
   * @param value the value you are searching for
   * @return the index of the first element >= value (or highPo + 1, if there are none)
   * @throws Exception if trouble
   */
  public static int binaryFindFirstGE(Variable variable, int lowPo, int highPo, double value)
      throws Exception {
    if (lowPo > highPo) return highPo + 1;
    int po = binarySearch(variable, lowPo, highPo, value);

    // an exact match? find the first exact match
    if (po >= 0) {
      while (po > lowPo && getDouble(variable, po - 1) == value) po--;
      return po;
    }

    // no exact match? return the binary search po
    // thus returning a positive number
    // the inverse of -x-1 is -x-1 !
    return -po - 1;
  }

  /**
   * Given an ascending sorted .nc Variable, this finds the index of the last element <= value.
   *
   * @param variable an ascending sorted nc Variable. The variable can have nDimensions, but the
   *     size of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (originalPrimitiveArray.size() - 1)
   * @param value the value you are searching for
   * @return the index of the first element <= value (or -1, if there are none)
   * @throws Exception if trouble
   */
  public static int binaryFindLastLE(Variable variable, int lowPo, int highPo, double value)
      throws Exception {

    if (lowPo > highPo) return -1;

    int po = binarySearch(variable, lowPo, highPo, value);

    // an exact match? find the last exact match
    if (po >= 0) {
      while (po < highPo && getDouble(variable, po + 1) == value) po++;
      return po;
    }

    // no exact match? return binary search po -1
    // thus returning a positive number
    // the inverse of -x-1 is -x-1 !
    return -po - 1 - 1;
  }

  /**
   * Given an ascending sorted .nc Variable, this finds the index of the element closest to value.
   *
   * @param variable an ascending sorted nc Variable. The variable can have nDimensions, but the
   *     size of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param value the value you are searching for
   * @return the index of the first element found which is closest to value (there may be other
   *     identical values)
   * @throws Exception if trouble
   */
  public static int binaryFindClosest(Variable variable, double value) throws Exception {
    return binaryFindClosest(variable, 0, variable.getDimension(0).getLength() - 1, value);
  }

  /**
   * Given an ascending sorted .nc Variable, this finds the index of the element closest to value.
   *
   * @param variable an ascending sorted nc Variable. The variable can have nDimensions, but the
   *     size of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with (e.g., length-1)
   * @param value the value you are searching for
   * @return the index of the first element found which is closest to value (there may be other
   *     identical values)
   * @throws Exception if trouble
   */
  public static int binaryFindClosest(Variable variable, int lowPo, int highPo, double value)
      throws Exception {

    int po = binarySearch(variable, lowPo, highPo, value);
    // String2.log("po1=" + po);
    // an exact match? find the first exact match
    if (po >= 0) {
      // would be much more efficient if read several values at once
      while (po > lowPo && getDouble(variable, po - 1) == value) po--;
      return po;
    }

    // insertionPoint at end point?
    int insertionPoint = -po - 1; // 0.. highPo
    if (insertionPoint == 0) return 0;
    if (insertionPoint >= highPo) return highPo;

    // insertionPoint between 2 points
    if (Math.abs(getDouble(variable, insertionPoint - 1) - value)
        < Math.abs(getDouble(variable, insertionPoint) - value)) return insertionPoint - 1;
    else return insertionPoint;
  }

  /**
   * This is like getPrimitiveArray, but converts it to DoubleArray. If it was FloatArray, this
   * rounds the values nicely to doubles.
   *
   * @param variable the variable to be read from. The variable can have nDimensions, but the size
   *     of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param firstRow the value of the leftmost dimension
   * @param lastRow the last row to be read (inclusive). If lastRow = -1, firstRow is ignored and
   *     the entire var is read.
   * @return the values in a DoubleArray
   * @throws Exception if trouble
   */
  public static DoubleArray getNiceDoubleArray(Variable variable, int firstRow, int lastRow)
      throws Exception {

    PrimitiveArray pa =
        lastRow == -1
            ? getPrimitiveArray(variable)
            : getPrimitiveArray(variable, firstRow, lastRow);
    if (pa instanceof DoubleArray) {
      return (DoubleArray) pa;
    }

    DoubleArray da = new DoubleArray(pa);
    if (pa instanceof FloatArray) {
      int n = da.size();
      for (int i = 0; i < n; i++) da.array[i] = Math2.floatToDouble(da.array[i]);
    }
    return da;
  }

  /**
   * This reads the number at 'row' from a numeric variable and returns a double.
   *
   * @param variable the variable to be read from. The variable can have nDimensions, but the size
   *     of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param row the value of the leftmost dimension
   * @return the value at 'row'
   */
  public static double getDouble(Variable variable, int row) throws Exception {
    int nDim = variable.getRank();
    int origin[] = new int[nDim]; // all 0's
    int shape[] = new int[nDim];
    origin[0] = row;
    Arrays.fill(shape, 1);

    Array array = variable.read(origin, shape);
    return toDoubleArray(array)[0];
  }

  /**
   * This reads the number at 'row' from a numeric variable and returns a nice double (floats are
   * converted with Math2.floatToDouble).
   *
   * @param variable the variable to be read from. The variable can have nDimensions, but the size
   *     of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
   * @param row the value of the leftmost dimension
   * @return the value at 'row'
   */
  public static double getNiceDouble(Variable variable, int row) throws Exception {
    double d = getDouble(variable, row);
    if (variable.getDataType() == DataType.FLOAT) return Math2.floatToDouble(d);
    return d;
  }

  /**
   * This reads a range of values from an nDimensional variable in a NetcdfFile. Use
   * getPrimitiveArray(variable) if you need all of the data.
   *
   * @param variable the variable to be read from. The variable can have nDimensions, but the size
   *     of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1] (or non-leftmost and
   *     non-rightmost (nChar/String) dimensions for char/String variables, e.g.,
   *     [1047][1][1][1][20]).
   * @param firstRow the first row to be read
   * @param lastRow the last row to be read. (inclusive). If lastRow=-1, firstRow is ignored and the
   *     entire variable is returned.
   * @return the values in a PrimitiveArray
   * @throws Exception if trouble
   */
  public static PrimitiveArray getPrimitiveArray(Variable variable, int firstRow, int lastRow)
      throws Exception {

    if (lastRow == -1) return getPrimitiveArray(variable);

    boolean isChar = variable.getDataType() == DataType.CHAR;
    int nDim = variable.getRank();
    int oShape[] = variable.getShape();
    // ???verify that shape is valid?
    int origin[] = new int[nDim]; // all 0's
    int shape[] = new int[nDim];
    origin[0] = firstRow;
    Arrays.fill(shape, 1);
    int nRows = lastRow - firstRow + 1;
    shape[0] = nRows;
    if (isChar) shape[nDim - 1] = oShape[nDim - 1]; // nChars / String
    PrimitiveArray pa = getPrimitiveArray(variable.read(origin, shape), true, isUnsigned(variable));

    // eek! opendap returns a full-sized array!
    //     netcdf  returns a shape-sized array
    if (pa.size() < nRows)
      Test.error(
          String2.ERROR
              + " in NcHelper.getPrimitiveArray(firstRow="
              + firstRow
              + " lastRow="
              + lastRow
              + ")\n"
              + "variable.read returned too few ("
              + pa.size()
              + ").");
    if (pa.size() > nRows) {
      // it full-sized; reduce to correct size
      if (reallyVerbose)
        String2.log("    NcHelper.getPrimitiveArray variable.read returned entire variable!");
      pa.removeRange(
          lastRow + 1, pa.size()); // remove tail first (so don't have to move it when remove head
      pa.removeRange(0, firstRow - 1); // remove head section
    }
    return pa;
  }

  /**
   * This reads a 1D range of values from a 4D variable (or 5D if it holds strings, so 5th dimension
   * DataType is CHAR) in a NetcdfFile.
   *
   * @param variable the variable to be read from
   * @param xIndex the first x index to be read
   * @param yIndex the first y index to be read
   * @param zIndex the first z index to be read
   * @param firstT the first t index to be read
   * @param lastT the last t index to be read
   * @return the values in a PrimitiveArray
   * @throws Exception if trouble
   */
  public static PrimitiveArray get4DValues(
      Variable variable, int xIndex, int yIndex, int zIndex, int firstT, int lastT)
      throws Exception {

    int rowOrigin[] = null;
    int rowShape[] = null;
    if (variable.getRank() == 5 && variable.getDataType() == DataType.CHAR) {
      rowOrigin = new int[] {firstT, zIndex, yIndex, xIndex, 0};
      rowShape = new int[] {lastT - firstT + 1, 1, 1, 1, variable.getDimension(4).getLength()};
    } else { // numeric
      rowOrigin = new int[] {firstT, zIndex, yIndex, xIndex};
      rowShape = new int[] {lastT - firstT + 1, 1, 1, 1};
    }
    Array array = variable.read(rowOrigin, rowShape);
    PrimitiveArray pa = getPrimitiveArray(array);
    Test.ensureEqual(
        pa.size(),
        lastT - firstT + 1,
        "NcHelper.getValues nFound!=nExpected.\n"
            + " name="
            + variable.getFullName()
            + " xIndex="
            + xIndex
            + " yIndex="
            + yIndex
            + " zIndex="
            + zIndex
            + " firstT="
            + firstT
            + " lastT="
            + lastT);
    return pa;
  }

  /**
   * This reads a 4D range of values from a 4D variable (or 5D if it holds strings, so 5th dimension
   * DataType is CHAR) in a NetcdfFile.
   *
   * @param variable the variable to be read from
   * @param firstX the first x index to be read
   * @param nX the number of x indexes to be read
   * @param firstY the first y index to be read
   * @param nY the number of y indexes to be read
   * @param firstZ the first z index to be read
   * @param nZ the number of z indexes to be read
   * @param firstT the first t index to be read
   * @param nT the number of t indexes to be read
   * @return the values in a PrimitiveArray (read from primitive array with loops: for t=... for
   *     z=... for y=... for x=... pa.getDouble(po++))
   * @throws Exception if trouble
   */
  public static PrimitiveArray get4DValues(
      Variable variable,
      int firstX,
      int nX,
      int firstY,
      int nY,
      int firstZ,
      int nZ,
      int firstT,
      int nT)
      throws Exception {

    int rowOrigin[] = null;
    int rowShape[] = null;
    if (variable.getRank() == 5 && variable.getDataType() == DataType.CHAR) {
      rowOrigin = new int[] {firstT, firstZ, firstY, firstX, 0};
      rowShape = new int[] {nT, nZ, nY, nX, variable.getDimension(4).getLength()};
    } else { // numeric
      rowOrigin = new int[] {firstT, firstZ, firstY, firstX};
      rowShape = new int[] {nT, nZ, nY, nX};
    }
    Array array = variable.read(rowOrigin, rowShape);
    PrimitiveArray pa = getPrimitiveArray(array);
    Test.ensureEqual(
        pa.size(),
        nX * nY * nZ * ((long) nT),
        "NcHelper.get4DValues nFound!=nExpected.\n"
            + " name="
            + variable.getFullName()
            + " firstX="
            + firstX
            + " nX="
            + nX
            + " firstY="
            + firstY
            + " nT="
            + nY
            + " firstZ="
            + firstZ
            + " nZ="
            + nZ
            + " firstT="
            + firstT
            + " nT="
            + nT);
    return pa;
  }

  /**
   * This opens the nc file and gets the names of the (pseudo)structure variables.
   *
   * @param fullName
   * @return StringArray variableNames
   * @throws Exception if trouble
   */
  public static String[] readColumnNames(String fullName) throws Exception {
    String tColumnNames[] = null;
    NetcdfFile netcdfFile = openFile(fullName);
    try {
      Variable loadVariables[] = findVariables(netcdfFile, null);
      tColumnNames = new String[loadVariables.length];
      for (int i = 0; i < loadVariables.length; i++)
        tColumnNames[i] = loadVariables[i].getFullName();

      return tColumnNames;
    } finally {
      try {
        netcdfFile.close(); // make sure it is explicitly closed
      } catch (Exception e2) {
      }
    }
  }

  /**
   * This writes values to a 1D netcdf variable in a NetcdfFormatWriter. This works with all
   * PrimitiveArray types, but in nc3mode: <br>
   * LongArray and ULongArray is stored as doubles, so retrieve with <br>
   * pa = new LongArray(pa) or new ULongArray(pa), and <br>
   * CharArray is stored as chars (ISO-8859-1).
   *
   * @param netcdfFileWriter
   * @param variableName
   * @param firstRow This is the origin/where to write this chunk of data within the complete var in
   *     the nc file.
   * @param pa will be converted to the appropriate numeric type
   * @throws Exception if trouble
   */
  public static void write(
      boolean nc3Mode,
      NetcdfFormatWriter netcdfFileWriter,
      Variable var,
      int firstRow,
      PrimitiveArray pa)
      throws Exception {

    write(nc3Mode, netcdfFileWriter, var, new int[] {firstRow}, new int[] {pa.size()}, pa);
  }

  /**
   * This is a thin wrapper to call netcdfFileWriter.writeStringDataToChar (for StringArray) or
   * netcdfFileWriter.write to write the pa to the file.
   *
   * <p>This will convert CharArray to ShortArray and LongArray to StringArray, but it usually saves
   * memory if caller does it.
   *
   * @param netcdfFileWriter
   * @param var
   * @param origin The start of where the data will be written in the var. Don't include the
   *     StringLength dimension.
   * @param shape The shape that the data will be arranged into in the var. Don't include
   *     StringLength dimension.
   * @param pa the data to be written
   */
  public static void write(
      boolean nc3Mode,
      NetcdfFormatWriter netcdfFileWriter,
      Variable var,
      int origin[],
      int shape[],
      PrimitiveArray pa)
      throws Exception {

    PAType paType = pa.elementType();
    if (paType == PAType.CHAR)
      // netcdf-java 3 & 4 just write 1 byte chars
      // (but nc4 will writes strings as utf-8 encoded
      pa = new CharArray(pa).toIso88591();
    else if (nc3Mode) {
      if (paType == PAType.LONG || paType == PAType.ULONG) pa = new DoubleArray(pa);
      else if (paType == PAType.CHAR)
        pa = new CharArray(pa).toIso88591(); // netcdf-java 3 just writes low byte
      else if (paType == PAType.STRING)
        pa = new StringArray(pa).toIso88591(); // netcdf-java 3 just writes low byte
    }

    if (nc3Mode && paType == PAType.STRING) {
      netcdfFileWriter.writeStringDataToChar(
          var, origin, Array.factory(DataType.STRING, shape, pa.toObjectArray()));
    } else {
      netcdfFileWriter.write(
          var.getFullName(),
          origin,
          Array.factory(getNc3DataType(paType), shape, pa.toObjectArray()));
    }
  }

  /**
   * This opens an opendap dataset as a NetcdfDataset. ALWAYS explicitly use netcdfDataset.close()
   * when you are finished with it, preferably in a "finally" clause.
   *
   * @param url This should not include a list of variables (e.g., ?varName,varName) or a constraint
   *     expression at the end.
   * @return a NetcdfDataset which is a superclass of NetcdfFile
   * @throws Exception if trouble
   */
  public static NetcdfDataset openOpendapAsNetcdfDataset(String url) throws Exception {
    return NetcdfDatasets.openDataset(url); // 2021 v5.4.1: 's' is new API
  }

  /**
   * This determines which rows of an .nc file have testVariables between min and max. This is
   * faster if the first testVariable(s) eliminate a lot of rows.
   *
   * @param testVariables the variables to be tested. They must all be ArrayXxx.D1 or ArrayChar.D2
   *     variables and use the same, one, dimension as the first dimension. This must not be null or
   *     empty. If you have variable names, use ncFile.findVariable(name);
   * @param min the minimum acceptable values for the testVariables
   * @param max the maximum acceptable values for the testVariables
   * @return okRows
   * @throws Exception if trouble
   */
  public static BitSet testRows(Variable testVariables[], double min[], double max[])
      throws Exception {

    String errorInMethod = String2.ERROR + " in testNcRows: ";
    long time = System.currentTimeMillis();

    BitSet okRows = null;
    long cumReadTime = 0;
    for (int col = 0; col < testVariables.length; col++) {
      Variable variable = testVariables[col];
      if (col == 0) {
        int nRows = variable.getDimension(0).getLength();
        okRows = new BitSet(nRows);
        okRows.set(0, nRows); // make all 'true' initially
      }

      // read the data
      cumReadTime -= System.currentTimeMillis();
      PrimitiveArray pa = getPrimitiveArray(variable.read());
      cumReadTime += System.currentTimeMillis();

      // test the data
      double tMin = min[col];
      double tMax = max[col];
      int row = okRows.nextSetBit(0);
      int lastSetBit = -1;
      while (row >= 0) {
        double d = pa.getDouble(row);
        if (d < tMin || d > tMax || Double.isNaN(d)) okRows.clear(row);
        else lastSetBit = row;
        row = okRows.nextSetBit(row + 1);
      }
      if (lastSetBit == -1) return okRows;
    }
    String2.log(
        "testNcRows totalTime="
            + (System.currentTimeMillis() - time)
            + " cumReadTime="
            + cumReadTime);
    return okRows;
  }

  /**
   * This writes the PrimitiveArrays into an .nc file. This works with all PrimitiveArray types, but
   * some datatypes (chars, longs, unsigned types) are specially encoded in the files and then
   * automatically decoded when read.
   *
   * @param fullName for the file (This writes to an intermediate file then renames quickly.)
   * @param varNames Names mustn't have internal spaces.
   * @param pas Each may have a different size! (Which makes this method different than
   *     Table.saveAsFlatNc.) In the .nc file, each will have a dimension with the same name as the
   *     varName. All must have at least 1 value.
   * @throws Exception if trouble
   */
  public static void writePAsInNc3(String fullName, StringArray varNames, PrimitiveArray pas[])
      throws Exception {
    String msg = "  NcHelper.writePAsInNc3 " + fullName;
    long time = System.currentTimeMillis();

    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);
    File2.delete(fullName);

    // tpas is same as pas, but with CharArray, LongArray and ULongArray converted to StringArray
    int nVars = varNames.size();
    PrimitiveArray tpas[] = new PrimitiveArray[nVars];

    // open the file (before 'try'); if it fails, no temp file to delete
    NetcdfFormatWriter ncWriter = null;
    try {
      NetcdfFormatWriter.Builder nc = NetcdfFormatWriter.createNewNetcdf3(fullName + randomInt);
      Group.Builder rootGroup = nc.getRootGroup();
      nc.setFill(false);

      // add the variables
      Variable.Builder newVars[] = new Variable.Builder[nVars];
      for (int var = 0; var < nVars; var++) {
        String name = varNames.get(var);
        tpas[var] = pas[var];
        if (tpas[var].elementType() == PAType.CHAR) {
          // nc 'char' is 1 byte!  So store java char (2 bytes) as shorts.
          tpas[var] = new ShortArray(((CharArray) pas[var]).toArray());
        } else if (tpas[var].elementType() == PAType.LONG
            || tpas[var].elementType() == PAType.ULONG) {
          // these will always be decoded by fromJson as-is; no need to encode with toJson
          tpas[var] = new StringArray(pas[var]);
        } else if (tpas[var].elementType() == PAType.STRING) {
          // .nc strings only support characters 1..255, so encode as Json strings
          StringArray oldSa = (StringArray) pas[var];
          int tSize = oldSa.size();
          StringArray newSa = new StringArray(tSize, false);
          tpas[var] = newSa;
          for (int i = 0; i < tSize; i++) {
            // Only save json version if oldS part of it (inside enclosing "'s) is different.
            // This minimizes new Strings, memory usage, garbage collection, etc.
            // Note that oldS starting and ending with " will be changed when encoded
            //  and so newS will be used.
            String oldS = oldSa.get(i);
            String newS = String2.toJson(oldS);
            newSa.add(newS.substring(1, newS.length() - 1).equals(oldS) ? oldS : newS);
          }
        }

        PAType type = tpas[var].elementType();
        Dimension dimension = NcHelper.addDimension(rootGroup, name, tpas[var].size());
        if (type == PAType.STRING) {
          int max =
              Math.max(
                  1,
                  ((StringArray) tpas[var])
                      .maxStringLength()); // nc libs want at least 1; 0 happens if no data
          Dimension lengthDimension =
              NcHelper.addDimension(rootGroup, name + StringLengthSuffix, max);
          newVars[var] =
              NcHelper.addVariable(
                  rootGroup, name, DataType.CHAR, Arrays.asList(dimension, lengthDimension));
          if (pas[var].elementType() == PAType.LONG)
            newVars[var].addAttribute(new Attribute("NcHelper", originally_a_LongArray));
          else if (pas[var].elementType() == PAType.ULONG)
            newVars[var].addAttribute(new Attribute("NcHelper", originally_a_ULongArray));
          else if (pas[var].elementType() == PAType.STRING)
            newVars[var].addAttribute(new Attribute("NcHelper", "JSON encoded"));
        } else {
          newVars[var] =
              NcHelper.addVariable(rootGroup, name, getNc3DataType(type), Arrays.asList(dimension));
          if (pas[var].elementType() == PAType.CHAR)
            newVars[var].addAttribute(new Attribute("NcHelper", originally_a_CharArray));
        }
      }

      // leave "define" mode
      ncWriter = nc.build();

      // write the data
      for (int var = 0; var < nVars; var++) {
        ncWriter.write(newVars[var].getFullName(), get1DArray(tpas[var]));
      }

      // if close throws exception, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      File2.rename(fullName + randomInt, fullName);

      // diagnostic
      if (reallyVerbose)
        String2.log(msg + " finished. TIME=" + (System.currentTimeMillis() - time) + "ms");
      // String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

    } catch (Exception e) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(fullName + randomInt);
        ncWriter = null;
      }

      if (!reallyVerbose) String2.log(msg);

      throw e;
    }
  }

  /**
   * This reads the PAs in the .nc file. This works with all PrimitiveArray types, but some
   * datatypes (char, long, unsigned, String) are specially encoded in the files and then
   * automatically decoded when read, so that this fully supports 2byte chars, longs, and Unicode
   * Strings (by encoding as json in file).
   *
   * @param fullName the name of the .nc file.
   * @param loadVarNames the names of the variables to load (null to get all)
   * @param varNames this will receive the varNames
   * @return PrimitiveArray[] paralleling the names in varNames
   * @throws Exception if trouble (e.g., one of the loadVarNames wasn't found)
   */
  public static PrimitiveArray[] readPAsInNc3(
      String fullName, String loadVarNames[], StringArray varNames) throws Exception {

    String msg = "  NcHelper.readPAsInNc3 " + fullName
        // + " \n  loadVarNames=" + String2.toCSSVString(loadVarNames)
        ;
    varNames.clear();
    ArrayList<PrimitiveArray> pas = new ArrayList();
    long time = System.currentTimeMillis();
    NetcdfFile netcdfFile = openFile(fullName);
    try {
      List<Variable> rootGroupVariables = null;
      if (loadVarNames == null) {
        // find variables in the rootGroup
        Group rootGroup = netcdfFile.getRootGroup();
        rootGroupVariables = rootGroup.getVariables();
      }
      int n = loadVarNames == null ? rootGroupVariables.size() : loadVarNames.length;

      for (int v = 0; v < n; v++) {
        Variable tVariable =
            loadVarNames == null
                ? rootGroupVariables.get(v)
                : netcdfFile.findVariable(loadVarNames[v]);
        if (tVariable == null)
          throw new RuntimeException(
              String2.ERROR
                  + ": Expected variable #"
                  + v
                  + " not found while reading "
                  + fullName
                  + " (loadVarNames="
                  + String2.toCSSVString(loadVarNames)
                  + ").");
        List<Dimension> tDimensions = tVariable.getDimensions();
        int nDimensions = tDimensions.size();
        // if (reallyVerbose) String2.log("i=" + i + " name=" + tVariable.getFullName() +
        //    " type=" + tVariable.getNc3DataType());
        if (nDimensions == 1 || (nDimensions == 2 && tVariable.getDataType() == DataType.CHAR)) {
          varNames.add(tVariable.getFullName());
          PrimitiveArray pa = getPrimitiveArray(tVariable);

          // decode the pa?
          Attribute att = tVariable.findAttribute("NcHelper");
          if (att != null && att.isString()) {
            String ncHelper = att.getStringValue();
            if (ncHelper == null) ncHelper = "";
            if (pa.elementType() == PAType.SHORT && ncHelper.indexOf(originally_a_CharArray) >= 0) {
              pa = new CharArray(((ShortArray) pa).toArray());
            } else if (pa.elementType() == PAType.STRING
                && ncHelper.indexOf(originally_a_LongArray) >= 0) { // before JSON test
              pa = new LongArray(pa);
            } else if (pa.elementType() == PAType.STRING
                && ncHelper.indexOf(originally_a_ULongArray) >= 0) { // before JSON test
              pa = new ULongArray(pa);
            } else if (pa.elementType() == PAType.STRING && ncHelper.indexOf("JSON encoded") >= 0) {
              int tSize = pa.size();
              for (int i = 0; i < tSize; i++) pa.setString(i, String2.fromJson(pa.getString(i)));
            }
          }

          // add the decoded pa
          pas.add(pa);
        }
      }
    } finally {
      netcdfFile.close();
    }

    int pasSize = pas.size();
    PrimitiveArray paar[] = new PrimitiveArray[pasSize];
    for (int i = 0; i < pasSize; i++) paar[i] = pas.get(i);
    if (reallyVerbose)
      String2.log(
          msg + " done. nPAs=" + pasSize + " TIME=" + (System.currentTimeMillis() - time) + "ms");
    return paar;
  }

  /**
   * This writes the contents of Attributes to an .nc3 file, so the .nc file can be a key-value-pair
   * (KVP) repository. This works with all PrimitiveArray types, but some datatypes are specially
   * encoded in the files and then automatically decoded when read.
   *
   * @param fullName for the file (This doesn't write to an intermediate file.)
   * @param attributes Names mustn't have internal spaces.
   * @throws Exception if trouble (if an attribute's values are in a LongArray, ULongArray, unsigned
   *     array)
   */
  public static void writeAttributesToNc3(String fullName, Attributes attributes) throws Exception {

    // gather the names and primitiveArrays
    String names[] = attributes.getNames();
    int nNames = names.length;
    PrimitiveArray pas[] = new PrimitiveArray[nNames];
    for (int i = 0; i < nNames; i++) pas[i] = attributes.get(names[i]);

    // write to .nc
    writePAsInNc3(fullName, new StringArray(names), pas);
  }

  /**
   * This reads the specified Attribute from an .nc file which was created by writeAttributesToNc.
   * This works with all PrimitiveArray types, but some datatypes are specially encoded in the files
   * and then automatically decoded when read.
   *
   * @param fullName for the file
   * @return the PrimitiveArray corresponding to name (e.g., fullName file or variable name not
   *     found)
   * @throws exception if trouble (e.g., name not found)
   */
  public static PrimitiveArray readAttributeFromNc(String fullName, String name) throws Exception {

    return readPAsInNc3(fullName, new String[] {name}, new StringArray())[0];
  }

  /**
   * This reads the specified Attributes from an .nc3 file which was created by writeAttributesToNc.
   * This works with all PrimitiveArray types, but some datatypes are specially encoded in the files
   * and then automatically decoded when read.
   *
   * @param fullName for the file
   * @return PrimitiveArray[] which parallels names
   * @throws exception if trouble (e.g., fullName file or one of the names not found)
   */
  public static PrimitiveArray[] readAttributesFromNc3(String fullName, String names[])
      throws Exception {

    return readPAsInNc3(fullName, names, new StringArray());
  }

  /**
   * This reads all Attributes from an .nc3 file which was created by writeAttributesToNc, and
   * creates an Attributes object. This works with all PrimitiveArray types, but some datatypes are
   * specially encoded in the files and then automatically decoded when read.
   *
   * @param fullName for the file
   * @return attributes
   * @throws Exception if trouble
   */
  public static Attributes readAttributesFromNc3(String fullName) throws Exception {

    StringArray names = new StringArray();
    PrimitiveArray pas[] = readPAsInNc3(fullName, null, names);
    Attributes atts = new Attributes();
    int nNames = names.size();
    for (int i = 0; i < nNames; i++) atts.add(names.get(i), pas[i]);
    return atts;
  }

  /**
   * This reads the specified data from a multidimensional structure.
   *
   * @param nc the netcdfFile
   * @param structureName the name of the structure
   * @param memberNames the name of the desired members of the structure). If a memberName isn't a
   *     member in this file, the PA returned will be all CoHort mv
   * @param tConstraints For each axis variable, there will be 3 numbers (startIndex, stride,
   *     stopIndex). !!! If there is a special axis0, this will not include constraints for axis0.
   * @return a PrimitiveArray[], with one for each varName. If a memberName wasn't found, its
   *     PrimitiveArray will be null
   * @throws Exception if trouble: IOError, structureName isn't found or isn't a structure,
   *     unexpected nDimensions, or similar. MemberName not found isn't an error.
   */
  public static PrimitiveArray[] readStructure(
      NetcdfFile nc, String structureName, String memberNames[], IntArray tConstraints)
      throws Exception {

    Variable v = nc.findVariable(structureName);
    if (v == null)
      throw new RuntimeException("structureName=" + structureName + " isn't the nc/hdf file.");
    if (!(v instanceof Structure))
      throw new RuntimeException(structureName + " isn't a structure.");
    Structure s = (Structure) v;

    // get structure's dimensions
    List<Dimension> dims = s.getDimensions();
    int nDim = dims.size();
    int shape[] = new int[nDim];
    for (int d = 0; d < nDim; d++) shape[d] = dims.get(d).getLength();
    NDimensionalIndex index = new NDimensionalIndex(shape);
    int currentIndex[] = index.getCurrent();
    int subsetIndex[] = index.makeSubsetIndex(structureName, tConstraints);
    boolean getAll = index.willGetAllValues(tConstraints);
    int subsetSize = index.subsetSize(tConstraints);

    StructureMembers sm = s.makeStructureMembers();
    // System.out.println("sm=" + sm);
    int nMembers = memberNames.length;
    PrimitiveArray pa[] = new PrimitiveArray[nMembers];
    for (int m = 0; m < nMembers; m++) {
      StructureMembers.Member smm = sm.findMember(memberNames[m]);
      if (smm != null)
        pa[m] =
            PrimitiveArray.factory(
                getElementPAType(smm.getDataType()), subsetSize, false); // active?
    }

    // boolean buildStringsFromChars = false;
    // boolean isUnsigned = false;
    StructureDataIterator it = s.getStructureIterator();
    int recNo = 0;
    boolean done = false;
    try {
      while (it.hasNext()) {
        StructureData sd = it.next(); // increment structure position
        boolean saveThisOne = true;
        if (!getAll) {
          index.increment(); // increment my count
          for (int d = nDim - 1; d >= 0; d--) { // for efficiency, check fastest varying first
            if (currentIndex[d] != subsetIndex[d]) {
              saveThisOne = false;
              break;
            }
          }
        }
        if (saveThisOne) {
          for (int m = 0; m < nMembers; m++) {
            if (pa[m] != null)
              pa[m].add(sd, memberNames[m]); // faster if use Member? but it returns junk numbers
          }

          if (!getAll)
            if (!index.incrementSubsetIndex(subsetIndex, tConstraints))
              break; // we got the entire subset
        }
        recNo++;
      }
    } finally {
      it.close();
    }

    return pa;
  }
}
