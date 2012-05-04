/* 
 * OpendapHelper Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.List;

/** The Java DAP classes.  */
import dods.dap.*;


/**
 * This class has some static convenience methods related to Opendap and the 
 * other Data classes.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-12-07
 */
public class OpendapHelper  {

    /**
     * Set this to true (by calling verbose=true in your program, not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /** "ERROR" is defined here (from String2.ERROR) so that it is consistent in log files. */
    public final static String ERROR = String2.ERROR; 

    /**
     * This defines the end-of-line characters to use when writing dap responses.
     * <br>The DAP2 specification says the end-of-line marker in a DODS data stream 
     *   "MUST use CR LF to conform to DAP2" 
     *   (James Gallagher's email to opendap-tech mailing list on Aug 28, 2007).
     * <br>But, the JDAP library only seems to be able to read DODS data streams 
     *   that use just LF. It does not work with DODS data streams that use CR LF.
     *   This can be confirmed by looking at the definition of endSequence in 
     *   dods.dap.HeaderInputStream and by seeing how it is used in getMoreBytes
     *   (in that class).
     * <br>(What about opendap C library?)
     * <br>For now, to be safe and compatible (although not compliant), use just LF.
     */
    public final static String EOL = "\n";

    public static int DEFAULT_TIMEOUT = 120000;

    /**
     * This converts a das to a string.
     *
     * @param das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
     * @return the string form of the das
     */
    public static String getDasString(DAS das) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        das.print(baos);
        return baos.toString();
    }

    /**
     * This converts a dds to a string.
     *
     * @param dds from dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT)
     * @return the string form of the dds
     */
    public static String getDdsString(DDS dds) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dds.print(baos);
        return baos.toString();
    }

    /** 
     * This sets attributes for a variable from the das.
     * See the more commonly used getOpendapAttributes.
     *
     * @param das the das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
     * @param variableName the name of the desired variable (use "GLOBAL" for global attributes).
     *     Composite names, e.g., MOC1.temperature, are allowed.
     * @return the desired attributeTable (or null if not found)
     */
    public static AttributeTable getAttributeTable(DAS das, String variableName) {
        if (variableName.equals("GLOBAL")) {
            //find the GLOBAL attributes
            //this assumes that GLOBAL is in the name (I've see GLOBAL and NC_GLOBAL)
            Enumeration names = das.getNames();
            while (names.hasMoreElements()) {
                String s = (String)names.nextElement();
                if (s.indexOf("GLOBAL") >= 0) {
                    return das.getAttributeTable(s);
                }
            }
            return null;
        } 
        
        AttributeTable at = das.getAttributeTable(variableName);
        if (at != null)
            return at;

        //is it in a parent attribute table?   (MOC1.temperature may be in MOC1's attributeTable)
        String sa[] = String2.split(variableName, '.');
        if (sa.length == 1)
            return null;
        for (int i = 0; i < sa.length; i++) {
            if (i == 0) {
                at = das.getAttributeTable(sa[i]);
            } else {
                Attribute a = at.getAttribute(sa[i]);
                if (a == null) {
                    //String2.log("getAttributeTable: attribute #" + i + "=" + sa[i] + " is null.");
                    return null;
                }
                if (a.getType() != Attribute.CONTAINER) {
                    //String2.log("getAttributeTable: attribute #" + i + "=" + sa[i] + 
                    //    " type=" + a.getType() + " is not CONTAINER.");
                    return null;
                }
                at = a.getContainer();
            }
            //String2.log("getAttributeTable: attTable #" + i + "=" + sa[i] + " is " + (at == null? "null." : "not null."));
            if (at == null) 
                return null;
        }
        return at;
    }

    /** 
     * This sets attributes for a variable from the das.
     * See javadocs for the other getOpendapAttributes().
     *
     * @param das the das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
     * @param variableName the name of the desired variable (use "GLOBAL" for global attributes)
     * @param attributes the Attributes to which attributes will be added
     * @throws Exception if trouble.  No attributeTable for variableName is not an error;
     *    it just doesn't change 'attributes'.
     */
    public static void getAttributes(DAS das, String variableName, Attributes attributes) {

        //get the attributeTable
        AttributeTable attributeTable = getAttributeTable(das, variableName);
        if (attributeTable == null) {
            if (verbose) String2.log("  No attributeTable for " + variableName + ".");
            return;
        }
        getAttributes(attributeTable, attributes);
        if (verbose) String2.log("  attributeTable for " + variableName + " has " + 
            attributes.size() + " attribute(s).");
    }

    /** 
     * This sets attributes from the information in the attributeTable.
     *
     * <p>Following the getValue return type, DUint16 is treated like short 
     * and DUInt32 is treated like int.
     * 
     * Currently this converts url, UNKNOWN, and unknown attributes to Strings.
     *
     * @param attributeTable It isn't an error if this is null; but nothing is done.
     * @param attributes the Attributes to which attributes will be added
     * @throws Exception if trouble. 
     */
    public static void getAttributes(AttributeTable attributeTable, Attributes attributes) {

        if (attributeTable == null) {
            return;
        }

        //get the attributes
        Enumeration names = attributeTable.getNames();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();            
            dods.dap.Attribute attribute = attributeTable.getAttribute(name);
            //If an attribute isContainer, then attribute.getValues() will fail. e.g.,
            //http://dm1.caricoos.org/thredds/dodsC/content/wrf_archive/wrfout_d01_2009-09-25_12_00_00.nc.das
            String[] sar;
            if (attribute.isContainer()) {
                sar = new String[0];
                if (verbose)
                    String2.log("WARNING: OpendapHelper attributeName=" + name + 
                        " isContainer!  So the attributes weren't read.");
            } else {
                sar = String2.toStringArray(String2.toArrayList(attribute.getValues()).toArray());
            }

            //remove enclosing quotes from strings
            for (int i = 0; i < sar.length; i++) {
                int sariLength = sar[i].length();
                if (sariLength >= 2 && 
                    sar[i].charAt(0) == '"' && sar[i].charAt(sariLength - 1) == '"')
                    sar[i] = sar[i].substring(1, sariLength - 1);
            }
            StringArray sa = new StringArray(sar);

            //store values in the appropriate type of PrimitiveArray
            //dilemma: store unsigned values as if signed, or in larger data type?
            //   decision: for now, store uint16 and uint32 as int
            PrimitiveArray pa = null;
            int type = attribute.getType();
            if       (type == dods.dap.Attribute.FLOAT32) pa = new FloatArray(); 
            else if  (type == dods.dap.Attribute.FLOAT64) pa = new DoubleArray();
            else if  (type == dods.dap.Attribute.INT16 ||
                      type == dods.dap.Attribute.UINT16)  pa = new ShortArray();
            else if  (type == dods.dap.Attribute.INT32 ||
                      type == dods.dap.Attribute.UINT32)  pa = new IntArray();
            //ignore STRING, URL, UNKNOWN, etc. (keep as StringArray)

            //move the sa data into pa
            if (pa == null) {
                pa = sa;
            } else {
                //convert to other data type
                pa.append(sa);
            }

            //store name,pa in attributes
            attributes.set(name, pa);
        }
    }

    /**
     * This gets the values for one of the attributes for one of the variables
     * from the DAS.
     * But for more than 2 calls to this, it is more efficient to use
     * getAttributesFromOpendap.
     * This won't throw an exception.
     *
     * @param das the das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
     * @param variableName e.g. "lat".  Use "GLOBAL" for global attributes 
     *  (even though they are called different things in different files).
     * @param attributeName e.g., "actual_range"
     * @return the values associated with the attribute (array.length = 0 if not found)
     *    (e.g., {"even"} or {"0.", "360."}).     
     */
    public static String[] getAttributeValues(DAS das, String variableName, 
          String attributeName) {
        try {
            AttributeTable at = getAttributeTable(das, variableName);
            if (at == null) {
                String2.log("NOTE: OpendapHelper.getAttributeValues:\n" +
                    "  attributeTable not found for var=" + variableName);
                return new String[] {};
            }   

            dods.dap.Attribute attribute = at.getAttribute(attributeName);
            if (attribute == null) {
                String2.log("NOTE: OpendapHelper.getAttributeValues:\n" +
                    "  attribute not found for var=" + variableName + 
                    " attributeName=" + attributeName);
                return new String[] {};
            }   

            String[] sar = String2.toStringArray(
                String2.toArrayList(attribute.getValues()).toArray());
            //remove enclosing quotes from strings
            for (int i = 0; i < sar.length; i++) {
                int sariLength = sar[i].length();
                if (sariLength >= 2 && 
                    sar[i].charAt(0) == '"' && sar[i].charAt(sariLength - 1) == '"')
                    sar[i] = sar[i].substring(1, sariLength - 1);
            }
            return sar;
        } catch (Exception e) {
            String2.log("WARNING: OpendapHelper.getAttributeValues(\nvarName=" + 
                variableName + " attributeName=" + attributeName + "):" +
                MustBe.throwableToString(e));
            return new String[] {};
        }
    }

    /**
     * This gets the first value for the one of the attributes for one of the variables
     * from the DAS.
     *
     * @param das the das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
     * @param variableName e.g. "time".  Use "GLOBAL" for global attributes 
     *  (even though they are called different things in different files).
     * @param attributeName e.g., "units"
     * @return the first value value associated with the attribute, 
     *    represented as a String (or null if not found)
     *    e.g., "seconds since 1970-01-01"
     */
    public static String getAttributeValue(DAS das, String variableName, String attributeName) {
        String[] values = getAttributeValues(das, variableName, attributeName);
        return values.length == 0? null : values[0];
    }

    /**
     * Get the PrimitiveVector from an opendap query.
     * 
     * @param dConnect
     * @param query For an entire variable that is a DArray, use, for example, "?lat",
     *   or for a portion: "?lat[0:1:5]".
     *   For a portion of a DGrid, use, for example, "?ssta.ssta[23:1:23][642:1:742][339:1:439]".
     *   This should be already percent encoded as needed.
     * @return a PrimitiveVector with the data from the DArray returned by the query 
     * @throws Exception if trouble
     */
    public static PrimitiveVector getPrimitiveVector(DConnect dConnect, String query) 
            throws Exception {
        long time = System.currentTimeMillis();
        DataDDS dataDds = dConnect.getData(query, null);
        BaseType bt = (BaseType)dataDds.getVariables().nextElement(); //first element is always main array
        //bt.printVal(System.out, " ");
        DArray da = (DArray)bt; 
        if (verbose)
            String2.log("    OpendapHelper.getPrimitiveVector(" + query + 
                ") done. TIME=" + (System.currentTimeMillis() - time));
        return da.getPrimitiveVector();
    }

    /**
     * Get the PrimitiveArrays from an opendap query.
     * 
     * @param dConnect
     * @param query For an entire variable that is a DArray, use, for example, "?lat",
     *   or for a portion: "?lat[0:1:5]".
     *   For an entire dimension of a grid, use, for example, "?ssta.lat".
     *   For a portion of a DGrid, use, for example, "?ssta[23:1:23][642:1:742][339:1:439]",
     *   which returns 4 PrimitiveArrays: #0=main data, #1=dimension0, #2=dimension1, #3=dimension2.
     *   This should already percent encoded as needed.
     * @return an array of PrimitiveArrays with the requested data 
     * @throws Exception if trouble
     */
    public static PrimitiveArray[] getPrimitiveArrays(DConnect dConnect, String query) 
            throws Exception {
        long time = System.currentTimeMillis();
        if (verbose)
            String2.log("    OpendapHelper.getPrimitiveArrays " + query);
        DataDDS dataDds = dConnect.getData(query, null);
        if (verbose)
            String2.log("    OpendapHelper.getPrimitiveArrays done. TIME=" + 
                (System.currentTimeMillis() - time) +
                "\n      query=" + query);      
        BaseType bt = (BaseType)dataDds.getVariables().nextElement(); //first element is always main array
        if (bt instanceof DGrid) {
            ArrayList al = String2.toArrayList( ((DGrid)bt).getVariables() ); //enumeration -> arraylist
            PrimitiveArray paAr[] = new PrimitiveArray[al.size()];
            for (int i = 0; i < al.size(); i++)
                paAr[i] = getPrimitiveArray( ((DArray)al.get(i)).getPrimitiveVector() );
            return paAr;
        } else if (bt instanceof DArray) {
            DArray da = (DArray)bt; 
            return new PrimitiveArray[]{getPrimitiveArray(da.getPrimitiveVector())};
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bt.printVal(baos, " ");
            Test.error(ERROR + ": OpendapHelper.getPrimitiveArrays(" + query + 
                ")\nUnrecogized result type:" + baos);
        }
        return null;

    }

    /**
     * Get the values from 1+ DArrays (axis variables) in PrimitiveArrays.
     * 
     * @param dConnect
     * @param names the names of the DArray variables (e.g., axes).
     * @param axis0Constraint  e.g., "[12:22]", or ""
     * @return an array of PrimitiveArrays with the requested data 
     * @throws Exception if trouble
     */
    public static PrimitiveArray[] getAxisValues(DConnect dConnect, String[] names, 
            String axis0Constraint) throws Exception {
        long time = System.currentTimeMillis();
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            query.append(i == 0? '?' : ',');
            query.append(names[i]);
            if (i == 0) query.append(axis0Constraint);
        }
        DataDDS dataDds = dConnect.getData(query.toString(), null);
        if (verbose)
            String2.log("    OpendapHelper.getAxisValues done. TIME=" + 
                (System.currentTimeMillis() - time) +
                "\n      query=" + query);      
        PrimitiveArray pa[] = new PrimitiveArray[names.length];
        for (int i = 0; i < names.length; i++) {               
            DArray da = (DArray)dataDds.getVariable(names[i]); 
            pa[i] = getPrimitiveArray(da.getPrimitiveVector());
        }
        return pa;
    }

    /**
     * Get a PrimitiveArray result from an opendap query.
     * 
     * @param dConnect
     * @param query For an entire variable that is a DArray, use, for example, "?lat",
     *   or for a portion: "?lat[0:1:5]".
     *   For a portion of a DGrid, use, for example, "?ssta.ssta[23:1:23][642:1:742][339:1:439]".
     *   This should be already percentEncoded as needed.
     * @return a PrimitiveArray with the data from the DArray returned by the query 
     * @throws Exception if trouble
     */
    public static PrimitiveArray getPrimitiveArray(DConnect dConnect, String query) 
            throws Exception {
        return getPrimitiveArray(getPrimitiveVector(dConnect, query));
    }

    /**
     * Get a PrimitiveArray result from an opendap query.
     * 
     * @param dConnect
     * @param query For an entire variable that is a DArray, use, for example, "?lat",
     *   or for a portion: "?lat[0:1:5]".
     *   For a portion of a DGrid, use, for example, "?ssta.ssta[23:1:23][642:1:742][339:1:439]".
     *   This should be already percent encoded as needed.
     * @return a double[] with the data from the DArray returned by the query 
     * @throws Exception if trouble
     */
    public static double[] getDoubleArray(DConnect dConnect, String query) 
            throws Exception {
        return getDoubleArray(getPrimitiveVector(dConnect, query));
    }

    /**
     * This converts a PrimitiveVector to a PrimitiveArray.
     *
     * @param pv
     * @return the corresponding PrimitiveArray
     * @throws Exception if trouble (e.g., pv is null)
     */
    public static PrimitiveArray getPrimitiveArray(PrimitiveVector pv) throws Exception {
        Test.ensureNotNull(pv, "pv is null");

        //new method
        Object intSto = pv.getInternalStorage();
        //String2.log("pv internalStorage=" + intSto);
        if (intSto instanceof BaseType[]) {
            BaseType bta[] = (BaseType[])intSto;
            int n = bta.length;
            if (n > 0 && bta[0] instanceof DString) {
                //String2.log("  pv internalStorage is BaseType[] of DString.");
                String sa[] = new String[n];
                for (int i = 0; i < n; i++) 
                    sa[i] = ((DString)bta[i]).getValue();
                intSto = sa;
            }
        }
        PrimitiveArray pa = PrimitiveArray.factory(intSto);
        if (pv.getLength() > pa.size()) {
            String2.log("\n!!!OpendapHelper.getPrimitiveArray pvLength=" + 
                pv.getLength() + " > paSize=" + pa.size() + "\n");
            pa.removeRange(pv.getLength(), pa.size());
        }
        return pa;
        /*
        int n = pv.getLength();
        if (pv instanceof Float32PrimitiveVector) {
            float a[] = new float[n];
            Float32PrimitiveVector tpv = (Float32PrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                a[i] = tpv.getValue(i);
            return new FloatArray(a);
        }
        if (pv instanceof Float64PrimitiveVector) {
            double a[] = new double[n];
            Float64PrimitiveVector tpv = (Float64PrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                a[i] = tpv.getValue(i);
            return new DoubleArray(a);
        } 
        if (pv instanceof BytePrimitiveVector) {
            byte a[] = new byte[n];
            BytePrimitiveVector tpv = (BytePrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                a[i] = tpv.getValue(i);
            return new ByteArray(a);
        } 
        if (pv instanceof Int16PrimitiveVector) {
            short a[] = new short[n];
            Int16PrimitiveVector tpv = (Int16PrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                a[i] = tpv.getValue(i);
            return new ShortArray(a);
        } 
        if (pv instanceof Int32PrimitiveVector) {
            int a[] = new int[n];
            Int32PrimitiveVector tpv = (Int32PrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                a[i] = tpv.getValue(i);
            return new IntArray(a);
        } 
        throw new Exception(ERROR + ": The PrimitiveVector is not numeric (" + pv + ").");
        */

    }

    /**
     * This converts a PrimitiveArray to a PrimitiveVector.
     *
     * @param name
     * @param pa
     * @return the corresponding PrimitiveVector
     * @throws Exception if trouble
     */
    public static PrimitiveVector getPrimitiveVector(String name, PrimitiveArray pa) 
            throws Exception {
        PrimitiveVector pv;
        if      (pa instanceof DoubleArray) pv = new Float64PrimitiveVector(new DFloat64(name));
        else if (pa instanceof FloatArray)  pv = new Float32PrimitiveVector(new DFloat32(name));
        else if (pa instanceof IntArray)    pv = new Int32PrimitiveVector(  new DInt32(name));
        else if (pa instanceof ShortArray)  pv = new Int16PrimitiveVector(  new DInt16(name));
        else if (pa instanceof ByteArray)   pv = new BytePrimitiveVector(   new DByte(name));
        else throw new Exception(ERROR + "in OpendapHelper.getPrimitiveVector: The PrimitiveArray type=" + 
            pa.elementClassString() + " is not supported.");

        pv.setInternalStorage(pa.toObjectArray());
        return pv;
    }

    /**
     * This returns the atomic-type String (e.g., Int16) corresponding to a 
     * PrimitiveArray's type.
     *
     * <p>Some Java types don't have exact matches. The closest match is returned,
     * e.g., short and char become int, long becomes double
     *
     * @param c the Java type class e.g., float.class
     * @return the corresponding atomic-type String
     * @throws Exception if trouble
     */
    public static String getAtomicType(Class c) throws Exception {
        if (c == long.class ||   //imperfect; there will be loss of precision
            c == double.class) return "Float64";
        if (c == float.class)  return "Float32";
        if (c == int.class)    return "Int32";
        if (c == short.class ||
            c == char.class)   return "Int16";
        if (c == byte.class)   return "Byte";
        if (c == String.class) return "String";
        throw new Exception(ERROR + "in OpendapHelper.getAtomicType: The classType=" + 
            PrimitiveArray.elementClassToString(c) + " is not supported.");
    }

    /**
     * This writes the attributes to the writer in the opendap DAS format.
     * E.g., <pre>
    altitude {
        Float32 actual_range 0.0, 0.0;
        Int32 fraction_digits 0;
        String long_name "Altitude";
    }
     * </pre>
     * @param varName the variable's name.
     *  For global attributes, ncBrowse and netcdf-java treat "NC_GLOBAL" as special case. (I had used "GLOBAL".)
     * @param attributes
     * @param writer
     * @param encodeAsHTML if true, characters like &lt; are converted to their 
     *    character entities and lines wrapped with \n if greater than 78 chars.
     * @throws Exception if trouble
     */
    public static void writeToDAS(String varName, Attributes attributes,
        Writer writer, boolean encodeAsHTML) throws Exception {

        writer.append(dasToStringBuilder(varName, attributes, encodeAsHTML));
    }

    /**
     * This writes the attributes to the StringBuilder in the opendap DAS format.
     * E.g., <pre>
    altitude {
        Float32 actual_range 0.0, 0.0;
        Int32 fraction_digits 0;
        String long_name "Altitude";
    }
     * </pre>
     * @param varName the variable's name
     * @param attributes
     * @param encodeAsHTML if true, characters like &lt; are converted to their 
     *    character entities and lines wrapped with \n if greater than 78 chars.
     * @throws Exception if trouble
     */
    public static StringBuilder dasToStringBuilder(String varName, Attributes attributes,
        boolean encodeAsHTML) throws Exception {
        StringBuilder sb = new StringBuilder();

        //see EOL definition for comments about it
        sb.append("  " + XML.encodeAsHTML(varName, encodeAsHTML) + " {" + EOL); 
        String names[] = attributes.getNames();
        for (int ni = 0; ni < names.length; ni++) {
            PrimitiveArray pa = attributes.get(names[ni]);
            Class et = pa.elementClass();
            sb.append(XML.encodeAsHTML("    " + getAtomicType(et) + " " + names[ni] + " ", encodeAsHTML));
            int paSize = pa.size();
            if (et == String.class) {
                //enquote, and replace internal quotes with \"
                for (int pai = 0; pai < paSize; pai++) {
                    String ts = pa.getString(pai);
                    if (encodeAsHTML) {
                        ts = String2.noLongLines(ts, 78, "");
                        if (ts.indexOf('\n') >= 0)
                            sb.append('\n'); //start on new line, so first line isn't super long
                    }
                    sb.append(XML.encodeAsHTML(
                        "\"" + String2.replaceAll(ts, "\"", "\\\"") + "\"", encodeAsHTML));
                    sb.append(pai < paSize - 1 ? ", " : "");
                }
            } else if (et == double.class) {
                //the spec says must be like Ansi C printf, %g format, precision=6
                //I couldn't get Jikes to compile String.format.
                for (int pai = 0; pai < paSize; pai++) {
                    String ts = "" + pa.getDouble(pai);
                    ts = String2.replaceAll(ts, "E-", "e-"); //do first
                    ts = String2.replaceAll(ts, "E", "e+");
                    sb.append(ts +
                        //String.format("%g.6", (Object)new Double(pa.getDouble(pai))) + 
                        (pai < paSize - 1 ? ", " : ""));  
                }
            } else if (et == float.class) {
                for (int pai = 0; pai < paSize; pai++) 
                    sb.append(pa.getFloat(pai) + 
                        (pai < paSize - 1 ? ", " : ""));  
            } else {
                sb.append(pa.toString());
            }
            sb.append(";" + EOL);
        }
        sb.append("  }" + EOL);
        return sb;
    }

    /**
     * This converts a numeric primitiveVector to double[].
     *
     * @param pv
     * @return the corresponding double[]
     * @throws Exception if trouble
     */
    public static double[] getDoubleArray(PrimitiveVector pv) throws Exception {
        Test.ensureNotNull(pv, "pv is null");
        int n = pv.getLength();
        double da[] = new double[n];
        if (pv instanceof Float32PrimitiveVector) {
            Float32PrimitiveVector tpv = (Float32PrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                da[i] = Math2.niceDouble(tpv.getValue(i), 7);
        } else if (pv instanceof Float64PrimitiveVector) {
             Float64PrimitiveVector tpv = (Float64PrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                da[i] = tpv.getValue(i);
        } else if (pv instanceof BytePrimitiveVector) {
            BytePrimitiveVector tpv = (BytePrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                da[i] = tpv.getValue(i);
        } else if (pv instanceof Int16PrimitiveVector) {
            Int16PrimitiveVector tpv = (Int16PrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                da[i] = tpv.getValue(i);
        } else if (pv instanceof Int32PrimitiveVector) {
            Int32PrimitiveVector tpv = (Int32PrimitiveVector)pv;
            for (int i = 0; i < n; i++)
                da[i] = tpv.getValue(i);
        } else {
            throw new Exception(ERROR + ": The PrimitiveVector is not numeric (" + pv + ").");
        }

        return da;
    }


    /**
     * This gets a value from a numeric primitiveVector and rounds it to an int.
     *
     * @param pv
     * @param index
     * @return the int value from pv.get(index)
     * @throws Exception if trouble
     */
    public static int getInt(PrimitiveVector pv, int index) throws Exception {
        Test.ensureNotNull(pv, "pv is null");
        if (pv instanceof BytePrimitiveVector) 
            return ((BytePrimitiveVector)pv).getValue(index);
        if (pv instanceof Int16PrimitiveVector) 
            return ((Int16PrimitiveVector)pv).getValue(index);
        if (pv instanceof Int32PrimitiveVector) 
            return ((Int32PrimitiveVector)pv).getValue(index);
        if (pv instanceof Float32PrimitiveVector) 
            return Math2.roundToInt(((Float32PrimitiveVector)pv).getValue(index));
        if (pv instanceof Float64PrimitiveVector) 
            return Math2.roundToInt(((Float64PrimitiveVector)pv).getValue(index));
        throw new Exception(ERROR + ": The PrimitiveVector is not numeric (" + pv + ").");
    }

    /**
     * This gets a value from a numeric primitiveVector and converts it to double.
     *
     * @param pv
     * @param index
     * @return the double value from pv.get(index)
     * @throws Exception if trouble
     */
    public static double getDouble(PrimitiveVector pv, int index) throws Exception {
        Test.ensureNotNull(pv, "pv is null");
        if (pv instanceof Float32PrimitiveVector) 
            return ((Float32PrimitiveVector)pv).getValue(index);
        if (pv instanceof Float64PrimitiveVector) 
            return ((Float64PrimitiveVector)pv).getValue(index);
        if (pv instanceof BytePrimitiveVector) 
            return ((BytePrimitiveVector)pv).getValue(index);
        if (pv instanceof Int16PrimitiveVector) 
            return ((Int16PrimitiveVector)pv).getValue(index);
        if (pv instanceof Int32PrimitiveVector) 
            return ((Int32PrimitiveVector)pv).getValue(index);
        throw new Exception(ERROR + ": The PrimitiveVector is not numeric (" + pv + ").");
    }

    /**
     * This gets a value from a primitiveVector and converts it to String.
     *
     * @param pv
     * @param index
     * @return the String value from pv.get(index)
     * @throws Exception if trouble
     */
    public static String getString(PrimitiveVector pv, int index) throws Exception {
        Test.ensureNotNull(pv, "pv is null");
        if (pv instanceof Float32PrimitiveVector) 
            return "" + ((Float32PrimitiveVector)pv).getValue(index);
        if (pv instanceof Float64PrimitiveVector) 
            return "" + ((Float64PrimitiveVector)pv).getValue(index);
        if (pv instanceof BytePrimitiveVector) 
            return "" + ((BytePrimitiveVector)pv).getValue(index);
        if (pv instanceof Int16PrimitiveVector) 
            return "" + ((Int16PrimitiveVector)pv).getValue(index);
        if (pv instanceof Int32PrimitiveVector) 
            return "" + ((Int32PrimitiveVector)pv).getValue(index);
        //if (pv instanceof StringPrimitiveVector)
        //    return ((StringPrimitiveVector)pv).getValue(index);
        if (pv instanceof BooleanPrimitiveVector)
            return ((BooleanPrimitiveVector)pv).getValue(index)? "true" : "false";
        throw new Exception(ERROR + ": Unknown PrimitiveVector type (" + pv + ").");
    }

    /**
     * This gets a value from a numeric BaseType and rounds it to an int.
     *
     * @param bt
     * @return the int value from bt.get(index)
     * @throws Exception if trouble
     */
    public static int getInt(BaseType bt) throws Exception {
        Test.ensureNotNull(bt, "bt is null");
        if (bt instanceof DByte)     return ((DByte)bt).getValue();
        if (bt instanceof DInt16)    return ((DInt16)bt).getValue();
        if (bt instanceof DInt32)    return ((DInt32)bt).getValue();
        if (bt instanceof DFloat32)  return Math2.roundToInt(((DFloat32)bt).getValue());
        if (bt instanceof DFloat64)  return Math2.roundToInt(((DFloat64)bt).getValue());
        throw new Exception(ERROR + ": The BaseType is not numeric (" + bt + ").");
    }

    /**
     * This gets a value from a numeric BaseType and converts it to double.
     *
     * @param bt
     * @return the double value from bt.get(index)
     * @throws Exception if trouble
     */
    public static double getDouble(BaseType bt) throws Exception {
        Test.ensureNotNull(bt, "bt is null");
        if (bt instanceof DFloat32)  return ((DFloat32)bt).getValue();
        if (bt instanceof DFloat64)  return ((DFloat64)bt).getValue();
        if (bt instanceof DByte)     return ((DByte)bt).getValue();
        if (bt instanceof DInt16)    return ((DInt16)bt).getValue();
        if (bt instanceof DInt32)    return ((DInt32)bt).getValue();
        throw new Exception(ERROR + ": The BaseType is not numeric (" + bt + ").");
    }

    /**
     * This gets a value from a BaseType and converts it to a String.
     *
     * @param bt
     * @return the double value from bt.get(index)
     * @throws Exception if trouble
     */
    public static String getString(BaseType bt, int index) throws Exception {
        Test.ensureNotNull(bt, "bt is null");
        if (bt instanceof DFloat32)  return "" + ((DFloat32)bt).getValue();
        if (bt instanceof DFloat64)  return "" + ((DFloat64)bt).getValue();
        if (bt instanceof DByte)     return "" + ((DByte)bt).getValue();
        if (bt instanceof DInt16)    return "" + ((DInt16)bt).getValue();
        if (bt instanceof DInt32)    return "" + ((DInt32)bt).getValue();
        if (bt instanceof DString)   return ((DString)bt).getValue();
        if (bt instanceof DBoolean)  return ((DBoolean)bt).getValue()? "true" : "false";
        throw new Exception(ERROR + ": Unknown BaseType (" + bt + ").");
    }

    /**
     * This returns the PrimitiveArray elementClass of a PrimitiveVector.
     *
     * @param pv 
     * @return the PrimitiveArray elementClass of this BaseType.
     * @throws Exception if trouble
     */
    public static Class getElementClass(PrimitiveVector pv) throws Exception {
        Test.ensureNotNull(pv, "pv is null");
        if (pv instanceof Float32PrimitiveVector)  return float.class;
        if (pv instanceof Float64PrimitiveVector)  return double.class;
        if (pv instanceof BytePrimitiveVector)     return byte.class;
        if (pv instanceof Int16PrimitiveVector)    return short.class;
        if (pv instanceof Int32PrimitiveVector)    return int.class;
        if (pv instanceof BaseTypePrimitiveVector) return String.class; //???
        if (pv instanceof BooleanPrimitiveVector)  return boolean.class;
        throw new Exception(ERROR + ": Unknown PrimitiveVector (" + pv + ").");
    }

    /**
     * This tests the methods in this class.
     * Currently, all tests are done in other classes (at a higher level).
     */
    public static void test() {
        String2.log("\n*** OpendapHelper.test...");

        //done
        String2.log("\n***** Opendap.test finished successfully");
        Math2.incgc(2000);
    } 



}
