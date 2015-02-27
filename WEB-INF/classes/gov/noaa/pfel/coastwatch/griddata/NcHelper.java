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
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * 2013-02-21 new netcdfAll uses Java logging, not slf4j.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
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
     * varName + StringLengthSuffix is used to create the name for the char dimension 
     * of a String variable. "_strlen" is what netcdf-java uses.
     */
    public final static String StringLengthSuffix = "_strlen"; //pre 2012-06-05 was StringLength="StringLength"; 

    /** Since .nc files can store 16bit char or longs, those data types are stored as
     * shorts or Strings and these messages are added to the attributes.
     */
    public final static String originally_a_CharArray = "originally a CharArray";
    public final static String originally_a_LongArray = "originally a LongArray";

    /**
     * If saving longs as Strings, this is the maxStringLength.
     */
    public final static int LONG_MAXSTRINGLENGTH = 20;

    /**
     * This generates a String with a dump of the contents of a netcdf file.
     * WARNING: if the file is big, this can be very slow.
     * WARNING: if printData is true, this may not show the data if there is lots of data. 
     *
     * @param fullFileName
     * @param printData if true, all of the data values are printed, too.
     * @return a String with the dump text
     */
    public static String dumpString(String fullFileName, 
            boolean printData) throws Exception {
        return lowDumpString(fullFileName, printData, "");
    }

        
    /**
     * This generates a String with a dump of the contents of a netcdf file.
     * WARNING: if the file is big, this can be very slow.
     * WARNING: if printData is true, this may not show the data if there is lots of data. 
     *
     * @param fullFileName
     * @param varNames    a CSV list of varNames whose data you want printed
     * @return a String with the dump text
     */
    public static String dumpString(String fullFileName, 
          String varNames) throws Exception {
        return lowDumpString(fullFileName, true, varNames);
    }

    /**
     * This generates a String with a dump of the contents of a netcdf file.
     * WARNING: if the file is big, this can be very slow.
     * WARNING: if printData is true, this may not show the data if there is lots of data. 
     *
     * @param fullFileName  
     * @param printData if true, all of the data values are printed, too.
     * @param varNames csv list of variable names (or "")
     * @return a String with the dump text
     */
    public static String lowDumpString(String fullFileName, 
            boolean printData, String varNames) throws Exception {

        StringWriter sw = new StringWriter();
        //NCdumpW.printHeader(fullFileName, baos);
        NCdumpW.print(fullFileName, sw, 
            varNames.length() > 0? false : printData, 
            false /*print only coord variables*/, 
            fullFileName.endsWith(".ncml"), 
            false, //strict
            String2.replaceAll(varNames, ',', ';'), 
            null /*cancel*/);
        String s = sw.toString();

        //remove the directory name from the string
        //These headers are used by the HTTP GET system and so are seen
        //by users, so they should not have directory info (on first line)
        //e.g., netcdf /u00/cwatch/cwexperimental/private/PTS_PMBcrunS1dayAverages_x-135_X-105_y22_Y50_z6_Z6_t20061026000000_T20061026000000.nc {
        //because users would know where the files are (which is bad for some files
        //like SeaWiFS).
        int nPo = s.indexOf('\n');
        if (nPo > 0) {
            String firstLine = s.substring(0, nPo);
            int slash1 = firstLine.indexOf('/');
            if (slash1 >= 2 && String2.isLetter(firstLine.charAt(slash1 - 2)) && 
                firstLine.charAt(slash1 - 1) == ':')
                slash1 -= 2;
            int slash2 = firstLine.lastIndexOf('/');
            if (slash1 >= 1 && slash2 >= 0) 
                s = s.substring(0, slash1) + s.substring(slash2 + 1);
        }

        //added with switch to netcdf-java 4.0
        s = decodeNcDump(s);

        return s;
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
        int po = 0;
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
        return (double[])array.get1DJavaArray(double.class);
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
        } else n = array.getSize();
        Test.ensureTrue(n < Integer.MAX_VALUE,  
            String2.ERROR + " in NcHelper.getSize; n = " + n);
        return (int)n; //safe since checked above

    }

    /** 
     * This returns an netcdf Attribute from a PrimitiveArray.
     *
     * @param name
     * @param pa 
     * @return an Attribute
     */
    public static Attribute getAttribute(String name, PrimitiveArray pa) {
        if (pa instanceof StringArray) {
            //String2.log("***getAttribute nStrings=" + pa.size());
            String ts = Attributes.valueToNcString(pa);
            //int maxLength = 32000; //pre 2010-10-13 8000 ok; 9000 not; now >32K ok; unclear what new limit is
            //if (ts.length() > maxLength) 
            //    ts = ts.substring(0, maxLength - 3) + "...";
            //String2.log("***getAttribute string=\"" + ts + "\"");
            return new Attribute(name, ts);
        }
        return new Attribute(name, get1DArray(pa.toObjectArray()));
    }

    /** 
     * This converts a String or array of primitives into a 
     * ucar.nc2.ArrayXxx.D1.
     * The o array is used as the storage for the Array.
     *
     * @param o the String or array of primitives (e.g., int[])
     * @return an ArrayXxx.D1.  A String is converted to a ArrayChar.D2.
     *    A long[] is converted to a String[] and then to ArrayChar.D2 (nc3 files don't support longs).
     */
    public static Array get1DArray(Object o) {
        if (o instanceof String) {
            o = ((String)o).toCharArray();
            //will be handled below
        }

        if (o instanceof char[])   return Array.factory(char.class,   new int[]{((char[])o).length}, o);
        if (o instanceof byte[])   return Array.factory(byte.class,   new int[]{((byte[])o).length}, o);
        if (o instanceof short[])  return Array.factory(short.class,  new int[]{((short[])o).length}, o);
        if (o instanceof int[])    return Array.factory(int.class,    new int[]{((int[])o).length}, o);
        if (o instanceof long[])   {
            o = (new LongArray((long[])o)).toStringArray();
            //then falls through to String[] handling
        }
        if (o instanceof float[])  return Array.factory(float.class,  new int[]{((float[])o).length}, o);
        if (o instanceof double[]) return Array.factory(double.class, new int[]{((double[])o).length}, o);
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
                ac.setString(i, sar[i]);
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
     * This reads all of the values from an nDimensional variable.
     * The variable can be any shape.
     *
     * @param variable
     * @return a suitable primitiveArray 
     */
     public static PrimitiveArray getPrimitiveArray(Variable variable) throws Exception {
         return getPrimitiveArray(variable.read());
     }

    /** 
     * This converts a ucar.nc2 numeric or char ArrayXxx.Dx into a PrimitiveArray.
     *
     * @param nc2Array an nc2Array
     * @return a PrimitiveArray
     */
    public static PrimitiveArray getPrimitiveArray(Array nc2Array) {
        return PrimitiveArray.factory(getArray(nc2Array));
    }

//was
//   * This converts a ucar.nc2 numeric ArrayXxx.D1, numeric ArrayXxx.D4,
//   *   ArrayChar.D2, or ArrayChar.D5 into an array of primitives.

    /** 
     * This converts a ucar.nc2 numeric or char ArrayXxx.Dx into a 1D array of primitives.
     * ArrayChars are converted to String[].
     * ArrayBooleans are converted to byte[].
     * 
     * @param nc2Array an nc2Array
     * @return String[] (from ArrayChar.D1, split at \n), String[],
     *    or primitive[] (from numeric ArrayXxx.D1)
     */
    public static Object getArray(Array nc2Array) {
/*  commented out 2012-08-17
    This incorrectly read var as string with lots of char#0 at the end.
    This affected reading a char var[strlen] in Table.testReadNcCFASATimeSeriesProfile(false);
        //String[] from ArrayChar.D1
        if (nc2Array instanceof ArrayChar.D1) {
            String s = new String((char[])((ArrayChar.D1)nc2Array).copyTo1DJavaArray());
            String sa[] = String2.splitNoTrim(s, '\n');
            for (int i = 0; i < sa.length; i++)
                sa[i] = String2.canonical(sa[i]);
            return sa;
        }
*/

        /*
        //String[] from ArrayChar.D2
        if (nc2Array instanceof ArrayChar.D2) {
            ArrayChar.D2 ac = (ArrayChar.D2)nc2Array;
            int shape[] = ac.getShape();
            int nStrings = shape[0];
            int max = shape[1];
            String[] sar = new String[nStrings];
            //StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nStrings; i++) {
                //intern() returns the canonical String
                //Since strings are often duplicates, this can lead to a huge memory savings.
                sar[i] = String2.canonical(ac.getString(i)); 
                //sb.setLength(0);
                //for (int po = 0; po < max; po++) {
                //    char ch = ac.get(i, po);
                //    if (ch == 0)
                //        break;
                //    sb.append(ch);
                //}
                //sar[i] = sb.toString();
            }
            return sar;
        }

        //String[] from ArrayChar.D5
        if (nc2Array instanceof ArrayChar.D5) {
            ArrayChar.D5 ac = (ArrayChar.D5)nc2Array;
            int shape[] = ac.getShape();
            int nt = shape[0];
            int nz = shape[1];
            int ny = shape[2];
            int nx = shape[3];
            int nStrings = nx * ny * nz * nt;
            String[] sar = new String[nStrings];
            //StringBuilder sb = new StringBuilder();
            Index index = nc2Array.getIndex();
            int count = 0;
            for (int t = 0; t < nt; t++) 
                for (int z = 0; z < nz; z++) 
                    for (int y = 0; y < ny; y++) 
                        for (int x = 0; x < nx; x++) 
                            //intern() returns the canonical String
                            //Since strings are often duplicates, this can lead to a huge memory savings.                
                            sar[count++] = String2.canonical(ac.getString(index.set(t,z,y,x,0))); 
            return sar;
        }
        */

        //String[] from ArrayChar.Dn
        if (nc2Array instanceof ArrayChar) {
            ArrayObject ao = ((ArrayChar)nc2Array).make1DStringArray();
            String sa[] = String2.toStringArray((Object[])ao.copyTo1DJavaArray());
            for (int i = 0; i < sa.length; i++)
                sa[i] = String2.canonical(sa[i]);
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
     * This converts an netcdf DataType into a PrimitiveArray ElementClass (e.g., int.class for integer primitives).
     * BEWARE: .nc files store strings as char arrays, so 
     * if variable.getRank()==1 it is a char variable, but
     * if variable.getRang()==2 it is a String variable.
     * This throws Exception if dataType not found.
     *
     * @param dataType the Netcdf dataType
     * @return the corresponding PrimitiveArray elementClass (e.g., int.class for integer primitives)
     */
     public static Class getElementClass(DataType dataType) {
         if (dataType == DataType.BOOLEAN) return boolean.class;
         if (dataType == DataType.BYTE)    return byte.class;
         if (dataType == DataType.CHAR)    return char.class;
         if (dataType == DataType.DOUBLE)  return double.class;
         if (dataType == DataType.FLOAT)   return float.class;
         if (dataType == DataType.INT)     return int.class;
         if (dataType == DataType.SHORT)   return short.class;
         if (dataType == DataType.STRING)  return String.class;
         //STRUCTURE not converted
         Test.error(String2.ERROR + " in NcHelper.getElementType:\n" +
             " unrecognized DataType: " + dataType.toString());
         return null;
     }

    /** 
     * This converts an ElementType (e.g., int.class for integer primitives) 
     * into an netcdf DataType.
     * BEWARE: .nc files store strings as char arrays, so 
     * if variable.getRank()==1 it is a char variable, but
     * if variable.getRang()==2 it is a String variable.
     * This throws Exception if elementClass not found.
     *
     * @param elementClass the PrimitiveArray elementClass (e.g., int.class for integer primitives)
     * @return the corresponding netcdf dataType 
     */
     public static DataType getDataType(Class elementClass) {
         if (elementClass == boolean.class) return DataType.BOOLEAN;
         if (elementClass == byte.class)    return DataType.BYTE;
         if (elementClass == char.class)    return DataType.CHAR;
         if (elementClass == double.class)  return DataType.DOUBLE;
         if (elementClass == float.class)   return DataType.FLOAT;
         if (elementClass == int.class)     return DataType.INT;
         if (elementClass == short.class)   return DataType.SHORT;
         if (elementClass == String.class)  return DataType.STRING;
         //STRUCTURE not converted
         Test.error(String2.ERROR + " in NcHelper.getDataType:\n" +
             " unrecognized ElementType: " + elementClass.toString());
         return null;
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

            //I care about this exception
            netcdfFile.close();
            return String2.replaceAll(results, "\r", ""); //2013-09-03 netcdf-java 4.3 started using \r\n

        } catch (Exception e) {
            try {
                netcdfFile.close(); //make sure it is explicitly closed
            } catch (Exception e2) {
                //don't care
            }
            throw e;
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
     * This opens a local file name or an "http:" address of a .nc file.
     * ALWAYS explicitly call netcdfFile.close() when you are finished with it,
     * preferably in a "finally" clause.
     *
     * <p>This purposely does not work for opendap urls. Use the opendap 
     * methods instead.
     *
     * <p>If the fullName is an http address, the name needs to start with "http:\\" 
     * (upper or lower case) and the server needs to support "byte ranges"
     * (see ucar.nc2.NetcdfFile documentation).
     * 
     * @param fullName This may be a local file name, an "http:" address of a
     *    .nc file, or an opendap url.  
     *    If this is an .ncml file, the name must end in .ncml.
     * @return a NetcdfFile
     * @throws Exception if trouble
     */
    public static NetcdfFile openFile(String fullName) throws Exception {
        return fullName.endsWith(".ncml")? NetcdfDataset.openDataset(fullName) : 
            NetcdfFile.open(fullName);
    }

    /** 
     * This converts a List of variables to a variable[].
     *
     * @param list   
     * @return a variable[]  (or null if list is null)
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
     * Get the list of variables from a netcdf file's structure or, if
     * there is no structure, a list of variables using the largest dimension
     * (a pseudo structure).
     *
     * @param netcdfFile
     * @param variableNames if null, this will search for the variables in 
     *   a (psuedo)structure.
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
                if (reallyVerbose) String2.log("NcHelper.findVariables found a Structure.");
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
                    String2.log("NcHelper.findVariables observationDimension: " + 
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
                        String2.log("NcHelper.findVariables found an unlimited dimension: " + 
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
                    units.getString(0).indexOf(" since ") > 0 && dimensions.size() > 0) {
                    mainDimension = (Dimension)dimensions.get(0);
                    if (reallyVerbose) 
                        String2.log("NcHelper.findVariables found a time variable with dimension: " + 
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
                    if (reallyVerbose) String2.log("NcHelper.findVariables found an outer dimension: " + 
                        mainDimension.getFullName());
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
            //    " type=" + tVariable.getDataType());
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
     * @return the similar variables (or null if none have a dimension!)
     */
    public static Variable[] findMaxDVariables(NetcdfFile netcdfFile) {

        //find dimNames of highest dimension variable
        String dimNames[] = new String[0];
        Group rootGroup = netcdfFile.getRootGroup();
        List rootGroupVariables = rootGroup.getVariables(); 
        List loadVariables = null; 
        for (int v = 0; v < rootGroupVariables.size(); v++) {
            Variable variable = (Variable)rootGroupVariables.get(v);
            boolean isChar = variable.getDataType() == DataType.CHAR;
            int tnDim = variable.getRank() - (isChar? 1 : 0);
            if (tnDim > dimNames.length) {
                //a new winner
                loadVariables = new ArrayList();
                loadVariables.add(variable);
                dimNames = new String[tnDim];
                for (int d = 0; d < tnDim; d++)
                    dimNames[d] = variable.getDimension(d).getFullName();
            } else if (tnDim > 0 && tnDim == dimNames.length) {
                //a similar variable?
                boolean ok = true;
                for (int d = 0; d < tnDim; d++) {
                    if (!dimNames[d].equals(variable.getDimension(d).getFullName())) {
                        ok = false;
                        break;
                    }
                }
                if (ok) 
                    loadVariables.add(variable);
            }
        }
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
     * @throws Exception if trouble
     */
    public static Variable findVariable(NetcdfFile netcdfFile, String variableName) {
        Variable variable = netcdfFile.findVariable(variableName);
        Test.ensureNotNull(variable, 
            String2.ERROR + " in NcHelper.findVariable: '" + variableName + 
            "' not found."); 
        return variable;
    }

    /**
     * This writes the global attributes to a netcdf file.
     *
     * @param netcdfFile
     * @param varName   use "NC_GLOBAL" for global attributes
     * @param attributes the Attributes that will be set
     */
    public static void setAttributes(NetcdfFileWriteable netcdfFile, String varName, Attributes attributes) {
        boolean isGlobal = "NC_GLOBAL".equals(varName);
        String names[] = attributes.getNames();
        for (int ni = 0; ni < names.length; ni++) { 
            String tName = names[ni];
            if (!String2.isSomething(tName)) 
                continue;
            PrimitiveArray tValue = attributes.get(tName);
            if (tValue == null || tValue.size() == 0 || tValue.toString().length() == 0) {
                //do nothing
            } else if (isGlobal) {
                netcdfFile.addGlobalAttribute(getAttribute(tName, tValue));
            } else {
                netcdfFile.addVariableAttribute(varName, getAttribute(tName, tValue));
            }
        }
    }



    /**
     * Given an attribute, this adds the value to the attributes.
     * If there is trouble, this lots a warning message and returns null.
     * This is low level and isn't usually called directly.
     *
     * @param att   
     * @param varName the variable name (or "global"), used for diagnostic messages only
     * @return a PrimitiveArray or null if trouble
     */
    public static PrimitiveArray getAttributePA(String varName, ucar.nc2.Attribute att) {
        if (att == null) {
            if (reallyVerbose)
                String2.log("Warning: NcHelper.getAttributePA " + varName + " att=null");
            return null;
        } else if (String2.isSomething(att.getFullName())) {
            try {
                //decodeAttribute added with switch to netcdf-java 4.0
                return PrimitiveArray.factory(decodeAttribute(getArray(att.getValues()))); 
            } catch (Throwable t) {
                String2.log("Warning: NcHelper caught an exception while reading '" + 
                    varName + "' attribute=" + att.getFullName() + "\n" +
                    MustBe.throwableToString(t));
                return null;
            }
        } else {
            if (reallyVerbose)
                String2.log("Warning: NcHelper.getAttributePA " + varName + 
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
    public static PrimitiveArray getVariableAttribute(Variable variable, String attributeName) {
        return getAttributePA(variable.getFullName(), variable.findAttribute(attributeName)); 
    }


    /**
     * This gets one global attribute from a netcdf variable.
     *
     * @param netcdfFile
     * @param attributeName
     * @return the attribute (or null if none)
     */
    public static PrimitiveArray getGlobalAttribute(NetcdfFile netcdfFile, String attributeName) {
        return getAttributePA("global", netcdfFile.findGlobalAttribute(attributeName)); 
    }

    /**
     * Given an attribute, this adds the value to the attributes.
     * If there is trouble, this logs a warning message and returns.
     * This is low level and isn't usually called directly.
     */
    public static void addAttribute(String varName, ucar.nc2.Attribute att, Attributes attributes) {
        if (att == null) {
            if (reallyVerbose)
                String2.log("Warning: NcHelper.addAttribute " + varName + " att=null");
            return;
        }
        if (String2.isSomething(att.getFullName())) {
            //attributes.set calls String2.canonical (useful since many names are in many datasets)
            attributes.set(att.getFullName(), getAttributePA(varName, att));
        } else {
            if (reallyVerbose)
                String2.log("Warning: NcHelper.addAttribute " + varName + 
                    " att.getFullName()=" + String2.annotatedString(att.getFullName()));
        }
    }

    /**
     * This reads the global attributes from a netcdf file.
     *
     * @param netcdfFile
     * @param attributes the Attributes that will be populated
     */
    public static void getGlobalAttributes(NetcdfFile netcdfFile, Attributes attributes) {
        getGlobalAttributes(netcdfFile.getGlobalAttributes(), attributes);
    }

    /**
     * This reads the global attributes from a netcdf file.
     *
     * @param globalAttList
     * @param attributes the Attributes that will be populated
     */
    public static void getGlobalAttributes(List globalAttList, Attributes attributes) {

        //read the globalAttributes
        if (globalAttList == null)
            return;
        int nAtt = globalAttList.size();
        for (int att = 0; att < nAtt; att++)  //there is also a dods.dap.Attribute
            addAttribute("global", (ucar.nc2.Attribute)globalAttList.get(att), attributes);
    }

    /**
     * This adds to the attributes for a netcdf variable.
     *
     * @param variable
     * @param attributes the Attributes that will be added to
     */
    public static void getVariableAttributes(Variable variable, Attributes attributes) {
        if (variable == null)
            return;
        String variableName = variable.getFullName();
        List variableAttList = variable.getAttributes();
        if (variableAttList == null) 
            return;
        for (int att = 0; att < variableAttList.size(); att++) 
            addAttribute(variableName, (ucar.nc2.Attribute)variableAttList.get(att), attributes);
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
    public static PrimitiveArray getPrimitiveArray(Variable variable, int firstRow, int lastRow) 
            throws Exception {

        if (lastRow == -1)
            return getPrimitiveArray(variable.read());

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
        PrimitiveArray pa = getPrimitiveArray(variable.read(origin, shape));

        //eek! opendap returns a full-sized array! 
        //     netcdf  returns a shape-sized array
        if (pa.size() < nRows) 
            Test.error(String2.ERROR + " in NcHelper.getPrimitiveArray(firstRow=" + 
                firstRow + " lastRow=" + lastRow + ")\n" +
                "variable.read returned too few (" + pa.size() + ").");
        if (pa.size() > nRows) {
            //it full-sized; reduce to correct size
            if (reallyVerbose) String2.log("NcHelper.getPrimitiveArray variable.read returned entire variable!"); 
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

            //I care about this exception
            netcdfFile.close();
        } catch (Exception e) {
            try {
                netcdfFile.close(); //make sure it is explicitly closed
            } catch (Exception e2) {
                //don't care
            }
            throw e;
        }

        return tColumnNames;
    }

    /**
     * This writes values to a 1D netcdf variable in a NetcdfFileWriteable.
     * This works with all PrimitiveArray types, but
     * <br>LongArray is stored as a StringArray, so retrieve with 
     * <br>pa = new LongArray(pa), and
     * <br>CharArray is stored as a ShortArray, so retrieve with
     * <br>pa = new CharArray(((ShortArray)pa).toArray()).
     *
     * @param netcdfFileWriteable
     * @param variableName
     * @param firstRow   
     * @param pa will be converted to the appropriate numeric type
     * @param maxStringLength is the maxLength to use for String data.
     *    If pa is not a StringArray, this is ignored.
     *    If <= 0, maxLength is calculated from the strings.
     *    What really matters is maxLength when var created; 
     *    longer strings here will be truncated.
     * @throws Exception if trouble
     */
    public static void write(NetcdfFileWriteable netcdfFileWriteable, 
        String variableName, int firstRow, PrimitiveArray pa, 
        int maxStringLength) throws Exception {

        write(netcdfFileWriteable, variableName, 
            new int[]{firstRow}, new int[]{pa.size()}, pa);

        /* pre 2012-06-06 was...
        if (pa.elementClass() == char.class) 
            pa = new ShortArray(((CharArray)pa).toArray());
        else if (pa.elementClass() == long.class) 
            pa = new StringArray(pa);

        if (pa.elementClass() == String.class) {
            StringArray sa = (StringArray)pa;
            //maxStringLength here doesn't matter much 
            //(may be more or less than var expects).
            //Longer strings will be truncated.
            if (maxStringLength <= 0)
                maxStringLength = sa.maxStringLength();
            int n = sa.size();
            ArrayChar.D2 ac = new ArrayChar.D2(n, maxStringLength);
            for (int i = 0; i < n; i++) {
                ac.setString(i, sa.get(i));
            }
            int rowOrigin[] = {firstRow, 0}; 
            netcdfFileWriteable.write(variableName, rowOrigin, ac);         

        } else {
            Array array = get1DArray(pa.toObjectArray());
            int rowOrigin[] = {firstRow}; 
            netcdfFileWriteable.write(variableName, rowOrigin, array);         
        }
        */
    }

    /**
     * This returns a PrimitiveArray (usually the same one) that has a 
     * data type that is suitable for .nc files
     * (LongArray becomes StringArray, CharArray becomes ShortArray).
     *
     * @param pa a PrimitiveArray
     * @returns a PrimitiveArray (usually the same one) that has a 
     * data type that is suitable for .nc files.
     */
    public static PrimitiveArray getNcSafePA(PrimitiveArray pa) {
        if (pa.elementClass() == char.class) 
            return new ShortArray(((CharArray)pa).toArray());
        if (pa.elementClass() == long.class)
            return new StringArray(pa);
        return pa;
    }

    /** 
     * This is a thin wrapper to call netcdfFileWriteable.writeStringData (for StringArray)
     * or netcdfFileWriteable.write to write the pa to the file.
     *
     * <p>This will convert CharArray to ShortArray and LongArray to StringArray, 
     *    but it usually saves memory if caller does it.
     *
     * @param netcdfFileWriteable
     * @param varName
     * @param origin  The start of where the data will be written in the var.
     *    Don't include the StringLength dimension.
     * @param shape   The shape that the data will be arranged into in the var.
     *    Don't include StringLength dimension.
     * @param pa   the data to be written 
     */
    public static void write(NetcdfFileWriteable netcdfFileWriteable, 
        String varName, int origin[], int shape[], PrimitiveArray pa) throws Exception {

        pa = getNcSafePA(pa);

        if (pa instanceof StringArray) {
            netcdfFileWriteable.writeStringData(varName, origin, 
                Array.factory(String.class, shape, pa.toObjectArray()));         
        } else {
            netcdfFileWriteable.write(varName, origin, 
                Array.factory(pa.elementClass(), shape, pa.toObjectArray()));         
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
     * This works with all PrimitiveArray types, but some datatypes 
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
    public static void writePAsInNc(String fullName, StringArray varNames, PrimitiveArray pas[]) 
            throws Exception {
        if (reallyVerbose) String2.log("NcHelper.savePAsInNc " + fullName); 
        long time = System.currentTimeMillis();

        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);
        File2.delete(fullName);

        //open the file (before 'try'); if it fails, no temp file to delete
        NetcdfFileWriteable nc = NetcdfFileWriteable.createNew(fullName + randomInt, false);

        //tpas is same as pas, but with CharArray and LongArray converted to StringArray
        int nVars = varNames.size();
        PrimitiveArray tpas[] = new PrimitiveArray[nVars]; 

        try {

            //add the variables
            for (int var = 0; var < nVars; var++) {
                String name = varNames.get(var);
                tpas[var] = pas[var];
                if (tpas[var].elementClass() == char.class) {
                    //nc 'char' is 1 byte!  So store java char (2 bytes) as shorts.
                    tpas[var] = new ShortArray(((CharArray)pas[var]).toArray());
                } else if (tpas[var].elementClass() == long.class) {
                    //these will always be decoded by fromJson as-is; no need to encode with toJson
                    tpas[var] = new StringArray(pas[var]);
                } else if (tpas[var].elementClass() == String.class) {
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

                Class type = tpas[var].elementClass();
                Dimension dimension  = nc.addDimension(name, tpas[var].size());
                if (type == String.class) {
                    int max = Math.max(1, ((StringArray)tpas[var]).maxStringLength()); //nc libs want at least 1; 0 happens if no data
                    Dimension lengthDimension  = nc.addDimension(name + StringLengthSuffix, max);
                    nc.addVariable(name, DataType.CHAR, 
                        new Dimension[]{dimension, lengthDimension}); 
                    if (pas[var].elementClass() == long.class) 
                        nc.addVariableAttribute(name, "NcHelper", originally_a_LongArray); 
                    else if (pas[var].elementClass() == String.class) 
                        nc.addVariableAttribute(name, "NcHelper", "JSON encoded"); 
                } else {
                    nc.addVariable(name, DataType.getType(type), new Dimension[]{dimension}); 
                    if (pas[var].elementClass() == char.class) 
                        nc.addVariableAttribute(name, "NcHelper", originally_a_CharArray); 
                }
            }

            //leave "define" mode
            nc.create();

            //write the data
            for (int var = 0; var < nVars; var++) {
                nc.write(varNames.get(var), get1DArray(tpas[var].toObjectArray()));
            }

            //if close throws exception, it is trouble
            nc.close(); //it calls flush() and doesn't like flush called separately

            File2.rename(fullName + randomInt, fullName);

            //diagnostic
            if (reallyVerbose) String2.log("  NcHelper.savePAsInNc done. TIME=" + 
                (System.currentTimeMillis() - time));
            //ncDump("End of Table.saveAsFlatNc", directory + name + ext, false);

        } catch (Exception e) {
            //try to close the file
            try {
                nc.close(); //it calls flush() and doesn't like flush called separately
            } catch (Exception e2) {
                //don't care
            }

            //delete any partial or pre-existing file
            File2.delete(fullName + randomInt);

            throw e;
        }

    }

    /**
     * This reads the PAs in the .nc file.
     * This works with all PrimitiveArray types, but some datatypes 
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName the name of the .nc file.
     * @param loadVarNames the names of the variables to load (null to get all)
     * @param varNames this will receive the varNames
     * @return PrimitiveArray[] paralleling the names in varNames
     * @throws Exception if trouble (e.g., one of the loadVarNames wasn't found)
     */
    public static PrimitiveArray[] readPAsInNc(String fullName, String loadVarNames[],
        StringArray varNames) throws Exception {

        if (reallyVerbose) String2.log("NcHelper.readPAsInNc " + fullName
            //+ " \n  loadVarNames=" + String2.toCSSVString(loadVarNames)
            ); 
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
                //    " type=" + tVariable.getDataType());
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
                        if (pa.elementClass() == short.class && 
                            ncHelper.indexOf(originally_a_CharArray) >= 0) {
                            pa = new CharArray(((ShortArray)pa).toArray());
                        } else if (pa.elementClass() == String.class &&  
                            ncHelper.indexOf(originally_a_LongArray) >= 0) {  //before JSON test
                            pa = new LongArray(pa);
                        } else if (pa.elementClass() == String.class && 
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

            //I care about this exception
            netcdfFile.close();

        } catch (Exception e) {
            //make sure it is explicitly closed
            try {
                netcdfFile.close(); 
            } catch (Exception e2) {
                //don't care
            }
            throw e;
        }

        int pasSize = pas.size();
        PrimitiveArray paar[] = new PrimitiveArray[pasSize];
        for (int i = 0; i < pasSize; i++)
            paar[i] = (PrimitiveArray)pas.get(i);
        if (reallyVerbose) String2.log("  NcHelper.readPAsInNc done. nPAs=" + pasSize + 
            " TIME=" + (System.currentTimeMillis() - time));
        return paar;
    }

    /** 
     * This writes the contents of Attributes to an .nc file,
     * so the .nc file can be a key-value-pair (KVP) repository.
     * This works with all PrimitiveArray types, but some datatypes 
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName for the file (This doesn't write to an intermediate file.)
     * @param attributes  Names mustn't have internal spaces.
     * @throws Exception if trouble (if an attribute's values are in a LongArray)
     */
    public static void writeAttributesToNc(String fullName, Attributes attributes) 
        throws Exception {

        //gather the names and primitiveArrays
        String names[] = attributes.getNames();
        int nNames = names.length;
        PrimitiveArray pas[] = new PrimitiveArray[nNames];
        for (int i = 0; i < nNames; i++) 
            pas[i] = attributes.get(names[i]);

        //write to .nc
        writePAsInNc(fullName, new StringArray(names), pas);
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

        return readPAsInNc(fullName, new String[]{name}, new StringArray())[0];
    }

    /** 
     * This reads the specified Attributes from an .nc file
     * which was created by writeAttributesToNc.
     * This works with all PrimitiveArray types, but some datatypes 
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName for the file 
     * @return PrimitiveArray[] which parallels names
     * @throws exception if trouble
     *    (e.g., fullName file or one of the names not found)
     */
    public static PrimitiveArray[] readAttributesFromNc(String fullName, String names[]) 
        throws Exception {

        return readPAsInNc(fullName, names, new StringArray());
    }

    /** 
     * This reads all Attributes from an .nc file 
     * which was created by writeAttributesToNc,
     * and creates an Attributes object.
     * This works with all PrimitiveArray types, but some datatypes 
     * are specially encoded in the files and then automatically decoded when read.
     *
     * @param fullName for the file 
     * @return attributes  
     * @throws Exception if trouble
     */
    public static Attributes readAttributesFromNc(String fullName) 
        throws Exception {

        StringArray names = new StringArray();
        PrimitiveArray pas[] = readPAsInNc(fullName, null, names);
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
        PrimitiveArray pas14 = getPrimitiveArray(var.read("0,0:14000:200,0:28000:200"));
        fi.close();
String2.log(pas14.toString());     

        fi = openFile(dir + "sst_20120212.nc");
        var = fi.findVariable("SST");  
        PrimitiveArray pas13 = getPrimitiveArray(var.read("0,0:14000:200,0:28000:200"));
        fi.close();
String2.log(pas13.toString());     

        String2.log("diffString=\n" + pas14.diffString(pas13));
    }

    public static String testLowNcDump(String fullFileName) throws Exception {
        StringWriter sw = new StringWriter();
        //NCdumpW.printHeader(fullFileName, sw);
        NCdumpW.print(fullFileName, sw, 
            false /*printData*/, false /*print only coord variables*/, 
            fullFileName.endsWith(".ncml"), false,
            "", null /*cancel*/);
        return sw.toString();
    }

    /** 
     * Test findAllVariablesWithDims.
     * @throws Exception if trouble
     */
    public static void testFindAllVariablesWithDims() throws Exception {
        NetcdfFile ncFile = openFile("c:/data/nodcTemplates/ncCFMA2a.nc");

        StringArray sa = new StringArray();
        try {
            Variable vars[] = findAllVariablesWithDims(ncFile);
            for (int v = 0; v < vars.length; v++)
                sa.add(vars[v].getFullName());
            sa.sort();
            ncFile.close();
        } catch (Exception e) {
            ncFile.close();
            throw e;
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
        NetcdfFileWriteable file = NetcdfFileWriteable.createNew(testUnlimitedFileName);
        int strlen = 6;

        try {

            // define dimensions, including unlimited
            Dimension timeDim = file.addUnlimitedDimension("time");
            ArrayList dims = new ArrayList();
            dims.add(timeDim);

            // define Variables
            file.addVariable("time", DataType.DOUBLE, dims);
            file.addVariableAttribute("time", "units", "seconds since 1970-01-01");

            file.addVariable("lat", DataType.DOUBLE, dims);
            file.addVariableAttribute("lat", "units", "degrees_north");

            file.addVariable("lon", DataType.DOUBLE, dims);
            file.addVariableAttribute("lon", "units", "degrees_east");

            file.addVariable("sst", DataType.DOUBLE, dims);
            file.addVariableAttribute("sst", "units", "degree_C");

            file.addStringVariable("comment", dims, strlen); 

            // create the file
            file.create();

        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        } finally {
            //ensure file is closed
            file.close();
        }

        String results, expected;
        try {
            for (int row = 0; row < 5; row++) {
            //write 1 row at a time to the file
                Math2.sleep(20);
                file = NetcdfFileWriteable.openExisting(testUnlimitedFileName);
                Dimension timeDim = file.findDimension("time");
                String2.log("writing row=" + row);

                int[] origin1 = new int[] {row};    
                int[] origin2 = new int[] {row, 0};  
                Array array;
                ArrayChar.D2 ac = new ArrayChar.D2(1, strlen);

                double cTime = System.currentTimeMillis() / 1000.0;
                array = Array.factory(new double[] {row});             file.write("time",    origin1, array);
                array = Array.factory(new double[] {33.33});           file.write("lat",     origin1, array);
                array = Array.factory(new double[] {-123.45});         file.write("lon",     origin1, array);
                array = Array.factory(new double[] {10 + row / 10.0}); file.write("sst",     origin1, array);
                ac.setString(0, row + " comment");                     file.write("comment", origin2, ac);
                file.flush(); //force file update

                if (row == 1) { 
                    results = NcHelper.dumpString(testUnlimitedFileName, true);
                    String2.log(results);
                    expected = 
"netcdf unlimited.nc {\n" +  //2013-09-03 netcdf-java 4.3 added blank lines
"  dimensions:\n" +
"    time = UNLIMITED;   // (2 currently\n" +
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
" data:\n" +
"time =\n" +
"  {0.0, 1.0}\n" +
"lat =\n" +
"  {33.33, 33.33}\n" +
"lon =\n" +
"  {-123.45, -123.45}\n" +
"sst =\n" +
"  {10.0, 10.1}\n" +
"comment =\"0 comm\", \"1 comm\"\n" +
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

        results = NcHelper.dumpString(testUnlimitedFileName, true);
        String2.log(results);
        expected = 
"netcdf unlimited.nc {\n" + //2013-09-03 netcdf-java 4.3 added blank lines
"  dimensions:\n" +
"    time = UNLIMITED;   // (5 currently\n" + //2013-09-03 bug! netcdf-java 4.3 removed close )
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
" data:\n" +
"time =\n" +
"  {0.0, 1.0, 2.0, 3.0, 4.0}\n" +
"lat =\n" +
"  {33.33, 33.33, 33.33, 33.33, 33.33}\n" +
"lon =\n" +
"  {-123.45, -123.45, -123.45, -123.45, -123.45}\n" +
"sst =\n" +  
"  {10.0, 10.1, 10.2, 10.3, 10.4}\n" +
"comment =\"0 comm\", \"1 comm\", \"2 comm\", \"3 comm\", \"4 comm\"\n" +
"}\n";
        Test.ensureEqual(results, expected, "");

    }


    /**
     * An experiment with NetcdfDataset accessing a DAP sequence dataset.
     */
    public static void testSequence() throws Throwable {
        NetcdfDataset ncd = NetcdfDataset.openDataset(
            "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdCAMarCatSY");
        String2.log(ncd.toString());
        ncd.close();
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
        Array array = get1DArray(o);
        Test.ensureTrue(array instanceof ArrayChar.D2, "get1DArray a");
        Test.ensureEqual(get1DArrayLength(array), 2, "get1DArrayLength a");
        o = getArray(array);
        Test.ensureTrue(o instanceof String[], "getArray a; o=" + o.toString());
        String sar[] = (String[])o;
        Test.ensureEqual(sar.length, 2, "");
        Test.ensureEqual(sar[0], "5.5", "");
        Test.ensureEqual(sar[1], "7.77", "");

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


        o = new byte[]{(byte)2, (byte)9};
        array = get1DArray(o);
        Test.ensureTrue(array instanceof ArrayByte.D1, "get1DArray b");
        Test.ensureEqual(get1DArrayLength(array), 2, "get1DArrayLength a");
        o = getArray(array);
        Test.ensureTrue(o instanceof byte[], "getArray b");

        o = new double[]{2.2, 9.9};
        array = get1DArray(o);
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
            sa.add("a" + (char)i + "z");  
        //write to file
        fullName = "c:/temp/PAsInNc.nc";
        File2.delete(fullName); //for test, make double sure it doesn't already exist
        StringArray varNames = new StringArray(new String[]{
            "ba", "sha", "ia", "la", "ca", "fa", "da", "sa"});
        PrimitiveArray pas[] = new PrimitiveArray[]{
            ba, sha, ia, la, ca, fa, da, sa};
        writePAsInNc(fullName, varNames, pas);
        //read from file
        StringArray varNames2 = new StringArray();
        PrimitiveArray pas2[] = readPAsInNc(fullName, null, varNames2);
        Test.ensureEqual(varNames, varNames2, "");
        Test.ensureEqual(pas.length, pas2.length, "");
        for (int i = 0; i < pas.length; i++) {
            int which = varNames2.indexOf(varNames.get(i));
            PrimitiveArray pa2 = pas2[which];
            Test.ensureEqual(pas[i].elementClassString(), pa2.elementClassString(), "i=" + i);
            if (pas[i].elementClass() == String.class) {
                for (int j = 0; j < pas[i].size(); j++)
                    Test.ensureEqual(String2.toJson(pas[i].getString(j)),
                                     String2.toJson(   pa2.getString(j)), "i=" + i + " j=" + j);
            } else {
                Test.ensureEqual(pas[i].toJsonCsvString(),pa2.toJsonCsvString(),    "i=" + i);
            }
        }

        pas2 = readPAsInNc(fullName, new String[]{"sa"}, varNames2);
        Test.ensureEqual(varNames2.size(), 1, "");
        Test.ensureEqual(varNames2.get(0), "sa", "");
        Test.ensureEqual(pas2.length, 1, "");
        Test.ensureEqual(pas[varNames.indexOf("sa")].toJsonCsvString(), pas2[0].toJsonCsvString(), "");

        //test writeAttributesToNc
        fullName = "c:/temp/AttsInNc.nc";
        Attributes atts = new Attributes();
        for (int i = 0; i < pas.length; i++)
            atts.set(varNames.get(i), pas[i]);
        writeAttributesToNc(fullName, atts);
        //read 1
        PrimitiveArray pa = readAttributeFromNc(fullName, "sa");
        Test.ensureEqual(sa.elementClassString(), pa.elementClassString(), "");
        Test.ensureEqual(sa, pa, "");
        pa = readAttributeFromNc(fullName, "ia");
        Test.ensureEqual(ia.elementClassString(), pa.elementClassString(), "");
        Test.ensureEqual(ia, pa, "");
        //read many
        pas2 = readAttributesFromNc(fullName, varNames.toArray());
        for (int i = 0; i < pas.length; i++) {
            Test.ensureEqual(pas[i].elementClassString(), pas2[i].elementClassString(), "i=" + i);
            Test.ensureEqual(pas[i].toJsonCsvString(),    pas2[i].toJsonCsvString(), "i=" + i);
        }
        //read all 
        atts = readAttributesFromNc(fullName);
        for (int i = 0; i < varNames.size(); i++) {
            pa = atts.get(varNames.get(i));
            Test.ensureEqual(pas[i].elementClassString(), pa.elementClassString(), "i=" + i);
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
            pas2 = readAttributesFromNc(fullName + "zztop", varNames.toArray());
            throw new RuntimeException("shouldn't get here");
        } catch (Exception e) {
            if (e.toString().indexOf(
                "java.io.FileNotFoundException: c:\\temp\\AttsInNc.nczztop " + 
                "(The system cannot find the file specified)") < 0)
            throw e;
        }

        try {
            atts = readAttributesFromNc(fullName + "zztop");
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
            pas2 = readAttributesFromNc(fullName, new String[]{"zztop"});
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
        NetcdfFileWriteable ncOut = null;
        try {
            //"define" mode    2vars * 3000*50000*8bytes = 2,400,000,000
            ncOut = NetcdfFileWriteable.createNew(fullName, false); //fill=false
            Dimension dim0 = ncOut.addDimension("dim0", 3000);
            Dimension dim1 = ncOut.addDimension("dim1", 50000);
            ArrayList dims = new ArrayList();
            dims.add(dim0);
            dims.add(dim1);
            ncOut.addVariable("d1", DataType.DOUBLE, dims); 
            ncOut.addVariable("d2", DataType.DOUBLE, dims); 

            //define a var above 2GB  (This is what causes the problem)
            dims = new ArrayList();
            dims.add(dim0);
            ncOut.addVariable("b3", DataType.BYTE,   dims); 

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
                    File2.delete(fullName);
                }
            } catch (Exception e) {
            }

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
        ncOut = null;
        try {
            //"define" mode
            ncOut = NetcdfFileWriteable.createNew(fullName, false); //fill=false
            Dimension dim0 = ncOut.addDimension("dim0", 2);
            Dimension dim1 = ncOut.addDimension("dim1", 3);
            ArrayList dims = new ArrayList();
            dims.add(dim0);
            dims.add(dim1);
            ncOut.addStringVariable("s1", dims, 4); //test strLength not long enough for all strings!
            Array ar;

            //"create" mode
            ncOut.create();
            String sa6[] = {"", "a", "abcde", "abc", "abcd", "abcde"};

            //this fails: so origin and ar must be correct nDimensions and size
            //ar = Array.factory(String.class, new int[]{6}, sa6);         
            //ncOut.writeStringData("s1", new int[]{0}, ar);

            ar = Array.factory(String.class, new int[]{2,3}, sa6);         
            ncOut.writeStringData("s1", new int[]{0, 0}, ar);

            //close file
            ncOut.close(); //it calls flush() and doesn't like flush called separately
            ncOut = null;

            //Strings are truncated to maxCharLength specified in "define" mode.
            String results = dumpString(fullName, true); //printData
            String expected = 
"netcdf StringsInNc.nc {\n" + 
"  dimensions:\n" +
"    dim0 = 2;\n" +
"    dim1 = 3;\n" +
"    s1_strlen = 4;\n" +
"  variables:\n" +
"    char s1(dim0=2, dim1=3, s1_strlen=4);\n" +
"\n" +
" data:\n" +
"s1 =\n" +
"  {\"\", \"a\", \"abcd\",\"abc\", \"abcd\", \"abcd\"\n" + 
"  }\n" +
"}\n";
            String2.log("results=\n" + results);
            Test.ensureEqual(results, expected, "");

            //nc chars are essentially unsigned bytes!
            String2.log(File2.hexDump(fullName, 1000000)); 

        } catch (Throwable t) {
            try { 
                if (ncOut != null) {
                    ncOut.close(); 
                    File2.delete(fullName);
                }
            } catch (Exception e) {
            }

            throw t;
        }
    }

    /**
     * This tests the methods in this class.
     */
    public static void test() throws Throwable {
        String2.log("\n*** NcHelper.test...");

        /* */
        testBasic();
        testFindAllVariablesWithDims();
        testUnlimited();        
 
        //done
        String2.log("\n***** NcHelper.test finished successfully");
        Math2.incgc(2000); //in a test
    } 

}
