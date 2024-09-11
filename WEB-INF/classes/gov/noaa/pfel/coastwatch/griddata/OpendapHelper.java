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
import dods.dap.*;
import gov.noaa.pfel.coastwatch.util.SSR;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * This class has some static convenience methods related to Opendap and the other Data classes.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-12-07
 */
public class OpendapHelper {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Set this to true (by calling debug=true in your program, not by changing the code here) if you
   * want lots of diagnostic messages sent to String2.log.
   */
  public static boolean debug = false;

  /**
   * This defines the end-of-line characters to use when writing dap responses. <br>
   * The DAP2 specification says the end-of-line marker in a DODS data stream "MUST use CR LF to
   * conform to DAP2" (James Gallagher's email to opendap-tech mailing list on Aug 28, 2007). <br>
   * But, the JDAP library only seems to be able to read DODS data streams that use just LF. It does
   * not work with DODS data streams that use CR LF. This can be confirmed by looking at the
   * definition of endSequence in HeaderInputStream and by seeing how it is used in getMoreBytes (in
   * that class). <br>
   * (What about opendap C library?) <br>
   * For now, to be safe and compatible (although not compliant), use just LF.
   */
  public static final String EOL = "\n";

  public static int DEFAULT_TIMEOUT = 120000; // 2 minutes in millis

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
   * This sets attributes for a variable from the das. See the more commonly used
   * getOpendapAttributes.
   *
   * @param das the das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
   * @param variableName the name of the desired variable (use "GLOBAL" for global attributes).
   *     Composite names, e.g., MOC1.temperature, are allowed.
   * @return the desired attributeTable (or null if not found)
   */
  public static AttributeTable getAttributeTable(DAS das, String variableName) {
    if (variableName.equals("GLOBAL")) {
      // find the GLOBAL attributes
      // this assumes that GLOBAL is in the name (I've see GLOBAL and NC_GLOBAL)
      Enumeration names = das.getNames();
      while (names.hasMoreElements()) {
        String s = (String) names.nextElement();
        if (s.indexOf("GLOBAL") >= 0) {
          return das.getAttributeTable(s);
        }
      }
      return null;
    }

    AttributeTable at = das.getAttributeTable(variableName);
    if (at != null) return at;

    // is it in a parent attribute table?   (MOC1.temperature may be in MOC1's attributeTable)
    String sa[] = String2.split(variableName, '.');
    if (sa.length == 1) return null;
    for (int i = 0; i < sa.length; i++) {
      if (i == 0) {
        at = das.getAttributeTable(sa[i]);
      } else {
        Attribute a = at.getAttribute(sa[i]);
        if (a == null) {
          // String2.log("getAttributeTable: attribute #" + i + "=" + sa[i] + " is null.");
          return null;
        }
        if (a.getType() != Attribute.CONTAINER) {
          // String2.log("getAttributeTable: attribute #" + i + "=" + sa[i] +
          //    " type=" + a.getType() + " is not CONTAINER.");
          return null;
        }
        at = a.getContainer();
      }
      // String2.log("getAttributeTable: attTable #" + i + "=" + sa[i] + " is " + (at == null?
      // "null." : "not null."));
      if (at == null) return null;
    }
    return at;
  }

  /**
   * This sets attributes for a variable from the das. See javadocs for the other
   * getOpendapAttributes().
   *
   * @param das the das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
   * @param variableName the name of the desired variable (use "GLOBAL" for global attributes)
   * @param attributes the Attributes to which attributes will be added
   * @throws Exception if trouble. No attributeTable for variableName is not an error; it just
   *     doesn't change 'attributes'.
   */
  public static void getAttributes(DAS das, String variableName, Attributes attributes) {

    // get the attributeTable
    AttributeTable attributeTable = getAttributeTable(das, variableName);
    if (attributeTable == null) {
      if (verbose) String2.log("  No attributeTable for " + variableName + ".");
      return;
    }
    getAttributes(attributeTable, attributes);
    if (verbose)
      String2.log(
          "  attributeTable for " + variableName + " has " + attributes.size() + " attribute(s).");
  }

  /**
   * This sets attributes from the information in the attributeTable.
   *
   * <p>Currently this converts url, UNKNOWN, and unknown attributes to Strings.
   *
   * @param attributeTable It isn't an error if this is null; but nothing is done.
   * @param attributes the Attributes to which attributes will be added
   * @throws Exception if trouble.
   */
  public static void getAttributes(AttributeTable attributeTable, Attributes attributes) {

    if (attributeTable == null) return;

    // get the attributes
    Enumeration names = attributeTable.getNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      Attribute attribute = attributeTable.getAttribute(name);
      if (attribute.isContainer()) {
        // process an attribute that isContainer by flattening it (name_subname=...)
        // http://dm1.caricoos.org/thredds/dodsC/content/wrf_archive/wrfout_d01_2009-09-25_12_00_00.nc.das
        // this works recursively, so should handle containers of containers of ...
        Attributes tAttributes = new Attributes();
        getAttributes(attribute.getContainer(), tAttributes);
        String tNames[] = tAttributes.getNames();
        String startName = name.trim() + "_";
        int ntNames = tNames.length;
        for (int tni = 0; tni < ntNames; tni++)
          attributes.add(startName + tNames[tni].trim(), tAttributes.get(tNames[tni]));

      } else {
        // process a simple attribute
        String[] sar = String2.toStringArray(String2.toArrayList(attribute.getValues()).toArray());

        // remove enclosing quotes from strings
        for (int i = 0; i < sar.length; i++) {
          int sariLength = sar[i].length();
          if (sariLength >= 2 && sar[i].charAt(0) == '"' && sar[i].charAt(sariLength - 1) == '"')
            sar[i] = String2.fromJson(sar[i]);
        }
        StringArray sa = new StringArray(sar);

        // store values in the appropriate type of PrimitiveArray
        PrimitiveArray pa = null;
        int type = attribute.getType();
        if (type == Attribute.FLOAT32) pa = new FloatArray();
        else if (type == Attribute.FLOAT64) pa = new DoubleArray();
        else if (type == Attribute.INT32) pa = new IntArray();
        else if (type == Attribute.UINT32) pa = new UIntArray();
        else if (type == Attribute.INT16) pa = new ShortArray();
        else if (type == Attribute.UINT16) pa = new UShortArray();
        else if (type == Attribute.BYTE) pa = new ByteArray();
        // ignore STRING, URL, UNKNOWN, etc. (keep as StringArray)

        // move the sa data into pa
        if (pa == null) {
          // pa will be the StringArray
          pa = sa;
          // trim first part of first string and end of last string
          if (sa.size() == 1) {
            sa.set(0, sa.get(0).trim());
          } else if (sa.size() > 1) {
            sa.set(0, String2.trimStart(sa.get(0)));
            sa.set(sa.size() - 1, String2.trimEnd(sa.get(sa.size() - 1)));
          }

        } else {
          // convert to other data type
          pa.append(sa);
        }

        // store name,pa in attributes
        attributes.set(name.trim(), pa);
      }
    }
    attributes.fromNccsvStrings();

    // String2.log("\n>> OpendapHelper.getAttributes pre\n" + attributes.toString());
    // 2020-09-01 in nc3 files, if variables has _Unsigned=true, convert some signed attributes to
    // unsigned
    // String2.log(">> getVariableAttributes var=" + variable.getFullName() + " _Unsigned=" +
    // attributes.getString("_Unsigned"));
    // leave attribute in so caller can tell if _Unsigned.
    if ("true".equals(attributes.getString("_Unsigned"))) {
      attributes.convertSomeSignedToUnsigned();
      // String2.log("\n>> OpendapHelper.getAttributes post\n" + attributes.toString());
    }
  }

  /**
   * This gets the values for one of the attributes for one of the variables from the DAS. But for
   * more than 2 calls to this, it is more efficient to use getAttributesFromOpendap. This won't
   * throw an exception.
   *
   * @param das the das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
   * @param variableName e.g. "lat". Use "GLOBAL" for global attributes (even though they are called
   *     different things in different files).
   * @param attributeName e.g., "actual_range"
   * @return the values associated with the attribute (array.length = 0 if not found) (e.g.,
   *     {"even"} or {"0.", "360."}).
   */
  public static String[] getAttributeValues(DAS das, String variableName, String attributeName) {
    try {
      AttributeTable at = getAttributeTable(das, variableName);
      if (at == null) {
        String2.log(
            "NOTE: OpendapHelper.getAttributeValues:\n"
                + "  attributeTable not found for var="
                + variableName);
        return new String[] {};
      }

      Attribute attribute = at.getAttribute(attributeName);
      if (attribute == null) {
        String2.log(
            "NOTE: OpendapHelper.getAttributeValues:\n"
                + "  attribute not found for var="
                + variableName
                + " attributeName="
                + attributeName);
        return new String[] {};
      }

      String[] sar = String2.toStringArray(String2.toArrayList(attribute.getValues()).toArray());
      // remove enclosing quotes from strings
      for (int i = 0; i < sar.length; i++) {
        int sariLength = sar[i].length();
        if (sariLength >= 2 && sar[i].charAt(0) == '"' && sar[i].charAt(sariLength - 1) == '"')
          sar[i] = String2.fromJson(sar[i]);
      }
      return sar;
    } catch (Exception e) {
      String2.log(
          "WARNING: OpendapHelper.getAttributeValues(\nvarName="
              + variableName
              + " attributeName="
              + attributeName
              + "):"
              + MustBe.throwableToString(e));
      return new String[] {};
    }
  }

  /**
   * This gets the first value for the one of the attributes for one of the variables from the DAS.
   *
   * @param das the das from dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT)
   * @param variableName e.g. "time". Use "GLOBAL" for global attributes (even though they are
   *     called different things in different files).
   * @param attributeName e.g., "units"
   * @return the first value value associated with the attribute, represented as a String (or null
   *     if not found) e.g., "seconds since 1970-01-01"
   */
  public static String getAttributeValue(DAS das, String variableName, String attributeName) {
    String[] values = getAttributeValues(das, variableName, attributeName);
    return values.length == 0 ? null : values[0];
  }

  /**
   * Given a projection constraint ("[0:5][1][0:2:10]"), this returns an array with
   * start,stride,stop for all the dimensions.
   *
   * @param p a projection e.g., [0:5][1][0:2:10]
   * @return an array with start,stride,stop for all the dimensions. This returns [0] if projection
   *     is null or "". This throws runtime exception if invalid syntax.
   */
  public static int[] parseStartStrideStop(String p) {
    if (p == null) return new int[0];
    int pLength = p.length();
    int po = 0;
    IntArray ia = new IntArray();
    String beginError = String2.ERROR + " parsing OPENDAP constraint=\"" + p + "\": ";
    while (po < pLength) {
      if (p.charAt(po) != '[')
        throw new RuntimeException(beginError + "'[' expected at projection position #" + po);
      po++;
      int colonPo1 = p.indexOf(':', po);
      if (colonPo1 < 0) colonPo1 = pLength;
      int colonPo2 = colonPo1 == pLength ? pLength : p.indexOf(':', colonPo1 + 1);
      if (colonPo2 < 0) colonPo2 = pLength;
      int endPo = p.indexOf(']', po);
      if (endPo < 0) throw new RuntimeException(beginError + "End ']' not found.");

      if (colonPo1 > endPo) {
        // [4]
        int start = Integer.parseInt(p.substring(po, endPo));
        if (start < 0)
          throw new RuntimeException(
              beginError + "Negative number=" + start + " at projection position #" + po);
        ia.add(start);
        ia.add(1);
        ia.add(start);

      } else if (colonPo2 > endPo) {
        // [3:4]
        int start = Integer.parseInt(p.substring(po, colonPo1));
        if (start < 0)
          throw new RuntimeException(
              beginError + "Negative number=" + start + " at projection position #" + po);
        int stop = Integer.parseInt(p.substring(colonPo1 + 1, endPo));
        if (stop < 0)
          throw new RuntimeException(
              beginError
                  + "Negative number="
                  + stop
                  + " at projection position #"
                  + (colonPo1 + 1));
        if (start > stop)
          throw new RuntimeException(
              beginError + "start=" + start + " must be less than or equal to stop=" + stop);
        ia.add(start);
        ia.add(1);
        ia.add(stop);

      } else {
        // [3:4:5]
        int start = Integer.parseInt(p.substring(po, colonPo1));
        if (start < 0)
          throw new RuntimeException(
              beginError + "Negative number=" + start + " at projection position #" + po);
        int stride = Integer.parseInt(p.substring(colonPo1 + 1, colonPo2));
        if (stride < 0)
          throw new RuntimeException(
              beginError
                  + "Negative number="
                  + stride
                  + " at projection position #"
                  + (colonPo1 + 1));
        int stop = Integer.parseInt(p.substring(colonPo2 + 1, endPo));
        if (stop < 0)
          throw new RuntimeException(
              beginError
                  + "Negative number="
                  + stop
                  + " at projection position #"
                  + (colonPo2 + 1));
        if (start > stop)
          throw new RuntimeException(
              beginError + "start=" + start + " must be less than or equal to stop=" + stop);
        ia.add(start);
        ia.add(stride);
        ia.add(stop);
      }
      po = endPo + 1;
    }
    return ia.toArray();
  }

  /**
   * Given start, stride, stop, this returns the actual number of points that will be found.
   *
   * @param start
   * @param stride (must be >= 1)
   * @param stop
   * @return the actual number of points that will be found.
   */
  public static int calculateNValues(int start, int stride, int stop) {
    if (start > stop)
      throw new RuntimeException("start=" + start + " must be less than or equal to stop=" + stop);
    if (stride < 1) throw new RuntimeException("stride=" + stride + " must be greater than 0");

    return 1 + (stop - start) / stride;
  }

  /**
   * Get the PrimitiveVector from an opendap query.
   *
   * @param dConnect
   * @param query For an entire variable that is a DArray, use, for example, "?lat", or for a
   *     portion: "?lat[0:1:5]". For a portion of a DGrid, use, for example,
   *     "?ssta.ssta[23:1:23][642:1:742][339:1:439]". This should be already percent encoded as
   *     needed.
   * @return a PrimitiveVector with the data from the DArray returned by the query
   * @throws Exception if trouble
   */
  public static PrimitiveVector getPrimitiveVector(DConnect dConnect, String query)
      throws Exception {
    long time = System.currentTimeMillis();
    DataDDS dataDds = dConnect.getData(query, null);
    BaseType bt =
        (BaseType) dataDds.getVariables().nextElement(); // first element is always main array
    // bt.printVal(System.out, " ");
    DArray da = (DArray) bt;
    if (verbose)
      String2.log(
          "    OpendapHelper.getPrimitiveVector("
              + query
              + ") done. TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
    return da.getPrimitiveVector();
  }

  /**
   * Get the PrimitiveArrays from the first var from an opendap query.
   *
   * @param dConnect
   * @param query For an entire variable that is a DArray, use, for example, "?lat", or for a
   *     portion: "?lat[0:1:5]". For an entire dimension of a grid, use, for example, "?ssta.lat".
   *     For a portion of a DGrid, use, for example, "?ssta[23:1:23][642:1:742][339:1:439]", which
   *     returns 4 PrimitiveArrays: #0=main data, #1=dimension0, #2=dimension1, #3=dimension2. This
   *     should already percent encoded as needed.
   * @return an array of PrimitiveArrays with the requested data from the first var in the query
   * @throws Exception if trouble
   */
  public static PrimitiveArray[] getPrimitiveArrays(DConnect dConnect, String query)
      throws Exception {
    try {
      long time = System.currentTimeMillis();
      StringBuilder sb = new StringBuilder(query);
      String2.replaceAll(sb, "[", "%5B");
      String2.replaceAll(sb, "]", "%5D");
      query = sb.toString();
      if (verbose)
        String2.log(
            "    OpendapHelper.getPrimitiveArrays " + query
            // + "\n" + MustBe.stackTrace()
            );
      DataDDS dataDds = dConnect.getData(query, null);
      if (verbose)
        String2.log(
            "    OpendapHelper.getPrimitiveArrays done. TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
      BaseType bt =
          (BaseType) dataDds.getVariables().nextElement(); // first element is always main array
      return getPrimitiveArrays(bt);
    } catch (Exception e) {
      throw new RuntimeException(
          String2.ERROR + " in getPrimitiveArrays for query=" + query + "\n" + e.getMessage());
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
    // String2.log(">>    baseType=" + baseType.getTypeName());
    if (baseType instanceof DGrid dgrid) {
      ArrayList al = String2.toArrayList(dgrid.getVariables());
      PrimitiveArray paAr[] = new PrimitiveArray[al.size()];
      for (int i = 0; i < al.size(); i++)
        paAr[i] = getPrimitiveArray(((DArray) al.get(i)).getPrimitiveVector());
      return paAr;
    } else if (baseType instanceof DArray da) {
      return new PrimitiveArray[] {getPrimitiveArray(da.getPrimitiveVector())};
    } else if (baseType instanceof DVector dvector) {
      return new PrimitiveArray[] {getPrimitiveArray(baseType.newPrimitiveVector())};
    } else if (baseType instanceof DFloat64 dfloat64) {
      return new PrimitiveArray[] {new DoubleArray(new double[] {dfloat64.getValue()})};
    } else if (baseType instanceof DFloat32 dfloat32) {
      return new PrimitiveArray[] {new FloatArray(new float[] {dfloat32.getValue()})};
    } else if (baseType instanceof DInt32 dint32) {
      return new PrimitiveArray[] {new IntArray(new int[] {dint32.getValue()})};
    } else if (baseType instanceof DUInt32 duint32) {
      return new PrimitiveArray[] {new UIntArray(new int[] {duint32.getValue()})};
    } else if (baseType instanceof DInt16 dint16) {
      return new PrimitiveArray[] {new ShortArray(new short[] {dint16.getValue()})};
    } else if (baseType instanceof DUInt16 duint16) {
      return new PrimitiveArray[] {new UShortArray(new short[] {duint16.getValue()})};
    } else if (baseType instanceof DByte dbyte) {
      return new PrimitiveArray[] {new ByteArray(new byte[] {dbyte.getValue()})};
    } else if (baseType instanceof DBoolean dboolean) {
      return new PrimitiveArray[] {
        new ByteArray(new byte[] {dboolean.getValue() ? (byte) 1 : (byte) 0})
      };
    } else if (baseType instanceof DString dstring) {
      // String2.log(">>  baseType is DString=" + String2.toJson(((DString)baseType).getValue()));
      return new PrimitiveArray[] {new StringArray(new String[] {dstring.getValue()})};
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      baseType.printVal(baos, " ");
      throw new SimpleException(String2.ERROR + ": Unrecogized baseType:" + baos);
    }
  }

  /**
   * Get the values from 1+ DArrays (axis variables) in PrimitiveArrays.
   *
   * @param dConnect
   * @param names the names of the DArray variables (e.g., axes).
   * @param axis0Constraint e.g., "[12:22]", or ""
   * @return an array of PrimitiveArrays with the requested data
   * @throws Exception if trouble
   */
  public static PrimitiveArray[] getAxisValues(
      DConnect dConnect, String[] names, String axis0Constraint) throws Exception {
    long time = System.currentTimeMillis();
    StringBuilder query = new StringBuilder();
    for (int i = 0; i < names.length; i++) {
      query.append(i == 0 ? '?' : ',');
      query.append(names[i]);
      if (i == 0) query.append(axis0Constraint);
    }
    DataDDS dataDds = dConnect.getData(query.toString(), null);
    if (verbose)
      String2.log(
          "    OpendapHelper.getAxisValues done. TIME="
              + (System.currentTimeMillis() - time)
              + "ms\n      query="
              + query);
    PrimitiveArray pa[] = new PrimitiveArray[names.length];
    for (int i = 0; i < names.length; i++) {
      DArray da = (DArray) dataDds.getVariable(names[i]);
      pa[i] = getPrimitiveArray(da.getPrimitiveVector());
    }
    return pa;
  }

  /**
   * Get a PrimitiveArray result from an opendap query.
   *
   * @param dConnect
   * @param query For an entire variable that is a DArray, use, for example, "?lat", or for a
   *     portion: "?lat[0:1:5]". For a portion of a DGrid, use, for example,
   *     "?ssta.ssta[23:1:23][642:1:742][339:1:439]". This should be already percentEncoded as
   *     needed.
   * @return a PrimitiveArray with the data from the DArray returned by the query
   * @throws Exception if trouble
   */
  public static PrimitiveArray getPrimitiveArray(DConnect dConnect, String query) throws Exception {
    return getPrimitiveArray(getPrimitiveVector(dConnect, query));
  }

  /**
   * Get a PrimitiveArray result from an opendap query.
   *
   * @param dConnect
   * @param query For an entire variable that is a DArray, use, for example, "?lat", or for a
   *     portion: "?lat[0:1:5]". For a portion of a DGrid, use, for example,
   *     "?ssta.ssta[23:1:23][642:1:742][339:1:439]". This should be already percent encoded as
   *     needed.
   * @return a double[] with the data from the DArray returned by the query
   * @throws Exception if trouble
   */
  public static double[] getDoubleArray(DConnect dConnect, String query) throws Exception {
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

    // new method
    Object intSto = pv.getInternalStorage();
    // String2.log("pv internalStorage=" + intSto);
    if (intSto instanceof BaseType[] bta) {
      int n = bta.length;
      if (n > 0 && bta[0] instanceof DString) {
        // String2.log("  pv internalStorage is BaseType[] of DString.");
        String sa[] = new String[n];
        for (int i = 0; i < n; i++) sa[i] = ((DString) bta[i]).getValue();
        intSto = sa;
      }
    }
    PrimitiveArray pa = PrimitiveArray.factory(intSto);
    if (pv.getLength() > pa.size()) {
      String2.log(
          "\n!!!OpendapHelper.getPrimitiveArray pvLength="
              + pv.getLength()
              + " > paSize="
              + pa.size()
              + "\n");
      pa.removeRange(pv.getLength(), pa.size());
    }
    return pa;
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
    if (pa instanceof DoubleArray || pa instanceof LongArray || pa instanceof ULongArray)
      pv = new Float64PrimitiveVector(new DFloat64(name));
    else if (pa instanceof FloatArray) pv = new Float32PrimitiveVector(new DFloat32(name));
    else if (pa instanceof IntArray) pv = new Int32PrimitiveVector(new DInt32(name));
    else if (pa instanceof UIntArray) pv = new UInt32PrimitiveVector(new DUInt32(name));
    else if (pa instanceof ShortArray) pv = new Int16PrimitiveVector(new DInt16(name));
    else if (pa instanceof UShortArray) pv = new UInt16PrimitiveVector(new DUInt16(name));
    else if (pa instanceof ByteArray || pa instanceof UByteArray)
      pv = new BytePrimitiveVector(new DByte(name));
    else
      throw new Exception(
          String2.ERROR
              + "in OpendapHelper.getPrimitiveVector: The PrimitiveArray type="
              + pa.elementTypeString()
              + " is not supported.");

    pv.setInternalStorage(pa.toObjectArray());
    return pv;
  }

  /* This variant fo getAtomicType assumes strictDapMode=true. */
  public static String getAtomicType(PAType paType) throws RuntimeException {
    return getAtomicType(true, paType);
  }

  /**
   * This returns the atomic-type String (e.g., Int16) corresponding to a PrimitiveArray's type.
   *
   * <p>Some Java types don't have exact matches. The closest match is returned, e.g., char becomes
   * String, long becomes double
   *
   * @param strictDapMode if true, this only returns DAP 2.0 types. If false, This returns all
   *     PAType types (written in DAP style).
   * @param paType a PAType
   * @return the corresponding atomic-type String
   * @throws RuntimeException if trouble
   */
  public static String getAtomicType(boolean strictDapMode, PAType paType) throws RuntimeException {
    if (paType == PAType.DOUBLE) return "Float64";
    if (paType == PAType.FLOAT) return "Float32";
    if (paType == PAType.LONG)
      return strictDapMode
          ? "Float64"
          : "Int64"; // DAP has no long.  This is imperfect; there will be loss of precision
    if (paType == PAType.ULONG)
      return strictDapMode
          ? "Float64"
          : "UInt64"; // DAP has no ulong. This is imperfect; there will be loss of precision
    if (paType == PAType.INT) return "Int32";
    if (paType == PAType.UINT) return "UInt32";
    if (paType == PAType.SHORT) return "Int16";
    if (paType == PAType.USHORT) return "UInt16";
    if (paType == PAType.BYTE)
      return "Byte"; // ! technically, DAP bytes are unsigned, but for name consistency and
    // ERDDAP/THREDDS convention, Byte means signed
    if (paType == PAType.UBYTE) return strictDapMode ? "Byte" : "UByte"; // DAP has no ubyte
    if (paType == PAType.CHAR)
      return strictDapMode ? "String" : "Char"; // DAP has no char, so represent it as a String
    if (paType == PAType.STRING) return "String";
    throw new RuntimeException(
        String2.ERROR
            + "in OpendapHelper.getAtomicType: The classType="
            + paType
            + " is not supported.");
  }

  /**
   * This writes the attributes to the writer in the opendap DAS format. This uses
   * strictDapMode=true.
   *
   * <p>E.g.,
   *
   * <pre>
   * altitude {
   * Float32 actual_range 0.0, 0.0;
   * Int32 fraction_digits 0;
   * String long_name "Altitude";
   * }
   * </pre>
   *
   * @param varName the variable's name. For global attributes, ncBrowse and netcdf-java treat
   *     "NC_GLOBAL" as special case. (I had used "GLOBAL".)
   * @param paType The PAType of the variable.
   * @param attributes
   * @param writer
   * @param encodeAsHTML if true, characters like &lt; are converted to their character entities and
   *     lines wrapped with \n if greater than 78 chars.
   * @throws Exception if trouble
   */
  public static void writeToDAS(
      String varName, PAType paType, Attributes attributes, Writer writer, boolean encodeAsHTML)
      throws Exception {

    writer.append(
        dasToStringBuilder(varName, paType, attributes, encodeAsHTML, true)); // strictDapMode
  }

  /**
   * This writes the attributes to the StringBuilder in the opendap DAS format. E.g.,
   *
   * <pre>
   * altitude {
   * Float32 actual_range 0.0, 0.0;
   * Int32 fraction_digits 0;
   * String long_name "Altitude";
   * }
   * </pre>
   *
   * @param varName the variable's name
   * @param paType The PAType of the variable. If isUnsigned and if the attributes doesn't already
   *     include it and if strictDapMode=true, a new _Unsigned=true attribute will be added to the
   *     output. Attributes with unsigned data types will always be converted to signed when
   *     written.
   * @param attributes
   * @param encodeAsHTML if true, characters like &lt; are converted to their character entities.
   * @param strictDapMode if true, this sticks to DAP 2.0 standard. If false, all PATypes are
   *     supported (with names that look like DAP names).
   * @throws Exception if trouble
   */
  public static StringBuilder dasToStringBuilder(
      String varName,
      PAType paType,
      Attributes attributes,
      boolean encodeAsHTML,
      boolean strictDapMode)
      throws Exception {
    StringBuilder sb = new StringBuilder();
    // String2.log(">> dasToString " + varName + " attributes:\n" + attributes.toString());
    // see EOL definition for comments about it
    int firstUEncodedChar = encodeAsHTML ? 65536 : 65536;
    sb.append("  " + XML.encodeAsHTML(varName, encodeAsHTML) + " {" + EOL);
    StringArray names = new StringArray(attributes.getNames());
    boolean addedUnsigned = false;
    if (strictDapMode
        && (paType == PAType.BYTE || paType == PAType.UBYTE)
        && // for bytes, explicitly say _Unsigned=true (which DAP defines) or false (which ERDDAP
        // and TDS used as default)
        attributes.getString("_Unsigned") == null) {
      addedUnsigned = true;
      names.atInsert(0, "_Unsigned"); // 0 is a good guess at the correct position
      names.sortIgnoreCase(); // but re-sort to make sure
    }
    for (int ni = 0; ni < names.size(); ni++) {
      String tName = names.get(ni);
      PrimitiveArray pa =
          tName.equals("_Unsigned") && addedUnsigned
              ? new StringArray(new String[] {paType == PAType.BYTE ? "false" : "true"})
              : attributes.get(tName);
      PAType et = pa.elementType();
      // technically this is wrong because DAP says bytes are unsigned
      // but THREDDS and ERDDAP have traditionally treated bytes as signed.
      if (strictDapMode && et == PAType.UBYTE) // but DAP does support UInt16 and UInt32.
      pa = pa.makeSignedPA();
      sb.append(
          XML.encodeAsHTML(
              "    " + getAtomicType(strictDapMode, et) + " " + tName + " ", encodeAsHTML));
      int paSize = pa.size();
      if (et == PAType.CHAR || et == PAType.STRING) {
        // if (et == PAType.CHAR) String2.log(">> dasToStringBuilder char#=" + pa.getInt(0));
        String ts = String2.toSVString(pa.toStringArray(), "\n", false);
        if (encodeAsHTML) {
          // was ts = String2.noLongLinesAtSpace(ts, 78, "");
          if (ts.indexOf('\n') >= 0)
            sb.append('\n'); // start on new line, so first line isn't super long
        }
        // enquote, and replace internal quotes with \"
        // DAP 2.0 appendix A says \ becomes \\ and " becomes \"
        // 2017-05-05 I considered toJson, but DASParser doesn't like e.g., \\uhhhh
        // 2020-05-12 Json is the logical extension of DAP spec.
        //    So do it but don't use \\u notation (firstUEncodedchar was 127, now 65536)
        ts = String2.toJson(ts, firstUEncodedChar, false);
        // ts = String2.replaceAll(ts, "\\", "\\\\");
        // ts = "\"" + String2.replaceAll(ts, "\"", "\\\"") + "\"";
        // String2.log(">> ts=" + ts);
        sb.append(XML.encodeAsHTML(ts, encodeAsHTML));
      } else if (et == PAType.LONG || et == PAType.ULONG) {
        // print with full precision, even if strictDapMode. No reason not to.
        for (int pai = 0; pai < paSize; pai++) {
          sb.append(
              pa.getRawString(pai)
                  +
                  // String.format("%g.6", (Object)Double.valueOf(pa.getDouble(pai))) +
                  (pai < paSize - 1 ? ", " : ""));
        }
      } else if (et == PAType.DOUBLE) {
        // the spec says must be like Ansi C printf, %g format, precision=6. That is clearly
        // insufficient.
        for (int pai = 0; pai < paSize; pai++) {
          String ts = "" + pa.getDouble(pai);
          // if (et==PAType.LONG) String2.log(">> Opendap long att #" + pai + " = " +
          // pa.getString(pai) + " => " + ts);
          ts = String2.replaceAll(ts, "E-", "e-"); // do first
          ts = String2.replaceAll(ts, "E", "e+");
          sb.append(
              ts
                  +
                  // String.format("%g.6", (Object)Double.valueOf(pa.getDouble(pai))) +
                  (pai < paSize - 1 ? ", " : ""));
        }
      } else if (et == PAType.FLOAT) {
        for (int pai = 0; pai < paSize; pai++) {
          String ts = "" + pa.getFloat(pai);
          ts = String2.replaceAll(ts, "E-", "e-"); // do first
          ts = String2.replaceAll(ts, "E", "e+");
          sb.append(ts + (pai < paSize - 1 ? ", " : ""));
        }
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
    if (pv instanceof Float32PrimitiveVector tpv) {
      for (int i = 0; i < n; i++) da[i] = Math2.niceDouble(tpv.getValue(i), 7);
    } else if (pv instanceof Float64PrimitiveVector tpv) {
      for (int i = 0; i < n; i++) da[i] = tpv.getValue(i);
    } else if (pv instanceof BytePrimitiveVector tpv) {
      for (int i = 0; i < n; i++) da[i] = tpv.getValue(i);
    } else if (pv
        instanceof
        UInt16PrimitiveVector tpv) { // uint16 is instanceof int16! so must test uint16 first
      for (int i = 0; i < n; i++) da[i] = tpv.getValue(i);
    } else if (pv instanceof Int16PrimitiveVector tpv) {
      for (int i = 0; i < n; i++) da[i] = tpv.getValue(i);
    } else if (pv
        instanceof
        UInt32PrimitiveVector tpv) { // uint32 is instanceof int32! so must test uint32 first
      for (int i = 0; i < n; i++) da[i] = tpv.getValue(i);
    } else if (pv instanceof Int32PrimitiveVector tpv) {
      for (int i = 0; i < n; i++) da[i] = tpv.getValue(i);
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
    if (pv instanceof BytePrimitiveVector t) return t.getValue(index);
    if (pv
        instanceof UInt16PrimitiveVector t) // uint16 is instanceof int16! so must test uint16 first
    return t.getValue(index);
    if (pv instanceof Int16PrimitiveVector t) return t.getValue(index);
    if (pv
        instanceof UInt32PrimitiveVector t) // uint32 is instanceof int32! so must test uint32 first
    return t.getValue(index);
    if (pv instanceof Int32PrimitiveVector t) return t.getValue(index);
    if (pv instanceof Float32PrimitiveVector t) return Math2.roundToInt(t.getValue(index));
    if (pv instanceof Float64PrimitiveVector t) return Math2.roundToInt(t.getValue(index));
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
    if (pv instanceof Float32PrimitiveVector t) return t.getValue(index);
    if (pv instanceof Float64PrimitiveVector t) return t.getValue(index);
    if (pv instanceof BytePrimitiveVector t) return t.getValue(index);
    if (pv
        instanceof UInt16PrimitiveVector t) // uint16 is instanceof int16! so must test uint16 first
    return t.getValue(index);
    if (pv instanceof Int16PrimitiveVector t) return t.getValue(index);
    if (pv
        instanceof UInt32PrimitiveVector t) // uint32 is instanceof int32! so must test uint32 first
    return t.getValue(index);
    if (pv instanceof Int32PrimitiveVector t) return t.getValue(index);
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
    if (pv instanceof Float32PrimitiveVector t) return "" + t.getValue(index);
    if (pv instanceof Float64PrimitiveVector t) return "" + t.getValue(index);
    if (pv instanceof BytePrimitiveVector t) return "" + t.getValue(index);
    if (pv
        instanceof UInt16PrimitiveVector t) // uint16 is instanceof int16! so must test uint16 first
    return "" + t.getValue(index);
    if (pv instanceof Int16PrimitiveVector t) return "" + t.getValue(index);
    if (pv
        instanceof UInt32PrimitiveVector t) // uint32 is instanceof int32! so must test uint32 first
    return "" + t.getValue(index);
    if (pv instanceof Int32PrimitiveVector t) return "" + t.getValue(index);
    // if (pv instanceof StringPrimitiveVector t)  return t.getValue(index);
    if (pv instanceof BooleanPrimitiveVector t) return t.getValue(index) ? "true" : "false";
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
    if (bt instanceof DByte t) return t.getValue();
    if (bt instanceof DInt16 t) return t.getValue();
    if (bt instanceof DUInt16 t) return t.getValue();
    if (bt instanceof DInt32 t) return t.getValue();
    if (bt instanceof DUInt32 t) return t.getValue();
    if (bt instanceof DFloat32 t) return Math2.roundToInt(t.getValue());
    if (bt instanceof DFloat64 t) return Math2.roundToInt(t.getValue());
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
    if (bt instanceof DFloat32 t) return t.getValue();
    if (bt instanceof DFloat64 t) return t.getValue();
    if (bt instanceof DByte t) return t.getValue();
    if (bt instanceof DInt16 t) return t.getValue();
    if (bt instanceof DUInt16 t) return t.getValue();
    if (bt instanceof DInt32 t) return t.getValue();
    if (bt instanceof DUInt32 t) return t.getValue();
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
    if (bt instanceof DFloat32 t) return "" + t.getValue();
    if (bt instanceof DFloat64 t) return "" + t.getValue();
    if (bt instanceof DByte t) return "" + t.getValue();
    if (bt instanceof DInt16 t) return "" + t.getValue();
    if (bt instanceof DUInt16 t) return "" + t.getValue();
    if (bt instanceof DInt32 t) return "" + t.getValue();
    if (bt instanceof DUInt32 t) return "" + t.getValue();
    if (bt instanceof DString t) return t.getValue();
    if (bt instanceof DBoolean t) return t.getValue() ? "true" : "false";
    throw new Exception(String2.ERROR + ": Unknown BaseType (" + bt + ").");
  }

  /**
   * This returns the PrimitiveArray elementPAType of a BaseType.
   *
   * @param pv
   * @return the PrimitiveArray elementPAType of this BaseType. This treats all Bytes as signed (it
   *     doesn't look at _Unsigned attribute).
   * @throws Exception if trouble
   */
  public static PAType getElementPAType(BaseType bt) throws Exception {
    return getElementPAType(bt.newPrimitiveVector()); // a new PV of suitable type for the BaseType
  }

  /**
   * This returns the PrimitiveArray elementPAType of a PrimitiveVector.
   *
   * @param pv
   * @return the PrimitiveArray elementPAType of this BaseType. This treats all Bytes as signed (it
   *     doesn't look at _Unsigned attribute).
   * @throws Exception if trouble
   */
  public static PAType getElementPAType(PrimitiveVector pv) throws Exception {
    Test.ensureNotNull(pv, "pv is null");
    // String2.pressEnterToContinue(">> OpendapHelper.getElementPAType(" + pv.toString());
    if (pv instanceof Float32PrimitiveVector) return PAType.FLOAT;
    if (pv instanceof Float64PrimitiveVector) return PAType.DOUBLE;
    if (pv instanceof BytePrimitiveVector)
      return PAType
          .BYTE; // technically should be UByte, but ERDDAP and TDS traditionally treat DAP bytes as
    // signed
    if (pv instanceof UInt16PrimitiveVector)
      return PAType.USHORT; // UInt16 is instanceof Int16! so must test uint16 first
    if (pv instanceof UInt32PrimitiveVector)
      return PAType.UINT; // UInt32 is instanceof Int32! so must test uint32 first
    if (pv instanceof Int16PrimitiveVector) return PAType.SHORT;
    if (pv instanceof Int32PrimitiveVector) return PAType.INT;
    if (pv instanceof BaseTypePrimitiveVector) return PAType.STRING; // ???
    if (pv instanceof BooleanPrimitiveVector) return PAType.BOOLEAN;
    throw new Exception(String2.ERROR + ": Unknown PrimitiveVector (" + pv + ").");
  }

  /**
   * This finds the vars with the most dimensions. If there are more than 1 groups with the same
   * number of dimensions, but different dimensions, the first group will be found.
   *
   * <p>Currently, this won't find variables in a sequence.
   *
   * @param dds
   * @return String varNames[]
   * @throws Exception if trouble
   */
  public static String[] findVarsWithSharedDimensions(DDS dds) throws Exception {

    Enumeration en = dds.getVariables();
    StringArray dimNames = new StringArray();
    StringArray varNames = new StringArray(); // vars with same dimNames
    while (en.hasMoreElements()) {
      BaseType baseType = (BaseType) en.nextElement();
      DArray dArray;
      if (baseType instanceof DGrid) {
        // dGrid has main dArray + dimensions
        DGrid dGrid = (DGrid) baseType;
        dArray = (DArray) dGrid.getVar(0);
      } else if (baseType instanceof DArray) {
        // dArray is usually 1 dim, but may be multidimensional
        dArray = (DArray) baseType;
      } else {
        continue;
      }
      int nDim = dArray.numDimensions();
      // I'm confused. See 'flag' in first test of this method -- no stringLength dim!
      // if (getElementPAType(dArray.getPrimitiveVector()) == PAType.STRING)
      //    nDim--;
      if (nDim == 0 || nDim < dimNames.size()) continue;
      if (nDim > dimNames.size()) {
        // switch to this set of dims
        varNames.clear();
        varNames.add(baseType.getName());
        dimNames.clear();
        for (int d = 0; d < nDim; d++) dimNames.add(dArray.getDimension(d).getName());
      } else {
        // nDim == dimNames.size() and it is >0
        // does this var shar the same dims?
        boolean allSame = true;
        for (int d = 0; d < nDim; d++) {
          if (!dArray.getDimension(d).getName().equals(dimNames.get(d))) {
            allSame = false;
            break;
          }
        }
        if (allSame) varNames.add(baseType.getName());
      }
    }
    return varNames.toArray();
  }

  /**
   * This tests if baseType is an instanceof a scalar (DBoolean, DByte, DFloat32, DFloat64, DInt16,
   * DInt32, DString) and multidimensional (DGrid, DArray) variable.
   *
   * @param baseType
   * @return true or false
   */
  public static boolean instanceofScalarOrMultiDimVar(BaseType baseType) {
    return baseType instanceof DGrid
        || baseType instanceof DArray
        || baseType instanceof DString
        || baseType instanceof DByte
        || baseType instanceof DInt16
        || baseType instanceof DUInt16
        || baseType instanceof DInt32
        || baseType instanceof DUInt32
        || baseType instanceof DFloat32
        || baseType instanceof DFloat64
        || baseType instanceof DBoolean;
  }

  /**
   * This finds all scalar (DBoolean, DByte, DFloat32, DFloat64, DInt16, DUInt16, DInt32, DUInt32,
   * DString) and multidimensional (DGrid, DArray) variables. This won't find DVectors, DLists,
   * DStructures or DSequences.
   *
   * <p>Currently, this won't find variables in a sequence.
   *
   * @param dds
   * @return String varNames[]
   * @throws Exception if trouble
   */
  public static String[] findAllScalarOrMultiDimVars(DDS dds) throws Exception {

    Enumeration en = dds.getVariables();
    StringArray dimNames = new StringArray();
    StringArray varNames = new StringArray(); // vars with same dimNames
    while (en.hasMoreElements()) {
      BaseType baseType = (BaseType) en.nextElement();
      if (instanceofScalarOrMultiDimVar(baseType)) varNames.add(baseType.getName());
    }
    return varNames.toArray();
  }

  /**
   * Get all of the data from allScalarOrMultiDimVars in an Opendap dataset and save in .nc file.
   *
   * @param dapUrl the base DAP URL
   * @param fullFileName the complete name (dir+name+extension) for the .nc file
   */
  public static void allDapToNc(String dapUrl, String fullFileName) throws Throwable {

    String beginError =
        "OpendapHelper.allDapToNc" + "\n  url=" + dapUrl + "\n  file=" + fullFileName;
    if (verbose) String2.log(beginError);
    beginError = String2.ERROR + " in " + beginError + "\n";
    long time = System.currentTimeMillis();

    // delete any existing file
    File2.delete(fullFileName);

    // get dConnect.  If this fails, no clean up needed.
    DConnect dConnect = new DConnect(dapUrl, true, 1, 1);
    DAS das;
    DDS dds;
    try {
      das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    } catch (Throwable t) {
      throw new SimpleException(
          "Error while getting DAS from " + dapUrl + ".das .\n" + t.getMessage(), t);
    }
    try {
      if (debug)
        String2.log(String2.annotatedString(SSR.getUrlResponseStringUnchanged(dapUrl + ".dds")));
      dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
    } catch (Throwable t) {
      throw new SimpleException(
          "Error while getting DDS from " + dapUrl + ".dds .\n" + t.getMessage(), t);
    }

    // find all vars
    String varNames[] = findAllScalarOrMultiDimVars(dds);
    int nVars = varNames.length;
    if (nVars == 0) throw new RuntimeException(beginError + "No variables found!");

    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // *** make ncOut.    If createNew fails, no clean up needed.
    File2.makeDirectory(File2.getDirectory(fullFileName));
    boolean nc3Mode = true;
    NetcdfFormatWriter ncWriter = null;
    try {
      NetcdfFormatWriter.Builder ncOut =
          NetcdfFormatWriter.createNewNetcdf3(fullFileName + randomInt);
      Group.Builder rootGroup = ncOut.getRootGroup();
      ncOut.setFill(false);

      // define the data variables in ncOut
      StringArray dimNames = new StringArray();
      IntArray dimSizes = new IntArray();
      ArrayList<Dimension> dims = new ArrayList(); // ucar.nc2.Dimension
      int varShape[][] = new int[nVars][];
      boolean isString[] = new boolean[nVars]; // all false
      Variable.Builder newVars[] = new Variable.Builder[nVars];
      for (int v = 0; v < nVars; v++) {

        BaseType baseType = dds.getVariable(varNames[v]);
        if (debug)
          String2.log(
              "\ncreate varName=" + varNames[v] + " has typeName=" + baseType.getTypeName());
        Attributes varAtts = new Attributes();
        getAttributes(das, varNames[v], varAtts);
        if (debug) String2.log(varAtts.toString());
        PAType tPAType = null;
        if (baseType instanceof DGrid dGrid) {
          int nDims = dGrid.elementCount(true) - 1;
          ArrayList<Dimension> tDims = new ArrayList();
          varShape[v] = new int[nDims];
          for (int d = 0; d < nDims; d++) {
            BaseType dimBaseType = dGrid.getVar(d + 1);
            String dimName = dimBaseType.getName();
            if (debug)
              String2.log("  dimName=" + dimName + " has typeName=" + dimBaseType.getTypeName());
            DArrayDimension dim = ((DArray) dimBaseType).getFirstDimension();
            int dimSize = dim.getSize();
            int which = dimNames.indexOf(dimName);
            if (which < 0) {
              // create it and store it
              which = dimNames.size();
              dimNames.add(dimName);
              dimSizes.add(dimSize);
              Dimension tDim = NcHelper.addDimension(rootGroup, dimName, dimSize);
              dims.add(tDim);
            }
            tDims.add(dims.get(which));
            varShape[v][d] = dimSize;
          }

          PrimitiveVector pv = ((DArray) dGrid.getVar(0)).getPrimitiveVector(); // has no data
          tPAType = getElementPAType(pv);
          if (debug) String2.log("  DGrid pv=" + pv.toString() + " tPAType=" + tPAType);
          if (tPAType == PAType.STRING) {
            // make String variable
            isString[v] = true;
            int strlen = varAtts.getInt("DODS_strlen");
            if (strlen <= 0 || strlen == Integer.MAX_VALUE) // netcdf-java doesn't like 0
            strlen = 255;
            newVars[v] = NcHelper.addNc3StringVariable(rootGroup, varNames[v], tDims, strlen);
          } else {

            // make numeric variable
            newVars[v] =
                NcHelper.addVariable(
                    rootGroup, varNames[v], NcHelper.getNc3DataType(tPAType), tDims);
          }

        } else if (baseType instanceof DArray dArray) {
          // dArray is usually 1 dim, but may be multidimensional
          // 2021-01-08 I think this is now incorrect with netcdf-java 5.4.1
          // I think there is no need to add extra dim here since NcHelper.addNc3StringVariable
          // handles that.
          int nDims = dArray.numDimensions();
          ArrayList<Dimension> tDims = new ArrayList();
          varShape[v] = new int[nDims];
          for (int d = 0; d < nDims; d++) { // 0..
            DArrayDimension dim = dArray.getDimension(d);
            String dimName = dim.getName();
            int dimSize = dim.getSize();
            int which = dimNames.indexOf(dimName);
            if (which < 0) {
              // create it and store it
              which = dimNames.size();
              dimNames.add(dimName);
              dimSizes.add(dimSize);
              Dimension tDim = NcHelper.addDimension(rootGroup, dimName, dimSize);
              dims.add(tDim);
            }
            tDims.add(dims.get(which));
            varShape[v][d] = dimSize;
          }

          PrimitiveVector pv = dArray.getPrimitiveVector(); // has no data
          tPAType = getElementPAType(pv);
          if (debug) String2.log("  DArray pv=" + pv.toString() + " tPAType=" + tPAType);
          if (tPAType == PAType.STRING) {
            // make String variable
            isString[v] = true;
            int strlen = varAtts.getInt("DODS_strlen");
            if (strlen <= 0 || strlen == Integer.MAX_VALUE) strlen = 255;
            newVars[v] = NcHelper.addNc3StringVariable(rootGroup, varNames[v], tDims, strlen);
          } else {
            // make numeric variable
            newVars[v] =
                NcHelper.addVariable(
                    rootGroup, varNames[v], NcHelper.getNc3DataType(tPAType), tDims);
          }

        } else {
          // it's a scalar variable
          PrimitiveVector pv = baseType.newPrimitiveVector(); // has no data
          tPAType = getElementPAType(pv);
          if (debug) String2.log("  scalar pv=" + pv.toString() + " tPAType=" + tPAType);

          if (tPAType == PAType.STRING) {
            // make String scalar variable
            isString[v] = true;
            if (debug) String2.log("  isString=true");
            int strlen = varAtts.getInt("DODS_strlen");
            if (strlen <= 0 || strlen == Integer.MAX_VALUE) strlen = 255;

            String dimName = "string1";
            int dimSize = 1;
            int which = dimNames.indexOf(dimName);
            if (which < 0) {
              // create it and store it
              which = dimNames.size();
              dimNames.add(dimName);
              dimSizes.add(dimSize);
              Dimension tDim = NcHelper.addDimension(rootGroup, dimName, dimSize);
              dims.add(tDim);
            }
            ArrayList<Dimension> tDims = new ArrayList();
            tDims.add(dims.get(which));
            varShape[v] = new int[1];
            varShape[v][0] = dimSize;
            newVars[v] = NcHelper.addNc3StringVariable(rootGroup, varNames[v], tDims, strlen);

          } else {
            // make numeric scalar variable
            varShape[v] = new int[0];
            newVars[v] =
                NcHelper.addVariable(
                    rootGroup, varNames[v], NcHelper.getNc3DataType(tPAType), new ArrayList());
          }
        }

        // write data variable attributes in ncOut
        NcHelper.setAttributes(nc3Mode, newVars[v], varAtts, tPAType.isUnsigned());
      }

      // write global attributes in ncOut
      Attributes gAtts = new Attributes();
      getAttributes(das, "GLOBAL", gAtts);
      NcHelper.setAttributes(nc3Mode, rootGroup, gAtts);

      // leave "define" mode in ncOut
      ncWriter = ncOut.build();

      // read and write the variables
      for (int v = 0; v < nVars; v++) {
        long vTime = System.currentTimeMillis();
        if (debug) String2.log("write v#" + v + "=" + varNames[v]);

        // DGrid, DArray, scalar: read it, write it
        PrimitiveArray pas[] = getPrimitiveArrays(dConnect, "?" + varNames[v]); // + projection);
        pas[0].trimToSize(); // so underlying array is exact size
        if (debug)
          String2.log(
              "  pas[0].size="
                  + pas[0].size()
                  + (pas[0].size() > 0 ? " #0=" + pas[0].getString(0) : ""));
        Variable newVar =
            ncWriter.findVariable(
                newVars[v].getFullName()); // because newVars are Variable.Builder's
        if (isString[v]) {
          // String variable
          int n = pas[0].size();
          // ArrayString.D1 doesn't work below. Why not?
          ArrayObject.D1 ta = (ArrayObject.D1) Array.factory(DataType.STRING, new int[] {n});
          for (int i = 0; i < n; i++) ta.set(i, pas[0].getString(i));
          ncWriter.writeStringDataToChar(newVar, ta);
        } else {
          // non-String variable
          ncWriter.write(
              newVar,
              Array.factory(
                  NcHelper.getNc3DataType(pas[0].elementType()),
                  varShape[v],
                  pas[0].toObjectArray()));
        }

        if (verbose)
          String2.log(
              "  v#"
                  + v
                  + "="
                  + varNames[v]
                  + " finished. time="
                  + Calendar2.elapsedTimeString(System.currentTimeMillis() - vTime));
      }

      // if close throws Throwable, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      // rename the file to the specified name
      File2.rename(fullFileName + randomInt, fullFileName);

      // diagnostic
      if (verbose)
        String2.log(
            "  OpendapHelper.allDapToNc finished.  TIME="
                + Calendar2.elapsedTimeString(System.currentTimeMillis() - time)
                + "\n");
      // String2.log(NcHelper.ncdump(fullFileName, "-h"));

    } catch (Throwable t) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(t));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(fullFileName + randomInt);
        ncWriter = null;
      }

      throw t;
    }
  }

  /**
   * Get data from a common type of Opendap grid request and save in .nc file.
   *
   * <p>Currently, this won't work with variables in a sequence.
   *
   * @param dapUrl the base DAP URL
   * @param varNames the DGrid or DArray var names (which must share the same dimensions) <br>
   *     If a var isn't present, it is skipped. <br>
   *     If some vars are multidimensional, 1D vars are removed (they'll be downloaded as dimensions
   *     of the multidimensional vars). <br>
   *     If varNames is null or varNames.length==0, the vars which share the most dimensions will be
   *     found and used.
   * @param projection e.g., [17][0][0:179][0:2:359] (or null or "" for all)
   * @param fullFileName the complete name
   * @param jplMode use for requests for jplG1SST data (to enable lat chunking) when the projection
   *     is [0][0:15999][0:35999]
   */
  public static void dapToNc(
      String dapUrl, String varNames[], String projection, String fullFileName, boolean jplMode)
      throws Throwable {

    // constants for jpl
    int jplLonSize = 36000;
    int jplLatSize = 16000;
    int jplLatChunk = 2000;
    int jplNChunks = jplLatSize / jplLatChunk;
    int jplLatDim = 1; // [time][lat][lon]
    FloatArray jplLatPa = null;
    if (jplMode) {
      jplLatPa = new FloatArray(jplLatSize, true);
      for (int i = 0; i < jplLatSize; i++) jplLatPa.setDouble(i, -79.995 + i * 0.01);
    }
    int jplChunkShape[] = {1, jplLatChunk, jplLonSize};

    String beginError =
        "OpendapHelper.dapToNc"
            + "\n  url="
            + dapUrl
            + "\n  varNames="
            + (varNames == null ? "null" : String2.toSVString(varNames, ",", false))
            + "  projection="
            + projection
            + "\n  file="
            + fullFileName;
    if (verbose) String2.log(beginError);
    beginError = String2.ERROR + " in " + beginError + "\n";
    long time = System.currentTimeMillis();

    // get dConnect.  If this fails, no clean up needed.
    DConnect dConnect = new DConnect(dapUrl, true, 1, 1);
    DAS das = dConnect.getDAS(DEFAULT_TIMEOUT);
    DDS dds = dConnect.getDDS(DEFAULT_TIMEOUT);

    if (varNames == null || varNames.length == 0) {
      // find the vars which share the most dimensions
      varNames = findVarsWithSharedDimensions(dds);
      if (varNames.length == 0)
        throw new RuntimeException(beginError + "No variables with dimensions were found!");

    } else {
      // check if varNames exist (if not, set var[v] = null)
      // also, check if there are vars with nDim>1
      boolean dim1Vars[] = new boolean[varNames.length]; // all false
      boolean someMultiDimVars = false;
      for (int v = 0; v < varNames.length; v++) {
        try {
          BaseType baseType = dds.getVariable(varNames[v]);
          DArray dArray;
          if (baseType instanceof DGrid dGrid) {
            // dGrid has main dArray + dimensions
            dArray = (DArray) dGrid.getVar(0);
          } else if (baseType instanceof DArray t) {
            // dArray is usually 1 dim, but may be multidimensional
            dArray = t;
          } else {
            continue;
          }
          if (dArray.numDimensions() <= 1) dim1Vars[v] = true;
          else someMultiDimVars = true;

        } catch (Throwable t) {
          varNames[v] = null;
          if (verbose) String2.log("  removing variable: " + t.toString());
          continue;
        }
      }

      // if someMultiDimVars, remove any dim1Vars (they'll be downloaded as dimensions)
      if (someMultiDimVars)
        for (int v = 0; v < varNames.length; v++) if (dim1Vars[v]) varNames[v] = null;

      // are there validVars remaining?
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

    // if projection is null or "", figure out the projection
    if (projection == null || projection.length() == 0) {
      StringBuilder sb = new StringBuilder();
      for (int v = 0; v < varNames.length; v++) {
        if (varNames[v] == null) continue;
        BaseType baseType = dds.getVariable(varNames[v]);
        DArray dArray;
        if (baseType instanceof DGrid dGrid) {
          // dGrid has main dArray + dimensions
          dArray = (DArray) dGrid.getVar(0);
        } else if (baseType instanceof DArray t) {
          // dArray is usually 1 dim, but may be multidimensional
          dArray = t;
        } else {
          throw new RuntimeException(
              beginError
                  + "var="
                  + varNames[v]
                  + " has unexpected baseType="
                  + baseType.getClass().getName());
        }
        int nDim = dArray.numDimensions();
        if (nDim == 0)
          throw new RuntimeException(
              beginError + "var=" + varNames[v] + " is a DArray with 0 dimensions.");
        for (int d = 0; d < nDim; d++) { // 0..
          sb.append("[0:" + (dArray.getDimension(d).getSize() - 1) + "]");
        }
        break;
      }

      if (sb.length() == 0)
        throw new RuntimeException(
            beginError + "File not created!  None of the requested varNames were found.");
      projection = sb.toString();
      if (verbose) String2.log("  created projection=" + projection);
    }
    int sss[] = parseStartStrideStop(projection); // throws Exception if trouble
    // if (true) throw new RuntimeException("stop here");

    // delete any existing file
    File2.delete(fullFileName);

    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // *Then* make ncOut.    If createNew fails, no clean up needed.
    File2.makeDirectory(File2.getDirectory(fullFileName));
    NetcdfFormatWriter ncWriter = null;
    boolean nc3Mode = true;

    try {
      NetcdfFormatWriter.Builder ncOut =
          NetcdfFormatWriter.createNewNetcdf3(fullFileName + randomInt);
      Group.Builder rootGroup = ncOut.getRootGroup();
      ncOut.setFill(false);

      // define the data variables in ncOut
      int nVars = varNames.length;
      Attributes varAtts[] = new Attributes[nVars];

      boolean firstValidVar = true;
      int nDims = sss.length / 3;
      ArrayList<Dimension> dims = new ArrayList();
      int shape[] = new int[nDims];
      PAType dimPAType[] = new PAType[nDims];
      boolean isDGrid = true; // change if false

      PAType dataPAType[] = new PAType[nVars];
      boolean isStringVar[] = new boolean[nVars]; // all false
      Variable.Builder newVars[] = new Variable.Builder[nVars];
      Variable.Builder newDimVars[] = new Variable.Builder[nDims];
      PAType dimPATypes[] = new PAType[nDims];
      for (int v = 0; v < nVars; v++) {
        // String2.log("  create var=" + varNames[v]);
        if (varNames[v] == null) continue;
        varAtts[v] = new Attributes();
        getAttributes(das, varNames[v], varAtts[v]);

        BaseType baseType = dds.getVariable(varNames[v]);
        if (baseType instanceof DGrid dGrid) {
          // dGrid has main dArray + dimensions
          if (firstValidVar) {
          } else {
            if (!isDGrid)
              throw new RuntimeException(
                  beginError
                      + "var="
                      + varNames[v]
                      + " is a DGrid but the previous vars are DArrays.");
          }
          if (dGrid.elementCount(true) - 1 != nDims)
            throw new RuntimeException(
                beginError
                    + "var="
                    + varNames[v]
                    + " has a different nDimensions than projection=\""
                    + projection
                    + "\".");
          for (int d = 0; d < nDims; d++) {
            String dimName = dGrid.getVar(d + 1).getName();
            if (firstValidVar) {
              // define the dimensions and their variables
              int dimSize = calculateNValues(sss[d * 3], sss[d * 3 + 1], sss[d * 3 + 2]);
              // String2.log("    dim#" + d + "=" + dimName + " size=" + dimSize);
              shape[d] = dimSize;
              Dimension tDim = NcHelper.addDimension(rootGroup, dimName, dimSize);
              dims.add(tDim);
              PrimitiveVector pv =
                  ((DVector) dds.getVariable(dimName)).getPrimitiveVector(); // has no data
              dimPATypes[d] = getElementPAType(pv);
              newDimVars[d] =
                  NcHelper.addVariable(
                      rootGroup,
                      dimName,
                      NcHelper.getNc3DataType(dimPATypes[d]),
                      Arrays.asList(dims.get(d)));
            } else {
              // check that dimension names are the same
              if (!dimName.equals(dims.get(d).getName())) // the full name
              throw new RuntimeException(
                    beginError
                        + "var="
                        + varNames[v]
                        + " has different dimensions than previous vars.");
            }
          }
          firstValidVar = false;

          // make the dataVariable
          PrimitiveVector pv = ((DArray) dGrid.getVar(0)).getPrimitiveVector(); // has no data
          dataPAType[v] = getElementPAType(pv);
          // String2.log("v=" + v + " pv=" + pv.toString() + " dataPAType=" + dataPAType[v]);
          newVars[v] =
              NcHelper.addVariable(
                  rootGroup, varNames[v], NcHelper.getNc3DataType(dataPAType[v]), dims);

        } else if (baseType instanceof DArray dArray) {
          // dArray is usually 1 dim, but may be multidimensional
          if (firstValidVar) {
            isDGrid = false;
          } else {
            if (isDGrid)
              throw new RuntimeException(
                  beginError
                      + "var="
                      + varNames[v]
                      + " is a DArray but the previous vars are DGrids.");
          }
          if (dArray.numDimensions() != nDims)
            throw new RuntimeException(
                beginError
                    + "var="
                    + varNames[v]
                    + " has a different nDimensions than projection=\""
                    + projection
                    + "\".");
          for (int d = 0; d < nDims; d++) { // 0..
            DArrayDimension dim = dArray.getDimension(d);
            String dimName = dim.getName();
            if (firstValidVar) {
              // define the dimensions
              int dimSize = calculateNValues(sss[d * 3], sss[d * 3 + 1], sss[d * 3 + 2]);
              // String2.log("    DArray dim#" + d + "=" + dimName + " size=" + dimSize);
              shape[d] = dimSize;
              Dimension tDim = NcHelper.addDimension(rootGroup, dimName, dimSize);
              dims.add(tDim);
              // don't make a related variable
            } else {
              // check that dimension names are the same
              if (!dimName.equals(dims.get(d).getName())) // the full name
              throw new RuntimeException(
                    beginError
                        + "var="
                        + varNames[v]
                        + " has different dimensions than previous vars.");
            }
          }
          firstValidVar = false;

          // make the dataVariable
          PrimitiveVector pv = dArray.getPrimitiveVector(); // has no data
          dataPAType[v] = getElementPAType(pv);
          // String2.log("  pv dataPAType=" + dataPAType[v]);

          if (dataPAType[v] == PAType.STRING) {
            // a String variable.  Add a dim for nchars
            isStringVar[v] = true;
            ArrayList<Dimension> tDims = new ArrayList(dims);
            int nChars = varAtts[v].getInt("DODS_strlen");
            if (nChars == Integer.MAX_VALUE) {
              if (verbose)
                String2.log(
                    beginError + "String var=" + varNames[v] + " has no DODS_strlen attribute.");
              varNames[v] = null;
              continue;
            }
            Dimension tDim =
                NcHelper.addDimension(rootGroup, varNames[v] + NcHelper.StringLengthSuffix, nChars);
            tDims.add(tDim);

            newVars[v] =
                NcHelper.addVariable(rootGroup, varNames[v], ucar.ma2.DataType.CHAR, tDims);

          } else {
            // a regular variable
            newVars[v] =
                NcHelper.addVariable(
                    rootGroup, varNames[v], NcHelper.getNc3DataType(dataPAType[v]), dims);
          }

        } else {
          throw new RuntimeException(
              beginError
                  + "var="
                  + varNames[v]
                  + " has unexpected baseType="
                  + baseType.getClass().getName());
        }
      }

      // write global attributes in ncOut
      Attributes tAtts = new Attributes();
      getAttributes(das, "GLOBAL", tAtts);
      NcHelper.setAttributes(nc3Mode, rootGroup, tAtts);

      // write dimension attributes in ncOut
      if (isDGrid) {
        for (int d = 0; d < nDims; d++) {
          String dimName = dims.get(d).getName(); // the full name
          tAtts.clear();
          getAttributes(das, dimName, tAtts);
          NcHelper.setAttributes(nc3Mode, newDimVars[d], tAtts, dimPATypes[d].isUnsigned());
        }
      }

      // write data variable attributes in ncOut
      for (int v = 0; v < nVars; v++) {
        if (varNames[v] == null) continue;
        NcHelper.setAttributes(nc3Mode, newVars[v], varAtts[v], dataPAType[v].isUnsigned());
      }

      // leave "define" mode in ncOut
      ncWriter = ncOut.build();

      // read and write the dimensions
      if (isDGrid) {
        for (int d = 0; d < nDims; d++) {
          String tProjection = "[" + sss[d * 3] + ":" + sss[d * 3 + 1] + ":" + sss[d * 3 + 2] + "]";
          PrimitiveArray pas[] =
              getPrimitiveArrays(
                  dConnect, "?" + dims.get(d).getName() + tProjection); // the full name
          pas[0].trimToSize(); // so underlying array is exact size
          ncWriter.write(
              newDimVars[d].getFullName(),
              NcHelper.get1DArray(pas[0].toObjectArray(), pas[0].isUnsigned()));
        }
      }

      // read and write the data variables
      firstValidVar = true;
      for (int v = 0; v < nVars; v++) {
        if (varNames[v] == null) continue;
        long vTime = System.currentTimeMillis();
        Variable newVar =
            ncWriter.findVariable(
                newVars[v].getFullName()); // because newVars are Variable.Builder's

        if (jplMode) {
          // read in chunks
          int origin[] = {0, 0, 0}; // nc uses: start stop! stride
          for (int chunk = 0; chunk < jplNChunks; chunk++) {
            int base = chunk * jplLatChunk;
            origin[1] = base;
            // change that lat part of the projection
            String tProjection =
                String2.replaceAll(
                    projection,
                    "[0:" + (jplLatSize - 1) + "]",
                    "[" + base + ":" + (base + jplLatChunk - 1) + "]");
            PrimitiveArray pas[] = getPrimitiveArrays(dConnect, "?" + varNames[v] + tProjection);
            pas[0].trimToSize(); // so underlying array is exact size
            // String2.log("pas[0]=" + pas[0].toString());
            ncWriter.write(
                newVar,
                origin,
                ucar.ma2.Array.factory(
                    NcHelper.getNc3DataType(pas[0].elementType()),
                    jplChunkShape,
                    pas[0].toObjectArray()));
          }
        } else {
          // DGrid and DArray: read it, write it

          PrimitiveArray pas[] = getPrimitiveArrays(dConnect, "?" + varNames[v] + projection);
          pas[0].trimToSize(); // so underlying array is exact size
          // String2.log("pas[0].size=" + pas[0].size());
          if (isStringVar[v]) {
            // String variable
            int n = pas[0].size();
            // ArrayString.D1 doesn't work below. Why not?
            ArrayObject.D1 ta = (ArrayObject.D1) Array.factory(DataType.STRING, new int[] {n});
            for (int i = 0; i < n; i++) ta.set(i, pas[0].getString(i));
            ncWriter.writeStringDataToChar(newVar, ta);
          } else {
            // non-String variable
            ncWriter.write(
                newVar,
                Array.factory(
                    NcHelper.getNc3DataType(pas[0].elementType()), shape, pas[0].toObjectArray()));
          }
        }

        firstValidVar = false;
        if (verbose)
          String2.log(
              "  v#"
                  + v
                  + "="
                  + varNames[v]
                  + " finished. time="
                  + Calendar2.elapsedTimeString(System.currentTimeMillis() - vTime));
      }

      // if close throws Throwable, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      // rename the file to the specified name
      File2.rename(fullFileName + randomInt, fullFileName);

      // diagnostic
      if (verbose)
        String2.log(
            "  OpendapHelper.dapToNc finished.  TIME="
                + Calendar2.elapsedTimeString(System.currentTimeMillis() - time)
                + "\n");
      // String2.log(NcHelper.ncdump(fullFileName, "-h"));

    } catch (Throwable t) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(t));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(fullFileName + randomInt);
        ncWriter = null;
      }

      throw t;
    }
  }
}
