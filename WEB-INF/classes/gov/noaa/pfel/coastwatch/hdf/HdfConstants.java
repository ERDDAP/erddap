/*
 * HdfConstants Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.hdf;

/**
 * Constants used with HDF version 4 SDS files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-01
 */
public class HdfConstants {

  /** FAIL (from HDFConstants.java) */
  public static final int FAIL = -1;

  /** DFACC constants are from HDFConstants.java. */
  public static final int DFACC_READ = 1;

  public static final int DFACC_WRITE = 2;
  public static final int DFACC_RDWR = 3;
  public static final int DFACC_CREATE = 4;
  public static final int DFACC_RDONLY = DFACC_READ;

  // public static final int DFACC_DEFAULT=000;  //why written this way, not 0?
  // public static final int DFACC_SERIAL=001;   //why written this way, not 0? conflict with READ?
  // public static final int DFACC_PARALLEL=011; //why written this way, not 0? meant to be 0x11?

  /** DNFT constants are from HDFConstants.java. unsigned char. */
  public static final byte DFNT_UCHAR8 = (byte) 3;

  public static final byte DFNT_UCHAR = (byte) 3;

  /** char */
  public static final byte DFNT_CHAR8 = (byte) 4;

  public static final byte DFNT_CHAR = (byte) 4;

  /** No supported by HDF */
  public static final byte DFNT_CHAR16 = (byte) 42;

  public static final byte DFNT_UCHAR16 = (byte) 43;

  /** float */
  public static final byte DFNT_FLOAT32 = (byte) 5;

  public static final byte DFNT_FLOAT = (byte) 5;

  // ** double */
  public static final byte DFNT_FLOAT64 = (byte) 6;
  public static final byte DFNT_FLOAT128 = (byte) 7;
  public static final byte DFNT_DOUBLE = (byte) 6;

  /** 8-bit integer */
  public static final byte DFNT_INT8 = (byte) 20;

  /** unsigned 8-bit interger */
  public static final byte DFNT_UINT8 = (byte) 21;

  /** byte */
  public static final byte DFNT_INT16 = (byte) 22;

  /** unsigned interger */
  public static final byte DFNT_UINT16 = (byte) 23;

  /** interger */
  public static final byte DFNT_INT32 = (byte) 24;

  /** unsigned interger */
  public static final byte DFNT_UINT32 = (byte) 25;

  /** No supported */
  public static final byte DFNT_INT64 = (byte) 26;

  public static final byte DFNT_UINT64 = (byte) 27;
  public static final byte DFNT_INT128 = (byte) 28;
  public static final byte DFNT_UINT128 = (byte) 30;

  // public static final byte DFNT_LITEND = (byte) 0x4000;  //what is this?

  /** Constants for class types for Floating point */
  public static final byte DFNTF_NONE = (byte) 0;

  public static final byte DFNTF_IEEE = (byte) 1;
  public static final byte DFNTF_VAX = (byte) 2;
  public static final byte DFNTF_CRAY = (byte) 3;
  public static final byte DFNTF_PC = (byte) 4;
  public static final byte DFNTF_CONVEX = (byte) 5;

  /** Constants for class types for Integer */
  public static final byte DFNTI_MBO = (byte) 1; // Motorola Byte Order (e.g., Java)

  public static final byte DFNTI_IBO = (byte) 2; // Intel Byte Order
  public static final byte DFNTI_VBO = (byte) 4; // Vax Byte Order

  /** Constants for class types for Character */
  public static final byte DFNTC_ASCII1 = (byte) 1;

  public static final byte DFNTC_EBCDOC = (byte) 2;
  public static final byte DFNTC_BYTE = (byte) 0;

  /**
   * This returns the DFNT type corresponding to a Byte, Short, Integer, Float, Double, String,
   * byte[], short[], int[], float[], double[], String[].
   *
   * @param o
   * @return the corresponding DFNT type
   */
  public static byte getDfntType(Object o) {
    if (o instanceof Byte) return DFNT_INT8;
    if (o instanceof Short) return DFNT_INT16;
    if (o instanceof Integer) return DFNT_INT32;
    if (o instanceof Float) return DFNT_FLOAT32;
    if (o instanceof Double) return DFNT_FLOAT64;
    if (o instanceof String) return DFNT_CHAR8;

    if (o instanceof byte[]) return DFNT_INT8;
    if (o instanceof short[]) return DFNT_INT16;
    if (o instanceof int[]) return DFNT_INT32;
    if (o instanceof float[]) return DFNT_FLOAT32;
    if (o instanceof double[]) return DFNT_FLOAT64;
    if (o instanceof String[]) return DFNT_CHAR8;

    throw new RuntimeException("HdfConstants.getDfntType unsupported object type: " + o.toString());
  }
}
