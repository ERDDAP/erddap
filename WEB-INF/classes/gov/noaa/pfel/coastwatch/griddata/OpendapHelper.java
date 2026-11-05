/*
 * OpendapHelper Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.XML;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import thredds.client.catalog.ServiceType;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
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
  public static final boolean debug = false;

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

  public static final int DEFAULT_TIMEOUT = 120000; // 2 minutes in millis

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
    int firstUEncodedChar = 65536;
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
        sb.append(pa);
      }
      sb.append(";" + EOL);
    }
    sb.append("  }" + EOL);
    return sb;
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

    DatasetUrl durl = DatasetUrl.create(ServiceType.OPENDAP, dapUrl);
    try (NetcdfDataset dataset =
        NetcdfDatasets.openDataset(
            durl, EnumSet.noneOf(NetcdfDataset.Enhance.class), -1, null, null)) {

      // find all variables excluding structures/sequences
      List<Variable> srcVars = new ArrayList<>();
      for (Variable v : dataset.getVariables()) {
        if (v.isMemberOfStructure()
            || v.getDataType() == DataType.STRUCTURE
            || v.getDataType() == DataType.SEQUENCE) {
          continue;
        }
        srcVars.add(v);
      }

      int nVars = srcVars.size();
      if (nVars == 0) throw new RuntimeException(beginError + "No variables found!");

      // If procedure fails half way through, there won't be a half-finished file.
      int randomInt = Math2.random(Integer.MAX_VALUE);

      // *** make ncOut. If createNew fails, no clean up needed.
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
        ArrayList<Dimension> dims = new ArrayList<>(); // ucar.nc2.Dimension
        boolean[] isString = new boolean[nVars]; // all false
        Variable.Builder<?>[] newVars = new Variable.Builder[nVars];

        for (int v = 0; v < nVars; v++) {
          Variable srcVar = srcVars.get(v);
          String varName = srcVar.getShortName();
          if (debug)
            String2.log("\ncreate varName=" + varName + " has dataType=" + srcVar.getDataType());

          Attributes varAtts = new Attributes();
          NcHelper.getVariableAttributes(srcVar, varAtts);
          if (debug) String2.log(varAtts.toString());

          PAType tPAType = NcHelper.getElementPAType(srcVar);
          boolean isStr = srcVar.getDataType() == DataType.STRING || tPAType == PAType.STRING;

          List<Dimension> srcDims = srcVar.getDimensions();
          ArrayList<Dimension> tDims = new ArrayList<>();

          if (isStr && srcDims.isEmpty()) {
            // String scalar variable
            String dimName = "string1";
            int dimSize = 1;
            int which = dimNames.indexOf(dimName);
            if (which < 0) {
              which = dimNames.size();
              dimNames.add(dimName);
              dimSizes.add(dimSize);
              Dimension tDim = NcHelper.addDimension(rootGroup, dimName, dimSize);
              dims.add(tDim);
            }
            tDims.add(dims.get(which));
          } else {
            for (Dimension dim : srcDims) {
              String dimName = dim.getShortName();
              int dimSize = dim.getLength();
              int which = dimNames.indexOf(dimName);
              if (which < 0) {
                which = dimNames.size();
                dimNames.add(dimName);
                dimSizes.add(dimSize);
                Dimension tDim = NcHelper.addDimension(rootGroup, dimName, dimSize);
                dims.add(tDim);
              }
              tDims.add(dims.get(which));
            }
          }

          if (isStr) {
            isString[v] = true;
            int strlen = varAtts.getInt("DODS_strlen");
            if (strlen <= 0 || strlen == Integer.MAX_VALUE) strlen = 255;
            newVars[v] = NcHelper.addNc3StringVariable(rootGroup, varName, tDims, strlen);
          } else {
            newVars[v] =
                NcHelper.addVariable(rootGroup, varName, NcHelper.getNc3DataType(tPAType), tDims);
          }

          // write data variable attributes in ncOut
          NcHelper.setAttributes(nc3Mode, newVars[v], varAtts, tPAType.isUnsigned());
        }

        // write global attributes in ncOut
        Attributes gAtts = new Attributes();
        NcHelper.getGroupAttributes(dataset.getRootGroup(), gAtts);
        NcHelper.setAttributes(nc3Mode, rootGroup, gAtts);

        // leave "define" mode in ncOut
        ncWriter = ncOut.build();

        // read and write the variables
        for (int v = 0; v < nVars; v++) {
          long vTime = System.currentTimeMillis();
          Variable srcVar = srcVars.get(v);
          String varName = srcVar.getShortName();
          if (debug) String2.log("write v#" + v + "=" + varName);

          Array array = srcVar.read();
          Variable newVar = ncWriter.findVariable(newVars[v].getFullName());

          if (isString[v]) {
            if (array.getRank() == 0) {
              array = array.reshape(new int[] {1});
            }
            ncWriter.writeStringDataToChar(newVar, array);
          } else {
            ncWriter.write(newVar, array);
          }

          if (verbose)
            String2.log(
                "  v#"
                    + v
                    + "="
                    + varName
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
}
