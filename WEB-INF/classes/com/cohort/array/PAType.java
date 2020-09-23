/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.Math2;
   
/**
 *
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

    //BOOLEAN is only used temporarily. There is no BooleanArray.
    BOOLEAN;

    /** This returns true for UBYTe, USHORT, UINT, and ULONG. */
    public boolean isUnsigned() {
        return isUnsigned(this); 
    }

    /** This returns true for UBYTe, USHORT, UINT, and ULONG. */
    public static boolean isUnsigned(PAType tPaType) {
        return tPaType == UBYTE || tPaType == USHORT || tPaType == UINT || tPaType == ULONG; 
    }

    /** This returns false for UBYTe, USHORT, UINT, and ULONG. */
    public static boolean isIntegerType(PAType tPaType) {
        return tPaType == FLOAT || tPaType == DOUBLE || tPaType == STRING || tPaType == CHAR? false : true; 
    }

    /** This returns the unsigned version of the PAType. */
    public static PAType makeUnsigned(PAType tPaType) {
        return tPaType == BYTE?  UBYTE :
               tPaType == SHORT? USHORT :
               tPaType == INT?   UINT :
               tPaType == LONG?  ULONG : tPaType;
    }

    /** This returns the signed version of the PAType. */
    public static PAType makeSigned(PAType tPaType) {
        return tPaType == UBYTE?  BYTE :
               tPaType == USHORT? SHORT :
               tPaType == UINT?   INT :
               tPaType == ULONG?  LONG : tPaType;
    }

    /**
     * This converts an element type String (e.g., "float") to an element type (e.g., PAType.FLOAT).
     *
     * @param type an element type string (e.g., "float")
     * @return the corresponding element type (e.g., PAType.FLOAT) or null if no match
     */
    public static PAType safeFromCohortString(String type) {  //was PrimitiveArray.safeElementStringToPAType
        if (type.equals("double")) return DOUBLE;
        if (type.equals("float"))  return FLOAT;
        if (type.equals("long"))   return LONG;
        if (type.equals("int"))    return INT;
        if (type.equals("short"))  return SHORT;
        if (type.equals("byte") ||
            type.equals("boolean"))return BYTE; //erddap stores booleans as bytes
        if (type.equals("char"))   return CHAR;
        if (type.equals("String")) return STRING;
        if (type.equals("ulong"))  return ULONG;
        if (type.equals("uint"))   return UINT;
        if (type.equals("ushort")) return USHORT;
        if (type.equals("ubyte"))  return UBYTE; 
        return null;
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
    public static String toCohortString(PAType type) {  //was PrimitiveArray.elementTypeToString(
        if (type == DOUBLE) return "double";
        if (type == FLOAT)  return "float";
        if (type == LONG)   return "long";
        if (type == INT)    return "int";
        if (type == SHORT)  return "short";
        if (type == BYTE)   return "byte";
        if (type == CHAR)   return "char";
        if (type == STRING) return "String";
        if (type == ULONG)  return "ulong";
        if (type == UINT)   return "uint";
        if (type == USHORT) return "ushort";
        if (type == UBYTE)  return "ubyte";
        throw new IllegalArgumentException(
            "PAType.toCohortString unsupported type: "  + type.toString());
    }

    /**
     * This converts an element type String (e.g., "float") to an element PAType (e.g., PAType.FLOAT).
     *
     * @param type an element type string (e.g., "float")
     * @return the corresponding element type (e.g., PAType.FLOAT)
     */
    public static PAType fromCohortStringCaseInsensitive(String type) {  //was PrimitiveArray.caseInsensitiveElementStringToPAType(
        type = type.toLowerCase();
        if (type.equals("double")) return PAType.DOUBLE;
        if (type.equals("float"))  return PAType.FLOAT;
        if (type.equals("long"))   return PAType.LONG;
        if (type.equals("ulong"))  return PAType.ULONG;
        if (type.equals("int"))    return PAType.INT;
        if (type.equals("uint"))   return PAType.UINT;
        if (type.equals("short"))  return PAType.SHORT;
        if (type.equals("ushort")) return PAType.USHORT;
        if (type.equals("byte") ||
            type.equals("boolean"))return PAType.BYTE; //erddap stores booleans as bytes
        if (type.equals("ubyte"))  return PAType.UBYTE;
        if (type.equals("char"))   return PAType.CHAR;
        if (type.equals("string")) return PAType.STRING;
        throw new IllegalArgumentException("PAType.fromCohortStringCaseInsensitive unsupported type: " + type);
    }

    /**
     * This indicates the number of bytes per element of the given type.
     * The value for PAType.STRING isn't a constant, so this returns 20.
     *
     * @param type an element PAType (e.g., PAType.FLOAT)
     * @return the corresponding number of bytes
     */
    public static int elementSize(PAType type) {
        if (type == PAType.DOUBLE) return 8;
        if (type == PAType.FLOAT)  return 4;
        if (type == PAType.LONG ||
            type == PAType.ULONG)  return 8;
        if (type == PAType.INT ||
            type == PAType.UINT)   return 4;
        if (type == PAType.SHORT ||
            type == PAType.USHORT) return 2;
        if (type == PAType.BYTE ||
            type == PAType.UBYTE ||
            type == PAType.BOOLEAN)return 1; //erddap stores booleans as bytes
        if (type == PAType.CHAR)   return 2;
        if (type == PAType.STRING) return 20;
        throw new IllegalArgumentException("PrimitiveArray.sizeOf unsupported type: " + type);
    }


    /**
     * This indicates the number of bytes per element of the given type.
     * The value for PAType.STRING isn't a constant, so this returns 20.
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
     * Currently, PAType.LONG and PAType.ULONG return F64.
     * Currently, PAType.CHAR returns a numeric U16.
     * Currently, PAType.STRING and others return UNKNOWN.
     *
     * @param tPAType e.g., PAType.DOUBLE or PAType.STRING
     * @return the corresponding ESRI pixel type
     */
    public static String toEsriPixelType(PAType tPAType) { //was PrimitiveArray.paTypeToEsriPixelType(
        //I can't find definitions of C64 and C128
        if (tPAType == PAType.DOUBLE) return "F64";
        if (tPAType == PAType.FLOAT)  return "F32"; 
        if (tPAType == PAType.LONG)   return "F64"; //not ideal, but no S64
        if (tPAType == PAType.ULONG)  return "F64"; //not ideal, but no U64
        if (tPAType == PAType.INT)    return "S32";
        if (tPAType == PAType.UINT)   return "U32";
        if (tPAType == PAType.SHORT)  return "S16";
        if (tPAType == PAType.USHORT) return "U16";
        if (tPAType == PAType.BYTE)   return "S8"; 
        if (tPAType == PAType.UBYTE)  return "U8"; 
        if (tPAType == PAType.CHAR)   return "U16"; //
        //if (tPAType == PAType.STRING) return ...
        return "UNKNOWN";
    }

    /**
     * This returns the cohort missing value for a given element type (e.g., PAType.BYTE),
     * expressed as a double.
     *
     * @param type an element type (e.g., PAType.BYTE)
     * @return the string representation of the element type (e.g., Byte.MAX_VALUE).
     *   Note that the mv for float is Float.NaN, but it gets converted
     *   to Double.NaN when returned by this method.
     *   StringArray supports several incoming missing values, but
     *   "" is used as the outgoing missing value.
     */
    public static double missingValueAsDouble(PAType type) {  //was PrimitiveArray.missingValue(
//2020-09-21 THIS IS NO LONGER IN USE. REMOVE IT???
        if (type == DOUBLE) return Double.NaN;
        if (type == FLOAT)  return Double.NaN;
        if (type == LONG)   return Long.MAX_VALUE;
        if (type == ULONG)  return Math2.ULONG_MAX_VALUE_AS_DOUBLE;
        if (type == INT)    return Integer.MAX_VALUE;
        if (type == UINT)   return UIntArray.MAX_VALUE;
        if (type == SHORT)  return Short.MAX_VALUE;
        if (type == USHORT) return UShortArray.MAX_VALUE;
        if (type == BYTE)   return Byte.MAX_VALUE;
        if (type == UBYTE)  return UByteArray.MAX_VALUE;
        if (type == CHAR)   return Character.MAX_VALUE;
        if (type == STRING) return Double.NaN;
        return Double.NaN;
    }

}
