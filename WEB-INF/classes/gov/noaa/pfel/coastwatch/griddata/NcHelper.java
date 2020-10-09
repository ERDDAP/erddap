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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.List;

/**
 * Get netcdfAll-......jar from ftp://ftp.unidata.ucar.edu/pub
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Put it in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;


/**
 * This class has some static convenience methods related to NetCDF files
 * and the other Data classes.
 *
 * <p>!!!netcdf classes use the Apache Jakarta Commons Logging project (Apache license).
 * The files are in the netcdfAll.jar.
 * !!!The commons logging system and your logger must be initialized before using this class.
 * It is usually set by the main program (e.g., NetCheck).
 * For a simple solution, use String2.setupCommonsLogging(-1);
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-12-07
 */
public class NcHelper  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /**
     * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false;

    /**
     * Set this to true (by calling debugMode=true in your program, not by changing the code here)
     * if you want lots and lots of diagnostic messages sent to String2.log.
     */
    public static boolean debugMode = false;

    /** 
     * varName + StringLengthSuffix is used to create the name for the char dimension 
     * of a String variable. "_strlen" is what netcdf-java uses.
     */
    public final static String StringLengthSuffix = "_strlen"; //pre 2012-06-05 was StringLength="StringLength"; 

    /** Since .nc files can't store 16bit char or longs, those data types are stored as
     * shorts or doubles and these messages are added to the attributes.
     */
    public final static String originally_a_CharArray  = "originally a CharArray";
    public final static String originally_a_LongArray  = "originally a LongArray";
    public final static String originally_a_ULongArray = "originally a ULongArray";

    /**
     * If saving longs or ulongs as Strings, this is the maxStringLength.
     */
    public final static int LONG_MAXSTRINGLENGTH = 20;

    /**
     * Tell netcdf-java to object if a file is truncated 
     *  (e.g., didn't get completely copied over)
     * See email from Christian Ward-Garrison Nov 2, 2016
     */
    static {
        ucar.nc2.iosp.netcdf3.N3header.disallowFileTruncation = true;
    }

    /**
     * This is an ncdump-like method.
     *
     * @param cmd Use just one option: "-h" (header), "-c" (coord. vars),
     *  "-vall" (default), "-v var1;var2", "-v var1(0:1,:,12)"
     *  "-k" (type), "-t" (human-readable times)  -- These 2 options don't work.
     */
     public static String ncdump(String fileName, String cmd) throws Exception{
        NetcdfFile nc = null;
        try {  
            nc = openFile(fileName);
            return ncdump(nc, cmd);
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return "Unable to open file or file not .nc-compatible.";
        } finally {
            try { nc.close(); } catch (Throwable t2) {}
        }
     }

     public static String ncdump(NetcdfFile nc, String cmd) throws Exception{
        try {  
            StringWriter sw = new StringWriter();
            //-vall is NCdumpW version of default for ncdump
            NCdumpW.print(nc, String2.isSomething(cmd)? cmd : "-vall", sw, null); 
            return decodeNcDump(sw.toString());
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return "Error while generating ncdump string.";
        }
     }

    /**
     * This is like String2.fromJson, but does less decoding and is made specifically to fix up 
     * unnecessarily encoded attributes in NcDump String starting with netcdf-java 4.0.
     * This is like replaceAll, but smarter.
     * null is returned as null.
     *
     * @param s
     * @return the decoded string
     */
    public static String decodeNcDump(String s) {
        StringBuilder sb = new StringBuilder();
        int sLength = s.length();

        //remove dir name from first line (since this is sometimes sent to ERDDAP users)    
        //"netcdf /data/_erddapBPD/cache/_test/EDDGridFromDap_Axis.nc {\n" +
        int po = 0;
        int brPo = s.indexOf(" {"); 
        if (brPo >= 0) {
            int slPo = s.substring(0, brPo).lastIndexOf('/');
            if (slPo < 0)
                slPo = s.substring(0, brPo).lastIndexOf('\\');
            if (slPo >= 0) {
                sb.append("netcdf ");
                po = slPo + 1;
            }
        }

        //process rest of s
        while (po < sLength) {
            char ch = s.charAt(po);
            if (ch == '\\') {
                if (po == sLength - 1) 
                    po--;  //so reread \ and treat as \\
                po++; 
                ch = s.charAt(po);
                if (ch == 'n') sb.append('\n');
                else if (ch == 'r') {}
                else if (ch == '\'') sb.append('\'');
                else sb.append("\\" + ch); //leave as backslash encoded
            } else if (ch == '\r') { //2013-09-03 netcdf-java 4.3 started using \r\n on non-data lines. Remove \r.
            } else {
                sb.append(ch);
            }
            po++;
        }
        return sb.toString();
    }


    /**
     * This is like fromJson, but specifically designed to 
     * decode an attribute, starting with netcdf-java 4.0 
     * (which returns them in a backslash encoded form).
     * null is returned as null
     *
     * @param o
     * @return the decoded o
     */
    public static Object decodeAttribute(Object o) {
        if (o == null ||
            !(o instanceof String[]))
            return o;
        String sar[] = (String[])o;
        for (int i = 0; i < sar.length; i++)
            sar[i] = String2.fromJson(sar[i]); //previously used precursor to decodeNcDump(sar[i]);
        return sar;
    }


    /**
     * This converts a ArrayXxx.D1 (of any numeric type) into a double[].
     *
     * @param array any numeric ArrayXxx.D1
     * @return a double[]
     * @throws Exception if trouble
     */
    public static double[] toDoubleArray(Array array) {
        return (double[])array.get1DJavaArray(DataType.DOUBLE);
    }


    /**
     * This returns the size of the array.
     * If n > Integer.MAX_VALUE, this throws an Exception.
     *
     * @param array  an ArrayChar.D2 or any ArrayXxx.D1
     * @return the length of array (the number of Strings in the ArrayChar,
     *    or the number of elements in any other array type)
     */
    public static int get1DArrayLength(Array array) {
        long n;
        if (array instanceof ArrayChar.D2) {
            n = ((ArrayChar.D2)array).getShape()[0]; 
        } else {
            n = array.getSize();
        }
        Test.ensureTrue(n < Integer.MAX_VALUE,  
            String2.ERROR + " in NcHelper.getSize; n = " + n);
        return (int)n; //safe since checked above

    }

    /** 
     * This creates an netcdf Attribute from a PrimitiveArray.
     *
     * @param name
     * @param pa 
     * @return an Attribute
     */
    public static Attribute createAttribute(boolean nc3Mode, String name, PrimitiveArray pa) {
        if (pa.elementType() == PAType.STRING) {
            if (nc3Mode) {
                //String2.log("***getAttribute nStrings=" + pa.size());
                String ts = Attributes.valueToNcString(pa);
                //int maxLength = 32000; //pre 2010-10-13 8000 ok; 9000 not; now >32K ok; unclear what new limit is
                //if (ts.length() > maxLength) 
                //    ts = ts.substring(0, maxLength - 3) + "...";
               //String2.log("***getAttribute string=\"" + ts + "\"");
                return new Attribute(name, ts);
            } else {
                String s = ((StringArray)pa).toNewlineString();
                return new Attribute(name, s.length() == 0? "" : s.substring(0, s.length() - 1));
            }
        }
        Array ar = null;
        if (pa instanceof CharArray) //pass all (Unicode) chars through unchanged (not limited to ISO_8859_1)
             ar = Array.factory(DataType.CHAR, new int[]{pa.size()}, pa.toObjectArray());
        else if (nc3Mode && (pa.elementType() == PAType.LONG || pa.elementType() == PAType.ULONG))
             ar = get1DArray(PrimitiveArray.rawFactory(PAType.DOUBLE, pa));
        else if (nc3Mode && pa.isUnsigned())
             ar = get1DArray(pa.toObjectArray(), false);  //2020-04-10 attributes in nc3 can't be unsigned
        else ar = get1DArray(pa);
        Attribute att = new Attribute(name, ar);
        //String2.log(">> NcHelper.createAttribute(" + name + ", " + pa.elementType() + ", " + pa + ") -> " + att.toString());
        return att;
    }

    /** 
     * This makes an ArrayString.D1 for use with netcdf-4.
     */
    public static ArrayString.D1 getStringArrayD1(StringArray sa) {
        int n = sa.size();
        ArrayString.D1 asd1 = new ArrayString.D1(n);
        for (int i = 0; i < n; i++)
            asd1.set(i, sa.get(i));
        return asd1;
    }


    /** 
     * This converts a String or array of primitives into a 
     * ucar.nc2.ArrayXxx.D1.
     * The o array is used as the storage for the Array.
     *
     * @param pa a PrimitiveArray
     */
    public static Array get1DArray(PrimitiveArray pa) {
        return get1DArray(pa.toObjectArray(), pa.isUnsigned());
    }

    /** 
     * This converts a String or array of primitives into a 
     * ucar.nc2.ArrayXxx.D1.
     * The o array is used as the storage for the Array.
     *
     * @param o the String or array of primitives (e.g., int[])
     * @param isUnsigned Only used for byte, short, int, long; ignored for other data types.
     * @return an ArrayXxx.D1.  A String is converted to a ArrayChar.D2.
     *    A long[] is converted to a String[] and then to ArrayChar.D2 (nc3 files don't support longs).
     */
    public static Array get1DArray(Object o, boolean isUnsigned) {
        if (o instanceof String) {
            o = ((String)o).toCharArray();
            //will be handled below
        }

        if (o instanceof char[]) {
            //netcdf-java just writes low byte, so use String2.toIso88591Chars()
            char[] car1 = (char[])o;
            int n = car1.length;
            char[] car2 = new char[n];
            for (int i = 0; i < n; i++)
                car2[i] = String2.toIso88591Char(car1[i]);
            return Array.factory(DataType.CHAR, new int[]{n}, car2);
        }


        if (o instanceof byte[])   return Array.factory(isUnsigned? DataType.UBYTE  : DataType.BYTE,   new int[]{((byte[] )o).length}, o);
        if (o instanceof short[])  return Array.factory(isUnsigned? DataType.USHORT : DataType.SHORT,  new int[]{((short[])o).length}, o);
        if (o instanceof int[])    return Array.factory(isUnsigned? DataType.UINT   : DataType.INT,    new int[]{((int[]  )o).length}, o);
        if (o instanceof long[])   {
            //String2.log("\n>> long values=" + String2.toCSSVString((long[])o));
            long lar[] = (long[])o;
            o = null;
            int tSize = lar.length;
            double dar[] = new double[tSize];
            if (isUnsigned) {   //ulong
                for (int i = 0; i < tSize; i++)
                    dar[i] = Math2.ulongToDouble(lar[i]); // !!! possible loss of precision
            } else {            //long
                for (int i = 0; i < tSize; i++)
                    dar[i] = lar[i]; // !!! possible loss of precision
            }
            o = dar;
            //then falls through to Double handling
            //String2.log(">> as doubles=" + String2.toCSSVString((double[])o));
        }
        if (o instanceof float[])  return Array.factory(DataType.FLOAT,  new int[]{((float[])o).length}, o);
        if (o instanceof double[]) return Array.factory(DataType.DOUBLE, new int[]{((double[])o).length}, o);
        if (o instanceof String[]) {
            //make ArrayChar.D2
            String[] sar = (String[])o;
            //String2.log("NcHelper.get1DArray sar=" + String2.toCSSVString(sar));
            int max = 1; //nc wants at least 1
            for (int i = 0; i < sar.length; i++) {
                //if (sar[i].length() > max) String2.log("new max=" + sar[i].length() + " s=\"" + sar[i] + "\"");
                max = Math.max(max, sar[i].length());
            }
            //String2.log("NcHelper.get1DArray String[] max=" + max);
            ArrayChar.D2 ac = new ArrayChar.D2(sar.length, max);
            for (int i = 0; i < sar.length; i++) {
                //setString just does low byte, so use String2.toIso88591String()
                ac.setString(i, String2.toIso88591String(sar[i]));  
                //String s = sar[i];
                //int sLength = s.length();
                //for (int po = 0; po < sLength; po++) 
                //    ac.set(i, po, s.charAt(po));
                //for (int po = sLength; po < max; po++)  //not necessary because filled with 0's?
                //    ac.set(i, po, '\u0000');
            }
            return ac;
        }

        Test.error(String2.ERROR + " in NcHelper.get1DArray: unexpected object type: " + 
            o.getClass().getCanonicalName() + ": " + o);
        return null;
    }

    /** 
     * This returns true if the variable's DataType isIntegral and isUnsigned.
     * This works with nc3 and nc4 files.
     * With nc3 files, this looks for the _Unsigned=true attribute
     *
     * @return true if the variable's DataType isIntegral and isUnsigned.
     */
    public static boolean isUnsigned(Variable variable) {
        DataType dt = variable.getDataType();
        if (!dt.isIntegral()) 
            return false;
        if (dt.isUnsigned())
            return true; //vars in nc4 files return correct isUnsigned status
        PrimitiveArray pa = getVariableAttribute(variable, "_Unsigned");
        if (pa != null && "true".equals(pa.toString()))
            return true;
        return false;
    }

    /**
     * This reads all of the values from an nDimensional variable.
     * The variable can be any shape.
     * This version uses buildStringsFromChars=true.
     *
     * @param variable
     * @return a suitable primitiveArray 
     */
     public static PrimitiveArray getPrimitiveArray(Variable variable) throws Exception {
         return getPrimitiveArray(variable.read(), true, isUnsigned(variable));
     }

    /**
     * This reads all of the values from an nDimensional variable.
     * The variable can be any shape.
     *
     * @param variable
     * @param buildStringsFromChars only applies to source DataType=char variables.
     * @return a suitable primitiveArray 
     */
    public static PrimitiveArray getPrimitiveArray(Variable variable, 
        boolean buildStringsFromChars) throws Exception {
        
        return getPrimitiveArray(variable.read(), buildStringsFromChars, isUnsigned(variable));
    }

    /** 
     * This converts a ucar.nc2 numeric or char ArrayXxx.Dx into a PrimitiveArray.
     *
     * @param nc2Array an nc2Array
     * @param buildStringsFromChars only applies to source DataType=char variables.
     * @param isUnsigned if true and if the object type isIntegerType, 
     *   the resulting PrimitiveArray will be an unsigned PAType.
     * @return a PrimitiveArray
     */
    public static PrimitiveArray getPrimitiveArray(Array nc2Array, boolean buildStringsFromChars, boolean isUnsigned) {
        //String2.log(">> NcHelper.getPrimitiveArray nc2Array.isUnsigned=" + nc2Array.isUnsigned());
        PrimitiveArray pa = PrimitiveArray.factory(getArray(nc2Array, buildStringsFromChars), isUnsigned);
        return pa;
    }

//was
//   * This converts a ucar.nc2 numeric ArrayXxx.D1, numeric ArrayXxx.D4,
//   *   ArrayChar.D2, or ArrayChar.D5 into an array of primitives.

    /** 
     * This converts a ucar.nc2 numeric or char ArrayXxx.Dx into a 1D array of primitives.
     * This version uses buildStringsFromChars=true, so ArrayChars are converted to String[].
     * ArrayBooleans are converted to byte[].
     * 
     * @param nc2Array an nc2Array
     * @return String[] (from ArrayChar.D1, split at \n), String[],
     *    or primitive[] (from numeric ArrayXxx.D1)
     */
    public static Object getArray(Array nc2Array) {
        return getArray(nc2Array, true);
    }

    /** 
     * This converts a ucar.nc2 numeric or char ArrayXxx.Dx into a 1D array of primitives.
     * ArrayChars are converted to String[].
     * ArrayBooleans are converted to byte[].
     * 
     * @param nc2Array an nc2Array
     * @param buildStringsFromChars
     * @return String[] (from ArrayChar.D1, split at \n), String[],
     *    or primitive[] (from numeric ArrayXxx.D1)
     */
    public static Object getArray(Array nc2Array, boolean buildStringsFromChars) {

        //String[] from ArrayChar.Dn
        if (buildStringsFromChars && nc2Array instanceof ArrayChar) {
            ArrayObject ao = ((ArrayChar)nc2Array).make1DStringArray();
            String sa[] = String2.toStringArray((Object[])ao.copyTo1DJavaArray());
            for (int i = 0; i < sa.length; i++) 
                sa[i] = String2.canonical(String2.trimEnd(sa[i]));
            return sa;
        }

        //byte[] from ArrayBoolean.Dn
        if (nc2Array instanceof ArrayBoolean) {
            boolean boolAr[] = (boolean[])nc2Array.copyTo1DJavaArray();
            int n = boolAr.length;
            byte    byteAr[] = new byte[n];
            for (int i = 0; i < n; i++)
                byteAr[i] = boolAr[i]? (byte)1 : (byte)0;
            return byteAr;
        }

        //ArrayXxxnumeric
        return nc2Array.copyTo1DJavaArray();
    }


    /** 
     * This converts an netcdf DataType into a PrimitiveArray elementType 
     * (e.g., PAType.INT for integer primitives).
     * BEWARE: This returns the stated dataType (not overthinking it).
     * .nc3 files store strings as char arrays, so 
     * if variable.getRank()==1 it is a char variable, but
     * if variable.getRank()==2 it should later be converted to to a String variable.
     * But .nc4 files have true String dataType.
     *
     * <p>Unsigned types return the same type, signed, e.g., ubyte returns byte.
     *
     * @param dataType the Netcdf dataType
     * @return the corresponding PrimitiveArray elementPAType (e.g., PAType.INT for integer primitives)
     * @throws RuntimeException if dataType is null or unexpected.
     */
     public static PAType getElementPAType(Variable var) {
         DataType dataType = var.getDataType(); //nc3 doesn't return signed types
         boolean unsigned = isUnsigned(var);    //so ask separately
         if (unsigned) {                        //and deal with
             if (dataType == DataType.BYTE)   return PAType.UBYTE;
             if (dataType == DataType.SHORT)  return PAType.USHORT;
             if (dataType == DataType.INT)    return PAType.UINT;
             if (dataType == DataType.LONG)   return PAType.LONG;
         }
         if (dataType == DataType.BOOLEAN) return PAType.BOOLEAN;
         if (dataType == DataType.BYTE)    return PAType.BYTE;
         if (dataType == DataType.UBYTE)   return PAType.UBYTE;
         if (dataType == DataType.CHAR)    return PAType.CHAR;
         if (dataType == DataType.DOUBLE)  return PAType.DOUBLE;
         if (dataType == DataType.FLOAT)   return PAType.FLOAT;
         if (dataType == DataType.INT)     return PAType.INT;
         if (dataType == DataType.UINT)    return PAType.UINT;
         if (dataType == DataType.LONG)    return PAType.LONG;
         if (dataType == DataType.ULONG)   return PAType.ULONG;
         if (dataType == DataType.SHORT)   return PAType.SHORT;
         if (dataType == DataType.USHORT)  return PAType.USHORT;
         if (dataType == DataType.STRING)  return PAType.STRING;
         //STRUCTURE not converted
         Test.error(String2.ERROR + " in NcHelper.getElementType:\n" +
             " unexpected DataType: " + dataType.toString());
         return null;
     }

     /**
      * This returns true if the dataType is one of the unsigned integer types.
      */
     public static boolean isUnsigned(DataType dataType) {
         return dataType == DataType.UINT   ||
                dataType == DataType.USHORT ||
                dataType == DataType.UBYTE  ||
                dataType == DataType.ULONG;
     }


    /** 
     * This converts an ElementType (e.g., PAType.INT for integer primitives) 
     * into an netcdf-3 DataType (so PAType.LONG returns DataType.DOUBLE).
     * BEWARE: .nc files store strings as char arrays, so 
     * if variable.getRank()==1 it is a char variable, but
     * if variable.getRang()==2 it is a String variable. [It isn't that simple!]
     * This throws Exception if elementPAType not found.
     *
     * @param elementPAType the PrimitiveArray elementPAType 
     *   (e.g., PAType.INT for integer primitives).
     *   longs are converted to doubles.
     * @return the corresponding netcdf dataType 
     */
     public static DataType getNc3DataType(PAType elementPAType) {
         if (elementPAType == PAType.BOOLEAN) return DataType.BOOLEAN; //?
         if (elementPAType == PAType.BYTE)    return DataType.BYTE;
         if (elementPAType == PAType.CHAR)    return DataType.CHAR;
         if (elementPAType == PAType.DOUBLE)  return DataType.DOUBLE;
         if (elementPAType == PAType.FLOAT)   return DataType.FLOAT;
         if (elementPAType == PAType.INT)     return DataType.INT;
         if (elementPAType == PAType.LONG)    return DataType.DOUBLE;  // long -> double in .nc3
         if (elementPAType == PAType.SHORT)   return DataType.SHORT;
         if (elementPAType == PAType.STRING)  return DataType.STRING;
         if (elementPAType == PAType.UBYTE)   return DataType.UBYTE;
         if (elementPAType == PAType.UINT)    return DataType.UINT;
         if (elementPAType == PAType.ULONG)   return DataType.DOUBLE; // ulong -> double in .nc3
         if (elementPAType == PAType.USHORT)  return DataType.USHORT;
         //STRUCTURE not converted
         Test.error(String2.ERROR + " in NcHelper.getNc3DataType:\n" +
             " unrecognized ElementType: " + elementPAType);
         return null;
     }

    /** 
     * This converts an ElementType (e.g., PAType.INT for integer primitives) 
     * into an netcdf-4 DataType.
     * This throws Exception if elementPAType not found.
     *
     * @param elementPAType the PrimitiveArray elementPAType 
     *   (e.g., PAType.INT for integer primitives).
     *   longs and ulongs are converted to doubles.
     * @return the corresponding netcdf dataType 
     */
     public static DataType getNc4DataType(PAType elementPAType) {
         //just deal with things that are different than nc3
         if (elementPAType == PAType.LONG)    return DataType.LONG;  
         if (elementPAType == PAType.ULONG)   return DataType.ULONG; 
         return getNc3DataType(elementPAType);
     }

     /**
      * This returns the appropriate DataType for nc3 or nc4 files.
      */
     public static DataType getDataType(boolean nc3Mode, PAType elementPAType) {
         return nc3Mode? 
             getNc3DataType(elementPAType) :
             getNc4DataType(elementPAType);
     }



    /**
     * From an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this generates a String with 
     * startLine+"<name>=<value>" on each line.
     * If arrayList == null, this returns startLine+"[null]\n".
     * <p>This nc version is used to print netcdf header attributes. 
     * It puts quotes around String attributeValues,
     * converts \ to \\, and " to \", and appends 'f' to float values/
     *
     * @param arrayList 
     * @return the desired string representation
     */
    public static String alternateToString(String prefix, ArrayList arrayList, String suffix) {
        if (arrayList == null)
            return prefix + "[null]\n";
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < arrayList.size(); index += 2) {
            sb.append(prefix);
            sb.append(arrayList.get(index).toString());
            sb.append(" = ");
            Object o = arrayList.get(index+1);
            if (o instanceof StringArray) {
                StringArray sa = (StringArray)o;
                int n = sa.size();
                String connect = "";
                for (int i = 0; i < n; i++) {
                    sb.append(connect);
                    connect = ", ";
                    String s = String2.replaceAll(sa.get(i), "\\", "\\\\");  //  \ becomes \\
                    s = String2.replaceAll(s, "\"", "\\\"");                 //  " becomes \"
                    sb.append("\"" + s + "\"");
                }
            } else if (o instanceof FloatArray) {
                FloatArray fa = (FloatArray)o;
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
     * <p>If the fullName is an http address, the name needs to start with "http:\\" 
     * (upper or lower case) and the server needs to support "byte ranges"
     * (see ucar.nc2.NetcdfFile documentation).
     * 
     * @param fullName This may be a local file name, an "http:" address of a
     *    .nc file, or an opendap url.
     * @return the string representation of a .nc file header
     * @throws Exception
     */
    public static String readCDL(String fullName) throws Exception {

        //get information
        NetcdfFile netcdfFile = openFile(fullName);
        try {
            String results = netcdfFile.toString();
            return String2.replaceAll(results, "\r", ""); //2013-09-03 netcdf-java 4.3 started using \r\n

        } finally {
            try {
                netcdfFile.close(); //make sure it is explicitly closed
            } catch (Exception e2) {
            }
        }

    }

    /**
     * THIS IS IMPERFECT/UNFINISHED. 
     * This generates a .dds-like list of variables in a .nc file.
     * 
     */
    public static String dds(String fileName) throws Exception {
        String sar[] = String2.splitNoTrim(readCDL(fileName), '\n');
        int n = sar.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            String trimS = sar[i].trim();
            if (trimS.length() > 0 && !trimS.startsWith(":"))
                sb.append(sar[i] + "\n");
            sar[i] = null;
        }
        return sb.toString();
    }

    

    /**
     * This opens a local file name (or an "http:" address, but that is discouraged) of a .nc file.
     * ALWAYS explicitly call netcdfFile.close() when you are finished with it,
     * preferably in a "finally" clause.
     *
     * <p>This does not work for opendap urls. Use the opendap methods instead.
     *
     * <p>If the fullName is an http address, the name needs to start with "http://" 
     * (upper or lower case) and the server needs to support "byte ranges"
     * (see ucar.nc2.NetcdfFile documentation).
     * 
     * @param fullName This may be a local file name, an "http:" address of a
     *    .nc file (discouraged), or an opendap url.  
     *    If this is an .ncml file, the name must end in .ncml.
     * @return a NetcdfFile
     * @throws Exception if trouble
     */
    public static NetcdfFile openFile(String fullName) throws Exception {
        return fullName.endsWith(".ncml")? NetcdfDataset.openDataset(fullName) : 
            NetcdfFile.open(fullName);
    }

    /** 
     * This converts a List of variables to a Variable[].
     *
     * @param list   
     * @return a Variable[]  (or null if list is null)
     */
    public static Variable[] variableListToArray(List list) {
        if (list == null)
            return null;
        int nVars = list.size();
        Variable vars[] = new Variable[nVars];
        for (int v = 0; v < nVars; v++) 
            vars[v] = (Variable)list.get(v);
        return vars;
    }

    /** 
     * This converts a List of dimensions to a Dimension[].
     *
     * @param list   
     * @return a Dimensione[]  (or null if list is null)
     */
    public static Dimension[] dimensionListToArray(List list) {
        if (list == null)
            return null;
        int nDims = list.size();
        Dimension dims[] = new Dimension[nDims];
        for (int d = 0; d < nDims; d++) 
            dims[d] = (Dimension)list.get(d);
        return dims;
    }

    /**
     * Get the list of variables from a netcdf file's structure or, if
     * there is no structure, a list of variables using the largest dimension
     * (a pseudo structure).
     *
     * @param netcdfFile
     * @param variableNames if null, this will search for the variables in 
     *   a (pseudo)structure.
     * @return structure's variables
     */
    public static Variable[] findVariables(NetcdfFile netcdfFile, String variableNames[]) {
        //just use the variable names
        if (variableNames != null) {
            ArrayList list = new ArrayList();
            for (int i = 0; i < variableNames.length; i++) {
                Variable variable = netcdfFile.findVariable(variableNames[i]);
                Test.ensureNotNull(variable, 
                    String2.ERROR + " in NcHelper.findVariables: '" + variableNames[i] + 
                    "' not found."); 
                list.add(variable);
            }
            return variableListToArray(list);
        }

        //find a structure among the variables in the rootGroup
        //if no structure found, it falls through the loop
        Group rootGroup = netcdfFile.getRootGroup();
        List rootGroupVariables = rootGroup.getVariables(); 
        //String2.log("rootGroup variables=" + String2.toNewlineString(rootGroupVariables.toArray()));
        for (int v = 0; v < rootGroupVariables.size(); v++) {
            if (rootGroupVariables.get(v) instanceof Structure) {
                if (reallyVerbose) String2.log("    NcHelper.findVariables found a Structure.");
                return variableListToArray(((Structure)rootGroupVariables.get(v)).getVariables());
            }
        }

        //is there an "observationDimension" global attribute?
        //observationDimension is from deprecated "Unidata Observation Dataset v1.0" conventions,
        //but if it exists, use it.
        Dimension mainDimension = null;
        ucar.nc2.Attribute gAtt = netcdfFile.findGlobalAttribute("observationDimension"); //there is also a dods.dap.Attribute
        if (gAtt != null) {
            PrimitiveArray pa = PrimitiveArray.factory(getArray(gAtt.getValues()));
            if (pa.size() > 0) {
                String dimName = pa.getString(0);
                if (reallyVerbose) 
                    String2.log("    NcHelper.findVariables observationDimension: " + 
                        dimName);
                mainDimension = netcdfFile.getRootGroup().findDimension(dimName);
            }
        }

        //look for unlimited dimension
        if (mainDimension == null) {
            List dimensions = netcdfFile.getDimensions(); //next nc version: getRootGroup().getDimensions();  //was netcdfFile.getDimensions()
            if (dimensions.size() == 0)
                Test.error(String2.ERROR + " in NcHelper.findVariables: the file has no dimensions.");
            for (int i = 0; i < dimensions.size(); i++) {
                Dimension tDimension = (Dimension)dimensions.get(i);
                if (tDimension.isUnlimited()) {
                    mainDimension = tDimension;
                    if (reallyVerbose) 
                        String2.log("    NcHelper.findVariables found an unlimited dimension: " + 
                            mainDimension.getFullName());
                    break;
                }
            }
        }

        //look for a time variable (units contain " since ")       
        if (mainDimension == null) {            
            for (int v = 0; v < rootGroupVariables.size(); v++) {
                Variable variable = (Variable)rootGroupVariables.get(v);
                List dimensions = variable.getDimensions();
                PrimitiveArray units = getVariableAttribute(variable, "units");
                if (units != null && units.size() > 0 && 
                    Calendar2.isNumericTimeUnits(units.getString(0)) && dimensions.size() > 0) {
                    mainDimension = (Dimension)dimensions.get(0);
                    if (reallyVerbose) 
                        String2.log("    NcHelper.findVariables found a time variable with dimension: " + 
                            mainDimension.getFullName());
                    break;
                }
            }
        }

        //use the first outer dimension  (look at last veriables first -- more likely to be the main data variables)
        if (mainDimension == null) {            
            for (int v = rootGroupVariables.size() - 1; v >= 0; v--) {
                Variable variable = (Variable)rootGroupVariables.get(v);
                List dimensions = variable.getDimensions();
                if (dimensions.size() > 0) {
                    mainDimension = (Dimension)dimensions.get(0);
                    if (reallyVerbose) {
                        String fName = mainDimension.getFullName();
                        if (!"row".equals(fName)) //may be null
                            String2.log("    NcHelper.findVariables found an outer dimension: " + fName);
                    }
                    break;
                }
            }
            //check for no dimensions
            if (mainDimension == null)
                Test.error(String2.ERROR + " in NcHelper.findVariables: the file doesn't use dimensions.");
        }

        //get a list of all variables which use just mainDimension
        List structureVariables = new ArrayList();
        for (int i = 0; i < rootGroupVariables.size(); i++) {
            //if (reallyVerbose) String2.log("  get all variables which use mainDimension, check " + i);
            Variable tVariable = (Variable)rootGroupVariables.get(i);
            List tDimensions = tVariable.getDimensions();
            int nDimensions = tDimensions.size();
            //if (reallyVerbose) String2.log("i=" + i + " name=" + tVariable.getFullName() + 
            //    " type=" + tVariable.getNc3DataType());
            if ((nDimensions == 1 && tDimensions.get(0).equals(mainDimension)) ||
                (nDimensions == 2 && tDimensions.get(0).equals(mainDimension) && 
                     tVariable.getDataType() == DataType.CHAR)) {
                    structureVariables.add(tVariable);
            }
        }
        return variableListToArray(structureVariables);
    }

    /** This finds the highest dimension variable in the rootGroup,
     * and then finds all similar variables.
     *
     * @param groupName the name of the group (e.g., "" for any group, 
     *    "somegroup/someSubGroup
     *    "[root]" if you want to limit the search to the root group.
     * @return the similar variables 
     * @throws RuntimeException if trouble (e.g., group doesn't exist or no vars with dimensions)
     */
    public static Variable[] findMaxDVariables(NetcdfFile netcdfFile, String groupName) {

        //find dimNames of highest dimension variable
        String dimNames[] = new String[0];
        List<Variable> allVariables;
        if (String2.isSomething(groupName)) {
            Group group = groupName.equals("[root]")? 
                netcdfFile.getRootGroup() : 
                netcdfFile.findGroup(groupName);
            if (group == null)
                throw new RuntimeException(String2.ERROR + 
                    " in NcHelper.findMaxDVariables: group=\"" + groupName + "\" isn't in the NetCDF file.");
            allVariables = group.getVariables();
        } else {
            allVariables = netcdfFile.getVariables();
        }
        List<Variable> loadVariables = null; 
        for (int v = 0; v < allVariables.size(); v++) {
            Variable variable = allVariables.get(v);
            boolean isChar = variable.getDataType() == DataType.CHAR;
            int tnDim = variable.getRank() - (isChar? 1 : 0);
            if (tnDim > dimNames.length) {
                //a new winner
                loadVariables = new ArrayList();
                loadVariables.add(variable);
                dimNames = new String[tnDim];
                for (int d = 0; d < tnDim; d++) {
                    Dimension tDim = variable.getDimension(d);
                    dimNames[d] = tDim == null || tDim.getFullName() == null? 
                        "size=" + variable.getShape(d) : tDim.getFullName();
                }
            } else if (tnDim > 0 && tnDim == dimNames.length) {
                //a similar variable?
                boolean ok = true;
                for (int d = 0; d < tnDim; d++) {
                    Dimension tDim = variable.getDimension(d);
                    String tName = tDim == null || tDim.getFullName() == null?
                        "size=" + variable.getShape(d) : tDim.getFullName();
                    if (!dimNames[d].equals(tName)) {
                        ok = false;
                        break;
                    }
                }
                if (ok) 
                    loadVariables.add(variable);
            }
        }
        if (loadVariables == null)
            throw new RuntimeException(String2.ERROR + 
                " in NcHelper.findMaxDVariables: None of the variables in group=\"" + 
                groupName + "\" have dimensions.");
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
            Variable variable = (Variable)rootGroupVariables.get(v);
            boolean isChar = variable.getDataType() == DataType.CHAR;
            int tnDim = variable.getRank() - (isChar? 1 : 0);
            if (tnDim > 0) 
                loadVariables.add(variable);
        }
        return variableListToArray(loadVariables);
    }

    /**
     * Get the list of 4D numeric (or 5D char) variables from a netcdf file.
     * Currently this does no verification that the dimensions are 
     *    t,z,y,x, but it could in the future.
     *
     * @param netcdfFile
     * @param variableNames if null, this will search for a 4D variable,
     *   then also include all other variables using the same axes.
     * @return the variables  (list length 0 if none found)
     */
    public static Variable[] find4DVariables(NetcdfFile netcdfFile, String variableNames[]) {
        //just use the variable names
        if (variableNames != null) {
            ArrayList list = new ArrayList();
            for (int i = 0; i < variableNames.length; i++) {
                Variable variable = netcdfFile.findVariable(variableNames[i]);
                Test.ensureNotNull(variable, 
                    String2.ERROR + " in NcHelper.find4DVariables: '" + variableNames[i] + 
                    "' not found."); 
                list.add(variable);
            }
            return variableListToArray(list);
        }

        //find a 4D variable among the variables in the rootGroup
        List allVariables = netcdfFile.getVariables(); 
        String foundDimensionNames[] = null;
        ArrayList foundVariables = new ArrayList();
        for (int v = 0; v < allVariables.size(); v++) {
            Variable variable = (Variable)allVariables.get(v);
            List dimensions = variable.getDimensions();

            if ((dimensions.size() == 4 && variable.getDataType() != DataType.CHAR) ||
                (dimensions.size() == 5 && variable.getDataType() == DataType.CHAR)) {

                //first found?    (future: check that axes are t,z,y,x?)
                if (foundDimensionNames == null) {
                    foundDimensionNames = new String[4];
                    for (int i = 0; i < 4; i++)
                       foundDimensionNames[i] = ((Dimension)dimensions.get(i)).getFullName();               
                }

                //does it match foundDimensions
                boolean matches = true;
                for (int i = 0; i < 4; i++) {
                    if (!foundDimensionNames[i].equals(((Dimension)dimensions.get(i)).getFullName())) {
                        matches = false;
                        break;
                    }
                }
                if (matches)
                    foundVariables.add(variable);

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
        Test.ensureNotNull(variable, 
            String2.ERROR + " in NcHelper.findVariable: '" + variableName + 
            "' not found."); 
        return variable;
    }

    /**
     * This adds global (group) attributes to a netcdf file's group.
     *
     * @param group usually the rootGroup
     * @param attributes the Attributes that will be set
     */
    public static void setAttributes(boolean nc3Mode, Group group, Attributes attributes) {
        String names[] = attributes.getNames();
        for (int ni = 0; ni < names.length; ni++) { 
            String tName = names[ni];
            if (!String2.isSomething(tName) || 
                tName.equals("_NCProperties")) //If I write this, netcdf nc4 code later throws Exception when it writes its own version
                continue;
            PrimitiveArray tValue = attributes.get(tName);
            if (tValue == null || tValue.size() == 0 || tValue.toString().length() == 0) 
                continue; //do nothing
            group.addAttribute(createAttribute(nc3Mode, tName, tValue));
        }
    }

    /**
     * This adds attributes to a variable in preparation for writing a .nc file.
     *
     * @param var  e.g., from findVariable(netcdfFile, varName)
     * @param attributes the Attributes that will be set
     */
    public static void setAttributes(boolean nc3Mode, Variable var, Attributes attributes, boolean unsigned) {
        String names[] = attributes.getNames();
        if (nc3Mode && unsigned)
            var.addAttribute(createAttribute(nc3Mode, "_Unsigned", new StringArray(new String[]{"true"})));
        for (int ni = 0; ni < names.length; ni++) { 
            String tName = names[ni];
            if (!String2.isSomething(tName)) 
                continue;
            PrimitiveArray tValue = attributes.get(tName);
            if (tValue == null || tValue.size() == 0 || 
                (tValue.elementType() == PAType.STRING && tValue.toString().length() == 0)) 
                continue; //do nothing
            var.addAttribute(createAttribute(nc3Mode, tName, tValue));
        }
    }



    /**
     * Given an attribute, this adds the value to the attributes.
     * If there is trouble, this lots a warning message and returns null.
     * This is low level and isn't usually called directly.
     *
     * @param varName the variable's name (or "global"), used for diagnostic messages only
     * @param att   
     * @return a PrimitiveArray or null if trouble
     */
    public static PrimitiveArray getAttributePA(String varName, ucar.nc2.Attribute att) {
        if (att == null) {
            //if (debugMode)
            //    String2.log("Warning: NcHelper.getAttributePA varName=" + varName + " att=null"); // + MustBe.stackTrace());
            return null;
        } else if (String2.isSomething(att.getFullName())) {
            try {
                //decodeAttribute added with switch to netcdf-java 4.0
                Array values = att.getValues();
                if (values == null) {
                    String2.log("Warning: varName=" + varName + " attribute=" + att.getFullName() + " has values=null");
                    return null;
                }
                return PrimitiveArray.factory(decodeAttribute(getArray(values))); 
            } catch (Throwable t) {
                String2.log("Warning: NcHelper caught an exception while reading varName='" + 
                    varName + "' attribute=" + att.getFullName() + "\n" +
                    MustBe.throwableToString(t));
                return null;
            }
        } else {
            if (reallyVerbose)
                String2.log("Warning: NcHelper.getAttributePA varName=" + varName + 
                    " att.getFullName()=" + String2.annotatedString(att.getFullName()));
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
    public static PrimitiveArray getVariableAttribute(Variable variable, 
        String attributeName) {
        return getAttributePA(variable.getFullName(), 
            variable.findAttribute(attributeName)); 
    }

    /**
     * This gets one global attribute from a netcdf variable.
     *
     * @param netcdfFile
     * @param attributeName
     * @return the attribute (or null if none)
     */
    public static PrimitiveArray getGlobalAttribute(NetcdfFile netcdfFile, 
        String attributeName) {
        return getAttributePA("global", netcdfFile.findGlobalAttribute(attributeName)); 
    }

    /**
     * This gets the first element of one global attribute as a String.
     *
     * @param netcdfFile
     * @param attributeName
     * @return the attribute (or null if none)
     */
    public static String getStringGlobalAttribute(NetcdfFile netcdfFile, 
        String attributeName) {
        PrimitiveArray pa = getGlobalAttribute(netcdfFile, attributeName);
        if (pa == null || pa.size() == 0)
            return null;
        return pa.getString(0);
    }

    /**
     * Given an attribute, this adds the value to the attributes.
     * If there is trouble, this logs a warning message and returns.
     * This is low level and isn't usually called directly.
     *
     * @param varName use "Global" for global attributes. This is just used for diagnostic messages.
     * @param att an nc attribute. If the name is already present, this new att
     *   takes precedence.
     * @param attributes the Attributes that will be added to. 
     */
    public static void addAttribute(String varName, ucar.nc2.Attribute att, 
        Attributes attributes) {
        addAttribute(varName, att, attributes, true);
    }

    /**
     * Given an attribute, this adds the value to the attributes.
     * If there is trouble, this logs a warning message and returns.
     * This is low level and isn't usually called directly.
     *
     * @param varName use "Global" for global attributes. This is just used for diagnostic messages.
     * @param att an nc attribute
     * @param attributes the Attributes that will be added to. 
     * @param addIfAlreadyPresent 
     */
    public static void addAttribute(String varName, ucar.nc2.Attribute att, 
        Attributes attributes, boolean addIfAlreadyPresent) {
        if (att == null) {
            if (reallyVerbose)
                String2.log("Warning: NcHelper.addAttribute " + varName + " att=null");
            return;
        }
        String tName = att.getFullName(); //It is the short name! It doesn't include the groupName.
        if (!String2.isSomething(tName)) {
            if (reallyVerbose)
                String2.log("Warning: NcHelper.addAttribute " + varName + 
                    " att.getFullName()=" + String2.annotatedString(tName));
        } else if (!addIfAlreadyPresent && attributes.get(tName) != null) {
            //do nothing
        } else {
            //attributes.set calls String2.canonical (useful since many names are in many datasets)
            attributes.set(tName, getAttributePA(varName, att));
        }
    }

    /**
     * This reads the global attributes from a netcdf file.
     *
     * @param netcdfFile
     * @param attributes the Attributes that will be added to.
     */
    public static void getGlobalAttributes(NetcdfFile netcdfFile, Attributes attributes) {
        getGlobalAttributes(netcdfFile, "", attributes);
    }

    /**
     * This reads the global attributes for the group and all its parent groups in a netcdf file.
     *
     * @param netcdfFile
     * @param groupName  a specific groupName, or "" or null for the root group
     * @param attributes the Attributes that will be added to.
     *     If group was specified but found, this just returns the root group atts.
     *     If group was specified, the group's (and parent groups) global attributes 
     *     are not marked with the name of the group -- it's as if all the global atts
     *     from all levels were thrown in together (with preference for lower level).
     */
    public static void getGlobalAttributes(NetcdfFile netcdfFile, String groupName, Attributes attributes) {
        Group group = String2.isSomething(groupName)?
            netcdfFile.findGroup(groupName) :
            netcdfFile.getRootGroup();
        if (group == null)
            group = netcdfFile.getRootGroup();
        while (true) {
            getGlobalAttributes(group.getAttributes(), attributes);
            group = group.getParentGroup(); //returns null if this is root group
            if (group == null) 
                return;
        }
    }

    /**
     * This reads the global attributes from a netcdf file.
     *
     * @param globalAttList
     * @param attributes the Attributes that will be added to.
     */
    public static void getGlobalAttributes(List globalAttList, Attributes attributes) {

        //read the globalAttributes
        if (globalAttList == null)
            return;
        int nAtt = globalAttList.size();
        for (int att = 0; att < nAtt; att++)  //there is also a dods.dap.Attribute                        
            addAttribute("global", (ucar.nc2.Attribute)globalAttList.get(att), attributes, false);
    }

    /**
     * This adds to the attributes for a netcdf variable.
     *
     * @param variable
     * @param attributes the Attributes that will be added to.
     */
    public static void getVariableAttributes(Variable variable, Attributes attributes) {
        if (variable == null)
            return;
        String variableName = variable.getFullName();

        //add the attributes
        List variableAttList = variable.getAttributes();
        if (variableAttList == null) 
            return;
        for (int att = 0; att < variableAttList.size(); att++) 
            addAttribute(variableName, (ucar.nc2.Attribute)variableAttList.get(att), attributes);

        //in nc3 files, if variables has _Unsigned=true, convert some signed attributes to unsigned
        //String2.log(">> getVariableAttributes var=" + variable.getFullName() + " _Unsigned=" + attributes.getString("_Unsigned"));
        PrimitiveArray us = attributes.remove("_Unsigned");
        if (us != null && "true".equals(us.toString()))
            attributes.convertSomeSignedToUnsigned();
    }


    /** 
     * Given a ncFile and the name of the pseudo-data Variable with the projection information,
     * this tries to get the attributes and then gatherGridMappingAtts.
     * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#grid-mappings-and-projections
     * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#rotated-pole-grid-ex
     *
     * @param gridMappingVarName  eg from attributes.getString("grid_mapping")  
     * @return gridMappingAttributes in a form suitable for addition to the 
     *   global addAttributes (or null if trouble, e.g., varName is null).
     */
    public static Attributes getGridMappingAtts(NetcdfFile ncFile, String gridMappingVarName) {
        if (gridMappingVarName == null)
            return null;
        return getGridMappingAtts(ncFile.findVariable(gridMappingVarName));
    }

    /** 
     * Given what might be the netcdf pseudo-data Variable with the projection information,
     * this tries to get the attributes and then gatherGridMappingAtts.
     * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#grid-mappings-and-projections
     * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#rotated-pole-grid-ex
     *
     * @param pseudoDataVariable  
     * @return gridMappingAttributes in a form suitable for addition to the 
     *   global addAttributes (or null if trouble, e.g., variable is null).
     */
    public static Attributes getGridMappingAtts(Variable pseudoDataVariable) {
        if (pseudoDataVariable == null)
            return null;
        Attributes sourceAtts = new Attributes();
        getVariableAttributes(pseudoDataVariable, sourceAtts);
        return getGridMappingAtts(sourceAtts);
    }

    /** 
     * Call this with the sourceAtts that might be from the pseudo-variable with  
     * a grid_mapping_name attribute in order to collect the grid_mapping attributes. 
     * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#grid-mappings-and-projections
     * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#rotated-pole-grid-ex
     *
     * @param sourceAtts 
     * @return gridMappingAttributes in a form suitable for addition to the 
     *   global addAttributes (or null if trouble, e.g., sourceAtts is null)
     */
    public static Attributes getGridMappingAtts(Attributes sourceAtts) {
        if (sourceAtts == null || 
            sourceAtts.getString("grid_mapping_name") == null) //it's the one required att 
            return null;
        Attributes gridMappingAtts = new Attributes();
        String[] attNames = sourceAtts.getNames();
        if (attNames.length == 0)
            return null;
        for (int an = 0; an < attNames.length; an++) {
            String attName = attNames[an];
            boolean keep = true;
            if ("comment".equals(attName) &&
                sourceAtts.getString(attName).startsWith("This is a container variable"))
                keep = false;
            if (!"DODS_strlen".equals(attName) && keep)
                gridMappingAtts.add(                            
                    "grid_mapping_name".equals(attName)? 
                        attNames[an] :
                        "grid_mapping_" + attName,
                    sourceAtts.get(attName)); //some PA type
        }
        return gridMappingAtts;
    }


    /**
     * If the var has time units, or scale_factor and add_offset, the values in the dataPa
     * will be unpacked (numeric times will be epochSeconds).
     * If missing_value and/or _FillValue are used, they are converted to PA standard mv.
     *
     * @param var this method gets the info it needs
     *   (_Unsigned, scale_factor, add_offset, units, _FillValue, missingvalue)
     *    from the var.
     * @param dataPa A data pa or an attribute value pa. It isn't an error if dataPa = null.
     * @param lookForUnsigned should be true for the main PA, but false when converting attribute PA's.
     * @return pa The same pa or a new pa or null (if the pa parameter = null).
     *   missing_value and _FillValue will be converted to PA standard mv 
     *     for the the unpacked datatype (or current type if not packed).
     */
    public static PrimitiveArray unpackPA(Variable var, PrimitiveArray dataPa,
        boolean lookForStringTimes, boolean lookForUnsigned) {

        if (dataPa == null) 
            return dataPa;
        Attributes atts = new Attributes();
        getVariableAttributes(var, atts);
        return atts.unpackPA(var.getFullName(), dataPa, lookForStringTimes, lookForUnsigned);
    }


    /**
     * This returns the double value of an attribute.
     * If the attribute DataType is DataType.FLOAT, this nicely
     * converts the float to a double.
     *
     * @param attribute
     * @return the double value of an attribute.
     */
    public static double getNiceDouble(Attribute attribute) {
        double d = attribute.getNumericValue().doubleValue();
        if (attribute.getDataType() == DataType.FLOAT)
            return Math2.floatToDouble(d);
        return d;
    }

    /**
     * Given an ascending sorted .nc Variable,
     * this finds the index of an instance of the value 
     * (not necessarily the first or last instance)
     * (or -index-1 where it should be inserted).
     *
     * @param variable an ascending sorted nc variable.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually (size - 1)
     * @param value the value you are searching for
     * @return the index of an instance of the value 
     *     (not necessarily the first or last instance)
     *     (or -index-1 where it should be inserted, with extremes of
     *     -lowPo-1 and -(highPo+1)-1).
     * @throws Exception if trouble
     */
    public static int binarySearch(Variable variable, 
        int lowPo, int highPo, double value) throws Exception {
        //String2.log("binSearch lowPo=" + lowPo + " highPo=" + highPo + " value=" + value);
        
        //ensure lowPo <= highPo
        //lowPo == highPo is handled by the following two chunks of code
        Test.ensureTrue(lowPo <= highPo,  
            String2.ERROR + "in NcHelper.binarySearch: lowPo > highPo.");
        
        double tValue = getDouble(variable, lowPo);
        if (tValue == value)
            return lowPo;
        if (tValue > value)
            return -lowPo - 1;

        tValue = getDouble(variable, highPo);
        if (tValue == value)
            return highPo;
        if (tValue < value)
            return -(highPo+1) - 1;

        //repeatedly look at midpoint
        //If no match, this always ends with highPo - lowPo = 1
        //  and desired value would be in between them.
        while (highPo - lowPo > 1) {
            int midPo = (highPo + lowPo) / 2;
            tValue = getDouble(variable, midPo);
            //String2.log("binSearch midPo=" + midPo + " tValue=" + tValue);
            if (tValue == value)
                return midPo;
            if (tValue < value) 
                lowPo = midPo;
            else highPo = midPo;
        }

        //no exact match
        return -highPo - 1;
    }

    /**
     * Given an ascending sorted .nc variable and a row,
     * this finds the first row that has that same value (which may be different if there are ties).
     *
     * @param variable an ascending sorted nc Variable.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param row the starting row
     * @return the first row that has the same value
     */
    public static int findFirst(Variable variable, int row) throws Exception {
        //would be much more efficient if read several values at once
        double value = getDouble(variable, row);
        while (row > 0 && getDouble(variable, row - 1) == value)
            row--;
        return row;
    }

    /**
     * Given an ascending sorted .nc variable and a row,
     * this finds the last row that has that same value (which may be different if there are ties).
     *
     * @param variable an ascending sorted nc Variable.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param row the starting row
     * @return the last row that has the same value
     */
    public static int findLast(Variable variable, int row) throws Exception {
        //would be much more efficient if read several values at once
        double value = getDouble(variable, row);
        int size1 = variable.getDimension(0).getLength() - 1;
        while (row < size1 && getDouble(variable, row + 1) == value)
            row++;
        return row;
    }


    /**
     * Given an ascending sorted .nc Variable,
     * this finds the index of the first element >= value. 
     *
     * @param variable an ascending sorted nc Variable.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually (size - 1)
     * @param value the value you are searching for
     * @return the index of the first element >= value 
     *     (or highPo + 1, if there are none)
     * @throws Exception if trouble
     */
    public static int binaryFindFirstGE(Variable variable, int lowPo, int highPo, double value) throws Exception {
        if (lowPo > highPo)
            return highPo + 1;
        int po = binarySearch(variable, lowPo, highPo, value);

        //an exact match? find the first exact match
        if (po >= 0) {
            while (po > lowPo && getDouble(variable, po - 1) == value)
                po--;
            return po;
        }

        //no exact match? return the binary search po
        //thus returning a positive number
        //the inverse of -x-1 is -x-1 !
        return -po -1;
    }

    /**
     * Given an ascending sorted .nc Variable,
     * this finds the index of the last element <= value. 
     *
     * @param variable an ascending sorted nc Variable.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually 
     *  (originalPrimitiveArray.size() - 1)
     * @param value the value you are searching for
     * @return the index of the first element <= value 
     *     (or -1, if there are none)
     * @throws Exception if trouble
     */
    public static int binaryFindLastLE(Variable variable, int lowPo, int highPo, double value) throws Exception {

        if (lowPo > highPo)
            return -1;

        int po = binarySearch(variable, lowPo, highPo, value);

        //an exact match? find the last exact match
        if (po >= 0) {
            while (po < highPo && getDouble(variable, po + 1) == value)
                po++;
            return po;
        }

        //no exact match? return binary search po -1
        //thus returning a positive number
        //the inverse of -x-1 is -x-1 !
        return -po -1 -1;
    }


    /**
     * Given an ascending sorted .nc Variable,
     * this finds the index of the element closest to value. 
     *
     * @param variable an ascending sorted nc Variable.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param value the value you are searching for
     * @return the index of the first element found which is closest to value 
     *    (there may be other identical values)
     * @throws Exception if trouble
     */
    public static int binaryFindClosest(Variable variable, double value) throws Exception {
        return binaryFindClosest(variable, 0, variable.getDimension(0).getLength() - 1, value);
    }

    /**
     * Given an ascending sorted .nc Variable,
     * this finds the index of the element closest to value. 
     *
     * @param variable an ascending sorted nc Variable.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with (e.g., length-1)
     * @param value the value you are searching for
     * @return the index of the first element found which is closest to value 
     *    (there may be other identical values)
     * @throws Exception if trouble
     */
    public static int binaryFindClosest(Variable variable, int lowPo, int highPo, double value) throws Exception {

        int po = binarySearch(variable, lowPo, highPo, value);
        //String2.log("po1=" + po);
        //an exact match? find the first exact match
        if (po >= 0) {
//would be much more efficient if read several values at once
            while (po > lowPo && getDouble(variable, po - 1) == value)
                po--;
            return po;
        }

        //insertionPoint at end point?
        int insertionPoint = -po - 1;  //0.. highPo
        if (insertionPoint == 0) 
            return 0;
        if (insertionPoint >= highPo)
            return highPo;

        //insertionPoint between 2 points 
        if (Math.abs(getDouble(variable, insertionPoint - 1) - value) <
            Math.abs(getDouble(variable, insertionPoint    ) - value))
             return insertionPoint - 1;
        else return insertionPoint;
    }

    /**
     * This is like getPrimitiveArray, but converts it to DoubleArray.
     * If it was FloatArray, this rounds the values nicely to doubles.
     *
     * @param variable the variable to be read from.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param firstRow  the value of the leftmost dimension
     * @param lastRow the last row to be read (inclusive).  
     *    If lastRow = -1, firstRow is ignored and the entire var is read.
     * @return the values in a DoubleArray
     * @throws Exception if trouble
     */
    public static DoubleArray getNiceDoubleArray(Variable variable, int firstRow, int lastRow) 
            throws Exception {

        PrimitiveArray pa = lastRow == -1?
            getPrimitiveArray(variable) :
            getPrimitiveArray(variable, firstRow, lastRow);
        if (pa instanceof DoubleArray) {
            return (DoubleArray)pa;
        }

        DoubleArray da = new DoubleArray(pa);
        if (pa instanceof FloatArray) {
            int n = da.size();
            for (int i = 0; i < n; i++)
                da.array[i] = Math2.floatToDouble(da.array[i]);
        }
        return da;
    }

    /** 
     * This reads the number at 'row' from a numeric variable
     * and returns a double.
     *
     * @param variable the variable to be read from.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param row  the value of the leftmost dimension
     * @return the value at 'row'
     */
    public static double getDouble(Variable variable, int row) throws Exception {
        int nDim = variable.getRank();
        int origin[] = new int[nDim]; //all 0's
        int shape[] = new int[nDim];
        origin[0] = row;
        Arrays.fill(shape, 1);        

        Array array = variable.read(origin, shape);
        return toDoubleArray(array)[0];
    }

    /** 
     * This reads the number at 'row' from a numeric variable
     * and returns a nice double (floats are converted with Math2.floatToDouble).
     *
     * @param variable the variable to be read from.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     * @param row  the value of the leftmost dimension
     * @return the value at 'row'
     */
    public static double getNiceDouble(Variable variable, int row) throws Exception {
        double d= getDouble(variable, row);
        if (variable.getDataType() == DataType.FLOAT) 
            return Math2.floatToDouble(d);
        return d;
    }


    /**
     * This reads a range of values from an nDimensional variable in a NetcdfFile.
     * Use getPrimitiveArray(variable) if you need all of the data.
     *
     * @param variable the variable to be read from.
     *   The variable can have nDimensions, but the size
     *   of the non-leftmost dimensions must be 1, e.g., [1047][1][1][1]
     *   (or non-leftmost and non-rightmost (nChar/String) dimensions for char/String variables, e.g., [1047][1][1][1][20]).
     * @param firstRow the first row to be read
     * @param lastRow the last row to be read. (inclusive). 
     *    If lastRow=-1, firstRow is ignored and the entire variable is returned.
     * @return the values in a PrimitiveArray
     * @throws Exception if trouble
     */
    public static PrimitiveArray getPrimitiveArray(Variable variable, 
        int firstRow, int lastRow) throws Exception {

        if (lastRow == -1)
            return getPrimitiveArray(variable);

        boolean isChar = variable.getDataType() == DataType.CHAR;
        int nDim = variable.getRank();
        int oShape[] = variable.getShape();
        //???verify that shape is valid?
        int origin[] = new int[nDim]; //all 0's
        int shape[] = new int[nDim];
        origin[0] = firstRow;
        Arrays.fill(shape, 1);        
        int nRows = lastRow - firstRow + 1;
        shape[0] = nRows;
        if (isChar) 
            shape[nDim - 1] = oShape[nDim - 1]; //nChars / String
        PrimitiveArray pa = getPrimitiveArray(variable.read(origin, shape), true, isUnsigned(variable));

        //eek! opendap returns a full-sized array! 
        //     netcdf  returns a shape-sized array
        if (pa.size() < nRows) 
            Test.error(String2.ERROR + " in NcHelper.getPrimitiveArray(firstRow=" + 
                firstRow + " lastRow=" + lastRow + ")\n" +
                "variable.read returned too few (" + pa.size() + ").");
        if (pa.size() > nRows) {
            //it full-sized; reduce to correct size
            if (reallyVerbose) String2.log(
                "    NcHelper.getPrimitiveArray variable.read returned entire variable!"); 
            pa.removeRange(lastRow + 1, pa.size()); //remove tail first (so don't have to move it when remove head
            pa.removeRange(0, firstRow - 1); //remove head section
        }
        return pa;
    }

    /**
     * This reads a 1D range of values from a 4D variable
     * (or 5D if it holds strings, so 5th dimension DataType is CHAR) in a NetcdfFile.
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
    public static PrimitiveArray get4DValues(Variable variable, int xIndex, 
            int yIndex, int zIndex, int firstT, int lastT) throws Exception {

        int rowOrigin[] = null; 
        int rowShape[]  = null; 
        if (variable.getRank() == 5 && variable.getDataType() == DataType.CHAR) {
            rowOrigin = new int[]{firstT, zIndex, yIndex, xIndex, 0};
            rowShape  = new int[]{lastT - firstT + 1, 1, 1, 1, variable.getDimension(4).getLength()};
        } else { //numeric
            rowOrigin = new int[]{firstT, zIndex, yIndex, xIndex};
            rowShape  = new int[]{lastT - firstT + 1, 1, 1, 1};
        }
        Array array = variable.read(rowOrigin, rowShape); 
        PrimitiveArray pa = PrimitiveArray.factory(getArray(array)); 
        Test.ensureEqual(pa.size(), lastT - firstT + 1, "NcHelper.getValues nFound!=nExpected.\n" +
            " name=" + variable.getFullName() +
            " xIndex=" + xIndex + 
            " yIndex=" + yIndex + 
            " zIndex=" + zIndex + 
            " firstT=" + firstT + 
            " lastT=" + lastT); 
        return pa;
    }

    /**
     * This reads a 4D range of values from a 4D variable
     * (or 5D if it holds strings, so 5th dimension DataType is CHAR) in a NetcdfFile.
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
     * @return the values in a PrimitiveArray (read from primitive array
     *     with loops: for t=... for z=... for y=... for x=...  pa.getDouble(po++))
     * @throws Exception if trouble
     */
    public static PrimitiveArray get4DValues(Variable variable, 
        int firstX, int nX, int firstY, int nY, 
        int firstZ, int nZ, int firstT, int nT) throws Exception {

        int rowOrigin[] = null; 
        int rowShape[]  = null; 
        if (variable.getRank() == 5 && variable.getDataType() == DataType.CHAR) {
            rowOrigin = new int[]{firstT, firstZ, firstY, firstX, 0};
            rowShape  = new int[]{nT, nZ, nY, nX, variable.getDimension(4).getLength()};
        } else { //numeric
            rowOrigin = new int[]{firstT, firstZ, firstY, firstX};
            rowShape  = new int[]{nT, nZ, nY, nX};
        }
        Array array = variable.read(rowOrigin, rowShape); 
        PrimitiveArray pa = PrimitiveArray.factory(getArray(array)); 
        Test.ensureEqual(pa.size(), nX*nY*nZ*nT, "NcHelper.get4DValues nFound!=nExpected.\n" +
            " name=" + variable.getFullName() +
            " firstX=" + firstX + " nX=" + nX +
            " firstY=" + firstY + " nT=" + nY +
            " firstZ=" + firstZ + " nZ=" + nZ +
            " firstT=" + firstT + " nT=" + nT); 
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
                netcdfFile.close(); //make sure it is explicitly closed
            } catch (Exception e2) {
            }
        }

    }

    /**
     * This writes values to a 1D netcdf variable in a NetcdfFileWriter.
     * This works with all PrimitiveArray types, but in nc3mode:
     * <br>LongArray and ULongArray is stored as doubles, so retrieve with 
     * <br>pa = new LongArray(pa) or new ULongArray(pa), and
     * <br>CharArray is stored as chars (ISO-8859-1).
     *
     * @param netcdfFileWriter
     * @param variableName
     * @param firstRow  This is the origin/where to write this chunk of data
     *     within the complete var in the nc file.
     * @param pa will be converted to the appropriate numeric type
     * @throws Exception if trouble
     */
    public static void write(boolean nc3Mode, NetcdfFileWriter netcdfFileWriter, 
        Variable var, int firstRow, PrimitiveArray pa) throws Exception {

        write(nc3Mode, netcdfFileWriter, 
            var, new int[]{firstRow}, new int[]{pa.size()}, pa);
    }

    /** 
     * This is a thin wrapper to call netcdfFileWriter.writeStringData (for StringArray)
     * or netcdfFileWriter.write to write the pa to the file.
     *
     * <p>This will convert CharArray to ShortArray and LongArray to StringArray, 
     *    but it usually saves memory if caller does it.
     *
     * @param netcdfFileWriter
     * @param var
     * @param origin  The start of where the data will be written in the var.
     *    Don't include the StringLength dimension.
     * @param shape   The shape that the data will be arranged into in the var.
     *    Don't include StringLength dimension.
     * @param pa   the data to be written 
     */
    public static void write(boolean nc3Mode, NetcdfFileWriter netcdfFileWriter, 
        Variable var, int origin[], int shape[], PrimitiveArray pa) throws Exception {

        PAType paType = pa.elementType();
        if (nc3Mode) {
            if (paType == PAType.LONG ||
                paType == PAType.ULONG)
                pa = new DoubleArray(pa);
            else if (paType == PAType.CHAR)
                pa = (new CharArray(pa)).toIso88591(); //netcdf-java just writes low byte
            else if (paType == PAType.STRING)
                pa = (new StringArray(pa)).toIso88591(); //netcdf-java just writes low byte
        }

        if (paType == PAType.STRING) {
            netcdfFileWriter.writeStringData(var, origin, 
                Array.factory(DataType.STRING, shape, pa.toObjectArray()));         
        } else {
            netcdfFileWriter.write(var, origin, 
                Array.factory(getNc3DataType(paType),
                    shape, pa.toObjectArray()));         
        }
    }



    /** 
     * This opens an opendap dataset as a NetcdfDataset.
     * ALWAYS explicitly use netcdfDataset.close() when you are finished with it,
     * preferably in a "finally" clause.
     *
     * @param url  This should not include a list of variables 
     *   (e.g., ?varName,varName) or a constraint expression at the end.
     * @return a NetcdfDataset which is a superclass of NetcdfFile
     * @throws Exception if trouble
     */
    public static NetcdfDataset openOpendapAsNetcdfDataset(String url) throws Exception {
        return NetcdfDataset.openDataset(url);
    }


    /**
     * This determines which rows of an .nc file have testVariables 
     * between min and max.
     * This is faster if the first testVariable(s) eliminate a lot of rows.
     *
     * @param testVariables the variables to be tested.
     *     They must all be ArrayXxx.D1 or ArrayChar.D2 variables and use the 
     *     same, one, dimension as the first dimension.
     *     This must not be null or empty.
     *     If you have variable names, use ncFile.findVariable(name);
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
                okRows.set(0, nRows); //make all 'true' initially
            }

            //read the data
            cumReadTime -= System.currentTimeMillis();
            PrimitiveArray pa = PrimitiveArray.factory(getArray(variable.read())); 
            cumReadTime += System.currentTimeMillis();

            //test the data
            double tMin = min[col];
            double tMax = max[col];
            int row = okRows.nextSetBit(0);
            int lastSetBit = -1;
            while (row >= 0) {
                double d = pa.getDouble(row);
                if (d < tMin || d > tMax || Double.isNaN(d))
                    okRows.clear(row);
                else lastSetBit = row;
                row = okRows.nextSetBit(row + 1);
            }
            if (lastSetBit == -1)
                return okRows;
        }
        String2.log("testNcRows totalTime=" + (System.currentTimeMillis() - time) +
            " cumReadTime=" + cumReadTime);
        return okRows;
    }

    /** 
     * This writes the PrimitiveArrays into an .nc file.
     * This works with all PrimitiveArray types, but some datatypes (chars, longs, unsigned types)
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName for the file (This writes to an intermediate file then renames quickly.)
     * @param varNames    Names mustn't have internal spaces.
     * @param pas   Each may have a different size! 
     *     (Which makes this method different than Table.saveAsFlatNc.)
     *     In the .nc file, each will have a dimension with the same name as the varName.
     *     All must have at least 1 value.
     * @throws Exception if trouble 
     */
    public static void writePAsInNc3(String fullName, StringArray varNames, 
        PrimitiveArray pas[]) throws Exception {
        String msg = "  NcHelper.savePAsInNc " + fullName; 
        long time = System.currentTimeMillis();

        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);
        File2.delete(fullName);

        //tpas is same as pas, but with CharArray, LongArray and ULongArray converted to StringArray
        int nVars = varNames.size();
        PrimitiveArray tpas[] = new PrimitiveArray[nVars]; 

        //open the file (before 'try'); if it fails, no temp file to delete
        NetcdfFileWriter nc = NetcdfFileWriter.createNew(
            NetcdfFileWriter.Version.netcdf3, fullName + randomInt);
        try {
            Group rootGroup = nc.addGroup(null, "");
            nc.setFill(false);

            //add the variables
            Variable newVars[] = new Variable[nVars];
            for (int var = 0; var < nVars; var++) {
                String name = varNames.get(var);
                tpas[var] = pas[var];
                if (tpas[var].elementType() == PAType.CHAR) {
                    //nc 'char' is 1 byte!  So store java char (2 bytes) as shorts.
                    tpas[var] = new ShortArray(((CharArray)pas[var]).toArray());
                } else if (tpas[var].elementType() == PAType.LONG ||
                           tpas[var].elementType() == PAType.ULONG) {
                    //these will always be decoded by fromJson as-is; no need to encode with toJson
                    tpas[var] = new StringArray(pas[var]);
                } else if (tpas[var].elementType() == PAType.STRING) {
                    //.nc strings only support characters 1..255, so encode as Json strings
                    StringArray oldSa = (StringArray)pas[var];
                    int tSize = oldSa.size();
                    StringArray newSa = new StringArray(tSize, false);
                    tpas[var] = newSa;
                    for (int i = 0; i < tSize; i++) {
                        //Only save json version if oldS part of it (inside enclosing "'s) is different.
                        //This minimizes new Strings, memory usage, garbage collection, etc.
                        //Note that oldS starting and ending with " will be changed when encoded
                        //  and so newS will be used.
                        String oldS = oldSa.get(i);
                        String newS = String2.toJson(oldS);
                        newSa.add(newS.substring(1, newS.length() - 1).equals(oldS)?
                            oldS : newS);
                    }
                }

                PAType type = tpas[var].elementType();
                Dimension dimension  = nc.addDimension(rootGroup, name, tpas[var].size());
                if (type == PAType.STRING) {
                    int max = Math.max(1, ((StringArray)tpas[var]).maxStringLength()); //nc libs want at least 1; 0 happens if no data
                    Dimension lengthDimension  = nc.addDimension(rootGroup, 
                        name + StringLengthSuffix, max);
                    newVars[var] = nc.addVariable(rootGroup, name, DataType.CHAR, 
                        Arrays.asList(dimension, lengthDimension)); 
                    if (pas[var].elementType() == PAType.LONG) 
                        newVars[var].addAttribute(new Attribute("NcHelper", originally_a_LongArray)); 
                    else if (pas[var].elementType() == PAType.ULONG) 
                        newVars[var].addAttribute(new Attribute("NcHelper", originally_a_ULongArray)); 
                    else if (pas[var].elementType() == PAType.STRING) 
                        newVars[var].addAttribute(new Attribute("NcHelper", "JSON encoded")); 
                } else {
                    newVars[var] = nc.addVariable(rootGroup, name, getNc3DataType(type),
                        Arrays.asList(dimension)); 
                    if (pas[var].elementType() == PAType.CHAR) 
                        newVars[var].addAttribute(new Attribute("NcHelper", originally_a_CharArray)); 
                }
            }

            //leave "define" mode
            nc.create();

            //write the data
            for (int var = 0; var < nVars; var++) {
                nc.write(newVars[var], get1DArray(tpas[var]));
            }

            //if close throws exception, it is trouble
            nc.close(); //it calls flush() and doesn't like flush called separately
            nc = null;

            File2.rename(fullName + randomInt, fullName);

            //diagnostic
            if (reallyVerbose) msg += " finished. TIME=" + 
                (System.currentTimeMillis() - time) + "ms";
            //String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

        } catch (Exception e) {
            //try to close the file
            try {
                if (nc != null) nc.abort(); 
            } catch (Exception e2) {
                //don't care
            }

            //delete any partial or pre-existing file
            File2.delete(fullName + randomInt);
           
            if (!reallyVerbose) String2.log(msg);

            throw e;
        } finally {
            if (reallyVerbose) String2.log(msg);
        }

    }

    /**
     * This reads the PAs in the .nc file.
     * This works with all PrimitiveArray types, but some datatypes 
     * (char, long, unsigned, String)
     * are specially encoded in the files and then automatically decoded when read,
     * so that this fully supports 2byte chars, longs, and Unicode Strings
     * (by encoding as json in file).
     *
     * @param fullName the name of the .nc file.
     * @param loadVarNames the names of the variables to load (null to get all)
     * @param varNames this will receive the varNames
     * @return PrimitiveArray[] paralleling the names in varNames
     * @throws Exception if trouble (e.g., one of the loadVarNames wasn't found)
     */
    public static PrimitiveArray[] readPAsInNc3(String fullName, String loadVarNames[],
        StringArray varNames) throws Exception {

        String msg = "  NcHelper.readPAsInNc3 " + fullName
            //+ " \n  loadVarNames=" + String2.toCSSVString(loadVarNames)
            ; 
        varNames.clear();
        ArrayList pas = new ArrayList();
        long time = System.currentTimeMillis();
        NetcdfFile netcdfFile = openFile(fullName);
        try {
            List rootGroupVariables = null;
            if (loadVarNames == null) {
                //find variables in the rootGroup
                Group rootGroup = netcdfFile.getRootGroup();
                rootGroupVariables = rootGroup.getVariables(); 
            }
            int n = loadVarNames == null? rootGroupVariables.size() : loadVarNames.length;

            for (int v = 0; v < n; v++) {
                Variable tVariable = loadVarNames == null? 
                    (Variable)rootGroupVariables.get(v) :
                    netcdfFile.findVariable(loadVarNames[v]);
                if (tVariable == null)
                    throw new RuntimeException(
                        String2.ERROR + ": Expected variable #" + v + " not found while reading " + fullName + 
                        " (loadVarNames=" + String2.toCSSVString(loadVarNames) + ").");
                List tDimensions = tVariable.getDimensions();
                int nDimensions = tDimensions.size();
                //if (reallyVerbose) String2.log("i=" + i + " name=" + tVariable.getFullName() + 
                //    " type=" + tVariable.getNc3DataType());
                if (nDimensions == 1 ||
                    (nDimensions == 2 && tVariable.getDataType() == DataType.CHAR)) {
                    varNames.add(tVariable.getFullName());
                    PrimitiveArray pa = getPrimitiveArray(tVariable);

                    //decode the pa?
                    Attribute att = tVariable.findAttribute("NcHelper");
                    if (att != null && att.isString()) {
                        String ncHelper = att.getStringValue();
                        if (ncHelper == null) 
                            ncHelper = "";
                        if (pa.elementType() == PAType.SHORT && 
                            ncHelper.indexOf(originally_a_CharArray) >= 0) {
                            pa = new CharArray(((ShortArray)pa).toArray());
                        } else if (pa.elementType() == PAType.STRING &&  
                            ncHelper.indexOf(originally_a_LongArray) >= 0) {  //before JSON test
                            pa = new LongArray(pa);
                        } else if (pa.elementType() == PAType.STRING &&  
                            ncHelper.indexOf(originally_a_ULongArray) >= 0) {  //before JSON test
                            pa = new ULongArray(pa);
                        } else if (pa.elementType() == PAType.STRING && 
                            ncHelper.indexOf("JSON encoded") >= 0) {
                            int tSize = pa.size();
                            for (int i = 0; i < tSize; i++) 
                                pa.setString(i, String2.fromJson(pa.getString(i)));
                        }
                    }

                    //add the decoded pa
                    pas.add(pa);
                }
            }
        } finally {
            netcdfFile.close(); 
        }

        int pasSize = pas.size();
        PrimitiveArray paar[] = new PrimitiveArray[pasSize];
        for (int i = 0; i < pasSize; i++)
            paar[i] = (PrimitiveArray)pas.get(i);
        if (reallyVerbose) String2.log(msg + " done. nPAs=" + pasSize + 
            " TIME=" + (System.currentTimeMillis() - time) + "ms");
        return paar;
    }

    /** 
     * This writes the contents of Attributes to an .nc3 file,
     * so the .nc file can be a key-value-pair (KVP) repository.
     * This works with all PrimitiveArray types, but some datatypes 
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName for the file (This doesn't write to an intermediate file.)
     * @param attributes  Names mustn't have internal spaces.
     * @throws Exception if trouble (if an attribute's values are in a LongArray, ULongArray, unsigned array)
     */
    public static void writeAttributesToNc3(String fullName, Attributes attributes) 
        throws Exception {

        //gather the names and primitiveArrays
        String names[] = attributes.getNames();
        int nNames = names.length;
        PrimitiveArray pas[] = new PrimitiveArray[nNames];
        for (int i = 0; i < nNames; i++) 
            pas[i] = attributes.get(names[i]);

        //write to .nc
        writePAsInNc3(fullName, new StringArray(names), pas);
    }

    /** 
     * This reads the specified Attribute from an .nc file
     * which was created by writeAttributesToNc.
     * This works with all PrimitiveArray types, but some datatypes 
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName for the file 
     * @return the PrimitiveArray corresponding to name
     *    (e.g., fullName file or variable name not found)
     * @throws exception if trouble (e.g., name not found)
     */
    public static PrimitiveArray readAttributeFromNc(String fullName, String name) 
        throws Exception {

        return readPAsInNc3(fullName, new String[]{name}, new StringArray())[0];
    }

    /** 
     * This reads the specified Attributes from an .nc3 file
     * which was created by writeAttributesToNc.
     * This works with all PrimitiveArray types, but some datatypes 
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName for the file 
     * @return PrimitiveArray[] which parallels names
     * @throws exception if trouble
     *    (e.g., fullName file or one of the names not found)
     */
    public static PrimitiveArray[] readAttributesFromNc3(String fullName, String names[]) 
        throws Exception {

        return readPAsInNc3(fullName, names, new StringArray());
    }

    /** 
     * This reads all Attributes from an .nc3 file 
     * which was created by writeAttributesToNc,
     * and creates an Attributes object.
     * This works with all PrimitiveArray types, but some datatypes 
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName for the file 
     * @return attributes  
     * @throws Exception if trouble
     */
    public static Attributes readAttributesFromNc3(String fullName) 
        throws Exception {

        StringArray names = new StringArray();
        PrimitiveArray pas[] = readPAsInNc3(fullName, null, names);
        Attributes atts = new Attributes();
        int nNames = names.size();
        for (int i = 0; i < nNames; i++) 
            atts.add(names.get(i), pas[i]);
        return atts;
    }

    /** Diagnose a problem */
    public static void testJplG1SST() throws Exception {
        String dir = "c:/data/jplG1SST/";
        String request[] = new String[]{"SST"};
        StringArray varNames = new StringArray();
        NetcdfFile fi;
        Variable var;    //read    start:stop:stride

        fi = openFile(dir + "sst_20120214.nc");
        var = fi.findVariable("SST");  
        PrimitiveArray pas14 = getPrimitiveArray(var.read("0,0:14000:200,0:28000:200"), true, isUnsigned(var));
        fi.close();
String2.log(pas14.toString());     

        fi = openFile(dir + "sst_20120212.nc");
        var = fi.findVariable("SST");  
        PrimitiveArray pas13 = getPrimitiveArray(var.read("0,0:14000:200,0:28000:200"), true, isUnsigned(var));
        fi.close();
String2.log(pas13.toString());     

        String2.log("diffString=\n" + pas14.diffString(pas13));
    }

    /** 
     * Test findAllVariablesWithDims.
     * @throws Exception if trouble
     */
    public static void testFindAllVariablesWithDims() throws Exception {
        StringArray sa = new StringArray();
        NetcdfFile ncFile = openFile("c:/data/nodcTemplates/ncCFMA2a.nc");
        try {
            Variable vars[] = findAllVariablesWithDims(ncFile);
            for (int v = 0; v < vars.length; v++)
                sa.add(vars[v].getFullName());
            sa.sort();
        } finally {
            ncFile.close();
        }
        String results = sa.toString();
        String expected = 
            "bottle_posn, cast, cruise_id, latitude, longitude, ship, temperature0, time";
        Test.ensureEqual(results, expected, "results=" + results);

    }    



    /** This is a test of unlimitedDimension
     */
    public static void testUnlimited() throws Exception {
        String testUnlimitedFileName = "/temp/unlimited.nc";
        String2.log("\n* Projects.testUnlimited() " + testUnlimitedFileName);
        int strlen = 6;
        NetcdfFileWriter file = NetcdfFileWriter.createNew(
            NetcdfFileWriter.Version.netcdf3, testUnlimitedFileName);
        try {
            Group rootGroup = file.addGroup(null, "");

            // define dimensions, including unlimited
            Dimension timeDim = file.addUnlimitedDimension("time");
            ArrayList dims = new ArrayList();
            dims.add(timeDim);

            // define Variables
            Variable timeVar = file.addVariable(rootGroup, "time", DataType.DOUBLE, dims);
            timeVar.addAttribute(new Attribute("units", "seconds since 1970-01-01"));

            Variable latVar = file.addVariable(rootGroup, "lat", DataType.DOUBLE, dims);
            latVar.addAttribute(new Attribute("units", "degrees_north"));

            Variable lonVar = file.addVariable(rootGroup, "lon", DataType.DOUBLE, dims);
            lonVar.addAttribute(new Attribute("units", "degrees_east"));

            Variable sstVar = file.addVariable(rootGroup, "sst", DataType.DOUBLE, dims);
            sstVar.addAttribute(new Attribute("units", "degree_C"));

            Variable commentVar = file.addStringVariable(rootGroup, "comment", dims, strlen); 

            // create the file
            file.create();
            file.close();
            file = null;

        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        } finally {
            //ensure file is closed
            try { if (file != null) file.abort();
            } catch (Exception t2) { }
        }

        String results, expected;
        try {
            for (int row = 0; row < 5; row++) {
            //write 1 row at a time to the file
                Math2.sleep(20);
                file = NetcdfFileWriter.openExisting(testUnlimitedFileName);
                String2.log("writing row=" + row);

                int[] origin1 = new int[] {row};    
                int[] origin2 = new int[] {row, 0};  
                Array array;
                ArrayChar.D2 ac = new ArrayChar.D2(1, strlen);

                double cTime = System.currentTimeMillis() / 1000.0;
                array = get1DArray(new double[] {row},             false); file.write(file.findVariable("time"),    origin1, array);
                array = get1DArray(new double[] {33.33},           false); file.write(file.findVariable("lat"),     origin1, array);
                array = get1DArray(new double[] {-123.45},         false); file.write(file.findVariable("lon"),     origin1, array);
                array = get1DArray(new double[] {10 + row / 10.0}, false); file.write(file.findVariable("sst"),     origin1, array);
                ac.setString(0, row + " comment");                         file.write(file.findVariable("comment"), origin2, ac);
                file.flush(); //force file update

                if (row == 1) { 
                    results = NcHelper.ncdump(testUnlimitedFileName, "");
                    String2.log(results);
                    expected = 
"netcdf unlimited.nc {\n" +  //2013-09-03 netcdf-java 4.3 added blank lines
"  dimensions:\n" +
"    time = UNLIMITED;   // (2 currently)\n" +
"    comment_strlen = 6;\n" +
"  variables:\n" +
"    double time(time=2);\n" +
"      :units = \"seconds since 1970-01-01\";\n" +
"\n" +
"    double lat(time=2);\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double lon(time=2);\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    double sst(time=2);\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"    char comment(time=2, comment_strlen=6);\n" +
"\n" +
"\n" +
"  data:\n" +
"    time = \n" +
"      {0.0, 1.0}\n" +
"    lat = \n" +
"      {33.33, 33.33}\n" +
"    lon = \n" +
"      {-123.45, -123.45}\n" +
"    sst = \n" +
"      {10.0, 10.1}\n" +
"    comment = \"0 comm\", \"1 comm\"\n" +
"}\n";
                    if (!results.equals(expected)) {
                        file.close();
                        file = null;
                        Test.ensureEqual(results, expected, "trouble when i==1");
                    }
                }
            }
                

            //NOTE: instead of closing the file to write changes to disk, you can use file.flush().
        } finally {
            //ensure file is closed
            if (file != null) 
                file.close(); //writes changes to file
        }

        results = NcHelper.ncdump(testUnlimitedFileName, "");
        String2.log(results);
        expected = 
"netcdf unlimited.nc {\n" + //2013-09-03 netcdf-java 4.3 added blank lines
"  dimensions:\n" +
"    time = UNLIMITED;   // (5 currently)\n" + 
"    comment_strlen = 6;\n" +
"  variables:\n" +
"    double time(time=5);\n" +
"      :units = \"seconds since 1970-01-01\";\n" +
"\n" +
"    double lat(time=5);\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double lon(time=5);\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    double sst(time=5);\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"    char comment(time=5, comment_strlen=6);\n" +
"\n" +
"\n" +
"  data:\n" +
"    time = \n" +
"      {0.0, 1.0, 2.0, 3.0, 4.0}\n" +
"    lat = \n" +
"      {33.33, 33.33, 33.33, 33.33, 33.33}\n" +
"    lon = \n" +
"      {-123.45, -123.45, -123.45, -123.45, -123.45}\n" +
"    sst = \n" +  
"      {10.0, 10.1, 10.2, 10.3, 10.4}\n" +
"    comment = \"0 comm\", \"1 comm\", \"2 comm\", \"3 comm\", \"4 comm\"\n" +
"}\n";
        Test.ensureEqual(results, expected, "");

    }


    /**
     * An experiment with NetcdfDataset accessing a DAP sequence dataset.
     */
    public static void testSequence() throws Throwable {
        NetcdfDataset ncd = NetcdfDataset.openDataset(
            "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdCAMarCatSY");
        try {
            String2.log(ncd.toString());
        } finally {
            ncd.close();
        }
    }


    /**
     * This tests the methods in this class.
     */
    public static void testBasic() throws Throwable {
        String2.log("\n*** NcHelper.testBasic...");
        String fullName;


        //getArray  get1DArray, get1DArrayLength
        Object o;
        o = new String[]{"5.5", "7.77"}; 
        Array array = get1DArray(o, false);
        Test.ensureTrue(array instanceof ArrayChar.D2, "get1DArray a");
        Test.ensureEqual(get1DArrayLength(array), 2, "get1DArrayLength a");
        o = getArray(array);
        Test.ensureTrue(o instanceof String[], "getArray a; o=" + o.toString());
        String sar[] = (String[])o;
        Test.ensureEqual(sar.length, 2, "");
        Test.ensureEqual(sar[0], "5.5", "");
        Test.ensureEqual(sar[1], "7.77", "");

        /*
        ArrayChar.D3 ac3 = new ArrayChar.D3(2, 3, 5);
        Index index = ac3.getIndex();
        index.set(0, 0); ac3.setString(index, "a");
        index.set(0, 1); ac3.setString(index, "bb");
        index.set(0, 2); ac3.setString(index, "ccc");
        index.set(1, 0); ac3.setString(index, "dddd");
        index.set(1, 1); ac3.setString(index, "e");
        index.set(1, 2); ac3.setString(index, "f");
        o = getArray(ac3);
        Test.ensureTrue(o instanceof String[], "getArray a; o=" + o.toString());
        sar = (String[])o;
        Test.ensureEqual(sar.length, 6, "");
        Test.ensureEqual(sar[0], "a", "");
        Test.ensureEqual(sar[1], "bb", "");
        Test.ensureEqual(sar[2], "ccc", "");
        Test.ensureEqual(sar[3], "dddd", "");
        Test.ensureEqual(sar[4], "e", "");
        Test.ensureEqual(sar[5], "f", "");
        */

        o = new byte[]{(byte)2, (byte)9};
        array = get1DArray(o, false);
        Test.ensureTrue(array instanceof ArrayByte.D1, "get1DArray b");
        Test.ensureEqual(get1DArrayLength(array), 2, "get1DArrayLength a");
        o = getArray(array);
        Test.ensureTrue(o instanceof byte[], "getArray b");

        o = new double[]{2.2, 9.9};
        array = get1DArray(o, false);
        Test.ensureTrue(array instanceof ArrayDouble.D1, "get1DArray c");
        Test.ensureEqual(get1DArrayLength(array), 2, "get1DArrayLength a");
        o = getArray(array);
        Test.ensureTrue(o instanceof double[], "getArray c");

        //test readWritePAsInNc
        ByteArray ba = new ByteArray(new byte[]{1,2,4,7});
        ShortArray sha = new ShortArray(new short[]{100,200,400,700});
        IntArray ia = new IntArray(new int[]{-1, 0, 1});
        LongArray la = new LongArray(new long[]{-10L, 0, 10L});
        char charAr[] = new char[65536];
        for (int i = 0; i < 65536; i++)
            charAr[i] = (char)i;
        CharArray ca = new CharArray(charAr);
        FloatArray fa = new FloatArray(new float[]{-1.1f, 2.2f, 5.5f, Float.NaN});
        DoubleArray da = new DoubleArray(new double[]{1.1, 2.2, 9.9, Double.NaN});
        StringArray sa = new StringArray();
        for (int i = 0; i < 65536; i++) 
            sa.add("a" + (i==8?" " : (char)i) + "z");  //backspace not saved
        //write to file
        fullName = "c:/temp/PAsInNc.nc";
        File2.delete(fullName); //for test, make double sure it doesn't already exist
        StringArray varNames = new StringArray(new String[]{
            "ba", "sha", "ia", "la", "ca", "fa", "da", "sa"});
        PrimitiveArray pas[] = new PrimitiveArray[]{
            ba, sha, ia, la, ca, fa, da, sa};
        writePAsInNc3(fullName, varNames, pas);
        //read from file
        StringArray varNames2 = new StringArray();
        PrimitiveArray pas2[] = readPAsInNc3(fullName, null, varNames2);
        Test.ensureEqual(varNames, varNames2, "");
        Test.ensureEqual(pas.length, pas2.length, "");
        for (int i = 0; i < pas.length; i++) {
            int which = varNames2.indexOf(varNames.get(i));
            PrimitiveArray pa2 = pas2[which];
            Test.ensureEqual(pas[i].elementTypeString(), pa2.elementTypeString(), "i=" + i);
            if (pas[i].elementType() == PAType.STRING) {
                for (int j = 0; j < pas[i].size(); j++)
                    Test.ensureEqual(String2.toJson(pas[i].getString(j)),
                                     String2.toJson(   pa2.getString(j)), "i=" + i + " j=" + j);
            } else {
                Test.ensureEqual(pas[i].toJsonCsvString(),pa2.toJsonCsvString(),    "i=" + i);
            }
        }

        pas2 = readPAsInNc3(fullName, new String[]{"sa"}, varNames2);
        Test.ensureEqual(varNames2.size(), 1, "");
        Test.ensureEqual(varNames2.get(0), "sa", "");
        Test.ensureEqual(pas2.length, 1, "");
        Test.ensureEqual(pas[varNames.indexOf("sa")].toJsonCsvString(), pas2[0].toJsonCsvString(), "");

        //test writeAttributesToNc
        fullName = "c:/temp/AttsInNc.nc";
        Attributes atts = new Attributes();
        for (int i = 0; i < pas.length; i++)
            atts.set(varNames.get(i), pas[i]);
        writeAttributesToNc3(fullName, atts);
        //read 1
        PrimitiveArray pa = readAttributeFromNc(fullName, "sa");
        Test.ensureEqual(sa.elementTypeString(), pa.elementTypeString(), "");
        Test.ensureEqual(sa, pa, "");
        pa = readAttributeFromNc(fullName, "ia");
        Test.ensureEqual(ia.elementTypeString(), pa.elementTypeString(), "");
        Test.ensureEqual(ia, pa, "");
        //read many
        pas2 = readAttributesFromNc3(fullName, varNames.toArray());
        for (int i = 0; i < pas.length; i++) {
            Test.ensureEqual(pas[i].elementTypeString(), pas2[i].elementTypeString(), "i=" + i);
            Test.ensureEqual(pas[i].toJsonCsvString(),    pas2[i].toJsonCsvString(), "i=" + i);
        }
        //read all 
        atts = readAttributesFromNc3(fullName);
        for (int i = 0; i < varNames.size(); i++) {
            pa = atts.get(varNames.get(i));
            Test.ensureEqual(pas[i].elementTypeString(), pa.elementTypeString(), "i=" + i);
            Test.ensureEqual(pas[i].toJsonCsvString(),    pa.toJsonCsvString(), "i=" + i);
        }
        //test fail to read non-existent file
        try {
            pa = readAttributeFromNc(fullName + "zztop", "sa");
            throw new RuntimeException("shouldn't get here");
        } catch (Exception e) {
            if (e.toString().indexOf(
                "java.io.FileNotFoundException: c:\\temp\\AttsInNc.nczztop " + 
                "(The system cannot find the file specified)") < 0)
            throw e;
        }

        try {
            pas2 = readAttributesFromNc3(fullName + "zztop", varNames.toArray());
            throw new RuntimeException("shouldn't get here");
        } catch (Exception e) {
            if (e.toString().indexOf(
                "java.io.FileNotFoundException: c:\\temp\\AttsInNc.nczztop " + 
                "(The system cannot find the file specified)") < 0)
            throw e;
        }

        try {
            atts = readAttributesFromNc3(fullName + "zztop");
            throw new RuntimeException("shouldn't get here");
        } catch (Exception e) {
            if (e.toString().indexOf(
                "java.io.FileNotFoundException: c:\\temp\\AttsInNc.nczztop " + 
                "(The system cannot find the file specified)") < 0)
            throw e;
        }

        //test fail to read non-existent var
        try {
            pa = readAttributeFromNc(fullName, "zztop");
            throw new RuntimeException("shouldn't get here");
        } catch (Exception e) {
            if (e.toString().indexOf(
                String2.ERROR + ": Expected variable #0 not found while reading " +
                "c:/temp/AttsInNc.nc (loadVarNames=zztop).") < 0)
            throw e;
        }

        try {
            pas2 = readAttributesFromNc3(fullName, new String[]{"zztop"});
            throw new RuntimeException("shouldn't get here");
        } catch (Exception e) {
            if (e.toString().indexOf(
                String2.ERROR + ": Expected variable #0 not found while reading " +
                "c:/temp/AttsInNc.nc (loadVarNames=zztop).") < 0)
            throw e;
        }

        //test if defining >2GB throws exception
        fullName = "c:/temp/TooBig.nc";
        File2.delete(fullName);
        NetcdfFileWriter ncOut = NetcdfFileWriter.createNew(
            NetcdfFileWriter.Version.netcdf3, fullName);
        try {
            //"define" mode    2vars * 3000*50000*8bytes = 2,400,000,000
            Group rootGroup = ncOut.addGroup(null, "");
            ncOut.setFill(false);

            Dimension dim0 = ncOut.addDimension(rootGroup, "dim0", 3000);
            Dimension dim1 = ncOut.addDimension(rootGroup, "dim1", 50000);
            List<Dimension> dims = Arrays.asList(dim0, dim1);
            ncOut.addVariable(rootGroup, "d1", DataType.DOUBLE, dims); 
            ncOut.addVariable(rootGroup, "d2", DataType.DOUBLE, dims); 

            //define a var above 2GB  (This is what causes the problem)
            ncOut.addVariable(rootGroup, "b3", DataType.BYTE, 
                Arrays.asList(dim0)); 

            //"create" mode   (and error isn't triggered till here)
            ncOut.create();
            ncOut.close(); //it calls flush() and doesn't like flush called separately
            ncOut = null;
            File2.delete(fullName);
            if (true)
                throw new RuntimeException("Shouldn't get here.");

        } catch (Throwable t) {
            try { 
                if (ncOut != null) {
                    ncOut.close(); 
                }
            } catch (Exception e) {
            }
            File2.delete(fullName);

            String msg = t.toString();
            String2.log("Intentional error:\n" + msg);

            if (!msg.equals(
                "java.lang.IllegalArgumentException: Variable starting pos=2400000172 " +
                "may not exceed 2147483647"))
                throw t;
        }

        //test writing Strings to nc files
        //Must the char[][] be the exact right size?  What if too long?
        fullName = "c:/temp/StringsInNc.nc";
        File2.delete(fullName);
        ncOut = NetcdfFileWriter.createNew(
            NetcdfFileWriter.Version.netcdf3, fullName);
        try {
            //"define" mode
            Group rootGroup = ncOut.addGroup(null, "");
            ncOut.setFill(false);
            Dimension dim0 = ncOut.addDimension(rootGroup, "dim0", 2);
            Dimension dim1 = ncOut.addDimension(rootGroup, "dim1", 3);
            ArrayList dims = new ArrayList();
            dims.add(dim0);
            dims.add(dim1);
            Variable s1Var = ncOut.addStringVariable(rootGroup, "s1", dims, 
                4); //test strLength not long enough for all strings!
            Array ar;

            //"create" mode
            ncOut.create();
            String sa6[] = {"", "a", "abcde", "abc", "abcd", "abcde"};

            //this fails: so origin and ar must be correct nDimensions and size
            //ar = Array.factory(DataType.STRING, new int[]{6}, sa6);         
            //ncOut.writeStringData("s1", new int[]{0}, ar);

            ar = Array.factory(DataType.STRING, new int[]{2,3}, sa6);         
            ncOut.writeStringData(s1Var, new int[]{0, 0}, ar);
            ncOut.close();
            ncOut = null;
        } catch (Exception e) {
            try { if (ncOut != null) ncOut.abort();
            } catch (Exception e2) {}
        }

        //Strings are truncated to maxCharLength specified in "define" mode.
        String results = ncdump(fullName, ""); 
        String expected = 
"netcdf StringsInNc.nc {\n" + 
"  dimensions:\n" +
"    dim0 = 2;\n" +
"    dim1 = 3;\n" +
"    s1_strlen = 4;\n" +
"  variables:\n" +
"    char s1(dim0=2, dim1=3, s1_strlen=4);\n" +
"\n" +
"\n" +
"  data:\n" +
"    s1 = \n" +
"      {\"\", \"a\", \"abcd\",\"abc\", \"abcd\", \"abcd\"\n" + 
"      }\n" +
"}\n";
        String2.log("results=\n" + results);
        Test.ensureEqual(results, expected, "");

        //nc chars are essentially unsigned bytes!
        String2.log(File2.hexDump(fullName, 1000000)); 
        File2.delete(fullName);

    }

    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 2;
        String msg = "\n^^^ NcHelper.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) testBasic();
                    if (test ==  1) testFindAllVariablesWithDims();
                    if (test ==  2) testUnlimited();        
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }


}
