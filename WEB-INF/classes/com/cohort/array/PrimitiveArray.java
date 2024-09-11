/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import ucar.ma2.StructureData;

/**
 * PrimitiveArray defines the methods to be implemented by various XxxArray classes for 1D arrays of
 * primitives with methods like ArrayLists's methods.
 *
 * <p>Primitive Arrays for integer types support the idea of a NaN or missing value, by designating
 * their MAX_VALUE as the missing value. This is consistent with JGOFS ("the server will set the
 * missing value field to the largest value possible for specified type.",
 * https://www.opendap.org/server/install-html/install_22.html). This has the convenient side effect
 * that missing values sort high (as to NaNs for floats and doubles).
 *
 * <p>PrimitiveArrays are not synchronized and so are not thread safe (i.e., they are not safe to
 * use with multiple threads).
 */
public abstract class PrimitiveArray {

  /**
   * Set these to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;
  public static boolean debugMode = false;

  /** The number of active values (which may be different from the array's capacity). */
  protected int size = 0;

  protected boolean maxIsMV = false;

  /**
   * The constants identify the items in the array returned by calculateStats. mean and SD are only
   * calculated by calculateStats2().
   */
  public static final int STATS_N = 0,
      STATS_MIN = 1,
      STATS_MAX = 2,
      STATS_SUM = 3,
      STATS_MEAN = 4,
      STATS_SD = 5;

  /* DON'T CHANGE THESE: NCCSV BINARY depends on them (but NCCSV Binary was never created).
  public final static int PATYPE_INDEX_BYTE = 0;
  public final static int PATYPE_INDEX_SHORT = 1;
  public final static int PATYPE_INDEX_CHAR = 2;
  public final static int PATYPE_INDEX_INT = 3;
  public final static int PATYPE_INDEX_LONG = 4;
  public final static int PATYPE_INDEX_FLOAT = 5;
  public final static int PATYPE_INDEX_DOUBLE = 6;
  public final static int PATYPE_INDEX_STRING = 7;

  public final static int PATYPE_INDEX_UBYTE = 8;
  public final static int PATYPE_INDEX_USHORT = 9;
  public final static int PATYPE_INDEX_UINT = 10;
  public final static int PATYPE_INDEX_ULONG = 11;
  */

  /**
   * The regular expression operator. The OPeNDAP spec says =~, so that is the ERDDAP standard. But
   * some implementations use ~=, so see sourceRegexOp. See also Table.REGEX_OP.
   */
  public static final String REGEX_OP = "=~";

  /**
   * These are *not* final so EDStatic can replace them with translated Strings. These are
   * MessageFormat-style strings, so any single quote ' must be escaped as ''. Note that some are
   * errors; some are just messages.
   */
  public static String ArrayAddN = String2.ERROR + " in {0}.addN: n ({1}) < 0.";

  public static String ArrayAppendTables =
      String2.ERROR
          + " in PrimitiveArray.append:\n"
          + "the tables have a different number of columns ({0} != {1}).";
  public static String ArrayAtInsert =
      String2.ERROR + " in {0}.atInsert: index ({1}) is < 0 or > size ({2}).";
  public static String ArrayDiff =
      String2.ERROR + ": The PrimitiveArrays differ at [{0}] ({1} != {2}).";
  public static String ArrayDifferentSize =
      "The other primitiveArray has a different size ({0} != {1}).";
  public static String ArrayDifferentValue =
      "The other primitiveArray has a different value for [{0}] ({1} != {2}).";
  public static String ArrayDiffString =
      "old [{0}]={1},\n  new [{0}]={2}."; // keep 2 spaces before "new"
  public static String ArrayMissingValue = "missing value";
  public static String ArrayNotAscending = "{0} isn''t sorted in ascending order: {1}.";
  public static String ArrayNotDescending = "{0} isn''t sorted in descending order: {1}.";
  public static String ArrayNotEvenlySpaced =
      "{0} isn''t evenly spaced: [{1}]={2}, [{3}]={4}, spacing={5}, average spacing={6}.";
  public static String ArrayRemove = String2.ERROR + " in {0}.remove: index ({1}) >= size ({2}).";
  public static String ArraySubsetStart =
      String2.ERROR + " in {0}.subset: startIndex={1} must be at least 0.";
  public static String ArraySubsetStride =
      String2.ERROR + " in {0}.subset: stride={1} must be greater than 0.";

  /**
   * This returns a PrimitiveArray wrapped around a String[] or array of primitives. For this
   * variant, integerType objects always return signed PrimitiveArrays.
   *
   * @param o a char[][], String[] or primitive[] (e.g., int[])
   * @return a PrimitiveArray which (at least initially) uses the array for data storage.
   */
  public static PrimitiveArray factory(Object o) {
    return factory(o, false);
  }

  /**
   * This returns a PrimitiveArray wrapped around a String[] or array of primitives.
   *
   * @param o a char[][], String[] or primitive[] (e.g., int[])
   * @param isUnsigned if true and if the object type isIntegerType, the resulting PrimitiveArray
   *     will be an unsigned PAType. If false and the objectType isIntegerType, then the result will
   *     be signed.
   * @return a PrimitiveArray which (at least initially) uses the array for data storage.
   */
  public static PrimitiveArray factory(Object o, boolean isUnsigned) {
    if (o == null)
      throw new IllegalArgumentException(String2.ERROR + " in PrimitiveArray.factory: o is null.");

    if (o instanceof char[][] car) {
      int nStrings = car.length;
      StringArray sa = new StringArray(nStrings, false);
      for (int i = 0; i < nStrings; i++) {
        String s = new String(car[i]);
        int po0 = s.indexOf('\u0000');
        if (po0 >= 0) s = s.substring(0, po0);
        sa.add(s);
      }
      return sa;
    }
    if (o instanceof double[] da) return new DoubleArray(da);
    if (o instanceof float[] fa) return new FloatArray(fa);
    if (o instanceof long[] la) return isUnsigned ? new ULongArray(la) : new LongArray(la);
    if (o instanceof int[] ia) return isUnsigned ? new UIntArray(ia) : new IntArray(ia);
    if (o instanceof short[] sa) return isUnsigned ? new UShortArray(sa) : new ShortArray(sa);
    if (o instanceof byte[] ba) return isUnsigned ? new UByteArray(ba) : new ByteArray(ba);
    if (o instanceof char[] ca) return new CharArray(ca);
    if (o instanceof String[] sa) return new StringArray(sa);

    if (o instanceof Object[]) {
      Object oar[] = (Object[]) o;
      int n = oar.length;
      StringArray sa = new StringArray(n, false);
      for (int i = 0; i < n; i++) sa.add(oar[i] == null ? "" : oar[i].toString());
      return sa;
    }

    if (o instanceof Double d) return new DoubleArray(new double[] {d.doubleValue()});
    if (o instanceof Float f) return new FloatArray(new float[] {f.floatValue()});
    if (o instanceof Long l) return new LongArray(new long[] {l.longValue()});
    if (o instanceof Integer i) return new IntArray(new int[] {i.intValue()});
    if (o instanceof Short s) return new ShortArray(new short[] {s.shortValue()});
    if (o instanceof Byte b) return new ByteArray(new byte[] {b.byteValue()});
    if (o instanceof Character c) return new CharArray(new char[] {c.charValue()});

    // String and fall through
    return new StringArray(new String[] {o.toString()});
  }

  /**
   * This returns a new PAOne with the minimum value that can be held by this class, e.g., -128b.
   * This must be a non-static method to work with inheritence.
   *
   * @return a new PAOne with the minimum value that can be held by this class, e.g., -128b.
   */
  public abstract PAOne MINEST_VALUE();

  /**
   * This returns a new PAOne with the maximum value that can be held by this class, e.g., 126b (not
   * including the cohort missing value).
   *
   * @return a new PAOne with the minimum value that can be held by this class, e.g., 126b.
   */
  public abstract PAOne MAXEST_VALUE();

  /**
   * This returns the current capacity (number of elements) of the internal data array.
   *
   * @return the current capacity (number of elements) of the internal data array.
   */
  public abstract int capacity();

  /**
   * This makes a new object which is a copy of this object.
   *
   * @return a new object, with the same elements. It will have a new backing array with a capacity
   *     equal to its size.
   */
  @Override
  public Object clone() {
    return subset(null, 0, 1, size - 1);
  }

  /**
   * This returns a PrimitiveArray of a specified type and capacity.
   *
   * @param elementType e.g., PAType.FLOAT
   * @param capacity The maximum size available before the internal data structure needs to be
   *     enlarged.
   * @param active if true, size will be set to capacity (filled with 0's), else size = 0.
   * @return a PrimitiveArray
   */
  public static PrimitiveArray factory(PAType elementType, int capacity, boolean active) {
    if (elementType == PAType.DOUBLE) return new DoubleArray(capacity, active);
    if (elementType == PAType.FLOAT) return new FloatArray(capacity, active);
    if (elementType == PAType.LONG) return new LongArray(capacity, active);
    if (elementType == PAType.ULONG) return new ULongArray(capacity, active);
    if (elementType == PAType.INT) return new IntArray(capacity, active);
    if (elementType == PAType.UINT) return new UIntArray(capacity, active);
    if (elementType == PAType.SHORT) return new ShortArray(capacity, active);
    if (elementType == PAType.USHORT) return new UShortArray(capacity, active);
    if (elementType == PAType.BYTE) return new ByteArray(capacity, active);
    if (elementType == PAType.UBYTE) return new UByteArray(capacity, active);
    if (elementType == PAType.CHAR) return new CharArray(capacity, active);
    if (elementType == PAType.STRING) return new StringArray(capacity, active);

    throw new IllegalArgumentException(
        String2.ERROR + " in PrimitiveArray.factory: unexpected elementType: " + elementType);
  }

  /**
   * This converts a PrimitiveArray into the specified elementType. This returns the current pa (if
   * already correct type) or a new pa of a specified type.
   *
   * @param elementType desired class e.g., PAType.FLOAT
   * @param pa the source PrimitiveArray
   * @return a PrimitiveArray
   */
  public static PrimitiveArray factory(PAType elementType, PrimitiveArray pa) {
    if (pa.elementType() == elementType) return pa;
    if (elementType == PAType.DOUBLE) return new DoubleArray(pa);
    if (elementType == PAType.FLOAT) return new FloatArray(pa);
    if (elementType == PAType.LONG) return new LongArray(pa);
    if (elementType == PAType.ULONG) return new ULongArray(pa);
    if (elementType == PAType.INT) return new IntArray(pa);
    if (elementType == PAType.UINT) return new UIntArray(pa);
    if (elementType == PAType.SHORT) return new ShortArray(pa);
    if (elementType == PAType.USHORT) return new UShortArray(pa);
    if (elementType == PAType.BYTE) return new ByteArray(pa);
    if (elementType == PAType.UBYTE) return new UByteArray(pa);
    if (elementType == PAType.CHAR) return new CharArray(pa);
    if (elementType == PAType.STRING) return new StringArray(pa);

    throw new IllegalArgumentException(
        String2.ERROR + " in PrimitiveArray.factory: unexpected elementType: " + elementType);
  }

  /**
   * This converts a PrimitiveArray into the specified elementType. This returns the current pa (if
   * already correct type) or a new pa of a specified type. In this "raw" variant, if pa
   * isIntegerType, then the cohort missingValue (e.g., ByteArray missingValue=127) is left intact
   * and NOT converted to new pa's missingValue (e.g., Double.NaN).
   *
   * @param elementType e.g., PAType.FLOAT
   * @param pa the source PrimitiveArray
   * @return a PrimitiveArray
   */
  public static PrimitiveArray rawFactory(PAType elementType, PrimitiveArray pa) {
    if (pa.elementType() == elementType) return pa;
    PrimitiveArray newPa = factory(elementType, pa.size(), false); // not active
    newPa.rawAppend(pa);
    return newPa;
  }

  /**
   * If this is a signed integer type, this makes an unsigned variant (e.g., PAType.BYTE returns a
   * PAType.UBYTE). The values from pa are then treated as unsigned, e.g., -1 in ByteArray becomes
   * 255 in a UByteArray.
   *
   * <p>The signed PA types overwrite this.
   *
   * @return a new unsigned PrimitiveArray, or this pa.
   */
  public PrimitiveArray makeUnsignedPA() {
    return this;
  }

  /**
   * If this is an unsigned integer type, this makes a signed variant (e.g., PAType.UBYTE returns a
   * PAType.BYTE). The values from pa are then treated as unsigned, e.g., 255 in UByteArray becomes
   * -1 in a ByteArray.
   *
   * <p>The unsigned PA types overwrite this.
   *
   * @return a new unsigned PrimitiveArray, or this pa.
   */
  public PrimitiveArray makeSignedPA() {
    return this;
  }

  /**
   * This returns a PrimitiveArray with size constantValues.
   *
   * @param elementType e.g., PAType.FLOAT
   * @param size the desired number of elements
   * @param constantValue the value for all of the elements (e.g., "1.6"). For numeric
   *     elementTypees, constantValue is parsed. For char, the first character is used (e.g., "ab"
   *     -&gt; 'a' -&gt; 97).
   * @return a PrimitiveArray with size constantValues.
   */
  public static PrimitiveArray factory(PAType elementType, int size, String constantValue) {
    if (constantValue == null) constantValue = "";
    PrimitiveArray pa = factory(elementType, 1, false);
    pa.addNStrings(size, constantValue); // this doesn't waste space
    return pa;
  }

  /**
   * This returns a PrimitiveArray of the specified type from the comma-separated values.
   *
   * @param elementType e.g., PAType.FLOAT or PAType.STRING
   * @param csv For elementType=PAType.STRING, individual values with interior commas must be
   *     completely enclosed in double quotes with interior double quotes converted to 2 double
   *     quotes or backslash quote. For String values without interior commas, you don't have to
   *     double quote the whole value.
   * @return a PrimitiveArray
   */
  public static PrimitiveArray csvFactory(PAType elementType, String csv) {

    String[] sa = StringArray.arrayFromCSV(csv);
    // String2.log(">> csvFactory " + elementType + " " + (new
    // StringArray(sa)).toNccsv127AttString());
    if (elementType == PAType.STRING) return new StringArray(sa);
    int n = sa.length;
    PrimitiveArray pa = factory(elementType, n, false);
    for (int i = 0; i < n; i++) pa.addString(sa[i]);
    return pa;
  }

  /**
   * This returns a PrimitiveArray of the specified type from the space or comma-separated values.
   *
   * @param elementType e.g., PAType.FLOAT or PAType.STRING
   * @param ssv For elementType=PAType.CHAR, encode and special characters (e.g., space, double
   *     quotes, backslash, &lt;#32, or &gt;#127) via their JSON or NCCSV encodings (e.g., " ",
   *     "\"", "\\" or """", "\n", "\u20ac").
   * @return a PrimitiveArray
   */
  public static PrimitiveArray ssvFactory(PAType elementType, String ssv) {
    StringArray sa = StringArray.wordsAndQuotedPhrases(ssv);
    int n = sa.size();
    PrimitiveArray pa = factory(elementType, n, false);
    if (elementType == PAType.CHAR) {
      CharArray ca = (CharArray) pa;
      for (int i = 0; i < n; i++) ca.add(String2.fromNccsvChar(sa.get(i)));
    } else {
      for (int i = 0; i < n; i++) pa.addString(sa.get(i));
    }
    return pa;
  }

  /**
   * Return the number of elements in the array.
   *
   * @return the number of elements in the array.
   */
  public int size() {
    return size;
  }

  /**
   * This returns true if maxIsMV is supported (i.e., isIntegerType). This always returns true for
   * CharArray.
   *
   * @return true if maxIsMV is supported
   */
  public boolean supportsMaxIsMV() {
    return isIntegerType();
  }

  /**
   * This sets the maxIsMv setting. This only affects integer types because they have no inherent
   * missing value (e.g. NaN for float and double, \\uffff char, or "" for Strings). When
   * maxIsMV=true, MAX_VALUEs (e.g., 127 for ByteArray) are treated as missing values in statistics
   * and are displayed as "" strings.
   *
   * <p>Initially, every PA has maxIsMV=false. If a caller adds a missing value to the PA, the add
   * method changes maxIsMV to 'true'. So the classes try to handle this automatically. You can also
   * do it explicitly with setMaxIsMV(true).
   *
   * @param tMaxIsMV The new value of maxIsMV (only settable for Integer types and Character)
   * @return this for convenience
   */
  public PrimitiveArray setMaxIsMV(boolean tMaxIsMV) {
    if (supportsMaxIsMV()) maxIsMV = tMaxIsMV;
    return this;
  }

  /**
   * This gets the maxIsMv setting.
   *
   * @return The value of maxIsMV. Non-integer types (other than char, which always returns true)
   *     will always return false;
   */
  public boolean getMaxIsMV() {
    return maxIsMV;
  }

  /**
   * This sets size to 0 and maxIsMV to false. XxxArrays with objects overwrite this to set the
   * no-longer-accessible elements to null.
   */
  public void clear() {
    size = 0;
    maxIsMV = false;
  }

  /**
   * This returns the PAType (e.g., PAType.FLOAT or PAType.STRING) of the element type.
   *
   * @return the PAType (e.g., PAType.FLOAT) of the element type.
   */
  public abstract PAType elementType();

  /**
   * This returns the string form (e.g., "float", "int", "char" or "String") of the element type.
   *
   * @return the string form (e.g., "float", "int", "char" or "String") of the element type.
   */
  public String elementTypeString() {
    return PAType.toCohortString(elementType());
  }

  /**
   * This returns the number of bytes per element for this PrimitiveArray. The value for "String"
   * isn't a constant, so this returns 20.
   *
   * @return the number of bytes per element for this PrimitiveArray. The value for "String" isn't a
   *     constant, so this returns 20.
   */
  public abstract int elementSize();

  /**
   * This returns the cohort missing value for this class (e.g., Integer.MAX_VALUE), expressed as a
   * double. FloatArray and StringArray return Double.NaN.
   *
   * @return the cohort missing value for this class (e.g., Integer.MAX_VALUE), expressed as a
   *     double.
   */
  public abstract double missingValueAsDouble();

  /**
   * This tests if the value at the specified index equals the data type's MAX_VALUE (for
   * integerTypes, which may or may not indicate a missing value, depending on maxIsMV), NaN (for
   * Float and Double), \\uffff (for CharArray), or "" (for StringArray).
   *
   * @param index The index in question
   * @return true if the value is the data type's MAX_VALUE.
   */
  public abstract boolean isMaxValue(int index);

  /**
   * This tests if the value at the specified index is a missing value. For integerTypes,
   * isMissingValue can only be true if maxIsMv is 'true'.
   *
   * @param index The index in question
   * @return true if the value is a missing value.
   */
  public abstract boolean isMissingValue(int index);

  /**
   * This returns the recommended sql data type for this PrimitiveArray. See
   *
   * <ul>
   *   <li>For Strings this is difficult, because it is hard to know what max length might be in the
   *       future. See stringLengthFactor below. (PostgresQL supports varchar without a length, or
   *       TEXT, but it isn't a SQL standard.)
   *   <li>This doesn't deal with sql DATE or TIMESTAMP, since I often store seconds since epoch in
   *       a DoubleArray.
   *   <li>Not all SQL types are universally supported (e.g., BIGINT for LongArray). See
   *       http://www.techonthenet.com/sql/datatypes.php .
   *   <li>For safety, ByteArray returns SMALLINT (not TINYINT, which isn't universally supported,
   *       e.g., postgresql).
   * </ul>
   *
   * @param stringLengthFactor for StringArrays, this is the factor (typically 1.5) to be multiplied
   *     by the current max string length (then rounded up to a multiple of 10, but only if
   *     stringLengthFactor &gt; 1) to estimate the varchar length.
   * @return the recommended sql type as a string e.g., varchar(40)
   */
  public String getSqlTypeString(double stringLengthFactor) {
    PAType type = elementType();
    if (type == PAType.DOUBLE) return "double precision";
    if (type == PAType.FLOAT) return "real"; // postgres treats "float" as double precision
    if (type == PAType.LONG || type == PAType.ULONG)
      return "bigint"; // not universally supported (pgsql does support it)
    if (type == PAType.INT) return "integer";
    if (type == PAType.UINT) return "integer"; // ???
    if (type == PAType.SHORT) return "smallint";
    if (type == PAType.USHORT) return "smallint"; // ???
    if (type == PAType.BYTE)
      return "smallint"; // not TINYINT, not universally supported (even pgsql)
    if (type == PAType.UBYTE)
      return "smallint"; // not TINYINT, not universally supported (even pgsql)
    if (type == PAType.CHAR) return "char(1)";
    if (type == PAType.STRING) {
      StringArray sa = (StringArray) this;
      int max = Math.max(1, sa.maxStringLength());
      if (stringLengthFactor > 1) {
        max = Math2.roundToInt(max * stringLengthFactor);
        max = Math2.hiDiv(max, 10) * 10;
      }
      // postgresql doesn't use longvarchar and allows varchar's max to be very large
      return "varchar(" + max + ")";
    }
    throw new IllegalArgumentException(
        String2.ERROR + " in PrimitiveArray.getSqlTypeString: unexpected type: " + type.toString());
  }

  /**
   * This returns the suggested elementType for the given java.sql.Types. This conversion is not
   * standardized across databases (see
   * http://www.onlamp.com/pub/a/onlamp/2001/09/13/aboutSQL.html?page=last). But choices below are
   * fairly safe. I can't find a table to link java.sql.Types constants to Postgres types. See
   * postgresql types at https://www.postgresql.org/docs/8.2/static/datatype-numeric.html
   *
   * @param sqlType a java.sql.Types constant
   * @return a PrimitiveArray of the suggested type. Basically, numeric types return numeric
   *     PrimitiveArrays; other other types return StringArray. Bit and Boolean return ByteArray.
   *     Date, Time and Timestamp return a StringArray. If the type is unexpected, this returns
   *     StringArray.
   */
  public static PrimitiveArray sqlFactory(int sqlType) {

    // see recommended types in table at
    //  https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/ResultSet.html
    // see JDBC API Tutorial book, pg 1087
    if (sqlType == Types.BIT
        || // PrimitiveArray doesn't have a separate BooleanArray
        sqlType == Types.BOOLEAN
        || // PrimitiveArray doesn't have a separate BooleanArray
        sqlType == Types.TINYINT) return new ByteArray();
    if (sqlType == Types.SMALLINT) return new ShortArray();
    if (sqlType == Types.INTEGER) return new IntArray();
    if (sqlType == Types.BIGINT) return new LongArray();
    if (sqlType == Types.REAL) return new FloatArray();
    if (sqlType == Types.FLOAT
        || // a 64 bit value(!)
        sqlType == Types.DOUBLE
        || sqlType == Types.DECIMAL
        || // DECIMAL == NUMERIC; infinite precision!!!
        sqlType == Types.NUMERIC) return new DoubleArray();
    if (sqlType == Types.DATE
        || // getTimestamp().getTime()/1000.0 -> epochSeconds
        sqlType == Types.TIMESTAMP) return new DoubleArray();
    // if (sqlType == Types.CHAR ||
    //    sqlType == Types.LONGVARCHAR ||
    //    sqlType == Types.VARCHAR ||
    //    sqlType == Types.CLOB ||
    //    sqlType == Types.DATALINK ||
    //    sqlType == Types.REF ||
    //    sqlType == Types.TIME ||     //or convert to time in 0001-01-01?
    //       true //What the heck!!!!! just get everything else as String, too.
    return new StringArray();
  }

  /**
   * This indicates if this class' type (e.g., PAType.SHORT) is an unsigned integer type. The
   * unsigned integer type classes overwrite this.
   *
   * @return true if this class' type (e.g., PAType.SHORT) is an unsigned integer type.
   */
  public boolean isUnsigned() {
    return false;
  }

  /**
   * This indicates if this class' type (e.g., PAType.SHORT) is an integer (in the math sense) type.
   * The integer type classes overwrite this.
   *
   * @return true if this class' type (e.g., PAType.SHORT) is an integer type.
   */
  public boolean isIntegerType() {
    return false;
  }

  /**
   * This indicates if this class' type is PAType.FLOAT or PAType.DOUBLE.
   *
   * @return true if this class' type is PAType.FLOAT or PAType.DOUBLE.
   */
  public boolean isFloatingPointType() {
    return false;
  }

  /**
   * This returns the cohort missing value for this class (e.g., Integer.MAX_VALUE), as a new PAOne.
   * For integer types, maxIsMV is false.
   *
   * @return the cohort missing value for this class (e.g., Integer.MAX_VALUE), as a new PAOne.
   */
  public PAOne missingValue() {
    PAOne paOne = new PAOne(elementType());
    paOne.setString("");
    paOne.pa().setMaxIsMV(false);
    return paOne;
  }

  /**
   * This returns the minimum PAType needed to completely and precisely contain the values in this
   * PA's PAType and tPAType (e.g., when merging two PrimitiveArrays).
   *
   * @param tPAType the PAType of the other PA that will be merged
   * @return the minimum PAType needed to completely and precisely contain the values in this PA's
   *     PAType and tPAType (e.g., when merging two PrimitiveArrays).
   */
  public abstract PAType needPAType(PAType tPAType);

  /**
   * This inserts an item into the array at the specified index, pushing subsequent items to
   * oldIndex+1 and increasing 'size' by 1.
   *
   * @param index 0..
   * @param value the value, as a String.
   */
  public abstract void atInsertString(int index, String value);

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array. This uses an appropriate simple method to do
   *     the conversion. If you need a specific method, create a DoubleArray or StringArray first,
   *     then convert that to the desired PrimitiveArray type.
   */
  public abstract void addObject(Object value);

  /**
   * This reads one value from the StrutureData and adds it to this PA.
   *
   * @param sd from an .nc file
   * @param memberName
   */
  public abstract void add(StructureData sd, String memberName);

  /**
   * This adds PAOne's value to the array.
   *
   * @param value the value, as a PAOne (or null).
   */
  public void addPAOne(PAOne value) {
    addNPAOnes(1, value);
  }

  /**
   * This adds n PAOne's to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a PAOne (or null).
   */
  public abstract void addNPAOnes(int n, PAOne value);

  /**
   * This adds the String to the array.
   *
   * @param value the value, as a String.
   */
  public void addString(String value) {
    addNStrings(1, value);
  }

  /**
   * This adds n Strings to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a String.
   */
  public abstract void addNStrings(int n, String value);

  /**
   * This adds an element to the array.
   *
   * @param value the value, as a Float.
   */
  public void addFloat(float value) {
    addNFloats(1, value);
  }

  /**
   * This adds n floats to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a float.
   */
  public abstract void addNFloats(int n, float value);

  /**
   * This adds an element to the array.
   *
   * @param value the value, as a Double.
   */
  public void addDouble(double value) {
    addNDoubles(1, value);
  }

  /**
   * This adds n doubles to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a double.
   */
  public abstract void addNDoubles(int n, double value);

  /**
   * This adds an element to the array.
   *
   * @param value the value, as an int.
   */
  public void addInt(int value) {
    addNInts(1, value);
  }

  /**
   * This adds n ints to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  public abstract void addNInts(int n, int value);

  /**
   * This adds an element to the array.
   *
   * @param value the value, as a long.
   */
  public void addLong(long value) {
    addNLongs(1, value);
  }

  /**
   * This adds n longs to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  public abstract void addNLongs(int n, long value);

  /**
   * This adds an element from another PrimitiveArray.
   *
   * @param otherPA the source PA
   * @param otherIndex the start index in otherPA
   * @param nValues the number of values to be added
   * @return 'this' for convenience
   */
  public abstract PrimitiveArray addFromPA(PrimitiveArray otherPA, int otherIndex, int nValues);

  /**
   * This is like the other addFromPA, with nValues=1.
   *
   * @param otherPA the source PA
   * @param otherIndex the start index in otherPA
   * @return 'this' for convenience
   */
  public PrimitiveArray addFromPA(PrimitiveArray otherPA, int otherIndex) {
    return addFromPA(otherPA, otherIndex, 1);
  }

  /**
   * This sets an element from another PrimitiveArray.
   *
   * @param index the index to be set
   * @param otherPA the other PrimitiveArray
   * @param otherIndex the index of the item in otherPA
   */
  public abstract void setFromPA(int index, PrimitiveArray otherPA, int otherIndex);

  /**
   * This removes the specified element.
   *
   * @param index the element to be removed, 0 ... size-1
   */
  public abstract void remove(int index);

  /**
   * This removes the specified range of elements.
   *
   * @param from the first element to be removed, 0 ... size
   * @param to one after the last element to be removed, from ... size
   */
  public abstract void removeRange(int from, int to);

  /**
   * Moves elements 'first' through 'last' (inclusive) to 'destination', shifting intermediate
   * values to fill the gap.
   *
   * @param first the first to be move
   * @param last (exclusive)
   * @param destination the destination, can't be in the range 'first+1..last-1'.
   */
  public abstract void move(int first, int last, int destination);

  /**
   * This just keeps the rows for the 'true' values in the bitset. Rows that aren't kept are
   * removed. The resulting PrimitiveArray is compacted (i.e., it has a smaller size()). This
   * doesn't alter the values in bitset.
   *
   * @param bitset The BitSet indicating which rows (indices) should be kept.
   */
  public abstract void justKeep(BitSet bitset);

  /**
   * This ensures that the capacity is at least 'minCapacity'.
   *
   * @param minCapacity the minimum acceptable capacity. minCapacity is type long, but &gt;=
   *     Integer.MAX_VALUE will throw exception.
   */
  public abstract void ensureCapacity(long minCapacity);

  /**
   * This returns a primitive[] (perhaps 'array') which has 'size' elements.
   *
   * @return a primitive[] (perhaps 'array') which has 'size' elements. Unsigned integer types will
   *     return an array with their storage type e.g., ULongArray returns a long[].
   */
  public abstract Object toObjectArray();

  /**
   * This returns a double[] which has 'size' elements.
   *
   * @return a double[] which has 'size' elements.
   */
  public abstract double[] toDoubleArray();

  /**
   * This returns a String[] which has 'size' elements.
   *
   * @return a String[] which has 'size' elements.
   */
  public abstract String[] toStringArray();

  /**
   * Return a value from the array as an int. Floating point values are rounded.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int. String values are parsed with String2.parseInt and so may return
   *     Integer.MAX_VALUE.
   */
  public abstract int getInt(int index);

  /**
   * Return a value from the array as an int. This "raw" variant leaves missingValue from smaller
   * data types (e.g., ByteArray missingValue=127) AS IS (even if maxIsMV=true). Floating point
   * values are rounded.
   *
   * <p>ByteArray, CharArray, ShortArray overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int. String values are parsed with String2.parseInt and so may return
   *     Integer.MAX_VALUE.
   */
  public int getRawInt(int index) {
    return getInt(index);
  }

  /**
   * Set a value in the array as an int.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToByte(i).
   */
  public abstract void setInt(int index, int i);

  /**
   * Return a value from the array as a long. Floating point values are rounded.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a long. String values are parsed with String2.parseLong and so may return
   *     Long.MAX_VALUE.
   */
  public abstract long getLong(int index);

  /**
   * Set a value in the array as a long.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToByte(i).
   */
  public abstract void setLong(int index, long i);

  /**
   * Return a value from the array as a ulong. Floating point values are rounded.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a BigInteger (which may be null).
   */
  public abstract BigInteger getULong(int index);

  /**
   * Set a value in the array as a ulong.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToByte(i).
   */
  public abstract void setULong(int index, BigInteger i);

  /**
   * Return a value from the array as a float.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a float. String values are parsed with String2.parseFloat and so may
   *     return Float.NaN.
   */
  public abstract float getFloat(int index);

  /**
   * Set a value in the array as a float.
   *
   * @param index the index number 0 ... size-1
   * @param d the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.roundToInt(d).
   */
  public abstract void setFloat(int index, float d);

  /**
   * Return a value from the array as a double. FloatArray converts float to double in a simplistic
   * way.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  public abstract double getDouble(int index);

  /**
   * Return a value from the array as a double. FloatArray converts float to double in a simplistic
   * way. For this variant: signed Integer source values will be treated as unsigned.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  public double getUnsignedDouble(int index) { // trouble: use PAOne instead
    return getDouble(index);
  }

  /**
   * Return a value from the array as a double. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS.
   *
   * <p>All integerTypes overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  public double getRawDouble(int index) {
    return getDouble(index);
  }

  /**
   * Return a value from the array as a double. FloatArray converts float to double via
   * Math2.floatToDouble.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  public double getNiceDouble(int index) {
    return getDouble(index);
  }

  /**
   * Return a value from the array as a double. FloatArray converts float to double via
   * Math2.floatToDouble. This "raw" variant leaves missingValue from integer data types (e.g.,
   * ByteArray missingValue=127) AS IS.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  public double getRawNiceDouble(int index) {
    return getRawDouble(index);
  }

  /**
   * Set a value in the array as a double.
   *
   * @param index the index number 0 ... size-1
   * @param d the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.roundToInt(d).
   */
  public abstract void setDouble(int index, double d);

  /**
   * Return a value from the array as a PAOne.
   *
   * @param index the index number 0 ... size-1
   * @return a new PAOne.
   */
  public PAOne getPAOne(int index) {
    return new PAOne(this, index);
  }

  /**
   * Return a value from the array as a PAOne.
   *
   * @param index the index number 0 ... size-1
   * @return the desired value in the supplied PAOne.
   */
  public PAOne getPAOne(int index, PAOne paOne) {
    return paOne.readFrom(this, index);
  }

  /**
   * Set a value in the array from a value in a PAOne.
   *
   * @param index the index number 0 ... size-1
   * @param paOne the value.
   */
  public void setPAOne(int index, PAOne paOne) {
    setFromPA(index, paOne.pa(), 0);
  }

  /**
   * Return a value from the array as a String (where the cohort missing value appears as "", not a
   * value).
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity. If this PA
   *     is unsigned, this method returns the unsigned value.
   */
  public abstract String getString(int index);

  /**
   * Return a value from the array as a String suitable for a JSON file. char returns a String with
   * 1 character. String returns a json String with chars above 127 encoded as \\udddd.
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or null for NaN or infinity.
   *     Represent NaN as null? yes, that is what json library does If I go to https://jsonlint.com/
   *     and enter [1, 2.0, 1e30], it says it is valid. If I enter [1, 2.0, NaN, 1e30], it says NaN
   *     is not valid.
   */
  public abstract String getJsonString(int index);

  /**
   * Return a value from the array as a String suitable for the data section of an NCCSV file. This
   * is close to a json string. StringArray and CharArray overwrite this. Note that LongArray
   * doesn't append L -- that is done separately by file writers.
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity. CharArray
   *     and StringArray overwrite this.
   */
  public String getNccsvDataString(int index) {
    return getString(index);
  }

  /**
   * This is like getNccsvDataString, but encodes chars &gt;=127. CharArray and StringArray
   * overwrite this.
   */
  public String getNccsv127DataString(int index) {
    return getString(index);
  }

  /**
   * Return a value from the array as a String suitable for the data section of an ASCII csv or tsv
   * string. This is close to a json string.
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity. CharArray
   *     and StringArray overwrite this.
   */
  public String getSVString(int index) {
    return getString(index);
  }

  /**
   * Return a value from the array as a String suitable for the data section of a UTF-8 tsv file.
   * This is close to a json string.
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity. CharArray
   *     and StringArray overwrite this.
   */
  public String getUtf8TsvString(int index) {
    return getString(index);
  }

  /**
   * Return a value from the array as a String. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS, regardless of maxIsMV. FloatArray and
   * DoubleArray return "" if the stored value is NaN.
   *
   * <p>All integer types overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a String.
   */
  public String getRawString(int index) {
    return getString(index);
  }

  /**
   * Return a value from the array as a String. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS, regardless of maxIsMV. FloatArray and
   * DoubleArray return "NaN" if the stored value is NaN. That's different than getRawString!!!
   *
   * <p>Float and DoubleArray overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a String.
   */
  public String getRawestString(int index) {
    return getRawString(index);
  }

  /**
   * Set a value in the array as a String.
   *
   * @param index the index number 0 ... size-1
   * @param s the value. For numeric PrimitiveArray's, it is parsed with String2.parse and narrowed
   *     if needed by methods like Math2.roundToInt(d).
   */
  public abstract void setString(int index, String s);

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public abstract int indexOf(String lookFor, int startIndex);

  /**
   * This finds the first value which equals 'lookFor' starting at index 0.
   *
   * @param lookFor the value to be looked for
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(String lookFor) {
    return indexOf(lookFor, 0);
  }

  /**
   * This finds the last value which equals'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public abstract int lastIndexOf(String lookFor, int startIndex);

  /**
   * This finds the last value which equals 'lookFor' starting at index size() - 1.
   *
   * @param lookFor the value to be looked for
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int lastIndexOf(String lookFor) {
    return lastIndexOf(lookFor, size() - 1);
  }

  /** If size != capacity, this makes a new 'array' of size 'size' so capacity will equal size. */
  public abstract void trimToSize();

  /**
   * This converts the elements into a Comma-Separated-Value (CSV) String. Chars acting like
   * unsigned shorts. StringArray overwrites this to specially encode the strings.
   *
   * @return a Comma-Separated-Value (CSV) String representation
   */
  public String toCSVString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(",");
      sb.append(getNccsv127DataString(i));
    }
    return sb.toString();
  }

  /**
   * This converts the elements into a Comma-Space-Separated-Value (CSSV) String. CharArray and
   * StringArray overwrite this to specially encode the strings. Integer types show MAX_VALUE
   * numbers (not "").
   *
   * @return a Comma-Space-Separated-Value (CSSV) String representation
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(", ");
      sb.append(getNccsv127DataString(i));
    }
    return sb.toString();
  }

  /**
   * This converts the elements into an NCCSV attribute String, e.g.,: -128b, 127b Integer types
   * show MAX_VALUE numbers (not ""). Chars above 255 are left as unicode chars (not json encoded).
   * See tests in PrimitiveArray.testNccsv().
   *
   * @return an NCCSV attribute String
   */
  public abstract String toNccsvAttString();

  /**
   * This is like the original version of toNccsvAttString, where chars &gt;127 are json127 encoded
   * in StringArray and CharArray values. StringArray and CharArray overwrite this. See tests in
   * PrimitiveArray.testNccsv().
   *
   * @return an NCCSV attribute String
   */
  public String toNccsv127AttString() {
    return toNccsvAttString();
  }

  /**
   * This returns a JSON-style comma-separated-value list of the elements. Numeric values where
   * getString(i) returns "" (e.g., NaN) are written as "null" in Json. CharArray and StringArray
   * overwrite this.
   *
   * @return a csv string of the elements.
   */
  public String toJsonCsvString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(", ");
      String s = getRawString(i);
      sb.append(s.length() == 0 ? "null" : s); // number missing values -> null
    }
    return sb.toString();
  }

  /**
   * This sorts the elements in ascending order. To get the elements in reverse order, just read
   * from the end of the list to the beginning. For numeric PrimitiveArrays, this is a numeric sort.
   * For String PrimitiveArrays, this is a lexical (plain, simplistic) sort.
   */
  public abstract void sort();

  /**
   * This is like sort(), but StringArray calls sortIgnoreCase(). This is more sophisticated than
   * Java's String.CASE_INSENSITIVE_ORDER. E.g., all charAt(0) A's will sort by for all charAt(0)
   * a's (e.g., AA, Aa, aA, aa).
   */
  public void sortIgnoreCase() {
    sort();
  }

  /** A variant of calculateStats that doesn't use Attributes. */
  public double[] calculateStats() {
    return calculateStats(null);
  }

  /**
   * This is a variant of calculateStats that returns the results as a double[].
   *
   * @return a double[] with dar[STATS_N] containing the number of valid values. dar[STATS_MIN]
   *     containing the minimum value, and dar[STATS_MAX] containing the maximum value.
   *     dar[STATS_SUM] containing the sum of the values. If n is 0, min and max will be Double.NaN,
   *     and sum will be 0.
   */
  public double[] calculateStats(Attributes atts) {
    return PAOne.toDoubleArray(calculatePAOneStats(atts));
  }

  /** A variant of calculatePAOneStats that doesn't use Attributes. */
  public PAOne[] calculatePAOneStats() {
    return calculatePAOneStats(null);
  }

  /**
   * This calculates min, max, and nValid for the values in this PrimitiveArray. Each data type's
   * missing value (e.g., Byte.MAX_VALUE) will be converted to NaN. CharArray's calculate min and
   * max as chars (eg 'a' sorts before 'b'). StringArray's calculate min and max via
   * String2.parseDouble(s).
   *
   * @param atts The related attributes. If they have _FillValue and/or missing_value, those will be
   *     temporarily applied so the stats don't include them. !!! THESE SHOULD BE NOT-PACKED
   *     _FillValue and/or missing_value ATTRIBUTES. This can be null.
   * @return a PAOne[] with dar[STATS_N] containing the number of valid values. dar[STATS_MIN]
   *     containing the minimum value, and dar[STATS_MAX] containing the maximum value.
   *     dar[STATS_SUM] containing the sum of the values. If n is 0, min and max will be NaN (or
   *     e.g., Long.MAX_VALUE), and sum will be 0.
   */
  public PAOne[] calculatePAOneStats(Attributes atts) {
    long time = System.currentTimeMillis();

    int n = 0;

    // strings?
    /*if (elementType() == PAType.STRING) {
        String mv   = "";
        String fv   = "";
        String tMin = "\u0000";
        String tMax = "\uFFFF";
        int tSum = 0;
        if (atts != null) {
            String s = atts.getString("_FillValue");
            if (s != null)
                fv = s;
            s = atts.getString("missing_value");
            if (s != null)
                mv = s;
        }

        for (int i = 0; i < size; i++) {
            String s = getString(i);
            if ("".equals(s) || fv.equals(s) || mv.equals(s)) {
            } else {
                n++;
                if (s.compareTo(tMin) <  0) tMin = s;
                if (s.compareTo(tMax) >= 0) tMax = s;
            }
        }
        //if (debugMode) String2.log(">> PrimitiveArray.calculateStats long n=" + n + " min=" + tMin " max=" + tMax);
        return new PAOne[]{
            PAOne.fromInt(n),
            PAOne.fromString(n == 0? "" : tMin),
            PAOne.fromString(n == 0? "" : tMax),
            PAOne.fromInt(tSum)};
    } */

    // chars?
    if (elementType() == PAType.CHAR) {
      char mv = Character.MAX_VALUE;
      char fv = Character.MAX_VALUE;
      char tMin = Character.MAX_VALUE;
      char tMax = Character.MIN_VALUE;
      int tSum = 0;
      if (atts != null) {
        String s = atts.getString("_FillValue");
        if (s != null && s.length() > 0) fv = s.charAt(0);
        s = atts.getString("missing_value");
        if (s != null && s.length() > 0) mv = s.charAt(0);
      }

      CharArray car = (CharArray) this;
      for (int i = 0; i < size; i++) {
        char c = car.get(i);
        // String2.log(">> calculateStats i=" + i + " n=" + n + " char=" +
        // String2.annotatedString("" + c));
        if (c == Character.MAX_VALUE || c == fv || c == mv) {
        } else {
          n++;
          tMin = (char) Math.min(tMin, c);
          tMax = (char) Math.max(tMax, c);
          tSum += c;
        }
      }
      // if (debugMode) String2.log(">> PrimitiveArray.calculateStats long n=" + n + " min=" + tMin
      // " max=" + tMax);
      return new PAOne[] {
        PAOne.fromInt(n),
        PAOne.fromChar(n == 0 ? Character.MAX_VALUE : tMin),
        PAOne.fromChar(n == 0 ? Character.MAX_VALUE : tMax),
        PAOne.fromInt(tSum)
      };
    }

    // ULONGs? calculate as BigIntegers
    if (elementType() == PAType.ULONG) {
      BigInteger mv = ULongArray.MAX_VALUE;
      BigInteger fv = ULongArray.MAX_VALUE;
      BigInteger tMin = ULongArray.MAX_VALUE;
      BigInteger tMax = ULongArray.MIN_VALUE;
      BigInteger tSum = BigInteger.ZERO;
      if (atts != null) {
        fv = atts.getULong("_FillValue");
        mv = atts.getULong("missing_value");
      }

      for (int i = 0; i < size; i++) {
        BigInteger d = getULong(i);
        if (d == null || d.equals(fv) || d.equals(fv)) {
        } else {
          n++;
          tMin = tMin.min(d);
          tMax = tMax.max(d);
          tSum = tSum.add(d);
        }
      }
      // if (debugMode) String2.log(">> PrimitiveArray.calculateStats ULong n=" + n + " min=" + tMin
      // + " max=" + tMax);
      return new PAOne[] {
        PAOne.fromInt(n),
        PAOne.fromULong(n == 0 ? null : tMin),
        PAOne.fromULong(n == 0 ? null : tMax),
        PAOne.fromULong(tSum)
      };
    }

    // integer type? calculate as longs
    if (isIntegerType()) { // includes LongArray

      // 2020-07-28 Not Perfect!
      //  This handles byte/ubyte/short/ushort/int/uint fv mv correctly
      //  (MAX_VALUE isn't assumed to be missing value)
      //  but long still treats MAX_VALUE as missing value

      long mv = Long.MAX_VALUE;
      long fv = Long.MAX_VALUE;
      long tMin = Long.MAX_VALUE;
      long tMax = Long.MIN_VALUE;
      long tSum = 0;
      if (atts != null) {
        // the atts take precedence if they exist
        fv = atts.getLong("_FillValue"); // eg byte 127 -> 127
        mv = atts.getLong("missing_value"); // eg byte 127 -> 127
      }

      for (int i = 0; i < size; i++) {
        long d = getLong(i); // converts local missingValue to Long.MAX_VALUE
        // String2.log(">> calculateStats i=" + i + " n=" + n + " long=" + d);
        if (d == Long.MAX_VALUE || d == fv || d == mv) {
        } else {
          n++;
          tMin = Math.min(tMin, d);
          tMax = Math.max(tMax, d);
          tSum += d;
        }
      }
      // if (debugMode) String2.log(">> PrimitiveArray.calculateStats long n=" + n + " min=" + tMin
      // " max=" + tMax);
      return new PAOne[] {
        PAOne.fromInt(n),
        PAOne.fromLong(n == 0 ? Long.MAX_VALUE : tMin),
        PAOne.fromLong(n == 0 ? Long.MAX_VALUE : tMax),
        PAOne.fromLong(tSum)
      };
    }

    // float, double, string? calculate as double
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE; // not Double.MIN_VALUE
    double sum = 0;

    boolean isFloat = elementType() == PAType.FLOAT;
    double mv = Double.NaN;
    double fv = Double.NaN;
    if (atts != null) {
      fv = atts.getNiceDouble("_FillValue");
      mv = atts.getNiceDouble("missing_value");
    }
    boolean fvIsFinite = Double.isFinite(fv);
    boolean mvIsFinite = Double.isFinite(mv);
    int precision = isFloat ? 5 : 14;

    for (int i = 0; i < size; i++) {
      double d = getNiceDouble(i); // converts floats to nice doubles
      if (!Double.isFinite(d)
          || (fvIsFinite && Math2.almostEqual(precision, fv, d))
          || (mvIsFinite && Math2.almostEqual(precision, mv, d))) {
      } else {
        n++;
        min = Math.min(min, d);
        max = Math.max(max, d);
        sum += d;
      }
    }

    if (n == 0) {
      min = Double.NaN;
      max = Double.NaN;
    }
    // if (debugMode) String2.log(">> PrimitiveArray.calculateStats double n=" + n + " min=" + tMin
    // " max=" + tMax);
    return new PAOne[] {
      PAOne.fromInt(n),
      PAOne.fromDouble(isFloat ? Math2.floatToDouble(min) : min),
      PAOne.fromDouble(isFloat ? Math2.floatToDouble(max) : max),
      PAOne.fromDouble(sum)
    };
  }

  /** This returns a string with the stats from calculateStats in a consistent way. */
  public static String displayPAOneStats(PAOne[] stats) {
    return "n="
        + String2.right("" + stats[STATS_N], 7)
        + " min="
        + String2.right("" + stats[STATS_MIN], 20)
        + " max="
        + String2.right("" + stats[STATS_MAX], 20);
  }

  /** This returns a string with the stats from calculateStats in a consistent way. */
  public static String displayStats(double[] stats) {
    return "n="
        + String2.right(String2.genEFormat6(stats[STATS_N]), 7)
        + " min="
        + String2.right(String2.genEFormat6(stats[STATS_MIN]), 15)
        + " max="
        + String2.right(String2.genEFormat6(stats[STATS_MAX]), 15);
  }

  /** This calculates and returns a string with the stats for this pa. */
  public String statsString() {
    double stats[] = calculateStats();
    return "nNaN="
        + String2.right(String2.genEFormat6(size() - stats[STATS_N]), 7)
        + " "
        + displayStats(stats);
  }

  /**
   * This calculates min, max, and nValid for the values in this PrimitiveArray. Each data type's
   * missing value (e.g., Byte.MAX_VALUE) will be converted to NaN. Calculation of mean and variance
   * is via the methods described in Spicer, C.C. 1972. Algorithm AS 52 Calculation of Power Sums of
   * Deviations About the Mean, Appl. Stat. 21:226-7.
   *
   * @param atts The related attributes. If they have _FillValue and/or missing_value, those will be
   *     temporarily applied so the stats don't include them. !!! THESE SHOULD BE NOT-PACKED
   *     _FillValue and/or missing_value ATTRIBUTES. This can be null.
   * @return a double[] with dar[STATS_N] containing the number of valid values. dar[STATS_MIN]
   *     containing the minimum value, and dar[STATS_MAX] containing the maximum value.
   *     dar[STATS_SUM] containing the sum of the values. dar[4] containing the mean of the values.
   *     dar[5] containing the variance of the values. If n is 0, min and max will be NaN (or e.g.,
   *     Long.MAX_VALUE), and sum will be 0.
   */
  public double[] calculateStats2(Attributes atts) {
    long time = System.currentTimeMillis();

    // for long and ulong, this could claculate as BigDecimal
    // then convert to double results.

    // calculate as double
    int n = 0;
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE; // not Double.MIN_VALUE
    double mean = Double.NaN;
    double sum = 0;
    double powerSum2 = 0;
    double sd = Double.NaN;

    boolean isFloat = elementType() == PAType.FLOAT;
    double mv = Double.NaN;
    double fv = Double.NaN;
    if (atts != null) {
      fv = atts.getNiceDouble("_FillValue");
      mv = atts.getNiceDouble("missing_value");
    }
    boolean fvIsFinite = Double.isFinite(fv);
    boolean mvIsFinite = Double.isFinite(mv);
    int precision = isFloat ? 5 : 14;

    // for the Spicer algorithm, see cohort Descriptive.addData()
    for (int i = 0; i < size; i++) {
      double d = getNiceDouble(i); // converts floats to nice doubles
      if (!Double.isFinite(d)
          || (fvIsFinite && Math2.almostEqual(precision, fv, d))
          || (mvIsFinite && Math2.almostEqual(precision, mv, d))) {
      } else {
        n++;
        min = Math.min(min, d);
        max = Math.max(max, d);
        sum += d;
        if (n == 1) {
          mean = d;
        } else {
          double dx = (d - mean) / n;
          powerSum2 += (dx * dx * n) * (n - 1); // avoid n*(n-1) int arithmetic
          mean += dx;
        }
      }
    }

    if (n == 0) {
      min = Double.NaN;
      max = Double.NaN;
      sum = Double.NaN;
      mean = Double.NaN;
    } else if (n > 1) {
      sd = powerSum2 / (n - 1);
    }

    // if (debugMode) String2.log(">> PrimitiveArray.calculateStats double n=" + n + " min=" + tMin
    // " max=" + tMax);
    return new double[] {n, min, max, sum, mean, sd};
  }

  /**
   * This calculates the median of the values in this PrimitiveArray as a double. This assumes mv's
   * all sort lower and/or higher that finite values. Each data type's missing value (e.g.,
   * Byte.MAX_VALUE) will be converted to NaN.
   *
   * @param atts The related attributes. If they have _FillValue and/or missing_value, those will be
   *     temporarily applied so the stats don't include them. !!! THESE SHOULD BE NOT-PACKED
   *     _FillValue and/or missing_value ATTRIBUTES. This can be null.
   * @return the median.
   */
  public double calculateMedian(Attributes atts) {
    long time = System.currentTimeMillis();

    // for long and ulong, this could claculate as BigDecimal
    // then convert to double results.

    // calculate as double
    boolean isFloat = elementType() == PAType.FLOAT;
    double mv = Double.NaN;
    double fv = Double.NaN;
    if (atts != null) {
      fv = atts.getNiceDouble("_FillValue");
      mv = atts.getNiceDouble("missing_value");
    }
    boolean fvIsFinite = Double.isFinite(fv);
    boolean mvIsFinite = Double.isFinite(mv);
    int precision = isFloat ? 5 : 14;

    // rank
    int rank[] = rank(true);

    // find first finite value
    // This assumes mv's all sort lower and/or higher that finite values.
    int firstFinite = 0;
    while (firstFinite < size) {
      double d = getNiceDouble(rank[firstFinite]); // converts floats to nice doubles
      if (!Double.isFinite(d)
          || (fvIsFinite && Math2.almostEqual(precision, fv, d))
          || (mvIsFinite && Math2.almostEqual(precision, mv, d))) firstFinite++;
      else break;
    }
    if (firstFinite == size) // all mv
    return Double.NaN;

    // find last finite value
    int lastFinite = size - 1;
    while (lastFinite > firstFinite) {
      double d = getNiceDouble(rank[lastFinite]); // converts floats to nice doubles
      if (!Double.isFinite(d)
          || (fvIsFinite && Math2.almostEqual(precision, fv, d))
          || (mvIsFinite && Math2.almostEqual(precision, mv, d))) lastFinite--;
      else break;
    }

    // calculate median
    int half = firstFinite + (lastFinite - firstFinite + 1) / 2;
    if ((lastFinite - firstFinite + 1) % 2 == 0)
      // even number of values
      return (getNiceDouble(rank[half]) + getNiceDouble(rank[half - 1])) / 2.0;
    else
      // odd number of values so grab the one in the middle
      return getNiceDouble(rank[half]);
  }

  /**
   * This compares the values in this.row1 and otherPA.row2 and returns a negative integer, zero, or
   * a positive integer if the value at index1 is less than, equal to, or greater than the value at
   * index2. Think (ar[index1] - ar[index2]). The cohort missing value sorts highest.
   *
   * @param index1 an index number 0 ... size-1
   * @param otherPA the other PrimitiveArray which must be the same (or close) PAType.
   * @param index2 an index number 0 ... size-1
   * @return a negative integer, zero, or a positive integer if the value at index1 is less than,
   *     equal to, or greater than the value at index2.
   */
  public abstract int compare(int index1, PrimitiveArray otherPA, int index2);

  /**
   * This compares the values in row1 and row2 for SortComparator, and returns a negative integer,
   * zero, or a positive integer if the value at index1 is less than, equal to, or greater than the
   * value at index2. Think (ar[index1] - ar[index2]). The cohort missing value sorts highest.
   *
   * @param index1 an index number 0 ... size-1
   * @param index2 an index number 0 ... size-1
   * @return a negative integer, zero, or a positive integer if the value at index1 is less than,
   *     equal to, or greater than the value at index2.
   */
  public int compare(int index1, int index2) {
    return compare(index1, this, index2);
  }

  /**
   * This is like compare(), except for CharArray and StringArray (which overwrite this) it is fancy
   * caseInsensitive.
   *
   * @param index1 an index number 0 ... size-1
   * @param otherPA the other PrimitiveArray which must be the same (or close) PAType.
   * @param index2 an index number 0 ... size-1
   * @return a negative integer, zero, or a positive integer if the value at index1 is less than,
   *     equal to, or greater than the value at index2.
   */
  public int compareIgnoreCase(int index1, PrimitiveArray otherPA, int index2) {
    return compare(index1, otherPA, index2);
  }

  /**
   * This is like compare(), except for CharArray and StringArray (which overwrite this) it is fancy
   * caseInsensitive.
   *
   * @param index1 an index number 0 ... size-1
   * @param index2 an index number 0 ... size-1
   * @return a negative integer, zero, or a positive integer if the value at index1 is less than,
   *     equal to, or greater than the value at index2.
   */
  public int compareIgnoreCase(int index1, int index2) {
    return compareIgnoreCase(index1, this, index2);
  }

  /**
   * This copies the value in row 'from' to row 'to'. This does not check that 'from' and 'to' are
   * valid; the caller should be careful. The value for 'from' is unchanged.
   *
   * @param from an index number 0 ... size-1
   * @param to an index number 0 ... size-1
   */
  public abstract void copy(int from, int to);

  /**
   * This reorders the values in 'array' based on rank.
   *
   * @param rank is an Integer[] with values (0 ... size-1) which points to the row number for a row
   *     with a specific rank (e.g., rank[0] is the row number of the first item in the sorted list,
   *     rank[1] is the row number of the second item in the sorted list, ...).
   */
  public abstract void reorder(int rank[]);

  /** This reverses the order of the values in 'array'. */
  public void reverse() {
    // probably more efficient if there were a swap(rowA,rowB) method
    int rank[] = new int[size];
    for (int i = 0; i < size; i++) rank[size - 1 - i] = i;
    reorder(rank);
  }

  /**
   * This reverses the order of the bytes in each value, e.g., if the data was read from a
   * little-endian source.
   */
  public abstract void reverseBytes();

  /**
   * This writes 'size' elements to a DataOutputStream.
   *
   * @param dos the DataOutputStream
   * @return the number of bytes used per element (for Strings, this is the size of one of the
   *     strings, not others, and so is useless; for other types the value is consistent). But if
   *     size=0, this returns 0.
   * @throws Exception if trouble
   */
  public abstract int writeDos(DataOutputStream dos) throws Exception;

  /**
   * This writes one element to a DataOutputStream.
   *
   * @param dos the DataOutputStream
   * @param i the index of the element to be written
   * @return the number of bytes used for this element (for Strings, this varies; for others it is
   *     consistent)
   * @throws Exception if trouble
   */
  public abstract int writeDos(DataOutputStream dos, int i) throws Exception;

  /**
   * This reads/adds n elements from a DataInputStream.
   *
   * @param dis the DataInputStream
   * @param n the number of elements to be read/added
   * @throws Exception if trouble
   */
  public abstract void readDis(DataInputStream dis, int n) throws Exception;

  /**
   * This writes all the data to a DataOutputStream in the DODS Array format (see www.opendap.org
   * DAP 2.0 standard, section 7.3.2.1). See also the XDR standard
   * (http://tools.ietf.org/html/rfc4506#section-4.11). ByteArray, ShortArray, StringArray overwrite
   * this. ???Does CharArray need to overwrite this???
   *
   * @param dos
   */
  public void externalizeForDODS(DataOutputStream dos) throws Exception {
    dos.writeInt(size);
    dos.writeInt(size); // yes, a second time
    writeDos(dos);
  }

  /**
   * This writes one element to a DataOutputStream in the DODS Atomic-type format (see
   * www.opendap.org DAP 2.0 standard, section 7.3.2). See also the XDR standard
   * (http://tools.ietf.org/html/rfc4506#section-4.11). ByteArray, ShortArray, StringArray overwrite
   * this. ???Does CharArray need to overwrite this???
   *
   * @param dos
   * @param i the index of the element to be written
   */
  public void externalizeForDODS(DataOutputStream dos, int i) throws Exception {
    writeDos(dos, i);
  }

  /**
   * This reads/appends same-type values to this PrimitiveArray from a DODS DataInputStream, and is
   * thus the complement of externalizeForDODS.
   *
   * @param dis
   * @throws IOException if trouble
   */
  public abstract void internalizeFromDODS(DataInputStream dis) throws java.io.IOException;

  /**
   * This makes a PrimitiveArray by reading contiguous values from a RandomAccessFile. This doesn't
   * work for PAType.STRING. endIndex-startIndex must be less than Integer.MAX_VALUE.
   *
   * @param raf the RandomAccessFile
   * @param type the elementType of the original PrimitiveArray
   * @param start the raf offset of the start of the array (nBytes)
   * @param startIndex the index of the desired value (0..)
   * @param endIndex the index after the last desired value (0..)
   * @return a PrimitiveArray
   * @throws Exception if trouble
   */
  public static PrimitiveArray rafFactory(
      RandomAccessFile raf, PAType type, long start, long startIndex, long endIndex)
      throws Exception {

    long longN = endIndex - startIndex;
    String cause = "PrimitiveArray.rafFactory";
    Math2.ensureArraySizeOkay(longN, cause);
    int n = (int) longN;
    PrimitiveArray pa = factory(type, n, true); // active?
    raf.seek(start + pa.elementSize() * startIndex);

    // byte
    if (type == PAType.BYTE) {
      byte tar[] = ((ByteArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readByte();
      return pa;
    }

    // ubyte
    if (type == PAType.UBYTE) {
      byte tar[] = ((UByteArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readByte();
      return pa;
    }

    // short
    if (type == PAType.SHORT) {
      short tar[] = ((ShortArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readShort();
      return pa;
    }

    // ushort
    if (type == PAType.USHORT) {
      short tar[] = ((UShortArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readShort();
      return pa;
    }

    // int
    if (type == PAType.INT) {
      int tar[] = ((IntArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readInt();
      return pa;
    }

    // uint
    if (type == PAType.UINT) {
      int tar[] = ((UIntArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readInt();
      return pa;
    }

    // long
    if (type == PAType.LONG) {
      long tar[] = ((LongArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readLong();
      return pa;
    }

    // ulong
    if (type == PAType.ULONG) {
      long tar[] = ((ULongArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readLong();
      return pa;
    }

    // char
    if (type == PAType.CHAR) {
      char tar[] = ((CharArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readChar();
      return pa;
    }

    // double
    if (type == PAType.DOUBLE) {
      double tar[] = ((DoubleArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readDouble();
      return pa;
    }

    // float
    if (type == PAType.FLOAT) {
      float tar[] = ((FloatArray) pa).array;
      for (int i = 0; i < n; i++) tar[i] = raf.readFloat();
      return pa;
    }

    // no support for String
    throw new Exception("PrimitiveArray.rafFactory type '" + type + "' not supported.");
  }

  /**
   * This writes array[index] to a randomAccessFile at the current position.
   *
   * @param raf the RandomAccessFile
   * @param index
   * @throws Exception if trouble
   */
  public abstract void writeToRAF(RandomAccessFile raf, int index) throws Exception;

  /**
   * This reads one value from a randomAccessFile at the current position and adds it to the
   * PrimitiveArraay.
   *
   * @param raf the RandomAccessFile
   * @throws Exception if trouble
   */
  public abstract void readFromRAF(RandomAccessFile raf) throws Exception;

  /**
   * This tests if the other object is of the same type and has equal values. Note that for
   * integerTypes, the value of maxIsMV can be different if there are no relevant values.
   *
   * @param other
   * @return "" if equal, or message if not. o=null doesn't throw an exception.
   */
  public abstract String testEquals(Object other);

  /**
   * This tests if the other PrimitiveArray has almost equal values. If both are integer types or
   * String types, this is an exact test (and says null==null is true). If either are float types,
   * this tests almostEqual5() (and says NaN==NaN is true). If either are double types, this tests
   * almostEqual9() (and says NaN==NaN is true).
   *
   * @param other
   * @return "" if almost equal, or message if not. other=null throws an exception.
   */
  public String almostEqual(PrimitiveArray other) {
    return almostEqual(other, 9);
  }

  /**
   * This tests if the other PrimitiveArray has almost equal values. If both are integer types or
   * String types, this is an exact test (and says null==null is true).
   *
   * @param other
   * @param matchNDigits This is used if this or other is DoubleArray or FloatArray. Otherwise, this
   *     is ignored. (&lt;=)0=no testing. 1 to 18 tests hidiv(nDigits,2) digits if either is
   *     FloatArray, or nDigits if either is DoubleArray. (Integer.MAX_VALUE is interpreted as 9.)
   *     &gt;18 says to test exact equality.
   * @return "" if almost equal, or message if not. other=null throws an exception.
   */
  public String almostEqual(PrimitiveArray other, int matchNDigits) {
    // trouble: Should this handle different values of maxIsMV?

    if (size != other.size())
      return MessageFormat.format(ArrayDifferentSize, "" + size, "" + other.size());

    if (matchNDigits <= 0) // no testing
    return "";

    if (this instanceof StringArray || other instanceof StringArray) {
      for (int i = 0; i < size; i++) {
        String s1 = getString(i);
        String s2 = other.getString(i);
        if (s1 == null && s2 == null) {
        } else if (s1 != null && s2 != null && s1.equals(s2)) {
        } else {
          return MessageFormat.format(
              ArrayDifferentValue, "" + i, String2.toJson(s1), String2.toJson(s2));
        }
      }
      return "";
    }

    if (this instanceof FloatArray || other instanceof FloatArray) {
      matchNDigits = matchNDigits == Integer.MAX_VALUE ? 5 : matchNDigits;
      if (matchNDigits > 18) {
        for (int i = 0; i < size; i++) {
          float f1 = getFloat(i);
          float f2 = other.getFloat(i);
          if (Float.isFinite(f1)) {
            if (Float.isFinite(f2)) {
              if (f1 != f2) // exact
              return MessageFormat.format(ArrayDifferentValue, "" + i, "" + f1, "" + f2);
            } else {
              return MessageFormat.format(ArrayDifferentValue, "" + i, "" + f1, "" + f2);
            }
          } else if (Float.isFinite(f2)) {
            return MessageFormat.format(ArrayDifferentValue, "" + i, "" + f1, "" + f2);
          }
        }
      } else {
        int tMatchNDigits = Math2.hiDiv(matchNDigits, 2);
        for (int i = 0; i < size; i++)
          if (!Math2.almostEqual(
              tMatchNDigits, // this says NaN==NaN is true
              getFloat(i),
              other.getFloat(i)))
            return MessageFormat.format(
                ArrayDifferentValue, "" + i, "" + getFloat(i), "" + other.getFloat(i));
      }
      return "";
    }

    if (this instanceof DoubleArray || other instanceof DoubleArray) {
      matchNDigits = matchNDigits == Integer.MAX_VALUE ? 9 : matchNDigits;
      if (matchNDigits > 18) {
        for (int i = 0; i < size; i++) {
          double d1 = getDouble(i);
          double d2 = other.getDouble(i);
          if (Double.isFinite(d1)) {
            if (Double.isFinite(d2)) {
              if (d1 != d2) // exact
              return MessageFormat.format(ArrayDifferentValue, "" + i, "" + d1, "" + d2);
            } else {
              return MessageFormat.format(ArrayDifferentValue, "" + i, "" + d1, "" + d2);
            }
          } else if (Double.isFinite(d2)) {
            return MessageFormat.format(ArrayDifferentValue, "" + i, "" + d1, "" + d2);
          }
        }
      } else {
        for (int i = 0; i < size; i++)
          if (!Math2.almostEqual(
              matchNDigits, getDouble(i), other.getDouble(i))) // this says NaN==NaN is true
          return MessageFormat.format(
                ArrayDifferentValue, "" + i, "" + getDouble(i), "" + other.getDouble(i));
      }
      return "";
    }

    if (this instanceof ULongArray || other instanceof ULongArray) {
      for (int i = 0; i < size; i++) {
        BigInteger bi1 = getULong(i);
        BigInteger bi2 = other.getULong(i);
        if (bi1 == null && bi2 == null) {
        } else if (bi1 == null || bi2 == null || !bi1.equals(bi2))
          return MessageFormat.format(ArrayDifferentValue, "" + i, "" + bi1, "" + bi2);
      }
      return "";
    }

    // test via long's which handles mv in long, int, uint, and smaller classes
    for (int i = 0; i < size; i++)
      if (getLong(i) != other.getLong(i))
        return MessageFormat.format(
            ArrayDifferentValue, "" + i, "" + getLong(i), "" + other.getLong(i));
    return "";
  }

  /**
   * Given a sorted PrimitiveArray, stored to a randomAccessFile, this finds the index of an
   * instance of the value (not necessarily the first or last instance) (or -index-1 where it should
   * be inserted).
   *
   * @param raf the RandomAccessFile
   * @param type the element type of the original PrimitiveArray
   * @param start the raf offset of the start of the array
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (originalPrimitiveArray.size() - 1)
   * @param value the value you are searching for
   * @return the index of an instance of the value (not necessarily the first or last instance) (or
   *     -index-1 where it should be inserted, with extremes of -lowPo-1 and -(highPo+1)-1).
   * @throws Exception if trouble
   */
  public static long rafBinarySearch(
      RandomAccessFile raf, PAType type, long start, long lowPo, long highPo, PAOne value)
      throws Exception {

    // ensure lowPo <= highPo
    // lowPo == highPo is handled by the following two chunks of code
    if (lowPo > highPo)
      throw new RuntimeException(
          String2.ERROR
              + " in PrimitiveArray.rafBinarySearch: lowPo("
              + lowPo
              + ") > highPo("
              + highPo
              + ").");

    PAOne tValue = new PAOne(type);
    tValue.readFromRAF(raf, start, lowPo);
    // String2.log("rafBinarySearch value=" + value + " po=" + lowPo + " tValue=" + tValue);
    int compare = tValue.compareTo(value);
    if (compare == 0) return lowPo;
    if (compare > 0) return -lowPo - 1;

    tValue.readFromRAF(raf, start, highPo);
    // String2.log("rafBinarySearch value=" + value + " po=" + highPo + " tValue=" + tValue);
    compare = tValue.compareTo(value);
    if (compare == 0) return highPo;
    if (compare < 0) return -(highPo + 1) - 1;

    // repeatedly look at midpoint
    // If no match, this always ends with highPo - lowPo = 1
    //  and desired value would be in between them.
    while (highPo - lowPo > 1) {
      long midPo = (highPo + lowPo) / 2;
      tValue.readFromRAF(raf, start, midPo);
      // String2.log("rafBinarySearch value=" + value + " po=" + midPo + " tValue=" + tValue);
      compare = tValue.compareTo(value);
      if (compare == 0) return midPo;
      if (compare < 0) lowPo = midPo;
      else highPo = midPo;
    }

    // not found
    return -highPo - 1;
  }

  /**
   * Given a sorted PrimitiveArray, stored to a randomAccessFile, this finds the index of the first
   * element &gt;= value.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param raf the RandomAccessFile
   * @param type the element type of the original PrimitiveArray
   * @param start the raf offset of the start of the array
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (originalPrimitiveArray.size() - 1)
   * @param value the value you are searching for
   * @return the index of the first element &gt;= value (or highPo + 1, if there are none)
   * @throws Exception if trouble
   */
  public static long rafFirstGE(
      RandomAccessFile raf, PAType type, long start, long lowPo, long highPo, PAOne value)
      throws Exception {

    if (lowPo > highPo) return highPo + 1;
    long po = rafBinarySearch(raf, type, start, lowPo, highPo, value);

    // an exact match? find the first exact match
    PAOne tValue = new PAOne(type);
    if (po >= 0) {
      while (po > lowPo && tValue.readFromRAF(raf, start, po - 1).compareTo(value) == 0) po--;
      return po;
    }

    // no exact match? return the binary search po
    // thus returning a positive number
    // the inverse of -x-1 is -x-1 !
    return -po - 1;
  }

  /**
   * Given a sorted PrimitiveArray, stored to a randomAccessFile, this finds the index of the first
   * element &gt; or almostEqual5 to value.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param raf the RandomAccessFile
   * @param type the element type of the original PrimitiveArray
   * @param start the raf offset of the start of the array
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (originalPrimitiveArray.size() - 1)
   * @param value the value you are searching for
   * @param precision e.g., 5 for floats and 9 for doubles
   * @return the index of the first element &gt; or Math2.almostEqual5 to value (or highPo + 1, if
   *     there are none)
   * @throws Exception if trouble
   */
  public static long rafFirstGAE(
      RandomAccessFile raf,
      PAType type,
      long start,
      long lowPo,
      long highPo,
      PAOne value,
      int precision)
      throws Exception {

    if (lowPo > highPo) return highPo + 1;

    long po = rafBinarySearch(raf, type, start, lowPo, highPo, value);

    // no exact match? return the binary search po
    // thus returning a positive number
    // the inverse of -x-1 is -x-1 !
    if (po < 0) po = -po - 1;

    // find the first GAE
    PAOne tValue = new PAOne(type);
    while (po > lowPo && tValue.readFromRAF(raf, start, po - 1).almostEqual(precision, value)) po--;

    return po;
  }

  /**
   * Given a sorted PrimitiveArray, stored to a randomAccessFile, this finds the index of the last
   * element &lt;= value.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param raf the RandomAccessFile
   * @param type the element type of the original PrimitiveArray
   * @param start the raf offset of the start of the array
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (originalPrimitiveArray.size() - 1)
   * @param value the value you are searching for
   * @return the index of the first element &lt;= value (or -1, if there are none)
   * @throws Exception if trouble
   */
  public static long rafLastLE(
      RandomAccessFile raf, PAType type, long start, long lowPo, long highPo, PAOne value)
      throws Exception {

    if (lowPo > highPo) return -1;
    long po = rafBinarySearch(raf, type, start, lowPo, highPo, value);

    // an exact match? find the first exact match
    PAOne tValue = new PAOne(type);
    if (po >= 0) {
      while (po < highPo && tValue.readFromRAF(raf, start, po + 1).equals(value)) po++;
      return po;
    }

    // no exact match? return binary search po -1
    // thus returning a positive number
    // the inverse of -x-1 is -x-1 !
    return -po - 1 - 1;
  }

  /**
   * Given a sorted PrimitiveArray, stored to a randomAccessFile, this finds the index of the last
   * element &lt; or almostEqual to value.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param raf the RandomAccessFile
   * @param type the element type of the original PrimitiveArray
   * @param start the raf offset of the start of the array
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (originalPrimitiveArray.size() - 1)
   * @param value the value you are searching for
   * @param precision e.g., 5 for floats and 9 for doubles
   * @return the index of the first element &lt; or Math2.almostEqual to value (or -1, if there are
   *     none)
   * @throws Exception if trouble
   */
  public static long rafLastLAE(
      RandomAccessFile raf,
      PAType type,
      long start,
      long lowPo,
      long highPo,
      PAOne value,
      int precision)
      throws Exception {

    if (lowPo > highPo) return -1;
    long po = rafBinarySearch(raf, type, start, lowPo, highPo, value);

    // no exact match? return previous value (binary search po -1)
    // thus returning a positive number
    // the inverse of -x-1 is -x-1 !
    if (po < 0) po = -po - 1 - 1;

    // look for last almost equal value
    PAOne tValue = new PAOne(type);
    while (po < highPo && tValue.readFromRAF(raf, start, po + 1).almostEqual(precision, value))
      po++;

    return po;
  }

  /**
   * Given an ascending sorted PrimitiveArray, this finds the index of an instance of the value (not
   * necessarily the first or last instance) (or -index-1 where it should be inserted).
   *
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (size - 1)
   * @param value the value you are searching for
   * @return the index of an instance of the value (not necessarily the first or last instance) (or
   *     -index-1 where it should be inserted, with extremes of -lowPo-1 and -(highPo+1)-1). [So
   *     insert at -response-1.]
   * @throws RuntimeException if lowPo &gt; highPo.
   */
  public int binarySearch(int lowPo, int highPo, PAOne value) {

    // ensure lowPo <= highPo
    // lowPo == highPo is handled by the following two chunks of code
    if (lowPo > highPo)
      throw new RuntimeException(
          String2.ERROR
              + " in PrimitiveArray.binarySearch: lowPo("
              + lowPo
              + ") > highPo("
              + highPo
              + ").");

    PAOne tValue = new PAOne(elementType());
    tValue.readFrom(this, lowPo);
    int compare = tValue.compareTo(value);
    if (compare == 0) return lowPo;
    if (compare > 0) return -lowPo - 1;

    tValue.readFrom(this, highPo);
    compare = tValue.compareTo(value);
    if (compare == 0) return highPo;
    if (compare < 0) return -(highPo + 1) - 1;

    // repeatedly look at midpoint
    // If no match, this always ends with highPo - lowPo = 1
    //  and desired value would be in between them.
    while (highPo - lowPo > 1) {
      int midPo = (highPo + lowPo) / 2;
      tValue.readFrom(this, midPo);
      compare = tValue.compareTo(value);
      if (compare == 0) return midPo;
      if (compare < 0) lowPo = midPo;
      else highPo = midPo;
    }

    // not found
    return -highPo - 1;
  }

  /**
   * Given an ascending sorted PrimitiveArray, this finds the index of the first element &gt;=
   * value.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (size - 1)
   * @param value the value you are searching for
   * @return the index of the first element &gt;= value (or highPo + 1, if there are none)
   */
  public int binaryFindFirstGE(int lowPo, int highPo, PAOne value) {

    if (lowPo > highPo) return highPo + 1;

    int po = binarySearch(lowPo, highPo, value);

    // an exact match? find the first exact match
    PAOne tValue = new PAOne(elementType());
    if (po >= 0) {
      while (po > lowPo && tValue.readFrom(this, po - 1).equals(value)) po--;
      return po;
    }

    // no exact match? return the binary search po
    // thus returning a positive number
    // the inverse of -x-1 is -x-1 !
    return -po - 1;
  }

  /**
   * Given an ascending sorted PrimitiveArray, this finds the index of the first element &gt; or
   * almostEqual to value.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (size - 1)
   * @param value the value you are searching for
   * @param precision e.g., 5 for floats and 9 for doubles
   * @return the index of the first element &gt; or Math2.almostEqual to value (or highPo + 1, if
   *     there are none)
   */
  public int binaryFindFirstGAE(int lowPo, int highPo, PAOne value, int precision) {

    if (lowPo > highPo) return highPo + 1;
    int po = binarySearch(lowPo, highPo, value);

    // no exact match? start at high and work back
    // the inverse of -x-1 is -x-1 !
    if (po < 0) po = -po - 1;

    // find the first match
    PAOne tValue = new PAOne(elementType());
    while (po > lowPo && tValue.readFrom(this, po - 1).almostEqual(precision, value)) po--;

    return po;
  }

  /**
   * Given an ascending sorted PrimitiveArray, this finds the index of the last element &lt;= value.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (originalPrimitiveArray.size() - 1)
   * @param value the value you are searching for
   * @return the index of the first element &lt;= value (or -1, if there are none)
   */
  public int binaryFindLastLE(int lowPo, int highPo, PAOne value) {

    if (lowPo > highPo) return -1;
    int po = binarySearch(lowPo, highPo, value);

    // an exact match? find the first exact match
    PAOne tValue = new PAOne(elementType());
    if (po >= 0) {
      while (po < highPo && tValue.readFrom(this, po + 1).equals(value)) po++;
      return po;
    }

    // no exact match? return binary search po -1
    // thus returning a positive number
    // the inverse of -x-1 is -x-1 !
    return -po - 1 - 1;
  }

  /**
   * Given an ascending sorted PrimitiveArray, this finds the index of the last element &lt; or
   * almostEqual to value.
   *
   * <p>If firstGE &gt; lastLE, there are no matching elements (because the requested range is less
   * than or greater than all the values, or between two adjacent values).
   *
   * @param lowPo the low index to start with, usually 0
   * @param highPo the high index to start with, usually (originalPrimitiveArray.size() - 1)
   * @param value the value you are searching for
   * @param precision e.g., 5 for floats and 9 for doubles
   * @return the index of the first element &lt; or Math2.almostEqual to value (or -1, if there are
   *     none)
   */
  public int binaryFindLastLAE(int lowPo, int highPo, PAOne value, int precision) {

    if (lowPo > highPo) return -1;
    int po = binarySearch(lowPo, highPo, value);

    // no exact match? start at lower and work forward
    // the inverse of -x-1 is -x-1 !
    if (po < 0) po = -po - 1 - 1;

    // find the last match
    PAOne tValue = new PAOne(elementType());
    while (po < highPo && tValue.readFrom(this, po + 1).almostEqual(precision, value)) po++;

    return po;
  }

  /**
   * Find the closest element to x in an ascending sorted array. If there are duplicates, any may be
   * returned.
   *
   * @param x
   * @return the index of the index of the element closest to x. If x is NaN, this returns -1.
   */
  public int binaryFindClosest(double x) {
    if (Double.isNaN(x)) return -1;
    int i = binarySearch(0, size - 1, PAOne.fromDouble(x));
    if (i >= 0) return i; // success, exact match

    // insertionPoint at end point?
    int insertionPoint = -i - 1; // 0.. dar.length
    if (insertionPoint == 0) return 0;
    if (insertionPoint >= size) return size - 1;

    // insertionPoint between 2 points
    if (Math.abs(getDouble(insertionPoint - 1) - x) < Math.abs(getDouble(insertionPoint) - x))
      return insertionPoint - 1;
    else return insertionPoint;
  }

  /**
   * Find the closest element to x in an array (regardless of if sorted or not). If there are
   * duplicates, any may be returned (i.e., for now, not specified).
   *
   * @param x
   * @return the index of the index of the element closest to x. If x is NaN, this returns -1.
   */
  public int linearFindClosest(double x) {
    if (Double.isNaN(x)) return -1;
    double diff = Double.MAX_VALUE;
    int which = -1;
    for (int i = 0; i < size; i++) {
      double tDiff = Math.abs(getDouble(i) - x);
      if (tDiff < diff) {
        diff = tDiff;
        which = i;
      }
    }
    return which;
  }

  /**
   * This returns an PrimitiveArray which is the simplist possible type of PrimitiveArray which can
   * accurately hold the data.
   *
   * <p>If this source primitiveArray is a StringArray, then null, ".", "", and "NaN" are allowable
   * missing values for conversion to numeric types. And if a value in the column has an internal
   * "." (e.g., 5.0), the column will not be converted to an integer type.
   *
   * @param colName is for diagnostics only
   * @return the simplest possible PrimitiveArray (possibly this PrimitiveArray) (although not a
   *     CharArray). If the source isUnsigned, this returns this PA unchanged. For integer types,
   *     this only looks for signed integer types, not unsigned types. Starting with ERDDAP v2.10, a
   *     StringArray with longs will return a LongArray (even though long ints are often used as
   *     String-like identifiers and .nc3 files don't support longs).
   */
  public PrimitiveArray simplify(String colName) {
    // return unsigned arrays as is
    if (isUnsigned()) return this;

    PAType type = PAType.BYTE; // the current, simplest possible type
    int n = size();
    boolean isStringArray = this instanceof StringArray;
    boolean hasNaN = false;
    boolean hasSomething = false;
    DoubleArray newDoubleArray = isStringArray ? new DoubleArray(n, false) : null;
    for (int i = 0; i < n; i++) {
      double d = getDouble(i);
      if (isStringArray) {
        newDoubleArray.add(d);
        String s = getString(i);

        if (!String2.isSomething2(
            s)) // this catches a large number of string and numeric missing value stand-ins, but
          // not NaN
          continue;

        if (s.toLowerCase().equals("nan")) { // signifies a numeric missing value
          // non-specific, skip this row
          hasNaN = true;
          continue;
        }

        // there's something in this column other than e.g., ""
        hasSomething = true;

        // If a String is found (not an acceptable "NaN" string above), return original array
        // Look for e.g., serial number (e.g., 0153) with leading 0+digit that must be kept as
        // string.
        // But if first 2 chars are "0.", this is a number, e.g., 0.14
        if ((s.length() >= 2 && s.charAt(0) == '0' && s.charAt(1) >= '0' && s.charAt(1) <= '9')
            || Double.isNaN(d)) { // it evaluates to NaN (even though common missing value
          if (reallyVerbose)
            String2.log(
                "  PrimitiveArray.simplify says column="
                    + colName
                    + " is type=String because of value="
                    + s);
          return this; // it's already a StringArray
        }

        // if string source column with an internal '.', don't let it be an integer type;
        // always treat as at least a float
        if (s.indexOf('.') >= 0) type = type == PAType.DOUBLE ? PAType.DOUBLE : PAType.FLOAT;
        if (elementType() == type) return this; // it's already a DoubleArraay or FloatArray
        // else fall through to code below

        // else fall through to code below
      }

      // all types allow NaN
      if (Double.isNaN(d)) continue;

      // assume column contains only bytes
      // if not true, work way up: short -> int -> long -> float -> double -> String
      if (type == PAType.BYTE) {
        if (d != Math.rint(d)) {
          type = PAType.FLOAT;
        } else if (d < Byte.MIN_VALUE || d > Byte.MAX_VALUE) {
          type = PAType.SHORT;
          if (this instanceof CharArray || this instanceof ShortArray)
            return this; // don't continue; it would just check that a ShortArray contains shorts
        }
      }
      if (type == PAType.SHORT) {
        if (d != Math.rint(d)) {
          type = PAType.FLOAT;
        } else if (d < Short.MIN_VALUE || d > Short.MAX_VALUE) {
          type = PAType.INT;
          if (this instanceof IntArray)
            return this; // don't continue; it would just check that an IntArray contains ints
        }
      }
      if (type == PAType.INT) {
        if (d != Math.rint(d)) {
          type = PAType.FLOAT;
        } else if (d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
          type = PAType.LONG;
          if (this instanceof LongArray)
            return this; // don't continue; it would just check that a LongArray contains longs
        }
      }
      if (type == PAType.LONG) {
        if (d != Math.rint(d)
            || d < Long.MIN_VALUE
            || d
                > Long
                    .MAX_VALUE) { // good (checks range) but imperfect: Long.MAX_VALUE is imprecise
          // as a double
          // String2.log(">> simplify -> LONG because d=" + d);
          type = PAType.FLOAT; // not ULONG
          if (this instanceof FloatArray) // not ULongArray
          return this; // don't continue; it would just check that a FloatArray contains floats
        }
      }
      // Don't do this because:
      //  1) ULONG is poorly supported in file types
      //  2) ULONG isn't a superset of the previous types (which allow negative values). So -1
      // earlier won't be caught now.
      // if (type == PAType.ULONG) {
      //    if (d != Math.rint(d) || d < 0 || d > Math2.ULONG_MAX_VALUE_AS_DOUBLE) {  //good (checks
      // range) but imperfect: ULONG_MAX_VALUE_AS_DOUBLE is imprecise
      //        //String2.log(">> simplify -> FLOAT because d=" + d);
      //        type = PAType.FLOAT;
      //        if (this instanceof FloatArray)
      //            return this;  //don't continue; it would just check that a FloatArray contains
      // floats
      //    }
      // }
      if (type == PAType.FLOAT) {
        if (d < -Float.MAX_VALUE || d > Float.MAX_VALUE || d != Math2.niceDouble(d, 7)) {
          // String2.log(">> simplify -> DOUBLE because d=" + d);
          type = PAType.DOUBLE;
          if (this instanceof DoubleArray)
            return this; // don't continue; it would just check that a DoubletArray contains doubles
        }
      }
      // otherwise it is a valid double
    }

    // nothing in a StringArray?
    if (isStringArray && !hasSomething) {
      // hasNaN -> Double  else leave as StringArray
      return hasNaN ? PrimitiveArray.factory(PAType.DOUBLE, n, "") : this;
    }

    // make array of simplified type
    PrimitiveArray tSourcePA =
        newDoubleArray == null ? this : newDoubleArray; // newDoubleArray has already parsed values
    if (type == PAType.BYTE) return new ByteArray(tSourcePA);
    if (type == PAType.SHORT) return new ShortArray(tSourcePA);
    if (type == PAType.INT) return new IntArray(tSourcePA);
    if (type == PAType.LONG) return new LongArray(this); // for full precision, use real sourcePA
    if (type == PAType.FLOAT) return new FloatArray(tSourcePA);
    if (type == PAType.DOUBLE) return new DoubleArray(tSourcePA);
    throw new IllegalArgumentException(
        String2.ERROR + " in PrimitiveArray.simplify: unknown new type=" + type + ".");
  }

  /**
   * This appends the data in another primitiveArray to the current data. WARNING: information may
   * be lost from the incoming primitiveArray if this primitiveArray is of a smaller type; see
   * needPAType().
   *
   * @param primitiveArray primitiveArray must be the same or a narrower data type, or the data will
   *     be rounded.
   */
  public abstract void append(PrimitiveArray primitiveArray);

  /**
   * This appends the data in another primitiveArray to the current data. This "raw" variant leaves
   * missingValue from smaller data types (e.g., ByteArray missingValue=127) AS IS. WARNING:
   * information may be lost from the incoming primitiveArray if this primitiveArray is of a simpler
   * type.
   *
   * @param primitiveArray primitiveArray must be a narrower data type, or the data will be rounded.
   */
  public abstract void rawAppend(PrimitiveArray primitiveArray);

  /**
   * This ranks this primitiveArray.
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param ascending a boolean indicating ascending or descending order.
   * @return an int[] with values (0 ... size-1) which points to the row number for a row with a
   *     specific rank (e.g., rank[0] is the row number of the first item in the sorted list,
   *     rank[1] is the row number of the second item in the sorted list, ...).
   */
  public int[] rank(boolean ascending) {
    ArrayList<PrimitiveArray> table = new ArrayList<>();
    table.add(this);
    return rank(table, new int[] {0}, new boolean[] {ascending});
  }

  /**
   * Given table[], keys[], and ascending[], this creates an int[] with the ranks the rows of the
   * table.
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param table a List of PrimitiveArrays
   * @param keys an array of the key column numbers (each is 0..nColumns-1, the first key is the
   *     most important) which are used to determine the sort order
   * @param ascending an array of booleans corresponding to the keys indicating if the arrays are to
   *     be sorted by a given key in ascending or descending order.
   * @return an int[] with values (0 ... size-1) which points to the row number for a row with a
   *     specific rank (e.g., rank[0] is the row number of the first item in the sorted list,
   *     rank[1] is the row number of the second item in the sorted list, ...).
   */
  public static int[] rank(List<PrimitiveArray> table, int keys[], boolean[] ascending) {
    return lowRank(new RowComparator(table, keys, ascending), table);
  }

  /** This is like rank, but StringArrays are tested case insensitively. */
  public static int[] rankIgnoreCase(List<PrimitiveArray> table, int keys[], boolean[] ascending) {
    return lowRank(new RowComparatorIgnoreCase(table, keys, ascending), table);
  }

  private static int[] lowRank(RowComparator comparator, List<PrimitiveArray> table) {

    // create the rowArray with pointer to specific rows
    int n = table.get(0).size();
    Integer[] rowArray = new Integer[n];
    for (int i = 0; i < n; i++) rowArray[i] = i;

    // sort the rows
    Arrays.sort(rowArray, comparator); // this is "stable"
    // String2.log("rank results: " + String2.toCSSVString(integerArray));

    // create the int[]
    int[] newArray = new int[n];
    for (int i = 0; i < n; i++) newArray[i] = rowArray[i];

    return newArray;
  }

  /**
   * Given a List of PrimitiveArrays, which represents a table of data, this sorts all of the
   * PrimitiveArrays in the table based on the keys and ascending values.
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param table a List of PrimitiveArray[]
   * @param keys an array of the key column numbers (each is 0..nColumns-1, the first key is the
   *     most important) which are used to determine the sort order
   * @param ascending an array of booleans corresponding to the keys indicating if the arrays are to
   *     be sorted by a given key in ascending or descending order.
   */
  public static void sort(List<PrimitiveArray> table, int[] keys, boolean[] ascending) {

    // rank the rows
    int[] ranks = rank(table, keys, ascending);

    // reorder the columns
    for (int col = 0; col < table.size(); col++) {
      table.get(col).reorder(ranks);
    }
  }

  /** This is like sort, but StringArrays are tested case insensitively. */
  public static void sortIgnoreCase(List<PrimitiveArray> table, int[] keys, boolean[] ascending) {

    // rank the rows
    int[] ranks = rankIgnoreCase(table, keys, ascending);

    // reorder the columns
    for (int col = 0; col < table.size(); col++) {
      table.get(col).reorder(ranks);
    }
  }

  /**
   * Given a List of PrimitiveArrays, which represents a table of data, this copies the values from
   * one row to another already extant row (without affecting any other rows).
   *
   * @param table a List of PrimitiveArray
   * @param from the 'from' row
   * @param to the 'to' row
   */
  public static void copyRow(List<PrimitiveArray> table, int from, int to) {
    int nColumns = table.size();
    for (int col = 0; col < nColumns; col++) table.get(col).copy(from, to);
  }

  /**
   * Given a sorted (regular or sortIgnoreCase) PrimitiveArray List, this looks for adjacent
   * identical rows of data and removes the duplicates.
   *
   * @return the number of duplicates removed
   */
  public int removeDuplicates() {
    return removeDuplicates(false);
  }

  /**
   * Given a sorted (plain or sortIgnoreCase) PrimitiveArray List, this looks for adjacent identical
   * rows of data and removes the duplicates.
   *
   * @param logDuplicates if true, this prints duplicates to String2.log
   * @return the number of duplicates removed
   */
  public int removeDuplicates(boolean logDuplicates) {
    return removeDuplicates(logDuplicates, null);
  }

  /**
   * Given a sorted (plain or sortIgnoreCase) PrimitiveArray List, this looks for adjacent identical
   * rows of data and removes the duplicates.
   *
   * @param logDuplicates if true, this prints duplicates to String2.log
   * @param sb if not null, duplicates are appended to sb
   * @return the number of duplicates removed
   */
  public int removeDuplicates(boolean logDuplicates, StringBuilder sb) {

    boolean logActive = logDuplicates || sb != null;
    int nRows = size;
    if (nRows <= 1) return 0;
    int nUnique = 1; // row 0 is unique
    for (int row = 1; row < nRows; row++) { // start at 1; compare to previous row
      // does it equal row above?
      boolean equal = compare(row - 1, row) == 0;
      if (equal) {
        if (logActive) {
          String msg =
              "  Removing duplicates at [" + (row - 1) + "] and [" + row + "] = " + getString(row);
          if (logDuplicates) String2.log(msg);
          if (sb != null) {
            sb.append(msg);
            sb.append('\n');
          }
        }
      } else {
        // not equal? copy row 'row' to row 'nUnique'
        if (row != nUnique) copy(row, nUnique);
        nUnique++;
      }
    }

    // remove the stuff at the end
    removeRange(nUnique, nRows);

    return nRows - nUnique;
  }

  /**
   * Given a (presumably) PrimitiveArray List, which represents a sorted table of data, this looks
   * for adjacent identical rows of data and removes the duplicates.
   *
   * @param table a List of PrimitiveArray
   * @return the number of duplicates removed
   */
  public static int removeDuplicates(List<PrimitiveArray> table) {

    int nRows = table.get(0).size();
    if (nRows <= 1) return 0;
    int nColumns = table.size();
    int nUnique = 1; // row 0 is unique
    for (int row = 1; row < nRows; row++) { // start at 1; compare to previous row
      // does it equal row above?
      boolean equal = true;
      for (int col = 0; col < nColumns; col++) {
        if (table.get(col).compare(row - 1, row) != 0) {
          equal = false;
          break;
        }
      }
      if (!equal) {
        // no? copy row 'row' to row 'nUnique'
        if (row != nUnique)
          for (int col = 0; col < nColumns; col++) table.get(col).copy(row, nUnique);
        nUnique++;
      }
    }

    // remove the stuff at the end
    for (int col = 0; col < nColumns; col++) table.get(col).removeRange(nUnique, nRows);

    return nRows - nUnique;
  }

  /**
   * Given a sorted (plain or sortIgnoreCase) PrimitiveArray List, this counts adjacent identical
   * rows.
   *
   * @param logDuplicates if true, this prints duplicates to String2.log
   * @return the number of duplicates. Specifically, this is the number that would be removed by
   *     removeDuplicates.
   */
  public int countDuplicates(boolean logDuplicates, boolean isEpochSeconds) {

    int nRows = size;
    if (nRows <= 1) return 0;
    int nDuplicates = 0;
    for (int row = 1; row < nRows; row++) { // start at 1; compare to previous row
      // does it equal row above?
      if (compare(row - 1, row) == 0) {
        ++nDuplicates;
        if (logDuplicates) {
          String s =
              isEpochSeconds
                  ? Calendar2.safeEpochSecondsToIsoStringTZ(getDouble(row), "NaN")
                  : getString(row);
          String2.log(
              "  duplicate #" + nDuplicates + ": [" + (row - 1) + "] and [" + row + "] = " + s);
        }
      }
    }
    return nDuplicates;
  }

  /**
   * Given another PrimitiveArray List sorted (this and other must be sort(), not sortIgnoreCase),
   * this just keeps the values which are in both PAs (the intersection(union) of the two PAs). <br>
   * Like Java Collection.retainAll().
   *
   * @param pa2 the other PrimitiveArray. It may be a different type. But this doesn't work quite
   *     right if one is a StringArray and the other isn't, and there are missing values in both.
   */
  public void inCommon(PrimitiveArray pa2) {
    int size1 = size;
    int size2 = pa2.size();
    int po1 = 0, po2 = 0;
    BitSet keep1 = new BitSet(size1);
    keep1.set(0, size1, false);
    boolean isNumeric1 = !(this instanceof StringArray);
    boolean isNumeric2 = !(pa2 instanceof StringArray);
    while (po1 < size1 && po2 < size2) {
      String s1 = getString(po1);
      String s2 = pa2.getString(po2);
      if (isNumeric1) {
        if (s1.length() == 0) s1 = "NaN"; // to reflect that NaNs sort high in numeric PAs
      } else {
        if (s1 == null) {
          if (s2 == null) {
            keep1.set(po1);
            po1++;
            po2++;
            continue;
          } else {
            po1++;
            continue;
          }
        }
      }
      if (isNumeric2) {
        if (s2.length() == 0) s2 = "NaN"; // to reflect that NaNs sort high in numeric PAs
      } else {
        if (s2 == null) {
          po2++;
          continue;
        }
      }

      int compare = s1.compareTo(s2);
      if (compare < 0) po1++;
      else if (compare > 0) po2++;
      else {
        keep1.set(po1);
        po1++;
        po2++;
      }
    }
    justKeep(keep1);
  }

  /**
   * This removes rows in which the value in 'column' are less than the value in the previous row.
   * Rows with values of NaN or bigger than 1e300 are also removed. !!!Trouble: one erroneous big
   * value will cause all subsequent valid values to be tossed.
   *
   * @param table a List of PrimitiveArray
   * @param column the column which should be ascending
   * @return the number of rows removed
   */
  public static int ensureAscending(List table, int column) {

    PrimitiveArray columnPA = (PrimitiveArray) table.get(column);
    int nRows = columnPA.size();
    int nColumns = table.size();
    int nGood = 0;
    double lastGood = -Double.MAX_VALUE;
    for (int row = 0; row < nRows; row++) {
      // is this a good row?
      double d = columnPA.getDouble(row);
      if (Double.isFinite(d) && d < 1e300 && d >= lastGood) {
        // copy row 'row' to row 'nGood'
        if (row != nGood)
          for (int col = 0; col < nColumns; col++)
            ((PrimitiveArray) table.get(col)).copy(row, nGood);
        nGood++;
        lastGood = d;
      } else {
      }
    }

    // remove the stuff at the end
    for (int col = 0; col < nColumns; col++)
      ((PrimitiveArray) table.get(col)).removeRange(nGood, nRows);

    int nRemoved = nRows - nGood;
    if (nRemoved > 0)
      String2.log(
          "PrimitveArray.ensureAscending nRowsRemoved=" + nRemoved + " lastGood=" + lastGood);

    return nRemoved;
  }

  /**
   * Given two PrimitivesArray[]'s (representing two tables of data with the same number columns),
   * this appends table2 to the end of table1.
   *
   * <p>The columns types may be different. If table2's column is narrower, the data is simply
   * appended to table1. If table2's column is wider, a new wider table1 is made before appending
   * table2's data. table1 and its columns will be affected by this method; table1 may contain
   * different PrimitiveArrays at the end. table2 and its columns will not be changed by this
   * method.
   *
   * @param table1 a List of PrimitiveArrays; it will contain the resulting table, perhaps
   *     containing some different PrimitiveArrays, always containing all the data from table1 and
   *     table2.
   * @param table2 a List of PrimitiveArrays
   */
  public static void append(List table1, List table2) {

    if (table1.size() != table2.size())
      throw new RuntimeException(
          MessageFormat.format(ArrayAppendTables, "" + table1.size(), "" + table2.size()));

    // append table2 to the end of table1
    for (int col = 0; col < table1.size(); col++) {
      // if needed, make a new wider PrimitiveArray in table1
      PrimitiveArray pa1 = (PrimitiveArray) table1.get(col);
      PrimitiveArray pa2 = (PrimitiveArray) table2.get(col);

      PAType needPAType = pa1.needPAType(pa2.elementType());
      if (pa1.elementType() != needPAType) {
        PrimitiveArray newPa1 =
            PrimitiveArray.factory(needPAType, pa1.size() + pa2.size(), false); // active?
        newPa1.append(pa1);
        pa1 = newPa1;
        table1.set(col, pa1);
      }

      // append the data from table2 to table1
      pa1.append(pa2);
    }
  }

  /**
   * Given two PrimitivesArray[]'s (representing two tables of data with the same number columns),
   * this merges table2 and table1 according to the sort order defined by keys and ascending. The
   * columns types may be different; if table2's column is wider, its data will be narrowed when it
   * is appended. table2 will not be changed by this method.
   *
   * @param table1 a List of PrimitiveArrays; it will contain the resulting table, perhaps
   *     containing some different PrimitiveArrays, always containing all the data from table1 and
   *     table2.
   * @param table2 a List of PrimitiveArrays
   * @param keys an array of the key column numbers (each is 0..nColumns-1, the first key is the
   *     most important) which are used to determine the sort order
   * @param ascending an array of booleans corresponding to the keys indicating if the arrays are to
   *     be sorted by a given key in ascending or descending order.
   * @param removeDuplicates specifies if completely identical rows should be removed.
   */
  public static void merge(
      List table1, List table2, int[] keys, boolean ascending[], boolean removeDuplicates) {
    // the current approach is quick, fun, and easy, but uses lots of memory
    // FUTURE: if needed, this could be done in a more space-saving way:
    //   sort each table, then merge table2 into table1,
    //   but would have to be careful to avoid slowness from inserting rows
    //   of table2 into table1, or space lost to copying to a 3rd table.

    append(table1, table2); // this is fast (although uses lots of memory)
    sort(table1, keys, ascending); // this is fast
    if (removeDuplicates) removeDuplicates(table1); // this is fast
  }

  /**
   * For all values, this unpacks the values by multipling by scale and then adding addOffset.
   * Calculations are done as doubles then, if necessary, rounded and stored.
   *
   * @param scale
   * @param addOffset
   */
  public void scaleAddOffset(double scale, double addOffset) {
    if (scale == 1 && addOffset == 0) return;
    for (int i = 0; i < size; i++)
      setDouble(i, getDouble(i) * scale + addOffset); // NaNs remain NaNs
  }

  /**
   * For all values, this packs the values by adding addOffset then multipling by scale.
   * Calculations are done as doubles then, if necessary, rounded and stored.
   *
   * @param scale
   * @param addOffset
   */
  public void addOffsetScale(double addOffset, double scale) {
    if (scale == 1 && addOffset == 0) return;
    for (int i = 0; i < size; i++)
      setDouble(i, (getDouble(i) + addOffset) * scale); // NaNs remain NaNs
  }

  /** This variant assumes sourceIsUnsigned=false. */
  public PrimitiveArray scaleAddOffset(PAType destElementPAType, double scale, double addOffset) {
    return scaleAddOffset(false, destElementPAType, scale, addOffset);
  }

  /**
   * This returns a PrimitiveArray of type elementType which has unpacked values (scale then
   * addOffset values applied). Calculations are done as doubles then, if necessary, rounded and
   * stored. The returned PrimitiveArray is new if the destination type does not match the source
   * type.
   *
   * @param destElementPAType
   * @param sourceIsUnsigned if true, integer-type source values will be interpreted as unsigned
   *     values.
   * @param scale
   * @param addOffset
   * @return a new (always) PrimitiveArray
   */
  public PrimitiveArray scaleAddOffset(
      boolean sourceIsUnsigned, PAType destElementPAType, double scale, double addOffset) {
    // Don't create a new PA if we don't need to.
    PrimitiveArray pa = this;
    if (this.elementType() != destElementPAType) {
      pa = factory(destElementPAType, size, true);
    }
    if (sourceIsUnsigned) {
      for (int i = 0; i < size; i++)
        pa.setDouble(i, getUnsignedDouble(i) * scale + addOffset); // NaNs remain NaNs
    } else {
      for (int i = 0; i < size; i++)
        pa.setDouble(i, getDouble(i) * scale + addOffset); // NaNs remain NaNs
    }
    return pa;
  }

  /**
   * This returns a new (always) PrimitiveArray of type destElementPAType which has had the packed
   * values (addOffset then scale values applied). Calculations are done as doubles then, if
   * necessary, rounded and stored.
   *
   * @param destElementPAType
   * @param addOffset
   * @param scale
   * @return a new (always) PrimitiveArray
   */
  public PrimitiveArray addOffsetScale(PAType destElementPAType, double addOffset, double scale) {
    PrimitiveArray pa = factory(destElementPAType, size, true);
    for (int i = 0; i < size; i++)
      pa.setDouble(i, (getDouble(i) + addOffset) * scale); // NaNs remain NaNs
    return pa;
  }

  /**
   * This populates 'indices' with the indices (ranks) of the values in this PrimitiveArray (ties
   * get the same index). For example, 10,10,25,3 returns 1,1,2,0.
   *
   * @param indices the intArray that will capture the indices of the values (ties get the same
   *     index). For example, 10,10,25,3 returns 1,1,2,0.
   * @return a PrimitveArray (the same type as this class) with the unique values, sorted
   */
  public abstract PrimitiveArray makeIndices(IntArray indices);

  /**
   * This changes all instances of the first value to the second value. Note that, e.g.,
   * ByteArray.switchFromTo("127", "") will correctly detect that 127=127 and do nothing.
   *
   * @param from the original value (use "" for standard missingValue)
   * @param to the new value (use "" for standard missingValue)
   * @return the number of values switched
   */
  public abstract int switchFromTo(String from, String to);

  /**
   * If the primitiveArray has fake _FillValue and/or missing_values (e.g., -9999999), those values
   * are converted to PrimitiveArray-style missing values (NaN, or MAX_VALUE for integer types).
   *
   * <p>2020-07-28 If mv or fv is not null, this calls this.setMaxIsMv(true);
   *
   * @param fakeFillValue (e.g., -9999999) from colAttributes.getDouble("_FillValue"); or null if
   *     none
   * @param fakeMissingValue (e.g., -9999999) from colAttributes.getDouble("missing_value"); or null
   *     if none
   * @return the number of missing values converted
   */
  public int convertToStandardMissingValues(
      String fakeFillValue, String fakeMissingValue) { // or use PAOne's?
    // is _FillValue used?    switch data to standard mv
    // String2.log(">> PrimitiveArray.convertToStandardMissingValues " + elementType() + "
    // fakeFillValue=" + fakeFillValue + " fakeMissingValue=" + fakeMissingValue);
    int nSwitched = 0;
    if (fakeFillValue != null) {
      nSwitched += switchFromTo(fakeFillValue, "");
      setMaxIsMV(true); // just the presence of _FillValue means maxIsMV should be true
    }

    // is missing_value used?    switch data to standard mv
    // String2.log ...
    if (fakeMissingValue != null && !fakeMissingValue.equals(fakeFillValue)) {
      nSwitched += switchFromTo(fakeMissingValue, "");
      setMaxIsMV(true); // just the presence of missing_value means maxIsMV should be true
    }

    return nSwitched;
  }

  /**
   * For any non-StringArray and non-CharArray, this changes all standard missing values (MAX_VALUE
   * or NaN's) to fakeMissingValues.
   *
   * @param fakeMissingValue
   * @return the number of values switched
   */
  public int switchNaNToFakeMissingValue(String fakeMissingValue) {
    PAType paType = elementType();
    if (paType == PAType.STRING) return 0;
    if (paType == PAType.FLOAT || paType == PAType.DOUBLE)
      return switchFromTo("", fakeMissingValue);

    // now just types that support maxIsMV
    if (!String2.isSomething(fakeMissingValue) && !fakeMissingValue.equals("NaN")) {
      int n = switchFromTo("", fakeMissingValue);
      setMaxIsMV(false);
      return n;
    } else {
      // there may be some if maxIsMV, but no value to switch to,
      // so leave maxIsMV as is.
      return 0;
    }
  }

  /**
   * For FloatArray and DoubleArray, this changes all fakeMissingValues to standard missing values
   * (NaN's).
   *
   * <p>2020-07-28 This always calls setMaxIsMv(true);
   *
   * @param fakeMissingValue
   * @return the number of missing values converted
   */
  public int switchFakeMissingValueToNaN(double fakeMissingValue) {
    setMaxIsMV(true);
    if (Double.isFinite(fakeMissingValue)
        &&
        // ???why just FloatArray and DoubleArray???
        (this instanceof FloatArray || this instanceof DoubleArray))
      return switchFromTo("" + fakeMissingValue, "");
    return 0;
  }

  /**
   * This tests if the values in the array are sorted in ascending order (ties are ok). This details
   * of this test are geared toward determining if the values are suitable for binarySearch.
   *
   * @return "" if the values in the array are sorted in ascending order; or an error message if not
   *     (i.e., if descending or unordered). If size is 0 or 1 (non-missing value), this returns "".
   *     A missing value returns an error message.
   */
  public String isAscending() {
    if (size == 0) return "";
    for (int i = 1; i < size; i++) {
      if (compare(i - 1, i) > 0) {
        return MessageFormat.format(
            ArrayNotAscending,
            getClass().getSimpleName(),
            "[" + (i - 1) + "]=" + getRawestString(i - 1) + " > [" + i + "]=" + getRawestString(i));
      }
    }
    if (isMissingValue(size - 1))
      return MessageFormat.format(
          ArrayNotAscending,
          getClass().getSimpleName(),
          "[" + (size - 1) + "]=(" + ArrayMissingValue + ")");
    return "";
  }

  /**
   * This tests if the values in the array are sorted in descending order (ties are ok).
   *
   * @return "" if the values in the array are sorted in descending order; or an error message if
   *     not (i.e., if ascending or unordered). If size is 0 or 1 (non-missing value), this returns
   *     "". A missing value returns an error message.
   */
  public String isDescending() {
    if (size == 0) return "";
    if (isMissingValue(0))
      return MessageFormat.format(
          ArrayNotDescending, getClass().getSimpleName(), "[0]=(" + ArrayMissingValue + ")");
    for (int i = 1; i < size; i++) {
      if (compare(i - 1, i) < 0) {
        return MessageFormat.format(
            ArrayNotDescending,
            getClass().getSimpleName(),
            "[" + (i - 1) + "]=" + getRawestString(i - 1) + " < [" + i + "]=" + getRawestString(i));
      }
    }
    return "";
  }

  /**
   * This tests for adjacent tied values and returns the index of the first tied value. Adjacent
   * NaNs are treated as ties.
   *
   * @return the index of the first tied value (or -1 if none).
   */
  public abstract int firstTie();

  /**
   * This tests if all of the values are identical.
   *
   * @return true if size == 0 or all of the values are identical.
   */
  public boolean allSame() {
    for (int i = 1; i < size; i++) {
      if (compare(i - 1, i) != 0) return false;
    }
    return true;
  }

  /**
   * This compares this PrimitiveArray's values to anothers, string representation by string
   * representation, and returns the first index where different. this.get(i)=null and
   * other.get(i)==null is treated as same value.
   *
   * @param other
   * @return index where first different (or -1 if same). Note that the index may equal the size of
   *     this or the other primitiveArray.
   */
  public int diffIndex(PrimitiveArray other) {
    int i = 0;
    int otherSize = other.size();

    while (true) {
      if (i == size && size == otherSize) return -1;
      if (i == size || i == otherSize) return i;
      String s = getString(i);
      String so = other.getString(i);
      if (s == null && so != null) return i;
      if (so == null && s != null) return i;
      if (s != null && so != null && !s.equals(so)) return i;
      i++;
    }

    // you could do a double test if both pa's were numeric
    // but tests with inifinity and nan are awkward and time consuming
    // so string test is pretty good approach.
  }

  /**
   * This compares this PrimitiveArray's values to anothers, string representation by string
   * representation, and returns a String indicating where different (or "" if not different).
   * this.get(i)=null and other.get(i)==null is treated as same value.
   *
   * @param other (or old)
   * @return String indicating where different (or "" if not different).
   */
  public String diffString(PrimitiveArray other) {
    int diffi = diffIndex(other);
    if (diffi == -1) return "";
    String s1 = diffi == size ? null : getString(diffi);
    String s2 = diffi == other.size() ? null : other.getString(diffi);
    return MessageFormat.format("  " + ArrayDiffString, "" + diffi, s2, s1);
  }

  /**
   * This compares this PrimitiveArray's values to anothers, string representation by string
   * representation, and throws Exception if different. this.get(i)=null and other.get(i)==null is
   * treated as same value.
   *
   * @param other
   * @throws Exception if different
   */
  public void diff(PrimitiveArray other) throws Exception {
    int diffi = diffIndex(other);
    if (diffi == -1) return;
    String s1 = diffi == size ? null : getString(diffi);
    String s2 = diffi == other.size() ? null : other.getString(diffi);
    if (!Test.equal(s1, s2))
      throw new RuntimeException(
          String2.ERROR + ": The PrimitiveArrays differ at [" + diffi + "]:\n" + s1 + "\n" + s2);
  }

  /**
   * This tests if the values in the array are evenly spaced (ascending or descending).
   *
   * @return "" if the values in the array are evenly spaced; or an error message if not. If size is
   *     0 or 1, this returns "".
   */
  public String isEvenlySpaced() {
    // This version works for all integer-based values.
    // StringArray, FloatArray and DoubleArray overwrite this.
    if (size <= 2) return "";
    // average is closer to exact than first diff
    // and usually detects not-evenly-spaced anywhere in the array on first test!
    double average = (getDouble(size - 1) - getDouble(0)) / (size - 1.0);
    for (int i = 1; i < size; i++) {
      if (getDouble(i) - getDouble(i - 1) != average) { // integer types must be exact
        return MessageFormat.format(
            ArrayNotEvenlySpaced,
            getClass().getSimpleName(),
            "" + (i - 1),
            "" + getDouble(i - 1),
            "" + i,
            "" + getDouble(i),
            "" + (getDouble(i) - getDouble(i - 1)),
            "" + average);
      }
    }
    return "";
  }

  /**
   * This returns a string indicating the smallest and largest actual spacing (not absolute values)
   * between two adjacent values. If there are ties, this returns the first one found. If size &lt;=
   * 2, this returns "".
   *
   * @return a string indicating the smallest and largest actual spacing (not absolute values)
   *     between two adjacent values.
   */
  public String smallestBiggestSpacing() {
    if (size <= 2) return "";
    int smalli = 1, bigi = 1;
    double small = getDouble(1) - getDouble(0);
    double big = small;
    for (int i = 2; i < size; i++) {
      double diff = getDouble(i) - getDouble(i - 1);
      if (diff < small) {
        smalli = i;
        small = diff;
      } else if (diff > big) {
        bigi = i;
        big = diff;
      }
    }
    return "    smallest spacing="
        + small
        + ": ["
        + (smalli - 1)
        + "]="
        + getDouble(smalli - 1)
        + ", ["
        + smalli
        + "]="
        + getDouble(smalli)
        + "\n"
        + "    biggest  spacing="
        + big
        + ": ["
        + (bigi - 1)
        + "]="
        + getDouble(bigi - 1)
        + ", ["
        + bigi
        + "]="
        + getDouble(bigi);
  }

  /**
   * This finds the number of non-missing values, and the index of the min and max value.
   *
   * @return int[3], [0]=the number of non-missing values, [1]=index of min value (if tie, index of
   *     last found; -1 if all mv), [2]=index of max value (if tie, index of last found; -1 if all
   *     mv).
   */
  public abstract int[] getNMinMaxIndex();

  /**
   * This returns the min and max of the non-null or "" strings (by simple comparison).
   *
   * @return String[3], 0=""+n (the number of non-null or "" strings), 1=min (as a string), 2=max
   *     (as a string). min and max are "" if n=0.
   */
  public String[] getNMinMax() {
    int nmm[] = getNMinMaxIndex();
    if (nmm[0] == 0) return new String[] {"0", "", ""};
    return new String[] {"" + nmm[0], getString(nmm[1]), getString(nmm[2])};
  }

  /**
   * Given nHave values and stride, this returns the actual number of points that will be found.
   *
   * @param nHave the size of the array (or end-start+1)
   * @param stride (must be &gt;= 1)
   * @return the actual number of points that will be found.
   */
  public static int strideWillFind(int nHave, int stride) {
    return 1 + (nHave - 1) / stride;
  }

  /**
   * This returns a new PrimitiveArray with a subset of this PrimitiveArray based on startIndex,
   * stride, and stopIndex.
   *
   * @param startIndex must be a valid index
   * @param stride must be at least 1
   * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
   * @return A new PrimitiveArray with the desired subset. It will have a backing array with a
   *     capacity equal to its size. If stopIndex &lt; startIndex, this returns a PrimitiveArray
   *     with size=0;
   */
  public PrimitiveArray subset(int startIndex, int stride, int stopIndex) {
    return subset(null, startIndex, stride, stopIndex);
  }

  /**
   * This returns a subset of this PrimitiveArray based on startIndex, stride, and stopIndex.
   *
   * @param pa the pa to be filled (may be null). If not null, must be of same type as this class.
   * @param startIndex must be a valid index
   * @param stride must be at least 1
   * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
   * @return The same pa (or a new PrimitiveArray if it was null) with the desired subset. If new,
   *     it will have a backing array with a capacity equal to its size. It will have a new backing
   *     array with a capacity equal to its size. If stopIndex &lt; startIndex, this returns a
   *     PrimitiveArray with size=0;
   */
  public abstract PrimitiveArray subset(
      PrimitiveArray pa, int startIndex, int stride, int stopIndex);

  /**
   * This tests if 'value1 op value2' is true. The =~ regex test must be tested with String
   * testValueOpValue, not here, because value2 is a regex (not a double).
   *
   * @param value1 Integer.MAX_VALUE is treated as NaN
   * @param op one of EDDTable.OPERATORS
   * @param value2
   * @return true if 'value1 op value2' is true. <br>
   *     Tests of "NaN = NaN" will evaluate to true. <br>
   *     Tests of "nonNaN != NaN" will evaluate to true. <br>
   *     All other tests where value1 is NaN or value2 is NaN will evaluate to false.
   * @throws RuntimeException if trouble (e.g., invalid op)
   */
  public static boolean testValueOpValue(int value1, String op, int value2) {
    // String2.log("testValueOpValue (long): " + value1 + op + value2);
    if (op.equals("=")) return value1 == value2;
    if (op.equals("!=")) return value1 != value2;

    if (value1 == Integer.MAX_VALUE || value2 == Integer.MAX_VALUE) return false;
    if (op.equals("<=")) return value1 <= value2;
    if (op.equals(">=")) return value1 >= value2;
    if (op.equals("<")) return value1 < value2;
    if (op.equals(">")) return value1 > value2;

    // Regex test has to be handled via String testValueOpValue
    //  if (op.equals(PrimitiveArray.REGEX_OP))
    throw new SimpleException("Query error: " + "Unknown operator=\"" + op + "\".");
  }

  /**
   * This tests if 'value1 op value2' is true. The =~ regex test must be tested with String
   * testValueOpValue, not here, because value2 is a regex (not a double).
   *
   * @param value1 Long.MAX_VALUE is treated as NaN
   * @param op one of EDDTable.OPERATORS
   * @param value2
   * @return true if 'value1 op value2' is true. <br>
   *     Tests of "NaN = NaN" will evaluate to true. <br>
   *     Tests of "nonNaN != NaN" will evaluate to true. <br>
   *     All other tests where value1 is NaN or value2 is NaN will evaluate to false.
   * @throws RuntimeException if trouble (e.g., invalid op)
   */
  public static boolean testValueOpValue(long value1, String op, long value2) {
    // String2.log("testValueOpValue (long): " + value1 + op + value2);
    if (op.equals("=")) return value1 == value2;
    if (op.equals("!=")) return value1 != value2;

    if (value1 == Long.MAX_VALUE || value2 == Long.MAX_VALUE) return false;
    if (op.equals("<=")) return value1 <= value2;
    if (op.equals(">=")) return value1 >= value2;
    if (op.equals("<")) return value1 < value2;
    if (op.equals(">")) return value1 > value2;

    // Regex test has to be handled via String testValueOpValue
    //  if (op.equals(PrimitiveArray.REGEX_OP))
    throw new SimpleException("Query error: " + "Unknown operator=\"" + op + "\".");
  }

  /**
   * This tests if 'value1 op value2' is true. The =~ regex test must be tested with String
   * testValueOpValue, not here, because value2 is a regex (not a double).
   *
   * @param value1 ULong.MAX_VALUE is treated as NaN
   * @param op one of EDDTable.OPERATORS
   * @param value2
   * @return true if 'value1 op value2' is true. <br>
   *     Tests of null == null will evaluate to true. <br>
   *     Tests of "nonNull!= null" will evaluate to true. <br>
   *     All other tests where value1 is NaN or value2 is NaN will evaluate to false.
   * @throws RuntimeException if trouble (e.g., invalid op)
   */
  public static boolean testValueOpValue(BigInteger value1, String op, BigInteger value2) {
    // String2.log("testValueOpValue (long): " + value1 + op + value2);
    // treat null as MAX_VALUE
    if (value1 == null) value1 = ULongArray.MAX_VALUE;
    if (value2 == null) value2 = ULongArray.MAX_VALUE;

    if (op.equals("=")) return value1.equals(value2);
    if (op.equals("!=")) return !value1.equals(value2);

    if (value1.equals(ULongArray.MAX_VALUE) || value2.equals(ULongArray.MAX_VALUE)) return false;
    if (op.equals("<=")) return value1.compareTo(value2) <= 0;
    if (op.equals(">=")) return value1.compareTo(value2) >= 0;
    if (op.equals("<")) return value1.compareTo(value2) < 0;
    if (op.equals(">")) return value1.compareTo(value2) > 0;

    // Regex test has to be handled via String testValueOpValue
    //  if (op.equals(PrimitiveArray.REGEX_OP))
    throw new SimpleException("Query error: " + "Unknown operator=\"" + op + "\".");
  }

  /**
   * This tests if 'value1 op value2' is true. The &lt;=, &gt;=, and = tests are (partly) done with
   * Math2.almostEqual9 so there is a little fudge factor. The =~ regex test must be tested with
   * String testValueOpValue, not here, because value2 is a regex (not a double).
   *
   * @param value1
   * @param op one of EDDTable.OPERATORS
   * @param value2
   * @return true if 'value1 op value2' is true. <br>
   *     Tests of "NaN = NaN" will evaluate to true. <br>
   *     Tests of "nonNaN != NaN" will evaluate to true. <br>
   *     All other tests where value1 is NaN or value2 is NaN will evaluate to false.
   * @throws RuntimeException if trouble (e.g., invalid op)
   */
  public static boolean testValueOpValue(float value1, String op, float value2) {
    // String2.log("testValueOpValue (float): " + value1 + op + value2);
    if (op.equals("<=")) return value1 <= value2 || Math2.almostEqual(6, value1, value2);
    if (op.equals(">=")) return value1 >= value2 || Math2.almostEqual(6, value1, value2);
    if (op.equals("="))
      return (Float.isNaN(value1) && Float.isNaN(value2)) || Math2.almostEqual(6, value1, value2);
    if (op.equals("<")) return value1 < value2;
    if (op.equals(">")) return value1 > value2;
    if (op.equals("!="))
      return (Float.isNaN(value1) && Float.isNaN(value2)) ? false : value1 != value2;
    // Regex test has to be handled via String testValueOpValue
    //  if (op.equals(PrimitiveArray.REGEX_OP))
    throw new SimpleException("Query error: " + "Unknown operator=\"" + op + "\".");
  }

  /**
   * This tests if 'value1 op value2' is true. The &lt;=, &gt;=, and = tests are (partly) done with
   * Math2.almostEqual9 so there is a little fudge factor. The =~ regex test must be tested with
   * String testValueOpValue, not here, because value2 is a regex (not a double).
   *
   * @param value1
   * @param op one of EDDTable.OPERATORS
   * @param value2
   * @return true if 'value1 op value2' is true. <br>
   *     Tests of "NaN = NaN" will evaluate to true. <br>
   *     Tests of "nonNaN != NaN" will evaluate to true. <br>
   *     All other tests where value1 is NaN or value2 is NaN will evaluate to false.
   * @throws RuntimeException if trouble (e.g., invalid op)
   */
  public static boolean testValueOpValue(double value1, String op, double value2) {
    // String2.log("testValueOpValue (double): " + value1 + op + value2);
    // if (Double.isNaN(value2) && Double.isNaN(value1)) { //test2 first, less likely to be NaN
    //    return (op.equals("=") || op.equals("<=") || op.equals(">=")); //the '=' matters
    // }
    if (op.equals("<=")) return value1 <= value2 || Math2.almostEqual(9, value1, value2);
    if (op.equals(">=")) return value1 >= value2 || Math2.almostEqual(9, value1, value2);
    if (op.equals("="))
      return (Double.isNaN(value1) && Double.isNaN(value2)) || Math2.almostEqual(9, value1, value2);
    if (op.equals("<")) return value1 < value2;
    if (op.equals(">")) return value1 > value2;
    if (op.equals("!="))
      return (Double.isNaN(value1) && Double.isNaN(value2)) ? false : value1 != value2;
    // Regex test has to be handled via String testValueOpValue
    //  if (op.equals(PrimitiveArray.REGEX_OP))
    throw new SimpleException("Query error: " + "Unknown operator=\"" + op + "\".");
  }

  /**
   * This tests if 'value1 op value2' is true for doubles to 12 sig figures (e.g., for time). The
   * &lt;=, &gt;=, and = tests are (partly) done with Math2.almostEqual12 so there is a little fudge
   * factor. The =~ regex test must be tested with String testValueOpValue, not here, because value2
   * is a regex (not a double).
   *
   * @param value1
   * @param op one of EDDTable.OPERATORS
   * @param value2
   * @return true if 'value1 op value2' is true. <br>
   *     Tests of "NaN = NaN" will evaluate to true. <br>
   *     Tests of "nonNaN != NaN" will evaluate to true. <br>
   *     All other tests where value1 is NaN or value2 is NaN will evaluate to false.
   * @throws RuntimeException if trouble (e.g., invalid op)
   */
  public static boolean testValueOpValueExtra(double value1, String op, double value2) {
    // String2.log("testValueOpValueExtra (double): " + value1 + op + value2);
    // if (Double.isNaN(value2) && Double.isNaN(value1)) { //test2 first, less likely to be NaN
    //    return (op.equals("=") || op.equals("<=") || op.equals(">=")); //the '=' matters
    // }
    if (op.equals("<=")) return value1 <= value2 || Math2.almostEqual(12, value1, value2);
    if (op.equals(">=")) return value1 >= value2 || Math2.almostEqual(12, value1, value2);
    if (op.equals("="))
      return (Double.isNaN(value1) && Double.isNaN(value2))
          || Math2.almostEqual(12, value1, value2);
    if (op.equals("<")) return value1 < value2;
    if (op.equals(">")) return value1 > value2;
    if (op.equals("!="))
      return (Double.isNaN(value1) && Double.isNaN(value2)) ? false : value1 != value2;
    // Regex test has to be handled via String testValueOpValue
    //  if (op.equals(PrimitiveArray.REGEX_OP))
    throw new SimpleException("Query error: " + "Unknown operator=\"" + op + "\".");
  }

  /**
   * This tests if 'value1 op value2' is true for doubles (e.g., source is an integer type). The
   * &lt;=, &gt;=, and = tests are raw so there is no fudge factor. The =~ regex test must be tested
   * with String testValueOpValue, not here, because value2 is a regex (not a double).
   *
   * @param value1
   * @param op one of EDDTable.OPERATORS
   * @param value2
   * @return true if 'value1 op value2' is true. <br>
   *     Tests of "NaN = NaN" will evaluate to true. <br>
   *     Tests of "nonNaN != NaN" will evaluate to true. <br>
   *     All other tests where value1 is NaN or value2 is NaN will evaluate to false.
   * @throws RuntimeException if trouble (e.g., invalid op)
   */
  public static boolean testValueOpValueExact(double value1, String op, double value2) {
    // String2.log("testValueOpValue (double): " + value1 + op + value2);
    // if (Double.isNaN(value2) && Double.isNaN(value1)) { //test2 first, less likely to be NaN
    //    return (op.equals("=") || op.equals("<=") || op.equals(">=")); //the '=' matters
    // }
    if (op.equals("<=")) return value1 <= value2;
    if (op.equals(">=")) return value1 >= value2;
    if (op.equals("=")) return (Double.isNaN(value1) && Double.isNaN(value2)) || value1 == value2;
    if (op.equals("<")) return value1 < value2;
    if (op.equals(">")) return value1 > value2;
    if (op.equals("!="))
      return (Double.isNaN(value1) && Double.isNaN(value2)) ? false : value1 != value2;
    // Regex test has to be handled via String testValueOpValue
    //  if (op.equals(PrimitiveArray.REGEX_OP))
    throw new SimpleException("Query error: " + "Unknown operator=\"" + op + "\".");
  }

  /**
   * This tests if 'value1 op value2' is true. The ops containing with &lt; and &gt; compare
   * value1.toLowerCase() and value2.toLowerCase().
   *
   * <p>Note that "" is not treated specially. "" isn't like NaN. <br>
   * testValueOpValue("a" &gt; "") will return true. <br>
   * testValueOpValue("a" &lt; "") will return false. <br>
   * testValueOpValue("" &lt; "a") will return true. <br>
   * testValueOpValue("" &gt; "a") will return false. <br>
   * testValueOpValue("" = "") will return true. <br>
   * Users should add another constraint (&amp;col2!="") if they don't want "" values. <br>
   * [I might like it to parallel NaN, but that would be just me -- <br>
   * it would defy common tests in all computer languages. <br>
   * And ERDDAP doesn't support null (hard to represent in many file types).] <br>
   * Stated another way, "" (as value1 or value2) behaves almost like char#0.
   *
   * @param value1 (shouldn't be null)
   * @param op one of EDDTable.OPERATORS
   * @param value2 (shouldn't be null)
   * @return true if 'value1 op value2' is true.
   * @throws RuntimeException if trouble (e.g., invalid op)
   */
  public static boolean testValueOpValue(String value1, String op, String value2) {
    // String2.log("testValueOpValue (String): " + value1 + op + value2);
    if (op.equals("=")) return value1.equals(value2);
    if (op.equals("!=")) return !value1.equals(value2);
    if (op.equals(REGEX_OP)) return value1.matches(value2); // regex test

    int t = value1.toLowerCase().compareTo(value2.toLowerCase());
    if (op.equals("<=")) return t <= 0;
    if (op.equals(">=")) return t >= 0;
    if (op.equals("<")) return t < 0;
    if (op.equals(">")) return t > 0;
    throw new SimpleException("Query error: " + "Unknown operator=\"" + op + "\".");
  }

  /** This is applies one constraint and just keeps the results. */
  public int applyConstraintAndKeep(boolean morePrecise, String op, String value2) {
    BitSet keep = new BitSet();
    keep.set(0, size());
    int nGood = applyConstraint(morePrecise, keep, op, value2);
    if (nGood == 0) clear();
    else justKeep(keep);
    return nGood;
  }

  /**
   * This tests the keep=true elements to see if 'get(element) op value2' is true. If the test is
   * false, the keep element is set to false. <br>
   * For float and double tests, the &lt;=, &gt;=, and = tests are (partly) done with
   * Math2.almostEqual(6) and (9) and (13 for morePrecise doubles), so there is a little fudge
   * factor. <br>
   * The =~ regex test is tested with String testValueOpValue, because value2 is a regex (not a
   * numeric type).
   *
   * <p>For integer-type PrimitiveArrays, MAX_VALUE is treated as a NaN when maxIsMV=true. <br>
   * Tests of "NaN = NaN" will evaluate to true. <br>
   * Tests of "nonNaN != NaN" will evaluate to true. <br>
   * All other tests where value1 is NaN or value2 is NaN will evaluate to false.
   *
   * @param morePrecise e.g., for tests of time values which are very precise.
   * @param keep The test is only applied to keep=true elements. If the test is false, the keep
   *     element is set to false.
   * @param op one of EDDTable.OPERATORS
   * @param value2
   * @return nStillGood
   * @throws RuntimeException if trouble (e.g., invalid op or invalid keep element)
   */
  public int applyConstraint(boolean morePrecise, BitSet keep, String op, String value2) {

    // regex
    if (op.equals(REGEX_OP)) {
      // String2.log("applyConstraint(regex)");
      int nStillGood = 0;
      Pattern p = Pattern.compile(value2); // big time savings
      for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
        if (p.matcher(getString(row)).matches()) nStillGood++;
        else keep.clear(row);
      }
      return nStillGood;
    }

    // string
    if (elementType() == PAType.STRING || elementType() == PAType.CHAR) {
      // String2.log("applyConstraint(String)");
      int nStillGood = 0;
      for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
        // String2.log(">> test " + getString(row) + op + value2 + " -> " +
        // testValueOpValue(getString(row), op, value2));
        if (testValueOpValue(getString(row), op, value2)) nStillGood++;
        else keep.clear(row);
      }
      return nStillGood;
    }

    // ulong
    if (elementType() == PAType.ULONG) {
      BigInteger value2l =
          String2.strictParseULongObject(value2); // null if has decimal part or ...
      int nStillGood = 0;
      if (value2l == null) { // if trouble, do 'exact' test via double
        double value2d = String2.parseDouble(value2);
        for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
          if (testValueOpValueExact(getDouble(row), op, value2d)) nStillGood++;
          else keep.clear(row);
        }
      } else { // value2 parsed cleanly as a ulong
        // String2.log("applyConstraint(long)");
        for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
          if (testValueOpValue(getULong(row), op, value2l)) nStillGood++;
          else keep.clear(row);
        }
      }
      return nStillGood;
    }

    // long
    if (elementType() == PAType.LONG) {
      double value2d = String2.parseDouble(value2);
      long value2l = String2.parseLong(value2); // LongArray.MAX_VALUE if trouble
      int nStillGood = 0;
      if (value2d == value2l && value2l != Long.MAX_VALUE) { // value2 parsed cleanly as a long
        // String2.log("applyConstraint(long)");
        for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
          if (testValueOpValue(getLong(row), op, value2l)) nStillGood++;
          else keep.clear(row);
        }
      } else { // do 'exact' test via double
        for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
          if (testValueOpValueExact(getDouble(row), op, value2d)) nStillGood++;
          else keep.clear(row);
        }
      }
      return nStillGood;
    }

    // int types
    if (isIntegerType()) {
      double value2d = String2.parseDouble(value2);
      int value2i = String2.parseInt(value2);
      int nStillGood = 0;
      if (value2d == value2i && value2d != Integer.MAX_VALUE) { // value2 parsed cleanly as int
        // String2.log("applyConstraint(int)");
        for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
          if (testValueOpValue(getInt(row), op, value2i)) nStillGood++;
          else keep.clear(row);
        }
      } else { // do exact test
        for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
          if (testValueOpValueExact(getDouble(row), op, value2d)) nStillGood++;
          else keep.clear(row);
        }
      }
      return nStillGood;
    }

    // float
    if (elementType() == PAType.FLOAT) {
      // String2.log("applyConstraint(float)");
      int nStillGood = 0;
      float value2f = String2.parseFloat(value2);
      for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
        if (testValueOpValue(getFloat(row), op, value2f)) nStillGood++;
        else keep.clear(row);
      }
      return nStillGood;
    }

    // morePrecise
    if (morePrecise) {
      int nStillGood = 0;
      double value2d = String2.parseDouble(value2);
      for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
        if (testValueOpValueExtra(getDouble(row), op, value2d)) // extra
        nStillGood++;
        else keep.clear(row);
      }
      // String2.log(">> applyConstraint(double, morePrecise) nStillGood=" + nStillGood);
      return nStillGood;
    }

    // treat everything else via double tests (that should be all that is left)
    // String2.log("applyConstraint(double)");
    int nStillGood = 0;
    double value2d = String2.parseDouble(value2);
    for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
      if (testValueOpValue(getDouble(row), op, value2d)) // normal
      nStillGood++;
      else keep.clear(row);
    }
    return nStillGood;
  }

  /**
   * This converts a StringArray with NCCSV attribute values into a typed PrimitiveArray.
   *
   * @param sa Almost always from StringArray.fromNccsv(). E.g., ["7b", "-12b"]
   * @return a typed PrimitiveArray e,g, ByteArray with [7, -12]. If sa is null or sa.length() == 0,
   *     this returns this sa. If sa is interpreted as a StringArray, sa will be returned with
   *     canonical (perhaps modified) values.
   * @throws SimpleException if trouble.
   */
  public static PrimitiveArray parseNccsvAttributes(StringArray sa) {
    if (sa == null || sa.size() == 0) return sa;
    // String2.log("nccsv(sa)=" + sa.toNccsv127AttString());

    // are first and lastChar all the same? e.g., 7b, -12b
    int saSize = sa.size();
    boolean firstCharSame = sa.get(0).length() >= 1; // initially just first value
    boolean lastCharSame = sa.get(0).length() >= 1; // initially just first value
    boolean last2CharSame = sa.get(0).length() >= 2; // initially just first value
    char firstChar = ' '; // junk for now
    char lastChar = ' '; // junk for now
    String last2Char = "";
    if (lastCharSame) {
      String s = sa.get(0); // it will be length() >= 1 (tested above)
      firstChar = s.charAt(0);
      lastChar = s.charAt(s.length() - 1);
      if (last2CharSame) // it will be length() >= 2 (tested above)
      last2Char = s.substring(s.length() - 2);
      for (int i = 1; i < saSize; i++) {
        s = sa.get(i);
        if (s.length() == 0 || s.charAt(0) != firstChar) firstCharSame = false;
        if (last2CharSame && !s.endsWith(last2Char)) last2CharSame = false;
        if (s.length() == 0 || s.charAt(s.length() - 1) != lastChar) {
          lastCharSame = false;
          break;
        }
      }
    }

    // what type is it?
    if (last2CharSame) { // it might be an unsigned type
      if (last2Char.equals("ub")) {
        if (sa.firstNonMatch(String2.NCCSV_UBYTE_ATT_PATTERN) < 0) {
          UByteArray uba = new UByteArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            uba.addString(s.substring(0, s.length() - 2));
            // ensure that maxValue was correctly specified, not e.g., 999ub
            if (uba.isMaxValue(i) && !"255ub".equals(s))
              throw new SimpleException("Invalid ubyte value: " + s);
          }
          return uba;
        }
      } else if (last2Char.equals("us")) {
        if (sa.firstNonMatch(String2.NCCSV_USHORT_ATT_PATTERN) < 0) {
          UShortArray uba = new UShortArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            uba.addString(s.substring(0, s.length() - 2));
            if (uba.isMaxValue(i) && !"65535us".equals(s))
              throw new SimpleException("Invalid ushort value: " + s);
          }
          return uba;
        }
      } else if (last2Char.equals("ui")) {
        if (sa.firstNonMatch(String2.NCCSV_UINT_ATT_PATTERN) < 0) {
          UIntArray uia = new UIntArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            uia.addString(s.substring(0, s.length() - 2));
            if (uia.isMaxValue(i) && !"4294967295ui".equals(s))
              throw new SimpleException("Invalid uint value: " + s);
          }
          return uia;
        }
      } else if (last2Char.equals("uL")) {
        if (sa.firstNonMatch(String2.NCCSV_ULONG_ATT_PATTERN) < 0) {
          ULongArray ula = new ULongArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            ula.addString(s.substring(0, s.length() - 2));
            if (ula.isMaxValue(i) && !"18446744073709551615uL".equals(s))
              throw new SimpleException("Invalid ulong value: " + s);
          }
          return ula;
        }
      }
    }

    if (lastCharSame) {
      if (lastChar == 'b') {
        if (sa.firstNonMatch(String2.NCCSV_BYTE_ATT_PATTERN) < 0) {
          ByteArray ba = new ByteArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            ba.addString(s.substring(0, s.length() - 1));
            if (ba.isMaxValue(i) && !"127b".equals(s))
              throw new SimpleException("Invalid byte value: " + s);
          }
          return ba;
        } // all of these: else fall through to StringArray
      } else if (lastChar == 's') {
        if (sa.firstNonMatch(String2.NCCSV_SHORT_ATT_PATTERN) < 0) {
          ShortArray ba = new ShortArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            ba.addString(s.substring(0, s.length() - 1));
            if (ba.get(i) == Short.MAX_VALUE && !"32767s".equals(s))
              throw new SimpleException("Invalid short value: " + s);
          }
          return ba;
        }
      } else if (lastChar == 'L') {
        if (sa.firstNonMatch(String2.NCCSV_LONG_ATT_PATTERN) < 0) {
          LongArray la = new LongArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            la.addString(s.substring(0, s.length() - 1));
            if (la.get(i) == Long.MAX_VALUE && !"9223372036854775807L".equals(s))
              throw new SimpleException("Invalid long value: " + s);
          }
          return la;
        }
      } else if (lastChar == 'f') {
        if (sa.firstNonMatch(String2.NCCSV_FLOAT_ATT_PATTERN) < 0) {
          FloatArray fa = new FloatArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            fa.addString(s.substring(0, s.length() - 1)); // Infinity -> NaN
            // String2.log(">> float " + fa.get(i) + " from " + s);
            if (Float.isNaN(fa.get(i)) && !"NaNf".equals(s))
              throw new SimpleException("Invalid float value: " + s);
          }
          return fa;
        }
      } else if (lastChar == 'd') {
        if (sa.firstNonMatch(String2.NCCSV_DOUBLE_ATT_PATTERN) < 0) {
          // String2.log(">> doubles? " + sa.firstNonMatch(String2.NCCSV_DOUBLE_ATT_PATTERN) + ": "
          // + sa.toString());
          DoubleArray da = new DoubleArray(saSize, false);
          for (int i = 0; i < saSize; i++) {
            String s = sa.get(i);
            da.addString(s); // Infinity -> NaN
            if (Double.isNaN(da.get(i)) && !"NaNd".equals(s))
              throw new SimpleException("Invalid double value: " + s);
          }
          return da;
        }
      }
    }

    // ints?
    if (sa.firstNonMatch(String2.NCCSV_INT_ATT_PATTERN) < 0) {
      IntArray ia = new IntArray(saSize, false);
      for (int i = 0; i < saSize; i++) {
        String s = sa.get(i);
        ia.addString(s.substring(0, s.length() - 1));
        if (ia.get(i) == Integer.MAX_VALUE && !"2147483647i".equals(s))
          throw new SimpleException("Invalid int value: " + s);
      }
      return ia;
    }

    // char?
    if (sa.firstNonMatch(String2.NCCSV_CHAR_ATT_PATTERN) < 0) {
      CharArray ca = new CharArray(saSize, false);
      for (int i = 0; i < saSize; i++) ca.add(String2.fromNccsvChar(sa.get(i)));
      return ca;
    }

    // if nothing else matched - > StringArray
    // convert nccsv strings to true strings (and canonical) and return StringArray
    sa.fromNccsv();
    return sa;
  }

  /**
   * This returns int[2] with the indices of the first 2 duplicate values, or null if no duplicates.
   *
   * @return int[2] with the indices of the first 2 duplicate values, or null if no duplicates.
   */
  public int[] firstDuplicates() {
    HashSet<String> hs = new HashSet();
    for (int i = 0; i < size(); i++) {
      String s = getString(i);
      if (!hs.add(s)) return new int[] {indexOf(s), i};
    }
    return null;
  }

  /**
   * This throws a SimpleException if there are duplicate values.
   *
   * @param partialMsg e.g., "Invalid table: Duplicate column names: "
   */
  public void ensureNoDuplicates(String partialMsg) {
    int fd[] = firstDuplicates();
    if (fd == null) return;
    throw new SimpleException(
        String2.ERROR
            + ": "
            + partialMsg
            + "["
            + fd[0]
            + "] and ["
            + fd[1]
            + "] are both \""
            + getString(fd[0])
            + "\".");
  }

  /**
   * For integer types, this fixes unsigned bytes that were incorrectly read as signed so that they
   * have the correct ordering of values (0 to 255 becomes -128 to 127). <br>
   * What were read as signed: 0 127 -128 -1 <br>
   * should become unsigned: -128 -1 0 255 <br>
   * This also does the reverse. <br>
   * For non-integer types, this does nothing.
   */
  public void changeSignedToFromUnsigned() {}

  /**
   * This tries to find the missing_value (e.g., 9999, 1e37) used by this pa. THIS HAS BEEN
   * SUPERSEDED BY EDD.addMvFvAttsIfNeeded(), which works with existing metadata and can find 2
   * values (mv and fv).
   *
   * @return the missing_value (e.g., 9999, 1e37) used by this pa, or null if none. For CharArray,
   *     this only looks for 0 and MAX_VALUE. For StringArray, this currently doesn't look for e.g.,
   *     "9999".
   */
  public PAOne tryToFindNumericMissingValue() {

    if (size() == 0 || elementType() == PAType.STRING) return null;

    int mmi[] = getNMinMaxIndex();
    // String2.log(">> nMinMaxIndex=" + String2.toCSSVString(mmi));
    boolean hasCoHortMV =
        mmi[0] < size
            || // maxIsMV=true and there is an mv
            (mmi[0] > 0 && isMaxValue(mmi[2])); // maxIsMV=false and the max value is the MAX_VALUE
    if (mmi[0] == 0) { // i.e., pa only has missing values
      if (hasCoHortMV) {
        // pa is only CoHortMV values
        return missingValue(); // cohort mv
      }
      return null;
    }

    PAOne min = getPAOne(mmi[1]);
    PAOne max = getPAOne(mmi[2]);
    // String2.log("> min=" + min);
    if (elementType() == PAType.CHAR) {
      // look for 0 or ffff
      if (min.getInt() == 0) return min;
      if (hasCoHortMV) return missingValue(); // cohort mv
      return null;
    }

    if (elementType() != PAType.STRING) {
      int whichMv9 = DoubleArray.MV9.indexOf(min.getDouble());
      if (whichMv9 >= 0) return min;
      whichMv9 = DoubleArray.MV9.indexOf(max.getDouble());
      if (whichMv9 >= 0) return max;
    }

    if (elementType() == PAType.DOUBLE) {
      if (min.getDouble() <= -1e300) return min;
      if (max.getDouble() >= 1e300) return max;

    } else if (elementType() == PAType.FLOAT) {
      if (min.getFloat() < -5e36f) return min;
      if (max.getFloat() > 5e36f) return max;

    } else if (isUnsigned()) {
      if (hasCoHortMV) return missingValue(); // cohort mv

    } else {
      // signed integer types
      if (MINEST_VALUE().equals(min)) return min;
      if (hasCoHortMV) return missingValue(); // cohort mv
    }
    return null;
  }
}
