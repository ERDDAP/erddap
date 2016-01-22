/* 
 * OpendapHelper Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
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
import java.util.HashMap;
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
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /**
     * Set this to true (by calling debug=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean debug = false;

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

    public static int DEFAULT_TIMEOUT = 120000; //2 minutes in millis

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
            if (attribute.isContainer()) {
                //process an attribute that isContainer by flattening it (name_subname=...)
                //http://dm1.caricoos.org/thredds/dodsC/content/wrf_archive/wrfout_d01_2009-09-25_12_00_00.nc.das
                //this works recursively, so should handle containers of containers of ...
                Attributes tAttributes = new Attributes();
                getAttributes(attribute.getContainer(), tAttributes);
                String tNames[] = tAttributes.getNames();
                String startName = name.trim() + "_";
                int ntNames = tNames.length;
                for (int tni = 0; tni < ntNames; tni++)
                    attributes.add(startName + tNames[tni].trim(), tAttributes.get(tNames[tni]));

            } else {
                //process a simple attribute
                String[] sar = String2.toStringArray(String2.toArrayList(attribute.getValues()).toArray());

                //remove enclosing quotes from strings
                for (int i = 0; i < sar.length; i++) {
                    int sariLength = sar[i].length();
                    if (sariLength >= 2 && 
                        sar[i].charAt(0) == '"' && sar[i].charAt(sariLength - 1) == '"')
                        sar[i] = String2.fromJson(sar[i]);
                }
                StringArray sa = new StringArray(sar);

                //store values in the appropriate type of PrimitiveArray
                //dilemma: store unsigned values as if signed, or in larger data type?
                //   decision: for now, store uint16 and uint32 as int
                PrimitiveArray pa = null;
                int type = attribute.getType();
                if       (type == dods.dap.Attribute.FLOAT32) pa = new FloatArray(); 
                else if  (type == dods.dap.Attribute.FLOAT64) pa = new DoubleArray();
                else if  (type == dods.dap.Attribute.INT32 ||
                          type == dods.dap.Attribute.UINT32)  pa = new IntArray();
                else if  (type == dods.dap.Attribute.INT16 ||
                          type == dods.dap.Attribute.UINT16)  pa = new ShortArray();
                else if  (type == dods.dap.Attribute.BYTE)    pa = new ByteArray();
                //ignore STRING, URL, UNKNOWN, etc. (keep as StringArray)

                //move the sa data into pa
                if (pa == null) {
                    //pa will be the StringArray
                    pa = sa;
                    //trim first part of first string and end of last string
                    if (sa.size() == 1) {
                        sa.set(0, sa.get(0).trim());
                    } else if (sa.size() > 1) {
                        sa.set(0, String2.trimStart(sa.get(0)));
                        sa.set(sa.size() - 1, String2.trimEnd(sa.get(sa.size() - 1)));
                    }

                } else {
                    //convert to other data type
                    pa.append(sa);
                }

                //store name,pa in attributes
                attributes.set(name.trim(), pa);
            }
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

            String[] sar = String2.toStringArray(String2.toArrayList(attribute.getValues()).toArray());
            //remove enclosing quotes from strings
            for (int i = 0; i < sar.length; i++) {
                int sariLength = sar[i].length();
                if (sariLength >= 2 && 
                    sar[i].charAt(0) == '"' && sar[i].charAt(sariLength - 1) == '"')
                    sar[i] = String2.fromJson(sar[i]);
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
     * Given a projection constraint ("[0:5][1][0:2:10]"), this 
     * returns an array with start,stride,stop for all the dimensions.
     * 
     * @param p   a projection e.g., [0:5][1][0:2:10]
     * @return an array with start,stride,stop for all the dimensions.
     *     This returns [0] if projection is null or "".
     *     This throws runtime exception if invalid syntax.
     */
    public static int[] parseStartStrideStop(String p) {
        if (p == null)
            return new int[0];
        int pLength = p.length();
        int po = 0;
        IntArray ia = new IntArray();
        String beginError = String2.ERROR + " parsing OPENDAP constraint=\"" + p + "\": ";
        while (po < pLength) {
            if (p.charAt(po) != '[')
                throw new RuntimeException(beginError + 
                    "'[' expected at projection position #" + po);
            po++;
            int colonPo1 = p.indexOf(':', po);
            if (colonPo1 < 0) 
                colonPo1 = pLength;
            int colonPo2 = colonPo1 == pLength? pLength : p.indexOf(':', colonPo1+1);
            if (colonPo2 < 0)
                colonPo2 = pLength;
            int endPo    = p.indexOf(']', po);
            if (endPo < 0)
                throw new RuntimeException(beginError +
                    "End ']' not found.");

            if (colonPo1 > endPo) {
                // [4]
                int start = Integer.parseInt(p.substring(po, endPo));
                if (start < 0)
                    throw new RuntimeException(beginError + 
                        "Negative number=" + start + " at projection position #" + po);
                ia.add(start); ia.add(1); ia.add(start);

            } else if (colonPo2 > endPo) {
                // [3:4]
                int start = Integer.parseInt(p.substring(po, colonPo1));
                if (start < 0)
                    throw new RuntimeException(beginError + 
                        "Negative number=" + start + " at projection position #" + po);
                int stop = Integer.parseInt(p.substring(colonPo1 + 1, endPo));
                if (stop < 0)
                    throw new RuntimeException(beginError + 
                        "Negative number=" + stop + " at projection position #" + (colonPo1 + 1));
                if (start > stop)
                    throw new RuntimeException(beginError + 
                        "start=" + start + " must be less than or equal to stop=" + stop);
                ia.add(start); ia.add(1); ia.add(stop);

            } else {
                // [3:4:5]
                int start = Integer.parseInt(p.substring(po, colonPo1));
                if (start < 0)
                    throw new RuntimeException(beginError + 
                        "Negative number=" + start + " at projection position #" + po);
                int stride = Integer.parseInt(p.substring(colonPo1 + 1, colonPo2));
                if (stride < 0)
                    throw new RuntimeException(beginError + 
                        "Negative number=" + stride + " at projection position #" + (colonPo1 + 1));
                int stop = Integer.parseInt(p.substring(colonPo2 + 1, endPo));
                if (stop < 0)
                    throw new RuntimeException(beginError + 
                        "Negative number=" + stop + " at projection position #" + (colonPo2 + 1));
                if (start > stop)
                    throw new RuntimeException(beginError + 
                        "start=" + start + " must be less than or equal to stop=" + stop);
                ia.add(start); ia.add(stride); ia.add(stop);
            }
            po = endPo + 1;
        }
        return ia.toArray();
    }

    /**
     * Given start, stride, stop, this returns the actual number of points that will be found.
     *
     * @param start 
     * @param stride  (must be >= 1)
     * @param stop 
     * @return the actual number of points that will be found.
     */
    public static int calculateNValues(int start, int stride, int stop) {
        if (start > stop)
            throw new RuntimeException( 
                "start=" + start + " must be less than or equal to stop=" + stop);
        if (stride < 1)
            throw new RuntimeException( 
                "stride=" + stride + " must be greater than 0");

        return 1 + (stop - start) / stride;
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
     * Get the PrimitiveArrays from the first var from an opendap query.
     * 
     * @param dConnect
     * @param query For an entire variable that is a DArray, use, for example, "?lat",
     *   or for a portion: "?lat[0:1:5]".
     *   For an entire dimension of a grid, use, for example, "?ssta.lat".
     *   For a portion of a DGrid, use, for example, "?ssta[23:1:23][642:1:742][339:1:439]",
     *   which returns 4 PrimitiveArrays: #0=main data, #1=dimension0, #2=dimension1, #3=dimension2.
     *   This should already percent encoded as needed.
     * @return an array of PrimitiveArrays with the requested data from the first var in the query
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
                (System.currentTimeMillis() - time));      
        BaseType bt = (BaseType)dataDds.getVariables().nextElement(); //first element is always main array
        try {
            return getPrimitiveArrays(bt);
        } catch (Exception e) {
            throw new RuntimeException(String2.ERROR + " in getPrimitiveArrays query=" + query + "\n" +
                e.getMessage());
        }

    }

    /**
     * Get the PrimitiveArrays from a BaseType.
     * 
     * @param baseType
     * @return an array of PrimitiveArrays with the data from the baseType
     * @throws Exception if trouble
     */
    public static PrimitiveArray[] getPrimitiveArrays(BaseType baseType) throws Exception {
        //String2.log("    baseType=" + baseType.getTypeName());
        if (baseType instanceof DGrid) {
            ArrayList al = String2.toArrayList( ((DGrid)baseType).getVariables() ); //enumeration -> arraylist
            PrimitiveArray paAr[] = new PrimitiveArray[al.size()];
            for (int i = 0; i < al.size(); i++)
                paAr[i] = getPrimitiveArray( ((DArray)al.get(i)).getPrimitiveVector() );
            return paAr;
        } else if (baseType instanceof DArray) {
            DArray da = (DArray)baseType; 
            return new PrimitiveArray[]{getPrimitiveArray(da.getPrimitiveVector())};
        } else if (baseType instanceof DVector) {
            return new PrimitiveArray[]{getPrimitiveArray(baseType.newPrimitiveVector())};
        } else if (baseType instanceof DFloat64) {
            return new PrimitiveArray[]{
                new DoubleArray(new double[]{((DFloat64)baseType).getValue()})}; 
        } else if (baseType instanceof DFloat32) {
            return new PrimitiveArray[]{
                new FloatArray( new float[] {((DFloat32)baseType).getValue()})}; 
        } else if (baseType instanceof DInt32)   {
            return new PrimitiveArray[]{
                new IntArray(   new int[]   {((DInt32)  baseType).getValue()})}; 
        } else if (baseType instanceof DInt16)   {
            return new PrimitiveArray[]{
                new ShortArray( new short[] {((DInt16)  baseType).getValue()})}; 
        } else if (baseType instanceof DByte)    {
            return new PrimitiveArray[]{
                new ByteArray(  new byte[]  {((DByte)   baseType).getValue()})}; 
        } else if (baseType instanceof DBoolean) {
            return new PrimitiveArray[]{
                new ByteArray(  new byte[]  {(byte)(((DBoolean)baseType).getValue()? 1 : 0)})}; 
        } else if (baseType instanceof DString)  {
String2.log("    baseType is DString=" + String2.toJson(((DString)baseType).getValue()));
            return new PrimitiveArray[]{
                new StringArray(new String[]{((DString)baseType).getValue()})}; 
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baseType.printVal(baos, " ");
            throw new SimpleException(String2.ERROR + ": Unrecogized baseType:" +
                baos);
        }
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
        throw new Exception(String2.ERROR + ": The PrimitiveVector is not numeric (" + pv + ").");
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
        else throw new Exception(String2.ERROR + "in OpendapHelper.getPrimitiveVector: The PrimitiveArray type=" + 
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
        throw new Exception(String2.ERROR + "in OpendapHelper.getAtomicType: The classType=" + 
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
                        ts = String2.noLongLinesAtSpace(ts, 78, "");
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
            throw new Exception(String2.ERROR + ": The PrimitiveVector is not numeric (" + pv + ").");
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
        throw new Exception(String2.ERROR + ": The PrimitiveVector is not numeric (" + pv + ").");
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
        throw new Exception(String2.ERROR + ": The PrimitiveVector is not numeric (" + pv + ").");
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
        throw new Exception(String2.ERROR + ": Unknown PrimitiveVector type (" + pv + ").");
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
        throw new Exception(String2.ERROR + ": The BaseType is not numeric (" + bt + ").");
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
        throw new Exception(String2.ERROR + ": The BaseType is not numeric (" + bt + ").");
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
        throw new Exception(String2.ERROR + ": Unknown BaseType (" + bt + ").");
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
        throw new Exception(String2.ERROR + ": Unknown PrimitiveVector (" + pv + ").");
    }

    /** 
     * This finds the vars with the most dimensions.
     * If there are more than 1 groups with the same number of dimensions,
     * but different dimensions, the first group will be be found.
     *
     * <p>Currently, this won't find variables in a sequence.
     *
     * @param dds
     * @return String varNames[]
     * @throws Exception if trouble
     */
    public static String[] findVarsWithSharedDimensions(DDS dds) 
        throws Exception {
  
        Enumeration en = dds.getVariables();
        StringArray dimNames = new StringArray();
        StringArray varNames = new StringArray(); //vars with same dimNames
        while (en.hasMoreElements()) {
            BaseType baseType = (BaseType)en.nextElement();
            DArray dArray;
            if (baseType instanceof DGrid) {
                //dGrid has main dArray + dimensions
                DGrid dGrid = (DGrid)baseType;
                dArray = (DArray)dGrid.getVar(0);                    
            } else if (baseType instanceof DArray) {
                //dArray is usually 1 dim, but may be multidimensional
                dArray = (DArray)baseType;
            } else {
                continue;
            }
            int nDim = dArray.numDimensions();
            //I'm confused. See 'flag' in first test of this method -- no stringLength dim!
            //if (getElementClass(dArray.getPrimitiveVector()) == String.class)
            //    nDim--; 
            if (nDim == 0 || nDim < dimNames.size()) 
                continue;
            if (nDim > dimNames.size()) {
                //switch to this set of dims
                varNames.clear();
                varNames.add(baseType.getName());
                dimNames.clear();
                for (int d = 0; d < nDim; d++) 
                    dimNames.add(dArray.getDimension(d).getName());
            } else { 
                //nDim == dimNames.size() and it is >0
                //does this var shar the same dims?                    
                boolean allSame = true;
                for (int d = 0; d < nDim; d++) {
                    if (!dArray.getDimension(d).getName().equals(dimNames.get(d))) {
                        allSame = false;
                        break;
                    }
                }
                if (allSame)
                    varNames.add(baseType.getName());
            }
        }
        return varNames.toArray();
    }

    /** This tests findVarsWithSharedDimensions. */
    public static void testFindVarsWithSharedDimensions() throws Throwable {
        String2.log("\n\n*** OpendapHelper.findVarsWithSharedDimensions");
        String expected, results;      
        DConnect dConnect;
        DDS dds;

        /*
        //test of Sequence DAP dataset        
        String2.log("\n*** test of Sequence DAP dataset");
        String sequenceUrl = "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecMoc1";
        dConnect = new DConnect(sequenceUrl, true, 1, 1);
        dds = dConnect.getDDS(DEFAULT_TIMEOUT);
        results = String2.toCSSVString(findVarsWithSharedDimensions(dds));
        expected = 
"zztop";
        Test.ensureEqual(results, expected, "results=" + results);
        */


        //test of DArray DAP dataset
        String dArrayUrl = "http://coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
        String2.log("\n*** test of DArray DAP dataset\n" + dArrayUrl);
        try {
        dConnect = new DConnect(dArrayUrl, true, 1, 1);
        dds = dConnect.getDDS(DEFAULT_TIMEOUT);
        results = String2.toCSSVString(findVarsWithSharedDimensions(dds));
        expected = 
"time, lat, lon, PL_HD, PL_CRS, DIR, PL_WDIR, PL_SPD, SPD, PL_WSPD, P, T, RH, date, time_of_day, flag";
        Test.ensureEqual(results, expected, "results=" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(
                "\nUnexpected error (server timed out 2013-10-24):\n" +
                MustBe.throwableToString(t)); 
        }


        //***** test of DGrid DAP dataset
        String2.log("\n*** test of DGrid DAP dataset");
        String dGridUrl = "http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday";
        dConnect = new DConnect(dGridUrl, true, 1, 1);
        dds = dConnect.getDDS(DEFAULT_TIMEOUT);
        results = String2.toCSSVString(findVarsWithSharedDimensions(dds));
        expected = "x_wind, y_wind";
        Test.ensureEqual(results, expected, "results=" + results);


        /* */
        String2.log("\n*** OpendapHelper.findVarsWithSharedDimensions finished.");

    }

    /**
     * This tests if baseType is an instanceof a 
     *   scalar (DBoolean, DByte, DFloat32, DFloat64, DInt16, DInt32, DString) 
     *   and multidimensional (DGrid, DArray) variable.
     *
     * @param baseType
     * @return true or false
     */
    public static boolean instanceofScalarOrMultiDimVar(BaseType baseType) {
        return  baseType instanceof DGrid    ||
                baseType instanceof DArray   ||
                baseType instanceof DString  ||
                baseType instanceof DByte    ||
                baseType instanceof DInt16   ||
                baseType instanceof DInt32   ||
                baseType instanceof DFloat32 ||
                baseType instanceof DFloat64 ||
                baseType instanceof DBoolean;
    }

    /** 
     * This finds all 
     *   scalar (DBoolean, DByte, DFloat32, DFloat64, DInt16, DInt32, DString) 
     *   and multidimensional (DGrid, DArray) variables.
     * This won't find DVectors, DLists, DStructures or DSequences.
     *
     * <p>Currently, this won't find variables in a sequence.
     *
     * @param dds
     * @return String varNames[]
     * @throws Exception if trouble
     */
    public static String[] findAllScalarOrMultiDimVars(DDS dds) 
        throws Exception {
  
        Enumeration en = dds.getVariables();
        StringArray dimNames = new StringArray();
        StringArray varNames = new StringArray(); //vars with same dimNames
        while (en.hasMoreElements()) {
            BaseType baseType = (BaseType)en.nextElement();
            if (instanceofScalarOrMultiDimVar(baseType))
                varNames.add(baseType.getName());
        }
        return varNames.toArray();
    }

    /** This tests findAllVars. */
    public static void testFindAllScalarOrMultiDimVars() throws Throwable {
        String2.log("\n\n*** OpendapHelper.findAllScalarOrMultiDimVars");
        String expected, results;      
        DConnect dConnect;
        DDS dds;
        String url;

        /*
        //test of Sequence DAP dataset        
        String2.log("\n*** test of Sequence DAP dataset");
        url = "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecMoc1";
        dConnect = new DConnect(url, true, 1, 1);
        dds = dConnect.getDDS(DEFAULT_TIMEOUT);
        results = String2.toCSSVString(findVarsWithSharedDimensions(dds));
        expected = 
"zztop";
        Test.ensureEqual(results, expected, "results=" + results);
        */


        //test of DArray DAP dataset
        url = "http://coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
        String2.log("\n*** test of DArray DAP dataset\n" + url);
        try {
            dConnect = new DConnect(url, true, 1, 1);
            dds = dConnect.getDDS(DEFAULT_TIMEOUT);
            results = String2.toCSSVString(findAllScalarOrMultiDimVars(dds));
            expected = 
"time, lat, lon, PL_HD, PL_CRS, DIR, PL_WDIR, PL_SPD, SPD, PL_WSPD, P, T, RH, date, time_of_day, flag, history";
            Test.ensureEqual(results, expected, "results=" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(
                "\nUnexpected error (server timed out 2013-10-24):\n" +
                MustBe.throwableToString(t)); 
        }


        //***** test of DGrid DAP dataset
        String2.log("\n*** test of DGrid DAP dataset");
        url = "http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday";
        dConnect = new DConnect(url, true, 1, 1);
        dds = dConnect.getDDS(DEFAULT_TIMEOUT);
        results = String2.toCSSVString(findAllScalarOrMultiDimVars(dds));
        expected = 
"time, altitude, latitude, longitude, x_wind, y_wind";
        Test.ensureEqual(results, expected, "results=" + results);

        //***** test of NODC template dataset
        String2.log("\n*** test of NODC template dataset");
        url = "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/timeSeries/BodegaMarineLabBuoyCombined.nc";
        dConnect = new DConnect(url, true, 1, 1);
        dds = dConnect.getDDS(DEFAULT_TIMEOUT);
        results = String2.toCSSVString(findAllScalarOrMultiDimVars(dds));
        expected = 
"time, lat, lon, alt, station_name, temperature, salinity, density, conductivity, " +
"turbidity, fluorescence, platform1, temperature_qc, salinity_qc, density_qc, " +
"conductivity_qc, turbidity_qc, fluorescence_qc, instrument1, instrument2, " +
"instrument3, ht_wgs84, ht_mllw, crs";
        Test.ensureEqual(results, expected, "results=" + results);

        //***** test of sequence dataset  (no vars should be found
        String2.log("\n*** test of sequence dataset");
        url = "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdCAMarCatLY";
        dConnect = new DConnect(url, true, 1, 1);
        dds = dConnect.getDDS(DEFAULT_TIMEOUT);
        results = String2.toCSSVString(findAllScalarOrMultiDimVars(dds));
        expected = "";
        Test.ensureEqual(results, expected, "results=" + results);


        /* */
        String2.log("\n*** OpendapHelper.findAllScalarOrMultiDimVars finished.");

    }



    /**
     * Get all of the data from allScalarOrMultiDimVars in an Opendap dataset
     * and save in .nc file.
     *
     * @param dapUrl       the base DAP URL
     * @param fullFileName the complete name (dir+name+extension) for the .nc file
     */
    public static void allDapToNc(String dapUrl, String fullFileName) throws Throwable {


        String beginError = "OpendapHelper.allDapToNc" +
            "\n  url=" + dapUrl + 
            "\n  file=" + fullFileName;
        if (verbose) String2.log(beginError); 
        beginError = String2.ERROR + " in " + beginError + "\n"; 
        long time = System.currentTimeMillis();

        //delete any existing file
        File2.delete(fullFileName);

        //get dConnect.  If this fails, no clean up needed.
        DConnect dConnect = new DConnect(dapUrl, true, 1, 1);
        DAS das;
        DDS dds;
        try {
            das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new SimpleException("Error while getting DAS from " + dapUrl + ".das .\n" +
                t.getMessage(), t);
        }
        try {
            if (debug) String2.log(String2.annotatedString(SSR.getUrlResponseString(dapUrl + ".dds")));
            dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new SimpleException("Error while getting DDS from " + dapUrl + ".dds .\n" +
                t.getMessage(), t);
        }

        //find all vars
        String varNames[] = findAllScalarOrMultiDimVars(dds);
        int nVars = varNames.length;
        if (nVars == 0)
            throw new RuntimeException(beginError + "No variables found!");

        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //*** make ncOut.    If createNew fails, no clean up needed.
        File2.makeDirectory(File2.getDirectory(fullFileName));
        ucar.nc2.NetcdfFileWriteable ncOut =
            ucar.nc2.NetcdfFileWriteable.createNew(fullFileName + randomInt,
                false); //false says: create a new file and don't fill with missing_values

        try {
            //define the data variables in ncOut
            StringArray dimNames = new StringArray();
            IntArray    dimSizes = new IntArray();
            ArrayList   dims     = new ArrayList(); //ucar.nc2.Dimension
            int         varShape[][] = new int[nVars][];
            boolean     isString[] = new boolean[nVars];  //all false
            for (int v = 0; v < nVars; v++) {

                BaseType baseType = dds.getVariable(varNames[v]);
                if (debug) String2.log("\ncreate varName=" + varNames[v] + 
                    " has typeName=" + baseType.getTypeName());
                Attributes varAtts = new Attributes();
                getAttributes(das, varNames[v], varAtts);
                if (debug) String2.log(varAtts.toString());
                if (baseType instanceof DGrid) {
                    DGrid dGrid = (DGrid)baseType;
                    int nDims = dGrid.elementCount(true) - 1;                    
                    ArrayList tDims = new ArrayList();
                    varShape[v] = new int[nDims];
                    for (int d = 0; d < nDims; d++) {
                        BaseType dimBaseType = dGrid.getVar(d + 1);
                        String dimName = dimBaseType.getName();
                        if (debug) String2.log("  dimName=" + dimName + 
                            " has typeName=" + dimBaseType.getTypeName());
                        DArrayDimension dim = ((DArray)dimBaseType).getFirstDimension();
                        int dimSize = dim.getSize();
                        int which = dimNames.indexOf(dimName);
                        if (which < 0) {
                            //create it and store it
                            which = dimNames.size();
                            dimNames.add(dimName);
                            dimSizes.add(dimSize);
                            dims.add(ncOut.addDimension(dimName, dimSize, true, false, false));
                        }
                        tDims.add(dims.get(which));
                        varShape[v][d] = dimSize;
                    }

                    PrimitiveVector pv = ((DArray)dGrid.getVar(0)).getPrimitiveVector(); //has no data
                    Class tClass = getElementClass(pv);
                    if (debug) String2.log("  DGrid pv=" + pv.toString() + " tClass=" + tClass);
                    if (tClass == String.class) {
                        //make String variable
                        isString[v] = true;
                        int strlen = varAtts.getInt("DODS_strlen");
                        if (strlen <= 0 || strlen == Integer.MAX_VALUE) //netcdf-java doesn't like 0
                            strlen = 255;
                        ncOut.addStringVariable(varNames[v], tDims, strlen);
                    } else {
                        
                        //make numeric variable
                        ncOut.addVariable(varNames[v], NcHelper.getDataType(tClass), tDims);
                    }

                } else if (baseType instanceof DArray) {
                    //dArray is usually 1 dim, but may be multidimensional
                    DArray dArray = (DArray)baseType;
                    int nDims = dArray.numDimensions();    
                    ArrayList tDims = new ArrayList();
                    varShape[v] = new int[nDims];
                    for (int d = 0; d < nDims; d++) {//0..
                        DArrayDimension dim = dArray.getDimension(d);
                        String dimName = dim.getName();
                        int dimSize = dim.getSize();
                        int which = dimNames.indexOf(dimName);
                        if (which < 0) {
                            //create it and store it
                            which = dimNames.size();
                            dimNames.add(dimName);
                            dimSizes.add(dimSize);
                            dims.add(ncOut.addDimension(dimName, dimSize, true, false, false));
                        }
                        tDims.add(dims.get(which));
                        varShape[v][d] = dimSize;
                    }

                    PrimitiveVector pv = dArray.getPrimitiveVector(); //has no data
                    Class tClass = getElementClass(pv);
                    if (debug) String2.log("  DArray pv=" + pv.toString() + " tClass=" + tClass);
                    if (tClass == String.class) {
                        //make String variable
                        isString[v] = true;
                        int strlen = varAtts.getInt("DODS_strlen");
                        if (strlen <= 0 || strlen == Integer.MAX_VALUE)
                            strlen = 255;
                        ncOut.addStringVariable(varNames[v], tDims, strlen);
                    } else {
                        //make numeric variable
                        ncOut.addVariable(varNames[v], NcHelper.getDataType(tClass), tDims);
                    }

                } else {
                    //it's a scalar variable
                    PrimitiveVector pv = baseType.newPrimitiveVector(); //has no data
                    Class tClass = getElementClass(pv);
                    if (debug) String2.log("  scalar pv=" + pv.toString() + " tClass=" + tClass);

                    if (tClass == String.class) {
                        //make String scalar variable
                        isString[v] = true;
                        if (debug) String2.log("  isString=true");
                        int strlen = varAtts.getInt("DODS_strlen");
                        if (strlen <= 0 || strlen == Integer.MAX_VALUE)
                            strlen = 255;

                        String dimName = "string1";
                        int dimSize = 1;
                        int which = dimNames.indexOf(dimName);
                        if (which < 0) {
                            //create it and store it
                            which = dimNames.size();
                            dimNames.add(dimName);
                            dimSizes.add(dimSize);
                            dims.add(ncOut.addDimension(dimName, dimSize, true, false, false));
                        }
                        ArrayList tDims = new ArrayList();
                        tDims.add(dims.get(which));
                        varShape[v] = new int[1];
                        varShape[v][0] = dimSize;
                        ncOut.addStringVariable(varNames[v], tDims, strlen);

                    } else {
                        //make numeric scalar variable
                        varShape[v] = new int[0];
                        ncOut.addVariable(varNames[v], NcHelper.getDataType(tClass), 
                            new ArrayList());
                    }
                }

                //write data variable attributes in ncOut
                NcHelper.setAttributes(ncOut, varNames[v], varAtts);
            }

            //write global attributes in ncOut
            Attributes gAtts = new Attributes();
            getAttributes(das, "GLOBAL", gAtts);
            NcHelper.setAttributes(ncOut, "NC_GLOBAL", gAtts);

            //leave "define" mode in ncOut
            ncOut.create();

            //read and write the variables
            for (int v = 0; v < nVars; v++) {
                long vTime = System.currentTimeMillis();
                if (debug) String2.log("write v#" + v + "=" + varNames[v]); 

                //DGrid, DArray, scalar: read it, write it
                PrimitiveArray pas[] = getPrimitiveArrays(
                    dConnect, "?" + varNames[v]); // + projection); 
                pas[0].trimToSize(); //so underlying array is exact size
                if (debug) String2.log("  pas[0].size=" + pas[0].size() + 
                    (pas[0].size() > 0? " #0=" + pas[0].getString(0) : ""));
                if (isString[v]) {
                    //String variable
                    int n = pas[0].size();
                    ucar.ma2.ArrayObject.D1 ao = new 
                        ucar.ma2.ArrayObject.D1(String.class, n); 
                    for (int i = 0; i < n; i++)
                        ao.set(i, pas[0].getString(i));
                    ncOut.writeStringData(varNames[v], ao);
                } else {
                    //non-String variable
                    ncOut.write(varNames[v], 
                        ucar.ma2.Array.factory(pas[0].elementClass(), 
                            varShape[v], pas[0].toObjectArray()));
                }

                if (verbose) String2.log("  v#" + v + "=" + varNames[v] + " finished. time=" + 
                    Calendar2.elapsedTimeString(System.currentTimeMillis() - vTime));
            }

            //if close throws Throwable, it is trouble
            ncOut.close(); //it calls flush() and doesn't like flush called separately

            //rename the file to the specified name
            File2.rename(fullFileName + randomInt, fullFileName);

            //diagnostic
            if (verbose) String2.log("  OpendapHelper.allDapToNc finished.  TIME=" + 
                Calendar2.elapsedTimeString(System.currentTimeMillis() - time) + "\n");
            //String2.log(NcHelper.dumpString(fullFileName, false));

        } catch (Throwable t) {
            //try to close the file
            try {
                ncOut.close(); //it calls flush() and doesn't like flush called separately
            } catch (Throwable t2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullFileName + randomInt);

            throw t;
        }
    }

    /**
     * Test allDapToNc.
     * @param whichTests -1 for all, or 0.. for specific ones
     */
    public static void testAllDapToNc(int whichTests) throws Throwable {
        //tests from nodc template examples http://www.nodc.noaa.gov/data/formats/netcdf/
        String2.log("\n*** OpendapHelper.testAllDapToNc(" + whichTests + ")");
        String dir = "c:/data/nodcTemplates/";
        String fileName;
        String url, results, expected;

        if (whichTests == -1 || whichTests == 0) {
            //this tests numeric scalars, and  numeric and String 1D arrays
            fileName = "pointKachemakBay.nc";
            url = "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/point/KachemakBay.nc";
            allDapToNc(url, dir + fileName);
            results = NcHelper.dds(dir + fileName);
            String2.log(results);
            //expected = "zztop";
            //Test.ensureEqual(results, expected, "");
        }

        if (whichTests == -1 || whichTests == 1) {
            //this tests numeric and String scalars, and  numeric 1D arrays
            fileName = "timeSeriesBodegaMarineLabBuoy.nc";
            url = "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/timeSeries/BodegaMarineLabBuoy.nc";
            allDapToNc(url, dir + fileName);
            results = NcHelper.dds(dir + fileName);
            expected = 
"netcdf c:/data/nodcTemplates/timeSeriesBodegaMarineLabBuoy.nc {\n" +
"  dimensions:\n" +
"    time = 63242;\n" +
"    string1 = 1;\n" +
"    station_name_strlen = 17;\n" +
"  variables:\n" +
"    double time(time=63242);\n" +
"    float lat;\n" +
"    float lon;\n" +
"    double alt;\n" +
"    char station_name(string1=1, station_name_strlen=17);\n" +
"    double temperature(time=63242);\n" +
"    double salinity(time=63242);\n" +
"    double density(time=63242);\n" +
"    double conductivity(time=63242);\n" +
"    int platform1;\n" +
"    int temperature_qc(time=63242);\n" +
"    int salinity_qc(time=63242);\n" +
"    int density_qc(time=63242);\n" +
"    int conductivity_qc(time=63242);\n" +
"    int instrument1;\n" +
"    int instrument2;\n" +
"    double ht_wgs84;\n" +
"    double ht_mllw;\n" +
"    int crs;\n" +
"  // global attributes:\n" +
"}\n";
            Test.ensureEqual(results, expected, "results=\n" + results);
        }

        if (whichTests == -1 || whichTests == 2) {
            //this tests numeric scalars, and    grids
            fileName = "trajectoryAoml_tsg.nc";
            url = "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/trajectory/aoml_tsg.nc";
            allDapToNc(url, dir + fileName);
            results = NcHelper.dds(dir + fileName);
            String2.log(results);
            expected = 
"netcdf c:/data/nodcTemplates/trajectoryAoml_tsg.nc {\n" +
"  dimensions:\n" +
"    trajectory = 1;\n" +
"    obs = 2880;\n" +
"  variables:\n" +
"    int trajectory(trajectory=1);\n" +
"    int time(trajectory=1, obs=2880);\n" +
"    double lat(trajectory=1, obs=2880);\n" +
"    double lon(trajectory=1, obs=2880);\n" +
"    double intp(trajectory=1, obs=2880);\n" +
"    double sal(trajectory=1, obs=2880);\n" +
"    double cond(trajectory=1, obs=2880);\n" +
"    double ext(trajectory=1, obs=2880);\n" +
"    double sst(trajectory=1, obs=2880);\n" +
"    byte plt(trajectory=1);\n" +
"    byte tsg(trajectory=1);\n" +
"    byte tmsr(trajectory=1);\n" +
"    byte sstr(trajectory=1);\n" +
"    byte flag_a(trajectory=1, obs=2880);\n" +
"    byte flag_b(trajectory=1, obs=2880);\n" +
"    byte flag_c(trajectory=1, obs=2880);\n" +
"    byte flag_d(trajectory=1, obs=2880);\n" +
"    byte flag_e(trajectory=1, obs=2880);\n" +
"    byte flag_f(trajectory=1, obs=2880);\n" +
"    byte flag_g(trajectory=1, obs=2880);\n" +
"    byte flag_h(trajectory=1, obs=2880);\n" +
"    byte flag_i(trajectory=1, obs=2880);\n" +
"    byte flag_j(trajectory=1, obs=2880);\n" +
"    byte flag_k(trajectory=1, obs=2880);\n" +
"    byte flag_l(trajectory=1, obs=2880);\n" +
"    byte crs(trajectory=1);\n" +
"  // global attributes:\n" +
"}\n";
            Test.ensureEqual(results, expected, "");
        }


        if (whichTests == -1 || whichTests == 3) {
            //this tests numeric scalars, and   byte/numeric arrays
            fileName = "trajectoryJason2_satelliteAltimeter.nc";
            url = "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/trajectory/jason2_satelliteAltimeter.nc";
            allDapToNc(url, dir + fileName);
            results = NcHelper.dds(dir + fileName);
            String2.log(results);
            expected = 
"netcdf c:/data/nodcTemplates/trajectoryJason2_satelliteAltimeter.nc {\n" +
"  dimensions:\n" +
"    trajectory = 1;\n" +
"    obs = 3;\n" +
"    meas_ind = 20;\n" +
"  variables:\n" +
"    double time(trajectory=1, obs=3);\n" +
"    byte meas_ind(trajectory=1, meas_ind=20);\n" +
"    int lat(trajectory=1, obs=3);\n" +
"    int lon(trajectory=1, obs=3);\n" +
"    byte surface_type(trajectory=1, obs=3);\n" +
"    byte orb_state_flag_rest(trajectory=1, obs=3);\n" +
"    byte ecmwf_meteo_map_avail(trajectory=1, obs=3);\n" +
"    byte interp_flag_meteo(trajectory=1, obs=3);\n" +
"    int alt(trajectory=1, obs=3);\n" +
"    byte range_numval_ku(trajectory=1, obs=3);\n" +
"    short model_wet_tropo_corr(trajectory=1, obs=3);\n" +
"    byte atmos_corr_sig0_ku(trajectory=1, obs=3);\n" +
"    short tb_187(trajectory=1, obs=3);\n" +
"    short rad_water_vapor(trajectory=1, obs=3);\n" +
"    short ssha(trajectory=1, obs=3);\n" +
"  // global attributes:\n" +
"}\n";
            Test.ensureEqual(results, expected, "");
        }

/*        if (whichTests == -1 || whichTests == 4) {
//JDAP fails to read/parse the .dds:
//Exception in thread "main" com.cohort.util.SimpleException: Error while getting DDS from http://data.nodc.noaa.gov/thredds/dodsC/testdata/ne
//tCDFTemplateExamples/profile/wodObservedLevels.nc.dds .
//
//Parse Error on token: String
//In the dataset descriptor object:
//Expected a variable declaration (e.g., Int32 i;).
//        at gov.noaa.pfel.coastwatch.griddata.OpendapHelper.allDapToNc(OpendapHelper.java:1239)
//        at gov.noaa.pfel.coastwatch.griddata.OpendapHelper.testAllDapToNc(OpendapHelper.java:1716)
//        at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:741)
            //this tests numeric scalars, and  numeric and string arrays
            fileName = "profileWodObservedLevels.nc";
            url = "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/profile/wodObservedLevels.nc";
            allDapToNc(url, dir + fileName);
            results = NcHelper.dumpString(dir + fileName, false);
            String2.log(results);
            //expected = "zztop";
            //Test.ensureEqual(results, expected, "");
        }
*/
        if (whichTests == -1 || whichTests == 5) {
            //this tests numeric scalars, and numeric arrays
            fileName = "timeSeriesProfileUsgs_internal_wave_timeSeries.nc";
            url = "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/timeSeriesProfile/usgs_internal_wave_timeSeries.nc";
            allDapToNc(url, dir + fileName);
            results = NcHelper.dds(dir + fileName);
            String2.log(results);
            expected = 
"netcdf c:/data/nodcTemplates/timeSeriesProfileUsgs_internal_wave_timeSeries.nc {\n" +
"  dimensions:\n" +
"    station = 1;\n" +
"    time = 38990;\n" +
"    z = 5;\n" +
"  variables:\n" +
"    int station_id(station=1);\n" +
"    double time(time=38990);\n" +
"    double z(z=5);\n" +
"    double lon(station=1);\n" +
"    double lat(station=1);\n" +
"    double T_20(station=1, time=38990, z=5);\n" +
"    double C_51(station=1, time=38990, z=5);\n" +
"    double S_40(station=1, time=38990, z=5);\n" +
"    double STH_71(station=1, time=38990, z=5);\n" +
"    int instrument_1(station=1, z=5);\n" +
"    int instrument_2(station=1);\n" +
"    int platform;\n" +
"    int crs;\n" +
"  // global attributes:\n" +
"}\n";
            Test.ensureEqual(results, expected, "");
        }

//currently no trajectoryProfile example

//currently no swath example


    }

    /**
     * Get data from a common type of Opendap grid request and save in .nc file.
     * <p>Currently, this won't work with variables in a sequence.
     *
     * @param dapUrl       the base DAP URL
     * @param varNames     the DGrid or DArray var names (which must share the same dimensions)
     *   <br>If a var isn't present, it is skipped.
     *   <br>If some vars are multidimensional, 1D vars are removed 
     *   (they'll be downloaded as dimensions of the multidimensional vars).
     *   <br>If varNames is null or varNames.length==0, the vars which share 
     *       the most dimensions will be found and used.
     * @param projection   e.g., [17][0][0:179][0:2:359]  (or null or "" for all) 
     * @param fullFileName the complete name
     * @param jplMode use for requests for jplG1SST data (to enable lat chunking)
     *   when the projection is [0][0:15999][0:35999]
     */
    public static void dapToNc(String dapUrl, String varNames[], String projection, 
        String fullFileName, boolean jplMode) throws Throwable {

        //constants for jpl
        int jplLonSize = 36000;
        int jplLatSize = 16000;
        int jplLatChunk = 2000;
        int jplNChunks = jplLatSize / jplLatChunk;
        int jplLatDim = 1;  //[time][lat][lon]
        FloatArray jplLatPa = null;
        if (jplMode) {
            jplLatPa = new FloatArray(jplLatSize, true);
            for (int i = 0; i < jplLatSize; i++)
                jplLatPa.setDouble(i, -79.995 + i * 0.01);
        }
        int jplChunkShape[] = {1, jplLatChunk, jplLonSize};

        String beginError = "OpendapHelper.dapToNc" +
            "\n  url=" + dapUrl + 
            "\n  varNames=" + 
                (varNames == null? "null" : String2.toSVString(varNames, ",", false)) + 
                "  projection=" + projection +
            "\n  file=" + fullFileName;
        if (verbose) String2.log(beginError); 
        beginError = String2.ERROR + " in " + beginError + "\n"; 
        long time = System.currentTimeMillis();

        //get dConnect.  If this fails, no clean up needed.
        DConnect dConnect = new DConnect(dapUrl, true, 1, 1);
        DAS das = dConnect.getDAS(DEFAULT_TIMEOUT);
        DDS dds = dConnect.getDDS(DEFAULT_TIMEOUT);

        if (varNames == null || varNames.length == 0) {
            //find the vars which share the most dimensions
            varNames = findVarsWithSharedDimensions(dds);
            if (varNames.length == 0)
                throw new RuntimeException(beginError + 
                    "No variables with dimensions were found!");

        } else {
            //check if varNames exist (if not, set var[v] = null) 
            //also, check if there are vars with nDim>1
            boolean dim1Vars[] = new boolean[varNames.length]; //all false
            boolean someMultiDimVars = false;
            for (int v = 0; v < varNames.length; v++) {
                try {
                    BaseType baseType = dds.getVariable(varNames[v]);
                    DArray dArray;
                    if (baseType instanceof DGrid) {
                        //dGrid has main dArray + dimensions
                        DGrid dGrid = (DGrid)baseType;
                        dArray = (DArray)dGrid.getVar(0);                    
                    } else if (baseType instanceof DArray) {
                        //dArray is usually 1 dim, but may be multidimensional
                        dArray = (DArray)baseType;
                    } else {
                        continue;
                    }
                    if (dArray.numDimensions() <= 1)
                        dim1Vars[v] = true;
                    else someMultiDimVars = true;

                } catch (Throwable t) {
                    varNames[v] = null;
                    if (verbose) String2.log("  removing variable: " + t.toString());
                    continue;
                }
            }

            //if someMultiDimVars, remove any dim1Vars (they'll be downloaded as dimensions)
            if (someMultiDimVars) 
                for (int v = 0; v < varNames.length; v++) 
                    if (dim1Vars[v]) 
                        varNames[v] = null;

            //are there validVars remaining? 
            boolean someValidVars = false;
            for (int v = 0; v < varNames.length; v++) {
                if (varNames[v] != null) {
                    someValidVars = true;
                    break;
                }
            }
            if (!someValidVars)
                throw new RuntimeException(beginError + "None of the varNames were found!");
        }


        //if projection is null or "", figure out the projection 
        if (projection == null || projection.length() == 0) {
            StringBuilder sb = new StringBuilder();
            BUILD_PROJECTION:
            for (int v = 0; v < varNames.length; v++) {
                if (varNames[v] == null)
                    continue;
                BaseType baseType = dds.getVariable(varNames[v]);
                DArray dArray;
                if (baseType instanceof DGrid) {
                    //dGrid has main dArray + dimensions
                    DGrid dGrid = (DGrid)baseType;
                    dArray = (DArray)dGrid.getVar(0);
                } else if (baseType instanceof DArray) {
                    //dArray is usually 1 dim, but may be multidimensional
                    dArray = (DArray)baseType;
                } else {
                    throw new RuntimeException(beginError + 
                        "var=" + varNames[v] + " has unexpected baseType=" + 
                        baseType.getClass().getName());
                }
                int nDim = dArray.numDimensions();
                if (nDim == 0) 
                    throw new RuntimeException(beginError + 
                        "var=" + varNames[v] + " is a DArray with 0 dimensions.");
                for (int d = 0; d < nDim; d++) {//0..
                    sb.append("[0:" + (dArray.getDimension(d).getSize() - 1) + "]");
                }
                break;
            }

            if (sb.length() == 0) 
                throw new RuntimeException(beginError + 
                    "File not created!  None of the requested varNames were found.");
            projection = sb.toString();
            if (verbose) String2.log("  created projection=" + projection);
        }
        int sss[] = parseStartStrideStop(projection); //throws Exception if trouble
        //if (true) throw new RuntimeException("stop here");

        //delete any existing file
        File2.delete(fullFileName);

        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //*Then* make ncOut.    If createNew fails, no clean up needed.
        File2.makeDirectory(File2.getDirectory(fullFileName));
        ucar.nc2.NetcdfFileWriteable ncOut =
            ucar.nc2.NetcdfFileWriteable.createNew(fullFileName + randomInt,
                false); //false says: create a new file and don't fill with missing_values

        try {

            //define the data variables in ncOut
            int nVars = varNames.length;
            Attributes varAtts[] = new Attributes[nVars];
            ucar.nc2.Dimension varCharDims[][] = new ucar.nc2.Dimension[nVars][];
            int varCharShapes[][] = new int[nVars][];

            boolean firstValidVar = true;
            int nDims = sss.length / 3;
            ucar.nc2.Dimension dims[] = new ucar.nc2.Dimension[nDims];
            int shape[] = new int[nDims];
            boolean isDGrid = true; //change if false

            for (int v = 0; v < nVars; v++) {
                //String2.log("  create var=" + varNames[v]);
                if (varNames[v] == null) 
                    continue;
                varAtts[v] = new Attributes();
                getAttributes(das, varNames[v], varAtts[v]);

                BaseType baseType = dds.getVariable(varNames[v]);
                if (baseType instanceof DGrid) {
                    //dGrid has main dArray + dimensions
                    if (firstValidVar) {
                    } else {
                        if (!isDGrid) 
                            throw new RuntimeException(beginError + "var=" + varNames[v] + 
                                " is a DGrid but the previous vars are DArrays.");
                    }                        
                    DGrid dGrid = (DGrid)baseType;
                    if (dGrid.elementCount(true)-1 != nDims)
                        throw new RuntimeException(beginError + "var=" + varNames[v] + 
                            " has a different nDimensions than projection=\"" + 
                            projection + "\".");
                    for (int d = 0; d < nDims; d++) {
                        String dimName = dGrid.getVar(d + 1).getName();
                        if (firstValidVar) {
                            //define the dimensions and their variables
                            int dimSize = calculateNValues(sss[d*3], sss[d*3+1], sss[d*3+2]);
                            //String2.log("    dim#" + d + "=" + dimName + " size=" + dimSize);
                            shape[d] = dimSize;
                            dims[d] = ncOut.addDimension(dimName, dimSize, true, false, false);
                            PrimitiveVector pv = ((DVector)dds.getVariable(dimName)).getPrimitiveVector(); //has no data
                            ncOut.addVariable(dimName, 
                                NcHelper.getDataType(getElementClass(pv)), 
                                new ucar.nc2.Dimension[]{dims[d]}); 
                        } else {
                            //check that dimension names are the same
                            if (!dimName.equals(dims[d].getName()))
                                throw new RuntimeException(beginError + "var=" + varNames[v] + 
                                    " has different dimensions than previous vars.");
                        }
                    }
                    firstValidVar = false;

                    //make the dataVariable
                    PrimitiveVector pv = ((DArray)dGrid.getVar(0)).getPrimitiveVector(); //has no data
                    Class tClass = getElementClass(pv);
                    //String2.log("pv=" + pv.toString() + " tClass=" + tClass);
                    ncOut.addVariable(varNames[v], NcHelper.getDataType(tClass), dims);

                } else if (baseType instanceof DArray) {
                    //dArray is usually 1 dim, but may be multidimensional
                    if (firstValidVar) {
                        isDGrid = false;
                    } else {
                        if (isDGrid)
                            throw new RuntimeException(beginError + "var=" + varNames[v] + 
                                " is a DArray but the previous vars are DGrids.");
                    }
                    DArray dArray = (DArray)baseType;
                    if (dArray.numDimensions() != nDims)
                        throw new RuntimeException(beginError + "var=" + varNames[v] + 
                            " has a different nDimensions than projection=\"" + 
                            projection + "\".");
                    for (int d = 0; d < nDims; d++) {//0..
                        DArrayDimension dim = dArray.getDimension(d);
                        String dimName = dim.getName();
                        if (firstValidVar) {
                            //define the dimensions
                            int dimSize = calculateNValues(sss[d*3], sss[d*3+1], sss[d*3+2]);
                            //String2.log("    DArray dim#" + d + "=" + dimName + " size=" + dimSize);
                            shape[d] = dimSize;
                            dims[d] = ncOut.addDimension(dimName, dimSize, 
                                true, false, false);
                            //don't make a related variable
                        } else {
                            //check that dimension names are the same
                            if (!dimName.equals(dims[d].getName()))
                                throw new RuntimeException(beginError + "var=" + varNames[v] + 
                                    " has different dimensions than previous vars.");
                        }
                    }
                    firstValidVar = false;

                    //make the dataVariable
                    PrimitiveVector pv = dArray.getPrimitiveVector(); //has no data
                    Class tClass = getElementClass(pv);
                    //String2.log("  pv tClass=" + tClass);

                    if (tClass == String.class) {
                        //a String variable.  Add a dim for nchars
                        varCharDims[v] = new ucar.nc2.Dimension[nDims + 1];
                        varCharShapes[v] = new int[nDims + 1];
                        System.arraycopy(dims,  0, varCharDims[v],   0, nDims);
                        System.arraycopy(shape, 0, varCharShapes[v], 0, nDims);
                        int nChars = varAtts[v].getInt("DODS_strlen");
                        if (nChars == Integer.MAX_VALUE) {
                            if (verbose) String2.log(beginError + "String var=" + varNames[v] + 
                                " has no DODS_strlen attribute.");
                            varNames[v] = null;
                            continue;
                        }
                        varCharDims[v][nDims] = ncOut.addDimension(
                            varNames[v] + NcHelper.StringLengthSuffix, 
                            nChars, true, false, false);
                        varCharShapes[v][nDims] = nChars;

                        ncOut.addVariable(varNames[v], ucar.ma2.DataType.CHAR, varCharDims[v]);

                    } else {
                        //a regular variable
                        ncOut.addVariable(varNames[v], NcHelper.getDataType(tClass), dims);
                    }

                } else {
                   throw new RuntimeException(beginError + 
                       "var=" + varNames[v] + " has unexpected baseType=" + 
                        baseType.getClass().getName());
                }
            }

            //write global attributes in ncOut
            Attributes tAtts = new Attributes();
            getAttributes(das, "GLOBAL", tAtts);
            NcHelper.setAttributes(ncOut, "NC_GLOBAL", tAtts);

            //write dimension attributes in ncOut
            if (isDGrid) {
                for (int dim = 0; dim < nDims; dim++) {
                    String dimName = dims[dim].getName();               
                    tAtts.clear();
                    getAttributes(das, dimName, tAtts);
                    NcHelper.setAttributes(ncOut, dimName, tAtts);
                }
            }

            //write data variable attributes in ncOut
            for (int v = 0; v < nVars; v++) {
                if (varNames[v] == null)
                    continue;
                NcHelper.setAttributes(ncOut, varNames[v], varAtts[v]);
            }

            //leave "define" mode in ncOut
            ncOut.create();

            //read and write the dimensions
            if (isDGrid) {
                for (int d = 0; d < nDims; d++) {
                    String tProjection = "[" + sss[d*3] + ":" + sss[d*3+1] + ":" + sss[d*3+2] + "]"; 
                    PrimitiveArray pas[] = getPrimitiveArrays(dConnect, 
                        "?" + dims[d].getName() + tProjection); 
                    pas[0].trimToSize(); //so underlying array is exact size
                    ncOut.write(dims[d].getName(), 
                        ucar.ma2.Array.factory(pas[0].toObjectArray()));
                }
            }

            //read and write the data variables
            firstValidVar = true;
            for (int v = 0; v < nVars; v++) {
                if (varNames[v] == null)
                    continue;
                long vTime = System.currentTimeMillis();

                if (jplMode) {
                    //read in chunks
                    int origin[] = {0, 0, 0}; //nc uses: start stop! stride
                    for (int chunk = 0; chunk < jplNChunks; chunk++) {
                        int base = chunk * jplLatChunk;
                        origin[1] = base;
                        //change that lat part of the projection
                        String tProjection = String2.replaceAll(projection, 
                            "[0:" + (jplLatSize - 1) + "]", 
                            "[" + base + ":" + (base + jplLatChunk - 1) + "]");
                        PrimitiveArray pas[] = getPrimitiveArrays(
                            dConnect, "?" + varNames[v] + tProjection); 
                        pas[0].trimToSize(); //so underlying array is exact size
                        //String2.log("pas[0]=" + pas[0].toString());
                        ncOut.write(varNames[v], origin,
                            ucar.ma2.Array.factory(pas[0].elementClass(), 
                                jplChunkShape, pas[0].toObjectArray()));
                    }
                } else {
                    //DGrid and DArray: read it, write it

                    PrimitiveArray pas[] = getPrimitiveArrays(
                        dConnect, "?" + varNames[v] + projection); 
                    pas[0].trimToSize(); //so underlying array is exact size
                    //String2.log("pas[0].size=" + pas[0].size());
                    if (varCharShapes[v] == null) {
                        //non-String variable
                        ncOut.write(varNames[v], 
                            ucar.ma2.Array.factory(pas[0].elementClass(), 
                                shape, pas[0].toObjectArray()));
                    } else {
                        //String variable
                        int n = pas[0].size();
                        ucar.ma2.ArrayObject.D1 ao = new 
                            ucar.ma2.ArrayObject.D1(String.class, n); 
                        for (int i = 0; i < n; i++)
                            ao.set(i, pas[0].getString(i));
                        ncOut.writeStringData(varNames[v], ao);
                    }
                }

                firstValidVar = false;
                if (verbose) String2.log("  v#" + v + "=" + varNames[v] + " finished. time=" + 
                    Calendar2.elapsedTimeString(System.currentTimeMillis() - vTime));
            }

            //if close throws Throwable, it is trouble
            ncOut.close(); //it calls flush() and doesn't like flush called separately

            //rename the file to the specified name
            File2.rename(fullFileName + randomInt, fullFileName);

            //diagnostic
            if (verbose) String2.log("  OpendapHelper.dapToNc finished.  TIME=" + 
                Calendar2.elapsedTimeString(System.currentTimeMillis() - time) + "\n");
            //String2.log(NcHelper.dumpString(fullFileName, false));

        } catch (Throwable t) {
            //try to close the file
            try {
                ncOut.close(); //it calls flush() and doesn't like flush called separately
            } catch (Throwable t2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullFileName + randomInt);

            throw t;
        }
    }

    /** This tests getting attibutes, notably the DODS_strlen attribute. */
    public static void testGetAttributes() throws Throwable {
        String url = "http://coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
        String2.log("\n* OpendapHelper.testGetAttributes\n" + url);
        try {
        DConnect dConnect = new DConnect(url, true, 1, 1);
        DAS das = dConnect.getDAS(DEFAULT_TIMEOUT);
        Attributes atts = new Attributes();
        getAttributes(das, "flag", atts);

        String results = atts.toString();
        String expected = //the DODS_ attributes are from an attribute that is a  container.
"    A=\"Units added\"\n" +
"    B=\"Data out of range\"\n" +
"    C=\"Non-sequential time\"\n" +
"    D=\"Failed T>=Tw>=Td\"\n" +
"    DODS_dimName=\"f_string\"\n" +
"    DODS_strlen=13\n" +
"    E=\"True wind error\"\n" +
"    F=\"Velocity unrealistic\"\n" +
"    G=\"Value > 4 s. d. from climatology\"\n" +
"    H=\"Discontinuity\"\n" +
"    I=\"Interesting feature\"\n" +
"    J=\"Erroneous\"\n" +
"    K=\"Suspect - visual\"\n" +
"    L=\"Ocean platform over land\"\n" +
"    long_name=\"quality control flags\"\n" +
"    M=\"Instrument malfunction\"\n" +
"    N=\"In Port\"\n" +
"    O=\"Multiple original units\"\n" +
"    P=\"Movement uncertain\"\n" +
"    Q=\"Pre-flagged as suspect\"\n" +
"    R=\"Interpolated data\"\n" +
"    S=\"Spike - visual\"\n" +
"    T=\"Time duplicate\"\n" +
"    U=\"Suspect - statistial\"\n" +
"    V=\"Spike - statistical\"\n" +
"    X=\"Step - statistical\"\n" +
"    Y=\"Suspect between X-flags\"\n" +
"    Z=\"Good data\"\n";
        Test.ensureEqual(results, expected, "results=" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(
                "\nUnexpected error (server timed out 2013-10-24):\n" +
                MustBe.throwableToString(t)); 
        }
    }


    /** This tests dapToNc DArray. */
    public static void testDapToNcDArray() throws Throwable {
        String2.log("\n\n*** OpendapHelper.testDapToNcDArray");
        String fileName, expected, results;      
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        fileName = SSR.getTempDirectory() + "testDapToNcDArray.nc";
        String dArrayUrl = "http://coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
        try {
            dapToNc(dArrayUrl, 
                //note that request for zztop is ignored (because not found)
                new String[] {"zztop", "time", "lat", "lon", "PL_HD", "flag"}, null, //projection
                fileName, false); //jplMode
            results = NcHelper.dumpString(fileName, true); //printData
            expected = 
"netcdf testDapToNcDArray.nc {\n" +
"  dimensions:\n" +
"    time = 144;\n" +
"    flag_strlen = 13;\n" +
"  variables:\n" +
"    int time(time=144);\n" +
"      :actual_range = 16870896, 16871039; // int\n" +
"      :data_interval = 60; // int\n" +
"      :long_name = \"time\";\n" +
"      :observation_type = \"calculated\";\n" +
"      :original_units = \"hhmmss UTC\";\n" +
"      :qcindex = 1; // int\n" +
"      :units = \"minutes since 1-1-1980 00:00 UTC\";\n" +
"\n" +
"    float lat(time=144);\n" +
"      :actual_range = 44.6f, 44.75f; // float\n" +
"      :average_center = \"time at end of period\";\n" +
"      :average_length = 60S; // short\n" +
"      :average_method = \"average\";\n" +
"      :data_precision = -9999.0f; // float\n" +
"      :instrument = \"unknown\";\n" +
"      :long_name = \"latitude\";\n" +
"      :observation_type = \"measured\";\n" +
"      :original_units = \"degrees (+N)\";\n" +
"      :qcindex = 2; // int\n" +
"      :sampling_rate = 1.0f; // float\n" +
"      :units = \"degrees (+N)\";\n" +
"\n" +
"    float lon(time=144);\n" +
"      :actual_range = 235.82f, 235.95f; // float\n" +
"      :average_center = \"time at end of period\";\n" +
"      :average_length = 60S; // short\n" +
"      :average_method = \"average\";\n" +
"      :data_precision = -9999.0f; // float\n" +
"      :instrument = \"unknown\";\n" +
"      :long_name = \"longitude\";\n" +
"      :observation_type = \"measured\";\n" +
"      :original_units = \"degrees (-W/+E)\";\n" +
"      :qcindex = 3; // int\n" +
"      :sampling_rate = 1.0f; // float\n" +
"      :units = \"degrees (+E)\";\n" +
"\n" +
"    float PL_HD(time=144);\n" +
"      :actual_range = 37.89f, 355.17f; // float\n" +
"      :average_center = \"time at end of period\";\n" +
"      :average_length = 60S; // short\n" +
"      :average_method = \"average\";\n" +
"      :data_precision = -9999.0f; // float\n" +
"      :instrument = \"unknown\";\n" +
"      :long_name = \"platform heading\";\n" +
"      :missing_value = -9999.0f; // float\n" +
"      :observation_type = \"calculated\";\n" +
"      :original_units = \"degrees (clockwise towards true north)\";\n" +
"      :qcindex = 4; // int\n" +
"      :sampling_rate = 1.0f; // float\n" +
"      :special_value = -8888.0f; // float\n" +
"      :units = \"degrees (clockwise towards true north)\";\n" +
"\n" +
"    char flag(time=144, flag_strlen=13);\n" +
"      :A = \"Units added\";\n" +
"      :B = \"Data out of range\";\n" +
"      :C = \"Non-sequential time\";\n" +
"      :D = \"Failed T>=Tw>=Td\";\n" +
"      :DODS_dimName = \"f_string\";\n" +
"      :DODS_strlen = 13; // int\n" +
"      :E = \"True wind error\";\n" +
"      :F = \"Velocity unrealistic\";\n" +
"      :G = \"Value > 4 s. d. from climatology\";\n" +
"      :H = \"Discontinuity\";\n" +
"      :I = \"Interesting feature\";\n" +
"      :J = \"Erroneous\";\n" +
"      :K = \"Suspect - visual\";\n" +
"      :L = \"Ocean platform over land\";\n" +
"      :long_name = \"quality control flags\";\n" +
"      :M = \"Instrument malfunction\";\n" +
"      :N = \"In Port\";\n" +
"      :O = \"Multiple original units\";\n" +
"      :P = \"Movement uncertain\";\n" +
"      :Q = \"Pre-flagged as suspect\";\n" +
"      :R = \"Interpolated data\";\n" +
"      :S = \"Spike - visual\";\n" +
"      :T = \"Time duplicate\";\n" +
"      :U = \"Suspect - statistial\";\n" +
"      :V = \"Spike - statistical\";\n" +
"      :X = \"Step - statistical\";\n" +
"      :Y = \"Suspect between X-flags\";\n" +
"      :Z = \"Good data\";\n" +
"\n" +
"  // global attributes:\n" +
"  :contact_email = \"samos@coaps.fsu.edu\";\n" +
"  :contact_info = \"Center for Ocean-Atmospheric Prediction Studies, The Florida State University, Tallahassee, FL, 32306-2840, USA\";\n" +
"  :Cruise_id = \"Cruise_id undefined for now\";\n" +
"  :Data_modification_date = \"02/07/2012 10:03:37 EST\";\n" +
"  :data_provider = \"Timothy Salisbury\";\n" +
"  :elev = 0S; // short\n" +
"  :end_date_time = \"2012/01/28 -- 23:59  UTC\";\n" +
"  :EXPOCODE = \"EXPOCODE undefined for now\";\n" +
"  :facility = \"NOAA\";\n" +
"  :fsu_version = \"300\";\n" +
"  :ID = \"WTEP\";\n" +
"  :IMO = \"009270335\";\n" +
"  :Metadata_modification_date = \"02/07/2012 10:03:37 EST\";\n" +
"  :platform = \"SCS\";\n" +
"  :platform_version = \"4.0\";\n" +
"  :receipt_order = \"01\";\n" +
"  :site = \"OSCAR DYSON\";\n" +
"  :start_date_time = \"2012/01/28 -- 21:36  UTC\";\n" +
"  :title = \"OSCAR DYSON Meteorological Data\";\n" +
" data:\n" +
"time =\n" +
"  {16870896, 16870897, 16870898, 16870899, 16870900, 16870901, 16870902, 16870903, 16870904, 16870905, 16870906, 16870907, 16870908, 16870909, 16870910, 16870911, 16870912, 16870913, 16870914, 16870915, 16870916, 16870917, 16870918, 16870919, 16870920, 16870921, 16870922, 16870923, 16870924, 16870925, 16870926, 16870927, 16870928, 16870929, 16870930, 16870931, 16870932, 16870933, 16870934, 16870935, 16870936, 16870937, 16870938, 16870939, 16870940, 16870941, 16870942, 16870943, 16870944, 16870945, 16870946, 16870947, 16870948, 16870949, 16870950, 16870951, 16870952, 16870953, 16870954, 16870955, 16870956, 16870957, 16870958, 16870959, 16870960, 16870961, 16870962, 16870963, 16870964, 16870965, 16870966, 16870967, 16870968, 16870969, 16870970, 16870971, 16870972, 16870973, 16870974, 16870975, 16870976, 16870977, 16870978, 16870979, 16870980, 16870981, 16870982, 16870983, 16870984, 16870985, 16870986, 16870987, 16870988, 16870989, 16870990, 16870991, 16870992, 16870993, 16870994, 16870995, 16870996, 16870997, 16870998, 16870999, 16871000, 16871001, 16871002, 16871003, 16871004, 16871005, 16871006, 16871007, 16871008, 16871009, 16871010, 16871011, 16871012, 16871013, 16871014, 16871015, 16871016, 16871017, 16871018, 16871019, 16871020, 16871021, 16871022, 16871023, 16871024, 16871025, 16871026, 16871027, 16871028, 16871029, 16871030, 16871031, 16871032, 16871033, 16871034, 16871035, 16871036, 16871037, 16871038, 16871039}\n" +
"lat =\n" +
"  {44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.62, 44.62, 44.62, 44.62, 44.62, 44.62, 44.62, 44.61, 44.61, 44.61, 44.61, 44.61, 44.61, 44.61, 44.61, 44.6, 44.6, 44.6, 44.6, 44.6, 44.6, 44.61, 44.61, 44.61, 44.61, 44.62, 44.62, 44.62, 44.62, 44.63, 44.63, 44.63, 44.64, 44.64, 44.64, 44.65, 44.65, 44.65, 44.66, 44.66, 44.66, 44.67, 44.67, 44.67, 44.68, 44.68, 44.68, 44.69, 44.69, 44.69, 44.7, 44.7, 44.7, 44.71, 44.71, 44.71, 44.72, 44.72, 44.72, 44.73, 44.73, 44.73, 44.73, 44.74, 44.74, 44.74, 44.75}\n" +
"lon =\n" +
"  {235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.94, 235.94, 235.94, 235.94, 235.94, 235.94, 235.93, 235.93, 235.93, 235.92, 235.92, 235.92, 235.91, 235.91, 235.91, 235.9, 235.9, 235.9, 235.89, 235.89, 235.88, 235.88, 235.88, 235.88, 235.88, 235.88, 235.87, 235.87, 235.87, 235.87, 235.87, 235.87, 235.87, 235.86, 235.86, 235.86, 235.86, 235.86, 235.86, 235.86, 235.85, 235.85, 235.85, 235.85, 235.85, 235.85, 235.85, 235.85, 235.85, 235.84, 235.84, 235.84, 235.84, 235.84, 235.84, 235.83, 235.83, 235.83, 235.83, 235.83, 235.82, 235.82, 235.82, 235.82, 235.82, 235.82, 235.82}\n" +
"PL_HD =\n" +
"  {75.53, 75.57, 75.97, 76.0, 75.81, 75.58, 75.99, 75.98, 75.77, 75.61, 75.72, 75.75, 75.93, 75.96, 76.01, 75.64, 75.65, 75.94, 75.93, 76.12, 76.65, 76.42, 76.25, 75.81, 76.5, 76.09, 76.35, 76.0, 76.16, 76.36, 76.43, 75.99, 75.93, 76.41, 75.85, 76.07, 76.15, 76.33, 76.7, 76.37, 76.58, 76.89, 77.14, 76.81, 74.73, 75.24, 74.52, 81.04, 80.64, 73.21, 63.34, 37.89, 347.02, 309.93, 290.99, 285.0, 279.38, 276.45, 270.26, 266.33, 266.49, 266.08, 263.59, 261.41, 259.05, 259.82, 260.35, 262.78, 258.73, 249.71, 246.52, 245.78, 246.16, 245.88, 243.52, 231.62, 223.09, 221.08, 221.01, 221.08, 220.81, 223.64, 234.12, 239.55, 241.08, 242.09, 242.04, 242.33, 242.06, 242.22, 242.11, 242.3, 242.07, 247.35, 285.6, 287.02, 287.96, 288.37, 321.32, 344.82, 346.91, 344.78, 347.95, 344.75, 344.66, 344.78, 344.7, 344.76, 343.89, 336.73, 334.01, 340.23, 344.76, 348.25, 348.74, 348.63, 351.97, 344.55, 343.77, 343.71, 347.04, 349.06, 349.45, 349.79, 349.66, 349.7, 349.74, 344.2, 343.22, 341.79, 339.11, 334.12, 334.47, 334.62, 334.7, 334.66, 327.06, 335.74, 348.25, 351.05, 355.17, 343.66, 346.85, 347.28}\n" +
"flag =\"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZEZZSZZZZ\", \"ZZZZZEZZSZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\", \"ZZZZZZZZZZZZZ\"\n" +
"}\n";
            Test.ensureEqual(results, expected, "results=" + results);
            File2.delete(fileName);
        } catch (Throwable t) {
            String2.pressEnterToContinue(
                "\nUnexpected error (server timed out 2013-10-24):\n" +
                MustBe.throwableToString(t)); 
        }

        //test subset
        try {
            String2.log("\n* testDapToNcDArray Subset");
            fileName = SSR.getTempDirectory() + "testDapToNcDArraySubset.nc";
            String dArraySubsetUrl = "http://coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
            dapToNc(dArraySubsetUrl, 
                new String[] {"zztop", "time", "lat", "lon", "PL_HD", "flag"}, "[0:10:99]", //projection
                fileName, false); //jplMode
            results = NcHelper.dumpString(fileName, true); //printData
            expected = 
"netcdf testDapToNcDArraySubset.nc {\n" +
" dimensions:\n" +
"   time = 10;\n" +
" variables:\n" +
"   int time(time=10);\n" +
"     :actual_range = 16870896, 16871039; // int\n" +
"     :data_interval = 60; // int\n" +
"     :long_name = \"time\";\n" +
"     :observation_type = \"calculated\";\n" +
"     :original_units = \"hhmmss UTC\";\n" +
"     :qcindex = 1; // int\n" +
"     :units = \"minutes since 1-1-1980 00:00 UTC\";\n" +
"   float lat(time=10);\n" +
"     :actual_range = 44.6f, 44.75f; // float\n" +
"     :average_center = \"time at end of period\";\n" +
"     :average_length = 60S; // short\n" +
"     :average_method = \"average\";\n" +
"     :data_precision = -9999.0f; // float\n" +
"     :instrument = \"unknown\";\n" +
"     :long_name = \"latitude\";\n" +
"     :observation_type = \"measured\";\n" +
"     :original_units = \"degrees (+N)\";\n" +
"     :qcindex = 2; // int\n" +
"     :sampling_rate = 1.0f; // float\n" +
"     :units = \"degrees (+N)\";\n" +
"   float lon(time=10);\n" +
"     :actual_range = 235.82f, 235.95f; // float\n" +
"     :average_center = \"time at end of period\";\n" +
"     :average_length = 60S; // short\n" +
"     :average_method = \"average\";\n" +
"     :data_precision = -9999.0f; // float\n" +
"     :instrument = \"unknown\";\n" +
"     :long_name = \"longitude\";\n" +
"     :observation_type = \"measured\";\n" +
"     :original_units = \"degrees (-W/+E)\";\n" +
"     :qcindex = 3; // int\n" +
"     :sampling_rate = 1.0f; // float\n" +
"     :units = \"degrees (+E)\";\n" +
"   float PL_HD(time=10);\n" +
"     :actual_range = 37.89f, 355.17f; // float\n" +
"     :average_center = \"time at end of period\";\n" +
"     :average_length = 60S; // short\n" +
"     :average_method = \"average\";\n" +
"     :data_precision = -9999.0f; // float\n" +
"     :instrument = \"unknown\";\n" +
"     :long_name = \"platform heading\";\n" +
"     :missing_value = -9999.0f; // float\n" +
"     :observation_type = \"calculated\";\n" +
"     :original_units = \"degrees (clockwise towards true north)\";\n" +
"     :qcindex = 4; // int\n" +
"     :sampling_rate = 1.0f; // float\n" +
"     :special_value = -8888.0f; // float\n" +
"     :units = \"degrees (clockwise towards true north)\";\n" +
"\n" +
" :contact_email = \"samos@coaps.fsu.edu\";\n" +
" :contact_info = \"Center for Ocean-Atmospheric Prediction Studies, The Florida State University, Tallahassee, FL, 32306-2840, USA\";\n" +
" :Cruise_id = \"Cruise_id undefined for now\";\n" +
" :Data_modification_date = \"02/07/2012 10:03:37 EST\";\n" +
" :data_provider = \"Timothy Salisbury\";\n" +
" :elev = 0S; // short\n" +
" :end_date_time = \"2012/01/28 -- 23:59  UTC\";\n" +
" :EXPOCODE = \"EXPOCODE undefined for now\";\n" +
" :facility = \"NOAA\";\n" +
" :fsu_version = \"300\";\n" +
" :ID = \"WTEP\";\n" +
" :IMO = \"009270335\";\n" +
" :Metadata_modification_date = \"02/07/2012 10:03:37 EST\";\n" +
" :platform = \"SCS\";\n" +
" :platform_version = \"4.0\";\n" +
" :receipt_order = \"01\";\n" +
" :site = \"OSCAR DYSON\";\n" +
" :start_date_time = \"2012/01/28 -- 21:36  UTC\";\n" +
" :title = \"OSCAR DYSON Meteorological Data\";\n" +
" data:\n" +
"time =\n" +
"  {16870896, 16870906, 16870916, 16870926, 16870936, 16870946, 16870956, 16870966, 16870976, 16870986}\n" +
"lat =\n" +
"  {44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.62, 44.61}\n" +
"lon =\n" +
"  {235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.94, 235.91}\n" +
"PL_HD =\n" +
"  {75.53, 75.72, 76.65, 76.43, 76.58, 63.34, 266.49, 246.52, 220.81, 242.11}\n" +
"}\n";
/* from
http://coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc.ascii?time[0:10:99],lat[0:10:99],lon[0:10:99],PL_HD[0:10:99]
time[10]  16870896, 16870906, 16870916, 16870926, 16870936, 16870946, 16870956, 16870966, 16870976, 16870986
lat[10]   44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.62, 44.61
lon[10]   235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.94, 235.91
PL_HD[10] 75.53, 75.72, 76.65, 76.43, 76.58, 63.34, 266.49, 246.52, 220.81, 242.11
*/
            Test.ensureEqual(results, expected, "results=" + results);
            File2.delete(fileName);
            if (true) throw new RuntimeException("shouldn't get here");
        } catch (OutOfMemoryError oome) {
            Test.knownProblem(
                "THREDDS OutOfMemoryError. I reported it to John Caron.",
                "2012-03-02 A TDS problem. I reported it to John Caron:\n" +
                MustBe.throwableToString(oome));
//OpendapHelper.getPrimitiveArrays ?flag[0:10:99]
//Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
//        at dods.dap.BaseTypePrimitiveVector.setLength(BaseTypePrimitiveVector.java:69)
//        at dods.dap.DVector.deserialize(DVector.java:221)
//        at dods.dap.DataDDS.readData(DataDDS.java:75)
//        at dods.dap.DConnect.getDataFromUrl(DConnect.java:523)
//        at dods.dap.DConnect.getData(DConnect.java:450)
//        at dods.dap.DConnect.getData(DConnect.java:633)
//        at gov.noaa.pfel.coastwatch.griddata.OpendapHelper.getPrimitiveArrays(OpendapHelper.java:458)
//        at gov.noaa.pfel.coastwatch.griddata.OpendapHelper.dapToNc(OpendapHelper.java:1398)
//        at gov.noaa.pfel.coastwatch.griddata.OpendapHelper.testDapToNcDArray(OpendapHelper.java:1628)
//        at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:723)
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error." +
                "\nOutOfMememoryError from TDS bug was expected." + 
                "\n(server timed out 2013-10-24)"); 
        }


        //test DArray error caused by history having different dimensions
        String2.log("\n*** test DArray error cause by history having different dimensions");
        try {
            dapToNc(dArrayUrl,
                new String[] {"zztop", "time", "lat", "lon", "PL_HD", "history"}, null, //projection
                fileName, false); //jplMode
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            results = t.toString();
expected = 
"java.lang.RuntimeException: ERROR in OpendapHelper.dapToNc\n" +
"  url=http://coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc\n" +
"  varNames=zztop,time,lat,lon,PL_HD,history  projection=null\n" +
"  file=C:/programs/tomcat/webapps/cwexperimental/WEB-INF/temp/testDapToNcDArraySubset.nc\n" +
"var=history has different dimensions than previous vars.";
            if (results.indexOf("java.net.ConnectException: Connection timed out: connect") >= 0)
                String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                    "\nurl=" + dArrayUrl +
                    "\n(The server timed out 2013-10-24.)"); 
            else Test.ensureEqual(results, expected, "results=" + results);
        }

        String2.log("\n*** OpendapHelper.testDapToNcDArray finished.");
    }


    /** This tests dapToNc DGrid. */
    public static void testDapToNcDGrid() throws Throwable {
        String2.log("\n\n*** OpendapHelper.testDapToNcDGrid");
        String fileName, expected, results;      
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        fileName = SSR.getTempDirectory() + "testDapToNcDGrid.nc";
        String dGridUrl = "http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday";
        dapToNc(dGridUrl, 
            //note that request for zztop is ignored (because not found)
            new String[] {"zztop", "x_wind", "y_wind"}, "[5][0][0:200:1200][0:200:2880]", //projection
            fileName, false); //jplMode
        results = NcHelper.dumpString(fileName, true); //printData
        expected = 
"netcdf testDapToNcDGrid.nc {\n" +
"  dimensions:\n" +
"    time = 1;\n" +
"    altitude = 1;\n" +
"    latitude = 7;\n" +
"    longitude = 15;\n" +
"  variables:\n" +
"    double time(time=1);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 9.348048E8, 1.2556944E9; // double\n" +
"      :axis = \"T\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Centered Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double altitude(altitude=1);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"up\";\n" +
"      :actual_range = 10.0, 10.0; // double\n" +
"      :axis = \"Z\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Altitude\";\n" +
"      :positive = \"up\";\n" +
"      :standard_name = \"altitude\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double latitude(latitude=7);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = -75.0, 75.0; // double\n" +
"      :axis = \"Y\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double longitude(longitude=15);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 0.0, 360.0; // double\n" +
"      :axis = \"X\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float x_wind(time=1, altitude=1, latitude=7, longitude=15);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 15.0; // double\n" +
"      :colorBarMinimum = -15.0; // double\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Zonal Wind\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"x_wind\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"    float y_wind(time=1, altitude=1, latitude=7, longitude=15);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 15.0; // double\n" +
"      :colorBarMinimum = -15.0; // double\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Meridional Wind\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"y_wind\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :composite = \"true\";\n" +
"  :contributor_name = \"Remote Sensing Systems, Inc.\";\n" +
"  :contributor_role = \"Source of level 2 data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"erd.data@noaa.gov\";\n" +
"  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :creator_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :date_created = \"2010-07-02Z\";\n" +
"  :date_issued = \"2010-07-02Z\";\n" +
"  :defaultGraphQuery = \"&.draw=vectors\";\n" +
"  :Easternmost_Easting = 360.0; // double\n" +
"  :geospatial_lat_max = 75.0; // double\n" +
"  :geospatial_lat_min = -75.0; // double\n" +
"  :geospatial_lat_resolution = 0.125; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 360.0; // double\n" +
"  :geospatial_lon_min = 0.0; // double\n" +
"  :geospatial_lon_resolution = 0.125; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 10.0; // double\n" +
"  :geospatial_vertical_min = 10.0; // double\n" +
"  :geospatial_vertical_positive = \"up\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"Remote Sensing Systems, Inc.\n" +
"2010-07-02T15:36:22Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
today + "T";  // + time " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/mday\n" +
//today + " http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.das\";\n" +
String expected2 = 
"  :infoUrl = \"http://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n" +
"  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"altitude, atmosphere,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"atmospheric, coast, coastwatch, data, degrees, global, noaa, node, ocean, oceans,\n" +
"Oceans > Ocean Winds > Surface Winds,\n" +
"QSux10, quality, quikscat, science, science quality, seawinds, surface, time, wcn, west, wind, winds, x_wind, zonal\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfel.coastwatch\";\n" +
"  :Northernmost_Northing = 75.0; // double\n" +
"  :origin = \"Remote Sensing Systems, Inc.\";\n" +
"  :processing_level = \"3\";\n" +
"  :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
"  :projection = \"geographic\";\n" +
"  :projection_type = \"mapped\";\n" +
"  :publisher_email = \"erd.data@noaa.gov\";\n" +
"  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :publisher_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :references = \"RSS Inc. Winds: http://www.remss.com/ .\";\n" +
"  :satellite = \"QuikSCAT\";\n" +
"  :sensor = \"SeaWinds\";\n" +
"  :source = \"satellite observation: QuikSCAT, SeaWinds\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :Southernmost_Northing = -75.0; // double\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :summary = \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meridional, and modulus sets. The reference height for all wind velocities is 10 meters. (This is a monthly composite.)\";\n" +
"  :time_coverage_end = \"2009-10-16T12:00:00Z\";\n" +
"  :time_coverage_start = \"1999-08-16T12:00:00Z\";\n" +
"  :title = \"Wind, QuikSCAT SeaWinds, 0.25°, Global, Science Quality, 1999-2009 (Monthly)\";\n" +
"  :Westernmost_Easting = 0.0; // double\n" +
" data:\n" +
"time =\n" +
"  {9.48024E8}\n" +
"altitude =\n" +
"  {10.0}\n" +
"latitude =\n" +
"  {-75.0, -50.0, -25.0, 0.0, 25.0, 50.0, 75.0}\n" +
"longitude =\n" +
"  {0.0, 25.0, 50.0, 75.0, 100.0, 125.0, 150.0, 175.0, 200.0, 225.0, 250.0, 275.0, 300.0, 325.0, 350.0}\n" +
"x_wind =\n" +
"  {\n" +
"    {\n" +
"      {\n" +
"        {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 0.76867574, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0},\n" +
"        {6.903795, 7.7432585, 8.052648, 7.375461, 8.358787, 7.5664454, 4.537408, 4.349131, 2.4506109, 2.1340106, 6.4230127, 8.5656395, 5.679372, 5.775274, 6.8520603},\n" +
"        {-3.513153, -9999999.0, -5.7222853, -4.0249896, -4.6091595, -9999999.0, -9999999.0, -3.9060166, -1.821446, -2.0546885, -2.349195, -4.2188687, -9999999.0, -0.7905332, -3.715024},\n" +
"        {0.38850072, -9999999.0, -2.8492346, 0.7843591, -9999999.0, -0.353197, -0.93183184, -5.3337674, -7.8715024, -5.2341905, -2.1567967, 0.46681255, -9999999.0, -3.7223456, -1.3264368},\n" +
"        {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -4.250928, -1.9779109, -2.3081408, -6.070514, -3.4209945, 2.3732827, -3.4732149, -3.2282434, -3.99131, -9999999.0},\n" +
"        {2.3816996, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 1.9863724, 1.746363, 5.305478, 2.3346918, -9999999.0, -9999999.0, 2.0079596, 3.4320266, 1.8692436},\n" +
"        {0.83961326, -3.4395192, -3.1952338, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -2.9099085}\n" +
"      }\n" +
"    }\n" +
"  }\n" +
"y_wind =\n" +
"  {\n" +
"    {\n" +
"      {\n" +
"        {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 3.9745862, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0},\n" +
"        {-1.6358501, -2.1310546, -1.672539, -2.8083494, -1.7282568, -2.5679686, -0.032763753, 0.6524638, 0.9784334, -2.4545083, 0.6344165, -0.5887741, -0.6837046, -0.92711323, -1.9981208},\n" +
"        {3.7522712, -9999999.0, -0.04178731, 1.6603879, 5.321683, -9999999.0, -9999999.0, 1.5633415, -0.50912154, -2.964269, -0.92438585, 3.959174, -9999999.0, -2.2249718, 0.46982485},\n" +
"        {4.8992314, -9999999.0, -4.7178936, -3.2770228, -9999999.0, -2.8111093, -0.9852706, 0.46997508, 0.0683085, 0.46172503, 1.2998049, 3.5235379, -9999999.0, 1.1354263, 4.7139735},\n" +
"        {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -5.092368, -3.3667018, -0.60028434, -0.7609817, -1.114303, -3.6573937, -0.934499, -0.40036556, -2.5770886, -9999999.0},\n" +
"        {0.56877106, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -3.2394278, 0.45922723, -0.8394715, 0.7333555, -9999999.0, -9999999.0, -2.3936603, 3.725975, 0.09879057},\n" +
"        {-6.128998, 2.379096, 7.463917, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -11.026609}\n" +
"      }\n" +
"    }\n" +
"  }\n" +
"}\n";
/*From .asc request:
http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.asc?x_wind[5][0][0:200:1200][0:200:2880],y_wind[5][0][0:200:1200][0:200:2880]
x_wind.x_wind[1][1][7][15]
[0][0][0], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 0.76867574, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0
[0][0][1], 6.903795, 7.7432585, 8.052648, 7.375461, 8.358787, 7.5664454, 4.537408, 4.349131, 2.4506109, 2.1340106, 6.4230127, 8.5656395, 5.679372, 5.775274, 6.8520603
[0][0][2], -3.513153, -9999999.0, -5.7222853, -4.0249896, -4.6091595, -9999999.0, -9999999.0, -3.9060166, -1.821446, -2.0546885, -2.349195, -4.2188687, -9999999.0, -0.7905332, -3.715024
[0][0][3], 0.38850072, -9999999.0, -2.8492346, 0.7843591, -9999999.0, -0.353197, -0.93183184, -5.3337674, -7.8715024, -5.2341905, -2.1567967, 0.46681255, -9999999.0, -3.7223456, -1.3264368
[0][0][4], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -4.250928, -1.9779109, -2.3081408, -6.070514, -3.4209945, 2.3732827, -3.4732149, -3.2282434, -3.99131, -9999999.0
[0][0][5], 2.3816996, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 1.9863724, 1.746363, 5.305478, 2.3346918, -9999999.0, -9999999.0, 2.0079596, 3.4320266, 1.8692436
[0][0][6], 0.83961326, -3.4395192, -3.1952338, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -2.9099085
y_wind.y_wind[1][1][7][15]
[0][0][0], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 3.9745862, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0
[0][0][1], -1.6358501, -2.1310546, -1.672539, -2.8083494, -1.7282568, -2.5679686, -0.032763753, 0.6524638, 0.9784334, -2.4545083, 0.6344165, -0.5887741, -0.6837046, -0.92711323, -1.9981208
[0][0][2], 3.7522712, -9999999.0, -0.04178731, 1.6603879, 5.321683, -9999999.0, -9999999.0, 1.5633415, -0.50912154, -2.964269, -0.92438585, 3.959174, -9999999.0, -2.2249718, 0.46982485
[0][0][3], 4.8992314, -9999999.0, -4.7178936, -3.2770228, -9999999.0, -2.8111093, -0.9852706, 0.46997508, 0.0683085, 0.46172503, 1.2998049, 3.5235379, -9999999.0, 1.1354263, 4.7139735
[0][0][4], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -5.092368, -3.3667018, -0.60028434, -0.7609817, -1.114303, -3.6573937, -0.934499, -0.40036556, -2.5770886, -9999999.0
[0][0][5], 0.56877106, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -3.2394278, 0.45922723, -0.8394715, 0.7333555, -9999999.0, -9999999.0, -2.3936603, 3.725975, 0.09879057
[0][0][6], -6.128998, 2.379096, 7.463917, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -11.026609
*/
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);
        int po = results.indexOf("  :infoUrl =");
        Test.ensureEqual(results.substring(po), expected2, "results=" + results);
        File2.delete(fileName);

        //test 1D var should be ignored if others are 2+D
        String2.log("\n*** test 1D var should be ignored if others are 2+D");
        fileName = SSR.getTempDirectory() + "testDapToNcDGrid1D2D.nc";
        dapToNc(dGridUrl,
            new String[] {"zztop", "x_wind", "y_wind", "latitude"}, 
            "[5][0][0:200:1200][0:200:2880]", //projection
            fileName, false); //jplMode
        results = NcHelper.dumpString(fileName, false); //printData
        expected = 
"netcdf testDapToNcDGrid1D2D.nc {\n" +
"  dimensions:\n" +
"    time = 1;\n" +
"    altitude = 1;\n" +
"    latitude = 7;\n" +
"    longitude = 15;\n" +
"  variables:\n" +
"    double time(time=1);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 9.348048E8, 1.2556944E9; // double\n" +
"      :axis = \"T\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Centered Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double altitude(altitude=1);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"up\";\n" +
"      :actual_range = 10.0, 10.0; // double\n" +
"      :axis = \"Z\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Altitude\";\n" +
"      :positive = \"up\";\n" +
"      :standard_name = \"altitude\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double latitude(latitude=7);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = -75.0, 75.0; // double\n" +
"      :axis = \"Y\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double longitude(longitude=15);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 0.0, 360.0; // double\n" +
"      :axis = \"X\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float x_wind(time=1, altitude=1, latitude=7, longitude=15);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 15.0; // double\n" +
"      :colorBarMinimum = -15.0; // double\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Zonal Wind\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"x_wind\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"    float y_wind(time=1, altitude=1, latitude=7, longitude=15);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 15.0; // double\n" +
"      :colorBarMinimum = -15.0; // double\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Meridional Wind\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"y_wind\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :composite = \"true\";\n" +
"  :contributor_name = \"Remote Sensing Systems, Inc.\";\n" +
"  :contributor_role = \"Source of level 2 data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"erd.data@noaa.gov\";\n" +
"  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :creator_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :date_created = \"2010-07-02Z\";\n" +
"  :date_issued = \"2010-07-02Z\";\n" +
"  :defaultGraphQuery = \"&.draw=vectors\";\n" +
"  :Easternmost_Easting = 360.0; // double\n" +
"  :geospatial_lat_max = 75.0; // double\n" +
"  :geospatial_lat_min = -75.0; // double\n" +
"  :geospatial_lat_resolution = 0.125; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 360.0; // double\n" +
"  :geospatial_lon_min = 0.0; // double\n" +
"  :geospatial_lon_resolution = 0.125; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 10.0; // double\n" +
"  :geospatial_vertical_min = 10.0; // double\n" +
"  :geospatial_vertical_positive = \"up\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"Remote Sensing Systems, Inc.\n" +
"2010-07-02T15:36:22Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
today + "T"; //time http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/mday\n" +
//today + time " http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.das\";\n" +
expected2 = 
"  :infoUrl = \"http://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n" +
"  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"altitude, atmosphere,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"atmospheric, coast, coastwatch, data, degrees, global, noaa, node, ocean, oceans,\n" +
"Oceans > Ocean Winds > Surface Winds,\n" +
"QSux10, quality, quikscat, science, science quality, seawinds, surface, time, wcn, west, wind, winds, x_wind, zonal\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfel.coastwatch\";\n" +
"  :Northernmost_Northing = 75.0; // double\n" +
"  :origin = \"Remote Sensing Systems, Inc.\";\n" +
"  :processing_level = \"3\";\n" +
"  :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
"  :projection = \"geographic\";\n" +
"  :projection_type = \"mapped\";\n" +
"  :publisher_email = \"erd.data@noaa.gov\";\n" +
"  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :publisher_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :references = \"RSS Inc. Winds: http://www.remss.com/ .\";\n" +
"  :satellite = \"QuikSCAT\";\n" +
"  :sensor = \"SeaWinds\";\n" +
"  :source = \"satellite observation: QuikSCAT, SeaWinds\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :Southernmost_Northing = -75.0; // double\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :summary = \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meridional, and modulus sets. The reference height for all wind velocities is 10 meters. (This is a monthly composite.)\";\n" +
"  :time_coverage_end = \"2009-10-16T12:00:00Z\";\n" +
"  :time_coverage_start = \"1999-08-16T12:00:00Z\";\n" +
"  :title = \"Wind, QuikSCAT SeaWinds, 0.25°, Global, Science Quality, 1999-2009 (Monthly)\";\n" +
"  :Westernmost_Easting = 0.0; // double\n" +
" data:\n" +
"}\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);
        po = results.indexOf("  :infoUrl =");
        Test.ensureEqual(results.substring(po), expected2, "results=" + results);
        File2.delete(fileName);



        /* */
        String2.log("\n*** OpendapHelper.testDapToNcDGrid finished.");

    }

    

    /** This tests parseStartStrideStop and throws exception if trouble.*/
    public static void testParseStartStrideStop() {

        Test.ensureEqual(String2.toCSSVString(parseStartStrideStop(null)), "", ""); 
        Test.ensureEqual(String2.toCSSVString(parseStartStrideStop("")), "", ""); 
        Test.ensureEqual(String2.toCSSVString(parseStartStrideStop("[6:7:8]")), 
            "6, 7, 8", ""); 
        Test.ensureEqual(String2.toCSSVString(parseStartStrideStop("[5][3:4][6:7:8]")), 
            "5, 1, 5, 3, 1, 4, 6, 7, 8", ""); 
        try {
            parseStartStrideStop("a");
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"a\": '[' expected at projection position #0", 
                ""); 
        }
        try {
            parseStartStrideStop("[");
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[\": End ']' not found.", 
                ""); 
        }
        try {
            parseStartStrideStop("[5");
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[5\": End ']' not found.", 
                ""); 
        }
        try {
            parseStartStrideStop("[5:t]");
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.NumberFormatException: For input string: \"t\"", 
                ""); 
        }
        try {
            parseStartStrideStop("[-1]");
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[-1]\": Negative number=-1 at projection position #1", 
                ""); 
        }
        try {
            parseStartStrideStop("[0:1:2:3]");
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.NumberFormatException: For input string: \"2:3\"", 
                ""); 
        }
        try {
            parseStartStrideStop("[4:3]");
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[4:3]\": start=4 must be less than or equal to stop=3", 
                ""); 
        }
        try {
            parseStartStrideStop("[4:2:3]");
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[4:2:3]\": start=4 must be less than or equal to stop=3", 
                ""); 
        }

        //test calculateNValues
        Test.ensureEqual(calculateNValues(1, 1, 3), 3, "");
        Test.ensureEqual(calculateNValues(1, 2, 3), 2, "");
        Test.ensureEqual(calculateNValues(1, 2, 4), 2, "");
        try {
            calculateNValues(4,2,3);
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.RuntimeException: start=4 must be less than or equal to stop=3", 
                ""); 
        }
        try {
            calculateNValues(3,0,5);
            Test.ensureEqual(0, 1, "");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(), 
                "java.lang.RuntimeException: stride=0 must be greater than 0", 
                ""); 
        }       
    }

    /**
     * This tests the methods in this class.
     */
    public static void test() throws Throwable{
        String2.log("\n*** OpendapHelper.test...");

/* 
        testGetAttributes();
        testParseStartStrideStop();
        testFindVarsWithSharedDimensions();
        testFindAllScalarOrMultiDimVars();
        testDapToNcDArray();
   */     testDapToNcDGrid();
        testAllDapToNc(-1);  //-1 for all tests, or 0.. for specific test

        String2.log("\n***** OpendapHelper.test finished successfully");
        Math2.incgc(2000); //in a test
    } 



}
