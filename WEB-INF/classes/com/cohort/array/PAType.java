/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.Math2;

/**
 * @author Bob Simons (CoHortSoftware@gmail.com)
 */
public enum PAType {
  BYTE,
  SHORT,
  CHAR,
  INT,
  LONG,
  FLOAT,
  DOUBLE,
  STRING,

  UBYTE,
  USHORT,
  UINT,
  ULONG,

  // BOOLEAN is only used temporarily. There is no BooleanArray.
  BOOLEAN;

  /** This returns true for UBYTe, USHORT, UINT, and ULONG. */
  public final boolean isUnsigned() {
    return switch (this) {
      case UBYTE, USHORT, UINT, ULONG -> true;
      default -> false;
    };
  }

  /** This returns false for UBYTe, USHORT, UINT, and ULONG. */
  public static final boolean isIntegerType(PAType tPaType) {
    return switch (tPaType) {
      case FLOAT, DOUBLE, STRING, CHAR -> false;
      default -> true;
    };
  }

  /** This returns the unsigned version of the PAType. */
  public static final PAType makeUnsigned(PAType tPaType) {
    return switch (tPaType) {
      case BYTE -> UBYTE;
      case SHORT -> USHORT;
      case INT -> UINT;
      case LONG -> ULONG;
      default -> tPaType;
    };
  }

  /** This returns the signed version of the PAType. */
  public static final PAType makeSigned(PAType tPaType) {
    return switch (tPaType) {
      case UBYTE -> BYTE;
      case USHORT -> SHORT;
      case UINT -> INT;
      case ULONG -> LONG;
      default -> tPaType;
    };
  }

  /**
   * This converts an element type String (e.g., "float") to an element type (e.g., PAType.FLOAT).
   *
   * @param type an element type string (e.g., "float")
   * @return the corresponding element type (e.g., PAType.FLOAT) or null if no match
   */
  public static PAType safeFromCohortString(
      String type) { // was PrimitiveArray.safeElementStringToPAType
    return switch (type) {
      case "double" -> DOUBLE;
      case "float" -> FLOAT;
      case "long" -> LONG;
      case "int" -> INT;
      case "short" -> SHORT;
      case "byte", "boolean" -> BYTE; // erddap stores booleans as bytes
      case "char" -> CHAR;
      case "String" -> STRING;
      case "ulong" -> ULONG;
      case "uint" -> UINT;
      case "ushort" -> USHORT;
      case "ubyte" -> UBYTE;
      default -> null;
    };
  }

  /**
   * This converts an element type String (e.g., "float") to an element PAType (e.g., PAType.FLOAT).
   *
   * @param type an element type string (e.g., "float")
   * @return the corresponding element type (e.g., PAType.FLOAT)
   */
  public static PAType fromCohortString(String type) {
    PAType tType = safeFromCohortString(type);
    if (tType == null)
      throw new IllegalArgumentException("PAType.fromCohortString unsupported type: " + type);
    return tType;
  }

  /**
   * This converts an element type (e.g., PAType.FLOAT) to a String (e.g., float).
   *
   * @param type an element type (e.g., PAType.FLOAT)
   * @return the string representation of the element type (e.g., float)
   */
  public static String toCohortString(PAType type) { // was PrimitiveArray.elementTypeToString(
    return switch (type) {
      case DOUBLE -> "double";
      case FLOAT -> "float";
      case LONG -> "long";
      case INT -> "int";
      case SHORT -> "short";
      case BYTE -> "byte";
      case CHAR -> "char";
      case STRING -> "String";
      case ULONG -> "ulong";
      case UINT -> "uint";
      case USHORT -> "ushort";
      case UBYTE -> "ubyte";
      default -> {
        throw new IllegalArgumentException(
            "PAType.toCohortString unsupported type: " + type.toString());
      }
    };
  }

  /**
   * This converts an element type String (e.g., "float") to an element PAType (e.g., PAType.FLOAT).
   *
   * @param type an element type string (e.g., "float")
   * @return the corresponding element type (e.g., PAType.FLOAT)
   */
  public static PAType fromCohortStringCaseInsensitive(
      String type) { // was PrimitiveArray.caseInsensitiveElementStringToPAType(
    return switch (type.toLowerCase()) {
      case "double" -> DOUBLE;
      case "float" -> FLOAT;
      case "long" -> LONG;
      case "ulong" -> ULONG;
      case "int" -> INT;
      case "uint" -> UINT;
      case "short" -> SHORT;
      case "ushort" -> USHORT;
      case "byte", "boolean" -> BYTE; // erddap stores booleans as bytes
      case "ubyte" -> UBYTE;
      case "char" -> CHAR;
      case "string" -> STRING;
      default -> {
        throw new IllegalArgumentException(
            "PAType.fromCohortStringCaseInsensitive unsupported type: " + type);
      }
    };
  }

  /**
   * This indicates the number of bytes per element of the given type. The value for PAType.STRING
   * isn't a constant, so this returns 20.
   *
   * @param type an element PAType (e.g., PAType.FLOAT)
   * @return the corresponding number of bytes
   */
  public static int elementSize(PAType type) {
    return switch (type) {
      case DOUBLE, LONG, ULONG -> 8;
      case FLOAT, INT, UINT -> 4;
      case SHORT, USHORT, CHAR -> 2;
      case BYTE, UBYTE, BOOLEAN -> 1; // erddap stores booleans as bytes
      case STRING -> 20;
      default -> {
        throw new IllegalArgumentException("PrimitiveArray.sizeOf unsupported type: " + type);
      }
    };
  }

  /**
   * This indicates the number of bytes per element of the given type. The value for PAType.STRING
   * isn't a constant, so this returns 20.
   *
   * @param type an element type (e.g., "String" or "float")
   * @return the corresponding number of bytes
   */
  public static int elementSize(String type) {
    return elementSize(fromCohortString(type));
  }

  /**
   * This converts a data class into an ESRI Pixel Type.
   * http://help.arcgis.com/en/arcgismobile/10.0/apis/android/api/com/esri/core/map/ImageServiceParameters.PIXEL_TYPE.html
   * Currently, PAType.LONG and PAType.ULONG return F64. Currently, PAType.CHAR returns a numeric
   * U16. Currently, PAType.STRING and others return UNKNOWN.
   *
   * @param tPAType e.g., PAType.DOUBLE or PAType.STRING
   * @return the corresponding ESRI pixel type
   */
  public static String toEsriPixelType(
      PAType tPAType) { // was PrimitiveArray.paTypeToEsriPixelType(
    // I can't find definitions of C64 and C128
    return switch (tPAType) {
      case DOUBLE -> "F64";
      case FLOAT -> "F32";
      case LONG -> "F64"; // not ideal, but no S64
      case ULONG -> "F64"; // not ideal, but no U64
      case INT -> "S32";
      case UINT -> "U32";
      case SHORT -> "S16";
      case USHORT -> "U16";
      case BYTE -> "S8";
      case UBYTE -> "U8";
      case CHAR -> "U16"; //
        //    case STRING ->   ...
      default -> "UNKNOWN";
    };
  }

  /**
   * This returns the cohort missing value for a given element type (e.g., PAType.BYTE), expressed
   * as a double.
   *
   * @param type an element type (e.g., PAType.BYTE)
   * @return the string representation of the element type (e.g., Byte.MAX_VALUE). Note that the mv
   *     for float is Float.NaN, but it gets converted to Double.NaN when returned by this method.
   *     StringArray supports several incoming missing values, but "" is used as the outgoing
   *     missing value.
   */
  public static double missingValueAsDouble(PAType type) { // was PrimitiveArray.missingValue(
    // 2020-09-21 THIS IS NO LONGER IN USE. REMOVE IT???
    return switch (type) {
      case DOUBLE, FLOAT, STRING -> Double.NaN;
      case LONG -> Long.MAX_VALUE;
      case ULONG -> Math2.ULONG_MAX_VALUE_AS_DOUBLE;
      case INT -> Integer.MAX_VALUE;
      case UINT -> UIntArray.MAX_VALUE;
      case SHORT -> Short.MAX_VALUE;
      case USHORT -> UShortArray.MAX_VALUE;
      case BYTE -> Byte.MAX_VALUE;
      case UBYTE -> UByteArray.MAX_VALUE;
      case CHAR -> Character.MAX_VALUE;
      default -> Double.NaN;
    };
  }
}
