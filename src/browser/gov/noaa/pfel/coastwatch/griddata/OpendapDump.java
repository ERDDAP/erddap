/* 
 * OpendapDump Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import java.util.Arrays;
import java.util.Enumeration;
import dods.dap.*;

public class OpendapDump {

    /**
     * This creates a String with 'indent' spaces.
     * @param indent 
     * @param a string with 'indent' spaces
     */
    private static String indentation(int indent) {
        char[] ca = new char[indent];
        Arrays.fill(ca, ' ');
        return new String(ca);
    }

    /**
     * This dumps the contents of an Opendap response.
     */
    public static void main(String args[]) throws Exception {
        boolean getDas = true;
        boolean getDds = true;
        boolean getData = true;
        boolean dumpData = false;
        boolean verbose = true;
        boolean acceptDeflate = true;
        String urlName = 
            //"https://data.nodc.noaa.gov/cgi-bin/nph-dods/pathfinder/Version5.0/5day/1990/1990001-1990005.s0451pfv50-sst-16b.hdf";
            //"https://data.nodc.noaa.gov/cgi-bin/nph-dods/pathfinder/Version5.0/Monthly/1985/198501.m04m1pfv50-qual.hdf";
            "http://las.pfeg.noaa.gov/cgi-bin/nph-dods/data/oceanwatch/nrt/gac/AG1day.nc";
        String expr = 
            //"?qual[0:2:20][0:2:20]";
            //"?qual[0:2:20][0:2:20]";
            //"?ssta[0][0:2:20][0:2:20]";  
            //"?lat";  
            //"?numberOfObservations.numberOfObservations";
            "?ssta";
        int indent = 0;

        //connect
        String2.log("URL = " + urlName);
        DConnect dConnect = null;
        long time = System.currentTimeMillis();
        dConnect = new DConnect(urlName, acceptDeflate, 1, 1);
        String2.log("DConnect time=" + (System.currentTimeMillis() - time) + "ms");

        //getDas
        if (getDas) {
            time = System.currentTimeMillis();
            DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
            String2.log("getDAS time=" + (System.currentTimeMillis() - time) + "ms");
            String2.log("DAS:");
            //das.print(System.out); 
            indent = 2;
            //das is read-only so no need to use sychronized(das)
            Enumeration names = das.getNames();
            while (names.hasMoreElements()) {
                String varName = (String)names.nextElement();
                String2.log(indentation(indent) + varName + ": ");
                AttributeTable at = das.getAttributeTable(varName);
                Enumeration e = at.getNames();
                while (e.hasMoreElements()) {
                    //All names and values stored as Strings.
                    //Values which really are Strings have " as first and list character.
                    Attribute a = at.getAttribute((String)e.nextElement());
                    String2.log(indentation(indent + 2) + a.getName() + " = " +
                       String2.toCSSVString(String2.toArrayList(a.getValues()).toArray()));
                }
                //or use String sa[]=String2.toStringArray(String2.toArrayList(at.getAttribute("range")).toArray());
                //or use String sa[]=String2.toIntArray(String2.toArrayList(at.getAttribute("range")).toArray());
                //or use Attribute a = at.getAttribute(attributeName);
            }
            indent = 0;
        }

        //getDds 
        if (getDds) {
            time = System.currentTimeMillis();
            DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
            String2.log("getDDS time=" + (System.currentTimeMillis() - time) + "ms");
            String2.log("BEGIN DDS:");
            //dds.print(System.out);
            Enumeration e = dds.getVariables();
            while (e.hasMoreElements()) {
                decodeBaseType((BaseType)e.nextElement(), indent + 2);
            }
            String2.log("END DDS");

        }

        //getData
        if (getData) {
            if ((expr.length() == 0) && (urlName.indexOf('?') == -1)) {
                String2.log("You must supply a constraint expression with -D.");
            } else {
                try {
                    time = System.currentTimeMillis();
                    DataDDS dds = dConnect.getData(expr, null);
                    String2.log("getData time=" + (System.currentTimeMillis() - time) + "ms");
                    //if (dumpData) 
                    //    dds.externalize(System.out, compress, true);
                    //else 
                    //    dds.printVal(System.out);

                    String2.log("Begin enumeration to get variables...");
                    Enumeration e = dds.getVariables();
                    while (e.hasMoreElements()) 
                        decodeBaseType((BaseType)e.nextElement(), indent + 2);
                    String2.log("End enumeration to get variables...");

                } catch (Exception e) {
                    String2.log(MustBe.throwableToString(e));
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    /**
     * This gets the information from a BaseType.
     * 
     * @param bt 
     * @param indent the number of spaces to indent the diagnostic messages
     */
    public static void decodeBaseType(BaseType bt, int indent) {
        String2.log(indentation(indent) + "decodeBaseType name = " + bt.getName());
        if (bt == null) {
            String2.log(indentation(indent) + "null baseType");
        } else if (bt instanceof DBoolean db) {
            boolean b = db.getValue();
            String2.log(indentation(indent) + "boolean = " + b);
        } else if (bt instanceof DByte db) {
            byte b = db.getValue();
            String2.log(indentation(indent) + "byte = " + b);
        } else if (bt instanceof DFloat32 df) {
            float f = df.getValue();
            String2.log(indentation(indent) + "float = " + f);
        } else if (bt instanceof DFloat64 df) {
            double d = df.getValue();
            String2.log(indentation(indent) + "double = " + d);
        } else if (bt instanceof DInt16 di) {
            short s = di.getValue();
            String2.log(indentation(indent) + "int16 = " + s);
        } else if (bt instanceof DInt32 di) {
            int i = di.getValue();
            String2.log(indentation(indent) + "int32 = " + i);
        } else if (bt instanceof DString ds) {
            String s = ds.getValue();
            String2.log(indentation(indent) + "string = " + s);
        } else if (bt instanceof DArray da) {
            String2.log(indentation(indent) + "  Begin DArray");
            Enumeration e = da.getDimensions();
            while (e.hasMoreElements()) {
                DArrayDimension dam = (DArrayDimension)e.nextElement();
                String2.log(indentation(indent) + "    dimension " + dam.getName() + "[" + 
                    //dam.getStart() + ":" +  //start is always 0
                    //dam.getStride() + ":" + //stride is always 1
                    dam.getStop() + "]");   //stop is inclusive; so nValues = getStop()+1
            }             
            decodePrimitiveVector(da.getPrimitiveVector(), indent + 4);
            String2.log(indentation(indent) + "  End DArray");
        } else if (bt instanceof DList dl) { 
            String2.log(indentation(indent) + "  Begin DList");
            decodePrimitiveVector(dl.getPrimitiveVector(), indent + 4);
            String2.log(indentation(indent) + "  End DList");
        } else if (bt instanceof DGrid dg) {
            String2.log(indentation(indent) + "  Begin DGrid");
            Enumeration e = dg.getVariables();
            while (e.hasMoreElements()) {
                decodeBaseType((BaseType)e.nextElement(), indent + 4);
            }
            String2.log(indentation(indent) + "  End DGrid");
        } else if (bt instanceof DStructure ds) {
            String2.log(indentation(indent) + "  Begin DStructure");
            Enumeration e = ds.getVariables();
            while (e.hasMoreElements()) {
                decodeBaseType((BaseType)e.nextElement(), indent + 4);
            }
            String2.log(indentation(indent) + "  End DStructure");
        } else if (bt instanceof DSequence ds) {
            String2.log(indentation(indent) + "  Begin DSequence");
            Enumeration e = ds.getVariables();
            while (e.hasMoreElements()) {
                decodeBaseType((BaseType)e.nextElement(), indent + 4);
            }
            String2.log(indentation(indent) + "  End DSequence");
        } else String2.log(indentation(indent) + "  Unknown basetype = " + bt.toString());
    }

    /**
     * This gets the information from a PrimitiveVector.
     * 
     * @param pv 
     * @param indent the number of spaces to indent the diagnostic messages
     */
    public static void decodePrimitiveVector(PrimitiveVector pv, int indent) {
        if (pv == null) {
            String2.log(indentation(indent) + "null PrimitiveVector");
            return;
        }
        if (pv.getInternalStorage() == null) {
            String2.log(indentation(indent) + "null PrimitiveVector internal storage");
            return;
        }
        
        int length = pv.getLength();
        if (pv instanceof BaseTypePrimitiveVector t) {
            String2.log(indentation(indent) + "Begin BaseTypePrimitiveVector");
            for (int i = 0; i < length; i++)
                decodeBaseType(t.getValue(i), indent + 2);
            String2.log(indentation(indent) + "End BaseTypePrimitiveVector");
        } else if (pv instanceof BooleanPrimitiveVector t) {
            boolean bFirst = t.getValue(0);
            boolean bLast  = t.getValue(length-1);
            String2.log(indentation(indent) + "BooleanPrimitiveVector[0] = " + bFirst);
            String2.log(indentation(indent) + "BooleanPrimitiveVector[" + (length-1) + "] = " + bLast);
        } else if (pv instanceof BytePrimitiveVector t) {
            byte bFirst = t.getValue(0);
            byte bLast  = t.getValue(length-1);
            String2.log(indentation(indent) + "BytePrimitiveVector[0] = " + bFirst);
            String2.log(indentation(indent) + "BytePrimitiveVector[" + (length-1) + "] = " + bLast);
        } else if (pv instanceof Float32PrimitiveVector t) {
            float fFirst = t.getValue(0);
            float fLast  = t.getValue(length-1);
            String2.log(indentation(indent) + "Float32PrimitiveVector[0] = " + fFirst);
            String2.log(indentation(indent) + "Float32PrimitiveVector[" + (length-1) + "] = " + fLast);
        } else if (pv instanceof Float64PrimitiveVector t) {
            double dFirst = t.getValue(0);
            double dLast  = t.getValue(length-1);
            String2.log(indentation(indent) + "Float64PrimitiveVector[0] = " + dFirst);
            String2.log(indentation(indent) + "Float64PrimitiveVector[" + (length-1) + "] = " + dLast);
        } else if (pv instanceof Int16PrimitiveVector t) {
            short sFirst = t.getValue(0);
            short sLast  = t.getValue(length-1);
            String2.log(indentation(indent) + "Int16PrimitiveVector[0] = " + sFirst);
            String2.log(indentation(indent) + "Int16PrimitiveVector[" + (length-1) + "] = " + sLast);
        } else if (pv instanceof Int32PrimitiveVector t) {
            int iFirst = t.getValue(0);
            int iLast  = t.getValue(length-1);
            String2.log(indentation(indent) + "Int32PrimitiveVector[0] = " + iFirst);
            String2.log(indentation(indent) + "Int32PrimitiveVector[" + (length-1) + "] = " + iLast);
        } else String2.log(indentation(indent) + "Unknown PrimitiveVector type = " + pv.toString());
    }
}